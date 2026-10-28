# Phase 5 smoke and E2E selection

> **Design only — not executed.** This selection chains existing module and screen testcase contracts. It is not a run record or acceptance result. All source cases remain `Chưa thực hiện`; actual E2E requires a deployed build and live backend/infrastructure. Selected journeys intentionally reuse source case IDs instead of copying their detailed endpoint oracles.

## Selection summary

| Set | Selection | Purpose | State |
| :--- | :--- | :--- | :--- |
| Smoke | 8 P0 module cases | Fast checks for identity/session, public catalog, generated starter, accepted judging, Hidden privacy, broker failure, admin permission invariant, and AI budget degradation | Design only |
| E2E | 3 role journeys: learner, instructor, administrator | Cross UI/API/DB/event/job checks with common seeded data and role boundaries | Design only |
| Optional learner extension | 2 F6 recall cases | Add interview-bank study/review when F6 service and learner data are deployed | Design only; not in P0 gate |

## Environment and shared fixtures

- Build tuple to record at execution: frontend SHA, backend SHA, database migration version, judge worker image/compiler versions, and deployed configuration version. Use a dedicated test deployment/database and UTC clock `2026-10-27T02:00:00Z`; all test deadlines and 15-minute expiry windows derive from that clock. Isolation is an environment fixture, not an assumed product tenant feature.
- Services required for core E2E: F1/F2/F3/F4 APIs, PostgreSQL, Redis, RabbitMQ, object storage (MinIO or test-compatible S3), judge worker/sandbox, browser frontend, and authenticated realtime channel. Enable DB snapshots and deterministic cleanup. The full eight-case smoke set additionally requires F5 with a deterministic provider stub for SMK-P0-08. If F5 is unavailable, record that case as blocked and do not report the full smoke gate passed.
- Actors: learner `L1` (STUDENT), unrelated learner `L2`, instructors `I1`/`I2` (INSTRUCTOR), and administrators `A1`/`A2` (ADMIN). Use unique emails under `phase5.example.test`; seed credentials in the test secret store, never in evidence. Learner L1 is enrolled in class `C1`, owned by I1; L2 is not enrolled. Each actor has the Function:Action grants required by its allowed steps.
- Problem `P1=51000000-0000-4000-8000-000000000001`, code `PH5-ECHO`, is PUBLISHED, not deleted, Wrapper mode, Java enabled, with TypeNode `STRING` return and one `STRING` parameter `value`, function name `echo` (Java signature `String echo(String value)`). Its reference solution is a complete stdin/stdout echo program, as required by F2 BR-15; learner S1 is the corresponding Wrapper function. It has exactly eight approved cases ordered 1..8: two Samples (`hello`→`hello`, `world`→`world`) and six Hidden (`PH5_HIDDEN_01` through `PH5_HIDDEN_06`, each echoed unchanged). Each input/output is the raw text plus a final newline, with no JSON quotes. Two worked examples and the remaining spec/publication checklist fields are valid; verify all five checks in F2 BR-07 before publishing. Hidden markers must stay out of learner surfaces. A single testcase-set version is pinned for the entire run. `P2=51000000-0000-4000-8000-000000000002` is an unpublished decoy that must never appear in L1 catalog results.
- Class `C1=61000000-0000-4000-8000-000000000001` is active and owned by I1; L1 has one active enrollment. Before instructor journey, P1 is not assigned. `C2=61000000-0000-4000-8000-000000000002` belongs to I2 and is the denial control. `C3=61000000-0000-4000-8000-000000000003` belongs to I1 and is reserved for the Outbox-failure batch. `P3=51000000-0000-4000-8000-000000000003` and `P4=51000000-0000-4000-8000-000000000004` are independent published copies of P1 with unique codes and complete publication fixtures, initially unassigned to C3; P3 will fail and P4 will succeed in that batch.
- Solution `S1` returns the correct output for all eight cases. `S2` is syntactically invalid on learner line 3 and is used only for negative result reconciliation. Judge fixture scripts record each testcase invocation and output marker. Browser network capture, DB audit/event queries, Outbox state, worker logs, storage object inventory, and realtime payload recorder are enabled with secret redaction.
- Resettable external states: Redis login counters/session keys; RabbitMQ queues and consumer offsets; Outbox rows; realtime subscriptions; storage objects; judge job records; AI stub responses/cache/quota for SMK-P0-08. Disable unrelated schedulers; retain the contract-defined retry behavior being asserted by each source case.

## P0 smoke selection

Run each selected module case with its own source fixture and reset, rather than substituting PH5-ECHO for `P-TWO`, `P-JUDGE` or other source fixtures. Shared PH5-ECHO data below is for the integrated journeys; there, catalog row counts, signature text and testcase counts are deliberately adapted to this fixture while preserving the source business oracles.

| Smoke ID | Source testcase | Critical oracle |
| :--- | :--- | :--- |
| SMK-P0-01 | [TC-F1-AC03-01](modules/F1_identity/F1_auth_session.md) | Login issues usable access token and secure HttpOnly refresh cookie; no raw refresh token in DB/log. |
| SMK-P0-02 | [TC-F2-AC01-01](modules/F2_problem-bank/F2_student_catalog_reads.md) | Learner list contains only the source fixture's published/nondeleted rows, with its expected paging/count, and excludes unpublished/deleted rows. |
| SMK-P0-03 | [TC-F3-AC01-01](modules/F3_harness/F3_starter_code.md) | Java/C++/Python starters match the source signature, argument names/types/order and Wrapper configuration, and compile/parse as specified by the source case. |
| SMK-P0-04 | [TC-F4-AC16-01](modules/F4_judge-orchestration/F4_verdict_privacy_queue.md) | Accepted submission has all cases AC, ratio 1.000 and exactly one grading plus accepted event. |
| SMK-P0-05 | [TC-F4-AC76-01](modules/F4_judge-orchestration/F4_verdict_privacy_queue.md) | Hidden input, expected output and actual output markers do not appear in API, realtime payload or rendered UI. |
| SMK-P0-06 | [TC-F4-AC08-01](modules/F4_judge-orchestration/F4_submission_resilience.md) | Broker publish failure terminates the submission path; no indefinite PENDING state or false learner verdict. |
| SMK-P0-07 | [TC-F1-AC28-01](modules/F1_identity/F1_rbac_audit.md) | Removing the last ADMIN UPDATE permission is rejected atomically; permission and audit state remain unchanged. |
| SMK-P0-08 | [TC-F5-AC08-01](modules/F5_ai-review/F5_solution_review.md) | AI budget lock blocks new learner analysis but does not block an existing report read or judge submission. Use deterministic AI stub/cache, not a live provider. |

### P0 smoke execution order

1. Reset the isolated test deployment/database, verify migrations and service health, seed each source fixture, and capture baseline DB/event/object snapshots.
2. Run SMK-P0-01 and SMK-P0-02. Stop on authentication, authorization, or catalog leakage failures.
3. Run SMK-P0-03, then SMK-P0-04 and SMK-P0-05 with separate disposable submissions and their own source fixtures/verdicts. Do not expose Hidden markers in screenshots or unredacted logs.
4. Run SMK-P0-06 with a fault hook scoped to the disposable submission; remove the hook immediately after capture. Verify no unrelated queue item was affected.
5. Run SMK-P0-07 against disposable role-permission rows and SMK-P0-08 with isolated quota/cache fixtures.
6. Restore baseline snapshots and verify no test actor, token, event, object, queue item, quota entry, or audit artifact leaked into shared environments.

## E2E journey E2E-L — learner discovers, solves, submits, and reviews

For one combined run, execute E2E-I steps 1–3 first, then E2E-L, then E2E-I steps 4–6 (reuse the accepted submission instead of submitting again). Defer cleanup shared by those journeys until both finish. Independent runs seed their own assignment/enrollment and restore their own baseline.

**Actors and source cases:** L1; [TC-F1-AC03-01](modules/F1_identity/F1_auth_session.md), [TC-F2-AC01-01](modules/F2_problem-bank/F2_student_catalog_reads.md), [TC-F2-AC08-01](modules/F2_problem-bank/F2_student_catalog_reads.md), [TC-F3-AC01-01](modules/F3_harness/F3_starter_code.md), [TC-USR0101-PLAC02-01](screens/users/USR0101_problem_list.md), [TC-USR0102-PDAC03-01](screens/users/USR0102_problem_detail.md), [TC-F4-AC16-01](modules/F4_judge-orchestration/F4_verdict_privacy_queue.md), [TC-F4-AC76-01](modules/F4_judge-orchestration/F4_verdict_privacy_queue.md), [TC-USR0201-SRAC04-01](screens/users/USR0201_submission_result.md).

**Preconditions and data:** Use L1 account and P1 from shared fixtures. P1 is assigned to L1 through C1 for the class-workspace portion; testcase version is frozen. Browser starts unauthenticated with no stored credentials or stale submission route.

**Steps and expected checkpoints:**

1. Sign in as L1. Check F1 response, usable `/me/profile`, access/session state and cookie flags; raw password/refresh token must not appear in response, DB, URL, or logs.
2. Open problem list and search/filter to PH5-ECHO. Check P1 appears once and P2 does not appear; URL state survives detail/back navigation. Open P1 by UUID, confirm saved prompt and Sample examples render, and confirm no Hidden markers are in response/UI.
3. Open workspace for P1. Check starter code uses the saved Java signature and P1 UUID; switch language only if that language is enabled by the fixture. Enter S1 and submit once.
4. Observe submission creation, queued job, worker execution and final result on this no-fault path. Check eight testcase executions and eight persisted result rows, including six Hidden; final verdict Accepted and score ratio 1.000; one persisted grading event and one accepted event; learner realtime delivery is scoped to L1. Delivery retries must not duplicate downstream business effects.
5. Open submission result. Check Sample rows merge as contracted, Hidden rows expose only allowed verdict/order/resource fields, and every learner response/UI/realtime capture is free of all six PH5_HIDDEN markers. Reopen/refresh result and verify the same persisted submission and no second judge run.
6. Optional failure branch: submit S2 on a second disposable problem revision or isolated cloned fixture. Check compile error maps to learner source line, no testcase run/score is invented, and detail screen retains editor text on recovery.

**Cleanup:** After both learner/instructor journeys finish, revoke L1 session and restore disposable DB/event/object/subscription fixtures from the baseline. Preserve QA run records and their redacted evidence outside the reset database. Remove copied browser downloads and clear browser storage.

**Evidence:** Build tuple, redacted login/profile responses, list/detail/workspace captures, request IDs, testcase-set version, submission DB row and testcase result summary, event IDs, worker invocation log, realtime payloads, Hidden-marker scan, and cleanup diff. Store secret-bearing evidence only in approved restricted test storage.

## E2E journey E2E-I — instructor assigns a problem and verifies class progress

**Actors and source cases:** I1 and L1; [TC-F1-AC03-01](modules/F1_identity/F1_auth_session.md), [TC-INS0201-AC-SC-04-01](screens/teacher/teacher_screens_phase4.md), [TC-F2-AC74-01](modules/F2_problem-bank/F2_class_assignments.md), [TC-F2-AC75-01](modules/F2_problem-bank/F2_class_assignments.md), [TC-F2-AC75-03](modules/F2_problem-bank/F2_class_assignments.md), [TC-INS0202-AC-SC-02-01](screens/teacher/teacher_screens_phase4.md), [TC-INS0203-AC-SC-01-01](screens/teacher/teacher_screens_phase4.md), [TC-F2-AC73-01](modules/F2_problem-bank/F2_class_assignments.md).

**Preconditions and data:** C1 belongs to I1, L1 is enrolled, P1 is published and not yet assigned. The class view must not include C2, another instructor's class, or L2. Outbox and assignment tables are snapshotted before the journey.

**Steps and expected checkpoints:**

1. Sign in as I1 and open owned C1. Check instructor area resolution, class ownership, and that invitation controls appear only in the approved class-detail context.
2. Open assignable-problem list. Check P1 is available, unpublished/deleted problems are excluded, and C2 is denied without leaking its assignments.
3. Assign P1 to C1. Check assignment row and ProblemAssignedToClass event commit together. Repeat the same assignment; UI/API should report already assigned/skipped without duplicate assignment or event.
4. Sign in as L1 and verify P1 appears in class work. L2 must not see C1 or its assignment. In the combined run, reuse the accepted submission already created by E2E-L; in an independent run, execute E2E-L once now.
5. Return as I1 to class progress and submission views. Check metrics include only C1 and L1's in-scope accepted result; assignment is counted once; unauthorized student result route is denied; historical rows remain readable under the permitted instructor scope.
6. As I1, submit one assignment batch for C3 containing P3 and P4; inject Outbox writer failure scoped only to P3. Check HTTP 200 with P3 in failedItems (`PRB-056`) and P4 in succeededIds; P3 has no assignment/event while P4 has one committed assignment/event. Remove fault and retry P3; exactly one P3 assignment/event is created and P4 stays unchanged.

**Cleanup:** Unassign P1 from C1 only after evidence capture; remove injected Outbox faults and restore C2/C3/P3/P4, assignment and event fixtures; retain original L1 enrollment and submissions until other journeys finish, then restore baseline. Preserve redacted QA records outside the reset database.

**Evidence:** Actor/owner claims, route URLs, assignment requests/responses, DB assignment rows, Outbox event IDs and consumer receipts, class learner visibility comparison, progress source rows/calculation, partial-failure response, retry diff, and cleanup diff.

## E2E journey E2E-A — administrator changes a safe permission and audits it

**Actors and source cases:** A1/A2 and instructor I1; [TC-F1-AC28-02](modules/F1_identity/F1_rbac_audit.md), [TC-F1-AC31-01](modules/F1_identity/F1_rbac_audit.md), [TC-ADM0202-RBAC-AC-02-01](screens/admin/admin_screens_phase4.md), [TC-ADM0403-LOG-AC-01-01](screens/admin/admin_screens_phase4.md).

**Preconditions and data:** A1 and A2 are active ADMIN accounts. A1 is assigned `ADMIN_PRIMARY`; A2 is assigned `ADMIN_SUPPORT`. Both roles currently grant `PERMISSION_MATRIX:UPDATE`; each user has one role. Snapshot permissions, permission_version, user-role links, audit rows, and Redis permission-cache entries. Admin UI uses its separate admin authentication route.

**Steps and expected checkpoints:**

1. Sign in at the admin route as A1. Check authenticated identity and admin-only area; verify I1 cannot access this route or admin permission/audit APIs.
2. Open permission matrix and disable `PERMISSION_MATRIX:UPDATE` on `ADMIN_SUPPORT`, leaving `ADMIN_PRIMARY` unchanged. Check successful response, exact cell update, one audit event, permission_version increment and invalidate publication. Confirm A1 retains the capability through `ADMIN_PRIMARY`; A2's next PATCH after success is rejected with `403 IDT-445` and creates no mutation/success audit.
3. Attempt a forbidden mutation using I1/non-admin credentials; verify rejection and no permission, audit-success, or cache mutation.
4. Open system audit log. Check the A1 action is visible with actor, target, action, and timestamp; use cursor/filter and confirm result order is stable. Verify credential/token fields are redacted. The ADMIN audit-read rule remains available even when the matrix READ cell for ordinary roles is disabled.
5. As A1, restore `ADMIN_SUPPORT`'s UPDATE cell through the API; verify a second audit/version increment and A2's capability restored. Then restore the isolated environment baseline.

**Cleanup:** Restore permission snapshot and role links; invalidate caches; reset application audit fixture rows through the isolated database reset. Preserve redacted QA evidence outside that database.

**Evidence:** Admin route and role claims, matrix before/after, mutation response, DB permissions, audit event and cache invalidation IDs, denied I1 response, filtered audit UI/API result, secret-redaction scan, and cleanup diff.

## Optional learner extension — interview recall

When F6 has an integration environment, add [TC-F6-AC01-01](modules/F6_interview-bank/F6_learner_browse_recall.md), [TC-F6-AC08-01](modules/F6_interview-bank/F6_learner_browse_recall.md), and learner screen cases [TC-USR0401-ILAC01-01](screens/users/USR0401_interview_bank_list.md) and [TC-USR0402-SCAC01-01](screens/users/USR0402_interview_question_detail.md). Use an active, published, rubric-enabled question with no preexisting L1 recall row; apply one rating at fixed time T0 and assert stored rating/next review date, then restore/delete the fixture. This path is outside the core P0 gate and must not use a live AI provider unless separately approved for cost and evidence handling.

## Missing or gated source IDs

- The selected P0 module IDs above exist in their linked F1–F5 files. The role-specific phase 4 source IDs are cited from the existing teacher/admin screen selection docs; they remain screen-design cases and do not themselves prove integrated backend authorization.
- Do not promote a journey to runnable until E2E-L has one exact deployed P1 fixture with compatible F2/F3/F4 contracts, E2E-I can seed C1/enrollment and consume assignment events, and E2E-A can restore permission/cache state safely.
- All linked case IDs in this draft were checked against their source files, including the F6 learner screen cases. SMK-P0-08 requires the F5 service with a stubbed provider; it does not assess real provider output quality.
- No source testcase ID was assigned to this phase 5 selection itself. A later run record must reference this artifact version plus each source ID, exact build/environment, run time, actual result, evidence link and defect ID where applicable.
