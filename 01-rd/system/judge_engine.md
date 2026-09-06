# Judge Engine (go-judge) — Giới Thiệu, Vai Trò, Cài Đặt, Triển Khai Trên Windows

Tài liệu này gộp lại mọi thứ cần biết về engine chấm bài trước khi động vào phân hệ `judge-orchestration`
(F4) và `harness` (F3): engine mặc định là **go-judge**, nó đứng ở đâu trong kiến trúc AlgoPrep, và — vì
máy phát triển ở đây chạy Windows — cách cài đặt và chạy nó cho **local development**. File này trước đây
tên `judge0.md`, viết cho Judge0; đổi tên và viết lại theo `DEC-2026-0823-go-judge-default-engine` — quyết
định chọn go-judge làm engine mặc định, giữ khả năng đổi lại Judge0 qua thiết kế cổng trung lập.

Nguồn: `README.md` (mục 1.2, 4 F3/F4, 5, 7 — đã cập nhật theo `DEC-2026-0823-go-judge-default-engine`);
`01-rd/overview/overview.md`; `01-rd/overview/system_survey.md`; `01-rd/system/backend_architecture.md`;
`01-rd/system/environment.md`; `01-rd/system/codebase_structure.md`;
quyết định `DEC-2026-0823-go-judge-default-engine`.

---

## 1. go-judge là gì

go-judge (`github.com/criyle/go-judge`) là một **dịch vụ REST/gRPC mã nguồn mở để thực thi mã nguồn đa
ngôn ngữ trong sandbox**. Nó nhận mã nguồn và dữ liệu vào qua `stdin`/file, chạy mã trong sandbox riêng do
chính dự án viết — `go-sandbox` (không dựa trên `isolate` như Judge0) — rồi trả về `stdout`, `stderr`, exit
code, thời gian chạy và bộ nhớ đã dùng. `[SoT: Suy luận — từ README chính thức của `criyle/go-judge`,
kiểm chứng 2026-08-23, không phải tài liệu nội bộ AlgoPrep]`

Bản thân go-judge **không hỗ trợ mô hình bọc hàm** (user chỉ viết một hàm, không tự đọc/ghi
`stdin`/`stdout`) và **không có phản hồi định tính** — giống hệt hạn chế của Judge0 mà `README.md` mục 1.2
đã ghi cho toàn bộ nhóm "REST API thực thi mã nguồn đa ngôn ngữ". Đây là lý do AlgoPrep tự xây thêm ba tầng
phía trên engine chấm thay vì dùng nó trần: **bộ sinh mã bọc hàm** (F3), **tầng điều phối chấm bài** (F4),
và **phân hệ AI** (F5) (`README.md` mục 4).

### 1.1. Vì sao chọn go-judge làm engine mặc định

| Lý do | Diễn giải |
| :--- | :--- |
| Không xây lại sandbox | Cô lập tiến trình khi chạy mã không tin cậy là bài toán cấp kernel; `go-sandbox` đã giải quyết, và đang là engine chấm chạy thật trong Hydro — một online judge đang hoạt động, không phải chỉ là thư viện chưa ai dùng. Làm lại chỉ thêm rủi ro bảo mật, không tạo đóng góp học thuật mới (`README.md` mục 1.2.1) |
| Đúng tầng cần tích hợp | go-judge chỉ là API thực thi, không áp đặt mô hình bài toán hay quy trình nghiệp vụ, khác với các OJ trọn gói như DOMjudge/HUSTOJ — ba tầng đóng góp của đề tài xây trực tiếp lên trên được (`README.md` mục 1.2.1) |
| Không kén cgroup version | Hỗ trợ cả cgroup v1 và v2 — xem mục 3, đóng dứt điểm rủi ro R1 mà Judge0 để lại trên Windows/WSL2 (`DEC-2026-0823-go-judge-default-engine`) |
| Stateless, hạ tầng nhẹ | Không cần Postgres/Redis riêng cho engine như Judge0 — submission vẫn lưu ở database của AlgoPrep |
| Định vị đề tài | Đóng góp của AlgoPrep không nằm ở việc chấm bài, mà ở mô hình bọc hàm đa ngôn ngữ, phản hồi thời gian thực, và tầng đánh giá bằng AI — không đổi khi đổi engine (`README.md` mục 1.3) |

**Giá phải trả, ghi rõ ngay đây — không đổi so với Judge0:** vì go-judge chỉ nhận `stdin`/`stdout`, muốn có
mô hình bọc hàm thì AlgoPrep vẫn phải tự sinh mã bao (harness) cho cả ba ngôn ngữ Java/C++/Python — đây vẫn
là lý do F3 tồn tại và là trọng tâm kỹ thuật của đề tài, không liên quan tới việc chọn engine nào.

**Đánh đổi khi rời Judge0, nói thẳng không giấu:** go-judge ít tiền lệ "chuẩn thi đấu ICPC/Olympic" hơn
Judge0, và sandbox tự viết (`go-sandbox`) chưa có "tiếng tăm học thuật" ngang `isolate` (dùng ở IOI nhiều
năm). Đây là lý do thiết kế cổng trung lập ở mục 2.2 tồn tại — nếu đồ án cần quay lại lập luận đó, chuyển
adapter là đủ, không phải redesign.

---

## 2. Vai trò của go-judge trong AlgoPrep

### 2.1. Vị trí trong kiến trúc

go-judge là **dịch vụ ngoài (external service)**, không phải một module trong Modular Monolith của
AlgoPrep. Nó chạy bằng Docker riêng (một container, không cần Postgres/Redis kèm — khác Judge0), nói
chuyện với backend qua REST đồng bộ: gửi request, nhận kết quả ngay trong cùng response, không có webhook
callback.

### 2.2. Cổng trung lập theo engine — vì sao và cụ thể là gì

`DEC-2026-0820-architecture-baseline` đã chốt: "Judge0 và LLM provider mỗi bên chỉ có đúng một adapter
chống ăn mòn (anti-corruption layer)". `DEC-2026-0823-go-judge-default-engine` làm rõ thêm: adapter đó
phải hiện thực một **cổng ra (outbound port) đặt tên và định hình theo ngôn ngữ nghiệp vụ**, không theo
hình dạng riêng của engine nào:

- **Tên cổng:** `JudgeExecutionPort` (tầng `domain/ports/out` theo `DEC-2026-0820-architecture-baseline`).
- **Hình dạng phương thức:** "chạy một testcase, trả về một kết quả" — không có field kiểu token/webhook
  (hình dạng riêng của Judge0), không có field kiểu `cmd[]`/`copyIn` (hình dạng riêng của go-judge) lộ ra
  ngoài cổng.
- **Adapter mặc định:** `GoJudgeAdapter` (tầng `infrastructure`), gọi go-judge đồng bộ, trả kết quả ngay.
- **Adapter có thể cắm lại sau:** `Judge0Adapter` hiện thực cùng cổng — nếu đồ án cần đúng chuẩn thi đấu
  ICPC/Olympic mà Judge0 có nhiều tiền lệ hơn (mục 1.1), đổi lại chỉ là viết thêm một adapter, domain/
  application không sửa gì, **miễn là cổng vẫn được giữ trung lập** — đây là điều kiện, không phải điều tự
  động có sẵn.

Chi tiết mô tả kỹ thuật (value object, exception, cấu trúc thư mục) ở `01-rd/system/backend_architecture.md`
mục 6.A.

### 2.3. Độ hạt gọi — 1 lần / 1 testcase

F4 gọi `JudgeExecutionPort` **một lần cho mỗi testcase**, không dồn nhiều testcase vào một request kiểu
`cmd[]` của go-judge. Hai lý do:

1. Giữ đúng trải nghiệm "kết quả từng testcase cập nhật realtime qua WebSocket" đã thiết kế — mỗi lần gọi
   xong, đẩy kết quả lên WebSocket ngay, không phải chờ hết cả submission.
