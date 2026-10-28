# F4 judge-orchestration — testcase QA

| Cụm | Phạm vi | Ca |
| :--- | :--- | ---: |
| [Tiếp nhận và giới hạn đầu vào](F4_submission_input_limits.md) | AC-01–07, 09; quota, byte, ngôn ngữ, mô hình | 24 |
| [Run Code](F4_run_code_limits.md) | AC-73 và nhánh AC-10, 12–13 | 10 |
| [Verdict, bảo mật và queue](F4_verdict_privacy_queue.md) | AC-15–21, 48, 76 | 13 |
| [Điều khiển queue](F4_queue_control.md) | AC-49–50 | 8 |
| [Autoscaler worker](F4_worker_autoscale.md) | AC-77 | 10 |
| [Lưu giữ và hiển thị kết quả](F4_result_retention_visibility.md) | AC-31–33, 76 | 8 |
| [Lỗi nộp, chấm và sweep](F4_submission_resilience.md) | AC-08, 22–24 | 8 |
| [Tiến trình realtime](F4_submission_realtime.md) | AC-25–29 | 9 |
| [Outbox sự kiện chấm](F4_outbox_events.md) | AC-63–65; payload, relay, dedupe, retention | 8 |
| [Lịch sử và số liệu học viên](F4_student_history_metrics.md) | AC-36–40; danh sách, lọc, đang làm dở, UTC+7 và range | 13 |
| [Chi tiết bài nộp, Beats và stderr](F4_submission_detail_beats_stderr.md) | AC-30, 34–35; kết quả Accepted, tập so sánh, lỗi chạy | 13 |
| [Phạm vi và số liệu giảng viên](F4_instructor_scope_metrics.md) | AC-41–47; danh sách, quyền lớp, lịch sử, tuần đủ | 15 |
| [Lệnh và cấu hình quản trị](F4_admin_configuration.md) | AC-51–54, 57–62; xung đột, quyền, giới hạn, audit | 15 |
| [Sự kiện hạ tầng](F4_infra_events.md) | AC-55–56; nguồn, mức, cursor, thẻ 24 giờ | 4 |
| [Run Code và giới hạn](F4_run_compile_limits.md) | AC-11, 14; lỗi biên dịch và giới hạn hiệu dụng | 4 |
| [Cổng đọc liên module](F4_cross_module_reads.md) | AC-66–72; cấu hình, thống kê, AI, Accepted, ngôn ngữ | 11 |

Tổng **173 ca thiết kế** trong 16 cụm; 77/77 AC có ca theo nguồn, mốc thiết kế đã đóng và chưa thực thi. Nhãn ngày của nhóm tuần `all` trong `TC-F4-AC39-03` đã chốt là thứ Hai đầu tuần UTC+7. Cụm snapshot chung F2/F4 được quản lý ở [F2 problem-bank](../F2_problem-bank/README.md); [biên bản đợt 1](../../AUDIT_DOT1.md).
