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

## Matrix A-6/A-7: đường giao diện và baseline cùng tải (2026-11-12)

Bộ mới không sửa `submit-load.js` hoặc kết quả cũ. `submit-ui-load.js` có hai chế độ dùng **cùng fixture, cùng 16 testcase, Java/C++/Python luân phiên và cùng thời gian nghỉ tối thiểu3,1giây**:

- `ui`: `/ws`, STOMP CONNECT có bearer, SUBSCRIBE `/user/queue/submissions`; tin nhắn chỉ là hint. REST `/progress` lấy snapshot ngay lúc bắt đầu/kết nối lại/hint, khi final mới GET chi tiết. Khi không có hint trong3giây thì hỏi dự phòng, tối đa10lần mỗi lần kết nối như hook FE; vẫn chờ STOMP tới deadline120giây. Đóng kết nối/lỗi thì thử lại sau3giây. Không tin kết luận chỉ từ WS.
- `poll`: GET chi tiết mỗi0,5giây như kịch bản cũ, nhưng dùng workload/ngôn ngữ/thời gian nghỉ **giống chế độ ui mới**. Không đem trực tiếp con số này so với lượt cũ chỉ Python8testcase và nghỉ1–2giây.

Đây là client tải giao thức k6, không phải browser k6. Nó không đo render hoặc băng thông Monaco. Kết nối WebSocket dùng API `k6/ws`; deadline được đóng bằng socket timer, thời gian bắt tay phụ thuộc thư viện/runtime. Ghi số `stomp_connected`, hints, reconnect/error và RESTreads để biết kịch bản có thật sự dùng realtime hay đã suy giảm.

Stack phải riêng: backend `8081`, DB **algoprep_load**, Rabbit vhost riêng và Redis DB riêng; ví dụ vhost `followup_load`/Redis11. Cùng hạ tầng vật lý vẫn tranh CPU với backend/engine, phải ghi cấu hình và không chạy đồng thời với test/lượt tải khác. Chỉ root/operator đã xác minh stack mới chạy bước dưới; script từ chối DB khác, nhưng không tự suy ra DB mà backend đang nối chỉ từ URL.

```powershell
# Chạy từ 05-coding/k6; đây là các lệnh có tác dụng khi operator chủ động thực thi.
$env:CONFIRM_LOAD_STACK = 'yes'
$env:ALGOPREP_DB_NAME = 'algoprep_load'
$env:BASE_URL = 'http://localhost:8081/api/v1'
node ./seed-load-matrix.mjs F:/prj_algoprep/06-plan/reports/261112-f1-f4-followup-fix/load-fixture.json

powershell -File ./run-load-matrix.ps1 `
  -Fixture F:/prj_algoprep/06-plan/reports/261112-f1-f4-followup-fix/load-fixture.json `
  -OutDir F:/prj_algoprep/06-plan/reports/261112-f1-f4-followup-fix/load-matrix `
  -BaseUrl http://localhost:8081/api/v1 -IsolatedStackConfirmed `
  -BackendPid <pid-backend-load> `
  -BackendJar F:/prj_algoprep/05-coding/backend/algoprep-bootstrap/target/algoprep-bootstrap-0.1.0-SNAPSHOT.jar `
  -ConfigurationNote '8081; DB algoprep_load; Rabbit followup_load; Redis11; pool/worker/engine config from runbook'
```

Seeder tạo một tác giả tổng hợp bằng đăng ký/OTP thật, dựng và publish bài echo16testcase qua contract hiện có. Sau đó tạo960 tài khoản học viên tổng hợp bằng SQL chỉ trong DB tải, dùng cùng BCrypt của tác giả **bên trong PostgreSQL**, không xuất hash/token. Mỗi tổ hợp mode/repeat/vus dùng một tập tài khoản riêng; hạn mức60lần/giờ/bài vẫn giữ nguyên. File fixture có mật khẩu **cố định của tài khoản tổng hợp**, không dùng tài khoản thật và không ghi bearer vào log/manifest. Không chạy lại cùng fixture sau khi đã dùng đủ lượt: seed file mới cho ma trận mới.

Runner mặc định chạy12hàng: `40/80/120 VU × 180giây × 2lượt × 2mode`. Lượt1 theo poll→ui, lượt2 đảo ui→poll để lộ độ nhiễu theo thời gian. Sau mỗi hàng, chờ toàn bộ PENDING/COMPILING/JUDGING về0 rồi nghỉ15giây; timeout180giây thì **dừng, ghi timeout, không purge**. Thời gian tổng gồm36phút giữ tải, cộng chờ tail/drain/cooldown và chuẩn bị; không có hai hàng chồng nhau.

Mỗi thư mục hàng có `k6.log`, `summary.json`, `metrics.csv`, `queue-before.csv`, `queue-after.csv`; có `cpu.csv`/samplerlog nếu truyền BackendPid. `matrix-manifest.json` ghi cấu hình, phiên bản k6, SHA256jar, giờ bắt đầu/kết thúc và exitcode từng hàng. Không chấp nhận ghi đè manifest cũ. Exit99 (threshold không đạt) vẫn lưu và tiếp tục hàng sau **chỉ khi queue đã idle**; lỗi script/hạ tầng khác dừng ma trận. Kết quả cuối exit1 nếu có hàng threshold đỏ, không đổi nhãn thành đạt.

Chỉ `k6 version`, `k6 inspect submit-ui-load.js`, `node --check seed-load-matrix.mjs` và parser PowerShell là kiểm cú pháp, **không phải bằng chứng A-6/A-7 đo tải đã đạt**. Báo cáo cuối cần bảng mỗi mode/level gồm2lượt và min–max: admissionstatus503/429, số verdict, AC, p95/p99, RESTreads/submission, STOMPhints/connect/errors, CPU và queue drain. Không dùng tổng `http_reqs/s` làm throughput bài chấm xong.

Sau khi có raw output, chạy `node ./analyze-load-matrix.mjs <matrix-output-directory>` để sinh `analysis.json` và `analysis.md`: từng hàng/exit/drain, hai lượt min–max theo mode/VUs, admission503/429, verdict/s, p95/p99, RESTreads/admission, WS và CPU. Chỉ đọc manifest/summary/CPU đã có; không chạy tải, không sửa raw evidence. Matrix chưa hoàn tất hoặc thiếu summary được ghi rõ, không tự coi là đạt.
