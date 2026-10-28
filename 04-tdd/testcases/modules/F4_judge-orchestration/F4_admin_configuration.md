# Test case QA — F4: lệnh và cấu hình quản trị

> 15 ca thiết kế cho AC-51–54, 57–62; chưa thực thi. Nguồn: TDD F4 các AC tương ứng; DD F4 API §19–21, §23–24; logic BR-JD-09. Dùng hai admin A1/A2, một instructor I, một student S; đọc trạng thái từ DB trước/sau và `system_audit_logs`. Mỗi ca hoàn nguyên cấu hình và chỉ dọn fixture riêng.

| AC | Nhánh | ID | Review | Thực thi |
| :--- | :--- | :--- | :--- | :--- |
| 51 | Xung đột pause | TC-F4-AC51-01 | Đã review | Chưa thực hiện |
| 52 | Autoscale tắt/bật, xung đột | TC-F4-AC52-01–02 | Đã review | Chưa thực hiện |
| 53 | Tham số hợp lệ và dữ liệu sai | TC-F4-AC53-01–02 | Đã review | Chưa thực hiện |
| 54 | Quyền ghi bốn cổng | TC-F4-AC54-01 | Đã review | Chưa thực hiện |
| 57 | Đọc đủ bốn khối và quyền | TC-F4-AC57-01–02 | Đã review | Chưa thực hiện |
| 58 | Thời điểm áp dụng và không đổi | TC-F4-AC58-01–02 | Đã review | Chưa thực hiện |
| 59 | Ngôn ngữ cuối | TC-F4-AC59-01 | Đã review | Chưa thực hiện |
| 60 | Ranh giới hệ số/hiệu dụng/script | TC-F4-AC60-01–02 | Đã review | Chưa thực hiện |
| 61 | Phiên bản cũ | TC-F4-AC61-01 | Đã review | Chưa thực hiện |
| 62 | Audit lỗi và retry | TC-F4-AC62-01 | Đã review | Chưa thực hiện |

### TC-F4-AC51-01 — Hai admin cùng đặt trạng thái queue

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-51; DD F4 API §19, logic BR-JD-09 |
| Kind / priority | Concurrency, P0: lệnh cũ không ghi đè trạng thái mới |
| Environment | API `PUT /admin/queue/consumption`, DB cluster_settings, AuditLogPort, FE ADM0401 |
| Preconditions | Queue `paused=false`; A1/A2 cùng đọc snapshot false; audit count N |
| Test data | A1 gửi `{paused:true,expectedPaused:false}`; A2 gửi cùng body từ màn cũ |
| Steps | Gửi tuần tự hai PUT, đọc lại snapshot/DB/audit. |
| Expected | A1 thành công, queue paused=true, audit count N+1 với actor A1 và before false/after true. A2 nhận `409 JDG-201`, FE báo trạng thái đã thay đổi và tải lại; DB vẫn true, không thêm audit thứ hai, job đang chấm không bị huỷ. |
| Cleanup | Đặt lại paused=false, dọn audit fixture |
| Evidence | Hai request/response, DB state và audit IDs, ảnh màn |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC52-01 — Tắt/bật autoscale và đọc snapshot

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-52; DD F4 API §20 T-QA-01/04; logic BR-JD-09 |
| Kind / priority | Normal, P1: cấu hình và controller thống nhất |
| Environment | API `PUT /admin/queue/autoscale`, snapshot queue, DB/audit, JOB-JD-05 |
| Preconditions | `autoscale_enabled=true`, `autoscaleJobThreshold=20`, controller có thể đang có 2 consumer; audit count N |
| Test data | `{enabled:false,expectedEnabled:true}` rồi `{enabled:true,expectedEnabled:false}` |
| Steps | PUT tắt, GET snapshot và quan sát poll controller; PUT bật, GET lại. |
| Expected | Mỗi PUT 200 và trạng thái đọc lại đúng; dòng mô tả vẫn nêu ngưỡng 20. Khi tắt controller drain về một consumer, không dừng job đang chạy; khi bật controller đọc lại cờ ở poll tiếp theo. Hai lần đổi tạo đúng hai audit với before/after tương ứng; PUT chỉ xác nhận lưu, không đợi scale xong. |
| Cleanup | Đặt lại trạng thái ban đầu, dọn audit fixture |
| Evidence | Hai response, snapshot, DB/audit, controller metrics |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC52-02 — `expectedEnabled` cũ bị từ chối

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-52; DD F4 API §20 T-QA-02 |
| Kind / priority | Concurrency, P0: chống ghi đè autoscale |
| Environment | API #17, DB/audit |
| Preconditions | A1/A2 cùng thấy `autoscale_enabled=true`; A1 đã tắt thành false; audit count N sau A1 |
| Test data | A2 gửi `{enabled:false,expectedEnabled:true}` |
| Steps | Gọi PUT của A2, đọc DB/audit. |
| Expected | `409 JDG-201`; trạng thái vẫn false, audit count vẫn N. Không phát tác dụng cấu hình mới cho controller. |
| Cleanup | Khôi phục true |
| Evidence | Request/response, DB/audit |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC53-01 — Tham số queue nhận số nguyên lớn và có hiệu lực lần đọc sau

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-53; DD F4 API §21.2/21.4 T-QT-01/03/05 |
| Kind / priority | Normal + boundary, P1 |
| Environment | GET/PUT `/admin/queue/settings`, danh sách job, DB/audit |
| Preconditions | Giá trị cũ `{autoscaleJobThreshold:20,jobsPerPage:20,errorRetentionHours:24,latencyCapSeconds:10}`; có 8 job; audit N |
| Test data | PUT `{5,7,100000,20}` theo thứ tự bốn trường; sau đó PUT `errorRetentionHours=48` |
| Steps | Lưu, GET lại settings, tải danh sách job và thanh độ trễ, lưu thay đổi 48. |
| Expected | Cả hai PUT 200; lần đầu bốn trường đọc lại đúng, 100000 được nhận; trang job tiếp theo có tối đa 7 dòng, ngưỡng/latency cap mới hiển thị ở lần tải sau. Mỗi lần có đúng một audit, chỉ các trường thực đổi với before/after; không áp trần nghiệp vụ ngoài kiểu INT. |
| Cleanup | Khôi phục bốn giá trị gốc, dọn audit fixture |
| Evidence | PUT/GET, DB/audit, danh sách job và ảnh màn |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC53-02 — Giá trị không phải số nguyên dương bị loại toàn bộ

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-53; DD F4 API §21.2/21.3 T-QT-02 |
| Kind / priority | Abnormal + boundary, P1 |
| Environment | API #19, DB/audit |
| Preconditions | Settings gốc `{20,20,24,10}`, audit count N |
| Test data | Mỗi request độc lập thay một trường thành `0`, `1.5`, `""`, `"abc"`, `2147483648`; các trường khác giữ hợp lệ |
| Steps | Gửi từng biến thể, đọc DB/audit sau mỗi lần. |
| Expected | Cả năm trả `400 JDG-V09`; bốn cột settings không đổi, audit count N, không có phần cập nhật thành công. |
| Cleanup | Không có thay đổi |
| Evidence | Năm response, DB/audit trước/sau |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC54-01 — Quyền ghi kiểm lại ở máy chủ trên cả bốn cổng

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-54; DD F4 API §19–21, §24; BR-JD-09 kiểm lệnh ghi |
| Kind / priority | Security, P0: FE ẩn nút không phải kiểm quyền |
| Environment | Bốn PUT quản trị, JWT instructor/student và admin chỉ có LANGUAGE_CONFIG:READ, DB/audit |
| Preconditions | Các request body đều hợp lệ, timestamp cấu hình đúng; snapshot DB/audit được ghi trước |
| Test data | Instructor và student gọi PUT consumption/autoscale/settings; admin chỉ có READ gọi PUT language-config |
| Steps | Gọi trực tiếp từng endpoint, không qua FE; so DB và audit. |
| Expected | Mọi request trả `403 JDG-301`; không thay cờ, ngưỡng, ngôn ngữ hay sandbox; không thêm audit. GET #21/#23 vẫn theo quyền đọc tương ứng, không suy ra quyền ghi. |
| Cleanup | Không có thay đổi |
| Evidence | Claims đã che bí mật, request/response, DB/audit diff |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC57-01 — Một GET trả đủ cấu hình và số job chờ

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-57; DD F4 API §23.2/23.4 T-LC-01–03 |
| Kind / priority | Normal, P1: màn không cần ghép dữ liệu thiếu |
| Environment | GET `/admin/language-config`, DB, FE ADM0501 |
| Preconditions | Seed đúng JAVA/CPP/PYTHON; PYTHON có 4 PENDING, 1 JUDGING; các ngôn ngữ khác 0 PENDING |
| Test data | GET bằng admin có LANGUAGE_CONFIG:READ |
| Steps | Gọi một GET, kiểm toàn JSON và tải màn. |
| Expected | 200; `languages[]` đúng ba mã, mỗi dòng có display name, hai hệ số, enabled, updatedAt và pendingJobCount (PYTHON=4, không tính JUDGING); có gradingDefaults bốn giới hạn + updatedAt, sandbox ba boolean + updatedAt, generatorSandbox hai giới hạn + updatedAt. Không có `maxChildProcesses` hay ô chỉnh số tiến trình con; FE tính giới hạn hiệu dụng từ mặc định và hệ số. Không ghi DB. |
| Cleanup | Dọn job fixture |
| Evidence | JSON, DB counts, ảnh màn |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC57-02 — Thiếu quyền xem cấu hình ngôn ngữ

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-57; DD F4 API §23.3 T-LC-04 |
| Kind / priority | Security, P0 |
| Environment | GET #21, JWT không có LANGUAGE_CONFIG:READ |
| Preconditions | Instructor I và student S đã đăng nhập |
| Test data | Hai GET `/admin/language-config` |
| Steps | Gọi bằng từng JWT, kiểm body. |
| Expected | Cả hai `403 JDG-301`, không có hệ số/công tắc/giới hạn trong response; không ghi DB. |
| Cleanup | Không ghi dữ liệu |
| Evidence | Hai response và claims đã che bí mật |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC58-01 — Worker giữ cấu hình lúc bắt đầu chấm

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-58; DD F4 API §24.1/24.5 T-UL-01/02; BR-JD-09 |
| Kind / priority | Concurrency, P0: không đổi giới hạn giữa lượt chấm |
| Environment | PUT #22, worker có barrier kiểm thử, DB/audit |
| Preconditions | JAVA timeMultiplier=2.00; job J1 đã bắt đầu và đọc cấu hình; J2 PENDING. Giá trị mặc định/giới hạn bài cho phép đổi sang 3.00; audit N. |
| Test data | A1 PUT toàn bộ DTO giữ nguyên trừ JAVA timeMultiplier=3.00, dùng updatedAt từ GET |
| Steps | Giữ J1 sau khi đọc cấu hình; PUT; thả J1 và cho J2 bắt đầu; đo giới hạn thực tế của hai lượt. |
| Expected | PUT 200, DB=3.00, audit N+1 chỉ có trường Java 2.00→3.00. J1 dùng hệ số 2.00 tới cuối; J2 dùng 3.00 vì bắt đầu sau PUT, dù đã nộp trước. Không cần restart và không lưu ảnh chụp giới hạn vào submission. |
| Cleanup | Khôi phục Java=2.00, dọn job/audit fixture |
| Evidence | GET/PUT, worker config-read trace, giới hạn sandbox, DB/audit |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC58-02 — PUT không đổi giá trị không tạo audit

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-58; DD F4 API §24.5 bước 3–4; BR-JD-09 |
| Kind / priority | Idempotency, P1: không tạo dấu vết giả |
| Environment | GET/PUT #22, DB/audit |
| Preconditions | Admin A1 có DTO mới nhất từ GET; audit count N, updatedAt T |
| Test data | PUT y nguyên DTO đầu vào hợp lệ |
| Steps | Gọi PUT, đọc DB/audit và GET lại. |
| Expected | 200; mọi giá trị và updatedAt T không đổi; audit count N; không phát sự kiện miền. |
| Cleanup | Không thay đổi |
| Evidence | GET/PUT/GET, DB/audit |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC59-01 — Không tắt cả ba ngôn ngữ

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-59; DD F4 API §24.2/24.4 T-UL-03 |
| Kind / priority | Abnormal, P0: giữ ít nhất một ngôn ngữ chạy được |
| Environment | PUT #22, DB/audit |
| Preconditions | Chỉ JAVA enabled=true, CPP/PYTHON=false; updatedAt hiện hành, audit N |
| Test data | PUT DTO hợp lệ ngoại trừ JAVA enabled=false |
| Steps | Gọi trực tiếp API, bỏ qua FE, đọc ba cờ/audit. |
| Expected | `422 JDG-203`; JAVA vẫn true, CPP/PYTHON false, không thêm audit, không ảnh hưởng job chờ. |
| Cleanup | Khôi phục fixture nếu cần |
| Evidence | Request/response, DB/audit |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC60-01 — Ranh giới hệ số và giới hạn hiệu dụng

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-60; DD F4 API §24.2/24.4 T-UL-04/05; BR-JD-09 |
| Kind / priority | Boundary, P0: cấu hình ngoài sandbox không được lưu |
| Environment | PUT #22, DB/audit |
| Preconditions | Cấu hình nền hợp lệ: mặc định time=1000 ms, memory=256 MB; các ngôn ngữ enabled, hệ số nền 1.00; mỗi biến thể dùng bản sao DB riêng và updatedAt mới |
| Test data | Lần lượt Java timeMultiplier `0.09`, `10.00` → V10; time=4000 và multiplier=9.99 → 39960 ms/V204; memory=100 MB và multiplier=0.10 → 10 MB/V205; hợp lệ timeMultiplier 0.10 và 9.99 với time=1000 ms, memory hiệu dụng giữ trong 64–1024 MB |
| Steps | Gửi từng DTO độc lập, đọc DB/audit ngay sau mỗi request; gửi hai cấu hình hợp lệ sát biên. |
| Expected | Hai hệ số sai trả `400 JDG-V10`; thời gian hiệu dụng 39960 trả `422 JDG-204`; bộ nhớ hiệu dụng 10 trả `422 JDG-205`. Mỗi lỗi không ghi bất kỳ phần nào và không audit. Hai cấu hình hợp lệ trả 200, time hiệu dụng lần lượt 100 và 9990 ms, có đúng một audit cho mỗi lần đổi. |
| Cleanup | Khôi phục cấu hình nền, dọn audit fixture |
| Evidence | Sáu response, DB/audit từng lần, phép tính hiệu dụng |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC60-02 — Giới hạn script và kiểu số

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-60; DD F4 API §24.2/24.4 |
| Kind / priority | Boundary + abnormal, P1 |
| Environment | PUT #22, DB/audit |
| Preconditions | DTO nền hợp lệ, updatedAt mới; audit N |
| Test data | `maxRuntimeSeconds=0`, `301`; `maxOutputMb=0`, `1025`; `timeLimitMs=1.5`; sau đó `maxRuntimeSeconds=1/300`, `maxOutputMb=1/1024` |
| Steps | Gửi từng request độc lập, xác nhận DB/audit sau lỗi; gửi các giá trị biên hợp lệ. |
| Expected | Bốn giá trị script ngoài khoảng trả `400 JDG-V12`; mặc định không nguyên trả `400 JDG-V11`; không ghi/audit ở năm lỗi. Các biên script hợp lệ trả 200 nếu những giới hạn hiệu dụng khác còn hợp lệ; mỗi lần có đổi ghi một audit. |
| Cleanup | Khôi phục nền, dọn audit fixture |
| Evidence | Request/response, DB/audit |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC61-01 — `updatedAt` cũ không ghi đè cấu hình mới

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-61; DD F4 API §24.4/24.5 T-UL-06 |
| Kind / priority | Concurrency, P0 |
| Environment | GET/PUT #22, DB/audit, FE ADM0501 |
| Preconditions | A1/A2 GET cùng DTO, Java multiplier=2.00, audit N |
| Test data | A1 PUT Java=3.00 với timestamps cũ; A2 PUT Java=4.00 cùng timestamps cũ |
| Steps | Lưu A1 rồi A2, GET lại; quan sát UI A2. |
| Expected | A1 200; A2 `409 JDG-202`, UI yêu cầu tải lại; Java vẫn 3.00 và các khối khác không đổi; đúng một audit (A1). |
| Cleanup | Khôi phục Java=2.00, dọn audit fixture |
| Evidence | Ba DTO, hai response, DB/audit, ảnh màn |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC62-01 — Audit lỗi rollback toàn bộ, retry thành công

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-62; DD F4 logic BR-JD-09 kiểm toán cùng giao dịch; API §19–21, §24 |
| Kind / priority | Recovery + atomicity, P0: không lưu thiếu dấu vết |
| Environment | DB transaction, AuditLogPort fault injection, bốn PUT quản trị |
| Preconditions | Snapshot config/queue/audit ban đầu; mỗi lệnh đổi một trường hợp lệ và dùng expected/current timestamp đúng |
| Test data | Lần lượt lỗi AuditLogPort cho PUT consumption, autoscale, settings, language-config; sau đó khôi phục cổng và retry mỗi lệnh bằng trạng thái/timestamp còn hiện hành |
| Steps | Với từng endpoint, gây lỗi ghi audit, gọi PUT, so DB/audit; khôi phục, retry rồi so lại. |
| Expected | Khi audit lỗi, request thất bại và toàn bộ thay đổi của lệnh rollback, không có trạng thái “đã lưu” hay audit thiếu. Sau khôi phục, retry thành công và tạo đúng một audit cùng thay đổi DB. Không phát sự kiện miền. |
| Cleanup | Khôi phục toàn bộ cấu hình, dọn audit fixture |
| Evidence | Tám response, transaction trace, DB/audit trước/sau |
| Design review | Đã review; chưa thực thi |
