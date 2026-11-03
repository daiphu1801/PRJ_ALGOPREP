# BD — Kiến trúc module `judge-orchestration` (F4)

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

**Triển khai 2026-11-03, D02j — queue dispatch kernel:** QueueSubmissionUseCase
gọi admission commit rồi publisher Rabbit thật (3 attempts,3s/lượt,200/500ms), message chỉID,
durable queue/persistent message/mandatory/ACK và không có return. Queue failure chỉ đổi
PENDING thành SYSTEM_ERROR/QUEUE_UNAVAILABLE; Graded Outbox cùng transaction mới, không
ghi đè worker/terminal đã thắng. Payload schema1/null scores/firstAttempt tuple(submittedAt,id).
262 test đạt,12 mutation mới; report `261103-1126-f4-queue-dispatch-core`.
Chỉ trusted kernel, chưa STOMP/HTTP/worker/relay; result trả trạng thái đã commit để caller
realtime/HTTP tương lai nối. B4 mới có nhánh lỗi queue persisted, chưa đóng B4 toàn luồng.


**Triển khai 2026-11-03, D02i — admission kernel:** có trusted internal
RecordPendingSubmissionUseCase; caller tương lai xác thực STUDENT/lấy userId từ JWT.
Kiểm đầu vào/source UTF-8/file, published/approved/snapshot, enabled/wrapper; ghim phiên bản
testcase vào PENDING. Khóa advisory theo user/problem, đếm cửa sổ >1h rồi INSERT cùng
giao dịch riêng READ COMMITTED; đọc đồng hồ DB sau khóa. Quota riêng hoặc property mặc
định60, mọi status tính lượt. Program/command toString không in code/file; lỗi DB kể cả
commit bị chặn ở ngoài TransactionTemplate, không kèm SQL/cause. Chưa HTTP, queue confirms,
failure finalization/Outbox/STOMP hoặc worker. Report `261103-1105-f4-pending-admission-core`.


**Grading dependency 2026-11-02:** ProblemGradingSpecPort/DTO/typed failure và bootstrap bridge
đã nối inbound F2/JDBC/MinIO thật. Metadata không có byte/key; nội dung chỉ ở DTO trusted với
array/list immutable và toString không in content. Phân biệt SPEC_UNAVAILABLE/SANDBOX_FAULT
cho worker tương lai; chưa state machine/persistence/finalize/Rabbit consumer trong lát này.
Report `261102-2340-f2-f4-grading-readers`.

**B1 assignment read, 2026-11-02:** F4 khai ClassAssignmentReadPort và ClassAssignment DTO,
bootstrap bridge gọi inbound F2 ListClassAssignmentsUseCase. Không import F2 từ judge,
không đọc chéo bảng hoặc gọi F1 từ F2. Port trả mọi khoảng giao/gỡ cho lớp caller đã kiểm
phạm vi; DB failure ném lỗi, không trả rỗng giả và không lộ raw SQL/cause qua bridge.
Chưa BR-JD-10 consumer hoặc P1 metrics; report `261102-1636-class-assignment-read-port`.

**Checkpoint component D02e 2026-11-01:** port compile/run được hiện thực bằng một scope
AutoCloseable: compile một lần, giữ artifact/fileId trên đúng instance, run từng testcase,
xóa cache trong close kể cả lỗi. EvaluateProgramUseCase là component nhận typed input, gọi
F3 mapping/matching qua port do F4 khai và bootstrap bridge; không phụ thuộc harness module.
Không có submission endpoint/worker/persistence/finalize/token/outbox trong lát này; state
machine và callback contract bên dưới chưa được hiện thực chỉ từ scope execution độc lập.

> Trạng thái: **BD lần đầu**, 2026-09-12. Module thứ tư theo
> `06-plan/260912-1055-bd-rollout-order.md`. Đọc cùng `02-bd/architecture/F3_harness.md` (định dạng gói tin
> harness đóng ra) và `02-bd/database/F2_problem-bank.md` mục 1.6 (versioning testcase, sự kiện
> `TestcaseSetVersionBumped`).

## 1. Vị trí trong kiến trúc

Module Maven `algoprep-judge`, schema PostgreSQL `judge` [SoT: `.nexa/domain-registry.json` — domain
`judge-orchestration`]. Bốn tầng theo baseline chung (`DEC-2026-0820-architecture-baseline`): `domain`
(aggregate `Submission`, value object `SubmissionTestcaseResult`, outbound port `JudgeExecutionPort` tại
`domain/ports/out`) / `application` (inbound port `application/ports/in`, `command/` `query/` `dto/`) /
`infrastructure` (adapter `GoJudgeAdapter` mặc định, `Judge0Adapter` dự phòng, consumer RabbitMQ, relay
Outbox, job sweep) / `presentation` (WebSocket/STOMP controller đẩy trạng thái).

Hạ tầng phụ thuộc: PostgreSQL · RabbitMQ (hàng đợi nộp bài) · WebSocket/STOMP (đẩy trạng thái) · REST đồng
bộ tới judge engine (mặc định go-judge) [SoT: `.nexa/control/dependency-map.md` mục 2]. Đây là **module
duy nhất** phụ thuộc một dịch vụ ngoài monolith [SoT: `dependency-map.md` mục 3].

**Không dùng MinIO riêng** — xác nhận ở mục 7 (Storage).

## 2. Phạm vi nghiệp vụ module này bao trùm

Đọc từ `01-rd/req/F4_judge-orchestration.md` (F4-01 → F4-13):

| Nhóm | Mã | Ghi chú |
| :--- | :--- | :--- |
| Tiếp nhận và chạy thử | F4-01, F4-02 | `PENDING` ngay, phản hồi không chờ; Sample không ghi tiến độ |
| Chấm từng testcase, không dừng sớm | F4-03, F4-04 (đã bãi bỏ), F4-13 | `DEC-2026-0831-partial-score-testcase-ratio` |
| Webhook/chống trùng (chỉ khi adapter bất đồng bộ) | F4-05, F4-06 | Chi tiết nội bộ adapter, không phải hợp đồng port |
| Sweep timeout | F4-07 | Ngưỡng 5 phút (`DEC-2026-0831-outside-screens-closures`) |
| Đẩy trạng thái thời gian thực | F4-08 | WebSocket/STOMP, kênh riêng theo `submissionId` |
| ~~Chấm lại~~ | ~~F4-09a→e~~ | **Đã loại bỏ khỏi phạm vi** (`DEC-2026-0828-remove-rejudge-scope`) — không thiết kế lại ở đây |
| Giám sát + điều khiển cụm | F4-10 | Bao gồm pause/resume tiêu thụ hàng đợi, bật/tắt autoscale worker (`DEC-2026-0831-judge-orchestration-ops-details`); sweep bỏ qua `PENDING` lúc đang tạm dừng (mục 5.3). **Cập nhật owner 2026-10-25:** bỏ bảng worker chi tiết vì go-judge không xuất các metric từng worker; snapshot chỉ lấy số liệu job/độ trễ từ DB và cấu hình F4 (`DEC-2026-1025-dd-drop-worker-detail`) |
| Cấu hình ngôn ngữ và giới hạn tài nguyên | F4-11 | 3 tham số sandbox go-judge: mạng (mặc định chặn), giới hạn tiến trình con, có trả `stderr` hay không |
| Chỉ số "Beats" | F4-12 | Chỉ hiện với `ACCEPTED`, so cùng bài + cùng ngôn ngữ |

## 3. Giao tiếp liên module

Theo `dependency-map.md` mục 1: không import trực tiếp. `judge-orchestration` dùng bốn kênh: hai cổng
ra do chính nó khai (mục 3.1, 3.2), một cặp cổng đọc cho module khác gọi vào (mục 3.3), và domain event
hai chiều (mục 3.4, 3.5).

### 3.1. Outbound port do chính module khai báo — gọi ra judge engine

`JudgeExecutionPort` (tên nghiệp vụ, không đặt theo transport của bất kỳ engine nào —
`DEC-2026-0823-go-judge-default-engine` điểm (2)) khai ở `domain/ports/out`, ký hiệu khái niệm:

```
JudgeExecutionPort.run(RunRequest) -> RunResult   // đồng bộ, một lần gọi cho một testcase
```

`RunRequest` nhận payload đã đóng gói sẵn theo định dạng adapter (do `harness` chuẩn bị — xem
`02-bd/architecture/F3_harness.md` mục 5 hàng "Đóng gói theo định dạng adapter": mặc định go-judge
`cmd[]`/`copyIn`-style, **không phải Base64 kiểu Judge0**). `judge-orchestration` không tự đóng gói lại,
chỉ chuyển tiếp payload harness đã tạo cho từng testcase kèm giới hạn thời gian/bộ nhớ đã nhân hệ số
ngôn ngữ (`language_configs`, mục 6 file `database/F4_judge-orchestration.md`).

`RunResult` trả về khái niệm: `exitCode`, `stdout`, `stderr` (chỉ lưu/trả cho `STANDARD_IO` và testcase Sample khi `sandbox_settings.return_stderr_to_student = true`, owner 2026-10-25; cột cờ ở `sandbox_settings` toàn cục), `runtimeMs`, `memoryKb`, `verdict` sơ bộ theo testcase (`AC`/`WA`/`TLE`/`MLE`/`RE`) —
verdict này do `harness` tính qua bước so khớp kết quả (F3-07→10), `judge-orchestration` chỉ **tổng hợp**,
không tự so khớp output.

### 3.2. Đọc dữ liệu chỉ-đọc từ `problem-bank` (không import, qua outbound port hẹp)

