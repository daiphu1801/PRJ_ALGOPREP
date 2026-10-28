# DD màn hình — Tiến độ lớp (`INS0203`)

## 0. Thông tin tài liệu

Bảng 0.1 là bìa.

| Mục | Giá trị |
| :--- | :--- |
| Mã, actor, route | `INS0203`, INSTRUCTOR, `/instructor/students` |
| Nguồn | `01-rd/screens/teacher/INS0203_class_progress.md`; `02-bd/screens/teacher/INS0203_class_progress.md` |
| Prototype | `05-coding/frontend/src/views/teacher/class-progress/` |
| Module | F1 identity #19, #57–59; F2/F4 qua cổng nội bộ F1 |
| TDD | `04-tdd/modules/F1_identity.md` AC-52–53; `04-tdd/modules/F4_judge-orchestration.md` AC-47; `04-tdd/screens/teacher/INS0203_class_progress.md` |
| Phiên bản, ngày | V0.1, 2026-10-24 |

Bảng 0.2 ghi lịch sử.

| Ver | Nội dung | Ngày |
| :--- | :--- | :--- |
| V0.1 | DD biểu đồ sáu tuần và khối cần chú ý theo ba nguồn độc lập | 2026-10-24 |

Bảng 0.3 quy ước nguồn.

| Ký hiệu | Nguồn |
| :--- | :--- |
| BD | `02-bd/screens/teacher/INS0203_class_progress.md` Sheet 5, 8, 9 |
| F1 | `03-dd/api/F1_identity.md` #19, #57–59 |
| F4 | `03-dd/api/F4_judge-orchestration.md` P1 (cổng F1 gọi, không phải browser) |
| V | `05-coding/frontend/src/views/teacher/class-progress/ui/class-progress-view.tsx` |

## 1. Bản đồ slice FSD

Bảng 1.1 là slice từ `05-coding/frontend/src/`.

| Lớp | Đường dẫn | Vai trò | Hiện trạng |
| :--- | :--- | :--- | :--- |
| app | `app/(instructor)/instructor/students/page.tsx` | Route view | Có |
| views | `views/teacher/class-progress/ui/class-progress-view.tsx` | Tab lớp, biểu đồ một đường, cần chú ý, bảng học viên | Có, mock |
| views | `views/teacher/class-progress/model/` | Lọc theo tab, debounce bảng, query key và retry từng vùng | Đích DD |
| widgets | Khối biểu đồ và cần chú ý trong `views` hiện tại; nâng lên `widgets/` nếu tái dùng ở màn khác | Chỉ màn này dùng, không nâng sớm | Có thể giữ trong view |
| entities | `entities/class/api/queries.ts` | Lớp F1 #19, trend #57, attention #58, danh sách học viên | Mock/chưa đủ |
| shared | Chart, DataTable, Pagination, Tabs, ErrorState, Skeleton | Tái dùng | Có |

View không tính nhãn vắng bài/cần hỗ trợ từ điểm mock; F1 #58 trả mã lý do và giá trị thật. Chiều import `app → views → entities → shared`. `[SoT: BD Sheet 4.5, 5 Khu C]`

## 2. API tiêu thụ

Bảng 2.1 nối EVT Sheet 8. Tên `ListClassStudents` trong BD được hiện thực bằng F1 #59 `ListInstructorStudents`; endpoint này phân trang trên toàn bộ lớp phụ trách, không ghép các trang #26 ở browser.

| EVT | API ID | Hook/query đích | Hiện trạng |
| :--- | :--- | :--- | :--- |
| EVT-1 tải | F1 #19 `ListInstructorClasses`, #57 `GetClassScoreTrend`, #58 `ListClassAttentionStudents`, #59 `ListInstructorStudents` | Bốn query, sau khi biết lớp | Mock |
| EVT-2 tab lớp | F1 #57/#58/#59 với cùng `classId`; trang bảng về 1 | query key theo tab | Mock |
| EVT-3 tìm học viên | Chỉ F1 #59 với `q` debounce | query bảng | Mock |
| EVT-4 đổi trang | Chỉ F1 #59 với `page` | query bảng | Mock |
| EVT-5 thử lại khối | Gọi lại đúng endpoint F1 của khối lỗi | `refetch` theo khối | Chưa tách |
| EVT-6 mở học viên từ bảng | Không API; route chi tiết theo cặp `classId/studentId` của dòng | `next/link` | Có |
| EVT-7 mở học viên từ chú ý | Không API; cùng route EVT-6 | `next/link` | Có |

F1 #57 trả một đường đúng sáu tuần ISO đầy đủ, tối đa cũ 15 phút; F1 #58 trả tối đa năm học viên trong phạm vi tab. Ô tìm chỉ tác động bảng, không lọc hai nguồn này. `[SoT: BD Sheet 8 EVT-2/3; DEC-2026-1014-instructor-batch2-weekly-rules]`

## 3. Trạng thái màn

Bảng 3.1 là trạng thái theo vùng, frontend dựa trên `error.code`.

| Vùng | Trạng thái/mã | Xử lý | i18n |
| :--- | :--- | :--- | :--- |
| Toàn màn | Không có lớp | Một empty state dẫn Quản lý lớp, ẩn ba khối con | `classProgress.*` |
| Biểu đồ | #57 đang tải/lỗi | Skeleton/Retry riêng, không mất bảng | `classProgress.*`, `apiError.*` |
| Biểu đồ | Điểm tuần `null` | Khoảng trống, không vẽ số 0 giả | `classProgress.*` |
| Cần chú ý | #58 rỗng/lỗi | Câu chưa có học viên cần chú ý hoặc Retry riêng | `classProgress.*` |
| Bảng | Đang tải/rỗng/lỗi | Khung chờ/câu lọc rỗng/Retry riêng; biểu đồ không reload | `classProgress.*` |
| Các khối | `IDT-414` khi tab lớp ngoài quyền | Trạng thái không tìm thấy lớp, không hiển thị dữ liệu tab cũ | `apiError.IDT-414` |

