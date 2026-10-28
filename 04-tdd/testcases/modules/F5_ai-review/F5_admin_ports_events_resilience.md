# F5 admin — cross-module ports, events và suy giảm (AC-49–54)

## Scope, sources, execution state

- TDD: `04-tdd/modules/F5_ai-review.md`, AC-49–54; trace rows for same ACs.
- DD: `03-dd/api/F5_ai-review.md` §§32–33; `03-dd/logic/F5_ai-review.md` BR-AI-01/04/05/09/11/14/15; `03-dd/validation/F5_ai-review.md` §§9–12; `03-dd/jobs/F5_ai-review.md` §1.
- All cases are design-only and **Chưa thực hiện**. Cross-module integration requires F2/F6/identity services or contract-faithful ports.

## Fixture catalog

- `stu_a` STUDENT; `admin_a` ADMIN; problem `P-X` includes statement/constraints, two public samples and hidden sentinel `HIDDEN-SECRET-123`; F6 practice question `Q-1`.
- Budget singleton may be locked (`limit=1000, used=1000, locked_at` set) or unlocked, as each precondition states. Practice answer saves independently of optional feedback.
- LLM stub valid P3 result: generatorScript `gen()`, validatorScript `check()`, labels `["edge"]`, model `gpt-test-a`, prompt `v2.1`, 120 prompt + 30 completion tokens. Other modes: timeout; malformed response; deterministic report/interview output.
- Identity sink can pause/resume; records event IDs/idempotency effects. PostgreSQL outbox is inspectable. Provider, Redis and identity faults are injected independently.

## Branch inventory / Coverage

| AC | Branch inventory | Case IDs | Review |
|---|---|---|---|
| 49 | Locked student skip with answer persistence; admin budget bypass; monthly token accounting/no problem ID; separate F6 hourly quota | TC-F5-AC49-01..03 | Draft |
| 50 | Valid P3 input minimization/result/token association; provider failure/retry; malformed schema/no problem mutation | TC-F5-AC50-01..03 | Draft |
| 51 | Caller ownership/all sessions incl empty; empty export; provider-down stored export and normal submission export | TC-F5-AC51-01..03 | Draft |
| 52 | New/cache-hit report events; scored vs empty-abandoned interview; disconnected consumer recovery/dedup; payload privacy | TC-F5-AC52-01..04 | Draft |
| 53 | Provider outage leaves core and stored reads available; generation timeout and quota refund; Redis fail-open vs PG budget | TC-F5-AC53-01..03 | Draft |
| 54 | Hidden testcase never appears on AI, persistence, logging, event or learner surfaces | TC-F5-AC54-01 | Draft |

### TC-F5-AC49-01 — Locked student is refused before practice answer is saved

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-49; API §32.1; BR-AI-14; BR-AI-05 |
| Kind / priority | Abnormal; P0 |
| Environment | F5/F6 integration + PostgreSQL |
| Preconditions | Budget locked; `stu_a` can answer Q-1; feedback provider call count zero. |
| Test data | Answer `2 4 8`; locked singleton fixture. |
| Steps | Submit answer through practice mode, which requests optional AI feedback. |
| Expected | P1 throws `AiBudgetExceededException` (`AIR-401`); F6 maps it to `429 INB-042` before saving an attempt. No `user_answers`, practice-history increment, LLM call or PRACTICE_FEEDBACK token row is created; hourly quota is not consumed. Budget infrastructure failure is a separate branch covered by F6 AC39-04 (`201`, saved FAILED attempt), not this refusal. |
| Cleanup | Delete isolated practice attempt. |
| Evidence | F6 response/attempt, budget-port result, LLM call count, token rows. |
| Design review | Draft |

### TC-F5-AC49-02 — ADMIN is exempt from the budget gate on practice feedback

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-49; API §32.1; BR-AI-05 budget scope |
| Kind / priority | Normal / Security; P1 |
| Environment | F5/F6 integration + LLM stub |
| Preconditions | Budget locked; `admin_a` is authorized to invoke same port. |
| Test data | Q-1 answer and successful feedback response. |
| Steps | Invoke port as admin_a, then student equivalent as stu_a. |
| Expected | Admin passes the budget gate; student is denied by the lock. Do not infer bypass of independent quotas. |
| Cleanup | Remove attempts/usage. |
| Evidence | Both outcomes, roles, provider count and persisted state. |
| Design review | Draft |

### TC-F5-AC49-03 — Practice tokens count in budget without problem association

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-49; API §32.1; BR-AI-05; validation §9 |
| Kind / priority | Normal / Boundary; P1 |
| Environment | F5/F6 integration + PostgreSQL/Redis |
| Preconditions | Unlocked budget limit 10,000; used=9,900; successful practice feedback; F5 user review/interview Redis counters at their limits. |
| Test data | Feedback call consumes 150 tokens. |
| Steps | Record successful port usage after 20 F6 practice calls for this user. |
| Expected | PRACTICE_FEEDBACK contributes 150 to monthly total (10,050) and causes lock; token row has no problem_id. F5 review/interview quota does not reject this port operation; separate F6 quota remains F6-owned. |
| Cleanup | Remove usage and restore budget/Redis. |
| Evidence | Token row, budget state, port outcome, Redis F5 counters. |
| Design review | Draft |

### TC-F5-AC50-01 — P3 sends only statement, constraints and two public samples

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-50; API §32.2; validation §6; BR-AI-05 |
| Kind / priority | Normal / Security; P0 |
| Environment | F2↔F5 integration + inspectable LLM stub + PostgreSQL |
| Preconditions | Author requests scripts for P-X; budget permits call. |
| Test data | Statement/constraints; exactly two public samples; private expected answers, hints, aiBrief and hidden testcase exist in problem fixture but are forbidden input fields. Use catalog valid response. |
| Steps | Invoke P3; capture GenerationInput, response and token_usage. |
| Expected | Input contains problem statement/constraints and exactly two samples; excludes answers/hints/aiBrief/hidden cases. Response includes two scripts, labels, model, prompt version and tokens. Usage feature GENERATE_TESTCASE links problem X; no F5 hourly quota consumed. Problem row unchanged. |
| Cleanup | Remove usage/output artifact; problem is read-only. |
| Evidence | Exact port input/response, usage row, Redis counters, problem snapshot. |
| Design review | Draft |

### TC-F5-AC50-02 — Provider failure leaves problem unchanged without consuming F5 quota

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-50; API §32.2; BR-AI-04/05 |
| Kind / priority | Recovery; P1 |
| Environment | F2↔F5 integration + PostgreSQL/Redis + controllable provider |
| Preconditions | Eligible author; provider fails first invocation then succeeds; problem snapshot captured. |
| Test data | Valid P3 input; timeout before a response/usage is received, then valid response. Snapshot F5 hourly counters and cooldown keys before both calls. |
| Steps | Invoke generation on timeout; immediately retry successfully. |
| Expected | First P3 call returns `AIR-501` (F2 maps to `503 PRB-051`); no problem mutation or token row without provider usage. P3 neither reserves nor consumes F5 hourly quota/cooldown, so those keys remain unchanged on both calls; retry reaches provider immediately and records successful GENERATE_TESTCASE usage once. |
| Cleanup | Remove successful retry artifacts and restore Redis. |
| Evidence | Responses, problem diff, token rows, Redis keys and provider calls. |
| Design review | Draft |

### TC-F5-AC50-03 — Malformed P3 response is rejected without problem mutation

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-50; API §32.2; validation §6 |
| Kind / priority | Abnormal; P1 |
| Environment | F2↔F5 integration + provider stub |
| Preconditions | Snapshot problem X. |
| Test data | Separate malformed responses: empty generatorScript; non-string validatorScript; categoryLabels wrong type. |
| Steps | Invoke generation for each response fixture. |
| Expected | Each yields `AIR-502`; no malformed script accepted; problem remains unchanged. |
| Cleanup | Restore stub. |
| Evidence | Responses and problem snapshots. |
| Design review | Draft |

### TC-F5-AC51-01 — Export only caller's three sessions and turns, including empty closed session

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-51; API §32.3; TDD AC-51 |
| Kind / priority | Normal / Security; P1 |
| Environment | F5/identity integration |
| Preconditions | `stu_a` owns A1/A2/A3, with A3 empty and closed; `stu_b` owns B1. |
| Test data | A1 two turns; A2 one; A3 zero; scores exist for A1/A2; B1 has unique sentinel. |
| Steps | Export as stu_a; parse per API contract. |
| Expected | Exactly A1/A2/A3, turns only, A3 has empty turns; no B1; no scores/rubric rows or B sentinel. |
| Cleanup | Delete isolated sessions/turns. |
| Evidence | Export payload and ownership query. |
| Design review | Draft |

