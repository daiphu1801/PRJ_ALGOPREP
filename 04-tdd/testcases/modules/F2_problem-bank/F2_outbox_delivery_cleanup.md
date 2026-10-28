# Test case QA — F2: Outbox, phát lại và dọn sự kiện

> Trạng thái: 8 ca đã review thiết kế; chưa thực thi. Nguồn: `04-tdd/modules/F2_problem-bank.md` AC-89, AC-90, AC-94; DD F2 API §53, §59; logic BR-26; jobs JOB-PB-01, JOB-PB-02 (2026-10-27).

## Fixture và câu hỏi hợp đồng

- `T0=2026-10-26T03:00:00Z` là clock DB điều khiển được. `P-OB` là bài thuộc tác giả `A-OB`, `PUBLISHED`, có `problem_specs.spec_version=7`; `I-OB` là giảng viên có `CLASS_MANAGEMENT:UPDATE` cho lớp `C-OB`, chưa giao bài `P-OB`. RabbitMQ exchange `algoprep.domain-events`; F2 relay chạy mỗi 2 giây, lô tối đa 100. Mỗi ca dùng UUID riêng cho `eventId` và `aggregateId`.
- Quan sát `problem.outbox_events` (`event_id`, `event_type`, `routing_key`, `payload`, `published_at`, `publish_attempts`), RabbitMQ publisher confirm, hàng nhận F1/F3 và bảng nghiệp vụ. Nếu schema đặt tên cột `id` cho eventId, đối chiếu bằng giá trị phong bì `eventId`, không suy thêm cột.
- **Oracle AC-90 đã chốt 2026-10-27:** lỗi ghi Outbox rollback riêng bài, lô trả HTTP 200 với `failedItems[].code=PRB-056`; thử lại sau phục hồi tạo đúng một assignment và event.

| AC | Nhánh | Test case | Review | Thực thi |
| :--- | :--- | :--- | :--- | :--- |
| AC-89 | Broker tắt, mất confirm, hai relay, payload lỗi | TC-F2-AC89-01 đến -04 | Đã review | Chưa thực hiện |
| AC-90 | Outbox insert lỗi, retry sau phục hồi | TC-F2-AC90-01 | Đã review | Chưa thực hiện |
| AC-94 | Ranh giới 90 ngày, hai cleaner, rollback lô | TC-F2-AC94-01 đến -03 | Đã review | Chưa thực hiện |

## Test case

