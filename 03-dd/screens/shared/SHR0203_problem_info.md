# DD màn hình — Chi tiết bài tập chỉ đọc (`SHR0203`)

## 0. Thông tin tài liệu

| Mục | Giá trị |
| :--- | :--- |
| Actor, route | A2 `/instructor/problems/{problemId}`; A3 `/admin/problems/{problemId}`; `problemId` UUID |
| Nguồn | `01-rd/screens/shared/SHR0203_problem_info.md`; `02-bd/screens/shared/SHR0203_problem_info.md` V1.12; `03-dd/api/F2_problem-bank.md` §34 |
| Prototype | `05-coding/frontend/src/views/shared/problem-info/ui/problem-info-view.tsx` |
| Phiên bản | V0.1, 2026-10-25; DD trục màn, Mode B (agent-native) |

Màn chỉ đọc, dùng chung dữ liệu F2 với `SHR0202`; không có form hay popup. BD Sheet 3 còn ghi `problemId` là mã số bỏ `#` theo mock, nhưng F2 §34.2 yêu cầu UUID. Chỉ nhãn `code` dùng để hiển thị.

## 1. Bản đồ slice FSD

| Lớp | Đường dẫn từ `src/` | Vai trò |
| :--- | :--- | :--- |
| app | `app/(admin)/admin/problems/[problemId]/page.tsx`; `app/(instructor)/instructor/problems/[problemId]/page.tsx` | Truyền UUID và `basePath` cố định từ route, không nhận basePath từ query |
| views | `views/shared/problem-info/ui/problem-info-view.tsx` | Ghép vùng đề bài, ví dụ, đặc tả, độ phủ, đáp án và thuộc tính; chỉ điều hướng |
| entities | `entities/problem/api/queries.ts`, `model/authoring-detail-adapter.ts` | `useProblemForAuthoring(problemId)` gọi F2 §34; chuyển DTO thật thành dữ liệu hiển thị, không tự dựng bài mẫu |
| shared | `MarkdownPreview`, `Card`, `Badge`, `Skeleton`, `EmptyState`, `ErrorState`, `IconAction` | Dùng lại; `MarkdownPreview` render Markdown và LaTeX, không chạy HTML thô |

`ProblemInfoView` nhận `{ problemId: string; basePath: "/admin/problems" | "/instructor/problems" }`. Nút Sửa, Xem như người học dùng cùng `problemId`, còn tên bài và mã trình bày lấy từ response. Không dùng `canViewerSeeProblem(problemId)` của mock để phân quyền; response `readOnly` và quyền F2 mới quyết định. [SoT: F2 §34.3; `problem-info-view.tsx:52-57, 76-132`]

## 2. API tiêu thụ

| Sự kiện | Endpoint | Hook | Ghi chú |
| :--- | :--- | :--- | :--- |
| Mở màn | F2 #33 `GET /authoring/problems/{problemId}` (§34) | `useProblemForAuthoring` | Một lần gọi đủ dữ liệu, kể cả `readOnly`; hiện mock `useProblemDraft` cần thay |
| EVT-1 quay lại | Không gọi API | Link tới `basePath` | Không có guard vì không có sửa |
| EVT-2 Sửa bài | Không gọi API | Link `{basePath}/{problemId}/edit` | Chỉ hiện khi `readOnly=false` và có quyền cập nhật |
| EVT-3 Xem như người học | Không gọi API | Mở `{basePath}/{problemId}/preview` ở tab mới | Trang preview cũng dùng F2 §34; chỉ tác giả hoặc Admin được mở |

Không gọi endpoint tiến độ học viên hoặc kết quả chạy đáp án mẫu. F2 §34 trả `checklist.sampleSolutionAllPass = null`; với tác giả/A3 màn chỉ hiện ngôn ngữ đáp án, không hiện `{passed}/{total}` như mock cũ. Với `readOnly=true`, ẩn thẻ đáp án mẫu. [SoT: BD Q2 đã đóng; F2 §34.3]

