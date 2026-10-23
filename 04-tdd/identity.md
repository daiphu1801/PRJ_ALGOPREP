# TDD — Tiêu chí nghiệm thu module `identity` (F1)

> Mỗi `AC-nn` map về đúng một endpoint (`03-dd/api/identity.md` mục 0) hoặc một khối BR
> (`03-dd/logic/identity.md`). Viết cùng lúc với DD theo `tdd-mode.md`.

## Xác thực và phiên

### AC-01 — Đăng ký thành công
- **Tiền điều kiện**: email chưa tồn tại trong `users`.
- **Hành động**: gọi `POST /auth/register` với email hợp lệ, mật khẩu đạt chuẩn (mục `03-dd/validation/identity.md` §1), `displayName` không rỗng.
- **Kết quả mong đợi**: `201`, tạo dòng `users` mới với `role_id` mặc định `STUDENT`, `password_hash` là BCrypt của mật khẩu nhập vào (không phải plaintext).

### AC-02 — Đăng ký trùng email
- **Tiền điều kiện**: email đã tồn tại.
- **Hành động**: gọi `POST /auth/register` với cùng email.
- **Kết quả mong đợi**: `409 IDT-101`, không tạo dòng mới.

### AC-03 — Đăng nhập đúng
- **Tiền điều kiện**: tài khoản `status = ACTIVE`, mật khẩu đúng.
- **Hành động**: `POST /auth/login`.
- **Kết quả mong đợi**: `200`, trả `accessToken` TTL 900 giây, cookie `refresh_token` có cờ `HttpOnly; Secure; SameSite=Strict`.

### AC-04 — Đăng nhập sai mật khẩu 5 lần trong 15 phút (BR-01)
- **Tiền điều kiện**: chưa có lần sai nào trong cửa sổ hiện tại.
- **Hành động**: gọi `POST /auth/login` sai mật khẩu liên tiếp 5 lần cùng `(email, IP)`, lần thứ 6 gọi lại dù mật khẩu đúng.
- **Kết quả mong đợi**: 5 lần đầu trả `401 IDT-102`; lần thứ 6 trả `429 IDT-103` dù mật khẩu đúng.

### AC-05 — Refresh token hợp lệ, rotation (BR-02)
- **Tiền điều kiện**: refresh token còn hạn, chưa bị revoke.
- **Hành động**: `POST /auth/refresh` kèm header `X-Requested-With`.
- **Kết quả mong đợi**: `200`, token cũ có `revoked_at` được set, token mới cùng `family_id` được tạo.

### AC-06 — Refresh token bị tái sử dụng sau khi đã rotate (BR-02)
- **Tiền điều kiện**: đã refresh một lần (token A đã bị revoke, token B đang hiệu lực).
- **Hành động**: gọi lại `POST /auth/refresh` với token A (đã revoke).
- **Kết quả mong đợi**: `401 IDT-106`, mọi token cùng `family_id` (bao gồm token B) chuyển `revoked_at`, ghi `system_audit_logs` loại `SECURITY_TOKEN_REUSE_DETECTED`.

### AC-07 — Refresh thiếu header chống CSRF
- **Hành động**: gọi `POST /auth/refresh` không kèm `X-Requested-With`.
- **Kết quả mong đợi**: `400 IDT-V11`.

### AC-08 — Quên mật khẩu với email không tồn tại (chống dò email)
- **Hành động**: `POST /auth/password/forgot` với email không tồn tại trong hệ thống.
- **Kết quả mong đợi**: `200` với cùng thông điệp như email tồn tại; không có OTP nào được sinh ra ở phía sau (kiểm qua log/side-effect, không kiểm qua response).

### AC-09 — Đặt lại mật khẩu với OTP đúng
- **Tiền điều kiện**: đã có OTP hợp lệ trong Redis, chưa hết hạn, chưa sai quá 5 lần.
- **Hành động**: `POST /auth/password/reset` với OTP đúng, mật khẩu mới hợp lệ.
- **Kết quả mong đợi**: `200`, `password_hash` cập nhật, toàn bộ `refresh_tokens` của user bị revoke.
- **Mở rộng (Sửa 2026-10-22 (đồng bộ DB vòng 3, DEC-2026-1022-self-register-pending-login-blocked), BR-04 bước 4)**: lặp lại với (a) tài khoản Admin tạo đang `status = PENDING_VERIFICATION`, `must_change_password = true`; (b) tài khoản `ACTIVE` có `must_change_password = true`. Kết quả: ở cả hai, `must_change_password = false`; ở (a) `status` chuyển `ACTIVE`; ở (b) `status` giữ `ACTIVE`. Đăng nhập ngay sau đó bằng mật khẩu mới thành công, phản hồi `user.mustChangePassword = false`.

### AC-10 — Đặt lại mật khẩu cho tài khoản chỉ OAuth
- **Tiền điều kiện**: `users.password_hash IS NULL`, chưa từng đặt mật khẩu.
- **Hành động**: `POST /auth/password/reset` với OTP đúng.
- **Kết quả mong đợi**: `422 IDT-108`, không tạo `password_hash`, email hướng dẫn quay lại provider được gửi.

### AC-11 — Liên kết OAuth khi email chưa xác thực ở provider (BR-03)
- **Tiền điều kiện**: provider trả `email_verified = false`.
- **Hành động**: hoàn tất `GET /auth/oauth/{provider}/callback`.
- **Kết quả mong đợi**: `422 IDT-110`, không tạo `oauth_identities`, không tạo `users` mới.

## Hồ sơ và cài đặt

### AC-12 — Cập nhật hồ sơ bỏ qua trường quyền
- **Hành động**: `PATCH /me/profile` gửi kèm `roleId`, `status` cùng các trường hợp lệ khác.
- **Kết quả mong đợi**: `200`, `role_id` và `status` của user **không đổi**, các trường hợp lệ khác được cập nhật.

### AC-13 — Đổi email yêu cầu OTP xác nhận
- **Hành động**: gọi `RequestMyEmailChange` rồi `ConfirmMyEmailChange` với OTP đúng.
- **Kết quả mong đợi**: `email` chỉ đổi sau bước confirm; giữa hai bước, `GetMyProfile` vẫn trả email cũ.

### AC-14 — Đổi mật khẩu thu hồi phiên khác, giữ phiên hiện tại (BR-06)
- **Tiền điều kiện**: user đang có ≥ 2 refresh token hiệu lực (2 thiết bị).
- **Hành động**: `POST /me/password` (biến thể có `currentPassword`) từ thiết bị A.
- **Kết quả mong đợi**: `200`; refresh token của thiết bị B bị revoke; refresh token của thiết bị A (phiên đang thao tác) vẫn hiệu lực.

### AC-15 — Đặt mật khẩu lần đầu cho tài khoản OAuth, không cần OTP (BR-05)
- **Tiền điều kiện**: `password_hash IS NULL`, phiên đăng nhập hợp lệ.
- **Hành động**: `POST /me/password` không kèm `currentPassword`.
- **Kết quả mong đợi**: `200`, `password_hash` được set, email thông báo "vừa đặt mật khẩu" được gửi.

### AC-16 — Xoá tài khoản với xác nhận email sai
- **Hành động**: `POST /me/account/delete` với `emailConfirmation` không khớp email hiện tại.
- **Kết quả mong đợi**: `422 IDT-201`, `status` không đổi.

### AC-17 — Xoá tài khoản thành công, token cũ vô hiệu ngay (BR-08)
- **Tiền điều kiện**: xác nhận email đúng.
- **Hành động**: `POST /me/account/delete` thành công, sau đó gọi bất kỳ endpoint nào bằng access token cũ (còn hạn).
- **Kết quả mong đợi**: request xoá trả `200`; dòng `users` có `status = DEACTIVATED` và `deactivation_source = 'SELF'` (sửa 2026-10-22, phase 5); request tiếp theo bằng access token cũ trả `401` vì filter kiểm `status = DEACTIVATED`.

### AC-18 — Ẩn danh hoá sau 30 ngày ân hạn, chỉ tài khoản tự xoá (BR-08, job định kỳ)
- **Tiền điều kiện** (sửa 2026-10-22, phase 5): tài khoản có `status = DEACTIVATED`, `deactivation_source = 'SELF'`, `deactivated_at <= now() - 30 ngày`, `anonymized_at IS NULL`; có `phone`, `school_or_company`, `current_position`, `target_position` khác NULL và ít nhất một dòng `oauth_identities`.
- **Hành động**: chạy job ẩn danh hoá.
- **Kết quả mong đợi**: `email`/`display_name` đổi thành giá trị vô danh, `phone`, `school_or_company`, `current_position`, `target_position` đều `NULL`, `anonymized_at` khác NULL; mọi dòng `oauth_identities` của user bị xoá; `id` giữ nguyên, các bảng `judge`/`ai` liên quan không bị cascade xoá.

### AC-18b — Job ẩn danh hoá không đụng tài khoản bị Admin khoá (BR-08, thêm 2026-10-22, phase 5)
- **Tiền điều kiện**: tài khoản `status = DEACTIVATED`, `deactivation_source = 'ADMIN'`, `deactivated_at` đã quá 30 ngày.
- **Hành động**: chạy job ẩn danh hoá.
- **Kết quả mong đợi**: dòng `users` giữ nguyên (`email`, `display_name`, `anonymized_at IS NULL`), `oauth_identities` còn nguyên; sau khi Admin mở khoá thì `status = ACTIVE`, `deactivation_source` và `deactivated_at` về `NULL`.

## Lớp học

### AC-19 — Tham gia lớp bằng mã hết hạn (BR-09)
- **Hành động**: `POST /classes/join` với `code` đã quá `expires_at`.
- **Kết quả mong đợi**: `422 IDT-410`, không tạo `class_enrollments`.

### AC-20 — Tham gia lớp trùng lặp (idempotent, BR-09)
- **Tiền điều kiện**: học viên đã ở trong lớp.
- **Hành động**: `POST /classes/join` lại với mã hợp lệ của cùng lớp.
- **Kết quả mong đợi**: `200`, không tạo dòng `class_enrollments` thứ hai, `left_at` vẫn NULL.

### AC-20b — Vào lại lớp bằng mã mời sau khi đã bị gỡ (BR-09, ĐÃ CHỐT 2026-10-14 DEC-2026-1014-class-leave-details)
- **Tiền điều kiện**: học viên đã bị gỡ khỏi lớp (dòng `class_enrollments` có `left_at` khác rỗng); có mã mời còn hạn của lớp đó.
- **Hành động**: `POST /classes/join` với mã hợp lệ.
- **Kết quả mong đợi**: `200`; dòng `class_enrollments` cũ được dùng lại, `left_at` = NULL, `joined_at` giữ nguyên; tổng số dòng của cặp (lớp, học viên) vẫn là 1; học viên xuất hiện lại trong danh sách học viên của lớp và được tính lại vào tổng số, tiến độ, số lượt nộp và tỉ lệ AC của `INS0202`; lịch sử làm bài cũ còn nguyên.

### AC-21 — Học viên rời lớp (gỡ): phát sự kiện, không đụng schema khác (BR-11)
- **Hành động**: ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-class-leave-details): `POST /classes/{classId}/students/{studentId}/leave` ~~`DELETE /classes/{classId}/students/{studentId}`~~ bởi đúng giảng viên sở hữu lớp.
- **Kết quả mong đợi**: `200`, ĐÃ ĐỔI 2026-10-14 (DEC-2026-1014-keep-learning-history-on-leave): dòng `class_enrollments` còn nguyên và có `left_at` khác rỗng, lượt nộp của học viên không bị xoá, ~~dòng `class_enrollments` bị xoá~~; message `StudentRemovedFromClass` được publish; `identity` không có câu lệnh nào ghi vào schema `judge`.

### AC-22 — Gỡ học viên bởi giảng viên không sở hữu lớp
- **Hành động**: giảng viên B gọi `POST .../leave` (ĐÃ CHỐT 2026-10-14, DEC-2026-1014-class-leave-details; ~~`DELETE`~~) trên lớp của giảng viên A.
- **Kết quả mong đợi**: `403`.

### AC-23 — Phân loại "Vắng bài" theo cửa sổ trượt 7 ngày (BR-10)
- **Tiền điều kiện**: học viên có lượt nộp cuối cách đúng 8 ngày trước thời điểm kiểm tra.
- **Hành động**: `GET /classes/{classId}/students`.
- **Kết quả mong đợi**: học viên đó mang nhãn `ABSENT`.

### AC-24 — Thứ tự ưu tiên nhãn phân loại (BR-10)
- **Tiền điều kiện**: một học viên vừa `ABSENT` (không nộp 7 ngày) vừa có hoàn thành < 50%.
- **Hành động**: `GET /classes/{classId}/students`.
- **Kết quả mong đợi**: nhãn trả về là `ABSENT` (xét trước `WATCH` theo đúng thứ tự BR-10), không phải `WATCH`.

## RBAC và quản trị

### AC-25 — Đổi ô ma trận có hiệu lực ngay, không cần khởi động lại (BR-13)
- **Hành động**: `PATCH /rbac/matrix/{roleId}/{functionId}/{actionId}` tắt một quyền đang bật, sau đó một instance backend khác (đã cache quyền này trước đó) xử lý request cần quyền vừa tắt.
- **Kết quả mong đợi**: request ở instance khác bị từ chối ngay sau khi cache invalidate lan tới (không cần restart), nhờ Redis pub/sub channel `identity:permcache:invalidate`.

### AC-26 — Không xoá được role hệ thống (BR-14)
- **Hành động**: `DELETE /rbac/roles/{roleId}` với `roleId` của `STUDENT`/`INSTRUCTOR`/`ADMIN`.
- **Kết quả mong đợi**: `422`, `is_system = true` là điều kiện chặn.

