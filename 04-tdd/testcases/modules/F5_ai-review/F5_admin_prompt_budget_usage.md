# F5 admin — prompt, budget và usage (AC-38–48)

## Phạm vi và nguồn

- TDD: `04-tdd/modules/F5_ai-review.md`, AC-38–48.
- DD: `03-dd/api/F5_ai-review.md` §§14–31; `03-dd/logic/F5_ai-review.md` BR-AI-04/05; `03-dd/validation/F5_ai-review.md` §§7–9; `03-dd/jobs/F5_ai-review.md` §5; TDD trace AC-38–48.
- Trạng thái toàn bộ: chưa thực thi.

## Fixture dùng chung

- `adm_cfg`: ADMIN + AI_CONFIG:READ/UPDATE. `adm_budget`: ADMIN + AI_TOKEN_BUDGET:READ/UPDATE. `adm_readonly`: ADMIN + AI_TOKEN_BUDGET:READ only. `stu_a`, `stu_c`: STUDENT.
- Allowed models exactly `gpt-test-a`, `gpt-test-b` (case-sensitive). Prompt features: SOLUTION_REVIEW, MOCK_INTERVIEW, GENERATE_TESTCASE; each seeded with one ACTIVE row and stable UUID. Guard object for first two has exactly `citeLines`, `answerInVietnamese`, `noFullSolution`; GENERATE_TESTCASE uses null.
- Budget singleton `id=1`; timezone UTC+7; fixed clock `2026-10-27T10:00:00+07:00`, unless test sets another. Usage token totals below mean `tokens_prompt + tokens_completion`.
- Integration environment uses PostgreSQL/API; Redis and LLM stub are available except where explicitly faulted. No case is run as part of this design.

## Branch inventory / Coverage

| AC | Branch inventory | Case IDs | Review |
|---|---|---|---|
| 38 | Normal config view; budget-only security denial | TC-F5-AC38-01..02 | Draft |
| 39 | Repeated draft/history; invalid model; blank instruction; guard false | TC-F5-AC39-01..03 | Draft |
| 40 | Invalid multi-feature atomicity; valid publish; stale editor; no-op | TC-F5-AC40-01..04 | Draft |
| 41 | 7-day forecast/all feature allocation; unconfigured; config-only read split | TC-F5-AC41-01..03 | Draft |
| 42 | Cross limit; new-operation gates; admin exemption; unrelated judging; lowered limit | TC-F5-AC42-01..02 | Draft |
| 43 | Unlock blocked at/over limit; raise then unlock; invalid/auth; unlocked no-op | TC-F5-AC43-01..04 | Draft |
| 44 | Valid update; bounds/empty patch; security; lower/upper accepted boundaries | TC-F5-AC44-01..04 | Draft |
| 45 | Create/default; duplicate/name; price reflected; deletion/unpriced history; negative price | TC-F5-AC45-01..05 | Draft |
| 46 | 7/14/30 full ranges, zero dates, local midnight, bad range/auth | TC-F5-AC46-01..02 | Draft |
| 47 | Both alerts; review idempotency; security; no student lock | TC-F5-AC47-01..02 | Draft |
| 48 | Rolling window/order/cost/aggregates/missing names/privacy; auth | TC-F5-AC48-01..02 | Draft |

### TC-F5-AC38-01 — Read active and draft prompt configuration

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-38; API §14; validation §7 |
| Kind / priority | Normal; P1 |
| Environment | Integration API + PostgreSQL; no LLM |
| Preconditions | `adm_cfg`; one ACTIVE per feature and one SOLUTION_REVIEW DRAFT. |
| Test data | Active models `gpt-test-a`, versions `v1.0`; draft `v1.1`; maxTokens per config 1200/1800/900; one modelOptions list. |
| Steps | GET `/admin/prompt-templates`; inspect items and order. |
| Expected | 200; feature order SOLUTION_REVIEW, MOCK_INTERVIEW, GENERATE_TESTCASE; exactly ACTIVE plus DRAFT rows; no ARCHIVED. Read-only fields include version/model/instruction/guards/schema/maxTokens/options; guards null for generator. No DB write/LLM call. |
| Cleanup | Remove isolated draft. |
| Evidence | Response; prompt status query; LLM invocation count. |
| Design review | Draft |

