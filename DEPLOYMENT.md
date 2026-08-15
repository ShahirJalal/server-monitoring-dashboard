# Deployment Pattern Reference

This document captures the full deployment setup used in this project (Angular +
Spring Boot + PostgreSQL, self-hosted via Docker Compose + Jenkins + Cloudflare
Tunnel), stripped down into a reusable pattern. Use it as a checklist/template
when standing up deployment for other projects.

Live reference implementation: [dashboard.shahirjalal.com](https://dashboard.shahirjalal.com)

---

## 1. Architecture Overview

```
Internet
    │
    ▼
Cloudflare Tunnel   (public hostname, e.g. app.example.com — no inbound ports opened)
    │
    ▼
Self-hosted server (Ubuntu box running Docker Compose)
    │
    ▼
Frontend container — Nginx serving static build (host port 4200 → container port 80)
    │
   /api  (reverse-proxied by Nginx)
    │
    ▼
Backend container — Spring Boot API (host port 8081 → container port 8081)
    │
    ▼
Database container — PostgreSQL (host port 5433 → container port 5432)
```

Key ideas behind this pattern:
- **One repo, multiple services**, each with its own `Dockerfile`, orchestrated by a single root `docker-compose.yml`.
- **Nginx is both the static file server and the reverse proxy** — the SPA and the `/api` prefix are served from the same origin, so there's no CORS/cookie complexity in production.
- **Jenkins builds artifacts on the host, then hands off to Docker Compose** to build images and redeploy — it does not build inside Docker.
- **No inbound firewall ports.** Public access goes through a Cloudflare Tunnel, so the server has no exposed ports at the network level.
- **Config via environment variables with sane local defaults** (Spring's `${VAR:default}` syntax) so the same image runs locally and in prod.

---

## 2. Repo Layout

```
project-root/
├── backend/                 Service A (API)
│   ├── Dockerfile
│   ├── .dockerignore
│   ├── .gitignore
│   └── src/...
├── frontend/                 Service B (SPA)
│   ├── Dockerfile
│   ├── .dockerignore
│   ├── .gitignore
│   ├── nginx.conf
│   └── src/...
├── docker-compose.yml         Orchestrates all services for local + prod
├── Jenkinsfile                 CI/CD pipeline
├── .gitignore                  Root-level ignores
└── README.md
```

Each deployable service gets its **own** `Dockerfile` and `.dockerignore` living
next to its source, so `docker compose build` can use `context: ./service-name`
independently.

---

## 3. Dockerfiles

### 3a. Backend (Spring Boot / JVM) — `backend/Dockerfile`

This project uses a **pre-built jar** copied into a slim JRE image (not a
multi-stage Maven build) — the jar is built on the Jenkins host in a prior
pipeline stage, so the image build stays fast and doesn't need Maven/network
access at image-build time.

```dockerfile
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY target/backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
```

Pattern to reuse for any JVM service:
- Use a `-jre` (not `-jdk`) base image for the runtime stage — smaller, no compiler needed.
- `EXPOSE` the port the app listens on (informational, matches `server.port`).
- If you'd rather build inside Docker (no host build step), switch to a multi-stage build: a `maven`/`gradle` builder stage that runs the packaging step, then `COPY --from=build` the jar into the `-jre` stage. Trade-off: slower/more complex image builds vs. no host build dependency.

### 3b. Frontend (Angular / any SPA) — `frontend/Dockerfile`

Multi-stage: build the static assets with Node, then serve them with Nginx.

```dockerfile
# ---------- Build Stage ----------
FROM node:20-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .
RUN npm run build

# ---------- Runtime Stage ----------
FROM nginx:alpine

COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

Pattern to reuse for any SPA (React/Vue/Angular/etc.):
- `COPY package*.json ./` then `RUN npm install` **before** copying the rest of the source — maximizes Docker layer caching so `npm install` only reruns when dependencies change.
- Final runtime image is `nginx:alpine` (~40MB) containing only static files — the Node toolchain never ships to production.
- Adjust the `COPY --from=build` source path to match your framework's build output directory (Angular: `dist/<project>/browser`; CRA: `build/`; Vite: `dist/`).

---

## 4. Nginx Config (`frontend/nginx.conf`)

Serves the SPA with client-side routing fallback, and reverse-proxies API calls
to the backend container by its Compose service name.

```nginx
server {
    listen 80;
    server_name localhost;

    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://backend:8081/api/;

        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Pattern to reuse:
- `try_files $uri $uri/ /index.html;` is required for any SPA using client-side routing — otherwise refreshing a deep link 404s.
- `proxy_pass http://<compose-service-name>:<port>/...` works because Compose puts all services on the same Docker network with DNS resolution by service name — no hardcoded IPs.
- Forwarding `X-Forwarded-*` headers lets the backend know the original client IP/protocol if it ever needs them (logging, rate limiting).
- Same-origin `/api` proxying avoids CORS entirely in production even though the app still needs permissive CORS for local dev (see §7).

---

## 5. Docker Compose (`docker-compose.yml`)

```yaml
services:

  postgres:
    image: postgres:16
    restart: unless-stopped
    environment:
      POSTGRES_DB: monitoring
      POSTGRES_USER: monitoring
      POSTGRES_PASSWORD: monitoring123
    volumes:
      - postgres-data:/var/lib/postgresql/data
    ports:
      - "5433:5432"

  backend:
    build:
      context: ./backend
    restart: unless-stopped
    depends_on:
      - postgres
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/monitoring
      SPRING_DATASOURCE_USERNAME: monitoring
      SPRING_DATASOURCE_PASSWORD: monitoring123
    ports:
      - "8081:8081"

  frontend:
    build:
      context: ./frontend
    restart: unless-stopped
    depends_on:
      - backend
    ports:
      - "4200:80"

volumes:
  postgres-data:
```

Pattern to reuse:
- **Named volume for the database** (`postgres-data`) so data survives container recreation/redeploys.
- **`restart: unless-stopped`** on every service — survives host reboots without manual intervention.
- **`depends_on`** encodes startup order (db → backend → frontend); note it only waits for the container to *start*, not for the app inside to be ready — for stricter ordering add healthchecks + `condition: service_healthy`.
- Backend talks to Postgres via the **service name** (`postgres`) as hostname, on the **container** port (`5432`), not the host-mapped port (`5433`).
- Host port mappings (`"5433:5432"`, `"8081:8081"`, `"4200:80"`) are only needed for direct/local access or debugging; the Cloudflare Tunnel (or any reverse proxy in front) only needs to reach the frontend's host port.
- Credentials here are plaintext defaults suitable for a single-host home-lab setup. For anything internet-facing beyond a hobby project, move these into an `.env` file consumed via `env_file:` and/or Docker/Swarm secrets — see §8.

**Local run order** (documented in the README, worth keeping for any project where an image copies a pre-built artifact rather than building it in-container):
```bash
cd backend && ./mvnw clean package -DskipTests && cd ..
docker compose up --build
```

---

## 6. Jenkins Pipeline (`Jenkinsfile`)

```groovy
pipeline {
    agent {
        label 'home-server'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build Backend') {
            steps {
                dir('backend') {
                    sh './mvnw clean package -DskipTests'
                }
            }
        }

        stage('Build Frontend') {
            steps {
                dir('frontend') {
                    sh 'npm install'
                    sh 'npm run build'
                }
            }
        }

        stage('Deploy') {
            steps {
                sh 'docker compose down'
                sh 'docker compose up --build -d'
            }
        }

    }
}
```

Pattern to reuse:
- **`agent { label '<tag>' }`** pins the job to a specific Jenkins agent — in this setup, the agent *is* the deployment target (the same Ubuntu box that runs the Compose stack), so "build" and "deploy" happen on the same machine. No artifact transfer/SSH step needed.
- Build steps run **on the host**, not in Docker, producing the backend jar and the frontend's compiled assets — the subsequent `docker compose up --build` then packages those outputs into images (`COPY target/*.jar`, `COPY . .` before `npm run build` inside the frontend image runs its own separate build — see note below).
- `docker compose down` before `up --build -d` gives a clean redeploy (old containers removed, images rebuilt, new containers started detached).
- `-DskipTests` / no test stage here trades safety for a simpler pipeline — for anything less experimental, add a `Test` stage before `Deploy` and gate on it.

⚠️ Minor redundancy worth knowing about if you copy this pattern: the frontend
build happens **twice** — once by Jenkins (`npm run build`, currently unused
output) and once again inside the Docker image build (`RUN npm run build` in
the Dockerfile, using freshly `npm install`ed deps in the container). If you
want Jenkins' build step to actually be the one that ships, either drop the
Jenkins frontend build stage (let Docker do it) or change the Dockerfile to
`COPY dist/ ...` instead of rebuilding. Same logic applies to the backend if
you ever switch it to a multi-stage in-Docker Maven build.

---

## 7. CORS Configuration

Because production traffic is same-origin (Nginx proxies `/api` on the same
host/port as the SPA), CORS only actually matters for **calling the API directly**
(e.g. a request that bypasses the SPA/proxy entirely). Angular's own calls never
trigger it, in dev or prod -- see the proxy note below.

This project now guards writes with a real login (see the backend's
`SecurityConfig`), so `allowedOrigins("*")` is no longer appropriate once
credentials are involved -- browsers refuse wildcard origins together with
`allowCredentials`. CORS is configured as part of the same Spring Security
filter chain, driven by an env var:

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);
    // ...
}
```

`app.cors.allowed-origins` (`CORS_ALLOWED_ORIGINS` env var, default
`http://localhost:4200`) lists the explicit origins allowed to call the API with
credentials attached. Pattern to reuse: once an API has auth/cookies, CORS must
name real origins -- `*` and `allowCredentials(true)` are mutually exclusive by
browser spec, so wildcard CORS is a signal an API has no session/cookie auth yet.

Frontend dev-only alternative (no CORS involved on the request path at all):
Angular's `proxy.conf.json`, used only by `ng serve`, mirrors the same proxy
behavior Nginx does in prod:
```json
{
  "/api": {
    "target": "http://localhost:8081",
    "secure": false,
    "changeOrigin": true
  }
}
```
This is the more idiomatic choice for local dev — it means the frontend code
always calls a relative `/api` path (see `environment.ts`: `apiUrl: '/api'`)
in every environment, dev and prod alike, and only the *proxy target* changes.
It's also why session cookies "just work" without extra `withCredentials`
wiring on the frontend: from the browser's point of view, `ng serve`'s proxy
and Nginx both make every request same-origin.

---

## 8. Environment / Configuration Pattern

`application.properties` — every externally-configurable value has a **local
default** baked in via Spring's `${VAR:default}` placeholder syntax:

```properties
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5433/monitoring}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:monitoring}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:monitoring123}
server.port=8081
```

This means:
- Running the jar directly on a dev machine needs **zero** env vars — it falls back to sane local defaults.
- `docker-compose.yml` overrides exactly those three vars for the containerized environment, pointing at the `postgres` service instead of `localhost`.
- The **same image** works in any environment purely by changing env vars — no rebuild needed to change datasource config.

Reuse checklist for a new service:
1. Never hardcode connection strings/secrets — always `${ENV_VAR:default}` (or your framework's equivalent, e.g. `process.env.X || 'default'`).
2. Keep local defaults pointed at `localhost` with the docker-compose-mapped host port, so `mvnw spring-boot:run` / `npm start` work without any `.env` file.
3. In Compose, override with the **service name** as host and the **container-internal** port.
4. For real secrets (prod passwords, API keys), don't commit them even as "defaults" — this project's `monitoring123` default is fine for a home-lab db with no external exposure, but swap to an `.env` file (git-ignored, see §9) + `env_file:` in Compose for anything sensitive.

This project follows its own checklist for the admin login it added: `docker-compose.yml`
reads `ADMIN_USERNAME`/`ADMIN_PASSWORD` from the shell environment with local-only
defaults (`${ADMIN_USERNAME:-admin}`), and a committed `.env.example` documents what
a real deployment needs to override in a git-ignored `.env` file.

---

## 9. Ignore Files

### Root `.gitignore`
```gitignore
# IntelliJ
.idea/

# VS Code
.vscode/

# OS
.DS_Store
Thumbs.db

# Backend
backend/target/

# Frontend
frontend/node_modules/
frontend/dist/
frontend/.angular/

# Logs
*.log

.env
```

### `backend/.dockerignore`
```dockerignore
target/*
!target/backend-0.0.1-SNAPSHOT.jar

.git
.idea
```
Note the negation pattern: everything in `target/` is excluded from the Docker
build context **except** the one jar the Dockerfile actually needs. Keeps the
build context small and avoids invalidating layer cache with unrelated build
output.

### `frontend/.dockerignore`
```dockerignore
node_modules
dist
.git
.angular
```
`node_modules` is excluded because the image installs its own deps fresh
(`RUN npm install` inside the build stage) — shipping the host's
`node_modules` into the build context would be slow and platform-risky
(native modules built for the host OS, not the container's).

### `backend/.gitignore` (Spring Initializr default) and `frontend/.gitignore` (Angular CLI default)
Standard framework-generated ignores — `target/`/`dist/`/`node_modules/`,
IDE folders, OS files. Nothing custom; kept as generated.

Reuse checklist for a new service:
- [ ] Root `.gitignore` covers cross-cutting concerns: IDE folders, OS cruft, `.env`.
- [ ] Each service gets its own `.gitignore` (usually the framework's default generated one) *and* its own `.dockerignore`.
- [ ] `.dockerignore` always excludes `.git`, IDE folders, and `node_modules`/`target`/`build` — except for whatever specific build artifact the Dockerfile actually `COPY`s in (use the negation pattern like the backend does).
- [ ] Never let `.env` or credential files into the image build context or git.

---

## 10. Public Ingress — Cloudflare Tunnel

Not committed to the repo (configured on the host, outside version control),
but part of the deployment pattern:
- A `cloudflared` tunnel runs on the same Ubuntu host as the Compose stack.
- It maps a public hostname (`dashboard.shahirjalal.com`) to the frontend's local port (`4200`).
- No inbound firewall ports are opened on the home network/router — outbound-only connection from `cloudflared` to Cloudflare's edge.
- TLS termination happens at Cloudflare's edge; the tunnel-to-origin hop is plain HTTP over the tunnel.

Reuse checklist for a new project on the same host:
1. `cloudflared tunnel create <name>`
2. Add a config entry (`~/.cloudflared/config.yml`) routing the new public hostname → `http://localhost:<frontend-host-port>`.
3. Add a DNS CNAME for the hostname pointing at the tunnel (`cloudflared tunnel route dns <name> <hostname>`).
4. No changes needed to Docker Compose or Nginx — this layer is entirely external to the app stack.

---

## 11. End-to-End Checklist for a New Project

1. **Structure**: one folder per service, each with its own `Dockerfile` + `.dockerignore` + `.gitignore`.
2. **Backend Dockerfile**: slim runtime base image (`-jre`, `alpine`, `slim`, etc.), `COPY` in a pre-built artifact or use a multi-stage build, `EXPOSE` the app port.
3. **Frontend Dockerfile**: multi-stage — Node (or equivalent) build stage → static file server (Nginx) runtime stage.
4. **Nginx**: SPA fallback (`try_files ... /index.html`) + `location /api/ { proxy_pass http://<backend-service>:<port>/api/; }` using Compose service DNS name.
5. **docker-compose.yml**: one service block per container, named volume for any stateful service (DB), `restart: unless-stopped`, `depends_on` for startup order, host port mappings only where actually needed externally.
6. **Config**: every datasource/secret value uses env-var-with-default (`${VAR:default}`), Compose overrides for the containerized environment, real secrets never committed.
7. **CORS**: permissive or proxy-based for dev; rely on same-origin Nginx proxying in prod; tighten if auth/cookies are involved.
8. **Jenkinsfile**: pin to the target host as the agent if doing host-based deploy; Checkout → Build (per service) → `docker compose down && docker compose up --build -d`. Add a Test stage if the project isn't purely experimental.
9. **Ignore files**: root `.gitignore` for cross-cutting + `.env`; per-service `.gitignore`/`.dockerignore`; `.dockerignore` excludes `.git`/IDE/`node_modules` but keeps whatever artifact the Dockerfile needs.
10. **Public ingress**: Cloudflare Tunnel (or reverse proxy of choice) mapped to the frontend's host port — configured outside the repo, on the host.
