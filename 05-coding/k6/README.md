# Kiểm thử tải đường chấm bài (k6)

Kịch bản đo thông lượng và thời gian chờ kết quả của luồng nộp bài, chấm, trả kết quả. Kết quả lượt đo đầu tiên: `06-plan/reports/261108-1320-f3-f4-load-test/report.md`.

## Chuẩn bị

1. Cài k6 (v2.x). Trên Windows có thể tải bản zip từ trang phát hành của Grafana và giải nén, không cần quyền quản trị.
2. Dựng hạ tầng (`docker compose up -d`, gồm go-judge có toolchain ở cổng 5050) và chạy **một** backend với profile `local,worker,outbox,maintenance`, `ALGOPREP_WORKER_ENGINE_URL=http://localhost:5050`.
3. Nên dùng một DB riêng cho tải (ví dụ `algoprep_load`, dựng từ `database/init/01-schemas.sql`) để không làm lệch số liệu của DB demo. Backend dùng chung hàng đợi RabbitMQ, nên không chạy hai backend cùng lúc.
4. Chạy backend bằng `java -jar`. **Không** dùng `-XX:TieredStopAtLevel=1`: với cờ này JVM đã sập hai lần khi chạy tải (xem báo cáo, mục 6).

## Chạy

```
ALGOPREP_DB_NAME=algoprep_load node seed-fixture.mjs        # in ra PROBLEM_ID
k6 run -e PROBLEM_ID=<id> -e LEVELS=2,5,10,20,40,80 -e HOLD=45s -e GAP=20 submit-load.js
```

| Biến | Mặc định | Ý nghĩa |
| :--- | :--- | :--- |
| `PROBLEM_ID` | bắt buộc | Bài đã xuất bản do `seed-fixture.mjs` dựng |
| `LEVELS` | `2,5,10,20` | Số người dùng ảo của từng bậc, chạy nối tiếp |
| `HOLD` | `45s` | Thời gian giữ mỗi bậc |
| `GAP` | `20` | Giây nghỉ giữa các bậc để hàng đợi xả |
| `MAX_WAIT` | `120` | Giây tối đa chờ một bài ra kết quả |
| `POLL` | `0.5` | Giây giữa hai lần hỏi trạng thái |
| `BASE_URL`, `MAILPIT_URL` | `localhost:8080/api/v1`, `localhost:8025/api/v1` | Địa chỉ API và Mailpit (lấy OTP) |

Mỗi người dùng ảo là một tài khoản riêng, tạo trong `setup()` qua API đăng ký và OTP thật, nên tổng số tài khoản bằng tổng các bậc. Mỗi người chỉ được nộp 60 lần mỗi giờ cho một bài (`JDG-106`), vì vậy đừng đặt `HOLD` quá dài.

## Lưu ý khi chạy

- k6 chạy cùng máy với backend sẽ tranh CPU; ghi lại cấu hình máy khi báo số.
- Nếu backend sập giữa chừng, dừng k6 ngay (`Get-Process k6 | Stop-Process`): k6 cũ còn chạy sẽ đè lên lượt sau.
- `seed-fixture.mjs` tạo thêm một tài khoản giảng viên và một bài; xoá DB tải sau khi đo.

## Ghi CPU trong lúc chạy

`sample-cpu.ps1` ghi CPU cả máy, backend, k6 và go-judge (`docker stats`) mỗi khoảng 2 giây ra CSV, dừng khi file `-StopFile` xuất hiện. Ví dụ: `powershell -File sample-cpu.ps1 -BackendPid <pid> -Out cpu.csv -StopFile stop.flag`. Kết quả mẫu: `06-plan/reports/261108-1646-f3-f4-go-judge-parallelism-8/`.
