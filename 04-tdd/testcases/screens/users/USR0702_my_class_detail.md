# User screen cases — USR0702 my class detail

Scope: learner class detail, assignment progress and withdrawn problem handling. TDD: `04-tdd/screens/users/USR0702_my_class_detail.md`; DD: `03-dd/screens/users/USR0702_my_class_detail.md`, §§3,7. F1/F2 own membership and assignment data. Draft only.

Fixture aliases, independent scenario reset, role assignments and mock/integration evidence follow [fixture conventions](../FIXTURE_CONVENTIONS.md). Per-case Test data overrides file baseline.

## Fixtures

- Alice active in C-702; assignments A-1 (P-701 published, completed by Alice) and A-2 (P-702 published, not started). P-703 withdrawn assignment remains historical. F1 class endpoint and F2 problem endpoint independently faultable.

## Coverage

| AC | Case | Review |
|---|---|---|
| AC-SC-01 | TC-USR0702-SCAC01-01, -02 | Draft |
| AC-SC-02 | TC-USR0702-SCAC02-01 | Draft |
| AC-SC-03 | TC-USR0702-SCAC03-01 | Draft |

### TC-USR0702-SCAC01-01 — Progress matches assignment rows

| Field | Value |
|---|---|
| AC / DD source | AC-SC-01; DD USR0702 §§3,7 |
| Kind / priority | Data integrity; P1 |
| Environment | Integration with F1/F2 data |
| Preconditions | Alice opens C-702; assignment list and progress summary loaded. |
| Test data | F2 returns exactly three rows: A-1 `SOLVED, withdrawn=false`; A-2 `UNSOLVED, withdrawn=false`; A-3 `SOLVED, withdrawn=true`; a former A-4 row was removed before response. `totalCount=3`, `solvedCount=2`. |
| Steps | Compare summary counts/percent with visible assignment table; refresh. |
| Expected | Progress is exactly 2/3: denominator counts every returned row including withdrawn A-3; numerator counts both `SOLVED` rows including A-3. Removed A-4 is absent from both counts. Withdrawn row remains in table with no problem link. Refresh does not double-count. |
| Cleanup | Restore assignments/progress. |
| Evidence | UI values, source API rows, calculation. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0702-SCAC02-01 — F1 and F2 failures stay within their own regions

| Field | Value |
|---|---|
| AC / DD source | AC-SC-02; DD USR0702 §§3,7 |
| Kind / priority | Abnormal; P1 |
| Environment | FE mock and integration |
| Preconditions | Run once with class/membership F1 endpoint failing; once with problem F2 endpoint failing. |
| Test data | Independent F1 404 IDT-411, F1 503, F2 404 PRB-015, F2 503, malformed UUID; populated previous cached class. |
| Steps | Open each variant; retry failing region, first failing again then recovering; inspect cache/data removal and request counts. |
| Expected | Malformed UUID and either 404 share non-disclosing not-found with prior class data cleared. F1 503 gives whole-page error. F2 503 keeps class metadata and local progress/table error. Retry calls only failed source and toast occurs only on repeated failure; no stale/duplicate rows. |
| Cleanup | Restore endpoints. |
| Evidence | UI captures and request traces. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0702-SCAC03-01 — Withdrawn assignment has no problem link

| Field | Value |
|---|---|
| AC / DD source | AC-SC-03; DD USR0702 §§3,7; F2 visibility contract |
| Kind / priority | Security; P1 |
| Environment | Integration |
| Preconditions | C-702 historical assignment references P-703, withdrawn. |
| Test data | P-703 withdrawn; A-3 remains historical; learner not otherwise authorized to view it. |
| Steps | Open C-702; inspect assignment row and attempt direct target navigation. |
| Expected | Withdrawn P-703 remains a text row with its withdrawn label and counts in `totalCount`, but has no link, link role or keyboard focus target. Direct unauthorized `/problems/P-703` navigation does not expose the statement. |
| Cleanup | Restore publication and assignment fixtures. |
| Evidence | UI, route/API response and access log. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0702-SCAC01-02 — Empty assignment set renders 0/0 without division error

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-SC-01; DD USR0702 §§3, BR-SC-02, and check SC-02 |
| Kind / priority | Boundary; P1, progress calculation for an empty set |
| Environment | FE mock and integration against F1 #25B / F2 #13A |
| Preconditions | Alice is an authorized member of C-702; F1 returns class metadata; F2 returns no assigned rows. |
| Test data | `items=[]`, `totalCount=0`, `solvedCount=0`; no stale cached assignments. |
| Steps | Open class detail; inspect progress text/bar/table; refresh and inspect again. |
| Expected | Progress displays 0/0 and the DD empty progress state; no division-by-zero, NaN, Infinity or misleading 0% chart; class metadata remains visible and table uses its empty state. Reload does not restore stale rows. |
| Cleanup | Clear cache and restore class fixture. |
| Evidence | UI capture, F1/F2 payloads and console scan. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |
