# Admin screen test cases — Phase 4 draft

Status: **Draft; Chưa thực hiện**. These are design cases only; no test has been run.

Fixture aliases, independent scenario reset, role assignments and mock/integration evidence follow [fixture conventions](../FIXTURE_CONVENTIONS.md). Per-case Test data overrides file baseline.

## Shared setup

- **Environment:** admin frontend with contract stubs for UI checks; integration requires F1/F2/F4 and AI/queue/log services plus disposable database. Use dedicated `adm-a` (authorized) and `viewer-a` (read-only/limited) accounts. Replace fixture slugs with valid UUIDs at integration time.
- **Fixture namespace:** users `u-101..103`, roles `role-admin`, `role-teacher`, `role-custom`; permissions `perm-01..03`; AI features `review`, `mock`, `interview`; models `model-a`, `model-b`; queue `judge`; languages Java/C++/Python; log rows `log-1..60`; infra event IDs `evt-1..60`.
- **Evidence:** screenshot/accessible tree, URL, request and response payloads, and DB/audit/event state for mutations. Redact credentials, OTPs, and secret values.
- **Cleanup:** restore changed settings and permissions, remove disposable records, clear mocks. Expected denied/cancelled operations must have no write side effects.

## ADM0101 — Admin overview

### TC-ADM0101-O-AC-01-01 — Ten independent metric blocks and retry
| Field | Value |
| :--- | :--- |
| AC / DD source | TDD O-AC-01; DD §§2–7, ADM-O-01/02; F1 dashboard operations |
| Kind / priority | Normal; P1 — Incorrect executive metrics |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | ten dashboard operations return distinct known values; difficulty catalog has four levels `L0..L3` with explicit sort order; fail one #43-backed block while other nine succeed. |
| Steps | open `/admin`, compare values and difficulty labels/order, retry failed block. |
| Expected | each block renders only its response; difficulty levels and order follow catalog, not hard-coded three-level assumptions; nine successful blocks remain visible; retry calls only failed block endpoint and does not alter other values. No synthesized sum across different response times. |
| Cleanup | restore endpoint |
| Evidence | retain value-to-response mapping and request log. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-ADM0101-O-AC-02-01 — No final verdicts means no acceptance rate
| Field | Value |
| :--- | :--- |
| AC / DD source | O-AC-02; DD §§3–5, ADM-O-03 |
| Kind / priority | Normal; P1 — Misleading KPI |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | verdict series has zero final-result submissions; no mock seed data. |
| Steps | open overview with empty finalized verdict dataset. |
| Expected | acceptance metric shows the contract's empty/no-data state; no division by zero, NaN/Infinity, sample-derived rate, or mock value is visible or emitted in accessible chart table. |
| Cleanup | restore dashboard fixture |
| Evidence | retain DOM/text and raw response. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-ADM0101-O-AC-03-01 — Admin links preserve overview footer
| Field | Value |
| :--- | :--- |
| AC / DD source | O-AC-03; DD §§2,6–7, ADM-O-04 |
| Kind / priority | Normal; P2 — Navigation |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | admin overview and footer enabled. |
| Steps | open each administration link from overview, verify destination, return using browser Back. |
| Expected | each link opens its corresponding configured admin route; overview footer remains rendered after return; shell handles navigation without extra dashboard reads from the link itself. |
| Cleanup | return to overview |
| Evidence | capture destination map and footer. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

## ADM0201 — User management

### TC-ADM0201-UM-AC-01-01 — Server filter, page selection, partial bulk result
| Field | Value |
| :--- | :--- |
| AC / DD source | UM-AC-01; DD §§3–7, BR-SC-01 |
| Kind / priority | Normal; P1 — Bulk scope |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | 25 users across roles; pageSize=8; page1 u-101..108 and page2 u-109..116; all selected targets eligible; three selected page2 targets produce two successes/one failure. |
| Steps | Filter instructor role, select all page1, move page2 and verify selection cleared; select exactly three page2 targets and invoke permitted action. |
| Expected | Filter/paging use server; select-all chooses only eight current rows and changing page/filter clears selection. Three N single-user requests act only selected page2 IDs; result counts actual two successes/one failure and retains only failed target for retry. No page1/off-page mutation. |
| Cleanup | undo bulk status change |
| Evidence | retain requests and before/after records. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-ADM0201-UM-AC-02-01 — Protect final admin and linked-login accounts
| Field | Value |
| :--- | :--- |
| AC / DD source | UM-AC-02; DD §§3–7, BR-SC-02 |
| Kind / priority | Normal; P0 — Privilege lockout |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | `u-101` is sole admin; `u-102` uses linked-login only; `u-103` can be locked; one account missing required lock reason. |
| Steps | attempt demotion of `u-101`; request temporary password for `u-102`; lock `u-103` with blank then supplied reason; attempt same action as viewer. |
| Expected | Final-admin demotion is blocked and valid bypass is rejected server-side without write. OAuth-only reset returns provider guidance, no temporary password. For notifyByEmail=true, lock requires nonblank trimmed reason; notifyByEmail=false does not impose an email/reason field beyond API contract. Anonymized account cannot be unlocked. Invalid/unauthorized UI actions send no mutation. |
| Cleanup | restore `u-103` status |
| Evidence | retain policy response and audit diff. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-ADM0201-UM-AC-03-01 — Account creation and audit do not reveal password
| Field | Value |
| :--- | :--- |
| AC / DD source | UM-AC-03; DD §§3–7, BR-SC-02 |
| Kind / priority | Security; P0 — Credential exposure |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | `adm-a` has user-create permission; unique email `qa-user-20261027@example.test`; generated temporary secret. |
| Steps | create account, inspect confirmation and browser storage/logs, open linked audit record. |
| Expected | F1 #50 creates PENDING_VERIFICATION account with permitted STUDENT/INSTRUCTOR role; server sends temporary password only via email sink. Password never appears in response/UI/dialog/URL/storage/audit/log, including initial handoff. List refresh shows new account; System Log link opens /admin/system-log and related audit identifies creation. |
| Cleanup | delete test user |
| Evidence | retain redacted audit linkage and verify no secret in captured artifacts. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

## ADM0202 — Permission matrix

### TC-ADM0202-RBAC-AC-01-01 — Tab switch discards stale permission data
| Field | Value |
| :--- | :--- |
| AC / DD source | RBAC-AC-01; DD §§3–7, BR-SC-01 |
| Kind / priority | Normal; P1 — Privilege display |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | role-admin and role-teacher tabs have different permission maps; delay first tab response. |
| Steps | open role-admin, switch rapidly to role-teacher, resolve teacher response then stale admin response; toggle one permission. |
| Expected | visible matrix matches active tab only; stale response cannot overwrite it; a single toggle persists immediately and re-renders returned value without a global Save control. |
| Cleanup | restore original permission |
| Evidence | retain response order and audit event. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-ADM0202-RBAC-AC-02-01 — Failed or repeated toggle rolls back the same cell
| Field | Value |
| :--- | :--- |
| AC / DD source | RBAC-AC-02; DD §§3–7, BR-SC-01 |
| Kind / priority | Security; P1 — Authorization integrity |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | `role-teacher/perm-02=false`; first mutation can fail, then succeed on explicit retry. |
| Steps | double-click toggle rapidly; separately cause server 500 on one toggle. |
| Expected | only one in-flight mutation is sent, no opposing duplicate write; on failure cell returns to persisted `false` and offers retry feedback; retry sends one update and renders confirmed server value. |
| Cleanup | restore original value |
| Evidence | capture request count and audit. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-ADM0202-RBAC-AC-03-01 — System and assigned roles cannot be deleted
| Field | Value |
| :--- | :--- |
| AC / DD source | RBAC-AC-03; DD §§3–7, BR-SC-02 |
| Kind / priority | Security; P1 — Access control |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | `role-admin` system role; `role-teacher` assigned to `u-102`; `role-custom` unused. |
| Steps | attempt deletion of system and assigned roles, create a custom role, delete unused role, switch tabs by keyboard. |
| Expected | protected roles are blocked with no delete request; valid role create/delete succeeds; after mutations focus lands at a sensible stable control and tab changes show correct matrix. |
| Cleanup | restore roles/assignment |
| Evidence | retain requests and focus trace. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

## ADM0301 — AI configuration

