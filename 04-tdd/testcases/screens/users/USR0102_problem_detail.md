# User screen cases — USR0102 problem detail / workspace

Scope: `04-tdd/screens/users/USR0102_problem_detail.md`. DD: `03-dd/screens/users/USR0102_problem_detail.md`; machine behavior references F4 AC-08, AC-73–76 and existing F2/F3/F4 module suites. All cases have reviewed designs and remain unrun; no run performed.

Fixture aliases, independent scenario reset, role assignments and mock/integration evidence follow [fixture conventions](../FIXTURE_CONVENTIONS.md). Per-case Test data overrides file baseline.

## Fixture catalog

- `A1`: authenticated learner Alice; problem `P-201` published, statement version `v7`; 12 grading cases: two Samples `S1`,`S2` and ten Hidden `H1`..`H10`; Alice has 0/60 run attempts this hour and an existing draft `D1` on Python 3.12 / function mode.
- `Draft` FE mock may validate editor/route presentation only. Integration requires workspace metadata, submission, judge and run APIs plus event stream; source drafts/history are local browser storage. Use deterministic judge controls: queued, final Accepted, and connection unavailable.

## Coverage map

| AC | Case | Branch | Review |
|---|---|---|---|
| PD-AC-01 | TC-USR0102-PDAC01-01 | auxiliary failure | Draft |
| PD-AC-02 | TC-USR0102-PDAC02-01 | language/mode, starter and draft restore | Draft |
| PD-AC-03 | TC-USR0102-PDAC03-01 | ordered submit and hidden-data privacy | Draft |
| PD-AC-04 | TC-USR0102-PDAC04-01 | reconnect/reopen while judging | Draft |
| PD-AC-05 | TC-USR0102-PDAC05-01 | total network loss and polling | Draft |
| PD-AC-06 | TC-USR0102-PDAC06-01 | completion/history/next-link gates | Draft |
| PD-AC-07 | TC-USR0102-PDAC07-01 | run sample-only/no persistent submission | Draft |
| PD-AC-08 | TC-USR0102-PDAC08-01 | isolated AI outage | Draft |
| PD-AC-09 | TC-USR0102-PDAC09-01 | optimistic save rollback | Draft |
| PD-AC-10 | TC-USR0102-PDAC10-01 | client/server validation | Draft |
| PD-AC-11 | TC-USR0102-PDAC11-01 | undo before AI apply | Draft |
| PD-AC-12 | TC-USR0102-PDAC12-01 | resize layout | Draft |
| PD-AC-13 | TC-USR0102-PDAC13-01 | run quota exhausted | Draft |

### TC-USR0102-PDAC01-01 — Auxiliary module outage does not block workspace

| Field | Value |
|---|---|
| AC / DD source | PD-AC-01; DD USR0102 sections 3,7, corresponding T-PD checks |
| Kind / priority | Abnormal; P1, editing/submission continuity |
| Environment | FE mock and integration |
| Preconditions | A1 opens P-201; inject failure independently for hints, discussion, and metadata panel. |
| Test data | P-201/v7, D1 source text `def solve(nums):\n    return sum(nums)`. |
| Steps | Load workspace with each auxiliary endpoint returning 503; edit code and submit in the integration run. |
| Expected | History failure shows an error/retry only in the submissions tab. Limits failure hides limits without substitute numbers. AI quick-insight failure hides Solution Review tab and both AI links without AI toast. Repeat all three failures together: statement, editor, starter and Run/Submit remain usable. Only primary statement failure replaces the whole workspace with error. |
| Cleanup | Reset endpoint faults and remove run record if generated. |
| Evidence | UI capture, request trace, submitted source hash. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0102-PDAC02-01 — Language/mode change selects matching starter and draft

