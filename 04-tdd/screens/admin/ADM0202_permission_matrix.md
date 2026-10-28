# Tiêu chí nghiệm thu — Màn ADM0202 — Ma trận quyền

> DD: `03-dd/screens/admin/ADM0202_permission_matrix.md` · Cập nhật: 2026-10-26.

## 1. Phạm vi

Các tiêu chí này kiểm hành vi màn hình và phối hợp giữa module. Hợp đồng máy chủ tương ứng nằm ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### RBAC-AC-01 — Ma trận theo tab không trộn dữ liệu cũ

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Quản trị viên có nhiều vai trò và nhóm quyền. |
| **Hành động** | Mở ma trận, đổi tab nhanh rồi đổi một ô quyền. |
| **Kết quả mong đợi** | Tab hiện đúng dữ liệu của nó; đổi một ô được lưu ngay và phản ánh giá trị mới, không cần nút Lưu chung. |
| **Nguồn** | [SoT: 03-dd/screens/admin/ADM0202_permission_matrix.md] mục 3, 7 |

### RBAC-AC-02 — Ghi lỗi hoàn nguyên đúng ô

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Một ô quyền đang ở trạng thái tắt. |
| **Hành động** | Bấm ô hai lần nhanh hoặc làm cho lần lưu thất bại. |
| **Kết quả mong đợi** | Không gửi hai thay đổi đối nghịch; lỗi trả ô về trạng thái đã lưu và báo để thử lại. |
| **Nguồn** | [SoT: 03-dd/screens/admin/ADM0202_permission_matrix.md] mục 3, 7 |

### RBAC-AC-03 — Vai trò hệ thống và vai trò đang dùng không bị xoá

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Có vai trò hệ thống, vai trò đã gán người dùng và vai trò có thể xoá. |
| **Hành động** | Thêm vai trò, thử xoá từng loại rồi đổi tab. |
| **Kết quả mong đợi** | Vai trò hệ thống hoặc đang gán bị chặn; vai trò hợp lệ được thêm hoặc xoá và focus ở vị trí hợp lý sau thao tác. |
| **Nguồn** | [SoT: 03-dd/screens/admin/ADM0202_permission_matrix.md] mục 3, 7 |

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| RBAC-AC-01 | Mục 3, 7; điểm kiểm DD: RBAC-01–02 | [TC-ADM0202-RBAC-AC-01-01](../../testcases/screens/admin/admin_screens_phase4.md) | Chưa thực hiện |
| RBAC-AC-02 | Mục 3, 7; điểm kiểm DD: RBAC-03 | [TC-ADM0202-RBAC-AC-02-01](../../testcases/screens/admin/admin_screens_phase4.md) | Chưa thực hiện |
| RBAC-AC-03 | Mục 3, 7; điểm kiểm DD: RBAC-04–05 | [TC-ADM0202-RBAC-AC-03-01](../../testcases/screens/admin/admin_screens_phase4.md) | Chưa thực hiện |

