# BD — Kiến trúc module `identity` (F1)

> Trạng thái: **BD lần đầu**, 2026-09-12. Module đầu tiên theo thứ tự
> `06-plan/260912-1055-bd-rollout-order.md`.

## 1. Vị trí trong kiến trúc

Module Maven `algoprep-identity`, schema PostgreSQL `identity`
[SoT: `.nexa/domain-registry.json` — domains[0]]. Bốn tầng theo baseline chung của toàn hệ thống
(`DEC-2026-0820-architecture-baseline`): `domain` (aggregate, value object, outbound port tại
`domain/ports/out`) / `application` (inbound port tại `application/ports/in`, một interface một
phương thức, cộng `command/` `query/` `dto/`) / `infrastructure` / `presentation`. Không có
`domain/repository/`, không có `application/usecase/`.

Hạ tầng phụ thuộc: PostgreSQL + Redis (blacklist refresh-token, rate limiting)
[SoT: `.nexa/control/dependency-map.md` mục 2]. **Không dùng MinIO** — `identity` không lưu file lớn,
nên `02-bd/storage/F1_identity.md` không được tạo (skill `bd-generation` Layer 3: "không phải module nào
cũng cần cả 4 file, xác nhận theo RD, không viết file rỗng").

## 2. Phạm vi nghiệp vụ module này bao trùm

Đọc từ `01-rd/req/F1_identity.md` (F1-01 → F1-32; sửa 2026-10-22, đồng bộ DB phase 3, ID-39: trước ghi "F1-01 → F1-30", RD có thêm F1-31 dashboard luyện tập của người học `01-rd/req/F1_identity.md:234` và F1-32 lý do khoá tài khoản `:76`):

| Nhóm | Mã | Ghi chú |
| :--- | :--- | :--- |
| Đăng ký/đăng nhập/đăng xuất | F1-01→04 | Email+mật khẩu |
| OAuth | F1-15 | GitHub, Google — tự liên kết theo email |
| RBAC hai lớp | F1-05, F1-10→14 | Lớp 1 (học tập cơ bản, theo vai trò) tách biệt Lớp 2 (ma trận phân quyền quản trị/nội dung) |
| Quản lý hồ sơ | F1-09, F1-19, F1-20 | Sửa thông tin, đổi mật khẩu khi đã đăng nhập, cá nhân hoá workspace |
| Quên mật khẩu | F1-17 | OTP 6 số qua email, không áp dụng cho tài khoản chỉ đăng ký OAuth |
| Tự xoá tài khoản | F1-16 | Khoá mềm → ẩn danh hoá sau ân hạn. Sửa 2026-10-22 (đồng bộ DB, phase 3, `DEC-2026-1022-identity-schema-sync` mục 1): cột `users.deactivation_source` (`SELF`/`ADMIN`) phân biệt hai nguồn `status = DEACTIVATED`; job ẩn danh hoá **chỉ quét `SELF`** quá ân hạn 30 ngày, tài khoản Admin khoá không bao giờ bị job ẩn danh (`database/F1_identity.md:28-32`; DD `03-dd/logic/F1_identity.md` BR-08 bước 1 đặt `SELF`/`ADMIN`) |
| Quản lý người dùng (ADMIN) | F1-13, F1-32 | Đổi vai trò, khoá/mở khoá (khoá ghi lý do, tuỳ chọn gửi email — F1-32), reset mật khẩu hộ. Sửa 2026-10-22 (phase 3, mục 2 của DEC): Admin tạo tài khoản đặt `users.must_change_password = true` để buộc đổi mật khẩu ở lần đăng nhập đầu (`database/F1_identity.md:26`). **Sửa vòng 2 (mục 4 của DEC `DEC-2026-1022-db-sync-round2`):** buộc đổi là mềm, chỉ giao diện chuyển hướng theo cờ ở phản hồi đăng nhập/`GET /me`, máy chủ không chặn endpoint khác |
| Xác thực email | F1-33 (đề xuất) | Sửa 2026-10-22 (phase 3, mục 3 của DEC): tài khoản `PENDING_VERIFICATION` **tự đăng ký** (F1-01) xác thực bằng OTP email, **Sửa 2026-10-22 (đồng bộ DB vòng 2, DEC-2026-1022-db-sync-round2 mục 1):** ~~tự đăng ký hoặc Admin tạo~~ — tài khoản Admin tạo (F1-13) cũng khởi tạo `PENDING_VERIFICATION` nhưng không có OTP, chuyển `ACTIVE` ở lần đăng nhập thành công đầu tiên bằng mật khẩu tạm;  cùng cơ chế F1-17 (khoá Redis `identity:emailverify:<userId>`, hạn 10 phút, `database/F1_identity.md:220`); xác thực đúng → `ACTIVE`. RD đã thêm F1-33 (OTP) và F1-34 (buộc đổi mật khẩu) dạng đề xuất `01-rd/req/F1_identity.md:101,111` |
| Lớp học | F1-23→28 | Tạo/xoá/sửa lớp, mã mời, gỡ học viên (đánh dấu đã rời lớp, giữ lịch sử làm bài — `DEC-2026-1014-keep-learning-history-on-leave`), chi tiết học viên, dashboard tiến độ lớp |
| Nhật ký hệ thống | F1-14 | Chỉ hành động quản trị của người, không gộp sự kiện hạ tầng (đó là F4-10) |
| Tiến độ cá nhân | F1-06, F1-07, F1-08 | Bài giải theo chủ đề, tỉ lệ Accepted, lịch sử phỏng vấn |
| Dashboard tổng quan | F1-29 (`admin_overview`), F1-30 (`instructor_overview`) | Đọc chéo dữ liệu module khác qua domain event |
| Thông báo, xuất dữ liệu | F1-21, F1-22 | Email định kỳ, xuất CSV/JSON dữ liệu cá nhân |

## 3. Giao tiếp liên module

Theo `DEC-2026-0820-architecture-baseline` + `.nexa/control/dependency-map.md` mục 1: module không
import lẫn nhau. `identity` dùng hai kênh:

### 3.1. Consume domain event (xây read model nội bộ)

`identity` không đọc trực tiếp schema `judge` hay `ai` — đó là phá vỡ ranh giới module dù chung một
instance Postgres. Thay vào đó, `identity` lắng nghe sự kiện domain và cập nhật read model của chính
mình:

| Sự kiện lắng nghe | Từ module | Dùng để cập nhật | Phục vụ |
| :--- | :--- | :--- | :--- |
| `SubmissionGraded` (verdict + tỉ lệ testcase F4-13; vòng 2: thêm `submittedAt` UTC và `firstAttemptForProblem`) | `judge-orchestration` | `user_problem_best_score`, `user_submission_stats`, `user_daily_submission_stats` | F1-06 (best-attempt theo bài), F1-07 (tỉ lệ Accepted), lưới hoạt động 12 tháng của USR0601 qua `GET /me/activity-calendar` (DEC `DEC-2026-1022-db-sync-round2` mục 3, 6) |
| `SolutionReviewCompleted` / `MockInterviewFinished` | `ai-review` | `identity_recent_activity` (read model của F1-30) | F1-08 (lịch sử phỏng vấn mở lại rubric), F1-30 ("Hoạt động gần đây") |
| `SubmissionAccepted` | `judge-orchestration` | `identity_recent_activity` | F1-29, F1-30 |
| Sự kiện tạo/nộp bài từ `problem-bank` (đề xuất, tên cụ thể chốt ở DD) | `problem-bank` | `identity_recent_activity` | F1-30 |

`[SoT: Suy luận]` — tên sự kiện cụ thể (payload, versioning) chốt ở `03-dd/api/F1_identity.md` khi vào DD;
BD chỉ chốt **có kênh event, không có import trực tiếp**.

### 3.1b. Sự kiện `identity` phát ra (producer)

**Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4):** bảng dưới liệt kê sự kiện do `identity` **phát**, không tiêu thụ. Payload chi tiết ở `03-dd/api/F1_identity.md` mục 2.

