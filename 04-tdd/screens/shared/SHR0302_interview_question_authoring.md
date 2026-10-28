# Tiêu chí nghiệm thu — Màn SHR0302 — Biên soạn câu hỏi phỏng vấn

> DD: `03-dd/screens/shared/SHR0302_interview_question_authoring.md` · Cập nhật: 2026-10-26.

## 1. Phạm vi

Các tiêu chí này kiểm hành vi màn hình và phối hợp giữa module. Hợp đồng máy chủ tương ứng nằm ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### IQA-AC-01 — Tạo, sửa và xem trước bản chưa lưu

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người soạn có câu hỏi mới hoặc câu hỏi đã lưu. |
| **Hành động** | Sửa nội dung, câu đào sâu và thang chấm rồi xem trước. |
| **Kết quả mong đợi** | Xem trước phản ánh bản nháp nhưng không lưu; tạo mới thành công chuyển sang đường sửa của câu vừa tạo. Sửa câu cũ giữ nguyên mã câu đó. |
| **Nguồn** | [SoT: 03-dd/screens/shared/SHR0302_interview_question_authoring.md] mục 3, 7 |

### IQA-AC-02 — Thang chấm được kiểm trước khi lưu

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Có tiêu chí chấm rỗng hoặc tổng trọng số khác 100. |
| **Hành động** | Thử lưu từng bản. |
| **Kết quả mong đợi** | Trường hợp được phép để trống và trường hợp tổng đúng 100 được lưu; tổng sai hoặc tên tiêu chí trùng bị báo để sửa, không tạo bản lưu sai. |
| **Nguồn** | [SoT: 03-dd/screens/shared/SHR0302_interview_question_authoring.md] mục 3, 7 |

### IQA-AC-03 — Lỗi lưu, rời màn và xoá có xác nhận

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người soạn có bản nháp chưa lưu. |
| **Hành động** | Thử lưu khi máy chủ lỗi, rời màn, rồi bấm xoá và huỷ hộp thoại. |
| **Kết quả mong đợi** | Lỗi giữ bản nháp; rời màn hỏi trước khi bỏ thay đổi. Xoá cần xác nhận; huỷ không gửi thao tác xoá. |
| **Nguồn** | [SoT: 03-dd/screens/shared/SHR0302_interview_question_authoring.md] mục 3, 7 |

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| IQA-AC-01 | Mục 3, 7; điểm kiểm DD: IQ-A-01–02, IQ-A-04 | [TC-SHR0302-IQAAC01-01](../../testcases/screens/shared/SHR0302_interview_question_authoring.md), [TC-SHR0302-IQAAC01-02](../../testcases/screens/shared/SHR0302_interview_question_authoring.md) | Chưa thực hiện |
| IQA-AC-02 | Mục 3, 7; điểm kiểm DD: IQ-A-03 | [TC-SHR0302-IQAAC02-01](../../testcases/screens/shared/SHR0302_interview_question_authoring.md), [TC-SHR0302-IQAAC02-02](../../testcases/screens/shared/SHR0302_interview_question_authoring.md) | Chưa thực hiện |
| IQA-AC-03 | Mục 3, 7; điểm kiểm DD: IQ-A-05–06 | [TC-SHR0302-IQAAC03-01](../../testcases/screens/shared/SHR0302_interview_question_authoring.md), [TC-SHR0302-IQAAC03-02](../../testcases/screens/shared/SHR0302_interview_question_authoring.md) | Chưa thực hiện |