### AC-27 — Không xoá được role tuỳ biến còn người gán (BR-14)
- **Tiền điều kiện**: role tuỳ biến có ≥ 1 user gán.
- **Hành động**: `DELETE /rbac/roles/{roleId}`.
- **Kết quả mong đợi**: `422`, role không bị xoá.

### AC-28 — Chặn gỡ ô `PERMISSION_MATRIX:UPDATE` cuối cùng của mọi role ADMIN (BR-13)
- **Tiền điều kiện**: chỉ còn đúng một role `base_category = ADMIN` có `PERMISSION_MATRIX:UPDATE = true`.
- **Hành động**: `PATCH` tắt đúng ô đó.
- **Kết quả mong đợi**: `422 IDT-420`, ô không đổi.

### AC-29 — Chặn ADMIN tự khoá chính mình (BR-15)
- **Hành động**: ADMIN gọi `ChangeUserStatus` trên chính `userId` của mình để khoá.
- **Kết quả mong đợi**: `422 IDT-430`.

### AC-30 — Chặn xoá ADMIN hoạt động cuối cùng (BR-15)
- **Tiền điều kiện**: chỉ còn một tài khoản `base_category = ADMIN, status = ACTIVE`.
- **Hành động**: một ADMIN khác (hoặc chính nó qua một phiên khác) gọi khoá/hạ vai trò tài khoản đó.
- **Kết quả mong đợi**: `422 IDT-432`, trạng thái không đổi.

### AC-31 — Đọc nhật ký hệ thống không bị chặn bởi ma trận quyền
- **Tiền điều kiện**: tài khoản `base_category = ADMIN`, ô `SYSTEM_AUDIT_LOG:READ` trong ma trận đang `false` (nếu có).
- **Hành động**: `GET /admin/audit-logs`.
- **Kết quả mong đợi**: `200` — quyền đọc không đi qua bảng `permissions` [SoT: `02-bd/security/identity.md:38-41`].

### AC-32 — Che dữ liệu nhạy cảm khi xuất audit log (BR-16)
- **Tiền điều kiện**: có dòng `system_audit_logs` với `after_json` chứa khoá `password_hash`.
- **Hành động**: `GET /admin/audit-logs/export?format=csv`.
- **Kết quả mong đợi**: giá trị tương ứng trong tệp xuất là `"***"`, không phải giá trị thật.

## Bổ sung đồng bộ schema (2026-10-22, phase 5, `DEC-2026-1022-identity-schema-sync`)

Các tiêu chí dưới đây mô tả hành vi mới hoặc đổi sau khi đồng bộ `02-bd/database/identity.md`. **AC-35 → AC-38 và AC-40 phụ thuộc endpoint
và mã lỗi đề xuất** (`[SoT: Suy luận]`, `03-dd/api/identity.md` mục 1) nên chỉ có hiệu lực sau khi chủ dự án chốt F1-33.

### AC-33 — Tỉ lệ hoàn thành của lớp tính khi đọc, không đọc bảng riêng (BR-12)
- **Tiền điều kiện**: lớp giao 4 bài, có 2 học viên còn trong lớp (A đã Accepted 2 bài giao, B đã Accepted 4 bài giao) và 1 học viên đã rời (`left_at` khác NULL, đã Accepted 4 bài giao).
- **Hành động**: `GET /classes/{classId}/progress`.
- **Kết quả mong đợi**: `completionPct` = 75 (trung bình của 50 và 100; học viên đã rời không tính); không có bảng `class_completion_stats` trong schema; nộp thêm một bài Accepted rồi gọi lại thì số mới có ngay, không chờ job hay event. Lớp chưa giao bài trả `null`.

### AC-34 — Feed hoạt động chỉ có sự kiện tức thời, không có "vắng bài" (BR-18 đã bỏ)
- **Tiền điều kiện**: lớp có học viên ở nhãn `ABSENT` (BR-10).
- **Hành động**: chờ qua mốc giờ job quét cũ (06:00) rồi `GET /instructor/dashboard/recent-activity?classId=`.
- **Kết quả mong đợi**: không có dòng `activity_type = 'CLASS_HAS_ABSENT_STUDENTS'`; mọi dòng trong `identity_recent_activity` có `activity_type` thuộc đúng năm giá trị `SUBMISSION_ACCEPTED`, `SOLUTION_REVIEW_COMPLETED`, `MOCK_INTERVIEW_FINISHED`, `CLASS_PROBLEM_PUBLISHED`, `STUDENT_STREAK_ACHIEVED`; nhãn `ABSENT` vẫn hiện ở `GET /classes/{classId}/students`.

### AC-35 — Tài khoản do Admin tạo: ACTIVE ở lần đăng nhập đầu, buộc đổi mật khẩu mềm (api mục 0.6.1, #50; BR-21)
- **Tiền điều kiện**: Admin tạo tài khoản bằng `POST /admin/users`.
- **Hành động**: (1) ngay sau tạo, đọc `users`; (2) người dùng mới đăng nhập bằng mật khẩu tạm; (3) với access token vừa nhận, gọi một endpoint không liên quan tới mật khẩu (ví dụ `GET /me/profile`); (4) gọi `POST /me/password` (có `currentPassword` = mật khẩu tạm, mật khẩu mới đạt chuẩn).
- **Kết quả mong đợi**: (1) `status = PENDING_VERIFICATION`, `must_change_password = true`, không có khoá Redis `identity:emailverify:<userId>`; (2) `200`, `users.status` chuyển `ACTIVE` trong cùng lần đăng nhập, phản hồi có `user.mustChangePassword = true`; (3) `200`, máy chủ **không** chặn endpoint khác (buộc đổi là mềm, chỉ giao diện chuyển hướng); (4) sau lần đổi đầu tiên `must_change_password = false` và `password_changed_at` khác NULL trong cùng giao dịch; tài khoản tự đăng ký (`POST /auth/register`) và OAuth luôn có `must_change_password = false`. *Sửa 2026-10-22 (đồng bộ DB vòng 2, DEC-2026-1022-db-sync-round2): trước đây AC này giả định đăng nhập được khi còn `PENDING_VERIFICATION` (chờ chốt Q3) và chưa nêu việc không chặn endpoint khác.*

### AC-36 — Xác thực email bằng OTP chuyển tài khoản tự đăng ký từ `PENDING_VERIFICATION` sang `ACTIVE` (đề xuất #53)
- **Tiền điều kiện**: tài khoản **tự đăng ký** (`POST /auth/register`) đang `PENDING_VERIFICATION` (Sửa 2026-10-22 (đồng bộ DB vòng 2, DEC-2026-1022-db-sync-round2): OTP chỉ áp cho tự đăng ký), Redis có `identity:emailverify:<userId>` còn hạn (TTL 10 phút), chưa sai quá 5 lần.
- **Hành động**: `POST /auth/email/verify` với OTP đúng.
- **Kết quả mong đợi**: `200`, `users.status = ACTIVE`, khoá Redis bị xoá; gọi lại với cùng OTP không còn tác dụng.

### AC-37 — OTP xác thực email sai, hết hạn hoặc quá 5 lần (đề xuất #53)
- **Tiền điều kiện**: tài khoản tự đăng ký `PENDING_VERIFICATION` có OTP đang hiệu lực.
- **Hành động**: gọi `POST /auth/email/verify` với OTP sai 5 lần liên tiếp, rồi lần thứ 6 với OTP đúng.
- **Kết quả mong đợi**: 5 lần đầu `422 IDT-112`; khoá Redis bị xoá sau lần sai thứ 5 nên lần thứ 6 cũng `422 IDT-112` dù OTP đúng; `status` vẫn `PENDING_VERIFICATION`; OTP hết hạn (quá 10 phút) cùng trả `422 IDT-112`, không phân biệt lý do.

### AC-38 — Gửi lại OTP xác thực email có cooldown 60 giây, không dò email (đề xuất #54)
- **Hành động**: gọi `POST /auth/email/verify/resend` hai lần trong 60 giây với email của tài khoản tự đăng ký `PENDING_VERIFICATION`; gọi thêm một lần với email không tồn tại và một lần với email của tài khoản Admin tạo chưa đăng nhập.
- **Kết quả mong đợi**: lần đầu `200` và phát OTP mới; lần hai `429 IDT-V12`; email không tồn tại và email tài khoản Admin tạo đều trả `200` cùng câu chung, không tạo khoá Redis (Sửa 2026-10-22 (đồng bộ DB vòng 2, DEC-2026-1022-db-sync-round2)).

