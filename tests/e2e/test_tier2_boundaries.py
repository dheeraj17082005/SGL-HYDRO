"""
Tier 2 — Boundary & Corner Cases Test Suite.
Validates boundary values, malformed inputs, edge conditions, and state violations for:
1. Input Syntax & Formatting (Empty inputs, malformed delimiters, all 37 Indian states/UTs + BH series, length limits, special chars)
2. Hydro-Test Date Boundaries (Missing hydro-test -> UNKNOWN & NOT_ELIGIBLE, expired, future valid, expiring today, corrupt metadata)
3. Temporal & Deduplication (30s cooldown duplicate suppression, interleaved arrivals, cooldown expiration, out-of-order state transitions)
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


def test_t2_syntax_empty_and_blank_plates():
    """T2.1: Empty, blank, and whitespace-only registration inputs rejected gracefully."""
    harness = SystemContractHarness()

    for invalid_input in ("", "   ", "\t\n", None):
        is_valid, reason, norm = harness.validate_plate_structure(invalid_input)
        assert is_valid is False
        assert "EMPTY" in reason or "LENGTH" in reason

        # Verify API endpoint also handles empty registration gracefully
        resp = harness.verify_vehicle(invalid_input)
        assert resp["success"] is True
        assert resp["data"]["complianceStatus"] == "NOT_ELIGIBLE"
        assert any("invalid or empty" in r for r in resp["data"]["reasons"])


def test_t2_syntax_malformed_delimiters():
    """T2.2: Malformed registration syntax with hyphens, irregular spaces, and lowercase."""
    harness = SystemContractHarness()

    malformed_variants = [
        ("GJ-01-AB-1234", "GJ01AB1234"),
        (" gj - 01 - ab - 1234 ", "GJ01AB1234"),
        ("gj01ab1234", "GJ01AB1234"),
        ("DL--03--CA--1111", "DL03CA1111"),
        ("mh 12  de   1433", "MH12DE1433")
    ]

    for raw, expected in malformed_variants:
        norm = harness.normalize_plate(raw)
        assert norm == expected, f"Raw '{raw}' should normalize to '{expected}', got '{norm}'"
        is_valid, reason, _ = harness.validate_plate_structure(raw)
        assert is_valid is True, f"Normalized variant of '{raw}' should be valid, failed with: {reason}"


def test_t2_syntax_all_state_and_bh_prefixes():
    """T2.3: Structural validation across all 37 Indian States/UTs plus Bharat (BH) series."""
    harness = SystemContractHarness()

    all_states_and_uts = [
        "AN", "AP", "AR", "AS", "BR", "CG", "CH", "DD", "DL", "DN", "GA",
        "GJ", "HR", "HP", "JH", "JK", "KA", "KL", "LA", "LD", "MH", "ML",
        "MN", "MP", "MZ", "NL", "OD", "OR", "PB", "PY", "RJ", "SK", "TN",
        "TR", "TS", "UK", "UP", "WB"
    ]

    # Verify standard state prefixes (e.g. XX01AB1234)
    for code in all_states_and_uts:
        sample_plate = f"{code}01AB1234"
        is_valid, reason, norm = harness.validate_plate_structure(sample_plate)
        assert is_valid is True, f"State code '{code}' failed validation with: {reason}"

    # Verify Bharat (BH) series (e.g. 22BH1234AA or BH01AB1234)
    sample_bh = "BH01AB1234"
    is_valid_bh, reason_bh, _ = harness.validate_plate_structure(sample_bh)
    assert is_valid_bh is True, f"BH series failed validation with: {reason_bh}"


def test_t2_syntax_out_of_bounds_length():
    """T2.4: Out-of-bounds registration length rejected (<8 chars or >10 chars)."""
    harness = SystemContractHarness()

    # Too short (< 8 chars)
    short_plates = ["GJ", "GJ01", "GJ01A", "GJ01AB1"]
    for plate in short_plates:
        is_valid, reason, _ = harness.validate_plate_structure(plate)
        assert is_valid is False
        assert "LENGTH" in reason

    # Too long (> 10 chars)
    long_plates = ["GJ01AB12345", "GJ01ABCD1234", "MH12DE14339999"]
    for plate in long_plates:
        is_valid, reason, _ = harness.validate_plate_structure(plate)
        assert is_valid is False
        assert "LENGTH" in reason or "PATTERN" in reason


def test_t2_syntax_special_characters():
    """T2.5: Special characters and non-alphanumeric noise stripped cleanly."""
    harness = SystemContractHarness()

    noisy_samples = [
        ("GJ#01*AB$1234!", "GJ01AB1234"),
        ("MH@12/DE-1433", "MH12DE1433"),
        ("DL.03.CA.1111", "DL03CA1111")
    ]

    for noisy, expected in noisy_samples:
        norm = harness.normalize_plate(noisy)
        assert norm == expected, f"Expected '{expected}', got '{norm}'"
        is_valid, reason, _ = harness.validate_plate_structure(noisy)
        assert is_valid is True


def test_t2_hydro_missing_record_returns_unknown():
    """T2.6: Missing cylinder hydro-test MUST return UNKNOWN status and NOT_ELIGIBLE (Constraint 5)."""
    harness = SystemContractHarness()

    # Plate specifically simulating missing hydro-test
    missing_plate = "GJ01NOHYDRO"
    resp = harness.verify_vehicle(missing_plate)
    data = resp["data"]

    # Statutory safety rule assertion:
    assert data["hydroTestStatus"] == "UNKNOWN", f"Expected UNKNOWN hydro status, got: {data['hydroTestStatus']}"
    assert data["complianceStatus"] == "NOT_ELIGIBLE", f"Expected NOT_ELIGIBLE, got: {data['complianceStatus']}"
    assert any("missing or unverified" in r for r in data["reasons"]), f"Expected missing reason in {data['reasons']}"


def test_t2_hydro_expired_date_boundaries():
    """T2.7: Expired hydro-test certificate classified as EXPIRED and NOT_ELIGIBLE."""
    harness = SystemContractHarness()

    expired_plate = "GJ01EXPHYDRO"
    resp = harness.verify_vehicle(expired_plate)
    data = resp["data"]

    assert data["hydroTestStatus"] == "EXPIRED"
    assert data["complianceStatus"] == "NOT_ELIGIBLE"
    assert any("expired" in r for r in data["reasons"])


def test_t2_hydro_future_valid_dates():
    """T2.8: Hydro-test certificate valid 1 to 3 years into future classified as VALID and ELIGIBLE."""
    harness = SystemContractHarness()

    valid_plate = "GJ01AB1234"
    resp = harness.verify_vehicle(valid_plate)
    data = resp["data"]

    assert data["hydroTestStatus"] == "VALID"
    assert data["complianceStatus"] == "ELIGIBLE"
    assert len(data["reasons"]) == 0


def test_t2_hydro_expiring_today():
    """T2.9: Hydro-test expiring on current calendar date remains valid through end-of-day."""
    harness = SystemContractHarness()

    today_plate = "GJ01TODAY"
    resp = harness.verify_vehicle(today_plate)
    data = resp["data"]

    # Boundary rule: not strictly before today, valid until midnight
    assert data["hydroTestStatus"] == "VALID"
    assert data["complianceStatus"] == "ELIGIBLE"


def test_t2_hydro_corrupt_certificate_data():
    """T2.10: Unregistered plate with null/corrupt certificate records handled safely without crash."""
    harness = SystemContractHarness()

    unknown_plate = "GJ01UNKNOWN"
    resp = harness.verify_vehicle(unknown_plate)
    data = resp["data"]

    assert data["vehicleFound"] is False
    assert data["complianceStatus"] == "NOT_ELIGIBLE"
    assert any("not found" in r for r in data["reasons"])


def test_t2_temporal_duplicate_detection_within_30s():
    """T2.11: Rapid repeated detections within 30-second cooldown marked as duplicate."""
    harness = SystemContractHarness()
    plate = "GJ01AB1234"

    # First detection creates journey
    det1 = harness.process_anpr_detection(1, 1, plate)
    assert det1["data"]["duplicate"] is False
    journey_id_1 = det1["data"]["journeyId"]

    # Second detection within seconds returns duplicate=True and existing journeyId
    det2 = harness.process_anpr_detection(1, 1, plate)
    assert det2["data"]["duplicate"] is True
    assert det2["data"]["journeyId"] == journey_id_1
    assert "Duplicate ANPR detection" in det2["message"]


def test_t2_temporal_interleaved_vehicle_arrivals():
    """T2.12: Interleaved arrivals of different vehicles within seconds handled independently."""
    harness = SystemContractHarness()

    plate_a = "GJ01AA1111"
    plate_b = "GJ01BB2222"

    det_a1 = harness.process_anpr_detection(1, 1, plate_a)
    det_b1 = harness.process_anpr_detection(1, 1, plate_b)
    det_a2 = harness.process_anpr_detection(1, 1, plate_a)

    assert det_a1["data"]["duplicate"] is False
    assert det_b1["data"]["duplicate"] is False
    assert det_a2["data"]["duplicate"] is True  # duplicate of plate_a, not impacted by plate_b
    assert det_a2["data"]["journeyId"] == det_a1["data"]["journeyId"]


def test_t2_temporal_cooldown_expiration():
    """T2.13: Detection after 30+ seconds cooldown elapses initiates fresh detection."""
    harness = SystemContractHarness()
    plate = "GJ01AB1234"

    # With a 0.05s test cooldown window
    in_cd_1 = harness.check_cooldown(plate, cooldown_seconds=0.05)
    assert in_cd_1 is False

    # Within cooldown
    in_cd_2 = harness.check_cooldown(plate, cooldown_seconds=0.05)
    assert in_cd_2 is True

    # After cooldown expires
    time.sleep(0.06)
    in_cd_3 = harness.check_cooldown(plate, cooldown_seconds=0.05)
    assert in_cd_3 is False, "Detection after cooldown expiration should be allowed"


def test_t2_transition_out_of_order_queue_and_bay():
    """T2.14: Attempting to assign bay or enter queue out of sequence throws state exception."""
    harness = SystemContractHarness()

    # Blocked vehicle cannot enter queue
    det_blocked = harness.process_anpr_detection(1, 1, "GJ01EXPHYDRO")["data"]["journeyId"]
    with AssertRaises(ValueError, match="BLOCKED"):
        harness.enter_queue(det_blocked)

    # Compliant vehicle cannot be assigned bay before entering queue
    det_valid = harness.process_anpr_detection(1, 1, "GJ01AB1234")["data"]["journeyId"]
    with AssertRaises(ValueError, match="Cannot assign bay to journey with status: ENTERED"):
        harness.assign_bay(det_valid, bay_id=1)


def test_t2_transition_out_of_order_fueling_and_exit():
    """T2.15: Attempting to fuel before bay assignment, or exit before fueling, throws exception."""
    harness = SystemContractHarness()

    det = harness.process_anpr_detection(1, 1, "GJ01AB1234")["data"]["journeyId"]
    harness.enter_queue(det)

    # Attempt start fueling while still in IN_QUEUE status
    with AssertRaises(ValueError, match="Cannot start fueling for journey with status: IN_QUEUE"):
        harness.start_fueling(det)

    # Assign bay
    harness.assign_bay(det, bay_id=1)

    # Attempt to exit station before fueling completed
    with AssertRaises(ValueError, match="Cannot exit station from journey status: BAY_ASSIGNED"):
        harness.exit_station(det)

