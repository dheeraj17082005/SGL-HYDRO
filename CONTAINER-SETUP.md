# Run the SGL station app with Docker

The stack includes the React console, Spring Boot API, Python ANPR service, and PostgreSQL. The browser reaches both APIs through the frontend container, so camera capture continues to run in the browser and plate recognition stays in the Python service.

## Start

From this folder:

```sh
cp .env.docker.example .env.docker
docker-compose --env-file .env.docker up --build -d
```

Open <http://localhost:5174>. The demo operator account seeded by the backend is `operator` / `operator123`. The seed process creates the default station and six available fueling bays. The first AI container start may take a few minutes while Python dependencies and OCR weights initialize.

The default verification provider is the backend mock provider so the demo works without an external key. To use the vehicle lookup API, set `VEHICLE_VERIFICATION_PROVIDER=rapidapi` and `RAPIDAPI_KEY` in `.env.docker`, then recreate the app container. Do not commit that file.

## Useful commands

```sh
docker-compose --env-file .env.docker ps
docker-compose --env-file .env.docker logs -f frontend app ai
docker-compose --env-file .env.docker down
```

PostgreSQL data persists in the `postgres_data` volume. `down` keeps that data; add `-v` only when you intend to erase it. The included fallback passwords and operator account are for local demonstration only. Set private credentials before deployment outside the laptop.
