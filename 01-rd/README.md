# 01-rd — Tài Liệu Định Nghĩa Yêu Cầu (Requirements Document)

Chỉ mục của tầng RD. Đây là tầng đầu của quy trình **RD → BD → Prototype → DD → CODE → TEST**
(`CLAUDE.md` mục Process).

---

## 1. Bản đồ file

### `overview/` — nền tảng và từ vựng

| File | Nội dung | Trạng thái |
| :--- | :--- | :--- |
| `overview.md` | Nền tảng lý thuyết cốt lõi: DDD, Clean Architecture, command/query, Java 21 + Spring Boot 4 + Virtual Threads, bất đồng bộ và realtime, sandbox, sinh mã bọc hàm, FSD, OWASP, tích hợp LLM. Kèm chính sách AI và mục "điều KHÔNG chốt" | Viết cho AlgoPrep 2026-08-20 |
| `glossary.md` | Ngôn ngữ thống nhất: tiếng Việt trong tài liệu, tiếng Anh trong mã, theo từng Bounded Context | Viết cho AlgoPrep 2026-08-20 |
| `system_survey.md` | Khảo sát **chi tiết**: hệ thống tham khảo, khoảng trống và định vị, bốn actor kèm ma trận quyền, **bảng chức năng có mã `Fx-nn`**, bốn quy trình nghiệp vụ chính, danh sách màn dự kiến (**nguồn thật là `01-rd/screens/` — 32 file, 31 màn sống**; bảng ở mục 7 của survey vẫn được cập nhật theo và tổng chốt ở cuối mục 7 là đúng, xem lưu ý ở mục 1 dưới đây), phạm vi, rủi ro, tiêu chí thành công | Viết cho AlgoPrep 2026-08-20 |

### `system/` — kiến trúc và môi trường

| File | Nội dung | Trạng thái |
| :--- | :--- | :--- |
| `backend_architecture.md` | Modular Monolith, tám module Maven, bốn tầng Clean Architecture, cổng vào và cổng ra, hai luồng dữ liệu điển hình, cách các module nói chuyện, cách ly judge engine (go-judge mặc định) và LLM | Viết cho AlgoPrep 2026-08-20 (trước đó là file rỗng); cập nhật engine 2026-08-23 |
| `frontend_architecture.md` | Next.js 16 App Router, sáu tầng FSD, tiêu chí đặt tầng, hai kênh realtime, bảo mật client | Viết cho AlgoPrep 2026-08-20 |
| `codebase_structure.md` | Cây thư mục monorepo, bên trong `05-coding/`, quy ước đặt tên xuyên tài liệu và mã | Viết cho AlgoPrep 2026-08-20; cập nhật engine 2026-08-23 |
| `environment.md` | Phiên bản đã chốt, hạ tầng Docker, công cụ QA, cổng chất lượng G-CHECK, chiến lược kiểm thử | Viết cho AlgoPrep 2026-08-20; cập nhật engine 2026-08-23 |
| `judge_engine.md` | Judge engine (go-judge mặc định): giới thiệu, vai trò, cổng trung lập để đổi engine, ràng buộc cgroup, cài đặt/chạy trên Windows, sự cố thường gặp | Viết cho AlgoPrep 2026-08-23 (trước đó là `judge0.md`, đổi tên theo `DEC-2026-0823-go-judge-default-engine`) |

### `req/` — yêu cầu

**Tách theo Bounded Context và theo actor từ 2026-08-31** (`DEC-2026-0831-rd-req-split-by-module`).
Hai file gốc `req.md` và `user_stories.md` giờ là **chỉ mục mỏng**, không chứa nội dung — đừng trích dẫn
`Fx-nn` từ chúng nữa.

Quan trọng khi trích dẫn: dùng **neo mã** (`F4-13`, `US-A1-03`) chứ không dùng số dòng. Mã là định danh
vĩnh viễn của yêu cầu; số dòng chết theo mỗi lần sửa file, và đó là lý do đợt tách phải viết lại 345
citation.

**Mở rộng 2026-09-09 — luật này áp cho cả `overview/system_survey.md`:** cấm neo theo số dòng vào file đó,
dùng `mục <số> dòng <slug|mã>`. Lý do: nó là file bị sửa nhiều nhất của tầng RD, và 111 citation neo theo
dòng từ `screens/` đã trôi hết (xem mục 7.1). Neo theo dòng chỉ còn dùng cho file **đã đóng băng** —
`09-layoutBase/*.dc.html`.

| File | Nội dung | Trạng thái |
| :--- | :--- | :--- |
| `req.md` | **Chỉ mục** — bảng trỏ tới 7 file dưới đây | Chỉ mục, không chứa nội dung |
| `identity.md` | F1 — danh tính, phân quyền, tiến độ cá nhân | Xong — `F1-01` tới `F1-30` |
| `problem-bank.md` | F2 — đề bài, đặc tả hàm, testcase, phiên bản bộ testcase | Xong — `F2-01` tới `F2-17` |
| `harness.md` | F3 — lược đồ kiểu, sinh mã ba ngôn ngữ, so khớp, ánh xạ lỗi biên dịch | Xong — `F3-01` tới `F3-13`; chi tiết codegen thuộc DD |
| `judge-orchestration.md` | F4 — hàng đợi, gọi engine từng testcase, realtime, timeout sweep | Xong — 11 mã sống; `F4-04` và `F4-09a`-`e` hết hiệu lực |
| `ai-review.md` | F5 — Solution Review một lượt, Mock Interview nhiều lượt, rubric | Xong — `F5-01` tới `F5-28` |
| `interview-bank.md` | F6 — ngân hàng câu hỏi lý thuyết, chế độ học, chế độ luyện | Xong — 12 mã sống; `F6-11` hết hiệu lực |
| `nfr.md` | Yêu cầu **phi chức năng**: hiệu năng, mở rộng, tin cậy, bảo mật, bảo trì, 8 tiêu chí thành công S1-S8 | Xong — `S1` tới `S8` |
| `user_stories.md` | **Chỉ mục** — bảng trỏ tới 5 file dưới đây | Chỉ mục, không chứa nội dung |
| `user_stories/a1_student.md` | A1 học viên | Xong — `US-A1-01` tới `US-A1-09` |
| `user_stories/a2_instructor.md` | A2 giảng viên | Xong — `US-A2-01` tới `US-A2-11`, gồm `US-A2-01b`; `US-A2-04` đã loại khỏi phạm vi |
| `user_stories/a3_admin.md` | A3 quản trị | Xong — `US-A3-01` tới `US-A3-04`; `US-A3-05` (chấm lại) đã loại khỏi phạm vi |
| `user_stories/a4_system.md` | A4 hệ thống tự động | Xong — `US-A4-01` tới `US-A4-05` |
| `user_stories/open_questions.md` | câu hỏi mở của tầng user story | `Q1` tới `Q4`, toàn bộ đã đóng |

### `screens/` — trục màn hình

**32 file, đã xong.** Mỗi màn một file `<slug>.md`, slug viết `snake_case`; slice frontend tương ứng viết
`kebab-case` (`codebase_structure.md` mục 3). Khu vực đặt theo **actor chính** của màn.

| Khu vực | Actor | Số màn | Slug |
| :--- | :--- | :-: | :--- |
| `shared/` | dùng chung nhiều actor | 5 | `auth`, `interview_question_authoring`, `interview_question_management`, `problem_authoring`, `problem_management` |
| `users/` | A1 học viên | 12 | `interview_bank_list`, `interview_question_detail`, `mock_interview`, `my_progress`, `my_submissions`, `problem_detail`, `problem_list`, `profile`, `saved_problems`, `settings`, `solution_review`, `submission_result` |
| `teacher/` | A2 giảng viên | 6 | `class_assignments`, `class_management`, `class_progress`, `class_student_detail`, `instructor_grading`, `instructor_overview` |
| `admin/` | A3 quản trị | 9 | `admin_ai_config`, `admin_ai_usage`, `admin_language_config`, `admin_overview`, `admin_permission_matrix`, `admin_queue_monitor`, `admin_rejudge`, `admin_system_log`, `admin_user_management` |

Hai lưu ý:

* `admin/admin_rejudge.md` **không phải màn sống** — rejudge đã bị loại khỏi phạm vi
  (`DEC-2026-0828-remove-rejudge-scope`), file giữ lại chỉ để lưu vết và **không dùng làm căn cứ viết
  BD/DD**. Route `/admin/rejudge` mà khung base FE từng dựng đã bị xoá ngày 2026-09-01.
* Frontend đã **soi 1-1 với trục màn**: 32 file RD, **31 màn sống, 31 slice** trong
  `05-coding/frontend/src/views/`. Màn duy nhất không có slice là `admin_rejudge` — đúng, vì nó ngoài
  phạm vi. Ba slice `class_assignments`, `class_student_detail`, `interview_question_authoring` được bổ
  sung ngày 2026-09-01 để khép khoảng lệch trước đó. Bảng ánh xạ slug sang route ở
  `05-coding/frontend/README.md`; bảng trạng thái do bản ghi trạng thái của repo giữ.
  **Cập nhật 2026-09-03:** dòng này trước ghi "28 slice, còn lệch 3 màn" — số đó đã lỗi thời.

---

## 2. Đọc theo thứ tự nào

| Bạn là | Đọc theo thứ tự |
| :--- | :--- |
| Người mới vào dự án | `README.md` (gốc repo) → `overview/overview.md` → `overview/glossary.md` |
| Người sắp viết mã backend | `overview/overview.md` mục 1.A đến 1.E → `system/backend_architecture.md` → `system/environment.md` |
| Người sắp viết mã frontend | `overview/overview.md` mục 1.H → `system/frontend_architecture.md` → `system/environment.md` mục 3.B và mục 4 |
| Người viết BD theo module | `overview/glossary.md` → **`req/<context>.md` của đúng module đó** → `req/nfr.md` → `system/backend_architecture.md` mục 8 (danh sách việc BD phải chốt). `system_survey.md` mục 5 vẫn là bảng `Fx-nn` tổng hợp, dùng để tra chéo |
| Người viết BD theo màn | **`screens/<khu vực>/<slug>.md`** → `req/<context>.md` của các context màn đó chạm → `system/frontend_architecture.md`. Một màn thường chạm nhiều context (`CLAUDE.md` mục Process) |

---

## 3. Nợ tài liệu — đã xử lý xong 2026-08-23

Toàn bộ cấu hình agent của repo này được chuyển từ dự án NestGame v2
(`DEC-2026-0819-agentconfig-retarget-algoprep`), và tài liệu `01-rd/` đi theo. Đợt 2026-08-20 viết lại phần
**nền tảng lý thuyết, kiến trúc và khảo sát hệ thống**; hai file còn lại (`req/req.md`, `req/user_stories.md`)
vẫn nói về nền tảng game retro (module IAM/Catalog/SaveState, OAuth Google, chơi game) cho tới khi được viết
lại ở đợt **2026-08-23**: `req.md` theo sáu phân hệ F1-F6 của `README.md` mục 4 (dùng mã `Fx-nn` ở
`overview/system_survey.md` mục 5 làm khoá truy vết), `user_stories.md` theo bốn actor A1-A4 của
`README.md` mục 3. Không còn file nào trong `01-rd/` mang nội dung của dự án cũ — đã rà toàn bộ chín file
còn lại trong đợt này, xem mục 4.

Mọi tài liệu từ `02-bd/` trở đi giờ có thể dẫn nguồn từ cả sáu file `overview/` `system/` lẫn hai file
`req/`.

## 4. Rà soát 2026-08-23 — không còn nội dung dự án cũ nào khác

Kiểm tra toàn bộ chín file còn lại của `01-rd/` (ngoài hai file vừa viết lại ở mục 3): `overview.md`,
`glossary.md`, `system_survey.md`, `backend_architecture.md`, `frontend_architecture.md`,
`codebase_structure.md`, `environment.md`, `judge_engine.md` (đổi tên từ `judge0.md` cùng ngày), và chính
file `README.md` này. Cả chín đều đã được viết cho AlgoPrep từ đợt 2026-08-20 (riêng `judge_engine.md` từ
2026-08-23), không còn thuật ngữ, module hay ví dụ nào của NestGame v2 (game, ROM, hệ máy, save state,
IAM/Catalog...). `01-rd/screens/` vẫn chưa tồn tại — đây là trạng thái greenfield đúng như
`01-rd/README.md` mục 1 ghi nhận, không phải nợ tài liệu.

**Đổi engine mặc định sang go-judge — hoàn tất 2026-08-23.** `DEC-2026-0823-go-judge-default-engine`: engine
chấm bài mặc định đổi từ Judge0 sang **go-judge**, qua một cổng ra trung lập theo engine
(`JudgeExecutionPort`) để giữ khả năng đổi lại Judge0 sau này. Đã rà và cập nhật đồng bộ: `README.md` (gốc),
`CLAUDE.md`, cùng ba bản ghi của agent config (domain registry, bản ghi trạng thái, bản đồ phụ thuộc),
và chín file `01-rd/` liệt kê ở trên. Mọi chỗ còn nhắc "Judge0" trong các
file này đều có chủ đích — mô tả nó như một adapter thay thế hợp lệ (`Judge0Adapter`), không phải sót lại từ
engine cũ.

**Sự cố với `req/req.md` và `req/user_stories.md` — sửa xong 2026-08-24.** Một lượt thực thi agent đã viết
lại nhầm gần như toàn bộ hai file này (531/542 dòng) trong khi chỉ cần sửa vài dòng nhắc Judge0. Xử lý bằng
`git checkout` để hoàn tác — nhưng bản 2026-08-23 của hai file **chưa từng được commit**, nên `git checkout`
đưa cả hai về đúng bản NestGame v2 cũ trong `HEAD`, không phải về bản AlgoPrep đã viết lại. Lúc đó grep tìm
"Judge0" ra kết quả rỗng và bị hiểu nhầm là "file đã sạch, không cần sửa" — thực ra rỗng vì đã là nội dung
sai chủ đề hoàn toàn, không phải vì file đã đúng. `req.md` được người dùng tự khôi phục lại thủ công;
`user_stories.md` không khôi phục được (không có bản backup) nên được viết lại từ đầu ngày 2026-08-24, cùng
tinh thần với `req.md` — kèm sửa luôn bảng tiêu chí S1-S8 trong `req.md` vì nó là bản chép nguyên từ
`system_survey.md` mục 10 **trước** khi mục đó được sửa (đoạn callback/job đối soát), nên bị lệch theo.

