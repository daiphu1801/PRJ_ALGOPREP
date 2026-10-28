# Test case QA — F4: giới hạn Run Code

> Trạng thái: đã review thiết kế; **chưa thực thi**. Các ca kiểm hành vi API và tác động phụ; frontend mock không chứng minh được Redis/semaphore/go-judge.

## Phạm vi và fixture

- TDD: `04-tdd/modules/F4_judge-orchestration.md` AC-10, AC-12, AC-13, AC-73.
- DD: `03-dd/api/F4_judge-orchestration.md` mục 5; `03-dd/logic/F4_judge-orchestration.md` BR-JD-02; `03-dd/api/F2_problem-bank.md` mục 8 cho Sample.
- `P-RUN`: bài Standard I/O đang xuất bản, Python bật, có 2 Sample đã duyệt (`1 → 2`, `2 → 3`) và 6 Hidden; học viên `A1-RUN` và `B1-RUN` là hai tài khoản khác nhau. Request hợp lệ dùng `language=PYTHON`, `submissionModel=STANDARD_IO`, `sourceCode="n = int(input())\nprint(n + 1)\n"`.
- Thời gian kiểm thử được điều khiển theo UTC. `T0 = 2026-10-26T03:00:00Z`; trừ TC-F4-AC73-02 có dữ liệu thời gian riêng, các ca cần 59 hoặc 60 mốc thì seed từng request ID khác nhau tại `T0 - 30 phút`. Seed bằng request thật hoặc fixture Redis theo đúng cấu trúc của BR-JD-02, ghi cách seed trong run record. Semaphore toàn cục có 8 suất. Mỗi ca dùng namespace Redis và bài fixture cô lập.

## Ma trận nhánh

| AC | Nhánh | Test case | Review | Thực thi |
| :--- | :--- | :--- | :--- | :--- |
| F4 AC-73 | Lượt 60 nhận, 61 bị chặn | TC-F4-AC73-01 | Đã review | Chưa thực hiện |
| F4 AC-73 | Hết cửa sổ trượt | TC-F4-AC73-02 | Đã review | Chưa thực hiện |
| F4 AC-73 | Hạn mức riêng theo tài khoản | TC-F4-AC73-03 | Đã review | Chưa thực hiện |
| F4 AC-73 | Hai request tranh suất cuối | TC-F4-AC73-04 | Đã review | Chưa thực hiện |
| F4 AC-13, AC-73 | Trần đồng thời 8, hoàn token khi bận | TC-F4-AC73-05 | Đã review | Chưa thực hiện |
| F4 AC-12, AC-73 | go-judge lỗi, hoàn token | TC-F4-AC73-06 | Đã review | Chưa thực hiện |
| F4 AC-73 | Lỗi biên dịch của học viên vẫn tính lượt | TC-F4-AC73-07 | Đã review | Chưa thực hiện |
| F4 AC-10, AC-73 | Redis lỗi: suy giảm còn trần đồng thời | TC-F4-AC73-08 | Đã review | Chưa thực hiện |
| F4 AC-10 | Ba Sample chạy đồng bộ, Hidden không chạy/ghi | TC-F4-AC10-01 | Đã review | Chưa thực hiện |
| F4 AC-12 | Timeout giữ nháp trên FE, thử lại được | TC-F4-AC12-01 | Đã review | Chưa thực hiện |

## Test case

