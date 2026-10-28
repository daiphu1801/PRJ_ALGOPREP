# Test case QA — F2: bookmark, gợi ý và tóm tắt kho bài

> 10 ca thiết kế cho AC-12–18; chưa thực thi. Nguồn: TDD F2 AC-12–18; DD F2 API §9–14, logic BR-21/23/25. Dùng JWT học viên A/B và DB problem; các ca gợi ý còn cần F1 SolveStatePort, F4 MyTopicSubmissionMetricsPort. Mỗi ca có fixture độc lập.

### TC-F2-AC12-01 — Tạo, sửa, xoá và Hoàn tác bookmark

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-12; DD F2 API §10 PB10-N1/N2/A1; BR-21 |
| Kind / priority | Normal + recovery, P1: không hồi sinh bookmark khi sửa ghi chú |
| Environment | PUT/DELETE `/me/bookmarks/{id}`, DB, clock cố định |
| Preconditions | P1 PUBLISHED, A chưa bookmark; T1/T2/T3 tăng dần từng phút |
| Test data | PUT `{note:"  ghi chú cũ  "}` tại T1; PUT `{note:"mới",createIfMissing:false}` T2; DELETE; PUT `{note:"mới",createIfMissing:false}`; PUT `{note:"ghi chú cũ",createIfMissing:true}` T3 |
| Steps | Gọi năm lệnh đúng thứ tự, đọc DB sau mỗi lệnh. |
| Expected | T1 `201`, note đã trim, savedAt=T1; T2 `200`, note=mới, savedAt vẫn T1; DELETE 204 xoá dòng; lần sửa sau xoá `404 PRB-014` và không tạo dòng; Hoàn tác `201`, note cũ được gửi lại, savedAt=T3 và đúng một dòng. Chỉ bookmark của A đổi. |
| Cleanup | Xoá bookmark fixture |
| Evidence | Năm response, DB timeline |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC13-01 — Biên 1000/1001 ký tự và ghi chú trắng

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-13; DD F2 API §10.2/10.4 PB10-A2/B1 |
| Kind / priority | Boundary + abnormal, P1 |
| Environment | PUT bookmark, DB |
| Preconditions | A đã bookmark P1, note ban đầu `old`, savedAt T1 |
| Test data | 1000 ký tự `A`; 1001 ký tự `A`; chuỗi `"   "` |
| Steps | PUT ba lần với createIfMissing=false, đọc DB sau mỗi lần. |
| Expected | 1000 được 200, lưu đủ 1000; 1001 trả `400 PRB-001`, details.fields[0]=note, không sửa dòng; chuỗi trắng được 200, DB note NULL và response note null. savedAt giữ T1 ở cả lần thành công. |
| Cleanup | Dọn bookmark |
| Evidence | Độ dài payload, ba response, DB note/savedAt |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC14-01 — DELETE idempotent kể cả bài đã rút

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-14; DD F2 API §11 PB11-N1/A1/A2 |
| Kind / priority | Recovery, P1 |
| Environment | DELETE bookmark, DB |
| Preconditions | A có bookmark P1 kèm note; P1 sau đó UNPUBLISHED/deleted=true |
| Test data | DELETE `/me/bookmarks/{P1}` hai lần |
| Steps | Gọi hai DELETE, đọc DB sau từng lần. |
| Expected | Cả hai 204 No Content; sau lần đầu không còn dòng, note cũng mất; lần hai không tạo lỗi/ghi mới. Trạng thái rút bài không chặn xoá. |
| Cleanup | Dọn fixture |
| Evidence | Hai response và DB |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC15-01 — Đặt trạng thái đích giữ ghi chú, không nhân đôi

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-15; DD F2 API §12 PB12-N1/A1 |
| Kind / priority | Idempotency, P1 |
| Environment | PUT `/me/bookmarks/{id}/state`, GET workspace, DB |
| Preconditions | P1 PUBLISHED; A có bookmark P1 note=`giữ lại`, savedAt=T1 |
| Test data | PUT `{bookmarked:true}` hai lần; PUT `{bookmarked:false}` hai lần |
| Steps | Gọi bốn lệnh, kiểm DB/note/savedAt và workspace sau từng trạng thái. |
| Expected | Hai lần true đều `200 {bookmarked:true}`, DB đúng một dòng với note/savedAt cũ; hai lần false đều `200 {bookmarked:false}`, DB không còn dòng; workspace.bookmarked đổi true→false, không có tác động chéo B. |
| Cleanup | Dọn fixture |
| Evidence | Bốn response, DB counts, workspace responses |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC15-02 — Không tạo bookmark cho bài chưa xuất bản

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-15; DD F2 API §12.4 PB12-A2 |
| Kind / priority | Security, P0 |
| Environment | PUT bookmark state, DB |
| Preconditions | DRAFT P2 có thật nhưng UNPUBLISHED; A chưa bookmark |
| Test data | PUT `{bookmarked:true}` P2 |
| Steps | Gọi trực tiếp API, kiểm lỗi và DB. |
| Expected | `404 PRB-010`, không tạo bookmark hoặc làm lộ tên/nội dung P2. |
| Cleanup | Dọn fixture |
| Evidence | Response, DB absence |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC16-01 — Danh sách bookmark chỉ của người gọi, giữ bài đã rút

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-16; DD F2 API §9 PB09-N1/A1/B1; BR-21 |
| Kind / priority | Security, P0: không đọc bookmark người khác |
| Environment | GET `/me/bookmarks`, DB, FE USR0103 |
| Preconditions | A có P1/P2/P3, P3 đã rút; A SOLVED P1/P3; B có Q1 riêng; savedAt P3>P2>P1 |
| Test data | A GET; B GET `?userId={A}` |
| Steps | Gọi hai token, so ID/meta và toàn payload. |
| Expected | A thấy P3,P2,P1, P3 withdrawn=true, savedTotal=3, solvedCount=2; B chỉ Q1, savedTotal=1, không thấy note hoặc ID của A dù query userId=A. Không có route thay người dùng bằng query. |
| Cleanup | Dọn fixture |
| Evidence | Hai response, DB bookmarks, solve map, ảnh màn |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC16-02 — Phân trang, lọc và lỗi identity

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-16; DD F2 API §9 PB09-A2/B2 |
| Kind / priority | Boundary + degraded, P1 |
| Environment | GET bookmarks, SolveStatePort fault injection |
| Preconditions | A có 21 bookmark P01–P21 theo savedAt phút tăng; hai bài SOLVED; note của P21 NULL |
| Test data | GET page=1, page=9; GET `?status=SOLVED` khi cổng identity lỗi |
| Steps | Gọi hai trang, gây lỗi cổng, gọi filter. |
| Expected | Trang 1 có 20 dòng mới nhất, page=9 kẹp về trang 2 một dòng; `savedTotal=21`. Note NULL hiển thị là chuỗi rỗng trong list. Khi cổng lỗi vẫn 200 đủ 21 bookmark, `solveState=null` từng item, `solvedCount=null`, bỏ filter status thay vì trả rỗng hoặc số 0 giả. |
| Cleanup | Khôi phục cổng, dọn fixture |
| Evidence | Ba response, DB timeline, fault trace |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC17-01 — Vector điểm dùng để xếp và bài chưa giải đứng trước

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-17; DD F2 API §13 PB13-N1; BR-23 |
| Kind / priority | Normal, P1: điểm FE không được tính khác server |
| Environment | GET `/suggested-problems`, SolveStatePort, F4 P5, FE USR0601 |
| Preconditions | Topic ARRAY có total=5, solved=1, submissionCount=10, acRate=0.20 → score=20; TREE có total=5, solved=4, submissionCount=10, acRate=0.80 → score=80. P1 ARRAY UNSOLVED, P2 TREE UNSOLVED, P3 ARRAY SOLVED, tất cả PUBLISHED. |
| Test data | GET gợi ý bằng A |
| Steps | Gọi API, so topicScores/ID order với phép tính và ảnh dashboard. |
| Expected | 200, `rankingAvailable=true`; ARRAY score=20, TREE score=80; P1 đứng trước P2, P3 đứng sau hai bài chưa giải dù thuộc ARRAY; FE hiển thị đúng vector topicScores từ API, không tự tính lại. Tối đa 7 item, không có bài rút. |
| Cleanup | Dọn fixture |
| Evidence | F1/F4 outputs, response, phép tính, ảnh màn |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC17-02 — Chủ đề chưa nộp không có điểm, cổng lỗi dùng thứ tự dự phòng

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-17; DD F2 API §13 PB13-A1/B1; BR-23 |
| Kind / priority | Degraded + boundary, P1 |
| Environment | GET gợi ý, F1/F4 port fault injection |
| Preconditions | Có 8 bài PUBLISHED mã P01–P08, topic UNUSED chưa từng nộp; A UNSOLVED P01–P07, SOLVED P08 |
| Test data | GET bình thường; F4 P5 lỗi rồi GET; F1 SolveStatePort lỗi rồi GET; filter topic không có bài |
| Steps | Gọi bốn lần, kiểm vector/ranking/order. |
| Expected | Bình thường tối đa 7 item, UNUSED không có score=0 giả. Khi F4 lỗi: 200, `rankingAvailable=false`, `topicScores=[]`, UNSOLVED trước SOLVED rồi code tăng. Khi F1 lỗi: 200, solveState=null, xếp chỉ code tăng, không đoán bài chưa giải. Topic rỗng trả `items=[]`. FE báo chưa đủ dữ liệu, không hiển thị điểm giả. |
| Cleanup | Khôi phục cổng, dọn fixture |
| Evidence | Bốn response, fault traces, ảnh màn |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC18-01 — Tổng bài xuất bản và bốn mức độ khó

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-18; DD F2 API §14 PB14-N1/A1/B1 |
| Kind / priority | Normal + boundary, P1 |
| Environment | GET `/catalog/summary`, DB levels/problems/topics |
| Preconditions | Seed EASY/MEDIUM/HARD, admin thêm EXPERT sortOrder=4 không bài. Có 2 bài EASY, 1 MEDIUM PUBLISHED; một EASY DRAFT, một HARD deleted; P1 gắn ARRAY và HASH, P2 gắn ARRAY. |
| Test data | GET summary bằng A |
| Steps | Gọi API, đối chiếu từng group DB. |
| Expected | `totalPublished=3`; byLevel có bốn mức theo sortOrder với count 2,1,0,0, tổng bằng 3; DRAFT/deleted không được đếm. byTopic ARRAY=2, HASH=1 và có thể cộng lớn hơn totalPublished do một bài nhiều chủ đề; không ghi DB. |
| Cleanup | Dọn fixture |
| Evidence | Response, DB counts |
| Design review | Đã review; chưa thực thi |
