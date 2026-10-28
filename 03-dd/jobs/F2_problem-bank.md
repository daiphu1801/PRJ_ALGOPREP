# DD Jobs — F2 `problem-bank`

> Cập nhật 2026-10-26. Nguồn: `03-dd/api/F2_problem-bank.md` §59, `03-dd/logic/F2_problem-bank.md` BR-26, `02-bd/database/F2_problem-bank.md` §1.16. Hai job chỉ xử lý Outbox của schema `problem`; tiêu chí nghiệm thu ở `04-tdd/modules/F2_problem-bank.md` AC-89, AC-90, AC-94.

## 0. Quy ước chung

- Mọi instance có thể chạy job; khoá dòng `FOR UPDATE SKIP LOCKED` tránh lấy cùng lô. `eventId` giữ nguyên khi phát lại; consumer xử lý ít nhất một lần và khử trùng.
- Payload không chứa nội dung testcase, mã đáp án mẫu hoặc mã người học. Log chỉ ghi `eventId`, `eventType`, `aggregateId`, số lần thử và `traceId`.
- Cấu hình vận hành ở `application.yml`: `problem.outbox.relay-delay-ms=2000`, `batch-size=100`, `cleanup-delay-hours=24`, `retention-days=90`. Đây là hằng số triển khai, có thể điều chỉnh khi đo tải.

## 1. JOB-PB-01 — Relay Outbox

| Mục | Hợp đồng |
| :--- | :--- |
| Kích hoạt | `fixedDelay = 2 s` sau khi lần trước kết thúc |
| Đọc | `problem.outbox_events WHERE published_at IS NULL ORDER BY created_at, id LIMIT 100 FOR UPDATE SKIP LOCKED` |
| Phát | RabbitMQ exchange topic `algoprep.domain-events`, `routing_key` từ dòng, phong bì `payload` nguyên vẹn |
| Thành công | Publisher confirm xong mới ghi `published_at = now()` và tăng `publish_attempts` |
| Thất bại | Tăng `publish_attempts`, giữ `published_at = NULL`, ghi WARN và thử ở lượt sau; không bỏ dòng theo số lần thử |
| Đồng thời | Khoá dòng cho từng bản ghi, publisher confirm timeout tối đa 5 giây; hai instance không claim cùng bản ghi một lúc; broker có thể nhận trùng nếu ack thất lạc |

Relay không sinh sự kiện mới. Nếu payload JSON không hợp lệ hoặc routing key chưa biết, giữ dòng chưa publish, ghi `ERROR` có `eventId` để vận hành sửa; không đánh dấu đã phát. Theo dõi số dòng chưa publish, tuổi dòng cũ nhất và số lần thử cao nhất. `ProblemAssignedToClass` phải tới F1 với cùng `eventId` qua mọi lần thử để feed không nhân đôi.

## 2. JOB-PB-02 — Dọn Outbox đã phát

| Mục | Hợp đồng |
| :--- | :--- |
| Kích hoạt | Mỗi 24 giờ |
| Đọc | `published_at < now() - interval '90 days'`, tối đa 1.000 dòng mỗi giao dịch |
| Ghi | Xoá các dòng đã phát qua hạn giữ; lặp lô tới khi không còn dòng phù hợp hoặc hết hạn lần chạy |
| Bất biến | Không bao giờ xoá dòng `published_at IS NULL`, dù cũ bao lâu |
| Lỗi | DB lỗi thì rollback lô hiện tại, ghi ERROR, lượt sau thử lại |

## 3. Điểm kiểm tra DD

| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB-J01 | Normal | Thay đổi bài và dòng Outbox commit cùng giao dịch, relay phát đủ phong bì và đánh dấu sau confirm |
| PB-J02 | Abnormal | RabbitMQ không xác nhận: dòng còn `published_at=NULL`, phát lại cùng `eventId` |
| PB-J03 | Concurrency | Hai instance relay không claim cùng dòng khi cùng chạy |
| PB-J04 | Boundary | Dọn dòng đã phát quá 90 ngày; không dọn dòng chưa phát quá 90 ngày |