Để build gói chấm cho một testcase (input/expected-output, giới hạn thời gian/bộ nhớ cơ sở của bài,
`matching_strategy`), `judge-orchestration` khai một outbound port hẹp riêng (tên đề xuất
`ProblemGradingSpecPort`, chốt thật ở DD) — **không** dùng lại `JudgeExecutionPort` cho việc này (khác
mục đích: một cái gọi engine ngoài, một cái đọc dữ liệu nội bộ hệ thống). `[SoT: Suy luận]` — đối xứng với
câu hỏi mở đã ghi ở `02-bd/architecture/F2_problem-bank.md` mục 3.2 về việc F2-14 có dùng chung
`JudgeExecutionPort` hay không; câu trả lời ở đây là **không dùng chung**, vì hai chiều gọi khác hẳn nhau
(một là "chấm bài của học viên", một là "đọc đặc tả bài toán").

### 3.2a. Outbound port — ghi nhật ký hệ thống (từ `identity`, thêm 2026-10-23)

`AuditLogPort` do `identity` phơi ra (xem `identity`, mục cổng `AuditLogPort`): `judge-orchestration` gọi cổng này để ghi dòng `system_audit_logs` trong cùng JVM và cùng giao dịch DB với thay đổi nghiệp vụ, không ghi thẳng schema `identity` (`DEC-2026-1023-db-actor-review-decisions`).

### 3.3. Cổng đọc số liệu nộp bài cho module khác — **hai luồng tách riêng**

`judge.submissions` là nơi duy nhất trả lời được bốn câu hỏi mà không read model nào của `identity` trả
lời được: chuỗi ngày luyện tập, mốc nộp gần nhất, chuỗi lượt nộp theo thời gian, và tỉ lệ Accepted **giới
hạn trong một tập bài**. `identity.user_submission_stats` chỉ có số luỹ kế toàn hệ thống
[Nguồn: 02-bd/database/F1_identity.md:113-114], và `identity` **không được truy vấn chéo schema**. Vì vậy
module này phải mở cổng đọc.

Năm màn đã viết BD dựa trên giả định cổng đó tồn tại — `INS0201`, `INS0203`, `INS0204`, `USR0501`,
`USR0601` — trong khi mục này **chưa từng được viết**, kể cả ở mục 9 "Việc còn mở". Đây là lần đầu nó
được đặc tả.

**Chốt: hai cổng, không phải một cổng tham số hoá** (`DEC-2026-0927-submission-metrics-two-ports`).

| | Luồng Giảng viên | Luồng Người học |
| :--- | :--- | :--- |
| Tên nghiệp vụ | `GetClassStudentSubmissionMetrics` | `GetMySubmissionMetrics` |
| Actor | A2 (`INSTRUCTOR`) | A1 (`STUDENT`) |
| Đọc dữ liệu của | **người khác** — một tập học viên | **chính mình**, không ai khác |
| Lấy danh tính từ | tham số `studentIds` tường minh, gác bởi phạm vi lớp | **token của phiên đăng nhập**, không có tham số danh tính |
| Đơn vị gom thời gian | tuần | ngày |
| Phạm vi lọc bài | tập bài đã giao cho lớp | tập chủ đề, hoặc toàn bộ |
| Màn gọi | `INS0201`, `INS0203`, `INS0204` | `USR0501`, `USR0601` |

Ký hiệu khái niệm (chữ ký thật chốt ở DD):

```
GetClassStudentSubmissionMetrics(classId, students[{studentId, joinedAt, leftAt?}], assignments[{problemId, assignedAt, removedAt?}], weekCount)
  -> [ { studentId, streakDays, lastSubmittedAt, acceptedRatio, weeklyTrend[], assignmentResults[] } ]

GetMySubmissionMetrics(range | dayCount, topicIds[]?)        // userId LẤY TỪ TOKEN
  -> { streakDays, lastSubmittedAt, acRateByTopic[], dailyCounts[] }
```

**Quy tắc tuần và cache của `GetClassStudentSubmissionMetrics`** (`DEC-2026-1014-instructor-batch2-weekly-rules`): số liệu theo tuần (`weeklyTrend[]` và nhãn trạng thái "Cần hỗ trợ" dựa trên nó) **tính lúc truy vấn** từ `judge.submissions`, kết quả cache Redis **15 phút**; không có bảng ảnh chụp theo tuần và không có job định kỳ. Tuần bắt đầu **thứ Hai** (ISO), tuần đang chạy chưa đủ 7 ngày **không được tính**, nên `weekCount` là số tuần đủ gần nhất. Mốc "Vắng bài" (không nộp trong 7 ngày gần nhất) là cửa sổ trượt tính từ thời điểm truy vấn, không theo tuần ISO. Chỉ xét lại bằng bảng ảnh chụp nếu đo được truy vấn chậm. Nguồn: `02-bd/screens/teacher/INS0203_class_progress.md` Câu hỏi mở Q3.

Số liệu lớp chỉ lấy lượt nộp trong khoảng giao bài `[assignedAt, removedAt)`; khi `removedAt` rỗng thì chưa có mốc kết thúc. `assignmentResults[]` chỉ trả lần giao còn hiệu lực để F1 tính hoàn thành; lượt Accepted trước khi giao và sau khi gỡ không hoàn thành bài trong lớp (`DEC-2026-1025-dd-class-assignment-time-window`).

**Vì sao tách chứ không tham số hoá.** Ba lý do, lý do đầu là lý do quyết định:

1. **Quy tắc chống IDOR trở thành thuộc tính của chữ ký, không còn là quy ước phải nhớ.** Cả
   `USR0501` lẫn `USR0601` đều có một dòng kiểm bắt buộc ở Sheet 9: mọi endpoint của màn lấy `user_id` từ
   token, **không** nhận từ tham số phía client. Một cổng gộp có tham số `studentIds[]` tuỳ chọn thì hai
   màn Người học vẫn gọi được kèm `studentIds` của người khác — không có gì trong chữ ký ngăn việc đó, chỉ
   có một câu văn trong BD. Tách ra thì cổng phía Người học **không có chỗ để truyền danh tính**, muốn lách
   cũng không lách được.
2. Hai đơn vị gom thời gian khác nhau (tuần cho xu hướng lớp, ngày cho biểu đồ cá nhân) và hai chiều dữ
   liệu khác nhau (một tập học viên so với một người). Gộp lại thì một nửa tham số luôn thừa ở mỗi lần gọi.
3. Hai đường đi qua hai tầng phân quyền khác nhau: luồng Giảng viên gác bởi `CLASS_MANAGEMENT` và phạm vi
   lớp, luồng Người học gác bởi chính phiên đăng nhập.

Đối xứng ở phía gọi: `identity` khai **hai** outbound port hẹp — `ClassStudentMetricsPort` và
`MySubmissionMetricsPort` — thay cho một `StudentSubmissionMetricsPort` duy nhất mà các BD màn trước đây
giả định. Cùng nguyên tắc "hai chiều gọi khác hẳn nhau thì không dùng chung cổng" đã áp ở mục 3.2.

**Lưới hoạt động 12 tháng** (`USR0601` Khu vực E) dùng chính `GetMySubmissionMetrics` với `dayCount = 364`,
**không** phải một endpoint riêng của `identity` như bản BD màn đó đề xuất lúc đầu: để hai module cùng trả
lời câu "mỗi ngày có bao nhiêu lượt nộp" là đúng cái trôi lệch mà mục này sinh ra để chặn. Việc quy đổi số
đếm sang 5 mức đậm nhạt là của bên gọi, không phải của cổng. Rủi ro hiệu năng của cửa sổ 364 ngày trên màn
đích sau đăng nhập vẫn là **việc còn mở**, xem mục 9.

`[Suy luận]` — tên cổng và tên tham số do BD này đề xuất, DD chốt lại. Cái được chốt ở đây là **ranh giới
hai luồng**, không phải chính tả của chữ ký.

### 3.4. Domain event — phát ra (F4 → F5, F4 → F1)

Xem mục 5 (Transactional Outbox, `DEC-2026-0912-judge-outbox-pattern`) — đây là điểm chốt dứt khoát của
BD này cho câu hỏi mở ở `dependency-map.md` mục 1.

### 3.5. Domain event — lắng nghe (F2 → F4)

`judge-orchestration` không tiêu thụ `TestcaseSetVersionBumped`. Khi nhận bài nộp, F4 đọc
`currentTestcaseSetVersion` từ F2 và ghi `testcase_set_version_at_grading` cùng lúc tạo dòng `PENDING`.
Worker đọc bundle bất biến của đúng phiên bản đã ghim; việc sửa testcase trong lúc chờ không đổi bộ chấm
của lượt đó. F2 phát sự kiện sau commit để bên cần làm mới cache, không dùng nó để gán phiên bản cho bài nộp.

## 4. Luồng chấm bài (đồng bộ, go-judge mặc định) — state machine

**Sáu trạng thái cuối, không bao giờ bị ghi đè** (sửa 2026-10-22, đồng bộ DB phase 3: trước ghi "bảy"; sáu = `ACCEPTED`, `WRONG_ANSWER`, `TIME_LIMIT_EXCEEDED`, `RUNTIME_ERROR`, `COMPILE_ERROR`, `SYSTEM_ERROR`, khớp `database/F4_judge-orchestration.md:33`) (`glossary.md` mục 3, giữ nguyên bất kể adapter) — chốt
chính thức ở `DEC-2026-0912-judge-callback-contract`:

```
PENDING --(worker nhận job từ RabbitMQ)--> COMPILING
COMPILING --(biên dịch lỗi)--> COMPILE_ERROR [CUỐI]
COMPILING --(biên dịch OK)--> JUDGING
JUDGING --(gọi JudgeExecutionPort.run() cho TỪNG testcase, KHÔNG dừng sớm — F4-04 đã bãi bỏ,
            DEC-2026-0831-partial-score-testcase-ratio)--> JUDGING (lặp N lần)
JUDGING --(đã chạy hết N testcase, tổng hợp verdict)--> một trong:
    ACCEPTED            [CUỐI]  -- toàn bộ N testcase AC
    WRONG_ANSWER        [CUỐI]  -- verdict testcase thất bại đầu tiên (theo thứ tự) là WA
    TIME_LIMIT_EXCEEDED [CUỐI]  -- verdict testcase thất bại đầu tiên là TLE (hoặc MLE, gộp nhãn)
    RUNTIME_ERROR       [CUỐI]  -- verdict testcase thất bại đầu tiên là RE
(bất kỳ trạng thái không-cuối) --(worker crash, không cập nhật > 5 phút, job sweep quét thấy)-->
    SYSTEM_ERROR [CUỐI], failure_reason = WORKER_TIMEOUT
```

Sửa 2026-10-22 (đồng bộ DB, phase 3, JD-14): sơ đồ trên chỉ có đường ghi `failure_reason = WORKER_TIMEOUT` (job sweep). Giá trị `SANDBOX_FAULT` của cột (`database/F4_judge-orchestration.md:23`, "lỗi sandbox tức thời") được **worker ghi** khi go-judge báo lỗi hạ tầng không phải verdict của testcase (ví dụ không khởi tạo được sandbox, mất kết nối go-judge): submission sang `SYSTEM_ERROR`, `failure_reason = SANDBOX_FAULT`, cùng giao dịch với dòng outbox như mọi trạng thái cuối. Còn `WORKER_TIMEOUT` do job sweep ghi. Hai nguồn này phân biệt được trên `ADM0401`/`ADM0404`. Sửa 2026-10-22 (đồng bộ DB vòng 2, DEC-2026-1022-db-sync-round2, mục 5): chủ dự án chốt; thay câu "chưa có luồng ghi" của phase 3. Ngưỡng và cách nhận diện lỗi hạ tầng cụ thể (mã lỗi nào của go-judge) để DD.

DD 2026-10-25 bổ sung `SPEC_UNAVAILABLE` khi worker thấy đặc tả/bundle testcase thiếu, hỏng hoặc không parse được; đây cũng là `SYSTEM_ERROR` với outbox và không tính vào tỉ lệ AC. `SANDBOX_FAULT` vẫn chỉ lỗi hạ tầng/harness nội bộ (`03-dd/logic/F4_judge-orchestration.md` BR-JD-03/04).

`[SoT: Suy luận]` — nguyên tắc "verdict tổng hợp = verdict của testcase thất bại đầu tiên theo thứ tự"
không có trong RD (RD chỉ nói Accepted là pass hết, chưa nói pattern hỗn hợp WA+TLE+RE trong cùng một lần
chấm hiển thị verdict nào). Đây là quy ước phổ biến (LeetCode/Codeforces): trả về verdict lỗi đầu tiên gặp
theo thứ tự testcase, dù có chạy hết N testcase để tính F4-13. Đề xuất chốt theo quy ước này; chủ dự án
xác nhận nếu muốn khác (ví dụ "verdict nghiêm trọng nhất" theo một thứ tự ưu tiên cố định RE > TLE > WA).

Điểm mấu chốt khác biệt so với thiết kế fail-fast cũ: **`JUDGING` không kết thúc sớm** dù testcase thứ 3
đã WA — vòng lặp vẫn gọi hết N-3 testcase còn lại để `F4-13` (điểm tỷ lệ) có đủ dữ liệu. Không thiết kế bất
kỳ nhánh "dừng khi gặp lỗi" nào ở tầng này.

## 5. Judge0 callback contract — chốt dứt khoát (`DEC-2026-0912-judge-callback-contract`)

Dù go-judge (adapter mặc định) đồng bộ và không có callback, `JudgeExecutionPort` vẫn phải thiết kế trung
lập để một adapter bất đồng bộ (`Judge0Adapter`) cắm lại được mà không đổi chữ ký port.

### 5.1. Idempotency — `judge_run_tokens`

Mỗi lần worker gọi `JudgeExecutionPort.run()` cho một `(submission_id, testcase_id)`, nó phát hành một
token (UUID) trước khi gọi, lưu ở `judge_run_tokens` với `consumed_at = NULL`. Ngay trong **cùng giao dịch
DB** ghi `submission_testcase_results`, `consumed_at` được set. Trước khi (gọi lại) một testcase — kể cả
sau khi RabbitMQ redeliver message vì worker trước đó crash giữa chừng — worker kiểm tra: đã có token
`consumed_at IS NOT NULL` cho đúng cặp `(submission_id, testcase_id)` chưa; nếu có, **bỏ qua, không gọi
lại engine, không ghi đè kết quả đã có** — đây là cách hiện thực hoá bất biến "không bao giờ ghi đè trạng
thái cuối" ở mức từng testcase, không chỉ ở mức submission.

Với `Judge0Adapter` (nếu kích hoạt lại), cùng token này còn đóng vai trò bí mật webhook (F4-05): gửi kèm
Judge0 dưới dạng tham số callback, Judge0 trả lại đúng token trong lời gọi webhook, adapter tra
`judge_run_tokens` để xác thực + chống trùng (F4-06) trước khi ghi kết quả — **toàn bộ logic này nằm trong
`Judge0Adapter`, không lộ ra khỏi `JudgeExecutionPort`.**

### 5.2. `callback_logs` — chỉ có ý nghĩa với adapter bất đồng bộ

Ghi mọi lượt callback nhận được (kể cả bị từ chối) để audit/debug khi `Judge0Adapter` hoạt động. **Với
go-judge mặc định, bảng này không bao giờ có dòng** — không phải bảng rỗng thừa, mà là bằng chứng vận
hành đúng: không có callback nào tồn tại trong luồng đồng bộ.

### 5.3. Sweep timeout (F4-07)

- **Ngưỡng "treo"**: 5 phút không nhận được cập nhật trạng thái nào cho một submission
  (`01-rd/req/F4_judge-orchestration.md` dòng 41-43, `DEC-2026-0831-outside-screens-closures`).
- **Chu kỳ quét**: job `@Scheduled` chạy mỗi 60 giây `[SoT: Suy luận]` — đủ nhanh để không kéo dài trải
  nghiệm chờ của học viên quá lâu sau ngưỡng 5 phút, đủ thưa để không tạo tải quét liên tục lên Postgres.
- **Truy vấn**: quét `submissions` có `status IN (PENDING, COMPILING, JUDGING)` và `updated_at <
  now() - interval '5 minutes'`.
- **Khi hàng đợi đang tạm dừng (G1, `DEC-2026-1010-admin-operations-regroup`)**: nếu `cluster_settings.queue_paused = true`
  thì job sweep **bỏ qua** job `PENDING` (chưa có worker nào nhận nên không có cập nhật trạng thái nào là
  hợp lệ) và chỉ quét `COMPILING` và `JUDGING`. Nếu không, tạm dừng quá 5 phút sẽ đẩy mọi bài nộp đang chờ
  sang `SYSTEM_ERROR`. Đồng hồ 5 phút của job `PENDING` chỉ bắt đầu tính lại từ lúc hàng đợi được mở lại
  (`updated_at` được chạm khi tiếp tục tiêu thụ) `[SoT: Suy luận]`, DD chốt cách chạm.
- **Hành động**: `UPDATE ... SET status = 'SYSTEM_ERROR', failure_reason = 'WORKER_TIMEOUT' WHERE id = ?
  AND status NOT IN (<6 trạng thái cuối>)` — mệnh đề `WHERE` có điều kiện là bắt buộc, không phải tối ưu:
  nó là cơ chế chống ghi đè khi một worker "chậm" hoàn tất đúng lúc job sweep chạy (race hiếm nhưng có
  thể xảy ra).
- **Ý nghĩa với adapter đồng bộ**: gần như luôn là worker crash (OOM, pod bị kill, hạ tầng go-judge tự nó
  timeout không trả lời) — **không phải "mất callback ngoài"** như khi dùng Judge0Adapter
  (`dependency-map.md` mục 3, bảng hai failure mode).

### 5.4. Webhook auth — thiết kế port-trung lập

`JudgeExecutionPort.run()` **không có tham số nào mang hình dạng webhook** (không có `callbackUrl`,
không có `webhookToken` ở chữ ký port) — những thứ đó chỉ tồn tại bên trong `Judge0Adapter` khi nó dịch
một lời gọi port đồng bộ (từ góc nhìn caller) thành `POST tới Judge0 kèm callback_url` +
`chờ webhook` nội bộ. Nhờ vậy nếu Judge0Adapter được bật lại, không ai gọi `JudgeExecutionPort` phải sửa
code — đúng nguyên tắc `dependency-map.md` mục 3: "complexity is adapter-internal, not exposed past
`JudgeExecutionPort`".

## 6. Transactional Outbox cho F4 → F5/F1 (`DEC-2026-0912-judge-outbox-pattern`)

**Chốt: có Outbox.** Khi một submission đạt một trong sáu trạng thái cuối (sửa 2026-10-22, đồng bộ DB phase 3), `judge-orchestration`:

1. Trong **cùng giao dịch DB** cập nhật `submissions.status`, ghi một dòng vào `outbox_events` (payload
   JSON, loại sự kiện, thời điểm tạo, `published_at = NULL`).
2. Một relay riêng (component `@Scheduled`, không nằm trong luồng request/giao dịch chấm bài) đọc các
   dòng `published_at IS NULL` theo thứ tự, publish lên RabbitMQ (exchange sự kiện domain), chỉ set
   `published_at` **sau khi** broker xác nhận (ack) — nếu publish lỗi, dòng ở lại hàng đợi outbox để lượt
   quét sau thử lại.

Hai sự kiện phát ra, khớp tên đã dùng ở `02-bd/architecture/F1_identity.md` mục 3.1:

| Sự kiện | Khi nào | Payload chính | Ai tiêu thụ |
| :--- | :--- | :--- | :--- |
| `SubmissionAccepted` | `status → ACCEPTED` | `submissionId`, `userId`, `problemId`, `language` | `ai-review` (kích hoạt F5.1/F5.2), `identity` (`identity_recent_activity`) |
| `SubmissionGraded` | Mọi trạng thái cuối (bao gồm cả `ACCEPTED`) | `submissionId`, `userId`, `problemId`, `verdict`, `passedCount`, `totalCount`, tỉ lệ F4-13, `language`, `submittedAt`, `firstAttemptForProblem` (Sửa 2026-10-22, đồng bộ DB vòng 2) | `identity` (`user_problem_best_score`, `user_submission_stats`, `user_daily_submission_stats`) |

Sửa 2026-10-22 (đồng bộ DB, phase 3, JD-18): payload `SubmissionGraded` còn phải mang `language` (nuôi `user_submission_stats.top_language`/`language_counts`, `database/F1_identity.md:191`) và một mốc thời gian của lượt nộp để `identity` quy ra `day` UTC+7 cho `user_daily_submission_stats` (`database/F1_identity.md:193`). Sửa 2026-10-22 (đồng bộ DB vòng 2, DEC-2026-1022-db-sync-round2, mục 6): **đã chốt** payload `SubmissionGraded` mang thêm `submittedAt` (mốc nộp, UTC; `identity` quy ra ngày UTC+7) và `firstAttemptForProblem` (boolean, `judge` tính từ lịch sử `submissions` của cùng user và bài; nuôi `first_try_accepted_count`). Bảng trên: payload chính của `SubmissionGraded` = `submissionId`, `userId`, `problemId`, `language`, `verdict`, `passedCount`, `totalCount`, tỉ lệ F4-13, `submittedAt`, `firstAttemptForProblem`. Phía `identity` do agent khác sửa (`03-dd/api/F1_identity.md`); cấu trúc JSON và version payload vẫn để DD.

**Phương án bị loại: `@TransactionalEventListener(phase = AFTER_COMMIT)` publish thẳng RabbitMQ.** Đảm bảo
listener chỉ chạy sau khi commit thành công, nhưng **không đảm bảo chính lời gọi publish thành công** — nếu
RabbitMQ không phản hồi đúng khoảnh khắc đó, sự kiện mất hẳn, không có bản ghi bền vững nào để thử lại.
Đây đúng là rủi ro cần đóng: mất trigger F5 ngay sau lần Accepted đầu tiên của học viên (thời điểm giá trị
sản phẩm cao nhất), và read model F1-06/F1-07 của `identity` âm thầm lệch dữ liệu không báo lỗi. Outbox +
relay polling giải quyết cả hai vì bảng `outbox_events` nằm cùng giao dịch với thay đổi trạng thái — hai
thứ không bao giờ lệch nhau, relay có thể retry vô hạn lần trên một dòng bền vững thay vì một lời gọi
trong bộ nhớ đã mất.

**Vì sao relay là polling, không phải gọi đồng bộ trong giao dịch chấm bài**: nếu publish đồng bộ ngay
trong giao dịch chấm, một broker chậm/down sẽ kéo dài thời gian phản hồi của chính luồng chấm testcase
đồng bộ — vi phạm tinh thần "không phụ thuộc AI path" mở rộng sang "không phụ thuộc broker path" cho luồng
chấm lõi.

## 7. Storage — xác nhận không cần MinIO riêng

`[SoT: Suy luận]` dựa trên `dependency-map.md` mục 2 (bảng hạ tầng của `judge-orchestration` chỉ liệt kê
PostgreSQL + RabbitMQ + WebSocket + judge engine REST, không có MinIO) và thực tế input/expected-output
lớn đã thuộc về `problem-bank` (`02-bd/storage/F2_problem-bank.md`). `judge-orchestration` chỉ cần lưu:

- `source_code` của bài nộp — TEXT trong Postgres (mã nguồn một bài giải hiếm khi vượt vài chục KB, không
  cần object storage).
- `stderr` thực tế của một lần chạy **chỉ cho `STANDARD_IO`, testcase Sample, verdict khác AC và cờ admin bật** — cắt ngắn
  theo giới hạn ký tự cố định 4KB rồi lưu trực tiếp trong `submission_testcase_results.stderr_snippet`;
  `FUNCTION_WRAPPER` và Hidden không lưu do có thể lộ stack trace khung hoặc input ẩn (owner 2026-10-25).

**Kết luận: không tạo `02-bd/storage/F4_judge-orchestration.md`** — đúng quy tắc `bd-generation` "không phải
module nào cũng cần cả 4 file, không viết file rỗng".

## 8. Chỉ số "Beats" (F4-12) — công thức đã chốt ở RD, đóng câu hỏi

**Đã xác nhận 2026-09-13 — không cần hỏi lại chủ dự án.** RD chốt công thức này từ trước (không phải BD
tự suy diễn): `01-rd/req/F4_judge-orchestration.md` F4-12, bổ sung 2026-08-25 qua việc đóng Câu hỏi mở Q4 của
`01-rd/screens/users/USR0201_submission_result.md` — trích nguyên văn: "tính tỉ lệ phần trăm bài nộp khác **nhanh
hơn hoặc bằng** (theo runtime) chậm hơn bài nộp hiện tại, trong tập tất cả bài nộp `Accepted` của cùng bài
toán và cùng ngôn ngữ lập trình". BD chỉ cần chuyển câu chữ đó thành công thức:

```
beats% = ROUND(
  (số bài nộp Accepted khác — cùng problemId, cùng language — có runtime_ms >= runtime_ms của bài nộp
   hiện tại) / (tổng số bài nộp Accepted — cùng problemId, cùng language, KỂ CẢ bài nộp hiện tại) * 100
)
```

Làm tròn tới số nguyên gần nhất (không thập phân) `[SoT: Suy luận]` — chi tiết làm tròn duy nhất RD chưa
nêu, chốt ở DD nếu cần đổi. Rỗng (`—`) cho mọi verdict khác `ACCEPTED` — đúng RD dòng 75. Xem
`07-review/bd_open_questions_260913.md` mục 2 câu #5.

## 9. Việc còn mở — chuyển sang DD

- Tên/chữ ký cụ thể `ProblemGradingSpecPort` (mục 3.2).
- Payload đầy đủ (versioning) của `SubmissionAccepted`/`SubmissionGraded` (mục 6).
- ~~Xác nhận công thức F4-12 với chủ dự án~~ — đã đóng 2026-09-13, xem mục 8.
- Định dạng `RunRequest`/`RunResult` thật (JSON shape gửi/nhận go-judge) — chốt cùng `harness` khi cả hai
  vào DD, vì `harness` là bên tạo payload.
- Chu kỳ quét sweep 60 giây (mục 5.3) và giới hạn cắt `stderr_snippet` 4KB (mục 7) — số đề xuất, chủ dự án
  xác nhận hoặc đổi số cụ thể.
- Chữ ký thật của `GetClassStudentSubmissionMetrics` và `GetMySubmissionMetrics` (mục 3.3): tên tham số,
  kiểu trả về, phân trang nếu tập học viên lớn.
- **Hiệu năng cửa sổ 364 ngày** của `GetMySubmissionMetrics`: đây là truy vấn chạy ở màn đích sau đăng
  nhập của **mọi** người học. Ba hướng — index `(user_id, submitted_at)` rồi đếm trực tiếp, thêm read model
  đếm sẵn theo ngày, hoặc cache Redis theo `user_id`. Chưa chốt; đề xuất đo trước rồi chọn, không tối ưu
  trước khi có số.

## 10. Tham chiếu

- `01-rd/req/F4_judge-orchestration.md` — đặc tả F4-01 → F4-13.
- `.nexa/domain-registry.json` — domain `judge-orchestration`, scope liệt kê bảng dữ liệu dự kiến.
- `.nexa/control/dependency-map.md` mục 2-4 — hạ tầng, luồng đồng bộ go-judge, thứ tự build (stage 2 cùng
  `harness`).
- `.nexa/control/decision-registry.md` — `DEC-2026-0823-go-judge-default-engine`,
  `DEC-2026-0831-partial-score-testcase-ratio`, `DEC-2026-0828-remove-rejudge-scope`,
  `DEC-2026-0831-judge-orchestration-ops-details`, `DEC-2026-0912-judge-callback-contract`,
  `DEC-2026-0912-judge-outbox-pattern`.
- `02-bd/architecture/F3_harness.md` mục 5 — định dạng gói tin adapter mà harness đóng ra.
- `02-bd/architecture/F2_problem-bank.md` mục 3.2 — câu hỏi outbound port F2-14 đối xứng với mục 3.2 file này.
- `02-bd/database/F2_problem-bank.md` mục 1.6 — versioning testcase, sự kiện `TestcaseSetVersionBumped`.
- `02-bd/architecture/F1_identity.md` mục 3.1 — tên sự kiện `SubmissionGraded`/`SubmissionAccepted` đã được
  phía `identity` dự kiến trước, file này xác nhận khớp tên.
- `06-plan/260912-1055-bd-rollout-order.md` — thứ tự triển khai BD.


## Ghi nhận triển khai D02n — 2026-11-03

Worker runtime D02n nối queue ID-only → lease phiên/job → F2 spec + snapshot đã ghim → F3 prepare → go-judge compile/run-all → persistence D02m → Outbox/STOMP. Không giữ transaction lúc gọi engine; lease dùng kết nối riêng qua các commit. Chỉ bật listener bằng `judge.worker.enabled=true`, Hikari tối thiểu 10 kết nối (8 job + leader + ghi). API mặc định không bật listener. Chưa có Compose/rollout, relay, sweep, multi-instance STOMP hoặc metrics lớp/admin; không coi cả F4 hoàn tất.


## D02n1 — Đóng nợ worker local (2026-11-03)

Đã có profile worker yêu cầu URL engine, Compose sandbox tùy chọn độc lập và runbook local một backend HTTP/WebSocket/worker. Có metric và drain, chặn tái dùng instance mất leader. Chưa dùng STOMP nội bộ cho topology nhiều process; production rollout/network-enabled theo phạm vi đã ghi.


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
