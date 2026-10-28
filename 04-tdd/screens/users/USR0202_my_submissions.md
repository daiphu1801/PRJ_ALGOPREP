# Tiêu chí nghiệm thu — Màn USR0202 — Bài nộp của tôi

> DD: `03-dd/screens/users/USR0202_my_submissions.md` · Cập nhật: 2026-10-26.

## 1. Phạm vi

Các tiêu chí dưới đây kiểm hành vi màn hình và phối hợp giữa module. Hợp đồng nghiệp vụ của từng module được kiểm ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### MY-AC-01 — Danh sách có trạng thái chờ và rỗng riêng

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người học chưa có bài nộp hoặc đang có bài chấm dở. |
| **Hành động** | Mở danh sách. |
| **Kết quả mong đợi** | Trong lúc tải không hiện câu rỗng; khi chưa từng nộp, màn chỉ đường tới danh sách bài. Bài chưa chấm xong hiện đúng trạng thái và không hiển thị điểm giả. |
| **Nguồn** | [SoT: 03-dd/screens/users/USR0202_my_submissions.md] mục 3, 7 |

### MY-AC-02 — Tìm, lọc và phân trang có thể quay lại

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người học có nhiều bài nộp thuộc các ngôn ngữ và kết quả khác nhau. |
| **Hành động** | Tìm kiếm, lọc kết quả và ngôn ngữ, đổi trang rồi quay lại. |
| **Kết quả mong đợi** | Các bộ lọc cộng dồn; thay bộ lọc về trang đầu. Đường dẫn giữ bộ lọc và trang; giá trị không hợp lệ được dọn. Khi tải lỗi, bộ lọc còn dùng được và có Thử lại; lỗi sau khi đổi lọc giữ bảng cũ để người học không mất ngữ cảnh. |
| **Nguồn** | [SoT: 03-dd/screens/users/USR0202_my_submissions.md] mục 3, 7 |

### MY-AC-03 — Liên kết kết quả luôn có, liên kết bài tùy trạng thái

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Danh sách có bài đang chấm, bài đã rút và bài không còn thông tin đề. |
| **Hành động** | Bấm Kết quả và thử bấm tên bài. |
| **Kết quả mong đợi** | Mọi dòng đều mở được kết quả của chính bài nộp đó. Bài đã rút hoặc không còn thông tin đề không dẫn đến đề cũ; bài còn xuất bản mở được đề. Quay lại danh sách sau khi chấm xong cho thấy trạng thái mới. |
| **Nguồn** | [SoT: 03-dd/screens/users/USR0202_my_submissions.md] mục 3, 7 |

### MY-AC-04 — Phân tích chỉ dành cho bài Accepted

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Danh sách có bài Accepted và bài ở trạng thái khác, gồm bài đã rút. |
| **Hành động** | Quan sát và bấm nút Phân tích. |
| **Kết quả mong đợi** | Bài Accepted mở được trang phân tích, kể cả khi đề đã rút; các bài khác không mở được và có giải thích. Dịch vụ AI lỗi không làm hỏng danh sách bài nộp. |
| **Nguồn** | [SoT: 03-dd/screens/users/USR0202_my_submissions.md] mục 3, 7 |

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| MY-AC-01 | Mục 3, 7; điểm kiểm DD: T-MY-01–03, T-MY-14 | [TC-USR0202-MYAC01-01](../../testcases/screens/users/USR0202_my_submissions.md) | Chưa thực hiện |
| MY-AC-02 | Mục 3, 7; điểm kiểm DD: T-MY-04–10 | [TC-USR0202-MYAC02-01](../../testcases/screens/users/USR0202_my_submissions.md) | Chưa thực hiện |
| MY-AC-03 | Mục 3, 7; điểm kiểm DD: T-MY-11, T-MY-13–15 | [TC-USR0202-MYAC03-01](../../testcases/screens/users/USR0202_my_submissions.md) | Chưa thực hiện |
| MY-AC-04 | Mục 3, 7; điểm kiểm DD: T-MY-12, T-MY-17 | [TC-USR0202-MYAC04-01](../../testcases/screens/users/USR0202_my_submissions.md) | Chưa thực hiện |

