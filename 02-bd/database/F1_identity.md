# BD — Database module `identity` (F1)

> Schema PostgreSQL: `identity`. Đọc cùng `02-bd/architecture/F1_identity.md` mục 4 (mô hình `base_category`
> vs `role`) trước khi đọc bảng dưới đây — hai khái niệm không trùng nhau.

## 1. Bảng chính

**Quy ước kiểu cột tham chiếu (bổ sung 2026-10-23, `DEC-2026-1023-db-actor-review-fixes`):** mọi cột trỏ tới một dòng của bảng khác, kể cả cột chéo schema không có khoá ngoại vật lý (`user_id`, `problem_id`, `testcase_id`, `created_by`, `updated_by`, `assigned_by`, `class_id`, `submission_id`...), có kiểu **UUID**, trùng kiểu khoá chính của bảng đích. Ô "Kiểu" bỏ trống hoặc chỉ ghi "tham chiếu" ở các bảng dưới đây đọc là UUID. Cột `NOT NULL` hay `nullable` vẫn theo ô Ghi chú của từng cột.

**Quy ước ON DELETE toàn schema (vòng 4):** thực thể gốc `users` không bao giờ bị xoá cứng (tự xoá chỉ ẩn danh hoá, Admin chỉ khoá, mục 1.1), nên mọi khoá ngoại trỏ tới `users` (`oauth_identities.user_id`, `refresh_tokens.user_id`, `class_enrollments.student_id`, `classes.instructor_id`, `created_by`, `updated_by`, `actor_user_id`...) dùng mặc định **NO ACTION**, không khai báo CASCADE. `users.role_id` → `roles.id` là **RESTRICT**: không xoá được role còn user gán (BR-14, `03-dd/logic/F1_identity.md:259`). Chỉ các bảng con của `classes` và `permissions` theo `roles` dùng CASCADE (mục 1.4, 1.9, 1.10), riêng `identity_recent_activity.scope_class_id` dùng SET NULL (mục 1.12). **Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4):** thêm quy ước.

### 1.1. `users`
Cốt lõi F1-01 → F1-09, F1-15, F1-16.

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `email` | VARCHAR(254) NOT NULL | Unique **không phân biệt hoa/thường**: chỉ mục `UNIQUE (lower(email))`, ghi và tra đều qua `LOWER(email)` (`03-dd/validation/F1_identity.md:34-35`). **Sửa 2026-10-22 (đồng bộ DB, phase 1):** trước đây ghi `VARCHAR unique` không nêu độ dài và cách so trùng |
| `password_hash` | VARCHAR nullable | NULL nếu tài khoản chỉ tạo qua OAuth (F1-15) và chưa từng đặt mật khẩu |
| `display_name` | VARCHAR | |
| `role_id` | FK → `roles.id` **ON DELETE RESTRICT** | Xem mục 1.2. **Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4):** khai báo RESTRICT: role còn user gán không xoá được (BR-14) |
| `current_position` | VARCHAR nullable | Trường hồ sơ "Vai trò hiện tại" (F1-09) — **chức danh tự khai**, không phải RBAC role, tránh nhầm với `role_id`. **Sửa 2026-10-18 (`DEC-2026-1018-usr-open-questions-phase2-5`):** chỉ còn mang vai trò hiện tại; ~~trước đây ghi chú nói cột này phục vụ cả "Vị trí mục tiêu"~~, phần đó chuyển sang `target_position` |
| `target_position` | VARCHAR nullable | **Cột MỚI, 2026-10-18, `DEC-2026-1018-usr-open-questions-phase2-5`** (ponytail bậc 1). Hồ sơ "Vị trí mục tiêu" (F1-09), chức danh tự khai, không phải RBAC role. Ẩn danh hoá (F1-16) không bắt buộc ghi đè (không phải dữ liệu định danh) `[SoT: Suy luận]` |
| `password_changed_at` | TIMESTAMPTZ nullable | **Cột MỚI, 2026-10-18, `DEC-2026-1018-usr-open-questions-phase2-5`.** `NULL` = chưa từng đổi mật khẩu kể từ khi tạo tài khoản; đặt thời điểm theo đồng hồ PostgreSQL ở **mọi** lần đổi mật khẩu và lần đặt mật khẩu đầu tiên (tài khoản OAuth chưa có `password_hash`). Phục vụ dòng "đổi mật khẩu lần cuối" ở hồ sơ. Với quên mật khẩu F1-17, so với `issued_at` của OTP dưới khoá dòng `users` để chặn dùng mã lần hai (owner chốt 2026-10-27). Không dùng để vô hiệu token: đổi mật khẩu thu hồi mọi refresh token khác qua `refresh_tokens.revoked_at` (giữ phiên hiện tại) |
| `school_or_company` | VARCHAR nullable | F1-09 |
| `phone` | VARCHAR(20) nullable | F1-09, thêm 2026-10-13 (`DEC-2026-1013-a2-reads-student-contact`). Số điện thoại tự khai, không bắt buộc, không xác thực. Chỉ chủ tài khoản sửa; A2 đọc được khi học viên thuộc lớp mình phụ trách. Ẩn danh hoá (F1-16) ghi đè thành NULL |
| `default_language` | VARCHAR(16) NOT NULL DEFAULT `'PYTHON'` | F1-09, F1-20 — ngôn ngữ nộp bài mặc định. Lưu **mã** `JAVA`/`CPP`/`PYTHON`, **không** dùng ENUM của DB để thêm/tắt ngôn ngữ không cần migration; danh sách hợp lệ lấy từ `judge.language_configs.enabled` (`02-bd/screens/users/USR0503_settings.md:297, 304-305, 413`). Mặc định `PYTHON` theo prototype Cài đặt (`USR0503_settings.md:413`). Sửa 2026-10-22 (đồng bộ DB vòng 2): số dòng trích đã dịch, trước ghi `:303, 412`. **Sửa 2026-10-22 (đồng bộ DB, phase 1):** đặt kiểu và mặc định |
| `status` | ENUM(`ACTIVE`,`DEACTIVATED`,`PENDING_VERIFICATION`) | F1-16, F1-13 (ADMIN khoá/mở khoá). **Thêm 2026-10-11 (`DEC-2026-1011-admin-system-open-questions-closed`):** `PENDING_VERIFICATION` = đã tạo nhưng chưa xác thực email; chuyển sang `ACTIVE` khi xác thực. **Sửa 2026-10-22 (đồng bộ DB vòng 2, DEC-2026-1022-db-sync-round2 mục 1):** OTP email chỉ áp cho tài khoản **tự đăng ký** (F1-01); tài khoản **Admin tạo** (F1-13) cũng khởi tạo `PENDING_VERIFICATION` nhưng chuyển `ACTIVE` ở **lần đăng nhập thành công đầu tiên bằng mật khẩu tạm** (người nhận đã chứng minh truy cập được email), sau đó `must_change_password` có hiệu lực. Không thêm cột `email_verified`, không dùng khoá Redis TTL **để biểu diễn trạng thái** vì không liệt kê hay lọc được. **Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-identity-schema-sync):** giữ `PENDING_VERIFICATION`; mã OTP xác thực email (không phải trạng thái) nằm ở Redis, khoá `identity:emailverify:<userId>` (mục 3), cùng cơ chế F1-17, **chỉ cho tự đăng ký** (vòng 2). ~~RD **chưa có** mã F1-nn cho luồng này~~ (đã lỗi thời). **Sửa 2026-10-22 (vòng 2):** RD nay có F1-33 và F1-34 (`01-rd/req/F1_identity.md:101, 113`). **Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4):** bỏ phần "chưa có mã" còn sót. **Sửa 2026-10-22 (đồng bộ DB vòng 3, DEC-2026-1022-self-register-pending-login-blocked):** tài khoản tự đăng ký còn `PENDING_VERIFICATION` **không đăng nhập được** (`403 IDT-113`, mã đề xuất) cho tới khi nhập OTP email; OTP chặn đăng nhập chỉ với tự đăng ký (`must_change_password = false`). Luồng Admin tạo không đổi. Đặt lại mật khẩu bằng OTP cũng đưa tài khoản Admin tạo đang `PENDING_VERIFICATION` sang `ACTIVE` và xoá `must_change_password` (BR-04). Không đổi schema |
| `last_active_at` | TIMESTAMPTZ nullable | Ghi nhận mốc hoạt động gần nhất (đăng nhập/nộp bài), cập nhật throttle theo phút (`DEC-2026-0922-users-and-admin-conflict-resolutions`). **Chỉ** nuôi phần hiển thị "hoạt động lần cuối"; **không** là nguồn của số "người dùng hoạt động" ở `ADM0101` (nguồn đó là `refresh_tokens.issued_at`, mục 1.6). **Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-identity-schema-sync):** bỏ câu "phục vụ ADM0201" gây hiểu lầm về nguồn số liệu dashboard |
| `must_change_password` | BOOLEAN NOT NULL DEFAULT false | **Cột MỚI, 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-identity-schema-sync, ponytail bậc 1).** `true` khi Admin tạo tài khoản (F1-13) để buộc đổi mật khẩu ở lần đăng nhập đầu; **buộc mềm** (vòng 2, DEC-2026-1022-db-sync-round2 mục 4): chỉ giao diện chuyển hướng theo cờ ở phản hồi đăng nhập/`GET /me`, máy chủ không chặn endpoint khác; xoá về `false` ở lần đổi mật khẩu đầu tiên, cùng giao dịch đặt `password_changed_at = now()`. Tự đăng ký (F1-01) và OAuth giữ `false` |
| `deactivation_source` | ENUM(`SELF`,`ADMIN`) nullable | **Cột MỚI, 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-identity-schema-sync, ponytail bậc 6).** Đặt cùng lúc `status` chuyển sang `DEACTIVATED`: `SELF` = người dùng tự xoá tài khoản (F1-16); `ADMIN` = quản trị viên khoá (F1-13, F1-32). `NULL` khi `status` khác `DEACTIVATED`; mở khoá thì đặt lại `NULL` cùng `deactivated_at`. Lý do tồn tại: `status = DEACTIVATED` dùng chung cho hai trường hợp, nhưng chỉ `SELF` được ẩn danh hoá — nếu thiếu cột này, job sẽ ẩn danh nhầm tài khoản bị Admin khoá (mất dữ liệu định danh vĩnh viễn) |
| `deactivated_at` | TIMESTAMPTZ nullable | Mốc `status` chuyển sang `DEACTIVATED`. Với `deactivation_source = SELF` đây là **mốc bắt đầu ân hạn 30 ngày** (F1-16); với `ADMIN` chỉ là thời điểm khoá, **không** khởi động đồng hồ ân hạn nào. **Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-identity-schema-sync):** tách nghĩa theo nguồn khoá |
| `anonymized_at` | TIMESTAMPTZ nullable | Job định kỳ set khi ẩn danh hoá xong; chỉ có thể khác NULL với dòng `deactivation_source = SELF`. **Sửa 2026-10-22 (đồng bộ DB vòng 3, DEC-2026-1022-self-register-pending-login-blocked):** khi `anonymized_at IS NOT NULL` Admin **không** mở khoá được (dữ liệu định danh đã mất, không khôi phục được); nút mở khoá ở `ADM0201` bị vô hiệu cho dòng đó. Không thêm cột |
| `created_at` / `updated_at` | TIMESTAMPTZ | |

