# User screen cases — USR0401 interview bank list

Scope: interview question discovery and quick practice. TDD: `04-tdd/screens/users/USR0401_interview_bank_list.md`; DD: `03-dd/screens/users/USR0401_interview_bank_list.md`, §§3,7. F6 is authoritative for question/attempt state. Cases are Draft.

Fixture aliases, independent scenario reset, role assignments and mock/integration evidence follow [fixture conventions](../FIXTURE_CONVENTIONS.md). Per-case Test data overrides file baseline.

## Fixtures

- Alice owns bookmark state for Q-401; published questions Q-401 (Array/Easy), Q-402 (Graph/Medium); topic catalog includes Arrays. Question list and topic/level catalogs independently faultable. Quick practice fixture has Q-401/Q-402.

## Coverage

| AC | Case | Branch | Review |
|---|---|---|---|
| IL-AC-01 | TC-USR0401-ILAC01-01 | partial catalog failure | Draft |
| IL-AC-02 | TC-USR0401-ILAC02-01 | selection/load-more context | Draft |
| IL-AC-03 | TC-USR0401-ILAC03-01 | bookmark/self-grade target | Draft |
| IL-AC-04 | TC-USR0401-ILAC04-01 | quick-practice navigation/exit | Draft |
| IL-AC-05 | TC-USR0401-ILAC05-01 | mock interview entry independence | Draft |

### TC-USR0401-ILAC01-01 — List and filters survive independent catalog failure

| Field | Value |
|---|---|
| AC / DD source | IL-AC-01; DD USR0401 §§3,7 |
| Kind / priority | Abnormal; P1 |
| Environment | FE mock and integration |
| Preconditions | Alice opens list; question list succeeds; level catalog fails 503; topic catalog succeeds. |
| Test data | Q-401/Q-402; level and topic endpoint fixtures. |
| Steps | Independently fail levels/topics; combine q, status, topic, level and bookmarked=1; change filter and load invalid query; open detail then return. |
| Expected | Healthy list/filters remain usable; filters combine and reset pagination. Invalid URL enums are cleaned. bookmarked=1 plus normalized q/status/topic/level survives detail and Back link; absent catalog gives no fabricated option. |
| Cleanup | Restore endpoints. |
| Evidence | UI and per-source request traces. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0401-ILAC02-01 — Selecting and loading more preserves list context

| Field | Value |
|---|---|
| AC / DD source | IL-AC-02; DD USR0401 §§3,7 |
| Kind / priority | Normal; P1 |
| Environment | FE and integration |
| Preconditions | Alice applies topic=Arrays, level=Easy; first page includes Q-401, next cursor exists. |
| Test data | Q-401 first page; Q-403 second page also matching. |
| Steps | Load next page with 503 then retry; select Q-401 then Q-403 rapidly and resolve Q-401 late; return INB-010 for selected Q-403; separately fail changed-filter query. |
| Expected | Page-next failure preserves loaded rows, retry appends correct next page once. Only latest selected question detail renders. Removed selected Q-403 leaves list and another available row is selected; changed-filter error retains previous rows without treating them as new results. |
| Cleanup | Restore URL/state. |
| Evidence | IDs, query params, URL and UI. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0401-ILAC03-01 — Bookmark and self-grade mutate only selected question

| Field | Value |
|---|---|
| AC / DD source | IL-AC-03; DD USR0401 §§3,7; F6 bookmark/attempt contracts |
| Kind / priority | Security; P1 |
| Environment | Integration |
| Preconditions | Q-401 selected; Q-402 visible but not selected. |
| Test data | Q-401 initially unbookmarked and UNSEEN; rating KNOWN; Q-402 unchanged; inject bookmark and rating failures independently. |
| Steps | Bookmark and rate Q-401; double-click controls; repeat with failures and while UNSEEN filter is active; reload. |
| Expected | Only Q-401 changes and review date uses server response. Failure rolls back exact prior state with error; rapid clicks do not send opposing writes. Just-rated Q-401 stays visible under UNSEEN until filter changes. Q-402 remains unchanged; KNOWN is API enum. |
| Cleanup | Remove bookmark/attempt fixture. |
| Evidence | API requests and DB records. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0401-ILAC04-01 — Quick practice advances and exits cleanly

| Field | Value |
|---|---|
| AC / DD source | IL-AC-04; DD USR0401 §§3,7 |
| Kind / priority | Normal; P1 |
| Environment | FE and integration |
| Preconditions | Quick practice set contains Q-401 then Q-402. |
| Test data | Current filter matches 12 questions; quick-practice returns at most 10; isolated empty/fetch-failure/rating-failure runs. |
| Steps | Start practice; rate cards, inject one rating failure; finish or exit midway. |
| Expected | Selected cards obey active filter and max=10; each rating advances, including failure with warning. Finish returns list; midway Exit returns without confirmation. Empty/fetch error makes no phantom practice completion or grade. |
| Cleanup | Delete practice session. |
| Evidence | Session/question order and UI captures. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0401-ILAC05-01 — Mock-interview entry works independently of bank lists

| Field | Value |
|---|---|
| AC / DD source | IL-AC-05; DD USR0401 §§3,7; F5 session contract |
| Kind / priority | Abnormal; P1 |
| Environment | FE mock and integration |
| Preconditions | Question catalog unavailable; interview service available. |
| Test data | Catalog 503; create-session response SES-401. |
| Steps | Activate mock interview entry from list shell. |
| Expected | Entry remains available and navigates to `/mock-interview`; it does not create a session or call F5 on the list screen. Failed catalog does not block the route or supply stale question data. |
| Cleanup | Close SES-401 and restore catalog. |
| Evidence | UI, route and session API. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |
