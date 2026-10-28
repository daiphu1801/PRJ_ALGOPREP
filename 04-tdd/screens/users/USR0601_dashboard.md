# Tiêu chí nghiệm thu — Màn USR0601 — Dashboard người học

> DD: `03-dd/screens/users/USR0601_dashboard.md` · Cập nhật: 2026-10-26.

## 1. Phạm vi

Các tiêu chí dưới đây kiểm hành vi màn hình và phối hợp giữa module. Hợp đồng nghiệp vụ của từng module được kiểm ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### DB-AC-01 — Các khối Dashboard tải và lỗi độc lập

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người học có dữ liệu ở nhiều phân hệ; một nguồn AI, bài nộp hoặc ngân hàng phỏng vấn có thể lỗi. |
| **Hành động** | Mở Dashboard rồi thử lại phần lỗi. |
| **Kết quả mong đợi** | Các khối còn nguồn tốt tiếp tục hiện. Phần AI báo lỗi riêng; lỗi bài nộp không biến số chưa biết thành số không. Thử lại một khối chỉ tải lại nguồn của khối đó; tài khoản mới thấy trạng thái rỗng có lời giải thích. |
| **Nguồn** | [SoT: 03-dd/screens/users/USR0601_dashboard.md] mục 3, 7 |

### DB-AC-02 — Đổi khoảng thời gian chỉ đổi số liệu theo kỳ

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Dashboard có số liệu luỹ kế và số liệu theo kỳ. |
| **Hành động** | Đổi từ toàn bộ sang bảy ngày rồi đổi lại. |
| **Kết quả mong đợi** | Các khối theo kỳ cập nhật cùng khoảng thời gian; số liệu luỹ kế giữ nguyên. Một nguồn được dùng chung giữa bảng và biểu đồ không bị tính thành hai lần tải. |
| **Nguồn** | [SoT: 03-dd/screens/users/USR0601_dashboard.md] mục 3, 7 |

### DB-AC-03 — Điểm năng lực và gợi ý dùng cùng kết quả

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người học đã giải bài thuộc nhiều chủ đề; một chủ đề chưa có bài. |
| **Hành động** | Mở radar, lời chào và danh sách bài nên ưu tiên. |
| **Kết quả mong đợi** | Các phần dùng cùng điểm năng lực do hệ thống trả; thứ tự bài gợi ý khớp điểm đó. Chủ đề chưa có dữ liệu không bị tính thành điểm không; khi dưới ba chủ đề có điểm, radar giải thích vì sao chưa vẽ. |
| **Nguồn** | [SoT: 03-dd/screens/users/USR0601_dashboard.md] mục 3, 7 |

### DB-AC-04 — Liên kết hành động bỏ bài đã rút

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người học có bài đang làm dở, gồm một bài đã rút; có phiên phỏng vấn cũ. |
| **Hành động** | Bấm Tiếp tục, tên bài, phiên phỏng vấn và các liên kết xem tất cả. |
| **Kết quả mong đợi** | Tiếp tục mở bài còn dùng được đầu tiên; nếu không có bài phù hợp thì dẫn tới danh sách bài. Phiên cũ mở ở chế độ chỉ xem, không tạo phiên mới. Liên kết báo cáo mở báo cáo đã có. |
| **Nguồn** | [SoT: 03-dd/screens/users/USR0601_dashboard.md] mục 3, 7 |

### DB-AC-05 — Lưới hoạt động và bảng chủ đề không hiển thị sai dữ liệu

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người học có dữ liệu hoạt động 364 ngày; sau đó nguồn trả thiếu ngày hoặc tiến độ chủ đề lỗi. |
| **Hành động** | Rê từng ô và xem bảng chủ đề. |
| **Kết quả mong đợi** | Mỗi ô nêu ngày và số bài nộp; thiếu ngày làm khối báo lỗi riêng, không vẽ lưới lệch. Bảng chủ đề giữ số đã giải khi nguồn tỷ lệ chấp thuận lỗi; tỷ lệ không biết hiển thị dấu gạch, không thành số không. |
| **Nguồn** | [SoT: 03-dd/screens/users/USR0601_dashboard.md] mục 3, 7 |

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| DB-AC-01 | Mục 3, 7; điểm kiểm DD: T-DB-01–05, T-DB-19–20 | [TC-USR0601-DBAC01-01](../../testcases/screens/users/USR0601_dashboard.md), [TC-USR0601-DBAC01-02](../../testcases/screens/users/USR0601_dashboard.md) | Chưa thực hiện |
| DB-AC-02 | Mục 3, 7; điểm kiểm DD: T-DB-06–07 | [TC-USR0601-DBAC02-01](../../testcases/screens/users/USR0601_dashboard.md) | Chưa thực hiện |
| DB-AC-03 | Mục 3, 7; điểm kiểm DD: T-DB-08–10 | [TC-USR0601-DBAC03-01](../../testcases/screens/users/USR0601_dashboard.md) | Chưa thực hiện |
| DB-AC-04 | Mục 3, 7; điểm kiểm DD: T-DB-11–12, T-DB-16–18 | [TC-USR0601-DBAC04-01](../../testcases/screens/users/USR0601_dashboard.md) | Chưa thực hiện |
| DB-AC-05 | Mục 3, 7; điểm kiểm DD: T-DB-13–15, T-DB-21 | [TC-USR0601-DBAC05-01](../../testcases/screens/users/USR0601_dashboard.md) | Chưa thực hiện |

