# DD màn hình — Quản lý câu hỏi phỏng vấn (`SHR0301`)

## 0. Thông tin tài liệu

| Mục | Giá trị |
| :--- | :--- |
| Actor, route | A2/A3; `/instructor/interview-questions`, `/admin/interview-questions` |
| Nguồn | `01-rd/screens/shared/SHR0301_interview_question_management.md`; `02-bd/screens/shared/SHR0301_interview_question_management.md` Sheet 1–9; [F6](../../api/F6_interview-bank.md) §9–18, §19, §23–24 |
| Prototype | `05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx` |
| Phiên bản | V0.1, 2026-10-25; DD màn, Mode B |

## 1. Bản đồ slice FSD

| Lớp | Đường dẫn từ `src/` | Vai trò |
| :--- | :--- | :--- |
| app | `app/(admin)/admin/interview-questions/page.tsx`, route tương ứng `(instructor)` | Truyền basePath, quyền quản lý danh mục cho A3 |
| views | `views/shared/interview-question-management/ui/` | Bảng, popup chủ đề/độ khó/CSV, trạng thái từng vùng |
| features | `features/manage-interview-questions/` | Tìm/lọc/chọn dòng, xác nhận xoá, parse CSV |
| entities | `entities/interview-question/api/queries.ts`, `mutations.ts` | F6 §9–19, §23–24 |
| shared | `ManagedListDialog`, `DataTable`, `ConfirmDialog`, `FilePicker` | Dùng chung; không nhân đôi theo actor |

## 2. API tiêu thụ

| EVT BD | API | Hành vi màn |
| :--- | :--- | :--- |
| 1–6, 28–31, 36 | F6 §19 `GET /admin/questions`, §9 `GET /topics`, §10 `GET /levels` | Tìm, lọc, sắp cột, phân trang thật; chọn dòng chỉ ở client |
| 7, 9, 21 | Không gọi API | Mở new, edit, info theo UUID và đúng basePath |
| 11, 33–35 | F6 §23 `POST /admin/questions/bulk/delete` | Xoá mềm một/nhiều UUID sau xác nhận |
| 14–18 | F6 §9, §11–14 | A3 quản lý chủ đề; A2 chỉ đọc |
| 22–26 | F6 §10, §15–18 | A3 quản lý độ khó; A2 chỉ đọc |
| 8, 37–39 | F6 §24 `POST /admin/questions/import` | Chọn CSV, kiểm client, gửi và trình bày dòng bị bỏ qua; tệp mẫu tải phía client |

EVT-10/32 nhân bản đã bỏ. API quản trị dùng cùng path cho A2/A3 nhưng quyền thao tác danh mục chỉ A3, kiểm lại ở server.

## 3. Trạng thái màn

| Trạng thái | Xử lý |
| :--- | :--- |
| Danh sách tải/lỗi/rỗng | Skeleton, lỗi với Thử lại, hoặc phân biệt ngân hàng chưa có câu với bộ lọc không khớp |
| Danh mục lỗi | Bộ lọc tương ứng hiện lỗi, không dùng danh mục seed trong mock |
| Xoá một phần | Dùng `retiredIds`/`failedItems` F6 §23; bỏ chọn ID thành công, giữ ID thất bại và thông báo lý do |
| CSV lỗi từng dòng | Giữ popup và danh sách `skippedRows`; chỉ báo số nhập thực tế từ server |
| Danh mục bị tham chiếu | F6 trả lỗi/số tham chiếu; giữ dòng và popup, không xoá lạc quan |
| 401/403 | Điều hướng xác thực/không đủ quyền; A2 không thấy nút quản lý danh mục |

## 4. Quy tắc đặc thù màn

### BR-SC-01 Bộ lọc, phân trang và chọn dòng

Từ khoá/tab/sắp cột/kích thước trang thuộc URL để quay lại giữ ngữ cảnh; thay bộ lọc hoặc kích thước thì về trang đầu, bỏ lựa chọn từ trang cũ. Checkbox chọn tất cả chỉ chọn các dòng đang hiển thị, không ngầm chọn toàn bộ kết quả server. Bấm vùng trống của dòng đổi chọn; liên kết nội dung, nút thao tác và checkbox không kích hoạt hành vi hai lần. Mã `IQ-%03d` từ `displayNo` chỉ hiển thị; request và route luôn dùng UUID. [SoT: BD EVT-28–36, Q5/Q10]

### BR-SC-02 Danh mục và nhập CSV

Popup chủ đề/độ khó chỉ mở cho A3 có `INTERVIEW_BANK_MANAGEMENT`; thêm/sửa/đổi thứ tự/xoá gọi F6 tương ứng, phản hồi mới là baseline. `code` sinh ở server, không gửi slug tự chế; ba mức seed giữ màu giao diện, mức mới trung tính. Nút Nhập CSV chỉ hiển thị khi người dùng có `INTERVIEW_BANK_MANAGEMENT:CREATE` (BD Sheet 9 NO 2); API vẫn kiểm quyền độc lập. CSV parse ở client để báo lỗi cấu trúc sớm, server F6 §24 là nguồn quyết định `importedCount` và `skippedRows`; ba trường `suggestedApproach`, `sampleAnswer`, `coreKeywords` chỉ qua luồng này. Tệp mẫu phiên bản theo cột F6 §24, không in dữ liệu riêng tư trong log. [SoT: BD Q3, Q8–Q11; F6 §11–18, §24]

## 5. Kiểm tra phía client

| Trường | Quy tắc | Nguồn |
| :--- | :--- | :--- |
| `q`, trang/kích thước, sort | Kiểm theo F6 §19; giá trị URL lạ về mặc định, không ghép vào SQL | F6 §19, validation F6 |
| Tên chủ đề/độ khó | Trim, không rỗng; server quyết định trùng và `code` | F6 §11–18 |
| CSV | Chỉ `.csv`, kiểm header và từng dòng theo F6 §24; server kiểm lại toàn bộ | F6 §24, validation F6 |

## 6. i18n và truy cập

Dùng `interviewQuestionManagement.*`, `apiError.INB-*`. Header sắp cột là button có `aria-sort`; checkbox chọn tất cả ghi rõ “trang hiện tại”. Popup có heading, Esc và trả focus; lỗi CSV có số dòng, không chỉ màu. Sau xoá mềm, vùng `aria-live` công bố số thành công/thất bại. Nội dung câu hỏi render như dữ liệu, không HTML thô.

## 7. Điểm kiểm thử

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| IQ-M-01 | Normal | EVT-1–6/28–29 lọc, sắp, phân trang qua F6, URL giữ ngữ cảnh | Vitest | TDD màn `IQM-AC-01` |
| IQ-M-02 | Boundary | EVT-30–31/36 chọn trang hiện tại, bấm link không đổi chọn | Playwright | TDD màn `IQM-AC-01` |
| IQ-M-03 | Normal | EVT-7/9/21 mở new/edit/info theo UUID | Playwright | TDD màn `IQM-AC-01` |
| IQ-M-04 | Abnormal | EVT-11/33–35 xoá một phần giữ ID thất bại | Vitest | TDD màn `IQM-AC-02` |
| IQ-M-05 | Security | EVT-14–18/22–26 A2 không quản lý danh mục; A3 thao tác theo quyền | Playwright | TDD màn `IQM-AC-03` |
| IQ-M-06 | Boundary | EVT-8/37–39 CSV sai header/dòng; báo kết quả server, tải mẫu | Vitest | TDD màn `IQM-AC-03` |

## 8. Nợ prototype và câu hỏi mở

Prototype hiện dùng store mock, phân trang/lọc phía client và CSV ghi kho giả; phải nối F6 §19, §23–24. Dải KPI và nhân bản đã bỏ theo BD. Cần tách trạng thái “chưa có câu” với “lọc rỗng” bằng `total=0` của query không lọc so với query có lọc, không cần API mới. Các câu hỏi BD về mã, màu, slug, CSV đã có quyết định; không còn câu hỏi màn cần owner. Nợ `PROTOTYPE_DEBT.md` chỉ gỡ sau nối API; TDD màn đã có tại `04-tdd/screens/shared/SHR0301_interview_question_management.md`.
