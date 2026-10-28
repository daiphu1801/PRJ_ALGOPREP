# Test case QA — F4: lưu giữ chi tiết và visibility của testcase

> Trạng thái: 8 ca đã review thiết kế; chưa thực thi. Trọng tâm là ranh giới 90 ngày và hai lớp lọc dữ liệu Sample/Hidden ở API.

## Phạm vi và fixture

- TDD: `04-tdd/modules/F4_judge-orchestration.md` AC-31–33, AC-76; DD F4 API §8, §9, logic BR-JD-11, job JOB-JD-04; DD F2 API §43–44.
- `T0=2026-10-26T03:00:00Z`, đồng hồ DB/API kiểm thử cố định. `P-RESULT` là bài Standard I/O đã `PUBLISHED`, snapshot v1 có 12 ca: S1,S2,S3 là Sample, H1..H9 là Hidden, đúng `position=1..12`. Học viên A sở hữu các bài nộp, học viên B không sở hữu. Các bài nộp kiểm retention đều `WRONG_ANSWER`, `passed=7`, `total=12`, `score_ratio=0.583`, `runtime_ms=27`, `memory_kb=2048`, `source_code="print(0)\n"`; có 12 dòng chi tiết được tạo một phút sau `submitted_at` khi chưa bị job dọn.
- Với ca đổi visibility, sử dụng bản sao độc lập `P-RESULT` và submission mới tại T0; bản ghi `is_sample` được chụp lúc chấm. Dùng marker input/output khác nhau cho mỗi ca; không thay đổi dữ liệu fixture dùng bởi ca khác.

## Ma trận nhánh

| AC | Nhánh | Test case | Review | Thực thi |
| :--- | :--- | :--- | :--- | :--- |
| F4 AC-33 | 89 ngày còn chi tiết | TC-F4-AC33-01 | Đã review | Chưa thực hiện |
| F4 AC-33 | Đúng 90 ngày còn chi tiết | TC-F4-AC33-02 | Đã review | Chưa thực hiện |
| F4 AC-33 | 91 ngày bị chặn trước job dọn | TC-F4-AC33-03 | Đã review | Chưa thực hiện |
| F4 AC-33 | Job chỉ xoá dòng con quá hạn | TC-F4-AC33-04 | Đã review | Chưa thực hiện |
| F4 AC-31, AC-76 | Hidden đổi thành Sample sau chấm | TC-F4-AC31-01 | Đã review | Chưa thực hiện |
| F4 AC-31, AC-76 | Sample đổi thành Hidden sau chấm | TC-F4-AC31-02 | Đã review | Chưa thực hiện |
| F4 AC-31, AC-76 | Không tra được visibility hiện tại | TC-F4-AC31-03 | Đã review | Chưa thực hiện |
| F4 AC-32 | Chủ sở hữu và ID không tồn tại cùng 404 | TC-F4-AC32-01 | Đã review | Chưa thực hiện |

## Test case

### TC-F4-AC33-01 — Bài nộp 89 ngày tuổi còn đủ chi tiết

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-33; DD F4 API §8 T-GD-04, BR-JD-11 |
| Kind / priority | Boundary, P1: không ẩn chi tiết sớm |
| Environment | API F4 + PostgreSQL, clock `T0` |
| Preconditions | Submission A của chủ sở hữu nộp `T0-89 ngày`; 12 dòng kết quả còn trong DB |
| Test data | GET `/submissions/{A}` và GET `/submissions/{A}/progress` bằng JWT chủ sở hữu |
| Steps | Gọi hai endpoint tại T0; đọc body và số dòng DB. |
| Expected | Chi tiết `testcaseDetail.available=true`, `unavailableReason=null`, đủ 12 `items` theo thứ tự; status `WRONG_ANSWER`, `passedCount=7`, `totalCount=12`, `scoreRatio=0.583`, runtime/memory và source code còn nguyên. Progress có 12 `{order,verdict}`; không lộ input/expected Hidden. |
| Cleanup | Dọn submission fixture |
| Evidence | Timestamp, DB kết quả, hai response |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC33-02 — Đúng mốc 90 ngày vẫn chưa hết hạn

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-33; DD F4 API §8 (`submitted_at < now()-90 ngày`), BR-JD-11 |
| Kind / priority | Boundary, P1: phép so sánh phải là nhỏ hơn nghiêm ngặt |
| Environment | API F4 + PostgreSQL, clock `T0` |
| Preconditions | Submission B có `submitted_at=T0-90 ngày` đúng giây; 12 dòng kết quả có `created_at=T0-90 ngày+1 phút` và còn trong DB |
| Test data | GET `/submissions/{B}` tại T0 |
| Steps | Đọc response và timestamps thực tế trong DB. |
| Expected | `testcaseDetail.available=true`, 12 items; không trả `RETENTION_EXPIRED`. Điểm và status vẫn 7/12, `0.583`, `WRONG_ANSWER`. |
| Cleanup | Dọn submission fixture |
| Evidence | Clock/timestamp DB, response chi tiết |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC33-03 — Sau 91 ngày API ẩn chi tiết dù job chưa dọn

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-33; DD F4 API §8 T-GD-04, §9.4, BR-JD-11 |
| Kind / priority | Security/Boundary, P0: không phụ thuộc lịch chạy job để bảo vệ dữ liệu |
| Environment | API F4 + PostgreSQL; JOB-JD-04 tạm dừng trong môi trường kiểm thử |
| Preconditions | Submission C nộp `T0-91 ngày`, 12 dòng chi tiết vẫn tồn tại vật lý; chủ sở hữu có JWT |
| Test data | GET `/submissions/{C}` và GET `/submissions/{C}/progress` tại T0 |
| Steps | Xác nhận DB còn 12 dòng, gọi hai endpoint, kiểm toàn bộ payload và tổng số. |
| Expected | Chi tiết trả `available=false`, `unavailableReason=RETENTION_EXPIRED`, `items=[]`; progress `testcases=[]`, `isFinal=true`. Status `WRONG_ANSWER`, 7/12, `scoreRatio=0.583`, runtime 27, memory 2048 và `sourceCode="print(0)\n"` vẫn còn. Không response nào chứa output hoặc ID testcase cũ. |
| Cleanup | Bật lại JOB-JD-04 và dọn fixture cô lập |
| Evidence | DB trước job, hai response, clock và trạng thái job |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC33-04 — JOB-JD-04 xoá chi tiết quá hạn, giữ submission và token

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-33; DD F4 JOB-JD-04, BR-JD-11 |
| Kind / priority | Recovery, P1: dọn dữ liệu lá không xoá lịch sử hoặc dấu vết idempotency |
| Environment | PostgreSQL + JOB-JD-04, clock `T0` |
| Preconditions | Submission C có 12 dòng `created_at=T0-91 ngày`; submission D có 12 dòng `created_at=T0-89 ngày`; cả hai có token đã tiêu thụ trỏ từ kết quả |
| Test data | Chạy JOB-JD-04 một lần, rồi chạy lại lần hai cùng clock |
| Steps | Ghi số dòng trước, kích hoạt job hai lần; đọc `submission_testcase_results`, `submissions`, `judge_run_tokens`, API chi tiết C/D. |
| Expected | 12 dòng của C bị xoá, 12 dòng D còn; cả C/D trong `submissions` giữ source/status/score, token của cả hai giữ nguyên. Chạy lần hai không xoá thêm dòng. API C vẫn `RETENTION_EXPIRED`, D còn 12 items. Không có FK lỗi hay thay đổi Outbox. |
| Cleanup | Dọn C/D và token fixture theo quy trình môi trường kiểm thử |
| Evidence | DB trước/sau hai lần job, job log, hai response API |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC31-01 — Hidden lúc chấm không lộ output sau khi thành Sample

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-31, AC-76; DD F4 API §8 T-GD-07, BR-JD-04 bước 8, BR-JD-11; DD F2 API §43 |
| Kind / priority | Security, P0: đổi visibility về sau không mở dữ liệu đã ẩn |
| Environment | F2/F4 API + PostgreSQL + snapshot/worker |
| Preconditions | `P-RESULT` v1; H1 ở vị trí 4, `HIDDEN`; học viên nộp và chấm xong khi H1 còn Hidden, H1=`WA` với output marker `ACTUAL_H1_SECRET_7` |
| Test data | A2 đổi H1 thành `SAMPLE` bằng PATCH §43 sau khi chấm; GET chi tiết submission cũ |
| Steps | Ghi DB kết quả H1 và phiên bản; PATCH visibility, đọc snapshot mới; gọi GET chi tiết bằng chủ sở hữu. |
| Expected | PATCH tạo phiên bản snapshot mới theo BR-11, nhưng submission cũ vẫn ghim v1. Dòng H1 cũ có `is_sample=false`, `actual_output_snippet=NULL`; API vẫn trả `visibility=HIDDEN`, `testcaseId=null`, `actualOutput=null`, `stderrSnippet=null`, không lộ marker. Không đọc bảng testcase hiện hành để phục hồi output cũ. |
| Cleanup | Dọn bài, submission và snapshot fixture |
| Evidence | PATCH response, phiên bản/snapshot, DB kết quả cũ, GET response |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC31-02 — Sample lúc chấm bị đổi Hidden thì response phải ẩn

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-31, AC-76; DD F4 BR-JD-11 lớp phản hồi; DD F2 API §43 |
| Kind / priority | Security, P0: visibility hiện tại phải chặn dữ liệu Sample cũ |
| Environment | F2/F4 API + PostgreSQL + worker |
| Preconditions | `P-RESULT` có ba Sample S1/S2/S3; submission cũ đã chấm S1=`WA`, lưu `is_sample=true`, `actual_output_snippet="OLD_SAMPLE_OUTPUT_9"` |
| Test data | A2 PATCH S1 `SAMPLE→HIDDEN`; bài vẫn còn hai Sample đã duyệt |
| Steps | PATCH S1, GET chi tiết submission cũ; so với dòng DB kết quả. |
| Expected | PATCH hợp lệ và tạo snapshot mới; DB cũ vẫn có output Sample nhưng API dòng S1 báo `visibility=HIDDEN`, `testcaseId=null`, `actualOutput=null`, `stderrSnippet=null`; không có `OLD_SAMPLE_OUTPUT_9` ở response. Các số tổng của submission không đổi. |
| Cleanup | Dọn fixture cô lập |
| Evidence | PATCH response, DB trước/sau, GET body đã quét marker |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC31-03 — Không tra được visibility thì mặc định ẩn dữ liệu

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-31, AC-76; DD F4 BR-JD-11, API §8.5 |
| Kind / priority | Degraded/Security, P0: lỗi cổng F2 không biến thành rò dữ liệu |
| Environment | API F4 + DB; fault injection cổng đọc visibility F2 |
| Preconditions | S1 đã chấm khi là Sample, DB lưu `is_sample=true`, `actual_output_snippet="SAMPLE_OUTPUT_13"`; submission còn hạn lưu giữ |
| Test data | Cổng F2 trả lỗi tạm thời khi GET `/submissions/{id}` |
| Steps | Bật lỗi cổng, gọi GET bằng chủ sở hữu; đọc body; tắt fault và gọi lại. |
| Expected | Khi cổng lỗi, API vẫn trả phần submission/điểm; dòng S1 coi như `HIDDEN`, `testcaseId/actualOutput/stderrSnippet=null`, không lộ marker. Sau khi cổng phục hồi và hiện tại S1 vẫn `SAMPLE`, cùng dòng có thể trả `visibility=SAMPLE` và `actualOutput="SAMPLE_OUTPUT_13"`; DB kết quả không bị sửa bởi hai lần đọc. |
| Cleanup | Tắt fault injection và dọn fixture |
| Evidence | Hai response, log lỗi cổng đã che dữ liệu, DB trước/sau |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC32-01 — Token của người khác và UUID không tồn tại trả cùng lỗi

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-32; DD F4 API §8.4 T-GD-03, §9.3 |
| Kind / priority | Security, P0: chống dò bài nộp và lộ mã nguồn |
| Environment | API F4 + PostgreSQL |
| Preconditions | Submission E thuộc học viên A; học viên B đăng nhập, UUID `P-GONE` không tồn tại |
| Test data | B gọi GET `/submissions/{E}`, GET `/submissions/{P-GONE}` và hai đường `/progress` tương ứng |
| Steps | Gọi bốn request, so status, mã lỗi, thông điệp, body và kiểm DB không có ghi. |
| Expected | Cả bốn trả 404 `JDG-110` với thân lỗi cùng dạng; B không nhận `sourceCode`, status, kết quả hoặc dấu hiệu E tồn tại. Không ghi DB hoặc sự kiện. |
| Cleanup | Dọn submission và tài khoản fixture |
| Evidence | Bốn request/response đã che JWT, DB trước/sau |
| Design review | Đã review; chưa thực thi |
