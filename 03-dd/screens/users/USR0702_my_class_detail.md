# DD màn hình — Chi tiết lớp của tôi (`USR0702`)

## 0. Thông tin tài liệu

Bảng 0.1 là bìa tài liệu.

| Mục | Giá trị |
| :--- | :--- |
| Mã, tên, actor | `USR0702`, Chi tiết lớp của tôi, STUDENT |
| Route | `/classes/{classId}`; `classId` UUID |
| RD, BD | `01-rd/screens/users/USR0702_my_class_detail.md`; `02-bd/screens/users/USR0702_my_class_detail.md` |
| Prototype | `05-coding/frontend/src/views/users/my-class-detail/` |
| Module | F1 identity #25B; F2 problem-bank #13A/§5A |
| TDD | `04-tdd/modules/F1_identity.md` AC-49; `04-tdd/modules/F2_problem-bank.md` AC-88; `04-tdd/screens/users/USR0702_my_class_detail.md` |
| Phiên bản, ngày | V0.1, 2026-10-24 |

Bảng 0.2 ghi lịch sử sửa.

| Ver | Nội dung | Ngày |
| :--- | :--- | :--- |
| V0.1 | Tạo DD theo F1/F2 tách nguồn và bài đã rút | 2026-10-24 |

Bảng 0.3 quy ước nguồn trích dẫn.

| Ký hiệu | Nguồn |
| :--- | :--- |
| BD | `02-bd/screens/users/USR0702_my_class_detail.md` (Sheet 5, 8, 9) |
| F1, F2 | `03-dd/api/F1_identity.md` mục 0.4.1; `03-dd/api/F2_problem-bank.md` mục 5A |
| V | `05-coding/frontend/src/views/users/my-class-detail/ui/my-class-detail-view.tsx` |
| DEC | `DEC-2026-1024-dd-student-class-contracts` |

## 1. Bản đồ slice FSD

Bảng 1.1 chỉ các slice dùng cho màn; đường dẫn tính từ `05-coding/frontend/src/`.

| Lớp | Đường dẫn | Vai trò | Trạng thái |
| :--- | :--- | :--- | :--- |
| app | `app/(student)/classes/[classId]/page.tsx` | Đọc UUID route, render view | Đã có |
| views | `views/users/my-class-detail/ui/my-class-detail-view.tsx` | Thông tin lớp, tiến độ và bảng bài giao | Đã có, mock |
| views | `views/users/my-class-detail/model/` | Kết hợp hai query, tính x/y, trạng thái từng vùng | Đích DD, chưa có |
| entities | `entities/class/api/queries.ts` | `useMyClass` gọi F1 #25B theo ID | Mock hiện tìm trong danh sách lớp |
| entities | `entities/class/model/types.ts` | Kiểu thông tin lớp; bỏ `assignments` gộp | Mock chưa khớp |
| entities | query F2 `ListMyClassProblems` | Chỉ bài của `classId` đang xem | Chưa có hook thật |
| shared | Card, ProgressBar, ErrorState, Badge, định dạng lịch/ngày, `next/link` | Tái dùng; không nâng cấp shared | Có |

View không dùng `useMyClasses` để suy ra một lớp: F1 #25B là nguồn riêng, có kiểm quyền ở server. Props bảng gồm `items` từ F2 và trạng thái tải/lỗi riêng; `withdrawn` tắt link. Chiều import `app → views → entities → shared`. `[SoT: BD Sheet 4.5; V:32-50,62-75]`

## 2. API tiêu thụ

Bảng 2.1 ánh xạ mọi sự kiện Sheet 8.

| EVT | API ID | Hook/query đích | Hiện trạng |
| :--- | :--- | :--- | :--- |
| EVT-1 mở màn | F1 #25B `GetMyClass` và F2 #13A/§5A `ListMyClassProblems?classId=...` | `useMyClass(classId)`, `useMyClassProblems(classId)` | `useMyClass` lọc mảng mock gộp |
| EVT-2 quay lại | Không gọi API, link `/classes` | `next/link` | Có |
| EVT-3 mở bài | Không gọi API, chỉ dòng `withdrawn = false` link `/problems/{problemId}` | `next/link` | Mock chưa chặn withdrawn |
| EVT-4 thử lại | Refetch chỉ nguồn bị lỗi: F1 #25B hoặc F2 #13A | `refetch` theo vùng | Chưa có vùng lỗi tách biệt |

