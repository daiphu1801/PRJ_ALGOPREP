# DD màn hình — Chi tiết câu hỏi phỏng vấn chỉ đọc (`SHR0303`)

## 0. Thông tin tài liệu

| Mục | Giá trị |
| :--- | :--- |
| Actor, route | A2 `/instructor/interview-questions/{questionId}`; A3 `/admin/interview-questions/{questionId}`; `questionId` UUID |
| Nguồn | `01-rd/screens/shared/SHR0303_interview_question_info.md`; `02-bd/screens/shared/SHR0303_interview_question_info.md` V0.6; `03-dd/api/F6_interview-bank.md` §20 |
| Prototype | `05-coding/frontend/src/views/shared/interview-question-info/ui/interview-question-info-view.tsx` |
| Phiên bản | V0.1, 2026-10-25; DD trục màn, Mode B (agent-native) |

Mã `IQ-%03d` lấy từ `displayNo` để hiển thị; không dùng mã làm route. Câu `RETIRED`, UUID sai hoặc không có cùng trả `404 INB-010` theo F6 §20. [SoT: `DEC-2026-1022-interview-bank-schema-sync`; F6 §20]

## 1. Bản đồ slice FSD

| Lớp | Đường dẫn từ `src/` | Vai trò |
| :--- | :--- | :--- |
| app | `app/(admin)/admin/interview-questions/[questionId]/page.tsx`; `app/(instructor)/instructor/interview-questions/[questionId]/page.tsx` | Truyền UUID và `basePath` cố định theo khu |
| views | `views/shared/interview-question-info/ui/interview-question-info-view.tsx` | Trang chỉ đọc: nội dung, câu đào sâu, rubric, phân loại, sử dụng |
| features | `features/interview-question-preview/` | Popup “Xem như học viên” dùng cùng thành phần với `SHR0302`; tại màn này nhận dữ liệu **đã lưu** từ F6 §20 |
| entities | `entities/interview-question/api/queries.ts`, `model/admin-question-adapter.ts` | Đọc chi tiết F6, danh mục chủ đề/mức; chuyển DTO thành props trình bày; bỏ dữ liệu mock theo mã `IQ-*` |
| shared | `MarkdownPreview`, `Card`, `Badge`, `QueryBoundary`, `EmptyState`, `ErrorState`, `Dialog` | Dùng lại, không nhân bản renderer Markdown/LaTeX |

`InterviewQuestionInfoView` nhận `{ questionId: string; basePath: "/admin/interview-questions" | "/instructor/interview-questions" }`. `basePath` không đến từ URL nhập tay. Mã `IQ-*` chỉ tạo từ `displayNo` sau khi query thành công. [SoT: BD SHR0303 Sheet 3; `interview-question-info-view.tsx:27-45`]

## 2. API tiêu thụ

| Sự kiện | Endpoint | Việc làm |
| :--- | :--- | :--- |
| EVT-1 mở màn | F6 #20 `GET /admin/questions/{questionId}` | Đọc nội dung, rubric, `usage` trong **một** response; hiện mock `useInterviewQuestion` cần thay |
| EVT-1 nhãn chủ đề/mức | F6 §9 `GET /topics`, §10 `GET /levels` | Query danh mục dùng chung với `SHR0301`/`SHR0302`; không dùng hằng ba mức |
| EVT-2 quay lại | Không gọi API | Link về `basePath` |
| EVT-3 Sửa | Không gọi API | Link `{basePath}/{questionId}/edit`, chỉ khi có quyền `UPDATE` |
| EVT-4 Xem như học viên | Không gọi API | Popup từ DTO đã lưu, không gửi dữ liệu tới F6/F5 |

BD Q3 được F6 §20 đóng: `usage` đi cùng chi tiết, không có endpoint chỉ số thứ hai. Điểm trung bình trong F6 là phần trăm 0–100; nhãn mock `x / 5` cần bỏ, hiển thị phần trăm một chữ số thập phân hoặc `-` khi `null`. [SoT: F6 §19–20, §28 Q3]

## 3. Trạng thái màn

| Điều kiện | Hành vi |
| :--- | :--- |
| Đang tải | Skeleton trong `QueryBoundary`; không hiện câu mock khác cho UUID chưa nhận |
| `404 INB-010` | Tiêu đề “Không tìm thấy câu hỏi”, nút quay lại; cùng một nội dung cho câu đã `RETIRED`, UUID sai và không tồn tại |
| `403 INB-301` | Trạng thái không đủ quyền, không hiện title/rubric/usage |
| Mạng/5xx | Trạng thái lỗi có thử lại chi tiết; không biến thành “không tìm thấy” |
| `contentMarkdown=null` | Chỉ hiện câu hỏi chính `title`, không tạo đoạn mô tả rỗng |
| Không có câu đào sâu/rubric | Thẻ tương ứng hiện giải thích ngắn; `practiceAvailable=false` khi rubric trống |
| Chưa có lượt `COMPLETED` | `usage.usageCount` hiển thị số nguồn trả; `averageScorePercent=null` hiện `-`, không hiện `0%` |
| Danh mục chủ đề/mức lỗi | Nội dung câu hỏi vẫn hiện; nhãn phân loại dùng trạng thái “Không tải được” và thử lại danh mục |

