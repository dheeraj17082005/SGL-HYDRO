# SGL Smart CNG Station System - End-to-End (E2E) Test Infrastructure

## 1. Overview & Objectives
The End-to-End (E2E) test harness for the Sabarmati Gas Limited (SGL) Smart CNG Station System (`sgl-smart-cng-station`) validates complete opaque-box functional workflows, statutory compliance rules, interlock safety mechanisms, and operator journeys across the entire system.

The test suite exercises the system's three primary tiers:
1. **Edge AI ANPR Ingestion Service (`sgl-cng-ai`)**: License plate detection, 6-stage image preprocessing, OCR recognition, Indian plate normalization, temporal stabilization, cooldown suppression, and backend API dispatch.
2. **Vehicle Compliance & RTO Engine (`cng-backend`)**: RapidAPI / mParivahan verification, PESO Rule 33/35 cylinder hydro-test validation, 9-stage vehicle journey state machine, FIFO station queue, dispenser interlock enforcement, alerts, and audit logging.
3. **Operator Workflows & Command Center (`cng frontend`)**: Dual-mode operations (Camera Simulation loop and Manual Registration Verification), 6-bay allocation grid, supervisor overrides, and telemetry.

---

## 2. 4-Tier Test Case Design Methodology

The E2E test suite adheres to a rigorous 4-Tier hierarchy ensuring full specification coverage, boundary resilience, multi-component integration, and realistic operational stability:

```
+--------------------------------------------------------------------------+
|                       Tier 4: Real-World Scenarios                       |
|   Fleet Peak Arrival | High-Risk Interlock | Unregistered Vehicle Entry  |
|         Supervisor Override Journey | Camera Disconnect Recovery         |
+--------------------------------------------------------------------------+
                                     ^
                                     |
+--------------------------------------------------------------------------+
|                  Tier 3: Cross-Feature (Pairwise) Combos                 |
|   ANPR -> Journey -> Queue | Non-Compliant -> Interlock -> Safety Alert  |
|      Supervisor Override -> Audit Log | Bay Allocation Concurrency       |
+--------------------------------------------------------------------------+
                                     ^
                                     |
+--------------------------------------------------------------------------+
|                   Tier 2: Boundary & Corner Cases                        |
|   Malformed / Empty Plates | 37 States & BH Series | Hydro-Test Dates    |
|       30s Cooldown Suppression | Out-of-Order Lifecycle Transitions      |
+--------------------------------------------------------------------------+
                                     ^
                                     |
+--------------------------------------------------------------------------+
|                      Tier 1: Feature Coverage                            |
|       AI ANPR Ingestion | Compliance & RTO Engine | Operator Workflows   |
+--------------------------------------------------------------------------+
```

### Tier 1 — Feature Coverage (15 Test Cases, >=5 per Feature Area)
- **Feature Area 1: AI ANPR Ingestion**
  - `test_t1_anpr_standard_plate_recognition`: Standard Indian license plates (e.g., `GJ01AB1234`, `DL3CAA1111`, `MH12DE1433`) correctly detected and extracted.
  - `test_t1_anpr_character_normalization`: Positional character corrections (state prefix letters, RTO digits, sequence digits) and HSRP `'IND'` stripping.
  - `test_t1_anpr_confidence_weighting`: Detector and OCR confidence weighting algorithm (`0.4 * det + 0.6 * ocr >= 0.85`).
  - `test_t1_anpr_duplicate_suppression_cooldown`: Re-reading identical plate within 30-second cooldown suppresses duplicate dispatch.
  - `test_t1_anpr_api_dispatch_contract`: `POST /api/v1/anpr/detections` payload dispatched with stationId, cameraId, registrationNumber, and ISO-8601 timestamp returning HTTP 201.

- **Feature Area 2: Vehicle Compliance & RTO Engine**
  - `test_t1_compliance_rto_lookup`: Retrieval and parsing of vehicle registration data (owner name, vehicle class, fuel type).
  - `test_t1_compliance_registration_validity`: Valid active registration evaluated as `VALID` vs suspended/invalid as `INVALID`.
  - `test_t1_compliance_fitness_validity`: Fitness certificate validation based on registration expiry date.
  - `test_t1_compliance_peso_hydro_test_validity`: PESO Gas Cylinder Rules 2016 Rule 33/35 validation (valid 3-year hydro-test cycle).
  - `test_t1_compliance_decision_matrix`: Decision matrix producing `ELIGIBLE` when all checks pass and itemized reasons when failing.

