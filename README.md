# Server Monitoring Dashboard

A full-stack server monitoring dashboard built with Angular, Spring Boot, PostgreSQL, Docker, and Jenkins.

## Features

- View deployed applications
- Add applications
- Delete applications
- REST API with Spring Boot
- PostgreSQL database
- Dockerized frontend and backend
- Docker Compose orchestration
- Nginx reverse proxy
- Jenkins CI/CD pipeline
- Deployable to Ubuntu Server

---

## Tech Stack

### Frontend
- Angular 18
- Bootstrap
- TypeScript

### Backend
- Java 17
- Spring Boot 3.5
- Spring Data JPA

### Database
- PostgreSQL 16

### DevOps
- Docker
- Docker Compose
- Nginx
- Jenkins

---

## Architecture

```
Browser
    │
    ▼
Angular (Nginx)
    │
   /api
    │
    ▼
Spring Boot
    │
    ▼
PostgreSQL
```

---

## Project Structure

```
server-monitoring-dashboard
│
├── backend
├── frontend
├── docker-compose.yml
└── Jenkinsfile
```

---

## Run locally

```bash
docker compose up --build
```

Frontend

```
http://localhost:4200
```

Backend

```
http://localhost:8081
```

---

## Future Improvements

- Authentication
- Server health monitoring
- CPU & RAM statistics
- Docker container monitoring
- Charts
- User management