## 3. Trạng thái màn

| Điều kiện | Hành vi |
| :--- | :--- |
| Đang tải | Skeleton thanh tiêu đề, cột nội dung và cột thuộc tính; không hiện bài mẫu cho UUID bất kỳ |
| `404 PRB-010` | `EmptyState` “Không tìm thấy bài tập” cho cả UUID sai, bài đã xoá và bản nháp người khác; không phân biệt lý do |
| `403 PRB-002` | Trạng thái không đủ quyền, không hiện tên hay nội dung bài |
| Mạng/5xx | `ErrorState` và thử lại chỉ query chi tiết; giữ route hiện tại |
| Chi tiết của mình/A3 | Đầy đủ phần tác giả được phép thấy; nút Sửa và Preview theo quyền thực |
| A2 xem bài xuất bản của người khác | `readOnly=true`; ẩn Sửa, Preview, nội dung testcase, liên kết tải và toàn bộ thẻ đáp án mẫu; dùng `testcaseSummary` thay vì `testcases[]` |
| `spec` hoặc `referenceSolution` rỗng với tác giả/A3 | Hiện trạng thái “Chưa có” trong thẻ tương ứng; với `readOnly=true` ẩn thẻ đáp án mẫu |

## 4. Quy tắc đặc thù màn

### BR-SC-01 Quyền và dữ liệu khi đọc bài của người khác

F2 §34 cho A2 đọc bài `PUBLISHED` của người khác ở chế độ `readOnly`; A2 không được đọc bản nháp của người khác. Ở `readOnly`, dữ liệu testcase chỉ có tổng hợp, nên thẻ “Độ phủ theo loại ca” không thể dựng đúng: ẩn thẻ này và chỉ hiện các số tổng hợp mà `testcaseSummary` thực sự trả. F2 đặt `referenceSolution=null` và không trả mã/URL; màn ẩn toàn bộ thẻ đáp án mẫu, không hiện “Chưa có”. Không dựng mảng testcase giả hoặc hiển thị hidden input. [SoT: F2 §34.3–34.4; quyết định owner 2026-10-25]

### BR-SC-02 Điều hướng cùng khu và bản đã lưu

Quay lại `{basePath}`. Nút Sửa vào `{basePath}/{problemId}/edit`; nút Xem như người học mở `{basePath}/{problemId}/preview` trong tab mới kèm `rel="noopener noreferrer"`. Cả hai chỉ xuất hiện nếu cùng người sở hữu hoặc A3 có quyền tương ứng, không dựa riêng vào nhãn khu A2/A3. Preview đọc bản đã lưu từ F2, không lấy bản nháp trong bộ nhớ của màn sửa. [SoT: `DEC-2026-1002-split-detail-and-edit-pages`; BD SHR0202 Q6; F2 §34]

### BR-SC-03 Nội dung hiển thị và nguồn số

Đề bài đọc Markdown từ DTO qua `MarkdownPreview`, không nhận HTML thô. Ví dụ mẫu, ràng buộc, đặc tả, thuộc tính và ba cờ AI chỉ đọc đúng trường F2 §34. Tổng testcase được duyệt/chưa duyệt và số công khai tính từ dữ liệu có quyền đọc; với `readOnly` dùng `testcaseSummary`, không tính từ mảng rỗng. Không có tỷ lệ AC hay lượt nộp trong màn này. **Bỏ ô Stack (MB)** ở cả chi tiết và biên soạn vì DB/API không có nguồn (`DEC-2026-1025-dd-admin-language-display-and-stack`). [SoT: RD SHR0203 §3; BD Q2, Q4; F2 §34.3]

## 5. Kiểm tra phía client

