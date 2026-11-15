# Hướng dẫn chạy AlgoPrep thủ công

Dùng **PowerShell** (không dùng Git Bash: nó đổi `/api/v1` thành đường dẫn Windows). Gốc repo: `F:\prj_algoprep`.

| Mức | Cần | Xem được |
| :--- | :--- | :--- |
| A. Chỉ giao diện (mock) | Frontend | Bố cục, luồng màn hình |
| B. Backend thật | Docker + backend + frontend | Đăng nhập, ngân hàng câu hỏi, Phỏng vấn giả lập, Solution Review, admin AI |
| C. Có chấm bài | Mức B + worker | Nộp bài, kết quả từng testcase |

Mức A: chỉ làm bước 3 (`pnpm dev`). Mức B, C: làm bước 1, 2, 3 trong ba cửa sổ riêng.

## 1. Hạ tầng (mức B, C)

```powershell
cd F:\prj_algoprep\05-coding
docker compose up -d
docker compose ps
```

Cổng: Postgres **5433**, Redis 6379, RabbitMQ 5672 (UI 15672), MinIO 9000 (UI 9001), go-judge 5050, Mailpit UI **8025**, Grafana **3001**, Prometheus 9090.

Backend chết lúc khởi động với lỗi `Unable to determine Dialect` thường do Docker chưa bật: xem `docker compose ps` trước (`backend/README.md:179-182`).

## 2. Backend, cổng 8080 (mức B, C)

Spring Boot **không tự đọc `.env`**, phải nạp vào tiến trình trước:

```powershell
cd F:\prj_algoprep\05-coding
Get-Content .env | Where-Object { $_ -match '^\s*[A-Za-z_][A-Za-z0-9_]*=' } | ForEach-Object { $k, $v = $_ -split '=', 2; Set-Item -Path "env:$($k.Trim())" -Value $v }
cd backend
.\mvnw.cmd -q install -DskipTests
```

Rồi chạy một trong hai:

| Mức | Lệnh |
| :--- | :--- |
| B | `.\mvnw.cmd -pl algoprep-bootstrap spring-boot:run "-Dspring-boot.run.profiles=local"` |
| C | `.\mvnw.cmd -pl algoprep-bootstrap spring-boot:run "-Dspring-boot.run.profiles=local,worker,outbox,maintenance"` |

Sẵn sàng khi `http://localhost:8080/actuator/health` trả `UP`. Mức C: chỉ **một** tiến trình bật worker; engine là go-judge ở cổng 5050, địa chỉ lấy từ `ALGOPREP_WORKER_ENGINE_URL` (`judge-engine/WORKER-RUNBOOK.md:35`).

**Lệch checksum Flyway `V1.004`:** đổi `ALGOPREP_DB_NAME` sang tên DB mới (migration chạy lúc khởi động), hoặc chạy `flyway repair`.

## 3. Frontend, cổng 3000

Mức A (mock):

```powershell
cd F:\prj_algoprep\05-coding\frontend
pnpm dev
```

Mức B, C (nối backend thật):

```powershell
cd F:\prj_algoprep\05-coding\frontend
$env:NEXT_PUBLIC_MOCK_DATA = "false"
$env:NEXT_PUBLIC_API_BASE_URL = "/api/v1"
$env:API_PROXY_TARGET = "http://localhost:8080"
pnpm dev
```

Mở `http://localhost:3000/login`. Đổi biến `NEXT_PUBLIC_*` thì phải khởi động lại `pnpm dev`. Không sửa `.env.local`.

## 4. Đăng nhập

- **Admin:** `admin@algoprep.local`, mật khẩu ở `ALGOPREP_BOOTSTRAP_ADMIN_PASSWORD` trong `.env` (lần đầu có thể bắt đặt mật khẩu mới).
- **OTP, mật khẩu tạm:** Mailpit `http://localhost:8025`.
- **Học viên, giảng viên:** admin tạo ở trang quản lý người dùng.

## 5. Xem F5, F6

- **Phỏng vấn giả lập:** `/mock-interview`, chọn **Kho câu hỏi** hoặc **Tự chọn chủ đề**. Gợi ý tối đa 3 lần mỗi phiên (lần 4 báo `AIR-117`); mở từ bài nộp thì không có nút Gợi ý.
- **Solution Review:** cần bài đã `Accepted` (mức C), vào `/review`.
- **Luyện tập F6:** chế độ luyện có nhận xét AI.
- **Đổi model AI:** sửa `ALGOPREP_AI_DEFAULT_MODEL` trong `.env` (mặc định `baidu/ernie-5.5-flash-preview:free`, dự phòng `nvidia/nemotron-3.5-super-vl:free`), nạp lại `.env`, khởi động lại backend. Lỗi 429 là hết hạn mức model miễn phí, không phải lỗi mã.

## 6. Dừng

- Backend, frontend: `Ctrl+C`.
- Hạ tầng: `docker compose stop` (giữ dữ liệu). `docker compose down -v` **xoá cả dữ liệu**.

## Nguồn

`backend/README.md:179-182`; `judge-engine/WORKER-RUNBOOK.md:35`; `docker-compose.yml`; `.claude/skills/fe-mock-to-real/references/run-stack.md`.
