# DD màn hình — Chi tiết bài tập / Workspace giải bài (`USR0102`)

> Tuân khung `03-dd/screens/README.md` mục 4 (chín mục 0 đến 8) và quy tắc cứng mục 5. Màn này chỉ ghi hợp đồng xuyên biên giới và quyết định kỹ thuật không tự suy ra được; request, response, mã lỗi nằm ở file API của module và chỉ được liệt kê theo ID. Không có logic phía backend ở đây.

## 0. Thông tin tài liệu

Bảng 0.1: Bìa tài liệu.

| Mục | Giá trị |
| :--- | :--- |
| Mã màn, slug | `USR0102`, `problem_detail` |
| Tên màn | Chi tiết bài tập (Workspace giải bài) |
| Nhóm actor | `users`, A1 (`STUDENT`) |
| Route | `/workspace/{problemId}` (`problemId` là UUID, `DEC-2026-1024-dd-screens-users-core-contract-changes`) |
| Bounded Context đứng sau | F2 `problem-bank`, F3 `harness`, F4 `judge-orchestration`, đọc thêm F5 `ai-review` |
| RD nguồn | `01-rd/screens/users/USR0102_problem_detail.md` |
| BD nguồn | `02-bd/screens/users/USR0102_problem_detail.md` (V0.14) |
| Prototype | `05-coding/frontend/src/views/users/problem-detail/` (chưa có STOMP thật, toàn bộ API mock) |
| Phiên bản, ngày | V0.1, 2026-10-23 |
| Tiêu chí nghiệm thu | `04-tdd/screens/users/USR0102_problem_detail.md` (luồng xuyên module) cộng các `AC` module ghi ở mục 7 |

Bảng 0.2: Lịch sử sửa.

| Ver | Mục | Nội dung | Ngày |
| :--- | :--- | :--- | :--- |
| V0.1 | Toàn bộ | Tạo mới từ prototype đã dựng và DD của F2, F3, F4, F5. Chốt cách nối mock sang endpoint thật, hợp đồng nối lại realtime ở mức màn, danh sách lệch cần chủ dự án quyết | 2026-10-23 |

Bảng 0.3: Viết tắt nguồn trích dẫn dùng trong file này.

| Viết tắt | Đường dẫn |
| :--- | :--- |
| RD | `01-rd/screens/users/USR0102_problem_detail.md` |
| BD | `02-bd/screens/users/USR0102_problem_detail.md` |
| F2, F3, F4, F5 | `03-dd/api/F2_problem-bank.md`, `F3_harness.md`, `F4_judge-orchestration.md`, `F5_ai-review.md` |
| V4 | `03-dd/validation/F4_judge-orchestration.md` |
| L4 | `03-dd/logic/F4_judge-orchestration.md` |
| API | `03-dd/api/api.md` |
| PV | `05-coding/frontend/src/views/users/problem-detail/ui/problem-detail-view.tsx` |
| PDLG | `05-coding/frontend/src/views/users/problem-detail/ui/run-results-dialog.tsx` |
| EP | `05-coding/frontend/src/entities/problem/` |
| CE | `05-coding/frontend/src/shared/ui/data/code-editor.tsx` |
| DEBT | `06-plan/PROTOTYPE_DEBT.md` |

## 1. Bản đồ slice FSD

Thứ tự import bắt buộc `app → views → widgets → features → entities → shared`; `eslint.config` chặn ngược chiều. Bảng 1.1 là bản đồ đích; cột "Hiện trạng" ghi prototype đang ở đâu.

Bảng 1.1: Slice của màn.

| Lớp | Đường dẫn (từ `src/`) | Vai trò | Hiện trạng |
| :--- | :--- | :--- | :--- |
| app | `app/(student)/workspace/[problemId]/page.tsx` | Chỉ render `ProblemDetailView` | Đã có [SoT: BD:385] |
| views | `views/users/problem-detail/ui/problem-detail-view.tsx` | Ghép ba vùng (đề, mã, kết quả) và hai `SplitPane`; giữ state màn | Có, 880 dòng, chứa mọi thứ [SoT: PV:119-879] |
| views | `views/users/problem-detail/ui/run-results-dialog.tsx` | Màn phụ liệt kê từng testcase (EVT-14) | Có [SoT: PDLG:25-163] |
| views | `views/users/problem-detail/model/use-workspace-state.ts` | Gom state cặp (ngôn ngữ, mô hình, phương thức nhập), nháp, đồng bộ URL | Chưa có, tách ra khỏi view khi nối API (mục 4 BR-SC-02) |
| entities | `entities/problem/api/queries.ts` | `useWorkspaceData` (gom 6 query của EVT-1), `useStarterCode`, `useSampleTestcases`, `useRunSample`, `useCreateSubmission`, `useToggleBookmark` | Mới có `useProblemDetail`, `useRunSampleTestcases`, `useCreateSubmission` mock [SoT: EP api/queries.ts:48-83] |
| entities | `entities/problem/model/submit-schema.ts` | zod của Run Code và Submit (mục 5) | Chưa có |
| entities | `entities/problem/model/workspace-draft.ts` | Khoá và ghi nháp `localStorage` | Có [SoT: BD:395] |
| entities | `entities/submission/api/use-submission-progress.ts` | Hook "đăng ký rồi chụp ảnh" cho một `submissionId` (BR-SC-06); **dùng chung với `USR0201`** | Chưa có |
| entities | `entities/submission/model/progress-reducer.ts` | Hàm thuần áp bản tin theo `seq` (F4:138-144) | Chưa có |
| shared | `shared/realtime/stomp-client.ts` | Khách STOMP (kết nối, đăng ký, nối lại có lùi), không biết nghiệp vụ | Chưa có [SoT: `05-coding/frontend/src/shared/realtime/README.md:6-9`] |
| shared | `shared/api` (`useErrorMessage`) | Dịch `error.code` sang chuỗi i18n | Chưa có, comment đã hẹn [SoT: `05-coding/frontend/src/shared/api/api-error.ts:1-3`] |

Thành phần dùng lại từ `shared/`: `SplitPane`, `SegmentedTabs`, `Modal`, `Badge`, `Button`, `Card`, `EmptyState`, `QueryBoundary`, `MarkdownPreview`, `SeeAllLink`, `CodeEditor` (`shared/ui`), `formatDateVN`, `toast` (`shared/lib`), `useMockQuery`, `useMockMutation`, `ApiError`, `tokenStore` (`shared/api`), `useT`, `useLocale` (`shared/i18n`) [SoT: PV:30-62].

Nâng lên `shared/` trước khi nối API (luật "Frontend: shared first"):

- **S1 `CodeEditor` thêm thuộc tính `readOnly`.** Màn cần khoá ô soạn trong lúc chấm (BD Sheet 6 Khu vực H NO 1) và `USR0201` cần trình xem mã chỉ đọc; `CodeEditor` hiện không có [SoT: CE:76-90; BD:411]. Hai màn dùng, nên thuộc `shared`.
- **S2 `shared/realtime/stomp-client.ts`** (xem Bảng 1.1). Thư viện STOMP đã chọn là `@stomp/stompjs` (`DEC-2026-1024-dd-screens-users-core-contract-changes`); chưa có trong `package.json` [SoT: `05-coding/frontend/package.json:19-35`], thêm khi dựng `stomp-client.ts`.
- **S3 `useErrorMessage`.** Mọi màn cần map `error.code`; viết một chỗ [SoT: API:22].

