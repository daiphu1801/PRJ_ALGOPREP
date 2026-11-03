# DD màn hình — Quản lý người dùng (`ADM0201`)

## 0. Thông tin tài liệu

| Mục | Giá trị |
| :--- | :--- |
| Actor, route | A3 `ADMIN`; `/admin/users` |
| Nguồn | `01-rd/screens/admin/ADM0201_user_management.md`; `02-bd/screens/admin/ADM0201_user_management.md` Sheet 1–9; [F1](../../api/F1_identity.md) §0.5–0.6 |
| Prototype | `05-coding/frontend/src/views/admin/user-management/ui/admin-user-management-view.tsx` |
| Phiên bản | V0.2, 2026-11-02 (nối API thật, task #12); V0.1, 2026-10-25; DD màn, Mode B |

## 1. Bản đồ slice FSD

| Lớp | Đường dẫn từ `src/` | Vai trò |
| :--- | :--- | :--- |
| app | `app/(admin)/admin/users/page.tsx` | Gắn view, actor A3 |
| views | `views/admin/user-management/ui/` | Bảng và popup đổi vai trò/khoá/mở khoá/thêm tài khoản |
| features | `features/manage-users/` | Chọn dòng, lệnh N tài khoản, tổng hợp thành công một phần |
| views (api) | `views/admin/user-management/api/` (không tách sang `entities`, xem mục 2) | F1 #32, #35–38, #50 |
| shared | `DataTable`, `ConfirmDialog`, `ErrorState`, `toast` | Dùng chung, dialog trả focus |

## 2. API tiêu thụ

| EVT BD | API | Hành vi màn |
| :--- | :--- | :--- |
| 1–7 tìm/lọc/trang | F1 #35 `GET /admin/users`; #32 `GET /rbac/roles` cho popup; #62 `GET /admin/users/management-stats`, #63 `GET /admin/users/role-distribution` | Danh sách phân trang server, hai khối phụ độc lập, bộ lọc trong state của view |
| 8–9 đặt lại mật khẩu | F1 #38 `POST /admin/users/{userId}/reset-password` | Một tài khoản; chỉ sau xác nhận |
| 10–11 đổi role | F1 #36 `PATCH /admin/users/{userId}/role` | Một tài khoản; sau thành công tải lại role distribution |
| 12–13 khoá, 19–20 mở khoá | F1 #37 `PATCH /admin/users/{userId}/status` | N lệnh đơn; tổng hợp thành công/thất bại theo từng ID |
| 15, 17, 18 thêm tài khoản | F1 #50 `POST /admin/users` | Server sinh mật khẩu tạm, UI không nhận mật khẩu |
| 16 | Không gọi API | Mở `/admin/system-log` |

**Triển khai 2026-11-02 (task #12, `DEC-2026-1102-admin-user-management-contracts`).** Màn đã nối API thật ở chế độ `NEXT_PUBLIC_MOCK_DATA=false`; chế độ mock giữ nguyên.
- **Vị trí mã.** Giữ trong `views/admin/user-management/api/` như mọi màn Admin khác đang đặt (bản đồ slice mục 1 ghi `entities/user` và `entities/role` là dự kiến của DD V0.1); không tách vì chưa có màn thứ hai dùng chung (ponytail bậc 4). Gồm: `real-user-management.ts` (gọi #35, #62, #63, #9, #32, #36–38, #50 và ánh xạ DTO), `queries.ts` (`useAdminUserPage(params)`), `mutations.ts` (`useAdminUserActions`).
- **Một truy vấn cho trang.** `realFetchAdminUserPage` gọi song song #35, #62, #63 và `GET /me/profile` (để biết email quản trị viên đang thao tác, phục vụ luật tự khoá); khoá truy vấn gồm bộ lọc và số trang, giữ dữ liệu cũ trong lúc tải trang mới nên gõ tìm kiếm không làm trống bảng. Từ khoá tìm gửi lên sau 300 ms ngừng gõ.
- **Bộ lọc, từ khoá và số trang nằm trong state của view**, không nằm trong URL như dòng EVT 1–7 của bảng trên từng viết. Đóng bằng tiêu chí ponytail bậc 3 (không có người dùng nào mở lại màn bằng đường dẫn có bộ lọc trong buổi demo hoặc bảo vệ) và bậc 1 (RD không đòi); sửa dòng EVT 1–7 cho khớp.
- **Hành động.** Đổi vai trò, khoá, mở khoá, đặt lại mật khẩu và tạo tài khoản đều chờ máy chủ trả lời rồi mới đổi dòng trên màn; thành công thì làm mới danh sách và hai khối thống kê. Khoá và mở khoá hàng loạt là N lệnh đơn chạy song song, một lệnh lỗi không dừng các lệnh khác; toast báo số lệnh thành công và số lệnh lỗi kèm lý do của lệnh lỗi đầu tiên (`batchFailed`). Khi quản trị viên tự khoá hoặc tự hạ vai trò, màn gửi `acknowledgeSelfImpact: true` vì hộp thoại đã hỏi xác nhận.
- **Vai trò đích.** Màn chỉ có ba vai trò gốc; `GET /rbac/roles` được gọi lúc thao tác để đổi `student`, `instructor`, `admin` thành id của role hệ thống cùng mã (BD Sheet 7.1 NO 12 dùng `roleId`).
- **Không hiển thị số bịa.** Cột "Hoạt động" lấy từ `lastActiveAt` của #35 (bổ sung 2026-11-02): thời gian tương đối theo ngôn ngữ giao diện ("5 phút trước", "Hôm qua"), quá 30 ngày thì hiện ngày theo UTC+7, tài khoản chưa đăng nhập hiện "-". Cột "Đã giải" và "Lượt nộp" chỉ xuất hiện khi dòng dữ liệu có các trường đó (chế độ mock); với API thật hai cột này ẩn đến khi F4 có read model (nợ D04).
- **Mã lỗi** hiển thị qua `apiErrors` (vi và en): `IDT-432`, `IDT-440`, `IDT-441`, `IDT-445`, `IDT-448`, `IDT-450`, `IDT-V17`, `IDT-V21`, `IDT-V26`.
- Kiểm chứng: `pnpm check` 123 file, 688 test; `e2e/admin-users-real.spec.ts` 5 kịch bản trên backend và frontend thật (chạy tay bằng Playwright, không phải `pnpm e2e` mặc định).

Hai khối thống kê/phân bố dùng F1 #62–63; không tính từ một trang 20 dòng. EVT-14 đóng popup không gửi API.

## 3. Trạng thái màn

| Trạng thái | Xử lý |
| :--- | :--- |
| Danh sách/khối thống kê lỗi | Lỗi đúng khối; không giữ số mock hoặc suy tổng từ trang |
| Chọn nhiều dòng | Thanh hành động đếm đúng số ID đang chọn; đổi bộ lọc/trang bỏ chọn |
| Tác động một phần | Hiện một toast số thành công/thất bại và lý do theo ID; làm mới danh sách |
| Tài khoản `anonymized=true` | Vô hiệu Mở khoá; server vẫn kiểm lại |
| `hasPassword=false` | Đặt lại mật khẩu chỉ gửi hướng dẫn provider, không báo “mật khẩu tạm” |
| Mutation lỗi | Giữ popup nếu chưa có kết quả; lỗi theo `apiError.IDT-*` |

## 4. Quy tắc đặc thù màn

### BR-SC-01 Hành động theo tập

Khoá/mở khoá tách tập chọn theo trạng thái; popup chỉ liệt kê tài khoản cần tác động và dòng bỏ qua. Mỗi tài khoản gọi F1 #37 một lần; không `Promise.all` vô hạn: giới hạn đồng thời và tổng hợp response theo ID, số trong toast là số thực sự thành công. Admin hoạt động cuối cùng bị chặn ở server; kiểm trước ở client chỉ giúp giải thích. Tự khoá cần xác nhận tác động phiên, sau thành công mất quyền thì về đăng nhập. Tự hạ role có cảnh báo, vẫn cho nếu còn admin khác. [SoT: BD EVT-9,11–13,19–20; F1 BR-15]

Sau xác nhận tự khoá hoặc tự hạ vai trò, request #37/#36 phải mang `acknowledgeSelfImpact: true`; huỷ không gửi request. Thiếu hoặc false bị server từ chối trước khi ghi dữ liệu theo hợp đồng F1, kể cả khi bỏ qua hộp thoại ở client. [SoT: F1 #36–37; quyết định chủ dự án về self-impact]

### BR-SC-02 Tạo và đổi quyền

Popup thêm tài khoản chỉ chọn vai trò STUDENT/INSTRUCTOR từ F1 #32; F1 #50 tạo `PENDING_VERIFICATION`, phát mật khẩu tạm qua email và buộc đổi ở lần đăng nhập đầu. UI không sinh, nhận hoặc hiển thị mật khẩu. Đổi role chỉ một tài khoản; sau thành công làm mới bảng, phân bố và nếu đổi chính mình thì làm mới quyền phiên. Admin khoá tài khoản với `notifyByEmail=true` phải nhập lý do; mở khoá không gửi email và không có lý do. [SoT: BD Q1/Q7, Sheet 8–9; F1 §0.6]

## 5. Kiểm tra phía client

| Trường | Quy tắc | Nguồn |
| :--- | :--- | :--- |
| `displayName` | Trim, bắt buộc | BD Sheet 9 NO 13; validation F1 |
| `email` | Trim, lower-case, định dạng email; trùng do server quyết định | F1 #50, `IDT-440` |
| `roleId` | ID từ #32, chỉ base category STUDENT/INSTRUCTOR | F1 #50, `IDT-441` |
| `reason`, `notifyByEmail` | Khi gửi email, lý do sau trim phải có chữ | BD Sheet 9 NO 16; F1 #37 |
| `q`, `page`, `pageSize` | Theo F1 #35, URL lạ về mặc định | F1 #35 |

## 6. i18n và truy cập

Dùng `adminUserManagement.*`, `apiError.IDT-*`. Checkbox chọn tất cả chỉ áp trang hiện tại và có nhãn. Dialog liệt kê tài khoản sẽ tác động, cảnh báo tự khoá bằng chữ, focus ở nút Huỷ khi hành động có hậu quả mạnh. Kết quả một phần đọc được qua `role=alert`, không chỉ màu. Email và tên người dùng là dữ liệu hiển thị, escape; không chèn vào HTML thô.

## 7. Điểm kiểm thử

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| USRM-01 | Normal | EVT-1–7 lọc/phân trang server, chọn chỉ trang hiện tại | Vitest | TDD màn `UM-AC-01` |
| USRM-02 | Boundary | EVT-8/9 OAuth-only nhận hướng dẫn, không mật khẩu tạm | Vitest | TDD màn `UM-AC-02` |
| USRM-03 | Boundary | EVT-10/11 chặn admin cuối, tự hạ role có cảnh báo | Vitest | TDD màn `UM-AC-02` |
| USRM-04 | Abnormal | EVT-12/13/19/20 thành công một phần, toast đếm đúng ID | Vitest | TDD màn `UM-AC-01` |
| USRM-05 | Boundary | Khoá gửi email cần lý do; mở khoá ẩn danh bị chặn | Vitest | TDD màn `UM-AC-02` |
| USRM-06 | Normal | EVT-15–18 tạo tài khoản, popup không lộ mật khẩu, link log đúng | Playwright | TDD màn `UM-AC-03` |

## 8. Nợ prototype và câu hỏi mở

Prototype còn mock và email/role seed, cần nối F1 #32, #35–38, #50, #62–63. Hai endpoint đọc tổng hợp đã được bổ sung vào F1 để không đếm từ trang hiện tại. Các hành động gộp dùng N lệnh đơn #37; không tự tạo bulk endpoint. TDD màn đã có tại `04-tdd/screens/admin/ADM0201_user_management.md`.
