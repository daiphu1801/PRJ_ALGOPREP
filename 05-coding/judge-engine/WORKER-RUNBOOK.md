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

Số consumer bắt đầu từ1, tăng tối đa theo `judge.autoscale.max-workers` (mặc định8). Mỗi3 giây (`judge.worker.scale-delay-ms`) kiểm số bài chờ; vượt `autoscale_job_threshold` đủ `judge.autoscale.scale-up-polls` lần liên tiếp (mặc định2) thì tăng1; rỗng đủ `judge.autoscale.scale-down-polls` lần (mặc định4) thì giảm1. Ba cấu hình phải dương. API queue snapshot dùng đúng cùng trần cấu hình, không quảng cáo giới hạn khác runtime. Ngưỡng DB mặc định20, demo có thể đặt5 trong màn quản lý hàng đợi. Theo `DEC-2026-1112-f4-worker-scaling-and-db-wait`, cấu hình mặc định giữ nguyên hành vi hiện tại.

Chỉ một tiến trình được bật worker. Profile cấp48 kết nối Hikari; worker từ chối khởi động nếu `maximum-pool-size < max-workers + 10` (mặc định tối thiểu18). Phần dự trữ dành cho lease leader, API và các giao dịch ghi/đọc khác; đây là guard tối thiểu, không phải chứng nhận pool18 đủ tải120 người. Khi nâng trần consumer phải nâng pool theo và giữ tổng các pool dưới `max_connections` PostgreSQL. Chờ lấy kết nối tối đa2000ms; khi cạn pool, detail/progress trả503 JDG-506 cố định thay vì chờ30 giây. Profile worker yêu cầu URL sandbox; mạng sandbox vẫn cô lập và không khởi động API process thứ hai rồi kỳ vọng STOMP nội bộ đi xuyên process.

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
