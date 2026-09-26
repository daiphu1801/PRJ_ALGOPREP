# DD — API module `identity` (F1)

> DD đầu tiên của dự án (`03-dd/` trước đây chỉ có `.gitkeep`). Đọc cùng `02-bd/architecture/identity.md`,
> `02-bd/database/identity.md`, `02-bd/security/identity.md` — DD này **không lặp lại** nội dung đã chốt ở
> BD, chỉ ghi thêm phần BD chưa nói được: hợp đồng endpoint xuyên màn, quyết định kỹ thuật, và các khối quy
> tắc nghiệp vụ trải dài nhiều sự kiện. Request/response chi tiết theo trường đã có đủ ở Sheet 7.1 của từng
> file `02-bd/screens/**` — ở đây chỉ liệt kê tối thiểu tên trường/kiểu/bắt buộc cho endpoint mà BD màn
> chưa đủ để code thẳng (chủ yếu là các API xác thực chưa gắn với màn cụ thể nào).
>
> **Quy ước path chốt tại DD này** (chưa có tiền lệ trong dự án, đây là DD đầu tiên): base path
> `/api/v1/identity`, JSON, xác thực qua `Authorization: Bearer <access_token>` trừ các endpoint đánh dấu
> "Công khai". Áp dụng chung `04xx`: `400` lỗi validate, `401` chưa xác thực/token hết hạn, `403` không đủ
> quyền, `404` không tìm thấy, `409` xung đột trạng thái, `422` vi phạm quy tắc nghiệp vụ (mã lỗi cụ thể
> trong `error.code`, dạng `IDT-nnn`).

## 0. Danh sách endpoint (khoanh vùng theo BD screens)

Quét toàn bộ `02-bd/screens/**` (Sheet 7.3 "Danh sách endpoint") tìm mọi endpoint ghi chú module sở hữu là
`identity`. Cột "Màn gọi" dùng mã màn `Fx`/`USR`/`INS`/`ADM`; cột "Nguồn BD" trỏ operation-name logic mà BD
đã đặt (BD không đặt path/method — đó là việc của DD này).

### 0.1. Xác thực và phiên (chưa gắn màn cụ thể — dùng chung bởi `SHR0101_auth` và mọi màn khác qua filter)

| # | Method | Path | Mô tả | Màn gọi | Ghi chú |
| :-: | :--- | :--- | :--- | :--- | :--- |
| 1 | POST | `/api/v1/identity/auth/register` | Đăng ký email + mật khẩu (F1-01) | `SHR0101_auth` | Công khai |
| 2 | POST | `/api/v1/identity/auth/login` | Đăng nhập, trả access token + set cookie refresh token (F1-02) | `SHR0101_auth` | Công khai; rate-limit BR-01 |
| 3 | POST | `/api/v1/identity/auth/refresh` | Xoay vòng refresh token, cấp access token mới (F1-03) | Toàn hệ thống (interceptor HTTP client) | Công khai nhưng bắt buộc header `X-Requested-With` (`02-bd/security/identity.md:16-18`); BR-02 |
| 4 | POST | `/api/v1/identity/auth/logout` | Thu hồi refresh token hiện tại, xoá cookie (F1-04) | `SHR0101_auth`, mọi khung `_shell.md` (menu người dùng) | |
| 5 | GET | `/api/v1/identity/auth/oauth/{provider}/redirect` | Bắt đầu luồng OAuth (`provider` = `github`\|`google`), trả URL kèm `state` (F1-15) | `SHR0101_auth` | Công khai; BR-03 |
| 6 | GET | `/api/v1/identity/auth/oauth/{provider}/callback` | Nhận callback OAuth, đối chiếu `state`, tự liên kết hoặc tạo tài khoản (F1-15) | `SHR0101_auth` | Công khai; BR-03 |
| 7 | POST | `/api/v1/identity/auth/password/forgot` | Gửi OTP quên mật khẩu (F1-17) | `SHR0101_auth` | Công khai; luôn trả 200 (chống dò email, `02-bd/security/identity.md:45-46`) |
| 8 | POST | `/api/v1/identity/auth/password/reset` | Xác nhận OTP + đặt mật khẩu mới (F1-17) | `SHR0101_auth` | Công khai; BR-04 |
| 49 | POST | `/api/v1/identity/auth/instructor/login` | Đăng nhập riêng cho giảng viên — cùng contract endpoint #2, tự chặn tài khoản không phải `INSTRUCTOR` | `views/instructor-auth` (route `/instructor/login`, không có màn RD/BD — `DEC-2026-0925-instructor-separate-login-route`) | Công khai; rate-limit BR-01; đánh số #49 (nối cuối danh sách) để không đổi số các endpoint 9-48 đã được trích dẫn nơi khác |

### 0.2. Hồ sơ cá nhân — `USR0502_profile.md`

| # | Method | Path | Mô tả | Nguồn BD (operation name) | Ghi chú |
| :-: | :--- | :--- | :--- | :--- | :--- |
| 9 | GET | `/api/v1/identity/me/profile` | Tải hồ sơ, trạng thái mật khẩu, danh sách provider đã liên kết | `GetMyProfile` [SoT: `02-bd/screens/users/USR0502_profile.md:594`] | |
| 10 | PATCH | `/api/v1/identity/me/profile` | Cập nhật hồ sơ, **trừ** `email` | `UpdateMyProfile` [SoT: `02-bd/screens/users/USR0502_profile.md:595`] | Server bỏ qua `role_id`/`status` nếu client gửi lên [SoT: `02-bd/screens/users/USR0502_profile.md:666`] |
| 11 | POST | `/api/v1/identity/me/email-change/request` | Yêu cầu đổi email, gửi OTP tới email mới | `RequestMyEmailChange` [SoT: `02-bd/screens/users/USR0502_profile.md:612`] | Trả trực tiếp lỗi "email đã dùng" — khác nguyên tắc chống dò email vì đã đăng nhập [SoT: `07-review/bd_open_questions_with_solutions_260924.md:136`] |
| 12 | POST | `/api/v1/identity/me/email-change/confirm` | Xác nhận OTP, đổi `email` | `ConfirmMyEmailChange` [SoT: `02-bd/screens/users/USR0502_profile.md:612`] | |
| 13 | POST | `/api/v1/identity/me/password` | Đổi mật khẩu (đã có) hoặc đặt mật khẩu lần đầu (tài khoản chỉ OAuth) | `ChangeMyPassword` [SoT: `02-bd/screens/users/USR0502_profile.md:571`] | BR-05: đặt mật khẩu lần đầu không cần mật khẩu cũ, không cần OTP thêm [SoT: `07-review/...:135`]; BR-06: đổi mật khẩu thu hồi mọi refresh token khác [SoT: `07-review/...:137`] |

### 0.3. Cài đặt và dữ liệu cá nhân — `USR0503_settings.md`

| # | Method | Path | Mô tả | Nguồn BD | Ghi chú |
| :-: | :--- | :--- | :--- | :--- | :--- |
| 14 | GET | `/api/v1/identity/me/settings` | Đọc `user_preferences` (không gồm chủ đề màu/ngôn ngữ giao diện — lưu thiết bị) | `GetMySettings` [SoT: Suy luận — theo cặp Get/Update đã dùng ở `GetMyProfile`] | |
| 15 | PATCH | `/api/v1/identity/me/settings` | Cập nhật `streak_reminder_enabled`, `weekly_report_enabled`, `preferred_interview_level` | `UpdateMySettings` [SoT: `07-review/bd_open_questions_with_solutions_260924.md:154`] | Độc lập với `UpdateMyInterviewPreferences` của `ai-review` (BR-07 — không gộp giao dịch) |
| 16 | GET | `/api/v1/identity/me/export?scope={profile\|submissions}` | Xuất CSV/JSON dữ liệu cá nhân (F1-22) | `ExportMyData` [SoT: `07-review/...:148`] | Đồng bộ, rate-limit 1 lần/5 phút/loại tệp [SoT: `07-review/...:148`]; kiểm `user_id` trong token khớp yêu cầu [SoT: `02-bd/security/identity.md:58-60`] |
| 17 | GET | `/api/v1/identity/me/export/interview-transcripts` | Xuất hội thoại phỏng vấn (dữ liệu ở `ai-review`, `identity` đọc qua cổng ra) | `ExportMyInterviewTranscripts` [SoT: `07-review/...:149`] | `ai-review` lỗi → trả `503` báo lỗi tại chỗ, **không khoá nút** phía UI [SoT: `07-review/...:149`] |
| 18 | POST | `/api/v1/identity/me/account/delete` | Tự xoá tài khoản — yêu cầu gõ lại email của chính mình để xác nhận (F1-16) | `DeleteMyAccount` [SoT: `07-review/...:150`] | Xem BR-08 |

