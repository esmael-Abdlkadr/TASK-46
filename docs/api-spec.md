# Workforce & Talent Operations Hub -- API Specification

| Property       | Value                                      |
|----------------|--------------------------------------------|
| Version        | 1.0.0                                      |
| Last Updated   | 2026-04-08                                 |
| Backend        | Spring Boot (Java 17)                      |
| Database       | MySQL 8.x                                  |
| Deployment     | Fully offline, on-premise                  |
| Consumers      | HTML / CSS / JS frontend (same origin)     |
| Auxiliary      | Python face-recognition service (LAN)      |

---

## Table of Contents

1. [General Conventions](#1-general-conventions)
2. [Authentication & Sessions](#2-authentication--sessions)
3. [Users & Roles (Admin)](#3-users--roles-admin)
4. [Dashboard](#4-dashboard)
5. [Candidate Profiles](#5-candidate-profiles)
6. [Job Profiles](#6-job-profiles)
7. [Talent Pools](#7-talent-pools)
8. [Pipeline Management](#8-pipeline-management)
9. [Saved Searches & Snapshots](#9-saved-searches--snapshots)
10. [Collectors & Sites (Dispatch)](#10-collectors--sites-dispatch)
11. [Shifts & Scheduling](#11-shifts--scheduling)
12. [Dispatch Jobs](#12-dispatch-jobs)
13. [Unified Search](#13-unified-search)
14. [Exports](#14-exports)
15. [Master Data (Admin)](#15-master-data-admin)
16. [Bulk Import](#16-bulk-import)
17. [Metrics Semantic Layer](#17-metrics-semantic-layer)
18. [Payments](#18-payments)
19. [Bank File Import & Reconciliation](#19-bank-file-import--reconciliation)
20. [Face Recognition](#20-face-recognition)
21. [Async Jobs](#21-async-jobs)
22. [Audit Log](#22-audit-log)
23. [Health & Monitoring](#23-health--monitoring)
24. [Appendix A -- Permission Matrix](#appendix-a--permission-matrix)
25. [Appendix B -- Pipeline Stage Transitions](#appendix-b--pipeline-stage-transitions)
26. [Appendix C -- Dispatch Job State Machine](#appendix-c--dispatch-job-state-machine)
27. [Appendix D -- Payment State Machine](#appendix-d--payment-state-machine)
28. [Appendix E -- Rate Limit Headers](#appendix-e--rate-limit-headers)

---

## 1. General Conventions

### 1.1 Base URL

All endpoints are prefixed with:

```
/api/v1
```

### 1.2 Authentication

Session-based authentication. Upon successful login the server sets an `HttpOnly`, `Secure`, `SameSite=Strict` cookie named `WSESSION`. Every subsequent request must include this cookie. No Bearer tokens are used.

### 1.3 Rate Limiting

- **Limit:** 30 requests per minute per authenticated user.
- Rate limit state is tracked server-side by user ID.
- When the limit is exceeded the server returns `429 Too Many Requests`.
- See [Appendix E](#appendix-e--rate-limit-headers) for response headers.

### 1.4 Standard Error Envelope

All error responses use the following shape:

```json
{
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Human-readable summary of the problem.",
    "details": {
      "fields": {
        "email": "Must be a valid email address."
      }
    }
  }
}
```

Common error codes referenced throughout this document:

| HTTP Status | Code                    | Meaning                                       |
|-------------|-------------------------|-----------------------------------------------|
| 400         | `BAD_REQUEST`           | Malformed JSON or missing required fields      |
| 401         | `UNAUTHORIZED`          | Missing or expired session                     |
| 403         | `FORBIDDEN`             | Authenticated but insufficient role/permission |
| 404         | `NOT_FOUND`             | Resource does not exist or has been soft-deleted|
| 409         | `CONFLICT`              | Duplicate resource or concurrent modification  |
| 422         | `UNPROCESSABLE_ENTITY`  | Semantically invalid (e.g., invalid state transition) |
| 429         | `RATE_LIMIT_EXCEEDED`   | Too many requests                              |

### 1.5 Pagination

All list endpoints accept page-based pagination parameters and return a pagination wrapper.

| Parameter | Type    | Default | Max | Description                |
|-----------|---------|---------|-----|----------------------------|
| `page`    | integer | 0       | --  | Zero-based page index      |
| `size`    | integer | 25      | 100 | Number of items per page   |

Response wrapper:

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 25,
  "totalPages": 4,
  "totalElements": 87
}
```

### 1.6 Soft Delete

All deletable resources use a `deleted_at` timestamp rather than physical removal. Deleted resources are excluded from list queries by default. Admins may pass `?includeDeleted=true` to see them.

### 1.7 Field Masking

Sensitive fields (SSN, bank account numbers, government IDs) are masked for non-Administrator roles. Only the last 4 characters are visible; the rest are replaced with asterisks.

Example for a Recruiter viewing a candidate SSN: `"ssn": "***-**-7890"`.

### 1.8 Async Jobs

Long-running operations (exports, bulk imports, settlement generation) return `202 Accepted` with a `job_id`. Clients poll `GET /api/v1/jobs/:id` until status reaches `completed` or `failed`. There is a 90-second server-side timeout per job step.

```json
{
  "jobId": "job_8f3a1c",
  "status": "queued",
  "pollUrl": "/api/v1/jobs/job_8f3a1c"
}
```

### 1.9 Timestamps

All timestamps are ISO-8601 in UTC: `2026-04-08T14:30:00Z`.

### 1.10 Content Types

- Request bodies: `application/json` unless noted (file uploads use `multipart/form-data`).
- Responses: `application/json` unless noted (file downloads use `application/octet-stream`).

---

## 2. Authentication & Sessions

### 2.1 POST /api/v1/auth/login

Log in with username and password. On success sets the `WSESSION` HttpOnly cookie.

**Required Role:** None (public).

**Request Body:**

| Field      | Type   | Required | Description         |
|------------|--------|----------|---------------------|
| `username` | string | Yes      | Account username    |
| `password` | string | Yes      | Account password    |

**Response 200:**

```json
{
  "userId": "usr_001",
  "username": "jdoe",
  "displayName": "Jane Doe",
  "roles": ["RECRUITER"],
  "permissions": ["candidates:read", "candidates:write", "jobs:read"],
  "sessionExpiresAt": "2026-04-08T15:30:00Z"
}
```

**Error Codes:**

| Status | Code               | When                                       |
|--------|--------------------|---------------------------------------------|
| 400    | `BAD_REQUEST`      | Missing username or password                |
| 401    | `UNAUTHORIZED`     | Invalid credentials                         |
| 403    | `FORBIDDEN`        | Account locked                              |
| 429    | `RATE_LIMIT_EXCEEDED` | More than 5 failed attempts in 15 minutes |

**Notes:**
- After 5 consecutive failed login attempts the account is locked for 15 minutes.
- Login rate limiting is separate from the global 30 req/min limit and is tracked per username.
- The response does not include the session token; it is transmitted only via the `Set-Cookie` header.

---

### 2.2 POST /api/v1/auth/logout

Destroy the current session and clear the cookie.

**Required Role:** Any authenticated user.

**Request Body:** None.

**Response 204:** No content.

**Error Codes:**

| Status | Code           | When                      |
|--------|----------------|---------------------------|
| 401    | `UNAUTHORIZED` | No active session         |

**Notes:**
- Idempotent. Calling logout when already logged out returns 401.

---

### 2.3 GET /api/v1/auth/session

Return information about the current session.

**Required Role:** Any authenticated user.

**Request Params:** None.

**Response 200:**

```json
{
  "userId": "usr_001",
  "username": "jdoe",
  "displayName": "Jane Doe",
  "roles": ["RECRUITER"],
  "permissions": ["candidates:read", "candidates:write", "jobs:read"],
  "sessionCreatedAt": "2026-04-08T14:00:00Z",
  "sessionExpiresAt": "2026-04-08T15:30:00Z",
  "lastActivityAt": "2026-04-08T14:25:00Z"
}
```

**Error Codes:**

| Status | Code           | When                      |
|--------|----------------|---------------------------|
| 401    | `UNAUTHORIZED` | No active session         |

---

### 2.4 POST /api/v1/auth/change-password

Change the password of the currently authenticated user.

**Required Role:** Any authenticated user.

**Request Body:**

| Field             | Type   | Required | Description                       |
|-------------------|--------|----------|-----------------------------------|
| `currentPassword` | string | Yes      | Current password for verification |
| `newPassword`     | string | Yes      | New password (min 12 chars, must include upper, lower, digit, special) |

**Response 200:**

```json
{
  "message": "Password changed successfully.",
  "sessionExpiresAt": "2026-04-08T15:30:00Z"
}
```

**Error Codes:**

| Status | Code                   | When                                  |
|--------|------------------------|----------------------------------------|
| 400    | `BAD_REQUEST`          | Missing fields                         |
| 401    | `UNAUTHORIZED`         | No active session                      |
| 422    | `UNPROCESSABLE_ENTITY` | New password does not meet complexity requirements or matches a recent password |

**Notes:**
- The last 5 passwords are remembered and cannot be reused.
- All other active sessions for the user are invalidated after a password change.

---

## 3. Users & Roles (Admin)

### 3.1 POST /api/v1/users

Create a new user account.

**Required Role:** Administrator.

**Request Body:**

| Field        | Type     | Required | Description                              |
|--------------|----------|----------|------------------------------------------|
| `username`   | string   | Yes      | Unique login name                        |
| `email`      | string   | Yes      | Email address                            |
| `displayName`| string   | Yes      | Full display name                        |
| `roleIds`    | string[] | Yes      | One or more role IDs to assign           |
| `password`   | string   | Yes      | Initial password (must meet complexity)  |

**Response 201:**

```json
{
  "id": "usr_012",
  "username": "msmith",
  "email": "msmith@company.local",
  "displayName": "Maria Smith",
  "roles": [
    { "id": "role_02", "name": "RECRUITER" }
  ],
  "locked": false,
  "createdAt": "2026-04-08T14:30:00Z",
  "updatedAt": "2026-04-08T14:30:00Z",
  "deletedAt": null
}
```

**Error Codes:**

| Status | Code                   | When                                  |
|--------|------------------------|----------------------------------------|
| 400    | `BAD_REQUEST`          | Missing required fields                |
| 403    | `FORBIDDEN`            | Non-admin caller                       |
| 409    | `CONFLICT`             | Username or email already exists       |
| 422    | `UNPROCESSABLE_ENTITY` | Password does not meet complexity      |

---

### 3.2 GET /api/v1/users

List all users with pagination.

**Required Role:** Administrator.

**Query Parameters:**

| Param            | Type    | Required | Description                      |
|------------------|---------|----------|----------------------------------|
| `page`           | integer | No       | Page index (default 0)           |
| `size`           | integer | No       | Page size (default 25, max 100)  |
| `search`         | string  | No       | Filter by username or display name |
| `roleId`         | string  | No       | Filter by role                   |
| `locked`         | boolean | No       | Filter by lock status            |
| `includeDeleted` | boolean | No       | Include soft-deleted users        |

**Response 200:**

```json
{
  "content": [
    {
      "id": "usr_001",
      "username": "jdoe",
      "email": "jdoe@company.local",
      "displayName": "Jane Doe",
      "roles": [{ "id": "role_02", "name": "RECRUITER" }],
      "locked": false,
      "createdAt": "2026-01-15T09:00:00Z",
      "updatedAt": "2026-03-20T11:00:00Z",
      "deletedAt": null
    }
  ],
  "page": 0,
  "size": 25,
  "totalPages": 1,
  "totalElements": 8
}
```

**Error Codes:**

| Status | Code        | When             |
|--------|-------------|------------------|
| 403    | `FORBIDDEN` | Non-admin caller |

---

### 3.3 GET /api/v1/users/:id

Retrieve a single user by ID.

**Required Role:** Administrator.

**Path Parameters:**

| Param | Type   | Description |
|-------|--------|-------------|
| `id`  | string | User ID     |

**Response 200:**

```json
{
  "id": "usr_001",
  "username": "jdoe",
  "email": "jdoe@company.local",
  "displayName": "Jane Doe",
  "roles": [{ "id": "role_02", "name": "RECRUITER" }],
  "locked": false,
  "createdAt": "2026-01-15T09:00:00Z",
  "updatedAt": "2026-03-20T11:00:00Z",
  "deletedAt": null
}
```

**Error Codes:**

| Status | Code        | When                 |
|--------|-------------|----------------------|
| 403    | `FORBIDDEN` | Non-admin caller     |
| 404    | `NOT_FOUND` | User does not exist  |

---

### 3.4 PUT /api/v1/users/:id

Update user details.

**Required Role:** Administrator.

**Path Parameters:**

| Param | Type   | Description |
|-------|--------|-------------|
| `id`  | string | User ID     |

**Request Body:**

| Field         | Type     | Required | Description            |
|---------------|----------|----------|------------------------|
| `email`       | string   | No       | Updated email          |
| `displayName` | string   | No       | Updated display name   |
| `roleIds`     | string[] | No       | Replacement role set   |

**Response 200:** Returns the updated user object (same shape as GET).

**Error Codes:**

| Status | Code                   | When                            |
|--------|------------------------|---------------------------------|
| 400    | `BAD_REQUEST`          | Malformed body                  |
| 403    | `FORBIDDEN`            | Non-admin caller                |
| 404    | `NOT_FOUND`            | User does not exist             |
| 409    | `CONFLICT`             | Email already taken             |

---

### 3.5 DELETE /api/v1/users/:id

Soft-delete a user.

**Required Role:** Administrator.

**Path Parameters:**

| Param | Type   | Description |
|-------|--------|-------------|
| `id`  | string | User ID     |

**Response 204:** No content.

**Error Codes:**

| Status | Code        | When                                    |
|--------|-------------|-----------------------------------------|
| 403    | `FORBIDDEN` | Non-admin caller or deleting own account|
| 404    | `NOT_FOUND` | User does not exist                     |

**Notes:**
- Administrators cannot soft-delete their own account.
- Soft-deleted users cannot log in.

---

### 3.6 POST /api/v1/users/:id/lock

Lock a user account, preventing login.

**Required Role:** Administrator.

**Path Parameters:**

| Param | Type   | Description |
|-------|--------|-------------|
| `id`  | string | User ID     |

**Request Body:**

| Field    | Type   | Required | Description           |
|----------|--------|----------|-----------------------|
| `reason` | string | No       | Reason for locking    |

**Response 200:**

```json
{
  "id": "usr_001",
  "locked": true,
  "lockedAt": "2026-04-08T14:35:00Z",
  "lockedReason": "Policy violation"
}
```

**Error Codes:**

| Status | Code                   | When                      |
|--------|------------------------|---------------------------|
| 403    | `FORBIDDEN`            | Non-admin or self-lock    |
| 404    | `NOT_FOUND`            | User does not exist       |
| 422    | `UNPROCESSABLE_ENTITY` | Account already locked    |

---

### 3.7 POST /api/v1/users/:id/unlock

Unlock a previously locked account.

**Required Role:** Administrator.

**Path Parameters:**

| Param | Type   | Description |
|-------|--------|-------------|
| `id`  | string | User ID     |

**Response 200:**

```json
{
  "id": "usr_001",
  "locked": false,
  "unlockedAt": "2026-04-08T14:40:00Z"
}
```

**Error Codes:**

| Status | Code                   | When                      |
|--------|------------------------|---------------------------|
| 403    | `FORBIDDEN`            | Non-admin caller          |
| 404    | `NOT_FOUND`            | User does not exist       |
| 422    | `UNPROCESSABLE_ENTITY` | Account is not locked     |

---

### 3.8 POST /api/v1/roles

Create a new role.

**Required Role:** Administrator.

**Request Body:**

| Field         | Type     | Required | Description                        |
|---------------|----------|----------|------------------------------------|
| `name`        | string   | Yes      | Unique role name (e.g., `FINANCE_CLERK`) |
| `description` | string   | No       | Human-readable description         |
| `permissions` | string[] | Yes      | List of permission strings         |

**Response 201:**

```json
{
  "id": "role_05",
  "name": "FINANCE_CLERK",
  "description": "Handles payments and reconciliation",
  "permissions": ["payments:read", "payments:write", "bank-files:read", "bank-files:write"],
  "createdAt": "2026-04-08T14:45:00Z",
  "updatedAt": "2026-04-08T14:45:00Z"
}
```

**Error Codes:**

| Status | Code        | When                    |
|--------|-------------|-------------------------|
| 403    | `FORBIDDEN` | Non-admin caller        |
| 409    | `CONFLICT`  | Role name already exists|

---

### 3.9 GET /api/v1/roles

List all roles.

**Required Role:** Administrator.

**Response 200:**

```json
{
  "content": [
    {
      "id": "role_01",
      "name": "ADMINISTRATOR",
      "description": "Full system access",
      "permissions": ["*"],
      "createdAt": "2026-01-01T00:00:00Z",
      "updatedAt": "2026-01-01T00:00:00Z"
    }
  ],
  "page": 0,
  "size": 25,
  "totalPages": 1,
  "totalElements": 4
}
```

---

### 3.10 GET /api/v1/roles/:id

Retrieve a single role.

**Required Role:** Administrator.

**Path Parameters:**

| Param | Type   | Description |
|-------|--------|-------------|
| `id`  | string | Role ID     |

**Response 200:** Single role object (same shape as list item).

**Error Codes:**

| Status | Code        | When                |
|--------|-------------|---------------------|
| 403    | `FORBIDDEN` | Non-admin caller    |
| 404    | `NOT_FOUND` | Role does not exist |

---

### 3.11 PUT /api/v1/roles/:id

Update a role's name or description.

**Required Role:** Administrator.

**Request Body:**

| Field         | Type   | Required | Description               |
|---------------|--------|----------|---------------------------|
| `name`        | string | No       | Updated role name         |
| `description` | string | No       | Updated description       |

**Response 200:** Updated role object.

**Error Codes:**

| Status | Code        | When                          |
|--------|-------------|-------------------------------|
| 403    | `FORBIDDEN` | Non-admin caller              |
| 404    | `NOT_FOUND` | Role does not exist           |
| 409    | `CONFLICT`  | Name conflicts with another role |

---

### 3.12 DELETE /api/v1/roles/:id

Soft-delete a role.

**Required Role:** Administrator.

**Response 204:** No content.

**Error Codes:**

| Status | Code                   | When                               |
|--------|------------------------|------------------------------------|
| 403    | `FORBIDDEN`            | Non-admin caller                   |
| 404    | `NOT_FOUND`            | Role does not exist                |
| 422    | `UNPROCESSABLE_ENTITY` | Role is still assigned to users    |

---

### 3.13 PUT /api/v1/roles/:id/permissions

Replace the entire permission set for a role.

**Required Role:** Administrator.

**Path Parameters:**

| Param | Type   | Description |
|-------|--------|-------------|
| `id`  | string | Role ID     |

**Request Body:**

| Field         | Type     | Required | Description                  |
|---------------|----------|----------|------------------------------|
| `permissions` | string[] | Yes      | Complete list of permissions |

**Response 200:**

```json
{
  "id": "role_02",
  "name": "RECRUITER",
  "permissions": ["candidates:read", "candidates:write", "jobs:read", "jobs:write", "pipeline:read", "pipeline:write"],
  "updatedAt": "2026-04-08T14:50:00Z"
}
```

**Error Codes:**

| Status | Code                   | When                                 |
|--------|------------------------|--------------------------------------|
| 400    | `BAD_REQUEST`          | Empty permissions array              |
| 403    | `FORBIDDEN`            | Non-admin caller                     |
| 404    | `NOT_FOUND`            | Role does not exist                  |
| 422    | `UNPROCESSABLE_ENTITY` | Unknown permission strings provided  |

**Notes:**
- This is a full replacement, not a merge. Omitted permissions are removed.
- Changes take effect on next request for users with this role (no session restart required).

---

## 4. Dashboard

### 4.1 GET /api/v1/dashboard

Returns role-specific dashboard data. The response structure adapts to the caller's role.

**Required Role:** Any authenticated user.

**Query Parameters:**

| Param      | Type   | Required | Description                                  |
|------------|--------|----------|----------------------------------------------|
| `dateFrom` | string | No       | Start date for metrics (ISO-8601, default: 30 days ago) |
| `dateTo`   | string | No       | End date for metrics (ISO-8601, default: today)          |

**Response 200 (Recruiter/Staffing Manager example):**

```json
{
  "role": "RECRUITER",
  "summary": {
    "openRequisitions": 14,
    "candidatesInPipeline": 237,
    "interviewsScheduledToday": 6,
    "averageTimeToFill": 18.5,
    "talentPoolSize": 1024
  },
  "alerts": [
    {
      "type": "STALE_CANDIDATE",
      "message": "12 candidates have been in 'Screening' for over 14 days.",
      "link": "/api/v1/pipeline/stages?stageId=screening&staleDays=14"
    }
  ],
  "recentActivity": [
    {
      "action": "CANDIDATE_MOVED",
      "description": "Alex Kim moved to Interview stage",
      "timestamp": "2026-04-08T13:00:00Z"
    }
  ]
}
```

**Response 200 (Dispatch Supervisor example):**

```json
{
  "role": "DISPATCH_SUPERVISOR",
  "summary": {
    "unassignedJobs": 7,
    "activeCollectors": 23,
    "jobsInProgress": 41,
    "completedToday": 18,
    "exceptionAlerts": 3
  },
  "alerts": [
    {
      "type": "UNASSIGNED_JOB",
      "message": "7 jobs have been unassigned for over 30 minutes.",
      "link": "/api/v1/dispatch/jobs/unassigned"
    }
  ]
}
```

**Response 200 (Finance Clerk example):**

```json
{
  "role": "FINANCE_CLERK",
  "summary": {
    "pendingReconciliation": 12,
    "exceptionsOpen": 4,
    "paymentsReceivedToday": 38,
    "totalRevenueToday": 12450.00,
    "settlementsDue": 2
  },
  "alerts": [
    {
      "type": "RECONCILIATION_EXCEPTION",
      "message": "4 bank file exceptions require manual review.",
      "link": "/api/v1/reconciliation/exceptions"
    }
  ]
}
```

**Error Codes:**

| Status | Code           | When              |
|--------|----------------|-------------------|
| 401    | `UNAUTHORIZED` | No active session |

---

## 5. Candidate Profiles

### 5.1 POST /api/v1/candidates

Create a new candidate profile.

**Required Role:** Recruiter/Staffing Manager, Administrator.

**Request Body:**

| Field              | Type     | Required | Description                                  |
|--------------------|----------|----------|----------------------------------------------|
| `firstName`        | string   | Yes      | First name                                   |
| `lastName`         | string   | Yes      | Last name                                    |
| `email`            | string   | Yes      | Contact email                                |
| `phone`            | string   | No       | Contact phone                                |
| `ssn`              | string   | No       | Social Security Number (stored encrypted)    |
| `skills`           | string[] | No       | List of skill tags                           |
| `experienceYears`  | number   | No       | Total years of experience                    |
| `certifications`   | string[] | No       | List of certification names                  |
| `preferredLocations`| string[]| No       | Preferred work locations                     |
| `salaryExpectation`| object   | No       | `{ "min": 50000, "max": 70000, "currency": "USD" }` |
| `resumeText`       | string   | No       | Plain-text resume content for indexing       |
| `tags`             | string[] | No       | Free-form classification tags                |

**Response 201:**

```json
{
  "id": "cand_4501",
  "firstName": "Alex",
  "lastName": "Kim",
  "email": "akim@example.com",
  "phone": "+1-555-0101",
  "ssn": "***-**-7890",
  "skills": ["Java", "Spring Boot", "MySQL"],
  "experienceYears": 6,
  "certifications": ["AWS Solutions Architect"],
  "preferredLocations": ["New York", "Remote"],
  "salaryExpectation": { "min": 50000, "max": 70000, "currency": "USD" },
  "tags": ["senior", "backend"],
  "createdAt": "2026-04-08T14:30:00Z",
  "updatedAt": "2026-04-08T14:30:00Z",
  "deletedAt": null
}
```

**Error Codes:**

| Status | Code                   | When                              |
|--------|------------------------|-----------------------------------|
| 400    | `BAD_REQUEST`          | Missing required fields           |
| 403    | `FORBIDDEN`            | Insufficient role                 |
| 409    | `CONFLICT`             | Duplicate email for active candidate |
| 422    | `UNPROCESSABLE_ENTITY` | Invalid salary range or skill format |

**Notes:**
- The `ssn` field is masked in responses for non-Administrator roles (see Section 1.7).
- Resume text is indexed for full-text search but not returned in list views.

---

### 5.2 GET /api/v1/candidates

List candidates with pagination.

**Required Role:** Recruiter/Staffing Manager, Administrator.

**Query Parameters:**

| Param            | Type    | Required | Description                         |
|------------------|---------|----------|-------------------------------------|
| `page`           | integer | No       | Page index (default 0)              |
| `size`           | integer | No       | Page size (default 25, max 100)     |
| `tags`           | string  | No       | Comma-separated tag filter          |
| `skills`         | string  | No       | Comma-separated skill filter        |
| `status`         | string  | No       | Pipeline status filter              |
| `includeDeleted` | boolean | No       | Include soft-deleted (Admin only)   |

**Response 200:** Paginated list of candidate objects (SSN masked for non-admins).

**Error Codes:**

| Status | Code        | When              |
|--------|-------------|-------------------|
| 403    | `FORBIDDEN` | Insufficient role |

---

### 5.3 GET /api/v1/candidates/:id

Retrieve a single candidate.

**Required Role:** Recruiter/Staffing Manager, Administrator.

**Path Parameters:**

| Param | Type   | Description  |
|-------|--------|--------------|
| `id`  | string | Candidate ID |

**Response 200:** Full candidate object.

**Error Codes:**

| Status | Code        | When                     |
|--------|-------------|--------------------------|
| 403    | `FORBIDDEN` | Insufficient role        |
| 404    | `NOT_FOUND` | Candidate does not exist |

---

### 5.4 PUT /api/v1/candidates/:id

Update a candidate profile.

**Required Role:** Recruiter/Staffing Manager, Administrator.

**Request Body:** Same fields as POST (all optional for update).

**Response 200:** Updated candidate object.

**Error Codes:**

| Status | Code                   | When                           |
|--------|------------------------|--------------------------------|
| 400    | `BAD_REQUEST`          | Malformed body                 |
| 403    | `FORBIDDEN`            | Insufficient role              |
| 404    | `NOT_FOUND`            | Candidate does not exist       |
| 409    | `CONFLICT`             | Email conflicts with another   |

---

### 5.5 DELETE /api/v1/candidates/:id

Soft-delete a candidate.

**Required Role:** Recruiter/Staffing Manager, Administrator.

**Response 204:** No content.

**Error Codes:**

| Status | Code        | When                     |
|--------|-------------|--------------------------|
| 403    | `FORBIDDEN` | Insufficient role        |
| 404    | `NOT_FOUND` | Candidate does not exist |

---

### 5.6 GET /api/v1/candidates/search

Advanced multi-criteria search with Boolean filters and weighted ranking.

**Required Role:** Recruiter/Staffing Manager, Administrator.

**Query Parameters:**

| Param            | Type    | Required | Description                                                 |
|------------------|---------|----------|-------------------------------------------------------------|
| `q`              | string  | No       | Free-text query (searches name, skills, resume)             |
| `skills`         | string  | No       | Boolean expression, e.g. `Java AND (Spring OR Micronaut)`   |
| `experienceMin`  | integer | No       | Minimum years of experience                                 |
| `experienceMax`  | integer | No       | Maximum years of experience                                 |
| `certifications` | string  | No       | Comma-separated required certifications                     |
| `locations`      | string  | No       | Comma-separated preferred locations                         |
| `salaryMin`      | number  | No       | Minimum salary expectation                                  |
| `salaryMax`      | number  | No       | Maximum salary expectation                                  |
| `tags`           | string  | No       | Comma-separated tags (AND logic)                            |
| `sortBy`         | string  | No       | `relevance` (default), `experience`, `name`, `updatedAt`    |
| `page`           | integer | No       | Page index                                                  |
| `size`           | integer | No       | Page size                                                   |

**Response 200:**

```json
{
  "content": [
    {
      "id": "cand_4501",
      "firstName": "Alex",
      "lastName": "Kim",
      "skills": ["Java", "Spring Boot", "MySQL"],
      "experienceYears": 6,
      "matchScore": 0.92,
      "matchHighlights": ["Skill match: Java, Spring Boot", "Experience: 6 yrs (required 5+)"]
    }
  ],
  "page": 0,
  "size": 25,
  "totalPages": 2,
  "totalElements": 34
}
```

**Error Codes:**

| Status | Code                   | When                                    |
|--------|------------------------|-----------------------------------------|
| 400    | `BAD_REQUEST`          | Malformed Boolean expression in skills  |
| 403    | `FORBIDDEN`            | Insufficient role                       |
| 422    | `UNPROCESSABLE_ENTITY` | salaryMin > salaryMax or invalid range  |

**Notes:**
- Weighted ranking considers: skill overlap (40%), experience fit (25%), certification match (20%), location match (15%).
- Results include `matchScore` (0.0-1.0) and `matchHighlights` explaining the ranking.

---

### 5.7 POST /api/v1/candidates/bulk-tag

Apply tags to multiple candidates in a single operation.

**Required Role:** Recruiter/Staffing Manager, Administrator.

**Request Body:**

| Field          | Type     | Required | Description                              |
|----------------|----------|----------|------------------------------------------|
| `candidateIds` | string[] | Yes      | List of candidate IDs (max 500)          |
| `addTags`      | string[] | No       | Tags to add                              |
| `removeTags`   | string[] | No       | Tags to remove                           |

**Response 200:**

```json
{
  "updated": 47,
  "skipped": 3,
  "errors": [
    { "candidateId": "cand_9999", "reason": "Candidate not found" }
  ]
}
```

**Error Codes:**

| Status | Code                   | When                                 |
|--------|------------------------|--------------------------------------|
| 400    | `BAD_REQUEST`          | Empty candidateIds or no tag actions |
| 403    | `FORBIDDEN`            | Insufficient role                    |
| 422    | `UNPROCESSABLE_ENTITY` | More than 500 candidate IDs         |

---

### 5.8 GET /api/v1/candidates/:id/match-rationale

Return an explainable rationale for how this candidate was matched/ranked.

**Required Role:** Recruiter/Staffing Manager, Administrator.

**Path Parameters:**

| Param | Type   | Description  |
|-------|--------|--------------|
| `id`  | string | Candidate ID |

**Query Parameters:**

| Param  | Type   | Required | Description                                   |
|--------|--------|----------|-----------------------------------------------|
| `jobId`| string | No       | Specific job to compare against; if omitted, returns general profile strength |

**Response 200:**

```json
{
  "candidateId": "cand_4501",
  "jobId": "job_prof_012",
  "overallScore": 0.92,
  "breakdown": {
    "skillMatch": { "score": 0.95, "weight": 0.40, "matched": ["Java", "Spring Boot"], "missing": ["Kubernetes"] },
    "experienceMatch": { "score": 0.90, "weight": 0.25, "candidateYears": 6, "requiredYears": 5 },
    "certificationMatch": { "score": 0.80, "weight": 0.20, "matched": ["AWS Solutions Architect"], "missing": [] },
    "locationMatch": { "score": 1.00, "weight": 0.15, "candidateLocations": ["New York"], "jobLocation": "New York" }
  },
  "narrative": "Strong match. Alex meets 95% of required skills and exceeds the experience threshold by 1 year. Only missing Kubernetes certification which is listed as preferred, not required."
}
```

**Error Codes:**

| Status | Code        | When                          |
|--------|-------------|-------------------------------|
| 403    | `FORBIDDEN` | Insufficient role             |
| 404    | `NOT_FOUND` | Candidate or job not found    |

---

## 6. Job Profiles

### 6.1 POST /api/v1/jobs

Create a new job profile / requisition.

**Required Role:** Recruiter/Staffing Manager, Administrator.

**Request Body:**

| Field               | Type     | Required | Description                                 |
|---------------------|----------|----------|---------------------------------------------|
| `title`             | string   | Yes      | Job title                                   |
| `departmentId`      | string   | Yes      | Reference to master department              |
| `description`       | string   | Yes      | Full job description                        |
| `requiredSkills`    | string[] | Yes      | Must-have skills                            |
| `preferredSkills`   | string[] | No       | Nice-to-have skills                         |
| `experienceMin`     | integer  | No       | Minimum years of experience                 |
| `experienceMax`     | integer  | No       | Maximum years of experience                 |
| `certifications`    | string[] | No       | Required certifications                     |
| `location`          | string   | Yes      | Work location                               |
| `salaryRange`       | object   | No       | `{ "min": 60000, "max": 90000, "currency": "USD" }` |
| `openings`          | integer  | Yes      | Number of positions to fill                 |
| `priority`          | string   | No       | `LOW`, `MEDIUM`, `HIGH`, `URGENT` (default `MEDIUM`) |

**Response 201:**

```json
{
  "id": "job_prof_012",
  "title": "Senior Backend Engineer",
  "departmentId": "dept_03",
  "description": "Build and maintain micro-services...",
  "requiredSkills": ["Java", "Spring Boot"],
  "preferredSkills": ["Kubernetes", "Kafka"],
  "experienceMin": 5,
  "experienceMax": 10,
  "certifications": ["AWS Solutions Architect"],
  "location": "New York",
  "salaryRange": { "min": 60000, "max": 90000, "currency": "USD" },
  "openings": 2,
  "filled": 0,
  "priority": "HIGH",
  "status": "OPEN",
  "createdAt": "2026-04-08T15:00:00Z",
  "updatedAt": "2026-04-08T15:00:00Z",
  "deletedAt": null
}
```

**Error Codes:**

| Status | Code                   | When                           |
|--------|------------------------|--------------------------------|
| 400    | `BAD_REQUEST`          | Missing required fields        |
| 403    | `FORBIDDEN`            | Insufficient role              |
| 422    | `UNPROCESSABLE_ENTITY` | Invalid department or salary range |

---

### 6.2 GET /api/v1/jobs

List job profiles with pagination.

**Required Role:** Recruiter/Staffing Manager, Dispatch Supervisor, Administrator.

**Query Parameters:**

| Param          | Type    | Required | Description                           |
|----------------|---------|----------|---------------------------------------|
| `page`         | integer | No       | Page index                            |
| `size`         | integer | No       | Page size                             |
| `status`       | string  | No       | `OPEN`, `CLOSED`, `ON_HOLD`, `DRAFT` |
| `departmentId` | string  | No       | Filter by department                  |
| `priority`     | string  | No       | Filter by priority                    |
| `search`       | string  | No       | Free-text search in title/description |

**Response 200:** Paginated list of job profile objects.

---

### 6.3 GET /api/v1/jobs/:id

Retrieve a single job profile.

**Required Role:** Recruiter/Staffing Manager, Dispatch Supervisor, Administrator.

**Response 200:** Full job profile object.

**Error Codes:**

| Status | Code        | When                |
|--------|-------------|---------------------|
| 404    | `NOT_FOUND` | Job does not exist  |

---

### 6.4 PUT /api/v1/jobs/:id

Update a job profile.

**Required Role:** Recruiter/Staffing Manager, Administrator.

**Request Body:** Same fields as POST (all optional for update).

**Response 200:** Updated job profile object.

**Error Codes:**

| Status | Code                   | When                         |
|--------|------------------------|------------------------------|
| 403    | `FORBIDDEN`            | Insufficient role            |
| 404    | `NOT_FOUND`            | Job does not exist           |
| 422    | `UNPROCESSABLE_ENTITY` | Invalid state change         |

---

### 6.5 DELETE /api/v1/jobs/:id

Soft-delete a job profile.

**Required Role:** Recruiter/Staffing Manager, Administrator.

**Response 204:** No content.

**Error Codes:**

| Status | Code                   | When                                    |
|--------|------------------------|-----------------------------------------|
| 403    | `FORBIDDEN`            | Insufficient role                       |
| 404    | `NOT_FOUND`            | Job does not exist                      |
| 422    | `UNPROCESSABLE_ENTITY` | Active candidates in pipeline for this job |

---

### 6.6 GET /api/v1/jobs/:id/matches

Return candidates matched to this job, ranked by fit.

**Required Role:** Recruiter/Staffing Manager, Administrator.

**Path Parameters:**

| Param | Type   | Description    |
|-------|--------|----------------|
| `id`  | string | Job Profile ID |

**Query Parameters:**

| Param       | Type    | Required | Description                     |
|-------------|---------|----------|---------------------------------|
| `minScore`  | number  | No       | Minimum match score (0.0-1.0)   |
| `page`      | integer | No       | Page index                      |
| `size`      | integer | No       | Page size                       |

**Response 200:**

```json
{
  "jobId": "job_prof_012",
  "content": [
    {
      "candidateId": "cand_4501",
      "firstName": "Alex",
      "lastName": "Kim",
      "matchScore": 0.92,
      "rationale": "Strong skill and experience fit. See /candidates/cand_4501/match-rationale?jobId=job_prof_012 for details."
    }
  ],
  "page": 0,
  "size": 25,
  "totalPages": 3,
  "totalElements": 72
}
```

**Error Codes:**

| Status | Code        | When               |
|--------|-------------|--------------------|
| 403    | `FORBIDDEN` | Insufficient role  |
| 404    | `NOT_FOUND` | Job does not exist |

---

## 7. Talent Pools

### 7.1 POST /api/v1/talent-pools

Create a talent pool.

**Required Role:** Recruiter/Staffing Manager, Administrator.

**Request Body:**

| Field         | Type   | Required | Description                  |
|---------------|--------|----------|------------------------------|
| `name`        | string | Yes      | Pool name                    |
| `description` | string | No       | Purpose of the pool          |
| `criteria`    | object | No       | Auto-inclusion criteria      |

**Response 201:**

```json
{
  "id": "pool_007",
  "name": "Senior Java Developers",
  "description": "Candidates with 5+ years Java experience",
  "criteria": { "skills": ["Java"], "experienceMin": 5 },
  "memberCount": 0,
  "createdAt": "2026-04-08T15:10:00Z",
  "updatedAt": "2026-04-08T15:10:00Z",
  "deletedAt": null
}
```

**Error Codes:**

| Status | Code        | When                    |
|--------|-------------|-------------------------|
| 400    | `BAD_REQUEST` | Missing name           |
| 403    | `FORBIDDEN` | Insufficient role       |
| 409    | `CONFLICT`  | Pool name already exists|

---

### 7.2 GET /api/v1/talent-pools

List talent pools.

**Required Role:** Recruiter/Staffing Manager, Administrator.

**Query Parameters:**

| Param  | Type    | Required | Description     |
|--------|---------|----------|-----------------|
| `page` | integer | No       | Page index      |
| `size` | integer | No       | Page size       |
| `search` | string | No     | Filter by name  |

**Response 200:** Paginated list of talent pool objects.

---

### 7.3 GET /api/v1/talent-pools/:id

Retrieve a single talent pool.

**Required Role:** Recruiter/Staffing Manager, Administrator.

**Response 200:** Full talent pool object including `memberCount`.

**Error Codes:**

| Status | Code        | When                |
|--------|-------------|---------------------|
| 404    | `NOT_FOUND` | Pool does not exist |

---

### 7.4 PUT /api/v1/talent-pools/:id

Update pool details.

**Required Role:** Recruiter/Staffing Manager, Administrator.

**Request Body:** Same fields as POST (all optional).

**Response 200:** Updated talent pool object.

---

### 7.5 DELETE /api/v1/talent-pools/:id

Soft-delete a talent pool.

**Required Role:** Recruiter/Staffing Manager, Administrator.

**Response 204:** No content.

---

### 7.6 POST /api/v1/talent-pools/:id/members

Add candidates to a talent pool.

**Required Role:** Recruiter/Staffing Manager, Administrator.

**Path Parameters:**

| Param | Type   | Description    |
|-------|--------|----------------|
| `id`  | string | Talent Pool ID |

**Request Body:**

| Field          | Type     | Required | Description                    |
|----------------|----------|----------|--------------------------------|
| `candidateIds` | string[] | Yes      | Candidate IDs to add (max 500) |

**Response 200:**

```json
{
  "added": 15,
  "alreadyMember": 3,
  "errors": [
    { "candidateId": "cand_9999", "reason": "Candidate not found" }
  ]
}
```

**Error Codes:**

| Status | Code                   | When                         |
|--------|------------------------|------------------------------|
| 400    | `BAD_REQUEST`          | Empty candidateIds           |
| 403    | `FORBIDDEN`            | Insufficient role            |
| 404    | `NOT_FOUND`            | Pool does not exist          |
| 422    | `UNPROCESSABLE_ENTITY` | More than 500 candidate IDs  |

---

### 7.7 DELETE /api/v1/talent-pools/:id/members/:candidateId

Remove a candidate from a talent pool.

**Required Role:** Recruiter/Staffing Manager, Administrator.

**Path Parameters:**

| Param         | Type   | Description    |
|---------------|--------|----------------|
| `id`          | string | Talent Pool ID |
| `candidateId` | string | Candidate ID   |

**Response 204:** No content.

**Error Codes:**

| Status | Code        | When                                 |
|--------|-------------|--------------------------------------|
| 403    | `FORBIDDEN` | Insufficient role                    |
| 404    | `NOT_FOUND` | Pool or candidate membership not found |

---

### 7.8 GET /api/v1/talent-pools/:id/members

List members of a talent pool.

**Required Role:** Recruiter/Staffing Manager, Administrator.

**Query Parameters:**

| Param  | Type    | Required | Description |
|--------|---------|----------|-------------|
| `page` | integer | No       | Page index  |
| `size` | integer | No       | Page size   |

**Response 200:** Paginated list of candidate summary objects.

**Error Codes:**

| Status | Code        | When                |
|--------|-------------|---------------------|
| 404    | `NOT_FOUND` | Pool does not exist |

---

## 8. Pipeline Management

### 8.1 GET /api/v1/pipeline/stages

List all configured pipeline stages in order.

**Required Role:** Recruiter/Staffing Manager, Administrator.

**Response 200:**

```json
{
  "stages": [
    { "id": "stage_01", "name": "Applied", "order": 1, "color": "#3498db" },
    { "id": "stage_02", "name": "Screening", "order": 2, "color": "#f39c12" },
    { "id": "stage_03", "name": "Interview", "order": 3, "color": "#e74c3c" },
    { "id": "stage_04", "name": "Offer", "order": 4, "color": "#2ecc71" },
    { "id": "stage_05", "name": "Hired", "order": 5, "color": "#27ae60" },
    { "id": "stage_06", "name": "Rejected", "order": 99, "color": "#95a5a6" }
  ]
}
```

**Error Codes:**

| Status | Code        | When              |
|--------|-------------|-------------------|
| 403    | `FORBIDDEN` | Insufficient role |

**Notes:**
- See [Appendix B](#appendix-b--pipeline-stage-transitions) for valid stage transitions.

---

### 8.2 POST /api/v1/pipeline/moves

Batch move one or more candidates to a new pipeline stage.

**Required Role:** Recruiter/Staffing Manager, Administrator.

**Request Body:**

| Field          | Type     | Required | Description                                  |
|----------------|----------|----------|----------------------------------------------|
| `moves`        | array    | Yes      | Array of move objects                        |
| `moves[].candidateId` | string | Yes | Candidate to move                          |
| `moves[].jobId`       | string | Yes | Job profile context                        |
| `moves[].toStageId`   | string | Yes | Target stage                               |
| `moves[].reason`      | string | No  | Reason for move (required for rejections)  |

**Response 202:**

```json
{
  "moveId": "move_abc123",
  "status": "completed",
  "results": [
    { "candidateId": "cand_4501", "fromStage": "Screening", "toStage": "Interview", "success": true },
    { "candidateId": "cand_4502", "fromStage": "Applied", "toStage": "Interview", "success": false, "error": "Invalid transition: Applied -> Interview" }
  ],
  "undoAvailableUntil": "2026-04-08T15:11:00Z"
}
```

**Error Codes:**

| Status | Code                   | When                                     |
|--------|------------------------|------------------------------------------|
| 400    | `BAD_REQUEST`          | Empty moves array                        |
| 403    | `FORBIDDEN`            | Insufficient role                        |
| 422    | `UNPROCESSABLE_ENTITY` | Invalid stage transition (see Appendix B)|

**Notes:**
- The `moveId` can be used to undo the batch within 60 seconds.
- Invalid moves within a batch do not block valid ones; each is processed independently.
- Moves to `Rejected` stage require a `reason` field.

---

### 8.3 POST /api/v1/pipeline/moves/:moveId/undo

Undo a batch move. Only valid within 60 seconds of the original move.

**Required Role:** Recruiter/Staffing Manager, Administrator.

**Path Parameters:**

| Param    | Type   | Description |
|----------|--------|-------------|
| `moveId` | string | Move ID     |

**Response 200:**

```json
{
  "moveId": "move_abc123",
  "undone": true,
  "restoredCandidates": 1
}
```

**Error Codes:**

| Status | Code                   | When                                          |
|--------|------------------------|-----------------------------------------------|
| 403    | `FORBIDDEN`            | Insufficient role                             |
| 404    | `NOT_FOUND`            | Move ID not found                             |
| 422    | `UNPROCESSABLE_ENTITY` | Undo window expired (>60 seconds) or candidate state changed since move |

**Notes:**
- The 60-second undo window is strict. After expiration the endpoint returns 422.
- If any candidate in the batch has been moved again since the original move, that candidate is skipped.

---

## 9. Saved Searches & Snapshots

### 9.1 POST /api/v1/saved-searches

Save a search configuration for reuse.

**Required Role:** Recruiter/Staffing Manager, Administrator.

**Request Body:**

| Field      | Type   | Required | Description                              |
|------------|--------|----------|------------------------------------------|
| `name`     | string | Yes      | Search name                              |
| `criteria` | object | Yes      | Search parameters (same as candidate search query params) |
| `isShared` | boolean| No       | Share with team (default false)          |

**Response 201:**

```json
{
  "id": "ss_041",
  "name": "Senior Java in NYC",
  "criteria": {
    "skills": "Java AND Spring",
    "experienceMin": 5,
    "locations": "New York"
  },
  "isShared": false,
  "createdBy": "usr_001",
  "createdAt": "2026-04-08T15:15:00Z",
  "updatedAt": "2026-04-08T15:15:00Z"
}
```

**Error Codes:**

| Status | Code        | When                              |
|--------|-------------|-----------------------------------|
| 400    | `BAD_REQUEST` | Missing name or criteria         |
| 403    | `FORBIDDEN` | Insufficient role                 |
| 409    | `CONFLICT`  | Duplicate name for the same user  |

---

### 9.2 GET /api/v1/saved-searches

List saved searches for the current user (plus shared searches).

**Required Role:** Recruiter/Staffing Manager, Administrator.

**Query Parameters:**

| Param  | Type    | Required | Description |
|--------|---------|----------|-------------|
| `page` | integer | No       | Page index  |
| `size` | integer | No       | Page size   |

**Response 200:** Paginated list of saved search objects.

---

### 9.3 GET /api/v1/saved-searches/:id

Retrieve a saved search.

**Required Role:** Owner of the search, or any user if `isShared` is true. Administrator.

**Response 200:** Full saved search object.

**Error Codes:**

| Status | Code        | When                                     |
|--------|-------------|------------------------------------------|
| 403    | `FORBIDDEN` | Not owner and search is not shared       |
| 404    | `NOT_FOUND` | Search does not exist                    |

---

### 9.4 PUT /api/v1/saved-searches/:id

Update a saved search.

**Required Role:** Owner of the search, Administrator.

**Request Body:** Same fields as POST (all optional).

**Response 200:** Updated saved search object.

---

### 9.5 DELETE /api/v1/saved-searches/:id

Delete a saved search (hard delete).

**Required Role:** Owner of the search, Administrator.

**Response 204:** No content.

---

### 9.6 GET /api/v1/search-snapshots

List search result snapshots. Snapshots are point-in-time captures of search results.

**Required Role:** Recruiter/Staffing Manager, Administrator.

**Query Parameters:**

| Param           | Type    | Required | Description                    |
|-----------------|---------|----------|--------------------------------|
| `savedSearchId` | string  | No       | Filter by parent saved search  |
| `page`          | integer | No       | Page index                     |
| `size`          | integer | No       | Page size                      |

**Response 200:**

```json
{
  "content": [
    {
      "id": "snap_201",
      "savedSearchId": "ss_041",
      "name": "Senior Java in NYC",
      "candidateCount": 34,
      "capturedAt": "2026-04-08T15:20:00Z"
    }
  ],
  "page": 0,
  "size": 25,
  "totalPages": 1,
  "totalElements": 5
}
```

---

### 9.7 GET /api/v1/search-snapshots/:id

Retrieve a snapshot with full stored results and rationale.

**Required Role:** Recruiter/Staffing Manager, Administrator.

**Response 200:**

```json
{
  "id": "snap_201",
  "savedSearchId": "ss_041",
  "criteria": {
    "skills": "Java AND Spring",
    "experienceMin": 5,
    "locations": "New York"
  },
  "capturedAt": "2026-04-08T15:20:00Z",
  "results": [
    {
      "candidateId": "cand_4501",
      "firstName": "Alex",
      "lastName": "Kim",
      "matchScore": 0.92,
      "rationale": {
        "skillMatch": 0.95,
        "experienceMatch": 0.90,
        "narrative": "Strong match across all criteria at time of capture."
      }
    }
  ],
  "totalResults": 34
}
```

**Error Codes:**

| Status | Code        | When                     |
|--------|-------------|--------------------------|
| 404    | `NOT_FOUND` | Snapshot does not exist  |

**Notes:**
- Snapshot data is immutable. It reflects the state of candidates at the time of capture.
- Stored rationale allows reviewers to understand why each candidate was ranked at that point in time.

---

## 10. Collectors & Sites (Dispatch)

### 10.1 POST /api/v1/collectors

Register a new collector.

**Required Role:** Dispatch Supervisor, Administrator.

**Request Body:**

| Field         | Type     | Required | Description                    |
|---------------|----------|----------|--------------------------------|
| `firstName`   | string   | Yes      | First name                     |
| `lastName`    | string   | Yes      | Last name                      |
| `phone`       | string   | Yes      | Contact phone                  |
| `email`       | string   | No       | Contact email                  |
| `siteIds`     | string[] | No       | Assigned site IDs              |
| `skills`      | string[] | No       | Collector skill tags           |
| `status`      | string   | No       | `ACTIVE`, `INACTIVE` (default `ACTIVE`) |

**Response 201:**

```json
{
  "id": "col_301",
  "firstName": "Sam",
  "lastName": "Rivera",
  "phone": "+1-555-0202",
  "email": "srivera@company.local",
  "siteIds": ["site_01", "site_02"],
  "skills": ["hazmat", "heavy-equipment"],
  "status": "ACTIVE",
  "createdAt": "2026-04-08T15:25:00Z",
  "updatedAt": "2026-04-08T15:25:00Z",
  "deletedAt": null
}
```

**Error Codes:**

| Status | Code        | When                 |
|--------|-------------|----------------------|
| 400    | `BAD_REQUEST` | Missing required fields |
| 403    | `FORBIDDEN` | Insufficient role    |
| 409    | `CONFLICT`  | Duplicate phone      |

---

### 10.2 GET /api/v1/collectors

List collectors.

**Required Role:** Dispatch Supervisor, Administrator.

**Query Parameters:**

| Param    | Type    | Required | Description                 |
|----------|---------|----------|-----------------------------|
| `page`   | integer | No       | Page index                  |
| `size`   | integer | No       | Page size                   |
| `siteId` | string  | No       | Filter by assigned site     |
| `status` | string  | No       | `ACTIVE` or `INACTIVE`      |
| `search` | string  | No       | Search by name              |

**Response 200:** Paginated list of collector objects.

---

### 10.3 GET /api/v1/collectors/:id

Retrieve a single collector.

**Required Role:** Dispatch Supervisor, Administrator.

**Response 200:** Full collector object.

**Error Codes:**

| Status | Code        | When                     |
|--------|-------------|--------------------------|
| 404    | `NOT_FOUND` | Collector does not exist |

---

### 10.4 PUT /api/v1/collectors/:id

Update collector details.

**Required Role:** Dispatch Supervisor, Administrator.

**Response 200:** Updated collector object.

---

### 10.5 DELETE /api/v1/collectors/:id

Soft-delete a collector.

**Required Role:** Dispatch Supervisor, Administrator.

**Response 204:** No content.

**Error Codes:**

| Status | Code                   | When                                     |
|--------|------------------------|------------------------------------------|
| 422    | `UNPROCESSABLE_ENTITY` | Collector has active/in-progress jobs    |

---

### 10.6 POST /api/v1/sites

Create a new site.

**Required Role:** Dispatch Supervisor, Administrator.

**Request Body:**

| Field       | Type   | Required | Description                      |
|-------------|--------|----------|----------------------------------|
| `name`      | string | Yes      | Site name                        |
| `address`   | string | Yes      | Physical address                 |
| `latitude`  | number | No       | GPS latitude                     |
| `longitude` | number | No       | GPS longitude                    |
| `capacity`  | object | No       | `{ "maxCollectors": 20, "maxShiftsPerDay": 3 }` |
| `timezone`  | string | Yes      | IANA timezone (e.g., `America/New_York`) |

**Response 201:**

```json
{
  "id": "site_01",
  "name": "Downtown Hub",
  "address": "123 Main St, New York, NY 10001",
  "latitude": 40.7128,
  "longitude": -74.0060,
  "capacity": { "maxCollectors": 20, "maxShiftsPerDay": 3 },
  "timezone": "America/New_York",
  "status": "ACTIVE",
  "createdAt": "2026-04-08T15:30:00Z",
  "updatedAt": "2026-04-08T15:30:00Z",
  "deletedAt": null
}
```

**Error Codes:**

| Status | Code        | When               |
|--------|-------------|---------------------|
| 400    | `BAD_REQUEST` | Missing required fields |
| 403    | `FORBIDDEN` | Insufficient role   |
| 409    | `CONFLICT`  | Site name duplicate  |

---

### 10.7 GET /api/v1/sites

List sites.

**Required Role:** Dispatch Supervisor, Administrator.

**Query Parameters:**

| Param    | Type    | Required | Description    |
|----------|---------|----------|----------------|
| `page`   | integer | No       | Page index     |
| `size`   | integer | No       | Page size      |
| `status` | string  | No       | `ACTIVE`, `INACTIVE` |
| `search` | string  | No       | Search by name |

**Response 200:** Paginated list of site objects.

---

### 10.8 GET /api/v1/sites/:id

Retrieve a single site.

**Required Role:** Dispatch Supervisor, Administrator.

**Response 200:** Full site object.

---

### 10.9 PUT /api/v1/sites/:id

Update site details.

**Required Role:** Dispatch Supervisor, Administrator.

**Response 200:** Updated site object.

---

### 10.10 DELETE /api/v1/sites/:id

Soft-delete a site.

**Required Role:** Dispatch Supervisor, Administrator.

**Response 204:** No content.

**Error Codes:**

| Status | Code                   | When                              |
|--------|------------------------|-----------------------------------|
| 422    | `UNPROCESSABLE_ENTITY` | Site has active shifts or jobs    |

---

### 10.11 GET /api/v1/sites/:id/capacity

Get current capacity status for a site.

**Required Role:** Dispatch Supervisor, Administrator.

**Response 200:**

```json
{
  "siteId": "site_01",
  "siteName": "Downtown Hub",
  "maxCollectors": 20,
  "maxShiftsPerDay": 3,
  "currentCollectors": 14,
  "shiftsToday": 2,
  "utilizationPercent": 70.0,
  "date": "2026-04-08"
}
```

---

### 10.12 PUT /api/v1/sites/:id/capacity

Set or update capacity limits for a site.

**Required Role:** Dispatch Supervisor, Administrator.

**Request Body:**

| Field              | Type    | Required | Description                  |
|--------------------|---------|----------|------------------------------|
| `maxCollectors`    | integer | No       | Maximum collectors per shift |
| `maxShiftsPerDay`  | integer | No       | Maximum shifts per day       |

**Response 200:** Updated capacity object (same shape as GET).

**Error Codes:**

| Status | Code                   | When                                          |
|--------|------------------------|-----------------------------------------------|
| 422    | `UNPROCESSABLE_ENTITY` | New limit lower than current active count     |

---

## 11. Shifts & Scheduling

### 11.1 POST /api/v1/shifts

Create a single shift.

**Required Role:** Dispatch Supervisor, Administrator.

**Request Body:**

| Field         | Type     | Required | Description                                     |
|---------------|----------|----------|-------------------------------------------------|
| `siteId`      | string   | Yes      | Site ID                                         |
| `date`        | string   | Yes      | Shift date (ISO-8601 date)                      |
| `startTime`   | string   | Yes      | Start time in HH:mm (15-minute increments only) |
| `endTime`     | string   | Yes      | End time in HH:mm (15-minute increments only)   |
| `collectorIds`| string[] | No       | Pre-assigned collector IDs                      |
| `requiredSkills` | string[] | No    | Skills needed for this shift                    |
| `notes`       | string   | No       | Shift notes                                     |

**Response 201:**

```json
{
  "id": "shift_501",
  "siteId": "site_01",
  "siteName": "Downtown Hub",
  "date": "2026-04-09",
  "startTime": "08:00",
  "endTime": "16:00",
  "collectorIds": ["col_301"],
  "requiredSkills": ["hazmat"],
  "notes": "Morning collection route A",
  "status": "SCHEDULED",
  "createdAt": "2026-04-08T15:35:00Z",
  "updatedAt": "2026-04-08T15:35:00Z"
}
```

**Error Codes:**

| Status | Code                   | When                                               |
|--------|------------------------|-----------------------------------------------------|
| 400    | `BAD_REQUEST`          | Missing required fields                             |
| 403    | `FORBIDDEN`            | Insufficient role                                   |
| 409    | `CONFLICT`             | Collector already assigned to overlapping shift     |
| 422    | `UNPROCESSABLE_ENTITY` | Time not in 15-minute increments or exceeds site capacity |

**Notes:**
- Start and end times must align to 15-minute boundaries (e.g., 08:00, 08:15, 08:30, 08:45).
- Creating a shift that would exceed the site's `maxShiftsPerDay` returns 422.

---

### 11.2 GET /api/v1/shifts

List shifts with filters.

**Required Role:** Dispatch Supervisor, Administrator.

**Query Parameters:**

| Param         | Type    | Required | Description                            |
|---------------|---------|----------|----------------------------------------|
| `siteId`      | string  | No       | Filter by site                         |
| `date`        | string  | No       | Filter by date (ISO-8601)              |
| `dateFrom`    | string  | No       | Start of date range                    |
| `dateTo`      | string  | No       | End of date range                      |
| `collectorId` | string  | No       | Filter by assigned collector           |
| `status`      | string  | No       | `SCHEDULED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |
| `page`        | integer | No       | Page index                             |
| `size`        | integer | No       | Page size                              |

**Response 200:** Paginated list of shift objects.

---

### 11.3 GET /api/v1/shifts/:id

Retrieve a single shift.

**Required Role:** Dispatch Supervisor, Administrator.

**Response 200:** Full shift object.

---

### 11.4 PUT /api/v1/shifts/:id

Update a shift.

**Required Role:** Dispatch Supervisor, Administrator.

**Response 200:** Updated shift object.

**Error Codes:**

| Status | Code                   | When                                     |
|--------|------------------------|------------------------------------------|
| 409    | `CONFLICT`             | Collector overlap                        |
| 422    | `UNPROCESSABLE_ENTITY` | Cannot modify a completed or cancelled shift |

---

### 11.5 DELETE /api/v1/shifts/:id

Cancel a shift (soft-delete, sets status to `CANCELLED`).

**Required Role:** Dispatch Supervisor, Administrator.

**Response 204:** No content.

**Error Codes:**

| Status | Code                   | When                            |
|--------|------------------------|---------------------------------|
| 422    | `UNPROCESSABLE_ENTITY` | Shift is in progress or completed |

---

### 11.6 POST /api/v1/shifts/bulk

Bulk create shifts (e.g., generate a week of shifts for a site).

**Required Role:** Dispatch Supervisor, Administrator.

**Request Body:**

| Field    | Type  | Required | Description                              |
|----------|-------|----------|------------------------------------------|
| `shifts` | array | Yes      | Array of shift objects (same as single POST, max 200) |

**Response 200:**

```json
{
  "created": 14,
  "errors": [
    { "index": 3, "error": "Collector col_305 has overlapping shift on 2026-04-10" }
  ]
}
```

**Error Codes:**

| Status | Code                   | When                    |
|--------|------------------------|-------------------------|
| 400    | `BAD_REQUEST`          | Empty array             |
| 422    | `UNPROCESSABLE_ENTITY` | More than 200 shifts    |

**Notes:**
- Partial success is allowed. Valid shifts are created; errors are reported per item.

---

## 12. Dispatch Jobs

### 12.1 POST /api/v1/dispatch/jobs

Create a dispatch job.

**Required Role:** Dispatch Supervisor, Administrator.

**Request Body:**

| Field            | Type   | Required | Description                                    |
|------------------|--------|----------|------------------------------------------------|
| `siteId`         | string | Yes      | Target site                                    |
| `shiftId`        | string | No       | Associated shift                               |
| `description`    | string | Yes      | Job description                                |
| `priority`       | string | No       | `LOW`, `MEDIUM`, `HIGH`, `URGENT` (default `MEDIUM`) |
| `requiredSkills` | string[]| No      | Skills needed                                  |
| `assignedCollectorId` | string | No  | Direct assignment (assigned-order mode)        |

**Response 201:**

```json
{
  "id": "dj_801",
  "siteId": "site_01",
  "shiftId": "shift_501",
  "description": "Collect recyclables from Zone B",
  "priority": "HIGH",
  "requiredSkills": ["hazmat"],
  "assignedCollectorId": null,
  "status": "CREATED",
  "createdAt": "2026-04-08T15:40:00Z",
  "updatedAt": "2026-04-08T15:40:00Z"
}
```

**Error Codes:**

| Status | Code                   | When                              |
|--------|------------------------|-----------------------------------|
| 400    | `BAD_REQUEST`          | Missing required fields           |
| 403    | `FORBIDDEN`            | Insufficient role                 |
| 422    | `UNPROCESSABLE_ENTITY` | Invalid site or shift reference   |

**Notes:**
- In grab-order mode, the job status starts as `OFFERED` and is visible to all eligible collectors at the site.
- In assigned-order mode, the job is directly assigned and status starts as `CREATED`.
- See [Appendix C](#appendix-c--dispatch-job-state-machine) for the full state machine.

---

### 12.2 GET /api/v1/dispatch/jobs

List dispatch jobs with filters.

**Required Role:** Dispatch Supervisor, Administrator.

**Query Parameters:**

| Param         | Type    | Required | Description                                      |
|---------------|---------|----------|--------------------------------------------------|
| `siteId`      | string  | No       | Filter by site                                   |
| `collectorId` | string  | No       | Filter by assigned collector                     |
| `status`      | string  | No       | `CREATED`, `OFFERED`, `ACCEPTED`, `IN_PROGRESS`, `COMPLETED`, `TIMEOUT`, `REDISPATCHED`, `CANCELLED` |
| `priority`    | string  | No       | Filter by priority                               |
| `dateFrom`    | string  | No       | Start of date range                              |
| `dateTo`      | string  | No       | End of date range                                |
| `page`        | integer | No       | Page index                                       |
| `size`        | integer | No       | Page size                                        |

**Response 200:** Paginated list of dispatch job objects.

---

### 12.3 GET /api/v1/dispatch/jobs/:id

Retrieve a single dispatch job.

**Required Role:** Dispatch Supervisor, Administrator.

**Response 200:** Full dispatch job object including status history.

```json
{
  "id": "dj_801",
  "siteId": "site_01",
  "shiftId": "shift_501",
  "description": "Collect recyclables from Zone B",
  "priority": "HIGH",
  "requiredSkills": ["hazmat"],
  "assignedCollectorId": "col_301",
  "status": "IN_PROGRESS",
  "statusHistory": [
    { "status": "CREATED", "timestamp": "2026-04-08T15:40:00Z" },
    { "status": "OFFERED", "timestamp": "2026-04-08T15:40:01Z" },
    { "status": "ACCEPTED", "timestamp": "2026-04-08T15:42:00Z", "collectorId": "col_301" },
    { "status": "IN_PROGRESS", "timestamp": "2026-04-08T15:45:00Z" }
  ],
  "createdAt": "2026-04-08T15:40:00Z",
  "updatedAt": "2026-04-08T15:45:00Z"
}
```

**Error Codes:**

| Status | Code        | When               |
|--------|-------------|---------------------|
| 404    | `NOT_FOUND` | Job does not exist  |

---

### 12.4 POST /api/v1/dispatch/jobs/:id/accept

Collector accepts a dispatch job (grab-order mode).

**Required Role:** Dispatch Supervisor, Administrator. (In production, collectors use this via the frontend.)

**Path Parameters:**

| Param | Type   | Description     |
|-------|--------|-----------------|
| `id`  | string | Dispatch Job ID |

**Request Body:**

| Field         | Type   | Required | Description        |
|---------------|--------|----------|--------------------|
| `collectorId` | string | Yes      | Accepting collector|

**Response 200:**

```json
{
  "id": "dj_801",
  "status": "ACCEPTED",
  "assignedCollectorId": "col_301",
  "acceptedAt": "2026-04-08T15:42:00Z"
}
```

**Error Codes:**

| Status | Code                   | When                                      |
|--------|------------------------|-------------------------------------------|
| 403    | `FORBIDDEN`            | Insufficient role                         |
| 404    | `NOT_FOUND`            | Job does not exist                        |
| 409    | `CONFLICT`             | Job already accepted by another collector |
| 422    | `UNPROCESSABLE_ENTITY` | Job not in OFFERED state                  |

**Notes:**
- This is a race-sensitive operation. The first collector to accept wins; others receive 409.

---

### 12.5 POST /api/v1/dispatch/jobs/:id/reassign

Manually reassign a dispatch job to a different collector.

**Required Role:** Dispatch Supervisor, Administrator.

**Request Body:**

| Field            | Type   | Required | Description                   |
|------------------|--------|----------|-------------------------------|
| `collectorId`    | string | Yes      | New collector ID              |
| `reason`         | string | No       | Reason for reassignment       |

**Response 200:**

```json
{
  "id": "dj_801",
  "status": "ACCEPTED",
  "assignedCollectorId": "col_305",
  "reassignedAt": "2026-04-08T16:00:00Z",
  "reassignReason": "Original collector called in sick"
}
```

**Error Codes:**

| Status | Code                   | When                                     |
|--------|------------------------|------------------------------------------|
| 403    | `FORBIDDEN`            | Insufficient role                        |
| 404    | `NOT_FOUND`            | Job or collector does not exist          |
| 422    | `UNPROCESSABLE_ENTITY` | Job is completed or cancelled            |

---

### 12.6 GET /api/v1/dispatch/jobs/unassigned

List jobs that are pending assignment.

**Required Role:** Dispatch Supervisor, Administrator.

**Query Parameters:**

| Param    | Type    | Required | Description            |
|----------|---------|----------|------------------------|
| `siteId` | string  | No       | Filter by site         |
| `page`   | integer | No       | Page index             |
| `size`   | integer | No       | Page size              |

**Response 200:** Paginated list of dispatch jobs with status `CREATED` or `OFFERED` (not yet accepted).

---

### 12.7 PATCH /api/v1/dispatch/config

Set the dispatch mode for a site.

**Required Role:** Dispatch Supervisor, Administrator.

**Request Body:**

| Field          | Type   | Required | Description                                         |
|----------------|--------|----------|-----------------------------------------------------|
| `siteId`       | string | Yes      | Site to configure                                   |
| `dispatchMode` | string | Yes      | `GRAB_ORDER` or `ASSIGNED_ORDER`                    |
| `offerTimeout` | integer| No       | Seconds before an offered job times out (default 300)|

**Response 200:**

```json
{
  "siteId": "site_01",
  "dispatchMode": "GRAB_ORDER",
  "offerTimeout": 300,
  "updatedAt": "2026-04-08T16:05:00Z"
}
```

**Error Codes:**

| Status | Code                   | When                           |
|--------|------------------------|--------------------------------|
| 400    | `BAD_REQUEST`          | Invalid dispatch mode value    |
| 403    | `FORBIDDEN`            | Insufficient role              |
| 404    | `NOT_FOUND`            | Site does not exist            |

**Notes:**
- In `GRAB_ORDER` mode, new jobs are offered to all eligible collectors and the first to accept gets it.
- In `ASSIGNED_ORDER` mode, the supervisor assigns each job to a specific collector.
- `offerTimeout` only applies to `GRAB_ORDER` mode. After timeout, the job transitions to `TIMEOUT` then `REDISPATCHED`.

---

## 13. Unified Search

### 13.1 GET /api/v1/search

Search across multiple entity types: members, enterprises, resources, orders, redemption records.

**Required Role:** Any authenticated user (results filtered by role permissions).

**Query Parameters:**

| Param     | Type   | Required | Description                                                  |
|-----------|--------|----------|--------------------------------------------------------------|
| `q`       | string | Yes      | Search query                                                 |
| `type`    | string | No       | Entity type filter: `member`, `enterprise`, `resource`, `order`, `redemption` (comma-separated for multiple) |
| `filters` | string | No       | JSON-encoded additional filters, e.g. `{"status":"active"}` |
| `page`    | integer| No       | Page index                                                   |
| `size`    | integer| No       | Page size                                                    |

**Response 200:**

```json
{
  "content": [
    {
      "type": "member",
      "id": "mem_1001",
      "title": "Jane Doe",
      "subtitle": "Enterprise: Acme Corp",
      "highlights": ["Name match: <em>Jane Doe</em>"],
      "url": "/api/v1/candidates/mem_1001"
    },
    {
      "type": "order",
      "id": "ord_5001",
      "title": "Order #5001",
      "subtitle": "Status: Pending",
      "highlights": ["Reference: <em>Jane Doe</em> project"],
      "url": "/api/v1/dispatch/jobs/ord_5001"
    }
  ],
  "page": 0,
  "size": 25,
  "totalPages": 1,
  "totalElements": 2,
  "facets": {
    "type": { "member": 1, "order": 1 }
  }
}
```

**Error Codes:**

| Status | Code           | When                    |
|--------|----------------|-------------------------|
| 400    | `BAD_REQUEST`  | Missing q parameter     |
| 401    | `UNAUTHORIZED` | No active session       |

**Notes:**
- Results are filtered by the caller's permissions. A Finance Clerk will not see candidate profiles.
- Highlights use `<em>` tags to indicate matched terms.

---

### 13.2 POST /api/v1/search/save

Save a unified search configuration.

**Required Role:** Any authenticated user.

**Request Body:**

| Field     | Type   | Required | Description           |
|-----------|--------|----------|-----------------------|
| `name`    | string | Yes      | Name for saved search |
| `query`   | string | Yes      | Search query          |
| `type`    | string | No       | Entity type filter    |
| `filters` | object | No       | Additional filters    |

**Response 201:**

```json
{
  "id": "usearch_012",
  "name": "Jane Doe orders",
  "query": "Jane Doe",
  "type": "order",
  "filters": {},
  "createdBy": "usr_001",
  "createdAt": "2026-04-08T16:10:00Z"
}
```

---

### 13.3 GET /api/v1/search/saved

List saved unified searches for the current user.

**Required Role:** Any authenticated user.

**Response 200:** Paginated list of saved unified search objects.

---

## 14. Exports

### 14.1 POST /api/v1/exports

Trigger an export job. Returns immediately with a job ID.

**Required Role:** Recruiter/Staffing Manager, Dispatch Supervisor, Finance Clerk, Administrator.

**Request Body:**

| Field      | Type     | Required | Description                                          |
|------------|----------|----------|------------------------------------------------------|
| `type`     | string   | Yes      | Export type: `candidates`, `jobs`, `dispatch`, `payments`, `reconciliation`, `audit-log` |
| `format`   | string   | No       | `CSV` or `XLSX` (default `CSV`)                      |
| `filters`  | object   | No       | Same filters as the corresponding list endpoint      |
| `columns`  | string[] | No       | Specific columns to include (default: all permitted) |

**Response 202:**

```json
{
  "jobId": "export_job_091",
  "status": "queued",
  "type": "candidates",
  "format": "CSV",
  "pollUrl": "/api/v1/jobs/export_job_091",
  "createdAt": "2026-04-08T16:15:00Z"
}
```

**Error Codes:**

| Status | Code                   | When                                    |
|--------|------------------------|-----------------------------------------|
| 400    | `BAD_REQUEST`          | Invalid type or format                  |
| 403    | `FORBIDDEN`            | Role not permitted to export this type  |
| 429    | `RATE_LIMIT_EXCEEDED`  | Too many concurrent export jobs         |

**Notes:**
- Maximum 3 concurrent export jobs per user.
- Export files are available for download for 24 hours after completion.
- The 90-second server-side timeout applies to each processing step.
- Sensitive fields are masked in exports for non-Administrator roles.

---

### 14.2 GET /api/v1/exports

List export jobs for the current user.

**Required Role:** Any authenticated user.

**Query Parameters:**

| Param    | Type    | Required | Description                                        |
|----------|---------|----------|----------------------------------------------------|
| `status` | string  | No       | `queued`, `running`, `completed`, `failed`         |
| `page`   | integer | No       | Page index                                         |
| `size`   | integer | No       | Page size                                          |

**Response 200:**

```json
{
  "content": [
    {
      "jobId": "export_job_091",
      "type": "candidates",
      "format": "CSV",
      "status": "completed",
      "downloadUrl": "/api/v1/exports/export_job_091/download",
      "fileSize": 245760,
      "rowCount": 1024,
      "createdAt": "2026-04-08T16:15:00Z",
      "completedAt": "2026-04-08T16:15:45Z",
      "expiresAt": "2026-04-09T16:15:45Z"
    }
  ],
  "page": 0,
  "size": 25,
  "totalPages": 1,
  "totalElements": 3
}
```

---

### 14.3 GET /api/v1/exports/:id/download

Download a completed export file.

**Required Role:** Owner of the export job, Administrator.

**Path Parameters:**

| Param | Type   | Description   |
|-------|--------|---------------|
| `id`  | string | Export Job ID |

**Response 200:** Binary file download (`Content-Type: application/octet-stream`, `Content-Disposition: attachment`).

**Error Codes:**

| Status | Code                   | When                          |
|--------|------------------------|-------------------------------|
| 403    | `FORBIDDEN`            | Not the owner                 |
| 404    | `NOT_FOUND`            | Export job does not exist      |
| 422    | `UNPROCESSABLE_ENTITY` | Export not yet completed or file expired |

---

## 15. Master Data (Admin)

All master data endpoints follow the same CRUD pattern. Only Administrators may create, update, or delete master data. Other roles have read access.

### 15.1 Departments -- /api/v1/master/departments

#### POST /api/v1/master/departments

**Required Role:** Administrator.

**Request Body:**

| Field         | Type   | Required | Description        |
|---------------|--------|----------|--------------------|
| `name`        | string | Yes      | Department name    |
| `code`        | string | Yes      | Short code (unique)|
| `description` | string | No       | Description        |

**Response 201:**

```json
{
  "id": "dept_03",
  "name": "Engineering",
  "code": "ENG",
  "description": "Software engineering department",
  "createdAt": "2026-04-08T16:20:00Z",
  "updatedAt": "2026-04-08T16:20:00Z",
  "deletedAt": null
}
```

**Error Codes:**

| Status | Code        | When                  |
|--------|-------------|------------------------|
| 403    | `FORBIDDEN` | Non-admin caller       |
| 409    | `CONFLICT`  | Duplicate code or name |

#### GET /api/v1/master/departments

**Required Role:** Any authenticated user.

**Response 200:** Paginated list of department objects.

#### GET /api/v1/master/departments/:id

**Required Role:** Any authenticated user.

**Response 200:** Single department object.

#### PUT /api/v1/master/departments/:id

**Required Role:** Administrator.

**Response 200:** Updated department object.

#### DELETE /api/v1/master/departments/:id

**Required Role:** Administrator.

**Response 204:** No content.

**Error Codes:**

| Status | Code                   | When                                          |
|--------|------------------------|-----------------------------------------------|
| 422    | `UNPROCESSABLE_ENTITY` | Department referenced by active job profiles  |

---

### 15.2 Courses -- /api/v1/master/courses

Same CRUD pattern as departments.

**Request Body (POST/PUT):**

| Field          | Type   | Required | Description                  |
|----------------|--------|----------|------------------------------|
| `name`         | string | Yes      | Course name                  |
| `code`         | string | Yes      | Short code (unique)          |
| `departmentId` | string | Yes      | Parent department            |
| `credits`      | integer| No       | Number of credits            |

**Response 201 example:**

```json
{
  "id": "course_015",
  "name": "Advanced Java",
  "code": "CS401",
  "departmentId": "dept_03",
  "credits": 4,
  "createdAt": "2026-04-08T16:25:00Z",
  "updatedAt": "2026-04-08T16:25:00Z",
  "deletedAt": null
}
```

---

### 15.3 Semesters -- /api/v1/master/semesters

Same CRUD pattern as departments.

**Request Body (POST/PUT):**

| Field       | Type   | Required | Description                  |
|-------------|--------|----------|------------------------------|
| `name`      | string | Yes      | Semester name (e.g., "Fall 2026") |
| `startDate` | string | Yes      | ISO-8601 date                |
| `endDate`   | string | Yes      | ISO-8601 date                |
| `status`    | string | No       | `UPCOMING`, `ACTIVE`, `COMPLETED` |

**Response 201 example:**

```json
{
  "id": "sem_06",
  "name": "Fall 2026",
  "startDate": "2026-09-01",
  "endDate": "2026-12-15",
  "status": "UPCOMING",
  "createdAt": "2026-04-08T16:30:00Z",
  "updatedAt": "2026-04-08T16:30:00Z",
  "deletedAt": null
}
```

---

### 15.4 Classes -- /api/v1/master/classes

Same CRUD pattern as departments.

**Request Body (POST/PUT):**

| Field        | Type    | Required | Description                 |
|--------------|---------|----------|-----------------------------|
| `courseId`   | string  | Yes      | Parent course               |
| `semesterId` | string | Yes      | Semester                    |
| `section`    | string  | Yes      | Section identifier          |
| `capacity`   | integer | Yes      | Maximum students            |
| `schedule`   | string  | No       | Schedule description        |

**Response 201 example:**

```json
{
  "id": "class_042",
  "courseId": "course_015",
  "semesterId": "sem_06",
  "section": "A",
  "capacity": 30,
  "enrolled": 0,
  "schedule": "Mon/Wed 10:00-11:30",
  "createdAt": "2026-04-08T16:35:00Z",
  "updatedAt": "2026-04-08T16:35:00Z",
  "deletedAt": null
}
```

---

## 16. Bulk Import

### 16.1 GET /api/v1/bulk/templates/:type

Download an Excel or CSV template for bulk import.

**Required Role:** Recruiter/Staffing Manager, Dispatch Supervisor, Administrator.

**Path Parameters:**

| Param  | Type   | Description                                               |
|--------|--------|-----------------------------------------------------------|
| `type` | string | Template type: `candidates`, `collectors`, `shifts`, `sites`, `departments` |

**Query Parameters:**

| Param    | Type   | Required | Description                  |
|----------|--------|----------|------------------------------|
| `format` | string | No       | `xlsx` (default) or `csv`    |

**Response 200:** Binary file download with appropriate `Content-Type` and `Content-Disposition` headers.

**Error Codes:**

| Status | Code           | When                     |
|--------|----------------|--------------------------|
| 400    | `BAD_REQUEST`  | Unknown template type    |
| 403    | `FORBIDDEN`    | Insufficient role        |

---

### 16.2 POST /api/v1/bulk/import/validate

Upload a file for pre-validation. Returns a row-level error report without persisting any data.

**Required Role:** Recruiter/Staffing Manager, Dispatch Supervisor, Administrator.

**Request Body:** `multipart/form-data`

| Field  | Type   | Required | Description                                              |
|--------|--------|----------|----------------------------------------------------------|
| `type` | string | Yes      | Import type (same as template types)                     |
| `file` | file   | Yes      | Excel or CSV file                                        |

**Response 200:**

```json
{
  "valid": false,
  "totalRows": 150,
  "validRows": 142,
  "errorRows": 8,
  "errors": [
    { "row": 12, "column": "email", "message": "Invalid email format: 'notanemail'" },
    { "row": 45, "column": "phone", "message": "Duplicate phone number in file: +1-555-0101" },
    { "row": 88, "column": "siteId", "message": "Site 'site_999' does not exist" }
  ],
  "warnings": [
    { "row": 23, "column": "skills", "message": "Unrecognized skill 'Jav' -- did you mean 'Java'?" }
  ]
}
```

**Error Codes:**

| Status | Code                   | When                                 |
|--------|------------------------|--------------------------------------|
| 400    | `BAD_REQUEST`          | Unsupported file format or empty file|
| 403    | `FORBIDDEN`            | Insufficient role                    |
| 422    | `UNPROCESSABLE_ENTITY` | File exceeds 10,000 rows            |

**Notes:**
- Maximum file size: 10 MB.
- Maximum rows: 10,000.
- Validation checks: required fields, data types, referential integrity, uniqueness.

---

### 16.3 POST /api/v1/bulk/import/execute

Execute a validated import. This is an async operation.

**Required Role:** Recruiter/Staffing Manager, Dispatch Supervisor, Administrator.

**Request Body:** `multipart/form-data`

| Field          | Type    | Required | Description                              |
|----------------|---------|----------|------------------------------------------|
| `type`         | string  | Yes      | Import type                              |
| `file`         | file    | Yes      | Excel or CSV file                        |
| `skipErrors`   | boolean | No       | Skip invalid rows (default false)        |
| `updateExisting`| boolean| No       | Update existing records on key match (default false) |

**Response 202:**

```json
{
  "jobId": "import_job_044",
  "status": "queued",
  "totalRows": 150,
  "pollUrl": "/api/v1/jobs/import_job_044",
  "createdAt": "2026-04-08T16:40:00Z"
}
```

**Error Codes:**

| Status | Code                   | When                                  |
|--------|------------------------|---------------------------------------|
| 400    | `BAD_REQUEST`          | Unsupported file or missing type      |
| 403    | `FORBIDDEN`            | Insufficient role                     |
| 422    | `UNPROCESSABLE_ENTITY` | Validation errors and skipErrors=false|
| 429    | `RATE_LIMIT_EXCEEDED`  | Another import job is already running |

**Notes:**
- Only one import job may run at a time per user.
- The 90-second timeout applies per processing batch (100 rows per batch).
- If `skipErrors` is true, invalid rows are logged but do not halt the import.

---

### 16.4 GET /api/v1/bulk/import/:jobId/results

Retrieve row-level results for a completed import job.

**Required Role:** Owner of the import job, Administrator.

**Path Parameters:**

| Param   | Type   | Description   |
|---------|--------|---------------|
| `jobId` | string | Import Job ID |

**Query Parameters:**

| Param    | Type    | Required | Description                          |
|----------|---------|----------|--------------------------------------|
| `status` | string  | No       | Filter: `success`, `error`, `skipped`|
| `page`   | integer | No       | Page index                           |
| `size`   | integer | No       | Page size                            |

**Response 200:**

```json
{
  "jobId": "import_job_044",
  "status": "completed",
  "summary": {
    "totalRows": 150,
    "succeeded": 142,
    "failed": 0,
    "skipped": 8
  },
  "content": [
    { "row": 1, "status": "success", "entityId": "cand_4550" },
    { "row": 12, "status": "skipped", "error": "Invalid email format" }
  ],
  "page": 0,
  "size": 25,
  "totalPages": 6,
  "totalElements": 150
}
```

**Error Codes:**

| Status | Code                   | When                              |
|--------|------------------------|-----------------------------------|
| 404    | `NOT_FOUND`            | Job does not exist                |
| 422    | `UNPROCESSABLE_ENTITY` | Job not yet completed             |

---

## 17. Metrics Semantic Layer

### 17.1 POST /api/v1/metrics/definitions

Create a metric definition.

**Required Role:** Administrator.

**Request Body:**

| Field          | Type     | Required | Description                                    |
|----------------|----------|----------|------------------------------------------------|
| `name`         | string   | Yes      | Metric name (e.g., `time_to_fill`)             |
| `displayName`  | string   | Yes      | Human-readable name                            |
| `description`  | string   | Yes      | What this metric measures                      |
| `formula`      | string   | Yes      | SQL expression or calculation formula          |
| `unit`         | string   | Yes      | Unit of measure (`days`, `count`, `currency`, `percent`) |
| `dimensionIds` | string[] | No       | Compatible dimensions                          |
| `tags`         | string[] | No       | Classification tags                            |

**Response 201:**

```json
{
  "id": "metric_001",
  "name": "time_to_fill",
  "displayName": "Time to Fill",
  "description": "Average calendar days from job opening to candidate acceptance.",
  "formula": "AVG(DATEDIFF(filled_date, open_date))",
  "unit": "days",
  "dimensionIds": ["dim_dept", "dim_location"],
  "tags": ["recruiting", "efficiency"],
  "version": 1,
  "status": "DRAFT",
  "createdAt": "2026-04-08T16:45:00Z",
  "updatedAt": "2026-04-08T16:45:00Z"
}
```

**Error Codes:**

| Status | Code        | When                     |
|--------|-------------|--------------------------|
| 403    | `FORBIDDEN` | Non-admin caller         |
| 409    | `CONFLICT`  | Metric name already exists |

---

### 17.2 GET /api/v1/metrics/definitions

List metric definitions.

**Required Role:** Any authenticated user.

**Query Parameters:**

| Param    | Type    | Required | Description                    |
|----------|---------|----------|--------------------------------|
| `page`   | integer | No       | Page index                     |
| `size`   | integer | No       | Page size                      |
| `status` | string  | No       | `DRAFT`, `PUBLISHED`, `DEPRECATED` |
| `tags`   | string  | No       | Comma-separated tag filter     |
| `search` | string  | No       | Search in name/description     |

**Response 200:** Paginated list of metric definition objects.

---

### 17.3 GET /api/v1/metrics/definitions/:id

Retrieve a single metric definition.

**Required Role:** Any authenticated user.

**Response 200:** Full metric definition object.

---

### 17.4 PUT /api/v1/metrics/definitions/:id

Update a metric definition (creates a new draft version).

**Required Role:** Administrator.

**Response 200:** Updated metric definition (version incremented, status set to `DRAFT`).

---

### 17.5 DELETE /api/v1/metrics/definitions/:id

Soft-delete (deprecate) a metric definition.

**Required Role:** Administrator.

**Response 204:** No content.

**Error Codes:**

| Status | Code                   | When                                      |
|--------|------------------------|-------------------------------------------|
| 422    | `UNPROCESSABLE_ENTITY` | Metric is referenced by active dashboards |

---

### 17.6 POST /api/v1/metrics/dimensions

Create a dimension.

**Required Role:** Administrator.

**Request Body:**

| Field         | Type   | Required | Description                             |
|---------------|--------|----------|-----------------------------------------|
| `name`        | string | Yes      | Dimension name (e.g., `department`)     |
| `displayName` | string | Yes      | Human-readable name                     |
| `sourceTable` | string | Yes      | Database table or view                  |
| `sourceColumn`| string | Yes      | Column in source table                  |
| `type`        | string | Yes      | `STRING`, `DATE`, `NUMERIC`, `BOOLEAN`  |

**Response 201:**

```json
{
  "id": "dim_dept",
  "name": "department",
  "displayName": "Department",
  "sourceTable": "departments",
  "sourceColumn": "name",
  "type": "STRING",
  "createdAt": "2026-04-08T16:50:00Z",
  "updatedAt": "2026-04-08T16:50:00Z"
}
```

---

### 17.7 GET /api/v1/metrics/dimensions

List dimensions.

**Required Role:** Any authenticated user.

**Response 200:** Paginated list of dimension objects.

---

### 17.8 GET /api/v1/metrics/dimensions/:id

Retrieve a single dimension.

**Required Role:** Any authenticated user.

**Response 200:** Full dimension object.

---

### 17.9 PUT /api/v1/metrics/dimensions/:id

Update a dimension.

**Required Role:** Administrator.

**Response 200:** Updated dimension object.

---

### 17.10 DELETE /api/v1/metrics/dimensions/:id

Soft-delete a dimension.

**Required Role:** Administrator.

**Response 204:** No content.

---

### 17.11 GET /api/v1/metrics/definitions/:id/versions

List all versions of a metric definition.

**Required Role:** Any authenticated user.

**Path Parameters:**

| Param | Type   | Description          |
|-------|--------|----------------------|
| `id`  | string | Metric Definition ID |

**Response 200:**

```json
{
  "metricId": "metric_001",
  "versions": [
    { "version": 1, "status": "DEPRECATED", "formula": "AVG(DATEDIFF(filled_date, open_date))", "publishedAt": "2026-02-01T10:00:00Z" },
    { "version": 2, "status": "PUBLISHED", "formula": "AVG(DATEDIFF(filled_date, open_date)) WHERE status='FILLED'", "publishedAt": "2026-04-01T10:00:00Z" },
    { "version": 3, "status": "DRAFT", "formula": "PERCENTILE(DATEDIFF(filled_date, open_date), 0.5)", "publishedAt": null }
  ]
}
```

---

### 17.12 POST /api/v1/metrics/definitions/:id/publish

Publish a draft version, making it the active version.

**Required Role:** Administrator.

**Path Parameters:**

| Param | Type   | Description          |
|-------|--------|----------------------|
| `id`  | string | Metric Definition ID |

**Response 200:**

```json
{
  "metricId": "metric_001",
  "version": 3,
  "status": "PUBLISHED",
  "publishedAt": "2026-04-08T16:55:00Z",
  "previousVersion": 2
}
```

**Error Codes:**

| Status | Code                   | When                               |
|--------|------------------------|------------------------------------|
| 403    | `FORBIDDEN`            | Non-admin caller                   |
| 404    | `NOT_FOUND`            | Metric does not exist              |
| 422    | `UNPROCESSABLE_ENTITY` | No draft version available to publish |

---

### 17.13 POST /api/v1/metrics/definitions/:id/rollback

Rollback to a prior published version.

**Required Role:** Administrator.

**Path Parameters:**

| Param | Type   | Description          |
|-------|--------|----------------------|
| `id`  | string | Metric Definition ID |

**Request Body:**

| Field     | Type    | Required | Description                |
|-----------|---------|----------|----------------------------|
| `version` | integer | Yes      | Version number to restore  |

**Response 200:**

```json
{
  "metricId": "metric_001",
  "restoredVersion": 2,
  "currentVersion": 4,
  "status": "PUBLISHED",
  "rolledBackAt": "2026-04-08T17:00:00Z"
}
```

**Error Codes:**

| Status | Code                   | When                                 |
|--------|------------------------|--------------------------------------|
| 403    | `FORBIDDEN`            | Non-admin caller                     |
| 404    | `NOT_FOUND`            | Metric or version does not exist     |
| 422    | `UNPROCESSABLE_ENTITY` | Target version was never published   |

**Notes:**
- Rollback creates a new version with the formula from the target version.
- The rolled-back-from version remains in history for auditing.

---

### 17.14 GET /api/v1/metrics/definitions/:id/lineage

View the data lineage graph for a metric.

**Required Role:** Any authenticated user.

**Response 200:**

```json
{
  "metricId": "metric_001",
  "name": "time_to_fill",
  "sources": [
    { "table": "job_profiles", "columns": ["open_date", "status"] },
    { "table": "pipeline_moves", "columns": ["moved_at", "to_stage"] }
  ],
  "dimensions": [
    { "id": "dim_dept", "name": "department" },
    { "id": "dim_location", "name": "location" }
  ],
  "dependents": [
    { "type": "dashboard_widget", "id": "widget_recruiting_01", "name": "Recruiting Efficiency" }
  ]
}
```

---

### 17.15 GET /api/v1/metrics/query

Query metric data for charts and reports.

**Required Role:** Any authenticated user.

**Query Parameters:**

| Param        | Type   | Required | Description                                          |
|--------------|--------|----------|------------------------------------------------------|
| `metric`     | string | Yes      | Metric name or ID                                    |
| `dimensions` | string | No       | Comma-separated dimension names to group by          |
| `filters`    | string | No       | JSON-encoded filters, e.g. `{"department":"Engineering"}` |
| `dateFrom`   | string | No       | Start date (ISO-8601)                                |
| `dateTo`     | string | No       | End date (ISO-8601)                                  |
| `granularity`| string | No       | `day`, `week`, `month`, `quarter` (default `month`)  |

**Response 200:**

```json
{
  "metric": "time_to_fill",
  "unit": "days",
  "granularity": "month",
  "dimensions": ["department"],
  "data": [
    { "period": "2026-01", "department": "Engineering", "value": 22.3 },
    { "period": "2026-02", "department": "Engineering", "value": 19.8 },
    { "period": "2026-03", "department": "Engineering", "value": 17.1 },
    { "period": "2026-01", "department": "Marketing", "value": 15.0 },
    { "period": "2026-02", "department": "Marketing", "value": 14.2 },
    { "period": "2026-03", "department": "Marketing", "value": 13.5 }
  ],
  "aggregates": {
    "overall": 17.0,
    "trend": -0.12
  }
}
```

**Error Codes:**

| Status | Code                   | When                                  |
|--------|------------------------|---------------------------------------|
| 400    | `BAD_REQUEST`          | Missing metric parameter              |
| 404    | `NOT_FOUND`            | Metric does not exist or is not published |
| 422    | `UNPROCESSABLE_ENTITY` | Incompatible dimension for this metric|

---

## 18. Payments

### 18.1 POST /api/v1/payments

Record a payment.

**Required Role:** Finance Clerk, Administrator.

**Request Body:**

| Field         | Type   | Required | Description                                         |
|---------------|--------|----------|-----------------------------------------------------|
| `amount`      | number | Yes      | Payment amount (positive decimal)                   |
| `currency`    | string | No       | ISO 4217 code (default `USD`)                       |
| `channel`     | string | Yes      | `CASH`, `CHECK`, `MANUAL_CARD`                      |
| `referenceId` | string | No       | External reference (check number, receipt ID)       |
| `payerId`     | string | Yes      | ID of the paying entity                             |
| `description` | string | No       | Payment description                                 |
| `metadata`    | object | No       | Arbitrary key-value pairs                           |

**Response 201:**

```json
{
  "id": "pay_1001",
  "amount": 250.00,
  "currency": "USD",
  "channel": "CHECK",
  "referenceId": "CHK-44521",
  "payerId": "ent_201",
  "description": "Monthly service fee - April",
  "status": "RECORDED",
  "metadata": {},
  "recordedBy": "usr_005",
  "createdAt": "2026-04-08T17:05:00Z",
  "updatedAt": "2026-04-08T17:05:00Z"
}
```

**Error Codes:**

| Status | Code                   | When                                 |
|--------|------------------------|--------------------------------------|
| 400    | `BAD_REQUEST`          | Missing required fields              |
| 403    | `FORBIDDEN`            | Insufficient role                    |
| 409    | `CONFLICT`             | Duplicate referenceId for same channel |
| 422    | `UNPROCESSABLE_ENTITY` | Negative amount or invalid channel   |

**Notes:**
- Payments are idempotent on `(channel, referenceId)` pairs. A duplicate returns 409 with the existing payment.
- See [Appendix D](#appendix-d--payment-state-machine) for state transitions.

---

### 18.2 GET /api/v1/payments

List payments with filters.

**Required Role:** Finance Clerk, Administrator.

**Query Parameters:**

| Param      | Type    | Required | Description                                 |
|------------|---------|----------|---------------------------------------------|
| `channel`  | string  | No       | Filter by channel                           |
| `status`   | string  | No       | `RECORDED`, `CLEARED`, `REFUNDED`, `PARTIAL_REFUND`, `VOIDED` |
| `dateFrom` | string  | No       | Start date                                  |
| `dateTo`   | string  | No       | End date                                    |
| `payerId`  | string  | No       | Filter by payer                             |
| `minAmount`| number  | No       | Minimum amount                              |
| `maxAmount`| number  | No       | Maximum amount                              |
| `page`     | integer | No       | Page index                                  |
| `size`     | integer | No       | Page size                                   |

**Response 200:** Paginated list of payment objects.

---

### 18.3 GET /api/v1/payments/:id

Retrieve a single payment.

**Required Role:** Finance Clerk, Administrator.

**Response 200:** Full payment object.

**Error Codes:**

| Status | Code        | When                    |
|--------|-------------|-------------------------|
| 404    | `NOT_FOUND` | Payment does not exist  |

**Notes:**
- Sensitive fields (bank account, card last 4) are masked for non-Administrator roles.

---

### 18.4 POST /api/v1/payments/:id/refund

Issue a full or partial refund.

**Required Role:** Finance Clerk, Administrator.

**Path Parameters:**

| Param | Type   | Description |
|-------|--------|-------------|
| `id`  | string | Payment ID  |

**Request Body:**

| Field    | Type   | Required | Description                                         |
|----------|--------|----------|-----------------------------------------------------|
| `amount` | number | Yes      | Refund amount (must be <= remaining refundable amount) |
| `reason` | string | Yes      | Reason for refund                                   |

**Response 201:**

```json
{
  "refundId": "ref_501",
  "paymentId": "pay_1001",
  "amount": 100.00,
  "reason": "Service not rendered for 2 days",
  "status": "PROCESSED",
  "remainingRefundable": 150.00,
  "paymentStatus": "PARTIAL_REFUND",
  "processedBy": "usr_005",
  "createdAt": "2026-04-08T17:10:00Z"
}
```

**Error Codes:**

| Status | Code                   | When                                           |
|--------|------------------------|-------------------------------------------------|
| 403    | `FORBIDDEN`            | Insufficient role                               |
| 404    | `NOT_FOUND`            | Payment does not exist                          |
| 422    | `UNPROCESSABLE_ENTITY` | Refund amount exceeds remaining or payment voided |

**Notes:**
- Multiple partial refunds are allowed up to the original payment amount.
- When total refunds equal the original amount, payment status becomes `REFUNDED`.

---

### 18.5 GET /api/v1/payments/:id/refunds

List all refunds for a specific payment.

**Required Role:** Finance Clerk, Administrator.

**Response 200:**

```json
{
  "paymentId": "pay_1001",
  "originalAmount": 250.00,
  "totalRefunded": 100.00,
  "remainingRefundable": 150.00,
  "refunds": [
    {
      "refundId": "ref_501",
      "amount": 100.00,
      "reason": "Service not rendered for 2 days",
      "status": "PROCESSED",
      "processedBy": "usr_005",
      "createdAt": "2026-04-08T17:10:00Z"
    }
  ]
}
```

---

## 19. Bank File Import & Reconciliation

### 19.1 POST /api/v1/bank-files/import

Import a bank statement file for reconciliation.

**Required Role:** Finance Clerk, Administrator.

**Request Body:** `multipart/form-data`

| Field       | Type   | Required | Description                             |
|-------------|--------|----------|-----------------------------------------|
| `file`      | file   | Yes      | Bank file (CSV, OFX, MT940)            |
| `bankName`  | string | Yes      | Originating bank name                   |
| `accountId` | string | Yes      | Internal account identifier             |

**Response 200 (file already imported):**

```json
{
  "fileId": "bf_201",
  "status": "ALREADY_IMPORTED",
  "fileHash": "sha256:abcdef1234567890",
  "originalImportedAt": "2026-04-07T10:00:00Z"
}
```

**Response 201 (new import):**

```json
{
  "fileId": "bf_202",
  "fileName": "april_statement.csv",
  "bankName": "First National",
  "accountId": "acct_01",
  "fileHash": "sha256:1234567890abcdef",
  "transactionCount": 87,
  "status": "IMPORTED",
  "importedAt": "2026-04-08T17:15:00Z"
}
```

**Error Codes:**

| Status | Code                   | When                                 |
|--------|------------------------|--------------------------------------|
| 400    | `BAD_REQUEST`          | Unsupported file format              |
| 403    | `FORBIDDEN`            | Insufficient role                    |
| 422    | `UNPROCESSABLE_ENTITY` | File parsing failed (corrupt or malformed) |

**Notes:**
- Idempotent by SHA-256 file hash. Re-uploading the same file returns the existing record with status `ALREADY_IMPORTED` and HTTP 200 (not 201).
- Supported formats: CSV (configurable column mapping), OFX, MT940.

---

### 19.2 GET /api/v1/bank-files

List imported bank files.

**Required Role:** Finance Clerk, Administrator.

**Query Parameters:**

| Param      | Type    | Required | Description             |
|------------|---------|----------|-------------------------|
| `bankName` | string  | No       | Filter by bank          |
| `dateFrom` | string  | No       | Imported after date     |
| `dateTo`   | string  | No       | Imported before date    |
| `page`     | integer | No       | Page index              |
| `size`     | integer | No       | Page size               |

**Response 200:** Paginated list of bank file objects.

---

### 19.3 GET /api/v1/reconciliation

Get overall reconciliation status.

**Required Role:** Finance Clerk, Administrator.

**Query Parameters:**

| Param      | Type   | Required | Description          |
|------------|--------|----------|----------------------|
| `dateFrom` | string | No       | Start date           |
| `dateTo`   | string | No       | End date             |

**Response 200:**

```json
{
  "period": { "from": "2026-04-01", "to": "2026-04-08" },
  "totalBankTransactions": 452,
  "matched": 438,
  "unmatched": 10,
  "exceptions": 4,
  "matchRate": 96.9,
  "lastReconciled": "2026-04-08T17:00:00Z"
}
```

---

### 19.4 GET /api/v1/reconciliation/exceptions

List reconciliation exceptions requiring manual review.

**Required Role:** Finance Clerk, Administrator.

**Query Parameters:**

| Param    | Type    | Required | Description                                  |
|----------|---------|----------|----------------------------------------------|
| `status` | string  | No       | `OPEN`, `RESOLVED`, `ESCALATED`              |
| `type`   | string  | No       | `AMOUNT_MISMATCH`, `MISSING_PAYMENT`, `DUPLICATE`, `UNKNOWN` |
| `page`   | integer | No       | Page index                                   |
| `size`   | integer | No       | Page size                                    |

**Response 200:**

```json
{
  "content": [
    {
      "id": "exc_301",
      "type": "AMOUNT_MISMATCH",
      "bankTransaction": {
        "reference": "TXN-88712",
        "amount": 255.00,
        "date": "2026-04-07"
      },
      "matchedPayment": {
        "id": "pay_1001",
        "amount": 250.00
      },
      "difference": 5.00,
      "status": "OPEN",
      "createdAt": "2026-04-08T17:00:00Z"
    }
  ],
  "page": 0,
  "size": 25,
  "totalPages": 1,
  "totalElements": 4
}
```

---

### 19.5 PATCH /api/v1/reconciliation/exceptions/:id

Resolve a reconciliation exception.

**Required Role:** Finance Clerk, Administrator.

**Path Parameters:**

| Param | Type   | Description  |
|-------|--------|--------------|
| `id`  | string | Exception ID |

**Request Body:**

| Field        | Type   | Required | Description                                       |
|--------------|--------|----------|---------------------------------------------------|
| `resolution` | string | Yes      | `MATCHED`, `WRITTEN_OFF`, `ESCALATED`, `ADJUSTED` |
| `notes`      | string | Yes      | Explanation of resolution                         |
| `adjustmentAmount` | number | No | If resolution is `ADJUSTED`, the correction amount |

**Response 200:**

```json
{
  "id": "exc_301",
  "status": "RESOLVED",
  "resolution": "ADJUSTED",
  "notes": "Bank fee of $5.00 applied. Adjustment recorded.",
  "adjustmentAmount": 5.00,
  "resolvedBy": "usr_005",
  "resolvedAt": "2026-04-08T17:20:00Z"
}
```

**Error Codes:**

| Status | Code                   | When                                |
|--------|------------------------|-------------------------------------|
| 404    | `NOT_FOUND`            | Exception does not exist            |
| 422    | `UNPROCESSABLE_ENTITY` | Already resolved or invalid resolution type |

---

### 19.6 POST /api/v1/settlement/generate

Generate a monthly settlement CSV. Async operation.

**Required Role:** Finance Clerk, Administrator.

**Request Body:**

| Field   | Type   | Required | Description                     |
|---------|--------|----------|---------------------------------|
| `month` | string | Yes      | Settlement month (`YYYY-MM`)    |

**Response 202:**

```json
{
  "jobId": "settle_job_015",
  "month": "2026-03",
  "status": "queued",
  "pollUrl": "/api/v1/jobs/settle_job_015",
  "createdAt": "2026-04-08T17:25:00Z"
}
```

**Error Codes:**

| Status | Code                   | When                                           |
|--------|------------------------|-------------------------------------------------|
| 403    | `FORBIDDEN`            | Insufficient role                               |
| 409    | `CONFLICT`             | Settlement for this month already generated     |
| 422    | `UNPROCESSABLE_ENTITY` | Open reconciliation exceptions exist for this month |

**Notes:**
- All reconciliation exceptions for the target month must be resolved before settlement can be generated.
- Idempotent: if a settlement already exists for the month, returns 409 with a reference to the existing settlement.

---

### 19.7 GET /api/v1/settlement

List settlement statements.

**Required Role:** Finance Clerk, Administrator.

**Query Parameters:**

| Param  | Type    | Required | Description |
|--------|---------|----------|-------------|
| `year` | integer | No       | Filter by year |
| `page` | integer | No       | Page index  |
| `size` | integer | No       | Page size   |

**Response 200:**

```json
{
  "content": [
    {
      "id": "settle_001",
      "month": "2026-03",
      "totalAmount": 145230.50,
      "transactionCount": 452,
      "status": "FINALIZED",
      "downloadUrl": "/api/v1/settlement/settle_001/download",
      "generatedAt": "2026-04-01T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 25,
  "totalPages": 1,
  "totalElements": 3
}
```

---

### 19.8 GET /api/v1/settlement/:id/download

Download a settlement CSV file.

**Required Role:** Finance Clerk, Administrator.

**Response 200:** Binary CSV download.

**Error Codes:**

| Status | Code        | When                         |
|--------|-------------|------------------------------|
| 404    | `NOT_FOUND` | Settlement does not exist    |

---

## 20. Face Recognition

### 20.1 POST /api/v1/identity/enroll

Submit a face image for feature extraction and enrollment.

**Required Role:** Dispatch Supervisor, Administrator.

**Request Body:** `multipart/form-data`

| Field    | Type   | Required | Description                                   |
|----------|--------|----------|-----------------------------------------------|
| `userId` | string | Yes      | User or collector ID to enroll                |
| `image`  | file   | Yes      | Face image (JPEG or PNG, max 5 MB)            |

**Response 202:**

```json
{
  "userId": "col_301",
  "enrollmentId": "enroll_091",
  "status": "PROCESSING",
  "message": "Face image submitted for feature extraction.",
  "pollUrl": "/api/v1/identity/col_301/status"
}
```

**Error Codes:**

| Status | Code                   | When                                       |
|--------|------------------------|--------------------------------------------|
| 400    | `BAD_REQUEST`          | Missing image or unsupported format        |
| 403    | `FORBIDDEN`            | Insufficient role                          |
| 409    | `CONFLICT`             | User already enrolled (re-enroll explicitly)|
| 422    | `UNPROCESSABLE_ENTITY` | No face detected in image or multiple faces |

**Notes:**
- The image is forwarded to the Python face recognition service on the local network.
- Feature extraction is async; poll the status endpoint for completion.
- Images are stored only until feature extraction completes, then deleted. Only the feature vector is retained.

---

### 20.2 POST /api/v1/identity/verify

Verify a live face image against a stored enrollment template.

**Required Role:** Dispatch Supervisor, Administrator.

**Request Body:** `multipart/form-data`

| Field    | Type   | Required | Description                        |
|----------|--------|----------|------------------------------------|
| `userId` | string | Yes      | User or collector ID to verify     |
| `image`  | file   | Yes      | Live face image (JPEG or PNG)      |

**Response 200:**

```json
{
  "userId": "col_301",
  "verified": true,
  "confidence": 0.97,
  "threshold": 0.85,
  "verifiedAt": "2026-04-08T17:30:00Z"
}
```

**Response 200 (verification failed):**

```json
{
  "userId": "col_301",
  "verified": false,
  "confidence": 0.42,
  "threshold": 0.85,
  "verifiedAt": "2026-04-08T17:30:00Z"
}
```

**Error Codes:**

| Status | Code                   | When                                       |
|--------|------------------------|--------------------------------------------|
| 400    | `BAD_REQUEST`          | Missing image or unsupported format        |
| 403    | `FORBIDDEN`            | Insufficient role                          |
| 404    | `NOT_FOUND`            | User not enrolled                          |
| 422    | `UNPROCESSABLE_ENTITY` | No face detected in image                  |

**Notes:**
- Verification requests have a 90-second timeout. If the Python service does not respond in time, the request fails.
- The `confidence` score ranges from 0.0 to 1.0. The server-side `threshold` is configurable (default 0.85).
- Verification images are not retained after the comparison.

---

### 20.3 GET /api/v1/identity/:userId/status

Check the enrollment status for a user.

**Required Role:** Dispatch Supervisor, Administrator.

**Path Parameters:**

| Param    | Type   | Description             |
|----------|--------|-------------------------|
| `userId` | string | User or collector ID    |

**Response 200:**

```json
{
  "userId": "col_301",
  "enrolled": true,
  "enrolledAt": "2026-04-08T17:28:00Z",
  "featureVersion": "v2.1",
  "lastVerified": "2026-04-08T17:30:00Z"
}
```

**Response 200 (not enrolled):**

```json
{
  "userId": "col_999",
  "enrolled": false,
  "enrolledAt": null,
  "featureVersion": null,
  "lastVerified": null
}
```

**Error Codes:**

| Status | Code        | When              |
|--------|-------------|-------------------|
| 403    | `FORBIDDEN` | Insufficient role |

---

## 21. Async Jobs

### 21.1 GET /api/v1/jobs

List async jobs for the current user (Admins see all).

**Required Role:** Any authenticated user (filtered to own jobs; Administrator sees all).

**Query Parameters:**

| Param    | Type    | Required | Description                                          |
|----------|---------|----------|------------------------------------------------------|
| `type`   | string  | No       | `EXPORT`, `IMPORT`, `SETTLEMENT`, `ENROLLMENT`       |
| `status` | string  | No       | `queued`, `running`, `completed`, `failed`            |
| `page`   | integer | No       | Page index                                           |
| `size`   | integer | No       | Page size                                            |

**Response 200:**

```json
{
  "content": [
    {
      "id": "export_job_091",
      "type": "EXPORT",
      "status": "completed",
      "progress": 100,
      "createdAt": "2026-04-08T16:15:00Z",
      "startedAt": "2026-04-08T16:15:02Z",
      "completedAt": "2026-04-08T16:15:45Z",
      "resultUrl": "/api/v1/exports/export_job_091/download"
    }
  ],
  "page": 0,
  "size": 25,
  "totalPages": 1,
  "totalElements": 5
}
```

---

### 21.2 GET /api/v1/jobs/:id

Get details and progress for a specific job.

**Required Role:** Owner of the job, Administrator.

**Path Parameters:**

| Param | Type   | Description |
|-------|--------|-------------|
| `id`  | string | Job ID      |

**Response 200:**

```json
{
  "id": "import_job_044",
  "type": "IMPORT",
  "status": "running",
  "progress": 65,
  "progressDetail": "Processing row 975 of 1500",
  "createdAt": "2026-04-08T16:40:00Z",
  "startedAt": "2026-04-08T16:40:05Z",
  "completedAt": null,
  "error": null
}
```

**Response 200 (failed job):**

```json
{
  "id": "import_job_045",
  "type": "IMPORT",
  "status": "failed",
  "progress": 33,
  "progressDetail": "Failed at row 500",
  "createdAt": "2026-04-08T16:50:00Z",
  "startedAt": "2026-04-08T16:50:03Z",
  "completedAt": "2026-04-08T16:51:30Z",
  "error": {
    "code": "IMPORT_FAILED",
    "message": "Database constraint violation at row 500: duplicate email"
  }
}
```

**Error Codes:**

| Status | Code        | When                     |
|--------|-------------|--------------------------|
| 403    | `FORBIDDEN` | Not the owner            |
| 404    | `NOT_FOUND` | Job does not exist       |

---

### 21.3 GET /api/v1/jobs/:id/result

Download the result file for a completed job.

**Required Role:** Owner of the job, Administrator.

**Response 200:** Binary file download.

**Error Codes:**

| Status | Code                   | When                              |
|--------|------------------------|-----------------------------------|
| 403    | `FORBIDDEN`            | Not the owner                     |
| 404    | `NOT_FOUND`            | Job does not exist                |
| 422    | `UNPROCESSABLE_ENTITY` | Job not completed or has no result file |

---

## 22. Audit Log

### 22.1 GET /api/v1/audit-log

Query the audit log.

**Required Role:** Administrator.

**Query Parameters:**

| Param           | Type    | Required | Description                                    |
|-----------------|---------|----------|------------------------------------------------|
| `userId`        | string  | No       | Filter by acting user                          |
| `action`        | string  | No       | Filter by action (e.g., `CREATE`, `UPDATE`, `DELETE`, `LOGIN`, `EXPORT`) |
| `resource`      | string  | No       | Filter by resource type (e.g., `candidate`, `payment`, `dispatch_job`) |
| `resourceId`    | string  | No       | Filter by specific resource ID                 |
| `dateFrom`      | string  | No       | Start of time range (ISO-8601)                 |
| `dateTo`        | string  | No       | End of time range (ISO-8601)                   |
| `workstationId` | string  | No       | Filter by workstation/machine identifier       |
| `page`          | integer | No       | Page index                                     |
| `size`          | integer | No       | Page size                                      |

**Response 200:**

```json
{
  "content": [
    {
      "id": "audit_90001",
      "userId": "usr_001",
      "username": "jdoe",
      "action": "UPDATE",
      "resource": "candidate",
      "resourceId": "cand_4501",
      "changes": {
        "skills": { "before": ["Java"], "after": ["Java", "Spring Boot", "MySQL"] }
      },
      "ipAddress": "192.168.1.45",
      "workstationId": "WS-045",
      "userAgent": "Mozilla/5.0",
      "timestamp": "2026-04-08T14:30:00Z"
    }
  ],
  "page": 0,
  "size": 25,
  "totalPages": 40,
  "totalElements": 998
}
```

**Error Codes:**

| Status | Code        | When             |
|--------|-------------|------------------|
| 403    | `FORBIDDEN` | Non-admin caller |

**Notes:**
- Audit log entries are immutable and cannot be deleted.
- The `changes` field captures before/after values for update operations.
- Audit log retention is 7 years.

---

### 22.2 POST /api/v1/audit-log/export

Export audit log entries as a CSV. Async operation.

**Required Role:** Administrator.

**Request Body:**

| Field      | Type   | Required | Description                 |
|------------|--------|----------|-----------------------------|
| `dateFrom` | string | Yes      | Start of time range         |
| `dateTo`   | string | Yes      | End of time range           |
| `filters`  | object | No       | Same filters as GET endpoint|
| `format`   | string | No       | `CSV` (default) or `XLSX`   |

**Response 202:**

```json
{
  "jobId": "audit_export_005",
  "status": "queued",
  "pollUrl": "/api/v1/jobs/audit_export_005",
  "createdAt": "2026-04-08T17:35:00Z"
}
```

**Error Codes:**

| Status | Code                   | When                                |
|--------|------------------------|-------------------------------------|
| 403    | `FORBIDDEN`            | Non-admin caller                    |
| 422    | `UNPROCESSABLE_ENTITY` | Date range exceeds 1 year           |

---

## 23. Health & Monitoring

### 23.1 GET /api/v1/health

System health check. Returns the status of all dependent services.

**Required Role:** None (public, but detailed info requires authentication).

**Response 200:**

```json
{
  "status": "UP",
  "timestamp": "2026-04-08T17:40:00Z",
  "components": {
    "database": {
      "status": "UP",
      "responseTime": 12,
      "details": { "type": "MySQL", "version": "8.0.35" }
    },
    "pythonFaceService": {
      "status": "UP",
      "responseTime": 45,
      "details": { "url": "http://192.168.1.100:5000", "version": "2.1.0" }
    },
    "diskSpace": {
      "status": "UP",
      "details": { "total": "500 GB", "free": "320 GB", "usedPercent": 36.0 }
    },
    "jobQueue": {
      "status": "UP",
      "details": { "pending": 2, "running": 1, "workers": 4 }
    }
  }
}
```

**Response 503 (degraded):**

```json
{
  "status": "DEGRADED",
  "timestamp": "2026-04-08T17:40:00Z",
  "components": {
    "database": { "status": "UP", "responseTime": 12 },
    "pythonFaceService": {
      "status": "DOWN",
      "responseTime": null,
      "details": { "error": "Connection refused" }
    },
    "diskSpace": { "status": "UP" },
    "jobQueue": { "status": "UP" }
  }
}
```

**Notes:**
- Unauthenticated requests receive only the top-level `status`. Authenticated requests include component details.
- Returns HTTP 200 when all components are UP, HTTP 503 when any component is DOWN.

---

### 23.2 GET /api/v1/metrics/system

System performance metrics.

**Required Role:** Administrator.

**Query Parameters:**

| Param      | Type   | Required | Description                              |
|------------|--------|----------|------------------------------------------|
| `period`   | string | No       | `1h`, `6h`, `24h`, `7d` (default `1h`)  |

**Response 200:**

```json
{
  "period": "1h",
  "from": "2026-04-08T16:40:00Z",
  "to": "2026-04-08T17:40:00Z",
  "requests": {
    "total": 4521,
    "perMinute": 75.4,
    "byStatus": { "2xx": 4380, "4xx": 128, "5xx": 13 }
  },
  "latency": {
    "p50": 45,
    "p90": 180,
    "p95": 320,
    "p99": 890,
    "unit": "ms"
  },
  "jobQueue": {
    "pending": 2,
    "running": 1,
    "completedLastHour": 14,
    "failedLastHour": 1,
    "avgDuration": 23400,
    "unit": "ms"
  },
  "database": {
    "activeConnections": 12,
    "maxConnections": 50,
    "avgQueryTime": 8,
    "slowQueries": 3,
    "unit": "ms"
  },
  "memory": {
    "heapUsed": "512 MB",
    "heapMax": "2048 MB",
    "usedPercent": 25.0
  }
}
```

**Error Codes:**

| Status | Code        | When             |
|--------|-------------|------------------|
| 403    | `FORBIDDEN` | Non-admin caller |

---

## Appendix A -- Permission Matrix

The following table maps each role to its feature access. **C** = Create, **R** = Read, **U** = Update, **D** = Delete. A dash (--) indicates no access.

| Feature                     | Administrator | Recruiter/Staffing Manager | Dispatch Supervisor | Finance Clerk |
|-----------------------------|:-------------:|:--------------------------:|:-------------------:|:-------------:|
| Users & Roles               | CRUD          | --                         | --                  | --            |
| Dashboard                   | R             | R                          | R                   | R             |
| Candidate Profiles          | CRUD          | CRUD                       | R                   | --            |
| Candidate Search            | R             | R                          | --                  | --            |
| Candidate Bulk Tag          | CU            | CU                         | --                  | --            |
| Match Rationale             | R             | R                          | --                  | --            |
| Job Profiles                | CRUD          | CRUD                       | R                   | --            |
| Talent Pools                | CRUD          | CRUD                       | --                  | --            |
| Pipeline Management         | CRUD          | CRUD                       | --                  | --            |
| Saved Searches & Snapshots  | CRUD          | CRUD                       | --                  | --            |
| Collectors                  | CRUD          | --                         | CRUD                | --            |
| Sites                       | CRUD          | --                         | CRUD                | --            |
| Shifts & Scheduling         | CRUD          | --                         | CRUD                | --            |
| Dispatch Jobs               | CRUD          | --                         | CRUD                | --            |
| Dispatch Config             | CU            | --                         | CU                  | --            |
| Unified Search              | R             | R                          | R                   | R             |
| Exports                     | CR            | CR                         | CR                  | CR            |
| Master Data                 | CRUD          | R                          | R                   | R             |
| Bulk Import                 | CRUD          | CRU                        | CRU                 | --            |
| Metrics Definitions         | CRUD          | R                          | R                   | R             |
| Metrics Dimensions          | CRUD          | R                          | R                   | R             |
| Metrics Query               | R             | R                          | R                   | R             |
| Payments                    | CRUD          | --                         | --                  | CRU           |
| Bank File Import            | CRUD          | --                         | --                  | CRU           |
| Reconciliation              | CRUD          | --                         | --                  | RU            |
| Settlement                  | CRUD          | --                         | --                  | CR            |
| Face Recognition            | CRUD          | --                         | CRU                 | --            |
| Async Jobs                  | R (all)       | R (own)                    | R (own)             | R (own)       |
| Audit Log                   | R             | --                         | --                  | --            |
| Audit Log Export            | CR            | --                         | --                  | --            |
| Health Check                | R (detailed)  | R (summary)                | R (summary)         | R (summary)   |
| System Metrics              | R             | --                         | --                  | --            |

---

## Appendix B -- Pipeline Stage Transitions

Candidates move through the following stages. Arrows indicate permitted transitions.

```
Applied ──> Screening ──> Interview ──> Offer ──> Hired
   │            │             │           │
   │            │             │           └──> Rejected
   │            │             └──────────────> Rejected
   │            └────────────────────────────> Rejected
   └─────────────────────────────────────────> Rejected
```

**Transition Rules:**

| From        | Allowed Targets                    |
|-------------|-------------------------------------|
| Applied     | Screening, Rejected                 |
| Screening   | Interview, Rejected                 |
| Interview   | Offer, Screening (re-screen), Rejected |
| Offer       | Hired, Interview (re-interview), Rejected |
| Hired       | -- (terminal state)                 |
| Rejected    | Applied (re-activate, Admin only)   |

**Constraints:**
- Forward transitions may skip at most one stage (e.g., Applied to Interview is not allowed).
- Moving to `Rejected` is always allowed from any non-terminal stage.
- Re-activating a rejected candidate (Rejected to Applied) requires Administrator role.
- All transitions are recorded in the audit log with timestamp, user, and optional reason.

---

## Appendix C -- Dispatch Job State Machine

```
                            ┌──────────────────────┐
                            │                      │
  Created ──> Offered ──> Accepted ──> In Progress ──> Completed
                 │                         │
                 │                         └──> Cancelled
                 │
                 └──> Timeout ──> Redispatched ──> Offered
                                                     │
                                                     └──> (cycle repeats)
```

**State Definitions:**

| State          | Description                                                 |
|----------------|-------------------------------------------------------------|
| `CREATED`      | Job created but not yet dispatched                          |
| `OFFERED`      | Job offered to eligible collectors (grab-order mode)        |
| `ACCEPTED`     | A collector has accepted the job                            |
| `IN_PROGRESS`  | Collector has started work                                  |
| `COMPLETED`    | Job finished successfully                                   |
| `TIMEOUT`      | No collector accepted within the `offerTimeout` window      |
| `REDISPATCHED` | Timed-out job re-entered the dispatch queue                 |
| `CANCELLED`    | Job cancelled by supervisor                                 |

**Transition Rules:**

| From           | To               | Trigger                                        |
|----------------|------------------|------------------------------------------------|
| CREATED        | OFFERED          | Automatic in grab-order mode                   |
| CREATED        | ACCEPTED         | Direct assignment in assigned-order mode       |
| OFFERED        | ACCEPTED         | Collector accepts via `/accept`                |
| OFFERED        | TIMEOUT          | `offerTimeout` seconds elapsed with no acceptance |
| OFFERED        | CANCELLED        | Supervisor cancels                             |
| ACCEPTED       | IN_PROGRESS      | Collector starts work                          |
| ACCEPTED       | ACCEPTED         | Reassigned to different collector              |
| ACCEPTED       | CANCELLED        | Supervisor cancels                             |
| IN_PROGRESS    | COMPLETED        | Collector marks job done                       |
| IN_PROGRESS    | CANCELLED        | Supervisor cancels (exceptional)               |
| TIMEOUT        | REDISPATCHED     | Automatic                                      |
| REDISPATCHED   | OFFERED          | Automatic (re-enters grab-order queue)         |

**Notes:**
- In assigned-order mode, jobs skip the `OFFERED` state entirely.
- A job can be redispatched a maximum of 3 times before requiring manual intervention.
- The `TIMEOUT` to `REDISPATCHED` transition is automatic and immediate.

---

## Appendix D -- Payment State Machine

```
  RECORDED ──> CLEARED ──> PARTIAL_REFUND ──> REFUNDED
     │            │
     │            └──> REFUNDED
     │
     └──> VOIDED
```

**State Definitions:**

| State            | Description                                       |
|------------------|---------------------------------------------------|
| `RECORDED`       | Payment entered into the system                   |
| `CLEARED`        | Payment verified/cleared (e.g., check cleared)    |
| `PARTIAL_REFUND` | One or more partial refunds issued                |
| `REFUNDED`       | Fully refunded (total refunds = original amount)  |
| `VOIDED`         | Payment voided before clearing                    |

**Transition Rules:**

| From             | To               | Trigger                              |
|------------------|------------------|--------------------------------------|
| RECORDED         | CLEARED          | Bank reconciliation confirms payment |
| RECORDED         | VOIDED           | Manual void before clearing          |
| CLEARED          | PARTIAL_REFUND   | Partial refund issued                |
| CLEARED          | REFUNDED         | Full refund issued                   |
| PARTIAL_REFUND   | PARTIAL_REFUND   | Additional partial refund            |
| PARTIAL_REFUND   | REFUNDED         | Remaining balance refunded           |

**Constraints:**
- `VOIDED` and `REFUNDED` are terminal states.
- A payment cannot be voided after it has been cleared.
- Refunds can only be issued against `CLEARED` or `PARTIAL_REFUND` payments.
- Total refund amount must never exceed the original payment amount.

---

## Appendix E -- Rate Limit Headers

Every response includes the following rate limit headers:

| Header                  | Description                                            | Example        |
|-------------------------|--------------------------------------------------------|----------------|
| `X-RateLimit-Limit`     | Maximum requests allowed per window                   | `30`           |
| `X-RateLimit-Remaining` | Requests remaining in the current window              | `27`           |
| `X-RateLimit-Reset`     | Unix timestamp (seconds) when the window resets       | `1744126860`   |
| `X-RateLimit-Window`    | Window duration in seconds                            | `60`           |

**When the limit is exceeded** (HTTP 429), the following additional header is included:

| Header         | Description                                  | Example |
|----------------|----------------------------------------------|---------|
| `Retry-After`  | Seconds until the client may retry           | `18`    |

**Example 429 Response:**

```http
HTTP/1.1 429 Too Many Requests
Content-Type: application/json
X-RateLimit-Limit: 30
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1744126860
X-RateLimit-Window: 60
Retry-After: 18

{
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "You have exceeded the rate limit of 30 requests per minute. Please retry after 18 seconds.",
    "details": {
      "limit": 30,
      "window": "60s",
      "retryAfter": 18
    }
  }
}
```

**Special Rate Limits:**

| Endpoint               | Limit                     | Window     | Notes                         |
|------------------------|---------------------------|------------|-------------------------------|
| `POST /auth/login`     | 5 failed attempts         | 15 minutes | Per username, not per session |
| `POST /exports`        | 3 concurrent jobs         | --         | Per user                      |
| `POST /bulk/import/execute` | 1 concurrent job     | --         | Per user                      |
| `POST /identity/verify`| 10 requests               | 1 minute   | Per userId                    |
| All other endpoints    | 30 requests               | 1 minute   | Per authenticated user        |
