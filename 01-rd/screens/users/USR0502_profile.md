# RD (Yêu cầu hệ thống mới) — Hồ sơ / `USR0502`

> Mã màn hình: `USR0502`, theo `02-bd/_rules/bd-template-9sheet.md` mục 8.
> Slug chính tắc: `profile`, khớp `01-rd/overview/system_survey.md` mục 7.1 dòng `profile`.
> Phạm vi/Bounded Context: `identity` (F1). Actor chính: A1. Màn chỉ dựng cho A1 (chốt 2026-10-18, owner, `DEC-2026-1018-usr-open-questions-phase2-5`); A2/A3 có khung và màn hồ sơ riêng ở lát cắt sau.
> Nguồn sự thật (SoT): `01-rd/req/F1_identity.md` (F1-09, F1-19, F1-35), `01-rd/req/user_stories/a1_student.md` (`US-A1-05`) và `09-layoutBase/Trang cá nhân.dc.html`.
> Tài liệu này mô tả hành vi và UX mức yêu cầu của màn hình, không lặp lại đặc tả chức năng nguồn.
> Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

Màn `profile` cho phép xem và sửa thông tin cá nhân, xem phương thức đăng nhập (chỉ đọc; đổi mật khẩu nằm ở Cài đặt), xem tóm tắt luyện tập và đi nhanh tới Cài đặt [SoT: `01-rd/req/F1_identity.md` — F1-09]. Người dùng cũng tải lên, thay và xoá ảnh đại diện của mình ở màn này [SoT: `01-rd/req/F1_identity.md` — F1-35].

---

## 2. Phạm vi

| # | Phạm vi | Nội dung | Nguồn/SoT |
|---|---|---|---|
| S-1 | Khối định danh | Ảnh đại diện (chữ viết tắt khi chưa có ảnh), tên, email, kèm nút đổi ảnh và xoá ảnh. Nút “Đổi ảnh” ở prototype chỉ là placeholder; bản thật là chọn tệp từ máy. **Đảo ngược quyết định 2026-10-18 (mục 4 của `DEC-2026-1018-usr-open-questions-phase1-scope`) theo owner 2026-11-17:** làm ảnh đại diện thật (`DEC-2026-1117-user-avatar-upload`, F1-35). | `09-layoutBase/Trang cá nhân.dc.html`:101-110; F1-35 |
| S-2 | Thông tin cá nhân | Xem và cập nhật Họ và tên, Email, Trường/công ty, Vai trò hiện tại, Ngôn ngữ mặc định, Vị trí mục tiêu; có Hoàn tác, Lưu thay đổi, trạng thái thay đổi chưa lưu và thông báo cập nhật. | F1-09; prototype:125-127, 214-230 |
| S-3 | Bảo mật | Chỉ hiển thị phương thức đăng nhập đã liên kết (chỉ đọc). **Đổi mật khẩu tự chọn (F1-19) và ngày đổi gần nhất không còn ở màn này, chuyển sang `settings` / `USR0503`** (chốt 2026-10-20, owner, `DEC-2026-1020-student-workspace-split-and-password-in-settings` mục 2). | F1-19 (nay thuộc `USR0503`); prototype:232-235 |
| S-4 | Tóm tắt tài khoản và luyện tập | Hiển thị mã người dùng, ngày tham gia, quyền; hiển thị Bài đã giải, Lượt nộp, Phiên phỏng vấn và liên kết `my_progress`. | Prototype:237-248; F1-06/07/08 |
| S-5 | Điều hướng | Liên kết Cài đặt tài khoản sang `settings`, nơi có luồng đổi mật khẩu F1-19 và luồng xoá tài khoản F1-16. | Prototype:174; F1-16 |
| S-6 | Ảnh ở các màn khác | Ảnh đã tải ở đây là ảnh hiện ở mọi màn có hiển thị người dùng đó (thanh đầu trang, lời chào, danh sách và chi tiết học viên của giảng viên, bảng quản trị). Màn này chỉ là nơi đổi ảnh; hành vi hiển thị ở từng màn do RD/BD của màn đó dẫn F1-35. | F1-35 |

### 2.1 Cấu trúc hệ thống liên quan

