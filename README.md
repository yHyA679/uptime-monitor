# Uptime Monitor

A full-stack uptime monitoring application built as a production-oriented learning project. It performs bounded concurrent HTTP checks, records immutable monitoring history, applies configurable failure and recovery thresholds, tracks incidents, stores alert events, and exposes dashboard and public-status APIs.

## What it demonstrates

- Spring Boot REST API design with validation and consistent errors
- PostgreSQL persistence with Flyway migrations and indexed history queries
- Realistic monitoring state transitions rather than a single-check status flip
- A bounded scheduler that prevents overlapping checks for the same website
- Incident and alert creation only on confirmed status transitions
- History retention, pagination, filtering, sorting, and aggregate statistics
- Actuator health probes, metrics, structured logging, Docker, and CI
- A React and TypeScript dashboard in `frontend/`

## Architecture

```text
React frontend
      |
REST controllers
      |
application services ---- public/dashboard projections
      |
check coordinator ---- bounded worker pool
      |                       |
      +---- WebsiteService HTTP check
                       |
               short persistence transaction
                       |
     Website / MonitoringResult / Incident / AlertEvent
                       |
                   PostgreSQL
```

The scheduler scans enabled websites every few seconds and submits only websites whose configured interval has elapsed. The coordinator keeps an in-memory set of checks in progress, so a manual and scheduled check cannot overlap for the same website in one application instance. Worker and queue limits provide backpressure when many websites become due together.

The network request happens outside the database transaction. Once it completes, the website state, raw monitoring result, incident transition, and alert event are persisted atomically.

## Monitoring state machine

- Every HTTP attempt creates a raw `MonitoringResult` with observed `UP` or `DOWN`, response time, HTTP status when available, timestamp, and failure reason.
- A website becomes `DOWN` only after `failureThreshold` consecutive failed attempts.
- A down website becomes `UP` only after `recoveryThreshold` consecutive successful attempts.
- An incident opens only on a confirmed `UP -> DOWN` transition.
- The open incident resolves and a recovery alert is recorded on `DOWN -> UP`.
- Repeated failures while already down do not create duplicate incidents or alerts.

HTTP responses from 200 through 399 are considered reachable. DNS, timeout, refused connection, TLS, unsafe target, other network errors, and non-success HTTP responses are recorded distinctly where Java exposes enough information.

## Security boundary

Monitoring arbitrary URLs creates an SSRF risk. By default, checks reject loopback, private, link-local, multicast, unspecified, carrier-grade NAT, and IPv6 unique-local destinations. URLs containing credentials are rejected and redirects are not followed. Set `MONITORING_ALLOW_PRIVATE_TARGETS=true` only in a trusted environment where internal monitoring is intentional.

This portfolio version does not include authentication. Treat the management API as an internal API and do not expose it directly to the internet. The public status endpoint is read-only and uses dedicated DTOs that omit internal IDs and configuration.

## Requirements

- Java 17
- Docker and Docker Compose for the recommended setup
- Node.js 22 and npm for frontend development

## Run with Docker Compose

Copy the example environment file and set a real local password:

```bash
cp .env.example .env
```

Then start the backend and PostgreSQL:

```bash
docker compose up --build
```

The API is available at `http://localhost:8080`. PostgreSQL is reachable only over the internal Compose network and its data is retained in the `uptime-postgres-data` volume.

Run the frontend separately for development:

```bash
cd frontend
npm ci
npm run dev
```

Vite runs at `http://localhost:5173` and proxies `/api` to the backend.

## Run locally without Docker

Start PostgreSQL and provide the required database password:

```bash
export DB_PASSWORD=your-password
./mvnw spring-boot:run
```

On Windows PowerShell, set `$env:DB_PASSWORD` and run `.\mvnw.cmd spring-boot:run`.

## Configuration

