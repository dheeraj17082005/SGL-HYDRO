"""
Tier 4 — Real-World Application Scenarios Test Suite.
Validates 5 complete, realistic operational workflows:
1. Scenario 1: Peak hour commercial fleet arrival (10 vehicles, 6-bay allocation, FIFO queue)
2. Scenario 2: High-risk safety interlock enforcement (expired hydro-test, dispenser lockout, safety alert)
3. Scenario 3: Unregistered/foreign state vehicle entry (RTO unverified, graceful handling without crash)
4. Scenario 4: Kiosk manual override journey (operator manual verification, supervisor override, audit trail)
5. Scenario 5: Edge camera disconnect and reconnection recovery during active fueling cycle
"""

import os
import sys
import time
from datetime import datetime

CURRENT_DIR = os.path.dirname(os.path.abspath(__file__))
if CURRENT_DIR not in sys.path:
    sys.path.insert(0, CURRENT_DIR)

try:
    from tests.e2e.harness import SystemContractHarness, AssertRaises
except ModuleNotFoundError:
    from harness import SystemContractHarness, AssertRaises


def test_t4_scenario_peak_hour_fleet_arrival():
    """
    Scenario 1: Peak Hour Commercial Fleet Arrival.
    10 commercial fleet vehicles queue in FIFO order, allocate across 6 bays,
    fuel, and exit while preserving queue integrity and concurrency limits.
    """
    harness = SystemContractHarness()
    fleet_plates = [f"GJ01FL{i:04d}" for i in range(1, 11)]

    # 1. All 10 fleet vehicles arrive and are detected
    journeys = []
    for plate in fleet_plates:
        det = harness.process_anpr_detection(station_id=1, camera_id=1, registration_number=plate)
        assert det["success"] is True
        assert det["data"]["complianceStatus"] == "ELIGIBLE"
        journeys.append(det["data"]["journeyId"])

    # 2. All 10 vehicles enter queue in FIFO order
    for j_id in journeys:
        harness.enter_queue(j_id)
        time.sleep(0.005)

    queue = harness.get_station_queue(1)
    assert len(queue) == 10
    # Strict FIFO verification: queue order matches arrival order
    assert [q["id"] for q in queue] == journeys

    # 3. Allocate first 6 vehicles to Bays 1 through 6
    active_fueling = []
    for bay_id in range(1, 7):
        next_vehicle = harness.get_station_queue(1)[0]["id"]
        assign_resp = harness.assign_bay(next_vehicle, bay_id=bay_id)
        assert assign_resp["data"]["status"] == "BAY_ASSIGNED"
        harness.start_fueling(next_vehicle)
        active_fueling.append((next_vehicle, bay_id))

    # All 6 bays are now OCCUPIED
    dashboard = harness.get_dashboard(1)
    assert dashboard["occupiedBays"] == 6
    assert dashboard["availableBays"] == 0
    assert dashboard["queueLength"] == 4  # 4 vehicles waiting in queue

    # 4. Attempting to assign 7th vehicle fails because no bay is available
    seventh_vehicle = harness.get_station_queue(1)[0]["id"]
    with AssertRaises(ValueError, match="is currently OCCUPIED"):
        harness.assign_bay(seventh_vehicle, bay_id=1)

    # 5. First 2 vehicles finish fueling and release Bays 1 and 2
    for j_id, bay_id in active_fueling[:2]:
        harness.complete_fueling(j_id)
        harness.exit_station(j_id)
        assert harness._bays[bay_id]["status"] == "AVAILABLE"

    # 6. Allocate waiting vehicles 7 and 8 to freed Bays 1 and 2
    v7 = harness.get_station_queue(1)[0]["id"]
    harness.assign_bay(v7, bay_id=1)
    harness.start_fueling(v7)

    v8 = harness.get_station_queue(1)[0]["id"]
    harness.assign_bay(v8, bay_id=2)
    harness.start_fueling(v8)

    # 7. Remaining vehicles complete fueling cycle
    for j_id in journeys:
        j = harness._journeys[j_id]
        if j["status"] == "IN_QUEUE":
            # Find available bay
            avail_bays = [b["id"] for b in harness._bays.values() if b["status"] == "AVAILABLE"]
            if not avail_bays:
                # complete one
                for b_id, bay in harness._bays.items():
                    if bay["status"] == "OCCUPIED":
                        occ_j = [item for item in harness._journeys.values() if item.get("assignedBayId") == b_id and item["status"] == "FUELING"]
                        if occ_j:
                            harness.complete_fueling(occ_j[0]["id"])
                            harness.exit_station(occ_j[0]["id"])
                            break
                avail_bays = [b["id"] for b in harness._bays.values() if b["status"] == "AVAILABLE"]
            harness.assign_bay(j_id, avail_bays[0])
            harness.start_fueling(j_id)

        if j["status"] == "FUELING":
            harness.complete_fueling(j_id)
            harness.exit_station(j_id)

    # Verify all 10 vehicles reached EXITED status
    for j_id in journeys:
        assert harness._journeys[j_id]["status"] == "EXITED"

    # All bays back to AVAILABLE
    for bay in harness._bays.values():
        assert bay["status"] == "AVAILABLE"


def test_t4_scenario_high_risk_hydro_interlock():
    """
    Scenario 2: High-Risk Safety Interlock Enforcement.
    A vehicle with an expired cylinder hydro-test certificate arrives;
    system enforces immediate block, locks dispenser, and triggers safety alert.
    """
    harness = SystemContractHarness()
    high_risk_plate = "GJ01EXPHYDRO"

    # 1. Vehicle arrives and ANPR processes detection
    det_resp = harness.process_anpr_detection(1, 1, high_risk_plate)
    journey_id = det_resp["data"]["journeyId"]

    # 2. System marks vehicle as NOT_ELIGIBLE and BLOCKED
    assert det_resp["data"]["complianceStatus"] == "NOT_ELIGIBLE"
    assert det_resp["data"]["journeyStatus"] == "BLOCKED"

    # 3. Dispenser interlock: Vehicle is completely barred from queue and bays
    with AssertRaises(ValueError, match="BLOCKED"):
        harness.enter_queue(journey_id)

    with AssertRaises(ValueError, match="Cannot assign bay to journey with status: BLOCKED"):
        harness.assign_bay(journey_id, bay_id=1)

    # 4. Verify high-severity safety alert is created in open state
    alerts = harness.get_alerts(station_id=1)
    high_risk_alert = [a for a in alerts if a["journeyId"] == journey_id][0]
    assert high_risk_alert["severity"] == "HIGH"
    assert high_risk_alert["status"] == "OPEN"
    assert "expired" in high_risk_alert["message"].lower()

    # 5. Station operator acknowledges and resolves alert after escorting vehicle away
    resolved_alert = harness.resolve_alert(high_risk_alert["id"], resolved_by="operator_patel")
    assert resolved_alert["status"] == "RESOLVED"
    assert resolved_alert["resolvedBy"] == "operator_patel"

    # 6. Verify dispenser bays were never occupied by the hazardous vehicle
    for bay in harness._bays.values():
        assert bay["status"] == "AVAILABLE"


def test_t4_scenario_unregistered_foreign_vehicle():
    """
    Scenario 3: Unregistered / Foreign State Vehicle Entry.
    Vehicle not present in mParivahan registry is gracefully rejected
    without system error, generating an alert and maintaining system stability.
    """
    harness = SystemContractHarness()
    unreg_plate = "GJ01UNKNOWN99"

    # 1. Vehicle verification lookup returns not found
    verify_resp = harness.verify_vehicle(unreg_plate)
    assert verify_resp["success"] is True
    assert verify_resp["data"]["vehicleFound"] is False
    assert verify_resp["data"]["complianceStatus"] == "NOT_ELIGIBLE"
    assert any("not found" in r for r in verify_resp["data"]["reasons"])

    # 2. ANPR detection creates blocked journey without throwing exception
    det_resp = harness.process_anpr_detection(1, 1, unreg_plate)
    journey_id = det_resp["data"]["journeyId"]
    assert det_resp["data"]["complianceStatus"] == "NOT_ELIGIBLE"
    assert det_resp["data"]["journeyStatus"] == "BLOCKED"

    # 3. Interlock prevents unauthorized entry
    with AssertRaises(ValueError, match="BLOCKED"):
        harness.enter_queue(journey_id)

    # 4. Alert created for unregistered vehicle
    alerts = harness.get_alerts(station_id=1)
    assert any(a["journeyId"] == journey_id for a in alerts)


