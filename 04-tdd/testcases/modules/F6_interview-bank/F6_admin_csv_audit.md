# F6 CSV import, audit, and catalog authorization — test cases

Status: **Draft; Chưa thực hiện**.

## Shared fixture and environment

- **Environment:** integration frontend CSV parser + F6 API + PostgreSQL + F1 `AuditLogPort`; disposable DB. Actors `G1` instructor with question CREATE and `Q` ADMIN with catalog functions. Topics: `T-DB` (`DATABASE`, display `Database`); level `L-MED` (`MEDIUM`, display `Trung bình`).
- **Evidence:** exact uploaded bytes/encoding, parsed row payloads, API responses, DB rows and audit logs with actor/action/target/time. Do not include learner personal content.
- **Cleanup:** remove imported questions and disposable catalog records after snapshots. Failure scenarios must show no unintended writes.

## AC-36 — CSV import

### TC-F6-AC36-01 — Mixed CSV rows import valid records and report line-specific skips

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-36; API §24; BR-IB-11; IB24-N1/A1. |
| Kind / priority | Normal + abnormal; P1. |
| Environment | Frontend CSV parser + F6 import API + PostgreSQL + F1 AuditLogPort spy, disposable DB. |
| Preconditions | G1 has CREATE |
| Test data | existing ACTIVE question title `Normalize joins`; CSV has header `topic,level,question,rubric,follow_ups,content,suggested_approach,core_keywords,sample_answer`; data rows lines 2–6: valid `Index tradeoffs` rubric `A:30\|B:40\|C:30`; unknown topic; rubric `A:30\|B:30`; title `nOrMaLiZe Joins` duplicate; valid `Foreign key reasoning` plus follow-ups, approach and keywords. |
| Steps | upload file through import UI, inspect preview/requests; execute import; inspect imported details, learner learning mode, and audit. |
| Expected | two rows imported; skippedRows identify source lines 3,4,5 with `unknownTopic`, `rubricSum`, `duplicate` and useful detail; duplicate does not overwrite original; imported rows each get one `INTERVIEW_QUESTION_IMPORTED` audit; CSV-only approach/keywords and follow-ups are retained and visible in learning detail; request contains parsed rows, not raw file bytes. |
| Cleanup | delete two imported questions |
| Evidence | retain original duplicate row hash, file bytes, parsed body, response and audits. |
| Design review | Draft; not executed; no execution evidence. Cited DD oracle captured; branch remains unexecuted. |

### TC-F6-AC36-02 — Reimport is idempotent-by-duplicate and does not overwrite

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-36; API §24.1, §24.5; BR-IB-11; IB24-B2. |
| Kind / priority | Recovery/idempotency; P1. |
| Environment | integration API + DB/audit. |
| Preconditions | use a two-row file already imported |
| Test data | alter one existing row's title-associated content/keywords in DB fixture after first import to known `Original` values. |
| Steps | submit exact same file a second time; inspect `importedCount`, skips, rows and audit delta. |
| Expected | `importedCount=0`; every valid row is `duplicate`; no content, keywords, rubric or CSV-only fields are overwritten; no new audit entries or display numbers. |
| Cleanup | restore/remove imported questions |
| Evidence | retain before/after row hashes and audit count. |
| Design review | Draft; not executed; no execution evidence. Cited DD oracle captured; branch remains unexecuted. |

### TC-F6-AC36-03 — CSV parser handles delimiters, quoting, blank optional fields, and row numbers

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-36; API §24.2; BR-IB-11; BD SHR0301 Q11; source parser contract. |
| Kind / priority | Boundary; silent field corruption, P1. |
| Environment | frontend CSV parser plus API integration. |
| Preconditions | UTF-8 CSV parser and F6 API available; topic `Database` and level `Trung bình` exist; `MissingTopic` does not. |
| Test data | Header is physical line 1: `topic,level,question,rubric,content,follow_ups`. Valid record starts line 2 with `Database,Trung bình,"Q, one",A:100,"First` and continues on line 3 with `second",Follow 1\|Follow 2`. A structurally valid unknown-topic record starts physical line 4: `MissingTopic,Trung bình,Q two,A:100,,`. A separate valid record has an empty `rubric` cell. |
| Steps | Upload/parse CSV and inspect preview/POST rows; assert the valid multiline record has `line=2` and its content includes one newline. Send the unknown-topic row with `line=4` to server and inspect `skippedRows`; compare parsed fields to source bytes. |
| Expected | Quoted comma/newline stay in one field; pipe splits the two follow-ups; rubric colon splits name/weight; blank optional fields map to empty/default values; empty rubric is allowed. `ImportRow.line=2` for the multiline record; `skippedRows[].line=4` with `reasonCode=unknownTopic` for the next record, counting header as line 1. |
| Cleanup | remove valid imported row |
| Evidence | retain exact bytes, parser output and displayed source line. |
| Design review | Draft; physical-line oracle chốt 2026-10-27; not executed |

### TC-F6-AC36-04 — Server revalidates malformed rows while valid row still imports

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-36; API §24.2–24.4; BR-IB-11. |
| Kind / priority | Abnormal; trust boundary, P1. |
| Environment | direct API integration, bypass frontend parser. |
| Preconditions | G1 CREATE |
| Test data | existing T-DB/L-MED; rows include valid row, unknown level, missing title, invalid rubric sum, title length 256, and duplicate against earlier row in same request. |
| Steps | POST all rows with line identifiers 2–7; inspect response and DB. |
| Expected | valid row imported; each invalid row skipped with corresponding reason (`unknownLevel`,`missingField`,`rubricSum`,`invalidField`,`duplicate`) and original line; server does not trust client validation; only valid question/rubric rows and one audit entry exist. |
| Cleanup | remove imported row |
| Evidence | retain response, DB and audit diff. |
| Design review | Draft; not executed; no execution evidence. Cited DD oracle captured; branch remains unexecuted. |

### TC-F6-AC36-05 — 500 row maximum and empty payload reject whole file

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-36; API §24.2/24.4; IB24-A2. |
| Kind / priority | Boundary/atomic rejection; P1. |
| Environment | API integration + DB/audit. |
| Preconditions | Authorized CREATE actor; distinct valid row titles; body stays below5 MiB; clean isolated database for each subcase. |
| Test data | Independent bodies with rows counts500,501,0 and missing rows; each valid row has topic Database, level Trung bình, unique title and one Explanation:100 rubric. |
| Steps | POST each body; query imported titles and audit rows after each. |
| Expected | Exactly500 rows succeeds with200/importedCount500 and500 import audits. Counts501/0 or missing rows return400 INB-V15 before writes/audit. Valid maximum and first rejected cardinality are checked independently from body size. |
| Cleanup | Delete the500 successfully imported questions/rubrics/audits after evidence. |
| Evidence | retain response and no-write queries. |
| Design review | Draft; not executed; no execution evidence. Cited DD oracle captured; branch remains unexecuted. |

### TC-F6-AC36-06 — Concurrent overlapping imports serialize duplicate detection

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-36; API §24.5; BR-IB-11 advisory lock. |
| Kind / priority | Concurrency; duplicate data/audit, P1. |
| Environment | two concurrent API clients, PostgreSQL, synchronized barrier. |
| Preconditions | same one-row title `Concurrent index question`, absent initially |
| Test data | Two G1/G2 CREATE tokens; both POST the same `rows:[{line:2,topic:"Database",level:"Trung bình",question:"Concurrent index question",rubric:[{criterionCode:"Explanation",weightPercent:100}]}]`. |
| Steps | release two identical import requests simultaneously; inspect both responses, DB, audits and display numbers. |
| Expected | Advisory lock serializes imports. Winner returns `200 {importedCount:1,skippedRows:[]}`; loser observes the committed ACTIVE title and returns `200 {importedCount:0,skippedRows:[{line:2,reasonCode:"duplicate",detail:...}]}`. Exactly one question and one import audit exist; no duplicate ACTIVE title or sequence gap from a committed duplicate. |
| Cleanup | remove inserted row |
| Evidence | retain transaction timeline, responses and unique-title query. |
| Design review | Draft; 200/duplicate follows API §24.3 and BR-IB-11 advisory lock; not executed |

### TC-F6-AC36-07 — Import body accepts 5 MiB and rejects the next byte

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-36; API §24.4, BR-IB-11; `DEC-2026-1028-qa-f6-technical-limits`. |
| Kind / priority | Boundary/atomic rejection; P1. |
| Environment | Direct F6 import API + PostgreSQL + F1 audit spy, without a lower proxy cap. |
| Preconditions | G1 has CREATE; T-DB/L-MED exist; distinct valid one-row import titles A and B do not exist. |
| Test data | Build valid UTF-8 JSON `{ "rows": [...] }` bodies with one legal row and JSON whitespace padding after the closing brace. A is exactly 5242880 bytes, B is 5242881 bytes and has a distinct title; both remain below the 500-row limit. Record exact byte lengths. |
| Steps | POST A then B; inspect HTTP responses, import counts, question/rubric rows, display numbers and import audit delta. |
| Expected | A returns `200` with `importedCount=1` and one question/rubric plus one import audit. B returns `413 INB-V17` before parsing/writing, with no B question/rubric/audit or committed display number. The cap is applied to the entire UTF-8 request body. |
| Cleanup | Remove A in disposable fixture; B requires no cleanup. |
| Evidence | Retain request hashes/byte lengths, responses, DB and audit diff. |
| Design review | Draft; owner confirmed 5 MiB cap 2026-10-28; not executed. |

### TC-F6-AC37-01 — Each successful question and catalog mutation has exactly one audit

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-37; API §§11–18,21–24,26; BR-IB-13; F1 logic BR-24. |
| Kind / priority | Normal; audit completeness, P1. |
| Environment | API integration + audit read. |
| Preconditions | Q has all catalog rights |
| Test data | G1 has question management rights; baseline audit cursor/time recorded. |
| Steps | create/update/soft-delete a question; import two valid questions; create/rename/reorder/delete an unused topic and level (include STAR update); inspect audit delta. |
| Expected | exactly one row per successful operation and per imported question; action type and target type/ID match operation; actor is actual caller and timestamp is populated; before/after snapshots contain only contract fields. No duplicate audit row for a single mutation. |
| Cleanup | remove test entities after log snapshot |
| Evidence | retain ordered audit delta and mutation responses. |
| Design review | Draft; not executed; no execution evidence. Cited DD oracle captured; branch remains unexecuted. |

### TC-F6-AC37-02 — User practice actions do not create admin audit rows

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-37; BR-IB-13; API §§4–8. |
| Kind / priority | Negative control/privacy; P2. |
| Environment | integration API + audit query. |
| Preconditions | S1 can read active question and has rubric |
| Test data | audit cursor captured. |
| Steps | S1 self-rates recall, bookmarks, submits practice answer, reads history/progress; inspect F6-related system audit rows. |
| Expected | learner operations succeed per their own API contracts but create no admin catalog/question audit action; learner identity/state remains scoped to S1. |
| Cleanup | remove learner fixtures |
| Evidence | retain audit query and operation IDs. |
| Design review | Draft; not executed; no execution evidence. Cited DD oracle captured; branch remains unexecuted. |

### TC-F6-AC37-03 — Audit port failure rolls back question creation

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-37; API §21.5/26; BR-IB-03/13; F1 BR-24. |
| Kind / priority | Recovery/transaction; P0. |
| Environment | integration API + DB with AuditLogPort fail-before-insert hook. |
| Preconditions | G1 CREATE, valid topic/level/title/rubric |
| Test data | audit writer deterministically throws before insert. |
| Steps | POST create question; inspect response, question/rubric rows, audit table and sequence behavior. |
| Expected | `503 INB-503`; question and rubric inserts roll back together; no audit row and no partially visible question. The response contains no success item. |
| Cleanup | disable fault |
| Evidence | retain exception correlation, transaction logs and DB absence checks. |
| Design review | Draft; `503 INB-503` chốt 2026-10-27; not executed |

