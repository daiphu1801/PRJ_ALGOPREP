# Tiêu chí nghiệm thu — Màn USR0502 — Hồ sơ

> DD: `03-dd/screens/users/USR0502_profile.md` · Cập nhật: 2026-10-26.

## 1. Phạm vi

Các tiêu chí dưới đây kiểm hành vi màn hình và phối hợp giữa module. Hợp đồng nghiệp vụ của từng module được kiểm ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### PF-AC-01 — Các phần hồ sơ tải và thử lại độc lập

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người học có hồ sơ và số liệu luyện tập; một nguồn dữ liệu tạm lỗi. |
| **Hành động** | Mở hồ sơ rồi thử lại phần lỗi. |
| **Kết quả mong đợi** | Thông tin tài khoản, luyện tập và phỏng vấn hiện theo phần có dữ liệu. Lỗi một phần không che các phần khác; phần phỏng vấn lỗi hiển thị dấu gạch, không báo lỗi toàn màn. Thử lại chỉ tải phần vừa lỗi. |
| **Nguồn** | [SoT: 03-dd/screens/users/USR0502_profile.md] mục 3, 7 |

### PF-AC-02 — Sửa hồ sơ giữ bản nháp khi lưu lỗi

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người học sửa tên và thông tin cá nhân. |
| **Hành động** | Bấm Lưu, Hoàn tác, rồi thử dữ liệu không hợp lệ và lỗi máy chủ. |
| **Kết quả mong đợi** | Chỉ các trường được phép được gửi đi; lưu thành công cập nhật thẻ danh tính. Hoàn tác trả giá trị đã lưu. Giá trị không hợp lệ bị chặn, lỗi máy chủ giữ bản nháp và chỉ ra ô cần sửa. |
| **Nguồn** | [SoT: 03-dd/screens/users/USR0502_profile.md] mục 3, 7 |

### PF-AC-03 — Đổi email xác minh trước khi dùng

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người học có email cũ và sửa đồng thời thông tin hồ sơ. |
| **Hành động** | Yêu cầu đổi email, nhập mã xác minh, gửi lại mã hoặc huỷ hộp thoại. |
| **Kết quả mong đợi** | Email đăng nhập chỉ đổi sau khi mã đúng; hồ sơ và yêu cầu email có kết quả độc lập. Mã sai giữ hộp thoại để sửa; gửi lại chịu thời gian chờ và cùng hạn mức ba lần trong 24 giờ. Huỷ giữ email đăng nhập cũ và bản nháp chưa lưu. |
| **Nguồn** | [SoT: 03-dd/screens/users/USR0502_profile.md] mục 3, 7 |

### PF-AC-04 — Rời màn khi còn thay đổi chưa lưu

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người học đã sửa hồ sơ hoặc email nhưng chưa hoàn tất. |
| **Hành động** | Bấm đường tới Trang tổng quan hoặc Cài đặt và chọn Lưu và thoát. |
| **Kết quả mong đợi** | Màn hỏi cách xử lý thay đổi; chỉ rời khi phần phải lưu đã thành công. Nếu đổi email còn chờ xác minh, người học ở lại để hoàn tất hoặc hủy bản nháp. |
| **Nguồn** | [SoT: 03-dd/screens/users/USR0502_profile.md] mục 3, 7 |

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| PF-AC-01 | Mục 3, 7; điểm kiểm DD: T-PF-01–03, T-PF-18 | [TC-USR0502-PFAC01-01](../../testcases/screens/users/USR0502_profile.md) | Chưa thực hiện |
| PF-AC-02 | Mục 3, 7; điểm kiểm DD: T-PF-04–07 | [TC-USR0502-PFAC02-01](../../testcases/screens/users/USR0502_profile.md) | Chưa thực hiện |
| PF-AC-03 | Mục 3, 7; điểm kiểm DD: T-PF-08–15, T-PF-20 | [TC-USR0502-PFAC03-01](../../testcases/screens/users/USR0502_profile.md) | Chưa thực hiện |
| PF-AC-04 | Mục 3, 7; điểm kiểm DD: T-PF-16–17 | [TC-USR0502-PFAC04-01](../../testcases/screens/users/USR0502_profile.md) | Chưa thực hiện |

