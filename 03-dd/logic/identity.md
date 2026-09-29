# DD — Logic module `identity` (F1)

> Các khối quy tắc nghiệp vụ (BR) trải dài nhiều sự kiện/endpoint. Không lặp lại nội dung đã chốt ở
> `02-bd/security/identity.md` — chỉ đặc tả thêm phần thuật toán/luồng cụ thể. Mỗi BR tham chiếu ngược
> endpoint liên quan ở `03-dd/api/identity.md` mục 0.

## BR-01 — Rate-limit đăng nhập sai

- **Input**: `email`, IP nguồn của `POST /auth/login`.
- **Điều kiện**: đếm số lần trả `401 IDT-102` trong cửa sổ trượt 15 phút, khoá theo cặp `(email, IP)`
  [SoT: `07-review/bd_open_questions_with_solutions_260924.md:384`, đã chốt 5 lần/15 phút — cụ thể ở
  `03-dd/validation/identity.md`].
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

- Ngưỡng cụ thể (10 phút, 5 lần sai, cooldown 60 giây) đã chốt ở BD [SoT: `02-bd/architecture/identity.md:96-98`].
- **Các bước `POST /auth/password/reset`**:
  1. Đọc Redis `identity:pwreset:<userId>` (tra `userId` qua `email`, nếu email không tồn tại vẫn tiếp tục
     luồng đến bước trả lỗi chung ở cuối — không rẽ nhánh sớm, tránh timing attack).
  2. Không có key hoặc đã hết hạn → tăng "coi như sai", trả `422 IDT-107`.
  3. So khớp OTP; sai → `INCR` bộ đếm sai trong cùng key, ≥ 5 lần → xoá key, buộc gửi lại; đúng → đặt
     `password_hash` mới, xoá key, thu hồi toàn bộ `refresh_tokens` của user (BR-06 dùng lại logic này).
- **Tài khoản chỉ OAuth** (`password_hash IS NULL` từ trước và **chưa từng đặt mật khẩu**): bước 1 vẫn gửi
  OTP bình thường ở `forgot` (không tiết lộ), nhưng `reset` trả `422 IDT-108` kèm email hướng dẫn quay lại
  provider, không đặt mật khẩu [SoT: `02-bd/security/identity.md:50-52`].

## BR-05 / BR-06 — Đổi mật khẩu khi đã đăng nhập

- **BR-05 (đặt mật khẩu lần đầu, tài khoản chỉ OAuth)**: `POST /me/password` không có trường
  `currentPassword`. Điều kiện kích hoạt biến thể này: `password_hash IS NULL`. Không yêu cầu OTP — phiên
  đăng nhập hợp lệ là đủ [SoT: `07-review/bd_open_questions_with_solutions_260924.md:135`]. Sau khi đặt:
  gửi email "tài khoản vừa được đặt mật khẩu", ghi `system_audit_logs`.
- **BR-06 (đổi mật khẩu, tài khoản đã có mật khẩu)**: bắt buộc `currentPassword`, đối chiếu BCrypt trước
  khi cho đổi. Sau khi đổi thành công: `UPDATE refresh_tokens SET revoked_at = now() WHERE user_id = :id
  AND id <> :currentSessionTokenId AND revoked_at IS NULL` — giữ lại đúng phiên đang thao tác, thu hồi mọi
  phiên khác [SoT: `07-review/...:137`]. Trả về một thông báo duy nhất, không liệt kê số phiên bị thu hồi.

## BR-07 — Lưu cài đặt vắt qua hai module

- **Input**: một lần bấm "Lưu" ở `USR0503_settings` sinh hai lời gọi độc lập: `PATCH /me/settings`
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
  1. `UPDATE users SET status = 'DEACTIVATED', deactivated_at = now() WHERE id = :id`.
  2. Thu hồi toàn bộ `refresh_tokens` của user (`revoked_at = now()`).
  3. Ghi `system_audit_logs` (`actor_user_id = :id`, `action_type = SELF_ACCOUNT_DELETE_REQUESTED`).
  4. Mọi request tiếp theo dùng access token cũ (còn hạn ≤ 15 phút) bị chặn ở filter vì filter kiểm
     `status` mỗi request, không chỉ `exp` [SoT: `02-bd/security/identity.md:64-66`].
- **Các bước (job định kỳ, sau 30 ngày ân hạn — xem `03-dd/jobs/` nếu tạo, hiện tại đặc tả tại đây vì chưa
  đủ lớn để tách file jobs riêng)**:
  1. Quét `users WHERE status = 'DEACTIVATED' AND deactivated_at <= now() - interval '30 days' AND
     anonymized_at IS NULL`.
  2. `UPDATE users SET email = 'deleted-user-' || id || '@anon.local', display_name = 'Người dùng đã xoá',
     anonymized_at = now()`.
  3. **Không cascade** sang bảng ở module khác [SoT: `02-bd/database/identity.md:27-30`].
- **Trong 30 ngày ân hạn**: không có đường tự huỷ; muốn khôi phục phải nhờ ADMIN mở khoá qua `ChangeUserStatus`
  (endpoint 37) [SoT: `07-review/bd_open_questions_with_solutions_260924.md:151`]. UI phải ghi rõ "liên hệ
  quản trị viên", không hứa tự khôi phục được.

## BR-09 — Tham gia lớp bằng mã mời

- **Input**: `code` từ `POST /classes/join`.
- **Các bước**:
  1. Tra `class_invite_codes WHERE code = :code AND expires_at > now()`.
  2. Không tìm thấy/hết hạn → `422 IDT-410`.
  3. Tìm thấy → `INSERT class_enrollments (class_id, student_id, joined_at)`, `ON CONFLICT (class_id,
     student_id) DO NOTHING` trả `200` idempotent nếu đã tham gia trước đó (không báo lỗi trùng).
- **Kết quả**: học viên xuất hiện trong danh sách của `INS0201_class_management`.

## BR-10 — Phân loại trạng thái học viên trong lớp (F1-27)

Ngưỡng đã chốt lại theo `DEC-2026-0921-teacher-screens-conflict-resolutions`
[SoT: `07-review/bd_open_questions_with_solutions_260924.md:187`], **thay thế** đề xuất ban đầu ở
`02-bd/architecture/identity.md:120-123` (3 mức) bằng bộ 5 nhãn, xét theo đúng thứ tự liệt kê (nhãn đầu
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

## BR-11 — Gỡ học viên khỏi lớp kèm cascade

- **Input**: `DELETE /classes/{classId}/students/{studentId}`, gọi bởi giảng viên sở hữu lớp.
- **Điều kiện**: kiểm `classes.instructor_id = :callerId` trước khi xoá.
- **Các bước**:
  1. `DELETE FROM class_enrollments WHERE class_id = :classId AND student_id = :studentId` (transaction
     của `identity`, chỉ xoá đúng bảng này — [SoT: `02-bd/database/identity.md:99-102`]).
  2. Sau khi commit, publish `StudentRemovedFromClass { classId, studentId, occurredAt }` lên RabbitMQ.
  3. `judge-orchestration` tự lắng nghe và xoá/ẩn dữ liệu lượt nộp thuộc phạm vi lớp đó của học viên (nằm
     ngoài phạm vi DD `identity` — thuộc DD `judge-orchestration`).
- **Kết quả**: `identity` không bao giờ viết trực tiếp vào schema `judge`.
- **Xử lý lỗi**: publish thất bại (RabbitMQ down) → dùng outbox pattern tương tự `judge-orchestration` nếu
  cần đảm bảo at-least-once; ở quy mô đồ án, chấp nhận log lỗi + retry thủ công qua endpoint idempotent nếu
  message mất — không xây outbox riêng cho `identity` chỉ vì một sự kiện này (ponytail: chấp nhận rủi ro
  mất một message hiếm gặp, nâng cấp lên outbox khi có sự kiện phát ra thứ hai cần đảm bảo tương tự).

## BR-12 — `class_completion_stats` (read model)

- **Input**: sự kiện `SubmissionGraded`/`SubmissionAccepted` (đã có ở BR nhận event, mục `03-dd/api`),
  cộng danh sách bài đã giao từ `problem-bank.ListClassAssignments` (đọc theo lịch, không qua event vì
  danh sách bài giao đổi không thường xuyên).
- **Bảng mới cần thêm vào `02-bd/database/identity.md`** (đề xuất, chưa migrate):
  `class_completion_stats(class_id PK, completion_percent NUMERIC(5,2), updated_at TIMESTAMPTZ)`
  [SoT: `07-review/bd_open_questions_with_solutions_260924.md:170`].
- **Công thức**: `completion_percent = COUNT(DISTINCT problem_id đã ACCEPTED bởi bất kỳ học viên nào của
  lớp) / COUNT(bài đã giao cho lớp) * 100`, cập nhật lại toàn bộ giá trị của một `class_id` mỗi khi nhận
  event `SubmissionGraded` có `classId` khớp (không tính incremental để tránh lệch khi bài giao thay đổi).
- **Dùng chung** cho `INS0101_overview` (thẻ "Hoàn thành TB") và `INS0203_class_progress`
  [SoT: `07-review/...:170`].

## BR-13 — Đổi ma trận phân quyền có hiệu lực ngay

- **Input**: `PATCH /rbac/matrix/{roleId}/{functionId}/{actionId}`, mỗi lần bấm ô gọi ngay (không gộp vào
  nút "Lưu thay đổi") [SoT: `07-review/bd_open_questions_with_solutions_260924.md:278`].
- **Các bước**:
  1. `UPSERT permissions (role_id, function_id, action_id, granted, updated_at, updated_by)`.
  2. Ghi `system_audit_logs` (`before_json`/`after_json` là giá trị `granted` trước/sau — snapshot tự do,
     không chuẩn hoá schema theo action type [SoT: `07-review/...:366`]).
  3. Publish message nội bộ qua Redis pub/sub channel `identity:permcache:invalidate` với payload
     `{ roleId }` [SoT: `07-review/...:383`].
  4. Mọi instance backend subscribe channel này, xoá cache Caffeine cục bộ theo `roleId` nhận được.
- **Kết quả**: request tiếp theo dùng `roleId` đó sẽ cache-miss, tra lại Postgres, nạp giá trị mới.
- **Ràng buộc bất biến (BR-14)**: không cho xoá role khi `is_system = true`, hoặc còn ≥ 1 user gán
  (`role_id` FK) [SoT: `02-bd/architecture/identity.md:78-79`]. Role tuỳ biến: áp dụng cùng điều kiện "còn
  người gán thì chặn xoá" — không tự chuyển user sang role mặc định, vì không có "role mặc định" nào được
  định nghĩa cho một `base_category` khi có nhiều role tuỳ biến cùng `base_category` đó
  [SoT: `07-review/bd_open_questions_with_solutions_260924.md:279`].
- **Ràng buộc bất biến bổ sung**: không cho gỡ ô `PERMISSION_MATRIX:UPDATE = false` nếu đó là ô cuối cùng
  còn `granted = true` trong toàn bộ role có `base_category = ADMIN`
  [SoT: `07-review/bd_open_questions_with_solutions_260924.md:281`] — trả `422 IDT-420` nếu vi phạm.

## BR-15 — Bảo vệ tài khoản ADMIN (`ChangeUserRole` / `ChangeUserStatus`)

- **Điều kiện** (áp dụng thứ tự, dừng ở điều kiện đầu tiên vi phạm)
  [SoT: `07-review/bd_open_questions_with_solutions_260924.md:272`]:
  1. Không cho một ADMIN tự khoá chính mình (`targetUserId = callerUserId` và hành động là khoá) → `422
     IDT-430`.
  2. Không cho một ADMIN tự hạ vai trò của chính mình → `422 IDT-431`.
  3. Không cho thực hiện nếu sau thao tác, số tài khoản `base_category = ADMIN` và `status = ACTIVE` còn
     lại bằng 0 → `422 IDT-432`.
- **Khoá tranh chấp**: hai quản trị viên cùng đổi trạng thái/vai trò của hai ADMIN khác nhau trong cùng một
  giây có thể cùng vượt qua kiểm tra "còn ≥ 1 ADMIN hoạt động" tại thời điểm đọc rồi cùng ghi — dùng
  `SELECT COUNT(*) FROM users WHERE base_category = 'ADMIN' AND status = 'ACTIVE' FOR UPDATE` (khoá dòng
  admin đang đếm) trong cùng transaction với câu `UPDATE`, đảm bảo đếm và ghi nguyên tử.
- **Audit**: ghi `system_audit_logs` cho mọi lần gọi thành công, kể cả khi ADMIN thao tác trên ADMIN khác
  [SoT: `02-bd/security/identity.md:55-57`].

## BR-16 — Che dữ liệu nhạy cảm khi xuất/hiển thị audit log

- **Danh sách khoá che** trong `before_json`/`after_json` khi hiển thị hoặc xuất CSV: `password_hash`,
  `token_hash`, bất kỳ khoá nào khớp regex `(?i)password|secret|token` — thay giá trị bằng `"***"`, giữ
  nguyên cấu trúc JSON còn lại.
- **Áp dụng**: tại tầng serializer chung dùng cho cả `GET /admin/audit-logs` lẫn
  `GET /admin/audit-logs/export` — một chỗ, không lặp lại logic che ở hai endpoint
  [SoT: `07-review/bd_open_questions_with_solutions_260924.md:329`].

## BR-17 — Số liệu tổng hợp xuyên module cho `admin_overview`

- **Quyết định**: đọc đồng bộ qua cổng ra (`ProblemBankStatsPort`, `JudgeStatsPort`) tại thời điểm gọi
  `GET /admin/dashboard/cross-module`, **không** xây read model tổng hợp toàn hệ thống mới trong `identity`
  [SoT: `07-review/bd_open_questions_with_solutions_260924.md:259`].
- **Lý do chọn đồng bộ thay vì read model**: dashboard admin xem không thường xuyên (không phải màn tải
  hàng nghìn lượt/phút như `problem_list`), nên chi phí một lời gọi đồng bộ mỗi lần tải màn rẻ hơn chi phí
  duy trì một read model tổng hợp phải nghe hàng chục loại event từ hai module khác chỉ để phục vụ một
  màn. Cache phía `identity` 60 giây theo `Cache-Control` nội bộ (Caffeine) để tránh gọi lặp khi admin
  refresh liên tục.
- **Suy giảm êm**: `problem-bank`/`judge-orchestration` lỗi → phần số liệu tương ứng trả `null` kèm cờ
  `unavailable: true`, không làm hỏng toàn bộ response.

## BR-18 — Job quét học viên chưa nộp bài tuần (nguồn của `activity_type` dạng quét định kỳ)

- **Input**: chạy định kỳ mỗi ngày (đề xuất 06:00 giờ Việt Nam — múi giờ hệ thống, xem
  `07-review/bd_open_questions_with_solutions_260924.md:118` về việc chốt múi giờ VN cho mọi phép cắt
  ngày).
- **Các bước**:
  1. Với mỗi `class_id`, đếm học viên có `ABSENT` theo đúng công thức BR-10 (7 ngày trượt).
  2. Nếu số học viên `ABSENT` của lớp > 0 và khác kết quả lần quét trước (tránh lặp lại đúng một cảnh báo
     mỗi ngày cho cùng một trạng thái không đổi), `INSERT identity_recent_activity
     (actor_user_id = instructor_id, scope_class_id = class_id, activity_type =
     'CLASS_HAS_ABSENT_STUDENTS', ref_type = 'CLASS', ref_id = class_id, occurred_at = now())`.
- **Vị trí đặt job**: `03-dd/jobs/identity.md` không được tạo riêng ở đợt DD này (chưa đủ job khác để tách
  file có ý nghĩa) — nếu số job của `identity` tăng lên (ví dụ thêm job dọn `identity_recent_activity` quá
  90 ngày, job ẩn danh hoá của BR-08), tách `03-dd/jobs/identity.md` lúc đó.

## Tham chiếu

`03-dd/api/identity.md` (endpoint tương ứng từng BR), `02-bd/architecture/identity.md`,
`02-bd/database/identity.md`, `02-bd/security/identity.md`,
`07-review/bd_open_questions_with_solutions_260924.md`,
`DEC-2026-0921-class-completion-owned-by-identity`, `DEC-2026-0921-teacher-screens-conflict-resolutions`,
`DEC-2026-0922-users-and-admin-conflict-resolutions`.
