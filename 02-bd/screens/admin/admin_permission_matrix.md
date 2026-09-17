# BD — Màn `admin_permission_matrix` (Ma trận phân quyền)

> Trục: **màn hình** (không phải Bounded Context). Slug khớp `01-rd/screens/admin/admin_permission_matrix.md`.
> Bounded Context sở hữu toàn bộ dữ liệu và logic của màn này: `identity` (F1) — một màn, một module, không
> cần gộp nhiều BC như `problem_detail`
> [SoT: 01-rd/screens/admin/admin_permission_matrix.md:4]. Actor: A3 (admin).
>
> Đọc cùng: `02-bd/architecture/identity.md` mục 4 (mô hình `base_category` vs `role`),
> `02-bd/database/identity.md` mục 1.1-1.4 (`roles`, `functions`, `actions`, `permissions`),
> `02-bd/security/identity.md` mục 2 (RBAC, cache quyền, hiệu lực ngay).
> Anti-drift: mọi endpoint chỉ nêu tên + BC sở hữu ở đây; request/response thuộc
> `03-dd/api/identity.md` (chưa viết tại thời điểm BD này).

## 1. Bối cảnh dữ liệu màn hình dựa trên

Đã chốt ở BD module `identity` (không lặp lại thiết kế, chỉ tham chiếu):

- **`base_category`** (`STUDENT`/`INSTRUCTOR`/`ADMIN`) quyết định Lớp 1 (quyền học tập cơ bản, không đi
  qua ma trận) [SoT: 02-bd/architecture/identity.md:74-76].
- **`role`** là thực thể tạo/sửa/xoá được (F1-10/F1-11), mỗi role gắn đúng một `base_category`; ba role hệ
  thống seed sẵn `is_system = true`, không xoá được
  [SoT: 02-bd/architecture/identity.md:76-80; 02-bd/database/identity.md:31-43]. Câu hỏi "role tuỳ biến có
  thật không" đã đóng — xem `02-bd/architecture/identity.md` mục 4, xác nhận 2026-09-13.
- `functions` (10 dòng, đã bỏ `REJUDGE_MANAGEMENT` theo `DEC-2026-0828-remove-rejudge-scope`) và `actions`
  (`CREATE`/`READ`/`UPDATE`/`DELETE`) là dữ liệu seed chỉ đọc trên giao diện
  [SoT: 02-bd/database/identity.md:45-53].
- `permissions` là ô `(role_id, function_id, action_id) → granted`, đổi có hiệu lực ngay qua invalidate
  cache theo role, không nhúng vào JWT [SoT: 02-bd/security/identity.md:9-10, 35-37].

## 2. Layout regions

Đối chiếu `09-layoutBase/Admin - Ma trận phân quyền.dc.html` (cấu trúc, không lấy màu/spacing — màn dùng
chung khung admin của toàn hệ thống, xem ghi chú mục 7):

| Vùng | Nội dung | Nguồn |
| :--- | :--- | :--- |
| Sidebar điều hướng (dùng chung mọi màn admin) | Nhóm menu Tổng quan / Nội dung / Vận hành / AI / Hệ thống, mục "Ma trận phân quyền" đang active trong nhóm Hệ thống | [SoT: 09-layoutBase/Admin - Ma trận phân quyền.dc.html:352-356] |
| Thanh tiêu đề màn (sticky) | Tiêu đề "Ma trận phân quyền" + phụ đề "Role × Function × Action · thay đổi có hiệu lực ngay", nút "Lưu thay đổi" | [SoT: 09-layoutBase/Admin - Ma trận phân quyền.dc.html:150-161] |
| Banner ghi chú cố định | "Function và Action là dữ liệu seed cố định, chỉ đọc — chỉ Role tạo/sửa/xoá được. Quyền học tập cơ bản của STUDENT... không đi qua ma trận này" | [SoT: 09-layoutBase/Admin - Ma trận phân quyền.dc.html:163-165] |
| Khối tab Role + hành động Role | Tab chọn role (chip), nút "+ Vai trò mới" (mở form nội tuyến: ô nhập tên + Tạo/Huỷ), nút "Xoá vai trò này" (role tuỳ biến) hoặc nhãn "Vai trò hệ thống · không thể xoá" (role hệ thống) | [SoT: 09-layoutBase/Admin - Ma trận phân quyền.dc.html:168-190] |
| Banner riêng khi chọn STUDENT | "STUDENT có sẵn toàn bộ quyền học tập cơ bản..., các ô dưới đây chỉ để tham khảo và không chỉnh được" | [SoT: 09-layoutBase/Admin - Ma trận phân quyền.dc.html:192-196] |
| Bảng ma trận | Cột 1 = tên Function (kèm mã `key` dạng mono nhỏ dưới tên), 4 cột Action = Tạo/Xem/Sửa/Xoá; mỗi ô là nút toggle vuông bật/tắt | [SoT: 09-layoutBase/Admin - Ma trận phân quyền.dc.html:198-224] |
| Footer trang | Ghi chú "Mọi thay đổi ghi vào Nhật ký hệ thống" | [SoT: 09-layoutBase/Admin - Ma trận phân quyền.dc.html:227-240] |

## 3. Component inventory

| Component | Hành vi |
| :--- | :--- |
| `RoleTabGroup` | Danh sách chip role (tối thiểu 3 hệ thống + N tuỳ biến), chọn 1 role active tại một thời điểm [SoT: 09-layoutBase/...:170-172, 406-411] |
| `AddRoleInlineForm` | Bấm "+ Vai trò mới" → hiện ô nhập tên + hai nút Tạo/Huỷ thay cho nút "+"; validate tên không rỗng trước khi cho phép Tạo (validate cụ thể để DD) [SoT: 09-layoutBase/...:174-183, 464-467] |
| `DeleteRoleButton` | Chỉ hiện khi role đang chọn không phải `is_system`; ẩn hoàn toàn (thay bằng nhãn tĩnh) với 3 role hệ thống [SoT: 09-layoutBase/...:184-189, 455-457] |
| `SystemRoleBadgeText` | Nhãn "Vai trò hệ thống · không thể xoá" thay thế `DeleteRoleButton` khi role hệ thống |
| `StudentReadOnlyNotice` | Banner chỉ hiện khi `activeRole.base_category === STUDENT`, giải thích lý do các ô bị khoá — không phải vì STUDENT không có quyền [SoT: 09-layoutBase/...:192-196] |
| `PermissionMatrixTable` | Bảng 10 hàng Function × 4 cột Action; mỗi ô là `PermissionToggleCell` | [SoT: 09-layoutBase/...:198-224] |
| `PermissionToggleCell` | Nút toggle vuông, trạng thái bật hiển thị dấu tick nền màu nhấn, tắt hiển thị viền rỗng; `disabled` khi role đang chọn có `base_category = STUDENT` [SoT: 09-layoutBase/...:216-217, 423-434] |
| `SaveChangesButton` | Nút "Lưu thay đổi" trên thanh tiêu đề, đổi nhãn tạm thời sau khi lưu thành công [SoT: 09-layoutBase/...:160, 483-484] — hành vi lưu tức thời từng ô so với nút Lưu tổng: xem mục 6 (câu hỏi mở) |

**Không đưa vào BD (thuộc phạm vi khác):** màu sắc/token thiết kế cụ thể — chưa có design-system SoT
chính thức cho Next.js, chỉ mô tả cấu trúc theo layoutBase; request/response của các API — DD.

## 4. Screen states

| State | Điều kiện | Hiển thị |
| :--- | :--- | :--- |
| `S1 — Đang xem role hệ thống (INSTRUCTOR/ADMIN)` | `activeRole.is_system = true` và `base_category ≠ STUDENT` | Bảng ma trận tương tác được, nút "Vai trò hệ thống · không thể xoá" thay cho nút xoá |
| `S2 — Đang xem STUDENT` | `activeRole.base_category = STUDENT` | Toàn bộ ô toggle `disabled`, hiện `StudentReadOnlyNotice`, nút xoá cũng ẩn (STUDENT cũng là role hệ thống) |
| `S3 — Đang xem role tuỳ biến` | `activeRole.is_system = false` | Bảng tương tác được (trừ khi `base_category = STUDENT`, xem S2 áp dụng đồng thời), hiện nút "Xoá vai trò này" |
| `S4 — Đang thêm role mới` | Bấm "+ Vai trò mới" | Form nội tuyến thay nút "+", các phần khác của màn giữ nguyên tương tác được |
| `S5 — Vừa lưu thành công` | Sau khi API cập nhật ô/role trả về thành công | Nhãn nút Lưu đổi tạm thời báo đã lưu, tự trở lại nhãn gốc sau một khoảng ngắn [SoT: 09-layoutBase/...:483-484] |
| `S6 — Lỗi thao tác` | API trả lỗi (ví dụ xoá role đang có người dùng gán, trùng tên role) | Thông báo lỗi tại chỗ, không đổi trạng thái hiển thị của ô/role liên quan — nội dung thông báo cụ thể để DD |
| `S7 — Trống dữ liệu ban đầu của role mới tạo` | Role vừa tạo, chưa có dòng `permissions` nào | Mọi ô Action của mọi Function đều tắt (`granted = false` mặc định), không phải `disabled` [SoT: 01-rd/screens/admin/admin_permission_matrix.md:48-50] |

## 5. APIs consumed (chỉ tên endpoint + BC sở hữu — chi tiết ở `03-dd/api/identity.md`)

| Hành động màn hình | Endpoint (tên nghiệp vụ, chưa chốt route) | BC sở hữu |
| :--- | :--- | :--- |
| Tải danh sách role | Lấy danh sách role | `identity` |
| Tải danh sách Function/Action seed | Lấy danh mục Function/Action | `identity` |
| Tải ma trận quyền của một role | Lấy quyền theo role | `identity` |
| Tạo role mới | Tạo role | `identity` |
| Xoá role tuỳ biến | Xoá role | `identity` |
| Bật/tắt một ô quyền | Cập nhật quyền (role, function, action) | `identity` |

Không có endpoint nào ngoài `identity` — màn này không đọc dữ liệu module khác.

## 6. Navigation

- Vào từ sidebar admin, nhóm "Hệ thống" → mục "Ma trận phân quyền"
  [SoT: 09-layoutBase/Admin - Ma trận phân quyền.dc.html:352-356].
- Không có điều hướng đi tiếp từ màn này sang màn khác (không click-through tới chi tiết người dùng hay
  chi tiết role) — mọi thao tác gói gọn tại chỗ (tab, form nội tuyến, toggle).
- Rời màn khi còn thay đổi chưa lưu: hành vi cụ thể (cảnh báo rời trang hay tự động lưu từng ô) là câu hỏi
  mở — xem mục 8.

## 7. Access rights

- Chỉ actor A3 (`ADMIN`, `base_category = ADMIN`) truy cập được toàn bộ màn — kiểm bằng `base_category`
  ở Lớp 1 (route-level gate), không qua bảng `permissions`
  [SoT: 02-bd/security/identity.md:31-34, nguyên tắc Lớp 1/Lớp 2 không lẫn lộn].
- Trong nội bộ màn, thao tác ghi (bật/tắt ô, tạo/xoá role) đồng thời phải qua kiểm `PERMISSION_MATRIX:UPDATE`
  / `PERMISSION_MATRIX:CREATE` / `PERMISSION_MATRIX:DELETE` ở tầng ứng dụng theo Lớp 2 — vì `ADMIN` mặc
  định có toàn quyền theo dữ liệu mẫu prototype (`ADMIN` toàn `true` ở mọi function)
  [SoT: 09-layoutBase/Admin - Ma trận phân quyền.dc.html:270-282] nhưng model dữ liệu không loại trừ khả
  năng một role tuỳ biến có `base_category = ADMIN` bị giới hạn quyền `PERMISSION_MATRIX` — màn phải kiểm
  quyền thật, không giả định "vào được màn nghĩa là toàn quyền trên màn".
- Mọi hành động ghi trên màn này ghi `system_audit_logs` không ngoại lệ, kể cả ADMIN tự sửa quyền của
  chính role đang gán cho mình [SoT: 02-bd/security/identity.md:51-53].
- Không tiết lộ thông tin nhạy cảm khác ngoài phạm vi RBAC trên màn này (không có PII người dùng hiển thị
  ở đây).

## 8. Câu hỏi mở

1. **Lưu tức thời theo từng ô hay gộp vào nút "Lưu thay đổi"?** Prototype có cả `toggleCell` (đổi state
   cục bộ ngay khi bấm) lẫn nút `save` riêng ở thanh tiêu đề
   [SoT: 09-layoutBase/Admin - Ma trận phân quyền.dc.html:217, 318-327, 483-484], nhưng
   `02-bd/security/identity.md` dòng 9-10 yêu cầu "đổi có hiệu lực ngay, không cần khởi động lại dịch vụ" —
   không nói rõ "ngay" là ngay khi bấm ô hay ngay sau khi bấm Lưu. Đề xuất: mỗi lần bấm ô gọi API cập nhật
   ngay lập tức (khớp đúng nghĩa "có hiệu lực ngay" của F1-10), nút "Lưu thay đổi" chỉ còn ý nghĩa xác nhận
   UX/hoàn tất một phiên chỉnh sửa chứ không phải điều kiện để quyền có hiệu lực — cần chốt ở DD trước khi
   viết `03-dd/api/identity.md`.
2. **Ràng buộc xoá role đang có người dùng gán** — RD dòng 76-80 (ở `architecture/identity.md`, dẫn lại từ
   RD) nói role hệ thống "không xoá được khi đang có người dùng gán", nhưng câu này áp dụng cho role hệ
   thống (vốn đã không xoá được vì `is_system`); với role tuỳ biến đang có người dùng gán, RD/BD module
   chưa nói rõ có chặn xoá hay tự động chuyển người dùng về role mặc định nào — cần chốt thông báo lỗi cụ
   thể ở DD (state `S6`).
3. **Không có form sửa tên role** trong prototype (chỉ có Tạo và Xoá) — RD F1-11 chỉ nói "Role tạo/sửa/xoá
   được" nhưng UI không thấy nút sửa tên. `[SoT: Suy luận]` — có thể "sửa" ở đây chỉ nghĩa là sửa ma trận
   quyền của role (không phải sửa tên hiển thị); đề xuất bổ sung một hành vi sửa tên tối thiểu (double-click
   vào tên role tuỳ biến hoặc icon bút chì nhỏ) khi vào Prototype thật trên Next.js, không chặn BD.

## 9. Tham chiếu

- `01-rd/screens/admin/admin_permission_matrix.md` — RD màn hình.
- `09-layoutBase/Admin - Ma trận phân quyền.dc.html` — bằng chứng layout.
- `02-bd/architecture/identity.md`, `02-bd/database/identity.md`, `02-bd/security/identity.md` — BD module.
- `.nexa/control/decision-registry.md` — `DEC-2026-0828-remove-rejudge-scope` (bỏ `REJUDGE_MANAGEMENT`
  khỏi danh sách Function).
