# go-judge — engine thực thi mã trong sandbox

Engine mặc định của AlgoPrep, chốt bởi `DEC-2026-0823-go-judge-default-engine`. Chi tiết kỹ thuật:
`01-rd/system/judge_engine.md`.

## Vị trí trong kiến trúc

go-judge là **dịch vụ ngoài, không phải module trong monolith**
(`01-rd/system/backend_architecture.md` mục 6.A). Cửa duy nhất vào nó là
`algoprep-judge/infrastructure/judgeengine/GoJudgeAdapter`, hiện thực cổng ra `JudgeExecutionPort`.
Không module nào khác được gọi engine trực tiếp.

Cổng phải **trung lập theo engine**: chữ ký chỉ có dạng "chạy một testcase, trả một kết quả", không
mang field riêng của go-judge (`cmd[]`, `copyIn`) cũng không mang field riêng của Judge0
(`status.id`, token, webhook). Nhờ vậy `Judge0Adapter` vẫn cắm lại được sau này mà không đổi
domain/application.

## Trạng thái: chỉ có hạ tầng, chưa có adapter

| Có | Chưa có |
| :--- | :--- |
| Service `go-judge` trong `05-coding/docker-compose.yml` (image `criyle/go-judge:v1.12.3`) | `GoJudgeAdapter` — thuộc slice `judge-orchestration` |
| `privileged: true` và `shm_size` để `go-sandbox` tạo được namespace và đặt cgroup limit | Cấu hình biên dịch/chạy cho Java, C++, Python |
| Endpoint `/version` dùng làm healthcheck | Ánh xạ kết quả engine sang value object của AlgoPrep (ACL) |

Thư mục này được mount vào container tại `/opt/algoprep/judge-engine` (chỉ đọc). Nó **rỗng có chủ
đích**: cấu hình biên dịch từng ngôn ngữ phụ thuộc `02-bd/architecture/harness.md` và
`03-dd/api/judge-orchestration.md`, cả hai chưa tồn tại. Đặt sẵn một cấu hình đoán trước ở đây sẽ
thành nợ mà không ai biết là nợ.

## Kết quả kiểm thực nghiệm 2026-09-01

Hai điều đã kiểm bằng lệnh thật trên máy phát triển (Windows 11 + Docker Desktop, WSL2, **cgroup
v2**), không phải suy luận từ tài liệu.

**1. Sandbox chạy được mã — rủi ro R1 đóng bằng thực nghiệm.** `POST /run` với `/bin/echo` trả về:

```json
[{"status":"Accepted","exitStatus":0,"time":904000,"memory":262144,
  "runTime":797862,"procPeak":1,"files":{"stdout":"sandbox chay duoc\n","stderr":""}}]
```

Đây đúng hình dạng dữ liệu mà `JudgeExecutionPort` sẽ tiêu thụ: một trạng thái, một exit status,
thời gian, bộ nhớ, stdout/stderr. Trên cùng máy này Judge0 sẽ không chạy được vì nó đòi cgroup v1.

**2. Cần một dòng cấu hình compose mà tài liệu chưa ghi.** go-judge chết trong vòng lặp restart với
`cgroup path is empty` cho tới khi thêm `cgroup: host` và mount `/sys/fs/cgroup`. Nguyên nhân đọc
được từ log của chính nó: nó thử nối systemd dbus để tạo cgroup, VM của Docker Desktop không có
dbus, nên nó fallback sang "dùng cgroup prefix hiện tại" — và trong cgroup namespace riêng,
container đọc đường dẫn cgroup của chính nó ra chuỗi rỗng. Chi tiết ghi ngay tại service `go-judge`
trong `05-coding/docker-compose.yml`.

Đây **không phải** rủi ro cgroup v1 cũ quay lại: Judge0 đòi *bản* cgroup v1, còn go-judge chạy tốt
trên v2 và chỉ cần một đường dẫn cgroup thật.

## Nợ đã biết: image mặc định KHÔNG có compiler nào

Đã kiểm trong container `criyle/go-judge:v1.12.3`:

| Công cụ | Có trong image |
| :--- | :--- |
| `python3` · `python` | **KHÔNG** |
| `g++` · `gcc` | **KHÔNG** |
| `javac` · `java` | **KHÔNG** |

`mount.yaml` của go-judge có bind mount cho `/etc/java-21-openjdk`, `/var/lib/ghc`, `/etc/mono`,
`/etc/fpc.cfg` — nhưng đó là mount **tuỳ chọn cho toolchain nếu có sẵn**, không phải toolchain được
cài. Image mặc định chỉ có sandbox, không có ngôn ngữ.

Hệ quả cho slice `judge-orchestration`: phải dựng **một image riêng** (`FROM criyle/go-judge` cộng
JDK 21, g++, Python 3), hoặc mount toolchain từ ngoài vào. Không có bước đó thì mọi bài nộp trả
`Internal Error: execve: no such file or directory` — đúng như lần chạy thử Python ở trên. Việc này
thuộc slice `judge-orchestration`, không thuộc khung base, nhưng phải biết trước khi lên kế hoạch
slice đó.

## Vì sao `privileged: true`

Không phải để cho tiện. `go-sandbox` cần tạo namespace (PID, mount, network) và đặt giới hạn cgroup
cho tiến trình con — đó chính là cơ chế cách ly mã người dùng (`01-rd/overview/overview.md` mục
1.F). Quyền này không thể cấp cho tiến trình ứng dụng web, và đó cũng là lý do engine phải tách
thành dịch vụ riêng thay vì nằm trong JVM.

## Vì sao go-judge chứ không Judge0

Judge0 yêu cầu cgroup **v1**, còn Linux hiện đại và WSL2 mặc định **v2** — đây từng là rủi ro số
một của đề tài (R1). go-judge hỗ trợ cả v1 và v2, nên rủi ro đó đã đóng cho luồng mặc định
(`DEC-2026-0823-go-judge-default-engine`, `README.md` mục 7). go-judge cũng **stateless**: không cần
PostgreSQL/Redis riêng như Judge0.

Rủi ro cgroup v1 chỉ quay lại nếu `Judge0Adapter` được bật làm engine thay thế.

## Lệnh kiểm nhanh

```bash
cd 05-coding
docker compose up -d go-judge
curl http://localhost:5050/version          # phai tra 200
curl http://localhost:5052/metrics | head   # metric cho Prometheus
```
