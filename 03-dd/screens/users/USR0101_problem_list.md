# DD màn hình — Danh sách bài tập (`USR0101`)

> Tuân khung `03-dd/screens/README.md` mục 4 (chín mục 0 đến 8) và quy tắc cứng mục 5. Màn này chỉ ghi hợp đồng xuyên biên giới và quyết định kỹ thuật không tự suy ra được; request, response, mã lỗi nằm ở file API của module và chỉ được liệt kê theo ID (tên tham số truy vấn nêu ở Bảng 2.2 chỉ để ánh xạ điều khiển sang tham số, không khai lại kiểu và ràng buộc). Không có logic phía backend ở đây.

## 0. Thông tin tài liệu

Bảng 0.1: Bìa tài liệu.

| Mục | Giá trị |
| :--- | :--- |
| Mã màn, slug | `USR0101`, `problem_list` |
| Tên màn | Danh sách bài tập |
| Nhóm actor | `users`, A1 (`STUDENT`) |
| Route | `/problems`; trạng thái lọc nằm trên query `q`, `status`, `difficulty`, `topic`, `sort`, `dir`, `page` [SoT: BD:586-591] |
| Bounded Context đứng sau | F2 `problem-bank` (chủ), đọc thêm F4 `judge-orchestration` (bài đang làm dở) và F5 `ai-review` (nhãn Solution Review); trạng thái giải của người học do F2 gọi F1 qua cổng trong tiến trình, **client không gọi F1** (mục 2) |
| RD nguồn | `01-rd/screens/users/USR0101_problem_list.md` |
| BD nguồn | `02-bd/screens/users/USR0101_problem_list.md` (V1.1) |
| Prototype | `05-coding/frontend/src/views/users/problem-list/` (một mock gộp, lọc và phân trang ở máy khách) |
| Phiên bản, ngày | V0.1, 2026-10-24 |
| Tiêu chí nghiệm thu | `04-tdd/screens/users/USR0101_problem_list.md` (luồng xuyên module) cộng các `AC` module ghi ở mục 7 |

Bảng 0.2: Lịch sử sửa.

| Ver | Mục | Nội dung | Ngày |
| :--- | :--- | :--- | :--- |
| V0.1 | Toàn bộ | Tạo mới từ prototype đã dựng và DD của F2, F4, F5. Chốt cách tách mock gộp thành sáu truy vấn độc lập, quy tắc trạng thái lọc trên URL, suy giảm từng khối, danh sách lệch cần chủ dự án quyết | 2026-10-24 |

Bảng 0.3: Viết tắt nguồn trích dẫn dùng trong file này.

| Viết tắt | Đường dẫn |
| :--- | :--- |
| RD | `01-rd/screens/users/USR0101_problem_list.md` |
| BD | `02-bd/screens/users/USR0101_problem_list.md` |
| F2, F4, F5 | `03-dd/api/F2_problem-bank.md`, `F4_judge-orchestration.md`, `F5_ai-review.md` |
| V2 | `03-dd/validation/F2_problem-bank.md` |
| API | `03-dd/api/api.md` |
| PLV | `05-coding/frontend/src/views/users/problem-list/ui/problem-list-view.tsx` |
| IPL | `05-coding/frontend/src/entities/problem/ui/in-progress-list.tsx` |
| EP | `05-coding/frontend/src/entities/problem/` |
| DEC24 | `DEC-2026-1024-dd-screens-users-core-contract-changes` (`.nexa/control/decision-registry.md:4675`) |
| DEBT | `06-plan/PROTOTYPE_DEBT.md` |

## 1. Bản đồ slice FSD

Thứ tự import bắt buộc `app → views → widgets → features → entities → shared`; `eslint.config` chặn ngược chiều và chặn entity import entity khác. Bảng 1.1 là bản đồ đích; cột "Hiện trạng" ghi prototype đang ở đâu.

Bảng 1.1: Slice của màn.

| Lớp | Đường dẫn (từ `src/`) | Vai trò | Hiện trạng |
| :--- | :--- | :--- | :--- |
| app | `app/(student)/problems/page.tsx` | Chỉ render `ProblemListView` | Có [SoT: `src/app/(student)/problems/page.tsx:1-5`] |
| views | `views/users/problem-list/ui/problem-list-view.tsx` | Ghép tiêu đề, thanh chủ đề, bộ lọc, bảng, hai khối phụ; **không** bọc cả trang trong một `QueryBoundary` | Có, 499 dòng, bọc cả trang trong `QueryBoundary` [SoT: PLV:69-76] |
| views | `views/users/problem-list/model/list-params.ts` | zod đọc query URL thành tham số bảng (mục 5), dùng S1 | Chưa có |
| entities | `entities/problem/api/queries.ts` | `useProblemList(params)`, `useTopicProgress`, `useProblemLevelList`, `useMyClassAssignments`, `useInProgressProblems(limit)`, `useRandomProblem` (mutation) | Mới có `useProblemListPage` mock gộp bốn nguồn [SoT: EP api/queries.ts:39-45] |
| entities | `entities/problem/model/list-params.ts` | Kiểu và hàm ánh xạ giá trị URL ↔ enum F2 (`todo` ↔ `UNSOLVED`, `difficulty` ↔ `level`), `levelTone(code)` | Chưa có; `problemLevelTone` đọc `level-store` mock [SoT: `entities/problem/model/level-store.ts:36`] |
| entities | `entities/solution-review/api/queries.ts` | Thêm `useReviewedProblemIds` (F5 mục 5). Đặt ở entity của F5 vì entity không import entity | Chưa có; mock giữ `hasSolutionReview` ngay trong dòng bảng [SoT: `entities/problem/model/types.ts:37`] |
| entities | `entities/problem/ui/in-progress-list.tsx` | Khối thẻ bài đang làm dở, **dùng chung với `USR0105`** | Có; cần đổi props theo DTO F4 mục 11 (BR-SC-05) [SoT: IPL:24-87] |
| shared | `shared/lib/use-url-list-state.ts` (S1), `shared/lib/use-debounced-value.ts` (S2) | Đọc và ghi trạng thái lọc trên URL; trễ 400 ms cho ô tìm kiếm | Chưa có |
| shared | `shared/api` (`useErrorMessage`, S3) | Dịch `error.code` sang chuỗi i18n | Chưa có, xem DD `USR0102` mục 1 (S3) |

Thành phần dùng lại từ `shared/`: `Badge`, `Button`, `Card`, `DataTable` (sắp xếp phía máy chủ: màn giữ trạng thái, bảng chỉ vẽ nút và `aria-sort` [SoT: `shared/ui/data/data-table.tsx:41-45`]), `Pagination` (có bộ chọn số dòng), `FilterBar`, `FilterMenu`, `QueryError`, `Skeleton`, `SeeAllLink`, `EmptyState` (`shared/ui`), `formatDateVN`, `usePersistedPageSize`, `toast` (`shared/lib`), `useMockQuery`, `useMockMutation`, `ApiError` (`shared/api`), `useT`, `useLocale` (`shared/i18n`) [SoT: PLV:31-45].

Nâng lên `shared/` trước khi nối API (luật "Frontend: shared first"):

- **S1 `use-url-list-state`.** Nhận lược đồ zod và giá trị mặc định, trả tham số đã chuẩn hoá cùng hàm đổi tham số với chế độ `replace` hoặc `push`; đổi bất kỳ tham số nào ngoài `page` thì tự đặt `page = 1`. Bốn màn danh sách cần cùng một cơ chế (`USR0101`, `USR0103`, và hai màn đang ghi nợ URL: `USR0202` [SoT: DEBT:2085 mục (5)], `USR0401` [SoT: DEBT:2087 mục (7)]), nên thuộc `shared`.
- **S2 `use-debounced-value`.** Trả giá trị sau khi người dùng ngừng gõ; dùng chung với `USR0103`.

**Lệch có chủ ý so với BD 4.5.** BD đề xuất `features/problem-topic-filter`, `features/problem-filter`, `widgets/problem-table`, `widgets/class-assignment-panel` [SoT: BD:328-332]. DD giữ **một view slice**: bộ lọc đã là `FilterBar` và `FilterMenu` dùng chung, thanh chủ đề, bảng và khối bài tập lớp mỗi thứ chỉ một nơi dùng (`CLAUDE.md`: một nơi dùng thì ở lại slice). Hai phần có nhiều nơi dùng được nâng lên: logic URL vào `shared` (S1, S2), khối thẻ bài dở ở lại `entities/problem/ui` như BD V1.1 đã chốt [SoT: BD:331].

## 2. API tiêu thụ

Bảng 2.1 liệt kê, theo từng sự kiện BD, endpoint (theo mục của file API), hook và trạng thái nối. Cột "Nối" là `mock` nếu prototype đang giả lập. Hiện **mọi dòng đều mock**, qua một hook gộp duy nhất [SoT: EP api/queries.ts:39-45].

Bảng 2.1: Sự kiện, endpoint, hook.

| EVT (BD) | Endpoint theo ID | Hook (`entities/*/api/`) | Nối |
| :--- | :--- | :--- | :--- |
| EVT-1, EVT-2 đến EVT-9 (gồm EVT-4 tab độ khó, EVT-8 sắp AC rate) | F2 mục 2 `ListPublishedProblems` | `useProblemList(params)` | mock (`items` trong `useProblemListPage`) |
| EVT-1 | F2 mục 4 `ListTopicsWithProgress` | `useTopicProgress` | mock (`topics` trong mock gộp) |
| EVT-1 | F2 mục 15 `ListProblemLevels` | `useProblemLevelList` | mock, đồng bộ qua `level-store` (không phải lời gọi) |
| EVT-1 | F2 mục 5 `ListMyClassAssignments` | `useMyClassAssignments` | mock (`classAssignments` trong mock gộp) |
| EVT-1 | F4 mục 11 `ListMyUnsolvedRecentProblems` (`limit = 3`) | `useInProgressProblems(3)` | mock (`inProgress` trong mock gộp) |
| EVT-1 | F5 mục 5 `ListMyReviewedProblemIds` | `useReviewedProblemIds` (`entities/solution-review`) | chưa có |
| EVT-11 | F2 mục 3 `GetRandomProblem` | `useRandomProblem` (`useMutation`: kết quả ngẫu nhiên, không cache) | chưa có, máy khách tự bốc [SoT: PLV:153-160] |
| EVT-10, 12, 13, 14, 15 | không gọi API | điều hướng `next/link` | không áp dụng |

Màn **không** gọi `ListMyProblemSolveStates` dù BD liệt kê ở Sheet 7.3 NO 5 [SoT: BD:564]: F2 đọc trạng thái giải qua `SolveStatePort` rồi trả `solveState` ngay trong dòng bảng, dòng bài tập lớp và tiến độ chủ đề, kèm cờ `solveStateAvailable` [SoT: F2:273-275, 288, 367-368]. Kênh realtime: không có (màn không dùng STOMP hay SSE).

Bảng 2.2 ánh xạ điều khiển sang tham số URL và tham số F2 (chỉ tên và giá trị; kiểu và ràng buộc ở F2 mục 2.2 và V2 mục 9).

Bảng 2.2: Điều khiển, tham số URL, tham số F2.

| Điều khiển | Tham số URL | Tham số gửi F2 mục 2 | Ghi chú |
| :--- | :--- | :--- | :--- |
| Ô tìm kiếm | `q` | `q` | gửi giá trị đã cắt khoảng trắng; rỗng thì bỏ tham số |
| Menu trạng thái | `status` = `solved`, `attempted`, `todo` | `status` = `SOLVED`, `ATTEMPTED`, `UNSOLVED` | chữ thường ở URL cho khớp khoá i18n `state.*` của bản dựng; ánh xạ ở `entities/problem/model/list-params.ts` |
| Menu độ khó | `difficulty` (= `levelCode`) | `difficulty` | |
| Thanh chủ đề | `topic` (= `topicId` của F2 mục 4) | `topic` | |
| Tiêu đề cột "Độ khó", "AC rate" | `sort` = `level`, `acRate`; `dir` = `asc`, `desc` | `sort`, `dir` | khoá cột `difficulty` của `DataTable` ↔ `level` ở API |
| Nút số trang | `page` | `page` | |
| Chọn số dòng | không lên URL; `localStorage` khoá `algoprep-problem-list-page-size` [SoT: PLV:94-98] | `pageSize` = 12, 24, 48 | |
| Nút "Bài ngẫu nhiên" | không có | F2 mục 3: `q`, `status`, `difficulty`, `topic` | không gửi `sort`, `dir`, `page` [SoT: F2:323] |

## 3. Trạng thái màn

Bảng 3.1 liệt kê trạng thái theo vùng. Khoá i18n có tiền tố `problemList.` (hoặc `common.` cho chuỗi dùng chung). Danh sách F2, F4, F5 mà màn gọi chỉ trả mã chung `401` (xử lý ở `shared/api/client.ts`), không có mã `PRB`, `JDG`, `AIR` riêng, nên mọi lỗi tải ánh xạ vào `common.error` [SoT: F2:279-284, 370-374, 411-415]. Frontend không đọc `message` [SoT: API:22].

Bảng 3.1: Trạng thái theo vùng.

| Vùng | Trạng thái | Điều kiện | Hiển thị | Khoá i18n |
| :--- | :--- | :--- | :--- | :--- |
| Tiêu đề, hai nút | Luôn dựng | không phụ thuộc truy vấn nào | Tiêu đề, "Bài đã lưu", "Bài ngẫu nhiên" hiện ngay | `title`, `summary.*` |
| Bảng | Đang tải | lần đầu chưa có dữ liệu | `Skeleton` đúng `pageSize` dòng [SoT: BD:465] | `common.loading` |
| Bảng | Đang tải lại | đổi bộ lọc hoặc trang, đã có dữ liệu cũ | giữ dòng cũ, làm mờ, `aria-busy`; nút "Bài ngẫu nhiên" chưa bấm được | không áp dụng |
| Bảng | Rỗng do lọc | `items = []`, có ít nhất một bộ lọc khác mặc định | "Không có bài nào khớp bộ lọc" kèm nút "Xóa bộ lọc" | `table.empty`, `table.clearFilters` |
| Bảng | Rỗng thật | `items = []`, không có bộ lọc | chỉ câu "Không có bài nào khớp bộ lọc", không nút xoá [SoT: BD:458] | `table.empty` |
| Bảng | Lỗi lần đầu | F2 mục 2 lỗi khi chưa có dữ liệu | vùng lỗi thay bảng, nút thử lại, giữ bộ lọc [SoT: BD:465, 597] | `common.error`, `common.retry` |
| Bảng | Lỗi lần sau | F2 mục 2 lỗi khi đã có dữ liệu cũ | giữ dòng cũ, toast lỗi [SoT: BD:598] | `common.error` |
| Dấu trạng thái, menu trạng thái | Ẩn | `solveStateAvailable = false` | ẩn chấm trạng thái trên mọi dòng **và** ẩn menu "Lọc theo trạng thái" (máy chủ bỏ lọc `status` trong trường hợp này [SoT: F2:288]); bảng vẫn hiện | không áp dụng |
| Nhãn Solution Review | Ẩn | F5 mục 5 lỗi hoặc chưa tải xong | không vẽ nhãn, không toast [SoT: BD:470, 631] | không áp dụng |
| Thanh chủ đề | Đang tải | | 7 ô chờ [SoT: BD:446] | |
| Thanh chủ đề | Ẩn | F2 mục 4 lỗi, hoặc không có chủ đề nào | ẩn cả thanh; BD chỉ nêu trường hợp rỗng, ẩn khi lỗi là `[SoT: Suy luận]` | không áp dụng |
| Thanh chủ đề | Mất tỉ lệ | `solved = null` | vẫn hiện tên chủ đề, ẩn `{solved}/{total}` [SoT: BD:448; F2:367] | không áp dụng |
| Menu độ khó | Ẩn | F2 mục 15 lỗi | ẩn menu; nhãn độ khó trong bảng vẫn hiện vì lấy từ `levelDisplayName` của từng dòng `[SoT: Suy luận]` | không áp dụng |
| Bài đang làm dở | Ẩn cả khối | F4 mục 11 lỗi, hoặc không có bài nào | ẩn khối kèm tiêu đề, không khối rỗng [SoT: BD:489] | không áp dụng |
| Bài tập lớp | Ẩn cả khối | F2 mục 5 lỗi, hoặc `groups = []` | ẩn khối kèm tiêu đề [SoT: BD:500; F2:417] | không áp dụng |
| Bài ngẫu nhiên | Không có bài | F2 mục 3 trả `problemId = null` | ở lại trang, toast cảnh báo | `toast.randomEmpty` |
| Toàn màn | Chưa đăng nhập | `401` sau khi làm mới token thất bại | chuyển về `auth` (do `shared/api/client.ts`) | không áp dụng |
| Toàn màn | Quyền bị từ chối | tài khoản không phải `STUDENT` | chặn ở nhóm route `(student)`; không dựng UI `403` riêng `[SoT: Suy luận]` | không áp dụng |

## 4. Quy tắc đặc thù màn

Sáu khối dưới là những việc đi qua nhiều sự kiện hoặc không tự suy ra được từ BD và file API. Đánh số cục bộ file này (`BR-SC-nn`).

### BR-SC-01 Khởi tạo: sáu truy vấn độc lập

- **Kích hoạt:** EVT-1.
- **Tiền điều kiện:** đã đăng nhập.
- **Các bước:**
  1. Đọc query URL bằng lược đồ ở mục 5; giá trị lạ bị bỏ qua, không báo lỗi [SoT: BD:628].
  2. Gửi song song F2 mục 2, mục 4, mục 5, mục 15, F4 mục 11, F5 mục 5. Chỉ F2 mục 2 phụ thuộc tham số URL; năm truy vấn còn lại có khoá không chứa tham số lọc nên **không gọi lại khi đổi bộ lọc** [SoT: BD:490, 502].
  3. Nhãn Solution Review: dòng nào có `id` thuộc tập `problemIds` của F5 mục 5 thì vẽ nhãn; tập chưa về thì chưa vẽ nhãn nào.
  4. Bốn khối phụ dùng `staleTime = 0` để quay lại từ màn khác (ví dụ vừa nộp bài `Accepted`) thấy dữ liệu mới ngay, không cần ai bắn sự kiện làm mới `[SoT: Suy luận]`.
- **Hậu điều kiện:** chỉ F2 mục 2 lỗi lần đầu mới hiện vùng lỗi có thử lại; mọi lỗi khác chỉ làm mất đúng khối của nó (Bảng 3.1) và **không bao giờ** chặn bảng [SoT: BD:597, 631].

### BR-SC-02 Bộ lọc, sắp xếp và trang nằm trên URL

- **Kích hoạt:** EVT-2 đến EVT-9.
- **Quy tắc:**
  1. Mọi thay đổi ngoài `page` đặt `page = 1` (S1).
  2. Lịch sử: `replace` cho `q`, `status`, `difficulty`, `topic`, `sort`, `dir`; `push` cho `page` [SoT: BD:589-591]. `sort` và `dir` theo nhóm `replace` là `[SoT: Suy luận]`, BD không nêu.
  3. Ô tìm kiếm: giá trị đang gõ giữ ở state cục bộ, ghi vào URL sau 400 ms ngừng gõ (S2) [SoT: BD:455]; nhấn Enter bỏ chờ và ghi ngay. Giá trị khởi tạo lấy từ `q`.
  4. Sắp xếp: bấm cột khác thì chuyển sang cột đó chiều `asc`; bấm cột đang sắp thì đảo chiều [SoT: BD:603]; hành vi này đã có trong `DataTable`. Khi URL không có `sort`, bảng không hiện mũi tên và máy chủ dùng mặc định `code` tăng dần [SoT: F2:260]; truyền `sort.key = "code"` (khác khoá mọi cột sắp được) để không cột nào sáng.
  5. Đổi khoá truy vấn thì giữ dữ liệu cũ trên bảng cho đến khi dữ liệu mới về (`placeholderData`). Lần tải sau mà lỗi: giữ bảng cũ và toast [SoT: BD:598, 605].
  6. `page` vượt quá số trang: máy chủ trả trang cuối [SoT: F2:143]; màn đồng bộ lại URL `page = page.number` bằng `replace`.
  7. Giá trị `difficulty`, `topic` không có trong danh mục đã tải (F2 mục 15, mục 4) thì bỏ khỏi yêu cầu và khỏi URL bằng `replace`, để menu không hiện một lựa chọn không tồn tại trong khi máy chủ coi như "Tất cả" [SoT: BD:628; F2:257-258].
  8. `status` ở URL không bị xoá khi `solveStateAvailable = false`; chỉ ẩn menu (Bảng 3.1).
- **Hậu điều kiện:** mở một URL đã lọc từ trình duyệt hay nút Quay lại cho đúng trạng thái đã lọc; không có dữ liệu nhập chưa lưu nên không hỏi xác nhận rời màn [SoT: BD:591].

### BR-SC-03 Enter trong ô tìm kiếm báo số bài

- **Kích hoạt:** EVT-2, phím Enter.
- **Các bước:** bỏ chờ 400 ms, ghi `q` vào URL, đặt cờ "báo sau khi tải xong"; khi truy vấn của đúng `q` đó trả về thì toast thông tin theo `page.totalItems` (`toast.searchResult` hoặc `toast.searchEmpty`) rồi hạ cờ. Gõ dở không toast [SoT: BD:598].
- **Khác bản dựng:** bản dựng đếm trên mảng đã lọc ở máy khách và toast ngay [SoT: PLV:163-169]; số thật chỉ có sau khi F2 trả `totalItems`.

### BR-SC-04 Bài ngẫu nhiên

