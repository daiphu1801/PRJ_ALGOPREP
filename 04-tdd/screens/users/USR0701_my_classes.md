# TDD màn `USR0701` — Lớp của tôi

> DD: `03-dd/screens/users/USR0701_my_classes.md` · Cập nhật: 2026-10-26.

Các AC dưới đây chỉ kiểm luồng UI ghép F1/F2; nghiệp vụ từng module dùng `04-tdd/modules/F1_identity.md` AC-19, AC-20, AC-20b, AC-49, AC-50 và `04-tdd/modules/F2_problem-bank.md` AC-88.


## 1. Phạm vi

Các tiêu chí màn này kiểm hành vi quan sát được và phối hợp các module; hợp đồng từng module ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### AC-SC-01 — Danh sách lớp vẫn dùng được khi bài giao lỗi

- **Tiền điều kiện:** người học có bốn lớp, F1 #25A thành công; F2 #13A lỗi 503; một lớp có bài đã rút.
- **Hành động:** mở `/classes`, sau đó cho F2 thành công ở lần đọc kế.
- **Kết quả mong đợi:** lần đầu đủ bốn thẻ lớp, tên, lịch, giảng viên và link; thanh/câu tiến độ ẩn, không báo nhầm là chưa có bài. Khi F2 có dữ liệu, x/y xuất hiện, bài đã rút vẫn nằm trong y. F1 lỗi riêng thì danh sách thay bằng vùng lỗi nhưng form mã vẫn dùng được.

- **Nguồn**: [SoT: 03-dd/screens/users/USR0701_my_classes.md] mục 3, 7.

### AC-SC-02 — Gửi mã mời chỉ làm mới sau thành công

- **Tiền điều kiện:** màn có ô mã, hai query F1/F2 đã tải.
- **Hành động:** gửi ô rỗng; nhập chữ thường có khoảng trắng, gửi; lặp với mã sai và lỗi mạng.
- **Kết quả mong đợi:** ô rỗng không gọi API; mã hợp lệ được trim/uppercase trước khi gửi, nút khoá trong lúc chờ; thành công xoá ô, toast dùng tên lớp từ response, làm mới cả hai query; `IDT-410` và lỗi mạng cho thông báo khác nhau, giữ nguyên chuỗi ô, không làm mới danh sách.

- **Nguồn**: [SoT: 03-dd/screens/users/USR0701_my_classes.md] mục 3, 7.

### AC-SC-03 — Link thẻ lớp mở đúng chi tiết

- **Tiền điều kiện:** danh sách có hai lớp UUID khác nhau.
- **Hành động:** bấm “Xem chi tiết lớp” ở thẻ thứ hai khi ô mã đang có ký tự chưa gửi.
- **Kết quả mong đợi:** route là `/classes/{classId}` của lớp thứ hai, link có tên đọc kèm tên lớp; không hiện xác nhận bỏ ô mã.

- **Nguồn**: [SoT: 03-dd/screens/users/USR0701_my_classes.md] mục 3, 7.

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| AC-SC-01 | [DD màn](../../../03-dd/screens/users/USR0701_my_classes.md) mục 3, 7 | [TC-USR0701-SCAC01-01](../../testcases/screens/users/USR0701_my_classes.md) | Chưa thực hiện |
| AC-SC-02 | [DD màn](../../../03-dd/screens/users/USR0701_my_classes.md) mục 3, 7 | [TC-USR0701-SCAC02-01](../../testcases/screens/users/USR0701_my_classes.md) | Chưa thực hiện |
| AC-SC-03 | [DD màn](../../../03-dd/screens/users/USR0701_my_classes.md) mục 3, 7 | [TC-USR0701-SCAC03-01](../../testcases/screens/users/USR0701_my_classes.md) | Chưa thực hiện |
