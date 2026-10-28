# Testcase QA F1 — ghi danh lớp và nhãn học viên

> Phạm vi [TDD F1](../../../modules/F1_identity.md) AC-19–24, gồm AC-20b. Oracle: `03-dd/api/F1_identity.md` #25–27, §0.4.1–0.4.3; `03-dd/logic/F1_identity.md` BR-09–11. Đây là ca thiết kế, **chưa thực thi**. Prefix API: `/api/v1/identity`.

Fixture chung: clock `T0=2026-10-27T02:00:00Z`; lớp C của giảng viên I1, lớp D của I2, học viên S1/S2, mỗi actor có JWT riêng. Mã `AB12CD34` thuộc C hết hạn tại T0−1 giây; mã `EF56GH78` thuộc C hết hạn T0+1 ngày. Submission/assignment của C do cổng F2/F4 fixture điều khiển, không dùng số liệu toàn cục. Mỗi ca tách DB, Redis và event spy riêng.

| AC | Nhánh | Case | Review |
| :--- | :--- | :--- | :--- |
| 19 | Mã hết hạn | TC-F1-AC19-01 | Đã review |
| 20 | Thành viên vào lại và học viên mới | TC-F1-AC20-01/02 | Đã review |
| 20b | Thành viên đã rời vào lại | TC-F1-AC20b-01 | Đã review |
| 21 | Gỡ học viên, giữ lịch sử và phát lại event khi retry | TC-F1-AC21-01/02 | Đã review |
| 22 | Giảng viên ngoài phạm vi | TC-F1-AC22-01 | Đã review |
| 23 | Cửa sổ 7 ngày | TC-F1-AC23-01/02 | Đã review |
| 24 | Thứ tự ưu tiên nhãn | TC-F1-AC24-01/02/03 | Đã review |