**Lệch có chủ ý so với BD 4.5.** BD đề xuất `widgets/problem-taskbar`, `widgets/problem-info-panel`, `widgets/judge-console`, `features/code-toolbar`, `features/source-file-upload`, `features/submit-solution`, `shared/api/stomp` [SoT: BD:429-441]. DD giữ **một view slice** có các component panel nằm trong `ui/`, vì từng khối chỉ có một nơi dùng (`CLAUDE.md`: một nơi dùng thì ở lại slice). Chỉ hai thứ có hai nơi dùng được nâng lên: hook theo dõi tiến trình (cùng `USR0201`) vào `entities/submission`, STOMP vào `shared/realtime` (đúng chỗ `README.md` của thư mục đã hẹn, không phải `shared/api/stomp`).

## 2. API tiêu thụ

Bảng 2.1 liệt kê, theo từng sự kiện BD, endpoint (theo mục của file API), hook và trạng thái nối. Cột "Nối" là `mock` nếu prototype đang giả lập, `thật` khi đã thay. Hiện **mọi dòng đều mock** [SoT: PV:1-17].

Bảng 2.1: Sự kiện, endpoint, hook.

| EVT (BD) | Endpoint theo ID | Hook (`entities/*/api/`) | Nối |
| :--- | :--- | :--- | :--- |
| EVT-1 | F2 mục 6 `GetProblemForWorkspace` | `useWorkspaceData` → `useProblemDetail` | mock [SoT: EP api/queries.ts:48-54] |
| EVT-1 | F2 mục 8 `ListSampleTestcases` | `useSampleTestcases` | mock (gộp trong `ProblemDetail.sampleTestcases`) |
| EVT-1, 4, 5 | F3 mục 3 `GetStarterCode` | `useStarterCode` (khoá theo `(problemId, language, submissionModel)`) | mock (`ProblemDetail.starterCode`) |
| EVT-1 | F4 mục 4 `GetEffectiveExecutionLimits` | `useExecutionLimits` (một lần, trả cả ba ngôn ngữ nên EVT-4 không gọi lại) | mock (`limitsByLanguage`) |
| EVT-1, 3, 12 | F4 mục 7 `ListMySubmissionsForProblem` | `useMySubmissionsForProblem` | mock (`mySubmissions`) |
| EVT-1 | F5 mục 6 `GetMySolutionReviewQuickInsight` | `useSolutionReviewQuickInsight` (query **riêng**, BD Sheet 7.1 NO 21) | mock (`solutionReview`) |
| EVT-10 | F4 mục 5 `RunSampleTestcases` | `useRunSampleTestcases` | mock [SoT: EP api/queries.ts:70-74] |
| EVT-11 | F4 mục 6 `CreateSubmission` | `useCreateSubmission` | mock [SoT: EP api/queries.ts:79-83] |
| EVT-11, 12 | F4 mục 2 kênh `/user/queue/submissions` (W1) | `useSubmissionProgress` (đăng ký) | chưa có |
| EVT-11, 12 | F4 mục 9 `GET /submissions/{submissionId}/progress` (ảnh chụp) | `useSubmissionProgress` (nối lại, dự phòng) | chưa có |
| EVT-15 | F2 mục 12 `ToggleMyProblemBookmark` | `useToggleBookmark` | **chưa gọi gì**, state cục bộ [SoT: PV:291-301] |
| EVT-16, EVT-17, EVT-18, EVT-19, EVT-22 | không gọi API; EVT-17/18 mở F5 ở màn đích, không tự kích hoạt | điều hướng `next/link` | không áp dụng |
| EVT-3, 6, 7, 8, 9, 13, 14, 20 | không gọi API | state màn | không áp dụng |
| EVT-21 (đã bỏ ở BD) | không có hành vi; nháp tự lưu vào `localStorage` | Không dựng hộp xác nhận rời màn | không áp dụng |

Kênh realtime: hợp đồng đầy đủ (đích, loại bản tin, thứ tự, nối lại) ở F4 mục 2.1 đến 2.5; file này **không** chép schema bản tin. Màn chỉ đọc bốn việc từ đó: đích duy nhất `/user/queue/submissions`, mỗi bản tin mang `submissionId` và `seq`, bản tin chỉ có `order` và `verdict` cho từng testcase, bản `isFinal` luôn được áp [SoT: F4:98-144].

Hai điểm khác BD khi nối thật, ghi để người nối API không tìm sai:

1. BD EVT-4 ghi gọi lại `GetEffectiveExecutionLimits` khi đổi ngôn ngữ [SoT: BD:796]; F4 trả cả ba ngôn ngữ trong một lần nên **không gọi lại** [SoT: F4:210, 1407].
2. Kết quả chạy thử không chứa input và kết quả mong đợi, chỉ có `testcaseId`; màn ghép với danh sách Sample đã tải ở EVT-1 [SoT: F4:286]. Ghép bằng `testcaseId`: F2 mục 8 trả `testcaseId` ở mỗi phần tử Sample (`DEC-2026-1024-dd-screens-users-core-contract-changes`) [SoT: F2:547].

## 3. Trạng thái màn

Bảng 3.1 liệt kê trạng thái theo vùng. Cột "Khoá i18n" có tiền tố `problemDetail.` (hoặc `apiError.` cho `error.code`; frontend không đọc `message`, API:22). Mã lỗi lấy từ F2 `PRB-nnn`, F3 `HRN-nnn`, F4 mục 27 `JDG-nnn`.

Bảng 3.1: Trạng thái theo vùng.

| Vùng | Trạng thái | Điều kiện | Hiển thị | Khoá i18n |
| :--- | :--- | :--- | :--- | :--- |
| Toàn màn | Đang tải | `GetProblemForWorkspace` chưa xong | Khung chờ cho ba vùng; Run Code và Submit chưa bật | `QueryBoundary` mặc định |
| Toàn màn | Rỗng (không tìm thấy) | `PRB-010` (không có, chưa xuất bản, đã ẩn mềm) | "Không tìm thấy bài toán." kèm liên kết về `/problems`; **không** nói "không có quyền" [SoT: BD:830] | `empty.title`, `empty.linkList` |
| Toàn màn | Lỗi | Lỗi khác của `GetProblemForWorkspace` | Vùng lỗi toàn màn, nút thử lại [SoT: BD:793] | `common.error` |
| Toàn màn | Chưa đăng nhập | `401` sau khi làm mới token thất bại | Chuyển về `auth` (do `shared/api/client.ts` xử lý, màn không tự xử) | không áp dụng |
| Toàn màn | Quyền bị từ chối | Tài khoản không phải `STUDENT` mở route | Chặn ở nhóm route `(student)`; màn không dựng UI `403` riêng `[SoT: Suy luận]` (BD chỉ yêu cầu bắt đăng nhập, Sheet 9 NO 1) | không áp dụng |
| Mã khung | Lỗi lúc mở | `GetStarterCode` lỗi (`HRN-001`, `HRN-003`, `HRN-006`, `5xx`) | Ô soạn mở **trống** và gõ được, kèm toast lỗi; Submit vẫn dùng được. `[SoT: Suy luận]`, BD chỉ nói trường hợp đổi cặp (BD:796) | `apiError.*` |
| Mã khung | Lỗi khi đổi cặp | như trên, sau EVT-4 hoặc EVT-5 | Giữ cặp cũ và mã cũ, toast lỗi [SoT: BD:796] | `apiError.*` |
| Giới hạn | Lỗi | `GetEffectiveExecutionLimits` lỗi | **Ẩn dòng giới hạn**, không hiện số khác. BD muốn hiện giới hạn cơ sở nhưng F2 không trả trường đó [SoT: BD:660; F2:462-463], xem OQ-3 | `toolbar.limits` |
| Tab Bài nộp | Đang tải | | 4 dòng chờ [SoT: BD:622] | |
| Tab Bài nộp | Rỗng | không có lượt nộp | "Bạn chưa nộp bài này lần nào." | `panel.noSubmissions` |
| Tab Bài nộp | Lỗi | `ListMySubmissionsForProblem` lỗi (`JDG-V14` không xảy ra vì `limit` cố định) | Lỗi riêng trong tab kèm nút thử lại; hai tab còn lại dùng được [SoT: BD:793] | `common.error` |
| Tab Solution Review | Ẩn | quick-insight lỗi, hoặc `review = null` | **Ẩn cả tab**, không báo lỗi [SoT: BD:593, 841; F5:379] | không áp dụng |
| Liên kết sang AI | Ẩn | `ACCEPTED` nhưng quick-insight lỗi | Ẩn "Phân tích bài giải" và "Phỏng vấn giả lập" (mục 4 BR-SC-09) | không áp dụng |
| Run Code | Đang chạy | từ lúc bấm đến khi có phản hồi | Màn phụ "Đang chạy...", hai nút khoá | `console.running` |
| Run Code | Lỗi biên dịch | `outcome = COMPILE_ERROR` (không phải lỗi HTTP) | Thông báo lỗi đã ánh xạ dòng, trong bảng kết quả | `console.*` |
| Run Code | Lỗi HTTP | `JDG-101`, `JDG-102`, `JDG-103`, `JDG-105`, `JDG-107`, `JDG-502`, `JDG-501` | Toast theo mã, mã nguồn giữ nguyên; `JDG-107` dùng `retryAfterSeconds` để nói lúc được thử lại | `apiError.JDG-xxx` |
| Submit | Đang chờ, biên dịch, chấm | `status` `PENDING`, `COMPILING`, `JUDGING` | Nhãn "Đang chờ", "Đang biên dịch", "Đang chấm" kèm dải ô | `verdict.pending`, `verdict.compiling`, `verdict.running` |
| Submit | Lỗi HTTP | `JDG-101`, `102`, `103`, `104`, `106`, `503` (`JDG-503`), `504` (`JDG-504`), `V05`, `V06` | Toast theo mã; `JDG-106` dùng `details.limit` để nói số lần; **không tự nộp lại** [SoT: F4 §6; BD Sheet 9] | `apiError.JDG-xxx` |
| Submit | Trạng thái cuối | sáu trạng thái cuối | Toast kết quả, nạp lại tab Bài nộp, hiện liên kết đi tiếp | `toast.submitAccepted`, `toast.submitRejected` |
| Submit | Cập nhật chậm | mất kênh, đã hỏi lại 10 lần mà chưa cuối | Một dòng nhắc "cập nhật đang chậm"; **không** kết luận trượt [SoT: F4:154; BD:804] | `console.slowUpdate` (mới) |
| Lưu bài | Lỗi | `PRB-001`, `PRB-010`, lỗi mạng | Hoàn nguyên nhãn, toast lỗi [SoT: BD:807] | `apiError.PRB-xxx` |
| AI suy giảm | F5 chết hoặc quá hạn | quick-insight lỗi | Ẩn tab Solution Review và hai liên kết; mọi chức năng F1 đến F4 chạy đủ [SoT: BD:841] | không áp dụng |

Bảng mã lỗi cần có chuỗi trong `messages/{vi,en}.json` mục `apiError`: `PRB-001`, `PRB-010`, `HRN-001`, `HRN-003`, `JDG-101`, `JDG-102`, `JDG-103`, `JDG-104`, `JDG-105`, `JDG-106`, `JDG-107`, `JDG-501`, `JDG-502`, `JDG-503`, `JDG-504`, `JDG-V05`, `JDG-V06`. Mã `JDG-V01`, `JDG-V02` có chuỗi riêng ở `toast.codeEmpty`, `toast.codeTooLarge` vì phía màn chặn trước [SoT: PV:274-289]; nếu máy chủ vẫn trả thì dùng chuỗi đó, không dựng chuỗi thứ hai.

## 4. Quy tắc đặc thù màn

Mười khối dưới là những việc đi qua nhiều sự kiện hoặc không tự suy ra được từ BD và file API. Đánh số cục bộ file này (`BR-SC-nn`).

### BR-SC-01 Khởi tạo song song và ghim mô hình

- **Kích hoạt:** EVT-1.
- **Tiền điều kiện:** có `problemId` trên đường dẫn; người dùng đã đăng nhập.
- **Các bước:**
  1. Chuẩn hoá `lang`, `model`, `tab` từ tham số truy vấn bằng zod `.catch(mặc định)`; giá trị lạ bị bỏ qua (BD:793 bước 3). Ngôn ngữ mặc định: giá trị đã nhớ ở `algoprep-workspace-prefs`, chưa có thì `PYTHON`; mô hình mặc định tương tự, `FUNCTION_WRAPPER` nếu chưa có [SoT: BD:527-528].
  2. Gọi song song F2 mục 6, F2 mục 8, F4 mục 4, F4 mục 7, F5 mục 6.
  3. `useStarterCode` **chờ** F2 mục 6 xong vì cần `functionWrapperSupported` để chốt `effectiveSubmissionModel`: `functionWrapperSupported = false` thì luôn `STANDARD_IO` bất kể giá trị đã nhớ [SoT: BD:793 bước 5; PV:160-161].
  4. Có bản nháp ở `localStorage` của cặp thì dùng bản nháp thay cho mã khung (BR-SC-02).
- **Hậu điều kiện:** năm query độc lập về lỗi. Chỉ F2 mục 6 lỗi mới chặn cả màn; mọi lỗi còn lại chỉ làm mất phần của mình (Bảng 3.1). Vùng soạn mã và nút Submit **không bao giờ** bị chặn vì module phụ chết [SoT: BD:793].

### BR-SC-02 Cặp (ngôn ngữ, mô hình), nháp và khoá khi đang chấm

- **Kích hoạt:** EVT-4, EVT-5, EVT-6, EVT-7.
- **Quy tắc:** khoá nháp là `algoprep-draft:{problemId}:{language}:{model}` (bài chỉ Standard I/O dùng `stdio`). Đổi cặp thì khôi phục nháp của cặp mới, không có nháp thì dùng mã khung từ `useStarterCode` (khoá cache theo cặp, mã khung tất định theo `specVersion` nên cache được [SoT: F3:223]). Không hỏi xác nhận ghi đè (BD Q14) vì mã cặp cũ còn trong nháp riêng [SoT: BD:873].
- **Tệp tải lên khi đổi ngôn ngữ:** tên tệp không khớp đuôi của ngôn ngữ mới thì bỏ tệp đã tải và đóng "Xem nội dung" [SoT: PV:208-219]. BD không nói điều này; giữ vì tệp sai đuôi sẽ bị `JDG-V06`.
- **Đổi phương thức nhập (EVT-6):** đổi sang "Tải file" giữ nguyên mã đã gõ; chọn mô hình hay ngôn ngữ **không** đổi phương thức nhập [SoT: BD:798, 646].
- **Khoá khi đang chấm** (BD Sheet 6 Khu vực G NO 2, 3 và Khu vực H NO 1): từ lúc có `submissionId` đến lúc `isFinal`, ba nhóm nút chọn ngôn ngữ, mô hình, phương thức nhập bị vô hiệu và `CodeEditor` ở `readOnly` (cần S1). Run Code cũng khoá khi có lượt nộp chưa cuối, Submit khoá khi lượt trước chưa cuối [SoT: BD:581-582, 644-645, 654]. Bản dựng chỉ khoá hai nút Run Code, Submit trong lúc `running` [SoT: PV:834-841].
- **URL:** `lang`, `model`, `tab` ghi lên query bằng `router.replace`, không sinh mục lịch sử [SoT: BD:779-787]; phương thức nhập và mã nguồn **không** vào URL. Bản dựng dùng `useState`, chưa ghi URL [SoT: PV:134-137; BD:388].

### BR-SC-03 Chuẩn bị nguồn gửi và kiểm phía trình duyệt

- **Kích hoạt:** EVT-8, EVT-10, EVT-11.
- **Tiền điều kiện:** có bài đã tải xong.
- **Các bước, theo thứ tự (khớp V4 bảng 2):**
  1. Chọn nguồn: `EDITOR` lấy mã trong ô soạn; `FILE_UPLOAD` lấy nội dung tệp đã đọc. `FILE_UPLOAD` mà chưa có tệp thì dừng, toast `toast.noFile`.
  2. Mã chỉ gồm khoảng trắng thì dừng, toast `toast.codeEmpty` (khớp `JDG-V01`).
  3. Đo bằng **byte UTF-8** (`TextEncoder`), quá 65.536 byte thì dừng, toast `toast.codeTooLarge` (khớp `JDG-V02`). Đúng 65.536 byte vẫn gửi [SoT: V4:34; PV:274-289].
  4. Khi chọn tệp (EVT-8), kiểm **đuôi** theo ngôn ngữ đang chọn rồi kiểm `file.size`, **trước khi đọc nội dung**; lỗi hiện toast, giữ tệp đã tải trước đó nếu có [SoT: BD:801; PV:238-260]. BD còn muốn viền vùng tải đổi đỏ; bản dựng chỉ có toast (nợ ở mục 8).
  5. Gửi: `CreateSubmission` mang thêm `sourceInputMethod` và, khi `FILE_UPLOAD`, `fileName` (máy chủ dùng để kiểm đuôi, không lưu) [SoT: F4:358-359; V4:37]. `RunSampleTestcases` bỏ qua hai trường này [SoT: V4:36].
- **Hậu điều kiện:** máy chủ vẫn là nơi kiểm thật; chuỗi lỗi trả về theo `error.code`, không theo vị trí kiểm.

### BR-SC-04 Chạy thử (Run Code)

- **Kích hoạt:** EVT-10.
- **Các bước:** (1) BR-SC-03; (2) chuyển bảng kết quả sang tab "Testcase", mở màn phụ "Đang chạy..." (BD Q16); (3) gọi F4 mục 5 một lần, **đồng bộ**, máy chủ có hạn 60 giây [SoT: F4:191]; hạn thời gian của axios phải lớn hơn, đặt 65 giây `[SoT: Suy luận]` để lỗi `JDG-501` của máy chủ luôn tới trước lỗi hết hạn của trình duyệt; (4) `outcome = COMPILE_ERROR` thì hiện `compileErrorMessage`; `COMPLETED` thì ghép từng `results[]` với Sample đã tải để lấy dữ liệu vào và kết quả mong đợi (F4:286, OQ-2), hiện `actualOutput`, verdict, thời gian, bộ nhớ.
- **Hậu điều kiện:** không tạo bản ghi, **không** làm mới tab Bài nộp, không đổi `my_progress` (F4-02; BD Q10). Người dùng không huỷ được lượt đang chạy (không có endpoint); rời màn thì huỷ yêu cầu phía trình duyệt (`AbortController`), kết quả muộn không toast sang màn khác [SoT: PV:167-168].
- **Dữ liệu hiển thị:** chỉ Sample. Chạy thử chỉ nhận testcase `SAMPLE` ngay tại truy vấn nên không có nội dung testcase ẩn nào để lộ (F4 mục 5.5 bước 2).

### BR-SC-05 Nộp bài, theo dõi tiến trình, nối lại (lõi realtime)

- **Kích hoạt:** EVT-11, EVT-12.
- **Tiền điều kiện:** BR-SC-03 đạt; không có lượt nộp nào của màn này đang ở trạng thái chưa cuối.
- **Hàm `attach(submissionId)`** dùng cho cả nộp mới, nối lại và mở lại màn: (a) đăng ký `/user/queue/submissions` **trước**; (b) rồi gọi F4 mục 9 (ảnh chụp); (c) dựng dải ô từ ảnh chụp; (d) áp mọi bản tin có `seq` lớn hơn `seq` của ảnh chụp. Đăng ký trước, chụp sau, để không có khe hở [SoT: F4:150-153].
- **Các bước nộp mới:**
  1. `CreateSubmission` trả `201` với `status = PENDING`; lưu `{submissionId, seq = 0}` làm lượt đang theo dõi.
  2. Dải ô dựng theo `totalCountHint` nếu có (ước lượng, số chính thức là `totalCount` trong bản tin `STATUS_CHANGED` khi vào `JUDGING`, F4:369); chưa có thì chưa vẽ ô.
  3. Chuyển bảng kết quả sang tab "Kết quả", mở màn phụ, gọi `attach`.
  4. Kết nối STOMP mở **lười** ở lần Submit đầu tiên của phiên màn, giữ đến khi rời màn (ponytail bậc 3: không mở khi chỉ đọc đề). Lỗi `CONNECT` thì rơi sang đường hỏi lại (bước 7).
  5. Áp bản tin theo quy tắc `seq` của F4 mục 2.4 bước 3: bỏ bản tin có `submissionId` khác lượt đang theo dõi; bản có `seq` thấp hơn hoặc bằng `seq` cuối thì bỏ, trừ bản `isFinal`.
  6. Mỗi `TESTCASE_FINISHED` tô một ô theo `order`, cập nhật "đạt/tổng" từ `completedCount`, `passedCount`, `totalCount`. **Chỉ `order` và `verdict` mô tả testcase**; ô hiển thị hai trạng thái Đạt (`AC`) hoặc Không đạt (mọi verdict khác), chú giải dạng "Testcase {n}: {trạng thái}" [SoT: F4:127; BD:831, 877].
  7. **Mất kết nối giữa chừng** (khung `ERROR`, đóng bất thường hoặc `disconnect`): lượt đang theo dõi chuyển `degraded`. Song song: nối lại STOMP có lùi tăng dần (vài lần); và hỏi F4 mục 9 mỗi **3 giây, tối đa 10 lần** [SoT: F4:154; BD:804]. Nối lại được thì chạy lại `attach` (đăng ký rồi chụp) và dừng hỏi. Hỏi đủ 10 lần mà chưa cuối thì hiện dòng "cập nhật đang chậm", **không** kết luận trượt.
  8. Ảnh chụp báo `isFinal` hoặc nhận `SUBMISSION_FINISHED`: dừng theo dõi; toast kết quả (Accepted là thành công, còn lại là lỗi kèm đạt/tổng); làm mới tab Bài nộp (F4 mục 7); hiện chỉ số `runtimeMs`, `memoryKb` từ bản tin cuối (ẩn ở `COMPILE_ERROR` và `SYSTEM_ERROR`, BD:672); bật lại điều khiển và ô soạn.
