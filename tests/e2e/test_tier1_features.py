"""
Tier 1 — Feature Coverage Test Suite.
Validates the primary functional behavior (happy path & standard workflows)
for:
1. AI ANPR Ingestion (Valid plates, confidence scoring, normalization, duplicate suppression, API dispatch)
2. Vehicle Compliance & RTO Engine (RTO lookup, fitness, registration validity, PESO hydro-test validity)
3. Operator Workflows (Queue entry, bay allocation, dispenser interlock, manual verification)
"""

import os
import sys
import time
from datetime import datetime, date

CURRENT_DIR = os.path.dirname(os.path.abspath(__file__))
if CURRENT_DIR not in sys.path:
    sys.path.insert(0, CURRENT_DIR)

try:
    from tests.e2e.harness import SystemContractHarness
except ModuleNotFoundError:
    from harness import SystemContractHarness


def test_t1_anpr_standard_plate_recognition():
    """T1.1: Standard Indian vehicle registration numbers correctly detected and parsed."""
    harness = SystemContractHarness()

    test_plates = [
        "GJ01AB1234",   # Standard Gujarat car
        "DL3CAA1111",   # Standard Delhi commercial
        "MH12DE1433",   # Standard Maharashtra car
        "KA05MJ9988",   # Standard Karnataka car
        "TN09AK7766"    # Standard Tamil Nadu car
    ]

    for plate in test_plates:
        is_valid, reason, normalized = harness.validate_plate_structure(plate)
        assert is_valid is True, f"Plate {plate} should have valid structure, got: {reason}"
        assert normalized == plate, f"Expected {plate}, got {normalized}"


def test_t1_anpr_character_normalization():
    """T1.2: OCR positional character substitutions and HSRP 'IND' prefix stripping."""
    harness = SystemContractHarness()

    # Case A: HSRP 'IND' prefix stripping
    raw_with_ind = "INDGJ01AB1234"
    normalized_ind = harness.normalize_plate(raw_with_ind)
    assert normalized_ind == "GJ01AB1234", f"Expected 'GJ01AB1234', got '{normalized_ind}'"

    # Case B: Digit '0' in state prefix corrected to letter 'O' (e.g. OD01AB1234)
    raw_state_num = "0D01AB1234"
    norm_state = harness.normalize_plate(raw_state_num)
    assert norm_state.startswith("OD"), f"Expected state prefix 'OD', got '{norm_state[:2]}'"

    # Case C: Letter 'O' in numeric RTO position corrected to digit '0'
    raw_rto_char = "GJ0IAB1234"  # 'I' in RTO code
    norm_rto = harness.normalize_plate(raw_rto_char)
    assert norm_rto[2:4] == "01", f"Expected RTO digits '01', got '{norm_rto[2:4]}'"

    # Case D: Letters in last 4 sequence corrected to digits
    raw_seq_chars = "GJ01ABIZ3S"  # 'I'->'1', 'Z'->'2', 'S'->'5'
    norm_seq = harness.normalize_plate(raw_seq_chars)
    assert norm_seq.endswith("1235"), f"Expected sequence digits '1235', got '{norm_seq[-4:]}'"


def test_t1_anpr_confidence_weighting():
    """T1.3: Confidence scoring algorithm weights detector (40%) and OCR (60%)."""
    harness = SystemContractHarness()

    # High detector (0.95), High OCR (0.90) -> (0.95*0.4) + (0.90*0.6) = 0.38 + 0.54 = 0.92
    conf1 = harness.calculate_confidence(0.95, 0.90)
    assert conf1 == 0.92, f"Expected 0.92, got {conf1}"
    assert conf1 >= 0.85, "Should satisfy default minimum confidence threshold (0.85)"

    # Moderate detector (0.80), High OCR (0.95) -> (0.80*0.4) + (0.95*0.6) = 0.32 + 0.57 = 0.89
    conf2 = harness.calculate_confidence(0.80, 0.95)
    assert conf2 == 0.89, f"Expected 0.89, got {conf2}"
    assert conf2 >= 0.85

    # Low OCR pulls score below threshold: 0.90 detector, 0.70 OCR -> 0.36 + 0.42 = 0.78 < 0.85
    conf3 = harness.calculate_confidence(0.90, 0.70)
    assert conf3 == 0.78, f"Expected 0.78, got {conf3}"
    assert conf3 < 0.85


