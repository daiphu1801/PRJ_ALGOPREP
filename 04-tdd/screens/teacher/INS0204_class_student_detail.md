# TDD màn `INS0204` — Học viên trong lớp

> DD: `03-dd/screens/teacher/INS0204_class_student_detail.md` · Cập nhật: 2026-10-26.


## 1. Phạm vi

Các tiêu chí màn này kiểm hành vi quan sát được và phối hợp các module; hợp đồng từng module ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### AC-SC-01 — Không ghép dữ liệu khác lớp hoặc khác học viên
- **Tiền điều kiện:** route chuyển nhanh từ cặp lớp/học viên A sang B, response A chậm; F2 hoặc F4 có thể lỗi.
- **Hành động:** mở A rồi B; thử F1 404 và lỗi riêng F2/F4.
- **Kết quả mong đợi:** chỉ dữ liệu B hiện; F1 404 xoá dữ liệu phụ và hiện không tìm thấy; F2/F4 lỗi chỉ khối liên quan. Browser không gọi F4 P1 vì đó là cổng nội bộ F1.

- **Nguồn**: [SoT: 03-dd/screens/teacher/INS0204_class_student_detail.md] mục 3, 7.

### AC-SC-02 — Phân trang, gỡ và mở kết quả
- **Tiền điều kiện:** 25 lượt nộp trong phạm vi lớp, một bài chưa chấm, học viên đang ở lớp.
- **Hành động:** tới trang 2, mở kết quả, huỷ rồi xác nhận gỡ học viên.
- **Kết quả mong đợi:** trang 2 chỉ tải F4 #12, tỉ lệ chưa chấm là `-`; kết quả dùng F4 #11 kiểm quyền; huỷ không ghi; gỡ chỉ đặt `left_at`, giữ lịch sử và trở về màn gọi khi thành công.

- **Nguồn**: [SoT: 03-dd/screens/teacher/INS0204_class_student_detail.md] mục 3, 7.

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| AC-SC-01 | [DD màn](../../../03-dd/screens/teacher/INS0204_class_student_detail.md) mục 3, 7 | [TC-INS0204-AC-SC-01-01](../../testcases/screens/teacher/teacher_screens_phase4.md), [TC-INS0204-AC-SC-01-02](../../testcases/screens/teacher/teacher_screens_phase4.md) | Chưa thực hiện |
| AC-SC-02 | [DD màn](../../../03-dd/screens/teacher/INS0204_class_student_detail.md) mục 3, 7 | [TC-INS0204-AC-SC-02-01](../../testcases/screens/teacher/teacher_screens_phase4.md), [TC-INS0204-AC-SC-02-02](../../testcases/screens/teacher/teacher_screens_phase4.md) | Chưa thực hiện |
