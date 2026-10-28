# User screen cases — USR0402 interview question detail

Scope: detail navigation, answer confirmation and history. TDD: `04-tdd/screens/users/USR0402_interview_question_detail.md`; DD: `03-dd/screens/users/USR0402_interview_question_detail.md`, §§3,7. F6 owns attempt persistence. All Draft.

Fixture aliases, independent scenario reset, role assignments and mock/integration evidence follow [fixture conventions](../FIXTURE_CONVENTIONS.md). Per-case Test data overrides file baseline.

## Fixture catalog

- Alice opens Q-402 with history Q-402 attempts dated 2026-10-20 and 2026-10-22; Q-403 has separate history. Q-404 is RETIRED. Submission endpoint supports success, validation error, and timeout.

## Coverage

| AC | Branch | Case | Review |
|---|---|---|---|
| AC-SC-01 | switch questions/history paging | TC-USR0402-SCAC01-01 | Draft |
| AC-SC-02 | confirmation/success/error | TC-USR0402-SCAC02-01 | Draft |

### TC-USR0402-SCAC01-01 — Question switch isolates data and history pagination

| Field | Value |
|---|---|
| AC / DD source | AC-SC-01; DD USR0402 §§3,7 |
| Kind / priority | Security; P1, prevents cross-question data mixing |
| Environment | FE and integration |
| Preconditions | Alice navigates A=Q-402 to B=Q-403 while A GET is delayed; B has attemptNo 1..12. |
| Test data | First B history page attemptNo 12..3, next page 2..1; A marker two pointers, B marker graph; unsent draft never persisted. |
| Steps | Resolve B then late A; activate B Show more; inspect before and merged rows. |
| Expected | Only B detail/history render; Show more sends before=3 for B and appends 2,1 in descending order without duplicate/A attempts. totalAttempts remains 12 across pages. Local unsent text is not an attempt. |
| Cleanup | Clear drafts/history fixtures. |
| Evidence | Request IDs, rendered IDs and pagination URLs. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0402-SCAC02-01 — Submission requires confirmation and errors preserve answer

| Field | Value |
|---|---|
| AC / DD source | AC-SC-02; DD USR0402 §§3,7; F6 attempt contract |
| Kind / priority | Recovery; P1 |
| Environment | FE mock and integration |
| Preconditions | Alice edits answer for Q-402; no pending attempt. |
| Test data | Answer Use a queue.; independent responses 201 COMPLETED, 201 FAILED/INB-501, 429 INB-042; dropped response after committed attempt for transport run. |
| Steps | Cancel confirmation and inspect focus; confirm each response variant independently. For dropped response load history before offering a new explicit submission. |
| Expected | Cancel sends no request and restores opener focus. COMPLETED adds one attempt; 201 FAILED adds the saved failed attempt with feedback error and no invented feedback. 429 adds no attempt and preserves answer. Transport uncertainty triggers history reconciliation with no automatic POST retry; F6 POST has no idempotency key, so do not promise server deduplication of a fresh manual submission. |
| Cleanup | Delete attempts/draft. |
| Evidence | Dialog/focus capture, POST and history GET order, attempt IDs/count and feedback state. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |
