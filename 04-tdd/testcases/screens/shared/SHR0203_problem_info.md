# QA test cases — shared problem info (SHR0203)

Scope: `04-tdd/screens/shared/SHR0203_problem_info.md`; DD §§2–7 and F2 §34. Design only; not executed.

Fixture aliases, independent scenario reset, role assignments and mock/integration evidence follow [fixture conventions](../FIXTURE_CONVENTIONS.md). Per-case Test data overrides file baseline.

## Fixtures

Actors: author A2 `author-21`, unrelated editor A2 `editor-22`, A3 `admin-23`. IDs: published `p=11111111-1111-4111-8111-111111111111`, foreign draft `d=22222222-2222-4222-8222-222222222222`, missing syntactically valid UUID `m=33333333-3333-4333-8333-333333333333`. Published DTO includes Markdown `**Two Sum**`, readOnly public summary `{total:8, public:5, hidden:3}`, and no reference solution/testcase bodies for non-owner. Separate owner DTO variants have `spec=null` and `referenceSolution=null`. Network fault modes are explicit 403 and 503.

## Coverage

| AC | Branch | Cases |
|---|---|---|
| PI-AC-01 | owner/admin, other editor public, foreign draft/missing | TC-SHR0203-PIAC01-01, -02, -03 |
| PI-AC-02 | absent spec/solution and absent-source metrics | TC-SHR0203-PIAC02-01 |
| PI-AC-03 | route/actions, permission, 404 vs network/retry | TC-SHR0203-PIAC03-01, -02 |

### TC-SHR0203-PIAC01-01 — Owner and admin see permitted saved detail

| Field | Value |
|---|---|
| AC / DD source | PI-AC-01; DD BR-SC-03, PI-01 |
| Kind / priority | Normal; P1 (detail integrity) |
| Environment | FE mock + F2 integration |
| Preconditions | Login as `author-21`, then `admin-23`; published `p` belongs to author. |
| Test data | DTO uses `problemId=p`, `displayCode=PRB-042`, Markdown `**Two Sum**`, spec present, solution present; no Stack field. |
| Steps | Open `/instructor/problems/<p>` as author; repeat `/admin/problems/<p>` as admin. |
| Expected | F2 called with UUID `p`. H1 and Markdown render expected text safely; status and cards match DTO. Owner/admin permitted solution controls reflect their real authorization. No Stack, fabricated pass count, AC percentage, learner progress or submission count appears. |
| Cleanup | Clear auth/session. |
| Evidence | Request URL, DTO fixture, rendered accessibility tree and screenshot. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0203-PIAC01-02 — Other editor reads published problem as read-only

| Field | Value |
|---|---|
| AC / DD source | PI-AC-01; DD BR-SC-01/02, PI-02 |
| Kind / priority | Security; P0 (hidden grading data exposure) |
| Environment | Browser + F2 integration |
| Preconditions | Login `editor-22`; published `p` belongs to someone else. |
| Test data | Read-only response has summary only and no testcase bodies, solution code, or solution URL. |
| Steps | Open detail; inspect visible text, links, DOM/accessibility tree and network payload; try direct edit/preview routes. |
| Expected | Read-only presentation; no edit or preview controls, no hidden testcase content or reference solution in DOM/payload. Summary values shown only from supplied summary; no derived coverage card. Direct routes remain server-protected. |
| Cleanup | Clear session. |
| Evidence | Response payload, DOM snapshot, route responses and screenshot. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0203-PIAC01-03 — Foreign draft and missing UUID share not-found state

| Field | Value |
|---|---|
| AC / DD source | PI-AC-01; DD state table, PI-03 |
| Kind / priority | Security; P1 (resource enumeration) |
| Environment | FE + F2 integration |
| Preconditions | Login `editor-22`; `d` is another user's DRAFT; `m` does not exist. |
| Test data | Same error contract `404 PRB-010` for each identifier. |
| Steps | Open `/instructor/problems/<d>` then `/instructor/problems/<m>`. |
| Expected | Both show indistinguishable not-found copy and list link; neither reveals title, owner, status or which identifier exists. |
| Cleanup | Retain isolated fixtures. |
| Evidence | Both responses and UI captures compared for status/body equivalence. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0203-PIAC02-01 — Missing source fields render empty states without invented values

| Field | Value |
|---|---|
| AC / DD source | PI-AC-02; DD state table, PI-04/07, BR-SC-03 |
| Kind / priority | Boundary; P1 (false grading/limit claims) |
| Environment | FE mock; owner/A3 detail DTO |
| Preconditions | Owner detail query returns successfully. |
| Test data | Variant A `spec=null`, `referenceSolution` present; variant B inverse; `checklist.sampleSolutionAllPass=null`, omit Stack and learner metrics. |
| Steps | Render both variants and inspect all cards, text, and accessible names. |
| Expected | Missing section has explicit “not available” empty state, page remains usable. No invented run, pass ratio, Stack, AC rate or learner statistic. No runtime exception. |
| Cleanup | None; isolated fixtures. |
| Evidence | Rendered DOM/accessibility tree and console output. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0203-PIAC03-01 — Navigation preserves UUID and correct actor area

| Field | Value |
|---|---|
| AC / DD source | PI-AC-03; DD BR-SC-02, PI-05 |
| Kind / priority | Normal; P1 (cross-area or wrong-record navigation) |
| Environment | Browser; instructor and admin routes |
| Preconditions | Author-owned detail `p` loaded at instructor route; actor has update permission. |
| Test data | UUID `p`; display code `PRB-042`; instructor base path `/instructor/problems`. |
| Steps | Click Edit; return, click Preview; activate preview tab and return to detail. |
| Expected | Edit URL uses UUID under instructor path. Preview opens a new tab with `noopener noreferrer`, UUID is unchanged, and data reflects saved F2 version. No requests to learner-progress or sample-run endpoint. |
| Cleanup | Close tabs. |
| Evidence | URLs, link attributes, request log and browser screenshots. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0203-PIAC03-02 — Forbidden, missing and network errors stay distinct; retry keeps route

| Field | Value |
|---|---|
| AC / DD source | PI-AC-03; DD state table, PI-06 |
| Kind / priority | Abnormal, Recovery; P1 (misleading error and route loss) |
| Environment | FE + F2 integration with fault injection |
| Preconditions | Route `/admin/problems/<p>`; first request returns 403 `PRB-002`, second returns 503, third succeeds. |
| Test data | Keep URL at UUID `p`; response variants above. |
| Steps | Load 403; reload under injected 503; press Retry with successful response. |
| Expected | 403 shows unauthorized without title/content; 503 shows recoverable error rather than not-found. Retry reissues only detail query and does not alter URL/base path. Success renders correct `p`. |
| Cleanup | Remove fault rules. |
| Evidence | HTTP responses, route before/after, request counts and UI captures. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |
