# Test case QA — F4: lỗi hàng đợi, máy chấm và sweep bài treo

> Trạng thái: 8 ca đã review thiết kế; chưa thực thi. Các ca cần bản tích hợp F4, PostgreSQL, RabbitMQ và fault injection có kiểm soát.

## Phạm vi và fixture

- TDD: `04-tdd/modules/F4_judge-orchestration.md` AC-08, AC-22–24; DD F4 API §6, logic BR-JD-03–08 và job JOB-JD-01.
- `P-RES`: bài Standard I/O đã `PUBLISHED`, snapshot v1/checksum hợp lệ với 12 testcase đã duyệt, Python bật, giới hạn chưa chạm. `A1-RES` có JWT học viên. Mã đúng `n = int(input())\nprint(n + 1)\n`; request POST §6 gồm `language=PYTHON`, `submissionModel=STANDARD_IO`, `sourceInputMethod=EDITOR` và mã này.
- `T0=2026-10-26T03:00:00Z`; clock DB được điều khiển ở ca sweep. Mỗi ca tạo bài nộp riêng và ghi trạng thái/Outbox/STOMP trước–sau. Khi fault injection RabbitMQ/go-judge/scheduler, luôn khôi phục dịch vụ kể cả ca trượt.

## Ma trận nhánh

| AC | Nhánh | Test case | Review | Thực thi |
| :--- | :--- | :--- | :--- | :--- |
| F4 AC-08 | RabbitMQ không confirm sau ba lần | TC-F4-AC08-01 | Đã review | Chưa thực hiện |
| F4 AC-08 | Mất confirm nhưng worker đã nhận | TC-F4-AC08-02 | Đã review | Chưa thực hiện |
| F4 AC-22 | go-judge lỗi hạ tầng hết retry | TC-F4-AC22-01 | Đã review | Chưa thực hiện |
| F4 AC-22 | TLE nghiệp vụ không retry | TC-F4-AC22-02 | Đã review | Chưa thực hiện |
| F4 AC-23 | PENDING/JUDGING đứng im quá 5 phút | TC-F4-AC23-01 | Đã review | Chưa thực hiện |
| F4 AC-23 | PENDING lúc pause được bỏ qua | TC-F4-AC23-02 | Đã review | Chưa thực hiện |
| F4 AC-23 | Resume làm mới mốc sweep | TC-F4-AC23-03 | Đã review | Chưa thực hiện |
| F4 AC-24 | 45 testcase chấm dài có tiến độ | TC-F4-AC24-01 | Đã review | Chưa thực hiện |

## Test case

### TC-F4-AC08-01 — Broker không xác nhận: kết thúc ngay thay vì kẹt PENDING

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-08; DD F4 API §6.4–6.5 T-CS-08, JOB-JD-01 |
| Kind / priority | Recovery, P0: nộp bài không được treo khi publish thất bại |
| Environment | API F4 + PostgreSQL + RabbitMQ publisher confirm fault + Outbox/STOMP |
| Preconditions | `P-RES` hợp lệ; broker không xác nhận bất kỳ lần publish nào, worker không nhận message |
| Test data | Một request nộp bài của `A1-RES`; publisher confirm timeout 3 giây/lần, tối đa 3 lần gọi với chờ 200 ms và 500 ms giữa các lần |
| Steps | Bật fault confirm, POST §6, ghi thời điểm và `details.submissionId`; đọc DB, Outbox, STOMP và lịch sử bài nộp ngay sau response. |
| Expected | API trả `503 JDG-503` kèm `details.submissionId`; đúng ba lần publish, không đợi sweep 5 phút. Dòng đã tạo kết thúc `SYSTEM_ERROR/QUEUE_UNAVAILABLE`, có `finished_at`, `score_ratio=NULL`; đúng một `SubmissionGraded`, không có `SubmissionAccepted`, một bản tin cuối sau commit; ID xuất hiện trong lịch sử của chủ bài. Mã nguồn đã nộp nằm trong DB để xem lại, không đưa vào message RabbitMQ. Bài nộp khác không đổi trạng thái. |
| Cleanup | Tắt fault, dọn submission/message/Outbox fixture |
| Evidence | Request/lỗi API, confirm attempts/timing, DB, lịch sử, Outbox và STOMP |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC08-02 — Mất confirm không ghi đè worker đã nhận message

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-08; DD F4 API §6.5 bước 7–8 T-CS-10 |
| Kind / priority | Concurrency, P0: tránh biến lượt đang chấm thành lỗi hàng đợi |
| Environment | API F4 + RabbitMQ + worker + PostgreSQL, barrier trước UPDATE `PENDING→SYSTEM_ERROR` |
| Preconditions | `P-RES` hợp lệ; broker nhận message nhưng không gửi confirm; worker đọc message và có thể chuyển submission sang `COMPILING` trước khi API hết ba lần chờ |
| Test data | Một POST §6; worker chuyển `PENDING→COMPILING` tại barrier, rồi API thử cập nhật lỗi queue |
| Steps | Gọi POST với confirm bị mất; cho worker commit `COMPILING`, thả API tiếp tục; đọc response, DB, Outbox/STOMP, chờ worker hoàn tất. |
| Expected | UPDATE có điều kiện `status=PENDING` ảnh hưởng 0 dòng; API trả `201 SubmissionTicketDto` với `submissionId`, `status=PENDING` theo hợp đồng ticket tạo bài, không trả `JDG-503`. GET progress sau response đọc trạng thái hiện hành `COMPILING` hoặc mới hơn từ DB. Worker chấm tiếp đến trạng thái cuối thật; không có `QUEUE_UNAVAILABLE`, không có `SubmissionGraded` giả hoặc hai trạng thái cuối. |
| Cleanup | Tắt fault/barrier, dọn fixture |
| Evidence | Publish/confirm timeline, worker status commits, response, DB và Outbox |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC22-01 — Lỗi go-judge ba lần đưa bài về SANDBOX_FAULT

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-22; DD F4 BR-JD-04, BR-JD-07, BR-JD-15 |
| Kind / priority | Recovery, P0: lỗi hạ tầng không bị gán cho học viên |
| Environment | F4 worker + go-judge adapter fault + DB/Outbox + API queue/infra-events |
| Preconditions | `P-RES` có 12 ca; vị trí 1 và 2 đã commit `AC`, vị trí 3 chưa có kết quả; worker đang `JUDGING` |
| Test data | Cả ba lần gọi `run` cho vị trí 3 đều trả HTTP 5xx hạ tầng; token của vị trí 3 giữ nguyên qua retry |
| Steps | Bật fault, cho worker chạy vị trí 3; ghi số lần gọi, khoảng chờ, token; đọc DB/API, tab queue `failed` và sự kiện hạ tầng. |
| Expected | Đúng ba lần gọi cùng token, chờ khoảng 500 ms rồi 1 giây; không thử lại vô hạn. Submission kết thúc `SYSTEM_ERROR/SANDBOX_FAULT`, `score_ratio=NULL`; hai dòng AC đầu vẫn nguyên, không có dòng kết quả vị trí 3; một `SubmissionGraded`, không có `SubmissionAccepted`. Tab lỗi quản trị và infra-events nhận diện bài lỗi; người học thấy lỗi hệ thống, không phải WA/TLE, không lộ chi tiết hạ tầng. |
| Cleanup | Tắt fault, dọn fixture |
| Evidence | Adapter call/token/timing, DB, Outbox, hai API quản trị và response học viên |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC22-02 — TLE là verdict nghiệp vụ, không retry như lỗi hạ tầng

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-22; DD F4 BR-JD-04 bước 7, BR-JD-07 bảng phân loại |
| Kind / priority | Boundary, P1: không biến TLE thành SANDBOX_FAULT |
| Environment | F4 worker + go-judge adapter điều khiển verdict + DB |
| Preconditions | `P-RES` v1, worker bắt đầu chấm 12 ca |
| Test data | Vị trí 1 trả engine timeout do CPU limit (`TLE`), 11 vị trí khác `AC` |
| Steps | Chấm hết bài; đếm lượt run từng vị trí, đọc DB và Outbox. |
| Expected | Vị trí 1 chỉ gọi run một lần và lưu `TLE`; 11 vị trí sau vẫn chạy, tổng 12 dòng. Submission `TIME_LIMIT_EXCEEDED`, `passedCount=11`, `totalCount=12`, `scoreRatio=0.917`; không có `SANDBOX_FAULT` hay retry hạ tầng. |
| Cleanup | Dọn fixture |
| Evidence | Engine verdict/call count, 12 dòng DB, submission/Outbox |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC23-01 — Sweep kết thúc PENDING và JUDGING đã đứng im quá ngưỡng

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-23; DD JOB-JD-01 mục 1, BR-JD-05 biến thể SYSTEM_ERROR |
| Kind / priority | Recovery/Boundary, P0: không để bài treo vô hạn |
| Environment | JOB-JD-01 + PostgreSQL + Outbox, clock DB `T0` |
| Preconditions | `queue_paused=false`; J1 `PENDING`, J2 `JUDGING`, cùng `updated_at=T0-5 phút-1 giây`; J3 `PENDING` có `updated_at=T0-5 phút` đúng ngưỡng; không worker nào đang xử lý J1/J2 |
| Test data | Một chu kỳ sweep tại T0, lặp chu kỳ ngay với cùng dữ liệu |
| Steps | Chạy sweep, đọc ba dòng DB/Outbox/STOMP; chạy lại và so số sự kiện. |
| Expected | J1/J2 chuyển `SYSTEM_ERROR/WORKER_TIMEOUT`, `finished_at` có giá trị và `score_ratio=NULL`; J3 giữ `PENDING` vì điều kiện nhỏ hơn nghiêm ngặt. Mỗi J1/J2 có đúng một `SubmissionGraded` và một bản tin cuối sau commit, không có `SubmissionAccepted`; lần sweep lặp không phát thêm sự kiện. |
| Cleanup | Dọn ba submission và Outbox fixture |
| Evidence | DB timestamp, hai lượt sweep, Outbox/STOMP |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC23-02 — Sweep bỏ qua PENDING khi hàng đợi tạm dừng

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-23; DD JOB-JD-01 mục 1, BR-JD-08 |
| Kind / priority | Boundary, P0: pause do admin không được coi là worker lỗi |
| Environment | API queue + JOB-JD-01 + PostgreSQL, clock DB `T0` |
| Preconditions | `queue_paused=true`; J1 và J2 `PENDING`, `updated_at=T0-6 phút`; J3 `JUDGING`, `updated_at=T0-6 phút` |
| Test data | Một lượt sweep trong khi pause vẫn bật |
| Steps | Chạy sweep, đọc ba status và Outbox. |
| Expected | J1/J2 vẫn `PENDING`, không có `WORKER_TIMEOUT` hay Outbox cho hai bài này; J3 là job đang chấm bị bỏ rơi nên vẫn chuyển `SYSTEM_ERROR/WORKER_TIMEOUT` và có một `SubmissionGraded`. |
| Cleanup | Resume queue, dọn fixture |
| Evidence | Cờ pause, DB trước/sau, Outbox và log sweep |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC23-03 — Resume đặt lại đồng hồ của bài PENDING

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-23; DD F4 API §19.5 bước 4, JOB-JD-01 mục 1 |
| Kind / priority | Recovery, P0: bài chờ lâu trong pause có đủ 5 phút sau khi mở |
| Environment | API queue + PostgreSQL + JOB-JD-01, clock DB điều khiển được |
| Preconditions | `queue_paused=true`; J1 `PENDING` từ `T0-10 phút`, `updated_at=T0-10 phút`; consumer chưa nhận J1 |
| Test data | ADMIN PUT `{paused:false,expectedPaused:true}` tại T0; sweep tại `T0+4 phút 59 giây` và `T0+5 phút 1 giây`, giữ worker chưa nhận J1 trong môi trường kiểm thử |
| Steps | Resume, đọc `updated_at` J1; chạy hai lượt sweep tại mốc nêu trên, đọc status/Outbox. |
| Expected | Resume cập nhật `updated_at` J1 gần T0 trong cùng giao dịch đổi cờ; lượt sweep trước mốc 5 phút giữ `PENDING`, lượt sau chuyển `SYSTEM_ERROR/WORKER_TIMEOUT` nếu vẫn không có worker nhận. Không dùng `updated_at` cũ từ lúc pause. |
| Cleanup | Khôi phục queue/worker, dọn fixture |
| Evidence | PUT response, DB timestamp, hai sweep và Outbox |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC24-01 — Bài chấm hơn 5 phút vẫn sống nhờ cập nhật mỗi testcase

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-24; DD F4 BR-JD-04 bước 9–10, JOB-JD-01 |
| Kind / priority | Concurrency/Boundary, P0: sweep không đánh lỗi bài dài hợp lệ |
| Environment | Worker + JOB-JD-01 + PostgreSQL, clock kiểm thử có thể tiến theo từng ca |
| Preconditions | Bài fixture có 45 testcase đã duyệt (2 Sample, 43 Hidden), snapshot hợp lệ; worker xử lý mỗi testcase khoảng 8 giây, tổng khoảng 360 giây; sweep chạy mỗi 60 giây |
| Test data | 45 verdict `AC`; sau mỗi ca worker commit kết quả và `submissions.updated_at` |
| Steps | Nộp, chạy worker/sweep qua ít nhất 6 phút; sau từng testcase ghi `updated_at`, sau từng sweep đọc status; đợi bài kết thúc. |
| Expected | Dù tổng thời gian >5 phút, mỗi lần sweep đều thấy mốc cập nhật mới và không đặt `WORKER_TIMEOUT`. Có đúng 45 dòng AC, status cuối `ACCEPTED`, `scoreRatio=1.000`; không có Outbox `SubmissionGraded(SYSTEM_ERROR)`. |
| Cleanup | Dọn 45 testcase, submission và job fixture |
| Evidence | Timeline 45 commits và sweep, DB status/updated_at, Outbox |
| Design review | Đã review; chưa thực thi |
