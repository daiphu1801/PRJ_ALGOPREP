# Tiêu chí nghiệm thu — Màn ADM0403 — Nhật ký hệ thống

> DD: `03-dd/screens/admin/ADM0403_system_log.md` · Cập nhật: 2026-10-26.

## 1. Phạm vi

Các tiêu chí này kiểm hành vi màn hình và phối hợp giữa module. Hợp đồng máy chủ tương ứng nằm ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### LOG-AC-01 — Lọc và cuộn nhật ký ổn định

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Nhật ký có hơn một trang và có sự kiện mới. |
| **Hành động** | Lọc, tải thêm, đổi bộ lọc rồi mất và nối lại luồng cập nhật. |
| **Kết quả mong đợi** | Mỗi trang tối đa 50 dòng; đổi bộ lọc bỏ vị trí trang cũ. Nối lại không nhân đôi hoặc bỏ mất dòng đã nhận. |
| **Nguồn** | [SoT: 03-dd/screens/admin/ADM0403_system_log.md] mục 3, 7 |

### LOG-AC-02 — Ngày và xuất dữ liệu được kiểm trước khi chạy

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Quản trị viên chọn khoảng ngày đảo ngược hoặc vượt hạn. |
| **Hành động** | Tìm và xuất CSV, sau đó sửa khoảng ngày và chọn cột. |
| **Kết quả mong đợi** | Khoảng sai bị chặn cho cả tìm và xuất. Khoảng hợp lệ xuất đúng bộ lọc và cột đã chọn, không lộ mã xác thực, mã dùng một lần hoặc khoá bí mật. |
| **Nguồn** | [SoT: 03-dd/screens/admin/ADM0403_system_log.md] mục 3, 7 |

### LOG-AC-03 — Giảm hạn lưu và xoá cần xác nhận

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Quản trị viên có quyền thay thời hạn lưu nhật ký. |
| **Hành động** | Giảm hạn hoặc bấm xoá, rồi xem trạng thái lưu trữ. |
| **Kết quả mong đợi** | Màn giải thích hệ quả và hỏi xác nhận trước thao tác; người chỉ có quyền đọc không thực hiện được thay đổi. |
| **Nguồn** | [SoT: 03-dd/screens/admin/ADM0403_system_log.md] mục 3, 7 |

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| LOG-AC-01 | Mục 3, 7; điểm kiểm DD: LOG-01, LOG-03 | [TC-ADM0403-LOG-AC-01-01](../../testcases/screens/admin/admin_screens_phase4.md), [TC-ADM0403-LOG-AC-01-02](../../testcases/screens/admin/admin_screens_phase4.md) | Chưa thực hiện |
| LOG-AC-02 | Mục 3, 7; điểm kiểm DD: LOG-02, LOG-04–05 | [TC-ADM0403-LOG-AC-02-01](../../testcases/screens/admin/admin_screens_phase4.md) | Chưa thực hiện |
| LOG-AC-03 | Mục 3, 7; điểm kiểm DD: LOG-06 | [TC-ADM0403-LOG-AC-03-01](../../testcases/screens/admin/admin_screens_phase4.md), [TC-ADM0403-LOG-AC-03-02](../../testcases/screens/admin/admin_screens_phase4.md) | Chưa thực hiện |

