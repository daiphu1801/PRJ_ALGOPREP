# QA test cases — shared problem authoring (SHR0202)

Scope: `04-tdd/screens/shared/SHR0202_problem_authoring.md`; DD §§2–7, F2 §§35–49, F3 §3.7 and F2 validation. Draft design; not executed.

Fixture aliases, independent scenario reset, role assignments and mock/integration evidence follow [fixture conventions](../FIXTURE_CONVENTIONS.md). Per-case Test data overrides file baseline.

## Fixtures

A3 author `admin-23`; source published problem UUID `11111111-1111-4111-8111-111111111111`; new problem create returns `22222222-2222-4222-8222-222222222222`. Topic `aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa`, level `bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb`. Draft testcase row UUID `cccccccc-cccc-4ccc-8ccc-cccccccccccc`, status AI draft; approved baseline has 8 cases including 2 Sample. Problem has two examples. F2 save can inject 503 or version conflict; F3 preview can inject stale response and wrapperUnsupported; runner can return actual verdict or unavailable. Upload pair fixtures `case01.in`/`case01.out`; invalid fixtures missing mate and duplicate `.in`.

## Coverage

| AC | Branch | Cases |
|---|---|---|
| PA-AC-01 | edit/duplicate/preview and signature preview | TC-SHR0202-PAAC01-01, -02 |
| PA-AC-02 | testcases, upload, AI/runner result, threshold; partial paired-upload retry | TC-SHR0202-PAAC02-01..03 |
| PA-AC-03 | step save/publish failure, dirty/recovery, advisory checklist/server denial, no Stack | TC-SHR0202-PAAC03-01..03 |
| PA-AC-04 | per-row visibility/review/reorder/keyboard and server rejection | TC-SHR0202-PAAC04-01 |
| PA-AC-05 | epsilon/validation/version conflict/context privacy | TC-SHR0202-PAAC05-01 |

### TC-SHR0202-PAAC01-01 — Duplicate and preview use draft without an early write

| Field | Value |
|---|---|
| AC / DD source | PA-AC-01; DD BR-SC-03, PA-01/02 |
| Kind / priority | Normal; P1 (unexpected record creation) |
| Environment | Browser + F2 request spy |
| Preconditions | A3 views source detail and selects Duplicate. |
| Test data | Source UUID above; draft title `Copied graph task`; description includes Markdown and HTML payload. |
| Steps | Duplicate; edit title/example; open preview; switch Java/C++/Python tabs; close preview without save. |
| Expected | Duplicate form carries editable source content with no persisted identity and no POST. Preview uses latest unsaved title/spec/examples, sanitizes markup and returns three language signatures; language-tab switches reuse response without writes. Below-two-worked-examples state explains required minimum before publish. |
| Cleanup | Discard draft. |
| Evidence | Network log, route, field snapshots, preview DOM. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0202-PAAC01-02 — Signature preview handles unsupported/stale replies without saving

| Field | Value |
|---|---|
| AC / DD source | PA-AC-01; DD BR-SC-05, PA-11; F3 §3.7 |
| Kind / priority | Abnormal, Recovery; P1 (incorrect generated signature) |
| Environment | FE mock + F3 integration |
| Preconditions | Spec tab open; debounce 400 ms; preview API returns all three language outputs. |
| Test data | First body function `solve(int n)`, then newer body `solve(long n)`; first response delayed; second `wrapperSupported=false`. |
| Steps | Enter first signature; before reply enter second; switch language tab; inspect both replies. |
| Expected | Latest body alone determines displayed output; stale reply ignored. Language tab changes visible line without a new request. Unsupported result shows server violation/reason, no fabricated signature or saved spec. |
| Cleanup | Clear draft and F3 stub. |
| Evidence | Request timestamps/bodies, rendered code per tab, save-call count. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0202-PAAC02-01 — Testcase pair validation, AI drafts and real run results

