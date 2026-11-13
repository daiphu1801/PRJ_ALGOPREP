# Hướng dẫn chạy AlgoPrep thủ công

Dùng **PowerShell** (không dùng Git Bash: nó đổi `/api/v1` thành đường dẫn Windows). Gốc repo: `F:\prj_algoprep`.

Có ba mức chạy, chọn theo việc cần xem:

| Mức | Cần | Dùng để xem |
| :--- | :--- | :--- |
| A. Chỉ giao diện (mock) | Chỉ frontend | Bố cục, luồng màn hình, không cần backend |
| B. Backend thật | Docker và backend và frontend | Đăng nhập, ngân hàng câu hỏi, Phỏng vấn giả lập, Solution Review, trang admin AI |
| C. Đầy đủ có chấm bài | Mức B và worker | Nộp bài, kết quả từng testcase |

## 1. Hạ tầng (mức B và C)

```powershell
cd F:\prj_algoprep\05-coding
docker compose up -d
docker compose ps
```

Cổng mặc định: Postgres **5433** (theo `.env`), Redis 6379, RabbitMQ 5672 (giao diện 15672), MinIO 9000 (giao diện 9001), go-judge 5050, Mailpit giao diện **8025**, Grafana **3001**, Prometheus 9090.

Nếu backend chết ngay lúc khởi động với lỗi Hibernate `Unable to determine Dialect`, nguyên nhân thật thường là Docker chưa bật: xem `docker compose ps` trước (`backend/README.md:179-182`).

## 2. Backend (mức B và C)

Spring Boot **không tự đọc `.env`**, phải nạp vào tiến trình trước. Nạp luôn cả model AI mặc định (đang là `baidu/ernie-5.5-flash-preview:free`).

```powershell
cd F:\prj_algoprep\05-coding
Get-Content .env | Where-Object { $_ -match '^\s*[A-Za-z_][A-Za-z0-9_]*=' } | ForEach-Object { $k, $v = $_ -split '=', 2; Set-Item -Path "env:$($k.Trim())" -Value $v }
cd backend
.\mvnw.cmd -q install -DskipTests
```

Lệnh chạy:

| Mức | Lệnh |
| :--- | :--- |
| B (không chấm bài) | `.\mvnw.cmd -pl algoprep-bootstrap spring-boot:run "-Dspring-boot.run.profiles=local"` |
| C (có worker chấm bài) | `.\mvnw.cmd -pl algoprep-bootstrap spring-boot:run "-Dspring-boot.run.profiles=local,worker,outbox,maintenance"` |

Sẵn sàng khi `http://localhost:8080/actuator/health` trả `UP`. Lệnh chạy mãi trong cửa sổ đó; `Ctrl+C` để dừng. Mức C: chỉ **một** tiến trình được bật worker. Engine chấm là go-judge ở cổng 5050 trong compose (đã có Java, C++, Python; `judge-engine/WORKER-RUNBOOK.md:35`), địa chỉ lấy từ `ALGOPREP_WORKER_ENGINE_URL` trong `.env`.

**Lệch checksum Flyway:** nếu DB `algoprep` của bạn đã áp `V1.004` bản cũ, backend có thể từ chối khởi động vì checksum không khớp. Cách nhanh nhất là dùng DB mới (đổi `ALGOPREP_DB_NAME` sang tên khác; schema lấy từ `database/init/01-schemas.sql`, migration chạy khi khởi động), hoặc chạy `flyway repair` trên DB hiện tại.

## 3. Frontend

Mức A (mock, không cần backend): mở cửa sổ PowerShell khác.

```powershell
cd F:\prj_algoprep\05-coding\frontend
pnpm dev
```

Mức B và C (nối backend thật):

```powershell
cd F:\prj_algoprep\05-coding\frontend
$env:NEXT_PUBLIC_MOCK_DATA = "false"
$env:NEXT_PUBLIC_API_BASE_URL = "/api/v1"
$env:API_PROXY_TARGET = "http://localhost:8080"
pnpm dev
```

Mở `http://localhost:3000/login`. Đổi các biến `NEXT_PUBLIC_*` thì phải khởi động lại `pnpm dev`. Không sửa `.env.local`.

## 4. Tài khoản và dữ liệu

- **Admin:** email `admin@algoprep.local`; mật khẩu lấy từ `ALGOPREP_BOOTSTRAP_ADMIN_PASSWORD` trong `.env`. Lần đăng nhập đầu có thể bắt đặt mật khẩu mới.
- **Mã OTP, mật khẩu tạm:** hộp thư giả Mailpit, `http://localhost:8025`.
- **Tài khoản học viên hoặc giảng viên:** admin tạo ở trang quản lý người dùng; mật khẩu tạm gửi qua Mailpit.

## 5. Xem tính năng F5 và F6

- **Phỏng vấn giả lập có nút Gợi ý:** vào `/mock-interview`, chọn lối vào **Kho câu hỏi** hoặc **Tự chọn chủ đề** (công tắc "Cho phép gợi ý" bật sẵn, tối đa 3 gợi ý mỗi phiên, lần 4 báo `AIR-117`). Mở phiên **từ bài nộp** thì không có nút Gợi ý.
- **Solution Review:** cần một bài đã `Accepted` (mức C), rồi mở trang `/review`.
- **Luyện tập F6:** câu hỏi trong ngân hàng, chế độ luyện có nhận xét AI.
- **Đổi model AI:** sửa `ALGOPREP_AI_DEFAULT_MODEL` trong `.env`, nạp lại `.env` và khởi động lại backend. Dự phòng: `nvidia/nemotron-3.5-super-vl:free`.
- Model miễn phí có hạn mức: lỗi 429 là hết hạn mức ở nguồn, không phải lỗi mã.

## 6. Dừng

- Backend và frontend: `Ctrl+C` ở từng cửa sổ.
- Hạ tầng: `cd F:\prj_algoprep\05-coding; docker compose stop` (giữ dữ liệu). `docker compose down -v` **xoá cả dữ liệu**, chỉ dùng khi muốn làm lại từ đầu.

## Nguồn

`backend/README.md:38-46, 92-93, 179-182`; `judge-engine/WORKER-RUNBOOK.md:35, 44`; `docker-compose.yml` (cổng); `.claude/skills/fe-mock-to-real/references/run-stack.md` (biến frontend, profile worker).
