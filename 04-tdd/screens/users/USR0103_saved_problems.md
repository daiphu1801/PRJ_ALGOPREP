# Tiêu chí nghiệm thu — Màn USR0103 — Bài đã lưu

> DD: `03-dd/screens/users/USR0103_saved_problems.md` · Cập nhật: 2026-10-26.

## 1. Phạm vi

Các tiêu chí dưới đây kiểm hành vi màn hình và phối hợp giữa module. Hợp đồng nghiệp vụ của từng module được kiểm ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### SP-AC-01 — Danh sách bài đã lưu và bộ lọc

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người học có hơn 20 bài đã lưu, gồm bài đã giải và bài đã rút. |
| **Hành động** | Mở trang, tìm kiếm, lọc, sắp xếp và đổi trang. |
| **Kết quả mong đợi** | Mỗi trang tối đa 20 bài; ghi chú luôn hiện. Bộ lọc và trang được giữ trên đường dẫn; trang vượt giới hạn được sửa về trang cuối. Khi dữ liệu tiến độ lỗi, phần trạng thái được ẩn nhưng danh sách vẫn dùng được. |
| **Nguồn** | [SoT: 03-dd/screens/users/USR0103_saved_problems.md] mục 3, 7 |

### SP-AC-02 — Sửa ghi chú bằng Lưu hoặc Hủy

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Một bài đã lưu có ghi chú; người học sửa nội dung. |
| **Hành động** | Bấm Lưu, dùng phím tắt lưu, rồi sửa tiếp và bấm Hủy hoặc Esc. |
| **Kết quả mong đợi** | Chỉ thao tác Lưu mới ghi thay đổi; Hủy và Esc bỏ bản nháp, không lưu khi rời ô. Ghi chú dài tối đa 1.000 ký tự, nội dung toàn khoảng trắng thành ghi chú rỗng. Lỗi lưu giữ nội dung đang sửa để thử lại. |
| **Nguồn** | [SoT: 03-dd/screens/users/USR0103_saved_problems.md] mục 3, 7 |

### SP-AC-03 — Bỏ lưu và Hoàn tác đúng bài

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người học có nhiều bài đã lưu ở hai trang. |
| **Hành động** | Bỏ lưu liên tiếp hai bài, hoàn tác một bài, rồi thử khi thao tác bỏ lưu lỗi. |
| **Kết quả mong đợi** | Bài biến mất ngay và số đếm được cập nhật; mỗi thông báo Hoàn tác phục hồi đúng bài. Lỗi đưa bài và số đếm về vị trí cũ. Bỏ bài cuối trang tự lùi về trang còn dữ liệu. Bài đã rút vẫn bỏ lưu được nhưng không đề nghị Hoàn tác. |
| **Nguồn** | [SoT: 03-dd/screens/users/USR0103_saved_problems.md] mục 3, 7 |

### SP-AC-04 — Bài rút vẫn giữ ghi chú nhưng không mở đề

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Danh sách có một bài đã rút và ghi chú chứa ký hiệu đánh dấu hoặc mã HTML. |
| **Hành động** | Mở danh sách, sửa ghi chú và thử bấm tên bài. |
| **Kết quả mong đợi** | Bài rút không dẫn tới đề bài; ghi chú vẫn sửa được và hiển thị nguyên văn như chữ, không chạy mã hay dựng đánh dấu. |
| **Nguồn** | [SoT: 03-dd/screens/users/USR0103_saved_problems.md] mục 3, 7 |

### SP-AC-05 — Rời ghi chú đang sửa cần lựa chọn rõ

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người học đã sửa ghi chú bài đã lưu nhưng chưa bấm Lưu |
| **Hành động** | Bấm liên kết sang màn khác rồi chọn lần lượt Ở lại, Hủy thay đổi hoặc Lưu và rời |
| **Kết quả mong đợi** | Hộp thoại có ba lựa chọn rõ ràng; Ở lại trả con trỏ về ô ghi chú, Hủy thay đổi không ghi bản nháp, Lưu và rời chỉ chuyển màn sau khi lưu thành công. Lỗi lưu giữ người học và nội dung đang sửa tại màn hiện tại |
| **Nguồn** | [SoT: 03-dd/screens/users/USR0103_saved_problems.md] BR-SC-06, T-SP-22 |

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| SP-AC-01 | Mục 3, 7; điểm kiểm DD: T-SP-01–08 | [TC-USR0103-SPAC01-01](../../testcases/screens/users/USR0103_saved_problems.md) | Chưa thực hiện |
| SP-AC-02 | Mục 3, 7; điểm kiểm DD: T-SP-09–13 | [TC-USR0103-SPAC02-01](../../testcases/screens/users/USR0103_saved_problems.md) | Chưa thực hiện |
| SP-AC-03 | Mục 3, 7; điểm kiểm DD: T-SP-14–18 | [TC-USR0103-SPAC03-01](../../testcases/screens/users/USR0103_saved_problems.md) | Chưa thực hiện |
| SP-AC-04 | Mục 3, 7; điểm kiểm DD: T-SP-19–21 | [TC-USR0103-SPAC04-01](../../testcases/screens/users/USR0103_saved_problems.md) | Chưa thực hiện |
| SP-AC-05 | BR-SC-06; điểm kiểm DD: T-SP-22 | [TC-USR0103-SPAC05-01](../../testcases/screens/users/USR0103_saved_problems.md) | Chưa thực hiện |

