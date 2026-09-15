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
nên `02-bd/storage/identity.md` không được tạo (skill `bd-generation` Layer 3: "không phải module nào
cũng cần cả 4 file, xác nhận theo RD, không viết file rỗng").

## 2. Phạm vi nghiệp vụ module này bao trùm

Đọc từ `01-rd/req/identity.md` (F1-01 → F1-30):

| Nhóm | Mã | Ghi chú |
| :--- | :--- | :--- |
| Đăng ký/đăng nhập/đăng xuất | F1-01→04 | Email+mật khẩu |
| OAuth | F1-15 | GitHub, Google — tự liên kết theo email |
| RBAC hai lớp | F1-05, F1-10→14 | Lớp 1 (học tập cơ bản, theo vai trò) tách biệt Lớp 2 (ma trận phân quyền quản trị/nội dung) |
| Quản lý hồ sơ | F1-09, F1-19, F1-20 | Sửa thông tin, đổi mật khẩu khi đã đăng nhập, cá nhân hoá workspace |
| Quên mật khẩu | F1-17 | OTP 6 số qua email, không áp dụng cho tài khoản chỉ đăng ký OAuth |
| Tự xoá tài khoản | F1-16 | Khoá mềm → ẩn danh hoá sau ân hạn |
| Quản lý người dùng (ADMIN) | F1-13 | Đổi vai trò, khoá/mở khoá, reset mật khẩu hộ |
| Lớp học | F1-23→28 | Tạo/xoá/sửa lớp, mã mời, gỡ học viên, chi tiết học viên, dashboard tiến độ lớp |
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
| `SubmissionGraded` (verdict + tỉ lệ testcase F4-13) | `judge-orchestration` | `user_problem_best_score`, `user_submission_stats` | F1-06 (best-attempt theo bài), F1-07 (tỉ lệ Accepted) |
| `SolutionReviewCompleted` / `MockInterviewFinished` | `ai-review` | `identity_recent_activity` (read model của F1-30) | F1-08 (lịch sử phỏng vấn mở lại rubric), F1-30 ("Hoạt động gần đây") |
| `SubmissionAccepted` | `judge-orchestration` | `identity_recent_activity` | F1-29, F1-30 |
| Sự kiện tạo/nộp bài từ `problem-bank` (đề xuất, tên cụ thể chốt ở DD) | `problem-bank` | `identity_recent_activity` | F1-30 |

`[SoT: Suy luận]` — tên sự kiện cụ thể (payload, versioning) chốt ở `03-dd/api/identity.md` khi vào DD;
BD chỉ chốt **có kênh event, không có import trực tiếp**.

### 3.2. Cung cấp outbound cho module khác gọi vào (bị gọi, không phải identity gọi ra)

`identity` là bên bị các module khác cần xác thực/phân quyền — không tự nó cần gọi ra module khác để
hoàn thành nghiệp vụ lõi của chính nó (đăng ký, đăng nhập không phụ thuộc module nào khác). Việc kiểm
JWT + RBAC ở các module khác dùng chung một thư viện/filter đọc claim từ Access Token
(`algoprep-common`), không phải một lời gọi đồng bộ tới `identity` mỗi request — tránh biến `identity`
thành điểm nghẽn đồng bộ cho toàn hệ thống.

## 4. Ma trận phân quyền — mô hình dữ liệu (chi tiết ở `02-bd/database/identity.md`)

`01-rd/req/identity.md` dòng 51 ghi rõ: *"Function và Action là dữ liệu seed cố định, chỉ đọc trên
giao diện; **Role tạo/sửa/xoá được**"* — khác với cách đọc thông thường rằng chỉ có đúng ba vai trò cố
định. BD tách hai khái niệm để không mâu thuẫn với F1-05 ("ba vai trò"):

- **`base_category`** (`STUDENT`/`INSTRUCTOR`/`ADMIN`, cố định, không tạo/xoá được) — quyết định Lớp 1
  (quyền học tập cơ bản, không đi qua ma trận) và khu vực giao diện gốc (`identity.md` dòng 12-25).
- **`role`** (thực thể có thể tạo/sửa/xoá qua F1-10/F1-11, mỗi role gắn với đúng một `base_category`)
  — là đơn vị được gán quyền trong ma trận Role × Function × Action. Ba role hệ thống
  (`STUDENT`/`INSTRUCTOR`/`ADMIN`) được seed sẵn, `is_system = true`, không xoá được và không xoá được
  khi đang có người dùng gán (đúng câu chữ RD dòng 54). Role tuỳ biến thêm (ví dụ "Trợ giảng") vẫn phải
  chọn một `base_category` — không có Lớp 1 riêng ngoài ba loại đã định.

**Đã xác nhận 2026-09-13 — role tuỳ biến là yêu cầu thật, không phải suy diễn.** Câu hỏi ban đầu (có thực
sự cần role tuỳ biến ngoài ba role hệ thống, hay câu "Role tạo/sửa/xoá được" chỉ nói ADMIN sửa tên hiển
thị) đã đóng bằng bằng chứng ở tầng RD màn hình: `01-rd/screens/admin/admin_permission_matrix.md` dòng
32-34 tả rõ "Tab chọn Role — STUDENT/INSTRUCTOR/ADMIN, nút '+ Vai trò mới', nút 'Xoá vai trò này' (ẩn với
vai trò hệ thống)"; dòng 48 có Given-When-Then "Cho quản trị viên bấm '+ Vai trò mới' và đặt tên, Khi xác
nhận tạo, Thì vai trò mới xuất hiện...". Thiết kế bảng `roles` mở rộng được ở mục 5 file `database` giữ
nguyên, không cần đổi. Xem `07-review/bd_open_questions_260913.md` mục 2 câu #1.

## 5. Giá trị mặc định BD chốt (RD để ngỏ, đánh dấu `[SoT: Suy luận]` ở RD, BD nhận giá trị đề xuất)

| Tham số | Giá trị | Nguồn RD |
| :--- | :--- | :--- |
| Access Token TTL | 15 phút | `01-rd/req/identity.md` F1-02 (RD không nêu số, BD chọn) |
| Refresh Token TTL | 30 ngày, xoay vòng (rotate) mỗi lần dùng | như trên |
| OTP quên mật khẩu — hạn dùng | 10 phút | `identity.md` dòng 209 (RD đã đề xuất số) |
| OTP quên mật khẩu — số lần sai tối đa | 5 lần | `identity.md` dòng 212 |
| OTP quên mật khẩu — cooldown gửi lại | 60 giây | `identity.md` dòng 213 |
| Ân hạn trước khi ẩn danh hoá tài khoản tự xoá | 30 ngày | `identity.md` dòng 199 |
| Mã mời lớp học — hạn dùng | 7 ngày | `identity.md` dòng 91-92 (RD chưa nêu số cụ thể, BD đề xuất mới) |
| "Người dùng hoạt động" (admin_overview) | đăng nhập trong 30 ngày gần nhất | `identity.md` dòng 148 |
| "Hoạt động gần đây" (instructor_overview) | giữ 90 ngày, hiển thị 10 mục gần nhất | `identity.md` dòng 181-182 (RD để BD quyết định) |

## 6. Ràng buộc bảo mật/thiết kế mang sang từ RD (chi tiết ở `02-bd/security/identity.md`)

- Kiểm quyền ở tầng ứng dụng, không chỉ giao diện; kiểm cả quyền sở hữu dữ liệu (`identity.md` dòng 13).
- Đổi ô ma trận phân quyền có hiệu lực ngay, không cần khởi động lại dịch vụ (`identity.md` dòng 50) —
  vì vậy quyền không được nhúng vào claim JWT tĩnh, phải tra tại thời điểm request (cache + invalidation,
  không phải bake vào token).
- Không tiết lộ email có tồn tại hay không ở luồng quên mật khẩu (`identity.md` dòng 214-217, OWASP user
  enumeration).
- F1-14: nhật ký hệ thống chỉ ghi hành động quản trị của người, tách khỏi sự kiện hạ tầng của F4
  (`identity.md` dòng 70-75).

## 7. Việc còn mở — chuyển sang DD

- Tên sự kiện domain cụ thể và payload (mục 3.1).
- Chữ ký outbound port nếu phát sinh (hiện tại BD không thấy `identity` cần gọi ra module nào để hoàn
  thành nghiệp vụ lõi của mình).
- Ngưỡng phân loại trạng thái học viên ("Đang tốt"/"Cần hỗ trợ"/"Vắng bài") ở F1-27 — RD để ngỏ
  (`identity.md` dòng 108-111), không có screen nào cho số cụ thể. BD đề xuất: "Vắng bài" = không nộp bài
  trong 7 ngày gần nhất; "Cần hỗ trợ" = tỉ lệ Accepted giảm liên tục qua ≥2 mốc tuần gần nhất; "Đang tốt" =
  còn lại — chốt số thật ở DD, không chặn BD màn hình.

## 8. Tham chiếu

- `01-rd/req/identity.md` — đặc tả F1-01 → F1-30.
- `.nexa/domain-registry.json` — domain `identity`, scope liệt kê bảng dữ liệu dự kiến.
- `.nexa/control/dependency-map.md` mục 2, 4 — hạ tầng phụ thuộc, thứ tự build (stage 1 cùng `problem-bank`).
- `.nexa/control/decision-registry.md` — `DEC-2026-0820-architecture-baseline`,
  `DEC-2026-0831-partial-score-testcase-ratio` (ảnh hưởng F1-06/F1-07).
- `06-plan/260912-1055-bd-rollout-order.md` — thứ tự triển khai BD.
