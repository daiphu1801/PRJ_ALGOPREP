# Tiêu chí nghiệm thu — Màn ADM0101 — Tổng quan quản trị

> DD: `03-dd/screens/admin/ADM0101_overview.md` · Cập nhật: 2026-10-26.

## 1. Phạm vi

Các tiêu chí này kiểm hành vi màn hình và phối hợp giữa module. Hợp đồng máy chủ tương ứng nằm ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### O-AC-01 — Mười khối số liệu tải độc lập

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Quản trị viên có dữ liệu người dùng, nội dung và vận hành. |
| **Hành động** | Mở Tổng quan rồi thử lại một khối bị lỗi. |
| **Kết quả mong đợi** | Các thẻ và biểu đồ lấy số thật, mức độ khó theo danh mục hiện hành. Lỗi một khối không che khối khác; Thử lại chỉ tải khối đó. |
| **Nguồn** | [SoT: 03-dd/screens/admin/ADM0101_overview.md] mục 3, 7 |

### O-AC-02 — Không có kết quả cuối thì không hiện tỷ lệ giả

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Chưa có bài nộp nào có kết quả cuối. |
| **Hành động** | Mở Tổng quan. |
| **Kết quả mong đợi** | Tỷ lệ chấp thuận hiện trạng thái chưa có dữ liệu, không chia cho không và không dùng số mẫu. |
| **Nguồn** | [SoT: 03-dd/screens/admin/ADM0101_overview.md] mục 3, 7 |

### O-AC-03 — Liên kết quản trị giữ đúng đích

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Quản trị viên đang ở Tổng quan. |
| **Hành động** | Bấm liên kết tới từng khu quản trị. |
| **Kết quả mong đợi** | Màn đích đúng khu; chân trang còn hiện trên Tổng quan khi quay lại. |
| **Nguồn** | [SoT: 03-dd/screens/admin/ADM0101_overview.md] mục 3, 7 |

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| O-AC-01 | Mục 3, 7; điểm kiểm DD: ADM-O-01–02 | [TC-ADM0101-O-AC-01-01](../../testcases/screens/admin/admin_screens_phase4.md) | Chưa thực hiện |
| O-AC-02 | Mục 3, 7; điểm kiểm DD: ADM-O-03 | [TC-ADM0101-O-AC-02-01](../../testcases/screens/admin/admin_screens_phase4.md) | Chưa thực hiện |
| O-AC-03 | Mục 3, 7; điểm kiểm DD: ADM-O-04 | [TC-ADM0101-O-AC-03-01](../../testcases/screens/admin/admin_screens_phase4.md) | Chưa thực hiện |