Không có realtime; quay lại từ bài toán hoặc mở lại trang sẽ tải dữ liệu mới để phản ánh trạng thái giải. Khi F1 cho `404 IDT-411`, không cần chờ F2 để quyết định trạng thái không tìm thấy. F2 có `404 PRB-015` khi quyền lớp thay đổi giữa hai lời gọi, cũng đưa màn về cùng trạng thái không tìm thấy. `[SoT: BD Sheet 8 EVT-1, EVT-3]`

## 3. Trạng thái màn

Bảng 3.1 là trạng thái và ánh xạ `error.code`; không dùng `error.message` làm câu UI.

| Vùng | Trạng thái/mã | Xử lý | Khoá i18n |
| :--- | :--- | :--- | :--- |
| Trang | UUID sai, `IDT-411`, `PRB-015` | Cùng trang không tìm thấy, link quay lại; không toast | `myClasses.detail.notFound` |
| Trang | F1 đang tải | Skeleton toàn trang | `myClasses.loading` |
| Trang | F1 mạng/5xx | Vùng lỗi toàn trang kèm Thử lại; retry thất bại thêm toast | `apiError.*` |
| Bài giao | F2 đang tải | Skeleton vùng tiến độ và bảng, thông tin lớp vẫn hiện | `myClasses.loading` |
| Bài giao | F2 mạng/5xx | Lỗi chỉ ở tiến độ/bảng kèm Thử lại; không báo lớp mất | `apiError.*` |
| Bài giao | Không có dòng | Tiến độ `0/0`, thanh rỗng, câu chưa giao bài | `myClasses.card.noAssignments` |
| Bài giao | `withdrawn=true` | Dòng còn trong bảng và mẫu số, tên mờ, nhãn “Đã rút xuống”, không có link | Khoá nhãn dùng chung với bài đã lưu, cần đối chiếu `messages/*` |
| Bài giao | `SOLVED`, còn lại | Hai nhãn “Đã xong”/“Chưa làm”; thanh tiến độ dựa trên `SOLVED` | `myClasses.card.solved`, `.todo`, `.progress` |

Thiếu phiên do shell chuyển auth. Lỗi 404 ngoài phạm vi và sai UUID có cùng nội dung để tránh dò lớp. `[SoT: BD Sheet 9 NO 1–7]`

## 4. Quy tắc đặc thù màn

**BR-SC-01 — Tải theo vùng và bảo vệ ID.** Kích hoạt EVT-1 với `classId` từ route. Kiểm UUID trước khi gọi server; sai dạng vào không tìm thấy. UUID hợp lệ gọi F1/F2 song song, gắn key theo ID; đổi route phải loại kết quả cũ. F1 lỗi làm mất nội dung trang, F2 lỗi chỉ thay tiến độ/bảng bằng vùng lỗi. Hậu điều kiện: metadata lớp không mất vì F2 suy giảm. `[SoT: BD Sheet 8 EVT-1]`

**BR-SC-02 — Tiến độ và bài rút.** Kích hoạt khi F2 thành công. Sắp theo `assignedAt` gần nhất trước theo hợp đồng §5A; `totalCount` là số dòng, gồm cả `withdrawn`, `solvedCount` là số `SOLVED`. F2/F4 đã tính trạng thái từ lúc giao hiện hành; client không ghép trạng thái giải toàn cục (bài Accepted trước khi giao vẫn chưa hoàn thành lớp). Nếu không có dòng, dùng 0% để tránh chia 0. Bài `withdrawn` render văn bản và nhãn, không gắn thẻ link, không nhận focus như liên kết. Hậu điều kiện: số x/y khớp đúng số dòng bảng. `[SoT: BD Sheet 9 NO 4, NO 7; DEC-2026-1025-dd-class-assignment-time-window]`

**BR-SC-03 — Điều hướng và tải lại.** Kích hoạt EVT-2/3. Quay về `/classes`; chỉ bài đang mở được link `/problems/{problemId}`. Khi về lại trang, query invalidated/refetch để cập nhật trạng thái giải; lớp đã xoá hoặc người học bị gỡ rơi vào không tìm thấy ở lần tải kế. `[SoT: BD Sheet 8 EVT-2, EVT-3; Sheet 9 NO 8]`

