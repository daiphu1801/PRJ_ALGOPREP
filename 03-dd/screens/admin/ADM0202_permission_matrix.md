# DD màn hình — Ma trận phân quyền (`ADM0202`)

## 0. Thông tin tài liệu

| Mục | Giá trị |
| :--- | :--- |
| Actor, route | A3 `ADMIN`; `/admin/permissions` |
| Nguồn | `01-rd/screens/admin/ADM0202_permission_matrix.md`; `02-bd/screens/admin/ADM0202_permission_matrix.md` Sheet 1–9; [F1](../../api/F1_identity.md) §0.5, BR-13–15 |
| Prototype | `05-coding/frontend/src/views/admin/permission-matrix/ui/admin-permission-matrix-view.tsx` |
| Phiên bản | V0.1, 2026-10-25; DD màn, Mode B |

## 1. Bản đồ slice FSD

| Lớp | Đường dẫn từ `src/` | Vai trò |
| :--- | :--- | :--- |
| app | `app/(admin)/admin/permissions/page.tsx` | Gắn view, chặn actor khác |
| views | `views/admin/permission-matrix/ui/admin-permission-matrix-view.tsx` | Tab role, ma trận, form thêm/xoá role |
| features | `features/manage-permissions/` | Mutation một ô, thêm/xoá role, bảo vệ trạng thái đang gửi |
| entities | `entities/permission/api/queries.ts`, `mutations.ts` | F1 #30–34, invalidate cache quyền |
| shared | `Switch`, `Tabs`, `ConfirmDialog`, `ErrorState` | Có tên row/column đọc được |

## 2. API tiêu thụ

| EVT BD | API | Hành vi màn |
| :--- | :--- | :--- |
| 1 mở màn, 2 đổi tab | F1 #32 `GET /rbac/roles`, #30 `GET /rbac/matrix` | Danh sách role và ma trận từ server; không hardcode functions/actions |
| 3–6 thêm role | F1 #33 `POST /rbac/roles` | Chỉ gửi khi form hợp lệ, chọn role mới sau response |
| 7–8 xoá role | F1 #34 `DELETE /rbac/roles/{roleId}` | Xác nhận; server chặn role hệ thống/đang có user |
| 9 | Không có nút Lưu chung | Mỗi ô gọi F1 #31 `PATCH /rbac/matrix/{roleId}/{functionId}/{actionId}` ngay |
| 10 | Không gọi API tại view | Khung xử lý điều hướng |

## 3. Trạng thái màn

| Trạng thái | Xử lý |
| :--- | :--- |
| Vai trò/ma trận đang tải/lỗi | Skeleton hoặc lỗi theo khối; không hiện ma trận mock |
| Role mới chưa có quyền | Mọi ô tắt theo response F1, không kế thừa role đang chọn |
| Một ô đang gửi | Chỉ khoá ô đó; response server mới là trạng thái chuẩn |
| Bật/tắt lỗi | Khôi phục giá trị cũ, thông báo `error.code` `IDT-*` |
| Xoá role bị chặn | Giữ tab và role, báo lý do từ mã lỗi |
| Quyền của chính admin đổi | Làm mới permission cache/route sau response; nếu mất quyền màn, chuyển trang không đủ quyền |

## 4. Quy tắc đặc thù màn

### BR-SC-01 Ghi tức thời và chống phản hồi lệch thứ tự

Một ô chỉ có một mutation đang chạy; nhấp liên tiếp bị khoá cho đến response. F1 #31 ghi và phát invalidation quyền, nên client làm mới ma trận và quyền phiên sau thành công; không chờ nút Lưu đã bỏ. Với một role khác, mutation có thể song song, nhưng response không ghi đè ma trận role hiện đang chọn. `ADMIN` vẫn có các quyền bất biến theo F1 BR-13/15; UI hiển thị ô bị khoá kèm giải thích, server luôn kiểm lại. [SoT: BD Sheet 8, F1 §0.5]

### BR-SC-02 Vai trò tuỳ biến

Tạo role cần tên và `baseCategory` theo F1 #33; role hệ thống không có nút xoá. Xoá role đang gán người dùng bị F1 #34 từ chối, UI không tự bỏ gán. Form đóng/huỷ không gửi API. Sau tạo hoặc xoá thành công làm mới role và ma trận, đặt tab về role còn tồn tại. [SoT: F1 BR-14; BD Sheet 8]

## 5. Kiểm tra phía client

| Trường | Quy tắc | Nguồn |
| :--- | :--- | :--- |
| Tên role | Trim, bắt buộc; độ dài/trùng theo validation F1 | F1 #33 |
| `baseCategory` | Chọn trong danh sách F1 cho phép, không suy từ nhãn tab | F1 #33 |
| `roleId`, `functionId`, `actionId` | Chỉ lấy từ #30/#32, server kiểm lại | F1 #30–31 |

## 6. i18n và truy cập

Dùng `adminPermissionMatrix.*`, `apiError.IDT-*`. Bảng có header cột/hàng, mỗi switch có tên “{vai trò} — {chức năng} — {hành động}”, trạng thái `aria-checked`. Tab role có `aria-selected`; sau tạo chọn tab mới và focus heading. Lỗi ô thông báo `role=alert` kèm tên ô; không chỉ đổi màu.

## 7. Điểm kiểm thử

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| RBAC-01 | Normal | EVT-1/2 ma trận từ F1, tab đổi không lẫn response cũ | Vitest | TDD màn `RBAC-AC-01` |
| RBAC-02 | Normal | Một ô ghi ngay, không có Lưu chung; invalidation sau thành công | Vitest | TDD màn `RBAC-AC-01` |
| RBAC-03 | Abnormal | Ghi ô lỗi hoàn nguyên; double click không gửi hai mutation | Vitest | TDD màn `RBAC-AC-02` |
| RBAC-04 | Boundary | Role hệ thống/đang gán user không xoá được | Vitest | TDD màn `RBAC-AC-03` |
| RBAC-05 | Normal | EVT-3–8 thêm/xoá role và focus/tab sau thành công | Playwright | TDD màn `RBAC-AC-03` |

## 8. Nợ prototype và câu hỏi mở

Prototype mock ma trận/role, cần nối F1 #30–34 và quyền thật; không đặt danh mục Function/Action cứng trong view. Nút Lưu chung đã bị BD bỏ. F1 §0.6 còn ghi chú mâu thuẫn cũ về `baseCategory`; khi nối dùng contract F1 #33 và validation F1 làm nguồn, không suy từ mock. Không còn câu hỏi mức màn cần owner. Chỉ gỡ nợ prototype khi nối thật; TDD màn đã có tại `04-tdd/screens/admin/ADM0202_permission_matrix.md`.