### TC-F4-AC73-01 — Lượt thứ 60 được nhận, lượt 61 bị chặn

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-73; DD F4 mục 5.4–5.5, BR-JD-02 bước 1 |
| Kind / priority | Boundary, P0: không vượt hạn mức cá nhân |
| Environment | API F4 + Redis + semaphore + go-judge/F3/F2 hoạt động |
| Preconditions | `A1-RUN` có 59 mốc đã nhận trong 60 phút trước `T0`, còn ít nhất một suất đồng thời |
| Test data | Request hợp lệ của `P-RUN` tại `T0` và một request y hệt ngay sau đó |
| Steps | Gọi Run Code lần 60, lưu kết quả; gọi tiếp lần 61, kiểm Redis và số lần gọi compile/run. |
| Expected | Lần 60 được nhận, trả 200 với `outcome=COMPLETED`, 2/2 Sample `AC`. Lần 61 trả `429 JDG-107`, `details.retryAfterSeconds` là số giây tới mốc cũ nhất hết hạn (khoảng 1.800 giây khi gọi ngay tại `T0`); không gọi compile/run cho lần bị chặn. Redis có đúng 60 mốc, không có dòng `submissions` hoặc sự kiện STOMP/domain do hai yêu cầu này. |
| Cleanup | Xoá mốc Redis fixture và bài/tài khoản cô lập |
| Evidence | Hai response, mốc Redis, số lần gọi engine, số dòng DB và bản tin/event |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC73-02 — Cửa sổ 60 phút trượt giải phóng lượt cũ nhất

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-73; DD F4 mục 5.4–5.5, BR-JD-02 bước 1 |
| Kind / priority | Boundary, P0: không khoá quá hạn hoặc mở sớm |
| Environment | API F4 + đồng hồ điều khiển được + Redis |
| Preconditions | `A1-RUN` có đúng 60 mốc: mốc đầu tại `T0`, 59 mốc còn lại tại `T0 + 30 phút` |
| Test data | Request hợp lệ tại `T0 + 59 phút 59 giây`; request tiếp tại `T0 + 60 phút + 2 giây` |
| Steps | Gọi lần đầu rồi lần sau; quan sát mốc Redis và kết quả. |
| Expected | Lần ở phút 59:59 trả `429 JDG-107`; lần sau khi mốc cũ đã ra khỏi cửa sổ được nhận và có thể chạy Sample. Chỉ các mốc cũ hơn 60 phút bị dọn; không reset toàn bộ hạn mức vào đầu giờ đồng hồ. |
| Cleanup | Dọn mốc Redis fixture |
| Evidence | Đồng hồ test, hai response và mốc Redis trước/sau |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC73-03 — Tài khoản khác không bị chia sẻ hạn mức

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-73; DD F4 mục 5.5 (`judge:run-sample:rate:<userId>`) |
| Kind / priority | Security, P1: hạn mức lấy từ JWT, không từ tham số người gọi |
| Environment | API F4 + Redis |
| Preconditions | `A1-RUN` có 60 mốc còn hạn; `B1-RUN` có 0 mốc; còn suất đồng thời |
| Test data | Cùng request hợp lệ `P-RUN`, hai JWT khác nhau |
| Steps | A gọi Run Code, sau đó B gọi cùng request. |
| Expected | A nhận `429 JDG-107`; B nhận kết quả Run Code bình thường, Redis của B tăng đúng một mốc, A vẫn 60. Không có cách gửi `userId` trong request để dùng hạn mức của B. |
| Cleanup | Dọn hai namespace Redis và fixture |
| Evidence | JWT actor đã ẩn token, hai response và số mốc từng tài khoản |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC73-04 — Hai Run Code đồng thời tranh suất hạn mức cuối

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-73; DD F4 mục 5.5, BR-JD-02 bước 1 (Lua nguyên tử) |
| Kind / priority | Concurrency, P0: không nhận quá 60 lượt khi nhiều request song song |
| Environment | API F4 nhiều luồng + Redis thật + đủ suất semaphore |
| Preconditions | `A1-RUN` có 59 mốc còn hạn |
| Test data | Hai request hợp lệ cùng JWT, phát đồng thời tại `T0` với request ID khác nhau |
| Steps | Dùng barrier cho hai lời gọi vào kiểm hạn mức cùng thời điểm; chờ cả hai response và đọc Redis. |
| Expected | Chính xác một yêu cầu được nhận và một yêu cầu trả `429 JDG-107`; Redis kết thúc với đúng 60 mốc, engine chỉ chạy cho yêu cầu được nhận. Không có hai kết quả Run Code thành công nhờ race. |
| Cleanup | Dọn namespace Redis fixture |
| Evidence | Hai response, dấu thời điểm/barrier, Redis và số lần gọi engine |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC73-05 — Lượt thứ 9 đồng thời bị từ chối nhưng không tốn hạn mức

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-13, AC-73; DD F4 mục 5.4–5.5, BR-JD-02 bước 1 |
| Kind / priority | Concurrency, P0: Run Code không chiếm tài nguyên của bài nộp thật |
| Environment | API F4 + Redis + semaphore 8 suất + RabbitMQ/worker chấm |
| Preconditions | 8 Run Code của 8 tài khoản khác đang giữ cả 8 suất; `A1-RUN` có 59 mốc; bài nộp thật hợp lệ có thể gửi |
| Test data | Request Run Code thứ 9 của A; một `CreateSubmission` hợp lệ cùng lúc |
| Steps | Giữ 8 lượt ở pha engine; gọi Run Code thứ 9 và nộp bài thật; cho một lượt Run Code hoàn tất rồi A thử lại. |
| Expected | Lượt thứ 9 trả `429 JDG-502` ngay, không xếp hàng, mốc Redis vừa thêm của A được xoá nên A vẫn 59; bài nộp thật được nhận và chấm theo hàng đợi riêng. Sau khi có suất, A chạy lại được và mốc lên 60. |
| Cleanup | Giải phóng tất cả suất, dọn Redis và fixture |
| Evidence | Số suất đang dùng, ba response, Redis và trạng thái submission thật |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC73-06 — go-judge lỗi hạ tầng được hoàn lượt

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-12, AC-73; DD F4 mục 5.4–5.5, BR-JD-02 bước 6–8 |
| Kind / priority | Abnormal/Recovery, P0: lỗi hệ thống không lấy mất cơ hội chạy thử |
| Environment | API F4 + Redis + go-judge fault injection |
| Preconditions | `A1-RUN` có 59 mốc; còn suất semaphore |
| Test data | Request hợp lệ `P-RUN`; go-judge không phản hồi sau chính sách retry hoặc hết hạn tổng 60 giây |
| Steps | Tiêm lỗi go-judge; gọi Run Code; đọc Redis và DB; khôi phục go-judge, gọi lại cùng request. |
| Expected | Lần lỗi trả `503 JDG-501`, hoàn mốc theo request ID để Redis còn 59, trả lại suất semaphore; không tạo `submissions`/event. Lần gọi sau được nhận và nếu engine phục hồi thì trả kết quả Sample, Redis lên 60. |
| Cleanup | Gỡ fault injection, dọn Redis/fixture |
| Evidence | Hai response, Redis, semaphore, số dòng DB và log retry |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC73-07 — Lỗi biên dịch do mã học viên vẫn tính một lượt

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-73; DD F4 mục 5.5 bước 5, 8 và BR-JD-02 bước 7 |
| Kind / priority | Abnormal, P1: phân biệt lỗi người dùng với lỗi hạ tầng khi trừ hạn mức |
| Environment | API F4 + Redis + compiler thật |
| Preconditions | `A1-RUN` có 59 mốc; còn suất đồng thời |
| Test data | `sourceCode="n = int(input()\nprint(n + 1)\n"` (Python sai cú pháp) |
| Steps | Gọi Run Code bằng mã sai, rồi gọi lại bằng mã đúng. |
| Expected | Lần đầu trả 200 `outcome=COMPILE_ERROR`, `results=[]`, mốc Redis lên 60; lần sau trả `429 JDG-107`. Không có dòng `submissions` hoặc kết quả testcase cho lần lỗi biên dịch. |
| Cleanup | Dọn Redis/fixture |
| Evidence | Hai response, mốc Redis, DB và dấu compile |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC73-08 — Redis lỗi thì còn trần đồng thời và không ghi bài nộp

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-10, AC-73; DD F4 mục 5.5 bước 3, 7, BR-JD-02 bước 1 |
| Kind / priority | Recovery, P1: suy giảm có kiểm soát khi kho hạn mức lỗi |
| Environment | API F4 + Redis fault injection + semaphore + go-judge |
| Preconditions | Redis không đáp ứng ở bước kiểm hạn mức; semaphore còn một suất; `P-RUN` có 2 Sample và 6 Hidden |
| Test data | Request hợp lệ của `A1-RUN` |
| Steps | Ngắt Redis, gọi Run Code; đọc phản hồi, số lần chạy testcase và DB/event; khôi phục Redis. |
| Expected | Ghi WARN vận hành, Run Code vẫn được nhận dưới trần đồng thời; chỉ 2 Sample được chạy/trả, không chạy Hidden. Không tạo `submissions`, `submission_testcase_results`, `judge_run_tokens`, outbox hoặc STOMP. Khi Redis khôi phục, hạn mức tiếp tục dựa trên các mốc còn lưu; không ghi một mốc giả cho lần đã chạy lúc Redis lỗi. |
| Cleanup | Khôi phục Redis, dọn fixture và bảo đảm semaphore được trả |
| Evidence | API response, log WARN, số lần gọi engine theo testcase, DB/event và trạng thái semaphore |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC10-01 — Chạy thử ba Sample, không chạy Hidden hoặc ghi bài nộp

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-10; DD F4 API §5.1–5.5, BR-JD-02 |
| Kind / priority | Normal + security, P0: Run Code không chạm bộ Hidden/lịch sử |
| Environment | API F4 + F2/F3 + go-judge, PostgreSQL, Redis, DB/event spies |
| Preconditions | Bản sao `P-RUN` có đúng ba Sample đã duyệt S1 `1→2`, S2 `2→3`, S3 `4→5` ở position 1–3; thêm 12 Hidden H1–H12 có input/output marker `QA_HIDDEN_01`..`QA_HIDDEN_12`; A1 chưa có submission, Redis còn suất |
| Test data | `POST /problems/{P-RUN}/run-sample` bằng A1 với `language=PYTHON`, `submissionModel=STANDARD_IO`, `sourceCode="n = int(input())\nprint(n + 1)\n"` |
| Steps | Gửi một request, chờ response cùng lời gọi; ghi testcase IDs F2 cấp cho engine, response và delta DB/event/tiến độ trước–sau. |
| Expected | `200 outcome=COMPLETED`, `compileErrorMessage=null`, `passedCount=3`, `totalCount=3`; `results[]` đúng S1/S2/S3 theo position với `order=1..3`, mỗi dòng `verdict=AC`, `actualOutput` sau chuẩn hoá lần lượt `"2"`, `"3"`, `"5"`, `runtimeMs`/`memoryKb` là số không âm. Engine chỉ chạy 3 Sample, không đọc/chạy/nhắc tới H1–H12 hoặc marker Hidden. Không tạo `submissions`, `submission_testcase_results`, outbox/STOMP hay thay số liệu tiến độ/tỉ lệ AC. |
| Cleanup | Dọn fixture và Redis mốc chạy thử |
| Evidence | Request/response, trace ID testcase F2/engine, DB/Outbox/STOMP và chỉ số tiến độ trước–sau |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC12-01 — Timeout Run Code giữ mã đang soạn và cho thử lại

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-12; DD F4 API §5.4–5.5 `JDG-501`; DD màn USR0102 lỗi Chạy thử |
| Kind / priority | Recovery, P0: lỗi engine không làm mất nháp học viên |
| Environment | FE USR0102 + API F4/F2/F3, Redis, go-judge fault injection |
| Preconditions | A1 mở `P-RUN`, mã trong editor chưa nộp là `n = int(input())\nprint(n + 1)\n`; Redis A1 có 0 mốc, còn suất đồng thời; go-judge không phản hồi trong hạn tổng 60 giây |
| Test data | A1 bấm Chạy thử với request hợp lệ `P-RUN`; sau response lỗi gỡ fault injection và bấm lại với nguyên sourceCode |
| Steps | Chụp nội dung editor; bấm Chạy thử và đo từ request tới thông báo; kiểm Redis/DB; phục hồi go-judge, bấm lại, so nội dung editor và response. |
| Expected | Lần lỗi trong tối đa 60 giây trả `503 JDG-501`/thông báo lỗi hệ thống; editor vẫn giữ chính xác mã ban đầu, nút Chạy thử dùng lại được. Redis hoàn mốc, không có submission/result/outbox/STOMP. Lần sau `200 outcome=COMPLETED` với 2 Sample của `P-RUN`; editor không tự xoá hoặc đổi nội dung. |
| Cleanup | Gỡ fault injection, dọn Redis/fixture |
| Evidence | Hai HTTP response, thời gian đo, ảnh/editor state, Redis/DB/event delta và trace engine |
| Design review | Đã review; chưa thực thi |

## Ranh giới thực thi

Ca đồng thời cần Redis và semaphore thật; stub của cả hai không kiểm được race hoặc hoàn mốc. Không báo Đạt/Trượt từ frontend mock. Các ca lỗi hạ tầng chỉ chạy trong môi trường cô lập, có bước khôi phục Redis/go-judge sau test.
