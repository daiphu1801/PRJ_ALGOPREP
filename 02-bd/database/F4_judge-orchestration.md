# BD — Database module `judge-orchestration` (F4)

> Schema PostgreSQL: `judge`. Đọc cùng `02-bd/architecture/F4_judge-orchestration.md` trước — đặc biệt mục 4
> (state machine), mục 5 (idempotency), mục 6 (Outbox).
>
> Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4): quy ước xoá của file này: thực thể gốc `submissions` **không bao giờ bị xoá cứng** ("một lượt nộp không bao giờ bị xoá hay ẩn", mục 1.2), nên mọi FK nội schema `judge` (`submission_testcase_results`, `judge_run_tokens` → `submissions`; `submission_testcase_results`, `callback_logs` → `judge_run_tokens`) là `NO ACTION`, không `ON DELETE CASCADE`. Ngoại lệ về dữ liệu: dòng lá `submission_testcase_results` bị job lưu giữ xoá sau 90 ngày và dòng `outbox_events` bị dọn sau 90 ngày (mục 1.2, 1.6), cả hai là bảng lá nên không ảnh hưởng FK. `[SoT: Suy luận]` cho câu tổng quát này.

## 1. Bảng chính

**Quy ước kiểu cột tham chiếu (bổ sung 2026-10-23, `DEC-2026-1023-db-actor-review-fixes`):** mọi cột trỏ tới một dòng của bảng khác, kể cả cột chéo schema không có khoá ngoại vật lý (`user_id`, `problem_id`, `testcase_id`, `created_by`, `updated_by`, `assigned_by`, `class_id`, `submission_id`...), có kiểu **UUID**, trùng kiểu khoá chính của bảng đích. Ô "Kiểu" bỏ trống hoặc chỉ ghi "tham chiếu" ở các bảng dưới đây đọc là UUID. Cột `NOT NULL` hay `nullable` vẫn theo ô Ghi chú của từng cột.

### 1.1. `submissions`

Cốt lõi F4-01 → F4-04, F4-13.

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `user_id` | tham chiếu `identity.users.id`, không FK vật lý xuyên schema | |
| `problem_id` | tham chiếu `problem.problems.id`, không FK vật lý xuyên schema | |
| `spec_version` | INT, **nullable** | Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-problem-bank-schema-sync): thay cột `problem_spec_id`. Giá trị `problem.problem_specs.spec_version` đọc được lúc bắt đầu chấm (`problem_specs` nay gộp cả chữ ký hàm, bảng `function_signatures` bỏ, `02-bd/database/F2_problem-bank.md`); `NULL` khi `status = PENDING` (phase 1: chưa biết, chỉ có giá trị từ lúc bắt đầu chấm). **Chỉ để truy vết**: một con số `spec_version` không đủ dựng lại đặc tả cũ (không có bảng lịch sử), và điều này được chấp nhận vì chấm lại đã bị cắt (`DEC-2026-0828-remove-rejudge-scope`). Không có FK, không dùng làm khoá tra cứu |
| `language` | ENUM(`JAVA`,`CPP`,`PYTHON`) | Khoá cứng ba ngôn ngữ (`README.md` mục "Submission languages") |
| `submission_model` | ENUM(`FUNCTION_WRAPPER`,`STANDARD_IO`) | F3-13, `DEC-2026-0824-dual-submission-model-per-problem` — học viên chọn mỗi lần nộp |
| `source_code` | TEXT | Mã nguồn bài nộp (xem `02-bd/architecture/F4_judge-orchestration.md` mục 7 — không cần MinIO). Sửa 2026-10-22 (đồng bộ DB, phase 1): tối đa **64 KB đo bằng byte UTF-8** (`octet_length(source_code) <= 65536`), kiểm ở tầng ứng dụng (màn hình và máy chủ, BD `USR0102` dòng 798 và Sheet 9 NO 8 dòng 832; sửa 2026-10-22, đồng bộ DB vòng 2: số dòng cũ 795 và 829 đã lệch) và có thể thêm `CHECK` ở DB; chốt số theo RD F4-01 amendment "chốt số ở BD" |
| `source_input_method` | ENUM(`EDITOR`,`FILE_UPLOAD`) | F4-01 amendment — chỉ ghi nhận nguồn gốc, không đổi luồng xử lý |
| `status` | ENUM(`PENDING`,`COMPILING`,`JUDGING`,`ACCEPTED`,`WRONG_ANSWER`,`TIME_LIMIT_EXCEEDED`,`RUNTIME_ERROR`,`COMPILE_ERROR`,`SYSTEM_ERROR`) | State machine đầy đủ — `02-bd/architecture/F4_judge-orchestration.md` mục 4, `DEC-2026-0912-judge-callback-contract` |
| `failure_reason` | ENUM(`WORKER_TIMEOUT`,`SANDBOX_FAULT`,`QUEUE_UNAVAILABLE`,`SPEC_UNAVAILABLE`) nullable | Chỉ có giá trị khi `status = SYSTEM_ERROR`; `QUEUE_UNAVAILABLE` do RabbitMQ không xác nhận; `SANDBOX_FAULT` cho lỗi go-judge/harness nội bộ; `SPEC_UNAVAILABLE` cho đặc tả hoặc bundle testcase thiếu/hỏng/không parse được sau khi đã nhận bài; `WORKER_TIMEOUT` do job sweep ghi. Cả bốn đều không tính vào mẫu số tỉ lệ AC |
| `compile_error_message` | TEXT nullable | Chỉ có giá trị khi `status = COMPILE_ERROR`; đã qua bước ánh xạ dòng của `harness` (F3-11) |
| `passed_testcase_count` | INT default 0 | Đếm sau khi `JUDGING` hoàn tất — dùng tính F4-13. Sửa 2026-10-22 (đồng bộ DB, phase 1): giữ `default 0` cho dễ ghi, nhưng **vô nghĩa cho tới khi bài vào trạng thái cuối**; đọc kèm `total_testcase_count` (`NULL` = chưa chấm) và không hiển thị `0` khi `total_testcase_count IS NULL` |
| `total_testcase_count` | INT nullable | Tổng số testcase đã duyệt (Sample và Hidden) từ bundle snapshot ghim lúc nộp; ghi khi bắt đầu `JUDGING`, `NULL` khi `PENDING` hoặc `COMPILING`. Màn `INS0206` hiển thị `NULL` bằng dấu `-`, không chia cho 0 (BD `INS0206` dòng 552) |
| `score_ratio` | DECIMAL(4,3) nullable | F4-13 = `passed_testcase_count / total_testcase_count`, tính khi vào trạng thái cuối (trừ `COMPILE_ERROR`/`SYSTEM_ERROR` — để `NULL`, không phải `0`, để phân biệt "chưa chấm được testcase nào" với "chấm hết, rớt hết") |
| `runtime_ms` | INT nullable | `MAX(submission_testcase_results.runtime_ms)` trong số testcase đã chạy — dùng cho F4-12 "Beats" |
| `memory_kb` | INT nullable | `MAX(submission_testcase_results.memory_kb)` |
| `testcase_set_version_at_grading` | INT NOT NULL | Sửa DD 2026-10-25: lấy `problems.current_testcase_set_version` **lúc nhận bài nộp**, cùng lúc kiểm bài xuất bản, rồi ghim vào dòng `PENDING`. Worker chỉ đọc bundle bất biến `problem.testcase_set_snapshots(problem_id, version)` của số này; không thay phiên bản khi vào `COMPILING`. Sửa 2026-10-29 (F4-01): bỏ nullable vì dự án greenfield, không có dòng lịch sử; xem mục 8 |
| `submitted_at` | TIMESTAMPTZ | Mốc F4-01 ghi `PENDING` |
| `started_grading_at` | TIMESTAMPTZ nullable | Mốc worker nhận job, chuyển `COMPILING` |
| `finished_at` | TIMESTAMPTZ nullable | Mốc vào một trong sáu trạng thái cuối (chín giá trị `status` trừ ba trạng thái chưa cuối `PENDING`, `COMPILING`, `JUDGING`; Sửa 2026-10-22 (đồng bộ DB, phase 1), trước đó ghi "bảy") |
| `updated_at` | TIMESTAMPTZ | Cập nhật ở **mọi** lần đổi trạng thái — cột này job sweep dùng để tính "treo quá 5 phút" (mục 5.3 kiến trúc) |