Màn không có form. `problemId` phải đúng UUID trước khi gọi F2; sai dạng đi thẳng trạng thái không tìm thấy, không gọi API. `basePath` là union cố định do route truyền, không ghép URL từ input người dùng. Response được kiểm hình dạng ở lớp `entities/problem`: `readOnly=true` cho phép `testcases[]` thiếu nội dung và dùng `testcaseSummary`, `spec`/`referenceSolution` có thể `null`. Không áp schema của form biên soạn lên response chỉ đọc; validation nghiệp vụ nằm ở [F2](../../validation/F2_problem-bank.md).

## 6. i18n và truy cập

Dùng `problemInfo.*`, `problemAuthoring.*` cho nhãn chia sẻ và `apiError.PRB-*` cho lỗi. Trình đọc màn hình nhận `h1` là tên bài, nhãn trạng thái đọc được bằng chữ, mỗi thẻ có heading đúng bậc. Markdown/LaTeX hiển thị nội dung như dữ liệu; không cho HTML trong đề chạy script. Nút mở preview nói rõ “mở tab mới”; focus từ tiêu đề tới hành động rồi nội dung. Khối `<details>` đáp án mẫu cần tên điều khiển rõ và hỗ trợ Space/Enter mặc định. Bảng độ phủ không hiện khi response chỉ có tổng hợp để không đọc ra số 0 sai.

## 7. Điểm kiểm thử

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| PI-01 | Normal | A3 hoặc tác giả A2 xem bài, đề Markdown/LaTeX và các khối đúng DTO | Vitest | TDD màn `PI-AC-01` |
| PI-02 | Security | A2 xem bài xuất bản người khác: `readOnly`, không có nút Sửa/Preview, testcase hay mã đáp án mẫu | Vitest | TDD màn `PI-AC-01` |
| PI-03 | Security | A2 xem bản nháp người khác và UUID không tồn tại cùng trạng thái `PRB-010` | Vitest | TDD màn `PI-AC-01` |
| PI-04 | Boundary | `spec=null` hoặc `referenceSolution=null` của tác giả/A3: trạng thái rỗng đúng, không lỗi render | Vitest | TDD màn `PI-AC-02` |
| PI-05 | Normal | Quay lại danh sách cùng khu; sửa/preview giữ UUID, preview mở tab mới | Playwright | TDD màn `PI-AC-03` |
| PI-06 | Abnormal | F2 403 và lỗi mạng hiển thị khác 404; retry không đổi route | Vitest | TDD màn `PI-AC-03` |
| PI-07 | Boundary | Không hiện kết quả `{passed}/{total}`, Stack hay tỷ lệ AC khi DTO không có nguồn | Vitest | TDD màn `PI-AC-02` |

## 8. Nợ prototype và câu hỏi mở

`PROTOTYPE_DEBT.md` mục 17.1 còn nợ mock trả cùng bài cho mọi ID; `useProblemDraft` phải thay bằng F2 §34. Prototype vẫn tính độ phủ từ testcase ở mọi quyền, dùng `canViewerSeeProblem(problemId)` của mock, có Stack và kết quả chạy đáp án mẫu. BD Q2 đã bỏ kết quả chạy; Q6 preview tab mới đã được `SHR0202` chốt và code hiện có; Q8 quyền đọc của A2 đã chốt. Mục nợ cũ ghi preview chưa gắn hành động đã lỗi thời, cần sửa khi rà nợ sau khi nối API.

| # | Câu hỏi | Hướng DD / trạng thái |
| :--- | :--- | :--- |
| OQ-1 | **Đã chốt** `DEC-2026-1025-dd-admin-language-display-and-stack`: bỏ ô Stack (MB) | Gỡ khỏi `SHR0202`/`SHR0203` khi nối API; không thêm `stack_limit_mb` vào DB/API; đồng bộ BD `SHR0203` Q4 ở lượt BD |
| OQ-2 | A2 đọc bài công khai của người khác, DTO chỉ trả `testcaseSummary` nên không có độ phủ theo loại ca | Ẩn thẻ độ phủ với `readOnly=true`; chỉ hiện các số tổng hợp có nguồn |

TDD màn đã có tại `04-tdd/screens/shared/SHR0203_problem_info.md`.
