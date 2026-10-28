# Tiêu chí nghiệm thu — Màn ADM0404 — Sự kiện hạ tầng

> DD: `03-dd/screens/admin/ADM0404_infra_events.md` · Cập nhật: 2026-10-26.

## 1. Phạm vi

Các tiêu chí này kiểm hành vi màn hình và phối hợp giữa module. Hợp đồng máy chủ tương ứng nằm ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### IE-AC-01 — Lọc sự kiện giữ ngữ cảnh và trang

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Có sự kiện nhiều mức và nhiều trang. |
| **Hành động** | Mở với bộ lọc trên đường dẫn, đổi từ khoá, mức, dịch vụ và khoảng ngày rồi tải thêm. |
| **Kết quả mong đợi** | Bộ lọc hợp lệ được áp, giá trị lạ về mặc định; đổi lọc bỏ vị trí trang cũ. Trang kế nối đúng thứ tự, khi hết dữ liệu không còn nút tải thêm. |
| **Nguồn** | [SoT: 03-dd/screens/admin/ADM0404_infra_events.md] mục 3, 7 |

### IE-AC-02 — Lỗi ngày, tải thêm và làm mới hiển thị đúng

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người quản trị nhập ngày bắt đầu sau ngày kết thúc; trang kế hoặc lần làm mới có thể lỗi. |
| **Hành động** | Tìm, tải thêm và làm mới. |
| **Kết quả mong đợi** | Ngày sai được chỉ rõ, không gửi truy vấn; tải thêm lỗi giữ các dòng đã có. Làm mới lỗi không hiện thông báo thành công. |
| **Nguồn** | [SoT: 03-dd/screens/admin/ADM0404_infra_events.md] mục 3, 7 |

### IE-AC-03 — Chỉ người có quyền xem sự kiện

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Một tài khoản thiếu quyền giám sát hàng đợi. |
| **Hành động** | Mở Sự kiện hạ tầng và thử các liên kết tới Hàng đợi, Nhật ký. |
| **Kết quả mong đợi** | Không thấy dữ liệu sự kiện khi thiếu quyền. Liên kết mở đúng màn được phép; màn không tự hỏi lại dữ liệu liên tục. |
| **Nguồn** | [SoT: 03-dd/screens/admin/ADM0404_infra_events.md] mục 3, 7 |

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| IE-AC-01 | Mục 3, 7; điểm kiểm DD: IE-01–02, IE-04 | [TC-ADM0404-IE-AC-01-01](../../testcases/screens/admin/admin_screens_phase4.md) | Chưa thực hiện |
| IE-AC-02 | Mục 3, 7; điểm kiểm DD: IE-03, IE-05 | [TC-ADM0404-IE-AC-02-01](../../testcases/screens/admin/admin_screens_phase4.md) | Chưa thực hiện |
| IE-AC-03 | Mục 3, 7; điểm kiểm DD: IE-06–07 | [TC-ADM0404-IE-AC-03-01](../../testcases/screens/admin/admin_screens_phase4.md) | Chưa thực hiện |

