# DD API — Danh mục endpoint toàn hệ thống (index)

> **Tài liệu này chỉ là mục lục.** Hợp đồng chi tiết của từng endpoint (request, response, lỗi, luồng xử lý) nằm đúng một nơi: `03-dd/api/<module>.md`. File này không lặp lại; nó chỉ liệt kê, đặt quy ước chung và đăng ký tiền tố mã lỗi (`CLAUDE.md`, quy tắc anti-drift).
> Tạo 2026-10-23, khi module thứ hai có DD (đúng điều kiện ghi ở `03-dd/api/F1_identity.md` mục 0). Nguồn quy ước: `03-dd/api/F1_identity.md` đầu file, `03-dd/api/F2_problem-bank.md` mục 1, `03-dd/api/F4_judge-orchestration.md` mục 1.
> Phạm vi hiện tại: sáu module đã có DD (`identity`, `problem-bank`, `harness`, `judge-orchestration`, `ai-review`, `interview-bank`; `ai-review` và `interview-bank` thêm 2026-10-23).

## 1. Quy ước chung

Bảng 1: Quy ước áp dụng cho mọi endpoint.

| Mục | Quy ước |
| :--- | :--- |
| Base path | `/api/v1/<module>`, với `<module>` là `identity`, `problem-bank`, `harness`, `judge` (module `judge-orchestration`), `ai` (module `ai-review`), `interview-bank` |
| Định dạng | JSON; xác thực `Authorization: Bearer <access_token>`, trừ endpoint đánh dấu công khai |
| HTTP status | `400` lỗi kiểm dữ liệu; `401` chưa xác thực hoặc hết hạn; `403` không đủ quyền; `404` không thấy; `409` xung đột trạng thái; `422` vi phạm quy tắc nghiệp vụ; `429` quá giới hạn tần suất |
| Quyền | Dạng `FUNCTION:ACTION` theo seed 11 function (`02-bd/database/F1_identity.md` mục 1.3); thao tác quản trị danh mục kiểm thêm vai trò `ADMIN` ở use case |
| Nhật ký quản trị | Ghi qua cổng `AuditLogPort` của `identity`, cùng giao dịch (`02-bd/architecture/F1_identity.md` mục 3.3) |
| Sự kiện miền | Phong bì `{eventId, eventType, occurredAt, schemaVersion, payload}` của `03-dd/api/F1_identity.md` mục 2 |

### 1.1. Phong bì lỗi

Mọi lỗi trả về dạng `{ "error": { "code": "<tiền tố>-<số>", "message": "<chuỗi dự phòng>", "details": { ... }, "traceId": "<uuid>" } }`. `code` bắt buộc và là giá trị enum thật, `message` là chuỗi dự phòng, `details` tuỳ mã lỗi, `traceId` do máy chủ sinh để tra log. Backend trả `code`; giao diện dịch qua `messages/{locale}.json`, không dựa vào `message`. Phong bì này hợp nhất hai cách viết của `problem-bank` và `judge-orchestration` `[SoT: Suy luận]` (identity chỉ nêu `error.code`); ba module đang dùng chung.

### 1.2. Tiền tố mã lỗi

Bảng 2: Tiền tố mã lỗi theo module. Mã kiểm dữ liệu dùng dạng `<tiền tố>-Vnn`, cảnh báo không chặn dùng `<tiền tố>-Wnn`.

| Tiền tố | Module | File hợp đồng |
| :--- | :--- | :--- |
| `IDT` | `identity` | `03-dd/api/F1_identity.md` |
| `PRB` | `problem-bank` | `03-dd/api/F2_problem-bank.md` mục 60 (bảng mã lỗi `PRB`) |
| `HRN` | `harness` | `03-dd/api/F3_harness.md`; mã kiểm đặc tả `HRN-Vnn` ở `03-dd/validation/F3_harness.md` |
| `JDG` | `judge-orchestration` | `03-dd/api/F4_judge-orchestration.md` mục 27 |
| `AIR` | `ai-review` | `03-dd/api/F5_ai-review.md` mục 34 (bảng mã lỗi `AIR`); mã kiểm đầu vào `AIR-Vnn` ở `03-dd/validation/F5_ai-review.md` |
| `INB` | `interview-bank` | `03-dd/api/F6_interview-bank.md` mục 27 (Bảng 7, mã lỗi `INB`); mã kiểm đầu vào `INB-Vnn` ở `03-dd/validation/F6_interview-bank.md` |

## 2. Danh mục endpoint

### 2.1. `identity` (75 endpoint)

Bảng 3: Endpoint của `identity`. Cột # là số thứ tự trong `03-dd/api/F1_identity.md` mục 0; đường dẫn là đường dẫn đầy đủ.

| # | Method | Path | Mô tả |
| :--- | :--- | :--- | :--- |
| 1 | POST | `/api/v1/identity/auth/register` | Đăng ký email + mật khẩu (F1-01) |
| 2 | POST | `/api/v1/identity/auth/login` | Đăng nhập, trả access token + set cookie refresh token (F1-02) |
| 3 | POST | `/api/v1/identity/auth/refresh` | Xoay vòng refresh token, cấp access token mới (F1-03) |
| 4 | POST | `/api/v1/identity/auth/logout` | Thu hồi refresh token hiện tại, xoá cookie (F1-04) |
| 5 | GET | `/api/v1/identity/auth/oauth/{provider}/redirect` | Bắt đầu luồng OAuth (`provider` = `github`\ |
| 6 | GET | `/api/v1/identity/auth/oauth/{provider}/callback` | Nhận callback OAuth, đối chiếu `state`, tự liên kết hoặc tạo tài khoản (F1-15) |
| 7 | POST | `/api/v1/identity/auth/password/forgot` | Gửi OTP quên mật khẩu (F1-17) |
| 8 | POST | `/api/v1/identity/auth/password/reset` | Xác nhận OTP + đặt mật khẩu mới (F1-17) |
| 9 | GET | `/api/v1/identity/me/profile` | Tải hồ sơ, trạng thái mật khẩu, danh sách provider đã liên kết |
| 10 | PATCH | `/api/v1/identity/me/profile` | Cập nhật hồ sơ, **trừ** `email` |
| 11 | POST | `/api/v1/identity/me/email-change/request` | Yêu cầu đổi email, gửi OTP tới email mới |
| 12 | POST | `/api/v1/identity/me/email-change/confirm` | Xác nhận OTP, đổi `email` |
| 13 | POST | `/api/v1/identity/me/password` | Đổi mật khẩu (đã có) hoặc đặt mật khẩu lần đầu (tài khoản chỉ OAuth) |
| 14 | GET | `/api/v1/identity/me/settings` | Đọc `user_preferences` (không gồm chủ đề màu/ngôn ngữ giao diện — lưu thiết bị) |
| 15 | PATCH | `/api/v1/identity/me/settings` | Cập nhật `streak_reminder_enabled`, `weekly_report_enabled`, `preferred_interview_level`, `pref |
| 16 | GET | `/api/v1/identity/me/export?scope={profile\|submissions}` | Xuất CSV/JSON dữ liệu cá nhân (F1-22) |
| 17 | GET | `/api/v1/identity/me/export/interview-transcripts` | Xuất hội thoại phỏng vấn (dữ liệu ở `ai-review`, `identity` đọc qua cổng ra) |
| 18 | POST | `/api/v1/identity/me/account/delete` | Tự xoá tài khoản — yêu cầu gõ lại email của chính mình để xác nhận (F1-16) |
| 19 | GET | `/api/v1/identity/classes/my` | Danh sách lớp của giảng viên đang đăng nhập |
| 20 | POST | `/api/v1/identity/classes` | Tạo lớp (F1-23) |
| 21 | PATCH | `/api/v1/identity/classes/{classId}` | Sửa tên/mô tả/lịch học (F1-23); `schedule` là jsonb có cấu trúc từ 2026-10-11, `DEC-2026-1011-c |
| 22 | DELETE | `/api/v1/identity/classes/{classId}` | Xoá lớp — hard delete (F1-24) |
| 23 | POST | `/api/v1/identity/classes/{classId}/invite-codes` | Sinh mã mời mới, TTL 7 ngày (F1-25) |
| 24 | GET | `/api/v1/identity/classes/{classId}/invite-codes` | Liệt kê mã mời còn hiệu lực của lớp |
| 25 | POST | `/api/v1/identity/classes/join` | Học viên tham gia lớp bằng mã mời (F1-23) |
| 25A | GET | `/api/v1/identity/me/classes` | `ListMyClasses` — lớp còn ghi danh của học viên |
| 25B | GET | `/api/v1/identity/me/classes/{classId}` | `GetMyClass` — chi tiết lớp còn ghi danh |
| 26 | GET | `/api/v1/identity/classes/{classId}/students` | Danh sách học viên trong lớp, kèm trạng thái phân loại (F1-27) |
| 27 | POST | `/api/v1/identity/classes/{classId}/students/{studentId}/leave` | Gỡ học viên khỏi lớp (F1-26) — ĐÃ ĐỔI 2026-10-14 (DEC-2026-1014-keep-learning-history-on-leave) |
| 28 | GET | `/api/v1/identity/classes/{classId}/students/{studentId}/detail` | Chi tiết một học viên trong lớp (F1-28) |
| 29 | GET | `/api/v1/identity/classes/{classId}/progress` | Dashboard tiến độ lớp: hoàn thành TB, phân loại 5 nhãn (F1-28) |
| 30 | GET | `/api/v1/identity/rbac/matrix` | Đọc toàn bộ ma trận Role × Function × Action |
| 31 | PATCH | `/api/v1/identity/rbac/matrix/{roleId}/{functionId}/{actionId}` | Bật/tắt một ô, có hiệu lực ngay |
| 32 | GET | `/api/v1/identity/rbac/roles` | Liệt kê role (3 hệ thống + tuỳ biến) |
| 33 | POST | `/api/v1/identity/rbac/roles` | Tạo role tuỳ biến, chọn `base_category` (F1-11) |
| 34 | DELETE | `/api/v1/identity/rbac/roles/{roleId}` | Xoá role tuỳ biến (F1-11) |
| 35 | GET | `/api/v1/identity/admin/users?status=&role=&q=&page=&pageSize=` | Danh sách người dùng, lọc + phân trang |
| 36 | PATCH | `/api/v1/identity/admin/users/{userId}/role` | Đổi vai trò (F1-13) |
| 37 | PATCH | `/api/v1/identity/admin/users/{userId}/status` | Khoá/mở khoá tài khoản (F1-13) |
| 38 | POST | `/api/v1/identity/admin/users/{userId}/reset-password` | Đặt lại mật khẩu hộ (F1-13) |
| 39 | GET | `/api/v1/identity/admin/audit-logs` | Tìm/lọc nhật ký bằng cursor |
| 40 | GET | `/api/v1/identity/admin/audit-logs/export` | Xuất CSV streaming, sáu cột tuỳ chọn |
| 41 | GET | `/api/v1/identity/admin/dashboard/summary` | Thẻ người dùng hoạt động 30 ngày |
| 42 | GET | `/api/v1/identity/admin/dashboard/user-growth` | Người dùng mới/quay lại trong sáu tháng |
| 43 | GET | `/api/v1/identity/admin/dashboard/cross-module` | Thẻ tổng lượt nộp |
| 44 | GET | `/api/v1/identity/instructor/dashboard/summary` | Legacy, không còn màn DD tiêu thụ; `INS0101` dùng #56 |
| 45 | GET | `/api/v1/identity/instructor/dashboard/recent-activity?classId=` | Feed "Hoạt động gần đây", lọc theo lớp qua `scope_class_id` |
| 46 | GET | `/api/v1/identity/me/recent-activity` | "Hoạt động gần đây" của cá nhân (10 mục, 90 ngày) |
| 47 | GET | `/api/v1/identity/me/progress/summary` | Best-attempt theo bài, tỉ lệ Accepted, "Đúng ngay lần đầu", ngôn ngữ dùng nhiều nhất |
| 48 | GET | `/api/v1/identity/me/problem-solve-states?problemIds=` | Trạng thái giải theo từng `problem_id` của người gọi (map) |
| 49 | POST | `/api/v1/identity/auth/instructor/login` | Đăng nhập riêng cho giảng viên — cùng contract endpoint #2, tự chặn tài khoản không phải `INSTR |
| 50 | POST | `/api/v1/identity/admin/users` | Admin tạo tài khoản thủ công (F1-13 mở rộng) |
| 51 | GET | `/api/v1/identity/admin/settings/log-retention` | Đọc thời hạn và chính sách log quá hạn |
| 52 | PUT | `/api/v1/identity/admin/settings/log-retention` | Sửa thời hạn và chính sách |
| 53 | POST | `/api/v1/identity/auth/email/verify` | Xác nhận OTP xác thực email của tài khoản **tự đăng ký**, `PENDING_VERIFICATION` chuyển `ACTIVE |
| 54 | POST | `/api/v1/identity/auth/email/verify/resend` | Gửi lại OTP xác thực email, **chỉ tự đăng ký** (F1-33, **đề xuất**; Sửa 2026-10-22 (đồng bộ DB  |
| 55 | GET | `/api/v1/identity/me/activity-calendar` | Lưới hoạt động 12 tháng: số lượt nộp theo từng ngày (UTC+7) |
| 56 | GET | `/api/v1/identity/instructor/classes/summary` | `ListMyClassesSummary` / `GetInstructorClassSummary` |
| 57 | GET | `/api/v1/identity/instructor/classes/score-trend` | `GetClassScoreTrend`, sáu tuần ISO đủ |
| 58 | GET | `/api/v1/identity/instructor/classes/attention-students` | `ListClassAttentionStudents`, tối đa năm học viên |
| 59 | GET | `/api/v1/identity/instructor/students` | `ListInstructorStudents`, phân trang học viên trên các lớp phụ trách |
| 60 | POST | `/api/v1/identity/auth/password/verify-otp` | Kiểm OTP quên mật khẩu trước bước đặt mật khẩu mới, không tiêu thụ mã |
| 61 | POST | `/api/v1/identity/auth/deactivation/restore` | Huỷ tự xoá trong hạn 30 ngày bằng vé sau xác minh đăng nhập |
| 62 | GET | `/api/v1/identity/admin/users/management-stats` | Số tài khoản chờ xác thực email |
| 63 | GET | `/api/v1/identity/admin/users/role-distribution` | Bốn nhóm phân bố vai trò |
| 64 | GET | `/api/v1/identity/admin/audit-logs/active-admins` | Năm quản trị viên hoạt động trong 24 giờ |
| 65 | GET | `/api/v1/identity/admin/audit-logs/category-breakdown` | Bốn nhóm hành động trong bảy ngày |
| 66 | GET | `/api/v1/identity/admin/audit-logs/stream` | SSE nhật ký thời gian thực |
| 67 | GET | `/api/v1/identity/admin/dashboard/submissions/by-language` | Lượt nộp theo ba ngôn ngữ |
| 68 | GET | `/api/v1/identity/admin/dashboard/submissions/verdicts` | Phân bố verdict cuối |
| 69 | GET | `/api/v1/identity/admin/dashboard/submissions/difficulty` | Lượt nộp/Accepted theo độ khó |
| 70 | GET | `/api/v1/identity/admin/dashboard/submissions/by-day` | Lượt nộp bảy ngày |
| 71 | GET | `/api/v1/identity/admin/dashboard/submissions/by-month` | Lượt nộp sáu tháng |
| 72 | GET | `/api/v1/identity/admin/dashboard/top-problems` | Bốn bài phổ biến đang xuất bản |
| 73 | GET | `/api/v1/identity/admin/dashboard/password-resets` | Thẻ yêu cầu đặt lại mật khẩu |

### 2.2. `problem-bank` (58 endpoint)

Bảng 4: Endpoint của `problem-bank`. Cột Mục là số mục trong `03-dd/api/F2_problem-bank.md`; đường dẫn đầy đủ gồm base `/api/v1/problem-bank`.

| Mục | Method | Path | Operation |
| :--- | :--- | :--- | :--- |
| 2 | GET | `/api/v1/problem-bank/problems` | `ListPublishedProblems` |
| 3 | GET | `/api/v1/problem-bank/problems/random` | `GetRandomProblem` |
| 4 | GET | `/api/v1/problem-bank/topics/progress` | `ListTopicsWithProgress` |
| 5 | GET | `/api/v1/problem-bank/me/class-assignments` | `ListMyClassAssignments` |
| 5A | GET | `/api/v1/problem-bank/me/class-problems` | `ListMyClassProblems`, đủ lớp và giữ bài đã rút |
| 6 | GET | `/api/v1/problem-bank/problems/{problemId}/workspace` | `GetProblemForWorkspace` |
| 7 | GET | `/api/v1/problem-bank/problems/{problemId}/overview` | `GetProblemOverview` |
| 8 | GET | `/api/v1/problem-bank/problems/{problemId}/sample-testcases` | `ListSampleTestcases` |
| 9 | GET | `/api/v1/problem-bank/me/bookmarks` | `ListMySavedProblems` |
| 10 | PUT | `/api/v1/problem-bank/me/bookmarks/{problemId}` | `SaveMyBookmark` |
| 11 | DELETE | `/api/v1/problem-bank/me/bookmarks/{problemId}` | `RemoveMyBookmark` |
| 12 | PUT | `/api/v1/problem-bank/me/bookmarks/{problemId}/state` | `ToggleMyProblemBookmark` |
| 13 | GET | `/api/v1/problem-bank/suggested-problems` | `GetSuggestedProblems` |
| 14 | GET | `/api/v1/problem-bank/catalog/summary` | `GetPublishedProblemCatalogSummary` |
| 14A | GET | `/api/v1/problem-bank/me/levels/progress` | `ListMyLevelProgress`, tử số và mẫu số chỉ cho bài đang xuất bản |
| 15 | GET | `/api/v1/problem-bank/levels` | `ListProblemLevels` |
| 16 | POST | `/api/v1/problem-bank/levels` | `CreateProblemLevel` |
| 17 | PATCH | `/api/v1/problem-bank/levels/{levelId}` | `UpdateProblemLevel` |
| 18 | PUT | `/api/v1/problem-bank/levels/order` | `ReorderProblemLevels` |
| 19 | DELETE | `/api/v1/problem-bank/levels/{levelId}` | `DeleteProblemLevel` |
| 20 | GET | `/api/v1/problem-bank/topics` | `ListProblemTopics` |
| 21 | POST | `/api/v1/problem-bank/topics` | `CreateProblemTopic` |
| 22 | PATCH | `/api/v1/problem-bank/topics/{topicId}` | `UpdateProblemTopic` |
| 23 | PUT | `/api/v1/problem-bank/topics/order` | `ReorderProblemTopics` |
| 24 | DELETE | `/api/v1/problem-bank/topics/{topicId}` | `DeleteProblemTopic` |
| 25 | GET | `/api/v1/problem-bank/authoring/problems` | `ListProblemsAdmin` |
| 26 | GET | `/api/v1/problem-bank/authoring/problems/topic-distribution` | `GetTopicDistribution` |
| 27 | GET | `/api/v1/problem-bank/authoring/problems/attention` | `GetAttentionList` |
| 28 | POST | `/api/v1/problem-bank/authoring/problems/export` | `ExportProblemsCsv` |
| 29 | POST | `/api/v1/problem-bank/authoring/problems/bulk/publish` | `PublishProblem` |
| 30 | POST | `/api/v1/problem-bank/authoring/problems/bulk/unpublish` | `UnpublishProblem` |
| 31 | POST | `/api/v1/problem-bank/authoring/problems/bulk/difficulty` | `ChangeDifficulty` |
| 32 | POST | `/api/v1/problem-bank/authoring/problems/bulk/topic` | `AssignTopic` |
| 33 | POST | `/api/v1/problem-bank/authoring/problems/bulk/delete` | `DeleteProblem` |
| 34 | GET | `/api/v1/problem-bank/authoring/problems/{problemId}` | `GetProblemForAuthoring` |
| 35 | GET | `/api/v1/problem-bank/authoring/problems/{problemId}/duplicate-source` | `DuplicateProblem` |
| 36 | POST | `/api/v1/problem-bank/authoring/problems` | `SaveProblemContent — tạo mới` |
| 37 | PUT | `/api/v1/problem-bank/authoring/problems/{problemId}/content` | `SaveProblemContent — cập nhật` |
| 38 | PUT | `/api/v1/problem-bank/authoring/problems/{problemId}/spec` | `SaveProblemSpec` |
| 39 | PUT | `/api/v1/problem-bank/authoring/problems/{problemId}/ai-context` | `SaveAiAuthoringContext` |
| 40 | PUT | `/api/v1/problem-bank/authoring/problems/{problemId}/status` | `UpdateProblemPublishStatus` |
| 41 | GET | `/api/v1/problem-bank/authoring/problems/{problemId}/stats` | `GetProblemStats` |
| 42 | POST | `/api/v1/problem-bank/authoring/problems/{problemId}/testcases` | `CreateTestcase` |
| 43 | PATCH | `/api/v1/problem-bank/authoring/problems/{problemId}/testcases/{testcaseId}` | `UpdateTestcase` |
| 44 | DELETE | `/api/v1/problem-bank/authoring/problems/{problemId}/testcases/{testcaseId}` | `DeleteTestcase` |
| 45 | PUT | `/api/v1/problem-bank/authoring/problems/{problemId}/testcases/order` | `ReorderTestcases` |
| 46 | POST | `/api/v1/problem-bank/authoring/problems/{problemId}/testcases/bulk-upload-url` | `RequestTestcaseBulkUploadUrl` |
| 47 | POST | `/api/v1/problem-bank/authoring/problems/{problemId}/testcases/bulk-upload/confirm` | `ConfirmTestcaseBulkUpload` |
| 48 | POST | `/api/v1/problem-bank/authoring/problems/{problemId}/reference-solution/run` | `RunSampleSolutionAgainstTestcases` |
| 49 | POST | `/api/v1/problem-bank/authoring/problems/{problemId}/testcases/generate` | `GenerateTestcasesWithAi` |
| 50 | GET | `/api/v1/problem-bank/instructor/class-assignments` | `ListClassAssignments` |
| 51 | GET | `/api/v1/problem-bank/instructor/class-assignments/summary` | `GetInstructorAssignmentSummary` |
| 52 | GET | `/api/v1/problem-bank/instructor/classes/{classId}/assignable-problems` | `ListAssignableProblems` |
| 53 | POST | `/api/v1/problem-bank/instructor/classes/{classId}/assignments` | `AssignProblemsToClass` |
| 54 | DELETE | `/api/v1/problem-bank/instructor/classes/{classId}/assignments/{problemId}` | `RemoveClassAssignment` |
| 55 | GET | `/api/v1/problem-bank/instructor/assigned-problems/top` | `ListMyAssignedProblems` |
| 56 | GET | `/api/v1/problem-bank/settings/ai-testcase-generation` | `GetAiTestcaseGenerationSettings` |
| 57 | PUT | `/api/v1/problem-bank/settings/ai-testcase-generation` | `UpdateAiTestcaseGenerationSettings` |

### 2.3. `harness` (2 endpoint HTTP)

Bảng 5: `harness` là thư viện trong tiến trình; hợp đồng chính là các cổng ở `03-dd/api/F3_harness.md` mục 1.1. Hai vỏ HTTP phục vụ mã khung người học và xem trước chữ ký của tác giả.

| Mục | Method | Path | Operation |
| :--- | :--- | :--- | :--- |
| 3 | GET | `/api/v1/harness/problems/{problemId}/starter-code?language=&model=` | `GetStarterCode` |
| 4 | POST | `/api/v1/harness/function-signature/preview` | `PreviewFunctionSignature` |

### 2.4. `judge-orchestration` (24 endpoint REST, hai kênh STOMP, năm cổng trong tiến trình)

Bảng 6: Endpoint của `judge-orchestration`. Cột Mục là số mục trong `03-dd/api/F4_judge-orchestration.md`; hai kênh STOMP ở mục 2 của file đó.

| Mục | Method | Path | Ghi chú |
| :--- | :--- | :--- | :--- |
| 4 | GET | `/api/v1/judge/problems/{problemId}/execution-limits` | |
| 5 | POST | `/api/v1/judge/problems/{problemId}/run-sample` | |
| 6 | POST | `/api/v1/judge/problems/{problemId}/submissions` | |
| 7 | GET | `/api/v1/judge/problems/{problemId}/submissions/mine` | |
| 8 | GET | `/api/v1/judge/submissions/{submissionId}` | |
| 9 | GET | `/api/v1/judge/submissions/{submissionId}/progress` | |
| 10 | GET | `/api/v1/judge/me/submissions` | |
| 11 | GET | `/api/v1/judge/me/in-progress-problems` | |
| 12 | GET | `/api/v1/judge/me/metrics` | |
| 13 | GET | `/api/v1/judge/instructor/submissions` | |
| 14 | GET | `/api/v1/judge/instructor/submissions/{submissionId}` | |
| 15 | GET | `/api/v1/judge/instructor/classes/{classId}/students/{studentId}/submissions` | |
| 16 | GET | `/api/v1/judge/admin/queue/snapshot` | |
| 17 | GET | `/api/v1/judge/admin/queue/jobs` | |
| 18 | GET | `/api/v1/judge/admin/queue/infra-summary` | |
| 19 | PUT | `/api/v1/judge/admin/queue/consumption` | |
| 20 | PUT | `/api/v1/judge/admin/queue/autoscale` | |
| 21 | GET | `/api/v1/judge/admin/queue/settings` | |
| 21 | PUT | `/api/v1/judge/admin/queue/settings` | |
| 22 | GET | `/api/v1/judge/admin/infra-events` | |
| 23 | GET | `/api/v1/judge/admin/language-config` | |
| 24 | PUT | `/api/v1/judge/admin/language-config` | |
| 24A | GET | `/api/v1/judge/me/accepted-submissions` | `ListMyAcceptedSubmissions` (thêm 2026-10-23, `USR0302`) |
| 24B | GET | `/api/v1/judge/languages/enabled` | `ListEnabledSubmissionLanguages` (đọc công khai cho người đã đăng nhập, `USR0503`) |

### 2.5. `ai-review` (28 endpoint REST, một luồng SSE, bốn cổng trong tiến trình)

Bảng 6a: Endpoint của `ai-review`. Cột Mục là số mục trong `03-dd/api/F5_ai-review.md`; đường dẫn đầy đủ gồm base `/api/v1/ai`.

| Mục | Method | Path | Operation |
| :--- | :--- | :--- | :--- |
| 4 | POST | `/api/v1/ai/submissions/{submissionId}/solution-review` | `GetOrCreateSolutionReview` |
| 5 | GET | `/api/v1/ai/me/solution-reviews/problem-ids` | `ListMyReviewedProblemIds` |
| 6 | GET | `/api/v1/ai/me/solution-reviews/quick-insight` | `GetMySolutionReviewQuickInsight` |
| 7 | GET | `/api/v1/ai/me/solution-reviews/recent` | `GetMyRecentSolutionReviews` |
| 8 | POST | `/api/v1/ai/interview-sessions` | `StartInterviewSession` |
| 9 | GET | `/api/v1/ai/interview-sessions/{sessionId}` | `GetSessionDetail` |
| 10 | POST | `/api/v1/ai/interview-sessions/{sessionId}/turns` | `SendInterviewAnswerStream` (phản hồi SSE) |
| 11 | POST | `/api/v1/ai/interview-sessions/{sessionId}/hint` | `RequestInterviewHint` |
| 12 | POST | `/api/v1/ai/interview-sessions/{sessionId}/complete` | `CompleteInterviewSession` |
| 13 | GET | `/api/v1/ai/me/interview-sessions/summary` | `GetMyInterviewSummary` |
| 14 | GET | `/api/v1/ai/admin/prompt-templates` | `ListPromptTemplates` |
| 15 | GET | `/api/v1/ai/admin/prompt-templates/{featureCode}/history` | `GetPromptTemplateHistory` |
| 16 | PUT | `/api/v1/ai/admin/prompt-templates/{featureCode}/draft` | `UpdatePromptTemplateDraft` |
| 17 | POST | `/api/v1/ai/admin/prompt-templates/publish` | `PublishPromptTemplate` |
| 18 | GET | `/api/v1/ai/admin/token-budget` | `GetAiTokenBudget` |
| 19 | PUT | `/api/v1/ai/admin/token-budget/limit` | `UpdateAiTokenBudgetLimit` |
| 20 | POST | `/api/v1/ai/admin/token-budget/unlock` | `UnlockAiTokenBudget` |
| 21 | GET | `/api/v1/ai/admin/limits-config` | `GetRateLimitConfig` |
| 22 | PATCH | `/api/v1/ai/admin/limits-config` | `UpdateRateLimitConfig` |
| 23 | GET | `/api/v1/ai/admin/model-prices` | `ListModelPrices` |
| 24 | POST | `/api/v1/ai/admin/model-prices` | `CreateModelPrice` |
| 25 | PATCH | `/api/v1/ai/admin/model-prices/{priceId}` | `UpdateModelPrice` |
| 26 | DELETE | `/api/v1/ai/admin/model-prices/{priceId}` | `DeleteModelPrice` |
| 27 | GET | `/api/v1/ai/admin/usage/daily` | `GetAiUsageDailyBreakdown` |
| 28 | GET | `/api/v1/ai/admin/usage/alerts` | `ListAiUsageAlerts` |
| 29 | POST | `/api/v1/ai/admin/usage/alerts/{alertId}/review` | `MarkAiUsageAlertReviewed` |
| 30 | GET | `/api/v1/ai/admin/usage/top-users` | `ListTopAiUsers` |
| 31 | GET | `/api/v1/ai/admin/usage/top-problems` | `ListTopAiProblems` |

Cổng trong tiến trình (mục 32 của file hợp đồng, không có route): `CheckAiBudgetUseCase` và `RecordAiTokenUsageUseCase` (cho `interview-bank`), `TestcaseScriptGenerationPort` (cho `problem-bank`), `InterviewTranscriptReadUseCase` (cho `identity`). Sự kiện phát ra: `SolutionReviewCompleted`, `MockInterviewFinished` (mục 33).

### 2.6. `interview-bank` (23 endpoint REST, không kênh thời gian thực, ba cổng trong tiến trình)

Bảng 6b: Endpoint của `interview-bank`. Cột Mục là số mục trong `03-dd/api/F6_interview-bank.md`; đường dẫn đầy đủ gồm base `/api/v1/interview-bank`.

| Mục | Method | Path | Operation |
| :--- | :--- | :--- | :--- |
| 2 | GET | `/api/v1/interview-bank/questions` | `ListInterviewQuestions` |
| 3 | GET | `/api/v1/interview-bank/questions/{questionId}` | `GetInterviewQuestion` |
| 4 | PUT | `/api/v1/interview-bank/questions/{questionId}/recall` | `RateQuestionRecall` |
| 5 | PUT | `/api/v1/interview-bank/questions/{questionId}/bookmark` | `ToggleQuestionBookmark` |
| 6 | POST | `/api/v1/interview-bank/questions/{questionId}/practice-answers` | `SubmitPracticeAnswer` |
| 7 | GET | `/api/v1/interview-bank/questions/{questionId}/practice-answers` | `ListMyAnswerAttempts` |
| 8 | GET | `/api/v1/interview-bank/me/topic-progress` | `GetMyInterviewTopicProgress` |
| 9 | GET | `/api/v1/interview-bank/topics` | `ListQuestionTopics` |
| 10 | GET | `/api/v1/interview-bank/levels` | `ListQuestionLevels` |
| 11 | POST | `/api/v1/interview-bank/topics` | `CreateQuestionTopic` |
| 12 | PATCH | `/api/v1/interview-bank/topics/{topicId}` | `UpdateQuestionTopic` |
| 13 | PUT | `/api/v1/interview-bank/topics/order` | `ReorderQuestionTopics` |
| 14 | DELETE | `/api/v1/interview-bank/topics/{topicId}` | `DeleteQuestionTopic` |
| 15 | POST | `/api/v1/interview-bank/levels` | `CreateQuestionLevel` |
| 16 | PATCH | `/api/v1/interview-bank/levels/{levelId}` | `UpdateQuestionLevel` |
| 17 | PUT | `/api/v1/interview-bank/levels/order` | `ReorderQuestionLevels` |
| 18 | DELETE | `/api/v1/interview-bank/levels/{levelId}` | `DeleteQuestionLevel` |
| 19 | GET | `/api/v1/interview-bank/admin/questions` | `ListInterviewQuestionsAdmin` |
| 20 | GET | `/api/v1/interview-bank/admin/questions/{questionId}` | `GetInterviewQuestionDetail` |
| 21 | POST | `/api/v1/interview-bank/admin/questions` | `CreateInterviewQuestion` |
| 22 | PUT | `/api/v1/interview-bank/admin/questions/{questionId}` | `UpdateInterviewQuestion` |
| 23 | POST | `/api/v1/interview-bank/admin/questions/bulk/delete` | `DeleteInterviewQuestion` |
| 24 | POST | `/api/v1/interview-bank/admin/questions/import` | `ImportInterviewQuestions` |

Cổng trong tiến trình (mục 25 đến 27 của file hợp đồng, không có route): cổng ra `AnswerFeedbackPort` (tới Spring AI); cổng ra tới `ai-review` (`CheckAiBudgetUseCase`, `RecordAiTokenUsageUseCase`) và tới `identity` (`AuditLogPort`); cổng vào `ReadMyInterviewTopicProgressUseCase` (cho `identity`, cùng use case với mục 8). Không phát hay nhận sự kiện miền. Không có job nền.

## 3. Kênh thời gian thực

Bảng 7: Kênh STOMP đã có hợp đồng. Chi tiết (schema thông điệp, thứ tự, hành vi khi kết nối lại) ở `03-dd/api/F4_judge-orchestration.md` mục 2.

| Kênh | Hướng | Module |
| :--- | :--- | :--- |
| `/user/queue/submissions` | máy chủ gửi, mỗi người dùng một hàng đợi | `judge-orchestration` |
| `/topic/admin/queue` | máy chủ gửi, mỗi 5 giây | `judge-orchestration` |

Bảng 7a: Luồng SSE đã có hợp đồng. Chi tiết (sự kiện `turn.accepted`, `token`, `turn.completed`, `error`, thứ tự, hành vi khi nối lại) ở `03-dd/api/F5_ai-review.md` mục 2.

| Kênh | Hướng | Module |
| :--- | :--- | :--- |
| phản hồi `text/event-stream` của `POST /api/v1/ai/interview-sessions/{sessionId}/turns` | máy chủ gửi, một luồng mỗi lượt hội thoại; xác thực bằng `Authorization: Bearer` (giao diện đọc bằng `fetch`, không dùng `EventSource`) | `ai-review` |

## 4. Tham chiếu chéo

- Job nền: `03-dd/jobs/F1_identity.md`, `03-dd/jobs/F4_judge-orchestration.md`, `03-dd/jobs/F5_ai-review.md`. `problem-bank` không có job (BD không định nghĩa). `interview-bank` không có job (quyết định ở `03-dd/logic/F6_interview-bank.md` mục 1: lịch ôn tính đồng bộ, nhắc ôn tập chỉ là đề xuất ngoài phạm vi).
- Logic và kiểm đầu vào của `interview-bank`: `03-dd/logic/F6_interview-bank.md`, `03-dd/validation/F6_interview-bank.md`.
- Nghiệm thu: `04-tdd/modules/F1_identity.md`, `04-tdd/modules/F2_problem-bank.md`, `04-tdd/modules/F3_harness.md`, `04-tdd/modules/F4_judge-orchestration.md`, `04-tdd/modules/F5_ai-review.md`, `04-tdd/modules/F6_interview-bank.md`.
- Tổng hợp điểm cần quyết giữa các DD: `06-plan/reports/261023-1000-dd-giai-doan-1-2-tong-hop/report.md`.
