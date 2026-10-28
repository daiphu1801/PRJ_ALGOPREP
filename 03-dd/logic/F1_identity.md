# DD — Logic module `identity` (F1)

## BR-21 — Đăng ký, Admin tạo tài khoản và đăng nhập đầu tiên (xác thực email, buộc đổi mật khẩu mềm)

**Thêm 2026-10-22 (đồng bộ DB vòng 2, `DEC-2026-1022-db-sync-round2` mục 1 và 4).** Số BR-21 là số tiếp theo còn trống; không đổi
số các BR cũ. `[SoT: Suy luận]` cho tên trường phản hồi và cách phân biệt hai nguồn tài khoản (phần dưới); các hành vi chính
theo DEC.

- **Tự đăng ký** (`POST /auth/register`, #1): tạo `users.status = PENDING_VERIFICATION`, `must_change_password = false`, phát OTP email
  (khoá Redis `identity:emailverify:<userId>`, #53/#54). Nhập đúng OTP (#53) → `ACTIVE`. Đây là luồng **duy nhất** dùng OTP email.
- **Admin tạo** (`POST /admin/users`, #50): tạo `status = PENDING_VERIFICATION`, `must_change_password = true`, sinh mật khẩu tạm gửi
  email. **Không** phát OTP, **không** tạo khoá `identity:emailverify:<userId>`.
- **Đăng nhập** (`POST /auth/login`, #2; `/auth/instructor/login`, #49), sau khi mật khẩu khớp và đã qua BR-01:
  1. `status = DEACTIVATED` → `403 IDT-104` (không đổi trạng thái). Nếu `deactivation_source = SELF`, còn trong 30 ngày và chưa ẩn danh, cấp vé khôi phục opaque một lần TTL 5 phút trong `error.details` cho #61; `ADMIN` lock không có vé (`DEC-2026-1025-dd-auth-recovery-contracts`).
  2. `status = PENDING_VERIFICATION` **và** `must_change_password = true` (tài khoản Admin tạo, vì tự đăng ký không bao giờ có cờ này) →
     `UPDATE users SET status = 'ACTIVE'` trong cùng giao dịch phát token. Đăng nhập thành công bằng mật khẩu tạm là bằng chứng
     người nhận truy cập được email.
  3. `status = PENDING_VERIFICATION` **và** `must_change_password = false` (tự đăng ký chưa nhập OTP): **chặn** — `403 IDT-113`
     (không phát token, không đổi `status`). Người dùng phải nhập OTP email (#53) trước, rồi đăng nhập lại.
     **Sửa 2026-10-22 (đồng bộ DB vòng 3, `DEC-2026-1022-self-register-pending-login-blocked`).** Mã `IDT-113`
     `[SoT: Suy luận — mã đề xuất]` (DEC ghi "proposed IDT-113"). Luồng Admin tạo (bước 2) không đổi. Lưu ý: bước 3 chạy sau khi
     mật khẩu khớp, nên không lộ trạng thái tài khoản cho người không biết mật khẩu.
  4. Phản hồi đăng nhập mang `user.mustChangePassword` (đọc `users.must_change_password` sau bước 2).
- **Buộc đổi mật khẩu là mềm**: máy chủ **không** chặn endpoint nào khác khi `must_change_password = true`; giao diện đọc cờ ở phản hồi
  đăng nhập (và `GET /me/profile`, #9) rồi chuyển người dùng tới màn đổi mật khẩu. Cờ về `false` ở lần đổi mật khẩu đầu tiên (BR-06,
  cùng giao dịch đặt `password_changed_at = now()`).
- **Ràng buộc kết quả**: `ACTIVE` luôn nghĩa "đã chứng minh truy cập email" (OTP, hoặc đăng nhập đầu bằng mật khẩu tạm gửi qua email).

## BR-19 — Ánh xạ `action_type` ra phần hiển thị của nhật ký

- **Cấu trúc**: một enum `AuditAction` trong `identity`, mỗi giá trị mang `category` (bốn nhóm `AUTH|PERMISSION|CONFIG|CONTENT` hiển thị theo BD ADM0403), `service` (tên dịch vụ hiển thị) và `messageKey` (khoá i18n của
  mẫu câu). `ListSystemAuditLogs` (#39) trả `action_type` cùng ba trường suy ra từ enum; giao diện dựng câu từ
  `messageKey` kèm tham số lấy từ `after_json`. **Không** thêm cột vào `system_audit_logs`
  [SoT: `02-bd/database/F1_identity.md` mục 1.11, `DEC-2026-1011-admin-system-open-questions-closed`].
- **Mã sự kiện rút gọn**: bảy ký tự đầu của `id` (UUID); trùng thì lọc theo `id` đầy đủ ở tầng truy vấn, không
  cần đảm bảo duy nhất `[SoT: Suy luận]`.
- **Danh sách khoá che**: dùng nguyên BR-16, không thêm cấu hình.
- **CSV xuất (#40; chủ dự án chốt 2026-10-28)**: cột `content` dựng câu từ `messageKey` của `actionType` và `safeParams` đã che; không serialize `before_json`/`after_json` vào CSV. Dùng `Accept-Language` `vi` hoặc `en`, thiếu thì `vi`; cùng action/params phải cho cùng câu trong một locale.
- **Hành động mới thêm 2026-10-11**: `USER_CREATED` cùng các hành động tạo/khoá/mở khoá/đổi vai trò người dùng thuộc `PERMISSION` (Ma trận quyền) theo quyết định owner 2026-10-25; `AUDIT_LOG_SETTINGS_CHANGED` thuộc `CONFIG` (Cấu hình). Không thêm nhóm `USER` thứ năm.
- **Xoá role tuỳ biến (chủ dự án chốt 2026-10-28)**: `ROLE_DELETED` thuộc `PERMISSION`, `target_type=ROLE`, `target_id` là role vừa xoá; `before_json` giữ id/tên/baseCategory/isSystem và không chứa dữ liệu nhạy cảm, `after_json` không có role. Ghi cùng giao dịch xoá.
- **Hành động mới thêm 2026-10-23 (do `interview-bank` ghi, `03-dd/logic/F6_interview-bank.md:237`)**, tất cả thuộc nhóm **Nội dung** (nhóm đóng 4 giá trị của `02-bd/screens/admin/ADM0403_system_log.md`, tab "Nội dung"; chọn nhóm theo ý nghĩa quản trị kho nội dung `[SoT: Suy luận]`):
  `INTERVIEW_QUESTION_CREATED`, `INTERVIEW_QUESTION_UPDATED`, `INTERVIEW_QUESTION_RETIRED`, `INTERVIEW_QUESTION_IMPORTED` (`target_type = INTERVIEW_QUESTION`);
  `INTERVIEW_TOPIC_CREATED`, `INTERVIEW_TOPIC_UPDATED`, `INTERVIEW_TOPIC_REORDERED`, `INTERVIEW_TOPIC_DELETED` (`INTERVIEW_TOPIC`);
  `INTERVIEW_LEVEL_CREATED`, `INTERVIEW_LEVEL_RENAMED`, `INTERVIEW_LEVEL_REORDERED`, `INTERVIEW_LEVEL_DELETED` (`INTERVIEW_LEVEL`).
- **Hành động F2 theo BR-27 của `03-dd/logic/F2_problem-bank.md`**, đều thuộc nhóm `CONTENT`: `PROBLEM_TOPIC_CREATED`, `PROBLEM_TOPIC_RENAMED`, `PROBLEM_TOPIC_REORDERED`, `PROBLEM_TOPIC_DELETED` (`PROBLEM_TOPIC`); `PROBLEM_LEVEL_CREATED`, `PROBLEM_LEVEL_RENAMED`, `PROBLEM_LEVEL_REORDERED`, `PROBLEM_LEVEL_DELETED` (`PROBLEM_LEVEL`); `PROBLEM_PUBLISHED`, `PROBLEM_UNPUBLISHED`, `PROBLEM_DIFFICULTY_CHANGED`, `PROBLEM_TOPIC_ASSIGNED`, `PROBLEM_DUPLICATED`, `PROBLEM_DELETED` (`PROBLEM`). Dùng chính `actionType` làm khoá i18n của mẫu câu; `safeParams` chỉ mang id, tên, mức và trạng thái, không mang nội dung bài/testcase.
- **Hành động F2 đổi trần sinh testcase AI**: `AI_TESTCASE_GENERATION_LIMIT_CHANGED` (`PROBLEM_BANK_SETTINGS`), nhóm `CONFIG`, `safeParams` chỉ có `maxPerProblem` cũ/mới; cùng quy tắc audit thay đổi hạn mức F5. [Owner 2026-10-25]

## BR-20 — Dọn nhật ký quá hạn

- **Điều kiện chạy**: job định kỳ hằng ngày đọc `system_settings` (`02-bd/database/F1_identity.md` mục 1.13).
- **Hành vi**: `expired_policy = DELETE` thì xoá dòng có `created_at < now() - retention_days` theo lô (ví dụ 5.000
  dòng mỗi lô). `ARCHIVE` **hoãn**: không xoá, ghi cảnh báo "chính sách nén chưa hỗ trợ". Lý do giữ thay vì xoá: tính
  bất biến của dấu vết kiểm toán (bậc 6, `.claude/rules/ponytail-criteria.md`).
- **Đồng bộ BR-15**: tự khoá và tự hạ vai trò là cảnh báo xác nhận của giao diện theo BD `ADM0201` Q6; máy chủ vẫn bảo vệ ADMIN hoạt động cuối cùng bằng `IDT-432`.

> Các khối quy tắc nghiệp vụ (BR) trải dài nhiều sự kiện/endpoint. Không lặp lại nội dung đã chốt ở
> `02-bd/security/F1_identity.md` — chỉ đặc tả thêm phần thuật toán/luồng cụ thể. Mỗi BR tham chiếu ngược
> endpoint liên quan ở `03-dd/api/F1_identity.md` mục 0.

## BR-01 — Rate-limit đăng nhập sai

- **Input**: `email`, IP nguồn của `POST /auth/login`.
- **Điều kiện**: đếm số lần trả `401 IDT-102` trong cửa sổ trượt 15 phút, khoá theo cặp `(email, IP)`
  [SoT: `07-review/bd_open_questions_with_solutions_260924.md:384`, đã chốt 5 lần/15 phút — cụ thể ở
  `03-dd/validation/F1_identity.md`].
- **Các bước**:
  1. Trước khi kiểm mật khẩu, đọc Redis key `identity:ratelimit:login:<email>:<ip>`.
  2. Nếu đếm ≥ ngưỡng → trả `429 IDT-103` ngay, không chạm DB, không tăng đếm thêm.
  3. Nếu đăng nhập sai → `INCR` key, đặt `EXPIRE` 15 phút nếu là lần đầu trong cửa sổ.
  4. Đăng nhập đúng → xoá key (`DEL`), không giữ lịch sử đếm cho lần sau.
- **Kết quả**: chặn dò mật khẩu theo cả email lẫn IP mà không khoá tài khoản vĩnh viễn.
- **Xử lý lỗi**: Redis down → **fail-open** (cho phép đăng nhập, không rate-limit) — đúng nguyên tắc F1-F4
  không phụ thuộc một thành phần phụ trợ để hoạt động; ghi log cảnh báo mức WARN.

## BR-02 — Refresh token rotation và phát hiện tái sử dụng

- **Input**: chuỗi refresh token thô từ cookie.
- **Điều kiện**: mọi refresh token chỉ dùng được đúng một lần.
- **Các bước**:
  1. Băm token thô (SHA-256), tra `refresh_tokens.token_hash`.
  2. Không tìm thấy → `401 IDT-105`.
  3. Tìm thấy nhưng `revoked_at IS NOT NULL` → đây là dấu hiệu reuse: `UPDATE refresh_tokens SET
     revoked_at = now() WHERE family_id = :familyId AND revoked_at IS NULL` (revoke toàn bộ family), ghi
     `system_audit_logs` (`action_type = SECURITY_TOKEN_REUSE_DETECTED`), trả `401 IDT-106`.
  4. Hợp lệ và chưa hết hạn → revoke token hiện tại (`revoked_at = now()`), tạo bản ghi mới cùng
     `family_id`, trả access token mới + cookie mới.
- **Kết quả**: một token bị đánh cắp và dùng lại sau khi chủ sở hữu đã refresh sẽ làm lộ toàn bộ family,
  buộc đăng nhập lại trên mọi thiết bị.
- **Xử lý lỗi**: hết hạn tự nhiên (`expires_at < now()`) → `401 IDT-105`, không coi là reuse.

## BR-03 — Liên kết OAuth an toàn

- **Input**: `state` gửi ở bước `redirect`, `code` + `state` nhận ở `callback`, claim `email_verified` từ
  provider.
- **Các bước**:
  1. `redirect`: sinh `state` ngẫu nhiên, lưu Redis `identity:oauthstate:<state>` TTL 10 phút, trả URL kèm
     `state`.
  2. `callback`: đối chiếu `state` với Redis, không khớp/hết hạn → `401 IDT-109`.
  3. Đổi `code` lấy token của provider, lấy thông tin hồ sơ.
  4. `email_verified = false` → `422 IDT-110`, không tạo `oauth_identities`, không tạo `users`.
  5. `email_verified = true`: tìm `users.email` khớp `provider_email` — có thì liên kết
     (`INSERT oauth_identities`), không có thì tạo `users` mới (`password_hash = NULL`) rồi liên kết.
  6. Phát hành access token + refresh token như đăng nhập thường.
- **Kết quả**: không thể chiếm tài khoản người khác qua provider cho email tuỳ ý.

## BR-04 — OTP quên mật khẩu

- Ngưỡng cụ thể (10 phút, 5 lần sai, cooldown 60 giây) đã chốt ở BD [SoT: `02-bd/architecture/F1_identity.md:124-126`].
- **Owner chốt 2026-10-27:** tài khoản chỉ có OAuth nhận email hướng dẫn đăng nhập qua provider ở `forgot`, **không phát OTP**. Phản hồi `forgot` vẫn là câu chung. `verify-otp` và `reset` với email này trả cùng `IDT-107` như mã không hợp lệ; không trả lỗi riêng tiết lộ loại tài khoản.
- Trong cooldown gửi lại OTP của `POST /auth/password/forgot`, vẫn trả `200` và thông báo chung như lần đầu; không gửi mail, không thay OTP hoặc TTL cũ. Chốt 2026-10-27 để không lộ tài khoản qua status.
- `POST /auth/password/verify-otp` (#60) dùng cùng phép so khớp và bộ đếm sai Redis của bước reset, trả chung `IDT-107` khi mã không hợp lệ; đúng thì **giữ mã và TTL gốc**, để #8 kiểm lại và chỉ tiêu thụ lúc đặt mật khẩu thành công. Hai request đồng thời kiểm sai dùng thao tác nguyên tử, không vượt ngưỡng 5 lần. [SoT: `DEC-2026-1025-dd-auth-recovery-contracts`]
- **Các bước `POST /auth/password/reset`**:
  1. Đọc Redis `identity:pwreset:<userId>` (tra `userId` qua `email`, nếu email không tồn tại vẫn tiếp tục
     luồng đến bước trả lỗi chung ở cuối — không rẽ nhánh sớm, tránh timing attack).
  2. Không có key hoặc đã hết hạn → tăng "coi như sai", trả `422 IDT-107`.
  3. So khớp OTP; sai → `INCR` bộ đếm sai trong cùng key, ≥ 5 lần → xoá key, buộc gửi lại. Đúng → lấy `issued_at` của OTP (mốc thời gian do PostgreSQL cấp lúc phát mã), **giữ key** trong lúc ghi DB. Trong giao dịch PostgreSQL, khoá dòng `users` và chỉ cho đổi khi `password_changed_at IS NULL OR password_changed_at < issued_at`; nếu không, trả lỗi OTP chung. Cùng giao dịch đặt `password_hash`, `password_changed_at` theo đồng hồ DB, thu hồi toàn bộ `refresh_tokens` và ghi audit. Sau commit mới xoá key Redis. Nếu DB rollback, giữ mã và TTL gốc để thử lại; nếu xoá Redis lỗi sau commit, điều kiện trên dòng `users` vẫn chặn dùng lại mã. Hai yêu cầu đồng thời được tuần tự hoá bằng khoá dòng `users`. **Owner chốt 2026-10-27**; không cần giao dịch phân tán Redis/PostgreSQL.
  4. **Sửa 2026-10-22 (đồng bộ DB vòng 3, `DEC-2026-1022-self-register-pending-login-blocked`)**: cùng giao dịch với bước 3, đặt
     `must_change_password = false`; nếu `status = PENDING_VERIFICATION` (tài khoản Admin tạo chưa đăng nhập lần nào) thì
     `UPDATE users SET status = 'ACTIVE'`. Lý do: đặt lại mật khẩu bằng OTP gửi tới email đã chứng minh quyền truy cập email.
     Tài khoản tự đăng ký còn `PENDING_VERIFICATION` chưa vào được nhánh này vì OTP quên mật khẩu chỉ dành cho tài khoản
     đã có mật khẩu; nếu có vẫn phải qua OTP email (#53), `[SoT: Suy luận]` — kiểm ở TDD.
- **Tài khoản chỉ OAuth** (`password_hash IS NULL` từ trước và **chưa từng đặt mật khẩu**): `forgot` chỉ gửi email hướng dẫn quay lại provider, không tạo khoá OTP hoặc audit `PASSWORD_RESET_REQUESTED`; không cho đặt mật khẩu qua luồng này [SoT: owner 2026-10-27; `01-rd/req/F1_identity.md` F1-17; `02-bd/security/F1_identity.md:50-52`].

## BR-05 / BR-06 — Đổi mật khẩu khi đã đăng nhập

- **BR-05 (đặt mật khẩu lần đầu, tài khoản chỉ OAuth)**: `POST /me/password` không có trường
  `currentPassword`. Điều kiện kích hoạt biến thể này: `password_hash IS NULL`. Không yêu cầu OTP — phiên
  đăng nhập hợp lệ là đủ [SoT: `07-review/bd_open_questions_with_solutions_260924.md:135`]. Sau khi đặt:
  gửi email "tài khoản vừa được đặt mật khẩu", ghi `system_audit_logs`.
- **BR-06 (đổi mật khẩu, tài khoản đã có mật khẩu)**: bắt buộc `currentPassword`, đối chiếu BCrypt trước
  khi cho đổi. Sau khi đổi thành công: `UPDATE refresh_tokens SET revoked_at = now() WHERE user_id = :id
  AND id <> :currentSessionTokenId AND revoked_at IS NULL` — giữ lại đúng phiên đang thao tác, thu hồi mọi
  phiên khác [SoT: `07-review/...:137`]. Trả về một thông báo duy nhất, không liệt kê số phiên bị thu hồi.
  Nếu `currentPassword` sai, trả `422 IDT-115` (mã riêng chốt 2026-10-27); không đổi hash/timestamp,
  không thu hồi phiên, không gửi email hoặc ghi audit thành công.

## BR-07 — Lưu cài đặt (ĐÃ ĐỔI 2026-10-22, DEC-2026-1022-db-sync-round4 ID-23: ~~vắt qua hai module~~, nay `identity` lưu cả tham số phỏng vấn)

**Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4):** `user_preferences` là bảng của `identity` và nay có `preferred_max_turns`, `preferred_hint_allowed`; hai giá trị này ghi cùng `PATCH /me/settings` (endpoint 15), **một giao dịch, một bảng**. `ai-review` không giữ bảng tuỳ chọn nào (không có trong `02-bd/database/F5_ai-review.md`) nên không còn `UpdateMyInterviewPreferences` ở `ai-review`; `interview_sessions.max_turns`/`hint_allowed` nhận giá trị lúc tạo phiên từ frontend (frontend đọc `GetMySettings` rồi truyền vào lời gọi tạo phiên). Đoạn dưới (hai lời gọi độc lập, `ai-review` lỗi không ảnh hưởng) là hành vi **trước vòng 4**, giữ làm lịch sử. Bản dựng giao diện hiện vẫn gọi hai mutation riêng (`05-coding/frontend/src/entities/user/model/types.ts:65-75`), cần dọn theo `USR0503`.

- **Input (cũ, trước vòng 4)**: một lần bấm "Lưu" ở `USR0503_settings` sinh hai lời gọi độc lập: `PATCH /me/settings`
  (`identity`) và `UpdateMyInterviewPreferences` (`ai-review`).
- **Điều kiện**: không gộp giao dịch — hai module khác schema, khác transaction boundary.
- **Các bước**: giao diện gọi song song hai endpoint, mỗi endpoint trả kết quả độc lập; phía
  `identity` không biết và không cần biết kết quả của `ai-review`.
- **Kết quả**: nhóm lưu được xoá cờ "thay đổi chưa lưu" của riêng nhóm đó; nhóm lỗi giữ nguyên cờ và báo
  lỗi tại chỗ [SoT: `07-review/bd_open_questions_with_solutions_260924.md:154`].
- **Xử lý lỗi**: `ai-review` timeout/lỗi không ảnh hưởng `PATCH /me/settings` — suy giảm êm đúng nguyên tắc
  CLAUDE.md.

## BR-08 — Tự xoá tài khoản và ẩn danh hoá sau ân hạn

- **Input**: `POST /me/account/delete` với `{ "emailConfirmation": string }` phải khớp `email` hiện tại
  [SoT: `07-review/...:150`].
- **Điều kiện**: không khớp → `422 IDT-201`, không đổi trạng thái.
- **Các bước (thời điểm xoá)**:
  1. `UPDATE users SET status = 'DEACTIVATED', deactivation_source = 'SELF', deactivated_at = now() WHERE id = :id`.
     **Sửa 2026-10-22 (đồng bộ DB, phase 5, `DEC-2026-1022-identity-schema-sync` mục 1):** thêm `deactivation_source = 'SELF'`
     [SoT: `02-bd/database/F1_identity.md:27`]. Đường Admin khoá (`ChangeUserStatus`, endpoint 37) đặt `deactivation_source = 'ADMIN'`;
     mở khoá đặt lại `NULL` cùng `deactivated_at`.
  2. Thu hồi toàn bộ `refresh_tokens` của user (`revoked_at = now()`).
  3. Ghi `system_audit_logs` (`actor_user_id = :id`, `action_type = SELF_ACCOUNT_DELETE_REQUESTED`).
  4. Mọi request tiếp theo dùng access token cũ (còn hạn ≤ 15 phút) bị chặn ở filter vì filter kiểm
     `status` mỗi request, không chỉ `exp` [SoT: `02-bd/security/F1_identity.md:64-66`].
- **Các bước (job định kỳ, sau 30 ngày ân hạn; lịch chạy, lô và xử lý lỗi ở `03-dd/jobs/F1_identity.md` mục 2)**:
  1. Quét `users WHERE status = 'DEACTIVATED' AND deactivation_source = 'SELF' AND deactivated_at <= now() -
     interval '30 days' AND anonymized_at IS NULL`. Dòng `deactivation_source = 'ADMIN'` **không bao giờ** bị job đụng
     tới (khôi phục bằng mở khoá của Admin). **Sửa 2026-10-22 (phase 5):** thêm điều kiện `deactivation_source = 'SELF'`;
     trước đó job ẩn danh cả tài khoản bị Admin khoá (mất dữ liệu định danh vĩnh viễn).
  2. Trong một giao dịch: `UPDATE users SET email = 'deleted-user-' || id || '@anon.local', display_name = 'Người dùng đã xoá',
     phone = NULL, school_or_company = NULL, current_position = NULL, target_position = NULL, anonymized_at = now()`;
     rồi `DELETE FROM oauth_identities WHERE user_id = :id` (cột `provider_email` là email thật của provider). Việc đặt
     NULL ba trường tự khai (`school_or_company`, `current_position`, `target_position`) đã được chủ dự án chốt (nêu trong
     brief phase 5; BD ghi `[SoT: Suy luận]` chờ chốt, `02-bd/database/F1_identity.md:36-37`).
  3. **Không cascade** sang bảng ở module khác [SoT: `02-bd/database/F1_identity.md:32-39`].
- **Trong 30 ngày ân hạn**: tài khoản `SELF` có đường tự huỷ #61 theo `DEC-2026-1018-account-deletion-grace-cancel` và `DEC-2026-1025-dd-auth-recovery-contracts`. Đăng nhập đúng mật khẩu hoặc OAuth callback đúng provider cấp vé opaque TTL 5 phút kèm `IDT-104`; nút trên banner gửi vé tới #61. Dưới khoá dòng, kiểm còn `SELF`, chưa quá hạn và chưa ẩn danh, rồi đặt `status = ACTIVE`, `deactivation_source = NULL`, `deactivated_at = NULL`, tiêu thụ vé; không phát token, người dùng đăng nhập lại. Tài khoản Admin khoá không nhận vé và chỉ Admin mở khoá qua #37. Vé sai/hết hạn/đã dùng trả chung `IDT-114`.
- **Sau khi đã ẩn danh hoá** (`anonymized_at IS NOT NULL`): `ChangeUserStatus` mở khoá bị từ chối cho tài khoản đó, vì email, tên và
  `oauth_identities` đã mất. **Sửa 2026-10-22 (đồng bộ DB vòng 3, `DEC-2026-1022-self-register-pending-login-blocked`).** Nút mở khoá ở
  `ADM0201` vô hiệu theo cờ `anonymized` của `ListUsers` (#35).
  **Owner chốt 2026-10-28**: request #37 cho một user đã ẩn danh trả `422 IDT-448`, không đổi user/phiên hoặc ghi audit thành công. Màn gửi nhiều lệnh gom thành kết quả một phần, giữ lỗi riêng của từng user; không phải response batch của #37.

- **Hiển thị và khôi phục ở màn Admin** (Sửa 2026-10-22, đồng bộ DB vòng 2, `DEC-2026-1022-db-sync-round2` mục 13): tài khoản `SELF`
  còn trong ân hạn **có mặt** trong danh sách Admin (#35) với nhãn "Bị khóa" như tài khoản `ADMIN` khoá (cùng `status = DEACTIVATED`).
  Admin mở khoá (#37) thì tự xoá bị huỷ: `status = ACTIVE`, `deactivation_source = NULL`, `deactivated_at = NULL`, đồng hồ 30 ngày
  mất hiệu lực. Mở khoá sau khi job đã ẩn danh hoá (`anonymized_at` khác NULL) bị chặn ở cả F1 #37 và nút giao diện,
  theo quy tắc đã chốt ở đoạn trên; dữ liệu định danh không thể khôi phục từ dòng đã ẩn danh.

## BR-09 — Tham gia lớp bằng mã mời

- **Input**: `code` từ `POST /classes/join`.
- **Các bước**:
  1. Tra `class_invite_codes WHERE code = :code AND expires_at > now()`.
  2. Không tìm thấy/hết hạn → `422 IDT-410`.
  3. Tìm thấy → ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-class-leave-details): `INSERT class_enrollments (class_id, student_id, joined_at) ... ON CONFLICT (class_id, student_id) DO UPDATE SET left_at = NULL WHERE class_enrollments.left_at IS NOT NULL`. Ba trường hợp: (a) chưa có dòng → tạo mới; (b) dòng đang `left_at IS NULL` → không đổi gì, trả `200` idempotent (không báo lỗi trùng); (c) dòng cũ đã rời (`left_at` khác rỗng) → **dùng lại dòng cũ**, đặt `left_at` = NULL, `joined_at` giữ nguyên, không sinh dòng thứ hai, trả `200`. ~~`ON CONFLICT (class_id, student_id) DO NOTHING` trả `200` idempotent nếu đã tham gia trước đó~~ (câu cũ làm học viên đã bị gỡ không vào lại được vì dòng cũ vẫn còn).
  4. Học viên quay lại xuất hiện lại trong "học viên của lớp" và được tính lại vào tổng số, tiến độ, số liệu `INS0202`.
- **Kết quả**: học viên xuất hiện trong danh sách của `INS0201_class_management`.

## BR-10 — Phân loại trạng thái học viên trong lớp (F1-27)

Ngưỡng đã chốt lại theo `DEC-2026-0921-teacher-screens-conflict-resolutions`
[SoT: `07-review/bd_open_questions_with_solutions_260924.md:187`], **thay thế** đề xuất ban đầu ở
`02-bd/architecture/F1_identity.md:148` (3 mức) bằng bộ 5 nhãn, xét theo đúng thứ tự liệt kê (nhãn đầu
tiên khớp điều kiện thắng):

1. `INSUFFICIENT_DATA` ("Chưa đủ dữ liệu") — chưa có lượt nộp nào trong phạm vi bài đã giao của lớp.
2. `ABSENT` ("Vắng bài") — không nộp bài nào trong 7 ngày gần nhất (cửa sổ trượt, không phải theo tuần lịch
   [SoT: `07-review/...:188`]).
3. `NEEDS_SUPPORT` ("Cần hỗ trợ") — tỉ lệ Accepted giảm liên tục qua ≥ 2 mốc tuần gần nhất.
4. `WATCH` ("Theo dõi") — tỉ lệ hoàn thành bài giao < 50%.
5. `ON_TRACK` ("Đang tốt") — còn lại.
- **Nguồn dữ liệu**: bài đã giao đọc từ `problem-bank.ListClassAssignments`; tiến độ đọc từ
  `judge-orchestration.GetClassStudentSubmissionMetrics` theo phạm vi lớp
  (đổi tên 2026-09-27 bởi `DEC-2026-0927-submission-metrics-two-ports`; cổng này nay tách hẳn khỏi cổng
  phía Người học) (không dùng `user_submission_stats`
  toàn cục — read model đó không lọc theo bài đã giao của một lớp cụ thể)
  [SoT: `07-review/bd_open_questions_with_solutions_260924.md:184`].

## BR-11 — Gỡ học viên khỏi lớp (ĐÃ ĐỔI 2026-10-14 (DEC-2026-1014-keep-learning-history-on-leave): chỉ đánh dấu đã rời lớp, giữ lịch sử; trước đây "kèm cascade")

- **Input**: ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-class-leave-details): `POST /classes/{classId}/students/{studentId}/leave` ~~`DELETE /classes/{classId}/students/{studentId}`~~, gọi bởi giảng viên sở hữu lớp.
- **Điều kiện**: kiểm `classes.instructor_id = :callerId` trước khi ghi.
- **Các bước**:
  1. ĐÃ ĐỔI 2026-10-14 (DEC-2026-1014-keep-learning-history-on-leave): `UPDATE class_enrollments SET left_at = now() WHERE class_id = :classId AND student_id = :studentId AND left_at IS NULL` (transaction
     của `identity`, chỉ ghi đúng bảng này — [SoT: `02-bd/database/F1_identity.md` mục 1.10]). ~~`DELETE FROM class_enrollments ...`~~
  2. Sau khi commit, publish `StudentRemovedFromClass { classId, studentId, occurredAt }` lên RabbitMQ.
  3. ĐÃ ĐỔI 2026-10-14 (DEC-2026-1014-keep-learning-history-on-leave): `judge-orchestration` **không** xoá/ẩn lượt nộp khi nhận sự kiện; lịch sử làm bài được giữ. ~~`judge-orchestration` tự lắng nghe và xoá/ẩn dữ liệu lượt nộp~~
- **Kết quả**: `identity` không bao giờ viết trực tiếp vào schema `judge`.
- **Xử lý lỗi**: publish thất bại (RabbitMQ down) → dùng outbox pattern tương tự `judge-orchestration` nếu
  cần đảm bảo at-least-once; ở quy mô đồ án, chấp nhận log lỗi + retry thủ công qua endpoint idempotent nếu
  message mất — không xây outbox riêng cho `identity` chỉ vì một sự kiện này (ponytail: chấp nhận rủi ro
  mất một message hiếm gặp, nâng cấp lên outbox khi có sự kiện phát ra thứ hai cần đảm bảo tương tự).
  **Chốt 2026-10-27:** khi gọi lại endpoint sau commit, không đổi `left_at`, trả `200` và phát lại
  `StudentRemovedFromClass` với `occurredAt = left_at` gốc. Lần gọi lại có thể tạo bản sao event nếu
  lần phát trước đã thành công; consumer xử lý idempotent theo `(classId, studentId, occurredAt)`.

## BR-22 — Xoá lớp (hard delete) và sự kiện `ClassDeleted`

**Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4):** BR mới (số tiếp theo sau BR-21), không đổi số BR cũ. Nguồn: F1-24, `DEC-2026-1022-db-sync-round4` mục 3, `02-bd/database/F1_identity.md` mục 1.8.

- **Input**: `DELETE /classes/{classId}` (endpoint 22) bởi giảng viên sở hữu lớp.
- **Điều kiện**: `classes.instructor_id = :callerId`, sai chủ lớp thì từ chối như các thao tác lớp khác (BR-11).
- **Các bước**:
  1. Trong một giao dịch: `DELETE FROM classes WHERE id = :classId`. Cơ sở dữ liệu tự xử lý: `class_invite_codes` và `class_enrollments` xoá theo (`ON DELETE CASCADE`); `identity_recent_activity.scope_class_id` về `NULL` (`ON DELETE SET NULL`). Lịch sử làm bài của học viên không bị đụng (nằm ở `judge`).
  2. Sau khi commit, publish `ClassDeleted { classId, occurredAt }` lên RabbitMQ, cùng kiểu với `StudentRemovedFromClass` ở BR-11.
  3. `problem-bank` nhận sự kiện và đặt `class_assignments.removed_at = now()` cho các dòng của lớp (xoá mềm, idempotent). `identity` không ghi vào schema `problem-bank`.
- **Xử lý lỗi**: publish thất bại thì giữ cách của BR-11 (log lỗi, chấp nhận mất hiếm gặp, chưa có outbox riêng cho `identity`). Hệ quả nếu mất: dòng `class_assignments` của lớp đã xoá còn `removed_at = NULL` (tham chiếu treo mềm, không vỡ khoá vì không có FK chéo schema). Đây là sự kiện phát ra thứ hai, tức đã đạt điều kiện "nâng lên outbox" mà BR-11 ghi; đề xuất cân nhắc outbox cho `identity` `[SoT: Suy luận]`.
- **Kết quả**: không để lại dòng nào của `identity` trỏ vào lớp đã xoá.

## BR-12 — Tỉ lệ hoàn thành của lớp (`completionPct`, tính khi đọc)

**Sửa 2026-10-22 (đồng bộ DB, phase 5, `DEC-2026-1022-identity-schema-sync` mục 4):** ~~bảng read model
`class_completion_stats(class_id PK, completion_percent, updated_at)` cập nhật mỗi lần nhận `SubmissionGraded`~~ **đã bỏ**;
`completionPct` tính trực tiếp lúc đọc, không bảng, không job, không event riêng
[SoT: `02-bd/database/F1_identity.md:122`; `02-bd/screens/teacher/INS0101_overview.md:77` (V0.13)].

- **Input**: `classId` (endpoint 19, 26, 29); nguồn gồm `class_enrollments` (`left_at IS NULL`), danh sách bài giao đọc từ
  `problem-bank.ListClassAssignments`, và F4 `GetClassStudentSubmissionMetrics.assignmentResults[]` cho từng lần giao còn hiệu lực. Không dùng `user_problem_best_score` toàn cục: lượt Accepted trước khi giao không hoàn thành bài trong lớp. [Owner 2026-10-25]
- **Công thức**: `completionPct` của lớp = trung bình cộng, trên các học viên còn trong lớp, của "Hoàn thành" từng học
  viên; "Hoàn thành" của một học viên = số bài còn giao cho lớp có `assignmentResults[].accepted=true` sau mốc giao / tổng số bài còn giao cho lớp
  × 100 (F1-28, `01-rd/req/F1_identity.md:148-150`; BD `INS0201` Khu vực C NO 6, `02-bd/screens/teacher/INS0201_class_management.md:380`).
  Lớp chưa giao bài hoặc chưa có học viên thì trả `null` (giao diện hiện `-`, `INS0205_class_detail.md:370`)
  `[SoT: Suy luận — tránh chia cho 0]`. Học viên đã rời (`left_at` khác NULL) không tính (`02-bd/database/F1_identity.md:128-130`).
- **Lưu ý khác biệt với bản cũ**: công thức cũ của BR-12 ("số bài khác nhau được **bất kỳ** học viên nào Accepted / số bài giao")
  khác công thức trung bình theo học viên của RD/BD màn. Phase 5 theo RD/BD (P2 cao hơn P3); xem câu hỏi Q1 trong báo cáo.
- **Dùng chung** cho `INS0101_overview` (thẻ/danh sách "Lớp của tôi") và `INS0203_class_progress`.
- **Nâng cấp khi chậm** `[SoT: Suy luận]`: nếu truy vấn chậm ở quy mô lớn thì cache 15 phút phía `identity` (cùng cách đã chốt cho
  sparkline, `01-rd/req/F1_identity.md:155-157`) trước khi nghĩ tới bảng mới.

## BR-13 — Đổi ma trận phân quyền có hiệu lực ngay

- **Input**: `PATCH /rbac/matrix/{roleId}/{functionId}/{actionId}`, mỗi lần bấm ô gọi ngay (không gộp vào
  nút "Lưu thay đổi") [SoT: `07-review/bd_open_questions_with_solutions_260924.md:278`].
- **Các bước**:
  1. `UPSERT permissions (role_id, function_id, action_id, granted, updated_at, updated_by)`.
  2. Ghi `system_audit_logs` (`before_json`/`after_json` là giá trị `granted` trước/sau — snapshot tự do,
     không chuẩn hoá schema theo action type [SoT: `07-review/...:366`]).
  3. Trong cùng transaction, `UPDATE roles SET permission_version = permission_version + 1 WHERE id = :roleId`; khoá dòng role đảm bảo version tăng đơn điệu khi hai PATCH tranh chấp. Commit permission, version và audit trước khi trả thành công.
  4. Sau commit, publish Redis pub/sub `identity:permcache:invalidate` với `{roleId,permissionVersion}` [SoT: `07-review/...:383` cho kênh; version theo `DEC-2026-1028-qa-f1-permission-immediate`]. Subscriber xoá cache Caffeine cục bộ; message chậm/mất không làm sai quyết định quyền.
- **Guard quyền Lớp 2**: trên mỗi request có gác `Function:Action`, đọc `roles.permission_version` bằng truy vấn mới tới PostgreSQL **primary** trước khi tin cache Caffeine/Redis. Cache chỉ dùng nếu giữ cùng version; khác version thì đọc ma trận mới từ primary, cập nhật cache và quyết định. Không đọc được version hoặc ma trận thì từ chối trước use case (fail closed), không dùng cache cũ. Bước đọc xảy ra sau khi request bắt đầu và trước mọi side effect của use case. Với request bắt đầu sau response PATCH thành công, version đã commit nên không thể dùng quyền cũ, kể cả Redis chưa giao invalidate.
- **Từ chối thiếu quyền**: request đã xác thực nhưng thiếu ô `Function:Action` theo version hiện tại trả `403 IDT-445`; use case được gác không chạy và không ghi side effect (chủ dự án chốt 2026-10-28).
- **Phạm vi seed** (sửa 2026-10-23, `DEC-2026-1023-db-actor-review-fixes`): ma trận gồm **11 function** (thêm `LANGUAGE_CONFIG`, F4-11), seed 3 role × 11 function × 4 action = **132** dòng `permissions` [SoT: `02-bd/database/F1_identity.md:66`]; mặc định `LANGUAGE_CONFIG` chỉ `ADMIN` có quyền.
- **Ràng buộc bất biến (BR-14)**: không cho xoá role khi `is_system = true`, hoặc còn ≥ 1 user gán
  (`role_id` FK) [SoT: `02-bd/architecture/F1_identity.md:78-79`]. Role tuỳ biến: áp dụng cùng điều kiện "còn
  người gán thì chặn xoá" — không tự chuyển user sang role mặc định, vì không có "role mặc định" nào được
  định nghĩa cho một `base_category` khi có nhiều role tuỳ biến cùng `base_category` đó
  [SoT: `07-review/bd_open_questions_with_solutions_260924.md:279`].
- **Phản hồi và audit xoá role (chủ dự án chốt 2026-10-28)**: `is_system=true` trả `422 IDT-421`; còn user gán trả `422 IDT-422`; cả hai không đổi role/quyền/user và không ghi audit thành công. Role tuỳ biến không có người gán xoá thành công trả `204` không body; xoá role và các permission rows cùng giao dịch với đúng một audit `ROLE_DELETED` ghi actor/target/before. Nếu ghi audit lỗi thì rollback cả thao tác và trả `503 IDT-446`.
- **Ràng buộc bất biến bổ sung**: không cho gỡ ô `PERMISSION_MATRIX:UPDATE = false` nếu đó là ô cuối cùng
  còn `granted = true` trong toàn bộ role có `base_category = ADMIN`
  [SoT: `07-review/bd_open_questions_with_solutions_260924.md:281`] — trả `422 IDT-420` nếu vi phạm.

## BR-15 — Bảo vệ tài khoản ADMIN (`ChangeUserRole` / `ChangeUserStatus`)

- **Điều kiện**: không cho thực hiện nếu sau thao tác, số tài khoản `base_category = ADMIN` và `status = ACTIVE` còn lại bằng 0 → `422 IDT-432`. Tự khoá hoặc tự hạ vai trò khi vẫn còn ADMIN khác được phép; giao diện cảnh báo rõ hậu quả và xác nhận trước khi gửi theo BD `ADM0201` Q6/Sheet 8. Máy chủ không trả `IDT-430`/`IDT-431` cho riêng hai tình huống đó. [SoT: BD ADM0201 Q6, Sheet 9 NO 6–8]
- **Xác nhận tự ảnh hưởng (chốt 2026-10-27)**: nếu actor tự khoá hoặc tự hạ vai trò ra khỏi ADMIN, máy chủ yêu cầu `acknowledgeSelfImpact=true` trong request #37/#36 sau khi đã bảo vệ bất biến ADMIN cuối. Thiếu hoặc `false` trả `400 IDT-V21`, không ghi user/phiên/audit/email. Cờ này không thay thế kiểm tra `422 IDT-432` cho ADMIN cuối. [SoT: quyết định chủ dự án 2026-10-27; API F1 §0.6].
- **Khoá tranh chấp**: hai quản trị viên cùng đổi trạng thái/vai trò của hai ADMIN khác nhau trong cùng một
  giây có thể cùng vượt qua kiểm tra "còn ≥ 1 ADMIN hoạt động" tại thời điểm đọc rồi cùng ghi — dùng
  `SELECT u.id FROM users u JOIN roles r ON r.id = u.role_id WHERE r.base_category = 'ADMIN' AND u.status = 'ACTIVE'
  FOR UPDATE OF u` rồi đếm số dòng trả về (khoá dòng admin đang đếm; `FOR UPDATE` không dùng được với `COUNT(*)` gộp) trong
  cùng transaction với câu `UPDATE`, đảm bảo đếm và ghi nguyên tử. **Sửa 2026-10-22 (phase 5):** `base_category` nằm ở
  `roles`, không có trên `users`, nên bắt buộc `JOIN roles` [SoT: `02-bd/database/F1_identity.md:17, 52`].
- **Audit**: ghi `system_audit_logs` cho mọi lần gọi thành công, kể cả khi ADMIN thao tác trên ADMIN khác
  [SoT: `02-bd/security/F1_identity.md:55-57`].

## BR-16 — Che dữ liệu nhạy cảm khi xuất/hiển thị audit log

- **Danh sách khoá che** trong `before_json`/`after_json` khi hiển thị hoặc xuất CSV: `password_hash`,
  `token_hash`, bất kỳ khoá nào khớp regex `(?i)password|secret|token` — thay giá trị bằng `"***"`, giữ
  nguyên cấu trúc JSON còn lại.
- **Áp dụng**: tại tầng serializer chung dùng cho cả `GET /admin/audit-logs` lẫn
  `GET /admin/audit-logs/export` — một chỗ, không lặp lại logic che ở hai endpoint
  [SoT: `07-review/bd_open_questions_with_solutions_260924.md:329`].
- **Phạm vi trả ra**: #39 chỉ trả `safeParams` đã che, #40 dựng `content` từ chính các tham số đó theo BR-19; không endpoint nào đưa raw `before_json`/`after_json` vào response hoặc CSV. Nếu một tham số được lấy từ khoá nhạy cảm, giá trị xuất là `***`. [Owner chốt định dạng CSV 2026-10-28]

## BR-17 — Số liệu tổng hợp xuyên module cho `admin_overview`

- **Quyết định**: đọc đồng bộ qua cổng ra (`ProblemBankStatsPort`, `JudgeStatsPort`) tại từng endpoint F1 #43, #67–72 của mười khối `ADM0101`, **không** xây read model tổng hợp toàn hệ thống mới trong `identity`
  [SoT: `07-review/bd_open_questions_with_solutions_260924.md:259`].
- **Lý do chọn đồng bộ thay vì read model**: dashboard admin xem không thường xuyên (không phải màn tải
  hàng nghìn lượt/phút như `problem_list`), nên chi phí một lời gọi đồng bộ mỗi lần tải màn rẻ hơn chi phí
  duy trì một read model tổng hợp phải nghe hàng chục loại event từ hai module khác chỉ để phục vụ một
  màn. Cache phía `identity` 60 giây theo `Cache-Control` nội bộ (Caffeine) để tránh gọi lặp khi admin
  refresh liên tục.
- **Suy giảm êm**: cổng `problem-bank`/`judge-orchestration` lỗi → endpoint của khối cần cổng đó trả `503` chung để view hiện lỗi và Thử lại đúng khối; các endpoint khối khác vẫn trả bình thường.

## BR-18 — ~~Job quét học viên chưa nộp bài tuần~~ **ĐÃ BỎ** (feed hoạt động chỉ giữ sự kiện tức thời)

**Sửa 2026-10-22 (đồng bộ DB, phase 5):** BR-18 cũ mô tả job hằng ngày `INSERT identity_recent_activity` với
`activity_type = 'CLASS_HAS_ABSENT_STUDENTS'`. RD đã bỏ loại sự kiện suy ra từ quét định kỳ ("vắng 9 ngày", "cần hỗ trợ")
từ 2026-10-14 (`01-rd/req/F1_identity.md:224-226`, `DEC-2026-1014-instructor-batch1-counting-rules`), và BD không có giá trị này
trong tập `activity_type` (`02-bd/database/F1_identity.md:195`). Giữ nguyên số hiệu BR-18 để các trích dẫn cũ không lệch; **không có job nào
ghi `identity_recent_activity` theo lịch**. Nhãn `ABSENT` vẫn tồn tại nhưng chỉ là nhãn phân loại tính khi đọc ở BR-10
(`GET /classes/{classId}/students`), không sinh dòng feed.

- **Nguồn hợp lệ của `identity_recent_activity`** (năm giá trị `activity_type`): `SUBMISSION_ACCEPTED`,
  `SOLUTION_REVIEW_COMPLETED`, `MOCK_INTERVIEW_FINISHED`, `CLASS_PROBLEM_PUBLISHED`, `STUDENT_STREAK_ACHIEVED`; tất cả ghi bởi
  event, danh sách ở `03-dd/api/F1_identity.md` mục 2. Không có giá trị cho sự kiện "tham gia lớp" (xem câu hỏi Q2 trong báo cáo).
- **Vị trí đặt job** (sửa 2026-10-23): ~~`03-dd/jobs/F1_identity.md` chưa tạo~~ đã tạo, gom job ẩn danh hoá (BR-08), dọn `identity_recent_activity` quá 90 ngày, dọn `refresh_tokens` và runner khởi tạo ADMIN (BR-23): xem `03-dd/jobs/F1_identity.md`.

## BR-23 — Khởi tạo tài khoản ADMIN đầu tiên lúc khởi động ứng dụng

**Thêm 2026-10-23** (`DEC-2026-1023-db-actor-review-fixes`; nguồn BD: `02-bd/database/F1_identity.md` mục 4, đoạn "Tài khoản `ADMIN` đầu tiên").

- **Input**: hai biến môi trường `ALGOPREP_BOOTSTRAP_ADMIN_EMAIL`, `ALGOPREP_BOOTSTRAP_ADMIN_PASSWORD` (tên `[SoT: Suy luận]` ở BD, DD chốt tên này).
- **Điều kiện kích hoạt**: không có dòng `users` nào có `role.base_category = 'ADMIN'`.
- **Các bước** (một giao dịch, chạy bởi runner khởi động của `algoprep-identity`):
  1. Có ít nhất một ADMIN: không làm gì, thoát (idempotent; đổi biến môi trường sau đó không đổi mật khẩu).
  2. Chưa có ADMIN và đủ hai biến: kiểm email theo quy tắc chung, băm mật khẩu BCrypt, `INSERT users` với role hệ thống `ADMIN`, `status = 'PENDING_VERIFICATION'`, `must_change_password = true`.
  3. Chưa có ADMIN và thiếu một trong hai biến: ném lỗi cấu hình, **ứng dụng dừng khởi động** kèm thông báo nêu rõ tên biến thiếu.
- **Không ghi `system_audit_logs`** (`actor_user_id` NOT NULL, không có người thao tác).
- **Kết quả**: lần đăng nhập đầu bằng mật khẩu này chuyển `ACTIVE`, giao diện buộc đổi mật khẩu (cùng luồng Admin tạo, BR-21).
- **Bảo mật**: không ghi mật khẩu ra log; không đặt mật khẩu trong migration SQL.

## BR-24 — Hợp đồng `AuditLogPort` cho module khác

**Thêm 2026-10-23** (`DEC-2026-1023-db-actor-review-decisions`; nguồn BD: `02-bd/architecture/F1_identity.md` mục 3.3).

- **Chữ ký**: `record(actorUserId, actionType, targetType, targetId, beforeJson, afterJson)`, do `identity` (`algoprep-identity`) cung cấp, ghi một dòng `identity.system_audit_logs`.
- **Giao dịch**: gọi trong cùng JVM và **cùng giao dịch DB** với thay đổi nghiệp vụ của module gọi (`Propagation.REQUIRED`, không `REQUIRES_NEW`): rollback nghiệp vụ thì dòng nhật ký cũng mất; lỗi ghi nhật ký làm hỏng cả giao dịch (không nuốt lỗi).
- **Người gọi**: `problem-bank` (quản lý `topics`, `problem_levels` và các hành động ghi trên bài ở màn quản lý theo F2 BR-27), `interview-bank` (quản lý `topics`, `levels`), `judge-orchestration` (ghi `language_configs`, `grading_defaults`, `sandbox_settings`, `generator_sandbox_settings`, `cluster_settings`).
- **Ràng buộc**: `actorUserId` bắt buộc (không null); module khác không viết trực tiếp vào schema `identity`; `beforeJson`/`afterJson` là snapshot tự do, che khoá nhạy cảm ở tầng đọc (BR-16). Phân loại hiển thị theo BR-19; `actionType` mới của các module gọi phải bổ sung vào bảng ánh xạ BR-19.

## Tham chiếu

`03-dd/api/F1_identity.md` (endpoint tương ứng từng BR), `02-bd/architecture/F1_identity.md`,
`02-bd/database/F1_identity.md`, `02-bd/security/F1_identity.md`,
`07-review/bd_open_questions_with_solutions_260924.md`,
`DEC-2026-0921-class-completion-owned-by-identity`, `DEC-2026-0921-teacher-screens-conflict-resolutions`,
`DEC-2026-0922-users-and-admin-conflict-resolutions`.
