# DD màn hình — Kết quả nộp bài (`USR0201`)

> Tuân khung `03-dd/screens/README.md` mục 4 (chín mục 0 đến 8) và quy tắc cứng mục 5. Request, response, mã lỗi nằm ở file API của module và chỉ được liệt kê theo ID. Không có logic backend ở đây. Một trang, hai vùng: học viên (A1) và giảng viên (A2, chỉ đọc), cùng một view slice.

## 0. Thông tin tài liệu

Bảng 0.1: Bìa tài liệu.

| Mục | Giá trị |
| :--- | :--- |
| Mã màn, slug | `USR0201`, `submission_result` |
| Tên màn | Kết quả nộp bài |
| Nhóm actor | `users` (A1 `STUDENT`); A2 `INSTRUCTOR` chỉ đọc trên cùng view |
| Route | A1 `/submissions/{submissionId}`; A2 `/instructor/submissions/{submissionId}?from=&studentId=&classId=` |
| Bounded Context đứng sau | F4 `judge-orchestration`; đọc thêm F2 `problem-bank`; liên kết sang F5 `ai-review` |
| RD nguồn | `01-rd/screens/users/USR0201_submission_result.md` |
| BD nguồn | `02-bd/screens/users/USR0201_submission_result.md` (V0.11) |
| Prototype | `05-coding/frontend/src/views/users/submission-result/` (toàn bộ mock, không STOMP) |
| Phiên bản, ngày | V0.1, 2026-10-23 |
| Tiêu chí nghiệm thu | `04-tdd/screens/users/USR0201_submission_result.md` (luồng xuyên module) cộng các `AC` module ghi ở mục 7 |

Bảng 0.2: Lịch sử sửa.

| Ver | Mục | Nội dung | Ngày |
| :--- | :--- | :--- | :--- |
| V0.1 | Toàn bộ | Tạo mới từ prototype đã dựng và DD F2, F4, F5. Chốt cách hợp nhất ảnh chụp REST với bản tin realtime, cách ghép dữ liệu testcase mẫu từ hai module, hành vi nối lại ở mức màn | 2026-10-23 |

Bảng 0.3: Viết tắt nguồn trích dẫn.

| Viết tắt | Đường dẫn |
| :--- | :--- |
| RD | `01-rd/screens/users/USR0201_submission_result.md` |
| BD | `02-bd/screens/users/USR0201_submission_result.md` |
| F2, F4, F5 | `03-dd/api/F2_problem-bank.md`, `F4_judge-orchestration.md`, `F5_ai-review.md` |
| V4 | `03-dd/validation/F4_judge-orchestration.md` |
| L4 | `03-dd/logic/F4_judge-orchestration.md` |
| API | `03-dd/api/api.md` |
| SRV | `05-coding/frontend/src/views/users/submission-result/ui/submission-result-view.tsx` |
| ES | `05-coding/frontend/src/entities/submission/` |
| DEBT | `06-plan/PROTOTYPE_DEBT.md` |
| DD-PD | `03-dd/screens/users/USR0102_problem_detail.md` (dùng chung hook theo dõi tiến trình) |

## 1. Bản đồ slice FSD

Thứ tự import `app → views → widgets → features → entities → shared`. Bảng 1.1 là bản đồ đích.

Bảng 1.1: Slice của màn.

| Lớp | Đường dẫn (từ `src/`) | Vai trò | Hiện trạng |
| :--- | :--- | :--- | :--- |
| app | `app/(student)/submissions/[submissionId]/page.tsx` | Render `SubmissionResultView` (A1) | Có |
| app | `app/(instructor)/instructor/submissions/[submissionId]/page.tsx` | Render cùng view với `area="instructor"` và hai render-prop breadcrumb, ngữ cảnh | Có; `app` tự nối nên `views` không import `views` |
| views | `views/users/submission-result/ui/submission-result-view.tsx` | Một view cho cả hai vùng, tham số `area` | Có, 626 dòng [SoT: SRV:77-543] |
| views | `views/teacher/submission-context/` | Breadcrumb và thẻ ngữ cảnh của A2 (không thuộc màn này, chỉ được truyền vào qua render-prop) | Có |
| entities | `entities/submission/api/queries.ts` | `useSubmissionDetail(id, area)` | Có, mock [SoT: ES api/queries.ts:29-41] |
| entities | `entities/submission/api/use-submission-progress.ts` | Hook "đăng ký rồi chụp ảnh" cho một `submissionId`; **dùng chung với DD-PD BR-SC-05** | Chưa có |
| entities | `entities/submission/model/progress-reducer.ts` | Hàm thuần áp bản tin theo `seq` | Chưa có |
| entities | `entities/submission/model/types.ts` | Kiểu `SubmissionDetail`, `SubmissionTestcaseResult` | Có, cần thêm trường (Bảng 1.2) [SoT: ES model/types.ts:33-68] |
| entities | `entities/problem/api/queries.ts` | `useSampleTestcases(problemId)` để lấy input và kết quả mong đợi của Sample | Chưa có |
| entities | `entities/submission/model/origin.ts` | `parseSubmissionOrigin` cho `from` (A2) | Có [SoT: ES model/origin.ts:4-10] |
| shared | `shared/ui` `CodeEditor` với `readOnly`; `shared/realtime/stomp-client.ts`; `useErrorMessage` | Nâng lên `shared` (mô tả ở DD-PD mục 1, S1 đến S3) | Chưa có |

Thành phần dùng lại từ `shared/`: `Badge`, `Button`, `EmptyState`, `QueryBoundary`, `QueryError`, `toast`, `formatDateVN`, `useT`, `useLocale`, `SUBMISSION_STATUS_VARIANT` [SoT: SRV:16-40].

Bảng 1.2: Trường thực thể cần thêm khi nối API thật (so với kiểu đang có).

| Trường | Lý do | Nguồn |
| :--- | :--- | :--- |
| `problem: { problemId, code, title, slug, levelDisplayName, withdrawn } \| null` thay `problemId`, `problemSlug`, `problemTitle`, `difficulty` đứng riêng | Máy chủ trả `problem` có thể `null` khi `problem-bank` lỗi | F4:486 |
| `scoreRatio`, `beatsSampleSize` | Có trong phản hồi; `beatsSampleSize` hiện không nằm trong thực thể | F4:489-492; SRV:62-64 |
| `testcaseDetail: { available, unavailableReason, items[] }` thay mảng `testcases` | Hết hạn 90 ngày và "không áp dụng" | F4:495-508 |
| `testcase.testcaseId` nullable, `testcase.actualOutput` thay `sampleActualOutput`; bỏ `sampleInput`, `sampleExpectedOutput` | Input và kết quả mong đợi của Sample không đi qua F4, ghép từ F2 (BR-SC-03) | F4:504-507; L4:236 |
| `memoryKb` đã là KB | Khớp F4 (BD dùng `memoryMb`, lệch) | F4:490 |

**Lệch có chủ ý so với BD 4.5.** BD đặt `model/use-submission-realtime.ts` bên trong view và bẻ view thành sáu file [SoT: BD:249-259]. DD để hook theo dõi ở `entities/submission` vì `USR0102` dùng cùng thuật toán; việc tách `verdict-banner`, `testcase-results-table`, `source-viewer`, `next-actions` thành file riêng là tuỳ chọn khi chạm vào file, không bắt buộc.

## 2. API tiêu thụ

Bảng 2.1: Sự kiện BD, endpoint theo ID, hook, trạng thái nối. Hiện mọi dòng đều mock [SoT: SRV:1-13].

| EVT (BD) | Endpoint theo ID | Hook | Nối |
| :--- | :--- | :--- | :--- |
| EVT-1 (A1) | F4 mục 8 `GetSubmissionDetail` | `useSubmissionDetail(id, "student")` | mock [SoT: ES api/queries.ts:29-41] |
| EVT-1 (A2) | F4 mục 14 `GetSubmissionDetail` biến thể giảng viên | `useSubmissionDetail(id, "instructor")` | mock |
| EVT-4 (mở rộng testcase mẫu) | F2 mục 8 `GetProblemSampleTestcases` | `useSampleTestcases(problemId)`, **gọi lười** ở lần mở rộng đầu tiên | chưa có |
| EVT-1, EVT-9 | F4 mục 2 kênh `/user/queue/submissions` (W1) | `useSubmissionProgress` (đăng ký) | chưa có |
| EVT-1, EVT-9 | F4 mục 9 `GET /submissions/{submissionId}/progress` | `useSubmissionProgress` (ảnh chụp, nối lại, dự phòng) | chưa có |
| EVT-2, 3, 6, 7, 8 | không gọi API | `next/link` | không áp dụng |
| EVT-5 | không gọi API (Clipboard) | state màn | không áp dụng |

Vùng giảng viên **không** dùng hai dòng realtime (BD:549; F4:778; RD REQ-18). Hợp đồng kênh (đích, loại bản tin, `seq`, nối lại) ở F4 mục 2; file này không chép schema bản tin.

Điểm khác BD khi nối thật:

1. BD mô tả bản tin mang `runtimeMs`, `memoryMb` theo từng testcase [SoT: BD:224]; F4 bản tin chỉ có `order`, `verdict` và số đếm, thời gian và bộ nhớ từng dòng đọc ở F4 mục 8 sau khi kết thúc [SoT: F4:127]. Trong lúc chấm hai cột Runtime, Memory của dòng hiện `-`.
2. BD Sheet 7.2 NO 4 còn ghi điều kiện `is_sample = true` và sắp theo `order_index` [SoT: BD:432]; cột thật là `visibility` và `position` [SoT: BD:308]. DD theo F2 mục 8.
3. Ghép Sample theo `testcaseId`: F2 mục 8 trả `testcaseId` ở mỗi phần tử Sample (`DEC-2026-1024-dd-screens-users-core-contract-changes`) [SoT: F2:547; F4:504].

## 3. Trạng thái màn

Bảng 3.1: Trạng thái theo vùng. Khoá i18n có tiền tố `submissionResult.` hoặc `apiError.` (frontend không đọc `message`, API:22).

| Vùng | Trạng thái | Điều kiện | Hiển thị | Khoá i18n |
| :--- | :--- | :--- | :--- | :--- |
| Toàn trang | Đang tải | `GetSubmissionDetail` chưa xong | Khung chờ, tiêu đề ẩn cho trình đọc màn hình | `pageTitle` |
| Toàn trang | Rỗng | `404 JDG-110` (không có, của người khác, hoặc ngoài lớp với A2) | "Không tìm thấy bài nộp." **Cùng một giao diện** cho mọi nguyên nhân, không lộ sự tồn tại [SoT: F4:523; BD:469] | `notFound` |
| Toàn trang | Lỗi | lỗi mạng, `5xx` | `QueryError` kèm thử lại; **không** coi là không tìm thấy | `common.error` |
| Toàn trang | Quyền bị từ chối | A2 thiếu `CLASS_MANAGEMENT:READ`: `403 JDG-301` | Thông báo theo mã, khác với không tìm thấy [SoT: F4:795] | `apiError.JDG-301` |
| Toàn trang | Chưa đăng nhập | `401` sau khi làm mới token thất bại | Chuyển về `auth` (xử lý ở `shared/api/client.ts`) | không áp dụng |
| Banner | Chưa cuối | `PENDING`, `COMPILING`, `JUDGING` | Nhãn trạng thái (`verdict.PENDING` ...), số đạt/tổng lấy từ tiến trình, các chỉ số `-` | `verdict.*` |
| Banner | `SYSTEM_ERROR` | | Nhãn "Lỗi hệ thống"; điểm `-`; không phải lỗi của học viên | `verdict.SYSTEM_ERROR` |
| Bảng testcase | Chờ | chưa cuối và chưa có dòng nào | Ghi chú "Chưa có testcase nào chạy"; dải chờ theo `totalCount` khi đã biết | `testcaseEmpty` |
| Bảng testcase | Hết hạn | `testcaseDetail.available = false`, `unavailableReason = RETENTION_EXPIRED` | Thay bảng bằng một câu "Chi tiết từng testcase chỉ lưu 90 ngày"; banner và mã vẫn hiện | `testcaseExpired` (mới) |
| Bảng testcase | Ẩn hẳn | `status = COMPILE_ERROR` | **Không vẽ** bảng; hiện khối lỗi biên dịch (Khu vực E NO 4) [SoT: BD:380; SRV:125-127] | không áp dụng |
| Bảng testcase | Không áp dụng | `NOT_APPLICABLE` với trạng thái cuối khác `COMPILE_ERROR` (ví dụ `SYSTEM_ERROR` trước khi chạy) | Câu "Chưa có testcase nào chạy" | `testcaseEmpty` |
| Mã nguồn | Hiện | luôn có `sourceCode` | Trình xem chỉ đọc; copy lỗi thì toast | `code.copyFailed` |
| Breadcrumb, nút Làm lại | Bài không đọc được | `problem = null` | Chỉ hiện `Lượt nộp #{id}`; nút "Làm lại" và liên kết bài bị vô hiệu | `withdrawn` (OQ-3 đã chốt) |
| Cập nhật chậm | mất kênh, hỏi đủ 10 lần mà chưa cuối | | Dòng "cập nhật đang chậm", không kết luận trượt | `progress.slow` (mới) |
| Vùng giảng viên | Chỉ đọc | `area = instructor` | Không có khu vực F; không kênh realtime | `instructorReadOnly` |
| AI suy giảm | F5 chết | | Không ảnh hưởng: màn **không gọi** F5, chỉ điều hướng (F4 AC-68) | không áp dụng |

Khi `status` chưa cuối, **bỏ qua** `testcaseDetail.available = false` do F4 chưa nêu rõ giá trị của `available` cho `PENDING` và `COMPILING` (OQ-4); chỉ coi `available = false` là hết hạn hay không áp dụng khi `status` đã ở một trong sáu trạng thái cuối.

## 4. Quy tắc đặc thù màn

### BR-SC-01 Chọn đường tải theo trạng thái

- **Kích hoạt:** EVT-1.
- **Tiền điều kiện:** có `submissionId` trên đường dẫn.
- **Các bước:** (1) gọi F4 mục 8 (A1) hoặc mục 14 (A2); (2) `status` đã cuối thì hết việc, **không** mở STOMP [SoT: BD:118, 452]; (3) `status` chưa cuối và `area = student` thì chạy `attach(submissionId)` (cùng thuật toán DD-PD BR-SC-05: đăng ký `/user/queue/submissions` trước, rồi chụp F4 mục 9, rồi áp bản tin có `seq` lớn hơn); (4) `area = instructor` thì **không** đăng ký, hiện trạng thái hiện tại, muốn mới hơn thì tải lại trang [SoT: BD:549; RD:231].
- **Hậu điều kiện:** mở trang của bài nộp đã cuối là chỉ một lần gọi REST (RD REQ-09 [SoT: RD:160]).
- **Lý do thiếu kênh với A2:** không mở thêm cửa nghe kênh của học viên (BD:549).

### BR-SC-02 Hợp nhất ảnh chụp REST với bản tin

- **Kích hoạt:** EVT-9 và nối lại.
- **Quy tắc:** nguồn sự thật là DB; bản tin chỉ làm tươi [SoT: F4:143]. Khi chưa cuối: banner lấy `status`, `passedCount`, `totalCount` từ tiến trình, dải dòng testcase lấy `{order, verdict}` từ tiến trình; hai cột Runtime, Memory từng dòng hiện `-`.
- **Khi nhận bản tin cuối (`isFinal`) hoặc ảnh chụp báo cuối:** tạm hiện số từ bản tin (`status`, `passedCount`, `totalCount`, `runtimeMs`, `memoryKb`), **làm mới** F4 mục 8 và, khi dữ liệu về, thay toàn bộ bằng phản hồi mục 8 (giữ dữ liệu cũ trong lúc làm mới để không nháy trống). Đóng/ngừng theo dõi sau đó [SoT: BD:460].
- **Áp bản tin:** như F4 mục 2.4 bước 3: `seq` thấp hơn hoặc bằng `seq` cuối thì bỏ, trừ bản `isFinal`; `submissionId` khác thì bỏ.
- **Mất kênh và nối lại:** mất kết nối khi chưa cuối thì nối lại STOMP có lùi và hỏi F4 mục 9 mỗi 3 giây tối đa 10 lần (BD Sheet 9 NO 5); nối lại được thì chạy lại `attach` và dừng hỏi. Hết 10 lần chưa cuối thì hiện dòng "đang chậm", **không** kết luận trượt [SoT: F4:146-156; BD:472]. Người học tải lại trang sẽ thấy đúng trạng thái.
- **Trang chỉ để một người xem:** khi người học đóng trang trong lúc chấm, bản tin không bị mất (chấm vẫn chạy ở máy chủ); mở lại `/submissions/{id}` là chạy lại BR-SC-01.

### BR-SC-03 Dữ liệu testcase mẫu ghép từ hai module và không bao giờ có testcase ẩn

- **Kích hoạt:** EVT-4 (bấm "Chi tiết").
- **Tiền điều kiện:** dòng là `visibility = SAMPLE` **và** có `testcaseId`.
- **Các bước:**
  1. Nút "Chi tiết" của dòng `HIDDEN` hoặc không có `testcaseId` bị vô hiệu (`aria-disabled`), bấm không mở gì và không gọi API [SoT: BD:386].
  2. Mở rộng dòng Sample lần đầu: gọi `useSampleTestcases(problem.problemId)` (cần `problem != null`); ghép **theo `testcaseId`** để lấy `input` và `expectedOutput`. `expectedOutput` của Sample do F2 phục vụ, không đi qua F4 [SoT: L4:236].
  3. `actualOutput` lấy từ chính dòng của F4 (cột `actual_output_snippet`, chỉ có với Sample). Hiển thị tối đa **2.000** ký tự; phần quá không hiện, không có nút "xem thêm" (DB lưu 4.096, hai số khác nhau có chủ ý) [SoT: BD:315; F4:507].
  4. Không có `actualOutput` thì hộp thực tế in `-` [SoT: BD:389].
  5. Lỗi F2 thì hai hộp input và mong đợi in `-` kèm một dòng giải thích; hộp thực tế vẫn hiện.
- **Phòng thủ chiều sâu:** hàm ánh xạ của `entities/submission` **bỏ** `testcaseId`, `actualOutput`, `stderrSnippet` của mọi dòng không phải `SAMPLE` kể cả khi máy chủ lỡ trả, và không bao giờ đọc input hay expected từ phản hồi F4. Máy chủ vẫn là chốt chặn thật (BR-JD-11, hai lớp độc lập).
- **Hậu điều kiện:** không có đường nào trong màn dẫn tới input, expected, output hoặc diff của testcase ẩn; chú giải dòng chỉ là "Testcase {n}: {verdict}". `stderrSnippet` chỉ hiện cho `STANDARD_IO`, testcase Sample, `verdict != AC` và khi quản trị cho phép; `FUNCTION_WRAPPER` luôn ẩn để không lộ stack trace mã khung [owner 2026-10-25; F4 §5].
- **Ghi chú:** BD nói "ẩn dòng thông báo testcase ẩn", bản dựng đã bỏ [SoT: BD:78].

### BR-SC-04 Điểm, Beats và chỉ số

- **Kích hoạt:** dựng banner.
- **Quy tắc:**
  - Điểm: banner in `{passedCount}/{totalCount}` (BD Khu vực B NO 3); `-` khi `null` (`COMPILE_ERROR`, `SYSTEM_ERROR`, chưa chấm). `scoreRatio` đi kèm phản hồi nhưng không in riêng (BD V0.9 bỏ thẻ Điểm), xem OQ-5.
  - Beats: `ACCEPTED` và `beatsPercent != null` thì in "Nhanh hơn {n}%"; `ACCEPTED` mà `beatsPercent = null` thì in "Chưa đủ dữ liệu so sánh"; trạng thái khác `ACCEPTED` thì `-`; A2 không in Beats [SoT: F4:491-492; BD:298; BR-JD-12].
  - **Bỏ hằng số `BEATS_MIN_ACCEPTED` ở client** [SoT: SRV:50-75]: máy chủ trả `beatsPercent = null` khi dưới 10 mẫu, ngưỡng chỉ nằm một chỗ (L4:255, `judge.beats.min-sample`). Lệch BD Sheet 9 NO 6 (BD:473 ghi cả máy chủ và màn kiểm); lý do: hai hằng số có thể trôi lệch nhau.
  - Runtime, Memory: `runtimeMs` (ms), `memoryKb` đổi sang MB một chữ số thập phân; `null` thì `-` [SoT: BD:296-297; SRV:54-60]. Giờ nộp in bằng `formatDateVN` (UTC+7).
  - Cách nộp: nhãn theo `submissionModel` ("Hàm (Wrapper)", "Standard I/O").
  - Dòng chữ chỉ số nằm trong banner, không dải thẻ (quy tắc chủ dự án 2026-10-13, BD V0.9).

### BR-SC-05 Điều hướng và điều kiện kích hoạt hành động

- **Kích hoạt:** EVT-2, 3, 6, 7, 8.
- **Quy tắc:**
  - "Phân tích bài giải" và "Phỏng vấn giả lập" chỉ bật khi `status = ACCEPTED`; nếu chưa đạt thì **vô hiệu** kèm giải thích bằng văn bản gắn `aria-describedby`, không chỉ `title` (tooltip của phần tử bị vô hiệu không tới được bằng bàn phím) [SoT: BD:404-405; SRV:322-350].
  - "Làm lại bài" đi `/workspace/{problem.problemId}`; breadcrumb tên bài đi `/problems/{problem.problemId}`; "Bài toán" đi `/problems`. Dùng **`problemId`**, không dùng `slug` như bản dựng [SoT: SRV:167-169, 532]; BD Sheet 3 đã sửa theo `DEC-2026-1024-dd-screens-users-core-contract-changes` (OQ-2).
  - `problem = null`: vô hiệu "Làm lại" và liên kết tên bài.
  - Bài đã rút khỏi ngân hàng: BD muốn nhãn "Đã rút khỏi ngân hàng" [SoT: BD:34]; `problem.withdrawn = true` thì hiện nhãn và vô hiệu "Làm lại" cùng liên kết tên bài; `problem = null` chỉ là lỗi của `problem-bank`, không hiện nhãn này (`DEC-2026-1024-dd-screens-users-core-contract-changes`, OQ-3).
  - Màn **không gọi F5**; "Phân tích bài giải" và "Phỏng vấn giả lập" chỉ điều hướng sang `/submissions/{id}/review` và `/submissions/{id}/interview` (hành động tự chọn ở màn đích) [SoT: BD:143, 128].
- **Vùng A2:** không dựng khu vực hành động (RD REQ-13); breadcrumb theo `from` (`submissions`, `student`, `class`, mặc định `submissions`), mục sidebar sáng theo màn gọi tới; `from`, `studentId`, `classId` chỉ là gợi ý giao diện, máy chủ lấy học viên và lớp từ lượt nộp [SoT: F4:785; ES model/origin.ts:4-10].

### BR-SC-06 Trình xem mã nguồn và lỗi biên dịch

- **Quy tắc:** mã nguồn là **dữ liệu**: hiển thị nguyên văn trong `CodeEditor readOnly`, không bao giờ diễn giải, không chèn vào chuỗi nào khác (`CLAUDE.md`). Ngôn ngữ cú pháp theo `language` của bài nộp (`java`, `cpp`, `python`, đúng ba) [SoT: `05-coding/frontend/src/shared/ui/data/code-editor.tsx:70`].
- `COMPILE_ERROR`: khối lỗi hiện `compileErrorMessage` (đã ánh xạ về dòng của học viên và lọc mã khung ở máy chủ; không bao giờ hiện nguyên văn `stderr`). Màn **không** tô dòng lỗi trong trình xem vì F4 chỉ trả một chuỗi văn bản, không có số dòng có cấu trúc; BD E NO 3 đòi tô dòng [SoT: BD:397; F4:1418], xem OQ-6.
- Bản dựng hiển thị mã bằng thẻ `<pre>`, không phải Monaco chỉ đọc [SoT: SRV:309-311, 444-446]; nợ.

## 5. Kiểm tra phía client

Màn **không có form**: không có trường nhập nào được gửi lên máy chủ. Bảng 5.1 chỉ ghi những giá trị đầu vào màn phải chuẩn hoá hoặc không được tự kiểm.

Bảng 5.1: Đầu vào của màn và ánh xạ.

| Trường | zod hoặc xử lý | Quy tắc V4 | Khớp |
| :--- | :--- | :--- | :--- |
| `submissionId` (path) | **Không kiểm ở client**; id lạ, sai định dạng hay của người khác đều để máy chủ trả `JDG-110` | `JDG-110`, cùng một mã (V4:59) | Có: một nguồn, không lộ khác biệt |
| `from` (query, A2) | `z.enum(["submissions","student","class"]).catch("submissions")` | không có (chỉ gợi ý giao diện, V4:62) | Không áp dụng |
| `studentId`, `classId` (query, A2) | Chỉ đối chiếu để dựng breadcrumb; bỏ qua nếu không khớp | bỏ qua hoàn toàn ở máy chủ (V4:62) | Có |

## 6. i18n và truy cập

**Tiền tố khoá:** `submissionResult.*` ở `messages/vi.json:1824` và `messages/en.json:1824`. Khoá cần **thêm**: `testcaseExpired`, `progress.slow`, `progress.live` (thông báo `aria-live`), `actions.needAcceptedDesc`, `code.withdrawn` (OQ-3 đã chốt), cùng `apiError.JDG-110`, `apiError.JDG-301`. Khoá `verdict.*` đã có đủ chín trạng thái. Mã verdict từng testcase (`AC`, `WA`, `TLE`, `MLE`, `RE`) hiện in mã quốc tế, chưa qua i18n; thêm `testcaseVerdict.*` (nợ nhỏ đã biết, BD:593).

**Không đặt tên endpoint, mã quyền hay thuật ngữ backend vào nhãn.**

**Truy cập:**

- Bảng testcase có `caption` ẩn (`testcaseTableCaption`), nút mở rộng có `aria-expanded` khi là Sample [SoT: SRV:241-243, 584].
- **Vùng `aria-live`:** một vùng `role="status"` thông báo "Đang chấm: {đã xong}/{tổng} testcase" và kết quả cuối một lần, không đọc từng dòng `[SoT: Suy luận]`. Dòng "đang chậm" cũng nằm trong vùng này.
- Màu verdict luôn đi kèm chữ trong `Badge`.
- Nút bị vô hiệu dùng `aria-disabled` và mô tả bằng văn bản (BR-SC-05).
- Bảng testcase là bảng dữ liệu thật (`<table>`, `<th scope="col">`).
- Thứ tự focus: breadcrumb, (thẻ ngữ cảnh với A2), bảng testcase, trình xem mã, nút "Sao chép mã", ba nút hành động. Cột phải dính khi cuộn không đổi thứ tự DOM.

## 7. Điểm kiểm thử

Bảng 7.1 liệt kê điểm kiểm. `AC` thuộc `04-tdd/screens/users/USR0201_submission_result.md` (ghi `SR-AC-nn`) hoặc module (ghi `F4 AC-nn`).

