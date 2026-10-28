# F5 admin — scheduled jobs and locked-session lifecycle (AC-55–58)

## Scope and sources

- TDD: `04-tdd/modules/F5_ai-review.md`, AC-55–58 and corresponding trace rows.
- DD: `03-dd/jobs/F5_ai-review.md` §§0,2–5; `03-dd/logic/F5_ai-review.md` BR-AI-05/09/12; `03-dd/api/F5_ai-review.md` §§4,8,10–12,32–33.
- Every case is design-only; execution status **Chưa thực hiện**.

## Fixture catalog

- Timezone UTC+7. JOB-AI-03 fixed clock around `2026-10-31T23:59:59+07:00` and `2026-11-01T00:05:00+07:00`; singleton budget config ID 1.
- `stu_a` is STUDENT, `admin_a` ADMIN. Interview fixtures use active stages EXPLAIN/CHALLENGE/SCALE_UP, explicit `turn_count`, turn rows and `updated_at`; `enoughTurns=4`, idle threshold 15 minutes. Provider/rubric outputs deterministic.
- Outbox fixtures relative to controlled `now()`: old sent `published_at=now()-91d`; recent sent `now()-1d`; old pending `published_at=NULL, created_at=now()-100d`.

## Branch inventory / Coverage

| AC | Branch inventory | Case IDs | Review |
|---|---|---|---|
| 55 | Month rollover/unlock/reset; startup catch-up; rerun idempotency; INFO without audit | TC-F5-AC55-01..02 | Draft |
| 56 | Exactly 5x/open dedupe; ADMIN excluded; reviewed→new open; below-threshold and empty baseline; no lock | TC-F5-AC56-01..03 | Draft |
| 57 | Current session turn/hint/completion allowed; stale enough-turn sweep grades despite lock; insufficient turns abandons; race guard; new report/session denied; P3 allowed | TC-F5-AC57-01..03 | Draft |
| 58 | Expired sent delete; recent sent/old pending retained; rerun idempotent; multi-instance safety | TC-F5-AC58-01..02 | Draft |

### TC-F5-AC55-01 — Rollover starts new month and unlocks budget

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-55; JOB-AI-03 §3; BR-AI-05 step 10 |
| Kind / priority | Recovery; P0 |
| Environment | Job integration + fixed PostgreSQL clock + API |
| Preconditions | October period_start, locked_at set, October usage over limit. |
| Test data | Singleton id=1; clock advances to Nov 1, 00:05 UTC+7; no November usage. |
| Steps | Execute monthly job after boundary; inspect singleton and issue eligible student AI call. |
| Expected | period_start=Nov 1, lock cleared, used tokens for new period=0; new student AI can proceed if other gates pass. Job writes INFO with changed count/new period/unlock flag; no system audit row. |
| Cleanup | Restore clock/config; delete usage and request artifacts. |
| Evidence | Before/after config, usage sum, INFO log, audit query, API result. |
| Design review | Draft |

### TC-F5-AC55-02 — Startup catch-up and repeated rollover are idempotent

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-55; JOB-AI-03 §3; TDD AC-55 |
| Kind / priority | Recovery / Boundary; P1 |
| Environment | Job integration + fixed PostgreSQL clock |
| Preconditions | Service was down across month start; stale October period and lock remain; current date Nov 2. |
| Test data | Singleton period_start=Oct 1, locked; capture updated_at/audit. |
| Steps | Run startup catch-up once; then invoke scheduled job again in same month. |
| Expected | First run advances to Nov 1 and clears lock; second updates zero rows and leaves row unchanged. No audit record on either run. |
| Cleanup | Restore singleton and clock. |
| Evidence | Row snapshots, job INFO entries/changed count, audit query. |
| Design review | Draft |

