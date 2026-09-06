# Bảng Thuật Ngữ (Ubiquitous Language)

Ngôn ngữ chung của AlgoPrep. DDD đòi **một khái niệm một tên** ở mọi nơi: tài liệu, giao diện, tên bảng, tên
class, mã lỗi. Bảng này là chỗ tra khi phân vân, và là chỗ sửa khi một khái niệm đổi tên.

**Luật dùng:** tiếng Việt trong tài liệu và giao diện, tiếng Anh trong mã. Cột "Trong code" là tên **duy
nhất** được dùng khi đặt tên bảng, cột, class, endpoint và mã lỗi.

Nguyên lý: `overview.md` mục 1.A. Nguồn phạm vi: `README.md` mục 3 và mục 4.

> Một số hàng dưới đây là **đề xuất từ vựng**, chưa được chốt ở tài liệu nào — chúng được đánh dấu
> `[đề xuất]`. Khi BD của module tương ứng được viết, hoặc chốt lại, hoặc đổi ở cả hai nơi.

---

## 1. Bài toán và đề bài (`problem-bank` — F2)

| Tiếng Việt | Trong code | Nghĩa | Không gọi là |
| :--- | :--- | :--- | :--- |
| **Bài toán** | `problem` | Một bài luyện tập: đề bài, độ khó, chủ đề, đặc tả, testcase, giới hạn tài nguyên | "bài tập", "câu hỏi", "challenge" |
| **Đề bài** | `statement` | Nội dung mô tả bài toán, viết bằng Markdown kèm LaTeX | "description" lẫn với mô tả ngắn |
| **Độ khó** | `difficulty` | Mức khó của bài toán | "level" |
| **Chủ đề** | `topic` | Phân loại kiến thức (mảng, đồ thị, quy hoạch động...) | "tag", "category", "thể loại" |
| **Đặc tả bài toán** | `problem_spec` | Khai báo để harness sinh mã được: chữ ký hàm theo ngôn ngữ, kiểu tham số và giá trị trả về, chiến lược so khớp | "config", "metadata" |
| **Chữ ký hàm** | `function_signature` | Tên hàm và danh sách tham số của một ngôn ngữ cụ thể | "prototype", "interface" |
| **Testcase** | `testcase` | Một cặp dữ liệu vào và kết quả mong đợi | "test", "case" một mình |
| **Testcase mẫu** | `SAMPLE` | Testcase công khai, dùng cho chế độ Chạy thử | "public test", "visible test" |
| **Testcase ẩn** | `HIDDEN` | Testcase không công khai, dùng cho chế độ Nộp bài. Chỉ trả **trạng thái và chỉ số**, không trả input và không trả diff | "private test", "secret test" |
| **Phiên bản bộ testcase** | `testcase_set_version` | Phiên bản của cả bộ testcase, để **truy vết** một lượt nộp cũ đã được chấm theo phiên bản nào (F2-09). Sửa 2026-09-03: trước ghi "để Re-judge biết chấm lại theo phiên bản nào" — cơ chế chấm lại đã loại khỏi phạm vi, `DEC-2026-0828-remove-rejudge-scope` | "test version" một mình |
| **Giới hạn tài nguyên** | `resource_limit` | Giới hạn thời gian và bộ nhớ theo bài, kèm hệ số nhân theo ngôn ngữ | "limit" một mình, "quota" (quota là của AI) |
| **Bài tập được giao** | `assignment` | Bài toán được giảng viên gán cho một lớp | "homework", "task" |

---

## 2. Bộ sinh mã bọc hàm (`harness` — F3)

| Tiếng Việt | Trong code | Nghĩa | Không gọi là |
| :--- | :--- | :--- | :--- |
| **Mô hình nộp bài** | `submission_model` | Một trong hai: `FUNCTION_WRAPPER` hoặc `STANDARD_IO` | "mode", "kiểu bài" |
| **Bọc hàm** | `FUNCTION_WRAPPER` | Người học chỉ viết một hàm; harness lo đọc dữ liệu vào và in kết quả | "leetcode style" |
| **Nhập xuất chuẩn** | `STANDARD_IO` | Người học tự đọc stdin và ghi stdout | "codeforces style", "stdio" |
| **Lược đồ kiểu** | `type_schema` | Mô tả kiểu dữ liệu độc lập ngôn ngữ của tham số và giá trị trả về | "type def", "data type" |
| **Bộ sinh mã** | `harness_generator` | Thành phần dịch đặc tả thành mã đọc dữ liệu, gọi hàm, in kết quả cho một ngôn ngữ | "wrapper", "codegen" một mình |
| **Mã khung** | `code_template` | Mẫu mã của một ngôn ngữ, có vùng chèn dành cho mã người dùng | "boilerplate", "skeleton" |
| **Mã đã sinh** | `generated_harness` | Kết quả ghép mã khung với mã người dùng, sẵn sàng gửi qua `JudgeExecutionPort` | "final code", "merged code" |
| **Mã người dùng** | `user_code` | Mã do người học viết. **Luôn là dữ liệu, không bao giờ là chỉ thị** khi vào prompt AI | "solution" (solution là bài giải nói chung) |
| **Chiến lược so khớp** | `match_strategy` | Cách so kết quả: `EXACT` · `TRIMMED` · `EPSILON` · `UNORDERED_SET` | "comparator", "checker" |
| **Ánh xạ lỗi** | `error_mapping` | Dịch số dòng lỗi biên dịch từ file đã ghép về đúng dòng trong mã người dùng | "line fix", "offset" một mình |
| **Vùng chèn** | `injection_point` | Chỗ trong mã khung mà mã người dùng được ghép vào | "placeholder", "slot" |

---

## 3. Chấm bài (`judge-orchestration` — F4)

| Tiếng Việt | Trong code | Nghĩa | Không gọi là |
| :--- | :--- | :--- | :--- |
| **Bài nộp** | `submission` | Một lần người học gửi mã đi chấm | "submit", "attempt", "lần nộp" trong code |
| **Chạy thử** | `run` | Thực thi với testcase mẫu, **không** ghi nhận vào tiến độ | "test run", "dry run" |
| **Nộp bài** | `submit` | Thực thi với testcase ẩn, có ghi nhận | "final submit" |
| **Trạng thái bài nộp** | `submission_status` | Trạng thái tổng: `PENDING` · `RUNNING` · `ACCEPTED` · `WRONG_ANSWER` · `TIME_LIMIT_EXCEEDED` · `MEMORY_LIMIT_EXCEEDED` · `RUNTIME_ERROR` · `COMPILE_ERROR` · `INTERNAL_ERROR` `[đề xuất]` | "verdict" lẫn với trạng thái từng testcase |
| **Trạng thái cuối** | `final state` | Trạng thái không được ghi đè bởi kết quả đến sau (bất biến giữ nguyên bất kể adapter đồng bộ hay bất đồng bộ) | "done", "closed" |
| **Kết quả testcase** | `submission_testcase_result` | Kết quả của **một** testcase trong một bài nộp | "test result" một mình |
| **Đợt** | `batch` | Nhóm testcase trong một bài nộp, xử lý theo thứ tự với fail-fast. **Không còn là đơn vị gửi cùng lúc sang engine** — F4 gọi `JudgeExecutionPort` từng testcase một (`DEC-2026-0823-go-judge-default-engine`); "đợt" giờ chỉ còn là khái niệm nhóm/thứ tự xử lý, không phải hình dạng request | "chunk", "group" |
| **Dừng sớm** | `fail_fast` | Ngừng gọi engine cho các testcase sau khi một testcase trước đã sai hoặc lỗi theo điều kiện dừng | "early exit", "short circuit" |
| **Token engine (đặc thù adapter bất đồng bộ)** | `engine_run_token` | Định danh do một engine bất đồng bộ (ví dụ Judge0) cấp cho một lần chạy — chỉ tồn tại nếu `Judge0Adapter` được bật; adapter mặc định (go-judge) không có khái niệm này vì gọi đồng bộ | "job id", "run id" |
| **Bí mật callback (đặc thù adapter bất đồng bộ)** | `callback_secret` | Token bí mật riêng theo từng bài nộp, dùng xác thực webhook — chỉ cần khi có adapter bất đồng bộ | "api key", "webhook token" |
| **Chống trùng** | `idempotency` | Consumer/adapter xử lý cùng một message hoặc cùng một callback nhiều lần vẫn cho ra cùng một kết quả, không ghi đè trạng thái cuối | "dedup" |
| **Timeout sweep** | `stuck_submission_sweep` | Job quét bài nộp treo quá ngưỡng thời gian — bản nhẹ của "job đối soát" cũ; với adapter đồng bộ, nguyên nhân treo chủ yếu là worker crash (RabbitMQ tự redeliver), không cần chủ động hỏi lại một hệ ngoài như khi dùng Judge0 | "reconciliation job" (tên cũ, dùng khi có adapter bất đồng bộ), "cron", "sync job" |
| ~~**Chấm lại**~~ | ~~`rejudge`~~ | **NGOÀI PHẠM VI (2026-08-28)** — `DEC-2026-0828-remove-rejudge-scope`. Không có cơ chế chấm lại bài nộp cũ; testcase sai thì học viên báo giảng viên, giảng viên tự sửa và tăng phiên bản bộ testcase (F2-09), lượt nộp cũ giữ nguyên kết quả cũ. Giữ dòng này để người đọc gặp chữ "chấm lại" trong tài liệu cũ biết nó đã bị bỏ | "regrade", "retry" |
| **Hệ máy chấm** | `judge engine` (mặc định: go-judge) | Dịch vụ ngoài, không phải module trong hệ thống. Judge0 vẫn là một adapter thay thế hợp lệ, không phải engine mặc định | "Judge0" (tên riêng của một adapter cụ thể, không phải tên chung), "sandbox" (sandbox là `go-sandbox`/`isolate`, tầng bên dưới engine) |

---

## 4. Phân hệ AI (`ai-review` — F5)

| Tiếng Việt | Trong code | Nghĩa | Không gọi là |
| :--- | :--- | :--- | :--- |
| **Phân tích bài giải** | `solution_review` | F5.1 — phân tích một lượt, trả JSON có cấu trúc | "AI review" một mình, "feedback", "study pack" |
| **Phỏng vấn giả lập** | `mock_interview` | F5.2 — phiên hội thoại nhiều lượt | "AI chat", "interview" một mình (dễ lẫn với F6) |
| **Phiên phỏng vấn** | `interview_session` | Một phiên F5.2 từ khi mở tới khi có rubric | "conversation", "chat" |
| **Lượt** | `interview_turn` | Một cặp hỏi và đáp trong phiên | "message", "round" |
| **Giai đoạn** | `stage` | Ba giai đoạn của phiên: `EXPLAIN` · `CHALLENGE` · `SCALE_UP` `[đề xuất]` | "phase", "step" |
| **Bảng đánh giá** | `rubric` | Bảng tiêu chí năng lực xuất khi kết phiên. **Không phải điểm số chính thức** | "score", "grade", "điểm" |
| **Điểm tiêu chí** | `rubric_score` | Điểm và nhận xét cho một tiêu chí | "rating" |
| **Mẫu prompt** | `prompt_template` | Chỉ thị hệ thống có tham số. Mã và câu trả lời người dùng vào đây dưới dạng **tham số dữ liệu** | "prompt" một mình |
| **Hash mã nguồn** | `source_hash` | Khoá cache kết quả F5.1 | "checksum", "digest" |
| **Lượng token đã dùng** | `token_usage` | Token tiêu thụ theo phiên, dùng cho kiểm soát chi phí | "cost", "usage" một mình |
| **Suy giảm có kiểm soát** | `graceful degradation` | AI hỏng thì F1-F4 vẫn chạy | "fallback" một mình |

---

## 5. Ngân hàng câu hỏi (`interview-bank` — F6)

| Tiếng Việt | Trong code | Nghĩa | Không gọi là |
| :--- | :--- | :--- | :--- |
| **Câu hỏi phỏng vấn** | `interview_question` | Câu hỏi lý thuyết, độc lập với bài nộp code | "question" một mình, "problem" |
| **Chế độ học** | `STUDY` | Xem gợi ý, khung trả lời, từ khoá cốt lõi | "learn mode", "review mode" |
| **Chế độ luyện** | `PRACTICE` | Người dùng tự soạn câu trả lời, AI đối chiếu | "test mode", "quiz" |
| **Tiêu chí trả lời** | `answer_rubric` | Chuẩn để AI đối chiếu câu trả lời | "answer key", "đáp án" |
| **Câu trả lời của người dùng** | `user_answer` | Nội dung người dùng nhập. **Là dữ liệu, không phải chỉ thị** | "response" |
| **Đánh dấu** | `bookmark` | Câu hỏi được lưu để xem lại | "favorite", "save" |
| **Bộ câu hỏi** | `question_set` | Nhóm câu hỏi do giảng viên tạo và gán cho lớp | "collection", "deck" |

---

## 6. Người dùng và phân quyền (`identity` — F1)

| Tiếng Việt | Trong code | Nghĩa | Không gọi là |
| :--- | :--- | :--- | :--- |
| **Người dùng** | `user` | Một tài khoản trong hệ thống | "account", "member" |
| **Vai trò** | `role` | `STUDENT` · `INSTRUCTOR` · `ADMIN` | "permission" (permission là quyền cụ thể, role là nhóm quyền) |
| **Người học** | `STUDENT` | Actor A1 — giải bài, nộp bài, dùng AI, xem tiến độ | "learner", "player" |
| **Giảng viên** | `INSTRUCTOR` | Actor A2 — soạn đề, tải testcase, giao bài theo lớp, quản trị nội dung kho câu hỏi phỏng vấn **dùng chung** (F6-13). Sửa 2026-09-03: trước ghi "tạo bộ câu hỏi", dễ hiểu thành bộ câu hỏi riêng theo lớp (F6-11) — F6-11 đã loại khỏi phạm vi, `DEC-2026-0828-remove-per-class-interview-set` | "teacher", "author" |
| **Quản trị viên** | `ADMIN` | Actor A3 — giám sát judge engine và hàng đợi (F4-10), cấu hình ngôn ngữ và giới hạn tài nguyên (F4-11), cấu hình prompt và rubric (F5-23), ngân sách token (F5-25), ma trận phân quyền (F1-10), quản lý tài khoản (F1-13). Sửa 2026-09-03: bỏ "chấm lại" khỏi mô tả vai trò, `DEC-2026-0828-remove-rejudge-scope` | "root", "superuser" |
| **Access Token** | `access_token` | JWT thời hạn ngắn, đi trong header `Authorization`, giữ trong bộ nhớ ở client | "token" một mình |
| **Refresh Token** | `refresh_token` | Thời hạn dài, nằm trong cookie HTTP-Only | "session" |
| **Tiến độ** | `user_progress` | Bài đã giải theo chủ đề, tỉ lệ chấp thuận, lịch sử phỏng vấn | "stats", "dashboard" |
| **Tỉ lệ chấp thuận** | `acceptance_rate` | Tỉ lệ bài nộp đạt `ACCEPTED` trên tổng bài nộp | "success rate", "pass rate" |
| **Lớp học** | `class_enrollment` | Quan hệ người học với lớp của một giảng viên | "course", "group" |

---

## 7. Từ vựng kiến trúc

Dùng đúng những từ này khi nói về mã nguồn; nguyên lý ở `overview.md` mục 1.B và 1.C.

| Tiếng Việt | Trong code | Nghĩa |
| :--- | :--- | :--- |
| **Bối cảnh giới hạn** | (tên module) | Một trong sáu vùng nghiệp vụ; một Maven module, một schema |
| **Gốc tập hợp** | (class trong `domain/model`) | Thực thể chủ giữ bất biến nghiệp vụ của cụm |
| **Đối tượng giá trị** | (class trong `domain/model`) | Bất biến, không định danh, so sánh theo giá trị |
| **Sự kiện miền** | `...Event` | Việc đã xảy ra, phát cho module khác nghe |
| **Cổng vào** | `application/ports/in`, `...UseCase` / `...Query` | Bề mặt gọi được của một module. Một interface một phương thức |
| **Cổng ra** | `domain/ports/out` (ghi) · `application/ports/out` (đọc) | Thứ module cần từ bên ngoài |
| **Handler lệnh** | `...CommandHandler` | Làm đổi trạng thái, đi qua Aggregate, mở transaction |
| **Handler truy vấn** | `...QueryHandler` | Chỉ đọc, không đi qua Aggregate, trả read model |
| **Read model** | `...View` / `...Summary` `[đề xuất]` | Hình dạng dữ liệu phẳng đúng nhu cầu một màn |
| **Adapter** | `infrastructure/...` | Hiện thực một cổng bằng công nghệ cụ thể |
| **Lớp chống ăn mòn** | (`GoJudgeAdapter` hiện thực `JudgeExecutionPort`; `Judge0Adapter` nếu cắm lại; client AI) | Cửa duy nhất ra một hệ ngoài; dịch mô hình của nó sang từ vựng AlgoPrep — cổng phải giữ trung lập theo engine (`DEC-2026-0823-go-judge-default-engine`) |

---

## 8. Ba từ đã bị dùng cho nghĩa khác — đừng dùng lẫn

| Từ | Nghĩa đã chiếm | Nếu muốn nói cái kia thì dùng |
| :--- | :--- | :--- |
| **`interview`** | Đã dùng cho **cả** F5.2 (phỏng vấn giả lập) và F6 (ngân hàng câu hỏi) | Luôn nói đủ: `mock_interview` cho F5.2, `interview_question` cho F6. Đứng một mình là mơ hồ |
| **`review`** | `solution_review` là F5.1 | Review tài liệu hoặc review mã của con người thì gọi `code_review` / bản ghi trong `07-review/` |
| **`test`** | `testcase` là dữ liệu chấm bài | Test tự động của lập trình viên gọi `unit test` / `e2e test`, không gọi tắt `test` |