| Màn/tài liệu | Vai trò | Bounded Context |
|---|---|---|
| Màn hiện tại | `USR0502` / `profile` | `identity` |
| Tài liệu yêu cầu | `01-rd/req/F1_identity.md` (F1-09, F1-35; F1-19 nay thuộc `USR0503`) | Nguồn SoT của màn |
| Prototype | `09-layoutBase/Trang cá nhân.dc.html` | Bằng chứng bố cục và trạng thái |

**Chi tiết trạng thái và cấu trúc màn**

| Lớp | Thành phần | Vai trò |
|---|---|---|
| Màn hình | `screens/users/USR0502_profile.md` | Đặc tả yêu cầu riêng của Trang cá nhân. |
| Chức năng | `req/F1_identity.md` — F1-09, F1-19, F1-35 | Nguồn yêu cầu cập nhật hồ sơ và ảnh đại diện (F1-19 đổi mật khẩu nay do `USR0503` đảm nhận). |
| User story | `req/user_stories/a1_student.md` — `US-A1-05` | Given-When-Then nền cho người học. |
| Prototype | `09-layoutBase/Trang cá nhân.dc.html` | Bằng chứng cho bố cục và trạng thái UI tham khảo. |
| Màn liên quan | `my_progress`, `settings`, `auth` | Tiến độ, đổi mật khẩu và xoá tài khoản (`settings`), cơ chế xác thực/khôi phục mật khẩu. |

---

## 3. Ngoài phạm vi (Out of Scope)

| # | Hạng mục ngoài phạm vi | Lý do/đích đến |
|---|---|---|
| O-1 | Bảng màu, spacing, component, breakpoint responsive | Thuộc BD: `02-bd/screens/users/USR0502_profile.md` (chưa viết). |
| O-2 | Hợp đồng API sửa hồ sơ và API ảnh đại diện (tải lên, xoá, đọc theo mã người dùng); đổi mật khẩu thuộc `USR0503` | Thuộc DD: `03-dd/api/F1_identity.md`. |
| O-3 | Validate chi tiết từng trường | Thuộc DD, không thuộc RD màn hình. |
| O-4 | Xác thực hai lớp (2FA) | Ngoài phạm vi đồ án; bỏ khỏi giao diện thật. |
| O-5 | Mô hình gói tài khoản Free/Pro | AlgoPrep không có mô hình phân hạng tài khoản; bỏ dữ liệu mẫu `Pro` khỏi giao diện thật. |
| O-6 | Cắt ảnh (crop) trong giao diện, kiểm duyệt nội dung ảnh, quét virus, CDN, job dọn tệp mồ côi | Không có yêu cầu nguồn (chỉ thu nhỏ về 256 x 256 ở trình duyệt); ghi nợ nếu cần sau. |

---

## 4. Tiền đề và ràng buộc

| # | Tiền đề/ràng buộc | Nguồn/SoT |
|---|---|---|
| P-1 | Sáu trường thông tin cá nhân được cụ thể hoá trong F1-09. | `F1_identity.md` — F1-09 |
| P-2 | Màn này **không** có đổi mật khẩu (F1-19 chuyển sang `USR0503` từ 2026-10-20); không nhầm với F1-17 là luồng quên mật khẩu khi chưa đăng nhập. | `F1_identity.md` — F1-19, F1-17; `DEC-2026-1020-student-workspace-split-and-password-in-settings` mục 2 |
| P-3 | Khi Hoàn tác thay đổi chưa lưu, mọi trường trở về giá trị đã lưu gần nhất, không cần xác nhận thêm. | Prototype:125, 230 |
| P-4 | Đổi email phải xác thực lại bằng mã sáu số gửi tới email mới; email cũ còn hiệu lực đến khi xác thực xong. | Quyết định đã chốt 2026-08-25, đã ghi vào F1-09 |
| P-5 | Những phần UI thuần hiển thị cần dựng lại bằng Next.js được đánh dấu `[Đợi nextjs]`; prototype chỉ là bản dựng tham khảo. | Quyết định đã chốt 2026-08-25 |
| P-6 | Ảnh chấp nhận PNG, JPEG, WebP, tối đa 512 KB; không nhận SVG; định dạng kiểm theo nội dung tệp; trình duyệt thu nhỏ về 256 x 256 trước khi gửi. Ảnh lưu ở kho đối tượng, không lưu trong cơ sở dữ liệu. | F1-35 mục 1, 3; `DEC-2026-1117-user-avatar-upload` |

---

## 5. Danh sách yêu cầu (REQ)

| ID | Yêu cầu | Loại | Nguồn/SoT |
|---|---|---|---|
| REQ-01 | Hiển thị thông tin định danh của người dùng gồm ảnh đại diện (chữ viết tắt khi chưa có ảnh), tên và email. | Chức năng | Prototype:101-110; F1-35 |
| REQ-02 | Cho phép xem, cập nhật và hoàn tác sáu trường thông tin cá nhân; hiển thị đúng trạng thái chưa lưu và đã cập nhật. | Chức năng | F1-09; prototype:125-127, 214-230 |
| REQ-03 | Khi email thay đổi, xác thực email mới bằng mã sáu số trước khi thay thế email đăng nhập hiện hành. | Chức năng | F1-09; quyết định 2026-08-25 |
| REQ-04 | **Đã chuyển sang `USR0503`** (chốt 2026-10-20, owner, `DEC-2026-1020-student-workspace-split-and-password-in-settings` mục 2): màn Hồ sơ **không** cho đổi hay đặt mật khẩu; khối Bảo mật chỉ hiển thị phương thức đăng nhập đã liên kết, chỉ đọc. Nội dung yêu cầu cũ (đổi mật khẩu tự chọn, ngày đổi gần nhất, "Chưa từng đổi mật khẩu", đăng xuất phiên khác, giữ phiên đang thao tác) vẫn hiệu lực nhưng do RD `USR0503` mang. Giữ số REQ để tham chiếu không lệch. | Chức năng | F1-19; prototype:232-235 |
| REQ-05 | Hiển thị tóm tắt luyện tập và điều hướng tới `my_progress`; hiển thị lối vào `settings` cho luồng đổi mật khẩu và xoá tài khoản. | Chức năng | F1-06/07/08, F1-16; prototype:174, 244-248 |
| REQ-06 | Không hiển thị 2FA hoặc gói tài khoản `Pro` trong giao diện thật. | Ràng buộc phạm vi | Quyết định 2026-08-25 |
| REQ-07 | ~~Không có nút “Đổi ảnh” và không có ảnh đại diện thật; avatar luôn là chữ viết tắt.~~ **Đã đảo ngược 2026-11-17 (owner, `DEC-2026-1117-user-avatar-upload`):** có ảnh đại diện thật; chữ viết tắt chỉ là hình thay thế khi chưa có ảnh, đã xoá ảnh hoặc ảnh tải lỗi. Giữ số REQ để tham chiếu không lệch. | Chức năng | F1-35 mục 2; ~~`DEC-2026-1018-usr-open-questions-phase1-scope` mục 4~~ |
| REQ-08 | **Cho** tôi đã đăng nhập ở màn Hồ sơ, **Khi** tôi chọn một tệp PNG, JPEG hoặc WebP không quá 512 KB, **Thì** tôi thấy bản xem trước; **Khi** tôi bấm Lưu ảnh, **Thì** ảnh mới thay ảnh hiện tại và hiện ngay ở khối định danh; **Khi** tôi bấm Huỷ, **Thì** ảnh đã chọn bị bỏ và ảnh hiện tại giữ nguyên. | Chức năng | F1-35 mục 1, 10 |
| REQ-09 | **Cho** tôi chọn tệp sai định dạng (kể cả SVG, hoặc tệp đổi đuôi) hoặc lớn hơn 512 KB, **Khi** tôi lưu, **Thì** tôi được báo lỗi nêu đúng lý do và ảnh đang dùng giữ nguyên. | Chức năng | F1-35 mục 1 |
| REQ-10 | **Cho** tôi đang có ảnh, **Khi** tôi bấm xoá ảnh và xác nhận, **Thì** ảnh bị xoá và mọi nơi hiện tên tôi trở lại chữ viết tắt. | Chức năng | F1-35 mục 2 |
| REQ-11 | **Cho** tôi đăng nhập lần đầu bằng Google hoặc GitHub và chưa có ảnh, **Khi** liên kết hoàn tất, **Thì** ảnh của nhà cung cấp được lấy một lần làm ảnh đại diện; nếu lấy không được, tôi vẫn đăng nhập bình thường và thấy chữ viết tắt. | Chức năng | F1-35 mục 6 |
| REQ-12 | **Cho** kho lưu ảnh tạm thời không dùng được, **Khi** tôi mở hoặc đổi ảnh, **Thì** hồ sơ và mọi chức năng khác vẫn dùng được, chỉ thao tác ảnh báo lỗi và nơi hiển thị rơi về chữ viết tắt. | Ràng buộc | F1-35 mục 7 |

