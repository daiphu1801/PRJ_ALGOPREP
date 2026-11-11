# TDD — Tiêu chí nghiệm thu module `identity` (F1)

> Mỗi `AC-nn` map về đúng một endpoint (`03-dd/api/F1_identity.md` mục 0) hoặc một khối BR
> (`03-dd/logic/F1_identity.md`). Viết cùng lúc với DD theo `tdd-mode.md`.

## 1. Phạm vi

Tiêu chí nghiệm thu module định danh, xác thực, tài khoản, lớp học và số liệu giảng viên; đối chiếu `03-dd/api/F1_identity.md` và các luồng DD cùng module.

## 2. Danh sách tiêu chí

## Xác thực và phiên

### AC-01 — Đăng ký thành công
- **Tiền điều kiện**: email chưa tồn tại trong `users`.
- **Hành động**: gọi `POST /auth/register` với email hợp lệ, mật khẩu đạt chuẩn (mục `03-dd/validation/F1_identity.md` §1), `displayName` không rỗng.
- **Kết quả mong đợi**: Tài khoản học viên mới được tạo; người dùng nhận hướng dẫn xác thực email. Mật khẩu không xuất hiện lại trong phản hồi, nhật ký hoặc thông báo.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #1; [SoT: 03-dd/logic/F1_identity.md].

### AC-02 — Đăng ký trùng email
- **Tiền điều kiện**: email đã tồn tại.
- **Hành động**: gọi `POST /auth/register` với cùng email.
- **Kết quả mong đợi**: Hệ thống báo email đã được dùng; không tạo thêm tài khoản hoặc gửi hướng dẫn đăng ký mới.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #1; [SoT: 03-dd/logic/F1_identity.md].

### AC-03 — Đăng nhập đúng
- **Tiền điều kiện**: tài khoản `status = ACTIVE`, mật khẩu đúng.
- **Hành động**: `POST /auth/login`.
- **Kết quả mong đợi**: Người dùng vào đúng khu theo vai trò và có phiên sử dụng được; thông tin đăng nhập dài hạn chỉ nằm trong cơ chế phiên an toàn của trình duyệt.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #2; [SoT: 03-dd/logic/F1_identity.md].

### AC-04 — Đăng nhập sai mật khẩu 5 lần trong 15 phút (BR-01)
- **Tiền điều kiện**: chưa có lần sai nào trong cửa sổ hiện tại.
- **Hành động**: gọi `POST /auth/login` sai mật khẩu liên tiếp 5 lần cùng `(email, IP)`, lần thứ 6 gọi lại dù mật khẩu đúng.
- **Kết quả mong đợi**: Năm lần sai được báo không thành công; lần thử tiếp theo trong cùng 15 phút bị tạm chặn kể cả khi nhập đúng mật khẩu. Hết thời gian chặn mới thử lại được.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #2, BR-01; [SoT: 03-dd/logic/F1_identity.md].

### AC-05 — Refresh token hợp lệ, rotation (BR-02)
- **Tiền điều kiện**: refresh token còn hạn, chưa bị revoke.
- **Hành động**: `POST /auth/refresh` kèm header `X-Requested-With`.
- **Kết quả mong đợi**: Phiên được gia hạn và tiếp tục dùng được; thông tin gia hạn cũ không thể dùng lại sau lần gia hạn thành công.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #3, BR-02; [SoT: 03-dd/logic/F1_identity.md].

### AC-06 — Refresh token bị tái sử dụng sau khi đã rotate (BR-02)
- **Tiền điều kiện**: đã refresh một lần (token A đã bị revoke, token B đang hiệu lực).
- **Hành động**: gọi lại `POST /auth/refresh` với token A (đã revoke).
- **Kết quả mong đợi**: Lần dùng lại thông tin gia hạn cũ bị từ chối; toàn bộ phiên thuộc cùng chuỗi đăng nhập bị thu hồi và sự kiện bảo mật được ghi một lần.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #3, BR-02; [SoT: 03-dd/logic/F1_identity.md].

### AC-07 — Refresh thiếu header chống CSRF
- **Tiền điều kiện**: phiên có refresh token còn hạn, chưa bị thu hồi; chưa gửi yêu cầu gia hạn nào bằng token này.
- **Hành động**: gọi `POST /auth/refresh` không kèm `X-Requested-With`.
- **Kết quả mong đợi**: Yêu cầu gia hạn thiếu dấu hiệu đến từ ứng dụng hợp lệ bị từ chối; không cấp phiên mới hoặc đổi trạng thái phiên hiện có.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #3; [SoT: 03-dd/logic/F1_identity.md].

### AC-08 — Quên mật khẩu với email không tồn tại (chống dò email)
- **Tiền điều kiện**: có một email thuộc tài khoản hợp lệ và một email chưa gắn với tài khoản nào; cùng điều kiện hạn mức gửi mã.
- **Hành động**: gọi `POST /auth/password/forgot` lần lượt với email có tài khoản và email không tồn tại.
- **Kết quả mong đợi**: Email có hay không có tài khoản đều nhận cùng `200` và thông báo chung; hệ thống chỉ gửi mã khi tài khoản hợp lệ, không tiết lộ sự tồn tại của email qua phản hồi. Gọi lại trong cooldown 60 giây vẫn trả cùng 200/body, không gửi mã mới hay kéo dài TTL; hết cooldown mới gửi lại.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #7; [SoT: 03-dd/logic/F1_identity.md].

### AC-09 — Đặt lại mật khẩu với OTP đúng
- **Tiền điều kiện**: đã có OTP hợp lệ trong Redis, chưa hết hạn, chưa sai quá 5 lần.
- **Hành động**: `POST /auth/password/reset` với OTP đúng, mật khẩu mới hợp lệ.
- **Kết quả mong đợi**: Mã đúng đổi được mật khẩu; những phiên đã cấp trước đó không còn dùng để gia hạn, người dùng đăng nhập lại bằng mật khẩu mới.
- **Mở rộng (Sửa 2026-10-22 (đồng bộ DB vòng 3, DEC-2026-1022-self-register-pending-login-blocked), BR-04 bước 4)**: lặp lại với (a) tài khoản Admin tạo đang `status = PENDING_VERIFICATION`, `must_change_password = true`; (b) tài khoản `ACTIVE` có `must_change_password = true`. Kết quả: ở cả hai, `must_change_password = false`; ở (a) `status` chuyển `ACTIVE`; ở (b) `status` giữ `ACTIVE`. Đăng nhập ngay sau đó bằng mật khẩu mới thành công, phản hồi `user.mustChangePassword = false`.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #8; [SoT: 03-dd/logic/F1_identity.md].

### AC-10 — Đặt lại mật khẩu cho tài khoản chỉ OAuth
- **Tiền điều kiện**: `users.password_hash IS NULL`, chưa từng đặt mật khẩu.
- **Hành động**: `POST /auth/password/reset` với OTP đúng.
- **Kết quả mong đợi**: Tài khoản chỉ đăng nhập liên kết không được đặt lại bằng luồng mật khẩu; hệ thống hướng dẫn trở lại nhà cung cấp, không tự tạo mật khẩu mới.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #8; [SoT: 03-dd/logic/F1_identity.md].

### AC-11 — Liên kết OAuth khi email chưa xác thực ở provider (BR-03)
- **Tiền điều kiện**: provider trả `email_verified = false`.
- **Hành động**: hoàn tất `GET /auth/oauth/{provider}/callback`.
- **Kết quả mong đợi**: Nhà cung cấp chưa xác thực email thì hệ thống từ chối liên kết; không tạo tài khoản hoặc liên kết mới.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #5–6; [SoT: 03-dd/logic/F1_identity.md].

## Hồ sơ và cài đặt

### AC-12 — Cập nhật hồ sơ bỏ qua trường quyền
- **Tiền điều kiện**: người dùng đã đăng nhập, có hồ sơ và biết giá trị `roleId`, `status` hiện tại.
- **Hành động**: `PATCH /me/profile` gửi kèm `roleId`, `status` cùng các trường hợp lệ khác.
- **Kết quả mong đợi**: Các trường hồ sơ được phép thay đổi theo yêu cầu; vai trò và trạng thái tài khoản giữ nguyên dù người dùng tự gửi thêm trường đó.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #10; [SoT: 03-dd/logic/F1_identity.md].

### AC-13 — Đổi email yêu cầu OTP xác nhận
- **Tiền điều kiện**: người dùng đã đăng nhập; email mới chưa thuộc tài khoản khác và có thể nhận OTP; email hiện tại vẫn dùng để đăng nhập.
- **Hành động**: gọi `RequestMyEmailChange` rồi `ConfirmMyEmailChange` với OTP đúng.
- **Kết quả mong đợi**: Email đăng nhập giữ nguyên trong lúc chờ xác minh; chỉ mã hợp lệ gửi tới email mới mới hoàn tất thay đổi. Mã sai không đổi email.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #11–12; [SoT: 03-dd/logic/F1_identity.md].

### AC-14 — Đổi mật khẩu thu hồi phiên khác, giữ phiên hiện tại (BR-06)
- **Tiền điều kiện**: user đang có ≥ 2 refresh token hiệu lực (2 thiết bị).
- **Hành động**: `POST /me/password` (biến thể có `currentPassword`) từ thiết bị A.
- **Kết quả mong đợi**: Đổi mật khẩu thành công; phiên đang thao tác tiếp tục dùng được, các thiết bị khác phải đăng nhập lại. Mật khẩu cũ sai trả `422 IDT-115`, không đổi hash/timestamp, không thu hồi phiên hay phát email/audit thành công.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #13; [SoT: 03-dd/logic/F1_identity.md].

### AC-15 — Đặt mật khẩu lần đầu cho tài khoản OAuth, không cần OTP (BR-05)
- **Tiền điều kiện**: `password_hash IS NULL`, phiên đăng nhập hợp lệ.
- **Hành động**: `POST /me/password` không kèm `currentPassword`.
- **Kết quả mong đợi**: Tài khoản chỉ đăng nhập liên kết có thể đặt mật khẩu lần đầu mà không cần mật khẩu cũ hay mã email bổ sung; người dùng nhận thông báo bảo mật về việc này. Mật khẩu yếu bị từ chối (`IDT-V01`), sau khi đặt thì đăng nhập được bằng mật khẩu mới và lần đổi tiếp theo bắt buộc `currentPassword` đúng. Nếu email thông báo không gửi được (SMTP lỗi), mật khẩu vẫn được đặt và phản hồi báo `noticeEmailFailed = true` để màn hình hiện cảnh báo.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #13; [SoT: 03-dd/logic/F1_identity.md].

### AC-16 — Xoá tài khoản với xác nhận email sai
- **Tiền điều kiện**: tài khoản đang hoạt động, đã đăng nhập; `emailConfirmation` khác email hiện tại.
- **Hành động**: `POST /me/account/delete` với `emailConfirmation` không khớp email hiện tại.
- **Kết quả mong đợi**: Email xác nhận không khớp thì yêu cầu tự xoá bị từ chối; tài khoản và phiên hiện có giữ nguyên.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #18; [SoT: 03-dd/logic/F1_identity.md].

### AC-17 — Xoá tài khoản thành công, token cũ vô hiệu ngay (BR-08)
- **Tiền điều kiện**: xác nhận email đúng.
- **Hành động**: `POST /me/account/delete` thành công, sau đó gọi bất kỳ endpoint nào bằng access token cũ (còn hạn).
- **Kết quả mong đợi**: Yêu cầu tự xoá hợp lệ chuyển tài khoản vào thời gian ân hạn; các phiên cũ mất hiệu lực ngay, bài nộp và lịch sử học tập chưa bị xoá.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #18; [SoT: 03-dd/logic/F1_identity.md].

### AC-18 — Ẩn danh hoá sau 30 ngày ân hạn, chỉ tài khoản tự xoá (BR-08, job định kỳ)
- **Tiền điều kiện** (sửa 2026-10-22, phase 5): tài khoản có `status = DEACTIVATED`, `deactivation_source = 'SELF'`, `deactivated_at <= now() - 30 ngày`, `anonymized_at IS NULL`; có `phone`, `school_or_company`, `current_position`, `target_position` khác NULL và ít nhất một dòng `oauth_identities`.
- **Hành động**: chạy job ẩn danh hoá.
- **Kết quả mong đợi**: Sau 30 ngày ân hạn của yêu cầu tự xoá, thông tin nhận diện và liên kết đăng nhập bị ẩn danh; lịch sử bài nộp vẫn gắn với mã tài khoản để giữ số liệu. Chạy lại tác vụ không đổi kết quả.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] JOB ẩn danh, BR-08; [SoT: 03-dd/logic/F1_identity.md].

### AC-18b — Job ẩn danh hoá không đụng tài khoản bị Admin khoá (BR-08, thêm 2026-10-22, phase 5)
- **Tiền điều kiện**: tài khoản `status = DEACTIVATED`, `deactivation_source = 'ADMIN'`, `deactivated_at` đã quá 30 ngày.
- **Hành động**: chạy job ẩn danh hoá.
- **Kết quả mong đợi**: Tài khoản bị quản trị viên khoá vẫn giữ thông tin và phương thức đăng nhập để có thể mở khoá; tác vụ ẩn danh chỉ xử lý yêu cầu tự xoá đã hết hạn.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] JOB ẩn danh, BR-08; [SoT: 03-dd/logic/F1_identity.md].

## Lớp học

### AC-19 — Tham gia lớp bằng mã hết hạn (BR-09)
- **Tiền điều kiện**: học viên đã đăng nhập, chưa ghi danh vào lớp; mã mời của lớp có `expires_at` trước thời điểm gửi yêu cầu.
- **Hành động**: `POST /classes/join` với `code` đã quá `expires_at`.
- **Kết quả mong đợi**: Mã mời hết hạn không cho tham gia; danh sách học viên và tiến độ lớp không thêm người này.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #25; [SoT: 03-dd/logic/F1_identity.md].

### AC-20 — Tham gia lớp trùng lặp (idempotent, BR-09)
- **Tiền điều kiện**: học viên đã ở trong lớp.
- **Hành động**: `POST /classes/join` lại với mã hợp lệ của cùng lớp.
- **Kết quả mong đợi**: Tham gia lại bằng cùng mã khi vẫn ở trong lớp trả kết quả thành công nhưng chỉ có một lần ghi danh; ngày tham gia không đổi.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #25; [SoT: 03-dd/logic/F1_identity.md].

### AC-20b — Vào lại lớp bằng mã mời sau khi đã bị gỡ (BR-09, ĐÃ CHỐT 2026-10-14 DEC-2026-1014-class-leave-details)
- **Tiền điều kiện**: học viên đã bị gỡ khỏi lớp (dòng `class_enrollments` có `left_at` khác rỗng); có mã mời còn hạn của lớp đó.
- **Hành động**: `POST /classes/join` với mã hợp lệ.
- **Kết quả mong đợi**: Học viên đã bị gỡ có thể dùng mã còn hạn để vào lại; danh sách lớp hiện đúng một lần ghi danh và giữ lịch sử học tập cũ. Tiến độ lớp chỉ tính các lượt thuộc thời gian ghi danh và thời gian bài được giao theo hợp đồng lớp.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #25; [SoT: 03-dd/logic/F1_identity.md].

### AC-21 — Học viên rời lớp (gỡ): phát sự kiện, không đụng schema khác (BR-11)
- **Tiền điều kiện**: học viên đang ghi danh trong lớp của giảng viên gọi lệnh và có lịch sử bài nộp thuộc lớp.
- **Hành động**: ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-class-leave-details): `POST /classes/{classId}/students/{studentId}/leave` ~~`DELETE /classes/{classId}/students/{studentId}`~~ bởi đúng giảng viên sở hữu lớp.
- **Kết quả mong đợi**: Gỡ học viên làm người đó rời danh sách đang học nhưng vẫn xem được lịch sử bài nộp. Các phần phụ thuộc nhận được thông tin gỡ lớp; thao tác không xoá lịch sử học tập. Nếu publish RabbitMQ lỗi sau commit, gọi lại endpoint trả `200`, giữ `left_at` và phát lại event với `occurredAt=left_at` gốc; consumer xử lý bản sao idempotent.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #27–28; [SoT: 03-dd/logic/F1_identity.md].

### AC-22 — Gỡ học viên bởi giảng viên không sở hữu lớp
- **Tiền điều kiện**: học viên đang ghi danh trong lớp thuộc giảng viên A; giảng viên B có phiên hợp lệ nhưng không sở hữu lớp này.
- **Hành động**: giảng viên B gọi `POST .../leave` (ĐÃ CHỐT 2026-10-14, DEC-2026-1014-class-leave-details; ~~`DELETE`~~) trên lớp của giảng viên A.
- **Kết quả mong đợi**: Giảng viên không quản lý lớp bị từ chối khi gỡ học viên; danh sách lớp và lịch sử học viên giữ nguyên.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #27; [SoT: 03-dd/logic/F1_identity.md].

### AC-23 — Phân loại "Vắng bài" theo cửa sổ trượt 7 ngày (BR-10)
- **Tiền điều kiện**: học viên có lượt nộp cuối cách đúng 8 ngày trước thời điểm kiểm tra.
- **Hành động**: `GET /classes/{classId}/students`.
- **Kết quả mong đợi**: Học viên không có lượt nộp thuộc lớp trong bảy ngày gần nhất được gắn nhãn Vắng bài; lượt nộp ngoài thời gian được giao không làm mất nhãn này.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #26, BR-10; [SoT: 03-dd/logic/F1_identity.md].

### AC-24 — Thứ tự ưu tiên nhãn phân loại (BR-10)
- **Tiền điều kiện**: một học viên vừa `ABSENT` (không nộp 7 ngày) vừa có hoàn thành < 50%.
- **Hành động**: `GET /classes/{classId}/students`.
- **Kết quả mong đợi**: Khi một học viên đồng thời thoả nhiều nhãn, màn chỉ hiện nhãn có mức ưu tiên cao nhất; Vắng bài được ưu tiên trước Theo dõi.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #26, BR-10; [SoT: 03-dd/logic/F1_identity.md].

## RBAC và quản trị

### AC-25 — Đổi ô ma trận có hiệu lực ngay, không cần khởi động lại (BR-13)
- **Tiền điều kiện**: một vai trò có quyền đang bật; hai instance backend đang chạy và instance thứ hai đã cache quyền đó; người gọi có quyền sửa ma trận.
- **Hành động**: `PATCH /rbac/matrix/{roleId}/{functionId}/{actionId}` tắt một quyền đang bật, giữ Redis invalidate chưa giao cho instance backend khác đã cache quyền đó, rồi gửi request cần quyền sau response PATCH thành công.
- **Kết quả mong đợi**: PATCH commit quyền, audit và `roles.permission_version` trước khi trả thành công. Request mới trên instance khác đọc version từ PostgreSQL primary, không dùng cache cũ và trả `403 IDT-445` dù Redis message chưa đến; không cần khởi động lại. Use case bị gác không chạy và không ghi side effect.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #31; [SoT: 03-dd/logic/F1_identity.md].

### AC-26 — Không xoá được role hệ thống (BR-14)
- **Tiền điều kiện**: ba role hệ thống `STUDENT`, `INSTRUCTOR`, `ADMIN` đang tồn tại; người gọi có quyền quản lý role.
- **Hành động**: `DELETE /rbac/roles/{roleId}` với `roleId` của `STUDENT`/`INSTRUCTOR`/`ADMIN`.
- **Kết quả mong đợi**: Mỗi role hệ thống trả `422 IDT-421`; role/quyền không đổi, không ghi audit xoá thành công; người dùng đang có vai trò đó vẫn giữ quyền truy cập.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #34; [SoT: 03-dd/logic/F1_identity.md].

### AC-27 — Không xoá được role tuỳ biến còn người gán (BR-14)
- **Tiền điều kiện**: role tuỳ biến có ≥ 1 user gán.
- **Hành động**: `DELETE /rbac/roles/{roleId}`.
- **Kết quả mong đợi**: Role tuỳ biến còn người dùng gán trả `422 IDT-422`; người dùng và quyền hiện có giữ nguyên, không ghi audit xoá thành công. Role tuỳ biến không còn người gán xoá thành công trả `204` không body; role/quyền bị xoá và đúng một audit `ROLE_DELETED` commit cùng giao dịch. Nếu ghi audit lỗi, rollback và trả `503 IDT-446`.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #34; [SoT: 03-dd/logic/F1_identity.md].

### AC-28 — Chặn gỡ ô `PERMISSION_MATRIX:UPDATE` cuối cùng của mọi role ADMIN (BR-13)
- **Tiền điều kiện**: chỉ còn đúng một role `base_category = ADMIN` có `PERMISSION_MATRIX:UPDATE = true`.
- **Hành động**: `PATCH` tắt đúng ô đó.
- **Kết quả mong đợi**: Không thể bỏ quyền cập nhật ma trận cuối cùng của nhóm quản trị; ô quyền và quyền truy cập hiện hành giữ nguyên.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #31; [SoT: 03-dd/logic/F1_identity.md].

### AC-29 — ADMIN tự khoá khi còn quản trị viên khác (BR-15)
- **Tiền điều kiện**: quản trị viên có phiên hợp lệ, tài khoản của chính mình đang `ACTIVE` và còn ít nhất một ADMIN `ACTIVE` khác.
- **Hành động**: giao diện cảnh báo rõ hậu quả, yêu cầu xác nhận lần nữa, rồi ADMIN gọi `ChangeUserStatus` trên chính `userId` của mình để khoá với `acknowledgeSelfImpact=true`; thử riêng khi cờ thiếu/`false`.
- **Kết quả mong đợi**: sau xác nhận, thao tác được phép vì vẫn còn ADMIN hoạt động; tài khoản chuyển `DEACTIVATED`, phiên cũ không còn dùng được và thao tác thành công được ghi audit. Thiếu/`false` trả `400 IDT-V21`, không ghi user/phiên/audit/email. Nếu đây là ADMIN hoạt động cuối cùng thì bị chặn `422 IDT-432` theo AC-30.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #37; [SoT: 03-dd/logic/F1_identity.md] BR-15; [SoT: 02-bd/screens/admin/ADM0201_user_management.md] Q6/Sheet 9 NO 6–8; `DEC-2026-1001-admin-configurable-settings`.

### AC-30 — Chặn xoá ADMIN hoạt động cuối cùng (BR-15)
- **Tiền điều kiện**: chỉ còn một tài khoản `base_category = ADMIN, status = ACTIVE`.
- **Hành động**: một ADMIN khác (hoặc chính nó qua một phiên khác) gọi khoá/hạ vai trò tài khoản đó.
- **Kết quả mong đợi**: Thao tác khiến hệ thống không còn quản trị viên hoạt động bị từ chối; quản trị viên cuối cùng vẫn đăng nhập và quản lý được.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #36–37; [SoT: 03-dd/logic/F1_identity.md].

### AC-31 — Đọc nhật ký hệ thống không bị chặn bởi ma trận quyền
- **Tiền điều kiện**: tài khoản `base_category = ADMIN`, ô `SYSTEM_AUDIT_LOG:READ` trong ma trận đang `false` (nếu có).
- **Hành động**: `GET /admin/audit-logs`.
- **Kết quả mong đợi**: Mọi quản trị viên được đọc nhật ký theo phạm vi quản trị, kể cả khi không có quyền sửa ma trận quyền hoặc cấu hình nhật ký. JWT hợp lệ nhưng `baseCategory != ADMIN` trả `403 IDT-447` và không lộ item/metadata.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #39; [SoT: 03-dd/logic/F1_identity.md].

### AC-32 — Che dữ liệu nhạy cảm khi xuất audit log (BR-16)
- **Tiền điều kiện**: có dòng `system_audit_logs` với `after_json` chứa khoá `password_hash`.
- **Hành động**: `GET /admin/audit-logs/export?format=csv`.
- **Kết quả mong đợi**: Tệp nhật ký không chứa mã dùng một lần, token, khoá bí mật hay raw `before_json`/`after_json`. Cột `content` là câu mô tả từ `actionType` và `safeParams` đã che, chọn `vi`/`en` theo `Accept-Language`, thiếu thì `vi`; dữ liệu nhạy cảm được che nhất quán ở mọi dòng và cột xuất.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #40; [SoT: 03-dd/logic/F1_identity.md].

## Bổ sung đồng bộ schema (2026-10-22, phase 5, `DEC-2026-1022-identity-schema-sync`)

Các tiêu chí dưới đây mô tả hành vi mới hoặc đổi sau khi đồng bộ `02-bd/database/F1_identity.md`. F1-33 đã có trong RD; endpoint #53–55 và mã lỗi tương ứng đã được quy định ở DD F1 hiện hành. Khi lập test case, dùng hợp đồng DD hiện hành thay cho trạng thái "đề xuất" trong tiêu đề AC lịch sử.

### AC-33 — Tỉ lệ hoàn thành của lớp tính khi đọc, không đọc bảng riêng (BR-12)
- **Tiền điều kiện**: lớp giao 4 bài, có 2 học viên còn trong lớp (A đã Accepted 2 bài giao, B đã Accepted 4 bài giao) và 1 học viên đã rời (`left_at` khác NULL, đã Accepted 4 bài giao).
- **Hành động**: `GET /classes/{classId}/progress`.
- **Kết quả mong đợi**: Với hai học viên hoàn thành 50% và 100%, tỷ lệ lớp là 75%; học viên đã rời không tham gia phép tính. Lớp chưa giao bài hiện chưa có tỷ lệ. Kết quả phản ánh lần nộp mới khi đọc lại, không chờ tác vụ tổng hợp.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #29, BR-12; [SoT: 03-dd/logic/F1_identity.md].

### AC-34 — Feed hoạt động chỉ có sự kiện tức thời, không có "vắng bài" (BR-18 đã bỏ)
- **Tiền điều kiện**: lớp có học viên ở nhãn `ABSENT` (BR-10).
- **Hành động**: chờ qua mốc giờ job quét cũ (06:00) rồi `GET /instructor/dashboard/recent-activity?classId=`.
- **Kết quả mong đợi**: Nguồn hoạt động gần đây chỉ chứa những sự kiện đã xảy ra; nhãn Vắng bài được tính trong danh sách học viên, không tự sinh một dòng hoạt động giả.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #45, BR-18; [SoT: 03-dd/logic/F1_identity.md].

### AC-35 — Tài khoản do Admin tạo: ACTIVE ở lần đăng nhập đầu, buộc đổi mật khẩu mềm (api mục 0.6.1, #50; BR-21)
- **Tiền điều kiện**: Admin tạo tài khoản bằng `POST /admin/users`.
- **Hành động**: (1) ngay sau tạo, đọc `users`; (2) người dùng mới đăng nhập bằng mật khẩu tạm; (3) với access token vừa nhận, gọi một endpoint không liên quan tới mật khẩu (ví dụ `GET /me/profile`); (4) gọi `POST /me/password` (có `currentPassword` = mật khẩu tạm, mật khẩu mới đạt chuẩn).
- **Kết quả mong đợi**: Tài khoản do quản trị viên tạo được đăng nhập bằng mật khẩu tạm; lần đăng nhập đầu yêu cầu đổi mật khẩu trên giao diện và kích hoạt tài khoản. Luồng đổi là lời nhắc mềm: các thao tác được phép khác vẫn hoạt động. Sau lần đổi đầu, lời nhắc biến mất; tài khoản tự đăng ký và liên kết không nhận nhắc này.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #50; [SoT: 03-dd/logic/F1_identity.md].

### AC-36 — Xác thực email bằng OTP chuyển tài khoản tự đăng ký từ `PENDING_VERIFICATION` sang `ACTIVE` (#53)
- **Tiền điều kiện**: tài khoản **tự đăng ký** (`POST /auth/register`) đang `PENDING_VERIFICATION` (Sửa 2026-10-22 (đồng bộ DB vòng 2, DEC-2026-1022-db-sync-round2): OTP chỉ áp cho tự đăng ký), Redis có `identity:emailverify:<userId>` còn hạn (TTL 10 phút), chưa sai quá 5 lần.
- **Hành động**: `POST /auth/email/verify` với OTP đúng.
- **Kết quả mong đợi**: Mã xác thực email đúng kích hoạt tài khoản tự đăng ký; dùng lại cùng mã không kích hoạt thêm lần nữa.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #53; [SoT: 03-dd/logic/F1_identity.md].

### AC-37 — OTP xác thực email sai, hết hạn hoặc quá 5 lần (#53)
- **Tiền điều kiện**: tài khoản tự đăng ký `PENDING_VERIFICATION` có OTP đang hiệu lực.
- **Hành động**: gọi `POST /auth/email/verify` với OTP sai 5 lần liên tiếp, rồi lần thứ 6 với OTP đúng.
- **Kết quả mong đợi**: Mã sai hoặc hết hạn cho cùng thông báo để tránh tiết lộ lý do; sau năm lần sai, mã cũ không còn dùng được kể cả khi lần sau nhập đúng. Tài khoản vẫn chờ xác thực.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #53; [SoT: 03-dd/logic/F1_identity.md].

### AC-38 — Gửi lại OTP xác thực email có cooldown 60 giây, không dò email (#54)
- **Tiền điều kiện**: tài khoản tự đăng ký còn `PENDING_VERIFICATION`, có thể nhận email và đã qua cooldown gửi mã trước đó; một email không tồn tại và một tài khoản Admin tạo chưa đăng nhập được chuẩn bị để đối chiếu phản hồi.
- **Hành động**: gọi `POST /auth/email/verify/resend` hai lần trong 60 giây với email của tài khoản tự đăng ký `PENDING_VERIFICATION`; gọi thêm một lần với email không tồn tại và một lần với email của tài khoản Admin tạo chưa đăng nhập.
- **Kết quả mong đợi**: Lần gửi lại đầu tạo mã mới; yêu cầu tiếp theo trong 60 giây bị tạm chặn. Email không có tài khoản và tài khoản do quản trị viên tạo cho cùng phản hồi chung, không gửi mã đăng ký.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #54; [SoT: 03-dd/logic/F1_identity.md].

### AC-39 — `SubmissionGraded` cập nhật ngôn ngữ nộp nhiều nhất và đếm lượt nộp theo ngày (mục 2 của api)
- **Tiền điều kiện**: user chưa có dòng `user_submission_stats`.
- **Hành động**: phát ba event `SubmissionGraded` với `language` lần lượt `PYTHON`, `JAVA`, `PYTHON` trong cùng một ngày UTC+7 (`submittedAt` cùng ngày UTC+7); lượt thứ nhất `verdict = ACCEPTED` kèm `firstAttemptForProblem = true`, hai lượt sau `firstAttemptForProblem = false`. Sửa 2026-10-22 (đồng bộ DB vòng 2, DEC-2026-1022-db-sync-round2): payload nay có `submittedAt` và `firstAttemptForProblem`.
- **Kết quả mong đợi**: Với hai lượt Python và một lượt Java, ngôn ngữ dùng nhiều nhất là Python; lượt Accepted đầu tiên được đếm đúng một lần. Lượt nộp sau nửa đêm theo giờ Việt Nam thuộc ngày mới; xử lý lại cùng sự kiện không làm số liệu tăng lần hai.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] Sự kiện SubmissionGraded; [SoT: 03-dd/logic/F1_identity.md].

### AC-40 — Lưới hoạt động 12 tháng đọc từ `identity` (#55)
- **Tiền điều kiện**: user có dòng `user_daily_submission_stats` ở vài ngày trong 12 tháng gần nhất và một dòng cũ hơn 12 tháng; user khác có dữ liệu riêng.
- **Hành động**: `GET /me/activity-calendar` bằng token của user thứ nhất.
- **Kết quả mong đợi**: Lưới trả đúng 364 ngày liên tiếp theo giờ Việt Nam, cũ đến mới; ngày không có bài nộp là 0. Chỉ có hoạt động của chính người dùng; thiếu phiên hợp lệ thì không đọc được lưới.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #55; [SoT: 03-dd/logic/F1_identity.md].

### AC-41 — Tài khoản tự đăng ký chưa nhập OTP không đăng nhập được (BR-21 bước 3, `IDT-113`)
- **Tiền điều kiện** (Sửa 2026-10-22 (đồng bộ DB vòng 3, DEC-2026-1022-self-register-pending-login-blocked)): tài khoản **tự đăng ký** (`POST /auth/register`) đang `status = PENDING_VERIFICATION`, `must_change_password = false`; mật khẩu đúng; chưa nhập OTP email. Một tài khoản Admin tạo cùng trạng thái nhưng `must_change_password = true`.
- **Hành động**: (1) `POST /auth/login` và `POST /auth/instructor/login` bằng email và mật khẩu đúng của tài khoản tự đăng ký; (2) nhập OTP đúng qua `POST /auth/email/verify` rồi đăng nhập lại; (3) đăng nhập bằng mật khẩu tạm của tài khoản Admin tạo.
- **Kết quả mong đợi**: Tài khoản tự đăng ký chưa nhập mã email không được đăng nhập và không nhận phiên. Mật khẩu sai luôn cho thông báo đăng nhập sai thông thường, không tiết lộ trạng thái xác thực. Sau khi xác thực email, đăng nhập được.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #2; [SoT: 03-dd/logic/F1_identity.md].

