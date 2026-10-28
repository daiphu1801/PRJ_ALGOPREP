# Tiêu chí nghiệm thu — Màn USR0101 — Danh sách bài toán

> DD: `03-dd/screens/users/USR0101_problem_list.md` · Cập nhật: 2026-10-26.

## 1. Phạm vi

Các tiêu chí dưới đây kiểm hành vi màn hình và phối hợp giữa module. Hợp đồng nghiệp vụ của từng module được kiểm ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### PL-AC-01 — Các khối tải và lỗi độc lập

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người học mở danh sách bài toán; dữ liệu bài, bài đang làm dở, nhãn phân tích và bài tập lớp có thể lần lượt lỗi. |
| **Hành động** | Mở màn rồi thử lại riêng khối bị lỗi. |
| **Kết quả mong đợi** | Bảng bài vẫn dùng được khi một khối phụ lỗi. Bài đang làm dở/bài tập lớp lỗi thì ẩn đúng khối; nhãn phân tích lỗi thì chỉ ẩn nhãn, không toast. Chỉ khi bảng bài lỗi mới hiện vùng lỗi của bảng cùng nút Thử lại; tiêu đề và các lối điều hướng vẫn hiện. |
| **Nguồn** | [SoT: 03-dd/screens/users/USR0101_problem_list.md] mục 3, 7 |

### PL-AC-02 — Bộ lọc, sắp xếp và phân trang giữ trên đường dẫn

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Danh sách có nhiều trang và đã chọn một số bộ lọc. |
| **Hành động** | Tìm kiếm, đổi bộ lọc, sắp xếp, đổi trang, mở một bài rồi quay lại. |
| **Kết quả mong đợi** | Các bộ lọc cộng dồn; thay bộ lọc về trang đầu, quay lại giữ trang cũ. Giá trị không hợp lệ trên đường dẫn được dọn và dùng mặc định; xoá bộ lọc dọn các tham số. Khối phụ không tải lại vì thay bộ lọc bảng. |
| **Nguồn** | [SoT: 03-dd/screens/users/USR0101_problem_list.md] mục 3, 7 |

### PL-AC-03 — Bài ngẫu nhiên tôn trọng bộ lọc

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Danh sách có bài phù hợp bộ lọc hoặc không có bài nào. |
| **Hành động** | Bấm Bài ngẫu nhiên. |
| **Kết quả mong đợi** | Khi có bài, màn mở một bài đúng bộ lọc đang áp. Khi không có bài, nút không thể bấm; khi không lấy được bài, màn báo lỗi và ở lại danh sách. |
| **Nguồn** | [SoT: 03-dd/screens/users/USR0101_problem_list.md] mục 3, 7 |

### PL-AC-04 — Điều hướng từ các khối phụ

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người học có bài đã lưu, bài đang làm dở và bài tập lớp. |
| **Hành động** | Bấm các lối đi từ đầu trang và từ thẻ bài. |
| **Kết quả mong đợi** | Mỗi lối mở đúng màn đích. Thẻ bài đã rút hoặc không còn thông tin bài không tạo liên kết sai; khi không có bài dở, khối này được ẩn. |
| **Nguồn** | [SoT: 03-dd/screens/users/USR0101_problem_list.md] mục 3, 7 |

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| PL-AC-01 | Mục 3, 7; điểm kiểm DD: T-PL-01–06, T-PL-20 | [TC-USR0101-PLAC01-01](../../testcases/screens/users/USR0101_problem_list.md), [TC-USR0101-PLAC01-02](../../testcases/screens/users/USR0101_problem_list.md) | Chưa thực hiện |
| PL-AC-02 | Mục 3, 7; điểm kiểm DD: T-PL-07–14 | [TC-USR0101-PLAC02-01](../../testcases/screens/users/USR0101_problem_list.md) | Chưa thực hiện |
| PL-AC-03 | Mục 3, 7; điểm kiểm DD: T-PL-16–17 | [TC-USR0101-PLAC03-01](../../testcases/screens/users/USR0101_problem_list.md) | Chưa thực hiện |
| PL-AC-04 | Mục 3, 7; điểm kiểm DD: T-PL-18–22 | [TC-USR0101-PLAC04-01](../../testcases/screens/users/USR0101_problem_list.md) | Chưa thực hiện |