### TC-F1-AC19-01 — Mã mời hết hạn không ghi danh

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-19; API #25, BR-09 |
| Kind / priority | Boundary; P0 vì ranh giới vào lớp |
| Environment | API, DB, Redis, clock |
| Preconditions | S1 chưa có enrollment C; mã AB12CD34 hết hạn T0−1 giây. |
| Test data | POST `/classes/join` JWT S1, body `{"code":"AB12CD34"}` tại T0. |
| Steps | Gọi join; đọc enrollment C/S1, danh sách học viên #26 và counter thử sai. |
| Expected | `422 IDT-410`, không lộ classId hay trạng thái mã; không có enrollment mới, danh sách/tiến độ C không thêm S1; counter sai của S1 tăng đúng một. |
| Cleanup | Reset fixture và Redis counter. |
| Evidence | Response, enrollment diff, #26, Redis counter. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC20-01 — Join lại khi đang ở lớp không đổi ngày tham gia

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-20; API #25, BR-09 |
| Kind / priority | Normal + idempotency; P0 vì số liệu lớp |
| Environment | API, DB, Redis |
| Preconditions | S1 đang ở C với `joined_at=T0−2 ngày`, `left_at=NULL`; mã EF56GH78 còn hạn. |
| Test data | POST `/classes/join` JWT S1, body `{"code":"EF56GH78"}` hai lần. |
| Steps | Gọi hai lần; đọc response, enrollment và thống kê C. |
| Expected | Hai lần `200 {classId:C,className:<tên C>}`; chỉ một dòng C/S1, `joined_at` ban đầu giữ nguyên, `left_at=NULL`; số học viên không tăng hai lần. Counter thử sai bị xoá khi join thành công. |
| Cleanup | Reset enrollment/counter. |
| Evidence | Hai response, DB row before/after, count lớp. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC20b-01 — Vào lại lớp sau khi bị gỡ dùng lại dòng cũ

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-20b; API #25, BR-09 |
| Kind / priority | Recovery; P0 vì lịch sử học tập |
| Environment | API, DB, cổng F2/F4 |
| Preconditions | S1 có dòng C/S1 `joined_at=T0−20 ngày`, `left_at=T0−2 ngày`; có submission cũ; mã EF56GH78 còn hạn. |
| Test data | POST `/classes/join` JWT S1, body `{"code":"EF56GH78"}`. |
| Steps | Gọi join; đọc enrollment, danh sách #26 và lịch sử bài nộp. |
| Expected | `200`; vẫn một dòng C/S1 với `left_at=NULL` và `joined_at` cũ; S1 có mặt lại trong danh sách, submission cũ giữ nguyên. Tiến độ lớp chỉ xét lượt trong phạm vi ghi danh/bài giao của hợp đồng F2/F4, không tự tính từ thống kê toàn cục. |
| Cleanup | Reset enrollment/history fixture. |
| Evidence | Response, enrollment diff, #26 và F4 history. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC21-01 — Gỡ học viên đánh dấu rời và phát sự kiện

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-21; API #27–28, BR-11 |
| Kind / priority | Normal + integration; P0 vì dữ liệu liên module |
| Environment | API, DB, RabbitMQ spy, F4 history |
| Preconditions | I1 sở hữu C; S1 đang ghi danh, có submission trong lớp; event spy rỗng. |
| Test data | POST `/classes/{C}/students/{S1}/leave` bằng JWT I1, không body, tại T0. |
| Steps | Gọi leave; đọc DB và event sau commit; gọi lại cùng endpoint; đọc lịch sử F4 và danh sách #26. |
| Expected | Lần đầu `200`, dòng enrollment giữ nguyên nhưng `left_at=T0`, S1 vắng trong danh sách hiện hành; `StudentRemovedFromClass {classId:C,studentId:S1,occurredAt:T0}` được phát sau commit; submission F4 không bị xoá/ẩn. Lần hai `200`, `left_at` không đổi, phát lại cùng event với `occurredAt=T0`; F4 không đăng ký consumer xoá dữ liệu cho event này; đọc phạm vi hiện hành qua ClassScopeReadPort, lịch sử vẫn còn sau cả hai event. |
| Cleanup | Reset enrollment/event/history. |
| Evidence | Hai response, DB diff, event spy timestamp, F4 history và #26. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC21-02 — Publish lỗi sau commit, gọi lại phát event với mốc gốc

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-21; API #27, BR-11; `DEC-2026-1027-qa-f1-class-leave-event-retry` |
| Kind / priority | Recovery + integration; P0 vì tránh mất event sau commit |
| Environment | API, DB, RabbitMQ publish spy có thể lỗi, F4 ClassScopeReadPort/query spy, clock cố định |
| Preconditions | I1 sở hữu C; S1 đang ghi danh và có một submission lịch sử. RabbitMQ publish được đặt lỗi cho lần gọi đầu, phục hồi trước lần gọi thứ hai. |
| Test data | `T0=2026-10-27T02:00:00Z`; hai POST `/classes/{C}/students/{S1}/leave` bằng JWT I1, không body; lần hai tại `T0+5 phút`. |
| Steps | Gọi lần đầu và chờ commit/publish lỗi; kiểm enrollment, audit/log và F4 history. Phục hồi broker; gọi lại cùng endpoint; đọc event và enrollment. Phát bản sao lên broker rồi đọc lại F4 history/phạm vi lớp để kiểm không mất dữ liệu. |
| Expected | Sau lần đầu, enrollment có đúng một dòng `left_at=T0`, S1 rời danh sách hiện hành, submission lịch sử vẫn xem được; publish lỗi có log cảnh báo và không rollback `left_at`. Lần hai trả `200`, không đổi `left_at`, phát `StudentRemovedFromClass {classId:C,studentId:S1,occurredAt:T0}` sau commit. F4 không tiêu thụ event này để xoá/ẩn lịch sử; bản sao không đổi history và current scope vẫn lấy từ ClassScopeReadPort. Không phát event mang `occurredAt=T0+5 phút`. |
| Cleanup | Reset enrollment, submission fixture, broker spy và clock. |
| Evidence | Hai response, transaction/DB diff, WARN log đã che PII, event payload/timestamp, ClassScopeReadPort/query spy và F4 history. |
| Design review | Đã review theo quyết định chủ dự án; chưa thực thi |

### TC-F1-AC22-01 — Giảng viên không sở hữu lớp không được gỡ học viên

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-22; API #27, BR-11, API §0.4.3 |
| Kind / priority | Security; P0 vì ranh giới lớp |
| Environment | API, DB, RabbitMQ spy |
| Preconditions | I1 sở hữu C; I2 có quyền giảng viên nhưng chỉ sở hữu D; S1 đang học C. |
| Test data | POST `/classes/{C}/students/{S1}/leave` bằng JWT I2, không body. |
| Steps | Gọi leave; đọc enrollment C/S1, sự kiện và lịch sử. |
| Expected | `404 IDT-414` theo quy tắc lớp ngoài phạm vi; C/S1 vẫn active, `left_at=NULL`; không phát `StudentRemovedFromClass`, không đổi lịch sử. Response không lộ hồ sơ S1 hoặc chi tiết lớp C. |
| Cleanup | Reset fixture. |
| Evidence | Response, DB/event diff. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC23-01 — Tám ngày không nộp trong lớp nhận nhãn ABSENT

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-23; API #26, BR-10 |
| Kind / priority | Boundary; P0 vì phân loại cần hỗ trợ |
| Environment | API, F2 assignment stub, F4 class metrics stub, clock |
| Preconditions | C có hai bài đang giao; S1 có một submission hợp lệ trong phạm vi C tại T0−8 ngày và một submission ngoài C tại T0−1 ngày. |
| Test data | GET `/classes/{C}/students` JWT I1 tại T0; F4 metrics lớp chỉ có lượt T0−8 ngày. |
| Steps | Đọc danh sách; đối chiếu payload cổng F4 class metrics với nhãn S1. |
| Expected | S1 `status=ABSENT` vì không có submission thuộc C trong bảy ngày trượt; lượt ngoài C không làm đổi nhãn. Không dùng `user_submission_stats` toàn cục. |
| Cleanup | Reset stubs và clock. |
| Evidence | Response #26, F2/F4 spy payload, clock trace. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC23-02 — Lượt đúng ranh giới bảy ngày không bị gắn ABSENT

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-23; API #26, BR-10 |
| Kind / priority | Boundary; P1 vì sai lệch thời gian |
| Environment | API, F2/F4 stubs, clock |
| Preconditions | S1 có ít nhất một submission thuộc bài C được giao, tại T0−7 ngày+1 giây; không có điều kiện NEEDS_SUPPORT/WATCH. |
| Test data | GET `/classes/{C}/students` JWT I1 tại T0. |
| Steps | Gọi #26; đọc nhãn và F4 metrics theo lớp. |
| Expected | S1 không mang nhãn `ABSENT`; với fixture còn lại, nhãn `ON_TRACK`. Không bị ảnh hưởng bởi mốc tuần lịch. |
| Cleanup | Reset stubs/clock. |
| Evidence | Response, metrics timestamps, clock trace. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC24-01 — ABSENT thắng WATCH khi cùng thoả

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-24; API #26, BR-10 |
| Kind / priority | Boundary; P0 vì thứ tự ưu tiên nhãn |
| Environment | API, F2/F4 stubs, clock |
| Preconditions | C có bốn bài đang giao; S1 đã có submission trong C từ T0−8 ngày, hoàn thành một bài (25%), không có lượt bảy ngày gần nhất. |
| Test data | GET `/classes/{C}/students` JWT I1 tại T0; F2 trả 4 assignments, F4 trả 1 accepted result và timestamp T0−8 ngày. |
| Steps | Gọi #26; đối chiếu một dòng S1 và dữ liệu cổng. |
| Expected | Một nhãn duy nhất `ABSENT`, không phải `WATCH`, dù 1/4=25%<50%; không rơi vào `INSUFFICIENT_DATA` vì đã có lượt nộp. |
| Cleanup | Reset stubs và clock. |
| Evidence | Response #26, F2/F4 spy payload. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC20-02 — Học viên mới vào lớp bằng mã hợp lệ

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-20 đối chứng trạng thái mới; API #25, #25A, BR-09 |
| Kind / priority | Normal; P0 vì ghi danh là đầu vào của mọi thống kê lớp |
| Environment | API, DB, Redis, clock |
| Preconditions | S2 chưa từng có dòng enrollment với C; mã EF56GH78 còn hạn; S2 có JWT STUDENT. |
| Test data | POST `/classes/join` JWT S2 body `{"code":" ef56gh78 "}` tại T0, server trim và uppercase. |
| Steps | Gọi join; đọc dòng C/S2, #25A của S2, #26 của I1 và counter sai. |
| Expected | `200 {classId:C,className:<tên C>}`; đúng một enrollment C/S2 có `joined_at=T0,left_at=NULL`; C xuất hiện trong #25A của S2 và S2 trong #26 của I1; counter sai bị xoá. Không có dòng ghi danh trùng. |
| Cleanup | Xoá enrollment fixture và reset counter. |
| Evidence | Response #25/#25A/#26, DB row và Redis diff. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC24-02 — INSUFFICIENT_DATA thắng ABSENT khi chưa từng nộp

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-24; API #26, BR-10 |
| Kind / priority | Boundary; P0 vì thứ tự nhãn đầu tiên |
| Environment | API, F2 assignment stub, F4 class metrics stub, clock |
| Preconditions | C có hai bài đang giao; S2 đang ghi danh nhưng không có submission nào trong phạm vi bài giao C, cũng không có submission bảy ngày qua. |
| Test data | GET `/classes/{C}/students` JWT I1 tại T0; F2 trả 2 assignments, F4 trả 0 lượt thuộc C. |
| Steps | Gọi #26; đọc status của S2 và payload cổng. |
| Expected | S2 có đúng một nhãn `INSUFFICIENT_DATA`; không gắn `ABSENT` dù không nộp bảy ngày, vì BR-10 xét chưa đủ dữ liệu trước. Không suy nhãn từ thống kê toàn cục. |
| Cleanup | Reset stubs. |
| Evidence | Response #26, F2/F4 spy payload. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC24-03 — NEEDS_SUPPORT thắng WATCH khi tỉ lệ giảm liên tục

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-24; API #26, BR-10 |
| Kind / priority | Boundary; P1 vì ưu tiên hỗ trợ học viên |
| Environment | API, F2 assignment stub, F4 class metrics stub, clock |
| Preconditions | S1 đang ghi danh C; có submission trong C tại T0−1 ngày để không ABSENT; C có 4 bài đang giao, S1 hoàn thành 1 bài = 25%; F4 trả tỉ lệ Accepted theo ba mốc tuần đủ gần nhất 80%, 60%, 40%. |
| Test data | GET `/classes/{C}/students` JWT I1 tại T0, F2 trả 4 assignments active, F4 trả 3 điểm theo thứ tự thời gian và một lượt mới trong tuần hiện tại. |
| Steps | Gọi #26; đối chiếu status S1 và ba mốc tuần của F4. |
| Expected | S1 có đúng một nhãn `NEEDS_SUPPORT` vì tỉ lệ giảm qua hai bước tuần liên tiếp; không gắn `WATCH` dù hoàn thành 25%<50%, không gắn `ABSENT` vì có lượt trong bảy ngày. |
| Cleanup | Reset stubs/clock. |
| Evidence | Response #26, F2/F4 spy payload và clock trace. |
| Design review | Đã review; chưa thực thi |
