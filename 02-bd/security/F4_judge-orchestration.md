# BD — Security module `judge-orchestration` (F4)

**Triển khai 2026-11-03, D02m — worker persistence:** năm cổng nội bộ cho
COMPILING/JUDGING, token, kết quả testcase và finalization chung worker/sweep tương lai.
REQUIRES_NEW/READ_COMMITTED, heartbeat guarded trước INSERT/consume; giữ pinned version,
duplicate/terminal-safe, privacy snippet; finalizer row lock, đủ N/first-failure/HALF_UP3,
Graded/Accepted Outbox nguyên tử và STOMP sau commit. Exception cố định không SQL/cause.
Focused verify 338 test đạt, ba mutation mới; Checkstyle0/SpotBugs0. Bootstrap coverage
chỉ suite chọn. Chưa consumer/ack, job lease, đọc cấu hình/snapshot, compile/run orchestration,
relay/sweep; không đóng D02 tổng/D03/D04/B2/B4/B3. Report261103-f4-worker-persistence.
Owner chốt khóa phiên riêng/job, giao dịch ghi ngắn; sweep khóa giao dịch
(DEC-2026-1103-f4-worker-job-lease); phần lease sẽ triển khai ở lát sau.


**Triển khai 2026-11-03, D02l — API submit #3:** POST /api/v1/judge/problems/{problemId}/submissions
đã nối CreateSubmissionUseCase→admission commit→Rabbit confirm. STUDENT/JWT owner,
typed JSON/UTF-8/file/quota, ticket201; queue failure thắng thì guarded SYSTEM_ERROR+
Outbox đã commit, STOMP owner-only seq3/nullscore và503 JDG-503 có ID. Worker thắng
thì201 ticket theo trạng thái đã commit, không ghi đè/push lỗi giả. Owner chốt503 JDG-505 cho lỗi database/F2/
finalizer; không tự nộp lại khi kết quả ghi chưa rõ (DEC-2026-1103-f4-admission-unavailable).
Focused verify 292 test đạt, hai mutation mới; Checkstyle0/SpotBugs0. Không phải full
backend regression; coverage bootstrap chỉ từ suite chọn. Chưa worker/relay/progress REST/
history/FE; không đóng D02 tổng/D03/D04/B2/B4/B3. Report261103-f4-submit-http.


**Triển khai 2026-11-03, D02k — STOMP progress transport:** GET /ws, CONNECT1.2
JWT RS256/exp và account live; STUDENT chỉ exact /user/queue/submissions; admin exact
/topic/admin/queue cần ADMIN + JUDGE_QUEUE_MONITOR:READ kiểm lại tại SUBSCRIBE và đóng
socket tại exp. Mọi SEND/đích khác trả ERROR generic rồi đóng. Typed payload không có
source/input/output/stderr; seq tất định, lỗi CE/SYSTEM_ERROR có score/count null.
SubmissionProgressPort có adapter thật, afterCommit/rollback-safe/best-effort WARN.
Broker giữ thứ tự gửi; bearer header được bỏ trước validation/logging. Chỉ transport:
caller trusted phải lấy owner từ submissions; chưa nối HTTP submit/worker, progress REST,
FE recovery hoặc QUEUE_BATCH5s. Report `261103-f4-stomp-progress`.

**D02j 2026-11-03:** submission message chỉUUID, không có source/testcase/owner;
Outbox dùng whitelist10 trường payload, null score cho SYSTEM_ERROR, không failureReason.
Queue failure UPDATE bắt buộc PENDING, chống ghi đè COMPILING/JUDGING/terminal; transaction
bao gồm Outbox, bắt lỗi kể cả commit với message cố định không cause/SQL. Dedicated Rabbit
connection/channel mỗi attempt, không thay RabbitTemplate của F1; chưa mở route hoặc STOMP.
Report `261103-1126-f4-queue-dispatch-core`.


**D02e review fix:** ngân sách debug wrapper độc lập4096 raw UTF-8 byte được owner chốt,
không chỉ dựa total collector. Engine OK vượt debug budget → RE trước matching và trước
lọc Sample/Hidden; không trả debug vượt trần. P4 console normalization không được làm giảm
byte budget. Source-copy mutation kiểm riêng bỏ debug guard, bỏ Hidden actual/debug guard,
bỏ wrapper stderr model guard; delivery source không bị mutation.

**Checkpoint component D02e 2026-11-01:** adapter có HTTP timeout, collector/envelope response
hữu hạn và exception thông điệp cố định; không log source/input/runtime stderr. Classifier
xét engine TLE/MLE/internal trước marker, cần model+status+exact marker cho 90/91/92. Component
lọc Hidden actual/debug/stderr, wrapper runtime stderr luôn null; Sample Standard I/O chỉ có
stderr khi cờ bật và verdict khác AC. Đây là lớp ghi/component, chưa thay thế lớp đọc có kiểm
visibility hiện hành/retention. Network isolation do sandbox image/container giữ; lát này
không triển khai admin network toggle hoặc rollout dịch vụ.

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
  giới hạn CPU, bộ nhớ, số tiến trình con (công tắc `sandbox_settings.child_process_limit_enabled`, F4-11 tham số 2/3; trần là **hằng số 60 ở `application.yml`**, không còn cột `max_child_processes` — sửa 2026-10-22, đồng bộ DB phase 3, `DEC-2026-1014-sandbox-child-process-cap-constant`; `02-bd/database/F4_judge-orchestration.md:187`, `02-bd/screens/admin/ADM0501_language_config.md:59,265`) cho
  **mỗi lần gọi `JudgeExecutionPort.run()`**, không chia sẻ giữa các testcase hay giữa các submission khác
  nhau — mỗi lần gọi là một sandbox instance độc lập, huỷ ngay sau khi trả kết quả.
- **Mạng**: chặn mặc định (`sandbox_settings.network_access_enabled = false`, F4-11 tham số 1/3) — mã
  người học không gọi được ra ngoài internet hay tới dịch vụ nội bộ khác (không đọc trộm Postgres/Redis
  của chính hệ thống, không dùng làm bàn đạp tấn công mạng nội bộ). Bật mạng chỉ là một tham số cấu hình
  A3 chỉnh được (F4-11), không có bài toán chuẩn nào trong phạm vi capstone cần mạng.
- **Giới hạn output**: `problems.max_output_size_kb` (`02-bd/database/F2_problem-bank.md` mục 1.1) chặn một
  chương trình in vô hạn (ví dụ vòng lặp `print` không dừng) làm tràn bộ nhớ/đĩa của chính go-judge hoặc
  của `judge-orchestration` khi đọc `stdout` trả về.
- **Timeout cứng ở tầng sandbox**: thời gian giới hạn nhân hệ số (`problems.time_limit_ms *
  language_configs.time_limit_multiplier`) đưa xuống go-judge như tham số kill-process, không dựa vào
  timeout tầng ứng dụng (Java) canh giờ rồi mới huỷ tiến trình — sandbox tự huỷ đúng lúc là chốt chặn thật
  sự, tầng ứng dụng chỉ là nơi khai báo con số.

## 3. Webhook auth — chỉ liên quan khi `Judge0Adapter` được bật lại

Thiết kế **trung lập ở tầng port** — `JudgeExecutionPort` không mang tham số webhook (chi tiết ở
`02-bd/architecture/F4_judge-orchestration.md` mục 5.4). Khi `Judge0Adapter` hoạt động:

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
  testcase thất bại đầu tiên, khớp nguyên tắc đã chốt ở `02-bd/security/F2_problem-bank.md` mục 2.
- **`stderr_snippet`** có thể chứa input testcase hoặc stack trace mã khung; không được coi là dữ liệu đã lọc.
  **Cập nhật owner 2026-10-25:** chỉ lưu/trả stderr runtime cho `STANDARD_IO` trên testcase Sample khi cờ admin bật
  và verdict khác AC; `FUNCTION_WRAPPER` và Hidden luôn NULL. Lúc đọc kết quả cũ vẫn kiểm `is_sample` lúc chấm,
  visibility hiện hành và mô hình bài nộp. `compile_error_message` đi theo đường ánh xạ riêng dưới đây.
- **`compile_error_message`** đã qua bước ánh xạ dòng của `harness` (F3-11) trước khi tới đây — nếu lỗi
  thuộc mã khung harness, `harness` đã thay bằng thông báo chung chung (`02-bd/architecture/F3_harness.md`
  mục 7 bước 3); `judge-orchestration` không tự sửa lại nội dung này, chỉ lưu và trả nguyên văn.

## 5. Phân quyền vận hành (F4-10, F4-11) — actor A3

- **Xem trạng thái hàng đợi/cụm** (`ADM0401`, và sự kiện hạ tầng `ADM0404`): gác bởi Function
  `JUDGE_QUEUE_MONITOR` (tên đã có trong danh sách Function của `identity`, `02-bd/database/F1_identity.md`
  mục 1.3). **Ghi** (pause/resume tiêu thụ hàng đợi, bật/tắt autoscale worker, đổi bốn tham số hàng đợi,
  F4-10, `DEC-2026-0831-judge-orchestration-ops-details`, `DEC-2026-1010-admin-operations-regroup`): máy
  chủ kiểm lại vai trò `ADMIN` ở mỗi lệnh. Mọi lệnh ghi vận hành (kể cả ở `ADM0501`) được ghi vào Nhật ký
  hệ thống phân loại "Cấu hình" kèm giá trị cũ và mới (G2). Sửa 2026-10-23 (`DEC-2026-1023-db-actor-review-fixes`, `DEC-2026-1023-db-actor-review-decisions`): `judge-orchestration` ghi qua cổng vào `AuditLogPort` của `identity` trong cùng giao dịch với thay đổi, không ghi thẳng schema `identity`; các thao tác ghi được kiểm toán gồm `language_configs`, `grading_defaults`, `sandbox_settings`, `generator_sandbox_settings` và `cluster_settings`. Tên `JUDGE_MONITORING` ở các bản trước là
  tên đề xuất chưa khớp `identity`, đã thay.
- **Sửa `language_configs`, `grading_defaults`, `sandbox_settings`** (F4-11): gác bởi Function `LANGUAGE_CONFIG` (function thứ 11 trong seed, chốt 2026-10-23, `DEC-2026-1023-db-actor-review-fixes`; áp cho cả `generator_sandbox_settings`) — chỉ `ADMIN`, thay
  đổi có hiệu lực ngay không cần khởi động lại dịch vụ (đọc trực tiếp từ DB mỗi lần build `RunRequest`,
  không cache tĩnh dài hạn — nếu có cache, phải invalidate chủ động khi `language_configs` đổi, cùng
  nguyên tắc đã áp cho ma trận phân quyền của `identity`, `02-bd/architecture/F1_identity.md` mục 6).
- **Không có Function `REJUDGE_MANAGEMENT`** — đã loại bỏ khỏi phạm vi
  (`DEC-2026-0828-remove-rejudge-scope`), không thiết kế lại quyền cho tính năng này.

## 6. Việc còn mở — chuyển sang DD

- Cấu hình xác thực thực tế giữa `judge-orchestration` và go-judge (`--auth-token` hay network policy
  thuần) — mục 1, cần xác nhận với chủ dự án khi vào giai đoạn hạ tầng/triển khai.
- ~~Tên Function chính xác `JUDGE_QUEUE_MONITOR`/`LANGUAGE_CONFIG`~~ — **đã đóng 2026-10-23** (`DEC-2026-1023-db-actor-review-fixes`): cả hai nằm trong 11 Function seed ở `02-bd/database/F1_identity.md` mục 1.3.
- Endpoint callback thật của `Judge0Adapter` (đường dẫn, method, shape payload) — chỉ cần thiết kế nếu
  adapter này được kích hoạt lại, không phải việc của lần BD này.

## 7. Tham chiếu

- `02-bd/architecture/F4_judge-orchestration.md` mục 5 — callback contract đầy đủ (`DEC-2026-0912-judge-
  callback-contract`).
- `02-bd/security/F2_problem-bank.md` mục 2 — nguyên tắc chống rò rỉ Hidden testcase mà file này kế thừa.
- `02-bd/security/F1_identity.md` mục 2 — nguyên tắc kiểm quyền tầng application, cache + invalidation quyền.
- `.nexa/control/decision-registry.md` — `DEC-2026-0823-go-judge-default-engine`,
  `DEC-2026-0831-judge-orchestration-ops-details`, `DEC-2026-0828-remove-rejudge-scope`.


## Ghi nhận triển khai D02n — 2026-11-03

Owner chốt `DEC-2026-1103-f4-worker-network-disabled`: sandbox khởi động không có `-net-share`; nếu cấu hình job yêu cầu mạng thì kết thúc SYSTEM_ERROR/SANDBOX_FAULT trước khi chạy mã. Chế độ cho phép mạng còn nợ hạ tầng. Message chỉ có UUID submissionId; JSON thừa trường/trùng trường/trailing token bị reject. Ack sau terminal commit; lỗi persistence thì requeue. Lease và lỗi/log không chứa source/expected output/token; privacy Sample/Hidden do persistence kiểm lại. Runtime mặc định một backend/STOMP broker nội bộ; đường phát realtime giữa nhiều tiến trình còn mở.


## D02n1 — Đóng nợ worker local (2026-11-03)

Compose sandbox tùy chọn chỉ bind loopback và không net-share; không thay service go-judge hiện hữu. Metric chỉ có số consumer, không source/testcase/token. Test terminate leader session thật xác minh dừng nhận và chặn start cùng instance. Network-enabled vẫn fail theo quyết định owner.


## Triển khai D02q — 2026-11-04: API đọc kết quả/progress/lịch sử

**D02q — Task 7: API kết quả/progress/lịch sử F4, 2026-11-04:** 7 API học viên/giảng viên và cổng F1/F2 thật; 1,976 test focused đạt, 12/12 mutation bị bắt; Checkstyle/SpotBugs 0, coverage đạt. Khóa kép Sample, stderr, retention 90 ngày, Beats, phạm vi lớp và snapshot/seq worker đã kiểm. Owner chốt 503 JDG-506. FE/browser và thống kê B2/B3 còn mở; tiếp theo task 8 Run Code/Sample và execution limits. Chưa stage/commit/push. [Report](F:/prj_algoprep/06-plan/reports/261104-f4-submission-reads/report.md).

Bảy endpoint #4/#5/#6/#7/#10/#11/#12 đã nối PostgreSQL và cổng F1/F2 thật. Owner/JDG-110,
CLASS_MANAGEMENT:READ và khoảng ghi danh/giao bài được kiểm ở backend; metadata-only list,
chi tiết Sample khóa kép/stderr, retention 90 ngày và Beats đúng tập. Query nhiều bước
REPEATABLE_READ; metadata F2 tùy chọn REQUIRES_NEW để lỗi SQL không làm hỏng kết quả đã lưu.
REST progress có seq khớp worker, kể cả SYSTEM_ERROR giữa chừng; chưa nối client/browser.
Owner chốt JDG-506 theo DEC-2026-1104-f4-submission-read-unavailable.

[SoT: Suy luận — chi tiết SQL]: JSONB recordset truyền các cửa sổ đã đọc qua port thay
unnest mảng trong ví dụ DD; giữ nguyên predicate, chọn lớp trước lọc/phân trang. Không đổi
schema, quyền hay quy tắc nghiệp vụ. Không đóng AC màn hình, B2/B3, API admin hoặc toàn F3/F4.


## Triển khai D02r — 2026-11-04: Run Code/Sample và execution limits

**D02r — Task 8: Run Code/Sample và execution limits, 2026-11-04:** hai API backend dùng F2/F3/sandbox thật; 14 unit mới + 18 integration mới đạt, cùng 29 test hợp đồng trực tiếp/kiến trúc (61 test liên quan); 8/8 mutation bị bắt. Spotless/Checkstyle/SpotBugs đạt; LINE của từng file mới được đo ≥80%. Chỉ test lát và phụ thuộc trực tiếp theo owner; full regression chờ mốc clear. FE/browser, network-enabled và production nhiều instance còn hoãn. [Report](F:/prj_algoprep/06-plan/reports/261104-f4-run-sample/report.md).

API #1/#2 dùng dữ liệu F2/F3/cấu hình/sandbox thật; STUDENT/JWT, chỉ Sample approved từ SQL,
không ghi submission/progress/outbox. Redis Lua 60/60 phút/user, fail-open chỉ phần quota;
semaphore JVM 8 theo quyết định single-instance. JDG-107 có retryAfterSeconds, JDG-502 busy
hoàn mốc, JDG-501 hoàn mốc hạ tầng, compile error tính lượt. Deadline 60 giây và cancellation
giữ slot đến khi task thực sự dừng. Output/stderr qua kernel privacy hiện có, HALF_UP giới hạn.
[SoT: Suy luận — chi tiết lifecycle]: Future/virtual thread và callback hoàn quota một lần,
trạng thái hủy tách interrupt để xử lý race; không đổi hợp đồng/schema/auth. GET limits giữ
hợp đồng lỗi phụ thuộc hiện có của §4.5, không mở rộng mã JDG-506 từ D02q. Source/gates/log/hash
có trong report. Chỉ test phần mới và hợp đồng trực tiếp theo owner; coverage BUNDLE/full
regression, FE/browser và nghiệm thu module tổng chưa thực hiện ở lát này.


**D02v — 2026-11-04:** F4 B2 cổng thống kê lớp đã xong: đúng cửa sổ ghi danh/giao/gỡ, tuần đủ UTC+7, best theo lần giao, cache15 phút/fallback. 24 test liên quan đạt (16 mới), 4/4 mutation bị bắt; 124/124 dòng logic mới, static gates đạt. Không sửa F1/F2 hoặc chạy full regression. B3 đã chốt 7 khối, chưa triển khai. [Report](F:/prj_algoprep/06-plan/reports/261104-1540-f4-class-metrics/report.md).


**D02w — 2026-11-04:** B3 backend F4 có đủ7 cổng thống kê admin theo owner; SQL đúng UTC+7/cửa sổ/trạng thái/scope bài. 23 test liên quan đạt (15 mới), 4/4 mutation, 191/191 dòng logic mới và static gates đạt. Chưa nối F1 HTTP/cache/catalogue hoặc FE; không chạy full regression/merge. [Report](F:/prj_algoprep/06-plan/reports/261104-1604-f4-admin-stats/report.md).


**D02x — 2026-11-04:** F4 GET metrics cá nhân và P5 topic đã xong; SQL/principal thật, range/streak/ratio/đọc topic lịch sử, không callback F2 từ P5. Owner chốt503 JDG-507. 29 test liên quan duy nhất đạt (21 mới),5/5 mutation, static gates đạt; coverage7 file logic không đổi115/115 dòng từ unit đầu. Không full regression/merge. [Report](F:/prj_algoprep/06-plan/reports/261104-1653-f4-my-metrics/report.md).


**D02y — 2026-11-05:** F4 P6/P7 backend xong: current-members/class attribution trước tab, cửa sổ giao/ghi danh; P7 principal STUDENT và trạng thái từng lần giao, không callback F2. 28 test liên quan đạt (16 mới),7/7 mutation,80/80 dòng logic mới; static gates đạt. Consumer F2/FE chưa nối, chưa full regression/merge. [Report](F:/prj_algoprep/06-plan/reports/261105-1020-f4-assignment-metrics/report.md).


P6/P7 là cổng SELECT nội bộ mới, không HTTP/quyền/schema mới. P6 kiểm ownership F1 và chỉ học viên còn ghi danh, rank mọi lớp trước tab; P7 lấy principal STUDENT, F2 kiểm membership đầu vào. Không callback F2, không source/testcase, không ghi dữ liệu; lỗi dependency cố định/no cause, caller F2 map theo DD. Các bound và kiểu cụ thể ghi tại DD API §26.6/26.7 và report D02y.


**D02z — 2026-11-05:** F4 P3 provider đủ generator limits/enabled languages/multipliers và defaults có sẵn; P4 snapshot7 trường chỉAC, lỗi missing/notAccepted riêng, no testcase/source trong log. 22 test liên quan đạt (14 mới),5/5 mutation,37/37 dòng logic mới; static gates đạt, Spotless judge/bootstrap apply+check đầy đủ. Consumer mới F2/F5/FE chưa nối; chưa full regression/merge. [Report](F:/prj_algoprep/06-plan/reports/261105-1045-f4-config-snapshot-reads/report.md).

P3 mới đọc DB mỗi lần, không fallback/cache ở F4; unknown language lỗi cổng. P4 chỉAccepted, snapshot7 trường và source redacted trong toString; không owner guard theo DD, F5 phải kiểm userId và map lỗi. Không testcase/quyền/schema/HTTP mới. Chi tiết chữ ký và test tại report D02z và DD API §26.3/26.4.


**D02aa — 2026-11-05:** API F4#8/#23/#24 xong: latest-own/withdrawn trước limit, Accepted history và enabled languages đúng thứ tự/quyền.30 test liên quan đạt (18 mới),6/6 mutation,83/83 dòng logic mới; static gates/Spotless judge+bootstrap đạt. F4-2 backend đóng; còn admin config/queue và nghiệm thu BE cuối, FE riêng. Chưa full regression/merge. [Report](F:/prj_algoprep/06-plan/reports/261105-1110-f4-learner-list-apis/report.md).

#8/#23 không nhận userId trong chữ ký usecase; STUDENT current principal và SQL own-only. #8 latest trước lọc status, metadata withdrawn trước limit, keyset32; F2 lỗi optional thìproblem=null. #23 giữAC lịch sử, metadata4 trường, khôngsource/testcase. #24 roleSTUDENT/INSTRUCTOR/P3 currentDB, thứ tựPYTHON/JAVA/CPP, khôngadminconfig. 400JDG-V14 cho limit1..20;503chung khôngthêmmã, khôngmởrộng506/507. Không write/audit/cache, không sửa schema. SoT chi tiết và giới hạn admin#21 tại report D02aa.


**D02ab — 2026-11-05:** API #21/#22 cấu hình ngôn ngữ/limits/sandbox/generator đã có backend thật; quyền riêng READ/UPDATE, khóa/version từng dòng, một audit diff và rollback hai chiều. 29 test liên quan đạt (21 mới), 6/6 mutation, 96/97 dòng logic mới; Spotless judge/bootstrap, Checkstyle phạm vi F4/bootstrap và SpotBugs đạt. F4-3 còn queue/infra/STOMP admin; chưa full regression/merge, FE riêng. [Report](F:/prj_algoprep/06-plan/reports/261105-1204-f4-admin-language-config/report.md).


**D02ac — 2026-11-05:** API #16–19 pause/resume, autoscale và queue settings có backend thật; resume tái dùng kernel heartbeat D02p, quyền ADMIN riêng với Function READ cho GET, stale409, no-op và audit REQUIRED. 34 test liên quan đạt (22 mới),9/9 mutation,73/73 dòng logic mới; Spotless judge/bootstrap, Checkstyle đúng phạm vi và SpotBugs đạt. F4-3 còn snapshot/jobs/infra/STOMP admin, runtime cuối ở FINAL; chưa full regression/merge, FE riêng. [Report](F:/prj_algoprep/06-plan/reports/261105-1547-f4-queue-controls/report.md).


**D02ad — 2026-11-05:** Backend snapshot queue #13 và danh sách job #14 xong: trạng thái/độ trễ từ SQL, counts và phân trang theo config thật, retention không xóa submission, quyền READ. 23 test liên quan đạt (14 mới),6/6 mutation,46/47 dòng logic mới; Spotless judge/bootstrap, scoped Checkstyle và SpotBugs đạt. F4-3 còn infra-summary/events và admin STOMP, rồi FINAL BE; chưa full regression/merge, FE riêng. [Report](F:/prj_algoprep/06-plan/reports/261105-1802-f4-queue-snapshot-jobs/report.md).


**D02ae — 2026-11-05:** Backend infra-summary #15 và infra-events #20 xong: cùng nguồn SQL thật, counts24h/danh sách90 ngày, message an toàn, lọc/ngày UTC+7 và cursor20 dòng. 20 test liên quan đạt (12 mới),6/6 mutation,78/78 dòng logic mới; Spotless judge/bootstrap, scoped Checkstyle và SpotBugs đạt. F4-3 còn admin STOMP W2, rồi FINAL BE; chưa full regression/merge, FE/QA riêng. [Report](F:/prj_algoprep/06-plan/reports/261105-f4-infra-summary-events/report.md).


**D02af — 2026-11-05:** STOMP admin W2 có QUEUE_BATCH thật theo chu kỳ5s, một lô chung cho mọi admin, không query khi không có subscriber hợp lệ. Snapshot/counts/infra-summary cùng transaction RR; outbound chặn phiên hết JWT hoặc đã unsubscribe.64 test liên quan đạt (18 mới),6/6 mutation,28/28 dòng logic mới; Spotless/Checkstyle/SpotBugs phạm vi chọn đạt. Backend F4-3 đủ API và W2; tiếp theo FINAL BE/full regression khi clear. FE/browser/C08 và production nhiều process giữ mốc riêng. Chưa commit/merge. [Report](F:/prj_algoprep/06-plan/reports/261105-f4-admin-queue-stream/report.md).
