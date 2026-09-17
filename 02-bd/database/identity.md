# BD — Database module `identity` (F1)

> Schema PostgreSQL: `identity`. Đọc cùng `02-bd/architecture/identity.md` mục 4 (mô hình `base_category`
> vs `role`) trước khi đọc bảng dưới đây — hai khái niệm không trùng nhau.

## 1. Bảng chính

### 1.1. `users`
Cốt lõi F1-01 → F1-09, F1-15, F1-16.

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `email` | VARCHAR unique | |
| `password_hash` | VARCHAR nullable | NULL nếu tài khoản chỉ tạo qua OAuth (F1-15) và chưa từng đặt mật khẩu |
| `display_name` | VARCHAR | |
| `role_id` | FK → `roles.id` | Xem mục 1.2 |
| `current_position` | VARCHAR nullable | Trường hồ sơ "Vị trí mục tiêu"/"Vai trò hiện tại" (F1-09) — **chức danh tự khai**, không phải RBAC role, tránh nhầm với `role_id` |
| `school_or_company` | VARCHAR nullable | F1-09 |
| `default_language` | VARCHAR | F1-09, F1-20 — ngôn ngữ nộp bài mặc định (Java/C++/Python) |
| `status` | ENUM(`ACTIVE`,`DEACTIVATED`) | F1-16, F1-13 (ADMIN khoá/mở khoá) |
| `deactivated_at` | TIMESTAMPTZ nullable | Mốc bắt đầu ân hạn 30 ngày (F1-16) |
| `anonymized_at` | TIMESTAMPTZ nullable | Job định kỳ set khi ẩn danh hoá xong |
| `created_at` / `updated_at` | TIMESTAMPTZ | |

Khi ẩn danh hoá (job định kỳ, F1-16): `email`, `display_name` được ghi đè bằng giá trị vô danh
(`deleted-user-<id>@anon.local`, `"Người dùng đã xoá"`); **không xoá dòng**, không cascade xoá bài
nộp/phiên phỏng vấn — các bảng ở module khác giữ nguyên `user_id` để không vỡ thống kê lớp
(`identity.md` dòng 200-202).

### 1.2. `roles`
Hỗ trợ mô hình hai lớp ở mục 4 kiến trúc.

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `code` | VARCHAR unique | Ví dụ `STUDENT`, `INSTRUCTOR`, `ADMIN`, hoặc role tuỳ biến |
| `name` | VARCHAR | Tên hiển thị |
| `base_category` | ENUM(`STUDENT`,`INSTRUCTOR`,`ADMIN`) | Quyết định Lớp 1 + khu giao diện; role tuỳ biến vẫn phải chọn một giá trị |
| `is_system` | BOOLEAN | `true` cho 3 role seed — không xoá được (F1-11) |
| `created_at` | TIMESTAMPTZ | |

Seed cố định 3 dòng `is_system = true`: `STUDENT`/`INSTRUCTOR`/`ADMIN`, `base_category` trùng `code`.

### 1.3. `functions` / `actions` — dữ liệu seed cố định (F1-11)

`functions(id, code, name)` — 10 dòng seed theo F1-12: `PROBLEM_AUTHORING`, `TESTCASE_MANAGEMENT`,
`CLASS_MANAGEMENT`, `USER_MANAGEMENT`, `JUDGE_QUEUE_MONITOR`, `AI_CONFIG`, `AI_TOKEN_BUDGET`,
`SYSTEM_AUDIT_LOG`, `INTERVIEW_BANK_MANAGEMENT`, `PERMISSION_MATRIX`.

`actions(id, code)` — 4 dòng seed: `CREATE`, `READ`, `UPDATE`, `DELETE`.

Chỉ đọc trên giao diện (F1-11 dòng 51-53) — không có API ghi cho hai bảng này ngoài migration seed.

### 1.4. `permissions` — ô của ma trận (F1-10)

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `role_id` | FK → `roles.id` | |
| `function_id` | FK → `functions.id` | |
| `action_id` | FK → `actions.id` | |
| `granted` | BOOLEAN | |
| `updated_at`, `updated_by` | TIMESTAMPTZ, FK user | Ghi vào `system_audit_logs` mỗi lần đổi (F1-14) |

Unique `(role_id, function_id, action_id)`. Đổi một dòng → invalidate cache quyền ngay (mục bảo mật).

### 1.5. `oauth_identities` (F1-15)

`(id, user_id FK, provider ENUM(GITHUB,GOOGLE), provider_user_id, provider_email, linked_at)`,
unique `(provider, provider_user_id)`. Liên kết tự động vào `users` đã tồn tại nếu `provider_email`
khớp email hiện có (`identity.md` dòng 190-194).

### 1.6. `refresh_tokens` (F1-02 → F1-04)

`(id, user_id FK, token_hash, family_id, issued_at, expires_at, revoked_at)`. `family_id` phục vụ phát
hiện tái sử dụng khi rotate (mục bảo mật). Nguồn sự thật là bảng này; Redis chỉ giữ blacklist để tránh
tra Postgres mỗi request — xem mục 3.

### 1.7. `password_reset_requests` / `email_change_requests`

Ephemeral, TTL ngắn (10 phút) — đặt ở **Redis**, không phải bảng Postgres (mục 3), vì không cần tồn tại
lâu và không cần join quan hệ. Tái dùng chung cơ chế OTP 6 số (F1-17, F1-09 đổi email).

### 1.8. `classes` (F1-23, F1-24)

`(id, instructor_id FK users, name, description, schedule_note, created_at, updated_at)`. Xoá là
**hard delete** (F1-24) — không có cột `deleted_at`.

### 1.9. `class_invite_codes` (F1-25)

`(id, class_id FK, code unique, expires_at, created_by FK user, created_at)`. Nhiều mã cùng hiệu lực
song song cho một lớp (`identity.md` dòng 90-97) — không có cột "mã hiện hành duy nhất".

### 1.10. `class_enrollments` (F1-23 tham gia, F1-26 gỡ)

`(id, class_id FK, student_id FK, joined_at)`, unique `(class_id, student_id)`. Gỡ học viên (F1-26) là
**xoá thật kèm cascade** dữ liệu lịch sử làm bài **trong phạm vi lớp này** — cascade cụ thể (bảng nào ở
module khác) thuộc phạm vi DD liên module (`judge-orchestration` sở hữu lượt nộp), `identity` chỉ xoá
dòng `class_enrollments` và phát sự kiện `StudentRemovedFromClass` để `judge-orchestration` tự xử lý
phần dữ liệu của nó — tránh `identity` viết trực tiếp vào schema khác.

### 1.11. `system_audit_logs` (F1-14)

`(id, actor_user_id FK, action_type, target_type, target_id, before_json, after_json, created_at)`.
Chỉ ghi hành động quản trị của người (đổi ma trận quyền, đổi vai trò, khoá/mở khoá, reset mật khẩu, tạo
role tuỳ biến...) — không ghi sự kiện hạ tầng (đó là `judge-orchestration`, F4-10).

### 1.12. Read model tổng hợp từ domain event (mục 3.1 kiến trúc)

- `user_problem_best_score(user_id, problem_id, best_ratio, best_verdict, updated_at)` — phục vụ F1-06
  best-attempt (`DEC-2026-0831-partial-score-testcase-ratio`).
- `user_submission_stats(user_id, total_submissions, accepted_count, updated_at)` — phục vụ F1-07 tỉ lệ
  Accepted.
- `identity_recent_activity(id, actor_user_id, activity_type, ref_type, ref_id, occurred_at)` — phục vụ
  F1-30 "Hoạt động gần đây", giữ 90 ngày (job dọn định kỳ), hiển thị 10 mục gần nhất.
- `notification_preferences(user_id PK, streak_reminder_enabled, weekly_report_enabled)` — F1-21.

## 2. Chỉ mục (index) đáng chú ý

- `users(email)` unique — tra cứu đăng nhập.
- `refresh_tokens(user_id, revoked_at)` — liệt kê phiên còn hiệu lực.
- `class_enrollments(student_id)` — "lớp của tôi" phía học viên.
- `identity_recent_activity(actor_user_id, occurred_at DESC)` — truy vấn 10 mục gần nhất.
- `permissions(role_id)` — nạp toàn bộ quyền của một role khi cache miss.

## 3. Redis — key pattern

| Key | TTL | Dùng cho |
| :--- | :--- | :--- |
| `identity:refresh:blacklist:<jti>` | = thời hạn còn lại của token | Chặn refresh token đã bị revoke mà chưa hết hạn tự nhiên (rotate/logout) |
| `identity:pwreset:<userId>` | 10 phút | Mã OTP quên mật khẩu, kèm số lần thử sai |
| `identity:pwreset:resend:<userId>` | 60 giây | Chặn gửi lại mã quá nhanh |
| `identity:emailchange:<userId>` | 10 phút | Mã OTP đổi email (tái dùng cơ chế F1-17) |
| `identity:permcache:<roleId>` | Không TTL, invalidate chủ động khi `permissions` đổi | Cache ma trận quyền theo role, tránh join 3 bảng mỗi request |
| `identity:ratelimit:login:<email|ip>` | Cửa sổ trượt (đề xuất 15 phút) | Chống dò mật khẩu |

## 4. Migration — thứ tự tạo bảng

`roles` → `functions`/`actions` (seed) → `permissions` → `users` → `oauth_identities` →
`refresh_tokens` → `classes` → `class_invite_codes` → `class_enrollments` → `system_audit_logs` →
các bảng read model (mục 1.12).

## 5. Việc còn mở — chuyển sang DD

- Payload chính xác của domain event tiêu thụ (mục 1.12) — `03-dd/api/identity.md`.
- Cấu trúc `before_json`/`after_json` của `system_audit_logs` theo từng loại hành động.
- Có cần bảng `role` tuỳ biến thật hay chỉ 3 role cố định — xem câu hỏi mở ở
  `02-bd/architecture/identity.md` mục 4.
