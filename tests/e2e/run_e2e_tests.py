#!/usr/bin/env python3
"""
Automated E2E Test Runner for SGL Smart CNG Station System.

Executes all 4 test tiers:
- Tier 1: Feature Coverage (15 tests: AI ANPR, Compliance & RTO, Operator Workflows)
- Tier 2: Boundary & Corner Cases (15 tests: Syntax/Prefixes, Hydro Dates, Temporal/Cooldown/Transitions)
- Tier 3: Cross-Feature Combinations (5 tests: Pairwise interactions)
- Tier 4: Real-World Operational Scenarios (5 tests: Commercial Fleet, Interlock, Unregistered, Override, Disconnect)

Usage:
    python3 tests/e2e/run_e2e_tests.py
    python3 tests/e2e/run_e2e_tests.py --tier 1
    python3 tests/e2e/run_e2e_tests.py --verbose
    python3 tests/e2e/run_e2e_tests.py --json-output e2e_results.json
"""

import sys
import os
import time
import argparse
import json
import traceback
from typing import List, Dict, Any, Callable

CURRENT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.abspath(os.path.join(CURRENT_DIR, "..", ".."))
if CURRENT_DIR not in sys.path:
    sys.path.insert(0, CURRENT_DIR)
if PROJECT_ROOT not in sys.path:
    sys.path.insert(0, PROJECT_ROOT)

# Import test suites
try:
    import tests.e2e.test_tier1_features as tier1
    import tests.e2e.test_tier2_boundaries as tier2
    import tests.e2e.test_tier3_pairwise as tier3
    import tests.e2e.test_tier4_scenarios as tier4
except ModuleNotFoundError:
    import test_tier1_features as tier1
    import test_tier2_boundaries as tier2
    import test_tier3_pairwise as tier3
    import test_tier4_scenarios as tier4


TIER_DEFINITIONS = {
    1: {
        "name": "Tier 1: Feature Coverage",
        "description": "Primary happy path behavior across AI ANPR Ingestion, Vehicle Compliance, and Operator Workflows",
        "module": tier1,
        "tests": [
            ("test_t1_anpr_standard_plate_recognition", "ANPR Standard Plate Recognition (GJ, DL, MH, KA, TN)"),
            ("test_t1_anpr_character_normalization", "ANPR Positional Correction & 'IND' Prefix Stripping"),
            ("test_t1_anpr_confidence_weighting", "ANPR Confidence Weighting (40% detector + 60% OCR >= 0.85)"),
            ("test_t1_anpr_duplicate_suppression_cooldown", "ANPR Cooldown Duplicate Suppression within 30s"),
            ("test_t1_anpr_api_dispatch_contract", "ANPR POST /api/v1/anpr/detections Ingestion Contract"),
            ("test_t1_compliance_rto_lookup", "RTO Vehicle Lookup & Attribute Extraction"),
            ("test_t1_compliance_registration_validity", "Registration Validity (Active vs Invalid/Suspended)"),
            ("test_t1_compliance_fitness_validity", "Vehicle Fitness Certificate Date Validation"),
            ("test_t1_compliance_peso_hydro_test_validity", "PESO Cylinder Hydro-Test Validity (Rule 33/35)"),
            ("test_t1_compliance_decision_matrix", "Compliance Decision Matrix (All Valid -> ELIGIBLE)"),
            ("test_t1_operator_queue_entry", "Operator Queue Entry (ENTERED -> IN_QUEUE)"),
            ("test_t1_operator_queue_fifo_order", "Station Queue FIFO Sequencing (queueEntryTime ASC)"),
            ("test_t1_operator_bay_allocation", "Fueling Bay Allocation (IN_QUEUE -> BAY_ASSIGNED)"),
            ("test_t1_operator_dispenser_interlock_fueling", "Dispenser Interlock & Fueling Lifecycle Completion"),
            ("test_t1_operator_manual_verification", "Operator Manual Registration Verification Breakdown")
        ]
    },
    2: {
        "name": "Tier 2: Boundary & Corner Cases",
        "description": "Boundary conditions, syntax variants, date limits, and invalid transition handling",
        "module": tier2,
        "tests": [
            ("test_t2_syntax_empty_and_blank_plates", "Input Syntax: Empty, Blank, and Whitespace Strings"),
            ("test_t2_syntax_malformed_delimiters", "Input Syntax: Hyphens, Irregular Spaces & Lowercase"),
            ("test_t2_syntax_all_state_and_bh_prefixes", "Input Syntax: All 37 State/UT Codes + Bharat (BH) Series"),
            ("test_t2_syntax_out_of_bounds_length", "Input Syntax: Out-of-Bounds Plate Length (<8 or >10 chars)"),
            ("test_t2_syntax_special_characters", "Input Syntax: Special Characters & Non-Alphanumeric Noise"),
            ("test_t2_hydro_missing_record_returns_unknown", "PESO Boundary: Missing Hydro-Test MUST return UNKNOWN & NOT_ELIGIBLE"),
            ("test_t2_hydro_expired_date_boundaries", "PESO Boundary: Expired Hydro-Test Date Classification"),
            ("test_t2_hydro_future_valid_dates", "PESO Boundary: Future Valid Hydro-Test Dates (1-3 years)"),
            ("test_t2_hydro_expiring_today", "PESO Boundary: Hydro-Test Expiring Exactly Today"),
            ("test_t2_hydro_corrupt_certificate_data", "PESO Boundary: Missing/Corrupt Certificate Metadata"),
            ("test_t2_temporal_duplicate_detection_within_30s", "Temporal Boundary: Rapid Re-detections within 30s Cooldown"),
            ("test_t2_temporal_interleaved_vehicle_arrivals", "Temporal Boundary: Rapid Interleaved Multi-Vehicle Arrivals"),
            ("test_t2_temporal_cooldown_expiration", "Temporal Boundary: Detection After Cooldown Expiration (30s+)"),
            ("test_t2_transition_out_of_order_queue_and_bay", "State Boundary: Blocked Queue Entry & Premature Bay Allocation"),
            ("test_t2_transition_out_of_order_fueling_and_exit", "State Boundary: Fueling Before Bay & Exit Before Fueling")
        ]
    },
    3: {
        "name": "Tier 3: Cross-Feature Combinations",
        "description": "Pairwise feature interactions and cross-cutting integration guarantees",
        "module": tier3,
        "tests": [
            ("test_t3_pairwise_anpr_to_queue", "Pairwise: ANPR Detection -> Journey Creation -> Compliance -> Queue Entry"),
            ("test_t3_pairwise_interlock_blocked_alert", "Pairwise: Non-Compliant Vehicle -> Dispenser Interlock BLOCKED -> Safety Alert"),
            ("test_t3_pairwise_manual_lookup_supervisor_override", "Pairwise: Manual Lookup -> Supervisor Override -> Audit Log Trail"),
            ("test_t3_pairwise_bay_concurrency_interlock", "Pairwise: Bay Allocation Concurrency & Duplicate Occupancy Prevention"),
            ("test_t3_pairwise_station_telemetry_aggregation", "Pairwise: Real-Time Telemetry Dynamic Aggregation Across Lifecycle")
        ]
    },
    4: {
        "name": "Tier 4: Real-World Operational Scenarios",
        "description": "Complex end-to-end operational scenarios simulating real CNG station workloads",
        "module": tier4,
        "tests": [
            ("test_t4_scenario_peak_hour_fleet_arrival", "Scenario 1: Peak Hour Commercial Fleet Arrival (10 vehicles, 6 bays, FIFO)"),
            ("test_t4_scenario_high_risk_hydro_interlock", "Scenario 2: High-Risk Safety Interlock Enforcement (Expired Hydro-Test)"),
            ("test_t4_scenario_unregistered_foreign_vehicle", "Scenario 3: Unregistered / Foreign State Vehicle Graceful Handling"),
            ("test_t4_scenario_kiosk_manual_override_lifecycle", "Scenario 4: Kiosk Manual Verification & Supervisor Override Lifecycle"),
            ("test_t4_scenario_camera_disconnect_and_recovery", "Scenario 5: Edge Camera Disconnect & Recovery During Fueling Cycle")
        ]
    }
}


def run_single_test(test_fn: Callable) -> Tuple[bool, float, Optional[str]]:
    """Runs a single test function and captures execution duration and errors."""
    start_time = time.time()
    try:
        test_fn()
        duration = time.time() - start_time
        return True, duration, None
    except (KeyboardInterrupt, SystemExit):
        raise
    except BaseException as e:
        duration = time.time() - start_time
        error_detail = traceback.format_exc()
        return False, duration, error_detail