### 0.4. Lớp học — `INS0201_class_management.md`, `INS0202_class_assignments.md`, `INS0203_class_progress.md`, `INS0204_class_student_detail.md`

| # | Method | Path | Mô tả | Màn gọi | Ghi chú |
| :-: | :--- | :--- | :--- | :--- | :--- |
| 19 | GET | `/api/v1/identity/classes/my` | Danh sách lớp của giảng viên đang đăng nhập | `INS0101_overview`, `INS0201_class_management` | |
| 20 | POST | `/api/v1/identity/classes` | Tạo lớp (F1-23) | `INS0201_class_management` | |
| 21 | PATCH | `/api/v1/identity/classes/{classId}` | Sửa tên/mô tả/ghi chú lịch (F1-23) | `INS0201_class_management` | Kiểm sở hữu: `instructor_id` khớp người gọi |
| 22 | DELETE | `/api/v1/identity/classes/{classId}` | Xoá lớp — hard delete (F1-24) | `INS0201_class_management` | `02-bd/database/identity.md:88-89` |
| 23 | POST | `/api/v1/identity/classes/{classId}/invite-codes` | Sinh mã mời mới, TTL 7 ngày (F1-25) | `INS0201_class_management` | Nhiều mã hiệu lực song song [SoT: `02-bd/database/identity.md:93-94`] |
| 24 | GET | `/api/v1/identity/classes/{classId}/invite-codes` | Liệt kê mã mời còn hiệu lực của lớp | `INS0201_class_management` | |
| 25 | POST | `/api/v1/identity/classes/join` | Học viên tham gia lớp bằng mã mời (F1-23) | `USR0101_problem_list` (khối "Bài tập lớp"), luồng nhập mã | BR-09: hết hạn/không tồn tại → `422 IDT-410` |
| 26 | GET | `/api/v1/identity/classes/{classId}/students` | Danh sách học viên trong lớp, kèm trạng thái phân loại (F1-27) | `INS0201_class_management`, `INS0203_class_progress` | Ngưỡng phân loại theo BR-10 |
| 27 | DELETE | `/api/v1/identity/classes/{classId}/students/{studentId}` | Gỡ học viên khỏi lớp (F1-26) | `INS0201_class_management` | Phát `StudentRemovedFromClass`, xem BR-11 |
| 28 | GET | `/api/v1/identity/classes/{classId}/students/{studentId}/detail` | Chi tiết một học viên trong lớp (F1-28) | `INS0204_class_student_detail` | Điểm TB tính theo phạm vi lớp [SoT: `07-review/...:184`, `DEC-2026-0921-teacher-screens-conflict-resolutions`] — đọc từ `judge-orchestration.GetStudentSubmissionMetrics` |
| 29 | GET | `/api/v1/identity/classes/{classId}/progress` | Dashboard tiến độ lớp: hoàn thành TB, phân loại 5 nhãn (F1-28) | `INS0203_class_progress` | Đọc `class_completion_stats` (BR-12); nhãn theo `DEC-2026-0921-teacher-screens-conflict-resolutions` |

### 0.5. Ma trận phân quyền và vai trò — `ADM0202_permission_matrix.md`

| # | Method | Path | Mô tả | Nguồn BD | Ghi chú |
| :-: | :--- | :--- | :--- | :--- | :--- |
| 30 | GET | `/api/v1/identity/rbac/matrix` | Đọc toàn bộ ma trận Role × Function × Action | `GetPermissionMatrix` [SoT: Suy luận] | `functions`/`actions` chỉ đọc [SoT: `02-bd/database/identity.md:54`] |
| 31 | PATCH | `/api/v1/identity/rbac/matrix/{roleId}/{functionId}/{actionId}` | Bật/tắt một ô, có hiệu lực ngay | `UpdateRolePermission` [SoT: `07-review/bd_open_questions_with_solutions_260924.md:278`] | BR-13: mỗi lần bấm ô gọi ngay, ghi `system_audit_logs`, publish invalidate cache |
| 32 | GET | `/api/v1/identity/rbac/roles` | Liệt kê role (3 hệ thống + tuỳ biến) | `ListRoles` [SoT: Suy luận] | |
| 33 | POST | `/api/v1/identity/rbac/roles` | Tạo role tuỳ biến, chọn `base_category` (F1-11) | `CreateRole` [SoT: `02-bd/architecture/identity.md:82-88`] | Xem ghi chú mục 0.6 (mâu thuẫn giữa hai nguồn BD) |
| 34 | DELETE | `/api/v1/identity/rbac/roles/{roleId}` | Xoá role tuỳ biến (F1-11) | `DeleteRole` | Chặn nếu `is_system = true` hoặc còn user gán (BR-14) |

### 0.6. Quản lý người dùng (ADMIN) — `ADM0201_user_management.md`

| # | Method | Path | Mô tả | Nguồn BD | Ghi chú |
| :-: | :--- | :--- | :--- | :--- | :--- |
| 35 | GET | `/api/v1/identity/admin/users?status=&role=&q=&page=` | Danh sách người dùng, lọc + phân trang | `ListUsers` [SoT: Suy luận] | |
| 36 | PATCH | `/api/v1/identity/admin/users/{userId}/role` | Đổi vai trò (F1-13) | `ChangeUserRole` [SoT: Suy luận] | BR-15 (bảo vệ admin cuối cùng) |
| 37 | PATCH | `/api/v1/identity/admin/users/{userId}/status` | Khoá/mở khoá tài khoản (F1-13) | `ChangeUserStatus` [SoT: `07-review/bd_open_questions_with_solutions_260924.md:268`] | Đổi hai chiều cùng một endpoint; BR-15 |
| 38 | POST | `/api/v1/identity/admin/users/{userId}/reset-password` | Đặt lại mật khẩu hộ (F1-13) | `ResetUserPassword` [SoT: Suy luận] | Sinh mật khẩu tạm, gửi email, thu hồi mọi refresh token của user đó |

**Chưa đưa vào phạm vi** (theo đề xuất BD, chưa xác nhận): "Thêm tài khoản" thủ công [SoT: `07-review/...:267`],
trạng thái "Chờ xác thực email" (chưa có cột nguồn) [SoT: `07-review/...:269`] — không tạo endpoint cho tới
khi chủ dự án xác nhận.

### 0.7. Nhật ký hệ thống — `ADM0403_system_log.md`

| # | Method | Path | Mô tả | Nguồn BD | Ghi chú |
| :-: | :--- | :--- | :--- | :--- | :--- |
| 39 | GET | `/api/v1/identity/admin/audit-logs?actor=&actionType=&from=&to=&page=` | Đọc nhật ký hệ thống | `ListSystemAuditLogs` [SoT: Suy luận] | Mọi `base_category = ADMIN` đọc được, **không qua** `permissions` [SoT: `02-bd/security/identity.md:38-41`] |
| 40 | GET | `/api/v1/identity/admin/audit-logs/export?format=csv` | Tải nhật ký dạng CSV | `ExportSystemAuditLogs` [SoT: `07-review/bd_open_questions_with_solutions_260924.md:328`] | Cột xuất: `occurred_at, actor_email, action_type, target_type, target_id`; che giá trị nhạy cảm trong `before_json`/`after_json` theo BR-16 |

### 0.8. Dashboard — `ADM0101_overview.md`, `INS0101_overview.md`, `USR0501_my_progress.md`

Theo ghi chú của BD [SoT: `07-review/bd_open_questions_with_solutions_260924.md:174,259`]: `admin_overview`
có 9 endpoint đều thuộc `identity`; `instructor_overview` có 3/6 thuộc `identity` (3 còn lại thuộc
`ai-review`/`problem-bank`, không liệt kê ở đây).

| # | Method | Path | Mô tả | Màn gọi | Ghi chú |
| :-: | :--- | :--- | :--- | :--- | :--- |
| 41 | GET | `/api/v1/identity/admin/dashboard/summary` | Thẻ chỉ số tổng quan (tổng người dùng, đang hoạt động 24h, mới/quay lại) | `ADM0101_overview` | "Đang hoạt động 24h" đếm `users.last_active_at` [SoT: `07-review/...:270`, `DEC-2026-0922-users-and-admin-conflict-resolutions`] |
| 42 | GET | `/api/v1/identity/admin/dashboard/user-growth` | Chuỗi số liệu người dùng mới theo ngày | `ADM0101_overview` | |
| 43 | GET | `/api/v1/identity/admin/dashboard/cross-module` | Số liệu tổng hợp `judge.submissions`/`problem.problems` qua đọc đồng bộ cổng ra (không cache read model per-user) | `ADM0101_overview` | Xem BR-17 |
| 44 | GET | `/api/v1/identity/instructor/dashboard/summary` | Tổng lớp, tổng học viên (DISTINCT), điểm TB toàn bộ lớp phụ trách | `INS0101_overview` | Đếm học viên theo người [SoT: `07-review/...:167`] |
| 45 | GET | `/api/v1/identity/instructor/dashboard/recent-activity?classId=` | Feed "Hoạt động gần đây", lọc theo lớp qua `scope_class_id` | `INS0101_overview` | BR-18 |
| 46 | GET | `/api/v1/identity/me/recent-activity` | "Hoạt động gần đây" của cá nhân (10 mục, 90 ngày) | `USR0501_my_progress`, `USR0530_dashboard nếu có` | [SoT: `02-bd/database/identity.md:116-117`] |
| 47 | GET | `/api/v1/identity/me/progress/summary` | Best-attempt theo bài, tỉ lệ Accepted, "Đúng ngay lần đầu", ngôn ngữ dùng nhiều nhất | `USR0202_my_submissions`, `USR0501_my_progress`, `USR0502_profile` | Đọc `user_submission_stats` [SoT: `02-bd/database/identity.md:114`, `07-review/...:68,71`] |
| 48 | GET | `/api/v1/identity/me/problem-solve-states?problemIds=` | Trạng thái giải theo từng `problem_id` của người gọi (map) | `ListMyProblemSolveStates` [SoT: `07-review/bd_open_questions_with_solutions_260924.md:16`] | Được `problem-bank` gọi qua cổng ra, cache 60 giây phía `problem-bank` |

**Chưa thiết kế ở DD này**: bảng ánh xạ `action_type` sang nhóm/nhãn hiển thị của nhật ký hệ thống, chu kỳ
làm mới dashboard, và endpoint `admin_overview` còn thiếu (6/9) — chốt khi viết DD `screens/` tương ứng theo
nguyên tắc "viết DD khi cần" của `CLAUDE.md`.

## 1. Yêu cầu/phản hồi tối thiểu (endpoint chưa gắn màn cụ thể, mục 0.1)

BD screens (Sheet 7.1) chỉ phủ endpoint có màn hình đích; nhóm 0.1 (auth) không thuộc màn hình nào (dùng
bởi `SHR0101_auth`, RD chưa vào chi tiết trường) nên DD chốt tối thiểu tại đây.

### `POST /auth/register`
Request: `{ "email": string (bắt buộc, RFC 5322), "password": string (bắt buộc), "displayName": string (bắt buộc, ≤100) }`
Response `201`: `{ "userId": uuid, "email": string }`
Lỗi: `409 IDT-101` (email đã tồn tại — không dùng ở luồng này vì người dùng đang tự đăng ký, không phải OWASP-enumeration case của quên mật khẩu).

### `POST /auth/login`
Request: `{ "email": string (bắt buộc), "password": string (bắt buộc) }`
Response `200`: `{ "accessToken": string, "expiresIn": 900, "user": { "id": uuid, "displayName": string, "baseCategory": "STUDENT"|"INSTRUCTOR"|"ADMIN" } }` + `Set-Cookie: refresh_token=...; HttpOnly; Secure; SameSite=Strict`
Lỗi: `401 IDT-102` (sai email/mật khẩu, thông báo chung không nói rõ sai cái nào), `429 IDT-103` (vượt rate-limit, BR-01), `403 IDT-104` (`status = DEACTIVATED`).