## 4. Quy tắc đặc thù màn

### BR-SC-01 Một nguồn cho nội dung và số liệu

F6 §20 trả `title`, `contentMarkdown`, câu đào sâu, rubric theo `sortOrder`, `practiceAvailable` và `usage`. `title` là câu hỏi chính; `contentMarkdown` chỉ là diễn giải thêm, render khi có. Tổng trọng số rubric tính tại client chỉ để trình bày, không sửa/chuẩn hoá trọng số. Chỉ số điểm dùng `averageScorePercent` của F6, không đổi thang sang `/5` và không tính lại từ `user_answers`. [SoT: F6 §20.3; BD SHR0303 Sheet 5 khu B/C]

### BR-SC-02 Xem trước và quyền

Nút “Xem như học viên” mở popup dùng cùng renderer với `SHR0302`: Chế độ học hiện nội dung và câu đào sâu; Chế độ luyện hiện rubric đủ điều kiện hoặc cảnh báo chưa mở khi `practiceAvailable=false`. `SHR0302` xem trước từ form chưa lưu; màn này xem trước từ DTO F6 đã lưu. Không cần endpoint preview mới. Nút Sửa chỉ hiện khi quyền `INTERVIEW_BANK_MANAGEMENT:UPDATE` có thật; route `/edit` vẫn bị máy chủ bảo vệ, ẩn nút không phải kiểm quyền duy nhất. [SoT: BD SHR0302 Q2; F6 §20]

## 5. Kiểm tra phía client

Màn không có form. `questionId` phải là UUID; chuỗi khác định dạng đi tới trạng thái không tìm thấy, không gọi F6. Response được kiểm ở `entities/interview-question`: `contentMarkdown` và `usage.averageScorePercent` nullable, `rubric` là mảng, `displayNo` nguyên dương. Không áp quy tắc nhập của `SHR0302` lên màn chỉ đọc. Không cho `displayNo` giả hoặc điểm trung bình mock lọt vào adapter thật.

## 6. i18n và truy cập

Dùng `interviewQuestionInfo.*`, nhãn chung `interviewQuestionAuthoring.*`, `apiError.INB-*` cho lỗi. `title` và `contentMarkdown` là dữ liệu, React/`MarkdownPreview` escape HTML thô; không chạy script trong nội dung câu. Mỗi thẻ có heading; rubric là danh sách với tên tiêu chí và phần trăm đọc được, không chỉ dựa vào màu badge. Nút popup trả focus về nút mở sau khi đóng, hỗ trợ Esc; nút Sửa/Quay lại có nhãn rõ. Khi danh mục tải xong, vùng phân loại cập nhật `aria-live=polite`.

## 7. Điểm kiểm thử

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| IQ-01 | Normal | UUID hợp lệ gọi F6 §20 một lần; `displayNo` thành `IQ-%03d`, không là route | Vitest | TDD màn `IQI-AC-01` |
| IQ-02 | Abnormal | UUID sai, không tồn tại, `RETIRED` đều cùng trạng thái `INB-010` | Vitest | TDD màn `IQI-AC-01` |
| IQ-03 | Security | Thiếu READ không hiện nội dung; thiếu UPDATE không hiện nút Sửa | Vitest | TDD màn `IQI-AC-02` |
| IQ-04 | Boundary | `contentMarkdown=null`, rubric/câu đào sâu trống, điểm `null` hiển thị đúng | Vitest | TDD màn `IQI-AC-02` |
| IQ-05 | Normal | Điểm dùng thang phần trăm, tên mức mới có badge trung tính, không in `/5` | Vitest | TDD màn `IQI-AC-02` |
| IQ-06 | Normal | Preview dùng bản đã lưu, đóng popup giữ nguyên trang và focus | Playwright | TDD màn `IQI-AC-03` |
| IQ-07 | Abnormal | Query danh mục lỗi không che câu hỏi; retry chi tiết/danh mục độc lập | Vitest | TDD màn `IQI-AC-03` |

## 8. Nợ prototype và câu hỏi mở

Prototype còn dùng mã `IQ-*` làm route, `useInterviewQuestion` đọc mock, nút Preview chưa gắn hành động, điểm mock `x / 5`. F6 §20 đã đóng các nguồn dữ liệu: `usage` có trong chi tiết, điểm thang phần trăm, `RETIRED` trả 404. Bản dựng cần nối UUID từ danh sách `SHR0301` và popup dùng chung với `SHR0302`; nợ ở `06-plan/PROTOTYPE_DEBT.md` chỉ gỡ sau khi nối API thật.

| # | Câu hỏi | Kết luận DD |
| :--- | :--- | :--- |
| OQ-1 | Câu đã `RETIRED` có cho xem chỉ đọc? | Không; F6 §20.4 trả `404 INB-010` |
| OQ-2 | Điểm trung bình và lượt dùng lấy qua lời gọi nào? | Cùng F6 §20, `usage.averageScorePercent` nullable; không gọi riêng |
| OQ-3 | Preview ở màn chỉ đọc lấy bản đang sửa hay bản đã lưu? | Bản đã lưu từ F6 §20; chia renderer với popup của `SHR0302` |

TDD màn đã có tại `04-tdd/screens/shared/SHR0303_interview_question_info.md`.
