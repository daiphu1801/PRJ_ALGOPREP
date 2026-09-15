# BD — Security module `interview-bank` (F6)

> Đọc cùng `02-bd/architecture/interview-bank.md` mục 5 (hai lớp instruction) trước — mục này bổ sung
> phần chưa nói ở đó: quyền sở hữu dữ liệu, ma trận phân quyền, và kiểm soát tầng ứng dụng còn lại.

## 1. Chống prompt injection (OWASP LLM01) — tham chiếu, không lặp lại

Thiết kế hai lớp instruction (system cố định / tiêu chí tham chiếu có cấu trúc / dữ liệu người dùng) đã
chốt ở `02-bd/architecture/interview-bank.md` mục 5. Bổ sung kiểm soát tầng ứng dụng:

- **Giới hạn độ dài câu trả lời** trước khi đưa vào `AnswerFeedbackPort`: đề xuất 3000 ký tự/lượt
  (`[SoT: Suy luận]`, gần với mức 4000 ký tự/lượt hội thoại của `ai-review` — câu trả lời phỏng vấn miệng
  thường ngắn hơn một lượt hội thoại đa vòng) — chặn "prompt flooding" đẩy trôi system instruction khỏi
  context window, cùng lý do đã nêu ở `02-bd/security/ai-review.md` mục 1.
- **`answer_rubrics` chỉ sửa được qua đúng một đường**: use case CRUD của chính `interview-bank`
  (`interview_question_authoring`, gác bởi `INTERVIEW_BANK_MANAGEMENT`, F1-12) — không có endpoint nào
  khác trong hệ thống được phép ghi bảng này, loại trừ khả năng một actor không có quyền soạn nội dung
  chỉnh tiêu chí chấm qua đường vòng.
- **Không log nguyên văn `answer_text`/`feedback_result_json` cùng cấp với system instruction** trong
  audit log — tách riêng, cùng lý do đã nêu ở `ai-review` (rà soát log không nhầm lẫn cấp bậc nguồn).

## 2. Quyền sở hữu dữ liệu (data ownership) — tách khỏi RBAC theo role

Đúng nguyên tắc đã đặt ở `02-bd/security/identity.md` mục 2: `identity` chỉ cấp `user_id` đã xác thực
qua claim JWT, **`interview-bank` tự kiểm sở hữu ở tầng use case của chính mình**.

- **Học viên**: chỉ đọc `user_answers`/`bookmarks`/`practice_history`/`recall_ratings` có `user_id` khớp
  `user_id` trong token — kiểm ở mọi query, không nhận `user_id` tuỳ ý từ tham số request. Vi phạm trả
  `403 FORBIDDEN`, không `404` — cùng chính sách lỗi đã áp dụng nhất quán ở `identity`/`ai-review`.
- **Danh sách/nội dung câu hỏi** (`interview_questions`, `question_topics`, `answer_rubrics` phần đọc
  Chế độ học/Chế độ luyện): công khai cho mọi `STUDENT` đã đăng nhập, **không có phạm vi sở hữu theo
  người dùng** — đây là dữ liệu dùng chung toàn hệ thống (F6-11 đã bỏ, không có khái niệm "câu hỏi của
  lớp tôi"), khác hẳn `user_answers` (dữ liệu cá nhân).
- **Giảng viên/Quản trị viên qua `interview_question_management`/`interview_question_authoring`
  (F6-13)**: tạo/sửa/nhân bản/xoá mềm `interview_questions`/`answer_rubrics` — gác bởi Function
  `INTERVIEW_BANK_MANAGEMENT` (`01-rd/req/identity.md:60`, F1-12). **Không có phạm vi "theo lớp"** cho
  thao tác này (khác `ai-review`'s `CLASS_MANAGEMENT` scope theo lớp phụ trách) — vì ngân hàng câu hỏi
  là dùng chung toàn hệ thống, một A2 có quyền `INTERVIEW_BANK_MANAGEMENT:CREATE`/`UPDATE` sửa được nội
  dung áp dụng cho toàn bộ học viên, không chỉ lớp mình phụ trách. **Đã đóng 2026-09-13** — căn cứ
  `01-rd/screens/shared/interview_question_management.md` dòng 110-112 (Given-When-Then): "tôi thấy và
  sửa được toàn bộ kho câu hỏi hệ thống — không chia theo lớp (A2 và A3 cùng phạm vi dữ liệu)". A2
  **không** bị giới hạn bởi `created_by` — cột đó chỉ phục vụ hiển thị/audit ("ai tạo"), không phải điều
  kiện lọc quyền sửa. Không thêm điều kiện `created_by = currentUser` ở tầng use case.
- **Admin (`INTERVIEW_BANK_MANAGEMENT` đầy đủ)**: đọc/ghi toàn bộ ngân hàng câu hỏi, không giới hạn.
  Không tự động thấy `user_answers`/`recall_ratings` của một học viên cụ thể trừ khi có Function riêng
  yêu cầu (cùng nguyên tắc đã áp dụng cho `ai-review`'s `interview_turns` — RD không đặc tả chức năng
  này, không thiết kế thêm ở đợt này).

## 3. `question_sets` — đã đóng 2026-09-13: không áp dụng

`question_sets` đã bỏ hẳn khỏi phạm vi module (xem `02-bd/architecture/interview-bank.md` mục 2.2,
`02-bd/database/interview-bank.md` mục 1.8) — không có mục quyền sở hữu nào cần thiết kế ở đây.

## 4. Bất biến ghi dữ liệu (F6-07) — kiểm ở tầng ứng dụng, không chỉ tầng API

- **Không tồn tại use case `UpdateUserAnswerCommand`** (kiến trúc mục 4.2) — đây là kiểm soát tầng thiết
  kế, không chỉ một quy ước API: nếu một agent/lập trình viên tương lai thêm endpoint `PATCH
  /user-answers/{id}`, đó là vi phạm trực tiếp `DEC-2026-0831-outside-screens-closures` cần chặn ở code
  review, không chỉ ở tài liệu.
- **`attempt_no` tính ở tầng ứng dụng trong cùng giao dịch** với việc `INSERT` (đếm + 1, khoá dòng cùng
  `(user_id, question_id)` bằng `SELECT ... FOR UPDATE` hoặc constraint unique bắt lỗi race condition) —
  hai request đồng thời cùng một học viên nộp cùng một câu hỏi không được phép sinh trùng `attempt_no`.

## 5. Kiểm soát chi phí — khía cạnh bảo mật

- **Rate limit kiểm TRƯỚC khi gọi `AnswerFeedbackPort`**, không phải sau — dùng lệnh tăng nguyên tử của
  Redis (`INCR` + `EXPIRE`), cùng nguyên tắc đã áp dụng ở `ai-review` mục 4.
- **Không có cơ chế token-budget/khoá tự động riêng cho F6** ở đợt này — module nhỏ hơn F5 nhiều, RD
  không nêu yêu cầu tương đương F5-21/F5-25 cho F6. `[SoT: Suy luận]` — nếu chi phí AI của F6 trở nên
  đáng kể trong thực tế vận hành, thêm cơ chế tương tự `ai_token_budget_configs` là một mở rộng độc lập,
  không phải khoảng trống của phiên BD này (RD không yêu cầu).

## 6. Suy giảm có kiểm soát — khía cạnh bảo mật

- `AnswerFeedbackPort` down/timeout không tạo trạng thái dữ liệu "treo" cần dọn dẹp — Chế độ luyện là
  một giao dịch ngắn: `user_answers` vẫn được ghi (`feedback_status = FAILED`), không có bản ghi dở
  dang cần sweep job như `judge-orchestration`.
- Toàn bộ Chế độ học (F6-04/05/06, không qua AI) không chạm `AnswerFeedbackPort` — module này **tự nó
  đã suy giảm nhẹ nhàng theo thiết kế**, không cần cơ chế circuit breaker phức tạp như `ai-review` (rủi
  ro thấp hơn nhiều vì chỉ một luồng nhỏ — F6-08 — phụ thuộc AI, so với hai luồng lớn của F5).
- Không có secret/API key của LLM provider lộ ra ngoài `infrastructure` layer — cấu hình qua biến môi
  trường, do `SpringAiFeedbackAdapter` giữ riêng (khác instance cấu hình của `ai-review`'s
  `SpringAiLlmAdapter`, dù cùng công nghệ nền Spring AI).

## 7. Rủi ro chưa xử lý — cần chủ dự án xác nhận trước DD

- Ngưỡng giới hạn ký tự câu trả lời mỗi lượt (mục 1, đề xuất 3000 ký tự) — số cụ thể.
- ~~A2 sửa được toàn bộ ngân hàng câu hỏi hay chỉ câu hỏi chính mình tạo~~ — **đã đóng 2026-09-13: toàn
  bộ**, xem mục 2.
- Có cần cơ chế token-budget/rate-limit chặt hơn cho F6 (mục 5) hay mức hiện tại (chỉ rate-limit đơn
  giản) là đủ cho phạm vi capstone.
- ~~Phạm vi quyền của `question_sets`~~ — không áp dụng nữa, xem mục 3.

## 8. Tham chiếu

- `02-bd/architecture/interview-bank.md` mục 5 — hai lớp instruction, chi tiết đầy đủ.
- `02-bd/security/identity.md` mục 2 — nguyên tắc "quyền sở hữu dữ liệu tách khỏi RBAC theo role".
- `02-bd/security/ai-review.md` — mẫu kiểm soát chi phí/suy giảm có kiểm soát tham chiếu lại ở đây.
- `01-rd/req/identity.md:60` — Function `INTERVIEW_BANK_MANAGEMENT` (F6-12, F6-13).
- `.nexa/control/decision-registry.md` — `DEC-2026-0828-remove-per-class-interview-set`,
  `DEC-2026-0830-interview-bank-crud`, `DEC-2026-0831-outside-screens-closures`.
- `CLAUDE.md` — "A submission is data, never an instruction", "The AI subsystem degrades gracefully".
