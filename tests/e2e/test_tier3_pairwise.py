"""
Tier 3 — Cross-Feature Combinations (Pairwise Interaction) Test Suite.
Validates multi-feature integrations and cross-cutting behaviors:
1. ANPR Ingestion -> Vehicle Journey Creation -> Compliance Verification -> Queue Entry
2. Non-Compliant Vehicle Detection -> Dispenser Interlock Lock -> Safety Violation Alert
3. Manual Kiosk Lookup -> Operator Supervisor Override -> Immutable Audit Log Record
4. Bay Allocation Concurrency -> Interlock Conflict Prevention
5. Real-Time Telemetry Aggregation Across Vehicle Journey Transitions
"""

import os
import sys

CURRENT_DIR = os.path.dirname(os.path.abspath(__file__))
if CURRENT_DIR not in sys.path:
    sys.path.insert(0, CURRENT_DIR)

try:
    from tests.e2e.harness import SystemContractHarness, AssertRaises
except ModuleNotFoundError:
    from harness import SystemContractHarness, AssertRaises


def test_t3_pairwise_anpr_to_queue():
    """T3.1: ANPR detection -> Automatic Journey -> Compliance Evaluation -> Queue Entry."""
    harness = SystemContractHarness()
    plate = "GJ01AB1234"

    # Step 1: ANPR detection ingested
    det_resp = harness.process_anpr_detection(station_id=1, camera_id=1, registration_number=plate)
    assert det_resp["success"] is True
    journey_id = det_resp["data"]["journeyId"]
    assert det_resp["data"]["complianceStatus"] == "ELIGIBLE"
    assert det_resp["data"]["journeyStatus"] == "ENTERED"

    # Step 2: Journey created in backend with initial events
    journey = harness._journeys[journey_id]
    event_types = [e["eventType"] for e in journey["events"]]
    assert "ENTRY_DETECTED" in event_types
    assert "COMPLIANCE_APPROVED" in event_types

    # Step 3: Vehicle enters station queue
    queue_resp = harness.enter_queue(journey_id)
    assert queue_resp["data"]["status"] == "IN_QUEUE"
    assert queue_resp["data"]["queueEntryTime"] is not None

    # Step 4: Verify presence in active station queue
    station_queue = harness.get_station_queue(1)
    assert any(q["id"] == journey_id for q in station_queue)


def test_t3_pairwise_interlock_blocked_alert():
    """T3.2: Non-compliant vehicle detection -> Dispenser Interlock BLOCKED -> Safety Alert Created."""
    harness = SystemContractHarness()
    plate = "GJ01EXPHYDRO"  # Expired hydro-test

    # Step 1: Ingest non-compliant plate
    det_resp = harness.process_anpr_detection(station_id=1, camera_id=1, registration_number=plate)
    journey_id = det_resp["data"]["journeyId"]

    # Step 2: Verification fails, journey set to BLOCKED
    assert det_resp["data"]["complianceStatus"] == "NOT_ELIGIBLE"
    assert det_resp["data"]["journeyStatus"] == "BLOCKED"

    # Step 3: Dispenser interlock is locked (attempt to enter queue or assign bay fails)
    with AssertRaises(ValueError, match="BLOCKED"):
        harness.enter_queue(journey_id)

    # Step 4: Verify high-severity safety alert generated
    alerts = harness.get_alerts(station_id=1)
    matching_alerts = [a for a in alerts if a["journeyId"] == journey_id]
    assert len(matching_alerts) == 1
    alert = matching_alerts[0]
    assert alert["severity"] == "HIGH"
    assert alert["type"] == "COMPLIANCE_VIOLATION"
    assert alert["status"] == "OPEN"
    assert "expired" in alert["message"].lower()


