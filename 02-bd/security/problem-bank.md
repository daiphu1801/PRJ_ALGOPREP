# BD — Security module `problem-bank` (F2)

## 1. Phân quyền tạo/sửa nội dung

- **Soạn đề, sửa đặc tả, xuất bản/rút xuống, xoá, nhân bản** (F2-01→04, F2-15, F2-16): gác bởi Function
  `PROBLEM_AUTHORING` trong ma trận Role × Function × Action của `identity`
  (`02-bd/database/identity.md` mục 1.3-1.4) — `problem-bank` chỉ **tham chiếu** mã `PROBLEM_AUTHORING` +
  `Action` (`CREATE`/`UPDATE`/`DELETE`) khi kiểm quyền ở tầng application của chính nó, không thiết kế lại
  ma trận (đúng ranh giới: ma trận là sở hữu của `identity`, `problem-bank` chỉ tra cứu qua claim JWT +
  cache quyền, giống cách mọi module khác tra quyền — `02-bd/security/identity.md` mục 2).
- **Quản lý testcase** (upload theo lô, sửa Hidden testcase, xác nhận testcase nháp AI sinh — F2-06,
  F2-07, F2-14): gác bởi Function `TESTCASE_MANAGEMENT`, tách riêng khỏi `PROBLEM_AUTHORING` vì đây là
  hai loại thao tác rủi ro khác nhau (sửa đề vs sửa dữ liệu chấm điểm) — khớp danh sách 10 Function seed
  đã liệt kê ở `02-bd/database/identity.md` mục 1.3.
- **Giao/gỡ bài theo lớp** (F2-12): gác bởi Function `CLASS_MANAGEMENT:UPDATE`
  (`DEC-2026-0831-class-assignments-round2` xác nhận đúng mã quyền này cho hành động gỡ).
- **Xuất CSV danh sách bài toán** (F2-17): cùng quyền `PROBLEM_AUTHORING:READ` — chỉ đọc metadata, không
  cần quyền ghi.

Mọi kiểm quyền ở **tầng application**, không chỉ ẩn/hiện nút trên giao diện — theo đúng nguyên tắc chung
đã áp dụng cho `identity` (`02-bd/security/identity.md` mục 2), lặp lại ở đây vì đây là quy tắc xuyên
suốt dự án, không phải đặc thù riêng module.

## 2. Chống rò rỉ Hidden testcase (F2-08)

Đây là ràng buộc bảo mật nghiệp vụ quan trọng nhất của module này — `README.md` mục 4-F2 và
`.claude/skills/bd-generation/SKILL.md` mục "Must Not" đều nhắc riêng.

- **Không API nào trả nội dung input hoặc expected-output của testcase `visibility = HIDDEN` cho actor
  A1 (học viên)** — dù trực tiếp (endpoint đọc testcase) hay gián tiếp (endpoint đọc kết quả chấm, log
  lỗi, thông báo lỗi biên dịch). Endpoint duy nhất học viên chạm tới liên quan Hidden testcase là kết quả
  chấm từ `judge-orchestration` — và kết quả đó, theo đúng ranh giới module, do `judge-orchestration` trả
  về (chỉ trạng thái + chỉ số testcase sai), không phải `problem-bank` trả trực tiếp.
- **Presigned URL của Hidden testcase không bao giờ cấp cho actor A1** — chi tiết ở
  `02-bd/storage/problem-bank.md` mục 3.
- **Log ứng dụng** (application log, không phải audit log của `identity`) không được ghi nguyên văn nội
  dung input/expected-output của Hidden testcase ở mức `INFO` trở lên — nếu cần log để debug, chỉ log
  `testcase_id` và độ dài chuỗi, không log nội dung. Đây là ràng buộc vận hành cần đưa vào coding
  convention khi vào giai đoạn code, không phải thứ kiểm bằng test tự động dễ dàng — ghi lại ở đây để DD
  và review code sau này không bỏ sót.
- **Endpoint nội bộ cho `judge-orchestration` lấy input để chấm** (outbound port `judge-orchestration` tự
  khai báo gọi sang, hoặc `problem-bank` phát dữ liệu qua cơ chế khác — chốt ở DD của cả hai module) là
  giao tiếp backend-to-backend, không đi qua đường học viên có thể trực tiếp gọi — không được lộ ra một
  route công khai (`/api/**`) mà chỉ đi qua kênh nội bộ (service-to-service, cùng process trong modular
  monolith — có thể là một lời gọi Java trực tiếp qua outbound port, không cần cả một HTTP round-trip vì
  hai module cùng chạy trong một JVM).

## 3. Bookmark riêng tư tuyệt đối (F2-13)

- Bảng `bookmarks` chỉ đọc/ghi được bởi đúng `user_id` khớp với `user_id` trong Access Token của request —
  không có endpoint nào cho `INSTRUCTOR`/`ADMIN` đọc bookmark của người khác, **kể cả khi có toàn quyền
  `PROBLEM_AUTHORING`/`TESTCASE_MANAGEMENT`/`ADMIN` qua ma trận F1-10** — vì đây không phải một ô của ma
  trận, mà là quyền sở hữu dữ liệu cá nhân nằm ngoài phạm vi RBAC theo Function/Action.
- Mức riêng tư áp dụng cho **cả việc đã bookmark bài nào**, không chỉ nội dung ghi chú
  (`01-rd/req/problem-bank.md:63-66`) — nghĩa là không có API thống kê "bao nhiêu học viên đã lưu bài
  này" lộ ra cho A2/A3, kể cả dạng số tổng hợp ẩn danh, trừ khi RD mở rộng lại phạm vi này (hiện chưa).

## 4. F2-14 — AI sinh testcase, kiểm soát rủi ro

- AI **chỉ được truyền vào prompt**: nội dung đề bài (Markdown) và ràng buộc dữ liệu do Admin khai —
  không truyền mã nguồn Đáp án mẫu vào prompt sinh input (không cần thiết, và giảm bề mặt tấn công
  prompt-injection nếu đề bài chứa văn bản độc hại từ một nguồn không tin cậy — dù ở đây người soạn đề là
  A2/A3 đã qua xác thực, vẫn áp nguyên tắc chung "dữ liệu vào prompt là tham số, tách khỏi chỉ thị hệ
  thống" theo `CLAUDE.md` mục Rules).
- **AI không bao giờ tự viết expected-output** — output luôn là kết quả chạy Đáp án mẫu thật qua
  `JudgeExecutionPort`/`SampleSolutionRunnerPort` (kiến trúc mục 3.2). Đây là kiểm soát bắt buộc, không
  phải tuỳ chọn cấu hình — loại bỏ hoàn toàn khả năng "AI bịa expected-output sai khiến bài đúng bị chấm
  Wrong Answer" (`01-rd/req/problem-bank.md:70-75`).
- Testcase AI sinh ở trạng thái nháp (`is_ai_generated_draft = true`) không được tự động gộp vào bộ Hidden
  dùng để chấm cho tới khi Admin xác nhận thủ công — enforce ở tầng application: mọi truy vấn xây "bộ
  testcase Hidden đang hiệu lực" cho một submission phải lọc `is_ai_generated_draft = false`, không có
  đường tắt nào bỏ qua điều kiện lọc này.

## 5. OWASP / input validation chung

- **Markdown/LaTeX injection**: `statement_md` hiển thị lại cho học viên — render phía frontend phải
  sanitize để tránh XSS qua Markdown chứa HTML thô (ví dụ thẻ `<script>`); `problem-bank` không tự làm
  sạch ở tầng lưu trữ (lưu nguyên văn để không mất định dạng đề bài A2 soạn), trách nhiệm sanitize khi
  hiển thị thuộc tầng render — ghi lại ở đây để không bị hiểu nhầm là `problem-bank` đã xử lý xong.
- **Giới hạn kích thước upload theo lô** (F2-07): giới hạn số lượng testcase/lô và kích thước tổng một
  lần tải lên để tránh một lượt upload làm nghẽn MinIO hoặc Postgres — số cụ thể `[SoT: Suy luận]`, chốt
  ở DD.
- **Xác thực chữ ký hàm khớp type schema** (F3-13, kiến trúc mục 5): validate ở tầng application trước
  khi lưu, không tin dữ liệu client gửi lên là đã đúng cấu trúc JSONB mong đợi.

## 6. Việc còn mở — chuyển sang DD

- Cơ chế cụ thể (HTTP nội bộ có xác thực service-token, hay lời gọi Java trực tiếp qua outbound port)
  cho `judge-orchestration` lấy input Hidden testcase để chấm (mục 2).
- Giới hạn cụ thể kích thước/số lượng một lô upload testcase (mục 5).
- Danh sách chính xác các endpoint áp `TESTCASE_MANAGEMENT` vs `PROBLEM_AUTHORING` khi có bảng API đầy đủ
  ở `03-dd/api/problem-bank.md`.
