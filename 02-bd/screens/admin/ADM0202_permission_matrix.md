# Tài liệu thiết kế cơ bản (BD) — Ma trận phân quyền (`ADM0202`)

## Phương châm tài liệu [Nội bộ — không cần khách duyệt]

- Viết theo **mẫu 9 sheet**. Bộ thẻ đóng, quy ước ID, ký hiệu chuẩn, ranh giới với tài liệu khác và
  checklist kiểm toán nằm ở `02-bd/_rules/bd-template-9sheet.md` — file này không chép lại.
- Phần gắn nhãn `[Nội bộ]` phục vụ sinh mã và tự kiểm toán, không phải yêu cầu của khách hàng: mục 4.5, mục
  7.3 và các ghi chú quy ước.
- Mã màn `ADM0202` theo bảng mã ở `02-bd/_rules/bd-template-9sheet.md` mục 8; tên file mang tiền tố mã.
- Màn này **không có màn con và không có popup nhập liệu**. Mọi thao tác gói gọn tại chỗ: tab vai trò, form nội tuyến
  thêm vai trò, ô bật hoặc tắt quyền. Có hai hộp **xác nhận** nhỏ: tự gỡ quyền của chính vai trò mình đang giữ
  (Sheet 9 NO 8) và xoá vai trò (Sheet 8 EVT-7, chỉ khi vai trò không còn tài khoản nào gán). **Sửa V0.4 (2026-10-08):** bỏ nút "Lưu thay đổi" — mỗi ô lưu tức thời
  (Q1 đã chốt).

> Đọc cùng `01-rd/screens/admin/ADM0202_permission_matrix.md` (hành vi ở mức yêu cầu, không lặp lại ở đây) và
> ba file BD module: `02-bd/architecture/identity.md` mục 4, `02-bd/database/identity.md` mục 1.2-1.4,
> `02-bd/security/identity.md` mục 2. Khung điều hướng dùng chung: `02-bd/screens/admin/_shell.md` — file
> này không mô tả lại thanh điều hướng, toolbar hay chân trang.
>
> **Không thiết kế** Function `REJUDGE_MANAGEMENT` — đã loại khỏi phạm vi theo
> `DEC-2026-0828-remove-rejudge-scope`, nên bảng ma trận còn **10 hàng** chứ không phải 11 như prototype
> [Nguồn: 01-rd/screens/admin/ADM0202_permission_matrix.md:39-41; 02-bd/database/identity.md:47-49].
>
> **Lưu ý về trục ma trận**: hàng là Function, cột là **Action** (`CREATE`/`READ`/`UPDATE`/`DELETE`); Role
> chọn bằng tab phía trên bảng, không phải bằng cột
> [Nguồn: 09-layoutBase/Admin - Ma trận phân quyền.dc.html:202-219].

---

## Sheet 1. Bìa

| Mục | Giá trị |
| :--- | :--- |
| Hệ thống | AlgoPrep |
| Phân hệ | `identity` (F1) |
| Công đoạn | BD — Thiết kế cơ bản |
| Phân loại | Màn hình |
| Tên màn | Ma trận phân quyền |
| Mã màn hình | `ADM0202` |
| Tên vật lý (slug) | `admin_permission_matrix` |
| Trục tài liệu | Màn hình (`02-bd/screens/`) |
| Actor | A3 (`ADMIN`) |
| Phiên bản | V0.6 |
| Người tạo | Nhóm phát triển AlgoPrep |
| Ngày tạo | 2026/09/15 |
| Người cập nhật | Nhóm phát triển AlgoPrep |
| Ngày cập nhật | 2026/10/08 |

---

## Sheet 2. Lịch sử sửa đổi

| Ver | Sheet bị sửa | Nội dung sửa | Ngày | Người sửa |
| :--- | :--- | :--- | :--- | :--- |
| V0.1 | Toàn bộ | Tạo mới theo cấu trúc 9 mục văn xuôi (bối cảnh dữ liệu, layout regions, component inventory, screen states, APIs consumed, navigation, access rights, câu hỏi mở, tham chiếu) | 2026/09/15 | Nhóm phát triển AlgoPrep |
| V0.2 | Toàn bộ | Chuyển sang mẫu 9 sheet. Chốt nguồn dữ liệu của mọi trường hiển thị, bổ sung Sheet 8 danh sách sự kiện và Sheet 9 đặc tả kiểm tra (thêm hai luật an toàn: tự gỡ quyền của chính vai trò mình, và gỡ hết quyền `PERMISSION_MATRIX` khỏi vai trò `ADMIN`), giữ nguyên ba câu hỏi mở của V0.1 và thêm hai câu mới | 2026/09/20 | Nhóm phát triển AlgoPrep |
| V0.3 | Sheet 6, 8, 9 | Áp `DEC-2026-1003-toast-feedback-channel`: kết quả thao tác và lỗi nhập liệu ghi là toast dùng chung, ô sai chỉ đổi viền đỏ. | 2026/10/03 | AI |
| V0.4 | Phương châm, Sheet 3, 4, 5, 6, 7, 8, 9, Câu hỏi mở | Đồng bộ prototype 2026-10-08 và chốt của owner cùng ngày (`DEC-2026-1008-admin-system-screens-review`). (1) **Q1 đã chốt: lưu tức thời từng ô; bỏ nút "Lưu thay đổi"** — Sheet 5/6 Khu vực A NO 3 đánh dấu đã bỏ, EVT-9 đã bỏ, EVT-2 và EVT-10 bỏ bước hỏi "thay đổi chưa lưu", Q5 tự đóng, Sheet 7.3 không còn endpoint lô. (2) **Q4 đã chốt: chặn cứng** việc gỡ ô `PERMISSION_MATRIX:UPDATE` cuối cùng của mọi vai trò `base_category = ADMIN` (Sheet 9 NO 9 vẫn mức Lỗi); NO 8 (tự gỡ quyền vai trò mình giữ) vẫn là cảnh báo xác nhận, câu chữ nay nói thay đổi có hiệu lực ngay. (3) Ghi nhận prototype: tên vai trò mới tối đa 50 ký tự (độ dài vẫn `[Suy luận]`), kiểm trùng so cả `code` sinh ra lẫn nhãn không phân biệt hoa thường, vai trò tuỳ biến mới mang `base_category = INSTRUCTOR` `[Suy luận]` — thêm Q6 | 2026/10/08 | AI |
| V0.5 | Sheet 5, 6, 8, 9, Câu hỏi mở | Form tạo vai trò có ô chọn nhóm cơ bản, mặc định `STUDENT` (Sheet 5 và 6 Khu vực C NO 8); Q6 phần 1 chốt (owner, 2026-10-08), phần 2 (khoá ô theo nhóm hay theo vai trò hệ thống) còn mở. | 2026/10/08 | AI |
| V0.6 | Phương châm, Sheet 3, 4, 5, 6, 7, 8, 9, Câu hỏi mở | Đồng bộ prototype vòng 2 ngày 2026-10-08 và chốt của owner (`DEC-2026-1008-admin-system-open-questions-closed`). (1) **Q2 đã chốt: chặn xoá** vai trò tuỳ biến còn tài khoản gán, không tự chuyển — `RoleDto` thêm `userCount` (Sheet 7.1 NO 12), nút xoá hiện toast cảnh báo kèm tên vai trò và số tài khoản, không mở hộp xác nhận; hộp xác nhận chỉ dành cho `userCount = 0` (Sheet 8 EVT-7, Sheet 9 NO 7). (2) **Q3 đã chốt: không đổi tên vai trò**; "sửa" của F1-11 là sửa ô ma trận. (3) **Q6 phần 2 đã chốt: khoá ô chỉ theo vai trò hệ thống `STUDENT` (theo `code`), không theo nhóm cơ bản**; vai trò tuỳ biến luôn chỉnh được kể cả khi nhóm là `STUDENT` — viết lại Sheet 3, 4, 5, 6, 7, 8 (EVT-2, EVT-8), 9 (NO 5). Q6 đóng hẳn. | 2026/10/08 | AI |

---

## Sheet 3. Sơ đồ chuyển màn

> [Nội bộ] Quy ước vẽ: bố trí từ trái sang phải. Màn gọi tới tô tím, màn hiện tại tô xanh nhạt. Màn này
> không có popup nên không dùng màu vàng. Màu và `[Nguồn:]` dùng để truy vết, không phải yêu cầu của khách.

### 3.1 Danh sách chuyển màn

#### Khung điều hướng Admin → Ma trận phân quyền

[Điều kiện mở] Chọn mục con "Ma trận phân quyền" trong nhóm "Hệ thống" ở thanh điều hướng bên trái.

[Chế độ mở] Chế độ xem và sửa. Không có chế độ chỉ xem riêng cho màn; phạm vi sửa phụ thuộc vai trò đang
chọn — vai trò hệ thống `STUDENT` thì toàn bộ ô khoá tương tác (khoá theo `code` vai trò, không theo nhóm cơ bản).

[Thông tin truyền] Không có.

[Giá trị trả về] Không có.

[Khi thành công] Tải danh sách vai trò, danh mục Function/Action seed và ma trận quyền của vai trò đang
chọn, hiển thị bảng 10 hàng Function và 4 cột Action.

[Khi huỷ] Không có.

#### Ma trận phân quyền → Màn Admin khác

[Điều kiện mở] Bấm một mục khác trên thanh điều hướng bên trái.

[Chế độ mở] Không có.

[Thông tin truyền] Không có.

[Giá trị trả về] Không có.

[Khi thành công] Mở màn Admin tương ứng. Màn không có thay đổi chưa lưu (mỗi ô lưu tức thời, Q1 đã chốt) nên
**không** hỏi xác nhận khi rời màn (Q5 đã đóng).

[Khi huỷ] Không có.

### 3.2 Sơ đồ

```mermaid
flowchart LR
    nav["Khung điều hướng Admin<br/>nhóm Hệ thống"] -->|"chọn Ma trận phân quyền"| main["Ma trận phân quyền<br/>admin_permission_matrix"]
    main -->|"chọn mục nav khác"| other["Màn Admin khác<br/>theo mục được chọn"]

    classDef source fill:#F3E5F5,stroke:#9C5CC4,color:#000
    classDef screen fill:#E3F2FD,stroke:#3B82F6,color:#000

    class nav,other source
    class main screen
```

[Nguồn: 09-layoutBase/Admin - Ma trận phân quyền.dc.html:352-356; 02-bd/screens/admin/_shell.md:29-43]

---

## Sheet 4. Bố cục màn hình

### 4.1 Tổng quan màn

[Mục đích màn] Cho quản trị viên cấu hình ma trận quyền Role × Function × Action, có hiệu lực ngay, để
kiểm soát ai được làm gì trong khu vực quản trị mà không phải sửa mã
[Nguồn: 01-rd/screens/admin/ADM0202_permission_matrix.md:13-15].

[Luồng nghiệp vụ chính]

1. **Hiển thị ban đầu**: vào màn, hệ thống tải danh sách vai trò, danh mục Function/Action seed và ma trận
   quyền của vai trò đang chọn. Trong lúc chờ, bảng hiển thị khung chờ đúng số dòng của danh mục Function.
2. **Chọn vai trò**: quản trị viên bấm một tab vai trò. Bảng nạp lại giá trị ô của vai trò đó. Vai trò hệ
   thống `STUDENT` hiển thị thêm ghi chú giải thích và khoá toàn bộ ô (khoá theo `code`, vai trò tuỳ biến thì không bao giờ bị khoá).
3. **Bật hoặc tắt quyền**: bấm một ô trong bảng để đảo trạng thái `granted` của bộ ba (vai trò, Function,
   Action). **Lưu tức thời từng ô** (Q1 đã chốt 2026-10-08): ô đổi và ghi ngay, không có bước "Lưu" riêng. Hai ràng
   buộc chạy trước khi ghi: gỡ ô `PERMISSION_MATRIX:UPDATE` cuối cùng của mọi vai trò `ADMIN` thì **chặn cứng**
   (toast); gỡ một ô của chính vai trò mình đang giữ thì **hỏi xác nhận** [Nguồn: 05-coding/frontend/src/views/admin/permission-matrix/ui/admin-permission-matrix-view.tsx:64-71; 05-coding/frontend/src/views/admin/permission-matrix/model/guards.ts:30-60].
4. **Tạo vai trò mới**: bấm "+ Vai trò mới", nhập tên (tối đa 50 ký tự, không trùng mã sinh ra hay nhãn
   của vai trò có sẵn, Sheet 9 NO 3, 4, 11), bấm "Tạo". Vai trò mới mặc định **không có quyền
   nào** [Nguồn: 01-rd/screens/admin/ADM0202_permission_matrix.md:48-50] và mang nhóm cơ bản chọn ở form, mặc định
   `STUDENT` (Q6 đã chốt) [Nguồn: 05-coding/frontend/src/views/admin/permission-matrix/ui/admin-permission-matrix-view.tsx:94-116].
5. **Xoá vai trò tuỳ biến**: bấm "Xoá vai trò này". Vai trò hệ thống không có nút này. Vai trò còn tài khoản gán
   (`userCount > 0`) thì **bị chặn** (Q2 đã chốt): hiện toast cảnh báo, không mở hộp xác nhận; chỉ khi `userCount = 0`
   mới hỏi xác nhận rồi xoá [Nguồn: 05-coding/frontend/src/views/admin/permission-matrix/ui/admin-permission-matrix-view.tsx:251-258,297-307].
6. ~~**Lưu thay đổi**: bấm "Lưu thay đổi" trên thanh tiêu đề.~~ **Bỏ 2026-10-08 (Q1 đã chốt):** mỗi ô lưu tức
   thời, thanh tiêu đề không còn nút lưu.

[Người dùng] Quản trị viên đã đăng nhập, `base_category = ADMIN`, và có quyền `PERMISSION_MATRIX` ở tầng
ứng dụng cho các thao tác ghi.

[Tệp liên quan] Không có. Màn này không xuất hay nhập tệp.

[Phạm vi]
- Không có Function `REJUDGE_MANAGEMENT` (`DEC-2026-0828-remove-rejudge-scope`) — bảng còn 10 hàng.
- Không quản lý quyền học tập cơ bản của `STUDENT` (Lớp 1) — luồng này kiểm bằng `base_category`, không
  đi qua bảng `permissions` [Nguồn: 02-bd/security/identity.md:31-34].
- Không đổi tên vai trò (Q3 đã chốt 2026-10-08): "sửa" ở F1-11 nghĩa là sửa ô ma trận của vai trò; prototype chỉ có Tạo và Xoá.
- Không hiển thị danh sách người dùng đang gán vai trò — đó là màn `admin_user_management`.
- Không thêm hay sửa Function và Action: hai bảng này là dữ liệu seed chỉ đọc
  [Nguồn: 02-bd/database/identity.md:45-53].

[Quyền sử dụng]
- Xem: được, khi `base_category = ADMIN`.
- Thêm: được (tạo vai trò tuỳ biến), qua kiểm `PERMISSION_MATRIX:CREATE`.
- Sửa: được (bật hoặc tắt ô quyền), qua kiểm `PERMISSION_MATRIX:UPDATE`.
- Xoá: được với vai trò tuỳ biến, qua kiểm `PERMISSION_MATRIX:DELETE`. Vai trò hệ thống
  (`is_system = true`) không xoá được [Nguồn: 02-bd/database/identity.md:40]; vai trò tuỳ biến còn tài khoản gán cũng không xoá được (Q2).

[Số bản ghi tối đa] Bảng ma trận: số hàng **động theo bảng seed `identity.functions`**, hiện là 10 dòng;
số cột cố định 4 theo `identity.actions`. Tab vai trò: 3 vai trò hệ thống cộng N vai trò tuỳ biến, không
phân trang.

[Nguồn: 01-rd/screens/admin/ADM0202_permission_matrix.md:13-15,39-44; 02-bd/database/identity.md:31-66; 02-bd/security/identity.md:31-37]

### 4.2 DTO liên quan

- `RoleDto`
- `PermissionFunctionDto`
- `PermissionActionDto`
- `RolePermissionMatrixDto`

`[Suy luận]` — tên DTO do BD này đề xuất, `03-dd/api/identity.md` chốt lại.

### 4.3 Bảng dữ liệu liên quan (6)

| NO | Bảng | Ghi chú |
| --: | :--- | :--- |
| 1 | `identity.roles` | [Nguồn: 02-bd/database/identity.md:31-43] |
| 2 | `identity.functions` | [Nguồn: 02-bd/database/identity.md:45-49] |
| 3 | `identity.actions` | [Nguồn: 02-bd/database/identity.md:51] |
| 4 | `identity.permissions` | [Nguồn: 02-bd/database/identity.md:55-66] |
| 5 | `identity.system_audit_logs` | [Nguồn: 02-bd/database/identity.md:103-107] |
| 6 | `identity.users` | Chỉ đọc, đếm số người dùng đang gán từng vai trò (`RoleDto.userCount`) và chặn xoá khi còn người gán [Nguồn: 02-bd/database/identity.md:17] |

Màn này không đọc hay ghi Redis trực tiếp. Sau mỗi thay đổi `permissions`, tầng ứng dụng tự invalidate
cache `identity:permcache:<roleId>` ở mọi instance — việc của BD module, không phải của màn
[Nguồn: 02-bd/security/identity.md:35-37].

### 4.4 Vùng bố cục

Đối chiếu `09-layoutBase/Admin - Ma trận phân quyền.dc.html` — bằng chứng bố cục chỉ-đọc, **không phải**
design system cuối cùng.

| Vùng | Vị trí trong prototype | Nội dung |
| :--- | :--- | :--- |
| Thanh điều hướng bên trái (khung chung Admin) | `:70-146` | Nhóm "Hệ thống" đang mở, mục con "Ma trận phân quyền" đang chọn — dùng lại khung chung của mọi màn Admin |
| Thanh tiêu đề dính trên | `:150-161` | Tiêu đề, mô tả phụ. Nút "Lưu thay đổi" của prototype `dc.html` **đã bỏ** (2026-10-08, Q1) |
| Banner ghi chú phạm vi | `:163-165` | Nhắc Function và Action là seed chỉ đọc, và quyền học tập cơ bản của STUDENT không đi qua ma trận |
| Khối vai trò | `:167-190` | Tab chọn vai trò, form nội tuyến thêm vai trò, nút "Xoá vai trò này" hoặc nhãn "Vai trò hệ thống · không thể xoá" |
| Banner riêng khi chọn STUDENT | `:192-196` | Giải thích vì sao các ô bị khoá, hiển thị có điều kiện |
| Bảng ma trận | `:198-224` | Cột đầu là tên Function kèm mã, 4 cột Action, mỗi ô là một nút vuông bật hoặc tắt |
| Chân trang (khung chung Admin) | `:227-240` | Phiên bản, ghi chú "Mọi thay đổi ghi vào Nhật ký hệ thống", liên kết phụ — dùng lại khung chung |

Bảng ma trận đặt trong vùng cuộn ngang, chiều rộng tối thiểu 640px (`:198-199`): số cột cố định nên khi màn
hẹp thì cuộn ngang thay vì ép chữ xuống dòng. Giữ nguyên cấu trúc này khi dựng Next.js; không quy định màu
sắc, khoảng cách hay typography ở BD.

Chân trang: dùng chân trang thật của khung chung Admin, chốt 2026-09-21
[Nguồn: 02-bd/screens/admin/_shell.md mục 9]. Ghi chú "Mọi thay đổi ghi vào Nhật ký hệ thống" ở dòng
`:227-240` của riêng prototype màn này **không** có trong chân trang dùng chung (mục 9.1 chỉ có
phiên bản/trạng thái/4 liên kết) — nếu cần giữ ghi chú đó thì đây là nội dung đặc thù của màn, để ngỏ
cho DD quyết định thêm dòng phụ hay bỏ, không phải phần của khung chung.

### 4.5 Cấu trúc slice FSD [Nội bộ]

| Khối | Slice dự kiến | Căn cứ |
| :--- | :--- | :--- |
| Trang | `views/admin/permission-matrix` | Quy ước FSD của dự án |
| Khung Admin | Dùng lại `widgets/app-shell` | [Nguồn: 02-bd/screens/admin/_shell.md:20-24] |
| Khối vai trò | `features/role-tabs` + `entities/role` | Prototype `:167-190` |
| Tạo và xoá vai trò | `features/role-create`, `features/role-delete` | Prototype `:174-189` |
| Bảng ma trận | `widgets/permission-matrix-table` + `entities/permission` | Prototype `:198-224` |

`[Suy luận]` — ánh xạ slice do BD đề xuất, DD màn hình chốt lại.

> [Nội bộ] Ảnh minh hoạ đặt ở `08-diagram/02-bd/screens/admin/`, chụp bằng Playwright trên ứng dụng Next.js
> thật khi đã có mã chạy được. Chưa có thì tham chiếu prototype, không vẽ tay.

---

## Sheet 5. Danh sách item màn hình

> Quy ước ID, bộ thẻ và giá trị chuẩn: `02-bd/_rules/bd-template-9sheet.md` mục 2, 3, 4.

### Khu vực A — Thanh tiêu đề

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Thanh tiêu đề | | | | | | | | | | | | | |
| | 1 | Tiêu đề màn | `adminPermissionMatrix.header.title` | - | - | Label | String | - | - | O | Ma trận phân quyền | - | Tên màn hiển thị cố định<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] - |
| | 2 | Mô tả phụ | `adminPermissionMatrix.header.subtitle` | - | - | Label | String | - | - | O | Role × Function × Action · thay đổi có hiệu lực ngay | - | Mô tả ngắn phạm vi màn<br>[Nguồn giá trị] Nhãn tĩnh i18n, câu chữ theo prototype `:153`<br>[EVT liên quan] - |
| | 3 | ~~Lưu thay đổi~~ | `adminPermissionMatrix.header.btnSave` | - | - | Button | - | - | - | - | - | - | **Đã bỏ 2026-10-08 (Q1 đã chốt: lưu tức thời từng ô).** Giữ số thứ tự để các tham chiếu không phải đánh số lại; không dựng control này<br>[EVT liên quan] EVT-9 (đã bỏ) |

### Khu vực B — Ghi chú phạm vi

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Ghi chú phạm vi | | | | | | | | | | | | | |
| | 1 | Ghi chú seed và Lớp 1 | `adminPermissionMatrix.notice.scope` | - | - | Label | String | - | - | O | Function và Action là dữ liệu seed cố định, chỉ đọc — chỉ Role tạo/sửa/xoá được. Quyền học tập cơ bản của STUDENT không đi qua ma trận này. | - | Banner cố định đầu vùng nội dung<br>[Nguồn giá trị] Nhãn tĩnh i18n, câu chữ theo prototype `:163-165`<br>[EVT liên quan] - |
| | 2 | Ghi chú riêng vai trò STUDENT | `adminPermissionMatrix.notice.studentReadOnly` | - | - | Label | String | - | - | O | STUDENT có sẵn toàn bộ quyền học tập cơ bản theo vai trò, không đi qua ma trận này — các ô dưới đây chỉ để tham khảo và không chỉnh được. | - | Giải thích lý do khoá ô, tránh hiểu nhầm STUDENT không có quyền gì. Chỉ cho vai trò hệ thống `STUDENT`<br>[Nguồn giá trị] Nhãn tĩnh i18n, câu chữ theo prototype `:192-196`<br>[EVT liên quan] EVT-2 |

### Khu vực C — Vai trò

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Vai trò | | | | | | | | | | | | | |
| | 1 | Tab vai trò | `adminPermissionMatrix.role.tabs` | `identity.roles` | `code`, `name` | List | List | - | - | I/O | Vai trò `INSTRUCTOR` (vai trò hệ thống đầu tiên chỉnh được; STUDENT chỉ đọc nên không mở mặc định) | - | Danh sách vai trò dạng chip, chọn đúng một vai trò tại một thời điểm. Tối thiểu 3 vai trò hệ thống cộng N vai trò tuỳ biến<br>[Nguồn giá trị] Kết quả gọi `ListRoles`<br>[EVT liên quan] EVT-1, EVT-2 |
| | 2 | Thêm vai trò mới | `adminPermissionMatrix.role.btnAdd` | - | - | Button | - | - | - | I | - | + Vai trò mới | Mở form nội tuyến nhập tên vai trò<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] EVT-3 |
| | 3 | Tên vai trò mới | `adminPermissionMatrix.role.inputNewName` | `identity.roles` | `name` | TextBox | String | 50 | Có | I | rỗng | - | Ô nhập tên vai trò tuỳ biến. Độ dài 50 là `[Suy luận]` — cột `name` khai kiểu VARCHAR không kèm độ dài, DD chốt lại. **Prototype 2026-10-08 đã chặn** tên dài hơn 50 ký tự (hằng `ROLE_NAME_MAX`) [Nguồn: 05-coding/frontend/src/views/admin/permission-matrix/model/guards.ts:3,16-28]<br>[Nguồn giá trị] Người dùng nhập<br>[EVT liên quan] EVT-4 |
| | 4 | Tạo vai trò | `adminPermissionMatrix.role.btnCreateConfirm` | - | - | Button | - | - | - | I | - | Tạo | Xác nhận tạo vai trò với tên đã nhập. Vai trò tạo ra mang `base_category` do người dùng chọn ở ô "Nhóm cơ bản của vai trò mới" (mặc định `STUDENT`, owner chốt 2026-10-08, Q6) [Nguồn: 05-coding/frontend/src/views/admin/permission-matrix/ui/admin-permission-matrix-view.tsx:101-103]<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] EVT-5 |
| | 5 | Huỷ thêm vai trò | `adminPermissionMatrix.role.btnCreateCancel` | - | - | Button | - | - | - | I | - | Huỷ | Đóng form nội tuyến, bỏ tên đang nhập<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] EVT-6 |
| | 6 | Xoá vai trò này | `adminPermissionMatrix.role.btnDelete` | `identity.roles` | `is_system` | Button | - | - | - | I | - | Xoá vai trò này | Xoá vai trò tuỳ biến đang chọn. Vai trò còn tài khoản gán (`userCount > 0`) thì không mở hộp xác nhận mà hiện toast cảnh báo "Vai trò {role} đang được gán cho {count} người dùng, không thể xoá. Chuyển họ sang vai trò khác trước." [Nguồn: 05-coding/frontend/src/views/admin/permission-matrix/ui/admin-permission-matrix-view.tsx:251-258; 05-coding/frontend/messages/vi.json:845]<br>[Nguồn giá trị] Nhãn tĩnh i18n; chỉ dựng khi `is_system = false` của vai trò đang chọn<br>[EVT liên quan] EVT-7 |
| | 7 | Nhãn vai trò hệ thống | `adminPermissionMatrix.role.labelSystemRole` | `identity.roles` | `is_system` | Label | String | - | - | O | Vai trò hệ thống · không thể xoá | - | Thay chỗ nút xoá khi vai trò đang chọn là vai trò hệ thống<br>[Nguồn giá trị] Nhãn tĩnh i18n, hiển thị theo `is_system = true`<br>[EVT liên quan] EVT-2 |
| | 8 | Nhóm cơ bản của vai trò mới | `adminPermissionMatrix.role.selectBaseCategory` | `identity.roles` | `base_category` | ComboBox | Enum | - | Có | I | `STUDENT` | `STUDENT` / `INSTRUCTOR` / `ADMIN` hiển thị "Nhóm Người học" / "Nhóm Giảng viên" / "Nhóm Quản trị" | Chọn nhóm cơ bản cho vai trò đang tạo; quyết định khu vực vai trò được vào (Lớp 1). Mặc định `STUDENT` (owner chốt 2026-10-08, Q6 hướng 1) [Nguồn: 05-coding/frontend/src/views/admin/permission-matrix/ui/admin-permission-matrix-view.tsx, ô `SelectField` trong form nội tuyến]<br>[Nguồn giá trị] Người dùng chọn<br>[EVT liên quan] EVT-5 |

