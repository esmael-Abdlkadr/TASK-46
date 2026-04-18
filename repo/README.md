# Workforce & Talent Operations Hub

**Fullstack** on-premises workforce management system with recruiting, dispatch, payments, metrics, and biometric identity verification. Backend: Java 17 / Spring Boot 3.2. Frontend: Thymeleaf server-rendered HTML. Microservice: Python 3.11 / Flask (face recognition).

## Prerequisites

- **Docker** and **Docker Compose** (required for all services)
- Ports: `8081` (backend), `5001` (face recognition), `3307` (MySQL)
- No external cloud dependencies -- fully offline/on-prem

## Quick Start

```bash
# Start all services (literal command required):
docker-compose up

# Or run in detached mode:
docker compose up -d
```

Wait ~40s for MySQL + Flyway migrations + Spring Boot startup. All 3 containers should show "healthy":

```bash
docker compose ps
```

## Demo Credentials

Seeded on first startup by `DataInitializer.java` (skipped if username already exists):

| Role | Username | Password | Dashboard URL |
|---|---|---|---|
| **Administrator** | `admin` | `admin123` | http://localhost:8081/admin/dashboard |
| **Recruiter** | `recruiter` | `recruiter123` | http://localhost:8081/recruiter/dashboard |
| **Dispatch Supervisor** | `dispatch` | `dispatch123` | http://localhost:8081/dispatch/dashboard |
| **Finance Clerk** | `finance` | `finance123` | http://localhost:8081/finance/payments |

You can create additional users under **Admin > Users** while logged in as `admin`.

## Verification

### API Verification (curl)

**Successful authentication and session check:**
```bash
# Step 1: Get CSRF token
CSRF=$(curl -s -c /tmp/wf.txt http://localhost:8081/login | grep -o 'value="[^"]*"' | head -1 | sed 's/value="//;s/"//')

# Step 2: Login
curl -s -b /tmp/wf.txt -c /tmp/wf.txt -o /dev/null \
  -X POST http://localhost:8081/login \
  --data-urlencode "username=admin" \
  --data-urlencode "password=admin123" \
  --data-urlencode "_csrf=$CSRF"

# Step 3: Call session API (should return {"authenticated":true,"username":"admin",...})
curl -s -b /tmp/wf.txt http://localhost:8081/api/v1/session
```

**Expected response:**
```json
{"authenticated":true,"username":"admin","roles":["ADMINISTRATOR"]}
```

**Unauthenticated request (should redirect to login):**
```bash
curl -v http://localhost:8081/api/v1/session
# Expected: HTTP 302 redirect to /login
```

**Authentication failure (wrong password):**
```bash
CSRF=$(curl -s -c /tmp/wf_bad.txt http://localhost:8081/login | grep -o 'value="[^"]*"' | head -1 | sed 's/value="//;s/"//')
curl -s -b /tmp/wf_bad.txt -c /tmp/wf_bad.txt -o /dev/null -w "%{http_code}" \
  -X POST http://localhost:8081/login \
  --data-urlencode "username=admin" \
  --data-urlencode "password=wrongpassword" \
  --data-urlencode "_csrf=$CSRF"
# Expected: 302 (redirect to /login?error)
```

**Payment API - create and list:**
```bash
# Create payment (requires authenticated session from Step 2 above)
curl -s -b /tmp/wf.txt -X POST http://localhost:8081/api/v1/payments \
  -H "Content-Type: application/json" \
  -d '{"referenceNumber":"VERIFY-001","amount":100.00,"channel":"CASH","location":"HQ"}'

# List payments
curl -s -b /tmp/wf.txt http://localhost:8081/api/v1/payments
```

**Error envelope - validation failure:**
```bash
curl -s -b /tmp/wf.txt -X POST http://localhost:8081/api/v1/payments \
  -H "Content-Type: application/json" -d '{}'
# Expected:
# {"status":400,"code":"VALIDATION_ERROR","message":"Validation failed","fieldErrors":[...],"timestamp":"...","path":"/api/v1/payments"}
```

### Web Verification (browser)

1. **Admin login:** Open http://localhost:8081 → login with `admin` / `admin123` → should reach `/admin/dashboard`
2. **Recruiter (seeded):** Log in with `recruiter` / `recruiter123` → should reach `/recruiter/dashboard`; `/admin/dashboard` should return 403
3. **Dispatch (seeded):** Log in with `dispatch` / `dispatch123` → should reach `/dispatch/dashboard`
4. **Finance (seeded):** Log in with `finance` / `finance123` → can open `/finance/payments`; should not access `/admin/dashboard` or `/recruiter/candidates`
5. **Optional:** Create another user under Admin > Users to confirm user management
6. **Face recognition:** Admin > Face Recognition → should show "Connected" to service on port 5001

## Acceptance Checklist (Reviewers)

- [ ] `docker-compose up` starts all 3 containers without errors
- [ ] `docker compose ps` shows `workforce-db`, `workforce-face`, `workforce-backend` all healthy
- [ ] Login with `admin` / `admin123` → `/admin/dashboard` loads (HTTP 200)
- [ ] API session endpoint returns `{"authenticated":true}`: `curl -s -b /tmp/wf.txt http://localhost:8081/api/v1/session`
- [ ] Log in as `recruiter` / `recruiter123` → `/recruiter/dashboard` loads; `/admin/dashboard` returns 403
- [ ] Unauthenticated request to `/api/v1/payments` redirects (HTTP 302)
- [ ] `./run_tests.sh` exits with status 0 (all tests pass)

## Configuration / Environment Variables

| Variable | Required | Description | Default |
|---|---|---|---|
| `APP_ENCRYPTION_KEY` | **Yes** | Base64-encoded 32-byte AES-256 key for biometric encryption. App will **refuse to start** if missing or invalid. | *(none -- must be set)* |
| `SPRING_DATASOURCE_URL` | No | JDBC URL for MySQL | `jdbc:mysql://localhost:3306/workforce_db` |
| `SPRING_DATASOURCE_USERNAME` | No | DB username | `root` |
| `SPRING_DATASOURCE_PASSWORD` | No | DB password | `root` |
| `FACE_RECOGNITION_URL` | No | URL of the Python face recognition service | `http://localhost:5001` |

Docker Compose overrides JDBC credentials to **`workforce` / `workforce`** (see `docker-compose.yml`). If you see MySQL **Access denied** after changing compose, remove the old volume once: `RUN_TESTS_RESET_DB=1 ./run_tests.sh` or `docker compose down -v && ./run_tests.sh`.
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
docker exec -it workforce-db mysql -uworkforce -pworkforce workforce_db
# admin (root): mysql -uroot -proot
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
3. Seeds demo users (see Demo Credentials): `admin`, `recruiter`, `dispatch`, `finance`

Create additional users via Admin > Users after login if needed.

## REST API

JSON API at `/api/v1/**` (CSRF disabled for API paths, session-authenticated):

| Endpoint | Method | Description |
|---|---|---|
| `/api/v1/session` | GET | Current user/role info |
| `/api/v1/payments` | GET/POST | List or create payment (idempotency key supported) |
| `/api/v1/payments/{id}` | GET | Payment detail (role-aware field masking) |
| `/api/v1/payments/{id}/refund` | POST | Process refund |
| `/api/v1/search` | POST | Unified search across 12 domains |
| `/api/v1/search/saved` | GET | List saved searches (owner-scoped) |
| `/api/v1/search/saved/{id}` | GET | Saved search detail (owner-scoped) |
| `/api/v1/exports` | GET/POST | List or queue export (owner-scoped list, validated DTO for create) |
| `/api/v1/exports/{id}` | GET | Export detail (owner-scoped) |
| `/api/v1/imports` | GET | List imports (owner-scoped) |
| `/api/v1/imports/{id}` | GET | Import detail (owner-scoped) |
| `/api/v1/metrics/{id}/versions` | GET | Metric version history |
| `/api/v1/metrics/versions/{id}/publish` | POST | Publish metric version (admin only) |
| `/api/v1/metrics/{id}/rollback` | POST | Rollback metric version (admin only) |

Error responses use a consistent envelope:
```json
{"status":404,"code":"NOT_FOUND","message":"...","timestamp":"...","path":"/api/v1/..."}
```
Validation errors include `fieldErrors`:
```json
{"status":400,"code":"VALIDATION_ERROR","message":"Validation failed","fieldErrors":["Amount is required"],"timestamp":"...","path":"/api/v1/payments"}
```
Internal errors never expose stack traces or internal details.

## Security