Bảng 7.1: Điểm kiểm thử.

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| T-SR-01 | Normal | EVT-1: bài nộp `ACCEPTED` đã cuối hiện banner, bảng đủ dòng, mã, **không** mở kênh | Vitest | SR-AC-01, F4 AC-30 |
| T-SR-02 | Abnormal | EVT-1: `JDG-110` hiện "Không tìm thấy bài nộp."; id của người khác và id không tồn tại cho **cùng** giao diện | Vitest | SR-AC-07, F4 AC-32 |
| T-SR-03 | Abnormal | EVT-1: lỗi mạng hiện `QueryError`, không phải "không tìm thấy" | Vitest | SR-AC-07 |
| T-SR-04 | Normal | EVT-1: `COMPILE_ERROR` ẩn bảng testcase, hiện khối lỗi biên dịch và mã | Vitest | SR-AC-05, F3 AC-14 |
| T-SR-05 | Boundary | `testcaseDetail.available = false`, `RETENTION_EXPIRED`: thay bảng bằng một câu, banner và mã còn | Vitest | SR-AC-06, F4 AC-33 |
| T-SR-06 | Boundary | Trạng thái chưa cuối và `available = false`: bỏ qua cờ, hiện chờ chứ không hiện "hết hạn" | Vitest | SR-AC-02 |
| T-SR-07 | Normal | EVT-1 chưa cuối: đăng ký kênh, chụp ảnh, áp bản tin đúng `seq`; dòng lấp dần | Vitest | SR-AC-02, F4 AC-25 |
| T-SR-08 | Normal | EVT-9: `isFinal` chuyển banner sang số cuối, làm mới chi tiết, Runtime và Memory từng dòng hiện sau khi kết thúc | Vitest | SR-AC-02 |
| T-SR-09 | Abnormal | Nối lại giữa chừng: dựng lại dòng từ ảnh chụp, không áp trùng bản tin cũ | Vitest | SR-AC-03, F4 AC-27 |
| T-SR-10 | Abnormal | Mất kênh: hỏi lại 3 giây tối đa 10 lần rồi dòng "đang chậm"; không kết luận trượt | Vitest (giả đồng hồ) | SR-AC-03, F4 AC-28 |
| T-SR-11 | Security | Vùng giảng viên: không đăng ký kênh, không hỏi tiến trình, không có khu vực F | Vitest | SR-AC-10, F4 AC-45 |
| T-SR-12 | Normal | EVT-4: mở rộng Sample ghép input, mong đợi từ ngân hàng bài theo `testcaseId`, thực tế từ kết quả; cắt 2.000 ký tự | Vitest | SR-AC-04, F2 AC-11 |
| T-SR-13 | Security | EVT-4: dòng `HIDDEN` vô hiệu, bấm không mở, không gọi F2; ánh xạ bỏ trường thừa dù máy chủ trả | Vitest | SR-AC-04, F4 AC-31 |
| T-SR-14 | Abnormal | EVT-4: F2 lỗi thì hộp input và mong đợi in `-`, hộp thực tế vẫn hiện | Vitest | SR-AC-04 |
| T-SR-15 | Normal | Beats: `beatsPercent` có giá trị, `null` khi `ACCEPTED` thiếu mẫu, `-` khi không `ACCEPTED`, ẩn với A2 | Vitest | SR-AC-08, F4 AC-34 |
| T-SR-16 | Normal | Điểm: `{đạt}/{tổng}`, `-` ở `COMPILE_ERROR` và `SYSTEM_ERROR` | Vitest | SR-AC-08 |
| T-SR-17 | Normal | EVT-5: sao chép mã thành công và thất bại đều có toast | Vitest | SR-AC-01 |
| T-SR-18 | Normal | EVT-6, 7: nút AI bật chỉ khi `ACCEPTED`; điều hướng kèm `submission_id`; mô tả bằng văn bản khi vô hiệu | Vitest | SR-AC-09 |
| T-SR-19 | Normal | EVT-8: "Làm lại" đi `/workspace/{problemId}`; `problem = null` thì vô hiệu | Vitest | SR-AC-09 |
| T-SR-20 | Normal | EVT-2, 3: breadcrumb về `/problems` và trang đọc đề của bài | Vitest | SR-AC-09 |
| T-SR-21 | Normal | A2: `from` lạ về `submissions`; `studentId`, `classId` sai lệch không đổi học viên và lớp hiện ra | Vitest | SR-AC-10, F4 AC-45 |
| T-SR-22 | Normal | Luồng đầy đủ: nộp từ Workspace, mở kết quả khi còn chấm, thấy dòng lấp dần và kết quả cuối | Playwright | SR-AC-02, SR-AC-03 |
| T-SR-23 | Normal | AI chết: trang vẫn hiển thị đủ, hai nút AI vẫn điều hướng | Vitest | SR-AC-09, F4 AC-68 |

## 8. Nợ prototype và câu hỏi mở

