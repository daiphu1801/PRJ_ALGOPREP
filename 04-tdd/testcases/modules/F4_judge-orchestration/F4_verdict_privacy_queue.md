# Test case QA — F4: tổng hợp verdict, bảo mật kết quả và hàng đợi

> Trạng thái: 13 ca đã review thiết kế; chưa thực thi. Stub adapter chỉ dùng để tạo chuỗi verdict quyết định; các kiểm DB/API/outbox cần bản tích hợp thật.

## Phạm vi và fixture

- TDD: `04-tdd/modules/F4_judge-orchestration.md` AC-15–21, AC-48, AC-76; DD F4 API §5, §8, §16 và logic BR-JD-03–05, BR-JD-11.
- `P-JUDGE`: bài `PUBLISHED`, Standard I/O, Python bật, snapshot bất biến v1 gồm 12 testcase đã duyệt theo `position=1..12`; S1, S2 là Sample, H1..H10 là Hidden. Mọi input/output có một dòng và newline cuối; snapshot/checksum hợp lệ. Học viên `A1-JUDGE` sở hữu submission; `A1-OTHER` không sở hữu.
- `P-JUDGE-10`: bản sao độc lập của `P-JUDGE` chỉ giữ S1, S2 và H1..H8 (10 ca), vẫn đủ ngưỡng xuất bản; dùng riêng cho AC-17.
- Stub `JudgeExecutionPort` nhận đúng 12 lượt run, trả verdict theo bảng riêng từng ca và runtime/memory cố định `runtimeMs=10+i`, `memoryKb=1000+10i` ở vị trí `i`; với `OK`, P4 match trả `MATCH` hay `NO_MATCH` theo ca. Nhờ vậy có thể kiểm điểm, trạng thái, MAX và số lần chạy mà không phụ thuộc lịch chạy của hệ điều hành. Ca bảo mật dùng thêm API thật và cấu hình sandbox thật.
- `T0=2026-10-26T03:00:00Z` cho ca hàng đợi; thời gian được cố định qua clock của môi trường kiểm thử. Dùng DB/queue namespace riêng, không dùng chung bài giữa các ca.

## Ma trận nhánh

| AC | Nhánh | Test case | Review | Thực thi |
| :--- | :--- | :--- | :--- | :--- |
| F4 AC-15 | Chạy đủ 12 ca, hai lỗi ở giữa | TC-F4-AC15-01 | Đã review | Chưa thực hiện |
| F4 AC-17 | Ba loại lỗi trong 10 ca, lấy lỗi đầu tiên | TC-F4-AC17-01 | Đã review | Chưa thực hiện |
| F4 AC-16 | 12/12 Accepted | TC-F4-AC16-01 | Đã review | Chưa thực hiện |
| F4 AC-18 | MLE gộp status TLE | TC-F4-AC18-01 | Đã review | Chưa thực hiện |
| F4 AC-19 | Lỗi biên dịch, không chạy testcase | TC-F4-AC19-01 | Đã review | Chưa thực hiện |
| F4 AC-20 | Worker chết rồi nhận lại | TC-F4-AC20-01 | Đã review | Chưa thực hiện |
| F4 AC-21 | Trạng thái cuối ghi một lần | TC-F4-AC21-01 | Đã review | Chưa thực hiện |
| F4 AC-76 | Hidden không lộ output/ID | TC-F4-AC76-01 | Đã review | Chưa thực hiện |
| F4 AC-76 | Bọc hàm không lưu/trả stderr | TC-F4-AC76-02 | Đã review | Chưa thực hiện |
| F4 AC-76 | Sample Standard I/O chỉ trả stderr khi được phép | TC-F4-AC76-03 | Đã review | Chưa thực hiện |
| F4 AC-48 | Snapshot hàng đợi dồn và tạm dừng | TC-F4-AC48-01 | Đã review | Chưa thực hiện |
| F4 AC-48 | Hàng đợi rỗng, thiếu Prometheus, thiếu quyền | TC-F4-AC48-02 | Đã review | Chưa thực hiện |
| F4 AC-48 | Độ trễ chỉ dùng job bắt đầu trong giờ gần nhất | TC-F4-AC48-03 | Đã review | Chưa thực hiện |

## Test case

### TC-F4-AC15-01 — Hai ca sai ở giữa không dừng vòng chấm 12 ca

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-15; DD F4 BR-JD-03–05, API §8 |
| Kind / priority | Normal/Boundary, P0: chạy hết để tính điểm từng phần |
| Environment | API F4 + DB + RabbitMQ + worker, adapter go-judge có thể cố định verdict |
| Preconditions | `P-JUDGE` có 12 ca; nộp mã hợp lệ, cho worker xử lý snapshot v1 |
| Test data | Theo `testcase_order`: vị trí 3 và 8=`WA`; mười vị trí còn lại=`AC`; runtime/memory theo fixture |
| Steps | Tạo submission, chờ trạng thái cuối; đọc kết quả DB/API, số lượt run adapter và Outbox. |
| Expected | Có đúng 12 lời gọi run và 12 dòng `submission_testcase_results`, kể cả vị trí 9..12 sau lỗi thứ 8. `passedCount=10`, `totalCount=12`, `scoreRatio=0.833`; status `WRONG_ANSWER`, `runtimeMs=22`, `memoryKb=1120`. Một `SubmissionGraded`, không có `SubmissionAccepted`; không có kết quả trùng. |
| Cleanup | Dọn submission, token, Outbox và fixture cô lập |
| Evidence | Snapshot v1, chuỗi verdict adapter, 12 kết quả, submission, response chi tiết, Outbox |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC17-01 — Trong 10 ca, status lấy lỗi đầu tiên và điểm vẫn tính độc lập

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-17; DD F4 BR-JD-03–05, API §8 |
| Kind / priority | Normal/Boundary, P0: điểm từng phần độc lập với status |
| Environment | API F4 + DB + RabbitMQ + worker, adapter go-judge có thể cố định verdict |
| Preconditions | `P-JUDGE-10` có 10 ca; nộp mã hợp lệ, cho worker xử lý v1 |
| Test data | Theo `testcase_order`: 2=`TLE`, 5=`WA`, 7=`RE`, bảy vị trí khác=`AC`; runtime/memory theo fixture |
| Steps | Tạo submission, chờ trạng thái cuối; đọc kết quả DB/API, số lượt run adapter và Outbox. |
| Expected | Có đúng 10 lời gọi run và 10 dòng `submission_testcase_results`, không dừng ở vị trí 2. `passedCount=7`, `totalCount=10`, `scoreRatio=0.700`; status `TIME_LIMIT_EXCEEDED` theo vị trí 2, không theo lỗi ở 5/7. `runtimeMs=20`, `memoryKb=1100` (MAX tại vị trí 10). Một `SubmissionGraded`, không có `SubmissionAccepted`; không có kết quả trùng. |
| Cleanup | Dọn submission, token, Outbox và fixture cô lập |
| Evidence | Snapshot v1 của `P-JUDGE-10`, chuỗi verdict adapter, 10 kết quả, submission, response chi tiết, Outbox |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC16-01 — Tất cả ca đạt thì Accepted và điểm bằng một

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-16; DD F4 BR-JD-05 bước 1–7, API §8 |
| Kind / priority | Normal, P0: đường hoàn tất chuẩn và sự kiện Accepted |
| Environment | API F4 + DB + RabbitMQ + worker + Outbox |
| Preconditions | `P-JUDGE` v1; `A1-JUDGE` nộp mã hợp lệ |
| Test data | 12 verdict `AC`, runtime/memory theo fixture |
| Steps | Chấm xong; đọc DB/API, số lượt run và Outbox. |
| Expected | 12/12 `AC`, `status=ACCEPTED`, `scoreRatio=1.000`, `runtimeMs=22`, `memoryKb=1120`, `finishedAt` có giá trị; đúng một `SubmissionGraded` và một `SubmissionAccepted`. |
| Cleanup | Dọn fixture cô lập |
| Evidence | Submission, 12 dòng kết quả, response chi tiết, hai sự kiện Outbox |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC18-01 — MLE đầu tiên cho status Quá thời gian nhưng dòng vẫn là MLE

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-18; DD F4 BR-JD-04 bước 7, BR-JD-05 bước 2 |
| Kind / priority | Boundary, P1: gộp nhãn tổng không được xoá verdict chi tiết |
| Environment | API F4 + DB + worker, adapter verdict cố định |
| Preconditions | `P-JUDGE` v1; submission đang `JUDGING` |
| Test data | Vị trí 1=`MLE`, vị trí 4=`WA`, 10 vị trí khác=`AC` |
| Steps | Cho worker hoàn tất; đọc submission và dòng kết quả. |
| Expected | 12 kết quả; dòng 1 vẫn `MLE`; status tổng `TIME_LIMIT_EXCEEDED` theo lỗi đầu tiên, `passedCount=10`, `totalCount=12`, `scoreRatio=0.833` (làm tròn 3 chữ số); không chuyển status thành `WRONG_ANSWER` theo vị trí 4. |
| Cleanup | Dọn fixture cô lập |
| Evidence | Chuỗi verdict adapter, DB kết quả và submission, response chi tiết |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC19-01 — Compile error không chạy ca nào và không có điểm giả

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-19; DD F4 BR-JD-03 bước 8, BR-JD-05 bước 4, API §8 |
| Kind / priority | Abnormal, P0: lỗi mã học viên tách khỏi lỗi dữ liệu |
| Environment | API F4 + DB + worker + F3 mapper/compiler stub |
| Preconditions | Bản sao `P-JUDGE` chuyển sang Bọc hàm với chữ ký hợp lệ, đủ 12 testcase và snapshot v1; bài nộp có lỗi cú pháp ở dòng 3 của vùng mã người học |
| Test data | Compiler trả diagnostic tương ứng dòng 3; không có verdict run |
| Steps | Nộp, chờ cuối; đọc số lần gọi run, DB/API và Outbox. |
| Expected | `status=COMPILE_ERROR`; thông báo nêu dòng 3 của mã học viên, không chứa mã khung. Run count=0, không có dòng testcase result. `score_ratio`, `total_testcase_count`, `runtime_ms`, `memory_kb` là NULL; API có `passedCount/totalCount/scoreRatio=null`, `testcaseDetail.available=false`, `unavailableReason=NOT_APPLICABLE`, `items=[]`. Một `SubmissionGraded`, không có `SubmissionAccepted`. |
| Cleanup | Dọn fixture cô lập |
| Evidence | Diagnostic đã che đường dẫn máy, message trả về, DB/API, run count, Outbox |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC20-01 — Worker nhận lại bỏ qua năm testcase đã commit

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-20; DD F4 BR-JD-04 bước 2–3/9, BR-JD-05 |
| Kind / priority | Recovery, P0: không chạy và ghi trùng khi message giao lại |
| Environment | API F4 + DB + RabbitMQ + hai worker kiểm thử, adapter đếm theo testcase |
| Preconditions | `P-JUDGE` v1; tạm dừng worker thứ hai; worker thứ nhất đã commit kết quả `AC` cho vị trí 1..5 và `judge_run_tokens.consumed_at` tương ứng |
| Test data | 12 ca đều `AC`; tắt worker thứ nhất ngay sau commit ca 5, trước ack message |
| Steps | Bật worker thứ hai, cho RabbitMQ giao lại; chờ hoàn tất; đọc token, số lần run từng ca, kết quả và Outbox. |
| Expected | Vị trí 1..5 không gọi run lại, các dòng cũ giữ nguyên token/timestamp; vị trí 6..12 chạy đúng một lần. Có đúng 12 dòng kết quả và 12 token đã tiêu thụ; `ACCEPTED`, `scoreRatio=1.000`, một `SubmissionGraded` và một `SubmissionAccepted`. Không phát lại `TESTCASE_FINISHED` cho 1..5. |
| Cleanup | Bật lại các worker, dọn message/fixture cô lập |
| Evidence | Thời điểm kill, delivery/redelivery, call count theo testcase, DB token/kết quả, Outbox/STOMP |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC21-01 — Finalize và sweep tranh nhau chỉ có một trạng thái cuối

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-21; DD F4 BR-JD-05 bước 6–9, JOB-JD-01 |
| Kind / priority | Concurrency, P0: trạng thái cuối và sự kiện không bị ghi đè |
| Environment | DB + worker + JOB-JD-01, barrier đồng thời tại lệnh UPDATE trạng thái cuối |
| Preconditions | Submission `JUDGING` đủ 12 kết quả `AC`; job sweep đã chọn cùng submission theo dữ liệu cũ |
| Test data | Worker gọi finalize `ACCEPTED`; sweep cố kết thúc `SYSTEM_ERROR/WORKER_TIMEOUT` tại cùng barrier |
| Steps | Cho hai tiến trình tranh update, thả barrier; đọc status, `finished_at`, Outbox và STOMP. |
| Expected | Chỉ một UPDATE điều kiện `status NOT IN (<trạng thái cuối>)` ghi được; status giữ nguyên từ bên thắng, không có chuyển trạng thái cuối sang cuối khác. Đúng một `SubmissionGraded` và một `SUBMISSION_FINISHED`; `SubmissionAccepted` chỉ có nếu bên thắng là `ACCEPTED`. |
| Cleanup | Tắt barrier/fault hook; dọn fixture cô lập |
| Evidence | Hai kết quả UPDATE, DB timeline, Outbox và STOMP |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC76-01 — Kết quả testcase ẩn chỉ lộ verdict và chỉ số cho phép

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-76; DD F4 API §8 T-GD-02, BR-JD-11 |
| Kind / priority | Security, P0: nội dung Hidden không được ra API/STOMP |
| Environment | API F4 + DB + F2 visibility port + STOMP |
| Preconditions | `P-JUDGE` v1; H5 ở `testcase_order=7`, verdict `WA`; H5 có input marker `SECRET_INPUT_73`, expected marker `SECRET_EXPECTED_73`, output người học marker `SECRET_ACTUAL_73` trong fixture nội bộ |
| Test data | Submission Standard I/O của `A1-JUDGE`; gọi GET `/submissions/{id}`, GET progress và theo dõi `TESTCASE_FINISHED` |
| Steps | Chấm xong; đọc dòng DB của H5, chi tiết API và payload STOMP; tìm marker trong mọi bề mặt trả người học. |
| Expected | DB H5 có `is_sample=false`, `actual_output_snippet=NULL`, `stderr_snippet=NULL`; API dòng 7 có `visibility=HIDDEN`, `testcaseId=null`, `actualOutput=null`, `stderrSnippet=null`, chỉ lộ `order`, `verdict`, `runtimeMs`, `memoryKb` trong dữ liệu testcase. Không response/bản tin nào chứa ba marker, input hoặc expected output; STOMP chỉ có order/verdict/số đếm cho ca. |
| Cleanup | Dọn fixture/đăng ký STOMP |
| Evidence | DB nội bộ, body API, payload STOMP và kết quả dò marker |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC76-02 — Bọc hàm không lưu hay trả stderr của khung

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-76; DD F4 API §5/§8, BR-JD-04 bước 8, BR-JD-11 |
| Kind / priority | Security, P0: stack trace của khung không đi vào dữ liệu học viên |
| Environment | F4 API + DB + go-judge stub; `return_stderr_to_student=true` |
| Preconditions | Bản sao `P-JUDGE` chuyển sang Bọc hàm với chữ ký hợp lệ, vẫn có S1/S2 và H1..H10 đủ điều kiện xuất bản; mã học viên lỗi chạy ở S1, H1; stderr thô chứa `INTERNAL_APH_STACK_91` và đường dẫn khung |
| Test data | Cùng nguồn mã gọi Run Code và Submit; adapter trả `RE` cùng stderr cho S1/H1 |
| Steps | Gọi Run Code; nộp và đợi chấm; đọc response Run Code, DB kết quả, GET chi tiết, STOMP. |
| Expected | `stderrSnippet=null` trong response Run Code và GET chi tiết cho cả S1/H1; DB `stderr_snippet=NULL` cho cả hai. Không có marker/đường dẫn khung trong response hoặc STOMP; verdict `RE` vẫn hiển thị, Hidden còn bị lọc output/ID. |
| Cleanup | Dọn bài, submission và stub fixture |
| Evidence | Raw stderr chỉ trong log kiểm thử nội bộ đã che, response, DB và STOMP |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC76-03 — Stderr Sample Standard I/O phụ thuộc cờ và verdict

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-76; DD F4 API §5/§8, BR-JD-04 bước 8, BR-JD-11 |
| Kind / priority | Security/Boundary, P1: chỉ stderr hợp lệ mới tới học viên |
| Environment | F4 API + DB + go-judge stub; `P-JUDGE` Standard I/O |
| Preconditions | S1 là Sample, H1 là Hidden; mỗi nhánh dùng submission riêng và cấu hình cờ được chụp lúc bắt đầu chấm |
| Test data | Stderr marker `STDERR_SAMPLE_17` từ S1; nhánh A `return_stderr_to_student=true`, S1=`WA`; nhánh B cờ false, S1=`WA`; nhánh C cờ true, S1=`AC`; H1 luôn ghi marker `STDERR_HIDDEN_17` với `WA` |
| Steps | Chạy riêng ba nhánh A/B/C; đọc DB và GET chi tiết; kiểm H1 ở cả ba. |
| Expected | Chỉ nhánh A có `stderrSnippet=STDERR_SAMPLE_17` cho S1 (tối đa 4.096 ký tự). B/C S1 có `stderrSnippet=null`; H1 luôn null trong DB/API. Không có marker Hidden trong bất kỳ response nào. |
| Cleanup | Khôi phục cấu hình sandbox; dọn ba submission fixture |
| Evidence | Cấu hình từng nhánh, DB và ba response chi tiết |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC48-01 — Backlog và pause có thứ tự ưu tiên rõ

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-48; DD F4 API §16 T-QS-02/03, BR-JD-15 |
| Kind / priority | Normal/Boundary, P1: trạng thái pause thắng backlog |
| Environment | API quản trị F4 + PostgreSQL, clock cố định `T0` |
| Preconditions | ADMIN có `JUDGE_QUEUE_MONITOR:READ`; `autoscale_job_threshold=20`, `queue_paused=false`; seed 25 job `PENDING` |
| Test data | Ba job khác có `started_grading_at=T0-10 phút`, `submitted_at` trước đó 2, 4, 6 giây; những job khác không bắt đầu trong giờ gần nhất |
| Steps | GET `/admin/queue/snapshot`; đổi `queue_paused=true` qua fixture quản trị; GET lại. |
| Expected | Lần đầu `status=BACKLOGGED`, `pendingCount=25`, `avgWaitMillis=4000`; lần hai `status=PAUSED` và vẫn 25/4000, `queueConsumptionPaused=true`. Response không có danh sách hoặc chỉ số từng worker. |
| Cleanup | Khôi phục `cluster_settings`, dọn job fixture |
| Evidence | Hai response, DB cấu hình và mốc thời gian ba job |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC48-02 — Hàng đợi rỗng không có độ trễ giả; mất Prometheus vẫn đọc được

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-48; DD F4 API §16 T-QS-01/04/05 |
| Kind / priority | Degraded/Security, P1: snapshot không phụ thuộc metric ngoài và kiểm quyền |
| Environment | API quản trị F4 + DB; có thể ngắt Prometheus; ADMIN và STUDENT thử |
| Preconditions | Không có job `PENDING`, không job nào bắt đầu chấm trong một giờ; `queue_paused=false`; cấu hình ngưỡng 20 |
| Test data | GET `/admin/queue/snapshot` khi Prometheus tắt bằng JWT ADMIN, sau đó JWT STUDENT không có `JUDGE_QUEUE_MONITOR:READ` |
| Steps | Ngắt Prometheus; gọi hai request; đọc response và call count Prometheus nếu có. |
| Expected | ADMIN nhận 200 `status=NORMAL`, `pendingCount=0`, `avgWaitMillis=null`, cấu hình queue còn đủ, không có trường worker; STUDENT nhận 403 `JDG-301`, không có dữ liệu queue. Endpoint không cần gọi Prometheus để dựng response. |
| Cleanup | Bật lại Prometheus; dọn fixture |
| Evidence | Hai request/response đã che JWT, trạng thái dịch vụ số liệu và DB |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC48-03 — Độ trễ chỉ tính job bắt đầu trong giờ gần nhất

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-48; DD F4 API §16.2 `avgWaitMillis`, BR-JD-15 |
| Kind / priority | Boundary, P1: cửa sổ thời gian không bị lệch bởi job cũ |
| Environment | API quản trị F4 + DB, clock cố định `T0` |
| Preconditions | `queue_paused=false`, không job `PENDING`; ADMIN có quyền đọc |
| Test data | Job A `started_grading_at=T0-59 phút`, `submitted_at` trước 2 giây; job B `started_grading_at=T0-61 phút`, `submitted_at` trước 30 giây |
| Steps | Seed hai job; GET `/admin/queue/snapshot` tại `T0`. |
| Expected | `status=NORMAL`, `pendingCount=0`, `avgWaitMillis=2000` chỉ từ A; B không được đưa vào mẫu số, dù thời gian chờ 30 giây. |
| Cleanup | Dọn hai job fixture |
| Evidence | DB timestamp A/B, response và thời gian clock |
| Design review | Đã review; chưa thực thi |

## Phạm vi tiếp theo

Các nhánh danh sách job (§17), pause/resume (§19), autoscaler (JOB-JD-05), hết hạn chi tiết 90 ngày và chuyển visibility Sample/Hidden sẽ được tách ca ở cụm hàng đợi/bảo mật tiếp theo. Không suy ra đã phủ toàn F4 từ 12 ca này.
