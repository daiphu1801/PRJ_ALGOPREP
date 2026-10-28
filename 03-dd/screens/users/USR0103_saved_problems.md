# DD màn hình — Bài đã lưu (`USR0103`)

> Tuân khung `03-dd/screens/README.md` mục 4 (chín mục 0 đến 8) và quy tắc cứng mục 5. Màn này chỉ ghi hợp đồng xuyên biên giới và quyết định kỹ thuật không tự suy ra được; request, response, mã lỗi nằm ở file API của module và chỉ được liệt kê theo ID. Màn là biến thể danh sách của `USR0101`: phần trùng (URL, S1, S2, ánh xạ trạng thái) dẫn sang `03-dd/screens/users/USR0101_problem_list.md`, không chép lại. Không có logic phía backend ở đây.

## 0. Thông tin tài liệu

Bảng 0.1: Bìa tài liệu.

| Mục | Giá trị |
| :--- | :--- |
| Mã màn, slug | `USR0103`, `saved_problems` |
| Tên màn | Bài đã lưu |
| Nhóm actor | `users`, A1 (`STUDENT`) |
| Route | `/saved`; query `q`, `status`, `difficulty`, `sort`, `dir`, `page` (không có `topic`) [SoT: BD:539-543] |
| Bounded Context đứng sau | F2 `problem-bank` (bookmark, ghi chú); trạng thái giải do F2 gọi F1 qua cổng trong tiến trình, **client không gọi F1** |
| RD nguồn | `01-rd/screens/users/USR0103_saved_problems.md` |
| BD nguồn | `02-bd/screens/users/USR0103_saved_problems.md` (V0.10) |
| Prototype | `05-coding/frontend/src/views/users/saved-problems/` (dữ liệu mẫu, lọc và phân trang ở máy khách, lưu và bỏ lưu chỉ đổi state) |
| Phiên bản, ngày | V0.1, 2026-10-24 |
| Tiêu chí nghiệm thu | Chỉ dùng `AC` module (mục 7); không có luồng xuyên module nào mà `AC` F2 chưa phủ nên **không** tạo `04-tdd/screens/users/USR0103_saved_problems.md` |

Bảng 0.2: Lịch sử sửa.

| Ver | Mục | Nội dung | Ngày |
| :--- | :--- | :--- | :--- |
| V0.1 | Toàn bộ | Tạo mới từ prototype đã dựng và DD của F2. Chốt giao thức sửa ghi chú, bỏ lưu và Hoàn tác trên ba endpoint bookmark, xử lý dòng "Đã rút xuống", đồng bộ bộ nhớ đệm giữa các màn, danh sách lệch cần chủ dự án quyết | 2026-10-24 |

Bảng 0.3: Viết tắt nguồn trích dẫn dùng trong file này.

| Viết tắt | Đường dẫn |
| :--- | :--- |
| RD | `01-rd/screens/users/USR0103_saved_problems.md` |
| BD | `02-bd/screens/users/USR0103_saved_problems.md` |
| F2 | `03-dd/api/F2_problem-bank.md` |
| V2 | `03-dd/validation/F2_problem-bank.md` |
| DD101 | `03-dd/screens/users/USR0101_problem_list.md` |
| API | `03-dd/api/api.md` |
| SPV | `05-coding/frontend/src/views/users/saved-problems/ui/saved-problems-view.tsx` |
| EP | `05-coding/frontend/src/entities/problem/` |
| DEC24 | `DEC-2026-1024-dd-screens-users-core-contract-changes` (`.nexa/control/decision-registry.md:4675`) |
| DEBT | `06-plan/PROTOTYPE_DEBT.md` |

## 1. Bản đồ slice FSD

Thứ tự import bắt buộc `app → views → widgets → features → entities → shared`; `eslint.config` chặn ngược chiều. Bảng 1.1 là bản đồ đích.

Bảng 1.1: Slice của màn.

| Lớp | Đường dẫn (từ `src/`) | Vai trò | Hiện trạng |
| :--- | :--- | :--- | :--- |
| app | `app/(student)/saved/page.tsx` | Chỉ render `SavedProblemsView` | Có |
| views | `views/users/saved-problems/ui/saved-problems-view.tsx` | Ghép đầu trang, bộ lọc, bảng, chân bảng; **không** chép dữ liệu truy vấn vào `useState` | Có, 387 dòng, `useState(initial)` chép mảng từ truy vấn rồi sửa cục bộ [SoT: SPV:64-67] |
| views | `views/users/saved-problems/model/saved-params.ts` | zod đọc query URL (mục 5), dùng S1 của DD101 | Chưa có |
| views | `views/users/saved-problems/ui/note-cell.tsx` | Ô ghi chú sửa tại chỗ (BR-SC-02); chỉ màn này dùng | Chưa có, đang nằm trong cột `note` của view [SoT: SPV:201-250] |
| entities | `entities/problem/api/queries.ts` | `useSavedProblems(params)`, `useSaveBookmark` (F2 mục 10), `useRemoveBookmark` (F2 mục 11), `useSetBookmarkState` (F2 mục 12, dùng ở `USR0104`) | Mới có `useSavedProblems` mock không tham số [SoT: EP api/queries.ts:56-62] |
| entities | `entities/problem/model/bookmark-cache.ts` | Hàm vá và làm mới bộ nhớ đệm bookmark (BR-SC-05) | Chưa có |
| entities | `entities/problem/model/types.ts` | `SavedProblem` theo F2 mục 9.3: `problemId`, `levelCode`, `levelDisplayName`, `topicNames`, `solveState`, `note`, `savedAt` (ISO), `withdrawn` | Có bản mock: `id`, `difficulty`, `savedAtLabel` (chuỗi dựng sẵn) [SoT: `types.ts:95-107`] |
| shared | `shared/lib/use-url-list-state.ts` (S1), `use-debounced-value.ts` (S2), `use-unsaved-changes-guard.ts` | URL và trễ 400 ms (xem DD101 mục 1); chặn rời màn (BR-SC-06) | S1, S2 chưa có; guard đã có [SoT: `shared/lib/use-unsaved-changes-guard.ts:1-17`] |

Thành phần dùng lại từ `shared/`: `Badge`, `Button`, `Card`, `DataTable`, `EmptyState`, `Pagination`, `TextArea`, `FilterBar`, `FilterMenu`, `QueryError`, `UnsavedChangesDialog` (`shared/ui`), `formatDateVN`, `toast` (có `action`, `shared/lib/toast-store.ts:19-20`) (`shared/lib`), `useMockQuery`, `useMockMutation`, `ApiError` (`shared/api`), `useT` (`shared/i18n`) [SoT: SPV:11-36].

**Lệch có chủ ý so với BD 4.5.** BD đặt `features/problem-filter`, `widgets/saved-problem-table`, `features/bookmark-note-edit`, `features/bookmark-toggle` [SoT: BD:296-299]. DD giữ **một view slice** cho bộ lọc (đã là `FilterBar`, `FilterMenu`), bảng và ô ghi chú vì mỗi thứ chỉ một nơi dùng. `features/bookmark-toggle` chưa tách: chỉ `USR0103` và `USR0104` bỏ lưu có Hoàn tác, nhưng dữ liệu snapshot đến từ hai nguồn khác nhau (dòng bảng và `GetProblemOverview`), phần dùng chung chỉ là ba hook ở `entities/problem` và `bookmark-cache.ts`. Tách thành feature khi `USR0102` cũng cần Hoàn tác.

## 2. API tiêu thụ

Bảng 2.1 liệt kê, theo từng sự kiện BD, endpoint (theo mục của file API), hook và trạng thái nối. Hiện **mọi dòng đều mock** [SoT: EP api/queries.ts:56-62].

Bảng 2.1: Sự kiện, endpoint, hook.

| EVT (BD) | Endpoint theo ID | Hook (`entities/problem/api/`) | Nối |
| :--- | :--- | :--- | :--- |
| EVT-1, EVT-2 đến EVT-7 (gồm EVT-4 tab trạng thái) | F2 mục 9 `ListMySavedProblems` | `useSavedProblems(params)` | mock (`fetchSavedProblems`, không nhận tham số) |
| EVT-1 | F2 mục 15 `ListProblemLevels` | `useProblemLevelList` (dùng chung `USR0101`) | mock qua `level-store` |
| EVT-9 | F2 mục 10 `SaveMyBookmark` với `createIfMissing = false` | `useSaveBookmark` | chưa có, đổi state cục bộ [SoT: SPV:120-130] |
| EVT-11 | F2 mục 11 `RemoveMyBookmark` | `useRemoveBookmark` | chưa có, đổi state cục bộ [SoT: SPV:98-118] |
| EVT-12 | F2 mục 10 `SaveMyBookmark` với `createIfMissing = true` | `useSaveBookmark` | chưa có |
| EVT-8, 10, 13, 14 | không gọi API | state màn, điều hướng `next/link` | không áp dụng |

Màn **không** gọi `ListMyProblemSolveStates` dù BD liệt kê [SoT: BD:511]: F2 mục 9 trả sẵn `solveState` của từng dòng và `meta.solvedCount` (xem D-103-1 ở mục 8). Kênh realtime: không có.

Bảng 2.2 ánh xạ điều khiển sang tham số (kiểu và ràng buộc ở F2 mục 9.2 và V2 mục 9).

Bảng 2.2: Điều khiển, tham số URL, tham số F2.

| Điều khiển | Tham số URL | Tham số gửi F2 mục 9 | Ghi chú |
| :--- | :--- | :--- | :--- |
| Ô tìm kiếm | `q` | `q` | |
| Menu trạng thái | `status` = `solved`, `attempted`, `todo` | `status` = `SOLVED`, `ATTEMPTED`, `UNSOLVED` | ánh xạ ở `entities/problem/model/list-params.ts` (DD101) |
| Menu độ khó | `difficulty` (= `levelCode`) | `difficulty` | |
| Tiêu đề cột "Ngày lưu" | `sort` = `savedAt`; `dir` | `sort`, `dir` | cột sắp xếp duy nhất; mặc định `desc` (mới lưu trước) [SoT: F2:594-595; BD:429] |
| Nút số trang | `page` | `page` | `pageSize` cố định 20 ở máy chủ, **không gửi** [SoT: F2:596] |
| `topic` trên URL | không đọc | không gửi | bị bỏ qua như tham số lạ [SoT: F2:598; BD:588] |

## 3. Trạng thái màn

Bảng 3.1 liệt kê trạng thái theo vùng. Khoá i18n có tiền tố `savedProblems.` Mã lỗi lấy từ F2 mục 60 (`PRB-nnn`) [SoT: F2:2847-2856]; frontend không đọc `message` [SoT: API:22].

Bảng 3.1: Trạng thái theo vùng.

| Vùng | Trạng thái | Điều kiện | Hiển thị | Khoá i18n |
| :--- | :--- | :--- | :--- | :--- |
| Đầu trang | Chưa có số | F2 mục 9 chưa xong | tiêu đề hiện, dòng mô tả chưa dựng [SoT: BD:404] | `title` |
| Đầu trang | Mất vế "đã giải" | `meta.solvedCount = null` | chỉ còn "{total} bài đã lưu" [SoT: BD:404; F2:604] | `subtitleTotalOnly` (mới) |
| Bảng | Đang tải | lần đầu | 8 dòng chờ [SoT: BD:421] | `common.loading` |
| Bảng | Lỗi lần đầu | F2 mục 9 lỗi, chưa có dữ liệu | vùng lỗi thay bảng, nút thử lại, giữ bộ lọc [SoT: BD:421, 591] | `common.error`, `common.retry` |
| Bảng | Lỗi lần sau | đổi bộ lọc hoặc trang mà lỗi | giữ dòng cũ, toast lỗi [SoT: BD:553, 557] | `common.error` |
| Bảng | Rỗng thật | `meta.savedTotal = 0` | một câu hướng dẫn lưu bài từ trang chi tiết, không liên kết [SoT: BD:359; `messages/vi.json:243`] | `emptyAll` |
| Bảng | Rỗng do lọc | `savedTotal > 0`, `filteredTotal = 0` | "Không có bài nào khớp bộ lọc" kèm "Xoá bộ lọc" và câu phụ [SoT: BD:340, 358-359] | `table.emptyFiltered`, `table.clearFilters` (mới), `table.emptyFilteredNote` (mới) |
| Cột trạng thái, menu trạng thái | Ẩn | mọi dòng `solveState = null` hoặc `solvedCount = null` | ẩn ô trạng thái và menu trạng thái; bảng và ghi chú vẫn dùng [SoT: BD:413, 427, 590] | không áp dụng |
| Ô ghi chú | Đang lưu | gọi F2 mục 10 chưa xong | ô và nút Lưu khoá, `aria-busy` | `table.noteSave` |
| Ô ghi chú | Quá dài | `PRB-001` với `details.fields[0].field = note` | giữ ô mở với nguyên văn, viền đỏ, toast "Ghi chú tối đa 1000 ký tự." [SoT: BD:560, 584; F2:657] | `toast.noteTooLong` (mới) |
| Ô ghi chú | Bookmark đã mất | `PRB-014` | toast, đóng ô, tải lại bảng; **không** tạo lại bookmark [SoT: BD:585; F2:659] | `toast.unsavedElsewhere` (mới) |
| Ô ghi chú | Lỗi khác | mạng hoặc `5xx` | giữ ô mở với nguyên văn, toast lỗi, cho lưu lại [SoT: BD:560] | `common.error` |
| Dòng | Bỏ lưu lỗi | F2 mục 11 lỗi | dòng trở lại đúng vị trí cũ, số đếm trở lại, toast lỗi [SoT: BD:562] | `common.error` |
| Hoàn tác | Lỗi | F2 mục 10 (tạo lại) lỗi | toast lỗi có lại nút Hoàn tác `[SoT: Suy luận]` (BD nói "giữ thông báo" [SoT: BD:563], toast chung tự tắt) | `common.error`, `undo.button` |
| Hoàn tác | Bài vừa bị rút | `PRB-010` khi tạo lại | toast "Bài này đã bị rút khỏi ngân hàng nên không thể lưu lại." | `toast.undoWithdrawn` (mới) |
| Toàn màn | Chưa đăng nhập | `401` sau khi làm mới token thất bại | chuyển về `auth` (`shared/api/client.ts`) | không áp dụng |
| Toàn màn | Quyền bị từ chối | tài khoản không phải `STUDENT` | chặn ở nhóm route `(student)`; không dựng UI `403` riêng `[SoT: Suy luận]` | không áp dụng |

## 4. Quy tắc đặc thù màn

Sáu khối dưới là những việc đi qua nhiều sự kiện hoặc không tự suy ra được từ BD và file API. Đánh số cục bộ file này (`BR-SC-nn`).

### BR-SC-01 Khởi tạo và suy giảm khi mất trạng thái giải

- **Kích hoạt:** EVT-1.
- **Các bước:** (1) đọc query URL (mục 5), giá trị lạ dùng mặc định, `topic` bỏ qua [SoT: BD:588; V2:150]; (2) gửi song song F2 mục 9 theo tham số và F2 mục 15; (3) `staleTime = 0` cho `useSavedProblems` để quay lại màn luôn thấy dữ liệu mới (BR-SC-05).
- **Suy giảm:** F1 chết thì F2 vẫn trả `200` với `solveState = null` ở mọi dòng và `meta.solvedCount = null`; màn ẩn ô trạng thái, menu trạng thái và vế "đã giải" của dòng mô tả, **không** toast [SoT: F2:604; BD:590]. Khác `USR0101`, F2 mục 9 không có cờ `solveStateAvailable`; tín hiệu là `solvedCount = null` `[SoT: Suy luận]`. Nhãn nút vào dòng luôn là "Mở bài", không phụ thuộc trạng thái giải [SoT: BD:357, V0.10]; câu BD Sheet 9 NO 11 nói lùi về nhãn "Giải" đã lỗi thời [SoT: BD:590].
- **Đầu trang:** `{total}` = `meta.savedTotal`, `{solved}` = `meta.solvedCount`, hai số của cả tập bookmark, không đổi khi lọc [SoT: BD:404; F2:604].

### BR-SC-02 Sửa ghi chú tại chỗ

- **Kích hoạt:** EVT-8, EVT-9, EVT-10.
- **Trạng thái:** `noteEdit = { problemId, draft } | null` (một ô mở tại một thời điểm) và `savingProblemId`.
- **Mở ô (EVT-8):** nếu đang mở ô ở dòng khác mà `draft.trim()` khác ghi chú đã lưu thì chạy lưu cho dòng đó trước; lưu lỗi thì **ở lại** ô cũ, không mở ô mới; ngược lại mở ô mới, đưa con trỏ về cuối nội dung [SoT: BD:428, 559]. Bản dựng thay `noteEdit` ngay nên mất phần gõ dở [SoT: SPV:241-248].
- **Lưu (EVT-9):** bấm nút Lưu hoặc `Ctrl/Cmd + Enter`. **Không lưu khi rời tiêu điểm** (BD nói lưu khi rời tiêu điểm [SoT: BD:560]): nút Hủy nằm ngoài ô, bấm Hủy làm ô mất tiêu điểm trước khi sự kiện bấm tới, nên lưu-khi-rời-tiêu-điểm sẽ lưu đúng bản người dùng định bỏ (D-103-2, OQ-1).
  1. `draft.trim()` bằng ghi chú đã lưu thì đóng ô, không gọi máy chủ [SoT: BD:560].
  2. Gọi F2 mục 10 với `note = draft.trim()` (rỗng thì gửi `null`) và `createIfMissing = false`; khoá dòng.
  3. Thành công (`200`): ghi `note` từ phản hồi vào đúng dòng trong bộ nhớ đệm, không tải lại bảng, `savedAt` không đổi [SoT: F2:652; BD:560]; toast `toast.noteSaved`; đóng ô và trả focus về nút ghi chú của dòng.
  4. `PRB-001`: giữ ô mở nguyên văn, viền đỏ, toast `toast.noteTooLong`. `PRB-014`: toast `toast.unsavedElsewhere`, đóng ô, tải lại bảng [SoT: BD:585]. Lỗi khác: giữ ô mở, toast, cho lưu lại. Không bao giờ bỏ nội dung đã gõ trong ba nhánh lỗi.
- **Hủy (EVT-10):** `Escape` hoặc nút Hủy: bỏ bản nháp, không gọi, không hỏi lại [SoT: BD:561].
- **Dữ liệu là dữ liệu:** ghi chú hiển thị bằng nút văn bản thuần và `TextArea`, **không** qua `MarkdownPreview` hay `dangerouslySetInnerHTML` [SoT: BD:584; V2:141]; đổi bộ lọc hoặc trang thì đóng ô đang sửa sau khi chạy lưu như EVT-8 [SoT: SPV:132-137].

### BR-SC-03 Bỏ lưu một bài và Hoàn tác

- **Kích hoạt:** EVT-11, EVT-12.
- **Snapshot:** tại lúc bấm, giữ `{ problemId, title, note, withdrawn, solveState, indexOnPage }` của dòng.
- **Bỏ lưu (EVT-11):**
  1. Gỡ dòng khỏi bộ nhớ đệm trang hiện tại; giảm `meta.savedTotal` và `meta.filteredTotal`; giảm `meta.solvedCount` khi nó khác `null` và `solveState = SOLVED`; đóng ô ghi chú nếu đang mở ở dòng đó [SoT: BD:404, 562; SPV:105].
  2. Gọi F2 mục 11 (idempotent, `204` cả khi bookmark đã mất [SoT: F2:689, 696]); không chặn vì bài đã rút [SoT: F2:704].
  3. Thành công: toast `undo.message` kèm nút Hoàn tác (BR-SC-04 nêu ngoại lệ); trang hiện tại còn 0 dòng mà `page > 1` thì đặt `page - 1` bằng `replace` [SoT: BD:562]; làm mới bộ nhớ đệm (BR-SC-05).
  4. Lỗi: trả dòng về đúng vị trí và trả số đếm, toast lỗi [SoT: BD:562].
- **Hoàn tác (EVT-12):** gọi F2 mục 10 với `note = snapshot.note` (rỗng thì `null`) và `createIfMissing = true`; thành công thì làm mới bảng, bài về lại kèm ghi chú cũ, `savedAt` là ngày tạo lại [SoT: F2:652; BD:563, 586]. Mỗi lần bỏ lưu một toast với closure riêng nên bỏ bài thứ hai không làm mất Hoàn tác bài thứ nhất [SoT: BD:71, 586; SPV:106-117].
- **Cửa sổ hoàn tác** là thời lượng của toast thông tin chung, 4 giây [SoT: `shared/lib/toast-store.ts:24-26`]; BD Sheet 6 nêu 8 giây [SoT: BD:449], xem D-103-3.

### BR-SC-04 Dòng "Đã rút xuống"

- **Kích hoạt:** EVT-1, EVT-9, EVT-11, EVT-13.
- **Quy tắc:** dòng có `withdrawn = true` (cờ do F2 tính, không trả hai cột thô [SoT: F2:603]) vẫn nằm trong bảng và trong `savedTotal`; tên bài là chữ không phải liên kết; badge "Đã rút xuống" cạnh badge độ khó; nút "Mở bài" mang `aria-disabled` kèm chú giải `withdrawnHint` và **không** điều hướng; ghi chú và Bỏ lưu dùng bình thường [SoT: BD:351, 425, 431, 582]. Ghi chú trên dòng rút xuống vẫn lưu được vì F2 mục 10 chỉ kiểm bài khi **tạo mới** [SoT: F2:658, 664].
- **Ngoại lệ Hoàn tác (D-103-4):** F2 mục 10 từ chối tạo lại bookmark của bài đã rút (`PRB-010`) [SoT: F2:664], trong khi BD không nói Hoàn tác trên dòng rút xuống. Vì vậy bỏ lưu một dòng `withdrawn = true` hiện toast "Đã bỏ lưu {title}." **không có** nút Hoàn tác, để không bày một hành động chắc chắn lỗi `[SoT: Suy luận]`. Bài rút **sau** lúc bỏ lưu thì lỗi `PRB-010` hiện `toast.undoWithdrawn`.

### BR-SC-05 Bộ nhớ đệm bookmark dùng chung giữa các màn

- **Kích hoạt:** mọi mutation bookmark ở `USR0103`, `USR0104`, `USR0102` (EVT-15 của `USR0102` [SoT: `03-dd/screens/users/USR0102_problem_detail.md:92`]).
- **Quy tắc:** `bookmark-cache.ts` làm mới ba nhóm khoá của entity: danh sách bài đã lưu (`["problem","saved"]` theo tiền tố, mọi tham số), chi tiết tổng quan và chi tiết workspace của đúng `problemId` (`bookmarked`). Mutation ở màn khác chỉ cần gọi hàm này; không màn nào tự ghi khoá của màn khác.
- **Lý do:** lưu ở `/problems/{id}` rồi mở `/saved` phải thấy bài ngay; bỏ lưu ở `/saved` rồi mở lại bài phải thấy nút "Lưu bài". Danh sách này `staleTime = 0` nên chỉ cần làm mới khi cùng phiên đang mở màn.

### BR-SC-06 Rời màn khi ghi chú sửa dở

- **Kích hoạt:** EVT-14 (nợ prototype theo BD Q8, thiết kế đích khi nối API) [SoT: BD:565, 611].
- **Quy tắc:** `dirty = noteEdit khác null và draft.trim() khác ghi chú đã lưu`. Dùng `useUnsavedChangesGuard(dirty)` có sẵn: chặn liên kết trong ứng dụng, nút Quay lại, đóng hoặc tải lại thẻ (hộp mặc định của trình duyệt, không tuỳ biến câu chữ) [SoT: `shared/lib/use-unsaved-changes-guard.ts:1-12`]. Dùng `UnsavedChangesDialog` ba nút: "Lưu và rời đi" (chạy BR-SC-02 rồi đi tiếp, lỗi thì ở lại), "Rời đi" (bỏ bản gõ), "Ở lại" (đặt lại tiêu điểm vào ô).
- **Lệch BD:** BD vẽ hộp hai nút [SoT: BD:459, 587]; hộp dùng chung có thêm "Lưu và rời đi", cùng chọn của `USR0503` [SoT: DEBT:2087 mục (4)] (D-103-5).
- **Đích DD:** nối guard khi triển khai API; nợ prototype vẫn giữ cho buổi demo hiện tại theo BD Q8. Chuyển trang bằng nút số trang và đổi bộ lọc không qua guard vì không rời màn: chạy "lưu dòng đang mở" như EVT-8 trước [SoT: BD:557].

## 5. Kiểm tra phía client

Màn có một trường nhập (ghi chú) và chuẩn hoá query URL. Bảng 5.1 ánh xạ sang zod (`zod 3.24.1`, `package.json:35`) và quy tắc ở V2.

Bảng 5.1: Ánh xạ zod và V2.

| Trường | zod (`views/users/saved-problems/model/saved-params.ts`, `entities/problem/model/note-schema.ts`) | Quy tắc V2 | Khớp |
| :--- | :--- | :--- | :--- |
| `note` | `z.string().transform(s => s.trim()).pipe(z.string().max(1000))`; `TextArea maxLength = 1000`; gửi `null` khi rỗng sau cắt | tối đa 1.000 ký tự sau cắt; rỗng lưu `NULL`; văn bản thuần (V2:141) | Lệch an toàn: `.length` của JavaScript đếm đơn vị UTF-16 nên ký tự ngoài mặt phẳng cơ bản (ví dụ biểu tượng cảm xúc) tính hai; client chặt hơn hoặc bằng máy chủ, không bao giờ lỏng hơn. Máy chủ là nguồn đúng |
| `q` | `z.string().transform(s => s.trim().slice(0, 100)).catch("")` | tối đa 100, cắt (V2:154) | Có; cắt im lặng [SoT: BD:583] |
| `status` | `z.enum(["solved","attempted","todo"]).optional().catch(undefined)` | V2:155 | Có sau ánh xạ chữ hoa |
| `difficulty` | như `USR0101` (DD101 mục 5) | V2:156 | Có |
| `sort` | `z.literal("savedAt").optional().catch(undefined)` | bookmark chỉ `savedAt` (V2:159) | Có |
| `dir` | `z.enum(["asc","desc"]).catch("desc")` | mặc định `savedAt` giảm (V2:159) | Có |
| `page` | `z.coerce.number().int().min(1).catch(1)` | V2:160 | Có |
| `problemId`, `createIfMissing`, `user_id` | không phải trường nhập: `problemId` từ dòng, `createIfMissing` do mã đặt (`false` sửa ghi chú, `true` Hoàn tác), không gửi `user_id` | V2:142, 146 | Không áp dụng |

Lệch bắt buộc nêu: bản dựng tính `solved` bằng cách đếm mảng cục bộ [SoT: SPV:94-96] và giữ `savedAtLabel` là chuỗi dựng sẵn [SoT: `types.ts:104`]; hợp đồng trả `meta.solvedCount` và `savedAt` ISO, màn định dạng bằng `formatDateVN`.

## 6. i18n và truy cập

**Tiền tố khoá:** `savedProblems.*` ở `messages/vi.json:209-260` và `messages/en.json`. Khoá cần **thêm**: `subtitleTotalOnly` ("{total} bài đã lưu"), `table.clearFilters`, `table.emptyFilteredNote` ("Bỏ một bộ lọc, hoặc lưu thêm bài từ ngân hàng bài toán." [SoT: BD:359]), `table.noteShortcutHint` ("Ctrl+Enter để lưu, Esc để hủy"), `toast.noteTooLong`, `toast.unsavedElsewhere`, `toast.undoWithdrawn`, `leave.*` (hộp rời màn, nếu OQ-2 chốt dựng). Câu `emptyAll` giữ một chuỗi như bản dựng (BD chia hai nhãn, cùng nội dung [SoT: BD:280]).

**Không đặt tên endpoint, mã quyền hay thuật ngữ backend vào nhãn:** chuỗi lỗi chỉ nêu việc, không nêu `PRB-nnn`.

**Truy cập:**

- Nút ghi chú dùng `aria-label` "Sửa ghi chú: {tên bài}"; `title` giữ làm chú giải nhìn [SoT: SPV:241-248].
- Mở ô ghi chú thì đưa focus vào `TextArea`, `aria-keyshortcuts="Control+Enter Meta+Enter"`, bộ đếm `{n}/1000` nằm trong `aria-live="off"` để không đọc từng ký tự; đóng ô thì trả focus về nút ghi chú của dòng.
- Sau Bỏ lưu: focus sang nút ghi chú của dòng kế tiếp (hoặc dòng trước nếu là dòng cuối); danh sách rỗng thì focus về `<h1>`. Toast đã báo việc bỏ lưu cho trình đọc màn hình (`role="status"` của `Toaster`) nên không thêm vùng live riêng.
- Dòng "Đã rút xuống": nút "Mở bài" `aria-disabled="true"` kèm `aria-describedby` trỏ chú giải, không chỉ làm mờ.
- Thứ tự focus: bộ lọc, tiêu đề cột "Ngày lưu", từng dòng (liên kết tên bài, ghi chú, Mở bài, Bỏ lưu), phân trang.
- Nút Bỏ lưu hiện khi rê chuột hoặc có focus bàn phím [SoT: BD:430]; bản dựng luôn hiện, giữ như vậy.

## 7. Điểm kiểm thử

Bảng 7.1 liệt kê điểm kiểm. Vitest dùng React Testing Library với mock `shared/api`; Playwright chạy trên ứng dụng thật. Tiêu chí nghiệm thu dùng `AC` của F2 vì mọi ràng buộc nghiệp vụ nằm ở module này; ca test hiện có ở `saved-problems-view.test.tsx` phải viết lại khi bỏ `useState(initial)`.

