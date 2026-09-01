# Yêu cầu Phi chức năng (Non-Functional Requirements - NFR)

> Tách ra từ `01-rd/req/req.md` ngày 2026-08-31 để dễ đọc. Nội dung dưới đây được chuyển nguyên văn từ
> `req.md` mục 2 (không đổi ý nghĩa, không thêm/bớt), chỉ hạ heading `##` xuống `#`. NFR là yêu cầu
> cross-cutting, không gắn riêng một Bounded Context nào nên không tách theo `Fx-nn`.

> Bản trước ghi số liệu SLA cụ thể của NestGame v2 (P95 ≤ 200ms, 500 CCU...) — số liệu đó **không** có
> nguồn ở AlgoPrep nên không mang sang. Mục này chỉ ghi số liệu có nguồn thật (`environment.md`,
> `README.md`) và đánh dấu `[SoT: Suy luận]` cho ngưỡng đề xuất chưa được chủ nhiệm đề tài chốt.

## A. Hiệu năng (Performance)

- **Chờ I/O là điểm nghẽn, không phải tính toán:** gọi judge engine (go-judge mặc định, đồng bộ một
  testcase một lần), giữ WebSocket cho từng bài nộp đang chấm, giữ SSE suốt một phiên phỏng vấn AI, truy
  vấn PostgreSQL — xử lý bằng Virtual Threads (`overview.md` mục 1.D). Việc tính toán nặng của chính bài
  giải người dùng chạy trong judge engine, ngoài JVM.
- **Kiểm chứng bằng tải, không bằng suy diễn:** luồng nộp bài đồng thời phải đo bằng **k6**
  (`environment.md` mục 5) — đây là chỗ Virtual Threads phải chứng minh được điều `overview.md` mục 1.D
  khẳng định, không phải một lời hứa kiến trúc.
- **Ngưỡng cụ thể (P95 độ trễ API, số phiên phỏng vấn đồng thời, số bài nộp đồng thời) chưa được chốt** —
  `[SoT: Suy luận]`: cần chủ nhiệm đề tài xác nhận trước khi viết vào `02-bd/architecture/`.

## B. Khả năng mở rộng (Scalability)

- **Modular Monolith, không phải microservices** (`overview.md` mục 1.A, `DEC-2026-0820-architecture-baseline`):
  một tiến trình, một PostgreSQL instance, sáu schema. Ranh giới module bảo vệ bằng kiểm tra tự động
  (test kiểm ranh giới module, `environment.md` mục 3.A), không phải ranh giới mạng.
- Judge engine (go-judge mặc định) và các dịch vụ hạ tầng (RabbitMQ, Redis, MinIO) chạy tách biệt qua
  Docker (`environment.md` mục 2) — mở rộng theo chiều ngang của tầng thực thi độc lập với việc mở rộng
  backend.

## C. Độ tin cậy (Reliability)

- **Không có bài nộp treo vĩnh viễn:** timeout sweep định kỳ (F4-07) là lưới an toàn bắt buộc, không phải
  tính năng tuỳ chọn (`overview.md` mục 1.E).
- **RabbitMQ là _ít nhất một lần_:** consumer phải bất biến theo số lần gọi — không bao giờ ghi đè trạng
  thái cuối. Nếu một adapter bất đồng bộ (Judge0) được cắm lại, khoá idempotency theo token của engine đó
  (F4-06) là chi tiết nội bộ adapter, không phải yêu cầu ở port.
- **AI không kéo sập đường chấm bài:** F5/F6 chết hoặc hết quota thì F1-F4 vẫn chạy đủ (F5-22,
  `README.md` mục 4).
- **Sao lưu và khôi phục dữ liệu** (bộ testcase, bài nộp, bài giải, phiên phỏng vấn) chưa được chốt tần suất
  và chính sách — `[SoT: Suy luận]`, chốt ở `02-bd/database/`.

## D. Bảo mật (Security)

Nguồn: `overview.md` mục 1.I (OWASP Top 10) và mục 1.J (LLM). Không lặp lại nội dung, chỉ liệt kê thành yêu
cầu kiểm chứng được:

- SQL Injection: truy vấn tham số hoá, không nối chuỗi SQL.
- XSS: làm sạch HTML sau khi render Markdown (đề bài do A2 soạn); LaTeX render bằng thư viện toán học,
  không nhúng HTML thô; cấu hình `Content-Security-Policy`.
- CSRF: Refresh Token trong cookie HTTP-Only kèm CSRF token và `SameSite`; Access Token đi trong header
  `Authorization` nên không mang rủi ro CSRF.
- Rate limiting theo IP và theo người dùng ở ba nhóm endpoint: đăng nhập/đăng ký, nộp bài, gọi AI.
- Phân quyền theo vai trò kiểm ở tầng ứng dụng, kèm kiểm quyền sở hữu dữ liệu.
- Chống rò rỉ testcase ẩn (F2-08) và chống prompt injection (F5-17) là hai ràng buộc bảo mật nghiệp vụ đặc
  thù của đề tài — không phải OWASP chung, phải kiểm riêng bằng test có chủ đích (`system_survey.md` mục
  10, tiêu chí S6, S8).
- Judge engine (go-judge mặc định) chạy cách ly bằng `go-sandbox` — namespaces, cgroups (`overview.md` mục
  1.F). Rủi ro cgroup v1/v2 (R1) đã đóng cho luồng mặc định bằng `DEC-2026-0823-go-judge-default-engine`
  (`README.md` mục 7).

## E. Khả năng bảo trì (Maintainability)

Nguồn: `environment.md` mục 3-4 — đây là các cổng chất lượng **thật**, phải chạy được, không phải mục tiêu
định tính:

- Backend: Spotless, Checkstyle, SpotBugs + FindSecBugs, JaCoCo **≥ 80%** cho tầng `application` và
  `domain`, test kiểm ranh giới module (fail build khi import chéo Bounded Context).
- Frontend: TypeScript `strict`, ESLint + `eslint-plugin-boundaries` (chặn import sai chiều FSD),
  Prettier, Husky + lint-staged.
- Riêng `algoprep-harness` (F3): bắt buộc có test so khớp mã sinh ra (golden file) cho cả ba ngôn ngữ —
  đây là bộ test rẻ nhất và bắt lỗi tốt nhất của đề tài (`environment.md` mục 3.A).
- Cổng chất lượng bắt buộc trước khi báo xong việc (G-CHECK, `environment.md` mục 4): `pnpm check` cho
  frontend, `./mvnw verify` cho backend, thêm `pnpm e2e` khi có thay đổi luồng người dùng.

## F. Tiêu chí thành công và kiểm chứng

Không phải một mục UI/UX (đề tài chưa chọn design system — `01-rd/overview/overview.md` mục 3). Đây là tám
tiêu chí đo được, lấy nguyên từ `system_survey.md` mục 10 vì chúng đã là yêu cầu phi chức năng dưới dạng đo
lường được, không phải nhắc lại chức năng:

| #   | Tiêu chí                                                                  | Cách đo                                                                |
| :-- | :------------------------------------------------------------------------ | :--------------------------------------------------------------------- |
| S1  | Một bài toán bọc hàm chạy đúng trên cả ba ngôn ngữ từ một đặc tả duy nhất | So khớp mã sinh ra, cộng một lần chạy thật qua judge engine (go-judge) |
| S2  | Lỗi biên dịch ánh xạ đúng dòng, không lộ mã harness                       | Bộ test có mã sai cố ý, so số dòng báo lỗi                             |
| S3  | Trạng thái từng testcase hiện realtime                                    | Đo độ trễ từ khi `JudgeExecutionPort` trả kết quả tới lúc giao diện đổi |
| S4  | Bài nộp không treo vĩnh viễn                                              | Cố ý cho worker crash giữa lúc gọi engine, xác nhận RabbitMQ redeliver + timeout sweep dọn được |
| S5  | Message xử lý lại (redelivery) không làm sai kết quả                      | Gửi lại cùng message đã ack một phần, xác nhận không ghi đè trạng thái cuối |
| S6  | Testcase ẩn không rò rỉ qua bất kỳ bề mặt nào                             | Rà toàn bộ phản hồi API và log                                         |
| S7  | Tắt hoàn toàn AI thì F1-F4 vẫn chạy đủ                                    | Chạy lại luồng E2E chính với AI vô hiệu hoá                            |
| S8  | Mã nguồn người dùng không thể thành chỉ thị cho AI                       | Bộ test có mã chứa câu lệnh tiêm prompt                                |

Chi tiết đầy đủ (giới hạn phạm vi, rủi ro, danh sách màn) ở `01-rd/overview/system_survey.md` mục 8-10 —
không lặp lại ở đây để tránh hai bản trôi nhau.