### AC-39 — `SubmissionGraded` cập nhật ngôn ngữ nộp nhiều nhất và đếm lượt nộp theo ngày (mục 2 của api)
- **Tiền điều kiện**: user chưa có dòng `user_submission_stats`.
- **Hành động**: phát ba event `SubmissionGraded` với `language` lần lượt `PYTHON`, `JAVA`, `PYTHON` trong cùng một ngày UTC+7 (`submittedAt` cùng ngày UTC+7); lượt thứ nhất `verdict = ACCEPTED` kèm `firstAttemptForProblem = true`, hai lượt sau `firstAttemptForProblem = false`. Sửa 2026-10-22 (đồng bộ DB vòng 2, DEC-2026-1022-db-sync-round2): payload nay có `submittedAt` và `firstAttemptForProblem`.
- **Kết quả mong đợi**: `language_counts = {"PYTHON": 2, "JAVA": 1}`, `top_language = 'PYTHON'`; `user_daily_submission_stats` có đúng một dòng `(user_id, day)` với `submission_count = 3`; `GET /me/progress/summary` trả ngôn ngữ dùng nhiều nhất là `PYTHON`; `first_try_accepted_count = 1` (chỉ tăng khi `firstAttemptForProblem = true` và `verdict = ACCEPTED`); một event có `submittedAt = 2026-03-01T17:30:00Z` (00:30 ngày 2026-03-02 theo UTC+7) ghi vào `day = 2026-03-02`, không phải `2026-03-01`; phát lại cùng `eventId` không làm tăng bộ đếm `[SoT: Suy luận — tính idempotent do DD đề xuất]`.

### AC-40 — Lưới hoạt động 12 tháng đọc từ `identity` (đề xuất #55)
- **Tiền điều kiện**: user có dòng `user_daily_submission_stats` ở vài ngày trong 12 tháng gần nhất và một dòng cũ hơn 12 tháng; user khác có dữ liệu riêng.
- **Hành động**: `GET /me/activity-calendar` bằng token của user thứ nhất.
- **Kết quả mong đợi**: `200` với `days` **luôn đủ 364 phần tử** (api mục 0.8.1), sắp xếp tăng dần, mỗi phần tử chỉ có `day` và `submissionCount` (**không** có `level`; ngày không có dòng là `submissionCount = 0`); ngày có dòng trong cửa sổ UTC+7 mang đúng số đếm; không có dòng cũ hơn cửa sổ; không có dữ liệu của user khác; thiếu token thì `401`. Không có lời gọi nào sang cổng ra `judge` để dựng lưới. `[SoT: Suy luận — endpoint đề xuất, Sửa 2026-10-22 (đồng bộ DB vòng 2, DEC-2026-1022-db-sync-round2) mục 3]` Sửa 2026-10-22 (đồng bộ DB vòng 3, DEC-2026-1022-self-register-pending-login-blocked): bản trước ghi "chỉ gồm các ngày có dòng, tối đa 366", mâu thuẫn với 364 phần tử của DD; việc quy 5 mức do màn tính, không kiểm ở AC này.

### AC-41 — Tài khoản tự đăng ký chưa nhập OTP không đăng nhập được (BR-21 bước 3, `IDT-113`)
- **Tiền điều kiện** (Sửa 2026-10-22 (đồng bộ DB vòng 3, DEC-2026-1022-self-register-pending-login-blocked)): tài khoản **tự đăng ký** (`POST /auth/register`) đang `status = PENDING_VERIFICATION`, `must_change_password = false`; mật khẩu đúng; chưa nhập OTP email. Một tài khoản Admin tạo cùng trạng thái nhưng `must_change_password = true`.
- **Hành động**: (1) `POST /auth/login` và `POST /auth/instructor/login` bằng email và mật khẩu đúng của tài khoản tự đăng ký; (2) nhập OTP đúng qua `POST /auth/email/verify` rồi đăng nhập lại; (3) đăng nhập bằng mật khẩu tạm của tài khoản Admin tạo.
- **Kết quả mong đợi**: (1) `403 IDT-113`, không có `accessToken`, không có `Set-Cookie` refresh token, `status` vẫn `PENDING_VERIFICATION`, không thêm dòng `refresh_tokens`; (2) `200`, `status = ACTIVE`, đăng nhập lần hai thành công; (3) `200` và `status` chuyển `ACTIVE` (luồng Admin tạo không đổi, xem AC-35). Mật khẩu **sai** vẫn trả `401 IDT-102`, không lộ trạng thái chờ xác thực. `[SoT: Suy luận — mã IDT-113 đề xuất]`

### AC-42 — Không mở khoá được tài khoản tự xoá đã ẩn danh hoá (`ADM0201`, BR-08)
- **Tiền điều kiện** (Sửa 2026-10-22 (đồng bộ DB vòng 3, DEC-2026-1022-self-register-pending-login-blocked)): tài khoản `status = DEACTIVATED`, `deactivation_source = 'SELF'`, `anonymized_at` khác NULL; một tài khoản tự xoá còn trong ân hạn (`anonymized_at IS NULL`) để đối chiếu.
- **Hành động**: (1) mở danh sách người dùng của Admin (`GET /admin/users`); (2) gọi `PATCH /admin/users/{userId}/status` mở khoá cho cả hai tài khoản.
- **Kết quả mong đợi**: (1) dòng đã ẩn danh hoá có `anonymized = true` và nút mở khoá bị vô hiệu; dòng còn trong ân hạn có `anonymized = false`, nút bấm được; (2) tài khoản đã ẩn danh hoá bị từ chối (thất bại một phần, lý do "đã ẩn danh hoá"), `status` vẫn `DEACTIVATED`, `email` vẫn dạng `deleted-user-<id>@anon.local`; tài khoản trong ân hạn được mở khoá, `status = ACTIVE`, `deactivation_source` và `deactivated_at` về `NULL`. `[SoT: Suy luận — tên trường `anonymized` do DD đề xuất]`

