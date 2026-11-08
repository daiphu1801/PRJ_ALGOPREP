# Chạy worker local và kiểm tra vận hành

Worker là role trong backend. Với broker STOMP nội bộ hiện tại, chạy một backend nhận HTTP/WebSocket và bật worker trên chính tiến trình đó. Chưa dùng hướng dẫn này để tách API/worker thành hai tiến trình hoặc triển khai nhiều node.

## Sandbox có toolchain

Từ `05-coding`, dựng image theo `judge-engine/Dockerfile` nếu máy chưa có image đã kiểm, rồi kiểm tra toolchain trước khi bật worker:

```powershell
docker build -t algoprep-judge-toolchain:f3-20261101 ./judge-engine
docker compose -f docker-compose.worker-sandbox.yml up -d
docker compose -f docker-compose.worker-sandbox.yml ps
```

Kiểm cả ba ngôn ngữ và hai model trên sandbox này bằng các bài tích hợp thực:

```powershell
Push-Location backend
.\mvnw.cmd -pl algoprep-bootstrap -am test '-Dtest=ExecutionBridgeFlowTest,PersistedWorkerFlowTest,WorkerOperationsFlowTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-Djudge.go-judge.base-url=http://127.0.0.1:5055'
Pop-Location
```

Đây là lệnh kiểm khả năng chạy toolchain/worker; không thay thế kiểm tra hồi quy và coverage của toàn backend.

Compose này mở sandbox ở `127.0.0.1:5055`, giới hạn truy cập trên host. Từ `DEC-2026-1107-go-judge-toolchain-image-in-compose`, service `algoprep-go-judge` trong compose hạ tầng (cổng 5050) cũng chạy image toolchain này, nên `docker compose up -d go-judge` là đủ để chấm ba ngôn ngữ; compose sandbox 5055 chỉ cần khi muốn một engine thứ hai tách riêng. Đặt `ALGOPREP_WORKER_ENGINE_IMAGE` khi dùng image đã kiểm khác; cấu hình không tự pull một image thay thế. Backend không cần Docker socket. Không thêm `-net-share`: job yêu cầu bật mạng hiện phải kết thúc SYSTEM_ERROR/SANDBOX_FAULT theo quyết định chủ dự án.

## Backend

Thiết lập các biến kết nối PostgreSQL/Redis/RabbitMQ/MinIO theo cấu hình local của dự án và URL sandbox rõ ràng:

```powershell
$env:ALGOPREP_WORKER_ENGINE_URL='http://127.0.0.1:5055'
# Chạy ứng dụng bằng cấu hình local,worker trong IDE hoặc lệnh khởi chạy backend hiện có.
# Ví dụ tham số JVM/ứng dụng: --spring.profiles.active=local,worker
```

Chỉ một tiến trình được bật worker. Profile cấp 32 kết nối Hikari (`DEC-2026-1108-hikari-pool-32`); không giảm dưới 10: tám lease job, một lease leader và ít nhất một kết nối cho giao dịch ghi. Chín kết nối đầu bị giữ cố định, phần còn lại phục vụ API và giao dịch ghi; pool 16 cạn ở khoảng 80 người nộp đồng thời (báo cáo `261108-1320-f3-f4-load-test`). Nâng số consumer thì nâng pool theo, và giữ tổng pool dưới `max_connections` của PostgreSQL (mặc định image là 100). Profile worker yêu cầu URL sandbox; không lặng lẽ nhận URL mặc định của engine chưa có toolchain. Không khởi động thêm một API process riêng rồi kỳ vọng STOMP nội bộ phát qua process đó.

## Kiểm tra

- `/actuator/health/readiness` phải UP; role worker bao gồm `gradingWorker`. Pause có chủ đích vẫn UP.
- Prometheus có `judge_grading_consumers_target` và `judge_grading_consumers_active`. Target giữ nguyên khi pause; active về 0 sau drain. Metric không đợi khóa điều khiển đang drain.
- Pause không ngắt job đang xử lý. Resume nhận phần còn trong queue. Tắt autoscale đặt target về 1 và cho consumer đang bận hoàn tất.
- Nếu mất kết nối leader, worker dừng nhận bài, readiness DOWN. Tiến trình mới tranh khóa; không gọi start lại tiến trình đã mất lease để thay cơ chế restart.
- Ngân sách compile lấy từ `judge.grading_defaults.compile_timeout_sec`; không dùng giới hạn run testcase để thay ngân sách compile. Xem số đo trong report closeout trước khi chọn cấu hình cho host và tải triển khai. Mặc định seed không bị đổi bởi fixture kiểm thử 60 giây.

Không có relay/sweep trong hướng dẫn này. Trước khi triển khai production cần hoàn tất các job đó, chiến lược realtime nhiều tiến trình nếu tách role, và thiết lập giám sát/restart của môi trường đích.

## Dừng sandbox tùy chọn

```powershell
docker compose -f docker-compose.worker-sandbox.yml down
```

Lệnh trên chỉ quản lý project sandbox tùy chọn; không dùng `down` của compose hạ tầng để dọn sandbox kiểm thử.
