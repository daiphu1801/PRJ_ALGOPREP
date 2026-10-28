# DD màn hình — Phỏng vấn giả lập (`USR0302`)

> Hợp đồng khung: `03-dd/screens/README.md`. File này chỉ ghi phần chính thức của tầng màn hình: cấu trúc slice, hook, máy trạng thái, quy tắc đặc thù màn, i18n, a11y. Request, response, mã lỗi và lược đồ sự kiện SSE **không** lặp ở đây: nằm ở `03-dd/api/F5_ai-review.md` (REST mục 8 đến 12, SSE mục 2). Logic phía máy chủ nằm ở `03-dd/logic/F5_ai-review.md`. Viết sau prototype (`03-dd/screens/README.md:7`).
>
> **Hiện trạng quan trọng:** prototype chạy **toàn bộ trên mock**: câu hỏi AI là kịch bản tất định, "streaming" là `setInterval` hiện dần ký tự (`05-coding/frontend/src/views/users/mock-interview/model/use-mock-interview-session.ts:79-100`). **Client SSE chưa được dựng** (`05-coding/frontend/src/shared/realtime/README.md:6-9`: "Build it when working on `features/conduct-mock-interview` (SSE)"). Mục 1 và mục 4 định nghĩa việc `features/conduct-mock-interview` phải bọc; hình dạng tin nhắn lấy nguyên từ hợp đồng F5 mục 2.2, không tự đoán thêm.

## 0. Thông tin tài liệu

Bảng 0.1 là bìa tài liệu.

**Bảng 0.1: Bìa**

| Mục | Giá trị |
| :--- | :--- |
| Mã màn, slug | `USR0302`, `mock_interview` |
| Tên màn | Phỏng vấn giả lập |
| Nhóm actor | `users` (A1, `STUDENT`) |
| Route | `/mock-interview` (lối vào tự do), `/mock-interview?sessionId={id}` (xem lại phiên), và `/submissions/{submissionId}/interview` (mở từ bài Accepted) |
| Bounded Context chủ | `ai-review` F5.2; đọc `judge-orchestration` F4 (danh sách bài nộp Accepted), `interview-bank` F6 (câu hỏi, chủ đề), `identity` F1 (tham số mặc định từ cài đặt) |
| RD nguồn | `01-rd/screens/users/USR0302_mock_interview.md` |
| BD nguồn | `02-bd/screens/users/USR0302_mock_interview.md` (V0.14) |
| Prototype | `05-coding/frontend/src/views/users/mock-interview/`, `05-coding/frontend/src/entities/mock-interview/` |
| Phiên bản DD | V0.1, 2026-10-24 |

**Bảng 0.2: Lịch sử sửa đổi**

| Ver | Nội dung | Ngày | Người sửa |
| :--- | :--- | :--- | :--- |
| V0.1 | Tạo mới từ prototype (chưa duyệt hình dựng lại 2026-10-20, BD302 V0.11) và DD module F5 (2026-10-23). Chưa có owner duyệt | 2026-10-24 | Claude |

Bảng 0.3 giải nghĩa các tên viết tắt nguồn.

**Bảng 0.3: Tên viết tắt nguồn**

| Viết tắt | Đường dẫn đầy đủ |
| :--- | :--- |
| RD302 | `01-rd/screens/users/USR0302_mock_interview.md` |
| BD302 | `02-bd/screens/users/USR0302_mock_interview.md` |
| API-F5 §n | `03-dd/api/F5_ai-review.md` mục n |
| API-F4 #n | `03-dd/api/F4_judge-orchestration.md`, số catalog mục 1.1 (kèm mục trong file khi cần) |
| API-F6 §n | `03-dd/api/F6_interview-bank.md` mục n |
| API-F1 #n | `03-dd/api/F1_identity.md`, số trong bảng endpoint |
| LOG-F5 | `03-dd/logic/F5_ai-review.md` (`BR-AI-nn`) |
| VAL-F5 | `03-dd/validation/F5_ai-review.md` |
| TDD-F5 | `04-tdd/modules/F5_ai-review.md` (`AC-nn` của module) |
| TDD-MI | `04-tdd/screens/users/USR0302_mock_interview.md` (`AC-MI-nn`, phần xuyên module của màn) |
| FE | `05-coding/frontend/src/` |
| DEBT | `06-plan/PROTOTYPE_DEBT.md` |

## 1. Bản đồ slice FSD

Bảng 1.1 liệt kê slice màn dùng và slice cần thêm khi nối API thật. Chiều import: `app → views → widgets → features → entities → shared`, entity chỉ được import `shared` (`05-coding/frontend/eslint.config.mjs:5`, `:110-113`), nên mọi phép ghép nhiều entity nằm ở view hoặc feature.

**Bảng 1.1: Slice của màn**

| Lớp | Đường dẫn (từ `FE`) | Vai trò | Hiện trạng |
| :--- | :--- | :--- | :--- |
| views | `views/users/mock-interview/ui/mock-interview-view.tsx` | Ba trạng thái `entry`, `running`, `result` trong một file (`:146-350`, `:352-615`, `:617-693`) | Có (695 dòng) |
| views | `views/users/mock-interview/model/use-mock-interview-session.ts` | Máy trạng thái cấp màn và cấu hình `entry`; hiện cũng chứa cơ chế stream giả (`:79-100`) | Có; phần "chạy phiên" sẽ **chuyển** sang feature |
| features | `features/conduct-mock-interview/` (**mới**): `model/use-interview-run.ts`, `model/reconcile-session.ts`, `model/stream-turn.ts` | Cơ chế chạy một phiên: gửi lượt qua SSE, gom `token`, nối lại, gợi ý, kết thúc, khôi phục. Một mình `USR0302` dùng, nhưng tách khỏi view vì có máy trạng thái nhiều bước và cần test riêng; tên khớp `shared/realtime/README.md:8` | **Chưa có** |
| entities | `entities/mock-interview` (`model/types.ts`, `api/queries.ts`) | Kiểu UI, ánh xạ DTO sang UI (mục dưới), lời gọi REST mục 8, 9, 11, 12, schema zod mục 5 | Có, đang mock; `queries.ts:23-57` còn các hook kịch bản |
| entities | `entities/submission` | Thêm `useMyAcceptedSubmissions` cho API-F4 #23 (mục 24A) | **Chưa có**; hiện danh sách nằm trong `entities/mock-interview` (`useAcceptedSubmissionOptions`, `queries.ts:25-30`) |
| entities | `entities/interview-question` | Thêm `useQuestionShortlist` cho API-F6 §2 và nối `useInterviewTopics` (hiện là store trong bộ nhớ, `entities/interview-question/model/topic-store.ts`) với API-F6 §9 | **Chưa có**; `useBankQuestionOptions` hiện ở `entities/mock-interview` (`queries.ts:32-37`) |
| entities | `entities/user` (`useMySettings`, `entities/user/api/queries.ts:27`) | Tham số mặc định của phiên tự luyện (API-F1 #14: `preferredMaxTurns`, `preferredHintAllowed`; mức người phỏng vấn nằm cùng bản ghi `user_preferences`) | Có, mock |
| shared | `shared/realtime/sse-client.ts` (**mới**, thuần hạ tầng) | Mở `POST` trả `text/event-stream` bằng `fetch` + `ReadableStream` kèm `Authorization`; nhả từng khung thô `{ event, id, data }`; bỏ qua dòng chú thích `: keep-alive`; lỗi trước luồng ném `ApiError` dựng từ phong bì lỗi; luồng đóng mà không có sự kiện cuối ném lỗi "bị ngắt" | **Chưa có** (mục dưới) |
| shared | `shared/api` | `ApiError`, `tokenStore`; **nâng cấp:** xuất hàm làm mới access token (`refreshAccessToken`, hiện riêng của module tại `shared/api/client.ts:22`) cho client SSE dùng, vì `fetch` không đi qua interceptor `axios` (`client.ts:19-50`) | Cần nâng cấp |
| shared | `shared/ui` | Dùng lại: `Badge`, `Button`, `ConfirmDialog` (có `pending`), `EmptyState`, `NoticeTile`, `QueryBoundary`, `SegmentedTabs`, `Skeleton`, `TextArea`, `Toggle`; `shared/lib/toast-store`. **Nâng cấp:** `NoticeTile` thêm prop `role` tuỳ chọn (mặc định `status`, người gọi cũ không đổi): hiện cố định `role="status"` (`shared/ui/feedback/notice-tile.tsx:50`) trong khi khối lỗi giữa phiên cần `role="alert"` (mục 6) | Cần nâng cấp |
| không dùng | `entities/ai-budget` | Chỉ là store tham số của màn quản trị (`ai-budget-store.ts:15-54`). Học viên **không có** endpoint đọc trạng thái ngân sách; chỉ biết bị khoá khi gặp `AIR-401` lúc hành động | Không nối |

**Ánh xạ DTO sang UI** (làm một chỗ, ở `entities/mock-interview/api/`). Prototype dùng chữ thường: `ChatRole` `ai`, `user`; `InterviewStage` `explain`, `challenge`, `scaleUp`; `InterviewerLevel` `intern`, `junior`, `middle`, `senior`; `RubricCriterionCode` `clarity`, ... (`entities/mock-interview/model/types.ts:8-40`). Hợp đồng F5 dùng chữ hoa: `USER`, `AI`; `EXPLAIN`, `CHALLENGE`, `SCALE_UP` (và `COMPLETED`, `ABANDONED`); `INTERN`, `JUNIOR`, `MIDDLE`, `SENIOR`; `CLARITY`, `TECHNICAL_ACCURACY`, `PUSHBACK_HANDLING`, `COMPLEXITY_AWARENESS` (API-F5 mục 8.2, 9.3, 12.3). Quy ước DD: **giữ kiểu UI chữ thường**, hàm ánh xạ hai chiều nằm trong entity; không để chữ hoa rò vào view. `entities/user` đã dùng chữ hoa cho mức người phỏng vấn (`entities/user/model/types.ts:54`): hai entity cùng một khái niệm nhưng khác kiểu là sai lệch D-302-5.

**Client SSE và việc `features/conduct-mock-interview` phải bọc.** Theo hợp đồng F5 mục 2 (`03-dd/api/F5_ai-review.md:112-122`): SSE là **chính phản hồi** của `POST /interview-sessions/{sessionId}/turns`, một chiều máy chủ tới client, `EventSource` gốc không dùng được (chỉ `GET`, không gắn được `Authorization`). Phân vai:

| Thành phần | Biết gì | Không biết gì |
| :--- | :--- | :--- |
| `shared/realtime/sse-client.ts` | Định dạng khung SSE (`event:`, `id:`, `data:`), `AbortSignal`, xử lý lỗi trước luồng, làm mới token một lần nếu `401` trước luồng (an toàn vì lượt chưa ghi), phát hiện luồng bị đóng giữa chừng | Tên sự kiện, trường dữ liệu, `sessionId`, bất kỳ nghiệp vụ AI nào |
| `features/conduct-mock-interview` (`stream-turn.ts`) | Tên bốn loại sự kiện và trường của từng loại **đúng như API-F5 mục 2.2 Bảng 3** (kiểu TypeScript dựng từ bảng đó, không thêm trường); kiểm `seq` tăng liên tục; ghép `text` của `token`; thay nội dung bằng `content` ở sự kiện hoàn tất | Cách hiển thị |
| `features/conduct-mock-interview` (`reconcile-session.ts`) | Quy trình nối lại ở API-F5 mục 2.4 (BR-SC-05) | Cách hiển thị |
| view | Hiển thị, focus, toast | Mọi thứ trên |

## 2. API tiêu thụ

Bảng 2.1 ánh xạ sự kiện BD302 Sheet 8 sang endpoint ở file API và hook. Không ghi lại request hay response; nguồn schema là các mục API được dẫn.

**Bảng 2.1: Sự kiện, endpoint, hook**

| EVT (BD302) | Endpoint (ID) | Hook, query | Thật hay mock |
| :--- | :--- | :--- | :--- |
| EVT-1 Mở màn (khôi phục phiên) | API-F5 §9 `GET /api/v1/ai/interview-sessions/{sessionId}` (`GetSessionDetail`), chỉ khi có `sessionId` trong `sessionStorage` hoặc `?sessionId=` | `useInterviewSession(sessionId)` (**mới**, `entities/mock-interview/api/queries.ts`) | Chưa có (prototype giữ phiên trong `useState`, `use-mock-interview-session.ts:39-57`) |
| EVT-1 (tham số mặc định tự luyện) | API-F1 #14 `GET /api/v1/identity/me/settings` | `useMySettings` (`entities/user/api/queries.ts:27`) | Mock. BD302 không có sự kiện cho bước này, xem D-302-8 |
| EVT-2 Tab Bài Accepted | API-F4 #23 `GET /api/v1/judge/me/accepted-submissions` (mục 24A; BD gọi tên `ListMyAcceptedSubmissions`) | `useMyAcceptedSubmissions` (**mới**, `entities/submission`) | Mock (`useAcceptedSubmissionOptions`, `queries.ts:25-30`) |
| EVT-1 route bài nộp ngoài 10 mục đầu | API-F4 #5 `GetSubmissionDetail`, chỉ khi #23 không chứa `submissionId` của route | `useSubmissionDetail(submissionId)`; chỉ ghim khi thuộc người gọi và `status=ACCEPTED` | Chưa có |
| EVT-3 Tab Kho câu hỏi | API-F6 §2 `GET /api/v1/interview-bank/questions`, trang 1 không lọc | `useQuestionShortlist` (**mới**, `entities/interview-question`) | Mock (`useBankQuestionOptions`, `queries.ts:32-37`) |
| EVT-4 Tab Tự chọn | API-F6 §9 `GET /api/v1/interview-bank/topics` (`ListQuestionTopics`) cho chip chủ đề (BD302 EVT-4 ghi "không gọi API" nhưng BD Sheet 5 V0.9 đã nói chip đọc từ danh mục) | `useInterviewTopics` (`entities/interview-question`) | Store trong bộ nhớ |
| EVT-5, 6, 7, 8, 9 Chọn nguồn, đổi cấu hình | Không có | state của `useMockInterviewSession` | Không áp dụng |
| EVT-10 Bắt đầu phiên | API-F5 §8 `POST /api/v1/ai/interview-sessions` (`StartInterviewSession`) | `useStartInterviewSession` (**mới**, mutation) | Mock (`useNextInterviewQuestion` kịch bản, `queries.ts:42-46`) |
| EVT-11, 12 Ngăn kéo đề và mã | Không dựng (owner hoãn, BD302 mục 4.4 dòng 163). Khi dựng: API-F4 #5 cho mã, API-F2 §6 hoặc §7 cho đề | | Không có |
| EVT-13 Nhập tin nhắn | Không có | state `draft` | Không áp dụng |
| EVT-14, EVT-21 Gửi câu trả lời | API-F5 §10 `POST /api/v1/ai/interview-sessions/{sessionId}/turns` trả `text/event-stream`; **kênh SSE:** hợp đồng ở API-F5 mục 2 (path, sự kiện, thứ tự, nối lại) | `useInterviewRun().sendAnswer` (feature, **mới**) trên `shared/realtime/sse-client.ts` (**mới**) | Mock (`useNextInterviewQuestion`; stream giả `use-mock-interview-session.ts:79-100`) |
| EVT-15 Xin gợi ý | API-F5 §11 `POST /api/v1/ai/interview-sessions/{sessionId}/hint` (`RequestInterviewHint`) | `useInterviewRun().requestHint` | Mock (`useInterviewHint`, `queries.ts:49-50`) |
| EVT-16, 17 Mở, huỷ popup dừng sớm | Không có | state `earlyExitOpen` (`mock-interview-view.tsx:130`) | Không áp dụng |
| EVT-18 Xác nhận dừng sớm; kết thúc tự động ở EVT-14 bước 7 | API-F5 §12 `POST /api/v1/ai/interview-sessions/{sessionId}/complete` (`CompleteInterviewSession`) | `useInterviewRun().complete` | Mock (`useFinishInterview`, `queries.ts:53-57`) |
| EVT-19 Luyện tập lại, EVT-20 Về danh sách | Không có | state, `next/link` tới `/problems` (`mock-interview-view.tsx:688-690`) | Không áp dụng |
| (bổ sung của DD) Nối lại giữa lượt | API-F5 §9 như EVT-1, theo API-F5 mục 2.4 | `reconcileSession` (feature) | Chưa có |

Hợp đồng thời gian thực (không lặp schema): API-F5 §2.1 (kênh, xác thực, `fetch` + `ReadableStream`), §2.2 Bảng 3 (bốn loại sự kiện), §2.3 (thứ tự), §2.4 (nối lại). Hạn chờ phía máy chủ: chunk đầu 10 giây, tổng 60 giây cho một lượt (API-F5 Bảng 4, `03-dd/api/F5_ai-review.md:204-211`); mở phiên và gợi ý 20 giây; chấm điểm 30 giây.

## 3. Trạng thái màn

Bảng 3.1 là máy trạng thái của màn (BD302 mục 4.1 chỉ có ba trạng thái; DD bổ sung các trạng thái con phát sinh từ hợp đồng F5). Bảng 3.2 ánh xạ `error.code` sang hành vi theo endpoint, Bảng 3.3 là trạng thái các danh sách ở `entry`. Frontend không dựa vào `message` (`03-dd/api/api.md:22`).

**Bảng 3.1: Trạng thái của màn**

| Trạng thái | Điều kiện vào | Người học thấy gì | Thoát bằng |
| :--- | :--- | :--- | :--- |
| `restoring` | Mở màn khi có `sessionId` (BR-SC-02, BR-SC-06) | Khung `running` dạng skeleton với chữ ẩn `restoringLabel`; chưa cho nhập | `GetSessionDetail` trả về (BR-SC-06) |
| `entry` | Không có phiên | Ba tab nguồn, cấu hình, nút Bắt đầu | Bấm Bắt đầu |
| `entry` + `starting` | Đã bấm Bắt đầu, chờ API-F5 §8 (tối đa 20 giây, `03-dd/api/F5_ai-review.md:475`) | Vẫn ở `entry`; nút Bắt đầu `disabled` và có chữ `startPending`; mọi điều khiển nguồn và cấu hình khoá; **đồng hồ chưa chạy**. Không có nút Huỷ (không có endpoint huỷ). Khác prototype: prototype chuyển sang `running` rồi mới chờ và quay về `entry` khi lỗi (`use-mock-interview-session.ts:102-117`); DD yêu cầu **ở lại `entry` cho tới khi có phiên** vì phiên không tồn tại trước đó (API-F5 Bảng 4, dòng 207 "phiên chỉ ghi vào DB sau khi có câu mở đầu") | Có phiên: sang `running`; lỗi: ở `entry` kèm toast |
| `running` / `idle` | Có phiên, không có lượt đang xử lý | Khung chat, ô nhập mở | Gửi, gợi ý, kết thúc |
| `running` / `sending` | Đã bấm Gửi, chưa nhận `turn.accepted` | Bong bóng của người học có nhãn `sending`; ô nhập `readOnly`; chỉ báo "AI đang gõ" | Nhận `turn.accepted` hoặc lỗi trước luồng |
| `running` / `streaming` | Đã nhận `turn.accepted` | Bong bóng AI lớn dần theo `token`; ô nhập `readOnly`; không có nút Dừng sinh (máy chủ không huỷ khi client ngắt, API-F5 mục 2.4, nên một nút Dừng sẽ gây hiểu lầm) | `turn.completed` hoặc `error` hoặc luồng bị ngắt |
| `running` / `waiting` | Mất kết nối sau `turn.accepted` và `replyState = GENERATING` (hoặc `AIR-113`) | Dòng `waitingReply`; sau 20 lần hỏi (60 giây) thêm `waitingSlow`, **không** kết luận lỗi | `replyState` đổi (BR-SC-05) |
| `running` / `failedTurn` | Sự kiện `error` sau `turn.accepted`, hoặc `replyState = FAILED` | Câu trả lời của người học vẫn nằm trong khung, nhãn "chưa có phản hồi"; `NoticeTile role="alert"` kèm nút `turnRetry` | Bấm Thử lại (BR-SC-05) |
| `running` / `completionPending` | `turn.completed.shouldComplete = true`, hoặc `AIR-114`, hoặc chấm điểm thất bại | Ô nhập khoá; banner `completionPendingTitle` kèm nút `completionRetry` | Gọi API-F5 §12 thành công |
| `running` / `completing` | Đang chờ API-F5 §12 (tối đa 30 giây) | Banner `completingLabel` (`role="status"`); `ConfirmDialog` nếu đang mở dùng `pending`; mọi nút trong khung khoá | Có kết quả hoặc lỗi |
| `result` | Có `result` (từ §12 hoặc từ §9 với `stage = COMPLETED`) | Vòng điểm tổng, bốn thẻ tiêu chí, nhận xét tổng hợp, banner "phản hồi để học" (mục dưới) | Luyện tập lại, Về danh sách |
| Phiên đã đóng | `stage = ABANDONED` khi khôi phục hoặc khi gửi | Về `entry` kèm toast `toast.sessionAbandoned` (BD302 Q9, chữ đề xuất chưa duyệt); **không** có màn `result` | Làm phiên mới |
| Quyền bị từ chối | `AIR-302` ở §9 đến §12; `401` | `AIR-302`: về `entry` kèm toast `toast.sessionGone`, xoá `sessionId` lưu. `401`: khung xử lý (BD302 Sheet 9 NO 1) | Đăng nhập, hoặc phiên mới |
| AI suy giảm | `AIR-501`, `AIR-502`, `AIR-401` | Chỉ phần cần AI bị ảnh hưởng (mở phiên, gửi lượt, gợi ý, chấm điểm) theo Bảng 3.2; danh sách nguồn, đọc lại kết quả cũ, mọi màn F1 đến F4 chạy bình thường (API-F5 §1.2 mục "Suy giảm êm", `03-dd/api/F5_ai-review.md:91`; BR-AI-11) | Không áp dụng |

Về banner "phản hồi để học" ở `result`: F5-18 yêu cầu hiện rõ cho mọi bảng điểm, và API-F5 §12.3, §9.3 luôn trả `isReferenceOnly = true`; RD302 mục 10 (`01-rd/screens/users/USR0302_mock_interview.md:107-108`) cũng yêu cầu khối này. **BD302 Sheet 5 Khu vực H không có item nào cho banner và prototype không dựng** (`mock-interview-view.tsx:617-693`). DD yêu cầu thêm một `NoticeTile` `info` với khoá `mockInterview.eduNoticeTitle`, `eduNoticeBody`; ghi D-302-6 và Q-302-6.

**Bảng 3.2: `error.code` sang hành vi, theo endpoint**

| Endpoint | error.code | HTTP | Hành vi trên màn | Khoá i18n (tiền tố `mockInterview.`) |
| :--- | :--- | :-: | :--- | :--- |
| §8 mở phiên | `AIR-401` | 429 | Ở `entry`, toast; **không** thử lại tự động; không có nút riêng (BD302 Sheet 9 NO 2) | `toast.budgetLocked` * |
| §8 | `AIR-403` | 429 | Ở `entry`, toast kèm `retryAfterSeconds` | `toast.sessionRateLimited` * |
| §8 | `AIR-404` | 429 | Ở `entry`, toast kèm `retryAfterSeconds` | `toast.cooldown` * |
| §8 | `AIR-501`, `AIR-502` | 503, 502 | Ở `entry`, toast; đã hoàn lại hạn mức nên bấm lại ngay được (TDD-F5 AC-22) | `toast.startFailed` * |
| §8 | `AIR-101`, `AIR-102`, `AIR-302` (lối `SUBMISSION`) | 404, 422, 403 | Toast, tải lại danh sách bài nộp, bỏ chọn | `toast.submissionUnavailable` * |
| §8 | `AIR-V03` đến `AIR-V07` | 400 | Không được xảy ra nếu zod mục 5 đúng; nếu xảy ra: toast chung | `toast.startFailed` * |
| §9 khôi phục | `AIR-110` (404), `AIR-302` (403) | | Xoá `sessionId` lưu, về `entry`, toast | `toast.sessionGone` * |
| §10 gửi lượt, trước luồng | `AIR-V08`, `AIR-V09` | 400 | Không được xảy ra (zod, state); nếu xảy ra: khôi phục bản nháp vào ô nhập, toast chung | `toast.sendFailed` * |
| §10 | `AIR-110`, `AIR-302` | 404, 403 | như §9 | `toast.sessionGone` * |
| §10 | `AIR-111` | 409 | Phiên không còn chạy: `reconcile` (BR-SC-05, BR-SC-08): `ABANDONED` thì `entry` + toast; `COMPLETED` thì `result` | `toast.sessionAbandoned` * |
| §10 | `AIR-112` | 409 | Số lượt lệch: `reconcile`, nạp lại dòng thời gian từ máy chủ, giữ bản nháp trong ô nhập | `toast.turnOutOfSync` * |
| §10 | `AIR-113` | 409 | Phiên đang xử lý lượt khác (tab khác): vào `waiting`, hỏi `GET` mỗi 3 giây (BR-SC-05) | `waitingReply` * |
| §10 | `AIR-114` | 409 | Đã đủ lượt: vào `completionPending`, tự gọi §12 (BR-SC-08) | `completionPendingTitle` * |
| §10 | `AIR-501` trước luồng | 503 | Dòng `USER` **chưa** ghi: gỡ bong bóng lạc quan, khôi phục bản nháp, toast | `toast.sendFailed` * |
| §10 sự kiện SSE `error` | `AIR-501`, `AIR-502` | (đã 200) | `failedTurn` (câu trả lời đã lưu) | `turnFailedTitle`, `turnFailedBody` * |
| §11 gợi ý | `AIR-115` | 422 | Không được xảy ra (nút ẩn khi không cho gợi ý); nếu xảy ra: toast | `toast.hintFailed` * |
| §11 | `AIR-111` | 409 | `reconcile` | như §10 |
| §11 | `AIR-113` | 409 | Toast "phiên đang xử lý"; nút gợi ý mở lại | `toast.sessionBusy` * |
| §11 | `AIR-501` | 503 | Toast; không đổi hội thoại | `toast.hintFailed` * |
| §12 kết thúc | `AIR-116` | 422 | Không được xảy ra nếu BR-SC-08 bước 2 đúng | `toast.completeFailed` * |
| §12 | `AIR-111` | 409 | `reconcile` (`ABANDONED` thì `entry` + toast) | như §10 |
| §12 | `AIR-113` | 409 | Chờ rồi gọi lại như `waiting` | `waitingReply` * |
| §12 | `AIR-501`, `AIR-502` | 503, 502 | Phiên giữ nguyên; vào `completionPending`, bấm Chấm điểm lại được | `completionPendingTitle`, `toast.completeFailed` * |
| mọi endpoint | (không có `error.code`: mạng đứt, hết hạn phía máy khách) | | như `AIR-501` của endpoint đó | như dòng `AIR-501` |

Khoá có dấu sao là khoá **mới**; chữ đề xuất ở Bảng 6.1. Mã `AIR-nnn` chỉ nằm trong bảng ánh xạ này, không xuất hiện trong nhãn UI (`03-dd/screens/README.md:48`).

**Bảng 3.3: Trạng thái danh sách ở `entry`**

| Danh sách | Loading | Rỗng | Lỗi |
| :--- | :--- | :--- | :--- |
| Bài nộp Accepted (API-F4 #23) | skeleton của `QueryBoundary` | `EmptyState` `submissionListEmpty` * kèm liên kết `/problems`; nút Bắt đầu vẫn khoá | `QueryError` kèm Thử lại; **không** ảnh hưởng hai tab còn lại (khác module) |
| Kho câu hỏi (API-F6 §2) | như trên | `EmptyState` `bankListEmpty` * | như trên |
| Chủ đề (API-F6 §9) | như trên | `EmptyState` `topicListEmpty` * | như trên |
| Tham số mặc định từ cài đặt (API-F1 #14) | dùng giá trị mặc định BD (`MIDDLE`, 12 lượt, gợi ý bật, BD302 Sheet 5 Khu vực B) ngay, không chặn màn | không áp dụng | giữ mặc định BD, không toast (không phải lỗi người học cần xử lý) |

Ba danh sách không phụ thuộc AI nên vẫn chạy khi LLM chết (`CLAUDE.md`: F1 đến F4 không phụ thuộc AI).

## 4. Quy tắc đặc thù màn (`BR-SC-nn`)

Chỉ việc không tự suy ra hoặc trải qua nhiều sự kiện. Mọi quy tắc máy chủ (ba giai đoạn, trần 6 lượt mỗi giai đoạn và tổng 18 lượt cho lối `SUBMISSION`, khoá chỉ chặn phiên mới, khoá một lượt mỗi phiên) đã ở LOG-F5 (BR-AI-04, 05, 07, 09) và API-F5; màn không tính lại.

### BR-SC-01 Máy trạng thái và nguồn sự thật

- **Kích hoạt**: mọi sự kiện của màn.
- **Quy tắc**:
  1. **Nguồn sự thật là máy chủ.** `turnCount`, `maxTurns`, `stage`, nội dung dòng thời gian chỉ lấy từ `turn.accepted`, `turn.completed`, `GetSessionDetail`; màn **không** tự tăng `turnCount` hay tự quyết kết thúc khi đủ lượt như prototype (`use-mock-interview-session.ts:145-151`).
  2. Với lối `SUBMISSION`, `maxTurns` là `null` (API-F5 §8.3 `maxTurns | null`; LOG-F5 BR-AI-06). Hệ quả cho giao diện, thay cho BD302 Sheet 5 Khu vực C NO 4 và NO 7 vốn luôn giả định có tổng: hiện `turnCountOpen` ("Lượt {current}") thay cho "Lượt {current} / {total}"; **ẩn** thanh tiến độ theo lượt; dòng "Lượt còn lại" ở thẻ "Phiên hiện tại" hiện "—". Lý do: trần 18 lượt là cấu hình của máy chủ (`ai.interview.stage-turn-ceiling`) không có trong phản hồi nên màn không biết, và không được hiện số suy ra (`03-dd/screens/README.md:49`).
  3. Đồng hồ phiên tính từ `startedAt` của máy chủ (API-F5 §9.3 không có `elapsedSeconds`), nên khôi phục giữ đúng thời gian đã trôi.
  4. Khoá `sessionStorage` lưu `sessionId` (BD302 Q2: `currentSessionId`); tên khoá cụ thể `[SoT: Suy luận]`: `algoprep:mock-interview:session-id`. Ghi khi §8 thành công. Xoá khi: bấm Luyện tập lại, phiên `ABANDONED`, hoặc `AIR-110`/`AIR-302`. **Không** xoá khi vào `result`, để tải lại ở màn kết quả vẫn thấy lại kết quả (đọc từ §9, idempotent).
- **Hậu điều kiện**: tại mọi thời điểm đúng một trạng thái ở Bảng 3.1.

### BR-SC-02 Lối vào, tiền điền và cấu hình theo tab

- **Kích hoạt**: mở route.
- **Các bước**:
  1. `/submissions/{submissionId}/interview` (từ `USR0201`, `USR0301`): trang route truyền `submissionId` xuống `MockInterviewView` làm `initialSubmissionId`. Tab Bài Accepted được chọn sẵn. Nếu #23 (mặc định 10 mục) chưa chứa ID đó, gọi F4 #5 để kiểm chủ và `status=ACCEPTED`, rồi ghim dòng ở đầu danh sách chọn, không thêm trùng. `404`/`403` hoặc trạng thái khác `ACCEPTED` thì không ghim, hiện lỗi nguồn phù hợp và giữ `entry`. `[SoT: DEC-2026-1024-dd-student-dashboard-and-interview-navigation]`
  2. `/mock-interview`: `entry` mặc định, tab Bài Accepted, chưa chọn nguồn (BD302 Sheet 3 NO 1).
  3. **Xem lại phiên cũ** từ `USR0501`/`USR0601`: `/mock-interview?sessionId={id}` gọi F5 §9. Đây là chế độ chỉ xem cho mọi trạng thái: `COMPLETED` hiện `result`, phiên còn chạy hiện dòng thời gian đã lưu nhưng khoá gửi tin, xin gợi ý và kết thúc; `ABANDONED` hiện trạng thái đã bỏ. Không ghi đè `sessionId` trong `sessionStorage`; nút "Luyện tập lại" về `entry`. Khôi phục phiên đang chạy từ `sessionStorage` (không có query) vẫn là chế độ tương tác. `[SoT: DEC-2026-1024-dd-student-dashboard-and-interview-navigation]`
  4. **Tham số tự luyện** (`interviewerLevel`, `maxTurns`, `hintAllowed`): khởi tạo từ API-F1 #14 (`preferredMaxTurns`, `preferredHintAllowed`; mức người phỏng vấn cùng bản ghi `user_preferences`) theo RD302 REQ-11 (`01-rd/screens/users/USR0302_mock_interview.md:174`); chưa tải xong dùng mặc định BD. Người học đổi tại màn chỉ áp cho phiên này, không ghi ngược cài đặt.
  5. **Cấu hình theo tab**: tham số F5-28 chỉ áp cho hai lối tự luyện (RD302 dòng 40, API-F5 §8.2: "bị bỏ qua và lưu NULL" với `SUBMISSION`). Khi tab là Bài Accepted, **ẩn** ba điều khiển (cấp độ, số lượt, gợi ý) và hiện một dòng `submissionConfigNote` *; lời gọi §8 cho lối này **không gửi** ba trường đó. Prototype hiện cả ba ở mọi tab (`mock-interview-view.tsx:278-331`); xem D-302-9, Q-302-5.
  6. **Chip chủ đề**: chọn tối đa **10** (hạn của `topics`, VAL-F5 Bảng 4); khi đủ 10 thì các chip chưa chọn `disabled` và hiện `topicsLimit` *. Mỗi phần tử gửi đi là `displayName` của chủ đề, 1 đến 60 ký tự sau khi cắt (VAL-F5 Bảng 4 đã nâng từ 50, Q-302-3 đã chốt 2026-10-24, `DEC-2026-1024-dd-screens-ai-topic-length-and-rd-trim`).
  7. **Tab Kho câu hỏi**: chọn một câu thì `questionRef` = `id` UUID của F6 và `questionText` = `title` (tối đa 255 ký tự, nằm trong khoảng 1 đến 2.000 của VAL-F5; Q-302-2). **Không** có lối vào "Phỏng vấn câu này" từ `USR0401` (BD302 mục Ghi chú 2026/10/18); liên kết "Xem toàn bộ N câu hỏi" lấy `N` từ `meta.total` của API-F6 §2 và mở `/interview-bank` (BD302 Sheet 3 NO 4).
- **Hậu điều kiện**: nút Bắt đầu chỉ mở khi đã chọn nguồn hợp lệ (BD302 Sheet 9 NO 3; mục 5).

### BR-SC-03 Mở phiên

- **Kích hoạt**: bấm Bắt đầu (EVT-10).
- **Tiền điều kiện**: nguồn hợp lệ (mục 5); chưa ở `starting`.
- **Các bước**:
  1. Dựng thân yêu cầu từ schema zod mục 5; gửi API-F5 §8. Trạng thái `starting` (Bảng 3.1).
  2. Thành công: lưu `sessionId` (BR-SC-01), dựng dòng thời gian từ `openingMessage` (một tin AI, hiện **ngay đủ chữ**: §8 trả REST một khối, không có luồng; không có hiệu ứng gõ giả, ponytail bậc 3), đặt `stage`, `turnCount = 0`, `maxTurns`, bắt đầu đồng hồ từ `startedAt`, sang `running` / `idle`.
  3. Lỗi: theo Bảng 3.2; ở `entry`, giữ nguyên lựa chọn nguồn và cấu hình.
  4. Rời màn khi đang `starting`: huỷ `fetch`. Nếu máy chủ đã kịp tạo phiên thì phiên không có lượt nào và sẽ thành `ABANDONED` sau 15 phút (LOG-F5 BR-AI-09, JOB-AI-04); không dọn gì ở máy khách.
- **Hậu điều kiện**: hoặc đang `running` với đúng một dòng AI, hoặc vẫn ở `entry`; không có trạng thái nửa chừng.

### BR-SC-04 Gửi một lượt và hiện luồng trả lời

- **Kích hoạt**: bấm Gửi hoặc Ctrl/Cmd + Enter (EVT-14, EVT-21).
- **Tiền điều kiện**: `running` / `idle`; `content` hợp lệ (mục 5).
- **Các bước**:
  1. Thêm bong bóng lạc quan của người học (nhãn `sending`), chuyển `sending`, **chưa xoá** bản nháp trong ô nhập (cần để khôi phục nếu lỗi trước luồng; prototype xoá ngay ở `mock-interview-view.tsx:500-501`, `:541`).
  2. Gọi API-F5 §10 qua `stream-turn` với `expectedTurnCount` = `turnCount` hiện có của màn. Lỗi trước luồng (JSON): theo Bảng 3.2; dòng `USER` chưa ghi nên gỡ bong bóng lạc quan và giữ bản nháp.
  3. Nhận `turn.accepted`: xoá bản nháp, bong bóng người học bỏ nhãn `sending`, chuyển `streaming`, tạo bong bóng AI rỗng.
  4. Mỗi `token`: kiểm `seq` tăng liên tục (API-F5 §2.3); cộng `text` vào bong bóng AI. `seq` nhảy cóc hoặc lặp thì coi luồng hỏng và chạy `reconcile` (BR-SC-05) thay vì tin phần đã ghép. Có thể chỉ có **một** `token` chứa toàn bộ câu (đường dự phòng của máy chủ, LOG-F5 BR-AI-07 bước 8): hiển thị bình thường, không giả định số lượng `token`.
  5. `turn.completed`: **thay** nội dung bong bóng bằng `content` chuẩn; cập nhật `stage`, `turnCount`, `maxTurns` từ sự kiện. `stageChanged = true` thì thông báo chuyển giai đoạn (mục 6). `shouldComplete = true` thì sang BR-SC-08 bước 1. Ngược lại về `idle`, trả focus (mục 6).
  6. `error`: theo Bảng 3.2, sang `failedTurn` (BR-SC-05).
  7. Chặn gửi đúp: `busyRef` giữ cho tới khi về `idle` hoặc `failedTurn`; mọi lần bấm trong lúc đó bị bỏ qua (prototype đã có, `use-mock-interview-session.ts:62-63`, `:139`).
- **Hậu điều kiện**: sau `turn.completed` dòng thời gian khớp `GET /interview-sessions/{sessionId}` (API-F5 §2.3 điểm 3).

### BR-SC-05 Mất kết nối giữa lượt, thử lại và nối lại

- **Kích hoạt**: luồng SSE bị đóng khi chưa có `turn.completed` hoặc `error`; sự kiện `error`; `AIR-111`, `AIR-112`, `AIR-113`; tải lại trang.
- **Nền**: phản hồi POST **không nối lại được** (không `Last-Event-ID`); máy chủ **không** huỷ việc sinh khi client ngắt, nên lượt thường hoàn tất ở phía máy chủ (API-F5 mục 2.4, `03-dd/api/F5_ai-review.md:162`).
- **Các bước** (`reconcile-session.ts`, theo đúng quy trình ở API-F5 mục 2.4, không đổi số):
  1. Mất kết nối **trước** `turn.accepted` (câu trả lời chưa chắc đã lưu): gọi API-F5 §9. Nếu `turnCount` chưa tăng và dòng cuối **không** phải `USER`: trả bản nháp về ô nhập, về `idle`, hiện toast `toast.sendFailed`; **không tự gửi lại** (người học thấy và bấm Gửi). Quyết định của màn, không phải của hợp đồng: tự gửi lại được phép về mặt idempotent nhưng người học phải biết tin của mình có thể đã đi.
  2. Mất kết nối **sau** `turn.accepted` hoặc giữa `token`: gọi §9, đọc `replyState`:
     - `GENERATING`: vào `waiting`; hỏi lại §9 mỗi **3 giây, tối đa 20 lần** (60 giây, API-F5 mục 2.4 điểm 2). Hết 20 lần hiện `waitingSlow`, **không** kết luận lỗi; người học tải lại thì §9 vẫn cho trạng thái thật.
     - `IDLE` và dòng cuối là `AI`: lượt đã xong; thay toàn bộ dòng thời gian bằng `turns[]` của §9. Đây là chức năng "Thử tải lại câu trả lời" của BD302 Sheet 9 NO 5, đọc từ Postgres (BD302 V0.14).
     - `FAILED` (dòng cuối là `USER`, không còn lượt đang sinh): vào `failedTurn`. Bấm `turnRetry` gửi lại §10 với `expectedTurnCount` = `turnCount` trả về của §9; máy chủ dùng lại dòng `USER` đó và chỉ sinh lại câu AI, không nhân đôi (API-F5 mục 2.4; TDD-F5 AC-29). Bản thử lại dùng **nguyên văn** câu đã lưu (hợp đồng cho phép sửa nội dung trước khi thử lại, LOG-F5 BR-AI-07 bước 4; sửa nội dung để sau, ponytail bậc 3).
  3. `AIR-112` (lệch số lượt) và `AIR-113` (phiên bận): `AIR-112` thay dòng thời gian bằng §9, giữ bản nháp; `AIR-113` vào `waiting` như `GENERATING` (cùng vòng hỏi).
  4. Khi §9 trả `stage` là `COMPLETED` hoặc `ABANDONED`: BR-SC-09.
  5. Dòng gợi ý lưu cục bộ (BR-SC-07) **mất** khi thay dòng thời gian bằng `turns[]`: chấp nhận (gợi ý không lưu ở máy chủ, API-F5 §11.3).
- **Hậu điều kiện**: câu trả lời của người học có đúng một bản trong hội thoại dù thử lại bao nhiêu lần; số lượt chỉ tăng khi AI trả lời xong (TDD-F5 AC-29).

### BR-SC-06 Rời màn, tải lại, quay lại

- **Kích hoạt**: đóng tab, bấm điều hướng khác, tải lại, quay lại `/mock-interview`.
- **Các bước**:
  1. **Không có hộp thoại cảnh báo rời** (`beforeunload`, `useUnsavedChangesGuard`): phiên không mất khi rời, máy chủ giữ 15 phút (BD302 Q3); thứ duy nhất mất là bản nháp chưa gửi (ponytail bậc 3).
  2. Khi rời lúc đang `sending`, `streaming` hoặc `waiting`: huỷ `fetch` bằng `AbortController`. Máy chủ vẫn hoàn tất lượt (API-F5 mục 2.4). **Không có** endpoint huỷ lượt; màn không cung cấp nút Dừng sinh (Bảng 3.1).
  3. Mở lại màn với `sessionId` trong `sessionStorage`: vào `restoring`, gọi §9 một lần rồi tiếp tục tương tác theo `stage`/`replyState`; đồng hồ tính lại từ `startedAt`. Mở bằng `?sessionId=`: cũng gọi §9 nhưng chỉ dựng dữ liệu đã lưu, mọi thao tác ghi của phiên bị khoá, kể cả khi `stage` còn chạy. Query ưu tiên hơn ID lưu mà không ghi đè ID lưu.
  4. Giữa hai lần mở, phiên có thể đã đóng vì bỏ trống 15 phút: không có hẹn giờ ở máy khách (BD302 Q3, hoãn); người học chỉ biết khi §9 hoặc §10 báo `stage`/`AIR-111`.

### BR-SC-07 Xin gợi ý

- **Kích hoạt**: bấm Xin gợi ý (EVT-15).
- **Các bước**: nút chỉ hiện khi phiên cho gợi ý (`hintAllowed = true`; với lối `SUBMISSION` luôn ẩn vì `hint_allowed` là NULL). Gọi API-F5 §11 (không thân yêu cầu, tối đa 20 giây); trong lúc chờ nút khoá và hiện `hintPending` *. Thành công: thêm một khối **khung riêng** "Gợi ý" vào dòng thời gian, tách hẳn bong bóng AI (vì gợi ý **không** ghi `interview_turns`, không tăng `turnCount`, không đổi `stage`, API-F5 §11.3). Khối chỉ sống ở máy khách; tải lại là mất (Q11 của API-F5 đã chốt mặc định "không lưu").
- **Hậu điều kiện**: `turnCount`, `stage`, dòng thời gian của máy chủ không đổi.

### BR-SC-08 Kết thúc phiên

- **Kích hoạt**: (a) `turn.completed.shouldComplete = true` hoặc `AIR-114`; (b) xác nhận Kết thúc sớm (EVT-18); (c) bấm Chấm điểm lại ở `completionPending`.
- **Các bước**:
  1. (a): **tự gọi** API-F5 §12, không hỏi lại. Điều kiện kích hoạt theo hợp đồng F5 là `shouldComplete` (đủ `maxTurns`, hoặc giai đoạn 3 hoàn tất, hoặc chạm trần giai đoạn 3; API-F5 §2.2 Bảng 3), rộng hơn BD302 EVT-14 bước 7 vốn chỉ nói "đạt `maxTurns`" (D-302-10).
  2. (b): nếu `turnCount = 0` thì **không** gọi §12 (máy chủ trả `AIR-116`, không có gì để chấm). Popup đổi chữ sang `earlyExitNoTurnBody` * và nút xác nhận thành `earlyExitNoTurnConfirm` *; xác nhận chỉ xoá `sessionId` lưu và về `entry`, phiên bị bỏ lại cho job quét (thành `ABANDONED`). Khác BD302 EVT-18 vốn luôn gọi `CompleteInterviewSession` (D-302-11, Q-302-8).
  3. Trong lúc gọi (tối đa 30 giây): `completing`; `ConfirmDialog` dùng `pending`; không có Huỷ.
  4. Thành công: dựng `result` từ phản hồi, **một** toast `toast.finished` cho mỗi lần kết thúc dù qua đường nào (BD302 EVT-14, EVT-18; `mock-interview-view.tsx:133-139`); khi vào `result` bằng khôi phục hay mở lại thì **không** toast.
  5. Lỗi `AIR-501`, `AIR-502`: phiên giữ nguyên (API-F5 §12.4); vào `completionPending` với nút Chấm điểm lại (bước (c)); ô nhập khoá trong lúc đó vì (a) đã tới giới hạn lượt, còn (b) người học đã chọn kết thúc.
- **Hậu điều kiện**: phiên hoặc `COMPLETED` có đúng bốn tiêu chí, hoặc giữ nguyên trạng thái đang chạy; không có điểm một phần.
- **Hiển thị điểm**: `overallScore` và `criteria[].score` thang 0 đến 10; tô màu theo ngưỡng 8 và 5 đã chốt (BD302 Q6); số hiển thị luôn có chữ nên màu không là kênh duy nhất. Trọng số lấy từ `criteria[].weightPercent` của phản hồi (API-F5 §12.3, dòng 765: "trả để giao diện không phải hard-code"), thay cho `RUBRIC_WEIGHTS` hằng ở `entities/mock-interview/model/types.ts:28-33` (D-302-7).

### BR-SC-09 Phiên đã đóng và mở lại kết quả cũ

- **Kích hoạt**: §9, §10, §11 hoặc §12 cho thấy `stage` cuối.
- **Các bước**:
  1. `ABANDONED`: khi khôi phục từ `sessionStorage`, xoá ID lưu, về `entry` và hiện `toast.sessionAbandoned`; khi mở bằng `?sessionId=`, giữ trang chỉ xem với thông báo phiên đã bỏ, không hiện điểm hay hành động ghi. Không có `rubric_scores` (`result = null` ở F5 §9.3).
  2. `COMPLETED` (do người học kết thúc ở tab khác, hoặc do job quét chấm tự động phiên đủ lượt, TDD-F5 AC-36): vào `result` bằng `result` của §9, không toast.
  3. `COMPLETED` nhưng `result = null`: không xảy ra theo thiết kế (4 dòng điểm và `COMPLETED` cùng một giao dịch, LOG-F5 BR-AI-09 bước 6); nếu gặp thì coi là lỗi tạm thời, về `entry` kèm `toast.sessionGone`.
  4. Mọi bảng điểm hiện kèm banner "phản hồi để học" (Bảng 3.1).

### BR-SC-10 AI suy giảm và khoá ngân sách

- **Kích hoạt**: `AIR-401`, `AIR-501`, `AIR-502`.
- **Quy tắc**:
  1. Khoá ngân sách **chỉ** chặn mở phiên mới (§8) và sinh báo cáo mới; lượt, gợi ý, kết thúc, chấm điểm của phiên **đã mở** vẫn chạy (LOG-F5 BR-AI-04 dòng 104, BR-AI-05 dòng 136; TDD-F5 AC-57). Vì vậy `AIR-401` ở §10, §11, §12 **không bao giờ** xảy ra; nếu gặp, xử lý như lỗi lạ (toast chung), không đóng phiên.
  2. Không có endpoint trạng thái ngân sách cho học viên, nên nút Bắt đầu **không** khoá trước; `AIR-401` chỉ báo bằng toast ở `entry` (BD302 Sheet 9 NO 2). Chữ BD ("hôm nay", "ngày mai") sai đơn vị vì ngân sách theo chu kỳ tháng (D-302-12, Q-301-4 ở `USR0301`).
  3. AI chết: mở phiên báo `AIR-501` bằng toast; giữa phiên là `failedTurn` hoặc `completionPending`; đọc kết quả cũ, danh sách nguồn, mọi màn F1 đến F4 không bị ảnh hưởng. Không có bộ ngắt mạch (`DEC-2026-1023-dd-f5-budget-lock-and-breaker`), nên mỗi lần bấm phải chờ tới hạn chờ của lời gọi mới thấy lỗi (LOG-F5 BR-AI-11, `03-dd/logic/F5_ai-review.md:239`): màn phải để nút ở trạng thái chờ rõ ràng, không cho bấm lại.

### BR-SC-11 Câu trả lời của người học và nội dung AI là dữ liệu

Nêu ở mức màn, nơi văn bản người dùng được gửi đi (`CLAUDE.md`, "A submission is data, never an instruction"; `03-dd/screens/README.md:50`):

1. Câu trả lời đi vào **một trường duy nhất `content`** của thân JSON ở §10; màn **không** ghép tiền tố, hậu tố, nhãn giai đoạn hay bất kỳ chỉ thị nào vào chuỗi đó; không lọc từ khoá, không "làm sạch" nội dung (VAL-F5 mục 1: chỉ chặn độ dài).
2. `questionText` (từ `title` của F6) và `topics` đi vào các trường riêng của §8; chúng là **dữ liệu** (API-F5 §8.2, LOG-F5 BR-AI-01); màn không dựng chúng thành câu lệnh.
3. Lời gọi gợi ý (§11) **không có thân**: màn không gửi văn bản nào.
4. Mã bài nộp (lối `SUBMISSION`) **không bao giờ đi qua trình duyệt tới F5**: §8 chỉ mang `submissionId`; máy chủ đọc mã qua `SubmissionSnapshotPort` (LOG-F5 BR-AI-06 bước 2). Cấm thêm trường mã nguồn vào §8.
5. Câu trả lời của AI, gợi ý, `comment` rubric và `feedbackSummary` hiện bằng nút văn bản React (như `mock-interview-view.tsx:466`, `:636`, `:670`), `whitespace-pre-wrap`; **cấm** `dangerouslySetInnerHTML`. Bản dựng hiện chưa render Markdown (BD302 mục 4.4 dòng 153); nếu thêm thì bắt buộc qua `sanitizeHtml()` (`shared/lib/sanitize-html.ts`, `eslint.config.mjs:65`).

## 5. Kiểm tra phía client

zod `3.24.1` đã có (`05-coding/frontend/package.json:35`). Schema đặt ở `entities/mock-interview/model/schemas.ts` (mới), dùng bởi view (chặn nút) và feature (dựng thân). Bảng 5.1 ánh xạ trường sang schema và quy tắc ở VAL-F5; cột "Khớp" nêu lệch.

**Bảng 5.1: Trường, zod, quy tắc VAL-F5**

| Trường | zod | Quy tắc ở VAL-F5 | Khớp |
| :--- | :--- | :--- | :--- |
| Tập trường theo `entryType` | `z.discriminatedUnion("entryType", [...])`, mỗi nhánh `.strict()`: `SUBMISSION` chỉ `submissionId`; `QUESTION_BANK` chỉ `questionRef`, `questionText` và ba tham số; `TOPIC_SELECTION` chỉ `topics` và ba tham số | Bảng 4 dòng 64: bắt buộc và cấm theo lối, `AIR-V03` | Khớp. Lối `SUBMISSION` **không gửi** ba tham số F5-28 (máy chủ bỏ qua, nhưng màn không gửi để khỏi tạo hiểu lầm), Q-302-5 |
| `submissionId` | `z.string().uuid()`, chọn từ danh sách, không nhập tay | `AIR-101` nếu không tồn tại | Khớp |
| `questionRef` | `z.string().uuid()` | Bảng 4 dòng 69: UUID 36 ký tự, `AIR-V07` | Khớp |
| `questionText` | `z.string().trim().min(1).max(2000)` | Bảng 4 dòng 70: 1 đến 2.000 sau khi cắt, `AIR-V07` | Khớp. Nguồn thực tế là `title` của F6 (1 đến 255, `03-dd/validation/F6_interview-bank.md:71`) nên luôn nằm trong khoảng |
| `topics` | `z.array(z.string().trim().min(1).max(60)).min(1).max(10)`; giao diện chặn chọn quá 10 và loại trùng không phân biệt hoa thường | Bảng 4 dòng 68: 1 đến 10 phần tử, mỗi phần tử 1 đến 60, `AIR-V06` | Khớp. Trần 60 trùng tên chủ đề F6 (`03-dd/validation/F6_interview-bank.md:95`) nên mọi chủ đề chọn được đều gửi được; nâng từ 50 theo `DEC-2026-1024-dd-screens-ai-topic-length-and-rd-trim` (Q-302-3 đã chốt 2026-10-24) |
| `interviewerLevel` | `z.enum(["INTERN","JUNIOR","MIDDLE","SENIOR"])` (sau ánh xạ UI sang API) | Bảng 4 dòng 65, `AIR-V04` | Khớp |
| `maxTurns` | `z.union([z.literal(8), z.literal(12), z.literal(16)])` | Bảng 4 dòng 66, `AIR-V05` | Khớp (`MAX_TURNS_OPTIONS`, `entities/mock-interview/model/types.ts:69`) |
| `hintAllowed` | `z.boolean()` | Bảng 4 dòng 67, `AIR-V03` | Khớp |
| `content` (câu trả lời) | `z.string().max(4000).refine(s => s.trim().length >= 1)`; ô nhập có `maxLength=4000`; gửi **nguyên văn** chưa cắt | Bảng 5 dòng 79: còn ít nhất 1 ký tự sau khi bỏ khoảng trắng đầu cuối; `String.length() <= 4000`; không cắt, không chuẩn hoá, `AIR-V08` | Khớp. `.length` của JavaScript đếm đơn vị UTF-16 như `String.length()` của Java; hằng `MESSAGE_MAX_LENGTH = 4000` ở `mock-interview-view.tsx:52` giữ một bản duy nhất |
| `expectedTurnCount` | `z.number().int().nonnegative()`, lấy từ state, người dùng không nhập | Bảng 5 dòng 80, `AIR-V09` | Khớp |

Kiểm theo thời điểm (nút Bắt đầu): chọn đủ nguồn theo tab (BD302 Sheet 9 NO 3; `canStart` ở `use-mock-interview-session.ts:68-71`). Thông báo "Vui lòng chọn một bài nộp, câu hỏi hoặc chủ đề để phỏng vấn" của BD hiện dùng làm gợi ý khi nút khoá, chưa có khoá i18n riêng ngoài `customTopicHint` (`vi.json:2127`).

## 6. i18n và truy cập

**i18n.** Tiền tố `mockInterview.` ở `05-coding/frontend/messages/vi.json:2120-2195` và `en.json:1996`. Bảng 6.1 liệt kê khoá **mới** cần thêm vào cả hai ngôn ngữ; chữ vi là đề xuất, chưa duyệt. Chuỗi kịch bản trong mock (câu hỏi, gợi ý, nhận xét) không qua `t()` (BD302 mục 4.4 nợ (f)); khi nối thật, nội dung AI là chuỗi từ máy chủ, **không** dịch.

**Bảng 6.1: Khoá i18n mới (tiền tố `mockInterview.`)**

| Khoá | Chữ vi đề xuất |
| :--- | :--- |
| `startPending` | Đang mở phiên... |
| `startPendingNote` (chữ ẩn) | Đang chuẩn bị câu hỏi mở đầu, có thể mất tới 20 giây |
| `restoringLabel` (chữ ẩn) | Đang khôi phục phiên phỏng vấn |
| `submissionConfigNote` | Phiên từ bài nộp dùng cấu hình mặc định, không chỉnh cấp độ, số lượt hay gợi ý |
| `topicsLimit` | Tối đa 10 chủ đề |
| `turnCountOpen` | Lượt {current} |
| `sending` | Đang gửi |
| `waitingReply` | Người phỏng vấn đang trả lời |
| `waitingSlow` | Việc này đang chậm hơn thường lệ. Bạn có thể đợi hoặc tải lại trang |
| `turnFailedTitle` | Chưa nhận được phản hồi |
| `turnFailedBody` | Câu trả lời của bạn đã được lưu. Hãy thử lại để nhận phản hồi |
| `turnRetry` | Thử lại lượt này |
| `completingLabel` | Đang chấm điểm phiên, có thể mất tới 30 giây |
| `completionPendingTitle` | Phiên đã đủ để chấm điểm |
| `completionRetry` | Chấm điểm |
| `earlyExitNoTurnBody` | Bạn chưa trả lời lượt nào nên chưa có gì để chấm điểm |
| `earlyExitNoTurnConfirm` | Thoát phiên |
| `hintPending` | Đang lấy gợi ý |
| `hintBlockTitle` | Gợi ý |
| `stageChanged` (chữ ẩn) | Chuyển sang giai đoạn {stage} |
| `eduNoticeTitle`, `eduNoticeBody` | Phản hồi để học, không phải điểm chính thức |
| `submissionListEmpty`, `bankListEmpty`, `topicListEmpty` | Chưa có bài nộp Accepted nào; Chưa có câu hỏi nào; Chưa có chủ đề nào |
| `toast.sessionAbandoned` | Phiên trước đã đóng do bỏ trống quá 15 phút, chưa đủ lượt để chấm điểm (câu của BD302 Q9, chưa duyệt) |
| `toast.sessionGone` | Không mở được phiên này |
| `toast.startFailed` | Chưa mở được phiên. Vui lòng thử lại |
| `toast.budgetLocked` | Bạn đã đạt giới hạn sử dụng AI trong kỳ này (bỏ "hôm nay", Q-301-4) |
| `toast.sessionRateLimited`, `toast.cooldown` | Bạn đã mở đủ số phiên trong giờ này, thử lại sau {seconds} giây; Bạn thao tác hơi nhanh, thử lại sau {seconds} giây |
| `toast.submissionUnavailable` | Không dùng được bài nộp này để phỏng vấn |
| `toast.sendFailed`, `toast.turnOutOfSync`, `toast.sessionBusy`, `toast.hintFailed`, `toast.completeFailed` | Chưa gửi được câu trả lời, hãy gửi lại; Dữ liệu phiên đã được cập nhật; Phiên đang xử lý, thử lại sau ít giây; Chưa lấy được gợi ý; Chưa chấm điểm được, hãy thử lại |

Khoá `mockInterview.startFrom` và các khoá đã có (`vi.json:2120-2195`) giữ nguyên. Khoá mồ côi `startMockInterviewUnavailable` (BD302 mục 4.4 nợ (j)) xoá khi sửa file này; không thuộc namespace.

**A11y.** Chỉ ghi khác biệt so với `shared/ui`:

| Hạng mục | Quy tắc |
| :--- | :--- |
| Vùng hội thoại (streaming) | Giữ `role="log"` `aria-live="polite"` `aria-busy={đang sinh}` (`mock-interview-view.tsx:417-423`). `aria-busy = true` suốt `sending` và `streaming` để trình đọc **không đọc từng đoạn `token`**; khi `turn.completed` thay nội dung bằng `content` chuẩn và `aria-busy` về `false`, câu trả lời được đọc **một lần**. Không đặt `aria-live` lên chính bong bóng đang lớn dần |
| Chỉ báo "AI đang gõ" | Vùng `role="status"` riêng (`mock-interview-view.tsx:472-479`), xuất hiện từ `sending`; nội dung `aiTyping` không đổi theo từng token |
| Chuyển giai đoạn | Chip đã có `aria-current="step"` (`:377`). Khi `stageChanged`, đặt một dòng chữ ẩn `stageChanged` vào vùng `status` (một lần) |
| Thanh tiến độ theo lượt | Thuần trang trí (`aria-hidden="true"`); thông tin nằm ở chữ "Lượt n / m" (`:394-399`). Ẩn hẳn với lối `SUBMISSION` (BR-SC-01) |
| Ô nhập | `TextArea` có nhãn ẩn (`:485-487`); `readOnly` khi AI đang trả lời (giữ focus được, khác `disabled`); thêm `aria-keyshortcuts="Control+Enter Meta+Enter"` và `aria-describedby` trỏ bộ đếm ký tự; bộ đếm **không** nằm trong vùng `aria-live` |
| Trả focus | Khi về `idle` sau một lượt, chuyển focus vào ô nhập **chỉ khi** focus đang ở `body` hoặc ở nút Gửi (BD302 Sheet 6 F.1 "tự focus khi AI hoàn tất"); không giành focus từ điều khiển khác |
| Khối lỗi giữa phiên | `failedTurn` và `completionPending` dùng `NoticeTile` với `role="alert"` (cần nâng cấp prop `role`, mục 1); các khối không cần hành động ngay giữ `status` |
| Popup dừng sớm | `ConfirmDialog` `destructive`; dùng `pending` trong lúc `completing`; chưa kiểm bẫy focus của `Modal`, đưa vào test a11y |
| Điểm | Vòng điểm và thanh tiêu chí có chữ số hiển thị, màu chỉ là tín hiệu phụ; thanh điểm thuần trang trí |
| Thứ tự focus (running) | giai đoạn, vùng hội thoại (cuộn được), ô nhập, Xin gợi ý, Kết thúc sớm, Gửi, cột phải |
| Bàn phím | Ctrl/Cmd + Enter gửi (BD302 EVT-21); mọi nút còn lại là `button` thường; `PickRow` dùng `aria-pressed` (`:92`) |

## 7. Điểm kiểm thử

Bảng 7.1 phủ mọi sự kiện BD302 Sheet 8 và có dòng riêng cho nối lại và mất kết nối của luồng thời gian thực. `AC-nn` của module ở `TDD-F5`; `AC-MI-nn` ở `TDD-MI`. Luồng SSE kiểm bằng luồng giả (`ReadableStream` dựng sẵn) trong Vitest; Playwright dùng máy chủ giả trả `text/event-stream`.

**Bảng 7.1: Điểm kiểm thử màn**

| ID | Loại | Nội dung | Công cụ | AC-nn |
| :--- | :--- | :--- | :--- | :--- |
| TP-302-01 | Normal | EVT-1: có `sessionId` lưu thì `restoring` rồi `running` với đủ `turns[]` đúng thứ tự, đúng `stage` và `turnCount` | Vitest | AC-23 |
| TP-302-02 | Normal | EVT-1: `?sessionId=` của phiên `COMPLETED` vào `result` chỉ đọc, không toast kết thúc | Vitest | AC-MI-04 |
| TP-302-03 | Abnormal | EVT-1: `AIR-110` hoặc `AIR-302` ở §9 thì về `entry`, xoá `sessionId`, toast | Vitest | AC-23 |
| TP-302-04 | Normal | EVT-1/route: `/submissions/{id}/interview` chọn sẵn tab Bài Accepted và bài đó; bài ngoài danh sách được thêm qua API-F4 #5 | Vitest | AC-MI-01 |
| TP-302-05 | Normal | EVT-1: tham số mặc định lấy từ cài đặt (API-F1 #14); chưa tải xong dùng mặc định BD | Vitest | AC-MI-02 |
| TP-302-06 | Normal | EVT-2: tab Bài Accepted hiện danh sách từ API-F4 #23; rỗng hiện `EmptyState` | Vitest | AC-MI-01 |
| TP-302-07 | Abnormal | EVT-2, 3, 4: một danh sách lỗi chỉ ảnh hưởng tab đó, hai tab còn lại dùng được | Vitest | AC-MI-03 |
| TP-302-08 | Normal | EVT-3: chọn câu hỏi F6, bắt đầu: thân §8 có `questionRef` và `questionText` = `title`, không có `submissionId` | Vitest | AC-20, AC-MI-03 |
| TP-302-09 | Boundary | EVT-4/5: chọn tối đa 10 chủ đề, chip thứ 11 bị khoá; chủ đề tên đúng 60 ký tự chọn và gửi được | Vitest | AC-21 |
| TP-302-10 | Normal | EVT-6, 7, 8, 9: tab Bài Accepted ẩn cấp độ, số lượt, gợi ý; thân §8 không có ba trường đó | Vitest | AC-19 |
| TP-302-11 | Normal | EVT-10: bấm Bắt đầu ở lại `entry`, nút `pending`, đồng hồ chưa chạy; khi có phản hồi sang `running` với tin mở đầu hiện đủ chữ | Vitest | AC-18 |
| TP-302-12 | Abnormal | EVT-10: `AIR-401`, `AIR-403`, `AIR-404`, `AIR-501`, `AIR-502` ở lại `entry`, giữ lựa chọn, đúng toast theo Bảng 3.2; bấm lại ngay được | Vitest | AC-22 |
| TP-302-13 | Normal | EVT-13: bộ đếm `{n} / 4000`, nút Gửi chỉ mở khi có chữ và AI không gõ | Vitest | AC-30 |
| TP-302-14 | Boundary | EVT-13: đúng 4000 ký tự gửi được; 4001 không nhập thêm được | Vitest | AC-30 |
| TP-302-15 | Normal | EVT-14: thứ tự `sending`, `streaming`, `idle`; bong bóng AI được thay bằng `content` ở sự kiện hoàn tất; `turnCount` chỉ đổi theo sự kiện | Vitest (luồng giả) | AC-24 |
| TP-302-16 | Normal | EVT-14: `stageChanged = true` làm chip chuyển bước và thông báo một lần | Vitest | AC-25 |
| TP-302-17 | Normal | EVT-14: một `token` duy nhất chứa cả câu vẫn hiển thị đúng | Vitest | AC-24 |
| TP-302-18 | Abnormal | EVT-14: `seq` nhảy cóc thì không tin phần đã ghép, chạy nối lại | Vitest | AC-29 |
| TP-302-19 | Abnormal | EVT-14: lỗi `AIR-501` trước luồng gỡ bong bóng lạc quan, trả bản nháp vào ô nhập | Vitest | AC-29 |
| TP-302-20 | Normal | EVT-14: `AIR-V08` không bao giờ phát sinh vì zod chặn trước | Vitest | AC-30 |
| TP-302-21 | Boundary | Nối lại: mất kết nối sau `turn.accepted`, `replyState = GENERATING` rồi `IDLE`: hiện dòng AI từ `turns[]`, không nhân đôi câu của người học | Vitest | AC-29, AC-MI-07 |
| TP-302-22 | Boundary | Nối lại: `GENERATING` kéo dài 20 lần hỏi thì hiện `waitingSlow`, không báo lỗi | Vitest (đồng hồ giả) | AC-29 |
| TP-302-23 | Abnormal | Nối lại: `replyState = FAILED` hiện `failedTurn`; Thử lại gửi cùng `expectedTurnCount`, hội thoại chỉ có một bản câu trả lời | Vitest | AC-29 |
| TP-302-24 | Abnormal | Mất kết nối **trước** `turn.accepted` và chưa lưu: bản nháp trở lại ô nhập, không tự gửi lại | Vitest | AC-29 |
| TP-302-25 | Abnormal | `AIR-112` nạp lại dòng thời gian, giữ bản nháp; `AIR-113` vào `waiting` | Vitest | AC-28 |
| TP-302-26 | Normal | Rời màn khi `streaming` huỷ `fetch`; mở lại thấy câu AI đầy đủ (máy chủ hoàn tất) | Playwright | AC-29 |
| TP-302-27 | Normal | EVT-15: gợi ý hiện ở khung riêng, không đổi `turnCount`; nút ẩn khi không cho gợi ý và ở lối `SUBMISSION` | Vitest | AC-31 |
| TP-302-28 | Abnormal | EVT-15: `AIR-501` thì toast, hội thoại không đổi | Vitest | AC-31 |
| TP-302-29 | Normal | EVT-16, 17: mở rồi huỷ popup không gọi §12 | Vitest | AC-MI-06 |
| TP-302-30 | Normal | EVT-18: Kết thúc sớm sau 2 lượt gọi §12, `completing`, rồi `result` với đúng 4 tiêu chí, `weightPercent` lấy từ phản hồi, banner "phản hồi để học", một toast | Vitest | AC-32, AC-33 |
| TP-302-31 | Boundary | EVT-18: `turnCount = 0` thì popup đổi chữ và **không** gọi §12 | Vitest | AC-34, AC-MI-06 |
| TP-302-32 | Normal | EVT-14 bước 7: `shouldComplete = true` tự gọi §12; chỉ một toast | Vitest | AC-27, AC-32 |
| TP-302-33 | Abnormal | Chấm điểm `AIR-501` hoặc `AIR-502`: phiên giữ nguyên, `completionPending`, Chấm điểm lại được | Vitest | AC-34 |
| TP-302-34 | Abnormal | `AIR-111` do phiên bị job đóng: `ABANDONED` thì `entry` kèm toast, không màn `result`; `COMPLETED` thì `result` | Vitest | AC-36, AC-MI-04 |
| TP-302-35 | Normal | EVT-19: Luyện tập lại xoá `sessionId` lưu và về `entry` | Vitest | AC-MI-04 |
| TP-302-36 | Normal | EVT-20: Về danh sách dẫn `/problems` (BD302 Sheet 8 NO 20) | Vitest | AC-MI-04 |
| TP-302-37 | Normal | EVT-21: Ctrl/Cmd + Enter gửi như EVT-14, chỉ khi AI không gõ và ô có chữ | Vitest | AC-24 |
| TP-302-38 | Security | Thân §10 chỉ có `content` và `expectedTurnCount`, `content` nguyên văn không tiền tố; §11 không thân; §8 không có mã nguồn | Vitest | AC-30, AC-54 |
| TP-302-39 | Security | Chuỗi AI chứa `<img onerror>` hiện dạng chữ | Vitest | AC-30 |
| TP-302-40 | Boundary | Khoá ngân sách giữa phiên: gửi lượt, gợi ý, kết thúc vẫn thành công; chỉ Bắt đầu phiên mới bị `AIR-401` | Playwright (máy chủ giả) | AC-57 |
| TP-302-41 | Abnormal | AI chết: danh sách nguồn, đọc kết quả cũ chạy bình thường; chỉ thao tác cần AI báo lỗi | Playwright | AC-53 |
| TP-302-42 | Normal | Lối `SUBMISSION` (`maxTurns = null`): hiện "Lượt n", không thanh tiến độ, "Lượt còn lại" là "—" | Vitest | AC-19, AC-MI-05 |

## 8. Nợ prototype và câu hỏi mở

### 8.1 Dòng nợ trong `DEBT`

Dòng `views/mock-interview` + `entities/mock-interview` ở mục 9.1 của `DEBT` (dòng 888) và dòng `USR0302_mock_interview` ở mục 10.2 (dòng 967) giữ mở cho việc nối client SSE và API thật. Tab Kho câu hỏi dùng F6 §2 đã có; banner F5-18 là yêu cầu RD, được đưa vào DD màn, không chờ endpoint hay quyết định mới.

### 8.2 Nợ của prototype so với BD và DD (kiểm khi nối API thật)

Danh sách các việc cụ thể khi nối API thật:

1. Dựng `shared/realtime/sse-client.ts` và xuất `refreshAccessToken` từ `shared/api` (mục 1).
2. Dựng `features/conduct-mock-interview` và chuyển phần chạy phiên ra khỏi `use-mock-interview-session.ts:79-185`.
3. Chuyển danh sách bài nộp Accepted sang `entities/submission`, danh sách câu hỏi sang `entities/interview-question`.
4. Ánh xạ chữ hoa, chữ thường ở entity (mục 1); bỏ `RUBRIC_WEIGHTS` hằng; **kiểu `InterviewResult` của prototype phải bỏ `strengths`, `improvements`** (`entities/mock-interview/model/types.ts:48-54`, D4 đã chốt 2026-10-24, `DEC-2026-1024-dd-screens-ai-topic-length-and-rd-trim`; chưa sửa code ở đợt này, kể cả mock `computeInterviewResult`).
5. Ở lại `entry` khi `starting`, không chuyển `running` trước (Bảng 3.1).
6. Route `/submissions/{submissionId}/interview` đọc `submissionId` và ghim bài qua F4 #5 khi ngoài 10 mục đầu; route `?sessionId=` chỉ xem đã chốt.
7. Ẩn cấu hình ở tab Bài Accepted; chặn chọn quá 10 chủ đề.
8. Banner "phản hồi để học" ở đầu `result` theo RD F5-18.
9. Ngăn kéo "Xem đề và mã" vẫn hoãn (BD302 mục 4.4, nợ (e), dòng 163).
10. `pnpm check` xanh (G-CHECK).

### 8.3 Câu hỏi mở mức màn

Bảng 8.1 liệt kê câu hỏi, mặc định DD đề xuất và bậc ponytail (`.claude/rules/ponytail-criteria.md`).

**Bảng 8.1: Câu hỏi mở của màn**

| # | Câu hỏi | Mặc định đề xuất | Bậc ponytail |
| :-: | :--- | :--- | :-: |
| Q-302-1 | **Đã đóng tại DD:** tên BD `ListInterviewBankShortlist` không phải một endpoint F6 riêng | Dùng F6 §2 `GET /questions` trang 1, không lọc (20 dòng), `meta.total` cho "Xem toàn bộ N câu hỏi"; F5 §1.1 đã sửa mô tả | 4 |
| Q-302-2 | `questionText` gửi lên F5 là `title` (tối đa 255) hay `title` ghép `contentMarkdown` (cần thêm lời gọi API-F6 §3, cắt tới 2.000)? | Chỉ `title`: không thêm lời gọi, nằm trong khoảng hợp lệ, đủ làm ngữ cảnh mở đầu (AI vẫn chất vấn dựa trên câu trả lời, RD302 REQ-09) | 5 |
| Q-302-3 | ~~Chủ đề F6 tối đa 60 ký tự, F5 `topics[]` mỗi phần tử tối đa 50. Sửa phía nào?~~ **Đã chốt 2026-10-24 (`DEC-2026-1024-dd-screens-ai-topic-length-and-rd-trim`):** nâng F5 lên 60 (API-F5 §8.2, VAL-F5 Bảng 4, `AIR-V06`, AC-21 của TDD-F5 đã sửa); gửi `displayName`; bỏ cách tạm vô hiệu hoá chip dài hơn 50 | Đóng | 1, 5 |
| Q-302-4 | Đã đóng: mở lại phiên cũ từ `USR0501`/`USR0601` | `/mock-interview?sessionId={id}` chỉ xem bằng F5 §9; khác khôi phục tương tác từ `sessionStorage` | 5 |
| Q-302-5 | Tab Bài Accepted có hiện cấp độ, số lượt, gợi ý không? RD302 dòng 40 và API-F5 §8.2 nói chỉ áp cho hai lối tự luyện; prototype hiện ở mọi tab. | Ẩn ba điều khiển ở tab Bài Accepted, thay bằng một dòng giải thích; không gửi ba trường | 1 (RD F5-28), 2 (không hiện giá trị không có tác dụng) |
| Q-302-6 | Đã đóng theo RD F5-18: `result` cần banner giáo dục | Thêm `NoticeTile` `info` ở đầu `result`, chữ như `solutionReview.eduNoticeBody`; đồng bộ BD khi rà chéo | 1 |
| Q-302-7 | Đã đóng: bài nộp route nằm ngoài 10 dòng đầu | Gọi F4 #5 kiểm chủ và `ACCEPTED`, ghim một dòng đầu danh sách; không ghim ID sai quyền/trạng thái | 4 |
| Q-302-8 | Kết thúc sớm khi chưa trả lời lượt nào: BD302 EVT-18 luôn gọi §12 nhưng máy chủ trả `AIR-116`. | Popup đổi chữ, xác nhận chỉ rời phiên (xoá `sessionId` lưu), phiên thành `ABANDONED` sau 15 phút; không gọi §12 | 3 |
| Q-302-9 | ~~RD302 còn các khối không có nguồn dữ liệu: bảng "Phiên gần đây" (khoản 4), cột "Mạch chủ đề" và "Ngữ cảnh" (khoản 7), "Chủ đề đã đi qua" và "Việc nên làm tiếp" (khoản 11).~~ **Đã chốt 2026-10-24 (`DEC-2026-1024-dd-screens-ai-topic-length-and-rd-trim`):** không dựng; RD302 đã sửa (ghi chú đầu file, ba khoản gạch bỏ, số khoản không đánh lại); F5-16 còn ở `USR0501` và `USR0601`. Khoản 12 ("Phiên mới", "Ôn chủ đề còn yếu") **giữ nguyên** vì không nằm trong danh sách bỏ | Đóng | 2 |
| Q-302-10 | Chữ khoá ngân sách ở BD302 Sheet 9 NO 2 ("hôm nay", "ngày mai") sai đơn vị (ngân sách theo tháng). | Bỏ đơn vị thời gian ("trong kỳ này"); cùng quyết định với Q-301-4 của `USR0301` | 2 |

### 8.4 Sai lệch phát hiện ở tài liệu và mã khác (DD này không sửa)

Bảng 8.2 liệt kê các sai lệch, kèm đề xuất. D-302-3 là điểm D4 của API-F5 mục 35 Bảng 9 (đã chốt 2026-10-24); D-302-3 và D-302-4 đã đóng.

**Bảng 8.2: Sai lệch**

| # | Nơi | Sai lệch | Đề xuất |
| :-: | :--- | :--- | :--- |
| D-302-1 | BD302 Sheet 7.3 NO 6 | Tên nghiệp vụ cũ `ListInterviewBankShortlist` không có endpoint riêng | Đã đóng ở DD F5 §1.1 và màn này: dùng F6 §2; đồng bộ tên BD ở lượt BD |
| D-302-2 | BD302 Sheet 3 | Không có đường vào mở lại phiên cũ, trong khi BD `USR0501` EVT-5 (`02-bd/screens/users/USR0501_my_progress.md:557`) và API-F5 §13 (`recentSessions.sessionId`, `03-dd/api/F5_ai-review.md:839`) giả định có | Q-302-4 |
| D-302-3 | BD302 Sheet 4.2 DTO `InterviewResultSummaryDto` (dòng 132); `entities/mock-interview/model/types.ts:48-54` | `strengths`, `improvements` không có nguồn dữ liệu (API-F5 §12.3 không trả; không cột DB). View hiện không render chúng nhưng kiểu và mock vẫn mang | **Đã chốt 2026-10-24 (`DEC-2026-1024-dd-screens-ai-topic-length-and-rd-trim`):** BD302 V0.15 đã gạch hai trường khỏi DTO; kiểu `InterviewResult` của prototype phải bỏ chúng (chưa sửa code) |
| D-302-4 | `03-dd/validation/F5_ai-review.md:68` và `03-dd/validation/F6_interview-bank.md:95` | `topics[]` tối đa 50 ký tự (F5) so với tên chủ đề tối đa 60 (F6) | **Đã chốt 2026-10-24 (`DEC-2026-1024-dd-screens-ai-topic-length-and-rd-trim`):** nâng F5 lên 60, đã sửa API-F5 §8.2, VAL-F5 Bảng 4, AC-21 |
| D-302-5 | `entities/mock-interview/model/types.ts:8-40` và `entities/user/model/types.ts:54` | Hai entity cùng khái niệm "mức người phỏng vấn" khác kiểu (chữ thường và chữ hoa) | Ánh xạ ở entity; chọn một kiểu khi nối `entities/user` |
| D-302-6 | BD302 Sheet 5 Khu vực H; `mock-interview-view.tsx:617-693` | Thiếu banner F5-18 ở `result` | Q-302-6 |
| D-302-7 | `entities/mock-interview/model/types.ts:28-33`, `mock-interview-view.tsx:654` | Trọng số rubric hard-code ở máy khách trong khi API-F5 §12.3 trả `criteria[].weightPercent` "để giao diện không phải hard-code" | Đọc từ phản hồi |
| D-302-8 | BD302 Sheet 8 EVT-1; RD302 REQ-11 | Không có sự kiện cho việc tải tham số mặc định từ cài đặt (API-F1 #14); `entities/user` còn hook `useInterviewPreferences` riêng (`entities/user/api/queries.ts:34-40`) trong khi API-F1 #14, #15 đã gộp hai giá trị phỏng vấn vào `user_preferences` (`03-dd/api/F1_identity.md:54-55`) | Thêm bước tải cài đặt vào EVT-1 của BD; bỏ `useInterviewPreferences` khi nối API |
| D-302-9 | BD302 Sheet 6 B.1 đến B.3 | Cấu hình hiện ở mọi tab | Q-302-5 |
| D-302-10 | BD302 Sheet 8 EVT-14 bước 7 | Chỉ nói "đạt `maxTurns`" trong khi hợp đồng F5 dùng `shouldComplete` (cả giai đoạn cuối, trần giai đoạn) và lối `SUBMISSION` không có `maxTurns` | Sửa BD theo BR-SC-08 bước 1 |
| D-302-11 | BD302 Sheet 8 EVT-18 | Luôn gọi `CompleteInterviewSession`, nhưng `turn_count = 0` bị `AIR-116` | Q-302-8 |
| D-302-12 | BD302 Sheet 9 NO 2 | Chữ "hôm nay", "ngày mai" cho ngân sách theo chu kỳ tháng (LOG-F5 BR-AI-05) | Q-302-10 |
| D-302-13 | BD302 Sheet 5 Khu vực C NO 4, NO 7 | Giả định luôn có `maxTurns`; lối `SUBMISSION` có `maxTurns = null` | BR-SC-01 bước 2; sửa BD |
| D-302-14 | `entities/mock-interview/api/queries.ts:25-37` | Hai danh sách thuộc F4 và F6 nằm trong entity `mock-interview` | Chuyển sang `entities/submission` và `entities/interview-question` (mục 1) |
| D-302-15 | `shared/ui/feedback/notice-tile.tsx:7-9`, `:50` | Chú thích hứa truyền được `role` riêng nhưng không có prop | Thêm prop `role` (cùng D-301-7 của `USR0301`) |
| D-302-16 | D5 của API-F5 mục 35 (không thuộc màn này) | `SolutionReviewQuickInsightDto` của `USR0102` nêu `highlights` không có nguồn | Ghi nhận; sửa ở DD `USR0102` |