### AC-43 — Xoá lớp phát `ClassDeleted`, dọn dữ liệu con, gỡ gắn lớp khỏi feed (BR-22, thêm 2026-10-22, đồng bộ DB vòng 4, `DEC-2026-1022-db-sync-round4`)
- **Tiền điều kiện**: giảng viên A sở hữu lớp L có mã mời còn hạn, ba học viên ghi danh (một người đã `left_at`), và hai dòng `identity_recent_activity` có `scope_class_id = L`.
- **Hành động**: A gọi `DELETE /classes/{L}`.
- **Kết quả mong đợi**: thành công; lớp L không còn; mã mời và toàn bộ dòng `class_enrollments` của L biến mất; hai dòng hoạt động vẫn còn, `scope_class_id` là rỗng, vẫn thấy ở feed cá nhân của học viên, không còn ở feed của lớp; message `ClassDeleted` với `classId = L` được publish; `identity` không có câu lệnh nào ghi vào schema `problem-bank` hay `judge`. Giảng viên B (không sở hữu L) gọi cùng endpoint bị từ chối và không có message nào được publish. Phía `problem-bank` (nhận sự kiện): các dòng `class_assignments` của L có `removed_at` khác rỗng, nhận lại cùng sự kiện lần hai không đổi gì (kiểm trong `04-tdd/problem-bank.md`).

### AC-44 — Mặc định phỏng vấn giả lập lưu theo người dùng (BR-07, thêm 2026-10-22, đồng bộ DB vòng 4, ID-23)
- **Tiền điều kiện**: học viên chưa có dòng `user_preferences`.
- **Hành động**: (1) `GET /me/settings`; (2) `PATCH /me/settings` với `preferredMaxTurns = 16`, `preferredHintAllowed = false`; (3) `GET /me/settings` lại; (4) `PATCH` với `preferredMaxTurns = 10`.
- **Kết quả mong đợi**: (1) trả mặc định 12 và bật gợi ý, không phải `null`; (2) thành công, dòng `user_preferences` được tạo; (3) trả 16 và tắt gợi ý; (4) bị từ chối `422` (chỉ nhận 8, 12, 16), giá trị đã lưu không đổi. Việc lưu không gọi `ai-review`: `ai-review` ngừng phản hồi thì (2) vẫn thành công.

### AC-45 — Ma trận quyền mặc định có function `LANGUAGE_CONFIG` (BR-13, thêm 2026-10-23, `DEC-2026-1023-db-actor-review-fixes`)
- **Tiền điều kiện**: cơ sở dữ liệu mới tạo, vừa chạy seed; ma trận quyền gồm 11 function (thêm `LANGUAGE_CONFIG`, F4-11) và 3 role hệ thống.
- **Hành động**: (1) đếm các dòng `permissions` sau seed; (2) đọc các ô của function `LANGUAGE_CONFIG` ở ba role; (3) đăng nhập bằng ADMIN và mở màn cấu hình ngôn ngữ (`ADM0501`); (4) đăng nhập bằng INSTRUCTOR và mở cùng chức năng.
- **Kết quả mong đợi**: (1) đúng 132 dòng (3 role x 11 function x 4 action); (2) ADMIN có cả bốn action `granted = true`, INSTRUCTOR và STUDENT đều `false`; (3) truy cập được; (4) bị từ chối `403`. Admin bật quyền cho INSTRUCTOR ở màn ma trận thì INSTRUCTOR dùng được ngay mà không cần khởi động lại.

