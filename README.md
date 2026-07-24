# Server Monitoring Dashboard

A full-stack CRUD dashboard for tracking the status of applications/services deployed on a server, built with Angular, Spring Boot, and PostgreSQL, containerized with Docker, and deployed via a Jenkins pipeline.

**Live:** [dashboard.shahirjalal.com](https://dashboard.shahirjalal.com)

## Features

- List all tracked applications
- Add an application (name, description, port, status)
- Delete an application
- REST API with full CRUD (including update by ID, not yet wired into the UI)
- Status badges (`RUNNING` / `STOPPED`)
- Dockerized frontend (Nginx) and backend (Spring Boot)
- Docker Compose orchestration with PostgreSQL
- Nginx reverse proxy for `/api`
- Jenkins CI/CD pipeline for build + deploy

---

## Tech Stack

### Frontend
- Angular 18 (standalone components)
- Bootstrap 5 / PrimeNG / PrimeFlex
- TypeScript

### Backend
- Java 17
- Spring Boot 3.5 (Web, Data JPA, Validation, Actuator)
- Lombok

### Database
- PostgreSQL 16

### DevOps
- Docker / Docker Compose
- Nginx
- Jenkins
- Cloudflare Tunnel (public ingress, no port forwarding)
- Self-hosted on an Ubuntu home server

---

## Architecture

```
Internet
    │
    ▼
Cloudflare Tunnel  (dashboard.shahirjalal.com)
    │
    ▼
Ubuntu home server
    │
    ▼
Angular (served by Nginx, port 4200 → container port 80)
    │
   /api  (proxied by Nginx)
    │
    ▼
Spring Boot (port 8081)
    │
    ▼
PostgreSQL (port 5433 → container port 5432)
```

---

## Project Structure

```
server-monitoring-dashboard
│
├── backend/            Spring Boot REST API
├── frontend/           Angular app
├── docker-compose.yml  Local/prod orchestration (postgres + backend + frontend)
└── Jenkinsfile         CI/CD pipeline (build jars, build frontend, redeploy via compose)
```

---

## API Reference

Base path: `/api/applications`

| Method | Path                    | Description              |
|--------|--------------------------|---------------------------|
| GET    | `/api/applications`      | List all applications     |
| POST   | `/api/applications`      | Create an application     |
| GET    | `/api/applications/{id}` | Get an application by ID  |
| PUT    | `/api/applications/{id}` | Update an application     |
| DELETE | `/api/applications/{id}` | Delete an application     |

Application payload:

```json
{
  "name": "string (required)",
  "description": "string",
  "port": "number (required)",
  "status": "RUNNING | STOPPED"
}
```

---

## Run locally with Docker

The backend image copies a **pre-built jar** rather than building it inside the container, so the jar must exist before running Compose.

```bash
# 1. Build the backend jar
cd backend
./mvnw clean package -DskipTests
cd ..

# 2. Build and start everything
docker compose up --build
```

| Service  | URL                          |
|----------|-------------------------------|
| Frontend | http://localhost:4200         |
| Backend  | http://localhost:8081/api     |
| Postgres | localhost:5433 (db `monitoring`) |

---

## Run locally without Docker (development)

**Postgres** — run it however you like, just make sure it's reachable at the URL below (or override via env vars).

**Backend**

```bash
cd backend
./mvnw spring-boot:run
```

Runs on `http://localhost:8081`. Defaults (overridable via env vars) come from [application.properties](backend/src/main/resources/application.properties):

| Variable                      | Default                                         |
|--------------------------------|--------------------------------------------------|
| `SPRING_DATASOURCE_URL`        | `jdbc:postgresql://localhost:5433/monitoring`     |
| `SPRING_DATASOURCE_USERNAME`   | `monitoring`                                     |
| `SPRING_DATASOURCE_PASSWORD`   | `monitoring123`                                  |

**Frontend**

```bash
cd frontend
npm install
npm start
```

Runs on `http://localhost:4200` and proxies `/api` to `http://localhost:8081` via [proxy.conf.json](frontend/proxy.conf.json).

---

## CI/CD

The [Jenkinsfile](Jenkinsfile) runs on a `home-server` agent and, on each build:

1. Checks out the repo
2. Builds the backend jar (`mvnw clean package -DskipTests`)
3. Installs and builds the frontend (`npm install && npm run build`)
4. Redeploys via `docker compose down && docker compose up --build -d`

The `home-server` agent is an Ubuntu box running the Docker Compose stack directly; a Cloudflare Tunnel exposes it publicly at [dashboard.shahirjalal.com](https://dashboard.shahirjalal.com) without opening any inbound ports.

---

## Future Improvements

- Authentication
- Wire the update (`PUT`) endpoint into the UI (edit existing applications)
- Server health monitoring (CPU & RAM statistics)
- Docker container monitoring
- Charts
- User management
