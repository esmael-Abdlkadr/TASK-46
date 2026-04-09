# Workforce & Talent Operations Hub -- Design Document

**Version:** 1.0
**Date:** 2026-04-08
**Status:** Draft

---

## Table of Contents

1. [Introduction & Scope](#1-introduction--scope)
2. [System Architecture](#2-system-architecture)
3. [Technology Stack](#3-technology-stack)
4. [Database Design](#4-database-design)
5. [Authentication & Session Management](#5-authentication--session-management)
6. [Authorization & RBAC](#6-authorization--rbac)
7. [Core Modules](#7-core-modules)
8. [Bulk Import/Export](#8-bulk-importexport)
9. [Python Face Recognition Integration](#9-python-face-recognition-integration)
10. [Async Job Queue](#10-async-job-queue)
11. [Metrics Semantic Layer](#11-metrics-semantic-layer)
12. [Payments, Settlement & Reconciliation](#12-payments-settlement--reconciliation)
13. [File Storage](#13-file-storage)
14. [Security](#14-security)
15. [Audit Trail](#15-audit-trail)
16. [Deployment](#16-deployment)

---

## 1. Introduction & Scope

### 1.1 Purpose

The Workforce & Talent Operations Hub is an on-premise, offline-first platform for managing the complete lifecycle of workforce operations: recruiting, talent matching, dispatch scheduling, payment settlement, and organizational reference data. The system is designed to operate entirely within a local network with zero dependency on external cloud services or internet connectivity.

### 1.2 Scope

The platform covers the following functional areas:

- **Recruiting Pipeline** -- Candidate sourcing, intelligent matching, talent pools, and pipeline management with explainable scoring rationale.
- **Dispatch Management** -- Shift-based assignment of collectors to sites with two dispatch modes (grab-order and assigned-order), capacity enforcement, and automatic redispatch on timeout.
- **Unified Search** -- Cross-entity search across members, enterprises, resources, orders, and redemption records with saved criteria and queued exports.
- **Master Data Management** -- Organizational reference tables for departments, courses, semesters, and classes adapted as staffing and training reference data.
- **Payments & Settlement** -- Offline-only payment recording (cash, check, manual card entry), bank file reconciliation, refunds, and monthly settlement statements.
- **Face Recognition** -- Optional biometric identity verification via a locally hosted Python service.
- **Metrics Semantic Layer** -- Versioned metric definitions with lineage tracking, rollback capability, and consistent charting across releases.
- **Audit & Security** -- Immutable audit logging, field-level data masking, encryption at rest for biometric data, and role-based access control.

### 1.3 Roles

| Role | Description |
|---|---|
| **Administrator** | Full system access. Manages users, roles, permissions, metric publication, and system configuration. |
| **Recruiter/Staffing Manager** | Manages candidate profiles, job profiles, talent pools, pipeline stages, and intelligent matching searches. |
| **Dispatch Supervisor** | Manages collector and site profiles, shift definitions, dispatch jobs, and monitors acceptance/redispatch workflows. |
| **Finance Clerk** | Records payments, processes refunds, imports bank files, runs reconciliation, and generates settlement statements. |

### 1.4 Design Principles

- **Offline-first**: Every feature must function without internet connectivity.
- **On-premise**: All data, services, and processing remain within the organization's network segment.
- **Auditability**: Every user action is logged with user ID, timestamp, and workstation ID.
- **Explainability**: Matching and scoring results include human-readable rationale that persists with search snapshots.
- **Idempotency**: File imports and bank callbacks are safe to retry without producing duplicate effects.

---

## 2. System Architecture

### 2.1 High-Level Architecture

All components reside on a single network segment with no outbound internet access required.

```
+-------------------------------------------------------+
|                  On-Premise Network                    |
|                                                       |
|  +-------------------+      +---------------------+  |
|  |  Browser (Client) |      |  Python Face Recog  |  |
|  |  HTML / CSS / JS  |      |  Service (HTTP)     |  |
|  +--------+----------+      +----------+----------+  |
|           |                            |              |
|           | HTTP/HTTPS                 | HTTP (local) |
|           |                            |              |
|  +--------v----------------------------v----------+   |
|  |           Spring Boot App Server               |   |
|  |                                                |   |
|  |  +------------+  +-------------+  +----------+ |   |
|  |  | REST API   |  | Async Queue |  | Security | |   |
|  |  | Controllers|  | (Internal)  |  | Filters  | |   |
|  |  +------------+  +-------------+  +----------+ |   |
|  |  +------------+  +-------------+  +----------+ |   |
|  |  | Service    |  | Scheduled   |  | Rate     | |   |
|  |  | Layer      |  | Tasks       |  | Limiter  | |   |
|  |  +------------+  +-------------+  +----------+ |   |
|  |                                                |   |
|  +-------------------+----------------------------+   |
|                      |                                |
|           +----------v----------+                     |
|           |       MySQL         |                     |
|           |  (Single Instance)  |                     |
|           +---------------------+                     |
|                                                       |
|           +---------------------+                     |
|           |  Local File System  |                     |
|           |  (Exports, Uploads) |                     |
|           +---------------------+                     |
+-------------------------------------------------------+
```

### 2.2 Component Responsibilities

| Component | Responsibility |
|---|---|
| **Browser Client** | Static HTML/CSS/JS served by Spring Boot. Communicates with the backend exclusively through REST API calls (JSON over HTTP). No server-side template rendering. |
| **Spring Boot App Server** | Hosts REST API endpoints, serves static frontend files from `/static` or `/public`, manages sessions, enforces RBAC, runs the internal async job queue, and communicates with the Python face recognition service. |
| **MySQL Database** | Single relational database instance storing all application data, audit logs, encrypted biometric templates, metric definitions, and job queue state. |
| **Python Face Recognition Service** | Standalone HTTP service on the same network segment. Accepts face images, extracts feature vectors, and performs 1:1 or 1:N matching. Communicates with Spring Boot over local HTTP. |
| **Local File System** | Stores uploaded files (Excel/CSV imports, exported reports, face images pending processing). The database holds metadata pointers (path, size, SHA-256 hash). |
| **Async Job Queue** | An internal, database-backed queue managed by the Spring Boot application. Handles long-running tasks: face recognition requests, batch imports, report generation, and export jobs. |

### 2.3 Request Flow

1. The browser loads static HTML/CSS/JS assets from the Spring Boot server.
2. JavaScript makes REST API calls (`fetch` / `XMLHttpRequest`) to Spring Boot endpoints.
3. Spring Boot security filters authenticate the session, enforce RBAC, and apply rate limiting.
4. The controller delegates to the service layer, which interacts with MySQL via Spring Data JPA / JDBC.
5. For long-running operations, the service layer enqueues an async job and returns a job ID to the client.
6. The client polls the job status endpoint or the user navigates to the Exports page to check completion.
7. For face recognition, the service layer makes a synchronous or async HTTP call to the Python service on the local network.

### 2.4 Network Topology

- All components share one network segment (e.g., `10.0.1.0/24`).
- No firewall rules required between components; a host-level firewall may restrict access to specific ports.
- Spring Boot listens on port `8080` (HTTP) or `8443` (HTTPS with self-signed or internal CA certificate).
- Python Face Recognition Service listens on port `5000`.
- MySQL listens on port `3306`, accepting connections only from the Spring Boot server.

---

## 3. Technology Stack

### 3.1 Backend

| Technology | Version (Minimum) | Purpose |
|---|---|---|
| Java | 17 LTS | Runtime language |
| Spring Boot | 3.x | Application framework, embedded Tomcat, REST controllers, dependency injection |
| Spring Security | 6.x | Authentication, session management, RBAC filter chain |
| Spring Data JPA / Hibernate | 3.x / 6.x | ORM, repository pattern, database migrations |
| Spring Async (`@Async`, `ThreadPoolTaskExecutor`) | 3.x | Internal async job processing |
| Jackson | 2.x | JSON serialization/deserialization for REST APIs |
| Apache POI | 5.x | Excel (`.xlsx`) file reading and writing for bulk import/export |
| OpenCSV | 5.x | CSV parsing and generation |
| BCrypt / Argon2 | (Spring Security built-in) | Password hashing. Argon2 preferred; BCrypt as fallback. |
| Flyway or Liquibase | Latest | Database schema migration and versioning |
| RestTemplate / WebClient | (Spring built-in) | HTTP client for communicating with the Python face recognition service |

### 3.2 Frontend

| Technology | Purpose |
|---|---|
| HTML5 | Page structure, semantic markup |
| CSS3 | Styling, responsive layout, field-level masking presentation |
| Vanilla JavaScript (ES6+) | DOM manipulation, REST API calls via `fetch`, client-side routing (hash-based or History API), form validation, polling for async job status |

The frontend is a collection of static `.html`, `.css`, and `.js` files placed in the Spring Boot project's `src/main/resources/static/` directory. Spring Boot's embedded Tomcat serves them directly. There is no server-side template engine (no Thymeleaf, no JSP, no Freemarker).

### 3.3 Database

| Technology | Version | Purpose |
|---|---|---|
| MySQL | 8.x | Primary relational data store |

### 3.4 Face Recognition Service

| Technology | Version | Purpose |
|---|---|---|
| Python | 3.10+ | Runtime |
| Flask or FastAPI | Latest | HTTP API framework |
| dlib / face_recognition | Latest | Face feature extraction and matching |
| NumPy | Latest | Numerical operations on feature vectors |

### 3.5 Build & Dependency Management

| Tool | Purpose |
|---|---|
| Maven or Gradle | Java build, dependency management, packaging as JAR/WAR |
| pip / requirements.txt | Python dependency management |

---

## 4. Database Design

All tables use InnoDB engine with `utf8mb4` character set. Primary keys are auto-incrementing `BIGINT` unless noted. Timestamps are `DATETIME` in UTC. Soft deletes use a `deleted_at DATETIME NULL` column where applicable.

### 4.1 Authentication & Authorization

#### `users`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| username | VARCHAR(100) | UNIQUE, NOT NULL | Login identifier |
| password_hash | VARCHAR(255) | NOT NULL | Argon2/BCrypt hash |
| display_name | VARCHAR(200) | NOT NULL | Full name shown in UI |
| email | VARCHAR(255) | UNIQUE, NULL | Optional contact email |
| is_active | BOOLEAN | NOT NULL, DEFAULT TRUE | Account enabled flag |
| failed_login_attempts | INT | NOT NULL, DEFAULT 0 | Consecutive failed logins |
| locked_until | DATETIME | NULL | Account lockout expiry |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |
| deleted_at | DATETIME | NULL | Soft delete |

#### `roles`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| name | VARCHAR(50) | UNIQUE, NOT NULL | e.g., ADMINISTRATOR, RECRUITER, DISPATCH_SUPERVISOR, FINANCE_CLERK |
| description | VARCHAR(500) | NULL | |
| created_at | DATETIME | NOT NULL | |

#### `permissions`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| code | VARCHAR(100) | UNIQUE, NOT NULL | e.g., CANDIDATE_VIEW, CANDIDATE_EDIT, DISPATCH_ASSIGN, PAYMENT_RECORD |
| description | VARCHAR(500) | NULL | |
| resource | VARCHAR(100) | NOT NULL | Target entity or module |
| action | VARCHAR(50) | NOT NULL | CREATE, READ, UPDATE, DELETE, EXPORT, PUBLISH |

#### `role_permissions`

| Column | Type | Constraints | Description |
|---|---|---|---|
| role_id | BIGINT | PK (composite), FK -> roles.id | |
| permission_id | BIGINT | PK (composite), FK -> permissions.id | |

#### `user_roles`

| Column | Type | Constraints | Description |
|---|---|---|---|
| user_id | BIGINT | PK (composite), FK -> users.id | |
| role_id | BIGINT | PK (composite), FK -> roles.id | |
| assigned_at | DATETIME | NOT NULL | |
| assigned_by | BIGINT | FK -> users.id | |

#### `sessions`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | VARCHAR(128) | PK | Session token (cryptographically random) |
| user_id | BIGINT | FK -> users.id, NOT NULL | |
| workstation_id | VARCHAR(200) | NOT NULL | IP address or machine hostname |
| created_at | DATETIME | NOT NULL | |
| expires_at | DATETIME | NOT NULL | |
| last_activity_at | DATETIME | NOT NULL | |
| is_revoked | BOOLEAN | NOT NULL, DEFAULT FALSE | |

### 4.2 Audit

#### `audit_log`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| user_id | BIGINT | FK -> users.id, NOT NULL | Who performed the action |
| action | VARCHAR(50) | NOT NULL | CREATE, UPDATE, DELETE, LOGIN, LOGOUT, EXPORT, SEARCH, etc. |
| resource_type | VARCHAR(100) | NOT NULL | e.g., candidate_profile, dispatch_job, payment |
| resource_id | VARCHAR(100) | NULL | ID of affected record (NULL for bulk ops or searches) |
| detail | JSON | NULL | Snapshot of changed fields (before/after) or search parameters |
| workstation_id | VARCHAR(200) | NOT NULL | IP address or machine hostname |
| timestamp | DATETIME | NOT NULL | UTC |

**Note:** This table is append-only. No UPDATE or DELETE operations are permitted. A database trigger or application-level enforcement ensures immutability. The table is indexed on `(user_id, timestamp)`, `(resource_type, resource_id)`, and `(timestamp)`.

### 4.3 Recruiting Pipeline

#### `candidate_profiles`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| first_name | VARCHAR(100) | NOT NULL | |
| last_name | VARCHAR(100) | NOT NULL | |
| email | VARCHAR(255) | NULL | |
| phone | VARCHAR(50) | NULL | |
| phone_masked | VARCHAR(50) | GENERATED | Last 4 digits only, for UI display |
| years_experience | INT | NULL | Total years of professional experience |
| current_title | VARCHAR(200) | NULL | |
| location | VARCHAR(200) | NULL | |
| availability_date | DATE | NULL | Earliest start date |
| resume_file_id | BIGINT | FK -> files.id, NULL | |
| profile_data | JSON | NULL | Structured data: certifications, education, preferences |
| status | VARCHAR(50) | NOT NULL, DEFAULT 'ACTIVE' | ACTIVE, INACTIVE, ARCHIVED |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |
| created_by | BIGINT | FK -> users.id | |

#### `candidate_skills`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| candidate_id | BIGINT | FK -> candidate_profiles.id, NOT NULL | |
| skill_name | VARCHAR(200) | NOT NULL | Normalized skill name |
| years_experience | INT | NULL | Years with this specific skill |
| proficiency_level | VARCHAR(50) | NULL | BEGINNER, INTERMEDIATE, ADVANCED, EXPERT |
| is_primary | BOOLEAN | NOT NULL, DEFAULT FALSE | Candidate's self-reported primary skill |

**Index:** `(candidate_id)`, `(skill_name, years_experience)`

#### `job_profiles`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| title | VARCHAR(200) | NOT NULL | Job title |
| department_id | BIGINT | FK -> departments.id, NULL | |
| description | TEXT | NULL | |
| location | VARCHAR(200) | NULL | |
| employment_type | VARCHAR(50) | NOT NULL | FULL_TIME, PART_TIME, CONTRACT, TEMPORARY |
| status | VARCHAR(50) | NOT NULL, DEFAULT 'OPEN' | DRAFT, OPEN, FILLED, CANCELLED |
| opened_at | DATETIME | NULL | |
| closed_at | DATETIME | NULL | |
| created_by | BIGINT | FK -> users.id | |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

#### `skill_requirements`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| job_profile_id | BIGINT | FK -> job_profiles.id, NOT NULL | |
| skill_name | VARCHAR(200) | NOT NULL | |
| min_years | INT | NULL | Minimum required years |
| is_required | BOOLEAN | NOT NULL, DEFAULT TRUE | Required vs. preferred |
| weight | DECIMAL(5,2) | NOT NULL, DEFAULT 1.00 | Weight for scoring algorithm (higher = more important) |

#### `talent_pools`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| name | VARCHAR(200) | NOT NULL | |
| description | TEXT | NULL | |
| created_by | BIGINT | FK -> users.id | |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

#### `pool_members`

| Column | Type | Constraints | Description |
|---|---|---|---|
| pool_id | BIGINT | PK (composite), FK -> talent_pools.id | |
| candidate_id | BIGINT | PK (composite), FK -> candidate_profiles.id | |
| added_at | DATETIME | NOT NULL | |
| added_by | BIGINT | FK -> users.id | |
| tag | VARCHAR(100) | NULL | Optional grouping tag |

#### `pipeline_stages`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| name | VARCHAR(100) | NOT NULL | e.g., SOURCED, SCREENED, INTERVIEWED, OFFERED, HIRED, REJECTED |
| display_order | INT | NOT NULL | Ordering in UI |
| is_terminal | BOOLEAN | NOT NULL, DEFAULT FALSE | HIRED and REJECTED are terminal |

#### `pipeline_moves`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| candidate_id | BIGINT | FK -> candidate_profiles.id, NOT NULL | |
| job_profile_id | BIGINT | FK -> job_profiles.id, NOT NULL | |
| from_stage_id | BIGINT | FK -> pipeline_stages.id, NULL | NULL for initial placement |
| to_stage_id | BIGINT | FK -> pipeline_stages.id, NOT NULL | |
| moved_by | BIGINT | FK -> users.id, NOT NULL | |
| moved_at | DATETIME | NOT NULL | |
| is_batch | BOOLEAN | NOT NULL, DEFAULT FALSE | Part of a batch move |
| batch_id | VARCHAR(36) | NULL | UUID grouping batch moves for undo |
| undone_at | DATETIME | NULL | Non-null means this move was reversed |
| undo_deadline | DATETIME | NULL | Timestamp after which undo expires (moved_at + 60 seconds) |

**Undo mechanism:** When a batch move is performed, all resulting `pipeline_moves` rows share the same `batch_id` and have `undo_deadline` set to `moved_at + 60 seconds`. A client-side timer shows the undo option. If the user clicks undo before the deadline, the system sets `undone_at` and creates reverse `pipeline_moves` entries. After the deadline, undo is no longer available.

#### `search_snapshots`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| name | VARCHAR(200) | NULL | Optional saved search name |
| user_id | BIGINT | FK -> users.id, NOT NULL | |
| search_criteria | JSON | NOT NULL | Full filter tree with Boolean logic and weights |
| job_profile_id | BIGINT | FK -> job_profiles.id, NULL | Associated job, if any |
| executed_at | DATETIME | NOT NULL | |
| is_saved | BOOLEAN | NOT NULL, DEFAULT FALSE | User chose to save this search |

#### `search_results`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| snapshot_id | BIGINT | FK -> search_snapshots.id, NOT NULL | |
| candidate_id | BIGINT | FK -> candidate_profiles.id, NOT NULL | |
| overall_score | DECIMAL(7,4) | NOT NULL | Weighted composite score at time of search |
| rationale | JSON | NOT NULL | Explainable breakdown (see Section 7.1) |
| rank | INT | NOT NULL | Position in result set |

**Key design choice:** Scores and rationale are computed at search time and stored permanently in `search_results`. Even if a candidate's profile changes later, the "why matched" data in the snapshot remains stable and auditable.

### 4.4 Dispatch Management

#### `collectors`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| employee_id | VARCHAR(50) | UNIQUE, NOT NULL | Internal employee reference |
| first_name | VARCHAR(100) | NOT NULL | |
| last_name | VARCHAR(100) | NOT NULL | |
| phone | VARCHAR(50) | NULL | |
| is_active | BOOLEAN | NOT NULL, DEFAULT TRUE | |
| biometric_template_id | BIGINT | FK -> biometric_templates.id, NULL | Optional face recognition link |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

#### `sites`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| name | VARCHAR(200) | NOT NULL | |
| address | VARCHAR(500) | NULL | |
| capacity | INT | NOT NULL | Maximum number of collectors per shift |
| is_active | BOOLEAN | NOT NULL, DEFAULT TRUE | |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

#### `shifts`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| site_id | BIGINT | FK -> sites.id, NOT NULL | |
| shift_date | DATE | NOT NULL | |
| start_time | TIME | NOT NULL | Must align to 15-minute boundary (00, 15, 30, 45) |
| end_time | TIME | NOT NULL | Must align to 15-minute boundary |
| required_collectors | INT | NOT NULL | Number of collectors needed |
| assigned_count | INT | NOT NULL, DEFAULT 0 | Current number assigned (denormalized, updated transactionally) |
| status | VARCHAR(50) | NOT NULL, DEFAULT 'OPEN' | OPEN, FILLED, IN_PROGRESS, COMPLETED, CANCELLED |
| created_at | DATETIME | NOT NULL | |

**Constraint:** `CHECK (MINUTE(start_time) IN (0, 15, 30, 45) AND MINUTE(end_time) IN (0, 15, 30, 45))` enforces 15-minute increment alignment. `CHECK (assigned_count <= required_collectors)` is enforced at application level with optimistic locking to allow capacity checks under concurrent access.

#### `dispatch_jobs`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| shift_id | BIGINT | FK -> shifts.id, NOT NULL | |
| mode | VARCHAR(20) | NOT NULL | GRAB_ORDER or ASSIGNED_ORDER |
| status | VARCHAR(50) | NOT NULL, DEFAULT 'PENDING' | PENDING, DISPATCHED, ACCEPTED, COMPLETED, CANCELLED, TIMED_OUT |
| created_by | BIGINT | FK -> users.id, NOT NULL | Dispatch Supervisor who created |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

#### `dispatch_assignments`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| dispatch_job_id | BIGINT | FK -> dispatch_jobs.id, NOT NULL | |
| collector_id | BIGINT | FK -> collectors.id, NOT NULL | |
| status | VARCHAR(50) | NOT NULL, DEFAULT 'OFFERED' | OFFERED, ACCEPTED, DECLINED, TIMED_OUT, CANCELLED |
| offered_at | DATETIME | NOT NULL | |
| acceptance_deadline | DATETIME | NOT NULL | `offered_at + 90 seconds` |
| responded_at | DATETIME | NULL | When the collector accepted or declined |
| redispatch_count | INT | NOT NULL, DEFAULT 0 | Number of times this slot was re-offered |

**Index:** `(dispatch_job_id, status)`, `(collector_id, status)`

### 4.5 Master Data (Organizational Reference)

These tables are adapted from a traditional academic structure to serve as organizational training and staffing reference tables.

#### `departments`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| code | VARCHAR(20) | UNIQUE, NOT NULL | Short code (e.g., ENG, OPS, HR) |
| name | VARCHAR(200) | NOT NULL | |
| parent_department_id | BIGINT | FK -> departments.id, NULL | Hierarchical nesting |
| is_active | BOOLEAN | NOT NULL, DEFAULT TRUE | |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

#### `courses`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| code | VARCHAR(20) | UNIQUE, NOT NULL | Training/course code |
| name | VARCHAR(200) | NOT NULL | |
| department_id | BIGINT | FK -> departments.id, NOT NULL | |
| credit_hours | INT | NULL | Duration or weight |
| is_active | BOOLEAN | NOT NULL, DEFAULT TRUE | |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

**Unique constraint:** `(code)` -- no duplicate course codes.

#### `semesters`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| code | VARCHAR(20) | UNIQUE, NOT NULL | e.g., 2026-Q1, 2026-H1 |
| name | VARCHAR(100) | NOT NULL | |
| start_date | DATE | NOT NULL | |
| end_date | DATE | NOT NULL | |
| is_current | BOOLEAN | NOT NULL, DEFAULT FALSE | Only one row should be TRUE at a time |
| created_at | DATETIME | NOT NULL | |

#### `classes`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| course_id | BIGINT | FK -> courses.id, NOT NULL | |
| semester_id | BIGINT | FK -> semesters.id, NOT NULL | |
| instructor_name | VARCHAR(200) | NULL | Trainer or lead |
| max_enrollment | INT | NULL | Capacity |
| current_enrollment | INT | NOT NULL, DEFAULT 0 | |
| schedule_info | VARCHAR(500) | NULL | Free-text schedule description |
| created_at | DATETIME | NOT NULL | |

**Unique constraint:** `(course_id, semester_id)` -- one class per course per semester unless explicitly duplicated with a section identifier.

### 4.6 Unified Search Entities

#### `members`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| member_code | VARCHAR(50) | UNIQUE, NOT NULL | External-facing identifier |
| first_name | VARCHAR(100) | NOT NULL | |
| last_name | VARCHAR(100) | NOT NULL | |
| email | VARCHAR(255) | NULL | |
| phone | VARCHAR(50) | NULL | |
| member_type | VARCHAR(50) | NOT NULL | INDIVIDUAL, CORPORATE_REP |
| status | VARCHAR(50) | NOT NULL, DEFAULT 'ACTIVE' | |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

#### `enterprises`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| enterprise_code | VARCHAR(50) | UNIQUE, NOT NULL | |
| name | VARCHAR(300) | NOT NULL | |
| industry | VARCHAR(100) | NULL | |
| contact_name | VARCHAR(200) | NULL | |
| contact_phone | VARCHAR(50) | NULL | |
| contact_email | VARCHAR(255) | NULL | |
| address | VARCHAR(500) | NULL | |
| status | VARCHAR(50) | NOT NULL, DEFAULT 'ACTIVE' | |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

#### `resources`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| resource_code | VARCHAR(50) | UNIQUE, NOT NULL | |
| name | VARCHAR(200) | NOT NULL | |
| category | VARCHAR(100) | NOT NULL | Equipment, vehicle, facility, etc. |
| description | TEXT | NULL | |
| status | VARCHAR(50) | NOT NULL, DEFAULT 'AVAILABLE' | AVAILABLE, IN_USE, MAINTENANCE, RETIRED |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

#### `orders`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| order_number | VARCHAR(50) | UNIQUE, NOT NULL | |
| member_id | BIGINT | FK -> members.id, NULL | |
| enterprise_id | BIGINT | FK -> enterprises.id, NULL | |
| order_type | VARCHAR(50) | NOT NULL | SERVICE, STAFFING, SUPPLY |
| status | VARCHAR(50) | NOT NULL, DEFAULT 'PENDING' | PENDING, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED |
| total_amount | DECIMAL(15,2) | NULL | |
| currency | VARCHAR(3) | NOT NULL, DEFAULT 'USD' | |
| notes | TEXT | NULL | |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

#### `redemption_records`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| order_id | BIGINT | FK -> orders.id, NOT NULL | |
| member_id | BIGINT | FK -> members.id, NULL | |
| resource_id | BIGINT | FK -> resources.id, NULL | |
| redeemed_at | DATETIME | NOT NULL | |
| quantity | INT | NOT NULL, DEFAULT 1 | |
| notes | TEXT | NULL | |
| created_by | BIGINT | FK -> users.id | |

### 4.7 Metrics Semantic Layer

#### `metric_definitions`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| code | VARCHAR(100) | UNIQUE, NOT NULL | Stable identifier across versions |
| name | VARCHAR(200) | NOT NULL | Human-readable name |
| description | TEXT | NULL | |
| data_type | VARCHAR(50) | NOT NULL | INTEGER, DECIMAL, PERCENTAGE, RATIO |
| aggregation_type | VARCHAR(50) | NOT NULL | SUM, AVG, COUNT, MIN, MAX, CUSTOM |
| unit | VARCHAR(50) | NULL | e.g., USD, hours, count |
| created_at | DATETIME | NOT NULL | |
| created_by | BIGINT | FK -> users.id | |

#### `metric_versions`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| metric_id | BIGINT | FK -> metric_definitions.id, NOT NULL | |
| version_number | INT | NOT NULL | Monotonically increasing per metric |
| definition_sql | TEXT | NOT NULL | SQL expression or calculation logic |
| parameters | JSON | NULL | Configuration parameters for the calculation |
| is_published | BOOLEAN | NOT NULL, DEFAULT FALSE | Only Administrators can set TRUE |
| published_at | DATETIME | NULL | |
| published_by | BIGINT | FK -> users.id, NULL | |
| is_active | BOOLEAN | NOT NULL, DEFAULT FALSE | Only one version active per metric |
| created_at | DATETIME | NOT NULL | |
| created_by | BIGINT | FK -> users.id | |

**Unique constraint:** `(metric_id, version_number)`
**Business rule:** Only one `is_active = TRUE` per `metric_id` at any time, enforced at application level.

#### `dimension_definitions`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| code | VARCHAR(100) | UNIQUE, NOT NULL | |
| name | VARCHAR(200) | NOT NULL | |
| source_table | VARCHAR(100) | NOT NULL | Table providing dimension values |
| source_column | VARCHAR(100) | NOT NULL | Column to group by |
| hierarchy_level | INT | NULL | For hierarchical dimensions |
| parent_dimension_id | BIGINT | FK -> dimension_definitions.id, NULL | |
| created_at | DATETIME | NOT NULL | |

#### `derived_metrics`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| code | VARCHAR(100) | UNIQUE, NOT NULL | |
| name | VARCHAR(200) | NOT NULL | |
| formula | TEXT | NOT NULL | Expression referencing other metric codes |
| input_metric_ids | JSON | NOT NULL | Array of metric_definition IDs used |
| window_function | VARCHAR(100) | NULL | e.g., ROLLING_AVG_7D, CUMULATIVE, YTD |
| window_params | JSON | NULL | Window size, offset, partition keys |
| is_published | BOOLEAN | NOT NULL, DEFAULT FALSE | |
| published_by | BIGINT | FK -> users.id, NULL | |
| created_at | DATETIME | NOT NULL | |

#### `metric_lineage`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| upstream_metric_id | BIGINT | FK -> metric_definitions.id, NOT NULL | |
| downstream_metric_id | BIGINT | FK -> derived_metrics.id, NOT NULL | |
| relationship_type | VARCHAR(50) | NOT NULL | INPUT, FILTER, DIMENSION |

### 4.8 Payments & Settlement

#### `payments`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| payment_reference | VARCHAR(50) | UNIQUE, NOT NULL | System-generated reference number |
| order_id | BIGINT | FK -> orders.id, NULL | |
| member_id | BIGINT | FK -> members.id, NULL | |
| amount | DECIMAL(15,2) | NOT NULL | |
| currency | VARCHAR(3) | NOT NULL, DEFAULT 'USD' | |
| channel | VARCHAR(50) | NOT NULL | CASH, CHECK, MANUAL_CARD |
| status | VARCHAR(50) | NOT NULL, DEFAULT 'RECORDED' | RECORDED, VERIFIED, RECONCILED, REFUNDED_PARTIAL, REFUNDED_FULL |
| channel_detail | JSON | NULL | Check number, last 4 of card, etc. (masked) |
| recorded_by | BIGINT | FK -> users.id, NOT NULL | |
| recorded_at | DATETIME | NOT NULL | |
| notes | TEXT | NULL | |

**Allowed channels enforced by application-level validation:** Only CASH, CHECK, and MANUAL_CARD are accepted. No online or network-based payment processing.

#### `payment_channels`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| code | VARCHAR(50) | UNIQUE, NOT NULL | CASH, CHECK, MANUAL_CARD |
| name | VARCHAR(100) | NOT NULL | |
| is_active | BOOLEAN | NOT NULL, DEFAULT TRUE | |
| requires_detail | BOOLEAN | NOT NULL | Whether channel_detail is required (TRUE for CHECK and MANUAL_CARD) |

#### `refunds`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| payment_id | BIGINT | FK -> payments.id, NOT NULL | |
| refund_reference | VARCHAR(50) | UNIQUE, NOT NULL | |
| amount | DECIMAL(15,2) | NOT NULL | Refund amount (may be less than original for partial) |
| refund_type | VARCHAR(20) | NOT NULL | FULL or PARTIAL |
| reason | TEXT | NULL | |
| processed_by | BIGINT | FK -> users.id, NOT NULL | |
| processed_at | DATETIME | NOT NULL | |

**Constraint:** Sum of refund amounts for a payment must not exceed the original payment amount. Enforced at application level.

#### `bank_file_imports`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| file_id | BIGINT | FK -> files.id, NOT NULL | Reference to uploaded bank file |
| file_sha256 | VARCHAR(64) | UNIQUE, NOT NULL | SHA-256 of the imported file, prevents re-import |
| record_count | INT | NOT NULL | Number of transaction rows in file |
| matched_count | INT | NOT NULL, DEFAULT 0 | Rows successfully matched to payments |
| unmatched_count | INT | NOT NULL, DEFAULT 0 | Rows sent to exception queue |
| status | VARCHAR(50) | NOT NULL, DEFAULT 'PENDING' | PENDING, PROCESSING, COMPLETED, COMPLETED_WITH_EXCEPTIONS, FAILED |
| imported_by | BIGINT | FK -> users.id, NOT NULL | |
| imported_at | DATETIME | NOT NULL | |

**Idempotency:** If a file with the same SHA-256 hash has already been imported, the system rejects the upload with a clear error message. This prevents accidental re-import of the same bank statement.

#### `reconciliation_records`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| bank_file_import_id | BIGINT | FK -> bank_file_imports.id, NOT NULL | |
| payment_id | BIGINT | FK -> payments.id, NULL | NULL if unmatched |
| bank_reference | VARCHAR(100) | NOT NULL | Reference from bank file |
| bank_amount | DECIMAL(15,2) | NOT NULL | Amount per bank file |
| bank_date | DATE | NOT NULL | |
| match_status | VARCHAR(50) | NOT NULL | MATCHED, UNMATCHED, DISCREPANT |
| discrepancy_amount | DECIMAL(15,2) | NULL | Difference if amounts don't match |
| resolved_at | DATETIME | NULL | |
| resolved_by | BIGINT | FK -> users.id, NULL | |
| resolution_notes | TEXT | NULL | |

#### `exception_queue`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| reconciliation_record_id | BIGINT | FK -> reconciliation_records.id, NOT NULL | |
| exception_type | VARCHAR(50) | NOT NULL | UNMATCHED_BANK, UNMATCHED_PAYMENT, AMOUNT_DISCREPANCY, DATE_DISCREPANCY |
| priority | VARCHAR(20) | NOT NULL, DEFAULT 'NORMAL' | LOW, NORMAL, HIGH, CRITICAL |
| status | VARCHAR(50) | NOT NULL, DEFAULT 'OPEN' | OPEN, IN_REVIEW, RESOLVED, WRITTEN_OFF |
| assigned_to | BIGINT | FK -> users.id, NULL | |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

#### `settlement_statements`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| statement_period_start | DATE | NOT NULL | First day of month |
| statement_period_end | DATE | NOT NULL | Last day of month |
| total_payments | DECIMAL(15,2) | NOT NULL | |
| total_refunds | DECIMAL(15,2) | NOT NULL | |
| net_amount | DECIMAL(15,2) | NOT NULL | |
| record_count | INT | NOT NULL | |
| export_file_id | BIGINT | FK -> files.id, NULL | CSV file reference |
| generated_by | BIGINT | FK -> users.id, NOT NULL | |
| generated_at | DATETIME | NOT NULL | |

### 4.9 File Storage & Biometrics

#### `files`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| original_name | VARCHAR(500) | NOT NULL | Original filename at upload |
| storage_path | VARCHAR(1000) | NOT NULL | Absolute path on local file system |
| mime_type | VARCHAR(100) | NOT NULL | |
| size_bytes | BIGINT | NOT NULL | |
| sha256 | VARCHAR(64) | NOT NULL | SHA-256 hash of file contents |
| uploaded_by | BIGINT | FK -> users.id, NOT NULL | |
| uploaded_at | DATETIME | NOT NULL | |
| is_deleted | BOOLEAN | NOT NULL, DEFAULT FALSE | Soft delete |

**Index:** `(sha256)` for deduplication checks.

#### `biometric_templates`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| person_type | VARCHAR(50) | NOT NULL | COLLECTOR, CANDIDATE, MEMBER |
| person_id | BIGINT | NOT NULL | FK reference dependent on person_type |
| template_data | VARBINARY(8000) | NOT NULL | AES-256 encrypted face feature vector |
| encryption_key_id | VARCHAR(100) | NOT NULL | Reference to the local key used for encryption |
| source_file_id | BIGINT | FK -> files.id, NULL | Original face image file |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |

**Security:** The `template_data` column stores the face feature vector encrypted with AES-256 using a local key. The key is stored in a local keystore file on the server, never in the database. The `encryption_key_id` identifies which key version was used, supporting key rotation.

### 4.10 Async Jobs & Exports

#### `async_jobs`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| type | VARCHAR(100) | NOT NULL | FACE_RECOGNITION, BATCH_IMPORT, REPORT_GENERATION, EXPORT, RECONCILIATION |
| status | VARCHAR(50) | NOT NULL, DEFAULT 'QUEUED' | QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED |
| progress_percent | INT | NOT NULL, DEFAULT 0 | 0-100 |
| input_params | JSON | NULL | Parameters for the job |
| result_summary | JSON | NULL | Summary of results |
| result_file_id | BIGINT | FK -> files.id, NULL | Output file if applicable |
| error_message | TEXT | NULL | Error details if FAILED |
| retry_count | INT | NOT NULL, DEFAULT 0 | |
| max_retries | INT | NOT NULL, DEFAULT 3 | |
| created_by | BIGINT | FK -> users.id, NOT NULL | |
| created_at | DATETIME | NOT NULL | |
| started_at | DATETIME | NULL | |
| completed_at | DATETIME | NULL | |

#### `export_jobs`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| user_id | BIGINT | FK -> users.id, NOT NULL | |
| export_type | VARCHAR(100) | NOT NULL | CANDIDATES, PAYMENTS, SETTLEMENT, DISPATCH_REPORT, SEARCH_RESULTS |
| status | VARCHAR(50) | NOT NULL, DEFAULT 'QUEUED' | QUEUED, RUNNING, COMPLETED, FAILED |
| filters_applied | JSON | NULL | Search/filter criteria used |
| file_id | BIGINT | FK -> files.id, NULL | Generated file |
| record_count | INT | NULL | Number of records exported |
| created_at | DATETIME | NOT NULL | |
| completed_at | DATETIME | NULL | |

**User experience:** The Exports page lists all `export_jobs` for the current user, showing status and download links for completed exports.

---

## 5. Authentication & Session Management

### 5.1 Login Flow

1. User navigates to the login page (`/login.html`).
2. JavaScript sends `POST /api/auth/login` with `{ username, password, workstationId }`.
   - `workstationId` is the IP address or machine hostname, captured by the frontend via a configuration variable or derived server-side from the request's remote address.
3. Spring Security's authentication provider:
   a. Looks up the user by username.
   b. Checks `is_active` and `locked_until`.
   c. Verifies the password against the stored Argon2/BCrypt hash.
   d. On failure: increments `failed_login_attempts`. After 5 consecutive failures, sets `locked_until` to `NOW + 30 minutes`.
   e. On success: resets `failed_login_attempts` to 0. Creates a `sessions` row with a cryptographically random session ID.
4. The session ID is returned as an HTTP-only, Secure (if HTTPS), SameSite=Strict cookie.
5. All subsequent API requests include this cookie. The security filter chain validates the session on every request.

### 5.2 Session Lifecycle

- **Idle timeout:** 30 minutes of inactivity (configurable). Each API request updates `last_activity_at`.
- **Absolute timeout:** 8 hours from creation (configurable). After this, the session expires regardless of activity.
- **Explicit logout:** `POST /api/auth/logout` sets `is_revoked = TRUE` on the session and clears the cookie.
- **Concurrent sessions:** A user may have multiple active sessions (e.g., different workstations). Administrators can view and revoke sessions for any user.

### 5.3 Password Policy

- Minimum 12 characters.
- Must contain at least one uppercase letter, one lowercase letter, one digit, and one special character.
- Password history: last 5 passwords cannot be reused (hashes stored in a separate `password_history` table).
- Passwords are hashed with Argon2id (preferred) or BCrypt (fallback). The hash algorithm is stored with the hash so migration is seamless.

---

## 6. Authorization & RBAC

### 6.1 Role-Permission Matrix

Permissions follow the pattern `RESOURCE_ACTION`. The table below shows key permission assignments per role.

| Permission | Administrator | Recruiter/Staffing Mgr | Dispatch Supervisor | Finance Clerk |
|---|---|---|---|---|
| USER_MANAGE | Yes | -- | -- | -- |
| ROLE_MANAGE | Yes | -- | -- | -- |
| CANDIDATE_VIEW | Yes | Yes | -- | -- |
| CANDIDATE_EDIT | Yes | Yes | -- | -- |
| JOB_PROFILE_MANAGE | Yes | Yes | -- | -- |
| TALENT_POOL_MANAGE | Yes | Yes | -- | -- |
| PIPELINE_MOVE | Yes | Yes | -- | -- |
| SEARCH_SNAPSHOT_VIEW | Yes | Yes | -- | -- |
| COLLECTOR_MANAGE | Yes | -- | Yes | -- |
| SITE_MANAGE | Yes | -- | Yes | -- |
| SHIFT_MANAGE | Yes | -- | Yes | -- |
| DISPATCH_MANAGE | Yes | -- | Yes | -- |
| PAYMENT_RECORD | Yes | -- | -- | Yes |
| PAYMENT_VIEW | Yes | -- | -- | Yes |
| REFUND_PROCESS | Yes | -- | -- | Yes |
| BANK_FILE_IMPORT | Yes | -- | -- | Yes |
| RECONCILIATION_MANAGE | Yes | -- | -- | Yes |
| SETTLEMENT_GENERATE | Yes | -- | -- | Yes |
| METRIC_VIEW | Yes | Yes | Yes | Yes |
| METRIC_PUBLISH | Yes | -- | -- | -- |
| METRIC_ROLLBACK | Yes | -- | -- | -- |
| EXPORT_OWN | Yes | Yes | Yes | Yes |
| EXPORT_ALL | Yes | -- | -- | -- |
| AUDIT_VIEW | Yes | -- | -- | -- |
| MASTER_DATA_MANAGE | Yes | -- | -- | -- |
| MEMBER_VIEW | Yes | Yes | Yes | Yes |
| MEMBER_EDIT | Yes | Yes | -- | -- |
| ENTERPRISE_VIEW | Yes | Yes | Yes | Yes |
| ENTERPRISE_EDIT | Yes | Yes | -- | -- |
| BIOMETRIC_MANAGE | Yes | -- | Yes | -- |

### 6.2 Enforcement Points

- **API layer:** A Spring Security filter checks the authenticated user's permissions against the required permission for each endpoint. Unauthorized requests receive HTTP 403.
- **Service layer:** Critical operations double-check permissions before executing business logic.
- **Data layer:** Query filters (e.g., `WHERE department_id IN (...)`) limit data access where row-level security is needed.

### 6.3 Field-Level Masking Rules

Field masking is applied at the API serialization layer (Jackson custom serializers). The UI receives already-masked data for roles that lack the unmasked-view permission.

| Field | Default Display | Full Access Roles |
|---|---|---|
| Phone numbers | `***-***-1234` (last 4 digits) | Administrator |
| Email addresses | `j***@***.com` (first char + domain hint) | Administrator |
| Card last 4 | `****1234` | Administrator, Finance Clerk |
| Check numbers | `****5678` | Administrator, Finance Clerk |
| Biometric template data | Never exposed in API | -- (only used server-side) |
| Password hashes | Never exposed in API | -- |

### 6.4 Role-Based Dashboards

Each role sees a tailored dashboard upon login:

- **Administrator:** System health, user activity summary, pending metric publications, exception queue count.
- **Recruiter/Staffing Manager:** Today's open requisitions, unassigned candidates, pipeline stage distribution, saved search quick links.
- **Dispatch Supervisor:** Unassigned jobs for today, shift fill rates, timed-out assignments needing redispatch, site capacity heatmap.
- **Finance Clerk:** Pending reconciliation items, exception queue count, today's payment totals by channel, upcoming settlement deadlines.

All dashboards include an **alerts panel** for exception conditions (e.g., dispatch timeouts, reconciliation discrepancies, import failures).

---

## 7. Core Modules

### 7.1 Recruiting Pipeline & Intelligent Matching

#### 7.1.1 Boolean Filter Engine

The search criteria are represented as a JSON filter tree supporting AND, OR, and NOT operators with arbitrary nesting.

**Example filter tree:**

```json
{
  "operator": "AND",
  "conditions": [
    {
      "field": "skills.skill_name",
      "operator": "EQUALS",
      "value": "Java",
      "sub_conditions": {
        "field": "skills.years_experience",
        "operator": "GTE",
        "value": 5
      }
    },
    {
      "operator": "OR",
      "conditions": [
        { "field": "skills.skill_name", "operator": "EQUALS", "value": "Spring Boot" },
        { "field": "skills.skill_name", "operator": "EQUALS", "value": "Spring Framework" }
      ]
    },
    {
      "field": "location",
      "operator": "IN",
      "value": ["New York", "New Jersey", "Remote"]
    }
  ]
}
```

The engine translates this tree into a parameterized SQL query at runtime. All values are bound as parameters (never interpolated) to prevent SQL injection.

#### 7.1.2 Weighted Scoring Algorithm

For each candidate matching the filter criteria, a weighted composite score is computed:

1. **Gather skill requirements** from the target `job_profile` via `skill_requirements`.
2. **For each requirement**, check the candidate's `candidate_skills`:
   - If a required skill is present and meets `min_years`: full weight awarded.
   - If a required skill is present but below `min_years`: proportional weight (`candidate_years / min_years * weight`).
   - If a preferred (non-required) skill is present: full weight awarded as a bonus.
   - If a required skill is missing: zero weight, and flagged in rationale.
3. **Composite score** = sum of awarded weights / sum of all possible weights, expressed as a percentage (0.00 to 100.00).
4. **Rank** candidates by composite score descending, then by total years of experience descending as tiebreaker.

#### 7.1.3 Explainable Rationale Generation

For each candidate in a search result, a `rationale` JSON object is generated and stored:

```json
{
  "matched_requirements": [
    {
      "skill": "Java",
      "required_years": 5,
      "candidate_years": 7,
      "is_required": true,
      "weight": 3.0,
      "score_awarded": 3.0,
      "explanation": "Java 5+ years met (candidate has 7 years)"
    },
    {
      "skill": "Spring Boot",
      "required_years": 2,
      "candidate_years": 3,
      "is_required": false,
      "weight": 1.5,
      "score_awarded": 1.5,
      "explanation": "Spring Boot preferred met (candidate has 3 years)"
    }
  ],
  "unmatched_requirements": [
    {
      "skill": "Kubernetes",
      "required_years": 1,
      "is_required": true,
      "weight": 2.0,
      "score_awarded": 0,
      "explanation": "Kubernetes required but not found in candidate profile"
    }
  ],
  "total_possible_score": 6.5,
  "total_awarded_score": 4.5,
  "overall_percentage": 69.23
}
```

#### 7.1.4 Search Snapshot Storage

When a search is executed:

1. The filter tree and weights are saved to `search_snapshots`.
2. The matching engine runs and produces scored/ranked results.
3. Each result (candidate ID, score, rank, and full rationale JSON) is stored in `search_results` linked to the snapshot.
4. If the user opts to save the search, `is_saved` is set to TRUE and a name is assigned.

Because rationale and scores are stored at search time, they remain stable even if candidate profiles or job requirements change later. This supports auditing and compliance reviews.

#### 7.1.5 Talent Pools

- Users with `TALENT_POOL_MANAGE` permission can create named talent pools.
- Candidates are added to pools individually or via bulk selection from search results.
- Bulk tagging: select multiple candidates, apply one or more tags. Tags are stored in `pool_members.tag`.
- Pools serve as saved groupings for recurring staffing needs (e.g., "Java Seniors - Northeast", "Contract DBAs").

#### 7.1.6 Bulk Operations & 60-Second Undo

**Batch pipeline moves:**

1. User selects multiple candidates (via checkboxes in the UI) and chooses a target pipeline stage.
2. A single confirmation dialog summarizes: "Move N candidates from [current stages] to [target stage]?"
3. On confirmation, the backend:
   a. Generates a UUID `batch_id`.
   b. Creates one `pipeline_moves` row per candidate, all sharing the same `batch_id`.
   c. Sets `undo_deadline = NOW + 60 seconds` on each row.
4. The API response includes the `batch_id` and `undo_deadline`.
5. The frontend displays an undo toast/banner with a countdown timer.
6. If the user clicks undo before the deadline:
   - `POST /api/pipeline/undo/{batchId}`
   - Backend verifies `NOW < undo_deadline`.
   - Sets `undone_at = NOW` on all moves in the batch.
   - Creates reverse `pipeline_moves` entries (moving candidates back to their previous stages).
7. After 60 seconds, the undo option disappears from the UI. The backend rejects late undo requests.

### 7.2 Dispatch Engine

#### 7.2.1 Shift Modeling

- Shifts are defined per site per date.
- Start and end times must align to 15-minute boundaries (`:00`, `:15`, `:30`, `:45`).
- Each shift specifies `required_collectors` (capacity need).
- The `assigned_count` is maintained as a denormalized counter, updated within the same transaction as assignment creation. Optimistic locking (via a version column or `SELECT ... FOR UPDATE`) prevents over-assignment under concurrent access.

#### 7.2.2 Capacity Enforcement

- Before creating a dispatch assignment, the system checks: `site.capacity >= total_assigned_across_all_shifts_for_site_and_date`.
- Per-shift: `shift.assigned_count < shift.required_collectors`.
- If either limit is exceeded, the assignment is rejected with a clear error.

#### 7.2.3 Grab-Order Flow

1. Dispatch Supervisor creates a `dispatch_job` with `mode = GRAB_ORDER` linked to a shift.
2. The system creates `dispatch_assignments` for all eligible collectors (based on availability, skills, location) with `status = OFFERED`.
3. Each assignment has `acceptance_deadline = NOW + 90 seconds`.
4. Collectors see offered jobs on their dashboard (via polling or page refresh).
5. A collector accepts: `POST /api/dispatch/assignments/{id}/accept`.
   - The system checks: deadline not passed, shift not full.
   - On success: assignment status becomes ACCEPTED, `shift.assigned_count` is incremented.
   - Other open offers for the same dispatch job may be cancelled if the shift is now full.
6. If a collector declines or does not respond within 90 seconds:
   - A scheduled task (running every 15 seconds) scans for expired offers.
   - Expired assignments are set to `TIMED_OUT`.
   - The system initiates auto-redispatch: creates new offers for the next batch of eligible collectors, incrementing `redispatch_count`.

#### 7.2.4 Assigned-Order Flow

1. Dispatch Supervisor creates a `dispatch_job` with `mode = ASSIGNED_ORDER`.
2. The system (or the supervisor manually) selects a specific collector and creates a single `dispatch_assignment` with `status = OFFERED`.
3. The 90-second acceptance timeout applies identically.
4. If the collector does not accept within 90 seconds, the system auto-redispatches to the next collector in priority order.
5. Priority order is configurable: by proximity to site, by collector rating, by round-robin fairness, or manually specified by the supervisor.

#### 7.2.5 Redispatch Logic

- Maximum redispatch attempts per dispatch job: configurable (default 3).
- After exhausting all eligible collectors, the dispatch job status becomes `TIMED_OUT` and an alert is raised on the Dispatch Supervisor's dashboard.
- All redispatch events are logged in `audit_log` for traceability.

### 7.3 Unified Search & Export

#### 7.3.1 Cross-Entity Search

The unified search endpoint (`GET /api/search`) accepts a combined filter object specifying:

- **Entity types** to include: `members`, `enterprises`, `resources`, `orders`, `redemption_records` (one or more).
- **Filters per entity type**: field-value conditions with operators (EQUALS, CONTAINS, GTE, LTE, IN, BETWEEN).
- **Global text search**: a free-text query matched against name/code/description fields across all selected entity types.
- **Sort** and **pagination** parameters.

The backend executes separate queries per entity type, merges results, and returns a unified response with entity type tags.

#### 7.3.2 Saved Search Criteria

Users can save search configurations (entity types, filters, sort order) for reuse. Saved criteria are stored in `search_snapshots` with `is_saved = TRUE` and a user-provided name. The unified search page includes a dropdown to load saved criteria.

#### 7.3.3 Queued Export Pipeline

1. User configures search/filter criteria and clicks "Export".
2. Frontend calls `POST /api/exports` with the criteria and desired format (CSV or Excel).
3. Backend creates an `export_jobs` row with `status = QUEUED` and an `async_jobs` row.
4. The async job processor:
   a. Executes the query with the saved criteria.
   b. Streams results into a CSV or Excel file on the local file system.
   c. Creates a `files` row with the file metadata.
   d. Updates `export_jobs` with `status = COMPLETED`, `file_id`, and `record_count`.
5. The user navigates to the Exports page (`/exports.html`) to see all their exports with status and download links.
6. Download: `GET /api/exports/{id}/download` streams the file with appropriate `Content-Disposition` header.

**Export controls:** Administrators can export any data. Other roles can only export data they have permission to view. The `filters_applied` JSON on each export job provides an audit trail of exactly what was exported.

### 7.4 Master Data Management

#### 7.4.1 Reference Table Design

Master data tables (`departments`, `courses`, `semesters`, `classes`) store organizational reference information. They share common patterns:

- Each has a unique natural key (`code`) in addition to the surrogate primary key.
- `is_active` flag supports soft-disable without deletion.
- Referential integrity is enforced at the database level via foreign keys.
- Uniqueness constraints on codes prevent duplicates.

#### 7.4.2 Uniqueness Enforcement

- **Database level:** `UNIQUE` constraints on `code` columns. Duplicate inserts fail with a constraint violation.
- **Application level:** Before insert, the service layer checks for existing records with the same code to provide a user-friendly error message (rather than exposing a raw SQL exception).
- **Hierarchical integrity:** `departments.parent_department_id` references `departments.id`, enforcing valid hierarchy. Application logic prevents circular references by checking ancestry before update.

#### 7.4.3 CRUD Operations

All master data management is restricted to users with `MASTER_DATA_MANAGE` permission (Administrators). Standard CRUD endpoints follow REST conventions:

- `GET /api/master/{entity}` -- List with pagination, sort, and filter.
- `GET /api/master/{entity}/{id}` -- Single record.
- `POST /api/master/{entity}` -- Create.
- `PUT /api/master/{entity}/{id}` -- Update.
- `DELETE /api/master/{entity}/{id}` -- Soft delete (sets `is_active = FALSE` or `deleted_at`).

---

## 8. Bulk Import/Export

### 8.1 Supported Formats

- **Excel** (`.xlsx`): Parsed using Apache POI. Expected to follow a defined template with header row matching expected column names.
- **CSV** (`.csv`): Parsed using OpenCSV. Comma-delimited, double-quote enclosed fields, UTF-8 encoding.

### 8.2 Import Flow

1. User uploads a file via `POST /api/imports` (multipart form data).
2. Backend validates:
   a. **File type**: Must be `.xlsx` or `.csv`.
   b. **File fingerprint**: SHA-256 hash is computed and checked against `files.sha256`. If a file with the same hash exists in `files` and has been associated with a successful import, the upload is rejected with: "This file has already been imported on [date]."
   c. **File size**: Must not exceed configured maximum (default 50 MB).
3. File is saved to the local file system and a `files` row is created.
4. An `async_jobs` row is created with `type = BATCH_IMPORT`.
5. The async job processor:
   a. Opens the file and validates the header row against the expected template.
   b. Iterates through each data row, performing:
      - **Format validation**: Data types, required fields, string lengths.
      - **Business rule validation**: Foreign key references exist, unique constraints satisfied, value ranges valid.
      - **Row-level error collection**: Each invalid row is recorded with the row number, column, and error description.
   c. Valid rows are inserted/updated in a batch transaction.
   d. A result summary is generated:
      - Total rows, successful rows, failed rows.
      - For each failed row: row number, column, error message.
   e. The result summary is stored in `async_jobs.result_summary` as JSON.
   f. If any rows failed, a detailed error report (CSV) is generated and linked as `async_jobs.result_file_id`.

### 8.3 Format Validation Details

| Check | Description |
|---|---|
| Header match | Column names in the file must exactly match the expected template. Extra or missing columns are flagged. |
| Data types | Numeric fields must be parseable as numbers. Dates must match `YYYY-MM-DD` format. |
| Required fields | Columns marked as required must not be empty. |
| String length | Values must not exceed the maximum length defined for the corresponding database column. |
| Enum values | Fields with restricted values (e.g., status, type) must match one of the allowed values. |
| Foreign keys | References to other entities (e.g., department_id) must correspond to existing, active records. |
| Uniqueness | Values for unique columns (e.g., codes) must not duplicate existing database records or other rows in the same file. |

### 8.4 File Fingerprint Deduplication

- SHA-256 is computed over the entire file content at upload time.
- The hash is stored in `files.sha256`.
- Before processing an import, the system queries: `SELECT id FROM files f JOIN bank_file_imports bfi ON bfi.file_id = f.id WHERE f.sha256 = ? AND bfi.status IN ('COMPLETED', 'COMPLETED_WITH_EXCEPTIONS')`.
- An analogous check is performed for general imports (candidate data, master data, etc.) using the `async_jobs` table.
- This prevents the same file from being processed twice, even if renamed.

### 8.5 Export Generation

- Exports follow the same async pipeline described in Section 7.3.3.
- Excel exports use Apache POI with a header row, auto-sized columns, and basic formatting.
- CSV exports use OpenCSV with proper quoting and UTF-8 BOM for Excel compatibility.

---

## 9. Python Face Recognition Integration

### 9.1 Service Architecture

The Python face recognition service is a standalone HTTP server running on the same network segment as the Spring Boot application. It exposes a REST API for face feature extraction and matching.

**Endpoints provided by the Python service:**

| Endpoint | Method | Description |
|---|---|---|
| `/api/health` | GET | Health check. Returns `{ "status": "ok" }`. |
| `/api/extract` | POST | Accepts a face image (JPEG/PNG), returns a feature vector (JSON array of floats). |
| `/api/match` | POST | Accepts two feature vectors, returns a similarity score (0.0 to 1.0) and match decision (boolean). |
| `/api/search` | POST | Accepts a feature vector and a list of candidate vectors, returns ranked matches above a threshold. |

### 9.2 Communication Protocol

- Spring Boot communicates with the Python service over HTTP on the local network (e.g., `http://10.0.1.50:5000`).
- The base URL is configurable via `application.properties`: `face-recognition.service.url=http://localhost:5000`.
- Requests use `RestTemplate` or `WebClient` with a configurable timeout (default: 30 seconds for extraction, 10 seconds for matching).
- A shared API key (configured in both services) is passed as an `X-API-Key` header for basic authentication between services.

### 9.3 Integration Flow

#### 9.3.1 Template Registration

1. User uploads a face image via the Spring Boot API.
2. Spring Boot saves the image to the local file system and creates a `files` row.
3. Spring Boot sends the image to the Python service's `/api/extract` endpoint.
4. The Python service returns a feature vector (typically 128-dimensional float array).
5. Spring Boot encrypts the feature vector with AES-256 using the local encryption key.
6. The encrypted vector is stored in `biometric_templates.template_data`.

#### 9.3.2 Identity Verification (1:1 Match)

1. A new face image is captured/uploaded.
2. Spring Boot sends it to `/api/extract` to get a probe vector.
3. Spring Boot decrypts the stored template for the claimed identity.
4. Spring Boot sends both vectors to the Python service's `/api/match` endpoint.
5. The service returns a similarity score and match decision.
6. The result is logged in the audit trail.

#### 9.3.3 Identity Search (1:N Match)

1. A probe image is extracted as above.
2. Spring Boot decrypts all relevant templates (scoped by `person_type` or other criteria).
3. Spring Boot sends the probe vector and the list of candidate vectors to `/api/search`.
4. The service returns ranked matches above the configured threshold (default: 0.6).
5. Results are returned to the caller or stored in an async job result.

### 9.4 Error Handling

| Scenario | Handling |
|---|---|
| Python service unreachable | Spring Boot returns HTTP 503 with message: "Face recognition service is currently unavailable. Please try again later." The async job (if any) is set to FAILED with a retry scheduled. |
| Request timeout | Treated as unreachable. Logged with timeout duration. |
| Invalid image (no face detected) | Python service returns HTTP 400. Spring Boot relays: "No face detected in the uploaded image." |
| Multiple faces detected | Python service returns HTTP 400 with count. Spring Boot relays: "Multiple faces detected. Please upload an image with a single face." |
| Service returns error 500 | Logged as a system error. Async job marked FAILED. Alert raised on Administrator dashboard. |

### 9.5 Health Monitoring

- Spring Boot calls `GET /api/health` on the Python service every 60 seconds (configurable).
- If the health check fails 3 consecutive times, the system sets an internal flag `faceRecognitionAvailable = false`.
- While unavailable, face recognition features in the UI show a warning banner: "Face recognition service is offline."
- When the health check succeeds again, the flag is cleared automatically.

---

## 10. Async Job Queue

### 10.1 Architecture

The async job queue is implemented using Spring's `@Async` with a `ThreadPoolTaskExecutor`, backed by the `async_jobs` database table for persistence and status tracking.

This is an internal, application-managed queue -- no external message broker is required, consistent with the offline-first design.

### 10.2 Job Types

| Type | Description | Typical Duration |
|---|---|---|
| FACE_RECOGNITION | Feature extraction or matching via Python service | 5-30 seconds |
| BATCH_IMPORT | Excel/CSV file parsing and record insertion | 10 seconds - 5 minutes |
| REPORT_GENERATION | Complex report compilation | 30 seconds - 10 minutes |
| EXPORT | Search result or data export to file | 5 seconds - 5 minutes |
| RECONCILIATION | Bank file matching against payments | 10 seconds - 2 minutes |

### 10.3 Status Lifecycle

```
QUEUED -> RUNNING -> COMPLETED
                  -> FAILED -> (retry) -> QUEUED
                            -> (max retries) -> FAILED (terminal)
QUEUED -> CANCELLED (user-initiated)
```

- **QUEUED**: Job created, waiting for a thread.
- **RUNNING**: Thread picked up the job, `started_at` set, `progress_percent` updated periodically.
- **COMPLETED**: Job finished successfully. `completed_at` set, `result_summary` and optionally `result_file_id` populated.
- **FAILED**: Job encountered an error. `error_message` populated. If `retry_count < max_retries`, the job is re-queued after a backoff delay (`retry_count * 30 seconds`).
- **CANCELLED**: User or administrator cancelled the job before completion.

### 10.4 Thread Pool Configuration

```properties
async.pool.core-size=4
async.pool.max-size=8
async.pool.queue-capacity=100
async.pool.thread-name-prefix=async-job-
```

These values are configurable via `application.properties` and tuned based on the server's CPU and memory.

### 10.5 Health Checks & Thresholds

| Metric | Threshold | Action |
|---|---|---|
| Queue depth (QUEUED jobs) | > 50 | Log warning, alert on Administrator dashboard |
| Average wait time (QUEUED to RUNNING) | > 5 minutes | Log warning |
| Failed job rate (last hour) | > 20% | Alert on Administrator dashboard |
| Thread pool saturation | max-size reached | Log warning, consider increasing pool size |
| Stuck jobs (RUNNING for > 30 minutes) | Any | Mark as FAILED, log error, alert |

A scheduled task runs every 60 seconds to compute these metrics and update an in-memory health status object exposed via `GET /api/admin/jobs/health`.

---

## 11. Metrics Semantic Layer

### 11.1 Purpose

The Metrics Semantic Layer provides a governed, versioned catalog of business metrics used by dashboards and reports. It ensures that a metric like "Fill Rate" means the same thing across all charts, releases, and user roles.

### 11.2 Definition Schema

A metric definition consists of:

- **Code**: Stable identifier (e.g., `fill_rate`, `time_to_hire`, `dispatch_acceptance_rate`).
- **Name**: Human-readable label.
- **Data type**: INTEGER, DECIMAL, PERCENTAGE, RATIO.
- **Aggregation type**: How the metric rolls up (SUM, AVG, COUNT, MIN, MAX, CUSTOM).
- **Unit**: Display unit (USD, hours, count, percentage).

Each metric has one or more **versions** (in `metric_versions`), each containing the actual calculation logic (`definition_sql` or parameter-driven formula).

### 11.3 Versioning Workflow

1. **Draft**: A new version is created with `is_published = FALSE`. Anyone with appropriate access can draft.
2. **Review**: The draft is reviewed by an Administrator.
3. **Publish**: Only an Administrator can set `is_published = TRUE` and `is_active = TRUE`. This deactivates the previously active version for the same metric.
4. **Active**: Dashboards and reports reference the active version of each metric.

### 11.4 Rollback Mechanism

1. Administrator navigates to the metric's version history.
2. Selects a previously published version.
3. Confirms rollback.
4. The system sets `is_active = FALSE` on the current version and `is_active = TRUE` on the selected version.
5. All dashboards referencing this metric automatically use the rolled-back version on next load.
6. The rollback action is logged in `audit_log`.

### 11.5 Lineage DAG

The `metric_lineage` table records directed edges in a dependency graph:

- **Upstream metric** feeds into a **downstream derived metric**.
- The `relationship_type` (INPUT, FILTER, DIMENSION) describes how the upstream metric is used.
- The lineage graph is used for:
  - **Impact analysis**: Before modifying a metric, show all downstream metrics that would be affected.
  - **Root cause analysis**: When a derived metric shows unexpected values, trace upstream to the source metrics.
  - **Visualization**: The UI renders the DAG as a tree or graph for Administrators.

### 11.6 Derived Metrics & Window Calculations

Derived metrics reference one or more base metrics via `input_metric_ids` and define a `formula` that combines them.

Supported window functions:

| Window | Description |
|---|---|
| ROLLING_AVG_7D | 7-day rolling average |
| ROLLING_AVG_30D | 30-day rolling average |
| CUMULATIVE | Running total from period start |
| YTD | Year-to-date accumulation |
| MOM_CHANGE | Month-over-month change (absolute or percentage) |
| QOQ_CHANGE | Quarter-over-quarter change |

Window parameters (stored in `window_params`) include window size, offset, partition keys (e.g., by site, by department), and date grain (daily, weekly, monthly).

### 11.7 Chart Consistency

Dashboards reference metrics by `code` and always load the `is_active = TRUE` version. Because the definition is centralized:

- Changing a metric formula in one place updates all charts.
- Version history ensures traceability.
- Rollback restores prior behavior instantly.
- New releases of the application do not break existing charts as long as metric codes are stable.

---

## 12. Payments, Settlement & Reconciliation

### 12.1 Design Principles

- **Entirely offline**: No network-based payment processing. All transactions are recorded manually.
- **Idempotent imports**: Bank files are identified by SHA-256 hash; re-importing the same file is safely rejected.
- **Auditability**: Every payment, refund, and reconciliation action is logged with user ID, timestamp, and workstation ID.

### 12.2 Payment Recording

#### 12.2.1 Allowed Channels

| Channel | Description | Required Detail |
|---|---|---|
| CASH | Physical currency | None |
| CHECK | Paper check | Check number (stored masked: only last 4 digits visible in UI) |
| MANUAL_CARD | Card number entered manually, no network processing | Last 4 digits of card number (full number never stored) |

No online payment gateway, no card network authorization, no e-wallet integration. The system is a ledger for manually collected payments.

#### 12.2.2 Recording Flow

1. Finance Clerk navigates to the payment recording form.
2. Selects or searches for the associated order/member.
3. Enters amount, selects channel, enters channel-specific detail.
4. Submits: `POST /api/payments`.
5. Backend validates:
   - Amount is positive.
   - Channel is one of the allowed values.
   - Required detail is provided for CHECK and MANUAL_CARD.
   - Associated order exists (if specified).
6. A `payments` row is created with `status = RECORDED`.
7. Audit log entry created.
8. System generates a unique `payment_reference` (format: `PAY-YYYYMMDD-NNNNNN`).

### 12.3 Refund Flow

#### 12.3.1 Full Refund

1. Finance Clerk locates the original payment.
2. Initiates refund: `POST /api/refunds` with `{ paymentId, amount: <full amount>, refundType: "FULL", reason: "..." }`.
3. Backend validates: no prior refunds for this payment.
4. Creates `refunds` row. Updates `payments.status` to `REFUNDED_FULL`.

#### 12.3.2 Partial Refund

1. Same flow, but `refundType: "PARTIAL"` and `amount` is less than the original.
2. Backend validates: sum of all refunds for this payment (existing + new) does not exceed the original payment amount.
3. Creates `refunds` row. Updates `payments.status` to `REFUNDED_PARTIAL`.
4. Multiple partial refunds are allowed as long as the total does not exceed the original amount.

### 12.4 Bank File Import & Reconciliation

#### 12.4.1 Import Flow

1. Finance Clerk uploads a bank statement file (CSV or Excel).
2. SHA-256 computed. If already imported, reject with message: "File already imported on [date], import ID [id]."
3. File saved, `bank_file_imports` row created with `status = PENDING`.
4. Async job created for reconciliation processing.

#### 12.4.2 Matching Logic

The reconciliation processor iterates through each row in the bank file:

1. **Extract** bank reference, amount, and date from the row.
2. **Attempt match** against `payments` using:
   - Primary: exact match on payment reference.
   - Secondary: match on amount + date range (within +/- 3 business days).
3. **Create** `reconciliation_records` row:
   - **MATCHED**: Bank reference maps to a payment with exact amount match.
   - **DISCREPANT**: Bank reference maps to a payment, but amounts differ. `discrepancy_amount` recorded.
   - **UNMATCHED**: No matching payment found.
4. For UNMATCHED and DISCREPANT records, an `exception_queue` entry is automatically created.
5. Update `bank_file_imports` counters (`matched_count`, `unmatched_count`).

#### 12.4.3 Exception Queue Management

- Finance Clerks see the exception queue on their dashboard, sorted by priority.
- Each exception can be:
  - **Resolved**: Manually matched to a payment, discrepancy explained. `resolution_notes` recorded.
  - **Written off**: Marked as irrecoverable with justification.
- All exception resolutions are logged in the audit trail.

#### 12.4.4 Idempotent Callback Handling

If the system receives or imports bank data that references a previously reconciled transaction:

1. The reconciliation record already exists.
2. The system detects the duplicate bank reference and skips it.
3. A note is logged: "Duplicate bank reference [ref] skipped during import [id]."

This ensures that re-processing a bank file (even with a different filename but same content) or processing overlapping files does not create duplicate reconciliation records.

### 12.5 Monthly Settlement Statements

1. Finance Clerk (or a scheduled task, if configured) initiates settlement generation for a given month.
2. `POST /api/settlements/generate` with `{ year, month }`.
3. Backend aggregates:
   - All payments recorded in the period.
   - All refunds processed in the period.
   - Net amount (payments minus refunds).
   - Record count.
4. Generates a CSV file with line items and summary.
5. Creates `settlement_statements` row linked to the CSV file.
6. The CSV is available for download on the Exports page.

**CSV format:**

```
Reference,Date,Channel,Amount,Type,Notes
PAY-20260301-000001,2026-03-01,CASH,150.00,PAYMENT,
PAY-20260302-000042,2026-03-02,CHECK,500.00,PAYMENT,Check #...5678
REF-20260315-000003,2026-03-15,CASH,-50.00,REFUND,Partial refund for PAY-...
...
,,,,TOTAL PAYMENTS,12500.00
,,,,TOTAL REFUNDS,-350.00
,,,,NET AMOUNT,12150.00
```

---

## 13. File Storage

### 13.1 Storage Strategy

All files are stored on the local file system of the Spring Boot server. The database stores metadata pointers, not file contents (except for encrypted biometric templates, which are small enough to store inline).

### 13.2 Directory Structure

```
/data/workforce-hub/
  uploads/
    imports/          # Uploaded Excel/CSV files for bulk import
    images/           # Face images and other uploaded images
    bank-files/       # Bank statement uploads
  exports/
    search-results/   # Exported search results
    settlements/      # Monthly settlement CSVs
    reports/          # Generated reports
  temp/               # Temporary processing files (cleaned up by scheduled task)
```

### 13.3 File Naming

Stored files are renamed to avoid collisions and prevent path traversal attacks:

- Format: `{UUID}.{original_extension}`
- Example: `a3b2c1d4-e5f6-7890-abcd-ef1234567890.xlsx`

The original filename is preserved in `files.original_name` for display purposes.

### 13.4 SHA-256 Fingerprints

- Computed at upload time using Java's `MessageDigest`.
- Stored in `files.sha256` (lowercase hex, 64 characters).
- Used for:
  - **Import deduplication**: Preventing re-import of the same bank file or data file.
  - **Integrity verification**: Confirming that a downloaded file matches the stored hash.

### 13.5 Cleanup

A scheduled task runs daily to:

- Remove files in `temp/` older than 24 hours.
- Identify `files` rows where `is_deleted = TRUE` and `deleted_at` is older than 30 days, then remove the physical file.

---

## 14. Security

### 14.1 Field-Level Masking

Field masking is implemented as a serialization-layer concern using custom Jackson serializers. The serializer inspects the current user's roles and applies masking rules before JSON output.

**Implementation approach:**

- A custom annotation `@Masked(roles = {"ADMINISTRATOR"}, strategy = MaskStrategy.LAST_4)` is applied to entity fields.
- A Jackson `BeanSerializerModifier` intercepts serialization, checks the current SecurityContext for the user's roles, and applies the masking strategy if the user lacks the required role.
- Masking strategies: `LAST_4` (show last 4 characters), `FIRST_CHAR` (show first character), `FULL_MASK` (replace entirely with asterisks), `HIDDEN` (omit field from response).

### 14.2 Biometric Data Encryption

- **Algorithm**: AES-256-GCM (authenticated encryption).
- **Key storage**: Local Java KeyStore (JCEKS or PKCS12) on the server's file system, protected by a keystore password.
- **Key rotation**: New keys can be added to the keystore with a new `key_id`. A background job re-encrypts existing templates with the new key. The `encryption_key_id` column tracks which key was used.
- **Access control**: Only the Spring Boot application process has read access to the keystore file.

### 14.3 SQL Injection Prevention

- All database queries use parameterized statements (JPA named parameters or JDBC `PreparedStatement`).
- The Boolean filter engine (Section 7.1.1) builds queries programmatically using JPA Criteria API or a query builder -- never string concatenation.
- Input validation rejects values containing SQL metacharacters where not expected.

### 14.4 Rate Limiting

- **Rate**: 30 requests per minute per authenticated user.
- **Implementation**: A Spring `HandlerInterceptor` or servlet filter using an in-memory sliding window counter keyed by `user_id`.
- **Storage**: `ConcurrentHashMap<Long, SlidingWindowCounter>` in memory. Counters are evicted after 5 minutes of inactivity to prevent memory leaks.
- **Response on limit exceeded**: HTTP 429 Too Many Requests with `Retry-After` header indicating seconds until the window resets.
- **Exemptions**: Health check endpoints (`/api/health`) are exempt. Authentication endpoints have a separate, stricter limit (10 requests per minute per IP) to mitigate brute force.

### 14.5 Additional Security Measures

| Measure | Description |
|---|---|
| HTTPS | Self-signed or internal CA certificate for TLS. Configured in `application.properties` with the keystore path and password. |
| CORS | Disabled or restricted to the same origin, since the frontend is served by the same server. |
| CSRF | CSRF token required for all state-changing requests. The token is embedded in the HTML page and included in `fetch` requests via a custom header. |
| Content Security Policy | `Content-Security-Policy` header restricts script sources to `'self'` only. No inline scripts, no external CDNs. |
| X-Content-Type-Options | `nosniff` -- prevents MIME type sniffing. |
| X-Frame-Options | `DENY` -- prevents clickjacking. |
| Cookie flags | `HttpOnly`, `Secure` (if HTTPS), `SameSite=Strict` on session cookies. |
| Input validation | All API inputs validated for type, length, and format before processing. Spring's `@Valid` with custom validators. |

---

## 15. Audit Trail

### 15.1 Design Principles

- **Immutable**: Audit log records are never updated or deleted. Enforced by removing UPDATE and DELETE privileges on the `audit_log` table for the application's database user, and adding a `BEFORE UPDATE` / `BEFORE DELETE` trigger that raises an error.
- **Comprehensive**: Every state-changing action (create, update, delete, login, logout, export, search, import) generates an audit entry.
- **Contextual**: Each entry includes `user_id`, `action`, `resource_type`, `resource_id`, `detail` (JSON), `workstation_id`, and `timestamp`.

### 15.2 What Gets Logged

| Action Category | Examples |
|---|---|
| Authentication | Login success, login failure, logout, session expiry |
| User management | User created, role assigned, role revoked, password changed |
| Candidate operations | Profile created, profile updated, pipeline move, talent pool add/remove |
| Dispatch operations | Job created, assignment offered, accepted, timed out, redispatched |
| Payment operations | Payment recorded, refund processed, bank file imported, exception resolved |
| Search operations | Search executed (criteria logged), search saved, export initiated |
| Metric operations | Version created, published, rolled back |
| Master data | Record created, updated, deactivated |
| System operations | Async job started, completed, failed; file uploaded, deleted |

### 15.3 Detail Field

The `detail` JSON column captures contextual information:

- **For updates**: `{ "before": { "status": "OPEN" }, "after": { "status": "FILLED" } }` -- capturing the changed fields and their old/new values.
- **For searches**: `{ "criteria": { ... }, "result_count": 42, "snapshot_id": 123 }`.
- **For logins**: `{ "ip": "10.0.1.25", "user_agent": "Mozilla/5.0 ...", "success": true }`.
- **For batch operations**: `{ "batch_id": "uuid", "affected_count": 15, "action": "pipeline_move", "to_stage": "INTERVIEWED" }`.

### 15.4 Querying the Audit Log

Administrators can query the audit log via `GET /api/admin/audit` with filters:

| Filter | Description |
|---|---|
| `userId` | Actions by a specific user |
| `action` | Specific action type |
| `resourceType` | Actions on a specific entity type |
| `resourceId` | Actions on a specific record |
| `workstationId` | Actions from a specific machine |
| `fromTimestamp` / `toTimestamp` | Time range |

Results are paginated and sortable by timestamp (default: descending). Export to CSV is supported via the standard export pipeline.

### 15.5 Retention

Audit log data is retained indefinitely by default. If storage becomes a concern, an Administrator can configure archival: records older than a configurable threshold (e.g., 2 years) are exported to a CSV file and the rows are moved to an `audit_log_archive` table (which may reside on a separate disk or be compressed).

---

## 16. Deployment

### 16.1 Deployment Model

The entire system runs on-premise on a single server or a small number of servers within one network segment. No cloud services, no internet access required.

### 16.2 Components & Packaging

| Component | Packaging | Port | Notes |
|---|---|---|---|
| Spring Boot Application | Executable JAR (embedded Tomcat) or WAR (external Tomcat) | 8080 (HTTP) / 8443 (HTTPS) | Includes static frontend files in `/static` |
| MySQL Database | Standard MySQL 8.x installation | 3306 | Accepts connections from Spring Boot server only |
| Python Face Recognition Service | Python application with `requirements.txt` | 5000 | Runs as a system service or via process manager |
| Local File System | Server disk | N/A | Data directory at `/data/workforce-hub/` |

### 16.3 Configuration

All configuration is externalized via `application.properties` (or `application.yml`) and environment variables. Key configuration parameters:

```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/workforce_hub
spring.datasource.username=app_user
spring.datasource.password=${DB_PASSWORD}

# Session
session.idle-timeout-minutes=30
session.absolute-timeout-hours=8

# Face Recognition
face-recognition.service.url=http://localhost:5000
face-recognition.service.api-key=${FACE_RECOG_API_KEY}
face-recognition.health-check-interval-seconds=60

# Rate Limiting
rate-limit.requests-per-minute=30
rate-limit.auth-requests-per-minute=10

# Async Jobs
async.pool.core-size=4
async.pool.max-size=8
async.pool.queue-capacity=100

# File Storage
file.storage.base-path=/data/workforce-hub
file.storage.max-upload-size-mb=50

# Encryption
biometric.keystore.path=/etc/workforce-hub/keystore.p12
biometric.keystore.password=${KEYSTORE_PASSWORD}
biometric.encryption.key-alias=biometric-key-v1
```

### 16.4 Database Initialization

- Flyway (or Liquibase) manages schema migrations.
- Migration scripts are packaged inside the JAR under `db/migration/`.
- On first startup, all migration scripts run in order, creating the full schema.
- On subsequent startups, only new migrations are applied.
- Seed data (default roles, permissions, role-permission mappings, pipeline stages, payment channels) is included in the initial migration scripts.

### 16.5 Startup Sequence

1. MySQL is started and accessible on port 3306.
2. Python Face Recognition Service is started (optional -- the system operates without it, with face recognition features disabled).
3. Spring Boot application is started:
   a. Flyway runs pending migrations.
   b. Connection pool is established to MySQL.
   c. Async job thread pool is initialized.
   d. Rate limiter is initialized.
   e. Health check scheduler pings the Python service.
   f. Application is ready to accept HTTP requests.
4. Users access the application via `http://<server-ip>:8080` in their browser.

### 16.6 Offline-First Guarantees

| Concern | Approach |
|---|---|
| No internet dependency | All libraries are bundled in the JAR/WAR. No CDN references in HTML/CSS/JS. All fonts and icons are local files. |
| No external DNS | Configuration uses IP addresses or local hostnames resolvable via `/etc/hosts` or local DNS. |
| No cloud database | MySQL runs locally. |
| No external auth provider | Authentication is fully local (username/password in MySQL). |
| No external queue/broker | Async job queue is database-backed and managed by the Spring Boot application internally. |
| No telemetry or analytics | No outbound data transmission. All metrics are computed and stored locally. |

### 16.7 Backup & Recovery

- **Database**: MySQL `mysqldump` or binary log replication to a secondary server on the same network.
- **Files**: Rsync or scheduled copy of `/data/workforce-hub/` to a backup location.
- **Configuration**: Version-controlled or backed up alongside the application JAR.
- **Recovery**: Restore MySQL from dump, restore file directory, start the application. Flyway ensures schema consistency.

### 16.8 Monitoring

Since external monitoring services are unavailable in an offline environment:

- Spring Boot Actuator endpoints (`/actuator/health`, `/actuator/metrics`) are enabled but restricted to the Administrator role.
- A simple monitoring page in the admin UI displays: database connectivity, Python service status, async job queue depth, disk space available, and last backup timestamp (if configured).
- Log files are written to `/var/log/workforce-hub/` with daily rotation and 90-day retention.
