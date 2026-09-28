# SGL Smart CNG Station Compliance & Vehicle Journey Management

A full-stack demo platform for managing CNG station vehicle compliance and vehicle journeys. It brings together a React operator console, a Spring Boot API, a Python ANPR service, and PostgreSQL. The Docker setup runs all four services together.

## What the application does

- **Vehicle identification:** Use a laptop webcam as an entry or exit camera. The browser captures frames and sends them to the Python AI service for number-plate detection and OCR; the browser itself does not run the recognition model. The operator can switch the camera mode between **Entry** and **Exit**.
- **Compliance checks:** Check vehicle registration status and CNG cylinder hydro-test validity before fueling. A non-compliant vehicle is marked ineligible and cannot proceed through the fueling workflow.
- **Vehicle journeys:** Follow a vehicle from detection and queue, to bay assignment, fueling, and exit. When an exit detection is recorded, the journey is completed and removed from the active queue.
- **Queue and six fueling bays:** View the queue and bay availability, assign vehicles to bays, and manually start or complete fueling. The station is seeded with six bays.
- **Station operations dashboard:** Review station activity and operational metrics, including journey status, queue, throughput, bay use, compliance, and alerts.
- **Security and traceability:** Sign in with role-based demo accounts. Compliance and station actions can be reviewed through the audit and alert views.
- **Demo verification data:** The mock hydro-test service includes 100 generated examples: valid, expired, due-soon, and missing certificates, plus named test plates. The mock provider is suitable for a local demo, not an authoritative compliance decision.

## Architecture

```text
Web browser (React UI + webcam)
        ├── station/API requests ──> Spring Boot backend ──> PostgreSQL
        └── camera frames ─────────> Python AI / ANPR service ──> Spring Boot backend
```

The backend uses mock vehicle-registration verification by default, and mock hydro-test data. Vehicle registration can optionally use the configured RapidAPI provider. Hydro-test verification remains mock data in this project.

## Requirements

- Docker Desktop (Mac/Windows) or Docker Engine (Linux)
- Docker Compose v2 (`docker compose`)
- A browser with webcam access, if you want to use live camera detection

The first build can take several minutes because it builds the services and initializes the AI dependencies/models.

## Deploy a free demo on Render

The repository includes a `render.yaml` Blueprint for a public React static site, Spring Boot API, and Render Postgres database. It keeps the Python ANPR service off the free cloud plan so the model runs on your laptop for the live-camera demo.

1. Sign in to Render and choose **New → Blueprint**.
2. Connect `dheeraj17082005/SGL-HYDRO` on the `main` branch.
3. Review the resources in `render.yaml` and deploy the Blueprint. It creates the API and database in Singapore; the React static site is served through Render's global CDN.
4. Open the frontend URL shown in the Render dashboard. The API URL is connected automatically by the Blueprint.

