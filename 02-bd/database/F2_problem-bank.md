# BD — Database module `problem-bank` (F2)

> Schema PostgreSQL: `problem`. Đọc cùng `02-bd/architecture/F2_problem-bank.md` trước — đặc biệt mục 2
> (bảng ánh xạ mã F2-nn) và mục 5 (cờ `function_wrapper_supported`).
>
> Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4): quy ước xoá của file này: thực thể gốc (`problems`, `testcases`, `tags`...) **không bao giờ bị xoá cứng**, bài toán chỉ ẩn mềm bằng `deleted = true` (mục 1.1), nên mọi FK nội schema `problem` là `NO ACTION` (mặc định, không `ON DELETE CASCADE`). Ngoại lệ đã ghi riêng: `topics` và `problem_levels` có xoá cứng nhưng bị chặn khi còn tham chiếu (mục 1.2, 1.2a); `class_assignments` gỡ mềm bằng `removed_at` (mục 1.7). `[SoT: Suy luận]` cho câu tổng quát này, từng điểm đã có nguồn ở mục tương ứng.

## 1. Bảng chính

**Quy ước kiểu cột tham chiếu (bổ sung 2026-10-23, `DEC-2026-1023-db-actor-review-fixes`):** mọi cột trỏ tới một dòng của bảng khác, kể cả cột chéo schema không có khoá ngoại vật lý (`user_id`, `problem_id`, `testcase_id`, `created_by`, `updated_by`, `assigned_by`, `class_id`, `submission_id`...), có kiểu **UUID**, trùng kiểu khoá chính của bảng đích. Ô "Kiểu" bỏ trống hoặc chỉ ghi "tham chiếu" ở các bảng dưới đây đọc là UUID. Cột `NOT NULL` hay `nullable` vẫn theo ô Ghi chú của từng cột.

### 1.1. `problems`
Cốt lõi F2-01, F2-02, F2-15, F2-16.

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `code` | VARCHAR(64) UNIQUE NOT NULL | Mã ngắn hiển thị (ví dụ `TWO-SUM`), dùng cho URL/tra cứu nhanh. **Chốt 2026-10-22 (`DEC-2026-1022-problem-bank-schema-sync` mục 7):** máy chủ sinh slug từ tiêu đề (chữ hoa, số, gạch nối), trùng thì thêm hậu tố `-2`, `-3`; unique tính cả bài `deleted`; bản sao (F2-16) sinh mã mới theo cùng quy tắc |
| `title` | VARCHAR(255) NOT NULL | Sửa 2026-10-22 (đồng bộ DB, phase 1): thêm độ dài 255 theo `SHR0202` BD dòng 356 (ô Tiêu đề, độ dài 255) |
| `statement_md` | TEXT | Nội dung đề bằng Markdown + LaTeX (F2-01). Giữ nguyên; là phần đề hiển thị cho người học |
| `constraints_md` | TEXT NOT NULL default `''` | Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-problem-bank-schema-sync) (PB-03): "Ràng buộc dữ liệu" (ví dụ `1 <= s.length <= 10^5`), văn bản tự do Markdown. Đầu vào bắt buộc của F2-14 (AI sinh input) và của bước kiểm input trong F2-14 (RD `01-rd/req/F2_problem-bank.md:79-80,96-99`). NOT NULL default rỗng thay vì nullable: ô này không bắt buộc khi auto-save bản nháp (BD `SHR0202` dòng 363, cột "Bắt buộc: Không", mặc định rỗng; DTO `constraintsText` dòng 599) nên chuỗi rỗng là trạng thái "chưa khai", tránh hai dạng rỗng (NULL và `''`) `[SoT: Suy luận]`. Nút "Sinh tự động" kiểm chuỗi rỗng ở tầng ứng dụng. Màn người học ghép `statement_md` và `constraints_md` thành một trang đề (`USR0102` Q5 đổi) |
| `level_id` | UUID FK → `problem_levels.id`, NOT NULL | F2-02, độ khó. Từ 2026-10-03 là danh mục **do ADMIN quản lý** (mục 1.2a), thay cho ENUM `EASY`/`MEDIUM`/`HARD` từng nằm ở cột `difficulty` (`DEC-2026-1001-admin-configurable-settings` mục 7). FK **không** `ON DELETE CASCADE`. Độ khó chỉ phân loại, không điều khiển điểm hay giới hạn tài nguyên |
| `status` | ENUM(`UNPUBLISHED`,`PUBLISHED`) | F2-15 — đúng hai trạng thái, không có `HIDDEN` (`DEC-2026-0830-problem-lifecycle-two-states`) |
| `deleted` | BOOLEAN default `false` | Ẩn mềm (`DEC-2026-0831-problem-management-lifecycle-details`) — xoá đặt `status = UNPUBLISHED` **và** bật cờ này (thống nhất 2026-10-08 theo RD và DEC, trước đây ghi "không đổi `status`"); lượt nộp, điểm, bookmark, lịch sử lớp giữ nguyên; bài `deleted=true` không hiện ở `problem_management` lẫn `problem_list` |
| `function_wrapper_supported` | BOOLEAN default `true` | F3-13 — tự tính khi lưu `problem_spec`, xem kiến trúc mục 5 |
| `time_limit_ms` | INT NOT NULL, CHECK > 0 | F2-10, mốc cơ sở trước khi nhân hệ số ngôn ngữ |
| `memory_limit_mb` | INT NOT NULL, CHECK > 0 | F2-10 |
| `max_output_size_kb` | INT NOT NULL, CHECK > 0 | F2-10 amendment (`DEC-2026-0831-problem-authoring-round2`), per-problem |
| `max_submissions_per_hour` | INT nullable | F2-10 amendment, per-problem; NULL = bài không khai riêng, đọc mặc định hệ thống (60 lần/giờ, mục 5) lúc kiểm tra, **không** ghi 60 vào cột. Chốt 2026-10-22 (`DEC-2026-1022-problem-bank-schema-sync` mục 6) |
| `duplicated_from_problem_id` | FK → `problems.id` nullable | F2-16 — vết tích nhân bản, không bắt buộc dùng ở UI |
| `known_optimal_complexity` | VARCHAR nullable | Bổ sung 2026-09-12 theo yêu cầu BD `ai-review` (F5-03 — Solution Review đối chiếu độ phức tạp bài giải với optimum đã biết). Dạng chuỗi ký hiệu Big-O (`O(n)`, `O(n log n)`...). Sửa 2026-10-22 (đồng bộ DB vòng 2, DEC-2026-1022-db-sync-round2), mục (10) của quyết định đó: **trong phạm vi hiện tại không có ô nhập ở màn nào** (kể cả `problem_authoring`, `SHR0202` mục 4.3), cột luôn NULL và được giữ lại để mở rộng sau; NULL = chưa khai báo, F5.1 bỏ qua bước so sánh optimum cho bài đó thay vì suy đoán `[SoT: Suy luận]` |
| `author_id` | UUID NOT NULL, tham chiếu `identity.users.id`, không FK vật lý xuyên schema | Bổ sung 2026-10-01 (`SHR0201` Q1, `DEC-2026-1001-admin-configurable-settings`): người tạo bài, **bất biến** — gán một lần khi tạo, không bao giờ đổi (khác `updated_by` đổi theo lần sửa gần nhất). Nhân bản (F2-16) gán `author_id` = người nhân bản. Dùng cho bộ lọc "Bài của tôi" của A2; ADMIN thấy và sửa mọi bài. Lưu ý: yêu cầu "FK users" của quyết định được thực hiện như tham chiếu id kiểm ở tầng ứng dụng, đúng nguyên tắc không FK xuyên schema bên dưới `[SoT: Suy luận]`. Index `problems(author_id)` |
| `updated_by` | UUID, tham chiếu `identity.users.id`, không FK vật lý xuyên schema | F2-10 amendment Q7(g) — trường phẳng, không phải audit trail đầy đủ. Sửa 2026-10-22 (đồng bộ DB, phase 1): bỏ cách viết tự mâu thuẫn "FK → user ... không FK cứng"; quy ước chung cho `updated_by`, `assigned_by`, `author_id`, `created_by` ở mọi bảng module này: kiểu UUID, kiểm tồn tại ở tầng ứng dụng |
| `current_testcase_set_version` | INT NOT NULL default 1 | Sửa 2026-10-22 (đồng bộ DB, phase 1): thêm thành hàng của bảng (trước chỉ có trong văn xuôi mục 1.6). Số hiệu bộ testcase hiện hành, tăng khi sửa/thêm/xoá testcase Hidden của bài đã xuất bản; cơ chế ở mục 1.6. `SHR0202` BD dòng 385 và 601 đọc cột này |
| `created_at` / `updated_at` | TIMESTAMPTZ | `updated_at` dùng cho ngưỡng "chưa xuất bản quá 7 ngày" |
| `published_at` | TIMESTAMPTZ nullable | Mốc xuất bản lần gần nhất |
| `ai_testcase_generations_used` | INT NOT NULL default 0 | Bổ sung 2026-10-03 (`DEC-2026-1003-ai-testcase-generation-quota`, `SHR0202` Q17 và Q18): số lần "Sinh tự động" (F2-14) đã chạy **thành công** cho bài này. Chỉ lần thêm được ít nhất 1 testcase nháp mới cộng; lần lỗi không cộng, duyệt hoặc loại nháp không hoàn lại. Bộ đếm do máy chủ giữ, client không đếm. Mốc trần là tham số ADMIN toàn hệ thống (`ADM0301` Khu vực H, nơi lưu: `ADM0301` Q12), không lưu ở bảng này |
| `ai_brief` | TEXT nullable | Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4), PB-07: "Chỉ dẫn cho trợ lý AI" của bài (Tab 5 của `SHR0202`, `briefText` của DTO). NULL = chưa khai; ô rỗng gửi lên ghi NULL `[SoT: Suy luận]`. Là dữ liệu, không phải chỉ thị: `ai-review` đọc cột này qua `ProblemContextPort` (port của `problem-bank`) lúc sinh prompt và đặt vào khối nhãn riêng, không ghép vào system instruction (`02-bd/architecture/F5_ai-review.md` mục 3.3, `DEC-2026-0831-ai-instruction-injection-guard`). Prototype: `05-coding/frontend/src/entities/problem/model/draft-types.ts:96` (`aiBrief: string`) |
| `no_full_code` | BOOLEAN NOT NULL default `true` | Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4), PB-07: cờ "Không đưa mã hoàn chỉnh" (`noFullCode`, `draft-types.ts:71,97`). **Chủ dự án chốt 2026-10-22: mặc định `true`** (khớp mock và form khởi tạo). Ghi chú trước đó: prototype chỉ khai kiểu `Record<AiGuardKey, boolean>` (`:97`), không đặt giá trị khởi tạo ở kiểu; mock bật cả hai (`05-coding/frontend/src/entities/problem/api/__mock__/problem-draft-mocks.ts:444`) và form khởi tạo "Bật" (`SHR0202` Sheet 5 Tab 5 NO 3), nên có thể chủ dự án muốn default `true`, xem mục 6 |
| `socratic_only` | BOOLEAN NOT NULL default `true` | Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4), PB-07: cờ "Chỉ hỏi ngược, không giải hộ" (`socraticOnly` ở prototype, `questionOnly` ở DTO `SHR0202`; `draft-types.ts:71,97`). Cùng ghi chú mặc định với `no_full_code`. Đổi ba cột AI này **không** tăng `spec_version` hay `current_testcase_set_version` |

Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4): ba cột giới hạn `time_limit_ms`, `memory_limit_mb`, `max_output_size_kb` đề xuất **NOT NULL, CHECK > 0**: `SHR0202` Sheet 5 khu vực C NO 4-6 ghi "Bắt buộc: Có" và checklist xuất bản cần giới hạn (F2-10). Không có DB default `[SoT: Suy luận]`: giá trị khởi tạo cho bản nháp mới do ứng dụng điền, số cụ thể chưa có nguồn, chốt ở DD.

`[SoT: Suy luận]` — `updated_by`, `author_id` (cùng `assigned_by`, `created_by` ở các bảng khác) tham chiếu `identity.users.id`, kiểu UUID, nhưng **không đặt FOREIGN KEY
vật lý xuyên schema** (đúng nguyên tắc modular monolith — mỗi schema độc lập migration); ràng buộc toàn vẹn
kiểm ở tầng ứng dụng khi ghi.

### 1.1a. Quy tắc "bài đã rút khỏi ngân hàng" (`deleted = true`) ở phía người học (G4, chốt 2026-10-08)

Một bài bị xoá mềm không còn hiện ở `problem_list` và `problem_detail` trả như không tồn tại, nhưng dữ liệu người học
đã sinh ra từ bài đó vẫn còn. Quy tắc dưới đây là **nguồn duy nhất**; các BD màn người học chỉ dẫn về mục này
(`USR0201`, `USR0202`, `USR0301`, `USR0501`), không chép lại.

1. **Lịch sử và kết quả giữ nguyên.** Dòng lịch sử nộp, kết quả từng testcase và báo cáo Solution Review của bài đã
   rút vẫn mở được; chỉ **liên kết sang `problem_detail` bị bỏ** (không có đích để mở), thay bằng nhãn "Đã rút khỏi
   ngân hàng" cạnh tiêu đề. Tiêu đề và mã bài đọc từ bản ghi `problems` còn lại (xoá mềm nên vẫn đọc được).
2. **Mẫu số tiến độ chỉ tính bài còn hiển thị.** Tổng số bài, tỉ lệ "đã giải / tổng số bài" và phân bố theo chủ đề,
   độ khó đếm trên `status = PUBLISHED AND NOT deleted`. Điểm cao nhất của bài đã rút (`user_problem_best_score`)
   **vẫn được lưu** nhưng **không cộng vào "đã giải"**.
3. **Lượt nộp đang dở.** Lượt nộp đã vào hàng đợi trước lúc bài bị xoá **vẫn được chấm bình thường** (máy chủ đọc
   bài và testcase đã khoá phiên bản); lượt nộp **mới** gửi sau khi xoá bị từ chối vì bài không tồn tại
   (`PROBLEM_NOT_FOUND`). `[SoT: Suy luận]` — chưa có DD `judge-orchestration` xác nhận hành vi hàng đợi.
4. **Nút hành động liên quan.** "Làm lại", "Mở bài", "Phân tích bài giải mới" trỏ về bài đã rút bị vô hiệu kèm
   tooltip "Bài này đã rút khỏi ngân hàng"; không có nút khôi phục ở phía người học.

### 1.2. `topics` / `problem_topics` (F2-02)

Danh mục chủ đề bài toán là **dữ liệu do ADMIN quản lý**, cùng cách xử lý với `interview_bank.question_topics`
(đã chốt 2026-10-01, owner uỷ quyền, xem `DEC-2026-1001-admin-configurable-settings`; thay cho câu "seed cố
định theo giáo trình" trước đó). Các chủ đề ban đầu (ví dụ "Mảng", "Cây", "Đồ thị"...) chỉ là dữ liệu seed.

`topics(id UUID PK, code VARCHAR UNIQUE NOT NULL, name VARCHAR NOT NULL, sort_order SMALLINT NOT NULL, created_at TIMESTAMPTZ NOT NULL)`:
`code` là slug ổn định sinh một lần khi tạo, không đổi khi đổi `name`; `name` duy nhất không phân biệt hoa thường
(unique index trên `lower(name)`, không dùng `UNIQUE` thường);
`sort_order` là thứ tự hiện ở bộ lọc. `problem_topics(problem_id FK, topic_id FK)`, many-to-many, **khoá chính phức hợp**
`(problem_id, topic_id)`; FK `topic_id` **không** `ON DELETE CASCADE`.
Sửa 2026-10-22 (đồng bộ DB, phase 1): `sort_order`/`created_at` NOT NULL, kiểu cột nêu rõ, PK phức hợp thay cho "unique".

Quyền thao tác: **chỉ ADMIN (A3)** tạo, đổi tên, sắp xếp lại, xoá chủ đề; A2 (INSTRUCTOR) chỉ chọn từ danh sách
có sẵn khi soạn đề. Không giới hạn số lượng chủ đề. Xoá bị **từ chối khi còn bất kỳ `problem_topics.topic_id` trỏ
tới** (tính cả bài `UNPUBLISHED` và bài `deleted = true`, vì FK vẫn tồn tại): API trả số bài đang tham chiếu để
UI yêu cầu ADMIN chuyển hoặc bỏ gán các bài đó trước. Bảo vệ hai lớp: kiểm ở use case (lỗi nghiệp vụ kèm số
đếm) và FK không cascade làm lưới an toàn. Mọi thao tác ghi `system_audit_logs` (F1-14). Cách sinh slug `code`
chốt ở DD `[SoT: Suy luận]` — quyết định chỉ nói "giống interview topics".

### 1.2a. `problem_levels` (F2-02)

Danh mục độ khó bài toán — **dữ liệu do ADMIN quản lý**, cùng cách xử lý với `topics` (mục 1.2) và với `interview_bank.question_levels`
(`02-bd/database/F6_interview-bank.md` mục 1.1a), nhưng là **bảng riêng, không dùng chung** với `question_levels`: F2 và F6 là hai
bounded context độc lập và ADMIN có thể muốn nhãn khác nhau ở mỗi bên (`DEC-2026-1001-admin-configurable-settings` mục 7, chốt 2026-10-03).
Ba mức cũ `EASY`/`MEDIUM`/`HARD` chỉ còn là dữ liệu seed khởi tạo.

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `code` | VARCHAR UNIQUE NOT NULL | Slug ổn định, sinh một lần khi tạo, không đổi khi đổi tên. **Không phải ENUM.** 3 dòng seed giữ mã `EASY`, `MEDIUM`, `HARD` (Dễ / Trung bình / Khó). Mức tạo mới: slug sinh từ `display_name` theo cùng quy tắc với `topics.code` (cách sinh chốt ở DD) |
| `display_name` | VARCHAR NOT NULL | Nhãn hiển thị tiếng Việt; ADMIN đổi tên được. Duy nhất không phân biệt hoa thường: unique index trên `lower(display_name)` (sửa 2026-10-22, đồng bộ DB phase 1; trước ghi `UNIQUE` thường, không khớp câu "không phân biệt hoa thường") |
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

`tags(id UUID PK, name VARCHAR NOT NULL UNIQUE, created_at TIMESTAMPTZ NOT NULL)` — **tự do**, không danh mục cố định, tạo mới khi A2 gõ thẻ chưa tồn tại;
`problem_tags(problem_id FK, tag_id FK)`, **khoá chính phức hợp** `(problem_id, tag_id)`. Sửa 2026-10-22 (đồng bộ DB, phase 1): thêm kiểu cột, `created_at`, PK phức hợp. Khác `topics` ở chỗ không có danh mục
do ADMIN quản lý: A2 gõ thêm thẻ mới ngay khi soạn đề, không qua ADMIN — dùng để lọc chi tiết hơn (F2-11).
`tags` không đổi trong đợt 2026-10-01.

### 1.4. `problem_specs` (F2-03, F2-04, F2-09)

Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-problem-bank-schema-sync) (PB-10): bảng này **gồm cả chữ ký hàm** (trước nằm ở `function_signatures`, mục 1.5 nay đã gộp vào đây). `problem_id` vẫn unique.

Một dòng = một phiên bản đặc tả hiện hành của một bài toán (không phải bảng versioning lịch sử đầy đủ —
lịch sử chỉ cần cho `testcase_set_version`, xem mục 1.6).

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `problem_id` | FK → `problems.id` unique | Một bài toán đúng một đặc tả hiện hành |
| `matching_strategy` | ENUM(`EXACT`,`TRIMMED`,`EPSILON`,`UNORDERED_SET`) | F2-04, `harness` đọc để sinh mã so khớp |
| `epsilon_value` | NUMERIC(18,12) nullable | Chỉ dùng khi `matching_strategy = EPSILON`. Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4): CHECK `epsilon_value > 0`; bắt buộc khác NULL khi `matching_strategy = EPSILON` (`SHR0202` Sheet 6 dòng kiểm số 7). Độ chính xác `(18,12)` `[SoT: Suy luận]`: nguồn chỉ nói "số thập phân", cần biểu diễn được cỡ `1e-9` |
| `stdin_format_md` | TEXT | Mô tả tự do định dạng đọc `stdin` cho mô hình Standard I/O (F2-03) |
| `stdout_format_md` | TEXT | Mô tả tự do định dạng in `stdout` |
| `function_name` | VARCHAR | Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-problem-bank-schema-sync) (PB-10, gộp từ `function_signatures`; `DEC-2026-1003-single-function-signature` giữ nguyên: một chữ ký chung cho cả bài). Tên chung, gõ snake_case hoặc camelCase đều được; tên từng ngôn ngữ do `harness` suy ra (Python snake_case, Java và C++ camelCase). Chỉ có nghĩa với mô hình function wrapper |
| `return_type` | JSONB | Cấu trúc kiểu theo type schema dùng chung với `harness` (xem kiến trúc mục 4 — shared kernel `algoprep-common`); ví dụ `{"kind":"LIST","of":{"kind":"INT"}}` |
| `parameters` | JSONB | Mảng `[{"name":"nums","type":{...}}]`, thứ tự tham số có ý nghĩa. Tên tham số dùng cho mã khung và cho cách hiển thị testcase mẫu (`GetStarterCode`, `ListSampleTestcases`); `harness` gọi hàm theo thứ tự |
| `name_overrides` | JSONB nullable | Ghi đè tên hàm cho một ngôn ngữ, ví dụ `{"java":"solve"}`; khoá là `JAVA`/`CPP`/`PYTHON`, chỉ chứa ngôn ngữ có ghi đè |
| `spec_version` | INT NOT NULL default 1 | Sửa 2026-10-22 (đồng bộ DB, phase 1; phase 2 cập nhật sau khi gộp): tăng mỗi lần **bất kỳ cột nào của dòng `problem_specs`** đổi (gồm `matching_strategy`, `epsilon_value`, `stdin_format_md`, `stdout_format_md`, `function_name`, `return_type`, `parameters`, `name_overrides`), không chỉ chữ ký hàm, vì `harness` cache cả dòng (mục 3 và `02-bd/database/F3_harness.md` mục 2). Sau gộp chỉ còn một bảng nên không còn cách nói "hai bảng cùng tăng". Dùng làm căn cứ phát `ProblemSpecChanged` với số hiệu để `harness` biết có nên vô hiệu cache hay không; `judge.submissions` ghim theo `spec_version` thay cho `problem_spec_id` (`DEC-2026-1022-problem-bank-schema-sync` mục 5). **Không phải cùng khái niệm với `testcase_set_version`** (mục 1.6) — một cái theo dõi đặc tả hàm, một cái theo dõi bộ dữ liệu test |
| `updated_at` | TIMESTAMPTZ | |

### 1.5. ~~`function_signatures`~~ — đã gộp vào mục 1.4 (`DEC-2026-1022-problem-bank-schema-sync` mục 5)

Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-problem-bank-schema-sync) (PB-10): bảng `function_signatures` **không còn**. Bốn cột `function_name`, `return_type`, `parameters`, `name_overrides` nay là cột của `problem_specs` (mục 1.4). Lý do (tiêu chí ponytail bậc 4): quan hệ 1-1 với `problem_specs` nên một bảng riêng là thừa, thêm một lần join và hai nơi tăng phiên bản. Mục này giữ tiêu đề để các tài liệu khác trích dẫn "1.5" không gãy.

`[SoT: Suy luận]` — cấu trúc JSONB cụ thể của type schema (primitive, mảng nhiều chiều, nested list, linked list, binary tree) là quyết định chung với `harness`, chốt chi tiết ở `03-dd/logic/F3_harness.md` và `03-dd/api/F2_problem-bank.md`; BD chỉ là chỗ lưu trữ.

### 1.6. `testcases` (F2-05, F2-06, F2-07)

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `problem_id` | FK → `problems.id` | |
| `visibility` | ENUM(`SAMPLE`,`HIDDEN`) NOT NULL | F2-05/F2-06. Nháp AI chưa duyệt (`approved = false`) mặc định `HIDDEN`, người soạn đổi khi duyệt (chủ dự án chốt 2026-10-22); nháp chưa duyệt không vào bộ chấm nên không bao giờ hiện cho người học |
| `input_storage` | ENUM(`INLINE`,`MINIO`) | Ranh giới kích thước — chi tiết `02-bd/storage/F2_problem-bank.md` |
| `input_inline` | TEXT nullable | Có giá trị khi `input_storage = INLINE` |
| `input_object_key` | VARCHAR nullable | Có giá trị khi `input_storage = MINIO` — xem storage |
| `expected_output_storage` | ENUM(`INLINE`,`MINIO`) | Độc lập với `input_storage` — input nhỏ nhưng output lớn (hoặc ngược lại) vẫn xảy ra |
| `expected_output_inline` | TEXT nullable | |
| `expected_output_object_key` | VARCHAR nullable | |
| ~~`is_worked_example`~~ | (bỏ) | Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-problem-bank-schema-sync) (PB-08, mục 4 của DEC): **bỏ cột**. Ví dụ mẫu có dữ liệu riêng (input, output, giải thích), không cần là testcase chấm; chuyển sang bảng `worked_examples` (mục 1.12). Ngưỡng "≥2 ví dụ mẫu" đếm dòng `worked_examples`, không còn đếm cờ ở đây |
| ~~`explanation_md`~~ | (bỏ) | Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-problem-bank-schema-sync) (PB-08): **bỏ cột**, chuyển thành `worked_examples.explanation_md` (mục 1.12). Ghi chú lịch sử: cột này được thêm 2026-10-18 theo `DEC-2026-1018-usr-open-questions-phase2-5`; phần đó **bị thay thế** bởi `DEC-2026-1022-problem-bank-schema-sync` |
| `origin` | ENUM(`MANUAL`,`AI`) NOT NULL default `MANUAL` | Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-problem-bank-schema-sync) (PB-05, thay `is_ai_generated_draft`): xuất xứ testcase; `AI` = do F2-14 sinh |
| `approved` | BOOLEAN NOT NULL | Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-problem-bank-schema-sync) (PB-05): người soạn đề đã duyệt chưa. Testcase `AI` sinh ra với `approved = false` và chuyển `true` khi người soạn đề duyệt (RD `01-rd/req/F2_problem-bank.md:111-116`). Testcase `MANUAL` do A2/A3 tự nhập tạo với `approved = true` `[SoT: Suy luận]`: RD chỉ nói rõ nháp AI chưa duyệt, không nói thẳng testcase tay mặc định đã duyệt; khớp prototype (`draft-types.ts:11-15`). **Chỉ testcase `approved = true` được tính vào các mốc checklist xuất bản (≥8 testcase, ≥2 Sample) và được gộp vào bộ Hidden/chạy chấm** (RD `:113-116`); nháp chưa duyệt không bao giờ vào bộ chấm. Duyệt một testcase Hidden của bài đã xuất bản là thêm vào bộ Hidden nên tăng `current_testcase_set_version` |
| `category` | ENUM(`TYPICAL`,`BOUNDARY_LOW`,`BOUNDARY_HIGH`,`DEGENERATE`,`DUPLICATE`,`SORTED`,`EXTREME`) nullable | Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-problem-bank-schema-sync) (PB-05): nhãn phân loại ca để dựng ma trận độ phủ ở bước duyệt (F2-14 bước 3). Bảy giá trị lấy từ union đóng `TestcaseCategory` của prototype (`05-coding/frontend/src/entities/problem/model/draft-types.ts:19-36`, mảng hằng `TESTCASE_CATEGORIES`) `[SoT: Suy luận]`: RD (`01-rd/req/F2_problem-bank.md:86-88`) chỉ liệt kê sáu nhãn bằng lời, `typical` chỉ có ở prototype. NULL = testcase nhập tay chưa gán nhãn. Chỉ để trình bày, không ảnh hưởng chấm |
| `position` | INT NOT NULL | Sửa 2026-10-22 (đồng bộ DB, phase 1): thứ tự trình bày testcase trong bảng, ghi bởi `ReorderTestcases` (`02-bd/screens/shared/SHR0202_problem_authoring.md:643`, kéo-thả `:696`). Chỉ ảnh hưởng hiển thị, không ảnh hưởng chấm (F4 chạy mọi testcase). Dòng mới nhận `max(position)+1` trong cùng `problem_id`. Không đặt unique `(problem_id, position)` để việc đổi chỗ không vướng ràng buộc tạm thời; ứng dụng đánh số lại cả bộ trong một giao dịch |
| `created_at` / `updated_at` | TIMESTAMPTZ | |

**Ràng buộc CHECK lưu trữ (sửa 2026-10-22, đồng bộ DB phase 1):** `input_storage = 'INLINE'` bắt buộc `input_inline IS NOT NULL`; `input_storage = 'MINIO'` bắt buộc `input_object_key IS NOT NULL`. Tương tự `expected_output_storage` với `expected_output_inline` / `expected_output_object_key`. Thêm 2 CHECK ở mức bảng.

Sửa 2026-10-22 (đồng bộ DB, phase 1): **bỏ cột `testcases.testcase_set_version`**. Cột mâu thuẫn với nguyên tắc "bảng chỉ giữ trạng thái hiện hành" (mục versioning dưới) và không có nguồn nào đọc: grep `02-bd`, `01-rd`, `03-dd` chỉ thấy `problems.current_testcase_set_version` (`SHR0202` BD dòng 385, 601), sự kiện `TestcaseSetVersionBumped` (`02-bd/architecture/F2_problem-bank.md:69`) và cột phía `judge-orchestration`.

