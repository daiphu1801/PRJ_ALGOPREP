# DD Validation — Module `interview-bank` (F6)

> Chỉ ghi quy tắc kiểm mà BD chưa tự suy ra hoặc cần chốt thành mã lỗi cụ thể: trường, quy tắc, mã lỗi, **lớp** thực thi. Không lặp thông điệp giao diện ở Sheet 9 của BD màn; file này là bản cố định **ở máy chủ** của các kiểm tra đó.
> Nguồn: `02-bd/database/F6_interview-bank.md` (kiểu cột, CHECK, unique), `02-bd/security/F6_interview-bank.md`, Sheet 9 của `USR0401`, `USR0402`, `SHR0301`, `SHR0302`, `SHR0303`. Hợp đồng endpoint ở `03-dd/api/F6_interview-bank.md` (Bảng 0 giải nghĩa tên viết tắt nguồn); mã lỗi ở mục 27 của file đó.
> Bounded Context: F6 `interview-bank` · Cập nhật: 2026-10-23.

## 1. Quy ước lớp kiểm

**Bảng 1: Các lớp kiểm**

| Lớp | Vị trí | Ví dụ |
| :--- | :--- | :--- |
| Presentation | controller, `@Valid`; lỗi định dạng trả `400` (hoặc `413`) mã `INB-Vnn` | độ dài, enum, UUID |
| Application | use case; quy tắc nghiệp vụ trả `403`, `404`, `409`, `422`, `429` mã `INB-nnn` | tổng trọng số, danh mục còn dùng, hạn mức |
| Domain | bất biến của aggregate | `UserAnswer` không có hành vi sửa, tổng trọng số 100 |
| DB | `CHECK`, unique, FK `NO ACTION` | `weight_percent` 0 đến 100, unique `lower(display_name)` |

Quy tắc chung: kiểm ở máy chủ là kiểm thật; schema zod phía giao diện phải khớp file này (anti-drift 3 của `dd-generation`). **Mọi chuỗi người dùng nhập là dữ liệu**: không lọc từ khoá, không "làm sạch" nội dung câu trả lời hay tiêu đề; chỉ chặn độ dài và kiểu (phòng thủ prompt injection là ranh giới kiểu và transport, ARCH6:221-240).

**Đọc rộng và đọc nghiêm**: endpoint học viên (mục 2 của file API) **bỏ qua** tham số lọc sai (`U401:561`); endpoint ghi và endpoint quản trị **từ chối** (`400`). Hai nhóm cố ý khác nhau.

## 2. Tham số đọc (học viên)

**Bảng 2: Tham số của `GET /questions` và `GET /questions/{id}/practice-answers`**

| Trường | Quy tắc | Khi sai | Mã lỗi | HTTP | Lớp | Nguồn |
| :--- | :--- | :--- | :--- | :-: | :--- | :--- |
| `questionId` (path) | UUID, câu tồn tại và `ACTIVE` | coi như không có | `INB-010` | 404 | Application | `U401:562`, `U402:624` |
| `q` | cắt khoảng trắng, tối đa 100 ký tự | **cắt**, không lỗi | | | Presentation | `U401:560` (BD: cắt bớt, không gọi máy chủ với phần dư) |
| `status` | `ALL`, `UNSEEN`, `REVIEWING`, `KNOWN` | dùng `ALL` | | | Presentation | `U401:561` |
| `topic`, `level` | mã có trong danh mục | bỏ qua tham số | | | Application | `U401:561` |
| `bookmarkedOnly` | boolean | dùng `false` | | | Presentation | |
| `page` | số nguyên từ 1 | dùng 1; vượt cuối thì trang cuối | | | Presentation | như `DD-F2:143` |
| `drill` | boolean | dùng `false` | | | Presentation | |
| `limit` (chỉ khi `drill`) | 1 đến 20 | dùng 10 | | | Presentation | `[SoT: Suy luận]`, `U401:584` |
| `before` (mục 7) | số nguyên dương | **từ chối** | `INB-V08` | 400 | Presentation | `[SoT: Suy luận]` |

## 3. Tự chấm và đánh dấu (mục 4, 5)

**Bảng 3: Trường của `RateQuestionRecall` và `ToggleQuestionBookmark`**

| Trường | Quy tắc | Mã lỗi | HTTP | Lớp | Nguồn |
| :--- | :--- | :--- | :-: | :--- | :--- |
| `rating` | bắt buộc; một trong `KNOWN`, `VAGUE`, `FORGOTTEN`; không có giá trị thứ tư (không "Chưa xem": không phải lựa chọn tự chấm) | `INB-V04` | 400 | Presentation | RD6:36-37, `U401:610` Q6 |
| `bookmarked` | bắt buộc; boolean thật (không nhận chuỗi `"true"`) | `INB-V05` | 400 | Presentation | `[SoT: Suy luận]` |
| Câu `ACTIVE` | `status = 'ACTIVE'` | `INB-010` | 404 | Application | `U401:562` |
| Chủ sở hữu | `user_id` của dòng bằng `user_id` của token; không nhận `userId` từ body | | | Application và truy vấn | SEC6:27-29 |
| `interval_days` | `FORGOTTEN`=1, `VAGUE`=3, `KNOWN`=7; client **không** gửi | | | Domain | DB6:187 |

## 4. Nộp câu trả lời luyện (mục 6)

**Bảng 4: Trường của `SubmitPracticeAnswer`**

| Trường | Quy tắc | Mã lỗi | HTTP | Lớp | Nguồn |
| :--- | :--- | :--- | :-: | :--- | :--- |
| `answerText` | sau khi cắt khoảng trắng hai đầu còn ít nhất 1 ký tự | `INB-V06` | 400 | Presentation, và lặp lại ở Application | `U402:626` (kiểm cả màn và máy chủ) |
| `answerText` | `String.length() <= 5000`; không cắt, không chuẩn hoá, không lọc ký tự, không lọc từ khoá | `INB-V07` | 400 | Presentation | `DEC-2026-1018-usr-open-questions-phase2-5`, DB6:123, `U402:627` |
| Rubric của câu | có ít nhất một dòng `answer_rubrics` | `INB-040` | 422 | Application | `U402:625`, DB6:75-78 |
| Hạn mức giờ | không quá 20 lượt mỗi giờ mỗi người (Redis) | `INB-041` | 429 | Application | DB6:256, `U402:628` |
| Ngân sách | `CheckAiBudgetUseCase` không ném `AiBudgetExceededException` | `INB-042` | 429 | Application | DEC-1011, `U402:629` |
| `attempt_no` | tính ở máy chủ trong giao dịch; client không gửi; unique `(user_id, question_id, attempt_no)` | `INB-050` | 409 | Application và DB | SEC6:96-98, DB6:124, 144 |
| Bất biến | không có use case, route hay phương thức nào sửa `user_answers` | | | Domain | ARCH6:214-219, SEC6:92-95 |
| `answerText` là dữ liệu | chỉ đi vào user message; không có hàm dựng system instruction nào nhận chuỗi người dùng | | | Domain (kiểu của `AnswerFeedbackPort`) | ARCH6:227-232 |

## 5. Soạn câu hỏi (mục 21, 22)

**Bảng 5: Trường của `CreateInterviewQuestion` và `UpdateInterviewQuestion`**

| Trường | Quy tắc | Mã lỗi | HTTP | Lớp | Nguồn |
| :--- | :--- | :--- | :-: | :--- | :--- |
| `title` | bắt buộc; sau khi cắt 1 đến 255 ký tự | `INB-V08` | 400 | Presentation, DB `VARCHAR(255) NOT NULL` | DB6:62, `S302:614` |
| `contentMarkdown` | tuỳ chọn; rỗng lưu `NULL`; chỉ có trần kỹ thuật theo kích thước body | `INB-V17` (413) | 413 | Presentation | DB6:63, `S302` Q1 |
| `topicId`, `levelId` | bắt buộc, UUID | `INB-V09` | 400 | Presentation | DB6:60-61 |
| `topicId`, `levelId` tồn tại | có dòng ở `question_topics`, `question_levels`; thiếu thì từ chối; FK `NO ACTION` là lưới an toàn khi đang bị xoá song song | `INB-035` | 422 | Application, DB FK | `[SoT: Suy luận]` mã |
| `followUpQuestions` | mảng chuỗi, mặc định `[]`; phần tử cắt khoảng trắng, **phần tử rỗng bị bỏ** (chuẩn hoá, không lỗi); không giới hạn số lượng và độ dài từng dòng | `INB-V12` (sai kiểu) | 400 | Presentation | `S302` Q1, DB6:67 |
| `rubric` | mảng, mặc định `[]` | `INB-V10` (sai kiểu phần tử) | 400 | Presentation | |
| `rubric[].criterionCode` | cắt khoảng trắng, 1 đến 100 ký tự | `INB-V10` | 400 | Presentation, DB `VARCHAR(100)` | DB6:90 |
| `rubric[].criterionCode` | không trùng trong cùng câu, không phân biệt hoa thường | `INB-V11` | 400 | Application, DB unique `(question_id, lower(criterion_code))` | DB6:96, `S302:617` |
| `rubric[].weightPercent` | số trong [0, 100], tối đa 2 chữ số thập phân | `INB-V10` | 400 | Presentation, DB `CHECK BETWEEN 0 AND 100`, `DECIMAL(5,2)` | DB6:92, `S302:616` |
| Tổng trọng số | có ít nhất một tiêu chí thì Σ `weightPercent` đúng 100 (cộng bằng `BigDecimal`); rỗng thì không kiểm | `INB-033` | 422 | Application (SQL không có ràng buộc tổng nhóm) | DB6:92, `S302:615` |
| `rubric[].description` | tuỳ chọn, `null` ở màn hiện tại; có thì là văn bản, không vào system message nguyên văn (chỉ qua `<reference_rubric>` đã thay `<` `>`) | | | Domain | `S302` Q7, ARCH6:231 |
| Số tiêu chí | không giới hạn | | | | `S302` Q4 |
| Thứ tự | `sort_order = 1..n` theo thứ tự mảng, máy chủ gán; unique `(question_id, sort_order)` | | | Application, DB | DB6:93, 96 |
| Ba cột chỉ-CSV | **không** nằm trong request; sửa câu không chạm | | | Application | `S302` Q3 |
| `display_no` | do sequence máy chủ, client không gửi; không tái sử dụng | | | DB | DB6:59 |
| Quyền | `INTERVIEW_BANK_MANAGEMENT:CREATE|UPDATE`; không điều kiện `created_by` | `INB-301` | 403 | Presentation (`@PreAuthorize`) | SEC6:34-47 |
| Trần body | 512 KiB = 524288 byte UTF-8 | `INB-V17` | 413 | Presentation | Chủ dự án chốt 2026-10-28 |

## 6. Danh mục chủ đề và độ khó, xoá câu hỏi (mục 11 đến 18, 23)

**Bảng 6: Trường của quản lý danh mục**

| Trường | Quy tắc | Mã lỗi | HTTP | Lớp | Nguồn |
| :--- | :--- | :--- | :-: | :--- | :--- |
| `displayName` | cắt khoảng trắng, 1 đến 60 ký tự | `INB-V03` | 400 | Presentation, DB `VARCHAR(60)` | DB6:20, 42 |
| `displayName` duy nhất | không trùng không phân biệt hoa thường (chủ đề với chủ đề, mức với mức) | `INB-020` (chủ đề), `INB-021` (mức) | 409 | Application, DB unique index `lower(display_name)` | DB6:20, 42, 222 |
| `code` | **không nhận từ client**; sinh theo BR-IB-01; `VARCHAR(64)` unique, bất biến | | | Application, DB | `DEC-2026-1023-dd-f5-f6-open-choices` |
| `usesStarFramework` | boolean khi có | `INB-V13` nếu body rỗng | 400 | Presentation | DB6:22 |
| `orderedIds` | mảng UUID | `INB-V14` | 400 | Presentation | |
| `orderedIds` đúng tập | bằng đúng tập id hiện có, không thiếu, không thừa, không trùng | `INB-034` | 422 | Application | `[SoT: Suy luận]`, như `PRB-049` |
| Xoá chủ đề | không còn `interview_questions.topic_id` (kể cả `RETIRED`) | `INB-030` | 422 | Application, FK `NO ACTION` | DB6:25-29 |
| Xoá độ khó | không còn `level_id` trỏ tới (kể cả `RETIRED`) | `INB-031` | 422 | Application, FK `NO ACTION` | DB6:49-52 |
| Xoá độ khó cuối | còn ít nhất một mức sau khi xoá | `INB-032` | 422 | Application (không có ràng buộc SQL) | DB6:50-52 |
| Quyền | Function + vai trò `ADMIN`; A2 gọi trực tiếp bị từ chối | `INB-301`, `INB-302` | 403 | Application | SEC6:48-84 |
| `ids` xoá theo lô | 1 đến 100 UUID, không trùng | `INB-V02` | 400 | Presentation | Chủ dự án chốt 2026-10-28 |
| Câu để xoá | tồn tại và chưa `RETIRED`; sai thì vào `failedItems`, không làm hỏng cả lô | `INB-010` (trong `failedItems`) | 200 | Application | `S301` EVT-34 |
| Không cascade | xoá mềm không xoá `answer_rubrics`, `user_answers`, `recall_ratings`, `bookmarks` | | | Application | DB6:68, 96-98 |

## 7. Kết quả AI của Chế độ luyện (lược đồ `feedback_result_json`)

Kiểm sau khi nhận kết quả, **trước** khi ghi DB. Sai lược đồ thì lượt vẫn ghi với `feedback_status = FAILED`, `feedback_result_json = NULL`, phản hồi `201` mang `feedbackErrorCode = INB-502` (không phải `502`): câu trả lời không mất. Token đã tiêu vẫn ghi.

**Bảng 7: Kiểm kết quả F6-08**

| Khoá | Quy tắc | Xử lý khi sai |
| :--- | :--- | :--- |
| `achievedPoints`, `missingPoints`, `improvementSuggestions` | mảng chuỗi (có thể rỗng), tối đa 10 phần tử, mỗi phần tử cắt về 500 ký tự (`[SoT: Suy luận]`, BD chỉ nói "ngắn", ARCH6:144) | thiếu khoá: coi là mảng rỗng; kiểu sai: `INB-502` |
| `criteria` | mảng **đúng** một phần tử cho mỗi `criterionCode` của `rubric_snapshot_json`, không trùng, không dư (DB6:140-141) | thiếu, dư hoặc trùng: `INB-502` |
| `criteria[].criterionCode` | khớp (không phân biệt hoa thường) một tiêu chí của snapshot; máy chủ **chuẩn hoá** về đúng chuỗi của snapshot | không khớp: `INB-502` |
| `criteria[].achievementPercent` | số nguyên trong [0, 100] | ngoài khoảng hoặc không phải số nguyên: `INB-502` |
| `criteria[].weightPercent` | **không do AI sinh**; máy chủ sao từ snapshot | luôn ghi đè |
| Điểm tổng, ngưỡng đạt, khoá lạ | không có; khoá lạ bị bỏ, không lưu (DB6:139-141) | chuẩn hoá |

## 8. Nhập CSV (mục 24)

**Bảng 8: Kiểm từng dòng `ImportRow`** (máy chủ kiểm lại; lỗi đầu tiên gặp thì dừng dòng đó)

| Thứ tự | Quy tắc | `reasonCode` | Nguồn |
| :-: | :--- | :--- | :--- |
| 0 | `rows` có 1 đến 500 phần tử; ngoài khoảng thì từ chối cả tệp | (`INB-V15`, 400) | `S301:857` Q11 (d) |
| 1 | `topic`, `level`, `question` không rỗng sau khi cắt | `missingField` | Q11 (a) |
| 2 | `topic` khớp `code` hoặc `display_name` (không phân biệt hoa thường) | `unknownTopic` | `csv-import.ts:157` |
| 3 | `level` như trên | `unknownLevel` | `csv-import.ts:162` |
| 4 | `rubric`: mỗi phần tử có tên (1 đến 100 ký tự), trọng số trong [0, 100]; tên không trùng | `badRubric` | `csv-import.ts:92-110` |
| 5 | có tiêu chí thì tổng đúng 100 | `rubricSum` | `csv-import.ts:173-179` |
| 6 | `question` tối đa 255 ký tự | `invalidField` (DD thêm) | DB6:62 |
| 7 | `lower(btrim(question))` chưa có ở câu `ACTIVE` nào và chưa gặp ở dòng trước trong tệp | `duplicate` | Q11 (c) |

`topic`, `level` ở tệp do giao diện đổi sang `code` trước khi gửi, máy chủ vẫn chấp nhận cả `display_name` (`S301:693`). Danh sách `followUps`, `coreKeywords`, `suggestedApproach`: phần tử rỗng bị bỏ.

## 9. Ràng buộc ở tầng DB (tóm tắt, để kiểm chéo với mục trên)

**Bảng 9: Ràng buộc DB**

| Bảng | Ràng buộc | Nguồn |
| :--- | :--- | :--- |
| `question_topics` | `code` `VARCHAR(64)` unique; unique `lower(display_name)`; `display_name` `VARCHAR(60)`; `sort_order` NOT NULL | DB6:19-21 |
| `question_levels` | như trên | DB6:41-43 |
| `interview_questions` | `display_no` unique NOT NULL; `topic_id`, `level_id` FK `NO ACTION` NOT NULL; `title` `VARCHAR(255)` NOT NULL; `follow_up_questions` JSONB NOT NULL default `'[]'`; `created_by` NOT NULL | DB6:59-69 |
| `answer_rubrics` | `weight_percent` `CHECK BETWEEN 0 AND 100`; unique `(question_id, lower(criterion_code))` và `(question_id, sort_order)` | DB6:92-96 |
| `bookmarks` | unique `(user_id, question_id)` | DB6:109 |
| `user_answers` | unique `(user_id, question_id, attempt_no)`; `rubric_snapshot_json` NOT NULL; không `updated_at` | DB6:124-144 |
| `practice_history` | unique `(user_id, topic_id)`; `questions_attempted` NOT NULL default 0 | DB6:159-163 |
| `recall_ratings` | unique `(user_id, question_id)`; `rating`, `interval_days`, `next_review_at`, `rated_at` NOT NULL | DB6:186-191 |
| Cần thêm (đề xuất, api mục 28 D5) | index `user_answers(question_id)` cho `usageCount`, `averageScorePercent` | |

## 10. Khớp với schema phía giao diện

Schema zod của `USR0402` (câu trả lời), `SHR0302` (câu hỏi, tiêu chí) và `SHR0301` (tên chủ đề, độ khó, tệp CSV) phải khớp: `answerText` 1 đến 5000; `title` 1 đến 255; `displayName` 1 đến 60; tên tiêu chí 1 đến 100; trọng số 0 đến 100 và tổng 100 khi có tiêu chí; tệp CSV tối đa 500 dòng. Khác biệt có chủ ý: giao diện được phép **cắt** `q` và chặn nhập; máy chủ cắt chứ không lỗi ở endpoint học viên. Hằng số 5000 giao diện giữ một bản để hiển thị bộ đếm (`U402:627`).