### TC-F5-AC56-01 — Five-times threshold creates one alert and excludes ADMIN

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-56; JOB-AI-05 §5; BR-AI-12; DB F5 alert rules |
| Kind / priority | Normal / Concurrency; P0 |
| Environment | Job + PostgreSQL + identity role lookup |
| Preconditions | Last 24 hours: `stu_c` has 10 calls and nine other active non-admin users have one call each; no OPEN alert for C. ADMIN has 30 calls and is included in baseline under DEC-2026-1028-qa-f5-anomaly-baseline-admin. |
| Test data | Subcase A: 11 users/49 calls => baseline_avg=4.45 and C ratio=2.25. Subcase B adds 30 more C calls: 11 users/79 calls => baseline_avg=7.18 and C ratio=5.57. All rows lie inside the fixed 24-hour window. |
| Steps | Run A and inspect no alert; add B's 30 usage rows, then run JOB-AI-05 twice consecutively; query alerts and budget row. |
| Expected | A creates no alert for C; ADMIN usage affects baseline but ADMIN never gets an alert. B creates one OPEN C alert with call_count=40, baseline_avg=7.18, ratio=5.57, exact window; repeated run adds none. Job never alters locked_at. |
| Cleanup | Delete test usage and alerts. |
| Evidence | Job logs, alert rows, role lookup outcomes, budget before/after. |
| Design review | Draft |

### TC-F5-AC56-02 — Reviewed alert may be replaced by new OPEN alert

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-56; JOB-AI-05 §5; API §29; DB F5 alert rules |
| Kind / priority | Normal; P1 |
| Environment | Job/API integration + PostgreSQL |
| Preconditions | Usage still anomalous; prior alert for C is REVIEWED and there is no OPEN alert. |
| Test data | Capture old reviewed_at and row ID. |
| Steps | Run job once. |
| Expected | New distinct OPEN row with current window/count/baseline/ratio; old row stays REVIEWED with timestamp unchanged; exactly one OPEN after run. |
| Cleanup | Delete both alert rows and usage fixture. |
| Evidence | Before/after alert rows and job logs. |
| Design review | Draft |

### TC-F5-AC56-03 — Below-threshold, empty and exactly-five-times boundaries

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-56; JOB-AI-05 §5 |
| Kind / priority | Boundary; P1 |
| Environment | Job integration + PostgreSQL |
| Preconditions | Run A: active counts 2,2,2, ratio=1. Run B: no active user; only stale token rows >24h. Run C in an isolated reset: C has 9 calls and nine other non-ADMIN users have one call each, baseline=18/10=1.80 and C ratio=5.00. |
| Test data | No OPEN alerts; snapshot budget lock before each run. |
| Steps | Run job for A then B, reset fixture and run C under matching clock. |
| Expected | A/B create no alert; B skips safely when baseline=0/no active users; no divide-by-zero. C creates exactly one OPEN alert for C at ratio=5.00 because threshold is inclusive >=5. No run changes the budget lock. |
| Cleanup | Delete usage fixtures. |
| Evidence | Job logs, alert query, budget snapshots. |
| Design review | Draft |

### TC-F5-AC57-01 — Lock blocks only new report/session; existing session operations and P3 continue

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-57; API §§4,8,10–12,32.2; BR-AI-04/05/09 |
| Kind / priority | Normal / Abnormal; P0 |
| Environment | F5/F2 integration + PostgreSQL + provider stub |
| Preconditions | `stu_a` has open interview with two turns; budget just became locked; P3 author separately authorized. |
| Test data | Valid next turn, allowed hint, successful completion/rubric response, valid P3 scripts. |
| Steps | Send next turn; request hint; end/score current session; attempt new session and new report; invoke P3 for problem X. |
| Expected | Existing session turn/hint/complete+score succeed; new student session/report return AIR-401 before provider; P3 succeeds because budget gate/hourly limit do not apply to P3. |
| Cleanup | Delete session/report/usage; restore budget. |
| Evidence | Per-call responses, turn/rubric rows, provider calls, usage and lock state. |
| Design review | Draft |

