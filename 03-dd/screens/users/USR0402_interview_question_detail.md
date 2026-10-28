# DD màn hình — Chi tiết câu hỏi phỏng vấn (`USR0402`)

> Màn dùng hai chế độ học và luyện. Hợp đồng request/response chỉ ở `03-dd/api/F6_interview-bank.md`; tiêu chí AI áp dụng thêm DD F5.

## 0. Thông tin tài liệu

**Bảng 0.1: Bìa.**

| Mục | Giá trị |
| :--- | :--- |
| Mã màn, slug | `USR0402`, `interview_question_detail` |
| Actor | A1 `STUDENT` |
| Route | `/interview-bank/{questionId}` theo App Router; BD cần đồng bộ nếu ghi path khác |
| Nguồn | `01-rd/screens/users/USR0402_interview_question_detail.md`; `02-bd/screens/users/USR0402_interview_question_detail.md` |
| Prototype | `05-coding/frontend/src/views/users/interview-question-detail/` (mock) |
| Module | F6 `interview-bank`; F5 `ai-review` qua cổng ngân sách/chấm câu trả lời |
| Phiên bản, ngày | V0.1, 2026-10-24 |
| TDD | `04-tdd/screens/users/USR0402_interview_question_detail.md`; `04-tdd/modules/F6_interview-bank.md`, `04-tdd/modules/F5_ai-review.md` |

**Bảng 0.2: Lịch sử sửa.**

| Ver | Nội dung | Ngày |
| :--- | :--- | :--- |
| V0.1 | Tạo DD màn, ghi rõ request/practice tách biệt và hành vi khi tải lại | 2026-10-24 |

**Bảng 0.3: Ký hiệu nguồn.**

| Ký hiệu | Nguồn |
| :--- | :--- |
| RD, BD | `01-rd/screens/users/USR0402_interview_question_detail.md`, `02-bd/screens/users/USR0402_interview_question_detail.md` |
| F6, V6, T6 | `03-dd/api/F6_interview-bank.md`, `03-dd/validation/F6_interview-bank.md`, `04-tdd/modules/F6_interview-bank.md` |
| V | `05-coding/frontend/src/views/users/interview-question-detail/ui/interview-question-detail-view.tsx` |
| DEBT | `06-plan/PROTOTYPE_DEBT.md` |

## 1. Bản đồ slice FSD

**Bảng 1.1: Slice của màn.**

| Lớp | Đường dẫn từ `src/` | Vai trò | Hiện trạng |
| :--- | :--- | :--- | :--- |
| app | `app/(student)/interview-bank/[questionId]/page.tsx` | Render view chi tiết; tên phân đoạn là `questionId` | Có [SoT: DEBT:2127] |
| views | `views/users/interview-question-detail/ui/interview-question-detail-view.tsx` | Hiển thị nội dung, chuyển chế độ, gửi câu trả lời, lịch sử và xác nhận rời | Có, mock/local state |
| views | `views/users/interview-question-detail/model/` | Adapter, query state, dirty guard và request generation | Đích; một số trạng thái hiện để trong component |
| entities | `entities/interview-question/api/queries.ts` | F6 #3, #4, #5, #6, #7 | Có query stubs/mock; đổi theo API DTO |
| entities | `entities/interview-question/model/types.ts` | `InterviewQuestionDetail`, `RecallDto`, `AnswerAttempt` | Có; rà soát theo contract F6 |
| shared | `ConfirmDialog`, `Markdown`, `Button`, `Textarea`, `toast`, `useT`, `useLocale` | Dùng chung; popup rời màn chỉ có “Ở lại” và “Rời màn” | `ConfirmDialog` có trong prototype [SoT: BD Sheet 8 EVT-11, EVT-12] |

Không có widget/feature mới: phần soạn, mode và lượt đang mở chỉ thuộc màn này. View giữ `answerText`, `shownAttemptNo`, `leaveTarget`; entities giữ query/mutation F6. Import theo `app → views → widgets → features → entities → shared`.

## 2. API tiêu thụ

**Bảng 2.1: Ánh xạ sự kiện sang API.**

| EVT (BD) | ID hợp đồng | Hook/query đích | Nối |
| :--- | :--- | :--- | :--- |
| EVT-1 tải chi tiết và lịch sử | F6 mục 3 `GetInterviewQuestion`; F6 mục 7 `ListMyAnswerAttempts` | `useInterviewQuestion`, `useMyAnswerAttempts` | Mock; query song song |
| EVT-2 quay lại danh sách | Không gọi API | Guarded navigation về `/interview-bank`, giữ `topic`, `q`, `status`, `level`, `bookmarked=1` khi có | Client |
| EVT-3 bookmark | F6 mục 5 `ToggleQuestionBookmark` | `toggleQuestionBookmark(questionId, bookmarked)` | Mock; optimistic, rollback khi lỗi |
| EVT-4 chuyển sang luyện | Không gọi API | Local tab state; khoá khi `practiceAvailable=false` | Client |
| EVT-5 chuyển sang học | Không gọi API | Local tab state | Client |
| EVT-6 tự chấm | F6 mục 4 `RateQuestionRecall` | `rateQuestionRecall(questionId, rating)` | Mock; ghi mức đích |
| EVT-7 soạn câu trả lời | Không gọi API | Local state, max 5000 ký tự | Client |
| EVT-8 nộp câu trả lời | F6 mục 6 `SubmitPracticeAnswer`; F5 mục 32 cổng ngân sách qua F6 | `submitPracticeAnswer(questionId, answer)` | Mock; AI feedback suy giảm theo contract F6/F5 |
| EVT-9 mở/đóng attempt | Không gọi API | Local disclosure state | Client |
| EVT-10 tải thêm history | F6 mục 7 `ListMyAnswerAttempts` | `useMyAnswerAttempts` trang kế | Mock |
| EVT-11 xác nhận rời khi draft | Không gọi API | `ConfirmDialog` | Client |
| EVT-12 ở lại/rời đi | Không gọi API | Modal action | Client |
| EVT-13 thử lượt mới | Không gọi API | Clear draft và feedback được chọn, giữ history | Client |

Không có SSE ở màn này. Chấm câu trả lời là request/response đồng bộ từ góc giao diện; không dựng luồng token streaming. Không tự retry POST sau timeout nếu chưa có idempotency key theo contract.

## 3. Trạng thái màn

**Bảng 3.1: Trạng thái và xử lý.**

| Vùng | Trạng thái | Hiển thị/xử lý | Khoá i18n |
| :--- | :--- | :--- | :--- |
| Câu hỏi | Loading | Skeleton; huỷ/loại response cũ khi `questionId` đổi | `interviewQuestionDetail.loading` |
| Câu hỏi | Not found/đã gỡ | Thông báo câu hỏi không khả dụng và link về `/interview-bank` | `interviewQuestionDetail.notFound` |
| Chế độ học | Nội dung tuỳ chọn null | Ẩn riêng phần đó; không coi là lỗi toàn trang | `interviewQuestionDetail.study.*` |
| Luyện | Đang gửi | Khoá nút gửi; giữ nội dung câu trả lời | `interviewQuestionDetail.practice.*` |
| Luyện | `429 INB-042` ngân sách khoá hoặc `429 INB-041` hạn mức giờ | Không tạo lượt; giữ nháp để thử lại sau, báo bằng mã lỗi | `apiError.INB-042`, `apiError.INB-041` |
| Luyện | `201 FAILED` với `INB-501`/`INB-502` | Lượt đã được lưu; hiện “Chưa chấm được”, refetch lịch sử, cho phép thử lượt mới; không tự gửi lại cùng câu | `interviewQuestionDetail.practice.feedbackUnavailable` |
| Luyện | `201 COMPLETED` | Hiển thị phản hồi định tính và mức đạt theo từng tiêu chí, thêm attempt vào lịch sử; không có điểm tổng | `interviewQuestionDetail.feedback.*` |
| Bookmark/recall | Lỗi ghi | Hoàn tác trạng thái lạc quan; thông báo riêng | `apiError.*` |
| Rời màn | Dirty | Modal ba lựa chọn: lưu nếu hành động được hỗ trợ, rời không lưu, ở lại theo `UnsavedChangesDialog` | `shared.unsavedChanges.*` |