- **Feature Area 3: Operator Workflows**
  - `test_t1_operator_queue_entry`: `POST /api/v1/journeys/{id}/queue` transitions journey from `ENTERED` to `IN_QUEUE` and stamps `queueEntryTime`.
  - `test_t1_operator_queue_fifo_order`: `GET /api/v1/stations/{id}/queue` returns active queue ordered strictly by `queueEntryTime` ASC.
  - `test_t1_operator_bay_allocation`: `POST /api/v1/journeys/{id}/assign-bay` allocates available bay and transitions bay status to `OCCUPIED`.
  - `test_t1_operator_dispenser_interlock_fueling`: Dispenser interlock permits fueling start only after bay assignment and releases bay to `AVAILABLE` on completion.
  - `test_t1_operator_manual_verification`: `POST /api/v1/vehicles/verify` returns itemized statutory breakdown for operator manual verification.

---

### Tier 2 — Boundary & Corner Cases (15 Test Cases, >=5 per Feature Area)
- **Boundary Area 1: Input Syntax & Formatting**
  - `test_t2_syntax_empty_and_blank_plates`: Null, empty (`""`), and whitespace (`"   "`) inputs rejected with validation error or `NOT_ELIGIBLE`.
  - `test_t2_syntax_malformed_delimiters`: Plates with hyphens, irregular spacing, and lowercase characters (e.g., `" gj - 01 - ab - 1234 "`) normalized properly.
  - `test_t2_syntax_all_state_and_bh_prefixes`: Full coverage of all 37 Indian State/UT codes (`GJ`, `MH`, `DL`, `KA`, `TN`, `UP`, `HR`, `RJ`, etc.) plus Bharat (`BH`) series (`22BH1234AA`).
  - `test_t2_syntax_out_of_bounds_length`: Short plates (`<8` chars) and overly long plates (`>10` chars) correctly flagged and rejected.
  - `test_t2_syntax_special_characters`: Non-alphanumeric noise (e.g., `"GJ#01*AB$1234!"`) stripped cleanly or rejected if corrupt.

- **Boundary Area 2: Hydro-Test Date Boundaries**
  - `test_t2_hydro_missing_record_returns_unknown`: Missing cylinder hydro-test record MUST return `hydroTestStatus: "UNKNOWN"`, `complianceStatus: "NOT_ELIGIBLE"`, and explicit disqualification reason.
  - `test_t2_hydro_expired_date_boundaries`: Cylinder hydro-test expired 1 day ago or 1 month ago correctly classified as `EXPIRED` and `NOT_ELIGIBLE`.
  - `test_t2_hydro_future_valid_dates`: Cylinder hydro-test valid for 1, 2, or 3 years into the future classified as `VALID` and `ELIGIBLE`.
  - `test_t2_hydro_expiring_today`: Hydro-test certificate expiring on the current calendar date remains valid through end-of-day.
  - `test_t2_hydro_corrupt_certificate_data`: Certificates with missing metadata (null certificate number or missing issuing authority) safely handled.

- **Boundary Area 3: Temporal, Cooldown & State Transitions**
  - `test_t2_temporal_duplicate_detection_within_30s`: Rapid repeated detections of the same vehicle within 30-second cooldown window are suppressed or return existing active journey.
  - `test_t2_temporal_interleaved_vehicle_arrivals`: Distinct vehicles arriving within seconds of each other are tracked independently without cooldown collision.
  - `test_t2_temporal_cooldown_expiration`: Detection after 30+ seconds cooldown elapses initiates a fresh verification cycle.
  - `test_t2_transition_out_of_order_queue_and_bay`: Attempting to assign a bay or enter queue from an invalid/blocked state throws HTTP 400 `InvalidStateTransitionException`.
  - `test_t2_transition_out_of_order_fueling_and_exit`: Attempting to fuel before bay assignment, or exit before fueling completed, throws HTTP 400 `InvalidStateTransitionException`.

---

### Tier 3 — Cross-Feature Combinations / Pairwise Interaction (5 Test Cases)
- `test_t3_pairwise_anpr_to_queue`: ANPR camera detection automatically initializes backend vehicle journey, executes compliance verification, and enters station queue.
- `test_t3_pairwise_interlock_blocked_alert`: Non-compliant vehicle detection triggers dispenser interlock lock (`BLOCKED` status) and generates a `HIGH` severity `COMPLIANCE_VIOLATION` safety alert.
- `test_t3_pairwise_manual_lookup_supervisor_override`: Operator manual lookup of non-compliant vehicle followed by authorized supervisor override action creates immutable audit log entries.
- `test_t3_pairwise_bay_concurrency_interlock`: Bay allocated to Vehicle A cannot be concurrently assigned to Vehicle B; dispenser interlock prevents duplicate occupancy.
- `test_t3_pairwise_station_telemetry_aggregation`: Real-time station telemetry updates dynamically as vehicles transition through queue, bay fueling, and station exit.