**Bài học ghi lại:** trước khi chạy `git checkout` trên một file, phải xác nhận nội dung committed
(`HEAD`) thực sự là bản muốn giữ, không suy đoán từ việc "diff lớn = chắc sai" — một diff lớn có thể là
việc thật (đổi hẳn chủ đề file, ví dụ viết lại từ NestGame sang AlgoPrep) chứ không chỉ là lỗi cần hoàn tác.

**Cập nhật 2026-09-01 — hai khẳng định ở mục 3 và mục 4 đã lỗi thời, giữ nguyên văn để lưu vết.** Mục 4
viết "`01-rd/screens/` vẫn chưa tồn tại — đây là trạng thái greenfield đúng"; điều đó đúng vào 2026-08-23 và
sai từ 2026-08-25, khi trục màn hình được viết (32 file, xem mục 1). Mục 3 viết "hai file `req/`"; từ
2026-08-31 tầng `req/` có 8 file cộng thư mục `user_stories/` (`DEC-2026-0831-rd-req-split-by-module`).
Không sửa nguyên văn hai mục đó vì chúng là **bản ghi lịch sử có ngày**, và mục 4 tự đặt luật "đừng âm thầm
sửa mà không để lại dấu vết".

Rà soát 2026-09-01 bằng máy, kết quả: **0 câu hỏi mở còn treo** trong 55 file; **157/157 citation
`file:line` trỏ đúng file và trong phạm vi số dòng**; **0 citation mã `Fx-nn`/`US-Axx-nn` treo**; **0 màn có
file RD mà không được nhắc trong `system_survey.md`**. Bảy mã không màn nào trích (`F1-04`, `F3-03`, `F3-05`,
`F3-08`, `F3-09`, `F4-05`, `F4-06`) đều thuần backend, không có bề mặt giao diện — đúng, không phải sót.

**Một vấn đề tầng, đã xử lý cùng ngày.** `req/harness.md` và `req/user_stories.md` từng trích tới hai file
trong `.claude/` (một skill reference và một rule). Hai file đó tồn tại thật nên citation không hỏng, nhưng
chúng là **agent config, không phải tài liệu bàn giao** — người đọc báo cáo đồ án không mở được. Đã bỏ cả
hai: `req/harness.md` giờ trỏ vào `03-dd/logic/harness.md` (đúng tầng sẽ giữ chi tiết codegen), và
`req/user_stories.md` phát biểu quy trình trực tiếp thay vì trích. Kiến thức không mất: skill
`dd-generation` đã tự chỉ thị đọc reference của nó khi trục là `harness`
(`.claude/skills/dd-generation/SKILL.md:126`), nên chỉ thị đó thuộc về agent config và ở lại đó.

**Luật rút ra, bản sửa 2026-09-03 (`DEC-2026-0903-sot-citation-scope`):** tài liệu trong `01-rd/` tới
`04-tdd/` trích các tài liệu cùng tầng bàn giao (`README.md` gốc, `01-rd/` tới `07-review/`,
`09-layoutBase/`) và mã nguồn trong `05-coding/`. Với agent config thì chia hai mức:

| Được | Không được |
| :--- | :--- |
| **Gọi tên một quyết định theo mã**, ví dụ "chốt theo `DEC-2026-0828-remove-rejudge-scope`" — không kèm đường dẫn, không kèm số dòng | Trích `.claude/**` hoặc `.nexa/**` **dưới dạng đường dẫn**, nhất là kèm số dòng như `[SoT: .nexa/control/decision-registry.md:281-285]` |

Ngoại lệ giữ nguyên: được hiện các đường dẫn đó khi đang **mô tả cây thư mục** của repo, như
`system/codebase_structure.md` mục 1.

Lý do tách hai mức thay vì cấm hẳn: decision registry là tầng **P1** của thang SoT, nằm ngay trên chính
`01-rd`, nên cấm RD nhắc tới nó là cắt tài liệu khỏi tầng có quyền ghi đè nó. Cái thực sự làm khó người đọc
báo cáo là **một đường dẫn họ không mở được cộng một số dòng đã trôi** — còn một mã `DEC-` thì đọc là tham
chiếu quyết định và không chết khi registry được sắp lại. Đây đúng là lý do đợt
`DEC-2026-0831-rd-req-split-by-module` đã chuyển citation từ số dòng sang neo mã `Fx-nn`.

Bản luật đầu tiên (2026-09-01) cấm hẳn, và **chính `01-rd/` vi phạm nó ngay hôm sau** ở khoảng 30 file với
hơn 50 citation `.nexa/` — vì đợt đó chỉ gỡ hai citation `.claude/` rồi phát biểu luật rộng hơn phạm vi đã
làm. Một luật bị vi phạm toàn bộ còn tệ hơn không có luật, nên nó được thu về đúng phần thực thi được.

## 5. Rà soát 2026-09-03 — đợt vá vênh (RD drift repair)

Đợt 2026-09-01 kiểm citation `file:line` và câu hỏi mở. Đợt này kiểm thêm bốn trục mà đợt trước không
kiểm, mỗi trục một script chạy trên toàn `01-rd/`. Kế hoạch:
`06-plan/nexa-plan/260903-2031-rd-drift-repair.md`.

| Trục kiểm | Trước đợt | Sau đợt |
| :--- | :--- | :--- |
| Mã `Fx-nn` dùng vs định nghĩa ở `system_survey.md` mục 5 | **20 mã sống treo** — chỉ có trong `req/*.md`, không có trong bảng tổng hợp | 0 treo; bảng có **111 mã sống**, 7 mã hết hiệu lực giữ lại lưu vết |
| Citation `file:line` | 0 lỗi | 0 lỗi (giữ nguyên) |
| Cột bảng "Câu hỏi mở" | 8 hàng lệch cột ở `admin_overview.md` | 0 hàng lệch |
| Pictograph trong tài liệu bàn giao | 5 ở `07-review/rd_review_closure_260831.md` (+4 ở bản `.html`) | 0 |

Bảy việc đã sửa, ngoài bốn trục trên:

1. **`system_survey.md` mục 5** — bổ sung `F1-18` tới `F1-30`, `F2-15` tới `F2-17`, `F5-28`, `F6-13`; tính
   lại mục 5.7. Đây là điểm nặng nhất: `01-rd/README.md` mục 2 chỉ người viết BD dùng bảng đó để tra chéo,
   nên thiếu 20 mã là thiếu 20 yêu cầu.
2. **Gỡ "chấm lại" khỏi từ vựng sống** — `glossary.md` (thuật ngữ `rejudge`, mô tả vai trò `ADMIN`, lý do
   tồn tại của `testcase_set_version`), `req/problem-bank.md` (mục đích `F2-09`),
   `screens/admin/admin_system_log.md` mục 1. Toàn bộ do `DEC-2026-0828-remove-rejudge-scope`; bản trong
   `user_stories/a2_instructor.md` đã sửa từ 2026-08-28, mấy bản kia sót lại.
3. **`glossary.md`** — mô tả vai trò `INSTRUCTOR` ghi "tạo bộ câu hỏi", dễ đọc thành bộ câu hỏi riêng theo
   lớp (`F6-11`, đã loại khỏi phạm vi); sửa thành quản trị kho câu hỏi **dùng chung** (`F6-13`).
4. **`admin_queue_monitor.md`** — citation còn placeholder `260825-XXXX-...`, file thật là
   `260825-1700-report-ai1-phase6-conflicts.md`.
5. **Chỗ xếp `auth`** — `screens/` xếp khu dùng chung, `system_survey.md` mục 7.1 lại đếm vào khu người
   học, nên số theo khu lệch (13/4 so với 12/5) dù tổng đúng. Chốt theo cây `screens/` và chuyển dòng
   `auth` sang mục 7.0.
6. **Số slice frontend ở mục 1** — ghi "28 slice, còn lệch 3 màn"; thực tế 31 slice, hết lệch từ
   2026-09-01.
7. **Bảng `req/` ở mục 1** — cột trạng thái ghi số dòng mỗi file, tự mâu thuẫn với lời khuyên "đừng neo
   theo số dòng" ở ngay trên nó, và lệch ngay khi file được sửa. Thay bằng **khoảng mã** mỗi file phủ.

**Câu hỏi mở:** vẫn **0 câu treo**. 61 marker `[Đợi nextjs]` trong 18 file là nợ prototype có chủ đích,
không phải câu hỏi mở — ai dựng màn nào phải grep marker đó trong file RD của màn trước.

**Phê duyệt 2026-09-05 — hết hạng mục treo.** `F1-30` (`instructor_overview`) và phần bổ sung của `F5-27`
(`instructor_grading`) chốt nội dung từ 2026-08-31 theo phương án kỹ thuật khuyến nghị, nhưng vòng xác nhận
bị gián đoạn giữa chừng nên mang nhãn "chờ phê duyệt". Chủ dự án đã rà soát và **duyệt cả hai ngày
2026-09-05, giữ nguyên nội dung** — xem `07-review/rd_review_closure_260831.md` mục "Hai hạng mục chờ phê
duyệt — đã duyệt 2026-09-05", và mục `approval:` của hai quyết định
`DEC-2026-0831-instructor-overview-dashboard` / `DEC-2026-0831-instructor-grading-round2`.

