# Server Monitoring Dashboard

A full-stack CRUD dashboard for tracking the status of applications/services deployed on a server, built with Angular, Spring Boot, and PostgreSQL, containerized with Docker, and deployed via a Jenkins pipeline.

**Live:** [dashboard.shahirjalal.com](https://dashboard.shahirjalal.com)

## Features

- List all tracked applications (public, no login required)
- Add, edit, and delete an application (requires login)
- Automated health checks: a scheduled job probes each app's port every 30s and
  keeps its status in sync, instead of trusting a manually-set field
- Status summary: live counts + a proportion bar across `RUNNING` / `STOPPED` / `UNKNOWN`
- Search and sort the application list
- Session-based login for a single admin account, backing all writes
- REST API with full CRUD, validated request bodies, and JSON error responses
- Status badges (`RUNNING` / `STOPPED` / `UNKNOWN`)
- Toast notifications for add/edit/delete/login feedback
- Dockerized frontend (Nginx) and backend (Spring Boot)
- Docker Compose orchestration with PostgreSQL
- Nginx reverse proxy for `/api`
- Jenkins CI/CD pipeline for build, test, and deploy

---

## Tech Stack

### Frontend
- Angular 18 (standalone components)
- Bootstrap 5 / PrimeNG / PrimeFlex
- TypeScript

### Backend
- Java 17
- Spring Boot 3.5 (Web, Data JPA, Validation, Security, Actuator)
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

Base path: `/api/applications`. `GET` is public; `POST`/`PUT`/`DELETE` require login
(see [Authentication](#authentication)).

| Method | Path                    | Description                              |
|--------|--------------------------|-------------------------------------------|
| GET    | `/api/applications`      | List all applications                     |
| POST   | `/api/applications`      | Create an application                     |
| GET    | `/api/applications/{id}` | Get an application by ID                  |
| PUT    | `/api/applications/{id}` | Update an application                     |
| DELETE | `/api/applications/{id}` | Delete an application                     |

Application payload:

```json
{
  "name": "string (required)",
  "description": "string",
  "port": "number (required, 1-65535)",
  "status": "RUNNING | STOPPED | UNKNOWN (optional -- the health check owns this after creation)"
}
```

Errors are returned as JSON (`{ "status", "error", "message", "fieldErrors" }`) with
the matching HTTP status: `400` for validation failures, `401`/`403` for auth, `404`
for a missing application.

Auth endpoints, base path `/api/auth`:

| Method | Path            | Description                          |
|--------|------------------|---------------------------------------|
| POST   | `/api/auth/login`  | Log in, starts a session               |
| POST   | `/api/auth/logout` | Log out, ends the session              |
| GET    | `/api/auth/me`      | Current logged-in user, or 401         |

---

## Authentication

A single admin account guards create/update/delete; the list itself stays public
so the dashboard can be shared as a read-only status page.

- Configured via `ADMIN_USERNAME` / `ADMIN_PASSWORD` env vars (local default:
  `admin` / `admin123` -- **override these before deploying anywhere public**,
  see [.env.example](.env.example)).
- Session-cookie based (not JWT): `/api/auth/login` starts a server session,
  `/api/auth/logout` ends it.
- CSRF protection via Spring Security's cookie pattern (`XSRF-TOKEN` cookie +
  `X-XSRF-TOKEN` header), which Angular's `HttpClient` handles automatically.
- Works without CORS complexity because both dev (`ng serve`'s proxy) and prod
  (Nginx) put the frontend and `/api` on the same origin -- see
  [DEPLOYMENT.md](DEPLOYMENT.md) section 7.

---

## Run locally with Docker

The backend image copies a **pre-built jar** rather than building it inside the container, so the jar must exist before running Compose.

```bash
# 1. Build the backend jar
cd backend
./mvnw clean package -DskipTests
cd ..

# 2. (optional) copy .env.example to .env and set real admin credentials --
#    otherwise it falls back to admin/admin123, fine for a local try-out
cp .env.example .env

# 3. Build and start everything
docker compose up --build
```

| Service  | URL                          |
|----------|-------------------------------|
| Frontend | http://localhost:4200         |
| Backend  | http://localhost:8081/api     |
| Postgres | localhost:5433 (db `monitoring`) |

Log in at `http://localhost:4200/login` with the admin credentials above to add,
edit, or delete applications.

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
| `ADMIN_USERNAME`               | `admin`                                          |
| `ADMIN_PASSWORD`               | `admin123`                                       |
| `CORS_ALLOWED_ORIGINS`         | `http://localhost:4200`                          |
| `HEALTH_CHECK_INTERVAL_MS`     | `30000`                                          |

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
3. Runs backend tests (`mvnw test`)
4. Installs and builds the frontend (`npm install && npm run build`)
5. Runs frontend tests (`ng test --no-watch --browsers=ChromeHeadless` -- needs
   Chrome/Chromium on the agent)
6. Redeploys via `docker compose down && docker compose up --build -d`

The `home-server` agent is an Ubuntu box running the Docker Compose stack directly; a Cloudflare Tunnel exposes it publicly at [dashboard.shahirjalal.com](https://dashboard.shahirjalal.com) without opening any inbound ports.

---

## Future Improvements

- Server health monitoring (CPU & RAM statistics), beyond the current port-open check
- Docker container monitoring
- Multi-user accounts (currently a single shared admin login)
- Uptime history view (the data -- `lastCheckedAt` / `lastStatusChangeAt` -- is
  already tracked; there's no timeline UI for it yet)
