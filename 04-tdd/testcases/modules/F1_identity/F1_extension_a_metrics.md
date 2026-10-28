# QA testcase F1 — read models and instructor metrics

Scope: [TDD F1](../../../modules/F1_identity.md), AC-33–34, AC-39–40, AC-51–53. Design only; all execution statuses remain **Chưa thực hiện**. Prefix `/api/v1/identity`.

DD sources: `03-dd/api/F1_identity.md` §§0.4, 0.8.1–0.8.2, §2; `03-dd/logic/F1_identity.md` BR-12 and BR-18 (removed); `03-dd/validation/F1_identity.md` §10; `02-bd/database/F1_identity.md` read models; `DEC-2026-1024-dd-teacher-f1-read-contracts`, `DEC-2026-1025-dd-system-error-ac-denominator`.

Fixture catalog: isolated DB/Redis and service-port stubs. T0 is `2026-10-27T12:00:00+07:00`. Instructor I1 owns classes C1/C2/C3; I2 owns C4. Learners S1/S2/S3 are current in C1; S4 has `left_at=T0-1d`; S5 belongs only to C4. For metrics cases seed F2 assignment and F4 submission-port fixtures as stated per case; no global `user_problem_best_score` substitution. Run against real API/integration adapters for service-port and DB assertions.

| AC | Branch | Case | Design review |
| :--- | :--- | :--- | :--- |
| 33 | Mean over active students / empty assignment set | TC-F1-AC33-01, TC-F1-AC33-02, TC-F1-AC33-03 | Reviewed 2026-10-28 theo BR-12 hiện hành |
| 34 | Feed excludes synthetic absence | TC-F1-AC34-01 | Reviewed 2026-10-28 |
| 39 | Event aggregation, UTC+7 day, idempotent replay | TC-F1-AC39-01, TC-F1-AC39-02 | Reviewed 2026-10-28 |
| 40 | Calendar window and user isolation | TC-F1-AC40-01 | Reviewed 2026-10-28 theo API #55 hiện hành |
| 51 | Summary distinct counts and class metrics | TC-F1-AC51-01, TC-F1-AC51-02 | Reviewed 2026-10-28 |
| 52 | Six completed ISO weeks / missing week | TC-F1-AC52-01, TC-F1-AC52-02, TC-F1-AC52-03 | Reviewed 2026-10-28 |
| 53 | Attention ranking, severity, scope and required metrics failure | TC-F1-AC53-01, TC-F1-AC53-02, TC-F1-AC53-03 | Reviewed 2026-10-28 theo quyết định 2026-10-27 |

### Branch inventory

| AC | Normal | Abnormal | Boundary | Security | Concurrency | Recovery / degraded |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 33 | TC-F1-AC33-01 | — | Pre-assignment history: TC-F1-AC33-02; no-active-student empty set: TC-F1-AC33-03 | Instructor class ownership covered by API common class rules; not duplicated here | DD §3 says ordinary single-user reads have no meaningful write race | Port failure is not defined specifically for #29; no error oracle asserted |
| 34 | — | No synthetic absence event: TC-F1-AC34-01 | Cross 06:00 boundary: TC-F1-AC34-01 | Feed scoped by class query in AC34 | No scheduled writer exists | No job recovery path; BR-18 removed |
| 35 | TC-F1-AC35-01 | Wrong current password must preserve flag: TC-F1-AC35-02 | First login boundary: TC-F1-AC35-01 | Source-type controls: TC-F1-AC35-01 | No meaningful single-user race per API §3 | — |
| 36 | TC-F1-AC36-01 | Admin-created OTP cannot verify: TC-F1-AC36-02 | OTP reuse: TC-F1-AC36-01 | OTP limited to self-registration: TC-F1-AC36-02 | No concurrent verify side effect beyond idempotent replay contract | — |
| 37 | Wrong-attempt cap: TC-F1-AC37-01 | Expired/unknown OTP generic rejection: TC-F1-AC37-02 | Fifth wrong attempt / following correct attempt: TC-F1-AC37-01 | Generic response, no activation: both cases | Counter must remain atomic under concurrent attempts: no explicit concurrency contract, not asserted | — |
| 38 | First resend: TC-F1-AC38-01 | Cooldown and no-enumeration: TC-F1-AC38-01/02 | After cooldown: TC-F1-AC38-02 | Unknown/admin/active accounts: TC-F1-AC38-01/02 | Redis atomicity not separately specified | Cooldown recovery: TC-F1-AC38-02 |
| 39 | Aggregate event sequence: TC-F1-AC39-01 | — | UTC+7 midnight: TC-F1-AC39-01 | Per-user event ownership as actor fixture | Concurrent duplicate delivery: TC-F1-AC39-02 | Sequential replay: TC-F1-AC39-01 |
| 40 | Calendar window: TC-F1-AC40-01 | Missing session: TC-F1-AC40-01 | 364-day/date boundary: TC-F1-AC40-01 | User isolation: TC-F1-AC40-01 | Read-only, no meaningful race in contract | — |
| 41 | Login after verification: TC-F1-AC41-01 | Pending/wrong password: TC-F1-AC41-01 | Pending to active: TC-F1-AC41-01 | Student/instructor login and no token while pending: TC-F1-AC41-01 | No concurrent branch defined | Verify then retry: TC-F1-AC41-01 |
| 42 | Grace-period admin restore control: TC-F1-AC42-01 | Anonymized restore denied: TC-F1-AC42-01 | Anonymized vs grace state: TC-F1-AC42-01 | Server rejects even direct request: TC-F1-AC42-01 | No meaningful race specified | — |
| 43 | Owned-class delete: TC-F1-AC43-01 | Foreign owner denial: TC-F1-AC43-02 | Cascade/event boundary: TC-F1-AC43-01 | Owner check: TC-F1-AC43-02 | API §3 excludes meaningful ordinary class-delete write race | Downstream idempotent consumer: TC-F1-AC43-01 |
| 44 | Defaults and valid preferences: TC-F1-AC44-01 | Invalid enum/value: TC-F1-AC44-01 | Default-to-custom readback: TC-F1-AC44-01 | User-scoped preferences: TC-F1-AC44-01 | API §3 says single-user writes have no meaningful race | AI outage does not block save: TC-F1-AC44-01 |
| 45 | Seed matrix: TC-F1-AC45-01 | Unauthorized roles: TC-F1-AC45-01 | 3×11×4 seed shape: TC-F1-AC45-01 | Permission grant/deny at server: TC-F1-AC45-01/02 | No matrix race contract cited | Permission available after authorized grant: TC-F1-AC45-02 |
| 46 | First bootstrap: TC-F1-AC46-01 | Missing env/invalid password: TC-F1-AC46-01 | Existing ADMIN skip: TC-F1-AC46-01 | Privileged account and secret handling: TC-F1-AC46-01 | Simultaneous starts: TC-F1-AC46-02 | Restart idempotency: TC-F1-AC46-01/02 |
| 47 | Expired SELF row anonymized: TC-F1-AC47-01 | ADMIN and under-grace rows skipped: TC-F1-AC47-01 | Exactly 30 days: TC-F1-AC47-02 | PII/OAuth removal and ADMIN preservation: TC-F1-AC47-01 | No concurrent schedule behavior specified | Repeat job: TC-F1-AC47-01 |
| 48 | Persist supported language: TC-F1-AC48-01 | Unsupported language: TC-F1-AC48-01 | Initial/default value: TC-F1-AC48-01 | Authenticated owner only | No meaningful race under API §3 | Omitted fields remain unchanged: TC-F1-AC48-01 |
| 49 | Active enrollment list/detail: TC-F1-AC49-01 | Left/foreign/nonexistent not-found equivalence: TC-F1-AC49-01 | Joined-at ordering: TC-F1-AC49-02 | Owner derived from session: both cases | Read-only; no meaningful race | — |
| 50 | Successful join clears counter: TC-F1-AC50-01 | Wrong/malformed/expired code and blocked attempt: TC-F1-AC50-01 | Attempts 1–5 vs 6: TC-F1-AC50-01 | Per-user isolation: TC-F1-AC50-01 | Concurrent failures and atomic quota: TC-F1-AC50-02 | Sliding-window expiry: TC-F1-AC50-01 |
| 51 | Distinct overview counts: TC-F1-AC51-01 | Service port failure: TC-F1-AC51-02 | Duplicate learner across classes: TC-F1-AC51-01 | Only instructor-owned classes: TC-F1-AC51-01 | Read-only; no meaningful race in contract | Null when no scored attempts: TC-F1-AC51-01 |
| 52 | Six completed weeks: TC-F1-AC52-01 | Required F4 failure: TC-F1-AC52-03 | Empty week and current-week boundary: TC-F1-AC52-01 | Foreign class same 404: TC-F1-AC52-01 | Redis cache scope: TC-F1-AC52-02 | Cache may last 15 minutes; unavailable port recovery: TC-F1-AC52-02/03 |
| 53 | Status precedence: TC-F1-AC53-01 | Required F4 failure: TC-F1-AC53-02 | Five-row maximum and severity/tie: TC-F1-AC53-01/03 | Current enrollments and class scope: TC-F1-AC53-01 | Read-only; no meaningful race | Port recovery: TC-F1-AC53-02; severity oracle owner-confirmed 2026-10-27 in API #58 |
| 54 | Verify then reset: TC-F1-AC54-01 | Wrong/expired/used/unknown: TC-F1-AC54-01 | TTL and atomic one-time consume: TC-F1-AC54-01/02 | Generic error and no session: TC-F1-AC54-01 | Simultaneous consume: TC-F1-AC54-02 | Retry after rejected OTP does not change password: TC-F1-AC54-01 |
| 55 | Eligible one-use restore: TC-F1-AC55-01 | Ineligible/expired/used: TC-F1-AC55-02 | 30-day eligibility / 5-minute ticket: both cases | No ticket for ADMIN/anonymized: both cases | Simultaneous ticket use: TC-F1-AC55-03 | Login again after restore, no session from restore: TC-F1-AC55-01 |

### TC-F1-AC33-01 — Completion averages current students at read time

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-33; API #29; BR-12; `DEC-2026-1022-identity-schema-sync` §4 |
| Kind / priority | Normal, Boundary; P1, teacher-facing calculation |
| Environment | API + DB + F2 `ListClassAssignments` and F4 `GetClassStudentSubmissionMetrics` fixtures |
| Preconditions | Override shared roster for this isolated case: only S1 and S2 are active in C1; S3 has no active C1 enrollment; S4 left C1. C1 has 4 active assignments, S1 accepted 2 after assignment and S2 accepted 4; S4 accepted 4 but is excluded. Ports report acceptance per current assignment. C2 has no assignments. |
| Test data | `GET /classes/C1/progress`, `GET /classes/C2/progress` as I1. Set `class_completion_stats` absent; no aggregate job is running. |
| Steps | 1. Call C1 progress. 2. Change S1 F4 result from 2 to 3 accepted. 3. Read again. 4. Read C2. |
| Expected | C1 mean is `(2/4 + 4/4)/2 = 75%`; S4 is excluded. Second read reflects `(3/4 + 4/4)/2 = 87.5%` without a summary event/job. C2 completion is `null` (UI renders `-`). No row is read/written in a completion-stat table. |
| Cleanup | Remove isolated classes, assignment and submission fixtures. |
| Evidence | Requests/responses; enrollment rows; F2/F4 port inputs; DB query showing no aggregate read model dependency. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC53-03 — Severity ordering within status and five-row cut-off

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-53; API #58; BR-10; owner decision 2026-10-27 |
| Kind / priority | Boundary; P0 because top-five truncation must preserve highest-risk learners |
| Environment | API + DB + F2 assignment/F4 class-metrics fixtures; fixed clock T0 |
| Preconditions | I1 owns C1 with six current learners and one ON_TRACK learner. Two ABSENT learners last submitted inside C1 12 and 8 full days ago; two NEEDS_SUPPORT learners have source-backed accepted-rate sequences 90→70→40% (50-point drop) and 70→60→50% (20-point drop), each with a class submission within seven days. Two WATCH learners have completion 10%, same severity, distinct studentIds W1<W2 and a recent class submission; neither has declining accepted-rate sequence. No learner is INSUFFICIENT_DATA. |
| Test data | GET `/instructor/classes/attention-students?classId=C1` as I1; W1/W2 each have one accepted assignment among ten active assignments, and stable UUIDs `00000000-0000-4000-8000-000000000071` / `...72`. |
| Steps | Call #58; compare returned status, studentId order, reason source and count against the exact F2/F4 fixtures. |
| Expected | Exactly five rows in order: ABSENT 12d, ABSENT 8d, NEEDS_SUPPORT 50pp, NEEDS_SUPPORT 20pp, WATCH W1 (10%). WATCH W2 is sixth and absent from response due to limit; ON_TRACK is absent regardless of limit. Within WATCH, the W1/W2 tie is broken by ascending studentId. No submission outside C1 changes ordering. |
| Cleanup | Remove learners, assignment/metrics fixtures and cache. |
| Evidence | Raw response, fixture timestamps/ratios/completion, sorted expected IDs and port traces. |
| Design review | Reviewed 2026-10-28 against API #58 decision 2026-10-27; chưa thực thi |

