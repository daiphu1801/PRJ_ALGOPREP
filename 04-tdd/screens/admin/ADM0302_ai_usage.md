# Tiêu chí nghiệm thu — Màn ADM0302 — Mức dùng AI

> DD: `03-dd/screens/admin/ADM0302_ai_usage.md` · Cập nhật: 2026-10-26.

## 1. Phạm vi

Các tiêu chí này kiểm hành vi màn hình và phối hợp giữa module. Hợp đồng máy chủ tương ứng nằm ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### AIU-AC-01 — Các khối mức dùng độc lập

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Quản trị viên có số liệu sử dụng AI; một khối tạm lỗi. |
| **Hành động** | Mở trang, thử lại khối lỗi và đổi kỳ xem nhanh. |
| **Kết quả mong đợi** | Khối lỗi không che bốn khối còn lại; thử lại chỉ tải khối đó. Khi đổi kỳ nhanh, phản hồi cũ không ghi đè kỳ mới; ngân sách tháng vẫn theo tháng. |
| **Nguồn** | [SoT: 03-dd/screens/admin/ADM0302_ai_usage.md] mục 3, 7 |

### AIU-AC-02 — Cảnh báo bất thường chỉ đổi sau khi lưu

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Có cảnh báo chưa xem. |
| **Hành động** | Đánh dấu đã xem, rồi thử khi máy chủ từ chối. |
| **Kết quả mong đợi** | Đánh dấu thành công mới cập nhật trạng thái; lỗi giữ cảnh báo chưa xem. Thiếu giá mô hình hiện dấu gạch và lượt chấm không bị gán sai nhóm. |
| **Nguồn** | [SoT: 03-dd/screens/admin/ADM0302_ai_usage.md] mục 3, 7 |

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| AIU-AC-01 | Mục 3, 7; điểm kiểm DD: AI-U-01–02, AI-U-05 | [TC-ADM0302-AIU-AC-01-01](../../testcases/screens/admin/admin_screens_phase4.md) | Chưa thực hiện |
| AIU-AC-02 | Mục 3, 7; điểm kiểm DD: AI-U-03–04, AI-U-06 | [TC-ADM0302-AIU-AC-02-01](../../testcases/screens/admin/admin_screens_phase4.md) | Chưa thực hiện |

