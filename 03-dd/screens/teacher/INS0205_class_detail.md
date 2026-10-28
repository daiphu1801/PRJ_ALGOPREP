# DD màn hình — Chi tiết lớp (`INS0205`)

## 0. Thông tin tài liệu

Bảng 0.1 là bìa.

| Mục | Giá trị |
| :--- | :--- |
| Mã, actor, route | `INS0205`, INSTRUCTOR, `/instructor/classes/{classId}` |
| Nguồn | `01-rd/screens/teacher/INS0205_class_detail.md`; `02-bd/screens/teacher/INS0205_class_detail.md` |
| Prototype | `05-coding/frontend/src/views/teacher/class-detail/` |
| Module | F1 identity #19, #21–24, #26–27 |
| TDD | `04-tdd/modules/F1_identity.md` AC-20b, AC-21–22, AC-43; `04-tdd/screens/teacher/INS0205_class_detail.md` |
| Phiên bản, ngày | V0.1, 2026-10-24 |

Bảng 0.2 ghi lịch sử.

| Ver | Nội dung | Ngày |
| :--- | :--- | :--- |
| V0.1 | DD theo BD V hiện hành, phân vùng tải lớp/mã mời/học viên | 2026-10-24 |

Bảng 0.3 quy ước nguồn.

| Ký hiệu | Nguồn |
| :--- | :--- |
| BD | `02-bd/screens/teacher/INS0205_class_detail.md` Sheet 5, 8, 9 |
| F1 | `03-dd/api/F1_identity.md` mục 0.4; `03-dd/logic/F1_identity.md` BR-09, BR-11, BR-22 |
| V | `05-coding/frontend/src/views/teacher/class-detail/ui/class-detail-view.tsx` |

## 1. Bản đồ slice FSD

Bảng 1.1 là ranh giới slice tính từ `05-coding/frontend/src/`.

| Lớp | Đường dẫn | Vai trò | Hiện trạng |
| :--- | :--- | :--- | :--- |
| app | `app/(instructor)/instructor/classes/[classId]/page.tsx` | Route UUID và view | Có |
| views | `views/teacher/class-detail/ui/class-detail-view.tsx` | Thông tin, lịch, mã mời, bảng học viên, dialog | Có, mock |
| views | `views/teacher/class-detail/model/` | Ba query độc lập và mutation invalidation | Đích DD |
| entities | `entities/class/api/queries.ts` | Lớp F1 #19, mã #24, học viên #26, lệnh #21–23, #27 | Mock |
| shared | Modal/ConfirmDialog, Card, DataTable, ErrorState, toast, Clipboard | Dùng chung, không nâng cấp | Có |

View chọn lớp có `classId` từ F1 #19; danh sách có sẵn đủ metadata lớp theo BD. Không đọc lớp qua endpoint học viên #25B. Import `app → views → entities → shared`. `[SoT: BD Sheet 4.5, 8 EVT-1]`

## 2. API tiêu thụ

Bảng 2.1 ánh xạ mọi EVT Sheet 8.

| EVT | API ID | Hook/mutation đích | Hiện trạng |
| :--- | :--- | :--- | :--- |
| EVT-1 tải | F1 #19 `ListInstructorClasses`, #24 `ListClassInviteCodes`, #26 `ListClassStudents` | `useInstructorClasses`, `useClassInviteCodes(classId)`, `useClassStudents(classId)` | Mock |
| EVT-2 quay lại | Không API; `/instructor/classes` | `next/link` | Có |
| EVT-3 sửa | F1 #21 `UpdateClass` | `updateClass` | Mock |
| EVT-4 xoá | F1 #22 `DeleteClass` | `deleteClass` | Mock |
| EVT-5 sao chép mã | Không API; Clipboard | shared clipboard | Có |
| EVT-6 tạo mã | F1 #23 `CreateClassInviteCode` | `createInviteCode` | Mock |
| EVT-7 mở học viên | Không API; `/instructor/classes/{classId}/students/{studentId}` | `next/link` | Có |
| EVT-8 mở xác nhận gỡ | Không API | dialog state | Có |
| EVT-9 gỡ | F1 #27 `RemoveStudentFromClass` | `removeStudent` | Mock |

Không có realtime. #22 hard delete lớp và phát `ClassDeleted`; màn chỉ chuyển hướng khi API xác nhận thành công. Mã mời cũ không bị vô hiệu khi tạo mã mới. `[SoT: F1 #22–24; BD EVT-4, EVT-6]`

## 3. Trạng thái màn

Bảng 3.1 khoanh lỗi theo vùng; nếu API F1 chưa gán `error.code` riêng thì dùng mã chung từ `api.md`, không phân nhánh trên `message`.

| Vùng | Trạng thái | Xử lý | i18n |
| :--- | :--- | :--- | :--- |
| Toàn trang | UUID sai/lớp không thuộc giảng viên/404 | Không tìm thấy lớp, link về danh sách | `classDetail.*` |
| Thông tin lớp | #19 lỗi mạng/5xx | Vùng lỗi thay metadata và retry; các vùng đã tải không mất | `apiError.*` |
| Mã mời | #24 lỗi | Vùng lỗi riêng, không hiện mã mock cũ | `apiError.*` |
| Học viên | #26 lỗi | Vùng lỗi riêng, nút retry tại bảng | `apiError.*` |
| Sửa lớp | Kiểm nhập/API lỗi | Giữ dialog và dữ liệu đã nhập, toast lỗi | `classDetail.*`, `apiError.*` |
| Xoá/gỡ | API lỗi | Lớp/dòng vẫn ở màn; dialog đóng theo BD, toast lỗi | `apiError.*` |
| Mã mời | Tạo mới thành công | Chèn mã mới đầu danh sách; mã cũ giữ nguyên | `classDetail.*` |
| Clipboard | Bị từ chối | Toast hướng dẫn chọn và chép thủ công | `classDetail.*` |

## 4. Quy tắc đặc thù màn

**BR-SC-01 — Ba nguồn tải độc lập.** Kích hoạt EVT-1 với UUID hợp lệ và quyền `CLASS_MANAGEMENT:READ`. Gọi #19, #24, #26 song song theo cùng `classId`; chọn metadata lớp từ #19. Lỗi của mã mời/học viên chỉ thay vùng đó; nếu lớp không tồn tại hoặc ngoài quyền, cả trang là không tìm thấy và không hiển thị dữ liệu tải được từ ID cũ. Hậu điều kiện: mỗi vùng retry riêng. `[SoT: BD EVT-1, Sheet 9 NO 2]`

**BR-SC-02 — Ghi lớp và làm mới.** Kích hoạt EVT-3 từ dialog sửa với form hợp lệ. Gọi F1 #21; chỉ đóng khi thành công, invalidate #19 và cập nhật đầu trang/lịch. Lỗi giữ form để người dùng sửa. Kích hoạt EVT-4 sau xác nhận; chỉ điều hướng `/instructor/classes` và hiện toast ở đích khi F1 #22 thành công. Hậu điều kiện: không hiển thị lớp đã xoá. `[SoT: BD EVT-3/4]`

**BR-SC-03 — Mã mời và gỡ học viên.** EVT-6 tạo mã mới không thay mã cũ, refetch #24 sau thành công. EVT-8/9 gỡ học viên chỉ sau xác nhận, gọi #27; thành công loại dòng/invalidate #26, giữ lịch sử học tập theo BR-11. Clipboard EVT-5 không gửi mã lên API khác. `[SoT: BD EVT-5/6/8/9]`

## 5. Kiểm tra phía client

Bảng 5.1 đối chiếu form với F1/BD; server kiểm lại.

| Trường | Zod/client | Nguồn | Khớp |
| :--- | :--- | :--- | :--- |
| `classId`, `studentId` | `z.string().uuid()` trước link/mutation | F1 #21–27; BD Sheet 9 | Có |
| Tên lớp, mô tả | Giới hạn theo form lớp dùng chung `INS0201`; trim theo F1 validation | `02-bd/screens/teacher/INS0205_class_detail.md` Sheet 9 NO 3–5 | Có khi dùng chung schema |
| `schedule` | Schema cấu trúc ngày/giờ/địa điểm dùng chung `entities/class/model/types.ts` | F1 #21; `DEC-2026-1011-class-schedule-structured` | Có khi nối API |

## 6. i18n và truy cập

Tiền tố `classDetail.*`, lỗi chung `apiError.*` ở hai locale. Modal sửa/xoá/gỡ có tiêu đề và focus trap; huỷ trả focus về nút mở. Mã mời có nhãn đọc và nút sao chép nêu rõ mã nào; không chỉ icon. Bảng học viên có header và tên là link có đích rõ. Retry theo vùng đặt focus lại đúng vùng sau thành công. Toast dùng `aria-live="polite"`. `[SoT: BD Sheet 5; V]`

## 7. Điểm kiểm thử

Bảng 7.1 bao phủ EVT-1–9; TDD module bảo đảm giao dịch server.

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| SC-01 | Normal | EVT-1 ba vùng tải độc lập; lớp ngoài quyền không hiện dữ liệu | Vitest | `class_detail` AC-SC-01 |
| SC-02 | Normal | EVT-2 quay về danh sách; EVT-7 tới đúng học viên của lớp | Playwright | `class_detail` AC-SC-01 |
| SC-03 | Normal | EVT-3 sửa form hợp lệ, invalidate; lỗi giữ dialog | Vitest | `class_detail` AC-SC-02 |
| SC-04 | Abnormal | EVT-4 xoá lỗi không chuyển; thành công về danh sách và phát sự kiện | Vitest | F1 AC-43; `class_detail` AC-SC-02 |
| SC-05 | Boundary | EVT-5 clipboard từ chối; EVT-6 tạo mã không mất mã cũ | Vitest | `class_detail` AC-SC-02 |
| SC-06 | Normal | EVT-8/9 gỡ sau xác nhận, giữ lịch sử, lỗi rollback | Vitest | F1 AC-21; `class_detail` AC-SC-02 |

## 8. Nợ prototype và câu hỏi mở

**Nợ prototype:** ba nhóm vẫn dùng mock và đang chia sẻ `entities/class` cho giảng viên/học viên; cần nối đúng F1 #19/#24/#26, dùng UUID thật, tách retry, sửa invalidate khi sửa/xoá/gỡ. Dòng `INS0205` trong `06-plan/PROTOTYPE_DEBT.md` còn mở đến khi code tích hợp. `[SoT: BD Sheet 4.4; V]`

**Đã đóng ở DD:** lớp không có/ngoài phạm vi dùng `404 IDT-414` thống nhất với các API đọc giảng viên; tên/mô tả/lịch học theo validation F1 §5 (`IDT-V10`, `IDT-V14`–`V16`). Khi nối API, màn ánh xạ `error.code` từ hợp đồng F1, không dựa vào chuỗi `message`. `[SoT: BD Sheet 9; F1 mục 0.4; validation F1 §5]`
