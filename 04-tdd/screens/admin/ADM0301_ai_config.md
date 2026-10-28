# Tiêu chí nghiệm thu — Màn ADM0301 — Cấu hình AI

> DD: `03-dd/screens/admin/ADM0301_ai_config.md` · Cập nhật: 2026-10-26.

## 1. Phạm vi

Các tiêu chí này kiểm hành vi màn hình và phối hợp giữa module. Hợp đồng máy chủ tương ứng nằm ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### AIC-AC-01 — Bản đang dùng và bản nháp tách rõ

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Mỗi tính năng AI có cấu hình đang dùng, bản nháp và lịch sử. |
| **Hành động** | Mở cấu hình rồi sửa và lưu bản nháp. |
| **Kết quả mong đợi** | Ba tính năng hiện đúng bản đang dùng và bản nháp; hủy sửa ở màn không gửi thay đổi. Mô hình được chọn từ danh sách hệ thống cho phép. |
| **Nguồn** | [SoT: 03-dd/screens/admin/ADM0301_ai_config.md] mục 3, 7 |

### AIC-AC-02 — Phát hành và khôi phục không tạo bản dùng dở

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Có bản nháp hợp lệ và bản lịch sử. |
| **Hành động** | Phát hành bản nháp; thử khi phát hành lỗi; khôi phục bản lịch sử. |
| **Kết quả mong đợi** | Phát hành thành công thay trọn bản đang dùng. Lỗi giữ nguyên bản nháp và bản đang dùng; khôi phục tạo bản nháp để xem lại, không sửa bản lưu trữ. |
| **Nguồn** | [SoT: 03-dd/screens/admin/ADM0301_ai_config.md] mục 3, 7 |

### AIC-AC-03 — Rời bản nháp chưa lưu có xác nhận

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Quản trị viên đã sửa cấu hình nhưng chưa lưu. |
| **Hành động** | Bấm rời màn, sau đó chọn ở lại hoặc bỏ thay đổi. |
| **Kết quả mong đợi** | Màn hỏi trước khi bỏ bản nháp; thao tác bàn phím và focus hoạt động trong hộp thoại. |
| **Nguồn** | [SoT: 03-dd/screens/admin/ADM0301_ai_config.md] mục 3, 7 |

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| AIC-AC-01 | Mục 3, 7; điểm kiểm DD: AIC-01–02 | [TC-ADM0301-AIC-AC-01-01](../../testcases/screens/admin/admin_screens_phase4.md) | Chưa thực hiện |
| AIC-AC-02 | Mục 3, 7; điểm kiểm DD: AIC-03–05 | [TC-ADM0301-AIC-AC-02-01](../../testcases/screens/admin/admin_screens_phase4.md) | Chưa thực hiện |
| AIC-AC-03 | Mục 3, 7; điểm kiểm DD: AIC-06 | [TC-ADM0301-AIC-AC-03-01](../../testcases/screens/admin/admin_screens_phase4.md) | Chưa thực hiện |