- **Hậu điều kiện:** không bao giờ suy ra "trượt" từ việc mất kênh; trạng thái thật luôn ở DB và tải lại trang là thấy [SoT: F4:154; `04-tdd/modules/F4_judge-orchestration.md` AC-28].
- **Lỗi tạo bài nộp:** không tự nộp lại khi lỗi mạng (không idempotent, F4:346). Nếu RabbitMQ không xác nhận sau ba lần, F4 kết thúc ngay lượt còn `PENDING` bằng `SYSTEM_ERROR/QUEUE_UNAVAILABLE` rồi trả `JDG-503` kèm `details.submissionId`; màn thông báo lỗi hệ thống và cho mở kết quả lượt đó. Người học có thể nộp lượt mới sau khi lỗi hạ tầng được khắc phục (F4 Q4).

### BR-SC-06 Mở lại màn khi còn lượt đang chấm

- **Kích hoạt:** EVT-1 hoàn tất mà `ListMySubmissionsForProblem` có dòng mới nhất ở `PENDING`, `COMPILING` hoặc `JUDGING`.
- **Các bước:** chạy `attach(submissionId)` cho dòng đó, chuyển bảng kết quả sang tab "Kết quả", khoá điều khiển như BR-SC-02.
- **Lý do:** BD yêu cầu Submit không kích hoạt khi lượt trước chưa cuối (Sheet 6 Khu vực A NO 5), mà trạng thái đó sau khi tải lại trang chỉ biết được từ danh sách lượt nộp. `[SoT: Suy luận]`, BD không nêu rõ việc tiếp tục theo dõi; ghi ở OQ-5.
- **Cạnh biên:** `JDG-503` không để lại lượt `PENDING`; nếu worker đã lấy message trước giao dịch kết thúc lỗi, F4 trả ticket `201` và màn theo dõi trạng thái thật. Hàng đợi tạm dừng (`queue_paused`) làm `PENDING` kéo dài, khi đó màn hết 10 lần hỏi và hiện dòng "chậm" (F4:396).

### BR-SC-07 Bật, tắt lưu bài (cập nhật lạc quan)

- **Kích hoạt:** EVT-15.
- **Các bước:** đổi nhãn và trạng thái nút ngay; gửi **trạng thái đích** (`bookmarked: true` hoặc `false`) chứ không gửi lệnh đảo, nên chạy lặp hay hai thiết bị đều không lệch [SoT: F2:725-729]; nút khoá trong lúc đang gửi; thành công thì toast ("Đã lưu bài" kèm hành động "Xem bài đã lưu" tới `/saved`, hoặc "Đã bỏ lưu bài"); lỗi thì hoàn nguyên nhãn và toast lỗi.
- **Nguồn ban đầu:** `GetProblemForWorkspace.bookmarked`. Prototype chỉ đổi state cục bộ, chưa gọi mutation [SoT: PV:291-301].

### BR-SC-08 Dữ liệu ngoài bảng hiển thị: testcase ẩn không bao giờ vào màn

- **Kích hoạt:** mọi nơi màn nhận dữ liệu testcase (EVT-10, EVT-11, EVT-12, EVT-14).
- **Quy tắc:** (1) tab Testcase và màn phụ Run Code chỉ nhận từ F2 mục 8 và F4 mục 5, đều chỉ là Sample; (2) mọi nguồn của lượt **nộp** (bản tin, ảnh chụp, danh sách lượt nộp) chỉ có `order`, `verdict`, số đếm và `firstFailingOrder`; (3) hàm ánh xạ của `entities/submission` **bỏ** mọi trường ngoài danh sách trên, kể cả khi máy chủ lỡ trả (phòng thủ chiều sâu, máy chủ vẫn là chốt chặn thật); (4) dải ô, chú giải, `aria-label`, toast không bao giờ chèn dữ liệu vào, kết quả mong đợi hay output; (5) câu hướng dẫn khi chưa chạy chỉ chèn **số** testcase ẩn (`console.runHint`), không kèm nội dung [SoT: BD:560, 831; F4:63; L4:230-240; RD:57].
- **Bài sai ở testcase ẩn:** màn phụ của Submit chỉ liệt kê Đạt hoặc Không đạt cho từng testcase, không mở chi tiết (BD Q17, đã chốt).

### BR-SC-09 Điều kiện hiện tab Solution Review và các liên kết đi tiếp

- **Kích hoạt:** EVT-1 (quick-insight) và EVT-12 (trạng thái cuối).
- **Quy tắc:**
  - Tab Solution Review hiện khi quick-insight thành công **và** `review != null`; ngược lại ẩn cả tab, không báo lỗi.
  - "Xem kết quả chấm" hiện sau **mọi** lượt nộp đã cuối, không riêng `ACCEPTED` [SoT: BD:674]. Bản dựng chỉ hiện khi `Accepted` [SoT: PV:780-801; PDLG:146-158], lệch BD.
  - "Phân tích bài giải" và "Phỏng vấn giả lập" hiện khi lượt nộp `ACCEPTED` **và** quick-insight không lỗi. Không có endpoint báo "AI đang khả dụng" nên lấy trạng thái quick-insight làm tín hiệu `[SoT: Suy luận]` (OQ-6); hết hạn mức AI do màn đích xử lý.
  - Liên kết trong tab Solution Review trỏ tới `review.submissionId` do F5 trả (báo cáo mới nhất), **không** tới lượt nộp mới nhất của lịch sử như bản dựng [SoT: F5:380; PV:513-528].
  - Màn này không tự kích hoạt F5 (hành động tự chọn ở màn đích, BD:809).
- **Tab rút gọn:** chỉ hai chỉ số độ phức tạp và liên kết báo cáo đầy đủ; khối nhận xét ngắn trong BD cũ đã bỏ theo RD cập nhật `DEC-2026-1025-dd-workspace-quick-insight-trim`. Prototype còn in `notes` mock cần gỡ khi nối API.

### BR-SC-10 Sao lưu và hoàn tác nháp trước khi áp dụng mã AI

- **Kích hoạt:** xác nhận “Ghi đè & Mở Workspace” ở `USR0301` và bấm Hoàn tác trong Workspace. Cùng cặp `(problemId, language, model)` dùng khoá nháp của BR-SC-02.
- **Quy tắc:** trước khi ghi mã đề xuất, `entities/problem` lưu bản mã hiện có vào lịch sử nháp `localStorage` với nhãn “Bản trước khi áp dụng AI Diff”, thời điểm và đúng cặp bài/ngôn ngữ/mô hình. Chỉ khi sao lưu thành công mới ghi mã đề xuất vào nháp và điều hướng; sao lưu lỗi hoặc bộ nhớ trình duyệt bị chặn thì giữ mã hiện có và ở lại màn phân tích. Mở Workspace cho thấy mã đề xuất và một nút Hoàn tác; bấm nút khôi phục đúng bản cũ và cập nhật nháp của cặp đó. Hoàn tác lỗi giữ cả mã hiện tại lẫn bản lịch sử để thử lại. [SoT: `02-bd/screens/users/USR0301_solution_review.md` EVT-6, Sheet 9 NO 7; `DEC-2026-1014-batch7-remaining-open-questions`]

## 5. Kiểm tra phía client

Màn có hai nhóm kiểm: gửi mã (Run Code, Submit, tải tệp) và chuẩn hoá tham số. Bảng 5.1 ánh xạ sang zod (`zod 3.24.1`, `package.json:35`, `README` của contract) và quy tắc ở V4. Cột "Khớp" là Có, Lệch (kèm lý do) hoặc Không áp dụng (chỉ phía client).

Bảng 5.1: Ánh xạ zod và V4.

| Trường | zod (`entities/problem/model/submit-schema.ts`) | Quy tắc V4 | Khớp |
| :--- | :--- | :--- | :--- |
| `language` | `z.enum(["JAVA","CPP","PYTHON"])` (đổi chữ thường ↔ chữ hoa ở lớp ánh xạ của entity) | `JDG-V03` (V4:29) | Có |
| `submissionModel` | `z.enum(["FUNCTION_WRAPPER","STANDARD_IO"])`; `refine`: `FUNCTION_WRAPPER` chỉ khi `functionWrapperSupported` | `JDG-V04`, `JDG-103` (V4:31-32) | Có; `refine` chỉ là bản sớm, máy chủ vẫn kiểm |
| `sourceCode` không rỗng | `.refine(s => s.trim().length > 0)` | `JDG-V01` (V4:33) | Lệch nhỏ: `trim` của JavaScript và phép bỏ khoảng trắng của máy chủ có thể khác ở ký tự đặc biệt (NBSP, BOM). Màn có thể cho qua mã trông trống mà máy chủ vẫn từ chối; khi đó hiện chuỗi của `JDG-V01`. Máy chủ là nguồn đúng |
| `sourceCode` kích thước | `.refine(s => new TextEncoder().encode(s).byteLength <= 65536)` | `JDG-V02`, đúng 65.536 nhận, 65.537 từ chối, đo bằng byte (V4:34) | Có |
| `sourceInputMethod` | `z.enum(["EDITOR","FILE_UPLOAD"])` | `JDG-V05` (V4:36) | Có |
| `fileName` | bắt buộc khi `FILE_UPLOAD`; đuôi không phân biệt hoa thường là `.java`, `.cpp`, `.py` theo ngôn ngữ; tối đa 255 ký tự | `JDG-V06` (V4:37) | Có (255 là `[SoT: Suy luận]` của V4) |
| Kích thước tệp | `file.size <= 65536` trước khi đọc | `JDG-V02` | Lệch thông điệp: màn nói "File vượt quá 64 KB" (BD:834), máy chủ coi nội dung tệp là mã nên trả chuỗi của `JDG-V02`. Cùng ngưỡng nên không lệch hành vi |
| `bookmarked` | `z.boolean()` | `PRB-001` (F2:737) | Có |
| `lang`, `model`, `tab` ở URL | `z.enum([...]).catch(mặc định)` | không có | Không áp dụng (chuẩn hoá của màn, BD:793 bước 3) |
| `algoprep-workspace-prefs` | `z.object({language, mode}).safeParse`, hỏng thì dùng mặc định | không có | Không áp dụng; bản dựng đang tự phân tích tay [SoT: PV:186-198] |

Lệch bắt buộc nêu khác: bản dựng giữ mô hình bằng `"function" | "stdio"` và ngôn ngữ bằng chữ thường [SoT: EP model/types.ts:48,111], còn hợp đồng dùng enum chữ hoa. Ánh xạ nằm ở `entities/problem/model`, view không biết enum của API; không cần thêm quyết định.

## 6. i18n và truy cập

**Tiền tố khoá:** `problemDetail.*` ở `messages/vi.json:261` và `messages/en.json:261`; mã lỗi API ở `apiError.*` dùng chung toàn app (mới). Khoá cần **thêm**: `statement.modelNoteFunction`, `statement.modelNoteStdio` (BD Khu vực D NO 6; bản dựng chưa có câu giải thích mô hình), `console.slowUpdate`, `console.reconnecting`, `console.queued`, `apiError.*` ở mục 3. Khoá `panel.noReview` không còn dùng sau OQ-7.

**Không đặt tên endpoint, mã quyền hay thuật ngữ backend vào nhãn:** chuỗi cho `JDG-106` nói "Bạn đã nộp quá số lần cho phép trong một giờ, thử lại sau." (BD:838), không nhắc "rate limit" hay mã.

**Truy cập:**

- Dải ô testcase: mỗi ô có tên truy cập "Testcase {n}: Đạt | Không đạt | Đang chờ", không có dữ liệu thật. Màu không là kênh duy nhất (nhãn chữ trong màn phụ).
- **Vùng `aria-live`:** một vùng `role="status"` (polite, ẩn thị giác) thông báo "Đang chấm: {đã xong}/{tổng} testcase" mỗi lần có `TESTCASE_FINISHED`, và thông báo kết quả cuối một lần. Không đọc từng ô để tránh tràn với bài 50 testcase `[SoT: Suy luận]`.
- Dòng "cập nhật đang chậm" cũng nằm trong vùng `role="status"`.
- Màn phụ dùng `Modal` (khoá focus, Esc đóng, trả focus về nút gọi) [SoT: PDLG:44-54].
- Thứ tự focus: thanh tác vụ (Danh sách bài, Run Code, Submit), dải tab panel trái, nội dung tab, nút Lưu bài, thanh công cụ soạn mã, ô soạn, vùng kết quả. Hai `SplitPane` dùng phím mũi tên và nhấp đúp (đã có, BD:375).
- Nút chọn ngôn ngữ, mô hình, phương thức nhập khi khoá dùng `aria-disabled` kèm lý do "đang chấm", không chỉ làm mờ.
- Monaco bắt phím Tab để thụt dòng; thoát bằng phím điều khiển sẵn có của Monaco để chuyển chế độ Tab di chuyển focus. `[SoT: Suy luận]` (hành vi thư viện, không phải yêu cầu BD); ghi vào chú thích dưới ô soạn nếu duyệt hình yêu cầu.

## 7. Điểm kiểm thử

Bảng 7.1 liệt kê điểm kiểm. Vitest dùng React Testing Library với mock `shared/realtime` (đưa chuỗi bản tin có `seq`); Playwright chạy trên ứng dụng thật khi backend và go-judge sẵn sàng. `AC` thuộc `04-tdd/screens/users/USR0102_problem_detail.md` (ghi `PD-AC-nn`) hoặc module (ghi `F4 AC-nn`).

Bảng 7.1: Điểm kiểm thử.

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| T-PD-01 | Normal | EVT-1: đề, ví dụ, mã khung, giới hạn, Sample, lịch sử hiện đủ | Vitest | PD-AC-01, F2 AC-08 |
| T-PD-02 | Abnormal | EVT-1: `PRB-010` hiện "Không tìm thấy bài toán.", không nhắc quyền | Vitest | F2 AC-09 |
| T-PD-03 | Abnormal | EVT-1: lịch sử nộp lỗi, tab Bài nộp có lỗi riêng, editor và Submit vẫn dùng được | Vitest | PD-AC-01 |
| T-PD-04 | Abnormal | EVT-1: quick-insight lỗi, ẩn tab Solution Review và hai liên kết, không toast lỗi | Vitest | PD-AC-08, F4 AC-68 |
| T-PD-05 | Abnormal | EVT-1: giới hạn lỗi, ẩn dòng giới hạn, không hiện số thay | Vitest | PD-AC-01 |
| T-PD-06 | Normal | EVT-3: đổi tab, mã và kết quả bên dưới không đổi; `tab` lên URL bằng replace | Vitest | PD-AC-02 |
| T-PD-07 | Normal | EVT-4: đổi ngôn ngữ khôi phục nháp của cặp mới, không có nháp thì mã khung; không gọi lại giới hạn | Vitest | PD-AC-02, F3 AC-01 |
| T-PD-08 | Boundary | EVT-5: `functionWrapperSupported = false` thì không vẽ nhóm nút mô hình, luôn Standard I/O dù đã nhớ `FUNCTION_WRAPPER` | Vitest | PD-AC-02, F3 AC-04 |
| T-PD-09 | Normal | EVT-6: đổi mô hình không đổi phương thức nhập; sang "Tải file" giữ mã đã gõ | Vitest | PD-AC-02 |
| T-PD-10 | Normal | EVT-7: gõ mã ghi nháp; nhãn "Đã lưu trên trình duyệt này" chỉ hiện khi ghi thành công, `localStorage` bị chặn thì không hiện và vẫn gõ được | Vitest | PD-AC-02 |
| T-PD-11 | Abnormal | EVT-8: sai đuôi, quá 64 KB bị chặn trước khi đọc, toast, giữ tệp cũ | Vitest | PD-AC-10, F4 AC-07 |
| T-PD-12 | Normal | EVT-9: "Xem nội dung" mở và đóng khối xem, không đổi phương thức | Vitest | PD-AC-10 |
| T-PD-13 | Boundary | EVT-10, 11: mã đúng 65.536 byte gửi, 65.537 byte chặn, nhiều byte tiếng Việt đo theo byte, toàn khoảng trắng chặn | Vitest | PD-AC-10, F4 AC-04 |
| T-PD-14 | Normal | EVT-10: Run Code hiện input, thực tế, mong đợi của Sample; không thêm dòng ở tab Bài nộp | Vitest, Playwright | PD-AC-07, F4 AC-10 |
| T-PD-15 | Abnormal | EVT-10: `outcome = COMPILE_ERROR` hiện thông báo dòng của học viên; `JDG-501` toast, mã giữ nguyên | Vitest | F4 AC-11, F4 AC-12 |
| T-PD-16 | Abnormal | EVT-10: `JDG-502` toast theo mã, nút chạy lại được | Vitest | F4 AC-13 |
| T-PD-16A | Boundary | EVT-10: `JDG-107` hiển thị thời gian chờ từ `retryAfterSeconds`, không làm mất mã nguồn | Vitest | PD-AC-13, F4 AC-73 |
| T-PD-17 | Normal | EVT-11: nộp nhận `PENDING` ngay, dải ô dựng, điều khiển và ô soạn khoá | Vitest | PD-AC-03, F4 AC-01 |
| T-PD-18 | Abnormal | EVT-11: `JDG-106` toast kèm số lần, không tạo lượt, không tự nộp lại khi lỗi mạng | Vitest | F4 AC-02 |
| T-PD-19 | Normal | EVT-12: chuỗi bản tin đúng `seq` tô ô theo thứ tự; bản tin `seq` thấp hoặc trùng bị bỏ; bản của `submissionId` khác bị bỏ | Vitest | PD-AC-03, F4 AC-25 |
| T-PD-20 | Normal | EVT-12: bản `isFinal` luôn áp, toast kết quả, tab Bài nộp nạp lại, điều khiển mở khoá | Vitest | PD-AC-06 |
| T-PD-21 | Abnormal | Nối lại: rớt kênh khi đã xong 4/12, trong lúc rớt xong thêm 3; nối lại thấy 7 ô đúng, không tô trùng; bài đã cuối trong lúc rớt thì thấy kết quả cuối | Vitest, Playwright | PD-AC-04, F4 AC-27 |
| T-PD-22 | Abnormal | Mất kết nối: không nối lại được, hỏi lại 3 giây tối đa 10 lần, rồi dòng "đang chậm", không kết luận trượt | Vitest (giả đồng hồ) | PD-AC-05, F4 AC-28 |
| T-PD-23 | Normal | Mở lại màn khi lượt mới nhất còn `JUDGING`: theo dõi tiếp, Submit khoá | Vitest, Playwright | PD-AC-04 |
| T-PD-24 | Security | Không nơi nào (DOM, `aria-label`, toast) chứa input, expected, output của testcase ẩn; ánh xạ bỏ trường thừa dù máy chủ trả | Vitest | PD-AC-03, F4 AC-29 |
| T-PD-25 | Normal | EVT-13, 14: đổi tab console; màn phụ Submit chỉ có Đạt, Không đạt cho testcase ẩn; Run Code mở sẵn testcase sai đầu | Vitest | PD-AC-03, PD-AC-07 |
| T-PD-26 | Normal | EVT-15: nhãn đổi ngay; lỗi thì hoàn nguyên và toast; gửi đúng trạng thái đích | Vitest | PD-AC-09, F2 AC-15 |
| T-PD-27 | Normal | EVT-16, 17, 18: liên kết sang kết quả mọi trạng thái cuối; sang review và phỏng vấn chỉ khi `ACCEPTED` và quick-insight không lỗi | Vitest | PD-AC-06, F5 AC-16 |
| T-PD-28 | Normal | EVT-19, 22: điều hướng thẳng không hộp xác nhận; click dòng lịch sử mở `submission_result` | Vitest | PD-AC-06 |
| T-PD-29 | Normal | EVT-20: kéo `SplitPane` đổi tỉ lệ, nhớ ở `localStorage`, chặn 22-74 và 25-85 | Vitest | TDD màn `PD-AC-12` |
| T-PD-30 | Normal | Luồng đầy đủ: mở bài, đổi sang C++, nộp, thấy dải ô chạy, Accepted, ba liên kết | Playwright | PD-AC-03, PD-AC-06 |

## 8. Nợ prototype và câu hỏi mở

**Dòng nợ sẽ gỡ.** Dòng `views/problem-detail + entities/problem` của `DEBT` mục 9 (`06-plan/PROTOTYPE_DEBT.md:883`) có "Điều kiện tốt nghiệp: BE có go-judge thật + WebSocket → viết DD → nối Monaco + STOMP thật → thay mock → test". DD xong là bước "viết DD"; các bước nối thật, thay mock và test **chưa làm** nên dòng này **chưa gỡ** (xem báo cáo bàn giao).

Bảng 8.1: Lệch giữa prototype và thiết kế, cần làm khi nối thật.

| # | Prototype hiện tại | Thiết kế | Nguồn |
| :-: | :--- | :--- | :--- |
| P1 | Toàn bộ API mock, một mock gộp `ProblemDetail` | Sáu query riêng (mục 2) | PV:1-17; EP model/types.ts:140-166 |
| P2 | Không STOMP, kết quả sau `setTimeout` | BR-SC-05, BR-SC-06 | PV:13-15, 341-378 |
| P3 | `lang`, `model`, `tab` ở `useState` | Ghi URL bằng replace | PV:134-137; BD:388, 779 |
| P4 | Khoá chỉ hai nút Run, Submit lúc `running`; ô soạn không `readOnly` | Khoá ba nhóm chọn và ô soạn (cần S1) | PV:834-841; BD:654 |
| P5 | Tab Solution Review luôn hiện, in `notes` mock, liên kết theo lượt nộp mới nhất | Ẩn tab khi không có báo cáo; không có `notes`; liên kết theo `review.submissionId` | PV:486-533; BD:593; F5:380 |
| P6 | Nút Lưu bài chỉ đổi state | Gọi `ToggleMyProblemBookmark` lạc quan | PV:291-301 |
| P7 | Nhãn "Cả hai mô hình khả dụng" ở tab Đề bài | Nhãn **mô hình đang chọn** (BD Khu vực D NO 4) và câu giải thích mô hình (NO 6) | PV:406-413; BD:490, 492 |
| P8 | "Xem kết quả chấm" chỉ khi `Accepted` | Mọi trạng thái cuối | PV:780-801; BD:674 |
| P9 | Dòng lịch sử nộp không bấm được | Bấm mở `submission_result` (EVT-22) | PV:449-480; BD:814 |
| P10 | Enum chữ thường, mô hình `function`/`stdio` | Ánh xạ sang enum API ở entity | EP model/types.ts:48,111 |
| P11 | Không gửi `fileName`, `sourceInputMethod` | BR-SC-03 bước 5 | PV:274-289; F4:358-359 |
| P12 | Lỗi tệp chỉ toast, không viền đỏ | Viền vùng tải đỏ kèm toast | PV:238-260; BD:801 |
| P13 | Giới hạn bộ nhớ, thời gian có fallback mock | Ẩn khi lỗi | PV:165 |
| P14 | Không có vùng `aria-live` cho tiến trình | Vùng `role="status"` (mục 6) | PV:663-803 |
| P15 | `PV` 880 dòng | Tách `ui/` và `model/` khi nối API | PV:119-879 |

**Câu hỏi mở mức màn.** Mỗi câu kèm mặc định đề xuất và bậc ponytail (`.claude/rules/ponytail-criteria.md`). Câu đã chốt ghi "Đã chốt" kèm mã quyết định; các câu còn lại do chủ dự án quyết.

Bảng 8.2: Câu hỏi mở.

| # | Câu hỏi | Mặc định đề xuất | Bậc |
| :-: | :--- | :--- | :-: |
| OQ-1 | **Đã chốt 2026-10-24** (`DEC-2026-1024-dd-screens-users-core-contract-changes`): thư viện STOMP là `@stomp/stompjs`, một khách ở `shared/realtime` dùng chung hai màn. Mã làm sau: thêm vào `package.json`, dựng `shared/realtime/stomp-client.ts` | Đã chốt | 1 |
| OQ-2 | **Đã chốt 2026-10-24** (`DEC-2026-1024-dd-screens-users-core-contract-changes`): F2 mục 8 trả `testcaseId` ở mỗi phần tử Sample, ghép kết quả F4 theo khoá, không theo `order` | Đã chốt, F2 mục 8 và `04-tdd/modules/F2_problem-bank.md` AC-11 đã sửa | 1 |
| OQ-3 | Khi `GetEffectiveExecutionLimits` lỗi, BD muốn hiện giới hạn cơ sở, F2 không trả giới hạn | Ẩn dòng giới hạn. Phương án thay: thêm hai trường giới hạn cơ sở vào F2 mục 6, nhưng số chưa nhân hệ số dễ gây hiểu sai | 2 |
| OQ-4 | F4 trả `enabled` theo ngôn ngữ (F4:219), BD ghi "luôn đủ ba nút, không ẩn" (BD:644) | Vẫn vẽ đủ ba nút; ngôn ngữ `enabled = false` vô hiệu kèm chú thích, không ẩn | 1 |
| OQ-5 | Tải lại trang khi lượt nộp còn chạy có tiếp tục theo dõi không (BR-SC-06)? | Có, lấy từ danh sách lượt nộp; cần vì Submit phải khoá khi lượt trước chưa cuối | 1 |
| OQ-6 | Không có endpoint báo AI sống hay chết; liên kết sang AI ẩn theo gì? | Theo trạng thái query quick-insight (lỗi thì ẩn); hết hạn mức xử lý ở màn đích | 5 |
| OQ-7 | **Đã chốt** `DEC-2026-1025-dd-workspace-quick-insight-trim`: bỏ khối nhận xét ngắn khỏi RD Workspace | DD giữ hai ô độ phức tạp và liên kết báo cáo; đồng bộ BD Khu vực F NO 2 ở lượt BD | 2 |
| OQ-8 | Mã khung lỗi lúc mở màn: BD không nêu | Ô soạn trống và gõ được, toast lỗi | 5 |
| OQ-9 | Axios timeout của Run Code | 65 giây (hơn hạn 60 giây của máy chủ) | 5 |
| OQ-10 | Dữ liệu vào của Sample ở mô hình Bọc hàm: BD muốn tách theo tham số, F2 chỉ trả `parameterNames` và chuỗi input chưa tách (F2:549) | Hiện nguyên chuỗi input trong một khối kèm dòng "Tham số: a, b, c"; không tự tách ở client | 2 |
| OQ-11 | **Đã chốt 2026-10-24** (`DEC-2026-1024-dd-screens-users-core-contract-changes`): mọi route của học viên dùng `problemId` (UUID); `slug` chỉ để hiển thị | Đã chốt, BD `USR0201` Sheet 3 đã sửa (BD `USR0102` không có route slug) | 4 |
