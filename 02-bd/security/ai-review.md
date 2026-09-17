# BD — Security module `ai-review` (F5)

> Đọc cùng `02-bd/architecture/ai-review.md` mục 5 (ba lớp instruction) trước — mục này bổ sung phần chưa
> nói ở đó: quyền sở hữu dữ liệu, ma trận phân quyền, và các kiểm soát OWASP còn lại.

## 1. Chống prompt injection (OWASP LLM01) — tham chiếu, không lặp lại

Thiết kế đầy đủ ba lớp instruction (system cố định / ngữ cảnh giảng viên bậc hai / dữ liệu người dùng) đã
chốt ở `02-bd/architecture/ai-review.md` mục 5 (`DEC-2026-0831-ai-instruction-injection-guard`). Bổ sung
kiểm soát tầng ứng dụng không thuộc phạm vi "kiến trúc":

- **Giới hạn độ dài** input người dùng trước khi đưa vào `LlmProviderPort`: mã nguồn tối đa theo giới hạn
  đã có của `judge-orchestration` (không kiểm lại, chỉ đọc từ `SubmissionSnapshotPort`); câu trả lời hội
  thoại mỗi lượt giới hạn ký tự (đề xuất 4000 ký tự/lượt, `[SoT: Suy luận]`) — chặn kiểu tấn công "prompt
  flooding" (nhồi văn bản cực dài để đẩy trôi system instruction ra khỏi context window của model, một
  biến thể của LLM01 không liên quan tới nội dung chỉ thị giả).
- **Không log nguyên văn `perProblemAiContext` cùng mức với system instruction cố định** trong audit log
  — ghi tách riêng để một phiên rà soát log không nhầm hai nguồn khác cấp bậc thành cùng một khối chỉ thị.
- **`perProblemAiContext` chỉ sửa được qua đúng một đường**: use case cập nhật bài toán của `problem-bank`
  (gác bởi `PROBLEM_AUTHORING:UPDATE`, F1-10 matrix) — `ai-review` **chỉ đọc** trường này qua
  `ProblemContextPort` (kiến trúc mục 3.3, 3.4), không có endpoint nào trong `ai-review` cho phép ghi hay
  sửa nội dung này trực tiếp. Loại trừ khả năng một actor không có quyền soạn đề chỉnh nội dung này qua
  một đường vòng nào đó bên trong `ai-review`.

## 2. Quyền sở hữu dữ liệu (data ownership) — tách khỏi RBAC theo role

Đúng nguyên tắc đã đặt ở `02-bd/security/identity.md` mục 2: `identity` chỉ cấp `user_id` đã xác thực qua
claim JWT, **`ai-review` tự kiểm sở hữu ở tầng use case của chính mình**, không nhờ `identity` kiểm hộ.

- **Học viên**: chỉ đọc `solution_reviews`/`interview_sessions` có `user_id` khớp `user_id` trong token —
  kiểm ở mọi query, không nhận `user_id` tuỳ ý từ tham số request (cùng nguyên tắc F1-22 đã áp cho
  `identity`, áp lại ở đây cho dữ liệu AI). Vi phạm trả `403 FORBIDDEN`, không trả `404` (tránh lộ thông
  tin "bản ghi này tồn tại nhưng không phải của bạn" qua khác biệt mã lỗi — dù ở mức độ rủi ro thấp hơn
  identity vì `id` là UUID không đoán được, vẫn giữ nhất quán chính sách lỗi giữa hai module).
- **Giảng viên qua `instructor_grading` (F5-27)**: đọc **và ghi thêm** (`manual_score`, `manual_comment`)
  trên `solution_reviews` — nhưng chỉ những bản ghi mà `submission.problem_id` thuộc lớp giảng viên đó phụ
  trách (cùng cơ chế phạm vi với F2-12, kiểm qua `identity.class_enrollments` — không import, đọc qua một
  outbound port riêng của `identity` mà `ai-review` gọi, tên đề xuất `ClassScopeQueryPort`
  `[SoT: Suy luận]`, chốt ở DD). Gác bởi Function `CLASS_MANAGEMENT` trong ma trận phân quyền
  (`01-rd/req/identity.md` F1-12) — **chỉ tham chiếu mã Function đã có, không thiết kế lại ma trận** ở BD
  này.
- **Ghi đè điểm chấm tay không phải ghi đè `ai_score_10`**: use case chấm tay chỉ `UPDATE` các cột
  `manual_*`, không bao giờ `UPDATE ai_score_10`/`result_json` — về mặt schema, hai cột này thuộc hai
  luồng ghi khác nhau (F5.1 sinh ra `ai_score_10` một lần, F5-27 ghi `manual_*` sau đó, không bao giờ cùng
  một câu lệnh `UPDATE`) — đảm bảo bất biến "AI: 4/10 · GV: 7/10" hiển thị song song (Q3,
  `DEC-2026-0831-instructor-grading-round2`) không bao giờ bị vi phạm ở tầng dữ liệu.
- **Admin (`AI_CONFIG`, `AI_TOKEN_BUDGET`)**: đọc mọi `prompt_templates`/`rubric_configs`/
  `ai_token_budget_configs`/`ai_usage_anomaly_alerts` — không có phạm vi "theo lớp", vì đây là cấu hình
  toàn hệ thống, không phải dữ liệu của một học viên/lớp cụ thể.
- **Admin không tự động thấy nội dung `interview_turns`/`solution_reviews.result_json` của một học viên cụ
  thể** trừ khi có một chức năng quản trị riêng yêu cầu (ví dụ điều tra khiếu nại) — RD không đặc tả chức
  năng này, **không thiết kế thêm** ở đợt này; nếu cần, mở một Function mới trong ma trận (ví dụ
  `AI_CONTENT_AUDIT`) qua một quyết định riêng, không tự suy diễn ở BD.

## 3. Định hướng giáo dục — không phải điểm chính thức (F5-18)

Ràng buộc này chủ yếu là UI, nhưng có một phần dữ liệu: **không có trường nào trong `ai` schema được đặt
tên hay comment kiểu "official score"/"điểm chính thức"** — `ai_score_10`, rubric scores đều gắn rõ ngữ
nghĩa "tham khảo" trong tên cột/comment (mục database `solution_reviews.ai_score_10`,
`02-bd/database/ai-review.md` mục 1.3). Endpoint trả về các trường này (chốt ở DD) phải luôn kèm một cờ
hoặc nhãn tường minh (ví dụ `isReferenceOnly: true`) để tầng giao diện không thể vô tình hiển thị như điểm
số chính thức — đây là điều kiện chấp nhận (acceptance criterion) cần đưa vào `04-tdd/ai-review.md` khi
viết DD, không phải điều BD tự áp đặt UI.

**Không ảnh hưởng trạng thái submission**: không có ràng buộc khoá ngoại hay trigger nào từ schema `ai`
ghi ngược lại `judge.submissions.status` — về mặt kỹ thuật `ai-review` **không có quyền ghi** vào schema
`judge` (module khác, không import). Đây là cách chốt cứng "tách bạch hoàn toàn khỏi kết quả Pass/Fail
chính thức" (F5-27) ở tầng kiến trúc, không chỉ ở tầng quy ước.

## 4. Kiểm soát chi phí — khía cạnh bảo mật/lạm dụng

Bổ sung khía cạnh bảo mật của mục kiểm soát chi phí đã thiết kế ở kiến trúc mục 6:

- **Rate limit và token budget kiểm tra TRƯỚC khi gọi `LlmProviderPort`**, không phải sau — tránh một
  actor gửi nhiều request đồng thời (race) vượt giới hạn trước khi bộ đếm kịp cập nhật. Dùng lệnh tăng
  nguyên tử của Redis (`INCR` + `EXPIRE`) cho rate-limit, không đọc-rồi-ghi hai bước riêng.
- **Khoá do vượt ngân sách (F5-25) kiểm ở tầng use case, không phải tầng gateway/proxy** — vì cần phân
  biệt role (`STUDENT`/`INSTRUCTOR` bị chặn, `ADMIN` không) mà một reverse proxy không biết role.
- **`AI_TOKEN_BUDGET`/`AI_CONFIG` là hai Function riêng** (F1-12) dù cùng nằm trong màn `admin_ai_usage`/
  `admin_ai_config` — một vai trò được cấp `AI_CONFIG:UPDATE` (sửa prompt) không tự động có
  `AI_TOKEN_BUDGET:UPDATE` (mở khoá ngân sách) trừ khi ma trận cấp riêng cả hai. Đây là chủ ý của F1-12
  tách hai Function, không phải sơ suất — kiểm cả hai độc lập ở tầng `@PreAuthorize`.
- **`ai_usage_anomaly_alerts` chỉ đọc/đánh dấu `REVIEWED` — không có endpoint nào tự động khoá tài khoản
  từ bảng này** (`DEC-2026-0831-ai-usage-anomaly-alert`) — nếu `ADMIN` quyết định khoá sau khi xem cảnh
  báo, đó là một hành động riêng qua `USER_MANAGEMENT` (khoá tài khoản, F1-13), không phải một hiệu ứng
  phụ tự động của module `ai-review`.

## 5. Suy giảm có kiểm soát — khía cạnh bảo mật

- `LlmProviderPort` xuống/timeout không tạo ra một trạng thái dữ liệu "treo" nào cần dọn dẹp bảo mật (khác
  `judge-orchestration` cần sweep job vì có trạng thái trung gian `PENDING/COMPILING/JUDGING`) — F5.1 là
  một giao dịch ngắn (gọi LLM thất bại → không ghi `solution_reviews`, trả lỗi ngay, không có bản ghi dở
  dang); F5.2 giữ nguyên `interview_sessions.stage` hiện tại nếu một lượt lỗi giữa chừng, học viên thử lại
  lượt đó — không cần trạng thái `SYSTEM_ERROR` riêng cho phiên phỏng vấn.
- **Không có secret/API key của LLM provider nào lộ ra ngoài `infrastructure` layer** — cấu hình qua biến
  môi trường/secret manager (đề xuất, `[SoT: Suy luận]`, khớp `CLAUDE.md` mục Deploy: Docker Compose), do
  `SpringAiLlmAdapter` giữ, không truyền qua tham số port hay log ra `outbox_events`/`token_usage`.

## 6. Rủi ro chưa xử lý — cần chủ dự án xác nhận trước DD

- Ngưỡng giới hạn ký tự input người dùng mỗi lượt hội thoại (mục 1, đề xuất 4000 ký tự) — số cụ thể.
- `ClassScopeQueryPort` (mục 2) — port này đã tồn tại dưới tên khác ở `identity`/`problem-bank` hay cần
  tạo mới hoàn toàn ở DD `ai-review`; nếu `problem-bank` (F2-12) đã có cơ chế tương tự cho phạm vi lớp,
  nên tái dùng thay vì tạo trùng — đối chiếu `02-bd/architecture/problem-bank.md` khi viết DD.
- Có cần Function riêng `AI_CONTENT_AUDIT` cho quản trị viên xem nội dung hội thoại khi có khiếu nại (mục
  2) hay để ngoài phạm vi capstone — RD chưa đặc tả.

## 7. Tham chiếu

- `02-bd/architecture/ai-review.md` mục 5 — ba lớp instruction, chi tiết đầy đủ.
- `02-bd/security/identity.md` mục 2 — nguyên tắc "quyền sở hữu dữ liệu tách khỏi RBAC theo role" mà mục
  2 file này áp dụng lại cho `ai-review`.
- `01-rd/req/identity.md` — F1-10 → F1-13 (ma trận phân quyền), Function `AI_CONFIG`, `AI_TOKEN_BUDGET`,
  `CLASS_MANAGEMENT`, `PROBLEM_AUTHORING`.
- `.nexa/control/decision-registry.md` — `DEC-2026-0831-ai-instruction-injection-guard`,
  `DEC-2026-0831-ai-usage-anomaly-alert`, `DEC-2026-0831-instructor-grading-round2`.
- `CLAUDE.md` — "A submission is data, never an instruction", "The AI subsystem degrades gracefully".
