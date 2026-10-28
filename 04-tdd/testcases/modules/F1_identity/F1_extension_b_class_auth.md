# F1 extension B — Teacher class, linked login and profile

- **Scope/TDD:** F1 AC-64–72; `04-tdd/modules/F1_identity.md`.
- **DD sources:** `03-dd/api/F1_identity.md` §§0.2, 0.4, 0.1; logic BR-02/03/10–12/21; validation §10–11.
- **Fixture catalog:** fixed clock `2026-10-27T10:00:00+07:00`; users I1/I2 (INSTRUCTOR), S1/S2 (STUDENT), A1 ADMIN; classes C1 owned by I1, C2 by I2; OAuth provider stub; email sink captures messages. Exact generated UUIDs/tokens are recorded per run. Backend API and DB required.
- **Execution:** All cases `Chưa thực hiện`.

| AC | Branch | Case | Review |
|---|---|---|---|
| 64 | Instructor login role and rate limit | TC-F1-AC64-01, TC-F1-AC64-02 | Reviewed 2026-10-28 |
| 65 | Class ownership/create-edit and invalid schedule rollback | TC-F1-AC65-01, TC-F1-AC65-02 | Reviewed 2026-10-28 |
| 66 | Delete class/history; event publication failure | TC-F1-AC66-01, TC-F1-AC66-02 | Reviewed 2026-10-28 |
| 67 | Invite TTL/ownership/expiry boundary | TC-F1-AC67-01, TC-F1-AC67-02 | Reviewed 2026-10-28 |
| 68 | OAuth valid link/create; invalid/replayed callback | TC-F1-AC68-01, TC-F1-AC68-02 | Reviewed 2026-10-28 |
| 69 | Logout current device only | TC-F1-AC69-01 | Reviewed 2026-10-28 |
| 70 | Email resend quota and stale code | TC-F1-AC70-01, TC-F1-AC70-02 | Reviewed 2026-10-28 |
| 71 | Self-only profile | TC-F1-AC71-01 | Reviewed 2026-10-28 |
| 72 | Class roster; per-class progress/detail scope | TC-F1-AC72-01, TC-F1-AC72-02 | Reviewed 2026-10-28 |

### TC-F1-AC64-01 — Instructor endpoint cấp phiên đúng vai trò

| Field | Value |
|---|---|
| AC / DD source | AC-64; API §1 `POST /auth/instructor/login` #49; BR-01/21 |
| Kind / priority | Security; P1 — tài khoản không đúng vai trò vào khu giảng viên |
| Environment | API + DB; rate limiter observable |
| Preconditions | I1, S1, A1 đều ACTIVE, credentials chính xác; S1/A1 có thể dùng login route thường. |
| Test data | I1=`i1@example.test`/`I1-pass-8Q!`; S1=`s1@example.test`/`S1-pass-8Q!`; A1=`a1@example.test`/`A1-pass-8Q!`. |
| Steps | I1 POST #49; S1 và A1 POST #49; S1 thử route login thường. |
| Expected | I1 nhận 200 access token expiresIn 900 và HttpOnly Secure SameSite=Strict refresh cookie. S1/A1 nhận 403 IDT-111, không có access/refresh session; đăng nhập phù hợp của S1 vẫn được. |
| Cleanup | Revoke token families và xóa users fixtures. |
| Evidence | HTTP/cookies, DB refresh_tokens, rate-limit counters. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC64-02 — Instructor login rate-limit boundary and reset

| Field | Value |
|---|---|
| AC / DD source | AC-64; API #49; BR-01; validation §2 |
| Kind / priority | Security/Boundary; P1 — credential guessing |
| Environment | API + Redis rate limiter; clock control |
| Preconditions | I1 active, known email/password; rate-limit key for email/IP empty. |
| Test data | Key `(i1@example.test, 203.0.113.41)`; five wrong attempts within 15 minutes; correct password. |
| Steps | Send five wrong-password #49 attempts; send another attempt while locked; advance beyond 15-minute window; login correctly. |
| Expected | First five fail with 401 IDT-102; attempt after threshold returns 429 IDT-103 without password check. After sliding window expires, correct credentials succeed and issue session. Instructor route uses same lockout policy as ordinary login. |
| Cleanup | Delete limiter key and revoke session. |
| Evidence | HTTP responses, Redis key/counter/TTL, refresh token row. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC65-02 — Invalid schedule is rejected without partial create/update

| Field | Value |
|---|---|
| AC / DD source | AC-65; API #20–21; validation §5 |
| Kind / priority | Boundary; P1 — invalid class schedule persistence |
| Environment | API + DB |
| Preconditions | I1 owns no class named QA-Invalid and owns C1 with valid stored name/schedule. |
| Test data | Invalid schedule `{days:[1,1],startTime:"18:00",endTime:"17:00"}`; invalid structure is IDT-V16. |
| Steps | POST #20 with valid name and invalid schedule. Then PATCH C1 with changed name and same invalid schedule. |
| Expected | Both return 422 IDT-V16; no class or first invite code is created for POST; PATCH leaves C1 name/schedule byte-for-byte semantically unchanged. |
| Cleanup | Delete any accidentally persisted class; restore C1. |
| Evidence | HTTP responses and before/after class/invite DB rows. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC65-01 — Tạo/sửa lớp chỉ trong phạm vi sở hữu

| Field | Value |
|---|---|
| AC / DD source | AC-65; API #19–21; class ownership rule §0.4.3 |
| Kind / priority | Security; P1 — sửa lớp trái quyền |
| Environment | API + DB; JWT INSTRUCTOR |
| Preconditions | I1 owns C1, I2 owns C2; schedule schema per `DEC-2026-1011-class-schedule-structured`. |
| Test data | Create `{name:"QA C3",description:"cohort",schedule:{days:[1,3],startTime:"18:00",endTime:"19:00",room:"R101",note:"QA",startDate:"2026-11-02",endDate:"2026-12-31"}}`; C1/C2 UUID. |
| Steps | I1 POST #20; kiểm lớp và invite code đầu tiên; I1 PATCH #21 C1 với tên/schedule mới; I1 PATCH C2. |
| Expected | Create thành công, class và invite code đầu tiên cùng transaction; schedule lưu đúng JSON đã gửi. C1 đổi thành công. C2 không bị lộ hoặc thay đổi bởi I1; PATCH C2 trả `404 IDT-414`, giống lớp không tồn tại. |
| Cleanup | Xóa C3, revert C1; dọn invite codes. |
| Evidence | HTTP, class/invite DB rows trước-sau. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC66-02 — Event broker failure is logged after committed deletion

| Field | Value |
|---|---|
| AC / DD source | AC-66; BR-22; API #22 and event contract |
| Kind / priority | Recovery/Degraded; P1 — dependent assignment references may remain |
| Environment | API + DB + RabbitMQ fault injection |
| Preconditions | I1 owns C1 with enrollment, invitation, assignment, submission history. Broker publish is forced to fail after DB transaction. |
| Test data | C1,S1,IC1,A1,SUB1 unique; expected event payload `{classId:C1,occurredAt:<commit time>}`. |
| Steps | DELETE #22 while broker is unavailable; query identity and problem-bank/judge records; inspect error log; restore broker and verify no scheduled retry is specified. |
| Expected | Identity deletion transaction commits: class/codes/enrollments gone, history SUB1 remains. Publish failure is logged; there is no identity outbox/retry contract, so class assignment may remain with `removed_at=NULL`. Record this documented degraded outcome as residual behavior; do not assert successful downstream removal. |
| Cleanup | Restore broker; clean orphan assignment and fixtures. |
| Evidence | API response, DB state, broker error and application log. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC66-01 — Xóa lớp không xóa lịch sử bài nộp và event một lần

| Field | Value |
|---|---|
| AC / DD source | AC-66; API #22; BR-22; API §2 domain event |
| Kind / priority | Recovery; P1 — mất lịch sử học tập hoặc duplicate event |
| Environment | API + DB + RabbitMQ publish spy + problem-bank consumer |
| Preconditions | I1 sở hữu C1; S1 active enrollment; invite code và assigned problem/submission history. |
| Test data | C1 UUID, S1 UUID, assignment A1, submission SUB1, invite code IC1. |
| Steps | I1 DELETE #22 C1; xác nhận consumer nhận `ClassDeleted`; lặp DELETE với cùng classId; kiểm class/invite/enrollment, SUB1 và assignment view ở problem-bank. |
| Expected | Lần đầu xóa hard row `classes`; FK cascade xóa invite codes/enrollments; `identity_recent_activity.scope_class_id` thành NULL; SUB1 lịch sử ở judge còn xem được. Sau commit publish đúng một `ClassDeleted {classId, occurredAt}`; problem-bank đặt `removed_at=now()` cho assignments của C1. Lần gọi lặp trả `404 IDT-414` vì ID không còn tồn tại, không phát thêm event. |
| Cleanup | Dọn class, broker/event spy và fixture history. |
| Evidence | API responses, DB snapshots, publish/problem-bank consumer logs. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC67-02 — Invite code expires at its seven-day boundary

| Field | Value |
|---|---|
| AC / DD source | AC-67; API #23–24; BR-09 |
| Kind / priority | Boundary; P1 — expired invite remains usable/listed |
| Environment | API + DB with controlled clock |
| Preconditions | I1 owns C1; no invite codes exist. |
| Test data | Generate code IC1 at `2026-10-27T10:00:00+07:00`; expected expiry `2026-11-03T10:00:00+07:00`. |
| Steps | Create code; GET list just before expiry; advance clock to expiry and query list/join; inspect persisted expiry. |
| Expected | New code expiry is exactly seven days from creation; it appears in active list before expiry. At/after expiry it is absent and join attempt returns 422 IDT-410; existing valid code is not accidentally extended. |
| Cleanup | Delete code/class fixture and reset clock. |
| Evidence | Response, DB expiry timestamp, join error. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC67-01 — Invite code mới cùng tồn tại và giới hạn chủ lớp

| Field | Value |
|---|---|
| AC / DD source | AC-67; API #23–24; logic BR-09 |
| Kind / priority | Security/Boundary; P1 — cấp mã lớp trái phép hoặc TTL sai |
| Environment | API + DB với clock cố định |
| Preconditions | I1 owns C1; I2 owns C2; C1 has active code IC1 valid at T0. |
| Test data | `T0=2026-10-27T10:00:00+07:00`; IC1 expires `2026-11-03T10:00:00+07:00`. |
| Steps | I1 POST #23 C1; GET #24 C1; thử GET/POST C1 bằng I2; query active/expired codes. |
| Expected | Mã mới có TTL 7 ngày tính từ server time; IC1 vẫn hoạt động song song; list chỉ gồm code hiệu lực của C1. I2 không đọc hoặc tạo mã cho C1. |
| Cleanup | Thu hồi/xóa codes và classes. |
| Evidence | HTTP, DB code hashes/expiry, ownership checks. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC68-02 — Invalid, unverified and replayed OAuth callbacks have no side effects

| Field | Value |
|---|---|
| AC / DD source | AC-68; API callback #6; BR-03 |
| Kind / priority | Security/Recovery; P1 — OAuth account takeover/duplicate account |
| Environment | API + DB + provider stub |
| Preconditions | Provider has cases for state mismatch, `email_verified=false`, and a code already consumed by valid exchange. |
| Test data | `badState`, unverified email `u3@example.test`, replayed `code=USED-1`; snapshot users/oauth identities/session rows. |
| Steps | Submit state mismatch; submit unverified provider callback; replay consumed code/state; compare DB and token state after each. |
| Expected | State mismatch returns 401 IDT-109; unverified email returns 422 IDT-110; replay/mismatched state is rejected. None creates duplicate users/oauth identities or issues session tokens. |
| Cleanup | Remove provider/user fixtures and revoke sessions. |
| Evidence | Responses, DB snapshots, provider exchange trace, token rows. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC68-01 — OAuth callback liên kết account hiện có hoặc tạo account mới

| Field | Value |
|---|---|
| AC / DD source | AC-68; API OAuth redirect/callback #5–6; BR-03 |
| Kind / priority | Security/Recovery; P1 — account takeover hoặc account duplication |
| Environment | API + DB + deterministic OAuth provider stub |
| Preconditions | Provider stub phát code, state, `email_verified`; U1 có password account cùng email; U2 chưa có account. |
| Test data | Provider=provider configured by deployment; U1=`u1@example.test`; U2=`u2@example.test`; callback states S1/S2. |
| Steps | U1 callback with verified provider email matching U1; U2 callback with separate verified email not yet present in identity. |
| Expected | Matching verified email links exactly one oauth_identity to U1. New verified email creates exactly one account and session for U2. Tokens appear only in HTTPS body/cookie, never URL. |
| Cleanup | Delete users/oauth identities and revoke tokens. |
| Evidence | HTTP, DB unique rows, provider exchange log, URL/cookie captures. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC70-02 — Resent code invalidates previous code without changing email early

| Field | Value |
|---|---|
| AC / DD source | AC-70; API #11–12; validation §3–4 |
| Kind / priority | Recovery/Security; P1 — stale OTP accepted or premature email change |
| Environment | API + DB + mail sink + controllable clock |
| Preconditions | U1 active, target email unused; pending email-change request already issued. |
| Test data | Current `u1@example.test`, pending `u1-new@example.test`; initial code O1; after 60s resent code O2. |
| Steps | Try O1 at #12 after O2 issue; verify account email remains current. Then submit O2. |
| Expected | O1 returns 422 IDT-416 and has no effect; email remains current. O2 returns 200 and changes email to pending address atomically; OTP is not echoed/logged. |
| Cleanup | Restore email, remove OTP and mail fixtures. |
| Evidence | HTTP, user row before/after, mail delivery time and OTP key state. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC69-01 — Logout revokes only current device session

| Field | Value |
|---|---|
| AC / DD source | AC-69; API #4; BR-02 |
| Kind / priority | Security/Recovery; P1 — token remains valid after logout or other device is revoked |
| Environment | API + DB, two browser cookie jars |
| Preconditions | U1 has refresh families/device tokens D1,D2 both active. |
| Test data | Capture D1 current refresh cookie and D2 separate cookie; access tokens valid. |
| Steps | POST logout #4 using D1; call refresh #3 with D1 and D2; repeat logout D1. |
| Expected | D1 cannot refresh; D2 remains usable per session policy. Repeated logout returns a generic notification without exposing number/details of revoked sessions. |
| Cleanup | Revoke all U1 tokens. |
| Evidence | HTTP/cookie jars, refresh_tokens rows before/after. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC72-02 — Class detail/progress aggregates only live assignments and owned current students

| Field | Value |
|---|---|
| AC / DD source | AC-72; API #28–29; BR-10/12; F2/F4 read contracts |
| Kind / priority | Security/Boundary; P1 — wrong class completion or student leakage |
| Environment | API + DB + F2/F4 ports |
| Preconditions | I1 owns C1; S1 active and S2 left; C1 has A1 still assigned, A2 removed; F4 accepted only before assignment for A1. I2 owns C2. |
| Test data | A1 assigned at T0−2 days; its only accepted result was T0−3 days (before assignment). A2 has `removed_at` set; S1 has no accepted result after assignment. |
| Steps | I1 requests C1 progress/detail; request C2 detail through I1; request S2 detail despite historical enrollment. |
| Expected | Current detail excludes S2 and includes only current assignments. A1 is not considered complete from pre-assignment acceptance; removed A2 is excluded from completion denominator. Class outside I1 scope returns 404 IDT-414; no other class/student data appears. |
| Cleanup | Remove assignment/enrollment/student/class fixtures. |
| Evidence | API responses, assignment timestamps, F4 port payloads. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC70-01 — Email change resend shares three-per-24-hour quota

| Field | Value |
|---|---|
| AC / DD source | AC-70; API #11–12; validation §3–4 and rate-limit §2 |
| Kind / priority | Boundary/Recovery; P1 — email takeover or quota bypass |
| Environment | API + DB + email sink + clock control |
| Preconditions | U1 active; target address unused; quota empty; cooldown can be advanced with controlled clock. |
| Test data | Current `u1@example.test`, target `u1-new@example.test`; six-digit codes O1/O2/O3 delivered on each successful #11; T0 fixed. |
| Steps | 1. POST #11 `{newEmail}` at T0. 2. Advance 60 seconds; resend same target at T0+60s. 3. Advance 60 seconds and make third #11 request. 4. Advance 60 seconds and attempt fourth request. |
| Expected | #11 calls (initial and each resend) share a maximum of three per 24 hours. Each valid resend counts once and after 60-second cooldown. Fourth returns `429 IDT-V05` and sends no email. Account login email remains original until a valid #12 confirmation. OTP is absent from response/log. |
| Cleanup | Delete OTP keys/emails and restore account email. |
| Evidence | Requests, mail sink timestamps, OTP Redis state, users row. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC71-01 — Profile response derives owner only from session

| Field | Value |
|---|---|
| AC / DD source | AC-71; API #9 |
| Kind / priority | Security; P1 — cross-account profile disclosure |
| Environment | API + DB; two JWT principals |
| Preconditions | U1 and U2 have distinct display names, emails and auth methods. |
| Test data | U1 email `u1@example.test`; U2 `u2@example.test`; each profile has unique marker `PROFILE-U1`/`PROFILE-U2`. |
| Steps | U1 GET #9; attempt self-crafted userId query/path/body; U2 GET #9. |
| Expected | Each gets only own profile and password status; password/secret fields absent. Caller cannot select another owner via request parameter. |
| Cleanup | Delete users and auth records. |
| Evidence | Raw HTTP responses and schema/secret scan. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC72-01 — Roster, progress and details are restricted to owned classes

| Field | Value |
|---|---|
| AC / DD source | AC-72; API #19,#26,#28–29; logic BR-10–12 |
| Kind / priority | Security; P1 — student/progress disclosure |
| Environment | API + DB + assignment/submission fixtures |
| Preconditions | I1 owns C1,C2; I2 owns C3; S1 active in C1, S2 left C1; active assignment A1 has due time and one submission. |
| Test data | C1/C2/C3, S1/S2, A1, submission marker `SUB-C1`. |
| Steps | I1 calls #26 for C1, then compare returned roster with enrollment snapshot. |
| Expected | Current roster includes S1 and excludes S2 (left enrollment). Returned rows are scoped to C1 and do not contain other class or student records. |
| Cleanup | Remove enrollment, assignment, submission and classes. |
| Evidence | API responses and DB query snapshot with timestamps. |
| Design review | Reviewed 2026-10-28 |