`[SoT: Suy luận]` — `user_id`/`problem_id` tham chiếu chéo schema chỉ bằng giá trị, không
FK vật lý (`spec_version` là số phiên bản chứ không phải tham chiếu, Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-problem-bank-schema-sync)), đúng nguyên tắc modular monolith (mỗi schema độc lập migration, giống cách `problem-bank` đã
làm với `updated_by` — `02-bd/database/F2_problem-bank.md` mục 1.1).

**Bảng này không lưu giới hạn hiệu dụng và không có snapshot cấu hình** (Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-ai-judge-schema-sync), JD-08, `DEC-2026-1022-ai-judge-schema-sync`, tiêu chí ponytail bậc 3): không có cột thời gian hay bộ nhớ hiệu dụng, không có cột nào chép `language_configs`, `grading_defaults` hay `sandbox_settings` tại lúc chấm. Chỉ có hai số truy vết: `spec_version` và `testcase_set_version_at_grading`.

**Chạy thử (Run Code) không tạo dòng nào ở bảng này** (Sửa 2026-10-22 (đồng bộ DB, phase 1)). Chạy thử testcase Sample là lời gọi đồng bộ, không ghi nhận tiến độ (RD F4-02, `01-rd/req/F4_judge-orchestration.md:16`); kết quả trả thẳng, không lưu, không có cột `is_trial` (BD `USR0102` Q10, chủ dự án chốt 2026-10-18, `DEC-2026-1018-usr-open-questions-phase2-5`, ponytail bậc 5). Mọi dòng `submissions` là một lượt nộp thật.

### 1.2. `submission_testcase_results` (F4-03, F3-07→10)

Một dòng cho **mỗi** testcase đã chạy — kể cả khi không dừng sớm, nên với một submission chạy hết N
testcase, luôn có đúng N dòng (trừ khi dừng ở `COMPILE_ERROR`/`SYSTEM_ERROR` trước khi vào `JUDGING`, khi
đó bảng này rỗng cho submission đó).

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `submission_id` | FK → `submissions.id` | |
| `testcase_id` | tham chiếu `problem.testcases.id`, không FK vật lý | |
| `is_sample` | BOOLEAN NOT NULL | Chụp `visibility = SAMPLE` tại lúc chấm. Khi trả dữ liệu học viên, chỉ hiện nội dung mẫu nếu cờ này **và** visibility hiện hành đều là Sample; testcase bị xoá hoặc không tra được được coi là Hidden. Bổ sung DD 2026-10-25 để testcase từng Hidden không bị lộ sau khi tác giả đổi visibility |
| `testcase_order` | INT | Thứ tự chạy trong submission này — dùng xác định "testcase thất bại đầu tiên" cho verdict tổng hợp (kiến trúc mục 4) |
| `verdict` | ENUM(`AC`,`WA`,`TLE`,`MLE`,`RE`) | Kết quả so khớp của `harness` (F3-07→10), module này chỉ ghi lại, không tự tính. Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4) (N3): `verdict` của testcase có năm giá trị còn `submissions.status` không có trạng thái riêng cho `MLE`; theo `02-bd/architecture/F4_judge-orchestration.md:165`, khi testcase thất bại đầu tiên (theo `testcase_order`) là `TLE` **hoặc `MLE`** thì `status = TIME_LIMIT_EXCEEDED` (gộp nhãn). Các ánh xạ còn lại: `WA` → `WRONG_ANSWER`, `RE` → `RUNTIME_ERROR`, toàn bộ `AC` → `ACCEPTED` (cùng sơ đồ, dòng 162-166). Loại `MLE` vẫn giữ nguyên ở dòng testcase để hiển thị chi tiết |
| `runtime_ms` | INT | |
| `memory_kb` | INT | |
| `stderr_snippet` | TEXT nullable, cắt tối đa 4096 ký tự | **Cập nhật owner 2026-10-25:** chỉ ghi cho `STANDARD_IO`, testcase `SAMPLE`, `sandbox_settings.return_stderr_to_student = true` và `verdict != AC`. `FUNCTION_WRAPPER`/Hidden luôn NULL |
| `actual_output_snippet` | TEXT nullable, cắt tối đa 4096 ký tự (cùng giới hạn `stderr_snippet`) | **Cột MỚI, 2026-10-18, `DEC-2026-1018-usr-open-questions-phase2-5`.** (Lưu tối đa 4096 ký tự; màn `USR0201` NO 8-10 chỉ hiển thị 2000 ký tự — hai con số khác nhau có chủ đích; Sửa 2026-10-22.) Output thực tế của chương trình học viên; **chỉ ghi khi `is_sample = true`**, testcase Hidden luôn `NULL` (không lộ dữ liệu ẩn). Dùng cho màn kết quả nộp bài khi so với output mong đợi của testcase mẫu |
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

Unique **một phần** `(submission_id, testcase_id) WHERE consumed_at IS NOT NULL` — đúng một token đã tiêu thụ cho mỗi cặp `(submission, testcase)`, khớp bất biến "không ghi đè kết quả đã có" ở `02-bd/architecture/F4_judge-orchestration.md` mục 5.1 (dòng 189-194). Cặp có thể có nhiều token chưa tiêu thụ (phát hành lại sau khi worker crash, `consumed_at = NULL`) nhưng tối đa một token đã tiêu thụ; lần ghi thứ hai cho cùng cặp vi phạm unique và bị từ chối ở tầng DB. Sửa 2026-10-22 (đồng bộ DB, phase 1): ràng buộc cũ `(submission_id, testcase_id, issued_at)` vô hiệu vì `issued_at` luôn khác nhau nên không chặn được gì.

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
không tạo/xoá dòng (khớp phạm vi đã khoá ở `02-bd/architecture/F3_harness.md` mục 1, 3).

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `language` | ENUM(`JAVA`,`CPP`,`PYTHON`) PK | |
| `time_limit_multiplier` | DECIMAL(3,2) | F2-10 — nhân với `problems.time_limit_ms` của từng bài. Đề xuất mặc định `[SoT: Suy luận]`: Java `2.00`, C++ `1.00`, Python `3.00` — Java/Python chậm hơn C++ đáng kể trên cùng thuật toán, số cụ thể chủ dự án xác nhận hoặc chỉnh |
| `memory_limit_multiplier` | DECIMAL(3,2) default `1.00` | Tương tự cho bộ nhớ. Seed `[SoT: Suy luận]`: Java `2.00` (JVM cần nhiều bộ nhớ hơn trên cùng thuật toán), C++ `1.00`, Python `1.00`; chủ dự án chỉnh được trên `ADM0501` (chốt 2026-10-10, `DEC-2026-1010-admin-operations-regroup`, đóng Q7 của BD `ADM0501`). Cùng khoảng hợp lệ với hệ số thời gian: 0,10 đến 9,99 |
| `enabled` | BOOLEAN default `true` | Ngôn ngữ có được chọn ở `problem_detail` cho lượt nộp **mới** hay không (thêm 2026-10-10, đóng Q1 của BD `ADM0501`). Tắt ngôn ngữ **không** huỷ job đang chờ của ngôn ngữ đó: chúng chạy nốt, chỉ bài nộp mới bị từ chối. Ràng buộc ở tầng ứng dụng: luôn còn ít nhất một dòng `enabled = true` |
| `updated_at` / `updated_by` | TIMESTAMPTZ / tham chiếu user id | `updated_by` không FK vật lý, giống quy ước `problems.updated_by` |

> [!NOTE]
> **Ba tham số sandbox không còn nằm ở bảng này** (`network_access_enabled`, `max_child_processes`, `return_stderr_to_student`). Chúng là thuộc tính của lệnh chạy chứ không của ngôn ngữ nên đã được áp ở cấp toàn hệ thống (`DEC-2026-0922-users-and-admin-conflict-resolutions`); từ 2026-10-10 chúng chuyển sang bảng một dòng `sandbox_settings` (mục 1.10, `DEC-2026-1010-admin-operations-regroup`, đóng Q4 của BD `ADM0501`). Migration: bỏ ba cột khỏi `language_configs` và seed `sandbox_settings` từ giá trị mặc định của hai cột còn lại (`network_access_enabled`, `return_stderr_to_student`) cùng `child_process_limit_enabled`; **không** tạo `max_child_processes` (Sửa 2026-10-22 (đồng bộ DB, phase 1), trần 60 là hằng số `application.yml`).

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

Index `outbox_events(created_at, id) WHERE published_at IS NULL` — relay quét theo `created_at ASC, id ASC` trong
tập con này (sửa 2026-10-29, F4-07: thay chỉ mục `published_at` cũ, vì cột đó luôn `NULL` trong tập con nên không giúp sắp xếp). Dọn dẹp dùng thêm `outbox_events(published_at) WHERE published_at IS NOT NULL`.

Sửa 2026-10-22 (đồng bộ DB vòng 2, DEC-2026-1022-db-sync-round2, mục 7): **dọn outbox** — dòng có `published_at` cũ hơn **90 ngày** bị một job xoá; số 90 là hằng số `application.yml`, không thêm cột. Dòng `published_at IS NULL` không bao giờ bị xoá. Sửa 2026-10-29 (F4-08): lịch chạy và cỡ lô đã nằm ở `03-dd/jobs/F4_judge-orchestration.md` (JOB-JD-03). Payload `SubmissionGraded` có thêm `submittedAt` và `firstAttemptForProblem` (`architecture/F4_judge-orchestration.md` mục 6); bảng này không liệt kê trường payload nên không đổi cột.

### 1.7. `cluster_settings` (F4-10, `DEC-2026-1001-admin-configurable-settings`)

Một dòng duy nhất (ràng buộc `CHECK (id = 1)`), chỉ ADMIN (A3) sửa qua khối "Điều khiển" và popup "Tham số hàng đợi" của
`ADM0401`. Lưu ở PostgreSQL để còn nguyên sau khi khởi động lại; module này cố ý không dùng Redis
(mục 3, chỉ dùng cho rate limiter Run Code và cache) nên không đặt ở đó `[SoT: Suy luận]`.

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

Một dòng duy nhất (`CHECK (id = 1)`), cùng khuôn `cluster_settings`, chỉ ADMIN (A3) sửa qua khối "Script sinh dữ liệu testcase" của `ADM0501` (Khu vực F). Giới hạn khi hệ thống chạy **script Python do AI viết** để sinh input testcase; riêng, không dùng chung với giới hạn bài nộp (RD `F2_problem-bank.md:88-91`). `problem-bank` đọc hai giá trị này khi chạy `GenerateTestcasesWithAi`, qua cổng đọc của module này (không đọc thẳng bảng).

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
| `child_process_limit_enabled` | BOOLEAN default `true` | Bật thì áp trần tiến trình con; tắt thì không giới hạn. Trần là **hằng số 60 ở `application.yml`**, không phải cột (`DEC-2026-1014-sandbox-child-process-cap-constant`, BD `ADM0501` V1.1 dòng 59, NO 5 dòng 266 (sửa vòng 2: trước ghi 265, đó là dòng NO 4); Sửa 2026-10-22 (đồng bộ DB, phase 1): đã bỏ cột `max_child_processes`) |
| `return_stderr_to_student` | BOOLEAN default `true` | Quyết định có ghi `submission_testcase_results.stderr_snippet` hay không |
| `updated_by` | UUID | Tham chiếu `identity.users.id`, không có FK vật lý chéo schema |
| `updated_at` | TIMESTAMPTZ | |

**Quy tắc thời điểm áp dụng (G3).** Một thay đổi ở `language_configs`, `grading_defaults` hoặc `sandbox_settings` có hiệu lực với một job từ lúc worker **bắt đầu chấm** nó, không phải lúc bài được nộp; job đã bắt đầu chấm thì chạy nốt với cấu hình nó đã đọc. **`judge.submissions` không lưu giới hạn thời gian/bộ nhớ hiệu dụng và không lưu snapshot cấu hình** (Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-ai-judge-schema-sync), JD-08, `DEC-2026-1022-ai-judge-schema-sync`): không RD nào đòi truy vết giới hạn và học viên không thấy nó, nên sau khi admin đổi cấu hình không thể biết một lượt nộp cũ đã chấm với giới hạn nào. Điều này được chấp nhận.

## 2. Chỉ mục (index) đáng chú ý

- `submissions(status, updated_at)` — job sweep quét theo điều kiện mục 1.1.
- `submissions(user_id, problem_id, submitted_at DESC)` — lịch sử nộp bài của một học viên cho một bài. ~~Cột cũ ghi `created_at`~~: bảng không có `created_at`, mốc nộp là `submitted_at` (dòng 31); sửa 2026-10-14 (`DEC-2026-1014-instructor-submissions-list-details`). Chỉ mục `submissions(status, updated_at)` ở dòng trên đúng với cột `updated_at` của bảng (dòng 34), không đổi.
- `submissions(problem_id, language, status) WHERE status = 'ACCEPTED'` — phục vụ F4-12 "Beats" (so runtime
  giữa các bài nộp Accepted cùng bài + cùng ngôn ngữ).
- `submission_testcase_results(submission_id, testcase_order)` — dựng lại kết quả theo đúng thứ tự chạy.
- `judge_run_tokens(submission_id, testcase_id) WHERE consumed_at IS NOT NULL` (unique một phần, mục 1.3) — vừa là ràng buộc idempotency vừa là chỉ mục tra "đã có token tiêu thụ chưa" trước mỗi lần gọi port (mục 5.1 kiến trúc). Sửa 2026-10-22 (đồng bộ DB, phase 1): bỏ chỉ mục thường `(submission_id, testcase_id)` cũ vì trùng phục vụ.
- `submissions(user_id, submitted_at DESC)` — lịch sử nộp của một học viên không lọc theo bài (`USR0202`, `INS0206`, cửa sổ 364 ngày của `GetMySubmissionMetrics`, kiến trúc mục 9). Sửa 2026-10-22 (đồng bộ DB, phase 1).

## 3. Redis

Sửa 2026-10-29 (F4-08): bản cũ ghi "không dùng Redis", đã lỗi thời. `judge-orchestration` dùng Redis cho đúng **hai** việc, cả hai do DD chốt: (1) bộ giới hạn tần suất Run Code 60 lượt trong 60 phút trượt cho mỗi người (`03-dd/logic/F4_judge-orchestration.md` BR-JD-02), chỉ phần này được fail-open khi Redis lỗi; trần đồng thời 8 lượt Run Code vẫn giữ; (2) cache số liệu lớp 15 phút. Trạng thái nộp bài vẫn đẩy trực tiếp qua WebSocket/STOMP; hàng đợi công việc là RabbitMQ. Không có bảng Redis nào cần migrate.

## 4. Migration — thứ tự tạo bảng

`language_configs` (seed 3 dòng), `grading_defaults`, `sandbox_settings` và `generator_sandbox_settings` (seed 1 dòng mỗi bảng) → `submissions` → `judge_run_tokens` → `submission_testcase_results` →
`callback_logs` → `outbox_events` → `cluster_settings` (seed 1 dòng).

Sửa 2026-10-22 (đồng bộ DB, phase 1): `judge_run_tokens` phải đứng trước `submission_testcase_results` vì cột `judge_run_token_id` là FK; thêm `generator_sandbox_settings` (mục 1.8, trước đó thiếu). Kiểm đủ FK: `judge_run_tokens` → `submissions`; `submission_testcase_results` → `submissions`, `judge_run_tokens`; `callback_logs` → `judge_run_tokens`; các bảng còn lại không có FK nội bộ.

## 5. Giá trị mặc định BD chốt (RD để ngỏ, `[SoT: Suy luận]`)

**Xác nhận 2026-10-23 (`DEC-2026-1023-db-actor-review-decisions`):** chủ dự án chốt **toàn bộ** giá trị ở bảng dưới theo đề xuất của BD; nhãn `[SoT: Suy luận]` còn lại chỉ ghi nhận rằng RD để ngỏ, không còn là điểm chờ xác nhận. Admin vẫn sửa được các giá trị này trên giao diện nếu cần.

| Tham số | Giá trị | Nguồn RD |
| :--- | :--- | :--- |
| Ngưỡng sweep timeout | 5 phút | `01-rd/req/F4_judge-orchestration.md` dòng 41-43 (RD đã chốt số) |
| Chu kỳ chạy job sweep | 60 giây | RD chỉ nói "bản nhẹ", không nêu chu kỳ — BD đề xuất |
| Giới hạn cắt `stderr_snippet` | 4096 ký tự | RD không nêu, BD đề xuất để tránh phình bảng khi chương trình học viên in log dài |
| `time_limit_multiplier` mặc định | Java `2.00`, C++ `1.00`, Python `3.00` | RD chỉ xác nhận có hệ số (F2-10), không nêu số — BD đề xuất |
| ~~`max_child_processes` mặc định~~ | ~~`60`~~ | Sửa 2026-10-22 (đồng bộ DB, phase 1): bỏ cột; `60` là hằng số `application.yml`, không còn mặc định trong bảng (`DEC-2026-1014-sandbox-child-process-cap-constant`, mục 1.10) |
| `memory_limit_multiplier` mặc định | Java `2.00`, C++ `1.00`, Python `1.00` | Sửa 2026-10-10 (trước đó `1.00` cho cả ba), mục 1.5 |
| Làm tròn F4-12 "Beats" | Số nguyên gần nhất | Xem kiến trúc mục 8 |

## 6. Việc còn mở — chuyển sang DD

- Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-ai-judge-schema-sync): ~~chốt ở DD việc ghi cấu hình đã dùng cùng kết quả chấm~~ **đã đóng** (JD-08): không ghi, mục 1.1 và 1.10. Không còn việc DD cho `problem_spec_id`: cột đã thay bằng `spec_version` (mục 1.1).
- Cấu trúc JSON đầy đủ của `outbox_events.payload` cho từng `event_type`.
- ~~Số cụ thể của mọi hàng ở mục 5 cần chủ dự án xác nhận trước khi seed migration thật.~~ **Đã chốt 2026-10-23** (`DEC-2026-1023-db-actor-review-decisions`): giữ nguyên các số của mục 5.
- `ProblemGradingSpecPort` (kiến trúc mục 3.2) — DTO trả về khi đọc đặc tả bài từ `problem-bank`.
- ~~Cơ chế đánh dấu "một dòng outbox kẹt quá nhiều lần" (`publish_attempts` vượt ngưỡng nào thì cảnh báo A3 qua `admin_queue_monitor`, F4-10) — chưa có số cụ thể.~~ **Đã chốt 2026-10-10** (`DEC-2026-1010-admin-operations-regroup`, tiêu chí ponytail bậc 5): hằng số trong `application.yml`, từ 3 lần thử là `WARN`, từ 5 lần là `ERROR`; hiển thị ở `ADM0404` và thẻ liên kết của `ADM0401`, không thêm cột hay bảng.

## 7. Tham chiếu

- `02-bd/architecture/F4_judge-orchestration.md` — kiến trúc, state machine, callback contract, Outbox.
- `02-bd/database/F2_problem-bank.md` mục 1.1, 1.6 — `current_testcase_set_version`,
  `TestcaseSetVersionBumped`.
- `.nexa/domain-registry.json` — domain `judge-orchestration`, scope: `submission`,
  `submission_testcase_result`, `judge_run_token`, `callback_log`, `language_config`.
- `.nexa/control/decision-registry.md` — `DEC-2026-0912-judge-callback-contract`,
  `DEC-2026-0912-judge-outbox-pattern`.

## 8. Hợp đồng DDL (đóng F4-01, 2026-10-29)

Bảng 8.1 là hợp đồng để viết `V4.001`. Quy ước chung giống F2 (`02-bd/database/F2_problem-bank.md` mục 7.1): ENUM của BD là `VARCHAR` cộng `CHECK` cùng tập giá trị; khoá chính UUID không có `DEFAULT` (id do `IdGeneratorPort` cấp); mọi FK nội schema là `NO ACTION`; cột trỏ sang schema khác là UUID thuần; `created_at`, `updated_at`, `issued_at`, `received_at`, `submitted_at` mặc định `now()` còn ứng dụng tự ghi `updated_at` khi sửa. Các CHECK dưới đây là **đề xuất của audit, chủ dự án duyệt 2026-10-29**, không phải điều đã có trong BD trước đó.

Bảng 8.1: Nullability và CHECK theo bảng.

| Bảng | Cột nullable (còn lại đều NOT NULL) | CHECK |
| :--- | :--- | :--- |
| `submissions` | `spec_version`, `failure_reason`, `compile_error_message`, `total_testcase_count`, `score_ratio`, `runtime_ms`, `memory_kb`, `started_grading_at`, `finished_at` | `octet_length(source_code) <= 65536`; tập giá trị của `language`, `submission_model`, `source_input_method`, `status`, `failure_reason`; `testcase_set_version_at_grading > 0`; `spec_version > 0` khi có; `passed_testcase_count >= 0`; `total_testcase_count >= 0` khi có; `passed_testcase_count <= total_testcase_count` khi có tổng; `score_ratio` trong `[0, 1]` khi có; `runtime_ms >= 0`, `memory_kb >= 0` khi có; `(status = 'SYSTEM_ERROR') = (failure_reason IS NOT NULL)`; `compile_error_message IS NULL OR status = 'COMPILE_ERROR'`; `(status IN <sáu trạng thái cuối>) = (finished_at IS NOT NULL)` |
| `submission_testcase_results` | `stderr_snippet`, `actual_output_snippet` | `verdict` thuộc năm giá trị; `testcase_order > 0`; `runtime_ms >= 0`, `memory_kb >= 0`; hai snippet tối đa 4096 ký tự; hai snippet chỉ có khi `is_sample`; `stderr_snippet` chỉ có khi `verdict <> 'AC'`. Unique `(submission_id, testcase_id)` |
| `judge_run_tokens` | `consumed_at` | Unique `token`; unique một phần `(submission_id, testcase_id) WHERE consumed_at IS NOT NULL` |
| `callback_logs` | `judge_run_token_id` | `outcome` thuộc bốn giá trị |
| `outbox_events` | `published_at` | `aggregate_type = 'Submission'`; `event_type` thuộc `SubmissionAccepted`, `SubmissionGraded`; `publish_attempts >= 0` |
| `language_configs` | `updated_by` | `language` thuộc ba giá trị; hai hệ số trong `[0.10, 9.99]` |
| `grading_defaults`, `sandbox_settings`, `generator_sandbox_settings`, `cluster_settings` | `updated_by` | `id = 1`; các cận `> 0` hoặc khoảng đã nêu ở mục 1.7 đến 1.10; không thu hẹp các cột INT đã khai |

Lý do giữ nullable ở `submissions`: `spec_version` chưa biết trước khi worker prepare; `total_testcase_count` chỉ có khi vào `JUDGING`; `score_ratio` để `NULL` cho `COMPILE_ERROR` và `SYSTEM_ERROR`; `finished_at` chỉ có ở trạng thái cuối. Mối quan hệ `status` với `failure_reason` và `finished_at` được ép bằng CHECK vì mọi đường kết thúc đều đi qua cùng một hàm `finalizeSubmission` (`03-dd/logic/F4_judge-orchestration.md` BR-JD-05). `SYSTEM_ERROR` vẫn được giữ `passed_testcase_count` và `total_testcase_count` từng phần, nên không có CHECK nào cấm điều đó.

**Seed (migration `V4.002`):** `language_configs` ba dòng: `JAVA` thời gian `2.00` bộ nhớ `2.00`; `CPP` `1.00` và `1.00`; `PYTHON` `3.00` và `1.00`; cả ba `enabled = true`. Bốn bảng singleton một dòng `id = 1` với mọi giá trị mặc định ở mục 1.7 đến 1.10. Mọi dòng seed có `updated_by = NULL`. Không seed `submissions` và các bảng còn lại.