### TC-F1-AC33-02 — Empty roster and pre-assignment acceptance do not inflate class completion

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-33; API #29; BR-12; `DEC-2026-1022-identity-schema-sync` §4 |
| Kind / priority | Boundary; P1, avoids reporting completion from unrelated history |
| Environment | API + DB + F2/F4 read-port fixtures |
| Preconditions | C1 has two active assignments and one active learner S1 with global accepted history for both problems exclusively before assignment; F4 assignment results are false for both current assignments. C2 has two assignments and one active learner with no accepted results. |
| Test data | Read #29 for C1 and C2 as owning instructor. |
| Steps | Call both endpoints and inspect returned `completionPct` and underlying port requests. |
| Expected | C1 and C2 are `0%`; global best-score history before assignment does not count. No completion-stat read model or scheduled aggregation is consulted. |
| Cleanup | Remove C1/C2 and read-port fixtures. |
| Evidence | Responses, active enrollment rows, assignment timestamps, F4 results and port trace. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC33-03 — Class with assignments but no active learners returns null completion

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-33; API #29; BR-12 |
| Kind / priority | Boundary; P2, empty population calculation |
| Environment | API + DB + F2/F4 read-port fixtures |
| Preconditions | C3 has two active assignments, but zero active enrollments; all prior enrollments have `left_at` set. |
| Test data | GET #29 for C3 as owning instructor. |
| Steps | Read completion result and inspect current enrollment/assignment fixtures. |
| Expected | `completionPct=null` under current BR-12 because the mean over zero active learners has no denominator; API must not return `0` or divide by zero. The DD marks this rule `[SoT: Suy luận]`, which remains visible as provenance, but it is the current DD contract for this design case. |
| Cleanup | Remove C3. |
| Evidence | TDD AC-33, BR-12 inference note, response and enrollment/assignment state. |
| Design review | Reviewed 2026-10-28 against current BR-12; chưa thực thi |

### TC-F1-AC34-01 — Absence label does not become a synthetic activity event

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-34; API #45; BR-18 (removed); `03-dd/jobs/F1_identity.md` §3; `02-bd/database/F1_identity.md` §1.12 |
| Kind / priority | Abnormal / compatibility; P1 |
| Environment | API + DB; event/feed fixture |
| Preconditions | C1 has S1 labeled `ABSENT`; feed contains one real event and no absence-derived event. BR-18 job was removed; no 06:00 absence scan is configured. Allowed `activity_type` values exclude `CLASS_HAS_ABSENT_STUDENTS`. |
| Test data | `GET /instructor/dashboard/recent-activity?classId=C1` at T0; include one real feed event and no absence event. |
| Steps | 1. Read feed immediately before the former 06:00 scan boundary. 2. Advance clock past 06:00 and read again. 3. Inspect `identity_recent_activity` and scheduler registrations. |
| Expected | No scheduled absence scan exists and crossing 06:00 creates no feed row. Feed contains only occurred events; S1 remains included in the roster with `ABSENT` label, but no synthetic “absent” activity is inserted. |
| Cleanup | Delete isolated feed/enrollment fixtures. |
| Evidence | Before/after responses, feed rows, scheduler registration/logs, allowed activity types. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC39-01 — Graded events aggregate language and first-try count once per UTC+7 day

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-39; API §2 `SubmissionGraded`; `02-bd/database/F1_identity.md` §1.12; `DEC-2026-1022-db-sync-round2` |
| Kind / priority | Normal, Recovery/idempotency; P1 |
| Environment | Integration: event broker, consumer, DB |
| Preconditions | U1 has no `user_submission_stats` row. Consumer starts with empty event-id deduplication state. UTC+7 date under test is 2026-10-27. |
| Test data | Three unique events same UTC+7 date: PYTHON, JAVA, PYTHON; only first has `verdict=ACCEPTED`, `firstAttemptForProblem=true`; remaining two false. Redeliver third event with same event identity. Then emit a new event with `submittedAt=2026-10-27T17:05:00Z` (2026-10-28 00:05 UTC+7). |
| Steps | 1. Publish the three events and wait for consumer acknowledgement. 2. Read `user_submission_stats`. 3. Replay third event. 4. Publish/read next-date event. |
| Expected | Top language is PYTHON (2 vs 1); first-try accepted count is incremented once. Replay does not increment any statistic twice. Event after UTC+7 midnight is assigned to 2026-10-28 daily bucket. |
| Cleanup | Delete isolated U1 aggregate and event records. |
| Evidence | Event payloads and IDs/timestamps, consumer acknowledgements, before/after aggregate rows and per-day bucket. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC39-02 — Concurrent redelivery of one graded event changes aggregates once

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-39; API §2 `SubmissionGraded`; AC idempotency oracle; event envelope `eventId` |
| Kind / priority | Concurrency, Recovery; P1, duplicate delivery must not inflate learner stats |
| Environment | Integration: event broker, multiple consumer workers, DB |
| Preconditions | U1 has no stats row; all workers use the same isolated event store. |
| Test data | One `SubmissionGraded` event with unique `eventId`, `language=PYTHON`, `ACCEPTED`, `firstAttemptForProblem=true`, submittedAt within test day. |
| Steps | Deliver the identical event concurrently to two consumer workers; wait for both acknowledgements; read stats and event processing state. |
| Expected | Exactly one logical event is applied: top language count rises by one and accepted first-attempt count rises by one, not two. Redelivery is safe under the event consumer's idempotency contract. |
| Cleanup | Delete U1 aggregate/event/deduplication rows. |
| Evidence | Event IDs and delivery timestamps, worker acknowledgements, DB transaction/dedup state and aggregate diff. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC40-01 — Activity calendar returns the caller’s 364-day UTC+7 window

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-40; API #55 / §0.8.1; `DEC-2026-1022-db-sync-round2` §3 |
| Kind / priority | Normal, Security, Boundary; P1 |
| Environment | API + DB |
| Preconditions | T0=2026-10-27 12:00 UTC+7. U1 and U2 have distinct daily stats; U1 has counts on T0 local date and dates inside the prior 363 days, plus a row immediately before the 12-month source window. |
| Test data | `GET /me/activity-calendar` using U1 JWT; request has no user selector. Also call without a valid session. |
| Steps | 1. Call as U1. 2. Compare dates/counts to U1 fixture, including empty dates and old row. 3. Attempt query/body `userId=U2`. 4. Call without session. |
| Expected | `200 {from,to,days}` at documented `/me/activity-calendar`; `days` has exactly 364 ascending local dates ending at T0 date, each `{day,submissionCount}` with zero-filled missing dates and no `level`; old out-of-window row and U2 data absent. Missing valid session receives `401`. DD labels path/field provenance `[SoT: Suy luận]`, while its present contract supplies a testable response shape. |
| Cleanup | Remove daily-stat fixtures. |
| Evidence | Requests, response, DB fixture query, comparison script/output and auth response. |
| Design review | Reviewed 2026-10-28 against current API #55; chưa thực thi |

