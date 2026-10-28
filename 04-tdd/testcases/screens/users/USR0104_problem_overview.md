# User screen cases — USR0104 problem overview

Scope: problem overview tabs, bookmark recovery and sample navigation. TDD: `04-tdd/screens/users/USR0104_problem_overview.md`; DD: `03-dd/screens/users/USR0104_problem_overview.md`, §§3,7. Module oracle for bookmark persistence is covered by F2/F1 contracts; no run evidence exists.

Fixture aliases, independent scenario reset, role assignments and mock/integration evidence follow [fixture conventions](../FIXTURE_CONVENTIONS.md). Per-case Test data overrides file baseline.

## Fixture catalog

- Alice (`learner.alice@example.test`) can view published `P-401` version `v3`, with sample `S1` and two secondary tabs; secondary content `C1`,`C2` each has an independent endpoint. Bookmark initially absent.
- FE mock covers tabs/layout only; API integration required for bookmark persistence and visibility rules.

## Coverage

| AC | Branch | Case | Review |
|---|---|---|---|
| AC-SC-01 | both secondary tabs fail independently | TC-USR0104-SCAC01-01 | Draft |
| AC-SC-02 | bookmark remove/undo persisted | TC-USR0104-SCAC02-01 | Draft |
| AC-SC-03 | route and copy sample | TC-USR0104-SCAC03-01 | Draft |

### TC-USR0104-SCAC01-01 — Statement remains usable when secondary tabs fail

| Field | Value |
|---|---|
| AC / DD source | AC-SC-01; DD USR0104 §§3,7 |
| Kind / priority | Abnormal; P1 |
| Environment | FE mock and integration |
| Preconditions | Alice opens P-401/v3; force C1 and C2 endpoints to return 503, separately and together. |
| Test data | Statement marker `P401_STATEMENT_V3`; sample S1; C1/C2 fault responses. |
| Steps | Load page; inspect statement and sample; retry each failed tab independently. |
| Expected | F4 history or F5 quick-insight failure hides only corresponding secondary card with no toast or retry card. Statement/examples/bookmark remain usable. F4 limits returns all three languages in one call without language parameter; no three duplicate limit queries. |
| Cleanup | Restore endpoints. |
| Evidence | UI capture and per-tab request trace. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0104-SCAC02-01 — Bookmark undo is persisted to server

| Field | Value |
|---|---|
| AC / DD source | AC-SC-02; DD USR0104 §§3,7; bookmark module contract |
| Kind / priority | Recovery; P1 |
| Environment | Integration with bookmark API/database |
| Preconditions | P-401 is already bookmarked; isolate healthy Undo and failed Undo runs. |
| Test data | Initial saved=true; unsave succeeds; Undo uses target saved=true; separate Undo request fails with 503. |
| Steps | Unsave then invoke Undo; inspect both F2 ?12 payloads and reload. Repeat with Undo failure. |
| Expected | Unsave sends saved=false and Undo sends saved=true to server. Success restores bookmark state, without promising the old deleted note. Failed Undo returns UI to most recent server baseline saved=false, shows error and never falsely claims restoration. |
| Cleanup | Delete any bookmark created by fixture. |
| Evidence | UI states, requests and DB row query. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0104-SCAC03-01 — Problem routes and sample copy action

| Field | Value |
|---|---|
| AC / DD source | AC-SC-03; DD USR0104 §§3,7 |
| Kind / priority | Normal; P2 |
| Environment | FE browser; clipboard permission enabled |
| Preconditions | P-401 overview loaded with sample S1. |
| Test data | Sample exact text `3\n1 2 3\n`; expected clipboard bytes identical. |
| Steps | Activate Workspace, Saved, problem list, latest submission and review links; copy Input and Output successfully, then deny Clipboard API. |
| Expected | Workspace uses /workspace/{problemId}, Saved /saved, list /problems; result uses actual submissionId and review uses F5 review.submissionId. Copy preserves exact text/newlines and only permitted examples; denied copy gives manual-copy guidance with source text intact. |
| Cleanup | Clear scratch field and clipboard. |
| Evidence | Destination URL and pasted byte/string comparison. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |
