# Test case QA — F4: sự kiện chấm và Outbox

> Trạng thái: 8 ca đã review thiết kế; chưa thực thi. Nguồn: `04-tdd/modules/F4_judge-orchestration.md` AC-63–65; DD F4 API §25; logic BR-JD-05, BR-JD-14; jobs JOB-JD-02/03; DD F1 API sự kiện và F2 BR-20.

## Fixture chung

- `A-EVT` là học viên, `P-EVT` là bài đã xuất bản với 10 testcase đã duyệt; Python bật. Nộp và chấm trong môi trường F4 + PostgreSQL + RabbitMQ + consumer F1/F2/F5. Các bài nộp dùng UUID khác nhau và mốc `submitted_at` cố định; `finished_at` do clock DB điều khiển. Ghi snapshot DB/Outbox và số liệu consumer trước–sau. Mỗi ca cô lập dữ liệu để không lẫn lượt cũ của cùng `(userId, problemId)`.
- Outbox `judge.outbox_events`: `id=eventId`, `aggregate_type=Submission`, `aggregate_id=submissionId`, `published_at=NULL`, `publish_attempts=0` lúc mới ghi. Phong bì `schemaVersion=1`; routing key `judge.submission.graded` hoặc `judge.submission.accepted`, exchange `algoprep.domain-events`. Payload không chứa source code, input/output testcase hay stderr.
- Khi fault broker/relay/clock được dùng, khôi phục kể cả ca trượt. Đối chiếu bất biến trạng thái cuối: không có hai lần `SubmissionGraded` cho cùng bài nộp trong Outbox, không ghi lại trạng thái cuối. Chỉ dùng dữ liệu đã biết ở DD để xác nhận, không dựa vào thứ tự giữa hai event khác loại.

| AC | Nhánh | Test case | Review | Thực thi |
| :--- | :--- | :--- | :--- | :--- |
| AC-63 | Lượt đầu sai, lượt sau Accepted, lỗi biên dịch | TC-F4-AC63-01 đến -03 | Đã review | Chưa thực hiện |
| AC-64 | Broker tắt/phục hồi, nhiều relay, retry nhiều lần, dọn Outbox | TC-F4-AC64-01 đến -04 | Đã review | Chưa thực hiện |
| AC-65 | Consumer nhận lặp eventId | TC-F4-AC65-01 | Đã review | Chưa thực hiện |

## Test case

### TC-F4-AC63-01 — Lượt đầu Wrong Answer có đầy đủ SubmissionGraded

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-63; DD F4 API §25.1–25.2, logic BR-JD-14 |
| Kind / priority | Normal, P0: payload đúng nuôi thống kê |
| Environment | F4 worker, PostgreSQL, Outbox, RabbitMQ, F1/F2 consumer |
| Preconditions | `A-EVT` chưa từng nộp `P-EVT`; clock nộp `2026-10-26T16:59:00Z`, chấm xong `2026-10-26T17:01:00Z` |
| Test data | Bài nộp Python có 7/10 testcase đạt, verdict `WRONG_ANSWER`, ratio `0.700` |
| Steps | Nộp và chấm bài; trước relay đọc submission/Outbox; chạy relay; đối chiếu message và số liệu F1/F2. |
| Expected | Một Outbox `SubmissionGraded`, không có `SubmissionAccepted`; payload có UUID `submissionId/userId/problemId`, `language=PYTHON`, `verdict=WRONG_ANSWER`, `passedCount=7`, `totalCount=10`, `passedTestcaseRatio=0.700`, `submittedAt=2026-10-26T16:59:00Z`, `firstAttemptForProblem=true`. `occurredAt=finished_at=2026-10-26T17:01:00Z`; F1 ghi ngày nộp theo UTC+7 từ `submittedAt` (**26/10**), không từ `occurredAt` (27/10) hay thời điểm relay; F2 đếm một lượt hợp lệ. Không có mã nguồn/testcase/stderr trong event. |
| Cleanup | Xoá submission, Outbox và read model fixture |
| Evidence | DB, hai timestamp, message và F1/F2 counters |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC63-02 — Lượt thứ hai Accepted có hai event, không là lượt đầu

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-63; DD F4 API §25.2–25.3; logic BR-JD-14 |
| Kind / priority | Normal, P0: điều kiện mở AI và số liệu Accepted |
| Environment | F4 worker, PostgreSQL, Outbox, RabbitMQ, F1/F2/F5 consumer |
| Preconditions | `A-EVT/P-EVT` có một lượt Wrong Answer đã nộp trước; lượt mới được chấm `ACCEPTED` |
| Test data | Python, 10/10, ratio `1.000`; `submittedAt` của lượt mới cố định và sau lượt đầu |
| Steps | Chấm lượt thứ hai; đọc Outbox; chạy relay; kiểm F1/F2/F5. |
| Expected | Đúng một `SubmissionGraded` và một `SubmissionAccepted` với hai `eventId` khác nhau nhưng cùng `submissionId`; Graded có `passedCount=10`, `totalCount=10`, `passedTestcaseRatio=1.000`, `firstAttemptForProblem=false`. Accepted chỉ có `submissionId/userId/problemId/language`, không suy ra `classId`; F1 tăng Accepted một lần nhưng không tăng `first_try_accepted_count`, F2 tăng số Accepted một lần, F5 chỉ mở điều kiện phân tích chứ không tự chạy báo cáo. Không phát event thứ ba khi worker retry sau trạng thái cuối. |
| Cleanup | Xoá dữ liệu ca và lượt seed |
| Evidence | DB Outbox, messages, F1/F2 counters, trạng thái F5 |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC63-03 — Compile Error vẫn có Graded, các số đếm là null

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-63; DD F4 API §25.2, logic BR-JD-14 bước 4 |
| Kind / priority | Abnormal, P0: lỗi biên dịch không thành điểm 0/0 |
| Environment | F4 worker, PostgreSQL, Outbox, RabbitMQ |
| Preconditions | Bài `P-COMP` đã xuất bản; mã Python của `A-EVT` có lỗi cú pháp ở dòng 2 |
| Test data | `sourceCode="x = 1\nprint(\n"`, verdict `COMPILE_ERROR` |
| Steps | Nộp/chấm; kiểm DB/Outbox; chạy relay. |
| Expected | Một `SubmissionGraded` cho `COMPILE_ERROR`, không có `SubmissionAccepted`; payload `passedCount=null`, `totalCount=null`, `passedTestcaseRatio=null`, `submittedAt` bằng DB. Không ghi testcase result và không đưa compiler output thô vào event. |
| Cleanup | Xoá dữ liệu ca |
| Evidence | Compiler result, DB, Outbox/message |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC64-01 — Broker dừng 10 phút không cản 5 bài chấm xong

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-64; DD F4 API §25.4, logic BR-JD-14; JOB-JD-02 |
| Kind / priority | Recovery, P0: chấm tách khỏi relay |
| Environment | F4 worker, PostgreSQL, broker fault, relay, F1/F2/F5 consumer |
| Preconditions | Broker domain-events không ack trong 10 phút; queue bài nộp của worker vẫn hoạt động; năm submission độc lập đang được chấm |
| Test data | Bốn verdict `WRONG_ANSWER`, một `ACCEPTED`; mỗi submission có ID riêng |
| Steps | Chặn confirm domain-events; cho 5 bài kết thúc và mở GET kết quả; kiểm Outbox; sau 10 phút khôi phục confirm, chạy relay tới khi hết pending; kiểm consumer. |
| Expected | Cả 5 submission vào trạng thái cuối và GET kết quả đúng khi domain-events đang lỗi, không chờ broker; có 5 `SubmissionGraded` và 1 `SubmissionAccepted` pending. Sau phục hồi, cả 6 event được ack và `published_at` có giá trị; F1/F2 có đúng năm lượt nghiệp vụ, F5 mở điều kiện cho một Accepted. Không còn event pending của năm bài. |
| Cleanup | Khôi phục broker; xoá dữ liệu ca |
| Evidence | Timeline chấm/GET, DB Outbox, publisher confirm, read models |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC64-02 — Hai relay không claim cùng Outbox row trong một lượt

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-64; JOB-JD-02 `FOR UPDATE SKIP LOCKED` |
| Kind / priority | Concurrency, P1: multi-instance relay |
| Environment | Hai relay F4, PostgreSQL barrier, RabbitMQ |
| Preconditions | Một `SubmissionGraded` pending, attempts=0; relay 1 giữ lock trước publish |
| Test data | `eventId=E-JD-RACE` từ Outbox fixture |
| Steps | Cho relay 2 quét khi relay 1 giữ lock; thả relay 1 và chờ confirm; kiểm DB/message. |
| Expected | Relay 2 bỏ qua row đang khoá, không có hai publish đồng thời của cùng event; sau confirm attempts=1, `published_at` có giá trị. |
| Cleanup | Thả barrier; xoá fixture |
| Evidence | Lock trace, message timeline, Outbox row |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC64-03 — Retry đạt ngưỡng WARN/ERROR vẫn không bỏ event

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-64; JOB-JD-02 ngưỡng attempts 3/5 |
| Kind / priority | Recovery, P1: event cũ không bị bỏ sau nhiều lỗi |
| Environment | F4 relay, PostgreSQL, RabbitMQ confirm fault, log hạ tầng |
| Preconditions | Một `SubmissionGraded` pending attempts=0; broker không ack năm lượt liên tiếp |
| Test data | Cùng `eventId=E-JD-RETRY` qua sáu lần thử |
| Steps | Chạy năm lượt relay lỗi, đọc row/log sau lần 3 và 5; khôi phục broker, chạy lần 6. |
| Expected | Mỗi lượt tăng attempts đúng 1, `published_at=NULL` qua lần 5; log WARN ở attempts 3–4, ERROR từ 5. Event vẫn được thử lần 6 với cùng `eventId`, broker ack thì attempts=6 và `published_at` có giá trị; không tạo Outbox mới. |
| Cleanup | Khôi phục broker, xoá fixture |
| Evidence | DB attempts/timestamps, WARN/ERROR log, publish/confirm |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC64-04 — Job dọn chỉ xoá event đã phát quá 90 ngày

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-64 (bảo toàn sự kiện pending); JOB-JD-03 |
| Kind / priority | Boundary, P1: cleanup không làm mất event chưa tới consumer |
| Environment | PostgreSQL, clock DB, JOB-JD-03 |
| Preconditions | Clock `T0`; tắt relay; seed bốn event vào `judge.outbox_events` |
| Test data | `E-OLD` published `T0-90d-1s`; `E-EDGE` published đúng `T0-90d`; `E-NEW` published `T0-90d+1s`; `E-PENDING` `published_at=NULL`, created `T0-100d` |
| Steps | Chạy cleaner tại `T0`; kiểm bốn eventId và submissions gốc. |
| Expected | Chỉ `E-OLD` bị xoá; ba dòng còn lại nguyên vẹn, đặc biệt pending quá 90 ngày. Không sửa submissions hay kết quả chấm. |
| Cleanup | Khôi phục clock/relay; xoá seed còn lại |
| Evidence | DB trước–sau, job log |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC65-01 — Nhận trùng SubmissionGraded không tăng số liệu hai lần

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-65; DD F4 API §25.4; DD F1 API sự kiện; F2 logic BR-20 |
| Kind / priority | Concurrency, P0: consumer idempotent |
| Environment | RabbitMQ, F1/F2 consumer, PostgreSQL read models |
| Preconditions | F1/F2 read models của `A-EVT/P-EVT` ở mốc 0; một `SubmissionGraded` Accepted hợp lệ với `eventId=E-DUP` |
| Test data | Payload `verdict=ACCEPTED`, `passedCount=10`, `totalCount=10`, `passedTestcaseRatio=1.000`, `language=PYTHON`, `firstAttemptForProblem=true`, `submittedAt=2026-10-26T16:59:00Z` |
| Steps | Phát cùng phong bì `E-DUP` hai lần; chờ hai lần consumer ack; so số liệu F1/F2 và bảng dedupe. |
| Expected | F1 `total_submissions=1`, `accepted_count=1`, `first_try_accepted_count=1`, `language_counts.PYTHON=1`, ngày nộp UTC+7 có `submission_count=1`; F2 `problem_stats.submission_count=1`, Accepted count/rate tính từ một lượt. Dedupe theo `eventId`; lần 2 không ghi nghiệp vụ lần nữa. |
| Cleanup | Xoá read models/dedupe fixture |
| Evidence | Hai message, consumer ack, dedupe và DB counters |
| Design review | Đã review; chưa thực thi |
