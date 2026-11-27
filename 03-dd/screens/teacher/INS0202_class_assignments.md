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
| Phiên bản, ngày | V0.2, 2026-11-23 |

Bảng 0.2 là lịch sử.

| Ver | Nội dung | Ngày |
| :--- | :--- | :--- |
| V0.1 | Thiết kế popup giao bài và gỡ bài, lọc/phân trang theo hợp đồng F2 | 2026-10-24 |
| V0.2 | Áp `DEC-2026-1123-assign-problems-dedicated-screen`: bỏ popup giao bài, nút "+ Gán từ ngân hàng bài toán" là liên kết sang màn con `INS0207`; phần gỡ bài giữ nguyên. Chưa duyệt hình | 2026-11-23 |

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
| views | `views/teacher/class-assign-problems/ui/class-assign-problems-view.tsx` | Màn con `INS0207` (giao bài cho lớp), thay popup cũ; DD của màn con viết sau khi duyệt hình | Có |
| views | `views/teacher/class-assignments/model/` | Query key, selection và invalidation theo lớp | Đích DD |
| entities | `entities/class/api/queries.ts` | Danh sách lớp F1 #19 | Mock |
| entities | `entities/problem/api/queries.ts` | F2 #49–53, metrics/bài giao/chọn/gỡ | Mock/chưa đủ |
| shared | DataTable, Select, Dialog, Pagination, ErrorState, toast | Tái dùng, không thêm shared | Có |

Từ V0.2 việc giao bài **không còn là popup** của view này mà là màn con `INS0207` (`/instructor/assignments/new?classId=`); popup còn lại ở view này chỉ là xác nhận gỡ bài. Dữ liệu bài giao vẫn thuộc F2; view không đọc bảng lớp hoặc bài trực tiếp. Import `app → views → entities → shared`. `[SoT: BD Sheet 4.5; DEC-2026-1123-assign-problems-dedicated-screen]`

## 2. API tiêu thụ

Bảng 2.1 nối mọi sự kiện Sheet 8 với API tiêu thụ.

| EVT | API ID | Hook/mutation đích | Hiện trạng |
| :--- | :--- | :--- | :--- |
| EVT-1 tải | F2 #50/§51 summary, F1 #19 classes, F2 #49/§50 assignments | `useAssignmentSummary`, `useInstructorClasses`, `useClassAssignmentsPage` | Mock |
| EVT-2 mở màn giao bài | Không API ở màn này; liên kết `/instructor/assignments/new?classId=<id>` (tab "Tất cả" thì không kèm `classId`). Hợp đồng `ListAssignableProblems` và `AssignProblemsToClass` xem `03-dd/api/F2_problem-bank.md` mục 52 và 53; hành vi ở màn con xem BD `INS0207` | `next/link` | Có |
| EVT-3 tìm bài | F2 #49/§50, `q` debounce | `useClassAssignmentsPage` | Mock |
| EVT-4 lọc lớp | F2 #49/§50 với `classId` | như trên | Mock |
| EVT-5 chi tiết bài | Không API; `/instructor/problems/{problemId}` | `next/link` | Có, cần đúng route |
| EVT-6 mở gỡ | Không API | dialog state | Có |
| EVT-7 chọn lớp gỡ | Không API | dialog state | Có |
| EVT-8 xác nhận gỡ | F2 #53/§54 `RemoveClassAssignment` | `removeClassAssignment` | Mock |
| EVT-9 huỷ gỡ | Không API | dialog state | Có |
| EVT-10 đổi trang | F2 #49/§50 `page`, `pageSize=20` | `useClassAssignmentsPage` | Mock |

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
| Màn con `INS0207` | Trạng thái và lỗi của việc giao bài | Thuộc BD `INS0207` Sheet 8, 9; không mô tả lại ở đây | `classAssignProblems.*` |
| Màn con `INS0207` | Kết quả một phần #52 | Báo bằng toast rồi về `/instructor/assignments?classId=<id>`; bảng của màn này tải lại khi vào | `classAssignProblems.*`, `apiError.*` |
| Popup gỡ | `PRB-025` hoặc lỗi khác | Giữ bảng, đóng/giữ dialog theo BD, toast theo mã | `apiError.*` |

## 4. Quy tắc đặc thù màn

**BR-SC-01 — Lọc bảng và lỗi độc lập.** Kích hoạt EVT-1, 3, 4, 10. Tải summary/lớp/bảng song song. Từ khoá debounce, trim, giới hạn 100 ký tự; tab lớp và từ khoá cộng dồn; đổi tab hoặc từ khoá reset trang 1, bảng đang xem giữ tới khi response mới đến. Lỗi chỉ ở khối liên quan; tab một lớp ẩn cột “Gán cho lớp”, tab Tất cả hiện lại. Hậu điều kiện: số liệu không bị lấy từ tập bài đã lọc trên client. `[SoT: BD EVT-1, 3, 4, 10; F2 §50]`

