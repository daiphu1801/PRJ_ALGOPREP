# QA test cases — shared problem management (SHR0201)

Scope: `04-tdd/screens/shared/SHR0201_problem_management.md`; DD: `03-dd/screens/shared/SHR0201_problem_management.md`, §§2–7; F2 API §§15–35. Design only; no execution evidence exists.

Fixture aliases, independent scenario reset, role assignments and mock/integration evidence follow [fixture conventions](../FIXTURE_CONVENTIONS.md). Per-case Test data overrides file baseline.

## Fixtures

- A3 editor `usr-a3-10` has `PROBLEM_AUTHORING:READ/CREATE/UPDATE/DELETE`; A2 editor `usr-a2-20` has content read but no taxonomy management. All IDs below are UUID fixtures.
- `p1=11111111-1111-4111-8111-111111111111` UNPUBLISHED authored by A3; `p2=22222222-2222-4222-8222-222222222222` PUBLISHED; `p3=33333333-3333-4333-8333-333333333333` UNPUBLISHED after withdrawal; list fixture uses nine rows and pageSize=8, with remaining aliases mapped by fixture conventions.
- `topic-used=aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa` is referenced by 2 problems; `level-last=bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb` is the final level. Batch outcomes are controlled by server fixture responses.

## Coverage and cases

| AC | Branch | Cases |
|---|---|---|
| PM-AC-01 | filter/page/select/route/duplicate draft | TC-SHR0201-PMAC01-01, -02 |
| PM-AC-02 | partial batch response | TC-SHR0201-PMAC02-01 |
| PM-AC-03 | taxonomy authorization/deletion bounds/export | TC-SHR0201-PMAC03-01, -02 |

### TC-SHR0201-PMAC01-01 — URL filters, paging and page-local select

| Field | Value |
|---|---|
| AC / DD source | PM-AC-01; DD BR-SC-01, PM-01 |
| Kind / priority | Normal, Boundary; P1 (selection scope and navigation correctness) |
| Environment | FE mock + browser; list query response fixture |
| Preconditions | Signed in as usr-a3-10; /admin/problems?q=graph&page=1&pageSize=8&sort=updatedAt&dir=desc; matching nine nondeleted rows p1..p9; page1 p1..p8 and page2 p9. |
| Test data | Above fixtures; display labels `PRB-001` etc. deliberately differ from UUIDs. |
| Steps | Select all page1; navigate page2; select all; click p9 title and Back; change filter. |
| Expected | Request uses pageSize8, sort=updatedAt and dir=desc. Page1 selects exactly eight IDs; page change clears selection; page2 selects only p9. Link uses p9 UUID without toggling checkbox; Back restores context and filter change resets page1/selection. |
| Cleanup | Clear browser session and fixture selections. |
| Evidence | URL, query parameters, rendered row IDs, selection state before/after navigation, browser history capture. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0201-PMAC01-02 — Duplicate opens a draft without creating a problem

| Field | Value |
|---|---|
| AC / DD source | PM-AC-01; DD BR-SC-02, PM-02 |
| Kind / priority | Normal; P1 (unexpected write or wrong-record route) |
| Environment | Browser + F2 integration |
| Preconditions | `usr-a3-10` selects only `p2`; source detail returns UUID `p2`. |
| Test data | Source display code `PRB-042`; source is PUBLISHED and has author-owned content. |
| Steps | Click Duplicate; inspect URL/form; do not save. Then open the source row title. |
| Expected | Duplicate opens `/admin/problems/new?from=<p2 UUID>` with copied editable values, no POST/create record, no source ID/status/metrics copied as identity. Title opens detail by UUID; display code is never used as route key. |
| Cleanup | Discard unsaved form; verify record count unchanged. |
| Evidence | Browser URL, network log proving no create POST, before/after problem count. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0201-PMAC02-01 — Batch publishes/retires/deletes with per-ID result

| Field | Value |
|---|---|
| AC / DD source | PM-AC-02; DD partial-result state, PM-03; F2 §§29–33 |
| Kind / priority | Abnormal, Recovery; P1 (false success or lost failed work) |
| Environment | FE + F2 integration; deterministic mixed batch response |
| Preconditions | A3 selected IDs `p1,p2,p3`; operation uses exactly these UUIDs. Server succeeds for `p1,p2`, rejects `p3` with `PRB-054`. |
| Test data | Batch request IDs above; response includes success IDs `[p1,p2]`, failed `p3` and reason. |
| Steps | Run the selected bulk action; inspect summary and selection; retry only failed row after changing its server state to eligible. |
| Expected | UI reports 2 successes and identifies `p3` and reason; `p1,p2` are unselected, `p3` remains selected. No blanket-success toast; failed item is not shown mutated. Server DB/event state for successful IDs matches response only. Retry request contains only `p3`. |
| Cleanup | Restore statuses and remove test audit events if environment permits. |
| Evidence | Request/response, UI alert text, DB rows and relevant audit/event IDs. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0201-PMAC03-01 — A2 cannot manage taxonomy; referenced/final entries cannot be deleted

| Field | Value |
|---|---|
| AC / DD source | PM-AC-03; DD PM-04/05, BR-SC-02; F2 §§15–24 |
| Kind / priority | Security, Boundary; P1 (authorization and referential integrity) |
| Environment | Browser + F2 integration with A2/A3 sessions |
| Preconditions | `usr-a2-20` lacks taxonomy permission; `topic-used` has 2 references; `level-last` is the final level. |
| Test data | Topic `ALG`; level `L1`; `topic-used`, `level-last` fixtures. |
| Steps | As A2 inspect page and attempt direct taxonomy mutation; as A3 try deleting `topic-used` then `level-last`; finally rename topic display name. |
| Expected | A2 sees no taxonomy-management controls; forged mutation is 403 and changes no DB/event. A3 delete attempts are rejected with reference/final-level reason and modal remains open. Rename succeeds; generated `code` stays unchanged and list reflects server response. |
| Cleanup | Restore display name. |
| Evidence | Role/permission claims, controls, HTTP responses, DB rows, reference counts and audit events. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0201-PMAC03-02 — CSV exports all filtered records, not current page

| Field | Value |
|---|---|
| AC / DD source | PM-AC-03; DD PM-06, BR-SC-01 |
| Kind / priority | Boundary; P2 (incomplete export) |
| Environment | FE mock + F2 API integration |
| Preconditions | A3 query filters match 3 records; current page is 1 of 2. |
| Test data | Nine filter-matching nondeleted problems p1..p9 across two pages; unrelated p10 excluded; title starts =SUM(1,1) on one row; sensitive testcase/statement canaries. |
| Steps | Export current filtered result; parse downloaded CSV. |
| Expected | Filter export includes all nine matching display codes and excludes p10, independent of current page selection. UTF-8 BOM and fixed eight headers match F2 §28; CSV includes no UUID-only expectation, source/spec/testcase content or canary. Formula-leading text is apostrophe-prefixed; request carries filter only, not page IDs. |
| Cleanup | Delete downloaded artifact. |
| Evidence | Downloaded CSV hash/rows and request query. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |
