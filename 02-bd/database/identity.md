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
| `current_position` | VARCHAR nullable | Trường hồ sơ "Vai trò hiện tại" (F1-09) — **chức danh tự khai**, không phải RBAC role, tránh nhầm với `role_id`. **Sửa 2026-10-18 (`DEC-2026-1018-usr-open-questions-phase2-5`):** chỉ còn mang vai trò hiện tại; ~~trước đây ghi chú nói cột này phục vụ cả "Vị trí mục tiêu"~~, phần đó chuyển sang `target_position` |
| `target_position` | VARCHAR nullable | **Cột MỚI, 2026-10-18, `DEC-2026-1018-usr-open-questions-phase2-5`** (ponytail bậc 1). Hồ sơ "Vị trí mục tiêu" (F1-09), chức danh tự khai, không phải RBAC role. Ẩn danh hoá (F1-16) không bắt buộc ghi đè (không phải dữ liệu định danh) `[SoT: Suy luận]` |
| `password_changed_at` | TIMESTAMPTZ nullable | **Cột MỚI, 2026-10-18, `DEC-2026-1018-usr-open-questions-phase2-5`.** `NULL` = chưa từng đổi mật khẩu kể từ khi tạo tài khoản; đặt `now()` ở **mọi** lần đổi mật khẩu và lần đặt mật khẩu đầu tiên (tài khoản OAuth chưa có `password_hash`). Phục vụ dòng "đổi mật khẩu lần cuối" ở hồ sơ. Không dùng để vô hiệu token: đổi mật khẩu thu hồi mọi refresh token khác qua `refresh_tokens.revoked_at` (giữ phiên hiện tại) |
| `school_or_company` | VARCHAR nullable | F1-09 |
| `phone` | VARCHAR(20) nullable | F1-09, thêm 2026-10-13 (`DEC-2026-1013-a2-reads-student-contact`). Số điện thoại tự khai, không bắt buộc, không xác thực. Chỉ chủ tài khoản sửa; A2 đọc được khi học viên thuộc lớp mình phụ trách. Ẩn danh hoá (F1-16) ghi đè thành NULL |
| `default_language` | VARCHAR | F1-09, F1-20 — ngôn ngữ nộp bài mặc định (Java/C++/Python) |
| `status` | ENUM(`ACTIVE`,`DEACTIVATED`,`PENDING_VERIFICATION`) | F1-16, F1-13 (ADMIN khoá/mở khoá). **Thêm 2026-10-11 (`DEC-2026-1011-admin-system-open-questions-closed`):** `PENDING_VERIFICATION` = đã tạo (tự đăng ký F1-01 hoặc Admin tạo F1-13) nhưng chưa xác thực email; chuyển sang `ACTIVE` khi xác thực. Không thêm cột `email_verified`, không dùng khoá Redis TTL vì không liệt kê hay lọc được |
| `last_active_at` | TIMESTAMPTZ nullable | Ghi nhận mốc hoạt động gần nhất (đăng nhập/nộp bài), cập nhật throttle theo phút, phục vụ ADM0201 (`DEC-2026-0922-users-and-admin-conflict-resolutions`) |
| `deactivated_at` | TIMESTAMPTZ nullable | Mốc bắt đầu ân hạn 30 ngày (F1-16) |
| `anonymized_at` | TIMESTAMPTZ nullable | Job định kỳ set khi ẩn danh hoá xong |
| `created_at` / `updated_at` | TIMESTAMPTZ | |

Khi ẩn danh hoá (job định kỳ, F1-16): `email`, `display_name` được ghi đè bằng giá trị vô danh, `phone` đặt NULL
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

`(id, instructor_id FK users, name, description, schedule jsonb NULL, created_at, updated_at)`. Xoá là
**hard delete** (F1-24) — không có cột `deleted_at`. **Đổi 2026-10-11:** cột `schedule_note` (text) thành
`schedule` (jsonb), cấu trúc ở mục 1.14 (`DEC-2026-1011-class-schedule-structured`).

