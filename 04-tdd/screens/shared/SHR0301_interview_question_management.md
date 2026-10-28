# Tiêu chí nghiệm thu — Màn SHR0301 — Quản lý câu hỏi phỏng vấn

> DD: `03-dd/screens/shared/SHR0301_interview_question_management.md` · Cập nhật: 2026-10-26.

## 1. Phạm vi

Các tiêu chí này kiểm hành vi màn hình và phối hợp giữa module. Hợp đồng máy chủ tương ứng nằm ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### IQM-AC-01 — Danh sách câu hỏi giữ bộ lọc và chọn theo trang

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Có nhiều câu hỏi thuộc nhiều chủ đề. |
| **Hành động** | Lọc, sắp, đổi trang, chọn tất cả và mở chi tiết hoặc sửa. |
| **Kết quả mong đợi** | Bộ lọc giữ trên đường dẫn; chọn tất cả chỉ áp dụng trang đang xem. Bấm liên kết không thay đổi các dòng đã chọn; chi tiết và sửa dùng đúng câu hỏi. |
| **Nguồn** | [SoT: 03-dd/screens/shared/SHR0301_interview_question_management.md] mục 3, 7 |

### IQM-AC-02 — Xoá lô báo từng kết quả

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Có một nhóm câu hỏi, trong đó một số không thể xoá. |
| **Hành động** | Xoá một hoặc nhiều câu. |
| **Kết quả mong đợi** | Màn cho biết câu nào đã xoá và giữ mã câu thất bại để xử lý tiếp; không báo cả lô thành công khi chỉ một phần thành công. |
| **Nguồn** | [SoT: 03-dd/screens/shared/SHR0301_interview_question_management.md] mục 3, 7 |

### IQM-AC-03 — Danh mục và nhập CSV theo quyền

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người soạn và quản trị viên cùng vào màn. |
| **Hành động** | Thử sửa danh mục và nhập CSV đúng, sai tiêu đề hoặc sai dòng. |
| **Kết quả mong đợi** | Người không có quyền không sửa danh mục; người có quyền thao tác được. Mẫu CSV tải được; kết quả nhập cho biết dòng đúng, dòng lỗi và lý do do máy chủ trả. |
| **Nguồn** | [SoT: 03-dd/screens/shared/SHR0301_interview_question_management.md] mục 3, 7 |

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| IQM-AC-01 | Mục 3, 7; điểm kiểm DD: IQ-M-01–03 | [TC-SHR0301-IQMAC01-01](../../testcases/screens/shared/SHR0301_interview_question_management.md), [TC-SHR0301-IQMAC01-02](../../testcases/screens/shared/SHR0301_interview_question_management.md) | Chưa thực hiện |
| IQM-AC-02 | Mục 3, 7; điểm kiểm DD: IQ-M-04 | [TC-SHR0301-IQMAC02-01](../../testcases/screens/shared/SHR0301_interview_question_management.md) | Chưa thực hiện |
| IQM-AC-03 | Mục 3, 7; điểm kiểm DD: IQ-M-05–06 | [TC-SHR0301-IQMAC03-01](../../testcases/screens/shared/SHR0301_interview_question_management.md), [TC-SHR0301-IQMAC03-02](../../testcases/screens/shared/SHR0301_interview_question_management.md), [TC-SHR0301-IQMAC03-03](../../testcases/screens/shared/SHR0301_interview_question_management.md) | Chưa thực hiện |

