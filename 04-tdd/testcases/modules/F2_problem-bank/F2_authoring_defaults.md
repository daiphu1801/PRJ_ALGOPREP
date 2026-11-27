# Test case QA — F2: mặc định soạn bài

> 13 ca thiết kế cho [TDD F2 AC-95](../../../modules/F2_problem-bank.md), DD F2 [§64/#57](../../../../03-dd/api/F2_problem-bank.md#64-đọc-mặc-định-soạn-bài--endpoint-57), §35–37, DEC-2026-1110-f2-authoring-defaults (2026-11-10). Review thiết kế 2026-11-11. **Thực thi: Chưa thực hiện**; log unit/flow/browser liên quan không tự trở thành run record cho 13 ca QA này.

## Fixture dùng chung

Dùng database/Redis namespace và tài khoản riêng của QA; không đổi cấu hình demo dùng chung. Ghi commit/build và ánh xạ UUID thực tế trước chạy. T0 = 2026-11-11T03:00:00Z. JWT có audience/issuer/chữ ký hợp lệ của stack; không dùng token giả chỉ chứa claim quyền.

| Mã | Giá trị cố định |
| :--- | :--- |
| U-C / R-C | User INSTRUCTOR ACTIVE, role tùy chỉnh chỉ có PROBLEM_AUTHORING:CREATE; không READ/UPDATE hoặc AI_CONFIG |
| U-R / R-R | User INSTRUCTOR ACTIVE, role tùy chỉnh chỉ có PROBLEM_AUTHORING:READ; không CREATE/UPDATE hoặc AI_CONFIG |
| U-N / R-N | User INSTRUCTOR ACTIVE, role không có cả CREATE lẫn READ; không dựa vào STUDENT-only để chứng minh phép OR |
| U-A | Instructor ACTIVE đủ quyền đọc/tạo/cập nhật bài của mình, không có AI_CONFIG; dùng ca UI |
| D1 | F4 grading defaults timeLimitMs=1700, memoryLimitMb=384, maxOutputSizeKb=96; F2 maxAiGenerationsPerProblem=3 |
| D2 | F4 grading defaults 1800/448/112; F2 maxAiGenerationsPerProblem=4 |
| Bí mật/cấu hình ngoài scope | compileTimeoutSeconds=19; queue/worker/provider fields và marker QA_DEFAULTS_SECRET chỉ ở cấu hình/admin test fixture, không được đưa vào response #57 |
| X | UUID 95000000-0000-4000-8000-000000000001, bài UNPUBLISHED của U-A, limits=2100/512/128, maxSubmissionsPerHour=17, AI used=2; nội dung nguồn “QA defaults source”, đủ dữ liệu cho duplicate-source |
| N | Bài mới chưa có ID; không có dòng problem trước khi bấm Lưu |
| Q0 | Chụp count problem.problems, outbox_events và audit trước mỗi GET; bỏ audit của bước thiết lập cấu hình khỏi cửa sổ so sánh |

Thiết lập quyền qua identity.roles/permissions/functions/actions đúng hợp đồng F1; xác minh effective authority qua server. Mỗi ca bắt đầu từ fixture gốc, không thừa hưởng D2 hoặc token từ ca trước. Lỗi nguồn chỉ tiêm vào cổng cấu hình được chọn; không dừng toàn bộ PostgreSQL vì điều đó sẽ làm xác thực lỗi trước khi tới defaults.

## Nhánh và truy vết

| AC | Nhánh | Ca | Review |
| :--- | :--- | :--- | :--- |
| AC-95 | Giá trị live, payload an toàn, không mutation | TC-F2-AC95-01 | Reviewed |
| AC-95 | CREATE-only / READ-only độc lập | TC-F2-AC95-02, 03 | Reviewed |
| AC-95 | Thiếu cả hai quyền, anonymous, hết JWT | TC-F2-AC95-04, 05, 06 | Reviewed |
| AC-95 | F4 lỗi / F2 lỗi, 503 cố định và phục hồi | TC-F2-AC95-07, 08 | Reviewed |
| AC-95 | Form mới / nhân bản / bài tồn tại | TC-F2-AC95-09, 10, 11 | Reviewed |
| AC-95 | Form mới / nhân bản không fallback khi lỗi | TC-F2-AC95-12, 13 | Reviewed |

Đọc nhiều nguồn cấu hình không có hợp đồng snapshot nguyên tử giữa F2/F4; kiểm giá trị khi cấu hình đứng yên, không tự thêm oracle cho race cấu hình.

### TC-F2-AC95-01 — Đọc đúng bốn giá trị cấu hình hiện hành và không có tác dụng ghi

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-95; DD F2 §64, DEC-2026-1110-f2-authoring-defaults |
| Kind / priority | Normal + security; P0: tránh lộ cấu hình quản trị hoặc áp limit sai |
| Environment | API tích hợp F1/F2/F4 và PostgreSQL thật; chưa cần LLM/go-judge |
| Preconditions | U-A không có AI_CONFIG; nguồn D1 ổn định, đã lưu Q0 |
| Test data | GET /api/v1/problem-bank/authoring/defaults bằng JWT U-A; D1 |
| Steps | Gọi GET; đối chiếu JSON và Q0 |
| Expected | 200; tập key chính xác timeLimitMs,memoryLimitMb,maxOutputSizeKb,maxAiGenerationsPerProblem và values 1700,384,96,3. Không nested config, compileTimeoutSeconds, queue/worker/provider hoặc marker bí mật; count bài/audit/outbox không đổi. Không cần quyền AI_CONFIG để đọc bốn scalar này. |
| Cleanup | Khôi phục D1 và xóa fixture QA bằng quy trình riêng, không xóa lịch sử chung |
| Evidence | Request/response đã che JWT, snapshot D1, schema key scan, Q0 trước/sau |
| Design review | Reviewed; thực thi Chưa thực hiện |

### TC-F2-AC95-02 — Chỉ CREATE vẫn đọc được defaults

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-95; DD F2 §64 phép OR CREATE/READ; BR-02 |
| Kind / priority | Security; P0: không vô tình yêu cầu cả hai authority |
| Environment | HTTP với JWT và F1 permission resolver thật |
| Preconditions | U-C ACTIVE; chỉ grant PROBLEM_AUTHORING:CREATE, xác nhận READ không granted; D1 |
| Test data | JWT U-C; GET /api/v1/problem-bank/authoring/defaults |
| Steps | Gọi GET bằng token U-C; lưu permission matrix cùng response |
| Expected | 200 và đúng bốn scalar D1. Không cần READ hoặc AI_CONFIG; không ghi DB/audit/outbox. Không chấp nhận chỉ đổi claim JWT mà bỏ qua quyền server. |
| Cleanup | Dọn session/user/role riêng theo quy trình fixture, khôi phục permission |
| Evidence | Matrix effective permissions, status/body và Q0 |
| Design review | Reviewed; thực thi Chưa thực hiện |

### TC-F2-AC95-03 — Chỉ READ vẫn đọc được defaults

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-95; DD F2 §64 phép OR CREATE/READ; BR-02 |
| Kind / priority | Security; P0: quyền đọc không phụ thuộc quyền tạo |
| Environment | HTTP với JWT và F1 permission resolver thật |
| Preconditions | U-R ACTIVE; chỉ grant PROBLEM_AUTHORING:READ, CREATE không granted; D1 |
| Test data | JWT U-R; cùng route #57 |
| Steps | Gọi GET bằng U-R, đối chiếu permission matrix |
| Expected | 200 với đúng D1/bốn key; không yêu cầu CREATE/AI_CONFIG, không phát sinh bài hoặc audit/outbox |
| Cleanup | Dọn fixture role/session riêng |
| Evidence | Ma trận quyền, request/response đã che JWT, Q0 |
| Design review | Reviewed; thực thi Chưa thực hiện |

### TC-F2-AC95-04 — Tài khoản thiếu cả hai authority nhận PRB-002

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-95; DD F2 §64, BR-02 |
| Kind / priority | Security; P0: chặn truy cập không có quyền |
| Environment | HTTP và identity permissions thật |
| Preconditions | U-N ACTIVE/INSTRUCTOR, JWT hợp lệ; không CREATE và không READ; D1 |
| Test data | GET #57 bằng U-N |
| Steps | Gọi GET, đối chiếu status/errorCode và schema |
| Expected | 403 PRB-002 theo RFC9457; không trả bốn defaults hoặc config quản trị, không ghi bài/audit/outbox. Chặn ở server dù base role INSTRUCTOR. |
| Cleanup | Dọn fixture riêng |
| Evidence | Effective permissions, response403 và Q0 |
| Design review | Reviewed; thực thi Chưa thực hiện |

### TC-F2-AC95-05 — Anonymous không đọc được defaults

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-95; DD F2 §1.2 và §64 |
| Kind / priority | Security; P0: endpoint không công khai |
| Environment | HTTP trực tiếp qua proxy và security thật |
| Preconditions | D1; client không có Authorization/cookie phiên |
| Test data | GET #57 không gửi JWT |
| Steps | Gọi GET từ request context sạch |
| Expected | 401 theo hợp đồng security chung; không trả payload defaults/config, không mutation Q0. Không yêu cầu mã PRB-002 của nhánh403. |
| Cleanup | Không có session cần dọn |
| Evidence | Request headers đã chứng minh thiếu token, status/body401, Q0 |
| Design review | Reviewed; thực thi Chưa thực hiện |

### TC-F2-AC95-06 — JWT hết hạn không được dùng quyền cũ

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-95; DD F2 §1.2 và §64 |
| Kind / priority | Boundary + security; P0: kiểm thời hạn xác thực |
| Environment | HTTP, clock kiểm soát và JWT signer của test stack |
| Preconditions | Token U-C ký hợp lệ với exp=T0-1s; đồng hồ server=T0; không refresh cookie |
| Test data | GET #57 với JWT hết hạn |
| Steps | Gửi một GET, không để client refresh tự động |
| Expected | 401 theo security chung; không có defaults, không gọi thành công chỉ vì token từng có CREATE, Q0 không đổi |
| Cleanup | Khôi phục clock; thu hồi session test |
| Evidence | exp/clock đã che token, response401 và Q0 |
| Design review | Reviewed; thực thi Chưa thực hiện |

### TC-F2-AC95-07 — Nguồn mặc định F4 lỗi trả 503 an toàn và đọc lại sau phục hồi

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-95; DD F2 §64; JudgeConfigReadPort |
| Kind / priority | Recovery; P0: không dùng limit hằng khi nguồn chết |
| Environment | API integration có fault injection vào JudgeConfigReadPort, F2/auth/DB hoạt động thật |
| Preconditions | U-A hợp lệ, F2 cap=3; fault F4 ném exception có marker QA_DEFAULTS_SECRET và chuỗi SQL nội bộ |
| Test data | GET #57 khi fault bật; sau đó bỏ fault, nguồn đứng yên D2 |
| Steps | Gọi GET khi F4 lỗi; bỏ fault, cấu hình D2 commit xong rồi gọi lại |
| Expected | Lần lỗi503 PRB-050, detail chính xác Authoring defaults unavailable, không marker/cause/stack/SQL và không payload mặc định1000/256/64. Lần phục hồi200 với D2, không cache lỗi/thay giá trị bằng D1. Q0 của từng GET không đổi. |
| Cleanup | Bỏ fault trong finally và khôi phục D1 |
| Evidence | Fault ID, hai response, snapshot D2 sau commit và Q0; không lưu nguyên secret |
| Design review | Reviewed; thực thi Chưa thực hiện |

### TC-F2-AC95-08 — Nguồn AI cap F2 lỗi không trả response thành công một phần

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-95; DD F2 §64; ProblemAuthoringReadRepository.aiGenerationLimit |
| Kind / priority | Abnormal + security; P0: không bịa quota0 hoặc che lỗi |
| Environment | API integration fault injection chỉ vào đọc aiGenerationLimit; F4/auth còn khỏe |
| Preconditions | U-A, F4 trả1700/384/96; F2 cap reader ném exception chứa QA_DEFAULTS_SECRET |
| Test data | GET #57 khi cap reader lỗi |
| Steps | Gọi GET, quét error body và chụp Q0 |
| Expected | 503 PRB-050 với detail cố định Authoring defaults unavailable; không JSON200 chỉ ba trường, không maxAiGenerationsPerProblem=0/2 giả, không cause/SQL/secret. Không sửa settings, bài, audit/outbox. |
| Cleanup | Bỏ fault trong finally, khôi phục D1 |
| Evidence | Response503, fault trace chỉ loại lỗi, Q0/config trước/sau |
| Design review | Reviewed; thực thi Chưa thực hiện |

### TC-F2-AC95-09 — Form mới phản ánh cấu hình mới trước lần lưu đầu

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-95; DD F2 §64 và §36; SHR0202 tạo mới |
| Kind / priority | Normal; P1: không dùng hard-coded defaults trên FE |
| Environment | E2E mock=false, FE và F1/F2/F4/DB thật |
| Preconditions | U-A; D1 đã commit, N chưa có ID; mở một form mới, sau đó bỏ form và đổi D2 trước khi mở form mới khác |
| Test data | Routes /instructor/problems/new; D1 rồi D2; ai used khởi tạo0 |
| Steps | Mở form N với D1, ghi ba ô limits và cap hiển thị; bỏ form không lưu; commit D2 rồi mở form mới trong phiên browser sạch để tránh dữ liệu form cũ |
| Expected | GET #57 thành công trước khởi tạo; N lần đầu1700/384/96 và AI0/3, form mới sau đổi1800/448/112 và AI0/4. Không tạo dòng bài/audit/outbox chỉ vì mở form; không hiện AI0/0 hoặc1000/256/64. |
| Cleanup | Rời form chưa lưu và khôi phục D1 |
| Evidence | Hai response #57, hai ảnh/giá trị ô, network và DB count trước/sau |
| Design review | Reviewed; thực thi Chưa thực hiện |

### TC-F2-AC95-10 — Nhân bản giữ limits nguồn và lấy AI cap hiện tại

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-95; DD F2 §35 duplicate-source, §36 và §64 |
| Kind / priority | Normal; P1: không ghi đè lựa chọn bài nguồn hoặc kế thừa lượt AI đã dùng |
| Environment | E2E mock=false, source X thuộc U-A, DB/ports thật |
| Preconditions | D2; X limits2100/512/128, hourly17 và AI used2; chưa có bản sao |
| Test data | Mở thao tác nhân bản X bằng UI, lấy duplicate-source #34 và defaults #57 |
| Steps | Mở form bản sao trước khi Lưu; kiểm limits, quota và DB nguồn |
| Expected | Ba limits/hạn mức giờ giữ2100/512/128/17 từ nguồn theo §35; AI quota form mới used0/limit4 lấy cap hiện tại. Không dùng limits D2 để ghi đè nguồn; không kế thừa used2. Chưa tạo bản sao hoặc thay X khi chỉ mở form. |
| Cleanup | Bỏ bản sao chưa lưu, khôi phục D1 |
| Evidence | Responses #34/#57, UI limits/quota, snapshot X và count bài |
| Design review | Reviewed; thực thi Chưa thực hiện |

### TC-F2-AC95-11 — Bài hiện hành giữ limits riêng và không phụ thuộc defaults

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-95; DD F2 §33/§37 và §64 |
| Kind / priority | Normal + degraded; P1: đổi cấu hình không sửa bài đã có |
| Environment | E2E mock=false, source X và authoring reader thật |
| Preconditions | X limits2100/512/128/hourly17, AI used2; D2; endpoint defaults được tiêm lỗi nếu có caller nhưng reader bài X vẫn khỏe |
| Test data | U-A mở /instructor/problems/{X}/edit |
| Steps | Mở X, kiểm ô limits và network #57; không bấm Lưu |
| Expected | X vẫn2100/512/128/17, AI used không reset; không gọi #57 khi đọc bài đã tồn tại. Lỗi defaults không làm màn X thất bại và không ghi đè limit riêng. Không có PUT/POST hoặc DB mutation chỉ vì mở bài. |
| Cleanup | Bỏ fault và khôi phục D1 |
| Evidence | Network chứng minh0 request #57, response authoring, UI và DB X trước/sau |
| Design review | Reviewed; thực thi Chưa thực hiện |

### TC-F2-AC95-12 — Form mới không fallback hằng khi defaults503

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-95; DD F2 §64 và §36 |
| Kind / priority | Abnormal; P0: không cho lưu cấu hình bịa khi nguồn lỗi |
| Environment | E2E mock=false; fault thật từ nguồn F4 của #57, F1/auth/levels còn khỏe |
| Preconditions | U-A; N chưa có ID; fault từ TC-F2-AC95-07 bật |
| Test data | Mở /instructor/problems/new |
| Steps | Mở form, chờ request #57 kết thúc503; kiểm lỗi/retry và network lưu |
| Expected | UI biểu thị không tải được dữ liệu/cho thử lại theo mẫu lỗi hiện có; không dựng form có thể lưu với1000/256/64/cap0 hoặc giá trị cũ. Không có POST tạo bài, quota/settings/audit/outbox không đổi. Sau bỏ fault và retry thành công mới hiện D1. |
| Cleanup | Bỏ fault trong finally, bỏ form không lưu |
| Evidence | Response503 và request retry200, UI lỗi/khôi phục, network không POST và Q0 |
| Design review | Reviewed; thực thi Chưa thực hiện |

### TC-F2-AC95-13 — Form nhân bản không bypass defaults503 dù nguồn có limits

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-95; DD F2 §35/§36 và §64 |
| Kind / priority | Abnormal; P1: không bịa AI cap cho bản sao |
| Environment | E2E mock=false; #34 đọc X được, nguồn cap F2 của #57 fault thật |
| Preconditions | U-A có quyền nguồn X; D1; fault TC-F2-AC95-08 bật; chưa có bản sao |
| Test data | Mở thao tác nhân bản X bằng UI |
| Steps | Mở bản sao, chờ #57 lỗi503; không bấm xác nhận lưu; quét UI/network và DB |
| Expected | Hiển thị lỗi tải dữ liệu/khả năng retry theo mẫu hiện hành; không dùng cap0/2 hoặc bỏ qua lỗi chỉ vì nguồn có limits. Không tự tạo bản sao, không reset used của X, không thay nội dung nguồn. Bỏ fault/retry mới dùng source limits với cap hiện tại như ca10. |
| Cleanup | Bỏ fault trong finally và rời form không lưu |
| Evidence | Response503/retry200, UI, network không POST, snapshot X/count bài |
| Design review | Reviewed; thực thi Chưa thực hiện |

## Review hai chiều và giới hạn thực thi

Tất cả13 ca dẫn AC-95/DD hiện hành; các nhánh quyền OR,401/403,hai nguồn503,payload an toàn/live defaults và ba chế độ form đều có ca độc lập. Không có oracle còn chờ quyết định cho cụm này. Limits bản sao theo nguồn §35, cap theo §64; không tự suy rằng nhân bản phải ghi đè limits nguồn bằng defaults hệ thống. Không thêm concurrency/write-validation vì #57 là read-only và không có hợp đồng snapshot cấu hình xuyên module.

Trạng thái13 ca vẫn **Chưa thực hiện**. Các test source ReadAuthoringDefaultsQueryHandlerTest và F2CloseoutFlowTest (gồm quyền CREATE-only/READ-only) là nguồn thực thi tự động liên quan; chỉ lập run record QA sau khi đối chiếu đủ fixture/oracle và có log cuối, không nâng Đạt từ thiết kế này.

