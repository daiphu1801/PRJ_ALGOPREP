# Tiêu chí nghiệm thu — Màn ADM0303 — Hạn mức AI

> DD: `03-dd/screens/admin/ADM0303_ai_limits.md` · Cập nhật: 2026-10-26.

## 1. Phạm vi

Các tiêu chí này kiểm hành vi màn hình và phối hợp giữa module. Hợp đồng máy chủ tương ứng nằm ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### AIL-AC-01 — Bốn khối hạn mức không phụ thuộc nhau

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Quản trị viên mở Hạn mức AI; nguồn giới hạn sinh testcase tạm lỗi. |
| **Hành động** | Tải trang, sửa tần suất hoặc ngưỡng một khối. |
| **Kết quả mong đợi** | Ngân sách AI và các khối còn lại vẫn hiện; chỉ trường đã đổi được gửi đi. Lỗi lưu giữ giá trị đã lưu cũ để so với bản đang sửa. |
| **Nguồn** | [SoT: 03-dd/screens/admin/ADM0303_ai_limits.md] mục 3, 7 |

### AIL-AC-02 — Ngân sách đã dùng hết không mở khoá sai

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Ngân sách đã dùng bằng hoặc vượt trần; quản trị viên tăng trần. |
| **Hành động** | Thử mở khoá trước và sau khi tăng trần. |
| **Kết quả mong đợi** | Khi mức dùng còn chạm trần, nút mở khoá không gửi yêu cầu và trạng thái vẫn khoá. Tăng trần hợp lệ không tự mở khoá; người quản trị chủ động mở sau khi đủ điều kiện. |
| **Nguồn** | [SoT: 03-dd/screens/admin/ADM0303_ai_limits.md] mục 3, 7 |

### AIL-AC-03 — Giá và trần testcase được kiểm ở cả màn và máy chủ

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Có bảng giá mô hình và cấu hình số lượt sinh testcase. |
| **Hành động** | Nhập tên giá trùng khác hoa, giá âm, và số lượt sinh ở các biên. |
| **Kết quả mong đợi** | Tên trùng khác chữ hoa bị từ chối. Trên màn, giá âm hoặc không phải số trở về 0 khi rời ô; yêu cầu gửi trực tiếp giá âm vẫn bị máy chủ từ chối. Đổi tên hoặc xoá giá không sửa lịch sử sử dụng. Trần sinh testcase chỉ nhận từ 1 đến 10; quyền cấu hình và quyền ngân sách được kiểm riêng. |
| **Nguồn** | [SoT: 03-dd/screens/admin/ADM0303_ai_limits.md] mục 3, 7 |

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| AIL-AC-01 | Mục 3, 7; điểm kiểm DD: AI-L-01, AI-L-04 | [TC-ADM0303-AIL-AC-01-01](../../testcases/screens/admin/admin_screens_phase4.md) | Chưa thực hiện |
| AIL-AC-02 | Mục 3, 7; điểm kiểm DD: AI-L-02–03 | [TC-ADM0303-AIL-AC-02-01](../../testcases/screens/admin/admin_screens_phase4.md) | Chưa thực hiện |
| AIL-AC-03 | Mục 3, 7; điểm kiểm DD: AI-L-05–08 | [TC-ADM0303-AIL-AC-03-01](../../testcases/screens/admin/admin_screens_phase4.md) | Chưa thực hiện |

