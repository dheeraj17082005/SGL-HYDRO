# SGL Smart CNG Station Compliance & Vehicle Journey Management

A full-stack, production-hardened platform for automated CNG station compliance verification and end-to-end vehicle journey orchestration. The system integrates a React operator console, a Java Spring Boot backend, a specialized Python AI ANPR microservice with temporal tracking & consensus, and PostgreSQL.

---

## 🏗 System Architecture & End-to-End Workflow

```text
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│                                   END-TO-END FLOW OVERVIEW                                  │
└─────────────────────────────────────────────────────────────────────────────────────────────┘

  [ Vehicle Enters ]
          │
          ▼
   📷 Camera Feed (Entry Lane)
          │  (Video frames streamed via HTTP/Canvas)
          ▼
   🧠 Python AI Microservice (Port 8001)
      ├── 1. Detection (YOLOv8 Plate & Vehicle Detector every N frames)
      ├── 2. Tracking (Centroid & Bounding Box overlap tracker per vehicle)
      ├── 3. Quality Filter (Blur Laplacian, resolution & aspect ratio checks)
      ├── 4. OCR Engine (EasyOCR with Indian Plate pattern heuristics)
      ├── 5. Temporal Consensus (Position-wise weighted voting across 3+ frames)
      └── 6. Event Emission (Emits single CONFIRMED event per vehicle)
          │
          ▼  POST /api/v1/anpr/events
   ☕ Spring Boot API Gateway (Port 8082)
      ├── 1. Ingests ANPR event & avoids duplicates
      ├── 2. Compliance Engine Checks:
      │       • VAHAN Registration status
      │       • CNG Cylinder Hydro-test certificate (validity & expiry)
      ├── 3. Journey State Machine:
      │       • Eligible   ──> Status: IN_QUEUE
      │       • Ineligible ──> Status: BLOCKED / AUDIT ALERT
      └── 4. Persists to PostgreSQL
          │
          ▼  WebSockets / REST Polling
   💻 React Operator Console (Port 5174)
      ├── Real-Time Overview & Metrics Dashboard
      ├── Live Camera AI Overlay with Bounding Boxes
      ├── Bays & Queue Manager (6 Dispensary Bays)
      ├── Compliance & Hydro-test Verification Engine
      └── Alerts, Exceptions & Audit Trails
          │
          ▼
   ⛽ Fueling & Dispatch Workflow
      ├── 1. Assign Vehicle from Queue to Available Bay (Bay 1–6)
      ├── 2. Fueling Dispenser Authorization & Progress
      ├── 3. Fueling Complete
      └── 4. Vehicle proceeds to Exit Lane
          │
          ▼
   📷 Exit Camera Detection
      └── AI verifies plate at exit ──> Journey marked COMPLETED ──> Dispatched
```

---

## 🚀 Key Functional Modules

| Module | Technologies | Key Responsibilities |
|---|---|---|
| **Frontend Console** | React 19, Vite, Tailwind/CSS | Operator UI, camera stream capture, live queue, fueling bay allocation, manual compliance overrides, and audit dashboards. |
| **Backend Core** | Spring Boot 3.4, Java 21, JPA/Hibernate | State machine orchestration, compliance policy engine, hydro-test verification, role-based security (JWT), and audit logs. |
| **AI ANPR Microservice** | Python 3.11, FastAPI, YOLOv8, EasyOCR | High-precision plate recognition, object tracking, quality filtering, character-level consensus voting, and duplicate event suppression. |
| **Database** | PostgreSQL 16 | Relational persistence for vehicle journeys, compliance certificates, bay statuses, audit trails, and user credentials. |

---

## 🛠 Step-by-Step Setup Guide

### Prerequisites
- [Docker Desktop](https://www.docker.com/) (Mac, Windows, or Linux) with Compose v2.
- 4GB+ RAM allocated to Docker.
- A webcam (optional, for live camera testing; static test samples are included).

---

### Step 1: Clone the Repository
```bash
git clone https://github.com/dheeraj17082005/SGL-HYDRO.git
cd SGL-HYDRO
```

---

### Step 2: Configure Environment Variables
Create your local environment file using the provided template:
```bash
cp .env.docker.example .env.docker
```

*Note: The defaults in `.env.docker.example` work out of the box for local development.*

---

### Step 3: Launch with Docker Compose
Run the entire 4-service stack:
```bash
docker compose --env-file .env.docker up --build -d
```

Verify that all 4 containers are healthy:
```bash
docker compose --env-file .env.docker ps
```

You should see:
- `sgl-hydro-frontend-1` on port `5174`
- `sgl-hydro-app-1` on port `8082`
- `sgl-hydro-ai-1` on port `8001`
- `sgl-hydro-postgres-1` on port `5432`

---

## 🔑 Default Credentials & Roles

Open your browser at **[http://localhost:5174](http://localhost:5174)**.

| Role | Username | Password | Access Scope |
|---|---|---|---|
| **Station Operator** | `operator` | `operator123` | Queue management, camera monitoring, bay fueling actions |
| **Station Manager** | `manager` | `manager123` | Full station metrics, throughput, employee activities |
| **Compliance Officer**| `compliance` | `compliance123` | Hydro-test certificate inspections, blacklisting, manual audits |
| **Administrator** | `admin` | `admin123` | Full system configuration and user management |
| **Auditor** | `auditor` | `auditor123` | Read-only compliance and event audit history |

---

## 📋 Complete End-to-End Walkthrough

### 1. Operator Login & Dashboard
1. Navigate to `http://localhost:5174`.
2. Log in using `operator` / `operator123`.
3. You will land on the **Overview** dashboard showing station throughput, active bays, fuel dispensed, queue count, and the latest verified vehicle.

---

### 2. Vehicle Entry & ANPR Detection
1. Click **Live Monitoring** in the sidebar.
2. Select **Entry Lane 1**.
3. Choose either:
   - **Live Camera Feed**: Allow browser camera permissions and hold up a vehicle number plate or phone screen.
   - **Upload Plate Photo / Quick Test Samples**: Click any of the preloaded test samples:
     - 🚗 `RJ14CV0002` (Kia Sonet - Valid Hydro Test)
     - 🚗 `HR98AA0000` (HSRP Plate - Valid)
     - 🚗 `EXPHYDRO` (Expired Cylinder - Non-compliant test)
     - 🚗 `NOHYDRO` (Unregistered Cylinder - Non-compliant test)
4. The AI microservice processes the frame:
   - Applies YOLOv8 detection & EasyOCR.
   - Evaluates temporal consensus buffer.
   - Emits a `CONFIRMED` ANPR event to Spring Boot.

---

### 3. Automated Compliance & Queueing
1. The backend automatically queries the compliance records:
   - Checks if the vehicle has an active CNG cylinder hydrostatic test certificate.
   - **If Valid (e.g. `RJ14CV0002`):** Status is set to `ELIGIBLE` and vehicle automatically enters `IN_QUEUE`.
   - **If Expired / Missing (e.g. `EXPHYDRO`):** Status is set to `INELIGIBLE / BLOCKED`. Fueling is prohibited, and an alert is raised in the **Alerts** tab.

---

### 4. Bay Allocation & Fueling
1. Click **Bays & Queue** in the navigation menu.
2. Under **Active Queue**, locate the eligible vehicle.
3. Select an available bay from **Bays 1 through 6** and click **Assign to Bay**.
4. The bay status switches to `DISPENSING`.
5. Once fueling is complete, click **Complete Fueling**. The bay resets to `AVAILABLE`.

---

### 5. Vehicle Exit & Journey Completion
1. Return to **Live Monitoring** and switch camera mode to **Exit Lane**.
2. Scan the vehicle plate upon exit (or select the sample plate).
3. The system matches the active journey:
   - Calculates total turnaround time and fuel dispensed.
   - Sets journey status to `COMPLETED`.
   - Clears the vehicle from the active station queue.

---

### 6. Audit & Compliance Inspection
- **Compliance Tab**: Look up any registration number (e.g., `MH12DE1001` through `MH12DE1100`) to inspect cylinder manufacturing dates, hydro-test centers, and re-test due dates.
- **Alerts Tab**: Review unauthorized fueling attempts, blocked vehicles, or low-confidence plate reads.
- **Audit Tab**: Full tamper-evident chronological event log of every operator action and camera detection.

---

## 🧪 Testing the AI Microservice Independently

The AI microservice can be queried directly via curl or Python scripts:

### Test Single Frame Detection
```bash
curl -X POST "http://localhost:8001/api/v1/anpr/detect" \
  -F "file=@cng frontend/public/samples/rj_kia.jpg"
```
**Expected Response:**
```json
{
  "plateDetected": true,
  "registrationNumber": "RJ14CV0002",
  "confidence": 0.85,
  "stateCode": "RJ"
}
```

### Run Synthetic Temporal Consensus Test Suite
```bash
python sgl-cng-ai/test_stage2_scenarios.py
```

---

## 🧰 Management & Troubleshooting Commands

| Command | Action |
|---|---|
| `docker compose --env-file .env.docker ps` | Check live status of all services |
| `docker compose --env-file .env.docker logs -f app` | Tail Spring Boot backend logs |
| `docker compose --env-file .env.docker logs -f ai` | Tail AI microservice inference logs |
| `docker compose --env-file .env.docker restart frontend` | Restart frontend Nginx container |
| `docker compose --env-file .env.docker down` | Safely stop containers (preserves DB data) |
| `docker compose --env-file .env.docker down -v` | **Full reset**: removes all containers and database volumes |

### Common Troubleshooting Tips
1. **Camera not starting in browser:** Webcams require `http://localhost` or HTTPS. Ensure you are accessing via `http://localhost:5174` and have allowed browser camera permissions.
2. **Port conflict on 8082 or 5174:** Edit `BACKEND_PORT` or `FRONTEND_PORT` in your `.env.docker` file and run `docker compose --env-file .env.docker up -d`.
3. **Database connection retry:** On first launch, PostgreSQL initializes in ~10 seconds. Spring Boot will wait for it and connect automatically.

---

## 📁 Repository Directory Structure

```text
SGL-HYDRO/
├── cng frontend/              # React 19 + Vite operator console
│   ├── public/                # Static assets, login photo, quick test sample images
│   ├── src/                   # React views (Overview, Monitoring, Bays, Compliance, Audit)
│   └── Dockerfile             # Multi-stage Nginx build
├── src/                       # Spring Boot 3.4 API backend
│   ├── main/java/com/sabarmati/cng/
│   │   ├── anpr/              # ANPR event intake & camera routing
│   │   ├── compliance/        # Hydro-test verification & rules engine
│   │   ├── journey/           # Vehicle journey state machine
│   │   ├── security/          # Spring Security, JWT & RBAC
│   │   └── station/           # Bay allocation & dispenser operations
│   └── main/resources/        # Liquibase DB migrations & application configs
├── sgl-cng-ai/                # Python ANPR microservice
│   ├── app/                   # YOLOv8 detector, tracker, EasyOCR engine, consensus
│   ├── plate_model_indian.pt  # Fine-tuned Indian number plate weights
│   ├── temporal_consensus.py  # Multi-frame consensus voting engine
│   ├── main.py                # FastAPI entrypoint
│   └── Dockerfile             # PyTorch + OpenCV + CUDA/CPU container
├── docker-compose.yml         # Unified 4-container production stack
├── .env.docker.example        # Environment template
└── README.md                  # Complete platform documentation
```

---

## 📜 License & Compliance Notice

This system is configured for demonstration and testbed purposes. In real-world operational deployments:
- Connect the verification pipeline to authorized state VAHAN APIs and PESO/CPCB approved hydro-testing databases.
- Enforce HTTPS and update all default passwords and JWT secrets in production environment files.
