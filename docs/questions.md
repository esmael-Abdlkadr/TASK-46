# Workforce & Talent Operations Hub -- Open Questions

> Offline-first on-premise web portal.
> Stack: Spring Boot (Java) backend, plain HTML/CSS/JS frontend, MySQL database, Python-based local face recognition service.
> Roles: Administrator, Recruiter/Staffing Manager, Dispatch Supervisor, Finance Clerk.

---

## 1. Undo Window for Batch Pipeline Moves -- Downstream Side-Effects

**Question:** When a recruiter executes a batch pipeline move and the 60-second undo window starts, what happens if downstream actions (email/SMS notifications, status-change webhooks, calendar holds, audit log entries) have already fired before the undo is triggered?

**My Understanding:** The 60-second undo is meant to let the user reverse an accidental bulk action quickly. However, if notifications or status changes are dispatched immediately upon the move, undoing the move does not unsend those notifications or retract external side-effects, leaving candidates and hiring managers with stale or contradictory information.

**Solution:** Treat the 60-second window as a soft-commit phase. During this phase the pipeline move is persisted with a `PENDING_CONFIRMATION` status. All downstream side-effects (notifications, status propagation, audit finalization) are enqueued but held in a deferred outbox and only released once the window expires without an undo. If the user triggers undo within 60 seconds, the deferred outbox entries are discarded, the move is rolled back, and a single audit record is written noting the reversal. The UI will display a countdown banner with an "Undo" button; once the timer expires the banner disappears and side-effects fire. This keeps the system eventually consistent without leaking premature notifications.

---

## 2. Explainable Rationale -- Configuration Scope

**Question:** Is the rationale template that produces human-readable match explanations (e.g., "Java 5+ years met; Spring Boot preferred met") configurable per individual job profile, or is it a single system-wide template?

**My Understanding:** Different job families (engineering, logistics, finance) may weight and phrase criteria differently. A single global template may produce generic explanations that lack relevance for specialized roles, while per-profile templates add maintenance overhead.

**Solution:** Implement a two-tier configuration model. A system-wide default rationale template is maintained by Administrators under Settings > Matching > Rationale Templates. Recruiters or Staffing Managers can then override this template at the job-profile level by attaching a profile-specific rationale template when creating or editing a requisition. The profile-level template inherits all tokens and syntax from the global template but allows reordering, rewording, or adding profile-specific criteria labels. If no profile-level override exists, the system-wide default applies. Template changes are versioned so that previously generated rationale strings stored in search snapshots remain unchanged and reproducible.

---

## 3. Search Snapshot Retention Policy

**Question:** How long are search snapshots (including the frozen matching/similarity scores and the "why matched" rationale) retained in the system? Are they kept indefinitely, or is there a time-to-live (TTL) policy with automatic purging?

**My Understanding:** Snapshots serve an audit and compliance purpose (proving why a candidate was or was not advanced), but indefinite retention will grow the MySQL snapshot tables without bound, eventually degrading query performance and consuming disk on the on-premise server.

**Solution:** Introduce a configurable retention policy managed by Administrators under Settings > Data Retention > Search Snapshots. The default TTL will be 365 days from snapshot creation. Snapshots linked to active requisitions or open disputes are exempt from purging until the requisition is closed or the dispute is resolved. A nightly async job (part of the existing job queue) will soft-delete expired snapshots, moving them to an archive table. A secondary purge job hard-deletes archived snapshots after an additional 90-day grace period. Administrators can adjust the TTL value, the grace period, and can manually pin individual snapshots to prevent expiration. Storage metrics (total snapshots, disk usage, upcoming expirations) will be surfaced on the Admin dashboard.

---

## 4. Weighted Ranking -- Authority to Configure Weights

**Question:** Who is authorized to configure the weights used in candidate ranking -- does each Recruiter set weights per individual search, or does an Administrator define them globally for all searches?

**My Understanding:** Giving every Recruiter full control over weights per search provides flexibility but risks inconsistent scoring across the organization. Centralizing weights under Admin control ensures consistency but may be too rigid for diverse job families.

**Solution:** Use a layered approach. Administrators define a set of global default weight profiles (e.g., "Engineering Standard", "Operations Standard") that are published organization-wide. Recruiters can select any published weight profile when creating a search. Additionally, Recruiters may create ad-hoc weight overrides for a specific search, but these overrides are flagged in the audit log and in the search snapshot as "custom weights -- deviates from [profile name]". Administrators can optionally enforce a policy that restricts Recruiters to published profiles only (toggled via a system setting). All weight profiles are versioned; the version used is recorded in the search snapshot for reproducibility.

---

## 5. Talent Pool Membership -- Multi-Pool and Pipeline Interaction

**Question:** Can a single candidate belong to multiple talent pools simultaneously? When a candidate is moved to a new pipeline stage within a requisition, what happens to their talent pool memberships?

**My Understanding:** Talent pools are meant to be reusable collections (e.g., "Senior Java Developers", "Chicago-area Logistics"), so multi-membership seems natural. However, there could be confusion if moving a candidate into an active pipeline stage automatically removes them from pools, especially if other Recruiters are relying on those pools.

**Solution:** A candidate can belong to any number of talent pools simultaneously. Talent pool membership and requisition pipeline stage are orthogonal concepts tracked independently. Moving a candidate to a new pipeline stage within a requisition does not alter their pool memberships. However, to keep pools meaningful, the system will provide a visual indicator on pool views showing each candidate's current pipeline status across all requisitions (e.g., a badge reading "Active in 2 pipelines"). Recruiters can manually remove a candidate from a pool at any time, and Administrators can configure an optional automation rule that removes candidates from specified pools when they reach a terminal pipeline stage (e.g., "Hired" or "Rejected") -- this rule is off by default and must be explicitly enabled per pool.

---

## 6. Dispatch Auto-Redispatch -- Maximum Cycle Limit

**Question:** When a dispatched job times out after the 90-second acceptance window and is auto-redispatched, how many redispatch cycles occur before the system flags the job as unassignable?

**My Understanding:** Without a cap, the system could cycle indefinitely through available collectors, creating noise and delaying escalation. With too low a cap, legitimate jobs may be flagged prematurely during busy periods.

**Solution:** The default maximum redispatch cycle count will be 3 attempts (configurable by Administrators under Settings > Dispatch > Redispatch Policy). After each failed 90-second acceptance timeout, the system selects the next eligible collector based on the current dispatch mode (grab-order or assigned-order) and increments the attempt counter. Once the maximum is reached without acceptance, the job transitions to an `UNASSIGNABLE` status and appears on the Dispatch Supervisor's exception dashboard with a visual alert. The Supervisor can then manually assign the job, expand the eligible collector pool, or cancel the dispatch. The attempt count and timestamps for each cycle are recorded in the dispatch audit trail. Administrators can also configure a parallel time-based ceiling (e.g., 15 minutes total elapsed) that triggers unassignable status even if the cycle count has not been reached.

---

## 7. Grab-Order vs Assigned-Order -- Mid-Shift Mode Switching

**Question:** Can a site switch between grab-order mode (collectors self-select) and assigned-order mode (system assigns) in the middle of an active shift, or is the mode locked at shift boundaries?

**My Understanding:** Operational needs can change mid-shift (e.g., a sudden staffing shortage may require switching to assigned-order for tighter control). However, switching mid-shift could confuse collectors who started under one mode and now see different behavior.

**Solution:** Mode switching is permitted mid-shift but restricted to the Dispatch Supervisor role and subject to safeguards. When a Supervisor initiates a mode switch, all currently pending (unaccepted) dispatches for that site are recalled and re-queued under the new mode. Jobs already accepted and in-progress are unaffected and continue under the mode in which they were accepted. The system displays a confirmation dialog summarizing the impact (number of pending dispatches to be re-queued, number of in-progress jobs unaffected). A push notification is sent to all active collectors at the site informing them of the mode change. The switch event is logged with timestamp, Supervisor ID, and workstation ID. Administrators can optionally disable mid-shift switching via a site-level policy flag if operational stability is preferred.

---

## 8. 15-Minute Shift Increments -- Overtime and Split Shifts

**Question:** Given that shifts are structured in 15-minute increments, how does the system handle overtime calculations and split shifts (e.g., a collector works 7:00-11:00, breaks, then returns 13:00-17:00)?

**My Understanding:** 15-minute granularity is fine for scheduling, but overtime rules (daily thresholds, weekly thresholds, consecutive-hour rules) and split-shift regulations vary by jurisdiction and may not align neatly with 15-minute blocks. The system needs a clear model for aggregating increments into payable hours.

**Solution:** Each collector's shift is recorded as a series of 15-minute time blocks with start/end timestamps. The system aggregates contiguous blocks into shift segments and calculates total daily and weekly hours. Overtime is computed using configurable rule sets managed by Administrators under Settings > Dispatch > Overtime Rules, supporting daily thresholds (e.g., over 8 hours), weekly thresholds (e.g., over 40 hours), and consecutive-hour thresholds. Split shifts are modeled as multiple segments within a single calendar day; a minimum gap duration (default 60 minutes, configurable) distinguishes a break from a shift split. For payroll export, each segment is tagged with its pay classification (regular, overtime, split-shift premium) based on the active rule set. The Finance Clerk can review and override classifications before the monthly settlement CSV is generated.

---

## 9. Capacity Limits Per Site -- Behavior When Capacity Is Reached

**Question:** When a site reaches its configured capacity limit for a time slot, does the system block new dispatch attempts entirely, or does it queue them for automatic dispatch once capacity frees up?

**My Understanding:** Hard-blocking prevents overloading a site but may cause dispatchers to lose track of pending demand. Queuing preserves demand visibility but risks over-promising if capacity does not free up soon.

**Solution:** Implement a hybrid approach. When capacity is reached, new dispatches for that site and time slot enter a `QUEUED_WAITING` state rather than being outright rejected. The Dispatch Supervisor dashboard shows the current queue depth alongside capacity utilization for each site. As accepted jobs are completed or cancelled and capacity opens, the oldest queued dispatch is automatically promoted and dispatched to the next eligible collector. The Supervisor can also manually promote or cancel queued items. A configurable queue ceiling (default: 50% of site capacity, adjustable by Administrators) prevents unbounded queue growth; once the queue ceiling is reached, further dispatch attempts for that slot are blocked with a clear message. Queued items that are not dispatched within a configurable timeout (default: 60 minutes) are auto-expired and moved to the exception dashboard.

---

## 10. File Fingerprint for Re-Import Prevention -- Fingerprint Scope

**Question:** Is the file fingerprint used to prevent duplicate imports based on the filename alone, the file content hash alone, or a combination of both?

**My Understanding:** Filename-based fingerprinting is trivially bypassed by renaming files. Content-hash-based fingerprinting correctly identifies duplicate data regardless of filename but may block a legitimate re-import if a previous import of the same file partially failed and needs to be retried.

**Solution:** The fingerprint will be based on a SHA-256 hash of the file content (byte-level), independent of the filename. This ensures that renaming a file does not bypass duplicate detection. To handle the partial-failure retry scenario, each import is tracked with a status (`COMPLETED`, `PARTIAL_FAILURE`, `FAILED`). A file whose content hash matches a previous `COMPLETED` import is blocked with a message identifying the original import date and record count. A file matching a `PARTIAL_FAILURE` or `FAILED` import is allowed to be re-imported, but the system presents the user with a summary of the prior attempt (rows succeeded, rows failed) and offers the option to retry only failed rows or re-import the full file. All fingerprint checks and outcomes are written to the import audit log.

---

## 11. Python Face Recognition Service -- Failure Handling

**Question:** If the Python face recognition service (running on the same network segment) is unreachable or unresponsive, does the system block all identity verification workflows, or does it degrade gracefully to an alternative flow?

**My Understanding:** Since the portal is offline-first and the face recognition service is an auxiliary component, a hard dependency would compromise availability. However, silently skipping identity checks could be a security concern for roles or workflows where biometric verification is required.

**Solution:** The system will implement graceful degradation with configurable enforcement levels. Administrators define per-workflow enforcement under Settings > Security > Face Recognition Policy, choosing from three modes: `REQUIRED` (face check must pass; block if service is down), `PREFERRED` (attempt face check; fall back to manual identity confirmation by a supervisor if service is unavailable), and `OPTIONAL` (attempt face check; proceed without it if service is down). When the service is unreachable, the system logs the failure, displays a banner to the user ("Face recognition service unavailable -- manual verification required"), and follows the configured enforcement mode. A health-check endpoint on the Python service is polled every 60 seconds by the async job queue; if three consecutive checks fail, an alert is raised on the Administrator dashboard. All fallback events are recorded in the audit log with timestamp, workstation ID, and the identity of the supervisor who performed manual verification (if applicable).

---

## 12. Metric Version Rollback -- Historical Data Treatment

**Question:** When an Administrator rolls back a metric definition to a previous version, does the system recompute all historical charts and dashboards using the restored definition, or does the old definition only apply to new data going forward?

**My Understanding:** Full historical recomputation provides consistency but may be expensive on an on-premise server with limited resources and could take significant time for large datasets. Forward-only application is fast but creates a discontinuity in trend lines where the definition changed.

