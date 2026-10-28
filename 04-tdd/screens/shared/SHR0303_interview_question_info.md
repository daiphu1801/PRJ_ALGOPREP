# Tiêu chí nghiệm thu — Màn SHR0303 — Thông tin câu hỏi phỏng vấn

> DD: `03-dd/screens/shared/SHR0303_interview_question_info.md` · Cập nhật: 2026-10-26.

## 1. Phạm vi

Các tiêu chí này kiểm hành vi màn hình và phối hợp giữa module. Hợp đồng máy chủ tương ứng nằm ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### IQI-AC-01 — Mở câu theo mã và trạng thái hiện hành

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Có câu đang dùng, câu đã rút, mã sai và mã không tồn tại. |
| **Hành động** | Mở từng đường chi tiết. |
| **Kết quả mong đợi** | Câu đang dùng hiển thị nội dung đã lưu; số hiển thị chỉ là nhãn, không phải mã đường dẫn. Các trường hợp sai, không tồn tại hoặc đã rút cho cùng trạng thái không tìm thấy. |
| **Nguồn** | [SoT: 03-dd/screens/shared/SHR0303_interview_question_info.md] mục 3, 7 |

### IQI-AC-02 — Nội dung và nút sửa theo quyền

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Một người có quyền đọc nhưng không có quyền sửa; câu có nội dung hoặc điểm chưa có. |
| **Hành động** | Mở trang, xem trước và thử mở Sửa. |
| **Kết quả mong đợi** | Thiếu quyền đọc không thấy nội dung; thiếu quyền sửa không có lối sửa. Trường trống và điểm chưa có hiện trạng thái rỗng đúng; thang điểm hiển thị theo phần trăm, không dùng thang năm điểm. |
| **Nguồn** | [SoT: 03-dd/screens/shared/SHR0303_interview_question_info.md] mục 3, 7 |

### IQI-AC-03 — Xem trước và lỗi danh mục độc lập

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Chi tiết câu tải được nhưng danh mục phụ tạm lỗi. |
| **Hành động** | Mở Xem trước, đóng hộp thoại, rồi thử lại nguồn lỗi. |
| **Kết quả mong đợi** | Xem trước dùng bản đã lưu; đóng trả người dùng về trang và vị trí thao tác cũ. Lỗi danh mục không che câu hỏi; có thể thử lại từng phần. |
| **Nguồn** | [SoT: 03-dd/screens/shared/SHR0303_interview_question_info.md] mục 3, 7 |

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| IQI-AC-01 | Mục 3, 7; điểm kiểm DD: IQ-01–02 | [TC-SHR0303-IQIAC01-01](../../testcases/screens/shared/SHR0303_interview_question_info.md), [TC-SHR0303-IQIAC01-02](../../testcases/screens/shared/SHR0303_interview_question_info.md) | Chưa thực hiện |
| IQI-AC-02 | Mục 3, 7; điểm kiểm DD: IQ-03–05 | [TC-SHR0303-IQIAC02-01](../../testcases/screens/shared/SHR0303_interview_question_info.md), [TC-SHR0303-IQIAC02-02](../../testcases/screens/shared/SHR0303_interview_question_info.md) | Chưa thực hiện |
| IQI-AC-03 | Mục 3, 7; điểm kiểm DD: IQ-06–07 | [TC-SHR0303-IQIAC03-01](../../testcases/screens/shared/SHR0303_interview_question_info.md), [TC-SHR0303-IQIAC03-02](../../testcases/screens/shared/SHR0303_interview_question_info.md) | Chưa thực hiện |