### TC-F6-AC37-04 — Audit port failure rolls back update and preserves CSV fields/history

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-37; API §22.5/26; BR-IB-03/09/13. |
| Kind / priority | Recovery/transaction; P0. |
| Environment | integration API + DB with audit failure. |
| Preconditions | Q-X has known title, topic, rubric, CSV-only fields and user answer snapshot |
| Test data | G1 UPDATE. |
| Steps | attempt valid title/rubric/topic update while audit write throws; compare all question, rubric, history, answer and audit rows. |
| Expected | Update returns `503 INB-503`; all DB state remains at pre-call snapshot, including topic progress and old answer; no audit row and no partial rubric replacement. |
| Cleanup | restore fault state |
| Evidence | Retain `503 INB-503`, row hashes and transaction trace. |
| Design review | Draft; `503 INB-503` chốt 2026-10-27; not executed |

### TC-F6-AC37-05 — Audit port failure rolls back each import and catalog change

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-37; API §§11–18,24,26; BR-IB-01/11/13. |
| Kind / priority | Recovery/transaction; P0. |
| Environment | integration API + DB with failing audit port. |
| Preconditions | valid two-row CSV |
| Test data | separate valid empty topic creation and topic rename; Q/G authorized. |
| Steps | fail audit during import, topic create and topic rename; inspect all affected rows, imported rubrics and logs. |
| Expected | Each request returns `503 INB-503`. For CSV all imported questions/rubrics rollback as one file transaction; topic creation/rename each rollback; no mutation has a missing audit row or partially successful subset. |
| Cleanup | disable fault |
| Evidence | retain DB snapshots and transaction logs. |
| Design review | Draft; `503 INB-503` chốt 2026-10-27; not executed |

### TC-F6-AC38-01 — Direct instructor catalog mutations are denied without side effects

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-38; API §§11–18; BR-IB-01/13; security F6 §§2.1–2.4. |
| Kind / priority | Security; P0. |
| Environment | direct API integration, database and audit read. |
| Preconditions | G1 has all four question-bank functions but role INSTRUCTOR |
| Test data | snapshot T-DB/T-BEH and L-MED/L-HARD including display names, STAR flags, sort order; G1 can read/select catalog. |
| Steps | G1 calls topic/level create, rename, STAR toggle, reorder and delete endpoints directly; then opens authorized question authoring reference lists. |
| Expected | Every catalog mutation returns403 INB-302 because G1 has the required Function but lacks ADMIN; all catalog rows/order remain byte-for-byte and no audit is created. G1 may still read/select topic and level when authoring a question. |
| Cleanup | none expected |
| Evidence | retain endpoint/status matrix, token claims, DB snapshots and audit absence. |
| Design review | Draft; not executed; no execution evidence. Cited DD oracle captured; branch remains unexecuted. |

### TC-F6-AC38-02 — Missing Function and non-admin role checks are both enforced

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-38; API §§11–18; TDD AC-38; BR-IB-01/13. |
| Kind / priority | Security boundary; P1. |
| Environment | API integration with token variants. |
| Preconditions | G1 has ADMIN-like Function claims but INSTRUCTOR role |
| Test data | another token has ADMIN role but lacks required catalog Function; baseline catalog snapshot. |
| Steps | attempt same topic create/level delete with both tokens. |
| Expected | Instructor with Function but no ADMIN category receives `403 INB-302` from the use case; ADMIN category without Function receives `403 INB-301` at `@PreAuthorize` before use-case entry. If both are absent, Function check wins and returns `403 INB-301`. No request mutates catalog or writes a success audit. |
| Cleanup | no changes expected |
| Evidence | retain claims and responses. |
| Design review | Draft; precedence follows DD F6 API §1.2 and BR-IB-02; not executed |