---

## 6. Yêu cầu dữ liệu

### 6.1 Dữ liệu tham chiếu

| # | Nhóm dữ liệu | Thuộc tính/nguồn | Thao tác |
|---|---|---|---|
| 1 | Hồ sơ | Họ và tên, email, trường/công ty, vai trò hiện tại, ngôn ngữ mặc định, vị trí mục tiêu. | Đọc/cập nhật theo F1-09 |
| 5 | Ảnh đại diện | Tệp ảnh (PNG/JPEG/WebP, tối đa 512 KB) và thời điểm cập nhật gần nhất. | Đọc, tải lên/thay, xoá theo F1-35 |
| 2 | Bảo mật | Phương thức đăng nhập đã liên kết (mật khẩu và ngày đổi gần nhất chuyển sang `USR0503`). | Chỉ đọc |
| 3 | Tài khoản | Mã người dùng, ngày tham gia, quyền. | Chỉ đọc |
| 4 | Luyện tập | Bài đã giải, lượt nộp, phiên phỏng vấn. | Chỉ đọc; liên kết `my_progress` |

### 6.2 Luồng dữ liệu

Hợp đồng API và quy tắc validate chi tiết chưa được xác định ở RD; các nội dung này thuộc DD. Khi đổi email, hệ thống giữ email cũ hiệu lực cho đến khi xác thực mã sáu số gửi tới email mới hoàn tất [SoT: F1-09; quyết định 2026-08-25].

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
| Nhóm dữ liệu bảo mật trong phạm vi | 1 (phương thức đăng nhập, chỉ đọc) | REQ-04 (đã chuyển) |
| Chỉ số luyện tập tóm tắt | 3 | REQ-05 |
| Hạng mục loại khỏi giao diện thật | 2 (2FA, gói `Pro`); ảnh đại diện thật đã đưa vào phạm vi 2026-11-17 | REQ-06; REQ-07 (đã đảo) |
| Yêu cầu ảnh đại diện | 5 (REQ-08 đến REQ-12) | F1-35 |
| Lịch, nhân sự, công sức | Chưa xác định — không tự suy luận | Chưa có kế hoạch được phê duyệt |

---

## 9. Tham chiếu

| Loại | Tham chiếu |
|---|---|
| Yêu cầu module | `01-rd/req/F1_identity.md` — F1-09, F1-35, F1-19, F1-16, F1-06/07/08, F1-17 |
| Quyết định | `.nexa/control/decision-registry.md` — `DEC-2026-1117-user-avatar-upload` |
| User story | `01-rd/req/user_stories/a1_student.md` — `US-A1-05` |
| Khảo sát hệ thống | `01-rd/overview/system_survey.md` — mục 7.1 dòng `profile` |
| Prototype | `09-layoutBase/Trang cá nhân.dc.html` |
| Màn hình liên quan | `01-rd/screens/shared/SHR0101_auth.md`, `01-rd/screens/users/USR0501_my_progress.md`, `01-rd/screens/users/USR0503_settings.md` |
| Quy chuẩn | `01-rd/RD_LAYOUT_RULES.md` |

---

## 10. Lịch sử thay đổi

| Ngày | Nội dung | Người sửa |
|---|---|---|
| 2026/11/17 (vòng 2) | Owner chốt Q12: đổi ảnh có bước xem trước rồi Lưu (REQ-08). Chưa duyệt hình | Claude |
| 2026/11/17 | Đưa ảnh đại diện thật vào phạm vi theo `DEC-2026-1117-user-avatar-upload` (F1-35): sửa S-1, thêm S-6, đảo REQ-07, thêm P-6, O-6, REQ-08 đến REQ-12, mục 6.1 dòng 5, mục 8, mục 9; sửa O-2. Chưa duyệt hình | Claude |
| 2026/10/21 | Bỏ đổi / đặt mật khẩu khỏi màn Hồ sơ, chuyển sang `USR0503` theo `DEC-2026-1020-student-workspace-split-and-password-in-settings` mục 2 (S-3, S-5, P-2, REQ-04, REQ-05, mục 6.1 dòng 2, mục 8). Đánh số REQ giữ nguyên. Chưa duyệt hình | Claude |
