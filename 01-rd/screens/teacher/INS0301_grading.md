# RD — Chấm tay / `INS0301` — ĐÃ BỎ KHỎI PHẠM VI 2026-10-13

> Mã màn hình: `INS0301`. Slug: `instructor_grading`. Actor từng dùng: A2 (Giảng viên).
> Trạng thái: **ĐÃ BỎ 2026-10-13** theo `DEC-2026-1013-remove-instructor-manual-grading`
> [SoT: .nexa/control/decision-registry.md:3524-3528]. File được giữ lại chỉ để các liên kết cũ không gãy;
> **không dùng làm căn cứ viết BD/DD hay code**.

## 1. Màn này từng là gì

Màn "Chấm bài" của khu Giảng viên: hàng đợi bài nộp `Accepted` của lớp mình phụ trách, hiển thị điểm AI quy
đổi thang 10 (F5-27), cho giảng viên chấm tay đè kèm nhận xét, có 4 thẻ thống kê và bộ lọc theo lớp
(`F5-27`, `US-A2-06`). Đối chiếu prototype `09-layoutBase/Giáo viên - Chấm bài.dc.html`.

## 2. Vì sao bỏ

Một dòng: giảng viên không đọc nổi mã của mọi học viên nên hàng đợi sẽ đầy mà không ai dùng khi demo và bảo
vệ (tiêu chí ponytail bậc 3), và điểm chấm tay không nuôi màn hay báo cáo nào khác (bậc 2)
[SoT: .nexa/control/decision-registry.md:3541-3545 — mục `rationale` của quyết định; tóm tắt lại].

## 3. Hệ quả cho RD

| Mục | Xử lý |
| :--- | :--- |
| `F5-27` | Đánh dấu đã bỏ ở `01-rd/req/ai-review.md`; mã không cấp lại |
| `US-A2-06` | Đánh dấu đã bỏ ở `01-rd/req/user_stories/a2_instructor.md`; mã không cấp lại |
| Nav "Chấm bài" khu Giảng viên | Bỏ |
| Khối/thẻ/cột/liên kết "Cần chấm" ở `INS0101`, `INS0201`, `INS0202`, `INS0204` | Bỏ, xem từng file |
| Câu hỏi mở Q1 tới Q13 của màn này, và các quyết định đã đóng chúng | **Đã đóng: màn bị bỏ** |
| Giữ nguyên | Báo cáo phân tích bài giải do học viên tự xin (F5.1), Function `CLASS_MANAGEMENT`, quyền đọc mã nguồn học viên trong lớp (`DEC-2026-1013-instructor-avatar-menu-and-read-source`) |

## 4. Câu hỏi còn mở phát sinh từ việc bỏ màn này

* Lối vào trang đọc-chỉ `/instructor/submissions/{id}` đã mất cùng popup chấm bài: xem Q5 ở
  `01-rd/screens/teacher/INS0204_class_student_detail.md`.
* ~~Số phận trường `ai_score_10`: xem OQ-F5-A ở `01-rd/req/ai-review.md`.~~ **ĐÃ BỎ 2026-10-14
  (`DEC-2026-1014-drop-ai-score-10`):** cột `ai_score_10` bị bỏ, OQ-F5-A đã đóng ở `01-rd/req/ai-review.md`.

Lịch sử nội dung cũ của file (bản trước 2026-10-13) xem qua `git log -- 01-rd/screens/teacher/INS0301_grading.md`.
