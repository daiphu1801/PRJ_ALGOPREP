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
| `stderr_snippet` | TEXT nullable, cắt tối đa 4096 ký tự | Chỉ ghi khi `sandbox_settings.return_stderr_to_student = true` **và** `verdict != AC` |
| `actual_output_snippet` | TEXT nullable, cắt tối đa 4096 ký tự (cùng giới hạn `stderr_snippet`) | **Cột MỚI, 2026-10-18, `DEC-2026-1018-usr-open-questions-phase2-5`.** Output thực tế của chương trình học viên; **chỉ ghi khi `problem.testcases.visibility = SAMPLE`**, testcase `HIDDEN` luôn `NULL` (không lộ dữ liệu ẩn). Dùng cho màn kết quả nộp bài khi so với output mong đợi của testcase mẫu |
| `judge_run_token_id` | FK → `judge_run_tokens.id` | Liên kết idempotency — kiến trúc mục 5.1 |
| `created_at` | TIMESTAMPTZ | |

**Quy tắc lưu giữ (2026-10-18, `DEC-2026-1018-usr-open-questions-phase2-5`):** dòng `submission_testcase_results`
được giữ **90 ngày** tính từ `created_at`; sau đó chỉ còn verdict tổng và các số thống kê nằm trên `submissions`
(`status`, `passed_testcase_count`, `total_testcase_count`, `score_ratio`, `runtime_ms`, `memory_kb`). Bản thân
`submissions` và `source_code` không bị xoá, và một lượt nộp không bao giờ bị xoá hay ẩn. Chi tiết job dọn thuộc
`03-dd/jobs` (không thiết kế ở đây).

**Múi giờ cắt ngày (2026-10-18, cùng quyết định):** mọi chỗ "ngày" của module này dùng một múi giờ hệ thống cố
định **UTC+7** (giờ Việt Nam); cột thời gian vẫn lưu `TIMESTAMPTZ`, chỉ khi cắt theo ngày mới quy đổi sang UTC+7.

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
| `memory_limit_multiplier` | DECIMAL(3,2) default `1.00` | Tương tự cho bộ nhớ. Seed `[SoT: Suy luận]`: Java `2.00` (JVM cần nhiều bộ nhớ hơn trên cùng thuật toán), C++ `1.00`, Python `1.00`; chủ dự án chỉnh được trên `ADM0501` (chốt 2026-10-10, `DEC-2026-1010-admin-operations-regroup`, đóng Q7 của BD `ADM0501`). Cùng khoảng hợp lệ với hệ số thời gian: 0,10 đến 9,99 |
| `enabled` | BOOLEAN default `true` | Ngôn ngữ có được chọn ở `problem_detail` cho lượt nộp **mới** hay không (thêm 2026-10-10, đóng Q1 của BD `ADM0501`). Tắt ngôn ngữ **không** huỷ job đang chờ của ngôn ngữ đó: chúng chạy nốt, chỉ bài nộp mới bị từ chối. Ràng buộc ở tầng ứng dụng: luôn còn ít nhất một dòng `enabled = true` |
| `updated_at` / `updated_by` | TIMESTAMPTZ / tham chiếu user id | `updated_by` không FK vật lý, giống quy ước `problems.updated_by` |

> [!NOTE]
> **Ba tham số sandbox không còn nằm ở bảng này** (`network_access_enabled`, `max_child_processes`, `return_stderr_to_student`). Chúng là thuộc tính của lệnh chạy chứ không của ngôn ngữ nên đã được áp ở cấp toàn hệ thống (`DEC-2026-0922-users-and-admin-conflict-resolutions`); từ 2026-10-10 chúng chuyển sang bảng một dòng `sandbox_settings` (mục 1.10, `DEC-2026-1010-admin-operations-regroup`, đóng Q4 của BD `ADM0501`). Migration: bỏ ba cột khỏi `language_configs` và seed `sandbox_settings` từ giá trị mặc định của ba cột cũ.

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

### 1.7. `cluster_settings` (F4-10, `DEC-2026-1001-admin-configurable-settings`)

Một dòng duy nhất (ràng buộc `CHECK (id = 1)`), chỉ ADMIN (A3) sửa qua khối "Điều khiển" và popup "Tham số hàng đợi" của
`ADM0401`. Lưu ở PostgreSQL để còn nguyên sau khi khởi động lại; module này cố ý không dùng Redis
(mục 3) nên không đặt ở đó `[SoT: Suy luận]`.

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | SMALLINT PK | Luôn `1` |
| `queue_paused` | BOOLEAN default `false` | Bật thì worker ngừng nhận job mới, job đang chấm vẫn chạy hết |
| `autoscale_enabled` | BOOLEAN default `true` | Bật thì cụm tự thêm worker khi hàng đợi vượt ngưỡng |
| `autoscale_job_threshold` | INT default `20`, CHECK `> 0` | Ngưỡng số job đang chờ để thêm worker; mặc định 20 là suy luận của BD `[SoT: Suy luận]`, ADMIN tự đổi |
| `jobs_per_page` | INT default `20`, CHECK `> 0` | Số dòng mỗi trang của bảng job `ADM0401` (thêm 2026-10-10, đóng Q7). Mặc định là suy luận của BD `[SoT: Suy luận]`, ADMIN tự đổi, không trần cứng |
| `error_retention_hours` | INT default `24`, CHECK `> 0` | Job `SYSTEM_ERROR` còn nằm trong bảng job bao lâu trước khi bị dọn khỏi danh sách (thêm 2026-10-10). Chỉ ảnh hưởng danh sách hiển thị, **không** xoá `submissions` |
| `latency_cap_seconds` | INT default `10`, CHECK `> 0` | Mẫu số tính chiều rộng thanh độ trễ (thêm 2026-10-10) |
| `updated_by` | UUID | Tham chiếu `identity.users.id`, không có FK vật lý chéo schema |
| `updated_at` | TIMESTAMPTZ | |

### 1.8. `generator_sandbox_settings` (F2-14, `DEC-2026-1003-ai-testcase-generation-quota`)

Một dòng duy nhất (`CHECK (id = 1)`), cùng khuôn `cluster_settings`, chỉ ADMIN (A3) sửa qua khối "Script sinh dữ liệu testcase" của `ADM0501` (Khu vực F). Giới hạn khi hệ thống chạy **script Python do AI viết** để sinh input testcase; riêng, không dùng chung với giới hạn bài nộp (RD `problem-bank.md:88-91`). `problem-bank` đọc hai giá trị này khi chạy `GenerateTestcasesWithAi`, qua cổng đọc của module này (không đọc thẳng bảng).

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | SMALLINT PK | Luôn `1` |
| `max_runtime_seconds` | INT default `30`, CHECK từ 1 đến 300 | Script chạy quá thì loại cả script và báo lý do. Mặc định và khoảng là suy luận của BD `[SoT: Suy luận]`, ADMIN tự đổi |
| `max_output_mb` | INT default `100`, CHECK từ 1 đến 1024 | Tổng output của script vượt thì loại cả script. Mặc định và khoảng là suy luận của BD `[SoT: Suy luận]` |
| `updated_by` | UUID | Tham chiếu `identity.users.id`, không có FK vật lý chéo schema |
| `updated_at` | TIMESTAMPTZ | |

### 1.9. `grading_defaults` (F4-11, `DEC-2026-1010-admin-operations-regroup`)

Một dòng duy nhất (`CHECK (id = 1)`), cùng khuôn `cluster_settings`, chỉ ADMIN (A3) có Function `LANGUAGE_CONFIG` sửa qua hàng "Giới hạn mặc định" của `ADM0501`. Đây là giới hạn dùng khi một bài toán không khai riêng; thay cho việc đọc từ `application.yml` (đóng Q2 của BD `ADM0501`, theo nguyên tắc `DEC-2026-1001-admin-configurable-settings`). Tách khỏi `cluster_settings` vì hai mối quan tâm khác nhau (giới hạn chấm và điều khiển cụm).

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | SMALLINT PK | Luôn `1` |
| `time_limit_ms` | INT default `1000`, CHECK `> 0` | Giới hạn thời gian mặc định; hiệu dụng của một ngôn ngữ = giá trị này nhân `time_limit_multiplier`, phải nằm trong 100 đến 30.000 ms |
| `memory_limit_mb` | INT default `256`, CHECK `> 0` | Giới hạn bộ nhớ mặc định; hiệu dụng = giá trị này nhân `memory_limit_multiplier`, phải nằm trong 64 đến 1.024 MB |
| `output_limit_kb` | INT default `64`, CHECK `> 0` | Kích thước output mặc định. Khác `problems.max_output_size_kb` khai theo từng bài |
| `compile_timeout_sec` | INT default `10`, CHECK `> 0` | Thời gian biên dịch tối đa, không tính vào giới hạn chạy |
| `updated_by` | UUID | Tham chiếu `identity.users.id`, không có FK vật lý chéo schema |
| `updated_at` | TIMESTAMPTZ | |

Giá trị mặc định là số của prototype `[SoT: Suy luận]`, ADMIN tự đổi. Áp dụng cho job từ lúc **bắt đầu chấm**, không phải lúc nộp (xem mục 1.10).

### 1.10. `sandbox_settings` (F4-11, `DEC-2026-1010-admin-operations-regroup`)

Một dòng duy nhất (`CHECK (id = 1)`), chỉ ADMIN sửa qua thẻ "Sandbox" của `ADM0501` (tab "Sandbox và sinh testcase"). Thay ba cột sandbox từng nằm ở `language_configs` (mục 1.5): ba tham số này là thuộc tính của **lệnh chạy** (cgroup và namespace của go-sandbox không đọc `language`), nên toàn cục (đóng Q4 của BD `ADM0501`).

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | SMALLINT PK | Luôn `1` |
| `network_access_enabled` | BOOLEAN default `false` | Mặc định chặn (F4-11) |
| `child_process_limit_enabled` | BOOLEAN default `true` | Bật thì áp `max_child_processes`; tắt thì không giới hạn tiến trình con |
| `max_child_processes` | INT default `60`, CHECK `> 0` | Số tiến trình con tối đa khi công tắc bật. Mặc định 60 là số prototype `[SoT: Suy luận]`; UI là công tắc kèm một ô số khi bật |
| `return_stderr_to_student` | BOOLEAN default `true` | Quyết định có ghi `submission_testcase_results.stderr_snippet` hay không |
| `updated_by` | UUID | Tham chiếu `identity.users.id`, không có FK vật lý chéo schema |
| `updated_at` | TIMESTAMPTZ | |

**Quy tắc thời điểm áp dụng (G3).** Một thay đổi ở `language_configs`, `grading_defaults` hoặc `sandbox_settings` có hiệu lực với một job từ lúc worker **bắt đầu chấm** nó, không phải lúc bài được nộp; job đã bắt đầu chấm thì chạy nốt với cấu hình nó đã đọc. Cấu hình đã dùng nên được ghi cùng kết quả chấm để truy vết `[SoT: Suy luận]`, chốt ở DD.

## 2. Chỉ mục (index) đáng chú ý

- `submissions(status, updated_at)` — job sweep quét theo điều kiện mục 1.1.
- `submissions(user_id, problem_id, submitted_at DESC)` — lịch sử nộp bài của một học viên cho một bài. ~~Cột cũ ghi `created_at`~~: bảng không có `created_at`, mốc nộp là `submitted_at` (dòng 31); sửa 2026-10-14 (`DEC-2026-1014-instructor-submissions-list-details`). Chỉ mục `submissions(status, updated_at)` ở dòng trên đúng với cột `updated_at` của bảng (dòng 34), không đổi.
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

`language_configs` (seed 3 dòng), `grading_defaults` và `sandbox_settings` (seed 1 dòng mỗi bảng) → `submissions` → `submission_testcase_results` → `judge_run_tokens` →
`callback_logs` → `outbox_events` → `cluster_settings` (seed 1 dòng).

## 5. Giá trị mặc định BD chốt (RD để ngỏ, `[SoT: Suy luận]`)

| Tham số | Giá trị | Nguồn RD |
| :--- | :--- | :--- |
| Ngưỡng sweep timeout | 5 phút | `01-rd/req/judge-orchestration.md` dòng 41-43 (RD đã chốt số) |
| Chu kỳ chạy job sweep | 60 giây | RD chỉ nói "bản nhẹ", không nêu chu kỳ — BD đề xuất |
| Giới hạn cắt `stderr_snippet` | 4096 ký tự | RD không nêu, BD đề xuất để tránh phình bảng khi chương trình học viên in log dài |
| `time_limit_multiplier` mặc định | Java `2.00`, C++ `1.00`, Python `3.00` | RD chỉ xác nhận có hệ số (F2-10), không nêu số — BD đề xuất |
| `max_child_processes` mặc định | `60` (chỉ áp khi công tắc giới hạn bật) | RD chỉ xác nhận có tham số này (F4-11), không nêu số — đổi từ `1` sang `60` ngày 2026-10-10 theo số prototype, ADMIN tự đổi (mục 1.10) |
| `memory_limit_multiplier` mặc định | Java `2.00`, C++ `1.00`, Python `1.00` | Sửa 2026-10-10 (trước đó `1.00` cho cả ba), mục 1.5 |
| Làm tròn F4-12 "Beats" | Số nguyên gần nhất | Xem kiến trúc mục 8 |

## 6. Việc còn mở — chuyển sang DD

- Cấu trúc JSON đầy đủ của `outbox_events.payload` cho từng `event_type`.
- Số cụ thể của mọi hàng ở mục 5 cần chủ dự án xác nhận trước khi seed migration thật.
- `ProblemGradingSpecPort` (kiến trúc mục 3.2) — DTO trả về khi đọc đặc tả bài từ `problem-bank`.
- ~~Cơ chế đánh dấu "một dòng outbox kẹt quá nhiều lần" (`publish_attempts` vượt ngưỡng nào thì cảnh báo A3 qua `admin_queue_monitor`, F4-10) — chưa có số cụ thể.~~ **Đã chốt 2026-10-10** (`DEC-2026-1010-admin-operations-regroup`, tiêu chí ponytail bậc 5): hằng số trong `application.yml`, từ 3 lần thử là `WARN`, từ 5 lần là `ERROR`; hiển thị ở `ADM0404` và thẻ liên kết của `ADM0401`, không thêm cột hay bảng.

## 7. Tham chiếu

- `02-bd/architecture/judge-orchestration.md` — kiến trúc, state machine, callback contract, Outbox.
- `02-bd/database/problem-bank.md` mục 1.1, 1.6 — `current_testcase_set_version`,
  `TestcaseSetVersionBumped`.
- `.nexa/domain-registry.json` — domain `judge-orchestration`, scope: `submission`,
  `submission_testcase_result`, `judge_run_token`, `callback_log`, `language_config`.
- `.nexa/control/decision-registry.md` — `DEC-2026-0912-judge-callback-contract`,
  `DEC-2026-0912-judge-outbox-pattern`.