### TC-F5-AC57-02 — JOB-AI-04 grades exactly four-turn stale session while budget locked

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-57; JOB-AI-04 §4; BR-AI-09; BR-AI-05 scope |
| Kind / priority | Recovery / Boundary; P0 |
| Environment | Job integration + PostgreSQL fixed clock + scoring stub |
| Preconditions | Locked budget; active session has `turn_count=4`, idle >15 min. |
| Test data | Session `50000000-0000-4000-8000-000000000001`; enoughTurns=4; four deterministic rubric scores. |
| Steps | Run sweep. |
| Expected | Session COMPLETED, ended_at set; four rubric_scores and token_usage inserted; one MockInterviewFinished outbox event; no budget-lock retry/abandon behavior. |
| Cleanup | Delete session, turns, scores, usage and outbox. |
| Evidence | Session/dependent rows, outbox, job/provider log. |
| Design review | Draft |

### TC-F5-AC57-03 — Too-few-turn session is abandoned; concurrent fresh activity wins guard

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-57; JOB-AI-04 §4; TDD AC-57 |
| Kind / priority | Abnormal / Concurrency; P1 |
| Environment | Job + PostgreSQL fixed clock, two transactions |
| Preconditions | Session A active/stale with 3 turns. Session B selected as stale candidate; hold sweep before acquiring B's session advisory lock. A learner turn then acquires the same lock, commits a fresh updated_at and releases it. |
| Test data | Distinct IDs `...0002` and `...0003`; B's updated_at advanced to now. |
| Steps | Sweep A; select B, let learner activity commit before sweep acquires the session lock, then resume sweep and inspect its eligibility re-read. In a separate subcase hold the learner lock while sweep tries it. |
| Expected | A becomes ABANDONED with ended_at; no scores, token_usage or event; turns preserved. After lock acquisition B's fresh updated_at fails the idle re-check, so B remains active with the new turn intact and no sweep scoring/provider call. When learner holds the lock sweep skips B; a stale stage-only check is insufficient. |
| Cleanup | Delete sessions and turns. |
| Evidence | Before/after session state, dependent row queries and job/SQL outcome. |
| Design review | Draft |

### TC-F5-AC58-01 — Retention deletes expired sent only and rerun is idempotent

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-58; JOB-AI-02 §2 |
| Kind / priority | Normal / Boundary; P0 |
| Environment | Job integration + fixed PostgreSQL clock |
| Preconditions | Four outbox rows: sent at now()-91d, sent at now()-1d, pending at now()-100d, and sent at exactly now()-90d. Keep PostgreSQL transaction clock fixed for the retention query. |
| Test data | Stable IDs 60000000-0000-4000-8000-000000000001/2/3/5; pending row has published_at NULL. |
| Steps | Run retention; inspect rows; run again; inspect again. |
| Expected | Only 91-day sent row is deleted; recent sent, exactly-90-day sent and pending rows are retained because cutoff uses strict `<`. Second run at the same fixed clock deletes no further row. |
| Cleanup | Delete retained test rows. |
| Evidence | Before/after rows and job logs/counts. |
| Design review | Draft |

### TC-F5-AC58-02 — Concurrent job instances preserve old pending row for relay

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-58; JOB-AI-02 §§0,2 |
| Kind / priority | Concurrency / Recovery; P0 |
| Environment | Two job instances + shared PostgreSQL |
| Preconditions | Pending outbox row older than 90 days; two instances trigger cleanup concurrently; per-job advisory lock enabled. |
| Test data | Pending ID `60000000-0000-4000-8000-000000000004`; one expired sent control row. |
| Steps | Start both jobs concurrently; inspect lock result, rows and relay candidate query. |
| Expected | At most one cleanup execution for cycle; pending row remains and is eligible for relay; only expired sent may be deleted. Age alone never deletes unpublished row. |
| Cleanup | Remove test rows after evidence capture. |
| Evidence | Both instance logs/lock result, DB before/after and relay eligibility query. |
| Design review | Draft |