**Dòng nợ sẽ gỡ.** Dòng `views/submission-result + entities/submission` của `DEBT` mục 9 (`06-plan/PROTOTYPE_DEBT.md:885`) có "Điều kiện tốt nghiệp: Có `03-dd/api/F4_judge-orchestration.md` (trả lời Q1-Q4) → viết DD → thay mock + nối STOMP → test". DD xong chỉ hoàn thành bước "viết DD"; nối STOMP, thay mock và test **chưa làm** nên dòng này **chưa gỡ**.

Bảng 8.1: Lệch giữa prototype và thiết kế.

| # | Prototype hiện tại | Thiết kế | Nguồn |
| :-: | :--- | :--- | :--- |
| P1 | Toàn bộ mock, không STOMP | BR-SC-01, BR-SC-02 | SRV:11-13 |
| P2 | Mã nguồn bằng `<pre>` | `CodeEditor readOnly` (cần S1) | SRV:309-311; BD:324 |
| P3 | Input, mong đợi mẫu nằm trong chi tiết bài nộp mock | Ghép từ F2 theo `testcaseId` (BR-SC-03) | ES model/types.ts:41-43 |
| P4 | Hằng `BEATS_MIN_ACCEPTED` ở client | Bỏ, dựa vào `beatsPercent = null` | SRV:50-75 |
| P5 | "Làm lại" và breadcrumb dùng `problemSlug` | Dùng `problemId` | SRV:167-169, 532 |
| P6 | Nút AI vô hiệu chỉ có `title` | `aria-describedby` | SRV:322-350 |
| P7 | Không có trạng thái hết hạn 90 ngày hay `NOT_APPLICABLE` | Bảng 3.1 | ES model/types.ts:46-68 |
| P8 | Thực thể bắt buộc `problemSlug`, `problemTitle` | `problem` nullable | ES model/types.ts:49-51 |
| P9 | Mã verdict từng testcase in thẳng | Qua i18n | SRV:567-572; BD:593 |
| P10 | Mock A2 dùng tập id cố định để giả lập phạm vi lớp | Máy chủ kiểm phạm vi (F4 mục 14) | ES api/queries.ts:36-38 |
| P11 | `SubmissionDetail` có `difficulty` kiểu khoá chuỗi | `problem.levelDisplayName` do máy chủ trả | ES model/types.ts:51; F4:486 |
| P12 | Không có vùng `aria-live` | Mục 6 | SRV:140-543 |

**Câu hỏi mở mức màn.**

Bảng 8.2: Câu hỏi mở, mặc định đề xuất và bậc ponytail.

| # | Câu hỏi | Mặc định đề xuất | Bậc |
| :-: | :--- | :--- | :-: |
| OQ-1 | **Đã chốt 2026-10-24** (`DEC-2026-1024-dd-screens-users-core-contract-changes`): F2 mục 8 trả `testcaseId` ở phần tử Sample để ghép với `testcaseId` của F4 mục 8 | Đã chốt, F2 mục 8 và `04-tdd/modules/F2_problem-bank.md` AC-11 đã sửa | 1 |
| OQ-2 | **Đã chốt 2026-10-24** (`DEC-2026-1024-dd-screens-users-core-contract-changes`): UUID `problemId` ở mọi route; `slug` chỉ để hiển thị | Đã chốt, BD `USR0201` Sheet 3 đã sửa (V0.12) | 4 |
| OQ-3 | **Đã chốt 2026-10-24** (`DEC-2026-1024-dd-screens-users-core-contract-changes`): `problem` của F4 mục 8 có `withdrawn: boolean`; `null` vẫn chỉ là lỗi của `problem-bank` | Đã chốt, F4 mục 8, F2 (`ProblemSummaryReadPort`, [SoT: F2:2770]) và `04-tdd/modules/F4_judge-orchestration.md` AC-71 đã sửa | 1 |
| OQ-4 | Đã đóng tại F4 §8: bài nộp `PENDING`/`COMPILING` chưa có testcase | `available=true`, `items=[]`; màn tiếp tục theo dõi trạng thái | 6 |
| OQ-5 | `scoreRatio` (F4-13, RD ví dụ "0.6/1") có in riêng ngoài `{đạt}/{tổng}` không | Giữ như bản dựng: chỉ `{đạt}/{tổng}` vì `scoreRatio = đạt / tổng` (cùng thông tin) | 3 |
| OQ-6 | Tô dòng lỗi biên dịch trong trình xem mã (BD E NO 3) | Không tô: F4 chỉ trả chuỗi, không có số dòng có cấu trúc (F4 Q23). Chỉ hiện khối lỗi | 2 |
| OQ-7 | **Đã chốt 2026-10-24** (`DEC-2026-1024-dd-screens-users-core-contract-changes`): thư viện STOMP là `@stomp/stompjs`, một khách ở `shared/realtime` dùng chung với `USR0102` (cùng OQ-1 của `USR0102`). Mã làm sau | Đã chốt | 1 |