### TC-F2-AC89-01 — Thay đổi bài commit khi broker tắt, relay giao sau phục hồi

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-89; DD F2 API §38, §59; logic BR-26; JOB-PB-01 PB-J01/J02 |
| Kind / priority | Recovery, P0: không mất sự kiện đã commit |
| Environment | API F2, PostgreSQL, RabbitMQ fault, relay, F3 consumer |
| Preconditions | `P-OB` có `spec_version=7`; dừng confirm RabbitMQ trước khi lưu; F3 chưa thấy phiên bản 8 |
| Test data | `A-OB` đổi `functionName` từ `solve` thành `solveV2`; sự kiện `ProblemSpecChanged` phiên bản 8 |
| Steps | Lưu đặc tả; đọc DB/outbox khi broker tắt; cho relay thử một lần; khôi phục broker; chạy relay và kiểm F3. |
| Expected | Thay đổi và đúng một dòng Outbox cùng commit; API lưu thành công dù broker tắt. Lần thử lỗi tăng `publish_attempts` lên 1, `published_at=NULL`, log WARN không chứa đặc tả/testcase thô. Sau phục hồi, cùng `eventId` và phong bì `{eventId,eventType,occurredAt,schemaVersion:1,payload}` được publish với routing key `problem.spec.changed`; chỉ sau confirm mới có `published_at`, attempts thành 2. F3 nhận `specVersion=8` đúng một lần nghiệp vụ. |
| Cleanup | Khôi phục broker; xoá fixture/outbox và reset F3 |
| Evidence | API, DB trước–sau, publish/confirm timeline, event và trạng thái consumer |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC89-02 — Mất confirm nhưng broker đã nhận: phát lại giữ eventId

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-89; JOB-PB-01 PB-J02; F2 API §59.1 idempotency |
| Kind / priority | Recovery, P0: at-least-once không nhân đôi nghiệp vụ |
| Environment | PostgreSQL, RabbitMQ confirm fault, relay, F1 consumer/feed |
| Preconditions | `I-OB` giao `P-OB` cho `C-OB`, có một dòng `ProblemAssignedToClass` chưa phát; broker nhận message nhưng confirm trả về relay bị mất |
| Test data | EventId `E-ACK` lấy từ Outbox; payload có `problemId`, `classId`, `actorUserId`, `problemTitle`, `assignedAt` |
| Steps | Cho relay publish lần 1 nhưng chặn confirm; kiểm outbox và F1; chạy relay lần 2 có confirm; kiểm hai message và feed. |
| Expected | Sau lần 1 `published_at=NULL`, attempts=1 dù broker/F1 đã nhận; lần 2 attempts=2, `published_at` có giá trị. Cả hai phong bì có cùng `eventId=E-ACK`, cùng routing key `problem.assignment.assigned`; F1 chỉ có một hoạt động `CLASS_PROBLEM_PUBLISHED`, không nhân đôi feed. |
| Cleanup | Tắt fault, xoá fixture/feed/outbox |
| Evidence | Hai message và confirm, DB Outbox, feed F1 |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC89-03 — Hai relay không claim cùng dòng trong một lượt

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-89; JOB-PB-01 `FOR UPDATE SKIP LOCKED`, PB-J03 |
| Kind / priority | Concurrency, P1: nhiều instance không tranh một event |
| Environment | Hai instance relay F2, PostgreSQL barrier, RabbitMQ confirm |
| Preconditions | Một event `ProblemSpecChanged` chưa phát, `publish_attempts=0`; hai relay chạy cùng lúc |
| Test data | Một `eventId=E-RACE` duy nhất |
| Steps | Giữ relay 1 sau khi khoá dòng; kích relay 2; thả relay 1 cho publish/confirm; đọc DB/message. |
| Expected | Relay 2 bỏ qua dòng đang khoá, không publish đồng thời cùng `E-RACE`; relay 1 publish một lần, attempts=1, `published_at` có giá trị. Không có hai claim trong một thời điểm. |
| Cleanup | Thả barrier; xoá event fixture |
| Evidence | Lock/relay trace, DB và message timeline |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC89-04 — Payload hoặc routing key lỗi không bị đánh dấu đã phát

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-89; JOB-PB-01 đoạn lỗi payload/routing key |
| Kind / priority | Abnormal, P1: sự kiện lỗi vẫn còn để vận hành sửa |
| Environment | PostgreSQL, relay F2, RabbitMQ, log vận hành |
| Preconditions | Một dòng Outbox chưa phát được seed với `routing_key="problem.unknown"`, `eventType=ProblemSpecChanged`, payload JSON hợp lệ |
| Test data | `eventId=E-BAD-ROUTE`, `publish_attempts=0` |
| Steps | Chạy một lượt relay; đọc Outbox, broker, log. |
| Expected | Không publish tới exchange, `published_at=NULL`; log ERROR có `eventId` để sửa, không chứa payload/testcase thô. Dòng không bị xoá hoặc bỏ qua vĩnh viễn; không có tác động nghiệp vụ ở consumer. |
| Cleanup | Xoá dòng seed, reset relay |
| Evidence | Outbox trước–sau, broker trace, ERROR log |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC90-01 — Ghi Outbox lỗi phải rollback giao bài

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-90; DD F2 API §53, §59; logic BR-26 |
| Kind / priority | Recovery, P0: không giao bài một nửa |
| Environment | API F2, PostgreSQL fault injection khi INSERT `problem.outbox_events`, RabbitMQ, F1 feed |
| Preconditions | `I-OB` phụ trách `C-OB`; `P-OB` đang xuất bản và chưa giao; Outbox insert bị lỗi cho riêng lần giao này |
| Test data | `POST /instructor/classes/{classId}/assignments` với `{ "problemIds": ["P-OB"] }` (thay bằng UUID fixture) |
| Steps | Bật fault; POST, đọc response và DB/feed; tắt fault; POST lại; chạy relay. |
| Expected | Lần đầu trả `200`, `succeededIds=[]`, `skippedIds=[]`, `failedItems` có đúng một mục `id=P-OB`, `code=PRB-056`, `message` là chuỗi không rỗng và không chứa lỗi SQL/stack trace; không có `class_assignments` hoạt động, Outbox hoặc feed mới. Lần hai sau phục hồi trả `200` với `P-OB` trong `succeededIds`, `failedItems=[]`, có đúng một assignment hoạt động và một Outbox `ProblemAssignedToClass`; relay giao một event. |
| Cleanup | Tắt fault, xoá assignment/outbox/feed fixture |
| Evidence | Hai response, DB giao dịch, Outbox, feed, fault timeline |
| Design review | Đã review theo quyết định chủ dự án 2026-10-27; chưa thực thi |

### TC-F2-AC94-01 — Dọn đúng biên 90 ngày, giữ sự kiện chưa phát

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-94; JOB-PB-02 PB-J04 |
| Kind / priority | Boundary, P0: không mất event pending |
| Environment | PostgreSQL, clock DB, JOB-PB-02 |
| Preconditions | Clock `T0`; tắt relay để dòng pending không đổi; seed bốn event độc lập |
| Test data | `E-OLD`: published `T0-90d-1s`; `E-EDGE`: published đúng `T0-90d`; `E-NEW`: published `T0-90d+1s`; `E-PENDING`: `published_at=NULL`, `created_at=T0-100d` |
| Steps | Chạy cleaner tại `T0`; kiểm bốn ID. |
| Expected | Chỉ `E-OLD` bị xoá vì điều kiện `<`; `E-EDGE`, `E-NEW`, `E-PENDING` còn nguyên. Không thay đổi trạng thái nghiệp vụ của aggregate. |
| Cleanup | Khôi phục relay/clock; xoá seed còn lại |
| Evidence | Timestamp và DB query trước–sau, job log |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC94-02 — Hai cleaner đồng thời không xoá event pending

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-94; JOB-PB-02 điều kiện DELETE và lô 1.000 |
| Kind / priority | Concurrency, P1: dọn song song an toàn |
| Environment | PostgreSQL, hai instance JOB-PB-02, clock DB, barrier |
| Preconditions | Clock `T0`, relay tắt; 1.005 event đã phát `T0-91d`, 1 event pending tạo `T0-100d` |
| Test data | 1.006 UUID phân biệt, lưu danh sách trước chạy |
| Steps | Kích hai cleaner cùng lúc; chờ cả hai hết lượt; đếm ID theo trạng thái. |
| Expected | Cả 1.005 event đã phát quá hạn bị xoá đúng một lần; event pending còn nguyên, không có lỗi giao dịch làm mất dữ liệu. Lô tối đa 1.000 dòng và xử lý phần dư ở lô tiếp theo. |
| Cleanup | Khôi phục relay/clock; xoá pending fixture |
| Evidence | SQL count theo nhóm, log hai worker và số dòng mỗi lô |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC94-03 — DB lỗi giữa lô: rollback lô và thử lại

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-94; JOB-PB-02 đoạn lỗi DB |
| Kind / priority | Recovery, P0: không dọn một nửa lô |
| Environment | PostgreSQL, JOB-PB-02, DB fault injection, clock DB |
| Preconditions | Clock `T0`; 10 event đã phát `T0-91d`, một event chưa phát cũ; fault trước commit DELETE lô đầu |
| Test data | Danh sách 11 eventId và published_at |
| Steps | Chạy cleaner với fault; kiểm DB/log; tắt fault rồi chạy lượt kế tiếp. |
| Expected | Lượt lỗi ghi ERROR và rollback toàn bộ lô 10: cả 11 dòng còn nguyên. Lượt sau xoá đúng 10 event đã phát, giữ dòng chưa phát; không cần khôi phục thủ công. |
| Cleanup | Khôi phục clock/fault, xoá pending fixture |
| Evidence | DB snapshots trước/sau mỗi lượt, transaction log, job ERROR |
| Design review | Đã review; chưa thực thi |