### TC-F5-AC51-02 — No sessions produces successful empty export

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-51; API §32.3; TDD AC-51 |
| Kind / priority | Boundary; P2 |
| Environment | F5/identity integration |
| Preconditions | `stu_empty` owns no interview sessions. |
| Test data | Empty query result. |
| Steps | Request transcript export. |
| Expected | P4 returns exactly `[]`; no other user's data and no LLM call. HTTP/file serialization is verified in the F1 export case, since P4 itself is an in-process read port. |
| Cleanup | None. |
| Evidence | Status and response payload. |
| Design review | Draft |

### TC-F5-AC51-03 — AI outage does not impair stored transcript or submission export

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-51; API §32.3; AC-53 |
| Kind / priority | Recovery; P1 |
| Environment | Integration; provider down; persistence and identity available |
| Preconditions | `stu_a` has stored transcript and regular submission-export data. |
| Test data | Existing transcript/submission records; provider unavailable. |
| Steps | Request transcript export and regular submission export. |
| Expected | Both read/export operations succeed from stored data; provider call count remains zero. F5 PostgreSQL and transcript port remain healthy; only the LLM provider is unavailable. A transcript-port infrastructure failure is a distinct F1 `503` branch, not an LLM outage. |
| Cleanup | None. |
| Evidence | Responses and provider call count. |
| Design review | Draft |

### TC-F5-AC52-01 — New and cache-hit reports each emit one completion event

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-52; API §33 E1; JOB-AI-01; BR-AI-15 |
| Kind / priority | Normal; P0 |
| Environment | Integration DB outbox + broker + identity consumer |
| Preconditions | Consumer active; two distinct ACCEPTED submissions S1 and S2 have the same problem/language/normalized source hash and owner, no reports, and the same ACTIVE prompt. S1 misses cache; S2 subsequently hits S1's cache entry. |
| Test data | S1=`30000000-0000-4000-8000-000000000001`, S2=`30000000-0000-4000-8000-000000000002`; inspectable event IDs. |
| Steps | Generate S1 report; request S2 report; repeat the S2 request; let relay publish and receive broker confirms. |
| Expected | Exactly two reports and two distinct completion events: one for generated S1 and one for newly created cache-hit S2. Repeating S2 returns its existing report and creates no event/token/cache-hit increment. Outbox is marked published only after broker confirm; identity deduplicates each eventId. No source or conversation content. |
| Cleanup | Purge test outbox/consumer state. |
| Evidence | API/cache results, outbox rows, broker messages, identity idempotency state. |
| Design review | Draft |

### TC-F5-AC52-02 — Completed interview emits scored event; empty abandoned interview emits none

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-52; API §33 E2; JOB-AI-01/04; BR-AI-15 |
| Kind / priority | Normal / Abnormal; P0 |
| Environment | Integration DB + scoring stub + consumer |
| Preconditions | One interview can complete with score; another empty session can age past 15 minutes. |
| Test data | Deterministic overall score; empty session below enoughTurns=4. |
| Steps | Finish first session; advance time and sweep second; relay events. |
| Expected | Exactly one MockInterviewFinished with overall score for completed session; no event for empty abandoned session. |
| Cleanup | Delete sessions, dependent rows and outbox fixtures. |
| Evidence | Session/rubric/outbox and consumer observations. |
| Design review | Draft |

### TC-F5-AC52-03 — Disconnected consumer receives pending event after reconnect without duplicate effect

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-52; API §33; JOB-AI-01 at-least-once relay |
| Kind / priority | Recovery / Concurrency; P0 |
| Environment | PostgreSQL, broker, pause/resume identity consumer |
| Preconditions | Consumer paused; outbox relay running. |
| Test data | One unique aggregate/event committed while consumer is disconnected. |
| Steps | Pause consumer; create completion; let broker confirm the publish while message waits in the durable queue; reconnect consumer; inject redelivery of the same eventId. Separately fail broker publish once and let the next relay tick retry. |
| Expected | Consumer disconnection does not prevent broker-confirmed outbox from becoming published; the queued message is delivered after reconnect and duplicate eventId causes one logical activity only. In the separate broker-failure subcase, published_at remains NULL and publish_attempts increases until confirmed retry. Consumer acknowledgment and publisher confirmation are observed separately. |
| Cleanup | Remove test event and dedupe record. |
| Evidence | Outbox published_at/attempts, broker deliveries, identity record. |
| Design review | Draft |