Không dùng `error.message`; lỗi tải lần đầu chỉ vùng lỗi, retry lỗi lặp mới toast theo quy tắc chung. `[SoT: BD Sheet 8, 9]`

## 4. Quy tắc đặc thù màn

**BR-SC-01 — Tab lớp đồng bộ ba khối.** Kích hoạt EVT-1/2 khi có lớp. Tab “Tất cả” bỏ `classId`, tab lớp truyền cùng UUID tới #57/#58/bảng; đổi tab reset trang 1 nhưng giữ `q`. Huỷ/loại kết quả tab cũ. Chỉ một đường biểu đồ cho phạm vi hiện tại; bảng ẩn cột Lớp khi chọn một lớp. Hậu điều kiện: biểu đồ, khối chú ý, bảng không trộn phạm vi. `[SoT: BD EVT-1/2]`

**BR-SC-02 — Tìm kiếm và phân trang chỉ ở bảng.** Kích hoạt EVT-3/4, chờ ngưng gõ, trim/cắt 100 ký tự, query lại bảng; đổi trang không gọi #57/#58. Nếu trang lỗi giữ trang đang xem và hiển thị retry đúng query. Hậu điều kiện: biểu đồ và khối chú ý không đổi vì ô tìm. `[SoT: BD EVT-3/4; DEC-2026-1014-instructor-batch2-weekly-rules]`

**BR-SC-03 — Hai đường vào cùng học viên.** Kích hoạt EVT-6/7. Mỗi dòng bảng hoặc chú ý có `classId` thật và `studentId`; link dùng đúng cặp đó tới `INS0204`. Trang chỉ đọc nên không hỏi xác nhận. Hậu điều kiện: hai đường không chọn nhầm lớp khi ở tab Tất cả. `[SoT: BD EVT-6/7]`

## 5. Kiểm tra phía client

Bảng 5.1 là kiểm tham số; F1 kiểm lại quyền.

| Trường | Zod/client | Nguồn | Khớp |
| :--- | :--- | :--- | :--- |
| `classId` tab | `z.string().uuid()` nếu có, phải thuộc danh sách #19 | F1 #57/#58; BD Sheet 9 NO 2 | Có |
| `q` | trim/cắt 100 ký tự, debounce | BD Sheet 5 Khu D NO 1 | Có |
| `page` | integer ≥1, reset 1 khi đổi tab/từ khoá | BD EVT-2–4 | Có |
| `classId/studentId` link | Hai UUID từ cùng dòng | BD EVT-6/7 | Có |

## 6. i18n và truy cập

Tiền tố `classProgress.*` trong hai locale. Biểu đồ có bảng/chuỗi văn bản sáu điểm làm phương án đọc thay thế; `null` báo “không có dữ liệu”, không vẽ về 0. Tab lớp dùng ARIA Tabs đúng phím mũi tên. Trạng thái “Cần chú ý” dịch bằng `reasonCode`/`reasonValue`, không đưa mã enum vào nhãn. Link học viên nêu tên và lớp để phân biệt khi một người thuộc nhiều lớp. Retry từng khối giữ focus trong khối. `[SoT: BD Sheet 5, 9]`

## 7. Điểm kiểm thử

Bảng 7.1 bao phủ EVT-1–7.

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| SC-01 | Normal | EVT-1 bốn nguồn; sáu tuần đủ, một đường, không có lớp thì empty state | Vitest | F1 AC-52–53; F4 AC-47 |
| SC-02 | Normal | EVT-2 tab lớp đổi cùng phạm vi ba khối, reset trang | Vitest | F1 AC-52–53; `class_progress` AC-SC-01 |
| SC-03 | Boundary | EVT-3 tìm chỉ tải bảng; EVT-4 đổi trang không tải trend/attention | Vitest | `class_progress` AC-SC-02 |
| SC-04 | Abnormal | EVT-5 retry khối lỗi không reset khối khác | Vitest | `class_progress` AC-SC-02 |
| SC-05 | Normal | EVT-6/7 hai link tới đúng cặp học viên/lớp | Playwright | `class_progress` AC-SC-02 |
| SC-06 | Security | Tab `classId` ngoài quyền cho `IDT-414`, không hiển thị dữ liệu cũ | Vitest | F1 AC-52–53 |

## 8. Nợ prototype và câu hỏi mở

**Nợ prototype:** biểu đồ hiện vẽ hai đường mock, khối chú ý là hằng và không lọc theo lớp, bảng chưa phân trang theo backend; cần một đường #57, danh sách #58, query bảng có phân trang và retry riêng. Dòng `INS0203` trong `06-plan/PROTOTYPE_DEBT.md` còn mở tới khi code nối thật. `[SoT: BD Sheet 4.4]`

**Đã đóng:** tab Tất cả và từng lớp dùng F1 #59 với `classId` tuỳ chọn, `q`, `page`, `pageSize=20`. #26 chỉ còn dùng ở chi tiết một lớp; quyết định `DEC-2026-1024-dd-teacher-student-list`.
