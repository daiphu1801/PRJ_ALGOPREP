# Chú giải chung cho sequence diagram

Các ảnh sequence trong thư mục này dùng **một chú giải chung** để giữ nội dung trên mỗi ảnh gọn và dễ đọc. Tên trong ô là lifeline của người, lớp/interface hoặc hệ ngoài; chữ nhỏ dưới tên chỉ tầng hay vai trò. Nhãn trên mũi tên diễn đạt hành động bằng tiếng Việt. Tên lifeline rút gọn có ánh xạ sang tên đầy đủ trong file `.spec.json` cạnh ảnh.

| Hình/nhãn | Ý nghĩa |
| :--- | :--- |
| Hình người | Actor khởi phát hoặc nhận kết quả. `Người soạn đề` áp cho giảng viên và quản trị viên khi có quyền. |
| Ô xanh lá `presentation` | Controller nhận yêu cầu, kiểm đầu vào/quyền, trả lời. |
| Ô vàng `application command` | Command handler điều phối thay đổi dữ liệu và giao dịch. |
| Ô vàng `application query` | Query handler đọc/ghép số liệu, không ghi trạng thái nghiệp vụ. |
| Ô hồng `domain output port` | Interface do module gọi sở hữu; `*Repository` là cổng ra, chưa phải lớp truy cập DB cụ thể. |
| Ô hồng `application read port` | Interface đường đọc, trả read model thay vì bắt buộc nạp aggregate. |
| Ô hồng có `port F1 → F2/F4` | Cổng do F1 khai để lấy dữ liệu từ module khác; adapter/bridge triển khai ở hạ tầng hoặc bootstrap. |
| Ô có `infrastructure` | Adapter hiện thực port; adapter DB gọi PostgreSQL, adapter hàng đợi gọi RabbitMQ, adapter lưu tệp gọi MinIO. |
| Ô xám `schema ...` | PostgreSQL ở schema của module ghi trên ô. |
| Ô xanh dương | Hàng đợi hoặc hệ ngoài như RabbitMQ, MinIO, dịch vụ email, go-judge. |
| Mũi tên liền đầu đặc | Lời gọi có chờ kết quả. |
| Mũi tên liền đầu rỗng | Gửi bất đồng bộ. |
| Mũi tên nét đứt | Giá trị hoặc kết quả trả về. |
| Khung `alt` | Hai nhánh loại trừ nhau theo điều kiện ghi trên khung. |
| Dải xám `Giai đoạn` | Điểm bắt đầu một chặng của cùng nghiệp vụ; các trang `1a`, `1b`, `1c` đọc liên tiếp. |

**Cách đọc dữ liệu:** Sequence cho thấy dữ liệu nào đổi quyết định hoặc trạng thái nào được ghi. Trường/cột đầy đủ nằm trong `02-bd/database/` và ERD; thành viên lớp nằm trong class diagram. Tên lớp nghiệp vụ hiện là thiết kế dự kiến cho đến khi backend được triển khai, xem ghi chú nguồn của từng spec.
