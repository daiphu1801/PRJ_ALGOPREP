# A4 — Hệ thống tự động

> Tách ra từ `01-rd/req/user_stories.md` ngày 2026-08-31 để dễ đọc (theo actor). Nội dung dưới đây được
> chuyển nguyên văn từ `user_stories.md` (không đổi ý nghĩa, không thêm/bớt). Bản gốc không có heading
> `## 4.` riêng cho A4 (nối tiếp ngay sau mục A3 bằng một đoạn giới thiệu) — giữ đúng cấu trúc đó, chỉ thêm
> heading `#` ở đây để file này có tiêu đề độc lập. Ký hiệu và quy ước INVEST/Given-When-Then: xem
> `01-rd/req/user_stories.md` (file mục lục).

Các mục dưới đây là hành vi **hệ thống phải tự thực hiện** mà không do ai bấm — job định kỳ và consumer lấy
việc từ hàng đợi. Giữ dạng "Cho/Khi/Thì" để nhất quán với phần trên, nhưng chủ thể là hệ thống, không phải
người.

### US-A4-01: Sinh mã bọc hàm cho bài nộp (F3)

- **Cho** một đặc tả bài toán và mã nguồn người dùng cho một trong ba ngôn ngữ, **Khi** hệ thống cần chấm
  bài nộp đó, **Thì** hệ thống sinh mã đọc dữ liệu vào, gọi hàm người dùng, và in kết quả từ đúng một đặc tả
  (F3-01 tới F3-04), tiêm mã người dùng vào vùng chèn của mã khung (F3-05), và đóng gói theo đúng định dạng
  mà adapter engine đang dùng yêu cầu — không hardcode Base64, vì đó là yêu cầu riêng của Judge0, không phải
  yêu cầu chung của mọi engine (F3-06, `DEC-2026-0823-go-judge-default-engine`).
- **Cho** trình biên dịch báo lỗi ở mã đã ghép, **Khi** hệ thống trả lỗi về cho người học, **Thì** số dòng
  lỗi được ánh xạ về đúng dòng trong mã người dùng, và lỗi thuộc phần mã harness bị che giấu hoàn toàn
  (F3-11, F3-12).

### US-A4-02: Điều phối bài nộp qua judge engine (F4)

- **Cho** một bài nộp mới, **Khi** hệ thống tiếp nhận, **Thì** hệ thống ghi trạng thái `PENDING`, đẩy việc
  vào hàng đợi RabbitMQ, và phản hồi ngay cho người dùng — không chờ kết quả chấm (F4-01).
- **Cho** một testcase cần chấm, **Khi** worker lấy việc khỏi hàng đợi, **Thì** hệ thống gọi
  `JudgeExecutionPort` **một lần cho mỗi testcase** (không dồn batch), qua adapter mặc định là go-judge,
  đồng bộ — nhận kết quả ngay trong lời gọi (F4-03, `DEC-2026-0823-go-judge-default-engine`).
- **Cho** một testcase sai hoặc lỗi, **Khi** hệ thống ghi nhận, **Thì** hệ thống vẫn chạy TIẾP các testcase
  còn lại của bài nộp đó — **không** dừng sớm (`DEC-2026-0831-partial-score-testcase-ratio` khai tử fail-fast
  `F4-04`; điểm tỷ lệ `F4-13` cần biết số testcase đạt trên tổng số).
- **Cho** một kết quả testcase vừa nhận, **Khi** hệ thống ghi nhận, **Thì** hệ thống đẩy trạng thái đó qua
  WebSocket theo kênh riêng của bài nộp ngay sau khi nhận, và **không bao giờ ghi đè một trạng thái đã là
  trạng thái cuối** (F4-08, bất biến giữ nguyên bất kể adapter đồng bộ hay bất đồng bộ).
- **Cho** một message bị xử lý lại do RabbitMQ redeliver (worker crash giữa lúc xử lý), **Khi** hệ thống xử
  lý lại message đó, **Thì** kết quả cuối không đổi so với nếu chỉ xử lý một lần (idempotency theo bất biến
  trạng thái cuối, không phải theo token — token chỉ cần nếu một adapter bất đồng bộ như Judge0 được cắm lại
  sau này, F4-05/F4-06).
- **Cho** một bài nộp treo ở trạng thái trung gian quá ngưỡng thời gian, **Khi** timeout sweep định kỳ chạy,
  **Thì** hệ thống phát hiện và đánh dấu để quản trị viên xử lý (F4-07) — bản nhẹ, dựa vào ack/nack và
  redelivery của RabbitMQ, không cần chủ động hỏi lại một hệ ngoài như khi dùng Judge0.

### US-A4-03: Bảo vệ testcase ẩn (F2-08)

- **Cho** một testcase ẩn được dùng để chấm, **Khi** hệ thống trả kết quả qua bất kỳ API hoặc ghi log,
  **Thì** chỉ trạng thái và chỉ số của testcase được lộ ra — không bao giờ lộ dữ liệu vào hoặc diff chi
  tiết.

### US-A4-04: Sinh phản hồi AI và điều phối phiên phỏng vấn (F5)

- **Cho** một bài nộp đã `Accepted` và người dùng yêu cầu phân tích, **Khi** hệ thống xử lý, **Thì** hệ
  thống kiểm cache theo hash mã nguồn trước (F5-20); nếu chưa có, gọi AI, nhận JSON theo lược đồ, và lưu kèm
  bài nộp (F5-01 tới F5-08).
- **Cho** một phiên phỏng vấn giả lập đang mở, **Khi** hệ thống điều phối ba giai đoạn, **Thì** hệ thống giữ
  ngữ cảnh phiên qua `ChatMemory` trên Redis xuyên cả ba giai đoạn (F5-13) và stream phản hồi AI qua SSE
  (F5-14).
- **Cho** mọi lời gọi AI ở F5 hoặc F6, **Khi** hệ thống dựng prompt, **Thì** mã nguồn và câu trả lời của
  người dùng luôn được đưa vào dưới dạng **tham số dữ liệu**, tách hoàn toàn khỏi chỉ thị hệ thống — không
  bao giờ ghép trực tiếp vào chuỗi chỉ thị (F5-17, chống prompt injection).
- **Cho** phân hệ AI gặp sự cố hoặc hết quota, **Khi** hệ thống phát hiện, **Thì** F1-F4 vẫn tiếp tục hoạt
  động bình thường — không có lời gọi AI nào nằm trong luồng ghi nhận kết quả chấm (F5-22).

### US-A4-05: Đối chiếu câu trả lời ở ngân hàng câu hỏi (F6-08)

- **Cho** người dùng gửi câu trả lời ở Chế độ luyện, **Khi** hệ thống xử lý, **Thì** hệ thống gọi qua phân
  hệ AI (không tự gọi LLM riêng) để đối chiếu với tiêu chí chuẩn của câu hỏi, và trả về điểm đã đạt, điểm
  còn thiếu, hướng bổ sung — chịu chung ràng buộc chống prompt injection và suy giảm có kiểm soát như F5.