### `POST /auth/instructor/login`
`DEC-2026-0925-instructor-separate-login-route`. Request/response giống hệt `POST /auth/login` ở trên
(cùng bảng lỗi `IDT-102`/`IDT-103`/`IDT-104`), cộng thêm một bước chặn sau khi mật khẩu đã khớp:
Lỗi thêm: `403 IDT-111` (`baseCategory != INSTRUCTOR` — tài khoản đúng nhưng không có quyền giảng viên;
không áp nguyên tắc chống dò tài khoản của `password/forgot` vì mật khẩu đã được xác minh đúng ở bước
này, không phải dò email chưa xác thực).

### `POST /auth/refresh`
Request: không body, đọc cookie `refresh_token`, bắt buộc header `X-Requested-With: XMLHttpRequest`.
Response `200`: `{ "accessToken": string, "expiresIn": 900 }` + `Set-Cookie` token mới (rotate).
Lỗi: `401 IDT-105` (revoked/hết hạn), `401 IDT-106` (phát hiện reuse — cả family bị revoke, xem BR-02).

### `POST /auth/password/forgot`
Request: `{ "email": string (bắt buộc) }`
Response `200` (luôn luôn, kể cả email không tồn tại): `{ "message": "Nếu email tồn tại, mã xác nhận đã được gửi." }`

### `POST /auth/password/reset`
Request: `{ "email": string (bắt buộc), "otp": string (bắt buộc, 6 số), "newPassword": string (bắt buộc, xem `03-dd/validation/identity.md`) }`
Response `200`: `{ "message": "Đặt lại mật khẩu thành công." }`
Lỗi: `422 IDT-107` (OTP sai/hết hạn/quá 5 lần — thông báo chung, không phân biệt), `422 IDT-108` (tài khoản chỉ có OAuth, không có mật khẩu — trả hướng dẫn quay lại provider, `02-bd/security/identity.md:50-52`).

### `GET /auth/oauth/{provider}/redirect` và `.../callback`
`redirect` response: `{ "authorizationUrl": string, "state": string }`. `callback` nhận `code`, `state` qua query; response giống `POST /auth/login`. Lỗi `401 IDT-109` (state không khớp), `422 IDT-110` (`email_verified = false` ở provider — từ chối tự liên kết, `02-bd/security/identity.md:19-21`).

## 2. Sự kiện domain — hợp đồng tiêu thụ

Payload tối thiểu, thống nhất một hình dạng cho mọi sự kiện `identity` lắng nghe (mục 3.1 kiến trúc BD):

```json
{
  "eventId": "uuid",
  "eventType": "SubmissionGraded | SubmissionAccepted | SolutionReviewCompleted | MockInterviewFinished | StudentRemovedFromClass(phát ra, không tiêu thụ)",
  "occurredAt": "ISO-8601",
  "payload": { "...": "theo từng eventType, xem bảng dưới" }
}
```

| eventType | Nguồn | `payload` tối thiểu | Cập nhật read model nào |
| :--- | :--- | :--- | :--- |
| `SubmissionGraded` | `judge-orchestration` | `{ userId, problemId, submissionId, verdict, passedTestcaseRatio, language }` | `user_problem_best_score` (nếu `passedTestcaseRatio` tốt hơn best hiện tại), `user_submission_stats.total_submissions`/`accepted_count`/`first_try_accepted_count`/`top_language` |
| `SubmissionAccepted` | `judge-orchestration` | `{ userId, problemId, submissionId, classId? }` | `identity_recent_activity` (`activity_type = SUBMISSION_ACCEPTED`, `scope_class_id = classId` nếu bài thuộc một `class_assignment`) |
| `SolutionReviewCompleted` | `ai-review` | `{ userId, problemId, reviewId }` | `identity_recent_activity` (`activity_type = SOLUTION_REVIEW_COMPLETED`) |
| `MockInterviewFinished` | `ai-review` | `{ userId, sessionId, overallScore }` | `identity_recent_activity` (`activity_type = MOCK_INTERVIEW_FINISHED`) |

