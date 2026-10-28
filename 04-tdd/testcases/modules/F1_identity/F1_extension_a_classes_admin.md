# QA testcase F1 — class deletion, settings, RBAC and enrollment limits

Scope: [TDD F1](../../../modules/F1_identity.md), AC-43–45 and AC-49–50. Design only; execution status **Chưa thực hiện**. API prefix `/api/v1/identity`.

DD sources: `03-dd/api/F1_identity.md` #14–15, #22, #25/#25A/#25B, #30–31; `03-dd/logic/F1_identity.md` BR-09, BR-13, BR-22; `03-dd/validation/F1_identity.md` §§8–9; `02-bd/database/F1_identity.md` §§1.3–1.5, 1.8–1.10, 1.12; `DEC-2026-1022-db-sync-round4`, `DEC-2026-1023-db-actor-review-fixes`.

Fixture catalog: isolated DB, Redis and RabbitMQ spy. I1 owns class C1; I2 owns C2; learner S1 has active enrollments C1/C3 and left enrollment C2; S2 has active enrollment C4. Invite code `AB12CD34` belongs to C1 and is valid through T0+1 day. T0 frozen to `2026-10-27T12:00:00+07:00`. Seed permission matrix from current seed contract (three roles × eleven functions × four actions) and do not assume any other role grants.

| AC | Branch | Case | Design review |
| :--- | :--- | :--- | :--- |
| 43 | Class hard-delete cascade, event and owner enforcement | TC-F1-AC43-01, TC-F1-AC43-02 | Reviewed 2026-10-28 |
| 44 | User-scoped preference defaults and validation | TC-F1-AC44-01, TC-F1-AC44-02 | Reviewed 2026-10-28 |
| 45 | Seeded LANGUAGE_CONFIG permission | TC-F1-AC45-01, TC-F1-AC45-02 | Reviewed 2026-10-28 |
| 49 | Active enrollments only, not-found equivalence and order | TC-F1-AC49-01, TC-F1-AC49-02 | Reviewed 2026-10-28 |
| 50 | Invite-code failed-attempt rate limit | TC-F1-AC50-01, TC-F1-AC50-02 | Reviewed 2026-10-28 |

### Branch inventory

| AC | Normal | Abnormal | Boundary | Security | Concurrency | Recovery / degraded |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 43 | Owned class deletion: TC-F1-AC43-01 | Foreign-owner delete: TC-F1-AC43-02 | FK/event side effects: TC-F1-AC43-01 | Owner authorization: both cases | API §3 lists class operations as ordinary single-user writes, no meaningful conflict | Problem-bank event consumer idempotence: TC-F1-AC43-01 |
| 44 | Defaults/valid values: TC-F1-AC44-01 | Invalid values preserving prior row: TC-F1-AC44-01/02 | Default-to-custom: TC-F1-AC44-01 | Per-user scope: TC-F1-AC44-01 | API §3 says same-user writes last update wins; no separate business race | AI dependency failure: TC-F1-AC44-01 |
| 45 | Seeded matrix/admin allow: TC-F1-AC45-01 | Student/instructor deny: TC-F1-AC45-01 | 132 cells: TC-F1-AC45-01 | Server-side permission enforcement and grant: TC-F1-AC45-01/02 | No matrix concurrent-write rule in DD | Authorized grant takes effect: TC-F1-AC45-02 |
| 49 | Own active class list/detail: TC-F1-AC49-01 | Left/other/nonexistent detail same 404: TC-F1-AC49-01 | Joined ordering: TC-F1-AC49-02 | Owner derived from JWT: both cases | Read-only; no meaningful race specified | — |
| 50 | Successful join and reset: TC-F1-AC50-01 | Invalid code/locked attempt: TC-F1-AC50-01 | Fifth vs sixth and window expiry: TC-F1-AC50-01 | Per-account counter isolation: TC-F1-AC50-01 | Atomic Redis failed-attempt set under concurrent calls: TC-F1-AC50-02 | Retry after sliding window: TC-F1-AC50-01 |

### TC-F1-AC43-01 — Class deletion cascades owned records and publishes ClassDeleted

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-43; API #22; BR-22; BD §§1.8, 1.10, 1.12 |
| Kind / priority | Normal, Security, Integration; P0, irreversible class lifecycle |
| Environment | API + DB + RabbitMQ consumer spies for identity/problem-bank + F4 history fixture |
| Preconditions | I1 owns C1; I2 owns C2. C1 has valid invite code, enrollments for S1/S3 (one left), two `identity_recent_activity` rows scoped to C1, class assignments and F4 submission history. Event spies are empty. |
| Test data | I1 calls `DELETE /classes/C1`. |
| Steps | 1. Delete C1. 2. Inspect DB transaction results and event delivery. 3. Allow problem-bank consumer to process event. 4. Check personal feed and F4 history. |
| Expected | C1, its invite codes and enrollments are removed by FK cascade; matching feed `scope_class_id` becomes NULL; personal activity and F4 submission history remain. Exactly one `ClassDeleted {classId:C1, occurredAt}` is published after commit; problem-bank marks C1 assignment rows removed idempotently. Identity does not write problem-bank schema directly. |
| Cleanup | Restore isolated DB snapshot and clear event queues. |
| Evidence | API response; before/after rows in identity, problem-bank and F4; event payload/order; consumer state. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC43-02 — Non-owner cannot delete a class or emit ClassDeleted

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-43; API #22 / §0.4.3; BR-22; DB `classes` |
| Kind / priority | Security; P0, class ownership boundary |
| Environment | API + DB + RabbitMQ spy |
| Preconditions | I1 owns C1; I2 owns C2. C1 has invite code, enrollments, activity rows and assignments. Event spy empty. |
| Test data | I2 sends `DELETE /classes/C1`. |
| Steps | Call delete as I2; inspect response, all C1 records and event spy. |
| Expected | Request returns `404 IDT-414` per the common class ownership contract; C1 and all related records remain unchanged, no `ClassDeleted` event is published, and no assignment is marked removed. Response does not disclose whether C1 exists. |
| Cleanup | Remove isolated fixtures. |
| Evidence | Response; before/after rows across identity/problem-bank; event spy. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC44-01 — Interview preferences default per user and reject invalid values

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-44; API #14–15; BR-07; validation §8; `DEC-2026-1022-db-sync-round4` ID-23 |
| Kind / priority | Normal, Abnormal, Security; P2 |
| Environment | API + DB + AI-service stub |
| Preconditions | U1 and U2 authenticated, neither has `user_preferences` row. AI service stub returns temporary failure. |
| Test data | U1 GET settings; PATCH `preferredMaxTurns=16`, `preferredHintAllowed=false`; GET; attempt value 10. U2 GET settings. |
| Steps | Perform operations; inspect preference rows and responses even when AI stub is failing. |
| Expected | Missing preferences return defaults 12 turns and hints enabled. U1's valid values persist and appear on reread. Value 10 returns `422 IDT-V18`; prior settings remain unchanged. U2 remains at its own defaults. AI failure does not prevent persistence. |
| Cleanup | Delete preferences and reset AI stub. |
| Evidence | Requests/responses, per-user preference rows, AI stub calls. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC44-02 — Non-boolean hint preference is rejected without partial update

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-44; API #15; validation §8 (`preferredHintAllowed` Boolean, `422 IDT-V18`) |
| Kind / priority | Abnormal, Boundary; P2 |
| Environment | API + DB |
| Preconditions | U1 has stored `preferredMaxTurns=16`, `preferredHintAllowed=false`, `preferredInterviewLevel=JUNIOR`. |
| Test data | PATCH #15 body `{"preferredMaxTurns":8,"preferredHintAllowed":"true"}` where the Boolean is incorrectly encoded as a string. |
| Steps | Submit update; read settings and inspect preference row. |
| Expected | Request returns `422 IDT-V18`; no field changes, including otherwise-valid `preferredMaxTurns=8`. Previously saved values and omitted `preferredInterviewLevel` remain unchanged. |
| Cleanup | Restore/delete U1 preference row. |
| Evidence | Request/response and before/after preference row. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC45-01 — Default permission seed includes LANGUAGE_CONFIG

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-45; API #30–31; BR-13; `DEC-2026-1023-db-actor-review-fixes` |
| Kind / priority | Security, Boundary; P0, role boundary |
| Environment | Fresh DB + seed runner + API permission checks |
| Preconditions | Fresh DB, seed has completed. Three system roles and 11 functions including `LANGUAGE_CONFIG` exist; each function has four actions per AC. |
| Test data | Inspect `permissions`; call language-config read/action as ADMIN, INSTRUCTOR and LEARNER. |
| Steps | 1. Count matrix rows and inspect LANGUAGE_CONFIG cells. 2. Attempt function access as each role. 3. Use authorized ADMIN matrix update to grant `READ` to INSTRUCTOR, save, then retry the server-protected function. |
| Expected | Seed contains 132 cells for 3 system roles × 11 functions × 4 actions. ADMIN is allowed to configure language; INSTRUCTOR and STUDENT are denied. Existing unrelated cells stay unchanged. After authorized grant/save, instructor matrix cell becomes true, but GET `/api/v1/judge/admin/language-config` still returns `403 JDG-301` because F4 §23–24 also requires ADMIN baseCategory. Granting Layer 2 permission never bypasses Layer 1 role guard. |
| Cleanup | Drop fresh DB or restore seed snapshot. |
| Evidence | Seed counts/rows, role API responses, before/after permission values. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC45-02 — Unauthorized matrix update cannot grant LANGUAGE_CONFIG

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-45; API #30–31; BR-13 |
| Kind / priority | Security; P0, server-enforced authorization |
| Environment | API + DB + audit log |
| Preconditions | Student S1 has no RBAC administration permission. INSTRUCTOR's LANGUAGE_CONFIG cells are denied. |
| Test data | S1 directly PATCHes a LANGUAGE_CONFIG permission cell for INSTRUCTOR. |
| Steps | Send update directly, bypassing UI; query matrix and audit log. Attempt language config as instructor. |
| Expected | Update denied by server; matrix cell and audit log remain unchanged; instructor remains denied. Exact error code follows common permission-denied contract and is not newly specified by AC. |
| Cleanup | Restore seed snapshot. |
| Evidence | Response, matrix before/after, audit rows and follow-up authorization result. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC49-01 — Learner class list and detail expose active own enrollments only

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-49; API #25A–25B; BR-09; validation §9 |
| Kind / priority | Security, Boundary; P0, tenant isolation |
| Environment | API + DB |
| Preconditions | S1 active in C1 and C3, has left C2; S2 active in C4; all classes have distinct names. |
| Test data | S1 calls #25A, then #25B for C1, C2, C4 and random UUID. |
| Steps | Compare list to enrollment rows; read each requested detail. |
| Expected | List includes only C1/C3, ordered by most recently joined first per AC. Detail for C1 succeeds. Left C2, other learner's C4 and nonexistent UUID all return same `404 IDT-411`, without revealing class existence. |
| Cleanup | Delete enrollments and classes. |
| Evidence | Requests/responses, enrollment rows and response comparison for equivalent not-found cases. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC49-02 — Active class list order is stable by joined time then class ID

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-49; API #25A; `03-dd/api/F1_identity.md` §0.4.1 |
| Kind / priority | Normal, Boundary; P2, stable list ordering |
| Environment | API + DB |
| Preconditions | S1 active in C1/C3/C5; C1 and C3 have same `joined_at`, C5 has later `joined_at`; S1 has left C2. |
| Test data | GET #25A as S1. |
| Steps | Call twice without changing fixture and compare `items` order. |
| Expected | Newest `joinedAt` first; ties ordered by ascending `classId`; left enrollment excluded. Both calls return identical ordering. |
| Cleanup | Delete fixtures. |
| Evidence | Responses and enrollment rows/timestamps. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC50-01 — Invalid invite-code attempts are rate-limited per learner

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-50; API #25 and §0.4.1; BR-09; `DEC-2026-1024-dd-student-class-contracts` |
| Kind / priority | Security, Boundary, Recovery; P1, abuse prevention |
| Environment | API + DB + Redis + clock |
| Preconditions | S1 has valid code `AB12CD34`; no wrong-attempt Redis key. S2 is a control account with separate key. |
| Test data | S1 submits five invalid codes then valid code during 15-minute window; retry after the window. Repeat using valid code succeeding once, then probe whether counter resets. |
| Steps | Capture responses for attempts 1–6, enrollment rows and Redis sorted set/TTL; repeat with S2 to verify per-account scope. Advance beyond 15 minutes and retry valid code. In a separate fixture, join successfully before five failures and inspect counter. |
| Expected | Wrong-code attempts 1–5 each return `422 IDT-410`; the fifth still increments failed-attempt set. Attempt 6 returns `429 IDT-412` even with the valid code, and creates no enrollment. After the sliding 15-minute window expires, valid code joins successfully. Successful join clears that learner's sorted set. S2's counter is independent. Invalid, malformed, nonexistent and expired codes return the same `422 IDT-410` without disclosing class existence. |
| Cleanup | Delete enrollments and Redis counters. |
| Evidence | Request/responses, DB enrollment diff, Redis sorted set/TTL per user. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC50-02 — Concurrent failed joins preserve the atomic five-attempt quota

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-50; API #25 / §0.4.1; `DEC-2026-1024-dd-student-class-contracts` Redis Lua counter |
| Kind / priority | Concurrency, Security; P1, rate-limit race |
| Environment | API + Redis + DB + concurrent clients |
| Preconditions | S1 has no failure set and is not enrolled in C1; S2 has separate counter key. |
| Test data | Five simultaneous wrong-code #25 requests by S1; then a sixth request with valid `AB12CD34`. One simultaneous wrong request by S2. |
| Steps | Submit five wrong requests concurrently; inspect all responses and sorted set. Submit sixth valid request; inspect enrollment and counters. Check S2 key. |
| Expected | Atomic Lua operations record exactly five wrong attempts; each wrong request returns `422 IDT-410`. Sixth valid request is blocked with `429 IDT-412` and does not create enrollment. S2's failure set is independent. |
| Cleanup | Delete counters and enrollment fixtures. |
| Evidence | All responses, Redis sorted set members/TTL, DB enrollment diff and per-user keys. |
| Design review | Reviewed 2026-10-28; not executed |
