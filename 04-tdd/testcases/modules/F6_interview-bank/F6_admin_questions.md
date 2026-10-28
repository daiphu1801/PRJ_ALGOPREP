# F6 admin question management — test cases

Status: **Draft; Chưa thực hiện**. No test run or execution evidence is claimed.

## Shared fixture and environment

- Integration environment: F6 API, PostgreSQL, F1 AuditLogPort, and learner/API read surfaces; use disposable DB. Actors: `G1` instructor with READ/CREATE/UPDATE/DELETE, `G2` instructor with no question ownership relation, `Q` admin, `S1` student. IDs: topics `T-DB` (`DATABASE`), `T-BEH` (`BEHAVIORAL`); levels `L-EASY`, `L-MED`, `L-HARD`; questions `Q-X`, `Q-Y`, `Q-Z` with distinct `IQ-nnn` display numbers.
- Seed all before/after state specified per case. Capture HTTP requests/responses, DB rows (`interview_questions`, `answer_rubrics`, `user_answers`, `practice_history`), and `system_audit_logs` read-only snapshots. Replace symbolic IDs with UUID fixtures.
- Cleanup restores saved content/catalog, removes disposable questions and learner data, and records evidence before cleanup. All cases remain **Chưa thực hiện**.

### TC-F6-AC30-01 — Shared admin list across creators; retired rows excluded

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-30; API F6 §19; BR-IB-13; IB19-N1. |
| Kind / priority | Normal + security; shared scope and soft-delete filtering; P1. |
| Environment | integration API, instructor/admin/student tokens, database. |
| Preconditions | G1 created Q-X/Q-Y/Q-Z |
| Test data | Q created Q-W/Q-V; Q-V is RETIRED; G2 created none. All other four ACTIVE. Seed display numbers and distinct topics/levels. |
| Steps | G1, G2, Q call `GET /admin/questions?pageSize=20`; S1 calls same route; compare list membership. |
| Expected | all three authorized actors see exactly the same four ACTIVE IDs, including rows created by another instructor/admin; no Q-V; each item has IQ number, follow-up/rubric/use counts and nullable average; S1 gets `403 INB-301`; no personal learner data returned. |
| Cleanup | remove fixture rows |
| Evidence | retain response comparison, status and DB status snapshot. |
| Design review | Draft; not executed; no execution evidence. Cited DD oracle captured; branch remains unexecuted. |

### TC-F6-AC30-03 — Usage and average sorting apply before pagination

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-30; API §19.2/19.5; BR-IB-10 |
| Kind / priority | Boundary / Regression; P1 |
| Environment | F6 API + PostgreSQL, isolated nine-question catalog |
| Preconditions | Authorized G1 READ; exactly nine ACTIVE questions Q1..Q9 with display_no1..9; no filters. Each Qi has i COMPLETED attempts with one snapshot criterion weight100 and achievementPercent=i*10; distinct aggregate values. |
| Test data | GET /admin/questions with sort=usageCount then averageScorePercent, dir=desc, pageSize=8, pages1 and2. |
| Steps | Seed all nine questions/attempts; call both pages for each sort; concatenate returned IDs and compare DB aggregate ranking. |
| Expected | Each sort page1 is Q9,Q8,...,Q2 and page2 is Q1; totalItems=9, no duplicate/missing item. Q9 must not be stranded on page2 by an initial displayNo pagination. Values are usageCount=i and averageScorePercent=i*10.0; no learner answer text appears. |
| Cleanup | Remove nine questions/attempts and dependent fixtures. |
| Evidence | Four responses, expected global ordering and per-question aggregate query. |
| Design review | Reviewed against API §19 global ORDER BY before LIMIT; not executed. |

### TC-F6-AC30-02 — Admin list sorting, paging, and input boundaries

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-30; API §19.2–19.7; IB19-N2/A1/B1. |
| Kind / priority | Boundary; wrong ordering/paging; P2. |
| Environment | integration API. |
| Preconditions | at least 9 ACTIVE questions spanning EASY/MEDIUM/HARD with display names deliberately not matching catalog `sort_order` |
| Test data | one item has no usage. |
| Steps | request `sort=level&dir=desc&pageSize=8`, then asc and page 2; request pageSize 20 and 50; request 7; request a question without usage. |
| Expected | level follows catalog `sort_order` (not alphabetic label); paging returns at most requested allowed size; invalid size 7 returns `400 INB-V01`; no-use item has `usageCount=0`, `averageScorePercent=null`. |
| Cleanup | remove records |
| Evidence | retain queries, ordered IDs and boundary response. |
| Design review | Draft; not executed; no execution evidence. Cited DD oracle captured; branch remains unexecuted. |

### TC-F6-AC31-01 — Admin usage score formula, failed attempt denominator, and privacy

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-31; API §19–20; BR-IB-10; IB19-B1. |
| Kind / priority | Normal + security; aggregation correctness and learner privacy; P1. |
| Environment | integration API and learner list/detail surfaces. |
| Preconditions | Q-X rubric weights 30/40/30 |
| Test data | COMPLETED attempt A scores 80/60/100; COMPLETED B scores 100/100/100; one FAILED attempt C; one ACTIVE question with no attempts. Expected weighted scores 78 and 100, aggregate 89. |
| Steps | G1 reads list and Q-X detail; S1 reads learner list/detail and any learner-facing progress surface containing Q-X. |
| Expected | instructor/admin aggregate is usageCount 3 and averageScorePercent 89.0; FAILED contributes to usage count but not average. No-use question is null (client displays `-`). Student responses omit both usage count and average anywhere in tested learner surfaces; no personal attempt detail leaks. |
| Cleanup | delete seeded attempts |
| Evidence | retain aggregate payloads and student schema/field scan. |
| Design review | Draft; not executed; no execution evidence. Cited DD oracle captured; branch remains unexecuted. |

### TC-F6-AC31-02 — Rounded average and completed-only inclusion

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-31; BR-IB-10 in logic §6. |
| Kind / priority | Boundary; decimal calculation; P2. |
| Environment | integration DB/API. |
| Preconditions | One ACTIVE question with rubric weights `30/40/30`; no prior attempts. Seed two COMPLETED attempts and one FAILED attempt for the same question. |
| Test data | COMPLETED A has criterion achievement `78/78/78` → score `78.0`; COMPLETED B has `77/80/78` → `77×0.30 + 80×0.40 + 78×0.30 = 78.5`; average `(78.0+78.5)/2 = 78.25`. FAILED C has no score and must not enter the denominator. |
| Steps | calculate from stored JSON and compare API list/detail aggregate. |
| Expected | each attempt score uses Σ(achievementPercent × stored weightPercent)/100; average is arithmetic mean across COMPLETED only, rounded to one decimal **half-up**; a tie at `78.25` returns `78.3`, not `78.2`; FAILED excluded. |
| Cleanup | delete attempts |
| Evidence | retain source JSON, independent calculation and response. |
| Design review | Draft; half-up oracle chốt 2026-10-27; not executed |

### TC-F6-AC32-01 — Detail ordering, no-rubric state, retired and unknown IDs

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-32; API §20; IB20-N1/A1/B1. |
| Kind / priority | Normal + abnormal; P1. |
| Environment | integration API. |
| Preconditions | Q-X has three ordered rubric rows and two follow-ups |
| Test data | Q-Y has no rubric; Q-Z is RETIRED; one UUID does not exist. |
| Steps | G1 fetches X, Y, Z, and unknown ID; S1 attempts X detail. |
| Expected | X returns complete editable fields, rubric ordered by `sort_order`, two follow-ups, `practiceAvailable=true`; Y returns `rubric=[]`, false; Z and unknown both return `404 INB-010`; unauthorized actor gets `403 INB-301` without content. |
| Cleanup | remove data |
| Evidence | retain responses and row order. |
| Design review | Draft; not executed; no execution evidence. Cited DD oracle captured; branch remains unexecuted. |

