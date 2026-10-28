# F6 learner: topic progress

## Scope and sources

- Scope: F6 TDD AC-22–23. Design only; all cases are **Chưa thực hiện**.
- TDD: `04-tdd/modules/F6_interview-bank.md` AC-22–23.
- DD: `03-dd/api/F6_interview-bank.md` §8, §22–23, §28 Q11; `03-dd/logic/F6_interview-bank.md` BR-IB-03, BR-IB-09; `03-dd/validation/F6_interview-bank.md` §9; `02-bd/database/F6_interview-bank.md` §8; `02-bd/screens/users/USR0601_dashboard.md` §7.3.
- Decision baseline: DD F6 updated 2026-10-23; Q11 resolves recalculation to ACTIVE questions on retirement/topic change. No background job; recalculation is synchronous in the write transaction (logic §1, BR-IB-09).
- Execution status: Chưa thực hiện; no execution evidence exists.

## Shared fixtures

- Clock `T0=2026-10-27T02:00:00Z`.
- Learners `U-A=10000000-0000-4000-8000-000000000001`, `U-B=10000000-0000-4000-8000-000000000002`.
- Topics ordered by `sort_order`: `T-DB=20000000-0000-4000-8000-000000000010`, code `DATABASE`, display `Database`, sort 10; `T-NET=20000000-0000-4000-8000-000000000011`, code `NETWORK`, display `Mạng`, sort 20; `T-EMPTY=20000000-0000-4000-8000-000000000012`, code `EMPTY_TOPIC`, display `Chủ đề rỗng`, sort 30.
- Question IDs use sequential UUIDs `40000000-0000-4000-8000-000000000001` onward; each fixture gets a unique suffix. Before each case, record exact ACTIVE/RETIRED status, topic_id, user_answers (answer id, created_at, status), and practice_history row for each learner/topic. “Attempted” means at least one `user_answers` row, including feedback_status FAILED.
- No jobs or AI service are required for GET progress; AI can be disabled. Admin question update/retire APIs are used where state mutation is required.

## Branch inventory and traceability

| AC | Branches covered | Case IDs | Review |
|---|---|---|---|
| AC-22 | distinct attempted questions vs attempts; FAILED counts; low score irrelevant; denominator active; zero/empty and unpracticed topics; last timestamp | TC-F6-AC22-01, -02 | Draft |
| AC-23 | retire recalculates numerator+denominator and preserves attempts; topic move recalculates old/new; no >100% | TC-F6-AC23-01, -02 | Draft |

## Cases

### TC-F6-AC22-01 — Progress counts distinct attempted ACTIVE questions, not attempts or scores

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-22; API §8; BR-IB-09; DB6 §8; validation §9 |
| Kind / priority | Normal; P1 (learner-facing aggregate) |
| Environment | API integration + PostgreSQL; no AI required |
| Preconditions | T-DB has exactly 10 ACTIVE questions. U-A has at least one attempt for three distinct T-DB questions: `P1` completed once at `T0-3d`, `P2` completed three times with final at `T0-2d`, `P3` FAILED once at `T0-1d`; U-A has not attempted other T-DB questions. P2’s repeated attempts all carry low score; P1 high score. U-A has 0 attempts in T-NET. |
| Test data | GET `/me/topic-progress` as U-A; T-NET is a newly-created topic with 0 questions, and T-EMPTY is a topic with questions but no attempts. |
| Steps | Call progress endpoint; compare each returned topic record against seeded attempts and ACTIVE questions. |
| Expected | HTTP 200; T-DB row has `questionsAttempted=3`, `totalQuestions=10`, `completionPercent=30`, `lastPracticedAt=T0-1d` (the latest attempt time among its counted active questions). Three attempts on P2 count only once; FAILED P3 counts; score values do not alter count. T-NET remains present as 0/0 and 0%, `lastPracticedAt=null`; T-EMPTY present as 0/n and 0%. All topics returned in sort_order and no userId/query parameter is accepted/used. |
| Cleanup | Delete attempts and practice_history rows for U-A; remove fixture questions/topics in dependency order. |
| Evidence | Response JSON, question status/topic query, user_answers and practice_history rows. |
| Design review | Draft |

