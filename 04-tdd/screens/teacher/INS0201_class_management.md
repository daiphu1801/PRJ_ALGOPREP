# TDD màn `INS0201` — Quản lý lớp

> DD: `03-dd/screens/teacher/INS0201_class_management.md` · Cập nhật: 2026-10-26.


## 1. Phạm vi

Các tiêu chí màn này kiểm hành vi quan sát được và phối hợp các module; hợp đồng từng module ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### AC-SC-01 — Lớp và bảng học viên độc lập
- **Tiền điều kiện:** ba lớp, học viên thuộc nhiều lớp; một khối tải lỗi.
- **Hành động:** mở màn, chọn thẻ lớp, chọn tab khác, bấm lại thẻ, chuyển trang.
- **Kết quả mong đợi:** ba thẻ tổng lấy từ F1 #56, không cộng học viên trùng; lỗi chỉ ở khối liên quan; thẻ và tab đồng bộ; lọc một lớp ẩn cột Lớp, tab Tất cả phân trang trên server, đổi tab về trang 1.

- **Nguồn**: [SoT: 03-dd/screens/teacher/INS0201_class_management.md] mục 3, 7.

### AC-SC-02 — Tạo/sửa và xác nhận thao tác nguy hiểm
- **Tiền điều kiện:** form lớp có tên, mô tả, lịch cấu trúc; lớp hiện có một học viên.
- **Hành động:** tạo lớp, sửa lớp hai lần, huỷ rồi xác nhận gỡ học viên, huỷ rồi xác nhận xoá lớp.
- **Kết quả mong đợi:** tạo #20 đúng một lần, server tạo mã mời đầu tiên, chuyển chi tiết theo `classId`; sửa mở lại dữ liệu mới và lỗi không mất draft; huỷ không gọi API; gỡ giữ lịch sử; xoá thành công làm mới thẻ/tổng/bảng.

- **Nguồn**: [SoT: 03-dd/screens/teacher/INS0201_class_management.md] mục 3, 7.

### AC-SC-03 — Form lớp chặn dữ liệu sai trước khi gửi
- **Tiền điều kiện:** giảng viên đang tạo hoặc sửa lớp, form chứa tên, mô tả và lịch học.
- **Hành động:** để trống tên, nhập tên/mô tả quá giới hạn hoặc lịch không hợp lệ, rồi bấm Lưu.
- **Kết quả mong đợi:** trường sai hiện lỗi cụ thể và nhận focus; không gửi yêu cầu lưu, không tạo hay đổi lớp. Khi sửa hợp lệ, lỗi được xoá và form có thể lưu.

- **Nguồn**: [SoT: 03-dd/screens/teacher/INS0201_class_management.md] mục 5, SC-03.

### AC-SC-04 — Mã mời chỉ quản lý ở chi tiết lớp
- **Tiền điều kiện:** giảng viên mở danh sách lớp có mã mời đang hoạt động.
- **Hành động:** xem thẻ lớp, menu và form quản lý, rồi mở chi tiết lớp.
- **Kết quả mong đợi:** màn quản lý không hiện nút xem, sao chép hoặc tạo lại mã mời; các thao tác mã mời chỉ nằm ở chi tiết lớp và liên kết tới lớp đúng.

- **Nguồn**: [SoT: 03-dd/screens/teacher/INS0201_class_management.md] SC-05; [SoT: 03-dd/screens/teacher/INS0205_class_detail.md] mục 3.

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| AC-SC-01 | [DD màn](../../../03-dd/screens/teacher/INS0201_class_management.md) mục 3, 7 | [TC-INS0201-AC-SC-01-01](../../testcases/screens/teacher/teacher_screens_phase4.md) | Chưa thực hiện |
| AC-SC-02 | [DD màn](../../../03-dd/screens/teacher/INS0201_class_management.md) mục 3, 7 | [TC-INS0201-AC-SC-02-01](../../testcases/screens/teacher/teacher_screens_phase4.md), [TC-INS0201-AC-SC-02-02](../../testcases/screens/teacher/teacher_screens_phase4.md), [TC-INS0201-AC-SC-02-03](../../testcases/screens/teacher/teacher_screens_phase4.md) | Chưa thực hiện |
| AC-SC-03 | [DD màn](../../../03-dd/screens/teacher/INS0201_class_management.md) SC-03 | [TC-INS0201-AC-SC-03-01](../../testcases/screens/teacher/teacher_screens_phase4.md), [TC-INS0201-AC-SC-03-02](../../testcases/screens/teacher/teacher_screens_phase4.md), [TC-INS0201-AC-SC-03-03](../../testcases/screens/teacher/teacher_screens_phase4.md) | Chưa thực hiện |
| AC-SC-04 | [DD màn](../../../03-dd/screens/teacher/INS0201_class_management.md) SC-05 | [TC-INS0201-AC-SC-04-01](../../testcases/screens/teacher/teacher_screens_phase4.md) | Chưa thực hiện |