- **Kích hoạt:** EVT-11.
- **Tiền điều kiện:** bảng không đang tải và `totalItems > 0` (nút dùng `aria-disabled`, không dùng `disabled`, để vẫn giải thích được lý do [SoT: BD:439; `02-bd/_rules/bd-template-9sheet.md:62`]).
- **Các bước:** gọi F2 mục 3 với đúng bộ lọc đang áp (Bảng 2.2); trong lúc chờ khoá nút để không bốc hai lần; `problemId = null` thì ở lại trang và toast `toast.randomEmpty` [SoT: BD:630]; ngược lại `router.push('/problems/{problemId}')` (UUID, `DEC24`). Lỗi gọi máy chủ: toast `common.error`, ở lại.
- **Lưu ý:** khi `solveStateAvailable = false`, F2 bỏ lọc `status` mà không báo [SoT: F2:288, 335]. Màn dùng cờ của lần tải bảng gần nhất: cờ tắt thì **không gửi `status`** dù URL còn giá trị cũ, để kết quả bốc không tự nhận là đã lọc theo trạng thái.

### BR-SC-05 Khối "Bài đang làm dở" (dùng chung với `USR0105`)

- **Kích hoạt:** EVT-1, EVT-13, EVT-15.
- **Quy tắc:**
  1. Gọi F4 mục 11 với `limit = 3` ở màn này (`USR0105` dùng 12) [SoT: F4:647].
  2. Thẻ có `problem = null` (F2 lỗi khi ghép tên) bị bỏ vì thiếu tên để hiển thị [SoT: F4:649]. Nếu **mọi** thẻ bị bỏ mà `items` không rỗng thì coi cả khối là lỗi (ẩn), không coi là rỗng, để tránh khẳng định "chưa có bài dở" khi chỉ là lỗi ghép tên `[SoT: Suy luận]`.
  3. Thẻ hiển thị `problem.title`, nhãn ngôn ngữ từ `language`, nhãn kết quả từ `status` (chín giá trị F4 ánh xạ vào khoá `verdict.*`, `JUDGING` ↔ `running`). Chỉ ghép `{đạt}/{tổng}` khi cả `passedCount` và `totalCount` khác `null` và `status` không thuộc `PENDING`, `COMPILING`, `JUDGING`; ba trạng thái đó chỉ hiện nhãn "Đang chấm" hoặc tương ứng [SoT: BD:493; F4:61]. BD chỉ nêu `WRONG_ANSWER` ghép đạt/tổng [SoT: BD:408]; DD mở rộng sang mọi trạng thái cuối có số đếm vì F4-13 tính tỉ lệ cho cả `TIME_LIMIT_EXCEEDED`, `RUNTIME_ERROR` `[SoT: Suy luận]`. Bản dựng luôn in `{đạt}/{tổng}` [SoT: IPL:71-76].
  4. Không có thẻ nào thì ẩn **cả khối kèm tiêu đề** và liên kết "Xem tất cả"; có thẻ thì dưới khối có `SeeAllLink` tới `/workspace` [SoT: BD:489, 494]. Bản dựng vẽ khối với câu rỗng [SoT: PLV:436-447].
  5. Mỗi thẻ là liên kết `/workspace/{problemId}` (UUID, `DEC24`), vào thẳng màn soạn mã [SoT: BD:135].
- **Bài đã rút xuống:** F4 mục 11 lọc ở máy chủ trước khi áp `limit`, nên thẻ đang làm dở không hiện bài đã rút [SoT: `02-bd/screens/users/USR0105_workspace_home.md:451`; F4 §11].

### BR-SC-06 Khối "Bài tập lớp"

- **Kích hoạt:** EVT-1, EVT-14.
- **Quy tắc:** nhóm theo `classId` (khoá `key`, thay `className` của bản dựng [SoT: PLV:459]); tối đa 3 nhóm do máy chủ cắt, màn không cắt lại; mỗi bài là liên kết `/problems/{problemId}`; chấm trạng thái chỉ vẽ khi `solveState` khác `null`. Dòng phụ chỉ có ngày giao `giao {assignedAt}` định dạng `formatDateVN` (UTC+7) [SoT: F2:407-409; BD:420]. **Không** có "x / y bài": F2 không trả số đó (OQ-1). Rỗng hoặc lỗi thì ẩn cả khối (Bảng 3.1). Không có liên kết "Xem tất cả" [SoT: BD:293].

## 5. Kiểm tra phía client

Màn không có form ghi; chỉ có ô tìm kiếm và chuẩn hoá query URL. Bảng 5.1 ánh xạ sang zod (`zod 3.24.1`, `package.json:35`) và quy tắc ở V2. Cột "Khớp" là Có, Lệch (kèm lý do) hoặc Không áp dụng.

Bảng 5.1: Ánh xạ zod và V2.

| Trường | zod (`views/users/problem-list/model/list-params.ts`) | Quy tắc V2 mục 9 | Khớp |
| :--- | :--- | :--- | :--- |
| `q` | `z.string().transform(s => s.trim().slice(0, 100)).catch("")`; ô nhập `maxLength = 100` | tối đa 100 ký tự, dài hơn thì cắt (V2:154) | Có; cắt im lặng, không toast [SoT: BD:627] |
| `status` | `z.enum(["solved","attempted","todo"]).optional().catch(undefined)` | `SOLVED`, `ATTEMPTED`, `UNSOLVED`, giá trị lạ bỏ qua (V2:155) | Có sau ánh xạ chữ hoa ở entity |
| `difficulty` | `z.string().min(1).max(64).optional().catch(undefined)`, rồi đối chiếu danh mục (BR-SC-02.7) | `levelCode` có thật, không thì "Tất cả" (V2:156) | Có; 64 ký tự là trần phòng thủ của client `[SoT: Suy luận]`, máy chủ là nguồn đúng |
| `topic` | `z.string().uuid().optional().catch(undefined)` | uuid chủ đề có thật, không thì bỏ lọc (V2:157) | Có |
| `sort` | `z.enum(["level","acRate"]).optional().catch(undefined)` | `level`, `acRate`, mặc định `code` (V2:159) | Có |
| `dir` | `z.enum(["asc","desc"]).catch("asc")` | `asc`, `desc`, mặc định `asc` (V2:159) | Có |
| `page` | `z.coerce.number().int().min(1).catch(1)` | số nguyên từ 1, quá số trang thì trang cuối (V2:160) | Có; việc kẹp trang cuối là của máy chủ, client đồng bộ URL (BR-SC-02.6) |
| `pageSize` | `z.union([z.literal(12), z.literal(24), z.literal(48)]).catch(12)` đọc từ `localStorage` | 12/24/48 (V2:161) | Có |
| `tag` | không có control | `tag` (V2:158) | Không áp dụng (hoãn lọc theo thẻ [SoT: BD:231]) |

Lệch bắt buộc nêu: bản dựng giữ trạng thái lọc bằng `useState` và giá trị chữ thường của `SolveState` [SoT: PLV:83-93; `entities/problem/model/types.ts:21`]; hợp đồng dùng enum chữ hoa và `sort = level`. Ánh xạ nằm ở `entities/problem/model`, view không biết enum của API; không cần thêm quyết định.

## 6. i18n và truy cập

**Tiền tố khoá:** `problemList.*` ở `messages/vi.json:133-208` và `messages/en.json` (cùng cấu trúc, `shared/i18n/messages.test.ts` giữ hai bản đồng khoá). Khoá cần **sửa hoặc thêm**: `assignment.meta` đổi thành `"giao {date}"` (bỏ `{solved} / {total} bài`, OQ-1); `toast.randomEmpty` đổi thành "Không có bài nào khớp bộ lọc hiện tại." để khớp BD (hiện đang là câu dài hơn [SoT: `messages/vi.json:193`; BD:630]); `table.btnSolveAria` ("Vào giải bài {title}"); `table.resultsLive` (vùng `aria-live`, "{total} bài khớp bộ lọc").

**Không đặt tên endpoint, mã quyền hay thuật ngữ backend vào nhãn:** chuỗi thông báo lỗi chỉ dùng `common.error`; không có chuỗi nào nhắc "máy chủ judge", "ai-review".

**Truy cập:**

- Chấm trạng thái đã có chữ ẩn thị giác `sr-only` theo `state.*` [SoT: PLV:178-192]; giữ. Khi `solveStateAvailable = false` thì không vẽ cả chấm lẫn chữ.
- Nút "Vào giải" lặp ở mọi dòng: thêm `aria-label` có tên bài để trình đọc màn hình phân biệt [SoT: PLV:261-269]. Liên kết tên bài là lối vào bằng bàn phím chính [SoT: BD:474].
- Vùng `role="status"` (polite, ẩn thị giác) đọc "{total} bài khớp bộ lọc" mỗi lần bảng tải xong sau khi đổi bộ lọc; không đọc ở lần tải đầu `[SoT: Suy luận]` (toast chỉ bật khi Enter, nên đổi tab hay chủ đề không có phản hồi nào cho người dùng đọc màn hình).
- Thanh chủ đề đã có `role="group"` và `aria-pressed` [SoT: PLV:293-331]; giữ. `DataTable` đã phát `aria-sort` [SoT: `shared/ui/data/data-table.tsx:175-180`].
- Thứ tự focus: hai nút ở tiêu đề, thanh chủ đề, ô tìm kiếm, hai menu lọc, tiêu đề cột sắp được, liên kết trong bảng, phân trang, rồi hai khối phụ (cột phải xếp sau bảng cả khi xếp một cột dưới `lg`).

## 7. Điểm kiểm thử

Bảng 7.1 liệt kê điểm kiểm. Vitest dùng React Testing Library với mock `shared/api`; Playwright chạy trên ứng dụng thật khi backend sẵn sàng. `AC` thuộc `04-tdd/screens/users/USR0101_problem_list.md` (ghi `PL-AC-nn`) hoặc module (ghi `F2 AC-nn`, `F4 AC-nn`, `F5 AC-nn`). Các ca test hiện có ở `problem-list-view.test.tsx` đang chạy trên mock gộp và phải viết lại khi tách truy vấn.

Bảng 7.1: Điểm kiểm thử.

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| T-PL-01 | Normal | EVT-1: sáu truy vấn gửi song song; bảng, thanh chủ đề, hai khối phụ hiện đủ | Vitest | F2 AC-01, F4 AC-38 |
| T-PL-02 | Abnormal | EVT-1: F4 mục 11 lỗi, ẩn khối "Bài đang làm dở", bảng vẫn đủ, không toast | Vitest | PL-AC-01 |
| T-PL-03 | Abnormal | EVT-1: F5 mục 5 lỗi, mất nhãn Solution Review, bảng vẫn đủ | Vitest | PL-AC-01, F5 AC-15 |
| T-PL-04 | Abnormal | EVT-1: `solveStateAvailable = false`, ẩn chấm trạng thái và menu trạng thái | Vitest | F2 AC-03 |
| T-PL-05 | Abnormal | EVT-1: F2 mục 2 lỗi lần đầu, vùng lỗi có thử lại, tiêu đề và hai nút vẫn hiện | Vitest | PL-AC-01 |
| T-PL-06 | Abnormal | EVT-1: F2 mục 5 lỗi hoặc `groups = []`, ẩn khối bài tập lớp | Vitest | F2 AC-07 |
| T-PL-07 | Normal | EVT-2: gõ từ khoá, URL đổi sau 400 ms (giả đồng hồ), `page` về 1, hai khối phụ không gọi lại | Vitest | PL-AC-02, F2 AC-02 |
| T-PL-08 | Boundary | EVT-2: gõ quá 100 ký tự bị dừng ở 100, không toast; Enter báo số bài sau khi tải xong | Vitest | F2 AC-02 |
| T-PL-09 | Normal | EVT-3, 4: chọn trạng thái, độ khó gửi `status`, `difficulty` đúng giá trị API; về trang 1 | Vitest | F2 AC-02 |
| T-PL-10 | Normal | EVT-5: bấm chủ đề gửi `topic` là UUID; bấm lại thì bỏ lọc | Vitest | F2 AC-06 |
| T-PL-11 | Normal | EVT-6: "Xóa bộ lọc" chỉ hiện khi rỗng do lọc; xoá sạch tham số khỏi URL | Vitest | PL-AC-02 |
| T-PL-12 | Normal | EVT-7, 8: sắp theo độ khó gửi `sort=level`, đảo chiều khi bấm lại; AC rate gửi `sort=acRate`; `acRate = null` hiện `-` | Vitest | F2 AC-04 |
| T-PL-13 | Normal | EVT-9: đổi trang thêm mục lịch sử; Quay lại trả trang cũ; `page` vượt số trang thì URL đồng bộ về trang cuối | Vitest, Playwright | PL-AC-02 |
| T-PL-14 | Abnormal | URL có `status=abc`, `sort=xyz`, `page=-4`, `difficulty` không có thật: màn dựng bình thường, dùng mặc định, URL được dọn | Vitest | PL-AC-02, F2 AC-02 |
| T-PL-15 | Normal | EVT-10: tên bài và "Vào giải" trỏ `/problems/{uuid}` | Vitest | F2 AC-01 |
| T-PL-16 | Normal | EVT-11: gửi đúng bộ lọc đang áp, điều hướng `/problems/{uuid}` | Vitest | PL-AC-03, F2 AC-05 |
| T-PL-17 | Abnormal | EVT-11: `problemId = null` thì toast, không chuyển màn; nút không bấm được khi `totalItems = 0` | Vitest | PL-AC-03 |
| T-PL-18 | Normal | EVT-12: nút "Bài đã lưu" sang `/saved`, không mang bộ lọc | Vitest | TDD màn `PL-AC-04` |
| T-PL-19 | Normal | EVT-13: thẻ bài dở trỏ `/workspace/{uuid}`; thẻ `problem = null` bị bỏ; trạng thái đang chấm không in `đạt/tổng` | Vitest | F4 AC-38 |
| T-PL-20 | Boundary | EVT-13: toàn bộ thẻ `problem = null` thì ẩn khối, không hiện câu rỗng | Vitest | PL-AC-01 |
| T-PL-21 | Normal | EVT-14: bài tập lớp nhóm theo `classId`, tối đa 3 nhóm, dòng phụ chỉ có ngày giao | Vitest | F2 AC-07 |
| T-PL-22 | Normal | EVT-15: "Xem tất cả" sang `/workspace`, chỉ hiện khi có ít nhất một thẻ | Vitest | TDD màn `PL-AC-04` |
| T-PL-23 | Normal | Luồng đầy đủ: mở `/problems`, lọc độ khó, bấm một bài, Quay lại thấy bộ lọc cũ | Playwright | PL-AC-02 |

## 8. Nợ prototype và câu hỏi mở

**Dòng nợ sẽ gỡ.** Dòng `views/problem-list + entities/problem` của `DEBT` mục 9 (`06-plan/PROTOTYPE_DEBT.md:882`) có "Điều kiện tốt nghiệp: BE có 4 endpoint trên → viết DD tách đúng ranh giới module → thay mock → test". DD xong là bước "viết DD"; thay mock và test **chưa làm** nên dòng này **chưa gỡ**. Dòng `USR0101_problem_list` ở mục 10.2 (`DEBT:962`) còn ghi "Chưa có Prototype Next.js" dù prototype đã dựng; dòng đó lỗi thời, nên sửa riêng (ngoài phạm vi file này).

Bảng 8.1: Lệch giữa prototype và thiết kế, cần làm khi nối thật.

| # | Prototype hiện tại | Thiết kế | Nguồn |
| :-: | :--- | :--- | :--- |
| P1 | Một mock gộp `ProblemListPage` (bốn nguồn) | Sáu truy vấn riêng (Bảng 2.1) | EP api/queries.ts:39-45; types.ts:76-81 |
| P2 | Lọc, sắp xếp, phân trang chạy ở máy khách trên mảng đầy đủ | Chạy ở máy chủ, màn chỉ gửi tham số | PLV:117-146 |
| P3 | Trạng thái lọc ở `useState`, không lên URL | Ghi URL theo BR-SC-02 | PLV:12-13, 83-98 |
| P4 | Tìm kiếm lọc ngay mỗi ký tự, Enter toast theo mảng cục bộ | Trễ 400 ms, toast theo `totalItems` (BR-SC-03) | PLV:162-169 |
| P5 | Sắp xếp mặc định theo độ khó tăng dần | Không `sort` thì máy chủ dùng `code` | PLV:87-92; F2:260 |
| P6 | `QueryBoundary` bọc cả trang bằng một truy vấn | Chỉ bảng có vùng lỗi; khối khác tự ẩn | PLV:69-76 |
| P7 | "Bài đang làm dở" và "Bài tập lớp" hiện câu rỗng trong khối | Ẩn cả khối kèm tiêu đề | PLV:444, 453-457; BD:489, 500 |
| P8 | Dòng phụ bài tập lớp có "x / y bài" | Chỉ ngày giao (OQ-1) | PLV:481-485; F2:407 |
| P9 | Nhóm bài tập lớp khoá theo `className` | Khoá theo `classId` | PLV:459 |
| P10 | `InProgressList` luôn in `{verdict} {đạt}/{tổng}` | Bỏ số đếm khi đang chấm hoặc `null` (BR-SC-05) | IPL:71-76 |
| P11 | `hasSolutionReview` nằm trong dòng bảng | Tập `problemIds` từ F5 mục 5, ghép ở view | types.ts:37 |
| P12 | Lọc chủ đề so khớp theo tên trong `problem.topics` | Gửi `topic` UUID | PLV:123-124 |
| P13 | Độ khó đọc từ `level-store` đồng bộ (dữ liệu mock) | Truy vấn F2 mục 15, tông màu theo `levelCode` seed | `level-store.ts:1-37` |
| P14 | Toast bốc rỗng dùng câu dài khác BD | "Không có bài nào khớp bộ lọc hiện tại." | `messages/vi.json:193`; BD:630 |
| P15 | Nút "Vào giải" thiếu tên truy cập riêng | `aria-label` có tên bài | PLV:261-269 |

Bảng 8.2: Sai khác giữa BD, DD module và DD màn, cần người sở hữu tài liệu đó xử lý (không sửa ở file này).

| # | Điểm lệch | Đề xuất |
| :-: | :--- | :--- |
| D-101-1 | BD liệt kê `ListMyProblemSolveStates` như endpoint màn gọi [SoT: BD:564, 645]; F2 chỉ dùng nó qua cổng nội bộ | Sửa BD Sheet 7.3 và Q2: màn chỉ đọc `solveState`, `solveStateAvailable` |
| D-101-2 | Đã đóng: BD `USR0105` Q6 yêu cầu ẩn bài đã rút ở bài đang làm dở | F4 mục 11 lọc `withdrawn=true` **trước** khi cắt `limit`; màn chỉ hiển thị dòng F4 trả về |
| D-101-3 | BD EVT-9 và Sheet 6 nêu bốn trạng thái ẩn nhưng không nêu menu trạng thái khi `solveStateAvailable = false`; `USR0103` BD ẩn nhóm tab [SoT: `02-bd/screens/users/USR0103_saved_problems.md:413`] | Thêm một câu vào BD Sheet 6 Khu vực C NO 2 cho `USR0101` giống `USR0103` |
| D-101-4 | BD chỉ yêu cầu ghép đạt/tổng cho `WRONG_ANSWER` [SoT: BD:408]; F4-13 tính tỉ lệ cho mọi trạng thái cuối có số đếm | Sửa BD Sheet 5 Khu vực F NO 5 theo BR-SC-05.3 |
| D-101-5 | BD ghi `ListProblemLevels` cấp cả màu badge [SoT: BD:387]; F2 mục 15 không có trường màu [SoT: F2:860] | Giữ: màu badge là bảng hằng theo `levelCode` seed, mức mới màu trung tính (BD Sheet 5 D NO 8 đã nói) |

**Câu hỏi mở mức màn.** Mỗi câu kèm mặc định đề xuất và bậc ponytail (`.claude/rules/ponytail-criteria.md`). Câu đã chốt ghi "Đã chốt" kèm mã quyết định; câu còn lại do chủ dự án quyết.

Bảng 8.3: Câu hỏi mở.

| # | Câu hỏi | Mặc định đề xuất | Bậc |
| :-: | :--- | :--- | :-: |
| OQ-1 | Dòng phụ bài tập lớp có "x / y bài" trong mock, `ClassAssignmentGroupDto` và F2 mục 5 không có nguồn số đó (BD đã nêu "hoặc bỏ khỏi mock, hoặc thêm trường, cần quyết định" [SoT: BD:309]) | Bỏ "x / y bài", chỉ hiện ngày giao. Phương án thay: thêm `solvedCount`, `totalCount` mỗi nhóm vào F2 mục 5, nhưng chỉ số này không ở RD nào | 2 |
| OQ-2 | **Đã chốt 2026-10-19** (owner, BD V0.8 [SoT: BD:64]): giữ bộ chọn 12/24/48, mặc định 12, nhớ ở trình duyệt (`usePersistedPageSize`, khoá `algoprep-problem-list-page-size`), không nhớ theo tài khoản | Đã chốt | 5 |
| OQ-3 | **Đã chốt 2026-10-18** (`DEC-2026-1018-usr-open-questions-phase1-scope`, `DEC-2026-1018-usr-open-questions-phase2-5`): bắt buộc đăng nhập, bài đang làm dở đọc từ `judge.submissions`, `identity` cấp trạng thái giải qua cổng, cache 60 giây | Đã chốt | 1, 3 |
| OQ-4 | **Đã chốt 2026-10-24** (`DEC24`): mọi route học viên dùng `problemId` UUID; liên kết bảng, bài ngẫu nhiên, bài tập lớp, thẻ bài dở đều là UUID | Đã chốt | 4 |