- **Rate limiting:** 30 req/min per authenticated user
- **Biometric encryption:** AES-256-GCM with random IV, key from environment. No hardcoded fallback -- `APP_ENCRYPTION_KEY` must be set or the application fails immediately at startup (`@PostConstruct` validation). The key is validated for correct Base64 encoding and 32-byte length.
- **Field masking:** Sensitive fields (payer name, check number, card last four) masked for non-admin roles in both API responses and Thymeleaf views via PaymentView DTO. Masking is enforced at the service/DTO layer -- Thymeleaf templates never see raw entity values.
- **Object-level authorization:** All ID-based routes (saved searches, snapshots, talent pools, exports, imports) enforce owner-or-admin access via centralized `ResourceAuthorizationService`; returns 404 for non-owner access (no existence leakage). List endpoints are owner-scoped for non-admin users; admins can see all records.
- **Import validation:** Strict header schema validation during VALIDATING phase (before any rows are processed) -- missing required columns and unknown/extra columns both fail fast with descriptive errors; row-level error reports retained. Duplicate files detected via SHA-256 fingerprint.
- **Payment idempotency:** Deterministic SHA-256 key from business fields only (no timestamp/random); accepts client-provided idempotency key; duplicate submissions return existing record
- **Domain exceptions:** Service layer uses typed exceptions (`ResourceNotFoundException`, `IllegalArgumentException`) -- no generic `RuntimeException` leakage from business logic
- **Audit logging:** All operations logged with user ID, timestamp, and workstation IP
- **RBAC:** 4 roles with URL-level and method-level security

## Migration Notes

If upgrading from a previous version:
- **Encryption key:** The `APP_ENCRYPTION_KEY` environment variable is now **mandatory** with no fallback default. The application will fail to start if this variable is missing, has invalid Base64, or does not decode to exactly 32 bytes. Set it before starting the application.
- **API error format:** The `details` field in API error responses has been renamed to `fieldErrors`. The error envelope now includes a `path` field. Update any API clients that parse error responses.
- **Import validation order:** Header schema validation now runs during the VALIDATING phase (before row processing). Previously, validation ran after the status changed to IMPORTING.

## Running Tests

All suites are orchestrated by `./run_tests.sh`. **Host Node/npm is not required** — frontend unit tests (Vitest) and Playwright E2E run inside the official Playwright Docker image (`docker/js-tests/run-js-tests.sh`). Java unit tests already run inside Docker Maven (`unit_tests/run_unit_tests.sh`).

For modes **`all`**, **`api`**, and **`js`**, the script **`cd`s to the repo**, runs **`docker compose up -d`**, then waits until **three** compose services report **healthy** before continuing — no separate manual compose step is required.

```bash
# Full suite: start stack + JUnit (Docker Maven) + curl API + Vitest + Playwright E2E (Docker):
./run_tests.sh

# Unit tests only (JUnit via Docker Maven; no full stack):
./run_tests.sh unit

# API tests only (stack started by this script, then curl from host):
./run_tests.sh api

# Vitest + Playwright E2E only (stack started first; backend at 8081 for E2E):
./run_tests.sh js

# Refresh package-lock.json without local npm (one-time / when dependencies change):
# docker run --rm -v "$PWD:/app" -w /app node:20-bookworm-slim npm install
```

### Test Coverage Scope

Playwright E2E (`e2e/*.spec.js`) exercises **login**, **admin** (dashboard / users), **recruiter** (dashboard → unified search), **finance** (payments), and **cross-role** denial where applicable.

Tests cover:
- **Security:** Route-level auth (401/403), object-level authz (IDOR prevention), cross-user isolation
- **Masking:** Admin vs non-admin field masking for API and web views
- **Idempotency:** Duplicate payment submission, client-supplied keys, deterministic key generation
- **Import:** Missing headers, unknown headers, wrong-type rows, duplicate fingerprints, validation ordering
- **Encryption:** Missing key fail-fast, invalid key length, invalid Base64
- **Search:** Cross-domain results, domain filtering, location filtering
- **Write endpoints:** Create/update for dispatch, recruiting, admin user management
- **API contracts:** Full error envelope (code + fieldErrors + path + timestamp) on all 4xx responses

Some Spring REST paths are covered both by **`API_tests/run_api_tests.sh`** (curl against the running stack) and by **`MockMvc`** tests under `src/test/java/.../controller/api/` (no TCP hop but full controller stack). Both are intentional: curl proves wiring through the servlet container; MockMvc enables tight JSON assertions with isolated test data.

Test outputs:
- `unit_tests/unit_test_results.txt` -- JUnit results
- `API_tests/api_test_results.txt` -- API test results
- `test_report.txt` -- combined summary (includes Vitest + Playwright logs when Phase 3 runs)

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