Tập giá trị `activity_type` chốt tại đây (BD chỉ khai cột, chưa liệt kê giá trị
[SoT: `02-bd/database/identity.md:116-117` cột `activity_type`]):
`SUBMISSION_ACCEPTED`, `SOLUTION_REVIEW_COMPLETED`, `MOCK_INTERVIEW_FINISHED`, `CLASS_PROBLEM_PUBLISHED`
(nhận từ `problem-bank` khi giao bài mới cho lớp), `STUDENT_STREAK_ACHIEVED` (tự phát sinh nội bộ khi quét
`user_submission_stats`). Hai giá trị dạng "quét định kỳ" (`học viên chưa nộp bài tuần`) **không** nằm
trong enum này — đó là kết quả job quét, xem `03-dd/logic/identity.md` BR-18.

`identity` là **producer** của `StudentRemovedFromClass` (không tiêu thụ event này) khi endpoint 27 chạy
xong, để `judge-orchestration` tự xử lý dữ liệu lượt nộp trong phạm vi lớp
[SoT: `02-bd/database/identity.md:99-102`].

## 3. Đặc tả khoá tranh chấp/giao dịch

Không áp dụng cho phần lớn endpoint — các thao tác ghi của `identity` (đổi hồ sơ, đổi mật khẩu, tạo/xoá
lớp) là thao tác đơn người dùng trên bản ghi của chính họ, xung đột ghi đồng thời không có ý nghĩa nghiệp
vụ thật (client cuối cùng thắng theo `updated_at`).

**Trường hợp có race-condition thật**: đổi vai trò/khoá tài khoản (endpoint 36, 37) khi hai quản trị viên
thao tác đồng thời trên cùng một `userId`, hoặc thao tác trên chính người đang giữ vai trò ADMIN cuối cùng.
Xem BR-15 ở `03-dd/logic/identity.md` — dùng khoá bi quan mức dòng (`SELECT ... FOR UPDATE` trên `users`)
trong phạm vi transaction của endpoint, không dùng optimistic lock vì thao tác admin có tần suất thấp và
cần chặn cứng ngay tại thời điểm ghi, không chấp nhận retry.

## 4. Ghi chú xung đột giữa hai nguồn BD (không tự quyết)

Mục 0.6 (endpoint 33 `CreateRole`): `02-bd/architecture/identity.md:82-88` ghi "Đã xác nhận 2026-09-13 —
role tuỳ biến là yêu cầu thật" dựa trên bằng chứng UI ở `ADM0202_permission_matrix.md`, nhưng
`07-review/bd_open_questions_with_solutions_260924.md:367` (mục `02-bd/database/identity.md` việc còn mở
#3) lại đề xuất "giữ 3 role cố định dạng enum, không cần bảng `role` riêng". Hai nguồn cùng cấp BD mâu
thuẫn trực tiếp. DD này **theo xác nhận có ngày tháng cụ thể** (`02-bd/architecture/identity.md:82-88`,
2026-09-13) vì nó dẫn bằng chứng UI cụ thể (`ADM0202_permission_matrix.md:32-34,48`) và đến sau về mặt thời
gian so với đề xuất suy luận chung chung trong tài liệu tổng hợp câu hỏi mở — không phải một thay đổi kiến
trúc mới do DD tự đề xuất, nên không mở decision entry. **Đề nghị chủ dự án**: cập nhật lại dòng #3 của
`02-bd/database/identity.md` mục "Việc còn mở" cho khớp, vì hiện tại BD gốc và tài liệu tổng hợp câu hỏi mở
đang nói hai điều khác nhau về cùng một bảng.

## 5. Tham chiếu

- `02-bd/architecture/identity.md`, `02-bd/database/identity.md`, `02-bd/security/identity.md`.
- `07-review/bd_open_questions_with_solutions_260924.md` — đề xuất mặc định dùng làm quyết định DD.
- `03-dd/logic/identity.md` — các khối BR dẫn ở bảng trên.
- `03-dd/validation/identity.md` — độ mạnh mật khẩu, rate-limit, độ dài trường.
- `04-tdd/identity.md` — tiêu chí nghiệm thu `AC-nn` tương ứng.
