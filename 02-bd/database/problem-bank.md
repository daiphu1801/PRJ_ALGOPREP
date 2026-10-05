# BD — Database module `problem-bank` (F2)

> Schema PostgreSQL: `problem`. Đọc cùng `02-bd/architecture/problem-bank.md` trước — đặc biệt mục 2
> (bảng ánh xạ mã F2-nn) và mục 5 (cờ `function_wrapper_supported`).

## 1. Bảng chính

### 1.1. `problems`
Cốt lõi F2-01, F2-02, F2-15, F2-16.

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `code` | VARCHAR unique | Mã ngắn hiển thị (ví dụ `TWO-SUM`), dùng cho URL/tra cứu nhanh |
| `title` | VARCHAR | |
| `statement_md` | TEXT | Nội dung đề bằng Markdown + LaTeX (F2-01) |
| `level_id` | UUID FK → `problem_levels.id`, NOT NULL | F2-02, độ khó. Từ 2026-10-03 là danh mục **do ADMIN quản lý** (mục 1.2a), thay cho ENUM `EASY`/`MEDIUM`/`HARD` từng nằm ở cột `difficulty` (`DEC-2026-1001-admin-configurable-settings` mục 7). FK **không** `ON DELETE CASCADE`. Độ khó chỉ phân loại, không điều khiển điểm hay giới hạn tài nguyên |
| `status` | ENUM(`UNPUBLISHED`,`PUBLISHED`) | F2-15 — đúng hai trạng thái, không có `HIDDEN` (`DEC-2026-0830-problem-lifecycle-two-states`) |
| `deleted` | BOOLEAN default `false` | Ẩn mềm (`DEC-2026-0831-problem-management-lifecycle-details`) — xoá không đổi `status`, chỉ bật cờ này; bài `deleted=true` không hiện ở `problem_management` lẫn `problem_list` |
| `function_wrapper_supported` | BOOLEAN default `true` | F3-13 — tự tính khi lưu `problem_spec`, xem kiến trúc mục 5 |
| `time_limit_ms` | INT | F2-10, mốc cơ sở trước khi nhân hệ số ngôn ngữ |
| `memory_limit_mb` | INT | F2-10 |
| `max_output_size_kb` | INT | F2-10 amendment (`DEC-2026-0831-problem-authoring-round2`), per-problem |
| `max_submissions_per_hour` | INT nullable | F2-10 amendment, per-problem; NULL = không giới hạn riêng, dùng mặc định hệ thống |
| `duplicated_from_problem_id` | FK → `problems.id` nullable | F2-16 — vết tích nhân bản, không bắt buộc dùng ở UI |
| `known_optimal_complexity` | VARCHAR nullable | Bổ sung 2026-09-12 theo yêu cầu BD `ai-review` (F5-03 — Solution Review đối chiếu độ phức tạp bài giải với optimum đã biết). Dạng chuỗi ký hiệu Big-O (`O(n)`, `O(n log n)`...), do giảng viên nhập khi soạn đề (`problem_authoring`); NULL = chưa khai báo, F5.1 bỏ qua bước so sánh optimum cho bài đó thay vì suy đoán `[SoT: Suy luận]` |
| `author_id` | tham chiếu `identity.users.id`, không FK vật lý xuyên schema, NOT NULL | Bổ sung 2026-10-01 (`SHR0201` Q1, `DEC-2026-1001-admin-configurable-settings`): người tạo bài, **bất biến** — gán một lần khi tạo, không bao giờ đổi (khác `updated_by` đổi theo lần sửa gần nhất). Nhân bản (F2-16) gán `author_id` = người nhân bản. Dùng cho bộ lọc "Bài của tôi" của A2; ADMIN thấy và sửa mọi bài. Lưu ý: yêu cầu "FK users" của quyết định được thực hiện như tham chiếu id kiểm ở tầng ứng dụng, đúng nguyên tắc không FK xuyên schema bên dưới `[SoT: Suy luận]`. Index `problems(author_id)` |
| `updated_by` | FK → user (tham chiếu id, không FK cứng liên schema) | F2-10 amendment Q7(g) — trường phẳng, không phải audit trail đầy đủ |
| `created_at` / `updated_at` | TIMESTAMPTZ | `updated_at` dùng cho ngưỡng "chưa xuất bản quá 7 ngày" |
| `published_at` | TIMESTAMPTZ nullable | Mốc xuất bản lần gần nhất |
| `ai_testcase_generations_used` | INT NOT NULL default 0 | Bổ sung 2026-10-03 (`DEC-2026-1003-ai-testcase-generation-quota`, `SHR0202` Q17 và Q18): số lần "Sinh tự động" (F2-14) đã chạy **thành công** cho bài này. Chỉ lần thêm được ít nhất 1 testcase nháp mới cộng; lần lỗi không cộng, duyệt hoặc loại nháp không hoàn lại. Bộ đếm do máy chủ giữ, client không đếm. Mốc trần là tham số ADMIN toàn hệ thống (`ADM0301` Khu vực H, nơi lưu: `ADM0301` Q12), không lưu ở bảng này |

`[SoT: Suy luận]` — `updated_by` tham chiếu `user_id` bên schema `identity` nhưng **không đặt FOREIGN KEY
vật lý xuyên schema** (đúng nguyên tắc modular monolith — mỗi schema độc lập migration); ràng buộc toàn vẹn
kiểm ở tầng ứng dụng khi ghi.

### 1.2. `topics` / `problem_topics` (F2-02)

Danh mục chủ đề bài toán là **dữ liệu do ADMIN quản lý**, cùng cách xử lý với `interview_bank.question_topics`
(đã chốt 2026-10-01, owner uỷ quyền, xem `DEC-2026-1001-admin-configurable-settings`; thay cho câu "seed cố
định theo giáo trình" trước đó). Các chủ đề ban đầu (ví dụ "Mảng", "Cây", "Đồ thị"...) chỉ là dữ liệu seed.

`topics(id UUID PK, code VARCHAR UNIQUE NOT NULL, name VARCHAR UNIQUE NOT NULL, sort_order SMALLINT, created_at)`:
`code` là slug ổn định sinh một lần khi tạo, không đổi khi đổi `name`; `name` duy nhất không phân biệt hoa thường;
`sort_order` là thứ tự hiện ở bộ lọc. `problem_topics(problem_id FK, topic_id FK)`, many-to-many, unique
`(problem_id, topic_id)`; FK `topic_id` **không** `ON DELETE CASCADE`.

Quyền thao tác: **chỉ ADMIN (A3)** tạo, đổi tên, sắp xếp lại, xoá chủ đề; A2 (INSTRUCTOR) chỉ chọn từ danh sách
có sẵn khi soạn đề. Không giới hạn số lượng chủ đề. Xoá bị **từ chối khi còn bất kỳ `problem_topics.topic_id` trỏ
tới** (tính cả bài `UNPUBLISHED` và bài `deleted = true`, vì FK vẫn tồn tại): API trả số bài đang tham chiếu để
UI yêu cầu ADMIN chuyển hoặc bỏ gán các bài đó trước. Bảo vệ hai lớp: kiểm ở use case (lỗi nghiệp vụ kèm số
đếm) và FK không cascade làm lưới an toàn. Mọi thao tác ghi `system_audit_logs` (F1-14). Cách sinh slug `code`
chốt ở DD `[SoT: Suy luận]` — quyết định chỉ nói "giống interview topics".

### 1.2a. `problem_levels` (F2-02)

Danh mục độ khó bài toán — **dữ liệu do ADMIN quản lý**, cùng cách xử lý với `topics` (mục 1.2) và với `interview_bank.question_levels`
(`02-bd/database/interview-bank.md` mục 1.1a), nhưng là **bảng riêng, không dùng chung** với `question_levels`: F2 và F6 là hai
bounded context độc lập và ADMIN có thể muốn nhãn khác nhau ở mỗi bên (`DEC-2026-1001-admin-configurable-settings` mục 7, chốt 2026-10-03).
Ba mức cũ `EASY`/`MEDIUM`/`HARD` chỉ còn là dữ liệu seed khởi tạo.

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `code` | VARCHAR UNIQUE NOT NULL | Slug ổn định, sinh một lần khi tạo, không đổi khi đổi tên. **Không phải ENUM.** 3 dòng seed giữ mã `EASY`, `MEDIUM`, `HARD` (Dễ / Trung bình / Khó). Mức tạo mới: slug sinh từ `display_name` theo cùng quy tắc với `topics.code` (cách sinh chốt ở DD) |
| `display_name` | VARCHAR UNIQUE NOT NULL | Nhãn hiển thị tiếng Việt; ADMIN đổi tên được. Duy nhất không phân biệt hoa thường |
| `sort_order` | SMALLINT NOT NULL | Thứ tự hiện ở bộ lọc, ô chọn; ADMIN sắp xếp lại được. **Sắp xếp theo độ khó (cột "Độ khó" ở danh sách) đi theo `sort_order`**, không theo tên hay mã |

**Không có cột màu.** Ba mức seed giữ màu badge cũ ở tầng giao diện, mức ADMIN thêm hiện màu trung tính. Nếu sau này cần chọn màu thì thêm cột
`color_token` kèm danh sách giá trị cho phép (cùng quyết định với `question_levels`).

Quyền thao tác: **chỉ ADMIN (A3)** tạo, đổi tên, sắp xếp lại, xoá; A2 (INSTRUCTOR) chỉ chọn khi soạn đề hoặc đổi độ khó theo lô. Không giới hạn
số mức. Hai bất biến: (1) xoá bị **từ chối khi còn bất kỳ `problems.level_id` trỏ tới** (tính cả bài `UNPUBLISHED` và `deleted = true`, vì FK vẫn tồn
tại); API trả số bài đang tham chiếu. (2) **Luôn còn ít nhất một mức**: xoá mức cuối bị từ chối, vì form soạn đề bắt buộc chọn độ khó. Hai lớp bảo vệ
như `topics`: kiểm ở use case (lỗi nghiệp vụ kèm số đếm) và FK không cascade làm lưới an toàn. Mọi thao tác ghi `system_audit_logs` (F1-14).
Độ khó **không mang logic** (không ảnh hưởng điểm, giới hạn, chấm bài). Module khác muốn hiện nhãn độ khó (bảng điều khiển, tiến độ học viên) lấy qua event hoặc
port của `problem-bank`, không join chéo schema `[SoT: Suy luận]`. Cách sinh slug `code` chốt ở DD.

### 1.3. `tags` / `problem_tags` (F2-02 amendment, `DEC-2026-0831-problem-authoring-round2`)

`tags(id, name unique)` — **tự do**, không danh mục cố định, tạo mới khi A2 gõ thẻ chưa tồn tại;
`problem_tags(problem_id FK, tag_id FK)`, unique `(problem_id, tag_id)`. Khác `topics` ở chỗ không có danh mục
do ADMIN quản lý: A2 gõ thêm thẻ mới ngay khi soạn đề, không qua ADMIN — dùng để lọc chi tiết hơn (F2-11).
`tags` không đổi trong đợt 2026-10-01.

### 1.4. `problem_specs` (F2-03, F2-04, F2-09)

Một dòng = một phiên bản đặc tả hiện hành của một bài toán (không phải bảng versioning lịch sử đầy đủ —
lịch sử chỉ cần cho `testcase_set_version`, xem mục 1.6).

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `problem_id` | FK → `problems.id` unique | Một bài toán đúng một đặc tả hiện hành |
| `matching_strategy` | ENUM(`EXACT`,`TRIMMED`,`EPSILON`,`UNORDERED_SET`) | F2-04, `harness` đọc để sinh mã so khớp |
| `epsilon_value` | DECIMAL nullable | Chỉ dùng khi `matching_strategy = EPSILON` |
| `stdin_format_md` | TEXT | Mô tả tự do định dạng đọc `stdin` cho mô hình Standard I/O (F2-03) |
| `stdout_format_md` | TEXT | Mô tả tự do định dạng in `stdout` |
| `spec_version` | INT default 1 | Tăng mỗi lần chữ ký hàm đổi — dùng làm căn cứ phát `ProblemSpecChanged` với số hiệu để `harness` biết có nên vô hiệu cache hay không. **Không phải cùng khái niệm với `testcase_set_version`** (mục 1.6) — một cái theo dõi đặc tả hàm, một cái theo dõi bộ dữ liệu test |
| `updated_at` | TIMESTAMPTZ | |

### 1.5. `function_signatures` (F2-03 — con của `problem_specs`)

**Một dòng mỗi bài** (chủ dự án chọn 2026-10-03, `DEC-2026-1003-single-function-signature`; trước đó ba dòng, mỗi ngôn ngữ một dòng). Chữ ký là chung cho Java, C++, Python vì `harness` tự ánh xạ kiểu sang từng ngôn ngữ (`02-bd/architecture/harness.md` mục 4.2, 4.3); chỉ cách viết tên hàm khác nhau và được suy ra từ tên chung.

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `problem_spec_id` | FK → `problem_specs.id`, unique | Đúng một chữ ký mỗi bài |
| `function_name` | VARCHAR | Tên chung, gõ snake_case hoặc camelCase đều được; tên từng ngôn ngữ do `harness` suy ra (Python snake_case, Java và C++ camelCase) |
| `return_type` | JSONB | Cấu trúc kiểu theo type schema dùng chung với `harness` (xem kiến trúc mục 4 — shared kernel `algoprep-common`); ví dụ `{"kind":"LIST","of":{"kind":"INT"}}` |
| `parameters` | JSONB | Mảng `[{"name":"nums","type":{...}}]`, thứ tự tham số có ý nghĩa. Tên tham số dùng cho mã khung và cho cách hiển thị testcase mẫu (`GetStarterCode`, `ListSampleTestcases`); `harness` gọi hàm theo thứ tự |
| `name_overrides` | JSONB nullable | Ghi đè tên hàm cho một ngôn ngữ, ví dụ `{"java":"solve"}`; khoá là `JAVA`/`CPP`/`PYTHON`, chỉ chứa ngôn ngữ có ghi đè |
| `created_at` / `updated_at` | TIMESTAMPTZ | |

`[SoT: Suy luận]` — quan hệ 1-1 với `problem_specs` cho thấy có thể gộp bốn cột chữ ký vào `problem_specs` thay vì giữ bảng riêng; để DD chốt. Cấu trúc JSONB cụ thể của type schema (primitive, mảng nhiều chiều, nested list, linked list, binary tree) là quyết định chung với `harness` — chốt chi tiết ở `03-dd/logic/harness.md` và `03-dd/api/problem-bank.md` khi cả hai vào DD, đây chỉ là chỗ lưu trữ.

### 1.6. `testcases` (F2-05, F2-06, F2-07)

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `problem_id` | FK → `problems.id` | |
| `visibility` | ENUM(`SAMPLE`,`HIDDEN`) | F2-05/F2-06 |
| `input_storage` | ENUM(`INLINE`,`MINIO`) | Ranh giới kích thước — chi tiết `02-bd/storage/problem-bank.md` |
| `input_inline` | TEXT nullable | Có giá trị khi `input_storage = INLINE` |
| `input_object_key` | VARCHAR nullable | Có giá trị khi `input_storage = MINIO` — xem storage |
| `expected_output_storage` | ENUM(`INLINE`,`MINIO`) | Độc lập với `input_storage` — input nhỏ nhưng output lớn (hoặc ngược lại) vẫn xảy ra |
| `expected_output_inline` | TEXT nullable | |
| `expected_output_object_key` | VARCHAR nullable | |
| `is_worked_example` | BOOLEAN default `false` | Đánh dấu ví dụ mẫu hiển thị kèm giải thích trong đề — phục vụ ngưỡng "≥2 ví dụ mẫu" của checklist xuất bản (`DEC-2026-0831-problem-management-lifecycle-details`); chỉ có ý nghĩa khi `visibility = SAMPLE` |
| `is_ai_generated_draft` | BOOLEAN default `false` | F2-14 — testcase AI sinh, **chưa gộp vào Hidden khi xuất bản** cho tới khi Admin xác nhận |
| `testcase_set_version` | INT | Phiên bản bộ testcase tại thời điểm testcase này đang hiệu lực — xem mục versioning dưới |
| `created_at` / `updated_at` | TIMESTAMPTZ | |

**Versioning (F2-09, chỉ để truy vết — `DEC-2026-0828-remove-rejudge-scope` đã cắt re-judge):**
`problems.current_testcase_set_version` (thêm một cột INT default 1 vào bảng 1.1 — bổ sung ở đây vì gắn
chặt với cơ chế versioning) tăng mỗi khi Admin sửa/thêm/xoá testcase Hidden của một bài **đã xuất bản**.
Sửa testcase không tạo dòng `testcases` mới cho version cũ — bảng `testcases` chỉ giữ **trạng thái hiện
hành**; lịch sử "submission X được chấm ở version nào" nằm ở phía `judge-orchestration`
(`submission.testcase_set_version_at_grading`, ghi lại tại thời điểm chấm qua sự kiện
`TestcaseSetVersionBumped`), **không phải** ở `problem-bank` giữ nhiều bản sao testcase theo version. Điều
này đủ để trả lời "submission cũ chấm theo version nào" (F2-09) mà không cần cơ chế phục dựng lại đúng nội
dung testcase của version cũ (không cần vì không còn re-judge để chạy lại).

`[SoT: Suy luận]` — nếu về sau khôi phục nhu cầu xem lại **nội dung** testcase ở một version cũ (không chỉ
số hiệu), cần thêm bảng lịch sử tách riêng (ví dụ `testcase_history` lưu snapshot mỗi lần bump version).
BD không tạo bảng đó ngay vì hiện tại không có yêu cầu đọc lại nội dung version cũ, tránh phình schema
cho một tính năng chưa được RD xác nhận.

### 1.7. `class_assignments` (F2-12)

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `problem_id` | FK → `problems.id` | |
| `class_id` | tham chiếu `identity.classes.id`, không FK vật lý xuyên schema | |
| `assigned_by` | tham chiếu user id | |
| `assigned_at` | TIMESTAMPTZ | |
| `removed_at` | TIMESTAMPTZ nullable | Gỡ là ẩn mềm (`DEC-2026-0831-class-assignments-round2`) — set cột này, không xoá dòng, không xoá lịch sử nộp bài liên quan |

Unique `(problem_id, class_id)` khi `removed_at IS NULL` (partial unique index) — cho phép gán lại sau khi
đã gỡ.

### 1.8. `bookmarks` (F2-13)

`(user_id — tham chiếu, không FK vật lý, problem_id FK, note TEXT nullable, created_at, updated_at)`,
PK phức hợp `(user_id, problem_id)`. **Không có API nào cho `INSTRUCTOR`/`ADMIN` đọc bảng này của người
khác** — ràng buộc ở tầng ứng dụng/security, không phải chỉ ở giao diện.

### 1.9. Read model tổng hợp từ domain event (kiến trúc mục 3.3)

`problem_stats(problem_id PK, submission_count, accepted_count, ac_rate, updated_at)` — phục vụ F2-11
(gợi ý trạng thái) và ngưỡng AC < 30% của "Bài cần chú ý".

### 1.10. `problem_bank_settings` (F2-14, `DEC-2026-1003-ai-testcase-generation-quota`)

Một dòng duy nhất (`CHECK (id = 1)`), cùng khuôn `judge.cluster_settings`, chỉ ADMIN (A3) sửa qua thẻ "Sinh testcase tự động" của `ADM0301` (Khu vực H). Đặt ở module này, không đặt ở schema `ai`, vì quy tắc "tối đa N lần" và bộ đếm `problems.ai_testcase_generations_used` cùng thuộc F2: kiểm trần và tăng bộ đếm trong một câu lệnh, không gọi chéo module.

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | SMALLINT PK | Luôn `1` |
| `max_ai_generations_per_problem` | INT default `2`, CHECK từ 1 đến 10 | Số lần "Sinh tự động" thành công tối đa mỗi bài. Mặc định 2 do chủ dự án chốt; khoảng 1 đến 10 là suy luận của BD `[SoT: Suy luận]`. Giảm giá trị **không** xoá bộ đếm đã dùng |
| `updated_by` | UUID | Tham chiếu `identity.users.id`, không FK vật lý xuyên schema |
| `updated_at` | TIMESTAMPTZ | |

## 2. Chỉ mục (index) đáng chú ý

- `problems(status, deleted)` — lọc nhanh danh sách `problem_list` (chỉ `PUBLISHED AND NOT deleted`).
- `problems(code)` unique.
- `problems(level_id)` — lọc theo độ khó và đếm số bài tham chiếu khi ADMIN xoá mức (mục 1.2a).
- `problem_topics(topic_id)`, `problem_tags(tag_id)` — lọc theo chủ đề/thẻ (F2-11).
- `testcases(problem_id, visibility)` — tách nhanh Sample/Hidden khi build gói chấm.
- `class_assignments(class_id)` WHERE `removed_at IS NULL` — danh sách bài đang giao cho một lớp.
- `bookmarks(user_id)` — "Bài đã lưu" của một học viên.

## 3. Redis — key pattern

| Key | TTL | Dùng cho |
| :--- | :--- | :--- |
| `problem:list:<filterHash>` | 60 giây (đề xuất, `[SoT: Suy luận]`) | Cache trang danh sách bài toán đã lọc/phân trang — `dependency-map.md` xác nhận có Redis cache cho `problem-bank` nhưng không nêu TTL |
| `problem:spec:<problemId>` | Invalidate chủ động khi `problem_specs`/`function_signatures` đổi | Cache đặc tả hàm để `harness` (qua outbound port) không phải join nhiều bảng mỗi lần sinh mã |

## 4. Migration — thứ tự tạo bảng

`topics` (seed khởi tạo, sau đó ADMIN thêm/đổi tên/sắp xếp/xoá qua API) → `problem_levels` (seed 3 mức `EASY`/`MEDIUM`/`HARD`, cùng cách quản lý; phải có trước `problems` vì `problems.level_id` là FK NOT NULL) → `tags` → `problems` → `problem_topics` → `problem_tags` → `problem_specs` →
`function_signatures` → `testcases` → `class_assignments` → `bookmarks` → `problem_stats` (read model).

## 5. Giá trị mặc định BD chốt (RD để ngỏ, `[SoT: Suy luận]`)

| Tham số | Giá trị | Nguồn RD |
| :--- | :--- | :--- |
| Ngưỡng checklist xuất bản | ≥8 testcase, ≥2 Sample, ≥2 ví dụ mẫu, đáp án mẫu Pass toàn bộ, đã khai đặc tả (F2-03, F2-04; bổ sung 2026-10-03 theo RD `01-rd/req/problem-bank.md:155-160`) | `DEC-2026-0831-problem-management-lifecycle-details` (đã chốt số, không phải BD tự suy luận) |
| Ngưỡng "Bài cần chú ý" | AC < 30%, chưa xuất bản > 7 ngày | như trên |
| `max_submissions_per_hour` mặc định khi A2 không khai | 60 lần/giờ | RD chỉ xác nhận trường tồn tại (`DEC-2026-0831-problem-authoring-round2`), không nêu số — BD đề xuất |
| Cache `problem:list` TTL | 60 giây | RD/dependency-map xác nhận có cache, không nêu TTL |
| Danh mục độ khó (`problem_levels`) | Seed 3 mức `EASY`/`MEDIUM`/`HARD` (Dễ / Trung bình / Khó), sau đó ADMIN quản lý, không cố định | Chủ dự án 2026-10-03 (`DEC-2026-1001-admin-configurable-settings` mục 7); RD chỉ nói "phân loại theo độ khó" (F2-02), tên ba mức seed là suy luận của BD `[SoT: Suy luận]` |
| Số lần sinh testcase AI tối đa mỗi bài (`problem_bank_settings.max_ai_generations_per_problem`) | 2, ADMIN cấu hình | Chủ dự án 2026-10-03 (`DEC-2026-1003-ai-testcase-generation-quota`); RD F2-14 không nêu giới hạn lần |

## 6. Việc còn mở — chuyển sang DD

- Cấu trúc JSONB chi tiết của `return_type`/`parameters` (mục 1.5) — chốt cùng lúc với type schema của
  `harness`.
- Cách sinh slug `code` cho mức độ khó mới (mục 1.2a), cùng câu hỏi với `topics.code` và `question_levels.code`; màu badge không cấu hình (đã chốt), mở lại nếu chủ dự án cần.
- Có cần bảng lịch sử snapshot nội dung testcase theo version hay không (mục 1.6) — hiện chưa tạo.
- Cơ chế chia sẻ enum type schema với `harness` (shared kernel `algoprep-common` hay port) —
  `02-bd/architecture/problem-bank.md` mục 4.
