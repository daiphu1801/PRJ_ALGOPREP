# QA testcase F1 — verification, account lifecycle and recovery

Scope: [TDD F1](../../../modules/F1_identity.md), AC-35–38, AC-41–42, AC-46–48, AC-54–55. Design only; execution status **Chưa thực hiện**. API prefix `/api/v1/identity`.

DD sources: `03-dd/api/F1_identity.md` #2, #8, #50, #53–54, #60–61 and §1; `03-dd/logic/F1_identity.md` BR-04, BR-06, BR-08, BR-21, BR-23; `03-dd/validation/F1_identity.md` §§4, 7–8; `03-dd/jobs/F1_identity.md` §§1–2; `02-bd/database/F1_identity.md` §§1, 3; `DEC-2026-1022-db-sync-round2`, `DEC-2026-1022-db-sync-round3`, `DEC-2026-1023-db-actor-review-fixes`, `DEC-2026-1025-dd-auth-recovery-contracts`.

Fixture catalog: isolated application, DB, Redis and mail/event spies. T0 fixed per test; do not share accounts across cases. Self-registered users start `PENDING_VERIFICATION`, `must_change_password=false`; admin-created users start `PENDING_VERIFICATION`, `must_change_password=true`, with temporary credentials delivered only through mail fixture. OTP is a six-digit fixture with ten-minute TTL; restore tickets are opaque, one-use, five-minute Redis values. Password fixture meets `03-dd/validation/F1_identity.md` §1. Seed exact role and account-source fields for each scenario.

| AC | Branch | Case | Design review |
| :--- | :--- | :--- | :--- |
| 35 | Admin-created first login and soft password prompt | TC-F1-AC35-01, TC-F1-AC35-02 | Reviewed 2026-10-28 |
| 36 | Correct email OTP, one-time activation and account-source restriction | TC-F1-AC36-01, TC-F1-AC36-02 | Reviewed 2026-10-28 |
| 37 | Wrong/expired/exhausted OTP | TC-F1-AC37-01, TC-F1-AC37-02 | Reviewed 2026-10-28 |
| 38 | Resend throttle, expiry and enumeration resistance | TC-F1-AC38-01, TC-F1-AC38-02 | Reviewed 2026-10-28 |
| 41 | Pending self-registration blocked until verify | TC-F1-AC41-01 | Reviewed 2026-10-28 |
| 42 | Anonymized self-delete cannot be admin-restored | TC-F1-AC42-01 | Reviewed 2026-10-28 |
| 46 | Bootstrap initial administrator | TC-F1-AC46-01, TC-F1-AC46-02 | Reviewed 2026-10-28 |
| 47 | Anonymization selection, cutoff and idempotence | TC-F1-AC47-01, TC-F1-AC47-02, TC-F1-AC47-03 | Reviewed 2026-10-28 |
| 48 | Default language persistence and enum validation | TC-F1-AC48-01 | Reviewed 2026-10-28 |
| 54 | Verify reset OTP before password step | TC-F1-AC54-01, TC-F1-AC54-02 | Reviewed 2026-10-28 |
| 54 | DB rollback keeps OTP; Redis cleanup failure cannot replay committed reset | TC-F1-AC54-03 | Đã review; chưa thực thi |
| 55 | Restore ticket eligible, one-use, no session | TC-F1-AC55-01, TC-F1-AC55-02, TC-F1-AC55-03 | Reviewed 2026-10-28 |

### Branch inventory

| AC | Normal | Abnormal | Boundary | Security | Concurrency | Recovery / degraded |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 35 | Admin first login/change: TC-F1-AC35-01 | Wrong current password: TC-F1-AC35-02 | First login activation: TC-F1-AC35-01 | Self/OAuth control accounts: TC-F1-AC35-01 | No meaningful per-user write race in API §3 | Flag clears only after successful password change: both cases |
| 36 | Correct OTP activation: TC-F1-AC36-01 | Non-self-register OTP: TC-F1-AC36-02 | OTP reuse does not activate twice: TC-F1-AC36-01 | OTP restricted to self-registration: TC-F1-AC36-02 | No separate race contract; endpoint idempotent replay is covered | — |
| 37 | — | Five wrong/then right and generic expiry: TC-F1-AC37-01/02 | Fifth failure cap: TC-F1-AC37-01 | No activation/session on failure: both cases | No OTP counter concurrency contract specified | Expired/unknown OTP recovery requires fresh valid OTP; no token emitted |
| 38 | Resend pending account: TC-F1-AC38-01 | Cooldown, unknown/admin: TC-F1-AC38-01 | 60-second cooldown expiry: TC-F1-AC38-02 | No enumeration or OTP to admin: both cases | Atomic resend limiter not defined | Resend after cooldown: TC-F1-AC38-02 |
| 41 | Login after verify: TC-F1-AC41-01 | Pending and wrong credentials: TC-F1-AC41-01 | Pending→active transition: TC-F1-AC41-01 | Student/instructor paths and no token: TC-F1-AC41-01 | No concurrency branch in contract | Verify then retry login: TC-F1-AC41-01 |
| 42 | In-grace restore via admin: TC-F1-AC42-01 | Anonymized denial: TC-F1-AC42-01 | Anonymized vs grace: TC-F1-AC42-01 | Direct server request and partial outcome: TC-F1-AC42-01 | No concurrency branch specified | Admin may restore only eligible account: TC-F1-AC42-01 |
| 46 | Initial bootstrap: TC-F1-AC46-01 | Missing/invalid configuration: TC-F1-AC46-01 | Existing admin means no config read: TC-F1-AC46-01 | Secret redaction/ADMIN privilege: TC-F1-AC46-01 | Two simultaneous starts: TC-F1-AC46-02 | Restart idempotence: TC-F1-AC46-01/02 |
| 47 | Expired SELF anonymized: TC-F1-AC47-01 | ADMIN and not-yet-due skipped: TC-F1-AC47-01/02 | Exact 30-day cutoff: TC-F1-AC47-02 | Preserve admin-locked data, remove self PII: TC-F1-AC47-01/02 | Job/restore row-lock race: TC-F1-AC47-03 | Repeat run no changes: TC-F1-AC47-01 |
| 48 | Supported language persistence: TC-F1-AC48-01 | Unsupported enum: TC-F1-AC48-01 | Initial default/readback: TC-F1-AC48-01 | Owner-derived preferences | API §3: single-user write race not meaningful | Omitted unrelated settings unchanged: TC-F1-AC48-01 |
| 54 | Verify then reset: TC-F1-AC54-01 | Wrong/expired/used generic failure: TC-F1-AC54-01 | TTL and one-time consume: TC-F1-AC54-01/02 | Unknown email generic error/no session: TC-F1-AC54-01 | Concurrent consume: TC-F1-AC54-02 | Valid verify retains original TTL for reset: TC-F1-AC54-01 |
| 55 | Eligible restore: TC-F1-AC55-01 | Ineligible/expired/used ticket: TC-F1-AC55-02 | 30-day/5-minute windows: TC-F1-AC55-01/02 | Admin/anonymized excluded, no session: TC-F1-AC55-01/02 | Concurrent ticket redemption: TC-F1-AC55-03 | Retry login after restore: TC-F1-AC55-01 |

### TC-F1-AC35-01 — Admin-created account gets a soft first-login password prompt

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-35; API #50, #2, #9; BR-21 and BR-06 |
| Kind / priority | Normal, Security; P1 |
| Environment | API + DB + mail spy + authenticated UI |
| Preconditions | Admin A has user-create permission. U1 does not exist. |
| Test data | Create U1 via #50 with compliant email/displayName/roleId; test-only secure password generator yields `T3mp!Pass-2026` captured in mail sink (not a client-supplied password field); no email OTP. Login as U1, read #9, change password via `POST /me/password` with current temporary password and compliant new password `N3w!Pass-2026`. Include self-registered U2 (`ACTIVE`, flag false) and OAuth U3 (`ACTIVE`, flag false) as controls. |
| Steps | 1. Create U1 and inspect row/mail/Redis. 2. Login with temporary password. 3. Read #9 and call another unrelated allowed endpoint. 4. Complete password change. 5. Read profile and login response again. |
| Expected | U1 is created `PENDING_VERIFICATION`, `must_change_password=true`; temporary secret delivered by mail, no OTP Redis key. Successful first login activates U1 and returns `mustChangePassword=true`. Other allowed endpoint still works; no server-side universal block. UI shows prompt. Successful first password change clears flag and stores change time; prompt is absent afterwards. U2/U3 do not receive this prompt. |
| Cleanup | Delete U1–U3 and related session/mail fixtures. |
| Evidence | Create/login/profile/password responses; user rows; mail and Redis spy; UI capture. Redact credentials in retained evidence. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC47-03 — Restore and anonymization serialize on the account row

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-47/55; BR-08; JOB-ID-02 §2 `FOR UPDATE SKIP LOCKED`; API #61 row lock |
| Kind / priority | Concurrency, Recovery, Security; P0, prevents partial account recovery/anonymization |
| Environment | DB + job runner + API #2/#61 + Redis + fixed clock |
| Preconditions | U1 is SELF-deactivated exactly 30 days ago, has PII/OAuth identity, and holds a live restore ticket issued after credential verification. |
| Test data | Race JOB-ID-02 selection/update against POST #61 for U1, repeating with controlled lock acquisition order in isolated runs. |
| Steps | Run two schedules: (a) restore acquires row lock first, then job; (b) job acquires row lock first, then restore resumes. Inspect account, OAuth and ticket state. |
| Expected | (a) Restore wins: account becomes ACTIVE, deactivation fields clear, ticket consumed; job skips locked row and does not anonymize. (b) Job wins: account is fully anonymized and OAuth identity removed; restore is rejected with generic `401 IDT-114`. No partial PII/OAuth changes or simultaneous ACTIVE+anonymized state. |
| Cleanup | Restore isolated database snapshot and remove ticket. |
| Evidence | Lock/transaction traces, both API/job outcomes, user/OAuth rows before and after, ticket state. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC35-02 — Failed first password change does not clear the soft-change flag

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-35; API #50, #2, #9; BR-21 and BR-06 |
| Kind / priority | Abnormal, Security; P1 |
| Environment | API + DB + authenticated UI |
| Preconditions | U1 is an admin-created account that has completed first login; `status=ACTIVE`, `must_change_password=true`; current temporary password remains valid. |
| Test data | Call `POST /me/password` with incorrect `currentPassword` and compliant proposed password. |
| Steps | Attempt change; reread profile and user row; retry with correct current password and inspect again. |
| Expected | Failed change is rejected; password hash and `must_change_password=true` remain unchanged; prompt remains visible. Only successful change clears flag. API does not impose a global lock on unrelated endpoints while flag is true. |
| Cleanup | Delete U1 and session. |
| Evidence | Response, before/after password hash and flag, UI prompt capture. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC36-01 — Correct self-registration email OTP activates once

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-36; API #53; BR-21; API §1 email verification |
| Kind / priority | Normal, idempotency; P1 |
| Environment | API + DB + Redis + mail spy |
| Preconditions | Self-registered U1 remains `PENDING_VERIFICATION`, flag false; Redis OTP key has 10-minute TTL, failed count below 5. |
| Test data | `POST /auth/email/verify` body `{"email":"u1@example.test","otp":"482619"}`. |
| Steps | Submit correct OTP; inspect response, user, Redis, then replay the same request. |
| Expected | First request returns documented success and changes U1 to `ACTIVE`; verification key is deleted. Replay does not change state or cause a second activation side effect; API DD specifies idempotent 200 for an already-active account (marked inference). No session is issued by verification endpoint. |
| Cleanup | Delete U1 and Redis/mail fixtures. |
| Evidence | Responses, DB before/after, Redis key state, mail spy. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC36-02 — Email verification OTP cannot activate an admin-created account

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-36; API #50/#53; BR-21; `DEC-2026-1022-db-sync-round2` |
| Kind / priority | Security, Abnormal; P1 |
| Environment | API + DB + Redis + mail spy |
| Preconditions | U1 created by Admin: `PENDING_VERIFICATION`, `must_change_password=true`; no `identity:emailverify:<userId>` key or OTP email exists. |
| Test data | Attempt #53 with U1 email and an arbitrary six-digit OTP. |
| Steps | Submit verify request; inspect response, user and Redis/mail state. |
| Expected | Request is rejected as generic invalid/unavailable verification code per #53 `422 IDT-112`; U1 remains pending and flag true; no verification key or session is created. Admin-created account follows BR-21 temporary-password first-login route, not email OTP. |
| Cleanup | Delete U1 and mail/Redis fixtures. |
| Evidence | Response, user row, Redis key lookup and mail spy. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC37-01 — Wrong or expired email OTP cannot activate the account

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-37; API #53; BR-21; validation §4 |
| Kind / priority | Abnormal, Boundary, Security; P1 |
| Environment | API + DB + Redis + clock |
| Preconditions | U1 is self-registered and pending with current OTP; failed count is zero. |
| Test data | Submit five consecutive wrong six-digit values for U1, followed by the correct OTP. |
| Steps | Make five wrong calls; call again with the correct code; inspect response, status, Redis key and session state. |
| Expected | Each wrong/exhausted request returns generic `422 IDT-112`. After five failures the old OTP cannot activate even when correct; U1 remains `PENDING_VERIFICATION`; no session or active-state transition. |
| Cleanup | Delete U1 and OTP/counter Redis keys. |
| Evidence | Requests/responses, user row, Redis TTL/counter, auth/session spy. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC37-02 — Expired and unknown email OTP share the generic failure

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-37; API #53; BR-21; validation §4 |
| Kind / priority | Abnormal, Boundary, Security; P1 |
| Environment | API + DB + Redis + clock |
| Preconditions | U1 pending with expired OTP; U2 is an unknown email; both requests use same request shape. |
| Test data | Submit the expired six-digit OTP for U1; submit any six-digit value for U2. |
| Steps | Call #53 for each and compare public responses and account/session state. |
| Expected | Both return generic `422 IDT-112` without revealing account existence or reason. U1 remains pending; U2 remains nonexistent; neither call creates a session or activates an account. |
| Cleanup | Remove U1 and Redis key. |
| Evidence | Responses, DB state and session spy. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC38-01 — Verification resend is throttled and does not enumerate accounts

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-38; API #54; validation §2; BR-21 |
| Kind / priority | Security, Boundary; P1 |
| Environment | API + Redis + mail spy + clock |
| Preconditions | U1 is self-registered and pending; A1 is admin-created and pending; absent email has no account. Resend cooldown expired for U1. |
| Test data | Resend U1 at T0, resend U1 at T0+30s; resend absent email and A1 email. |
| Steps | Capture each response, OTP/mail spy and Redis key/TTL. |
| Expected | First U1 resend issues a new OTP. Second within 60 seconds returns `429 IDT-V12` and sends no mail. Absent and A1 emails receive the same generic 200 response as applicable; neither emits an OTP email. No OTP is issued for Admin-created accounts. |
| Cleanup | Remove Redis keys and mail records. |
| Evidence | Responses, Redis values/TTL, mail spy records. Do not retain actual OTP values in logs. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC38-02 — Resend becomes available after cooldown and remains generic for active accounts

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-38; API #54; validation §2; BR-21 |
| Kind / priority | Boundary, Security, Recovery; P1 |
| Environment | API + Redis + mail spy + clock |
| Preconditions | U1 is pending and received a verification OTP at T0; U2 is already ACTIVE. |
| Test data | Resend U1 at T0+61 seconds; resend U2 at same time. |
| Steps | Capture response, Redis values/TTL and mail events. |
| Expected | U1 may receive a new OTP after cooldown; resend response is generic 200 and mail is sent only for pending self-registration. U2 receives same generic 200 but no OTP/mail/key. |
| Cleanup | Delete Redis and mail records. |
| Evidence | Responses, key/TTL diff, mail spy and user states. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC41-01 — Pending self-registration login is denied until email verification

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-41; API #2/#49/#53; BR-21; validation §7 |
| Kind / priority | Security; P0, unauthenticated access control |
| Environment | API + DB + Redis + session spy |
| Preconditions | U1 self-registration is pending/flag false with correct password and valid OTP. A1 admin-created is pending/flag true with temp password. |
| Test data | Correct-password calls to student and instructor login for U1; wrong-password control; verify U1; retry login; admin-created A1 login. |
| Steps | Attempt calls in order and inspect session/token, DB status and error shape. |
| Expected | Correct password for pending self-register is denied `403 IDT-113` on both login endpoints, with no token/session and no state change. Wrong password retains ordinary invalid-credentials response. After valid email verification, login succeeds. A1 temporary-password login activates account and succeeds. |
| Cleanup | Revoke/delete sessions and fixture accounts/OTP. |
| Evidence | Responses, token/session spy, user rows, OTP state. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC42-01 — Admin cannot reopen an anonymized self-deactivated account

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-42; API #37; BR-08; jobs §2 |
| Kind / priority | Security, Boundary; P1 |
| Environment | API + DB + job fixture |
| Preconditions | U1 is `DEACTIVATED`, source `SELF`, `anonymized_at` non-null. U2 is self-deactivated but still within grace period and not anonymized. Admin A can list/status-edit users. |
| Test data | GET #35, rồi hai request PATCH `/api/v1/identity/admin/users/{userId}/status` riêng cho U1/U2, body `{"targetStatus":"ACTIVE"}`. |
| Steps | Perform the two status changes and inspect response, rows and audit record. |
| Expected | U1 trả `422 IDT-448`, giữ nguyên user/phiên và không ghi audit thành công. U2 được mở lại: `status=ACTIVE`, `deactivation_source=NULL`, `deactivated_at=NULL`. Màn gom hai response thành kết quả một phần, chỉ U2 thành công; từng request #37 vẫn dành cho một user. |
| Cleanup | Restore isolated user rows to fixture baseline. |
| Evidence | Requests/responses, before/after rows, audit records. |
| Design review | Đã review; owner chốt 422 IDT-448 ngày 2026-10-28; chưa thực thi |

### TC-F1-AC46-01 — Bootstrap provisions the first ADMIN once

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-46; API startup; BR-23; jobs §1 |
| Kind / priority | Recovery, Security; P0, privileged bootstrap |
| Environment | Fresh DB + application startup + environment config + login API |
| Preconditions | DB contains no ADMIN. Bootstrap email and password are configured. |
| Test data | Configured email `bootstrap@example.test`; initial password `Bootstrap!2026`; restart later with a changed configured password. Separate fresh DB trials: omit email, then omit password; also try a password that violates validation §1. |
| Steps | 1. Start app with valid config and inspect ADMIN row. 2. Restart with changed configured password. 3. Login with original and changed password. 4. On isolated fresh DB, omit each variable in separate starts and test invalid password in another start. |
| Expected | First start inserts exactly one ADMIN with configured email, `PENDING_VERIFICATION`, `must_change_password=true`, and BCrypt password hash. First login activates account and requires password change. Restart does not create another administrator, read changed password to rotate the existing credential, or change its state. If no ADMIN exists, either missing variable or invalid password causes configuration error and application startup stops without inserting an account. |
| Cleanup | Destroy isolated DBs and revoke secrets. |
| Evidence | Startup exit/status and redacted configuration error; user rows; login response; job invocation record. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC46-02 — Simultaneous application starts create exactly one bootstrap ADMIN

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-46; JOB-ID-01 §1; BR-23; users email uniqueness |
| Kind / priority | Concurrency, Recovery; P0, protects one-time privileged bootstrap |
| Environment | Fresh shared DB + two application instances + startup logs |
| Preconditions | Fresh database after migrations/seeds, no ADMIN; both instances have same valid bootstrap email/password configuration. |
| Test data | Start two instances concurrently so their ADMIN existence checks overlap. |
| Steps | Await both application starts; inspect ADMIN rows and both startup outcomes. |
| Expected | Exactly one ADMIN row is inserted with expected state and BCrypt hash. The losing instance treats the unique-email conflict as successful completion; neither creates a second account or logs the password. |
| Cleanup | Destroy isolated DB and revoke secrets. |
| Evidence | Startup logs with secrets redacted, rows, unique constraint result and job outcomes. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC47-01 — Scheduled anonymization selects only expired SELF deactivations

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-47; BR-08; jobs §2; BD `users` and `oauth_identities` |
| Kind / priority | Security, Recovery, Idempotency; P0, personal data lifecycle |
| Environment | Job runner + DB + clock |
| Preconditions | U1: SELF, deactivated 31 days ago, not anonymized, OAuth and free-form profile fields. U2: ADMIN, deactivated 31 days ago, not anonymized. U3: SELF, deactivated 29 days ago. |
| Test data | Run JOB-ID-02 twice at fixed T0; inspect users and OAuth identities. |
| Steps | Snapshot rows, run job, inspect, then rerun and compare. |
| Expected | U1 is anonymized once according to BR-08: email becomes `deleted-user-<id>@anon.local`, display name becomes `Người dùng đã xoá`, phone/school/company/current/target positions become NULL, OAuth identity rows are deleted; identity user row/history remains. U2 and U3 remain unchanged. Second run changes no additional records. |
| Cleanup | Restore isolated DB snapshot. |
| Evidence | Job logs/batch IDs, before/after user and OAuth rows, second-run diff. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC47-02 — Anonymization includes the exact 30-day cutoff and excludes one second before it

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-47; BR-08; JOB-ID-02 §2 selection predicate `deactivated_at <= now() - 30 days` |
| Kind / priority | Boundary; P1, irreversible PII lifecycle cutoff |
| Environment | Job runner + DB + fixed clock |
| Preconditions | At T0, U1 SELF-deactivated exactly T0−30 days; U2 SELF-deactivated T0−30 days + 1 second. Both have `anonymized_at=NULL`. |
| Test data | Run JOB-ID-02 once at T0. |
| Steps | Inspect both rows and OAuth identities after job completion. |
| Expected | U1 meets inclusive `<=` cutoff and is anonymized; U2 is not yet eligible and remains unchanged. No admin-deactivated account is selected. |
| Cleanup | Restore isolated DB snapshot. |
| Evidence | Fixed-clock value, job log, before/after rows and selection query result. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC48-01 — Default language persists and rejects unsupported enum

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-48; API #14–15; validation §8; `DEC-2026-1024-dd-student-language-contracts` |
| Kind / priority | Normal, Abnormal; P2 |
| Environment | API + DB |
| Preconditions | Learner U1 has `default_language=PYTHON` and unrelated settings populated. |
| Test data | Read #14; PATCH #15 with `defaultLanguage=JAVA`; PATCH with `RUBY`; omit unrelated settings in both writes. |
| Steps | Read, update to JAVA, read, attempt RUBY, read again. |
| Expected | Initial value is PYTHON; JAVA persists on reread. RUBY is rejected `422 IDT-413`; stored JAVA and unrelated setting values remain unchanged. |
| Cleanup | Restore default language and settings. |
| Evidence | Requests/responses and DB values before/after. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC54-01 — Password-reset verification does not consume OTP before reset

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-54; API #60 and #8; `DEC-2026-1025-dd-auth-recovery-contracts`; validation §4 |
| Kind / priority | Normal, Security, Recovery; P1 |
| Environment | API + Redis + DB + session spy + clock |
| Preconditions | U1 has an unexpired reset OTP, under five failed attempts; U2 fixture OTP expired; U3 fixture OTP already consumed. |
| Test data | #60 `{email,otp}` with correct U1 code; #8 with same code and compliant new password. Negative #60/#8 calls with expired, consumed and wrong codes. |
| Steps | Verify correct code; inspect Redis/password/session; use same code for #8; make negative calls and inspect state. |
| Expected | #60 returns `200 {verified:true}` but retains the code and original expiry; does not change password or issue a session. #8 rechecks and atomically consumes valid OTP while changing password. Wrong/expired/consumed/exhausted/email-unknown errors are generic `422 IDT-107`; no password/session change. |
| Cleanup | Delete OTP keys and reset U1 password fixture. |
| Evidence | Responses, Redis TTL/counter, password-hash diff, session spy; redact OTP and secrets. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC54-02 — Concurrent resets cannot consume the same OTP twice

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-54; API #60/#8; `DEC-2026-1025-dd-auth-recovery-contracts`; validation §4 atomic consume |
| Kind / priority | Concurrency, Security; P0, one-time credential reset |
| Environment | API + DB + Redis + concurrent clients + session spy |
| Preconditions | U1 has one valid unexpired OTP, failed count below five; old password hash recorded. |
| Test data | Two simultaneous #8 requests with same email/OTP and distinct compliant new passwords. |
| Steps | Submit both requests concurrently; inspect outcomes, Redis key and password hash; try login using both proposed passwords. |
| Expected | Exactly one reset succeeds and consumes OTP; the other returns generic `422 IDT-107`. Only one proposed password authenticates; no session is issued by reset. |
| Cleanup | Delete U1 and Redis state. |
| Evidence | Both responses, Redis state, before/after hash and login checks. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC55-01 — Eligible self-deactivation restores once without issuing a session

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-55; API #2/#61; `DEC-2026-1025-dd-auth-recovery-contracts`; BR-08 |
| Kind / priority | Recovery, Security, Idempotency; P0, account recovery |
| Environment | API + DB + Redis + session spy + clock |
| Preconditions | U1 is SELF-deactivated within 30 days and not anonymized; password is verified. U2 is ADMIN-deactivated; U3 is SELF-deactivated but anonymized. |
| Test data | Login U1 with correct password to obtain opaque restore ticket in error details; POST #61 with ticket. Also obtain/replay ticket as applicable for U2/U3 negative paths. |
| Steps | 1. Obtain U1 ticket. 2. Restore. 3. Replay same ticket. 4. Inspect account, Redis and session issuance. |
| Expected | Restore returns `200 {restored:true}`; U1 becomes ACTIVE and `deactivation_source`/`deactivated_at` clear. No access/refresh session is issued; user must log in again. Ticket is one-use; replay gives generic `401 IDT-114`. Ticket is opaque, not JWT, and is not written to URL/log/local storage. |
| Cleanup | Delete ticket keys, sessions and fixture accounts. |
| Evidence | Responses, DB diff, Redis ticket state, session spy and redacted logs. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC55-02 — Admin-locked, anonymized, expired or invalid restore ticket is rejected uniformly

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-55; API #2/#6/#61; `DEC-2026-1025-dd-auth-recovery-contracts`; BR-08 |
| Kind / priority | Security, Boundary; P1 |
| Environment | API + DB + Redis + clock |
| Preconditions | Separate fixtures: ADMIN lock; SELF but anonymized; SELF beyond 30-day grace; SELF eligible with expired ticket. |
| Test data | Submit absent, expired, used, mismatched and ineligible restore ticket values to #61; exercise login for each account source. |
| Steps | Attempt restore for each fixture and inspect account/ticket/session state. |
| Expected | Invalid/used/expired/ineligible attempts have the same `401 IDT-114` response; no account-state change and no session. ADMIN lock receives no restore ticket. Anonymized account cannot be restored. |
| Cleanup | Remove fixture rows and Redis keys. |
| Evidence | Responses, DB diffs, ticket issuance/state, session spy. |
| Design review | Reviewed 2026-10-28; not executed |

### TC-F1-AC55-03 — Concurrent restoration redeems one ticket once

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-55; API #61; `DEC-2026-1025-dd-auth-recovery-contracts`; BR-08 row lock and one-use ticket |
| Kind / priority | Concurrency, Recovery, Security; P0, prevents duplicate restore side effects |
| Environment | API + DB + Redis + concurrent clients + session spy |
| Preconditions | U1 is eligible SELF-deactivated and not anonymized. One live restore ticket is bound to U1. |
| Test data | Two simultaneous POST #61 requests with the identical restore ticket. |
| Steps | Submit concurrently; inspect both responses, account row, ticket state and access/refresh session stores. |
| Expected | One request restores U1 and consumes ticket; the other receives generic `401 IDT-114` after locked eligibility/ticket recheck. U1 ends ACTIVE with deactivation fields cleared; neither request issues a login session. |
| Cleanup | Delete U1, Redis ticket and fixture sessions. |
| Evidence | Both responses, DB state, Redis ticket state and session spy. |
| Design review | Reviewed 2026-10-28; not executed |


### TC-F1-AC54-03 — Reset rollback and failed OTP cleanup preserve one-time semantics

| Field | Value |
|---|---|
| AC / DD source | AC-54; BR-04 step 3; API #8/#60; owner reset transaction decision 2026-10-27 |
| Kind / priority | Recovery/Security; P0 — prevents lost OTP or replay after commit |
| Environment | API + PostgreSQL + Redis + mail/audit spies + fault injection + controlled clock |
| Preconditions | U1 ACTIVE with existing password hash and two active refresh tokens. Issue OTP at DB timestamp T0; prior password_changed_at is strictly before T0. Both trials start from separate snapshots. |
| Test data | New compliant password ResetPass123; correct OTP; trial A fails DB transaction before commit, trial B fails Redis DEL after successful DB commit. Retry each within original ten-minute TTL. |
| Steps | A: reset while DB fault active; capture DB/key TTL, clear fault and retry same OTP. B: reset with Redis DEL fault; capture committed user/token/audit and retained key, clear fault and replay same OTP with DifferentPass123. |
| Expected | A: transaction rolls back hash/password_changed_at/session revocations/audit together; OTP and original expiry remain, retry succeeds once after dependency recovers. Assert documented rollback/state invariant; DD has no dedicated status/code for this injected dependency failure. B: first reset commits once; leftover Redis key cannot authorize a second write because locked users.password_changed_at is not earlier than OTP issued_at. Replay returns generic `422 IDT-107`; hash stays ResetPass123, no second success audit or token issuance. TTL is never extended. |
| Cleanup | Restore user/session snapshot, delete OTP and remove faults/mail/audit artifacts. |
| Evidence | DB transaction/commit ordering, HTTP responses, Redis remaining TTL/DEL failure, before/after password_changed_at/hash/token IDs and audit count. |
| Design review | Reviewed against BR-04 state/replay contract; not executed. Failure status/code in trial A is outside current DD exact-error scope. |
