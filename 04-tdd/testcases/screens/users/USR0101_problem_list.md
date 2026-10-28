# User screen cases — USR0101 problem list

Scope: `04-tdd/screens/users/USR0101_problem_list.md`; UI routing/filter behavior only. Server search semantics remain covered by F2 cases. DD: `03-dd/screens/users/USR0101_problem_list.md` §§3,7. Execution: all cases are **Draft / Chưa thực hiện**.

Fixture aliases, independent scenario reset, role assignments and mock/integration evidence follow [fixture conventions](../FIXTURE_CONVENTIONS.md). Per-case Test data overrides file baseline.

## Shared fixtures

- `U1`: learner `learner.alice@example.test`, authenticated, UTC+7.
- `PSET`: visible problems `P-101` (Easy, Array, Published), `P-102` (Medium, Graph, Published), `P-103` (Hard, Array, Published); withdrawn `P-104` is not linkable. Alice has an in-progress draft for P-102 and a saved bookmark for P-101.
- FE mock can prove rendering and URL state only. API/integration run requires the problem-list and auxiliary endpoints; do not infer server authorization from mock.

## Coverage

| AC | Branch | Case | Review |
|---|---|---|---|
| PL-AC-01 | independent auxiliary failure/retry; primary list failure | TC-USR0101-PLAC01-01, -02 | Draft |
| PL-AC-02 | cumulative filters, sort/page/back; invalid URL and reset | TC-USR0101-PLAC02-01, -02 | Draft |
| PL-AC-03 | filtered selection, empty set, fetch error | TC-USR0101-PLAC03-01, -02 | Draft |
| PL-AC-04 | auxiliary routes; withdrawn/in-progress absence | TC-USR0101-PLAC04-01, -02 | Draft |

### TC-USR0101-PLAC01-01 — Auxiliary blocks fail independently and retry

| Field | Value |
|---|---|
| AC / DD source | PL-AC-01; DD §§3,7 checks T-PL-01–06, T-PL-20 |
| Kind / priority | Abnormal; P1, primary discovery remains usable during partial outage |
| Environment | FE mock, then integration with list + auxiliary endpoints |
| Preconditions | Sign in as U1; configure problem list success with PSET; in-progress, analysis-label and class-assignment endpoints each independently return 503. |
| Test data | PSET; saved P-101; draft P-102; 503 responses for each auxiliary source. |
| Steps | Inject each auxiliary failure in an isolated run; open /problems, inspect the corresponding block and network/toasts. Separately fail the primary list and retry it after recovery. |
| Expected | In-progress F4 failure hides the entire in-progress block; class-assignment failure hides that block; F5 label failure removes only analysis labels. These auxiliary failures show no error toast or per-block retry control (DD ?3/BR-SC-01). The table, title and navigation remain usable. Only primary-list failure shows a list error/retry; retry reissues the list request only. |
| Cleanup | Restore endpoints and clear mock state. |
| Evidence | Screen captures; request log proving isolated retries; response trace. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0101-PLAC02-01 — Filters, sort, pagination and back navigation preserve URL state

| Field | Value |
|---|---|
| AC / DD source | PL-AC-02; DD §§3,7 checks T-PL-07–14 |
| Kind / priority | Normal; P1, core list state and return path |
| Environment | FE mock; integration for canonical server result set |
| Preconditions | U1 on a list with at least 3 pages; URL initially `/problems`. |
| Test data | At least 41 matching rows; q=graph, difficulty=MEDIUM from the loaded catalog, sort=level and dir=asc then desc; page=2. Isolated invalid URL: status=abc, sort=xyz, page=-4. |
| Steps | Search, apply difficulty, set sort, move to page 2; open P-102 and navigate back; load a URL with invalid page/filter values; clear filters. |
| Expected | Search waits 400 ms (Enter flushes immediately), caps q at 100 characters; filters combine and reset page=1 without reloading auxiliary queries. Column sort sends level/acRate with asc/desc, never updated_desc. page changes use push; other filter changes replace. Back restores previous filters/page; no assertion forbids a freshness refetch. Invalid URL values are cleaned/defaulted; clear removes filter parameters. |
| Cleanup | Restore `/problems`; clear browser history fixture. |
| Evidence | Before/after URLs, visible selected controls, network log for list reload behavior. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0101-PLAC03-01 — Random problem respects active filters

| Field | Value |
|---|---|
| AC / DD source | PL-AC-03; DD §§3,7 checks T-PL-16–17 |
| Kind / priority | Normal; P2, convenience navigation constrained by current filter |
| Environment | FE mock and integration |
| Preconditions | U1 has active `difficulty=Medium`, `topic=Graph`; list has eligible records. |
| Test data | Only P-102 matches; P-101 and P-103 are nonmatches. |
| Steps | Click Random; repeat with no matching record; repeat with random endpoint failure. |
| Expected | Success navigates to P-102 only. Empty eligible set disables the action. Fetch failure shows an error and retains the list and filters. |
| Cleanup | Restore default filter and endpoint. |
| Evidence | Selected problem ID, URL, disabled state/error capture, request/response. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0101-PLAC04-01 — Auxiliary cards route to the right destination

| Field | Value |
|---|---|
| AC / DD source | PL-AC-04; DD §§3,7 checks T-PL-18–22 |
| Kind / priority | Normal; P1, prevents stale or incorrect destinations |
| Environment | FE mock; integration for current card data |
| Preconditions | U1 has saved P-101 and draft P-102; withdrawn P-104 is absent from current source response. |
| Test data | Saved entry action, in-progress P-102, class assignment P-103 with classId C-101; isolated all-empty and problem=null/withdrawn in-progress variants. |
| Steps | Follow Saved, draft card, class task and See all in-progress links; inspect all-empty and stale metadata variants. |
| Expected | Saved routes /saved without filters; in-progress P-102 to /workspace/P-102 and See all to /workspace; class task P-103 to /problems/P-103. Empty in-progress hides block/title/See all; null/withdrawn metadata never yields invalid link. Class grouping uses classId and max3 server groups; no invented x/y subtitle. |
| Cleanup | Clear fixture state. |
| Evidence | Destination URLs, rendered blocks, captured stale-card handling. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0101-PLAC01-02 — Empty list, primary failure and retry are distinct

| Field | Value |
| :--- | :--- |
| AC / DD source | PL-AC-01; DD USR0101 §§3,7, checks T-PL-01–06 and T-PL-20 |
| Kind / priority | Abnormal; P1, avoids presenting an outage as no available work |
| Environment | FE mock and integration with problem-list API |
| Preconditions | Alice opens `/problems`; auxiliary blocks return success. Run once with successful empty list, once with primary list returning 503, then restore service. |
| Test data | Empty response `{items: [], total: 0}`; failure response 503; recovery returns P-101 and P-102. |
| Steps | Load empty response; reload under 503; activate list retry after service recovery. |
| Expected | Empty success shows the list empty state without retry affordance. 503 shows primary list error, not empty state, while title/navigation remain. Retry re-requests primary list and renders P-101/P-102; no auxiliary request or fabricated card is produced by retry. |
| Cleanup | Restore endpoints and default list fixture. |
| Evidence | Screen captures, request counts/URLs, and response bodies. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |
