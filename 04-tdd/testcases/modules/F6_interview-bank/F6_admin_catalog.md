# F6 admin: topics and difficulty catalog

## Scope and sources

- Scope: F6 TDD AC-24–29. All cases are design only: **Chưa thực hiện**.
- TDD: `04-tdd/modules/F6_interview-bank.md` AC-24–29 and corresponding trace rows.
- DD: `03-dd/api/F6_interview-bank.md` §9–18, §26–27; `03-dd/logic/F6_interview-bank.md` BR-IB-01–02, BR-IB-13; `03-dd/validation/F6_interview-bank.md` §1, §6, §9; `02-bd/database/F6_interview-bank.md` §1.1, §1.1a, §4; `02-bd/security/F6_interview-bank.md` §2; `03-dd/logic/F1_identity.md` BR-24.
- Decision baseline: DD F6 updated 2026-10-23; catalog role boundary per DEC-2026-1001-admin-configurable-settings; slug immutability/collision behavior per DEC-2026-1023-dd-f5-f6-open-choices and BR-IB-01. No F6 job exists (logic §1).
- Execution status: **Chưa thực hiện**; no run evidence.

## Shared fixtures

- UUID namespace: topics `20000000-0000-4000-8000-0000000000xx`; levels `30000000-0000-4000-8000-0000000000xx`; questions `40000000-0000-4000-8000-0000000000xx`.
- Actors: `A1=10000000-0000-4000-8000-000000000001` STUDENT; `A2=...0002` INSTRUCTOR with all `INTERVIEW_BANK_MANAGEMENT` functions; `A3=...0003` ADMIN with all four functions. Tokens are valid. For catalogue endpoints, A2 must still fail role check; A3 passes both Function and role check.
- Existing topic rows: `T-DB=...0010` code `DATABASE`, display `Database`, STAR false, sort 1; `T-BEH=...0011` code `BEHAVIORAL`, display `Hành vi`, STAR true, sort 2; `T-SD=...0012` code `SYSTEM_DESIGN`, display `System design`, STAR false, sort 3. Existing levels `L-EASY=...0010/EASY/Dễ/sort1`, `L-MED=...0011/MEDIUM/Trung bình/sort2`, `L-HARD=...0012/HARD/Khó/sort3`.
- AuditLogPort can be configured healthy, fail-before-insert, and synchronized concurrent. DB evidence reads `question_topics`, `question_levels`, `interview_questions`, `identity.system_audit_logs`; no direct write to identity schema is part of product behavior.

## Branch inventory

| AC | Branches | Case IDs | Review |
|---|---|---|---|
| AC-24 | all roles read; counts only for ADMIN | TC-F6-AC24-01, -02 | Draft |
| AC-25 | slug/order create; case-insensitive duplicate; rename immutability/collision suffix; name bounds/fallback; audit rollback; level creation and role guards | TC-F6-AC25-01–06 | Reviewed 2026-10-28; not executed |
| AC-26 | STAR toggle preserves stored framework; rename preserves code; audit | TC-F6-AC26-01, -02 | Draft |
| AC-27 | reorder both taxonomies reflected in consumers; invalid full-set rejected atomically; singleton boundary; malformed input | TC-F6-AC27-01–04 | Reviewed 2026-10-28; not executed |
| AC-28 | referenced RETIRED row blocks delete; empty row delete; non-admin rejection; concurrent question create/delete FK | TC-F6-AC28-01–04 | Reviewed 2026-10-28; not executed |
| AC-29 | referenced RETIRED level; unused level delete; last-level guard; concurrent final deletes; rename keeps code; no-op/duplicate/unknown rename | TC-F6-AC29-01–06 | Reviewed 2026-10-28; not executed |

## Cases

### TC-F6-AC24-01 — Authenticated roles can read topic and level catalogs

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-24; F6 API §§9–10; BR-IB-13; IB09-N1, IB10-N1. |
| Kind / priority | Normal/security; authenticated catalog visibility and order; P1. |
| Environment | F6 API integration with PostgreSQL and STUDENT, INSTRUCTOR, ADMIN JWTs. |
| Preconditions | A1, A2, A3 have valid unexpired tokens; catalog rows from shared fixture exist. |
| Test data | Topics T-DB/T-BEH/T-SD at sort orders 1/2/3; levels L-EASY/L-MED/L-HARD at sort orders 1/2/3; no includeCounts parameter. |
| Steps | Each actor calls GET /topics and GET /levels; compare item IDs, codes, names, sortOrder, and response keys. |
| Expected | All authenticated roles receive the same complete catalog arrays ordered by sortOrder. No color field is invented for difficulty. Responses contain no questionCount unless ADMIN explicitly requests includeCounts=true. |
| Cleanup | No writes; revoke disposable tokens if created. |
| Evidence | Six responses, token role claims, ordered IDs and response-key comparison. |
| Design review | Draft; not executed; oracle per API §§9–10. |

### TC-F6-AC24-02 — Question counts are exposed only to ADMIN

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-24; F6 API §§9.2–9.7, 10.2–10.7; BR-IB-13; IB09-A1/B1, IB10-A1. |
| Kind / priority | Boundary/security; role-scoped aggregate fields; P1. |
| Environment | F6 API integration, DB read, three actor tokens. |
| Preconditions | Seed T-DB with one ACTIVE and one RETIRED question; T-BEH with no questions; A1/A2/A3 authenticated. |
| Test data | Each actor requests topics and levels with includeCounts=true; count should include all statuses only for ADMIN per DD. |
| Steps | Call both list endpoints for A1, A2, A3 using includeCounts=true; inspect count fields and compare DB counts. |
| Expected | A3 receives questionCount values including RETIRED rows per API §9.5/§10.5. A1 and A2 do not receive questionCount (A2 parameter is ignored). Catalog order remains unchanged. |
| Cleanup | No writes; remove seeded question rows if created for this case. |
| Evidence | Requests/responses by actor and read-only SQL count snapshot. |
| Design review | Draft; not executed; no open oracle. |

### TC-F6-AC25-01 — Create a topic generates slug and appends sort order

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-25; F6 API §11; BR-IB-01/02; IB11-N1. |
| Kind / priority | Normal; catalog creation and audit; P1. |
| Environment | F6 API integration, PostgreSQL, healthy AuditLogPort, A3 with CREATE and ADMIN role. |
| Preconditions | Shared T-DB/T-BEH/T-SD exist; max topic sortOrder is 3; “Mạng máy tính” does not exist. |
| Test data | POST /topics {"displayName":"Mạng máy tính"}. |
| Steps | Submit once; GET /topics; query created row and audit log. |
| Expected | 201; generated code is MANG_MAY_TINH; sortOrder=4; usesStarFramework=false; questionCount=0; exactly one INTERVIEW_TOPIC_CREATED audit identifies A3 and new ID. |
| Cleanup | Delete the new topic after evidence; it has no question references. |
| Evidence | Request/response, topic row and audit event. |
| Design review | Draft; not executed; slug and append order per DD. |

### TC-F6-AC25-02 — Duplicate name ignoring case is rejected

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-25; F6 API §11.2–11.7; BR-IB-02; IB11-A2. |
| Kind / priority | Abnormal/concurrency-safe uniqueness; P1. |
| Environment | F6 API integration with PostgreSQL unique lower(display_name) index and AuditLogPort. |
| Preconditions | T-DB displayName is Database; A3 has CREATE. |
| Test data | Subcase A: POST /topics {"displayName":"database"}. Subcase B: two concurrent creates with names `Network` and `network`. |
| Steps | Submit duplicate-case name; for race branch release both create requests concurrently; inspect all rows and audit. |
| Expected | Duplicate returns 409 INB-020; concurrent case permits at most one persisted case-insensitive name and loser also receives INB-020. Failed attempt creates no audit. |
| Cleanup | Delete any successfully created concurrent test topic after evidence. |
| Evidence | Responses, lower(display_name) query, audit delta and concurrent request timing. |
| Design review | Draft; not executed; expected uniqueness enforced by DD unique index. |

### TC-F6-AC25-03 — Slug collision suffix, slug fallback and rename immutability

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-25; F6 API §§11–12, 15–16; BR-IB-01; IB11-B1, IB12-N1. |
| Kind / priority | Boundary/normal; stable slug on rename and collision suffix; P1. |
| Environment | API integration + PostgreSQL + audit read. |
| Preconditions | A3 has CREATE/UPDATE; no existing displayName or code for the three new topics; `CO_SO_DU_LIEU`, `CO_SO_DU_LIEU_2` and `TOPIC` are unused. |
| Test data | Create `Cơ sở dữ liệu`; create distinct displayName `Co so du lieu` (same generated base slug, different lower(display_name)); create `???`; rename first topic to `CSDL`. |
| Steps | Perform creates and rename; read rows and GET /topics after each. |
| Expected | First code is CO_SO_DU_LIEU; second is CO_SO_DU_LIEU_2; all-symbol name uses TOPIC fallback. All three create requests succeed with 201; rename changes displayName but leaves code and ID unchanged. Repeating an identical displayName belongs to duplicate-name rejection, not this slug collision. One audit for each successful write. |
| Cleanup | Delete disposable topics after confirming no references. |
| Evidence | Responses, IDs/codes before and after rename, sort order and audits. |
| Design review | Draft; not executed. Slug conversion details follow BR-IB-01; suffix collision ordering confirmed by contract. |

### TC-F6-AC25-04 — Topic display name length boundary and rejected names

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-25; API §§11–12; BR-IB-02; IB11-B1. |
| Kind / priority | Boundary/validation; P1. |
| Environment | F6 API integration + DB/audit read. |
| Preconditions | A3 has topic CREATE; no duplicate test name. |
| Test data | Submit trimmed names of exactly 60 chars, 61 chars, blank/whitespace, and `???`; use unique prefixes for accepted values. |
| Steps | POST each name; query topic count and audit delta after each. |
| Expected | 60-character trimmed name is accepted; 61 and empty-after-trim return 400 INB-V03; rejected requests create no row/audit; `???` is accepted with code TOPIC (detailed in TC-AC25-03). |
| Cleanup | Delete accepted test topics. |
| Evidence | Character counts, responses, row/audit deltas. |
| Design review | Draft; not executed; bounds are fixed by API §11.2. |

### TC-F6-AC25-05 — Audit failure rolls back topic creation

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-25; F6 API §11.5; BR-IB-02; F1 BR-24; IB11-A3. |
| Kind / priority | Recovery/transaction; P0. |
| Environment | Integration API, DB, AuditLogPort fail-before-insert hook. |
| Preconditions | A3 authorized; unique valid topic name; snapshot topic rows and audit count. |
| Test data | POST /topics {"displayName":"Rollback topic"}; configure AuditLogPort to throw before insert. |
| Steps | Submit create and inspect response, topic table, and audit. |
| Expected | `503 INB-503`; topic INSERT rolls back; no audit row and no visible partial catalog entry, per the owner-approved F6 audit failure contract in API §27. |
| Cleanup | Disable fault; no data cleanup expected. |
| Evidence | Error/correlation logs, before/after topic rows and audit count. |
| Design review | Reviewed against API §27 and the approved audit-failure oracle; not executed. |

### TC-F6-AC26-01 — STAR toggle changes learner flag without changing answer framework

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-26; F6 API §§12, 3; BR-IB-02/05; DB F6 §1.2. |
| Kind / priority | Normal; metadata-only presentation toggle; P1. |
| Environment | API integration plus learner question detail endpoint and DB. |
| Preconditions | T-BEH STAR=true; active behavior question has sampleAnswerFramework exactly `Situation\nTask\nAction\nResult`; A3 UPDATE. |
| Test data | PATCH T-BEH {"usesStarFramework":false}; GET learner detail for seeded question. |
| Steps | Snapshot question framework; toggle STAR off; fetch topic and question detail; inspect audit. |
| Expected | Topic flag becomes false; learner detail reflects false; stored framework remains byte-for-byte unchanged; exactly one INTERVIEW_TOPIC_UPDATED audit. |
| Cleanup | Restore STAR=true. |
| Evidence | Before/after topic and question rows, endpoint response and audit. |
| Design review | Draft; not executed; per DD STAR controls rendering only. |

### TC-F6-AC26-02 — Topic rename preserves code and no-op update creates no audit

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-26; F6 API §12; BR-IB-01/02; IB12-N1/B1. |
| Kind / priority | Normal/boundary; stable code and idempotent audit; P2. |
| Environment | API integration + PostgreSQL + audit query. |
| Preconditions | T-BEH displayName `Hành vi`, code `BEHAVIORAL`; A3 UPDATE. |
| Test data | PATCH displayName to `Soft skills`; repeat identical PATCH with same STAR value. |
| Steps | Apply rename; inspect row/audit; submit no-op update; inspect audit delta. |
| Expected | First update preserves ID/code and adds one audit containing before/after; no-op returns 200 but adds no audit. |
| Cleanup | Restore original displayName and STAR flag. |
| Evidence | Responses, topic rows, audit entries/count. |
| Design review | Draft; not executed; no open oracle. |

### TC-F6-AC27-01 — Reorder topics and levels is reflected in read surfaces

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-27; F6 API §§13,17,19; BR-IB-02; IB13-N1, IB17-N1. |
| Kind / priority | Normal; shared catalog ordering; P1. |
| Environment | API integration, DB, learner list/filter and admin question-list read surfaces. |
| Preconditions | Three topics/levels and questions across levels as shared fixtures; A3 UPDATE. |
| Test data | PUT topics/order `[T-SD,T-DB,T-BEH]`; PUT levels/order `[L-HARD,L-EASY,L-MED]`. |
| Steps | Save baseline; submit both orders; read catalogs, authoring selectors and admin questions sorted by level. |
| Expected | Both return full arrays; persisted sortOrder becomes 1..3; topic and level consumers use updated order; admin level sort follows level sort_order, not alphabetical display name; one audit per reorder. |
| Cleanup | Restore original topic and level order. |
| Evidence | Requests/responses, sort_order rows, downstream arrays and audit entries. |
| Design review | Draft; not executed; oracle per API §§13/17 and §19. |

### TC-F6-AC27-02 — Incomplete, duplicate or extra IDs reject reorder atomically

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-27; F6 API §§13.2–13.5,17; validation §6. |
| Kind / priority | Abnormal/atomicity; P1. |
| Environment | API integration + PostgreSQL + audit query. |
| Preconditions | Three current topics; save baseline order; A3 UPDATE. |
| Test data | Submit incomplete `[T-DB,T-BEH]`, extra `[T-DB,T-BEH,T-SD,T-UNKNOWN]`, and duplicate `[T-DB,T-BEH,T-BEH]`. |
| Steps | Call reorder endpoint for each body; read all sortOrder values and audit after each. |
| Expected | Each valid-UUID but wrong set returns 422 INB-034; persisted order remains baseline and no audit is added. Malformed UUID is a separate 400 INB-V14 input branch and is not conflated with this test. |
| Cleanup | No mutation expected. |
| Evidence | Responses, before/after order snapshots and audit count. |
| Design review | Draft; not executed; no open oracle. |

### TC-F6-AC27-03 — One-item catalog can be reordered with its complete singleton set

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-27; F6 API §§13.2–13.5,17. |
| Kind / priority | Boundary; minimum full-set cardinality; P2. |
| Environment | Isolated F6 integration database + PostgreSQL. |
| Preconditions | Isolated DB contains exactly one topic `T-ONLY` and one level `L-ONLY`; A3 UPDATE. |
| Test data | PUT each order endpoint with its complete singleton array. |
| Steps | Submit both orders; read response and rows. |
| Expected | Each returns 200 with sole ID at sortOrder 1 and one reorder audit; no neighboring production/shared fixture is modified. |
| Cleanup | Drop isolated database fixture. |
| Evidence | Responses, rows, and audit entries. |
| Design review | Draft; not executed; singleton set is complete and valid per API. |

### TC-F6-AC28-01 — RETIRED question still blocks topic deletion with exact count

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-28; F6 API §14; BR-IB-02; DB F6 §1.1; IB14-A1. |
| Kind / priority | Abnormal/referential integrity; P1. |
| Environment | API integration + PostgreSQL + audit query. |
| Preconditions | A3 DELETE; topic `T-USED` has exactly one RETIRED question reference. |
| Test data | DELETE /topics/T-USED. |
| Steps | Call delete; inspect response, topic/question rows and audit. |
| Expected | 422 INB-030 with `referencingQuestionCount=1`; topic and RETIRED question remain; no delete audit. |
| Cleanup | No mutation; retain retired question. |
| Evidence | Response, read-only FK/reference counts and audit absence. |
| Design review | Draft; not executed; all statuses count per API §14.5. |

### TC-F6-AC28-02 — Empty topic deletes and is absent from catalog

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-28; F6 API §14; BR-IB-02; IB14-N1. |
| Kind / priority | Normal; valid catalog deletion; P1. |
| Environment | API integration + PostgreSQL + AuditLogPort. |
| Preconditions | A3 DELETE; T-EMPTY exists with zero question references. |
| Test data | DELETE /topics/T-EMPTY; then GET /topics. |
| Steps | Delete topic; list topics; inspect audit. |
| Expected | 204; topic is absent from catalog; exactly one INTERVIEW_TOPIC_DELETED audit targets T-EMPTY. |
| Cleanup | No mutation after evidence. |
| Evidence | Response, catalog list, topic row absence and audit. |
| Design review | Draft; not executed; no open oracle. |

### TC-F6-AC28-03 — Instructor cannot delete an empty topic through direct API

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-28; F6 API §§14.1,14.4–14.5; BR-IB-02; security F6 §2.1. |
| Kind / priority | Security; P1. |
| Environment | Direct API integration, DB and audit query. |
| Preconditions | T-EMPTY exists and has no question references; A2 has DELETE Function but role INSTRUCTOR. |
| Test data | A2 directly calls DELETE /topics/T-EMPTY. |
| Steps | Invoke endpoint without UI; query topic and audit. |
| Expected | 403 INB-302; topic remains unchanged; no audit row. |
| Cleanup | Delete T-EMPTY with A3 if it is disposable. |
| Evidence | Token claims, response, topic row and audit absence. |
| Design review | Draft; not executed; ADMIN role check is mandatory in use case. |

### TC-F6-AC29-01 — RETIRED question still blocks level deletion

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-29; F6 API §18; BR-IB-02; DB F6 §1.1a; IB18-A1. |
| Kind / priority | Abnormal/referential integrity; P1. |
| Environment | API integration + PostgreSQL + audit query. |
| Preconditions | At least two levels remain; L-USED is referenced by exactly one RETIRED question; A3 DELETE. |
| Test data | DELETE /levels/L-USED. |
| Steps | Call delete and inspect response, rows and audit. |
| Expected | 422 INB-031 with reason IN_USE and referencingQuestionCount=1; level remains; no audit. |
| Cleanup | No mutation; preserve reference fixture. |
| Evidence | Response, DB reference count and audit absence. |
| Design review | Draft; not executed; RETIRED references count per API §18. |

### TC-F6-AC29-02 — Unreferenced level can be deleted

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-29; F6 API §18.3–18.7; BR-IB-02; IB18-N1. |
| Kind / priority | Normal; valid level deletion; P1. |
| Environment | API integration + PostgreSQL + AuditLogPort. |
| Preconditions | At least two levels remain; L-EXTRA is unreferenced; A3 DELETE. |
| Test data | DELETE /levels/L-EXTRA; then GET /levels. |
| Steps | Delete; list remaining levels; inspect audit. |
| Expected | 204; L-EXTRA absent; at least one other level remains; exactly one INTERVIEW_LEVEL_DELETED audit. |
| Cleanup | Restore isolated level fixture or drop DB. |
| Evidence | Response, remaining rows and audit event. |
| Design review | Draft; not executed; no open oracle. |

### TC-F6-AC29-03 — Last remaining level cannot be deleted

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-29; F6 API §18.4–18.5; BR-IB-02; IB18-A2. |
| Kind / priority | Boundary/invariant; P1. |
| Environment | Isolated F6 integration database. |
| Preconditions | Exactly one unreferenced level L-ONLY exists; A3 DELETE. |
| Test data | DELETE /levels/L-ONLY. |
| Steps | Call delete; query level row and audit. |
| Expected | 422 INB-032 with reason LAST_LEVEL; sole level remains; no audit. |
| Cleanup | Drop isolated DB fixture. |
| Evidence | Response, row and audit absence. |
| Design review | Draft; not executed; minimum-one invariant per API §18.5. |

### TC-F6-AC29-04 — Two concurrent deletions cannot leave zero levels

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-29; F6 API §18.5–18.7; BR-IB-02; IB18-B1. |
| Kind / priority | Concurrency; catalog minimum invariant; P1. |
| Environment | API integration + PostgreSQL; two synchronized ADMIN sessions. |
| Preconditions | Exactly two unreferenced levels L-A/L-B remain; both A3 sessions have DELETE. |
| Test data | Concurrent DELETE requests for L-A and L-B released at barrier. |
| Steps | Execute both requests; wait for commit; inspect responses, remaining levels and audit. |
| Expected | Exactly one returns 204 and one returns 422 INB-032; exactly one level remains and exactly one delete audit exists. |
| Cleanup | Restore isolated seed levels or drop DB. |
| Evidence | Both responses, transaction/lock timeline, remaining row count and audit. |
| Design review | Draft; not executed; expected serialization follows `FOR UPDATE` contract. |

### TC-F6-AC29-05 — Renaming a level does not change its code

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-29; F6 API §16; BR-IB-01/02; IB16-N1. |
| Kind / priority | Normal; stable level identifier; P2. |
| Environment | API integration + PostgreSQL + audit query. |
| Preconditions | L-HARD displayName `Khó`, code `HARD`, sortOrder=3; A3 UPDATE. |
| Test data | PATCH /levels/L-HARD {"displayName":"Nâng cao"}. |
| Steps | Rename; GET /levels; inspect row and audit. |
| Expected | 200; displayName changes; ID, code `HARD`, and sortOrder remain; exactly one INTERVIEW_LEVEL_RENAMED audit. |
| Cleanup | Restore displayName `Khó`. |
| Evidence | Request/response, before/after row and audit entry. |
| Design review | Draft; not executed; code immutability per BR-IB-01. |

### TC-F6-AC25-06 — Level creation, duplicate name and caller guards

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-25; API §15; validation §6; BR-IB-01/02; IB15-N1/A1/A2. |
| Kind / priority | Normal / Abnormal / Security; P1 |
| Environment | F6 API + PostgreSQL + audit spy; fresh isolated catalog |
| Preconditions | A3 ADMIN has CREATE; A2 INSTRUCTOR also has CREATE; A1 lacks CREATE. Three levels with max sortOrder=3; no name `Rất khó` or code RAT_KHO. |
| Test data | POST /levels body `{"displayName":"Rất khó"}`; duplicate `{"displayName":"rất khó"}`; same valid body under A2/A1. |
| Steps | A3 creates once then sends duplicate; A2 and A1 send valid unique names; compare rows/audit after each. |
| Expected | A3 gets 201 with code RAT_KHO, sortOrder=4 and one INTERVIEW_LEVEL_CREATED audit. Duplicate gets 409 INB-021. A2 gets 403 INB-302; A1 gets 403 INB-301. Rejected calls create no level or audit. |
| Cleanup | Delete isolated created level and audit fixture. |
| Evidence | Four responses, actor claims, level/audit deltas. |
| Design review | Reviewed against API §15; not executed. |

### TC-F6-AC27-04 — Malformed reorder input is distinct from wrong full set

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-27; API §§13/17; validation §6 INB-V14/INB-034 |
| Kind / priority | Abnormal / Boundary; P1 |
| Environment | F6 API + PostgreSQL + audit spy |
| Preconditions | A3 has UPDATE; three valid catalog entries in each taxonomy; save orders/audit counts. |
| Test data | For each order endpoint send `{}`, `{"orderedIds":["not-a-uuid"]}`, and `{"orderedIds":[]}` in independent subcases. |
| Steps | PUT /topics/order and /levels/order for each fixture; read catalogs/audit after each response. |
| Expected | Missing orderedIds and malformed UUID return 400 INB-V14; empty array is a valid UUID array but not the existing full set, so returns 422 INB-034. Every rejected request preserves order and creates no audit. |
| Cleanup | Restore spies; no product mutation expected. |
| Evidence | Six responses, saved orders and audit deltas. |
| Design review | Reviewed against validation §6; not executed. |

### TC-F6-AC28-04 — Concurrent topic deletion and question creation preserve references

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-28; API §14.5/14.7 IB14-B1 and §21; validation §5 INB-035 |
| Kind / priority | Concurrency / Recovery; P1 |
| Environment | F6 API + real PostgreSQL, two transaction barriers, audit spy |
| Preconditions | A3 has CREATE/DELETE; empty T-RACE topic; valid other level; no question currently references T-RACE. |
| Test data | DELETE /topics/T-RACE races POST /admin/questions with title `FK race`, topicId T-RACE, valid levelId and empty rubric. |
| Steps | Run two independent orders: create commits before delete; delete commits before create. Use transaction barriers and then release the losing transaction. |
| Expected | Create-first: create 201 and delete 422 INB-030 with count=1; both question and topic remain. Delete-first: delete 204 and create 422 INB-035 (`details.field=topicId`); topic/question absent. No orphan FK and no audit for the rejected mutation in either order. |
| Cleanup | Delete surviving question/topic and isolated audit fixtures. |
| Evidence | Response pairs, commit timeline, FK/reference checks and audits. |
| Design review | Reviewed against API §14 FK safety net; not executed. |

### TC-F6-AC29-06 — Level rename no-op, duplicate and unknown ID

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-29; API §16; validation §6; IB16-B1/A1 |
| Kind / priority | Boundary / Abnormal; P1 |
| Environment | F6 API + PostgreSQL + audit spy |
| Preconditions | A3 UPDATE; levels HARD=`Khó`, EASY=`Dễ`; unknown UUID U has no level. |
| Test data | PATCH /levels/HARD with `{"displayName":"Khó"}` then `{"displayName":"dễ"}`; PATCH /levels/U with `{"displayName":"Unique level"}`. |
| Steps | Snapshot levels/audits; run three independent requests; read after each. |
| Expected | Identical name returns 200 with no audit; duplicate case-insensitive name returns 409 INB-021; unknown ID returns 404 INB-012. All preserve original names/codes/order and create no audit. |
| Cleanup | Restore spies; no data mutation expected. |
| Evidence | Responses and before/after level/audit snapshots. |
| Design review | Reviewed against API §16; not executed. |
