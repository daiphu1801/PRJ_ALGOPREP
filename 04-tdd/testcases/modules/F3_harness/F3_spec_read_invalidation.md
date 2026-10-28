# Test case QA — F3: đọc đặc tả và vô hiệu cache

> Trạng thái: 6 ca đã review thiết kế; chưa thực thi. Nguồn: `04-tdd/modules/F3_harness.md` AC-39–40; DD F3 API O1 §4, E1 §5; DD F2 API §59 và logic BR-26.

## Fixture chung

- `P-READ` là bài `PUBLISHED`, đặc tả v7 có `functionName="solveOld"`, sau cập nhật v8 có `functionName="solveNew"`; người gọi có JWT hợp lệ. P1 xin starter code, P2 chuẩn bị gói `FUNCTION_WRAPPER` với mã học viên tương ứng chữ ký; dùng Java để đối chiếu dòng khai báo/call-site. `ProblemSpecReaderPort.findSpec` đọc từ F2, cache nội bộ F3 **mặc định tắt**; ca kiểm invalidation bật riêng `CachingProblemSpecReader` và seed mục cache có phiên bản ghi rõ.
- Sự kiện F2 `ProblemSpecChanged` có phong bì `{eventId,eventType,occurredAt,schemaVersion:1,payload:{problemId,specVersion,changedColumns,signatureChanged,...}}`. Gửi trực tiếp tới listener E1 với eventId riêng; không phụ thuộc thứ tự nhận giữa các bài khác nhau. Không dùng mã sinh ra từ lần gọi cũ làm bằng chứng cho lần gọi mới.

| AC | Nhánh | Test case | Review | Thực thi |
| :--- | :--- | :--- | :--- | :--- |
| AC-39 | Reader lỗi ở P1 và P2 | TC-F3-AC39-01 đến -02 | Đã review | Chưa thực hiện |
| AC-40 | Event mới, lặp, cũ hơn, không có cache | TC-F3-AC40-01 đến -04 | Đã review | Chưa thực hiện |

## Test case

### TC-F3-AC39-01 — Reader lỗi khi xin starter code trả 503, không trả mã nửa vời

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-39; DD F3 API §3.4, §4.2 O1-T03 |
| Kind / priority | Recovery, P0: lỗi đọc đặc tả phải rõ |
| Environment | F3 API/P1, F2 reader fault injection |
| Preconditions | `P-READ` mở được; quyền đọc qua O2 hợp lệ; O1 ném timeout trước khi trả view |
| Test data | GET starter code Java `FUNCTION_WRAPPER` cho `P-READ` |
| Steps | Gọi endpoint, ghi response; gỡ fault và gọi lại. |
| Expected | Lần lỗi `503 HRN-003`, không có `starterCode`, chữ ký hoặc tệp mã khung một phần; không ghi DB/cache mã. Sau phục hồi trả starter của spec hiện hành, có thể thử lại cùng request. |
| Cleanup | Gỡ fault, reset fixture |
| Evidence | Hai response, O1 call trace và trạng thái DB/cache |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC39-02 — Reader lỗi khi prepare không trả gói chấm nửa vời

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-39; DD F3 API §2.4, §4.2, P2-T09 |
| Kind / priority | Recovery, P0: không chấm bằng đặc tả thiếu |
| Environment | P2 component, F2 reader timeout fault |
| Preconditions | `P-READ` có spec hợp lệ; O1 timeout khi P2 gọi |
| Test data | `PrepareSubmissionCommand` Java Wrapper, mã `class Solution { int solveOld(int value) { return value; } }`, `expectedSpecVersion=7` |
| Steps | Gọi P2 với fault; kiểm lỗi và artifact; khôi phục O1 rồi gọi lại với version hiện hành. |
| Expected | Lần lỗi `HRN-003 SPEC_READER_UNAVAILABLE`, không trả `PreparedProgram`/`JudgePayload`, không tạo tệp phần; lần sau có thể prepare thành công theo spec hiện hành. |
| Cleanup | Gỡ fault, xoá artifact fixture |
| Evidence | P2 command/error, artifact inventory trước–sau |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC40-01 — Event phiên bản 8 làm mã mới dùng solveNew

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-40; DD F3 API §5 E1-T01; F2 API §59.1 |
| Kind / priority | Normal, P0: không dùng mã khung cũ sau cập nhật |
| Environment | F2 SaveProblemSpec, F3 cache decorator/P1/P2, RabbitMQ event |
| Preconditions | Cache F3 chứa `P-READ` v7 `solveOld`; P1 đã sinh mã v7; F2 lưu spec v8 `solveNew` và Outbox event v8 |
| Test data | `eventType=ProblemSpecChanged`, `specVersion=8`, `changedColumns=["function_name"]`, `signatureChanged=true` |
| Steps | Phát event v8 tới E1; ngay sau đó gọi P1 Java và P2 với mã dùng `solveNew`, `expectedSpecVersion=8`. |
| Expected | Cache v7 bị xoá; P1 khai báo `solveNew`, P2 call-site dùng `solveNew` và `matchSpec`/version từ v8, không có chuỗi `solveOld` trong mã mới. Không cần restart F3; listener không tự sinh hoặc lưu mã. |
| Cleanup | Reset cache/spec/outbox fixture |
| Evidence | F2 DB/event, cache trước–sau, P1/P2 mã sinh |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC40-02 — Nhận lại cùng event không phá cache mới

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-40; DD F3 API §5 E1-T02 |
| Kind / priority | Concurrency, P1: relay at-least-once |
| Environment | E1 listener, cache decorator F3 |
| Preconditions | Event v8 đã xử lý; cache `P-READ` đã nạp lại view v8 `solveNew` |
| Test data | Phát lại chính phong bì v8 cùng `eventId` |
| Steps | Gọi E1 lần hai; kiểm cache và P1 kế tiếp. |
| Expected | Không ném lỗi, không xoá view v8, P1 vẫn sinh `solveNew`; không tái sinh mã ngay trong listener. |
| Cleanup | Reset cache |
| Evidence | Hai event delivery, cache trace, P1 output |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC40-03 — Event v7 đến muộn không xoá cache v8

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-40; DD F3 API §5 E1-T04 |
| Kind / priority | Concurrency, P0: tránh lùi phiên bản do event cũ |
| Environment | E1 listener, cache decorator F3 |
| Preconditions | Cache `P-READ` v8 `solveNew` đã nạp |
| Test data | Event `ProblemSpecChanged` cùng `problemId`, `specVersion=7`, eventId khác event v8 |
| Steps | Phát event v7; kiểm cache; gọi P1 và P2 v8. |
| Expected | Cache v8 vẫn còn; P1/P2 dùng `solveNew`, không xoá nhầm và không trả lại `solveOld`. |
| Cleanup | Reset cache |
| Evidence | Event, cache trace, P1/P2 output |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC40-04 — Event cho bài không cache là no-op

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-40; DD F3 API §5 E1-T03 |
| Kind / priority | Boundary, P1: listener an toàn khi cache rỗng |
| Environment | E1 listener, cache decorator F3 |
| Preconditions | Cache không có mục `P-READ`; F2 spec hiện hành v8 `solveNew` |
| Test data | Event v8 hợp lệ của `P-READ` |
| Steps | Phát event; gọi P1 lần đầu sau event. |
| Expected | Listener hoàn tất không lỗi, không tạo cache/mã từ event; P1 đọc F2 và sinh `solveNew`. |
| Cleanup | Reset cache/spec fixture |
| Evidence | Event, cache miss/read trace, P1 output |
| Design review | Đã review; chưa thực thi |