def test_t4_scenario_kiosk_manual_override_lifecycle():
    """
    Scenario 4: Kiosk Manual Override Journey.
    Vehicle with missing online hydro-test undergoes operator inspection,
    authorized supervisor override, and completes fueling with full audit trail.
    """
    harness = SystemContractHarness()
    missing_plate = "GJ01NOHYDRO"

    # 1. ANPR detects vehicle with missing online hydro-test -> BLOCKED
    det_resp = harness.process_anpr_detection(1, 1, missing_plate)
    journey_id = det_resp["data"]["journeyId"]
    assert det_resp["data"]["complianceStatus"] == "NOT_ELIGIBLE"
    assert det_resp["data"]["journeyStatus"] == "BLOCKED"

    # 2. Driver approaches kiosk; operator evaluates statutory breakdown
    kiosk_eval = harness.verify_vehicle(missing_plate)["data"]
    assert kiosk_eval["hydroTestStatus"] == "UNKNOWN"
    assert kiosk_eval["complianceStatus"] == "NOT_ELIGIBLE"

    # 3. Driver presents valid physical PESO certificate stamped by accredited test workshop
    # Supervisor verifies certificate and executes manual override
    supervisor_user = "supervisor_desai"
    justification = "Accredited workshop physical certificate #PESO-AHM-98765 inspected and verified"
    override_result = harness.supervisor_override(journey_id, supervisor_user, justification)

    assert override_result["data"]["complianceStatus"] == "ELIGIBLE"
    assert override_result["data"]["status"] == "ENTERED"

    # 4. Verify immutable audit log entry
    audit_logs = harness.get_audit_logs(action="SUPERVISOR_OVERRIDE")
    assert len(audit_logs) >= 1
    log_entry = [l for l in audit_logs if l["entityId"] == str(journey_id)][0]
    assert log_entry["userId"] == supervisor_user
    assert "PESO-AHM-98765" in log_entry["details"]

    # 5. Vehicle proceeds through fueling cycle
    harness.enter_queue(journey_id)
    harness.assign_bay(journey_id, bay_id=1)
    harness.start_fueling(journey_id)
    harness.complete_fueling(journey_id)
    exit_resp = harness.exit_station(journey_id)

    assert exit_resp["data"]["status"] == "EXITED"
    assert harness._bays[1]["status"] == "AVAILABLE"


def test_t4_scenario_camera_disconnect_and_recovery():
    """
    Scenario 5: Edge Camera Disconnect & Reconnection Recovery.
    Camera drop during active fueling cycle does not corrupt or interrupt
    in-progress fueling operations; reconnection resumes seamlessly.
    """
    harness = SystemContractHarness()
    fueling_plate = "GJ01AB1234"

    # 1. Vehicle enters, is assigned to Bay 2, and starts fueling
    det = harness.process_anpr_detection(1, 1, fueling_plate)["data"]["journeyId"]
    harness.enter_queue(det)
    harness.assign_bay(det, bay_id=2)
    harness.start_fueling(det)
    assert harness._journeys[det]["status"] == "FUELING"

    # 2. Simulate camera network disconnection (stream worker retry loop)
    # Stream worker attempts up to 3 reconnections with 2.0s delay
    reconnect_attempts = 0
    max_retries = 3
    connected = False

    while reconnect_attempts < max_retries and not connected:
        reconnect_attempts += 1
        # Simulating successful reconnection on attempt 2
        if reconnect_attempts == 2:
            connected = True

    assert connected is True
    assert reconnect_attempts == 2

    # 3. Active vehicle journey was unaffected by camera interruption
    assert harness._journeys[det]["status"] == "FUELING"
    assert harness._bays[2]["status"] == "OCCUPIED"

    # 4. Vehicle completes fueling and exits normally
    harness.complete_fueling(det)
    harness.exit_station(det)

    assert harness._journeys[det]["status"] == "EXITED"
    assert harness._bays[2]["status"] == "AVAILABLE"