**Solution:** Rollback will apply the restored metric definition going forward by default, meaning new data points and newly rendered charts use the rolled-back version. Historical data points that were computed under previous versions are retained as-is and tagged with the metric version that produced them. To maintain transparency, dashboards will display a version-change marker (a vertical annotation line) on time-series charts at the point where the definition changed. Administrators can optionally trigger a full historical recomputation as an async background job; the job queue will process this with a lower priority than real-time operations and provide progress updates on the Admin dashboard. During recomputation, dashboards continue serving the existing cached values until the job completes, at which point values are atomically swapped. Recomputation jobs can be cancelled if server resources become constrained.

---

## 13. Bank File Import Idempotency -- Idempotency Key Design

**Question:** What constitutes the idempotency key for imported bank files used in payment reconciliation -- is it the file-level hash, individual transaction IDs within the file, or a composite key?

**My Understanding:** A file-level hash prevents re-importing the same file but does not handle the case where a bank sends a corrected file containing a mix of already-processed and new transactions. Individual transaction IDs handle partial overlaps but require the bank file format to include unique transaction identifiers, which is not always guaranteed.

**Solution:** Use a two-tier idempotency strategy. At the file level, a SHA-256 hash of the file content prevents exact-duplicate file imports (same behavior as the bulk import fingerprint in question 10). At the transaction level, each row is keyed by a composite of: bank transaction reference number + transaction date + amount + payer/payee identifier. During import, each transaction is checked against this composite key in the `bank_transactions` table. Transactions matching an existing key are skipped and reported in the import summary as "previously processed". New transactions are inserted and queued for reconciliation. This allows corrected or supplemental files from the bank to be imported safely. If the bank file format lacks a transaction reference number, the system falls back to a composite of date + amount + payer/payee + row sequence number within the file, and the Finance Clerk is warned that manual review of potential duplicates is recommended.

---

## 14. Reconciliation Exception Queue -- Resolution Ownership and Escalation

**Question:** Who is responsible for resolving reconciliation exceptions (mismatches between expected payments and bank file entries), and is there an auto-escalation mechanism if exceptions remain unresolved past a certain threshold?

**My Understanding:** The Finance Clerk likely handles day-to-day reconciliation, but without escalation rules, exceptions could sit unresolved indefinitely -- especially edge cases that require higher authority (e.g., write-offs, adjustments beyond a certain amount).

**Solution:** Reconciliation exceptions are initially assigned to the Finance Clerk who initiated the bank file import. Each exception appears in the Finance Clerk's exception queue with a severity classification: `LOW` (minor rounding differences within a configurable tolerance, default $0.50), `MEDIUM` (partial payment or overpayment), `HIGH` (missing payment or unknown transaction). Finance Clerks can resolve `LOW` and `MEDIUM` exceptions directly by marking them as adjusted, matched, or written off (within a configurable write-off threshold set by Administrators, default $25.00). `HIGH` exceptions and write-offs exceeding the threshold require Administrator approval. Auto-escalation is triggered if any exception remains unresolved beyond a configurable time window (default: 5 business days). Escalation promotes the exception to the Administrator dashboard with a notification. A second escalation tier (default: 10 business days) sends an alert to all Administrators. All resolution actions, approvals, and escalation events are captured in the audit log.

---

## 15. Monthly Settlement CSV -- Cut-Off Period Definition

**Question:** Does the monthly settlement CSV use a fixed calendar month (1st through last day of month) as its cut-off period, or is the billing cycle configurable (e.g., 15th to 14th, or aligned to a fiscal calendar)?

**My Understanding:** Many organizations operate on fiscal calendars or custom billing cycles that do not align with calendar months. A fixed calendar-month cut-off may force Finance Clerks to manually adjust or split data to match their actual settlement period.

**Solution:** The settlement cut-off period will be configurable by Administrators under Settings > Finance > Settlement Cycle. Three modes will be supported: `CALENDAR_MONTH` (1st through last day, the default), `CUSTOM_FIXED` (Administrator specifies a start day-of-month, e.g., the 15th, and the cycle runs from the 15th of one month to the 14th of the next), and `CUSTOM_VARIABLE` (Administrator manually defines each cycle's start and end dates, accommodating irregular fiscal periods). When a Finance Clerk generates the monthly settlement CSV, the system pre-fills the date range based on the configured cycle but allows the clerk to adjust the range before generation if needed. The generated CSV header includes the exact date range used, the cycle mode, and a generation timestamp. Historical settlement CSVs are stored in the Exports page and tagged with their date range for easy retrieval and audit.
