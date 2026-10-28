# User screen cases — USR0202 my submissions

Scope: learner submission history list. TDD: `04-tdd/screens/users/USR0202_my_submissions.md`; DD: `03-dd/screens/users/USR0202_my_submissions.md`, §§3,7. Judge result semantics are covered by F4; this file checks list state, routing and gating.

Fixture aliases, independent scenario reset, role assignments and mock/integration evidence follow [fixture conventions](../FIXTURE_CONVENTIONS.md). Per-case Test data overrides file baseline.

## Fixtures

- Alice owns `SUB-201` Accepted for published P-201, `SUB-202` Queued for P-202, `SUB-203` Wrong Answer for P-203. Bob owns `SUB-B`; it must never appear to Alice. List has 3 pages; use deterministic timestamps and UTC+7.

## Coverage

| AC | Branch | Case | Review |
|---|---|---|---|
| MY-AC-01 | loading, populated, empty | TC-USR0202-MYAC01-01 | Draft |
| MY-AC-02 | search/filter/page/back | TC-USR0202-MYAC02-01 | Draft |
| MY-AC-03 | result link and conditional problem link | TC-USR0202-MYAC03-01 | Draft |
| MY-AC-04 | Accepted-only analysis | TC-USR0202-MYAC04-01 | Draft |

### TC-USR0202-MYAC01-01 — Loading, populated and empty states

| Field | Value |
|---|---|
| AC / DD source | MY-AC-01; DD USR0202 §§3,7 |
| Kind / priority | Normal; P1 |
| Environment | FE mock and API integration |
| Preconditions | Alice authenticated; submissions endpoint returns fixture rows. |
| Test data | SUB-201/202/203 plus empty response; Bob's SUB-B. |
| Steps | Load list slowly; inspect loading; resolve with rows; reload with empty list. |
| Expected | Loading has no empty message; queued/compiling/judging records show status without fabricated score. Empty success links to /problems; only Alice rows show and error state is distinct. |
| Cleanup | Remove submissions. |
| Evidence | UI states and API response. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0202-MYAC02-01 — Search, filters, pagination and back restore list context

| Field | Value |
|---|---|
| AC / DD source | MY-AC-02; DD USR0202 §§3,7 |
| Kind / priority | Normal; P1 |
| Environment | FE mock and integration |
| Preconditions | Alice has at least 3 pages of submissions. |
| Test data | Alice has 45 submissions; query `q=Lee`, `verdict=ACCEPTED`, `language=PYTHON`, `page=2`; SUB-202 is an Accepted Python result on page 2. |
| Steps | Apply q/Accepted/Python, page2, open result then Back; load invalid enum/page URL and inspect cleanup; after known rows change filter and fail request; Retry then Clear. |
| Expected | q/verdict/language combine in server request and URL; change resets page1, Back restores old filter/page. Invalid values are removed/defaulted. Failed refresh keeps last rows/filter controls and retry for new key; Clear removes filters/page. |
| Cleanup | Restore default URL. |
| Evidence | URLs, rendered controls, list request parameters. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0202-MYAC03-01 — Result link always opens owned submission; problem link follows availability

| Field | Value |
|---|---|
| AC / DD source | MY-AC-03; DD USR0202 §§3,7 |
| Kind / priority | Security; P1, ownership and stale problem routing |
| Environment | Integration |
| Preconditions | Alice owns SUB-201/202/203; P-203 withdrawn; Bob owns SUB-B. |
| Test data | Stable submission IDs and problem availability states. |
| Steps | Follow each result link; inspect problem links; alter URL to Bob's submission. |
| Expected | Every own result link opens its corresponding result. Problem link appears only when target remains available. Bob's ID is denied without data disclosure. |
| Cleanup | Delete fixtures. |
| Evidence | URLs, API status/body, ownership query. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0202-MYAC04-01 — Analysis action is available only for Accepted submission

| Field | Value |
|---|---|
| AC / DD source | MY-AC-04; DD USR0202 §§3,7 |
| Kind / priority | Security; P1, prevents invalid analysis entry |
| Environment | Integration |
| Preconditions | Alice list contains Accepted SUB-201 and non-Accepted SUB-202/203. |
| Test data | Own Accepted SUB-201 for withdrawn P-201; Queued SUB-202 and WrongAnswer SUB-203; AI unavailable. |
| Steps | Inspect and activate analysis action on each row; attempt direct analysis route for SUB-203. |
| Expected | Only Accepted analysis link is enabled, including withdrawn problem. Other rows explain unavailable by text/accessible tooltip. Direct ineligible navigation cannot create an AI report/job; it may make an eligibility-check request that is rejected. AI failure never breaks history list. |
| Cleanup | Remove any unexpected analysis task. |
| Evidence | UI capture, API/job log. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |
