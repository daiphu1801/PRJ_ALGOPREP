# Tiêu chí nghiệm thu — Màn SHR0201 — Quản lý bài toán

> DD: `03-dd/screens/shared/SHR0201_problem_management.md` · Cập nhật: 2026-10-26.

## 1. Phạm vi

Các tiêu chí này kiểm hành vi màn hình và phối hợp giữa module. Hợp đồng máy chủ tương ứng nằm ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### PM-AC-01 — Danh sách quản lý lọc và chọn theo trang

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người soạn có nhiều bài thuộc nhiều trạng thái. |
| **Hành động** | Lọc, đổi trang, chọn tất cả rồi mở chi tiết hoặc nhân bản. |
| **Kết quả mong đợi** | Danh sách theo phạm vi người soạn; chọn tất cả chỉ chọn trang đang xem. Nhân bản mở bản nháp chưa lưu, không tạo bài ngay. Điều hướng giữ đúng mã bài. |
| **Nguồn** | [SoT: 03-dd/screens/shared/SHR0201_problem_management.md] mục 3, 7 |

### PM-AC-02 — Thao tác hàng loạt báo kết quả từng bài

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Một nhóm bài có bài hợp lệ và bài không thể thay đổi. |
| **Hành động** | Xuất bản, rút hoặc xoá theo lô. |
| **Kết quả mong đợi** | Màn báo số bài thành công và giữ danh sách mã thất bại để xử lý tiếp; một bài lỗi không làm thao tác đã thành công bị báo sai. |
| **Nguồn** | [SoT: 03-dd/screens/shared/SHR0201_problem_management.md] mục 3, 7 |

### PM-AC-03 — Danh mục và xuất dữ liệu theo quyền

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Có chủ đề hoặc mức độ khó đang được bài tham chiếu; danh sách đang lọc. |
| **Hành động** | Thử sửa danh mục, xoá mục đang dùng và xuất CSV. |
| **Kết quả mong đợi** | Người soạn không có quyền quản trị danh mục không thao tác được; mục còn tham chiếu hoặc mức cuối bị chặn xoá. CSV xuất toàn bộ tập phù hợp bộ lọc, không chỉ trang đang xem. |
| **Nguồn** | [SoT: 03-dd/screens/shared/SHR0201_problem_management.md] mục 3, 7 |

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| PM-AC-01 | Mục 3, 7; điểm kiểm DD: PM-01–02 | [TC-SHR0201-PMAC01-01](../../testcases/screens/shared/SHR0201_problem_management.md), [TC-SHR0201-PMAC01-02](../../testcases/screens/shared/SHR0201_problem_management.md) | Chưa thực hiện |
| PM-AC-02 | Mục 3, 7; điểm kiểm DD: PM-03 | [TC-SHR0201-PMAC02-01](../../testcases/screens/shared/SHR0201_problem_management.md) | Chưa thực hiện |
| PM-AC-03 | Mục 3, 7; điểm kiểm DD: PM-04–06 | [TC-SHR0201-PMAC03-01](../../testcases/screens/shared/SHR0201_problem_management.md), [TC-SHR0201-PMAC03-02](../../testcases/screens/shared/SHR0201_problem_management.md) | Chưa thực hiện |

