# DD màn hình — Phân tích bài giải (`USR0301`)

> Hợp đồng khung: `03-dd/screens/README.md`. File này chỉ ghi phần chính thức của tầng màn hình (cấu trúc slice, hook, trạng thái, quy tắc đặc thù màn, i18n, a11y). Request, response, mã lỗi chỉ nằm ở `03-dd/api/`; logic phía máy chủ chỉ nằm ở `03-dd/logic/`. Viết sau prototype (`03-dd/screens/README.md:7`), nên mọi dòng "Hiện trạng" dẫn mã thật ở `05-coding/frontend/src/`.

## 0. Thông tin tài liệu

Bảng 0.1 là bìa tài liệu; mỗi dòng nêu một thuộc tính nhận dạng của màn.

**Bảng 0.1: Bìa**

| Mục | Giá trị |
| :--- | :--- |
| Mã màn, slug | `USR0301`, `solution_review` |
| Tên màn | Phân tích bài giải |
| Nhóm actor | `users` (A1, `STUDENT`, chủ bài nộp) |
| Route | `/submissions/{submissionId}/review` (`app/(student)/submissions/[submissionId]/review/page.tsx`) |
| Bounded Context chủ | `ai-review` F5.1; đọc `judge-orchestration` F4 (mã nguồn, nhãn bài); liên kết sang `USR0302`, `USR0102` |
| RD nguồn | `01-rd/screens/users/USR0301_solution_review.md` |
| BD nguồn | `02-bd/screens/users/USR0301_solution_review.md` (V1.7) |
| Prototype | `05-coding/frontend/src/views/users/solution-review/`, `05-coding/frontend/src/entities/solution-review/` |
| Phiên bản DD | V0.1, 2026-10-24 |

**Bảng 0.2: Lịch sử sửa đổi**

| Ver | Nội dung | Ngày | Người sửa |
| :--- | :--- | :--- | :--- |
| V0.1 | Tạo mới từ prototype đã duyệt hình một phần và DD module F5 (2026-10-23). Chưa có owner duyệt | 2026-10-24 | Claude |

Bảng 0.3 giải nghĩa các tên viết tắt dùng trong cột trích dẫn của file này.

**Bảng 0.3: Tên viết tắt nguồn**

| Viết tắt | Đường dẫn đầy đủ |
| :--- | :--- |
| RD301 | `01-rd/screens/users/USR0301_solution_review.md` |
| BD301 | `02-bd/screens/users/USR0301_solution_review.md` |
| API-F5 §n | `03-dd/api/F5_ai-review.md` mục n (mục 1.1 Bảng 1 là danh mục) |
| API-F4 #n | `03-dd/api/F4_judge-orchestration.md`, số trong catalog mục 1.1 (kèm số mục trong file khi cần) |
| LOG-F5 | `03-dd/logic/F5_ai-review.md` (khối `BR-AI-nn`) |
| VAL-F5 | `03-dd/validation/F5_ai-review.md` |
| TDD-F5 | `04-tdd/modules/F5_ai-review.md` (`AC-nn` của module) |
| TDD-SR | `04-tdd/screens/users/USR0301_solution_review.md` (`AC-SR-nn`, phần xuyên module của màn) |
| FE | `05-coding/frontend/src/` |
| DEBT | `06-plan/PROTOTYPE_DEBT.md` |

## 1. Bản đồ slice FSD

Bảng 1.1 liệt kê slice màn dùng; chiều import theo `eslint.config`: `app → views → widgets → features → entities → shared`, entity chỉ được import `shared` (`05-coding/frontend/eslint.config.mjs:5`, `:110-113`).

**Bảng 1.1: Slice của màn**

| Lớp | Đường dẫn (từ `FE`) | Vai trò | Hiện trạng |
| :--- | :--- | :--- | :--- |
| views | `views/users/solution-review/ui/solution-review-view.tsx` | Một file chứa `SolutionReviewContent` (trạng thái màn), `LoadingSkeleton`, `ReadyReport`, `Stat` (`solution-review-view.tsx:77`, `:222`, `:232`, `:437`) | Có. Giữ **một file**: BD301 mục 4.5 liệt kê 7 file như đích nhưng không có yêu cầu nguồn nào bắt tách; tách `CodeDiffCard` khi thay bằng Monaco Diff Editor (ponytail bậc 3) |
| views | `app/(student)/submissions/[submissionId]/review/page.tsx` | Trang route, dựng `SolutionReviewView` | Có |
| features | không | "Áp bản đề xuất vào Workspace" chỉ màn này dùng, nên ở lại trong view (luật `CLAUDE.md` "Frontend: shared first": một nơi dùng, thuộc một miền, ở lại slice) | Không tạo |
| entities | `entities/solution-review` (`model/types.ts`, `model/review-errors.ts`, `api/queries.ts`, `lib/line-diff.ts`) | Kiểu báo cáo, mã lỗi màn rẽ nhánh, hook `useSolutionReview`, hàm `lineDiff` | Có, đang mock |
| entities | `entities/submission` (`useSubmissionDetail`, `entities/submission/api/queries.ts:29`) | Mã nguồn, ngôn ngữ, mô hình nộp, tên và nhãn độ khó của bài, từ API-F4 #5 | Hook có; màn **chưa dùng** (mã nguồn nằm trong mock của `solution-review`) |
| entities | `entities/problem` (`saveWorkspaceDraft`, `entities/problem/model/workspace-draft.ts:16`) | Ghi mã đề xuất vào nháp trình duyệt của Workspace | Có |
| shared | `shared/ui`: `Badge`, `Button`, `Card`, `ConfirmDialog`, `NoticeTile`, `Skeleton`, `SeeAllLink`; `shared/lib`: `formatDateVN`, `toast-store`; `shared/api`: `ApiError`, `useMockQuery` | Dùng lại nguyên trạng | Có |

Việc **cần đổi ở lớp dưới** khi nối API thật:

1. `SolutionReviewDetail` (`entities/solution-review/model/types.ts:35-58`) hiện gộp trường của ba nguồn (`problemTitle`, `difficulty`, `sourceCode`, `languageKey`, `submissionModel` của F4 và F2; còn lại của F5). Khi nối, kiểu này chỉ giữ phần F5 (`id`, `submissionId`, `problemId`, `language`, `createdAt`, `servedFromCache`, `isReferenceOnly`, `readabilityScore`, `schemaVersion`, `approachTitle`, `verdict`, `isApproachOptimal`, `complexity`, `strengths`, `improvements`, `edgeCases`, `followUpTopics`, `codeDiff`; tên trường ở API-F5 §4.3). View ghép với `useSubmissionDetail` (entity không được import entity khác, nên phép ghép nằm ở view). Lý do: API-F5 §4.3 nói rõ `problemTitle`, `levelCode`, `levelDisplayName` **không** nằm trong phản hồi F5 để báo cáo vẫn hiện khi `problem-bank` lỗi (`03-dd/api/F5_ai-review.md:263`).
2. `problemLevelLabel` và `useProblemLevels` (`solution-review-view.tsx:33-36`, `:241`) thôi dùng: API-F4 #5 trả sẵn `problem.levelDisplayName` (`03-dd/api/F4_judge-orchestration.md:486`), không cần tra danh mục độ khó phía máy khách.
3. `review-errors.ts:3-9` đặt mã tự chế (`AI_BUDGET_LOCKED`, `AI_PROVIDER_TIMEOUT`, `NOT_FOUND`, `FORBIDDEN`, `NOT_ACCEPTED`) và tham số `?demo=`. Thay bằng mã `AIR-nnn` của Bảng 3.2; `?demo=` (`solution-review-view.tsx:58-66`, `:83-86`) chỉ tồn tại tới khi nối API.
4. `useSolutionReview` hiện có `demo` trong `queryKey` (`entities/solution-review/api/queries.ts:9-17`); khi nối, `queryKey = ["solution-review", submissionId]`.

## 2. API tiêu thụ

Bảng 2.1 ánh xạ sự kiện của BD301 Sheet 8 sang endpoint ở file API; cột "Thật hay mock" nêu hiện trạng của hook. Không ghi lại request hay response.

**Bảng 2.1: Sự kiện, endpoint, hook**

| EVT (BD301) | Endpoint (ID) | Hook, query | Thật hay mock |
| :--- | :--- | :--- | :--- |
| EVT-1 Mở màn hình | API-F5 §4 `POST /api/v1/ai/submissions/{submissionId}/solution-review` (`GetOrCreateSolutionReview`) | `useSolutionReview` (`entities/solution-review/api/queries.ts:9`) | Mock (`useMockQuery`, `spec` trỏ `03-dd/api/F5_ai-review.md`) |
| EVT-1 (phần mã nguồn, nhãn bài) | API-F4 #5 `GET /api/v1/judge/submissions/{submissionId}` (mục 8 của file F4; BD301 Sheet 7.3 NO 2 gọi tên `GetSubmissionSourceCode`) | `useSubmissionDetail` (`entities/submission/api/queries.ts:29`) | Hook có; màn chưa gọi |
| EVT-2 Bấm breadcrumb | Không có | `next/link` tới `/submissions/{submissionId}` (`solution-review-view.tsx:122-134`) | Không áp dụng |
| EVT-3 Bấm Phỏng vấn bài này | Không có | `next/link` tới `/submissions/{submissionId}/interview` (`solution-review-view.tsx:270-274`) | Không áp dụng |
| EVT-4, EVT-5 Mở, huỷ popup | Không có | state `applyConfirmOpen` (`solution-review-view.tsx:89`) | Không áp dụng |
| EVT-6 Xác nhận áp dụng | Không có endpoint. Ghi `localStorage` qua `saveWorkspaceDraft`, rồi điều hướng `/workspace/{problemId}`. F5-26 thuộc tầng Workspace (API-F5 §1.1, `03-dd/api/F5_ai-review.md:78`) | `saveWorkspaceDraft` (`entities/problem/model/workspace-draft.ts:16`) | Thật (trình duyệt) |
| EVT-7 Bấm Thử lại | Như EVT-1 (`query.refetch()`, `solution-review-view.tsx:150`) | `useSolutionReview().refetch` | Mock |

Các hệ quả của hợp đồng F5 đối với cách gọi (chi tiết ở BR-SC-01):

- API-F5 §4 là `POST` nhưng **idempotent** (`03-dd/api/F5_ai-review.md:231`): gọi lại cho cùng `submissionId` trả đúng báo cáo đã lưu, không tốn token (LOG-F5 BR-AI-02 bước 3, `03-dd/logic/F5_ai-review.md:75`). Vì vậy dùng `useQuery` chứ không `useMutation`, kèm các tuỳ chọn ở BR-SC-01 để không bao giờ tự gọi lại ngầm.
- Không có kênh realtime ở màn này: báo cáo là một phản hồi JSON đồng bộ (chờ tối đa 30 giây, `03-dd/api/F5_ai-review.md:206`). SSE chỉ có ở `USR0302`.

## 3. Trạng thái màn

Bảng 3.1 liệt kê các trạng thái loại trừ nhau của vùng thân màn (BD301 Sheet 5 Khu vực I thêm các trạng thái DD này bổ sung); Bảng 3.2 ánh xạ `error.code` sang trạng thái và khoá i18n. Frontend không dựa vào `message` (`03-dd/api/api.md:22`).

**Bảng 3.1: Trạng thái của màn**

| Trạng thái | Điều kiện vào | Thấy gì | Thoát bằng |
| :--- | :--- | :--- | :--- |
| Loading | `query.isFetching` (kể cả khi Thử lại, `solution-review-view.tsx:93`) | Skeleton thay toàn bộ Khu vực B đến G, tối đa 30 giây phía máy chủ (BD301 Sheet 5 Khu vực I NO 1). **Bổ sung của DD:** vùng `role="status"` phải có chữ cho trình đọc màn hình, khoá `solutionReview.loadingLabel` (hiện `LoadingSkeleton` chỉ có `aria-busy`, không có chữ: `solution-review-view.tsx:222-229`) | Có báo cáo hoặc lỗi |
| Ready | Có `data` và không lỗi | Khu vực A đến G đầy đủ. Banner giáo dục F5-18 hiện **vô điều kiện**, không đọc `isReferenceOnly` để quyết định hiện hay ẩn (BD301 Sheet 6 Khu vực C NO 1) | Rời màn |
| Rỗng từng phần (trong Ready) | `strengths`, `improvements`, `edgeCases` rỗng: thẻ vẫn hiện, danh sách trống. `followUpTopics` rỗng: ẩn cả thẻ (BD301 Sheet 5 Khu vực F NO 4). `codeDiff.suggestedCode = null` (`verdict = OPTIMAL`): ẩn thẻ Diff và nút Áp dụng (`solution-review-view.tsx:383`, `:421`). `complexity.optimalTime`, `optimalSpace` là `null`: hiện "—" (`solution-review-view.tsx:303`, `:317`; TDD-F5 AC-11) | Như mô tả | Không áp dụng |
| Lỗi tạm thời | `AIR-501`, `AIR-502`, hết hạn phía máy khách, mã lạ, lỗi mạng | `NoticeTile` cảnh báo kèm nút Thử lại, và một toast mỗi lần thất bại (BD301 Sheet 8 EVT-1, EVT-7) | Thử lại |
| Chờ giới hạn tần suất (BD301 Sheet 5 Khu vực I NO 5, thêm theo `DEC-2026-1024-dd-screens-ai-topic-length-and-rd-trim`) | `AIR-402`, `AIR-404` | `NoticeTile` cảnh báo; nút Thử lại tắt cho tới khi hết `details.retryAfterSeconds` | Hết đếm ngược, bấm Thử lại |
| Bị khoá ngân sách | `AIR-401` | `NoticeTile` đỏ, **không** có nút Thử lại, có liên kết thoát; toast (BD301 Sheet 9 NO 5) | Rời màn |
| Không mở được | `AIR-101`, `AIR-302`, `AIR-102` | Một `NoticeTile` chung cho cả ba nguyên nhân kèm liên kết về Bài đã nộp, không Thử lại. Gộp có chủ ý: không lộ bài nộp của người khác (API-F5 §4.4, `03-dd/api/F5_ai-review.md:270`) | Rời màn |
| Đang được sinh ở nơi khác | `AIR-103` (409) | Giữ nguyên skeleton, không báo lỗi | Tự gọi lại sau 3 giây (BR-SC-01 bước 4) |
| Bài đã rút | `problem.withdrawn = true` ở API-F4 #5 (`03-dd/api/F4_judge-orchestration.md:486`) | Báo cáo vẫn hiện đủ; nhãn `solutionReview.withdrawn` ("Đã rút khỏi ngân hàng") hiện cạnh tên bài ở Khu vực B; nút Áp dụng giữ nguyên (BR-SC-05) | Không áp dụng |
| Một phần thất bại (F4 lỗi) | F5 thành công, `useSubmissionDetail` lỗi hoặc `problem = null` | Báo cáo vẫn hiện; tên bài là "—"; khối "Code đã nộp" và thẻ Diff ẩn kèm câu `solutionReview.sourceUnavailable`; nút Áp dụng **tắt** vì thiếu `submissionModel` | Tải lại trang |
| Chưa đăng nhập | `401` (mã chung) | Chuyển về `auth` theo khung (BD301 Sheet 9 NO 1); màn không tự xử lý | Đăng nhập |
| AI suy giảm | `AIR-501`, `AIR-502`, `AIR-401` | Chỉ vùng thân đổi; breadcrumb, điều hướng và mọi màn F1 đến F4 chạy bình thường (CLAUDE.md "AI subsystem degrades gracefully"; API-F5 §1.2 mục "Suy giảm êm", `03-dd/api/F5_ai-review.md:91`). Báo cáo **đã lưu** vẫn mở được kể cả khi AI chết hoặc khoá ngân sách (TDD-F5 AC-02, AC-08) | Không áp dụng |

Bảng 3.2 là bản ánh xạ chính thức `error.code` sang trạng thái và khoá i18n của màn này (bảng mã ở `03-dd/api/F5_ai-review.md:1875-1928`). Khoá có dấu sao là khoá **mới**, chữ là đề xuất chưa duyệt.

**Bảng 3.2: `error.code` sang trạng thái và khoá i18n (tiền tố `solutionReview.`)**

| error.code | HTTP | Trạng thái | Khoá i18n tiêu đề, nội dung | Toast | Thử lại |
| :--- | :-: | :--- | :--- | :--- | :-: |
| `AIR-101` | 404 | Không mở được | `errorUnavailableTitle`, `errorUnavailableBody` (`vi.json:2111-2112`) | `toast.unavailable` (`vi.json:2107`) | Không |
| `AIR-302` | 403 | Không mở được | như trên | như trên | Không |
| `AIR-102` | 422 | Không mở được | như trên | như trên | Không |
| `AIR-103` | 409 | Loading (gọi lại tự động) | không có thông báo | không | tự động |
| `AIR-401` | 429 | Bị khoá ngân sách | `errorBudgetLockedTitle`, `errorBudgetLockedBody` (`vi.json:2096-2097`) | `toast.budgetLocked` (`vi.json:2104`) | Không |
| `AIR-402` | 429 | Chờ giới hạn tần suất | `errorRateLimitedTitle*`, `errorRateLimitedBody*` | `toast.rateLimited*` | sau `retryAfterSeconds` |
| `AIR-404` | 429 | Chờ giới hạn tần suất | như trên | như trên | sau `retryAfterSeconds` |
| `AIR-501` | 503 | Lỗi tạm thời | `errorTransientTitle`, `errorTransientBody` (`vi.json:2093-2094`) | `toast.transientError` (`vi.json:2105`) | Có |
| `AIR-502` | 502 | Lỗi tạm thời | như trên | như trên | Có |
| `AIR-V01`... (400 kiểm đầu vào) | 400 | Không xảy ra ở màn này (không có tham số do người dùng nhập) | coi như lỗi tạm thời | `toast.transientError` | Có |
| (không có `error.code`: mạng đứt, hết hạn 40 giây phía máy khách) | | Lỗi tạm thời | như `AIR-501` | như `AIR-501` | Có |

Chữ đề xuất cho khoá mới:

| Khoá | Chữ vi |
| :--- | :--- |
| `solutionReview.loadingLabel` * | Đang phân tích bài giải, có thể mất tới 30 giây |
| `solutionReview.errorRateLimitedTitle` * | Chưa thể phân tích lúc này |
| `solutionReview.errorRateLimitedBody` * | Bạn đã yêu cầu phân tích quá nhiều lần. Hãy thử lại sau {seconds} giây |
| `solutionReview.retryIn` * | Thử lại sau {seconds} giây |
| `solutionReview.toast.rateLimited` * | Bạn yêu cầu hơi nhanh. Thử lại sau {seconds} giây |
| `solutionReview.sourceUnavailable` * | Chưa tải được mã đã nộp nên chưa hiện bản so sánh |
| `solutionReview.withdrawn` * | Đã rút khỏi ngân hàng (BD301 V0.4; nguồn là `problem.withdrawn` của API-F4 #5, `DEC-2026-1024-dd-screens-users-core-contract-changes`) |
| `solutionReview.diff.added`, `diff.removed` * | thêm, bỏ (chữ ẩn cho trình đọc màn hình, mục 6) |

Nhãn UI không chứa tên endpoint, mã quyền hay mã `AIR-nnn` (`03-dd/screens/README.md:48`); mã chỉ nằm trong bảng ánh xạ.

## 4. Quy tắc đặc thù màn (`BR-SC-nn`)

Chỉ các việc không tự suy ra được hoặc trải qua nhiều sự kiện. Mọi quy tắc phía máy chủ (hạn mức, ngân sách, cache theo hash, lược đồ) đã ở LOG-F5 và không lặp lại ở đây.

### BR-SC-01 Nạp báo cáo một lần, không tự gọi lại ngầm

- **Kích hoạt**: mở route (EVT-1) hoặc bấm Thử lại (EVT-7).
- **Tiền điều kiện**: có `submissionId` ở route; đã đăng nhập.
- **Các bước**:
  1. `useSolutionReview` cấu hình `retry: false`, `staleTime: Infinity`, `refetchOnWindowFocus: false`, `refetchOnReconnect: false`. Lý do: báo cáo của một bài nộp là bất biến (không có thao tác "sinh lại", BD301 Sheet 9 NO 8), còn một lần gọi thất bại khi chưa có báo cáo có thể tốn token và đếm vào hạn mức giờ; chỉ người học được quyết định gọi lại (API-F5 Bảng 4, `03-dd/api/F5_ai-review.md:206`: "không thử lại tự động"). `useMockQuery` hiện đặt `staleTime = 30_000` mặc định (`shared/api/mock-query.ts:33-47`); khi nối thật phải truyền `staleTime: Infinity`.
  2. `useSubmissionDetail(submissionId)` chạy song song. Báo cáo chỉ hiện khi F5 xong; F4 thường xong trước nên không kéo dài thêm.
  3. `queryFn` của F5 gắn hạn **40 giây** phía máy khách (`AbortSignal.timeout`): máy chủ chờ LLM 30 giây (API-F5 §3.2) cộng tối đa hai lần chờ cổng 3 giây (`03-dd/api/F5_ai-review.md:102-103`). Số 40 là `[SoT: Suy luận]` từ cộng hạn máy chủ cộng đệm; quá hạn coi là lỗi tạm thời.
  4. Nhận `AIR-103` (báo cáo của bài nộp này đang được sinh bởi lời gọi khác): **không** hiện lỗi; chờ 3 giây rồi gọi lại, tối đa 10 lần (khoảng 30 giây, bằng hạn LLM), trong vòng lặp bên trong `queryFn` để `isFetching` luôn `true` và skeleton không nhấp nháy. Hết 10 lần thì coi là lỗi tạm thời. Căn cứ: API-F5 §4.4 dòng 272 "client chờ 3 giây rồi gọi lại (idempotent)". Tình huống thực tế gặp `AIR-103`: mở hai tab, hoặc chế độ phát triển của React gọi `queryFn` hai lần (`StrictMode`).
  5. Rời màn khi đang chờ: huỷ `fetch` bằng `signal` của TanStack Query. Máy chủ **có thể** vẫn ghi báo cáo (LOG-F5 BR-AI-02 bước 11 là một giao dịch ngắn sau khi gọi LLM `[SoT: Suy luận]`, ngắt kết nối không nói rõ có huỷ lời gọi LLM không); lần mở sau trả báo cáo đã lưu tức thì. Không có hộp thoại cảnh báo khi rời (không có dữ liệu người dùng chưa lưu).
- **Hậu điều kiện**: đúng một kết quả hiển thị: Ready, hoặc đúng một trong các khối lỗi ở Bảng 3.1; mỗi lần thất bại sinh đúng một toast.

### BR-SC-02 Phân loại lỗi và Thử lại

- **Kích hoạt**: `query.isError`, hoặc bấm Thử lại.
- **Các bước**:
  1. Phân loại theo Bảng 3.2 (`error.code`, không theo `message`).
  2. Mỗi lần thất bại (thay đổi `errorUpdatedAt`, `solution-review-view.tsx:107-114`) hiện đúng một toast theo cột "Toast"; nếu thử lại thất bại nữa thì có toast mới. Khối lỗi nằm thay chỗ báo cáo nên giữ nguyên (BD301 Sheet 8 EVT-1).
  3. `AIR-401`: không nút Thử lại. Nếu người dùng quay lại sau khi admin mở khoá, mở màn lại gọi lại bình thường (không lưu trạng thái khoá phía máy khách).
  4. `AIR-402`, `AIR-404` (trạng thái chờ giới hạn tần suất, chỉ chữ và đồng hồ đếm ngược): đọc `error.details.retryAfterSeconds` (API-F5 §4.4, `03-dd/api/F5_ai-review.md:274-275`). Nút Thử lại `disabled` cho tới khi đếm ngược về 0. Đếm ngược cập nhật mỗi giây nhưng **không** nằm trong vùng `aria-live` (tránh đọc mỗi giây); chỉ khi nút mở lại thì nút đó đổi nhãn từ `retryIn` về `retry`. Nếu `retryAfterSeconds` thiếu, dùng 20 giây (cùng giá trị seed của `cooldownSeconds` ở `entities/ai-budget/model/ai-budget-store.ts:40-44`) `[SoT: Suy luận]`.
  5. Thử lại dùng `isFetching`, nên khối lỗi ẩn và skeleton hiện lại, không bấm đôi được (đã đúng ở prototype, `solution-review-view.tsx:91-93`).
- **Hậu điều kiện**: một lần bấm Thử lại tạo tối đa một lời gọi (cộng vòng chờ `AIR-103` của BR-SC-01).

### BR-SC-03 Áp bản đề xuất vào Workspace (F5-26)

- **Kích hoạt**: bấm "Áp dụng vào Workspace" (EVT-4), rồi xác nhận (EVT-6).
- **Tiền điều kiện**: `codeDiff.suggestedCode` khác `null`; có `problemId` và `language` từ F5; có `submissionModel` từ API-F4 #5 (không có thì nút tắt, mục "Một phần thất bại").
- **Các bước**:
  1. EVT-4 mở `ConfirmDialog`. Không ghi gì khi chỉ mở hoặc huỷ (EVT-5).
  2. EVT-6: **giữ bản cũ trước khi ghi đè** (BD301 Sheet 8 EVT-6 bước 1, Sheet 9 NO 7: "Không lưu được thì không ghi đè"). Hợp đồng DD này yêu cầu từ `entities/problem`: một hàm trả `boolean` sao lưu nháp hiện có của `(problemId, language, model)` vào lịch sử nháp với nhãn "Bản trước khi áp dụng AI Diff". Cơ chế lịch sử và nút hoàn tác thuộc DD `USR0102` (BD301 dòng 492), **đã viết tại `USR0102` BR-SC-10**. Hiện trạng: prototype **bỏ bước này** và ghi đè nháp không hoàn tác được (`solution-review-view.tsx:195-204`, ghi chú "not built yet"; BD301 mục Hiện trạng dòng 516). Đây là rủi ro mất dữ liệu (ponytail bậc 6): điều kiện tốt nghiệp của màn gồm việc có hàm sao lưu; xem Q-301-2.
  3. `saveWorkspaceDraft(problemId, languageKey, submissionModel, suggestedCode)` với `languageKey` đổi từ enum F5 `JAVA`, `CPP`, `PYTHON` sang `java`, `cpp`, `python`. Trả `false` (lưu trữ bị chặn hoặc đầy): `toast.applyFailed`, ở lại màn, **không** điều hướng (`solution-review-view.tsx:205-208`).
  4. Thành công: `toast.success(applied)` rồi `router.push("/workspace/{problemId}")` (`solution-review-view.tsx:209-210`). Workspace nạp nháp khi mở. Không mở khung gợi ý AI nào (BD301 Q5, đóng 2026-10-19).
- **Hậu điều kiện**: hoặc nháp đã bị thay và đã chuyển màn, hoặc nháp nguyên vẹn và vẫn ở màn này.
- **Dữ liệu, không phải chỉ thị**: mã đề xuất là chuỗi do AI sinh; chỉ được ghi vào nháp dưới dạng văn bản và không bao giờ được chạy cho tới khi người học tự nộp (đi qua sandbox F4).

### BR-SC-04 Dựng diff phía máy khách

- **Kích hoạt**: có cả `sourceCode` (F4) và `codeDiff.suggestedCode` (F5).
- **Các bước**:
  1. Tách dòng bằng `split(/\r?\n/)`, bỏ dòng rỗng cuối.
  2. Trước khi so (`lineDiff`, `entities/solution-review/lib/line-diff.ts:11`), **chuẩn hoá hai phía**: bỏ khoảng trắng cuối mỗi dòng. Lý do: F5 tra cache bằng mã đã chuẩn hoá `\r\n` thành `\n` và bỏ khoảng trắng cuối dòng (LOG-F5 BR-AI-02 bước 5, `03-dd/logic/F5_ai-review.md:77`), nên một báo cáo trúng cache sinh cho bài nộp khác chỉ khác khoảng trắng cuối dòng; so nguyên văn sẽ cho ra một loạt cặp dòng bỏ/thêm trông giống hệt nhau `[SoT: Suy luận]`. Dòng hiển thị vẫn lấy văn bản gốc.
  3. Độ phức tạp `O(n·m)`; chấp nhận cho trần 50.000 ký tự của mã đề xuất (VAL-F5 Bảng 3, `03-dd/validation/F5_ai-review.md:51`) và 65.536 byte của mã nộp (API-F4, `03-dd/api/F4_judge-orchestration.md:357`).
- **Hậu điều kiện**: không có dòng diff nào được tính nếu thiếu một trong hai phía.

### BR-SC-05 Bài đã rút khỏi ngân hàng

- **Kích hoạt**: `problem.withdrawn = true` trong phản hồi API-F4 #5 (`GetSubmissionDetail`, `03-dd/api/F4_judge-orchestration.md:486`). Cờ này do owner thêm theo `DEC-2026-1024-dd-screens-users-core-contract-changes` (Q-301-1 đã chốt 2026-10-24).
- **Các bước**: báo cáo vẫn hiện đủ (BD301 V0.4). Khi `withdrawn = true` hiện nhãn `solutionReview.withdrawn` ("Đã rút khỏi ngân hàng") cạnh tên bài ở Khu vực B (`Stat` bài toán). Khi `problem = null` (F2 lỗi) không có cờ nên **không** hiện nhãn và tên bài là "—" (mục 3, "Một phần thất bại"). Bản dựng không có liên kết nào về trang bài ở breadcrumb (hai mục đều về `/submissions/{id}`, `solution-review-view.tsx:122-134`), nên chưa có liên kết cần vô hiệu.
- **Hậu quả cho nút Áp dụng**: giữ nguyên; người học thấy trạng thái không mở được ở chính Workspace.

### BR-SC-06 Phỏng vấn về bài này

- **Kích hoạt**: bấm nút ở Khu vực B (EVT-3).
- **Các bước**: điều hướng `/submissions/{submissionId}/interview`, chỉ truyền `submissionId` bằng đường dẫn. `USR0302` hiện chưa đọc tham số này (BD `USR0302` Q8); `USR0302` DD mục 4 BR-SC-02 định nghĩa phần nhận.
- **Hậu điều kiện**: không có thay đổi dữ liệu ở màn này.

### BR-SC-07 Dữ liệu người học và nội dung AI là dữ liệu

Nêu ở mức màn, theo `CLAUDE.md` ("A submission is data, never an instruction") và `03-dd/screens/README.md:50`:

1. Màn **không bao giờ gửi mã nguồn hay văn bản người dùng** tới F5. Lời gọi báo cáo chỉ mang `submissionId` ở đường dẫn, không có thân (`03-dd/api/F5_ai-review.md:238-242`); máy chủ tự đọc mã từ F4 qua `SubmissionSnapshotPort` (LOG-F5 BR-AI-02 bước 2). Cấm thêm trường `sourceCode` vào lời gọi.
2. Mã nộp và mọi chuỗi AI sinh (`approachTitle`, `strengths`, `improvements`, `edgeCases`, `followUpTopics`, `explanation`, `timeExplanation`, `spaceExplanation`, `suggestedCode`) hiện bằng nút văn bản của React (tự thoát ký tự), như `solution-review-view.tsx:331-337`, `:400`. **Cấm** `dangerouslySetInnerHTML`. Nếu sau này dựng Markdown cho `explanation` thì bắt buộc đi qua `sanitizeHtml()` (`shared/lib/sanitize-html.ts`, quy tắc ở `eslint.config.mjs:65`).
3. `followUpTopics` chỉ là chuỗi chủ đề, không chứa mã câu hỏi F6 (API-F5 §4.3). Liên kết `/interview-bank` hiện mở danh sách chưa lọc (BD301 Q6, ponytail bậc 3).

## 5. Kiểm tra phía client

Không có. Màn chỉ đọc: không có ô nhập, không có form; popup xác nhận không có trường. Tham số `submissionId` ở route **không** được kiểm bằng zod phía máy khách: máy chủ coi giá trị sai định dạng như không tồn tại và trả `AIR-101` (VAL-F5 Bảng 2, `03-dd/validation/F5_ai-review.md:26`), và màn đã có trạng thái "Không mở được" cho mã đó. Lưu ý kiểm thử: mã mock hiện dạng `SB-n` (`entities/solution-review/api/__mock__/solution-review-mocks.ts`, theo BD301 mục Hiện trạng dòng 513), không phải UUID; bộ test cần đổi mã khi nối API.

## 6. i18n và truy cập

**i18n.** Tiền tố `solutionReview.` ở `05-coding/frontend/messages/vi.json:2067-2119` và `en.json:1943`; khoá mới ở Bảng 3.2. Khoá `backToWorkspace` hiện trỏ `/problems` với nhãn "Quay lại Ngân hàng bài toán" (`vi.json:2098`, `solution-review-view.tsx:172-177`) trong khi BD301 Sheet 8 EVT-1 nói lối thoát là `submission_result` hoặc Workspace: breadcrumb đã có liên kết về kết quả nộp nên giữ nguyên, ghi ở mục 8.

**A11y.** Chỉ ghi khác biệt so với `shared/ui`:

| Hạng mục | Quy tắc |
| :--- | :--- |
| Vùng chờ | `role="status"` kèm chữ ẩn `loadingLabel`; chỉ một vùng `status` đang chờ tại một thời điểm |
| Chuyển từ chờ sang lỗi hoặc báo cáo | `NoticeTile` đã có `role="status"` (`shared/ui/feedback/notice-tile.tsx:50`) nên thông báo lỗi được đọc lịch sự một lần; toast đồng thời (BD301 yêu cầu cả hai) có thể bị đọc hai lần: chấp nhận, ghi vào nợ kiểm thử a11y |
| Đếm ngược `retryIn` | Không nằm trong `aria-live` (xem BR-SC-02 bước 4) |
| Thẻ Diff | Khung `role="group"` có `aria-label` là `diffTitle`. Màu không là kênh duy nhất (đã có dấu `+` và `−`, `solution-review-view.tsx:56`), nhưng dấu `+` `−` bị `select-none` chứ không bị ẩn khỏi trình đọc; thêm chữ ẩn `diff.added`, `diff.removed` cho dòng thêm và dòng bỏ, dòng giữ nguyên không đọc gì thêm |
| Popup xác nhận | Dùng `ConfirmDialog` (`shared/ui/feedback/confirm-dialog.tsx:30`); chưa kiểm bẫy focus, thêm vào test a11y |
| Thứ tự focus | breadcrumb, nút Phỏng vấn, banner, bảng độ phức tạp, các thẻ, thẻ Diff, nút Áp dụng (theo thứ tự DOM của `ReadyReport`, `solution-review-view.tsx:243-433`); sau khi đóng popup trả focus về nút Áp dụng |
| Bàn phím | Không có điều khiển phức tạp |

## 7. Điểm kiểm thử

Bảng 7.1 phủ mọi sự kiện BD301 Sheet 8; công cụ theo `CLAUDE.md` (Vitest cho logic và view, Playwright cho luồng qua màn). Cột `AC-nn` trỏ `TDD-F5` (hành vi phía máy chủ) hoặc `TDD-SR` (phần xuyên module của màn).

**Bảng 7.1: Điểm kiểm thử màn**

| ID | Loại | Nội dung | Công cụ | AC-nn |
| :--- | :--- | :--- | :--- | :--- |
| TP-301-01 | Normal | EVT-1: mở route, thấy skeleton rồi báo cáo; banner giáo dục hiện | Vitest (`solution-review-view.test.tsx`) | AC-01, AC-14 |
| TP-301-02 | Normal | EVT-1: báo cáo đã có thì hiện ngay, không gọi sinh lại (kiểm số lần gọi `queryFn` bằng 1) | Vitest | AC-02, AC-03 |
| TP-301-03 | Abnormal | EVT-1: `AIR-501` hiện `NoticeTile` kèm Thử lại và đúng một toast | Vitest | AC-09 |
| TP-301-04 | Abnormal | EVT-1: `AIR-502` xử lý như `AIR-501` | Vitest | AC-10 |
| TP-301-05 | Abnormal | EVT-1: `AIR-401` hiện khối khoá, không có nút Thử lại | Vitest | AC-08 |
| TP-301-06 | Abnormal | EVT-1: `AIR-101`, `AIR-302`, `AIR-102` cùng hiện một khối "Không mở được", không Thử lại, không lộ nguyên nhân | Vitest | AC-05 |
| TP-301-07 | Boundary | EVT-1: `AIR-402` với `retryAfterSeconds = 5`: nút Thử lại tắt 5 giây rồi mở (đồng hồ giả); khối có chữ lý do và số giây, không có nút nào khác | Vitest | AC-06, AC-SR-06 |
| TP-301-08 | Boundary | `AIR-103` ba lần liên tiếp rồi thành công: chỉ skeleton, không toast; sau 10 lần: lỗi tạm thời | Vitest (đồng hồ giả) | AC-13 |
| TP-301-09 | Normal | EVT-7: Thử lại ẩn khối lỗi, hiện skeleton, không bấm đôi được | Vitest | AC-09 |
| TP-301-10 | Normal | Không bao giờ gọi lại ngầm: đổi tab và quay lại, mất mạng rồi có lại mạng không sinh lời gọi mới | Vitest | AC-SR-02 |
| TP-301-11 | Boundary | `optimalTime`, `optimalSpace` null hiện "—"; `codeDiff` null ẩn thẻ Diff và nút Áp dụng; `followUpTopics` rỗng ẩn thẻ | Vitest | AC-11 |
| TP-301-12 | Normal | EVT-2: hai mục breadcrumb dẫn `/submissions/{id}` | Vitest | AC-SR-01 |
| TP-301-13 | Normal | EVT-3: nút Phỏng vấn dẫn `/submissions/{id}/interview` | Vitest | AC-SR-01 |
| TP-301-14 | Normal | EVT-4, EVT-5: mở rồi huỷ popup không đổi `localStorage` | Vitest | AC-SR-03 |
| TP-301-15 | Normal | EVT-6: xác nhận ghi nháp đúng khoá `algoprep-draft:{problemId}:{language}:{model}` rồi chuyển `/workspace/{problemId}` | Vitest, Playwright | AC-SR-03 |
| TP-301-16 | Abnormal | EVT-6: `saveWorkspaceDraft` trả `false` thì toast lỗi, ở lại màn, nháp cũ nguyên vẹn | Vitest | AC-SR-04 |
| TP-301-17 | Boundary | EVT-6: bản cũ được sao lưu trước khi ghi đè; sao lưu thất bại thì không ghi đè (theo `USR0102` BR-SC-10) | Vitest | AC-SR-04 |
| TP-301-18 | Abnormal | F4 lỗi: báo cáo vẫn hiện, thẻ Diff ẩn, nút Áp dụng tắt, có `sourceUnavailable` | Vitest | AC-SR-05 |
| TP-301-19 | Boundary | Báo cáo trúng cache khác khoảng trắng cuối dòng: diff không hiện cặp dòng giả | Vitest (`line-diff.test.ts`) | AC-SR-05 |
| TP-301-20 | Security | Chuỗi AI chứa `<img onerror>` hiện dạng chữ, không tạo phần tử | Vitest | AC-12 |
| TP-301-21 | Security | Lời gọi báo cáo không có thân và không có `sourceCode` | Vitest | AC-12 |
| TP-301-22 | Normal | Giao diện `en`: mọi khoá mới có mặt ở `en.json` (`shared/i18n/messages.test.ts`) | Vitest | TDD màn `AC-SR-08` |
| TP-301-23 | Boundary | EVT-1: `AIR-404` với `retryAfterSeconds = 20`: cùng khối chờ, nút Thử lại mở đúng khi đếm ngược về 0; đếm ngược không nằm trong vùng `aria-live`; thiếu `retryAfterSeconds` thì dùng 20 giây | Vitest (đồng hồ giả) | AC-07, AC-SR-06 |
| TP-301-24 | Normal | `problem.withdrawn = true` hiện nhãn "Đã rút khỏi ngân hàng" cạnh tên bài, báo cáo vẫn đủ; `withdrawn = false` hoặc `problem = null` không hiện nhãn | Vitest | TDD màn `AC-SR-07` |

## 8. Nợ prototype và câu hỏi mở

### 8.1 Dòng nợ trong `DEBT`

Dòng `views/solution-review` + `entities/solution-review` ở mục 9.1 của `DEBT` (dòng 887) và dòng `USR0301_solution_review` ở mục 10.2 (dòng 966; mục 10 đã bị mục 9.1 đảo ngược). Điều kiện tốt nghiệp ghi ở dòng 887 là: "Có DD `ai-review` ... → thay `queries.ts` gọi API thật → test". DD màn này xong **phần tài liệu**, nhưng việc thay `queries.ts` và test chưa làm, và còn nợ triển khai bước sao lưu `USR0102` BR-SC-10. Vì vậy **chưa gỡ dòng**; báo cáo kèm theo nêu phần còn thiếu.

### 8.2 Điều kiện nối API thật (kiểm khi làm)

1. Thay `queries.ts` theo mục 1 (kiểu F5 thu gọn, ghép F4 ở view).
2. Thay mã lỗi theo Bảng 3.2, bỏ `?demo=`.
3. `useSolutionReview` đặt `retry: false`, `staleTime: Infinity` (BR-SC-01).
4. Bước sao lưu bản cũ (BR-SC-03) và nhãn bài đã rút theo `problem.withdrawn` (BR-SC-05, đã chốt).
5. `pnpm check` xanh (G-CHECK trong `CLAUDE.md`).

### 8.3 Câu hỏi mở mức màn

Bảng 8.1 liệt kê câu hỏi, mặc định DD đề xuất và bậc ponytail (`.claude/rules/ponytail-criteria.md`).

**Bảng 8.1: Câu hỏi mở của màn**

| # | Câu hỏi | Mặc định đề xuất | Bậc ponytail |
| :-: | :--- | :--- | :-: |
| Q-301-1 | ~~Nhãn "Đã rút khỏi ngân hàng" không có nguồn dữ liệu.~~ **Đã chốt 2026-10-24 (`DEC-2026-1024-dd-screens-users-core-contract-changes`):** API-F4 #5 có `problem.withdrawn: boolean` (`03-dd/api/F4_judge-orchestration.md:486`); màn hiện nhãn từ cờ đó | Đóng | 1, 2 |
| Q-301-2 | Bước sao lưu bản cũ trước khi áp dụng (F5-26) cần hàm của `entities/problem` mà DD `USR0102` BR-SC-10 đã định nghĩa. Làm gì trong lúc chờ? | Giữ nguyên cho tới khi DD `USR0102` xong; đánh dấu là điều kiện tốt nghiệp; nếu demo trước thì thêm một dòng cảnh báo trong popup "Mã nháp hiện tại sẽ bị thay thế" (chữ có sẵn: `applyConfirmBody`, `vi.json:2100`) | 6 (mất dữ liệu người học) |
| Q-301-3 | ~~Thêm trạng thái "Chờ giới hạn tần suất" (`AIR-402`, `AIR-404`)?~~ **Đã chốt 2026-10-24 (`DEC-2026-1024-dd-screens-ai-topic-length-and-rd-trim`):** là trạng thái bình thường của màn, chỉ chữ và đồng hồ đếm ngược, không endpoint mới; BD301 V1.9 thêm item Khu vực I NO 5; `AC-SR-06` | Đóng | 1 |
| Q-301-4 | Chữ khoá ngân sách ở BD301 Sheet 9 NO 5 ("giới hạn phân tích AI trong ngày", `vi.json:2097`) sai đơn vị: ngân sách tính theo chu kỳ tháng (LOG-F5 BR-AI-05 bước 10, `03-dd/logic/F5_ai-review.md:135`). Sửa chữ bỏ "trong ngày"? | Sửa thành "trong kỳ này" ở cả `vi.json` và `en.json`; không đổi logic | 2 (không hiện giá trị sai) |
| Q-301-5 | Mở thẳng URL `/review` của bài chưa có báo cáo là **sinh ngay** (tốn token, đếm hạn mức giờ). Có cần nút xác nhận "Tạo phân tích" trước khi gọi, hay giữ hành vi BD301 EVT-1? | Giữ BD301 EVT-1 (sinh ngay): người học đã bấm "Phân tích bài giải" ở màn trước (API-F5 §4.1: hành động chủ động) | 1 (BD301 đã quy định), 3 (không có cảnh demo cần bước thừa) |
| Q-301-6 | Hạn 40 giây phía máy khách và 10 lần chờ `AIR-103` là số suy luận. Chốt hay đổi? | Giữ 40 giây và 10 lần, hằng số trong `entities/solution-review` | 5 (hằng số mặc định) |

### 8.4 Sai lệch phát hiện ở tài liệu và mã khác (DD này không sửa)

Bảng 8.2 liệt kê sai lệch; mỗi dòng nêu cách xử lý đề xuất. Không dòng nào buộc ghi `DEC-` ngay, trừ ghi chú.

**Bảng 8.2: Sai lệch**

| # | Nơi | Sai lệch | Đề xuất |
| :-: | :--- | :--- | :--- |
| D-301-1 | `entities/solution-review/model/review-errors.ts:3-9` | Mã lỗi mock (`AI_BUDGET_LOCKED`, `AI_PROVIDER_TIMEOUT`, `NOT_FOUND`, `FORBIDDEN`, `NOT_ACCEPTED`) khác mã thật `AIR-nnn` | Thay theo Bảng 3.2 khi nối |
| D-301-2 | BD301 Sheet 7.3 NO 2 | Tên `GetSubmissionSourceCode` không có endpoint đúng tên; thực tế là `GetSubmissionDetail` (API-F4 #5), như DD F5 đã ghi (`03-dd/api/F5_ai-review.md:78`) | Sửa chữ Sheet 7.3 |
| D-301-3 | BD301 Sheet 8 EVT-6 | Toast thành công BD ghi "Đã áp dụng gợi ý vào bài làm." còn `vi.json:2106` là "Đã ghi đè mã nháp bằng bản đề xuất. Đang mở Workspace." | Chọn một; đề xuất giữ chữ bản dựng (nói rõ hành vi) và sửa BD |
| D-301-4 | BD301 Sheet 7.2 NO 2, 3, 4 | Liệt kê màn đọc `judge.submissions`, `problem.problems`, `problem.problem_levels`; thực tế F5 không đọc các bảng này (API-F5 §4.3 không trả `problemTitle`, `levelCode`) và màn lấy chữ từ API-F4 #5 | Sửa BD Sheet 7.2 theo API-F4 #5 |
| D-301-5 | BD301 Sheet 9 NO 4, 5 | Hai chữ "Hệ thống AI đang bận..." và "... trong ngày..." trùng với `vi.json` nhưng chưa có mã thông báo; mã nay đã có (`AIR-501`, `AIR-401`) | Điền cột "Mã thông báo" theo Bảng 3.2 |
| D-301-6 | `solution-review-view.tsx:172-177` | Khối khoá ngân sách dẫn `/problems`, BD nói `submission_result` hoặc Workspace | Giữ (breadcrumb đã về kết quả); ghi nhận |
| D-301-7 | `shared/ui/feedback/notice-tile.tsx:7-9`, `:50` | Chú thích nói màn đẩy thông báo giữa phiên "nên truyền role riêng" nhưng `NoticeTile` **không có** prop `role` | Nâng cấp `NoticeTile` thêm prop `role` tuỳ chọn (mặc định `status`, người gọi cũ không đổi); cần cho `USR0302` (mục 1 của DD đó) |