### TC-ADM0301-AIC-AC-01-01 — Current, draft, and history remain distinct
| Field | Value |
| :--- | :--- |
| AC / DD source | AIC-AC-01; DD §§3–7, BR-SC-01/02 |
| Kind / priority | Normal; P1 — Live model configuration |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | three features each have current version 4, draft version 5, history version 3; allowed models `[model-a,model-b]`. |
| Steps | inspect all features, edit one prompt and save draft, cancel another edit; attempt a model outside allowlist. |
| Expected | current/draft/history are labeled and show their own exact version values; cancel sends no write; draft save changes only draft; selector permits only listed models. Live version remains unchanged. |
| Cleanup | restore draft |
| Evidence | retain version IDs and request diff. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-ADM0301-AIC-AC-02-01 — Atomic publish and safe history restore
| Field | Value |
| :--- | :--- |
| AC / DD source | AIC-AC-02; DD §§3–7, BR-SC-01 |
| Kind / priority | Normal; P0 — Production behavior |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | valid complete draft v5, current v4, archived v3; publish endpoint can succeed or fail. |
| Steps | publish v5; repeat with injected failure; restore archived v3 for review. |
| Expected | success atomically replaces complete current config; failure leaves both current and draft byte-for-byte unchanged; restore creates a new editable draft from v3 and does not modify archived v3. No partially active config. |
| Cleanup | restore original current/draft |
| Evidence | capture version table and audit events. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-ADM0301-AIC-AC-03-01 — Unsaved draft leave confirmation is accessible
| Field | Value |
| :--- | :--- |
| AC / DD source | AIC-AC-03; DD §§3–7, BR-SC-01 |
| Kind / priority | Recovery; P2 — Loss prevention/accessibility |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | modify prompt locally without saving; keyboard-only session. |
| Steps | attempt route exit; choose stay; attempt again and choose discard; repeat via Escape/Tab/Enter. |
| Expected | Attempting to leave with dirty prompt, including Escape from the editor, cannot silently discard changes: confirmation appears. Stay/cancel keeps draft and route; discard exits without save request. Focus stays inside the active dialog while it is open and returns to the editor context when canceled; labels/actions are keyboard operable. Exact element within editor context is outside this oracle. |
| Cleanup | restore route and draft |
| Evidence | capture keyboard/focus trace. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

## ADM0302 — AI usage

### TC-ADM0302-AIU-AC-01-01 — Independent usage blocks, periods, and stale response
| Field | Value |
| :--- | :--- |
| AC / DD source | AIU-AC-01; DD §§3–7, BR-SC-01 |
| Kind / priority | Normal; P1 — Budget reporting |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | five usage blocks with distinct values; one fails; monthly budget 100 units; period A response delayed after period B. |
| Steps | open usage page; retry failed block; rapidly choose period A then B and resolve B before A. |
| Expected | four successful blocks remain visible; retry only calls failed block; late A response cannot replace B; budget remains calendar-month value regardless of selected display period. |
| Cleanup | restore period and responses |
| Evidence | retain requests and displayed values. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-ADM0302-AIU-AC-02-01 — Alert state changes only after successful save
| Field | Value |
| :--- | :--- |
| AC / DD source | AIU-AC-02; DD §§3–7, BR-SC-02 |
| Kind / priority | Normal; P1 — Auditability |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | alert `evt-1` unseen; one model lacks price; usage rows cover two model groups. |
| Steps | mark alert seen with success, repeat with server rejection; inspect cost and grouped run counts. |
| Expected | UI marks seen only after confirmed success; rejected update keeps alert unseen; missing price shows `-`; run is assigned to its actual model group and is not misattributed. |
| Cleanup | restore alert state |
| Evidence | retain response and rendered group values. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

## ADM0303 — AI limits

### TC-ADM0303-AIL-AC-01-01 — Four limit blocks save independently
| Field | Value |
| :--- | :--- |
| AC / DD source | AIL-AC-01; DD §§3–7, BR-SC-01 |
| Kind / priority | Normal; P1 — Budget controls |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | four limit blocks, with testcase-generation source returning 503; saved rate 10/min and draft rate 12/min. |
| Steps | load page; change only rate to 12; save while injecting failure. |
| Expected | budget and other blocks remain visible; request body contains only changed field; failed save preserves server value 10 and local draft 12 for comparison/retry; unrelated block data unchanged. |
| Cleanup | restore rate |
| Evidence | capture request body and values. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-ADM0303-AIL-AC-02-01 — Budget exhaustion cannot be bypassed by unlock
| Field | Value |
| :--- | :--- |
| AC / DD source | AIL-AC-02; DD §§3–7, BR-SC-02 |
| Kind / priority | Normal; P0 — Spend control |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | F5 budget usedTokens=100, limitTokens=100, lockedAt set; then raise limitTokens to 150 with usedTokens still 100. |
| Steps | As authorized admin try UI unlock at equality; independently bypass UI and call unlock endpoint; raise limit to 150, inspect lock, explicitly unlock. |
| Expected | At usedTokens>=limitTokens UI disables unlock and sends no request. Authorized direct bypass returns 422 AIR-203 with no unlock/audit. Raising limit does not auto-unlock; with 100<150 explicit unlock succeeds and clears lockedAt with one audit. |
| Cleanup | restore cap/lock |
| Evidence | retain request and audit log. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-ADM0303-AIL-AC-03-01 — Price and generation boundaries enforced client/server
| Field | Value |
| :--- | :--- |
| AC / DD source | AIL-AC-03; DD §§3–7, BR-SC-03 |
| Kind / priority | Boundary; P1 — Billing/config integrity |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | existing price label `Provider-X`; generation cap 1–10; one usage-history row references price. |
| Steps | add `provider-x` duplicate; enter negative and nonnumeric price then blur; submit direct negative API; set cap 0,1,10,11; rename/delete price; try config and budget actions with separated permissions. |
| Expected | case-insensitive duplicate rejected; negative/nonnumeric UI value normalizes to 0 on blur, direct negative request rejected; cap accepts 1 and 10 only; renaming/deleting price leaves usage history intact; config and budget permissions are evaluated independently. |
| Cleanup | restore price and cap |
| Evidence | retain validation/network/audit evidence. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

## ADM0401 — Queue monitor

### TC-ADM0401-QM-AC-01-01 — Queue blocks load independently without invented worker detail
| Field | Value |
| :--- | :--- |
| AC / DD source | QM-AC-01; DD §§3–7, BR-SC-01–03 |
| Kind / priority | Normal; P1 — Operational visibility |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | queue state, job list, config, and worker summary; fail job list; two tabs with distinct job rows. |
| Steps | refresh, switch tabs, paginate, inspect worker area. |
| Expected | successful blocks remain visible through job-list failure; switching tab/page never displays previous tab's jobs; no per-worker state table or unsupported worker-overload status is fabricated. |
| Cleanup | restore responses |
| Evidence | retain query and accessible content. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-ADM0401-QM-AC-02-01 — Operational switches update after accepted write only
| Field | Value |
| :--- | :--- |
| AC / DD source | QM-AC-02; DD §§3–7, BR-SC-01 |
| Kind / priority | Normal; P0 — Queue disruption |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | queue running, autoscale off; pause request succeeds; autoscale request first fails; F4 backlog threshold fixture. |
| Steps | pause queue; toggle autoscale with rejected response; retry successfully while backlog crosses configured threshold. |
| Expected | UI state changes only after accepted writes; failure is not reported as success and does not create repeated calls; autoscale reception capacity follows F4 criteria for supplied backlog. No guessed threshold is asserted here. |
| Cleanup | restore running/autoscale state |
| Evidence | retain F4 config and audit. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-ADM0401-QM-AC-03-01 — Queue parameters and navigation dialog
| Field | Value |
| :--- | :--- |
| AC / DD source | QM-AC-03; DD §§3–7, BR-SC-02 |
| Kind / priority | Normal; P1 — Operational configuration |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | current positive integer values for concurrency, retry count and interval; keyboard-only session. |
| Steps | enter positive integers and save; try zero, negative, decimal; cancel a second edit; open Infra Events. |
| Expected | only positive integers are submitted; invalid values are rejected; save commits and cancel does not; dialog has clear actions and returns focus to its opener; Infra Events link opens correct admin route. |
| Cleanup | restore original parameters |
| Evidence | capture requests and focus trace. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

## ADM0403 — System log

### TC-ADM0403-LOG-AC-01-01 — Filters, cursor paging, and stream reconnect
| Field | Value |
| :--- | :--- |
| AC / DD source | LOG-AC-01; DD §§3–7, BR-SC-01 |
| Kind / priority | Normal; P1 — Audit completeness |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | 60 ordered log rows and one newly appended event; stream disconnect/reconnect with replay overlap. |
| Steps | filter; load pages; change filter; disconnect and reconnect stream. |
| Expected | each page has at most 50 rows; filter change resets old cursor; reconnect shows each accepted row once without gaps or duplicates, including overlap at last seen cursor. |
| Cleanup | restore stream |
| Evidence | retain ordered IDs and cursor requests. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-ADM0403-LOG-AC-02-01 — Date validation and secret-safe export
| Field | Value |
| :--- | :--- |
| AC / DD source | LOG-AC-02; DD §§3–7, BR-SC-02 |
| Kind / priority | Security; P0 — Sensitive data |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | Fixed today 2026-10-28 and retentionDays=90; reversed range, future date, before-retention date, valid same-day range; selected columns include content; sensitive canaries OTP-CANARY/SECRET-CANARY; Accept-Language vi/en and omitted. |
| Steps | Try search and export for each invalid range; use valid range then open export, change underlying list filter before confirming export; choose columns; repeat locales. |
| Expected | Reversed/future/before-retention ranges block search/export with field feedback and no request; equal valid dates pass. Export snapshots filters at dialog open and uses only selected nonempty column set, independent of current page. content is redacted human description from actionType/safeParams using Accept-Language vi/en (default vi), never raw before/after JSON; no secret canary in UI/CSV. ADMIN read/export works without SYSTEM_AUDIT_LOG update permission. |
| Cleanup | securely delete CSV and fixtures |
| Evidence | retain only redacted evidence and request log. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-ADM0403-LOG-AC-03-01 — Retention reduction/deletion requires permission and confirmation
| Field | Value |
| :--- | :--- |
| AC / DD source | LOG-AC-03; DD §§3–7, BR-SC-02; F1 #51/#52, BR-20 and daily cleanup contract |
| Kind / priority | Recovery; P0 — Irreversible audit loss |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | retention 365 days; records older than proposed 90-day policy; compare `adm-a` and `viewer-a`. |
| Steps | As admin lower retention, cancel then confirm required confirmations; as ADMIN read-only viewer attempt change; inspect row-delete controls and daily cleanup in integration. |
| Expected | Cancel sends no #52; confirmed setting uses one #52 only after applicable second confirmation. Read-only admin cannot update retention; no manual row-delete control/endpoint is invented. Existing rows are not immediately purged; DELETE daily cleanup removes created_at<now()-retentionDays in batches 5000, ARCHIVE retains expired logs while archive job is absent. |
| Cleanup | restore retention |
| Evidence | retain permission/audit evidence without purged personal data. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

## ADM0404 — Infrastructure events

### TC-ADM0404-IE-AC-01-01 — Filtered cursor paging preserves order and URL context
| Field | Value |
| :--- | :--- |
| AC / DD source | IE-AC-01; DD §§3–7, BR-SC-01 |
| Kind / priority | Normal; P1 — Operational traceability |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | 60 events with known severity/service/time order; URL has valid service and severity; include invalid query enum. |
| Steps | load URL; change keyword, severity, service, date range; load more to end. |
| Expected | Valid filters apply; invalid enum is ignored; each filter change resets cursor; newest events sort first by server tuple `(occurredAt, sourceKey)`; next-page cursor is base64url and page predicate is tuple `< cursor`, so equal-time events continue without omission/duplication; client appends response order without re-sorting. End-of-data (`nextCursor=null`) removes/disables load-more. |
| Cleanup | restore URL |
| Evidence | retain cursors and event IDs. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-ADM0404-IE-AC-02-01 — Invalid dates and page/refresh errors retain truthful state
| Field | Value |
| :--- | :--- |
| AC / DD source | IE-AC-02; DD §§3–7, BR-SC-02 |
| Kind / priority | Normal; P1 — Operational visibility |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | start date later than end date; initial events `evt-1..evt-20`; page-next fails once; refresh fails. |
| Steps | search invalid range; correct and load events; request next page during failure; retry and refresh with failure. |
| Expected | invalid range has field-specific message and no query; next-page failure retains existing rows; refresh failure never shows success; loaded event list remains truthful and retry can recover. |
| Cleanup | restore healthy endpoints |
| Evidence | retain rows and request sequence. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-ADM0404-IE-AC-03-01 — Permission-gated event visibility and destinations
| Field | Value |
| :--- | :--- |
| AC / DD source | IE-AC-03; DD §§3–7, BR-SC-02 |
| Kind / priority | Security; P0 — Operational data access |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | `viewer-a` lacks queue-monitor permission; `adm-a` has permission; known event canary `evt-42`. |
| Steps | open Infra Events as viewer and follow Queue/Log links where allowed; open as admin; observe background request count over fixed 30-second idle window. |
| Expected | Viewer receives no event rows/canary or sensitive payload; links resolve only to authorized correct destinations. After initial fetch completes, the admin screen makes **zero further event-list requests** during the 30-second idle window; only a user-triggered refresh may call F4 §22 again. No SSE connection is opened. |
| Cleanup | clear event canary |
| Evidence | retain redacted request count and permission responses. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

## ADM0501 — Language configuration

### TC-ADM0501-AL-AC-01-01 — Three language drafts persist across tabs and save together
| Field | Value |
| :--- | :--- |
| AC / DD source | AL-AC-01; DD §§3–7, BR-SC-01 |
| Kind / priority | Normal; P1 — Judge configuration |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | GET returns all four blocks languages/gradingDefaults/sandbox/generatorSandbox; JAVA/CPP/PYTHON rows include read-only compilerDisplayName, multipliers and updatedAt; separate updatedAt for other blocks. |
| Steps | Edit Java timeLimitMultiplier in first tab and generatorSandbox runtime in second tab; switch back/forth; save once; server response returns new timestamps and canonical values; change another field and save again. |
| Expected | Exactly two tabs share one draft with three language rows. One PUT sends full four-block snapshot, including unchanged CPP and each original updatedAt, and excludes read-only compilerDisplayName/pendingJobCount. PUT response replaces saved/draft/cache and all timestamps; second save uses returned versions and has no artificial 409. No per-language compile-timeout field is invented. |
| Cleanup | restore settings |
| Evidence | retain payload and version timestamp. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-ADM0501-AL-AC-02-01 — Numeric boundaries and last enabled language guard
| Field | Value |
| :--- | :--- |
| AC / DD source | AL-AC-02; DD §§3–7, BR-SC-01 |
| Kind / priority | Normal; P0 — Submission processing availability |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | Multipliers: 0.10, 9.99, 0.09, 10.00 and 1.234; time examples use `timeLimitMs=3004` and multiplier 9.99 (rounded above 30,000), plus `1000 × 0.10=100`; memory examples use `64 × 1.00=64` and `103 × 9.99` (above 1,024). For last-language guard, Java is the only enabled language; separate fixture has C++ enabled with 7 queued jobs. |
| Steps | Try each multiplier/effective-limit variant in UI; separately bypass client for exact API codes. Try last-language disable; in valid fixture disable C++ with seven pending jobs and press Save, cancel once then confirm; follow a job already accepted before disable. |
| Expected | UI blocks invalid multiplier/effective-limit/last-language writes with associated feedback. Direct invalid requests return 400 JDG-V10, 422 JDG-204/205 and JDG-203 respectively with no write. Multiplier endpoints 0.10/9.99 and effective time 100/30000 ms, memory 64/1024 MB pass. Valid disable prompts once on Save with C++ pending=7; cancel keeps draft/no PUT, confirm sends one snapshot, already-accepted job still judges. |
| Cleanup | restore enabled flags and coefficients |
| Evidence | retain queue/job state. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-ADM0501-AL-AC-03-01 — Save conflict and authorization preserve draft
| Field | Value |
| :--- | :--- |
| AC / DD source | AL-AC-03; DD §§3–7, BR-SC-02/03 |
| Kind / priority | Security; P0 — Unauthorized config/data exposure |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | `adm-a` can read/write; `viewer-a` read-only; local draft differs from server version; server version changes concurrently; network failure toggle. |
| Steps | save during conflict and during network failure; choose reload server version and cancel once then confirm; attempt leaving with draft; repeat with viewer lacking write, then no read permission. |
| Expected | PUT sends the `updatedAt` value returned by the last GET/PUT for each language row. A stale row returns `409 JDG-202` with no write/audit; local draft remains and no success appears. Reload GET asks before discarding the draft; cancel preserves it, confirm loads new `updatedAt` values without automatic resubmit. A network failure also retains draft; leaving unsaved prompts. Lacking write sends no mutation, lacking read reveals no configuration. |
| Cleanup | restore language config and permission assignments |
| Evidence | retain version/audit/request evidence. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-ADM0201-UM-AC-01-02 — Partial bulk outcome retains failed IDs for retry

| Field | Value |
| :--- | :--- |
| AC / DD source | ADM0201 UM-AC-01; DD §§3,7 USRM-01/04; F1 #37 |
| Kind / priority | Abnormal/recovery; P1 — Bulk action accountability |
| Environment | Admin UI + F1 users/status API integration with audit read. |
| Preconditions | `adm-a` authorized; page 1 currently displays users u-101/u-102; both selected; page size 8. |
| Test data | #37 for u-101 succeeds; #37 for u-102 returns permission error; subsequent retry for u-102 succeeds. |
| Steps | Invoke bulk lock; inspect alert/toast and selected IDs; retry only failed user. |
| Expected | Result reports exactly one success and one failure with per-ID reason in accessible role=alert; successful u-101 is removed from pending selection, u-102 remains selected/retryable; list refresh reflects actual state. No request for off-page user. |
| Cleanup | Restore both user states. |
| Evidence | Request IDs, state before/after, visible selected IDs, accessible alert. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-ADM0201-UM-AC-02-02 — Self-lock requires explicit acknowledgement and returns to sign-in

| Field | Value |
| :--- | :--- |
| AC / DD source | ADM0201 UM-AC-02; DD §§3–4, BR-SC-01; F1 #37 |
| Kind / priority | Security/recovery; P0 — Admin session revocation |
| Environment | Admin UI + F1 #37 integration; disposable second admin session to verify route. |
| Preconditions | `adm-a` is active admin and targets own account; another admin `adm-b` remains active. |
| Test data | Lock own account with reason `QA self-lock`; `notifyByEmail=false` for self-lock case; initial confirmation cancelled, second attempt confirmed. |
| Steps | Open self-lock confirmation; inspect impact warning; cancel and verify unchanged state; reopen and confirm; observe session and route. |
| Expected | Dialog warns current session ends; cancel sends no #37 and restores focus. Confirm sends exactly one #37 with acknowledgeSelfImpact=true; successful lock clears session and routes sign-in. Direct missing/false acknowledgement is rejected without write as module AC-29 specifies; adm-b remains active. |
| Cleanup | Unlock adm-a through adm-b and restore session fixture. |
| Evidence | Dialog text/focus, API call count, status row, session storage and final route. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-ADM0201-UM-AC-02-03 — Self-demotion warning permits change only while another admin remains

| Field | Value |
| :--- | :--- |
| AC / DD source | ADM0201 UM-AC-02; DD §4 BR-SC-01; F1 #36 |
| Kind / priority | Security/boundary; P0 — Avoid admin lockout |
| Environment | Admin UI + F1 role API and role distribution integration. |
| Preconditions | Two active admins adm-a and adm-b; adm-a is signed in and is not sole admin. |
| Test data | Change adm-a role to INSTRUCTOR; separate baseline run has adm-a as final admin. |
| Steps | As adm-a open role change; inspect self-impact warning; cancel once; reopen and confirm. Repeat final-admin case through UI. |
| Expected | Cancel sends no #36. Confirm while adm-b remains sends acknowledgeSelfImpact=true and updates role/table/distribution plus current-session authorization; admin controls disappear. Final-admin client precheck explains block; separately race/bypass is rejected by server with no change. No client-only guard is treated as security proof. |
| Cleanup | Restore adm-a ADMIN role and role distribution. |
| Evidence | Dialog text, requests, role rows/distribution, active-session permissions. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-ADM0401-QM-AC-01-02 — Snapshot retry reloads only failed region

| Field | Value |
| :--- | :--- |
| AC / DD source | ADM0401 QM-AC-01; DD §§2–3,7 QMON-01; F4 §§16–18 |
| Kind / priority | Abnormal/recovery; P1 — Operational visibility |
| Environment | Admin queue-monitor UI + F4 response spies/integration. |
| Preconditions | Queue snapshot fails; jobs and infra summary have rendered successfully with known values. |
| Test data | First snapshot retry returns 503; second retry returns `NORMAL` and known depth; other endpoints remain stable. |
| Steps | Open `/admin/queue-monitor`; activate snapshot retry twice; compare request calls and other regions. |
| Expected | Only snapshot region shows error; jobs and infra-summary remain visible; each retry invokes F4 §16 only; second response replaces snapshot error and renders source values; no fabricated worker detail or status appears. |
| Cleanup | Restore healthy snapshot response. |
| Evidence | Request counts/payloads, rendered values and region state. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-ADM0401-QM-AC-03-02 — Parameter dialog cancel, reopen baseline, validation and save failure

| Field | Value |
| :--- | :--- |
| AC / DD source | ADM0401 QM-AC-03; DD §§2,4–7 BR-SC-02, QMON-04/05; F4 §21 |
| Kind / priority | Boundary/recovery/accessibility; P1 — Queue configuration integrity |
| Environment | Admin UI + F4 settings endpoint spies/integration; keyboard-only. |
| Preconditions | Current server settings: concurrency=4, retryCount=3, intervalSeconds=30, jobsPerPage=25; `adm-a` authorized. |
| Test data | Change concurrency to 5 then cancel; externally update retryCount to 4; reopen; try 0, -1, 1.5, blank; valid draft save gets 500 then succeeds. |
| Steps | Edit/cancel; reopen and inspect baseline; submit invalid values; submit valid full settings during failure then retry; inspect focus. |
| Expected | Cancel sends no PUT and discards local draft; reopen uses latest server baseline including retryCount=4; invalid non-positive/noninteger values are blocked; save failure keeps dialog and entered draft with no success state; success updates snapshot/jobs/settings and focus returns to Config button. |
| Cleanup | Restore original settings. |
| Evidence | GET/PUT payloads, dialog values, validation/focus trace and refreshed blocks. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-ADM0403-LOG-AC-01-02 — Filter change drops stale page and late response

| Field | Value |
| :--- | :--- |
| AC / DD source | ADM0403 LOG-AC-01; DD §§3–4 BR-SC-01, LOG-01/03; F1 #39/#66 |
| Kind / priority | Concurrency/recovery; P1 — Audit list correctness |
| Environment | Admin system-log UI + F1 audit list/SSE response stubs. |
| Preconditions | Initial filter `category=AUTH`, page cursor c-auth; second filter `category=ADMIN`; first query response is delayed. |
| Test data | Auth response contains log-1; admin response contains log-2; delayed auth response resolves after admin. |
| Steps | Load AUTH page; change category to ADMIN; resolve ADMIN then late AUTH; reconnect stream with a replay of log-2 and new log-3. |
| Expected | Filter change removes old cursor/rows; late AUTH response is ignored; list shows log-2 and then log-3 once, without duplicate replay; URL reflects current filter and page cursor. |
| Cleanup | Clear mocks and restore query. |
| Evidence | URLs, cursor/request ordering, rendered row IDs and SSE event IDs. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-ADM0403-LOG-AC-03-02 — Retention change and DELETE policy require two confirmations

| Field | Value |
| :--- | :--- |
| AC / DD source | ADM0403 LOG-AC-03; DD §§2–5 BR-SC-02, LOG-06; F1 #51/#52 |
| Kind / priority | Destructive action/accessibility; P0 — Audit retention loss |
| Environment | Admin UI + F1 retention API integration using disposable retention settings. |
| Preconditions | `adm-a` has SYSTEM_AUDIT_LOG write; saved retentionDays=365, expiredPolicy=ARCHIVE. |
| Test data | Draft retentionDays=90 and policy DELETE; configure second confirmation separately for reduced retention and DELETE transition. |
| Steps | Open settings; lower retention and cancel first confirmation; reopen and confirm first then cancel second; repeat and confirm both; inspect settings and archived/deleted behavior description. |
| Expected | #52 is never called when either confirmation is cancelled; confirmation explains consequence and appears again before final DELETE transition; final confirmed save sends one complete payload updating configuration. Existing rows are not immediately purged: F1 cleanup deletes rows with `created_at < now()-retentionDays` in daily batches of 5,000. UI explains ARCHIVE is selected but no archive job currently stores expired logs and does not promise archival. |
| Cleanup | Restore retentionDays=365 and ARCHIVE via authorized setup. |
| Evidence | Dialog copy, focus return, #52 call count/payload, saved settings and displayed archive explanation. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |


