# DD Logic — Module `judge-orchestration` (F4)

> Nguồn: `01-rd/req/judge-orchestration.md` · `02-bd/architecture/judge-orchestration.md` · `02-bd/database/judge-orchestration.md` · `02-bd/security/judge-orchestration.md` · các BD màn có endpoint của module.
> Bounded Context: F4 `judge-orchestration` (schema `judge`) · Cập nhật: 2026-10-23.
> Đọc cùng `03-dd/api/judge-orchestration.md` (hợp đồng endpoint, kênh realtime, mã lỗi) và `03-dd/jobs/judge-orchestration.md`.
>
> File này ghi các **khối quy tắc nghiệp vụ** `BR-JD-nn` trải dài nhiều endpoint: trigger, tiền điều kiện, các bước, hậu điều kiện, sự kiện phát ra. Không lặp lại hợp đồng request/response (ở file API) và không lặp lại bốn job nền: JOB-JD-01 (sweep bài nộp treo), JOB-JD-02 (relay outbox), JOB-JD-03 (dọn outbox), JOB-JD-04 (dọn kết quả testcase quá 90 ngày) đã đặc tả đầy đủ ở `03-dd/jobs/judge-orchestration.md`; khối dưới chỉ nêu **chỗ chúng chạm vào logic ở đây**.
>
> Kiểm tra file job so với BD (2026-10-23): **không có xung đột thực chất**. Hai ghi chú nhỏ, không sửa file job: (1) job sweep (JOB-JD-01) tạo dòng outbox `SubmissionGraded` nên phải dùng **chung hàm kết thúc bài nộp** với worker để payload đủ `submittedAt` và `firstAttemptForProblem` (BR-JD-05, BR-JD-14); (2) file job viết consumer "idempotent theo `submissionId`" (dòng 93, 96) trong khi hợp đồng tiêu thụ của `identity` khử trùng theo `eventId` (`03-dd/api/identity.md:251`); với module này hai khoá tương đương vì mỗi bài nộp có đúng một dòng `SubmissionGraded` và một dòng `SubmissionAccepted` (BR-JD-14).

## 1. Máy trạng thái bài nộp (nền cho mọi BR)

Nguồn: `02-bd/architecture/judge-orchestration.md:155-185`, `DEC-2026-0912-judge-callback-contract`.

**Bảng 1: Chuyển trạng thái hợp lệ**

| Từ | Sang | Do ai | Điều kiện (câu lệnh có điều kiện bắt buộc) | Ghi kèm |
| :--- | :--- | :--- | :--- | :--- |
| (không có) | `PENDING` | endpoint #3 | tạo dòng | `submitted_at`, `updated_at` |
| `PENDING`, `COMPILING`, `JUDGING` | `COMPILING` | worker nhận job (kể cả khi giao lại) | `WHERE id = ? AND status IN ('PENDING','COMPILING','JUDGING')`; 0 dòng nghĩa là đã cuối, bỏ qua | `started_grading_at = COALESCE(started_grading_at, now())`, `spec_version`, `testcase_set_version_at_grading`, `updated_at` |
| `COMPILING` | `COMPILE_ERROR` (cuối) | worker | sau biên dịch lỗi | `compile_error_message`, `passed_testcase_count = 0`, `total_testcase_count`, `score_ratio` đều `NULL` |
| `COMPILING` | `JUDGING` | worker | biên dịch xong | `total_testcase_count = N`, `updated_at` |
| `JUDGING` | `JUDGING` (lặp) | worker | mỗi testcase xong | chỉ `updated_at` (xem BR-JD-04 bước 10) |
| `JUDGING` | `ACCEPTED`, `WRONG_ANSWER`, `TIME_LIMIT_EXCEEDED`, `RUNTIME_ERROR` (cuối) | worker | sau khi chạy hết N testcase | BR-JD-05 |
| mọi trạng thái chưa cuối | `SYSTEM_ERROR` (cuối) | worker (`SANDBOX_FAULT`) hoặc sweep (`WORKER_TIMEOUT`) | `WHERE status NOT IN (<6 trạng thái cuối>)` | `failure_reason`, `finished_at` |

**Bất biến** (kiểm bằng câu lệnh có điều kiện ở tầng DB chứ không bằng cờ trong bộ nhớ): một khi bài nộp vào một trong sáu trạng thái cuối thì **không bao giờ bị ghi đè**. Mọi `UPDATE` đổi `status` đều có mệnh đề `WHERE status NOT IN (<sáu trạng thái cuối>)` (hoặc `IN` danh sách chưa cuối như hàng 2); số dòng bị ảnh hưởng bằng 0 nghĩa là bên kia thắng cuộc đua, bên này **dừng không lỗi** (không ghi outbox, không phát bản tin). Đây là cách hiện thực bất biến ở mức bài nộp, còn `judge_run_tokens` hiện thực nó ở mức từng testcase (`02-bd/architecture/judge-orchestration.md:199-200`, `glossary.md` mục 3).

## BR-JD-01 — Tiếp nhận bài nộp (`CreateSubmission`, F4-01)

- **Trigger**: `POST /problems/{problemId}/submissions` (endpoint #3).
- **Tiền điều kiện**: người gọi là `STUDENT` đã xác thực; bài ở trạng thái xuất bản; ngôn ngữ đang `enabled`; mô hình hợp lệ cho bài; mã không rỗng và không quá 65.536 byte UTF-8.
- **Các bước**:
  1. Kiểm đầu vào (lớp Presentation) theo `03-dd/validation/judge-orchestration.md` mục 2.
  2. `loadGradingSpec(problemId)` qua `ProblemGradingSpecPort`: không xuất bản hoặc ẩn mềm thì `JDG-101` (không phân biệt, `USR0102` Sheet 9 NO 2); `functionWrapperSupported = false` mà mô hình `FUNCTION_WRAPPER` thì `JDG-103`; số testcase đã duyệt bằng 0 thì `JDG-104`.
  3. Kiểm `language_configs.enabled` của ngôn ngữ; `false` thì `JDG-102`.
  4. Mở giao dịch. Lấy khoá tư vấn mức giao dịch `pg_advisory_xact_lock(hashtextextended(userId || ':' || problemId, 0))` để việc **kiểm giới hạn rồi ghi** là nguyên tử với cùng một cặp (người, bài) `[SoT: Suy luận]`.
  5. Đếm lượt nộp của cặp trong một giờ gần nhất; so với `maxSubmissionsPerHour` của bài, `NULL` thì mặc định **60** (hằng số `judge.submission.default-hourly-limit`, `02-bd/database/problem-bank.md:295`). Đã đạt giới hạn thì `JDG-106`. Đếm mọi lượt nộp, kể cả lượt kết thúc `SYSTEM_ERROR` do hạ tầng `[SoT: Suy luận]`: BD không loại trừ, và loại trừ sẽ cần thêm trạng thái hay cột.
  6. `INSERT submissions (status = 'PENDING', submitted_at = now(), updated_at = now(), passed_testcase_count = 0, total_testcase_count = NULL, ...)`. Commit.
  7. Sau commit, publish `{ submissionId }` lên hàng đợi `judge.submissions` (publisher confirm, 3 lần, 200 ms rồi 500 ms). Message **không chứa mã nguồn**.
  8. Publish lỗi: ghi log `ERROR`, trả `JDG-503`; dòng `PENDING` ở lại cho sweep xử lý (câu hỏi mở Q4 của file API).
- **Hậu điều kiện**: tồn tại đúng một dòng `submissions` `PENDING` cho lượt nộp; message nằm trong hàng đợi (trừ nhánh lỗi bước 8). Phản hồi trả ngay, không chờ chấm.
- **Sự kiện**: không phát sự kiện miền ở bước này (chỉ phát khi vào trạng thái cuối). Không có bản tin STOMP cho `PENDING`.
- **Bất biến liên quan**: không bao giờ xoá hay ẩn lượt nộp (`USR0202` Q3); không có thao tác ghi nào khác trên `submissions` ngoài bảng 1.

## BR-JD-02 — Chạy thử với testcase Sample (`RunSampleTestcases`, F4-02)

- **Trigger**: `POST /problems/{problemId}/run-sample` (endpoint #2).
- **Tiền điều kiện**: như BR-JD-01 bước 1 đến 3, và bài có ít nhất một testcase Sample đã duyệt.
- **Các bước**:
  1. Xin "suất chạy thử" từ semaphore toàn cục (`judge.run-sample.max-concurrent` = 8 `[SoT: Suy luận]`); không còn suất thì `JDG-502` ngay, không xếp hàng.
  2. `loadSampleTestcases(problemId)`: điều kiện `visibility = SAMPLE` đặt ngay trong truy vấn của `problem-bank` (`USR0102` Sheet 9 NO 4).
  3. Đọc cấu hình **một lần** (BR-JD-09 bước 1) và tính giới hạn hiệu lực.
  4. `PrepareSubmissionUseCase.prepare` rồi `JudgeExecutionPort.compile`; lỗi biên dịch thì `MapCompileErrorUseCase.map`, dựng thông báo như BR-JD-03 bước 8 và trả `outcome = COMPILE_ERROR` (không có `results`).
  5. Với từng Sample theo `position`, **tuần tự**: `loadTestcaseData`, `run` (chính sách BR-JD-07), ánh xạ verdict (BR-JD-04 bước 7), `MatchOutputUseCase.match` nếu cần. **Không** dừng sớm ở testcase sai (cùng tinh thần F4-04 đã bãi bỏ).
  6. Hạn tổng 60 giây cho cả lượt; vượt hạn hoặc `run` hết lượt thử lại thì trả `JDG-501`.
  7. Trả kết quả; trả suất về semaphore (khối `finally`).
- **Hậu điều kiện**: **không có dòng nào được ghi** vào `submissions`, `submission_testcase_results`, `judge_run_tokens`, `outbox_events`; không có bản tin STOMP; không ảnh hưởng tiến độ hay `problem_stats` (`database/judge-orchestration.md:46`, F4-02). Idempotency: lệnh gọi không có tác dụng phụ nên không cần `judge_run_tokens` (bảng này khoá theo `submission_id`, run-sample không có).
- **Sự kiện**: không có.
- **Hạn chế đã biết**: không có giới hạn tần suất theo người cho Run Code (câu hỏi mở Q5), chỉ có trần đồng thời toàn cục.

## BR-JD-03 — Worker nhận job và mở đầu chấm

- **Trigger**: một message `{ submissionId }` tới từ hàng đợi `judge.submissions` (consumer RabbitMQ, ack thủ công).
- **Tiền điều kiện**: `cluster_settings.queue_paused = false` (khi `true`, consumer đã bị dừng, BR-JD-08).
- **Các bước**:
  1. Đọc `submissionId`. Dòng không tồn tại: ack, ghi log `ERROR`, bỏ.
  2. Lấy **khoá tư vấn mức phiên** `pg_try_advisory_lock(hashtextextended('judge:sub:' || id, 0))` trên một kết nối giữ suốt lượt chấm. Không lấy được thì ack và bỏ (worker khác đang chấm; nếu worker đó chết, khoá nhả và sweep hoặc lượt giao lại sẽ xử lý). Việc này bảo đảm **một người ghi duy nhất theo bài nộp** (nền của thứ tự realtime, mục 2.4 file API) `[SoT: Suy luận]`.
  3. Đọc `status`. Nếu đã là một trong sáu trạng thái cuối: ack và bỏ (message giao lại sau khi sweep đã kết thúc bài).
  4. **Đọc cấu hình một lần cho cả lượt chấm** (BR-JD-09): dòng `language_configs` của ngôn ngữ, `grading_defaults`, `sandbox_settings`. Không cache tĩnh giữa các job.
  5. `loadGradingSpec(problemId)`: nhận `specVersion`, `currentTestcaseSetVersion`, `matchingStrategy`, giới hạn của bài và **danh sách testcase đã duyệt** (mọi `visibility`) theo `position`; gán `testcase_order = 1..N`. Đọc ngay lúc này là mốc "bắt đầu compile" mà BD yêu cầu cho `testcase_set_version_at_grading` (`02-bd/architecture/judge-orchestration.md:150-153`). Lỗi đọc đặc tả: thử lại tối đa 2 lần, rồi kết thúc `SYSTEM_ERROR` với `failure_reason = SANDBOX_FAULT` (BR-JD-07, câu hỏi mở Q21).
  6. Chuyển trạng thái `-> COMPILING` theo bảng 1 (câu lệnh có điều kiện), ghi `spec_version`, `testcase_set_version_at_grading`, `started_grading_at`. Sau commit phát `STATUS_CHANGED(COMPILING)`.
  7. `PrepareSubmissionUseCase.prepare(problemId, language, model, sourceCode, expectedSpecVersion)` của `harness` (`expectedSpecVersion` là `specVersion` đọc ở bước 5). Mã lỗi `HRN-002` (đặc tả đổi giữa hai lần đọc, khoảng mili giây): đọc lại đặc tả một lần rồi gọi lại; vẫn lệch thì kết thúc `SYSTEM_ERROR` (`SANDBOX_FAULT`). `HRN-003` (cổng đọc của `harness` lỗi tạm): thử lại theo BR-JD-07. `HRN-004`, `HRN-005`, `HRN-006`, `HRN-030`, `HRN-031` (dữ liệu bài hoặc lỗi harness, không phải lỗi của học viên): ghi log `ERROR` và kết thúc `SYSTEM_ERROR` (`SANDBOX_FAULT`, câu hỏi mở Q21). **Một `PreparedProgram` dùng cho cả bài nộp**: đặc tả đổi giữa chừng không làm các testcase của cùng một bài nộp chấm bằng hai phiên bản. Rồi `JudgeExecutionPort.compile(...)` với payload của `PreparedProgram`.
  8. Biên dịch lỗi: gọi `MapCompileErrorUseCase.map(language, model, rawCompilerOutput, lineMap)`. `status = USER_ERRORS`: dựng `compile_error_message` từ `diagnostics[]` (mỗi dòng `Dòng {line}: {message}`, theo thứ tự `line`, tối đa 10 mục như harness trả về), cắt 10.000 ký tự. `status = HARNESS_INTERNAL` (lỗi nằm ở mã khung): dùng thông báo cố định theo `fixedMessageKey` (khuôn tiếng Việt đặt ở tài nguyên của module, `[SoT: Suy luận]`, câu hỏi mở Q23); harness đã ghi log nội bộ (F3-12); **không** đưa `rawCompilerOutput` vào bất kỳ đâu ngoài log mức `ERROR` của harness. Kết thúc `COMPILE_ERROR` (BR-JD-05 biến thể). Dừng.
  9. Biên dịch xong: chuyển `-> JUDGING`, ghi `total_testcase_count = N`; sau commit phát `STATUS_CHANGED(JUDGING, totalCount = N)`. Sang BR-JD-04.
- **Hậu điều kiện**: bài nộp ở `COMPILE_ERROR`, hoặc ở `JUDGING` với `total_testcase_count = N`.
- **Giao lại message** (worker trước chết): bước 3 và 6 cho phép **tiếp tục** từ `COMPILING` hoặc `JUDGING`; compile chạy lại (go-judge không có trạng thái); các testcase đã có kết quả được bỏ qua ở BR-JD-04 bước 2.
- **Message được ack khi nào**: chỉ **sau khi** trạng thái cuối commit (hoặc ở các nhánh bỏ qua trên). Worker chết trước đó thì message được giao lại.

## BR-JD-04 — Vòng chấm từng testcase, chạy hết, có chống ghi trùng (F4-03, F4-13)

- **Trigger**: BR-JD-03 bước 9 xong, bài ở `JUDGING`.
- **Tiền điều kiện**: biết `N` testcase theo `testcase_order`; đã có `PreparedProgram`; cấu hình đã đọc.
- **Nguyên tắc cứng**: gọi `JudgeExecutionPort.run` **một lần cho mỗi testcase**, cho **mọi** testcase, **không dừng sớm** khi một testcase sai hay lỗi (F4-04 đã bãi bỏ, `DEC-2026-0831-partial-score-testcase-ratio`). Không có nhánh "dừng khi gặp lỗi" ở tầng này; điểm dừng duy nhất sớm là lỗi **hạ tầng** (BR-JD-07).
- **Các bước, lặp cho `i = 1..N`**:
  1. Tải trước các dòng `submission_testcase_results` đã có của bài (để tính `completedCount`, `passedCount` đúng khi tiếp tục sau giao lại).
  2. **Chống chạy lại**: nếu đã có token `consumed_at IS NOT NULL` cho `(submission_id, testcase_id)` thì **bỏ qua**, không gọi engine, không ghi đè, không phát lại bản tin (`02-bd/architecture/judge-orchestration.md:196-200`).
  3. Giao dịch ngắn: `INSERT judge_run_tokens (submission_id, testcase_id, token = uuid, issued_at = now(), consumed_at = NULL)`. Commit (token chưa tiêu thụ còn lại nếu worker chết giữa lượt gọi).
  4. `loadTestcaseData(problemId, testcaseId)` lấy input và expected output (INLINE hoặc MinIO). Expected output chỉ dùng cho `MatchOutputUseCase.match`, **không bao giờ** đi vào bản ghi nào trả về học viên.
  5. Dựng `RunRequest`: `PreparedProgram.payload` (đã đóng theo định dạng adapter), `stdin` = input, giới hạn hiệu lực (`ROUND(time_limit_ms * time_limit_multiplier)`, `ROUND(memory_limit_mb * memory_limit_multiplier)`, `max_output_size_kb`), tham số sandbox từ `sandbox_settings` (mạng, trần tiến trình con là hằng số 60 nếu `child_process_limit_enabled`).
  6. `run(RunRequest)` với chính sách thử lại ở BR-JD-07.
  7. **Ánh xạ verdict**: trạng thái engine `OK` thì gọi `MatchOutputUseCase.match(model, matchSpec, resultSentinel, actualStdout, expectedOutput)` (`MATCH` là `AC`, `NO_MATCH` là `WA`; `HRN-050` expected output không parse được là lỗi dữ liệu testcase, **không** phải `WA`: ghi log `ERROR` và kết thúc `SYSTEM_ERROR`, xem Q21); mã thoát bằng `PreparedProgram.harnessInputErrorExitCode` (90) nghĩa là chương trình sinh ra không đọc nổi input của testcase, là lỗi dữ liệu hoặc harness chứ **không** phải `RE` của học viên, xử như `HRN-050`; hết thời gian là `TLE`; hết bộ nhớ là `MLE`; lỗi chạy (thoát khác không, tín hiệu) là `RE`; vượt giới hạn output gộp vào `RE` (câu hỏi mở Q21). Module **không tự so khớp** output, chỉ tổng hợp (`02-bd/architecture/judge-orchestration.md:62-63`).
  8. Dựng các cột lưu: `actual_output_snippet` = `MatchResult.actualOutput` (phần kết quả của học viên sau khi harness tách dòng đánh dấu kết quả; không phải stdout thô) cắt 4.096 ký tự **chỉ khi testcase `SAMPLE`**, testcase `HIDDEN` luôn `NULL`; `stderr_snippet` = stderr cắt 4.096 ký tự chỉ khi `sandbox_settings.return_stderr_to_student = true` **và** `verdict != AC` (`database/judge-orchestration.md:63-64`).
  9. **Một giao dịch**: `INSERT submission_testcase_results (submission_id, testcase_id, testcase_order = i, verdict, runtime_ms, memory_kb, stderr_snippet, actual_output_snippet, judge_run_token_id, created_at = now())`, `UPDATE judge_run_tokens SET consumed_at = now() WHERE token = :token`, `UPDATE submissions SET updated_at = now() WHERE id = :id AND status = 'JUDGING'`. Vi phạm unique `(submission_id, testcase_id)` nghĩa là writer khác đã ghi: huỷ giao dịch, tải lại dòng đã có, tiếp tục (không phải lỗi).
  10. **Chạm `updated_at` ở mỗi testcase** (bước 9) để sweep không đưa một bài đang chấm bình thường nhưng dài (nhiều testcase) về `SYSTEM_ERROR` sau 5 phút kể từ lúc bắt đầu. Đây là quy định bổ sung của DD: BD chỉ nói `updated_at` cập nhật ở mọi lần đổi trạng thái (`database/judge-orchestration.md:38`); nếu không chạm theo testcase, một bài chấm hợp lệ chạy hơn 5 phút sẽ bị sweep đánh trượt.
  11. Sau commit, phát `TESTCASE_FINISHED` (BR-JD-06).
- **Hậu điều kiện**: sau vòng lặp có **đúng N dòng** `submission_testcase_results` cho bài (trừ nhánh dừng do lỗi hạ tầng hoặc biên dịch).
- **Thứ tự `testcase_order`**: theo `position` của `problem-bank` lúc đọc đặc tả, đánh số lại từ 1. "Testcase thất bại đầu tiên" luôn theo `testcase_order` (`database/judge-orchestration.md:59-60`).
- **Sự kiện**: `TESTCASE_FINISHED` (STOMP, không phải sự kiện miền).

## BR-JD-05 — Kết thúc bài nộp: tổng hợp trạng thái, điểm tỷ lệ, outbox (F4-13)

- **Trigger**: (a) vòng BR-JD-04 hoàn tất; (b) biên dịch lỗi (BR-JD-03 bước 8); (c) lỗi hạ tầng (BR-JD-07); (d) sweep JOB-JD-01. Cả bốn đường gọi **cùng một hàm** `finalizeSubmission(...)` để payload sự kiện luôn đủ trường.
- **Tiền điều kiện**: bài chưa ở trạng thái cuối.
- **Các bước (một giao dịch)**:
  1. **Trường hợp chạy hết N testcase**: đọc mọi dòng `submission_testcase_results` theo `testcase_order`. `passed = số dòng AC`, `total = N`.
  2. **Suy ra `status`** từ testcase thất bại **đầu tiên** theo `testcase_order`: tất cả `AC` là `ACCEPTED`; `WA` là `WRONG_ANSWER`; `TLE` hoặc `MLE` là `TIME_LIMIT_EXCEEDED` (gộp nhãn); `RE` là `RUNTIME_ERROR` (`database/judge-orchestration.md:60`). Quy tắc "verdict của testcase thất bại đầu tiên" là suy luận của BD theo quy ước LeetCode và Codeforces (`02-bd/architecture/judge-orchestration.md:177-181`), đang chờ chủ dự án xác nhận.
  3. `score_ratio = ROUND(passed / total, 3)` làm tròn nửa lên, kiểu `DECIMAL(4,3)`; ví dụ 12 trên 20 là `0.600`. `runtime_ms = MAX(runtime_ms)`, `memory_kb = MAX(memory_kb)` trên các dòng đã chạy.
  4. **Biến thể `COMPILE_ERROR`**: `compile_error_message` đã qua ánh xạ dòng, cắt 10.000 ký tự; `passed_testcase_count = 0` nhưng `total_testcase_count`, `score_ratio`, `runtime_ms`, `memory_kb` đều `NULL` (phân biệt "chưa chấm được testcase nào" với "chấm hết, rớt hết", `database/judge-orchestration.md:31`).
  5. **Biến thể `SYSTEM_ERROR`**: `failure_reason` là `SANDBOX_FAULT` (worker) hoặc `WORKER_TIMEOUT` (sweep); `score_ratio = NULL`; `passed_testcase_count` và `total_testcase_count` giữ giá trị hiện có nhưng hiển thị `-` (đọc kèm `score_ratio IS NULL`).
  6. `UPDATE submissions SET status = ?, passed_testcase_count = ?, score_ratio = ?, runtime_ms = ?, memory_kb = ?, finished_at = now(), updated_at = now(), failure_reason = ?, compile_error_message = ? WHERE id = ? AND status NOT IN (<sáu trạng thái cuối>)`. **0 dòng** thì dừng, không ghi outbox, không phát bản tin (bên kia đã kết thúc bài).
  7. **Ghi outbox trong cùng giao dịch**: một dòng `SubmissionGraded` (mọi trạng thái cuối); thêm một dòng `SubmissionAccepted` nếu `ACCEPTED` (BR-JD-14).
  8. Commit.
  9. Sau commit: phát `SUBMISSION_FINISHED` qua STOMP (`seq` theo mục 2.4 file API); ack message RabbitMQ.
- **Hậu điều kiện**: bài ở một trong sáu trạng thái cuối, `finished_at` có giá trị, đúng một dòng `SubmissionGraded` trong outbox cho bài. `verdict` nhị phân `Accepted` và F4-13 là **hai thông tin độc lập**: điểm tỷ lệ không thay thế, không ảnh hưởng điều kiện kích hoạt F5, tỉ lệ chấp thuận F1-07 hay ngưỡng "Bài cần chú ý" (`01-rd/req/judge-orchestration.md:23-33`).
- **Sự kiện**: `SubmissionGraded`, `SubmissionAccepted` (qua outbox); `SUBMISSION_FINISHED` (STOMP).

## BR-JD-06 — Đẩy trạng thái thời gian thực (F4-08)

- **Trigger**: giao dịch ở các bước đổi trạng thái (BR-JD-03 bước 6, 9), ghi kết quả testcase (BR-JD-04 bước 9) và kết thúc (BR-JD-05) vừa commit.
- **Các bước**:
  1. Đăng ký hành động `afterCommit` ngay trong phạm vi giao dịch; chỉ khi commit thành công mới đẩy.
  2. Dựng bản tin theo lược đồ mục 2.3 file API, tính `seq` tất định (mục 2.4).
  3. `convertAndSendToUser(userId, "/queue/submissions", bản tin)` với `userId` là chủ bài nộp (đọc từ dòng `submissions`).
  4. Lỗi đẩy: ghi `WARN`, bỏ qua; **không** làm hỏng việc chấm. Phần mất được phục hồi bằng `GET /submissions/{id}/progress`.
- **Quy tắc nội dung**: bản tin chỉ mang `order` và `verdict` của testcase, không runtime, không output, không stderr; mọi số liệu chi tiết lấy ở endpoint #5 sau khi kết thúc (BR-JD-11).
- **Kênh quản trị**: một bộ lập lịch 5 giây dựng một lô `QUEUE_BATCH` (BR-JD-15) và đẩy tới `/topic/admin/queue`, chỉ khi `SimpUserRegistry` còn người đăng ký đích đó.
- **Hậu điều kiện**: thứ tự bản tin khớp thứ tự commit (một worker duy nhất cho một bài nộp). **Không** bảo đảm giao đúng một lần; nguồn sự thật là DB.
- **Sự kiện**: không phải sự kiện miền; không đi qua outbox (khác sự kiện miền vì mất một bản tin hiển thị không gây lệch dữ liệu hệ thống).

## BR-JD-07 — Đường lỗi khi gọi go-judge: timeout, thử lại, `SANDBOX_FAULT`

- **Trigger**: mọi lời gọi `JudgeExecutionPort.compile` hoặc `run` (từ BR-JD-02, BR-JD-03, BR-JD-04).
- **Phân loại kết quả** (do `GoJudgeAdapter` làm, nên engine khác cắm vào không đổi logic):

  | Loại | Ví dụ trạng thái engine | Hành xử |
  | :--- | :--- | :--- |
  | Kết quả nghiệp vụ | thành công; hết thời gian; hết bộ nhớ; thoát khác không; vượt output; biên dịch thất bại | có verdict, **không thử lại** |
  | Lỗi hạ tầng | không nối được; `5xx`; quá timeout lưới an toàn; go-judge báo lỗi nội bộ; không khởi tạo được sandbox; không tìm thấy tệp trung gian đã biên dịch | thử lại theo bên dưới |

- **Timeout** (hằng số `application.yml`, `[SoT: Suy luận]`, xem bảng 4 file API): kết nối 2 giây; một lần `run` = thời gian hiệu lực của testcase cộng 5 giây; một lần `compile` = `grading_defaults.compile_timeout_sec` cộng 5 giây. Sandbox là chốt chặn thật cho giới hạn thời gian; các timeout này chỉ là lưới an toàn để không treo luồng.
- **Thử lại**: tối đa 2 lần thử lại, chờ 500 ms rồi 1 giây. Dùng lại **cùng một** `judge_run_tokens.token` cho mọi lần thử của cùng một lượt gọi testcase (cùng một lần gọi logic). Tệp trung gian biên dịch bị mất ở go-judge (go-judge khởi động lại): thử lại bằng cách biên dịch lại một lần rồi chạy lại testcase đó `[SoT: Suy luận]`.
- **Hết lượt thử lại** (`failure_reason = SANDBOX_FAULT`):
  1. Với bài nộp thật: gọi `finalizeSubmission` biến thể `SYSTEM_ERROR` (BR-JD-05 bước 5): một giao dịch ghi trạng thái, `failure_reason`, dòng outbox `SubmissionGraded`; sau commit phát `SUBMISSION_FINISHED(SYSTEM_ERROR)`. Các testcase đã chạy xong **giữ nguyên** dòng kết quả.
  2. Với Run Code: trả `JDG-501`, không ghi gì.
- **Không** thử lại khi worker tự crash: RabbitMQ giao lại message; BR-JD-03 và BR-JD-04 tiếp tục không chạy trùng nhờ `judge_run_tokens`.
- **Khi nào sweep vào cuộc**: chỉ khi worker chết và không ai nhận lại, hoặc lúc `updated_at` đứng im quá 5 phút (một lượt gọi đơn lẻ không thể quá thời gian hiệu lực tối đa 30.000 ms cộng 5 giây, nên một bài đang chạy bình thường không bị sweep nhờ bước 10 của BR-JD-04).
- **Học viên thấy gì**: trong lúc thử lại, bài vẫn `JUDGING`, ô testcase đang chạy chưa tô màu; không có thông báo riêng. Sau khi hết lượt thử lại: nhãn lỗi hệ thống (`SYSTEM_ERROR`), không phải `WRONG_ANSWER`.
- **Quan sát cho admin**: bài `SYSTEM_ERROR` hiện ở `ADM0404` (dịch vụ `go-judge`) và ở tab "Lỗi" của `ADM0401` (BR-JD-15).
- **Cách nhận diện cụ thể** mã trả về của go-judge (HTTP và trạng thái từng lệnh) là chi tiết của `GoJudgeAdapter`, kiểm bằng test tích hợp với go-judge thật (`dependency-map.md` mục 4: dựng cùng `harness` ở giai đoạn 2).

## BR-JD-08 — Tạm dừng và tiếp tục tiêu thụ hàng đợi (F4-10)

- **Trigger**: `PUT /admin/queue/consumption` (endpoint #16) và bộ điều khiển worker.
- **Tiền điều kiện**: người gọi là `ADMIN`.
- **Các bước**:
  1. **Khoá và so sánh trạng thái mong đợi**: `SELECT ... FROM cluster_settings WHERE id = 1 FOR UPDATE`; `expectedPaused` khác `queue_paused` thì `JDG-201` (`ADM0401` Sheet 9 NO 5).
  2. Ghi `queue_paused`, `updated_by`, `updated_at`.
  3. Khi **tiếp tục** (`true` sang `false`): trong cùng giao dịch `UPDATE submissions SET updated_at = now() WHERE status = 'PENDING'` để đồng hồ 5 phút của sweep tính lại từ lúc mở (`02-bd/architecture/judge-orchestration.md:220-221`; `03-dd/jobs/judge-orchestration.md:64`).
  4. `AuditLogPort.record(...)` **trong cùng giao dịch**; lỗi ghi nhật ký làm hỏng cả giao dịch.
  5. **Bộ điều khiển worker** (`QueueConsumptionGate`, `@Scheduled`, chu kỳ 3 giây): đọc `queue_paused` (cache trong tiến trình tối đa 3 giây, `ADM0401` Q3); `true` thì **dừng** container của `@RabbitListener` `judge.submissions` (dừng nhận message mới, **đợi message đang xử lý xong**); `false` thì khởi động lại. Không dùng nack hay requeue để tránh vòng lặp trả message.
- **Hậu điều kiện**: khi tạm dừng, bài nộp mới vẫn được nhận (`PENDING`) và nằm chờ; job đang chấm chạy hết; **sweep bỏ qua `PENDING`** (JOB-JD-01); bảng job `ADM0401` vẫn thấy các job `PENDING`.
- **Sự kiện**: không phát sự kiện miền; dòng nhật ký `QUEUE_PAUSE_CHANGED`.
- **Bảo vệ khỏi sai lệch**: nếu RabbitMQ vẫn giữ message lúc tiếp tục, worker tiêu thụ tiếp theo thứ tự đến; message của bài đã kết thúc (do sweep trước đó) bị ack bỏ qua ở BR-JD-03 bước 3.

## BR-JD-09 — Cấu hình admin: thời điểm áp dụng, giới hạn hiệu lực, kiểm toán

- **Trigger**: `PUT /admin/language-config` (#22), `PUT /admin/queue/settings` (#19), `PUT /admin/queue/autoscale` (#17), cùng với mọi lượt chấm (đọc).
- **Quy tắc thời điểm áp dụng (G3, `database/judge-orchestration.md:198`)**:
  1. Worker **đọc cấu hình một lần lúc bắt đầu chấm một bài** (BR-JD-03 bước 4) trong một giao dịch chỉ đọc và giữ giá trị đó tới hết lượt chấm; **không cache tĩnh dài hạn** (`02-bd/security/judge-orchestration.md:77-80`).
  2. Hệ quả: một thay đổi có hiệu lực với job **từ lúc worker bắt đầu chấm nó**, không phải lúc bài được nộp; job đã bắt đầu chấm chạy nốt với giá trị đã đọc. Ngoại lệ: nếu worker chết và message được giao lại, lượt đọc thứ hai thấy giá trị mới (chấp nhận được, hiếm).
  3. `judge.submissions` không lưu giới hạn hiệu lực và không lưu ảnh chụp cấu hình (`database/judge-orchestration.md:44`): không truy vết lại được giới hạn đã dùng cho bài cũ. Được chấp nhận.
- **Giới hạn hiệu lực của một lượt chấm**: thời gian = `ROUND(problem.time_limit_ms * time_limit_multiplier)`, bộ nhớ = `ROUND(problem.memory_limit_mb * memory_limit_multiplier)`, output = `problem.max_output_size_kb`, biên dịch = `grading_defaults.compile_timeout_sec`. Giá trị `grading_defaults` còn làm **dự phòng** khi `problem-bank` trả giá trị thiếu (không xảy ra với cột `NOT NULL`, `database/problem-bank.md:42`) và làm giá trị khởi tạo cho nháp bài mới (qua `JudgeConfigReadPort.getGradingDefaults`).
- **Ngôn ngữ tắt (G4)**: `enabled = false` chỉ chặn bài nộp **mới** (`JDG-102` ở BR-JD-01 bước 3 và Run Code). Máy chủ **không** huỷ hay đổi trạng thái bài đang chờ của ngôn ngữ đó; chúng chạy nốt. Hệ quả cho worker: bước kiểm `enabled` **không** có ở BR-JD-03 (chỉ có ở lúc nhận bài).
- **Kiểm toán (G2)**: mỗi lệnh ghi thành công gọi `AuditLogPort.record(actorUserId, actionType, targetType, targetId, beforeJson, afterJson)` **trong cùng giao dịch** (`Propagation.REQUIRED`), gồm giá trị cũ và mới của chỉ các trường đã đổi. Lỗi ghi nhật ký làm hỏng cả giao dịch (đồng bộ với `03-dd/logic/identity.md` BR-24 dòng 356); khác với câu "lỗi ghi nhật ký không làm hỏng lệnh đã xác nhận" ở `ADM0401` EVT-14 và `ADM0501` EVT-11 (hai nơi BD tự ghi `[SoT: Suy luận]` rồi giao DD chốt thứ tự giao dịch): DD chọn cùng giao dịch vì `AuditLogPort` đã chốt như vậy (`02-bd/architecture/identity.md:92`). Bảng kiểm toán:

  | `actionType` (tên đề xuất `[SoT: Suy luận]`) | `targetType` | Lệnh |
  | :--- | :--- | :--- |
  | `QUEUE_PAUSE_CHANGED` | `CLUSTER_SETTINGS` | #16 |
  | `QUEUE_AUTOSCALE_CHANGED` | `CLUSTER_SETTINGS` | #17 |
  | `QUEUE_SETTINGS_CHANGED` | `CLUSTER_SETTINGS` | #19 |
  | `LANGUAGE_CONFIG_CHANGED` | `LANGUAGE_CONFIG` | #22 (một dòng cho cả lần lưu: hệ số, bật tắt, giới hạn mặc định, sandbox, script sinh dữ liệu) |

  Bốn `actionType` này **phải được thêm vào enum `AuditAction`** của `identity` (nhóm "Cấu hình", `03-dd/logic/identity.md` BR-19) để nhật ký hiển thị được.
- **Kiểm lệnh ghi**: `ADMIN` kiểm lại ở máy chủ mỗi lệnh; `LANGUAGE_CONFIG:UPDATE` cho #22. Lệnh không đổi giá trị nào thì không ghi và không có dòng nhật ký.
- **Xung đột đồng thời**: #16, #17 so `expectedPaused` hoặc `expectedEnabled` với trạng thái thật dưới khoá `FOR UPDATE`; #22 so `updatedAt` từng dòng (`JDG-202`).
- **Sự kiện**: không phát sự kiện miền.

## BR-JD-10 — Phạm vi lớp cho giảng viên (A2)

- **Trigger**: endpoint #10, #11, #12.
- **Tiền điều kiện**: người gọi có `CLASS_MANAGEMENT:READ`.
- **Các bước**:
  1. `ClassScopeReadPort.getInstructorScope(instructorId, classId?)` trả `{classId, className, studentId, studentName, joinedAt, leftAt}` cho các lớp có `instructor_id` bằng người gọi, **kể cả dòng ghi danh đã `left_at`**. Có `classId` mà không thuộc lớp của người gọi, hoặc sai định dạng UUID thì `JDG-111` (không trả danh sách rỗng, không lộ lớp có thật).
  2. `ClassAssignmentReadPort.listAssignments(classIds)` trả `{classId, problemId, problemTitle, assignedAt, removedAt}`, **kể cả bài đã gỡ mềm** (`INS0206` dòng 208-209).
  3. Truy vấn `judge.submissions` với **ba điều kiện phạm vi cùng lúc**:
     - (a) học viên thuộc một lớp do người gọi phụ trách;
     - (b) bài của lượt nộp **đã giao cho lớp đó**, không gồm bài tự luyện (REQ-15);
     - (c) `submitted_at >= joined_at` và (`left_at IS NULL` hoặc `submitted_at <= left_at`). Học viên rời rồi vào lại dùng lại dòng ghi danh cũ (`left_at` về `NULL`, `joined_at` giữ nguyên) nên **khoảng đã rời cũng tính là trong lớp**; chấp nhận khoảng hở này (`DEC-2026-1014-instructor-submissions-list-details`, `INS0206` dòng 212).
  4. **Chọn đúng một lớp cho mỗi bài nộp** khi học viên ở hai lớp của cùng giảng viên và cả hai cùng giao một bài: lớp có `assigned_at` **mới nhất mà không sau `submitted_at`**; mỗi bài nộp đúng một dòng; `judge.submissions` không có cột `class_id`, lớp suy ra lúc truy vấn (`INS0206` dòng 213). Bộ lọc `classId` áp **sau** bước chọn lớp, nên lọc lớp B không thấy bài nộp đã gắn sang lớp A.

  ```sql
  WITH scope  AS (SELECT * FROM unnest(:classIds, :studentIds, :joinedAts, :leftAts)
                  AS s(class_id, student_id, joined_at, left_at)),
       assign AS (SELECT * FROM unnest(:aClassIds, :aProblemIds, :assignedAts)
                  AS a(class_id, problem_id, assigned_at)),
       cand AS (
         SELECT sub.id, sc.class_id, sc.student_id,
                row_number() OVER (PARTITION BY sub.id ORDER BY a.assigned_at DESC, sc.class_id) AS rn
         FROM judge.submissions sub
         JOIN scope sc  ON sc.student_id = sub.user_id
                       AND sub.submitted_at >= sc.joined_at
                       AND (sc.left_at IS NULL OR sub.submitted_at <= sc.left_at)
         JOIN assign a  ON a.class_id = sc.class_id AND a.problem_id = sub.problem_id
                       AND a.assigned_at <= sub.submitted_at)
  SELECT ... FROM cand JOIN judge.submissions s ON s.id = cand.id
  WHERE cand.rn = 1 [AND cand.class_id = :classId] [AND bộ lọc kết quả, từ khoá]
  ORDER BY s.submitted_at DESC OFFSET :o LIMIT 20;
  ```

  Truy vấn mẫu do DD đề xuất; BD giao cách viết cho DD (`INS0206` dòng 213). Dữ liệu phạm vi đi vào truy vấn bằng tham số mảng, kích thước giới hạn bởi sĩ số các lớp của một giảng viên.
  5. `q` khớp `studentName` (từ phạm vi) hoặc `problemTitle` (từ bài giao) bằng so khớp không phân biệt hoa thường, không cần gọi thêm cổng.
  6. `result`: `accepted` là `ACCEPTED`; `failed` là trạng thái cuối khác `ACCEPTED` và khác `SYSTEM_ERROR`; `all` không lọc; `PENDING`, `COMPILING`, `JUDGING`, `SYSTEM_ERROR` chỉ ở `all` (`INS0206` Sheet 9 NO 10).
  7. `leftClass = true` khi dòng ghi danh khớp có `left_at` khác `NULL`.
- **Biến thể chi tiết một bài nộp** (#11): kiểm tồn tại một lớp thoả (a), (b), (c) cho **bài nộp đó**; ngoài ra `JDG-110` (404, không phải 403). `studentId`, `classId`, `from` trên URL bị bỏ qua; học viên và lớp lấy từ chính bài nộp (A2-2).
- **Biến thể lịch sử một học viên** (#12): lớp cố định theo đường dẫn; bước 4 không cần.
- **Hậu điều kiện**: phản hồi danh sách chỉ có siêu dữ liệu (không `source_code`, không `compile_error_message`, không dòng testcase); phản hồi chi tiết (#11) có mã nguồn và dòng testcase theo BR-JD-11.
- **Suy giảm êm**: `identity` hoặc `problem-bank` lỗi thì endpoint trả `503` chung cho danh sách phụ thuộc phạm vi (không thể trả dữ liệu đúng khi thiếu phạm vi, và không được trả rộng hơn); không ảnh hưởng luồng nộp và chấm của học viên.

## BR-JD-11 — Quy tắc cứng: không lộ testcase ẩn, hết hạn 90 ngày, stderr

- **Trigger**: mọi lần dựng phản hồi có dữ liệu testcase: endpoint #2, #5, #6, #11, và bản tin STOMP.
- **Nguyên tắc**: lọc ở **tầng dựng phản hồi và ở tầng truy vấn**, không ẩn ở giao diện (`USR0102` Sheet 9 NO 3). Có **hai lớp độc lập**:
  1. **Lớp nguồn**: chạy thử chỉ nhận testcase `SAMPLE` ngay trong truy vấn (BR-JD-02 bước 2); cột `actual_output_snippet` chỉ được ghi cho testcase `SAMPLE` lúc chấm (BR-JD-04 bước 8).
  2. **Lớp phản hồi**: bộ dựng phản hồi chỉ được thêm `actualOutput`, `testcaseId`, `stderrSnippet` cho một dòng khi **cả hai** đúng: (i) cột `actual_output_snippet` khác `NULL` (chứng cứ lúc chấm) **và** (ii) `visibility` tra được hiện tại là `SAMPLE`. Không tra được (testcase đã xoá, `problem-bank` lỗi) thì coi là `HIDDEN`: chỉ `order`, `verdict`, `runtimeMs`, `memoryKb`. Xem câu hỏi mở Q7 về việc chụp `visibility` lúc chấm.
- **Không bao giờ trả** cho học viên hay giảng viên: input, expected output, output thực tế và diff của testcase `HIDDEN`; expected output của **mọi** testcase (expected của Sample do `problem-bank` phục vụ qua `GetProblemSampleTestcases`, không đi qua F4); `compile_error_message` còn phần mã khung (đã lọc ở `harness`).
- **`stderr`** (`02-bd/security/judge-orchestration.md:60-63`): chỉ trả khi `sandbox_settings.return_stderr_to_student = true`, `verdict != AC` và testcase `SAMPLE`; cắt 4.096 ký tự. Giới hạn thêm điều kiện `SAMPLE` là **đề xuất** của DD (Q6): stderr của testcase ẩn có thể in lại input.
- **Hết hạn lưu giữ 90 ngày** (`database/judge-orchestration.md:68-72`): chi tiết từng testcase có hiệu lực **90 ngày** từ lúc nộp. Với bài nộp có `submitted_at < now() - 90 ngày`, endpoint #5 và #11 trả `testcaseDetail = { available: false, unavailableReason: "RETENTION_EXPIRED", items: [] }` và endpoint #6 trả `testcases = []`, **kể cả khi job JOB-JD-04 chưa kịp xoá dòng** (kết quả nhất quán bất kể giờ chạy job). Các số tổng, `sourceCode`, `status` vẫn trả. Bài nộp nào cũng có `available = false` kèm `NOT_APPLICABLE` khi `COMPILE_ERROR` hoặc `SYSTEM_ERROR` trước khi chạy testcase.
- **Bản tin STOMP** chỉ mang `order` và `verdict` (và số đếm); không runtime, không output, không stderr.
- **Hậu điều kiện**: không có đường nào, kể cả endpoint chi tiết giảng viên, trả dữ liệu ẩn.

## BR-JD-12 — Chỉ số Beats (F4-12)

- **Trigger**: dựng `SubmissionDetailDto` (endpoint #5) cho bài nộp `ACCEPTED`. **Không** có ở vùng giảng viên (`USR0201` dòng 539, 548) và **không** ở `problem_detail` (`USR0102` Q11).
- **Công thức** (`02-bd/architecture/judge-orchestration.md:293-310`, RD `01-rd/req/judge-orchestration.md:84-89`):

  ```sql
  SELECT count(*) AS total,
         count(*) FILTER (WHERE id <> :cur AND runtime_ms >= :curRuntimeMs) AS slower_or_equal
  FROM judge.submissions
  WHERE problem_id = :p AND language = :l AND status = 'ACCEPTED';
  ```

  `beatsPercent = ROUND(slower_or_equal * 100.0 / total)`, làm tròn nửa lên tới số nguyên (chi tiết làm tròn là suy luận của BD). Tập so sánh gồm **mọi** bài nộp `ACCEPTED` của cùng bài và cùng ngôn ngữ, của mọi người, kể cả bài hiện tại ở mẫu số, không so giữa ngôn ngữ khác nhau. Nhãn hiển thị "nhanh hơn X%" nghĩa là X% bài `ACCEPTED` khác chậm hơn hoặc bằng bài này.
- **Ngưỡng dữ liệu** (`USR0201` Sheet 9 NO 6, `DEC-2026-1018-usr-open-questions-phase2-5`): `total < 10` thì `beatsPercent = null`, `beatsSampleSize = total`; giao diện in "Chưa đủ dữ liệu so sánh". Hằng số `judge.beats.min-sample = 10`.
- **Verdict khác `ACCEPTED`**: `beatsPercent = null` và `beatsSampleSize = null` (hiển thị `-`).
- **Không phải cột lưu sẵn**: tính lúc đọc. Chỉ mục BD `submissions(problem_id, language, status) WHERE status = 'ACCEPTED'` phục vụ đếm; truy vấn phải đọc thêm `runtime_ms`. Quy mô capstone chấp nhận được; nếu đo thấy chậm thì bổ sung `runtime_ms` vào chỉ mục (không tối ưu trước, như BD ghi cho cửa sổ 364 ngày ở mục 9).

## BR-JD-13 — Số liệu nộp bài (`GetMySubmissionMetrics`, `GetClassStudentSubmissionMetrics`)

- **Trigger**: endpoint #9 và hai cổng trong tiến trình (P1, `MySubmissionMetricsPort` phía `identity`).
- **Quy tắc ngày**: mọi phép cắt ngày dùng **UTC+7 cố định** (`(submitted_at AT TIME ZONE '+07:00')::date`), không dùng múi giờ trình duyệt (`DEC-2026-1018-usr-open-questions-phase2-5`, `USR0501` Q4).
- **A1 `GetMySubmissionMetrics`**:
  1. `userId` lấy từ token; chữ ký không có tham số danh tính.
  2. **`streakDays`**: lấy tập ngày (UTC+7) có ít nhất một lượt nộp của người này, đi **ngược từ hôm nay**; đếm số ngày liên tiếp; gặp ngày trống thì dừng. Hôm nay chưa nộp thì kết quả `0` theo đúng chữ định nghĩa của RD (`USR0501` Q1) (câu hỏi mở Q14). Truy vấn dùng chỉ mục `(user_id, submitted_at DESC)` và dừng ở ngày trống đầu tiên.
  3. **`lastSubmittedAt`**: `MAX(submitted_at)`.
  4. **`dailyCounts[]`**, luôn **đủ phần tử** (ngày trống `count = 0`):
     - `7d`: 7 phần tử, mỗi ngày trong 7 ngày kết thúc hôm nay.
     - `30d`: cửa sổ 30 ngày kết thúc hôm nay, **14 phần tử**. Với `daysAgo` từ 0 đến 29, nhóm số `13 - floor(daysAgo * 14 / 30)` (nhóm cũ nhất là 0); phần lớn nhóm 2 ngày, hai nhóm 3 ngày; `day` của phần tử là ngày cuối nhóm. Quy tắc biên nhóm do DD chốt (BD để ngỏ, `USR0601` dòng 457) `[SoT: Suy luận]`.
     - `all`: gom theo **tuần ISO** (thứ Hai là ngày đầu) từ tuần chứa lượt nộp đầu tiên tới **tuần hiện tại, gồm cả tuần chưa đủ 7 ngày** (khác luồng giảng viên); `day` là thứ Hai của tuần.
  5. **`acRateByTopic[]`** khi có `topicIds`: `ProblemTopicReadPort.getProblemIdsByTopics(topicIds)`; với mỗi chủ đề, đếm lượt nộp **trạng thái cuối** của người này thuộc các bài đó trong cửa sổ `range` (`submissionCount`), trong đó `ACCEPTED` là `acceptedCount`, `acRate = acceptedCount / submissionCount` (null nếu 0), `lastSubmittedAt` là `MAX`. Một bài thuộc nhiều chủ đề được đếm ở từng chủ đề.
  6. Mẫu số "mọi lượt nộp ở trạng thái cuối, kể cả `SYSTEM_ERROR` và `COMPILE_ERROR`" để khớp tập `SubmissionGraded` mà `identity` đếm (BR-JD-14); nếu chủ dự án muốn loại `SYSTEM_ERROR` thì phải đổi **cả hai** nơi (câu hỏi mở Q14).
- **A2 `GetClassStudentSubmissionMetrics(classId, studentIds[], problemIds[], weekCount)`** (`DEC-2026-1014-instructor-batch2-weekly-rules`):
  1. Việc gác quyền thuộc `identity` (cổng không có route HTTP).
  2. Phạm vi dữ liệu: lượt nộp của từng `studentId` cho `problemIds` (bài đã giao cho lớp). `problemIds` rỗng thì mọi học viên trả giá trị 0 và null. Không áp cửa sổ `joined_at..left_at` (chữ ký không mang; câu hỏi mở Q19).
  3. **Tuần**: số liệu theo tuần ISO, **tính lúc truy vấn** từ `judge.submissions`, **không** có bảng ảnh chụp theo tuần và **không** có job định kỳ. **Tuần đang chạy chưa đủ 7 ngày không được tính**; `weeklyTrend[]` gồm đúng `weekCount` tuần đủ gần nhất, cũ nhất trước, tuần không có lượt nộp vẫn là phần tử với `submissionCount = 0` và `acceptedRatio = null`.
  4. `streakDays`, `lastSubmittedAt`, `acceptedRatio` tính trên cùng phạm vi (bài đã giao), `acceptedRatio` bằng `ACCEPTED / mọi lượt nộp trạng thái cuối`.
  5. Mốc "Vắng bài" (7 ngày gần nhất) là cửa sổ trượt tính từ lúc truy vấn, **không** theo tuần ISO; do `identity` suy từ `lastSubmittedAt`.
  6. **Bộ nhớ đệm Redis 15 phút**, khoá gồm `classId`, băm `studentIds` và `problemIds`, `weekCount` và ngày đầu của tuần hiện tại. Không có cơ chế vô hiệu chủ động (chấp nhận số liệu cũ tối đa 15 phút). Redis không sẵn sàng thì tính trực tiếp, không lỗi (suy giảm êm).
  7. Không trả mã nguồn, nội dung testcase hay kết quả từng testcase.
- **Sự kiện**: không có (chỉ đọc).

## BR-JD-14 — Sự kiện miền phát ra qua Transactional Outbox

- **Trigger**: BR-JD-05 bước 7 (và JOB-JD-01 khi sweep kết thúc bài nộp).
- **Các bước dựng dòng outbox** (cùng giao dịch với `UPDATE submissions`):
  1. `outbox_events (id = uuid, aggregate_type = 'Submission', aggregate_id = submissionId, event_type, payload JSONB, created_at = now(), published_at = NULL, publish_attempts = 0)`. `id` chính là `eventId` trong phong bì.
  2. **`SubmissionGraded`**: mọi trạng thái cuối. Payload theo mục 25.2 file API. `firstAttemptForProblem = NOT EXISTS (SELECT 1 FROM judge.submissions WHERE user_id = :u AND problem_id = :p AND (submitted_at, id) < (:submittedAt, :id))`, tính **tại lúc ghi sự kiện** (chỉ mục `(user_id, problem_id, submitted_at DESC)`); lượt nộp đầu tiên từ trước tới nay của người cho bài mới là `true` (`03-dd/api/identity.md:251`). Quy ước `submitted_at` bằng nhau thì so `id`.
  3. **`SubmissionAccepted`**: chỉ khi `ACCEPTED`.
  4. `passedTestcaseRatio`, `passedCount`, `totalCount` là `null` khi `COMPILE_ERROR` và `SYSTEM_ERROR`.
- **Quy tắc**: không publish thẳng RabbitMQ từ giao dịch chấm (loại phương án `AFTER_COMMIT`, `02-bd/architecture/judge-orchestration.md:263-270`); relay JOB-JD-02 lo việc đó, ngoài luồng chấm. Một broker chậm hay chết không kéo dài luồng chấm.
- **Khử trùng và giao hàng**: at-least-once; consumer khử trùng theo `eventId`. Thứ tự giữa các bài nộp khác nhau không được đảm bảo; trong một bài nộp chỉ có hai sự kiện độc lập nên không cần thứ tự.
- **Phiên bản payload**: `schemaVersion = 1`; thêm trường tuỳ chọn không đổi số.
- **Kết quả**: `identity` cập nhật `user_problem_best_score`, `user_submission_stats`, `user_daily_submission_stats`; `problem-bank` cập nhật `problem_stats`; `ai-review` mở điều kiện F5. Việc chấm không phụ thuộc đường AI: F5 hoặc broker lỗi không ảnh hưởng trạng thái bài nộp.

## BR-JD-15 — Giám sát hàng đợi và sự kiện hạ tầng (`ADM0401`, `ADM0404`)

- **Trigger**: endpoint #13 đến #15, #18, #20 và bộ lập lịch 5 giây của kênh `/topic/admin/queue`.
- **Ảnh chụp** (`GetQueueSnapshot`):
  1. Đọc `cluster_settings` (một dòng, `id = 1`).
  2. Đếm `submissions` theo `status = 'PENDING'` (chỉ mục `(status, updated_at)`).
  3. **Độ trễ chờ trung bình** = `avg(started_grading_at - submitted_at)` của job đã bắt đầu chấm trong **một giờ gần nhất**; không có job thì `null` (không hiển thị 0,0s).
  4. **Danh sách worker** từ Prometheus (`timeout` 2 giây). Không trả lời hoặc go-judge không xuất metric: `workers = null`, `workersAvailable = false`; giao diện ẩn khối thay vì bịa số (tiêu chí ponytail bậc 2). **Việc phải làm trước khi code**: kiểm go-judge có xuất các metric mã worker, tải, số job, uptime, vCPU hay không (`ADM0401` Q2).
  5. `status` (dải trạng thái), theo thứ tự ưu tiên: `PAUSED` nếu `queue_paused`; `WORKER_OVERLOADED` nếu có worker tải trên 85%; `BACKLOGGED` nếu số `PENDING` vượt `autoscale_job_threshold`; còn lại `NORMAL`.
- **Danh sách job** (`ListQueueJobs`): `status IN ('PENDING','COMPILING','JUDGING')` cộng `SYSTEM_ERROR` có `finished_at >= now() - error_retention_hours` (chỉ ảnh hưởng danh sách, **không xoá** `submissions`). Thứ tự: khoá `COALESCE(started_grading_at, submitted_at)` tăng dần (chờ lâu nhất trước), job `SYSTEM_ERROR` xếp cuối. Kích thước trang `cluster_settings.jobs_per_page`. `waitingMillis = now() - submitted_at` nếu chưa có `started_grading_at`, ngược lại `now() - started_grading_at`; `null` với job lỗi. Bộ đếm ba tab: `waiting` (`PENDING`), `failed` (`SYSTEM_ERROR` trong cửa sổ giữ), `all` (job chưa cuối cộng `failed`).
- **Nguồn sự kiện hạ tầng** (dùng chung cho `GetInfraSummary` và `ListInfraEvents`, để hai nơi không lệch):
  1. `outbox_events` có `publish_attempts >= 3`: `WARN` ở 3 đến 4 lần, `ERROR` từ 5 lần (hằng số `judge.infra.outbox-warn-attempts` và `outbox-error-attempts`); `service = 'outbox'`; `occurredAt = created_at`.
  2. `submissions` ở `SYSTEM_ERROR`: `ERROR`, `service = 'go-judge'`, `occurredAt = finished_at`.
  3. Cửa sổ lưu **90 ngày** (`ADM0404` Q3, Q5); thẻ tóm tắt đếm 24 giờ gần nhất.
  4. `message` dựng ở tầng ứng dụng từ khuôn câu và các trường có sẵn (`publish_attempts`, `event_type`, `failure_reason`, `id`), không có cột `message`; khuôn chỉ chứa định danh hệ thống, không chứa dữ liệu người dùng. Mức `INFO` hiện chưa có nguồn.
- **Lô kênh quản trị**: `{ snapshot, jobCounts, infraSummary }` mỗi 5 giây; **một** lần tính cho mọi người xem (không tính theo người).
- **Suy giảm êm**: Prometheus lỗi thì `workers = null`, còn lại vẫn đúng; không endpoint nào ở đây chạm luồng nộp và chấm.
- **Chỉ đọc** ngoại trừ các lệnh đã nêu ở BR-JD-08 và BR-JD-09.

## BR-JD-16 — Cổng đọc cho module khác

- **`JudgeConfigReadPort`** (cho `problem-bank`): `getGeneratorSandboxLimits()`, `getEnabledLanguages()`, `getGradingDefaults()`; mỗi lần gọi đọc DB, không cache tĩnh dài hạn; `problem-bank` **không** đọc thẳng bảng của `judge` (`database/judge-orchestration.md:159`).
- **`JudgeStatsPort.GetAdminSubmissionStats`** (cho `identity`, `ADM0101`): các phép đếm gom từ `judge.submissions` theo UTC+7 (tổng, theo ngôn ngữ theo mốc thời gian, năm verdict trên tổng đã có kết quả cuối, theo ngày 7 cột, theo tháng 6 điểm, bài phổ biến); bài chưa cuối không tính vào mẫu số verdict; `SYSTEM_ERROR` không có lát riêng (`02-bd/screens/admin/ADM0101_overview.md:363`). `identity` lưu bộ nhớ đệm 60 giây phía họ (`03-dd/logic/identity.md` BR-17), module này **không** cache thêm.
- **Hai cổng số liệu nộp bài** của BR-JD-13.
- **Ràng buộc chung**: không cổng nào trả mã nguồn, nội dung testcase hay kết quả từng testcase; không cổng nào cho `identity` hay `problem-bank` ghi vào schema `judge`.

## Tham chiếu

`03-dd/api/judge-orchestration.md` (endpoint tương ứng từng BR), `03-dd/jobs/judge-orchestration.md`, `03-dd/validation/judge-orchestration.md`, `02-bd/architecture|database|security/judge-orchestration.md`, `03-dd/logic/identity.md` (BR-17, BR-19, BR-24), `DEC-2026-0823-go-judge-default-engine`, `DEC-2026-0912-judge-callback-contract`, `DEC-2026-0912-judge-outbox-pattern`, `DEC-2026-0831-partial-score-testcase-ratio`, `DEC-2026-0828-remove-rejudge-scope`, `DEC-2026-0927-submission-metrics-two-ports`, `DEC-2026-1010-admin-operations-regroup`.
