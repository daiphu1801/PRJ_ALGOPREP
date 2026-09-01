# A3 — Quản trị viên

> Tách ra từ `01-rd/req/user_stories.md` ngày 2026-08-31 để dễ đọc (theo actor). Nội dung dưới đây được
> chuyển nguyên văn từ `user_stories.md` (không đổi ý nghĩa, không thêm/bớt), chỉ hạ heading `##` xuống `#`
> và `###` giữ nguyên cho từng story. Ký hiệu và quy ước INVEST/Given-When-Then: xem
> `01-rd/req/user_stories.md` (file mục lục).

### US-A3-01: Quản lý ma trận phân quyền

Là **quản trị viên**, tôi muốn cấu hình ma trận phân quyền theo vai trò và chức năng, để kiểm soát ai được
làm gì trong khu vực quản trị mà không phải sửa mã.

- **Cho** tôi mở màn ma trận phân quyền, **Khi** tôi tích/bỏ tích một ô `CREATE`/`READ`/`UPDATE`/`DELETE`
  cho một cặp (vai trò, chức năng), **Thì** quyền đó có hiệu lực ngay, không cần khởi động lại dịch vụ
  (F1-10).
- **Cho** danh sách Function/Action là dữ liệu seed cố định, **Khi** tôi mở màn ma trận, **Thì** tôi thấy
  chúng ở dạng chỉ đọc; tôi tạo/sửa/xoá được Role, nhưng **không xoá được** vai trò hệ thống
  (`STUDENT`/`INSTRUCTOR`/`ADMIN`) hoặc vai trò đang có người dùng (F1-11).
- **Cho** tôi đổi một ô trong ma trận, **Khi** thay đổi được lưu, **Thì** hệ thống ghi vào Nhật ký hệ thống
  ai đổi, đổi gì, đổi lúc nào — không có ngoại lệ (F1-14).

### US-A3-02: Quản lý tài khoản người dùng

Là **quản trị viên**, tôi muốn quản lý tài khoản người dùng, để xử lý khi có sự cố tài khoản (quên mật khẩu,
vi phạm quy định...).

- **Cho** một tài khoản cần xử lý, **Khi** tôi đổi vai trò, khoá/mở khoá, hoặc reset mật khẩu tài khoản đó,
  **Thì** thay đổi có hiệu lực ngay và được ghi vào Nhật ký hệ thống (F1-13, F1-14) — hành động này bị gác
  bởi quyền `USER_MANAGEMENT` trong ma trận F1-10.

### US-A3-03: Giám sát cụm judge engine và hàng đợi

Là **quản trị viên**, tôi muốn giám sát hàng đợi bài nộp và tình trạng cụm judge engine, để biết hệ thống có
đang khoẻ không và xử lý kịp khi có bài nộp treo.

- **Cho** tôi mở bảng giám sát, **Khi** tôi xem, **Thì** tôi thấy độ dài hàng đợi, số bài nộp đang chạy, và
  tình trạng cụm judge engine (mặc định go-judge) (F4-10).
- **Cho** một bài nộp bị treo quá ngưỡng thời gian, **Khi** timeout sweep phát hiện, **Thì** tôi thấy nó
  trên bảng giám sát (F4-07).

### US-A3-04: Cấu hình ngôn ngữ, giới hạn tài nguyên, và AI

Là **quản trị viên**, tôi muốn cấu hình ngôn ngữ, giới hạn tài nguyên mặc định, và prompt/rubric của AI, để
vận hành hệ thống đúng với năng lực hạ tầng thật.

- **Cho** tôi mở cấu hình hệ thống, **Khi** tôi đổi cấu hình ngôn ngữ hoặc giới hạn tài nguyên mặc định,
  **Thì** thay đổi áp dụng cho các bài toán mới hoặc theo phạm vi tôi chọn (F4-11).
- **Cho** tôi mở cấu hình AI, **Khi** tôi sửa prompt hoặc trọng số rubric, **Thì** một phiên bản mới được
  lưu lại (kèm lịch sử các phiên bản trước) và dùng cho các yêu cầu AI tiếp theo — tôi xem lại hoặc khôi
  phục được một phiên bản cũ nếu bản mới có vấn đề (F5-23).
- **Cho** tôi muốn kiểm soát chi phí AI, **Khi** tôi xem trang theo dõi token, **Thì** tôi thấy lượng token
  đã tiêu thụ, ngân sách còn lại, dự báo thời điểm cạn quota, và bảng xếp hạng người dùng/bài toán tốn token
  nhiều nhất, để biết chỗ cần điều chỉnh giới hạn tần suất gọi (F5-19, F5-21, F5-25).
- **Cho** ngân sách token đã cạn, **Khi** một `STUDENT` hoặc `INSTRUCTOR` gọi một tính năng AI, **Thì** lời
  gọi bị tạm khoá và họ nhận thông báo rõ ràng; **Cho** tôi là `ADMIN`, **Khi** ngân sách cạn, **Thì** tôi
  không bị khoá và là người duy nhất mở lại hoặc tăng ngân sách (F5-25).

### ~~US-A3-05: Vận hành phiên chấm lại quy mô lớn~~ — ĐÃ LOẠI BỎ KHỎI PHẠM VI

**Loại bỏ 2026-08-28** (qua hỏi trực tiếp chủ dự án) — xem `DEC-2026-0828-remove-rejudge-scope`. Story này
mô tả vận hành một phiên chấm lại quy mô toàn hệ thống (F4-09a→e); giữ tiêu đề gạch ngang để lưu vết, không
còn là yêu cầu. Nếu testcase cần sửa sau khi đã có người nộp, học viên báo cho giảng viên, giảng viên tự
sửa (F2-09); không có luồng chấm lại hàng loạt các lượt nộp cũ.