**BR-SC-02 — Giao bài chuyển sang màn con `INS0207`.** Kích hoạt EVT-2. Từ V0.2 màn này chỉ giữ nút liên kết "+ Gán từ ngân hàng bài toán" (có `CLASS_MANAGEMENT:UPDATE` mới hiện) mang theo lớp của tab đang chọn qua `?classId=`; quy tắc chọn bài, lọc, bài `assigned`, giao một lần và xử lý `succeededIds`/`skippedIds`/`failedItems` thuộc màn con (BD `INS0207`, hợp đồng ở `03-dd/api/F2_problem-bank.md` mục 52 và 53). Nội dung BR-SC-02 bản V0.1 (popup) đã bị thay. `[SoT: DEC-2026-1123-assign-problems-dedicated-screen]`

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

Tiền tố `classAssignments.*` ở hai locale, lỗi qua `apiError.*`. Dialog gỡ nói rõ bài và lớp, không dùng thông báo chung chung. Bảng có header, nút "Chi tiết" và "Gỡ khỏi lớp" có tên đọc kèm tên bài. Phần truy cập của việc giao bài thuộc màn con `INS0207`.

## 7. Điểm kiểm thử

Bảng 7.1 bao phủ EVT-1–10.

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| SC-01 | Normal | EVT-1 ba khối tải độc lập, bài rút còn trong bảng, số liệu null là `-` | Vitest | F2 AC-72–73 |
| SC-02 | Boundary | ~~EVT-2 popup đổi lớp xoá selection~~ Đã chuyển sang màn con `INS0207` (V0.2); ở đây chỉ kiểm nút liên kết mang đúng `classId` của tab | Vitest | F2 AC-74–75 (phần liên kết) |
| SC-03 | Normal | EVT-3/4/10 lọc cộng dồn, reset trang, giữ dữ liệu cũ lúc tải | Vitest | F2 AC-72; `class_assignments` AC-SC-01 |
| SC-04 | Normal | EVT-5 mở đúng trang chi tiết bài giảng viên | Playwright | F2 AC-69 |
| SC-05 | Boundary | EVT-6/7 tab Tất cả chọn lớp cụ thể trước khi gỡ | Vitest | F2 AC-76; `class_assignments` AC-SC-02 |
| SC-06 | Abnormal | EVT-8 gỡ lỗi rollback; thành công vẫn giữ bài nếu lớp khác giao | Vitest | F2 AC-76; `class_assignments` AC-SC-02 |
| SC-07 | Normal | EVT-9 huỷ gỡ không gọi API | Vitest | `class_assignments` AC-SC-02 |

## 8. Nợ prototype và câu hỏi mở

**Nợ prototype:** nối ba query tải ban đầu, gỡ với §54 và xử lý kết quả một phần; route chi tiết bài phải là `/instructor/problems/{problemId}`. Phần giao bài đã tách sang màn con `INS0207`, DD của màn con viết sau khi chủ dự án duyệt hình (CLAUDE.md: DD màn hình viết sau prototype). Dòng `INS0202` trong `06-plan/PROTOTYPE_DEBT.md` giữ mở đến khi code khớp DD. `[SoT: BD Sheet 4.4; V]`

**Câu hỏi mở:** không có lựa chọn sản phẩm chặn DD. Mã `problemStatus` của BD và `withdrawn` boolean của F2 đã được DD module chọn; khi đồng bộ BD nên dùng nhãn màn “Đã rút xuống” mà không đưa tên enum backend ra UI. `[SoT: F2 §50.3, mục 61 Q-11]`

## Nối API thật (F2-6 phần 2, 2026-11-06)

Bảng dưới liệt kê endpoint màn tiêu thụ và hiện trạng; hợp đồng chỉ định nghĩa ở `03-dd/api/F2_problem-bank.md`.

| Endpoint | Hiện trạng |
| :--- | :--- |
| `#49` | `useClassAssignmentsPage` / `realListAssignments` |
| `#50` | `useAssignmentSummary`; `avgAcRatePercent` chưa có chỗ hiển thị |
| `#51` | `useAssignableProblems` nay do màn con `INS0207` gọi (bộ lọc `levelCode`, `topic`, trường `assigned`, backend đã làm 2026-11-23, `ClassAssignmentFlowTest` đạt) |
| `#52` | `useAssignProblems` trả `AssignResult` |
| `#53` | `useRemoveAssignment`, làm mới ở `onSettled` |
| `#54` | Chưa nối (thuộc INS0101) |

Test đường thật: `real-assignments.test.ts` (13 ca), `views/teacher/class-assignments/ui/class-assignments-real.test.tsx` (8 ca).

## Ghi chú đồng bộ prototype 2026-11-07: AC trung bình

`#50` (`realAssignmentSummary`) trả `avgAcRatePercent`; `AssignmentSummary.avgAcRate` hiển thị ở dòng mô tả đầu màn, sau số bài đã gán, khoá `classAssignments.header.avgAcRate` ("AC trung bình {value}"). `null` hoặc mock không có giá trị: "-". 0 thật: "0%". Test: `class-assignments-real.test.tsx` (hai ca: 62%, 0%, và dấu gạch khi `null`).

Còn mở: chỗ đặt (dòng tóm tắt thay cho thẻ riêng của BD Sheet 5 B NO 4) `[SoT: Suy luận]`, chờ owner xem hình; hook `useClassAssignments()` không phân trang đã xoá 2026-11-14 vì không còn nơi gọi (INS0101 và INS0204 không dùng).
