# BD — Database module `interview-bank` (F6)

> Schema PostgreSQL: `interview_bank`. Đọc cùng `02-bd/architecture/interview-bank.md` trước — đặc biệt
> mục 2 (phạm vi), mục 4 (hai luồng nghiệp vụ), mục 5 (hai lớp instruction), mục 6 (spaced repetition).

## 1. Bảng chính

### 1.1. `question_topics` (F6-01)

Danh mục chủ đề dùng chung — 5 giá trị đã chốt, seed cố định.

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `code` | ENUM(`CS_THEORY`,`SYSTEM_DESIGN`,`DATABASE`,`LANGUAGE`,`BEHAVIORAL`) | Tương ứng Lý thuyết CS / System design / Database / Ngôn ngữ / Hành vi [SoT: `01-rd/req/interview-bank.md:9-13`] |
| `display_name` | VARCHAR | Nhãn hiển thị tiếng Việt |
| `sort_order` | SMALLINT | Thứ tự hiện ở bộ lọc |

Seed đúng 5 dòng, không có use case tạo/xoá topic ở đợt này — thay đổi danh mục chủ đề là một quyết định
kiến trúc (đã từng cần một DEC — `DEC-2026-0830-interview-bank-crud` mục 4), không phải một CRUD thường.

### 1.2. `interview_questions` (F6-01, F6-04, F6-05, F6-06, F6-13)

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `topic_id` | FK → `question_topics.id` | F6-01 |
| `difficulty` | ENUM(`EASY`,`MEDIUM`,`HARD`) | F6-01 |
| `title` | VARCHAR | |
| `content_markdown` | TEXT | Nội dung câu hỏi, Markdown |
| `suggested_approach` | TEXT | F6-04 — hướng tiếp cận gợi ý, dữ liệu tĩnh |
| `sample_answer_framework` | TEXT | F6-05 — khung trả lời chuẩn; với `topic_id = BEHAVIORAL` áp dụng cấu trúc STAR (Situation/Task/Action/Result), lưu dạng văn bản có cấu trúc (Markdown 4 mục), không phải 4 cột riêng — vì chỉ câu hành vi mới cần STAR, câu kỹ thuật dùng khung tự do khác |
| `core_keywords` | TEXT[] hoặc JSONB mảng chuỗi | F6-06 — từ khoá kỹ thuật cốt lõi cần nêu |
| `follow_up_questions` | JSONB mảng chuỗi | "Câu hỏi đào sâu" — `DEC-2026-0830-interview-bank-crud` mục 1: truy vấn tiếp theo cùng câu hỏi gốc, **khác** F6-04/05/06 và khác giai đoạn Phản biện F5-11 của `ai-review` (không dùng ở Chế độ luyện của F6, chỉ hiển thị tham khảo ở Chế độ học) |
| `status` | ENUM(`ACTIVE`,`RETIRED`) | F6-13 Q7 — xoá mềm: `RETIRED` ẩn khỏi mọi màn phía học viên, không cascade xoá dữ liệu đã dùng |
| `created_by` | tham chiếu `identity.users.id`, không FK vật lý | Actor A2/A3 |
| `created_at` / `updated_at` | TIMESTAMPTZ | |

Index: `(topic_id, difficulty, status)` — lọc danh sách (F6-02); full-text search trên `title` +
`content_markdown` (GIN + `to_tsvector`, tiếng Việt không dấu hoá ở tầng ứng dụng nếu cần, `[SoT: Suy
luận]` — RD chỉ nói "tìm kiếm" F6-02, không chốt công nghệ).

**Câu hỏi thiếu rubric** (không có dòng nào ở `answer_rubrics`, mục 1.3, khớp `question_id` này): vẫn
`status = ACTIVE` và hiện ở Chế độ học, nhưng bị lọc khỏi danh sách chọn ở Chế độ luyện tại tầng truy vấn
(`WHERE EXISTS (SELECT 1 FROM answer_rubrics WHERE question_id = iq.id)`) — không phải một cột trạng thái
riêng, tính bằng join.

### 1.3. `answer_rubrics` (F6-13 — rubric thứ ba, độc lập với F5-15/F5-23)

Tiêu chí đánh giá có trọng số phần trăm, do người soạn câu hỏi cấu hình — chính là "tiêu chí chuẩn" mà
F6-08 đối chiếu. **Rõ ràng khác** `ai_review.rubric_configs` (F5-23) và `ai_review.rubric_scores` với
`owner_type = MOCK_INTERVIEW` (F5-15) — ba rubric độc lập trong hệ thống
[SoT: `07-review/rd_drift_repair_260909.md` mục 3.1, `DEC-2026-0830-interview-bank-crud` mục 1].

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `question_id` | FK → `interview_questions.id` | Một câu hỏi có nhiều dòng tiêu chí (1-n) |
| `criterion_code` | VARCHAR | Ví dụ `CORRECTNESS`, `COMPLETENESS`, `CLARITY` — `[SoT: Suy luận]`, RD không liệt kê tên tiêu chí cụ thể, chỉ nói "bộ tiêu chí đánh giá có trọng số phần trăm" (`DEC-2026-0830-interview-bank-crud` mục 1); A2/A3 tự đặt tên tiêu chí qua `interview_question_authoring` |
| `description` | TEXT | Mô tả tiêu chí — nội dung có cấu trúc, không phải văn bản tự do hướng dẫn AI (khác Lớp 2 của F5, xem kiến trúc mục 5) |
| `weight_percent` | DECIMAL(5,2) | Tổng các dòng cùng `question_id` phải bằng 100 — kiểm ở tầng ứng dụng khi A2/A3 lưu |
| `created_at` / `updated_at` | TIMESTAMPTZ | |

Unique `(question_id, criterion_code)`. Xoá câu hỏi (mục 1.2, `status = RETIRED`) giữ nguyên các dòng
rubric — không cascade xoá, để các `user_answers` cũ (mục 1.5) vẫn còn ngữ cảnh tra cứu lại tiêu chí đã
chấm tại thời điểm nộp qua `feedback_result_json` (đã lưu snapshot, không tham chiếu sống).

### 1.4. `bookmarks` (F6-03)

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `user_id` | tham chiếu `identity.users.id`, không FK vật lý | |
| `question_id` | FK → `interview_questions.id` | |
| `created_at` | TIMESTAMPTZ | |

Unique `(user_id, question_id)` — bookmark là nhị phân (có/không), không có ghi chú kèm theo (RD không
yêu cầu — F6-03 chỉ nói "đánh dấu để xem lại").

### 1.5. `user_answers` (F6-07, F6-08) — bất biến, không `UPDATE`

Mỗi lần nộp ở Chế độ luyện là một bản ghi độc lập —
[SoT: `DEC-2026-0831-outside-screens-closures`: "câu trả lời đã nộp ở Chế độ luyện không sửa lại được
— mỗi lần nộp là một lượt (attempt) độc lập"].

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `user_id` | tham chiếu `identity.users.id`, không FK vật lý | |
| `question_id` | FK → `interview_questions.id` | |
| `answer_text` | TEXT | Câu trả lời tự do của học viên — luôn là DATA truyền vào AI (kiến trúc mục 5), không bao giờ đọc như instruction |
| `attempt_no` | INT | Thứ tự lượt nộp của chính `user_id` cho chính `question_id`, tăng dần từ 1 — tính ở tầng ứng dụng (đếm số dòng hiện có + 1) khi tạo bản ghi mới, không phải cột tự tăng toàn bảng |
| `feedback_status` | ENUM(`PENDING`,`COMPLETED`,`FAILED`) | Tách rời việc ghi nhận đã nộp khỏi việc có phản hồi AI hay không (kiến trúc mục 8) — `FAILED` khi `AnswerFeedbackPort` lỗi, KHÔNG xoá dòng `user_answers`, chỉ đánh dấu để học viên biết chưa có phản hồi |
| `feedback_result_json` | JSONB nullable | F6-08 — điểm đã đạt / điểm còn thiếu / hướng bổ sung; NULL khi `feedback_status != COMPLETED` |
| `rubric_snapshot_json` | JSONB | Chốt lại toàn bộ `answer_rubrics` của câu hỏi này **tại thời điểm nộp** (không tham chiếu sống) — đổi rubric sau này không làm lệch phản hồi đã chấm trước đó, cùng nguyên tắc `weight_percent_snapshot` của `ai_review.rubric_scores` |
| `created_at` | TIMESTAMPTZ | Không có `updated_at` — bảng này không bao giờ `UPDATE` sau khi tạo, thiếu cột này là chủ ý thiết kế, không phải thiếu sót |

Index: `(user_id, question_id, attempt_no DESC)` — xem lịch sử các lượt của một câu hỏi (F6-09);
`(user_id, created_at DESC)` — lịch sử luyện tập toàn cục. Unique `(user_id, question_id, attempt_no)`.

**Không có bảng `user_answer_versions`/lịch sử sửa** — vì bản thân bảng đã là lịch sử theo thiết kế, mỗi
attempt là một dòng độc lập, không cần một cơ chế versioning chồng lên trên.

### 1.6. `practice_history` (F6-09, F6-10) — bảng tổng hợp

Dẫn xuất từ `user_answers`, nhưng giữ một bảng tổng hợp riêng để truy vấn tiến độ nhanh (không phải
`GROUP BY` toàn bộ `user_answers` mỗi lần tải trang tiến độ).

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `user_id` | tham chiếu `identity.users.id`, không FK vật lý | |
| `topic_id` | FK → `question_topics.id` | |
| `questions_attempted` | INT | Số câu hỏi riêng biệt (không phải số lượt) đã có ít nhất một `user_answers` thuộc topic này |
| `questions_total` | INT | Tổng số câu hỏi `ACTIVE` thuộc topic này tại thời điểm tính — dùng tính tỉ lệ hoàn thành F6-10 |
| `last_practiced_at` | TIMESTAMPTZ | |
| `updated_at` | TIMESTAMPTZ | |

Unique `(user_id, topic_id)`. Cập nhật đồng bộ (cùng giao dịch) mỗi khi ghi một `user_answers` mới —
không cần job nền, vì đây là phép cộng dồn đơn giản theo `(user_id, topic_id)`, không phải một phép tính
tốn kém.

**Định nghĩa "hoàn thành" (F6-10)**: `questions_attempted / questions_total` — "đã thử" (có ít nhất một
lượt nộp), **không** yêu cầu điểm đạt ngưỡng nào, vì F6-08 chỉ là phản hồi định tính (điểm đã đạt/còn
thiếu), không có khái niệm "đạt/không đạt" nhị phân cho một câu hỏi phỏng vấn miệng. `[SoT: Suy luận]` —
RD không định nghĩa chính xác công thức "tỉ lệ hoàn thành theo từng chủ đề" (F6-10), BD chọn định nghĩa
đơn giản nhất khớp dữ liệu đã có; cần chủ dự án xác nhận ở mục 7.

### 1.7. `recall_ratings` (F6-12 — spaced repetition)

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `user_id` | tham chiếu `identity.users.id`, không FK vật lý | |
| `question_id` | FK → `interview_questions.id` | |
| `rating` | ENUM(`KNOWN`,`VAGUE`,`FORGOTTEN`) | Biết rõ / Mơ hồ / Quên [SoT: `01-rd/req/interview-bank.md:32-34`] |
| `interval_days` | INT | Khoảng cách hiện tại (ngày), công thức kiến trúc mục 6 |
| `next_review_at` | TIMESTAMPTZ | `rated_at + interval_days` — dùng lọc danh sách "cần ôn lại" (F6-09) |
| `rated_at` | TIMESTAMPTZ | Lần tự chấm gần nhất — mỗi lần tự chấm **ghi đè** dòng hiện có (khác `user_answers`: đây là trạng thái hiện tại "lịch ôn tập", không phải lịch sử từng lần tự chấm) |

Unique `(user_id, question_id)` — đúng một dòng trạng thái ôn tập cho mỗi cặp người dùng/câu hỏi, **cập
nhật tại chỗ** khi tự chấm lại (khác nguyên tắc bất biến của `user_answers` — hai bảng có ngữ nghĩa khác
nhau: một là lịch sử luyện tập không sửa được, một là trạng thái lịch ôn tập luôn cập nhật theo lần tự
chấm mới nhất). Index `(user_id) WHERE next_review_at <= now()` — truy vấn "cần ôn lại" nhanh.

`[SoT: Suy luận]` — không giữ lịch sử từng lần tự chấm trước đó (chỉ giữ trạng thái mới nhất); nếu chủ dự
án muốn xem biểu đồ "tiến bộ tự chấm theo thời gian" cần thêm bảng lịch sử riêng — mục 7.

### 1.8. `question_sets` — đã đóng 2026-09-13: bỏ hẳn, không tạo bảng

Bản BD 2026-09-12 từng cân nhắc giữ `question_sets`/`question_set_items` ở mức tối giản (xem kiến trúc
mục 2.2). Đã bác bỏ: `01-rd/screens/shared/interview_question_management.md` dòng 97-103 xác nhận màn
quản trị nội dung — nơi duy nhất một khái niệm nhóm câu hỏi có thể xuất hiện — **không có bất kỳ điều
khiển nào liên quan tới "bộ câu hỏi"**. Không tạo `question_sets`, `question_set_items`, hay
`question_set_class_assignment` — cả ba đều ngoài phạm vi.

## 2. Chỉ mục (index) đáng chú ý — tổng hợp

- `interview_questions(topic_id, difficulty, status)` — lọc danh sách F6-02.
- `interview_questions` full-text search trên `title`/`content_markdown` — tìm kiếm F6-02.
- `answer_rubrics(question_id, criterion_code)` unique — cấu hình rubric.
- `bookmarks(user_id, question_id)` unique — F6-03.
- `user_answers(user_id, question_id, attempt_no)` unique, `(user_id, question_id, attempt_no DESC)`,
  `(user_id, created_at DESC)` — lịch sử luyện tập F6-09.
- `practice_history(user_id, topic_id)` unique — tiến độ theo chủ đề F6-10.
- `recall_ratings(user_id, question_id)` unique, `(user_id) WHERE next_review_at <= now()` — danh sách
  cần ôn lại F6-09/F6-12.

## 3. Redis

`[SoT: dependency-map.md dòng 52]` — một vai trò duy nhất, khác `ai-review` (không có `ChatMemory` vì
Chế độ luyện là một lượt hỏi-đáp, không phải hội thoại đa lượt):

| Vai trò | Key pattern | TTL | Ghi chú |
| :--- | :--- | :--- | :--- |
| Rate-limit gọi AI Chế độ luyện (F6-08) | `interview_bank:ratelimit:<userId>` | Bằng độ dài cửa sổ trượt | Namespace riêng, không dùng chung `ai:ratelimit:*` của `ai-review` — hai module không chia sẻ trạng thái. Bộ đếm, không phải nguồn sự thật — mất do Redis restart chỉ làm giới hạn "mở lại" sớm hơn dự kiến |

## 4. Migration — thứ tự tạo bảng

`question_topics` (seed 5 dòng) → `interview_questions` → `answer_rubrics` → `bookmarks` →
`user_answers` → `practice_history` → `recall_ratings`. Không có `question_sets`/`question_set_items` —
đã bỏ khỏi phạm vi (mục 1.8).

## 5. Giá trị mặc định BD chốt (RD để ngỏ, `[SoT: Suy luận]`)

| Tham số | Giá trị | Nguồn RD |
| :--- | :--- | :--- |
| Hệ số nhân khoảng cách ôn tập khi `KNOWN` | ×2.0, trần 60 ngày | `01-rd/req/interview-bank.md:36-37`, chưa chốt công thức |
| Khoảng cách khi `FORGOTTEN` | reset về 1 ngày | Cùng dòng trên |
| Định nghĩa "hoàn thành" F6-10 | đã thử ≥ 1 lượt / tổng câu hỏi topic | RD không định nghĩa công thức chính xác |
| Rate limit Chế độ luyện | đề xuất 20 lượt/giờ/người dùng `[SoT: Suy luận]` | RD không nêu số, tham khảo cùng bậc với F5.1 (10 lượt/giờ) nhưng nới hơn vì phản hồi F6-08 ngắn/rẻ hơn F5.1 |

## 6. Điều kiện đầy đủ danh sách "cần ôn lại" (F6-09)

```sql
SELECT iq.* FROM interview_bank.interview_questions iq
JOIN interview_bank.recall_ratings rr
  ON rr.question_id = iq.id AND rr.user_id = :userId
WHERE iq.status = 'ACTIVE'
  AND rr.next_review_at <= now()
ORDER BY rr.next_review_at ASC
```

Câu hỏi **chưa từng** được tự chấm (không có dòng `recall_ratings`) không nằm trong danh sách này —
danh sách "cần ôn lại" chỉ áp dụng cho câu hỏi đã từng xem qua Chế độ học ít nhất một lần và tự chấm.
`[SoT: Suy luận]` — cách hiểu hợp lý nhất của "cần ôn lại": chỉ ôn lại cái đã học, không phải mọi câu hỏi
chưa từng đụng tới.

## 7. Việc còn mở — chuyển sang DD

- ~~Phạm vi cuối cùng của `question_sets`/`question_set_items`~~ — **đã đóng 2026-09-13: bỏ hẳn**, xem
  mục 1.8.
- Công thức chính xác "tỉ lệ hoàn thành theo chủ đề" (F6-10, mục 1.6) — BD chọn định nghĩa đơn giản, cần
  xác nhận.
- Có giữ lịch sử từng lần tự chấm recall (mục 1.7) hay chỉ trạng thái mới nhất — quyết định ảnh hưởng có
  cần bảng `recall_rating_history` riêng hay không.
- Số cụ thể rate-limit Chế độ luyện (mục 5).
- Danh sách `criterion_code` cụ thể mặc định gợi ý cho A2/A3 khi soạn rubric mới (mục 1.3) — có seed sẵn
  vài tiêu chí phổ biến hay để trống hoàn toàn tự đặt.

## 8. Tham chiếu

- `02-bd/architecture/interview-bank.md` — kiến trúc, hai luồng nghiệp vụ, hai lớp instruction, spaced
  repetition.
- `02-bd/database/ai-review.md` mục 1.1, 1.5 — mẫu `prompt_templates`/`rubric_scores`/snapshot trọng số
  tham chiếu lại ở đây cho `answer_rubrics`/`rubric_snapshot_json`.
- `.nexa/domain-registry.json` — domain `interview-bank`, scope liệt kê 7 thực thể dự kiến.
- `.nexa/control/decision-registry.md` — `DEC-2026-0828-remove-per-class-interview-set`,
  `DEC-2026-0830-interview-bank-crud`, `DEC-2026-0831-outside-screens-closures`.
- `07-review/rd_drift_repair_260909.md` mục 3.1 — ba rubric độc lập, F6-13 là rubric thứ ba.