### Khu vực D — Bảng ma trận

Cấu trúc bảng: mỗi dòng là một Function, mỗi cột Action là một `ListColumn` riêng — không liệt kê từng ô.
Số hàng **động** theo bảng seed `identity.functions` (hiện 10 dòng); số cột **cố định 4** theo
`identity.actions` [Nguồn: 02-bd/database/identity.md:45-51].

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Bảng ma trận | | | | | | | | | | | | | |
| | 1 | Bảng ma trận quyền | `adminPermissionMatrix.matrix.list` | `identity.permissions` | - | List | List | - | - | I/O | 10 dòng | - | Mỗi dòng một Function, mỗi cột một Action. Prototype vẽ 11 dòng vì dựng trước khi bỏ `REJUDGE_MANAGEMENT`<br>[Nguồn giá trị] `GetPermissionCatalog` cho hàng và cột, `GetRolePermissions` cho giá trị ô<br>[EVT liên quan] EVT-1, EVT-2 |
| | 2 | Tên chức năng | `adminPermissionMatrix.matrix.col.functionName` | `identity.functions` | `name` | ListColumn | String | - | - | O | - | - | Tên chức năng hiển thị ở cột đầu<br>[Nguồn giá trị] Cột `name`<br>[EVT liên quan] - |
| | 3 | Mã chức năng | `adminPermissionMatrix.matrix.col.functionKey` | `identity.functions` | `code` | ListColumn | String | - | - | O | - | Chữ đều (mono), in hoa | Mã seed hiển thị dưới tên chức năng để đối chiếu với mã nguồn<br>[Nguồn giá trị] Cột `code`<br>[EVT liên quan] - |
| | 4 | Ô quyền Tạo | `adminPermissionMatrix.matrix.col.actionCreate` | `identity.permissions` | `granted` | Toggle | Boolean | - | - | I/O | Tắt | Bật hiện dấu tick, tắt hiện ô viền rỗng | Ô của bộ ba (vai trò đang chọn, Function của dòng, Action `CREATE`)<br>[Nguồn giá trị] `granted` của dòng khớp `(role_id, function_id, action_id)`; không có dòng nào thì coi là Tắt<br>[EVT liên quan] EVT-8 |
| | 5 | Ô quyền Xem | `adminPermissionMatrix.matrix.col.actionRead` | `identity.permissions` | `granted` | Toggle | Boolean | - | - | I/O | Tắt | Bật hiện dấu tick, tắt hiện ô viền rỗng | Ô của bộ ba với Action `READ`<br>[Nguồn giá trị] Như ô quyền Tạo, đổi Action thành `READ`<br>[EVT liên quan] EVT-8 |
| | 6 | Ô quyền Sửa | `adminPermissionMatrix.matrix.col.actionUpdate` | `identity.permissions` | `granted` | Toggle | Boolean | - | - | I/O | Tắt | Bật hiện dấu tick, tắt hiện ô viền rỗng | Ô của bộ ba với Action `UPDATE`<br>[Nguồn giá trị] Như ô quyền Tạo, đổi Action thành `UPDATE`<br>[EVT liên quan] EVT-8 |
| | 7 | Ô quyền Xoá | `adminPermissionMatrix.matrix.col.actionDelete` | `identity.permissions` | `granted` | Toggle | Boolean | - | - | I/O | Tắt | Bật hiện dấu tick, tắt hiện ô viền rỗng | Ô của bộ ba với Action `DELETE`<br>[Nguồn giá trị] Như ô quyền Tạo, đổi Action thành `DELETE`<br>[EVT liên quan] EVT-8 |

[Nguồn: 09-layoutBase/Admin - Ma trận phân quyền.dc.html:150-224,417-436; 02-bd/database/identity.md:31-66; 01-rd/screens/admin/ADM0202_permission_matrix.md:32-44]

---

## Sheet 6. Đặc tả điều khiển item

> Ký hiệu cột `Hiển thị` và thứ tự thẻ: `02-bd/_rules/bd-template-9sheet.md` mục 2, 3. Dùng đúng NO, tên
> item và thứ tự của Sheet 5. Lỗi nhập liệu ghi ở Sheet 9, xử lý nghiệp vụ ghi ở Sheet 8.

### Khu vực A — Thanh tiêu đề

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Thanh tiêu đề | | | | |
| | 1 | Tiêu đề màn | Có | - |
| | 2 | Mô tả phụ | Có | - |
| | 3 | ~~Lưu thay đổi~~ | Không còn | Đã bỏ 2026-10-08 (Q1 đã chốt: lưu tức thời từng ô); không dựng nút này. |

### Khu vực B — Ghi chú phạm vi

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Ghi chú phạm vi | | | | |
| | 1 | Ghi chú seed và Lớp 1 | Có | - |
| | 2 | Ghi chú riêng vai trò STUDENT | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị khi vai trò đang chọn là vai trò hệ thống `STUDENT` (theo `code`, không theo `base_category`) [Nguồn: 05-coding/frontend/src/views/admin/permission-matrix/ui/admin-permission-matrix-view.tsx:65,266-270]. |

### Khu vực C — Vai trò

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Vai trò | | | | |
| | 1 | Tab vai trò | Có | [Điều kiện hiển thị] Trong lúc tải hiển thị khung chờ đúng 3 chip.<br>[Điều kiện kích hoạt] Kích hoạt sau khi tải xong danh sách vai trò.<br>[Tự động đặt] Vai trò vừa tạo trở thành vai trò đang chọn. |
| | 2 | Thêm vai trò mới | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị khi form nội tuyến đang đóng; form mở thì nút này được thay bằng chính form đó. |
| | 3 | Tên vai trò mới | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị khi form nội tuyến đang mở.<br>[Tự động đặt] Mở form thì ô được xoá rỗng và nhận con trỏ nhập. |
| | 4 | Tạo vai trò | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị khi form nội tuyến đang mở.<br>[Điều kiện kích hoạt] Không kích hoạt khi tên vai trò rỗng hoặc chỉ gồm khoảng trắng [Nguồn: 05-coding/frontend/src/views/admin/permission-matrix/ui/admin-permission-matrix-view.tsx:199]. Tên quá dài hoặc trùng thì nút vẫn bấm được; lỗi hiện qua viền đỏ và toast (Sheet 9 NO 3, 4, 11). |
| | 5 | Huỷ thêm vai trò | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị khi form nội tuyến đang mở. |
| | 6 | Xoá vai trò này | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị khi vai trò đang chọn có `is_system = false`.<br>[Điều kiện kích hoạt] Luôn bấm được; khi `userCount > 0` thì bấm chỉ hiện toast cảnh báo, không mở hộp xác nhận (Q2) [Nguồn: 05-coding/frontend/src/views/admin/permission-matrix/ui/admin-permission-matrix-view.tsx:251-258]. |
| | 7 | Nhãn vai trò hệ thống | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị khi vai trò đang chọn có `is_system = true`, thay chỗ nút "Xoá vai trò này". |
| | 8 | Nhóm cơ bản của vai trò mới | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị khi form nội tuyến đang mở.<br>[Tự động đặt] Mở form thì đặt `STUDENT`; đóng form hoặc tạo xong thì đặt lại `STUDENT`. |

### Khu vực D — Bảng ma trận

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Bảng ma trận | | | | |
| | 1 | Bảng ma trận quyền | Có | [Điều kiện hiển thị] Trong lúc tải hiển thị khung chờ đúng số dòng của danh mục Function đã tải (hiện 10). Đổi tab vai trò thì chỉ nạp lại giá trị ô, giữ nguyên hàng và cột. |
| | 2 | Tên chức năng | Có | - |
| | 3 | Mã chức năng | Có | - |
| | 4 | Ô quyền Tạo | Có | [Điều kiện kích hoạt] Không kích hoạt khi vai trò đang chọn là vai trò hệ thống `STUDENT` (theo `code`; vai trò tuỳ biến luôn kích hoạt, kể cả khi nhóm cơ bản là `STUDENT`) [Nguồn: 05-coding/frontend/src/views/admin/permission-matrix/ui/admin-permission-matrix-view.tsx:65-68,157].<br>[Tự động đặt] Bấm một lần thì đảo trạng thái ô và ghi ngay (không có bước lưu riêng). Hai ràng buộc chạy trước: chặn cứng ô `PERMISSION_MATRIX:UPDATE` cuối cùng của vai trò quản trị (Sheet 9 NO 9) và hộp xác nhận khi gỡ quyền của chính vai trò mình đang giữ (NO 8). |
| | 5 | Ô quyền Xem | Có | [Điều kiện kích hoạt] Như ô quyền Tạo.<br>[Tự động đặt] Như ô quyền Tạo. |
| | 6 | Ô quyền Sửa | Có | [Điều kiện kích hoạt] Như ô quyền Tạo.<br>[Tự động đặt] Như ô quyền Tạo. |
| | 7 | Ô quyền Xoá | Có | [Điều kiện kích hoạt] Như ô quyền Tạo.<br>[Tự động đặt] Như ô quyền Tạo. |

[Nguồn: 09-layoutBase/Admin - Ma trận phân quyền.dc.html:174-189,192-196,318-327,425-432,455-456,483-484; prototype Next.js 2026-10-08: 05-coding/frontend/src/views/admin/permission-matrix/ui/admin-permission-matrix-view.tsx:64-111,199,257-270]

---

## Sheet 7. Danh sách truyền dữ liệu

### 7.1 Trường DTO

| NO | DTO | Trường DTO | Kiểu | Bảng DB | Cột DB | Item màn | Hiển thị | Ghi chú |
| --: | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :--- |
| 1 | `RoleDto` | `id` | UUID | `identity.roles` | `id` | - | Không | [Nguồn] Phản hồi của `ListRoles`<br>[Đích] Tham số của `GetRolePermissions`, `UpdateRolePermission`, `DeleteRole`. Không hiển thị trên màn. |
| 2 | `RoleDto` | `code` | String | `identity.roles` | `code` | Vai trò "Tab vai trò" | Có | [Chuyển đổi] Ba vai trò hệ thống hiển thị bằng chính `code`; vai trò tuỳ biến hiển thị `name`. |
| 3 | `RoleDto` | `name` | String | `identity.roles` | `name` | Vai trò "Tab vai trò", "Tên vai trò mới" | Có | [Nguồn] Giá trị người dùng nhập khi tạo<br>[Đích] Tham số của `CreateRole`. |
| 4 | `RoleDto` | `baseCategory` | Enum | `identity.roles` | `base_category` | Vai trò "Nhóm cơ bản của vai trò mới" | Có | [Chuyển đổi] Không hiển thị trực tiếp dưới dạng chữ và **không** quyết định việc khoá ô (khoá theo `code` vai trò hệ thống `STUDENT`, Q6 đã chốt); chỉ quyết định khu vực vai trò được vào (Lớp 1). |
| 5 | `RoleDto` | `isSystem` | Boolean | `identity.roles` | `is_system` | Vai trò "Xoá vai trò này", "Nhãn vai trò hệ thống" | Có | [Chuyển đổi] `true` thì dựng nhãn tĩnh thay nút xoá; `false` thì dựng nút xoá. |
| 6 | `PermissionFunctionDto` | `code` | String | `identity.functions` | `code` | Ma trận "Mã chức năng" | Có | [Nguồn] Phản hồi của `GetPermissionCatalog`<br>[Đích] Khoá xác định hàng khi gọi `UpdateRolePermission`. |
| 7 | `PermissionFunctionDto` | `name` | String | `identity.functions` | `name` | Ma trận "Tên chức năng" | Có | [Nguồn] Phản hồi của `GetPermissionCatalog` |
| 8 | `PermissionActionDto` | `code` | Enum | `identity.actions` | `code` | Ma trận — tiêu đề 4 cột Action | Có | [Chuyển đổi] `CREATE` / `READ` / `UPDATE` / `DELETE` đổi sang "Tạo" / "Xem" / "Sửa" / "Xoá"<br>[Đích] Khoá xác định cột khi gọi `UpdateRolePermission`. |
| 9 | `RolePermissionMatrixDto` | `roleId` | UUID | `identity.permissions` | `role_id` | - | Không | [Nguồn] Vai trò đang chọn ở tab |
| 10 | `RolePermissionMatrixDto` | `cells` | List | `identity.permissions` | `function_id`, `action_id`, `granted` | Ma trận "Ô quyền Tạo", "Ô quyền Xem", "Ô quyền Sửa", "Ô quyền Xoá" | Có | [Nguồn] Phản hồi của `GetRolePermissions`; mỗi phần tử là bộ ba mã chức năng, mã Action và `granted`<br>[Chuyển đổi] Bộ ba không có trong danh sách trả về thì hiển thị là Tắt, không coi là thiếu dữ liệu. |
| 11 | `RolePermissionMatrixDto` | `updatedAt`, `updatedBy` | Date, UUID | `identity.permissions` | `updated_at`, `updated_by` | - | Không | [Nguồn] Máy chủ tự đặt khi ghi<br>[Đích] Ghi kèm vào `system_audit_logs`. Màn không hiển thị và không gửi lên. |
| 12 | `RoleDto` | `userCount` | Integer | `identity.users` | `role_id` (đếm) | Vai trò "Xoá vai trò này" | Không | [Nguồn] Phản hồi của `ListRoles`; máy chủ đếm `COUNT(*)` các dòng `identity.users` có `role_id` trỏ tới vai trò (chỉ đọc, cùng bảng ở Sheet 7.2 NO 6)<br>[Chuyển đổi] `> 0` thì nút xoá chỉ hiện toast cảnh báo kèm tên vai trò và số này, không mở hộp xác nhận; `= 0` thì mở hộp xác nhận. Không hiển thị thành chữ trên tab [Nguồn: 05-coding/frontend/src/views/admin/permission-matrix/model/types.ts:55; 05-coding/frontend/src/views/admin/permission-matrix/api/__mock__/permission-matrix-mocks.ts:11-16]. |

### 7.2 Truy cập bảng dữ liệu (6)

| NO | Tên logic | Bảng | Repository | CRUD | Mục đích | Ghi chú |
| --: | :--- | :--- | :--- | :-: | :--- | :--- |
| 1 | Vai trò | `identity.roles` | `RoleRepository` | C, R, D | Đọc danh sách vai trò, tạo vai trò tuỳ biến, xoá vai trò tuỳ biến | `ListRoles`: R<br>`CreateRole`: C<br>`DeleteRole`: R, D |
| 2 | Danh mục chức năng | `identity.functions` | `FunctionRepository` | R | Đọc 10 dòng seed để dựng hàng của bảng | `GetPermissionCatalog`: R |
| 3 | Danh mục hành động | `identity.actions` | `ActionRepository` | R | Đọc 4 dòng seed để dựng cột của bảng | `GetPermissionCatalog`: R |
| 4 | Ô ma trận quyền | `identity.permissions` | `PermissionRepository` | C, R, U, D | Đọc ma trận theo vai trò, thêm hoặc sửa ô, xoá toàn bộ ô khi xoá vai trò | `GetRolePermissions`: R<br>`UpdateRolePermission`: C, U<br>`DeleteRole`: D |
| 5 | Nhật ký quản trị | `identity.system_audit_logs` | `SystemAuditLogRepository` | C | Ghi lại mọi thay đổi quyền và vai trò, không ngoại lệ | `CreateRole`, `DeleteRole`, `UpdateRolePermission`: C |
| 6 | Người dùng | `identity.users` | `UserRepository` | R | Đếm số người dùng đang gán vai trò trước khi cho xoá | `ListRoles`: R (đếm `userCount` theo `role_id`)<br>`DeleteRole`: R (đếm lại phía máy chủ, từ chối khi còn người gán; Q2 đã chốt) |

Không có thao tác ghi nào trên `identity.functions` và `identity.actions`: hai bảng là seed chỉ đọc, chỉ
migration mới ghi [Nguồn: 02-bd/database/identity.md:53].

`[Suy luận]` — tên repository do BD này đề xuất, DD module `identity` chốt lại.

### 7.3 Danh sách endpoint [Nội bộ]

> Chỉ tên nghiệp vụ và BC sở hữu. Hợp đồng chi tiết thuộc `03-dd/api/identity.md`.

| NO | Endpoint (tên nghiệp vụ) | Mục đích | BC sở hữu |
| --: | :--- | :--- | :--- |
| 1 | `ListRoles` | Tải danh sách vai trò để dựng tab | `identity` |
| 2 | `GetPermissionCatalog` | Tải danh mục Function và Action seed để dựng hàng và cột | `identity` |
| 3 | `GetRolePermissions` | Tải ma trận quyền của một vai trò | `identity` |
| 4 | `CreateRole` | Tạo vai trò tuỳ biến mới, chưa có quyền nào | `identity` |
| 5 | `DeleteRole` | Xoá vai trò tuỳ biến cùng toàn bộ ô quyền của nó | `identity` |
| 6 | `UpdateRolePermission` | Bật hoặc tắt một ô quyền của bộ ba vai trò, chức năng, hành động | `identity` |

Không có endpoint nào ngoài `identity` — màn này không đọc dữ liệu module khác. Q1 đã chốt lưu tức thời từng
ô (2026-10-08) nên **không** cần endpoint ghi theo lô; `UpdateRolePermission` gọi một lần cho mỗi lần bấm ô.

[Nguồn: 02-bd/database/identity.md:31-66,103-107]

---

## Sheet 8. Danh sách sự kiện

> Bộ thẻ và quy ước cột `Chuyển màn`: `02-bd/_rules/bd-template-9sheet.md` mục 2, 3.

### Màn chính: Ma trận phân quyền

| NO | Loại | Sự kiện | Chi tiết | Chuyển màn | Gọi API | Tên xử lý | Ghi chú |
| --: | :--- | :--- | :--- | :-: | :-: | :--- | :--- |
| 1 | Màn hình | Khởi tạo màn | Vào màn thì tải vai trò, danh mục seed và ma trận của vai trò đang chọn. | Không | Có | `ListRoles`, `GetPermissionCatalog`, `GetRolePermissions` | [Các bước]<br>1. Kiểm `base_category = ADMIN` ở tầng định tuyến.<br>2. Hiển thị khung chờ cho tab vai trò và bảng ma trận.<br>3. Tải danh sách vai trò và danh mục seed, rồi tải ma trận của vai trò `INSTRUCTOR`, chọn mặc định vì tab STUDENT chỉ đọc (owner đồng ý 2026-10-08 theo đề xuất; prototype `admin-permission-matrix-view.tsx` `useState("INSTRUCTOR")`).<br>[Khi thành công] Bảng hiển thị đủ hàng Function và 4 cột Action.<br>[Khi lỗi] Hiển thị thông báo lỗi tại khối tải thất bại, không rời màn. |
| 2 | Tab | Chọn vai trò | Bấm một chip vai trò. | Không | Có | `GetRolePermissions` | [Các bước]<br>1. Đặt vai trò đang chọn.<br>2. Tải lại giá trị ô của vai trò đó.<br>[Ghi chú] Không hỏi xác nhận khi đổi tab: mỗi ô đã lưu tức thời (Q1 đã chốt 2026-10-08), không còn thay đổi chưa lưu.<br>[Khi thành công] Bảng đổi sang ma trận của vai trò mới. Vai trò hệ thống `STUDENT` thì hiện banner chỉ-đọc và khoá toàn bộ ô (theo `code`, không theo nhóm cơ bản); vai trò hệ thống thì nhãn "Vai trò hệ thống · không thể xoá" thay nút xoá.<br>[Khi lỗi] Giữ nguyên vai trò đang chọn, hiện toast lỗi. |
| 3 | Nút | Mở form thêm vai trò | Bấm "+ Vai trò mới". | Không | Không | - | [Các bước]<br>1. Thay nút bằng form nội tuyến gồm ô nhập tên và hai nút "Tạo", "Huỷ".<br>[Khi thành công] Form hiển thị, ô nhập rỗng và nhận con trỏ; phần còn lại của màn vẫn tương tác được. |
| 4 | Nhập liệu | Nhập tên vai trò mới | Gõ vào ô "Tên vai trò mới". | Không | Không | - | [Các bước]<br>1. Ghi nhận tên vào trạng thái biên soạn.<br>[Khi thành công] Tên không rỗng thì kích hoạt nút "Tạo".<br>[Khi lỗi] Tên rỗng hoặc chỉ gồm khoảng trắng thì không kích hoạt nút "Tạo". |
| 5 | Nút | Tạo vai trò | Bấm "Tạo" trong form nội tuyến. | Không | Có | `CreateRole` | [Các bước]<br>1. Kiểm tên không rỗng, không quá 50 ký tự, không trùng (so mã sinh ra **và** nhãn, không phân biệt hoa thường; Sheet 9 NO 3, 4, 11) [Nguồn: 05-coding/frontend/src/views/admin/permission-matrix/model/guards.ts:16-28].<br>2. Gửi yêu cầu tạo vai trò; vai trò mới mang `base_category` đã chọn ở form, mặc định `STUDENT` (owner chốt 2026-10-08, Q6).<br>3. Đóng form, thêm chip mới vào tab và chọn chính vai trò đó.<br>[Khi thành công] Bảng ma trận của vai trò mới hiển thị toàn bộ ô ở trạng thái Tắt, không phải khoá tương tác.<br>[Khi lỗi] Giữ form mở, giữ nguyên tên đã nhập, ô nhập đổi viền đỏ và hiện một toast lỗi.<br>[Thông báo hoàn tất] Toast "Đã tạo vai trò mới." |
| 6 | Nút | Huỷ thêm vai trò | Bấm "Huỷ" trong form nội tuyến. | Không | Không | - | [Các bước]<br>1. Đóng form và xoá tên đang nhập.<br>[Khi thành công] Nút "+ Vai trò mới" hiện lại; tab vai trò và bảng ma trận không đổi. |
| 7 | Nút | Xoá vai trò đang chọn | Bấm "Xoá vai trò này". | Không | Có | `DeleteRole` | [Các bước]<br>1. Kiểm `userCount` của vai trò đang chọn. Còn tài khoản gán (`userCount > 0`) thì **chặn**: hiện toast cảnh báo "Vai trò {role} đang được gán cho {count} người dùng, không thể xoá. Chuyển họ sang vai trò khác trước.", không mở hộp xác nhận, không gọi API (Q2 đã chốt) [Nguồn: 05-coding/frontend/src/views/admin/permission-matrix/ui/admin-permission-matrix-view.tsx:251-258; 05-coding/frontend/messages/vi.json:845].<br>2. `userCount = 0` thì hỏi xác nhận "Vai trò này và toàn bộ ô quyền của nó sẽ bị xoá, không hoàn tác được." [Nguồn: 05-coding/frontend/src/views/admin/permission-matrix/ui/admin-permission-matrix-view.tsx:297-307; 05-coding/frontend/messages/vi.json:815].<br>3. Gửi yêu cầu xoá; máy chủ đếm lại và từ chối nếu lúc đó đã có người gán (Sheet 9 NO 7).<br>4. Bỏ chip khỏi tab và chuyển sang vai trò `INSTRUCTOR`.<br>[Khi xác nhận] Bắt buộc hỏi xác nhận vì thao tác không hoàn tác được.<br>[Khi thành công] Tab không còn vai trò đó; bảng hiển thị ma trận của vai trò được chọn thay thế.<br>[Khi lỗi] Máy chủ từ chối (vd. vừa có người được gán vai trò), giữ nguyên tab và bảng, hiện toast lỗi.<br>[Thông báo hoàn tất] Toast "Đã xoá vai trò." |
| 8 | Ô ma trận | Bật hoặc tắt một ô quyền | Bấm một ô trong bảng ma trận. | Không | Có | `UpdateRolePermission` | [Các bước]<br>1. Vai trò đang chọn là vai trò hệ thống `STUDENT` (theo `code`) thì bỏ qua, ô không nhận thao tác; vai trò tuỳ biến luôn nhận, kể cả khi nhóm cơ bản là `STUDENT`. Hệ quả: vai trò nhóm `STUDENT` được cấp Function của khu giảng viên vẫn không vào được khu đó vì Lớp 1 kiểm theo `base_category`.<br>2. Đảo trạng thái ô trên giao diện.<br>3. Gửi yêu cầu cập nhật ô; máy chủ ghi `permissions`, ghi `system_audit_logs` và invalidate cache quyền của vai trò đó.<br>[Khi xác nhận] Tắt một ô thuộc chính vai trò mà người đang thao tác đang giữ thì **hỏi xác nhận** (Sheet 9 dòng 8, thay đổi có hiệu lực ngay); tắt ô `PERMISSION_MATRIX:UPDATE` cuối cùng của mọi vai trò `ADMIN` thì **chặn cứng**, toast, không đổi gì (Sheet 9 dòng 9, Q4 đã chốt) [Nguồn: 05-coding/frontend/src/views/admin/permission-matrix/ui/admin-permission-matrix-view.tsx:64-71,257-270; 05-coding/frontend/src/views/admin/permission-matrix/model/guards.ts:38-60].<br>[Khi thành công] Ô giữ trạng thái mới; quyền có hiệu lực ngay, không cần khởi động lại dịch vụ [Nguồn: 02-bd/security/identity.md:35-37].<br>[Khi lỗi] Trả ô về trạng thái trước khi bấm và hiện toast lỗi, không để giao diện lệch với dữ liệu thật. |
| 9 | Nút | ~~Lưu thay đổi~~ | **Đã bỏ 2026-10-08** (Q1 đã chốt: lưu tức thời từng ô, thanh tiêu đề không còn nút lưu). | Không | Không | - | Giữ số thứ tự để các tham chiếu EVT ở Sheet 5, 9 không phải đánh số lại; không có hành vi nào gắn với EVT này. |
| 10 | Liên kết | Rời màn qua thanh điều hướng | Bấm một mục khác trên thanh điều hướng bên trái. | Có | Không | - | [Các bước]<br>1. Điều hướng sang màn Admin tương ứng.<br>[Ghi chú] Không hỏi xác nhận khi rời màn: mỗi ô đã lưu tức thời, không có thay đổi chưa lưu (Q1 đã chốt, Q5 đã đóng).<br>[Khi thành công] Mở màn được chọn. |

[Nguồn: 09-layoutBase/Admin - Ma trận phân quyền.dc.html:160,171,177-178,182,185,217,318-327,464-484; EVT-9 đã bỏ theo owner 2026-10-08; 01-rd/screens/admin/ADM0202_permission_matrix.md:48-53; 02-bd/security/identity.md:35-37,51-53]

---

## Sheet 9. Đặc tả kiểm tra

> Bộ thẻ và quy ước mã thông báo: `02-bd/_rules/bd-template-9sheet.md` mục 2, 4. Sheet này ghi **điều kiện
> kiểm và thông báo**; tiêu chí nghiệm thu `AC-nn` thuộc `04-tdd/admin_permission_matrix.md`, không lặp lại
> ở đây.

| NO | Loại | Tóm tắt kiểm | Chi tiết | Mức | Mã thông báo | Ghi chú | EVT gọi | Thứ tự |
| --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: |
| 1 | Kiểm quyền | Quyền truy cập màn | [Nội dung kiểm] Người dùng có `base_category` khác `ADMIN` thì không được vào màn.<br>[Nơi thực thi] Chặn ở cả tầng định tuyến phía giao diện và tầng phân quyền phía máy chủ, không chỉ ẩn giao diện. | Lỗi | Chưa có mã thông báo | Nội dung "Bạn không có quyền truy cập chức năng này." Đây là kiểm Lớp 1 bằng `base_category`, **không** tra bảng `permissions` [Nguồn: 02-bd/security/identity.md:31-34]. | EVT-1 | 1 |
| 2 | Kiểm quyền | Quyền cho thao tác ghi | [Nội dung kiểm] Thao tác ghi phải qua kiểm `PERMISSION_MATRIX:CREATE`, `PERMISSION_MATRIX:UPDATE` hoặc `PERMISSION_MATRIX:DELETE` tương ứng.<br>[Nơi thực thi] Tầng ứng dụng phía máy chủ. | Lỗi | Chưa có mã thông báo | Nội dung "Bạn không có quyền thực hiện thao tác này." Không giả định "vào được màn nghĩa là toàn quyền trên màn": một vai trò tuỳ biến vẫn có thể có `base_category = ADMIN` mà bị giới hạn quyền `PERMISSION_MATRIX` [Nguồn: 02-bd/security/identity.md:25-26]. | EVT-5, EVT-7, EVT-8 | 2 |
| 3 | Kiểm nhập liệu | Tên vai trò bắt buộc | [Nội dung kiểm] Tên vai trò rỗng hoặc chỉ gồm khoảng trắng thì không cho tạo.<br>[Nơi thực thi] Màn hình và máy chủ.<br>[Tiêu điểm] Viền ô "Tên vai trò mới" + toast. | Lỗi | Chưa có mã thông báo | Nội dung "Tên vai trò không được để trống." Prototype đã chặn sẵn ở giao diện [Nguồn: 09-layoutBase/Admin - Ma trận phân quyền.dc.html:465-466]. | EVT-5 | 1 |
| 4 | Kiểm nhập liệu | Tên vai trò không trùng | [Nội dung kiểm] Tên vai trò sinh ra `code` trùng một vai trò đã có thì không cho tạo.<br>[Nơi thực thi] Máy chủ.<br>[Tiêu điểm] Viền ô "Tên vai trò mới" + toast. | Lỗi | Chưa có mã thông báo | Nội dung "Tên vai trò đã tồn tại." Cột `roles.code` là unique [Nguồn: 02-bd/database/identity.md:37]. **Prototype 2026-10-08:** so cả mã sinh ra (`role_` + tên chữ thường, khoảng trắng thành `_`) **và nhãn** của vai trò có sẵn, không phân biệt hoa thường, để tên "admin" không che vai trò hệ thống `ADMIN` [Nguồn: 05-coding/frontend/src/views/admin/permission-matrix/model/guards.ts:7-28]. | EVT-5 | 2 |
| 5 | Kiểm nghiệp vụ | Không sửa ô của vai trò Lớp 1 | [Nội dung kiểm] Vai trò đang chọn là vai trò hệ thống `STUDENT` (theo `code`, không theo `base_category`) thì mọi thao tác ghi ô đều bị từ chối; vai trò tuỳ biến luôn sửa được, kể cả nhóm `STUDENT` (Q6 đã chốt 2026-10-08).<br>[Nơi thực thi] Màn hình khoá ô, máy chủ kiểm lại. | Lỗi | Chưa có mã thông báo | Nội dung "Quyền học tập cơ bản của STUDENT không cấu hình qua ma trận này." Giao diện khoá ô thay vì ẩn bảng, để tránh hiểu nhầm STUDENT không có quyền gì [Nguồn: 01-rd/screens/admin/ADM0202_permission_matrix.md:35-38; 05-coding/frontend/src/views/admin/permission-matrix/ui/admin-permission-matrix-view.tsx:65-68]. Hệ quả cần ghi rõ: vai trò nhóm `STUDENT` được cấp Function của khu giảng viên vẫn không vào được khu đó, vì Lớp 1 kiểm theo `base_category`. | EVT-8 | 1 |
| 6 | Kiểm nghiệp vụ | Không xoá vai trò hệ thống | [Nội dung kiểm] Vai trò có `is_system = true` thì không cho xoá.<br>[Nơi thực thi] Màn hình ẩn nút xoá, máy chủ kiểm lại. | Lỗi | Chưa có mã thông báo | Nội dung "Vai trò hệ thống không thể xoá." Máy chủ vẫn phải kiểm dù giao diện đã ẩn nút [Nguồn: 02-bd/database/identity.md:40]. | EVT-7 | 1 |
| 7 | Kiểm nghiệp vụ | Xoá vai trò đang có người dùng gán | [Nội dung kiểm] Còn ít nhất một người dùng có `users.role_id` trỏ tới vai trò này thì dừng xoá (chặn, không tự chuyển người dùng sang vai trò khác; Q2 đã chốt 2026-10-08).<br>[Nơi thực thi] Màn hình (theo `userCount`, toast, không mở hộp xác nhận) và máy chủ (đếm lại khi nhận `DeleteRole`). | Lỗi | Chưa có mã thông báo | Nội dung "Vai trò {role} đang được gán cho {count} người dùng, không thể xoá. Chuyển họ sang vai trò khác trước." (đúng chữ prototype [Nguồn: 05-coding/frontend/messages/vi.json:845]); máy chủ trả lỗi cùng ý nghĩa nhưng không phụ thuộc số đếm của giao diện. Vai trò mẫu "Trợ giảng" có 3 tài khoản nên nhánh chặn dựng được [Nguồn: 05-coding/frontend/src/views/admin/permission-matrix/api/__mock__/permission-matrix-mocks.ts:16]. | EVT-7 | 2 |
| 8 | Kiểm nghiệp vụ | Tự gỡ quyền của chính vai trò mình | [Nội dung kiểm] Người đang thao tác tắt một ô thuộc chính vai trò mà tài khoản mình đang giữ thì phải xác nhận lại trước khi ghi.<br>[Nơi thực thi] Màn hình hỏi xác nhận; máy chủ vẫn ghi bình thường sau khi xác nhận. | Cảnh báo | Chưa có mã thông báo | Nội dung "Bạn đang gỡ quyền của chính vai trò mình đang giữ. Thay đổi có hiệu lực ngay và bạn có thể mất quyền truy cập chức năng này. Tiếp tục?" (đổi từ "Sau khi lưu" 2026-10-08 vì không còn bước lưu; đúng chữ prototype [Nguồn: 05-coding/frontend/src/views/admin/permission-matrix/ui/admin-permission-matrix-view.tsx:257-270]). Vẫn là cảnh báo xác nhận, không chặn. `[Suy luận]` — RD không có yêu cầu nào cho tình huống này; BD đề xuất vì đây là thao tác tự khoá mình và không hoàn tác được từ giao diện. Dù xác nhận hay không, thay đổi vẫn ghi `system_audit_logs` không ngoại lệ [Nguồn: 02-bd/security/identity.md:51-53]. | EVT-8 | 3 |
| 9 | Kiểm nghiệp vụ | Gỡ hết quyền `PERMISSION_MATRIX` khỏi vai trò `ADMIN` | [Nội dung kiểm] Thao tác khiến không còn vai trò nào có `base_category = ADMIN` giữ `PERMISSION_MATRIX:UPDATE` thì **chặn**, không chỉ cảnh báo.<br>[Nơi thực thi] Máy chủ, vì phải đếm trên toàn bộ vai trò chứ không chỉ vai trò đang hiển thị. | Lỗi | Chưa có mã thông báo | Nội dung "Phải còn ít nhất một vai trò quản trị giữ quyền sửa ma trận phân quyền." `[Suy luận]` — RD không nêu; BD đề xuất vì nếu gỡ hết thì không còn đường nào cấp lại quyền qua giao diện, chỉ sửa được bằng truy vấn cơ sở dữ liệu trực tiếp. **Đã chốt 2026-10-08 (Q4): chặn cứng**, không còn là đề xuất. Prototype hiện toast cảnh báo và không đổi ô [Nguồn: 05-coding/frontend/src/views/admin/permission-matrix/ui/admin-permission-matrix-view.tsx:64-68; 05-coding/frontend/src/views/admin/permission-matrix/model/guards.ts:30-60]. | EVT-8 | 4 |
| 10 | Kiểm nghiệp vụ | Lỗi hệ thống hoặc lỗi gọi máy chủ | [Nội dung kiểm] Gọi máy chủ thất bại hoặc trả lỗi nghiệp vụ thì dừng thao tác, trả giao diện về trạng thái trước khi thao tác.<br>[Nơi thực thi] Màn hình. | Lỗi | Mã lỗi trong phản hồi | Phản hồi có mã lỗi đã đăng ký thì hiển thị nội dung tương ứng; chưa đăng ký thì hiển thị "Không kết nối được máy chủ." | EVT-1, EVT-2, EVT-5, EVT-7, EVT-8 | 1 |
| 11 | Kiểm nhập liệu | Tên vai trò tối đa 50 ký tự | [Nội dung kiểm] Tên vai trò (sau khi cắt khoảng trắng hai đầu) dài hơn 50 ký tự thì không cho tạo.<br>[Nơi thực thi] Màn hình và máy chủ.<br>[Tiêu điểm] Viền ô "Tên vai trò mới" + toast. | Lỗi | Chưa có mã thông báo | Nội dung "Tên vai trò tối đa 50 ký tự." (chữ prototype, số 50 lấy từ `ROLE_NAME_MAX`) [Nguồn: 05-coding/frontend/src/views/admin/permission-matrix/model/guards.ts:3,16-28; 05-coding/frontend/messages/vi.json — `adminPermissionMatrix.roleNameError.tooLong`]. Con số 50 vẫn là `[Suy luận]`, DD chốt cùng độ dài cột `roles.name`. | EVT-5 | 3 |
| 12 | Kiểm nghiệp vụ | Nhóm cơ bản của vai trò tuỳ biến mới | [Nội dung kiểm] Vai trò tuỳ biến vừa tạo phải có một `base_category`. Form có ô chọn nhóm, mặc định `STUDENT` (owner chốt 2026-10-08, Q6 hướng 1) [Nguồn: 05-coding/frontend/src/views/admin/permission-matrix/ui/admin-permission-matrix-view.tsx, `newRoleCategory`].<br>[Nơi thực thi] Máy chủ kiểm giá trị thuộc ba nhóm hợp lệ. | Cảnh báo | Chưa có mã thông báo | Không có thông báo cho người dùng. Nhóm đã chọn quyết định vai trò mới vào được khu vực nào; việc khoá ô không phụ thuộc nhóm này (Q6 đã chốt). | EVT-5 | 4 |

Cột `Thứ tự` là thứ tự kiểm trong cùng một sự kiện.

[Nguồn: 02-bd/security/identity.md:25-26,31-34,35-37,51-53; 02-bd/database/identity.md:37,40,55-66]

---

## Câu hỏi mở

| # | Câu hỏi | Vì sao chưa trả lời được | Chủ sở hữu |
| :-: | :--- | :--- | :--- |
| Q1 | ~~Lưu tức thời theo từng ô hay gộp vào nút "Lưu thay đổi"?~~ **ĐÃ CHỐT 2026-10-08 (owner, theo đề xuất của BD, `DEC-2026-1008-admin-system-screens-review`): lưu tức thời từng ô; bỏ nút "Lưu thay đổi" ở thanh tiêu đề.** Mỗi lần bấm ô gọi `UpdateRolePermission` ngay, đúng nghĩa "có hiệu lực ngay" của F1-10 [Nguồn: 05-coding/frontend/src/views/admin/permission-matrix/ui/admin-permission-matrix-view.tsx:64-86; 02-bd/security/identity.md:35-37]. Hệ quả: Sheet 5/6 Khu vực A NO 3 và EVT-9 đã bỏ, EVT-2/EVT-10 bỏ bước hỏi thay đổi chưa lưu, Sheet 7.3 không cần endpoint lô, Q5 tự đóng. | Đã đóng | Đã đóng |
| Q2 | ~~Vai trò tuỳ biến đang có người dùng gán thì chặn xoá hay tự chuyển người dùng về một vai trò mặc định?~~ **ĐÃ CHỐT 2026-10-08 (owner, theo đề xuất, `DEC-2026-1008-admin-system-open-questions-closed`): chặn xoá**, không tự chuyển. Prototype: `Role.userCount`, nút xoá hiện toast cảnh báo và không mở hộp xác nhận [Nguồn: 05-coding/frontend/src/views/admin/permission-matrix/ui/admin-permission-matrix-view.tsx:251-258; 05-coding/frontend/src/views/admin/permission-matrix/model/types.ts:55]. Máy chủ cũng phải chặn (Sheet 9 NO 7). | Đã đóng | Đã đóng |
| Q3 | ~~Có cần chức năng sửa tên vai trò tuỳ biến không?~~ **ĐÃ CHỐT 2026-10-08 (owner, theo đề xuất): không đổi tên vai trò**; "sửa" ở F1-11 nghĩa là sửa ô ma trận của vai trò. Muốn đổi tên thì xoá rồi tạo lại (khi không còn người gán). | Đã đóng | Đã đóng |
| Q4 | ~~Luật chặn ở Sheet 9 dòng 9 — không cho gỡ ô `PERMISSION_MATRIX:UPDATE` cuối cùng của mọi vai trò `ADMIN` — có được chấp nhận là ràng buộc cứng không?~~ **ĐÃ CHỐT 2026-10-08 (owner, theo đề xuất): chặn cứng** mọi lần gỡ ô `PERMISSION_MATRIX:UPDATE` cuối cùng của các vai trò có `base_category = ADMIN`; Sheet 9 NO 9 giữ mức Lỗi và không còn là đề xuất. Prototype đã dựng [Nguồn: 05-coding/frontend/src/views/admin/permission-matrix/model/guards.ts:30-60]. | Đã đóng | Đã đóng |
| Q5 | ~~Rời màn khi còn thay đổi chưa lưu (bấm mục khác trên thanh điều hướng) thì có hỏi xác nhận không?~~ **Tự đóng 2026-10-08:** Q1 chốt lưu tức thời từng ô nên không còn thay đổi chưa lưu, không hỏi xác nhận khi rời màn. | Đã đóng | Đã đóng |
| Q6 | ~~Nhóm cơ bản của vai trò tuỳ biến và việc khoá ô~~ **ĐÃ CHỐT HẲN 2026-10-08 (owner).** Phần 1: form "+ Vai trò mới" có ô chọn nhóm cơ bản (`base_category`: STUDENT, INSTRUCTOR, ADMIN), mặc định `STUDENT`. Phần 2: **ô chỉ bị khoá với vai trò hệ thống `STUDENT` (theo `code`), không bao giờ theo nhóm cơ bản**; mọi vai trò tuỳ biến luôn chỉnh được, kể cả nhóm `STUDENT`, nên mặc định tắt hết rồi cấp dần (REQ-05) vẫn làm được. Hệ quả giữ lại: vai trò nhóm `STUDENT` được cấp Function của khu giảng viên vẫn không vào được khu đó (Lớp 1 kiểm theo `base_category`) [Nguồn: 05-coding/frontend/src/views/admin/permission-matrix/ui/admin-permission-matrix-view.tsx:65,104-107]. | Đã đóng | Đã đóng |