### 1.9. `class_invite_codes` (F1-25)

`(id, class_id FK, code unique, expires_at, created_by FK user, created_at)`. Nhiều mã cùng hiệu lực
song song cho một lớp (`identity.md` dòng 90-97) — không có cột "mã hiện hành duy nhất".

### 1.10. `class_enrollments` (F1-23 tham gia, F1-26 gỡ)

`(id, class_id FK, student_id FK, joined_at, left_at TIMESTAMPTZ NULL)`, unique `(class_id, student_id)`.

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
  hai); `joined_at` giữ nguyên; cài đặt bằng `ON CONFLICT (class_id, student_id) DO UPDATE SET left_at = NULL`, không dùng `DO NOTHING` (xem BR-09 ở `03-dd/logic/identity.md`). ~~Bài nộp trong khoảng đã rời thuộc phạm vi cần chủ dự án chốt `[SoT: Suy luận — decision-registry chưa nói về việc tham gia lại]`~~ (phần "tham gia lại" đã chốt). ~~Phần bài nộp làm trong khoảng đã rời còn mở: sau khi vào lại thì `left_at` = NULL nên khoảng đã rời không còn thể hiện trong dòng này.~~ **ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-instructor-submissions-list-details):** chấp nhận khoảng thời gian đã rời **tính là "trong lớp"** khi xét quyền đọc bài nộp (giữ `joined_at` cũ, `left_at` về NULL); không thêm bảng lịch sử từng đoạn tham gia. Quy tắc cửa sổ ở `INS0206` và `USR0201`: `submitted_at >= joined_at` và (`left_at IS NULL` hoặc `submitted_at <= left_at`) [Nguồn: .nexa/control/decision-registry.md:3987-3989].

### 1.11. `system_audit_logs` (F1-14)

`(id, actor_user_id FK, action_type, target_type, target_id, before_json, after_json, created_at)`.
Chỉ ghi hành động quản trị của người (đổi ma trận quyền, đổi vai trò, khoá/mở khoá, reset mật khẩu, tạo
role tuỳ biến...) — không ghi sự kiện hạ tầng (đó là `judge-orchestration`, F4-10).

**Bổ sung 2026-10-01 (`DEC-2026-1001-admin-configurable-settings`), loại `action_type` thứ nhất do người dùng tự kích hoạt:**
`PASSWORD_RESET_REQUESTED` ("yêu cầu đặt lại mật khẩu", nhóm phân loại hiển thị **Xác thực**,
`02-bd/screens/admin/ADM0403_system_log.md`). Ghi một dòng mỗi lần hệ thống **phát hành** một mã OTP quên mật
khẩu (cùng thời điểm tạo khoá Redis `identity:pwreset:<userId>`, mục 3 dưới), với `actor_user_id` = `target_id`
= `users.id` của tài khoản được đặt lại, `target_type = 'USER'`, `before_json`/`after_json` rỗng. **Không bao
giờ lưu mã OTP**, không lưu email hay mật khẩu. Yêu cầu cho email không khớp tài khoản nào không ghi dòng
(không có `users.id` để làm tác nhân, và để khỏi lộ ra email nào tồn tại); thẻ ở trang tổng quan Admin vì vậy
đếm số **mã đã phát hành**, không đếm mọi lần bấm "Quên mật khẩu". Đây là mở rộng so với câu "chỉ ghi hành
động quản trị" ở trên: F1-14 gốc nói về hành động quản trị, còn loại này là sự kiện xác thực — thuộc phạm vi
quyết định đã nêu, nhưng RD F1-14 (`01-rd/req/identity.md:68-75`) chưa nhắc, cần mở rộng RD `[SoT: Suy luận]`.
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

- `user_problem_best_score(user_id, problem_id, best_ratio, best_verdict, updated_at)` — phục vụ F1-06
  best-attempt (`DEC-2026-0831-partial-score-testcase-ratio`).
- `user_submission_stats(user_id, total_submissions, accepted_count, first_try_accepted_count, updated_at)` — phục vụ F1-07 tỉ lệ
  Accepted và chỉ số "Đúng ngay lần đầu" trên USR0202/USR0501 (`DEC-2026-0922-users-and-admin-conflict-resolutions`).
- `user_daily_submission_stats(user_id, day DATE, submission_count INT, PRIMARY KEY (user_id, day))` — **Bảng MỚI, ĐỀ XUẤT 2026-10-18 (`DEC-2026-1018-usr-open-questions-phase2-5`, ponytail bậc 1 + 4).** Read model đếm lượt nộp theo ngày cho lưới hoạt động 12 tháng của `USR0601`. Tên bảng là **đề xuất, chốt ở DD**. Rà các bảng hiện có: `user_submission_stats` chỉ giữ tổng cộng dồn (không có chiều ngày) và `identity_recent_activity` bị dọn sau 90 ngày, nên **không tái dùng được**. Điền bởi **cùng domain event** nuôi `user_submission_stats` (upsert `submission_count + 1` tại `day` quy theo UTC+7, mục 1.15), nên không thêm đường tiêu thụ thứ hai. Lưới 12 tháng tối đa 366 dòng mỗi người; không có TTL, không bị bộ lọc khoảng ngày của màn hình ảnh hưởng
- `identity_recent_activity(id, actor_user_id, scope_class_id, activity_type, ref_type, ref_id, occurred_at)` — phục vụ
  F1-30 "Hoạt động gần đây", `scope_class_id` nullable phục vụ giảng viên lọc hoạt động học viên theo lớp trên INS0101 (`DEC-2026-0922-users-and-admin-conflict-resolutions`), giữ 90 ngày (job dọn định kỳ), hiển thị 10 mục gần nhất.
- `user_preferences(user_id PK, streak_reminder_enabled, weekly_report_enabled, preferred_interview_level)` — F1-21, mở rộng lưu thêm cấp độ phỏng vấn ưa thích (`INTERN`/`JUNIOR`/`MIDDLE`/`SENIOR`, `DEC-2026-0922-users-and-admin-conflict-resolutions`). Các tuỳ chọn editor (cỡ chữ, vim mode) được lưu tại `localStorage` trình duyệt theo nguyên tắc ponytail tinh gọn, không đẻ thêm bảng nháp hay bảng cấu hình editor.

## 2. Chỉ mục (index) đáng chú ý

- `users(email)` unique — tra cứu đăng nhập.
- `refresh_tokens(user_id, revoked_at)` — liệt kê phiên còn hiệu lực.
- `class_enrollments(student_id)` — "lớp của tôi" phía học viên.
- `class_enrollments(class_id) WHERE left_at IS NULL` — đếm và liệt kê học viên còn trong lớp (thêm 2026-10-14, `DEC-2026-1014-keep-learning-history-on-leave`) `[SoT: Suy luận]`.
- `identity_recent_activity(actor_user_id, occurred_at DESC)` — truy vấn 10 mục gần nhất của cá nhân.
- `identity_recent_activity(scope_class_id, occurred_at DESC) WHERE scope_class_id IS NOT NULL` — feed hoạt động lớp học (INS0101).
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

### 1.13. `system_settings` (cấu hình hệ thống do ADMIN đặt, bảng khoá-giá trị)

`(key PK, value_json JSONB, updated_at TIMESTAMPTZ, updated_by FK → users.id)`. Thêm 2026-10-11 theo
`DEC-2026-1011-admin-system-open-questions-closed` (H5 phần c, ponytail bậc 1 qua
`DEC-2026-1001-admin-configurable-settings`). Hiện có hai khoá:

| `key` | `value_json` | Mặc định | Ghi chú |
| :--- | :--- | :--- | :--- |
| `audit_log.retention_days` | số nguyên | `90` | Thời hạn lưu `system_audit_logs`; tối thiểu `1` |
| `audit_log.expired_policy` | `"DELETE"` hoặc `"ARCHIVE"` | `"DELETE"` | `ARCHIVE` (nén và lưu trữ) **hoãn** (bậc 3): job dọn coi như "giữ nguyên dòng", không xoá |

Mỗi lần sửa ghi một dòng `system_audit_logs` (phân loại "Cấu hình") với giá trị cũ và mới trong
`after_json`. Chỉ ghi qua `PUT /admin/settings/log-retention` (`03-dd/api/identity.md` mục 0.7.1).

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
`03-dd/validation/identity.md` mục 5. Không cần chỉ mục vì lịch chỉ đọc theo `classes.id`. Backend chưa có
bảng `classes` thật (`05-coding` chỉ có kiểu dữ liệu phía giao diện), nên không cần bước chuyển dữ liệu từ
`schedule_note`.

### 1.15. Thay đổi lược đồ 2026-10-18 (`DEC-2026-1018-usr-open-questions-phase2-5`) — danh sách cho người viết migration

Bảng 1.15.1 gom mọi cột hoặc bảng **mới** của quyết định này ở cả bốn module để tìm một chỗ.

| Đối tượng | Module / file BD | Loại | Ghi chú |
| :--- | :--- | :--- | :--- |
| `testcases.explanation_md` TEXT nullable | `problem-bank` (`02-bd/database/problem-bank.md` mục 1.6) | Cột mới | Chỉ có nghĩa khi `is_worked_example = true` |
| `submission_testcase_results.actual_output_snippet` TEXT nullable | `judge-orchestration` (`02-bd/database/judge-orchestration.md` mục 1.2) | Cột mới | Chỉ ghi khi testcase `SAMPLE` |
| `users.target_position` VARCHAR nullable | `identity` (mục 1.1) | Cột mới | `current_position` đổi nghĩa, không đổi kiểu |
| `users.password_changed_at` TIMESTAMPTZ nullable | `identity` (mục 1.1) | Cột mới | `NULL` = chưa từng đổi |
| `user_daily_submission_stats` | `identity` (mục 1.12) | Bảng mới, **tên đề xuất, chốt ở DD** | PK `(user_id, day)` |

Không phải cột mới (quy tắc hiển thị hoặc tính ở tầng ứng dụng): mã người dùng hiển thị = **8 ký tự đầu của
`users.id`** (UUID), không có cột; `overallScore`/`feedbackSummary` của phiên phỏng vấn tính từ `rubric_scores`
(`02-bd/database/ai-review.md`); ngôn ngữ nộp mặc định ở Cài đặt lấy từ `judge.language_configs.enabled = true`,
`users.default_language` giữ nguyên.

**Múi giờ cắt ngày:** mọi chỗ "ngày" trong `identity` (cột `day` của `user_daily_submission_stats`, chuỗi liên
tiếp ngày, biểu đồ, lưới 12 tháng) dùng một múi giờ hệ thống cố định **UTC+7**; cột thời gian vẫn là
`TIMESTAMPTZ`, quy đổi chỉ khi cắt ngày. Cùng quy tắc với `judge-orchestration`.

## 4. Migration — thứ tự tạo bảng

`roles` → `functions`/`actions` (seed) → `permissions` → `users` → `oauth_identities` →
`refresh_tokens` → `classes` → `class_invite_codes` → `class_enrollments` → `system_audit_logs` → `system_settings` →
các bảng read model (mục 1.12).

## 5. Việc còn mở — chuyển sang DD

- Payload chính xác của domain event tiêu thụ (mục 1.12) — `03-dd/api/identity.md`.
- Cấu trúc `before_json`/`after_json` của `system_audit_logs` theo từng loại hành động.
- **Đã đóng 2026-10-11:** trạng thái "Chờ xác thực" (mục 1.1), bảng ánh xạ `action_type` (mục 1.11), bảng cấu hình lưu log (mục 1.13). Còn lại cho DD: bản nén "Nén và lưu trữ" (hoãn).
- Có cần bảng `role` tuỳ biến thật hay chỉ 3 role cố định — xem câu hỏi mở ở
  `02-bd/architecture/identity.md` mục 4.
