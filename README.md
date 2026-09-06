# BÁO CÁO KHẢO SÁT HỆ THỐNG

## NỀN TẢNG LUYỆN TẬP THUẬT TOÁN LẬP TRÌNH VÀ ÔN TẬP PHỎNG VẤN TÍCH HỢP AI

> **Tích hợp go-judge và Spring AI**

---

## 1. HỆ THỐNG THAM KHẢO

### 1.1. Nền tảng tham khảo

| Tiêu chí                   | LeetCode                         | Codeforces                     | HackerRank           |
| :------------------------- | :------------------------------- | :----------------------------- | :------------------- |
| **Mô hình nộp bài**        | Bọc hàm (Function Wrapper)       | Nhập/Xuất chuẩn (Standard I/O) | Cả hai               |
| **Phản hồi khi giải đúng** | Percentile, gợi ý code (trả phí) | Chỉ trạng thái (Accepted)      | Rubric tĩnh          |
| **Rèn kỹ năng giải trình** | Không                            | Không                          | Theo mẫu cố định     |
| **Triển khai nội bộ**      | Không                            | Không                          | Chỉ qua hợp đồng B2B |

### 1.2. Judge mã nguồn mở

| Hệ thống           | Giải quyết được                                                                            | Còn thiếu                                                                                                                                      |
| :----------------- | :----------------------------------------------------------------------------------------- | :--------------------------------------------------------------------------------------------------------------------------------------------- |
| **isolate**        | Cô lập tiến trình cấp kernel                                                               | Chỉ là thư viện sandbox, không có tầng ứng dụng                                                                                                |
| **Judge0**         | REST API thực thi mã đa ngôn ngữ                                                           | Chỉ nhận stdin/stdout; không hỗ trợ bọc hàm, không có phản hồi định tính; đóng gói cần cgroup v1 — xem `DEC-2026-0823-go-judge-default-engine` |
| **go-judge**       | REST/gRPC thực thi mã đa ngôn ngữ, tự viết sandbox `go-sandbox` (không dựa trên `isolate`) | Chỉ nhận stdin/stdout như Judge0; không hỗ trợ bọc hàm, không có phản hồi định tính                                                            |
| **DOMjudge / CMS** | Judge cho thi đấu ICPC, IOI                                                                | Thiết kế cho thi đấu, không cho tự học                                                                                                         |
| **QDUOJ / HUSTOJ** | Judge kèm giao diện cho trường học                                                         | Chỉ stdin/stdout, không có tầng đánh giá chuyên sâu                                                                                            |

#### 1.2.1. Lý do chọn go-judge làm engine mặc định

- **Không xây lại sandbox:** Cô lập tiến trình khi chạy mã không tin cậy là bài toán cấp kernel; `go-sandbox` (lõi của go-judge) đã giải quyết, và là engine chấm đang chạy thật trong Hydro — một online judge đang hoạt động. Làm lại chỉ thêm rủi ro bảo mật mà không tạo đóng góp mới (tiết kiệm thời gian).
- **Đúng tầng cần tích hợp:** go-judge chỉ là API thực thi, không áp đặt mô hình bài toán hay quy trình nghiệp vụ — nên ba tầng đóng góp của đề tài xây trực tiếp lên trên được, khác với DOMjudge hay HUSTOJ vốn trọn gói.
- **Không kén cgroup version:** go-judge chạy được cả cgroup v1 và v2 (Judge0 chỉ chạy được v1) — đóng rủi ro triển khai trên Windows/WSL2 mà không cần ép tham số kernel. Chi tiết: `01-rd/system/judge_engine.md`.
- **Stateless, hạ tầng nhẹ:** Không cần Postgres/Redis riêng cho engine như Judge0 — submission vẫn lưu ở database của AlgoPrep.
- **Hỗ trợ đóng gói đa lệnh trong một request:** `cmd: Cmd[]` → `[]Result`, có `pipeMapping`/`copyOutCached` để compile rồi run trong một lần gọi, đúng với kiến trúc hàng đợi và phản hồi thời gian thực ở F4.
- **Đóng gói sẵn, self-host được:** Ba ngôn ngữ trong phạm vi (Java, C++, Python) tự cấu hình compiler/runtime trong image chạy go-judge; mã nguồn mở, chạy nội bộ bằng Docker Compose.
- **Giữ khả năng đổi lại Judge0:** Cổng ra ở tầng `application` của F4 được thiết kế trung lập theo engine (`JudgeExecutionPort`, xem `DEC-2026-0823-go-judge-default-engine`) — nếu sau này cần đúng chuẩn thi đấu ICPC/Olympic mà Judge0 có nhiều tiền lệ hơn, chuyển lại chỉ là viết thêm một adapter, không đổi domain/application.

### 1.3. Khoảng trống và định vị

- Không có hệ thống mã nguồn mở nào hỗ trợ mô hình bọc hàm — hình thức được dùng trong phỏng vấn thực tế.
- Không có hệ thống nào cung cấp phản hồi định tính sau khi bài nộp được chấp nhận.
- Không có môi trường luyện kỹ năng giải trình, phản biện giải thuật, triển khai được nội bộ.
- **Định vị đề tài:** Đề tài không xây lại bộ máy chấm bài — bài toán cô lập tiến trình đã được engine chấm (go-judge) giải quyết. Đóng góp nằm ở ba tầng phía trên: **mô hình bọc hàm đa ngôn ngữ**, **phản hồi thời gian thực**, và **tầng đánh giá bằng AI**.

---

## 2. TỔNG QUAN ĐỀ TÀI

Hệ thống là nền tảng web giúp người học luyện thuật toán và rèn kỹ năng phỏng vấn kỹ thuật. Người dùng chọn bài toán, viết code trên trình soạn thảo trực tuyến theo hai hình thức — **viết hàm giải thuật** hoặc **đọc/ghi dữ liệu chuẩn** — bài nộp được thực thi trong môi trường cô lập và kết quả từng testcase hiện về theo thời gian thực.

Điểm khác biệt nằm ở phần sau khi bài nộp đạt **Accepted**. Thay vì kết thúc ở đúng/sai, hệ thống mở ra hai hướng:

1. **Phân tích bài giải (Solution Review):** Báo cáo một lượt chỉ ra độ phức tạp thực tế của mã đã nộp, bài giải đã tối ưu chưa, còn lỗ hổng hay trường hợp biên nào chưa xử lý.
2. **Phỏng vấn giả lập 1:1 (Mock Interview):** Phiên hội thoại nhiều lượt với AI đóng vai kỹ sư phỏng vấn, người học bảo vệ chính bài giải của mình qua ba giai đoạn (giải trình, phản biện và mở rộng quy mô), kết thúc bằng bảng đánh giá năng lực theo rubric.

Ngoài luồng gắn với bài nộp, hệ thống có thêm ngân hàng câu hỏi phỏng vấn dạng lý thuyết để người dùng ôn tập và luyện trả lời độc lập.

Hệ thống tích hợp go-judge self-hosted làm tầng thực thi (mặc định, có thể đổi adapter — `DEC-2026-0823-go-judge-default-engine`) và tự xây ba phần: bộ sinh mã bọc hàm, tầng điều phối chấm bài, và phân hệ AI. Toàn bộ hệ thống là mã nguồn mở, tự triển khai được bằng Docker Compose.

---

## 3. ĐỐI TƯỢNG CỦA HỆ THỐNG

| Actor  | Tên đối tượng                   | Vai trò chính                                                                                                                                             |
| :----- | :------------------------------ | :-------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **A1** | Sinh viên / Người dùng cuối     | Tìm và giải bài toán, chạy thử và nộp bài, xem kết quả từng testcase, chọn Study Pack hoặc Mock Interview sau khi đạt Accepted, theo dõi tiến độ cá nhân. |
| **A2** | Giảng viên (hoặc Quản trị viên) | Soạn đề bài và đặc tả hàm, tải lên bộ testcase, quản lý câu hỏi và giao bài tập theo lớp.                                                                 |
| **A3** | Quản trị viên                   | Giám sát cụm judge engine (go-judge) và hàng đợi, kích hoạt Re-judge, quản lý ngôn ngữ và giới hạn tài nguyên, cấu hình prompt và rubric AI.              |
| **A4** | Hệ thống tự động                | Điều phối bài nộp sang judge engine, phát hiện bài nộp bị treo (timeout sweep), sinh Study Pack và điều phối phiên phỏng vấn.                             |

---

## 4. PHÂN HỆ CHỨC NĂNG (Dự tính theo kế hoạch)

### F1 — Danh tính và phân quyền

- **Xác thực JWT:** Access Token thời hạn ngắn, Refresh Token trong HTTP-Only Cookie.
- **Phân quyền theo vai trò:** `STUDENT`, `INSTRUCTOR`, `ADMIN`.
- **Trang tiến độ cá nhân:** Bài đã giải theo chủ đề, tỉ lệ chấp thuận (acceptance rate), lịch sử phỏng vấn.

### F2 — Ngân hàng bài toán và test case

- **Soạn đề bài:** Hỗ trợ Markdown và công thức LaTeX; phân loại độ khó và chủ đề.
- **Khai báo đặc tả bài toán:** Signature hàm theo ngôn ngữ, kiểu tham số và giá trị trả về, chiến lược so khớp kết quả.
- **Hai loại testcase:**
  - _Sample:_ Công khai, dành cho chế độ "Run Code".
  - _Hidden:_ Ẩn, dành cho chế độ "Submit".
- **Chống rò rỉ testcase ẩn:** Chỉ hiển thị trạng thái và chỉ số testcase sai, không hiển thị input hay diff chi tiết.
- **Phiên bản hóa bộ testcase:** Để **truy vết** một lượt nộp cũ đã được chấm theo phiên bản nào. _(Sửa 2026-09-05: câu gốc ghi "hỗ trợ cơ chế Re-judge biết chấm lại theo phiên bản nào" — cơ chế chấm lại đã loại khỏi phạm vi, `DEC-2026-0828-remove-rejudge-scope`; phiên bản nay chỉ dùng truy vết.)_
- **Giới hạn tài nguyên:** Giới hạn thời gian (Time Limit) và bộ nhớ (Memory Limit) theo bài, có hệ số nhân theo từng ngôn ngữ.

### F3 — Bộ sinh mã bọc hàm (Test Harness Generator)

_Phân hệ cho phép mô hình bọc hàm hoạt động trên judge engine (go-judge) — vốn chỉ nhận stdin/stdout, y như Judge0. Đây là trọng tâm kỹ thuật của đề tài._

- **Lược đồ đặc tả kiểu dữ liệu độc lập ngôn ngữ:** Kiểu nguyên thủy, chuỗi, mảng nhiều chiều, danh sách lồng nhau, danh sách liên kết, cây nhị phân.
- **Sinh mã đa ngôn ngữ:** Từ một đặc tả duy nhất, sinh mã đọc dữ liệu, gọi hàm người dùng và in kết quả cho cả **Java, C++, Python**.
- **Tiêm mã và đóng gói:** Tiêm mã người dùng vào template bài toán, đóng gói theo đúng định dạng adapter đang dùng yêu cầu (không hardcode Base64 — đó là yêu cầu riêng của Judge0, không phải yêu cầu chung của mọi engine; `DEC-2026-0823-go-judge-default-engine`).
- **Chiến lược so khớp linh hoạt:** Chính xác (exact), chuẩn hóa khoảng trắng, số thực theo sai số epsilon, tập hợp không thứ tự.
- **Ánh xạ lỗi biên dịch (Error Mapping):** Ánh xạ lỗi biên dịch về đúng dòng trong mã người dùng, che giấu và không hiển thị lỗi thuộc phần mã harness.

### F4 — Điều phối và giao tiếp judge engine

_Gọi qua một cổng ra trung lập theo engine (`JudgeExecutionPort`), adapter mặc định là go-judge; xem `DEC-2026-0823-go-judge-default-engine`._

- **Tiếp nhận bài nộp:** Ghi trạng thái `PENDING`, đẩy vào hàng đợi RabbitMQ, phản hồi ngay lập tức cho người dùng.
- **Gửi từng testcase, chạy hết N testcase:** Gọi cổng ra một lần cho mỗi testcase (không dồn batch) và **chạy hết toàn bộ testcase của bài nộp**, để luôn biết tỷ lệ pass. _(Sửa 2026-09-05: câu gốc mô tả cơ chế **fail-fast** — bỏ các testcase còn lại khi một testcase sai. Cơ chế đó đã hết hiệu lực theo `DEC-2026-0831-partial-score-testcase-ratio`: điểm tỷ lệ testcase cần chạy đủ N testcase mới tính được, đổi lại tải máy chủ và thời gian chờ cao hơn.)_
- **Xác thực webhook và chống callback trùng — chỉ khi dùng adapter bất đồng bộ:** Với adapter mặc định (go-judge, gọi đồng bộ), kết quả trả về ngay trong lời gọi, không có khái niệm callback. Nếu một adapter bất đồng bộ (ví dụ Judge0) được cắm vào sau này, xác thực webhook bằng token bí mật và chống trùng theo token là chi tiết **nội bộ của adapter đó**, domain không cần biết.
- **Timeout sweep (bản nhẹ của job đối soát):** Quét các bài nộp bị treo quá ngưỡng thời gian — với mô hình gọi đồng bộ qua RabbitMQ, nguyên nhân chủ yếu là worker crash giữa lúc đang gọi engine, xử lý bằng ack/nack và redelivery có sẵn của RabbitMQ, không cần chủ động hỏi lại trạng thái một hệ ngoài như khi dùng Judge0.
- **Cập nhật thời gian thực:** Đẩy trạng thái từng testcase qua WebSocket theo kênh riêng (topic) của từng bài nộp, ngay sau mỗi lần gọi cổng ra trả về kết quả.

### F5 — Phân hệ AI

_Phân hệ gồm hai chức năng độc lập, người dùng chủ động lựa chọn. **F5.1** kích hoạt sau khi bài nộp đạt `Accepted`. **F5.2** mở được theo ba lối vào: từ một bài nộp `Accepted`, từ kho câu hỏi F6, hoặc tự chọn chủ đề để tự luyện. (Sửa 2026-09-05: câu gốc ghi cả phân hệ "kích hoạt sau khi Accepted" — với F5.2 thì `Accepted` không còn là điều kiện bắt buộc kể từ khi ba lối vào được chốt.)_

#### F5.1 — Phân tích bài giải (Solution Review)

Chức năng phân tích một lượt, không hội thoại. AI tiếp nhận đề bài, đặc tả hàm và mã nguồn vừa nộp, trả về dữ liệu có cấu trúc (JSON) gồm:

- **Độ phức tạp thực tế:** Phân tích độ phức tạp thời gian ($O$) và bộ nhớ ($O$) kèm lập luận chi tiết.
- **Đối chiếu tối ưu:** So sánh với độ phức tạp tối ưu đã biết của bài toán; đánh giá bài giải đã tối ưu chưa, nếu chưa thì gợi ý hướng tiếp cận tốt hơn.
- **Lỗ hổng & Trường hợp biên:** Chỉ ra các edge case bộ test chưa phủ, giả định ngầm trong mã, nguy cơ tràn số, rủi ro khi kích thước dữ liệu vào lớn hơn ràng buộc đề bài.
- **Chất lượng mã nguồn (Clean Code):** Nhận xét về cách đặt tên biến/hàm, cấu trúc phân rã, trùng lặp mã (DRY), độ dễ đọc và khả năng bảo trì.
- **Câu hỏi mở rộng:** Đưa ra các câu hỏi suy ngẫm để người học tự củng cố kiến thức.
- **Lưu trữ:** Kết quả hiển thị dạng báo cáo tĩnh, lưu kèm bài nộp để tra cứu lại trong trang tiến độ cá nhân.

#### F5.2 — Phỏng vấn giả lập 1:1 (Mock Interview)

Phiên hội thoại nhiều lượt, AI đóng vai kỹ sư phỏng vấn kỹ thuật, người dùng đóng vai ứng viên bảo vệ bài giải của chính mình qua 3 giai đoạn:

1. **Giải trình thuật toán:** Trình bày ý tưởng tiếp cận và lý do lựa chọn cấu trúc dữ liệu/giải thuật.
2. **Phản biện:** Người phỏng vấn chất vấn các điểm chưa tối ưu, hỏi xoáy vào các trường hợp biên.
3. **Mở rộng (Scale-up):** Đưa ra tình huống khi quy mô dữ liệu tăng đột biến hoặc ràng buộc hệ thống thay đổi.

- **Quản lý ngữ cảnh & Streaming:** Duy trì ngữ cảnh phiên qua `ChatMemory` trên Redis; phản hồi stream mượt mà về giao diện qua Server-Sent Events (SSE).
- **Đánh giá Rubric:** Kết thúc phiên xuất bảng đánh giá năng lực chi tiết: độ rõ ràng khi trình bày, độ chính xác kỹ thuật, khả năng phản biện, nhận thức về độ phức tạp — kèm nhận xét và góp ý cho từng tiêu chí.

#### Ràng buộc chung cho phân hệ AI

- **Chống Prompt Injection:** Mã nguồn và câu trả lời của người dùng được đưa vào prompt dưới dạng dữ liệu (data parameters), tách biệt hoàn toàn khỏi chỉ thị hệ thống (system instructions).
- **Định hướng giáo dục:** Bảng đánh giá và báo cáo phân tích mang tính chất phản hồi hỗ trợ học tập, không dùng làm điểm số chính thức.
- **Kiểm soát chi phí:** Giới hạn tần suất gọi (Rate Limit) theo người dùng, cache kết quả F5.1 theo hash mã nguồn, theo dõi và ghi nhận lượng token tiêu thụ theo từng phiên.
- **Suy giảm có kiểm soát (Graceful Degradation):** Khi phân hệ AI gặp sự cố hoặc hết quota API, các chức năng chấm bài và quản lý cốt lõi (F1–F4) vẫn hoạt động bình thường.

### F6 — Ngân hàng câu hỏi phỏng vấn

_Màn hình độc lập, không gắn liền với bài nộp code — phục vụ ôn luyện lý thuyết và kỹ năng trả lời phỏng vấn._

- **Danh sách câu hỏi:** Phân loại theo **5 chủ đề** (Lý thuyết CS, System design, Database, Ngôn ngữ, Hành vi) và mức độ khó; hỗ trợ tìm kiếm, lọc và đánh dấu yêu thích (bookmark). _(Sửa 2026-09-05: danh mục gốc có 4 chủ đề; `DEC-2026-0830-interview-bank-crud` chốt lấy danh mục 5 chủ đề của prototype làm chuẩn.)_
- **Chế độ học:** Xem gợi ý hướng tiếp cận, khung trả lời chuẩn (áp dụng mô hình STAR cho câu hỏi hành vi) và danh sách các từ khóa kỹ thuật cốt lõi cần nêu.
- **Chế độ luyện:** Người dùng tự soạn câu trả lời, AI đối chiếu với tiêu chí chuẩn của câu hỏi và trả về phản hồi ngắn — điểm đã đạt, điểm còn thiếu, hướng dẫn bổ sung.
- **Theo dõi tiến độ:** Lịch sử luyện tập, danh sách câu hỏi cần ôn tập lại, tỷ lệ hoàn thành theo từng chủ đề.
- ~~**Mở rộng:** Giảng viên (A2) có thể tạo bộ câu hỏi riêng và gán cho các lớp học phụ trách.~~ **NGOÀI PHẠM VI** — `DEC-2026-0828-remove-per-class-interview-set` (ghi nhận vào đây 2026-09-05). Ngân hàng câu hỏi là **một kho duy nhất dùng chung ở cấp hệ thống**; giảng viên và quản trị viên cùng quản trị nội dung kho đó, không có khái niệm bộ câu hỏi riêng theo lớp.

---

## 5. CÔNG NGHỆ SỬ DỤNG

| Tầng kiến trúc            | Công nghệ lựa chọn                                                                                                                                                                      |
| :------------------------ | :-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Giao diện (Frontend)**  | Next.js, React, TypeScript, Tailwind CSS, Monaco Editor                                                                                                                                 |
| **Backend**               | Java 21 (Virtual Threads), Spring Boot, Spring Security, Spring Data JPA                                                                                                                |
| **Hàng đợi & Realtime**   | RabbitMQ, WebSocket (STOMP), Server-Sent Events (SSE)                                                                                                                                   |
| **Dữ liệu & Lưu trữ**     | PostgreSQL, Redis (Cache, ChatMemory, Rate limit), MinIO (Lưu bộ testcase lớn)                                                                                                          |
| **Thực thi mã nguồn**     | go-judge self-hosted (mặc định) — `go-sandbox`: cgroup v1/v2, namespace. Cổng ra trung lập theo engine, Judge0 vẫn là adapter thay thế hợp lệ — `DEC-2026-0823-go-judge-default-engine` |
| **Trí tuệ nhân tạo (AI)** | Spring AI                                                                                                                                                                               |
| **Triển khai & Giám sát** | Docker Compose, GitHub Actions, Prometheus, Grafana                                                                                                                                     |
| **Kiểm thử (Testing)**    | JUnit 5, Testcontainers, k6                                                                                                                                                             |

---

## 6. GIỚI HẠN PHẠM VI

### Nằm trong phạm vi:

- **Ngôn ngữ hỗ trợ:** 3 ngôn ngữ chính: Java, C++, Python.
- **Mô hình nộp bài:** Hỗ trợ cả 2 mô hình: Bọc hàm (Function Wrapper) và Nhập/Xuất chuẩn (Standard I/O).
- **Môi trường chạy:** Thực thi đơn luồng cho mỗi bài nộp trong sandbox cô lập an toàn.
- **Tương tác AI:** Phỏng vấn AI dưới dạng văn bản (Text-based), nắm bắt đầy đủ ngữ cảnh mã nguồn vừa nộp.
- **Quản lý học tập:** Giao bài và quản lý bài tập theo lớp học cho giảng viên.

### Ngoài phạm vi:

- Xây dựng Cloud IDE hoàn chỉnh.
- Bài toán tương tác (Interactive Problems).
- Phỏng vấn bằng giọng nói (Voice Interview).
- Hệ thống giải đấu (Contest) và Bảng xếp hạng trực tiếp (Leaderboard) thời gian thực.
- **Cơ chế chấm lại (Re-judge)** — _bổ sung 2026-09-05_. Câu gốc ở dòng trên ghi ngược lại ("Lưu ý: cơ chế Re-judge vẫn nằm trong phạm vi"); `DEC-2026-0828-remove-rejudge-scope` đã loại hẳn khỏi phạm vi. Khi một testcase sai, học viên báo giảng viên và giảng viên tự sửa rồi tăng phiên bản bộ testcase — không có luồng chấm lại hàng loạt, lượt nộp cũ giữ nguyên kết quả cũ.
- Trình chấm tùy biến (Custom Checker) do người ra đề tự tải lên.

---
