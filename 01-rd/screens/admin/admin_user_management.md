# RD — Màn `admin_user_management` (Quản lý người dùng)

> Slug: `admin_user_management` — khớp `01-rd/overview/system_survey.md` mục 7.3 [SoT: 01-rd/overview/system_survey.md:556].
> Bounded Context: `identity` (F1) [SoT: 01-rd/overview/system_survey.md:556]. Actor: A3.
>
> Đối chiếu prototype: `09-layoutBase/Admin - Người dùng.dc.html`. File này mô tả **hành vi và UX ở mức yêu
> cầu** — không lặp lại đặc tả chức năng đã có ở `01-rd/req/req.md` (F1-13), chỉ trỏ tới và bổ sung phần đặc
> thù của màn.
> **Phát hiện của Phase 6 — xem Câu hỏi mở Q1**: khối "Cần xử lý" có mục "Báo cáo nghi gian lận" (phát hiện
> trùng mã nguồn giữa hai tài khoản) hoàn toàn chưa có mã `Fx-nn`.
> **Q2 đã chốt 2026-08-25 qua yêu cầu trực tiếp của chủ dự án:** không có luồng tự yêu cầu nâng vai trò —
> **chỉ ADMIN được đổi vai trò**, đúng nguyên văn F1-13. Mục "Đề nghị cấp quyền giảng viên" trong prototype
> bị loại khỏi phạm vi, xoá khi dựng UI thật **[Đợi nextjs]**.

## 1. Mục đích màn hình

Quản lý tài khoản người dùng — đổi vai trò, khoá/mở khoá, reset mật khẩu — để xử lý sự cố tài khoản
[SoT: 01-rd/req/req.md:60-61 — F1-13].

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Đổi vai trò, khoá/mở khoá, reset mật khẩu tài khoản khác | F1-13 | `01-rd/req/req.md:60-61` |
| Mọi thay đổi ghi vào Nhật ký hệ thống | F1-14 | `01-rd/req/req.md:62-68` |
| Given-When-Then đầy đủ | — | `01-rd/req/user_stories.md:298-305` (`US-A3-02`) |

## 3. Trạng thái và cấu trúc màn (screen states)

Đối chiếu `09-layoutBase/Admin - Người dùng.dc.html`:

1. **4 chỉ số tổng** — Tổng tài khoản, Đang hoạt động 24 giờ, Chờ xác thực email, Bị khoá (dòng 422-427) —
   không có mã riêng, tổng hợp hiển thị của F1-13 và trạng thái tài khoản (F1-01/F1-16).
2. **Bảng người dùng** — tìm kiếm, lọc theo vai trò/trạng thái, chọn nhiều dòng rồi hành động gộp (Đặt lại
   mật khẩu / Đổi vai trò / Khoá tài khoản, dòng 194-203) — khớp F1-13, mở rộng UX cho phép áp dụng hành
   động lên nhiều tài khoản cùng lúc (không có mã riêng, là cách trình bày của F1-13).
3. **Phân bố theo vai trò** — biểu đồ tỉ lệ Người học/Giảng viên/Quản trị/Đã vô hiệu (dòng 475-480) — không
   có mã riêng, minh hoạ số liệu.
4. **Khối "Cần xử lý"** — 4 mục: "Yêu cầu đặt lại mật khẩu" (khớp F1-17 tự đặt lại mật khẩu, hoặc F1-13 nếu
   là ADMIN reset hộ — cần phân biệt rõ ở Câu hỏi mở Q3), "Chờ xác thực email" (khớp F1-01, đăng ký chờ xác
   thực), **"Báo cáo nghi gian lận"** (trùng mã nguồn giữa hai tài khoản, dòng 485) — **không có mã `Fx-nn`
   nào cho phát hiện gian lận/đạo văn mã nguồn** — xem Câu hỏi mở Q1, và **"Đề nghị cấp quyền giảng viên"**
   (cần quản trị viên phê duyệt, dòng 486) — **đã chốt loại khỏi phạm vi (xem banner đầu file)**: chỉ ADMIN
   được đổi vai trò theo F1-13, không có luồng tự yêu cầu; mục này xoá khi dựng UI thật **[Đợi nextjs]**.

## 4. Given-When-Then bổ sung ở mức màn

- **Cho** quản trị viên chọn nhiều tài khoản cùng lúc, **Khi** áp dụng một hành động gộp (ví dụ khoá tài
  khoản), **Thì** hành động áp dụng cho tất cả tài khoản đã chọn và mỗi tài khoản ghi một dòng riêng vào
  Nhật ký hệ thống (không gộp thành một dòng) — khớp nguyên tắc "ai đổi, đổi gì" chi tiết theo từng đối
  tượng của F1-14 [SoT: 09-layoutBase/Admin - Người dùng.dc.html:194-203].

## 5. Câu hỏi mở

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q2 (đã chốt) | "Đề nghị cấp quyền giảng viên" — luồng tự yêu cầu nâng vai trò từ STUDENT lên INSTRUCTOR. | Chủ dự án đã trả lời trực tiếp: **không có luồng self-service**. | **Chốt: chỉ ADMIN được đổi vai trò**, đúng nguyên văn F1-13 (`req.md:60-61`), không sửa mã này. Không mở RD/BD cho một luồng yêu cầu-duyệt. Xoá mục "Đề nghị cấp quyền giảng viên" khỏi khối "Cần xử lý" khi dựng UI thật **[Đợi nextjs]**. | Đã chốt — không còn mở. |
| Q1 | **"Báo cáo nghi gian lận" — phát hiện trùng mã nguồn giữa hai tài khoản** (ví dụ log thật ở `admin_system_log`: "Khóa tài khoản hmtri do trùng mã nguồn với bklinh") — không có mã `Fx-nn` nào cho cơ chế phát hiện đạo văn/gian lận trong bài nộp, ở bất kỳ đâu trong `req.md`/`README.md`. Đây là một tính năng chống gian lận (plagiarism detection) hoàn toàn mới, có thể thuộc `judge-orchestration` (so khớp mã nguồn giữa các lượt nộp) hoặc `problem-bank`. Có trong phạm vi đồ án không, và nếu có thì cơ chế so khớp là gì (so khớp chuỗi, AST, hay chỉ cảnh báo thủ công do giảng viên báo)? | Đây là phát hiện lớn thứ hai của Phase 6 (sau "Gợi ý theo bậc" ở `admin_ai_config.md`) — một tính năng an toàn học thuật hoàn toàn vắng mặt trong RD nhưng có mặt như một luồng vận hành thật trong 2 màn khác nhau (khối "Cần xử lý" ở đây, và một dòng log cụ thể ở `admin_system_log`). Không tự quyết được vì đây là tính năng mới có thể cần thuật toán so khớp riêng (nằm ngoài năng lực go-judge). | Nếu trong phạm vi: mở RD riêng cho tính năng chống đạo văn (mã mới, có thể thuộc `judge-orchestration` hoặc một Bounded Context mới), xác nhận cơ chế (tự động so khớp hay chỉ khung UI để giảng viên/ADMIN tự báo cáo thủ công — tương tự cách "Chạy đối chiếu" ở `admin_ai_config` được chốt là chỉ giữ ở tầng giao diện). Nếu ngoài phạm vi: xoá mục này khỏi khối "Cần xử lý" và dòng log liên quan khi dựng UI thật. | Chủ dự án |
| Q2 | **"Đề nghị cấp quyền giảng viên" — luồng tự yêu cầu nâng vai trò từ STUDENT lên INSTRUCTOR, cần ADMIN phê duyệt** — không có mã `Fx-nn`. F1-13 hiện tại chỉ mô tả ADMIN đơn phương đổi vai trò một tài khoản, không mô tả một luồng người dùng *tự yêu cầu* rồi chờ duyệt. | Đây là một luồng self-service mới (có màn/nút yêu cầu ở phía người dùng, có hàng đợi duyệt ở phía ADMIN) — ảnh hưởng tới cả `profile`/`settings` (nơi người dùng bấm yêu cầu) chứ không chỉ màn quản trị này. | Nếu trong phạm vi: bổ sung mã mới (ví dụ F1-13b hoặc F1-23) mô tả luồng hai chiều (yêu cầu → hàng đợi duyệt → ADMIN chấp thuận/từ chối → đổi vai trò theo F1-13), và bổ sung điểm vào ở `profile`/`settings` cho phía người dùng. Nếu ngoài phạm vi: xoá mục này khỏi khối "Cần xử lý", giữ nguyên mô hình ADMIN chủ động đổi vai trò theo yêu cầu ngoài hệ thống (email, gặp trực tiếp). | Chủ dự án |
| Q3 | "Yêu cầu đặt lại mật khẩu" trong khối "Cần xử lý" — đây là số lượng yêu cầu tự đặt lại mật khẩu qua OTP (F1-17, người dùng tự làm, ADMIN không cần hành động) hay là hàng đợi cần ADMIN reset hộ (F1-13)? Nếu là loại đầu, tại sao lại nằm trong khối "Cần xử lý" của ADMIN? | Câu chữ "Gửi trong 24 giờ qua" (dòng 483) gợi ý đây chỉ là số liệu thống kê, không phải hàng đợi cần ADMIN duyệt — nhưng đặt trong khối "Cần xử lý" cùng các mục cần hành động khác gây mơ hồ. | Đề xuất tách "Yêu cầu đặt lại mật khẩu" ra khỏi khối "Cần xử lý" (chuyển vào một khối thống kê riêng, vì F1-17 không cần ADMIN hành động) khi dựng UI thật, tránh nhầm với các mục thật sự cần ADMIN xử lý (gian lận, xác thực email, cấp quyền). | Chủ dự án |

## 6. Ngoài phạm vi file này

- Bảng màu, spacing, component cụ thể — thuộc BD (`02-bd/screens/admin/admin_user_management.md`, chưa
  viết) — **không nên mở BD trước khi Q1 được chốt** (Q2 đã chốt, không còn chặn).
- Hợp đồng API (tìm kiếm/lọc người dùng, hành động gộp, reset mật khẩu) — thuộc DD (`03-dd/api/identity.md`,
  chưa viết).
- Cơ chế so khớp mã nguồn chống gian lận (Q1) — không tự đặc tả trong file này, chờ chủ dự án quyết định
  phạm vi.

## 7. Tham chiếu

- `01-rd/req/req.md:60-68` — F1-13, F1-14.
- `01-rd/req/user_stories.md:298-305` — `US-A3-02`.
- `01-rd/overview/system_survey.md` mục 7.3 — dòng `admin_user_management`.
- `09-layoutBase/Admin - Người dùng.dc.html` — prototype.
- `01-rd/screens/admin/admin_system_log.md` mục 3 — dòng log "trùng mã nguồn" củng cố Câu hỏi mở Q1.