Không hiển thị lỗi AI bằng cách thay toàn trang; nội dung câu hỏi và chế độ học vẫn hoạt động nếu phần luyện bị suy giảm.

## 4. Quy tắc đặc thù màn

**BR-SC-01 — Cô lập dữ liệu theo questionId.** Khi route đổi, reset state cục bộ của câu trước, huỷ request GET cũ nếu có thể và bỏ qua response đến muộn. Không để attempt của câu A xuất hiện tại câu B.

**BR-SC-02 — Gửi câu trả lời tạo attempt mới.** Kích hoạt EVT-8 khi nội dung hợp lệ và không có request đang chạy. Mỗi `POST` hợp lệ tạo một attempt độc lập; `201 FAILED` vẫn là lượt đã lưu, cần refetch F6 mục 7 rồi hiển thị trạng thái chưa chấm được. Không sửa attempt đã gửi. Khi mất kết nối và không rõ server đã nhận chưa, tải lại lịch sử trước khi cho người học chủ động nộp lượt mới; không tự retry `POST` không có idempotency key [SoT: F6:301-327; T6:102-109].

**BR-SC-03 — Tải lại và khôi phục.** Kích hoạt EVT-1 hoặc EVT-10. Lịch sử attempt lấy từ F6 mục 7, trang đầu tối đa 10 lượt; “Xem thêm” gửi `before` là `attemptNo` nhỏ nhất đang có và ghép theo thứ tự giảm dần. Câu chưa gửi chỉ nằm trong state cục bộ; khi bấm liên kết quay lại danh sách và còn chữ, EVT-11 dùng `ConfirmDialog` hai lựa chọn. Browser Back và menu header hiện không được chặn theo chốt của BD [SoT: BD Sheet 8 EVT-2, EVT-10, EVT-11].

**BR-SC-04 — Phản hồi người học là dữ liệu.** Câu trả lời, nội dung câu hỏi và phản hồi AI được render như văn bản/Markdown an toàn, không ghép vào system prompt phía client và không diễn giải nội dung như lệnh.

**BR-SC-06 — Bảo toàn bộ lọc danh sách.** Liên kết từ `USR0401` chuyển đúng các tham số đã chuẩn hoá `topic`, `q`, `status`, `level` và `bookmarked=1` khi công tắc đánh dấu bật. Nút Quay lại dựng `/interview-bank` từ chính tập tham số này, bỏ giá trị mặc định/rỗng; không dùng `router.back()` vì màn chi tiết có thể được mở trong tab mới hoặc tải lại. Query không thuộc tập cho phép bị bỏ, không cho tham số bất kỳ trở thành đường điều hướng. [SoT: `DEC-2026-1025-dd-interview-bookmark-url`; BD EVT-2]

## 5. Kiểm tra phía client

**Bảng 5.1: Kiểm đầu vào ở màn.**

| Trường | Zod phía client | Quy tắc nguồn | Khớp |
| :--- | :--- | :--- | :--- |
| `questionId` | `z.string().uuid()` trước query | V6 mục 2, `INB-V01` | Có khi nối thật; prototype đang dùng `code` trong mock [SoT: DEBT:2127] |
| `rating` | `z.enum(["KNOWN","VAGUE","FORGOTTEN"])` | V6 mục 3, `INB-V04` | Có; UI mock hiện dùng chữ thường, cần adapter khi nối |
| `bookmarked` | `z.boolean()` | V6 mục 3, `INB-V05` | Có |
| `answerText` | `z.string().max(5000).refine(v => v.trim().length > 0)` | V6 mục 4, `INB-V06`, `INB-V07` | Có; giữ nguyên nội dung hợp lệ, không lọc từ khoá |
| `before` | `z.number().int().positive().optional()` | V6 mục 2, `INB-V08` | Có; chỉ lấy từ `attemptNo` đã trả |

Server vẫn kiểm lại; schema này không quyết định rubric, hạn mức hay ngân sách.

## 6. i18n và truy cập

Tiền tố `interviewQuestionDetail.` và `apiError.`. Mode học/luyện có nhãn và trạng thái chọn; nếu dùng tab chuẩn thì hỗ trợ mũi tên, Home/End theo hành vi của thành phần chung. Nội dung Markdown giữ cấp heading. Phản hồi submit chỉ thông báo trạng thái vừa đổi bằng `aria-live="polite"`, không đọc lại toàn lịch sử. `ConfirmDialog` giữ focus bên trong và trả focus về nút Quay lại khi đóng. Bộ lọc `topic`, `q`, `status`, `level`, `bookmarked=1` từ URL danh sách được giữ nguyên trên liên kết quay lại [SoT: BD Sheet 8 EVT-1, EVT-2; `DEC-2026-1025-dd-interview-bookmark-url`].

## 7. Điểm kiểm thử

**Bảng 7.1: Điểm kiểm thử và tiêu chí.**

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| SC-01 | Normal | EVT-1 tải chi tiết/history; câu thiếu rubric vẫn học được | Vitest | F6 AC-06, AC-07, AC-21 |
| SC-02 | Normal | EVT-2/4/5/9/13 điều hướng, chuyển mode, đọc history, thử lượt mới | Playwright | F6 AC-06, AC-11 |
| SC-03 | Boundary | EVT-6 tự chấm; EVT-7/8 kiểm rỗng và độ dài, submit attempt | Vitest | F6 AC-08, AC-10, AC-19 |
| SC-04 | Abnormal | EVT-8: ngân sách khoá không tạo lượt; AI lỗi trả `201 FAILED` có lượt trong history; mất kết nối tải history trước khi submit lại | Vitest | F6 AC-15, AC-16, AC-18; F5 AC-49 |
| SC-05 | Boundary | EVT-1 đổi questionId khi GET cũ chạy không làm lẫn dữ liệu | Vitest | `interview_question_detail` AC-SC-01 |
| SC-06 | Abnormal | EVT-3 bookmark lỗi rollback; reload dùng trạng thái server | Vitest | F6 AC-05 |
| SC-07 | Normal | EVT-10 tải trang kế bằng `before`; EVT-11/12 modal hai lựa chọn | Playwright | F6 AC-21; `interview_question_detail` AC-SC-01, AC-SC-02 |

## 8. Nợ prototype và câu hỏi mở

Prototype màn đã có nhưng còn dùng mock, không có “Xem thêm”, chưa giữ bốn bộ lọc khi quay về và vẫn dùng `code` thay UUID trên route. Các nợ tương ứng ở `06-plan/PROTOTYPE_DEBT.md:890,2127` vẫn mở; chỉ gỡ sau khi code nối F6, route dùng UUID, history có phân trang và UI được duyệt.

**Câu hỏi cần xử lý trong lượt tổng hợp DD:**

| ID | Nội dung | Đề xuất mặc định | Bậc ponytail |
| :--- | :--- | :--- | :--- |
| D-1 | Prototype giữ attempt cục bộ, không tải thêm; F6 mục 7 đã có phân trang | Lấy lịch sử từ F6, thêm “Xem thêm”, không persist nháp chưa gửi | 3 |
| D-2 | `questionId` trong route thực tế hiện là `code` ở entity mock, nhưng BD và F6 dùng UUID | Khi nối thật, đổi link từ `USR0401` và route này cùng lượt theo UUID; không tự suy code thành id | 2 |