**Versioning (F2-09, chỉ để truy vết — `DEC-2026-0828-remove-rejudge-scope` đã cắt re-judge):**
`problems.current_testcase_set_version` (đã là một hàng của bảng 1.1; INT NOT NULL default 1) tăng mỗi khi Admin sửa/thêm/xoá testcase Hidden của một bài **đã xuất bản**.
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
| `class_id` | UUID, tham chiếu `identity.classes.id`, không FK vật lý xuyên schema | |
| `assigned_by` | UUID, tham chiếu `identity.users.id`, không FK vật lý xuyên schema | Sửa 2026-10-22 (đồng bộ DB, phase 1): thống nhất kiểu UUID |
| `assigned_at` | TIMESTAMPTZ | |
| `removed_at` | TIMESTAMPTZ nullable | Gỡ là ẩn mềm (`DEC-2026-0831-class-assignments-round2`) — set cột này, không xoá dòng, không xoá lịch sử nộp bài liên quan |

Unique `(problem_id, class_id)` khi `removed_at IS NULL` (partial unique index) — cho phép gán lại sau khi
đã gỡ.

Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4) (ID-13, phía problem-bank): `class_assignments` **tiêu thụ sự kiện `ClassDeleted`** do `identity` phát khi xoá cứng một lớp: mọi dòng có `class_id` = lớp đã xoá và `removed_at IS NULL` được đặt `removed_at = now()` (gỡ mềm như thao tác gỡ thường, không xoá dòng, lịch sử nộp bài giữ nguyên). `class_id` vẫn **không có FK** tới `identity.classes` (không FK vật lý xuyên schema); tham chiếu treo được tránh bằng sự kiện này. Xử lý idempotent: nhận lại sự kiện thì không còn dòng `removed_at IS NULL` để cập nhật. Sự kiện nằm trong danh sách consume ở `02-bd/architecture/F2_problem-bank.md` mục 3.3.

### 1.8. `bookmarks` (F2-13)

`(user_id — tham chiếu, không FK vật lý, problem_id FK, note TEXT nullable, created_at, updated_at)`,
PK phức hợp `(user_id, problem_id)`. **Không có API nào cho `INSTRUCTOR`/`ADMIN` đọc bảng này của người
khác** — ràng buộc ở tầng ứng dụng/security, không phải chỉ ở giao diện.

### 1.9. Read model tổng hợp từ domain event (kiến trúc mục 3.3)

`problem_stats` phục vụ F2-11 (gợi ý trạng thái), cột "AC rate" của danh sách bài và ngưỡng AC < 30% của "Bài cần chú ý". Sửa 2026-10-22 (đồng bộ DB, phase 1): đầy đủ cột như bảng dưới.

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `problem_id` | UUID PK, FK → `problems.id` | Một dòng mỗi bài |
| `submission_count` | INT NOT NULL default 0 | Tổng lượt nộp đã chấm xong, cộng dồn từ `SubmissionGraded` |
| `accepted_count` | INT NOT NULL default 0 | Số lượt có verdict `Accepted`, cộng dồn từ `SubmissionAccepted`; CHECK `accepted_count <= submission_count` |
| `ac_rate` | NUMERIC(5,2) nullable | **Giá trị dẫn xuất** `accepted_count * 100.0 / submission_count`, **được lưu** (không phải generated column) để ORDER BY và lọc "AC < 30%" dùng index; cập nhật trong cùng câu lệnh với hai bộ đếm. NULL khi `submission_count = 0`. Đề xuất của BD, xem câu hỏi mở mục 6 |
| `updated_at` | TIMESTAMPTZ NOT NULL | Lần cập nhật gần nhất từ sự kiện |

**Dòng có thể chưa tồn tại**: dòng được tạo (upsert) khi nhận sự kiện chấm đầu tiên của bài; bài chưa ai nộp không có dòng. Truy vấn phải `LEFT JOIN`; không có dòng hoặc `ac_rate IS NULL` thì API trả `null` và màn hiển thị `-`, không hiển thị `0%` (khớp prototype, `USR0101` BD dòng 388). Không thêm hai chỉ báo phụ từ `SHR0202` Q5 (chưa có quyết định).

### 1.10. `problem_bank_settings` (F2-14, `DEC-2026-1003-ai-testcase-generation-quota`)

Một dòng duy nhất (`CHECK (id = 1)`), cùng khuôn `judge.cluster_settings`, chỉ ADMIN (A3) sửa qua thẻ "Sinh testcase tự động" của `ADM0303` (Khu vực E; trước 2026-10-10 là `ADM0301` Khu vực H). Đặt ở module này, không đặt ở schema `ai`, vì quy tắc "tối đa N lần" và bộ đếm `problems.ai_testcase_generations_used` cùng thuộc F2: kiểm trần và tăng bộ đếm trong một câu lệnh, không gọi chéo module.

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | SMALLINT PK, CHECK (`id = 1`) | Luôn `1` (singleton); sửa 2026-10-22 (đồng bộ DB, phase 1): ghi rõ CHECK ở cột để khớp mô tả đầu mục |
| `max_ai_generations_per_problem` | INT default `2`, CHECK từ 1 đến 10 | Số lần "Sinh tự động" thành công tối đa mỗi bài. Mặc định 2 do chủ dự án chốt; khoảng 1 đến 10 là suy luận của BD `[SoT: Suy luận]`. Giảm giá trị **không** xoá bộ đếm đã dùng |
| `updated_by` | UUID | Tham chiếu `identity.users.id`, không FK vật lý xuyên schema, kiểm ở tầng ứng dụng |
| `updated_at` | TIMESTAMPTZ | |

### 1.11. `problem_reference_solutions` (F2-18, PB-04)

Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-problem-bank-schema-sync) (PB-04, `DEC-2026-1022-problem-bank-schema-sync` mục 2): nơi lưu **Đáp án mẫu** (golden solution) mà F2-18 và F2-14 cần (RD `01-rd/req/F2_problem-bank.md:126-135`); trước đó `SHR0202` BD dòng 280 ghi "không có bảng riêng". **Một dòng mỗi bài**.

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `problem_id` | UUID NOT NULL UNIQUE, FK → `problems.id` | Đúng một đáp án mẫu mỗi bài; không `ON DELETE CASCADE` (bài chỉ ẩn mềm) |
| `language` | ENUM(`JAVA`,`CPP`,`PYTHON`) NOT NULL | Một trong ba ngôn ngữ nộp bài |
| `source_code` | TEXT NOT NULL | Mã nguồn đáp án mẫu. Là dữ liệu cho sandbox, **không bao giờ đưa vào prompt AI** (RD `:83-85,135`) |
| `updated_at` | TIMESTAMPTZ NOT NULL | |

Kết quả "Chạy với đáp án mẫu" (Pass/Fail từng testcase) là **tạm thời, không lưu**; điều kiện xuất bản "đáp án mẫu Pass toàn bộ" kiểm bằng một lần chạy lúc bấm xuất bản, không đọc cờ đã lưu. Nhân bản bài (F2-16) **sao chép** dòng này sang bài mới (RD `:170`). Đổi dòng này không tăng `current_testcase_set_version` và không tăng `spec_version`.

### 1.12. `worked_examples` (PB-08)

Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-problem-bank-schema-sync) (PB-08, `DEC-2026-1022-problem-bank-schema-sync` mục 4): **ví dụ mẫu** hiển thị kèm giải thích ở tab đề bài; tách khỏi `testcases` vì không thuộc bộ chấm. Ngưỡng "≥2 ví dụ mẫu" của checklist xuất bản đếm dòng bảng này.

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | UUID PK | |
| `problem_id` | UUID NOT NULL, FK → `problems.id` | Không `ON DELETE CASCADE` |
| `position` | INT NOT NULL | Thứ tự hiển thị trong đề; không unique, đánh số lại trong một giao dịch như `testcases.position` |
| `input` | TEXT NOT NULL | Input hiển thị; văn bản ngắn, không có nhánh MinIO `[SoT: Suy luận]` |
| `output` | TEXT NOT NULL | Output hiển thị |
| `explanation_md` | TEXT nullable | Lời giải thích, Markdown. Đổi bảng này **không** tăng `current_testcase_set_version` |

Quan hệ với `testcases`: hai tập độc lập, **không có khoá ngoại hay cờ nối**. Ví dụ mẫu **không** dùng để chấm; testcase `SAMPLE` (chấm được, người học thấy) vẫn ở `testcases` với `visibility = SAMPLE`. Checklist xuất bản: "≥2 Sample" đếm `testcases` có `visibility = 'SAMPLE' AND approved = true`; "≥2 ví dụ mẫu" đếm `worked_examples` của bài. Nhân bản (F2-16) sao chép cả các dòng này.

## 2. Chỉ mục (index) đáng chú ý

- `problems(status, deleted)` — lọc nhanh danh sách `problem_list` (chỉ `PUBLISHED AND NOT deleted`).
- `problems(code)` unique.
- `problems(level_id)` — lọc theo độ khó và đếm số bài tham chiếu khi ADMIN xoá mức (mục 1.2a).
- `problem_topics(topic_id)`, `problem_tags(tag_id)` — lọc theo chủ đề/thẻ (F2-11).
- `problems(author_id)` — bộ lọc "Bài của tôi" của A2 (mục 1.1). Bổ sung 2026-10-22 (đồng bộ DB, phase 1).
- `testcases(problem_id, visibility)` — tách nhanh Sample/Hidden khi build gói chấm. Phase 2: truy vấn đếm checklist và build gói chấm thêm điều kiện `approved = true`, không thêm cột vào index (bộ test mỗi bài nhỏ) `[SoT: Suy luận]`.
- `problem_specs(problem_id)` unique (mục 1.4) — sau gộp là khoá tra cứu duy nhất của đặc tả và chữ ký hàm; không còn index `function_signatures(problem_spec_id)`. `problem_reference_solutions(problem_id)` unique (mục 1.11). `worked_examples(problem_id, position)` — đọc ví dụ mẫu theo thứ tự (mục 1.12), không unique. Bổ sung 2026-10-22 (đồng bộ DB, phase 2).
- `testcases(problem_id, position)` — đọc bảng testcase của một bài theo thứ tự trình bày (`ReorderTestcases`). Bổ sung 2026-10-22 (đồng bộ DB, phase 1); không unique.
- `class_assignments(class_id)` WHERE `removed_at IS NULL` — danh sách bài đang giao cho một lớp.
- `class_assignments(problem_id, class_id)` **unique một phần** WHERE `removed_at IS NULL` — cho phép gán lại sau khi gỡ (mục 1.7). Bổ sung 2026-10-22 (đồng bộ DB, phase 1).
- `topics(lower(name))` unique, `problem_levels(lower(display_name))` unique — duy nhất không phân biệt hoa thường (mục 1.2, 1.2a). Bổ sung 2026-10-22 (đồng bộ DB, phase 1).
- Không tạo thêm index `bookmarks(user_id)`: khoá chính phức hợp `(user_id, problem_id)` đã có `user_id` ở đầu, phục vụ "Bài đã lưu" của một học viên. Gỡ 2026-10-22 (đồng bộ DB, phase 1) vì thừa.

## 3. Redis — key pattern

| Key | TTL | Dùng cho |
| :--- | :--- | :--- |
| `problem:list:<filterHash>` | 60 giây (đề xuất, `[SoT: Suy luận]`) | Cache trang danh sách bài toán đã lọc/phân trang — `dependency-map.md` xác nhận có Redis cache cho `problem-bank` nhưng không nêu TTL |
| `problem:spec:<problemId>` | Invalidate chủ động khi dòng `problem_specs` đổi (gồm chữ ký hàm đã gộp, mục 1.4; sửa 2026-10-22 phase 2) | Cache đặc tả hàm để `harness` (qua outbound port) đọc một dòng thay vì join nhiều bảng mỗi lần sinh mã |

## 4. Migration — thứ tự tạo bảng

`topics` (seed khởi tạo, sau đó ADMIN thêm/đổi tên/sắp xếp/xoá qua API) → `problem_levels` (seed 3 mức `EASY`/`MEDIUM`/`HARD`, cùng cách quản lý; phải có trước `problems` vì `problems.level_id` là FK NOT NULL) → `tags` → `problems` → `problem_topics` → `problem_tags` → `problem_specs` (đã gồm chữ ký hàm; bảng `function_signatures` **không được tạo**, phase 2 DEC-2026-1022) →
`testcases` → `problem_reference_solutions` → `worked_examples` → `class_assignments` → `bookmarks` → `problem_stats` (read model) → `problem_bank_settings` (singleton, seed một dòng `id = 1`; bổ sung 2026-10-22, đồng bộ DB phase 1; không phụ thuộc bảng nào nên đặt cuối được, chỉ cần có trước lần "Sinh tự động" đầu tiên).

## 5. Giá trị mặc định BD chốt (RD để ngỏ, `[SoT: Suy luận]`)

**Xác nhận 2026-10-23 (`DEC-2026-1023-db-actor-review-decisions`):** chủ dự án chốt **toàn bộ** giá trị ở bảng dưới theo đề xuất của BD; nhãn `[SoT: Suy luận]` còn lại chỉ ghi nhận rằng RD để ngỏ, không còn là điểm chờ xác nhận. Admin vẫn sửa được các giá trị này trên giao diện nếu cần.

| Tham số | Giá trị | Nguồn RD |
| :--- | :--- | :--- |
| Ngưỡng checklist xuất bản | ≥8 testcase đã duyệt (`testcases.approved`), ≥2 Sample đã duyệt (`testcases` có `visibility = SAMPLE`), ≥2 ví dụ mẫu (đếm dòng `worked_examples`, sửa 2026-10-22 phase 2 DEC-2026-1022-problem-bank-schema-sync), đáp án mẫu Pass toàn bộ, đã khai đặc tả (F2-03, F2-04; bổ sung 2026-10-03 theo RD `01-rd/req/F2_problem-bank.md:155-160`) | `DEC-2026-0831-problem-management-lifecycle-details` (đã chốt số, không phải BD tự suy luận) |
| Ngưỡng "Bài cần chú ý" | AC < 30%, chưa xuất bản > 7 ngày | như trên |
| `max_submissions_per_hour` mặc định khi A2 không khai | 60 lần/giờ, đọc lúc kiểm tra khi cột NULL, không ghi vào cột | RD chỉ xác nhận trường tồn tại (`DEC-2026-0831-problem-authoring-round2`); chủ dự án chốt đọc mặc định hệ thống (`DEC-2026-1022-problem-bank-schema-sync` mục 6) |
| `problems.constraints_md` | `''` (rỗng) | Phase 2, PB-03: ô không bắt buộc khi lưu nháp |
| `testcases.origin` / `approved` | `MANUAL` / testcase tay `true`, nháp AI `false` | Phase 2, PB-05; `approved = true` cho testcase tay là suy luận (mục 1.6) |
| Cache `problem:list` TTL | 60 giây | RD/dependency-map xác nhận có cache, không nêu TTL |
| Danh mục độ khó (`problem_levels`) | Seed 3 mức `EASY`/`MEDIUM`/`HARD` (Dễ / Trung bình / Khó), sau đó ADMIN quản lý, không cố định | Chủ dự án 2026-10-03 (`DEC-2026-1001-admin-configurable-settings` mục 7); RD chỉ nói "phân loại theo độ khó" (F2-02), tên ba mức seed là suy luận của BD `[SoT: Suy luận]` |
| Số lần sinh testcase AI tối đa mỗi bài (`problem_bank_settings.max_ai_generations_per_problem`) | 2, ADMIN cấu hình | Chủ dự án 2026-10-03 (`DEC-2026-1003-ai-testcase-generation-quota`); RD F2-14 không nêu giới hạn lần |

## 6. Việc còn mở — chuyển sang DD

- Cấu trúc JSONB chi tiết của `return_type`/`parameters` (mục 1.4, đã gộp từ 1.5) — chốt cùng lúc với type schema của
  `harness`.
- Cách sinh slug `code` cho mức độ khó mới (mục 1.2a), cùng câu hỏi với `topics.code` và `question_levels.code` (quy tắc sinh slug của `problems.code` đã chốt ở mục 1.1, có thể tái dùng); màu badge không cấu hình (đã chốt), mở lại nếu chủ dự án cần.
- Có cần bảng lịch sử snapshot nội dung testcase theo version hay không (mục 1.6) — hiện chưa tạo.
- ~~`problem_stats.ac_rate` lưu cột hay tính khi đọc (mục 1.9): BD đề xuất lưu, chờ xác nhận.~~ **Đã chốt 2026-10-23** (`DEC-2026-1023-db-actor-review-decisions`): lưu cột `ac_rate`, cập nhật cùng câu lệnh với hai bộ đếm; khoảng 1 đến 10 của hạn mức sinh testcase AI giữ nguyên.

Sửa 2026-10-22 (đồng bộ DB, phase 1): đã xoá mục "cơ chế chia sẻ enum type schema với `harness`" vì đã chốt dùng shared kernel `algoprep-common` (`02-bd/architecture/F2_problem-bank.md:151-153`).

Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-problem-bank-schema-sync): đã đóng các mục mở PB-03 (`constraints_md`), PB-04 (`problem_reference_solutions`), PB-05 (`origin`/`approved`/`category`), PB-08 (`worked_examples`), PB-10 (gộp `function_signatures`) và PB-16 (`problems.code`); ghi chú "có thể gộp, để DD chốt" của mục 1.5 cũ đã được thay bằng quyết định gộp.

- Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4): mặc định DB của `no_full_code` và `socratic_only` (mục 1.1) trước đây là `false` theo suy luận, nay đã chốt `true`; prototype/mock và form khởi tạo bật cả hai. **Đã chốt 2026-10-22: mặc định `true`** (`DEC-2026-1022-db-sync-round4` mục 1, bổ sung).
