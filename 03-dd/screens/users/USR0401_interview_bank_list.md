# DD màn hình — Ngân hàng câu hỏi phỏng vấn (`USR0401`)

> Tuân khung `03-dd/screens/README.md` mục 4 (chín mục 0 đến 8) và quy tắc cứng mục 5. Request, response, mã lỗi nằm ở `03-dd/api/F6_interview-bank.md` và chỉ được liệt kê theo ID. Không có logic backend ở đây. Một màn, hai trạng thái (`browse`, `drill`), **không gọi AI**: chế độ học và tự chấm chạy nguyên vẹn khi F5/F6 phía AI chết hoặc hết ngân sách.

## 0. Thông tin tài liệu

Bảng 0.1 là bìa tài liệu.

Bảng 0.1: Bìa.

| Mục | Giá trị |
| :--- | :--- |
| Mã màn, slug | `USR0401`, `interview_bank_list` |
| Tên màn | Ngân hàng câu hỏi phỏng vấn |
| Nhóm actor | `users` (A1 `STUDENT`; mọi vai trò đã đăng nhập gọi được vì F6 mục 2 đến 5 chỉ đòi JWT, F6:77-79) |
| Route | `/interview-bank`; tham số URL `topic`, `q`, `status`, `level`, `bookmarked=1` (BR-SC-01). Chi tiết một câu ở `/interview-bank/{questionId}` (`USR0402`), `questionId` là UUID |
| Bounded Context đứng sau | F6 `interview-bank` (toàn bộ dữ liệu); liên kết sang F5 `ai-review` (`/mock-interview`) bằng điều hướng, không gọi API |
| RD nguồn | `01-rd/screens/users/USR0401_interview_bank_list.md` |
| BD nguồn | `02-bd/screens/users/USR0401_interview_bank_list.md` (V1.15) |
| Prototype | `05-coding/frontend/src/views/users/interview-bank-list/` (mock, toàn bộ bộ lọc ở `useState`, lọc phía trình duyệt trên cả ngân hàng) |
| Phiên bản, ngày | V0.1, 2026-10-24 |
| Tiêu chí nghiệm thu | `04-tdd/screens/users/USR0401_interview_bank_list.md` phủ trạng thái chọn câu, lọc, luyện nhanh và điều hướng của màn; hợp đồng máy chủ dùng `F6 AC-01` đến `AC-09` và `AC-40`. Mọi điểm mục 7 đã có mã AC truy vết. |

Bảng 0.2 ghi lịch sử sửa.

Bảng 0.2: Lịch sử sửa.

| Ver | Mục | Nội dung | Ngày |
| :--- | :--- | :--- | :--- |
| V0.1 | Toàn bộ | Tạo mới từ prototype, DD F6 và các quyết định `DEC-2026-1023-*`, `DEC-2026-1024-*`. Chốt: bộ lọc nằm trên URL, ghim câu vừa thao tác trên danh sách phân trang máy chủ, thẻ luyện nhanh lấy từ máy chủ theo thứ tự của F6, cách tính "ôn lại sau n ngày", cách tách "Ý cần nói" | 2026-10-24 |

Bảng 0.3 giải nghĩa viết tắt nguồn trích dẫn.

Bảng 0.3: Viết tắt nguồn.

| Viết tắt | Đường dẫn |
| :--- | :--- |
| BD | `02-bd/screens/users/USR0401_interview_bank_list.md` |
| BD402 | `02-bd/screens/users/USR0402_interview_question_detail.md` |
| F6 | `03-dd/api/F6_interview-bank.md` (số sau dấu hai chấm là dòng; "mục n" là số mục trong file) |
| L6 | `03-dd/logic/F6_interview-bank.md` |
| V6 | `03-dd/validation/F6_interview-bank.md` |
| TDD6 | `04-tdd/modules/F6_interview-bank.md` |
| API | `03-dd/api/api.md` |
| IBV | `05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.tsx` |
| IQE | `05-coding/frontend/src/entities/interview-question/` |
| DD-402 | `03-dd/screens/users/USR0402_interview_question_detail.md` |
| DEBT | `06-plan/PROTOTYPE_DEBT.md` |

## 1. Bản đồ slice FSD

Chiều import `app → views → widgets → features → entities → shared`. Bảng 1.1 là bản đồ đích.

Bảng 1.1: Slice của màn.

| Lớp | Đường dẫn (từ `src/`) | Vai trò | Hiện trạng |
| :--- | :--- | :--- | :--- |
| app | `app/(student)/interview-bank/page.tsx` | Render `InterviewBankListView` | Có |
| views | `views/users/interview-bank-list/ui/interview-bank-list-view.tsx` | Trạng thái `browse` và `drill` trong **một file** (513 dòng [SoT: IBV:84-513]). BD 4.5 đề xuất bảy slice `widgets/` và `features/` [SoT: BD:295-305]; DD không tạo: mỗi khối chỉ màn này (và quick view chỉ ở đây) dùng, luật `CLAUDE.md` "một nơi dùng, thuộc một miền, ở lại slice". Tách `DrillSession` thành file riêng khi chạm vào, tuỳ chọn | Có |
| views | `views/users/interview-bank-list/model/use-bank-filters.ts` | Đọc, chuẩn hoá, ghi năm tham số URL (BR-SC-01) | Chưa có |
| views | `views/users/interview-bank-list/model/use-pinned-ids.ts` | Tập id ghim gắn chữ ký bộ lọc (BR-SC-02); logic hiện nằm rải ở IBV:110-121, 193-198 | Chưa có (tách khỏi view) |
| entities | `entities/interview-question/api/queries.ts` | Thay `useInterviewQuestionPage` bằng `useInterviewQuestionList(filters)` (`useInfiniteQuery`, F6 mục 2), `useDrillCards`, `useInterviewQuestion(id)` (F6 mục 3), `useRateRecall` (mục 4), `useSetBookmark` (mục 5), `useQuestionTopics` (mục 9), `useQuestionLevels` (mục 10) | Có, mock, kiểu cũ [SoT: IQE api/queries.ts:24-58] |
| entities | `entities/interview-question/model/types.ts` | **Tách kiểu phía người học** (`QuestionListItem`, `QuestionDetail`, `Recall`) khỏi kiểu quản trị `InterviewQuestion` đang mang `rubric`, `usageCount`, `averageScore`, `attempts`, `defaultRecall`. Kiểu người học **không có** `rubric` (F6:88, 190) | Có, gộp [SoT: IQE model/types.ts:19-54] |
| entities | `entities/interview-question/model/recall-map.ts` | Một chỗ ánh xạ `known`/`vague`/`forgotten` (giao diện) với `KNOWN`/`VAGUE`/`FORGOTTEN` (API) và `new` với `UNSEEN` | Chưa có |
| entities | `entities/interview-question/lib/split-outline.ts`, `lib/next-review.ts` | `splitOutline` (BR-SC-07), `nextReviewDays` (BR-SC-04); dùng chung với `USR0402` | Chưa có |
| entities | `entities/interview-question/model/use-recall-state.ts` | **Bỏ** cho màn người học: kho module giả lập dữ liệu theo người dùng [SoT: IQE model/use-recall-state.ts:1-75]; thật thì bộ nhớ đệm truy vấn là nguồn duy nhất, hai màn chia sẻ qua cùng khoá | Có (xoá khi nối API) |
| entities | `entities/interview-question/ui/` (`InterviewQuestionListRow`, `RecallLevelPicker`, `BookmarkToggle`) | Dùng lại; `InterviewQuestionListRow` đổi props nhận `QuestionListItem` | Có |
| shared | `shared/lib/use-debounced-value.ts` (S1), `shared/lib/use-search-param-state.ts` (S2) | **Nâng lên `shared/lib`**, dùng chung với `USR0202` (DD `USR0202` Bảng 1.1). Hiện chưa có | Chưa có |
| shared | `shared/ui`: `FilterBar`, `FilterMenu`, `Toggle`, `Card`, `Badge`, `ProgressBar`, `EmptyState`, `QueryError`, `SeeAllLink`, `Button`, `MarkdownPreview` (không dùng ở màn này); `shared/lib/toast-store`; `shared/api` `useErrorMessage` (đang chờ dựng [SoT: `05-coding/frontend/src/shared/api/api-error.ts:1-3`]) | Dùng lại | Có |

Việc **không làm**: không dựng `widgets/recall-summary-bar` (đã bỏ 2026-10-18 [SoT: BD:299]); không thêm tính năng "Thêm vào phiên giả lập tới" (Q5 BD đã chốt bỏ).

## 2. API tiêu thụ

Bảng 2.1 ánh xạ sự kiện BD Sheet 8 sang endpoint F6. Hiện mọi dòng gọi API đều mock [SoT: IQE api/queries.ts:1-58].

Bảng 2.1: Sự kiện BD, endpoint theo ID, hook, trạng thái nối.

| EVT (BD) | Endpoint theo ID | Hook | Nối |
| :--- | :--- | :--- | :--- |
| EVT-1 Khởi tạo | F6 mục 9 `GET /topics`, mục 10 `GET /levels`, mục 2 `GET /questions` (trang 1) song song; rồi F6 mục 3 cho dòng được chọn | `useQuestionTopics`, `useQuestionLevels`, `useInterviewQuestionList`, `useInterviewQuestion(selectedId)` | mock |
| EVT-2 Tìm, EVT-3 Trạng thái, EVT-4 Chủ đề, EVT-15 Độ khó, EVT-16 Chỉ câu đã đánh dấu | F6 mục 2, đổi `q`, `status`, `topic`, `level`, `bookmarkedOnly`, trở về trang 1 | `useInterviewQuestionList(filters)` | mock |
| EVT-5 Cuộn tải thêm | F6 mục 2, `page + 1` cùng bộ lọc | `fetchNextPage` của `useInfiniteQuery` | mock |
| EVT-6 Chọn một dòng | F6 mục 3 | `useInterviewQuestion(id)` | mock |
| EVT-7 Đánh dấu | F6 mục 5 `PUT /questions/{id}/bookmark` | `useSetBookmark` | mock |
| EVT-8 Tự chấm (xem nhanh), EVT-13 Tự chấm trên thẻ | F6 mục 4 `PUT /questions/{id}/recall` | `useRateRecall` | mock |
| EVT-11 Bắt đầu luyện nhanh | F6 mục 2 với `drill=true&limit=10` và bộ lọc hiện hành | `useDrillCards` (gọi một lần theo yêu cầu, không lưu lâu) | mock |
| EVT-9 Xem chi tiết, EVT-10 Vào phiên giả lập | Không gọi API | `next/link` tới `/interview-bank/{id}?...` và `/mock-interview` | không áp dụng |
| EVT-12 Hiện/ẩn ý cần nói, EVT-14 Thoát luyện nhanh | Không gọi API | state cục bộ | không áp dụng |

Màn không có kênh thời gian thực. Điểm khác BD khi nối thật:

1. BD ghi `status` trên URL là `all`, `new`, `reviewing`, `known` [SoT: BD:136]; F6 nhận `ALL`, `UNSEEN`, `REVIEWING`, `KNOWN` (F6:129). URL giữ giá trị của BD; một hàm ánh xạ ở `recall-map.ts` đổi `new` thành `UNSEEN` khi gọi.
2. Dòng đếm "Hiển thị {shown} / {total} câu" đọc `meta.shown`, `meta.total` của F6 mục 2 (F6:138-142), **không** phải độ dài mảng đã nạp như bản dựng [SoT: IBV:398-401]; xem BR-SC-02 bước 5.
3. Thẻ luyện nhanh do máy chủ sắp và cắt (F6:134, L6:110). Bản dựng sắp ở trình duyệt theo mức tự chấm [SoT: IBV:71-82, 161-170], khác thứ tự thật: máy chủ xếp "đến hạn" theo `next_review_at`, rồi "chưa từng chấm", rồi phần còn lại.
4. Dòng "Sẽ nhắc ôn lại sau {n} ngày": F6 mục 4 và mục 3 trả `nextReviewAt` (F6:100, 233); `n` do màn tính (BR-SC-04). Bản dựng tra bảng 1/3/7 [SoT: IQE model/types.ts:62-71].
5. Tìm kiếm của BD ghi khớp "tiêu đề, nội dung, từ khoá cốt lõi" [SoT: BD:338]; F6 ghi "tiêu đề, mô tả, từ khoá" (F6:128) nhưng L6:105 so `title`, `content_markdown`, `core_keywords`: cùng ý, không lệch ở màn.

## 3. Trạng thái màn

Bảng 3.1 liệt kê trạng thái theo vùng. Frontend không đọc `message` của lỗi (API:22). Khoá i18n có tiền tố `interviewBankList.`; nhãn lỗi chung dùng `common.error`, `common.retry`.

Bảng 3.1: Trạng thái màn.

| Vùng | Trạng thái | Điều kiện | Hiển thị | Khoá i18n |
| :--- | :--- | :--- | :--- | :--- |
| Menu chủ đề, độ khó | Đang tải | `GET /topics` hoặc `/levels` chưa xong | Khung chờ cho menu (BD:413-414) | không có chữ |
| Menu chủ đề, độ khó | Lỗi | Một trong hai gọi lỗi | Menu đó chỉ còn "Tất cả"; danh sách vẫn xem được; không toast | `filterAll` |
| Danh sách | Đang tải trang đầu | Chưa có dữ liệu | Khung chờ **chín dòng** (BD:422) | không có chữ |
| Danh sách | Rỗng, có lọc | `meta.shown = 0` và có bộ lọc | "Không có câu hỏi khớp bộ lọc." | `emptyFiltered` |
| Danh sách | Rỗng, ngân hàng trống | `meta.total = 0` | "Ngân hàng chưa có câu hỏi nào." | `emptyBank` (mới) |
| Danh sách | Lỗi trang đầu | Lỗi mạng hoặc `5xx` | `QueryError` kèm Thử lại **trong vùng danh sách**; thanh lọc và hàng đầu trang còn (BD:529) | `common.error`, `common.retry` |
| Danh sách | Lỗi trang kế | `fetchNextPage` lỗi | Giữ các dòng đã nạp; dòng cuối danh sách có chữ lỗi và nút Thử lại (BD:533) | `loadMoreFailed` (mới), `common.retry` |
| Danh sách | Đang nạp lại | Đổi bộ lọc, đã có dữ liệu cũ | Giữ dòng cũ, `aria-busy`, mờ nhẹ | không có chữ |
| Danh sách | Lỗi sau đổi bộ lọc | Lỗi khi nạp theo bộ lọc mới | Khôi phục giá trị bộ lọc ngay trước (BD:531-532, 543) và `toast.error` | `toast.loadFailed` (mới) |
| Xem nhanh | Chưa chọn | Danh sách rỗng | Câu hướng dẫn chọn câu hỏi | `quickView.selectPrompt` |
| Xem nhanh | Đang tải chi tiết | `GET /questions/{id}` chưa xong | Chủ đề, độ khó, tiêu đề đã có từ dòng danh sách hiện ngay; "Ý cần nói", từ khoá và khối tự chấm là khung chờ (BR-SC-03) | không có chữ |
| Xem nhanh | Lỗi chi tiết | Lỗi mạng hoặc `5xx` | Lỗi trong cột phải kèm Thử lại; danh sách bên trái giữ nguyên (BD:534) | `quickView.loadFailed` (mới) |
| Xem nhanh, danh sách | Câu không còn khả dụng | `404 INB-010` ở F6 mục 3, 4 hoặc 5 (câu bị xoá mềm lúc đang xem; F6:84) | Bỏ dòng khỏi bộ nhớ đệm, chọn dòng đầu; toast | `toast.unavailable` (mới), `apiError.INB-010` |
| Tự chấm | Lỗi | Lỗi mạng, `5xx` ở F6 mục 4 | Giữ mức cũ; toast; không chặn thao tác khác (BD:536) | `toast.rateFailed` (mới) |
| Đánh dấu | Lỗi | Lỗi ở F6 mục 5 | Hoàn trạng thái về như cũ; toast (BD:535) | `toast.bookmarkFailed` (mới) |
| Luyện nhanh | Lỗi bắt đầu | Lỗi ở F6 mục 2 `drill` | Không chuyển trạng thái; ở lại `browse`; toast (BD:539) | `toast.drillFailed` (mới) |
| Luyện nhanh | Không có thẻ | Máy chủ trả mảng rỗng | Ở lại `browse`; toast "Không có câu hỏi nào khớp bộ lọc để luyện." (BD:563) | `toast.drillEmpty` (mới) |
| Luyện nhanh | Lỗi chấm một thẻ | Lỗi ở F6 mục 4 | **Vẫn sang thẻ kế**; toast cảnh báo lần tự chấm chưa ghi được (BD:541) | `toast.drillRateFailed` (mới) |
| Toàn màn | Chưa đăng nhập | `401` sau khi làm mới token thất bại | Chuyển về `auth` (xử lý ở `shared/api/client.ts`) | không áp dụng |
| Toàn màn | Quyền bị từ chối | Không có: mục 2 đến 5, 9, 10 chỉ đòi JWT, không `INB-301`, `INB-302` (F6:77-79) | Không có trạng thái | không áp dụng |
| AI suy giảm | F5 hoặc nhà cung cấp LLM chết, ngân sách token khoá | Bất kỳ | **Không có trạng thái nào**: màn không gọi `AnswerFeedbackPort`, F6 mục 2 đến 5, 9, 10 không chạm AI (F6:87, `L6:111`; `TDD6:60` `AC-06`, `AC-40`). Nút "Vào phiên giả lập" chỉ điều hướng; trạng thái AI hiện ở `USR0302` | không áp dụng |

## 4. Quy tắc đặc thù màn

### BR-SC-01 Bộ lọc nằm trên URL

- **Kích hoạt:** EVT-1; EVT-2 đến EVT-4; EVT-15; vào từ `USR0402` (nút Quay lại) hoặc từ `solution_review`.
- **Tham số:** `topic` (mã chủ đề, một giá trị), `q`, `status` (`all|new|reviewing|known`), `level` (mã độ khó, một giá trị), `bookmarked=1` khi bật công tắc "Chỉ câu đã đánh dấu"; tắt thì bỏ tham số [SoT: BD:116, 134-136; `DEC-2026-1025-dd-interview-bookmark-url`]. Giá trị mặc định (`all`, rỗng, tắt) không ghi lên URL.
- **Các bước:**
  1. Đọc URL qua schema zod có `.catch` (mục 5). `status` lạ về `all`, không toast.
  2. `topic`, `level` đối chiếu với danh mục F6 mục 9, 10 **sau khi tải xong**: không khớp mã nào thì bỏ tham số, giữ "Tất cả" và hiện **một** toast "Bộ lọc trong đường dẫn không hợp lệ, đã bỏ qua." (BD Sheet 9 NO 4 [SoT: BD:561]). Danh mục tải lỗi thì không kiểm, không toast, giữ nguyên tham số gửi cho máy chủ (máy chủ bỏ tham số không khớp, V6:31).
  3. Ô tìm kiếm giữ chữ gõ ở state cục bộ; `q` trên URL và gọi API chỉ **sau 400 ms** ngừng gõ (BD:530); Enter đẩy ngay và hiện toast số câu tìm thấy (đã có [SoT: IBV:215-222]).
  4. Mọi đổi bộ lọc xoá danh sách đã nạp và nạp trang 1 theo khoá truy vấn mới; ghi URL bằng `router.replace` (lý do như DD `USR0202` BR-SC-01 bước 4).
- **Vào từ `solution_review`:** BD cho phép nhận `topic`, `q` [SoT: BD:109-126]; DD `USR0301` hiện chỉ mở `/interview-bank` không tham số vì `followUpTopics` là chuỗi chứ không phải mã chủ đề [SoT: `03-dd/screens/users/USR0301_solution_review.md:210`]. Màn vẫn đọc hai tham số (không tốn thêm), nhưng chưa có nơi gửi chúng, xem D-2.
- **Hậu điều kiện:** URL mô tả đúng danh sách đang hiện; người dùng chuyển sang `USR0402` rồi quay lại thấy đúng bộ lọc.
- **Hiện trạng:** prototype giữ mọi bộ lọc ở `useState`, không đọc URL [SoT: IBV:100-108; BD:585].

### BR-SC-02 Ghim câu vừa thao tác trên danh sách phân trang máy chủ

- **Kích hoạt:** tự chấm thành công (EVT-8, EVT-13), bật hoặc tắt đánh dấu (EVT-7).
- **Mục đích:** dòng vừa chấm hoặc vừa bỏ đánh dấu ở lại danh sách và xem nhanh dù bộ lọc đang áp sẽ loại nó, để người học kịp đọc dòng "ôn lại sau n ngày" (BD:536; owner 2026/10/19 [SoT: BD:76]).
- **Các bước:**
  1. Cập nhật bằng **vá bộ nhớ đệm**, không tải lại: `setQueryData` đổi `recallRating`, `bookmarked` của phần tử cùng `id` ở mọi trang đã nạp của danh sách, và `recall`, `bookmarked` ở khoá chi tiết của `id` đó. Không `invalidateQueries` danh sách sau thao tác; vì vậy dòng không biến mất.
  2. Truy vấn danh sách đặt `refetchOnWindowFocus: false` và `staleTime: Infinity` trong một lượt mở màn, và `refetchOnMount: "always"`. Lý do: tải lại tự động (lấy lại focus) sẽ làm dòng đã ghim biến mất giữa chừng; chỉ ba việc làm danh sách tải lại là đổi bộ lọc, mở lại màn (kể cả quay lại từ `USR0402`, BD:138), tải lại trang. Đúng chữ ký ghim của bản dựng: `[query, status, topic, level, bookmarkedOnly]` [SoT: IBV:113-121].
  3. Cuộn tải trang kế: gộp theo `id`, bỏ trùng.
  4. **Giới hạn đã biết:** F6 phân trang theo vị trí (`page`, 20 dòng). Khi máy chủ đã loại một câu vừa chấm khỏi tập khớp, trang kế dịch lên một dòng và một câu có thể bị bỏ sót tới khi nạp lại. Chấp nhận ở quy mô đồ án (ponytail bậc 3) `[SoT: Suy luận]`.
  5. Dòng đếm: `shown` và `total` lấy từ `meta` của **trang đầu** của khoá bộ lọc hiện hành (ảnh chụp lúc đổi bộ lọc, trước mọi ghim), không lấy từ trang sau và không đếm `items.length`. Nhờ vậy số đếm khớp tập dòng người học đang thấy (gồm cả dòng ghim, đúng BD:356).
- **Hậu điều kiện:** đổi bộ lọc hoặc ô tìm kiếm thì danh sách nạp mới và không còn dòng ghim nào.

### BR-SC-03 Chọn dòng và xem nhanh

- **Kích hoạt:** EVT-1 (chọn sẵn dòng đầu), EVT-6, đổi bộ lọc.
- **Các bước:**
  1. `selectedId` là `id` (UUID) của câu, không phải mã hiển thị. Sau khi trang 1 về, chọn dòng đầu (BD:529 bước 4). Đổi bộ lọc: giữ nếu câu còn trong các dòng đã nạp, ngược lại chọn dòng đầu của danh sách mới (EVT-15 [SoT: BD:543; IBV:157-159]).
  2. Bản xem nhanh gọi F6 mục 3 cho `selectedId`. Chủ đề, độ khó, tiêu đề lấy ngay từ dòng danh sách (F6:140) để cột phải không trống trong lúc chờ; "Ý cần nói", từ khoá và `recall` đến từ mục 3.
  3. Chọn A rồi B liên tiếp: chỉ chi tiết của `selectedId` hiện hành được vẽ; phản hồi đến muộn của A nằm trong bộ nhớ đệm theo khoá của A, không ghi đè.
  4. `404 INB-010` ở mục 3: bỏ dòng khỏi mọi trang đã nạp, chọn dòng đầu, toast (Bảng 3.1).
- **Hậu điều kiện:** chọn dòng không cuộn lại đầu danh sách (BD:534).

### BR-SC-04 Tự chấm, đánh dấu và dòng "ôn lại sau n ngày"

- **Tự chấm (không lạc quan):** ba nút của câu đó `disabled` trong lúc gửi; thành công: vá bộ nhớ đệm bằng `RecallDto` (BR-SC-02), toast `toast.rated`; lỗi: giữ mức cũ, `toast.rateFailed` (BD:536). Lý do không lạc quan: F6 mục 4 **không idempotent** (mỗi lần đặt lại `rated_at`, F6:224), nên không gửi trùng.
- **Đánh dấu (lạc quan):** đảo ngay rồi gửi **trạng thái đích** (F6 mục 5 là `PUT` idempotent, F6:265, 271); lỗi thì hoàn lại và `toast.bookmarkFailed`. Hai lần bấm nhanh cùng một câu: xếp hàng tuần tự theo `id` và lần sau tính trạng thái đích từ giá trị lạc quan hiện tại, để hai phản hồi đến sai thứ tự không đảo ngược nhau `[SoT: Suy luận]` (dùng `scope.id` của mutation TanStack Query v5, kiểm khi code).
- **`n`:** `nextReviewDays(nextReviewAt, now) = ceil((nextReviewAt − now) / 86 400 000)`. `n ≥ 1` hiện "Sẽ nhắc ôn lại sau {n} ngày"; `n ≤ 0` (câu `KNOWN` đã quá hạn, F6:170 B2) hiện "Đã đến hạn ôn lại" (`quickView.nextReviewDue`, mới). Ngay sau khi chấm `n` bằng đúng 1, 3 hoặc 7 vì `nextReviewAt = rated_at + intervalDays` (F6:242). Cách làm tròn và chữ "đến hạn" là `[SoT: Suy luận]`: BD chỉ nói "số ngày từ bây giờ tới `next_review_at`" [SoT: BD:369]. Chữ "đến hạn" khớp tên của F6 ("đến hạn" khác "đang ôn", L6:136-139).
- Dòng chỉ hiện khi câu đang chọn đã có `recall`.

### BR-SC-05 Luyện nhanh

- **Kích hoạt:** EVT-11 đến EVT-14. **Tiền điều kiện:** `meta.shown > 0` và danh sách không đang tải (nút vô hiệu ngược lại, BD:399).
- **Các bước:**
  1. Gọi F6 mục 2 với **đúng bộ lọc hiện hành**, `drill=true`, `limit=10` (hằng số giao diện `DRILL_SIZE`, [SoT: IBV:48-50; BD:324]). Nhãn nút "Luyện nhanh {count} câu" với `count = min(10, meta.shown)`.
  2. **Không sắp lại ở trình duyệt:** thứ tự do máy chủ (L6:110): đến hạn theo `next_review_at`, rồi chưa chấm, rồi còn lại. Bỏ `drillPriority` của bản dựng [SoT: IBV:71-82, 163-169].
  3. Có thẻ thì vào `drill`, thẻ đầu chưa lộ ý cần nói; không có thì `toast.drillEmpty`, ở lại `browse`. Phiên là danh sách cố định đã nhận: bộ lọc và URL **không đổi** suốt phiên.
  4. "Xem ý cần nói" chỉ đảo state cục bộ (dữ liệu thẻ đã có, F6:140); thẻ có `suggestedApproach` rỗng hoặc `null` thì **ẩn nút** (không có gì để lộ) `[SoT: Suy luận]`.
  5. Tự chấm một thẻ: gọi F6 mục 4 **không chờ phản hồi**, sang thẻ kế ngay (BD:541). Lỗi: `toast.drillRateFailed`, vẫn ở thẻ kế. Mỗi câu đã chấm trong phiên được thêm vào tập ghim (BR-SC-02) và vá bộ nhớ đệm.
  6. Hết thẻ: về `browse`, `toast.drillDone` với `count` là số thẻ của phiên. Thoát giữa chừng (EVT-14): không hỏi xác nhận, không gọi API (BD:542).
- **Hậu điều kiện:** nhãn trạng thái trên danh sách đã đổi theo các thẻ vừa chấm mà không cần tải lại.

### BR-SC-06 Sang chi tiết và quay lại

- **Kích hoạt:** EVT-9 (bấm "Xem chi tiết").
- **Các bước:** liên kết `/interview-bank/{selectedId}` kèm `topic`, `q`, `status`, `level`, `bookmarked=1` lấy từ URL **đã chuẩn hoá** hiện hành, chỉ tham số khác mặc định [SoT: BD:134-136; `DEC-2026-1025-dd-interview-bookmark-url`]. `selectedId` đi bằng đường dẫn. Bản dựng liên kết bằng `code` ("IQ-nnn") và không mang bộ lọc [SoT: IBV:498-503]; đổi sang `id` (IB-20 [SoT: DEBT:2127]). Màn không giữ thay đổi chưa lưu nên không hỏi xác nhận (BD:537).
- **Quay lại:** `USR0402` trả cả năm tham số (DD-402 BR-SC-06); màn này mở lại với danh sách nạp mới (BR-SC-02 bước 2) và chọn dòng đầu (BD402:168).

### BR-SC-07 Hiển thị "Ý cần nói"

- **Quy tắc:** `suggestedApproach` là **một chuỗi Markdown**, không phải mảng (F6:186); bản dựng giả định mảng [SoT: IQE model/types.ts:40]. `splitOutline(markdown)` tách thành các mục: mỗi dòng bắt đầu bằng `- `, `* ` hoặc `n. ` là một mục; dòng khác không rỗng nối vào mục ngay trước, hoặc là một mục riêng nếu đứng đầu `[SoT: Suy luận]` (F6:1094 Q8 chỉ nói CSV lưu mỗi mục một dòng `- mục`). Kết quả hiện dạng danh sách đánh số hai chữ số, mỗi mục một ô (BD:365). Mảng rỗng thì ẩn cả khối (BD:590). "Bẫy thường gặp" là một mục tiêu đề nằm trong chuỗi, không tách riêng (Q1 `USR0402` đã chốt [SoT: BD402:680]).
- **Dùng chung** `splitOutline` với DD-402 (Chế độ học) và thẻ luyện nhanh. Mục chữ được dựng bằng nút văn bản của React; **cấm** `dangerouslySetInnerHTML`.

## 5. Kiểm tra phía client

Màn không có form nhập ghi. Bảng 5.1 là giá trị đầu vào màn chuẩn hoá; máy chủ vẫn kiểm theo V6 (đọc rộng, V6:20).

Bảng 5.1: Trường, zod, quy tắc V6, khớp.

| Trường | zod | Quy tắc ở V6 | Khớp |
| :--- | :--- | :--- | :--- |
| `q` | Ô nhập `maxLength = 100` [SoT: IBV:53, 346]; khi dựng tham số `z.string().transform((s) => s.trim().slice(0, 100)).catch("")` | V6:29: cắt khoảng trắng, tối đa 100, cắt không lỗi | Khớp |
| `status` | `z.enum(["all","new","reviewing","known"]).catch("all")`, ánh xạ `new` thành `UNSEEN` | V6:30: `ALL`, `UNSEEN`, `REVIEWING`, `KNOWN`, sai thì `ALL` | Khớp; **tên giá trị khác** có chủ ý: URL theo BD, API theo F6, một hàm ánh xạ |
| `topic`, `level` | `z.string().optional()`; đối chiếu danh mục đã tải ở BR-SC-01 | V6:31: mã có trong danh mục, không thì bỏ qua | Khớp; client thêm toast (BD Sheet 9 NO 4) |
| `bookmarkedOnly` | `z.boolean()`; mặc định `false` | V6:32 | Khớp |
| `page` | Không trên URL; `useInfiniteQuery` quản lý | V6:33: từ 1, `pageSize` cố định 20 | Khớp |
| `drill`, `limit` | `drill = true`, `limit = 10` hằng số | V6:34-35: `limit` 1 đến 20, mặc định 10 | Khớp |
| `rating` | `z.enum(["KNOWN","VAGUE","FORGOTTEN"])` sau ánh xạ; không có giá trị "chưa xem" | V6:44 | Khớp |
| `bookmarked` | `z.boolean()` (boolean thật) | V6:45 | Khớp |

Không kiểm `questionId` ở client: id lạ và câu đã xoá mềm đều do máy chủ trả `INB-010` (V6:28).

## 6. i18n và truy cập

**Tiền tố khoá:** `interviewBankList.*` ở `messages/vi.json:1943-1994` và `messages/en.json:2073`. Khoá **thêm** ở cả hai file: `emptyBank`, `loadMore`, `loadMoreFailed`, `quickView.unavailable`, `quickView.loadFailed`, `quickView.nextReviewDue`, `toast.filterInvalid`, `toast.loadFailed`, `toast.rateFailed`, `toast.bookmarkFailed`, `toast.drillFailed`, `toast.drillEmpty`, `toast.drillRateFailed`, `toast.unavailable`. Khoá **bỏ**: `startMockInterviewUnavailable` (không còn dùng [SoT: BD:596]).

**Không đặt tên endpoint, mã lỗi hay thuật ngữ backend vào nhãn.** Nhãn ba mức tự chấm (Biết rõ, Mơ hồ, Quên) và bốn giá trị lọc trạng thái (Tất cả, Chưa xem, Đang ôn, Đã thuộc) giữ như `vi.json:1950-1964`; "Chưa xem" chỉ là giá trị lọc và nhãn dòng, không bao giờ là nút tự chấm (BD:339).

**Truy cập** (chỉ ghi khác biệt so với `shared/ui`):

| Hạng mục | Quy tắc |
| :--- | :--- |
| Dòng đếm | Dòng "Hiển thị {shown} / {total} câu" bọc `role="status"` để đọc một lần mỗi khi bộ lọc đổi xong |
| Danh sách | Mỗi dòng là nút chọn (`aria-pressed` đã có [SoT: `05-coding/frontend/src/entities/interview-question/ui/interview-question-list-row.tsx:59-65`]) cộng nút đánh dấu có tên rõ (`bookmarkLabel`, `aria-pressed`) |
| Cuộn tải thêm | Cuộn vô hạn không dùng được bằng bàn phím: ngoài bộ quan sát cuối danh sách, luôn có **nút "Xem thêm"** (`loadMore`) hiển thị khi `hasNextPage` |
| Bản xem nhanh | Không `aria-live` khi chọn dòng khác (tránh đọc dài); tên vùng `aria-label` là tiêu đề câu |
| Tự chấm | `RecallLevelPicker` đã `role="group"` có nhãn và `aria-pressed` [SoT: IQE ui/recall-level-picker.tsx:34-48]; `disabled` trong lúc gửi |
| Luyện nhanh | Sau mỗi lần chấm đưa focus tới tiêu đề thẻ mới (`tabIndex = -1`); nút "Xem ý cần nói" có `aria-expanded` [SoT: IBV:293]; thanh tiến độ có nhãn `drill.progressLabel`; khi kết thúc hoặc thoát trả focus về nút "Luyện nhanh" |
| Thứ tự focus | Tiêu đề, hai nút đầu trang, ô tìm kiếm, ba menu, công tắc, danh sách, nút "Xem thêm", cột xem nhanh (tự chấm, "Xem chi tiết") |
| Màu | Nhãn trạng thái luôn có chữ trong `Badge` |

## 7. Điểm kiểm thử

Bảng 7.1 phủ mọi sự kiện BD Sheet 8. Vitest sẵn có ở `interview-bank-list-view.test.tsx` (6 ca [SoT: `05-coding/frontend/src/views/users/interview-bank-list/ui/interview-bank-list-view.test.tsx:35-125`]). Cột AC trỏ `F6 AC-nn` (trong `TDD6`).

Bảng 7.1: Điểm kiểm thử.

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| T-IL-01 | Normal | EVT-1: ba khu vực tải song song, chọn dòng đầu, xem nhanh hiện; dòng đếm lấy `meta.shown` và `meta.total` | Vitest | F6 AC-01 |
| T-IL-02 | Boundary | EVT-1: danh mục chủ đề hoặc độ khó lỗi chỉ còn "Tất cả", danh sách vẫn xem được | Vitest | TDD màn `IL-AC-01` |
| T-IL-03 | Normal | EVT-2: gõ liên tiếp gọi API một lần sau 400 ms; `q` lên URL bằng `replace`; Enter gọi ngay và toast | Vitest (đồng hồ giả) | F6 AC-01 |
| T-IL-04 | Normal | EVT-3 đến EVT-4, EVT-15, EVT-16: các bộ lọc cộng dồn `VÀ`; `status=new` gửi `UNSEEN`; đổi bộ lọc trở về trang 1 | Vitest | F6 AC-01, AC-02 |
| T-IL-05 | Abnormal | EVT-1: URL `status=xyz&topic=KHONG_CO&level=KHONG_CO`: `status` về `all` im lặng, hai mã lạ bị bỏ và đúng một toast | Vitest | F6 AC-01 |
| T-IL-06 | Abnormal | EVT-3 và EVT-4: lỗi sau đổi bộ lọc khôi phục giá trị trước, toast, danh sách cũ còn | Vitest | TDD màn `IL-AC-01` |
| T-IL-07 | Normal | EVT-5: cuộn tải trang kế gộp theo `id`, giữ dòng đang chọn; nút "Xem thêm" thay cho cuộn; lỗi trang kế giữ dòng đã nạp và có Thử lại | Vitest | TDD màn `IL-AC-02` |
| T-IL-08 | Normal | EVT-6: chọn A rồi B nhanh chỉ vẽ chi tiết B; tiêu đề hiện ngay từ dòng danh sách | Vitest | TDD màn `IL-AC-02` |
| T-IL-09 | Abnormal | EVT-6, EVT-8, EVT-7: `404 INB-010` bỏ dòng khỏi danh sách, chọn dòng đầu, toast | Vitest | F6 AC-04 |
| T-IL-10 | Normal | EVT-7: đảo lạc quan, gửi trạng thái đích; hai lần bấm nhanh không đảo ngược nhau; lỗi hoàn lại và toast | Vitest | F6 AC-05 |
| T-IL-11 | Normal | EVT-8: tự chấm cập nhật mức, nhãn dòng, dòng "ôn lại sau {n} ngày" với `n` = 1, 3, 7 sau khi chấm; nút `disabled` lúc gửi; lỗi giữ mức cũ | Vitest | F6 AC-08 |
| T-IL-12 | Boundary | Dòng ngày ôn lại: `nextReviewAt` đã quá hạn hiện "Đã đến hạn ôn lại", không hiện "0 ngày" | Vitest | F6 AC-02 (câu `KNOWN` quá hạn) |
| T-IL-13 | Normal | Ghim: chấm một câu khi lọc "Chưa xem": câu ở lại cùng nhãn mới; đổi bộ lọc thì hết ghim; lấy lại focus cửa sổ không làm mất dòng ghim | Vitest | TDD màn `IL-AC-03` |
| T-IL-14 | Boundary | Dòng đếm giữ `meta` của trang đầu: sau ghim và cuộn trang kế số `shown` không đổi | Vitest | F6 AC-01 |
| T-IL-15 | Normal | EVT-11: gửi `drill=true&limit=10` đúng bộ lọc; không sắp lại ở trình duyệt; nhãn nút `min(10, shown)` | Vitest | F6 AC-03 |
| T-IL-16 | Abnormal | EVT-11: lỗi hoặc mảng rỗng ở lại `browse` kèm toast; bộ lọc rỗng thì nút vô hiệu | Vitest | TDD màn `IL-AC-04` |
| T-IL-17 | Normal | EVT-12, EVT-13: ý cần nói đảo cục bộ, không gọi API; chấm sang thẻ kế không chờ phản hồi; hết thẻ về `browse` và toast `drillDone` | Vitest | F6 AC-08 |
| T-IL-18 | Abnormal | EVT-13: lỗi chấm một thẻ vẫn sang thẻ kế và toast cảnh báo | Vitest | TDD màn `IL-AC-04` |
| T-IL-19 | Normal | EVT-14: thoát giữa chừng không hỏi, không gọi API; nhãn các thẻ đã chấm đã đổi | Vitest | TDD màn `IL-AC-04` |
| T-IL-20 | Normal | EVT-9: liên kết `/interview-bank/{id}` mang năm tham số khác mặc định, gồm `bookmarked=1` khi bật; quay lại nạp lại và chọn dòng đầu | Vitest, Playwright | TDD màn `IL-AC-01` |
| T-IL-21 | Normal | EVT-10: "Vào phiên giả lập" luôn bấm được, dẫn `/mock-interview` | Vitest | TDD màn `IL-AC-05` |
| T-IL-22 | Boundary | `splitOutline`: `- a`, `* b`, `1. c`, dòng nối tiếp, chuỗi rỗng, một mục tiêu đề "Bẫy thường gặp" | Vitest (`split-outline.test.ts`) | F6 AC-06 |
| T-IL-23 | Security | AI và `ai-review` tắt: toàn bộ màn dùng được, không gọi F5; kiểu người học không có `rubric` | Vitest | F6 AC-06, AC-40 |
| T-IL-24 | Normal | Câu đã xoá mềm không có mặt ở bộ lọc nào, kể cả `bookmarkedOnly` và luyện nhanh | Playwright | F6 AC-04 |

## 8. Nợ prototype và câu hỏi mở

**Dòng nợ sẽ gỡ.** Dòng `views/interview-bank-list (mở rộng entities/interview-question)` ở `DEBT:889` có "Điều kiện tốt nghiệp: BE có `03-dd/api/F6_interview-bank.md` → viết DD → thay mock → test" [SoT: `06-plan/PROTOTYPE_DEBT.md:889`]. DD xong chỉ hoàn thành bước "viết DD"; thay mock và test **chưa làm** nên dòng này **chưa gỡ**.

Bảng 8.1 liệt kê lệch giữa prototype và thiết kế.

Bảng 8.1: Lệch giữa prototype và thiết kế.

| # | Prototype hiện tại | Thiết kế | Nguồn |
| :-: | :--- | :--- | :--- |
| P1 | Nạp **cả ngân hàng** một lần và lọc phía trình duyệt | Máy chủ lọc và phân trang 20 dòng, cuộn tải thêm | IBV:86-89, 128-155 |
| P2 | Bộ lọc ở `useState`, không đọc URL | BR-SC-01 | IBV:100-108 |
| P3 | Khoá thực thể là `code` ("IQ-nnn"); liên kết `/interview-bank/{code}` | UUID `id`; liên kết `/interview-bank/{id}?...` | IQE model/types.ts:20; IBV:498-503; DEBT:2127 |
| P4 | `use-recall-state.ts` là kho module giả lập dữ liệu theo người dùng | Bộ nhớ đệm truy vấn + hai mutation | IQE model/use-recall-state.ts:1-75 |
| P5 | Dòng ngày ôn lại tra bảng hằng số 1/3/7 | `nextReviewAt` của máy chủ (BR-SC-04) | IQE model/types.ts:62-71 |
| P6 | Thẻ luyện nhanh sắp theo mức tự chấm ở trình duyệt | Thứ tự máy chủ (BR-SC-05) | IBV:71-82, 163-169 |
| P7 | `suggestedApproach` là `string[]`, `rubric` nằm trong kiểu dùng chung | Chuỗi Markdown, tách bằng `splitOutline`; kiểu người học không có `rubric` | IQE model/types.ts:27, 40 |
| P8 | Danh mục chủ đề, độ khó đọc từ kho quản trị cục bộ | F6 mục 9, 10 | IBV:94-95 |
| P9 | `total` của dòng đếm là độ dài mảng | `meta.total` (BR-SC-02 bước 5) | IBV:398-401 |
| P10 | Không có khung chờ, trạng thái lỗi riêng từng vùng, nút "Xem thêm" | Bảng 3.1, mục 6 | IBV:84-90 |
| P11 | Khoá `startMockInterviewUnavailable` còn trong `vi.json` | Xoá | `vi.json:1946` |

Bảng 8.2 liệt kê sai lệch phát hiện ở tài liệu khác. DD này không sửa RD, BD hay DD module.

Bảng 8.2: Sai lệch ở tài liệu khác.

| # | Nơi | Sai lệch | Đề xuất |
| :-: | :--- | :--- | :--- |
| D-1 | BD:553, 621-622 (và BD402:617, 692-693) | Lịch sử: ban đầu đề xuất chỉ dùng AC F6; giai đoạn TDD đã bổ sung file riêng cho cả hai màn để kiểm trạng thái giao diện và điều hướng | Đã xử lý: giữ liên kết tới AC màn và AC module tương ứng |
| D-2 | BD:109-126 và `03-dd/screens/users/USR0301_solution_review.md:210` | BD cho `solution_review` gửi `topic`, `q` sang màn này; DD `USR0301` chỉ mở `/interview-bank` không tham số (F5 không trả mã chủ đề F6) | Ghi vào BD: lối vào có tham số hoãn tới khi có nguồn mã chủ đề (ponytail bậc 3) |
| D-3 | F6:128 và L6:105 | `q` ghi "tiêu đề, mô tả, từ khoá"; "mô tả" là `content_markdown` | Chỉ lệch chữ, không đổi hành vi; sửa chữ F6 khi chạm |
| D-4 | BD:324 và F6:134-135 | BD nói số thẻ do hằng số giao diện; F6 có trần 20 (`drill.max-size`, L6:42) | Không lệch: giao diện gửi 10 |

Bảng 8.3 là câu hỏi mở mức màn, mặc định đề xuất và bậc ponytail (`.claude/rules/ponytail-criteria.md`).

Bảng 8.3: Câu hỏi mở.

| # | Câu hỏi | Mặc định đề xuất | Bậc |
| :-: | :--- | :--- | :-: |
| OQ-1 | **Đã chốt** `DEC-2026-1025-dd-interview-bookmark-url`: giữ công tắc "Chỉ câu đã đánh dấu" khi mở chi tiết rồi quay lại | `bookmarked=1` là tham số URL thứ năm; `USR0402` chuyển tiếp nguyên trạng; đồng bộ BD hai màn ở lượt BD | 4 |
| OQ-2 | Giới hạn của phân trang theo vị trí (BR-SC-02 bước 4): một câu có thể bị bỏ sót khi cuộn sau khi đã chấm ở bộ lọc "Chưa xem" | Chấp nhận, ghi nợ; sửa bằng phân trang theo con trỏ nếu về sau thấy lỗi | 3 |
