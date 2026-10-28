# Test case QA — F2: danh mục độ khó và chủ đề

> 14 ca thiết kế cho AC-19–28; chưa thực thi. Nguồn: TDD F2 AC-19–28; DD F2 API §15–24; logic BR-22/27. A3 là ADMIN đủ quyền, A2 có đủ Function PROBLEM_AUTHORING nhưng không phải ADMIN, A1 là học viên. Đối chiếu DB `problem_levels`, `topics`, quan hệ bài, và AuditLogPort trước/sau từng lệnh.

### TC-F2-AC19-01 — Ba vai trò đọc danh mục, chỉ ADMIN nhận count

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-19; DD F2 API §15/20 PB15-N1/A1/B1, PB20-N1/A1 |
| Kind / priority | Normal + security, P1 |
| Environment | GET `/levels`, `/topics`, DB |
| Preconditions | Level EASY/MEDIUM/HARD sortOrder 1/2/3, thêm EXPERT sortOrder 4 chưa bài; Topics ARRAY/TREE sortOrder 2/1; có một bài deleted vẫn tham chiếu EASY và ARRAY |
| Test data | A1/A2/A3 GET hai route; A2/A3 thêm `includeCounts=true` |
| Steps | Gọi tám request, so thứ tự và count với DB. |
| Expected | Cả ba vai trò 200 theo sort_order; A2 xin count vẫn `problemCount=null`; A3 có count đúng, gồm cả bài deleted tham chiếu, EXPERT=0. Không có quyền ghi được suy ra từ quyền đọc; không ghi DB. |
| Cleanup | Dọn fixture |
| Evidence | Responses, DB sort/count |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC20-01 — Tạo mức và chặn tên trùng không phân biệt hoa thường

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-20; DD F2 API §16 PB16-N1/A2; BR-22/27 |
| Kind / priority | Normal + concurrency, P1 |
| Environment | POST `/levels`, DB, AuditLogPort |
| Preconditions | EASY/MEDIUM/HARD tồn tại, audit N |
| Test data | A3 POST `{displayName:" Rất khó "}` rồi `{displayName:"rất khó"}` |
| Steps | Gọi hai lần, đọc level/audit và GET danh mục. |
| Expected | Lần đầu 201 với `code=RAT-KHO`, displayName=`Rất khó`, sortOrder=4, audit N+1 chứa before null/after mức mới; lần sau `409 PRB-022`, chỉ một mức và một audit. |
| Cleanup | Xoá mức fixture, dọn audit |
| Evidence | Hai response, DB/GET/audit |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC20-02 — Biên tên 60/61 và tên rỗng

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-20; DD F2 API §16 PB16-B1 |
| Kind / priority | Boundary, P1 |
| Environment | POST `/levels`, DB/audit |
| Preconditions | A3 đủ quyền, tên 60 chữ `A` chưa có; audit N |
| Test data | Tên 60, 61 ký tự; chuỗi chỉ khoảng trắng |
| Steps | POST ba body độc lập, kiểm DB/audit. |
| Expected | 60 ký tự được 201 và một audit; 61 và trắng trả `400 PRB-001`, không tạo mức hoặc audit thêm. |
| Cleanup | Dọn mức mới/audit |
| Evidence | Ba response, DB/audit counts |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC21-01 — A2 có Function vẫn không ghi được tám route danh mục

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-21; DD F2 API §16–19, §21–24; phân quyền ba lớp |
| Kind / priority | Security, P0 |
| Environment | Tám POST/PATCH/PUT/DELETE danh mục, DB/audit |
| Preconditions | A2 có CREATE/UPDATE/DELETE PROBLEM_AUTHORING nhưng baseCategory không phải ADMIN; body/id của mỗi route đều hợp lệ; snapshot DB/audit N |
| Test data | POST level/topic, PATCH level/topic, PUT level/topic order, DELETE level/topic |
| Steps | Gọi cả tám bằng JWT A2, đọc lại toàn danh mục/audit. |
| Expected | Mọi lệnh `403 PRB-004`; không thêm/sửa/xoá/thay sort_order, audit vẫn N. Không lọt dữ liệu từ các lệnh quản trị vào response. |
| Cleanup | Không có thay đổi |
| Evidence | Tám request/response, DB/audit diff |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC22-01 — Audit lỗi rollback tạo mức, retry thành công

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-22; DD F2 API §16 PB16-A3; logic BR-27 |
| Kind / priority | Recovery + atomicity, P0 |
| Environment | POST `/levels`, AuditLogPort fault injection, DB |
| Preconditions | Chưa có mức `TEST-AUDIT`, audit N; A3 đủ quyền |
| Test data | POST `{displayName:"Test Audit"}` khi cổng audit ném lỗi, rồi retry sau khôi phục |
| Steps | Gây lỗi, gọi POST, đọc DB/audit; khôi phục và gọi lại. |
| Expected | Lần đầu thất bại và không có dòng level/audit; retry 201, đúng một mức `TEST-AUDIT` và một audit. Không có trạng thái tạo mức nhưng thiếu dấu vết. |
| Cleanup | Dọn fixture |
| Evidence | Hai response, transaction trace, DB/audit |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC23-01 — Đổi tên giữ code, lặp lại không audit

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-23; DD F2 API §17 PB17-N1/B1 |
| Kind / priority | Normal + idempotency, P1 |
| Environment | PATCH `/levels/{id}`, GET levels, DB/audit |
| Preconditions | HARD tên `Khó`, code HARD, audit N |
| Test data | PATCH `{displayName:"Nâng cao"}` hai lần |
| Steps | Gọi hai lần, GET lại, kiểm DB/audit. |
| Expected | Cả hai 200, code HARD không đổi; lần đầu đổi tên và audit N+1 có before Khó/after Nâng cao; lần hai không ghi DB/audit thêm. Danh sách dùng tên mới. |
| Cleanup | Khôi phục tên |
| Evidence | Hai response, GET, DB/audit |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC24-01 — Đổi thứ tự đầy đủ có hiệu lực ở danh sách bài

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-24; DD F2 API §18 PB18-N1 |
| Kind / priority | Normal, P1 |
| Environment | PUT `/levels/order`, GET `/levels`, GET `/problems?sort=level`, DB/audit |
| Preconditions | EASY/MEDIUM/HARD sortOrder 1/2/3, mỗi mức một bài PUBLISHED; audit N |
| Test data | `{orderedIds:[HARD,EASY,MEDIUM]}` |
| Steps | PUT, gọi hai GET và kiểm DB/audit. |
| Expected | PUT 200 trả cả ba theo HARD/EASY/MEDIUM, sortOrder 1/2/3; hai GET phản ánh thứ tự mới, audit N+1 chứa thứ tự cũ/mới. Không thay levelId của bài. |
| Cleanup | Khôi phục thứ tự |
| Evidence | PUT/GET, DB/audit, ảnh cột độ khó |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC24-02 — Tập ID thiếu/trùng/lạ bị chặn nguyên khối

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-24; DD F2 API §18 PB18-A1 |
| Kind / priority | Abnormal, P1 |
| Environment | PUT levels order, DB/audit |
| Preconditions | Ba level EASY/MEDIUM/HARD, audit N |
| Test data | `[EASY,HARD]`; `[EASY,EASY,HARD]`; `[EASY,MEDIUM,HARD,UNKNOWN]` |
| Steps | Gửi ba request riêng, kiểm DB/audit sau mỗi lần. |
| Expected | Mỗi request `422 PRB-049`, sort_order giữ nguyên 1/2/3, audit N. |
| Cleanup | Không thay đổi |
| Evidence | Ba response, DB/audit |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC25-01 — Mức còn bài đã xoá mềm vẫn không xoá được

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-25; DD F2 API §19 PB19-A1; BR-22 |
| Kind / priority | Integrity, P0: giữ khoá ngoại lịch sử |
| Environment | DELETE `/levels/{id}`, DB/audit |
| Preconditions | EASY và HARD tồn tại; một bài deleted=true vẫn tham chiếu HARD, không bài khác; audit N |
| Test data | A3 DELETE HARD |
| Steps | Gọi lệnh, đọc DB và audit. |
| Expected | `422 PRB-032`, details.reason=IN_USE, referencingProblemCount=1; HARD và bài vẫn còn, audit N. |
| Cleanup | Dọn fixture |
| Evidence | Response, FK/DB, audit |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC25-02 — Không xoá mức cuối cùng

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-25; DD F2 API §19 PB19-A2 |
| Kind / priority | Boundary, P0 |
| Environment | DELETE level, DB/audit |
| Preconditions | Chỉ có EASY, không có bài tham chiếu; audit N |
| Test data | A3 DELETE EASY |
| Steps | Gọi API, kiểm DB/audit. |
| Expected | `422 PRB-033`, details.reason=LAST_LEVEL; EASY còn, audit N. |
| Cleanup | Khôi phục seed |
| Evidence | Response, DB/audit |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC26-01 — Hai admin xoá hai mức không bài đồng thời

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-26; DD F2 API §19 PB19-N1/B1 |
| Kind / priority | Concurrency, P0: luôn còn một mức |
| Environment | Hai HTTP worker, DB row locks, AuditLogPort |
| Preconditions | Chỉ có L1/L2, đều không bài; A3 và admin B đủ quyền, audit N |
| Test data | Hai DELETE L1/L2 được đồng bộ bắt đầu cùng lúc |
| Steps | Gửi song song có barrier, chờ hai kết quả, đọc DB/audit và GET levels. |
| Expected | Đúng một 204 và một `422 PRB-033`; đúng một level còn, audit N+1 cho ID đã xoá. Không có thời điểm commit mà bảng rỗng. |
| Cleanup | Khôi phục seed |
| Evidence | Hai response, lock/transaction trace, DB/audit |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC27-01 — Tạo/đổi tên chủ đề và chặn xoá khi còn tham chiếu

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-27; DD F2 API §21/22/24 PB21-N1, PB22-N1, PB24-A1 |
| Kind / priority | Normal + integrity, P1 |
| Environment | POST/PATCH/DELETE topics, DB/audit |
| Preconditions | GRAPH còn một bài UNPUBLISHED gắn vào; topic EMPTY không bài; audit N |
| Test data | POST `{name:"Quy hoạch động"}`; PATCH topic mới `{name:"DP"}`; DELETE GRAPH; DELETE EMPTY |
| Steps | Gọi bốn lệnh, kiểm code/sort_order/quan hệ/audit sau mỗi lần. |
| Expected | POST 201 code QUY-HOACH-DONG xếp cuối, audit N+1; PATCH 200 code giữ nguyên, tên DP, audit N+2; DELETE GRAPH `422 PRB-031`, referencingProblemCount=1, không audit; DELETE EMPTY 204, audit N+3. Quan hệ bài GRAPH còn. |
| Cleanup | Dọn topic/bài fixture |
| Evidence | Bốn response, DB/audit timeline |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC27-02 — Trùng tên và bài xoá mềm vẫn chặn xoá chủ đề

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-27; DD F2 API §21/24 PB21-A1, PB24-B1 |
| Kind / priority | Abnormal + integrity, P1 |
| Environment | POST/DELETE topics, DB/audit |
| Preconditions | Topic Mảng đã có; topic TREE chỉ gắn một bài deleted=true; audit N |
| Test data | POST `{name:"mảng"}`; DELETE TREE |
| Steps | Gọi hai lệnh, đọc DB/audit. |
| Expected | POST `409 PRB-021`; DELETE `422 PRB-031`, referencingProblemCount=1; không mất topic/quan hệ, audit N. |
| Cleanup | Dọn fixture |
| Evidence | Hai response, DB/audit |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC28-01 — Sắp xếp chủ đề và chặn tập ID thiếu

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-28; DD F2 API §23 PB23-N1/A1 |
| Kind / priority | Normal + abnormal, P1 |
| Environment | PUT `/topics/order`, GET `/topics`, DB/audit |
| Preconditions | Topic ARRAY/TREE/GRAPH sortOrder 1/2/3; audit N |
| Test data | `[GRAPH,ARRAY,TREE]`, rồi `[GRAPH,TREE]` |
| Steps | PUT hợp lệ, GET lại; PUT thiếu ID, GET/DB/audit lần nữa. |
| Expected | Lần đầu 200 trả ba topic đúng thứ tự, GET và DB theo 1/2/3 mới, audit N+1; lần hai `422 PRB-049`, thứ tự vừa lưu giữ nguyên, không thêm audit. |
| Cleanup | Khôi phục thứ tự |
| Evidence | Hai PUT, hai GET, DB/audit |
| Design review | Đã review; chưa thực thi |