### TC-F5-AC40-05 — Audit failure rolls back a multi-feature publication

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-40; API §1.2/17; BR-AI-13 step7; DEC-2026-1028-qa-f5-admin-audit-failure |
| Kind / priority | Recovery / Atomicity; P0 |
| Environment | API + PostgreSQL + transactional AuditLogPort fault hook |
| Preconditions | adm_cfg has UPDATE; valid changed drafts for SOLUTION_REVIEW and MOCK_INTERVIEW, current expectedActive IDs; snapshot all template/audit rows. |
| Test data | Publish both changed features; AuditLogPort writes first audit then throws on second within same transaction. |
| Steps | POST publish; compare all active/draft/archive/version/audit rows; remove fault and repeat with same expectedActive IDs. |
| Expected | First returns 503 AIR-503; both feature changes and first audit rollback, old ACTIVE and drafts remain exact snapshots. Retry succeeds with one ACTIVE and one publish audit per changed feature; no partial first publication. |
| Cleanup | Restore prompts/audits and fault hook after evidence capture. |
| Evidence | Responses, transaction timeline, full template/audit before-after snapshots. |
| Design review | Reviewed against owner-approved audit failure oracle; not executed. |

### TC-F5-AC43-05 — Audit failure rolls back limit change and unlock

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-43; API §1.2/19/20; BR-AI-05; DEC-2026-1028-qa-f5-admin-audit-failure |
| Kind / priority | Recovery / Security; P0 |
| Environment | API + PostgreSQL + transactional AuditLogPort fault hook |
| Preconditions | adm_budget has UPDATE; limit=8000000, used=5200000, locked_at set; snapshot singleton/audits. |
| Test data | Independent requests: PUT /admin/token-budget/limit with limitTokens=9000000; POST /admin/token-budget/unlock with no body. Audit writer throws before commit for each. |
| Steps | Reset baseline before each subcase; invoke with fault; compare singleton/audits; disable fault and repeat each request. |
| Expected | Each fault returns 503 AIR-503; limit_tokens/locked_at/updated_at/updated_by and audit rows unchanged. Healthy retries succeed: limit change preserves lock and records AI_BUDGET_LIMIT_CHANGED; explicit unlock clears lock and records AI_BUDGET_UNLOCKED. |
| Cleanup | Restore singleton and audits; disable fault. |
| Evidence | Four responses, config/audit deltas and fault timeline. |
| Design review | Reviewed against owner-approved audit failure oracle; not executed. |

### TC-F5-AC38-02 — Budget-only role cannot read prompts

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-38; API §14.4; validation §7 |
| Kind / priority | Security; P1 |
| Environment | Integration API |
| Preconditions | `adm_budget` lacks AI_CONFIG:READ. |
| Test data | Seed prompt rows as AC38-01. |
| Steps | GET prompt templates as `adm_budget`. |
| Expected | 403 `AIR-301`, no prompt data, rows unchanged. |
| Cleanup | None. |
| Evidence | Status/body and DB counts. |
| Design review | Draft |

### TC-F5-AC39-01 — Repeated saves update one draft and preserve active

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-39; API §§15–16; validation §7 |
| Kind / priority | Normal; P1 |
| Environment | Integration API + PostgreSQL |
| Preconditions | `adm_cfg`; active SOLUTION_REVIEW; no draft. |
| Test data | Same model `gpt-test-a`, two different nonblank instruction texts, exact three boolean guards. |
| Steps | PUT draft revision one, then revision two; GET history and templates. |
| Expected | Same DRAFT identity updated; latest text is revision two; history newest-first; active ID/content/status unchanged. |
| Cleanup | Delete isolated draft/history. |
| Evidence | Responses, history, DB IDs/status/content snapshots. |
| Design review | Draft |

### TC-F5-AC39-02 — Reject invalid model and whitespace instruction

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-39; API §16; validation §7 |
| Kind / priority | Abnormal / Boundary; P1 |
| Environment | Integration API + PostgreSQL |
| Preconditions | `adm_cfg`; known ACTIVE, no draft. |
| Test data | Independent draft attempts: model GPT-TEST-A=>422 AIR-202; instruction whitespace=>400 AIR-V12; SOLUTION_REVIEW guards missing a required boolean, extra field, or string true=>400 AIR-V13; GENERATE_TESTCASE with non-null guards=>400 AIR-V13. Use valid baseline fields otherwise. |
| Steps | Submit each draft request. |
| Expected | Every isolated request returns the exact status/code in Test data, creates no draft, changes no ACTIVE row and writes no publish audit. |
| Cleanup | None. |
| Evidence | Responses and before/after rows. |
| Design review | Draft |

### TC-F5-AC39-03 — `noFullSolution=false` is accepted

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-39; validation §7; decision `DEC-2026-1011-ai-no-full-solution-can-be-turned-off` |
| Kind / priority | Normal; P1 |
| Environment | Integration API + PostgreSQL |
| Preconditions | `adm_cfg`; active SOLUTION_REVIEW. |
| Test data | Valid instruction/model; guards exactly `{citeLines:true,answerInVietnamese:true,noFullSolution:false}`. |
| Steps | Save draft and read it back. |
| Expected | Accepted; false remains false; no coercion to true. |
| Cleanup | Delete draft. |
| Evidence | Response and stored guard object. |
| Design review | Draft |

### TC-F5-AC40-01 — Invalid member rejects multi-feature publish atomically

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-40; API §17; validation §7; BR-AI-13 |
| Kind / priority | Abnormal; P0 |
| Environment | Integration API + PostgreSQL |
| Preconditions | `adm_cfg`; snapshot active/draft/audit rows for SOLUTION_REVIEW and MOCK_INTERVIEW. |
| Test data | Publish both with current expectedActive IDs; first valid model, second `unknown-model`. |
| Steps | POST `/admin/prompt-templates/publish`. |
| Expected | `AIR-202`; neither feature changes, no archived/new versions or publish audit. |
| Cleanup | Remove isolated drafts. |
| Evidence | HTTP response and before/after rows/audit. |
| Design review | Draft |

### TC-F5-AC40-02 — Valid publication archives old active and creates sole active

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-40; API §17; BR-AI-13 |
| Kind / priority | Normal; P0 |
| Environment | Integration API + PostgreSQL |
| Preconditions | `adm_cfg`; changed valid draft; expectedActive ID current. |
| Test data | One feature change with allowed model and valid guards. |
| Steps | Publish; GET history/templates; inspect audit. |
| Expected | Old row ARCHIVED, new server-version ACTIVE; exactly one ACTIVE for feature and one audit entry naming publisher; other features untouched. |
| Cleanup | Delete test versions/audit. |
| Evidence | Response/history/status count/audit row. |
| Design review | Draft |

### TC-F5-AC40-03 — Stale editor cannot overwrite newer publication

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-40; API §17; validation §7 (`expectedActivePromptId`) |
| Kind / priority | Concurrency; P0 |
| Environment | Integration API + PostgreSQL, independent P/Q sessions |
| Preconditions | P and Q read active A; P publishes to B first. |
| Test data | Q submits valid change with expectedActivePromptId=A. |
| Steps | After P commit, submit Q stale publish. |
| Expected | Q gets 409 `AIR-201`; B remains active; Q values unapplied; no Q publish audit. |
| Cleanup | Remove test versions. |
| Evidence | Ordered responses and active/audit snapshot. |
| Design review | Draft |

### TC-F5-AC40-04 — No-change publish is rejected

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-40; validation §7; AIR-205 |
| Kind / priority | Abnormal; P1 |
| Environment | Integration API + PostgreSQL |
| Preconditions | Current expected ID; request equals active values. |
| Test data | A: exact active instruction/model/guards repeated. Independent malformed variants B/C/D: changes=[], duplicate feature entries, four entries (including a repeated feature to exceed the maximum). |
| Steps | POST publish. |
| Expected | A returns422 AIR-205. B/C/D return400 AIR-V14 before publication. All leave versions/status/audits unchanged. |
| Cleanup | None. |
| Evidence | Response and row counts. |
| Design review | Draft |