### TC-F6-AC33-01 — Create valid question and empty-rubric learning behavior

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-33; API §21; BR-IB-03; IB21-N1/N2. |
| Kind / priority | Normal; P1. |
| Environment | integration API, audit query, learner read endpoint. |
| Preconditions | G1 has CREATE |
| Test data | T-DB/L-MED exist; baseline max display number known. |
| Steps | POST valid title `Explain indexes` with rubric `30/40/30`, then POST title `What is a table?` with empty rubric. |
| Expected | Each returns 201 with new increasing server displayNo and ACTIVE state; each has exactly one `INTERVIEW_QUESTION_CREATED` audit. First has ordered rubric; second has `practiceAvailable=false`, stays readable in learning detail and remains eligible for recall/drill selection (DD §2 has no rubric filter). Its practice submission is rejected with 422 INB-040. No client-supplied display number is accepted. |
| Cleanup | delete test questions |
| Evidence | retain response, audit and learner list/detail. |
| Design review | Draft; not executed; no execution evidence. Cited DD oracle captured; branch remains unexecuted. |

### TC-F6-AC33-02 — Invalid rubric totals, duplicate names, topic deletion race

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-33; API §21.4–21.7; BR-IB-03. |
| Kind / priority | Abnormal; data integrity; P1. |
| Environment | integration API + DB. |
| Preconditions | G1 CREATE |
| Test data | valid baseline topic/level; a topic scheduled for deletion before request. |
| Steps | submit rubric weights 30/40/20; submit criterion codes `Rõ ràng` and `rõ ràng`; delete a topic then POST using its ID. |
| Expected | first is `422 INB-033` with currentSum 90; second is `400 INB-V11`; deleted topic is `422 INB-035`. Each failure creates no question, rubric, sequence-visible committed record, or audit row. |
| Cleanup | none beyond restoring topic fixture |
| Evidence | retain requests/responses and DB/audit absence checks. |
| Design review | Draft; not executed; no execution evidence. Cited DD oracle captured; branch remains unexecuted. |

### TC-F6-AC33-03 — Create title, rubric decimal, and count boundaries

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-33; API §21.2–21.4; validation F6 §5. |
| Kind / priority | Boundary; P1. |
| Environment | integration API. |
| Preconditions | valid T-DB/L-MED |
| Test data | title length 255/256; rubric weights 33.33/33.33/33.34, then 3-decimal value; title with leading/trailing whitespace. |
| Steps | POST each variant. |
| Expected | 255 characters accepted, 256 returns `400 INB-V08`; valid hundredth precision sum succeeds; >2 decimal places returns `400 INB-V10`; accepted title is trimmed. No failed payload writes. Body byte boundary is checked independently in AC33-05. |
| Cleanup | remove successful question |
| Evidence | retain byte/character count, responses and DB rows. |
| Design review | Draft; not executed; no execution evidence. Cited DD oracle captured; branch remains unexecuted. |

### TC-F6-AC33-04 — Unlimited follow-up/rubric count is not silently capped

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-33; API §21.2; BR-IB-03 and DEC-2026-1001-admin-configurable-settings. |
| Kind / priority | Boundary; P2. |
| Environment | integration API with request below 512 KiB. |
| Preconditions | one question with 101 nonempty follow-up strings |
| Test data | a separate question with 101 distinct criterion codes and valid weights totaling exactly 100 (100 entries at 0.99 and one at 1.00). |
| Steps | POST each; GET admin detail. |
| Expected | All 101 supplied entries persist in order; no undocumented count cap or truncation; each response/detail preserves count. Both requests remain below the stated 512 KiB technical body cap in API §21. This case makes no claim about requests above that cap. |
| Cleanup | remove test questions |
| Evidence | retain request counts and response lengths. |
| Design review | Draft; below-cap 101-entry oracle bounded by API §21; not executed. |

### TC-F6-AC33-05 — Create body accepts 512 KiB and rejects the next byte

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-33; API §21.4, validation F6 §5; `DEC-2026-1028-qa-f6-technical-limits`. |
| Kind / priority | Boundary/atomic rejection; P1. |
| Environment | Direct F6 API + DB/audit, bypass proxies with a lower request cap. |
| Preconditions | G1 has CREATE; T-DB/L-MED exist; two distinct valid short titles are absent. |
| Test data | Build valid UTF-8 JSON bodies with one question, one 100% rubric criterion and legal JSON whitespace padding after the closing brace. Body A is exactly 524288 bytes; body B uses a distinct title and is 524289 bytes. Record exact byte lengths before sending. |
| Steps | POST A then POST B with `Content-Type: application/json`; inspect responses, committed question/rubric rows and audit delta. |
| Expected | A is accepted with `201` and one question/rubric plus one create audit; B returns `413 INB-V17` before any mutation, with no B question/rubric/audit. Boundary is measured on the full UTF-8 request body, not character count. |
| Cleanup | Remove A in disposable fixture; B requires no cleanup. |
| Evidence | Retain body hashes/byte lengths, responses and DB/audit diff without logging sensitive content. |
| Design review | Draft; owner confirmed 512 KiB cap 2026-10-28; not executed. |

