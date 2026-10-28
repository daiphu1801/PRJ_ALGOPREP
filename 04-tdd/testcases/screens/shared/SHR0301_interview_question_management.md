# QA test cases — shared interview question management (SHR0301)

Scope: `04-tdd/screens/shared/SHR0301_interview_question_management.md`; DD §§2–7, F6 §§9–24. Design only; not executed.

Fixture aliases, independent scenario reset, role assignments and mock/integration evidence follow [fixture conventions](../FIXTURE_CONVENTIONS.md). Per-case Test data overrides file baseline.

## Fixtures

A3 `admin-23` has `INTERVIEW_BANK_MANAGEMENT`; A2 `editor-22` lacks taxonomy permissions. Active rows `q1=11111111-1111-4111-8111-111111111111` (`IQ-041`), `q2=22222222-2222-4222-8222-222222222222` (`IQ-042`), `q3=33333333-3333-4333-8333-333333333333` (`IQ-043`); pageSize=8 with nine matching ACTIVE rows. `topic-used=aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa` referenced by 2 questions. CSV valid file: UTF-8 with F6 §24 header; invalid variants have wrong header or row 3 invalid. Server import response imports row 2 and skips row 3 with reason. Bulk delete response retires q1 and rejects q2 with `errorCode=INB-010` because q2 was concurrently retired.

## Coverage

| AC | Branch | Cases |
|---|---|---|
| IQM-AC-01 | filter/sort/page selection and UUID route | TC-SHR0301-IQMAC01-01, -02 |
| IQM-AC-02 | partial soft delete | TC-SHR0301-IQMAC02-01 |
| IQM-AC-03 | taxonomy permissions/reference bounds and CSV import/template; import permission denial | TC-SHR0301-IQMAC03-01..03 |

### TC-SHR0301-IQMAC01-01 — Filter and sort persist; select-all is page-local

| Field | Value |
|---|---|
| AC / DD source | IQM-AC-01; DD BR-SC-01, IQ-M-01/02 |
| Kind / priority | Normal, Boundary; P1 (wrong selection/route) |
| Environment | FE mock + F6 integration |
| Preconditions | A3 at /admin/interview-questions?q=hash&page=1&pageSize=8&sort=displayNo&dir=asc; nine ACTIVE matching rows q1..q9, page1 q1..q8 and page2 q9. |
| Test data | Rows and UUIDs in fixture catalog. |
| Steps | Select all first page, next page, select all; click q9 title then return. |
| Expected | URL/request use separate sort=displayNo,dir=asc,pageSize=8. Selection resets with page and contains only eight first-page or one second-page ID respectively. q9 link uses its UUID and does not toggle selection; Back restores filter context. |
| Cleanup | Clear selection/session. |
| Evidence | URL, requests, row UUIDs, checkbox state, navigation capture. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0301-IQMAC01-02 — Detail/edit routes use selected UUID

| Field | Value |
|---|---|
| AC / DD source | IQM-AC-01; DD IQ-M-03 |
| Kind / priority | Normal; P1 (wrong question mutation) |
| Environment | Browser + F6 integration |
| Preconditions | q2 selected and A3 has appropriate UPDATE. |
| Test data | UUID q2; display label IQ-042. |
| Steps | Open row detail; return; activate edit action. |
| Expected | Detail/edit routes each contain q2 UUID in correct admin base path. Selection survives link activation; no API path uses display label. |
| Cleanup | Close browser context. |
| Evidence | URLs and request logs. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0301-IQMAC02-01 — Partial bulk soft-delete keeps only failures selected

| Field | Value |
|---|---|
| AC / DD source | IQM-AC-02; DD partial-delete state, IQ-M-04; F6 §23 |
| Kind / priority | Abnormal, Recovery; P1 (false success/history loss) |
| Environment | Browser + F6 integration; mixed response fixture |
| Preconditions | A3 selects q1 and q2; server retires q1, rejects q2. |
| Test data | q1 ACTIVE, q2 already RETIRED by another admin after list fetch; request ids=[q1,q2]; response retiredIds=[q1], failedItems=[{id:q2,errorCode:INB-010}]. |
| Steps | Confirm bulk delete; inspect per-ID alert/selection/history; refresh and inspect retired q2 removal from active list. |
| Expected | HTTP 200 reports one retired and q2 INB-010 failure; success q1 unselected, failed q2 retained for explanation/reconciliation until refresh. q2 is already retired, so no retry success is promised by changing eligibility. Both histories remain; no whole-batch success or physical deletion. |
| Cleanup | Restore q1/q2 fixture lifecycle and remove audit artifacts. |
| Evidence | F6 request/response, visible alert, DB status and history rows, retry request. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0301-IQMAC03-01 — A2 taxonomy denied; referenced topic cannot be deleted

| Field | Value |
|---|---|
| AC / DD source | IQM-AC-03; DD BR-SC-02, IQ-M-05; F6 §§11–18 |
| Kind / priority | Security, Boundary; P1 (authorization/referential integrity) |
| Environment | Browser + F6 integration |
| Preconditions | A2 lacks management permission; `topic-used` is referenced by 2 questions. |
| Test data | `topic-used`; duplicate topic name `Algorithms`; server-owned generated code. |
| Steps | As A2 inspect taxonomy controls and submit direct mutation; as A3 attempt deleting referenced topic; then rename an unreferenced topic. |
| Expected | A2 controls absent; forged API denied without mutation. Referenced topic delete rejected, popup remains open and shows reference count. A3 rename updates displayName but server code stays unchanged. Dialog is keyboard closable and restores focus. |
| Cleanup | Restore renamed topic. |
| Evidence | Role claims, rendered controls, API responses, DB rows/audit. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0301-IQMAC03-02 — CSV validates template/header and displays server row outcomes

| Field | Value |
|---|---|
| AC / DD source | IQM-AC-03; DD IQ-M-06, BR-SC-02; F6 §24 |
| Kind / priority | Abnormal, Boundary; P1 (wrong import count or lost row detail) |
| Environment | FE mock + F6 integration |
| Preconditions | A3 opens CSV import; server template contract is F6 §24. |
| Test data | Valid UTF-8 CSV with valid row starting physical line 2 containing a quoted newline and duplicate question at physical line 4; separate wrong header. Server outcome importedCount=1, skippedRows=[{line:4,reasonCode:duplicate,detail:null}]. |
| Steps | Download template; inspect headers; upload wrong-header file; upload mixed valid file. |
| Expected | Template headers follow F6 ?24 mapping; wrong header blocks request with structural feedback. Client parses quoted newline and sends line=2/4 for records, server outcome shows importedCount=1 and physical line 4 duplicate exactly. Dialog stays open, no private content in logs and no invented counts. |
| Cleanup | Remove uploaded test file and imported question. |
| Evidence | Template bytes/header, request log, response and visible row errors. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0301-IQMAC03-03 - CSV import is denied without CREATE permission

| Field | Value |
|---|---|
| AC / DD source | IQM-AC-03; screen DD sections 3-5, BR-SC-02; F6 sections 2 and 24 (`POST /admin/questions/import`, `INB-301`) |
| Kind / priority | Security, Abnormal; P0 - prevents unauthorized question creation through direct API calls |
| Environment | Browser + F6 integration with role/permission fixtures, API audit and DB snapshot |
| Preconditions | Actor `editor-no-create` is authenticated and can open the screen but lacks `INTERVIEW_BANK_MANAGEMENT:CREATE`; `admin-23` has CREATE. No active question titled `CSV permission sentinel` exists. |
| Test data | Valid UTF-8 CSV using the exact F6 section 24 header and one row titled `CSV permission sentinel`; request payload matches the valid import schema. |
| Steps | As `editor-no-create`, inspect available import controls and attempt import through UI if exposed; independently send the valid request directly to F6 section 24. Then repeat once as authorized `admin-23` to prove fixture/file validity. |
| Expected | `editor-no-create` sees no Nhập CSV control; direct unauthorized request returns `403 INB-301`. No question row, rubric row, display-number allocation, `INTERVIEW_QUESTION_IMPORTED` audit event, or accepted import count is produced by the denied request. The authorized control request imports exactly one row and produces its corresponding question/audit event, establishing denial was permission-based. |
| Cleanup | Soft-delete authorized sentinel question and rubric; remove test upload artifacts; restore permission fixtures. |
| Evidence | Actor permission claims, rendered control state, request/response, DB before/after including display number, and audit event query. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |
