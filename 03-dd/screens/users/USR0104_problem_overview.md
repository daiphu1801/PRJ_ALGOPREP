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
| V0.2 | Thẻ Solution Review chỉ hiện hai độ phức tạp (đã đúng); gỡ trường `notes` khỏi kiểu `SolutionReviewQuickInsight` dùng chung (2026-11-12, owner, ponytail bậc 2) | 2026-11-12 |

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
| EVT-12 khách bấm Giải bài hoặc Lưu bài | Không gọi API | Mở `LoginPromptDialog` (mục 4A); không gọi `toggleMyProblemBookmark` | Client |

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
| Khách (không có phiên) | `useSession()` trả `guest` | Dựng đề và ví dụ bình thường. **Không gọi** F4 giới hạn thực thi, F4 lịch sử và F5 quick insight (F4 chưa mở cho khách); không dựng bảng giới hạn theo ngôn ngữ, thẻ Bài nộp gần đây và thẻ Solution Review. Nút Lưu luôn "Lưu bài"; Giải bài và Lưu bài mở `LoginPromptDialog` thay vì chuyển trang hoặc gọi API | `problemOverview.guest.*` |
| Phiên hết hạn giữa chừng | F2 mục 7 trả `200` nhưng lời gọi cá nhân trả `401` | Lời gọi cá nhân: làm mới phiên; thất bại thì xoá token và chuyển sang trạng thái khách, **không** chuyển trang (trang này công khai) | `apiError.*` |

Frontend phân nhánh theo `error.code`; không đọc `message` từ server (`03-dd/api/api.md` mục 1.1).

## 4. Quy tắc đặc thù màn

**BR-SC-01 — Phân lập tải theo bounded context.** Kích hoạt khi vào route, với `problemId` hợp lệ. Bắt đầu F2 overview và các query phụ F4/F5 theo cùng ID; chỉ F2 quyết định trạng thái toàn trang. Nếu route đổi, huỷ/loại kết quả của ID cũ. F4/F5 lỗi thì ẩn riêng thẻ theo BD, không toast. Hậu điều kiện: đề và thao tác bookmark vẫn dùng được.

**BR-SC-02 — Bookmark lạc quan và Hoàn tác.** Kích hoạt EVT-3/4 khi không có mutation đang chờ. Gửi trạng thái đích `bookmarked`, khoá thao tác lặp đến khi hoàn tất. Bỏ lưu thành công hiện toast Hoàn tác; Hoàn tác gọi lại F2 mục 12 với đích `true` (không chỉ đảo state client), có thể mất ghi chú đã xoá theo hợp đồng bookmark nên không khôi phục ghi chú cũ. Lỗi ở bất kỳ lượt nào trả UI về giá trị server gần nhất và báo mã lỗi [SoT: BD Sheet 8 EVT-3, EVT-4].

**BR-SC-03 — Đích điều hướng theo ý định.** “Giải bài toán” mở `/workspace/{problemId}`; mọi liên kết chỉ để xem bài giữ ở `/problems/{problemId}`. “Quay về danh sách” mở `/problems`.

**BR-SC-04 — An toàn nội dung và link review.** Chỉ render statement/examples qua renderer Markdown an toàn; không truy vấn testcase ẩn. Link báo cáo đầy đủ dùng `review.submissionId` của F5 mục 6, vì báo cáo mới nhất theo thời điểm tạo không nhất thiết thuộc bài nộp `ACCEPTED` mới nhất [SoT: F5:383-400].

**BR-SC-05 — Bố cục đọc đề và ví dụ.** View dựng đề từ `statementMd` và dùng `PageContainer` của khung Người học để căn chiều rộng theo danh sách bài toán trên desktop. Khi có ví dụ, liên kết Đến ví dụ trỏ tới `#problem-examples`. Mỗi ví dụ ghép hai khối Đầu vào và Đầu ra, kèm nhãn sao chép riêng cho trình đọc màn hình. Nội dung sao chép lấy trực tiếp từ chuỗi API. Ở màn hẹp, nhóm thao tác đặt trước đề theo cả thứ tự hiển thị và thứ tự tab. Bảng giới hạn chỉ hiển thị khi có phiên.

## 4A. Trạng thái khách và hộp thoại "Cần đăng nhập" (`DEC-2026-1126-guest-browse-published-problems`)

Bảng 4A.1 là hợp đồng của hộp thoại dùng chung cho `USR0104`, `USR0101` (liên kết Bài đã lưu) và thanh nav của khung Người học. BD: `02-bd/screens/users/_shell.md` mục 2.5A. Hộp thoại không có backend riêng.

Bảng 4A.1: Thành phần dùng chung cho khách.

