# DD màn hình — Tổng quan người học (`USR0601`)

> Tuân khung `03-dd/screens/README.md` mục 4 (chín mục 0 đến 8) và quy tắc cứng mục 5. Request, response, mã lỗi nằm ở file API của module và chỉ được liệt kê theo ID. Không có logic backend ở đây. Màn gom mười một khối từ năm module (F1, F2, F4, F5, F6) và là **đích sau đăng nhập** của `STUDENT`, nên phần quan trọng nhất của DD này là cách ghép nguồn, cách cô lập lỗi từng khối và cách khối do AI nuôi suy giảm mà không kéo cả màn xuống. Màn đã **gộp** `USR0501` (`03-dd/screens/users/USR0501_my_progress.md`).

## 0. Thông tin tài liệu

Bảng 0.1: Bìa tài liệu.

| Mục | Giá trị |
| :--- | :--- |
| Mã màn, slug | `USR0601`, `dashboard` |
| Tên màn | Tổng quan người học |
| Nhóm actor | `users` (A1 `STUDENT`), chỉ đọc |
| Route | `/dashboard` (`app/(student)/dashboard`); đích sau đăng nhập của `STUDENT` (RD:101). Route `/progress` của `USR0501` **không tồn tại**, không cần chuyển hướng [SoT: `05-coding/frontend/src/entities/user/model/area.ts:28-30`] |
| Bounded Context đứng sau | F1 `identity` (chủ màn); đọc F2 `problem-bank`, F4 `judge-orchestration`, F5 `ai-review`, F6 `interview-bank` |
| RD nguồn | `01-rd/screens/users/USR0601_dashboard.md`; `01-rd/screens/users/USR0501_my_progress.md` (hồ sơ gốc của REQ-01 đến REQ-06, còn hiệu lực) |
| BD nguồn | `02-bd/screens/users/USR0601_dashboard.md` (V0.14); `02-bd/screens/users/USR0501_my_progress.md` (V0.8, nguồn đối chiếu của Khu vực F, G, H) |
| Prototype | `05-coding/frontend/src/views/users/dashboard/` (khối giao diện chạy, dữ liệu mock), `entities/progress/` |
| Phiên bản, ngày | V0.1, 2026-10-24 |
| Tiêu chí nghiệm thu | `04-tdd/screens/users/USR0601_dashboard.md` (luồng xuyên module) cộng các `AC` module ghi ở mục 7 |

Bảng 0.2: Lịch sử sửa.

| Ver | Mục | Nội dung | Ngày |
| :--- | :--- | :--- | :--- |
| V0.1 | Toàn bộ | Tạo mới từ prototype đã dựng, BD V0.14 và DD F1, F2, F4, F5, F6. Chốt cách thay hai endpoint `GetMyProgressOverview`, `GetMyTopicProgress` mà F1 không có bằng phép ghép F1 + F2 + F4; chốt phạm vi `range`; chốt một phép tính điểm năng lực; chốt trạng thái suy giảm từng khối; nêu các giá trị chưa có nguồn thật | 2026-10-24 |

Bảng 0.3: Viết tắt nguồn trích dẫn.

| Viết tắt | Đường dẫn |
| :--- | :--- |
| RD | `01-rd/screens/users/USR0601_dashboard.md` |
| BD | `02-bd/screens/users/USR0601_dashboard.md` |
| BD05 | `02-bd/screens/users/USR0501_my_progress.md` |
| F1, F2, F4, F5, F6 | `03-dd/api/F1_identity.md` (số `#nn`, mục 0), `F2_problem-bank.md`, `F4_judge-orchestration.md`, `F5_ai-review.md`, `F6_interview-bank.md` (số `mục`) |
| V1 | `03-dd/validation/F1_identity.md` |
| API | `03-dd/api/api.md` |
| PV | `05-coding/frontend/src/views/users/dashboard/` |
| PRG | `05-coding/frontend/src/entities/progress/` |
| DEBT | `06-plan/PROTOTYPE_DEBT.md` |
| DD-PD, DD-SR | `03-dd/screens/users/USR0102_problem_detail.md`, `USR0301_solution_review.md` |

## 1. Bản đồ slice FSD

Thứ tự import `app → views → widgets → features → entities → shared`. Bảng 1.1 là bản đồ đích; cột "Hiện trạng" so với mã đang có.

Bảng 1.1: Slice của màn.

| Lớp | Đường dẫn (từ `src/`) | Vai trò | Hiện trạng |
| :--- | :--- | :--- | :--- |
| app | `app/(student)/dashboard/page.tsx` | Render `DashboardView` | Có |
| views | `views/users/dashboard/ui/dashboard-view.tsx` | Giữ state `range` duy nhất, dựng mười một khối [SoT: PV ui/dashboard-view.tsx:56-113] | Có |
| views | `views/users/dashboard/ui/blocks/*` | Một file mỗi khối: `greeting-block`, `stat-cards-row`, `daily-submissions-block`, `skill-radar-block`, `activity-block`, `topics-table`, `difficulty-card`, `focus-card`, `suggested-problems-block`, `recent-interviews-block`, `recent-reviews-block` | Có; thiếu bảng phụ F6-10 (`interview-topics-table`, mới) |
| views | `views/users/dashboard/model/derive-skill-radar.ts` | Đổi thành ánh xạ vector `topicScores` từ F2 §13 sang lời chào/radar/ưu tiên, không tính lại điểm (BR-SC-04) | Đang tính ở client, cần sửa |
| views | `views/users/dashboard/model/activity-level.ts` | Hằng ngưỡng quy số lượt nộp sang mức 0-4 (BR-SC-10) | Chưa có |
| views | `views/users/dashboard/api/queries.ts` | Chỉ giữ `useActivityCalendar` (F1 #55). `useDashboardDaily` bỏ (thay bằng `useMyMetrics`); `useRecentSolutionReviews` chuyển sang entity `solution-review` vì F5 sở hữu [SoT: PV api/queries.ts:19-46] | Có, mock |
| entities | `entities/progress/api/queries.ts` | Một hook **một endpoint**, không hook gộp: `useSolveSummary` (F1 #47), `useCatalogSummary` (F2 mục 14), `useTopicSolveProgress` (F2 mục 4), `useMyMetrics(range, topicIds?)` (F4 mục 12), `useInterviewSummary(range, limit)` (F5 mục 13), `useInterviewTopicProgress` (F6 mục 8) [SoT: PRG api/queries.ts:21-58] | Có 5 hook mock theo tên BD cũ; viết lại theo cột này |
| entities | `entities/progress/model/join-topic-rows.ts` | Hàm thuần ghép dòng chủ đề: `solved/total` của F2 mục 4 với `acRateByTopic` của F4 mục 12, khoá `topicId` (BR-SC-03) | Chưa có |
| entities | `entities/progress/model/derive-focus.ts` | Quy tắc "Nên ưu tiên" thuần, không gọi AI [SoT: PRG model/derive-focus.ts] | Có |
| entities | `entities/problem/api/queries.ts` | Thêm `useSuggestedProblems({difficulty, topic})` (F2 mục 13); `useProblemLevels` đọc F2 mục 15 | Có `useProblemLevels` dạng store; `useSuggestedProblems` chưa có |
| entities | `entities/submission/api/queries.ts` | Thêm `useLatestUnsolved(limit)` (F4 mục 11) cho nút "Tiếp tục bài đang làm" | Chưa có (nút đang đọc `useProblemListPage().inProgress`) [SoT: PV ui/blocks/greeting-block.tsx:38-41] |
| entities | `entities/solution-review/api/queries.ts` | Thêm `useRecentSolutionReviews(limit)` (F5 mục 7) | Chưa có ở entity này |
| entities | `entities/user/api/queries.ts` | Dùng `useMyProfile` (F1 #9) để lấy tên chào, chung với khung | Có, mock |
| shared | `DashboardBlockState`, `StatCard`, `SegmentedTabs`, `RadarChart`, `LineChartWithTotal`, `ActivityHeatmap`, `ProgressBar`, `SeeAllLink`, `Skeleton`, `Badge`; `shared/lib/format-date` (`formatDateVN`); `shared/api` `useErrorMessage` (hẹn ở DD-PD mục 1, S3) | Dùng lại | Có, trừ `useErrorMessage` |

Không có thành phần mới nào cần nâng lên `shared/`. Hằng ngưỡng của lưới hoạt động nằm ở `views` vì chỉ màn này dùng (BD:483).

**Hai lệch có chủ ý so với BD.** (1) BD 4.5 để dữ liệu riêng của màn trong `views/.../api` [SoT: BD:393]; DD chỉ để đó phần chỉ màn này dùng (lưới hoạt động), còn dữ liệu có nhiều màn đọc (`entities/progress` cho `USR0502`) hoặc thuộc module khác (báo cáo gần đây thuộc F5) đặt ở entity tương ứng. (2) BD gọi `GetMyProgressOverview` và `GetMyTopicProgress` là endpoint của `identity` [SoT: BD:813-814]; F1 không có hai endpoint này (F1 mục 0.8), nên DD thay bằng phép ghép ở mục 2.

## 2. API tiêu thụ

Bảng 2.1: Sự kiện BD, endpoint theo ID, hook, trạng thái nối. Hiện mọi dòng đều mock [SoT: PRG api/queries.ts:21-58; PV api/queries.ts:19-46]. Không có kênh realtime; màn không dùng STOMP hay SSE.

| EVT (BD) | Khối | Endpoint theo ID | Hook | Nối |
| :--- | :--- | :--- | :--- | :--- |
| EVT-1 | A NO 1 (tên) | F1 #9 `GetMyProfile` | `useMyProfile` | mock |
| EVT-1, EVT-3 | A NO 4 (nút) | F4 mục 11 `limit = 3` | `useLatestUnsolved` | mock (đọc nguồn khác) |
| EVT-1 | B "Đã giải" tử số/mẫu số | F2 §14A `ListMyLevelProgress` | `useDifficultyProgress` | chưa nối |
| EVT-1 | B "Acceptance rate" | F1 #47 | `useSolveSummary` | mock |
| EVT-1 | I chip chủ đề | F2 mục 14 `GetPublishedProblemCatalogSummary` | `useCatalogSummary` | mock |
| EVT-1 | B "Chuỗi ngày" | F4 mục 12 `range = 7d` (chỉ lấy `streakDays`, `lastSubmittedAt`) | `useMyMetrics("7d")` | mock |
| EVT-1 | B "Mock Interview" | F5 mục 13 `range = all`, `limit = 1` | `useInterviewSummary("all", 1)` | mock |
| EVT-1, EVT-2 | C | F4 mục 12 `range` hiện chọn | `useMyMetrics(range)` | mock |
| EVT-1 | D, A NO 2, H (xếp hạng), I (thứ tự) | F2 mục 4 + F4 mục 12 `range = all` kèm `topicIds` | `useTopicSolveProgress` + `useMyMetrics("all", topicIds)` | mock |
| EVT-1, EVT-2 | F NO 3-8 | F2 mục 4 + F4 mục 12 `range` kèm `topicIds` | như trên, `range` hiện chọn | mock |
| EVT-1 | A điểm yếu, D radar, H/I thứ tự ưu tiên | F2 §13 `GetSuggestedProblems` trả `topicScores` từ cửa sổ `all` | `useSuggestedProblems` | chưa nối |
| EVT-1 | F NO 10-14 (F6-10) | F6 mục 8 `GetMyInterviewTopicProgress` | `useInterviewTopicProgress` | chưa có |
| EVT-1 | G NO 4 (đã giải theo mức) | F2 §14A `ListMyLevelProgress` | `useDifficultyProgress` | chưa nối |
| EVT-1 | E | F1 #55 `GetMyActivityCalendar` | `useActivityCalendar` | mock |
| EVT-1, EVT-5, EVT-6 | I | F2 mục 13 `GetSuggestedProblems`, tham số `difficulty`, `topic`; tab độ khó từ F2 mục 15 | `useSuggestedProblems`, `useProblemLevels` | mock |
| EVT-1, EVT-2 | J | F5 mục 13 `range` hiện chọn, `limit = 3` | `useInterviewSummary(range, 3)` | mock |
| EVT-1 | K | F5 mục 7 `limit = 3` | `useRecentSolutionReviews` | mock |
| EVT-12 | mọi khối | gọi lại đúng query của khối | `query.refetch()` | không áp dụng |
| EVT-3, EVT-4, EVT-7, EVT-8 (chủ đề ưu tiên), EVT-9, EVT-10, EVT-11 (báo cáo phân tích) | điều hướng, chú giải | không gọi API | `next/link`, state | không áp dụng |

Điểm khác BD khi nối thật:

1. **Tên nghiệp vụ BD ghép nhiều nguồn.** `GetMyProgressOverview` tương ứng F1 #47 (DTO mục 0.8.4), F2 §14A (đã giải trên tập xuất bản) và F4 mục 12 (chuỗi ngày); `GetMyTopicProgress` tương ứng F2 mục 4 cùng F4 mục 12. Các query chạy độc lập theo vùng, không tạo endpoint F1 tổng hợp mới.
2. **Lọc "Bài toán gợi ý" ở máy chủ, không ở client.** BD EVT-5, 6 ghi "không gọi máy chủ, lọc trên tập đã tải" [SoT: BD:864-865] nhưng F2 mục 13 luôn trả đúng 7 dòng và nhận `difficulty`, `topic` làm tham số [SoT: F2:757-775]; lọc trên 7 dòng cho ra 0-2 dòng với hầu hết chip. DD theo F2: mỗi lần đổi tab hoặc chip là một lời gọi mới (BR-SC-07). Đề xuất BD đổi cột "Gọi API" của EVT-5, 6 sang "Có".
3. **Nút "Tiếp tục bài đang làm"** dùng F4 mục 11 (BD gọi `GetMyLatestDraft`, tên không có ở F4) [SoT: F4:637-666; BD:820].
4. **Chuỗi ngày** đọc trực tiếp từ F4 mục 12; F4 chỉ ghi cổng trong tiến trình `MySubmissionMetricsPort` cho `identity` ghép `MyProgressOverviewDto` [SoT: F4:672], nhưng F1 không có endpoint ghép đó, nên màn gọi F4 thẳng.
5. `completedSessionCount` của F5 chỉ đếm phiên `COMPLETED` **đủ bốn dòng rubric** [SoT: F5:835] (BD ghi "COMPLETED"); `range` của F5 lọc theo `ended_at` (BD ghi `started_at`) [SoT: F5:843; BD:572]. DD theo F5.

## 3. Trạng thái màn

Bảng 3.1: Trạng thái theo khối. Khoá i18n có tiền tố `dashboard.` hoặc `myProgress.` (hai namespace, mục 6) hoặc `apiError.` (frontend không đọc `message`, API:22). Mọi khối có trạng thái "đang tải" riêng bằng `Skeleton` tại chỗ và trạng thái "lỗi" riêng kèm nút thử lại (`DashboardBlockState`).

| Khối | Trạng thái | Điều kiện | Hiển thị | Khoá i18n |
| :--- | :--- | :--- | :--- | :--- |
| Toàn trang | Chưa đăng nhập | `401` sau khi làm mới token thất bại | Chuyển về `auth` (xử lý ở `shared/api/client.ts`) | không áp dụng |
| Toàn trang | Không đúng khu | vai trò khác `STUDENT` vào `/dashboard` | Layout khu Người học chuyển về khu của vai trò đó, không render màn [SoT: Suy luận, theo `entities/user/model/area.ts`] | không áp dụng |
| A | Tên chưa có | `GetMyProfile` đang tải hoặc lỗi | Câu "Chào bạn, tiếp tục nhé", không toast | `greeting.title` |
| A | Chưa có dữ liệu luyện tập | không chủ đề nào có điểm (BR-SC-04) | Câu thay chỗ dòng điểm yếu, không số 0 giả | `greeting.noData` |
| A | Nút | F4 mục 11 lỗi hoặc không có bài hợp lệ | Nhãn "Bắt đầu giải bài" dẫn `/problems`; **không** vô hiệu nút, không toast (BR-SC-06) | `greeting.btnStart` |
| B | Lỗi riêng từng thẻ | ba thẻ F1/F4 và thẻ F5 là hai nhóm query khác nhau | Thẻ lỗi hiện nút thử lại riêng; thẻ khác nguyên | `errorGeneric`, `retry` |
| B | "Acceptance rate" rỗng | `totalSubmissions = 0` | Câu "Chưa có bài nộp nào", không `0%` (Sheet 9 NO 4, BD:889) | `stats.acRateEmpty` |
| B | "Chuỗi ngày" | `lastSubmittedAt = null` (chưa nộp lần nào) | `-`; có lượt nộp mà `streakDays = 0` vẫn in `0 ngày` vì đó là giá trị thật (xem OQ-5) | `stats.streakUnit` |
| B | "Mock Interview" suy giảm AI | F5 mục 13 lỗi hoặc quá hạn | Thẻ in câu lỗi AI riêng; ba thẻ còn lại nguyên | `errorAi` |
| B | "Mock Interview" rỗng | `completedSessionCount = 0` | "Chưa có phiên phỏng vấn nào" | `stats.interviewEmpty` |
| C | Rỗng | mọi `dailyCounts[].count = 0` | Vẫn vẽ đủ mốc giá trị 0 kèm câu "Chưa có bài nộp nào trong khoảng này" (BD05:420: biểu đồ không có trạng thái rỗng riêng; DD giữ câu chú thích) | `daily.empty` |
| D | Thiếu dữ liệu | dưới 3 chủ đề có điểm, hoặc `solved = null` (BR-SC-03) | "Cần ít nhất 3 chủ đề có dữ liệu để vẽ biểu đồ" | `radar.empty` |
| E | Lỗi khung | `days.length != 364` | Trạng thái lỗi khối, **không** vẽ lưới lệch (BR-SC-10) | `errorGeneric` |
| E | Rỗng | mọi `submissionCount = 0` | Lưới 364 ô mức 0 kèm câu tóm tắt "0 ngày có bài nộp" | `activity.summary` |
| F NO 3-8 | F4 lỗi | F2 mục 4 xong, F4 mục 12 lỗi | Bảng vẫn hiện "Đã giải" và thanh; cột "Tỉ lệ AC", "Lần cuối" in `-` kèm một dòng chú nhỏ (BR-SC-03) | `topicProgress.acUnavailable` (mới) |
| F NO 3-8 | `solved = null` | F2 mục 4 trả `solveStateAvailable = false` | Cột "Đã giải" in `-`, ẩn thanh | `topicProgress.solvedUnavailable` (mới) |
| F NO 3-8 | Lỗi | F2 mục 4 lỗi (không có danh sách chủ đề) | Cả bảng lỗi kèm thử lại | `errorGeneric` |
| F NO 10-14 | Lỗi riêng F6 | F6 mục 8 lỗi (không phải AI, nhưng là module khác) | Bảng phụ lỗi kèm thử lại; bảng F NO 3-8 nguyên | `errorGeneric` |
| F NO 10-14 | Rỗng thật | chủ đề chưa luyện | Vẫn một dòng mỗi chủ đề, `0 / n` và `-`; không câu rỗng (F6 AC-41) | `topicProgress.review.*` (mới) |
| G | F2 §14A lỗi | Tiến độ theo mức tạm không có | Giữ khung và nút thử lại riêng; không thay tử số bằng 0 | `difficulty.solvedUnavailable` (mới) |
| H | Ẩn | F2 mục 4 hoặc F4 mục 12 lỗi | Khối ẩn theo BD (cùng nguồn với F) [SoT: BD:543; PV ui/blocks/focus-card.tsx:23] | không áp dụng |
| H | Chúc mừng | không còn chủ đề có bài chưa giải | Câu "Bạn đã giải hết bài của mọi chủ đề" | `focus.allSolved` |
| I | Rỗng | bộ lọc không còn bài | "Không có bài nào khớp bộ lọc" | `suggested.empty` |
| I | Không xếp hạng | F2 mục 13 trả `rankingAvailable = false` | Vẫn hiện tối đa 7 bài, thêm một dòng chú "Chưa sắp theo điểm yếu của bạn" | `suggested.rankingFallback` (mới) |
| I | Không có dấu trạng thái | `solveState = null` | Ẩn chấm trạng thái từng dòng, không coi là "chưa làm" | `solveState.*` |
| J, K | **AI suy giảm** | F5 mục 13 hoặc mục 7 lỗi, quá hạn, hoặc F5 chết | Khối in "Phân hệ AI đang không phản hồi — các khối còn lại vẫn hoạt động" kèm nút thử lại; hai liên kết "Tất cả" vẫn hoạt động (EVT-10) | `errorAi`, `retry` |
| J, K | Rỗng | chưa có phiên hoặc báo cáo | Câu rỗng riêng từng khối | `interviews.empty`, `reviews.empty` |
| K | Tên bài thiếu | F5 mục 7 trả `problemTitle = null` (F2 lỗi) | Dòng vẫn hiện, tên thay bằng `Lượt nộp #{id}` rút gọn | `reviews.titleUnknown` (mới) |
| Mọi khối | `400` | `JDG-V07`, `JDG-V15`, `AIR-V10`, `AIR-V02`, `JDG-V14` | Không xảy ra qua giao diện (`range` là enum cố định, `limit` là hằng); nếu xảy ra coi như lỗi khối | `apiError.JDG-V07`, `apiError.AIR-V10` |

Toast chỉ dùng cho `400` bất thường của `range` (BD Sheet 9 NO 3 [SoT: BD:888]); mọi lỗi tải khối là nội dung thay chỗ khối, không toast (BD05:578).

## 4. Quy tắc đặc thù màn

### BR-SC-01 Khởi tạo song song, ba nhóm nguồn cô lập

- **Kích hoạt:** EVT-1.
- **Quy tắc:** mỗi dòng của Bảng 2.1 là một `useQuery` độc lập với `retry: false` và `staleTime` 30 giây [SoT: PRG api/queries.ts:13]. **Không** có `Promise.all` giữa hai query, không có hook gộp. Ba nhóm nguồn: (a) F1 + F2 + F4 (sổ sách học tập); (b) F5 (thẻ "Mock Interview", khối J, khối K); (c) F6 (bảng phụ F6-10). Một nhóm chết không đổi trạng thái nhóm khác.
- **Hậu điều kiện:** F5 hoặc F6 dừng hẳn thì chín khối còn lại hiển thị đủ (REQ-07 [SoT: RD:108], BD Sheet 9 NO 5). Đây là màn đích sau đăng nhập, nên một khối AI chặn được cả màn nghĩa là người học không vào được ứng dụng khi F5 hỏng (BD:890).
- **Không** có endpoint nào trong màn gọi mô hình ngôn ngữ; F5 mục 13 chỉ đọc Postgres [SoT: F5:822]. Phần "suy giảm AI" ở đây là suy giảm khi **module** F5 không phản hồi.

### BR-SC-02 Phạm vi bộ lọc khoảng thời gian

- **Kích hoạt:** EVT-2.
- **Các bước:** (1) tô sáng nút vừa chọn ngay; (2) khoá query mới theo `range`, giữ dữ liệu cũ (`placeholderData: keepPreviousData`) trong lúc tải để khối không nháy trống; (3) vùng `role="status"` đọc "Đang xem dữ liệu {range}" một lần [SoT: PV ui/dashboard-view.tsx:78-81].
- **Bảng phạm vi (nguồn BD:912, Q6 chốt 2026-10-18):**

Bảng 4.1: Khối nào đổi theo `range`.

| Đổi theo `range` | Query | Không đổi (luỹ kế) |
| :--- | :--- | :--- |
| C biểu đồ | `useMyMetrics(range)` | B bốn thẻ (thẻ "Mock Interview" dùng `range = all`) |
| F cột "Tỉ lệ AC", "Lần cuối" | `useMyMetrics(range, topicIds)` | F cột "Đã giải", bảng phụ F6-10 |
| H "Nên ưu tiên" (xếp hạng) | từ F, giữ nguyên | D radar, A NO 2 lời chào |
| J khối phỏng vấn | `useInterviewSummary(range, 3)` | E lưới 12 tháng, G, I |

- **Hậu điều kiện:** đổi `range` **không** gọi lại F2, F1, F6. Khi `range = all` thì query của bảng F và query của radar là **cùng khoá** nên chỉ một lời gọi F4 mục 12.

### BR-SC-03 Ghép bảng "Theo chủ đề" từ F2 và F4

- **Kích hoạt:** EVT-1, EVT-2.
- **Các bước:** (1) F2 mục 4 trả mọi chủ đề, theo `sortOrder`, mỗi dòng có `topicId`, `solved`, `total` (không trả `acRate`) [SoT: F2:366-368]; (2) lấy `topicIds` từ (1), gọi F4 mục 12 với `topicIds`, tối đa 50, quá thì `JDG-V15` (kỳ vọng ít hơn 50 chủ đề; vượt thì cắt 50 chủ đề đầu theo `sortOrder`, chủ đề còn lại cột AC in `-` `[SoT: Suy luận]`); (3) hàm `join-topic-rows` ghép theo `topicId`: `acRate` đổi từ tỉ lệ 0..1 sang số nguyên phần trăm, `submissionCount = 0` hoặc thiếu phần tử thì `-`; (4) sắp **ở client** theo số bài chưa giải (`total - solved`) giảm dần, đúng ghi chú NO 2 [SoT: BD:505-506].
- **Suy giảm trong khối:** F4 lỗi thì chỉ hai cột AC in `-`; F2 mục 4 lỗi thì cả bảng lỗi; `solved = null` (identity lỗi bên trong F2) thì cột "Đã giải" in `-`. Điểm năng lực của A/D/H/I đọc `topicScores` từ F2 §13; `rankingAvailable=false` thì các phần dùng điểm hiện "thiếu dữ liệu", **không** coi điểm thiếu bằng 0; danh sách gợi ý vẫn có thể hiển thị theo thứ tự dự phòng.
- **Bài đã rút:** F2 mục 4 loại bài đã rút khỏi cả tử số và mẫu số [SoT: F2:368, 377]; màn không tự trừ lại.

### BR-SC-04 Một phép tính điểm năng lực duy nhất

- **Kích hoạt:** dựng A NO 2, D, H (ghim), I (thứ tự).
- **Quy tắc:** F2 §13 tính `topicScores` một lần từ tỉ lệ AC cửa sổ `all` (hệ số 0.7) và tỉ lệ bài đã giải trên tập đang xuất bản (hệ số 0.3), không trọng số độ khó. Màn nhận đúng vector này cho lời chào, radar và thứ tự "Nên ưu tiên"; không chạy lại `scoreTopics` ở client. Chọn chủ đề yếu nhất theo `score` tăng dần, hoà theo `sortOrder`, rồi `topicId`. `[SoT: DEC-2026-1024-dd-f2-topic-score-source; BD Sheet 9 NO 7]`
- **Sửa so với bản dựng:** chủ đề **chưa có lượt nộp nào** hoặc `total=0` không có phần tử trong `topicScores`, không có điểm 0 giả. Lời chào nêu chủ đề yếu nhất trong vector; radar vẽ khi có từ ba phần tử. `rankingAvailable=true`, `topicScores=[]` nghĩa là chưa có dữ liệu; `false` nghĩa là cổng F1/F4 suy giảm và cần retry khối. `[SoT: F2 §13.3–13.5]`
- **Đồng bộ:** F2 dùng chính `topicScores` để xếp bài gợi ý; frontend dùng vector đó cho A/D/H/I. Không còn `LearnerTopicRankingPort` của F1 và không có công thức thứ hai ở client. `[SoT: DEC-2026-1024-dd-f2-topic-score-source]`

### BR-SC-05 "Nên ưu tiên"

- **Kích hoạt:** EVT-1, EVT-2.
- **Quy tắc:** xếp hạng, ghim chủ đề yếu nhất của lời chào và câu chúc mừng theo BD Khu vực H NO 2 (BD:543), cài ở `derive-focus.ts`; DD không chép lại thuật toán. Nhận hai nguồn: dữ liệu `range` hiện chọn cho thứ tự và lý do, dữ liệu `all` cho việc ghim [SoT: PV ui/blocks/focus-card.tsx:17-23]. `reasonCode` là khoá tra i18n, không ghép câu ở backend (BD:770).
- **Không có endpoint riêng** cho khối này.

### BR-SC-06 Nút "Tiếp tục bài đang làm"

- **Kích hoạt:** EVT-1 (chọn đích), EVT-3 (bấm).
- **Các bước:** (1) gọi F4 mục 11 với `limit = 3`; (2) lấy phần tử **đầu tiên** có `problem != null` và `problem.withdrawn != true`; (3) có thì đích là `/workspace/{problemId}`, nhãn "Tiếp tục bài đang làm"; không có (rỗng, F4 lỗi, hoặc cả ba bài đã rút) thì đích `/problems`, nhãn "Bắt đầu giải bài".
- **Hậu điều kiện:** nút **không bao giờ** bị vô hiệu và không toast khi F4 lỗi (REQ-08 [SoT: RD:109; BD:862]).
- **Lệch cần biết:** F4 định nghĩa "đang làm dở" là bài nộp **gần nhất** chưa `ACCEPTED`, không có bảng nháp [SoT: F4 §11], còn RD REQ-03 nói "bản nháp chưa nộp trong Workspace" [SoT: RD:102]; bản nháp chỉ ở `localStorage` (DD-PD BR-SC-02) nên không hiện ở đây. Theo `DEC-2026-0922-users-and-admin-conflict-resolutions` (BD:436). F4 §11 đã lọc bài `withdrawn=true` trước khi cắt `limit` (G4 đã đóng).

### BR-SC-07 Bài toán gợi ý lọc ở máy chủ

- **Kích hoạt:** EVT-1, EVT-5, EVT-6.
- **Các bước:** (1) trạng thái `{difficulty, topic}` giữ ở state khối, mặc định cả hai là "Tất cả"; (2) mỗi lần đổi là một query `useSuggestedProblems({difficulty, topic})` mới, giữ bảng cũ trong lúc tải; (3) tab độ khó dựng từ F2 mục 15 (mọi mức theo `sortOrder`, kể cả mức chưa có bài), chip chủ đề dựng từ `byTopic` của F2 mục 14 (chỉ chủ đề có bài đã xuất bản) [SoT: F2:820-822; BD:555]; (4) hai bộ lọc **cộng dồn**; số "N bài" ở tiêu đề là `items.length`.
- **Dòng bảng:** `solveState` ba trạng thái, `acRate` là tỉ lệ **toàn hệ thống** của bài (BD:563), `functionWrapperSupported` bật nhãn `function`. Tên bài là liên kết `/problems/{problemId}` mở `problem_overview` (EVT-7) [SoT: BD:866].

### BR-SC-08 Độ trễ giữa các nguồn

- **Quy tắc:** số lượt nộp, "Đã giải", lưới 12 tháng do `identity` nuôi bằng sự kiện `SubmissionGraded` bất đồng bộ [SoT: F1:249-251]; F4 mục 12 và F2 mục 4 đọc trực tiếp. Ngay sau khi nộp có thể lệch vài giây giữa hai nguồn. Màn **không** polling, **không** dùng nguồn này vá nguồn kia (ví dụ không dùng `dailyCounts` của F4 để điền lưới của F1), làm mới khi tải lại hoặc khi cửa sổ lấy lại tiêu điểm (mặc định của TanStack Query). `[SoT: Suy luận]`, vì BD chốt "tính lại ở mỗi lần vào màn" (BD05:392).

### BR-SC-09 Tên trong lời chào

- **Quy tắc:** `Chào {displayName}, tiếp tục nhé`, `displayName` lấy từ `useMyProfile` (F1 #9), **không** cắt tên (không có cách chắc chắn lấy "tên gọi" của họ tên tiếng Việt). Access token không mang tên [SoT: `02-bd/security/F1_identity.md:8`] và `POST /auth/refresh` chỉ trả token [SoT: F1:203], nên sau khi tải lại trang chỉ có F1 #9 mới có tên. Chưa có thì dùng "bạn". Đây là một lời gọi dùng chung với khung (avatar, tên) chứ không phải lời gọi riêng của màn [SoT: BD:433].

### BR-SC-10 Lưới hoạt động 12 tháng

- **Quy tắc:** nhận đúng 364 phần tử `{day, submissionCount}` từ F1 #55, tăng dần theo ngày [SoT: F1:164-167]; màn **không** vá chỗ thiếu: `days.length != 364` thì khối lỗi (BD Sheet 9 NO 8). Quy số lượt nộp sang mức 0-4 do **màn** tính (F1 chỉ trả số đếm) bằng hằng ở `model/activity-level.ts`: mức 0 khi 0; mức 1 khi 1; mức 2 khi 2 đến 3; mức 3 khi 4 đến 6; mức 4 khi từ 7 `[SoT: Suy luận]`, BD để "chọn khi dựng" (BD:483). Dòng tóm tắt đếm số ô có `submissionCount > 0`. Nhãn tháng suy từ ngày của các cột, múi giờ UTC+7 (`formatDateVN`).

### BR-SC-11 Dữ liệu người học là dữ liệu

- **Quy tắc:** tên bài, tên chủ đề, tên hiển thị, `timeComplexity` của báo cáo hiện ra là văn bản thuần (React tự escape), **không** dùng `dangerouslySetInnerHTML`, không ghép vào chuỗi i18n dạng HTML. Màn không có nội dung đi vào lời nhắc AI; khối K chỉ đọc hai khoá nhỏ của báo cáo (`verdict`, `complexity.actualTime`), không bao giờ nguyên `result_json` (BD Sheet 9 NO 6 [SoT: BD:891; F5:437-438]).

## 5. Kiểm tra phía client

Màn **không có form**: không có trường nào do người học nhập và gửi lên. Bảng 5.1 chỉ ghi các giá trị đầu vào mà màn phải chuẩn hoá.

Bảng 5.1: Đầu vào của màn và ánh xạ.

| Giá trị | zod hoặc xử lý | Quy tắc module | Khớp |
| :--- | :--- | :--- | :--- |
| `range` | `progressRangeSchema = z.enum(["7d","30d","all"])` [SoT: PRG model/types.ts:8] | `JDG-V07` (F4 mục 12), `AIR-V10` (F5 mục 13): giá trị lạ **bị từ chối**, không tự đưa về `all` | Có, cùng ba giá trị |
| `limit` (F4 mục 11, F5 mục 7, mục 13) | hằng trong hook: 3, 3, 3 và 1 | `JDG-V14` (1-20), `AIR-V02` (1-10) | Có |
| `topicIds` | lấy từ F2 mục 4, không do người học nhập; cắt 50 | `JDG-V15` | Có |
| `difficulty` | chỉ nhận `levelCode` có trong F2 mục 15, không thì "Tất cả" | F2 mục 13: không khớp thì coi như "Tất cả" [SoT: F2:770] | Có |
| `topic` | UUID của chip, không do người học gõ | F2 mục 13 | Có |
| `days.length` của lưới | phải bằng 364 | F1 mục 0.8.1 | Có |

## 6. i18n và truy cập

**Tiền tố khoá.** Hai namespace: `dashboard.*` ở `messages/vi.json:2843` (và `en.json:2843`) và `myProgress.*` ở `messages/vi.json:2789`; ba khối gộp từ `USR0501` (F, G, H) vẫn đọc `myProgress` (BD:400). Mã lỗi dùng `apiError.*` chung toàn app. Khoá cần **thêm**: `topicProgress.acUnavailable`, `topicProgress.solvedUnavailable`, `topicProgress.review.*` (tiêu đề, tên cột, `-`), `difficulty.solvedUnavailable`, `suggested.rankingFallback`, `reviews.titleUnknown`, `greeting.title` đổi thành có tham số `{name}`. Nhãn tiêu chí rubric `CLARITY`, `TECHNICAL_ACCURACY`, `PUSHBACK_HANDLING`, `COMPLEXITY_AWARENESS` tra ở `myProgress.interview.criteria.*` [SoT: PV ui/blocks/recent-interviews-block.tsx:76-78]. Không đặt tên endpoint, mã quyền hay `error.code` vào nhãn hiển thị.

**Truy cập.**

- Mỗi khối là một vùng (`section`) có tiêu đề; thẻ chỉ số có tên vùng "Chỉ số {metric}" để không trùng tên với khối "Mock Interview" [SoT: PV ui/blocks/stat-cards-row.tsx:15-18].
- Dải khoảng thời gian là nhóm nút một-trong-nhiều có nhãn; thay đổi được đọc bằng một vùng `role="status"` (đã có).
- **Radar** và **lưới hoạt động** là hình: mỗi cái có một bảng văn bản ẩn (`sr-only`) liệt kê trục và điểm, hoặc dòng tóm tắt, vì ô lưới không focus được `[SoT: Suy luận]`.
- Chấm trạng thái giải của bài gợi ý luôn kèm chữ (`solveState.*`), không chỉ màu.
- Nút "Thử lại" từng khối có tên có ngữ cảnh ("Thử lại: {khối}"); khi khối chuyển sang lỗi, tiêu đề khối giữ nguyên để người dùng bàn phím không mất vị trí.
- Thứ tự focus theo thứ tự DOM: lời chào và nút hành động, dải khoảng thời gian, bốn thẻ, hai khối biểu đồ, lưới, bảng chủ đề cùng cột phải, bài gợi ý cùng cột phải.

## 7. Điểm kiểm thử

Bảng 7.1: Điểm kiểm thử. `DB-AC-nn` thuộc `04-tdd/screens/users/USR0601_dashboard.md`; `F<n> AC-nn` thuộc module.

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| T-DB-01 | Normal | EVT-1: tài khoản có dữ liệu, mười một khối hiện đủ từ chín nguồn mock | Vitest | DB-AC-01 |
| T-DB-02 | Abnormal | EVT-1: F5 mục 13 và mục 7 lỗi, thẻ "Mock Interview", J, K hiện câu lỗi AI; chín khối còn lại nguyên | Vitest | DB-AC-01, F5 AC-37 |
| T-DB-03 | Abnormal | EVT-1: F6 mục 8 lỗi, chỉ bảng phụ F6-10 lỗi | Vitest | DB-AC-01, F6 AC-41 |
| T-DB-04 | Abnormal | EVT-1: F4 mục 12 lỗi, bảng chủ đề vẫn có "Đã giải", cột AC in `-`; radar, lời chào ở "thiếu dữ liệu", "Nên ưu tiên" ẩn | Vitest | DB-AC-06 |
| T-DB-05 | Boundary | `solved = null` (`solveStateAvailable = false`): cột "Đã giải" in `-`, radar không coi 0 | Vitest | DB-AC-06, F2 AC-06 |
| T-DB-06 | Normal | EVT-2: đổi sang `7d` chỉ gọi lại F4 mục 12 và F5 mục 13; không gọi F2, F1, F6; khối luỹ kế không đổi | Vitest | DB-AC-02 |
| T-DB-07 | Boundary | EVT-2: `range = all` dùng chung khoá query giữa bảng và radar (một lời gọi) | Vitest | DB-AC-02 |
| T-DB-08 | Normal | Lời chào, radar, thứ tự "Nên ưu tiên" cùng đọc `topicScores` F2 §13; chủ đề chưa nộp không có điểm | Vitest | TDD màn `DB-AC-03` |
| T-DB-09 | Boundary | Dưới 3 chủ đề có điểm: radar in câu "cần ít nhất 3"; lời chào vẫn nêu chủ đề yếu nhất nếu có | Vitest | DB-AC-03, DB-AC-05 |
| T-DB-10 | Normal | F2 §13 trả cùng vector 70/30 dùng để xếp bài; client không tính lại | Vitest | TDD màn `DB-AC-03` |
| T-DB-11 | Normal | EVT-3: có bài chưa xong thì nút đi `/workspace/{problemId}`; F4 rỗng hoặc lỗi thì "Bắt đầu giải bài" `/problems`, nút không vô hiệu | Vitest | DB-AC-04, F4 AC-38 |
| T-DB-12 | Security | EVT-3: bài đầu tiên đã rút hoặc `problem = null` thì bỏ qua, lấy bài kế; không có link tới bài đã rút | Vitest | DB-AC-04 |
| T-DB-13 | Normal | EVT-4: rê ô lưới hiện "{ngày}: {số} bài nộp", không gọi API | Vitest | DB-AC-08 |
| T-DB-14 | Boundary | EVT-1: lưới đúng 364 ô; `days.length != 364` thì khối lỗi, không vẽ lệch | Vitest | DB-AC-08, F1 AC-40 |
| T-DB-15 | Normal | EVT-5, 6: đổi tab độ khó rồi chip chủ đề gọi F2 mục 13 với tham số tương ứng, hai bộ lọc cộng dồn; `rankingAvailable = false` hiện dòng chú | Vitest | DB-AC-09, F2 AC-17 |
| T-DB-16 | Normal | EVT-7, 8: tên bài đi `/problems/{problemId}`; "Nên ưu tiên" đi `/problems` kèm chủ đề | Vitest | DB-AC-09 |
| T-DB-17 | Normal | EVT-9: dòng phiên mở `/mock-interview?sessionId=` ở chế độ chỉ xem đã chốt trong `USR0302` Q-302-4 | Vitest | DB-AC-10 |
| T-DB-18 | Normal | EVT-10, 11: "Tất cả" của J đi `/mock-interview`, của K đi `/submissions`; tên bài mở báo cáo, không sinh báo cáo mới | Vitest | DB-AC-10, F5 AC-17 |
| T-DB-19 | Normal | EVT-12: thử lại một khối chỉ gọi lại query của khối đó | Vitest | DB-AC-01 |
| T-DB-20 | Normal | Tài khoản mới: từng khối có trạng thái rỗng riêng, không số `0%`, không biểu đồ trống không giải thích (REQ-08) | Vitest | DB-AC-05 |
| T-DB-21 | Boundary | F6-10: chủ đề mới chưa có câu hiện `0 / 0`; không vượt 100% sau khi xoá mềm câu đã luyện | Vitest | DB-AC-07, F6 AC-22, AC-23 |
| T-DB-22 | Normal | Luồng đầy đủ: đăng nhập `STUDENT` vào `/dashboard`, đổi `range`, bấm nút hành động, không lỗi console | Playwright | DB-AC-01, DB-AC-02 |
| T-DB-23 | Abnormal | Playwright chặn mọi lời gọi tới F5: màn vẫn dùng được, hai khối AI hiện câu suy giảm | Playwright | DB-AC-01 |
| T-DB-24 | Normal | Chuỗi ngày dùng `range = 7d` cố định, không đổi khi đổi `range` | Vitest | DB-AC-02, F4 AC-39 |

## 8. Nợ prototype và câu hỏi mở

**Dòng nợ sẽ gỡ.** `DEBT` mục 12 (`USR0601_dashboard`, từ dòng 1001) và mục 13.2: các dòng "Lưới 12 tháng + chuỗi bài nộp theo ngày — sinh bằng bộ sinh số có hạt giống cố định, điều kiện tốt nghiệp: nối endpoint thật" (`06-plan/PROTOTYPE_DEBT.md`, bảng mục 13.2) và dòng "Công thức thang năng lực 70/30 — BD/DD chốt bằng số". Điều kiện tốt nghiệp của dòng thứ nhất là **nối endpoint thật**, chưa làm; điều kiện của dòng thứ hai (chốt công thức bằng số) **đã đủ** ở BR-SC-04 nhưng cần sửa mã (chủ đề chưa nộp) nên **chưa gỡ**. Không sửa file `DEBT` ở lượt này.

Bảng 8.1: Lệch giữa prototype và thiết kế.

| # | Prototype hiện tại | Thiết kế | Nguồn |
| :-: | :--- | :--- | :--- |
| P1 | Toàn bộ khối đọc mock theo tên BD cũ (`useProgressOverview`, `useTopicProgress`, `useDifficultyBreakdown`) | Hook một-endpoint theo Bảng 1.1; ghép bằng `join-topic-rows` | PRG api/queries.ts:21-58 |
| P2 | `acRate ?? 0` làm chủ đề chưa nộp có điểm thấp | Chủ đề chưa nộp không có điểm (BR-SC-04) | PV model/derive-skill-radar.ts:33 |
| P3 | Nút "Tiếp tục" đọc `useProblemListPage().inProgress` | `useLatestUnsolved` (F4 mục 11), bỏ bài đã rút | PV ui/blocks/greeting-block.tsx:38-41 |
| P4 | Bài gợi ý lấy cả trang danh sách bài rồi tự xếp và lọc ở client | F2 mục 13, lọc ở máy chủ (BR-SC-07) | PV ui/blocks/suggested-problems-block.tsx:48-56 |
| P5 | Lời chào "Chào bạn" cố định | Tên từ F1 #9 (BR-SC-09) | `messages/vi.json:2858` |
| P6 | Không có bảng phụ F6-10 | `interview-topics-table` + `useInterviewTopicProgress` | BD:417 |
| P7 | Dòng phiên phỏng vấn chưa là liên kết (EVT-9 lùi) | Liên kết `/mock-interview?sessionId=` ở chế độ chỉ xem theo `USR0302` Q-302-4 đã chốt | BD:420; DD `USR0302` BR-SC-02 |
| P8 | Khối `RecentReviews` và hook ở `views/.../api` | Chuyển sang `entities/solution-review` | PV api/queries.ts:35-46 |
| P9 | Chưa có `useErrorMessage`, chưa có khoá `apiError.*` | Theo DD-PD S3 | DD-PD:70 |
| P10 | Biểu đồ vẽ mock 7 / 14 / 13 cột | Theo F4 mục 12: 7, 14 và một cột mỗi tuần ISO | BD:414; F4:688 |

**Khoảng trống cần module khác hoặc BD xử lý** (DD không tự bù):

- **G1 đã đóng.** F2 §14A trả `solved/totalPublished` và từng mức, chỉ cho bài còn xuất bản; thẻ B và khối G dùng cùng nguồn (`DEC-2026-1024-dd-student-dashboard-and-interview-navigation`).
- **G2 đã đóng.** F1 #47 mục 0.8.4 ghi DTO `GetMyProgressOverview` và thêm `USR0601` làm consumer; thẻ Đã giải dùng F2 §14A để loại bài rút.
- **G3 đã đóng.** F2 §13 tính `topicScores` từ `SolveStatePort` F1 và F4 P5 `MyTopicSubmissionMetricsPort`, trả cùng vector dùng cho xếp bài. F2 gửi sẵn bản đồ chủ đề–bài để F4 không gọi ngược F2; không cần cổng điểm F1.
- **G4 đã đóng.** F4 mục 11 lọc bài đã rút trước khi áp `limit`; F4 mục 7 giữ cờ `withdrawn` cho lịch sử.
- **G5 đã đóng.** F4 §12 tính tỉ lệ AC theo lịch sử trong cửa sổ, kể cả lượt nộp của bài đã rút; F2 §4/§14A chỉ tính bài đang xuất bản cho tiến độ.

**Câu hỏi mở mức màn.**

Bảng 8.2: Câu hỏi mở, mặc định đề xuất và bậc ponytail.

| # | Câu hỏi | Mặc định đề xuất | Bậc |
| :-: | :--- | :--- | :-: |
| OQ-1 | Đã đóng theo `DEC-2026-1024-dd-student-dashboard-and-interview-navigation` | F2 §14A `ListMyLevelProgress`; thẻ B và khối G cùng dùng | 1 |
| OQ-2 | Đã đóng theo `DEC-2026-1024-dd-f2-topic-score-source` | F2 §13 tính và trả `topicScores`, cùng vector xếp bài gợi ý và dựng A/D/H/I | 5 |
| OQ-3 | Lọc "Bài toán gợi ý" ở máy chủ theo F2 mục 13, đổi BD EVT-5, 6 | Theo F2 (BR-SC-07) | 4 |
| OQ-4 | Khối J nhận thêm dòng tóm tắt (số phiên, điểm trung bình, mục yếu nhất trong khoảng) mà code đã vẽ và RD REQ-05 của `USR0501` đòi, nhưng BD Khu vực J chưa liệt kê | Thêm vào BD Khu vực J đúng ba giá trị F5 mục 13 đã trả (`completedSessionCount`, `averageScore`, `weakestCriterionCode`); không cần endpoint mới | 1 |
| OQ-5 | **Đã đóng theo F4 Q14:** chuỗi ngày bằng 0 khi hôm nay chưa nộp dù hôm qua còn chuỗi | Giữ `0 ngày` theo định nghĩa RD; thẻ nhắc nộp hằng ngày là lời nhắc cho chuỗi tiếp theo, client không ân hạn hay tự tính lại | 5 |