Sau lượt này, **tầng RD không còn hạng mục nào chờ chủ dự án**: 49/49 câu hỏi mở đã đóng và đã được phê
duyệt, 111/111 mã chức năng đã duyệt. Các ngưỡng số trong hai hạng mục trên (điểm AI dưới 6/10 lọc hàng đợi
chấm tay, cửa sổ thống kê 7 ngày) giữ nhãn `[SoT: Suy luận]` — BD/DD chỉnh được mà không phải mở lại quyết
định.

**Sửa thêm cùng ngày:** ba mục "Ngoài phạm vi file này" còn ghi "chờ chủ dự án" cho những câu hỏi thực ra
đã đóng từ 2026-08-31 — `screens/admin/admin_overview.md` (Q2, Q5), `screens/admin/admin_queue_monitor.md`
(Q2), `screens/admin/admin_user_management.md` (Q1). Cùng loại vênh với đợt 2026-09-03: quyết định đã chốt
nhưng một chỗ trong tài liệu chưa theo kịp.

## 6. Rà soát 2026-09-05 — mở rộng ra ngoài `01-rd/`

Đợt 2026-09-03 chỉ quét trong `01-rd/`. Đợt này quét thêm **`README.md` gốc, `09-layoutBase/github.md`,
`02-bd/`, `05-coding/*/README.md`, `06-plan/PROTOTYPE_DEBT.md`** với cùng bộ script, cộng một trục mới:
**mã `Fx-nn` đã hết hiệu lực nhưng vẫn được mô tả như đang sống**.

### 6.1. `README.md` gốc chưa từng đồng bộ với quyết định sau 2026-08-28

Nặng nhất của đợt, vì `README.md` là **tầng P2 của thang SoT** — trên cả BD/DD — và là nguồn của chương 1
báo cáo. Sáu phát biểu bị quyết định sau đó đảo ngược mà chưa ai sửa:

| Chỗ | Phát biểu cũ | Thực tế |
| :--- | :--- | :--- |
| F2 — phiên bản bộ testcase | "hỗ trợ cơ chế Re-judge biết chấm lại theo phiên bản nào" | Chấm lại ngoài phạm vi; phiên bản chỉ để truy vết |
| F4 | "Gửi từng testcase, **fail-fast** — bỏ các testcase còn lại" | Chạy hết N testcase; `F4-04` hết hiệu lực |
| F5 mở đầu | "Kích hoạt sau khi bài nộp đạt `Accepted`" cho cả phân hệ | Chỉ đúng với F5.1; F5.2 có ba lối vào |
| F6 — danh mục chủ đề | 4 chủ đề | 5 chủ đề, lấy theo prototype |
| F6 — "Mở rộng" | "Giảng viên tạo bộ câu hỏi riêng và gán cho lớp" | `F6-11` ngoài phạm vi; kho dùng chung |
| Mục 6 — phạm vi | "_Lưu ý: cơ chế Re-judge **vẫn nằm trong phạm vi**_" | Ngoài phạm vi từ 2026-08-28 |

### 6.2. `F4-04` hết hiệu lực nhưng chưa lan tới nơi trích nó

`DEC-2026-0831-partial-score-testcase-ratio` khai tử fail-fast, nhưng chỉ `req/judge-orchestration.md`,
`system_survey.md` mục 5.4 và `user_stories/a4_system.md` được sửa. Bảy chỗ còn lại vẫn mô tả fail-fast như
hành vi sống — đã sửa hết trong đợt này:

* `req/user_stories/a1_student.md` — **nghiêm trọng nhất**: một tiêu chí Given-When-Then khẳng định "các
  testcase còn lại không được chạy tiếp", **mâu thuẫn trực tiếp** với tiêu chí tương ứng ở
  `a4_system.md`. Test sinh ra từ tiêu chí này sẽ kiểm đúng cái ngược lại với hành vi thật.
* `req/user_stories/a2_instructor.md` · `screens/teacher/instructor_grading.md` (2 chỗ) ·
  `screens/shared/problem_authoring.md` (2 chỗ) · `screens/users/submission_result.md` ·
  `overview/system_survey.md` mục 5.5 và mục 6.1 (2 bước của luồng chính) · `09-layoutBase/github.md`.

### 6.3. Bảng ma trận quyền của survey lệch với ma trận thật

`system_survey.md` mục 4.3 còn hàng **"Kích hoạt chấm lại"**, trong khi
`screens/admin/admin_permission_matrix.md` mục 3 đã bỏ Function `REJUDGE_MANAGEMENT` từ 2026-08-28 — hai
bảng nói khác nhau. Đã bỏ hàng đó, và sửa thêm hai hàng sai actor: "Tạo bộ câu hỏi phỏng vấn" (mô tả
`F6-11` đã bỏ, nay là `F6-13` dùng chung A2+A3) và "Soạn đề bài" (A3 phải là **Có** — màn dùng chung theo
`DEC-2026-0825-shared-content-authoring-screens`). Mô tả vai trò A3 ở mục 4.1 cũng còn "kích hoạt chấm
lại".

### 6.4. Còn lại

