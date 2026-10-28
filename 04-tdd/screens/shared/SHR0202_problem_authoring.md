# Tiêu chí nghiệm thu — Màn SHR0202 — Biên soạn bài toán

> DD: `03-dd/screens/shared/SHR0202_problem_authoring.md` · Cập nhật: 2026-10-26.

## 1. Phạm vi

Các tiêu chí này kiểm hành vi màn hình và phối hợp giữa module. Hợp đồng máy chủ tương ứng nằm ở `04-tdd/modules/`.

## 2. Danh sách tiêu chí

### PA-AC-01 — Bản nháp và xem trước không lưu ngầm

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người soạn tạo, sửa hoặc nhân bản bài. |
| **Hành động** | Đổi tên, đặc tả hàm, ví dụ; xem trước chữ ký từng ngôn ngữ và đổi tab. |
| **Kết quả mong đợi** | Nhân bản chưa tạo bản ghi. Xem trước phản ánh bản đang sửa cho Java, C++ và Python mà không lưu ngầm; đổi tab ngôn ngữ không gọi lại, phản hồi xem trước cũ không ghi đè bản nháp mới. Khi chữ ký không hỗ trợ hoặc nhập sai, nêu đúng lý do/trường sai và không tự dựng chữ ký giả. Ví dụ thiếu ngưỡng tối thiểu được báo trước khi xuất bản. |
| **Nguồn** | [SoT: 03-dd/screens/shared/SHR0202_problem_authoring.md] mục 3, 7 |

### PA-AC-02 — Testcase và đáp án mẫu chỉ đạt khi chạy thật

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Bài có testcase, tệp tải lên và đáp án mẫu. |
| **Hành động** | Thêm, sửa, sắp, xoá testcase; tải lô, sinh bằng AI rồi chạy đáp án mẫu. |
| **Kết quả mong đợi** | Lỗi tải, sinh hoặc chạy không tạo dấu đạt giả hay tự duyệt testcase AI. Tệp thiếu hoặc trùng cặp đầu vào và đầu ra bị chặn trước khi xin URL; chỉ xác nhận lô khi mọi tệp thành công, lỗi tải giữ lựa chọn để thử lại. Xoá hoặc sửa làm bài đã xuất bản xuống dưới ngưỡng bị chặn và giao diện giữ dữ liệu trước thao tác. |
| **Nguồn** | [SoT: 03-dd/screens/shared/SHR0202_problem_authoring.md] mục 3, 7 |

### PA-AC-03 — Lưu từng bước và xuất bản nguyên vẹn

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Bài đang soạn có đặc tả, ngữ cảnh AI và testcase. |
| **Hành động** | Lưu từng phần, thử xuất bản khi một phần không hợp lệ, rồi rời màn khi bản nháp chưa lưu. |
| **Kết quả mong đợi** | Phần đã lưu được giữ; xuất bản lỗi vẫn để bài chưa xuất bản và chỉ ra mục cần sửa. Nút xuất bản không có đường tắt bỏ qua checklist và luôn chờ xác nhận từ máy chủ. Rời màn khi còn sửa dở hỏi xác nhận; chọn Hủy rời giữ form. Thay đổi mới trong lúc chờ lưu không bị phản hồi cũ ghi đè. Màn không có ô giới hạn Stack không có nguồn dữ liệu. |
| **Nguồn** | [SoT: 03-dd/screens/shared/SHR0202_problem_authoring.md] mục 3, 7 |

### PA-AC-04 — Thao tác từng dòng testcase giữ đúng thứ tự và quyền

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Bài có testcase mẫu, ẩn và nháp AI; người soạn có thể dùng chuột hoặc bàn phím. |
| **Hành động** | Đổi Công khai/Ẩn trên dòng, duyệt nháp, kéo thả hoặc bấm Lên/Xuống, rồi thử lại khi máy chủ từ chối. |
| **Kết quả mong đợi** | Mỗi thao tác chỉ đổi dòng hoặc thứ tự liên quan sau khi lưu thành công; đổi nhãn hiển thị không làm lộ nội dung ca ẩn cho học viên. Đổi Sample làm bài đang xuất bản thiếu hai Sample giữ toggle cũ và báo lý do. Duyệt nháp làm mới phiên bản bộ chấm. Sắp xếp thực sự trên bài `PUBLISHED` nhận phiên bản mới, màn hiển thị thứ tự đã lưu; sắp xếp thất bại trả về đúng thứ tự máy chủ. Nút bàn phím cho cùng kết quả như kéo thả. |
| **Nguồn** | [SoT: 03-dd/screens/shared/SHR0202_problem_authoring.md] BR-SC-02, mục 6, 7 |

### PA-AC-05 — Đặc tả và ngữ cảnh AI chỉ lưu khi hợp lệ

| Mục | Nội dung |
| :--- | :--- |
| **Tiền đề** | Người soạn mở tab đặc tả của bài đã xuất bản và sửa chữ ký hàm, chiến lược so khớp, ngữ cảnh AI. |
| **Hành động** | Nhập epsilon khi không dùng chiến lược sai số, sửa đặc tả hợp lệ, rồi thử lưu khi máy chủ báo xung đột phiên bản. |
| **Kết quả mong đợi** | Epsilon chỉ được chấp nhận với chiến lược sai số. Giá trị không hợp lệ chỉ rõ trường cần sửa và không ghi nửa chừng. Lưu hợp lệ làm mới thông tin phiên bản từ phản hồi; xung đột phiên bản không ghi đè thay đổi của người khác và giữ bản đang soạn để xử lý. Ngữ cảnh AI không xuất hiện trong nội dung công khai cho học viên. |
| **Nguồn** | [SoT: 03-dd/screens/shared/SHR0202_problem_authoring.md] mục 3–7; [SoT: 03-dd/api/F2_problem-bank.md] mục 38 |

## 3. Truy vết

| AC | Luồng DD | Test case | Trạng thái |
| :--- | :--- | :--- | :--- |
| PA-AC-01 | Mục 3, 7; điểm kiểm DD: PA-01–02, PA-11 | [TC-SHR0202-PAAC01-01](../../testcases/screens/shared/SHR0202_problem_authoring.md), [TC-SHR0202-PAAC01-02](../../testcases/screens/shared/SHR0202_problem_authoring.md) | Chưa thực hiện |
| PA-AC-02 | Mục 3, 7; điểm kiểm DD: PA-03–04, PA-09–10 | [TC-SHR0202-PAAC02-01](../../testcases/screens/shared/SHR0202_problem_authoring.md), [TC-SHR0202-PAAC02-02](../../testcases/screens/shared/SHR0202_problem_authoring.md), [TC-SHR0202-PAAC02-03](../../testcases/screens/shared/SHR0202_problem_authoring.md) | Chưa thực hiện |
| PA-AC-03 | Mục 3, 7; điểm kiểm DD: PA-06–08 | [TC-SHR0202-PAAC03-01](../../testcases/screens/shared/SHR0202_problem_authoring.md), [TC-SHR0202-PAAC03-02](../../testcases/screens/shared/SHR0202_problem_authoring.md), [TC-SHR0202-PAAC03-03](../../testcases/screens/shared/SHR0202_problem_authoring.md) | Chưa thực hiện |
| PA-AC-04 | BR-SC-02, mục 6–7; điểm kiểm DD: PA-04 | [TC-SHR0202-PAAC04-01](../../testcases/screens/shared/SHR0202_problem_authoring.md) | Chưa thực hiện |
| PA-AC-05 | Mục 3–7; điểm kiểm DD: PA-05 | [TC-SHR0202-PAAC05-01](../../testcases/screens/shared/SHR0202_problem_authoring.md) | Chưa thực hiện |