| Field | Value |
|---|---|
| AC / DD source | PA-AC-02; DD BR-SC-02/04, PA-03/09; F2 §§42–49 |
| Kind / priority | Boundary, Abnormal; P1 (false accepted testcase or data loss) |
| Environment | FE + F2/F4 integration with upload and runner |
| Preconditions | Draft problem form; upload endpoints available; AI returns unapproved draft. |
| Test data | Valid pair case01.in/out; invalid missing mate and duplicate case01.in; AI testcase approved=false; runner actual verdict Accepted then service unavailable. |
| Steps | Submit invalid file selections; submit valid pair; generate AI testcase; attempt sample run; separately inject runner failure. |
| Expected | Invalid pairs rejected before signed upload URL and no mutation. Valid upload confirms only after both files succeed. AI draft remains unapproved. Run shows actual F4 verdict only after completion; unavailable/error never displays Pass or changes approved state. |
| Cleanup | Delete uploaded blobs and testcase fixtures. |
| Evidence | Upload requests, F2 pair response, approval status, runner job/result and visible state. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0202-PAAC02-02 — Published problem threshold protects the last eligible testcase/example

| Field | Value |
|---|---|
| AC / DD source | PA-AC-02; DD BR-SC-02, PA-10; F2 §§42–45 |
| Kind / priority | Boundary; P1 (published grading contract change) |
| Environment | FE + F2 integration |
| Preconditions | Published problem with exactly 8 approved testcases and 2 Sample examples. |
| Test data | Approved sample row; approved non-sample row. |
| Steps | Toggle last Sample to Hidden; attempt delete last eligible testcase; then attempt non-threshold edit. |
| Expected | Server `PRB-054` on changes violating 2-Sample/8-approved thresholds; UI restores exact server baseline and reason. Permitted edit remains scoped to target row. No optimistic local deletion survives rejection. |
| Cleanup | Restore original approved set. |
| Evidence | Requests/responses, before/after order/status and published version. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0202-PAAC03-01 — Partial saves and failed publish retain published baseline

| Field | Value |
|---|---|
| AC / DD source | PA-AC-03; DD BR-SC-01, PA-06 |
| Kind / priority | Abnormal, Recovery; P1 (partial publish) |
| Environment | FE + F2 integration with injected publish validation error |
| Preconditions | Existing draft has valid content but invalid spec; content save succeeds; publish returns validation error. |
| Test data | Draft content title Updated graph task; missing required function signature. |
| Steps | Save content; attempt publish; correct spec; retry publish; edit while save response is delayed. |
| Expected | Successful content save remains. Failed publish leaves UNPUBLISHED (draft label), names unmet checklist and emits no publish success/event. Corrected publish only changes to PUBLISHED after response. Old save response never overwrites newer typed content; unsaved dirty navigation offers Stay and preserves form, reload guard works where DD specifies. |
| Cleanup | Restore original draft/status. |
| Evidence | Ordered requests/responses, DB version/status, visible validation and field values. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0202-PAAC03-02 — Stack is absent from problem editor and detail

| Field | Value |
|---|---|
| AC / DD source | PA-AC-03; DD PA-08, decision `DEC-2026-1025-dd-admin-language-display-and-stack` |
| Kind / priority | Boundary; P2 (unsupported contract field) |
| Environment | Browser FE mock + detail view |
| Preconditions | Open new/edit problem and saved problem detail. |
| Test data | DTO intentionally has no stack property. |
| Steps | Inspect form controls, payload, detail cards and accessible tree. |
| Expected | No Stack (MB) control or displayed value; no invented default or outgoing `stack_limit_mb`. |
| Cleanup | None. |
| Evidence | DOM snapshots and request payload. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0202-PAAC04-01 — Per-row visibility, review, reorder and rejection preserve server order

| Field | Value |
|---|---|
| AC / DD source | PA-AC-04; DD BR-SC-02, PA-04 |
| Kind / priority | Normal, Abnormal, Boundary; P1 (hidden-case leakage/order corruption) |
| Environment | Browser + F2 integration; mouse and keyboard |
| Preconditions | Three rows ordered t1,t2,t3; t2 contains hidden input; draft AI row is unapproved. |
| Test data | UUIDs t1/t2/t3; server reorder failure returns canonical t1,t2,t3. |
| Steps | Toggle t2 visibility; inspect learner preview; approve AI draft; reorder via drag then keyboard Up; inject reorder rejection. |
| Expected | Toggle applies only target row on successful save; learner view never shows Hidden content. On published problem, actual reorder and explicit approve advance grading snapshot version per F2; no-op reorder leaves version unchanged. Drag/keyboard persist same order. Rejection restores canonical order/baseline; version does not advance on failed write, and below-two-Sample toggle remains old. |
| Cleanup | Restore row statuses/order. |
| Evidence | API payloads, student preview DOM, order before/after, keyboard/focus trace. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0202-PAAC05-01 — Spec validation, epsilon pairing and version conflict

| Field | Value |
|---|---|
| AC / DD source | PA-AC-05; DD client validation, PA-05; F2 §38 |
| Kind / priority | Boundary, Concurrency, Security; P1 (invalid spec or lost concurrent edit) |
| Environment | FE + F2 integration with two editor sessions |
| Preconditions | Published problem opened at version 7 in sessions A and B. |
| Test data | epsilon 0.01; exact strategy `EPSILON` vs `EXACT`; valid spec change; AI context contains private note `internal rubric`. |
| Steps | In A enter epsilon with EXACT and save; verify rejection. Change to EPSILON, save. In B save an older version after A; inspect public learner preview. |
| Expected | Epsilon rejected unless strategy EPSILON; rejected payload creates no write. Valid update advances server version. B receives version conflict, retains local draft and does not overwrite A. AI context is absent from learner/public response. |
| Cleanup | Restore saved version and private context. |
| Evidence | Both request payloads/responses, DB version/values, public response/DOM. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0202-PAAC02-03 - One failed upload retains the testcase pair for retry

| Field | Value |
|---|---|
| AC / DD source | PA-AC-02; DD BR-SC-04, PA-03; F2 API 46 and 47, DD BR-SC-04 and PA-03 |
| Kind / priority | Recovery, Boundary; P1 - prevents orphaned or incomplete testcase pairs |
| Environment | Browser + F2 integration with object-store signed-URL stub and controllable network |
| Preconditions | Draft problem; no testcase pair with key `retry01` exists. F2 #46 has just issued signed PUT URLs with their 15-minute validity window. The retry is performed before expiry. |
| Test data | `retry01.in` bytes `3\n1 2\n`; `retry01.out` bytes `3\n3\n`; first input upload succeeds, paired output upload returns an injected 503; retry the failed PUT using its still-valid issued URL and both pair legs are confirmed successfully. |
| Steps | Select the pair and start upload; allow input PUT success and output PUT failure; inspect selection, F2 confirmation calls, records and blobs. Retry the retained pair using the still-valid signed URL(s) before their 15-minute expiry; inspect #47 confirmation and final persisted pair. |
| Expected | Pair validation passes and signed URLs are requested for both files. After one leg fails, selection and filenames remain available for retry; no F2 batch-confirmation call or visible saved testcase occurs. Retry completes both legs and confirms the pair exactly once with matching base key `retry01`; final record references both files. No orphaned confirmed half-pair exists. |
| Cleanup | Delete pair record and both test blobs; clear upload stubs and draft. |
| Evidence | Signed URL requests, PUT outcomes, confirmation count/payload, testcase rows and object keys before/after retry. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0202-PAAC03-03 - Advisory checklist cannot bypass server publish validation

| Field | Value |
|---|---|
| AC / DD source | PA-AC-03; DD BR-SC-01, PA-06; F2 API 40 sections 40.2-40.5; `PRB-030` checklist validation |
| Kind / priority | Security, Abnormal; P1 - prevents publishing an incomplete grading contract |
| Environment | Browser + F2 integration; controllable checklist state and publish response |
| Preconditions | Existing problem is `DRAFT`, version 12; saved content is valid but one required testcase is absent. UI checklist is stale and displays complete. |
| Test data | Problem UUID `55555555-5555-4555-8555-555555555555`; `expectedUpdatedAt` equals current server baseline; saved title `Grid paths`; checklist omits the minimum approved testcase condition. |
| Steps | Open draft with stale-complete checklist; activate Publish; inspect the #40 request/response, status, checklist refresh, toast and public learner query. Add the missing approved testcase, refresh checklist, then retry with current `expectedUpdatedAt`. |
| Expected | Because the client believes checklist complete, it sends one #40 request. Server returns `422 PRB-030` and `details.items` includes the unmet minimum-approved-testcase condition (`met=false`); persisted status remains `UNPUBLISHED` (draft in UI), fields and `updatedAt` stay unchanged, and no publish audit/event or success state appears. Learner/public query does not expose the draft. After adding the requirement, refreshing checklist and retrying with current `expectedUpdatedAt`, success changes status to `PUBLISHED` exactly once. |
| Cleanup | Restore original draft status/content/version and remove any test publish event. |
| Evidence | Checklist state, ordered publish requests/responses, server status/version, event log and learner-facing query. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |
