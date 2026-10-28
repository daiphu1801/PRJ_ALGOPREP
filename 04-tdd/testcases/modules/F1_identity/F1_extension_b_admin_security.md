# F1 extension B — Admin authorization and audit

- **Scope/TDD:** F1 AC-73–74, AC-76–78; `04-tdd/modules/F1_identity.md`.
- **DD:** `03-dd/api/F1_identity.md` §§0.5–0.7.2; `03-dd/logic/F1_identity.md` BR-13–16, BR-19; `03-dd/validation/F1_identity.md` §§10–12.
- **Fixture catalog:** fixed clock `2026-10-27T10:00:00+07:00`; A1/A2 ADMIN, N1 STUDENT, role R1 custom non-admin, permission matrix includes known cells; email sink and DB audit rows available. Exact IDs/credentials captured per run.
- **Execution:** All cases `Chưa thực hiện`.

| AC | Branch | Case | Review |
|---|---|---|---|
| 73 | server-side permission enforcement and authorized live update | TC-F1-AC73-01, TC-F1-AC73-02 | Reviewed 2026-10-28 |
| 74 | full list/reset variants and unauthorized direct access | TC-F1-AC74-01, TC-F1-AC74-02 | Reviewed 2026-10-28 |
| 76 | preserve last admin, audit, concurrent changes and self-impact acknowledgement | TC-F1-AC76-01, TC-F1-AC76-02, TC-F1-AC76-03 | Reviewed 2026-10-28 |
| 77 | legacy/new teacher overview and role scope | TC-F1-AC77-01 | Reviewed 2026-10-28 |
| 78 | shared filter set/export; deleted cursor anchor; failed CSV stream | TC-F1-AC78-01, TC-F1-AC78-02, TC-F1-AC78-03 | Reviewed 2026-10-28 |

### TC-F1-AC73-01 — Role permissions are enforced by server

| Field | Value |
|---|---|
| AC / DD source | AC-73; API #30–34; BR-13–14 |
| Kind / priority | Security; P1 — privilege escalation |
| Environment | API + DB + permission cache/event observer |
| Preconditions | A1 ADMIN has RBAC permission management; N1 has no permission update permission; R1 custom non-admin role exists; system roles exist. |
| Test data | Matrix cell `(R1, CLASS_MANAGEMENT, READ)` initial false; request to set true. |
| Steps | 1. GET #30/#32 as A1. 2. POST #33 new role R2. 3. Confirm other role cells unchanged. 4. N1 directly PATCH #31 target cell. 5. Verify DB/audit/cache state. |
| Expected | Role/matrix listing reflects persisted roles and mappings; created role does not change another role's permissions. N1 direct API write is rejected despite bypassing UI; cell remains false and no audit/invalidation side effect occurs. Authorized write is covered independently by AC73-02. |
| Cleanup | Delete R1/R2 after unassigning users and restore matrix cell. |
| Evidence | HTTP, role_permission DB rows, audit/event/cache invalidation records. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC78-02 — Cursor continues when its anchor row was cleaned up

| Field | Value |
|---|---|
| AC / DD source | AC-78; API #39; §0.7.2 cursor contract |
| Kind / priority | Recovery/Boundary; P1 — missing/repeated rows after retention cleanup |
| Environment | API + DB, controllable cleanup between requests |
| Preconditions | A1 ADMIN; 51 matching audit rows ordered by created_at desc,id desc; no concurrent insertions. |
| Test data | Rows L01–L51; page limit 50; cursor returned after L50. |
| Steps | GET first page and capture nextCursor; remove L50 row to model retention cleanup; GET next page using unchanged opaque cursor. |
| Expected | Second page succeeds using cursor's encoded timestamp/UUID pair despite missing anchor row. It returns L51 exactly once, no technical cursor error and no repeat of L01–L49. |
| Cleanup | Remove remaining audit fixtures. |
| Evidence | First/second response IDs, cursor, DB delete and ordering query. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC78-03 — CSV stream failure is not reported as a successful export

| Field | Value |
|---|---|
| AC / DD source | AC-78; API #40; §0.7.2 export failure behavior |
| Kind / priority | Recovery/Security; P1 — truncated audit export presented as complete |
| Environment | API + DB; fault-injectable CSV response stream |
| Preconditions | A1 ADMIN; matching logs exist; export action can write audit record before stream starts. |
| Test data | `categoryCode=CONFIG`, date range `2026-10-26`–`2026-10-27`, columns `time,actor,id`; inject stream failure after first data row. |
| Steps | Request #40; terminate stream after first row; inspect client transfer, server log and audit action. |
| Expected | Stream is terminated and failure logged; client does not receive a completed-success indication/file masquerading as complete. Export action log is written before response body per DD. No unfiltered or sensitive fields are emitted. |
| Cleanup | Clear injected fault and remove audit fixtures. |
| Evidence | HTTP headers/chunks/termination, server error log, export audit row. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC73-02 — Authorized matrix write is immediate, audited and invalidates role cache

| Field | Value |
|---|---|
| AC / DD source | AC-73; API #30–31; BR-13 |
| Kind / priority | Normal/Concurrency; P1 — stale or unaudited permission change |
| Environment | API + DB + two backend instances + Redis pub/sub |
| Preconditions | A1 authorized; custom role R1 assigned to test user T1; R1 `CLASS_MANAGEMENT:READ=false` cached on both instances. |
| Test data | Cell `(R1,CLASS_MANAGEMENT,READ)` false; R1 baseCategory INSTRUCTOR, T1 owns class C1 UUID `00000000-0000-4000-8000-000000000173`; protected route GET `/api/v1/identity/classes/00000000-0000-4000-8000-000000000173/students` (#26). |
| Steps | Hold invalidate delivery to instance 2. A1 PATCH #31 set true and await response; T1 calls #26 on instance 2 before delivery. Set false, await response and T1 calls #26 again while delivery stays held. Release delivery after both probes. |
| Expected | Each PATCH commits permission, monotonic role permission_version and one audit atomically; invalidate publish is observed separately. Instance 2 reads version from PostgreSQL primary before trusting cache and permits #26 after grant. After revocation the next request returns `403 IDT-445` before use case despite delayed Redis. Other role cells remain unchanged. |
| Cleanup | Reset cell false, clear caches, unassign/delete R1. |
| Evidence | PATCH responses, permission rows, audit JSON, pub/sub and instance request logs. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC74-02 — Non-admin cannot list users or reset password by direct API call

| Field | Value |
|---|---|
| AC / DD source | AC-74; API #35,#38; authorization contract §0.6 |
| Kind / priority | Security; P1 — privilege escalation/account takeover |
| Environment | API + DB + email sink; N1 STUDENT principal |
| Preconditions | N1 active; U1 target has password and active refresh token. Snapshot U1 password_hash, tokens and mail queue. |
| Test data | U1 UUID and valid endpoint payload for #38; N1 has no ADMIN role/permission. |
| Steps | N1 directly GET #35; directly POST #38 for U1; inspect target and side-effect state. |
| Expected | Both requests return 403. No user data is disclosed; U1 password_hash/tokens remain unchanged; no reset email or successful reset audit entry is created. |
| Cleanup | Remove N1/U1 and captured email fixture. |
| Evidence | HTTP responses, before/after DB snapshots, mail/audit logs. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC76-02 — Concurrent demotions serialize around the last-active-admin guard

| Field | Value |
|---|---|
| AC / DD source | AC-76; API #36; BR-15; API §3 row locking |
| Kind / priority | Concurrency/Security; P1 — concurrent lockout of all administrators |
| Environment | API + PostgreSQL transaction timing/barrier |
| Preconditions | Exactly A1,A2 are active ADMINs; R1 is non-admin. Both requests authorized and synchronized to reach guard concurrently. |
| Test data | A1 PATCH role→R1 targets A1; A2 PATCH role→R1 targets A2; both bodies include `acknowledgeSelfImpact:true`. Initial active ADMIN count=2. |
| Steps | Release both role-change requests at same barrier; wait for both transactions; inspect role count, responses and audits. |
| Expected | Row locking serializes guard and update: exactly one demotion commits; the other returns 422 IDT-432 after observing one active ADMIN remains. Final active ADMIN count=1; no zero-admin state; audit exists only for committed success. |
| Cleanup | Restore both users as ADMIN and remove fixtures. |
| Evidence | Request timing, responses, transaction/row-lock trace, role and audit rows. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC74-01 — User management is global and reset follows account auth method

| Field | Value |
|---|---|
| AC / DD source | AC-74; API #35,#38; §0.6; BR-06/16 |
| Kind / priority | Security/Recovery; P1 — unauthorized reset or secret exposure |
| Environment | API + DB + email sink + refresh token store |
| Preconditions | User dataset spans >1 page. U1 has password and two active refresh tokens; U2 OAuth-only (`password_hash IS NULL`); A1 ADMIN; N1 STUDENT. |
| Test data | `pageSize=20`; U1/U2 have distinct names and role/status; reset temporary secret is generated by service. |
| Steps | A1 filters/searches #35 across pages; reset U1 via #38; reset U2 via #38; N1 attempts list and reset. |
| Expected | Filters and pagination query full user set, not only current page. U1 receives secure reset guidance; all old U1 refresh sessions revoked; temporary password never appears in DB plaintext/audit payload. U2 receives provider guidance, no temporary password generated. N1 cannot list/change users. |
| Cleanup | Delete users, mail, refresh tokens and audit rows. |
| Evidence | HTTP, mail sink (redacted), password_hash non-plaintext inspection, token rows, audit after_json scan. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC76-01 — Role changes cannot remove final administrator

| Field | Value |
|---|---|
| AC / DD source | AC-76; API #36; BR-15 |
| Kind / priority | Security/Boundary; P1 — lock out all administrators |
| Environment | API + DB + audit log; transaction/concurrency capable |
| Preconditions | A1,A2 are the only active ADMINs; N1 is non-admin; A1 can change user roles. |
| Test data | R1 has non-admin base_category `STUDENT`; baseline two admins. |
| Steps | A1 changes N1 to R1; verify success/audit. A1 demotes A2 while A1 remains ADMIN, then restore A2 to ADMIN. A1 changes own role while A2 remains ADMIN using `{"targetRoleId":"<R1 UUID>","acknowledgeSelfImpact":true}`. Finally A2 attempts its own role change as last active ADMIN with the same flag. N1 directly calls #36. |
| Expected | Valid role changes with another active ADMIN succeed and are audited. The final active ADMIN cannot be demoted: response `422 IDT-432`, role remains ADMIN, no success audit. N1's direct request is denied. |
| Cleanup | Restore N1/A2 roles and remove fixtures. |
| Evidence | HTTP, role rows, audit entries, transaction state. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC76-03 — Self-demotion without acknowledgement is denied

| Field | Value |
|---|---|
| AC / DD source | AC-76; API #36 §0.6; logic BR-15; validation §13; DEC-2026-1027-qa-f1-self-impact-ack |
| Kind / priority | Security/Abnormal; P0 — explicit acknowledgement for losing Admin access |
| Environment | API + DB + token/audit spy |
| Preconditions | A1 and A2 are both ADMIN/ACTIVE; A1 has permission to change roles, R1 is a non-ADMIN role. |
| Test data | Two isolated PATCH #36 requests by A1 on its own userId with `targetRoleId=R1`: one omits `acknowledgeSelfImpact`, the other sends `false`. |
| Steps | Send each request; inspect response, A1 role/session and audit. Positive control on a separate snapshot sends `acknowledgeSelfImpact=true`. |
| Expected | Missing/false each returns `400 IDT-V21`, A1 remains ADMIN/ACTIVE with usable session, and no success audit. Positive control succeeds because A2 stays ADMIN/ACTIVE; A1 loses Admin access, and one role-change audit is written. |
| Cleanup | Restore A1 role/session and remove fixture audit rows. |
| Evidence | Three responses, DB role/active ADMIN counts, token authorization and audit diff. |
| Design review | Reviewed 2026-10-28; chưa thực thi |

### TC-F1-AC77-01 — Legacy teacher summary remains distinct from current overview

| Field | Value |
|---|---|
| AC / DD source | AC-77; API #44,#56; BR-17 |
| Kind / priority | Compatibility/Security; P1 — client breakage or cross-class leakage |
| Environment | API + DB with teacher overview endpoints |
| Preconditions | I1 owns C1,C2; I2 owns C3. S1 is enrolled in C1 and C2, S2 in C2, S3 in C1, S4 in C3. Each class has identical mean score 8/10 so legacy mean is unambiguous. |
| Test data | C1/C2 have same `created_at` with ordered UUIDs for tie rule; C1 and C2 each have 2 active students; C3 has 1. F4 accepted ratios for each class yield mean 8/10. |
| Steps | I1 calls legacy #44 and current #56; open current `INS0101` overview contract #56. I2 calls its own endpoints; N1 attempts #56. |
| Expected | Legacy #44 stays at its path and reports two classes, three distinct students, average score 8.0 across I1 classes. Current #56 returns `classCount=2`, `studentCount=3`, scoped items C1/C2 ordered newest first then classId ascending on tie; per-class stats use active enrollment and assignment results. C3 never appears for I1. Non-INSTRUCTOR is denied. The legacy count/value contract is not silently substituted into current overview fields. |
| Cleanup | Remove class and aggregate fixtures. |
| Evidence | Both raw responses, endpoint schemas, owner-scoped DB query results. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC78-01 — Audit search cursor and CSV export use identical filters

| Field | Value |
|---|---|
| AC / DD source | AC-78; API #39–40, §0.7.2; validation §12; BR-16/19 |
| Kind / priority | Security/Boundary; P1 — incomplete export or secret disclosure |
| Environment | API + DB, ADMIN JWT, CSV stream capture |
| Preconditions | A1 ADMIN; at least 55 matching logs plus non-matching logs outside selected category/actor/date; retention window includes range. |
| Test data | Filter `categoryCode=CONFIG`, `q=qa-admin@example.test` (actor email), date range `2026-10-26` through `2026-10-26` UTC+7 (export itself occurs Oct 27 outside selected day); export columns `time,actor,category,id`. Include matching and nonmatching actor rows plus a `before_json`/`after_json` secret sentinel only in protected DB row. |
| Steps | 1. #39 first page with limit 50 and chosen filters. 2. Follow returned opaque cursor. 3. Change filters and verify page restarts without old cursor. 4. Call #40 with same filters and columns. 5. In separate requests try future date, invalid columns, retention-violating range and q of 101 characters. |
| Expected | Results ordered `(created_at DESC,id DESC)`; next page has no duplicate/missing row among matching snapshot; cursor retains filter context. Changed filters start from first page. CSV includes all matching filtered records and only selected headers in requested order; no auth token/OTP/before_json/after_json sentinel. Invalid date/columns return 400 IDT-443; too-long q returns 400 IDT-444. |
| Cleanup | Delete audit fixtures and restore retention setting. |
| Evidence | JSON pages/cursors, CSV headers/row IDs, secret scan, error responses. |
| Design review | Reviewed 2026-10-28 |