### TC-F6-AC34-01 — Cross-author update preserves CSV-only fields and old rubric snapshots

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-34; API §22; BR-IB-03/09; IB22-N1/N3/N4. |
| Kind / priority | Normal + data preservation; P1. |
| Environment | integration API, DB, audit query. |
| Preconditions | Q-created Q-X with `suggested_approach="- Plan"`, `core_keywords=["index"]`, `sample_answer_framework="Frame"` |
| Test data | prior S1 answer contains feedback and `rubric_snapshot_json` for 30/40/30. G1 has UPDATE. |
| Steps | G1 PUT title change and replacement valid rubric (remove one criterion and adjust remaining weights to 100); inspect row and old answer; GET detail. |
| Expected | G1 may update Q-created content; all three CSV-only columns remain byte-for-byte; old answer feedback and snapshot retain prior rubric/weights; exactly one `INTERVIEW_QUESTION_UPDATED` audit with actor G1; new detail reflects new rubric. |
| Cleanup | restore question |
| Evidence | retain before/after row, old answer and audit. |
| Design review | Draft; not executed; no execution evidence. Cited DD oracle captured; branch remains unexecuted. |

### TC-F6-AC34-02 — Empty rubric hides practice but preserves learning and history

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-34; API §22; BR-IB-03/05/09; IB22-N2. |
| Kind / priority | Normal; P1. |
| Environment | API + learner practice/learning endpoints. |
| Preconditions | Q-X ACTIVE with rubric and one prior practice attempt by S1. |
| Test data | Fixture values and actor state are specified in Preconditions. |
| Steps | G1 PUT full form with `rubric=[]`; query practice list, learning detail, prior attempt, and practice_history. |
| Expected | update succeeds; question is excluded from practice eligibility and `practiceAvailable=false`, remains available in learning detail; previous answer/history rows are preserved and relevant topic progress is recalculated per BR-IB-09; one update audit. |
| Cleanup | restore rubric and progress fixture |
| Evidence | retain endpoint/DB snapshots. |
| Design review | Draft; not executed; no execution evidence. Cited DD oracle captured; branch remains unexecuted. |

### TC-F6-AC34-03 — Topic move recalculates progress for old and new topics

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-34; API §22.5; BR-IB-09; IB22-B1. |
| Kind / priority | Boundary; progress integrity; P1. |
| Environment | integration API + DB. |
| Preconditions | Q-X in T-DB |
| Test data | S1 attempted Q-X and a second active T-DB question, and has one T-BEH attempt; known `practice_history` before. |
| Steps | move Q-X to T-BEH with valid complete form; fetch both topic progress and inspect history. |
| Expected | transaction recomputes attempted distinct active questions for both topics, with last practiced timestamps from matching answers; old topic count decreases only by Q-X if another remains; new count includes Q-X once; no additive double-count; one update audit. |
| Cleanup | restore topic and history |
| Evidence | retain transaction/rows. |
| Design review | Draft; not executed; no execution evidence. Cited DD oracle captured; branch remains unexecuted. |

### TC-F6-AC34-04 — Invalid update is atomic; retired question cannot update

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-34; API §22.4–22.7; BR-IB-03; IB22-A1. |
| Kind / priority | Abnormal; P1. |
| Environment | integration API + DB/audit. |
| Preconditions | Q-X ACTIVE snapshot |
| Test data | Q-Z RETIRED; submit valid title with rubric sum 99, then update Q-Z. |
| Steps | send both PUT calls; compare all content, rubric, history and audit to baseline. |
| Expected | sum 99 returns `422 INB-033`; retired returns `404 INB-010`; both leave content, rubrics, practice history and audit unchanged. |
| Cleanup | no changes expected |
| Evidence | retain responses and before/after hashes. |
| Design review | Draft; not executed; no execution evidence. Cited DD oracle captured; branch remains unexecuted. |