### AC-46 — Tài khoản ADMIN đầu tiên được tạo một lần lúc khởi động và buộc đổi mật khẩu (BR-23, thêm 2026-10-23)
- **Tiền điều kiện**: cơ sở dữ liệu chưa có người dùng nào thuộc role `ADMIN`; hai biến môi trường `ALGOPREP_BOOTSTRAP_ADMIN_EMAIL`, `ALGOPREP_BOOTSTRAP_ADMIN_PASSWORD` đã đặt.
- **Hành động**: (1) khởi động ứng dụng, đọc `users`; (2) khởi động lại lần hai, lần này đổi giá trị biến mật khẩu; (3) đăng nhập bằng mật khẩu ở bước (1); (4) tách riêng: xoá hết ADMIN, bỏ một biến môi trường rồi khởi động.
- **Kết quả mong đợi**: (1) có đúng một tài khoản ADMIN, `status = PENDING_VERIFICATION`, `must_change_password = true`, không có dòng `system_audit_logs` nào cho việc này; (2) vẫn một ADMIN, mật khẩu không đổi; (3) thành công, `status` chuyển `ACTIVE` và giao diện yêu cầu đổi mật khẩu (như AC-35); (4) ứng dụng dừng khởi động kèm thông báo nêu rõ biến còn thiếu, không có tài khoản nào được tạo.

### AC-47 — Job ẩn danh hoá bỏ qua tài khoản bị Admin khoá (BR-08, thêm 2026-10-23)
- **Tiền điều kiện**: hai tài khoản `status = DEACTIVATED` đều đã khoá quá 30 ngày và `anonymized_at IS NULL`: tài khoản X có `deactivation_source = 'SELF'`, tài khoản Y có `deactivation_source = 'ADMIN'`; X có liên kết đăng nhập OAuth và các trường tự khai (trường/công ty, vị trí hiện tại, vị trí mục tiêu).
- **Hành động**: chạy job ẩn danh hoá (hoặc chờ đến lịch chạy), sau đó chạy lần hai.
- **Kết quả mong đợi**: X có email dạng `deleted-user-<id>@anon.local`, tên "Người dùng đã xoá", các trường tự khai về rỗng, `anonymized_at` khác rỗng, liên kết OAuth bị xoá; Y giữ nguyên email, tên và `anonymized_at` rỗng, Admin vẫn mở khoá được Y; lần chạy thứ hai không đổi gì ở X (idempotent).

## Tổng hợp

Tổng: 49 tiêu chí (Sửa 2026-10-23: thêm AC-45, AC-46, AC-47; số bên dưới tính trước đợt này) cũ: 46 tiêu chí (Sửa 2026-10-22 (đồng bộ DB vòng 4): thêm AC-43, AC-44; các số bên dưới tính trước vòng 4, còn nguyên với 44 tiêu chí cũ), cũ: 44 tiêu chí (`AC-01` → `AC-42`, kèm `AC-18b` và `AC-20b`; Sửa 2026-10-22 (đồng bộ DB vòng 3, DEC-2026-1022-self-register-pending-login-blocked): thêm AC-41, AC-42; AC-09 và AC-40 mở rộng/sửa), phủ 18/48 endpoint có hành vi rẽ nhánh rõ (endpoint thuần đọc không
rẽ nhánh, ví dụ `GetMyProfile`, `ListUsers`, không cần AC riêng — kiểm bằng test tích hợp thông thường,
không phải tiêu chí nghiệm thu nghiệp vụ) và toàn bộ 18 khối BR ở `03-dd/logic/identity.md` (BR-04, BR-07,
BR-17 không có AC riêng vì là luồng nội bộ không lộ qua API công khai theo cách kiểm tra
được từ bên ngoài, hoặc đã phủ gián tiếp qua AC khác — ví dụ AC-09/AC-10 phủ BR-04). **Sửa 2026-10-22 (phase 5):** BR-12 nay có AC-33;
BR-18 đã bỏ nên AC-34 chỉ kiểm sự vắng mặt của loại sự kiện cũ. Số "18/48 endpoint" chưa tính lại sau khi thêm #53, #54.

| Criterion | Test case | Trạng thái |
| :--- | :--- | :--- |
| AC-01 → AC-32 (kèm AC-18b, AC-20b) | (điền khi `testcase-generation` chạy) | Chưa sinh |
| AC-43, AC-44 (mới 2026-10-22, đồng bộ DB vòng 4) | (điền khi `testcase-generation` chạy) | Chưa sinh |
| AC-45, AC-46, AC-47 (mới 2026-10-23, `DEC-2026-1023-db-actor-review-fixes`) | (điền khi `testcase-generation` chạy) | Chưa sinh |
| AC-33 → AC-42 (mới 2026-10-22, phase 5, vòng 2 và vòng 3; AC-36 → AC-38, AC-40 chờ chốt F1-33/#55; AC-41 chờ chốt mã `IDT-113`) | (điền khi `testcase-generation` chạy) | Chưa sinh |

## Tham chiếu

`03-dd/api/identity.md`, `03-dd/logic/identity.md`, `03-dd/validation/identity.md`,
`02-bd/security/identity.md`, `02-bd/database/identity.md`.
