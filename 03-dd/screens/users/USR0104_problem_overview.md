# DD màn hình — Tổng quan bài toán (`USR0104`)

> Hợp đồng giao diện theo `03-dd/screens/README.md` mục 4. API chi tiết chỉ ở DD module.

## 0. Thông tin tài liệu

Bảng 0.1 là bìa tài liệu.

**Bảng 0.1: Bìa.**

| Mục | Giá trị |
| :--- | :--- |
| Mã màn, slug | `USR0104`, `problem_overview` |
| Tên màn | Tổng quan bài toán |
| Actor | A1 `STUDENT` |
| Route | `/problems/{problemId}` [SoT: `02-bd/screens/users/USR0104_problem_overview.md`, Sheet 3] |
| Nguồn | `01-rd/screens/users/USR0104_problem_overview.md`; `02-bd/screens/users/USR0104_problem_overview.md` |
| Prototype | `05-coding/frontend/src/views/users/problem-overview/` (prototype Next.js; không có prototype tĩnh riêng) |
| Module | F2 `problem-bank`; đọc thêm F4 `judge-orchestration`, F5 `ai-review` |
| Phiên bản, ngày | V0.1, 2026-10-24 |
| TDD | `04-tdd/modules/F2_problem-bank.md`, `F4_judge-orchestration.md`, `F5_ai-review.md`, `04-tdd/screens/users/USR0104_problem_overview.md` |

**Bảng 0.2: Lịch sử sửa.**

| Ver | Nội dung | Ngày |
| :--- | :--- | :--- |
| V0.1 | Tạo DD màn; ánh xạ đủ hợp đồng và tiêu chí liên module | 2026-10-24 |

**Bảng 0.3: Ký hiệu nguồn.**

| Ký hiệu | Nguồn |
| :--- | :--- |
| RD, BD | `01-rd/screens/users/USR0104_problem_overview.md`, `02-bd/screens/users/USR0104_problem_overview.md` |
| F2, F4, F5 | `03-dd/api/F2_problem-bank.md`, `03-dd/api/F4_judge-orchestration.md`, `03-dd/api/F5_ai-review.md` |
| V | `05-coding/frontend/src/views/users/problem-overview/ui/problem-overview-view.tsx` |
| DEBT | `06-plan/PROTOTYPE_DEBT.md` |

## 1. Bản đồ slice FSD

**Bảng 1.1: Slice của màn.**

| Lớp | Đường dẫn từ `src/` | Vai trò | Hiện trạng |
| :--- | :--- | :--- | :--- |
| app | `app/(student)/problems/[problemId]/page.tsx` | Render view tổng quan | Có |
| views | `views/users/problem-overview/ui/problem-overview-view.tsx` | Đề Markdown/LaTeX, ví dụ, thông tin bài, bookmark và điều hướng | Có, mock |
| views | `views/users/problem-overview/model/` | Adapter DTO và trạng thái độc lập theo khối cho overview/recent submissions/quick insight | Đích DD; chưa có |
| entities | `entities/problem/api/queries.ts` | Query `GetProblemOverview` (F2 mục 7) và lệnh bookmark (F2 mục 12) | Hiện mock; cần đổi kiểu overview |
| entities | `entities/submission/api/queries.ts` | Query lượt nộp gần đây của người dùng theo bài | Chưa có query riêng cho hợp đồng này |
| entities | `entities/solution-review/api/queries.ts` | Query quick insight F5 | Chưa có query theo DTO F5 mục 6 |
| shared | `Markdown`/KaTeX, `Button`, `Badge`, `Card`, `Skeleton`, `ErrorState`, `toast`, `useT`, `useLocale` | Thành phần trình bày chung | Tái sử dụng; không cần nâng cấp shared |

Không có widget/feature mới. `ProblemOverviewView` nhận `problemId`; các query phụ nhận đúng `problemId`, không dùng `detail.mySubmissions` mock khi nối thật. Tuân chiều import `app → views → widgets → features → entities → shared`. Dữ liệu Markdown là dữ liệu không tin cậy; renderer chung xử lý an toàn.

## 2. API tiêu thụ

**Bảng 2.1: Ánh xạ sự kiện sang API.**

| EVT (BD) | ID hợp đồng | Hook/query đích | Nối |
| :--- | :--- | :--- | :--- |
| EVT-1 tải trang | F2 mục 7 `GetProblemOverview`, F2 mục 15 `ListProblemLevels`; F4 mục 4, endpoint #1 `GetEffectiveExecutionLimits` không truyền `language`; F4 mục 7, endpoint #4 `ListMySubmissionsForProblem`; F5 mục 6 `GetMySolutionReviewQuickInsight` | Các query độc lập theo vùng | Mock; giới hạn ba ngôn ngữ dùng F4 #1 sẵn có [SoT: F4:194-227] |
| EVT-2 mở Workspace | Không gọi API | `next/link` | Client |
| EVT-3 lưu bài | F2 mục 12 `ToggleMyProblemBookmark` | `toggleMyProblemBookmark(problemId, true)` | Mock |
| EVT-4 bỏ lưu | F2 mục 12 `ToggleMyProblemBookmark` | `toggleMyProblemBookmark(problemId, false)` | Mock |
| EVT-5 xem bài đã lưu | Không gọi API | `next/link` tới `/saved` | Client |
| EVT-6 copy ví dụ | Không gọi API | Clipboard API | Client |
| EVT-7 quay lại danh sách | Không gọi API | `next/link` tới `/problems` | Client |
| EVT-8 xem tất cả bài nộp | Không gọi API | `next/link` tới `/submissions` | Client |
| EVT-9 link not found | Không gọi API | `next/link` tới `/problems` | Client |
| EVT-10 mở kết quả | Không gọi API | Link tới `/submissions/{submissionId}` | Client |
| EVT-11 mở report đầy đủ | Không gọi API | Link tới `/submissions/{submissionId}/review` | Client |

Màn không dùng realtime. Không gộp quick insight F5 vào query F2: nếu F5 hỏng, phần đề và thao tác bookmark vẫn hoạt động. Không đọc hoặc đưa nội dung testcase ẩn tới trình duyệt; chỉ hiển thị số lượng được F2 cho phép.

## 3. Trạng thái màn

**Bảng 3.1: Trạng thái theo vùng.**

| Vùng | Trạng thái | Hiển thị và xử lý | Khoá i18n |
| :--- | :--- | :--- | :--- |
| Toàn trang | Loading | Skeleton cho cột đề và thẻ phụ; không hiển thị dữ liệu mock cũ của `problemId` trước đó | `problemOverview.loading` |
| Đề | `404`/bài không công khai | Thay nội dung bằng “Không tìm thấy bài toán.” và link `/problems` | `problemOverview.notFound` |
| Đề | Mạng/5xx | Giữ trang lỗi có nút thử lại; không suy diễn thành không tìm thấy | `apiError.*` theo `error.code` |
| Bookmark | Đang ghi | Khoá nút trong một mutation; lỗi thì hoàn tác trạng thái lạc quan, báo toast | `problemOverview.bookmark.*`, `apiError.*` |
| Recent submissions | Rỗng | Câu “Bạn chưa nộp bài này lần nào.” | `problemOverview.recent.empty` |
| Recent submissions | Lỗi | Ẩn êm thẻ theo BD EVT-1; không toast, không làm mất đề | Không hiện nhãn lỗi [SoT: BD Sheet 8 EVT-1] |
| Quick insight | Không có báo cáo | Ẩn thẻ | Không có |
| Quick insight | Lỗi/AI suy giảm | Ẩn thẻ; không hiện lỗi AI trên phần đề | Không có; lỗi không chặn |
| Copy ví dụ | Clipboard từ chối | Toast hướng dẫn chọn và sao chép thủ công | `problemOverview.copy.*` |

Frontend phân nhánh theo `error.code`; không đọc `message` từ server (`03-dd/api/api.md` mục 1.1).

## 4. Quy tắc đặc thù màn

**BR-SC-01 — Phân lập tải theo bounded context.** Kích hoạt khi vào route, với `problemId` hợp lệ. Bắt đầu F2 overview và các query phụ F4/F5 theo cùng ID; chỉ F2 quyết định trạng thái toàn trang. Nếu route đổi, huỷ/loại kết quả của ID cũ. F4/F5 lỗi thì ẩn riêng thẻ theo BD, không toast. Hậu điều kiện: đề và thao tác bookmark vẫn dùng được.

**BR-SC-02 — Bookmark lạc quan và Hoàn tác.** Kích hoạt EVT-3/4 khi không có mutation đang chờ. Gửi trạng thái đích `bookmarked`, khoá thao tác lặp đến khi hoàn tất. Bỏ lưu thành công hiện toast Hoàn tác; Hoàn tác gọi lại F2 mục 12 với đích `true` (không chỉ đảo state client), có thể mất ghi chú đã xoá theo hợp đồng bookmark nên không khôi phục ghi chú cũ. Lỗi ở bất kỳ lượt nào trả UI về giá trị server gần nhất và báo mã lỗi [SoT: BD Sheet 8 EVT-3, EVT-4].

**BR-SC-03 — Đích điều hướng theo ý định.** “Giải bài toán” mở `/workspace/{problemId}`; mọi liên kết chỉ để xem bài giữ ở `/problems/{problemId}`. “Quay về danh sách” mở `/problems`.

**BR-SC-04 — An toàn nội dung và link review.** Chỉ render statement/examples qua renderer Markdown an toàn; không truy vấn testcase ẩn. Link báo cáo đầy đủ dùng `review.submissionId` của F5 mục 6, vì báo cáo mới nhất theo thời điểm tạo không nhất thiết thuộc bài nộp `ACCEPTED` mới nhất [SoT: F5:383-400].

## 5. Kiểm tra phía client

**Bảng 5.1: Kiểm đầu vào ở màn.**

| Trường | Zod phía client | Quy tắc nguồn | Khớp |
| :--- | :--- | :--- | :--- |
| `problemId` từ route | `z.string().uuid()` | F2 mục 7; F4 mục 7; F5 mục 6 | Có; ID sai dừng trước khi gửi query |
| `bookmarked` | `z.boolean()` | `03-dd/validation/F2_problem-bank.md` mục 8, F2 mục 12 | Có |

Không có form; thao tác Copy dùng chuỗi từ ví dụ đã được F2 trả. Các quy tắc trên chỉ bảo vệ luồng UI; server kiểm lại theo DD module.

## 6. i18n và truy cập

Tiền tố khoá: `problemOverview.` và `apiError.`. Nút copy có tên truy cập riêng cho Input/Output và số thứ tự ví dụ. Các liên kết có văn bản mô tả đích. Bố cục cột sticky phải vẫn theo thứ tự đọc trên màn hẹp. Khi bookmark đổi, thông báo toast dùng vùng `aria-live="polite"`; trạng thái đã lưu được nêu bằng văn bản, không chỉ bằng màu/biểu tượng.

## 7. Điểm kiểm thử

**Bảng 7.1: Điểm kiểm thử và tiêu chí.**

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| SC-01 | Normal | EVT-1 overview và ví dụ render an toàn | Vitest | F2 AC-10 |
| SC-02 | Abnormal | EVT-1 bài không công khai hiển thị not found | Vitest | F2 AC-09, AC-10 |
| SC-03 | Normal | EVT-1 recent/quick insight thành công; empty insight ẩn card | Vitest | F4 AC-36; F5 AC-16 |
| SC-04 | Normal | EVT-2, EVT-5, EVT-7..EVT-11 điều hướng tới đúng đích | Playwright | `problem_overview` AC-SC-03 |
| SC-05 | Normal | EVT-3/4 bookmark gửi trạng thái đích; Hoàn tác gửi lại `true` | Vitest | F2 AC-15 |
| SC-06 | Abnormal | EVT-3/4 lỗi rollback; EVT-6 copy bị từ chối báo hướng dẫn | Vitest | F2 AC-15; `problem_overview` AC-SC-02, AC-SC-03 |
| SC-07 | Boundary | EVT-1 không có testcase ẩn nào ngoài số đếm trong payload/UI | Vitest | F2 AC-11 |

## 8. Nợ prototype và câu hỏi mở

**Nợ prototype:** view đang đọc `ProblemDetail` mock, tự sắp `detail.mySubmissions`, bookmark chỉ đổi `useState` (kể cả Hoàn tác) [SoT: V:87-120]. Cần nối F2/F4/F5, trong đó F4 recent chỉ lấy ba lượt và F5 insight có nguồn riêng. Nợ prototype theo `06-plan/PROTOTYPE_DEBT.md` mục 9 còn mở; DD chỉ chốt thiết kế, không tự gỡ nợ code.

**Câu hỏi mở/lệch hợp đồng:**

| ID | Nội dung | Đề xuất mặc định | Bậc ponytail |
| :--- | :--- | :--- | :--- |
| D-1 | BD đặt tên `GetProblemLanguageLimits`, còn F4 #1 tên `GetEffectiveExecutionLimits` và trả cả ba khi vắng `language` | Dùng lại F4 #1, không thêm endpoint; đồng bộ tên ở BD khi rà chéo | 4 |
| D-2 | BD cần ba bài nộp gần đây; F4 #4 hỗ trợ `limit=3` | Đã ghi `USR0104` là consumer ở F4; không còn chỗ lệch hợp đồng | 3 |
| D-3 | BD còn hỏi về các trường Bình luận/Điểm/Nguồn; không thấy trong DD/API hiện tại | Để ngoài màn cho đến khi owner chốt | 3 |
