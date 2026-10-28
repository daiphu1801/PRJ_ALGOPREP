# DD màn hình — Bài đã nộp (`USR0202`)

> Tuân khung `03-dd/screens/README.md` mục 4 (chín mục 0 đến 8) và quy tắc cứng mục 5. Request, response, mã lỗi nằm ở file API của module và chỉ được liệt kê theo ID. Không có logic backend ở đây. Màn chỉ đọc: một danh sách phân trang máy chủ, hai nút điều hướng mỗi dòng, không form ghi.

## 0. Thông tin tài liệu

Bảng 0.1 là bìa tài liệu.

Bảng 0.1: Bìa.

| Mục | Giá trị |
| :--- | :--- |
| Mã màn, slug | `USR0202`, `my_submissions` |
| Tên màn | Bài đã nộp (RD đặt tiêu đề "Lịch sử nộp bài" [SoT: RD:1]) |
| Nhóm actor | `users` (A1 `STUDENT`, chỉ dữ liệu của chính mình) |
| Route | `/submissions` theo bản dựng và thanh điều hướng [SoT: `05-coding/frontend/src/app/(student)/submissions/page.tsx:1`; `05-coding/frontend/src/widgets/app-shell/model/student-nav.ts:37`]. BD ghi `/my-submissions` [SoT: BD:97], lệch, xem Bảng 8.2 dòng D-3 |
| Bounded Context đứng sau | F4 `judge-orchestration`; tên và nhãn độ khó của bài do F4 ghép từ F2 ở máy chủ (F4:620), màn không gọi F2; liên kết sang F5 `ai-review` bằng điều hướng |
| RD nguồn | `01-rd/screens/users/USR0202_my_submissions.md` |
| BD nguồn | `02-bd/screens/users/USR0202_my_submissions.md` (V1.0) |
| Prototype | `05-coding/frontend/src/views/users/my-submissions/` (mock, bộ lọc ở `useState`, chưa đọc URL) |
| Phiên bản, ngày | V0.1, 2026-10-24 |
| Tiêu chí nghiệm thu | `04-tdd/screens/users/USR0202_my_submissions.md` phủ trạng thái, lọc và điều hướng của màn; hợp đồng máy chủ dùng `F4 AC-37`, `F4 AC-68`, `F4 AC-71` và `F5 AC-05`. Mọi điểm mục 7 đã có mã AC truy vết. |

Bảng 0.2 ghi lịch sử sửa.

Bảng 0.2: Lịch sử sửa.

| Ver | Mục | Nội dung | Ngày |
| :--- | :--- | :--- | :--- |
| V0.1 | Toàn bộ | Tạo mới từ prototype và DD F4. Chốt: URL là nguồn của bộ lọc, giữ bảng cũ khi nạp lại, quy tắc dòng với bài đã rút hoặc không đọc được, danh sách là ảnh chụp không phát trực tuyến | 2026-10-24 |

Bảng 0.3 giải nghĩa viết tắt nguồn trích dẫn.

Bảng 0.3: Viết tắt nguồn.

| Viết tắt | Đường dẫn |
| :--- | :--- |
| RD | `01-rd/screens/users/USR0202_my_submissions.md` |
| BD | `02-bd/screens/users/USR0202_my_submissions.md` |
| F4 | `03-dd/api/F4_judge-orchestration.md` (số sau dấu hai chấm là dòng; "mục n" là số mục trong file) |
| V4 | `03-dd/validation/F4_judge-orchestration.md` |
| TDD4 | `04-tdd/modules/F4_judge-orchestration.md` |
| API | `03-dd/api/api.md` |
| MSV | `05-coding/frontend/src/views/users/my-submissions/ui/my-submissions-view.tsx` |
| ES | `05-coding/frontend/src/entities/submission/` |
| DD-SR | `03-dd/screens/users/USR0201_submission_result.md` |
| DEBT | `06-plan/PROTOTYPE_DEBT.md` |

## 1. Bản đồ slice FSD

Chiều import `app → views → widgets → features → entities → shared`. Bảng 1.1 là bản đồ đích.

Bảng 1.1: Slice của màn.

| Lớp | Đường dẫn (từ `src/`) | Vai trò | Hiện trạng |
| :--- | :--- | :--- | :--- |
| app | `app/(student)/submissions/page.tsx` | Render `MySubmissionsView` | Có |
| views | `views/users/my-submissions/ui/my-submissions-view.tsx` | Thanh lọc (B), bảng (C), chân bảng (D). Giữ **một file** (315 dòng [SoT: MSV:1-315]): BD hoãn việc tách tới DD [SoT: BD:225], DD quyết không tách vì không phần nào dùng lại ở màn khác (ponytail bậc 3) | Có |
| views | `views/users/my-submissions/model/use-submission-list-filters.ts` | Đọc, chuẩn hoá và ghi bốn tham số URL (BR-SC-01) | Chưa có |
| entities | `entities/submission/api/queries.ts` (`useMySubmissionsPage`) | Gọi F4 mục 10; đổi tham số và kiểu trả về theo Bảng 1.2 | Có, mock [SoT: ES api/queries.ts:17-22] |
| entities | `entities/submission/model/types.ts` (`SubmissionListItem`, `SubmissionListPage`, `ListMySubmissionsParams`) | Đổi theo Bảng 1.2 | Có [SoT: ES model/types.ts:70-101] |
| entities | `entities/submission/model/list-params.ts` | Schema zod của bốn tham số, dùng cho cả hook URL và query (mục 5) | Chưa có |
| entities | `entities/submission/model/status-variant.ts` (`SUBMISSION_STATUS_VARIANT`) | Màu badge đủ chín trạng thái, thay bảng cục bộ `VERDICT_VARIANT` thiếu ba trạng thái chưa cuối [SoT: MSV:43-50; ES model/status-variant.ts:6-19] | Có |
| shared | `shared/lib/use-debounced-value.ts` (S1), `shared/lib/use-search-param-state.ts` (S2) | **Nâng lên `shared/lib`**: hai màn danh sách đồng bộ bộ lọc lên URL (màn này và `USR0401`), luật "Frontend: shared first" của `CLAUDE.md`. Hiện `shared/lib` chưa có cả hai | Chưa có |

Thành phần dùng lại từ `shared/`: `FilterBar`, `FilterMenu`, `DataTable`, `Pagination`, `Badge`, `Button`, `QueryError`, `toast`, `formatDateVN`, `useT`, `useLocale` [SoT: MSV:10-41].

Thôi dùng: `useProblemLevels`, `problemLevelLabel`, `problemLevelTone` (nhãn độ khó nay do F4 trả sẵn, F4:610), và `usePersistedPageSize` nếu OQ-1 giữ mặc định [SoT: MSV:12-16, 24-29].

Bảng 1.2 liệt kê trường thực thể phải đổi khi nối API thật.

Bảng 1.2: Trường thực thể.

| Hiện có | Đổi thành | Nguồn |
| :--- | :--- | :--- |
| `id` | `submissionId` | F4:610 |
| `problemId`, `problemSlug`, `problemCode`, `problemTitle`, `difficulty` đứng riêng | `problem: { problemId, code, title, slug, levelDisplayName, withdrawn } \| null` | F4 §10.3 đã có `withdrawn`; D-1 đã đóng |
| `currentPage`, `totalItems` | `page`, `totalCount` | F4:60 |
| `pageSize` tham số | Bỏ khỏi request (F4 cố định 20) | F4:60; OQ-1 |
| `status` | Giữ chín giá trị | F4:61 |

## 2. API tiêu thụ

Bảng 2.1 ánh xạ sự kiện BD Sheet 8 sang endpoint. Hiện mọi dòng gọi API đều mock [SoT: ES api/queries.ts:17-22].

Bảng 2.1: Sự kiện BD, endpoint theo ID, hook, trạng thái nối.

| EVT (BD) | Endpoint theo ID | Hook | Nối |
| :--- | :--- | :--- | :--- |
| EVT-1 Tải màn | F4 mục 10 `GET /me/submissions` (`ListMySubmissions`, mục 1.1 số 7) | `useMySubmissionsPage(params)` | mock |
| EVT-2 Tìm theo từ khoá | như EVT-1, thêm `q` | như trên | mock |
| EVT-3 Lọc verdict, EVT-4 Lọc ngôn ngữ | như EVT-1, thêm `verdict`, `language` | như trên | mock |
| EVT-8, EVT-9, EVT-10 Chuyển trang | như EVT-1, đổi `page` | như trên | mock |
| EVT-5 Bấm tên bài | Không gọi API | `next/link` tới `/problems/{problem.problemId}` | không áp dụng |
| EVT-6 Bấm Kết quả | Không gọi API | `next/link` tới `/submissions/{submissionId}` (màn `USR0201`) | không áp dụng |
| EVT-7 Bấm Phân tích | Không gọi API; **không** gọi F5 | `next/link` tới `/submissions/{submissionId}/review` (màn `USR0301`) | không áp dụng |

Màn không có kênh thời gian thực (BR-SC-04). Điểm khác BD khi nối thật:

1. BD Sheet 4.2 có `memoryMb`, `levelCode`, `filteredAcceptedCount` [SoT: BD:187-188]; F4 mục 10.3 không trả ba trường này (F4:610). Màn không dùng chúng: Sheet 5 khu vực C không có cột bộ nhớ, chân bảng chỉ cần `totalCount` (BD:265).
2. BD Sheet 4.4 khu vực D và prototype có bộ chọn cỡ trang 10, 20, 50 [SoT: BD:211; MSV:53, 283-293]; F4 cố định 20 và không nhận tham số `pageSize` (F4:60, 599-606). Xem OQ-1.
3. BD EVT-5 điều hướng `/problems/{problem_slug}` [SoT: BD:360]; DD dùng `problemId` (UUID) theo `DEC-2026-1024-dd-screens-users-core-contract-changes` mục (3), như DD-SR BR-SC-05. Bản dựng dùng `problemSlug` [SoT: MSV:123-128].

## 3. Trạng thái màn

Bảng 3.1 liệt kê trạng thái theo vùng. Frontend không đọc `message` của lỗi (API:22); mục này chỉ có `401` và `5xx` vì F4 mục 10 không có mã nghiệp vụ riêng (F4:614).

Bảng 3.1: Trạng thái màn.

| Vùng | Trạng thái | Điều kiện | Hiển thị | Khoá i18n (`mySubmissions.`) |
| :--- | :--- | :--- | :--- | :--- |
| Bảng | Đang tải lần đầu | Chưa có dữ liệu nào | Khung chờ cho bảng, thanh lọc vẫn hiện; **không** hiện câu "không có bài nộp" (bản dựng hiện câu đó vì `lastPage = EMPTY_PAGE` [SoT: MSV:57-62, 87-91]) | `table.loading` (mới, chữ ẩn cho `role="status"`) |
| Bảng | Đang nạp lại | Đổi bộ lọc hoặc trang, đã có dữ liệu cũ | Giữ nguyên dòng cũ, bảng `aria-busy`, mờ nhẹ (BR-SC-02) | không có chữ |
| Bảng | Rỗng, không lọc | `totalCount = 0` và `q`, `verdict`, `language` đều mặc định | "Bạn chưa nộp bài nào" kèm liên kết `/problems` | `table.emptyNone` (mới), `table.goProblems` (mới) |
| Bảng | Rỗng, có lọc | `totalCount = 0` và có ít nhất một bộ lọc khác mặc định | "Không có bài nộp nào khớp bộ lọc." | `table.empty` [SoT: `messages/vi.json:1929`] |
| Màn | Lỗi lần đầu | Lỗi mạng hoặc `5xx`, chưa từng có dữ liệu | `QueryError` kèm Thử lại **thay thân bảng**, giữ tiêu đề và thanh lọc (khác bản dựng trả cả màn [SoT: MSV:217-223]) | `common.error`, `common.retry` |
| Bảng | Lỗi sau thao tác | Lỗi khi đổi bộ lọc, tìm, chuyển trang | Khôi phục tham số URL về giá trị ngay trước và hiện toast; bảng giữ kết quả cũ (BR-SC-02) | `toast.loadFailed` (mới) |
| Màn | Chưa đăng nhập | `401` sau khi làm mới token thất bại | Chuyển về `auth`, xử lý ở `shared/api/client.ts` | không áp dụng |
| Màn | Quyền bị từ chối | Không có: F4 mục 10 chỉ có `401` (F4:614) | Không có trạng thái | không áp dụng |
| Dòng | Bài đã rút | `problem.withdrawn = true` | Tên bài là chữ thường, nhãn "Đã rút khỏi ngân hàng", không liên kết (BR-SC-03) | `table.withdrawn` (mới) |
| Dòng | Không đọc được bài | `problem = null` | "Không tải được tên bài", không liên kết, **không** nhãn rút | `table.problemUnavailable` (mới) |
| Dòng | Chưa cuối | `status` là `PENDING`, `COMPILING`, `JUDGING` | Nhãn trạng thái, Testcase và Thời gian là `-`; không tự cập nhật (BR-SC-04) | `verdict.PENDING`, `verdict.COMPILING`, `verdict.JUDGING` (mới) |
| AI suy giảm | F5 hoặc nhà cung cấp LLM chết, ngân sách khoá | Bất kỳ | Không đổi gì: màn không gọi F5; nút Phân tích chỉ điều hướng, trạng thái AI hiện ở `USR0301` | không áp dụng (F4:64; `F4 AC-68`) |

## 4. Quy tắc đặc thù màn

### BR-SC-01 Bộ lọc nằm trên URL

- **Kích hoạt:** EVT-1; EVT-2 đến EVT-4; EVT-8 đến EVT-10.
- **Tham số:** `q`, `verdict`, `language`, `page` [SoT: BD:356]. Giá trị hợp lệ theo F4:604-606. Giá trị mặc định (rỗng, `ALL`, `1`) **không** ghi lên URL để URL gọn `[SoT: Suy luận]`.
- **Các bước:**
  1. Đọc `useSearchParams` qua schema zod có `.catch` (mục 5); giá trị lạ về mặc định, không báo lỗi (BD Sheet 9 NO 4 [SoT: BD:376]).
  2. Ô tìm kiếm giữ chữ gõ trong state cục bộ; `q` chỉ đổi trên URL và chỉ gọi API sau **400 ms** ngừng gõ [SoT: BD:357]. Nhấn Enter đẩy ngay và hiện toast số kết quả (đã có [SoT: MSV:98-105]).
  3. Mọi đổi `q`, `verdict`, `language` đặt `page` về 1 [SoT: MSV:93-96].
  4. Ghi URL bằng `router.replace`, không `push`: ý của BD là mở lại danh sách thì thấy đúng bộ lọc, mà URL hiện hành đã mang đủ bộ lọc; một mục lịch sử cho mỗi lần gõ làm nút Quay lại vô dụng `[SoT: Suy luận]`.
  5. F4 đưa `page` vượt cuối về trang cuối hợp lệ (F4:60): màn đồng bộ `page` trên URL theo `page` của phản hồi, bằng `replace`.
- **Hậu điều kiện:** URL luôn mô tả đúng điều đang hiện; mở URL ở tab khác ra cùng bảng. Màn không nhận và không gửi `userId`; phạm vi là người trong token (F4:57, 597).
- **Hiện trạng:** prototype giữ trạng thái ở `useState`, chưa đọc URL [SoT: MSV:5-7, 68-76].

### BR-SC-02 Giữ bảng cũ khi nạp lại, và lỗi sau thao tác

- **Kích hoạt:** đổi khoá truy vấn (mọi đổi tham số ở BR-SC-01).
- **Các bước:**
  1. Truy vấn dùng `placeholderData: keepPreviousData` thay cho state `lastPage` tự giữ [SoT: MSV:85-91]; chỉ kết quả của khoá hiện hành được vẽ, kết quả đến muộn của khoá cũ không ghi đè.
  2. Trong lúc nạp lại bảng `aria-busy="true"`, không hiện câu rỗng.
  3. Lỗi khi chưa có dữ liệu: `QueryError` trong thân bảng.
  4. Lỗi khi đã có dữ liệu cũ: đặt lại tham số URL về giá trị ngay trước (`replace`), `toast.error(loadFailed)`. Lý do: BD chỉ ghi "toast lỗi tải dữ liệu" [SoT: BD:357-359], nhưng giữ bảng cũ trong khi thanh lọc đã đổi sang giá trị mới làm bảng và bộ lọc nói hai điều khác nhau; cách khôi phục này cùng tinh thần EVT-3 của `USR0401` ("trả tab về giá trị trước") `[SoT: Suy luận]`.
- **Hậu điều kiện:** bảng và thanh lọc luôn khớp nhau.

### BR-SC-03 Dòng bài nộp

- **Kích hoạt:** dựng từng phần tử `items[]`.
- **Quy tắc:**
  1. **Cột Bài toán** theo `problem` (ba trường hợp): (a) có và `withdrawn = false`: liên kết `/problems/{problem.problemId}` với chữ `{code} · {title}`, kèm badge `levelDisplayName` (trung tính, xem D-2 và OQ-2); (b) `withdrawn = true`: chữ thường không liên kết, nhãn "Đã rút khỏi ngân hàng", badge độ khó vẫn hiện (BD:30); (c) `problem = null`: câu "Không tải được tên bài", không liên kết, không nhãn rút. `null` chỉ là lỗi của `problem-bank`, không phải bài đã rút (cùng quy tắc F4:486 và DD-SR BR-SC-05).
  2. **Nút Kết quả** luôn bật mọi dòng, kể cả bài rút, `problem = null` và dòng chưa cuối [SoT: BD:299].
  3. **Nút Phân tích** bật khi và chỉ khi `status = ACCEPTED` (F5-01 [SoT: BD:25, 300]); các dòng khác nút vô hiệu bằng `aria-disabled="true"` kèm mô tả bằng văn bản qua `aria-describedby`, không chỉ `title` (tooltip của phần tử bị vô hiệu không tới được bằng bàn phím [SoT: MSV:198-200]; cùng DD-SR BR-SC-05). Bài đã rút vẫn bật: báo cáo hiện đủ ở `USR0301` [SoT: `03-dd/screens/users/USR0301_solution_review.md:106`].
  4. **Ba ô số:** Testcase là `{passedCount}/{totalCount}` khi cả hai khác `null`, ngược lại `-`; Thời gian là `{runtimeMs} ms` hoặc `-`; Kết quả là nhãn của đủ chín `status`, màu theo `SUBMISSION_STATUS_VARIANT` [SoT: MSV:160-178].
  5. **Thời điểm** theo `formatDateVN` (UTC+7, theo ngôn ngữ giao diện) [SoT: MSV:114].
- **Dữ liệu, không phải chỉ thị:** tên bài do người soạn viết và `q` do người học gõ hiện bằng nút văn bản của React; `q` chỉ đi vào query string đã mã hoá, không ghép vào chuỗi i18n hay HTML.

### BR-SC-04 Danh sách là ảnh chụp, không phát trực tuyến

- **Quy tắc:** màn không mở STOMP và không thăm dò theo chu kỳ. Dòng `PENDING`, `COMPILING`, `JUDGING` giữ đúng trạng thái lúc nạp; muốn theo dõi thì bấm Kết quả, nơi `USR0201` BR-SC-01 và BR-SC-02 nạp và phát trực tuyến (DD-SR mục 4).
- **Nạp lại khi:** mở màn (`staleTime: 0`, `refetchOnMount: "always"`; mặc định 30 giây của `useMockQuery` [SoT: `05-coding/frontend/src/shared/api/mock-query.ts:33-47`] làm một bài vừa chấm xong còn hiện `JUDGING` khi người học quay lại) và khi cửa sổ lấy lại focus (mặc định của TanStack Query).
- **Lý do:** kênh thời gian thực theo từng bài nộp thuộc `USR0102` và `USR0201` (F4 mục 2); thêm một kênh cho danh sách không có yêu cầu (ponytail bậc 3).

## 5. Kiểm tra phía client

Màn không có form ghi. Bảng 5.1 là bốn tham số URL mà màn chuẩn hoá trước khi gọi F4; máy chủ vẫn chuẩn hoá lại theo cùng quy tắc (V4:47-52).

Bảng 5.1: Tham số, zod, quy tắc `V4`, khớp.

| Trường | zod | Quy tắc ở `V4` | Khớp |
| :--- | :--- | :--- | :--- |
| `q` | Ô nhập `maxLength = 100` (đã có [SoT: MSV:55, 240]); khi dựng tham số: `z.string().transform((s) => s.trim().slice(0, 100)).catch("")`. Không cắt khoảng trắng trong ô lúc đang gõ | V4:49: bỏ khoảng trắng hai đầu, cắt ở 100, không báo lỗi | Khớp |
| `verdict` | `z.enum(["ALL","ACCEPTED","WRONG_ANSWER","TIME_LIMIT_EXCEEDED","COMPILE_ERROR","RUNTIME_ERROR"]).catch("ALL")` | V4:50: sáu giá trị, lạ về `ALL`. `PENDING`, `COMPILING`, `JUDGING`, `SYSTEM_ERROR` chỉ hiện ở `ALL` (F4:604) | Khớp; prototype kiểu `VerdictFilter` rộng chín giá trị [SoT: ES model/types.ts:85], thu về sáu |
| `language` | `z.enum(["ALL","JAVA","CPP","PYTHON"]).catch("ALL")` | V4:51 | Khớp |
| `page` | `z.coerce.number().int().min(1).catch(1)`; không chặn trên | V4:52: không hợp lệ về 1, vượt cuối về trang cuối (máy chủ) | Khớp |

## 6. i18n và truy cập

**Tiền tố khoá:** `mySubmissions.*` ở `messages/vi.json:1893-1942` và `messages/en.json:1893` [SoT: `05-coding/frontend/messages/`]. Khoá **thêm** ở `vi.json` và `en.json` cùng lượt: `verdict.PENDING`, `verdict.COMPILING`, `verdict.JUDGING`, `table.loading`, `table.emptyNone`, `table.goProblems`, `table.withdrawn`, `table.problemUnavailable`, `table.needAcceptedDesc` (câu mô tả cho `aria-describedby`, thay vai trò `table.needAccepted` đang đặt ở `title`), `toast.loadFailed`. Khoá **bỏ** khi OQ-1 giữ mặc định: `footer.pageSizeLabel`.

**Nhãn ngôn ngữ:** `language.PYTHON` "Python 3", `CPP` "C++ 17", `JAVA` "Java 21" [SoT: `vi.json:1911-1915`] mang số phiên bản mà F4 không có nguồn dữ liệu nào (F4 chỉ có enum `JAVA`, `CPP`, `PYTHON`, F4:61, 605). Theo quy tắc cứng README mục 5 (không hiện giá trị chỉ có trong mock), đổi thành "Python", "C++", "Java" (đúng BD Sheet 5 khu vực B NO 3 [SoT: BD:244]).

**Không đặt tên endpoint, mã quyền hay thuật ngữ backend vào nhãn.**

**Truy cập** (chỉ ghi khác biệt so với `shared/ui`):

| Hạng mục | Quy tắc |
| :--- | :--- |
| Bảng | `DataTable` đã có `caption` (`table.caption`); `<th scope="col">`; cột hành động có tiêu đề ẩn cho trình đọc màn hình |
| Kết quả lọc | Dòng tóm tắt chân bảng bọc `role="status"` để đọc "Hiển thị a - b trong số n bài nộp" một lần sau mỗi lần nạp xong (không đọc từng dòng) `[SoT: Suy luận]` |
| Nút Phân tích vô hiệu | `aria-disabled="true"` và `aria-describedby` trỏ câu `table.needAcceptedDesc` (BR-SC-03) |
| Màu verdict | Luôn đi kèm chữ trong `Badge` |
| Thứ tự focus | Tiêu đề, ô tìm kiếm, menu verdict, menu ngôn ngữ, từng dòng (liên kết bài, Kết quả, Phân tích), chân bảng (trước, số trang, sau). Sau khi đổi trang, focus về tiêu đề bảng và cuộn lên đầu bảng (BD EVT-8 đến EVT-10 [SoT: BD:363-365]) |

## 7. Điểm kiểm thử

Bảng 7.1 phủ mọi sự kiện BD Sheet 8. Công cụ theo `CLAUDE.md`: Vitest cho view và logic, Playwright cho luồng qua màn. Vitest sẵn có ở `views/users/my-submissions/ui/my-submissions-view.test.tsx` (5 ca, trong đó ca đổi cỡ trang sẽ bỏ cùng OQ-1) [SoT: `05-coding/frontend/src/views/users/my-submissions/ui/my-submissions-view.test.tsx:30-92`].

Bảng 7.1: Điểm kiểm thử.

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| T-MY-01 | Normal | EVT-1: khung chờ rồi 20 dòng, URL không có tham số | Vitest | F4 AC-37 |
| T-MY-02 | Boundary | EVT-1 lần đầu: trong lúc chờ **không** hiện "Không có bài nộp nào khớp bộ lọc" | Vitest | TDD màn `MY-AC-01` |
| T-MY-03 | Normal | EVT-1 người học chưa nộp bài nào: `table.emptyNone` kèm liên kết `/problems` | Vitest | TDD màn `MY-AC-01` |
| T-MY-04 | Normal | EVT-2: gõ liên tiếp chỉ gọi API một lần sau 400 ms (đồng hồ giả); `q` lên URL bằng `replace`; `page` về 1; Enter gọi ngay và toast số kết quả | Vitest | F4 AC-37 |
| T-MY-05 | Boundary | EVT-2: ô dừng ở 100 ký tự; tham số `q` gửi đi không quá 100 và đã bỏ khoảng trắng hai đầu | Vitest | F4 AC-37 |
| T-MY-06 | Normal | EVT-3, EVT-4: lọc verdict và ngôn ngữ cộng dồn, menu verdict đủ sáu giá trị, `page` về 1 | Vitest | F4 AC-37 |
| T-MY-07 | Abnormal | EVT-1: URL `verdict=FOO&language=RUST&page=-2` về `ALL`, `ALL`, `1`, không lỗi, không toast | Vitest | F4 AC-37 |
| T-MY-08 | Normal | EVT-8 đến EVT-10: đổi trang, `page` trên URL, cuộn lên đầu bảng; `page=99` thì URL về trang cuối của phản hồi | Vitest | F4 AC-37 |
| T-MY-09 | Abnormal | EVT-1: lỗi `5xx` lần đầu hiện `QueryError` trong thân bảng, thanh lọc còn; Thử lại gọi lại | Vitest | TDD màn `MY-AC-02` |
| T-MY-10 | Abnormal | EVT-2 đến EVT-4: lỗi sau thao tác khôi phục tham số URL, toast, bảng cũ còn | Vitest | TDD màn `MY-AC-02` |
| T-MY-11 | Normal | EVT-6: Kết quả bật mọi dòng (đã cuối, chưa cuối, bài rút, `problem = null`) và dẫn `/submissions/{submissionId}` | Vitest | TDD màn `MY-AC-03` |
| T-MY-12 | Normal | EVT-7: Phân tích bật chỉ khi `ACCEPTED`; dòng khác `aria-disabled` kèm mô tả; bài rút `ACCEPTED` vẫn bật | Vitest | `F5 AC-05` |
| T-MY-13 | Normal | EVT-5 và cột Bài toán ba trường hợp: có liên kết `/problems/{problemId}`; rút có nhãn, không liên kết; `problem = null` không nhãn, không liên kết | Vitest | TDD màn `MY-AC-03` |
| T-MY-14 | Normal | Dòng chưa cuối hiện nhãn `verdict.PENDING`/`COMPILING`/`JUDGING` và `-`; không có thăm dò theo chu kỳ | Vitest | TDD màn `MY-AC-01`, `MY-AC-03` |
| T-MY-15 | Normal | Quay lại từ `USR0201` sau khi bài vừa chấm xong: dòng đã đổi trạng thái (nạp lại khi mở) | Playwright | TDD màn `MY-AC-03` |
| T-MY-16 | Security | Không có tham số nào chọn người dùng; yêu cầu gửi đi chỉ có bốn tham số | Vitest | F4 AC-37 |
| T-MY-17 | Normal | F5 chết: danh sách và hai nút vẫn như cũ, bấm Phân tích vẫn điều hướng | Vitest | F4 AC-68 |

## 8. Nợ prototype và câu hỏi mở

**Dòng nợ sẽ gỡ.** Dòng `views/my-submissions + entities/submission (dùng chung)` ở `DEBT:886` có "Điều kiện tốt nghiệp: Có 2 DD trên → viết DD → đồng bộ URL param → thay mock → test" [SoT: `06-plan/PROTOTYPE_DEBT.md:886`]. DD xong chỉ hoàn thành bước "viết DD"; đồng bộ URL, thay mock và test **chưa làm** nên dòng này **chưa gỡ**. Dòng đó còn nhắc `03-dd/api/F1_identity.md`: không còn cần, màn đã bỏ thống kê của `identity` (BD V0.5 [SoT: BD:77]).

Bảng 8.1 liệt kê lệch giữa prototype và thiết kế.

Bảng 8.1: Lệch giữa prototype và thiết kế.

| # | Prototype hiện tại | Thiết kế | Nguồn |
| :-: | :--- | :--- | :--- |
| P1 | Bộ lọc và trang ở `useState`, mất khi tải lại | BR-SC-01 | MSV:68-76 |
| P2 | Lần tải đầu hiện "Không có bài nộp" | Khung chờ | MSV:57-62, 87-91 |
| P3 | Lỗi lần đầu và lỗi sau thao tác đều thay cả màn bằng `QueryError` | BR-SC-02 | MSV:217-223 |
| P4 | Liên kết bài dùng `problemSlug` | `problemId` | MSV:123-128 |
| P5 | Không có nhãn bài rút, không xử lý `problem = null` | BR-SC-03 | MSV:118-135 |
| P6 | Nút Phân tích vô hiệu chỉ có `title` | `aria-describedby` | MSV:194-211 |
| P7 | Bảng màu cục bộ thiếu ba trạng thái chưa cuối; nhãn thiếu `verdict.PENDING` và hai khoá nữa | `SUBMISSION_STATUS_VARIANT` và khoá mới | MSV:43-50; `vi.json:1903-1910` |
| P8 | Bộ chọn cỡ trang 10, 20, 50 nhớ ở trình duyệt | OQ-1 | MSV:53, 72-76 |
| P9 | Nhãn ngôn ngữ có số phiên bản | Bỏ số phiên bản | `vi.json:1911-1915` |
| P10 | Màu độ khó tra từ danh mục `useProblemLevels` | `levelDisplayName` của F4, badge trung tính (D-2, OQ-2) | MSV:130-132 |
| P11 | Mock lọc và phân trang phía trình duyệt, `staleTime` 30 giây | Máy chủ lọc, `staleTime: 0` | `submission-mocks.ts:304-332`; `mock-query.ts:33` |

Bảng 8.2 liệt kê sai lệch phát hiện ở tài liệu khác. DD này không sửa RD, BD hay DD module.

Bảng 8.2: Sai lệch ở tài liệu khác.

| # | Nơi | Sai lệch | Đề xuất |
| :-: | :--- | :--- | :--- |
| D-1 | F4 §10.3 | Đã đóng: `StudentSubmissionItemDto.problem.withdrawn` có trong F4, bài rút vẫn giữ tên và lịch sử, tắt link mở đề | DD màn dùng cờ này; TDD để giai đoạn sau |
| D-2 | F4 §10.3 và BD V0.3 | BD cũ giữ màu badge theo ba mức khởi tạo, còn F4 chỉ trả `levelDisplayName` | Đã chốt badge trung tính, không thêm `levelCode`; đồng bộ BD ở lượt sau (`DEC-2026-1024-dd-my-submissions-page-and-badge`) |
| D-3 | BD:97, 141, 360 | Route `/my-submissions`, đích bài `/problems/{problem_slug}` | Sửa BD thành `/submissions` và `/problems/{problemId}` |
| D-4 | F4:618 (mục 10.5) | Bước 1 không nêu điều gì xảy ra khi `searchProblemIds` lỗi (mục 10.4 "không có lỗi nghiệp vụ riêng") | Chưa chốt; màn coi mọi `5xx` theo Bảng 3.1 |

Bảng 8.3 là câu hỏi mở mức màn, mặc định đề xuất và bậc ponytail (`.claude/rules/ponytail-criteria.md`).

Bảng 8.3: Câu hỏi mở.

| # | Câu hỏi | Mặc định đề xuất | Bậc |
| :-: | :--- | :--- | :-: |
| OQ-1 | **Đã chốt** `DEC-2026-1024-dd-my-submissions-page-and-badge`: bỏ bộ chọn cỡ trang | F4 cố định 20 dòng/trang; đồng bộ BD và prototype ở lượt sau | 5 |
| OQ-2 | **Đã chốt** cùng quyết định: badge độ khó màu trung tính | Dùng `levelDisplayName`; không thêm `levelCode` vào F4 chỉ để tô màu | 1 |