| Thành phần | Vị trí FSD | Hợp đồng |
| :--- | :--- | :--- |
| `useSession()` | `entities/user` | Trả `guest` (không có token) hoặc `signedIn`. Không có trạng thái "đang tải" riêng vì `AuthProvider` chỉ dựng cây con sau khi đã thử khôi phục phiên. Đọc từ `tokenStore` (qua `hasSession()` của `shared/api`), không gọi mạng; đổi theo đăng nhập, đăng xuất và refresh bị từ chối |
| `LoginPromptDialog` | `features/login-prompt` | Props `{ open: boolean; returnTo: string; onClose(): void }`. Dựng trên `shared/ui/overlay/modal.tsx`; ba nút Đăng nhập, Đăng ký, Để sau |
| Đích của hai nút | - | `/login?redirect=<returnTo mã hoá>` và `/register?redirect=<returnTo mã hoá>`. `returnTo` luôn là đường dẫn nội bộ bắt đầu bằng `/`; `auth` lọc bằng `isSafeRedirect` (`shared/lib/safe-redirect.ts`), nên giá trị ngoài site bị bỏ |
| Đóng bằng Escape, bấm nền hoặc Để sau | - | Đóng, trả focus về nút đã mở hộp thoại, không đổi trang |

Quy tắc:

- **Màn tổng quan cho khách:** `returnTo = /problems/{problemId}`. Quay lại xong người dùng bấm lại nút; thao tác đã bấm không tự chạy (không lưu hành động chờ).
- **`RoleGate`:** hai route giữ nguyên vị trí trong `app/(student)`; `RoleGate` cho khách qua khi khu là `student` và đường dẫn khớp `isGuestReadablePath` (`/problems`, `/problems/{id}`). Mọi đường dẫn khác vẫn chuyển về `auth` kèm `?redirect=`.
- **Không gọi API cá nhân khi là khách:** `useMyProfile`, `useMyInProgress`, `useMyClassAssignments`, `useTopicProgress`, `useReviewedProblemIds`, `useReviewQuickInsight` tự đặt `enabled = hasSession()`; `fetchOverviewProblem` bỏ hai lời gọi F4 (giới hạn, lịch sử); `useSetBookmark` không được gọi vì nút Lưu mở hộp thoại.
- **Vai trò khác:** `INSTRUCTOR`/`ADMIN` mở `/problems` vẫn bị đưa về khu của mình như hiện nay (`RoleGate` chỉ nới cho khách); khu giảng viên và quản trị không đổi.

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
| SC-08 | Normal | EVT-1 khách: đề và ví dụ hiện, không gọi F4/F5/bookmark, không dựng hai thẻ cá nhân | Vitest | F2 AC-98; `problem_overview` AC-SC-04 |
| SC-09 | Normal | EVT-12 khách bấm Giải bài hoặc Lưu bài mở hộp thoại; Đăng nhập dẫn tới `/login?redirect=/problems/{id}`; Để sau đóng và giữ trang | Vitest | `problem_overview` AC-SC-04 |
| SC-10 | Abnormal | Bài nháp, đã gỡ hoặc id lạ với khách hiện "Không tìm thấy bài toán.", không chuyển trang | Vitest | F2 AC-99 |

## 8. Nợ prototype và câu hỏi mở

**Nợ prototype:** view đang đọc `ProblemDetail` mock, tự sắp `detail.mySubmissions`, bookmark chỉ đổi `useState` (kể cả Hoàn tác) [SoT: V:87-120]. Cần nối F2/F4/F5, trong đó F4 recent chỉ lấy ba lượt và F5 insight có nguồn riêng. Nợ prototype theo `06-plan/PROTOTYPE_DEBT.md` mục 9 còn mở; DD chỉ chốt thiết kế, không tự gỡ nợ code.

**Câu hỏi mở/lệch hợp đồng:**

| ID | Nội dung | Đề xuất mặc định | Bậc ponytail |
| :--- | :--- | :--- | :--- |
| D-1 | BD đặt tên `GetProblemLanguageLimits`, còn F4 #1 tên `GetEffectiveExecutionLimits` và trả cả ba khi vắng `language` | Dùng lại F4 #1, không thêm endpoint; đồng bộ tên ở BD khi rà chéo | 4 |
| D-2 | BD cần ba bài nộp gần đây; F4 #4 hỗ trợ `limit=3` | Đã ghi `USR0104` là consumer ở F4; không còn chỗ lệch hợp đồng | 3 |
| D-3 | BD còn hỏi về các trường Bình luận/Điểm/Nguồn; không thấy trong DD/API hiện tại | Để ngoài màn cho đến khi owner chốt | 3 |

## Nối API thật (F2-6 phần 2, 2026-11-06)

Bảng dưới liệt kê endpoint màn tiêu thụ và hiện trạng; hợp đồng chỉ định nghĩa ở `03-dd/api/F2_problem-bank.md`.

| Endpoint | Hiện trạng |
| :--- | :--- |
| `#6` | `fetchOverviewProblem`: `#6` cộng `/judge/problems/{id}/execution-limits` và `/judge/problems/{id}/submissions/mine`; `acRate=null` hiện `-` |
| `#11` | `useSetBookmark`; Hoàn tác gửi `bookmarked=true` |

Test đường thật: `views/users/problem-overview/ui/problem-overview-real.test.tsx` (6 ca), `workspace-queries.test.ts` (overview).
