# Tiêu chí nghiệm thu — Màn ADM0401 — Giám sát hàng đợi

> DD: `03-dd/screens/admin/ADM0401_queue_monitor.md` · Cập nhật: 2026-10-26.

## 1. Phạm vi

Các tiêu chí này kiểm hành vi màn hình và phối hợp giữa module. Hợp đồng máy chủ tương ứng nằm ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### QM-AC-01 — Trạng thái hàng đợi, việc và cấu hình tải riêng

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Quản trị viên mở màn giám sát; một nguồn số liệu tạm lỗi. |
| **Hành động** | Làm mới trang, đổi tab và đổi trang danh sách việc. |
| **Kết quả mong đợi** | Khối còn dữ liệu vẫn hiện; lỗi một khối không che khối khác. Đổi tab hoặc trang không giữ việc của tab cũ. Màn không dựng bảng trạng thái từng worker hay trạng thái quá tải worker không có nguồn. |
| **Nguồn** | [SoT: 03-dd/screens/admin/ADM0401_queue_monitor.md] mục 3, 7 |

### QM-AC-02 — Công tắc vận hành chỉ đổi khi lưu thành công

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Hàng đợi đang chạy và tự mở rộng đang tắt. |
| **Hành động** | Bấm tạm dừng hoặc bật tự mở rộng, rồi thử khi máy chủ từ chối. |
| **Kết quả mong đợi** | Trạng thái trên màn chỉ đổi khi thao tác được chấp nhận; lỗi không gửi lặp và không báo thành công. Bật tự mở rộng làm năng lực nhận việc thay đổi theo tồn đọng, theo tiêu chí F4. |
| **Nguồn** | [SoT: 03-dd/screens/admin/ADM0401_queue_monitor.md] mục 3, 7 |

### QM-AC-03 — Tham số vận hành và hộp thoại

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Quản trị viên mở cài đặt hàng đợi. |
| **Hành động** | Nhập các tham số, lưu, hủy, rồi mở Sự kiện hạ tầng. |
| **Kết quả mong đợi** | Chỉ số nguyên dương được gửi; hộp thoại lưu hoặc hủy rõ ràng và trả focus đúng chỗ. Lối Sự kiện hạ tầng mở đúng màn. |
| **Nguồn** | [SoT: 03-dd/screens/admin/ADM0401_queue_monitor.md] mục 3, 7 |

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| QM-AC-01 | Mục 3, 7; điểm kiểm DD: QMON-01, QMON-03, QMON-07 | [TC-ADM0401-QM-AC-01-01](../../testcases/screens/admin/admin_screens_phase4.md), [TC-ADM0401-QM-AC-01-02](../../testcases/screens/admin/admin_screens_phase4.md) | Chưa thực hiện |
| QM-AC-02 | Mục 3, 7; điểm kiểm DD: QMON-02, QMON-07 | [TC-ADM0401-QM-AC-02-01](../../testcases/screens/admin/admin_screens_phase4.md) | Chưa thực hiện |
| QM-AC-03 | Mục 3, 7; điểm kiểm DD: QMON-04–06 | [TC-ADM0401-QM-AC-03-01](../../testcases/screens/admin/admin_screens_phase4.md), [TC-ADM0401-QM-AC-03-02](../../testcases/screens/admin/admin_screens_phase4.md) | Chưa thực hiện |

