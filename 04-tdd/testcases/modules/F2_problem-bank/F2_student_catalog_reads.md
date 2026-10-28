# Test case QA — F2: học viên đọc kho bài và testcase mẫu

> 15 ca thiết kế cho AC-01–11; chưa thực thi. Nguồn: TDD F2 AC-01–11; DD F2 API §2–8, logic BR-01/13/23. Dùng JWT học viên A, học viên B khi kiểm cách ly. Mọi bài có ID/mã riêng; fixture DB và trạng thái từ cổng F1/F4 được cố định trước mỗi ca. Kiểm JSON thô, không chỉ UI.

### TC-F2-AC01-01 — Chỉ bài xuất bản chưa xoá lên danh sách

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-01; DD F2 API §2 PB02-N1/A1; logic BR-01 |
| Kind / priority | Security + normal, P0: bài nháp không được lộ |
| Environment | GET `/problems`, DB problem, FE USR0101 |
| Preconditions | Có 13 bài PUBLISHED không deleted mã P01–P13, một UNPUBLISHED mã DRAFT99, một PUBLISHED deleted mã DEL99; solve-state port hoạt động |
| Test data | GET không query; GET `?q=DRAFT99`; GET `?q=DEL99` |
| Steps | Gọi ba URL, so ID với DB, xem danh sách. |
| Expected | URL đầu 200, đúng 12 dòng theo `code` tăng, page meta đếm 13; hai URL q đều items rỗng, không lộ tên/ID/nội dung bài ngoài phạm vi. Không ghi DB. |
| Cleanup | Dọn fixture |
| Evidence | Ba response, DB trạng thái, ảnh màn |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC02-01 — Bộ lọc difficulty/topic/status giao nhau

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-02; DD F2 API §2 PB02-N2 |
| Kind / priority | Normal, P1: không ghép nhầm OR |
| Environment | GET `/problems`, SolveStatePort, DB |
| Preconditions | A đã SOLVED P1(Level EASY, Topic ARRAY), P2(EASY,TREE), P3(HARD,ARRAY); P4(EASY,ARRAY) UNSOLVED; tất cả PUBLISHED |
| Test data | `difficulty=EASY&topic={ARRAY}&status=SOLVED` |
| Steps | Gọi API, đối chiếu ba thuộc tính từng bài với fixture. |
| Expected | 200, chỉ P1; không có P2/P3/P4; `solveStateAvailable=true`, P1 `solveState=SOLVED`; không ghi DB. |
| Cleanup | Dọn fixture |
| Evidence | Request/response, solve map, DB join |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC02-02 — Tham số lạ dùng mặc định, trang vượt biên bị kẹp

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-02; DD F2 API §2 PB02-A2/B1 |
| Kind / priority | Boundary + abnormal, P1 |
| Environment | GET `/problems`, DB |
| Preconditions | Có 14 bài PUBLISHED mã P01–P14 |
| Test data | `difficulty=khong-co&sort=abc&page=-4`; rồi `page=99` |
| Steps | Gọi hai URL, so với GET mặc định/trang cuối. |
| Expected | Request lạ trả 200 như mặc định, 12 item trang đầu theo code; page=99 trả 2 item P13/P14 của trang cuối, không lỗi 400. Không đưa bài nháp vào vì filter bị bỏ qua. |
| Cleanup | Dọn fixture |
| Evidence | Hai response và page meta |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC03-01 — Identity lỗi vẫn trả kho bài, bỏ lọc trạng thái

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-03; DD F2 API §2 PB02-A3; BR-23 |
| Kind / priority | Degraded, P1: không biến dữ liệu thiếu thành UNSOLVED |
| Environment | GET `/problems`, SolveStatePort fault injection |
| Preconditions | P1/P2 PUBLISHED, A SOLVED P1; cổng trạng thái trả timeout/503 |
| Test data | GET `?status=SOLVED` và GET không status |
| Steps | Gọi hai URL khi cổng lỗi, xem JSON. |
| Expected | Cả hai 200 và vẫn có P1/P2 theo bộ lọc còn lại; `solveStateAvailable=false`, mọi item `solveState=null`, không gán UNSOLVED. Không ghi DB. |
| Cleanup | Khôi phục cổng, dọn fixture |
| Evidence | Fault trace, hai response |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC04-01 — Chưa có thống kê thì acRate null ở hai màn

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-04; DD F2 API §2 PB02-B2, §6 |
| Kind / priority | Boundary, P1: không dựng 0% giả |
| Environment | GET `/problems`, GET `/problems/{id}/workspace`, DB |
| Preconditions | P1 PUBLISHED nhưng không có dòng `problem_stats`; có bookmark A để workspace vẫn có dữ liệu |
| Test data | GET danh sách và workspace P1 |
| Steps | Gọi hai API, kiểm `acRate` và DB. |
| Expected | Hai response 200 với `acRate=null`, không có `0`; các trường khác vẫn trả, không tự tạo dòng stats. |
| Cleanup | Dọn fixture |
| Evidence | Hai response, DB absence query |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC05-01 — Bốc ngẫu nhiên chỉ từ tập chưa giải

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-05; DD F2 API §3 PB03-N2/A1/A2 |
| Kind / priority | Normal + security, P1 |
| Environment | GET `/problems/random`, SolveStatePort, DB |
| Preconditions | P1 SOLVED, P2 ATTEMPTED, P3 UNSOLVED, P4 DRAFT, P5 deleted; cùng chủ đề ARRAY, khác mã |
| Test data | `status=NOT_SOLVED&topic={ARRAY}`; rồi topic EMPTY không có bài |
| Steps | Gọi request đầu 20 lần độc lập, kiểm tập ID; gọi tập rỗng. |
| Expected | Mỗi lần đầu 200 và chỉ P2 hoặc P3, không bao giờ P1/P4/P5; tập rỗng 200 `{problemId:null}`, không lỗi, không ghi DB. |
| Cleanup | Dọn fixture |
| Evidence | 20 response, solve map, response rỗng |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC06-01 — Tiến độ chủ đề chỉ tính bài còn xuất bản

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-06; DD F2 API §4 PB04-N1/B1 |
| Kind / priority | Normal + boundary, P1 |
| Environment | GET `/topics/progress`, SolveStatePort, DB |
| Preconditions | ARRAY có 5 bài PUBLISHED và một bài đã rút; A SOLVED 2 trong 5 và bài đã rút; topic EMPTY không có bài xuất bản |
| Test data | GET `/topics/progress` bằng A |
| Steps | Gọi API, đối chiếu nối problem_topics/problems và solve map. |
| Expected | 200, ARRAY `solved=2,total=5`, EMPTY `solved=0,total=0`; bài rút không ở tử/mẫu, mọi topic theo sort_order; `solveStateAvailable=true`. Không ghi DB. |
| Cleanup | Dọn fixture |
| Evidence | Response, DB topic/problem, solve map |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC06-02 — Identity lỗi thì solved null, total vẫn đúng

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-06; DD F2 API §4 PB04-A1 |
| Kind / priority | Degraded, P1 |
| Environment | API §4, SolveStatePort fault injection |
| Preconditions | ARRAY có 5 bài PUBLISHED; cổng trạng thái lỗi |
| Test data | GET `/topics/progress` |
| Steps | Gọi API, kiểm từng topic. |
| Expected | 200, ARRAY `total=5`, `solved=null`; `solveStateAvailable=false`; không hiển thị 0 bài giải như giá trị thật. |
| Cleanup | Khôi phục cổng, dọn fixture |
| Evidence | Response và fault trace |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC07-01 — Bài đang giao của lớp mình, giới hạn ba nhóm

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-07; DD F2 API §5 PB05-N1/A1/B1; logic BR-01 |
| Kind / priority | Normal + scope, P0: không lộ lớp khác |
| Environment | GET `/me/class-assignments`, StudentEnrollmentPort, F4 state port, DB |
| Preconditions | A đang ở L1–L4, không ở L5; mỗi lớp có một bài PUBLISHED giao tại ngày 04,03,02,01/10 tương ứng; L1 thêm một bài giao muộn hơn; L1 có bài đã gỡ, L2 có thêm bài đã rút; L5 có bài đang giao |
| Test data | GET không query; GET `?classId={L5}` |
| Steps | Gọi hai URL, đối chiếu lớp/bài và thứ tự. |
| Expected | Chỉ ba nhóm gần nhất L1,L2,L3 theo assignedAt mới nhất; trong L1 bài mới nhất trước. Không bài gỡ, bài rút hay L5; query classId không đổi phạm vi/nhóm vì API không nhận tham số này. Không ghi DB. |
| Cleanup | Dọn fixture |
| Evidence | Hai response, enrollment/assignment rows |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC07-02 — Cổng lớp và trạng thái lỗi theo hai cách riêng

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-07; DD F2 API §5 PB05-A2/B2; §5.5 suy giảm êm |
| Kind / priority | Degraded + boundary, P1 |
| Environment | API §5, EnrollmentPort và F4 state port fault injection |
| Preconditions | A đang ở L1, bài P1 được giao hôm qua; A Accepted P1 trước giờ giao, chưa Accepted sau giờ giao |
| Test data | GET bình thường; sau đó lỗi F4 state port; sau đó lỗi enrollment port |
| Steps | Gọi ba lần với cùng fixture, kiểm groups/solveState. |
| Expected | Bình thường dòng P1 `solveState=UNSOLVED` vì Accepted trước giao không tính; F4 lỗi: vẫn có L1/P1 nhưng `solveState=null`; enrollment lỗi: `200 {groups:[]}`. Không lấy Accepted toàn cục để gán SOLVED; không ghi DB. |
| Cleanup | Khôi phục hai cổng, dọn fixture |
| Evidence | Ba response, port faults, timestamps giao/nộp |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC08-01 — Workspace có đề/ví dụ và bookmark đúng chủ

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-08; DD F2 API §6 PB06-N1/A2/B1; BR-01 |
| Kind / priority | Normal + security, P0: DTO không mang dữ liệu chấm |
| Environment | GET `/problems/{id}/workspace`, DB, FE USR0102 |
| Preconditions | P1 PUBLISHED có ba workedExamples position 2,1,3; constraintsMd rỗng; A có bookmark, B không; có referenceSolution, aiBrief, Sample/Hidden nội bộ chứa marker SECRET08 |
| Test data | A và B GET workspace P1 |
| Steps | Gọi hai token, kiểm thứ tự ví dụ, toàn payload và màn. |
| Expected | Cả hai 200, đề/ràng buộc/chủ đề/format đúng; ví dụ theo order 1,2,3, constraintsMd `""`, FE không vẽ khối rỗng. A `bookmarked=true`, B false. Không có testcase, solution, aiBrief, SECRET08 hay giới hạn hiệu dụng trong JSON. |
| Cleanup | Dọn fixture |
| Evidence | Hai response, DB examples/bookmarks, ảnh màn |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC09-01 — Ba route che bài nháp, đã rút và ID không tồn tại

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-09; DD F2 API §6.4, §7.4, §8.4; BR-01 |
| Kind / priority | Security, P0: không lộ sự tồn tại hoặc testcase |
| Environment | Ba GET workspace/overview/sample-testcases, DB |
| Preconditions | DRAFT có nội dung và testcase, WITHDRAWN deleted=true, UUID MISSING không có; JWT A |
| Test data | Mỗi ID gọi đủ ba route (9 request) |
| Steps | Gọi chín request, so nguyên status/code/message/body, tìm marker Hidden. |
| Expected | Cả chín trả `404 PRB-010` với thân lỗi như nhau; không có tên, nguồn, testcase, object key hay marker của bài có thật; không ghi DB. |
| Cleanup | Dọn fixture |
| Evidence | Chín response, DB trạng thái |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC10-01 — Overview chỉ đếm Hidden đã duyệt

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-10; DD F2 API §7 PB07-N1/A2/B1 |
| Kind / priority | Security + normal, P0 |
| Environment | GET `/problems/{id}/overview`, DB |
| Preconditions | P1 PUBLISHED có 6 Hidden approved, 2 Hidden chưa approved, 2 Sample approved; workedExample có explanationMd; Hidden input marker HIDDEN10 |
| Test data | GET overview P1; bài P2 PUBLISHED không có Hidden approved |
| Steps | Gọi hai ID, so count và toàn payload. |
| Expected | P1 `hiddenTestcaseCount=6`, P2 bằng 0; examples có explanationMd theo position. Không có testcaseId/input/expected/objectKey hay HIDDEN10 trong JSON; query chỉ đếm Hidden, không đọc nội dung. |
| Cleanup | Dọn fixture |
| Evidence | Hai response, DB testcase counts, query trace |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC11-01 — Sample INLINE/MINIO chỉ trả hai ca đã duyệt

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-11; DD F2 API §8 PB08-N1/A1, BR-13 |
| Kind / priority | Security, P0: không lộ Hidden qua danh sách hay URL |
| Environment | GET `/problems/{id}/sample-testcases`, TestcaseObjectStorePort, F4 result fixture |
| Preconditions | P1 có Sample S1 INLINE approved position 2, S2 MINIO approved position 1, Sample S3 chưa approved, 6 Hidden approved; Hidden key/contents có marker HIDDEN11. F4 kết quả Sample S1/S2 có đúng testcaseId. |
| Test data | GET sample-testcases P1 |
| Steps | Gọi API, kiểm IDs/thứ tự/URL, so F4 IDs và thử URL S2 trong cửa sổ 5 phút. |
| Expected | 200, đúng S2 rồi S1 với `order` tương ứng; S2 `input=null` và `inputDownloadUrl` chỉ tới object S2, hiệu lực 5 phút, không có object_key. S1 INLINE có input trực tiếp; `testcaseId` cả hai khớp F4. Không có S3/Hidden, HIDDEN11 hay presigned URL Hidden ở bất kỳ trường nào. |
| Cleanup | Dọn testcase/object fixture |
| Evidence | JSON, DB visibility/approval, F4 sample IDs, URL metadata đã che chữ ký |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC11-02 — Hidden object không được presign khi đọc Sample

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-11; DD F2 API §8.5/8.7 PB08-A1; BR-13 |
| Kind / priority | Security, P0: lọc ở truy vấn trước khi ký URL |
| Environment | API §8, MinIO presign port có call trace |
| Preconditions | P1 có một Sample MINIO approved và một Hidden MINIO approved; key khác nhau, trace presign rỗng |
| Test data | GET sample-testcases P1 |
| Steps | Gọi API, đếm presign calls, kiểm JSON và truy vấn testcase. |
| Expected | Chỉ một presign GET cho object Sample; không có call với key Hidden, không có ID/key/URL/nội dung Hidden trong response. Điều kiện `visibility=SAMPLE AND approved=true` áp ngay ở truy vấn. |
| Cleanup | Dọn object fixture |
| Evidence | JSON, query trace, presign call trace đã che chữ ký |
| Design review | Đã review; chưa thực thi |