## 5. Kiểm tra phía client

Bảng 5.1 đối chiếu dữ liệu route; màn không có form.

| Trường | Zod/client | Nguồn | Khớp |
| :--- | :--- | :--- | :--- |
| `classId` route | `z.string().uuid()`; sai dạng vào trạng thái không tìm thấy, không gọi API | `03-dd/validation/F1_identity.md` mục 9; `03-dd/validation/F2_problem-bank.md` mục 9A | Có |
| `problemId` từ dòng F2 | `z.string().uuid()` trước khi dựng link; `withdrawn=true` không dựng link | F2 §5A; BD Sheet 9 NO 4 | Có |

## 6. i18n và truy cập

Tiền tố `myClasses.detail.*`, `myClasses.card.*` đã có trong `messages/vi.json` và `en.json`; bổ sung khoá “Đã rút xuống” dùng chung và khoá retry/lỗi nếu còn thiếu. Nút quay lại có tên đọc `myClasses.detail.back`. Bảng bài giao dùng tiêu đề cột thật, ngày `assignedAt` định dạng theo locale. Thanh tiến độ có câu x/y, không chỉ màu. Link bài đang mở dùng tên bài; bài rút là văn bản với nhãn, không giả làm link. Focus retry trở về vùng đã tải sau thành công; lỗi trong `aria-live="polite"`, không cướp focus khi F2 suy giảm. `[SoT: BD Sheet 5; V:108-155]`

## 7. Điểm kiểm thử

Bảng 7.1 bao phủ EVT-1 đến EVT-4.

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| SC-01 | Normal | EVT-1 F1/F2 theo cùng UUID; bài rút vẫn ở bảng và mẫu số | Vitest | F1 AC-49; F2 AC-88; `my_class_detail` AC-SC-01 |
| SC-02 | Boundary | EVT-1 không bài: 0/0, thanh rỗng, không chia 0 | Vitest | `my_class_detail` AC-SC-01 |
| SC-03 | Abnormal | EVT-1 UUID sai hoặc một trong hai API 404: cùng không tìm thấy | Vitest | F1 AC-49; F2 AC-88; `my_class_detail` AC-SC-02 |
| SC-04 | Abnormal | EVT-1 F2 503: thông tin lớp vẫn hiện, vùng bài có retry | Vitest | `my_class_detail` AC-SC-02 |
| SC-05 | Normal | EVT-2 quay lại `/classes`; EVT-3 bài còn mở được link overview | Playwright | `my_class_detail` AC-SC-03 |
| SC-06 | Boundary | EVT-3 bài rút không có link/focus, có nhãn và vẫn giữ trạng thái giải | Vitest | F2 AC-88; `my_class_detail` AC-SC-03 |
| SC-07 | Abnormal | EVT-4 retry đúng nguồn lỗi; retry lỗi lần nữa mới toast | Vitest | `my_class_detail` AC-SC-02 |
| SC-08 | Boundary | Accepted trước khi giao vẫn chưa tăng x/y; Accepted sau khi giao tăng đúng một bài | Vitest | F2 §5A; `my_class_detail` AC-SC-01 |

## 8. Nợ prototype và câu hỏi mở

**Nợ prototype:** `useMyClass` đang tìm trong danh sách lớp mock, `MyClass.assignments` gộp dữ liệu F1/F2, bài rút vẫn là link, chưa có lỗi từng vùng. Cần nối hai endpoint, dùng UUID thật và ISO `joinedAt`/`assignedAt`, bổ sung `withdrawn`. Dòng màn lớp trong `06-plan/PROTOTYPE_DEBT.md` chỉ đóng sau khi code được tích hợp. `[SoT: BD Sheet 4.4; V]`

**Câu hỏi mở:** hai lựa chọn trọng yếu về split F1/F2 và bài rút đã chốt tại DEC. Các mặc định BD còn lại: thông tin giảng viên chỉ `display_name`, lớp ngoài phạm vi trả cùng 404, bài giao mới trước; theo các nguyên tắc phạm vi dữ liệu và thứ tự đã có, không còn câu hỏi chặn DD. `[SoT: BD mục Câu hỏi mở Q4–Q7]`