Bảng 7.1: Điểm kiểm thử.

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| T-SP-01 | Normal | EVT-1: tải trang đầu, 20 dòng, dòng mô tả có `savedTotal` và `solvedCount` | Vitest | F2 AC-16 |
| T-SP-02 | Abnormal | EVT-1: `solvedCount = null`, `solveState = null`: ẩn ô trạng thái, menu trạng thái, vế "đã giải", không toast | Vitest | F2 AC-16 |
| T-SP-03 | Abnormal | EVT-1: F2 mục 9 lỗi lần đầu, vùng lỗi kèm thử lại | Vitest | F2 AC-16 |
| T-SP-04 | Normal | EVT-2: gõ từ khoá, URL `q` đổi sau 400 ms, về trang 1; Enter báo số bài sau khi tải; không có `topic` nào được gửi | Vitest | F2 AC-16 |
| T-SP-05 | Normal | EVT-3, 4: chọn độ khó và trạng thái gửi đúng giá trị API; ghi chú của mọi dòng vẫn hiện | Vitest | F2 AC-16 |
| T-SP-06 | Normal | EVT-5: bấm "Ngày lưu" đảo `dir`; mặc định `desc`; giữ trang | Vitest | F2 AC-16 |
| T-SP-07 | Boundary | EVT-6: 21 bookmark, trang 1 có 20 dòng, `page` quá lớn thì URL đồng bộ về trang cuối | Vitest | F2 AC-16 |
| T-SP-08 | Normal | EVT-7: "Xoá bộ lọc" chỉ hiện khi rỗng do lọc; không hiện khi chưa lưu bài nào | Vitest | F2 AC-16 |
| T-SP-09 | Normal | EVT-8: mở ô thứ hai khi ô thứ nhất đã đổi thì lưu ô thứ nhất trước; lưu lỗi thì không mở ô mới | Vitest | F2 AC-12 |
| T-SP-10 | Normal | EVT-9: Lưu gửi `createIfMissing = false`, chỉ vá đúng dòng, `savedAt` không đổi; Ctrl+Enter cũng lưu; blur không lưu | Vitest | F2 AC-12, AC-13 |
| T-SP-11 | Boundary | EVT-9: ghi chú đúng 1000 ký tự gửi được; 1001 bị chặn ở ô; toàn khoảng trắng gửi `null` | Vitest | F2 AC-13 |
| T-SP-12 | Abnormal | EVT-9: `PRB-001` giữ ô mở nguyên văn, viền đỏ; `PRB-014` toast, đóng ô, tải lại, không tạo lại bookmark | Vitest | F2 AC-12 |
| T-SP-13 | Normal | EVT-10: Esc và Hủy bỏ bản nháp, không gọi API | Vitest | F2 AC-12 |
| T-SP-14 | Normal | EVT-11: dòng biến mất ngay, `savedTotal` giảm, `solvedCount` giảm khi dòng đã giải, toast có nút Hoàn tác | Vitest | F2 AC-14 |
| T-SP-15 | Abnormal | EVT-11: F2 mục 11 lỗi, dòng về đúng vị trí, số đếm trở lại, toast lỗi | Vitest | F2 AC-14 |
| T-SP-16 | Boundary | EVT-11: bỏ dòng cuối của trang 2 thì lùi về trang 1; bỏ hai bài liên tiếp có hai toast, mỗi toast hoàn tác đúng bài của nó | Vitest | F2 AC-14 |
| T-SP-17 | Normal | EVT-12: Hoàn tác gửi `createIfMissing = true` kèm ghi chú cũ; bài về lại, `savedAt` là ngày mới | Vitest | F2 AC-12 |
| T-SP-18 | Abnormal | EVT-12: `PRB-010` khi hoàn tác thì toast `toast.undoWithdrawn` | Vitest | F2 AC-14 |
| T-SP-19 | Normal | EVT-13: tên bài và "Mở bài" trỏ `/problems/{uuid}`; bấm vào ô ghi chú là EVT-8, không điều hướng | Vitest | F2 AC-16 |
| T-SP-20 | Boundary | Dòng `withdrawn = true`: tên không là liên kết, "Mở bài" không điều hướng, vẫn sửa ghi chú, Bỏ lưu không có nút Hoàn tác | Vitest | F2 AC-14, AC-16 |
| T-SP-21 | Security | Ghi chú chứa `<script>` và Markdown hiển thị nguyên văn như chữ, không dựng đánh dấu | Vitest | F2 AC-13 |
| T-SP-22 | Normal | EVT-14 (nếu dựng): ô sửa dở rồi bấm liên kết khác hiện hộp ba nút; "Ở lại" trả focus vào ô | Vitest | TDD màn `SP-AC-05` |
| T-SP-23 | Normal | Luồng đầy đủ: lưu bài ở `/problems/{id}`, mở `/saved` thấy bài, sửa ghi chú, bỏ lưu, Hoàn tác | Playwright | F2 AC-12, AC-15 |

## 8. Nợ prototype và câu hỏi mở

**Dòng nợ sẽ gỡ.** Dòng `views/saved-problems + entities/problem` của `DEBT` mục 9 (`06-plan/PROTOTYPE_DEBT.md:884`) có "Điều kiện tốt nghiệp: BE có bảng `problem.bookmarks` + endpoint → viết DD → thay mock → test". DD xong là bước "viết DD"; thay mock và test **chưa làm** nên dòng này **chưa gỡ**. Dòng `USR0103_saved_problems` ở mục 10.2 (`DEBT:964`) cũng lỗi thời (ghi chưa có prototype); sửa riêng.

Bảng 8.1: Lệch giữa prototype và thiết kế, cần làm khi nối thật.

| # | Prototype hiện tại | Thiết kế | Nguồn |
| :-: | :--- | :--- | :--- |
| P1 | `useState(initial)` chép mảng truy vấn rồi sửa cục bộ | Máy chủ là nguồn; vá bộ nhớ đệm | SPV:64-67 |
| P2 | Lọc và phân trang ở máy khách trên toàn mảng | Chạy ở máy chủ, màn gửi tham số | SPV:74-92 |
| P3 | Trạng thái lọc không lên URL; không có ô sắp xếp "Ngày lưu" | Ghi URL, tiêu đề cột sắp được (EVT-5) | SPV:68-72, 251-260 |
| P4 | `SavedProblem` mang `id`, `difficulty`, `savedAtLabel` dựng sẵn | `problemId`, `levelCode`, `savedAt` ISO | `types.ts:95-107` |
| P5 | Số `solved` đếm từ mảng cục bộ; không có suy giảm khi mất trạng thái giải | `meta.solvedCount`; `null` thì bỏ vế "đã giải" (BR-SC-01) | SPV:94-96, 308 |
| P6 | Lưu ghi chú chỉ đổi state, không có `PRB-001`, `PRB-014` | Giao thức BR-SC-02 | SPV:120-130 |
| P7 | Mở ô ghi chú mới thay `noteEdit`, mất bản gõ dở của ô trước | Lưu ô cũ trước khi mở ô mới | SPV:241-248 |
| P8 | Hoàn tác khôi phục từ closure cục bộ; ngày lưu giữ nguyên | Hoàn tác gọi F2 mục 10, ngày lưu mới | SPV:98-118 |
| P9 | Dòng "Đã rút xuống" vẫn hiện Hoàn tác | Không có Hoàn tác trên dòng rút xuống (BR-SC-04) | SPV:98-118 |
| P10 | Ngày lưu là nhãn dựng sẵn | `formatDateVN` cho `dd/MM/yyyy` (D-103-6) | `types.ts:104`; `shared/lib/format-date.ts:38-48` |
| P11 | Không có "Xoá bộ lọc" ở trạng thái rỗng do lọc, không có câu phụ | Có nút và câu phụ (BD EVT-7) | SPV:348-359; BD:340, 358-359 |
| P12 | Hộp xác nhận rời màn chưa dựng | Theo BR-SC-06 nếu OQ-2 chốt dựng | SPV (không có); BD:565 |

Bảng 8.2: Sai khác giữa BD, DD module và DD màn, cần người sở hữu tài liệu đó xử lý (không sửa ở file này).

| # | Điểm lệch | Đề xuất |
| :-: | :--- | :--- |
| D-103-1 | BD liệt kê `ListMyProblemSolveStates` là endpoint của màn [SoT: BD:511]; F2 chỉ dùng qua cổng nội bộ và `ListMySavedProblems` không có `solveStateAvailable` | Sửa BD Sheet 7.3 NO 4; nếu muốn đồng bộ với `USR0101` thì thêm `solveStateAvailable` vào F2 mục 9.3 (hiện suy từ `solvedCount = null`) |
| D-103-2 | BD EVT-9 lưu khi rời tiêu điểm [SoT: BD:560]; bản dựng và DD dùng nút Lưu, Hủy | Sửa BD EVT-9: lưu bằng nút hoặc Ctrl/Cmd+Enter (OQ-1) |
| D-103-3 | BD Sheet 6 Khu vực E nói thông báo tự tắt sau 8 giây [SoT: BD:449]; toast thông tin chung tồn tại 4 giây [SoT: `toast-store.ts:24-26`] | Sửa BD theo V0.7 (cửa sổ hoàn tác là thời lượng toast), hoặc cho `toast.info` nhận `durationMs` nếu cần 8 giây |
| D-103-4 | BD không nói Hoàn tác trên dòng "Đã rút xuống"; F2 mục 10 từ chối tạo lại (`PRB-010`) | Ghi vào BD Sheet 8 EVT-11: dòng rút xuống không có nút Hoàn tác |
| D-103-5 | BD vẽ hộp rời màn hai nút [SoT: BD:459, 587]; `UnsavedChangesDialog` ba nút | Sửa BD theo `USR0503` (đã chọn hộp ba nút) |
| D-103-6 | BD nói ngày lưu định dạng `dd/MM` [SoT: BD:355]; `formatDateVN` chỉ có `dd/MM/yyyy` và bài lưu có thể qua năm | Giữ `dd/MM/yyyy`; sửa BD Sheet 5 NO 9 |

**Câu hỏi mở mức màn.** Mỗi câu kèm mặc định đề xuất và bậc ponytail (`.claude/rules/ponytail-criteria.md`). Câu đã chốt ghi "Đã chốt" kèm mã quyết định; câu còn lại do chủ dự án quyết.

Bảng 8.3: Câu hỏi mở.

| # | Câu hỏi | Mặc định đề xuất | Bậc |
| :-: | :--- | :--- | :-: |
| OQ-1 | **Đã chốt** `DEC-2026-1024-dd-saved-note-explicit-save`: ghi chú dùng Lưu/Hủy, không tự lưu khi rời ô | Ctrl/Cmd+Enter lưu, Esc hủy; đồng bộ BD EVT-9 ở lượt BD | 6 |
| OQ-2 | EVT-14 (hộp rời màn lúc sửa dở) owner đã giữ là nợ của prototype demo ngày 2026-10-19 (BD Q8 [SoT: BD:611]) | Đã đóng ở DD: khi nối API, dùng `useUnsavedChangesGuard` và `UnsavedChangesDialog` có sẵn; BR-SC-06 và T-SP-22 là thiết kế đích, không sửa prototype trong lượt DD | 4 |
| OQ-3 | **Đã chốt 2026-10-18** (`DEC-2026-1018-usr-open-questions-phase2-5`): bỏ lưu có Hoàn tác không popup, ghi chú sửa tại chỗ, tối đa 1000 ký tự văn bản thuần, 20 dòng một trang | Đã chốt | 3, 5 |
| OQ-4 | **Đã chốt 2026-10-18** (`DEC-2026-1018-usr-open-questions-last-four`): bài đã rút vẫn hiện kèm nhãn "Đã rút xuống", tính vào tổng, ghi chú và Bỏ lưu dùng được | Đã chốt | 6 |
| OQ-5 | **Đã chốt 2026-10-21** (owner, BD V0.10 [SoT: BD:74]): điểm lưu bài ở `problem_overview` và `problem_detail`; nút vào dòng là "Mở bài"; chỉ tên bài và nút mở được bấm | Đã chốt | 1 |
| OQ-6 | **Đã chốt 2026-10-24** (`DEC24`): `problemId` UUID ở mọi route; liên kết dòng là `/problems/{uuid}` | Đã chốt | 4 |
