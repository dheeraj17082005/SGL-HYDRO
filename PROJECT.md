# Project: SGL Smart CNG Station System (`sgl-smart-cng-station`)

## Architecture
The SGL Smart CNG Station System is an automated vehicle journey and compliance platform comprising three primary tiers:
1. **Edge AI ANPR Ingestion Service (`sgl-cng-ai`)**:
   - Python 3.14 / FastAPI service.
   - YOLOv8 plate detector (`license_plate_yolov8n.pt`) with 2-level multi-scale detection, adaptive padding, and OpenCV morphological fallback.
   - EasyOCR engine with 6-stage image preprocessing candidate selection.
   - Indian license plate normalization (37 state/UT codes, HSRP 'IND' stripping, positional character correction, strict regex validation).
   - Sliding-window temporal stabilization (2.0s, 3 stable frames) and cooldown duplicate suppression (30s).
   - Ingests camera streams (RTSP / webcam) and dispatches detections to Spring Boot backend via `POST /api/v1/anpr/detections`.
2. **Vehicle Compliance & RTO Engine (`cng-backend`)**:
   - Spring Boot 3.2.4 with Java 21, Spring Data JPA, Flyway migrations (V1-V4), and stateless JJWT authentication.
   - Production PostgreSQL 15 database; in-memory H2 database for test execution.
   - Integration with external RTO via RapidAPI (`RapidApiVehicleRegistrationClient`) and local deterministic simulation (`MockVehicleRegistrationClient`).
   - PESO Gas Cylinder Rules 2016 (Rule 33/35) & IS 8451 compliance engine (`ComplianceService`, `VehicleService`).
   - 9-stage vehicle journey lifecycle management (`VehicleJourney`, `JourneyEvent`), station queue, and dispenser interlock control.
3. **Operator Console & Workflows (`cng frontend`)**:
   - React 18 SPA with Vite 6, TypeScript 5.7, Tailwind CSS with SGL corporate brand identity.
   - Dual-mode operator console:
     - Hands-free Camera Simulation with automated 1.5s scanning loop to AI service and real-time backend compliance verification.
     - Manual Registration Verification mode for operator lookup, statutory itemized safety breakdown, and supervisor override.
   - Real-time command center telemetry, FIFO fueling bay queue (6 bays), and official printable compliance certificates.

---

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| F1 | YOLOv8 Plate Detection | Multi-scale full-frame + 1.5x fallback license plate detection with adaptive ROI padding | M1 | ORIGINAL_REQUEST §2 R1, Survey |
| F2 | EasyOCR 6-Variant Recognition | Multi-candidate text recognition evaluated across 6 image preprocessing variants | M1 | ORIGINAL_REQUEST §2 R1, Survey |
| F3 | Indian Plate Normalization | 37 state/UT prefixes, HSRP 'IND' strip, positional character substitutions, regex structure validation | M1 | ORIGINAL_REQUEST §2 R1, Survey |
| F4 | Temporal Stabilization & Cooldown | 2.0s sliding window, 3-frame frequency voting, confidence weighting, 30s duplicate suppression | M1 | ORIGINAL_REQUEST §2 R1, Survey |
| F5 | RTSP Stream & Webcam Debugger | Reconnecting stream worker and interactive OpenCV HUD webcam display | M1 | ORIGINAL_REQUEST §2 R1, Survey |
| F6 | Pytest Suite & Packaging | 100% passing bare `pytest` via `pytest.ini` and pinned `requirements.txt` | M1 | ORIGINAL_REQUEST §2 R4, Survey |
| F7 | RapidAPI RTO Integration | Server-side secure RTO lookup via RapidAPI with isolated `RAPIDAPI_KEY` and graceful error handling | M2 | ORIGINAL_REQUEST §2 R2, Survey |
| F8 | PESO Hydro-Test Evaluation | Rule 33/35 validity check, ensuring missing hydro-test yields `UNKNOWN` status and `NOT_ELIGIBLE` | M2 | ORIGINAL_REQUEST §2 R2 & §5, Survey |
| F9 | Vehicle Verification Endpoint | `POST /api/v1/vehicles/verify` endpoint returning structured eligibility and RTO/PESO reasons | M2 | ORIGINAL_REQUEST §4, Survey |
| F10 | ANPR Detection Ingestion | `POST /api/v1/anpr/detections` endpoint initiating journeys and evaluating compliance | M2 | ORIGINAL_REQUEST §4, Survey |
| F11 | 9-Stage Vehicle Journey Lifecycle | Journey states (ENTERED -> QUEUE -> BAY_ASSIGNED -> FUELING -> EXITED) with event audit logging | M2 | ORIGINAL_REQUEST §1, Survey |
| F12 | Spring Boot Test Suite | 100% passing `mvn test` across all controllers, services, repositories, and integration tests | M2 | ORIGINAL_REQUEST §4, Survey |
| F13 | Camera Simulation Mode | Hands-free continuous loop scanning camera/video feed, detecting plates, auto-verifying compliance | M3 | ORIGINAL_REQUEST §2 R3, Survey |
| F14 | Manual Registration Verification | Manual plate entry, itemized statutory/safety breakdown, and supervisor override action | M3 | ORIGINAL_REQUEST §2 R3, Survey |
| F15 | Bay Allocation & Queue Display | FIFO queue management and visual 6-bay fueling grid with real-time status | M3 | ORIGINAL_REQUEST §2 R3, Survey |
| F16 | Printable Compliance Certificate | Official printable Sabarmati Gas Ltd compliance certificate (IS 8451 / PESO Rule 33) | M3 | ORIGINAL_REQUEST §2 R3, Survey |
| F17 | Frontend Build & Type Health | `npm run build` and `npx tsc --noEmit` passing with 0 errors and 0 lint warnings | M3 | ORIGINAL_REQUEST §4, Survey |
| F18 | Dual-Track Opaque E2E Test Suite | Automated end-to-end 4-tier test suite validating integration across AI, Backend, and Frontend | E2E-Track / M4 | ORIGINAL_REQUEST §2 R4 & §4 |
| F19 | Adversarial Coverage Hardening | Tier 5 white-box challenger stress tests against edge cases, malformed payloads, race conditions | M4 | Project Pattern Phase 2 |

---

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| E2E | E2E Testing Track | Requirement-driven opaque-box 4-tier test harness and test cases -> `TEST_READY.md` | none | IN_PROGRESS |
| M1 | AI ANPR Stabilization & Ingestion (R1) | Fix `pytest.ini`, `requirements.txt`, stream double-buffering, sync worker sleep, bare `pytest` 100% pass | none | IN_PROGRESS |
| M2 | Vehicle Compliance & RTO Engine (R2) | Fix `VehicleService` missing hydro-test false compliance, add `UNKNOWN` to `HydroTestStatus`, populate DTOs, verify `mvn test` | none | IN_PROGRESS |
| M3 | Operator UI Workflows & Override (R3) | Implement supervisor manual override modal/action, ensure dual-mode UI completeness, verify `npm run build` | M2 | PLANNED |
| M4 | Final E2E Verification & Hardening (R4) | Phase 1: 100% E2E test pass (Tiers 1-4). Phase 2: Tier 5 adversarial coverage hardening | M1, M2, M3, E2E | PLANNED |

---

## Interface Contracts

### AI Service ↔ Backend Ingestion Contract
- **Endpoint**: `POST /api/v1/anpr/detections`
- **Caller**: `sgl-cng-ai/app/pipeline/anpr_pipeline.py` (`_dispatch_to_backend`)
- **Receiver**: `com.sabarmati.cng.anpr.controller.AnprController`
- **Request Format**:
  ```json
  {
    "stationId": 1,
    "cameraId": 1,
    "registrationNumber": "GJ01AB1234",
    "detectedAt": "2026-09-28T13:10:00"
  }
  ```
- **Response Format (201 Created)**:
  ```json
  {
    "success": true,
    "message": "ANPR detection processed successfully",
    "data": {
      "detectionId": 101,
      "journeyId": 501,
      "registrationNumber": "GJ01AB1234",
      "complianceStatus": "ELIGIBLE",
      "journeyStatus": "ENTERED",
      "timestamp": "2026-09-28T13:10:00",
      "duplicate": false
    }
  }
  ```

### Frontend ↔ Backend Vehicle Verification Contract
- **Endpoint**: `POST /api/v1/vehicles/verify`
- **Caller**: `cng frontend/src/api/vehicleApi.ts` (`verifyVehicle`)
- **Receiver**: `com.sabarmati.cng.vehicle.controller.VehicleController`
- **Request Format**:
  ```json
  {
    "registrationNumber": "GJ01AB1234"
  }
  ```
- **Response Format (200 OK)**:
  ```json
  {
    "success": true,
    "message": "Vehicle verification completed",
    "data": {
      "registrationNumber": "GJ01AB1234",
      "ownerName": "Rajesh Patel",
      "vehicleClass": "Commercial / Auto",
      "fuelType": "CNG",
      "registrationStatus": "VALID",
      "fitnessValid": true,
      "insuranceValid": true,
      "pucValid": true,
      "hydroTestStatus": "VALID",
      "complianceStatus": "ELIGIBLE",
      "reasons": []
    }
  }
  ```
  *Safety Rule*: If hydro-test certificate is missing, `hydroTestStatus` must be `"UNKNOWN"`, `complianceStatus` must be `"NOT_ELIGIBLE"`, and `reasons` must contain `"CNG cylinder hydro-test certificate is missing or unverified"`.

### Frontend ↔ AI Service ANPR Recognition Contract
- **Endpoint**: `POST /ai-api/api/v1/anpr/recognize`
- **Caller**: `cng frontend/src/api/anprApi.ts` (`recognizeImage`)
- **Receiver**: `sgl-cng-ai/app/api/server.py` (`recognize_plate`)
- **Request**: `multipart/form-data` with form field `file` containing JPEG/PNG frame.
- **Response (200 OK)**:
  ```json
  {
    "plateDetected": true,
    "registrationNumber": "GJ01AB1234",
    "rawOcr": "GJ01AB1234",
    "confidence": 0.92,
    "detectorConfidence": 0.95,
    "ocrConfidence": 0.90,
    "bbox": [120, 240, 310, 95],
    "timestamp": "2026-09-28T13:10:00.123456",
    "reason": "OK"
  }
  ```

---

## Code Layout
- Root:
  - `pom.xml`, `Dockerfile`, `docker-compose.yml`, `.env.example`
  - `PROJECT.md`, `TEST_INFRA.md`, `TEST_READY.md`
- Backend:
  - `src/main/java/com/sabarmati/cng/`: Java source packages (`alert`, `anpr`, `audit`, `common`, `compliance`, `dashboard`, `fueling`, `integration`, `journey`, `security`, `station`, `vehicle`)
  - `src/main/resources/`: `application.yml`, `db/migration/` (Flyway SQL V1-V4)
  - `src/test/java/com/sabarmati/cng/`: Unit and integration test suites
  - `src/test/resources/`: `application-test.yml`
- AI Service:
  - `sgl-cng-ai/`: Python package root (`app/`, `tests/`, `main.py`, `pytest.ini`, `requirements.txt`)
  - `sgl-cng-ai/app/detector/`: YOLO plate detector
  - `sgl-cng-ai/app/ocr/`: EasyOCR engine
  - `sgl-cng-ai/app/pipeline/`: ANPR pipeline, normalizer
  - `sgl-cng-ai/app/api/`: FastAPI server
  - `sgl-cng-ai/tests/`: Pytest test suite
- Frontend:
  - `cng frontend/`: React application root (`src/`, `package.json`, `vite.config.ts`, `tsconfig.json`)
  - `cng frontend/src/pages/`: Page components (`LiveAnprPage`, `DashboardPage`, `QueuePage`, `JourneyDetailsPage`, etc.)
  - `cng frontend/src/components/`: Reusable UI components
  - `cng frontend/src/api/`: Axios client modules
  - `cng frontend/src/types/`: TypeScript type definitions
- E2E Tests:
  - `tests/e2e/`: Opaque-box automated E2E test harness and test scenarios
