# Tiêu chí nghiệm thu — Màn ADM0501 — Cấu hình ngôn ngữ

> DD: `03-dd/screens/admin/ADM0501_language_config.md` · Cập nhật: 2026-10-26.

## 1. Phạm vi

Các tiêu chí này kiểm hành vi màn hình và phối hợp giữa module. Hợp đồng máy chủ tương ứng nằm ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### AL-AC-01 — Ba ngôn ngữ và bản nháp qua các tab

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Quản trị viên có quyền xem; Java, C++ và Python đã có cấu hình. |
| **Hành động** | Mở từng tab, sửa giới hạn rồi đổi tab và lưu. |
| **Kết quả mong đợi** | Tab ngôn ngữ hiện ba dòng Java/C++/Python với tên trình biên dịch chỉ đọc; tab còn lại chứa cấu hình sandbox. Bản nháp đủ bốn khối được giữ khi đổi hai tab. Một lần Lưu gửi trọn snapshot theo F4 §24; phản hồi thành công làm baseline mới cùng updatedAt cho lần lưu kế tiếp. |
| **Nguồn** | [SoT: 03-dd/screens/admin/ADM0501_language_config.md] mục 3, 7 |

### AL-AC-02 — Biên giới hạn và ngôn ngữ cuối

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Có một ngôn ngữ cuối đang bật và nhiều bài đang chờ. |
| **Hành động** | Nhập hệ số ở biên, thử tắt ngôn ngữ cuối và tắt ngôn ngữ khác. |
| **Kết quả mong đợi** | Giá trị ngoài biên bị chặn; không thể tắt ngôn ngữ cuối. Tắt ngôn ngữ khác có xác nhận một lần, cho biết số bài đang chờ; bài đã nhận tiếp tục được chấm. |
| **Nguồn** | [SoT: 03-dd/screens/admin/ADM0501_language_config.md] mục 3, 7 |

### AL-AC-03 — Lưu lỗi và xung đột giữ bản nháp

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Quản trị viên có bản nháp; cấu hình trên máy chủ vừa bị người khác đổi hoặc mạng lỗi. |
| **Hành động** | Bấm Lưu, thử tải lại bản máy chủ và rời màn. |
| **Kết quả mong đợi** | Lỗi không báo thành công và giữ bản nháp. Tải bản mới cần xác nhận trước khi bỏ nháp; rời màn với thay đổi chưa lưu có hộp thoại. Thiếu quyền sửa không gửi thay đổi, thiếu quyền đọc không lộ cấu hình. |
| **Nguồn** | [SoT: 03-dd/screens/admin/ADM0501_language_config.md] mục 3, 7 |

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| AL-AC-01 | Mục 3, 7; điểm kiểm DD: AL-01, AL-04, AL-08 | [TC-ADM0501-AL-AC-01-01](../../testcases/screens/admin/admin_screens_phase4.md) | Chưa thực hiện |
| AL-AC-02 | Mục 3, 7; điểm kiểm DD: AL-02–03, AL-09 | [TC-ADM0501-AL-AC-02-01](../../testcases/screens/admin/admin_screens_phase4.md) | Chưa thực hiện |
| AL-AC-03 | Mục 3, 7; điểm kiểm DD: AL-05–08 | [TC-ADM0501-AL-AC-03-01](../../testcases/screens/admin/admin_screens_phase4.md) | Chưa thực hiện |

