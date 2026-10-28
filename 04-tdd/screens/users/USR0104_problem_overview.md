# TDD màn `USR0104` — Tổng quan bài toán

> DD: `03-dd/screens/users/USR0104_problem_overview.md` · Cập nhật: 2026-10-26.

Các tiêu chí chỉ kiểm phối hợp F2/F4/F5 và thao tác trình duyệt; API riêng dùng AC module.


## 1. Phạm vi

Các tiêu chí màn này kiểm hành vi quan sát được và phối hợp các module; hợp đồng từng module ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### AC-SC-01 — Đề vẫn dùng được khi hai thẻ phụ lỗi

- **Tiền điều kiện:** F2 overview thành công, F4 lịch sử hoặc F5 quick insight lỗi.
- **Hành động:** mở `/problems/{problemId}`.
- **Kết quả mong đợi:** đề, ví dụ và bookmark vẫn hoạt động; thẻ phụ tương ứng ẩn, không toast làm người dùng hiểu đề lỗi. F4 giới hạn trả ba ngôn ngữ trong một lời gọi khi không truyền `language`.

- **Nguồn**: [SoT: 03-dd/screens/users/USR0104_problem_overview.md] mục 3, 7.

### AC-SC-02 — Bookmark Hoàn tác ghi lại lên máy chủ

- **Tiền điều kiện:** bài đã được lưu.
- **Hành động:** bỏ lưu, bấm Hoàn tác; lặp khi lời gọi Hoàn tác lỗi.
- **Kết quả mong đợi:** cả bỏ lưu và Hoàn tác đều gọi F2 mục 12 với trạng thái đích; lỗi đưa UI về trạng thái server gần nhất, không tuyên bố đã khôi phục ghi chú cũ.

- **Nguồn**: [SoT: 03-dd/screens/users/USR0104_problem_overview.md] mục 3, 7.

### AC-SC-03 — Điều hướng và sao chép ví dụ

- **Tiền điều kiện:** overview có ví dụ, bài nộp gần đây và quick insight gắn `submissionId`.
- **Hành động:** mở Workspace, bài đã lưu, danh sách, kết quả, báo cáo; copy Input/Output khi Clipboard API bị từ chối.
- **Kết quả mong đợi:** mỗi link dùng đúng ID và route theo BD; báo cáo dùng `review.submissionId` của F5; copy lỗi hiện hướng dẫn thủ công, không mất nội dung ví dụ.

- **Nguồn**: [SoT: 03-dd/screens/users/USR0104_problem_overview.md] mục 3, 7.

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| AC-SC-01 | [DD màn](../../../03-dd/screens/users/USR0104_problem_overview.md) mục 3, 7 | [TC-USR0104-SCAC01-01](../../testcases/screens/users/USR0104_problem_overview.md) | Chưa thực hiện |
| AC-SC-02 | [DD màn](../../../03-dd/screens/users/USR0104_problem_overview.md) mục 3, 7 | [TC-USR0104-SCAC02-01](../../testcases/screens/users/USR0104_problem_overview.md) | Chưa thực hiện |
| AC-SC-03 | [DD màn](../../../03-dd/screens/users/USR0104_problem_overview.md) mục 3, 7 | [TC-USR0104-SCAC03-01](../../testcases/screens/users/USR0104_problem_overview.md) | Chưa thực hiện |
