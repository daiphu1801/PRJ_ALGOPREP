# RD (Yêu cầu hệ thống mới) — Hồ sơ / `USR0502`

> Mã màn hình: `USR0502`, theo `02-bd/_rules/bd-template-9sheet.md` mục 8.
> Slug chính tắc: `profile`, khớp `01-rd/overview/system_survey.md` mục 7.1 dòng `profile`.
> Phạm vi/Bounded Context: `identity` (F1). Actor chính: A1; A2/A3 dùng chung cấu trúc màn và khác biệt (nếu có) được xử lý khi viết BD.
> Nguồn sự thật (SoT): `01-rd/req/identity.md` (F1-09, F1-19), `01-rd/req/user_stories/a1_student.md` (`US-A1-05`) và `09-layoutBase/Trang cá nhân.dc.html`.
> Tài liệu này mô tả hành vi và UX mức yêu cầu của màn hình, không lặp lại đặc tả chức năng nguồn.
> Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

Màn `profile` cho phép xem và sửa thông tin cá nhân, quản lý bảo mật tài khoản ở mức cơ bản, xem tóm tắt luyện tập và đi nhanh tới Cài đặt [SoT: `01-rd/req/identity.md` — F1-09].

---

## 2. Phạm vi

| # | Phạm vi | Nội dung | Nguồn/SoT |
|---|---|---|---|
| S-1 | Khối định danh | Avatar chữ viết tắt, tên, email và nút “Đổi ảnh”; nút đổi ảnh trong prototype chỉ là placeholder, chưa là input file thật. | `09-layoutBase/Trang cá nhân.dc.html`:101-110 |
| S-2 | Thông tin cá nhân | Xem và cập nhật Họ và tên, Email, Trường/công ty, Vai trò hiện tại, Ngôn ngữ mặc định, Vị trí mục tiêu; có Hoàn tác, Lưu thay đổi, trạng thái thay đổi chưa lưu và thông báo cập nhật. | F1-09; prototype:125-127, 214-230 |
| S-3 | Bảo mật | Đổi mật khẩu tự chọn theo F1-19; hiển thị ngày đổi mật khẩu gần nhất. | F1-19; prototype:232-235 |
| S-4 | Tóm tắt tài khoản và luyện tập | Hiển thị mã người dùng, ngày tham gia, quyền; hiển thị Bài đã giải, Lượt nộp, Phiên phỏng vấn và liên kết `my_progress`. | Prototype:237-248; F1-06/07/08 |
| S-5 | Điều hướng | Liên kết Cài đặt tài khoản sang `settings`, nơi có luồng xoá tài khoản F1-16. | Prototype:174; F1-16 |

### 2.1 Cấu trúc hệ thống liên quan

| Màn/tài liệu | Vai trò | Bounded Context |
|---|---|---|
| Màn hiện tại | `USR0502` / `profile` | `identity` |
| Tài liệu yêu cầu | `01-rd/req/identity.md` (F1-09, F1-19) | Nguồn SoT của màn |
| Prototype | `09-layoutBase/Trang cá nhân.dc.html` | Bằng chứng bố cục và trạng thái |

**Chi tiết trạng thái và cấu trúc màn**

| Lớp | Thành phần | Vai trò |
|---|---|---|
| Màn hình | `screens/users/USR0502_profile.md` | Đặc tả yêu cầu riêng của Trang cá nhân. |
| Chức năng | `req/identity.md` — F1-09, F1-19 | Nguồn yêu cầu cập nhật hồ sơ và đổi mật khẩu tự chọn. |
| User story | `req/user_stories/a1_student.md` — `US-A1-05` | Given-When-Then nền cho người học. |
| Prototype | `09-layoutBase/Trang cá nhân.dc.html` | Bằng chứng cho bố cục và trạng thái UI tham khảo. |
| Màn liên quan | `my_progress`, `settings`, `auth` | Tiến độ, xoá tài khoản và cơ chế xác thực/khôi phục mật khẩu. |

---

## 3. Ngoài phạm vi (Out of Scope)

| # | Hạng mục ngoài phạm vi | Lý do/đích đến |
|---|---|---|
| O-1 | Bảng màu, spacing, component, breakpoint responsive | Thuộc BD: `02-bd/screens/users/USR0502_profile.md` (chưa viết). |
| O-2 | Hợp đồng API sửa hồ sơ, đổi mật khẩu, upload avatar | Thuộc DD: `03-dd/api/identity.md` (chưa viết). |
| O-3 | Validate chi tiết từng trường | Thuộc DD, không thuộc RD màn hình. |
| O-4 | Xác thực hai lớp (2FA) | Ngoài phạm vi đồ án; bỏ khỏi giao diện thật. |
| O-5 | Mô hình gói tài khoản Free/Pro | AlgoPrep không có mô hình phân hạng tài khoản; bỏ dữ liệu mẫu `Pro` khỏi giao diện thật. |

---

## 4. Tiền đề và ràng buộc

| # | Tiền đề/ràng buộc | Nguồn/SoT |
|---|---|---|
| P-1 | Sáu trường thông tin cá nhân được cụ thể hoá trong F1-09. | `identity.md` — F1-09 |
| P-2 | Đổi mật khẩu tự chọn dùng F1-19; không nhầm với F1-17 là luồng quên mật khẩu khi chưa đăng nhập. | `identity.md` — F1-19, F1-17 |
| P-3 | Khi Hoàn tác thay đổi chưa lưu, mọi trường trở về giá trị đã lưu gần nhất, không cần xác nhận thêm. | Prototype:125, 230 |
| P-4 | Đổi email phải xác thực lại bằng mã sáu số gửi tới email mới; email cũ còn hiệu lực đến khi xác thực xong. | Quyết định đã chốt 2026-08-25, đã ghi vào F1-09 |
| P-5 | Những phần UI thuần hiển thị cần dựng lại bằng Next.js được đánh dấu `[Đợi nextjs]`; prototype chỉ là bản dựng tham khảo. | Quyết định đã chốt 2026-08-25 |

---

## 5. Danh sách yêu cầu (REQ)

| ID | Yêu cầu | Loại | Nguồn/SoT |
|---|---|---|---|
| REQ-01 | Hiển thị thông tin định danh của người dùng gồm avatar, tên và email. | Chức năng | Prototype:101-110 |
| REQ-02 | Cho phép xem, cập nhật và hoàn tác sáu trường thông tin cá nhân; hiển thị đúng trạng thái chưa lưu và đã cập nhật. | Chức năng | F1-09; prototype:125-127, 214-230 |
| REQ-03 | Khi email thay đổi, xác thực email mới bằng mã sáu số trước khi thay thế email đăng nhập hiện hành. | Chức năng | F1-09; quyết định 2026-08-25 |
| REQ-04 | Cho phép đổi mật khẩu tự chọn và hiển thị ngày đổi mật khẩu gần nhất. | Chức năng | F1-19; prototype:232-235 |
| REQ-05 | Hiển thị tóm tắt luyện tập và điều hướng tới `my_progress`; hiển thị lối vào `settings` cho luồng xoá tài khoản. | Chức năng | F1-06/07/08, F1-16; prototype:174, 244-248 |
| REQ-06 | Không hiển thị 2FA hoặc gói tài khoản `Pro` trong giao diện thật. | Ràng buộc phạm vi | Quyết định 2026-08-25 |

---

## 6. Yêu cầu dữ liệu

### 6.1 Dữ liệu tham chiếu

| # | Nhóm dữ liệu | Thuộc tính/nguồn | Thao tác |
|---|---|---|---|
| 1 | Hồ sơ | Họ và tên, email, trường/công ty, vai trò hiện tại, ngôn ngữ mặc định, vị trí mục tiêu. | Đọc/cập nhật theo F1-09 |
| 2 | Bảo mật | Mật khẩu và ngày đổi gần nhất. | Đổi mật khẩu theo F1-19 |
| 3 | Tài khoản | Mã người dùng, ngày tham gia, quyền. | Chỉ đọc |
| 4 | Luyện tập | Bài đã giải, lượt nộp, phiên phỏng vấn. | Chỉ đọc; liên kết `my_progress` |

### 6.2 Luồng dữ liệu

Hợp đồng API, upload avatar và quy tắc validate chi tiết chưa được xác định ở RD; các nội dung này thuộc DD. Khi đổi email, hệ thống giữ email cũ hiệu lực cho đến khi xác thực mã sáu số gửi tới email mới hoàn tất [SoT: F1-09; quyết định 2026-08-25].

---

## 7. RACI và danh sách bàn giao

| Bàn giao | Thực hiện | Rà soát | Trạng thái |
|---|---|---|---|
| RD màn `profile` | Nhóm phát triển | Chủ dự án | Có tài liệu này |
| BD màn `profile` | Nhóm phát triển | Chưa xác định — không tự suy luận | Chưa viết |
| DD API `identity` | Nhóm phát triển | Chưa xác định — không tự suy luận | Chưa viết |

---

## 8. Kế hoạch và quy mô

| Chỉ số | Giá trị | Căn cứ |
|---|---|---|
| Trường hồ sơ | 6 | REQ-02 |
| Nhóm dữ liệu bảo mật trong phạm vi | 1 (mật khẩu) | REQ-04 |
| Chỉ số luyện tập tóm tắt | 3 | REQ-05 |
| Hạng mục loại khỏi giao diện thật | 2 (2FA, gói `Pro`) | REQ-06 |
| Lịch, nhân sự, công sức | Chưa xác định — không tự suy luận | Chưa có kế hoạch được phê duyệt |

---

## 9. Tham chiếu

| Loại | Tham chiếu |
|---|---|
| Yêu cầu module | `01-rd/req/identity.md` — F1-09, F1-19, F1-16, F1-06/07/08, F1-17 |
| User story | `01-rd/req/user_stories/a1_student.md` — `US-A1-05` |
| Khảo sát hệ thống | `01-rd/overview/system_survey.md` — mục 7.1 dòng `profile` |
| Prototype | `09-layoutBase/Trang cá nhân.dc.html` |
| Màn hình liên quan | `01-rd/screens/shared/SHR0101_auth.md`, `01-rd/screens/users/USR0501_my_progress.md`, `01-rd/screens/users/USR0503_settings.md` |
| Quy chuẩn | `01-rd/RD_LAYOUT_RULES.md` |
