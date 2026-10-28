# TDD màn `INS0205` — Chi tiết lớp

> DD: `03-dd/screens/teacher/INS0205_class_detail.md` · Cập nhật: 2026-10-26.


## 1. Phạm vi

Các tiêu chí màn này kiểm hành vi quan sát được và phối hợp các module; hợp đồng từng module ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### AC-SC-01 — Ba vùng đọc độc lập và lớp ngoài phạm vi
- **Tiền điều kiện:** F1 #19 trả lớp, #24 lỗi, #26 thành công; thử lớp người khác.
- **Hành động:** mở trang, retry vùng mã mời, mở lớp người khác.
- **Kết quả mong đợi:** metadata và bảng học viên vẫn hiện khi mã mời lỗi; retry chỉ #24; lớp người khác cùng trạng thái không tìm thấy như lớp không tồn tại, không giữ dữ liệu lớp cũ.

- **Nguồn**: [SoT: 03-dd/screens/teacher/INS0205_class_detail.md] mục 3, 7.

### AC-SC-02 — Sửa, mã mời, gỡ và xoá lớp
- **Tiền điều kiện:** lớp có mã mời còn hạn và một học viên.
- **Hành động:** sửa lịch, tạo mã mới, copy mã, huỷ rồi xác nhận gỡ học viên, huỷ rồi xác nhận xoá lớp.
- **Kết quả mong đợi:** sửa thành công cập nhật metadata; mã mới đứng đầu nhưng mã cũ còn hiệu lực; copy lỗi có hướng dẫn; huỷ không gọi API; gỡ giữ lịch sử; xoá chỉ chuyển danh sách khi #22 thành công.

- **Nguồn**: [SoT: 03-dd/screens/teacher/INS0205_class_detail.md] mục 3, 7.

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| AC-SC-01 | [DD màn](../../../03-dd/screens/teacher/INS0205_class_detail.md) mục 3, 7 | [TC-INS0205-AC-SC-01-01](../../testcases/screens/teacher/teacher_screens_phase4.md) | Chưa thực hiện |
| AC-SC-02 | [DD màn](../../../03-dd/screens/teacher/INS0205_class_detail.md) mục 3, 7 | [TC-INS0205-AC-SC-02-01](../../testcases/screens/teacher/teacher_screens_phase4.md), [TC-INS0205-AC-SC-02-02](../../testcases/screens/teacher/teacher_screens_phase4.md), [TC-INS0205-AC-SC-02-03](../../testcases/screens/teacher/teacher_screens_phase4.md) | Chưa thực hiện |