Render's free web services sleep after 15 minutes without traffic and can take about a minute to wake. Free Render Postgres is limited to 1 GB and expires 30 days after creation, so this setup is for a short demo, not durable storage. See [Render's free-instance limits](https://render.com/docs/free).

### Live camera for the demo

The public Render frontend and the AI service running on your laptop are on different networks. A deployed HTTPS page cannot reach the laptop's private `localhost` service automatically. The reliable camera demonstration is to run the complete Docker Compose stack locally and open `http://localhost:5174`; the camera frames then reach the local Python AI service through the frontend proxy.

If you specifically need the public Render page to use the laptop's AI, the laptop AI service needs a publicly reachable HTTPS tunnel, the frontend must be rebuilt with that tunnel URL in `VITE_AI_API_BASE_URL`, and the local AI process must be started with `AI_ALLOWED_ORIGINS` set to the Render frontend origin. The AI endpoint is not authenticated, so do not expose it through an unrestricted tunnel; use an access-controlled tunnel for a brief demo.

## Start with Docker Compose

Open a terminal in this project folder and run:

```sh
cp .env.docker.example .env.docker
docker compose --env-file .env.docker up --build -d
```

When the containers are up, open **http://localhost:5174** and sign in. The seeded operator account is:

- Username: `operator`
- Password: `operator123`

The backend also seeds these demo roles:

| Role | Username | Password |
|---|---|---|
| Administrator | `admin` | `admin123` |
| Station manager | `manager` | `manager123` |
| Station operator | `operator` | `operator123` |
| Compliance officer | `compliance` | `compliance123` |
| Auditor | `auditor` | `auditor123` |

These are local-demo credentials. Change or remove them and replace the sample database/JWT credentials before any deployment beyond a private demo environment.

## Try the main workflow

1. Sign in with the operator account.
2. Open **Live Monitoring**, allow browser camera access, and start the camera. Choose **Entry Camera** or **Exit Camera** for the single webcam.
3. For an entry, keep a plate centered and visible so the AI service can read it. The backend records the detection and checks registration and hydro-test status.
4. Open **Bays & Queue** to review eligible vehicles, assign a bay, and advance fueling manually. Ineligible vehicles are blocked from fueling actions.
5. Switch to **Exit Camera** and detect the vehicle at exit. Its journey is marked complete and it leaves the active queue.
6. Review compliance, alerts, or audit history in the corresponding sections.

For controlled demos without a physical vehicle, use the built-in vehicle verification form and mock examples. The hydro-test mock dataset includes plates from `MH12DE1001` through `MH12DE1100`; for example, the first 75 are valid, the next 15 are expired, the next five are due soon, and the final five have no record. Named examples include `GJ01AB1234`, `EXPHYDRO`, and `NOHYDRO`.

## Optional: use RapidAPI for vehicle registration

The default configuration uses the built-in mock registration provider and needs no API key. To enable the RapidAPI registration provider:

1. Edit the local, ignored `.env.docker` file.
2. Set `VEHICLE_VERIFICATION_PROVIDER=rapidapi` and put your key in `RAPIDAPI_KEY`.
3. Keep the configured host and URL unless your RapidAPI subscription specifies different values.
4. Recreate the backend service:

   ```sh
   docker compose --env-file .env.docker up --build -d app
   ```

Never put a real API key in source files or commit `.env.docker`. Hydro-test checks continue to use mock data.

## Useful commands

Show service status:

```sh
docker compose --env-file .env.docker ps
```

Follow service logs:

```sh
docker compose --env-file .env.docker logs -f frontend app ai postgres
```

Restart after configuration changes:

```sh
docker compose --env-file .env.docker up --build -d
```

Stop the application while keeping PostgreSQL data:

```sh
docker compose --env-file .env.docker down
```

Reset the local database and remove its stored data (destructive):

```sh
docker compose --env-file .env.docker down -v
```

The frontend is available at **http://localhost:5174** and the backend is mapped to **http://localhost:8082** on localhost. The AI service is reached through the frontend proxy and the Docker network.

## Camera and startup troubleshooting

- **Camera permission:** Allow camera access for `http://localhost:5174` in the browser. Camera access is restricted to localhost or HTTPS origins. Close other applications that may be using the webcam, then reload the page.
- **Services still starting:** Check `docker compose --env-file .env.docker ps` and the logs. The AI service may take longer on its first start.
- **Port already in use:** Change `FRONTEND_PORT` or `BACKEND_PORT` in `.env.docker`, then recreate the services.
- **Need a clean app state:** `docker compose --env-file .env.docker down -v` deletes the database volume and all persisted local data; use it only when you intend to reset the demo.

## Project layout

```text
.
├── cng frontend/       # React + Vite operator console and Nginx container
├── src/                # Spring Boot API, persistence, security, and migrations
├── sgl-cng-ai/         # Python ANPR service, OCR models, and AI tests
├── tests/e2e/          # End-to-end workflow test sources
├── docker-compose.yml  # Local multi-service stack
└── .env.docker.example # Safe local configuration template
```

## Scope and deployment note

This repository is a demonstration and development system. The mock data and default credentials are not production compliance sources. Before real operational use, connect authorized vehicle and hydro-test registries, secure all credentials, set production database/JWT secrets, configure HTTPS and access controls, and complete deployment-specific security and reliability reviews.
