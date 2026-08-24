# RD — Màn `admin_language_config` (Ngôn ngữ và giới hạn chấm)

> Slug: `admin_language_config` — khớp `01-rd/overview/system_survey.md` mục 7.3 [SoT: 01-rd/overview/system_survey.md:531].
> Bounded Context: `judge-orchestration` + `problem-bank` [SoT: 01-rd/overview/system_survey.md:531]. Actor: A3.
>
> Đối chiếu prototype: `09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html`. File này mô tả **hành vi và UX
> ở mức yêu cầu** — không lặp lại đặc tả chức năng đã có ở `01-rd/req/req.md` (F2-10, F4-11), chỉ trỏ tới và
> bổ sung phần đặc thù của màn.

## 1. Mục đích màn hình

Cấu hình ba ngôn ngữ hỗ trợ (hệ số thời gian, giới hạn tài nguyên mặc định, sandbox), để vận hành đúng năng
lực hạ tầng thật [SoT: 01-rd/req/req.md:155-157, 290-291 — F2-10, F4-11].

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Hệ số nhân thời gian theo ngôn ngữ | F2-10 | `01-rd/req/req.md:155-157` |
| Cấu hình ngôn ngữ và giới hạn tài nguyên mặc định | F4-11 | `01-rd/req/req.md:290-291` |
| Given-When-Then liên quan | — | `01-rd/req/user_stories.md:317-323` (`US-A3-04`, GWT đầu) |

## 3. Trạng thái và cấu trúc màn (screen states)

Đối chiếu `09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html`:

1. **Bảng ngôn ngữ hỗ trợ** — đúng 3 ngôn ngữ đã khoá (Python 3, C++ 17, Java 21, khớp `CLAUDE.md` mục
   "Submission languages"), mỗi dòng: trình biên dịch, hệ số thời gian, giới hạn thời gian tuyệt đối, bộ
   nhớ, bật/tắt (dòng 402-414) — khớp F2-10 (hệ số) + F4-11 (giới hạn theo ngôn ngữ). Hệ số mẫu (Python x3,
   C++ x1, Java x2) khớp đúng ví dụ ở `req.md:155-157` ("Java chậm hơn C++ nên cùng một bài phải khác hệ
   số").
2. **Giới hạn mặc định** — thời gian/bộ nhớ/kích thước output/thời gian biên dịch, áp dụng khi bài toán
   không khai báo riêng (dòng 416-421) — khớp F4-11.
3. **Sandbox** — 3 toggle: "Cho phép truy cập mạng" (mặc định tắt, ghi chú "Tắt trong mọi kỳ thi" — cùng
   phát hiện khái niệm "kỳ thi" ở `admin_queue_monitor.md` Q2), "Giới hạn tiến trình con", "Trả stderr cho
   người học" (dòng 424-428). Không có mã `Fx-nn` riêng cho cấu hình sandbox go-judge — xem Câu hỏi mở Q1.
4. **Lưu ý khi đổi giới hạn** — ghi rõ đổi hệ số/giới hạn không tự áp dụng cho lượt nộp cũ, phải tạo phiên
   chấm lại riêng (dòng 241, liên kết sang `admin_rejudge`) — khớp đúng ranh giới giữa F4-11 (cấu hình mặc
   định cho lượt nộp *mới*) và F4-09 (chấm lại lượt *cũ*), không xung đột.

## 4. Given-When-Then bổ sung ở mức màn

- **Cho** quản trị viên tắt một ngôn ngữ (ví dụ Java 21), **Khi** thay đổi có hiệu lực, **Thì** ngôn ngữ đó
  không còn chọn được ở màn giải bài (`problem_detail`) cho lượt nộp mới, nhưng lượt nộp cũ bằng ngôn ngữ đó
  vẫn giữ được kết quả [SoT: 09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html:406-414].
- **Cho** quản trị viên đổi hệ số thời gian của một ngôn ngữ, **Khi** lưu thay đổi, **Thì** hệ số mới chỉ áp
  dụng cho lượt nộp từ lúc đó về sau — khớp mục 3 dòng 241.

## 5. Câu hỏi mở

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | 3 toggle Sandbox (mạng, giới hạn tiến trình con, kích thước stderr) là tham số cấu hình trực tiếp của `go-sandbox`/go-judge, không có mã `Fx-nn` riêng. Có cần một mã mới thuộc F4 (`judge-orchestration`), hay coi là chi tiết triển khai của F4-11 (giới hạn tài nguyên) không cần mã riêng? | `req.md` F4-11 nói "giới hạn tài nguyên mặc định" ở mức chung, chưa liệt kê các tham số sandbox cụ thể (network/fork/stderr) — đây là mức chi tiết hơn cần cho BD nhưng không phải xung đột nội dung. | Coi là mở rộng chi tiết của F4-11, không cần mã mới — nhưng liệt kê rõ 3 tham số này vào `req.md` khi viết BD cho `judge-orchestration` để BD có căn cứ. | Chủ dự án |

## 6. Ngoài phạm vi file này

- Bảng màu, spacing, component cụ thể — thuộc BD (`02-bd/screens/admin/admin_language_config.md`, chưa viết).
- Hợp đồng API (đổi hệ số, giới hạn, sandbox) — thuộc DD (`03-dd/api/judge-orchestration.md`, chưa viết).
- Khái niệm "kỳ thi" ở ghi chú "Tắt trong mọi kỳ thi" — cùng phát hiện Q2 của
  `01-rd/screens/admin/admin_queue_monitor.md`, không lặp lại chi tiết ở đây.

## 7. Tham chiếu

- `01-rd/req/req.md:155-157, 290-291` — F2-10, F4-11.
- `01-rd/req/user_stories.md:317-323` — `US-A3-04` (GWT đầu).
- `01-rd/overview/system_survey.md` mục 7.3 — dòng `admin_language_config`.
- `CLAUDE.md` mục "Locked stack" — khoá đúng 3 ngôn ngữ.
- `09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html` — prototype.
