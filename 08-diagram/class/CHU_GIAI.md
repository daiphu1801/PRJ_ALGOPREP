# Chú giải chung cho class diagram AlgoPrep

Đọc mỗi hình từ lớp ở trên xuống lớp bên dưới. **Hộp** là lớp hoặc giao diện; **đường nối** nói lớp này liên quan đến lớp kia như thế nào. Tên lớp và phương thức giữ theo thiết kế kỹ thuật; dòng ghi chú dưới mỗi hình giải thích riêng nội dung hình đó.

| Ký hiệu | Cách đọc |
| :--- | :--- |
| `<<interface>>` trong hộp | Giao diện: quy định các phương thức mà lớp triển khai phải cung cấp. |
| Tên lớp *in nghiêng* | Lớp trừu tượng: chưa dùng trực tiếp để tạo đối tượng. |
| `+`, `-`, `#`, `~` trước thành viên | Lần lượt: công khai, riêng tư, lớp con được dùng, cùng package được dùng. |
| Nét liền, tam giác rỗng hướng về lớp trên | Lớp dưới kế thừa lớp trên. |
| Nét đứt, tam giác rỗng hướng về giao diện | Lớp dưới hiện thực giao diện. |
| Nét đứt, mũi tên mở | Một lớp sử dụng lớp kia; nhãn trên đường nói rõ việc sử dụng. |
| Nét liền, không hình thoi | Liên kết/tham chiếu; mũi tên mở (nếu có) chỉ hướng truy cập. |
| Hình thoi rỗng | Kết tập: một đối tượng gom các đối tượng khác. |
| Hình thoi đặc | Hợp thành: đối tượng tổng thể sở hữu vòng đời phần bên trong. |
| `0..1`, `1`, `*` cạnh đầu đường | Số đối tượng có thể tham gia quan hệ: tùy chọn, đúng một, nhiều. |
| Hộp nét đứt | Lớp thuộc package khác, chỉ hiện tên để cho thấy điểm giao tiếp. |

`[SoT: Suy luận]` báo một tên hoặc chi tiết là đề xuất từ tài liệu thiết kế, chưa được chốt trong mã nguồn. Các sơ đồ thể hiện **cấu trúc lớp**, không thay thế use case diagram (ai làm gì) hoặc sequence diagram (việc xảy ra theo thứ tự nào).

## Hai hình mẫu F3

- [Sinh mã theo ngôn ngữ](class_f3_plugin_ngon_ngu.png): ba plugin Java/C++/Python hiện thực cùng giao diện; registry chọn plugin theo ngôn ngữ.
- [Chuẩn bị chương trình cho máy chấm](class_f3_dong_goi_payload.png): assembler đọc kế hoạch chạy và tạo dữ liệu gửi máy chấm; go-judge là mặc định, Judge0 là phương án thay thế.
