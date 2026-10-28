# Test case QA — F4: tiếp nhận bài nộp và kiểm đầu vào

> Trạng thái: 24 ca đã review thiết kế; chưa thực thi. Nguồn: `04-tdd/modules/F4_judge-orchestration.md` AC-01–07, AC-09; `03-dd/api/F4_judge-orchestration.md` §5–6, §24; `03-dd/logic/F4_judge-orchestration.md` BR-JD-01, BR-JD-09. Kết quả thực thi sẽ ghi ở run record riêng.

## Fixture chung

- `A1`: học viên có JWT `baseCategory=STUDENT`; `ADM1`: quản trị viên được sửa cấu hình ngôn ngữ. `P-X`, `P-Y`: bài Standard I/O đã `PUBLISHED`, snapshot hiện hành sẵn sàng, mỗi bài có 12 testcase đã duyệt gồm Sample; `maxSubmissionsPerHour=NULL`. `P-Z` tương tự nhưng `maxSubmissionsPerHour=5`. `P-S`: bài chỉ hỗ trợ Standard I/O. Dùng UUID riêng cho từng bài và lưu trong evidence.
- Mặc định Python và Java bật, RabbitMQ xác nhận publish, worker có thể giữ bằng barrier để quan sát `PENDING`. Request nộp mặc định: `POST /problems/{problemId}/submissions`, `language=PYTHON`, `submissionModel=STANDARD_IO`, `sourceInputMethod=EDITOR`, `sourceCode="print(2)\n"`. Request Run Code mặc định: endpoint #2 trong DD F4 API, cùng bài/ngôn ngữ/mô hình/mã; Sample input rỗng, expected `2\n`.
- `SRC-65536` là chuỗi ASCII `print(2)\n#` (10 byte) nối với 65.526 ký tự `x`, tổng 65.536 byte. `SRC-65537` thay bằng 65.527 ký tự `x`. `SRC-UNICODE-65537` là tiền tố 10 byte đó, tiếp 32.763 ký tự `á` (65.526 byte UTF-8) và một `x`: 65.537 byte nhưng chỉ 32.774 ký tự. Tạo fixture nguyên văn bằng trình sinh dữ liệu, xác nhận độ dài byte trước khi gửi; không gắn thêm newline.
- Các ca độc lập: xoá submission, kết quả, Outbox và message phát sinh; khôi phục cấu hình ngôn ngữ, clock và barrier sau mỗi ca. Với ca từ chối, so số dòng DB và số message/event trước–sau, bao gồm kiểm tra không có bản ghi một phần.

## Ma trận nhánh

| AC | Nhánh | Test case | Review | Thực thi |
| :--- | :--- | :--- | :--- | :--- |
| AC-01 | Ticket đang chờ và lịch sử | TC-F4-AC01-01 | Đã review | Chưa thực hiện |
| AC-02 | Lượt 61, bài khác, mép cửa sổ giờ | TC-F4-AC02-01 đến -03 | Đã review | Chưa thực hiện |
| AC-03 | Giới hạn riêng 5 | TC-F4-AC03-01 | Đã review | Chưa thực hiện |
| AC-04 | Trống, 65.536, 65.537, Unicode ở Submit và Run Code | TC-F4-AC04-01 đến -08 | Đã review | Chưa thực hiện |
| AC-05 | Bài mới bị chặn ở Submit/Run Code, ba bài cũ chạy nốt | TC-F4-AC05-01 đến -03 | Đã review | Chưa thực hiện |
| AC-06 | Wrapper bị chặn ở hai API; bài hỗ trợ cả hai mô hình | TC-F4-AC06-01 đến -04 | Đã review | Chưa thực hiện |
| AC-07 | Đuôi đúng, sai hoặc thiếu | TC-F4-AC07-01 đến -03 | Đã review | Chưa thực hiện |
| AC-09 | Hai lượt tranh suất cuối | TC-F4-AC09-01 | Đã review | Chưa thực hiện |

## Test case

### TC-F4-AC01-01 — Nộp bài trả ticket khi chưa chấm

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-01; DD F4 API §6.3, §6.5 T-CS-01, §7.3 |
| Kind / priority | Normal, P0: hợp đồng tiếp nhận và hiển thị đang chờ |
| Environment | API, PostgreSQL, RabbitMQ, worker barrier, lịch sử bài nộp |
| Preconditions | `A1` chưa chạm quota; `P-X` sẵn sàng; giữ worker trước khi nhận message |
| Test data | Mã Java `class Main { public static void main(String[] args) { System.out.println(2); } }`; `language=JAVA`, `submissionModel=STANDARD_IO`, `sourceInputMethod=EDITOR` |
| Steps | POST nộp; giữ barrier; lấy ticket, GET lịch sử bài `P-X`, kiểm DB/message; sau đó thả worker. |
| Expected | POST `201`, UUID `submissionId`, `status=PENDING`, `language=JAVA`, `submittedAt` khớp DB, `totalCountHint=12`, response đến khi worker vẫn bị giữ. DB có đúng một dòng `PENDING`, `passed_testcase_count=0`, `total_testcase_count=NULL`, `score_ratio=NULL`; lịch sử của `A1` có dòng này với `passedCount`, `totalCount`, `scoreRatio` đều `null` để UI hiện `-`, không hiện `0/0`. Message chỉ chứa `submissionId`, không chứa mã nguồn. |
| Cleanup | Thả barrier; xoá dữ liệu ca |
| Evidence | Request/response có thời điểm, GET lịch sử, DB và message |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC02-01 — Lượt 60 được nhận, lượt 61 bị chặn

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-02; DD F4 API §6.4–6.5 T-CS-02/T-CS-03 |
| Kind / priority | Boundary, P0: không vượt quota mặc định |
| Environment | API, PostgreSQL, RabbitMQ, clock DB kiểm soát |
| Preconditions | `P-X.maxSubmissionsPerHour=NULL`; `A1` có đúng 59 lượt tại `T0-30m` cho `P-X`, không có lượt khác trong giờ; `T0=2026-10-26T03:00:00Z` |
| Test data | Hai request nộp mặc định liên tiếp tại `T0`; mặc định hệ thống 60 |
| Steps | Đếm lượt trước; POST lần 60 rồi POST lần 61; kiểm DB, queue và Outbox. |
| Expected | Lần 60 `201 PENDING`; lần 61 `429 JDG-106`, `details.limit=60`, `windowMinutes=60`. Chỉ một submission mới, tổng đúng 60; request bị từ chối không tạo submission, message hay event. |
| Cleanup | Xoá 59 lượt seed và dữ liệu phát sinh |
| Evidence | Hai response, clock, count SQL trước–sau, message/Outbox |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC02-02 — Quota độc lập theo bài

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-02; DD F4 API §6.5 bước 4–5 |
| Kind / priority | Boundary, P1: không chặn nhầm bài khác |
| Environment | API, PostgreSQL, RabbitMQ |
| Preconditions | `A1` có đúng 60 submission `P-X` trong giờ, không có submission `P-Y` trong giờ |
| Test data | POST mặc định cho `P-Y` |
| Steps | Gọi POST cho `P-Y`; đếm riêng hai cặp `(A1,P-X)` và `(A1,P-Y)`. |
| Expected | `201 PENDING` cho `P-Y`; `P-X` vẫn 60, `P-Y` thành 1; message chỉ cho submission `P-Y`. |
| Cleanup | Xoá dữ liệu ca |
| Evidence | Response, SQL đếm theo user/problem, message |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC02-03 — Lượt ở đúng mép một giờ rời cửa sổ

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-02; DD F4 API §6.5 bước 5: `submitted_at > now() - interval '1 hour'` |
| Kind / priority | Boundary, P1: dấu `>` ở mép cửa sổ |
| Environment | API, PostgreSQL, clock DB kiểm soát |
| Preconditions | Tại `T0=2026-10-26T03:00:00Z`, `A1/P-X` có 59 lượt tại `T0-30m` và một lượt tại đúng `T0-1h`; không có lượt khác |
| Test data | Một POST mặc định cho `P-X` tại `T0` |
| Steps | Cố định clock DB tại `T0`; đếm query cửa sổ, POST, đếm lại. |
| Expected | Lượt đúng `T0-1h` không được đếm; trước POST có 59 trong cửa sổ, POST `201`, sau POST có 60. Không trả `JDG-106`. |
| Cleanup | Khôi phục clock; xoá dữ liệu ca |
| Evidence | Timestamps DB, câu query và counts, response |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC03-01 — Giới hạn riêng 5 thay mặc định 60

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-03; DD F4 API §6.4–6.5 T-CS-03 |
| Kind / priority | Boundary, P0: áp cấu hình của bài |
| Environment | API, PostgreSQL, RabbitMQ |
| Preconditions | `P-Z.maxSubmissionsPerHour=5`; `A1` có đúng 4 lượt `P-Z` trong giờ |
| Test data | Hai POST mặc định cho `P-Z` |
| Steps | POST lượt 5 rồi lượt 6; đọc DB và message. |
| Expected | Lượt 5 `201`, lượt 6 `429 JDG-106` với `details={limit:5,windowMinutes:60}`; tổng 5, chỉ một message mới, không tạo dòng/event cho lượt 6. |
| Cleanup | Xoá dữ liệu ca |
| Evidence | Hai response, DB và queue/Outbox |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC04-01 — Submit từ chối mã toàn khoảng trắng

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-04; DD F4 API §6.2, §6.4 `JDG-V01` |
| Kind / priority | Abnormal, P1: validate máy chủ |
| Environment | API, PostgreSQL, RabbitMQ |
| Preconditions | `A1/P-X` dưới quota; gọi thẳng API |
| Test data | `sourceCode=" \t\n  "`, các trường khác theo request mặc định |
| Steps | Ghi count DB/message, POST, so count sau. |
| Expected | `400 JDG-V01`; không có submission, message hay event mới. |
| Cleanup | Không có dữ liệu ghi; reset fixture |
| Evidence | Payload/response, counts trước–sau |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC04-02 — Submit nhận đúng 65.536 byte

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-04; DD F4 API §6.2, §6.7 T-CS-04 |
| Kind / priority | Boundary, P1: ngưỡng chấp nhận |
| Environment | API, PostgreSQL, RabbitMQ |
| Preconditions | `A1/P-X` dưới quota, Python bật |
| Test data | `SRC-65536`, xác nhận `octet_length=65536` |
| Steps | POST với mã fixture; đọc dòng đã lưu. |
| Expected | `201 PENDING`; DB lưu nguyên 65.536 byte, đúng một submission/message; lỗi biên dịch hay chạy sau đó không thay đổi kết luận đầu vào được nhận. |
| Cleanup | Xoá dữ liệu ca |
| Evidence | Byte count fixture và DB, response, message |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC04-03 — Submit từ chối 65.537 byte ASCII

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-04; DD F4 API §6.2, §6.4 `JDG-V02` |
| Kind / priority | Boundary, P1: ngưỡng từ chối |
| Environment | API, PostgreSQL, RabbitMQ |
| Preconditions | `A1/P-X` dưới quota |
| Test data | `SRC-65537`, xác nhận 65.537 byte |
| Steps | POST trực tiếp; đối chiếu DB/message/Outbox trước–sau. |
| Expected | `400 JDG-V02`; không có submission, message hay event mới. |
| Cleanup | Không có dữ liệu ghi |
| Evidence | Byte count, response, DB/message/Outbox delta |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC04-04 — Submit tính byte UTF-8 thay vì số ký tự

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-04; DD F4 API §6.2, §6.4 `JDG-V02` |
| Kind / priority | Boundary, P1: mã tiếng Việt |
| Environment | API, PostgreSQL, RabbitMQ |
| Preconditions | `A1/P-X` dưới quota |
| Test data | `SRC-UNICODE-65537`, 32.774 ký tự và 65.537 byte UTF-8 |
| Steps | Đo fixture; POST; đối chiếu DB/message/Outbox. |
| Expected | `400 JDG-V02`; không tạo submission, message hay event dù số ký tự nhỏ hơn 65.536. |
| Cleanup | Không có dữ liệu ghi |
| Evidence | Length ký tự/byte, response, delta DB/queue |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC04-05 — Run Code từ chối mã toàn khoảng trắng

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-04; DD F4 API §5.2, §5.4 `JDG-V01` |
| Kind / priority | Abnormal, P1: cùng validation ở Run Code |
| Environment | API, PostgreSQL, RabbitMQ |
| Preconditions | `A1/P-X` và Sample sẵn sàng |
| Test data | Request Run Code mặc định với `sourceCode=" \t\n  "` |
| Steps | Ghi count submission, gọi Run Code trực tiếp, kiểm count. |
| Expected | `400 JDG-V01`; không chạy Sample, không ghi submission/kết quả/event. |
| Cleanup | Không có dữ liệu ghi |
| Evidence | Payload/response, DB và engine invocation count |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC04-06 — Run Code nhận đúng 65.536 byte

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-04; DD F4 API §5.2, §5.7 T-RS-05 |
| Kind / priority | Boundary, P1: ngưỡng chấp nhận Run Code |
| Environment | API, go-judge, PostgreSQL |
| Preconditions | `A1/P-X` và Sample sẵn sàng |
| Test data | Request Run Code với `SRC-65536`; Python bình luận dài hợp lệ, output `2\n` |
| Steps | Xác nhận byte count; gọi Run Code; kiểm kết quả và count submission. |
| Expected | `200`, `outcome=COMPLETED`, Sample `AC` với output `2\n`; không ghi submission, kết quả DB hay event. |
| Cleanup | Reset fixture |
| Evidence | Byte count, response, engine log, DB counts |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC04-07 — Run Code từ chối 65.537 byte ASCII

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-04; DD F4 API §5.2, §5.4 `JDG-V02` |
| Kind / priority | Boundary, P1: ngưỡng từ chối Run Code |
| Environment | API, go-judge, PostgreSQL |
| Preconditions | `A1/P-X` và Sample sẵn sàng |
| Test data | `SRC-65537` |
| Steps | Gọi Run Code, so engine invocation và DB trước–sau. |
| Expected | `400 JDG-V02`; không gọi engine, không ghi submission/kết quả/event. |
| Cleanup | Không có dữ liệu ghi |
| Evidence | Byte count, response, engine log và DB delta |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC04-08 — Run Code từ chối 65.537 byte UTF-8

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-04; DD F4 API §5.2, §5.4 `JDG-V02` |
| Kind / priority | Boundary, P1: đo byte Unicode ở Run Code |
| Environment | API, go-judge, PostgreSQL |
| Preconditions | `A1/P-X` và Sample sẵn sàng |
| Test data | `SRC-UNICODE-65537` |
| Steps | Đo ký tự/byte, gọi Run Code, kiểm engine và DB. |
| Expected | `400 JDG-V02`; không gọi engine, không ghi submission/kết quả/event. |
| Cleanup | Không có dữ liệu ghi |
| Evidence | Length, response, engine log, DB delta |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC05-01 — Ngôn ngữ bị tắt chặn nộp mới

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-05; DD F4 API §6.4 `JDG-102`, §24.5 |
| Kind / priority | Abnormal, P0: không nhận bài bằng ngôn ngữ tắt |
| Environment | API, PostgreSQL, cấu hình ngôn ngữ admin |
| Preconditions | `ADM1` tắt Python qua cấu hình §24; Java/C++ vẫn bật; `A1/P-X` dưới quota |
| Test data | POST mặc định với `language=PYTHON` |
| Steps | GET cấu hình xác nhận Python tắt; POST; so DB/message/Outbox. |
| Expected | `422 JDG-102`; không có submission, message hay event mới. Không cần khởi động lại dịch vụ để hiệu lực. |
| Cleanup | Bật lại Python; xoá dữ liệu ca |
| Evidence | Cấu hình trước–sau, response, DB/queue delta |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC05-02 — Ba bài Python đã chờ vẫn được chấm sau khi tắt

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-05; DD F4 API §6.7 T-CS-05, §24.5 T-UL-07; logic BR-JD-09 |
| Kind / priority | Recovery, P0: cấu hình mới không huỷ job cũ |
| Environment | API, PostgreSQL, RabbitMQ, worker barrier, go-judge, admin config |
| Preconditions | Python bật; `A1/P-X` dưới quota; giữ worker trước khi lấy ba message |
| Test data | Ba POST Python mã `print(2)\n`, ba `submissionId` khác nhau |
| Steps | POST ba lần, xác nhận ba `PENDING` và message đã confirm; dùng `ADM1` tắt Python khi Java/C++ bật; thả worker; theo dõi ba ID tới trạng thái cuối. |
| Expected | Cả ba job nhận trước khi tắt được chấm đến trạng thái cuối theo kết quả testcase thực tế; không bị huỷ hoặc thành `SYSTEM_ERROR` vì Python bị tắt. Chỉ bài mới sau thời điểm tắt nhận `JDG-102` (ca AC05-01). |
| Cleanup | Bật Python, thả barrier, xoá dữ liệu ca |
| Evidence | Timeline POST/cấu hình/worker, DB status, kết quả và event |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC06-01 — Submit từ chối Wrapper trên bài Standard I/O

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-06; DD F4 API §6.2, §6.4 `JDG-103` |
| Kind / priority | Security, P1: không dựa vào nút đã ẩn ở FE |
| Environment | API, PostgreSQL, RabbitMQ |
| Preconditions | `P-S.functionWrapperSupported=false`; `A1` dưới quota |
| Test data | Request trực tiếp cho `P-S`, `submissionModel=FUNCTION_WRAPPER`, `language=PYTHON`, `sourceCode="def solve(): return 2"` |
| Steps | Bỏ qua FE, POST; kiểm DB/message/Outbox. |
| Expected | `422 JDG-103`; không tạo submission, message hay event. |
| Cleanup | Không có dữ liệu ghi |
| Evidence | Request/response và DB/queue delta |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC06-02 — Run Code từ chối Wrapper trên bài Standard I/O

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-06; DD F4 API §5.2, §5.4 `JDG-103` |
| Kind / priority | Security, P1: server kiểm mô hình cho Run Code |
| Environment | API, go-judge, PostgreSQL |
| Preconditions | `P-S.functionWrapperSupported=false` và có Sample |
| Test data | Request Run Code trực tiếp với `submissionModel=FUNCTION_WRAPPER`, mã `def solve(): return 2` |
| Steps | Gọi Run Code, kiểm engine invocation và DB. |
| Expected | `422 JDG-103`; không gọi engine, không ghi submission/kết quả/event. |
| Cleanup | Không có dữ liệu ghi |
| Evidence | Request/response, engine log và DB delta |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC07-01 — Tệp Python đúng đuôi được nhận, không lưu tên

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-07; DD F4 API §6.2, §6.7 T-CS-09; database F4 `source_input_method` |
| Kind / priority | Normal, P1: ghi nhận nguồn file |
| Environment | API, PostgreSQL, RabbitMQ |
| Preconditions | `A1/P-X` dưới quota, Python bật |
| Test data | `sourceInputMethod=FILE_UPLOAD`, `fileName=main.py`, `language=PYTHON`, nội dung `print(2)\n` |
| Steps | POST; đọc submission, message và các trường lưu trong DB. |
| Expected | `201 PENDING`; DB `source_input_method=FILE_UPLOAD`, `source_code` đúng nội dung; không có trường/giá trị tên tệp được lưu với submission, message chỉ mang ID. |
| Cleanup | Xoá dữ liệu ca |
| Evidence | Request/response, schema/DB row, message |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC07-02 — Tệp Python sai đuôi bị chặn

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-07; DD F4 API §6.2, §6.4 `JDG-V06` |
| Kind / priority | Abnormal, P1: chống khai sai loại tệp |
| Environment | API, PostgreSQL, RabbitMQ |
| Preconditions | `A1/P-X` dưới quota, Python bật |
| Test data | `sourceInputMethod=FILE_UPLOAD`, `fileName=Main.java`, `language=PYTHON`, nội dung `print(2)\n` |
| Steps | POST trực tiếp; so DB/message/Outbox trước–sau. |
| Expected | `400 JDG-V06`; không có submission, message hay event mới; tên tệp không được lưu. |
| Cleanup | Không có dữ liệu ghi |
| Evidence | Request/response, DB/queue delta |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC09-01 — Hai lượt đồng thời tranh một suất cuối

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-09; DD F4 API §6.5 bước 4–5, §6.7 T-CS-07 |
| Kind / priority | Concurrency, P0: quota phải nguyên tử |
| Environment | Hai client/API, PostgreSQL advisory lock, RabbitMQ, barrier giao dịch |
| Preconditions | `A1/P-X` có đúng 59 submission trong giờ, `maxSubmissionsPerHour=NULL`; hai client dùng cùng JWT `A1` |
| Test data | Hai POST mặc định khác request ID, gửi đồng thời tại `T0` |
| Steps | Dùng barrier cho hai request tới bước khoá/đếm; thả cùng lúc; đợi cả hai response; kiểm DB/message/Outbox. |
| Expected | Chính xác một `201 PENDING` với UUID mới và một `429 JDG-106` (`limit=60`, `windowMinutes=60`), bất kể thứ tự client. Tổng submission trong giờ bằng 60; đúng một message mới, không có dòng/event cho request bị từ chối. |
| Cleanup | Gỡ barrier; xoá 59 lượt seed và dữ liệu ca |
| Evidence | Timeline hai request, response, SQL count, lock trace và queue/Outbox |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC05-03 — Run Code bằng ngôn ngữ đã tắt bị chặn

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-05; DD F4 API §5.4 `JDG-102`, §24.5 |
| Kind / priority | Abnormal, P1: cùng cấu hình cho chạy thử mới |
| Environment | API, PostgreSQL, go-judge, admin config |
| Preconditions | `ADM1` tắt Python; Java/C++ bật; `P-X` có Sample |
| Test data | Request Run Code mặc định bằng Python |
| Steps | GET cấu hình xác nhận Python tắt; gọi Run Code; kiểm engine và DB. |
| Expected | `422 JDG-102`; không gọi engine, không tạo submission/kết quả/event. |
| Cleanup | Bật lại Python |
| Evidence | Cấu hình, response, engine log, DB delta |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC06-03 — Bài hỗ trợ Wrapper nhận Submit mô hình Bọc hàm

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-06; DD F4 API §6.2–6.3 |
| Kind / priority | Normal, P1: không chặn nhầm bài hỗ trợ Wrapper |
| Environment | API, PostgreSQL, RabbitMQ, fixture F3 Wrapper hợp lệ |
| Preconditions | `P-W` đã xuất bản, `functionWrapperSupported=true`, snapshot Wrapper sẵn sàng; Python bật; `A1` dưới quota |
| Test data | Mã Python và chữ ký hàm đúng fixture Wrapper của `P-W`; `submissionModel=FUNCTION_WRAPPER` |
| Steps | POST bài `P-W`; kiểm ticket, DB và message. |
| Expected | `201 PENDING`, DB ghi `FUNCTION_WRAPPER` và mã nguyên vẹn; đúng một message chứa ID, không trả `JDG-103`. |
| Cleanup | Xoá dữ liệu ca |
| Evidence | Fixture F3, request/response, DB và message |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC06-04 — Bài hỗ trợ Wrapper nhận Run Code mô hình Bọc hàm

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-06; DD F4 API §5.2–5.3 |
| Kind / priority | Normal, P1: không chặn nhầm chạy thử Wrapper |
| Environment | API, go-judge, PostgreSQL, fixture F3 Wrapper hợp lệ |
| Preconditions | `P-W` đã xuất bản, `functionWrapperSupported=true`, có Sample Wrapper; Python bật |
| Test data | Mã và chữ ký hàm đúng fixture Wrapper của `P-W` |
| Steps | Gọi Run Code với `submissionModel=FUNCTION_WRAPPER`; đọc kết quả Sample và DB. |
| Expected | Response Run Code thành công theo fixture Sample, không trả `JDG-103`; không có submission/event mới. |
| Cleanup | Reset fixture |
| Evidence | Fixture F3, response, engine log và DB delta |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC07-03 — Tải file không gửi tên bị chặn

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-07; DD F4 API §6.2, §6.4 `JDG-V06` |
| Kind / priority | Abnormal, P1: ràng buộc tên file trên máy chủ |
| Environment | API, PostgreSQL, RabbitMQ |
| Preconditions | `A1/P-X` dưới quota, Python bật |
| Test data | `sourceInputMethod=FILE_UPLOAD`, không có `fileName`, `language=PYTHON`, `sourceCode="print(2)\n"` |
| Steps | POST trực tiếp; so DB/message/Outbox trước–sau. |
| Expected | `400 JDG-V06`; không tạo submission, message hay event. |
| Cleanup | Không có dữ liệu ghi |
| Evidence | Request/response, DB/queue delta |
| Design review | Đã review; chưa thực thi |