def test_t1_anpr_duplicate_suppression_cooldown():
    """T1.4: Repetitive detections within 30-second cooldown window are suppressed."""
    harness = SystemContractHarness()
    plate = "GJ01AB1234"

    # First detection is not in cooldown
    in_cooldown_1 = harness.check_cooldown(plate, cooldown_seconds=30.0)
    assert in_cooldown_1 is False, "Initial detection should not be suppressed"

    # Immediate second detection is suppressed
    in_cooldown_2 = harness.check_cooldown(plate, cooldown_seconds=30.0)
    assert in_cooldown_2 is True, "Second immediate detection must be suppressed within 30s"

    # Immediate third detection is suppressed
    in_cooldown_3 = harness.check_cooldown(plate, cooldown_seconds=30.0)
    assert in_cooldown_3 is True, "Third immediate detection must be suppressed within 30s"


def test_t1_anpr_api_dispatch_contract():
    """T1.5: POST /api/v1/anpr/detections payload structure adheres to interface contract."""
    harness = SystemContractHarness()

    resp = harness.process_anpr_detection(
        station_id=1,
        cameraId=1,
        registration_number="GJ01AB1234",
        detected_at=datetime.now().isoformat()
    )

    assert resp["success"] is True
    assert "data" in resp
    data = resp["data"]
    assert "detectionId" in data and data["detectionId"] > 0
    assert "journeyId" in data and data["journeyId"] > 0
    assert data["registrationNumber"] == "GJ01AB1234"
    assert data["complianceStatus"] == "ELIGIBLE"
    assert data["journeyStatus"] == "ENTERED"
    assert data["duplicate"] is False


def test_t1_compliance_rto_lookup():
    """T1.6: Vehicle verification correctly retrieves RTO registration attributes."""
    harness = SystemContractHarness()

    result = harness.verify_vehicle("GJ01AB1234")
    assert result["success"] is True
    data = result["data"]

    assert data["registrationNumber"] == "GJ01AB1234"
    assert data["vehicleFound"] is True
    assert data["vehicleType"] is not None
    assert data["fuelType"] == "CNG"
    assert data["source"] in ("MOCK", "RAPIDAPI")


def test_t1_compliance_registration_validity():
    """T1.7: Registration status evaluated properly for active vs invalid plates."""
    harness = SystemContractHarness()

    # Valid vehicle
    res_valid = harness.verify_vehicle("GJ01AB1234")["data"]
    assert res_valid["registrationValid"] is True
    assert res_valid["complianceStatus"] == "ELIGIBLE"

    # Invalid vehicle (deterministic mock ending in 'INV')
    res_invalid = harness.verify_vehicle("GJ01AB9999INV")["data"]
    assert res_invalid["registrationValid"] is False
    assert res_invalid["complianceStatus"] == "NOT_ELIGIBLE"
    assert any("invalid or expired" in r for r in res_invalid["reasons"])


def test_t1_compliance_fitness_validity():
    """T1.8: Vehicle fitness validity based on registration expiry."""
    harness = SystemContractHarness()

    # Valid fitness
    res_valid = harness.verify_vehicle("GJ01AB1234")["data"]
    assert res_valid["fitnessValid"] is True

    # Expired fitness (deterministic mock ending in 9999)
    res_expired = harness.verify_vehicle("GJ01AB9999")["data"]
    assert res_expired["fitnessValid"] is False
    assert res_expired["complianceStatus"] == "NOT_ELIGIBLE"


def test_t1_compliance_peso_hydro_test_validity():
    """T1.9: PESO Rule 33/35 cylinder hydro-test validity evaluation."""
    harness = SystemContractHarness()

    # Valid cylinder hydro-test
    res_valid = harness.verify_vehicle("GJ01AB1234")["data"]
    assert res_valid["hydroTestStatus"] == "VALID"

    # Expired cylinder hydro-test
    res_expired = harness.verify_vehicle("GJ01EXPHYDRO")["data"]
    assert res_expired["hydroTestStatus"] == "EXPIRED"
    assert res_expired["complianceStatus"] == "NOT_ELIGIBLE"
    assert any("expired" in r for r in res_expired["reasons"])