def execute_test_suite(selected_tier: Optional[int] = None, verbose: bool = False) -> Dict[str, Any]:
    """Executes the selected tiers and returns a structured result dictionary."""
    tiers_to_run = [selected_tier] if selected_tier else [1, 2, 3, 4]

    print("\n" + "=" * 80)
    print("  SGL SMART CNG STATION SYSTEM - AUTOMATED E2E TEST RUNNER")
    print("=" * 80)
    print("  Mode       : Opaque-Box Functional & Contract Verification")
    print("  Target     : REST APIs, PESO Gas Cylinder Rules 2016, Edge AI ANPR")
    print("  Scope      : Requirements R1, R2, R3, R4 | Interface Contracts in PROJECT.md")
    print("=" * 80 + "\n")

    overall_start = time.time()
    all_results = []
    tier_summaries = {}

    total_passed = 0
    total_failed = 0

    for tier_num in tiers_to_run:
        tier_def = TIER_DEFINITIONS[tier_num]
        print(f"--- [RUNNING] {tier_def['name']} ---")
        print(f"    Description: {tier_def['description']}\n")

        tier_module = tier_def["module"]
        tier_tests = tier_def["tests"]
        tier_passed = 0
        tier_failed = 0
        tier_results = []

        for fn_name, display_name in tier_tests:
            test_fn = getattr(tier_module, fn_name, None)
            if not test_fn:
                print(f"  [ERROR] Test function {fn_name} not found in {tier_module.__name__}!")
                tier_failed += 1
                total_failed += 1
                tier_results.append({
                    "name": fn_name,
                    "displayName": display_name,
                    "passed": False,
                    "duration": 0.0,
                    "error": "Function not found"
                })
                continue

            passed, duration, error = run_single_test(test_fn)

            if passed:
                tier_passed += 1
                total_passed += 1
                status_icon = "[PASS]"
                color_code = "\033[92m"  # Green
            else:
                tier_failed += 1
                total_failed += 1
                status_icon = "[FAIL]"
                color_code = "\033[91m"  # Red

            reset_code = "\033[0m"
            print(f"  {color_code}{status_icon}{reset_code} {display_name} ({duration * 1000:.1f}ms)")

            if not passed and verbose and error:
                print(f"         Error: {error.strip().splitlines()[-1]}")

            tier_results.append({
                "name": fn_name,
                "displayName": display_name,
                "passed": passed,
                "duration": round(duration, 4),
                "error": error
            })

        tier_summaries[tier_num] = {
            "name": tier_def["name"],
            "total": len(tier_tests),
            "passed": tier_passed,
            "failed": tier_failed,
            "results": tier_results
        }
        all_results.extend(tier_results)
        print(f"\n  Tier {tier_num} Summary: {tier_passed}/{len(tier_tests)} Passed (Failed: {tier_failed})\n")

    overall_duration = time.time() - overall_start

    # Final Rollup Report
    print("=" * 80)
    print("  E2E TEST SUITE EXECUTION SUMMARY")
    print("=" * 80)
    for tier_num, summary in tier_summaries.items():
        status = "PASSED" if summary["failed"] == 0 else "FAILED"
        print(f"  * {summary['name']:<42} : {summary['passed']}/{summary['total']} passed [{status}]")

    print("-" * 80)
    total_tests = total_passed + total_failed
    print(f"  Total Test Cases : {total_tests}")
    print(f"  Passed           : {total_passed}")
    print(f"  Failed           : {total_failed}")
    print(f"  Total Duration   : {overall_duration:.2f} seconds")
    print("=" * 80)

    if total_failed == 0:
        print("\033[92m  >>> ALL E2E TESTS PASSED SUCCESSFULLY! (Exit Code 0) <<<\033[0m\n")
    else:
        print(f"\033[91m  >>> {total_failed} TESTS FAILED! (Exit Code 1) <<<\033[0m\n")

    return {
        "timestamp": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "totalTests": total_tests,
        "totalPassed": total_passed,
        "totalFailed": total_failed,
        "durationSeconds": round(overall_duration, 3),
        "allPassed": (total_failed == 0),
        "tiers": tier_summaries
    }


def main():
    parser = argparse.ArgumentParser(description="Run SGL Smart CNG Station E2E Test Suite")
    parser.add_argument("--tier", type=int, choices=[1, 2, 3, 4], help="Run only specific tier (1, 2, 3, or 4)")
    parser.add_argument("-v", "--verbose", action="store_true", help="Print detailed error tracebacks")
    parser.add_argument("--json-output", type=str, help="Save structured results to JSON file")
    parser.add_argument("--live", action="store_true", help="Execute against running live REST services")
    parser.add_argument("--backend-url", type=str, default="http://localhost:8080", help="Live backend URL")

    args = parser.parse_args()

    results = execute_test_suite(selected_tier=args.tier, verbose=args.verbose)

    if args.json_output:
        try:
            with open(args.json_output, "w") as f:
                json.dump(results, f, indent=2)
            print(f"  Structured test results exported to: {args.json_output}\n")
        except Exception as e:
            print(f"  Failed to write JSON output: {e}")

    sys.exit(0 if results["allPassed"] else 1)


if __name__ == "__main__":
    main()
