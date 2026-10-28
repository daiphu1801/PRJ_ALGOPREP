# User screen cases — USR0201 submission result

Scope: result page rendering, reconnection, privacy and learner/teacher route behavior. TDD: `04-tdd/screens/users/USR0201_submission_result.md`; DD: `03-dd/screens/users/USR0201_submission_result.md`, §§3,7. F4 remains authoritative for judge values and hidden-case policy. All cases have reviewed designs and remain unrun.

Fixture aliases, independent scenario reset, role assignments and mock/integration evidence follow [fixture conventions](../FIXTURE_CONVENTIONS.md). Per-case Test data overrides file baseline.

## Fixtures

- Learner Alice owns SUB-901 for P-901. Bob owns SUB-902. Teacher T1 may view Alice's submission through a class route. P-901 published; P-902 withdrawn. Sample cases are `S1`,`S2`; hidden case marker `HIDDEN_901` is never sent to learner browser. SUB-901 statuses can be Queued→Running→Accepted. Use clock to create records at 89 days 23:59 and 90 days 00:01 old.
- API payload fixture supplies score, beats only where contract says denominator exists, and separate static result case data plus live updates.

## Coverage

| AC | Case | Branch | Review |
|---|---|---|---|
| SR-AC-01 | TC-USR0201-SRAC01-01 | completed result content | Draft |
| SR-AC-02 | TC-USR0201-SRAC02-01 | queued/live transition | Draft |
| SR-AC-03 | TC-USR0201-SRAC03-01 | reconnect and channel loss | Draft |
| SR-AC-04 | TC-USR0201-SRAC04-01, -02 | sample merge/hidden denial | Draft |
| SR-AC-05 | TC-USR0201-SRAC05-01 | compile error privacy | Draft |
| SR-AC-06 | TC-USR0201-SRAC06-01, -02 | 90-day retention and exact-cutoff boundary | Draft |
| SR-AC-07 | TC-USR0201-SRAC07-01, -02 | not-found, forbidden and transport error | Draft |
| SR-AC-08 | TC-USR0201-SRAC08-01–04 | exact Beats boundary, non-Accepted score, System Error | Draft |
| SR-AC-09 | TC-USR0201-SRAC09-01, -02 | next action, withdrawn/null problem and AI independence | Draft |
| SR-AC-10 | TC-USR0201-SRAC10-01 | teacher read-only path | Draft |

### TC-USR0201-SRAC01-01 — Completed result loads full static content without realtime channel

| Field | Value |
|---|---|
| AC / DD source | SR-AC-01; DD USR0201 §§3,7 |
| Kind / priority | Normal; P1 |
| Environment | Integration/E2E |
| Preconditions | Alice requests completed Accepted SUB-901. |
| Test data | Source, verdict, permitted sample summary, score and timestamps fixture. |
| Steps | Open terminal result route; inspect source/verdict/score/runtime/memory/Beats/cases and realtime requests. Copy source with Clipboard allowed then denied. |
| Expected | All response-backed static result sections render immediately; no SSE/WebSocket opens for terminal submission. Successful Copy sends exact source to clipboard and success feedback; denied copy shows error with unchanged source. No duplicate mutation. |
| Cleanup | Remove fixture. |
| Evidence | UI capture and network trace. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0201-SRAC02-01 — In-progress result fills progressively then becomes final

| Field | Value |
|---|---|
| AC / DD source | SR-AC-02; DD USR0201 §§3,7 |
| Kind / priority | Normal; P1 |
| Environment | Integration with event stream |
| Preconditions | SUB-901 queued; per-case updates controllable. |
| Test data | One in-progress submission with 12 cases; ordered case progress and terminal Accepted snapshot including final runtime/memory. |
| Steps | Open result; emit updates; then terminal event. |
| Expected | Current status renders immediately; 12 rows progress in position order with correct passed/total, no early stop. While judging row runtime/memory show dashes; terminal snapshot supplies final values and verdict without reload or new submission. Events for other ID/stale seq are ignored. |
| Cleanup | Delete fixture and close stream. |
| Evidence | Timestamped event/UI trace. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0201-SRAC03-01 — Reconnect or lost channel reconciles current result

| Field | Value |
|---|---|
| AC / DD source | SR-AC-03; DD USR0201 §§3,7 |
| Kind / priority | Recovery; P1 |
| Environment | Integration/E2E with stream fault injection |
| Preconditions | SUB-901 Running; disconnect after S1 update. |
| Test data | 12 cases, four completed before disconnect and three during outage; isolated permanently unavailable realtime with GET status available. |
| Steps | Reconnect; repeat with stream permanently unavailable but API fetch available. |
| Expected | Reconnect reconciles seven rows once and continues without gaps. Permanent channel loss polls status every 3s at most 10 times, then slow-update notice without failed verdict; terminal GET applies if received and reload shows true state. No duplicate submission. |
| Cleanup | Restore stream and delete fixture. |
| Evidence | Event/fetch logs, final UI and submission count. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0201-SRAC04-01 — Sample cases merge; hidden cases remain inaccessible

| Field | Value |
|---|---|
| AC / DD source | SR-AC-04; DD USR0201 §§3,7; F4 hidden-case contract |
| Kind / priority | Security; P0 |
| Environment | Integration with DOM, accessibility and network inspection |
| Preconditions | Alice owns SUB-901; visible samples originate from both problem samples and user custom cases. |
| Test data | Sample S1 actual output 2001 characters, expected/input in F2; Hidden HIDDEN_901 absent from payload; independent F2 Sample-detail 503. |
| Steps | Expand S1; compare F2 input/expected against F4 actual by testcaseId; repeat F2 fault; inspect disabled Hidden row. |
| Expected | Sample shows merged input/expected from F2 and actual from F4 clipped to 2000 characters. F2 fault leaves actual output available and input/expected as dashes, not whole-result error. Hidden has no details request/control activation and no input/expected/actual/diff in any learner surface. |
| Cleanup | Remove fixture. |
| Evidence | Redacted payloads, DOM scan, response status. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0201-SRAC05-01 — Compile error hides result table and reports learner line only

| Field | Value |
|---|---|
| AC / DD source | SR-AC-05; DD USR0201 §§3,7; F3 diagnostic contract |
| Kind / priority | Security; P1 |
| Environment | Integration with compiler fixture |
| Preconditions | SUB-901 ends Compilation Error. |
| Test data | Diagnostic points to learner line 4; compiler internal path `/worker/secret/build.cc` and server frame marker must not leak. |
| Steps | Open result and inspect visible/accessibility text, logs, response. |
| Expected | COMPILE_ERROR hides testcase table entirely, shows learner line4 mapped correctly and no wrapper/infrastructure line/path. Score/runtime show dashes; Analysis/Interview disabled with accessible explanation. |
| Cleanup | Delete fixture. |
| Evidence | UI, accessibility tree, redacted response/log scan. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0201-SRAC06-01 — Result table retention boundary at 90 days

| Field | Value |
|---|---|
| AC / DD source | SR-AC-06; DD USR0201 §§3,7 |
| Kind / priority | Boundary; P1 |
| Environment | Integration with fixed clock |
| Preconditions | Alice owns two terminal submissions at 89d23h59m and 90d00h01m. |
| Test data | Same verdict/source metadata; table details retained only within specified retention window. |
| Steps | Open both results; advance clock across boundary and reload. |
| Expected | Under-window record shows table; expired record replaces table with contract message while remaining result sections still render. No boundary off-by-one beyond DD rule. |
| Cleanup | Remove submissions; restore clock. |
| Evidence | Clock values, UI captures, API responses. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0201-SRAC07-01 — Missing submission differs from network failure

| Field | Value |
|---|---|
| AC / DD source | SR-AC-07; DD USR0201 §§3,7 |
| Kind / priority | Abnormal; P1 |
| Environment | FE mock and API integration |
| Preconditions | Alice requests a nonexistent ID; separately make API unreachable for owned SUB-901. |
| Test data | Missing ID `SUB-NONE`; owned SUB-901; connection reset. |
| Steps | Request each condition. |
| Expected | All genuine not-found causes share the defined not-found screen; transport failure shows retryable connectivity error, not not-found. No cross-owner existence disclosure. |
| Cleanup | Restore service. |
| Evidence | UI and API status/body. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0201-SRAC08-01 — Score and Beats follow eligibility and denominator

| Field | Value |
|---|---|
| AC / DD source | SR-AC-08; DD USR0201 §§3,7; F4 scoring source |
| Kind / priority | Boundary; P1 |
| Environment | Integration |
| Preconditions | Results cover scoring-enabled accepted, non-accepted, and no-comparison-population cases. |
| Test data | A ACCEPTED: passedCount=12,totalCount=12,beatsSampleSize=25,beatsPercent=73; B ACCEPTED: passedCount=12,totalCount=12,beatsSampleSize=3,beatsPercent=null; C WRONG_ANSWER: passedCount=4,totalCount=12,scoreRatio=0.3333,beatsPercent=null; D SYSTEM_ERROR: passedCount=null,totalCount=12,scoreRatio=null,beatsPercent=null. |
| Steps | Open each result. |
| Expected | A shows 12/12 and Faster than 73%; B shows 12/12 and Not enough comparison data. C shows 4/12 and Beats dash; D shows System Error with score/Beats dashes and no learner-failure alert. scoreRatio is not a duplicate score card. Valid integration ACCEPTED fixture always passes all cases. |
| Cleanup | Remove fixtures. |
| Evidence | API calculation inputs and rendered values. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0201-SRAC09-01 — Next actions remain usable during AI outage

| Field | Value |
|---|---|
| AC / DD source | SR-AC-09; DD USR0201 §§3,7 |
| Kind / priority | Abnormal; P1 |
| Environment | FE mock and integration with AI unavailable |
| Preconditions | Alice sees final Accepted result; AI endpoint times out. |
| Test data | Own ACCEPTED and WRONG_ANSWER submissions; problem available, withdrawn and null variants; AI provider unavailable. |
| Steps | Open each variant, inspect Analysis/Interview/Retry problem and breadcrumb; activate available actions. |
| Expected | Result remains complete during AI outage. ACCEPTED Analysis/Interview links stay enabled and carry submissionId (provider may fail on destination); other verdicts disable them with keyboard-readable reason. Available problem Retry opens /workspace/{problemId}; withdrawn/null problem disables Retry/name link. No next-problem suggestion is invented. |
| Cleanup | Restore AI endpoint. |
| Evidence | UI capture and request trace. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0201-SRAC10-01 — Teacher result view is read-only and URL hints do not mutate

| Field | Value |
|---|---|
| AC / DD source | SR-AC-10; DD USR0201 §§3,7; class ownership source |
| Kind / priority | Security; P0 |
| Environment | Integration/E2E |
| Preconditions | T1 has class permission to read Alice/SUB-901; route opened through teacher context. |
| Test data | Teacher authorized for real (class L,student H), submission currently JUDGING; independent from=list, from=student and absent context; forged URL class/student and outside-class submission. |
| Steps | Open each teacher entry; alter URL hints; complete server judging without reload; then reload; try outside-class submission. |
| Expected | Teacher view has no Analysis/Interview/Retry/Beats and no realtime; state stays current snapshot until reload. Context labels always use true H/L from server. Breadcrumb/sidebar reflect valid caller, default submissions list when absent; hints cannot authorize/change result. Outside-class submission gives same not-found as missing. |
| Cleanup | Restore route and remove fixture. |
| Evidence | API/DB/event traces and UI capture. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0201-SRAC04-02 — Hidden testcase rows expose no detail affordance or payload

| Field | Value |
| :--- | :--- |
| AC / DD source | SR-AC-04; DD USR0201 §3, branch EVT-4; F2 §8 and F4 §8 hidden-testcase contract |
| Kind / priority | Security; P0, prevents disclosure of hidden inputs and expected outputs |
| Environment | Integration/E2E with browser DOM, accessibility tree, storage, network and console inspection |
| Preconditions | Alice owns completed SUB-901 for P-901. One result row is `visibility=HIDDEN`, `testcaseId=null`; separate permitted sample S1 has `testcaseId=TC-S1`. |
| Test data | Hidden canaries `HIDDEN_INPUT_901`, `HIDDEN_EXPECTED_901`, `HIDDEN_ACTUAL_901`; sample S1 input `3`, expected output `6`. |
| Steps | Inspect hidden row and activate its disabled detail control; inspect all client surfaces. Expand S1 and inspect the F2 sample request and merged display. |
| Expected | Hidden row detail control is disabled and activation sends no F2 request. No hidden canary appears in DOM, accessible names, network response, local/session storage or console. S1 opens lazily and merges input/expected by `testcaseId=TC-S1`; only its permitted sample detail is shown. |
| Cleanup | Delete submission/sample fixture and clear browser storage. |
| Evidence | Redacted HAR, DOM/accessibility scan, storage/console scan, and sample merge output. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0201-SRAC06-02 — Retention behavior on both sides of the 90-day cutoff

| Field | Value |
| :--- | :--- |
| AC / DD source | SR-AC-06; DD USR0201 §3 retention-expired state; F4 AC-33 and API §8 |
| Kind / priority | Boundary; P1, avoids hiding retained details early or exposing expired details |
| Environment | Integration with fixed UTC+7 clock and F4 detail API |
| Preconditions | Alice owns terminal submissions with otherwise identical results. Set fixed `now=2026-10-27T12:00:00+07:00`. |
| Test data | SUB-89 created at `2026-07-29T12:00:01+07:00`; SUB-91 at `2026-07-29T11:59:59+07:00`; exact-cutoff SUB-90 at `2026-07-29T12:00:00+07:00`. Their testcase details are available only when F4 contract says retained. |
| Steps | Open each result and record `testcaseDetail.available`, `unavailableReason`, table/banner/source rendering. |
| Expected | F4 applies `submitted_at < now() - 90 days`: SUB-89 and SUB-90 have available testcase details and render rows, including the exact cutoff timestamp; SUB-91 returns `available=false`, `unavailableReason=RETENTION_EXPIRED`, replacing rows with the retention sentence while banner, source and totals remain. |
| Cleanup | Restore fixed clock and remove submissions. |
| Evidence | Exact timestamps, F4 payloads and screenshots for all three records. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0201-SRAC07-02 — Not-found, forbidden and transport failures stay distinct

| Field | Value |
| :--- | :--- |
| AC / DD source | SR-AC-07; DD USR0201 §3 whole-page states; F4 API §§8,14 |
| Kind / priority | Security; P0, avoids ownership disclosure and false not-found on outage |
| Environment | Integration/API with Alice, Bob, teacher context, and fault proxy |
| Preconditions | Alice requests a nonexistent submission, Bob's submission, and an out-of-class submission in teacher area; separately Alice requests her own submission while F4 returns 503. |
| Test data | Nonexistent `SUB-NONE`; Bob-owned `SUB-902`; out-of-class `SUB-903`; Alice-owned `SUB-901`; errors `404 JDG-110`, `403 JDG-301`, and 503. |
| Steps | Open each route; compare not-found screens; then inject 503 and activate retry after recovery. |
| Expected | All three `404 JDG-110` cases use the same non-disclosing not-found UI. `403 JDG-301` uses the permission error state, not not-found. A network/5xx error uses retryable `QueryError`, never not-found; retry fetches SUB-901 and renders it after recovery. No forbidden record fields appear. |
| Cleanup | Restore service and remove test records. |
| Evidence | UI captures, status/code/body, retry request trace, authorization audit. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0201-SRAC09-02 — Withdrawn problem differs from missing problem and AI outage

| Field | Value |
| :--- | :--- |
| AC / DD source | SR-AC-09; DD USR0201 §3 next actions and BR-SC-04; F4 AC-71 / DEC-2026-1024-dd-screens-users-core-contract-changes |
| Kind / priority | Boundary; P1, preserves historical result without linking unavailable content |
| Environment | Integration/E2E; AI endpoint instrumented to prove no call |
| Preconditions | Alice has Accepted SUB-901. Run once with problem `{withdrawn:true}`, once with `problem=null`; AI endpoint returns timeout if ever called. |
| Test data | P-901 withdrawn record; null problem record; submitted ID SUB-901. |
| Steps | Open both results; inspect problem label/retry links and Accepted-only analysis/interview actions; activate navigation actions. |
| Expected | Withdrawn problem shows “withdrawn from bank” label and disables problem/retry links. `problem=null` shows no withdrawn label and likewise disables links. Accepted-only analysis/interview routes remain tied to SUB-901; result stays fully rendered. Screen performs no F5 request; AI timeout has no effect because no AI call is made. |
| Cleanup | Restore P-901 publication state and remove fixtures. |
| Evidence | UI/accessibility capture, route IDs, request trace and F5 job count. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0201-SRAC08-02 — Accepted result renders server-provided Beats at eligibility boundary

| Field | Value |
| :--- | :--- |
| AC / DD source | SR-AC-08; DD USR0201 §3 score/Beats rules and BR-JD-12; F4 AC-34 |
| Kind / priority | Boundary; P1, prevents client-side sample threshold drift |
| Environment | Integration with F4 GetSubmissionDetail fixture |
| Preconditions | Alice owns an Accepted submission for P-901/Python; comparison cohort has the exact minimum eligible sample count configured in F4. |
| Test data | F4 ACCEPTED response passedCount=12,totalCount=12,beatsSampleSize=10,beatsPercent=63; second eligible-verdict fixture with passedCount=12,totalCount=12,beatsSampleSize=9,beatsPercent=null. |
| Steps | Open the first result; then open the second result without changing client configuration. |
| Expected | First result shows 12/12 and Faster than 63%; second shows 12/12 and Not enough comparison data. Client renders server percentage without recomputing or a separate hard-coded threshold; integration fixtures obey ACCEPTED=all tests pass. |
| Cleanup | Remove result fixtures. |
| Evidence | Exact F4 payloads and rendered text/accessible output. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0201-SRAC08-03 — Wrong Answer keeps score count but suppresses Beats

| Field | Value |
| :--- | :--- |
| AC / DD source | SR-AC-08; DD USR0201 §3 score/Beats rules; F4 AC-34 |
| Kind / priority | Boundary; P1, verdict-specific score presentation |
| Environment | FE defensive DTO-adapter test for contradictory beatsPercent, followed by integration with valid F4 payload. |
| Preconditions | Alice owns terminal `Wrong Answer` submission SUB-WA. |
| Test data | FE-only malformed DTO: WRONG_ANSWER,passedCount=3,totalCount=12,scoreRatio=0.25,beatsPercent=88. Valid integration variant has beatsPercent=null. |
| Steps | Open SUB-WA and inspect banner, score and Beats. |
| Expected | Both render score3/12 and Beats dash, never Accepted-only text. Defensive invalid DTO is not seeded into F4 DB or treated as a valid server response. No duplicate scoreRatio card. |
| Cleanup | Delete SUB-WA fixture. |
| Evidence | Response payload and UI/accessibility capture. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0201-SRAC08-04 — System Error is not presented as a learner failure

| Field | Value |
| :--- | :--- |
| AC / DD source | SR-AC-08; DD USR0201 §3 `SYSTEM_ERROR` banner and score rules; F4 AC-34 |
| Kind / priority | Abnormal; P1, prevents blaming learner for infrastructure failure |
| Environment | Integration with F4 result fixture |
| Preconditions | Alice owns terminal `SYSTEM_ERROR` submission SUB-SE. |
| Test data | F4 response `verdict=SYSTEM_ERROR`, `passedCount=null`, `totalCount=12`, `scoreRatio=null`, `beatsPercent=null`. |
| Steps | Open SUB-SE; inspect banner, score, Beats, testcase section and toast/alert region. |
| Expected | Banner says “System Error”; score and Beats are `-`; no learner-facing failure toast or incorrect-answer label is shown. Testcase area follows its separately contracted NOT_APPLICABLE state. |
| Cleanup | Delete SUB-SE fixture. |
| Evidence | API payload, UI/accessibility capture and toast log. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |
