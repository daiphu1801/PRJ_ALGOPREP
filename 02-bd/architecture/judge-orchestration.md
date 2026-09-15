# BD — Kiến trúc module `judge-orchestration` (F4)

> Trạng thái: **BD lần đầu**, 2026-09-12. Module thứ tư theo
> `06-plan/260912-1055-bd-rollout-order.md`. Đọc cùng `02-bd/architecture/harness.md` (định dạng gói tin
> harness đóng ra) và `02-bd/database/problem-bank.md` mục 1.6 (versioning testcase, sự kiện
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

Đọc từ `01-rd/req/judge-orchestration.md` (F4-01 → F4-13):

| Nhóm | Mã | Ghi chú |
| :--- | :--- | :--- |
| Tiếp nhận và chạy thử | F4-01, F4-02 | `PENDING` ngay, phản hồi không chờ; Sample không ghi tiến độ |
| Chấm từng testcase, không dừng sớm | F4-03, F4-04 (đã bãi bỏ), F4-13 | `DEC-2026-0831-partial-score-testcase-ratio` |
| Webhook/chống trùng (chỉ khi adapter bất đồng bộ) | F4-05, F4-06 | Chi tiết nội bộ adapter, không phải hợp đồng port |
| Sweep timeout | F4-07 | Ngưỡng 5 phút (`DEC-2026-0831-outside-screens-closures`) |
| Đẩy trạng thái thời gian thực | F4-08 | WebSocket/STOMP, kênh riêng theo `submissionId` |
| ~~Chấm lại~~ | ~~F4-09a→e~~ | **Đã loại bỏ khỏi phạm vi** (`DEC-2026-0828-remove-rejudge-scope`) — không thiết kế lại ở đây |
| Giám sát + điều khiển cụm | F4-10 | Bao gồm pause/resume tiêu thụ hàng đợi, bật/tắt autoscale worker (`DEC-2026-0831-judge-orchestration-ops-details`) |
| Cấu hình ngôn ngữ và giới hạn tài nguyên | F4-11 | 3 tham số sandbox go-judge: mạng (mặc định chặn), giới hạn tiến trình con, có trả `stderr` hay không |
| Chỉ số "Beats" | F4-12 | Chỉ hiện với `ACCEPTED`, so cùng bài + cùng ngôn ngữ |

## 3. Giao tiếp liên module

Theo `dependency-map.md` mục 1: không import trực tiếp. `judge-orchestration` dùng ba kênh:

### 3.1. Outbound port do chính module khai báo — gọi ra judge engine

`JudgeExecutionPort` (tên nghiệp vụ, không đặt theo transport của bất kỳ engine nào —
`DEC-2026-0823-go-judge-default-engine` điểm (2)) khai ở `domain/ports/out`, ký hiệu khái niệm:

```
JudgeExecutionPort.run(RunRequest) -> RunResult   // đồng bộ, một lần gọi cho một testcase
```

`RunRequest` nhận payload đã đóng gói sẵn theo định dạng adapter (do `harness` chuẩn bị — xem
`02-bd/architecture/harness.md` mục 5 hàng "Đóng gói theo định dạng adapter": mặc định go-judge
`cmd[]`/`copyIn`-style, **không phải Base64 kiểu Judge0**). `judge-orchestration` không tự đóng gói lại,
chỉ chuyển tiếp payload harness đã tạo cho từng testcase kèm giới hạn thời gian/bộ nhớ đã nhân hệ số
ngôn ngữ (`language_configs`, mục 6 file `database/judge-orchestration.md`).

`RunResult` trả về khái niệm: `exitCode`, `stdout`, `stderr` (chỉ giữ nếu `language_configs.return_stderr_
to_student = true`), `runtimeMs`, `memoryKb`, `verdict` sơ bộ theo testcase (`AC`/`WA`/`TLE`/`MLE`/`RE`) —
verdict này do `harness` tính qua bước so khớp kết quả (F3-07→10), `judge-orchestration` chỉ **tổng hợp**,
không tự so khớp output.

### 3.2. Đọc dữ liệu chỉ-đọc từ `problem-bank` (không import, qua outbound port hẹp)

Để build gói chấm cho một testcase (input/expected-output, giới hạn thời gian/bộ nhớ cơ sở của bài,
`matching_strategy`), `judge-orchestration` khai một outbound port hẹp riêng (tên đề xuất
`ProblemGradingSpecPort`, chốt thật ở DD) — **không** dùng lại `JudgeExecutionPort` cho việc này (khác
mục đích: một cái gọi engine ngoài, một cái đọc dữ liệu nội bộ hệ thống). `[SoT: Suy luận]` — đối xứng với
câu hỏi mở đã ghi ở `02-bd/architecture/problem-bank.md` mục 3.2 về việc F2-14 có dùng chung
`JudgeExecutionPort` hay không; câu trả lời ở đây là **không dùng chung**, vì hai chiều gọi khác hẳn nhau
(một là "chấm bài của học viên", một là "đọc đặc tả bài toán").

### 3.3. Domain event — phát ra (F4 → F5, F4 → F1)

Xem mục 5 (Transactional Outbox, `DEC-2026-0912-judge-outbox-pattern`) — đây là điểm chốt dứt khoát của
BD này cho câu hỏi mở ở `dependency-map.md` mục 1.

### 3.4. Domain event — lắng nghe (F2 → F4)

`judge-orchestration` lắng nghe `TestcaseSetVersionBumped` phát từ `problem-bank`
(`02-bd/database/problem-bank.md` mục 1.6) — không để cập nhật testcase (không còn re-judge), chỉ để ghi
lại `testcase_set_version_at_grading` đúng số hiệu hiện hành tại thời điểm một submission mới bắt đầu
chấm (không phải tại thời điểm nộp, vì hai mốc có thể lệch nếu Admin bump version giữa lúc nộp và lúc
worker nhận job — ghi tại thời điểm bắt đầu compile là mốc chính xác nhất để trả lời đúng F2-09 "chấm
theo version nào").

## 4. Luồng chấm bài (đồng bộ, go-judge mặc định) — state machine

**Bảy trạng thái cuối, không bao giờ bị ghi đè** (`glossary.md` mục 3, giữ nguyên bất kể adapter) — chốt
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
  (`01-rd/req/judge-orchestration.md` dòng 41-43, `DEC-2026-0831-outside-screens-closures`).
- **Chu kỳ quét**: job `@Scheduled` chạy mỗi 60 giây `[SoT: Suy luận]` — đủ nhanh để không kéo dài trải
  nghiệm chờ của học viên quá lâu sau ngưỡng 5 phút, đủ thưa để không tạo tải quét liên tục lên Postgres.
- **Truy vấn**: quét `submissions` có `status IN (PENDING, COMPILING, JUDGING)` và `updated_at <
  now() - interval '5 minutes'`.
- **Hành động**: `UPDATE ... SET status = 'SYSTEM_ERROR', failure_reason = 'WORKER_TIMEOUT' WHERE id = ?
  AND status NOT IN (<7 trạng thái cuối>)` — mệnh đề `WHERE` có điều kiện là bắt buộc, không phải tối ưu:
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

**Chốt: có Outbox.** Khi một submission đạt một trong bảy trạng thái cuối, `judge-orchestration`:

1. Trong **cùng giao dịch DB** cập nhật `submissions.status`, ghi một dòng vào `outbox_events` (payload
   JSON, loại sự kiện, thời điểm tạo, `published_at = NULL`).
2. Một relay riêng (component `@Scheduled`, không nằm trong luồng request/giao dịch chấm bài) đọc các
   dòng `published_at IS NULL` theo thứ tự, publish lên RabbitMQ (exchange sự kiện domain), chỉ set
   `published_at` **sau khi** broker xác nhận (ack) — nếu publish lỗi, dòng ở lại hàng đợi outbox để lượt
   quét sau thử lại.

Hai sự kiện phát ra, khớp tên đã dùng ở `02-bd/architecture/identity.md` mục 3.1:

| Sự kiện | Khi nào | Payload chính | Ai tiêu thụ |
| :--- | :--- | :--- | :--- |
| `SubmissionAccepted` | `status → ACCEPTED` | `submissionId`, `userId`, `problemId`, `language` | `ai-review` (kích hoạt F5.1/F5.2), `identity` (`identity_recent_activity`) |
| `SubmissionGraded` | Mọi trạng thái cuối (bao gồm cả `ACCEPTED`) | `submissionId`, `userId`, `problemId`, `verdict`, `passedCount`, `totalCount`, tỉ lệ F4-13 | `identity` (`user_problem_best_score`, `user_submission_stats`) |

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
lớn đã thuộc về `problem-bank` (`02-bd/storage/problem-bank.md`). `judge-orchestration` chỉ cần lưu:

- `source_code` của bài nộp — TEXT trong Postgres (mã nguồn một bài giải hiếm khi vượt vài chục KB, không
  cần object storage).
- `stderr` thực tế của một lần chạy (nếu `language_configs.return_stderr_to_student = true`) — cắt ngắn
  theo giới hạn ký tự cố định (đề xuất 4KB `[SoT: Suy luận]`) lưu trực tiếp trong
  `submission_testcase_results.stderr_snippet`, không cần MinIO vì đây là output lỗi ngắn để hiển thị,
  không phải dữ liệu testcase lớn.

**Kết luận: không tạo `02-bd/storage/judge-orchestration.md`** — đúng quy tắc `bd-generation` "không phải
module nào cũng cần cả 4 file, không viết file rỗng".

## 8. Chỉ số "Beats" (F4-12) — công thức đã chốt ở RD, đóng câu hỏi

**Đã xác nhận 2026-09-13 — không cần hỏi lại chủ dự án.** RD chốt công thức này từ trước (không phải BD
tự suy diễn): `01-rd/req/judge-orchestration.md` F4-12, bổ sung 2026-08-25 qua việc đóng Câu hỏi mở Q4 của
`01-rd/screens/users/submission_result.md` — trích nguyên văn: "tính tỉ lệ phần trăm bài nộp khác **nhanh
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

## 10. Tham chiếu

- `01-rd/req/judge-orchestration.md` — đặc tả F4-01 → F4-13.
- `.nexa/domain-registry.json` — domain `judge-orchestration`, scope liệt kê bảng dữ liệu dự kiến.
- `.nexa/control/dependency-map.md` mục 2-4 — hạ tầng, luồng đồng bộ go-judge, thứ tự build (stage 2 cùng
  `harness`).
- `.nexa/control/decision-registry.md` — `DEC-2026-0823-go-judge-default-engine`,
  `DEC-2026-0831-partial-score-testcase-ratio`, `DEC-2026-0828-remove-rejudge-scope`,
  `DEC-2026-0831-judge-orchestration-ops-details`, `DEC-2026-0912-judge-callback-contract`,
  `DEC-2026-0912-judge-outbox-pattern`.
- `02-bd/architecture/harness.md` mục 5 — định dạng gói tin adapter mà harness đóng ra.
- `02-bd/architecture/problem-bank.md` mục 3.2 — câu hỏi outbound port F2-14 đối xứng với mục 3.2 file này.
- `02-bd/database/problem-bank.md` mục 1.6 — versioning testcase, sự kiện `TestcaseSetVersionBumped`.
- `02-bd/architecture/identity.md` mục 3.1 — tên sự kiện `SubmissionGraded`/`SubmissionAccepted` đã được
  phía `identity` dự kiến trước, file này xác nhận khớp tên.
- `06-plan/260912-1055-bd-rollout-order.md` — thứ tự triển khai BD.
