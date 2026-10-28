# Audit bộ chọn smoke/E2E — 2026-10-28

Phạm vi: [8 smoke và 3 hành trình](PHASE5_SMOKE_E2E_SELECTION.md), không tạo thêm ID testcase và không thực thi ứng dụng.

| Phát hiện | Nguồn / sửa đổi |
| :--- | :--- |
| PH5-ECHO đã xuất bản chỉ có 3 testcase, trái ngưỡng tối thiểu 8 | [DD F2 BR-07](../../03-dd/logic/F2_problem-bank.md): đổi fixture thành 2 Sample + 6 Hidden, bổ sung 2 worked examples và đủ checklist xuất bản; đáp án mẫu là chương trình stdin/stdout theo BR-15. |
| Link ca nguồn F3/F4 dùng fixture two_sum/12 testcase trong khi hành trình dùng echo/3 testcase | Smoke giữ nguyên fixture từng ca nguồn; E2E ghi rõ ánh xạ sang echo/8 testcase, giữ oracle nghiệp vụ và thay số lượng/chữ ký theo fixture tích hợp. |
| F5 bị gọi là tùy chọn dù có trong cổng smoke 8 ca | Full smoke cần dịch vụ F5 với provider stub. Thiếu F5 thì SMK-P0-08 blocked, chưa đủ cổng 8 ca. |
| Learner cần assignment nhưng instructor bắt đầu khi chưa giao bài; cleanup learner xóa submission mà instructor cần | Khai thứ tự I bước 1–3 → L → I bước 4–6; giữ submission chung đến hết hai hành trình. Chạy độc lập phải seed/reset riêng. |
| Cleanup dự kiến xóa run records, làm mất bằng chứng | Reset dữ liệu ứng dụng; lưu QA run records/evidence đã che bên ngoài DB reset. |
| Tự giả định product có tenant | Dùng deployment/database test cô lập, không phụ thuộc một cơ chế tenant chưa có hợp đồng. |
| Chỉ dò 1 marker sau khi tăng fixture Hidden | Dò cả 6 marker trên API/UI/realtime; kiểm 8 dòng kết quả và sự kiện nghiệp vụ không lặp. |
| C2 vừa ngoài quyền I1 vừa dùng để tiêm lỗi Outbox; không có sibling để kiểm partial commit | C2 thuộc I2 làm đối chứng từ chối. C3 thuộc I1, batch P3 lỗi/P4 thành công kiểm đúng HTTP 200/PRB-056 và retry riêng P3. |
| Actor ADMIN chưa gắn rõ role khi thu hồi quyền | A1 thuộc PRIMARY giữ quyền; A2 thuộc SUPPORT mất `PERMISSION_MATRIX:UPDATE`, bị `403 IDT-445`; A1 khôi phục qua API và kiểm audit/version. |

Các ID nguồn trong bộ chọn được kiểm tồn tại trong file đích. Đây là audit tài liệu bộ chọn, chưa chứng minh seed thực tế, khả năng fault injection hoặc chạy E2E. Các cổng môi trường/build/golden tiếp tục nằm ở [checklist](CHECKLIST.md).

## Cải tiến skill từ phát hiện thực tế

[Skill audit](../../.agents/skills/algoprep-qa-audit/SKILL.md) được bổ sung kiểm fixture chung theo bất biến của từng module, phân biệt fixture ca nguồn với fixture E2E, dependency dịch vụ và thứ tự setup/cleanup. Audit màn hình đồng thời phát hiện có ca đủ cấu trúc nhưng expected lệch nội dung AC; bước review nay bắt đầu bằng từng mệnh đề AC trước khi mở rộng sang nhánh DD. Không sửa checker để suy diễn correctness từ cấu trúc.

Lần quét toàn corpus còn phát hiện checker cũ bỏ qua ID chữ thường (`AC-18b`, `AC-20b`), link có anchor, nhãn gộp nhiều ID và dải `01–03`/`01 đến -03`. Đã sửa parser, thêm kiểm chiều ngược TDD → ID thực trong file đã chọn; bỏ false positive của cụm phủ định `No open oracle`. Các probe tạm xác nhận dạng link/ID hợp lệ được đọc, duplicate và Expected rỗng bị từ chối. Skill thêm bước tính lại oracle số/ngày sau khi audit F5 phát hiện dự báo ngày sai và F6 phân trang 12 dòng với page size 10.
