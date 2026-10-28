# Tiêu chí nghiệm thu — Màn SHR0203 — Thông tin bài toán

> DD: `03-dd/screens/shared/SHR0203_problem_info.md` · Cập nhật: 2026-10-26.

## 1. Phạm vi

Các tiêu chí này kiểm hành vi màn hình và phối hợp giữa module. Hợp đồng máy chủ tương ứng nằm ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### PI-AC-01 — Xem bài theo phạm vi và trạng thái

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Có bài xuất bản, bài nháp của mình, bài nháp của người khác và mã bài không tồn tại. |
| **Hành động** | Mở từng bài bằng tài khoản tác giả, người soạn khác và quản trị viên. |
| **Kết quả mong đợi** | Tác giả và quản trị viên xem được dữ liệu trong phạm vi; người soạn khác xem bài xuất bản ở chế độ chỉ đọc, không thấy testcase ẩn hoặc mã đáp án mẫu. Bài nháp ngoài phạm vi và mã không tồn tại cho cùng trạng thái không tìm thấy. |
| **Nguồn** | [SoT: 03-dd/screens/shared/SHR0203_problem_info.md] mục 3, 7 |

### PI-AC-02 — Dữ liệu thiếu có trạng thái rỗng đúng

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Bài hợp lệ nhưng chưa có đặc tả hàm hoặc đáp án mẫu. |
| **Hành động** | Mở trang và các phần chi tiết. |
| **Kết quả mong đợi** | Các phần thiếu có lời giải thích, không lỗi hiển thị; trang không bịa kết quả chạy, Stack hoặc tỷ lệ chấp thuận khi nguồn không cung cấp. |
| **Nguồn** | [SoT: 03-dd/screens/shared/SHR0203_problem_info.md] mục 3, 7 |

### PI-AC-03 — Điều hướng và lỗi tải có phân biệt

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người soạn mở một bài, sau đó gặp thiếu quyền hoặc lỗi mạng. |
| **Hành động** | Bấm Sửa, Xem trước, Quay lại; thử tải lại khi lỗi. |
| **Kết quả mong đợi** | Các lối mở đúng bài và đúng khu; Xem trước ở tab riêng. Thiếu quyền, không tìm thấy và lỗi mạng được giải thích phù hợp; Thử lại không đổi đường dẫn. |
| **Nguồn** | [SoT: 03-dd/screens/shared/SHR0203_problem_info.md] mục 3, 7 |

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| PI-AC-01 | Mục 3, 7; điểm kiểm DD: PI-01–03 | [TC-SHR0203-PIAC01-01](../../testcases/screens/shared/SHR0203_problem_info.md), [TC-SHR0203-PIAC01-02](../../testcases/screens/shared/SHR0203_problem_info.md), [TC-SHR0203-PIAC01-03](../../testcases/screens/shared/SHR0203_problem_info.md) | Chưa thực hiện |
| PI-AC-02 | Mục 3, 7; điểm kiểm DD: PI-04, PI-07 | [TC-SHR0203-PIAC02-01](../../testcases/screens/shared/SHR0203_problem_info.md) | Chưa thực hiện |
| PI-AC-03 | Mục 3, 7; điểm kiểm DD: PI-05–06 | [TC-SHR0203-PIAC03-01](../../testcases/screens/shared/SHR0203_problem_info.md), [TC-SHR0203-PIAC03-02](../../testcases/screens/shared/SHR0203_problem_info.md) | Chưa thực hiện |