### TC-F1-AC51-01 — Instructor overview counts distinct learners across owned classes

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-51; API #56; `DEC-2026-1024-dd-teacher-f1-read-contracts`; AC-33/BR-12 |
| Kind / priority | Normal, Boundary; P1 |
| Environment | API + DB + F2/F4 read-port fixtures |
| Preconditions | I1 owns C1/C2/C3, I2 owns C4. S1 current in C1 and C2; S2 current C1; S3 current C3; S4 left C1. For C1 assignment results: S1 2/4, S2 4/4. C2/C3 have no scored submissions. |
| Test data | GET instructor summary #56 as I1; GET as I2 for comparison. |
| Steps | Call both summaries; compare overall and per-class values to fixture rows. |
| Expected | I1 classCount=3; overall studentCount=3 distinct active students (S1 counted once), per-class C1 count=2, C2=1, C3=1; S4 excluded. Per-class completion is calculated from current assignments and F4 accepted results. `avgScore=null` for classes without scored attempts. No class from I2 appears. |
| Cleanup | Remove isolated fixture. |
| Evidence | Both response bodies; enrollment query; F2/F4 stub request/response logs. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC51-02 — Required metrics port failure is not reported as zero

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-51; API #56 / §0.8.2; `DEC-2026-1024-dd-teacher-f1-read-contracts` |
| Kind / priority | Abnormal, Recovery; P1, prevents misleading instructor data |
| Environment | API + DB + F2/F4 port stubs |
| Preconditions | I1 owns C1 with at least one active learner and assignment. Configure F4 metrics port to fail for this read. |
| Test data | GET #56 as I1; repeat with F2 assignment port failing instead. |
| Steps | Call summary with each independent required-port failure and inspect response; restore port and retry. |
| Expected | Each unavailable required data port yields generic `503`, never a successful summary with 0 or fabricated null metrics. Once port recovers, response is computed from current fixture data. |
| Cleanup | Reset stubs and delete fixtures/cache. |
| Evidence | Responses, port failure trace, retry result and no-write DB diff. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC52-01 — Score trend uses six completed ISO weeks

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-52; API #57 / §0.8.2; validation §10; `DEC-2026-1014-instructor-batch2-weekly-rules` |
| Kind / priority | Boundary, Security; P1 |
| Environment | API + F4 weekly-trend port + Redis |
| Preconditions | Freeze T0 at Monday 2026-10-26 UTC+7, 12:00. I1 owns C1; I2 owns C4. The six Monday–Sunday weeks before the current week include scored submissions in five weeks and no scored submissions in one completed week. Current week has scored submissions. |
| Test data | Call #57 with no classId, C1, C4, and invalid UUID as I1. |
| Steps | Read each request, verify point count/week boundaries and inspect F4 port payload/cache keys. |
| Expected | Exactly six points: the six completed ISO Monday–Sunday weeks, oldest to newest; current week is excluded. The completed week with no graded submissions remains in the six and has `avgScore=null`, not zero. C1 request includes only I1's C1 data; C4 and invalid UUID return same `404 IDT-414` as nonexistent class; no cross-class scores. |
| Cleanup | Remove fixtures and cache entries. |
| Evidence | TDD AC-52, API #57 response, week/date fixtures, F4 port trace and cache key. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC52-02 — Score-trend cache is isolated by class scope

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-52; API #57 / §0.8.2; Redis cache rule (maximum 15 minutes, scoped by query) |
| Kind / priority | Security, Boundary; P1, prevents cross-class cache leakage |
| Environment | API + Redis + F4 port fixture |
| Preconditions | I1 owns C1; I2 owns C4. C1/C4 have distinct per-week accepted ratios and scores. Cache starts empty. |
| Test data | Read #57 for C1 as I1, then no classId as I1, then C4 as I2. |
| Steps | Issue calls in sequence without waiting for cache expiry; inspect response points and Redis cache keys. |
| Expected | Each response reflects only its allowed scope; all-class scope for I1 contains only I1 classes. Cache entries are distinct by scope and do not return C1 metrics for C4 or vice versa. Cache lifetime does not exceed 15 minutes. |
| Cleanup | Delete fixture/cache entries. |
| Evidence | Responses, cache keys/TTL and F4 port input trace. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC52-03 — Score-trend metrics failure returns service unavailable

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-52; API #57 / §0.8.2; `DEC-2026-1024-dd-teacher-f1-read-contracts` |
| Kind / priority | Abnormal, Recovery; P1, avoids fabricated score trend |
| Environment | API + F4 port stub + Redis |
| Preconditions | I1 owns C1 with submission metrics. Redis cache is empty/expired. F4 `GetClassStudentSubmissionMetrics` is unavailable. |
| Test data | GET #57 for C1 while F4 port returns an error; restore port and retry. |
| Steps | Call endpoint during outage, then restore service and call again. |
| Expected | Outage returns generic `503`; response is not six fabricated zero/null points. After recovery, endpoint returns the six-week trend from F4 data. Error result is not cached as a successful trend. |
| Cleanup | Reset F4 stub and cache. |
| Evidence | Responses, F4 failure/recovery traces and Redis cache entries. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC53-01 — Attention list is bounded, ranked, and scoped to owned current enrollments

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-53; API #58; validation §10; `DEC-2026-1024-dd-teacher-f1-read-contracts` |
| Kind / priority | Security, Boundary; P1 |
| Environment | API + DB + F2/F4 metric fixtures |
| Preconditions | I1 owns C1; C1 has >5 active students including ABSENT, NEEDS_SUPPORT, WATCH and ON_TRACK cases; S4 left C1. I2 owns C4 with an attention student. Reasons are represented by source metrics, not hand-authored labels. |
| Test data | Call #58 with no classId, with C1, and with C4 as I1. Attempt client `limit=100`. |
| Steps | Read results; compare status/reason and stable ordering to the seeded source metrics. |
| Expected | At most five rows, only current students in selected owned scope. Status precedence is ABSENT, then NEEDS_SUPPORT, then WATCH; within each status sort by the metric fixed in API #58, then studentId. `reasonCode`/`reasonValue` must be source-backed; no arbitrary reason strings. C4 returns `404 IDT-414`; caller cannot raise the five-row limit. |
| Cleanup | Remove fixture enrollments and metric inputs. |
| Evidence | Requests/responses, enrollment state, metric inputs, ordering comparison. |
| Design review | Reviewed 2026-10-28 against API #58 decision 2026-10-27; chưa thực thi |

### TC-F1-AC53-02 — Attention metrics failure is not converted into an empty list

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-53; API #58 / §0.8.2; `DEC-2026-1024-dd-teacher-f1-read-contracts` |
| Kind / priority | Abnormal, Recovery; P1, prevents hiding learners needing attention |
| Environment | API + F4 port stub |
| Preconditions | I1 owns C1 with active learners whose fixture metrics would produce at least one attention item. |
| Test data | Make F4 metrics port fail for #58; restore port and retry. |
| Steps | Call #58 during outage, then after recovery. |
| Expected | Required data port failure returns generic `503`; it does not return `{items:[]}` or fabricate a status. After recovery, the source-derived attention rows return. |
| Cleanup | Reset stub and remove fixture. |
| Evidence | Responses, F4 port traces and fixture-driven result. |
| Design review | Reviewed 2026-10-28; not executed |
