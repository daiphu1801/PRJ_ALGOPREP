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
| `system_survey.md` | Khảo sát **chi tiết**: hệ thống tham khảo, khoảng trống và định vị, bốn actor kèm ma trận quyền, **bảng chức năng có mã `Fx-nn`**, bốn quy trình nghiệp vụ chính, **danh sách 20 màn dự kiến**, phạm vi, rủi ro, tiêu chí thành công | Viết cho AlgoPrep 2026-08-20 |

### `system/` — kiến trúc và môi trường

| File | Nội dung | Trạng thái |
| :--- | :--- | :--- |
| `backend_architecture.md` | Modular Monolith, tám module Maven, bốn tầng Clean Architecture, cổng vào và cổng ra, hai luồng dữ liệu điển hình, cách các module nói chuyện, cách ly judge engine (go-judge mặc định) và LLM | Viết cho AlgoPrep 2026-08-20 (trước đó là file rỗng); cập nhật engine 2026-08-23 |
| `frontend_architecture.md` | Next.js 16 App Router, sáu tầng FSD, tiêu chí đặt tầng, hai kênh realtime, bảo mật client | Viết cho AlgoPrep 2026-08-20 |
| `codebase_structure.md` | Cây thư mục monorepo, bên trong `05-coding/`, quy ước đặt tên xuyên tài liệu và mã | Viết cho AlgoPrep 2026-08-20; cập nhật engine 2026-08-23 |
| `environment.md` | Phiên bản đã chốt, hạ tầng Docker, công cụ QA, cổng chất lượng G-CHECK, chiến lược kiểm thử | Viết cho AlgoPrep 2026-08-20; cập nhật engine 2026-08-23 |
| `judge_engine.md` | Judge engine (go-judge mặc định): giới thiệu, vai trò, cổng trung lập để đổi engine, ràng buộc cgroup, cài đặt/chạy trên Windows, sự cố thường gặp | Viết cho AlgoPrep 2026-08-23 (trước đó là `judge0.md`, đổi tên theo `DEC-2026-0823-go-judge-default-engine`) |

### `req/` — yêu cầu

| File | Nội dung | Trạng thái |
| :--- | :--- | :--- |
| `req.md` | Yêu cầu chức năng (theo `Fx-nn`) và phi chức năng | Viết lại cho AlgoPrep 2026-08-23 |
| `user_stories.md` | User story theo actor A1-A4 và tiêu chí chấp nhận dạng Cho / Khi / Thì | Viết lại cho AlgoPrep 2026-08-24 (lần viết 2026-08-23 bị mất do một lệnh `git checkout` không kiểm tra kỹ, xem mục 3) |

### `screens/` — trục màn hình

Chưa tồn tại. Mỗi màn một file `<slug>.md`, slug viết `snake_case`; slice frontend tương ứng viết
`kebab-case` (`codebase_structure.md` mục 3).

---

## 2. Đọc theo thứ tự nào

| Bạn là | Đọc theo thứ tự |
| :--- | :--- |
| Người mới vào dự án | `README.md` (gốc repo) → `overview/overview.md` → `overview/glossary.md` |
| Người sắp viết mã backend | `overview/overview.md` mục 1.A đến 1.E → `system/backend_architecture.md` → `system/environment.md` |
| Người sắp viết mã frontend | `overview/overview.md` mục 1.H → `system/frontend_architecture.md` → `system/environment.md` mục 3.B và mục 4 |
| Người viết BD theo module | `overview/glossary.md` → `system_survey.md` mục 5 (bảng chức năng `Fx-nn`) → `system/backend_architecture.md` mục 8 (danh sách việc BD phải chốt) |
| Người viết BD theo màn | `system_survey.md` mục 7 (danh sách màn) và mục 6 (quy trình) → `system/frontend_architecture.md` |

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
`CLAUDE.md`, `.nexa/domain-registry.json`, `.nexa/control/status-tracking.md`,
`.nexa/control/dependency-map.md`, và chín file `01-rd/` liệt kê ở trên. Mọi chỗ còn nhắc "Judge0" trong các
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

**Cảnh báo giữ nguyên cho tương lai:** nếu phát hiện một file bất kỳ trong `01-rd/` còn nhắc tới NestGame v2
(game, ROM, hệ máy, module IAM/Catalog/SaveState/Interaction/Tournament cũ), đó là nợ tài liệu mới phát
sinh — ghi lại vào mục này kèm ngày phát hiện, đừng âm thầm sửa mà không để lại dấu vết.
