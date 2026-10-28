# Tiêu chí nghiệm thu — Màn ADM0201 — Quản lý người dùng

> DD: `03-dd/screens/admin/ADM0201_user_management.md` · Cập nhật: 2026-10-26.

## 1. Phạm vi

Các tiêu chí này kiểm hành vi màn hình và phối hợp giữa module. Hợp đồng máy chủ tương ứng nằm ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### UM-AC-01 — Lọc và chọn người dùng theo trang

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Danh sách có nhiều trang và nhiều vai trò. |
| **Hành động** | Lọc, đổi trang, chọn tất cả trên trang hiện tại rồi thực hiện thao tác hàng loạt. |
| **Kết quả mong đợi** | Bộ lọc và phân trang dùng dữ liệu máy chủ; chọn tất cả chỉ chọn các dòng đang thấy. Thành công một phần nêu đúng số người xử lý được và giữ lại người lỗi để xử lý tiếp. |
| **Nguồn** | [SoT: 03-dd/screens/admin/ADM0201_user_management.md] mục 3, 7 |

### UM-AC-02 — Khoá và đổi vai trò có rào bảo vệ

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Có quản trị viên cuối cùng, tài khoản dùng đăng nhập liên kết và tài khoản cần khoá. |
| **Hành động** | Thử hạ vai trò quản trị viên cuối, cấp mật khẩu tạm cho tài khoản liên kết, khoá và mở khoá. |
| **Kết quả mong đợi** | Hệ thống chặn mất quản trị viên cuối; tài khoản liên kết được hướng dẫn đúng cách truy cập. Khoá kèm email yêu cầu lý do; thao tác không có quyền hoặc thiếu dữ liệu không được gửi. |
| **Nguồn** | [SoT: 03-dd/screens/admin/ADM0201_user_management.md] mục 3, 7 |

### UM-AC-03 — Tạo tài khoản và xem nhật ký không lộ bí mật

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Quản trị viên có quyền tạo tài khoản. |
| **Hành động** | Tạo tài khoản rồi mở nhật ký thao tác. |
| **Kết quả mong đợi** | Tài khoản mới hiện trong danh sách; hộp thoại không lộ lại mật khẩu. Liên kết nhật ký mở đúng bản ghi liên quan. |
| **Nguồn** | [SoT: 03-dd/screens/admin/ADM0201_user_management.md] mục 3, 7 |

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| UM-AC-01 | Mục 3, 7; điểm kiểm DD: USRM-01, USRM-04 | [TC-ADM0201-UM-AC-01-01](../../testcases/screens/admin/admin_screens_phase4.md), [TC-ADM0201-UM-AC-01-02](../../testcases/screens/admin/admin_screens_phase4.md) | Chưa thực hiện |
| UM-AC-02 | Mục 3, 7; điểm kiểm DD: USRM-02–03, USRM-05 | [TC-ADM0201-UM-AC-02-01](../../testcases/screens/admin/admin_screens_phase4.md), [TC-ADM0201-UM-AC-02-02](../../testcases/screens/admin/admin_screens_phase4.md), [TC-ADM0201-UM-AC-02-03](../../testcases/screens/admin/admin_screens_phase4.md) | Chưa thực hiện |
| UM-AC-03 | Mục 3, 7; điểm kiểm DD: USRM-06 | [TC-ADM0201-UM-AC-03-01](../../testcases/screens/admin/admin_screens_phase4.md) | Chưa thực hiện |

