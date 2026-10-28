# TDD màn `USR0702` — Chi tiết lớp của tôi

> DD: `03-dd/screens/users/USR0702_my_class_detail.md` · Cập nhật: 2026-10-26.

Các AC này kiểm phối hợp hai module và điều hướng. Phạm vi dữ liệu dùng `04-tdd/modules/F1_identity.md` AC-49 và `04-tdd/modules/F2_problem-bank.md` AC-88.


## 1. Phạm vi

Các tiêu chí màn này kiểm hành vi quan sát được và phối hợp các module; hợp đồng từng module ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### AC-SC-01 — Tiến độ khớp bảng bài giao

- **Tiền điều kiện:** F1 trả lớp L; F2 trả ba bài còn giao: một `SOLVED`, một `UNSOLVED`, một `withdrawn=true` và `SOLVED`; bài thứ tư đã gỡ khỏi lớp.
- **Hành động:** mở `/classes/{L}`, sau đó thử lớp không có bài giao.
- **Kết quả mong đợi:** bảng có ba dòng, tiến độ 2/3, bài đã rút có nhãn và không là link; bài gỡ vắng mặt. Lớp rỗng hiện 0/0, thanh rỗng, không chia cho 0.

- **Nguồn**: [SoT: 03-dd/screens/users/USR0702_my_class_detail.md] mục 3, 7.

### AC-SC-02 — Lỗi F1/F2 được khoanh đúng vùng

- **Tiền điều kiện:** lần lượt mô phỏng F1 `IDT-411`, F1 503, F2 `PRB-015`, F2 503.
- **Hành động:** mở trang và bấm Thử lại ở vùng lỗi.
- **Kết quả mong đợi:** hai mã 404 và UUID sai đều vào cùng trang không tìm thấy; F1 503 là vùng lỗi toàn trang; F2 503 giữ thông tin lớp, chỉ vùng tiến độ/bảng lỗi; Thử lại chỉ gọi nguồn lỗi, lỗi lặp mới hiện toast.

- **Nguồn**: [SoT: 03-dd/screens/users/USR0702_my_class_detail.md] mục 3, 7.

### AC-SC-03 — Điều hướng không mở bài đã rút

- **Tiền điều kiện:** bảng có một bài đang công khai và một bài `withdrawn=true`.
- **Hành động:** bấm bài công khai, quay lại, thử bàn phím trên bài rút, rồi bấm nút quay lại danh sách lớp.
- **Kết quả mong đợi:** bài công khai mở `/problems/{problemId}`; khi quay lại trạng thái giải được tải mới; bài rút không có link hoặc điểm focus; nút quay lại mở `/classes`.

- **Nguồn**: [SoT: 03-dd/screens/users/USR0702_my_class_detail.md] mục 3, 7.

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| AC-SC-01 | [DD màn](../../../03-dd/screens/users/USR0702_my_class_detail.md) mục 3, 7 | [TC-USR0702-SCAC01-01](../../testcases/screens/users/USR0702_my_class_detail.md), [TC-USR0702-SCAC01-02](../../testcases/screens/users/USR0702_my_class_detail.md) | Chưa thực hiện |
| AC-SC-02 | [DD màn](../../../03-dd/screens/users/USR0702_my_class_detail.md) mục 3, 7 | [TC-USR0702-SCAC02-01](../../testcases/screens/users/USR0702_my_class_detail.md) | Chưa thực hiện |
| AC-SC-03 | [DD màn](../../../03-dd/screens/users/USR0702_my_class_detail.md) mục 3, 7 | [TC-USR0702-SCAC03-01](../../testcases/screens/users/USR0702_my_class_detail.md) | Chưa thực hiện |
