# BD — Security module `identity` (F1)

## 1. Xác thực

- **Mật khẩu**: băm bằng BCrypt (Spring Security mặc định), không lưu plaintext, không lưu mã hoá đối
  xứng có thể giải mã ngược.
  Chủ dự án chốt 2026-10-30: tối đa **72 byte UTF-8**, không cắt mật khẩu; vượt giới hạn trả `IDT-V02`
  khi đặt mật khẩu (`DEC-2026-1030-bcrypt-password-byte-limit`). Mức tối thiểu vẫn 8 ký tự, có chữ và số;
  quy tắc này thay giới hạn 128 ký tự trước đây trong tài liệu màn hình.
- **Access Token**: JWT ký bằng khoá bất đối xứng (RS256), TTL 15 phút (`02-bd/architecture/F1_identity.md`
  mục 5). Claim chỉ chứa `sub` (user id), `base_category`, `role_code`, `sid` (family id của refresh token, để
  đổi mật khẩu giữ lại đúng phiên đang thao tác, sửa 2026-10-31, `DEC-2026-1031-access-token-session-id`), `iat`, `exp` — **không** nhúng
  toàn bộ ma trận quyền vào token, vì đổi ô ma trận phải có hiệu lực ngay không cần chờ token hết hạn
  (`F1_identity.md` dòng 50) — quyền chi tiết luôn tra tại thời điểm request qua cache (mục 3).
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

- Kiểm ở **tầng application**, không chỉ ẩn/hiện ở giao diện (`F1_identity.md` dòng 13) — mọi use case
  chạm dữ liệu nhạy cảm bắt buộc qua `@PreAuthorize` hoặc tương đương, khớp `FUNCTION:ACTION` cụ thể.
- Kiểm **quyền sở hữu dữ liệu** riêng, tách khỏi RBAC theo role: một `STUDENT` không đọc được bài nộp
  hay phiên phỏng vấn của người khác dù có role hợp lệ — luật này nằm ở tầng use case của module sở hữu
  dữ liệu đó (`judge-orchestration`, `ai-review`), `identity` chỉ cung cấp `user_id` đã xác thực qua
  claim, không tự kiểm sở hữu hộ module khác.
- **Lớp 1 vs Lớp 2 không được lẫn lộn trong code**: quyền học tập cơ bản (F2-F6 cho mọi `STUDENT`) kiểm
  bằng `base_category`, không bao giờ tra bảng `permissions` — tắt một ô trong ma trận không được phép
  ảnh hưởng luồng này (`F1_identity.md` dòng 22-23). Nếu một đoạn code kiểm quyền học tập cơ bản lại đi qua
  bảng `permissions`, đó là lỗi thiết kế cần sửa ngay khi review.
- Mỗi kiểm quyền Lớp 2 đọc `roles.permission_version` từ PostgreSQL primary; chỉ dùng cache Caffeine/Redis `{permissionVersion,matrix}` khi version khớp. Version khác thì nạp lại ma trận từ primary. Nếu không đọc được version, từ chối trước use case (fail closed), không dùng cache cũ. PATCH quyền tăng version cùng giao dịch ghi permission/audit; Redis pub/sub `{roleId,permissionVersion}` chỉ làm mới cache sớm. Vì vậy request bắt đầu sau PATCH thành công thấy quyền đã commit ngay cả khi message chậm/mất (`DEC-2026-1028-qa-f1-permission-immediate`).
- **Ngoại lệ xem Audit Log cho Quản trị viên (`DEC-2026-0922-users-and-admin-conflict-resolutions`)**:
  Mọi tài khoản có `base_category = ADMIN` đều có quyền xem (READ) nhật ký hệ thống `system_audit_logs`.
  Chức năng `SYSTEM_AUDIT_LOG` trong ma trận phân quyền chỉ gác việc chỉnh sửa cấu hình log / xuất log,
  không gác quyền đọc để tránh nguy cơ cấu hình nhầm dẫn đến Admin bị tước quyền giám sát chính mình.

## 3. OWASP / chống lạm dụng