def test_t1_compliance_decision_matrix():
    """T1.10: Decision matrix produces ELIGIBLE only when all safety checks pass."""
    harness = SystemContractHarness()

    # Fully compliant vehicle
    res = harness.verify_vehicle("GJ01AB1234")["data"]
    assert res["vehicleFound"] is True
    assert res["registrationValid"] is True
    assert res["fitnessValid"] is True
    assert res["hydroTestStatus"] == "VALID"
    assert res["complianceStatus"] == "ELIGIBLE"
    assert len(res["reasons"]) == 0


def test_t1_operator_queue_entry():
    """T1.11: Eligible vehicle transitions ENTERED -> IN_QUEUE with timestamp."""
    harness = SystemContractHarness()

    det = harness.process_anpr_detection(1, 1, "GJ01AB1234")
    journey_id = det["data"]["journeyId"]

    queue_resp = harness.enter_queue(journey_id)
    assert queue_resp["success"] is True
    journey = queue_resp["data"]

    assert journey["status"] == "IN_QUEUE"
    assert journey["queueEntryTime"] is not None
    assert any(e["eventType"] == "QUEUE_ENTERED" for e in journey["events"])


def test_t1_operator_queue_fifo_order():
    """T1.12: Station queue returns vehicles ordered strictly by queueEntryTime ASC."""
    harness = SystemContractHarness()

    # Ingest 3 distinct vehicles
    det1 = harness.process_anpr_detection(1, 1, "GJ01AA1001")["data"]["journeyId"]
    det2 = harness.process_anpr_detection(1, 1, "GJ01AA1002")["data"]["journeyId"]
    det3 = harness.process_anpr_detection(1, 1, "GJ01AA1003")["data"]["journeyId"]

    harness.enter_queue(det1)
    time.sleep(0.01)
    harness.enter_queue(det2)
    time.sleep(0.01)
    harness.enter_queue(det3)

    queue = harness.get_station_queue(1)
    assert len(queue) == 3
    assert queue[0]["id"] == det1
    assert queue[1]["id"] == det2
    assert queue[2]["id"] == det3


def test_t1_operator_bay_allocation():
    """T1.13: Allocates available bay: IN_QUEUE -> BAY_ASSIGNED, bay status -> OCCUPIED."""
    harness = SystemContractHarness()

    det = harness.process_anpr_detection(1, 1, "GJ01AB1234")["data"]["journeyId"]
    harness.enter_queue(det)

    assign_resp = harness.assign_bay(det, bay_id=1)
    assert assign_resp["success"] is True
    journey = assign_resp["data"]

    assert journey["status"] == "BAY_ASSIGNED"
    assert journey["assignedBayId"] == 1
    assert harness._bays[1]["status"] == "OCCUPIED"


def test_t1_operator_dispenser_interlock_fueling():
    """T1.14: Fueling cycle executes BAY_ASSIGNED -> FUELING -> FUELING_COMPLETED (releases bay)."""
    harness = SystemContractHarness()

    det = harness.process_anpr_detection(1, 1, "GJ01AB1234")["data"]["journeyId"]
    harness.enter_queue(det)
    harness.assign_bay(det, bay_id=1)

    # Start fueling
    start_resp = harness.start_fueling(det)
    assert start_resp["data"]["status"] == "FUELING"
    assert start_resp["data"]["fuelingStartTime"] is not None

    # Complete fueling
    comp_resp = harness.complete_fueling(det)
    assert comp_resp["data"]["status"] == "FUELING_COMPLETED"
    assert comp_resp["data"]["fuelingEndTime"] is not None
    # Interlock released: Bay 1 becomes AVAILABLE
    assert harness._bays[1]["status"] == "AVAILABLE"


def test_t1_operator_manual_verification():
    """T1.15: POST /api/v1/vehicles/verify returns statutory itemized breakdown for operator kiosk."""
    harness = SystemContractHarness()

    verify_resp = harness.verify_vehicle("GJ01AB1234")
    assert verify_resp["success"] is True
    data = verify_resp["data"]

    # Verify all statutory inspection checklist fields are populated
    assert "registrationValid" in data
    assert "fitnessValid" in data
    assert "insuranceValid" in data
    assert "pucValid" in data
    assert "hydroTestStatus" in data
    assert "complianceStatus" in data
    assert "reasons" in data
    assert "verifiedAt" in data
