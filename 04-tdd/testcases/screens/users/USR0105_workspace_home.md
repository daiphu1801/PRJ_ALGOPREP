# User screen cases — USR0105 workspace home

Scope: learner workspace home. TDD: `04-tdd/screens/users/USR0105_workspace_home.md`; DD: `03-dd/screens/users/USR0105_workspace_home.md`, §§3,7. Execution status: Draft / Chưa thực hiện.

Fixture aliases, independent scenario reset, role assignments and mock/integration evidence follow [fixture conventions](../FIXTURE_CONVENTIONS.md). Per-case Test data overrides file baseline.

## Fixtures

- Alice has in-progress P-501 submission and P-502 draft; P-503 is withdrawn. Workspace list endpoint is controllable for success, empty, and 503 responses.

## Coverage

| AC | Branch | Case | Review |
|---|---|---|---|
| AC-SC-01 | in-progress list and valid routing | TC-USR0105-SCAC01-01 | Draft |
| AC-SC-02 | empty vs error | TC-USR0105-SCAC02-01 | Draft |

### TC-USR0105-SCAC01-01 — In-progress items route to the matching workspace

| Field | Value |
|---|---|
| AC / DD source | AC-SC-01; DD USR0105 §§3,7 |
| Kind / priority | Normal; P1 |
| Environment | FE mock and integration |
| Preconditions | Alice authenticated; list contains P-501/P-502 and excludes P-503. |
| Test data | F4 contains 14 in-progress entries; returned first 12 include one problem=null and active P-501/P-502; no browser-only draft is invented as a server record. |
| Steps | Open home; activate each card; attempt stale P-503 URL/card payload. |
| Expected | F4 #8 request uses limit=12; no more than 12 valid cards render; null metadata has no card/link. Catalog action routes /problems and valid card /workspace/{problemId}; each ID is retained and no stale statement is exposed. |
| Cleanup | Clear drafts and restore list. |
| Evidence | Card capture, URLs, API responses. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0105-SCAC02-01 — Empty and failed workspace list are distinct

| Field | Value |
|---|---|
| AC / DD source | AC-SC-02; DD USR0105 §§3,7 |
| Kind / priority | Abnormal; P1 |
| Environment | FE mock and API integration |
| Preconditions | Alice opens home. |
| Test data | Empty success response `items=[]`; separate 503 response; recovery success with P-502. |
| Steps | Load with empty response; reload with 503; retry after restoring service. |
| Expected | Empty success shows empty state; 503 shows retryable error, not empty state. Retry requests again and renders P-502 on success. |
| Cleanup | Restore endpoint. |
| Evidence | UI captures and request/response log. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |
