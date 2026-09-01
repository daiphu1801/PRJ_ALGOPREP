## 1. Đặc tả Yêu cầu Chức năng (Functional Requirements - PRD)

> **Viết lại 2026-08-23.** Bản trước là nội dung của dự án cũ NestGame v2 (module IAM/Catalog/SaveState,
> OAuth Google, chơi game retro) — nợ tài liệu đã ghi ở `01-rd/README.md` mục 3 (bản sử). Bản này viết theo
> đúng sáu phân hệ **F1-F6** của `README.md` mục 4, chi tiết hoá từ bảng chức năng có mã `Fx-nn` ở
> `01-rd/overview/system_survey.md` mục 5. Mỗi yêu cầu dưới đây trỏ lại mã đó để truy vết sang BD, DD,
> `04-tdd/` và test — **không đánh số lại**, dùng nguyên mã `Fx-nn` làm khoá tham chiếu.
>
> Đây là yêu cầu **chức năng ở mức đặc tả nghiệp vụ** — không phải lược đồ database (`02-bd/database/`),
> không phải hợp đồng API (`03-dd/api/`). Thuật ngữ dùng đúng `01-rd/overview/glossary.md`.
>
> **Tách file 2026-08-31** (`DEC-2026-0831-rd-req-split-by-module`): nội dung chi tiết của từng phân hệ đã
> chuyển sang file riêng theo Bounded Context (bảng dưới), để dễ đọc hơn so với một file 764 dòng. File này
> giờ chỉ là **mục lục** — không lặp lại bất kỳ mã `Fx-nn` nào, chỉ trỏ tới đúng file chứa nó. Khi trích dẫn
> một yêu cầu, dùng mã `Fx-nn` làm khoá tra cứu (ví dụ `identity.md` — F1-05), không dùng số dòng — số dòng
> sẽ lệch mỗi khi file được sửa, mã `Fx-nn` thì không.

NestGame v2 → thay bằng: AlgoPrep gồm **sáu phân hệ chức năng chính** theo Bounded Context
(`.nexa/domain-registry.json`), mỗi phân hệ có yêu cầu kỹ thuật và trải nghiệm người dùng riêng.

| Mã phân hệ | Bounded Context | File | Phạm vi (một dòng) |
| :--- | :--- | :--- | :--- |
| F1 | `identity` | `01-rd/req/identity.md` | Đăng ký/đăng nhập/OAuth, RBAC, ma trận phân quyền, hồ sơ cá nhân, lớp học (tạo/xoá/mã mời), dashboard tổng quan A2/A3 |
| F2 | `problem-bank` | `01-rd/req/problem-bank.md` | Soạn đề, testcase Sample/Hidden, giới hạn tài nguyên, tìm kiếm/lọc, giao bài theo lớp, bookmark, vòng đời bài toán |
| F3 | `harness` | `01-rd/req/harness.md` | Bộ sinh mã bọc hàm đa ngôn ngữ, lược đồ kiểu, so khớp kết quả, ánh xạ lỗi biên dịch, song song hai mô hình nộp bài |
| F4 | `judge-orchestration` | `01-rd/req/judge-orchestration.md` | Tiếp nhận bài nộp, gọi `JudgeExecutionPort` theo testcase, điểm tỷ lệ testcase, timeout sweep, WebSocket realtime, giám sát cụm |
| F5 | `ai-review` | `01-rd/req/ai-review.md` | Phân tích bài giải (F5.1), phỏng vấn giả lập 1:1 (F5.2), ràng buộc chống prompt injection và suy giảm có kiểm soát, cấu hình AI |
| F6 | `interview-bank` | `01-rd/req/interview-bank.md` | Ngân hàng câu hỏi phỏng vấn lý thuyết, Chế độ học/luyện, spaced repetition, quản trị nội dung dùng chung |
| — | Cross-cutting | `01-rd/req/nfr.md` | Yêu cầu phi chức năng: hiệu năng, khả năng mở rộng, độ tin cậy, bảo mật, khả năng bảo trì, tiêu chí thành công |

Đọc thêm: user story theo actor `01-rd/req/user_stories.md` · từ vựng chuẩn `01-rd/overview/glossary.md` ·
bảng chức năng đầy đủ `01-rd/overview/system_survey.md` mục 5.
