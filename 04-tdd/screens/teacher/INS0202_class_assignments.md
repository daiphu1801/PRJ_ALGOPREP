# TDD màn `INS0202` — Bài tập giao lớp

> DD: `03-dd/screens/teacher/INS0202_class_assignments.md` · Cập nhật: 2026-10-26.


## 1. Phạm vi

Các tiêu chí màn này kiểm hành vi quan sát được và phối hợp các module; hợp đồng từng module ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### AC-SC-01 — Bảng giao bài giữ phạm vi lọc và dữ liệu đang xem
- **Tiền điều kiện:** bài P giao cho hai lớp, một bài đã rút, có số liệu chưa tính được.
- **Hành động:** đổi tab lớp, tìm tên bài, đổi trang; mô phỏng một query lỗi.
- **Kết quả mong đợi:** F2 §50 lọc trên server, thẻ tổng không biến theo trang; cột lớp ẩn khi lọc một lớp; bài rút còn dòng và nhãn; metrics null là `-`; lỗi giữ bảng cũ và bộ lọc.

- **Nguồn**: [SoT: 03-dd/screens/teacher/INS0202_class_assignments.md] mục 3, 7.

### AC-SC-02 — Popup giao/gỡ đúng cặp lớp và kết quả một phần
- **Tiền điều kiện:** hai lớp, một bài chưa giao, một bài đã giao, một bài bị rút giữa lúc chọn và gửi.
- **Hành động:** đổi lớp trong popup, giao ba bài; ở tab Tất cả gỡ bài P khỏi một lớp rồi huỷ lần thử tiếp.
- **Kết quả mong đợi:** đổi lớp xoá selection cũ; response `succeededIds/skippedIds/failedItems` hiện đúng từng nhóm, refetch dữ liệu; gỡ yêu cầu chọn lớp, chỉ bỏ đúng cặp, bài P vẫn hiện nếu lớp còn lại giao; huỷ không gọi API.

- **Nguồn**: [SoT: 03-dd/screens/teacher/INS0202_class_assignments.md] mục 3, 7.

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| AC-SC-01 | [DD màn](../../../03-dd/screens/teacher/INS0202_class_assignments.md) mục 3, 7 | [TC-INS0202-AC-SC-01-01](../../testcases/screens/teacher/teacher_screens_phase4.md) | Chưa thực hiện |
| AC-SC-02 | [DD màn](../../../03-dd/screens/teacher/INS0202_class_assignments.md) mục 3, 7 | [TC-INS0202-AC-SC-02-01](../../testcases/screens/teacher/teacher_screens_phase4.md), [TC-INS0202-AC-SC-02-02](../../testcases/screens/teacher/teacher_screens_phase4.md), [TC-INS0202-AC-SC-02-03](../../testcases/screens/teacher/teacher_screens_phase4.md) | Chưa thực hiện |
