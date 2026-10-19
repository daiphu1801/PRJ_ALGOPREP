# Tài liệu thiết kế cơ bản (BD) — Chấm tay (`INS0301`) — ĐÃ BỎ

> **Màn này đã bị gỡ khỏi phạm vi ngày 2026/10/13.** File được giữ lại chỉ để các liên kết cũ không gãy.
> Toàn bộ thiết kế cũ (V0.1 đến V0.9) nằm trong lịch sử git, không chép lại ở đây.

| Mục | Giá trị |
| :--- | :--- |
| Tên màn | Chấm tay (chấm bài của giảng viên) |
| Mã màn hình | `INS0301` — **mã không dùng lại** |
| Tên vật lý (slug) | `instructor_grading` |
| Ngày bỏ | 2026/10/13 |
| Quyết định | `DEC-2026-1013-remove-instructor-manual-grading` (thay `DEC-2026-0831-instructor-grading-round2` và `DEC-2026-1013-grading-queue-all-accepted`) |
| Lý do (một dòng) | Giảng viên không đọc nổi mã của mọi học viên nên hàng đợi sẽ đầy mà không ai dùng, và điểm chấm tay không nuôi màn hình hay báo cáo nào khác (tiêu chí ponytail bậc 3 và 2) |
| Cùng bị bỏ | Yêu cầu `F5-27`, user story `US-A2-06`, mục nav "Chấm bài", khối và cột "Cần chấm tay" ở `INS0101`/`INS0201`/`INS0202`/`INS0205`, liên kết "Xem chấm bài của học viên này" ở `INS0204`, bảng `ai.manual_grades` (chưa dựng) và 5 cột `manual_*` của `ai.solution_reviews` |
| Giữ nguyên | Báo cáo AI do học viên chủ động xin (F5.1); Function `CLASS_MANAGEMENT`; quyền A2 đọc mã nguồn bài nộp của học viên trong lớp mình (`DEC-2026-1013-instructor-avatar-menu-and-read-source`) |

## Sheet 2. Lịch sử sửa đổi

| Ver | Sheet bị sửa | Nội dung sửa | Ngày | Người sửa |
| :--- | :--- | :--- | :--- | :--- |
| V1.0 | Toàn bộ | Thay toàn bộ nội dung (V0.1 đến V0.9) bằng thông báo gỡ màn theo `DEC-2026-1013-remove-instructor-manual-grading`. Prototype và mã (`views/teacher/grading`, `entities/manual-grading`, mục nav, chuỗi i18n) vẫn còn các phần bị bỏ, chưa gỡ | 2026/10/13 | AI |
| V1.1 | Câu hỏi mở | Áp `DEC-2026-1014-drop-ai-score-10`: việc mở về điểm AI quy đổi thang 10 đã đóng, cột bị bỏ. Màn vẫn chỉ là thông báo gỡ, không có số thứ tự hay mục gạch bỏ nào để đánh số lại | 2026/10/14 | AI |

## Câu hỏi mở

Mọi câu hỏi Q1 đến Q13 của bản cũ **Đã đóng: màn bị bỏ**. Việc còn mở sau đây là hệ quả của quyết định gỡ, đã chuyển đi nơi liên quan:

- Điểm AI quy đổi thang 10 từng là "tham khảo cho giảng viên": **đã bỏ cùng cột dẫn xuất của nó theo `DEC-2026-1014-drop-ai-score-10` (2026/10/14)**, không còn việc nào mở ở đây.
- Lối vào trang bài nộp chỉ-đọc `/instructor/submissions/{id}` của giảng viên (trước là popup chấm): xem Câu hỏi mở ở `INS0204_class_student_detail.md`.
