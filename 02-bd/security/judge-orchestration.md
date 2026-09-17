# BD — Security module `judge-orchestration` (F4)

## 1. Xác thực nội bộ giữa `judge-orchestration` và judge engine (go-judge)

- **go-judge chạy trên mạng nội bộ (Docker Compose), không expose port ra ngoài internet** — chỉ
  `judge-orchestration` (qua adapter `GoJudgeAdapter`) gọi tới, không route công khai `/api/**` nào chạm
  trực tiếp go-judge. `[SoT: Suy luận]` — hệ quả tự nhiên của kiến trúc "self-hosted, một dịch vụ ngoài
  monolith nhưng cùng mạng Docker nội bộ" (`.nexa/control/dependency-map.md` mục 2-3), RD không nói cụ thể
  network topology nhưng đây là thực hành bảo mật tối thiểu cho một sandbox thực thi mã tuỳ ý.
- **Xác thực giữa hai tiến trình**: go-judge tự thân không yêu cầu auth token bắt buộc theo mặc định của
  dự án upstream `criyle/go-judge` — BD đề xuất bật cấu hình `--auth-token` của go-judge (nếu bản triển
  khai hỗ trợ) hoặc giới hạn hoàn toàn bằng network policy (chỉ container `algoprep-judge` được phép nối
  tới port go-judge trong cùng Docker network) `[SoT: Suy luận]`, chốt cấu hình cụ thể ở DD/hạ tầng triển
  khai (không phải trách nhiệm của BD tầng ứng dụng).
- **Adapter thay thế `Judge0Adapter`** (nếu kích hoạt lại) giữ nguyên constraint cgroup v1 cũ
  (`README.md` mục 7, đã đóng với go-judge — `DEC-2026-0823-go-judge-default-engine`) — không phải mối
  quan tâm của go-judge mặc định.

## 2. Cách ly tài nguyên sandbox — một bài nộp độc hại không ảnh hưởng bài nộp khác

Đây là ràng buộc bảo mật cốt lõi của module chấm bài — thực thi mã người dùng tuỳ ý luôn là bề mặt tấn
công cao nhất hệ thống.

- **cgroup** (v1 hoặc v2, go-sandbox hỗ trợ cả hai — `DEC-2026-0823-go-judge-default-engine` điểm (1)):
  giới hạn CPU, bộ nhớ, số tiến trình con (`language_configs.max_child_processes`, F4-11 tham số 2/3) cho
  **mỗi lần gọi `JudgeExecutionPort.run()`**, không chia sẻ giữa các testcase hay giữa các submission khác
  nhau — mỗi lần gọi là một sandbox instance độc lập, huỷ ngay sau khi trả kết quả.
- **Mạng**: chặn mặc định (`language_configs.network_access_enabled = false`, F4-11 tham số 1/3) — mã
  người học không gọi được ra ngoài internet hay tới dịch vụ nội bộ khác (không đọc trộm Postgres/Redis
  của chính hệ thống, không dùng làm bàn đạp tấn công mạng nội bộ). Bật mạng chỉ là một tham số cấu hình
  A3 chỉnh được (F4-11), không có bài toán chuẩn nào trong phạm vi capstone cần mạng.
- **Giới hạn output**: `problems.max_output_size_kb` (`02-bd/database/problem-bank.md` mục 1.1) chặn một
  chương trình in vô hạn (ví dụ vòng lặp `print` không dừng) làm tràn bộ nhớ/đĩa của chính go-judge hoặc
  của `judge-orchestration` khi đọc `stdout` trả về.
- **Timeout cứng ở tầng sandbox**: thời gian giới hạn nhân hệ số (`problems.time_limit_ms *
  language_configs.time_limit_multiplier`) đưa xuống go-judge như tham số kill-process, không dựa vào
  timeout tầng ứng dụng (Java) canh giờ rồi mới huỷ tiến trình — sandbox tự huỷ đúng lúc là chốt chặn thật
  sự, tầng ứng dụng chỉ là nơi khai báo con số.

## 3. Webhook auth — chỉ liên quan khi `Judge0Adapter` được bật lại

Thiết kế **trung lập ở tầng port** — `JudgeExecutionPort` không mang tham số webhook (chi tiết ở
`02-bd/architecture/judge-orchestration.md` mục 5.4). Khi `Judge0Adapter` hoạt động:

- Mỗi lần gọi phát hành một `judge_run_tokens.token` (UUID) riêng cho từng `(submission_id, testcase_id)`
  — đây chính là bí mật webhook, không phải một secret cố định dùng chung cho cả hệ thống (tránh một token
  bị lộ ảnh hưởng mọi submission).
- Endpoint callback (`Judge0Adapter`-owned, ví dụ `/internal/adapters/judge0/callback`) **không nằm dưới**
  `/api/**` công khai, xác thực bằng chính token trong payload callback đối chiếu `judge_run_tokens` —
  token không khớp hoặc đã `consumed_at IS NOT NULL` → ghi `callback_logs.outcome = INVALID_TOKEN` hoặc
  `STATE_ALREADY_FINAL`, không cập nhật `submissions.status` (bất biến không ghi đè trạng thái cuối).
- Endpoint callback này **không cần** RBAC theo Role/Function của `identity` (không phải người dùng cuối
  gọi) — xác thực bằng token bí mật per-request, khác cơ chế JWT dùng cho các API còn lại của hệ thống.

## 4. Chống rò rỉ thông tin qua kết quả chấm (kế thừa ràng buộc F2-08 của `problem-bank`)

- **Không trả input/expected-output của Hidden testcase** cho học viên qua bất kỳ API nào của
  `judge-orchestration` — chỉ trả `status`, `verdict`, `testcase_order` (chỉ số, không phải nội dung) của
  testcase thất bại đầu tiên, khớp nguyên tắc đã chốt ở `02-bd/security/problem-bank.md` mục 2.
- **`stderr_snippet`** (nếu `language_configs.return_stderr_to_student = true`) chỉ chứa output lỗi từ
  chính chương trình người học (compiler/runtime báo lỗi) — không bao giờ chứa nội dung testcase hay mã
  khung harness (đã lọc ở tầng `harness`, F3-12), `judge-orchestration` chỉ lưu/chuyển tiếp nguyên văn,
  không tự parse thêm.
- **`compile_error_message`** đã qua bước ánh xạ dòng của `harness` (F3-11) trước khi tới đây — nếu lỗi
  thuộc mã khung harness, `harness` đã thay bằng thông báo chung chung (`02-bd/architecture/harness.md`
  mục 7 bước 3); `judge-orchestration` không tự sửa lại nội dung này, chỉ lưu và trả nguyên văn.

## 5. Phân quyền vận hành (F4-10, F4-11) — actor A3

- **Xem trạng thái hàng đợi/cụm** và **điều khiển vận hành cơ bản** (pause/resume tiêu thụ hàng đợi,
  bật/tắt autoscale worker, F4-10, `DEC-2026-0831-judge-orchestration-ops-details`): gác bởi Function
  `JUDGE_MONITORING` (tên đề xuất, khớp danh sách Function của `identity`,
  `02-bd/database/identity.md` mục 1.3) — chỉ `ADMIN`.
- **Sửa `language_configs`** (F4-11): gác bởi Function `LANGUAGE_CONFIG` (tên đề xuất) — chỉ `ADMIN`, thay
  đổi có hiệu lực ngay không cần khởi động lại dịch vụ (đọc trực tiếp từ DB mỗi lần build `RunRequest`,
  không cache tĩnh dài hạn — nếu có cache, phải invalidate chủ động khi `language_configs` đổi, cùng
  nguyên tắc đã áp cho ma trận phân quyền của `identity`, `02-bd/architecture/identity.md` mục 6).
- **Không có Function `REJUDGE_MANAGEMENT`** — đã loại bỏ khỏi phạm vi
  (`DEC-2026-0828-remove-rejudge-scope`), không thiết kế lại quyền cho tính năng này.

## 6. Việc còn mở — chuyển sang DD

- Cấu hình xác thực thực tế giữa `judge-orchestration` và go-judge (`--auth-token` hay network policy
  thuần) — mục 1, cần xác nhận với chủ dự án khi vào giai đoạn hạ tầng/triển khai.
- Tên Function chính xác `JUDGE_MONITORING`/`LANGUAGE_CONFIG` — đối chiếu lại danh sách 10 Function seed
  đầy đủ ở `02-bd/database/identity.md` mục 1.3 khi viết DD, tránh tạo trùng tên khác đã có.
- Endpoint callback thật của `Judge0Adapter` (đường dẫn, method, shape payload) — chỉ cần thiết kế nếu
  adapter này được kích hoạt lại, không phải việc của lần BD này.

## 7. Tham chiếu

- `02-bd/architecture/judge-orchestration.md` mục 5 — callback contract đầy đủ (`DEC-2026-0912-judge-
  callback-contract`).
- `02-bd/security/problem-bank.md` mục 2 — nguyên tắc chống rò rỉ Hidden testcase mà file này kế thừa.
- `02-bd/security/identity.md` mục 2 — nguyên tắc kiểm quyền tầng application, cache + invalidation quyền.
- `.nexa/control/decision-registry.md` — `DEC-2026-0823-go-judge-default-engine`,
  `DEC-2026-0831-judge-orchestration-ops-details`, `DEC-2026-0828-remove-rejudge-scope`.
