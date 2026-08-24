# RD — Màn `profile` (Trang cá nhân)

> Slug: `profile` — khớp `01-rd/overview/system_survey.md:485` [SoT: 01-rd/overview/system_survey.md:485].
> Bounded Context: `identity` (F1) [SoT: 01-rd/overview/system_survey.md:485]. Actor: A1 (mọi actor có tài
> khoản thật ra đều có trang này, nhưng trục màn `users/` áp dụng vì prototype và luồng chính là của A1 —
> A2/A3 dùng chung cấu trúc màn, khác biệt nếu có sẽ xử lý khi viết BD).
>
> Đối chiếu prototype: `09-layoutBase/Trang cá nhân.dc.html`. File này mô tả **hành vi và UX ở mức yêu
> cầu** — không lặp lại đặc tả chức năng đã có ở `01-rd/req/req.md` (mục F1) và
> `01-rd/req/user_stories.md` (`US-A1-05`), chỉ trỏ tới và bổ sung phần đặc thù của màn.
> **Phát hiện của Phase 2, đã tự chốt 2026-08-25 — xem mục 5**: `req.md` ban đầu chỉ chốt "sửa thông tin cá
> nhân" ở mức suy luận chung (F1-09); đã bổ sung 6 trường cụ thể, mã mới F1-19 (đổi mật khẩu tự chọn), và
> loại 2FA/"Gói" khỏi phạm vi.

## 1. Mục đích màn hình

Xem và sửa thông tin cá nhân, quản lý bảo mật tài khoản ở mức cơ bản, và xem tóm tắt số liệu luyện tập kèm
lối vào nhanh tới Cài đặt [SoT: 01-rd/req/req.md:40-42 — F1-09].

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Sửa thông tin cá nhân | F1-09 | `01-rd/req/req.md:40-42` (`[SoT: Suy luận]`) |
| Given-When-Then liên quan | — | `01-rd/req/user_stories.md:107-108` (`US-A1-05`, GWT thứ 3) |

## 3. Trạng thái và cấu trúc màn (screen states)

Đối chiếu `09-layoutBase/Trang cá nhân.dc.html`:

1. **Khối định danh** — avatar (chữ viết tắt), tên, email, nút "Đổi ảnh" (dòng 101-110) — nút đổi ảnh không
   có kết quả thao tác nào trong prototype tĩnh (không phải input file thật), coi là placeholder hành vi.
2. **Thông tin cá nhân** (6 trường, dòng 214-221): Họ và tên, Email, Trường/công ty, Vai trò hiện tại, Ngôn
   ngữ mặc định, Vị trí mục tiêu — kèm nút "Hoàn tác" và "Lưu thay đổi" (dòng 125-127), trạng thái "Có thay
   đổi chưa lưu" khi khác giá trị gốc (dòng 226-227) và "Đã cập nhật thông tin" sau khi lưu (dòng 224-225).
   Khớp F1-09 (sửa thông tin cá nhân) nhưng cụ thể hoá 6 trường mà `req.md` chưa liệt kê tên nào — xem Câu
   hỏi mở Q1.
3. **Bảo mật** — hai dòng: Mật khẩu (kèm ngày đổi lần cuối, nút "Đổi") và Xác thực hai lớp (kèm trạng thái
   "Chưa bật", nút "Bật") (dòng 232-235). **Không có mã `Fx-nn`** cho đổi mật khẩu tự chọn (khác F1-17 —
   quên mật khẩu khi chưa đăng nhập) hay cho 2FA — xem Câu hỏi mở Q1.
4. **Khối "Tài khoản"** (sidebar) — Mã người dùng, Ngày tham gia, Gói (`Pro`), Quyền (`Người học`) (dòng
   237-242). Trường **"Gói" (`Pro`)** ngụ ý một mô hình phân hạng tài khoản (free/pro) chưa từng được nhắc
   tới ở bất kỳ đâu trong `README.md`/`req.md` — xem Câu hỏi mở Q2.
5. **Khối "Luyện tập"** (sidebar) — 3 số tóm tắt (Bài đã giải, Lượt nộp, Phiên phỏng vấn) kèm liên kết "Xem
   tiến độ chi tiết" sang `my_progress` (dòng 244-248) — không có mã riêng, là tóm tắt lại F1-06/07/08 đã
   đặc tả ở màn `my_progress`.
6. **Liên kết "Cài đặt tài khoản"** (dòng 174) — sang màn `settings`, nơi đặt luồng Xoá tài khoản (F1-16).

## 4. Given-When-Then bổ sung ở mức màn

- **Cho** tôi đã sửa ít nhất một trường thông tin cá nhân nhưng chưa bấm Lưu, **Khi** tôi bấm "Hoàn tác",
  **Thì** toàn bộ trường trở lại giá trị đã lưu gần nhất, không hỏi xác nhận thêm
  [SoT: 09-layoutBase/Trang cá nhân.dc.html:125, 230].
- **Cho** tôi đổi trường "Email", **Khi** tôi bấm Lưu, **Thì** hệ thống phải xử lý email mới này theo đúng
  quy tắc định danh đăng nhập hiện có (duy nhất toàn hệ thống, dùng để đăng nhập) — chưa rõ có cần xác thực
  lại (gửi mã xác nhận email mới) trước khi áp dụng hay áp dụng ngay, vì đổi email ảnh hưởng trực tiếp tới
  luồng đăng nhập và luồng quên mật khẩu (F1-01, F1-17) — xem Câu hỏi mở Q3.

## 5. Câu hỏi mở — đã tự chốt 2026-08-25

> Tự quyết theo yêu cầu trực tiếp của chủ dự án — prototype là bản dựng tham khảo, phần thuần UI đánh dấu
> **[Đợi nextjs]** để dựng lại đúng khi có frontend Next.js thật.

| # | Câu hỏi | Quyết định | Ghi chú |
| :-: | :--- | :--- | :--- |
| Q1 | F1-09 chưa liệt kê 6 trường cụ thể; mục Bảo mật (đổi mật khẩu tự chọn, 2FA) chưa có mã. | **6 trường đã liệt kê vào F1-09** (`req.md`). **Đổi mật khẩu tự chọn → F1-19** (mới, đã ghi vào `req.md`/`user_stories.md`). **2FA nằm ngoài phạm vi đồ án** — bỏ khỏi giao diện thật, không đặc tả trong BD. | 2FA đánh dấu **[Đợi nextjs]** (xoá khối UI khi dựng thật). |
| Q2 | Trường "Gói" (`Pro`) không có mô hình phân hạng tài khoản nào trong RD. | **Chốt: dữ liệu mẫu không có ý nghĩa** — AlgoPrep không có mô hình phân hạng tài khoản. Bỏ khỏi giao diện thật. | **[Đợi nextjs]** (xoá trường "Gói" khi dựng UI thật). |
| Q3 | Đổi email có cần xác thực lại trước khi áp dụng không? | **Chốt: bắt buộc xác thực lại** bằng mã 6 số gửi tới email mới (tái dùng cơ chế F1-17), giữ email cũ hiệu lực tới khi xác thực xong. Đã ghi vào F1-09. | Luồng nhập mã, UI xác nhận cụ thể **[Đợi nextjs]**. |

## 6. Ngoài phạm vi file này

- Bảng màu, spacing, component cụ thể, breakpoint responsive — thuộc BD (`02-bd/screens/users/profile.md`,
  chưa viết).
- Hợp đồng API (sửa hồ sơ, đổi mật khẩu, upload avatar) — thuộc DD (`03-dd/api/identity.md`, chưa viết).
- Ràng buộc validate từng trường (độ dài tên, định dạng email...) — thuộc DD, không thuộc file RD theo trục
  màn này.

## 7. Tham chiếu

- `01-rd/req/req.md:40-42` — F1-09.
- `01-rd/req/user_stories.md:107-108` — `US-A1-05` (GWT thứ 3).
- `01-rd/overview/system_survey.md:485` — dòng `profile` trong bảng màn mục 7.
- `09-layoutBase/Trang cá nhân.dc.html` — prototype.
- `01-rd/screens/shared/auth.md` — luồng F1-17 (quên mật khẩu) tham chiếu ở Câu hỏi mở Q3.