### TC-F5-AC41-01 — Seven-day forecast and four feature allocations reconcile

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-41; API §18; BR-AI-05 |
| Kind / priority | Normal; P1 |
| Environment | Integration API + PostgreSQL; UTC+7 fixed clock |
| Preconditions | `adm_budget`; limit 5,000,000; unlocked; 2,000,000 tokens over Oct 21–27. |
| Test data | Daily sums 250k, 275k, 275k, 300k, 300k, 300k, 300k; feature allocations sum to those totals and include a zero feature. |
| Steps | GET `/admin/token-budget`. |
| Expected | Used 2,000,000; remaining 3,000,000; rate 2,000,000/7 per day; remaining/rate=10.5, ceil=11 days, so projectedRunOutDate=2026-11-07 from fixed Oct 27 clock. All four feature codes including zero; totals reconcile. |
| Cleanup | Restore singleton; remove usage. |
| Evidence | Response and period/feature aggregation query. |
| Design review | Draft |

### TC-F5-AC41-02 — Zero limit is unconfigured and has no projection

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-41; API §18; BR-AI-05 steps 1,6 |
| Kind / priority | Boundary; P1 |
| Environment | Integration API + PostgreSQL |
| Preconditions | Seeded `limit_tokens=0`; no usage; `adm_budget`. |
| Test data | Singleton id=1, zero limit. |
| Steps | GET token budget. |
| Expected | UI/API reports unconfigured, projection null, no lock or divide-by-zero. |
| Cleanup | Restore singleton. |
| Evidence | Response and singleton row. |
| Design review | Draft |

### TC-F5-AC41-03 — Config-only admin sees budget but cannot read daily usage

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-41/46; API §§18,27; validation §8 |
| Kind / priority | Security; P1 |
| Environment | Integration API |
| Preconditions | Actor has ADMIN + AI_CONFIG:READ only. |
| Test data | Configured budget and usage rows. |
| Steps | GET token budget, then GET daily usage range 7. |
| Expected | Budget read 200 (either config-read or budget-read accepted); usage read 403 `AIR-301`, no usage payload. |
| Cleanup | None. |
| Evidence | Responses and role claims. |
| Design review | Draft |

### TC-F5-AC42-01 — Crossing threshold locks later student generation, exempts admin and preserves judge flow

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-42; BR-AI-05 steps 3–4; validation §9 |
| Kind / priority | Normal / Abnormal; P0 |
| Environment | Integration API + PostgreSQL + LLM stub + judge service |
| Preconditions | Limit 10,000,000; used 9,999,000; unlocked; student A has eligible report/session; ADMIN eligible; accepted-submission fixture. |
| Test data | First report consumes 1,500 tokens; second report/new interview each otherwise valid. |
| Steps | A generates report; inspect budget; A tries another report and new interview; ADMIN calls AI; A submits and reads normal judge result. |
| Expected | First succeeds; usage=10,000,500 and lock set; later student new report/session rejected `AIR-401` before provider; ADMIN passes budget gate; submission/judge/result unaffected. |
| Cleanup | Restore budget; remove AI/submit fixtures. |
| Evidence | Responses; token sum/lock; provider calls; judge result. |
| Design review | Draft |

### TC-F5-AC42-02 — Lowered limit is enforced on next eligible call

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-42; BR-AI-05 step 3 |
| Kind / priority | Boundary; P1 |
| Environment | Integration API + PostgreSQL + LLM stub |
| Preconditions | used=5,200,000; limit lowered to 5,000,000; locked_at null; student call eligible. |
| Test data | Valid new report request. |
| Steps | Student calls for new report. |
| Expected | Gate computes locked and persists locked_at; returns `AIR-401`; no provider call/token row. |
| Cleanup | Restore singleton. |
| Evidence | Response, lock, provider count, usage query. |
| Design review | Draft |

### TC-F5-AC43-01 — Unlock is blocked when usage is still over the limit

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-43; API §20; BR-AI-05 step 9 |
| Kind / priority | Abnormal; P0 |
| Environment | Integration API + PostgreSQL |
| Preconditions | `adm_budget`; locked; used=5.2m, limit=5m. |
| Test data | POST unlock. |
| Steps | Submit and inspect config/audit. |
| Expected | 422 `AIR-203`; remains locked; no `AI_BUDGET_UNLOCKED` audit. |
| Cleanup | Restore config/audit fixture. |
| Evidence | Response and DB rows. |
| Design review | Draft |

### TC-F5-AC43-02 — Raise limit does not unlock; explicit unlock does

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-43; API §§19–20; BR-AI-05 steps 8–9 |
| Kind / priority | Normal; P0 |
| Environment | Integration API + PostgreSQL + LLM stub |
| Preconditions | Locked, used=5.2m, limit=5m. |
| Test data | New limit 8m; valid student report. |
| Steps | Set 8m; read lock; explicitly unlock; call new report. |
| Expected | Limit change writes `AI_BUDGET_LIMIT_CHANGED` but lock persists; explicit unlock clears lock, writes `AI_BUDGET_UNLOCKED`; report reaches provider and succeeds. |
| Cleanup | Restore config and remove rows. |
| Evidence | Responses, config snapshots, audit, provider result. |
| Design review | Draft |

### TC-F5-AC43-03 — Reject zero/negative limit and config-only set/unlock

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-43; API §§19–20; validation §8 |
| Kind / priority | Boundary / Security; P1 |
| Environment | Integration API |
| Preconditions | Current config snapshot; `adm_budget` and config-only actor. |
| Test data | Invalid limit requests0,-1,1.5,9000000000001=>400 AIR-V15; otherwise-valid set/unlock as config-only actor=>403 AIR-301. |
| Steps | Submit each request independently. |
| Expected | Exact rejection status/code per Test data; no state/audit mutation. Valid inclusive maximum9000000000000 is exercised as a separate healthy subcase and returns200 with that limit and one limit-change audit. Restore baseline before testing permission subcases. |
| Cleanup | None. |
| Evidence | Responses and snapshots. |
| Design review | Draft |

### TC-F5-AC43-04 — Unlock when already unlocked is no-op

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-43; API §20; BR-AI-05 step 9 |
| Kind / priority | Boundary; P2 |
| Environment | Integration API + PostgreSQL |
| Preconditions | `locked_at=null`, used below positive limit; `adm_budget`. |
| Test data | Capture row/update timestamp and audit count. |
| Steps | POST unlock. |
| Expected | Successful no-op according to API response; row/timestamp unchanged; no audit. |
| Cleanup | None. |
| Evidence | Response, row and audit snapshots. |
| Design review | Draft |

### TC-F5-AC44-01 — Apply allowed limits on subsequent calls

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-44; API §§21–22; validation §8 |
| Kind / priority | Normal; P1 |
| Environment | Integration API + PostgreSQL + Redis |
| Preconditions | `adm_cfg`; config snapshot; test user has clean counters. |
| Test data | PATCH reviewPerHour=5, interviewPerHour=2, cooldownSeconds=999, warnPercent=100. |
| Steps | PATCH, GET config, exercise next eligible report/session calls to boundary. |
| Expected | Values persist and next requests use them; omitted values remain unchanged. |
| Cleanup | Restore config and test Redis keys. |
| Evidence | Responses, DB config, quota outcomes. |
| Design review | Draft |

### TC-F5-AC44-02 — Reject limits outside bounds and empty PATCH

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-44; API §22; validation §8 |
| Kind / priority | Boundary; P1 |
| Environment | Integration API + PostgreSQL |
| Preconditions | Snapshot config; `adm_cfg`. |
| Test data | Separate requests: reviewPerHour=0; interviewPerHour=1000; cooldownSeconds=1000; warnPercent=0; warnPercent=101; `{}`. |
| Steps | PATCH each request. |
| Expected | Each 400 `AIR-V16`; unchanged config. Valid bounds: 1..999 and warnPercent 1..100. |
| Cleanup | None. |
| Evidence | Responses and row snapshots. |
| Design review | Draft |

### TC-F5-AC44-03 — Budget-only admin cannot update limits

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-44; API §22; validation §8 |
| Kind / priority | Security; P1 |
| Environment | Integration API |
| Preconditions | `adm_budget` only. |
| Test data | Valid patch reviewPerHour=5. |
| Steps | PATCH limits config. |
| Expected | 403 `AIR-301`; config unchanged. |
| Cleanup | None. |
| Evidence | Response and config before/after. |
| Design review | Draft |

### TC-F5-AC44-04 — Inclusive lower and upper valid values are accepted

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-44; API §22; validation §8 |
| Kind / priority | Boundary; P1 |
| Environment | Integration API + PostgreSQL |
| Preconditions | `adm_cfg`; snapshot current limits. |
| Test data | PATCH `reviewPerHour=1`, `interviewPerHour=999`, `cooldownSeconds=1`, `warnPercent=1`, then separate patch all four at maximum 999/999/999/100. |
| Steps | Submit each boundary patch independently and GET config after each. |
| Expected | Both requests accepted; exact boundary values persist. |
| Cleanup | Restore snapshot. |
| Evidence | Responses and config rows. |
| Design review | Draft |

### TC-F5-AC45-01 — Create model price defaults to zero

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-45; API §23; validation §8 |
| Kind / priority | Normal; P1 |
| Environment | Integration API + PostgreSQL |
| Preconditions | Empty model-price table; `adm_cfg`. |
| Test data | Create `M` without price. |
| Steps | POST create, then GET list. |
| Expected | Row exists with price 0. |
| Cleanup | Delete by returned UUID. |
| Evidence | Responses and DB row. |
| Design review | Draft |

### TC-F5-AC45-02 — Reject duplicate case-insensitive name and blank name

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-45; API §23; validation §8 |
| Kind / priority | Abnormal / Boundary; P1 |
| Environment | Integration API + PostgreSQL |
| Preconditions | Price `M` exists. |
| Test data | Independent attempts `m`, `"   "`. |
| Steps | Submit both creates independently. |
| Expected | `m` -> 409 `AIR-204`; blank -> 400 `AIR-V17`; no extra row. |
| Cleanup | None. |
| Evidence | Responses and row list. |
| Design review | Draft |

### TC-F5-AC45-03 — Price update changes ranking cost semantically

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-45; API §§24,30; validation §8 |
| Kind / priority | Normal; P1 |
| Environment | Integration API + PostgreSQL |
| Preconditions | Model M has 2,000,000 tokens in ranking window; actor has read/update permissions. |
| Test data | Set price to 0.15 USD per million tokens. |
| Steps | Update price; read top-users ranking. |
| Expected | Cost semantically equals 2 * 0.15 = 0.30, subject to documented output precision; no byte/schema assertion beyond API contract. |
| Cleanup | Restore/delete price and usage. |
| Evidence | Update and ranking response; input usage. |
| Design review | Draft |

### TC-F5-AC45-04 — Deleted price leaves usage and shows unpriced dash

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-45; API §§26,30 |
| Kind / priority | Normal; P1 |
| Environment | Integration API + PostgreSQL |
| Preconditions | M price and corresponding ranking usage exist. |
| Test data | Existing usage stays after price deletion. |
| Steps | DELETE price; query ranking. |
| Expected | Price deleted; usage retained; cost appears as dash/unpriced, not computed. |
| Cleanup | Delete usage. |
| Evidence | Delete response, DB rows, ranking result. |
| Design review | Draft |

### TC-F5-AC45-05 — Negative model price is rejected

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-45; API §24; validation §8 |
| Kind / priority | Boundary; P1 |
| Environment | Integration API + PostgreSQL |
| Preconditions | Existing model price with known value; `adm_cfg`. |
| Test data | Update price to `-0.01`. |
| Steps | PUT model price and read row. |
| Expected | 400 `AIR-V18`; stored price remains unchanged. |
| Cleanup | None. |
| Evidence | HTTP response and row before/after. |
| Design review | Draft |

### TC-F5-AC46-01 — Daily chart fills dates and separates Vietnam midnight

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-46; API §27; validation §8 |
| Kind / priority | Normal / Boundary; P1 |
| Environment | Integration API + PostgreSQL, UTC+7 |
| Preconditions | Authorized budget reader; seed activity and empty dates. |
| Test data | 11 SOLUTION_REVIEW tokens at Oct 26 23:30+07; 13 MOCK_INTERVIEW at Oct 27 00:30+07; no usage on Oct 25. |
| Steps | GET rangeDays=7,14,30. |
| Expected | Exactly 7/14/30 dates, oldest-first; every date includes four feature buckets and zero for Oct 25; the two calls land on distinct local dates. |
| Cleanup | Remove usage fixture. |
| Evidence | Responses and timestamp/aggregation query. |
| Design review | Draft |

### TC-F5-AC46-02 — Invalid range and config-only usage read are denied

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-46; API §27; validation §8 |
| Kind / priority | Boundary / Security; P1 |
| Environment | Integration API |
| Preconditions | Budget-reader actor and config-only actor. |
| Test data | rangeDays=10, then rangeDays=7 as config-only actor. |
| Steps | Request both. |
| Expected | First 400 `AIR-V19`; second 403 `AIR-301`; no usage payload. |
| Cleanup | None. |
| Evidence | Responses. |
| Design review | Draft |

### TC-F5-AC47-01 — List both warning types; review anomaly idempotently; user remains usable

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-47; API §§28–29; BR-AI-05/12 |
| Kind / priority | Normal; P1 |
| Environment | Integration API + PostgreSQL |
| Preconditions | Budget warning condition true; OPEN anomaly for stu_c; actor has budget read/update. |
| Test data | Alert UUID `20000000-0000-4000-8000-000000000001`, count=14, average=2, ratio=7; identity name available. |
| Steps | List; mark reviewed; list; repeat review; attempt valid AI operation as stu_c. |
| Expected | Both budget and anomaly warnings visible; anomaly has name/count/ratio/window; reviewed alert leaves OPEN list; repeat indicates already reviewed and timestamp unchanged; student still allowed by budget/anomaly state (assuming not independently budget-locked). |
| Cleanup | Delete alert/usage; restore budget. |
| Evidence | API bodies, alert row timestamps, student action and lock state. |
| Design review | Draft |

### TC-F5-AC47-02 — Read-only viewer cannot mark alert reviewed

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-47; API §29; validation §8 |
| Kind / priority | Security; P1 |
| Environment | Integration API + PostgreSQL |
| Preconditions | `adm_readonly`; alert OPEN. |
| Test data | Existing alert UUID. |
| Steps | POST mark-reviewed. |
| Expected | 403 `AIR-301`; remains OPEN, reviewed_at null. |
| Cleanup | Remove alert. |
| Evidence | Response and row. |
| Design review | Draft |

### TC-F5-AC48-01 — Rankings honor 30-day window, aggregates, missing names and privacy

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-48; API §§30–31; validation §8 |
| Kind / priority | Normal / Security; P1 |
| Environment | Integration API + PostgreSQL; identity/problem stubs |
| Preconditions | Required read permissions; ranking data seeded. |
| Test data | In-window users A=900 tokens/B=400; A also has 5,000 tokens at 31 days old; P1 has 3 calls/900 tokens; model with no price; unresolved user/problem IDs; source/chat sentinels stored outside ranking fields. |
| Steps | GET both ranking endpoints; inspect order, values, names, serialized content. |
| Expected | Old row excluded; descending token order; missing price is dash; P1 count=3 and average=300; missing names do not remove rows/table; no source or conversation text appears. |
| Cleanup | Delete usage fixtures. |
| Evidence | Responses, aggregation query, stub results, sentinel scan. |
| Design review | Draft |

### TC-F5-AC48-02 — Missing endpoint-specific ranking permission is rejected

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-48; API §§30–31; validation §8 |
| Kind / priority | Security; P1 |
| Environment | Integration API |
| Preconditions | ADMIN actors lack `AI_TOKEN_BUDGET:READ`, required by both ranking endpoints. |
| Test data | Claims have AI_CONFIG grants only, without budget read. |
| Steps | Call each endpoint with its missing-grant actor. |
| Expected | 403 `AIR-301` for both endpoints; no ranking data. |
| Cleanup | None. |
| Evidence | Responses and claims. |
| Design review | Draft |
