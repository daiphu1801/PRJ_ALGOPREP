# User screen cases — USR0301 solution review

Scope: AI solution analysis page and safe application of suggested code. TDD: `04-tdd/screens/users/USR0301_solution_review.md`; DD: `03-dd/screens/users/USR0301_solution_review.md`, §§3,7. F5 owns AI report generation/rate limits; these cases verify screen routing, state, and write safeguards. All Draft.

Fixture aliases, independent scenario reset, role assignments and mock/integration evidence follow [fixture conventions](../FIXTURE_CONVENTIONS.md). Per-case Test data overrides file baseline.

## Fixtures

- Alice owns Accepted submission `SUB-301` for P-301; P-302 is withdrawn. Report `R-301` references SUB-301 and has a suggested patch. Rate limit fixture returns retry-after 30 seconds. Workspace draft `W-301` initially contains exact source hash `H0`.

## Coverage

| AC | Case | Branch | Review |
|---|---|---|---|
| AC-SR-01 | TC-USR0301-SRAC01-01 | report navigation | Draft |
| AC-SR-02 | TC-USR0301-SRAC02-01–04 | no implicit generation; transient, budget and rate-limit error states | Draft |
| AC-SR-03 | TC-USR0301-SRAC03-01 | confirmed patch apply | Draft |
| AC-SR-04 | TC-USR0301-SRAC04-01 | backup/save failure safety | Draft |
| AC-SR-05 | TC-USR0301-SRAC05-01 | missing source and diff fidelity | Draft |
| AC-SR-06 | TC-USR0301-SRAC06-01 | rate limit countdown | Draft |
| AC-SR-07 | TC-USR0301-SRAC07-01 | withdrawn problem report | Draft |
| AC-SR-08 | TC-USR0301-SRAC08-01 | English labels | Draft |

### TC-USR0301-SRAC01-01 — Report links navigate to the correct related screens

| Field | Value |
|---|---|
| AC / DD source | AC-SR-01; DD USR0301 §§3,7 |
| Kind / priority | Normal; P1 |
| Environment | FE mock and integration |
| Preconditions | Alice opens R-301. |
| Test data | R-301, SUB-301, P-301 and workspace route. |
| Steps | Activate Result breadcrumb and submission breadcrumb; return to report then activate Interview about this submission. |
| Expected | Both breadcrumbs target the result for SUB-301; Interview targets /mock-interview?submissionId=SUB-301 and preselects it. No unrelated learner/submission identifier is sent. |
| Cleanup | Clear route state. |
| Evidence | Destination URLs and network responses. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0301-SRAC02-01 — Opening without a report does not silently generate one

| Field | Value |
|---|---|
| AC / DD source | AC-SR-02; DD USR0301 §§3,7 |
| Kind / priority | Security; P1, avoids unintended AI work/cost |
| Environment | Integration with F5 request/job instrumentation |
| Preconditions | Owned Accepted SUB-301 has no report/cache; initial F5 request returns transient provider failure. |
| Test data | AI call/rate counters initially zero; subsequent explicit retry succeeds. |
| Steps | Open report; switch window focus away/back and cycle offline/online without Retry; then click Retry once. |
| Expected | Page open makes exactly one initial AI/report request. Focus/reconnect makes zero additional requests/toasts/hour-quota charges. Explicit Retry sends exactly one new request and may produce report; no automatic retry or duplicate concurrent request. |
| Cleanup | Remove any unexpected report/job. |
| Evidence | UI capture, request/job logs and DB query. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0301-SRAC03-01 — Applying suggested code requires confirmation

| Field | Value |
|---|---|
| AC / DD source | AC-SR-03; DD USR0301 §§3,7 |
| Kind / priority | Security; P1 |
| Environment | FE browser and workspace integration |
| Preconditions | R-301 patch available; W-301 source hash H0. |
| Test data | Patch changes one function; expected resulting hash H1. |
| Steps | Choose apply; cancel confirmation; repeat and confirm. |
| Expected | Cancel preserves draft H0 and route. Confirm backs up old source, writes suggestion to the exact problem/language/model draft and opens its workspace with one success toast. No apply control appears when report has no suggested code. |
| Cleanup | Restore W-301 from fixture backup. |
| Evidence | Dialog capture, before/after hashes, workspace API record. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0301-SRAC04-01 — Backup or save failure never overwrites learner source

| Field | Value |
|---|---|
| AC / DD source | AC-SR-04; DD USR0301 §§3,7 |
| Kind / priority | Recovery; P0, source-loss prevention |
| Environment | Browser draft/history adapter fault injection; API integration only for fetching the source/report. |
| Preconditions | W-301 source H0; patch H1. |
| Test data | Independent local backup write failure (storage denied/quota exceeded) and saveWorkspaceDraft returning false. |
| Steps | Confirm patch apply in each fault condition; reload workspace. |
| Expected | Either failure keeps old local draft hash H0 intact and user on report page with save error; no workspace navigation or partial overwrite. Backup-before-apply is required; no backend draft row/API is invented. |
| Cleanup | Restore browser storage adapter and clear only this test namespace. |
| Evidence | Storage calls in order, H0/H1 hashes, final route and error feedback. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0301-SRAC05-01 — Missing submission and diff rows are represented honestly

| Field | Value |
|---|---|
| AC / DD source | AC-SR-05; DD USR0301 §§3,7 |
| Kind / priority | Boundary; P1 |
| Environment | FE mock and integration |
| Preconditions | R-301 report has no source snapshot; comparison has one insertion and one deletion. |
| Test data | A: report exists but submitted source fetch fails; B: cache-equivalent submissions differ only in trailing whitespace/CRLF versus LF. |
| Steps | Open report and inspect source/diff. |
| Expected | A retains report prose, hides submitted-source/diff blocks with explanation and disables Apply. B normalizes line endings/trailing spaces for comparison and creates no visually identical remove/add line pair; no synthetic diff rows. |
| Cleanup | Remove report fixture. |
| Evidence | UI capture and report payload. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0301-SRAC06-01 — Rate limit explains wait and blocks early retry

| Field | Value |
|---|---|
| AC / DD source | AC-SR-06; DD USR0301 §§3,7; F5 rate-limit contract |
| Kind / priority | Boundary; P1 |
| Environment | Integration with fixed clock |
| Preconditions | Generate action receives rate-limited response with retry-after 30s. |
| Test data | Clock T0; retry boundary T0+30s. |
| Steps | Observe message/countdown; attempt click before expiry; advance clock to boundary. |
| Expected | Reason and countdown shown; action disabled and no request before allowed time; enabled/request permitted at contract boundary. |
| Cleanup | Restore clock and rate-limit counter. |
| Evidence | UI timestamps and request count. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0301-SRAC07-01 — Report for withdrawn problem remains readable with status notice

| Field | Value |
|---|---|
| AC / DD source | AC-SR-07; DD USR0301 §§3,7 |
| Kind / priority | Boundary; P1 |
| Environment | Integration |
| Preconditions | Alice owns historical report R-301; P-301 becomes withdrawn. |
| Test data | R-301 unchanged; current problem state withdrawn. |
| Steps | Open R-301 from history. |
| Expected | Existing report remains accessible with explicit withdrawn status; unavailable problem content/action is not exposed. |
| Cleanup | Restore problem status. |
| Evidence | UI and API payloads. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0301-SRAC08-01 — English locale labels cover analysis flow

| Field | Value |
|---|---|
| AC / DD source | AC-SR-08; DD USR0301 §§3,7 |
| Kind / priority | Normal; P2, localization completeness |
| Environment | FE browser with English locale |
| Preconditions | Account locale English; R-301 report has all sections and rate-limit state. |
| Test data | English locale; report, loading, error and confirmation fixtures. |
| Steps | Inspect each report section, loading/error, apply dialog and rate-limit controls. |
| Expected | All visible labels/actions are nonempty English strings, with no untranslated keys or mixed-language fallback. |
| Cleanup | Restore locale. |
| Evidence | Screenshots and rendered-string export. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0301-SRAC02-02 — Transient report errors remain retryable and isolated

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-SR-02; DD USR0301 §3 transient-state row / BR-SC-01; F5 error mapping AIR-501 and AIR-502 |
| Kind / priority | Recovery; P1, report recovery without navigation or source loss |
| Environment | FE mock and integration with F5 error stubs, clock and toast capture |
| Preconditions | Alice opens R-301 route with a report request; separately simulate AIR-501 (503), AIR-502 (502), and transport timeout. |
| Test data | Existing report absent; each request fails once then returns report R-301 on explicit retry; report fetch count and toast count start at 0. |
| Steps | Observe each failure; activate Retry once; resolve successful R-301 response; inspect other screen regions and request counts. |
| Expected | Each transient failure shows NoticeTile retry and one toast for that failed attempt. Explicit retry shows loading then R-301 without changing route or starting a duplicate concurrent query. Navigation and non-AI shell remain usable; no report is represented as successful before response. |
| Cleanup | Restore F5 stub and clear toast/request state. |
| Evidence | UI captures, request sequence/count, toast count, resulting report ID. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0301-SRAC02-03 — Budget-locked report has no retry control

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-SR-02; DD USR0301 §3 budget-locked state; F5 error mapping AIR-401 |
| Kind / priority | Security; P1, prevents repeated generation while budget is locked |
| Environment | FE mock and integration with F5 budget/job instrumentation |
| Preconditions | Alice opens eligible SUB-301 review route; report does not exist; F5 returns AIR-401/429. |
| Test data | Used AI budget equals monthly cap; job count 0 before route open. |
| Steps | Open route; inspect locked NoticeTile, retry affordances, and F5 job count; follow exit link. |
| Expected | Red budget-locked NoticeTile and explanatory toast appear; no Retry control is rendered and page load creates only the expected initial request. Exit link remains usable. No additional job/request occurs without a new explicit user action after budget eligibility. |
| Cleanup | Restore budget and remove any report/job fixture. |
| Evidence | UI/accessibility capture, F5 request/job count, budget state. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0301-SRAC02-04 — Rate-limited report retry is disabled until countdown expires

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-SR-02; DD USR0301 §3 rate-limit state / BR-SC-02; F5 AIR-402 and AIR-404 mapping; DEC-2026-1024-dd-screens-ai-topic-length-and-rd-trim |
| Kind / priority | Boundary; P1, rate-limit retry boundary |
| Environment | FE mock/integration with fake clock and F5 stub |
| Preconditions | F5 request returns AIR-402 with `retryAfterSeconds=5`; a second run returns AIR-404 without the field. |
| Test data | Fake clock starts T0; AIR-402 retry deadline T0+5s; DD fallback countdown 20s for missing retryAfterSeconds. |
| Steps | Try Retry immediately and at T0+4.999s; advance to T0+5s and retry. Repeat AIR-404, advance to T0+19.999s and T0+20s. |
| Expected | Notice explains rate limit and countdown. Retry is disabled and sends no request before deadline; it enables at the deadline and one explicit click sends one request. Missing `retryAfterSeconds` uses 20s per DD. Countdown is not announced continuously through `aria-live`. |
| Cleanup | Restore clock and F5 rate-limit fixture. |
| Evidence | Timestamped button state, request count, accessible tree and response details. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |
