# Audit F5 cổng/job và F6 danh mục/tích hợp

> 2026-10-28. Áp dụng `algoprep-qa-audit`. Chỉ review thiết kế, **0 ca thực thi**; đợt 3 chưa đóng.

Đây là biên bản batch đầu, giữ số liệu tại thời điểm243 ca. Review các cụm còn lại đã hoàn tất sau batch; trạng thái/count/oracle hiện hành nằm ở [audit đợt3](AUDIT_DOT3_F5_F6.md), thay cho mục “phần còn lại” lịch sử bên dưới.

## Phạm vi đã đọc sâu

- F5 AC-49–58: 27 ca ở [cổng/sự kiện](modules/F5_ai-review/F5_admin_ports_events_resilience.md) và [job](modules/F5_ai-review/F5_admin_jobs_budget_lifecycle.md); đối chiếu DD API §32–33, logic BR-AI-02/03/04/09/14/15, validation §9 và JOB-AI-01–05.
- F6 AC-24–29: 24 ca sau bổ sung ở [danh mục](modules/F6_interview-bank/F6_admin_catalog.md), đối chiếu DD API §9–18 và validation §6. F6 AC-39–41: 13 ca [ngân sách/suy giảm/tiến độ](modules/F6_interview-bank/F6_admin_budget_resilience_progress.md), đối chiếu DD API §6/8/25–27 và F5 P1/P2.
- Tổng phạm vi đọc sâu lần này: 64 ca. Các cụm còn lại không được coi đã review sâu chỉ vì qua checker.

## Phát hiện và sửa

1. F5 AC-49 TDD/ca từng nói budget bị khoá vẫn lưu câu trả lời, trái hợp đồng F6 §6/26 và quyết định `DEC-2026-1023-dd-f6-practice-budget-and-prompt`. Sửa thành `429 INB-042`, không lưu lượt; giữ riêng lỗi hạ tầng budget => `201`, lưu FAILED.
2. P3 không dùng F5 hourly quota/cooldown nhưng AC50-02 đòi refund reservation. Sửa oracle thành các key không bị thay đổi; fault fixture timeout trước khi có usage để không suy mọi lỗi đều tiêu 0 token.
3. AC52-01 dùng lại cùng submission nhưng đòi event mới. Sửa thành hai submission cùng hash, tạo report/cache-hit riêng; gọi lại submission thứ hai không tạo event. AC52-03 tách publisher confirm khỏi consumer acknowledgment: consumer dừng vẫn có thể publish thành công và message nằm trong queue.
4. JOB-AI-04 chỉ kiểm stage không bảo đảm fresh activity làm sweep bỏ lượt. DD bổ sung lấy cùng session advisory lock, đọc lại idle eligibility dưới lock, giữ lock qua scoring/commit và conditional update theo observed updated_at. AC57-03 điều khiển fresh learner commit trước khi sweep lấy lock; kiểm không gọi scoring cho phiên đã hoạt động lại.
5. Owner chốt lại `DEC-2026-1028-qa-f5-anomaly-baseline-admin`: ADMIN tham gia baseline nhưng không nhận alert. DD và AC56-01 khớp: C10 + chín user một lượt + ADMIN30 => baseline 4.45, không alert; C40 => baseline 7.18, ratio5.57, một alert. Thêm subcase ratio đúng5.00 và retention đúng90 ngày vào ca hiện có.
6. F6 AC25-03 từng dùng hai displayName giống hệt để kiểm slug collision, nhưng request thứ hai phải bị duplicate-name guard chặn. Dùng `Cơ sở dữ liệu`/`Co so du lieu` để tên khác nhưng slug cùng base. Bỏ fixture seed vừa chiếm base vừa kỳ vọng base chưa dùng.
7. F6 AC25-05 còn ghi status audit failure chưa chốt. Đồng bộ `503 INB-503` đã được owner duyệt. Bổ sung AC25-06, AC27-04, AC28-04, AC29-06 cho các nhánh danh mục còn thiếu; trace TDD và README cập nhật.

## Kiểm tra và phần còn lại

Checker chạy trên toàn bộ 14 file F5/F6: **243 ID, 243 link trace, 0 lỗi cấu trúc** (F5 137, F6 106). Năm flag trong F6 catalog là câu phủ định `no open oracle`; đã đọc nguồn và xác nhận oracle cụ thể, không phải quyết định chưa chốt. Checker chỉ xác nhận cấu trúc/trace.

Còn phải đọc sâu F5 AC-01–48 và F6 AC-01–23/30–38, đối chiếu branch inventory với DD, trước khi đóng đợt 3. Không còn câu hỏi owner từ phạm vi lần này; vẫn cần môi trường, clock/fault hooks, broker và PostgreSQL thật để thực thi.