### TC-F6-AC35-01 — Mixed soft-delete batch succeeds partially with per-item audit

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-35; API §23; BR-IB-03/09; IB23-N1/N2/A1. |
| Kind / priority | Normal + abnormal; data retention, P1. |
| Environment | integration API + DB/audit. |
| Preconditions | Q-X/Y/Z ACTIVE each has rubrics, one user answer/bookmark/recall |
| Test data | Q-RETIRED already retired; G1 DELETE. |
| Steps | POST ids `[X,Y,Z,RETIRED]`; inspect response, all learner/admin lists, retained rows, history and audits. |
| Expected | HTTP 200; three IDs in `retiredIds`, retired ID in `failedItems` with `INB-010`; active questions disappear from all current lists; rubrics, answers, bookmarks, recall remain; practice history recomputes; exactly one per-question delete audit for each successful item and none for failed item. |
| Cleanup | restore fixture status/history |
| Evidence | retain payloads and DB row counts. |
| Design review | Draft; not executed; no execution evidence. Cited DD oracle captured; branch remains unexecuted. |

### TC-F6-AC35-02 — Bulk limit and duplicate/malformed IDs reject whole request

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-35; API §23.2–23.4; validation §9. |
| Kind / priority | Boundary/security; P1. |
| Environment | integration API. |
| Preconditions | 101 valid ACTIVE question IDs |
| Test data | also valid duplicate ID and malformed UUID variants. |
| Steps | POST 101 IDs, duplicate IDs, and malformed ID separately; inspect every target and audit. |
| Expected | each returns `400 INB-V02` before processing any item; no question status, history or audit changes. The maximum 100 IDs is confirmed by `DEC-2026-1028-qa-f6-technical-limits`. |
| Cleanup | none expected |
| Evidence | retain response and no-mutation queries. |
| Design review | Draft; owner confirmed 100-ID cap 2026-10-28; not executed. |

### TC-F6-AC35-04 — Exactly 100 IDs are accepted as one delete batch

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-35; API §23.2–23.4, validation F6 §9; `DEC-2026-1028-qa-f6-technical-limits`. |
| Kind / priority | Boundary/transaction; P1. |
| Environment | Direct F6 API + PostgreSQL + F1 audit spy. |
| Preconditions | G1 has DELETE; seed exactly 100 distinct ACTIVE question UUIDs with rubrics and no concurrent writers; snapshot all statuses and audit count. |
| Test data | `ids` contains each seeded UUID once, in ascending fixture order; request contains no other ID. |
| Steps | POST one bulk-delete request with all 100 IDs; inspect response, every status, dependent learner/history rows and audit delta. |
| Expected | HTTP `200`; `retiredIds` contains exactly the 100 input IDs and `failedItems=[]`; all 100 questions become RETIRED, retained dependent rows are not deleted, and exactly 100 corresponding question-retire audit rows are committed. The 101-ID rejection is checked separately in AC35-02. |
| Cleanup | Restore disposable database fixture. |
| Evidence | Retain request ID count, response ID sets, status diff, dependent-row counts and audit delta. |
| Design review | Draft; owner confirmed 100-ID cap 2026-10-28; not executed. |

### TC-F6-AC35-03 — Non-delete permission cannot soft-delete

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-35; API §23.1/23.4; BR-IB-03/13; security F6 §2. |
| Kind / priority | Security; P1. |
| Environment | integration API. |
| Preconditions | Q-X ACTIVE |
| Test data | G2 has READ/CREATE/UPDATE but lacks DELETE. |
| Steps | G2 directly calls bulk delete with Q-X; inspect status, question and audit. |
| Expected | `403 INB-301`; Q-X remains ACTIVE, no history change and no audit row. |
| Cleanup | none |
| Evidence | retain permission claims, response and state check. |
| Design review | Draft; not executed; no execution evidence. Cited DD oracle captured; branch remains unexecuted. |

