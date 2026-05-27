# HMCTS Task Management System

A full-stack task management application for HMCTS caseworkers, built using the HMCTS technology stack.

## Architecture

```
┌─────────────────┐     HTTP      ┌──────────────────────┐     JDBC     ┌──────────┐
│  Node.js / Exp  │ ──────────▶  │  Java / Spring Boot  │ ──────────▶ │ Postgres │
│  Frontend :3000 │              │  Backend API :8080    │             └──────────┘
└─────────────────┘              └──────────────────────┘     Cache    ┌──────────┐
                                                               ──────▶ │  Redis   │
                                                                        └──────────┘
```

| Layer          | Technology                          |
|----------------|-------------------------------------|
| Backend API    | Java 17, Spring Boot 3, Gradle      |
| Frontend       | Node.js 20, Express, Nunjucks       |
| Database       | PostgreSQL 16                       |
| Cache          | Redis 7                             |
| Containers     | Docker, Docker Compose              |
| Orchestration  | Kubernetes (Azure AKS)              |
| CI/CD          | Jenkins                             |
| IaC            | Terraform                           |
| Cloud          | Azure                               |
| Monitoring     | Dynatrace                           |
| UI Framework   | GOV.UK Frontend                     |

## API Endpoints

| Method   | Path                     | Description                     |
|----------|--------------------------|---------------------------------|
| `POST`   | `/api/tasks`             | Create a new task               |
| `GET`    | `/api/tasks`             | Retrieve all tasks (by due date)|
| `GET`    | `/api/tasks/{id}`        | Retrieve a task by ID           |
| `PATCH`  | `/api/tasks/{id}/status` | Update the status of a task     |
| `DELETE` | `/api/tasks/{id}`        | Delete a task                   |

Full OpenAPI documentation is served at `/swagger-ui.html` when the backend is running.

### Task model

```json
{
  "id":          "uuid",
  "title":       "string (required)",
  "description": "string (optional)",
  "status":      "TODO | IN_PROGRESS | DONE",
  "dueDate":     "ISO-8601 datetime (required, must be future)",
  "createdAt":   "ISO-8601 datetime",
  "updatedAt":   "ISO-8601 datetime"
}
```

## Quick start (Docker Compose)

**Prerequisites:** Docker, Docker Compose v2

```bash
# Clone the repo
git clone <repo-url>
cd dts-developer-challenge

# Start all services (Postgres, Redis, backend, frontend)
docker-compose up --build

# Frontend → http://localhost:3000
# Backend API → http://localhost:8080
# Swagger UI → http://localhost:8080/swagger-ui.html
```

## Local development

### Backend (Java / Spring Boot)

```bash
cd backend

# Run tests
./gradlew test

# Run with coverage report
./gradlew test jacocoTestReport
# Report: build/reports/jacoco/test/html/index.html

# Start (requires Postgres + Redis running)
./gradlew bootRun
```

### Frontend (Node.js)

```bash
cd frontend

# Install dependencies
npm install

# Copy environment config
cp .env.example .env

# Run tests
npm test

# Start in dev mode (hot-reload)
npm run dev
```

## Project structure

```
dts-developer-challenge/
├── backend/                    # Java Spring Boot API
│   ├── src/
│   │   ├── main/java/uk/gov/hmcts/tasks/
│   │   │   ├── controller/     # REST controllers
│   │   │   ├── service/        # Business logic + Redis caching
│   │   │   ├── repository/     # JPA repositories
│   │   │   ├── model/          # JPA entities + TaskStatus enum
│   │   │   ├── dto/            # Request/response DTOs
│   │   │   ├── exception/      # Custom exceptions + global handler
│   │   │   └── config/         # Redis cache configuration
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/   # Flyway SQL migrations
│   └── build.gradle
│
├── frontend/                   # Node.js Express app
│   ├── src/
│   │   ├── app.js              # Express application entry point
│   │   ├── routes/tasks.js     # Route handlers with validation
│   │   ├── services/taskService.js  # HTTP client to backend API
│   │   └── views/              # Nunjucks templates (GOV.UK Frontend)
│   ├── test/routes/tasks.test.js
│   └── package.json
│
├── kubernetes/                 # K8s manifests for AKS
│   ├── backend/
│   ├── frontend/
│   └── redis/
│
├── infrastructure/             # Terraform for Azure
│   ├── main.tf                 # AKS, Postgres, Redis modules
│   ├── variables.tf
│   ├── outputs.tf
│   └── modules/
│       ├── aks/
│       ├── postgres/
│       └── redis/
│
├── Jenkinsfile                 # CI/CD pipeline
└── docker-compose.yml          # Local development stack
```

## CI/CD (Jenkins)

The `Jenkinsfile` pipeline:

1. **Test** — runs backend JUnit tests with JaCoCo coverage and frontend Jest tests
2. **Security scan** — OWASP Dependency Check (backend) + `npm audit` (frontend)
3. **Docker build** — builds and tags images for both services
4. **Push to ACR** — pushes images to Azure Container Registry
5. **Terraform plan/apply** — provisions/updates Azure infrastructure (with manual approval gate for production)
6. **Deploy to AKS** — rolling update via `kubectl set image` + rollout wait
7. **Dynatrace event** — registers a deployment event for traceability in Dynatrace

## Infrastructure (Terraform)

```bash
cd infrastructure
terraform init
terraform plan -var="environment=dev" -var="subscription_id=<your-id>"
terraform apply
```

Resources provisioned:
- **Azure Resource Group**
- **Azure Kubernetes Service (AKS)** — 2-node cluster
- **Azure Database for PostgreSQL Flexible Server**
- **Azure Cache for Redis**

## Monitoring (Dynatrace)

Dynatrace is integrated in two ways:

- **Kubernetes**: Dynatrace OneAgent DaemonSet (deployed separately via the Dynatrace Operator) auto-instruments the Spring Boot JVM and Node.js processes.
- **Metrics**: The Spring Boot backend exposes a Prometheus `/actuator/prometheus` endpoint, scraped by Dynatrace via the annotations on the backend `Deployment`.
- **Deployments**: Jenkins posts a `CUSTOM_DEPLOYMENT` event to the Dynatrace API after each successful production deploy.

## Design decisions

- **Flyway** for DB migrations — schema changes are versioned alongside the code.
- **Redis caching** with 10-minute TTL — reduces Postgres load for the task list and individual lookups. Cache is evicted on any write.
- **GOV.UK Frontend** — consistent with HMCTS design standards for internal tooling.
- **ProblemDetail (RFC 9457)** — structured JSON error responses from the Spring Boot API.
- **Non-root containers** — both Dockerfiles run as a dedicated `hmcts` user.
- **Readiness/liveness probes** — Kubernetes uses the Spring Boot Actuator health endpoint to manage pod lifecycle safely.