| Sự kiện phát ra | Khi nào | Người nhận | Hệ quả |
| :--- | :--- | :--- | :--- |
| `StudentRemovedFromClass { classId, studentId, occurredAt }` | Giảng viên gỡ học viên khỏi lớp (F1-26), BR-11 | Không module nào phải xử lý dữ liệu (`DEC-2026-1014-keep-learning-history-on-leave`) | Chỉ `class_enrollments.left_at` đổi |
| `ClassDeleted { classId, occurredAt }` | Giảng viên xoá cứng lớp (F1-24), BR-22 | `problem-bank` đặt `removed_at = now()` cho các dòng `class_assignments` của lớp | `class_invite_codes`, `class_enrollments` đi theo bằng CASCADE; `identity_recent_activity.scope_class_id` về NULL bằng SET NULL (`02-bd/database/F1_identity.md` mục 1.8, 1.12) |

`ClassDeleted` thuộc `DEC-2026-1022-db-sync-round4` mục 3 (tiêu chí ponytail bậc 6: không để tham chiếu treo sau xoá cứng). Phát sau khi giao dịch xoá commit; cơ chế phát đi theo BR-22 (cùng kiểu `StudentRemovedFromClass`, chưa có outbox riêng cho `identity`).

### 3.2. Cung cấp outbound cho module khác gọi vào (bị gọi, không phải identity gọi ra)

`identity` là bên bị các module khác cần xác thực/phân quyền — không tự nó cần gọi ra module khác để
hoàn thành nghiệp vụ lõi của chính nó (đăng ký, đăng nhập không phụ thuộc module nào khác). Sửa 2026-10-22 (đồng bộ DB, phase 3, XS-05): ngoại lệ ở phần **hiển thị** (không phải nghiệp vụ lõi) — dashboard `USR0601` có phụ thuộc đọc qua outbound port của `identity` sang `problem-bank`, `judge-orchestration`, `ai-review` và `interview-bank` (F6-10: `practice_history`, `question_topics`), không truy vấn chéo schema (`02-bd/screens/users/USR0601_dashboard.md:50,786-796 (số dòng sửa 2026-10-22 vòng 2, trước ghi 782-791)`; phía `interview-bank`: `architecture/F6_interview-bank.md` mục 3.3). Việc kiểm
JWT ở các module khác dùng thư viện/filter đọc claim từ Access Token (`algoprep-common`). Với quyền Lớp 2 `Function:Action`, filter gọi cổng phân quyền trong tiến trình của `identity`: cổng đọc `roles.permission_version` từ PostgreSQL primary trước khi dùng cache ma trận. Đây là truy vấn đồng bộ mỗi request có gác quyền Lớp 2, không phải HTTP liên dịch vụ; quyền học tập Lớp 1 chỉ đọc claim như trước. Quy tắc này ưu tiên bảo đảm thu hồi quyền ngay sau PATCH thành công (`DEC-2026-1028-qa-f1-permission-immediate`).

### 3.3. Cổng vào `AuditLogPort` (bổ sung 2026-10-23, `DEC-2026-1023-db-actor-review-decisions`)

Các module khác (`problem-bank`, `interview-bank`, `judge-orchestration`) có thao tác quản trị cần ghi nhật ký F1-14 nhưng **không được ghi trực tiếp vào schema `identity`**. `identity` cung cấp một cổng vào (inbound port) tên `AuditLogPort` trong `algoprep-identity`:

`record(actorUserId, actionType, targetType, targetId, beforeJson, afterJson)` → một dòng `identity.system_audit_logs`.

| Nơi gọi | Thao tác quản trị cần ghi |
| :--- | :--- |
| `problem-bank` | quản lý `topics`, `problem_levels` |
| `interview-bank` | quản lý `topics`, `levels` của ngân hàng câu hỏi |
| `judge-orchestration` | ghi cấu hình admin: `language_configs`, `grading_defaults`, `sandbox_settings`, `generator_sandbox_settings`, `cluster_settings` |

Quy tắc: (1) gọi **trong cùng JVM và cùng giao dịch DB** với thay đổi nghiệp vụ, nên thay đổi và dòng nhật ký cùng commit hoặc cùng rollback; (2) module gọi chỉ biết cổng, không biết tên bảng; (3) che dữ liệu nhạy cảm ở tầng đọc nhật ký (DD `03-dd/logic/F1_identity.md` BR-16), không ở nơi gọi.

**Ngoại lệ không ghi nhật ký:** runner khởi tạo `ADMIN` đầu tiên lúc khởi động ứng dụng (`02-bd/database/F1_identity.md` mục 4) và job ẩn danh hoá, vì `actor_user_id` NOT NULL và không có người thao tác. Runner này nằm trong `algoprep-identity`, chạy một lần khi khởi động, idempotent; đặc tả ở `03-dd/jobs/F1_identity.md`.

## 4. Ma trận phân quyền — mô hình dữ liệu (chi tiết ở `02-bd/database/F1_identity.md`)

`01-rd/req/F1_identity.md` dòng 51 ghi rõ: *"Function và Action là dữ liệu seed cố định, chỉ đọc trên
giao diện; **Role tạo/sửa/xoá được**"* — khác với cách đọc thông thường rằng chỉ có đúng ba vai trò cố
định. BD tách hai khái niệm để không mâu thuẫn với F1-05 ("ba vai trò"):

- **`base_category`** (`STUDENT`/`INSTRUCTOR`/`ADMIN`, cố định, không tạo/xoá được) — quyết định Lớp 1
  (quyền học tập cơ bản, không đi qua ma trận) và khu vực giao diện gốc (`F1_identity.md` dòng 12-25).
- **`role`** (thực thể có thể tạo/sửa/xoá qua F1-10/F1-11, mỗi role gắn với đúng một `base_category`)
  — là đơn vị được gán quyền trong ma trận Role × Function × Action. Ba role hệ thống
  (`STUDENT`/`INSTRUCTOR`/`ADMIN`) được seed sẵn, `is_system = true`, không xoá được và không xoá được
  khi đang có người dùng gán (đúng câu chữ RD dòng 54). Role tuỳ biến thêm (ví dụ "Trợ giảng") vẫn phải
  chọn một `base_category` — không có Lớp 1 riêng ngoài ba loại đã định.

**Đã xác nhận 2026-09-13 — role tuỳ biến là yêu cầu thật, không phải suy diễn.** Câu hỏi ban đầu (có thực
sự cần role tuỳ biến ngoài ba role hệ thống, hay câu "Role tạo/sửa/xoá được" chỉ nói ADMIN sửa tên hiển
thị) đã đóng bằng bằng chứng ở tầng RD màn hình: `01-rd/screens/admin/ADM0202_permission_matrix.md` dòng
32-34 tả rõ "Tab chọn Role — STUDENT/INSTRUCTOR/ADMIN, nút '+ Vai trò mới', nút 'Xoá vai trò này' (ẩn với
vai trò hệ thống)"; dòng 48 có Given-When-Then "Cho quản trị viên bấm '+ Vai trò mới' và đặt tên, Khi xác
nhận tạo, Thì vai trò mới xuất hiện...". Thiết kế bảng `roles` mở rộng được ở mục 5 file `database` giữ
nguyên, không cần đổi. Xem `07-review/bd_open_questions_260913.md` mục 2 câu #1.

## 5. Giá trị mặc định BD chốt (RD để ngỏ, đánh dấu `[SoT: Suy luận]` ở RD, BD nhận giá trị đề xuất)

| Tham số | Giá trị | Nguồn RD |
| :--- | :--- | :--- |
| Access Token TTL | 15 phút | `01-rd/req/F1_identity.md` F1-02 (RD không nêu số, BD chọn) |
| Refresh Token TTL | 30 ngày, xoay vòng (rotate) mỗi lần dùng | như trên |
| OTP quên mật khẩu — hạn dùng | 10 phút | `F1_identity.md` dòng 209 (RD đã đề xuất số) |
| OTP quên mật khẩu — số lần sai tối đa | 5 lần | `F1_identity.md` dòng 212 |
| OTP quên mật khẩu — cooldown gửi lại | 60 giây | `F1_identity.md` dòng 213 |
| Ân hạn trước khi ẩn danh hoá tài khoản tự xoá | 30 ngày, tính từ `deactivated_at`; **chỉ áp cho `deactivation_source = SELF`**, tài khoản `ADMIN` khoá không bao giờ bị ẩn danh (`02-bd/database/F1_identity.md` mục 1.1; sửa 2026-10-23, `DEC-2026-1023-db-actor-review-fixes`) | `F1_identity.md` dòng 199 |
| Mã mời lớp học — hạn dùng | 7 ngày, cố định, không có ô nhập hạn | `01-rd/req/F1_identity.md:121-124` (F1-25). Sửa 2026-10-22 (đồng bộ DB, phase 3, ID-39): trước ghi "RD chưa nêu số, BD đề xuất mới" — RD đã chốt 7 ngày. Định dạng mã `VARCHAR(8)`, chữ hoa và số bỏ `0`/`O`/`1`/`I`, máy chủ sinh (`DEC-2026-1022-identity-schema-sync` mục 7; `database/F1_identity.md:115`) |
| "Người dùng hoạt động" (admin_overview) | đăng nhập trong 30 ngày gần nhất | `F1_identity.md` dòng 148 |
| "Hoạt động gần đây" (instructor_overview) | giữ 90 ngày, hiển thị 10 mục gần nhất | `F1_identity.md` dòng 181-182 (RD để BD quyết định) |

## 6. Ràng buộc bảo mật/thiết kế mang sang từ RD (chi tiết ở `02-bd/security/F1_identity.md`)

- Kiểm quyền ở tầng ứng dụng, không chỉ giao diện; kiểm cả quyền sở hữu dữ liệu (`F1_identity.md` dòng 13).
- Đổi ô ma trận phân quyền có hiệu lực ngay, không cần khởi động lại dịch vụ (`F1_identity.md` dòng 50) —
  vì vậy quyền không được nhúng vào claim JWT tĩnh, phải tra tại thời điểm request (cache + invalidation,
  không phải bake vào token).
- Không tiết lộ email có tồn tại hay không ở luồng quên mật khẩu (`F1_identity.md` dòng 214-217, OWASP user
  enumeration).
- F1-14: nhật ký hệ thống chỉ ghi hành động quản trị của người, tách khỏi sự kiện hạ tầng của F4
  (`F1_identity.md` dòng 70-75).

## 7. Việc còn mở — chuyển sang DD

- Tên sự kiện domain cụ thể và payload (mục 3.1).
- Chữ ký outbound port nếu phát sinh (hiện tại BD không thấy `identity` cần gọi ra module nào để hoàn
  thành nghiệp vụ lõi của mình; ngoại lệ đọc cho dashboard `USR0601`, mục 3.2).
- Ngưỡng phân loại trạng thái học viên ("Đang tốt"/"Cần hỗ trợ"/"Vắng bài") ở F1-27 — RD để ngỏ
  (`F1_identity.md` dòng 108-111), không có screen nào cho số cụ thể. BD đề xuất: "Vắng bài" = không nộp bài
  trong 7 ngày gần nhất; "Cần hỗ trợ" = tỉ lệ Accepted giảm liên tục qua ≥2 mốc tuần gần nhất; "Đang tốt" =
  còn lại — chốt số thật ở DD, không chặn BD màn hình.

## 8. Tham chiếu

- `01-rd/req/F1_identity.md` — đặc tả F1-01 → F1-30.
- `.nexa/domain-registry.json` — domain `identity`, scope liệt kê bảng dữ liệu dự kiến.
- `.nexa/control/dependency-map.md` mục 2, 4 — hạ tầng phụ thuộc, thứ tự build (stage 1 cùng `problem-bank`).
- `.nexa/control/decision-registry.md` — `DEC-2026-0820-architecture-baseline`,
  `DEC-2026-0831-partial-score-testcase-ratio` (ảnh hưởng F1-06/F1-07).
- `06-plan/260912-1055-bd-rollout-order.md` — thứ tự triển khai BD.