| Field | Value |
|---|---|
| AC / DD source | PD-AC-02; DD USR0102 sections 3,7 |
| Kind / priority | Normal; P1, protects learner source across editor configuration changes |
| Environment | FE mock and draft API integration |
| Preconditions | A1 has D1 for Python 3.12/function mode and a separate Java 21/STDIO draft. |
| Test data | Python draft `def solve(nums): return sum(nums)`; Java draft reads stdin; configured starter snippets for each pair. |
| Steps | Switch Python/function ? Java/STDIO ? Python/function; change input method then model and information tab; open a STDIO-only problem with remembered FUNCTION_WRAPPER; deny localStorage writes and type. |
| Expected | Each exact language/model pair restores its own source or starter when no draft exists; changing model does not change input method, and tab changes do not change source. STDIO-only problem hides wrapper controls and forces STDIO. Failed localStorage writes leave editing usable and never show the browser-saved label. |
| Cleanup | Delete test drafts. |
| Evidence | Pair selection, editor source hash before/after, draft API records. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0102-PDAC03-01 — Submit uses ordered cases and does not expose hidden inputs

| Field | Value |
|---|---|
| AC / DD source | PD-AC-03; DD USR0102 sections 3,7; F4 AC-08, AC-73–76 |
| Kind / priority | Security; P0, hidden testcase confidentiality |
| Environment | Integration/E2E with judge and browser network/DOM inspection |
| Preconditions | A1 on P-201; judge fixture returns ordered case summaries for S1,S2,H1,H2. |
| Test data | Source `def solve(nums): return sum(nums)`; hidden marker strings `HIDDEN_INPUT_91` and `HIDDEN_EXPECTED_91`. |
| Steps | Submit once; inspect rendered text, accessibility tree, local storage, network responses and console after queued and final states. |
| Expected | PENDING?COMPILING?JUDGING labels appear, all 12 cells progress in position order and judging continues after failing hidden case 7. Language/model/input controls, editor, Run and Submit are locked while judging. Only permitted status/verdict appears for Hidden; input/expected/actual canaries never appear in DOM, accessible text, storage, logs or learner response payload. |
| Cleanup | Delete submission and judge fixture. |
| Evidence | Redacted API capture, DOM/accessibility scan, DB submission reference. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0102-PDAC04-01 — Reconnect and reopen a judging submission

| Field | Value |
|---|---|
| AC / DD source | PD-AC-04; DD USR0102 sections 3,7 |
| Kind / priority | Recovery; P1, avoids duplicate submission and stale result |
| Environment | Integration/E2E with event stream and controllable judge |
| Preconditions | A1 has submitted `SUB-401`; judge remains queued; event stream disconnects after first update. |
| Test data | One submission with 12 cases; first four complete before disconnect, another three complete during outage; isolated run finishes entirely while disconnected. |
| Steps | Disconnect stream; reconnect; close and reopen route while still queued; wait for terminal event. |
| Expected | Reconnect reconciles seven completed cells exactly once then continues; reopening while JUDGING follows the same submission and keeps Submit locked. If final during outage, reconnect shows terminal result immediately. No second submission or false failure is created. |
| Cleanup | Remove SUB-401 and event records. |
| Evidence | Event timeline, request IDs, submission count, UI captures. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0102-PDAC05-01 — Total network loss does not imply failure or discard source

| Field | Value |
|---|---|
| AC / DD source | PD-AC-05; DD USR0102 sections 3,7 |
| Kind / priority | Recovery; P1 |
| Environment | E2E with network fault proxy and clock capture |
| Preconditions | A1 submitted SUB-402; then block all API and event traffic. |
| Test data | Editor source contains unique marker `alice-local-402`; judge outcome unknown during outage. |
| Steps | Make realtime reconnect fail while status GET remains available; use fake clock to observe ten 3-second polling intervals; separately make status GET fail too, then restore network and reload. |
| Expected | Fallback polls status every 3 seconds at most 10 times and applies a terminal response if received. After ten unresolved polls show updates-are-slow text, no failure verdict; editor source stays byte-identical and controls remain locked while judging. Reload reconciles true server state and never automatically resubmits. |
| Cleanup | Restore network; delete fixture. |
| Evidence | Timestamped requests, UI state, local source hash and submission count. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0102-PDAC06-01 — Terminal result refreshes history and gates next action

