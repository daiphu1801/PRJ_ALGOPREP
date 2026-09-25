# DD — Validation module `identity` (F1)

> Chỉ ghi quy tắc validate mà BD **chưa** tự suy ra được — độ mạnh mật khẩu, rate-limit chính xác, độ dài
> trường chưa chốt. Không lặp lại validate đã có sẵn ở Sheet 7.1 các file `02-bd/screens/**` (ví dụ độ dài
> `display_name` 100 ký tự đã chốt ở `02-bd/screens/users/USR0502_profile.md:359`).

## 1. Mật khẩu

Theo đề xuất đã duyệt trước [SoT: `07-review/bd_open_questions_with_solutions_260924.md:384`,
`02-bd/security/identity.md:73-75`]:

| Quy tắc | Giá trị | Mã lỗi |
| :--- | :--- | :--- |
| Độ dài tối thiểu | 8 ký tự | `IDT-V01` |
| Độ dài tối đa | 128 ký tự (khớp Sheet 7.1 `USR0502_profile.md:430-431`) | `IDT-V02` |
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

## 3. Email

- Định dạng: RFC 5322, tối đa 254 ký tự [SoT: `02-bd/security/identity.md:53`, khớp `USR0502_profile.md:360`].
- Không phân biệt hoa/thường khi so trùng unique (`LOWER(email)` trước khi ghi và trước khi tra).

## 4. OTP

- 6 chữ số, sinh ngẫu nhiên bằng `SecureRandom`, không dùng bộ sinh số giả tất định.
- Không log giá trị OTP thật ở bất kỳ mức log nào (kể cả DEBUG) — chỉ log việc "đã gửi"/"đã xác nhận".

## 5. Trường tự do (chưa có ràng buộc độ dài ở BD)

| Trường | Bảng | Giới hạn | Mã lỗi |
| :--- | :--- | :--- | :--- |
| `current_position` | `users` | 100 ký tự [SoT: `02-bd/screens/users/USR0502_profile.md:362`] | `IDT-V07` |
| `target_position` (mới, xem `03-dd/api/identity.md` mục 4 và `07-review/...:130`) | `users` | 100 ký tự, cùng giới hạn `current_position` | `IDT-V08` |
| `school_or_company` | `users` | 150 ký tự [SoT: `02-bd/screens/users/USR0502_profile.md:361`] | `IDT-V09` |
| Ghi chú lớp (`schedule_note`) | `classes` | 500 ký tự — chưa có nguồn nào chốt, đề xuất mới `[SoT: Suy luận]` vì đủ cho một dòng lịch học, không cần dài như ghi chú bookmark (1000 ký tự đã chốt ở `problem-bank`) | `IDT-V10` |
| Xác nhận email tự xoá tài khoản (`emailConfirmation`) | không lưu | phải khớp tuyệt đối `users.email` hiện tại (case-sensitive theo giá trị đã lưu, không lower-case hoá để người dùng gõ đúng những gì họ thấy) | `IDT-201` |

## 6. Token/Session

- `X-Requested-With` bắt buộc trên `POST /auth/refresh`, thiếu header → `400 IDT-V11`
  [SoT: `02-bd/security/identity.md:16-18`].
- `state` OAuth: chuỗi ngẫu nhiên ≥ 32 byte trước khi encode base64url, TTL Redis 10 phút.

## Tham chiếu

`02-bd/security/identity.md`, `03-dd/api/identity.md`, `03-dd/logic/identity.md`,
`07-review/bd_open_questions_with_solutions_260924.md`.