`09-layoutBase/github.md`: slug `interview_bank_management` không tồn tại, dòng `admin_rejudge`, hai chỗ
trích `F6-11`. `06-plan/PROTOTYPE_DEBT.md`: tiêu đề mục 7.3 ghi "Còn mở — cần chủ dự án quyết" trong khi
cả 6 hạng mục đã `[x]`, và một mục "Còn mở" thực ra đã xong.

### 6.5. Kết quả script

| Trục | Kết quả |
| :--- | :--- |
| Mã `Fx-nn` dùng vs định nghĩa | 111 định nghĩa, **0 treo** |
| Mã đã hết hiệu lực mô tả như đang sống | **0** (mọi hit còn lại đều kèm dấu hết hiệu lực) |
| Citation `file:line` | **0 lỗi** |
| Cột bảng câu hỏi mở | **0 lệch** |
| Pictograph | **0** |
| Mã `DEC-` trích mà không tồn tại trong registry | **0** |

## 7. Rà soát 2026-09-09 — đợt vá vênh thứ tư

Ba đợt trước (2026-09-01, 09-03, 09-05) kiểm mã `Fx-nn`, câu hỏi mở, pictograph và **sự tồn tại** của
citation. Đợt này thêm hai trục mà cả ba đợt trước đều không kiểm: **citation `file:line` có trỏ đúng chỗ
hay không** (không chỉ đúng file và trong phạm vi dòng), và **một phát biểu đã sửa ở nơi phát sinh có lan
tới bảng tổng hợp hay chưa**. Bản ghi đầy đủ: `07-review/rd_drift_repair_260909.md`.

### 7.1. `system_survey.md:<dòng>` — 111 citation đều đã trôi

Trục nặng nhất của đợt. `01-rd/` có **111 citation neo theo số dòng vào `system_survey.md`** — file bị sửa
nhiều nhất của tầng RD. Script kiểm được 14 cái (những cái có mã `Fx-nn` trong cùng câu để đối chiếu):
**sai 14/14**. Kiểm tay thêm 4 cái: sai cả 4, lệch 30-100 dòng. Ví dụ `screens/users/solution_review.md`
trích `system_survey.md:321` cho `F5-05`, trong khi dòng 321 là bảng F4 và `F5-05` nằm ở dòng 350.

Mục 4 của tài liệu này khai "157/157 citation `file:line` trỏ đúng file và trong phạm vi số dòng" — đúng
theo nghĩa hẹp đó, nhưng script khi ấy chỉ kiểm **file có tồn tại** và **số dòng không vượt độ dài file**,
không kiểm **trỏ đúng nội dung**. Một anchor trôi vẫn "hợp lệ" với phép kiểm đó.

Đã chuyển **toàn bộ 111 citation sang neo ngữ nghĩa**: `01-rd/overview/system_survey.md` — mục 7.1 dòng
`problem_list`, mục 5.5 dòng `F5-05`, mục 8 bảng giới hạn phạm vi... Neo mục + tên dòng không chết khi file
được chèn thêm nội dung.

**Luật bổ sung, hiệu lực từ 2026-09-09:** cấm neo theo số dòng vào `01-rd/overview/system_survey.md`. Dùng
`mục <số> dòng <slug|mã>`. Đây là mở rộng đúng phạm vi của `DEC-2026-0831-rd-req-split-by-module` — đợt đó
đã chuyển 345 citation trong `req/` sang neo mã nhưng chưa áp cho `screens/`, nên `screens/` tiếp tục sinh
anchor theo dòng thêm hơn một tuần. Với các file **ổn định** (`09-layoutBase/*.dc.html` — prototype tĩnh,
đã đóng băng) thì neo theo dòng vẫn dùng được và vẫn đang đúng.

### 7.2. Sáu chỗ sửa ở nơi phát sinh mà không lan tới bảng tổng hợp

| Chỗ còn vênh | Nói gì | Đúng phải là |
| :--- | :--- | :--- |
| `overview/overview.md` mục 1 (nguyên lý bất đồng bộ) | Mục "**Fail-fast theo testcase**" mô tả việc bỏ các testcase còn lại như nguyên lý kiến trúc đang sống | `F4-04` hết hiệu lực, chạy hết N testcase (`DEC-2026-0831-partial-score-testcase-ratio`) |
| `overview/glossary.md` — thuật ngữ `batch` | "xử lý theo thứ tự **với fail-fast**" | Bỏ mệnh đề fail-fast; thứ tự không còn quyết định testcase nào bị bỏ |
| `system/judge_engine.md` mục 2.5 — bảng actor | A3 "**kích hoạt Re-judge**" | Rejudge ngoài phạm vi (`DEC-2026-0828-remove-rejudge-scope`) |
| `README.md` gốc mục 3 — bảng actor | A3 "**kích hoạt Re-judge**" | Cùng lý do. Đợt 2026-09-05 sửa 5 phát biểu khác trong file này nhưng sót bảng actor |
| `overview/system_survey.md` mục 4.3 | A2 thấy bài "**của mình/lớp mình**" | A2 theo **quyền tác giả**, không theo lớp phụ trách — bài toán không phải thực thể sở hữu theo lớp |
| `screens/admin/admin_overview.md` mục 3 | Còn khung "4 màn Admin cần scaffold i18n → còn 3 màn" | Q7 của chính file đó đã bỏ danh sách tên màn, mở rộng thành mọi màn Admin + Giảng viên (`DEC-2026-0831-i18n-scope-expansion`) |

