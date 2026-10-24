# BD — Database module `ai-review` (F5)

> Schema PostgreSQL: `ai`. Đọc cùng `02-bd/architecture/F5_ai-review.md` trước — đặc biệt mục 4 (hai luồng
> nghiệp vụ), mục 5 (ba lớp instruction), mục 6 (kiểm soát chi phí).
>
> Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4): quy ước xoá của file này: thực thể gốc của schema `ai` (`prompt_templates`, `solution_reviews`, `interview_sessions`, `ai_cache_entries`...) **không bao giờ bị xoá cứng** (prompt chỉ chuyển `ARCHIVED`, báo cáo và phiên giữ lâu dài làm lịch sử), nên mọi FK nội schema là `NO ACTION` (mặc định, không `ON DELETE CASCADE`); chỉ `outbox_events` bị job dọn sau 90 ngày (mục 1.10) và dòng đó không có FK. `[SoT: Suy luận]` cho câu tổng quát này.

## 1. Bảng chính

**Quy ước kiểu cột tham chiếu (bổ sung 2026-10-23, `DEC-2026-1023-db-actor-review-fixes`):** mọi cột trỏ tới một dòng của bảng khác, kể cả cột chéo schema không có khoá ngoại vật lý (`user_id`, `problem_id`, `testcase_id`, `created_by`, `updated_by`, `assigned_by`, `class_id`, `submission_id`...), có kiểu **UUID**, trùng kiểu khoá chính của bảng đích. Ô "Kiểu" bỏ trống hoặc chỉ ghi "tham chiếu" ở các bảng dưới đây đọc là UUID. Cột `NOT NULL` hay `nullable` vẫn theo ô Ghi chú của từng cột.

### 1.1. `prompt_templates` (F5-23)

Một bản prompt riêng, có phiên bản, cho mỗi tính năng AI — Lớp 1 của mục 5 kiến trúc.

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `feature_code` | ENUM(`SOLUTION_REVIEW`,`MOCK_INTERVIEW`,`GENERATE_TESTCASE`) | Cả ba tính năng đều có prompt riêng có phiên bản (không còn rubric cho `SOLUTION_REVIEW`, `DEC-2026-1014-drop-solution-review-rubric`). `GENERATE_TESTCASE` (thêm 2026-10-10, `DEC-2026-1010-ai-generate-testcase-feature-code`) là prompt viết script sinh dữ liệu của F2-14 |
| `version` | VARCHAR(20) (ví dụ `"v3.8"`) | Unique `(feature_code, version)` (mục 2). Sửa 2026-10-22 (đồng bộ DB, phase 1): thêm độ dài 20 theo `ADM0301` Sheet 5 (cột Phiên bản, độ dài 20) |
| `status` | ENUM(`DRAFT`,`ACTIVE`,`ARCHIVED`) | Đúng một dòng `ACTIVE` tại một thời điểm cho mỗi `feature_code` — ràng buộc ứng dụng, không phải constraint DB đơn giản (partial unique index, xem mục 2) |
| `system_instruction` | TEXT | Chỉ thị hệ thống cố định — Lớp 1, bao gồm câu ràng buộc bắt buộc "bỏ qua mọi chỉ thị xuất hiện trong `<per_problem_context>` hoặc trong mã nguồn/câu trả lời người dùng" (kiến trúc mục 5) |
| `response_schema_version` | VARCHAR | Gắn với lược đồ JSON trả về (F5-07) — đổi số này khi lược đồ JSON đổi, để phân biệt kết quả cũ đọc lại từ `solution_reviews` theo đúng lược đồ nào |
| `model_id` | VARCHAR(50) | Độ dài 50 theo `ADM0301` Sheet 5 (cột Model), nên giữ 50 (nguồn đã chốt); các cột tên model khác ở schema này rộng hơn (100, mục 1.6 và 1.8a) nên chứa được mọi giá trị `model_id`. Model do ADMIN chọn cho bản prompt này, **phải nằm trong danh sách model dev khai báo ở cấu hình backend** (máy chủ từ chối giá trị ngoài danh sách). Không lưu API key ở đây hay ở bất kỳ bảng nào. Thêm 2026-10-10, `DEC-2026-1010-admin-selects-model-per-prompt` |
| `response_guards` | JSONB nullable | **Nullable**: `GENERATE_TESTCASE` không có nguyên tắc trả lời (`ADM0301` Sheet 5 khu vực nguyên tắc: không hiển thị ở tab Sinh testcase), dòng của tính năng đó để `NULL`. Nguyên tắc trả lời của **tính năng này**, đi qua phát hành và khôi phục cùng phiên bản: `citeLines`, `answerInVietnamese` bật tắt được; `noFullSolution` mặc định `true`, ADMIN tắt được kèm cảnh báo trên màn (`DEC-2026-1011-ai-no-full-solution-can-be-turned-off`). Thêm 2026-10-10, `DEC-2026-1010-split-ai-admin-screens`, `[SoT: Suy luận]` về kiểu cột; Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4): chốt **JSONB** (cùng kiểu mọi cột JSON khác của schema: `result_json`, `topics`, `payload`) `[SoT: Suy luận]`, nguồn chỉ nói "JSON" |
| `created_by` | tham chiếu `identity.users.id`, không FK vật lý | Actor A3, gác bởi `AI_CONFIG` (F1-12) |
| `created_at` / `published_at` | TIMESTAMPTZ / nullable | `published_at` set khi chuyển `DRAFT` → `ACTIVE` |

Không có bảng nào cho "Chạy đối chiếu" (regression test 30 bài mẫu): tính năng đã bỏ hẳn cả giao diện lẫn
backend 2026-10-23 (`DEC-2026-1023-drop-ai-regression-run`), nên không có bảng lưu kết quả đối chiếu.

Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4) (PB-07): **ngữ cảnh theo bài** (`problems.ai_brief`, `no_full_code`, `socratic_only`, `02-bd/database/F2_problem-bank.md` mục 1.1) **không được sao chép** vào schema `ai`. `ai-review` đọc chúng lúc sinh (Solution Review, Mock Interview) qua `ProblemContextPort` do `problem-bank` cung cấp (`02-bd/architecture/F5_ai-review.md` mục 3.3 và 3.4), nên đổi cột ở `problem-bank` có hiệu lực ngay ở lần sinh kế tiếp. Chỉ thị bậc hai: dữ liệu đó vào khối nhãn riêng, không vào `system_instruction`. Hệ quả cache: kết quả đã lưu ở `ai_cache_entries` không tự vô hiệu khi ba cột này đổi, vì khoá cache (mục 1.7) không chứa chúng `[SoT: Suy luận]`; chủ dự án quyết có cần thêm vào khoá hay không, xem mục 7.

### 1.2. ~~`rubric_configs`~~ — Đã bỏ 2026-10-14

**Đã bỏ 2026-10-14** (`DEC-2026-1014-drop-solution-review-rubric`, thay `DEC-2026-1014-drop-admin-rubric-config` và mục (5) của
`DEC-2026-1014-instructor-batch5-6-nav-pagination-rubric`): không còn bảng `rubric_configs`, không còn
trọng số, không còn danh sách tiêu chí, và báo cáo bài giải F5.1 **không có rubric nào** (không có 5 tiêu chí cố định). Rubric của F5 chỉ còn Mock Interview (F5-15, mục 1.5). Giữ số mục 1.2 để các tham chiếu mục 1.3 đến 1.5 không lệch.

### 1.3. `solution_reviews` (F5-01 → F5-08)

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `submission_id` | tham chiếu `judge.submissions.id`, không FK vật lý xuyên schema | Đúng một `solution_review` cho một `submission_id` — unique |
| `user_id` | tham chiếu `identity.users.id`, không FK vật lý | Chốt quyền sở hữu (`02-bd/security/F5_ai-review.md` mục 2) |
| `problem_id` | tham chiếu `problem.problems.id`, không FK vật lý | |
| `language` | ENUM(`JAVA`,`CPP`,`PYTHON`) | Ngôn ngữ của bài nộp được phân tích. Thêm 2026-10-22 (đồng bộ DB, phase 1): `USR0301` hiển thị nhãn ngôn ngữ của báo cáo (`solutionReview.language.*`, `02-bd/screens/users/USR0301_solution_review.md:513`; kiểu `languageKey` ở `05-coding/frontend/src/entities/solution-review/model/types.ts:41`). Chọn thêm cột thay vì để màn đọc xuyên schema `judge.submissions`. Không dùng để tra cache: cache tra ở `ai_cache_entries` (mục 1.7) |
| `source_hash` | CHAR(64) | SHA-256 mã nguồn đã chuẩn hoá (kiến trúc mục 6.2) |
| `prompt_template_id` | FK → `prompt_templates.id` | Phiên bản prompt đã dùng để sinh báo cáo này — cho phép audit "báo cáo này chấm theo bản prompt nào" |
| `result_json` | JSONB | Toàn bộ báo cáo F5-07 (độ phức tạp, so sánh optimum, edge case và rủi ro, nhận xét chất lượng mã, chủ đề gợi mở) — **không có `criteriaScores`** (bỏ theo `DEC-2026-1014-drop-solution-review-rubric`); lược đồ **đã đặc tả ở mục 1.3a** (2026-10-18), gắn version qua `prompt_templates.response_schema_version` |
| `readability_score` | SMALLINT (1-5) | F5-05 — điểm dễ đọc AI tự chấm trong cùng lượt, không gọi thêm lần nào |
| `served_from_cache` | BOOLEAN NOT NULL default `false` | `true` nếu dòng này được tạo từ một lần **cache hit** (không gọi LLM lần này) — phục vụ thống kê tiết kiệm chi phí. Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4) (AI-11): cache hit **tạo dòng `solution_reviews` mới**, quy tắc đầy đủ ở đoạn "Cache hit" dưới mục 1.3a. Kiểu NOT NULL default `false` `[SoT: Suy luận]` |
| `created_at` | TIMESTAMPTZ | |

### 1.3a. Lược đồ `solution_reviews.result_json` (đặc tả 2026-10-18, `DEC-2026-1018-usr-open-questions-phase2-5`)

Quyết định yêu cầu đặc tả lược đồ trước khi viết DD, vì `USR0301` và `USR0601` đang đọc cùng một khối JSON. Bảng 1.3a
liệt kê khoá và kiểu, chỉ gồm khoá mà một màn hình hoặc RD F5 thật sự cần (tiêu chí ponytail bậc 1 và 2). Đường dẫn
của `USR0301` lấy từ Sheet 5 khu vực B, D, F, G của `02-bd/screens/users/USR0301_solution_review.md`; yêu cầu nội
dung lấy từ `01-rd/req/F5_ai-review.md` F5-02 đến F5-07 (mục "F5.1 — Phân tích bài giải").

| Khoá | Kiểu | Bắt buộc | Nguồn yêu cầu | Ghi chú |
| :--- | :--- | :-: | :--- | :--- |
| `schemaVersion` | string | Có | Cột `prompt_templates.response_schema_version` | Bản sao để đọc được JSON cũ khi lược đồ đổi `[SoT: Suy luận]` — ứng dụng ghi khi lưu, không do LLM sinh |
| `approachTitle` | string, tối đa 60 ký tự | Có | `USR0301` thẻ "Hướng giải thuật" (path `approachTitle`) | Tên hướng giải AI nhận diện |
| `verdict` | enum `OPTIMAL` / `IMPROVABLE` / `NOT_OPTIMAL` | Có | `USR0601` khu vực K NO 3 ("đã tối ưu / còn cải thiện được / chưa tối ưu") | Ba giá trị theo chính ba nhãn của màn. F5-03 chỉ nói "gợi ý hướng tốt hơn nếu chưa tối ưu" nên số mức là **suy luận** `[SoT: Suy luận]`: dashboard đòi chấm 3 màu, không có nguồn nào khác nêu số mức |
| `isApproachOptimal` | boolean | Có | `USR0301` thẻ "Hướng giải thuật" (path `isApproachOptimal`) | `true` khi và chỉ khi `verdict = OPTIMAL`; ứng dụng kiểm khi lưu, nếu LLM trả hai khoá mâu thuẫn thì lấy `verdict` |
| `complexity.actualTime` | string, tối đa 30 | Có | F5-02; `USR0301` khu vực D NO 1 | Ký hiệu Big-O. **`USR0601` khu vực K NO 6 ("timeComplexity") đọc đúng khoá này**, không có khoá `timeComplexity` riêng ở gốc |
| `complexity.optimalTime` | string, tối đa 30, nullable | Không | F5-03; `USR0301` khu vực D NO 2 | `null` khi `problems.known_optimal_complexity` là NULL (kiến trúc mục 3.3: bỏ bước so sánh optimum) |
| `complexity.timeExplanation` | string, tối đa 500 | Có | F5-02 "kèm lập luận"; `USR0301` khu vực D NO 3 | |
| `complexity.actualSpace` | string, tối đa 30 | Có | F5-02; `USR0301` khu vực D NO 4 | |
| `complexity.optimalSpace` | string, tối đa 30, nullable | Không | F5-03; `USR0301` khu vực D NO 5 | `null` cùng điều kiện `optimalTime` |
| `complexity.spaceExplanation` | string, tối đa 500 | Có | F5-02; `USR0301` khu vực D NO 6 | |
| `strengths` | mảng string | Có (có thể rỗng) | F5-05; `USR0301` khu vực F NO 1 | |
| `improvements` | mảng string | Có (có thể rỗng) | F5-05; `USR0301` khu vực F NO 2 | |
| `edgeCases` | mảng string | Có (có thể rỗng) | F5-04; `USR0301` khu vực F NO 3 | Ca biên, giả định ngầm, tràn số, rủi ro khi dữ liệu lớn |
| `followUpTopics` | mảng string | Có (có thể rỗng) | F5-06 (RD: chủ đề/từ khoá gợi ý, giao diện tự dựng liên kết sang `interview_bank_list`) | Tên khoá là **suy luận**; chỉ là chủ đề/từ khoá, **không** chứa id câu hỏi F6 (`ai-review` không đọc `interview-bank`). `USR0301` Sheet 5 Khu vực F NO 4 hiển thị khoá này (thêm 2026-10-23, `DEC-2026-1023-db-actor-review-decisions`) |
| `codeDiff.suggestedCode` | string, tối đa 50000 | Không | F5-26; `USR0301` khu vực G NO 1 | `null` khi hướng giải đã tối ưu (không có gì để đề xuất). **Lệch tên với `USR0301` G NO 1 đang ghi path `suggestedCode` ở gốc** — xem "Cần chủ dự án / màn hình chốt" dưới |
| `codeDiff.explanation` | string, tối đa 1000 | Không | F5-26; `USR0301` khu vực G NO 2 (path `codeDiff.explanation`) | Có mặt cùng lúc với `codeDiff.suggestedCode` |

Không đưa vào JSON (có nguồn riêng, tránh trùng): `readability_score` (cột riêng, mục 1.3, F5-05), `problemTitle`,
`levelCode`, `levelDisplayName` (đọc từ `problem-bank` qua `problem_id`), `originalCode` (là `judge.submissions.source_code`),
`diffText` (Monaco Diff Editor tự tính từ hai chuỗi), `createdAt` (cột `created_at`). **Không có `criteriaScores`**
(`DEC-2026-1014-drop-solution-review-rubric`).

`summaryText` (có trong `SolutionReviewDetailDto` của `USR0301` mục 4.2) **không đưa vào** lược đồ: không item nào của
Sheet 5 `USR0301` hiển thị nó và RD F5-07 không nêu `[SoT: Suy luận]`, tiêu chí ponytail bậc 1. **Chốt 2026-10-23:** không thêm thẻ này (`DEC-2026-1023-db-actor-review-decisions`).

Cần chủ dự án / màn hình chốt (lệch giữa lược đồ này và BD màn hình, BD màn hình do agent khác sửa nên không sửa ở đây):

Sửa 2026-10-22 (đồng bộ DB, phase 1): đã xoá điểm 1 cũ (lệch tên `suggestedCode` / `codeDiff.suggestedCode`) — `USR0301` V1.2 đã đổi path sang `codeDiff.suggestedCode` (`02-bd/screens/users/USR0301_solution_review.md:77`, Sheet 5 khu vực G NO 1 dòng 309). Còn lại:

1. `SolutionReviewDetailDto` ở `USR0301` có `timeComplexity`/`spaceComplexity`/`suggestedCodeDiff` ở gốc; DTO phản hồi của API là việc của DD, nhưng phải ánh xạ từ `complexity.actualTime`, `complexity.actualSpace`, `codeDiff`.
2. ~~(Câu hỏi mở, chưa quyết) Prototype `05-coding/frontend/src/entities/solution-review/model/types.ts` còn `suggestedCode` ở gốc, `diffLines`, `diffNote`; lược đồ 1.3a không có ba khoá này (`diffText` do Monaco tự tính). Chọn: sửa prototype theo lược đồ, hoặc thêm khoá vào lược đồ. Không đổi schema ở phiên này.~~ **Đã chốt 2026-10-23 (`DEC-2026-1023-db-actor-review-decisions`):** sửa **prototype** theo lược đồ 1.3a; lược đồ không đổi, không thêm `suggestedCode` ở gốc, `diffLines`, `diffNote` (ponytail bậc 4: Monaco tự tính diff từ hai chuỗi).

Index bắt buộc: unique `(submission_id)`. Sửa 2026-10-22 (đồng bộ DB, phase 1): bỏ index `(problem_id, language_snapshot, source_hash)` cũ — cột `language_snapshot` không tồn tại và việc tra cache theo hash đã có ở unique của `ai_cache_entries` (mục 1.7, tiêu chí ponytail bậc 4: dùng lại cái đã có). Hai bảng phục vụ hai câu hỏi khác nhau: `ai_cache_entries` quyết định **có gọi LLM hay không**, `solution_reviews` là bản ghi lịch sử đầy đủ tra cứu lại từ trang tiến độ (F5-08) kể cả khi cache đã bị dọn.

**Cache hit (AI-11, Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4); `DEC-2026-1022-db-sync-round4` mục 2).** Khi một submission mới có `(problem_id, language, source_hash, prompt_template_id)` trùng một dòng `ai_cache_entries` (mục 1.7): (1) **tạo một dòng `solution_reviews` mới** cho submission đó với `user_id`, `problem_id`, `language`, `source_hash` của lượt nộp mới, `result_json` và `readability_score` **sao chép** từ review gốc, `prompt_template_id` **giữ nguyên** bản prompt của cache entry (không phải bản `ACTIVE` lúc này, vì tra cache luôn kèm đúng bản này), `served_from_cache = true`, `created_at` = lúc hit; (2) `submission_id` vẫn unique, mỗi submission có đúng một review nên trang tiến độ (F5-08) đọc một chỗ duy nhất, không cần bảng ánh xạ; (3) `ai_cache_entries.solution_review_id` **vẫn trỏ review gốc** (dòng sinh bởi LLM), không đổi sang dòng mới; `hit_count` tăng 1; (4) sự kiện `SolutionReviewCompleted` **vẫn được ghi vào `outbox_events` và phát** cho dòng mới (`aggregate_id` = id dòng mới), cùng giao dịch với việc tạo dòng; (5) **không** ghi `token_usage` vì không tốn token (mục 1.6). Đếm lượt hit cho thống kê dùng `served_from_cache = true` hoặc `hit_count`.

### 1.4. `interview_sessions` (F5-09, F5-13, F5-16, F5-24, F5-28)

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | = `interviewSessionId` dùng trong key Redis `ai:chatmemory:<id>` |
| `user_id` | tham chiếu `identity.users.id`, không FK vật lý | |
| `entry_type` | ENUM(`SUBMISSION`,`QUESTION_BANK`,`TOPIC_SELECTION`) | F5-24 |
| `submission_id` | tham chiếu `judge.submissions.id` nullable, không FK vật lý | Chỉ có giá trị khi `entry_type = SUBMISSION` |
| `question_ref` | VARCHAR nullable | F5-24a — định danh câu hỏi F6 do frontend truyền vào, `ai-review` không đọc bảng `interview_bank` (kiến trúc mục 4.2). Sửa 2026-10-22 (đồng bộ DB, phase 2, XS-06): giá trị là `interview_bank.interview_questions.id` dạng chuỗi UUID (định danh công khai), **không** phải mã `IQ-nnn` (`display_no` chỉ hiện ở khu quản trị, `DEC-2026-1022-interview-bank-schema-sync`) |
| `topics` | JSONB nullable | F5-24b — mảng chủ đề thuật toán học viên tick chọn |
| `interviewer_level` | ENUM(`INTERN`,`JUNIOR`,`MIDDLE`,`SENIOR`) nullable | F5-28 — chỉ áp dụng `entry_type IN (QUESTION_BANK, TOPIC_SELECTION)`. 4 mức, thống nhất toàn hệ thống theo `DEC-2026-0922-users-and-admin-conflict-resolutions` mục (2). Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-ai-judge-schema-sync) (AI-03): **chốt cuối**, giữ nguyên enum 4 giá trị, cùng cột `identity.user_preferences.preferred_interview_level`. Hệ quả cho file khác: RD F5-28 và `USR0302` đã đổi sang enum cố định 4 mức (`01-rd/req/F5_ai-review.md:125`; `02-bd/screens/users/USR0302_mock_interview.md` V0.12); dòng V0.10 ở bảng lịch sử còn nói "admin cấu hình" chỉ là lịch sử. Sửa 2026-10-23: đóng việc đồng bộ này |
| `max_turns` | SMALLINT nullable | F5-28 — giới hạn số lượt tối đa; `NULL` khi `entry_type = SUBMISSION` (không giới hạn cứng theo lối vào này) |
| `hint_allowed` | BOOLEAN nullable | F5-28 |
| `stage` | ENUM(`EXPLAIN`,`CHALLENGE`,`SCALE_UP`,`COMPLETED`,`ABANDONED`) | State machine — kiến trúc mục 4.2. Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-ai-judge-schema-sync) (AI-04): `COMPLETED` đạt được bằng **hai đường**: hoàn tất bình thường, và **Kết thúc sớm** (popup "Kết thúc & Chấm điểm", rubric chấm từ các lượt đã có, ghi `rubric_scores`, phát `MockInterviewFinished`). `ABANDONED` **chỉ** khi phiên bỏ trống 15 phút mà chưa đủ lượt để chấm: không bao giờ chấm điểm, không có `rubric_scores`, không phát sự kiện. Sửa 2026-10-22 (đồng bộ DB vòng 2, DEC-2026-1022-db-sync-round2, mục 8): **không có màn kết quả cho `ABANDONED`**; học viên quay lại phiên này thì mở màn vào (`USR0302`) kèm toast giải thích phiên trước đã đóng. Ngưỡng "đủ lượt" là hằng số `application.yml` (chủ dự án chốt 2026-10-22; mặc định đề xuất 4 lượt `[SoT: Suy luận]`, đổi không cần sửa tài liệu) |
| `turn_count` | INT default 0 | Tăng mỗi lượt hội thoại — dùng so với `max_turns` |
| `started_at` | TIMESTAMPTZ | |
| `ended_at` | TIMESTAMPTZ nullable | Set khi vào `COMPLETED` (kể cả Kết thúc sớm) hoặc `ABANDONED`; Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-ai-judge-schema-sync) (AI-04) |
| `updated_at` | TIMESTAMPTZ | |

Index `(stage, updated_at)` (Sửa 2026-10-22 (đồng bộ DB, phase 1), AI-18): job quét bỏ trống lấy các phiên chưa kết thúc (`stage` thuộc `EXPLAIN`/`CHALLENGE`/`SCALE_UP`) có `updated_at` cũ hơn 15 phút. Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-ai-judge-schema-sync) (AI-04): 15 phút là hằng số đã chốt (`DEC-2026-1018-usr-open-questions-phase2-5`, Q3 của `USR0302`); phiên bỏ trống **chưa đủ lượt** thì sang `ABANDONED`, không chấm. Phiên bỏ trống nhưng **đủ lượt** tự chấm điểm và sang `COMPLETED` (chủ dự án chốt 2026-10-22). Ngưỡng "đủ lượt" và chi tiết job nằm ở `03-dd/jobs`. TTL `ChatMemory` Redis đề xuất 2 giờ (mục 3) là số khác, phục vụ giữ ngữ cảnh LLM, vẫn là đề xuất chưa ai xác nhận.

Ràng buộc ứng dụng (không phải CHECK constraint đơn giản vì phụ thuộc điều kiện chéo cột): `entry_type =
SUBMISSION` → `submission_id` bắt buộc, `interviewer_level`/`max_turns`/`hint_allowed` phải `NULL` (F5-28
"không áp dụng cho lối vào từ bài nộp Accepted").

### 1.4a. `interview_turns` (F5-13, F5-16)

Sửa 2026-10-22 (đồng bộ DB, phase 1): chuyển từ mục 3 (Redis) lên thành mục bảng chính; giữ số 1.5 cho `rubric_scores`.

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `interview_session_id` | FK → `interview_sessions.id` | |
| `turn_index` | INT | Thứ tự lượt trong phiên |
| `role` | ENUM(`USER`,`AI`) | |
| `stage_at_turn` | ENUM(`EXPLAIN`,`CHALLENGE`,`SCALE_UP`) | Giai đoạn tại thời điểm lượt này diễn ra |
| `content` | TEXT | |
| `created_at` | TIMESTAMPTZ | |

Unique `(interview_session_id, turn_index)`.

Đây là **nguồn sự thật lâu dài** của hội thoại (F5-16 "lưu phiên... mở lại được từ trang tiến độ") —
Redis `ChatMemory` chỉ phục vụ việc LLM cần ngữ cảnh nhanh trong khi phiên còn mở, không phải nơi tra cứu
lại lịch sử. Khi phiên `COMPLETED`/`ABANDONED`, ứng dụng vẫn giữ nguyên các dòng `interview_turns` đã ghi
theo thời gian thực (ghi mỗi lượt, không đợi kết phiên mới ghi hàng loạt) — Redis hết hạn (TTL) không ảnh
hưởng khả năng xem lại phiên cũ.

### 1.5. `rubric_scores` (chỉ rubric Mock Interview F5-15; từ 2026-10-14 không còn owner `SOLUTION_REVIEW`)

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `owner_type` | ENUM(`MOCK_INTERVIEW`) | Một giá trị duy nhất sau `DEC-2026-1014-drop-solution-review-rubric` (giá trị `SOLUTION_REVIEW` rời enum). Giữ cột để mở rộng và để không đổi khoá unique; **không bao giờ trộn** với `answer_rubric`/F6-13 của `interview-bank`, đó là bảng ở schema khác, module khác |
| `owner_id` | UUID | = `interview_sessions.id` (owner `MOCK_INTERVIEW`) — không FK vật lý xuyên bảng, kiểm ở tầng ứng dụng |
| `criterion_code` | VARCHAR | Cố định 4 giá trị của `MOCK_INTERVIEW` — `CLARITY` (độ rõ ràng), `TECHNICAL_ACCURACY` (độ chính xác kỹ thuật), `PUSHBACK_HANDLING` (khả năng phản biện), `COMPLEXITY_AWARENESS` (nhận thức độ phức tạp) — F5-15 |
| ~~`weight_percent_snapshot`~~ | - | **Đã bỏ cột 2026-10-14** (`DEC-2026-1014-batch7-remaining-open-questions`, tiêu chí ponytail bậc 5): trọng số `25.00`/`30.00`/`25.00`/`20.00` của 4 tiêu chí trên (F5-15) là **hằng số ứng dụng**, gắn với `prompt_templates.response_schema_version` / phiên bản mã; không lưu theo từng dòng điểm. Điểm tổng phiên = Σ `score` × trọng số hằng số / 100. Đổi hằng số = đổi phiên bản lược đồ, không đọc lại điểm cũ bằng snapshot. F5-15 và trọng số của nó **không đổi** |
| `score` | DECIMAL(4,2) | **Thang 0 đến 10** (chốt 2026-10-18, `DEC-2026-1018-usr-open-questions-phase2-5`, thay ghi chú "chốt ở DD"). Giá trị hợp lệ `0.00` đến `10.00`; ứng dụng kiểm khi ghi. Cột `DECIMAL(4,2)` đủ chứa `10.00` |
| `comment` | TEXT | Nhận xét từng tiêu chí — RD yêu cầu bắt buộc có (F5-15 "kèm nhận xét từng tiêu chí") |
| `created_at` | TIMESTAMPTZ | |