- **User enumeration (quên mật khẩu)**: phản hồi luôn là một câu chung bất kể email tồn tại hay không
  (`F1_identity.md` dòng 214-217) — áp dụng cho cả bước gửi OTP lẫn thông báo lỗi.
- **Rate limiting**: giới hạn số lần thử sai OTP (5 lần, mục database Redis), giới hạn tần suất gửi lại
  OTP (60 giây), giới hạn số lần thử đăng nhập sai theo email/IP (cửa sổ trượt, đề xuất 15 phút,
  `[SoT: Suy luận]`).
- **Đăng nhập cho tài khoản chỉ OAuth**: từ chối tạo mật khẩu mới qua luồng quên mật khẩu, chỉ gửi email
  hướng dẫn quay lại đúng provider (`F1_identity.md` dòng 218-223) — tránh việc kẻ tấn công dùng luồng quên
  mật khẩu để gắn thêm một phương thức đăng nhập mới vào tài khoản không phải của mình.
- **Input validation**: email theo RFC hợp lệ, độ mạnh mật khẩu tối thiểu (đề xuất 8 ký tự, có chữ và
  số, `[SoT: Suy luận]`, chốt số cụ thể ở DD).
- **Audit không có ngoại lệ**: mọi thay đổi ma trận quyền, đổi vai trò, khoá/mở khoá, reset mật khẩu đều
  ghi `system_audit_logs`, kể cả khi ADMIN tự thao tác trên chính tài khoản ADMIN khác (`F1_identity.md`
  dòng 69-70).
- **Lý do khoá (F1-32, thêm 2026-10-05)**: dòng `system_audit_logs` của thao tác **khoá** phải kèm lý do
  quản trị viên nhập, kể cả khi quyết định tắt gửi email cho người dùng — đây là thứ trả lời "ai khoá
  tài khoản của tôi và vì sao" khi không có người dùng để hỏi. Lý do là văn bản tự do do quản trị viên
  quyết định, không phải danh mục có sẵn, nên **không** dùng làm điều kiện truy vấn lọc được; xem
  `02-bd/screens/admin/ADM0201_user_management.md` Sheet 9 NO 16. Email thông báo cho người dùng chỉ
  được nêu tài khoản bị khoá, thời điểm, lý do và cách liên hệ quản trị viên — **không** lộ dữ liệu của
  tài khoản khác trong cùng tập chọn.
- **Xuất dữ liệu cá nhân (F1-22)**: chỉ cho phép xuất dữ liệu của chính người dùng đang đăng nhập —
  kiểm `user_id` trong token khớp `user_id` được yêu cầu xuất, không nhận tham số `user_id` tuỳ ý từ
  client.

## 4. Xoá tài khoản và ẩn danh hoá

- Bấm xoá → `status = DEACTIVATED` ngay, mọi Access Token đang có bị coi là hết hiệu lực từ lần kiểm
  quyền tiếp theo (kiểm `status` mỗi request, không chỉ dựa `exp` của JWT) — ngăn dùng token cũ trong
  15 phút còn lại sau khi tự xoá. Cùng lúc xoá luôn ảnh đại diện của người đó (mục 6.6, owner chốt 2026-11-17).
- Trong 30 ngày ân hạn, đăng nhập đúng thông tin vào tài khoản `DEACTIVATED` không cấp phiên làm việc đầy đủ mà chỉ mở banner khôi phục (`RestoreDeactivatedAccount`, `02-bd/screens/shared/SHR0101_auth.md` Khu vực E); người dùng bấm huỷ yêu cầu xoá thì `status` về `ACTIVE` (`DEC-2026-1018-account-deletion-grace-cancel`, 2026-10-18).
- Job định kỳ (sau 30 ngày ân hạn) ẩn danh hoá `email`/`display_name`, giữ nguyên `id` và mọi bảng liên
  quan ở module khác — không cascade xoá, đúng nguyên tắc "không phá vỡ thống kê lớp" (`F1_identity.md`
  dòng 200-202).

## 5. Việc còn mở — chuyển sang DD

- Thuật toán/thư viện cụ thể cho JWT (ví dụ Nimbus JOSE, hay Spring Security OAuth2 Resource Server).
- Cơ chế invalidate cache quyền phân tán đã chốt: Redis pub/sub là tối ưu refresh; kiểm version ở PostgreSQL primary là cổng đúng/sai bắt buộc (`DEC-2026-1028-qa-f1-permission-immediate`).
- Độ mạnh mật khẩu tối thiểu chính xác, cửa sổ rate-limit đăng nhập chính xác.

## 6. Ảnh đại diện (F1-35, thêm 2026-11-17, `DEC-2026-1117-user-avatar-upload`)

Ảnh do người dùng tải lên được **mọi người dùng đã đăng nhập** xem, nên một ảnh xấu ảnh hưởng tới nhiều người, không chỉ chủ nhân (owner đã chốt D2b, ghi trong decision). Các điều dưới đây là ràng buộc bảo mật; lưu trữ ở `02-bd/storage/F1_identity.md`, cột và bảng ở `02-bd/database/F1_identity.md` mục 1.16.

### 6.1. Kiểm tra tệp khi tải lên

| Kiểm | Quy tắc | Lý do |
| :--- | :--- | :--- |
| Kích thước | Tối đa 512 KB, **cắt khi đọc luồng** chứ không chỉ tin tiêu đề `Content-Length`; ngưỡng multipart của máy chủ đặt cùng mức | Chặn tệp lớn giả khai báo nhỏ |
| Định dạng | Chỉ PNG, JPEG, WebP, nhận biết bằng byte đầu tệp (PNG `89 50 4E 47 0D 0A 1A 0A`; JPEG `FF D8 FF`; WebP `RIFF....WEBP`). **Không** tin `Content-Type` hay phần mở rộng tên tệp | Tệp đổi đuôi hoặc khai sai kiểu vẫn bị chặn |
| SVG và mọi kiểu khác | Từ chối | SVG chạy được script và có thể tham chiếu tài nguyên ngoài |
| Kích thước điểm ảnh | Đọc từ tiêu đề tệp, tối đa 1024 x 1024; vượt thì từ chối `[SoT: Suy luận]` | Một tệp PNG 512 KB có thể giải nén ra hàng tỉ điểm ảnh; vì ảnh được người khác xem nên trình duyệt của họ phải giải nén. Máy chủ không xử lý ảnh nhưng vẫn phải chặn kích thước khai báo. Giao diện đã thu nhỏ về 256 x 256 trước khi gửi, nên ngưỡng này chỉ chặn tệp gửi thẳng vào API |
| Tên tệp người dùng | Bỏ hẳn; khoá đối tượng do hệ thống sinh | Chống ghi đè và đường dẫn lạ |

### 6.2. Khi phục vụ ảnh

Trả đúng `Content-Type` lấy từ `user_avatars.content_type` (kiểu đã phát hiện lúc tải lên), kèm `X-Content-Type-Options: nosniff`. Vì chỉ có ba kiểu ảnh raster nên không có đường chạy script. Giao diện tải ảnh bằng API client có bearer rồi dựng URL tạm cho trình duyệt, người dùng không "mở" ảnh như một trang, nên không có bề mặt XSS lưu trữ (cách tải chi tiết do DD chốt).

### 6.3. Phân quyền

| Thao tác | Ai được | Ghi chú |
| :--- | :--- | :--- |
| Tải lên, thay, xoá ảnh | Chỉ chính chủ | Mã người dùng lấy từ token, **không** nhận tham số `userId` từ client |
| Đọc ảnh theo mã người dùng | Mọi người dùng đã đăng nhập | Owner chốt D2b; không kiểm lớp hay vai trò. Chưa đăng nhập: từ chối `401` |
| Đọc ảnh của người không có ảnh, không tồn tại, `DEACTIVATED` hoặc đã ẩn danh hoá | Trả **cùng một** phản hồi "không tìm thấy" | Không để người đã đăng nhập dò xem một mã người dùng có tồn tại hay không |
| Gỡ ảnh của người khác | Quản trị viên có `USER_MANAGEMENT:UPDATE` | Chỉ gỡ, không tải thay. Ghi `system_audit_logs` loại `USER_AVATAR_REMOVED` (`02-bd/database/F1_identity.md` mục 1.11); người không có ảnh thì bỏ qua, không ghi dòng nào |

Quản trị viên gỡ được ảnh của người khác (owner chốt 2026-11-17, Q-AV-4); không có đường để quản trị viên tải ảnh thay người khác.

### 6.4. Giới hạn tần suất

Tải lên và xoá ảnh: tối đa 10 lần mỗi giờ cho mỗi người `[SoT: Suy luận]`, cùng cơ chế giới hạn tần suất Redis hiện có (`02-bd/database/F1_identity.md` mục 3), vì mỗi lần ghi đều chạm MinIO. Lượt đọc ảnh dùng giới hạn chung của API.

### 6.5. Nhập ảnh từ nhà cung cấp OAuth (chống SSRF)

Lần đầu liên kết Google hoặc GitHub mà người dùng chưa có ảnh, **máy chủ gọi ra ngoài** để tải ảnh. Đây là bề mặt SSRF nên phải có đủ các điều sau:

| Kiểm soát | Quy tắc |
| :--- | :--- |
| Nguồn URL | Chỉ lấy từ phản hồi của nhà cung cấp trong luồng OAuth (trường ảnh của người dùng), **không bao giờ** từ tham số client |
| Giao thức | Chỉ `https` |
| Máy chủ cho phép | So khớp chính xác sau khi phân tích URL: `lh3.googleusercontent.com`, `avatars.githubusercontent.com` |
| Địa chỉ đích | Phân giải tên miền rồi từ chối địa chỉ nội bộ (loopback, link-local, dải riêng) và kết nối đúng địa chỉ đã kiểm, để chống DNS rebinding |
| Chuyển hướng | Không đi theo chuyển hướng sang máy chủ khác; không cho quá một lần và chỉ trong danh sách cho phép |
| Thời gian | Hết hạn kết nối và đọc ngắn (đề xuất 3 giây và 5 giây `[SoT: Suy luận]`) |
| Dung lượng | Đọc luồng, dừng ở 512 KB |
| Nội dung | Cùng kiểm tra tệp ở mục 6.1 (byte đầu tệp, kích thước điểm ảnh) |
| Cỡ ảnh xin | Xin bản nhỏ qua tham số của nhà cung cấp (Google `=s256-c`, GitHub `?s=256`) `[SoT: Suy luận]` để nằm trong 512 KB (Q-AV-2 đã chốt 2026-11-17: xin bản 256 điểm ảnh, vẫn quá 512 KB thì bỏ qua) |
| Điều kiện chạy | Chỉ khi người dùng chưa có dòng `user_avatars`, và chỉ ở lần liên kết đầu |
| Lỗi | Bỏ qua, ghi log WARN (chỉ ghi máy chủ, **không** ghi toàn bộ URL), **không làm hỏng đăng nhập**. Việc tải ảnh không chặn phản hồi đăng nhập: chạy sau khi đã cấp phiên `[SoT: Suy luận]`, DD chốt cơ chế |

### 6.6. Riêng tư và nhật ký

Xoá tài khoản (F1-16) xoá ảnh ngay khi bấm xoá (`02-bd/storage/F1_identity.md` mục 4). Xuất dữ liệu cá nhân (F1-22) chỉ ghi có hay không có ảnh, không kèm tệp. Log không ghi byte ảnh hay URL ảnh của nhà cung cấp. Việc đổi ảnh không ghi `system_audit_logs` (đó là nhật ký thao tác quản trị, mục 3) `[SoT: Suy luận]`.

### 6.7. Việc còn mở

| # | Câu hỏi | Chủ |
| :--- | :--- | :--- |
| Q-AV-2 | ~~Ảnh nhà cung cấp quá 512 KB~~ **Đã chốt 2026-11-17:** xin bản nhỏ, vẫn quá thì bỏ qua | Owner |
| Q-AV-4 | ~~Quản trị viên có cần gỡ ảnh của người khác không~~ **Đã chốt 2026-11-17:** có (mục 6.3) | Owner |
| Q-AV-5 | Xác nhận trần 1024 x 1024 điểm ảnh khi tải lên | Nhóm phát triển, chốt ở DD validation |
