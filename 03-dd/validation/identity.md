# DD — Validation module `identity` (F1)

> Chỉ ghi quy tắc validate mà BD **chưa** tự suy ra được — độ mạnh mật khẩu, rate-limit chính xác, độ dài
> trường chưa chốt. Không lặp lại validate đã có sẵn ở Sheet 7.1 các file `02-bd/screens/**` (ví dụ độ dài
> `display_name` 100 ký tự đã chốt ở `02-bd/screens/users/USR0502_profile.md:350`).

## 1. Mật khẩu

Theo đề xuất đã duyệt trước [SoT: `07-review/bd_open_questions_with_solutions_260924.md:384`,
`02-bd/security/identity.md:73-75`]:

| Quy tắc | Giá trị | Mã lỗi |
| :--- | :--- | :--- |
| Độ dài tối thiểu | 8 ký tự | `IDT-V01` |
| Độ dài tối đa | 128 ký tự (khớp ô nhập mật khẩu mới ở `USR0503_settings.md:522`, trước 2026-10-20 ở USR0502) | `IDT-V02` |
| Thành phần bắt buộc | ít nhất 1 chữ cái, ít nhất 1 chữ số | `IDT-V03` |
| Không được trùng | không được trùng chính email/`display_name` của tài khoản (chống mật khẩu đoán ngay) | `IDT-V04` |

Áp dụng cho: `POST /auth/register`, `POST /auth/password/reset`, `POST /me/password` (cả hai biến thể
BR-05/BR-06 ở `03-dd/logic/identity.md`).

## 2. Rate-limit

| Luồng | Ngưỡng | Khoá | Mã lỗi |
| :--- | :--- | :--- | :--- |
| Đăng nhập sai | 5 lần / 15 phút | `(email, IP)` | `IDT-103` (`429`) |
| Gửi lại OTP quên mật khẩu | 1 lần / 60 giây | `userId` | đã có ở BD, `02-bd/database/identity.md:135` |
| Sai OTP quên mật khẩu | tối đa 5 lần trước khi buộc gửi lại | `userId` | đã có ở BD, `02-bd/database/identity.md:134` |
| Đổi email (đã đăng nhập) | 3 lần / 24 giờ | `userId` | `IDT-V05` (`429`) — mới, đề xuất tại DD vì BD chưa nói tới ca đã đăng nhập [SoT: `07-review/bd_open_questions_with_solutions_260924.md:136`] |
| Xuất dữ liệu cá nhân (`/me/export*`) | 1 lần / 5 phút / loại tệp | `userId + scope` | `IDT-V06` (`429`) [SoT: `07-review/...:148`] |
| Gửi lại OTP xác thực email, chỉ tài khoản tự đăng ký (`POST /auth/email/verify/resend`; Sửa 2026-10-22 (đồng bộ DB vòng 2, DEC-2026-1022-db-sync-round2)) | 1 lần / 60 giây | `userId` (khoá Redis `identity:emailverify:resend:<userId>`) | `IDT-V12` (`429`) — **đề xuất phase 5**, theo mẫu gửi lại OTP quên mật khẩu `[SoT: Suy luận]`; `02-bd/database/identity.md:221` |
| Sai OTP xác thực email | tối đa 5 lần trước khi buộc gửi lại | `userId` (bộ đếm trong khoá `identity:emailverify:<userId>`) | `422 IDT-112` (`03-dd/api/identity.md` mục 1) `[SoT: Suy luận]`, theo mẫu OTP quên mật khẩu |

## 3. Email

- Định dạng: RFC 5322, tối đa 254 ký tự [SoT: `02-bd/security/identity.md:53`, khớp `USR0502_profile.md:351`]. DB: `users.email VARCHAR(254) NOT NULL`
  [SoT: `02-bd/database/identity.md:14`]; ô nhập của `ADM0201` còn ghi 255 là lệch BD (ID-14), DD theo 254.
- Không phân biệt hoa/thường khi so trùng unique (`LOWER(email)` trước khi ghi và trước khi tra); DB có chỉ mục
  `UNIQUE (lower(email))` nên ghi `LOWER(email)` ở tầng ứng dụng và ràng buộc DB khớp nhau (phase 5, ID-14).

## 4. OTP

- 6 chữ số, sinh ngẫu nhiên bằng `SecureRandom`, không dùng bộ sinh số giả tất định.
- Không log giá trị OTP thật ở bất kỳ mức log nào (kể cả DEBUG) — chỉ log việc "đã gửi"/"đã xác nhận".

## 5. Trường tự do (chưa có ràng buộc độ dài ở BD)

| Trường | Bảng | Giới hạn | Mã lỗi |
| :--- | :--- | :--- | :--- |
| `current_position` | `users` | 100 ký tự [SoT: `02-bd/screens/users/USR0502_profile.md:353`] | `IDT-V07` |
| `target_position` (mới, xem `03-dd/api/identity.md` mục 4 và `07-review/...:130`) | `users` | 100 ký tự, cùng giới hạn `current_position` | `IDT-V08` |
| `school_or_company` | `users` | 150 ký tự [SoT: `02-bd/screens/users/USR0502_profile.md:352`] | `IDT-V09` |
| ~~Ghi chú lớp (`schedule_note`)~~ — **đã thay 2026-10-11** bởi lịch học có cấu trúc `classes.schedule` (`DEC-2026-1011-class-schedule-structured`, `02-bd/database/identity.md` mục 1.14); giới hạn 500 ký tự cũ không còn áp dụng | `classes` | Quy tắc mới, mọi phần tuỳ chọn: `note` tối đa 200 ký tự; `room` tối đa 100 ký tự; `days` là số nguyên 1 tới 7, không trùng; `startTime`/`endTime` dạng `HH:mm`, đi cùng nhau, kết thúc sau bắt đầu; có giờ thì phải có ít nhất một ngày; `startDate`/`endDate` dạng `yyyy-MM-dd`, ngày kết thúc không trước ngày bắt đầu. Hai giới hạn 100 và 200 là đề xuất của chủ dự án, **chưa chốt** `[SoT: Suy luận]` | `IDT-V10` (giữ mã cho giới hạn độ dài `room`/`note`; các quy tắc còn lại chờ DD `INS0201`/`INS0205` cấp mã) |
| Số điện thoại (`phone`, F1-09) — **thêm phase 5, ID-28** | `users` | `VARCHAR(20)`, không bắt buộc, không xác thực với nhà mạng [SoT: `02-bd/database/identity.md:22`]; DD kiểm: bỏ khoảng trắng đầu/cuối, chỉ cho chữ số, khoảng trắng và `+ - ( )`, độ dài ≤ 20 `[SoT: Suy luận — BD chỉ nêu độ dài 20, tập ký tự do DD đề xuất]`. Chuỗi rỗng lưu `NULL` | `IDT-V13` (mã mới, đề xuất) |
| Tên lớp (`classes.name`) — **thêm phase 5, ID-26** | `classes` | bắt buộc, 1 tới 120 ký tự sau khi bỏ khoảng trắng đầu/cuối [SoT: `02-bd/database/identity.md:109`, `02-bd/screens/teacher/INS0201_class_management.md`] | `IDT-V14` (mã mới, đề xuất) |
| Mô tả lớp (`classes.description`) — **thêm phase 5, ID-26** | `classes` | tuỳ chọn, tối đa 500 ký tự [SoT: `02-bd/database/identity.md:109`] | `IDT-V15` (mã mới, đề xuất) |
| Xác nhận email tự xoá tài khoản (`emailConfirmation`) | không lưu | phải khớp tuyệt đối `users.email` hiện tại (case-sensitive theo giá trị đã lưu, không lower-case hoá để người dùng gõ đúng những gì họ thấy) | `IDT-201` |

## 6. Token/Session

- `X-Requested-With` bắt buộc trên `POST /auth/refresh`, thiếu header → `400 IDT-V11`
  [SoT: `02-bd/security/identity.md:16-18`].
- `state` OAuth: chuỗi ngẫu nhiên ≥ 32 byte trước khi encode base64url, TTL Redis 10 phút.

## 7. Chặn đăng nhập theo trạng thái tài khoản

**Sửa 2026-10-22 (đồng bộ DB vòng 3, DEC-2026-1022-self-register-pending-login-blocked).** Sau khi mật khẩu khớp và qua BR-01, `POST /auth/login` (#2) và `/auth/instructor/login` (#49) kiểm trạng thái theo thứ tự (BR-21):

| Điều kiện | Kết quả | Mã lỗi |
| :--- | :--- | :--- |
| `status = DEACTIVATED` | chặn | `403 IDT-104` |
| `status = PENDING_VERIFICATION` và `must_change_password = false` (tự đăng ký chưa nhập OTP) | chặn, không phát token | `403 IDT-113` `[SoT: Suy luận — mã đề xuất]` |
| `status = PENDING_VERIFICATION` và `must_change_password = true` (Admin tạo) | cho vào, chuyển `ACTIVE` | không lỗi |

## 8. Tuỳ chọn phỏng vấn giả lập trong `PATCH /me/settings`

**Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4):** (ID-23) bảng giá trị hợp lệ của hai trường mới. Nguồn giá trị: ba nút 8/12/16 và công tắc ở `02-bd/screens/users/USR0503_settings.md:425-426`.

| Trường | Quy tắc | Mã lỗi |
| :--- | :--- | :--- |
| `preferredMaxTurns` | Số nguyên, một trong `8`, `12`, `16` | `422` `[SoT: Suy luận — chưa có mã IDT-V riêng, dùng mã kiểm nhập chung của PATCH /me/settings]` |
| `preferredHintAllowed` | Boolean | như trên |
| `preferredInterviewLevel` | Một trong `INTERN`, `JUNIOR`, `MIDDLE`, `SENIOR` (khớp ENUM DB) | như trên |

## Tham chiếu

`02-bd/security/identity.md`, `03-dd/api/identity.md`, `03-dd/logic/identity.md`,
`07-review/bd_open_questions_with_solutions_260924.md`.
