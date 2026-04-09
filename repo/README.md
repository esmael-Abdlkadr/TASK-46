# Workforce & Talent Operations Hub

On-premises workforce management system with recruiting, dispatch, payments, metrics, and biometric identity verification.

## Prerequisites

- **Docker** and **Docker Compose** (required for all services)
- Ports: `8081` (backend), `5001` (face recognition), `3307` (MySQL)
- No external cloud dependencies -- fully offline/on-prem

## Quick Start

```bash
docker compose up -d
# Wait ~40s for MySQL + Flyway migrations + Spring Boot startup
# All 3 containers should show "healthy":
docker compose ps
```

Open http://localhost:8081 and login:
- **Username:** `admin`
- **Password:** `admin123`

## Configuration / Environment Variables

| Variable | Required | Description | Default |
|---|---|---|---|
| `APP_ENCRYPTION_KEY` | **Yes** | Base64-encoded 32-byte AES-256 key for biometric encryption. App will **refuse to start** if missing or invalid. | *(none -- must be set)* |
| `SPRING_DATASOURCE_URL` | No | JDBC URL for MySQL | `jdbc:mysql://localhost:3306/workforce_db` |
| `SPRING_DATASOURCE_USERNAME` | No | DB username | `root` |
| `SPRING_DATASOURCE_PASSWORD` | No | DB password | `root` |
| `FACE_RECOGNITION_URL` | No | URL of the Python face recognition service | `http://localhost:5001` |
| `APP_RATELIMIT_REQUESTSPERMINUTE` | No | Rate limit per user per minute | `30` |
| `APP_EXPORT_PATH` | No | Export file storage directory | `./exports` |
| `APP_IMPORT_PATH` | No | Import file storage directory | `./imports` |
| `APP_FILE_STORAGE_PATH` | No | General file storage directory | `./file-store` |

### Generating an encryption key

```bash
# Generate a random 32-byte AES-256 key (base64):
openssl rand -base64 32
```

Set in `docker-compose.yml` or as an environment variable:
```bash
export APP_ENCRYPTION_KEY="your-base64-key-here"
```

## Database Migrations

Flyway runs automatically on startup. Migrations are in `src/main/resources/db/migration/` (V1 through V13). No manual SQL execution needed.

To connect to the database directly:
```bash
docker exec -it workforce-db mysql -uroot -proot workforce_db
```

## Services Architecture

| Service | Container | Port | Technology |
|---|---|---|---|
| Backend (Spring Boot) | `workforce-backend` | 8081 | Java 17, Spring Boot 3.2 |
| Face Recognition | `workforce-face` | 5001 | Python 3.11, Flask |
| Database | `workforce-db` | 3307 | MySQL 8.0 |

## Admin Bootstrap

The first startup automatically:
1. Runs all Flyway migrations (creates 43+ tables)
2. Seeds 4 roles: ADMINISTRATOR, RECRUITER, DISPATCH_SUPERVISOR, FINANCE_CLERK
3. Creates admin user: `admin` / `admin123`

Create additional users via Admin > Users after login.

## REST API

JSON API at `/api/v1/**` (CSRF disabled for API paths, session-authenticated):

| Endpoint | Method | Description |
|---|---|---|
| `/api/v1/session` | GET | Current user/role info |
| `/api/v1/payments` | GET/POST | List or create payment (idempotency key supported) |
| `/api/v1/payments/{id}` | GET | Payment detail (role-aware field masking) |
| `/api/v1/payments/{id}/refund` | POST | Process refund |
| `/api/v1/search` | POST | Unified search across 12 domains |
| `/api/v1/exports` | GET/POST | List or queue export |
| `/api/v1/exports/{id}` | GET | Export detail (owner-scoped) |
| `/api/v1/imports/{id}` | GET | Import detail (owner-scoped) |
| `/api/v1/metrics/{id}/versions` | GET | Metric version history |
| `/api/v1/metrics/versions/{id}/publish` | POST | Publish metric version (admin only) |
| `/api/v1/metrics/{id}/rollback` | POST | Rollback metric version (admin only) |

Error responses use structured JSON: `{"status":404,"code":"NOT_FOUND","message":"...","timestamp":"..."}`.

## Security

- **Rate limiting:** 30 req/min per authenticated user
- **Biometric encryption:** AES-256-GCM with random IV, key from environment (no hardcoded fallback)
- **Field masking:** Sensitive fields (payer name, check number) masked for non-admin roles via DTO transformation
- **Object-level authorization:** User-created resources (saved searches, exports, imports, talent pools) are owner-scoped; admins see all
- **Audit logging:** All operations logged with user ID, timestamp, and workstation IP
- **RBAC:** 4 roles with URL-level and method-level security

## Running Tests

```bash
# Full suite (unit + API):
./run_tests.sh

# Unit tests only:
./run_tests.sh unit

# API tests only:
./run_tests.sh api
```

Test outputs:
- `unit_tests/unit_test_results.txt` -- JUnit results
- `API_tests/api_test_results.txt` -- API test results
- `test_report.txt` -- combined summary

## Project Structure

```
repo/
  docker-compose.yml          # 3-service Docker setup
  Dockerfile                  # Spring Boot multi-stage build
  run_tests.sh                # Unified test runner
  unit_tests/                 # JUnit test runner script
  API_tests/                  # curl-based API test scripts
  face-recognition-service/   # Python Flask service
  src/main/
    java/.../workforce/
      config/                 # Security, async, rate limiting
      controller/             # Thymeleaf controllers
      controller/api/         # REST /api/v1 controllers
      crypto/                 # AES-256-GCM biometric encryption
      dto/                    # Request/response DTOs
      entity/                 # JPA entities (43+ tables)
      enums/                  # Business enums
      exception/              # Custom exceptions + REST error handler
      masking/                # Role-aware field masking
      repository/             # Spring Data JPA repositories
      service/                # Business logic layer
    resources/
      db/migration/           # Flyway V1-V13
      templates/              # Thymeleaf HTML templates
```
