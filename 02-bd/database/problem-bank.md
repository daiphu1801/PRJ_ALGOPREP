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
| `difficulty` | ENUM(`EASY`,`MEDIUM`,`HARD`) | F2-02. `[SoT: Suy luận]` — RD không liệt kê tên cụ thể 3 mức, BD chọn theo quy ước phổ biến của thể loại bài toán này |
| `status` | ENUM(`UNPUBLISHED`,`PUBLISHED`) | F2-15 — đúng hai trạng thái, không có `HIDDEN` (`DEC-2026-0830-problem-lifecycle-two-states`) |
| `deleted` | BOOLEAN default `false` | Ẩn mềm (`DEC-2026-0831-problem-management-lifecycle-details`) — xoá không đổi `status`, chỉ bật cờ này; bài `deleted=true` không hiện ở `problem_management` lẫn `problem_list` |
| `function_wrapper_supported` | BOOLEAN default `true` | F3-13 — tự tính khi lưu `problem_spec`, xem kiến trúc mục 5 |
| `time_limit_ms` | INT | F2-10, mốc cơ sở trước khi nhân hệ số ngôn ngữ |
| `memory_limit_mb` | INT | F2-10 |
| `max_output_size_kb` | INT | F2-10 amendment (`DEC-2026-0831-problem-authoring-round2`), per-problem |
| `max_submissions_per_hour` | INT nullable | F2-10 amendment, per-problem; NULL = không giới hạn riêng, dùng mặc định hệ thống |
| `duplicated_from_problem_id` | FK → `problems.id` nullable | F2-16 — vết tích nhân bản, không bắt buộc dùng ở UI |
| `known_optimal_complexity` | VARCHAR nullable | Bổ sung 2026-09-12 theo yêu cầu BD `ai-review` (F5-03 — Solution Review đối chiếu độ phức tạp bài giải với optimum đã biết). Dạng chuỗi ký hiệu Big-O (`O(n)`, `O(n log n)`...), do giảng viên nhập khi soạn đề (`problem_authoring`); NULL = chưa khai báo, F5.1 bỏ qua bước so sánh optimum cho bài đó thay vì suy đoán `[SoT: Suy luận]` |
| `updated_by` | FK → user (tham chiếu id, không FK cứng liên schema) | F2-10 amendment Q7(g) — trường phẳng, không phải audit trail đầy đủ |
| `created_at` / `updated_at` | TIMESTAMPTZ | `updated_at` dùng cho ngưỡng "chưa xuất bản quá 7 ngày" |
| `published_at` | TIMESTAMPTZ nullable | Mốc xuất bản lần gần nhất |

`[SoT: Suy luận]` — `updated_by` tham chiếu `user_id` bên schema `identity` nhưng **không đặt FOREIGN KEY
vật lý xuyên schema** (đúng nguyên tắc modular monolith — mỗi schema độc lập migration); ràng buộc toàn vẹn
kiểm ở tầng ứng dụng khi ghi.

### 1.2. `topics` / `problem_topics` (F2-02)

`topics(id, name, created_at)` — seed cố định theo giáo trình (danh mục chủ đề, ví dụ "Mảng", "Cây",
"Đồ thị"...); `problem_topics(problem_id FK, topic_id FK)`, many-to-many, unique
`(problem_id, topic_id)`.

### 1.3. `tags` / `problem_tags` (F2-02 amendment, `DEC-2026-0831-problem-authoring-round2`)

`tags(id, name unique)` — **tự do**, không danh mục cố định, tạo mới khi A2 gõ thẻ chưa tồn tại;
`problem_tags(problem_id FK, tag_id FK)`, unique `(problem_id, tag_id)`. Khác `topics` ở chỗ không seed
cố định — dùng để lọc chi tiết hơn (F2-11).

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

Một dòng mỗi ngôn ngữ trong ba ngôn ngữ khoá cứng (Java/C++/Python).

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `problem_spec_id` | FK → `problem_specs.id` | |
| `language` | ENUM(`JAVA`,`CPP`,`PYTHON`) | Khớp danh sách ngôn ngữ khoá cứng của `harness` (F3-01→04) |
| `function_name` | VARCHAR | |
| `return_type` | JSONB | Cấu trúc kiểu theo type schema dùng chung với `harness` (xem kiến trúc mục 4 — shared kernel `algoprep-common`); ví dụ `{"kind":"LIST","of":{"kind":"INT"}}` |
| `parameters` | JSONB | Mảng `[{"name":"nums","type":{...}}]`, thứ tự tham số có ý nghĩa |
| `created_at` / `updated_at` | TIMESTAMPTZ | |

Unique `(problem_spec_id, language)` — đúng một chữ ký mỗi ngôn ngữ mỗi bài.

`[SoT: Suy luận]` — cấu trúc JSONB cụ thể của type schema (primitive, mảng nhiều chiều, nested list, linked
list, binary tree) là quyết định chung với `harness` — chốt chi tiết ở `03-dd/logic/harness.md` và
`03-dd/api/problem-bank.md` khi cả hai vào DD, đây chỉ là chỗ lưu trữ.

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

## 2. Chỉ mục (index) đáng chú ý

- `problems(status, deleted)` — lọc nhanh danh sách `problem_list` (chỉ `PUBLISHED AND NOT deleted`).
- `problems(code)` unique.
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

`topics` (seed) → `tags` → `problems` → `problem_topics` → `problem_tags` → `problem_specs` →
`function_signatures` → `testcases` → `class_assignments` → `bookmarks` → `problem_stats` (read model).

## 5. Giá trị mặc định BD chốt (RD để ngỏ, `[SoT: Suy luận]`)

| Tham số | Giá trị | Nguồn RD |
| :--- | :--- | :--- |
| Ngưỡng checklist xuất bản | ≥8 testcase, ≥2 Sample, ≥2 ví dụ mẫu, đáp án mẫu Pass toàn bộ | `DEC-2026-0831-problem-management-lifecycle-details` (đã chốt số, không phải BD tự suy luận) |
| Ngưỡng "Bài cần chú ý" | AC < 30%, chưa xuất bản > 7 ngày | như trên |
| `max_submissions_per_hour` mặc định khi A2 không khai | 60 lần/giờ | RD chỉ xác nhận trường tồn tại (`DEC-2026-0831-problem-authoring-round2`), không nêu số — BD đề xuất |
| Cache `problem:list` TTL | 60 giây | RD/dependency-map xác nhận có cache, không nêu TTL |
| Danh mục `difficulty` | `EASY`/`MEDIUM`/`HARD` | RD chỉ nói "phân loại theo độ khó" (F2-02), không liệt kê tên |

## 6. Việc còn mở — chuyển sang DD

- Cấu trúc JSONB chi tiết của `return_type`/`parameters` (mục 1.5) — chốt cùng lúc với type schema của
  `harness`.
- Có cần bảng lịch sử snapshot nội dung testcase theo version hay không (mục 1.6) — hiện chưa tạo.
- Cơ chế chia sẻ enum type schema với `harness` (shared kernel `algoprep-common` hay port) —
  `02-bd/architecture/problem-bank.md` mục 4.
