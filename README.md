# Coolify Mini Cloud Lab

A small production-shaped Spring Boot application for learning Docker, PostgreSQL and deployment through Coolify.

For the Docker workflow you only need Git and Docker Desktop. Running Maven tests directly also requires JDK 17 with `JAVA_HOME` configured.

## What is included

- Java 17 and Spring Boot 3.5
- REST API with validation, idempotent creation and pagination
- PostgreSQL persistence managed by Flyway
- Redis cache-aside reads with a five-minute default TTL
- Actuator health checks and Prometheus metrics
- Multi-stage, non-root Docker image
- Separate Docker Compose definitions for local development and Coolify
- Persistent database volume
- PowerShell and shell smoke tests
- GitHub Actions build and test workflow

## API

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/hello` | Verify the running instance |
| `POST` | `/api/tasks` | Create a task and return `202 Accepted`; supports `Idempotency-Key` |
| `GET` | `/api/tasks/{id}` | Read a task |
| `GET` | `/api/tasks?page=0&size=20` | List tasks |
| `GET` | `/actuator/health` | Container and deployment health check |
| `GET` | `/actuator/prometheus` | Prometheus metrics |

Reusing an `Idempotency-Key` with the same request returns the original task and response header `Idempotency-Replayed: true`. Reusing it with a different request returns `409 Conflict`. PostgreSQL's unique constraint is the duplicate-prevention boundary; Redis is used only as a disposable read cache.

## Run locally with Docker Desktop

From PowerShell:

```powershell
Copy-Item .env.example .env
# Change POSTGRES_PASSWORD in .env. Never commit this file.
docker compose up -d --build
docker compose ps
./scripts/smoke-test.ps1
```

The smoke test creates a task, repeats the request with the same idempotency key, verifies that the same task is returned, and reads it back.

View logs:

```powershell
docker compose logs -f api
docker compose logs -f postgres
```

Stop the stack without deleting data:

```powershell
docker compose down
```

`docker compose down -v` also deletes the PostgreSQL volume and all lab data. Use it only when a full reset is intended.

## Run tests without Docker

Tests use an in-memory H2 database:

```powershell
.\mvnw.cmd clean verify
```

The Maven Wrapper downloads the pinned Maven version on its first run, so a separate Maven installation is not required.

## Create a standalone Git repository

This directory is self-contained. If it currently lives inside another repository, copy only `coolify-lab` to a clean directory before publishing it:

```powershell
Copy-Item -Recurse .\coolify-lab C:\dev\coolify-lab
Set-Location C:\dev\coolify-lab
git init
git add .
git commit -m "feat: bootstrap Coolify mini cloud lab"
git branch -M main
git remote add origin <YOUR_REPOSITORY_URL>
git push -u origin main
```

Do not publish this lab inside a company repository or push company code to a personal Git host.

## Deploy with Coolify

### 1. Prepare the VPS

Use a fresh Linux VPS. A practical starting point for this stack is 2 vCPU, 4 GB RAM and enough disk for images, builds, logs and backups.

Install Coolify as root:

```sh
curl -fsSL https://cdn.coollabs.io/coolify/install.sh | bash
```

Initially allow SSH and Coolify's setup ports. For public traffic allow `80/tcp` and `443/tcp`. After the dashboard works through its own HTTPS domain, close public access to `8000`, `6001` and `6002`.

### 2. Create the application

1. In Coolify, create a Project and a `production` environment.
2. Select **New resource**, connect the Git repository, and select branch `main`.
3. Select **Docker Compose** as the build strategy.
4. If this folder is inside a larger repository, set **Base Directory** to `/coolify-lab`. For a standalone repository use `/`.
5. Set **Docker Compose Location** to `/compose.coolify.yaml`.
6. Save and review the parsed services, environment variables, health checks and persistent storage.

The Coolify Compose file intentionally has no `ports:` entries. The API is exposed only through Coolify's proxy, and PostgreSQL remains private on the Compose network.

Redis is also private. Its volume enables append-only persistence, but the application treats Redis as disposable: cache failures fall back to PostgreSQL. Container liveness therefore does not depend on Redis health, while `/actuator/health` still reports Redis degradation for diagnosis.

Coolify generates and reuses these values automatically:

- `SERVICE_USER_POSTGRES`
- `SERVICE_PASSWORD_64_POSTGRES`
- `SERVICE_URL_API_8080`

The last variable routes the generated domain to service `api` on internal port `8080`.

### 3. Deploy and verify

Click **Deploy**, inspect Deployment Logs, then run:

```powershell
./scripts/smoke-test.ps1 -BaseUrl "https://your-app.example.com"
```

For a custom domain, point its DNS record at the VPS and configure the domain for service `api`, internal port `8080`. Keep the database service private.

### 4. Enable automatic deployments

Connect the repository through the provider's Git App or configure a signed webhook. Enable deploy-on-push for `main`. Push a harmless change and verify that a new deployment appears in Coolify.

### 5. Backups and rollback

- Configure scheduled PostgreSQL backups and copy them to S3-compatible storage outside the VPS.
- Perform a restore drill; a successful backup file alone is not proof that recovery works.
- Keep several application images in Coolify for rollback.
- Image rollback does not reverse Flyway migrations or restore database data.

Operational troubleshooting is documented in [docs/RUNBOOK.md](docs/RUNBOOK.md).

## Security boundaries

- Never commit `.env`, passwords, tokens, private keys or production data.
- Do not publish PostgreSQL port `5432` to the Internet.
- Restrict SSH at the provider firewall when possible.
- Keep the OS, Docker and Coolify updated.
- Persistent volumes survive container replacement, but they are not backups.

## Next phases

Current checkpoint: database-backed idempotency and Redis caching are implemented. The next phase is reliable RabbitMQ task processing with a transactional outbox, retry and dead-letter queue. After that, add Grafana dashboards, alerting, preview environments and load testing one phase at a time.
