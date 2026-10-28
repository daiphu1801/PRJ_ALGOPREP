# TDD màn `INS0206` — Quản lý bài nộp

> DD: `03-dd/screens/teacher/INS0206_submission_management.md` · Cập nhật: 2026-10-26.


## 1. Phạm vi

Các tiêu chí màn này kiểm hành vi quan sát được và phối hợp các module; hợp đồng từng module ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### AC-SC-01 — URL quyết định bộ lọc và phân trang

- **Tiền điều kiện:** giảng viên có hai lớp và nhiều bài nộp; URL có `classId`, `result`, `q`, `page`.
- **Hành động:** mở URL, đổi lần lượt từ khoá, lớp, kết quả và trang; dùng Back/Forward; bấm “Xem tất cả bài nộp”.
- **Kết quả mong đợi:** F4 #10 luôn nhận đúng bốn tham số đã chuẩn hoá; đổi lọc đưa trang về 1; Back/Forward tái hiện bảng; “Xem tất cả” bỏ riêng `classId` và `page`, giữ `q/result`.

- **Nguồn**: [SoT: 03-dd/screens/teacher/INS0206_submission_management.md] mục 3, 7.

### AC-SC-02 — Trạng thái rỗng và lỗi không trộn nhau

- **Tiền điều kiện:** lần lượt không có lớp, có lớp chưa có bài nộp, có bài nhưng lọc không khớp, F4 lỗi.
- **Hành động:** mở danh sách và bấm Thử lại sau lỗi.
- **Kết quả mong đợi:** ba thông điệp rỗng khác nhau; F4 lỗi giữ thanh lọc và URL, retry chỉ toast khi lỗi lặp; bài chưa chấm hoặc tổng testcase bằng 0 hiện `-`.

- **Nguồn**: [SoT: 03-dd/screens/teacher/INS0206_submission_management.md] mục 3, 7.

### AC-SC-03 — Điều hướng theo ID dòng

- **Tiền điều kiện:** một dòng có `classId`, `studentId`, `submissionId` thật.
- **Hành động:** mở hồ sơ học viên và kết quả bài nộp từ dòng.
- **Kết quả mong đợi:** route nhận ID đúng dòng; trang kết quả gác quyền bằng `submissionId`, không tin query truyền theo link; quay lại giữ lọc URL.

- **Nguồn**: [SoT: 03-dd/screens/teacher/INS0206_submission_management.md] mục 3, 7.

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| AC-SC-01 | [DD màn](../../../03-dd/screens/teacher/INS0206_submission_management.md) mục 3, 7 | [TC-INS0206-AC-SC-01-01](../../testcases/screens/teacher/teacher_screens_phase4.md) | Chưa thực hiện |
| AC-SC-02 | [DD màn](../../../03-dd/screens/teacher/INS0206_submission_management.md) mục 3, 7 | [TC-INS0206-AC-SC-02-01](../../testcases/screens/teacher/teacher_screens_phase4.md), [TC-INS0206-AC-SC-02-02](../../testcases/screens/teacher/teacher_screens_phase4.md) | Chưa thực hiện |
| AC-SC-03 | [DD màn](../../../03-dd/screens/teacher/INS0206_submission_management.md) mục 3, 7 | [TC-INS0206-AC-SC-03-01](../../testcases/screens/teacher/teacher_screens_phase4.md) | Chưa thực hiện |
