# DD màn hình — Bài tập giao lớp (`INS0202`)

## 0. Thông tin tài liệu

Bảng 0.1 là bìa.

| Mục | Giá trị |
| :--- | :--- |
| Mã, actor, route | `INS0202`, INSTRUCTOR, `/instructor/assignments` |
| Nguồn | `01-rd/screens/teacher/INS0202_class_assignments.md`; `02-bd/screens/teacher/INS0202_class_assignments.md` |
| Prototype | `05-coding/frontend/src/views/teacher/class-assignments/` |
| Module | F1 identity #19; F2 problem-bank #49–53, mục 50–54 |
| TDD | `04-tdd/modules/F2_problem-bank.md` AC-72–76; `04-tdd/screens/teacher/INS0202_class_assignments.md` |
| Phiên bản, ngày | V0.1, 2026-10-24 |

Bảng 0.2 là lịch sử.

| Ver | Nội dung | Ngày |
| :--- | :--- | :--- |
| V0.1 | Thiết kế popup giao bài và gỡ bài, lọc/phân trang theo hợp đồng F2 | 2026-10-24 |

Bảng 0.3 quy ước nguồn.

| Ký hiệu | Nguồn |
| :--- | :--- |
| BD | `02-bd/screens/teacher/INS0202_class_assignments.md` Sheet 5, 8, 9 |
| F2 | `03-dd/api/F2_problem-bank.md` mục 50–54; `03-dd/validation/F2_problem-bank.md` mục 12 |
| V | `05-coding/frontend/src/views/teacher/class-assignments/ui/` |

## 1. Bản đồ slice FSD

Bảng 1.1 là thành phần tính từ `05-coding/frontend/src/`.

| Lớp | Đường dẫn | Vai trò | Hiện trạng |
| :--- | :--- | :--- | :--- |
| app | `app/(instructor)/instructor/assignments/page.tsx` | Route và view | Có |
| views | `views/teacher/class-assignments/ui/class-assignments-view.tsx` | Số liệu, bộ lọc, bảng, dialog gỡ | Có, mock |
| views | `views/teacher/class-assignments/ui/assign-problem-dialog.tsx` | Chọn lớp và bài từ ngân hàng | Có, mock |
| views | `views/teacher/class-assignments/model/` | Query key, selection và invalidation theo lớp | Đích DD |
| entities | `entities/class/api/queries.ts` | Danh sách lớp F1 #19 | Mock |
| entities | `entities/problem/api/queries.ts` | F2 #49–53, metrics/bài giao/chọn/gỡ | Mock/chưa đủ |
| shared | DataTable, Select, Dialog, Pagination, ErrorState, toast | Tái dùng, không thêm shared | Có |

Popup là thành phần của view này, không tạo màn riêng. Dữ liệu bài giao vẫn thuộc F2; view không đọc bảng lớp hoặc bài trực tiếp. Import `app → views → entities → shared`. `[SoT: BD Sheet 4.5; DEC-2026-1011-class-assignment-popup-on-assignments-screen]`

## 2. API tiêu thụ

Bảng 2.1 nối mọi sự kiện Sheet 8 và luồng popup.

| EVT | API ID | Hook/mutation đích | Hiện trạng |
| :--- | :--- | :--- | :--- |
| EVT-1 tải | F2 #50/§51 summary, F1 #19 classes, F2 #49/§50 assignments | `useAssignmentSummary`, `useInstructorClasses`, `useClassAssignments` | Mock |
| EVT-2 mở popup | F2 #51/§52 `ListAssignableProblems` sau khi chọn lớp; F2 #52/§53 khi bấm “Giao n bài” trong popup | `useAssignableProblems`, `assignProblemsToClass` | Mock |
| EVT-3 tìm bài | F2 #49/§50, `q` debounce | `useClassAssignments` | Mock |
| EVT-4 lọc lớp | F2 #49/§50 với `classId` | như trên | Mock |
| EVT-5 chi tiết bài | Không API; `/instructor/problems/{problemId}` | `next/link` | Có, cần đúng route |
| EVT-6 mở gỡ | Không API | dialog state | Có |
| EVT-7 chọn lớp gỡ | Không API | dialog state | Có |
| EVT-8 xác nhận gỡ | F2 #53/§54 `RemoveClassAssignment` | `removeClassAssignment` | Mock |
| EVT-9 huỷ gỡ | Không API | dialog state | Có |
| EVT-10 đổi trang | F2 #49/§50 `page`, `pageSize=20` | `useClassAssignments` | Mock |

Không có realtime. F2 #49 trả một dòng mỗi bài và `assignedClasses`; màn không tự cộng `submissionCount` toàn hệ thống. Bài đã rút vẫn hiện nhãn, có thể gỡ nhưng không mở như bài công khai của người học. `[SoT: F2 §50; BD Sheet 9]`

## 3. Trạng thái màn

Bảng 3.1 ánh xạ lỗi theo `error.code`, không dùng `message` từ máy chủ.

| Vùng | Trạng thái/mã | Xử lý | i18n |
| :--- | :--- | :--- | :--- |
| Summary, lớp, bảng | Tải/lỗi độc lập | Skeleton hoặc lỗi + retry đúng khối; khối khác giữ dữ liệu | `classAssignments.*`, `apiError.*` |
| Bảng | Rỗng toàn bộ | Lời mời giao bài đầu tiên | `classAssignments.*` |
| Bảng | Rỗng do lọc | Nút xoá lọc; không báo chưa giao bài | `classAssignments.*` |
| Bảng | `PRB-005` | Lớp không phụ trách, không lộ dữ liệu | `apiError.PRB-005` |
| Bảng | metrics null | Hiện dấu `-`, không tự suy `0%` | `classAssignments.*` |
| Popup giao | Không chọn lớp/bài | Khoá nút Giao; chọn lớp mới xoá selection cũ | `classAssignments.*` |
| Popup giao | Kết quả một phần #52 | Hiển thị số thành công/bỏ qua/thất bại, refetch bảng và nguồn chọn | `classAssignments.*`, `apiError.*` |
| Popup gỡ | `PRB-025` hoặc lỗi khác | Giữ bảng, đóng/giữ dialog theo BD, toast theo mã | `apiError.*` |

## 4. Quy tắc đặc thù màn

**BR-SC-01 — Lọc bảng và lỗi độc lập.** Kích hoạt EVT-1, 3, 4, 10. Tải summary/lớp/bảng song song. Từ khoá debounce, trim, giới hạn 100 ký tự; tab lớp và từ khoá cộng dồn; đổi tab hoặc từ khoá reset trang 1, bảng đang xem giữ tới khi response mới đến. Lỗi chỉ ở khối liên quan; tab một lớp ẩn cột “Gán cho lớp”, tab Tất cả hiện lại. Hậu điều kiện: số liệu không bị lấy từ tập bài đã lọc trên client. `[SoT: BD EVT-1, 3, 4, 10; F2 §50]`

**BR-SC-02 — Popup giao bài theo lớp.** Kích hoạt EVT-2; phải chọn lớp do F1 #19 trả. Khi lớp đổi, huỷ selection bài và query F2 §52 theo lớp mới. Chỉ chọn bài `PUBLISHED` chưa giao trả từ server, tối đa 50 ID; bấm Giao gọi §53 một lần, xử lý `succeededIds`, `skippedIds`, `failedItems` theo kết quả từng bài. `failedItems[].code=PRB-056` nghĩa là bài đó chưa được giao do ghi Outbox lỗi; giữ bài lỗi trong popup để thử lại, không hiển thị như thành công dù HTTP của lô là 200. Refetch summary/bảng/danh sách có thể giao, giữ popup mở nếu còn bài thất bại để thử lại. Hậu điều kiện: không coi kết quả một phần là thành công toàn bộ. `[SoT: F2 §52–53; BD EVT-2; chủ dự án chốt 2026-10-27]`

**BR-SC-03 — Gỡ đúng cặp lớp-bài.** Kích hoạt EVT-6–9. Tab một lớp chọn sẵn lớp; tab Tất cả và bài giao nhiều lớp buộc chọn một lớp trong `assignedClasses`. Chỉ khi có cặp `classId, problemId` mới bật xác nhận; gọi F2 §54, không xóa bài khỏi ngân hàng. Thành công refetch bảng/summary; bài có thể còn dòng nếu vẫn giao cho lớp khác. Lịch sử nộp giữ nguyên. `[SoT: BD EVT-6–9; F2 §54]`

## 5. Kiểm tra phía client

Bảng 5.1 đối chiếu zod và module validation.

| Trường | Zod/client | Nguồn | Khớp |
| :--- | :--- | :--- | :--- |
| `classId`, `problemId` | `z.string().uuid()` và phải thuộc option server trả | F2 §52–54; BD Sheet 9 | Có |
| `problemIds` | `z.array(z.string().uuid()).min(1).max(50)` | F2 §53; `03-dd/validation/F2_problem-bank.md` mục 12 | Có |
| `q` | trim/cắt 100 ký tự, debounce | F2 §50, §52 | Có |
| `page` | integer ≥1; `pageSize=20` | F2 §50; BD EVT-10 | Có |

## 6. i18n và truy cập

Tiền tố `classAssignments.*` ở hai locale, lỗi qua `apiError.*`. Popup chọn bài có label tìm kiếm, checkbox tên bài và bộ đếm “Giao n bài”; đổi lớp đưa focus về danh sách bài mới. Dialog gỡ nói rõ bài và lớp, không dùng thông báo chung chung. Thông báo kết quả một phần đọc qua `aria-live`, không chỉ màu sắc. Bảng có header, nút “Chi tiết” và “Gỡ khỏi lớp” có tên đọc kèm tên bài. `[SoT: BD Sheet 5, 8]`

## 7. Điểm kiểm thử

Bảng 7.1 bao phủ EVT-1–10.

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| SC-01 | Normal | EVT-1 ba khối tải độc lập, bài rút còn trong bảng, số liệu null là `-` | Vitest | F2 AC-72–73 |
| SC-02 | Boundary | EVT-2 popup đổi lớp xoá selection; 1–50 bài, kết quả một phần được báo đúng | Vitest | F2 AC-74–75 |
| SC-03 | Normal | EVT-3/4/10 lọc cộng dồn, reset trang, giữ dữ liệu cũ lúc tải | Vitest | F2 AC-72; `class_assignments` AC-SC-01 |
| SC-04 | Normal | EVT-5 mở đúng trang chi tiết bài giảng viên | Playwright | F2 AC-69 |
| SC-05 | Boundary | EVT-6/7 tab Tất cả chọn lớp cụ thể trước khi gỡ | Vitest | F2 AC-76; `class_assignments` AC-SC-02 |
| SC-06 | Abnormal | EVT-8 gỡ lỗi rollback; thành công vẫn giữ bài nếu lớp khác giao | Vitest | F2 AC-76; `class_assignments` AC-SC-02 |
| SC-07 | Normal | EVT-9 huỷ gỡ không gọi API | Vitest | `class_assignments` AC-SC-02 |

## 8. Nợ prototype và câu hỏi mở

**Nợ prototype:** cần nối ba query tải ban đầu, popup với F2 §52–53, gỡ với §54 và xử lý kết quả một phần; route chi tiết bài phải là `/instructor/problems/{problemId}`. Dòng `INS0202` trong `06-plan/PROTOTYPE_DEBT.md` giữ mở đến khi code khớp DD. `[SoT: BD Sheet 4.4; V]`

**Câu hỏi mở:** không có lựa chọn sản phẩm chặn DD. Mã `problemStatus` của BD và `withdrawn` boolean của F2 đã được DD module chọn; khi đồng bộ BD nên dùng nhãn màn “Đã rút xuống” mà không đưa tên enum backend ra UI. `[SoT: F2 §50.3, mục 61 Q-11]`
