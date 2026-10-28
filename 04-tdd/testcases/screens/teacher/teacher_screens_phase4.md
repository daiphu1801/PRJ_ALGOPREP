# Teacher screen test cases — Phase 4 draft

Status: **Draft; Chưa thực hiện**. These cases are design artifacts; no FE mock, integration, or E2E run has been performed.

Fixture aliases, independent scenario reset, role assignments and mock/integration evidence follow [fixture conventions](../FIXTURE_CONVENTIONS.md). Per-case Test data overrides file baseline.

## Shared setup

- **Environment:** instructor frontend, API contract stubs for FE checks; integration run additionally requires F1/F2/F4 services and a disposable test database. Use two instructor identities `ins-a` and `ins-b`; do not use production data.
- **Fixture IDs:** classes `c-101`, `c-102`, `c-103`; students `s-201`, `s-202`; problems `p-301`, `p-302`, submissions `sub-401`, `sub-402`. Substitute valid UUIDs in integration. Unless a case specifies failure, return deterministic records with those IDs.
- **Evidence:** capture screen and route, request/response log for named endpoints, and relevant DB/event state for writes. Keep secrets and private submission data redacted.
- **Cleanup:** restore fixture records and permissions; discard disposable DB. All expected errors must leave prohibited writes absent.

## INS0101 — Instructor overview

### TC-INS0101-AC-SC-01-01 — Independent widget failure and scoped ranking
| Field | Value |
| :--- | :--- |
| AC / DD source | `04-tdd/screens/teacher/INS0101_overview.md` AC-SC-01; `03-dd/screens/teacher/INS0101_overview.md` §§2–7, SC-01/02/05; cross-module F1 #56/#45, F2 §55 |
| Kind / priority | Normal; P1 — Data isolation |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | #56 returns 3 classes and `studentCount=2` with `s-201` enrolled in both `c-101` and `c-102`; #45 returns one activity; §55 initially returns 503. On retry return five assigned problems ranked with counts 9,8,7,6,5, while a global-only problem has count 99. |
| Steps | Open `/instructor/overview`; inspect widgets; retry failed assignments after service recovery; navigate away and back after changing fixture counts. |
| Expected | independent calls start; class/activity widgets remain visible through §55 failure; distinct student total is 2; only assignments widget shows retry; retry calls §55 only; recovered list contains exactly the five instructor-scope rows in supplied order, excludes global-only problem; return triggers fresh reads. No fallback sums or fabricated values. |
| Cleanup | restore responses |
| Evidence | retain rendered counts, request traces, and scoped rows. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-INS0101-AC-SC-02-01 — Only approved overview navigation
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-SC-02; DD §§2,4,6–7, SC-03/04 |
| Kind / priority | Normal; P2 — Navigation regression |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | overview has `c-101` and `p-301` rows. |
| Steps | Activate class card and problem row by mouse and keyboard; activate “Quản lý lớp” and “Xem tất cả”; inspect controls. |
| Expected | cards/rows do not navigate or expose link semantics; the two approved links resolve exactly to `/instructor/classes` and `/instructor/assignments`; search and create-class controls are absent; no class/problem-detail request is made by inert cards. |
| Cleanup | return to overview |
| Evidence | capture routes, accessible roles, and network log. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

## INS0201 — Class management

### TC-INS0201-AC-SC-01-01 — Independent class data, filters, and server paging
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-SC-01; DD §§3–7, SC-01/07/08/10 |
| Kind / priority | Normal; P1 — Counts and scope |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | Three classes, studentCount=41 DISTINCT (s-201 belongs to two classes); All roster has 41 distinct rows, server pageSize=20; roster failure isolated from successful summaries. |
| Steps | Open screen; select `c-101` card, another tab, then reselect active card; visit page 2 in All, switch tab. |
| Expected | Counts match #56 without duplicate memberships. Roster error is local; card/tab sync, reselect returns All. All page2 requests pageSize=20 and shows rows21..40; single-class hides class column and tab change resets page1. Totals stay independent of page. |
| Cleanup | restore roster |
| Evidence | capture query params and row count. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-INS0201-AC-SC-02-01 — Create/edit and guarded destructive actions
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-SC-02; DD §§3–7, SC-02/06/09; F1 #20/#21/#22/#27 |
| Kind / priority | Recovery; P1 — Data loss |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | valid class form: name `Algebra A`, description `Fall cohort`, days `[1,3]`, 09:00–10:00, date range `2026-09-01`–`2026-12-31`; existing `c-101` with `s-201`. |
| Steps | Create once; edit and save; reopen and confirm server values; force edit save 500; cancel member removal then confirm; cancel class deletion then confirm. |
| Expected | #20 called once; server-generated invite code is not duplicated client-side; success navigates to `INS0205` with returned classId. Each edit open loads latest server data; failure retains draft/dialog. Cancel buttons call no write endpoint. Confirm removal uses #27 and preserves membership history (`left_at`); confirm delete uses #22 and refreshes list, totals and roster only on success. |
| Cleanup | delete disposable class and restore membership |
| Evidence | record API counts, state before/after. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-INS0201-AC-SC-03-01 — Reject invalid class form locally
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-SC-03; DD §5 and SC-03; F1 validation §5 (`IDT-V14`–`IDT-V16`) |
| Kind / priority | Boundary; P1 — Data integrity |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | create and edit forms; baseline class `c-101` unchanged. |
| Steps | Submit whitespace-only name and a 121-character trimmed name; submit a 501-character description; submit a schedule with `endTime=08:00` before `startTime=09:00`; correct each case to valid boundary values and submit. |
| Expected | Name must be 1–120 characters after trim (`IDT-V14`); optional description allows at most 500 (`IDT-V15`); malformed schedule returns `IDT-V16`. Invalid field gets associated error and focus; no #20/#21 request or persisted change occurs. Corrected values clear validation and can be saved. |
| Cleanup | reset form |
| Evidence | retain field values, focus trace, request log. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-INS0201-AC-SC-04-01 — Invite controls exist only in class detail
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-SC-04; DD INS0201 SC-05 and INS0205 §3 |
| Kind / priority | Security; P2 — Scope leakage |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | `c-101` has active invite `INV-101`. |
| Steps | Inspect class card, action menu, and management form; open class detail. |
| Expected | management screen has no view/copy/regenerate invite control; detail links to `c-101` and is the only screen exposing invite actions. No invite API request occurs from management screen. |
| Cleanup | none beyond route reset |
| Evidence | retain accessible control inventory and request log. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

## INS0202 — Class assignments

### TC-INS0202-AC-SC-01-01 — Assignment filters preserve scope and stale table
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-SC-01; DD §§3–7, SC-01/03 |
| Kind / priority | Recovery; P1 — Scope and stale data |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | `p-301` assigned to `c-101`,`c-102`; withdrawn `p-302`; one metric null; at least 21 rows. Fail one table refresh after initial successful response. |
| Steps | Select `c-101`, search `Algebra`, page forward; switch to All; trigger query failure. |
| Expected | §50 receives server filters and pagination; summary totals stay fixed across pages; single-class view hides class column; withdrawn row remains with withdrawn label; null renders `-`; failed refresh retains prior rows and current filter. No client-side recomputation of totals. |
| Cleanup | restore normal response |
| Evidence | capture query params and visible rows. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-INS0202-AC-SC-02-01 — Assignment batch partial result and exact unassignment pair
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-SC-02; DD §§3–7, SC-02/05–07; F2 §§52–54 |
| Kind / priority | Normal; P1 — Cross-class data mutation |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | c-101 and c-102; for c-101 selectable p-301/p-302/p-303 initially PUBLISHED and unassigned. After selection another actor assigns p-302 and withdraws p-303. p-301 is separately assigned to c-102. Response failedItems p-303 uses PRB-037 per F2 ?53. |
| Steps | Select c-102 then switch c-101 and prove reset; select three initially eligible rows, apply assignment/withdrawal races and submit. In All unassign p-301 from c-101, cancel once then confirm. |
| Expected | Changed class clears selection/query. Response groups p-301 success, p-302 skipped, p-303 failed with actual F2 eligibility code; failed popup remains, refetches summary/table/eligibility. PRB-056 Outbox failure is separately covered by AC-SC-02-03, not attributed to withdrawal. Unassign affects only (c-101,p-301), leaves c-102/history and cancel sends no write. |
| Cleanup | restore class assignments |
| Evidence | retain payloads and DB pair/history snapshot. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

## INS0203 — Class progress

### TC-INS0203-AC-SC-01-01 — One class scope across chart, attention, and table
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-SC-01; DD §§3–7, SC-01/02/06 |
| Kind / priority | Security; P1 — Cross-scope privacy |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | classes `c-101`,`c-102`, different weekly scores and attention rows; six completed ISO weeks, one missing score; table initially on page 2. |
| Steps | select `c-101`, then All; delay `c-101` response until after All response. |
| Expected | #57/#58/table use the selected class UUID together; chart has one series and six completed weeks, missing score is a gap; table resets to page 1 and class column visibility follows scope; late old response cannot replace All data. |
| Cleanup | remove delays |
| Evidence | retain request sequence and rendered values. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-INS0203-AC-SC-02-01 — Search and retries stay within their query
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-SC-02; DD §§3–7, SC-03–05 |
| Kind / priority | Recovery; P2 — Data consistency |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | table has 2 pages; #57 succeeds; #58 returns 503; table returns `s-201` in `c-101` and `s-202` in `c-102`. |
| Steps | Search `  Lee `, change table page; retry #58; open one row and one attention item. |
| Expected | search trims and calls only table query; page change also calls only table query; retry invokes #58 only and leaves chart intact; both links carry exact row/item `classId` and `studentId` into `INS0204`. |
| Cleanup | clear search |
| Evidence | retain network log and target routes. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

## INS0204 — Class student detail

### TC-INS0204-AC-SC-01-01 — Route pair isolation and independent failures
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-SC-01; DD §§3–7, SC-01/02/06 |
| Kind / priority | Security; P0 — Cross-student privacy |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | route A=`(c-101,s-201)`, B=`(c-102,s-202)`; A response delayed, B resolves first; F1 may return 404; separately inject F2 and F4 503. |
| Steps | navigate A then B; resolve A late; replay with F1 404; replay with F2-only and F4-only errors. |
| Expected | only B data remains; F1 404 clears dependent detail and shows not-found; F2/F4 errors affect only their blocks; browser never calls internal F4 P1. No A data is visible under B URL. |
| Cleanup | clear mocks |
| Evidence | retain route/request traces and screenshots. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-INS0204-AC-SC-02-01 — Submission paging, authorized result, and removal
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-SC-02; DD §§3–7, SC-03–07; F1 #27, F4 #11/#12 |
| Kind / priority | Boundary; P1 — History integrity |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | 25 submissions for `(c-101,s-201)`; `scoreRatio=null`; one ungraded submission `sub-401`; caller route is class management. |
| Steps | go to page 2; open `sub-401`; cancel member removal; reopen and confirm. |
| Expected | page 2 calls only F4 #12; null ratio renders `-`; result route uses `sub-401` and target independently authorizes with F4 #11; cancel causes no write; confirm calls #27 once, sets `left_at`, preserves submission history, and returns to originating screen after success. |
| Cleanup | restore membership state |
| Evidence | retain API and DB audit snapshots. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

## INS0205 — Class detail

### TC-INS0205-AC-SC-01-01 — Independent detail regions and out-of-scope class
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-SC-01; DD §§3–7, SC-01 |
| Kind / priority | Security; P0 — Authorization/privacy |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | #19 returns `c-101`; #24 returns 503; #26 returns `s-201`; also prepare `c-102` owned by `ins-b`. |
| Steps | open `c-101`, retry invite region; navigate directly to `c-102` as `ins-a`. |
| Expected | metadata and roster survive invite error; retry calls #24 only; unauthorized class has same not-found state as nonexistent and shows no prior `c-101` values. |
| Cleanup | restore endpoints |
| Evidence | retain screenshots and requests. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-INS0205-AC-SC-02-01 — Edit, invite rotation, clipboard, removal, and deletion
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-SC-02; DD §§3–7, SC-02–06; F1 #21/#22/#24/#27 |
| Kind / priority | Recovery; P1 — Data loss |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | `c-101`, invite `INV-OLD` unexpired, member `s-201`; clipboard permission denied for first attempt. |
| Steps | edit schedule; create new invite; copy invite with clipboard denied; cancel then confirm removal; cancel then confirm delete, with #22 first failing then succeeding. |
| Expected | #21 updates metadata on success; new invite is first while `INV-OLD` remains valid; clipboard denial presents recovery guidance and sends no other API; cancel writes nothing; confirmed removal preserves history; delete failure keeps detail visible, success alone routes to class list and shows success feedback. No success toast on failure. |
| Cleanup | restore test class and invite state |
| Evidence | retain API/DB evidence. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

## INS0206 — Submission management

### TC-INS0206-AC-SC-01-01 — URL owns filters and pagination
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-SC-01; DD §§3–7, SC-01/06; F4 #10 |
| Kind / priority | Boundary; P1 — State integrity |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | `c-101`,`c-102`; URL `/instructor/submissions?classId=c-101&result=accepted&q=%20Lee%20&page=2`. |
| Steps | open URL; change q, class, result, page; use Back/Forward; activate “Xem tất cả bài nộp”. |
| Expected | normalized request carries `classId,result,q,page`; changed filters reset page to 1; Back/Forward restore corresponding table; show-all removes only `classId` and `page`, retaining `q/result`. Unknown result normalizes to `all`, invalid page to 1, q is trimmed/capped at 100 chars. |
| Cleanup | restore route |
| Evidence | capture URL history and F4 #10 calls. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-INS0206-AC-SC-02-01 — Distinct empty states and retry behavior
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-SC-02; DD §§3–7, SC-03/04 |
| Kind / priority | Recovery; P2 — Error-state accuracy |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | sequential responses: no classes; class with no submissions; submissions exist but filter matches none; F4 #10 503; unresolved score and testcase total zero. |
| Steps | visit each state and retry failed table once, with retry succeeding and then failing on a second run. |
| Expected | each empty state has its own contract message; table error preserves URL and filters; retry uses current URL, and toast appears only on repeat failure; ungraded and zero testcase totals display `-`. |
| Cleanup | restore success response |
| Evidence | retain state captures and requests. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-INS0206-AC-SC-03-01 — Row identity controls destinations
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-SC-03; DD §§3–7, SC-05; F4 #11 |
| Kind / priority | Security; P0 — IDOR risk |
| Environment | Instructor/admin frontend; contract stubs for FE checks; integration requires the services listed in Shared setup. |
| Preconditions | Establish the stated actor and initial screen state; use the exact case fixture values below. |
| Test data | row contains `(c-101,s-201,sub-401)` and active filters. |
| Steps | open student and submission links from that row; alter any query `classId` before target result resolves; return Back. |
| Expected | student route receives row class/student IDs; result authorization uses `submissionId=sub-401` rather than trusting query; return restores prior URL filters. No neighboring row ID is substituted. |
| Cleanup | restore route |
| Evidence | retain URL, requests, authorization response. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-INS0201-AC-SC-02-02 — Cancel class deletion and member removal without writes

| Field | Value |
| :--- | :--- |
| AC / DD source | INS0201 AC-SC-02; DD §3 BR-SC-03, EVT-11–13/17–19; F1 #22/#27 |
| Kind / priority | Negative/recovery; P0 — Prevent unintended destructive writes |
| Environment | Instructor UI with mutation request spy; integration variant uses disposable class/member. |
| Preconditions | `ins-a` manages `c-101`; member `s-201` is active; both dialogs can be opened by authorized instructor. |
| Test data | Attempt delete `c-101` and removal of `s-201`; leave each confirmation dialog open then activate Cancel/Escape. |
| Steps | Open class-delete confirmation, record its irreversible warning, cancel; open remove-member confirmation, cancel; repeat cancel with Escape and keyboard focus. |
| Expected | Neither F1 #22 nor #27 is called; class and membership rows, totals and roster remain unchanged; dialogs close and focus returns to each opening control. |
| Cleanup | No data mutation; reset dialog state. |
| Evidence | Mutation call counts, DB snapshots, dialog text and focus trace. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-INS0201-AC-SC-02-03 — Edit failure retains draft; reopening reads latest server state

| Field | Value |
| :--- | :--- |
| AC / DD source | INS0201 AC-SC-02; DD §4 BR-SC-02; F1 #21 |
| Kind / priority | Recovery; P1 — Prevent lost edits/stale form data |
| Environment | Instructor UI + F1 #19/#21 contract stub; integration with disposable class. |
| Preconditions | `c-101` has name `Algebra A`, description `Fall cohort`, valid structured schedule; `ins-a` can update it. |
| Test data | Change name to `Algebra B`; make #21 return 500; then update server fixture externally to description `Updated cohort` before reopening. |
| Steps | Open edit, change name, submit and receive 500; inspect dialog; close/cancel; reopen edit after external server update. |
| Expected | On #21 failure dialog stays open with entered draft and error; no success toast or query invalidation; reopening loads current server description `Updated cohort`, not stale draft `Fall cohort`; no class data is changed by failed write. |
| Cleanup | Restore `c-101` server fixture. |
| Evidence | Request/response, form state before/after, class row snapshot. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-INS0201-AC-SC-03-02 — Class name/description validation blocks submit and focuses first invalid field

| Field | Value |
| :--- | :--- |
| AC / DD source | INS0201 AC-SC-03; DD §5 and SC-03; `03-dd/validation/F1_identity.md` §5 (`IDT-V14`, `IDT-V15`) |
| Kind / priority | Boundary/validation; P1 — Prevent invalid writes |
| Environment | Instructor UI with #20/#21 spies; integration uses disposable form only. |
| Preconditions | `ins-a` opens create form and edit form for unchanged `c-101`; baseline requests are captured. |
| Test data | Whitespace-only name `"   "`; name `" A "` trims to one character; 120-character trimmed name; 121-character trimmed name; optional description omitted; 500-character description; 501-character description. |
| Steps | Submit each invalid create/edit fixture; then correct values and submit. |
| Expected | Name is required and must contain 1–120 characters after trimming; 120 is accepted, empty or 121 returns `IDT-V14`. Description is optional; 500 is accepted, 501 returns `IDT-V15`. Invalid input shows field-associated error, focus lands on the first invalid field, neither #20 nor #21 is called and no row changes; corrected values clear errors and can submit. |
| Cleanup | Reset form; restore baseline if valid submit was run. |
| Evidence | Exact character counts, focus and aria-describedby, request counts, DB state. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-INS0201-AC-SC-03-03 — Invalid structured schedule is rejected before class write

| Field | Value |
| :--- | :--- |
| AC / DD source | INS0201 AC-SC-03; DD §5, SC-03; `03-dd/validation/F1_identity.md` §5 (`IDT-V10`, `IDT-V16`) |
| Kind / priority | Abnormal/boundary; P1 — Schedule integrity |
| Environment | Instructor UI with #20/#21 request spy. |
| Preconditions | Create and edit forms load with no pending request; baseline `c-101` is preserved. |
| Test data | `days=[1,1]`; `room` length 101; `note` length 201; endTime 08:00 before startTime 09:00; startDate 2026-12-31 after endDate 2026-09-01; time present with empty days. |
| Steps | Submit each invalid schedule in create and edit modes; inspect errors, focus, requests and row. |
| Expected | Oversized room/note return `IDT-V10`; malformed schedule (duplicate/out-of-range days, invalid time/date, end before start, or time without day) returns `IDT-V16`. Each invalid form shows associated feedback and focuses the first invalid field; no #20/#21 request or persisted mutation occurs. |
| Cleanup | Reset forms. |
| Evidence | Schedule values, validation messages/focus, request spy and row snapshot. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-INS0202-AC-SC-02-02 — Cancel assignment and unassignment leaves selections and pairs untouched

| Field | Value |
| :--- | :--- |
| AC / DD source | INS0202 AC-SC-02; DD §4 BR-SC-02/03, EVT-2/6–9; F2 §§52–54 |
| Kind / priority | Negative/recovery; P1 — Prevent cross-class assignment change |
| Environment | Instructor UI with assignment/unassignment request spies. |
| Preconditions | `ins-a` manages `c-101`,`c-102`; `p-301` eligible for c-101 and assigned to c-102. |
| Test data | Select `p-301` for c-101; change selected class before submit; open removal for pair `(c-102,p-301)` and cancel. |
| Steps | In assign popup select p-301, switch class and verify selection reset, close/cancel; open unassign confirmation for exact pair, cancel. |
| Expected | No §53 or §54 write call on close/cancel; stale p-301 selection is not carried across class switch; assignment relation `(c-102,p-301)` remains; history unchanged. |
| Cleanup | Reset popup state; no DB cleanup required. |
| Evidence | Selection state, calls/payloads and pair snapshot. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-INS0202-AC-SC-02-03 — PRB-056 partial batch remains retryable and is not reported as full success

| Field | Value |
| :--- | :--- |
| AC / DD source | INS0202 AC-SC-02; DD §4 BR-SC-02; F2 §§52–53; owner decision 2026-10-27 |
| Kind / priority | Abnormal/recovery; P1 — Outbox failure visibility |
| Environment | Instructor UI + F2 assignment API integration. |
| Preconditions | `ins-a` can assign; c-101 selected; p-301 and p-302 are PUBLISHED and eligible. |
| Test data | §53 returns HTTP 200 with `succeededIds=[p-301]`, `failedItems=[{id:p-302,code:PRB-056}]`; second response succeeds p-302. |
| Steps | Submit both; inspect popup and summary; retry p-302 only; inspect refetched list. |
| Expected | p-301 appears successful; p-302 remains visibly failed and retryable, never shown as success from batch HTTP 200; popup remains open while failure exists; retry sends only p-302 and successful retry refreshes summary/table/eligibility. |
| Cleanup | Remove fixture assignments. |
| Evidence | Both request/response payloads, popup state, assignment pairs and refetch calls. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-INS0204-AC-SC-01-02 — Instructor cannot view a student under a class outside their scope

| Field | Value |
| :--- | :--- |
| AC / DD source | INS0204 AC-SC-01; DD §4 BR-SC-01; F1 authorization and F4 #12 scope |
| Kind / priority | Security; P0 — Cross-class student data exposure |
| Environment | Integration with two instructor identities and API/DB reads. |
| Preconditions | `ins-b` owns c-102 and s-202; `(c-101,s-201)` belongs to ins-a; no c-102 membership for s-201. |
| Test data | As ins-b request route `/instructor/classes/c-101/students/s-201`; first seed prior page data for `(c-102,s-202)` in client cache. |
| Steps | Navigate from authorized B route to unauthorized A pair; observe F1 response, all dependent queries, and rendered detail. |
| Expected | Route resolves to the same not-found state as nonexistent pair; cached B data is cleared; no A student profile, assignments, submissions or scores render; browser does not call internal F4 P1 directly. |
| Cleanup | Clear cache and fixture. |
| Evidence | Authorization response, network log, screenshot and cache/state inspection. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-INS0204-AC-SC-02-02 — Cancel student removal returns focus and performs no mutation

| Field | Value |
| :--- | :--- |
| AC / DD source | INS0204 AC-SC-02; DD §§3,6–7, EVT-6; F1 #27 |
| Kind / priority | Negative/accessibility; P1 — Prevent accidental removal |
| Environment | Instructor UI with #27 spy; keyboard-only pass. |
| Preconditions | `ins-a` views `(c-101,s-201)`; membership and submission history are seeded. |
| Test data | Open remove dialog for s-201 and cancel using Cancel, then separately Escape. |
| Steps | Record dialog title/body; cancel each attempt; verify row, history, URL and focus. |
| Expected | #27 is never called; membership `left_at` remains null and history unchanged; dialog names student/class and traps focus; close returns focus to opener; route stays on student detail. |
| Cleanup | Reset dialog state. |
| Evidence | Call count, membership/history snapshot and keyboard/focus trace. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-INS0205-AC-SC-02-02 — Cancel delete/removal and failed delete keep detail route

| Field | Value |
| :--- | :--- |
| AC / DD source | INS0205 AC-SC-02; DD §§2–4, EVT-4/8/9; F1 #22/#27 |
| Kind / priority | Negative/recovery; P0 — Prevent unintended class loss |
| Environment | Instructor UI + F1 mutation stubs/integration. |
| Preconditions | `ins-a` owns c-101 with member s-201 and active invite; current detail route is c-101. |
| Test data | Cancel class delete; cancel member removal; then confirm class delete with #22 returning 500. |
| Steps | Open and cancel each dialog; reopen delete and confirm under failing #22; inspect route/data/toast. |
| Expected | Both cancels call no API and restore focus; failed #22 does not navigate to class list or remove c-101; row stays visible and error toast is shown; no success toast/event. |
| Cleanup | Restore class and member fixture; reset route. |
| Evidence | Request counts, URL, DB state, toast and focus. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-INS0205-AC-SC-02-03 — Invite regeneration failure preserves current invite; clipboard denial is local

| Field | Value |
| :--- | :--- |
| AC / DD source | INS0205 AC-SC-02; DD §§2–4, BR-SC-03; F1 #23/#24 |
| Kind / priority | Recovery/security; P1 — Preserve active access code |
| Environment | Instructor UI + F1 invite API and browser clipboard permission control. |
| Preconditions | c-101 has only active invite `INV-OLD`; `ins-a` has class-management access. |
| Test data | #23 returns 500 on first regeneration; browser clipboard rejects write for INV-OLD. |
| Steps | Attempt regenerate; inspect code list; attempt copy with denied clipboard; inspect outbound API calls and guidance. |
| Expected | Failed regeneration leaves INV-OLD visible and active, adds no new code; clipboard failure shows manual-copy guidance and sends no API containing code. |
| Cleanup | Restore invite fixture and clipboard permission. |
| Evidence | API response, invite rows, clipboard error, rendered guidance and request log. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-INS0206-AC-SC-02-02 — Repeated failed retry keeps URL/filter and only then emits toast

| Field | Value |
| :--- | :--- |
| AC / DD source | INS0206 AC-SC-02; DD §§3–4, BR-SC-02; F4 #10 |
| Kind / priority | Recovery; P1 — Stable filtered work context |
| Environment | Instructor UI + F1/F4 response stubs. |
| Preconditions | URL filters are `classId=c-101,result=ACCEPTED,q=Lee,page=2`; initial F4 call fails. |
| Test data | First retry returns 503; second retry returns 503; third retry succeeds with one row. |
| Steps | Trigger each retry in sequence; inspect URL, query body, toast and table contents. |
| Expected | Initial auto-load failure shows inline QueryError without toast. Every explicit retry uses current normalized URL/filters and each failed explicit retry emits one error toast. Success clears error and renders current rows; no stale cross-filter rows. |
| Cleanup | Restore successful F4 response and original route. |
| Evidence | URL snapshots, requests, toast count and rendered row IDs. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |


