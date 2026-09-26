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
- **Kết quả mong đợi**: request xoá trả `200`; request tiếp theo bằng access token cũ trả `401` vì filter kiểm `status = DEACTIVATED`.

### AC-18 — Ẩn danh hoá sau 30 ngày ân hạn (BR-08, job định kỳ)
- **Tiền điều kiện**: `deactivated_at <= now() - 30 ngày`, `anonymized_at IS NULL`.
- **Hành động**: chạy job ẩn danh hoá.
- **Kết quả mong đợi**: `email`/`display_name` đổi thành giá trị vô danh, `id` giữ nguyên, các bảng `judge`/`ai` liên quan không bị cascade xoá.

## Lớp học

### AC-19 — Tham gia lớp bằng mã hết hạn (BR-09)
- **Hành động**: `POST /classes/join` với `code` đã quá `expires_at`.
- **Kết quả mong đợi**: `422 IDT-410`, không tạo `class_enrollments`.

### AC-20 — Tham gia lớp trùng lặp (idempotent, BR-09)
- **Tiền điều kiện**: học viên đã ở trong lớp.
- **Hành động**: `POST /classes/join` lại với mã hợp lệ của cùng lớp.
- **Kết quả mong đợi**: `200`, không tạo dòng `class_enrollments` thứ hai.

### AC-21 — Gỡ học viên phát sự kiện, không đụng schema khác (BR-11)
- **Hành động**: `DELETE /classes/{classId}/students/{studentId}` bởi đúng giảng viên sở hữu lớp.
- **Kết quả mong đợi**: `200`, dòng `class_enrollments` bị xoá, message `StudentRemovedFromClass` được publish; `identity` không có câu lệnh nào ghi vào schema `judge`.

### AC-22 — Gỡ học viên bởi giảng viên không sở hữu lớp
- **Hành động**: giảng viên B gọi `DELETE` trên lớp của giảng viên A.
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

## Tổng hợp

Tổng: 32 tiêu chí (`AC-01` → `AC-32`), phủ 18/48 endpoint có hành vi rẽ nhánh rõ (endpoint thuần đọc không
rẽ nhánh, ví dụ `GetMyProfile`, `ListUsers`, không cần AC riêng — kiểm bằng test tích hợp thông thường,
không phải tiêu chí nghiệm thu nghiệp vụ) và toàn bộ 18 khối BR ở `03-dd/logic/identity.md` (BR-04, BR-07,
BR-12, BR-17, BR-18 không có AC riêng vì là luồng nội bộ không lộ qua API công khai theo cách kiểm tra
được từ bên ngoài, hoặc đã phủ gián tiếp qua AC khác — ví dụ AC-09/AC-10 phủ BR-04).

| Criterion | Test case | Trạng thái |
| :--- | :--- | :--- |
| AC-01 → AC-32 | (điền khi `testcase-generation` chạy) | Chưa sinh |

## Tham chiếu

`03-dd/api/identity.md`, `03-dd/logic/identity.md`, `03-dd/validation/identity.md`,
`02-bd/security/identity.md`, `02-bd/database/identity.md`.
