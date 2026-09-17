# BD — Security module `identity` (F1)

## 1. Xác thực

- **Mật khẩu**: băm bằng BCrypt (Spring Security mặc định), không lưu plaintext, không lưu mã hoá đối
  xứng có thể giải mã ngược.
- **Access Token**: JWT ký bằng khoá bất đối xứng (RS256), TTL 15 phút (`02-bd/architecture/identity.md`
  mục 5). Claim chỉ chứa `sub` (user id), `base_category`, `role_code`, `iat`, `exp` — **không** nhúng
  toàn bộ ma trận quyền vào token, vì đổi ô ma trận phải có hiệu lực ngay không cần chờ token hết hạn
  (`identity.md` dòng 50) — quyền chi tiết luôn tra tại thời điểm request qua cache (mục 3).
- **Refresh Token**: chuỗi ngẫu nhiên (không phải JWT), lưu ở cookie `HttpOnly` + `Secure` + `SameSite=Strict`.
  Chỉ lưu **hash** trong Postgres (`refresh_tokens.token_hash`), không lưu bản rõ. **Rotation**: mỗi lần
  dùng refresh token phát hành token mới, revoke token cũ ngay (`revoked_at`); nếu một token đã bị revoke
  lại được gửi lên lần nữa → coi cả `family_id` là compromised, revoke toàn bộ token cùng family, buộc
  đăng nhập lại (phát hiện refresh-token reuse).
- **CSRF trên endpoint refresh**: vì refresh token nằm trong cookie (trình duyệt tự gửi kèm), endpoint
  `/auth/refresh` bắt buộc thêm một header tuỳ chỉnh (ví dụ `X-Requested-With`) mà CSRF từ site khác
  không tự chèn được — `SameSite=Strict` đã chặn phần lớn nhưng không đủ với mọi trình duyệt cũ.
- **OAuth (F1-15)**: dùng `state` parameter chống CSRF trong luồng OAuth; chỉ tự động liên kết tài khoản
  khi provider xác nhận email đã verify (`email_verified` claim), không tự liên kết theo email chưa xác
  thực — chặn kịch bản chiếm tài khoản qua provider cho phép email tuỳ ý.

## 2. RBAC — kiểm quyền

- Kiểm ở **tầng application**, không chỉ ẩn/hiện ở giao diện (`identity.md` dòng 13) — mọi use case
  chạm dữ liệu nhạy cảm bắt buộc qua `@PreAuthorize` hoặc tương đương, khớp `FUNCTION:ACTION` cụ thể.
- Kiểm **quyền sở hữu dữ liệu** riêng, tách khỏi RBAC theo role: một `STUDENT` không đọc được bài nộp
  hay phiên phỏng vấn của người khác dù có role hợp lệ — luật này nằm ở tầng use case của module sở hữu
  dữ liệu đó (`judge-orchestration`, `ai-review`), `identity` chỉ cung cấp `user_id` đã xác thực qua
  claim, không tự kiểm sở hữu hộ module khác.
- **Lớp 1 vs Lớp 2 không được lẫn lộn trong code**: quyền học tập cơ bản (F2-F6 cho mọi `STUDENT`) kiểm
  bằng `base_category`, không bao giờ tra bảng `permissions` — tắt một ô trong ma trận không được phép
  ảnh hưởng luồng này (`identity.md` dòng 22-23). Nếu một đoạn code kiểm quyền học tập cơ bản lại đi qua
  bảng `permissions`, đó là lỗi thiết kế cần sửa ngay khi review.
- Cache quyền theo role (`identity:permcache:<roleId>`) tại tầng ứng dụng (Caffeine cục bộ hoặc đọc
  Redis); khi `permissions` đổi, publish một sự kiện nội bộ invalidate cache **ở mọi instance** (Redis
  pub/sub hoặc tương đương) để đáp ứng yêu cầu "có hiệu lực ngay, không khởi động lại dịch vụ".

## 3. OWASP / chống lạm dụng

- **User enumeration (quên mật khẩu)**: phản hồi luôn là một câu chung bất kể email tồn tại hay không
  (`identity.md` dòng 214-217) — áp dụng cho cả bước gửi OTP lẫn thông báo lỗi.
- **Rate limiting**: giới hạn số lần thử sai OTP (5 lần, mục database Redis), giới hạn tần suất gửi lại
  OTP (60 giây), giới hạn số lần thử đăng nhập sai theo email/IP (cửa sổ trượt, đề xuất 15 phút,
  `[SoT: Suy luận]`).
- **Đăng nhập cho tài khoản chỉ OAuth**: từ chối tạo mật khẩu mới qua luồng quên mật khẩu, chỉ gửi email
  hướng dẫn quay lại đúng provider (`identity.md` dòng 218-223) — tránh việc kẻ tấn công dùng luồng quên
  mật khẩu để gắn thêm một phương thức đăng nhập mới vào tài khoản không phải của mình.
- **Input validation**: email theo RFC hợp lệ, độ mạnh mật khẩu tối thiểu (đề xuất 8 ký tự, có chữ và
  số, `[SoT: Suy luận]`, chốt số cụ thể ở DD).
- **Audit không có ngoại lệ**: mọi thay đổi ma trận quyền, đổi vai trò, khoá/mở khoá, reset mật khẩu đều
  ghi `system_audit_logs`, kể cả khi ADMIN tự thao tác trên chính tài khoản ADMIN khác (`identity.md`
  dòng 69-70).
- **Xuất dữ liệu cá nhân (F1-22)**: chỉ cho phép xuất dữ liệu của chính người dùng đang đăng nhập —
  kiểm `user_id` trong token khớp `user_id` được yêu cầu xuất, không nhận tham số `user_id` tuỳ ý từ
  client.

## 4. Xoá tài khoản và ẩn danh hoá

- Bấm xoá → `status = DEACTIVATED` ngay, mọi Access Token đang có bị coi là hết hiệu lực từ lần kiểm
  quyền tiếp theo (kiểm `status` mỗi request, không chỉ dựa `exp` của JWT) — ngăn dùng token cũ trong
  15 phút còn lại sau khi tự xoá.
- Job định kỳ (sau 30 ngày ân hạn) ẩn danh hoá `email`/`display_name`, giữ nguyên `id` và mọi bảng liên
  quan ở module khác — không cascade xoá, đúng nguyên tắc "không phá vỡ thống kê lớp" (`identity.md`
  dòng 200-202).

## 5. Việc còn mở — chuyển sang DD

- Thuật toán/thư viện cụ thể cho JWT (ví dụ Nimbus JOSE, hay Spring Security OAuth2 Resource Server).
- Cơ chế invalidate cache quyền phân tán cụ thể (Redis pub/sub vs Spring `@CacheEvict` cluster-aware).
- Độ mạnh mật khẩu tối thiểu chính xác, cửa sổ rate-limit đăng nhập chính xác.