### TC-F5-AC52-04 — Event payload excludes source and conversation content

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-52; API §33 E1/E2; validation §12 |
| Kind / priority | Security; P0 |
| Environment | Integration outbox/broker/logs |
| Preconditions | Unique sentinels `SRC-PRIVATE-52`, `CHAT-PRIVATE-52` exist in source/transcript. |
| Test data | One report and one completed interview using those contents. |
| Steps | Produce both events; inspect serialized outbox payload, broker payload and AI logs. |
| Expected | Sentinels and source/conversation text absent; payload otherwise conforms semantically to declared event fields. |
| Cleanup | Purge fixture events and data. |
| Evidence | Payload/log captures and sentinel scan. |
| Design review | Draft |

### TC-F5-AC53-01 — AI outage preserves submission, judging, reads and usage administration

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-53; BR-AI-11; BR-AI-05; graceful-degradation rule |
| Kind / priority | Recovery; P0 |
| Environment | Cross-module integration; provider down, core DB/judge available |
| Preconditions | Existing accepted submission/result, stored report/session, admin usage fixtures. Provider times out for generation. |
| Test data | Student, instructor, admin actors with valid permissions. |
| Steps | Submit normal answer, poll judge, view result, instructor list, admin queue, read stored report/session/progress, then separately trigger new report/session/turn/hint/completion. |
| Expected | Core operations and persisted AI reads succeed without AI error. Only generation-dependent operations fail locally after their configured timeout; no fast circuit-breaker error. |
| Cleanup | Remove created test data. |
| Evidence | Responses, judge state, provider timeout logs and core availability. |
| Design review | Draft |

### TC-F5-AC53-02 — Provider failure refunds reservation so immediate retry can proceed

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-53; BR-AI-04 step 8; BR-AI-05; validation §9 |
| Kind / priority | Recovery; P1 |
| Environment | Integration API + PostgreSQL/Redis + controllable LLM |
| Preconditions | User under budget; counters clear; first provider request fails, second succeeds. |
| Test data | Identical report request; first AIR-501, then valid result. |
| Steps | Call once during provider failure; immediately retry. |
| Expected | Failed attempt creates no token usage and returns hour/cooldown reservation; retry reaches provider and succeeds, not AIR-402/AIR-404 due to prior failure. |
| Cleanup | Remove report/usage and user's Redis keys. |
| Evidence | Responses, Redis keys, token_usage, provider call count. |
| Design review | Draft |

### TC-F5-AC53-03 — Redis fail-open does not bypass PostgreSQL budget lock

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-53; BR-AI-04 step 9; BR-AI-05; validation §9 |
| Kind / priority | Degraded / Security; P1 |
| Environment | Integration; Redis unavailable; PostgreSQL and provider up |
| Preconditions | `stu_a`; first subcase unlocked; second subcase locked. |
| Test data | Valid new report request in both subcases. |
| Steps | Request with unlocked budget while Redis down; lock budget then repeat. |
| Expected | Redis-only cooldown/hour checks fail open with WARN and first eligible request reaches provider; locked PG budget still denies second with AIR-401 before provider. |
| Cleanup | Restore Redis and budget singleton. |
| Evidence | Responses, WARN log, provider calls and budget state. |
| Design review | Draft |

### TC-F5-AC54-01 — Hidden testcase sentinel never reaches AI, learner or F5 observability

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-54; API §32.4; validation §§10–11; F2 hidden-case rule |
| Kind / priority | Security; P0 |
| Environment | E2E F2/F5; inspectable provider, DB, outbox, logs and learner surfaces |
| Preconditions | Problem X hidden testcase contains `HIDDEN-SECRET-123`; report and interview paths available. |
| Test data | Sentinel only in hidden input/output; public samples are distinct. |
| Steps | Generate report; open/run/complete interview from submission; inspect provider requests/responses, token rows, events, logs and learner-facing responses/views. |
| Expected | Sentinel absent from every AI input/output, token row, event, AI log and learner-facing surface; no hidden detail leaks via errors. |
| Cleanup | Delete isolated report/session/usage/event and problem fixture. |
| Evidence | Captured payloads, DB, logs, events, learner view and sentinel scan. |
| Design review | Draft |