### AC-42 — Không mở khoá được tài khoản tự xoá đã ẩn danh hoá (`ADM0201`, BR-08)
- **Tiền điều kiện** (Sửa 2026-10-22 (đồng bộ DB vòng 3, DEC-2026-1022-self-register-pending-login-blocked)): tài khoản `status = DEACTIVATED`, `deactivation_source = 'SELF'`, `anonymized_at` khác NULL; một tài khoản tự xoá còn trong ân hạn (`anonymized_at IS NULL`) để đối chiếu.
- **Hành động**: (1) mở danh sách người dùng của Admin (`GET /admin/users`); (2) gọi `PATCH /admin/users/{userId}/status` mở khoá cho cả hai tài khoản.
- **Kết quả mong đợi**: Tài khoản đã ẩn danh không thể mở khoá qua quản trị; tài khoản còn trong ân hạn và được phép khôi phục có thể trở lại hoạt động. Màn quản trị giải thích rõ khi một thao tác hàng loạt chỉ thành công một phần.

- **Oracle API chốt 2026-10-28**: #37 là request cho một user; mở lại người đã ẩn danh trả `422 IDT-448`, không thay user/phiên/audit thành công. Màn gom các response thành kết quả một phần.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #37; [SoT: 03-dd/logic/F1_identity.md].

### AC-43 — Xoá lớp phát `ClassDeleted`, dọn dữ liệu con, gỡ gắn lớp khỏi feed (BR-22, thêm 2026-10-22, đồng bộ DB vòng 4, `DEC-2026-1022-db-sync-round4`)
- **Tiền điều kiện**: giảng viên A sở hữu lớp L có mã mời còn hạn, ba học viên ghi danh (một người đã `left_at`), và hai dòng `identity_recent_activity` có `scope_class_id = L`.
- **Hành động**: A gọi `DELETE /classes/{L}`.
- **Kết quả mong đợi**: Xoá lớp làm lớp và mã mời biến khỏi các màn hiện hành; hoạt động cá nhân và bài nộp cũ vẫn xem được. Các phân hệ nhận đúng một lần thay đổi có ý nghĩa; giảng viên không sở hữu lớp không xoá được.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #22, ClassDeleted; [SoT: 03-dd/logic/F1_identity.md].

### AC-44 — Mặc định phỏng vấn giả lập lưu theo người dùng (BR-07, thêm 2026-10-22, đồng bộ DB vòng 4, ID-23)
- **Tiền điều kiện**: học viên chưa có dòng `user_preferences`.
- **Hành động**: (1) `GET /me/settings`; (2) `PATCH /me/settings` với `preferredMaxTurns = 16`, `preferredHintAllowed = false`; (3) `GET /me/settings` lại; (4) `PATCH` với `preferredMaxTurns = 10`.
- **Kết quả mong đợi**: Khi chưa có cài đặt, phiên phỏng vấn mặc định 12 lượt và cho gợi ý. Đổi sang 16 lượt và tắt gợi ý được lưu cho lần mở sau; số lượt ngoài ba giá trị cho phép bị từ chối mà không sửa cài đặt. Dịch vụ AI tạm lỗi không cản việc lưu mặc định này.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #14–15; [SoT: 03-dd/logic/F1_identity.md].

### AC-45 — Ma trận quyền mặc định có function `LANGUAGE_CONFIG` (BR-13, thêm 2026-10-23, `DEC-2026-1023-db-actor-review-fixes`)
- **Tiền điều kiện**: cơ sở dữ liệu mới tạo, vừa chạy seed; ma trận quyền gồm 11 function (thêm `LANGUAGE_CONFIG`, F4-11) và 3 role hệ thống.
- **Hành động**: (1) đếm các dòng `permissions` sau seed; (2) đọc các ô của function `LANGUAGE_CONFIG` ở ba role; (3) đăng nhập bằng ADMIN và mở màn cấu hình ngôn ngữ (`ADM0501`); (4) đăng nhập bằng INSTRUCTOR và mở cùng chức năng.
- **Kết quả mong đợi**: Ma trận mặc định có đủ ba vai trò, mười một chức năng và bốn hành động cho mỗi chức năng. Quản trị viên có quyền cấu hình ngôn ngữ. Cấp ô `LANGUAGE_CONFIG` cho học viên/giảng viên chỉ đổi ma trận; endpoint F4 vẫn bắt buộc vai trò ADMIN ở Lớp 1 và trả `403 JDG-301` cho người không phải ADMIN, kể cả có ô quyền. Quyền mới có hiệu lực sau khi lưu trong phạm vi vai trò được endpoint cho phép. [Đối chiếu DD F4 §23–24, BD ADM0501 Sheet 9 và security F4]

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #30–31; [SoT: 03-dd/logic/F1_identity.md].

### AC-46 — Tài khoản ADMIN đầu tiên được tạo một lần lúc khởi động và buộc đổi mật khẩu (BR-23, thêm 2026-10-23)
- **Tiền điều kiện**: cơ sở dữ liệu chưa có người dùng nào thuộc role `ADMIN`; hai biến môi trường `ALGOPREP_BOOTSTRAP_ADMIN_EMAIL`, `ALGOPREP_BOOTSTRAP_ADMIN_PASSWORD` đã đặt.
- **Hành động**: (1) khởi động ứng dụng, đọc `users`; (2) khởi động lại lần hai, lần này đổi giá trị biến mật khẩu; (3) đăng nhập bằng mật khẩu ở bước (1); (4) tách riêng: xoá hết ADMIN, bỏ một biến môi trường rồi khởi động.
- **Kết quả mong đợi**: Khi khởi động lần đầu với thông tin cấu hình đủ, hệ thống có đúng một quản trị viên ban đầu và yêu cầu đổi mật khẩu sau lần đăng nhập đầu. Khởi động lại không tạo người thứ hai hoặc đổi mật khẩu; thiếu thông tin cấu hình thì khởi động bị từ chối và không tạo tài khoản nửa chừng.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] Khởi động F1, BR-23; [SoT: 03-dd/logic/F1_identity.md].

### AC-47 — Job ẩn danh hoá bỏ qua tài khoản bị Admin khoá (BR-08, thêm 2026-10-23)
- **Tiền điều kiện**: hai tài khoản `status = DEACTIVATED` đều đã khoá quá 30 ngày và `anonymized_at IS NULL`: tài khoản X có `deactivation_source = 'SELF'`, tài khoản Y có `deactivation_source = 'ADMIN'`; X có liên kết đăng nhập OAuth và các trường tự khai (trường/công ty, vị trí hiện tại, vị trí mục tiêu).
- **Hành động**: chạy job ẩn danh hoá (hoặc chờ đến lịch chạy), sau đó chạy lần hai.
- **Kết quả mong đợi**: Tài khoản tự xoá quá hạn được ẩn danh đúng một lần; tài khoản bị quản trị viên khoá vẫn giữ dữ liệu và có thể mở lại. Chạy tác vụ lần nữa không thay đổi người đã ẩn danh.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] JOB ẩn danh, BR-08; [SoT: 03-dd/logic/F1_identity.md].

### AC-48 — Cài đặt ngôn ngữ nộp mặc định lưu trong tài khoản
- **Tiền điều kiện**: người học có `users.default_language = PYTHON`.
- **Hành động**: đọc #14, PATCH #15 `defaultLanguage = JAVA`, đọc lại; thử PATCH `defaultLanguage = RUBY`.
- **Kết quả mong đợi**: Cài đặt ngôn ngữ mặc định ban đầu là Python; đổi sang Java được lưu và đọc lại đúng. Ngôn ngữ không thuộc ba lựa chọn bị từ chối, giá trị đã lưu không đổi; các cài đặt khác không gửi kèm vẫn giữ nguyên.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #14–15; [SoT: 03-dd/logic/F1_identity.md].

### AC-49 — Danh sách lớp của tôi chỉ gồm ghi danh còn hiệu lực
- **Tiền điều kiện**: học viên A đang ở ba lớp, đã rời một lớp; học viên B ở một lớp khác.
- **Hành động**: A gọi #25A và #25B với từng `classId`, gồm lớp của B, lớp đã rời và UUID sai.
- **Kết quả mong đợi**: Danh sách chỉ gồm lớp người học còn tham gia, mới tham gia đứng trước. Lớp đã rời, lớp của người khác và mã lớp không tồn tại cho cùng trạng thái không tìm thấy khi xem chi tiết.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #25A–25B; [SoT: 03-dd/logic/F1_identity.md].

### AC-50 — Chặn dò mã mời sau năm lần sai trong 15 phút
- **Tiền điều kiện**: học viên có mã mời hợp lệ và chưa có bộ đếm Redis.
- **Hành động**: gửi năm mã sai, lần thứ sáu gửi mã hợp lệ, rồi thử lại sau khi cửa sổ hết hạn; lặp với một lần tham gia thành công.
- **Kết quả mong đợi**: Sau năm mã mời sai trong 15 phút, lần thử tiếp theo bị tạm chặn dù mã đúng; hết cửa sổ thì tham gia được. Hạn mức tính riêng cho tài khoản và được xoá sau một lần tham gia thành công.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #25; [SoT: 03-dd/logic/F1_identity.md].

### AC-51 — Tóm tắt lớp giảng viên nhất quán giữa Overview và Quản lý lớp
- **Tiền điều kiện**: giảng viên có ba lớp, một học viên thuộc hai lớp, một học viên đã rời; có bài giao và trạng thái giải.
- **Hành động**: gọi #56, so sánh các thẻ của `INS0101` và `INS0201`.
- **Kết quả mong đợi**: Số lớp là ba; người học ở hai lớp chỉ được tính một lần trong tổng học viên. Mỗi thẻ lớp có sĩ số và tỷ lệ hoàn thành đúng dữ liệu hiện hành. Nếu nguồn cần cho phép tính tạm lỗi, phần liên quan báo chưa có dữ liệu thay vì hiện 0.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #56; [SoT: 03-dd/logic/F1_identity.md].

### AC-52 — Xu hướng điểm chỉ có sáu tuần ISO đủ
- **Tiền điều kiện**: có bài chấm ở tuần đang chạy, sáu tuần đã đủ và một tuần đủ không có bài.
- **Hành động**: gọi #57 không `classId`, rồi với lớp thuộc mình, lớp người khác và UUID sai.
- **Kết quả mong đợi**: Xu hướng gồm đúng sáu tuần đã kết thúc theo thứ tự thời gian; tuần hiện tại không xuất hiện. Tuần không có bài chấm để trống thay vì hiện điểm 0. Bộ lọc lớp ngoài phạm vi không lộ dữ liệu lớp.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #57; [SoT: 03-dd/logic/F1_identity.md].

### AC-53 — Học viên cần chú ý theo đúng phạm vi tab và mức nghiêm trọng
- **Tiền điều kiện**: hai lớp có học viên vắng bài, giảm điểm, hoàn thành dưới 50%, một học viên đã rời.
- **Hành động**: gọi #58 cho tất cả và từng lớp, cả lớp không thuộc mình.
- **Kết quả mong đợi**: Danh sách tối đa năm học viên còn tham gia trong phạm vi lớp chọn. Vắng bài đứng trước Cần hỗ trợ rồi Theo dõi; trong từng nhãn xếp theo số ngày vắng giảm dần, mức giảm tỉ lệ Accepted lớn nhất trước, hoặc tỷ lệ hoàn thành thấp nhất trước tương ứng; hoà thì `studentId` tăng dần. Lý do được hiển thị từ mã và giá trị có nguồn. Lớp không thuộc giảng viên không lộ học viên.

- **Nguồn**: [SoT: 03-dd/api/F1_identity.md] #58; [SoT: 03-dd/logic/F1_identity.md].

### AC-54 — Xác minh mã quên mật khẩu trước bước đặt lại
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người dùng đã yêu cầu đặt lại mật khẩu và có mã còn hiệu lực; một mã khác đã hết hạn. |
| **Hành động** | Nhập mã ở bước xác minh rồi nhập mật khẩu mới; thử lại với mã hết hạn và mã đã dùng. |
| **Kết quả mong đợi** | Mã đúng cho phép tới bước nhập mật khẩu nhưng chưa đổi mật khẩu và chưa tiêu thụ mã. Bước đặt lại kiểm mã lần nữa rồi mới đổi mật khẩu; mã sai, hết hạn hoặc đã dùng không tạo phiên và không đổi mật khẩu. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md] #60, #8; BR-04 |

### AC-55 — Khôi phục tài khoản tự xoá chỉ trong thời gian ân hạn
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Tài khoản tự yêu cầu xoá còn trong hạn; tài khoản khác bị quản trị viên khoá hoặc đã ẩn danh. |
| **Hành động** | Xác minh lại thông tin đăng nhập, huỷ yêu cầu xoá và thử dùng lại vé khôi phục; lặp với hai tài khoản không hợp lệ. |
| **Kết quả mong đợi** | Chỉ tài khoản tự xoá còn hạn được khôi phục; vé dùng một lần. Người dùng phải đăng nhập lại và không nhận phiên từ thao tác khôi phục. Tài khoản bị quản trị viên khoá hoặc đã ẩn danh không được mở bằng luồng này. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md] #2, #6, #61; BR-08 |

### AC-56 — Xuất dữ liệu cá nhân đúng phạm vi và hạn mức
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người dùng có hồ sơ và bài nộp; tài khoản khác cũng có dữ liệu. |
| **Hành động** | Xuất từng loại dữ liệu, thử xuất lại cùng loại trước năm phút và thử truy cập dữ liệu tài khoản khác. |
| **Kết quả mong đợi** | Mỗi tệp chỉ chứa dữ liệu của người yêu cầu; lần xuất quá sớm của cùng loại bị từ chối kèm thời điểm thử lại. Xuất loại khác vẫn được; dữ liệu nhạy cảm không bị ghi vào trang hoặc tệp ngoài phạm vi. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md] #16 |

### AC-57 — Xuất hội thoại phỏng vấn suy giảm độc lập
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người dùng có transcript; dịch vụ phỏng vấn có thể tạm lỗi. |
| **Hành động** | Xuất transcript khi dịch vụ hoạt động, rồi lặp khi dịch vụ lỗi. |
| **Kết quả mong đợi** | Lần thành công tải được tệp của chính người dùng; lần lỗi báo có thể thử lại và không làm hỏng cài đặt hoặc khoá nút xuất vĩnh viễn. Không trả transcript của người khác. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md] #17 |

### AC-58 — Danh sách học viên giảng viên phân trang trên toàn bộ lớp
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Giảng viên có hai lớp, một học viên ở cả hai lớp, một học viên đã rời. |
| **Hành động** | Mở tab Tất cả, lọc từng lớp, tìm kiếm và đổi trang; thử mã lớp không thuộc mình. |
| **Kết quả mong đợi** | Tab Tất cả phân trang trên toàn bộ học viên còn thuộc các lớp được quản lý, không cắt theo từng lớp ở trình duyệt; bộ lọc lớp chỉ hiện lớp đã chọn. Học viên đã rời không bị tính là đang học; lớp ngoài phạm vi không lộ danh sách. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md] #59 |

### AC-59 — Các khối Tổng quan quản trị trả số liệu độc lập
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Có người dùng, bài nộp và bài toán ở nhiều ngôn ngữ, mức khó và thời điểm. |
| **Hành động** | Đọc lần lượt mười khối Tổng quan; cho một nguồn chéo module tạm lỗi rồi thử lại khối đó. |
| **Kết quả mong đợi** | Mỗi khối có đúng loại số liệu và khoảng thời gian của nó; lỗi một nguồn không biến số chưa biết thành số không ở khối liên quan và không che các khối khác. Mức khó lấy từ danh mục hiện hành; bài phổ biến chỉ gồm bài đang xuất bản. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md] #41–43, #67–73; BR-17 |

### AC-60 — Tỷ lệ quản trị không chia trên tập rỗng
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Không có lượt nộp nào có kết quả cuối hoặc một mức khó chưa có bài nộp. |
| **Hành động** | Đọc các thẻ và biểu đồ kết quả. |
| **Kết quả mong đợi** | Màn nhận giá trị chưa có dữ liệu cho tỷ lệ không tính được; các số đếm còn lại vẫn đúng. Không có tỷ lệ giả từ dữ liệu mẫu, không chia cho không và không cộng lỗi hệ thống vào mẫu số tỷ lệ chấp thuận. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md] #43, #68–69 |

### AC-61 — Thống kê quản lý người dùng không suy từ trang hiện tại
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Có nhiều trang người dùng; tài khoản chờ xác thực và các vai trò nằm ở trang sau. |
| **Hành động** | Đọc danh sách trang đầu, số chờ xác thực và phân bố vai trò. |
| **Kết quả mong đợi** | Hai số tổng hợp phản ánh toàn bộ tập dữ liệu trong phạm vi quản trị, không chỉ trang hiện tại. Một người chỉ nằm trong đúng một nhóm vai trò; người không có quyền quản lý người dùng không đọc được. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md] #35, #62–63 |

### AC-62 — Nhật ký phụ trợ và luồng mới theo cùng quyền đọc
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Có nhật ký của nhiều quản trị viên ở bốn nhóm hành động. |
| **Hành động** | Đọc năm người hoạt động gần nhất, phân bố nhóm và nhận dòng nhật ký mới; thử bằng tài khoản không phải quản trị viên. |
| **Kết quả mong đợi** | Quản trị viên xem được các khối đọc dù không có quyền sửa cấu hình nhật ký. Dòng mới không nhân đôi khi nối lại; tài khoản ngoài khu quản trị không nhận dữ liệu nhật ký. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md] #64–66 |

### AC-63 — Đổi chính sách lưu nhật ký có xác nhận và ghi dấu
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Đang có chính sách lưu nhật ký; quản trị viên có hoặc không có quyền sửa. |
| **Hành động** | Đọc chính sách, thử giá trị ngày không hợp lệ, đổi sang chính sách hợp lệ rồi đọc lại. |
| **Kết quả mong đợi** | Người có quyền đọc thấy cấu hình hiện hành. Giá trị không hợp lệ bị từ chối, dữ liệu cũ còn nguyên. Thay đổi hợp lệ có hiệu lực và xuất hiện trong nhật ký cùng giá trị trước, sau; người chỉ có quyền đọc không sửa được. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md] #51–52 |

### AC-64 — Đăng nhập giảng viên chỉ dành cho tài khoản đúng vai trò
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Có tài khoản học viên, giảng viên và quản trị viên với thông tin đăng nhập hợp lệ. |
| **Hành động** | Dùng từng tài khoản ở trang đăng nhập giảng viên; thử mật khẩu sai nhiều lần. |
| **Kết quả mong đợi** | Chỉ giảng viên nhận phiên ở khu giảng viên. Vai trò khác bị từ chối ở đường này nhưng vẫn có thể dùng đường đăng nhập phù hợp; giới hạn chống dò mật khẩu áp như đăng nhập thường. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md] #49 |

### AC-65 — Tạo và sửa lớp chỉ trong phạm vi giảng viên sở hữu
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Giảng viên A có lớp của mình; giảng viên B có lớp khác. |
| **Hành động** | A tạo lớp kèm lịch học, sửa lớp của mình rồi thử sửa lớp của B. |
| **Kết quả mong đợi** | Lớp mới và mã mời đầu tiên cùng xuất hiện; lịch học lưu đúng cấu trúc đã nhập. Sửa lớp của mình có hiệu lực; lớp của người khác không bị lộ hoặc sửa qua yêu cầu trực tiếp. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md] #19–21 |

### AC-66 — Xoá lớp giữ lịch sử học tập và phát thay đổi
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Một lớp có học viên, mã mời và bài đã giao. |
| **Hành động** | Giảng viên sở hữu xác nhận xoá lớp; thử lại cùng yêu cầu. |
| **Kết quả mong đợi** | Lớp biến khỏi danh sách và mã mời không dùng lại được; lịch sử bài nộp của học viên còn xem được. Các nơi phụ thuộc được cập nhật bởi sự kiện xoá lớp; lặp lại không phát thêm một lần xoá giả. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md] #22; BR-22 |

### AC-67 — Mã mời mới và danh sách mã còn hạn
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Giảng viên sở hữu một lớp và đang có mã mời chưa hết hạn. |
| **Hành động** | Tạo mã mời mới, đọc danh sách rồi thử bằng giảng viên lớp khác. |
| **Kết quả mong đợi** | Mã mới có hạn dùng bảy ngày; mã cũ còn hạn vẫn hoạt động song song. Danh sách chỉ gồm mã còn hiệu lực của lớp đó; người không sở hữu lớp không đọc hoặc tạo mã. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md] #23–24 |

### AC-68 — Đăng nhập liên kết hợp lệ và callback sai
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người dùng có tài khoản phù hợp hoặc là người mới; nhà cung cấp xác thực có thể trả về trạng thái sai. |
| **Hành động** | Bắt đầu đăng nhập liên kết, hoàn tất callback hợp lệ rồi thử callback sai hoặc dùng lại. |
| **Kết quả mong đợi** | Callback hợp lệ liên kết hoặc tạo đúng một tài khoản và mở phiên; callback sai, dùng lại hoặc không khớp trạng thái bị từ chối mà không tạo phiên giả hay tài khoản trùng. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md] #5–6; BR-03 |

### AC-69 — Đăng xuất thu hồi phiên hiện tại
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người dùng đang có phiên hợp lệ và một thiết bị khác cũng đã đăng nhập. |
| **Hành động** | Đăng xuất ở một thiết bị rồi thử làm mới phiên từ thiết bị đó; kiểm thiết bị còn lại. |
| **Kết quả mong đợi** | Phiên của thiết bị đã đăng xuất không cấp lại quyền; thiết bị kia vẫn hoạt động theo chính sách phiên hiện hành. Gọi đăng xuất lặp lại không tạo lỗi làm lộ thông tin phiên. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md] #4; BR-02 |

### AC-70 — Đổi email và gửi lại cùng một hạn mức
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người dùng đã yêu cầu đổi email và vừa nhận mã; giới hạn là ba yêu cầu trong 24 giờ. |
| **Hành động** | Bấm gửi lại mã cho tới khi chạm hạn mức, thử xác nhận mã cũ và mã mới. |
| **Kết quả mong đợi** | Mỗi lần gửi lại được tính vào cùng hạn mức; mã trước bị vô hiệu khi có mã mới. Chạm hạn mức không gửi thêm thư và email đăng nhập không đổi. Chỉ mã mới hợp lệ mới đổi email. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md] #11–12 |

### AC-71 — Đọc hồ sơ chỉ trả dữ liệu của chính mình
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Hai tài khoản có tên, email và phương thức đăng nhập khác nhau. |
| **Hành động** | Mỗi tài khoản mở Hồ sơ, rồi thử dùng mã người khác trong yêu cầu tự dựng. |
| **Kết quả mong đợi** | Mỗi người chỉ nhận hồ sơ và trạng thái mật khẩu của chính mình; không thể chọn chủ hồ sơ khác qua tham số. Dữ liệu bí mật và mật khẩu không xuất hiện trong phản hồi. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md] #9 |

### AC-72 — Danh sách học viên và tiến độ lớp giữ đúng phạm vi
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Giảng viên có hai lớp, một học viên đã rời và một lớp của giảng viên khác. |
| **Hành động** | Xem học viên từng lớp, tiến độ lớp và chi tiết lớp; thử mã lớp ngoài phạm vi. |
| **Kết quả mong đợi** | Chỉ học viên còn thuộc lớp được quản lý xuất hiện trong danh sách hiện tại; tiến độ lớp tính theo bài giao đúng thời gian. Lớp ngoài phạm vi cho cùng phản hồi không tìm thấy như mã lớp không tồn tại. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md] #19, #26, #28–29; BR-10–12 |

### AC-73 — Ma trận quyền và vai trò được kiểm ở máy chủ
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Có vai trò hệ thống và vai trò tuỳ biến; người gọi có hoặc thiếu quyền quản trị quyền. |
| **Hành động** | Đọc ma trận, đọc vai trò, tạo một vai trò hợp lệ rồi thử đổi ô quyền khi không có quyền. |
| **Kết quả mong đợi** | Danh sách phản ánh vai trò hiện hành; tạo vai trò mới không đổi quyền của vai trò khác. Người thiếu quyền cập nhật bị từ chối ngay cả khi tự gửi yêu cầu, không chỉ bị ẩn nút trên màn. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md] #30–33; BR-13–14 |

### AC-74 — Danh sách người dùng và đặt lại mật khẩu hộ đúng phạm vi
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Có người dùng nhiều trang, một tài khoản chỉ đăng nhập liên kết và một tài khoản có mật khẩu. |
| **Hành động** | Quản trị viên lọc danh sách, đặt lại mật khẩu cho từng loại và thử bằng tài khoản không có quyền. |
| **Kết quả mong đợi** | Bộ lọc và phân trang thực hiện trên toàn bộ người dùng; người không có quyền không đọc hoặc thay đổi. Tài khoản có mật khẩu nhận hướng dẫn đặt lại an toàn và các phiên cũ bị thu hồi; tài khoản chỉ liên kết được hướng dẫn qua nhà cung cấp, không nhận mật khẩu tạm. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md] #35, #38 |

### AC-75 — Tiến độ cá nhân và hoạt động gần đây giữ cùng quy tắc đếm
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người dùng có bài nộp Accepted, sai, lỗi hệ thống và ngày không nộp. |
| **Hành động** | Đọc tóm tắt tiến độ và hoạt động gần đây (trạng thái giải theo bài xem AC-80). |
| **Kết quả mong đợi** | Số bài đã giải theo kết quả tốt nhất; tỷ lệ chấp thuận loại lượt lỗi hệ thống khỏi mẫu số, còn tổng lượt vẫn thể hiện đúng. Hoạt động mới nhất theo thời gian thật và chỉ của chính người đó. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md] #46–48 |

### AC-76 — Đổi vai trò không làm mất người quản trị cuối
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Có hai quản trị viên và người dùng ở vai trò khác. |
| **Hành động** | Quản trị viên có quyền đổi vai trò một người, rồi thử đổi vai trò người quản trị cuối và tự đổi vai trò mình. |
| **Kết quả mong đợi** | Thay đổi hợp lệ có hiệu lực và ghi nhật ký; thay đổi khiến hệ thống mất người quản trị cuối bị từ chối. Yêu cầu trực tiếp không có quyền cũng bị từ chối. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md] #36; BR-15 |

### AC-77 — Hợp đồng tổng quan giảng viên cũ không thay màn mới
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Giảng viên có nhiều lớp và có khách hàng cũ còn gọi bản tóm tắt trước đây. |
| **Hành động** | Đọc tóm tắt cũ và mở Overview giảng viên hiện hành. |
| **Kết quả mong đợi** | Đường đọc cũ vẫn trả theo hợp đồng được giữ; Overview hiện hành lấy số liệu từ hợp đồng mới và không trộn hai cách đếm. Người ngoài phạm vi giảng viên không đọc được số liệu lớp. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md] #44, #56 |
| **Cập nhật 2026-11-07** | Owner chốt bỏ #44, thay bởi #56 (tiêu chí ponytail bậc 4; DD `03-dd/api/F1_identity.md` dòng #44 gạch ngang). Tiêu chí còn hiệu lực ở phần Overview hiện hành đọc #56 và người ngoài phạm vi giảng viên không đọc được; vế "đường đọc cũ vẫn trả" ở trên không còn áp dụng. Văn bản gốc giữ nguyên để truy vết. |

### AC-78 — Tìm và xuất nhật ký giữ cùng bộ lọc
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Có nhật ký thuộc nhiều nhóm, người thực hiện và ngày khác nhau. |
| **Hành động** | Lọc theo nhóm, người và khoảng ngày; tải trang kế rồi xuất dữ liệu theo cùng bộ lọc. |
| **Kết quả mong đợi** | Trang kế không lặp hoặc mất dòng; đổi bộ lọc bắt đầu lại từ trang đầu. Tệp xuất chứa đúng tập bản ghi lọc và chỉ các cột đã chọn, không có mã xác thực hoặc bí mật. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md] #39–40 |

### AC-79 — Thao tác quản trị một người dùng sai dữ liệu hoặc không gửi được email thì không đổi gì (task #11)
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Quản trị viên có quyền quản lý người dùng; có một tài khoản đang hoạt động, một tài khoản đã ẩn danh hoá, và dịch vụ gửi email có thể lỗi. |
| **Hành động** | (1) Gọi danh sách với bộ lọc hoặc phân trang sai. (2) Đổi vai trò hoặc khoá với `userId` không có, vai trò đích không có, trạng thái lạ, lý do quá dài hoặc bật gửi email mà thiếu lý do. (3) Đặt mật khẩu tạm cho tài khoản đã ẩn danh. (4) Tạo tài khoản hoặc đặt mật khẩu tạm khi gửi email lỗi. |
| **Kết quả mong đợi** | (1) `400 IDT-V17`. (2) `404 IDT-450`, `404 IDT-424` hoặc `422 IDT-V26`, không đổi người dùng, không ghi audit, không gửi email. (3) `422 IDT-448`. (4) Tài khoản không được tạo hoặc mật khẩu cũ vẫn dùng được, không có dòng audit; mật khẩu tạm không xuất hiện trong phản hồi, nhật ký hay audit. Chọn đúng vai trò hoặc trạng thái đang có thì thành công và không ghi audit. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md] #35–38, #50 (khối "Triển khai 2026-11-02"); [SoT: 03-dd/validation/F1_identity.md] mục 14; `DEC-2026-1102-admin-user-management-contracts` |

### AC-80 — Trạng thái giải theo bài chỉ trả đúng bài được hỏi của chính người gọi
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người dùng có bài đã giải, bài đang làm (kết quả tốt nhất chưa Accepted) và bài chưa nộp; người khác có dữ liệu riêng cho cùng bài. |
| **Hành động** | Đọc trạng thái giải của một danh sách bài (tối đa 100 id), rồi thử danh sách rỗng, sai định dạng hoặc quá 100 id. |
| **Kết quả mong đợi** | Chỉ trả các bài được hỏi mà người gọi đã nộp, mỗi bài là Đã giải hoặc Đang làm; bài chưa nộp không có mặt; bài không được hỏi và dữ liệu người khác không bị trả. Danh sách thiếu, rỗng, sai định dạng hoặc quá 100 id bị từ chối 422 `IDT-V27` và không đọc dữ liệu. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md] #48, `IDT-V27`; owner chốt 2026-11-05 |

### AC-81 — Giao bài cho lớp ghi một dòng feed lớp, giao lại cùng sự kiện không nhân đôi (thêm 2026-11-07, F1-12)
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Giảng viên giao một bài cho lớp của mình; hệ thống nhận sự kiện "bài được giao cho lớp" từ ngân hàng bài. Có thể gặp sự kiện bị gửi lặp, sự kiện của lớp đã xoá, hoặc tin hỏng. |
| **Hành động** | Hệ thống xử lý sự kiện; gửi lại đúng sự kiện đó; gửi sự kiện cho lớp không còn tồn tại; gửi một tin hỏng rồi một sự kiện hợp lệ. |
| **Kết quả mong đợi** | Feed của lớp có đúng một dòng loại "giảng viên giao bài", gắn đúng lớp, đúng giảng viên, đúng bài và giờ xảy ra thật của sự kiện; gửi lại cùng sự kiện không thêm dòng. Sự kiện của lớp đã xoá được tiêu thụ nhưng không ghi gì. Tin hỏng bị bỏ, các sự kiện sau vẫn được xử lý bình thường. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md:492] (khối "Triển khai 2026-11-07, consumer `ProblemAssignedToClass`"); `05-coding/backend/algoprep-bootstrap/src/test/java/com/algoprep/bootstrap/security/ProblemAssignedConsumerFlowTest.java:152`; `06-plan/reports/261107-0100-f1-12-f1-8/report.md:44` |

### AC-82 — Bài nộp Accepted ghi dòng feed vào đúng lớp đã được giao bài đó (thêm 2026-11-07)
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Học viên đang ở một lớp được giao bài vào lúc nộp; có học viên khác chỉ ở lớp được giao bài khác hoặc vào lớp sau thời điểm đó; có thể gặp sự kiện gửi lặp hoặc tin hỏng. |
| **Hành động** | Hệ thống nhận sự kiện "nộp bài Accepted" của từng học viên; gửi lặp cùng sự kiện; gửi một tin hỏng. |
| **Kết quả mong đợi** | Học viên ở lớp được giao bài có đúng một dòng "nộp bài đạt" gắn đúng lớp, đúng bài nộp, giờ xảy ra thật; gửi lặp không thêm dòng. Học viên không thuộc lớp nào được giao bài đó vẫn có dòng trong lịch sử cá nhân nhưng không gắn lớp, nên giảng viên không thấy. Tin hỏng bị bỏ, không chặn các tin sau. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md:487-490] (consumer `SubmissionAccepted`, chọn lớp theo `DEC-2026-1103-f1-accepted-event-class-resolution`); `SubmissionAcceptedConsumerFlowTest.java:197`, `:223`, `:246` |

### AC-83 — Một nguồn số liệu lỗi chỉ làm hỏng đúng khối Tổng quan quản trị đó, mã IDT-506 (thêm 2026-11-07, F1-8)
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Quản trị viên mở Tổng quan; một trong các nguồn (bài nộp hoặc ngân hàng bài) không đọc được hoặc quá hạn. |
| **Hành động** | Tải lần lượt các khối của Tổng quan. |
| **Kết quả mong đợi** | Khối phụ thuộc nguồn lỗi trả 503 mã `IDT-506`, thân lỗi không để lộ tên bảng hay chi tiết nguồn; các khối chỉ dùng nguồn còn lại vẫn trả 200 với số liệu thật. Không khối nào thay lỗi bằng số 0. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md:159] `IDT-506`, [SoT: 03-dd/api/F1_identity.md:376-381]; `AdminSubmissionDashboardFlowTest.java:231`; `GetAdminSubmissionBlocksQueryHandlerTest.java:100` |

### AC-84 — Số liệu theo ngày và theo tháng ở Tổng quan quản trị tính theo giờ UTC+7 trong đúng cửa sổ (thêm 2026-11-07, F1-8)
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Có bài nộp ở nhiều thời điểm: vài phút trước, ba ngày trước và rất cũ; có lượt chưa có kết luận hoặc lỗi hệ thống. Một lần thử khác chưa có bài nộp nào. |
| **Hành động** | Đọc các khối theo ngày (bảy ngày), theo tháng (sáu tháng), theo ngôn ngữ, phân bố kết quả; đọc khi chưa có bài nộp. |
| **Kết quả mong đợi** | Cột cuối của khối theo ngày là ngày hôm nay theo UTC+7, tháng cuối của khối theo tháng là tháng hiện tại theo UTC+7; mỗi khối chỉ đếm trong cửa sổ của nó. Phân bố kết quả chỉ tính kết luận cuối (không tính lượt chờ hay lỗi hệ thống). Khi chưa có bài nộp, mọi khối trả hình dạng rỗng đúng (đủ cột, giá trị 0, danh sách rỗng), không bịa số. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md:376-381]; `AdminSubmissionDashboardFlowTest.java:137`, `:111` |

### AC-85 — Số liệu lớp của giảng viên lỗi nguồn thì 503 IDT-507, riêng danh sách học viên vẫn 200 (thêm 2026-11-07, F1-7)
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Giảng viên có lớp; nguồn bài nộp hoặc danh sách bài giao không đọc được (hết hạn hoặc lỗi). |
| **Hành động** | Đọc hồ sơ học viên trong lớp, tiến độ lớp, tóm tắt các lớp và danh sách học viên của lớp. |
| **Kết quả mong đợi** | Hồ sơ học viên, tiến độ lớp và tóm tắt lớp trả 503 mã `IDT-507`, thân lỗi không có chi tiết nguồn, không thay bằng số 0. Danh sách học viên của lớp vẫn trả 200 với đủ học viên, các trường trạng thái, điểm TB, tỷ lệ hoàn thành và lần nộp cuối là rỗng để giảng viên còn quản lý và gỡ học viên. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md:112], [SoT: 03-dd/api/F1_identity.md:160] `IDT-507`; `InstructorClassMetricsFlowTest.java:327`; `ClassMetricsHandlersTest.java:124`, `:135`, `:146` |

### AC-86 — Nhãn "Cần hỗ trợ" theo điểm giảm hai tuần liên tiếp, tuần thiếu bài nộp cắt chuỗi (thêm 2026-11-07, F1-7, owner chốt A1)
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Học viên có bài nộp gần đây, chưa vắng quá 7 ngày; điểm các tuần đủ gần nhất của học viên lần lượt giảm, đi ngang, hoặc có tuần không có bài nộp hợp lệ. |
| **Hành động** | Hệ thống phân loại học viên của lớp. |
| **Kết quả mong đợi** | Điểm giảm chặt chẽ ở ít nhất hai tuần liên tiếp tính đến tuần đủ gần nhất (một lần giảm giữa hai tuần liền nhau là đủ) thì học viên có nhãn "Cần hỗ trợ", kèm số tuần của chuỗi giảm và mức giảm. Hai tuần bằng nhau, tăng, hoặc chỉ có một tuần thì không phải suy giảm. Tuần không có bài nộp hợp lệ cắt chuỗi, không coi là 0. Nhãn "Vắng bài" thắng "Cần hỗ trợ", "Cần hỗ trợ" thắng "Cần chú ý". |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md:312] (Owner chốt 2026-11-07, A1), [SoT: 03-dd/api/F1_identity.md:295]; `ClassProgressRulesTest.java:66`, `:106`; `InstructorClassMetricsFlowTest.java:187` |

### AC-87 — Danh sách người dùng quản trị có thêm số bài đã giải và tổng lượt nộp, 0 khi chưa có dữ liệu (thêm 2026-11-07, F1-4)
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Có tài khoản đã giải hai bài với bảy lượt nộp, tài khoản chưa có dữ liệu thống kê nào, và tài khoản đã đăng nhập hoặc chưa từng đăng nhập. |
| **Hành động** | Quản trị viên đọc danh sách người dùng. |
| **Kết quả mong đợi** | Mỗi dòng có số bài đã giải (chỉ tính bài có kết quả tốt nhất là Accepted) và tổng lượt nộp; tài khoản chưa có dữ liệu hiện 0 cho cả hai, không để trống. Cột hoạt động gần nhất có giờ đăng nhập gần nhất với tài khoản từng đăng nhập và rỗng với tài khoản chưa từng đăng nhập. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md:191] (#35); `AdminUserManagementFlowTest.java:197`, `:229`; `06-plan/reports/261107-0300-f1-7-f1-4/report.md` |

### AC-88 — Xuất bài nộp bị cắt ở 2000 dòng và mỗi loại tệp chỉ xuất một lần mỗi 5 phút (thêm 2026-11-07, F1-9)
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Học viên có hơn 2000 lượt nộp; học viên vừa xuất tệp bài nộp. |
| **Hành động** | Xuất tệp bài nộp; xuất lần hai trong vòng 5 phút; xuất tệp hồ sơ ngay sau đó. |
| **Kết quả mong đợi** | Tệp có đúng 2000 dòng dữ liệu (bỏ các dòng cũ nhất) cùng dòng tiêu đề, phản hồi có header `X-Export-Truncated: true` để người dùng biết bị cắt. Lần xuất bài nộp thứ hai trong 5 phút bị từ chối 429 `IDT-V06`; tệp hồ sơ có hạn mức riêng nên vẫn xuất được. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md:66] (#16); `SubmissionExportFlowTest.java:151`, `:162` |

### AC-89 — Xuất bài nộp lỗi nguồn thì 503 IDT-508 và trả lại lượt xuất (thêm 2026-11-07, F1-9, owner chốt B7)
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Học viên có hạn mức xuất còn trống; lịch sử nộp bài không đọc được hoặc quá 20 giây. |
| **Hành động** | Xuất tệp bài nộp, rồi thử lại ngay khi nguồn đã hồi phục. |
| **Kết quả mong đợi** | Lần đầu trả 503 mã `IDT-508`, thân lỗi không có chi tiết nguồn. Hạn mức được trả lại nên lần thử lại ngay vẫn xuất được, không bị 429. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md:161] `IDT-508`; `SubmissionExportFlowTest.java:184`; `ExportMySubmissionsQueryHandlerTest.java:49` |

### AC-90 — Báo cáo Solution Review hoàn tất ghi một dòng feed cá nhân, giao lại không nhân đôi (thêm 2026-11-10, F5-5a)
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Học viên vừa nhận một báo cáo Solution Review; hệ thống nhận sự kiện "báo cáo hoàn tất" từ phân hệ AI. Có thể gặp sự kiện bị gửi lặp hoặc tin hỏng. |
| **Hành động** | Hệ thống xử lý sự kiện; gửi lại đúng sự kiện đó (cùng mã sự kiện); gửi một tin hỏng rồi một sự kiện hợp lệ. |
| **Kết quả mong đợi** | Feed hoạt động có đúng một dòng loại "báo cáo Solution Review hoàn tất", của đúng học viên, không gắn lớp nào, trỏ tới đúng báo cáo (`ref_type = SOLUTION_REVIEW`, `ref_id` là mã báo cáo); gửi lại cùng sự kiện không thêm dòng; tin hỏng bị bỏ và các sự kiện sau vẫn được xử lý. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md:475] (bảng sự kiện tiêu thụ, dòng `SolutionReviewCompleted`); [SoT: 03-dd/api/F5_ai-review.md] mục 33; `05-coding/backend/algoprep-bootstrap/src/test/java/com/algoprep/bootstrap/security/SolutionReviewCompletedConsumerFlowTest.java:146`; `SolutionReviewCompletedConsumerFlowTest.java:170`; `06-plan/reports/261110-2045-f5-5a-outbox-events/report.md`; `DEC-2026-1110-f5-outbox-event-decisions` |

### AC-91 — Phiên Mock Interview kết thúc ghi một dòng feed cá nhân, giao lại không nhân đôi (thêm 2026-11-11, F5-8)
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Học viên vừa kết thúc một phiên Mock Interview có điểm; hệ thống nhận sự kiện "phiên phỏng vấn kết thúc" từ phân hệ AI. Có thể gặp sự kiện bị gửi lặp hoặc tin hỏng. |
| **Hành động** | Hệ thống xử lý sự kiện; gửi lại đúng sự kiện đó; gửi một tin hỏng rồi một sự kiện hợp lệ. |
| **Kết quả mong đợi** | Feed hoạt động có đúng một dòng loại "phỏng vấn thử kết thúc", của đúng học viên, không gắn lớp, trỏ tới đúng phiên (`ref_type = INTERVIEW_SESSION`, `ref_id` là mã phiên); gửi lại không thêm dòng; tin hỏng bị bỏ và các sự kiện sau vẫn được xử lý. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md:476] (dòng `MockInterviewFinished`); [SoT: 03-dd/api/F5_ai-review.md] mục 33; `MockInterviewFinishedConsumerFlowTest`; `AiOutboxFlowTest.aFinishedInterviewReachesTheIdentityFeedOnce...`; `DEC-2026-1110-f5-outbox-event-decisions` |

## 3. Truy vết

Bảng 1: Truy vết tiêu chí nghiệm thu sang endpoint, test case QA và test mã nguồn của module F1. Cột "Test case QA" giữ ID test case do `testcase-generation` sinh (tài liệu QA, chưa chạy); cột "Test mã nguồn" liệt kê test thật đã kiểm bằng grep, theo dạng `Lớp:dòng` (dòng của phương thức test, tên lớp là duy nhất trong cây nên không ghi đường dẫn); "Trạng thái" ghi bằng chữ theo bảng 2. Cập nhật 2026-11-07, chỉ sửa tài liệu: không chạy Maven hay pnpm khi cập nhật bảng này.

Quy ước đường dẫn: lớp flow nằm trong `05-coding/backend/algoprep-bootstrap/src/test/java/com/algoprep/bootstrap/security/` (riêng `SubmissionExportBridgeTest` ở `.../bootstrap/identity/`, `IdentityMigrationTest` ở `.../bootstrap/migration/`); lớp unit nằm trong `05-coding/backend/algoprep-identity/src/test/java/com/algoprep/identity/`. Lớp không kèm số dòng là lớp unit gồm nhiều ca cùng kiểm hành vi. Test frontend (`real-*.test.ts`) kiểm ánh xạ hợp đồng, không dùng làm bằng chứng nghiệm thu cho AC nào.

Bảng 2: Ý nghĩa trạng thái truy vết (cập nhật 2026-11-07)

| Trạng thái | Nghĩa |
| :--- | :--- |
| Đạt | Có test khẳng định hành vi của AC và lớp test đó xanh trong full regression 2026-11-06 hoặc 2026-11-08 (hoặc xanh ở lượt chạy lại sau khi sửa, ghi ở cột Ghi chú). Mutation chỉ tính cho các quy tắc đã chọn, xem ghi chú dưới bảng 3 |
| Code xong, chưa test toàn bộ | Từ 2026-11-08 không còn AC nào ở trạng thái này: 21 AC của các lát F1-12, F1-8, F1-7, F1-4, F1-9 và AC-15 đã xanh trong full regression 2026-11-08 và được chuyển sang Đạt. Giữ dòng để trạng thái dùng lại được cho lát mới |
| Một phần | Có test nhưng chưa khẳng định hết AC; phần thiếu ghi ở cột Ghi chú |
| Chưa làm | Hành vi chưa có code. Hiện không có AC nào |
| Hoãn | Chủ ý hoãn. Hiện không còn AC nào: AC-57 (xuất hội thoại phỏng vấn, #17) đã Đạt từ F5-9 ngày 2026-11-11 (`DEC-2026-1108-f1-f2-closing-decisions` là gốc của việc hoãn) |

Mutation test của các lát quyền, riêng tư (F1-7, F1-8, F1-9, F1-12) và kiểm trình duyệt thật chưa làm: để bước FINAL theo `DEC-2026-1106-f1-f2-batch-test-workflow`. Mutation 3 ca của F1-1 (`06-plan/reports/261105-1530-f1-my-progress/report.md:32`) và bộ mutation task #11 (`05-coding/backend/logs/261102-mutation-t11-summary.md`) là của các lát đó, không mở rộng cho toàn bộ AC. Cột "Test case QA" không phản ánh việc chạy test case QA: toàn bộ test case QA của F1 vẫn ở trạng thái chưa chạy (AC-79 đã viết 2026-11-02, AC-80 chưa có test case QA).

| AC | Endpoint / luồng DD | Test case QA | Test mã nguồn | Trạng thái | Ghi chú |
| :--- | :--- | :--- | :--- | :--- | :--- |
| AC-01 | #1 | [TC-F1-AC01-01](../testcases/modules/F1_identity/F1_auth_session.md#tc-f1-ac01-01--đăng-ký-tạo-tài-khoản-chờ-xác-thực), [TC-F1-AC01-02](../testcases/modules/F1_identity/F1_auth_failure_boundaries.md), [TC-F1-AC01-03](../testcases/modules/F1_identity/F1_auth_failure_boundaries.md), [TC-F1-AC01-04](../testcases/modules/F1_identity/F1_auth_failure_boundaries.md) | RegistrationFlowTest:108; RegistrationFlowTest:370; RegistrationValidationTest | Đạt |  |
| AC-02 | #1 | [TC-F1-AC02-01](../testcases/modules/F1_identity/F1_auth_session.md#tc-f1-ac02-01--đăng-ký-trùng-email-khác-chữ-hoa) | RegistrationFlowTest:145 | Đạt |  |
| AC-03 | #2 | [TC-F1-AC03-01](../testcases/modules/F1_identity/F1_auth_session.md#tc-f1-ac03-01--đăng-nhập-đúng-tạo-phiên-an-toàn) | AuthSessionFlowTest:70; AuthSessionFlowTest:93 | Đạt |  |
| AC-04 | #2, BR-01 | [TC-F1-AC04-01](../testcases/modules/F1_identity/F1_auth_session.md#tc-f1-ac04-01--lần-sai-thứ-năm-và-lần-thử-kế-tiếp-trong-cửa-sổ), [TC-F1-AC04-02](../testcases/modules/F1_identity/F1_auth_session.md#tc-f1-ac04-02--phạm-vi-ip-và-cửa-sổ-hết-hạn), [TC-F1-AC04-03](../testcases/modules/F1_identity/F1_auth_session.md#tc-f1-ac04-03--redis-rate-limit-lỗi-vẫn-cho-kiểm-mật-khẩu), [TC-F1-AC04-04](../testcases/modules/F1_identity/F1_auth_failure_boundaries.md), [TC-F1-AC04-05](../testcases/modules/F1_identity/F1_auth_failure_boundaries.md) | LoginInfrastructureTest:93; LoginInfrastructureTest:115; LoginInfrastructureTest:131; LoginInfrastructureTest:170; LoginRateLimitTest | Đạt |  |
| AC-05 | #3, BR-02 | [TC-F1-AC05-01](../testcases/modules/F1_identity/F1_auth_session.md#tc-f1-ac05-01--refresh-xoay-vòng-một-lần), [TC-F1-AC05-02](../testcases/modules/F1_identity/F1_auth_failure_boundaries.md), [TC-F1-AC05-03](../testcases/modules/F1_identity/F1_auth_failure_boundaries.md) | AuthSessionFlowTest:154; AuthSessionFlowTest:184; SessionHandlersTest | Đạt |  |
| AC-06 | #3, BR-02 | [TC-F1-AC06-01](../testcases/modules/F1_identity/F1_auth_session.md#tc-f1-ac06-01--tái-sử-dụng-token-a-thu-hồi-cả-family), [TC-F1-AC06-02](../testcases/modules/F1_identity/F1_auth_failure_boundaries.md) | AuthSessionFlowTest:154; SessionHandlersTest | Đạt |  |
| AC-07 | #3 | [TC-F1-AC07-01](../testcases/modules/F1_identity/F1_auth_session.md#tc-f1-ac07-01--thiếu-header-chống-csrf-không-đổi-phiên) | AuthSessionFlowTest:184 | Đạt | Ca thiếu header trả 400 `IDT-V11`. |
| AC-08 | #7 | [TC-F1-AC08-01](../testcases/modules/F1_identity/F1_auth_session.md#tc-f1-ac08-01--quên-mật-khẩu-không-tiết-lộ-sự-tồn-tại-email), [TC-F1-AC08-02](../testcases/modules/F1_identity/F1_auth_failure_boundaries.md) | PasswordResetFlowTest:115 | Đạt |  |
| AC-09 | #8 | [TC-F1-AC09-01](../testcases/modules/F1_identity/F1_auth_session.md#tc-f1-ac09-01--otp-đúng-đổi-mật-khẩu-và-thu-hồi-phiên-cũ), [TC-F1-AC09-02](../testcases/modules/F1_identity/F1_auth_session.md#tc-f1-ac09-02--otp-reset-tài-khoản-admin-tạo-chuyển-trạng-thái), [TC-F1-AC09-03](../testcases/modules/F1_identity/F1_auth_failure_boundaries.md), [TC-F1-AC09-04](../testcases/modules/F1_identity/F1_auth_failure_boundaries.md) | PasswordResetFlowTest:136; PasswordResetFlowTest:155; PasswordResetFlowTest:184; PasswordResetFlowTest:204; PasswordResetFlowTest:311 | Đạt |  |
| AC-10 | #8 | [TC-F1-AC10-01](../testcases/modules/F1_identity/F1_auth_session.md#tc-f1-ac10-01--oauth-only-không-được-tạo-mật-khẩu-qua-reset) | PasswordResetFlowTest:292 | Đạt |  |
| AC-11 | #5–6 | [TC-F1-AC11-01](../testcases/modules/F1_identity/F1_auth_session.md#tc-f1-ac11-01--oauth-provider-chưa-xác-thực-email-bị-từ-chối), [TC-F1-AC11-02](../testcases/modules/F1_identity/F1_auth_failure_boundaries.md) | OAuthLoginFlowTest:161; OAuthLoginHandlersTest; HttpOAuthProviderAdapterTest | Đạt | Bảng cũ ghi "Chưa thực hiện" cho dòng này; lớp flow `OAuthLoginFlowTest` đã có và xanh trong full regression. |
| AC-12 | #10 | [TC-F1-AC12-01](../testcases/modules/F1_identity/F1_profile_lifecycle.md) | MyAccountFlowTest:176; MyAccountFlowTest:212; MyAccountFlowTest:229; UpdateMyProfileCommandHandlerTest | Đạt |  |
| AC-13 | #11–12 | [TC-F1-AC13-01](../testcases/modules/F1_identity/F1_profile_lifecycle.md), [TC-F1-AC13-02](../testcases/modules/F1_identity/F1_profile_lifecycle.md), [TC-F1-AC13-03](../testcases/modules/F1_identity/F1_profile_lifecycle.md) | MyAccountFlowTest:242; MyAccountFlowTest:282; MyAccountFlowTest:299; EmailChangeHandlersTest | Đạt |  |
| AC-14 | #13 | [TC-F1-AC14-01](../testcases/modules/F1_identity/F1_profile_lifecycle.md), [TC-F1-AC14-02](../testcases/modules/F1_identity/F1_profile_lifecycle.md) | MyAccountFlowTest:98; MyAccountFlowTest:128; ChangeMyPasswordCommandHandlerTest | Đạt |  |
| AC-15 | #13 | [TC-F1-AC15-01](../testcases/modules/F1_identity/F1_profile_lifecycle.md) | MyAccountFlowTest:144; MyAccountFlowTest:181; ChangeMyPasswordCommandHandlerTest | Đạt | Làm 2026-11-07 (`03-dd/api/F1_identity.md:50`). Trước đó ghi Hoãn; con số tổng hợp Bảng 3 do người sửa phần AC-81+ cập nhật (trước khi sửa dòng này, AC-15 đang tính là Hoãn). |
| AC-16 | #18 | [TC-F1-AC16-01](../testcases/modules/F1_identity/F1_profile_lifecycle.md) | MyAccountFlowTest:424; AccountDeletionHandlersTest:78 | Đạt |  |
| AC-17 | #18 | [TC-F1-AC17-01](../testcases/modules/F1_identity/F1_profile_lifecycle.md) | MyAccountFlowTest:424; AccountDeletionHandlersTest:65 | Đạt |  |
| AC-18 | JOB ẩn danh, BR-08 | [TC-F1-AC18-01](../testcases/modules/F1_identity/F1_profile_lifecycle.md), [TC-F1-AC18-02](../testcases/modules/F1_identity/F1_profile_lifecycle.md) | MyAccountFlowTest:463; AccountDeletionHandlersTest:117; AccountDeletionHandlersTest:125; AccountDeletionHandlersTest:133 | Đạt |  |
| AC-18b | JOB ẩn danh, BR-08 | [TC-F1-AC18b-01](../testcases/modules/F1_identity/F1_profile_lifecycle.md) | MyAccountFlowTest:463 | Đạt | Ca có tài khoản bị `ADMIN` khoá 400 ngày, email không đổi sau job. |
| AC-19 | #25 | [TC-F1-AC19-01](../testcases/modules/F1_identity/F1_class_membership_status.md) | StudentClassFlowTest:141 | Đạt |  |
| AC-20 | #25 | [TC-F1-AC20-01](../testcases/modules/F1_identity/F1_class_membership_status.md), [TC-F1-AC20-02](../testcases/modules/F1_identity/F1_class_membership_status.md) | StudentClassFlowTest:86; JoinClassByCodeCommandHandlerTest | Đạt |  |
| AC-20b | #25 | [TC-F1-AC20b-01](../testcases/modules/F1_identity/F1_class_membership_status.md) | StudentClassFlowTest:112 | Đạt |  |
| AC-21 | #27–28 | [TC-F1-AC21-01](../testcases/modules/F1_identity/F1_class_membership_status.md), [TC-F1-AC21-02](../testcases/modules/F1_identity/F1_class_membership_status.md) | ClassManagementFlowTest:355; AmqpClassEventAdapterTest | Đạt |  |
| AC-22 | #27 | [TC-F1-AC22-01](../testcases/modules/F1_identity/F1_class_membership_status.md) | ClassManagementFlowTest:355 (người lạ nhận 404); ClassManagementFlowTest:158 | Đạt |  |
| AC-23 | #26, BR-10 | [TC-F1-AC23-01](../testcases/modules/F1_identity/F1_class_membership_status.md), [TC-F1-AC23-02](../testcases/modules/F1_identity/F1_class_membership_status.md) | ClassProgressRulesTest:57; InstructorClassMetricsFlowTest:199; InstructorClassMetricsFlowTest:231 | Đạt | F1-7 (2026-11-07): `ClassProgressRulesTest` và `InstructorClassMetricsFlowTest` xanh ở batch `20261107-f17-f14-batch-flow.log` (dòng 2759) nhưng không có trong full regression. |
| AC-24 | #26, BR-10 | [TC-F1-AC24-01](../testcases/modules/F1_identity/F1_class_membership_status.md), [TC-F1-AC24-02](../testcases/modules/F1_identity/F1_class_membership_status.md), [TC-F1-AC24-03](../testcases/modules/F1_identity/F1_class_membership_status.md) | ClassProgressRulesTest:66; InstructorClassMetricsFlowTest:199; ClassManagementFlowTest:329 | Đạt | Như AC-23. `ClassManagementFlowTest:329` (danh sách học viên) có trong full regression. |
| AC-25 | #31 | [TC-F1-AC25-01](../testcases/modules/F1_identity/F1_rbac_audit.md), [TC-F1-AC25-02](../testcases/modules/F1_identity/F1_rbac_audit.md) | RbacManagementFlowTest:121; RbacManagementFlowTest:158; UpdateRolePermissionCommandHandlerTest | Đạt |  |
| AC-26 | #34 | [TC-F1-AC26-01](../testcases/modules/F1_identity/F1_rbac_audit.md) | RbacManagementFlowTest:371; DeleteRoleCommandHandlerTest | Đạt |  |
| AC-27 | #34 | [TC-F1-AC27-01](../testcases/modules/F1_identity/F1_rbac_audit.md), [TC-F1-AC27-02](../testcases/modules/F1_identity/F1_rbac_audit.md), [TC-F1-AC27-03](../testcases/modules/F1_identity/F1_rbac_audit.md) | RbacManagementFlowTest:371; DeleteRoleCommandHandlerTest | Đạt |  |
| AC-28 | #31 | [TC-F1-AC28-01](../testcases/modules/F1_identity/F1_rbac_audit.md), [TC-F1-AC28-02](../testcases/modules/F1_identity/F1_rbac_audit.md) | RbacManagementFlowTest:232; RbacManagementFlowTest:253; RbacManagementFlowTest:284 | Đạt |  |
| AC-29 | #37 | [TC-F1-AC29-01](../testcases/modules/F1_identity/F1_rbac_audit.md), [TC-F1-AC29-02](../testcases/modules/F1_identity/F1_rbac_audit.md), [TC-F1-AC29-03](../testcases/modules/F1_identity/F1_rbac_audit.md) | AdminUserManagementFlowTest:631; AdminUserManagementFlowTest:656; AdminUserHandlersTest | Đạt |  |
| AC-30 | #36–37 | [TC-F1-AC30-01](../testcases/modules/F1_identity/F1_rbac_audit.md), [TC-F1-AC30-02](../testcases/modules/F1_identity/F1_rbac_audit.md), [TC-F1-AC30-03](../testcases/modules/F1_identity/F1_rbac_audit.md), [TC-F1-AC30-04](../testcases/modules/F1_identity/F1_rbac_audit.md) | AdminUserManagementFlowTest:431; AdminUserManagementFlowTest:446; AdminUserManagementFlowTest:631; AccountDeletionHandlersTest:89 | Đạt |  |
| AC-31 | #39 | [TC-F1-AC31-01](../testcases/modules/F1_identity/F1_rbac_audit.md), [TC-F1-AC31-02](../testcases/modules/F1_identity/F1_rbac_audit.md) | AuditLogReadFlowTest:124; AuditLogReadFlowTest:497 | Đạt |  |
| AC-32 | #40 | [TC-F1-AC32-01](../testcases/modules/F1_identity/F1_rbac_audit.md), [TC-F1-AC32-02](../testcases/modules/F1_identity/F1_rbac_audit.md) | AuditLogReadFlowTest:314; AuditLogReadFlowTest:338; AuditLogMaskerTest | Đạt |  |
| AC-33 | #29, BR-12 | [TC-F1-AC33-01](../testcases/modules/F1_identity/F1_extension_a_metrics.md), [TC-F1-AC33-02](../testcases/modules/F1_identity/F1_extension_a_metrics.md), [TC-F1-AC33-03](../testcases/modules/F1_identity/F1_extension_a_metrics.md) | InstructorClassMetricsFlowTest:149; InstructorClassMetricsFlowTest:315; ClassMetricsHandlersTest:184; ClassProgressRulesTest:124 | Đạt | F1-7; như AC-23. |
| AC-34 | #45, BR-18 | [TC-F1-AC34-01](../testcases/modules/F1_identity/F1_extension_a_metrics.md) | InstructorActivityFlowTest:78; ListInstructorRecentActivityQueryHandlerTest; ProblemAssignedConsumerFlowTest:152; ProblemAssignedConsumerFlowTest:175; ProblemAssignedConsumerFlowTest:185; ApplyProblemAssignedToClassCommandHandlerTest; ProblemAssignedParserTest | Đạt | `InstructorActivityFlowTest` xanh trong full regression (dòng 19235). Consumer `ProblemAssignedToClass` (F1-12) chỉ xanh ở `20261106-235021-f1-12-f1-8-flow.log` (dòng 2552). Consumer này chưa có AC riêng, xem danh sách [NO-CRITERION] dưới bảng 3. |
| AC-35 | #50 | [TC-F1-AC35-01](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md), [TC-F1-AC35-02](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md) | AdminUserManagementFlowTest:300; AuthSessionFlowTest:125; MyAccountFlowTest:98; MyAccountFlowTest:144 | Đạt | Nhắc đổi mật khẩu mềm: `MyAccountFlowTest:144` đọc cờ `mustChangePassword`, `:98` đổi xong thì cờ tắt. |
| AC-36 | #53 | [TC-F1-AC36-01](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md), [TC-F1-AC36-02](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md) | RegistrationFlowTest:108; RegistrationFlowTest:274; EmailVerificationFlowTest | Đạt |  |
| AC-37 | #53 | [TC-F1-AC37-01](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md), [TC-F1-AC37-02](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md) | RegistrationFlowTest:162; RegistrationFlowTest:177; RegistrationFlowTest:230 | Đạt |  |
| AC-38 | #54 | [TC-F1-AC38-01](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md), [TC-F1-AC38-02](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md) | RegistrationFlowTest:190; RegistrationFlowTest:249 | Đạt |  |
| AC-39 | Sự kiện SubmissionGraded | [TC-F1-AC39-01](../testcases/modules/F1_identity/F1_extension_a_metrics.md), [TC-F1-AC39-02](../testcases/modules/F1_identity/F1_extension_a_metrics.md) | SubmissionGradedConsumerFlowTest:152; SubmissionGradedConsumerFlowTest:221; SubmissionGradedFromJudgeFlowTest:93; JudgeOutboxRelayFlowTest:89; ApplySubmissionGradedCommandHandlerTest; SubmissionGradedParserTest | Đạt | Ca đi từ F4 thật thêm 2026-11-08 (đợt B3, file test mới, không sửa file F4): ba bài nộp chạy hết vòng đời chấm của F4 (`compile`, `judging`, `record`, `finalize`) tự ghi Outbox, relay thật đẩy qua RabbitMQ thật, consumer F1 thật cập nhật `user_submission_stats` (tổng 3, Accepted 1 lần, lần đầu 1, ngôn ngữ nhiều nhất `PYTHON`) và số bài theo ngày UTC+7 khớp với số đếm thẳng từ `judge.submissions`; phát lại sự kiện đầu không làm số liệu tăng. Mutant đổi tên khóa `firstAttemptForProblem` ở parser F1 (mô phỏng F4 đổi khóa) bị bắt: test cũ dựng payload bằng tay không bắt được lỗi này. Chạy riêng lớp test, chưa qua full regression sau thay đổi này. |
| AC-40 | #55 | [TC-F1-AC40-01](../testcases/modules/F1_identity/F1_extension_a_metrics.md) | MyProgressFlowTest:142; MyProgressQueryHandlersTest | Đạt |  |
| AC-41 | #2 | [TC-F1-AC41-01](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md) | AuthSessionFlowTest:107; RegistrationFlowTest:108 | Đạt |  |
| AC-42 | #37 | [TC-F1-AC42-01](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md) | AdminUserManagementFlowTest:575 | Đạt |  |
| AC-43 | #22, ClassDeleted | [TC-F1-AC43-01](../testcases/modules/F1_identity/F1_extension_a_classes_admin.md), [TC-F1-AC43-02](../testcases/modules/F1_identity/F1_extension_a_classes_admin.md) | ClassManagementFlowTest:402; AmqpClassEventAdapterTest; InstructorClassHandlersTest | Đạt |  |
| AC-44 | #14–15 | [TC-F1-AC44-01](../testcases/modules/F1_identity/F1_extension_a_classes_admin.md), [TC-F1-AC44-02](../testcases/modules/F1_identity/F1_extension_a_classes_admin.md) | MyAccountFlowTest:331; MyAccountFlowTest:370; UpdateMySettingsCommandHandlerTest | Đạt |  |
| AC-45 | #30–31 | [TC-F1-AC45-01](../testcases/modules/F1_identity/F1_extension_a_classes_admin.md), [TC-F1-AC45-02](../testcases/modules/F1_identity/F1_extension_a_classes_admin.md) | IdentityMigrationTest:104 | Đạt | Full regression đỏ lớp này (danh sách bảng kỳ vọng cũ); đã sửa, chạy lại 7/7 xanh ở `261106-2312-f2-final-retest-failures.log`. |
| AC-46 | Khởi động F1, BR-23 | [TC-F1-AC46-01](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md), [TC-F1-AC46-02](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md) | FirstAdminAndTokenPurgeTest:63; FirstAdminAndTokenPurgeTest:85; FirstAdminAndTokenPurgeTest:94; BootstrapFirstAdminCommandHandlerTest | Đạt |  |
| AC-47 | JOB ẩn danh, BR-08 | [TC-F1-AC47-01](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md), [TC-F1-AC47-03](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md), [TC-F1-AC47-02](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md) | MyAccountFlowTest:463; AccountDeletionHandlersTest:117 | Đạt | Chạy job lần hai trả 0 (`MyAccountFlowTest:463`). |
| AC-48 | #14–15 | [TC-F1-AC48-01](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md) | MyAccountFlowTest:331; MyAccountFlowTest:370 | Đạt |  |
| AC-49 | #25A–25B | [TC-F1-AC49-01](../testcases/modules/F1_identity/F1_extension_a_classes_admin.md), [TC-F1-AC49-02](../testcases/modules/F1_identity/F1_extension_a_classes_admin.md) | StudentClassFlowTest:211; StudentClassQueryHandlersTest | Đạt |  |
| AC-50 | #25 | [TC-F1-AC50-01](../testcases/modules/F1_identity/F1_extension_a_classes_admin.md), [TC-F1-AC50-02](../testcases/modules/F1_identity/F1_extension_a_classes_admin.md) | StudentClassFlowTest:171; StudentClassFlowTest:193 | Đạt |  |
| AC-51 | #56 | [TC-F1-AC51-01](../testcases/modules/F1_identity/F1_extension_a_metrics.md), [TC-F1-AC51-02](../testcases/modules/F1_identity/F1_extension_a_metrics.md) | InstructorClassMetricsFlowTest:149; InstructorClassMetricsFlowTest:181; ClassMetricsHandlersTest:196; ClassMetricsHandlersTest:209 | Đạt | F1-7; như AC-23. |
| AC-52 | #57 | [TC-F1-AC52-01](../testcases/modules/F1_identity/F1_extension_a_metrics.md), [TC-F1-AC52-02](../testcases/modules/F1_identity/F1_extension_a_metrics.md), [TC-F1-AC52-03](../testcases/modules/F1_identity/F1_extension_a_metrics.md) | InstructorClassMetricsFlowTest:217; ClassProgressRulesTest:137; ClassProgressRulesTest:152; ClassMetricsHandlersTest:233; ClassMetricsHandlersTest:249 | Đạt | F1-7; như AC-23. |
| AC-53 | #58 | [TC-F1-AC53-01](../testcases/modules/F1_identity/F1_extension_a_metrics.md), [TC-F1-AC53-02](../testcases/modules/F1_identity/F1_extension_a_metrics.md), [TC-F1-AC53-03](../testcases/modules/F1_identity/F1_extension_a_metrics.md) | InstructorClassMetricsFlowTest:199; ClassProgressRulesTest:161; ClassProgressRulesTest:190; ClassMetricsHandlersTest:262 | Đạt | F1-7; như AC-23. |
| AC-54 | #60, #8 | [TC-F1-AC54-01](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md), [TC-F1-AC54-02](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md), [TC-F1-AC54-03](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md) | PasswordResetFlowTest:136; PasswordResetFlowTest:184; PasswordResetFlowTest:204 | Đạt |  |
| AC-55 | #61 | [TC-F1-AC55-01](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md), [TC-F1-AC55-02](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md), [TC-F1-AC55-03](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md) | AccountRestorationFlowTest:107; AccountRestorationFlowTest:169; AccountRestorationFlowTest:206; RestoreDeactivatedAccountCommandHandlerTest | Đạt |  |
| AC-56 | #16 | [TC-F1-AC56-01](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md), [TC-F1-AC56-02](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md) | SubmissionExportFlowTest:102; SubmissionExportFlowTest:162; SubmissionExportFlowTest:174; MyAccountFlowTest:500; MyAccountFlowTest:520; ExportMySubmissionsQueryHandlerTest; SubmissionExportBridgeTest:109 | Đạt | F1-9 #16 (2026-11-07): xanh ở `261107-1437-f1-9-bootstrap-flow2.log` (dòng 2354 và 1144), không có trong full regression. Cổng 5 phút theo từng loại: `SubmissionExportFlowTest:162`. Mutation (id lấy từ token, trả lại hạn mức khi F4 lỗi) chưa làm. |
| AC-57 | #17 | [TC-F1-AC57-01](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md), [TC-F1-AC57-02](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md) | InterviewTranscriptExportFlowTest:95; InterviewTranscriptExportFlowTest:134; InterviewTranscriptExportFlowTest:144; InterviewTranscriptExportFlowTest:156; InterviewTranscriptExportFlowTest:166; ExportMyInterviewTranscriptsQueryHandlerTest:28; ExportMyInterviewTranscriptsQueryHandlerTest:48; InterviewTranscriptExportBridgeTest | Đạt | Code xong, chưa test cả đợt (F5-9, 2026-11-11): dữ liệu qua cổng P4 của F5; hạn mức 1 lần/5 phút riêng cho loại này, trả hạn mức khi lỗi; `IDT-509` 503. Hai đột biến (bỏ lọc `user_id` ở F5, bỏ trả hạn mức) đều bị flow bắt (`20261111-1110-f5-9-mut1.log`, `20261111-1120-f5-9-mut2.log`). Frontend còn trả 501, nối ở M5. |
| AC-58 | #59 | [TC-F1-AC58-01](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md), [TC-F1-AC58-02](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md) | InstructorClassMetricsFlowTest:231; InstructorClassMetricsFlowTest:258; ClassMetricsHandlersTest:271; ClassMetricsHandlersTest:286; ClassMetricsHandlersTest:304 | Đạt | F1-7; như AC-23. |
| AC-59 | #41–43, #67–73 | [TC-F1-AC59-01](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md), [TC-F1-AC59-02](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md), [TC-F1-AC59-03](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md) | AdminDashboardFlowTest:85; AdminDashboardFlowTest:96; AdminDashboardFlowTest:128; AdminDashboardFlowTest:150; AdminSubmissionDashboardFlowTest:104; AdminSubmissionDashboardFlowTest:141; AdminSubmissionDashboardFlowTest:222; AdminSubmissionDashboardFlowTest:247; AdminSubmissionDashboardFlowTest:275; GetAdminSubmissionBlocksQueryHandlerTest | Đạt | #41, #42, #73: `AdminDashboardFlowTest` có trong full regression (dòng 17819). #43, #67 đến #72 (F1-8): `AdminSubmissionDashboardFlowTest` xanh ở `20261106-235021-f1-12-f1-8-flow.log` (dòng 2373) và batch `20261107-f17-f14-batch-flow.log` (dòng 1699), không có trong full regression. |
| AC-60 | #43, #68–69 | [TC-F1-AC60-01](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md), [TC-F1-AC60-02](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md) | AdminSubmissionDashboardFlowTest:115; AdminSubmissionDashboardFlowTest:141; AdminDashboardFlowTest:117; GetAdminSubmissionBlocksQueryHandlerTest | Đạt | F1-8; như AC-59. |
| AC-61 | #35, #62–63 | [TC-F1-AC61-01](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md) | AdminUserManagementFlowTest:265; AdminUserManagementFlowTest:147; AdminUserQueryHandlersTest | Đạt |  |
| AC-62 | #64–66 | [TC-F1-AC62-01](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md), [TC-F1-AC62-02](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md), [TC-F1-AC62-03](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md) | AuditLogReadFlowTest:433; AuditLogReadFlowTest:465; AuditLogReadFlowTest:474; AuditLogReadFlowTest:497 | Đạt |  |
| AC-63 | #51–52 | [TC-F1-AC63-01](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md), [TC-F1-AC63-02](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md), [TC-F1-AC63-03](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md) | LogRetentionFlowTest:80; LogRetentionFlowTest:105; LogRetentionFlowTest:125; LogRetentionFlowTest:167; UpdateLogRetentionSettingsCommandHandlerTest | Đạt |  |
| AC-64 | #49 | [TC-F1-AC64-01](../testcases/modules/F1_identity/F1_extension_b_class_auth.md), [TC-F1-AC64-02](../testcases/modules/F1_identity/F1_extension_b_class_auth.md) | AuthSessionFlowTest:141 | Đạt |  |
| AC-65 | #19–21 | [TC-F1-AC65-01](../testcases/modules/F1_identity/F1_extension_b_class_auth.md), [TC-F1-AC65-02](../testcases/modules/F1_identity/F1_extension_b_class_auth.md) | ClassManagementFlowTest:114; ClassManagementFlowTest:158; ClassManagementFlowTest:258; InstructorClassHandlersTest | Đạt |  |
| AC-66 | #22 | [TC-F1-AC66-01](../testcases/modules/F1_identity/F1_extension_b_class_auth.md), [TC-F1-AC66-02](../testcases/modules/F1_identity/F1_extension_b_class_auth.md) | ClassManagementFlowTest:402 | Đạt |  |
| AC-67 | #23–24 | [TC-F1-AC67-01](../testcases/modules/F1_identity/F1_extension_b_class_auth.md), [TC-F1-AC67-02](../testcases/modules/F1_identity/F1_extension_b_class_auth.md) | ClassManagementFlowTest:304 | Đạt |  |
| AC-68 | #5–6 | [TC-F1-AC68-01](../testcases/modules/F1_identity/F1_extension_b_class_auth.md), [TC-F1-AC68-02](../testcases/modules/F1_identity/F1_extension_b_class_auth.md) | OAuthLoginFlowTest:88; OAuthLoginFlowTest:100; OAuthLoginFlowTest:122; OAuthLoginFlowTest:134; OAuthLoginFlowTest:147; HttpOAuthProviderAdapterTest | Đạt | Chạy với máy chủ nhà cung cấp giả; khoá OAuth thật chưa điền (F1-6 chờ owner). |
| AC-69 | #4 | [TC-F1-AC69-01](../testcases/modules/F1_identity/F1_extension_b_class_auth.md) | AuthSessionFlowTest:225; SessionHandlersTest | Đạt |  |
| AC-70 | #11–12 | [TC-F1-AC70-01](../testcases/modules/F1_identity/F1_extension_b_class_auth.md), [TC-F1-AC70-02](../testcases/modules/F1_identity/F1_extension_b_class_auth.md) | MyAccountFlowTest:242; MyAccountFlowTest:282; MyAccountFlowTest:299 | Đạt |  |
| AC-71 | #9 | [TC-F1-AC71-01](../testcases/modules/F1_identity/F1_extension_b_class_auth.md) | MyAccountFlowTest:144; MyAccountFlowTest:603 | Đạt |  |
| AC-72 | #19, #26, #28–29 | [TC-F1-AC72-01](../testcases/modules/F1_identity/F1_extension_b_class_auth.md), [TC-F1-AC72-02](../testcases/modules/F1_identity/F1_extension_b_class_auth.md) | InstructorClassMetricsFlowTest:231; InstructorClassMetricsFlowTest:258; InstructorClassMetricsFlowTest:279; InstructorClassMetricsFlowTest:296; InstructorClassMetricsFlowTest:315; ClassManagementFlowTest:329 | Đạt | F1-7; như AC-23. |
| AC-73 | #30–33 | [TC-F1-AC73-01](../testcases/modules/F1_identity/F1_extension_b_admin_security.md), [TC-F1-AC73-02](../testcases/modules/F1_identity/F1_extension_b_admin_security.md) | RbacManagementFlowTest:121; RbacManagementFlowTest:443; ClassManagementFlowTest:201; AdminUserManagementFlowTest:131 | Đạt |  |
| AC-74 | #35, #38 | [TC-F1-AC74-01](../testcases/modules/F1_identity/F1_extension_b_admin_security.md), [TC-F1-AC74-02](../testcases/modules/F1_identity/F1_extension_b_admin_security.md) | AdminUserManagementFlowTest:147; AdminUserManagementFlowTest:197; AdminUserManagementFlowTest:685; AdminUserManagementFlowTest:720; AdminUserHandlersTest; AdminUserQueryHandlersTest | Đạt | F1-4 (2026-11-07): ca mới `AdminUserManagementFlowTest:197` (cột Đã giải, Lượt nộp) xanh ở batch `20261107-f17-f14-batch-flow.log` (dòng 1922, 26 test), không có trong full regression (25 test, dòng 18146). Các ca lọc, phân trang và đặt lại mật khẩu đã xanh trong full regression. |
| AC-75 | #46–47 | [TC-F1-AC75-01](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md), [TC-F1-AC75-02](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md), [TC-F1-AC75-03](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md), [TC-F1-AC75-04](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md) | MyProgressFlowTest:80; MyProgressFlowTest:115; MyProgressQueryHandlersTest | Đạt |  |
| AC-76 | #36 | [TC-F1-AC76-01](../testcases/modules/F1_identity/F1_extension_b_admin_security.md), [TC-F1-AC76-02](../testcases/modules/F1_identity/F1_extension_b_admin_security.md), [TC-F1-AC76-03](../testcases/modules/F1_identity/F1_extension_b_admin_security.md) | AdminUserManagementFlowTest:396; AdminUserManagementFlowTest:431; AdminUserManagementFlowTest:446; AdminUserManagementFlowTest:469; AdminUserManagementFlowTest:490 | Đạt |  |
| AC-77 | ~~#44~~, #56 | [TC-F1-AC77-01](../testcases/modules/F1_identity/F1_extension_b_admin_security.md) | InstructorClassMetricsFlowTest:149; InstructorClassMetricsFlowTest:169; InstructorClassMetricsFlowTest:181 | Đạt | Đóng 2026-11-07: owner bỏ #44, thay bởi #56 (ponytail bậc 4); phần còn lại do #56 phủ. Hai test #56 xanh ở lượt F1-7 (`20261107-f17-f14-batch-flow.log`), chưa vào full regression. TC-F1-AC77-01 còn nhắc ca #44, cần soát lại bởi agent test case. |
| AC-78 | #39–40 | [TC-F1-AC78-01](../testcases/modules/F1_identity/F1_extension_b_admin_security.md), [TC-F1-AC78-02](../testcases/modules/F1_identity/F1_extension_b_admin_security.md), [TC-F1-AC78-03](../testcases/modules/F1_identity/F1_extension_b_admin_security.md) | AuditLogReadFlowTest:253; AuditLogReadFlowTest:360; AuditLogReadFlowTest:402 | Đạt |  |
| AC-79 | #35–38, #50 | [TC-F1-AC79-01](../testcases/modules/F1_identity/F1_extension_c_admin_user_management.md), [TC-F1-AC79-02](../testcases/modules/F1_identity/F1_extension_c_admin_user_management.md), [TC-F1-AC79-03](../testcases/modules/F1_identity/F1_extension_c_admin_user_management.md), [TC-F1-AC79-04](../testcases/modules/F1_identity/F1_extension_c_admin_user_management.md) | AdminUserManagementFlowTest:349; AdminUserManagementFlowTest:380; AdminUserManagementFlowTest:609; AdminUserManagementFlowTest:743; AdminUserHandlersTest | Đạt | TC QA đã viết 2026-11-02, chưa chạy. |
| AC-80 | #48 | Chưa có TC QA | MyProgressFlowTest:166; MyProgressFlowTest:192; ListAllMySolveStatesQueryHandlerTest | Đạt | Chưa có TC QA. |
| AC-81 | Sự kiện ProblemAssignedToClass (F1-12) | Chưa có TC QA | ProblemAssignedConsumerFlowTest:152; ProblemAssignedConsumerFlowTest:175; ProblemAssignedConsumerFlowTest:185; ApplyProblemAssignedToClassCommandHandlerTest; ProblemAssignedParserTest | Đạt | Xanh ở `20261106-235021-f1-12-f1-8-flow.log` (dòng 2552), không có trong full regression. |
| AC-82 | Sự kiện SubmissionAccepted | Chưa có TC QA | SubmissionAcceptedConsumerFlowTest:197; SubmissionAcceptedConsumerFlowTest:223; SubmissionAcceptedConsumerFlowTest:246; ApplySubmissionAcceptedCommandHandlerTest; SubmissionAcceptedParserTest | Đạt | `SubmissionAcceptedConsumerFlowTest` 3 test xanh trong full regression (dòng 23665). Chưa có sự kiện thật từ relay F4. |
| AC-83 | #43, #67–72, IDT-506 (F1-8) | Chưa có TC QA | AdminSubmissionDashboardFlowTest:275; GetAdminSubmissionBlocksQueryHandlerTest:100 | Đạt | F1-8; như AC-59. |
| AC-84 | #43, #67–72 (F1-8) | Chưa có TC QA | AdminSubmissionDashboardFlowTest:141; AdminSubmissionDashboardFlowTest:115; AdminSubmissionDashboardFlowTest:185; AdminSubmissionDashboardFlowTest:206; GetAdminSubmissionStatsQueryHandlerTest:62; GetAdminSubmissionStatsQueryHandlerTest:78 | Đạt | Ca ranh giới thêm 2026-11-08 (đợt B1): mốc "bây giờ" cố định 23:59:59 và 00:00:00 UTC+7 cho ngày và tháng (handler), và bài nộp đặt đúng giây trước, đúng giây của nửa đêm và đầu tháng UTC+7 (flow). Mutant đổi múi giờ nhóm ngày sang UTC bị bắt (2 ca đỏ), mutant nhóm tháng sang UTC bị bắt (1 ca đỏ). Chạy riêng lớp test, chưa qua full regression sau thay đổi này. |
| AC-85 | #26, #28, #29, #56–59, IDT-507 (F1-7) | Chưa có TC QA | InstructorClassMetricsFlowTest:339; InstructorClassMetricsFlowTest:364; ClassMetricsHandlersTest:125; ClassMetricsHandlersTest:136; ClassMetricsHandlersTest:147; ClassMetricsHandlersTest:166; ClassMetricsHandlersTest:175 | Đạt | Ca 503 cho #56 đến #59 thêm 2026-11-08 (đợt B2): handler với F4 lỗi và với F2 lỗi (cả bốn), flow thật với giảng viên và lớp mới (tránh cache) khi đổi tên bảng `judge.submissions` rồi `problem.class_assignments`: 503 `IDT-507`, thân lỗi không chứa tên bảng. Mutant nuốt lỗi F2 (`windows()`) bị bắt (2 ca handler đỏ), mutant nuốt lỗi F4 (`rows()`) bị bắt (2 ca handler đỏ), cả hai cùng lúc làm ca flow mới đỏ ở #56. Chạy riêng lớp test, chưa qua full regression sau thay đổi này. |
| AC-86 | #26, #58, BR-10 (F1-7) | Chưa có TC QA | ClassProgressRulesTest:66; ClassProgressRulesTest:106; InstructorClassMetricsFlowTest:199 | Đạt | F1-7; như AC-23. `ClassProgressRulesTest:106` khẳng định chuỗi giảm, hai tuần bằng nhau, một tuần và tuần rỗng cắt chuỗi. |
| AC-87 | #35 (F1-4) | Chưa có TC QA | AdminUserManagementFlowTest:197; AdminUserManagementFlowTest:229 | Đạt | F1-4; như AC-74. Lớp có trong full regression nhưng hai ca này thêm sau lượt đó. |
| AC-88 | #16 (F1-9) | Chưa có TC QA | SubmissionExportFlowTest:151; SubmissionExportFlowTest:162 | Đạt | F1-9; như AC-56 (`261107-1437-f1-9-bootstrap-flow2.log`). |
| AC-89 | #16, IDT-508 (F1-9) | Chưa có TC QA | SubmissionExportFlowTest:184; ExportMySubmissionsQueryHandlerTest:49 | Đạt | F1-9; như AC-56. |
| AC-90 | Sự kiện SolutionReviewCompleted (F5-5a) | Chưa có TC QA | SolutionReviewCompletedConsumerFlowTest:146; SolutionReviewCompletedConsumerFlowTest:170; ApplySolutionReviewCompletedCommandHandlerTest; SolutionReviewCompletedParserTest | Đạt | Code xong, chưa test cả đợt: `SolutionReviewCompletedConsumerFlowTest` 2 test và `identity` 380 test xanh ở lượt riêng của lát (`20261110-1930-f5-5a-identity-verify.log`, `20261110-2005-f5-5a-bootstrap.log`); chưa qua full regression |
| AC-91 | Sự kiện MockInterviewFinished (F5-8) | Chưa có TC QA | MockInterviewFinishedConsumerFlowTest; AiOutboxFlowTest; ApplyMockInterviewFinishedCommandHandlerTest; MockInterviewFinishedParserTest | Đạt | Code xong, chưa test cả đợt: identity 384 test xanh (`20261111-0030-f5-8-identity-verify.log`), bootstrap 192 test xanh (`20261111-0040-f5-8-bootstrap.log`). `ref_type = INTERVIEW_SESSION` do code chọn (DD F1 chưa nêu). |

Bảng 3: Tổng hợp trạng thái 93 tiêu chí (AC-01 đến AC-91 cùng AC-18b và AC-20b), trước và sau lần đồng bộ 2026-11-11 (cột Sau tính cả AC-81 đến AC-89 thêm 2026-11-07, AC-90 và AC-91 thêm ở F5-5a và F5-8, việc đóng AC-77, việc AC-39, AC-84, AC-85 chuyển Đạt ngày 2026-11-08 và AC-57 chuyển từ Hoãn sang Đạt ngày 2026-11-11 khi F5-9 xong; đếm lại từ bảng truy vết: 93 dòng, 93 Đạt, 0 Hoãn)

| Trạng thái | Trước (bảng cũ, 2026-11-05) | Sau (2026-11-08) |
| :--- | :-: | :-: |
| Đạt | - | 93 |
| Code xong, chưa test toàn bộ | - | 0 |
| Một phần | 1 | 0 |
| Chưa làm | - | 0 |
| Hoãn (thuộc F5) | - | 0 |
| Có JUnit tự động, chưa phân loại | 66 | 0 |
| "Chưa thực hiện" (gộp test mã nguồn và test case QA) | 15 | 0 |
| Tổng | 82 | 93 |

Cột "Trước" dùng cách ghi cũ nên không so trực tiếp được: bảng cũ chỉ có ba giá trị (JUnit tự động đạt, Một phần, Chưa thực hiện) và dòng "Chưa thực hiện" thường chỉ nghĩa là test case QA chưa chạy. Khi tra lại bằng grep, 13 trong 15 dòng đó có test mã nguồn thật (AC-11, 23, 24, 33, 47, 51, 52, 53, 58, 60, 68, 72 và một phần AC-77); AC-15 và AC-57 là hai dòng thật sự chưa có hành vi tại thời điểm đó (nay cả hai đã Đạt); AC-77 chỉ phủ một phần.

### Ghi chú đồng bộ 2026-11-07

Nguồn bằng chứng:

- Full regression backend `05-coding/backend/logs/261106-2253-f2-final-full-regression-r2.log` (chạy từ module `algoprep-identity`, tổng 910 test tại dòng 25373): module unit `algoprep-identity` SUCCESS (dòng 25378). Các lớp flow F1 đều Failures 0, Errors 0: `AccountRestorationFlowTest` dòng 17661, `AdminDashboardFlowTest` 17819, `AdminUserManagementFlowTest` 18146, `AuditLogReadFlowTest` 18422, `AuthSessionFlowTest` 18603, `ClassManagementFlowTest` 18876, `FirstAdminAndTokenPurgeTest` 19022, `InstructorActivityFlowTest` 19235, `LoginInfrastructureTest` 19442, `LogRetentionFlowTest` 19656, `MyAccountFlowTest` 19983, `MyProgressFlowTest` 20810, `OAuthLoginFlowTest` 21420, `PasswordResetFlowTest` 21786, `RbacManagementFlowTest` 22236, `RegistrationFlowTest` 22954, `SecurityFoundationTest` 23112, `StudentClassFlowTest` 23361, `SubmissionAcceptedConsumerFlowTest` 23665, `SubmissionGradedConsumerFlowTest` 24360.
- Lượt đó có 16 test lỗi ở 4 lớp, không lớp nào là test flow của F1 trừ `IdentityMigrationTest` (AC-45): `IdentityMigrationTest` (danh sách bảng kỳ vọng cũ, đã sửa, chạy lại 7/7 xanh ở `261106-2312-f2-final-retest-failures.log`) và `JudgeOutboxRelayFlowTest` (đã sửa, chạy lại 6/6 xanh); `RunSampleFlowTest` và `StarterCodeFlowTest` là lỗi F3/F4 có sẵn ở commit sạch `0d1be94`, không thuộc F1.
- Các lát sau full regression, mỗi lát chạy lượt flow riêng và xanh, nhưng không có trong lượt full: F1-12 và F1-8 (`20261106-234744-f1-12-f1-8-identity-verify.log` 334 test unit; `20261106-235021-f1-12-f1-8-flow.log` 61 flow test); F1-7 và F1-4 (`20261107-f17-f14-batch-flow.log` 103 flow test, `20261107-f17-flow-metrics.log` 11 test, `20261107-f17-identity-verify.log` 360 test unit); F1-9 #16 (`261107-1431-f1-9-identity-verify.log` 363 test unit; `261107-1437-f1-9-bootstrap-flow2.log` 55 flow test). Mười ba AC thuộc các lát này ở trạng thái "Code xong, chưa test toàn bộ" (AC-23, 24, 33, 34, 51, 52, 53, 56, 58, 59, 60, 72, 74). Cùng ngày thêm AC-77 (đóng, thay bởi #56) và sáu AC mới là AC-81, 83, 86, 87, 88, 89, nên bảng 3 ghi 20.

Chưa làm và không được tính là đã có: mutation test cho quyền, riêng tư và đếm (quyền: chỉ ADMIN đọc `ADM0101`, giảng viên chỉ đọc lớp của mình, id người dùng lấy từ token ở #16 và #48; trần và hoàn hạn mức xuất dữ liệu), kiểm trình duyệt thật cho các màn đã nối API, chạy test case QA, và lượt full regression sau khi F3/F4 sửa hai lớp lỗi có sẵn. "Đạt" ở bảng 1 không bao hàm mutation.

Không còn AC nào thiếu hành vi: AC-57 (#17) từng thuộc backlog F5 (`DEC-2026-1108-f1-f2-closing-decisions`) và đã Đạt từ F5-9 ngày 2026-11-11. AC-15 (BR-05) đã qua full regression 2026-11-08. Ba AC từng "Một phần" đã đóng ngày 2026-11-08: AC-39 (thêm ca từ vòng đời chấm bài thật của F4 qua relay Outbox và RabbitMQ thật tới consumer F1, `SubmissionGradedFromJudgeFlowTest`), AC-84 (thêm ca ranh giới nửa đêm và đầu tháng UTC+7) và AC-85 (thêm ca 503 `IDT-507` cho #56 đến #59 khi F4 hoặc F2 lỗi). Không còn AC nào ở "Một phần".

**Cập nhật 2026-11-07 (đợt đóng [NO-CRITERION]).** Tám hành vi đã có code và test nhưng chưa có tiêu chí nay có AC: consumer `ProblemAssignedToClass` thành AC-81; consumer `SubmissionAccepted` thành AC-82; `IDT-506` và cửa sổ ngày UTC+7 của `ADM0101` thành AC-83 và AC-84; `IDT-507` thành AC-85; quy tắc "Cần hỗ trợ" thành AC-86; hai cột của #35 thành AC-87; trần 2000 dòng, hạn mức 5 phút và `IDT-508` của #16 thành AC-88 và AC-89. Hành vi "trạng thái giải theo bài" của #48 đã có từ AC-80. Hiện không còn hành vi nào trong danh sách [NO-CRITERION]. Các AC mới chưa có test case QA (không thuộc phạm vi đợt này) và không mang bằng chứng mutation.

## 4. Tham chiếu

`03-dd/api/F1_identity.md`, `03-dd/logic/F1_identity.md`, `03-dd/validation/F1_identity.md`,
`02-bd/security/F1_identity.md`, `02-bd/database/F1_identity.md`.

### Ghi chú đồng bộ 2026-11-08 (full regression sạch)

Chạy `mvn verify` toàn bộ backend ở cây chính ngày 2026-11-08: BUILD SUCCESS, 21 phút 15 giây, log `05-coding/backend/logs/full-regression-261107.log`. Tám module có test đều xanh: common 40, identity 369, problem-bank 461, harness 809, judge 429, bootstrap 952 (35 bỏ qua), tổng khoảng 3060 test, 0 lỗi; Spotless, Checkstyle, SpotBugs và JaCoCo qua. 20 lớp test mà 21 AC trích dẫn đều có trong lượt chạy này và đều 0 lỗi (ví dụ `MyAccountFlowTest` 25, `InstructorClassMetricsFlowTest` 12, `SubmissionExportFlowTest` 7, `AdminUserManagementFlowTest` 26, `ChangeMyPasswordCommandHandlerTest` 13). Vì vậy 21 AC chuyển từ "Code xong, chưa test toàn bộ" sang "Đạt" (66 lên 87); AC-57 vẫn Hoãn, ba AC "Một phần" (AC-39, AC-84, AC-85) không đổi.

35 test bỏ qua nằm ở `ExecutionBridgeFlowTest`, `PersistedWorkerFlowTest` và `ReferenceSolutionSandboxFlowTest`: chúng cần sandbox có toolchain nên tự bỏ qua. Chạy riêng với sandbox dùng một lần (`-Djudge.go-judge.base-url=http://127.0.0.1:5055`): 66 ca, 0 lỗi, 1 bỏ qua (`finalTenMinuteEventRouteOutage...`, ca chấp nhận 10 phút chỉ bật bằng `judge.acceptance.long-runtime`, thuộc F4), log `logs/full-regression-toolchain-261108.log`.

Mutation đã chạy ngày 2026-11-07 cho 32 quy tắc quyền và riêng tư (báo cáo `06-plan/reports/261107-2330-f1-f2-mutation/report.md`): 32/32 bị bắt, hai mutant sống sót lượt đầu và đã có test mới (`InstructorClassMetricsFlowTest.aStudentInTwoClassesIsOnePersonInTheSummaryButOneRowPerClass`). Mutation không phủ mọi quy tắc; xem Bảng 3 của báo cáo đó. Kiểm trình duyệt thật cũng đã chạy (`06-plan/reports/261107-2100-f1-f2-kiem-trinh-duyet/report.md`). Hai đoạn "Chưa làm" ở ghi chú 2026-11-07 phía trên về mutation, trình duyệt thật và full regression vì thế đã lỗi thời.

### Ghi chú đồng bộ 2026-11-08 (full regression lượt 2, sau đợt B đến D)

Chạy `mvn verify` toàn bộ backend ở cây chính ngày 2026-11-08 (lượt 2, sau khi thêm test đợt B và đợt D): BUILD SUCCESS, 20 phút 6 giây, log `05-coding/backend/logs/261108-e1-full-verify-r2.log`. Lượt 1 cùng ngày đỏ ở `algoprep-bootstrap` vì Docker Desktop tắt (Testcontainers không lấy được image), không phải lỗi mã, log `261108-e1-full-verify.log`. Lượt 2: bootstrap 969 test, 0 lỗi, 35 bỏ qua (`ExecutionBridgeFlowTest` 18, `PersistedWorkerFlowTest` 16, `ReferenceSolutionSandboxFlowTest` 1, cần sandbox có toolchain); các module khác 40, 371, 461, 811, 431 test, 0 lỗi. Các lớp của AC-39, AC-84, AC-85 đều xanh trong lượt này: `SubmissionGradedFromJudgeFlowTest` 1, `AdminSubmissionDashboardFlowTest` 8, `GetAdminSubmissionStatsQueryHandlerTest` 5, `InstructorClassMetricsFlowTest` 13, `ClassMetricsHandlersTest` 17. Mutation đợt 3 (F1-4, F1-12) ở `06-plan/reports/261108-1600-f1-f2-mutation/report.md`: các mutant của hai lát này đều bị bắt.