Nặng nhất là dòng đầu: `overview.md` là file nền lý thuyết, nguồn chương 2 của báo cáo, và nó nói **ngược**
với `req/judge-orchestration.md` (F4-03) cùng `system/backend_architecture.md`. Mục 6.2 của tài liệu này
khai đã quét hết fail-fast ở 7 chỗ — bốn chỗ trong bảng trên không nằm trong 7 chỗ đó.

Ba chỗ nhỏ hơn cùng dạng đã sửa kèm: `req/ai-review.md` + `screens/users/mock_interview.md` (Q1) làm rõ
**hệ thống có ba rubric độc lập** — `F5-23` (Solution Review, admin cấu hình trọng số) · `F5-15` (bốn tiêu
chí kết phiên Mock Interview) · `F6-13` (tiêu chí đối chiếu Chế độ luyện); trước đó `mock_interview.md` neo
rubric của F5.2 vào `F5-23`, sai mã. Và `screens/teacher/class_management.md` Q6 bỏ mệnh đề "go-judge đã
fail-fast" khỏi căn cứ bỏ chấm lại (kết luận không đổi, chỉ đổi căn cứ).

### 7.3. Sáu citation còn trỏ vào hai file chỉ mục

Mục 1 của tài liệu này đã ghi "hai file gốc `req.md` / `user_stories.md` giờ là chỉ mục mỏng — đừng trích
dẫn `Fx-nn` từ chúng nữa", nhưng 6 chỗ vẫn trích: `screens/teacher/class_management.md` (Q2: `F1-23`,
`US-A2-07`), `overview/system_survey.md` (khối `F1-21`), `req/ai-review.md`, `req/identity.md`,
`req/interview-bank.md`, `req/judge-orchestration.md` (Câu hỏi mở của `user_stories.md`, nay ở
`req/user_stories/open_questions.md`). Đã trỏ lại đúng file nội dung. Các con trỏ điều hướng thuần
(bảng "đọc gì ở đâu") vẫn được trỏ vào file chỉ mục — đó là đúng vai của nó.

### 7.4. Một hạng mục đã kiểm và **không** phải vênh

Chuỗi cộng dồn tổng số màn ở mục 7 của survey (`29 → 30`, `30 → 31`, `31 → 30`, `30 → 31`) đọc liền nhau
thì tưởng lệch, nhưng mỗi mốc đều có ngày và khối tổng ở cuối mục 7 đã dựng lại đúng trình tự thời gian —
tổng chốt **31 màn sống / 32 file** là đúng. Chỉ là thứ tự vật lý trong file khác thứ tự thời gian. Không
sửa, để không tạo commit churn.

### 7.5. Kết quả script sau đợt vá

| Trục | Kết quả |
| :--- | :--- |
| Citation neo theo dòng vào `system_survey.md` | **0** (trước đợt: 111, sai 100% ở mẫu kiểm được) |
| Mã `Fx-nn` dùng vs định nghĩa | 111 định nghĩa, **0 treo**, **0 mã định nghĩa mà không ai dùng** |
| Mã `Fx-nn` hết hiệu lực mô tả như đang sống | **0** |
| `US-Axx-nn` treo | **0** |
| Mã `DEC-` trích mà không có trong registry | **0** (43 id) |
| Citation `file:line` trỏ file không tồn tại / vượt số dòng | **0** |
| Câu hỏi mở còn treo | **0** |
| Section màn khai trong file RD vs mục thật trong survey | **0 lệch** (32/32 slug) |
| Pictograph | **0** |

Trục cần thêm cho đợt sau: **kiểm chéo giữa `feature_catalog.md` và `feature_catalog.html`** (hiện khớp
111-111) và **kiểm citation `09-layoutBase/*.dc.html:<dòng>`** — đợt này chỉ kiểm phạm vi dòng, chưa kiểm
nội dung dòng đó có đúng thứ đang được trích hay không.

---

**Cảnh báo giữ nguyên cho tương lai:** nếu phát hiện một file bất kỳ trong `01-rd/` còn nhắc tới NestGame v2
(game, ROM, hệ máy, module IAM/Catalog/SaveState/Interaction/Tournament cũ), đó là nợ tài liệu mới phát
sinh — ghi lại vào mục này kèm ngày phát hiện, đừng âm thầm sửa mà không để lại dấu vết.