| Field | Value |
|---|---|
| AC / DD source | PD-AC-06; DD USR0102 sections 3,7 |
| Kind / priority | Normal; P1 |
| Environment | Integration/E2E |
| Preconditions | A1 receives final Accepted for SUB-403; next-problem link exists only for eligible P-201. |
| Test data | Independent terminal runs WRONG_ANSWER, TIME_LIMIT_EXCEEDED and ACCEPTED; quick-insight healthy then unavailable; history initially omits current submission. |
| Steps | Deliver each final event; inspect toast, controls and three action links; click a history row in each run. |
| Expected | Each final result unlocks controls, refreshes history once and provides View Result for that submission. Accepted gives success toast; other terminal learner failures give error toast with passed count. Analysis and Interview links appear only for ACCEPTED with healthy AI quick-insight; history row opens its own result UUID. No next-problem target is inferred. |
| Cleanup | Delete submission fixture. |
| Evidence | Event/API and history responses; link URL. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0102-PDAC07-01 — Run Code uses samples only and creates no submission

| Field | Value |
|---|---|
| AC / DD source | PD-AC-07; DD USR0102 sections 3,7 |
| Kind / priority | Security; P1, distinguishes run from submit |
| Environment | Integration with run service, DB and logs |
| Preconditions | A1 has run quota and P-201 sample cases S1,S2 plus hidden H1,H2. |
| Test data | Source returns known outputs for S1,S2; hidden markers as above. |
| Steps | Run code that fails first Sample, then syntactically invalid code in an isolated run; inspect details, history, progress and diagnostic line. |
| Expected | Run shows exactly two Samples including input/expected/actual/time/memory and opens the first failed Sample. It creates no submission/history/progress change and no Hidden payload. Syntax errors are processed by compiler and return COMPILE_ERROR with learner line mapping; wrapper lines stay hidden. |
| Cleanup | Remove run artifacts if retention is not automatic. |
| Evidence | Run response, DB query, UI capture, redacted logs. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0102-PDAC08-01 — AI provider failure is isolated

| Field | Value |
|---|---|
| AC / DD source | PD-AC-08; DD USR0102 sections 3,7 |
| Kind / priority | Abnormal; P2 |
| Environment | FE mock and integration with AI provider stub |
| Preconditions | A1 editor loaded; AI endpoint returns timeout. |
| Test data | Source marker `keep-this-source`; timeout 30s. |
| Steps | Make AI quick-insight timeout or return budget unavailable; read statement, edit, run Samples and submit through ACCEPTED. |
| Expected | All non-AI actions work; Solution Review tab and analysis/interview links are absent, with no AI error toast or fabricated retry panel. Source and draft remain unchanged. |
| Cleanup | Clear AI fault and draft. |
| Evidence | UI capture, source hash, AI request trace. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0102-PDAC09-01 — Failed optimistic bookmark save rolls back

| Field | Value |
|---|---|
| AC / DD source | PD-AC-09; DD USR0102 sections 3,7 |
| Kind / priority | Recovery; P1 |
| Environment | Integration with bookmark API and DB |
| Preconditions | A1 has not saved P-201; bookmark POST returns 500. |
| Test data | P-201, initial `saved=false`. |
| Steps | Save successfully, then attempt unsave with endpoint failure; double-click Save in an isolated fresh run. |
| Expected | Successful save changes label immediately, offers View Saved and appears in saved list. Failed unsave restores the saved label and existing bookmark with one error toast; source remains unchanged. Repeated clicks create no duplicate bookmark. |
| Cleanup | Remove bookmark if created on retry. |
| Evidence | UI states, API response, DB query. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0102-PDAC10-01 — Client guard and authoritative server validation

| Field | Value |
|---|---|
| AC / DD source | PD-AC-10; DD USR0102 sections 3,7; submission module source AC as applicable |
| Kind / priority | Security; P0, browser checks must not replace server checks |
| Environment | Integration/API |
| Preconditions | A1 has invalid source for selected language; server fixture rejects submission with contract validation response. |
| Test data | Independent fixtures: whitespace-only source; UTF-8 source exactly 65536 bytes and 65537 bytes including Vietnamese text; Python upload wrong suffix .java; 65537-byte file; upload mode with no file. Existing valid file and source kept as baseline. |
| Steps | Attempt Run and Submit with each fixture; spy on FileReader and request count. Bypass one size guard with authorized direct request to exercise server validation; render its error in UI. |
| Expected | Empty/over-limit/no-file and wrong-extension input is blocked with one message and no request. Exactly 65536 bytes is sent; 65537 is blocked by byte count. Wrong-extension/oversized file is rejected before reading and preserves previous file/source. Server size rejection maps to its error code and preserves draft. Compiler syntax is not a client validation rule. |
| Cleanup | Remove any unexpected records and restore fixtures. |
| Evidence | UI validation, API response, DB/job absence query. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0102-PDAC11-01 — Undo restores original source before AI suggestion apply

| Field | Value |
|---|---|
| AC / DD source | PD-AC-11; DD USR0102 sections 3,7 |
| Kind / priority | Recovery; P1 |
| Environment | FE mock and draft integration |
| Preconditions | Editor contains source `ORIGINAL_11`; AI suggestion available as separate patch. |
| Test data | Original and suggested code have distinct SHA-256 hashes. |
| Steps | Apply AI suggestion with backup available, open workspace and Undo; repeat with localStorage backup write denied before applying. |
| Expected | Undo restores exact original source hash. If old-source backup cannot be saved, apply aborts on the report screen, retains original draft and never navigates/overwrites. No hidden auto-apply occurs. |
| Cleanup | Delete test draft. |
| Evidence | Before/after hashes, editor capture, draft record. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0102-PDAC12-01 — Resizing editor preserves usable layout

| Field | Value |
|---|---|
| AC / DD source | PD-AC-12; DD USR0102 sections 3,7 |
| Kind / priority | Boundary; P2, layout interaction |
| Environment | FE browser at 1280×800 and 390×844 viewports |
| Preconditions | P-201 open with statement and editor visible. |
| Test data | For the statement/editor split, min/max fractions are 22%/74%; for the editor/results split, 25%/85%. Fixture has both panes enabled and editor source `resize-marker-12`. |
| Steps | Drag divider through each position; tab through editor controls; reload. |
| Expected | Statement/editor and editor/results ratios clamp respectively to 22–74% and 25–85%; dragging to each edge never collapses either pane. Both ratios persist in browser localStorage and restore after reload; at 390×844 each pane remains usable without horizontal overlap. |
| Cleanup | Restore viewport and default divider. |
| Evidence | Before/after screenshots, measured pane ratios, localStorage values and keyboard traversal recording. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0102-PDAC13-01 — Exhausted run quota preserves editor text

| Field | Value |
|---|---|
| AC / DD source | PD-AC-13; DD USR0102 sections 3,7 |
| Kind / priority | Boundary; P1, prevents source loss at limit |
| Environment | Integration with quota service |
| Preconditions | A1 has used 60/60 run attempts in rolling 60 minutes. |
| Test data | Unsaved editor source marker `quota-boundary-13`. |
| Steps | Click Run Code at limit; inspect quota response; save draft and reload. |
| Expected | Run returns 429 JDG-107 and displays retryAfterSeconds; source stays byte-identical, no fabricated run or submission appears. At the deadline an explicit retry can run; Submit uses its separate quota. |
| Cleanup | Reset quota fixture and delete draft. |
| Evidence | UI capture, quota counters, source hash, run/submission DB queries. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |
