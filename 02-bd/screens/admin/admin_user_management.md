# BD — Màn `admin_user_management` (Quản lý người dùng)

> Trục: **màn hình**. Slug khớp `01-rd/screens/admin/admin_user_management.md` và sẽ khớp tiếp
> `03-dd/screens/admin/admin_user_management.md` khi viết DD (anti-drift rule 3, `bd-generation` Layer 2).
> Actor: A3 (ADMIN). Bounded Context chủ quản: `identity` (F1)
> [SoT: 01-rd/screens/admin/admin_user_management.md:4].
> BD module liên quan đã có: `02-bd/architecture/identity.md`, `02-bd/database/identity.md`,
> `02-bd/security/identity.md` — file này **không lặp lại** nội dung đó, chỉ tham chiếu.

## 0. Phạm vi đã chốt trước khi viết BD

RD mục 5 xác nhận Q1/Q2/Q3 đều đã đóng [SoT: 01-rd/screens/admin/admin_user_management.md:58-64]. BD kế
thừa trực tiếp, không mở lại:

- Không có "Báo cáo nghi gian lận" — ngoài phạm vi đồ án (`DEC-2026-0831-remove-plagiarism-report`).
- Không có "Đề nghị cấp quyền giảng viên" tự yêu cầu — chỉ ADMIN đổi vai trò (F1-13 nguyên văn).
- "Yêu cầu đặt lại mật khẩu" tách khỏi khối "Cần xử lý", chuyển vào khối thống kê riêng.

## 1. Layout — các vùng màn hình

Đối chiếu `09-layoutBase/Admin - Người dùng.dc.html` (nav-shell dùng chung mọi màn Admin, không mô tả
lại ở đây — xem `02-bd/screens/admin/_shell.md` nếu đã có, hoặc coi là hạ tầng UI dùng chung).

| Vùng | Nội dung | Nguồn layout |
| :--- | :--- | :--- |
| Thanh tiêu đề trang (header) | Tiêu đề "Quản lý người dùng" + phụ đề, công tắc theme, nút "Thêm tài khoản" | `09-layoutBase/Admin - Người dùng.dc.html:148-159` |
| Dải chỉ số tổng (stat cards) | 5 thẻ số liệu dạng lưới co giãn | `...:161-172`, dữ liệu mẫu `...:422-428` |
| Khối bảng người dùng (khối chính) | Thanh tìm kiếm + lọc + tab trạng thái, thanh hành động gộp (khi có chọn), bảng, phân trang | `...:174-237` |
| Khối "Phân bố theo vai trò" | Biểu đồ thanh ngang tỉ lệ theo `role.base_category` | `...:239-256`, `...:476-481` |
| Khối "Cần xử lý" | Danh sách việc cần ADMIN xử lý + liên kết "Nhật ký" | `...:258-274`, `...:483-486` |
| Footer | Thông tin phiên bản, trạng thái dịch vụ, liên kết phụ | `...:277-290` |

Bố cục cụ thể (màu sắc, khoảng cách, breakpoint) thuộc phạm vi dựng UI thật ở Next.js, không lặp lại số
đo pixel của prototype ở đây — BD chỉ chốt vùng và dữ liệu mỗi vùng cần.

## 2. Danh mục thành phần (component inventory)

### 2.1. Dải chỉ số tổng (5 thẻ)

| Thẻ | Nguồn dữ liệu | Ghi chú |
| :--- | :--- | :--- |
| Tổng tài khoản | `COUNT(users)` | |
| Đang hoạt động 24 giờ | `COUNT(users)` có hoạt động trong 24 giờ gần nhất | Định nghĩa "hoạt động" cụ thể (đăng nhập hay có request) chốt ở DD |
| Chờ xác thực email | `COUNT(users WHERE email_verified = false)` | Tương ứng khối "Cần xử lý" bên dưới, không phải hai nguồn khác nhau |
| Bị khoá | `COUNT(users WHERE status = 'DEACTIVATED')` | Bảng `users.status`, không phân biệt "tự xoá" và "ADMIN khoá" ở stat card này — xem mục 5 |
| Yêu cầu đặt lại mật khẩu | `COUNT` yêu cầu OTP quên mật khẩu đang treo (`identity:pwreset:*` Redis, TTL 10 phút) | Tách khỏi khối "Cần xử lý" theo `DEC` đóng Q3; đặt cùng dải thống kê thay vì hàng đợi hành động [SoT: 01-rd/screens/admin/admin_user_management.md:64] |

Số liệu "delta" (`+42`, `−3`...) trong prototype là minh hoạ UX, không phải trường dữ liệu bắt buộc — BD
không cam kết công thức delta cụ thể ở đây, chốt ở DD nếu owner muốn giữ.

`[SoT: Suy luận]` — RD không nêu công thức "hoạt động 24 giờ", BD đề xuất dựa theo cột hoạt động gần
nhất chưa có trong `database/identity.md` hiện tại; cần bổ sung cột theo dõi phiên hoặc dùng
`refresh_tokens.issued_at` làm proxy. Ghi vào mục 6 việc còn mở.

### 2.2. Bảng người dùng

Cột theo `09-layoutBase/Admin - Người dùng.dc.html:206-207, 210-225`:

| Cột | Ánh xạ dữ liệu | Ghi chú |
| :--- | :--- | :--- |
| Checkbox chọn dòng | trạng thái UI, không lưu server | Phục vụ hành động gộp mục 2.4 |
| Người dùng (avatar chữ cái đầu, tên, email) | `users.display_name`, `users.email` | |
| Vai trò | `roles.name` qua `users.role_id` | Badge màu theo `roles.base_category`, không phải theo `roles.code` — vì role tuỳ biến vẫn phải hiển thị đúng nhóm cơ bản [SoT: 02-bd/architecture/identity.md:74-80] |
| Đã giải | Read model `user_problem_best_score` — đếm bài có `best_verdict = ACCEPTED` | [SoT: 02-bd/database/identity.md:111-112] |
| Lượt nộp | `user_submission_stats.total_submissions` | [SoT: 02-bd/database/identity.md:113] |
| Hoạt động (mô tả tương đối: "5 phút trước") | Mốc hoạt động gần nhất — cùng nguồn với mục 2.1 "Đang hoạt động 24 giờ" | `[SoT: Suy luận]`, cột nguồn chưa có trong schema `identity`, xem mục 6 |
| Trạng thái | `users.status` (`ACTIVE`/`DEACTIVATED`) + cờ `email_verified` | Ba nhãn hiển thị "Hoạt động"/"Chờ xác thực"/"Bị khóa" ánh xạ từ hai trường này, không phải một cột enum ba giá trị riêng — tránh BD tạo cột thừa trùng `status` |

Tìm kiếm theo tên/email/tài khoản, lọc theo tab Vai trò (Tất cả/Người học/Giảng viên/Quản trị — ánh xạ
`roles.base_category`, không phải danh sách toàn bộ `roles.code` vì role tuỳ biến có thể nhiều) và tab
Trạng thái (Tất cả/Hoạt động/Bị khóa) [SoT: `09-layoutBase/Admin - Người dùng.dc.html:181-190`].
Phân trang server-side (prototype hiển thị "Trang 1 trong 143").

### 2.3. Nút "Thêm tài khoản" (header)

[SoT: `09-layoutBase/Admin - Người dùng.dc.html:158`]. RD F1-13 chỉ nêu "đổi vai trò, khoá/mở khoá,
reset mật khẩu" — không nêu ADMIN **tạo** tài khoản mới thay người dùng
[SoT: 01-rd/screens/admin/admin_user_management.md:21-22, F1-13]. Đây là điểm lệch giữa prototype và
RD chưa được RD xác nhận.

**Đề xuất của BD**: giữ nút này cho một luồng tạo tài khoản ADMIN/INSTRUCTOR thủ công (ví dụ cấp tài
khoản giảng viên mới không qua OAuth tự đăng ký) — hữu ích vận hành thực tế và không mâu thuẫn kiến
trúc, nhưng **chưa có mã `Fx-nn`** nào bao phủ. Đây không phải thay đổi kiến trúc/schema/auth theo
nghĩa cần quyết định trước khi viết BD (không đổi bảng, không đổi luồng RBAC), nên BD chỉ ghi nhận là
câu hỏi mở (mục 7, không tự ý cấp mã) thay vì tạo decision — nếu owner xác nhận đưa vào phạm vi, DD sẽ
cấp mã mới và bổ sung API tương ứng.

