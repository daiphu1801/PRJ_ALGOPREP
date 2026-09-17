# BD — Database module `judge-orchestration` (F4)

> Schema PostgreSQL: `judge`. Đọc cùng `02-bd/architecture/judge-orchestration.md` trước — đặc biệt mục 4
> (state machine), mục 5 (idempotency), mục 6 (Outbox).

## 1. Bảng chính

### 1.1. `submissions`

Cốt lõi F4-01 → F4-04, F4-13.

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `user_id` | tham chiếu `identity.users.id`, không FK vật lý xuyên schema | |
| `problem_id` | tham chiếu `problem.problems.id`, không FK vật lý xuyên schema | |
| `problem_spec_id` | tham chiếu `problem.problem_specs.id`, không FK vật lý | Chốt tại thời điểm bắt đầu chấm — dùng để `harness` sinh đúng bản chữ ký hàm đang hiệu lực |
| `language` | ENUM(`JAVA`,`CPP`,`PYTHON`) | Khoá cứng ba ngôn ngữ (`README.md` mục "Submission languages") |
| `submission_model` | ENUM(`FUNCTION_WRAPPER`,`STANDARD_IO`) | F3-13, `DEC-2026-0824-dual-submission-model-per-problem` — học viên chọn mỗi lần nộp |
| `source_code` | TEXT | Mã nguồn bài nộp (xem `02-bd/architecture/judge-orchestration.md` mục 7 — không cần MinIO) |
| `source_input_method` | ENUM(`EDITOR`,`FILE_UPLOAD`) | F4-01 amendment — chỉ ghi nhận nguồn gốc, không đổi luồng xử lý |
| `status` | ENUM(`PENDING`,`COMPILING`,`JUDGING`,`ACCEPTED`,`WRONG_ANSWER`,`TIME_LIMIT_EXCEEDED`,`RUNTIME_ERROR`,`COMPILE_ERROR`,`SYSTEM_ERROR`) | State machine đầy đủ — `02-bd/architecture/judge-orchestration.md` mục 4, `DEC-2026-0912-judge-callback-contract` |
| `failure_reason` | ENUM(`WORKER_TIMEOUT`,`SANDBOX_FAULT`) nullable | Chỉ có giá trị khi `status = SYSTEM_ERROR`; phân biệt sweep-timeout với lỗi sandbox tức thời |
| `compile_error_message` | TEXT nullable | Chỉ có giá trị khi `status = COMPILE_ERROR`; đã qua bước ánh xạ dòng của `harness` (F3-11) |
| `passed_testcase_count` | INT default 0 | Đếm sau khi `JUDGING` hoàn tất — dùng tính F4-13 |
| `total_testcase_count` | INT | Snapshot tổng số testcase Hidden tại thời điểm bắt đầu chấm |
| `score_ratio` | DECIMAL(4,3) nullable | F4-13 = `passed_testcase_count / total_testcase_count`, tính khi vào trạng thái cuối (trừ `COMPILE_ERROR`/`SYSTEM_ERROR` — để `NULL`, không phải `0`, để phân biệt "chưa chấm được testcase nào" với "chấm hết, rớt hết") |
| `runtime_ms` | INT nullable | `MAX(submission_testcase_results.runtime_ms)` trong số testcase đã chạy — dùng cho F4-12 "Beats" |
| `memory_kb` | INT nullable | `MAX(submission_testcase_results.memory_kb)` |
| `testcase_set_version_at_grading` | INT | Snapshot `problems.current_testcase_set_version` tại thời điểm bắt đầu `COMPILING` (mục 3.4 kiến trúc) — trả lời F2-09 "chấm theo version nào", **không có cơ chế phục dựng nội dung version cũ** vì re-judge đã bị cắt (`DEC-2026-0828-remove-rejudge-scope`) |
| `submitted_at` | TIMESTAMPTZ | Mốc F4-01 ghi `PENDING` |
| `started_grading_at` | TIMESTAMPTZ nullable | Mốc worker nhận job, chuyển `COMPILING` |
| `finished_at` | TIMESTAMPTZ nullable | Mốc vào một trong bảy trạng thái cuối |
| `updated_at` | TIMESTAMPTZ | Cập nhật ở **mọi** lần đổi trạng thái — cột này job sweep dùng để tính "treo quá 5 phút" (mục 5.3 kiến trúc) |

`[SoT: Suy luận]` — `user_id`/`problem_id`/`problem_spec_id` tham chiếu chéo schema chỉ bằng giá trị, không
FK vật lý, đúng nguyên tắc modular monolith (mỗi schema độc lập migration, giống cách `problem-bank` đã
làm với `updated_by` — `02-bd/database/problem-bank.md` mục 1.1).

### 1.2. `submission_testcase_results` (F4-03, F3-07→10)

Một dòng cho **mỗi** testcase đã chạy — kể cả khi không dừng sớm, nên với một submission chạy hết N
testcase, luôn có đúng N dòng (trừ khi dừng ở `COMPILE_ERROR`/`SYSTEM_ERROR` trước khi vào `JUDGING`, khi
đó bảng này rỗng cho submission đó).

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `submission_id` | FK → `submissions.id` | |
| `testcase_id` | tham chiếu `problem.testcases.id`, không FK vật lý | |
| `testcase_order` | INT | Thứ tự chạy trong submission này — dùng xác định "testcase thất bại đầu tiên" cho verdict tổng hợp (kiến trúc mục 4) |
| `verdict` | ENUM(`AC`,`WA`,`TLE`,`MLE`,`RE`) | Kết quả so khớp của `harness` (F3-07→10), module này chỉ ghi lại, không tự tính |
| `runtime_ms` | INT | |
| `memory_kb` | INT | |
| `stderr_snippet` | TEXT nullable, cắt tối đa 4096 ký tự | Chỉ ghi khi `language_configs.return_stderr_to_student = true` **và** `verdict != AC` |
| `judge_run_token_id` | FK → `judge_run_tokens.id` | Liên kết idempotency — kiến trúc mục 5.1 |
| `created_at` | TIMESTAMPTZ | |

Unique `(submission_id, testcase_id)` — đúng một kết quả mỗi testcase mỗi submission, và là ràng buộc DB
tầng cuối chống ghi trùng nếu logic idempotency ở tầng ứng dụng có sơ suất.

### 1.3. `judge_run_tokens` (idempotency, kiến trúc mục 5.1)

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `submission_id` | FK → `submissions.id` | |
| `testcase_id` | tham chiếu `problem.testcases.id`, không FK vật lý | |
| `token` | UUID unique | Phát hành trước mỗi lần gọi `JudgeExecutionPort.run()`; cũng là bí mật webhook nếu `Judge0Adapter` kích hoạt |
| `issued_at` | TIMESTAMPTZ | |
| `consumed_at` | TIMESTAMPTZ nullable | Set trong cùng giao dịch ghi `submission_testcase_results` — `NULL` nghĩa là lời gọi đang treo hoặc đã mất do worker crash |

Unique `(submission_id, testcase_id, issued_at)` — cho phép một cặp `(submission, testcase)` có nhiều
token theo thời gian nếu phải phát hành lại (ví dụ sau `SYSTEM_ERROR` không áp dụng vì đó là trạng thái
cuối của submission, không phải retry per-testcase — trong thực tế mỗi cặp chỉ có đúng một token thành
công; trường số nhiều chỉ để không chặn cứng ở tầng DB nếu tầng ứng dụng cần phát lại).

### 1.4. `callback_logs` (adapter-internal, chỉ có dữ liệu khi `Judge0Adapter` hoạt động)

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `judge_run_token_id` | FK → `judge_run_tokens.id` nullable | `NULL` nếu token trong callback không khớp bất kỳ token nào đã phát hành (khả nghi/tấn công) |
| `raw_payload` | JSONB | Nguyên văn body callback nhận được — phục vụ audit/debug |
| `received_at` | TIMESTAMPTZ | |
| `outcome` | ENUM(`ACCEPTED`,`DUPLICATE_IGNORED`,`INVALID_TOKEN`,`STATE_ALREADY_FINAL`) | Kết quả xử lý callback đó |

Với go-judge mặc định, bảng này **luôn rỗng** — không phải bảng thừa (kiến trúc mục 5.2).

### 1.5. `language_configs` (F4-11)

Một dòng cố định cho mỗi ngôn ngữ trong ba ngôn ngữ khoá cứng — seed sẵn, Admin (A3) chỉ **sửa giá trị**,
không tạo/xoá dòng (khớp phạm vi đã khoá ở `02-bd/architecture/harness.md` mục 1, 3).

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `language` | ENUM(`JAVA`,`CPP`,`PYTHON`) PK | |
| `time_limit_multiplier` | DECIMAL(3,2) | F2-10 — nhân với `problems.time_limit_ms` của từng bài. Đề xuất mặc định `[SoT: Suy luận]`: Java `2.00`, C++ `1.00`, Python `3.00` — Java/Python chậm hơn C++ đáng kể trên cùng thuật toán, số cụ thể chủ dự án xác nhận hoặc chỉnh |
| `memory_limit_multiplier` | DECIMAL(3,2) default `1.00` | Tương tự cho bộ nhớ — RD không nhấn mạnh chênh lệch bộ nhớ giữa ba ngôn ngữ bằng thời gian, mặc định `1.00` cho cả ba, chủ dự án chỉnh nếu cần |
| `network_access_enabled` | BOOLEAN default `false` | Tham số sandbox 1/3 (F4-11, `DEC-2026-0831-judge-orchestration-ops-details`) — mặc định chặn |
| `max_child_processes` | INT default `1` | Tham số sandbox 2/3 — `[SoT: Suy luận]` số cụ thể, đa số bài không cần spawn tiến trình con, chủ dự án xác nhận |
| `return_stderr_to_student` | BOOLEAN default `false` | Tham số sandbox 3/3 |
| `updated_at` / `updated_by` | TIMESTAMPTZ / tham chiếu user id | `updated_by` không FK vật lý, giống quy ước `problems.updated_by` |

### 1.6. `outbox_events` (kiến trúc mục 6, `DEC-2026-0912-judge-outbox-pattern`)

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `aggregate_type` | VARCHAR | Cố định `"Submission"` cho module này |
| `aggregate_id` | UUID | = `submissions.id` |
| `event_type` | VARCHAR | `"SubmissionAccepted"` hoặc `"SubmissionGraded"` |
| `payload` | JSONB | Nội dung sự kiện — xem bảng payload ở kiến trúc mục 6 |
| `created_at` | TIMESTAMPTZ | Ghi trong cùng giao dịch với `submissions.status` đổi sang trạng thái cuối |
| `published_at` | TIMESTAMPTZ nullable | `NULL` = relay chưa publish thành công; job relay chỉ set sau khi broker ack |
| `publish_attempts` | INT default 0 | Đếm số lần relay thử publish dòng này — phục vụ cảnh báo nếu một dòng kẹt quá nhiều lần |

Index `outbox_events(published_at) WHERE published_at IS NULL` — relay quét theo `created_at ASC` trong
tập con này.

## 2. Chỉ mục (index) đáng chú ý

- `submissions(status, updated_at)` — job sweep quét theo điều kiện mục 1.1.
- `submissions(user_id, problem_id, created_at DESC)` — lịch sử nộp bài của một học viên cho một bài.
- `submissions(problem_id, language, status) WHERE status = 'ACCEPTED'` — phục vụ F4-12 "Beats" (so runtime
  giữa các bài nộp Accepted cùng bài + cùng ngôn ngữ).
- `submission_testcase_results(submission_id, testcase_order)` — dựng lại kết quả theo đúng thứ tự chạy.
- `judge_run_tokens(submission_id, testcase_id)` — tra idempotency trước mỗi lần gọi port (mục 5.1 kiến
  trúc).

## 3. Redis

`judge-orchestration` **không dùng Redis** — `dependency-map.md` mục 2 không liệt kê Redis cho module này
(khác `identity`/`problem-bank`). Trạng thái nộp bài đẩy trực tiếp qua WebSocket/STOMP, không cần cache
trung gian; hàng đợi công việc đã có RabbitMQ đảm nhiệm.

## 4. Migration — thứ tự tạo bảng

`language_configs` (seed 3 dòng) → `submissions` → `submission_testcase_results` → `judge_run_tokens` →
`callback_logs` → `outbox_events`.

## 5. Giá trị mặc định BD chốt (RD để ngỏ, `[SoT: Suy luận]`)

| Tham số | Giá trị | Nguồn RD |
| :--- | :--- | :--- |
| Ngưỡng sweep timeout | 5 phút | `01-rd/req/judge-orchestration.md` dòng 41-43 (RD đã chốt số) |
| Chu kỳ chạy job sweep | 60 giây | RD chỉ nói "bản nhẹ", không nêu chu kỳ — BD đề xuất |
| Giới hạn cắt `stderr_snippet` | 4096 ký tự | RD không nêu, BD đề xuất để tránh phình bảng khi chương trình học viên in log dài |
| `time_limit_multiplier` mặc định | Java `2.00`, C++ `1.00`, Python `3.00` | RD chỉ xác nhận có hệ số (F2-10), không nêu số — BD đề xuất |
| `max_child_processes` mặc định | `1` | RD chỉ xác nhận có tham số này (F4-11), không nêu số — BD đề xuất |
| Làm tròn F4-12 "Beats" | Số nguyên gần nhất | Xem kiến trúc mục 8 |

## 6. Việc còn mở — chuyển sang DD

- Cấu trúc JSON đầy đủ của `outbox_events.payload` cho từng `event_type`.
- Số cụ thể của mọi hàng ở mục 5 cần chủ dự án xác nhận trước khi seed migration thật.
- `ProblemGradingSpecPort` (kiến trúc mục 3.2) — DTO trả về khi đọc đặc tả bài từ `problem-bank`.
- Cơ chế đánh dấu "một dòng outbox kẹt quá nhiều lần" (`publish_attempts` vượt ngưỡng nào thì cảnh báo A3
  qua `admin_queue_monitor`, F4-10) — chưa có số cụ thể.

## 7. Tham chiếu

- `02-bd/architecture/judge-orchestration.md` — kiến trúc, state machine, callback contract, Outbox.
- `02-bd/database/problem-bank.md` mục 1.1, 1.6 — `current_testcase_set_version`,
  `TestcaseSetVersionBumped`.
- `.nexa/domain-registry.json` — domain `judge-orchestration`, scope: `submission`,
  `submission_testcase_result`, `judge_run_token`, `callback_log`, `language_config`.
- `.nexa/control/decision-registry.md` — `DEC-2026-0912-judge-callback-contract`,
  `DEC-2026-0912-judge-outbox-pattern`.