Khi ẩn danh hoá (job định kỳ, F1-16): job **chỉ quét** `status = 'DEACTIVATED' AND deactivation_source = 'SELF' AND
deactivated_at <= now() - interval '30 days' AND anonymized_at IS NULL`; tài khoản bị Admin khoá (`ADMIN`) không bao giờ
bị job đụng tới, khôi phục bằng mở khoá của Admin. `email`, `display_name` được ghi đè bằng giá trị vô danh, `phone` đặt NULL
(`deleted-user-<id>@anon.local`, `"Người dùng đã xoá"`); đồng thời **xoá các dòng `oauth_identities` của user** (cột `provider_email`
là email thật, mục 1.5) và đặt NULL `school_or_company`, `current_position`, `target_position` `[SoT: Suy luận — F1-16 chỉ nêu
email/tên hiển thị; ba trường tự khai này có thể gắn với một người cụ thể (tên trường, nơi làm việc) nên đề xuất xoá, chờ chủ dự án chốt]`;
**không xoá dòng**, không cascade xoá bài
nộp/phiên phỏng vấn — các bảng ở module khác giữ nguyên `user_id` để không vỡ thống kê lớp
(`01-rd/req/F1_identity.md:262-268`, F1-16). **Sửa 2026-10-22 (đồng bộ DB, phase 1):** sửa số dòng RD trích sai (`200-202`).
**Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-identity-schema-sync):** thêm điều kiện `deactivation_source = 'SELF'` và phần xoá `oauth_identities`/trường tự khai.
**Chênh lệch tài liệu cần sửa ở nơi khác (không sửa ở đây):** `03-dd/logic/F1_identity.md` BR-08 (`:122-131`) bước 1 quét `status = 'DEACTIVATED' AND deactivated_at <= now() - 30 ngày` **không có** `deactivation_source`, nên theo DD hiện hành job vẫn ẩn danh cả tài khoản Admin khoá; bước 2 chưa xoá `oauth_identities`; bước 1 của đường tự xoá (`:117`) chưa đặt `deactivation_source = 'SELF'`. `02-bd/architecture/F1_identity.md:31, 99` chỉ nêu "khoá mềm → ẩn danh hoá" và ân hạn 30 ngày, không phân biệt nguồn khoá.

### 1.2. `roles`
Hỗ trợ mô hình hai lớp ở mục 4 kiến trúc.

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `code` | VARCHAR unique | Ví dụ `STUDENT`, `INSTRUCTOR`, `ADMIN`, hoặc role tuỳ biến |
| `name` | VARCHAR | Tên hiển thị |
| `base_category` | ENUM(`STUDENT`,`INSTRUCTOR`,`ADMIN`) | Quyết định Lớp 1 + khu giao diện; role tuỳ biến vẫn phải chọn một giá trị |
| `is_system` | BOOLEAN | `true` cho 3 role seed — không xoá được (F1-11) |
| `permission_version` | BIGINT NOT NULL DEFAULT 0 | Tăng đơn điệu cùng giao dịch mỗi khi ma trận quyền của role thay đổi; authorization đọc từ PostgreSQL primary trước khi dùng cache (`DEC-2026-1028-qa-f1-permission-immediate`) |
| `created_at` / `updated_at` | TIMESTAMPTZ | `updated_at` thêm 2026-10-22: role tuỳ biến đổi được tên/`base_category` (F1-11) |

Seed cố định 3 dòng `is_system = true`: `STUDENT`/`INSTRUCTOR`/`ADMIN`, `base_category` trùng `code`.

### 1.3. `functions` / `actions` — dữ liệu seed cố định (F1-11)

`functions(id, code, name)` — 11 dòng seed theo F1-12: `PROBLEM_AUTHORING`, `TESTCASE_MANAGEMENT`,
`CLASS_MANAGEMENT`, `USER_MANAGEMENT`, `JUDGE_QUEUE_MONITOR`, `LANGUAGE_CONFIG`, `AI_CONFIG`, `AI_TOKEN_BUDGET`,
`SYSTEM_AUDIT_LOG`, `INTERVIEW_BANK_MANAGEMENT`, `PERMISSION_MATRIX`. **Sửa 2026-10-23 (rà soát DB theo 3 actor, `DEC-2026-1023-db-actor-review-fixes`):** thêm `LANGUAGE_CONFIG` (F4-11) làm function thứ 11. Trước đó 10 dòng, thiếu function này dù `ADM0501` và bốn bảng cấu hình của `judge` (`language_configs`, `grading_defaults`, `sandbox_settings`, `generator_sandbox_settings`) đã kiểm quyền bằng nó (`02-bd/database/F4_judge-orchestration.md:169`, `02-bd/security/F4_judge-orchestration.md:77`).

`actions(id, code)` — 4 dòng seed: `CREATE`, `READ`, `UPDATE`, `DELETE`.

Chỉ đọc trên giao diện (F1-11, `01-rd/req/F1_identity.md:54-57`) — không có API ghi cho hai bảng này ngoài migration seed.

### 1.4. `permissions` — ô của ma trận (F1-10)

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `role_id` | FK → `roles.id` | |
| `function_id` | FK → `functions.id` | |
| `action_id` | FK → `actions.id` | |
| `granted` | BOOLEAN | |
| `updated_at`, `updated_by` | TIMESTAMPTZ, FK user | Ghi vào `system_audit_logs` mỗi lần đổi (F1-14) |

**Seed ma trận mặc định:** migration chèn đủ 3 role x 11 function x 4 action = 132 dòng `permissions` (**Sửa 2026-10-23:** trước đây 10 function, 120 dòng; thêm 12 dòng của `LANGUAGE_CONFIG`: `ADMIN` bật cả 4, `INSTRUCTOR` và `STUDENT` tắt). `ADMIN`: `granted = true` toàn bộ, trong đó `PERMISSION_MATRIX` bắt buộc đủ 4 hành động (RD ghi "mặc định chỉ `ADMIN` có toàn quyền", `01-rd/req/F1_identity.md:64`). `STUDENT`: toàn bộ `false` (quyền học viên đi theo vai trò Lớp 1, F1-05, không qua ma trận, `01-rd/req/F1_identity.md:22`). `INSTRUCTOR`: RD chưa nêu từng ô; **chốt 2026-10-22 (chủ dự án chốt 2026-10-22, chờ ghi DEC- ở phase 0):** bật đủ 4 hành động `CREATE`/`READ`/`UPDATE`/`DELETE` cho `CLASS_MANAGEMENT`, `PROBLEM_AUTHORING`, `TESTCASE_MANAGEMENT` (khớp luồng giảng viên F1-23 đến F1-28, F2-01 đến F2-09), các function còn lại `false`. **Sửa 2026-10-22 (đồng bộ DB, phase 1):** thêm seed.

Unique `(role_id, function_id, action_id)` (chỉ mục này đã phủ truy vấn theo `role_id` vì `role_id` đứng đầu, không tạo chỉ mục riêng, xem mục 2). **Khoá ngoại:** `role_id` → `roles.id` **ON DELETE CASCADE**: xoá role chỉ xảy ra khi `is_system = false` và không còn user gán (BR-14, `03-dd/logic/F1_identity.md:204-205`) nên các ô ma trận của role đó đi theo role, không mồ côi; `function_id`, `action_id` là `RESTRICT` (dữ liệu seed). **Sửa 2026-10-22 (đồng bộ DB, phase 1):** bổ sung hành vi xoá. Đổi một dòng → invalidate cache quyền ngay (mục bảo mật).

### 1.5. `oauth_identities` (F1-15)

`(id, user_id FK, provider ENUM(GITHUB,GOOGLE), provider_user_id, provider_email, linked_at)`,
unique `(provider, provider_user_id)`. `provider_email` là email thật của provider, nên ẩn danh hoá (mục 1.1) xoá cả các dòng của user (**Sửa 2026-10-22, phase 2**). Liên kết tự động vào `users` đã tồn tại nếu `provider_email`
khớp email hiện có (`01-rd/req/F1_identity.md:254-260`, F1-15).

### 1.6. `refresh_tokens` (F1-02 → F1-04)

`(id, user_id FK, token_hash, family_id, issued_at, expires_at, revoked_at)`. `family_id` phục vụ phát
hiện tái sử dụng khi rotate (mục bảo mật). Nguồn sự thật là bảng này; Redis chỉ giữ blacklist để tránh
tra Postgres mỗi request — xem mục 3.

**Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-identity-schema-sync, ponytail bậc 3):** `issued_at` còn là **nguồn
số liệu người dùng hoạt động của `ADM0101`**: thẻ "người dùng hoạt động" 30 ngày, sparkline 7 ngày và cột "quay lại" của biểu đồ
giữ chân theo tháng đều đếm `user_id` phân biệt theo `issued_at` (`02-bd/screens/admin/ADM0101_overview.md:336-338, 420, 581` (số dòng sửa 2026-10-22 vòng 2);
câu hỏi Q2 ở `:684` được đóng theo hướng này). Hệ quả: dòng hết hạn/đã thu hồi **phải giữ tối thiểu 6 tháng** (đúng dải 6 tháng của
biểu đồ giữ chân) trước khi job dọn xoá; thêm chỉ mục `refresh_tokens(issued_at)` (mục 2). Số liệu đo **lần cấp token**,
không hẳn lần đăng nhập (rotate cũng sinh dòng mới nhưng `user_id` phân biệt nên không đếm trùng). `users.last_active_at` không tham gia.

### 1.7. `password_reset_requests` / `email_change_requests`

**Hai tên này KHÔNG phải bảng.** Chúng là **khoá Redis** có TTL ngắn (10 phút), không có DDL, không có migration, không có dòng Postgres: `password_reset_requests` ứng với `identity:pwreset:<userId>` và `identity:pwreset:resend:<userId>`, `email_change_requests` ứng với `identity:emailchange:<userId>` (bảng khoá ở mục 3). Lý do: dữ liệu ngắn hạn, không cần join quan hệ. Tái dùng chung cơ chế OTP 6 số (F1-17, F1-09 đổi email). **Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4):** làm rõ để không ai tạo bảng theo tên mục.

### 1.8. `classes` (F1-23, F1-24)

`(id, instructor_id FK users, name VARCHAR(120) NOT NULL, description VARCHAR(500) NULL, schedule jsonb NULL, created_at, updated_at)`. Độ dài lấy từ `02-bd/screens/teacher/INS0201_class_management.md` (tên lớp 120 ở Khu vực C NO 2; mô tả 500 ở mục Kiểm nhập liệu "Độ dài các trường lớp") — **Sửa 2026-10-22 (đồng bộ DB, phase 1):** ghi kiểu và độ dài. Xoá là
**hard delete** (F1-24) — không có cột `deleted_at`. **Đổi 2026-10-11:** cột `schedule_note` (text) thành
`schedule` (jsonb), cấu trúc ở mục 1.14 (`DEC-2026-1011-class-schedule-structured`).
**Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4):** xoá lớp phát sự kiện `ClassDeleted { classId }` (producer `identity`, `03-dd/api/F1_identity.md` mục 2); hệ quả với bảng của `identity`: `class_invite_codes` và `class_enrollments` đi theo bằng `ON DELETE CASCADE`, `identity_recent_activity.scope_class_id` về `NULL` bằng `ON DELETE SET NULL` (mục 1.12). Dữ liệu schema khác (`problem-bank.class_assignments`) do module đó tự xử lý khi nhận sự kiện, `identity` không ghi chéo schema.

### 1.9. `class_invite_codes` (F1-25)

`(id, class_id FK → classes.id ON DELETE CASCADE, code VARCHAR(8) unique, expires_at, created_by FK user, created_at)`. `code`: 8 ký tự chữ hoa và số, bỏ ký tự dễ nhầm `0`/`O`/`1`/`I`, máy chủ sinh (chốt 2026-10-22, chủ dự án chốt 2026-10-22, chờ ghi DEC- ở phase 0). Xoá lớp là hard delete (F1-24) nên mã mời đi theo lớp. **Sửa 2026-10-22 (đồng bộ DB, phase 1):** thêm hành vi xoá. Nhiều mã cùng hiệu lực
song song cho một lớp (`01-rd/req/F1_identity.md:121-130`, F1-25) — không có cột "mã hiện hành duy nhất".

### 1.10. `class_enrollments` (F1-23 tham gia, F1-26 gỡ)

`(id, class_id FK → classes.id ON DELETE CASCADE, student_id FK → users.id, joined_at, left_at TIMESTAMPTZ NULL)`, unique `(class_id, student_id)`. Xoá lớp (hard delete, F1-24) kéo theo dòng ghi danh; xoá học viên không cascade vì tài khoản chỉ ẩn danh hoá, không xoá dòng (mục 1.1). **Sửa 2026-10-22 (đồng bộ DB, phase 1):** thêm hành vi xoá.

**Tỉ lệ hoàn thành của lớp tính khi đọc:** `completionPct` ở `INS0101`/`class_progress` dùng `class_enrollments` (`left_at IS NULL`), danh sách bài còn giao và F4 `GetClassStudentSubmissionMetrics.assignmentResults[]`. Một bài chỉ hoàn thành nhờ lượt Accepted từ lúc giao hiện hành; `user_problem_best_score` toàn cục không làm nguồn cho chỉ số lớp (`DEC-2026-1025-dd-class-assignment-time-window`). **Không** có bảng read model `class_completion_stats`.

**Đổi 2026-10-14 (`DEC-2026-1014-keep-learning-history-on-leave`, ponytail bậc 6):** gỡ học viên (F1-26) **không
xoá dòng** và **không xoá lịch sử làm bài**; chỉ đặt `left_at = now()`. Trước đây: ~~xoá thật kèm cascade dữ
liệu lịch sử làm bài trong phạm vi lớp~~.

- **Ghi danh còn hiệu lực** = `left_at IS NULL`. Mọi truy vấn "học viên của lớp", "tổng số học viên", tiến độ lớp
  (F1-27/F1-28), "lớp của tôi" phía học viên **phải lọc `left_at IS NULL`**. Học viên đã rời **không tính** vào
  tổng số và tiến độ lớp.
- **Giữ nguyên dòng** để `INS0206` (danh sách bài nộp) và giảng viên vẫn đọc được bài nộp làm lúc học viên còn
  trong lớp, gắn ghi chú "Đã rời lớp" (`DEC-2026-1014-instructor-submissions-list-rules`). Dữ liệu của chính học
  viên không bị đụng.
- `identity` chỉ đặt `left_at` rồi phát `StudentRemovedFromClass`; **`judge-orchestration` không còn xoá hay ẩn lượt
  nộp** khi nhận sự kiện (chỉ dùng nếu cần cập nhật bộ đếm). `identity` vẫn không viết vào schema khác.
- Gỡ lặp lại (dòng đã có `left_at`) là idempotent, không ghi đè mốc cũ: `UPDATE ... SET left_at = now() WHERE
  left_at IS NULL`.
- ĐÃ CHỐT 2026-10-14 (`DEC-2026-1014-class-leave-details`): học viên tham gia lại bằng mã mời: dùng lại dòng cũ, đặt `left_at = NULL` (giữ unique, không sinh dòng thứ
  hai); `joined_at` giữ nguyên; cài đặt bằng `ON CONFLICT (class_id, student_id) DO UPDATE SET left_at = NULL`, không dùng `DO NOTHING` (xem BR-09 ở `03-dd/logic/F1_identity.md`). ~~Bài nộp trong khoảng đã rời thuộc phạm vi cần chủ dự án chốt `[SoT: Suy luận — decision-registry chưa nói về việc tham gia lại]`~~ (phần "tham gia lại" đã chốt). ~~Phần bài nộp làm trong khoảng đã rời còn mở: sau khi vào lại thì `left_at` = NULL nên khoảng đã rời không còn thể hiện trong dòng này.~~ **ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-instructor-submissions-list-details):** chấp nhận khoảng thời gian đã rời **tính là "trong lớp"** khi xét quyền đọc bài nộp (giữ `joined_at` cũ, `left_at` về NULL); không thêm bảng lịch sử từng đoạn tham gia. Quy tắc cửa sổ ở `INS0206` và `USR0201`: `submitted_at >= joined_at` và (`left_at IS NULL` hoặc `submitted_at <= left_at`) [Nguồn: .nexa/control/decision-registry.md:3987-3989].

### 1.11. `system_audit_logs` (F1-14)

`(id, actor_user_id FK, action_type, target_type, target_id, before_json, after_json, created_at)`.

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `actor_user_id` | UUID NOT NULL, FK → `users.id` | Mọi dòng hiện có đều do người thao tác (kể cả tự xoá tài khoản, `03-dd/logic/F1_identity.md:119`); job định kỳ ẩn danh hoá không ghi dòng audit (`03-dd/logic/F1_identity.md:122-129`) nên không cần NULL. Nếu sau này job hệ thống phải ghi thì mới đổi sang nullable |
| `action_type` | VARCHAR(64) NOT NULL | Giá trị theo enum `AuditAction` (đoạn H4 dưới), không phải ENUM của DB |
| `target_type` | VARCHAR(32) NOT NULL | Ví dụ `USER`, `ROLE`, `PERMISSION`, `SETTING` |
| `target_id` | VARCHAR(64) NULL | VARCHAR vì đích có thể là UUID, khoá chuỗi (`system_settings.key`) hoặc khoá ghép của một ô ma trận |
| `before_json`, `after_json` | JSONB NULL | Snapshot tự do, không chuẩn hoá theo loại hành động (`03-dd/logic/F1_identity.md:198-200`) |
| `created_at` | TIMESTAMPTZ NOT NULL DEFAULT now() | |

Chỉ mục: `(created_at DESC)` cho màn nhật ký và job dọn theo hạn lưu; `(actor_user_id, created_at DESC)` cho lọc theo người; `(action_type, created_at DESC)` (đã nêu ở dưới). **Sửa 2026-10-22 (đồng bộ DB, phase 1):** đặt kiểu và chỉ mục.

Chỉ ghi hành động quản trị của người (đổi ma trận quyền, đổi vai trò, khoá/mở khoá, reset mật khẩu, tạo
role tuỳ biến...) — không ghi sự kiện hạ tầng (đó là `judge-orchestration`, F4-10). **Bổ sung 2026-10-23 (`DEC-2026-1023-db-actor-review-decisions`):** hành động quản trị phát sinh ở module khác (quản lý chủ đề và độ khó của `problem-bank` và `interview-bank`, sửa cấu hình của `judge-orchestration`) cũng nằm trong bảng này, nhưng module đó **không ghi trực tiếp** vào schema `identity`: nó gọi cổng vào `AuditLogPort` do `identity` cung cấp, trong cùng JVM và cùng giao dịch với thay đổi nghiệp vụ; `actor_user_id` là người đang thao tác.

**Bổ sung 2026-10-01 (`DEC-2026-1001-admin-configurable-settings`), loại `action_type` thứ nhất do người dùng tự kích hoạt:**
`PASSWORD_RESET_REQUESTED` ("yêu cầu đặt lại mật khẩu", nhóm phân loại hiển thị **Xác thực**,
`02-bd/screens/admin/ADM0403_system_log.md`). Ghi một dòng mỗi lần hệ thống **phát hành** một mã OTP quên mật
khẩu (cùng thời điểm tạo khoá Redis `identity:pwreset:<userId>`, mục 3 dưới), với `actor_user_id` = `target_id`
= `users.id` của tài khoản được đặt lại, `target_type = 'USER'`, `before_json`/`after_json` rỗng. **Không bao
giờ lưu mã OTP**, không lưu email hay mật khẩu. Yêu cầu cho email không khớp tài khoản nào không ghi dòng
(không có `users.id` để làm tác nhân, và để khỏi lộ ra email nào tồn tại); thẻ ở trang tổng quan Admin vì vậy
đếm số **mã đã phát hành**, không đếm mọi lần bấm "Quên mật khẩu". Đây là mở rộng so với câu "chỉ ghi hành
động quản trị" ở trên: F1-14 gốc nói về hành động quản trị, còn loại này là sự kiện xác thực — thuộc phạm vi
quyết định đã nêu, nhưng RD F1-14 (`01-rd/req/F1_identity.md:68-75`) chưa nhắc, cần mở rộng RD `[SoT: Suy luận]`.
Chỉ mục gợi ý `(action_type, created_at DESC)` để đếm 24 giờ/7 ngày nhanh `[SoT: Suy luận]`.

**Bổ sung 2026-10-05 (F1-32) — lý do khoá nằm ở đâu:** thao tác khoá tài khoản ghi lý do trong
`after_json` (ví dụ `{"status": "DEACTIVATED", "reason": "..."}`, kèm `notifyByEmail` để biết có gửi
email không). **Không cột mới** và **không bảng lý do riêng**: cột mới thì phải migrate mọi bản ghi cũ,
bảng riêng thì phải thêm khóa ngoại và một đường ghi thứ hai cho cùng một giao dịch — trong khi lý do chỉ
được **đọc** cùng dòng nhật ký, không bao giờ lọc hay sửa, nên cột JSON sẵn có là chỗ đúng. Đổi lại, lý do
không lọc được theo câu truy vấn; nếu sau này cần thống kê "khoá vì lý do gì" thì mở câu hỏi phạm vi riêng
chứ không sửa ngược bản ghi cũ. Mở khoá **không** có lý do — nên cấu trúc `after_json` của hai hành
động này không giống nhau, và màn nhật ký phải hiển thị khác nhau `[SoT: Suy luận]`.

**Bổ sung 2026-10-11 (H4) — ánh xạ `action_type` ra phần hiển thị:** một enum `AuditAction` trong module
`identity` mang, cho từng `action_type`, ba thứ: nhóm phân loại hiển thị, tên dịch vụ và khoá i18n của mẫu
câu. Giao diện dựng câu nội dung từ khoá đó cộng tham số lấy từ `after_json`; **không** thêm cột `category`,
`message`, `service` vào bảng. Danh sách khoá che là hằng số của DD BR-16, không phải cấu hình
(`DEC-2026-1011-admin-system-open-questions-closed`, ponytail bậc 5).

### 1.12. Read model tổng hợp từ domain event (mục 3.1 kiến trúc)

- `user_problem_best_score(user_id, problem_id, best_ratio NUMERIC(5,4), best_verdict VARCHAR(32) NOT NULL, updated_at)`, PK `(user_id, problem_id)`; `best_ratio` trong [0,1] `[SoT: Suy luận]`; `best_verdict` dùng lại đúng giá trị của `judge.submissions.status` (`ACCEPTED`, `WRONG_ANSWER`, `TIME_LIMIT_EXCEEDED`, `RUNTIME_ERROR`, `COMPILE_ERROR`; không gồm trạng thái chưa cuối), là verdict của lượt đang giữ `best_ratio`, lưu VARCHAR để không phải migrate khi `judge` đổi enum `[SoT: Suy luận]` (**Sửa 2026-10-22, phase 2**) — phục vụ F1-06
  best-attempt (`DEC-2026-0831-partial-score-testcase-ratio`).
- `user_submission_stats(user_id PK, total_submissions, ac_eligible_count INT NOT NULL DEFAULT 0, accepted_count, first_try_accepted_count, top_language VARCHAR(16) NULL, language_counts JSONB NOT NULL DEFAULT '{}', updated_at)` — `total_submissions` đếm mọi lượt kết thúc, gồm `SYSTEM_ERROR`; `ac_eligible_count` đếm trạng thái cuối khác `SYSTEM_ERROR` và là mẫu số tỉ lệ AC theo quyết định owner 2026-10-25. `top_language` là mã ngôn ngữ nộp nhiều nhất (`JAVA`/`CPP`/`PYTHON`), NULL khi chưa nộp; cập nhật bằng event `SubmissionGraded`; `language_counts` (JSONB) đếm số lượt nộp theo mã ngôn ngữ; `top_language` là khoá có đếm lớn nhất, hoà thì giữ giá trị hiện có `[SoT: Suy luận]` — phục vụ F1-07 tỉ lệ
  Accepted và chỉ số "Đúng ngay lần đầu" trên USR0202/USR0501 (`DEC-2026-0922-users-and-admin-conflict-resolutions`).

Khi thêm `ac_eligible_count` cho dữ liệu đã có, migration tính lại một lần từ `judge.submissions` theo `user_id` với `status` cuối khác `SYSTEM_ERROR`; không gán mặc định 0 cho tài khoản đã có lịch sử. Sau backfill, sự kiện mới cập nhật tăng dần theo `eventId`.
- `user_daily_submission_stats(user_id, day DATE, submission_count INT, PRIMARY KEY (user_id, day))` — **Bảng MỚI, 2026-10-18 (`DEC-2026-1018-usr-open-questions-phase2-5`, ponytail bậc 1 + 4).** Read model đếm lượt nộp theo ngày cho lưới hoạt động 12 tháng của `USR0601`. **Chốt tên `user_daily_submission_stats`, PK `(user_id, day)` (Sửa 2026-10-22, phase 2; trước đây ghi "đề xuất, chốt ở DD")**. Rà các bảng hiện có: `user_submission_stats` chỉ giữ tổng cộng dồn (không có chiều ngày) và `identity_recent_activity` bị dọn sau 90 ngày, nên **không tái dùng được**. Điền bởi **cùng domain event** nuôi `user_submission_stats` (upsert `submission_count + 1` tại `day` quy theo UTC+7, mục 1.15), nên không thêm đường tiêu thụ thứ hai. Lưới 12 tháng tối đa 366 dòng mỗi người; không có TTL, không bị bộ lọc khoảng ngày của màn hình ảnh hưởng. **Sửa 2026-10-22 (vòng 2, DEC-2026-1022-db-sync-round2 mục 3, 6):** lưới được đọc từ bảng này qua endpoint `identity` `GET /me/activity-calendar` (`03-dd/api/F1_identity.md` #55), không còn qua cổng ra `judge`; `day` được `identity` suy ra từ `submittedAt` (UTC) của `SubmissionGraded`, quy sang UTC+7. `user_submission_stats.first_try_accepted_count` tăng 1 khi payload có `firstAttemptForProblem = true` và `verdict = ACCEPTED`; cờ do `judge` tính (true chỉ với lượt nộp đầu tiên từ trước tới nay của người đó cho bài đó, mặc định của chủ dự án 2026-10-22)
- `identity_recent_activity(id PK, actor_user_id, scope_class_id, activity_type, ref_type, ref_id, occurred_at)` — phục vụ
  F1-30 "Hoạt động gần đây", `scope_class_id` nullable phục vụ giảng viên lọc hoạt động học viên theo lớp trên INS0101 (`DEC-2026-0922-users-and-admin-conflict-resolutions`), giữ 90 ngày (job dọn định kỳ), hiển thị 10 mục gần nhất. **Sửa 2026-10-22 (đồng bộ DB, phase 2):** `activity_type VARCHAR(40) NOT NULL` (không dùng ENUM của DB), tập giá trị theo `03-dd/api/F1_identity.md:206-211`: `SUBMISSION_ACCEPTED`, `SOLUTION_REVIEW_COMPLETED`, `MOCK_INTERVIEW_FINISHED`, `CLASS_PROBLEM_PUBLISHED`, `STUDENT_STREAK_ACHIEVED`. **Không** có `CLASS_HAS_ABSENT_STUDENTS`: loại sự kiện suy ra từ quét định kỳ đã bỏ 2026-10-14 (`01-rd/req/F1_identity.md:224-226`, DEC-2026-1014-instructor-batch1-counting-rules); `03-dd/logic/F1_identity.md:261` (BR-18) còn nhắc giá trị này, DD cần dọn. RD `:224-226` liệt kê sự kiện "join" nhưng DD chưa có giá trị tương ứng, nên BD **chưa** thêm giá trị nào cho sự kiện tham gia lớp. **Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4):** bỏ tham chiếu "câu hỏi mở mục 5" đã lỗi thời.
  **Khoá ngoại `scope_class_id` (bổ sung khai báo còn thiếu, ID-13):** `scope_class_id` → `classes.id` **ON DELETE SET NULL** (cùng schema `identity`, nên được khai báo FK vật lý). Xoá lớp (hard delete, F1-24) không xoá dòng hoạt động của học viên mà chỉ gỡ gắn lớp; dòng đó vẫn hiện ở feed cá nhân (`actor_user_id`) và biến mất khỏi feed lớp vì chỉ mục feed lớp lọc `scope_class_id IS NOT NULL` (mục 2). `[SoT: DEC-2026-1022-db-sync-round4 mục 3]`
- `user_preferences(user_id PK FK → users.id, streak_reminder_enabled BOOLEAN NOT NULL DEFAULT true, weekly_report_enabled BOOLEAN NOT NULL DEFAULT false, preferred_interview_level ENUM(`INTERN`,`JUNIOR`,`MIDDLE`,`SENIOR`) NOT NULL DEFAULT 'MIDDLE', preferred_max_turns SMALLINT NOT NULL DEFAULT 12 CHECK (preferred_max_turns IN (8, 12, 16)), preferred_hint_allowed BOOLEAN NOT NULL DEFAULT true, updated_at TIMESTAMPTZ)` (mặc định theo `02-bd/screens/users/USR0503_settings.md:424, 436-437`, số dòng sửa 2026-10-22 vòng 2, trước ghi `:423, 435-436`; chưa có dòng thì máy chủ trả mặc định; **Sửa 2026-10-22 (đồng bộ DB, phase 1):** thêm kiểu, mặc định, `updated_at`; **Sửa 2026-10-22 (phase 2, DEC-2026-1022-identity-schema-sync, ponytail bậc 4):** chỉ **một** bảng `user_preferences` cho mọi tuỳ chọn cá nhân của F1-21; tên `notification_preferences` không còn dùng, BD `USR0503` phải đổi sang `user_preferences`) — F1-21, mở rộng lưu thêm cấp độ phỏng vấn ưa thích (`INTERN`/`JUNIOR`/`MIDDLE`/`SENIOR`, `DEC-2026-0922-users-and-admin-conflict-resolutions`). **Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4):** (N2) `preferred_interview_level` đổi từ VARCHAR(16) sang **ENUM** bốn giá trị, cùng tập với `ai.interview_sessions.interviewer_level` (`DEC-2026-1022-ai-judge-schema-sync` mục 1). **Quy ước mã ngôn ngữ:** các cột chứa mã ngôn ngữ trong `identity` (`default_language`, `top_language`, khoá của `language_counts`) giữ `VARCHAR(16)`, không dùng ENUM, để `identity` không phụ thuộc kiểu của schema `judge`. (ID-23) thêm hai cột mặc định phỏng vấn giả lập theo người dùng: `preferred_max_turns` (ba giá trị 8/12/16 và mặc định 12 theo `02-bd/screens/users/USR0503_settings.md:425`; ràng buộc CHECK là đề xuất của BD `[SoT: Suy luận]`) và `preferred_hint_allowed` (mặc định bật theo `USR0503_settings.md:426`). Kiểu `SMALLINT`/`BOOLEAN` khớp `ai.interview_sessions.max_turns`/`hint_allowed` (`02-bd/database/F5_ai-review.md:106-107`) nhưng **không có FK và không có truy cập chéo schema**: `ai-review` không đọc bảng này; frontend đọc giá trị qua `GetMySettings` rồi truyền vào lời gọi tạo phiên, `interview_sessions` chốt giá trị tại thời điểm tạo (`DEC-2026-1022-db-sync-round4` mục 4). Hai cột luôn có giá trị (NOT NULL + mặc định) nên dòng chưa tồn tại vẫn trả mặc định 12/bật. Các tuỳ chọn editor (cỡ chữ, vim mode) được lưu tại `localStorage` trình duyệt theo nguyên tắc ponytail tinh gọn, không đẻ thêm bảng nháp hay bảng cấu hình editor.

## 2. Chỉ mục (index) đáng chú ý

- `users(lower(email))` unique — tra cứu đăng nhập, không phân biệt hoa/thường (`03-dd/validation/F1_identity.md:34-35`).
- `refresh_tokens(token_hash)` UNIQUE — tra token khi refresh (`03-dd/logic/F1_identity.md:49-53`, BR-02 bước 1).
- `refresh_tokens(family_id)` — revoke cả family khi phát hiện tái sử dụng (BR-02 bước 3).
- `refresh_tokens(user_id, revoked_at)` — liệt kê phiên còn hiệu lực.
- `refresh_tokens(issued_at)` — đếm người dùng hoạt động theo cửa sổ 30 ngày/7 ngày/tháng cho `ADM0101` và job dọn theo hạn giữ 6 tháng (mục 1.6). **Thêm 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-identity-schema-sync).**
- `system_audit_logs(created_at DESC)`, `system_audit_logs(actor_user_id, created_at DESC)` — mục 1.11.
- `class_enrollments(student_id) WHERE left_at IS NULL` — "lớp của tôi" phía học viên, chỉ ghi danh còn hiệu lực (khớp quy tắc lọc `left_at IS NULL` ở mục 1.10) `[SoT: Suy luận]`. **Sửa 2026-10-22 (đồng bộ DB, phase 1):** thêm điều kiện lọc.
- `class_enrollments(class_id) WHERE left_at IS NULL` — đếm và liệt kê học viên còn trong lớp (thêm 2026-10-14, `DEC-2026-1014-keep-learning-history-on-leave`) `[SoT: Suy luận]`.
- `identity_recent_activity(actor_user_id, occurred_at DESC)` — truy vấn 10 mục gần nhất của cá nhân.
- `identity_recent_activity(scope_class_id, occurred_at DESC) WHERE scope_class_id IS NOT NULL` — feed hoạt động lớp học (INS0101).
- ~~`permissions(role_id)`~~ — **bỏ**: unique `(role_id, function_id, action_id)` đã phục vụ truy vấn nạp quyền theo role khi cache miss (`role_id` là cột đầu), chỉ mục riêng chỉ tốn chi phí ghi. **Sửa 2026-10-22 (đồng bộ DB, phase 1):** loại chỉ mục thừa.

## 3. Redis — key pattern

| Key | TTL | Dùng cho |
| :--- | :--- | :--- |
| `identity:refresh:blacklist:<jti>` | = thời hạn còn lại của token | Chặn refresh token đã bị revoke mà chưa hết hạn tự nhiên (rotate/logout) |
| `identity:pwreset:<userId>` | 10 phút | Mã OTP quên mật khẩu, kèm số lần thử sai và `issued_at` lấy từ đồng hồ PostgreSQL khi phát mã. Không tạo cho tài khoản chỉ OAuth. `issued_at` dùng cùng `users.password_changed_at` để chặn phát lại sau khi đổi thành công; lỗi ghi DB giữ mã và TTL gốc (owner chốt 2026-10-27; DD BR-04) |
| `identity:pwreset:resend:<userId>` | 60 giây | Chặn gửi lại mã quá nhanh |
| `identity:emailchange:<userId>` | 10 phút | Mã OTP đổi email (tái dùng cơ chế F1-17) |
| `identity:emailverify:<userId>` | 10 phút | Mã OTP xác thực email cho tài khoản `PENDING_VERIFICATION` **tự đăng ký** (F1-01), kèm bộ đếm số lần thử sai, cùng cơ chế F1-17. Xác thực đúng → `status = ACTIVE`. **Sửa 2026-10-22 (vòng 2, DEC-2026-1022-db-sync-round2 mục 1):** khoá này chỉ có cho tự đăng ký; tài khoản Admin tạo không có khoá này (chuyển `ACTIVE` ở lần đăng nhập đầu bằng mật khẩu tạm). Tên khoá `[SoT: Suy luận — DEC-2026-1022-identity-schema-sync nêu tên này, mã nghiệp vụ là F1-33 (xác thực email) và F1-34 (buộc đổi mật khẩu), `01-rd/req/F1_identity.md:101, 113`; hai mã này ở RD đang ghi "mã đề xuất"]. **Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4):** bỏ câu "RD chưa có mã F1-nn".` |
| `identity:emailverify:resend:<userId>` | 60 giây | Chặn gửi lại mã xác thực quá nhanh, theo mẫu `identity:pwreset:resend` `[SoT: Suy luận]` |
| `identity:oauthstate:<state>` | 10 phút | `state` chống CSRF của luồng OAuth, sinh ở `redirect`, đối chiếu ở `callback` (`03-dd/logic/F1_identity.md:63-65`; `03-dd/validation/F1_identity.md` mục 6) |
| `identity:permcache:<roleId>` | Không TTL, invalidate chủ động khi `permissions` đổi | Cache `{permissionVersion,matrix}` theo role; chỉ dùng khi version bằng `roles.permission_version` vừa đọc từ PostgreSQL primary. Không dùng cache để thay thế bước đọc version. |
| `identity:ratelimit:login:<email>:<ip>` | 15 phút (`EXPIRE` đặt ở lần sai đầu tiên), ngưỡng 5 lần | Chống dò mật khẩu, khoá theo cặp `(email, IP)` (`03-dd/logic/F1_identity.md:36`; `03-dd/validation/F1_identity.md` mục 2). Xoá khi đăng nhập đúng |
| `identity:ratelimit:emailchange:<userId>` | 24 giờ, ngưỡng 3 lần | Giới hạn đổi email khi đã đăng nhập, lỗi `IDT-V05` (`03-dd/validation/F1_identity.md` mục 2). Tên khoá do BD đặt theo mẫu trên `[SoT: Suy luận]`, DD chưa nêu tên |
| `identity:ratelimit:export:<userId>:<scope>` | 5 phút, ngưỡng 1 lần | Giới hạn xuất dữ liệu cá nhân theo loại tệp, lỗi `IDT-V06` (`03-dd/validation/F1_identity.md` mục 2). Tên khoá `[SoT: Suy luận]` như trên |
| Kênh pub/sub `identity:permcache:invalidate` | Không lưu (pub/sub) | Payload `{ roleId, permissionVersion }`; mọi instance subscribe và xoá cache Caffeine cục bộ theo `roleId` (BR-13). Pub/sub giúp refresh sớm; bước đọc version từ PostgreSQL primary bảo đảm quyền đúng khi message chậm hoặc mất. |

### 1.13. `system_settings` (cấu hình hệ thống do ADMIN đặt, bảng khoá-giá trị)

`(key PK, value_json JSONB, updated_at TIMESTAMPTZ, updated_by FK → users.id)`. Thêm 2026-10-11 theo
`DEC-2026-1011-admin-system-open-questions-closed` (H5 phần c, ponytail bậc 1 qua
`DEC-2026-1001-admin-configurable-settings`). Hiện có hai khoá:

| `key` | `value_json` | Mặc định | Ghi chú |
| :--- | :--- | :--- | :--- |
| `audit_log.retention_days` | số nguyên | `90` | Thời hạn lưu `system_audit_logs`; tối thiểu `1` |
| `audit_log.expired_policy` | `"DELETE"` hoặc `"ARCHIVE"` | `"DELETE"` | `ARCHIVE` (nén và lưu trữ) **hoãn** (bậc 3): job dọn coi như "giữ nguyên dòng", không xoá |

Mỗi lần sửa ghi một dòng `system_audit_logs` (phân loại "Cấu hình") với giá trị cũ và mới trong
`after_json`. Chỉ ghi qua `PUT /admin/settings/log-retention` (`03-dd/api/F1_identity.md` mục 0.7.1).

### 1.14. Cấu trúc `classes.schedule` (đổi 2026-10-11, `DEC-2026-1011-class-schedule-structured`)

Cột `classes.schedule_note` (text, ghi chú tự do) đổi thành `classes.schedule` (jsonb, NULL được), vì chủ dự
án muốn hiển thị lịch học từng phần ở trang chi tiết lớp (`INS0205`). Tiêu chí ponytail bậc 1 (chủ dự án chỉ
đạo) và bậc 3 (demo hiển thị lịch). Mục này đặt cuối mục 1 để các số dòng của mục 1.9 tới 1.13 mà tài liệu
khác đang trích không bị lệch.

| Khoá | Kiểu | Ý nghĩa |
| :--- | :--- | :--- |
| `days` | mảng số nguyên | Thứ trong tuần theo ISO: 1 = Thứ 2 tới 7 = Chủ nhật; không trùng |
| `startTime`, `endTime` | chuỗi `HH:mm` | Giờ bắt đầu, giờ kết thúc |
| `room` | chuỗi | Phòng học |
| `startDate`, `endDate` | chuỗi `yyyy-MM-dd` | Khoảng thời gian áp dụng lịch |
| `note` | chuỗi | Ghi chú tự do còn lại |

Mọi khoá đều tuỳ chọn; lớp không có lịch thì cột là NULL (hoặc `{}`, giao diện coi như nhau). Quy tắc kiểm
(máy chủ thực thi, DB không tự kiểm jsonb): có giờ thì phải có ít nhất một ngày; `startTime` và `endTime`
đi cùng nhau và `endTime` sau `startTime`; `endDate` không trước `startDate`; `room` tối đa 100 ký tự và
`note` tối đa 200 ký tự `[SoT: Suy luận — hai giới hạn là đề xuất, chủ dự án chưa chốt]`. Mã lỗi ở
`03-dd/validation/F1_identity.md` mục 5. Không cần chỉ mục vì lịch chỉ đọc theo `classes.id`. Backend chưa có
bảng `classes` thật (`05-coding` chỉ có kiểu dữ liệu phía giao diện), nên không cần bước chuyển dữ liệu từ
`schedule_note`.

### 1.15. Thay đổi lược đồ 2026-10-18 (`DEC-2026-1018-usr-open-questions-phase2-5`) — danh sách cho người viết migration

Bảng 1.15.1 gom mọi cột hoặc bảng **mới** của quyết định này ở cả bốn module để tìm một chỗ.

| Đối tượng | Module / file BD | Loại | Ghi chú |
| :--- | :--- | :--- | :--- |
| ~~`testcases.explanation_md` TEXT nullable~~ | `problem-bank` (`02-bd/database/F2_problem-bank.md` mục 1.12) | Cột mới, **bị thay 2026-10-22** (`DEC-2026-1022-problem-bank-schema-sync`) | Lời giải thích nay là `worked_examples.explanation_md`; cột trên `testcases` và cờ `is_worked_example` đã bỏ |
| `submission_testcase_results.actual_output_snippet` TEXT nullable | `judge-orchestration` (`02-bd/database/F4_judge-orchestration.md` mục 1.2) | Cột mới | Chỉ ghi khi testcase `SAMPLE` |
| `users.target_position` VARCHAR nullable | `identity` (mục 1.1) | Cột mới | `current_position` đổi nghĩa, không đổi kiểu |
| `users.password_changed_at` TIMESTAMPTZ nullable | `identity` (mục 1.1) | Cột mới | `NULL` = chưa từng đổi |
| `user_daily_submission_stats` | `identity` (mục 1.12) | Bảng mới, **tên đã chốt 2026-10-22** | PK `(user_id, day)` |

Không phải cột mới (quy tắc hiển thị hoặc tính ở tầng ứng dụng): mã người dùng hiển thị = **8 ký tự đầu của
`users.id`** (UUID), không có cột; `overallScore`/`feedbackSummary` của phiên phỏng vấn tính từ `rubric_scores`
(`02-bd/database/F5_ai-review.md`); ngôn ngữ nộp mặc định ở Cài đặt lấy từ `judge.language_configs.enabled = true`,
`users.default_language` giữ nguyên.

**Múi giờ cắt ngày:** mọi chỗ "ngày" trong `identity` (cột `day` của `user_daily_submission_stats`, chuỗi liên
tiếp ngày, biểu đồ, lưới 12 tháng) dùng một múi giờ hệ thống cố định **UTC+7**; cột thời gian vẫn là
`TIMESTAMPTZ`, quy đổi chỉ khi cắt ngày. Cùng quy tắc với `judge-orchestration`.

## 4. Migration — thứ tự tạo bảng

(Sửa 2026-10-22, phase 2: không có bảng hay khoá ngoại mới; chỉ thêm cột `users.must_change_password`/`deactivation_source`, `user_submission_stats.top_language`/`language_counts`, chỉ mục `refresh_tokens(issued_at)`, nên thứ tự dưới đây giữ nguyên; hai cột `ENUM`/`BOOLEAN` của `users` nằm trong migration tạo `users`.)

`roles` → `functions`/`actions` (seed) → `users` → `permissions` (vì `permissions.updated_by` là khoá ngoại tới `users`; seed ma trận chèn sau bước này) → `user_preferences` → `oauth_identities` →
`refresh_tokens` → `classes` → `class_invite_codes` → `class_enrollments` → `system_audit_logs` → `system_settings` →
các bảng read model (mục 1.12).

**Tài khoản `ADMIN` đầu tiên (bổ sung 2026-10-23, `DEC-2026-1023-db-actor-review-fixes`, ponytail bậc 6):** màn quản lý người dùng không tạo được `ADMIN` (`01-rd/req/F1_identity.md:72-75`), nên nếu không có đường khác thì triển khai xong không ai vào được khu quản trị. Đường duy nhất là **seed lúc khởi động ứng dụng**, không phải migration SQL: khi bảng `users` **chưa có người dùng nào gán role `ADMIN`**, một bước khởi động (runner của `algoprep-identity`) tạo một tài khoản `ADMIN` từ hai biến môi trường `ALGOPREP_BOOTSTRAP_ADMIN_EMAIL` và `ALGOPREP_BOOTSTRAP_ADMIN_PASSWORD` (tên biến `[SoT: Suy luận]`, DD chốt). Quy tắc: (1) **không đặt mật khẩu trong file migration** để không lộ băm mật khẩu trong kho mã; (2) tài khoản khởi tạo giống luồng Admin tạo (F1-13): `status = PENDING_VERIFICATION`, `must_change_password = true`, chuyển `ACTIVE` ở lần đăng nhập đầu bằng mật khẩu này (mục 1.1); (3) chạy lại khi đã có `ADMIN` thì **không làm gì** (idempotent), nên đổi biến môi trường sau đó không đổi mật khẩu; (4) **không ghi `system_audit_logs`** vì `actor_user_id` NOT NULL và không có người thao tác (cùng lý do job ẩn danh hoá, mục 1.11); (5) thiếu một trong hai biến khi chưa có `ADMIN` thì ứng dụng **dừng khởi động** kèm thông báo rõ, không chạy với hệ thống không có quản trị viên. Không thêm cột hay bảng.

## 5. Việc còn mở — chuyển sang DD

- ~~Payload chính xác của domain event tiêu thụ (mục 1.12)~~ — **đã đóng 2026-10-29:** nằm ở `03-dd/api/F1_identity.md:385-392` (phong bì và payload tối thiểu theo từng `eventType`).
- ~~Cấu trúc `before_json`/`after_json` của `system_audit_logs` theo từng loại hành động~~ — **đã đóng 2026-10-29 (chủ dự án chốt):** giữ snapshot tự do như mục 1.11, không chuẩn hoá theo loại hành động. Tham số hiển thị lấy từ `after_json` qua `messageKey` của `AuditAction` (`03-dd/logic/F1_identity.md` BR-19), khoá nhạy cảm bị che theo BR-16.
- **Đã đóng 2026-10-11:** trạng thái "Chờ xác thực" (mục 1.1), bảng ánh xạ `action_type` (mục 1.11), bảng cấu hình lưu log (mục 1.13). Còn lại cho DD: bản nén "Nén và lưu trữ" (hoãn).
- **Đã đóng 2026-10-22 (phase 2, DEC-2026-1022-identity-schema-sync):** nguồn trạng thái khoá (`deactivation_source`), đổi mật khẩu bắt buộc (`must_change_password`), OTP xác thực email (khoá Redis), tiến độ lớp tính khi đọc, nguồn số hoạt động `ADM0101`, bảng `user_preferences`.
- **Việc đồng bộ ở tài liệu khác (không thuộc file này):** DD logic BR-08 (thêm `deactivation_source`, xoá `oauth_identities`, đặt `SELF` ở bước tự xoá) và BR-12/endpoint 29 (bỏ read model lớp); DD logic BR-18 (`:261`) còn `CLASS_HAS_ABSENT_STUDENTS`; BD `USR0503` đổi tên bảng; BD `ADM0101` đóng Q2; BD kiến trúc identity nêu nguồn khoá; ~~RD thêm mã F1-nn cho xác thực email và luồng Admin tạo tài khoản buộc đổi mật khẩu~~ — đã có F1-33, F1-34 (`01-rd/req/F1_identity.md:101, 113`); **Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4):** xoá việc tồn đọng này.
- **Đã chốt 2026-10-22:** `top_language` cập nhật bằng event kèm `language_counts`; tập `activity_type` không có sự kiện tham gia lớp (RD `01-rd/req/F1_identity.md:224-226` phải sửa cho khớp); ẩn danh hoá đặt NULL ba trường tự khai và xoá `oauth_identities`. **Đã chốt 2026-10-29 (chủ dự án xác nhận mặc định):** `best_verdict` VARCHAR(32), dùng lại giá trị của `judge.submissions.status` (mục 1.12).
- ~~Có cần bảng `role` tuỳ biến thật hay chỉ 3 role cố định~~ — **đã đóng:** cần bảng `roles` thật, role tuỳ biến tạo/sửa/xoá được (`01-rd/req/F1_identity.md:54`, `02-bd/architecture/F1_identity.md:98-104`). **Sửa 2026-10-22 (đồng bộ DB, phase 1):** xoá câu hỏi mở lỗi thời.