2. Đây là điều khiến cổng ở mục 2.2 khớp được cả engine đồng bộ (go-judge, trả kết quả ngay trong lời gọi)
   và engine bất đồng bộ (Judge0, kết quả tới sau qua webhook) — cả hai đều tự nhiên hiện thực được "chạy
   một testcase, trả một kết quả", không ai phải giả hình dạng của bên kia.

### 2.4. Luồng dữ liệu qua go-judge (tóm tắt)

1. F3 sinh mã harness (mã người dùng được tiêm vào template, đóng gói theo đúng định dạng adapter yêu cầu —
   không hardcode Base64 như Judge0 từng cần).
2. F4 lấy từng testcase, gọi `JudgeExecutionPort` (adapter `GoJudgeAdapter`) cho **mọi** testcase, không
   dừng sớm. Fail-fast (`F4-04`) đã bị khai tử bởi `DEC-2026-0831-partial-score-testcase-ratio`: điểm
   tỷ lệ `F4-13` cần biết số testcase đạt trên tổng số, nên bài nộp phải chạy hết.
3. go-judge chạy trong sandbox `go-sandbox`, trả kết quả (`status`, `stdout`, `stderr`, thời gian, bộ nhớ)
   **ngay trong response** — không có bước webhook/token như Judge0.
4. F4 đẩy kết quả testcase đó lên WebSocket ngay, rồi gọi tiếp testcase kế.
5. Nếu worker crash giữa lúc đang gọi go-judge, message chưa được ack ở RabbitMQ sẽ được redeliver — đây là
   lưới an toàn thay cho job đối soát kiểu Judge0 (xem mục 4 của `DEC-2026-0823-go-judge-default-engine` để
   biết vì sao job đối soát được đơn giản hoá, không xoá hẳn).

### 2.5. Actor liên quan

| Actor | Tương tác với judge engine |
| :--- | :--- |
| A1 — Học viên | Không gọi trực tiếp; thấy kết quả qua UI thời gian thực |
| A3 — Quản trị viên | Giám sát cụm go-judge và hàng đợi, kích hoạt Re-judge |
| A4 — Hệ thống tự động | Điều phối bài nộp sang judge engine qua `JudgeExecutionPort`, phát hiện bài nộp bị treo (timeout sweep) |

---

## 3. Ràng buộc kỹ thuật — cgroup v1 và v2 đều dùng được

Khác với Judge0 (chỉ hiểu cgroup v1, vì fork `isolate` mà Judge0 đóng gói pin ở bản trước dòng 2.0 —
chi tiết ở lịch sử trao đổi khi so sánh engine), **go-judge hỗ trợ cả hai**:

- **cgroup v1:** cần quyền root để tạo cgroup, hoặc tự tạo trước các thư mục con
  `/sys/fs/cgroup/{cpuacct,memory,pids}/gojudge` với quyền phù hợp.
- **cgroup v2:** khi có systemd, go-judge tự liên hệ qua **systemd dbus** để tạo một transient scope làm
  cgroup root — không cần cấu hình tay.
- Nhân Linux >= 5.19 cho khả năng theo dõi bộ nhớ tốt hơn (`memory.peak`).
- go-judge còn có backend sandbox riêng cho Windows (`winc`) và macOS, nhưng **README chính thức của
  go-judge ghi rõ hai backend này còn ở trạng thái thực nghiệm, không dùng cho production**. Không ảnh
  hưởng tới AlgoPrep vì mọi triển khai ở đây đều chạy go-judge **trong container Linux** (qua Docker trên
  WSL2), dùng đúng backend `go-sandbox` cho Linux — không chạy binary go-judge trực tiếp trên Windows.

`[SoT: Suy luận — từ README chính thức `criyle/go-judge`, kiểm chứng 2026-08-23]`

**Hệ quả cho Windows/WSL2:** vì go-judge tự thích ứng theo cgroup version của máy host, **không cần bước
"ép cgroup v1" như Judge0 từng cần** (mục 5.2 của bản `judge0.md` cũ, đã xoá hoàn toàn ở bản này). Container
go-judge chạy thẳng trong WSL2 hiện có, dùng cgroup version mặc định của WSL2 (v2 trên bản mới) là đủ.

---

## 4. Phương án triển khai trên Windows

Vì go-judge không kén cgroup version, danh sách phương án ngắn hơn nhiều so với Judge0 — không còn phương
án nào bị đánh giá "rủi ro/không ổn định" do cgroup:

| # | Phương án | Độ dễ cài | Phù hợp khi |
| :--- | :--- | :--- | :--- |
| **1** | **WSL2, chạy Docker Engine trực tiếp trong WSL2** (không qua Docker Desktop) | Rất dễ — không cần cấu hình `.wslconfig` gì đặc biệt cho cgroup | Máy dev cá nhân, đã cài WSL2, muốn tiết kiệm RAM/CPU so với Docker Desktop |
| 2 | Docker Desktop for Windows (nền WSL2) | Rất dễ — không cấu hình gì thêm | Muốn UI quản lý container, không quan tâm tiết kiệm RAM |
| 3 | Máy ảo Linux đầy đủ hoặc VPS | Dễ, nhưng không còn cần thiết | Chỉ đáng làm nếu có lý do khác ngoài go-judge (ví dụ cần tách hẳn khỏi máy dev) |

### 4.1. Khuyến nghị — Phương án 1 (WSL2 + Docker Engine trong WSL2)

Chọn phương án 1 không phải vì cgroup nữa (go-judge giải quyết sẵn), mà vì lý do còn lại: nhẹ hơn Docker
Desktop, không tốn thêm RAM cho lớp quản lý GUI. Nếu bạn đã dùng Docker Desktop cho việc khác trong máy,
dùng luôn phương án 2 cũng không có nhược điểm kỹ thuật nào — khác hẳn thời còn Judge0, khi phương án 2 bị
đánh giá "không đáng tin" vì không kiểm soát được cgroup.

---

## 5. Hướng dẫn cài đặt và chạy trên Windows

### 5.1. Điều kiện tiên quyết

| Yêu cầu | Kiểm tra bằng |
| :--- | :--- |
| Windows 10 phiên bản ≥ 2004 (build ≥ 19041) hoặc Windows 11 | `winver` |
| Đã bật WSL2 (không phải WSL1) | `wsl -l -v` — cột `VERSION` phải là `2` |
| Ảo hoá đã bật trong BIOS/UEFI (Intel VT-x hoặc AMD-V) | Task Manager → tab Performance → "Virtualization: Enabled" |
| Đủ RAM cho container go-judge (nhẹ hơn Judge0 vì không có Postgres/Redis riêng) | Khuyến nghị tối thiểu 4 GB dành cho WSL2 |

**Chạy trong: PowerShell trên Windows** (mở với quyền Administrator). Nếu chưa có WSL2:

```powershell
wsl --install -d Ubuntu-22.04
```

Khởi động lại máy khi được yêu cầu, rồi mở app **Ubuntu** từ Start Menu lần đầu để tạo user Linux — từ đây,
mọi terminal Ubuntu chính là "terminal WSL2 (Ubuntu)" được nhắc lại xuyên suốt mục 5.

### 5.2. Cài Docker Engine trong WSL2 (không dùng Docker Desktop)

**Chạy trong: terminal Ubuntu (WSL2)** cho toàn bộ mục này.

```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
```

Đóng và mở lại terminal Ubuntu để nhóm `docker` có hiệu lực, rồi kiểm tra:

```bash
docker run hello-world
```

Khởi động Docker daemon (nếu WSL2 chưa bật systemd):

```bash
sudo service docker start
docker ps
```

Muốn Docker tự chạy mỗi lần mở WSL2, thêm vào `~/.bashrc`:

```bash
if ! service docker status > /dev/null 2>&1; then
    sudo service docker start > /dev/null
fi
```

### 5.3. Chạy go-judge

**Chạy trong: terminal Ubuntu (WSL2).** Image chính thức: `criyle/go-judge`
(`[SoT: Suy luận — Docker Hub/README chính thức `criyle/go-judge`, kiểm chứng 2026-08-23]`):

```bash
docker run -d --privileged --shm-size=256m -p 5050:5050 --name=go-judge --restart=unless-stopped criyle/go-judge
```

Giải thích flag: `--privileged` cần cho sandbox tạo namespace/cgroup; `--shm-size=256m` cấp bộ nhớ chia sẻ
cho các tiến trình con trong sandbox; cổng REST mặc định là `5050` (khác `2358` của Judge0).

Kiểm tra container đã lên:

```bash
docker ps --filter name=go-judge
```

Từ Windows, WSL2 forward cổng tự động sang `localhost` — gọi thử từ **PowerShell trên Windows**:

```powershell
curl.exe http://localhost:5050/version
```

Dùng `curl.exe` (không phải alias `curl` = `Invoke-WebRequest` của PowerShell) để nhận JSON thô.

### 5.4. Đóng gói compiler/runtime cho ba ngôn ngữ đã chốt

Khác với Judge0 (có sẵn bảng `language_id` với image đã cấu hình compiler), go-judge chỉ chạy lệnh
(`cmd`) — image `criyle/go-judge` mặc định **không** có sẵn JDK/g++/Python. Cần tự build một image kế thừa,
cài thêm ba runtime đã chốt cho AlgoPrep (Java 21, g++, Python 3), ví dụ hướng đi (không phải Dockerfile đã
chốt, chỉ minh hoạ hướng cấu hình — chi tiết thật để lại cho DD của F3/F4):

```dockerfile
FROM criyle/go-judge
RUN apt-get update && apt-get install -y openjdk-21-jdk-headless g++ python3
```

Build và chạy image này thay cho `criyle/go-judge` trần ở mục 5.3.

### 5.5. Gọi thử — ví dụ compile rồi run C++ trong một request

**Chạy trong: terminal Ubuntu (WSL2) hoặc PowerShell với `curl.exe`.** Ví dụ dưới lấy đúng theo tài liệu
API chính thức của go-judge (`docs.goj.ac/api`, `docs.goj.ac/example`, kiểm chứng 2026-08-23) — hai lệnh
(`cmd[]`) trong một request: lệnh đầu compile, lệnh sau chạy file vừa compile bằng `copyOutCached`/`copyIn`
với `fileId` nối hai bước lại:

```bash
curl -X POST http://localhost:5050/run \
  -H "Content-Type: application/json" \
  -d '{
    "cmd": [
      {
        "args": ["/usr/bin/g++", "a.cc", "-o", "a"],
        "env": ["PATH=/usr/bin:/bin"],
        "files": [{"content": ""}, {"name": "stdout", "max": 10240}, {"name": "stderr", "max": 10240}],
        "cpuLimit": 10000000000,
        "memoryLimit": 104857600,
        "procLimit": 50,
        "copyIn": {"a.cc": {"content": "#include <iostream>\nusing namespace std;\nint main(){int a,b;cin>>a>>b;cout<<a+b<<endl;}"}},
        "copyOutCached": ["a.cc", "a"]
      },
      {
        "args": ["a"],
        "env": ["PATH=/usr/bin:/bin"],
        "files": [{"content": "1 1"}, {"name": "stdout", "max": 10240}, {"name": "stderr", "max": 10240}],
        "cpuLimit": 10000000000,
        "memoryLimit": 104857600,
        "procLimit": 50,
        "copyIn": {"a": {"fileId": "<fileId của \"a\" trong response bước 1>"}}
      }
    ]
  }'
```

Ghi chú field: `cpuLimit` tính bằng nanosecond (`10000000000` = 10 giây), `memoryLimit` tính bằng byte
(`104857600` = 100 MiB). Response là một mảng `Result`, một phần tử cho mỗi lệnh trong `cmd[]`; lệnh compile
trả về `fileIds` (ví dụ `{"a": "5LWIZAA45JHX4Y4Z"}`) — lấy đúng `fileId` đó gắn vào `copyIn.a.fileId` của
lệnh chạy để nối hai bước.

Kết quả mong đợi cho lệnh chạy: `"status": "Accepted"`, `stdout` chứa `2`. Đây là bước kiểm chứng go-judge
đã lên và giới hạn tài nguyên (`cpuLimit`/`memoryLimit`) có hoạt động — thử một request cố tình vượt
`memoryLimit` để xác nhận sandbox **có chặn được**, không chỉ chạy được.

Vì F4 gọi theo độ hạt 1 lần/testcase (mục 2.3), luồng thật của AlgoPrep sẽ không gộp compile+run+nhiều
testcase vào một request như ví dụ trên — ví dụ này chỉ minh hoạ khả năng của API, không phải hình dạng
lời gọi thật từ `GoJudgeAdapter`.

### 5.6. Trỏ AlgoPrep sang go-judge

Trong `.env` hoặc file cấu hình của `algoprep-judge` (`GoJudgeAdapter`), đặt base URL trỏ vào cổng đã
forward:

```
JUDGE_ENGINE_BASE_URL=http://localhost:5050
```

Không cần biến kiểu `*_CALLBACK_BASE_URL` như Judge0 — go-judge gọi đồng bộ, không có webhook gọi ngược về
backend.

---

## 6. Sự cố thường gặp khi triển khai trên Windows

| Sự cố | Nguyên nhân thường gặp | Cách xử lý |
| :--- | :--- | :--- |
| Container go-judge không chạy được lệnh nào, báo lỗi liên quan cgroup/permission | Thiếu `--privileged`, hoặc daemon Docker chưa chạy | Kiểm tra lại flag ở mục 5.3, `docker logs go-judge` |
| `docker: permission denied` | User chưa vào nhóm `docker`, hoặc chưa mở lại terminal sau `usermod` | Mở terminal WSL2 mới, hoặc `newgrp docker` |
| `/run` trả lỗi thiếu compiler (`g++`/`javac`/`python3` không tìm thấy) | Đang dùng image `criyle/go-judge` trần, chưa build image kèm runtime theo mục 5.4 | Build lại image kèm JDK/g++/Python, chạy image đó thay cho image gốc |
| WSL2 chiếm nhiều RAM | Không đặt giới hạn `memory=` trong `.wslconfig` | Thêm `[wsl2] memory=4GB` vào `.wslconfig`, sau đó `wsl --shutdown` |
| Mất go-judge sau khi tắt máy | Container không tự khởi động lại | Thêm `--restart=unless-stopped` khi `docker run` (đã có ở mục 5.3), hoặc thêm `service docker start` vào `~/.bashrc` như mục 5.2 |

---

## 7. Điều tài liệu này KHÔNG chốt

| Chưa chốt | Sẽ chốt ở |
| :--- | :--- |
| Dockerfile chính thức đóng gói JDK 21 + g++ + Python 3 cho go-judge | Khi tạo `05-coding/judge-engine/` thật (`codebase_structure.md`) |
| Danh sách compiler flag/version cụ thể cho từng ngôn ngữ | Khi viết DD cho F3 (harness) |
| Cấu hình `cpuLimit`/`memoryLimit`/`procLimit` production (khác giá trị ví dụ ở mục 5.5) | Khi chốt giới hạn thời gian/bộ nhớ mặc định theo bài toán (F2) |
| Có build lại `Judge0Adapter` thật hay không | Chỉ khi có nhu cầu cụ thể cần quay lại Judge0 (ví dụ chuẩn ICPC) — xem mục 2.2 |
