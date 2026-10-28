# User screen cases — USR0302 mock interview

Scope: interview session entry, lifecycle and recovery. TDD: `04-tdd/screens/users/USR0302_mock_interview.md`; DD: `03-dd/screens/users/USR0302_mock_interview.md`, §§3,7; F5 session contract is server oracle. All Draft.

Fixture aliases, independent scenario reset, role assignments and mock/integration evidence follow [fixture conventions](../FIXTURE_CONVENTIONS.md). Per-case Test data overrides file baseline.

## Fixtures

- Alice owns Accepted `SUB-302`; F1 settings SENIOR, 16 turns, hints disabled. Interview bank question `Q-302`; topic `Arrays`; existing closed session `SES-OLD`. AI provider and network can be faulted deterministically.

## Coverage

| AC | Case | Branch | Review |
|---|---|---|---|
| AC-MI-01 | TC-USR0302-MIAC01-01 | submission source selection | Draft |
| AC-MI-02 | TC-USR0302-MIAC02-01 | defaults | Draft |
| AC-MI-03 | TC-USR0302-MIAC03-01 | independent bank/topic tabs | Draft |
| AC-MI-04 | TC-USR0302-MIAC04-01 | reopen/repractice/closed | Draft |
| AC-MI-05 | TC-USR0302-MIAC05-01 | no total-turn count | Draft |
| AC-MI-06 | TC-USR0302-MIAC06-01 | cancel before first turn | Draft |
| AC-MI-07 | TC-USR0302-MIAC07-01 | network loss idempotency/draft | Draft |

### TC-USR0302-MIAC01-01 — Submission entry selects only the intended source

| Field | Value |
|---|---|
| AC / DD source | AC-MI-01; DD USR0302 §§3,7 |
| Kind / priority | Normal; P1 |
| Environment | FE and integration |
| Preconditions | Alice opens entry from SUB-302 result. |
| Test data | Own Accepted SUB-302 outside recent-page result; recent own SUB-303 and foreign Bob SUB-304. |
| Steps | Open /mock-interview?submissionId=SUB-302 from result/report, inspect preselection then Start. Separately open menu without submissionId, inspect recent list/empty variant and choose SUB-303. |
| Expected | Entry via submissionId selects SUB-302 in Accepted tab even outside recent list and enables Start. Direct menu entry lists only own Accepted records with problem title/language, no internal display IDs; empty own list shows empty state. Session source is selected exact submission only. |
| Cleanup | Close/delete created session. |
| Evidence | UI, session API payload and DB association. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0302-MIAC02-01 — Self-practice initializes from saved defaults

| Field | Value |
|---|---|
| AC / DD source | AC-MI-02; DD USR0302 §§3,7 |
| Kind / priority | Normal; P2 |
| Environment | Integration |
| Preconditions | Alice defaults English/20 minutes; enter self-practice. |
| Test data | F1 #14 settings: preferredInterviewLevel=SENIOR, preferredMaxTurns=16, preferredHintAllowed=false; separate unset-settings run. |
| Steps | Open Topic tab; inspect defaults, change to JUNIOR/8/hints=true and start; reload settings; inspect Accepted tab. |
| Expected | Initial controls use SENIOR/16/false. Local changes apply to this session only, never write F1 settings. Unset defaults are MIDDLE/12/true. Accepted-submission tab hides these three controls; no language/duration setting is invented. |
| Cleanup | Close session. |
| Evidence | UI and session creation payload. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0302-MIAC03-01 — Bank and topic entry remain independent during partial failure

| Field | Value |
|---|---|
| AC / DD source | AC-MI-03; DD USR0302 §§3,7 |
| Kind / priority | Abnormal; P1 |
| Environment | FE mock and integration |
| Preconditions | Bank endpoint returns 503; topic endpoint returns Arrays successfully. |
| Test data | Q-302 bank question; 11 selectable topics; Accepted-list endpoint fails while other two sources are healthy; active question total=37. |
| Steps | Start from bank question without submission; independently choose zero, 10 and 11 topics; retry Accepted-list failure and use View all 37 questions. |
| Expected | Bank session uses Q-302 without submission. Zero topics blocks Start; 10 allowed and 11th blocked with limit message. Accepted-list failure is local and retryable while bank/topic tabs work; View all opens bank list and shows active total=37. |
| Cleanup | Restore endpoint and close session. |
| Evidence | UI capture and requests. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0302-MIAC04-01 — Reopen old result, repeat practice, and respect closed state

| Field | Value |
|---|---|
| AC / DD source | AC-MI-04; DD USR0302 §§3,7 |
| Kind / priority | Normal; P1 |
| Environment | Integration |
| Preconditions | SES-OLD is completed/closed and belongs to Alice. |
| Test data | P1 completed/scored; P2 abandoned/closed below minimum; separate session closed by job while answer is being typed, both enough-turn and below-minimum variants. |
| Steps | Open P1; Practice again then reload; open P2; submit after each job-closure variant. |
| Expected | P1 shows read-only score with learning-feedback disclaimer and no just-finished toast. Practice again returns to source setup, without creating a session until Start or reopening old session on reload. P2 and insufficient closed session return setup with abandoned notice, no score; sufficiently completed job closure displays score. |
| Cleanup | Delete new session. |
| Evidence | Session IDs/statuses and transcript captures. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0302-MIAC05-01 — Submission-based interview omits total-turn count

| Field | Value |
|---|---|
| AC / DD source | AC-MI-05; DD USR0302 §§3,7 |
| Kind / priority | Boundary; P2 |
| Environment | FE and integration |
| Preconditions | Start interview sourced from SUB-302. |
| Test data | Submission-based session payload with no total-turn field. |
| Steps | Inspect setup and active session at first and second turns. |
| Expected | No total-turn number/progress denominator is displayed or inferred; turn sequence itself remains visible. |
| Cleanup | Close session. |
| Evidence | UI and payload field inspection. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0302-MIAC06-01 — Cancelling before first answer closes without transcript turns

| Field | Value |
|---|---|
| AC / DD source | AC-MI-06; DD USR0302 §§3,7 |
| Kind / priority | Recovery; P1 |
| Environment | Integration |
| Preconditions | New session opened; confirmation popup shown; no turn submitted. |
| Test data | Independent active sessions with 2 answered turns and 0 answered turns. |
| Steps | Cancel popup; inspect status/list; repeat and confirm end early. |
| Expected | Cancel early-end dialog preserves session and sends no scoring request. Confirm with 2 turns scores and displays result. Zero-turn dialog states nothing to score; confirm returns setup with no score screen, error or empty answer. |
| Cleanup | Remove SES-CANCEL. |
| Evidence | UI, session state and turn/job query. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0302-MIAC07-01 — Network loss during answer does not duplicate turn or lose draft

| Field | Value |
|---|---|
| AC / DD source | AC-MI-07; DD USR0302 §§3,7 |
| Kind / priority | Recovery; P0, learner response integrity |
| Environment | E2E with network fault proxy |
| Preconditions | Alice active in SES-NEW after one completed turn. |
| Test data | Answer marker answer-unique-302; request turn 2; isolated disconnect before acknowledgement, disconnect during AI stream, and provider-stream failure. |
| Steps | Inject each fault independently; observe before-ack failure; reconnect while AI replying; delay stream beyond 60s; invoke Retry this turn after provider error. |
| Expected | Before acknowledgement, answer returns to input with not-sent notice and no automatic repost. Midstream reconnect reconciles complete AI answer without duplicate learner message. Beyond 60s shows slow notice, not false failure. Retry this turn retains one learner answer and increments turn once only on completed AI reply; no unsupported idempotency-key contract is assumed. |
| Cleanup | Delete session and turns. |
| Evidence | Request keys, turn/AI-job count, draft hash and event timeline. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |
