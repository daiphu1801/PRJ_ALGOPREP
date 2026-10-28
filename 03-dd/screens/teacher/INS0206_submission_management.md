# DD màn hình — Quản lý bài nộp (`INS0206`)

## 0. Thông tin tài liệu

Bảng 0.1 là bìa.

| Mục | Giá trị |
| :--- | :--- |
| Mã, actor, route | `INS0206`, INSTRUCTOR, `/instructor/submissions` |
| Nguồn | `01-rd/screens/teacher/INS0206_submission_management.md`; `02-bd/screens/teacher/INS0206_submission_management.md` |
| Prototype | `05-coding/frontend/src/views/teacher/submission-management/` |
| Module | F1 identity #19; F4 judge-orchestration #10 |
| TDD | `04-tdd/modules/F4_judge-orchestration.md` AC-41–44; `04-tdd/screens/teacher/INS0206_submission_management.md` |
| Phiên bản, ngày | V0.1, 2026-10-24 |

Bảng 0.2 ghi lịch sử.

| Ver | Nội dung | Ngày |
| :--- | :--- | :--- |
| V0.1 | Thiết kế nối F1/F4, đồng bộ lọc với URL và phân vùng lỗi | 2026-10-24 |

Bảng 0.3 là ký hiệu nguồn.

| Ký hiệu | Nguồn |
| :--- | :--- |
| BD | `02-bd/screens/teacher/INS0206_submission_management.md` Sheet 5, 8, 9 |
| F1, F4 | `03-dd/api/F1_identity.md` #19; `03-dd/api/F4_judge-orchestration.md` #10 |
| V | `05-coding/frontend/src/views/teacher/submission-management/ui/submission-management-view.tsx` |

## 1. Bản đồ slice FSD

Bảng 1.1 là ranh giới slice, tính từ `05-coding/frontend/src/`.

| Lớp | Đường dẫn | Vai trò | Hiện trạng |
| :--- | :--- | :--- | :--- |
| app | `app/(instructor)/instructor/submissions/page.tsx` | Render view, nhận URL | Có |
| views | `views/teacher/submission-management/ui/submission-management-view.tsx` | Thanh lọc, bảng, ba empty state, retry | Có, mock |
| views | `views/teacher/submission-management/model/` | Parse/serialize URL và giữ query theo bốn tham số | Đích DD |
| entities | `entities/class/api/queries.ts` | Danh sách lớp F1 #19 | Mock |
| entities | `entities/submission/api/queries.ts` | Danh sách giảng viên F4 #10 | Mock |
| shared | DataTable, Select, Pagination, ErrorState, Skeleton, toast, `useT` | Dùng chung, không nâng cấp | Có |

Không có widget/feature riêng. View chỉ chuyển tham số UI sang F4, không lọc bài nộp theo lớp ở client; F4 kiểm quyền từng request. Import `app → views → entities → shared`. `[SoT: BD Sheet 4.5, 9 NO 1–4]`

## 2. API tiêu thụ

Bảng 2.1 ánh xạ tất cả EVT đang hiệu lực trong Sheet 8.

| EVT | API ID | Hook/query đích | Hiện trạng |
| :--- | :--- | :--- | :--- |
| EVT-1 khởi tạo | F1 #19 `ListInstructorClasses`; F4 #10 `ListInstructorSubmissions` | `useInstructorClasses`, `useInstructorSubmissions(params)` | Mock |
| EVT-2 tìm kiếm | F4 #10, sau debounce; `q` từ URL | `useInstructorSubmissions` | Mock |
| EVT-3 lọc lớp | F4 #10, `classId` từ URL | như trên | Mock |
| EVT-4 lọc kết quả | F4 #10, `result` từ URL | như trên | Mock |
| EVT-5 đổi trang | F4 #10, `page` từ URL | như trên | Mock |
| EVT-6 mở học viên | Không API; tới `INS0204` với `classId`, `studentId` của dòng | `next/link` | Có |
| EVT-7 mở kết quả | Không API; `/instructor/submissions/{submissionId}` | `next/link` | Có |
| EVT-8 thử lại | Refetch F1/F4 đúng tham số URL hiện tại | `refetch()` | Cần tách lỗi |
| EVT-9 bỏ lọc lớp | F4 #10 sau khi bỏ `classId`, reset trang 1 | `useInstructorSubmissions` | Cần đồng bộ URL |

Màn không dùng realtime. Danh sách F4 chỉ chứa metadata, không trả mã nguồn/testcase; mã chỉ đọc ở trang kết quả đã kiểm quyền. `[SoT: BD Sheet 9 NO 4; F4 #10]`

## 3. Trạng thái màn

Bảng 3.1 là trạng thái và cách ánh xạ `error.code`; không dùng `message` server.

| Vùng | Trạng thái | Xử lý | i18n |
| :--- | :--- | :--- | :--- |
| Toàn màn | 401/403 | Chuyển auth hoặc trang không có quyền theo shell | `apiError.*` |
| Bảng | F4 tải | Skeleton tại bảng; giữ bộ lọc | `instructorSubmissions.*` |
| Bảng | Chưa có lớp | Thông báo tạo lớp, khác danh sách rỗng | `instructorSubmissions.*` |
| Bảng | Có lớp, chưa có lượt nộp | Empty state riêng | `instructorSubmissions.*` |
| Bảng | Bộ lọc không khớp | Empty state kèm xoá lọc | `instructorSubmissions.*` |
| Bảng | `JDG-111`/`JDG-301` do lớp ngoài phạm vi | Không tìm thấy lớp kèm “Xem tất cả bài nộp” | `instructorSubmissions.*` |
| Bảng | Mạng/5xx | Vùng lỗi và Thử lại; chỉ retry lỗi lặp mới toast | `apiError.*` |
| Dòng | Chưa chấm hoặc `totalTestcaseCount=0` | Tỉ lệ testcase là `-`, không phải `0/0` | `instructorSubmissions.*` |

