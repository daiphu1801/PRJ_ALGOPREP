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

### AC-SC-04 — Khách đọc được đề, hành động cần phiên nhắc đăng nhập

- **Tiền điều kiện:** người xem không có phiên; bài đã xuất bản.
- **Hành động:** mở `/problems/{problemId}` trực tiếp; bấm "Giải bài toán"; đóng hộp thoại; bấm "Lưu bài"; chọn Đăng nhập.
- **Kết quả mong đợi:** khách xem được đề và ví dụ. Bảng giới hạn, bài nộp gần đây và Solution Review yêu cầu phiên. "Giải bài toán" và "Lưu bài" mở hộp thoại "Cần đăng nhập". Chọn Đăng nhập dẫn tới `/login?redirect=/problems/{problemId}` và đăng nhập xong quay lại đúng bài.

- **Nguồn**: [SoT: 03-dd/screens/users/USR0104_problem_overview.md] mục 3, 4A, 7.

### AC-SC-05 — Bố cục đọc đề và ví dụ trên màn hình rộng và hẹp

- **Tiền điều kiện** Mở một bài đã xuất bản có ít nhất một ví dụ.
- **Hành động** Mở màn ở 390 px và 1440 px, dùng liên kết Đến ví dụ và bấm Sao chép đầu vào.
- **Kết quả mong đợi** Ở 390 px, Giải bài toán xuất hiện trước phần đề, hai khối ví dụ xếp dọc và trang không cuộn ngang. Ở 1440 px, hai khối cùng ví dụ nằm cạnh nhau. Liên kết đưa người đọc đến mục Ví dụ và sao chép giữ nguyên dữ liệu gốc.

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| AC-SC-01 | [DD màn](../../../03-dd/screens/users/USR0104_problem_overview.md) mục 3, 7 | [TC-USR0104-SCAC01-01](../../testcases/screens/users/USR0104_problem_overview.md) | Chưa thực hiện |
| AC-SC-02 | [DD màn](../../../03-dd/screens/users/USR0104_problem_overview.md) mục 3, 7 | [TC-USR0104-SCAC02-01](../../testcases/screens/users/USR0104_problem_overview.md) | Chưa thực hiện |
| AC-SC-03 | [DD màn](../../../03-dd/screens/users/USR0104_problem_overview.md) mục 3, 7 | [TC-USR0104-SCAC03-01](../../testcases/screens/users/USR0104_problem_overview.md) | Chưa thực hiện |
| AC-SC-04 | [DD màn](../../../03-dd/screens/users/USR0104_problem_overview.md) mục 3, 4A, 7; điểm kiểm DD: SC-08–10 | `problem-overview-guest.test.tsx`; `e2e/guest-browse-real.spec.ts` | Đạt (2026-11-26) |
| AC-SC-05 | [DD màn](../../../03-dd/screens/users/USR0104_problem_overview.md) BR-SC-05 | [Kiểm tra Chromium và ảnh thực tế](../../../06-plan/reports/261126-1513-ui-tong-quan-bai-toan/report.md) | Đạt (2026-11-26) |
