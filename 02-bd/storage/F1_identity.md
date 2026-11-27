# BD — Storage module `identity` (F1)

> Bổ sung 2026-11-17 theo `DEC-2026-1117-user-avatar-upload` (`.nexa/control/decision-registry.md`) và yêu cầu F1-35 (`01-rd/req/F1_identity.md`). Trước đó `identity` không có file `storage` vì không lưu tệp [SoT: `02-bd/storage/F2_problem-bank.md:3-5`]; nay có, do ảnh đại diện (avatar).
> Đọc cùng `02-bd/database/F1_identity.md` mục 1.16 (bảng `user_avatars`) và `02-bd/security/F1_identity.md` mục 6 (kiểm tra tệp, chống SSRF).

## 1. Ranh giới PostgreSQL và MinIO

Owner chốt ở D1: byte ảnh nằm trong MinIO, PostgreSQL chỉ giữ siêu dữ liệu [SoT: `.nexa/control/decision-registry.md`, `DEC-2026-1117-user-avatar-upload`].

| Nơi | Giữ gì | Ghi chú |
| :--- | :--- | :--- |
| PostgreSQL `identity.user_avatars` | Khoá đối tượng, kiểu nội dung, kích thước, thời điểm cập nhật | Một dòng cho mỗi người có ảnh; không có ảnh thì không có dòng (mục 1.16 của file database) |
| MinIO | Byte của ảnh | Mọi ảnh vào MinIO, **không có ngưỡng kích thước** như F2 (8 KB): ảnh tối đa 512 KB nên chọn một đường duy nhất, không rẽ nhánh theo cỡ |

## 2. Bucket và khoá đối tượng

| Mục | Giá trị | Ghi chú |
| :--- | :--- | :--- |
| Bucket | `algoprep-avatars`, cấu hình bằng biến `MINIO_BUCKET_AVATAR` | Bucket **riêng**, không dùng chung `algoprep-testcases` (`05-coding/backend/algoprep-bootstrap/src/main/resources/application.yml:147`). `[SoT: Suy luận]` F2 gom một bucket vì "`problem-bank` là module duy nhất cần MinIO" (`02-bd/storage/F2_problem-bank.md:22-25`); lý do đó không còn đúng. Bucket riêng cho chính sách truy cập và vòng đời độc lập, không phải sửa F2 |
| Khoá đối tượng | `users/<user_id>/<avatar_id>.<ext>` | `avatar_id` là UUID mới sinh **mỗi lần đổi ảnh**; `ext` thuộc `png`, `jpg`, `webp` suy từ định dạng phát hiện được, **không** lấy từ tên tệp người dùng |
| Số ảnh hiện hành | 1 cho mỗi người | Ảnh cũ bị xoá khi thay (mục 4) |

Khoá mới ở mỗi lần đổi ảnh cho ba lợi ích: (a) thay ảnh là ghi khoá mới, cập nhật dòng, rồi xoá khoá cũ, nên không có lúc nào người đọc nhận nửa ảnh cũ nửa ảnh mới; (b) khoá cũng là dấu phân biệt phiên bản để làm ETag và phá cache ở trình duyệt; (c) liệt kê theo prefix `users/<user_id>/` đủ để dọn tay ảnh mồ côi của một người `[SoT: Suy luận]`.

## 3. Đọc ảnh: không dùng presigned URL

| Phương án | Quyết định | Lý do |
| :--- | :--- | :--- |
| URL ký sẵn trỏ thẳng MinIO (như testcase ở `02-bd/storage/F2_problem-bank.md` mục 3) | **Không dùng** | Trình duyệt phải với tới MinIO, URL có hạn nên mỗi DTO có người phải nhúng URL hết hạn; khó cache |
| Đọc qua API của `identity`, backend đọc MinIO rồi trả byte | **Dùng** | Cần kiểm bearer token cho mọi lượt đọc (owner chốt mọi người dùng đã đăng nhập đều đọc được, D2b), và giữ MinIO ngoài đường truy cập của trình duyệt |

Cache: ETag lấy từ `avatar_id` trong khoá đối tượng; `Cache-Control: private` (ảnh chỉ cho người đã đăng nhập). Cách DTO mang phiên bản ảnh để làm mới cache (`avatarVersion`) do DD chốt (`03-dd/api/F1_identity.md`).

## 4. Vòng đời đối tượng

Thứ tự ghi luôn là **MinIO trước, PostgreSQL sau, xoá đối tượng cũ cuối cùng**, để lỗi ở bước nào cũng không để dòng DB trỏ vào đối tượng không có.

| Thao tác | MinIO | PostgreSQL | Khi lỗi giữa chừng |
| :--- | :--- | :--- | :--- |
| Tải ảnh lần đầu (tay hoặc nhập từ OAuth) | Ghi khoá mới | Thêm dòng `user_avatars` | Thêm dòng lỗi thì xoá đối tượng vừa ghi (cố gắng hết sức) |
| Thay ảnh | Ghi khoá mới | Cập nhật dòng sang khoá mới | Cập nhật dòng lỗi thì xoá đối tượng vừa ghi; ảnh cũ giữ nguyên |
| Sau khi thay thành công | Xoá khoá cũ **sau khi commit** | — | Xoá lỗi chỉ ghi log WARN kèm khoá, không làm lỗi thao tác |
| Xoá ảnh | Xoá khoá hiện tại **sau khi commit** | Xoá dòng | Như trên |
| Bấm xoá tài khoản (F1-16) | Xoá khoá **sau khi commit** | Xoá dòng, cùng giao dịch đặt `DEACTIVATED` | Xoá đối tượng lỗi chỉ ghi log WARN; việc xoá tài khoản vẫn hoàn tất |
| Quản trị viên gỡ ảnh người khác | Xoá khoá **sau khi commit** | Xoá dòng; ghi `system_audit_logs` (`USER_AVATAR_REMOVED`) | Như trên |

**Đã chốt 2026-11-17 (owner, Q-AV-1):** ảnh bị xoá **ngay khi bấm xoá tài khoản**, không đợi ẩn danh hoá. Tài khoản khôi phục trong 30 ngày ân hạn sẽ không còn ảnh, người dùng tự tải lại. Vì ảnh đã xoá từ đầu nên API đọc ảnh của tài khoản `DEACTIVATED` trả "không tìm thấy" như người chưa có ảnh (`02-bd/security/F1_identity.md` mục 6.3).

**Đối tượng mồ côi (ghi nợ, ponytail bậc 3):** lỗi xoá đối tượng chỉ để lại tệp mồ côi, không ảnh hưởng đúng/sai. Lượt này không làm job dọn; khi cần thì liệt kê theo prefix `users/` và đối chiếu `user_avatars.object_key`.

## 5. Khi MinIO lỗi hoặc tạm dừng

`identity` không được phụ thuộc cứng vào MinIO, cùng nguyên tắc suy giảm có kiểm soát của F5/F6 (`CLAUDE.md`, mục Rules).

| Tình huống | Hành vi |
| :--- | :--- |
| Đăng nhập, đọc hồ sơ, mọi API F1 khác | Không đổi, không gọi MinIO |
| Đọc ảnh một người | API trả lỗi dịch vụ không sẵn sàng; giao diện rơi về chữ viết tắt |
| Tải ảnh, thay, xoá | Trả lỗi dịch vụ không sẵn sàng; ảnh cũ giữ nguyên; không đổi dòng DB |
| Nhập ảnh khi liên kết OAuth | Bỏ qua, ghi log WARN, đăng nhập vẫn hoàn tất (`02-bd/security/F1_identity.md` mục 6.5) |

## 6. Phụ thuộc kỹ thuật cần DD/kiến trúc chốt

- Thư viện máy khách MinIO hiện chỉ khai báo ở `05-coding/backend/algoprep-problem-bank/pom.xml:31`; `identity` cần phụ thuộc riêng hoặc dùng chung qua `algoprep-common`. `[SoT: Suy luận]` theo baseline `DEC-2026-0820-architecture-baseline`, truy cập MinIO là một cổng ra (`domain/ports/out`) do `identity` khai báo và một adapter ở tầng hạ tầng; tên cổng do DD đặt.
- Biến môi trường `MINIO_BUCKET_AVATAR` và việc tạo bucket khi khởi động (script `minio-init` của Docker Compose) cần cập nhật cùng lượt triển khai.

## 7. Việc còn mở

| # | Câu hỏi | Chủ | Ghi chú |
| :--- | :--- | :--- | :--- |
| Q-AV-1 | ~~Ảnh xoá ở bước ẩn danh hoá hay ngay khi bấm xoá tài khoản~~ **Đã chốt 2026-11-17:** ngay khi bấm xoá tài khoản | Owner | Mục 4 |
| Q-AV-2 | ~~Ảnh nhà cung cấp lớn hơn 512 KB~~ **Đã chốt 2026-11-17:** xin bản 256 điểm ảnh, vẫn quá thì bỏ qua | Owner | `02-bd/security/F1_identity.md` mục 6.5 |
| Q-AV-3 | Job dọn đối tượng mồ côi | Nhóm phát triển | Hoãn, bậc 3 |