## 4. Quy tắc đặc thù màn

**BR-SC-01 — URL là nguồn bộ lọc.** Kích hoạt EVT-1–5, 9 khi URL đổi hoặc trình duyệt Back/Forward. Parse `classId`, `result`, `q`, `page`; `result` lạ về `all`, `page` sai về 1, `q` trim và cắt 100 ký tự theo F4. Gọi F4 với query key gồm cả bốn giá trị; thay đổi tìm kiếm/lớp/kết quả reset trang 1. Hậu điều kiện: link chia sẻ mở đúng bảng, Back/Forward tái hiện lọc. `[SoT: BD Sheet 8 EVT-1–5, 9]`

**BR-SC-02 — Tải và lỗi từng nguồn.** Kích hoạt EVT-1/8. F1 lớp để dựng menu; F4 bảng theo URL. Nếu F1 rỗng, hiện “chưa có lớp” và không gọi F4 theo BD. F4 lỗi giữ thanh lọc, hiện retry tại bảng; retry dùng đúng URL hiện thời và mới toast nếu lỗi lặp. Hậu điều kiện: dữ liệu lớp và bảng không làm mất trạng thái của nhau. `[SoT: BD Sheet 8 EVT-1, 8]`

**BR-SC-03 — Điều hướng bảo toàn ngữ cảnh.** Kích hoạt EVT-6/7. Mở học viên chỉ khi dòng thuộc lớp còn hiệu lực; `classId` và `studentId` lấy từ dòng F4. Mở kết quả với `submissionId`, giữ `classId`/nguồn ở query để sidebar và Back hoạt động, nhưng trang đích tự kiểm quyền bằng ID chứ không tin query. `[SoT: BD Sheet 8 EVT-6, 7; F4 #11]`

## 5. Kiểm tra phía client

Bảng 5.1 là kiểm URL; server F1/F4 kiểm lại phạm vi.

| Trường | Zod/client | Nguồn | Khớp |
| :--- | :--- | :--- | :--- |
| `classId` | UUID nếu có; sai dạng vào cùng trạng thái không tìm thấy lớp | `03-dd/validation/F4_judge-orchestration.md` mục 5; BD Sheet 9 NO 5 | Có |
| `result` | `z.enum(["all","accepted","failed"]).catch("all")` | BD Sheet 9 NO 6; F4 #10 | Có |
| `q` | trim, cắt 100 ký tự, debounce | BD Sheet 9 NO 7; F4 #10 | Có |
| `page` | integer ≥1, sai về 1; quá tổng về trang cuối | BD Sheet 9 NO 7; F4 #10 | Có |

## 6. i18n và truy cập

Tiền tố `instructorSubmissions.*` trong `messages/vi.json`, `en.json`, lỗi qua `apiError.*`. Ô tìm kiếm có label; thay bảng sau lọc báo số kết quả bằng `aria-live="polite"`. Bảng có header cho trạng thái và tỉ lệ, dấu `-` có lời giải thích cho công nghệ hỗ trợ. Link học viên/kết quả có tên đọc mô tả. Sau phân trang, focus tới tiêu đề bảng và cuộn bảng về đầu. Tab/lọc dùng phím theo shared UI. `[SoT: BD Sheet 5, 8]`

## 7. Điểm kiểm thử

Bảng 7.1 bao phủ EVT-1–9.

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| SC-01 | Normal | EVT-1, 2, 3, 4, 5: URL/Back/Forward giữ đúng bốn lọc; bảng dùng F4, trang 1 khi đổi lọc | Vitest | F4 AC-41, AC-43; `instructor_submissions` AC-SC-01 |
| SC-02 | Security | EVT-1 lớp ngoài phạm vi cho không tìm thấy, không lộ dữ liệu | Vitest | F4 AC-44 |
| SC-03 | Boundary | EVT-1 ba trạng thái rỗng khác nhau; tỉ lệ chưa chấm là `-` | Vitest | `instructor_submissions` AC-SC-02 |
| SC-04 | Abnormal | EVT-8 retry giữ URL và chỉ toast sau lỗi lặp | Vitest | `instructor_submissions` AC-SC-02 |
| SC-05 | Normal | EVT-6/7 mở đúng route với ID dòng, trang kết quả tự kiểm quyền | Playwright | F4 AC-45; `instructor_submissions` AC-SC-03 |
| SC-06 | Normal | EVT-9 bỏ riêng `classId`, giữ `q/result`, reset `page` | Vitest | `instructor_submissions` AC-SC-01 |

## 8. Nợ prototype và câu hỏi mở

**Nợ prototype:** view đang dùng mock F1/F4, cần nối query key theo URL, phân biệt ba rỗng và hai loại lỗi, kiểm `result` lạ về `all`, bảo đảm metadata không chứa nguồn mã. Giữ dòng nợ `INS0206` trong `06-plan/PROTOTYPE_DEBT.md` cho đến khi code được nối thật. `[SoT: BD Sheet 4.4, 9 NO 6]`

**Câu hỏi mở:** không có lựa chọn chặn DD cho màn này; quy tắc một dòng/lượt, cửa sổ thuộc lớp và phạm vi bài giao đã chốt ở F4 #10. Khi tích hợp cần đối chiếu đúng tên mã lỗi F4 cho lớp ngoài phạm vi và khoá i18n thực tế. `[SoT: F4 #10; BD Sheet 9]`
