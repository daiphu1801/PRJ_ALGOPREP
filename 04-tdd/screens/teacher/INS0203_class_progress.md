# TDD màn `INS0203` — Tiến độ lớp

> DD: `03-dd/screens/teacher/INS0203_class_progress.md` · Cập nhật: 2026-10-26.


## 1. Phạm vi

Các tiêu chí màn này kiểm hành vi quan sát được và phối hợp các module; hợp đồng từng module ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### AC-SC-01 — Tab lớp đồng bộ biểu đồ, chú ý và bảng
- **Tiền điều kiện:** giảng viên có hai lớp với điểm tuần và học viên cần chú ý khác nhau.
- **Hành động:** chọn tab một lớp rồi Tất cả.
- **Kết quả mong đợi:** ba khối cùng phạm vi `classId`; mỗi biểu đồ một đường, đúng sáu tuần ISO đã kết thúc; điểm tuần thiếu là khoảng trống; đổi tab reset trang bảng và cột Lớp; dữ liệu tab cũ không xuất hiện sau response chậm.

- **Nguồn**: [SoT: 03-dd/screens/teacher/INS0203_class_progress.md] mục 3, 7.

### AC-SC-02 — Tìm kiếm chỉ đổi bảng, retry từng vùng
- **Tiền điều kiện:** bảng có nhiều trang; F1 #58 lỗi trong khi #57 và bảng thành công.
- **Hành động:** tìm học viên, đổi trang, retry khối chú ý, mở một học viên từ bảng và một người từ khối chú ý.
- **Kết quả mong đợi:** tìm/trang chỉ gọi query bảng, khối khác không reload; retry #58 không làm mất biểu đồ; cả hai link truyền đúng cặp `classId/studentId`.

- **Nguồn**: [SoT: 03-dd/screens/teacher/INS0203_class_progress.md] mục 3, 7.

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| AC-SC-01 | [DD màn](../../../03-dd/screens/teacher/INS0203_class_progress.md) mục 3, 7 | [TC-INS0203-AC-SC-01-01](../../testcases/screens/teacher/teacher_screens_phase4.md) | Chưa thực hiện |
| AC-SC-02 | [DD màn](../../../03-dd/screens/teacher/INS0203_class_progress.md) mục 3, 7 | [TC-INS0203-AC-SC-02-01](../../testcases/screens/teacher/teacher_screens_phase4.md) | Chưa thực hiện |
