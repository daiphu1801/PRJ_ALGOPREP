# DD — Validation module `identity` (F1)

> Chỉ ghi quy tắc validate mà BD **chưa** tự suy ra được — độ mạnh mật khẩu, rate-limit chính xác, độ dài
> trường chưa chốt. Không lặp lại validate đã có sẵn ở Sheet 7.1 các file `02-bd/screens/**` (ví dụ độ dài
> `display_name` 100 ký tự đã chốt ở `02-bd/screens/users/USR0502_profile.md:350`).

## 1. Mật khẩu

Theo đề xuất đã duyệt trước [SoT: `07-review/bd_open_questions_with_solutions_260924.md:384`,
`02-bd/security/F1_identity.md:73-75`]:

| Quy tắc | Giá trị | Mã lỗi |
| :--- | :--- | :--- |
| Độ dài tối thiểu | 8 ký tự | `IDT-V01` |
| Độ dài tối đa | 128 ký tự (khớp ô nhập mật khẩu mới ở `USR0503_settings.md:522`, trước 2026-10-20 ở USR0502) | `IDT-V02` |
| Thành phần bắt buộc | ít nhất 1 chữ cái, ít nhất 1 chữ số | `IDT-V03` |
| Không được trùng | không được trùng chính email/`display_name` của tài khoản (chống mật khẩu đoán ngay) | `IDT-V04` |

`POST /me/password` với tài khoản đã có mật khẩu kiểm `currentPassword` bằng BCrypt sau khi qua validation request; giá trị sai trả **`422 IDT-115`**, không dùng mã OTP `IDT-107` hoặc mã đăng nhập `IDT-102` (owner chốt 2026-10-27).

Áp dụng cho: `POST /auth/register`, `POST /auth/password/reset`, `POST /me/password` (cả hai biến thể
BR-05/BR-06 ở `03-dd/logic/F1_identity.md`).

## 2. Rate-limit

| Luồng | Ngưỡng | Khoá | Mã lỗi |
| :--- | :--- | :--- | :--- |
| Đăng nhập sai | 5 lần / 15 phút | `(email, IP)` | `IDT-103` (`429`) |
| Gửi lại OTP quên mật khẩu | 1 lần / 60 giây | `userId` | đã có ở BD, `02-bd/database/F1_identity.md:135` |
| Sai OTP quên mật khẩu | tối đa 5 lần trước khi buộc gửi lại | `userId` | đã có ở BD, `02-bd/database/F1_identity.md:134` |
| Đổi email (đã đăng nhập) | 3 lần / 24 giờ, **tính cả gửi lại OTP** qua cùng #11 | `userId` | `IDT-V05` (`429`); owner chốt `DEC-2026-1025-dd-email-resend-budget` |
| Xuất dữ liệu cá nhân (`/me/export*`) | 1 lần / 5 phút / loại tệp | `userId + scope` | `IDT-V06` (`429`) [SoT: `07-review/...:148`] |
| Gửi lại OTP xác thực email, chỉ tài khoản tự đăng ký (`POST /auth/email/verify/resend`; Sửa 2026-10-22 (đồng bộ DB vòng 2, DEC-2026-1022-db-sync-round2)) | 1 lần / 60 giây | `userId` (khoá Redis `identity:emailverify:resend:<userId>`) | `IDT-V12` (`429`) — **đề xuất phase 5**, theo mẫu gửi lại OTP quên mật khẩu `[SoT: Suy luận]`; `02-bd/database/F1_identity.md:221` |
| Sai OTP xác thực email | tối đa 5 lần trước khi buộc gửi lại | `userId` (bộ đếm trong khoá `identity:emailverify:<userId>`) | `422 IDT-112` (`03-dd/api/F1_identity.md` mục 1) `[SoT: Suy luận]`, theo mẫu OTP quên mật khẩu |
| Thử mã mời lớp sai | 5 lần / 15 phút trượt; từ lần kế tiếp chặn đến hết cửa sổ, thành công xoá bộ đếm | `identity:class-join:failed:<userId>` trong Redis | `429 IDT-412` (`DEC-2026-1024-dd-student-class-contracts`); mã sai thứ 1–5 là `422 IDT-410` |

## 3. Email

- Định dạng: RFC 5322, tối đa 254 ký tự [SoT: `02-bd/security/F1_identity.md:53`, khớp `USR0502_profile.md:351`]. DB: `users.email VARCHAR(254) NOT NULL`
  [SoT: `02-bd/database/F1_identity.md:14`]; ô nhập của `ADM0201` còn ghi 255 là lệch BD (ID-14), DD theo 254.
- Không phân biệt hoa/thường khi so trùng unique (`LOWER(email)` trước khi ghi và trước khi tra); DB có chỉ mục
  `UNIQUE (lower(email))` nên ghi `LOWER(email)` ở tầng ứng dụng và ràng buộc DB khớp nhau (phase 5, ID-14).

## 4. OTP

Endpoint #60 kiểm OTP quên mật khẩu trước khi đặt mật khẩu: `email` hợp lệ, `otp` đúng sáu chữ số; dùng cùng Redis TTL 10 phút và tối đa 5 lần sai với #8, đúng không tiêu thụ mã, sai/hết hạn trả chung `422 IDT-107`. Endpoint #61 nhận `restoreTicket` opaque không rỗng, chỉ chấp nhận vé chưa dùng còn hạn 5 phút của tài khoản `SELF` trong ân hạn; mọi sai lệch trả chung `401 IDT-114` (`DEC-2026-1025-dd-auth-recovery-contracts`).

- 6 chữ số, sinh ngẫu nhiên bằng `SecureRandom`, không dùng bộ sinh số giả tất định.
- Không log giá trị OTP thật ở bất kỳ mức log nào (kể cả DEBUG) — chỉ log việc "đã gửi"/"đã xác nhận".

## 5. Trường tự do (chưa có ràng buộc độ dài ở BD)

| Trường | Bảng | Giới hạn | Mã lỗi |
| :--- | :--- | :--- | :--- |
| `current_position` | `users` | 100 ký tự [SoT: `02-bd/screens/users/USR0502_profile.md:353`] | `IDT-V07` |
| `display_name` | `users` | 2–100 ký tự sau trim; áp cho đăng ký và PATCH hồ sơ, khớp BD USR0502 Sheet 9 | `IDT-V19` nếu ngắn, `IDT-V20` nếu dài |
| `target_position` (mới, xem `03-dd/api/F1_identity.md` mục 4 và `07-review/...:130`) | `users` | 100 ký tự, cùng giới hạn `current_position` | `IDT-V08` |
| `school_or_company` | `users` | 150 ký tự [SoT: `02-bd/screens/users/USR0502_profile.md:352`] | `IDT-V09` |
| Lịch học có cấu trúc `classes.schedule` | `classes` | Mọi phần tuỳ chọn: `note` tối đa 200 ký tự; `room` tối đa 100 ký tự; `days` là số nguyên 1 tới 7, không trùng; `startTime`/`endTime` dạng `HH:mm`, đi cùng nhau, kết thúc sau bắt đầu; có giờ thì phải có ít nhất một ngày; `startDate`/`endDate` dạng `yyyy-MM-dd`, ngày kết thúc không trước ngày bắt đầu. | `IDT-V10` cho quá dài `room`/`note`; `IDT-V16` cho cấu trúc/ngày/giờ sai. Các giới hạn theo BD INS0201/INS0205 Sheet 9; mã V16 chốt tại DD. |
| Số điện thoại (`phone`, F1-09) — **thêm phase 5, ID-28** | `users` | `VARCHAR(20)`, không bắt buộc, không xác thực với nhà mạng [SoT: `02-bd/database/F1_identity.md:22`]; DD kiểm: bỏ khoảng trắng đầu/cuối, chỉ cho chữ số, khoảng trắng và `+ - ( )`, độ dài ≤ 20 `[SoT: Suy luận — BD chỉ nêu độ dài 20, tập ký tự do DD đề xuất]`. Chuỗi rỗng lưu `NULL` | `IDT-V13` (mã mới, đề xuất) |
| Tên lớp (`classes.name`) — **thêm phase 5, ID-26** | `classes` | bắt buộc, 1 tới 120 ký tự sau khi bỏ khoảng trắng đầu/cuối [SoT: `02-bd/database/F1_identity.md:109`, `02-bd/screens/teacher/INS0201_class_management.md`] | `IDT-V14` (mã mới, đề xuất) |
| Mô tả lớp (`classes.description`) — **thêm phase 5, ID-26** | `classes` | tuỳ chọn, tối đa 500 ký tự [SoT: `02-bd/database/F1_identity.md:109`] | `IDT-V15` (mã mới, đề xuất) |
| Xác nhận email tự xoá tài khoản (`emailConfirmation`) | không lưu | phải khớp tuyệt đối `users.email` hiện tại (case-sensitive theo giá trị đã lưu, không lower-case hoá để người dùng gõ đúng những gì họ thấy) | `IDT-201` |

## 6. Token/Session

- `X-Requested-With` bắt buộc trên `POST /auth/refresh`, thiếu header → `400 IDT-V11`
  [SoT: `02-bd/security/F1_identity.md:16-18`].
- `state` OAuth: chuỗi ngẫu nhiên ≥ 32 byte trước khi encode base64url, TTL Redis 10 phút.

## 7. Chặn đăng nhập theo trạng thái tài khoản

**Sửa 2026-10-22 (đồng bộ DB vòng 3, DEC-2026-1022-self-register-pending-login-blocked).** Sau khi mật khẩu khớp và qua BR-01, `POST /auth/login` (#2) và `/auth/instructor/login` (#49) kiểm trạng thái theo thứ tự (BR-21):

| Điều kiện | Kết quả | Mã lỗi |
| :--- | :--- | :--- |
| `status = DEACTIVATED` | chặn | `403 IDT-104` |
| `status = PENDING_VERIFICATION` và `must_change_password = false` (tự đăng ký chưa nhập OTP) | chặn, không phát token | `403 IDT-113` `[SoT: Suy luận — mã đề xuất]` |
| `status = PENDING_VERIFICATION` và `must_change_password = true` (Admin tạo) | cho vào, chuyển `ACTIVE` | không lỗi |

## 8. Tuỳ chọn trong `PATCH /me/settings`

**Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4):** (ID-23) bảng giá trị hợp lệ của hai trường mới. Nguồn giá trị: ba nút 8/12/16 và công tắc ở `02-bd/screens/users/USR0503_settings.md:425-426`.

| Trường | Quy tắc | Mã lỗi |
| :--- | :--- | :--- |
| `preferredMaxTurns` | Số nguyên, một trong `8`, `12`, `16` | `422 IDT-V18` |
| `preferredHintAllowed` | Boolean | `422 IDT-V18` |
| `preferredInterviewLevel` | Một trong `INTERN`, `JUNIOR`, `MIDDLE`, `SENIOR` (khớp ENUM DB) | `422 IDT-V18` |
| `defaultLanguage` | Một trong `JAVA`, `CPP`, `PYTHON`; trường vắng mặt giữ nguyên. Trạng thái bật/tắt thuộc F4 và client chỉ cho chọn mã đang bật; F1 không đọc schema F4 | `422 IDT-413` nếu mã không hợp lệ (`DEC-2026-1024-dd-student-language-contracts`) |

## 9. Tham gia lớp bằng mã mời

Client cắt khoảng trắng hai đầu, đổi chữ hoa; server lặp lại chuẩn hoá để không tin client. Mã sau chuẩn hoá phải gồm đúng 8 ký tự `A-Z0-9`; sai dạng, không tồn tại và hết hạn cùng `422 IDT-410`, không phân biệt. Ô client nhận tối đa 16 ký tự (gồm khoảng trắng trước khi cắt), rỗng sau cắt thì không gửi. `classId` ở `GetMyClass` là UUID; sai dạng và ngoài phạm vi cùng `404 IDT-411`. Nguồn: `02-bd/screens/users/USR0701_my_classes.md` Sheet 9, `USR0702_my_class_detail.md` Sheet 9.

## 10. Đọc tổng hợp lớp của giảng viên (#56–58)

JWT INSTRUCTOR và `CLASS_MANAGEMENT:READ` bắt buộc. `classId` ở #57/#58 là UUID tuỳ chọn; sai dạng, lớp không có hoặc lớp ngoài phạm vi cùng `404 IDT-414`. Không nhận `instructorId` hoặc `studentId` để chọn người khác. #57 trả đúng sáu tuần ISO đã kết thúc, không gồm tuần hiện tại. #58 giới hạn năm dòng, không nhận `limit` từ client; từ khoá bảng học viên không lọc biểu đồ hay khối cần chú ý. Nguồn: `DEC-2026-1024-dd-teacher-f1-read-contracts`, `02-bd/screens/teacher/INS0203_class_progress.md` Sheet 8–9.

## 11. Danh sách học viên trên các lớp phụ trách (#59)

JWT INSTRUCTOR và `CLASS_MANAGEMENT:READ`; `classId` UUID tuỳ chọn, sai dạng hoặc ngoài phạm vi trả cùng `404 IDT-414`. `q` trim tối đa 100 ký tự; `page >= 1`, `pageSize` từ 1 đến 100, mặc định 1 và 20; sai `q`/phân trang trả `400 IDT-V17`. Chỉ ghi danh còn hiệu lực (`left_at IS NULL`), mỗi cặp lớp–học viên là một dòng. F1 phân trang sau khi lọc trên toàn bộ lớp phụ trách, trước khi ghép số liệu; không phân trang từng lớp rồi ghép ở client. Nguồn: `DEC-2026-1024-dd-teacher-student-list`.

## 12. Bộ lọc và xuất nhật ký hệ thống (#39–40)

| Trường | Kiểm phía client và server | Mã lỗi |
| :--- | :--- | :--- |
| `q` | Trim, tối đa 100 ký tự; rỗng nghĩa là không lọc | `IDT-444` |
| `categoryCode` | `ALL|AUTH|PERMISSION|CONFIG|CONTENT`; `ALL` không lọc | `IDT-443` |
| `dateFrom`, `dateTo` | `YYYY-MM-DD` hợp lệ theo UTC+7; cùng ngày được nhận, ngày tương lai/bắt đầu sau kết thúc/trước cửa sổ `retentionDays` bị từ chối | `IDT-443` |
| `cursor` | Opaque do #39 cấp; không tự dựng từ thời gian trên client | `IDT-443` |
| `columns` (#40) | Tập không rỗng, không trùng, chỉ `time|actor|service|category|content|id` | `IDT-443` |

Server kiểm lại toàn bộ tham số của cả tìm và xuất, kể cả khi client đã chặn; nội dung lỗi không lộ log. [SoT: BD ADM0403 Sheet 9 NO 3–6, 10; API F1 §0.7.2]

## 13. Xác nhận ADMIN tự ảnh hưởng (#36–37)

Khi `userId` đích bằng actor và yêu cầu khoá chính mình (`targetStatus=DEACTIVATED`) hoặc đổi `targetRoleId` sang role có `base_category != ADMIN`, request bắt buộc `acknowledgeSelfImpact: true` (boolean). Thiếu, `null`, `false` hoặc sai kiểu trả `400 IDT-V21`; không ghi dữ liệu hoặc phát email/audit. Bất biến ADMIN hoạt động cuối cùng dùng `422 IDT-432` và được kiểm trước nhánh xác nhận. Các thao tác khác không bắt buộc cờ này. [SoT: quyết định chủ dự án 2026-10-27; API F1 §0.6, logic BR-15].

## Tham chiếu

`02-bd/security/F1_identity.md`, `03-dd/api/F1_identity.md`, `03-dd/logic/F1_identity.md`,
`07-review/bd_open_questions_with_solutions_260924.md`.
