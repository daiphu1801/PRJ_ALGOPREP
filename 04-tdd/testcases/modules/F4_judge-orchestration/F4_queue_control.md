# Test case QA — F4: danh sách job và điều khiển tiêu thụ hàng đợi

> Trạng thái: 8 ca đã review thiết kế; chưa thực thi. Các ca dùng clock và job fixture cô lập để kiểm thứ tự, cửa sổ giữ và hiệu ứng pause/resume.

## Phạm vi và fixture

- TDD: `04-tdd/modules/F4_judge-orchestration.md` AC-49, AC-50; DD F4 API §17, §19; logic BR-JD-08, BR-JD-15; JOB-JD-01.
- `T0=2026-10-26T03:00:00Z`; `ADMIN-A`, `ADMIN-B` là hai quản trị viên, `A2-OTHER` là giảng viên. `cluster_settings`: `jobs_per_page=20`, `error_retention_hours=24`, `queue_paused=false` trừ khi ca nêu khác.
- Mỗi ca dùng DB/queue namespace riêng. Job lỗi quá hạn bị loại khỏi **danh sách quản trị**, còn dòng `submissions` và lịch sử học viên được giữ.

## Ma trận nhánh

| AC | Nhánh | Test case | Review | Thực thi |
| :--- | :--- | :--- | :--- | :--- |
| F4 AC-49 | Tab và thứ tự job | TC-F4-AC49-01 | Đã review | Chưa thực hiện |
| F4 AC-49 | Job lỗi quá 24 giờ | TC-F4-AC49-02 | Đã review | Chưa thực hiện |
| F4 AC-49 | Kích thước trang mới có hiệu lực | TC-F4-AC49-03 | Đã review | Chưa thực hiện |
| F4 AC-50 | Pause, job mới chờ, resume và sweep | TC-F4-AC50-01 | Đã review | Chưa thực hiện |
| F4 AC-50 | Hai admin dùng expected state cũ | TC-F4-AC50-02 | Đã review | Chưa thực hiện |
| F4 AC-50 | Gửi lại giá trị hiện tại không tạo audit | TC-F4-AC50-03 | Đã review | Chưa thực hiện |
| F4 AC-50 | Audit lỗi phải rollback | TC-F4-AC50-04 | Đã review | Chưa thực hiện |
| F4 AC-50 | Giảng viên gọi trực tiếp bị chặn | TC-F4-AC50-05 | Đã review | Chưa thực hiện |

## Test case

### TC-F4-AC49-01 — Ba tab lọc đúng và job lỗi xếp cuối

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-49; DD F4 API §17 T-QJ-01/04, BR-JD-15 |
| Kind / priority | Normal, P1: thứ tự và bộ đếm tab có cùng nguồn |
| Environment | API quản trị F4 + PostgreSQL, clock `T0` |
| Preconditions | ADMIN-A có `JUDGE_QUEUE_MONITOR:READ`; không có job ngoài fixture |
| Test data | J1 `PENDING` nộp `T0-30s`; J2 `PENDING` nộp `T0-10s`; J3 `COMPILING` bắt đầu `T0-20s`; J4 `JUDGING` bắt đầu `T0-5s`; J5 `SYSTEM_ERROR` kết thúc `T0-2h` |
| Steps | GET `/admin/queue/jobs?tab=waiting`, `tab=failed`, `tab=all` với `page=1`; đọc items và counts. |
| Expected | `waiting`: J1,J2; `failed`: chỉ J5; `all`: J1,J3,J2,J4,J5 theo khoá thời gian, J5 luôn cuối. Mỗi response có `counts.waiting=2`, `failed=1`, `all=5`, `pageSize=20`; `waitingMillis` của J1=30000, J2=10000, J3=20000, J4=5000, J5=null. Không có trường priority hoặc mã job ngoài `submissionId`. |
| Cleanup | Dọn năm submission fixture |
| Evidence | DB timestamp/status, ba request/response |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC49-02 — Job lỗi hết hạn hiển thị không bị xoá submission

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-49; DD F4 API §17 T-QJ-02, BR-JD-15 |
| Kind / priority | Boundary, P1: giữ lịch sử học viên dù hết cửa sổ quản trị |
| Environment | API F4 + PostgreSQL, clock `T0` |
| Preconditions | `error_retention_hours=24`; ADMIN-A đọc queue; `A1-JUDGE` sở hữu hai bài nộp lỗi fixture |
| Test data | J6 `SYSTEM_ERROR`, `finished_at=T0-23h`; J7 `SYSTEM_ERROR`, `finished_at=T0-25h` |
| Steps | GET `tab=failed` và `tab=all`; đọc DB `submissions` J6/J7 và lịch sử của học viên. |
| Expected | Chỉ J6 hiện trong hai tab, `counts.failed=1`, `counts.all=1`; J7 vẫn tồn tại trong `submissions` và vẫn có trong lịch sử bài nộp của chủ sở hữu, không bị dọn vì quy tắc danh sách queue. |
| Cleanup | Dọn fixture cô lập |
| Evidence | Hai response queue, DB J6/J7 và response lịch sử |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC49-03 — Kích thước trang mới áp dụng ở lần đọc kế tiếp

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-49; DD F4 API §17 T-QJ-03, `cluster_settings.jobs_per_page` |
| Kind / priority | Boundary, P2: phân trang không cắt mất job |
| Environment | API quản trị F4 + PostgreSQL, clock `T0` |
| Preconditions | Có 6 job `PENDING` J1..J6 với `submitted_at=T0-60s` đến `T0-10s`, khác nhau 10s; `jobs_per_page=20` |
| Test data | PUT `/admin/queue/settings` bằng ADMIN-A với `{autoscaleJobThreshold:20,jobsPerPage:5,errorRetentionHours:24,latencyCapSeconds:10}`; sau đó GET trang 1 và 2 của tab `waiting` |
| Steps | GET trước khi sửa; lưu cấu hình; GET `page=1` và `page=2`, đọc `totalCount`, `counts`, ID. |
| Expected | Trước sửa có 6 job trong trang 1 (`pageSize=20`). Sau sửa trang 1 có J1..J5, trang 2 có J6, cả hai có `pageSize=5`, `totalCount=6`, `counts.waiting=6`; không lặp hoặc mất job. |
| Cleanup | Khôi phục `jobs_per_page`, dọn fixture |
| Evidence | Response trước/sau lưu cấu hình, hai trang và DB timestamps |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC50-01 — Pause giữ job mới chờ và resume làm mới đồng hồ sweep

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-50; DD F4 API §19 T-QC-01/02/05, BR-JD-08, JOB-JD-01 |
| Kind / priority | Recovery/Integration, P0: pause không làm mất bài nộp |
| Environment | API F4 + DB + RabbitMQ + worker + sweep, clock điều khiển được |
| Preconditions | `queue_paused=false`; J0 đang `JUDGING`; ADMIN-A có quyền; một học viên có bài hợp lệ mới |
| Test data | PUT `{paused:true,expectedPaused:false}` tại T0; sau đó học viên nộp J1 trong lúc pause; giữ pause hơn 5 phút; PUT `{paused:false,expectedPaused:true}` tại T1 |
| Steps | Pause; đợi cổng tiêu thụ dừng, nộp J1; chạy một lượt sweep sau khi J1 chờ hơn 5 phút; resume, đọc `updated_at` J1 và audit, bật worker cho J1 kết thúc. |
| Expected | Pause trả 200 `paused=true`, J0 chạy hết; J1 được nhận `PENDING` và sweep không đổi thành `SYSTEM_ERROR` khi đang pause. Resume trả 200 `paused=false`; trong cùng giao dịch mọi PENDING có `updated_at` mới gần T1, nên lượt sweep trong 5 phút tiếp theo không đánh lỗi J1. Worker nhận J1 sau resume. Có đúng hai audit `QUEUE_PAUSE_CHANGED`: false→true và true→false, gắn ADMIN-A. |
| Cleanup | Resume hàng đợi kể cả khi ca lỗi; dọn J0/J1 và audit fixture |
| Evidence | Hai request/response, DB status/timestamp, sweep log, worker call, hai audit |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC50-02 — Hai admin dùng trạng thái cũ: người sau bị từ chối

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-50; DD F4 API §19 T-QC-03, BR-JD-08 |
| Kind / priority | Concurrency, P1: không ghi đè cấu hình từ màn cũ |
| Environment | API F4 + DB + AuditLogPort |
| Preconditions | `queue_paused=false`; ADMIN-A và ADMIN-B đều đã đọc giá trị false |
| Test data | A PUT `{paused:true,expectedPaused:false}`; B PUT cùng body sau khi A commit |
| Steps | Gọi A rồi B; đọc response, DB cluster và audit. |
| Expected | A nhận 200, cờ true và một audit false→true; B nhận `409 JDG-201`, cờ vẫn true, không có audit thứ hai. |
| Cleanup | Khôi phục cờ và dọn audit fixture |
| Evidence | Hai request/response, DB và audit |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC50-03 — Gửi lại giá trị đang có là no-op

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-50; DD F4 API §19 T-QC-05 |
| Kind / priority | Retry, P1: không tạo audit giả khi đặt lại trạng thái hiện tại |
| Environment | API F4 + DB + AuditLogPort |
| Preconditions | `queue_paused=false`, ghi `updated_at` và số audit ban đầu |
| Test data | PUT `{paused:false,expectedPaused:false}` hai lần bằng ADMIN-A |
| Steps | Gọi hai lần, đọc response, cluster_settings và audit sau mỗi lần. |
| Expected | Cả hai trả 200 `paused=false`; cờ và `updated_at` không đổi, không thêm `QUEUE_PAUSE_CHANGED`, worker không bị dừng/mở lại. |
| Cleanup | Dọn fixture nếu có |
| Evidence | Hai response, DB/audit trước sau, worker state |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC50-04 — AuditLogPort lỗi khiến pause rollback

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-50; DD F4 API §19 T-QC-06, BR-JD-08 |
| Kind / priority | Recovery, P0: cấu hình và audit phải commit cùng nhau |
| Environment | API F4 + DB; fault injection AuditLogPort |
| Preconditions | `queue_paused=false`, worker đang nhận job; audit ghi được trước khi bật lỗi |
| Test data | PUT `{paused:true,expectedPaused:false}` bằng ADMIN-A; AuditLogPort ném lỗi đúng lúc ghi |
| Steps | Bật fault, gọi PUT, tắt fault; đọc cờ, `updated_at`, audit và trạng thái consumer. |
| Expected | Request thất bại theo lỗi hệ thống, không báo 200; giao dịch rollback nên cờ vẫn false, `updated_at` và audit không đổi, consumer tiếp tục nhận job. Không có trạng thái pause một phần. |
| Cleanup | Tắt fault injection, dọn fixture |
| Evidence | Response lỗi, DB/audit trước sau, worker state và log đã che dữ liệu |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC50-05 — INSTRUCTOR gọi trực tiếp không được đổi trạng thái hàng đợi

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-50; DD F4 API §19 T-QC-04 |
| Kind / priority | Security, P1: kiểm quyền ở máy chủ |
| Environment | API F4 + DB + AuditLogPort |
| Preconditions | `queue_paused=false`, `A2-OTHER` có JWT INSTRUCTOR nhưng không phải ADMIN |
| Test data | PUT `{paused:true,expectedPaused:false}` bằng JWT A2-OTHER |
| Steps | Gọi PUT; đọc response, cờ queue và audit. |
| Expected | `403 JDG-301`; queue vẫn chạy, không cập nhật cluster_settings hoặc ghi audit. |
| Cleanup | Dọn tài khoản fixture |
| Evidence | Request/response đã che JWT, DB/audit trước sau |
| Design review | Đã review; chưa thực thi |