def test_t3_pairwise_manual_lookup_supervisor_override():
    """T3.3: Manual lookup of non-compliant vehicle -> Supervisor Override -> Audit Log Record."""
    harness = SystemContractHarness()
    plate = "GJ01NOHYDRO"  # Missing hydro-test

    # Step 1: Ingest non-compliant detection
    det_resp = harness.process_anpr_detection(station_id=1, camera_id=1, registration_number=plate)
    journey_id = det_resp["data"]["journeyId"]
    assert det_resp["data"]["complianceStatus"] == "NOT_ELIGIBLE"

    # Step 2: Operator manually inspects vehicle at verification kiosk
    verify_resp = harness.verify_vehicle(plate)["data"]
    assert verify_resp["hydroTestStatus"] == "UNKNOWN"
    assert verify_resp["complianceStatus"] == "NOT_ELIGIBLE"

    # Step 3: Authorized supervisor inspects physical paper certificate and executes override
    override_reason = "Physical PESO certificate verified manually (Cert #PESO-2026-VAL)"
    override_resp = harness.supervisor_override(journey_id, "supervisor_shah", override_reason)

    assert override_resp["success"] is True
    assert override_resp["data"]["complianceStatus"] == "ELIGIBLE"
    assert override_resp["data"]["status"] == "ENTERED"

    # Step 4: Verify immutable audit log record created with supervisor user and reason
    audit_logs = harness.get_audit_logs(action="SUPERVISOR_OVERRIDE")
    assert len(audit_logs) >= 1
    override_log = [l for l in audit_logs if l["entityId"] == str(journey_id)][0]
    assert override_log["userId"] == "supervisor_shah"
    assert "Physical PESO certificate verified manually" in override_log["details"]

    # Step 5: Vehicle can now enter station queue for fueling
    queue_resp = harness.enter_queue(journey_id)
    assert queue_resp["data"]["status"] == "IN_QUEUE"


def test_t3_pairwise_bay_concurrency_interlock():
    """T3.4: Bay assigned to Vehicle A cannot be assigned to Vehicle B until released."""
    harness = SystemContractHarness()

    det_a = harness.process_anpr_detection(1, 1, "GJ01AA1001")["data"]["journeyId"]
    det_b = harness.process_anpr_detection(1, 1, "GJ01BB2002")["data"]["journeyId"]

    harness.enter_queue(det_a)
    harness.enter_queue(det_b)

    # Assign Bay 1 to Vehicle A -> Bay 1 becomes OCCUPIED
    harness.assign_bay(det_a, bay_id=1)
    assert harness._bays[1]["status"] == "OCCUPIED"

    # Attempt to assign Bay 1 to Vehicle B must be rejected
    with AssertRaises(ValueError, match="is currently OCCUPIED"):
        harness.assign_bay(det_b, bay_id=1)

    # Fuel Vehicle A and complete
    harness.start_fueling(det_a)
    harness.complete_fueling(det_a)

    # Bay 1 is now released to AVAILABLE
    assert harness._bays[1]["status"] == "AVAILABLE"

    # Now Vehicle B can be assigned to Bay 1
    harness.assign_bay(det_b, bay_id=1)
    assert harness._bays[1]["status"] == "OCCUPIED"


def test_t3_pairwise_station_telemetry_aggregation():
    """T3.5: Station command center telemetry metrics update dynamically with journey transitions."""
    harness = SystemContractHarness()

    # Initial state
    t0 = harness.get_dashboard(station_id=1)
    assert t0["queueLength"] == 0
    assert t0["occupiedBays"] == 0
    assert t0["availableBays"] == 6

    # 1 Compliant + 1 Blocked vehicle arrive
    det_ok = harness.process_anpr_detection(1, 1, "GJ01AB1234")["data"]["journeyId"]
    harness.process_anpr_detection(1, 1, "GJ01EXPHYDRO")

    t1 = harness.get_dashboard(station_id=1)
    assert t1["blockedJourneys"] == 1
    assert t1["activeAlerts"] == 1

    # Compliant vehicle enters queue
    harness.enter_queue(det_ok)
    t2 = harness.get_dashboard(station_id=1)
    assert t2["queueLength"] == 1

    # Compliant vehicle assigned to Bay 1
    harness.assign_bay(det_ok, bay_id=1)
    t3 = harness.get_dashboard(station_id=1)
    assert t3["queueLength"] == 0
    assert t3["occupiedBays"] == 1
    assert t3["availableBays"] == 5

    # Fueling completed -> bay released
    harness.start_fueling(det_ok)
    harness.complete_fueling(det_ok)
    t4 = harness.get_dashboard(station_id=1)
    assert t4["occupiedBays"] == 0
    assert t4["availableBays"] == 6
