# BD — Database module `interview-bank` (F6)

> Schema PostgreSQL: `interview_bank`. Đọc cùng `02-bd/architecture/F6_interview-bank.md` trước — đặc biệt
> mục 2 (phạm vi), mục 4 (hai luồng nghiệp vụ), mục 5 (hai lớp instruction), mục 6 (spaced repetition).

## 1. Bảng chính

**Quy ước kiểu cột tham chiếu (bổ sung 2026-10-23, `DEC-2026-1023-db-actor-review-fixes`):** mọi cột trỏ tới một dòng của bảng khác, kể cả cột chéo schema không có khoá ngoại vật lý (`user_id`, `problem_id`, `testcase_id`, `created_by`, `updated_by`, `assigned_by`, `class_id`, `submission_id`...), có kiểu **UUID**, trùng kiểu khoá chính của bảng đích. Ô "Kiểu" bỏ trống hoặc chỉ ghi "tham chiếu" ở các bảng dưới đây đọc là UUID. Cột `NOT NULL` hay `nullable` vẫn theo ô Ghi chú của từng cột.

### 1.1. `question_topics` (F6-01)

Danh mục chủ đề dùng chung — **dữ liệu do ADMIN quản lý** (đã chốt 2026-10-01, owner uỷ quyền cân nhắc, xem
`DEC-2026-1001-admin-configurable-settings`). 5 dòng ban đầu chỉ là dữ liệu seed khởi tạo, không còn là danh
sách cố định.

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `code` | VARCHAR(64) UNIQUE NOT NULL | Sửa 2026-10-22 (đồng bộ DB, phase 1): thêm độ dài 64 `[SoT: Suy luận]` (slug sinh từ tên tối đa 60 ký tự cộng hậu tố số). Slug ổn định, sinh một lần khi tạo, không đổi khi đổi tên (`display_name`) nên không vỡ tham chiếu. **Không còn là ENUM.** 5 dòng seed giữ nguyên mã `CS_THEORY`, `SYSTEM_DESIGN`, `DATABASE`, `LANGUAGE`, `BEHAVIORAL` (Lý thuyết CS / System design / Database / Ngôn ngữ / Hành vi) [SoT: `01-rd/req/F6_interview-bank.md:9-13`]. Chủ đề tạo mới: slug sinh từ `display_name` (chữ hoa, gạch dưới, hậu tố số nếu trùng) `[SoT: Suy luận]` — DEC chỉ yêu cầu "unique stable slug string", cách sinh cụ thể chốt ở DD |
| `display_name` | VARCHAR(60) NOT NULL | Nhãn hiển thị tiếng Việt; ADMIN đổi tên được. Sửa 2026-10-22 (đồng bộ DB, phase 1): độ dài 60 theo `SHR0301` BD dòng 816 (tên chủ đề tối đa 60 ký tự, `[SoT: Suy luận]` ở đó); duy nhất không phân biệt hoa thường thực thi bằng **unique index `lower(display_name)`** (ràng buộc `UNIQUE` thường trên cột chỉ so khớp phân biệt hoa thường) |
| `sort_order` | SMALLINT NOT NULL | Thứ tự hiện ở bộ lọc; ADMIN sắp xếp lại được. Sửa 2026-10-22 (đồng bộ DB, phase 1): thêm NOT NULL như `problem_levels.sort_order` (`02-bd/database/F2_problem-bank.md:86`) |
| `uses_star_framework` | BOOLEAN NOT NULL default `false` | Bổ sung 2026-10-01: câu hỏi thuộc chủ đề bật cờ này kết xuất khung trả lời chuẩn theo 4 mục STAR (mục 1.2, `sample_answer_framework`). Dòng seed `BEHAVIORAL` có giá trị `true`, các dòng seed khác `false`. ADMIN bật/tắt trong popup quản lý chủ đề; thay cho việc gắn STAR vào mã `BEHAVIORAL` |

Quyền thao tác: **chỉ ADMIN (A3)** tạo, đổi tên, bật/tắt cờ STAR, sắp xếp lại, xoá chủ đề; A2 (INSTRUCTOR) chỉ chọn từ danh
sách có sẵn khi soạn câu hỏi. Không giới hạn số lượng chủ đề. Xoá chủ đề bị **từ chối khi còn bất kỳ
`interview_questions.topic_id` trỏ tới** (tính cả câu `RETIRED`, vì FK vẫn tồn tại): API trả số câu hỏi đang
tham chiếu để UI yêu cầu ADMIN chuyển hoặc xoá các câu đó trước. Bảo vệ ở hai lớp: kiểm ở tầng use case
(trả lỗi nghiệp vụ kèm số đếm) và FK `interview_questions.topic_id` không `ON DELETE CASCADE` làm lưới an
toàn. Quyết định này **thay thế** tiểu quyết định 4 (Q6, "phân loại 5 chủ đề cố định") của
`DEC-2026-0830-interview-bank-crud`; ghi lại như lịch sử, không còn hiệu lực.

### 1.1a. `question_levels` (F6-01)

Danh mục độ khó dùng chung — **dữ liệu do ADMIN quản lý**, cùng cách xử lý với `question_topics` (đã chốt 2026-10-03,
`DEC-2026-1001-admin-configurable-settings` mục 6). Thay cho ENUM `EASY`/`MEDIUM`/`HARD` từng nằm ở
`interview_questions.difficulty`; ba mức cũ chỉ còn là dữ liệu seed khởi tạo.

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `code` | VARCHAR(64) UNIQUE NOT NULL | Sửa 2026-10-22 (đồng bộ DB, phase 1): thêm độ dài 64 `[SoT: Suy luận]`, cùng `question_topics.code`. Slug ổn định, sinh một lần khi tạo, không đổi khi đổi tên. **Không phải ENUM.** 3 dòng seed giữ mã `EASY`, `MEDIUM`, `HARD` (Dễ / Trung bình / Khó). Mức tạo mới: slug sinh từ `display_name` theo cùng quy tắc với `question_topics.code` (cách sinh chốt ở DD) |
| `display_name` | VARCHAR(60) NOT NULL | Nhãn hiển thị tiếng Việt; ADMIN đổi tên được. Sửa 2026-10-22 (đồng bộ DB, phase 1): độ dài 60 theo `SHR0301` BD dòng 820 (tên độ khó tối đa 60 ký tự); duy nhất không phân biệt hoa thường bằng unique index `lower(display_name)` |
| `sort_order` | SMALLINT NOT NULL | Thứ tự hiện ở bộ lọc và ô chọn; ADMIN sắp xếp lại được. Sửa 2026-10-22 (đồng bộ DB, phase 1): thêm NOT NULL như `problem_levels.sort_order` |

**Không có cột màu.** Màu badge không do ADMIN cấu hình: ba mức seed giữ màu cũ ở tầng giao diện, mức ADMIN thêm hiện
màu trung tính (quyết định 2026-10-03). Nếu sau này cần chọn màu thì thêm cột `color_token` kèm danh sách giá trị cho phép.

Quyền thao tác: **chỉ ADMIN (A3)** tạo, đổi tên, sắp xếp lại, xoá độ khó; A2 (INSTRUCTOR) chỉ chọn. Không giới hạn số
lượng. Hai bất biến: (1) xoá bị **từ chối khi còn bất kỳ `interview_questions.level_id` trỏ tới** (tính cả câu `RETIRED`);
API trả số câu đang tham chiếu. (2) **Luôn còn ít nhất một mức**: xoá mức cuối bị từ chối, vì form soạn câu hỏi bắt buộc
chọn độ khó. Hai lớp bảo vệ như `question_topics`: kiểm ở tầng use case (lỗi nghiệp vụ kèm số đếm) và FK
`interview_questions.level_id` không `ON DELETE CASCADE`; bất biến (2) chỉ kiểm ở use case vì SQL không có ràng buộc "tối thiểu một dòng".

### 1.2. `interview_questions` (F6-01, F6-04, F6-05, F6-06, F6-13)

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | Định danh công khai ở mọi nơi (API, route, khoá danh sách phía học viên) |
| `display_no` | INT UNIQUE NOT NULL | Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-interview-bank-schema-sync) (IB-01): số hiệu dễ đọc cho khu quản trị. Lấy từ **sequence ở máy chủ** `interview_bank.interview_question_display_no_seq` `[SoT: Suy luận]` (tên sequence là đề xuất, DD chốt), gán lúc `INSERT`, không đổi, không tái sử dụng khi câu hỏi `RETIRED`. Chỉ hiển thị dạng `IQ-%03d` (ví dụ `IQ-014`) ở khu **ADMIN và INSTRUCTOR** (`02-bd/screens/shared/SHR0301_interview_question_management.md` Q5 dòng 850; `02-bd/screens/shared/SHR0302_interview_question_authoring.md` mục 4.4, tiêu đề "Sửa câu hỏi {mã}"). **Màn học viên không hiện mã** (`02-bd/screens/users/USR0401_interview_bank_list.md` Q3 dòng 605). Không dùng `display_no` làm tham số API hay route; mã chỉ là nhãn, `id` mới là khoá. Thay cho Q5 "nguồn định danh dễ đọc" từng để ngỏ |
| `topic_id` | FK → `question_topics.id` NOT NULL, `ON DELETE NO ACTION` | F6-01. Sửa 2026-10-22 (đồng bộ DB, phase 1): thêm NOT NULL vì form soạn bắt buộc chọn (`SHR0302` BD dòng 380 cho độ khó, cùng quy tắc cho chủ đề) |
| `level_id` | FK → `question_levels.id` NOT NULL, `ON DELETE NO ACTION` | F6-01 — độ khó. Trước 2026-10-03 là cột `difficulty` ENUM(`EASY`,`MEDIUM`,`HARD`); migration ánh xạ ba giá trị cũ sang ba dòng seed cùng mã |
| `title` | VARCHAR(255) NOT NULL | Sửa 2026-10-22 (đồng bộ DB, phase 1): = ô nhập "câu hỏi" ngắn (cột `question` của CSV, bắt buộc, `csv-import.ts:11-20`); `content_markdown` là phần mô tả thêm. Độ dài 255 `[SoT: Suy luận]`. **Đã chốt 2026-10-22** (IB-06, `DEC-2026-1022-interview-bank-schema-sync`): `title` là trường "câu hỏi" ngắn, bắt buộc; `content_markdown` chỉ là mô tả thêm. Cần sửa `SHR0302` Q5 (BD dòng 627) cho khớp, ngoài phạm vi file này |
| `content_markdown` | TEXT nullable | Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-interview-bank-schema-sync) (IB-06): **phần mô tả thêm** của câu hỏi, Markdown, tuỳ chọn; câu hỏi chính nằm ở `title`. Khớp CSV: cột `content` tuỳ chọn, cột `question` bắt buộc (`02-bd/screens/shared/SHR0301_interview_question_management.md` Q11, dòng 856) |
| `suggested_approach` | TEXT nullable | F6-04 — hướng tiếp cận gợi ý, dữ liệu tĩnh, Markdown. Sửa 2026-10-22 (đồng bộ DB, phase 1): cả ba trường `suggested_approach`, `sample_answer_framework`, `core_keywords` chỉ được điền qua **nhập CSV** (cột `suggested_approach`, `core_keywords`, `sample_answer` ở `csv-import.ts:14-18`), màn soạn không có ô nhập; xem `SHR0302` Q3 (BD dòng 625, chưa đóng) |
| `sample_answer_framework` | TEXT nullable | F6-05 — khung trả lời chuẩn; với câu hỏi thuộc chủ đề có `question_topics.uses_star_framework = true` (mục 1.1; chủ đề `BEHAVIORAL` seed bật cờ này) áp dụng cấu trúc STAR (Situation/Task/Action/Result), lưu dạng văn bản có cấu trúc (Markdown 4 mục), không phải 4 cột riêng — vì chỉ câu hành vi mới cần STAR, câu kỹ thuật dùng khung tự do khác |
| `core_keywords` | JSONB nullable (mảng chuỗi) | F6-06 — từ khoá kỹ thuật cốt lõi cần nêu. Sửa 2026-10-22 (đồng bộ DB, phase 1): chốt JSONB mảng chuỗi (cùng kiểu `follow_up_questions`, prototype `string[]`), bỏ phương án `TEXT[]` |
| `follow_up_questions` | JSONB NOT NULL default `'[]'` (mảng chuỗi) | "Câu hỏi đào sâu" — `DEC-2026-0830-interview-bank-crud` mục 1: truy vấn tiếp theo cùng câu hỏi gốc, **khác** F6-04/05/06 và khác giai đoạn Phản biện F5-11 của `ai-review` (không dùng ở Chế độ luyện của F6, chỉ hiển thị tham khảo ở Chế độ học). Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4): NOT NULL default mảng rỗng để chỉ còn một dạng "chưa có" (khác `core_keywords` nullable ở dòng trên, nguồn chưa nói rõ nên đây là đề xuất) `[SoT: Suy luận]` |
| `status` | ENUM(`ACTIVE`,`RETIRED`) NOT NULL default `ACTIVE` | F6-13 Q7 — xoá mềm: `RETIRED` ẩn khỏi mọi màn phía học viên, không cascade xoá dữ liệu đã dùng |
| `created_by` | UUID NOT NULL, tham chiếu `identity.users.id`, không FK vật lý | Actor A2/A3. **Sửa 2026-10-23 (`DEC-2026-1023-db-actor-review-fixes`):** thêm NOT NULL cho cùng quy ước với `problem.problems.author_id`; mọi đường tạo câu hỏi (màn soạn, nhập CSV) đều có người thao tác. Chỉ để hiển thị và kiểm toán, không dùng lọc quyền sửa (A2 và A3 cùng phạm vi, `SHR0301` BD dòng 34), nên **không thêm chỉ mục** và không thêm `updated_by` (ponytail bậc 3) |
| `created_at` / `updated_at` | TIMESTAMPTZ | |

Index: `display_no` unique (từ ràng buộc cột); `(topic_id, level_id, status)` — lọc danh sách (F6-02); full-text search trên `title` +
`content_markdown` (GIN trên `to_tsvector('simple', f_unaccent(title || ' ' || coalesce(content_markdown, '')))`; cấu hình `simple` vì PostgreSQL không có cấu hình tiếng Việt, `unaccent` mặc định chỉ STABLE nên phải bọc hàm IMMUTABLE `f_unaccent` mới lập index được; `[SoT: Suy luận]`, RD chỉ nói "tìm kiếm" F6-02). Sửa 2026-10-22 (đồng bộ DB, phase 1): chốt cấu hình FTS. Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-interview-bank-schema-sync): thêm `coalesce` vì `content_markdown` nay nullable (nối `NULL` làm cả biểu thức thành `NULL`, câu chỉ có `title` sẽ không tìm được).

**Câu hỏi thiếu rubric** (không có dòng nào ở `answer_rubrics`, mục 1.3, khớp `question_id` này): vẫn
`status = ACTIVE` và hiện ở Chế độ học, nhưng bị lọc khỏi danh sách chọn ở Chế độ luyện tại tầng truy vấn
(`WHERE EXISTS (SELECT 1 FROM answer_rubrics WHERE question_id = iq.id)`) — không phải một cột trạng thái
riêng, tính bằng join.

### 1.3. `answer_rubrics` (F6-13 — rubric độc lập với rubric Mock Interview F5-15)

Tiêu chí đánh giá có trọng số phần trăm, do người soạn câu hỏi cấu hình — chính là "tiêu chí chuẩn" mà
F6-08 đối chiếu. **Rõ ràng khác** `ai_review.rubric_scores` (chỉ còn rubric Mock Interview F5-15; rubric bài giải F5-23 và `rubric_configs` đã bỏ, `DEC-2026-1014-drop-solution-review-rubric`) — hai rubric độc lập trong hệ thống
[SoT: `07-review/rd_drift_repair_260909.md` mục 3.1, `DEC-2026-0830-interview-bank-crud` mục 1].

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `question_id` | FK → `interview_questions.id` NOT NULL, `ON DELETE NO ACTION` | Một câu hỏi có nhiều dòng tiêu chí (1-n) |
| `criterion_code` | VARCHAR(100) NOT NULL | Sửa 2026-10-22 (đồng bộ DB, phase 1): **không phải slug**: là tên tiêu chí do A2/A3 tự gõ ở ô "Tên/mã tiêu chí" (`SHR0302` BD dòng 399, 477; không có danh mục gợi ý, Q6) và hiển thị nguyên văn cho học viên (`USR0402` hiện `{criterionCode}: %`, `interview-question-detail-view.tsx:479-493`). Duy nhất không phân biệt hoa thường trong cùng câu hỏi. Độ dài 100 `[SoT: Suy luận]`. RD không liệt kê tên tiêu chí cụ thể (`DEC-2026-0830-interview-bank-crud` mục 1) |
| `description` | TEXT nullable | Sửa 2026-10-22 (đồng bộ DB, phase 1): nullable vì màn soạn không có ô nhập (`SHR0302` Q7, BD dòng 629, chưa đóng). Mô tả tiêu chí — nội dung có cấu trúc, không phải văn bản tự do hướng dẫn AI (khác Lớp 2 của F5, xem kiến trúc mục 5) |
| `weight_percent` | DECIMAL(5,2) NOT NULL, CHECK `BETWEEN 0 AND 100` | Tổng các dòng cùng `question_id` phải bằng 100 — kiểm ở tầng ứng dụng khi A2/A3 lưu (SQL không có ràng buộc tổng theo nhóm). Sửa 2026-10-22 (đồng bộ DB, phase 1): thêm CHECK từng dòng |
| `sort_order` | SMALLINT NOT NULL | Sửa 2026-10-22 (đồng bộ DB, phase 1): thứ tự dòng tiêu chí trong form và trên màn học viên (không có cột này thì thứ tự trả về không ổn định) |
| `created_at` / `updated_at` | TIMESTAMPTZ | |

Unique `(question_id, lower(criterion_code))` và `(question_id, sort_order)`. Xoá câu hỏi (mục 1.2, `status = RETIRED`) giữ nguyên các dòng
rubric — không cascade xoá, để các `user_answers` cũ (mục 1.5) vẫn còn ngữ cảnh tra cứu lại tiêu chí đã
chấm tại thời điểm nộp qua `feedback_result_json` (đã lưu snapshot, không tham chiếu sống).

### 1.4. `bookmarks` (F6-03)

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `user_id` | tham chiếu `identity.users.id`, không FK vật lý | |
| `question_id` | FK → `interview_questions.id` NOT NULL, `ON DELETE NO ACTION` | Sửa 2026-10-22 (đồng bộ DB, phase 1): nêu rõ `ON DELETE` |
| `created_at` | TIMESTAMPTZ | |

Unique `(user_id, question_id)` — bookmark là nhị phân (có/không), không có ghi chú kèm theo (RD không
yêu cầu — F6-03 chỉ nói "đánh dấu để xem lại").

### 1.5. `user_answers` (F6-07, F6-08) — bất biến, không `UPDATE`

Mỗi lần nộp ở Chế độ luyện là một bản ghi độc lập —
[SoT: `DEC-2026-0831-outside-screens-closures`: "câu trả lời đã nộp ở Chế độ luyện không sửa lại được
— mỗi lần nộp là một lượt (attempt) độc lập"].

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | Sửa 2026-10-22 (đồng bộ DB, phase 1): **ứng dụng sinh trước** khi gọi AI và trước khi ghi dòng, vì `ai.token_usage.owner_id` (`PRACTICE_FEEDBACK`) trỏ tới `user_answers.id` (`02-bd/database/F5_ai-review.md:148`); không dùng giá trị mặc định do DB sinh |
| `user_id` | tham chiếu `identity.users.id`, không FK vật lý | |
| `question_id` | FK → `interview_questions.id` NOT NULL, `ON DELETE NO ACTION` | |
| `answer_text` | TEXT NOT NULL | Giới hạn **5000 ký tự** là hằng số cấu hình ở tầng ứng dụng, không phải ràng buộc DB (`USR0402` BD dòng 626, Q3 dòng 681; `DEC-2026-1018-usr-open-questions-phase2-5`). Câu trả lời tự do của học viên — luôn là DATA truyền vào AI (kiến trúc mục 5), không bao giờ đọc như instruction |
| `attempt_no` | INT NOT NULL | Thứ tự lượt nộp của chính `user_id` cho chính `question_id`, tăng dần từ 1 — tính ở tầng ứng dụng (đếm số dòng hiện có + 1) khi tạo bản ghi mới, không phải cột tự tăng toàn bảng |
| `feedback_status` | ENUM(`COMPLETED`,`FAILED`) NOT NULL, không có giá trị mặc định | Sửa 2026-10-23 (`DEC-2026-1023-dd-f6-practice-budget-and-prompt`, đồng bộ chéo giai đoạn 3): **bỏ giá trị `PENDING`** vì bảng ghi một lần, không `UPDATE`, và luồng ghi (`03-dd/api/F6_interview-bank.md` mục 6 bước 9) chỉ ghi `COMPLETED` hoặc `FAILED`; ứng dụng luôn đặt giá trị khi `INSERT`. Ngân sách khoá thì **không ghi dòng nào** (từ chối `429 INB-042`); dòng `FAILED` chỉ có khi AI lỗi hoặc quá hạn. Tách rời việc ghi nhận đã nộp khỏi việc có phản hồi AI hay không (kiến trúc mục 8) — `FAILED` khi `AnswerFeedbackPort` lỗi, KHÔNG xoá dòng `user_answers`, chỉ đánh dấu để học viên biết chưa có phản hồi |
| `feedback_result_json` | JSONB nullable | F6-08 — điểm đã đạt / điểm còn thiếu / hướng bổ sung, **kèm mảng theo từng tiêu chí `criteria[]`** (thêm 2026-10-18, `DEC-2026-1018-usr-open-questions-phase2-5`, ghi chú dưới bảng); NULL khi `feedback_status != COMPLETED` |
| `rubric_snapshot_json` | JSONB NOT NULL | Chốt lại toàn bộ `answer_rubrics` của câu hỏi này **tại thời điểm nộp** (không tham chiếu sống) — đổi rubric sau này không làm lệch phản hồi đã chấm trước đó. Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4): NOT NULL vì snapshot luôn được chốt lúc nộp (câu trên); câu hỏi không có dòng `answer_rubrics` nào thì lưu `'[]'` `[SoT: Suy luận]` |
| `created_at` | TIMESTAMPTZ | Không có `updated_at` — bảng này không bao giờ `UPDATE` sau khi tạo, thiếu cột này là chủ ý thiết kế, không phải thiếu sót |

**Mảng `feedback_result_json.criteria[]` (2026-10-18, `DEC-2026-1018-usr-open-questions-phase2-5`, không thêm cột):** mỗi
phần tử ứng với một dòng `answer_rubrics` của câu hỏi (cùng tập với `rubric_snapshot_json`):

| Khoá | Kiểu | Ý nghĩa |
| :--- | :--- | :--- |
| `criterionCode` | string | = `answer_rubrics.criterion_code` |
| `achievementPercent` | số nguyên 0 đến 100 | **Mức đạt** của tiêu chí đó. **Không** nhân với trọng số |
| `weightPercent` | số, sao chép `answer_rubrics.weight_percent` (từ `rubric_snapshot_json` tại thời điểm nộp) | Chỉ để hiển thị cạnh mức đạt |

Vẫn **không có điểm tổng hợp và không có ngưỡng đạt/không đạt** (phản hồi là định tính cộng phần trăm theo tiêu chí,
`DEC-2026-1018-usr-open-questions-phase1-scope`). Ứng dụng kiểm khi ghi: `achievementPercent` nguyên trong `[0,100]`, một
phần tử cho mỗi `criterionCode` của snapshot, không trùng.

Index: `(user_id, question_id, attempt_no DESC)` — xem lịch sử các lượt của một câu hỏi (F6-09);
`(user_id, created_at DESC)` — lịch sử luyện tập toàn cục; `(question_id)` — gom theo câu hỏi cho `usageCount` và điểm trung bình (thêm 2026-10-23). Unique `(user_id, question_id, attempt_no)`.

**Không có bảng `user_answer_versions`/lịch sử sửa** — vì bản thân bảng đã là lịch sử theo thiết kế, mỗi
attempt là một dòng độc lập, không cần một cơ chế versioning chồng lên trên.

### 1.6. `practice_history` (F6-09, F6-10) — bảng tổng hợp

Dẫn xuất từ `user_answers`, nhưng giữ một bảng tổng hợp riêng để truy vấn tiến độ nhanh (không phải
`GROUP BY` toàn bộ `user_answers` mỗi lần tải trang tiến độ). **Người đọc là `USR0601` (dashboard)**; `USR0501` đã gộp vào `USR0601` (`01-rd/screens/users/USR0501_my_progress.md:5-10`), route `/progress` đã bỏ. Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-interview-bank-schema-sync) (IB-04): bảng **không còn cột `questions_total`**; chỉ lưu phần cộng dồn theo người học (`questions_attempted`, `last_practiced_at`), còn mẫu số lấy lúc đọc.

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `user_id` | tham chiếu `identity.users.id`, không FK vật lý | |
| `topic_id` | FK → `question_topics.id` | |
| `questions_attempted` | INT NOT NULL default 0 | Số câu hỏi riêng biệt (không phải số lượt) đã có ít nhất một `user_answers` thuộc topic này **và câu đó đang `ACTIVE`** (sửa 2026-10-23, `DEC-2026-1023-dd-f6-practice-budget-and-prompt`: câu `RETIRED` không tính vào tử số, khớp mẫu số chỉ đếm `ACTIVE`) |
| `last_practiced_at` | TIMESTAMPTZ | |
| `updated_at` | TIMESTAMPTZ | |

Unique `(user_id, topic_id)`. `topic_id` `ON DELETE NO ACTION`.

**Mẫu số tính lúc đọc** (Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-interview-bank-schema-sync), IB-04): tổng số câu của chủ đề = `COUNT(*)` các `interview_questions` có `status = 'ACTIVE'` theo `topic_id`. Truy vấn xuất phát từ `question_topics`, `LEFT JOIN` sang `interview_questions` (đã lọc `ACTIVE`) và `LEFT JOIN practice_history` theo `(user_id, topic_id)`, nhờ vậy chủ đề ADMIN mới tạo, chưa có câu nào hoặc học viên chưa làm câu nào, vẫn hiện với `0`. Lý do: bản chụp `questions_total` trôi mỗi khi ADMIN thêm, `RETIRED` hoặc chuyển chủ đề câu hỏi (ponytail bậc 4, tính thay vì giữ bản chụp).

**Quy tắc khi `interview_questions.topic_id` đổi** (`[SoT: Suy luận]`, chưa có nguồn nêu): `questions_attempted` là giá trị dẫn xuất, nên cùng giao dịch với việc đổi `topic_id`, ứng dụng **tính lại** `questions_attempted` và `last_practiced_at` cho chủ đề cũ và chủ đề mới, chỉ với những `user_id` có `user_answers` của câu đó (`COUNT(DISTINCT question_id)` và `MAX(created_at)` join `interview_questions` theo `topic_id` hiện tại, **lọc `status = 'ACTIVE'`**). Sửa 2026-10-23: cùng phép tính lại này cũng chạy khi một câu chuyển `ACTIVE` sang `RETIRED` (xoá mềm); phía đọc vẫn kẹp tỉ lệ bằng `min(100, ...)` để dữ liệu lệch tạm thời không bao giờ hiện trên 100 phần trăm. Không cộng/trừ tăng dần vì sẽ lệch khi một người đã làm nhiều lượt cùng câu. Cập nhật đồng bộ (cùng giao dịch) mỗi khi ghi một `user_answers` mới —
không cần job nền, vì đây là phép cộng dồn đơn giản theo `(user_id, topic_id)`, không phải một phép tính
tốn kém.

**Định nghĩa "hoàn thành" (F6-10)** (Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-interview-bank-schema-sync): mẫu số đổi nguồn): `questions_attempted / COUNT câu ACTIVE của chủ đề` (tử số cũng chỉ đếm câu `ACTIVE`, sửa 2026-10-23), mẫu số tính lúc đọc như trên, kết quả kẹp `min(100, ...)`; chủ đề không có câu `ACTIVE` thì tỉ lệ coi là `0`, tránh chia cho 0 — "đã thử" (có ít nhất một
lượt nộp), **không** yêu cầu điểm đạt ngưỡng nào, vì F6-08 chỉ là phản hồi định tính (điểm đã đạt/còn
thiếu), không có khái niệm "đạt/không đạt" nhị phân cho một câu hỏi phỏng vấn miệng. `[SoT: Suy luận]` —
RD không định nghĩa chính xác công thức "tỉ lệ hoàn thành theo từng chủ đề" (F6-10), BD chọn định nghĩa
đơn giản nhất khớp dữ liệu đã có; cần chủ dự án xác nhận ở mục 7.

**Phương án không chọn** (Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-interview-bank-schema-sync)): bỏ hẳn `practice_history` và suy `questions_attempted` thẳng từ `user_answers` (`COUNT(DISTINCT question_id)` join `interview_questions` theo chủ đề, theo `user_id`). Đúng tuyệt đối và không cần quy tắc cập nhật khi đổi chủ đề, nhưng phải quét `user_answers` mỗi lần tải dashboard, trái lý do giữ bảng tổng hợp ở đầu mục. Chủ dự án chỉ quyết bỏ cột `questions_total`, không quyết bỏ bảng.

### 1.7. `recall_ratings` (F6-12 — spaced repetition)

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `user_id` | tham chiếu `identity.users.id`, không FK vật lý | |
| `question_id` | FK → `interview_questions.id` NOT NULL, `ON DELETE NO ACTION` | |
| `rating` | ENUM(`KNOWN`,`VAGUE`,`FORGOTTEN`) NOT NULL | Biết rõ / Mơ hồ / Quên [SoT: `01-rd/req/F6_interview-bank.md:36-37`] (sửa 2026-10-22, trước ghi `:32-34`; RD chỉ nêu ba mức làm ví dụ). Tab "Đang ôn" / "Đã thuộc" của `USR0401` suy từ cột này, xem hai khái niệm dưới bảng |
| `interval_days` | INT NOT NULL | Khoảng cách ôn lại (ngày). **Chốt 2026-10-22 (IB-16, `DEC-2026-1022-interview-bank-schema-sync`):** bảng cố định `rating` → `interval_days`: `FORGOTTEN` 1, `VAGUE` 3, `KNOWN` 7 (khớp prototype, `02-bd/screens/users/USR0401_interview_bank_list.md` Q7 dòng 609); **không cộng dồn** theo lịch sử tự chấm, mỗi lần chấm đặt lại theo đúng bảng; công thức x2 trần 60 ngày cũ bị bỏ. Hằng số ở tầng ứng dụng, không phải cột cấu hình. `02-bd/architecture/F6_interview-bank.md` mục 6 và mục 10 cần sửa theo (ngoài phạm vi file này) |
| `next_review_at` | TIMESTAMPTZ NOT NULL | `rated_at + interval_days` ngày, tính khi ghi — dùng lọc danh sách **"đến hạn"** (F6-09), xem hai khái niệm dưới bảng |
| `rated_at` | TIMESTAMPTZ NOT NULL | Lần tự chấm gần nhất — mỗi lần tự chấm **ghi đè** dòng hiện có (khác `user_answers`: đây là trạng thái hiện tại "lịch ôn tập", không phải lịch sử từng lần tự chấm) |

Unique `(user_id, question_id)` — đúng một dòng trạng thái ôn tập cho mỗi cặp người dùng/câu hỏi, **cập
nhật tại chỗ** khi tự chấm lại (khác nguyên tắc bất biến của `user_answers` — hai bảng có ngữ nghĩa khác
nhau: một là lịch sử luyện tập không sửa được, một là trạng thái lịch ôn tập luôn cập nhật theo lần tự
chấm mới nhất). Index thường `(user_id, next_review_at)` — truy vấn "cần ôn lại" nhanh. Sửa 2026-10-22 (đồng bộ DB, phase 1): bỏ partial index `WHERE next_review_at <= now()` vì `now()` không IMMUTABLE, PostgreSQL từ chối tạo (kiến thức PostgreSQL, không có trong repo).

**Hai khái niệm tách biệt** (Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-interview-bank-schema-sync), IB-03). Hai cụm "cần ôn lại" và "đang ôn" từng bị dùng lẫn; từ nay chỉ dùng hai tên sau:

| Tên | Định nghĩa | Dùng ở |
| :--- | :--- | :--- |
| **đến hạn** | `next_review_at <= now()`, bất kể `rating`. Là điều kiện duy nhất của danh sách F6-09 (SQL ở mục 6) và thứ tự ưu tiên hàng đợi Luyện nhanh | Danh sách F6-09, hàng đợi Luyện nhanh |
| **đang ôn** | Lần tự chấm gần nhất là `VAGUE` hoặc `FORGOTTEN`, bất kể đã tới hạn hay chưa. Đối ngược: **đã thuộc** = `KNOWN`; **chưa xem** = không có dòng `recall_ratings` | Tab lọc trạng thái và nhãn trạng thái của `USR0401` (`02-bd/screens/users/USR0401_interview_bank_list.md` dòng 337, 350, 470) |

Hai tập **giao nhau nhưng không chứa nhau**. Ví dụ rõ nhất: một câu `KNOWN` đã chấm hơn 7 ngày trước có `next_review_at <= now()` nên **đến hạn** (hiện ở danh sách F6-09), nhưng ở tab `USR0401` vẫn là **đã thuộc**, không phải "đang ôn". Ngược lại câu `FORGOTTEN` chấm hôm nay là "đang ôn" nhưng chưa đến hạn tới ngày mai. Tab "Đang ôn" vì vậy **không** thay được danh sách F6-09. Đối chiếu: `USR0401` BD Sheet 7.1 NO 5 (dòng 470) ánh xạ rating sang nhãn đúng với bảng này; RD `USR0401` REQ-08 (`01-rd/screens/users/USR0401_interview_bank_list.md:134`) dùng nhãn "Cần ôn lại"/"Chưa xem" cho hàng đợi, lẫn với "đến hạn" (xem báo cáo, câu hỏi cho chủ dự án).

`[SoT: Suy luận]` — không giữ lịch sử từng lần tự chấm trước đó (chỉ giữ trạng thái mới nhất); nếu chủ dự
án muốn xem biểu đồ "tiến bộ tự chấm theo thời gian" cần thêm bảng lịch sử riêng — mục 7.

### 1.8. `question_sets` — đã đóng 2026-09-13: bỏ hẳn, không tạo bảng

Bản BD 2026-09-12 từng cân nhắc giữ `question_sets`/`question_set_items` ở mức tối giản (xem kiến trúc
mục 2.2). Đã bác bỏ: `01-rd/screens/shared/SHR0301_interview_question_management.md` dòng 97-103 xác nhận màn
quản trị nội dung — nơi duy nhất một khái niệm nhóm câu hỏi có thể xuất hiện — **không có bất kỳ điều
khiển nào liên quan tới "bộ câu hỏi"**. Không tạo `question_sets`, `question_set_items`, hay
`question_set_class_assignment` — cả ba đều ngoài phạm vi.

## 2. Chỉ mục (index) đáng chú ý — tổng hợp

- `interview_questions(display_no)` unique — số hiệu `IQ-%03d` ở khu quản trị (Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-interview-bank-schema-sync)).
- `interview_questions(topic_id, level_id, status)` — lọc danh sách F6-02; đồng thời phục vụ `COUNT` câu `ACTIVE` theo chủ đề cho tiến độ (mục 1.6).
- `interview_questions` full-text search trên `title`/`content_markdown` — tìm kiếm F6-02.
- `answer_rubrics(question_id, lower(criterion_code))` unique và `(question_id, sort_order)` unique — cấu hình rubric.
- `question_topics(lower(display_name))`, `question_levels(lower(display_name))` unique — tên không trùng, không phân biệt hoa thường.
- `interview_questions` FTS: GIN trên `to_tsvector('simple', f_unaccent(...))` (mục 1.2).
- `bookmarks(user_id, question_id)` unique — F6-03.
- `user_answers(user_id, question_id, attempt_no)` unique, `(user_id, question_id, attempt_no DESC)`,
  `(user_id, created_at DESC)` — lịch sử luyện tập F6-09.
- `user_answers(question_id)` — thêm 2026-10-23 (`DEC-2026-1023-dd-sync-audit-and-index`): `usageCount` và điểm trung bình gom theo câu hỏi, không dùng được các chỉ mục bắt đầu bằng `user_id`.
- `practice_history(user_id, topic_id)` unique — tiến độ theo chủ đề F6-10 (dashboard `USR0601`).
- `recall_ratings(user_id, question_id)` unique, `(user_id, next_review_at)` (index thường, không partial vì `now()` không IMMUTABLE) — danh sách
  "đến hạn" F6-09/F6-12.

## 3. Redis

`[SoT: dependency-map.md dòng 52]` — một vai trò duy nhất, khác `ai-review` (không có `ChatMemory` vì
Chế độ luyện là một lượt hỏi-đáp, không phải hội thoại đa lượt):

| Vai trò | Key pattern | TTL | Ghi chú |
| :--- | :--- | :--- | :--- |
| Rate-limit gọi AI Chế độ luyện (F6-08) | `interview_bank:ratelimit:<userId>` | Bằng độ dài cửa sổ trượt | Namespace riêng, không dùng chung `ai:ratelimit:*` của `ai-review` — hai module không chia sẻ trạng thái. Bộ đếm, không phải nguồn sự thật — mất do Redis restart chỉ làm giới hạn "mở lại" sớm hơn dự kiến |

## 4. Migration — thứ tự tạo bảng

Trước khi tạo `interview_questions`: tạo sequence `interview_bank.interview_question_display_no_seq` `[SoT: Suy luận]` (tên) và hàm `f_unaccent`. `question_topics` (seed 5 dòng khởi tạo, sau đó ADMIN thêm/đổi tên/xoá qua API) → `question_levels` (seed 3 dòng, cùng cách) → `interview_questions` → `answer_rubrics` → `bookmarks` →
`user_answers` → `practice_history` → `recall_ratings`. Không có `question_sets`/`question_set_items` —
đã bỏ khỏi phạm vi (mục 1.8).

**Ghi chú migration Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-interview-bank-schema-sync):** (1) `display_no`: thêm cột nullable, gán `nextval` cho các dòng `interview_questions` hiện có theo thứ tự `created_at, id` (backfill; bảng trống thì không có gì để gán), rồi đặt `NOT NULL` và `UNIQUE`, và đặt `DEFAULT nextval(...)` để mọi `INSERT` sau tự nhận số. Backfill chạy một lần. (2) `content_markdown`: bỏ `NOT NULL` nếu đã có; dòng cũ giữ nguyên giá trị. Dòng cũ chưa có `title` ngắn không có nguồn tự sinh `[SoT: Suy luận]`; dự án greenfield nên chưa có dữ liệu thật cần xử lý. (3) `practice_history`: bỏ cột `questions_total` nếu đã tạo.

## 5. Giá trị mặc định BD chốt (RD để ngỏ, `[SoT: Suy luận]`)

**Xác nhận 2026-10-23 (`DEC-2026-1023-db-actor-review-decisions`):** chủ dự án chốt **toàn bộ** giá trị ở bảng dưới theo đề xuất của BD; nhãn `[SoT: Suy luận]` còn lại chỉ ghi nhận rằng RD để ngỏ, không còn là điểm chờ xác nhận. Admin vẫn sửa được các giá trị này trên giao diện nếu cần.

| Tham số | Giá trị | Nguồn RD |
| :--- | :--- | :--- |
| Khoảng cách ôn lại theo mức tự chấm | `FORGOTTEN` 1 ngày, `VAGUE` 3 ngày, `KNOWN` 7 ngày; `next_review_at = rated_at + interval_days`; không cộng dồn. Thay công thức ×2.0 trần 60 ngày cũ (IB-16) | `01-rd/req/F6_interview-bank.md:37-40` (RD chỉ nói "kiểu SM-2 đơn giản"). Chủ dự án chốt 2026-10-22, `DEC-2026-1022-interview-bank-schema-sync` |
| Định nghĩa "hoàn thành" F6-10 | đã thử ≥ 1 lượt / số câu `ACTIVE` của chủ đề, mẫu số tính lúc đọc (IB-04) | RD không định nghĩa công thức chính xác |
| Rate limit Chế độ luyện | đề xuất 20 lượt/giờ/người dùng `[SoT: Suy luận]` | RD không nêu số, tham khảo cùng bậc với F5.1 (10 lượt/giờ) nhưng nới hơn vì phản hồi F6-08 ngắn/rẻ hơn F5.1 |

## 6. Điều kiện đầy đủ danh sách "đến hạn" (F6-09)

```sql
SELECT iq.* FROM interview_bank.interview_questions iq
JOIN interview_bank.recall_ratings rr
  ON rr.question_id = iq.id AND rr.user_id = :userId
WHERE iq.status = 'ACTIVE'
  AND rr.next_review_at <= now()
ORDER BY rr.next_review_at ASC
```

**"Đến hạn" chỉ xét `next_review_at <= now()`, không xét `rating`** (Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-interview-bank-schema-sync), IB-03): câu `KNOWN` quá hạn vẫn nằm trong danh sách này dù ở tab `USR0401` nó là "đã thuộc"; không lẫn với tab "đang ôn" (`rating` là `VAGUE` hoặc `FORGOTTEN`), xem mục 1.7. Ví dụ: chấm `VAGUE` lúc `T` thì `next_review_at = T + 3 ngày`; chấm lại `KNOWN` lúc `T2` thì ghi đè thành `T2 + 7 ngày`, không phụ thuộc lần trước.

Câu hỏi **chưa từng** được tự chấm (không có dòng `recall_ratings`) không nằm trong danh sách này —
danh sách "đến hạn" chỉ áp dụng cho câu hỏi đã từng xem qua Chế độ học ít nhất một lần và tự chấm.
`[SoT: Suy luận]` — cách hiểu hợp lý nhất của "đến hạn ôn lại": chỉ ôn lại cái đã học, không phải mọi câu hỏi
chưa từng đụng tới.

## 7. Việc còn mở — chuyển sang DD

- ~~Phạm vi cuối cùng của `question_sets`/`question_set_items`~~ — **đã đóng 2026-09-13: bỏ hẳn**, xem
  mục 1.8.
- Công thức chính xác "tỉ lệ hoàn thành theo chủ đề" (F6-10, mục 1.6) — BD chọn định nghĩa đơn giản, cần
  xác nhận. Mẫu số đã chốt tính lúc đọc (IB-04, 2026-10-22); phần còn mở chỉ là quy tắc cập nhật khi đổi chủ đề câu hỏi và việc có giữ bảng `practice_history` hay không (mục 1.6).
- Có giữ lịch sử từng lần tự chấm recall (mục 1.7) hay chỉ trạng thái mới nhất — quyết định ảnh hưởng có
  cần bảng `recall_rating_history` riêng hay không.
- Quản lý chủ đề (mục 1.1, chốt 2026-10-01): (a) cách sinh slug `code` cho chủ đề mới (còn mở, chốt ở DD);
  (b) ~~quy tắc STAR gắn với mã `BEHAVIORAL`~~ — **đã chốt 2026-10-01 (owner uỷ quyền), xem
  `DEC-2026-1001-admin-configurable-settings`**: STAR là cờ `question_topics.uses_star_framework`, `BEHAVIORAL`
  xoá/đổi tên được như mọi chủ đề; (c) ~~`topics` của problem-bank (F2-02) chưa đổi~~ — **đã chốt cùng cách
  xử lý**, xem `02-bd/database/F2_problem-bank.md` mục 1.2.
- Quản lý độ khó (mục 1.1a, chốt 2026-10-03): cách sinh slug `code` cho mức mới (còn mở, chốt ở DD, cùng câu hỏi với chủ đề); màu badge không cấu hình (đã chốt), mở lại nếu chủ dự án cần.
- ~~Công thức ôn tập, `title`, mã hiển thị `IQ-nnn`, `questions_total`, hai nghĩa của "cần ôn lại"~~ — **đã đóng 2026-10-22**, `DEC-2026-1022-interview-bank-schema-sync` (mục 1.2, 1.6, 1.7, 6). Việc còn lại nằm ở tài liệu khác (kiến trúc mục 6, 10; `SHR0302` Q5; `USR0401`).
- ~~Số cụ thể rate-limit Chế độ luyện (mục 5).~~ **Đã chốt 2026-10-23** (`DEC-2026-1023-db-actor-review-decisions`): 20 lượt/giờ/người dùng.
- ~~Danh sách `criterion_code` cụ thể mặc định gợi ý cho A2/A3 khi soạn rubric mới (mục 1.3) — có seed sẵn vài tiêu chí phổ biến hay để trống hoàn toàn tự đặt.~~ **Đã chốt 2026-10-23** (`DEC-2026-1023-open-questions-closed`): giữ rubric câu hỏi, không seed gợi ý, người soạn tự đặt tên.

## 8. Tham chiếu

- `02-bd/architecture/F6_interview-bank.md` — kiến trúc, hai luồng nghiệp vụ, hai lớp instruction, spaced
  repetition.
- `02-bd/database/F5_ai-review.md` mục 1.1, 1.5 — mẫu `prompt_templates`/`rubric_scores`/snapshot trọng số
  tham chiếu lại ở đây cho `answer_rubrics`/`rubric_snapshot_json`.
- `.nexa/domain-registry.json` — domain `interview-bank`, scope liệt kê 7 thực thể dự kiến.
- `.nexa/control/decision-registry.md` — `DEC-2026-1022-interview-bank-schema-sync`, `DEC-2026-0828-remove-per-class-interview-set`,
  `DEC-2026-0830-interview-bank-crud`, `DEC-2026-0831-outside-screens-closures`.
- `07-review/rd_drift_repair_260909.md` mục 3.1 — ba rubric độc lập, F6-13 là rubric thứ ba.
