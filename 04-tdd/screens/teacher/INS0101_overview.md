# TDD màn `INS0101` — Tổng quan giảng viên

> DD: `03-dd/screens/teacher/INS0101_overview.md` · Cập nhật: 2026-10-26.


## 1. Phạm vi

Các tiêu chí màn này kiểm hành vi quan sát được và phối hợp các module; hợp đồng từng module ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### AC-SC-01 — Ba nguồn tải độc lập
- **Tiền điều kiện:** F1 #56/#45 thành công, F2 §55 lỗi; một học viên thuộc hai lớp.
- **Hành động:** mở Overview rồi làm mới sau khi F2 phục hồi.
- **Kết quả mong đợi:** tổng học viên đếm DISTINCT, hoạt động và lớp vẫn hiện khi bài giao lỗi; vùng bài giao có retry. Khi F2 phục hồi, đúng năm bài có nhiều lượt nộp trong lớp của giảng viên xuất hiện, không dùng tổng toàn hệ thống.

- **Nguồn**: [SoT: 03-dd/screens/teacher/INS0101_overview.md] mục 3, 7.

### AC-SC-02 — Chỉ điều hướng ở hai liên kết đã chốt
- **Tiền điều kiện:** Overview có lớp và bài giao.
- **Hành động:** bấm thẻ lớp, dòng bài, link Quản lý lớp, link Xem tất cả; kiểm hai điều khiển đã bỏ.
- **Kết quả mong đợi:** thẻ/dòng chỉ đọc, hai link mở `/instructor/classes` và `/instructor/assignments`; không còn ô tìm kiếm và nút tạo lớp trên Overview.

- **Nguồn**: [SoT: 03-dd/screens/teacher/INS0101_overview.md] mục 3, 7.

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| AC-SC-01 | [DD màn](../../../03-dd/screens/teacher/INS0101_overview.md) mục 3, 7 | [TC-INS0101-AC-SC-01-01](../../testcases/screens/teacher/teacher_screens_phase4.md) | Chưa thực hiện |
| AC-SC-02 | [DD màn](../../../03-dd/screens/teacher/INS0101_overview.md) mục 3, 7 | [TC-INS0101-AC-SC-02-01](../../testcases/screens/teacher/teacher_screens_phase4.md) | Chưa thực hiện |
