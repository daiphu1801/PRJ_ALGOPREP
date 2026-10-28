# Test case QA — F2: lưu testcase, upload và chạy đáp án mẫu

> 10 ca thiết kế cho AC-54, 55, 58, 61–64; chưa thực thi. Nguồn: TDD F2 AC tương ứng; DD F2 API §42–48, logic BR-12/14/15. Cụm phiên bản bộ chấm AC-56/57/59/60 đã có file riêng. Dùng A2 là tác giả, bài X; đo cả DB, MinIO, Redis và snapshot khi ca yêu cầu.

### TC-F2-AC54-01 — Hai vế chọn INLINE/MINIO độc lập ở mốc 8192 byte

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-54; DD F2 API §42 PB42-N1/B1, BR-12 |
| Kind / priority | Boundary, P0: object và cột lưu đúng cặp |
| Environment | POST testcase, DB, MinIO |
| Preconditions | X UNPUBLISHED có position max=3, version=1; A2 có TESTCASE_MANAGEMENT:CREATE |
| Test data | T1 input ASCII 8192 byte, expected `ok`; T2 input 8193 byte; T3 input `x`, expected 8193 byte |
| Steps | POST ba ca độc lập, kiểm response/DB/MinIO keys. |
| Expected | Cả ba 201, origin MANUAL, approved=true, position lần lượt 4/5/6; T1 input INLINE, T2 input MINIO key `problems/X/testcases/T2/input.txt`, T3 input INLINE và expected MINIO. Mỗi vế chỉ một nơi có nội dung theo CHECK; X chưa xuất bản nên version vẫn 1, không phát bump. |
| Cleanup | Dọn testcase và objects |
| Evidence | Ba response, DB CHECK columns, MinIO object hash |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC55-01 — Input rỗng, vế quá 1MB và trần 500

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-55; DD F2 API §42.2/42.4 PB42-A1 |
| Kind / priority | Abnormal + boundary, P1 |
| Environment | POST testcase, DB/MinIO |
| Preconditions | X UNPUBLISHED; fixture A có 499 testcase, fixture B có 500; DB/object counts chụp trước |
| Test data | input `""`; input 1,048,577 byte ASCII; input `x` trên fixture B |
| Steps | Gửi ba request độc lập, kiểm counts/objects. |
| Expected | Lần đầu `400 PRB-001`; lần hai `422 PRB-048`; lần ba `422 PRB-046`; không thêm testcase/object, không đổi version ở cả ba. |
| Cleanup | Dọn fixture |
| Evidence | Ba response, byte counts, DB/MinIO diff |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC58-01 — Sửa INLINE sang MINIO và chặn ID testcase bài khác

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-58; DD F2 API §43 PB43-B2/A2; BR-12 |
| Kind / priority | Boundary + security, P0 |
| Environment | PATCH testcase, DB/MinIO |
| Preconditions | X UNPUBLISHED có T1 input INLINE `x`; Y có TY; A2 sở hữu X; version X=1 |
| Test data | PATCH T1 input ASCII 9216 byte; PATCH `/problems/X/testcases/TY` |
| Steps | Gọi hai request, kiểm cột lưu/URL/object và DB Y. |
| Expected | T1 200 chuyển input sang MINIO, inline input NULL, object key của X/T1 có hash đúng; expectedOutput không đổi, version X vẫn 1. TY trả `404 PRB-011`, không đọc/sửa nội dung Y, không tạo object mới. |
| Cleanup | Dọn objects/fixture |
| Evidence | Hai response, DB CHECK, object hash, Y before/after |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC61-01 — URL upload có khoá server và giới hạn số cặp

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-61; DD F2 API §46 PB46-N1/A1/B1, BR-14 |
| Kind / priority | Boundary + security, P0: client không chọn object key |
| Environment | POST bulk-upload-url, Redis, MinIO presign, DB |
| Preconditions | X có 499 testcase; clock T0; A2 là tác giả |
| Test data | Request 1 cặp 1KB/2KB clientKey=a; 2 cặp; 101 cặp; một vế 65MB |
| Steps | Gọi bốn request, kiểm Redis TTL, URL scope và DB count. |
| Expected | Một cặp trả 200 uploadId mới, 2 putUrl chỉ ở `problems/X/testcases/{serverUUID}/`, hết hiệu lực T0+15 phút; Redis session TTL 20 phút, DB vẫn 499. Hai cặp `422 PRB-046`; 101 cặp và 65MB `422 PRB-047`; các lỗi không thêm session/testcase. |
| Cleanup | Xoá Redis session và object tạm |
| Evidence | Bốn response đã che signature, Redis TTL, DB count/presign trace |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC62-01 — Confirm ba cặp nguyên tử và retry cùng uploadId

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-62; DD F2 API §47 PB47-N1/N2/B1, BR-14 |
| Kind / priority | Atomicity + idempotency, P0 |
| Environment | URL/confirm API, Redis, MinIO, DB/snapshot/Outbox |
| Preconditions | X PUBLISHED có 8 approved testcase, version=4; session U thuộc A2 có 3 cặp (một Sample, hai Hidden), sáu object đã PUT đủ; một vế 1KB, vế khác 9KB |
| Test data | POST confirm `{uploadId:U}` hai lần |
| Steps | Confirm lần đầu, kiểm DB/snapshot/object; confirm lại, so ID/version/events. |
| Expected | Cả hai trả 201 với cùng ba testcaseId; sau lần đầu thêm đúng ba MANUAL approved ở cuối, vế 1KB thành INLINE, vế 9KB giữ MINIO; version 4→5, đúng một snapshot mới và một TestcaseSetVersionBumped. Lần hai không thêm dòng, không bump/event lại. |
| Cleanup | Dọn fixture/session/object |
| Evidence | Hai response, DB/Redis/MinIO, snapshot hashes, Outbox |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC62-02 — Object thiếu hoặc session người khác không thêm nửa lô

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-62; DD F2 API §47 PB47-A1/A2 |
| Kind / priority | Security + atomicity, P0 |
| Environment | Confirm API, Redis, MinIO, DB |
| Preconditions | Session U của A2 có 3 cặp, thiếu expectedOutput của cặp b; session V thuộc B; X version=4, DB N testcase |
| Test data | A2 confirm U; A2 confirm V; sau đó bổ sung object thiếu cho U và retry |
| Steps | Gọi ba request, kiểm DB/session/version sau từng lần. |
| Expected | U lần đầu `422 PRB-045`, details.items chứa clientKey=b side=expectedOutput problem=MISSING, DB vẫn N/version4, session còn hiệu lực; V `422 PRB-044`, không lộ URL của B; sau tải bổ sung U confirm 201 thêm đúng ba ca, version chỉ tăng một. |
| Cleanup | Dọn fixture/session |
| Evidence | Ba response, HEAD traces, DB/version |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC63-01 — Chạy đáp án trên tám ca duyệt, nháp chỉ khi chọn ID

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-63; DD F2 API §48 PB48-N1/N2 |
| Kind / priority | Normal + security, P0: không trả output Hidden |
| Environment | POST reference-solution/run, sandbox, DB/MinIO |
| Preconditions | X có 8 approved (2 Sample, 6 Hidden), một AI draft chưa approved; reference solution pass mọi ca; DB/used counters chụp trước |
| Test data | POST body `{}`; POST `{testcaseId:draftId}` |
| Steps | Gọi hai lần, so số runner calls/results và tìm output marker Hidden. |
| Expected | Lần đầu 200 summary `{passed:8,total:8,allPass:true}`, 8 results theo position, không draft; lần hai đúng một result của draft. Không có actualOutput/diff/input/expected trong response kể cả Hidden; không ghi DB/AI used. |
| Cleanup | Dọn fixture |
| Evidence | Hai response, runner traces, DB counts |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC63-02 — Mã tạm sai một ca không ảnh hưởng ca sau

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-63; DD F2 API §48 PB48-A1 |
| Kind / priority | Abnormal, P1 |
| Environment | Reference run, sandbox, DB |
| Preconditions | X có 5 approved, mã tạm sai đúng testcase position 3, bốn ca còn lại pass |
| Test data | POST `{language:PYTHON,sourceCode:"mã tạm sai ca 3"}` |
| Steps | Gọi API, kiểm 5 results/verdict và DB. |
| Expected | 200, summary passed=4,total=5,allPass=false; ca 3 FAIL/WRONG_ANSWER, ca 4–5 vẫn chạy/pass; không actualOutput/diff, không lưu mã tạm làm đáp án mẫu. |
| Cleanup | Dọn fixture |
| Evidence | Response, runner trace, DB reference hash |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC64-01 — Thiếu đáp án, lỗi biên dịch, không testcase

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-64; DD F2 API §48 PB48-A2/B1 |
| Kind / priority | Abnormal + boundary, P1 |
| Environment | Reference run, sandbox, DB |
| Preconditions | X không có reference solution; Y có 3 approved và mã tạm lỗi cú pháp; Z có reference solution nhưng không approved testcase |
| Test data | POST X `{}`; POST Y với language/sourceCode lỗi; POST Z `{}` |
| Steps | Gọi ba request, so lỗi/results/DB. |
| Expected | X `422 PRB-040`; Y 200 `compileError` có nội dung, mỗi dòng `failure=COMPILE_ERROR`, không actualOutput; Z 200 `summary.total=0,allPass=false`, results=[]; cả ba không ghi DB. |
| Cleanup | Dọn fixture |
| Evidence | Ba response, DB counts, compiler trace |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC64-02 — Go-judge lỗi trả 503 và hoàn lượt rate limit

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-64; DD F2 API §48.4/48.5 PB48-A3/B2 |
| Kind / priority | Recovery, P1 |
| Environment | Reference run, sandbox fault injection, Redis rate key |
| Preconditions | A2 có 9 lượt được nhận trong 60 giây, X có reference/testcase; Redis key quan sát được |
| Test data | POST khi go-judge tắt; khôi phục và POST thêm lần hợp lệ |
| Steps | Gọi lỗi, kiểm key/DB; khôi phục và gọi lại. |
| Expected | Lần lỗi `503 PRB-050`, bảng/DB không đổi và mốc đã giữ được hoàn; lần sau không bị `PRB-055` vì vẫn chỉ có 9 lượt trước đó, chạy được. Không tính vào quota sinh AI. |
| Cleanup | Khôi phục sandbox, dọn Redis key/fixture |
| Evidence | Hai response, Redis timestamps, DB/runner trace |
| Design review | Đã review; chưa thực thi |
