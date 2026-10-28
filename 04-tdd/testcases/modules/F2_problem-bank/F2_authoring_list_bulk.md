# Test case QA — F2: danh sách quản lý, CSV và thao tác lô

> 13 ca thiết kế cho AC-29–38; chưa thực thi. Nguồn: TDD F2 AC-29–38; DD F2 API §25–33, logic BR-03/08/09. A2 chỉ đọc/sửa bài mình, A3 là ADMIN; DB/audit/Outbox được chụp trước/sau mỗi ca. Các lô độc lập không chia sẻ fixture.

### TC-F2-AC29-01 — Mã slug tránh trùng cả bài xoá mềm và ký hiệu rỗng

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-29; DD F2 logic BR-03; API §21/36 |
| Kind / priority | Boundary, P1: code giữ unique lịch sử |
| Environment | POST authoring problem/topic, DB |
| Preconditions | Có bài code HAI-SO cho tiêu đề `Hai số`; bài code TWO-SUM tiêu đề `Two Sum` đã deleted=true; chưa có topic code TOPIC |
| Test data | Tạo bài `Hai số`, bài `Two Sum`, topic `!!!` |
| Steps | Gửi ba request hợp lệ, đọc code/DB unique. |
| Expected | Bài thứ nhất mới code `HAI-SO-2`; bài Two Sum mới có hậu tố `TWO-SUM-2` vì code cũ vẫn chiếm unique; topic ký hiệu rỗng code `TOPIC`; không đổi code các dòng cũ. |
| Cleanup | Dọn fixture mới |
| Evidence | Ba response, DB code trước/sau |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC30-01 — Phạm vi A2/A3 và tổng không phụ thuộc lọc

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-30; DD F2 API §25 PB25-N1/N2/A1/A2 |
| Kind / priority | Security + normal, P0 |
| Environment | GET `/authoring/problems`, DB, FE SHR0201 |
| Preconditions | A2 có P1–P3 (2 published), A2' có Q1–Q2 (1 published), D deleted=true; levels sort_order HARD=1,EASY=2 khác tên chữ cái |
| Test data | A2/A3 GET mặc định, A2 GET `?q={Q1 code}`, A3 GET `?sort=level&dir=asc` |
| Steps | Gọi bốn request, so items/totals/sort với DB. |
| Expected | A2 thấy 3, `totals={total:3,published:2}`; A3 thấy 5, totals 5/3; D không ở bất kỳ danh sách. A2 q khớp Q1 trả rỗng nhưng totals vẫn 3/2; A3 sort level theo sort_order HARD trước EASY, không theo tên. Không trả nội dung testcase. |
| Cleanup | Dọn fixture |
| Evidence | Bốn response, DB rows, ảnh màn |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC31-01 — Tham số quản trị đọc nghiêm

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-31; DD F2 API §25 PB25-A3/B1 |
| Kind / priority | Boundary + abnormal, P1 |
| Environment | GET authoring problems |
| Preconditions | A3 đủ quyền; bài code gồm chuỗi khớp q 100 ký tự |
| Test data | `pageSize=7`, q 101 ký tự, q 100 ký tự |
| Steps | Gọi ba request độc lập, kiểm status/body. |
| Expected | Hai request đầu `400 PRB-001`, không trả danh sách; q đúng 100 ký tự trả 200 theo bộ lọc. Không ghi DB. |
| Cleanup | Không ghi dữ liệu |
| Evidence | Ba URL/response |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC32-01 — Phân bố chủ đề và bốn quy tắc cần chú ý

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-32; DD F2 API §26/27 PB26-N1/B1, PB27-N1/B1 |
| Kind / priority | Normal + boundary, P1 |
| Environment | GET topic-distribution/attention, DB, clock T0 |
| Preconditions | A3 có 10 bài chưa deleted, 5 bài gắn ARRAY, tổng 8 bài có ít nhất một topic; một bài acRate=20, một bài =30, một bài null; một nháp unpublished never published updatedAt=T0-10d, một nháp updatedAt=T0-7d; một bài đã từng published rồi rút, publishedAt khác null; đúng một bài không testcase, chín bài còn lại đều có testcase. |
| Test data | GET hai endpoint bằng A3; lặp distribution với actor chỉ có bài không gắn topic |
| Steps | Gọi ba request, đối chiếu từng tập bài theo DB. |
| Expected | ARRAY count=5, percent=62.5 (5/8), totalProblemsWithTopic=8; attention luôn bốn ruleCode, LOW_AC=1, STALE_DRAFT=1, HIDDEN_FROM_LEARNERS=1, NO_TESTCASE=1. AC đúng 30/null không khớp LOW_AC; nháp đúng mốc 7d không khớp STALE_DRAFT. Actor không topic nhận items=[], totalProblemsWithTopic=0, không chia 0. |
| Cleanup | Dọn fixture |
| Evidence | Ba response, DB sets/timestamps |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC33-01 — CSV chỉ tám cột và chặn công thức

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-33; DD F2 API §28 PB28-N1/A1/B1 |
| Kind / priority | Security + normal, P0: CSV injection và rò dữ liệu |
| Environment | POST `/authoring/problems/export`, DB, parser CSV chuẩn |
| Preconditions | A3 có P1–P3; P2 title `=SUM(1,1)`, có source/reference/testcase marker SECRET33; A2 có thêm Q1 của người khác |
| Test data | A3 `{problemIds:[P1,P2,P3]}`; A2 `{problemIds:[P1,Q1]}` với P1 của A2, Q1 của A2' |
| Steps | Tải hai CSV, parse BOM/header/cột/dòng và tìm marker. |
| Expected | A3 200 text/csv UTF-8 BOM, tên file problems-YYYYMMDD.csv, đúng một header tám cột và ba dòng; ô title P2 bắt đầu `'=SUM(1,1)`. Không có statement/spec/testcase/SECRET33; A2 chỉ xuất P1, không Q1. Không ghi DB/audit. |
| Cleanup | Dọn fixture |
| Evidence | Response headers, bytes CSV, parsed rows, DB |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC33-02 — Chỉ nhận đúng một nguồn chọn CSV

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-33; DD F2 API §28.2/28.4 |
| Kind / priority | Abnormal, P1 |
| Environment | POST export |
| Preconditions | A3 đủ quyền, P1 tồn tại |
| Test data | `{problemIds:[P1],filter:{}}`, `{}`, `{problemIds:[]}` |
| Steps | Gọi ba body độc lập, kiểm response. |
| Expected | Cả ba `400 PRB-001`, không có file CSV hoặc ghi DB. |
| Cleanup | Không ghi dữ liệu |
| Evidence | Ba request/response |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC34-01 — Xuất bản lô xử lý từng bài độc lập

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-34; DD F2 API §29 PB29-A1, BR-08 |
| Kind / priority | Partial failure, P0 |
| Environment | POST bulk/publish, SampleSolutionRunnerPort, DB/audit/Outbox |
| Preconditions | P1 đạt checklist và đáp án mẫu pass; P2 thiếu ba điều kiện, cả hai UNPUBLISHED của A3; audit/outbox N/M |
| Test data | `{problemIds:[P1,P2]}` |
| Steps | Gọi lô, đọc kết quả từng ID, DB/audit/outbox và GET public. |
| Expected | 200, P1 ở succeededIds, status PUBLISHED và public GET thấy, một audit và một ProblemPublished; P2 ở failedItems `PRB-030` kèm ba checklist thiếu, vẫn UNPUBLISHED, không audit/event. Lỗi P2 không rollback P1. |
| Cleanup | Dọn fixture |
| Evidence | Response, runner trace, DB/audit/outbox |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC35-01 — Rút bài giữ lịch sử và gọi lại không thêm tác động

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-35; DD F2 API §30 PB30-N1/B1, BR-08 |
| Kind / priority | Recovery + data integrity, P0 |
| Environment | POST bulk/unpublish, GET public, DB F2/F4, audit/Outbox |
| Preconditions | P1 PUBLISHED, publishedAt=T1, có submission S1, bookmark A và assignment L1; audit/outbox N/M |
| Test data | Hai POST `{problemIds:[P1]}` |
| Steps | Gọi lần đầu, đọc public/DB/audit/outbox; gọi lại, so counts. |
| Expected | Cả hai 200 thành công; sau lần đầu P1 UNPUBLISHED, biến khỏi GET public ngay, publishedAt vẫn T1, S1/bookmark/assignment còn; đúng một audit và một ProblemUnpublished. Lần hai không thêm audit/event, không đổi dữ liệu. |
| Cleanup | Dọn fixture |
| Evidence | Hai response, public GET, DB/audit/outbox timeline |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC36-01 — Đổi mức ba bài và chặn mức đã xoá cả lô

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-36; DD F2 API §31 PB31-N1/A1 |
| Kind / priority | Normal + integrity, P1 |
| Environment | POST bulk/difficulty, DB/audit |
| Preconditions | P1–P3 EASY của A3, HARD còn, OLD bị admin xoá; audit N |
| Test data | `{problemIds:[P1,P2,P3],levelId:HARD}`; rồi cùng IDs levelId OLD |
| Steps | Gọi hai request, đọc level_id/audit sau từng lần. |
| Expected | Lần đầu 200, cả ba succeededIds và HARD, audit N+3; lần sau `422 PRB-039`, details.field=levelId, không bài nào đổi khỏi HARD, audit vẫn N+3. |
| Cleanup | Khôi phục mức cũ |
| Evidence | Hai response, DB/audit |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC36-02 — Gán chủ đề lần hai không nhân đôi quan hệ

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-36; DD F2 API §32 PB32-N1/B1 |
| Kind / priority | Idempotency, P1 |
| Environment | POST bulk/topic, DB problem_topics/audit |
| Preconditions | P1–P3 chưa gắn GRAPH, audit N |
| Test data | Gửi `{problemIds:[P1,P2,P3],topicId:GRAPH}` hai lần |
| Steps | Gọi hai request, đếm quan hệ và audit. |
| Expected | Cả hai 200; sau lần đầu đúng ba quan hệ và ba audit, sau lần hai vẫn đúng ba quan hệ và audit N+3. Không nhân đôi topicNames ở danh sách. |
| Cleanup | Dọn quan hệ fixture |
| Evidence | Hai response, DB/audit, GET list |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC37-01 — Xoá mềm bài có lịch sử và lớp giao

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-37; DD F2 API §33 PB33-N1, BR-09 |
| Kind / priority | Data integrity + security, P0 |
| Environment | POST bulk/delete, DB F2/F4, GET public/admin, audit/Outbox |
| Preconditions | P1 PUBLISHED có 50 submissions, 2 assignments, 8 testcases, bookmark A; audit/outbox N/M |
| Test data | A3 POST `{problemIds:[P1]}` |
| Steps | Gọi lô, kiểm tất cả bảng và hai list. |
| Expected | 200 succeededIds=P1; `status=UNPUBLISHED`, `deleted=true`; public/admin lists không còn P1; submissions/bookmark/assignments/testcases vẫn đủ, không hard delete; một audit và một ProblemUnpublished sau commit. |
| Cleanup | Dọn fixture |
| Evidence | Response, DB counts, two GET, audit/outbox |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC37-02 — A2 xoá bài người khác nhận lỗi theo trạng thái

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-37; DD F2 API §33 PB33-A1/A2, BR-02 |
| Kind / priority | Security, P0 |
| Environment | POST bulk/delete, DB/audit |
| Preconditions | A2' sở hữu PUBLISHED Q1 và UNPUBLISHED Q2; A2 có quyền DELETE nhưng không phải tác giả; D đã deleted; audit N |
| Test data | A2 gửi `{problemIds:[Q1,Q2,D]}` |
| Steps | Gọi lô, xem failedItems và DB/audit. |
| Expected | 200; Q1 failed `PRB-003`, Q2 failed `PRB-010`, D failed `PRB-010`; không ID nào succeeded, DB/audit không đổi. Không lộ nội dung nháp Q2. |
| Cleanup | Dọn fixture |
| Evidence | Response, DB ownership/status, audit |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC38-01 — Lô rỗng và publish quá 20 bị chặn trước xử lý

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-38; DD F2 API §29–33 request/errors |
| Kind / priority | Boundary, P1: không xử lý một phần input sai |
| Environment | Năm POST bulk endpoints, DB/audit/runner |
| Preconditions | A3 có 21 bài hợp lệ; snapshot DB/audit, runner call count 0 |
| Test data | `problemIds=[]` cho publish/unpublish/difficulty/topic/delete (thêm levelId/topicId hợp lệ khi cần); publish 21 IDs |
| Steps | Gọi sáu request độc lập, kiểm lỗi và side effects. |
| Expected | Cả sáu `400 PRB-001`, không chạy sandbox, không sửa bài/quan hệ, không ghi audit/event. 20 IDs publish là ngưỡng hợp lệ theo DD nhưng fixture này không dùng để xuất bản. |
| Cleanup | Không thay đổi |
| Evidence | Sáu response, DB/audit/runner trace |
| Design review | Đã review; chưa thực thi |
