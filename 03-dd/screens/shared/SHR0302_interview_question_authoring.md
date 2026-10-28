# DD màn hình — Soạn câu hỏi phỏng vấn (`SHR0302`)

## 0. Thông tin tài liệu

| Mục | Giá trị |
| :--- | :--- |
| Actor, route | A2/A3; `{basePath}/new`, `{basePath}/{questionId}/edit`, `basePath` là `/instructor/interview-questions` hoặc `/admin/interview-questions` |
| Nguồn | `01-rd/screens/shared/SHR0302_interview_question_authoring.md`; `02-bd/screens/shared/SHR0302_interview_question_authoring.md` Sheet 1–9; [F6](../../api/F6_interview-bank.md) §9–10, §20–23 |
| Prototype | `05-coding/frontend/src/views/shared/interview-question-authoring/ui/interview-question-authoring-view.tsx` |
| Phiên bản | V0.1, 2026-10-25; DD màn, Mode B |

## 1. Bản đồ slice FSD

| Lớp | Đường dẫn từ `src/` | Vai trò |
| :--- | :--- | :--- |
| app | `app/(admin)/admin/interview-questions/{new,[questionId]/edit}/page.tsx`, route tương ứng dưới `(instructor)` | Truyền `questionId`, `basePath`, quyền A2/A3 |
| views | `views/shared/interview-question-authoring/ui/interview-question-authoring-view.tsx` | Form bốn nhóm, trạng thái lưu, popup xem trước |
| features | `features/author-interview-question/` | Bản nháp form, kiểm rubric, lưu/xoá mềm |
| entities | `entities/interview-question/api/queries.ts`, `mutations.ts` | F6 §9–10, §20–23 |
| shared | `MarkdownEditor`, `NumberStepper`, `ConfirmDialog`, `PreviewDialog` | Dùng lại ở hai actor, dialog trả focus |

## 2. API tiêu thụ

| EVT BD | API | Hành vi màn |
| :--- | :--- | :--- |
| 1 tạo mới | F6 §9 `GET /topics`, §10 `GET /levels` | Không gọi chi tiết khi `new` |
| 2 sửa | F6 §9, §10, §20 `GET /admin/questions/{questionId}` | Tải song song, chỉ điền form khi đủ dữ liệu bắt buộc |
| 3–9, 16 | Không gọi API | Chỉnh form, câu đào sâu, rubric; xem trước từ form chưa lưu |
| 10 lưu mới | F6 §21 `POST /admin/questions` | Thành công chuyển sang `{id}/edit`, UUID từ response |
| 11 lưu sửa | F6 §22 `PUT /admin/questions/{questionId}` | Thành công cập nhật baseline |
| 13–15 xoá mềm | F6 §23 `POST /admin/questions/bulk/delete` | Chỉ một UUID; sau thành công về danh sách |

Đường `/admin/questions` là path API cho cả A2/A3 có quyền, không đồng nghĩa A2 phải dùng route giao diện `/admin`. EVT-12 nhân bản đã bỏ. Ba trường gợi ý/câu mẫu/từ khoá chỉ có trong nhập CSV ở `SHR0301`, không có ô ở màn soạn.

## 3. Trạng thái màn

| Trạng thái | Xử lý |
| :--- | :--- |
| Tạo mới đang tải danh mục | Skeleton bốn nhóm, chưa cho lưu |
| Sửa không tìm thấy/đã retired | Trạng thái không tìm thấy, liên kết về danh sách đúng `basePath` |
| Danh mục lỗi | Lỗi riêng và Thử lại; không cho lưu với ID danh mục không xác nhận được |
| Lưu đang gửi/lỗi | Khoá nút Lưu; lỗi `error.code` `INB-*` vào `apiError.*`, giữ bản nháp |
| Form chưa lưu khi rời | Cảnh báo mất thay đổi với điều hướng nội bộ và reload; chọn ở lại giữ form |
| Không có quyền | 403 ở API hiển thị trang không đủ quyền, không suy quyền từ route |

## 4. Quy tắc đặc thù màn

### BR-SC-01 Form và xem trước

`title` là câu hỏi ngắn bắt buộc tối đa 255 ký tự; `contentMarkdown` là mô tả thêm tuỳ chọn. Câu đào sâu không giới hạn số dòng và không đặt giới hạn ký tự nghiệp vụ từng dòng, chỉ trần body API. Rubric có thể rỗng; nếu có thì tổng trọng số đúng 100 và tên tiêu chí không trùng không phân biệt hoa thường. `criterionCode` là tên hiển thị do người soạn gõ, `description` để null; không seed tiêu chí. Popup “Xem như học viên” render từ bản nháp hiện thời, kể cả chưa lưu, không gọi F6 §20. Nội dung markdown là dữ liệu, escape/sanitize trước render. [SoT: BD Q1–Q7, Sheet 8]

### BR-SC-02 Lưu và xoá

Khi tạo thành công dùng UUID mới trong URL sửa; mã `IQ-%03d` là nhãn từ `displayNo`, không là khoá route. Chỉ response thành công mới xoá cờ dirty. Xoá mềm dùng F6 §23, hỏi xác nhận, không xoá vật lý hoặc cascade lịch sử; sau thành công về danh sách cùng actor. Nếu lưu lỗi, giữ bản nháp và focus trường lỗi đầu tiên. [SoT: F6 §20–23, BD EVT-10–15]

## 5. Kiểm tra phía client

| Trường | Quy tắc | Nguồn |
| :--- | :--- | :--- |
| `title` | Trim; bắt buộc, tối đa 255 ký tự | validation F6, BD Q5 |
| `contentMarkdown` | Tuỳ chọn, không đổi thành title | F6 §21–22 |
| `topicId`, `levelId` | Chọn ID từ danh mục tải thật | F6 §9–10 |
| `criterionCode` | Trim, không rỗng, tối đa 100 ký tự, duy nhất không phân biệt hoa thường | validation F6, BD Q4/Q6 |
| `weightPercent` | Số hợp lệ; tổng các dòng bằng 100 nếu có dòng | validation F6, BD Sheet 9 |
| `followUps` | Mỗi dòng là chuỗi; không tự đặt trần số dòng | BD Q1 |

## 6. i18n và truy cập

Dùng `interviewQuestionAuthoring.*`, `apiError.INB-*`. Nút thêm/xoá dòng có tên gồm vị trí dòng; sau xoá focus dòng liền kề. Tổng trọng số có thông báo văn bản và `aria-live=polite`; lỗi gắn `aria-describedby` tới ô tương ứng. Nút xem trước có tên rõ và dialog đóng bằng Esc, trả focus. Không đưa nội dung người học/soạn vào thông điệp hệ thống hoặc lệnh AI.

## 7. Điểm kiểm thử

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| IQ-A-01 | Normal | EVT-1/2 tạo mới và sửa tải đúng nguồn, UUID thay vì displayNo | Vitest | TDD màn `IQA-AC-01` |
| IQ-A-02 | Normal | EVT-4–9 thêm/xoá/sửa đào sâu và rubric, xem trước bản chưa lưu | Vitest | TDD màn `IQA-AC-01` |
| IQ-A-03 | Boundary | Rubric rỗng hoặc tổng 100 hợp lệ; 99/101, tên trùng bị chặn | Vitest | TDD màn `IQA-AC-02` |
| IQ-A-04 | Normal | EVT-10 tạo rồi chuyển URL edit, EVT-11 sửa giữ đúng ID | Playwright | TDD màn `IQA-AC-01` |
| IQ-A-05 | Abnormal | Lưu lỗi giữ bản nháp; rời màn dirty có guard | Playwright | TDD màn `IQA-AC-03` |
| IQ-A-06 | Normal | EVT-13–15 xoá mềm có xác nhận; đóng popup không gọi API | Playwright | TDD màn `IQA-AC-03` |

## 8. Nợ prototype và câu hỏi mở

Prototype còn mock store và chưa nhập riêng `title`/`contentMarkdown` theo BD Q5; cần tách hai trường khi nối F6. Ba trường AI context của CSV không thêm vào form. EVT-12 nhân bản đã bỏ. Màu badge mức mới là trung tính, không cần cột màu. Q1–Q7/Q9 của BD đã có kết luận; không còn quyết định màn chờ owner. Chỉ gỡ nợ `PROTOTYPE_DEBT.md` sau nối API. TDD màn đã có tại `04-tdd/screens/shared/SHR0302_interview_question_authoring.md`.