### 2.4. Thanh hành động gộp (bulk action bar)

Hiện khi `hasSelection = true` [SoT: `09-layoutBase/Admin - Người dùng.dc.html:194-203`]:

| Nút | Ánh xạ nghiệp vụ | Ghi chú |
| :--- | :--- | :--- |
| Đặt lại mật khẩu | F1-13 (ADMIN reset hộ) | Áp dụng cho từng tài khoản đã chọn |
| Đổi vai trò | F1-13 | Cần modal chọn `role_id` đích — nội dung modal chốt ở DD |
| Khóa tài khoản | F1-13 | Đặt `users.status = DEACTIVATED` cho từng tài khoản đã chọn |

Mỗi hành động gộp áp dụng cho **từng** tài khoản, ghi **từng** dòng riêng vào `system_audit_logs` (không
gộp một dòng) [SoT: 01-rd/screens/admin/admin_user_management.md:52-56, F1-14]. Về mặt kiến trúc: một
lệnh bulk phía UI là **N lệnh đơn** phía application layer của `identity` — không cần một use case
"bulk" riêng ở tầng domain, tránh model hoá sai một hành động gộp thành một aggregate mới.

Không có nút "Mở khoá tài khoản" riêng trong thanh hành động gộp ở prototype — mở khoá chỉ thao tác
trên từng dòng (chưa rõ vị trí UI chính xác, prototype không thể hiện nút mở khoá đơn dòng nào ở bảng
chính). Ghi vào mục 7 câu hỏi mở.

### 2.5. Khối "Phân bố theo vai trò"

Thanh ngang theo 4 nhóm: Người học/Giảng viên/Quản trị/Đã vô hiệu
[SoT: `09-layoutBase/Admin - Người dùng.dc.html:476-481`]. Ba nhóm đầu ánh xạ `roles.base_category`;
"Đã vô hiệu" ánh xạ `users.status = DEACTIVATED`, **không phải một base_category thứ tư** — đây là điểm
cần làm rõ khi viết DD để tránh cộng dồn sai tổng 100%: một tài khoản `DEACTIVATED` vẫn có
`base_category` xác định, hai chiều dữ liệu này không loại trừ nhau về mặt schema nhưng khối biểu đồ
prototype trình bày như bốn nhóm loại trừ nhau. `[SoT: Suy luận]` — BD đề xuất: nhóm "Đã vô hiệu" ưu
tiên hiển thị tách khỏi ba nhóm `base_category` (tài khoản `DEACTIVATED` không tính vào nhóm vai trò của
nó nữa), khớp trực giác "đã ngừng hoạt động" của khối UI này; chốt số liệu chính xác ở DD.

### 2.6. Khối "Cần xử lý" (đã thu hẹp còn 1 mục sau khi đóng Q1/Q2/Q3)

| Mục còn lại | Ánh xạ | Ghi chú |
| :--- | :--- | :--- |
| Chờ xác thực email | `COUNT(users WHERE email_verified = false)`, cùng nguồn 2.1 | Liên kết dẫn tới bảng người dùng đã lọc theo trạng thái này (chốt điều hướng cụ thể ở DD) |

Hai mục còn lại trong prototype ("Báo cáo nghi gian lận", "Đề nghị cấp quyền giảng viên") **xoá khỏi
UI thật** theo quyết định đã đóng — không thiết kế component cho hai mục này
[SoT: 01-rd/screens/admin/admin_user_management.md:46-49].

Link "Nhật ký" điều hướng sang màn `admin_system_log` [SoT: `09-layoutBase/Admin - Người dùng.dc.html:261`].

## 3. Trạng thái màn hình (screen states)

| Trạng thái | Mô tả | Ghi chú |
| :--- | :--- | :--- |
| `loading` | Đang tải trang đầu / đổi bộ lọc | Skeleton cho dải stat card + bảng |
| `loaded-empty` | Bộ lọc/tìm kiếm không khớp tài khoản nào | Thông báo rỗng, không phải lỗi |
| `loaded-with-data` | Trạng thái mặc định | Như mô tả mục 2 |
| `has-selection` | ≥1 dòng được chọn | Hiện thanh hành động gộp (mục 2.4) |
| `bulk-action-confirming` | Đang chờ xác nhận một hành động gộp (đặc biệt "Khóa tài khoản" — hành động phá huỷ khả năng đăng nhập) | BD yêu cầu bước xác nhận (modal) trước khi gọi API cho hành động khoá/đổi vai trò gộp — chưa thấy modal này trong prototype tĩnh, `[SoT: Suy luận]` theo nguyên tắc UX chuẩn cho hành động không hoàn tác dễ dàng trên nhiều tài khoản cùng lúc |
| `error` | API lỗi (mạng, 5xx) | Thông báo lỗi + nút thử lại, không để bảng hiển thị dữ liệu cũ gây hiểu nhầm |
| `action-forbidden` | ADMIN thao tác trên chính tài khoản của mình (tự khoá/tự đổi vai trò) | BD đề xuất chặn ở tầng UI + tầng application: một ADMIN không được tự khoá chính mình hoặc tự đổi vai trò khỏi ADMIN nếu là ADMIN cuối cùng còn hoạt động — quy tắc chưa có trong F1-13, ghi vào mục 7 |

## 4. API tiêu thụ (chỉ liệt kê tên — hợp đồng chốt ở DD)

Tất cả thuộc Bounded Context `identity`, hợp đồng request/response **chốt ở `03-dd/api/identity.md`**
(chưa viết) — file này không mô tả request/response theo anti-drift rule 1 (`bd-generation` Layer 2).

| Hành động màn hình | Endpoint (tên nghiệp vụ, đường dẫn cụ thể chốt ở DD) | Ghi chú |
| :--- | :--- | :--- |
| Tải dải chỉ số tổng | `GET /admin/users/stats` (tên tạm) | Gồm cả số liệu "Chờ xác thực email" dùng chung khối 2.6 |
| Tìm kiếm/lọc/phân trang bảng | `GET /admin/users` (tên tạm) | Tham số: `query`, `role`, `status`, `page` |
| Đổi vai trò (đơn/gộp) | `PATCH /admin/users/{id}/role` hoặc biến thể bulk (tên tạm) | F1-13 |
| Khoá/mở khoá tài khoản (đơn/gộp) | `PATCH /admin/users/{id}/status` hoặc biến thể bulk (tên tạm) | F1-13 |
| Đặt lại mật khẩu hộ (đơn/gộp) | `POST /admin/users/{id}/reset-password` hoặc biến thể bulk (tên tạm) | F1-13 — kết quả gửi email cho người dùng, không hiển thị mật khẩu mới cho ADMIN |
| Phân bố theo vai trò | `GET /admin/users/role-distribution` (tên tạm) | Mục 2.5 |
| (Mở, chưa chốt phạm vi) Tạo tài khoản thủ công | — | Xem mục 2.3, chờ owner xác nhận trước khi cấp endpoint |

Quyết định gộp bulk vào cùng endpoint đơn (nhận mảng `id`) hay tách endpoint riêng — quyết định kỹ
thuật DD, không phải BD.

## 5. Điều hướng (navigation)

- Vào từ: menu Sidebar nhóm "Hệ thống" → "Người dùng" [SoT: `09-layoutBase/Admin - Người dùng.dc.html:339-341`].
- Ra: link "Nhật ký" → `admin_system_log`; các dòng "Chờ xác thực email" trong khối "Cần xử lý" → bảng
  người dùng đã lọc sẵn theo trạng thái (điều hướng nội bộ trong cùng màn, không sang màn khác).
- Nút "Thêm tài khoản" → mở modal/route tạo tài khoản (nếu phạm vi được xác nhận, xem mục 2.3).

## 6. Quyền truy cập màn hình (access rights)

- Toàn bộ màn chỉ ADMIN (`base_category = ADMIN`) truy cập được — kiểm ở tầng route (frontend) và tầng
  application (backend, `@PreAuthorize` hoặc tương đương) theo nguyên tắc chung của `identity`
  [SoT: 02-bd/security/identity.md:25-26].
- Đây là chức năng Lớp 2 (ma trận phân quyền) hay Lớp 1 (cố định theo `base_category`)? Theo mô hình hai
  lớp [SoT: 02-bd/architecture/identity.md:74-80], "Quản lý người dùng" khớp function seed
  `USER_MANAGEMENT` trong bảng `functions` [SoT: 02-bd/database/identity.md:47-49] — nghĩa là **kiểm qua
  ma trận `permissions` (Lớp 2)**, không phải hard-code theo `base_category` — vì `USER_MANAGEMENT` là
  một trong 10 function seed có thể bật/tắt theo role. Một role tuỳ biến có `base_category = ADMIN`
  nhưng bị tắt quyền `USER_MANAGEMENT:READ` sẽ không thấy màn này — BD ghi rõ để tránh nhầm với các màn
  Lớp 1 (ví dụ trang cá nhân) không bao giờ tra `permissions`.
- Mọi hành động ghi (đổi vai trò, khoá/mở khoá, reset mật khẩu) yêu cầu action `UPDATE` trên function
  `USER_MANAGEMENT`; xem yêu cầu action `READ` cho việc chỉ xem bảng.
- Audit bắt buộc không ngoại lệ cho mọi hành động ghi ở màn này, kể cả ADMIN thao tác trên ADMIN khác
  [SoT: 02-bd/security/identity.md:51-53].

## 7. Câu hỏi mở

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| BD-Q1 | Nút "Thêm tài khoản" (mục 2.3) — có nằm trong phạm vi F1-13 hay là tính năng mới chưa có mã? | RD F1-13 chỉ nêu đổi vai trò/khoá/reset, không nêu ADMIN tạo tài khoản mới | Giữ như một khả năng vận hành hữu ích, cấp mã mới ở DD nếu owner xác nhận; nếu không xác nhận thì xoá nút khi dựng UI thật | Chủ dự án |
| BD-Q2 | Cột "Hoạt động" (mục 2.2) và stat "Đang hoạt động 24 giờ" (mục 2.1) lấy từ nguồn dữ liệu nào — chưa có cột theo dõi hoạt động gần nhất trong `database/identity.md` hiện tại | `02-bd/database/identity.md` chưa có bảng/cột nào ghi "lần hoạt động cuối" của user (chỉ có `refresh_tokens.issued_at` làm proxy gián tiếp) | Bổ sung cột `users.last_active_at` (cập nhật mỗi request có xác thực, throttle theo phút) hoặc chấp nhận proxy qua `refresh_tokens`; chốt ở DD/database khi đụng tới bảng này | Chủ dự án + BD `database/identity.md` |
| BD-Q3 | Nút mở khoá tài khoản đơn dòng — vị trí UI không thấy trong prototype tĩnh (chỉ có "Khóa tài khoản" ở thanh hành động gộp) | Prototype không có ảnh cho trạng thái "đã bị khóa, xem hành động mở khóa" | Thêm nút "Mở khóa" xuất hiện theo điều kiện khi dòng có `status = DEACTIVATED`, hoặc dùng chung nút "Đổi trạng thái" đảo chiều | Chủ dự án |
| BD-Q4 | Có cần chặn ADMIN tự khoá/tự đổi vai trò chính mình, đặc biệt khi là ADMIN hoạt động cuối cùng? | F1-13 không nêu ràng buộc này, RD màn cũng không | Chặn ở tầng application: từ chối hành động khi `target_user_id == actor_user_id` cho khoá tài khoản; từ chối hạ vai trò ADMIN cuối cùng còn `status = ACTIVE` | Chủ dự án |

## 8. Tham chiếu

- `01-rd/screens/admin/admin_user_management.md` — RD màn hình.
- `09-layoutBase/Admin - Người dùng.dc.html` — prototype.
- `02-bd/architecture/identity.md`, `02-bd/database/identity.md`, `02-bd/security/identity.md` — BD module `identity`.
- `01-rd/screens/admin/admin_system_log.md` — màn đích của link "Nhật ký".
- `DEC-2026-0831-remove-plagiarism-report` — loại "Báo cáo nghi gian lận" khỏi phạm vi.