---

### Tier 4 — Real-World Operational Scenarios (5 Test Cases)
- `test_t4_scenario_peak_hour_fleet_arrival`: 10 commercial fleet vehicles arrive during peak hour; system maintains strict FIFO queue ordering, schedules vehicles across 6 fueling bays, executes fueling start/complete cycles, and records station exits.
- `test_t4_scenario_high_risk_hydro_interlock`: High-risk vehicle with expired PESO cylinder hydro-test attempts entry; immediate interlock prevents queue/bay assignment, triggers high-priority safety alert, and blocks dispensing.
- `test_t4_scenario_unregistered_foreign_vehicle`: Unregistered / foreign state vehicle arrives; RTO database lookup returns unverified; system handles the unknown state gracefully without crash or 500 error, logs alert, and rejects fueling.
- `test_t4_scenario_kiosk_manual_override_lifecycle`: Vehicle flagged as non-compliant at entry undergoes physical kiosk inspection; operator reviews itemized failure reasons; authorized station supervisor executes verified override; vehicle completes fueling with full audit trail.
- `test_t4_scenario_camera_disconnect_and_recovery`: Edge camera connection drops during active fueling cycle; stream worker reconnects automatically; active vehicle journey and fueling operations remain completely intact and undisrupted.

---

## 3. Directory & File Structure
```
tests/e2e/
├── __init__.py                # Package marker
├── harness.py                 # E2E Test Harness & dual-mode client (Live HTTP & Contract Simulation)
├── test_tier1_features.py     # Tier 1: Feature Coverage (15 tests)
├── test_tier2_boundaries.py   # Tier 2: Boundary & Corner Cases (15 tests)
├── test_tier3_pairwise.py     # Tier 3: Cross-Feature Interactions (5 tests)
├── test_tier4_scenarios.py    # Tier 4: Real-World Scenarios (5 tests)
└── run_e2e_tests.py           # Automated Test Runner CLI with structured output & exit code 0
```

---

## 4. Test Execution & Usage

### Standard Invocation
```bash
python3 tests/e2e/run_e2e_tests.py
```
*(Also fully compatible with `./sgl-cng-ai/.venv/bin/python tests/e2e/run_e2e_tests.py`)*

### Command-Line Options
| Option | Description | Example |
|---|---|---|
| `--tier <1-4>` | Execute specific test tier only | `python3 tests/e2e/run_e2e_tests.py --tier 1` |
| `-v`, `--verbose` | Enable detailed test step logging | `python3 tests/e2e/run_e2e_tests.py -v` |
| `--live` | Force live HTTP requests against running services | `python3 tests/e2e/run_e2e_tests.py --live` |
| `--backend-url <url>` | Override backend URL (default `http://localhost:8080`) | `python3 tests/e2e/run_e2e_tests.py --backend-url http://127.0.0.1:8080` |
| `--ai-url <url>` | Override AI service URL (default `http://localhost:8000`) | `python3 tests/e2e/run_e2e_tests.py --ai-url http://127.0.0.1:8000` |
| `--json-output <path>` | Export structured test results to JSON file | `python3 tests/e2e/run_e2e_tests.py --json-output results.json` |

---

## 5. Traceability Matrix

| Requirement | Description | E2E Test Cases |
|---|---|---|
| **R1** | AI ANPR & Stream Ingestion | `test_t1_anpr_*`, `test_t2_syntax_*`, `test_t2_temporal_*`, `test_t3_pairwise_anpr_to_queue`, `test_t4_scenario_camera_disconnect_and_recovery` |
| **R2** | Vehicle Compliance & RTO Engine | `test_t1_compliance_*`, `test_t2_hydro_*`, `test_t3_pairwise_interlock_blocked_alert`, `test_t4_scenario_high_risk_hydro_interlock`, `test_t4_scenario_unregistered_foreign_vehicle` |
| **R3** | Operator UI & Workflows | `test_t1_operator_*`, `test_t2_transition_*`, `test_t3_pairwise_manual_lookup_supervisor_override`, `test_t3_pairwise_bay_concurrency_interlock`, `test_t4_scenario_kiosk_manual_override_lifecycle` |
| **R4** | End-to-End Reliability & Scenarios | `test_t4_scenario_peak_hour_fleet_arrival`, `test_t4_scenario_*`, `run_e2e_tests.py` multi-tier automated test suite |
