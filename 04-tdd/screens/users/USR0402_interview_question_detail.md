# TDD màn `USR0402` — Chi tiết câu hỏi phỏng vấn

> DD: `03-dd/screens/users/USR0402_interview_question_detail.md` · Cập nhật: 2026-10-26.

Các AC chỉ kiểm UI qua F6 và vòng đời route; quy tắc lưu attempt, ngân sách và mã lỗi thuộc `04-tdd/modules/F6_interview-bank.md`.


## 1. Phạm vi

Các tiêu chí màn này kiểm hành vi quan sát được và phối hợp các module; hợp đồng từng module ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### AC-SC-01 — Đổi câu hỏi không trộn dữ liệu và phân trang lịch sử

- **Tiền điều kiện:** hai `questionId` UUID A và B, lời gọi A trả chậm; B có hơn 10 attempt.
- **Hành động:** mở A rồi chuyển B trước khi A trả; bấm Xem thêm của B.
- **Kết quả mong đợi:** chỉ dữ liệu B hiện, trang tiếp dùng `before` từ B, không gộp attempt A hoặc lặp dòng B; nháp chưa gửi không được tự lưu thành attempt.

- **Nguồn**: [SoT: 03-dd/screens/users/USR0402_interview_question_detail.md] mục 3, 7.

### AC-SC-02 — Xác nhận gửi và đường lỗi

- **Tiền điều kiện:** người học đã nhập câu trả lời, hộp xác nhận có hai lựa chọn.
- **Hành động:** huỷ, sau đó xác nhận gửi; lặp khi F6 trả 201 FAILED hoặc 429 ngân sách khoá.
- **Kết quả mong đợi:** huỷ không gọi API; xác nhận tạo đúng một attempt; 201 FAILED vẫn hiện attempt đã lưu với lỗi feedback; 429 không thêm attempt. Focus được trả về nút mở hộp khi huỷ.

- **Nguồn**: [SoT: 03-dd/screens/users/USR0402_interview_question_detail.md] mục 3, 7.

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| AC-SC-01 | [DD màn](../../../03-dd/screens/users/USR0402_interview_question_detail.md) mục 3, 7 | [TC-USR0402-SCAC01-01](../../testcases/screens/users/USR0402_interview_question_detail.md) | Chưa thực hiện |
| AC-SC-02 | [DD màn](../../../03-dd/screens/users/USR0402_interview_question_detail.md) mục 3, 7 | [TC-USR0402-SCAC02-01](../../testcases/screens/users/USR0402_interview_question_detail.md) | Chưa thực hiện |