Unique `(owner_type, owner_id, criterion_code)` — mỗi tiêu chí chỉ chấm một lần cho một review/phiên.

Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-ai-judge-schema-sync) (AI-04): `rubric_scores` có dòng cho mọi phiên `stage = COMPLETED`, **kể cả phiên Kết thúc sớm** (điểm chấm từ các lượt đã có, nên có thể thấp hơn vì thiếu giai đoạn). Phiên `ABANDONED` **không có dòng nào** và `USR0501` tiếp tục bỏ qua phiên này (chỉ đếm `COMPLETED`, `02-bd/screens/users/USR0501_my_progress.md:307`). Câu "Phiên chưa đủ 4 dòng thì không tính, màn hiển thị `-`" ở trên vẫn đúng cho phiên chấm lỗi dở dang, không phải cho Kết thúc sớm.

**Giá trị suy ra, không có cột (2026-10-18, `DEC-2026-1018-usr-open-questions-phase2-5`):**

- `overallScore` của một phiên = Σ (`score` × trọng số hằng số F5-15 25/30/25/20) / 100, **tính ở tầng ứng dụng** từ đúng 4 dòng `rubric_scores` của phiên; không có cột, không cache (thang 0 đến 10). Phiên chưa đủ 4 dòng thì không tính, màn hiển thị `-`.
- `feedbackSummary` = ghép 4 `comment` của 4 dòng đó khi đọc; **không thêm cột** tóm tắt.
- `recentSessions` của dashboard (`USR0601`, thẻ phiên phỏng vấn gần đây) cần điểm từng tiêu chí cho 4 thanh rubric: dữ liệu **đã có** ở `rubric_scores` (`criterion_code`, `score` theo `owner_id`), chỉ thêm vào DTO, **không thêm cột hay bảng**. "Mục yếu nhất" của dashboard là một trong 4 `criterion_code` có `score` nhỏ nhất.

### 1.6. `token_usage` (F5-21)

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `user_id` | tham chiếu `identity.users.id`, không FK vật lý | |
| `feature_code` | ENUM(`SOLUTION_REVIEW`,`MOCK_INTERVIEW`,`GENERATE_TESTCASE`,`PRACTICE_FEEDBACK`) | Thêm `GENERATE_TESTCASE` 2026-10-10; thêm `PRACTICE_FEEDBACK` 2026-10-11 (`DEC-2026-1011-ai-practice-feedback-in-shared-budget`): một dòng mỗi lượt phản hồi AI của Chế độ luyện F6-08. Chỉ có ở bảng này, **không** thêm vào `prompt_templates` (F6-08 không có prompt admin sửa được) |
| `problem_id` | tham chiếu `problem.problems.id`, không FK vật lý, **nullable** | Sửa 2026-10-22 (đồng bộ DB, phase 1): cần để `ADM0302` xếp hạng "bài tốn token nhất" (`02-bd/screens/admin/ADM0302_ai_usage.md:347`; Q6 `:564` nêu `token_usage` thiếu cột này). Có giá trị với `SOLUTION_REVIEW` và `GENERATE_TESTCASE`; `NULL` với `MOCK_INTERVIEW` và `PRACTICE_FEEDBACK` (không gắn bài của F2) |
| `owner_id` | UUID | = `solution_reviews.id` hoặc `interview_sessions.id` tương ứng `feature_code` (polymorphic, giống mục 1.5); với `GENERATE_TESTCASE` là `problems.id` để cộng token theo bài toán ở `ADM0302` — Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4): giá trị này **trùng** `problem_id` của cùng dòng (cả hai đều là `problems.id`); trùng là có chủ ý, `owner_id` luôn khác NULL (polymorphic) còn `problem_id` là cột để lọc và xếp hạng; với `PRACTICE_FEEDBACK` là `user_answers.id` của lượt nộp (bảng thuộc `interview-bank`, không FK vật lý) |
| `tokens_prompt` | INT | |
| `tokens_completion` | INT | |
| `model_name` | VARCHAR(100) NOT NULL | Tên model LLM thật đã gọi — phục vụ đổi provider mà không mất lịch sử. Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4) (N8): độ dài **100 thống nhất** với `ai_model_prices.model_name` (mục 1.8a) để hai bên so khớp được; `prompt_templates.model_id` giữ 50 theo `ADM0301` (mục 1.1) và là tập con. 100 là `[SoT: Suy luận]`, nguồn không nêu độ dài cho hai cột này |
| `created_at` | TIMESTAMPTZ | Với `MOCK_INTERVIEW` có thể có nhiều dòng (một dòng mỗi lượt hội thoại), với `SOLUTION_REVIEW` đúng một dòng (một lượt), với `GENERATE_TESTCASE` một dòng mỗi lượt sinh — trừ khi dòng `solution_reviews` tương ứng có `served_from_cache = true` (cache hit, mục 1.3a) thì **không có dòng nào** (không tốn token) |

Index (đồng bộ với mục 2; Sửa 2026-10-22 (đồng bộ DB, phase 1), trước đây mục 1.6 và mục 2 mâu thuẫn nhau): `(created_at, feature_code)` — tổng hợp theo chu kỳ cho `ai_token_budget_configs` (mục 1.8) và thống kê theo tính năng; `(user_id, created_at)` — job cảnh báo bất thường (mục 1.9, kiến trúc mục 6.4).

### 1.7. `ai_cache_entries` (F5-20)

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `problem_id` | tham chiếu `problem.problems.id`, không FK vật lý | |
| `language` | ENUM(`JAVA`,`CPP`,`PYTHON`) | Cùng mã nguồn khác ngôn ngữ không hit cache (khác ngôn ngữ luôn khác mã) |
| `source_hash` | CHAR(64) | |
| `prompt_template_id` | FK → `prompt_templates.id` | **Cache theo đúng phiên bản prompt** — đổi prompt (A3 publish bản mới) làm mọi cache cũ hết hiệu lực ngầm định, vì tra cứu luôn kèm điều kiện này; tránh trả kết quả đã lỗi thời theo tiêu chí chấm cũ |
| `solution_review_id` | FK → `solution_reviews.id` | Bản ghi đầy đủ **gốc** (dòng do LLM sinh). Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4) (AI-11): cache hit không đổi cột này sang dòng review mới; xem mục 1.3a |
| `hit_count` | INT default 0 | Tăng 1 mỗi lần một submission khác trùng khoá hit vào dòng này (kèm tạo một dòng `solution_reviews` mới có `served_from_cache = true`, mục 1.3a) — số liệu tiết kiệm chi phí cho `admin_ai_usage` |
| `created_at` | TIMESTAMPTZ | |

Unique `(problem_id, language, source_hash, prompt_template_id)`.

Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4) (AI-11): dòng cache gắn với **review gốc**; các review sinh ra từ cache hit là dòng `solution_reviews` riêng (`served_from_cache = true`) và **không** được thêm dòng `ai_cache_entries` mới (nếu có, unique ở trên sẽ bị vi phạm).

### 1.8. `ai_token_budget_configs` (F5-21, F5-25 — cơ chế tự động khoá)

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | SMALLINT PK, `CHECK (id = 1)` | Singleton hệ thống — đúng một dòng. Sửa 2026-10-22 (đồng bộ DB, phase 1): bỏ UUID và cách nói `is_active` (không có cột đó; `ADM0303` Khu vực B đọc/ghi một cấu hình duy nhất, `02-bd/screens/admin/ADM0303_ai_limits.md:209`, sửa vòng 2 từ :208). `[SoT: Suy luận]`: RD mô tả ngân sách như hạn mức chung của hệ thống, không theo từng người dùng |
| `period` | ENUM(`MONTHLY`) | F5-25 ví dụ "theo tháng" — chỉ một loại chu kỳ ở đợt này |
| `limit_tokens` | BIGINT | Hạn mức |
| `period_start` | DATE | Đầu chu kỳ hiện hành |
| `locked_at` | TIMESTAMPTZ nullable | Set khi vượt hạn mức — chặn `STUDENT`/`INSTRUCTOR`, không chặn `ADMIN` (kiến trúc mục 6.3) |
| `warn_percent` | SMALLINT, `CHECK (warn_percent BETWEEN 1 AND 100)`, default 70 | Sửa 2026-10-22 (đồng bộ DB, phase 1): ngưỡng % mức dùng làm thanh ngân sách ở `admin_ai_usage` đổi màu (`ADM0303` Khu vực D NO 3 đề xuất tên `warn_percent`; mặc định 70 theo `05-coding/frontend/src/entities/ai-budget/model/ai-budget-store.ts:15`). Do `AI_CONFIG` gác, không phải `AI_TOKEN_BUDGET` (`02-bd/security/F5_ai-review.md` mục 4); cùng dòng nhưng quyền ghi khác nhau, ứng dụng kiểm theo cột `[SoT: Suy luận]` |
| `review_per_hour` | SMALLINT, `CHECK (review_per_hour BETWEEN 1 AND 999)`, default 10 | Sửa 2026-10-22 (đồng bộ DB, phase 1): giới hạn Phân tích bài giải mỗi giờ (F5-19; `ai-budget-store.ts:40-44`). Khoảng 1-999 theo `ADM0303` Khu vực C NO 4 `[SoT: Suy luận]`. Lưu ở DB vì Redis mất khi restart sẽ mất cấu hình admin đã đặt; Redis chỉ giữ bộ đếm (mục 3) |
| `interview_per_hour` | SMALLINT, `CHECK (interview_per_hour BETWEEN 1 AND 999)`, default 3 | Giới hạn phiên Mock Interview mỗi giờ (F5-19) |
| `cooldown_seconds` | SMALLINT, `CHECK (cooldown_seconds BETWEEN 1 AND 999)`, default 20 | Giây chờ giữa hai yêu cầu; 20 lấy từ prototype `[SoT: Suy luận]` |
| `updated_by` | tham chiếu user id, không FK vật lý | `ADMIN` sửa: `AI_TOKEN_BUDGET` cho `limit_tokens`/`locked_at`, `AI_CONFIG` cho các cột còn lại |
| `updated_at` | TIMESTAMPTZ | |

Chuyển chu kỳ (AI-17; Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-ai-judge-schema-sync)): **đã chốt** (`DEC-2026-1022-ai-judge-schema-sync` mục 4). Một job `@Scheduled` chạy đầu mỗi tháng đặt `period_start` về ngày 1 của tháng và xoá `locked_at` (mở khoá tự động); job mô tả ở `03-dd/jobs`. `02-bd/architecture/F5_ai-review.md` mục 6.3 chưa nói điều này, cần bổ sung (file khác, không sửa ở đây). `usedTokens` luôn tính từ `token_usage` kể từ `period_start`, nên chỉ cần `period_start` đúng là số dùng tự về 0.

### 1.8a. `ai_model_prices` (`ADM0303` popup đơn giá)

Sửa 2026-10-22 (đồng bộ DB, phase 1): bảng mới. `ADM0303` Q3 (`02-bd/screens/admin/ADM0303_ai_limits.md:494`, sửa vòng 2 từ :491) hỏi nơi lưu đơn giá và đề xuất bảng riêng; không gộp vào singleton vì có nhiều dòng.

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | `ModelPriceDto.id` |
| `model_name` | VARCHAR(100) | Khớp `token_usage.model_name`; so khớp không phân biệt hoa thường, cắt khoảng trắng hai đầu (`ADM0303` mục 4.2 NO 7). Độ dài 100 `[SoT: Suy luận]`, nguồn không nêu; đã thống nhất với `token_usage.model_name` VARCHAR(100) (mục 1.6, Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4)) |
| `usd_per_million_tokens` | NUMERIC(10,4), `CHECK (usd_per_million_tokens >= 0)`, default 0 | USD mỗi 1 triệu token, mặc định 0 cho mô hình mới (`ADM0303` Popup NO 4). Kiểu số `[SoT: Suy luận]` |
| `updated_at` | TIMESTAMPTZ | |

Unique `lower(model_name)`. Không FK từ `token_usage`: xoá mô hình khỏi bảng giá không kiểm tham chiếu, cột Chi phí của dòng liên quan hiện `-` (`ADM0303` Popup NO 5). Đổi tên làm `token_usage` cũ không còn khớp (`ADM0303` Q4). **Chốt 2026-10-23 (`DEC-2026-1023-db-actor-review-decisions`):** chấp nhận, giữ khoá nối bằng tên; dòng cũ hiện `-` ở cột Chi phí, không thêm FK hay cột id (ponytail bậc 5).

### 1.9. `ai_usage_anomaly_alerts` (F5-19/F5-25 amendment, `DEC-2026-0831-ai-usage-anomaly-alert`)

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `user_id` | tham chiếu `identity.users.id`, không FK vật lý | |
| `window_start` / `window_end` | TIMESTAMPTZ | Cửa sổ 24 giờ đã xét |
| `call_count` | INT | Số lượt gọi AI trong cửa sổ |
| `baseline_avg` | DECIMAL(8,2) | Trung bình toàn hệ thống cùng khung giờ, tại thời điểm tính |
| `ratio` | DECIMAL(5,2) | `call_count / baseline_avg` |
| `status` | ENUM(`OPEN`,`REVIEWED`) | `ADMIN` đánh dấu `REVIEWED` sau khi xem ở `admin_ai_usage` — không có hành động tự động nào khác |
| `created_at` / `reviewed_at` | TIMESTAMPTZ / nullable | |

Chống trùng khi job chạy mỗi giờ (AI-16; Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-ai-judge-schema-sync)): **đã chốt** (`DEC-2026-1022-ai-judge-schema-sync` mục 3). Khi đã có một dòng `status = OPEN` của cùng `user_id`, job **không ghi thêm dòng mới**; kiểm ở tầng ứng dụng, không thêm unique cứng ở DB (nên hai lần chạy đồng thời hiếm hoi vẫn có thể tạo hai dòng, chấp nhận được vì đây là cảnh báo mềm). `02-bd/architecture/F5_ai-review.md` mục 6.4 chỉ nói job ghi một dòng `OPEN`, chưa nêu quy tắc này, cần bổ sung (file khác, không sửa ở đây). Sau khi ADMIN đặt `REVIEWED`, job được ghi dòng `OPEN` mới cho user đó ở lần chạy sau.

**Tách bạch khỏi mục 1.8**: bảng này không có cột nào ảnh hưởng khả năng gọi AI của tài khoản — đọc để
hiểu ý đồ thiết kế "cảnh báo mềm, không tự khoá" ngay từ cấu trúc dữ liệu, không cần đọc code.

### 1.10. `outbox_events`

Cùng mẫu thiết kế với `judge.outbox_events` (`02-bd/database/F4_judge-orchestration.md` mục 1.6,
`DEC-2026-0912-judge-outbox-pattern`), bảng riêng trong schema `ai`:

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `aggregate_type` | VARCHAR | `"SolutionReview"` hoặc `"InterviewSession"` |
| `aggregate_id` | UUID | = `solution_reviews.id` hoặc `interview_sessions.id` |
| `event_type` | VARCHAR | `"SolutionReviewCompleted"` hoặc `"MockInterviewFinished"` |
| `payload` | JSONB | Xem bảng payload ở `02-bd/architecture/F5_ai-review.md` mục 3.6 |
| `created_at` | TIMESTAMPTZ | Cùng giao dịch với việc ghi `solution_reviews`/chuyển `interview_sessions.stage = COMPLETED` |
| `published_at` | TIMESTAMPTZ nullable | Relay set sau khi broker ack |
| `publish_attempts` | INT default 0 | |

Index `outbox_events(published_at) WHERE published_at IS NULL`, giống hệt lý do ở `judge-orchestration`.

Sửa 2026-10-22 (đồng bộ DB vòng 2, DEC-2026-1022-db-sync-round2, mục 7): **dọn outbox** cùng mẫu với `judge.outbox_events` (`02-bd/database/F4_judge-orchestration.md` mục 1.6) — dòng có `published_at` cũ hơn 90 ngày bị job xoá, số 90 là hằng số `application.yml`, không thêm cột. `03-dd/jobs/` chưa có file (chưa tạo).

## 2. Chỉ mục (index) đáng chú ý — tổng hợp

- `prompt_templates(feature_code) WHERE status = 'ACTIVE'` — partial unique index, đảm bảo đúng một bản
  đang chạy cho mỗi tính năng.
- `solution_reviews(user_id, created_at DESC)` — trang tiến độ cá nhân (F5-08).
- `interview_sessions(user_id, started_at DESC)` — trang tiến độ, mở lại phiên cũ (F5-16).
- `token_usage(created_at, feature_code)` — tổng hợp theo chu kỳ ngân sách (mục 1.8) và thống kê theo tính năng.
- `token_usage(user_id, created_at)` — cảnh báo bất thường (mục 1.9).
- `interview_sessions(stage, updated_at)` — job quét phiên bỏ trống chuyển `ABANDONED` (mục 1.4).
- `ai_model_prices(lower(model_name))` unique — tên mô hình không trùng (mục 1.8a).
- `prompt_templates(feature_code, version)` unique (mục 1.1).

## 3. Redis

`[SoT: dependency-map.md mục 2]` — bốn vai trò, bốn nhóm key riêng biệt:

| Vai trò | Key pattern | TTL | Ghi chú |
| :--- | :--- | :--- | :--- |
| `ChatMemory` phiên phỏng vấn (F5-13) | `ai:chatmemory:<interviewSessionId>` | Đề xuất 2 giờ kể từ lượt cuối, gia hạn mỗi lượt mới `[SoT: Suy luận]` | Nguồn sự thật ngắn hạn cho ngữ cảnh hội thoại trong phiên đang mở; **không phải nguồn sự thật lâu dài** — lịch sử đầy đủ nằm ở Postgres `interview_turns` (mục 1.4a) |
| Rate-limit theo người dùng (F5-19) | `ai:ratelimit:<userId>:<featureCode>:<epochHour>` (chỉ `SOLUTION_REVIEW`, `MOCK_INTERVIEW`) | Hết hạn sau 1 giờ, đặt ở lần đếm đầu tiên | Ngưỡng đọc từ `ai_token_budget_configs` (mục 1.8), Redis chỉ giữ bộ đếm. **Cửa sổ cố định theo giờ** (INCR), `DEC-2026-1010-ai-rate-limit-fixed-window`. Bộ đếm, không phải nguồn sự thật — mất do Redis restart chỉ làm giới hạn "mở lại" sớm hơn dự kiến, không gây sai dữ liệu nghiệp vụ |
| Chờ giữa hai yêu cầu (F5-19) | `ai:cooldown:<userId>` | Bằng `ai_token_budget_configs.cooldown_seconds` (mục 1.8), ghi bằng SET NX | Khoá còn tồn tại thì từ chối lần gọi kế tiếp |
| Lớp tăng tốc tra cache (tuỳ chọn) | `ai:cachehint:<problemId>:<language>:<sourceHash>` | Đề xuất 24 giờ | Chỉ tăng tốc, Postgres `ai_cache_entries` (mục 1.7) vẫn là nguồn sự thật — thiếu Redis không làm sai kết quả, chỉ chậm hơn |

Ghi chú (Sửa 2026-10-22 (đồng bộ DB, phase 1), AI-25): giới hạn 20 lượt mỗi giờ của Chế độ luyện F6-08 là hằng số riêng và key đếm của nó thuộc `interview-bank` (`02-bd/screens/admin/ADM0303_ai_limits.md:270`, sửa vòng 2 từ :268), không nằm trong bảng trên (bảng này chỉ `SOLUTION_REVIEW`, `MOCK_INTERVIEW`). Việc key ở `ADM0303` mục 4.3 còn thiếu `<epochHour>` thuộc file `ADM0303`, không sửa ở đây.

## 4. Hàng đợi chấm tay

Đã bỏ 2026-10-13 theo `DEC-2026-1013-remove-instructor-manual-grading`.

## 5. Migration — thứ tự tạo bảng

`prompt_templates` (seed 3 dòng ACTIVE ban đầu, một cho mỗi `feature_code`) → `ai_token_
budget_configs` (seed đúng 1 dòng `id = 1`: `warn_percent` 70, `review_per_hour` 10, `interview_per_hour` 3, `cooldown_seconds` 20) → `ai_model_prices` (không phụ thuộc bảng nào, seed rỗng) → `solution_reviews` → `rubric_scores` → `interview_sessions` →
`interview_turns` → `token_usage` → `ai_cache_entries` → `ai_usage_anomaly_alerts` → `outbox_events`.

## 6. Giá trị mặc định BD chốt (RD để ngỏ, `[SoT: Suy luận]`)

**Xác nhận 2026-10-23 (`DEC-2026-1023-db-actor-review-decisions`):** chủ dự án chốt **toàn bộ** giá trị ở bảng dưới theo đề xuất của BD; nhãn `[SoT: Suy luận]` còn lại chỉ ghi nhận rằng RD để ngỏ, không còn là điểm chờ xác nhận. Admin vẫn sửa được các giá trị này trên giao diện nếu cần.

| Tham số | Giá trị | Nguồn RD |
| :--- | :--- | :--- |
| Ngưỡng cảnh báo bất thường | Gấp 5 lần trung bình | RD chỉ nêu ví dụ minh hoạ (gấp 7 lần), không chốt ngưỡng kích hoạt |
| Chu kỳ ngân sách token | Theo tháng | F5-25 nêu ví dụ, BD chọn cố định `MONTHLY` cho đợt này |
| TTL `ChatMemory` Redis | 2 giờ kể từ lượt cuối | RD không nêu, BD đề xuất |
| Rate limit F5.1 / F5.2 | 10 lượt/giờ / 3 phiên/giờ | RD không nêu số, BD đề xuất khởi điểm |

## 7. Việc còn mở — chuyển sang DD

**Sửa 2026-10-14**: bỏ cột `solution_reviews.ai_score_10` (mục 1.3) và quy tắc quy đổi thang 10 theo
`DEC-2026-1014-drop-ai-score-10`.

**Sửa 2026-09-24**: `interview_sessions.interviewer_level` (mục 1.4) trước đây liệt kê 3 giá trị
(`JUNIOR`,`MIDDLE`,`SENIOR`), lệch thời so với `DEC-2026-0922-users-and-admin-conflict-resolutions` mục
(2) đã chốt 4 mức thống nhất toàn hệ thống. Đã sửa thành `ENUM(INTERN,JUNIOR,MIDDLE,SENIOR)`.

- ~~Lược đồ JSON đầy đủ `solution_reviews.result_json` (F5-07) — tên trường, kiểu từng phần.~~ **Đã đặc tả 2026-10-18 ở mục 1.3a** (còn hai điểm lệch với BD `USR0301` và prototype chờ chốt, nêu cuối mục 1.3a); DD chỉ còn việc ánh xạ sang DTO API.
- ~~Thang điểm chính xác của `rubric_scores.score` cho `MOCK_INTERVIEW` (0-10 hay 0-100).~~ **Đã đóng 2026-10-18: thang 0 đến 10** (`DEC-2026-1018-usr-open-questions-phase2-5`, mục 1.5).
- ~~`rubric_scores` của Mock Interview cấu hình được trọng số hay cố định~~ — **đã đóng 2026-09-13: cố
  định 25/30/25/20%, không cấu hình qua `admin_ai_config`.** Căn cứ: `01-rd/screens/admin/ADM0301_ai_config.md`
  dòng 39-42 (lúc đó) xác nhận màn này chỉ cấu hình rubric 5 tiêu chí của Solution Review (F5-23, nay đã bỏ hẳn, mục này giữ làm lịch sử), tự nhận "khác 4
  tiêu chí rubric phỏng vấn — hai rubric riêng cho hai tính năng khác nhau (F5.1 vs F5.2), không xung đột".
  `rubric_scores.owner_type = MOCK_INTERVIEW` do đó không cần cột trọng số cấu hình được — trọng số
  25/30/25/20% là hằng số ứng dụng, không phải dữ liệu cấu hình.
- ~~Danh sách `criterion_code` cụ thể của `rubric_configs` (F5-23)~~ — **đã đóng 2026-10-14
  (`DEC-2026-1014-drop-solution-review-rubric`): không còn bảng và không còn 5 mã nào cho báo cáo bài giải.** Cũng đóng đề xuất thêm/sửa/bật tắt tiêu chí (ADM0301 Q16): không làm.
- ~~Toàn bộ số ở mục 6 cần chủ dự án xác nhận trước khi seed migration thật.~~ **Đã chốt 2026-10-23** (`DEC-2026-1023-db-actor-review-decisions`): giữ nguyên mục 6, kể cả TTL `ChatMemory` 2 giờ ở mục 3.
- Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4) (PB-07): khoá cache `ai_cache_entries` (mục 1.7) không chứa `ai_brief`, `no_full_code`, `socratic_only`; đổi chúng ở `problem-bank` không vô hiệu cache cũ. **Đã chốt 2026-10-22: chấp nhận cache cũ** (không thêm hash vào khoá; ghi rõ giới hạn này).
- ~~AI-03, AI-04, AI-16, AI-17~~ — **đã đóng 2026-10-22** (DEC-2026-1022-ai-judge-schema-sync): `interviewer_level` giữ 4 mức (mục 1.4); Kết thúc sớm cho `COMPLETED` có chấm điểm, `ABANDONED` chỉ khi bỏ trống 15 phút chưa đủ lượt và không chấm (mục 1.4, 1.5); một dòng `OPEN` mỗi user (mục 1.9); job đầu tháng đặt lại chu kỳ (mục 1.8). Ngưỡng "đủ lượt" là hằng số `application.yml` (đã chốt 2026-10-22, mặc định đề xuất 4); phiên bỏ trống đủ lượt tự chấm sang `COMPLETED`.
- ~~Giữ hay bỏ `solution_reviews.ai_score_10`~~ — **đã đóng 2026-10-14 (`DEC-2026-1014-drop-ai-score-10`): bỏ
  cột và quy tắc quy đổi thang 10 theo trọng số rubric** (không màn nào còn dùng sau
  `DEC-2026-1013-remove-instructor-manual-grading`). Giữ `result_json`, `readability_score`; `rubric_scores` chỉ còn cho Mock Interview (`DEC-2026-1014-drop-solution-review-rubric`).
- ~~OQ-F5-B: trọng số rubric admin của F5-23 còn giữ không~~ — **đã đóng 2026-10-14
  (`DEC-2026-1014-instructor-batch5-6-nav-pagination-rubric`, tiêu chí ponytail bậc 2 và 3): bỏ trọng số,
  giữ danh sách tiêu chí admin chỉnh được** (hướng hai trong ba hướng đã nêu). Phần "giữ danh sách tiêu chí
  admin chỉnh được" **bị thay ngày 2026-10-14 bởi `DEC-2026-1014-drop-admin-rubric-config`** (bỏ luôn
  `rubric_configs`); phần bỏ trọng số vẫn đúng; cột `rubric_scores.weight_percent_snapshot` sau đó bị bỏ hẳn ở `DEC-2026-1014-batch7-remaining-open-questions`.
- ~~Câu hỏi: `rubric_scores.weight_percent_snapshot` còn cần cho `MOCK_INTERVIEW` không?~~ — **đã đóng 2026-10-14 (`DEC-2026-1014-batch7-remaining-open-questions`, tiêu chí ponytail bậc 5): chọn hướng (b), bỏ cột**; chủ dự án không nhận đề xuất giữ nullable. Trọng số 25/30/25/20 là hằng số ứng dụng gắn theo `response_schema_version`.
- ~~Cột `problems.known_optimal_complexity` chưa tồn tại ở `problem-bank`~~ — **Sửa 2026-10-22 (đồng bộ DB, phase 1): đã tồn tại** (`02-bd/database/F2_problem-bank.md:26`), ghi chú cũ sai. ~~Còn mở: `perProblemAiContext` chưa có cột nào~~ **đã đóng 2026-10-22 (`DEC-2026-1022-db-sync-round4`, PB-07):** ngữ cảnh theo bài là ba cột `problems.ai_brief`, `no_full_code`, `socratic_only` (`02-bd/database/F2_problem-bank.md` mục 1.1), `ai-review` đọc qua `ProblemContextPort` lúc sinh (ghi chú sau mục 1.1). `02-bd/architecture/F5_ai-review.md` mục 3.3, 3.4 và 10 đã bỏ tên `perProblemAiContext` (kiểm 2026-10-23: `02-bd/architecture/F5_ai-review.md` và `02-bd/security/F5_ai-review.md` dùng ba tên cột).

## 8. Tham chiếu

- `02-bd/architecture/F5_ai-review.md` — kiến trúc, hai luồng nghiệp vụ, ba lớp instruction, kiểm soát chi
  phí.
- `02-bd/database/F4_judge-orchestration.md` mục 1.6 — mẫu Outbox tái dùng.
- `.nexa/domain-registry.json` — domain `ai-review`, scope liệt kê 7 bảng dự kiến.
- `.nexa/control/decision-registry.md` — `DEC-2026-1014-batch7-remaining-open-questions`, `DEC-2026-0831-ai-usage-anomaly-alert`,
  `DEC-2026-1013-remove-instructor-manual-grading`.
