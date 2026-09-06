# User Story và Tiêu Chí Chấp Nhận

`req.md` (và các file `01-rd/req/<module>.md` tách ra từ nó) liệt kê **tính năng** theo phân hệ `Fx-nn`;
các file dưới đây diễn đạt cùng phạm vi đó thành **việc mỗi actor cần làm được** và điều kiện để coi là làm
được. Đây là đầu vào để viết BD/DD theo màn và để viết tiêu chí nghiệm thu.

> **Viết lại 2026-08-24.** Bản trước là nội dung của dự án cũ NestGame v2 (module IAM/Catalog/SaveState,
> chơi game retro) — nợ tài liệu đã ghi ở `01-rd/README.md` mục 3/4. Bản này viết theo bốn actor A1-A4 của
> `README.md` mục 3, bám đúng bảng chức năng có mã `Fx-nn` ở `01-rd/overview/system_survey.md` mục 5 —
> **không đánh số lại**, mỗi story trỏ về đúng mã `Fx-nn` liên quan để truy vết sang BD, DD, `04-tdd/` và
> test. Nội dung đồng bộ với `req.md` (viết lại cùng đợt) — không lặp lại phần đặc tả kỹ thuật, chỉ diễn đạt
> lại dưới góc nhìn actor.
>
> **Quan hệ với `04-tdd/`.** File này ở tầng RD, viết theo ngôn ngữ nghiệp vụ — một tiêu chí chấp nhận là
> một câu Cho/Khi/Thì. `04-tdd/<module>.md` là bộ tiêu chí nghiệm thu chi tiết hơn (`AC-nn` dạng Precondition
> / Action / Expected result), viết **cùng lúc với DD của module đó** theo quy trình
> RD → BD → Prototype → DD → CODE → TEST. Chưa file `04-tdd/` nào tồn tại — viết dần theo từng slice, không
> viết trước hàng loạt.
>
> Ký hiệu: **Cho** (bối cảnh) · **Khi** (hành động) · **Thì** (kết quả) — Given/When/Then.

Mỗi story theo chuẩn INVEST (Independent, Negotiable, Valuable, Estimable, Small, Testable). Story của A4
(hệ thống tự động) không có "tôi muốn" theo nghĩa người — A4 là job/consumer, viết dưới dạng "hệ thống tự
động phải" để giữ đúng ngữ pháp nhưng không giả vờ A4 là người.

> **Tách file 2026-08-31** (`DEC-2026-0831-rd-req-split-by-module`): nội dung chi tiết của từng actor đã
> chuyển sang file riêng (bảng dưới), để dễ đọc hơn so với một file 517 dòng. File này giờ chỉ là **mục
> lục** — không lặp lại bất kỳ story nào, chỉ trỏ tới đúng file chứa nó. Khi trích dẫn một story, dùng mã
> `US-Axx-nn` làm khoá tra cứu, không dùng số dòng.

---

| Actor | File | Phạm vi mã story |
| :--- | :--- | :--- |
| A1 — Sinh viên / người dùng cuối | `01-rd/req/user_stories/a1_student.md` | US-A1-01 → US-A1-09 |
| A2 — Giảng viên | `01-rd/req/user_stories/a2_instructor.md` | US-A2-01 → US-A2-11 (gồm US-A2-01b; US-A2-04 đã loại bỏ khỏi phạm vi) |
| A3 — Quản trị viên | `01-rd/req/user_stories/a3_admin.md` | US-A3-01 → US-A3-04 (US-A3-05 đã loại bỏ khỏi phạm vi) |
| A4 — Hệ thống tự động | `01-rd/req/user_stories/a4_system.md` | US-A4-01 → US-A4-05 |
| Câu hỏi mở | `01-rd/req/user_stories/open_questions.md` | Q1 → Q4 (toàn bộ đã đóng) |

---

Đọc thêm: yêu cầu ở mức đặc tả kỹ thuật `01-rd/req/req.md` · từ vựng chuẩn `01-rd/overview/glossary.md` ·
bảng chức năng đầy đủ `01-rd/overview/system_survey.md` mục 5.
