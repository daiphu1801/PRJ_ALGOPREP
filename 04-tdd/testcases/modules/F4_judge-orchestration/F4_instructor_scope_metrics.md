# Test case QA — F4: phạm vi bài nộp và số liệu giảng viên

> 15 ca thiết kế cho AC-41–47; chưa thực thi. Nguồn: TDD F4 AC-41–47, DD F4 API §13–15, §26.1 và logic BR-JD-10/13. Mọi giờ dưới đây ở UTC+7. `G` phụ trách L1/L2; `M` thuộc giảng viên khác; `H` là học viên. Các ca dùng fixture tách biệt, gọi API bằng JWT G trừ khi ghi khác. Đối chiếu cả payload thô, DB và màn.

| AC | Nhánh | ID | Review | Thực thi |
| :--- | :--- | :--- | :--- | :--- |
| 41 | Danh sách, cửa sổ giao bài, filter/trang, upstream lỗi | TC-F4-AC41-01–03 | Đã review | Chưa thực hiện |
| 42 | Rời lớp, vào lại | TC-F4-AC42-01–02 | Đã review | Chưa thực hiện |
| 43 | Hai lớp, lọc sau chọn | TC-F4-AC43-01 | Đã review | Chưa thực hiện |
| 44 | Lớp khác/sai UUID, thiếu quyền | TC-F4-AC44-01–02 | Đã review | Chưa thực hiện |
| 45 | Chi tiết trong/ngoài phạm vi, URL giả, Hidden | TC-F4-AC45-01–03 | Đã review | Chưa thực hiện |
| 46 | Lịch sử, phạm vi và lỗi | TC-F4-AC46-01–02 | Đã review | Chưa thực hiện |
| 47 | Tuần đủ, rỗng, cache/phạm vi | TC-F4-AC47-01–02 | Đã review | Chưa thực hiện |

## Fixture gốc

- L1 do G phụ trách; H ghi danh từ `2026-09-01T00:00:00+07:00`. Bài P1 giao L1 lúc `2026-10-01T00:00:00+07:00`; P2 không giao. `S1` của H cho P1 nộp `2026-10-10T10:00+07`, ACCEPTED, 5/5. `S2` cho P2 nộp lúc `10:01`, WRONG_ANSWER. Hai nguồn lần lượt chứa marker `SOURCE_S1`, `SOURCE_S2`; chi tiết Hidden có `HIDDEN_47` chỉ trong fixture nội bộ.
- Dữ liệu lớp, ghi danh và lần giao được cấp qua `ClassScopeReadPort`/`ClassAssignmentReadPort`. Dữ liệu bài nộp nằm ở `judge.submissions`. Sau mỗi ca hoàn nguyên clock/cổng và dọn fixture riêng. Bằng chứng: request/response, hàng DB, dữ liệu hai cổng, ảnh FE nếu ca có kiểm màn.

### TC-F4-AC41-01 — Danh sách chỉ có bài được giao trong phạm vi

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-41; API §13.3/13.7 T-LI-01/02/07; BR-JD-10 |
| Kind / priority | Normal + security, P0: không lộ bài tự luyện |
| Environment | API #10, DB judge, hai read port, FE INS0206 |
| Preconditions | Fixture gốc; thêm 21 bài nộp P1 của H tại 21 phút liên tiếp sau S1, đều trong thời gian giao |
| Test data | `GET /instructor/submissions?page=1`, `page=2` |
| Steps | Gọi hai trang; so ID/thứ tự và toàn JSON với DB; mở danh sách. |
| Expected | HTTP 200; `totalCount=22`, trang 1 đúng 20 dòng mới nhất, trang 2 còn 2; mọi dòng có student/class/problem/name/language/status/count/submittedAt, `leftClass=false`; chỉ P1, không S2. Thứ tự `submittedAt` giảm; không có `sourceCode`, `compileErrorMessage`, `HIDDEN_47` hay dòng testcase ở bất kỳ item/JSON nào. FE không cần gọi cổng riêng để ghép tên từng dòng. |
| Cleanup | Dọn 23 bài nộp và fixture lớp/bài |
| Evidence | Hai response, DB timeline, ảnh màn và request trace |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC41-02 — Khoảng giao bài gồm đầu, loại cuối

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-41; API §13.7 T-LI-09; BR-JD-10 bước 3b |
| Kind / priority | Boundary, P0: không gán bài tự luyện thành bài lớp |
| Environment | API #10, DB, ClassAssignmentReadPort |
| Preconditions | P1 giao L1 tại `10:00` và gỡ mềm tại `11:00` cùng ngày; H nộp P1 tại `09:59:59`, `10:00:00`, `10:59:59`, `11:00:00` |
| Test data | Bốn submission có ID `PRE`, `START`, `LAST`, `END` |
| Steps | GET danh sách G, đối chiếu bốn ID với mốc giao/gỡ. |
| Expected | Chỉ `START` và `LAST` có mặt; `PRE`, `END` không có. Bài đã gỡ mềm vẫn được tra lịch sử trong khoảng `[assignedAt,removedAt)`; không có nguồn/testcase. |
| Cleanup | Dọn fixture |
| Evidence | Assignment timestamps, DB rows, response |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC41-03 — Bộ lọc kết quả/từ khoá và lỗi cổng phạm vi

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-41; API §13.2/13.3; BR-JD-10 bước 5/6 và suy giảm êm |
| Kind / priority | Normal + degraded, P1: lọc đúng, lỗi không mở rộng phạm vi |
| Environment | API #10, ClassScopeReadPort có thể lỗi |
| Preconditions | G có 5 bài P1 trong phạm vi: ACCEPTED của H, WRONG_ANSWER của `K`, SYSTEM_ERROR của H, PENDING của K, COMPILE_ERROR của H; tên bài `Two Sum`, tên H `Hoa`; mọi submission có ID khác nhau |
| Test data | `result=failed&q=  tWo  `; `result=accepted&q=hOa`; `result=unknown`; sau đó cưỡng bức ClassScopeReadPort 503 |
| Steps | Gọi từng request; so tập ID; bật lỗi cổng và gọi lại. |
| Expected | `failed` chỉ gồm WRONG_ANSWER và COMPILE_ERROR; `accepted&q=hOa` chỉ gồm bài của H; `unknown` được chuẩn về all và gồm 5. Khi không có scope, API trả 503 chung, không trả danh sách rộng hay dữ liệu lớp khác; không ghi DB. |
| Cleanup | Khôi phục cổng, dọn fixture |
| Evidence | Bốn response, lỗi cổng và DB scope |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC42-01 — Rời lớp: mốc cuối còn, sau mốc bị loại

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-42; API §13.7 T-LI-03; BR-JD-10 bước 3c/7 |
| Kind / priority | Boundary + security, P0: không lộ bài sau khi rời |
| Environment | API #10, FE INS0206, ClassScopeReadPort |
| Preconditions | H có `leftAt=2026-10-15T12:00:00+07:00`; P1 còn giao; ba bài nộp lúc 11:59:59, 12:00:00, 12:00:01 |
| Test data | ID `BEFORE`, `AT`, `AFTER` |
| Steps | GET danh sách G, xem dòng và liên kết hồ sơ. |
| Expected | `BEFORE` và `AT` có mặt, `leftClass=true`; `AFTER` không có; FE ghi “Đã rời lớp” và tên ở hai dòng không dẫn sang hồ sơ. Không hiện nguồn/chi tiết Hidden. |
| Cleanup | Dọn fixture |
| Evidence | Scope timestamps, JSON, ảnh màn |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC42-02 — Vào lại dùng ghi danh gốc

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-42; API §13.7 T-LI-04; BR-JD-10 bước 3c |
| Kind / priority | Recovery, P1: giữ quy tắc khoảng hở đã chốt |
| Environment | API #10, ClassScopeReadPort |
| Preconditions | H rời L1 ngày 15/10, nộp P1 ngày 18/10 khi đã rời; trước khi vào lại `leftAt=15/10`; sau vào lại cùng dòng ghi danh có `leftAt=null`, `joinedAt=01/09` |
| Test data | ID `GAP` |
| Steps | Gọi trước và sau khi đổi ghi danh; so JSON và scope. |
| Expected | Trước vào lại không có `GAP`; sau vào lại có đúng một dòng `GAP`, `leftClass=false` theo dòng ghi danh hiện tại. Không tạo bản sao submission. |
| Cleanup | Dọn fixture |
| Evidence | Hai scope states, hai response, DB submission |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC43-01 — Hai lớp chọn lần giao mới nhất rồi mới lọc

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-43; API §13.7 T-LI-05; BR-JD-10 bước 4 |
| Kind / priority | Concurrency/scope, P0: không nhân đôi hoặc gắn sai lớp |
| Environment | API #10, hai read port, DB judge |
| Preconditions | H thuộc L1 và L2 của G; P1 giao L1 ngày 01/10, L2 ngày 05/10; H nộp P1 ngày 06/10 một lần ID `BOTH` |
| Test data | GET không classId; GET `classId=L1`; GET `classId=L2` |
| Steps | Gọi ba request, đếm ID và lớp của dòng. |
| Expected | Không lọc: `BOTH` xuất hiện đúng một lần với `classId=L2`; lọc L1 không thấy; lọc L2 thấy đúng một lần. `totalCount` tương ứng 1,0,1; không lọc L1 trước bước chọn lớp. |
| Cleanup | Dọn fixture |
| Evidence | Ba response, assignments và DB |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC44-01 — Lớp ngoài phạm vi và UUID sai có cùng lỗi

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-44; API §13.2/13.4; BR-JD-10 bước 1 |
| Kind / priority | Security, P0: không suy ra lớp có tồn tại |
| Environment | API #10, JWT G |
| Preconditions | Lớp M có thật thuộc giáo viên khác; UUID `00000000-0000-0000-0000-000000000099` không có lớp; G có L1 |
| Test data | `classId=M`, classId không có, `classId=not-a-uuid` |
| Steps | Gọi ba request, so status/code/message, kiểm không có items. |
| Expected | Cả ba trả `404 JDG-111` với cùng thông điệp “không tìm thấy lớp”; không trả 200 danh sách rỗng hay dữ liệu của M. |
| Cleanup | Không ghi dữ liệu |
| Evidence | Ba response |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC44-02 — Học viên thiếu quyền đọc danh sách giảng viên

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-44; API §13.1/13.4 |
| Kind / priority | Security, P0: chặn truy cập không có `CLASS_MANAGEMENT:READ` |
| Environment | API #10, JWT học viên |
| Preconditions | A là học viên không có quyền; L1 có dữ liệu S1 |
| Test data | GET `/instructor/submissions?classId={L1}` |
| Steps | Gọi API bằng JWT A; kiểm body và DB. |
| Expected | `403 JDG-301`, không có item, mã nguồn hay thông tin lớp; không ghi DB. |
| Cleanup | Không ghi dữ liệu |
| Evidence | JWT claims đã che bí mật, response |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC45-01 — Chi tiết hợp lệ giữ tên thật và che Hidden

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-45; API §14.2/14.3/14.6 T-AD-01/03/06/07; BR-JD-10/11 |
| Kind / priority | Normal + security, P0: không tin breadcrumb URL và không lộ Hidden |
| Environment | API #11, FE USR0201 vùng giảng viên, DB/read ports |
| Preconditions | S1 trong phạm vi L1, 1 Sample và 1 Hidden; H tên Hoa, lớp L1; Hidden có marker `HIDDEN_47`; G có quyền |
| Test data | GET `/instructor/submissions/{S1}?studentId={B}&classId={M}&from=forged` |
| Steps | Gọi API, xem JSON và màn, đối chiếu chủ/lớp của S1. |
| Expected | 200; `studentId=H`, `studentName=Hoa`, `classId=L1`, `className` đúng L1, `leftClass=false`; trả `sourceCode=SOURCE_S1`, trạng thái/điểm/chi tiết Sample đúng. Không có `beatsPercent` hay `beatsSampleSize`; Hidden chỉ có order/verdict/runtime/memory, không có marker, ID/input/expected/output/stderr. FE không dùng B/M từ URL để hiển thị tên. |
| Cleanup | Dọn fixture |
| Evidence | Request/response, DB, ảnh màn, tìm marker toàn payload |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC45-02 — Ngoài phạm vi giống bài không tồn tại

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-45; API §14.4/14.6 T-AD-02/05/08; BR-JD-10 |
| Kind / priority | Security, P0: chống IDOR bài nộp giảng viên |
| Environment | API #11, DB/read ports |
| Preconditions | Có S1 hợp lệ; `OTHER` thuộc M, `LEFT` của H nộp sau `leftAt`, `EARLY` trước `assignedAt`, `REMOVED` đúng `removedAt`; UUID `MISSING` không tồn tại |
| Test data | GET năm ID ngoài phạm vi bằng JWT G |
| Steps | Gọi tuần tự và so toàn lỗi với MISSING; kiểm không có nguồn/điểm/testcase. |
| Expected | Cả năm trả `404 JDG-110` cùng thông điệp; không phân biệt bài có thật và không có; không trả dữ liệu S1 hay lớp M. |
| Cleanup | Dọn fixture |
| Evidence | Năm response, DB/scope/assignment timestamps |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC45-03 — Bài trước khi rời lớp vẫn mở được, học viên bị chặn

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-45; API §14.4/14.6 T-AD-04, T-AD-08 (quyền) |
| Kind / priority | Boundary + security, P1 |
| Environment | API #11, ClassScopeReadPort |
| Preconditions | H rời L1 lúc 12:00, S1 nộp 11:59 khi P1 còn giao; tài khoản học viên A không có `CLASS_MANAGEMENT:READ` |
| Test data | GET S1 bằng G và A |
| Steps | Gọi hai JWT, so phản hồi. |
| Expected | G nhận 200, `leftClass=true`, mã nguồn/chi tiết theo BR-JD-11; A nhận `403 JDG-301` và không có dữ liệu S1. |
| Cleanup | Dọn fixture |
| Evidence | Scope, hai response |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC46-01 — Lịch sử lớp 25 lượt, loại tự luyện và điểm rỗng

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-46; API §15.3/15.6 T-SC-01/02/06/07 |
| Kind / priority | Normal + security, P0: lịch sử đúng phạm vi và không lộ nguồn |
| Environment | API #12, FE INS0204, DB/read ports |
| Preconditions | H có 25 lượt P1 trong khoảng giao/ghi danh, gồm một COMPILE_ERROR và một SYSTEM_ERROR; thêm 3 lượt P2 tự luyện, một P1 trước assignedAt, một P1 đúng removedAt |
| Test data | GET `/instructor/classes/{L1}/students/{H}/submissions?page=1`, `page=2` |
| Steps | Gọi hai trang, đối chiếu ID/điểm/thứ tự với DB; mở màn. |
| Expected | `totalCount=25`, trang 1 có 20, trang 2 có 5 theo submittedAt giảm; không có năm lượt ngoài phạm vi. Mỗi item có problemTitle/language/status/scoreRatio/submittedAt, không có sourceCode/testcase; COMPILE_ERROR và SYSTEM_ERROR có `scoreRatio=null`, FE hiện dấu gạch. |
| Cleanup | Dọn fixture |
| Evidence | Hai response, DB timeline, ảnh màn |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC46-02 — Lớp khác, học viên không thuộc lớp, thiếu quyền

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-46; API §15.4/15.6 T-SC-04/05 |
| Kind / priority | Security, P0: không lộ lịch sử ngoài lớp |
| Environment | API #12, JWT G và học viên A |
| Preconditions | M thuộc giáo viên khác; K chưa từng ghi danh L1; H có dữ liệu S1 |
| Test data | G gọi M/H rồi L1/K; A gọi L1/H |
| Steps | Gọi ba request, kiểm status/code/body. |
| Expected | M/H trả `404 JDG-111`; L1/K trả `404 JDG-112`; A gọi L1/H trả `403 JDG-301`. Không response nào chứa nguồn/chi tiết hay dữ liệu S1. |
| Cleanup | Không ghi dữ liệu |
| Evidence | Ba response và scope |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC47-01 — Bốn tuần đủ, tuần trống và học viên chưa nộp

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-47; API §26.1; logic BR-JD-13; DD F4 tuần đủ |
| Kind / priority | Normal + boundary, P1: không tính tuần đang chạy |
| Environment | Cổng P1 trong tiến trình, Redis, DB judge, đồng hồ cố định `2026-10-21T12:00+07` (thứ Tư) |
| Preconditions | L1 có H và K; P1 giao 01/09; H nộp ACCEPTED thứ Hai 28/09, WRONG_ANSWER thứ Ba 06/10, thêm ACCEPTED thứ Tư 21/10; K chưa nộp. `students[]` có cả hai, `assignments[]` có P1. |
| Test data | `GetClassStudentSubmissionMetrics(L1, students, assignments, weekCount=4)` |
| Steps | Gọi cổng, đối chiếu UTC+7 và DB; đọc payload đầy đủ. |
| Expected | Mỗi học viên có đúng bốn `weeklyTrend` cũ nhất trước với `weekStart` 21/09, 28/09, 05/10, 12/10; H có `submissionCount` 0,1,1,0 và `acceptedCount` 0,1,0,0; tuần rỗng `acceptedRatio=null`; lượt 21/10 không vào bốn tuần đủ. K có 4 tuần số 0, `streakDays=0`, `lastSubmittedAt=null`, `acceptedRatio=null`, một assignmentResult của P1 với bestRatio/bestVerdict null và accepted=false. Không có nguồn/testcase. |
| Cleanup | Dọn fixture, xoá cache riêng L1 |
| Evidence | Clock, input/output cổng, DB timeline, Redis key TTL |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC47-02 — Cache 15 phút và đổi phạm vi làm đổi khoá

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-47; API §26.1 cache key/TTL; logic BR-JD-13 |
| Kind / priority | Recovery + boundary, P1: dữ liệu mới không vượt quá hạn cache |
| Environment | Cổng P1, Redis, clock điều khiển được |
| Preconditions | T0 là `2026-10-21T12:00+07`; H đã có một bài nộp P1 trong phạm vi; cache L1 rỗng ban đầu. Sau lần đọc đầu, H nộp thêm ACCEPTED P1. |
| Test data | Hai lời gọi cùng input trong 14 phút; lời gọi sau TTL 15 phút; thêm lời gọi đổi `assignedAt` của P1 và lời gọi đổi `leftAt` của H |
| Steps | Đọc lần đầu, thêm bài, đọc tại T0+14 phút và T0+16 phút; đổi từng input window rồi đọc; kiểm Redis key và payload. |
| Expected | Lần trong 14 phút được phép giữ snapshot cũ; sau TTL kết quả phản ánh bài mới. Đổi mốc giao/ghi danh tạo khoá khác và kết quả tính lại theo cửa sổ mới; không dùng snapshot của phạm vi cũ. Không trả nguồn/testcase. |
| Cleanup | Xoá cache fixture và submission mới |
| Evidence | Input/output và thời điểm từng gọi, Redis keys/TTL, DB |
| Design review | Đã review; chưa thực thi |
