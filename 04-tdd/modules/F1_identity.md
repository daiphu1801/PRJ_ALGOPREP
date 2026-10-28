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
- **Kết quả mong đợi**: Tài khoản chỉ đăng nhập liên kết có thể đặt mật khẩu lần đầu mà không cần mật khẩu cũ hay mã email bổ sung; người dùng nhận thông báo bảo mật về việc này.

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
| **Hành động** | Đọc tóm tắt tiến độ, hoạt động gần đây và trạng thái giải của một nhóm bài. |
| **Kết quả mong đợi** | Số bài đã giải theo kết quả tốt nhất; tỷ lệ chấp thuận loại lượt lỗi hệ thống khỏi mẫu số, còn tổng lượt vẫn thể hiện đúng. Hoạt động mới nhất theo thời gian thật và chỉ của chính người đó; trạng thái bài ngoài danh sách yêu cầu không bị trả kèm. |
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

### AC-78 — Tìm và xuất nhật ký giữ cùng bộ lọc
| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Có nhật ký thuộc nhiều nhóm, người thực hiện và ngày khác nhau. |
| **Hành động** | Lọc theo nhóm, người và khoảng ngày; tải trang kế rồi xuất dữ liệu theo cùng bộ lọc. |
| **Kết quả mong đợi** | Trang kế không lặp hoặc mất dòng; đổi bộ lọc bắt đầu lại từ trang đầu. Tệp xuất chứa đúng tập bản ghi lọc và chỉ các cột đã chọn, không có mã xác thực hoặc bí mật. |
| **Nguồn** | [SoT: 03-dd/api/F1_identity.md] #39–40 |

## 3. Truy vết

Bảng này nối từng tiêu chí với hợp đồng DD. Mã test case được điền khi tạo testcase; giữ nguyên mã AC đã được tài liệu khác tham chiếu.

| AC | Endpoint / luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| AC-10 | #8 | [TC-F1-AC10-01](../testcases/modules/F1_identity/F1_auth_session.md#tc-f1-ac10-01--oauth-only-không-được-tạo-mật-khẩu-qua-reset) | Chưa thực hiện |
| AC-11 | #5–6 | [TC-F1-AC11-01](../testcases/modules/F1_identity/F1_auth_session.md#tc-f1-ac11-01--oauth-provider-chưa-xác-thực-email-bị-từ-chối), [TC-F1-AC11-02](../testcases/modules/F1_identity/F1_auth_failure_boundaries.md) | Chưa thực hiện |
| AC-12 | #10 | [TC-F1-AC12-01](../testcases/modules/F1_identity/F1_profile_lifecycle.md) | Chưa thực hiện |
| AC-13 | #11–12 | [TC-F1-AC13-01](../testcases/modules/F1_identity/F1_profile_lifecycle.md), [TC-F1-AC13-02](../testcases/modules/F1_identity/F1_profile_lifecycle.md), [TC-F1-AC13-03](../testcases/modules/F1_identity/F1_profile_lifecycle.md) | Chưa thực hiện |
| AC-14 | #13 | [TC-F1-AC14-01](../testcases/modules/F1_identity/F1_profile_lifecycle.md), [TC-F1-AC14-02](../testcases/modules/F1_identity/F1_profile_lifecycle.md) | Chưa thực hiện |
| AC-15 | #13 | [TC-F1-AC15-01](../testcases/modules/F1_identity/F1_profile_lifecycle.md) | Chưa thực hiện |
| AC-16 | #18 | [TC-F1-AC16-01](../testcases/modules/F1_identity/F1_profile_lifecycle.md) | Chưa thực hiện |
| AC-17 | #18 | [TC-F1-AC17-01](../testcases/modules/F1_identity/F1_profile_lifecycle.md) | Chưa thực hiện |
| AC-18 | JOB ẩn danh, BR-08 | [TC-F1-AC18-01](../testcases/modules/F1_identity/F1_profile_lifecycle.md), [TC-F1-AC18-02](../testcases/modules/F1_identity/F1_profile_lifecycle.md) | Chưa thực hiện |
| AC-19 | #25 | [TC-F1-AC19-01](../testcases/modules/F1_identity/F1_class_membership_status.md) | Chưa thực hiện |
| AC-20 | #25 | [TC-F1-AC20-01](../testcases/modules/F1_identity/F1_class_membership_status.md), [TC-F1-AC20-02](../testcases/modules/F1_identity/F1_class_membership_status.md) | Chưa thực hiện |
| AC-21 | #27–28 | [TC-F1-AC21-01](../testcases/modules/F1_identity/F1_class_membership_status.md), [TC-F1-AC21-02](../testcases/modules/F1_identity/F1_class_membership_status.md) | Chưa thực hiện |
| AC-22 | #27 | [TC-F1-AC22-01](../testcases/modules/F1_identity/F1_class_membership_status.md) | Chưa thực hiện |
| AC-23 | #26, BR-10 | [TC-F1-AC23-01](../testcases/modules/F1_identity/F1_class_membership_status.md), [TC-F1-AC23-02](../testcases/modules/F1_identity/F1_class_membership_status.md) | Chưa thực hiện |
| AC-24 | #26, BR-10 | [TC-F1-AC24-01](../testcases/modules/F1_identity/F1_class_membership_status.md), [TC-F1-AC24-02](../testcases/modules/F1_identity/F1_class_membership_status.md), [TC-F1-AC24-03](../testcases/modules/F1_identity/F1_class_membership_status.md) | Chưa thực hiện |
| AC-25 | #31 | [TC-F1-AC25-01](../testcases/modules/F1_identity/F1_rbac_audit.md), [TC-F1-AC25-02](../testcases/modules/F1_identity/F1_rbac_audit.md) | Chưa thực hiện |
| AC-26 | #34 | [TC-F1-AC26-01](../testcases/modules/F1_identity/F1_rbac_audit.md) | Chưa thực hiện |
| AC-27 | #34 | [TC-F1-AC27-01](../testcases/modules/F1_identity/F1_rbac_audit.md), [TC-F1-AC27-02](../testcases/modules/F1_identity/F1_rbac_audit.md), [TC-F1-AC27-03](../testcases/modules/F1_identity/F1_rbac_audit.md) | Chưa thực hiện |
| AC-28 | #31 | [TC-F1-AC28-01](../testcases/modules/F1_identity/F1_rbac_audit.md), [TC-F1-AC28-02](../testcases/modules/F1_identity/F1_rbac_audit.md) | Chưa thực hiện |
| AC-29 | #37 | [TC-F1-AC29-01](../testcases/modules/F1_identity/F1_rbac_audit.md), [TC-F1-AC29-02](../testcases/modules/F1_identity/F1_rbac_audit.md), [TC-F1-AC29-03](../testcases/modules/F1_identity/F1_rbac_audit.md) | Chưa thực hiện |
| AC-30 | #36–37 | [TC-F1-AC30-01](../testcases/modules/F1_identity/F1_rbac_audit.md), [TC-F1-AC30-02](../testcases/modules/F1_identity/F1_rbac_audit.md), [TC-F1-AC30-03](../testcases/modules/F1_identity/F1_rbac_audit.md), [TC-F1-AC30-04](../testcases/modules/F1_identity/F1_rbac_audit.md) | Chưa thực hiện |
| AC-31 | #39 | [TC-F1-AC31-01](../testcases/modules/F1_identity/F1_rbac_audit.md), [TC-F1-AC31-02](../testcases/modules/F1_identity/F1_rbac_audit.md) | Chưa thực hiện |
| AC-32 | #40 | [TC-F1-AC32-01](../testcases/modules/F1_identity/F1_rbac_audit.md), [TC-F1-AC32-02](../testcases/modules/F1_identity/F1_rbac_audit.md) | Chưa thực hiện |
| AC-33 | #29, BR-12 | [TC-F1-AC33-01](../testcases/modules/F1_identity/F1_extension_a_metrics.md), [TC-F1-AC33-02](../testcases/modules/F1_identity/F1_extension_a_metrics.md), [TC-F1-AC33-03](../testcases/modules/F1_identity/F1_extension_a_metrics.md) | Chưa thực hiện |
| AC-34 | #45, BR-18 | [TC-F1-AC34-01](../testcases/modules/F1_identity/F1_extension_a_metrics.md) | Chưa thực hiện |
| AC-35 | #50 | [TC-F1-AC35-01](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md), [TC-F1-AC35-02](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md) | Chưa thực hiện |
| AC-36 | #53 | [TC-F1-AC36-01](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md), [TC-F1-AC36-02](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md) | Chưa thực hiện |
| AC-37 | #53 | [TC-F1-AC37-01](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md), [TC-F1-AC37-02](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md) | Chưa thực hiện |
| AC-38 | #54 | [TC-F1-AC38-01](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md), [TC-F1-AC38-02](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md) | Chưa thực hiện |
| AC-39 | Sự kiện SubmissionGraded | [TC-F1-AC39-01](../testcases/modules/F1_identity/F1_extension_a_metrics.md), [TC-F1-AC39-02](../testcases/modules/F1_identity/F1_extension_a_metrics.md) | Chưa thực hiện |
| AC-40 | #55 | [TC-F1-AC40-01](../testcases/modules/F1_identity/F1_extension_a_metrics.md) | Chưa thực hiện |
| AC-41 | #2 | [TC-F1-AC41-01](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md) | Chưa thực hiện |
| AC-42 | #37 | [TC-F1-AC42-01](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md) | Chưa thực hiện |
| AC-43 | #22, ClassDeleted | [TC-F1-AC43-01](../testcases/modules/F1_identity/F1_extension_a_classes_admin.md), [TC-F1-AC43-02](../testcases/modules/F1_identity/F1_extension_a_classes_admin.md) | Chưa thực hiện |
| AC-44 | #14–15 | [TC-F1-AC44-01](../testcases/modules/F1_identity/F1_extension_a_classes_admin.md), [TC-F1-AC44-02](../testcases/modules/F1_identity/F1_extension_a_classes_admin.md) | Chưa thực hiện |
| AC-45 | #30–31 | [TC-F1-AC45-01](../testcases/modules/F1_identity/F1_extension_a_classes_admin.md), [TC-F1-AC45-02](../testcases/modules/F1_identity/F1_extension_a_classes_admin.md) | Chưa thực hiện |
| AC-46 | Khởi động F1, BR-23 | [TC-F1-AC46-01](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md), [TC-F1-AC46-02](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md) | Chưa thực hiện |
| AC-47 | JOB ẩn danh, BR-08 | [TC-F1-AC47-01](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md), [TC-F1-AC47-03](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md), [TC-F1-AC47-02](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md) | Chưa thực hiện |
| AC-48 | #14–15 | [TC-F1-AC48-01](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md) | Chưa thực hiện |
| AC-49 | #25A–25B | [TC-F1-AC49-01](../testcases/modules/F1_identity/F1_extension_a_classes_admin.md), [TC-F1-AC49-02](../testcases/modules/F1_identity/F1_extension_a_classes_admin.md) | Chưa thực hiện |
| AC-50 | #25 | [TC-F1-AC50-01](../testcases/modules/F1_identity/F1_extension_a_classes_admin.md), [TC-F1-AC50-02](../testcases/modules/F1_identity/F1_extension_a_classes_admin.md) | Chưa thực hiện |
| AC-51 | #56 | [TC-F1-AC51-01](../testcases/modules/F1_identity/F1_extension_a_metrics.md), [TC-F1-AC51-02](../testcases/modules/F1_identity/F1_extension_a_metrics.md) | Chưa thực hiện |
| AC-52 | #57 | [TC-F1-AC52-01](../testcases/modules/F1_identity/F1_extension_a_metrics.md), [TC-F1-AC52-02](../testcases/modules/F1_identity/F1_extension_a_metrics.md), [TC-F1-AC52-03](../testcases/modules/F1_identity/F1_extension_a_metrics.md) | Chưa thực hiện |
| AC-53 | #58 | [TC-F1-AC53-01](../testcases/modules/F1_identity/F1_extension_a_metrics.md), [TC-F1-AC53-02](../testcases/modules/F1_identity/F1_extension_a_metrics.md), [TC-F1-AC53-03](../testcases/modules/F1_identity/F1_extension_a_metrics.md) | Chưa thực hiện |
| AC-54 | #60, #8 | [TC-F1-AC54-01](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md), [TC-F1-AC54-02](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md), [TC-F1-AC54-03](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md) | Chưa thực hiện |
| AC-55 | #61 | [TC-F1-AC55-01](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md), [TC-F1-AC55-02](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md), [TC-F1-AC55-03](../testcases/modules/F1_identity/F1_extension_a_auth_lifecycle.md) | Chưa thực hiện |
| AC-56 | #16 | [TC-F1-AC56-01](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md), [TC-F1-AC56-02](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md) | Chưa thực hiện |
| AC-57 | #17 | [TC-F1-AC57-01](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md), [TC-F1-AC57-02](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md) | Chưa thực hiện |
| AC-58 | #59 | [TC-F1-AC58-01](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md), [TC-F1-AC58-02](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md) | Chưa thực hiện |
| AC-59 | #41–43, #67–73 | [TC-F1-AC59-01](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md), [TC-F1-AC59-02](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md), [TC-F1-AC59-03](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md) | Chưa thực hiện |
| AC-60 | #43, #68–69 | [TC-F1-AC60-01](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md), [TC-F1-AC60-02](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md) | Chưa thực hiện |
| AC-61 | #35, #62–63 | [TC-F1-AC61-01](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md) | Chưa thực hiện |
| AC-62 | #64–66 | [TC-F1-AC62-01](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md), [TC-F1-AC62-02](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md), [TC-F1-AC62-03](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md) | Chưa thực hiện |
| AC-63 | #51–52 | [TC-F1-AC63-01](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md), [TC-F1-AC63-02](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md), [TC-F1-AC63-03](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md) | Chưa thực hiện |
| AC-64 | #49 | [TC-F1-AC64-01](../testcases/modules/F1_identity/F1_extension_b_class_auth.md), [TC-F1-AC64-02](../testcases/modules/F1_identity/F1_extension_b_class_auth.md) | Chưa thực hiện |
| AC-65 | #19–21 | [TC-F1-AC65-01](../testcases/modules/F1_identity/F1_extension_b_class_auth.md), [TC-F1-AC65-02](../testcases/modules/F1_identity/F1_extension_b_class_auth.md) | Chưa thực hiện |
| AC-66 | #22 | [TC-F1-AC66-01](../testcases/modules/F1_identity/F1_extension_b_class_auth.md), [TC-F1-AC66-02](../testcases/modules/F1_identity/F1_extension_b_class_auth.md) | Chưa thực hiện |
| AC-67 | #23–24 | [TC-F1-AC67-01](../testcases/modules/F1_identity/F1_extension_b_class_auth.md), [TC-F1-AC67-02](../testcases/modules/F1_identity/F1_extension_b_class_auth.md) | Chưa thực hiện |
| AC-68 | #5–6 | [TC-F1-AC68-01](../testcases/modules/F1_identity/F1_extension_b_class_auth.md), [TC-F1-AC68-02](../testcases/modules/F1_identity/F1_extension_b_class_auth.md) | Chưa thực hiện |
| AC-69 | #4 | [TC-F1-AC69-01](../testcases/modules/F1_identity/F1_extension_b_class_auth.md) | Chưa thực hiện |
| AC-70 | #11–12 | [TC-F1-AC70-01](../testcases/modules/F1_identity/F1_extension_b_class_auth.md), [TC-F1-AC70-02](../testcases/modules/F1_identity/F1_extension_b_class_auth.md) | Chưa thực hiện |
| AC-71 | #9 | [TC-F1-AC71-01](../testcases/modules/F1_identity/F1_extension_b_class_auth.md) | Chưa thực hiện |
| AC-72 | #19, #26, #28–29 | [TC-F1-AC72-01](../testcases/modules/F1_identity/F1_extension_b_class_auth.md), [TC-F1-AC72-02](../testcases/modules/F1_identity/F1_extension_b_class_auth.md) | Chưa thực hiện |
| AC-73 | #30–33 | [TC-F1-AC73-01](../testcases/modules/F1_identity/F1_extension_b_admin_security.md), [TC-F1-AC73-02](../testcases/modules/F1_identity/F1_extension_b_admin_security.md) | Chưa thực hiện |
| AC-74 | #35, #38 | [TC-F1-AC74-01](../testcases/modules/F1_identity/F1_extension_b_admin_security.md), [TC-F1-AC74-02](../testcases/modules/F1_identity/F1_extension_b_admin_security.md) | Chưa thực hiện |
| AC-75 | #46–48 | [TC-F1-AC75-01](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md), [TC-F1-AC75-02](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md), [TC-F1-AC75-03](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md), [TC-F1-AC75-04](../testcases/modules/F1_identity/F1_extension_b_privacy_metrics.md) | Chưa thực hiện |
| AC-76 | #36 | [TC-F1-AC76-01](../testcases/modules/F1_identity/F1_extension_b_admin_security.md), [TC-F1-AC76-02](../testcases/modules/F1_identity/F1_extension_b_admin_security.md), [TC-F1-AC76-03](../testcases/modules/F1_identity/F1_extension_b_admin_security.md) | Chưa thực hiện |
| AC-77 | #44, #56 | [TC-F1-AC77-01](../testcases/modules/F1_identity/F1_extension_b_admin_security.md) | Chưa thực hiện |
| AC-78 | #39–40 | [TC-F1-AC78-01](../testcases/modules/F1_identity/F1_extension_b_admin_security.md), [TC-F1-AC78-02](../testcases/modules/F1_identity/F1_extension_b_admin_security.md), [TC-F1-AC78-03](../testcases/modules/F1_identity/F1_extension_b_admin_security.md) | Chưa thực hiện |
| AC-01 | #1 | [TC-F1-AC01-01](../testcases/modules/F1_identity/F1_auth_session.md#tc-f1-ac01-01--đăng-ký-tạo-tài-khoản-chờ-xác-thực), [TC-F1-AC01-02](../testcases/modules/F1_identity/F1_auth_failure_boundaries.md), [TC-F1-AC01-03](../testcases/modules/F1_identity/F1_auth_failure_boundaries.md), [TC-F1-AC01-04](../testcases/modules/F1_identity/F1_auth_failure_boundaries.md) | Chưa thực hiện |
| AC-02 | #1 | [TC-F1-AC02-01](../testcases/modules/F1_identity/F1_auth_session.md#tc-f1-ac02-01--đăng-ký-trùng-email-khác-chữ-hoa) | Chưa thực hiện |
| AC-03 | #2 | [TC-F1-AC03-01](../testcases/modules/F1_identity/F1_auth_session.md#tc-f1-ac03-01--đăng-nhập-đúng-tạo-phiên-an-toàn) | Chưa thực hiện |
| AC-04 | #2, BR-01 | [TC-F1-AC04-01](../testcases/modules/F1_identity/F1_auth_session.md#tc-f1-ac04-01--lần-sai-thứ-năm-và-lần-thử-kế-tiếp-trong-cửa-sổ), [TC-F1-AC04-02](../testcases/modules/F1_identity/F1_auth_session.md#tc-f1-ac04-02--phạm-vi-ip-và-cửa-sổ-hết-hạn), [TC-F1-AC04-03](../testcases/modules/F1_identity/F1_auth_session.md#tc-f1-ac04-03--redis-rate-limit-lỗi-vẫn-cho-kiểm-mật-khẩu), [TC-F1-AC04-04](../testcases/modules/F1_identity/F1_auth_failure_boundaries.md), [TC-F1-AC04-05](../testcases/modules/F1_identity/F1_auth_failure_boundaries.md) | Chưa thực hiện |
| AC-05 | #3, BR-02 | [TC-F1-AC05-01](../testcases/modules/F1_identity/F1_auth_session.md#tc-f1-ac05-01--refresh-xoay-vòng-một-lần), [TC-F1-AC05-02](../testcases/modules/F1_identity/F1_auth_failure_boundaries.md), [TC-F1-AC05-03](../testcases/modules/F1_identity/F1_auth_failure_boundaries.md) | Chưa thực hiện |
| AC-06 | #3, BR-02 | [TC-F1-AC06-01](../testcases/modules/F1_identity/F1_auth_session.md#tc-f1-ac06-01--tái-sử-dụng-token-a-thu-hồi-cả-family), [TC-F1-AC06-02](../testcases/modules/F1_identity/F1_auth_failure_boundaries.md) | Chưa thực hiện |
| AC-07 | #3 | [TC-F1-AC07-01](../testcases/modules/F1_identity/F1_auth_session.md#tc-f1-ac07-01--thiếu-header-chống-csrf-không-đổi-phiên) | Chưa thực hiện |
| AC-08 | #7 | [TC-F1-AC08-01](../testcases/modules/F1_identity/F1_auth_session.md#tc-f1-ac08-01--quên-mật-khẩu-không-tiết-lộ-sự-tồn-tại-email), [TC-F1-AC08-02](../testcases/modules/F1_identity/F1_auth_failure_boundaries.md) | Chưa thực hiện |
| AC-09 | #8 | [TC-F1-AC09-01](../testcases/modules/F1_identity/F1_auth_session.md#tc-f1-ac09-01--otp-đúng-đổi-mật-khẩu-và-thu-hồi-phiên-cũ), [TC-F1-AC09-02](../testcases/modules/F1_identity/F1_auth_session.md#tc-f1-ac09-02--otp-reset-tài-khoản-admin-tạo-chuyển-trạng-thái), [TC-F1-AC09-03](../testcases/modules/F1_identity/F1_auth_failure_boundaries.md), [TC-F1-AC09-04](../testcases/modules/F1_identity/F1_auth_failure_boundaries.md) | Chưa thực hiện |
| AC-18b | JOB ẩn danh, BR-08 | [TC-F1-AC18b-01](../testcases/modules/F1_identity/F1_profile_lifecycle.md) | Chưa thực hiện |
| AC-20b | #25 | [TC-F1-AC20b-01](../testcases/modules/F1_identity/F1_class_membership_status.md) | Chưa thực hiện |

## 4. Tham chiếu

`03-dd/api/F1_identity.md`, `03-dd/logic/F1_identity.md`, `03-dd/validation/F1_identity.md`,
`02-bd/security/F1_identity.md`, `02-bd/database/F1_identity.md`.