### TC-F6-AC22-02 — Another learner’s attempts do not affect caller progress

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-22; API §8.1, §8.5; BR-IB-09; DB6 §8 |
| Kind / priority | Security; P1 (progress privacy/ownership) |
| Environment | API integration + PostgreSQL |
| Preconditions | T-DB has 10 ACTIVE questions. U-B has attempted 4 distinct questions; U-A has attempted 0. DB includes no U-A practice_history row for T-DB. |
| Test data | GET `/me/topic-progress` with U-A token, then with U-B token. |
| Steps | Request each response; do not supply `user_id` query value. |
| Expected | U-A T-DB row 0/10, 0%, null lastPracticedAt. U-B row 4/10, 40%, timestamp from U-B’s max qualifying attempt. No cross-account attempt count or timestamps leak. |
| Cleanup | Remove seeded attempts/history and fixtures. |
| Evidence | Both responses and DB grouped counts scoped by user_id. |
| Design review | Draft |

### TC-F6-AC23-01 — Soft retirement recalculates active numerator and denominator, preserving answer rows

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-23; API §23 and §8; API §28 Q11; BR-IB-03, BR-IB-09; DB6 §8 |
| Kind / priority | Recovery; P1 (lifecycle consistency and no progress over 100%) |
| Environment | Integration API + PostgreSQL; authorized teacher/admin retirement route |
| Preconditions | T-DB has 10 ACTIVE questions; U-A has practiced 3 distinct questions, including P, with P attempt at `T0-3d` and two other qualifying attempts later/earlier as seeded. `practice_history` initially 3/10; snapshot P’s user_answers IDs/content and related recall/bookmark rows. |
| Test data | Retire P using API §23 request, then GET progress as U-A. |
| Steps | 1. Verify precondition 3/10. 2. Call bulk retire for P. 3. Read response and progress. 4. Query related learner rows. |
| Expected | Retire operation succeeds; same transaction marks P RETIRED and recalculates practice_history. T-DB reports 2/9 (22% by integer rounding) and lastPracticedAt equals max created_at among the two remaining ACTIVE attempted questions. P’s user_answers, recall_ratings and bookmarks remain stored unchanged. No job/event delay; no percent >100. |
| Cleanup | Remove attempts and user state; restore/remove question and recalculate isolated history. |
| Evidence | Retire response, transaction result, progress response, before/after learner row snapshots. |
| Design review | Draft |

### TC-F6-AC23-02 — Moving an attempted question recalculates both topic rows

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-23; API §22 (question update), §8; BR-IB-03, BR-IB-09; DB6 §8; API §28 Q11 |
| Kind / priority | Normal; P1 (cross-topic aggregate repair) |
| Environment | Integration API + PostgreSQL; authorized question editor |
| Preconditions | T-DB has 10 ACTIVE questions and U-A practiced 3 unique questions including P. T-NET has 0 ACTIVE questions. Seed P `last_practiced_at=T0-3d`; other two T-DB attempts with latest at T0-1d. Capture P user_answers IDs/content. |
| Test data | Update P `topicId` from T-DB to T-NET through question update endpoint; read progress. |
| Steps | 1. Verify initial T-DB 3/10, T-NET 0/0. 2. Update P topic. 3. GET progress. 4. Query answer rows and both history rows. |
| Expected | After update, T-DB is 2/9 (22%); T-NET is 1/1 (100%); neither exceeds 100. T-DB lastPracticedAt is max of its remaining attempted questions; T-NET timestamp is P’s latest attempt. Answer rows remain unchanged. Both topic aggregates are recalculated synchronously in the same transaction as topic change. |
| Cleanup | Restore topic or delete isolated rows; remove attempts/history/questions/topics. |
| Evidence | Update response, progress response, before/after practice_history and answer snapshots. |
| Design review | Draft |

