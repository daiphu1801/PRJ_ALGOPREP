# Phạm vi class diagram theo actor

## Nguồn hiện hành

- RD: `01-rd/req/user_stories/a1_student.md` tới `a4_system.md` và `01-rd/req/F*.md`.
- DD: `03-dd/logic/F*.md`, `03-dd/api/F*.md`; tên lớp/phương thức lấy nguyên văn nếu đã chốt.
- Code backend hiện có 9 file Java sản phẩm không tính `package-info.java`; phần nghiệp vụ F1-F6 phần lớn chưa hiện thực. Vì vậy bộ sơ đồ là **thiết kế cho coding**, không phải ảnh chụp code đang chạy. Mọi lớp đặt tên thêm được ghi `[SoT: Suy luận]` trong spec.
- Bảng truy vết: `actor_coverage.csv`, mỗi bullet `Cho/Khi/Thì` đang hoạt động có một dòng. Mã F1-32 đã được bổ sung vào catalog từ RD và `US-A3-02`.

## Ngoài phạm vi

| Mục | Lý do |
| :--- | :--- |
| `US-A2-04`, `F6-11` | Bộ câu hỏi phỏng vấn riêng từng lớp đã loại bỏ. |
| `US-A3-05`, `F4-09a` tới `F4-09e` | Chấm lại hàng loạt đã loại bỏ. |
| `F4-04` | Fail-fast đã thay bằng chấm hết testcase và điểm tỷ lệ. |
| `F5-27`, `US-A2-06` | Chấm tay giảng viên đã loại bỏ; story A2-06 không được cấp. |
| `F1-33`, `F1-34` | RD hiện ghi là mã đề xuất `[SoT: Suy luận]`, chờ xác nhận; không đưa vào `covers`. Nếu thiết kế lớp xác thực email/đổi mật khẩu tạm theo DD, phải ghi nguồn và phân biệt với mã chức năng đã chốt. |

## Bốn tiêu chí không ghi mã ngay trong bullet

| Tiêu chí | Mã suy từ ngữ cảnh | Căn cứ |
| :--- | :--- | :--- |
| `US-A1-10#06` | `F1-23` | Bảo vệ màn chi tiết lớp theo quan hệ thành viên, `USR0702 REQ-06`. |
| `US-A2-07#03` | `F1-23`, `F1-25` | Mã mời không tồn tại/hết hạn trong story tạo lớp và mã mời. |
| `US-A4-03#01` | `F2-08` | Mã có trong tiêu đề story bảo vệ testcase ẩn. |
| `US-A4-05#01` | `F6-08` | Mã có trong tiêu đề story đối chiếu câu trả lời. |

Một class diagram cho thấy **lớp nào sở hữu hành vi**; điều kiện và kết quả chi tiết vẫn được kiểm chứng bằng DD/TDD. Bảng truy vết không coi việc nhắc tên một mã trong ghi chú là đã bao phủ mã đó.

## Chênh lệch nguồn cần chốt trước khi code

- `02-bd/database/F1_identity.md` mục 1.1 yêu cầu job ẩn danh chỉ xử lý tài khoản `deactivation_source = SELF`; tài khoản bị Admin khoá (`ADMIN`) không được ẩn danh. `03-dd/logic/F1_identity.md` BR-08 hiện chưa có điều kiện này và chưa xoá `oauth_identities`. Sơ đồ model F1 theo BD mới hơn; trước khi coding job cần đồng bộ DD và TDD.
- Một số tiêu chí bao gồm hành vi phía client (tự refresh sau `401`, giữ mã khi đổi mô hình nộp, xác nhận ghi đè Workspace, điều hướng giao diện). Class diagram backend thể hiện phần dịch vụ/dữ liệu hỗ trợ; hành vi giao diện phải được kiểm ở màn hình/TDD, không thể suy ra chỉ từ sơ đồ lớp.