| Environment variable | Default | Purpose |
| --- | ---: | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/uptime_monitor` | JDBC connection URL |
| `DB_USERNAME` | `postgres` | Database username |
| `DB_PASSWORD` | required | Database password |
| `APP_PORT` | `8080` | Host port used by Compose |
| `MONITORING_WORKER_THREADS` | `4` | Maximum simultaneous scheduled checks |
| `MONITORING_QUEUE_CAPACITY` | `100` | Maximum queued scheduled checks |
| `MONITORING_SCHEDULER_DELAY_MS` | `10000` | Delay between due-work scans |
| `MONITORING_CONNECT_TIMEOUT_MS` | `5000` | TCP connection timeout |
| `MONITORING_READ_TIMEOUT_MS` | `5000` | HTTP response timeout |
| `MONITORING_RETENTION_DAYS` | `30` | Monitoring-result retention |
| `MONITORING_RETENTION_CLEANUP_DELAY_MS` | `86400000` | Cleanup frequency |
| `MONITORING_ALLOW_PRIVATE_TARGETS` | `false` | Allow intentional internal targets |
| `JPA_SHOW_SQL` | `false` | Enable SQL logging for local debugging |

Monitoring intervals are validated between 10 and 3600 seconds. Failure and recovery thresholds are validated between 1 and 10.

## Main API endpoints

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/api/websites` | Create a monitored website |
| `GET` | `/api/websites` | List, filter, sort, and optionally paginate websites |
| `GET` | `/api/websites/{id}` | Read one monitored website |
| `PATCH` | `/api/websites/{id}` | Update monitoring settings |
| `DELETE` | `/api/websites/{id}` | Delete a website and related records |
| `POST` | `/api/websites/{id}/check` | Run a manual check |
| `GET` | `/api/websites/{id}/history` | Read newest monitoring results |
| `GET` | `/api/websites/{id}/stats` | Read aggregate uptime statistics |
| `GET` | `/api/websites/{id}/incidents` | Read incident history |
| `GET` | `/api/websites/{id}/alerts` | Read alert history |
| `GET` | `/api/dashboard/summary` | Read dashboard aggregates |
| `GET` | `/api/public/status` | Read public website status DTOs |
| `GET` | `/actuator/health/readiness` | Database-aware readiness probe |
| `GET` | `/actuator/health/liveness` | Process liveness probe |
| `GET` | `/actuator/metrics` | Available application metrics |

Growing collection endpoints accept `page` and `size`; page size is capped at 100. Website filters include `status`, `enabled`, `tag`, and `name`. Supported sort fields include `name`, `status`, `uptimePercentage`, `averageResponseTime`, and `lastChecked`.

## Example request

```http
POST /api/websites
Content-Type: application/json

{
  "name": "Example",
  "url": "https://example.com",
  "checkIntervalSeconds": 60,
  "enabled": true,
  "failureThreshold": 3,
  "recoveryThreshold": 2
}
```

Errors consistently contain `timestamp`, `status`, `error`, `message`, and `path`.

## Tests and CI

Run the backend suite:

```bash
./mvnw test
```

Run the frontend build:

```bash
cd frontend
npm ci
npm run build
```

GitHub Actions caches Maven and npm dependencies, runs unit tests, runs integration tests against PostgreSQL, packages the jar, builds the frontend, validates Compose, builds the Docker image, and uploads test reports even when a test fails.

## Schema migrations

Flyway owns the production schema and Hibernate validates it at startup. A new empty database receives `V1__initial_schema.sql`. An older non-empty development database without Flyway metadata is baselined automatically so existing Docker volumes can continue to start; recreate disposable local volumes if you want the complete migration-created constraint set.

## Deliberate non-goals

- No Kafka, Redis, or external job queue
- No Kubernetes deployment
- No fake SMTP setup; email delivery remains behind a notification interface
- No distributed scheduler lock; this project is designed for one application instance
- No authentication in this portfolio scope; protect management endpoints at the deployment boundary
