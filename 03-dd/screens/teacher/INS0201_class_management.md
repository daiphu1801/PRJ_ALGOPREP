# DD màn hình — Quản lý lớp (`INS0201`)

## 0. Thông tin tài liệu

Bảng 0.1 là bìa.

| Mục | Giá trị |
| :--- | :--- |
| Mã, actor, route | `INS0201`, INSTRUCTOR, `/instructor/classes` |
| Nguồn | `01-rd/screens/teacher/INS0201_class_management.md`; `02-bd/screens/teacher/INS0201_class_management.md` |
| Prototype | `05-coding/frontend/src/views/teacher/class-management/` |
| Module | F1 identity #19–24, #27, #56, #59 |
| TDD | `04-tdd/modules/F1_identity.md` AC-21–22, AC-43, AC-51; `04-tdd/screens/teacher/INS0201_class_management.md` |
| Phiên bản, ngày | V0.1, 2026-10-24 |

Bảng 0.2 ghi lịch sử.

| Ver | Nội dung | Ngày |
| :--- | :--- | :--- |
| V0.1 | DD thẻ lớp, form tạo/sửa có lịch cấu trúc, gỡ học viên và phân trang | 2026-10-24 |

Bảng 0.3 quy ước nguồn.

| Ký hiệu | Nguồn |
| :--- | :--- |
| BD | `02-bd/screens/teacher/INS0201_class_management.md` Sheet 5, 8, 9 |
| F1 | `03-dd/api/F1_identity.md` mục 0.4, 0.8.2–0.8.3; `03-dd/validation/F1_identity.md` |
| V | `05-coding/frontend/src/views/teacher/class-management/ui/class-management-view.tsx` |
| DEC | `DEC-2026-1024-dd-teacher-f1-read-contracts`; `DEC-2026-1011-class-schedule-structured` |

## 1. Bản đồ slice FSD

Bảng 1.1 là slice tính từ `05-coding/frontend/src/`.

| Lớp | Đường dẫn | Vai trò | Hiện trạng |
| :--- | :--- | :--- | :--- |
| app | `app/(instructor)/instructor/classes/page.tsx` | Route và view | Có |
| views | `views/teacher/class-management/ui/class-management-view.tsx` | Ba thẻ tổng, lưới lớp, bảng học viên, dialog | Có, mock |
| views | `views/teacher/class-management/model/` | Form schema, lọc lớp, trang bảng, invalidation | Đích DD |
| entities | `entities/class/api/queries.ts` | F1 #56/#19, tạo #20, sửa #21, xoá #22, gỡ #27, danh sách học viên | Mock/chưa đủ |
| entities | `entities/class/model/types.ts`, `model/schedule.ts` | Kiểu lịch JSON, định dạng một dòng | Có, cần khớp DTO F1 |
| shared | Card, Tabs, DataTable, Pagination, Modal/ConfirmDialog, ErrorState, toast | Tái dùng; form sửa chia sẻ với `INS0205` | Có |

Không tạo popup mã mời ở màn này: EVT-8–10 đã chuyển sang `INS0205`. Tạo lớp xong tới trang chi tiết nơi mã mời đầu tiên đã sinh. Import `app → views → entities → shared`. `[SoT: BD V0.6, Sheet 4.5]`

## 2. API tiêu thụ

Bảng 2.1 ánh xạ EVT Sheet 8; số bị bỏ được giữ để truy vết.

| EVT | API ID | Hook/mutation đích | Hiện trạng |
| :--- | :--- | :--- | :--- |
| EVT-1 tải | F1 #56 `GetInstructorClassSummary` (gồm thẻ tổng), #19 `ListInstructorClasses`, #59 `ListInstructorStudents` | Ba query độc lập | Mock |
| EVT-2 mở tạo | Không API | form/dialog state | Có |
| EVT-3 lưu lớp | F1 #20 `CreateClass` hoặc #21 `UpdateClass` | `createClass`/`updateClass` | Mock |
| EVT-4 huỷ form | Không API | dialog state | Có |
| EVT-5 menu lớp | Không API | popover state | Có |
| EVT-6 sửa lớp | Không API khi mở; F1 #21 ở EVT-3 | form/dialog state | Có |
| EVT-7 chi tiết lớp | Không API; `/instructor/classes/{classId}` | `next/link` | Có |
| EVT-8, EVT-9, EVT-10 đã chuyển | Không dựng mã mời ở màn này; dùng `INS0205` | Không áp dụng | Cần gỡ sót code nếu có |
| EVT-11 mở xoá | Không API | ConfirmDialog | Có |
| EVT-12 xác nhận xoá | F1 #22 `DeleteClass` | `deleteClass` | Mock |
| EVT-13 huỷ xoá | Không API | dialog state | Có |
| EVT-14 chọn thẻ lớp | F1 #59 với `classId` tương ứng, hoặc bỏ `classId` khi bấm lại | query bảng | Mock |
| EVT-15 tab lớp | F1 #59 với cùng phạm vi EVT-14 | query bảng | Mock |
| EVT-16 mở học viên | Không API; `/instructor/classes/{classId}/students/{studentId}` | `next/link` | Có |
| EVT-17 mở xác nhận gỡ | Không API | ConfirmDialog | Có |
| EVT-18 gỡ học viên | F1 #27 `RemoveStudentFromClass` | `removeStudent` | Mock |
| EVT-19 huỷ gỡ | Không API | dialog state | Có |
| EVT-20 trang bảng | F1 #59 với `page`, `pageSize=20` | query bảng | Mock |

#20 tạo mã mời đầu tiên cùng lớp trong giao dịch; frontend chỉ dùng `classId` response để điều hướng, không tự gọi #23 tạo thêm mã. #22 xoá lớp hard delete và phát `ClassDeleted`. `[SoT: F1 #20/#22; BD EVT-3/12]`

## 3. Trạng thái màn

Bảng 3.1 là trạng thái và lỗi; frontend dựa trên `error.code`, không dùng `message` server.

| Vùng | Trạng thái | Xử lý | i18n |
| :--- | :--- | :--- | :--- |
| Tổng/lớp/bảng | Đang tải/lỗi độc lập | Skeleton hoặc retry đúng khối; khối khác giữ nguyên | `classManagement.*`, `apiError.*` |
| Lớp | Không có lớp | Empty state kèm tạo lớp | `classManagement.*` |
| Bảng | Không có học viên hoặc lọc không khớp | Câu riêng, không hiện học viên lớp khác | `classManagement.*` |
| Form | Tên trống/quá dài/lịch sai | Lỗi field, focus ô đầu sai; không gọi API | `classManagement.validation.*` |
| Form | API lỗi | Giữ dialog và draft, toast lỗi | `apiError.*` |
| Xoá/gỡ | API lỗi | Giữ thẻ/dòng, rollback mọi thay đổi lạc quan, toast | `apiError.*` |
| Bảng | Lớp ngoài phạm vi | Không tìm thấy lớp, bỏ dữ liệu bảng cũ | `apiError.*` |

## 4. Quy tắc đặc thù màn

**BR-SC-01 — Ba vùng đọc và lọc đồng bộ.** Kích hoạt EVT-1/14/15/20. #56 cung cấp ba thẻ số liệu tổng và dữ liệu tóm tắt; #19 là nguồn lưới/tab lớp; bảng học viên có lọc và phân trang server. Chọn thẻ đồng bộ tab, bấm lại thẻ đã chọn về Tất cả; đổi tab reset trang 1 và ẩn/hiện cột Lớp. Hậu điều kiện: thẻ tổng không bị lọc theo trang bảng. `[SoT: BD EVT-1/14/15/20]`

**BR-SC-02 — Tạo/sửa lớp bằng cùng form.** Kích hoạt EVT-2/3/4/6. Form tạo rỗng, focus Tên lớp; form sửa nạp lại đúng dữ liệu lớp **mỗi lần mở**, không dùng draft cũ. Kiểm tên/mô tả/lịch cấu trúc; tạo gọi #20 một lần, thành công đóng popup và tới `INS0205` theo `classId` mới; sửa gọi #21, thành công refetch #19/#56/bảng. Lỗi giữ popup và draft. Hậu điều kiện: trang chi tiết có mã mời đầu tiên, không tạo mã hai lần. `[SoT: BD EVT-2–4/6, V0.6]`

**BR-SC-03 — Xoá và gỡ có xác nhận.** Kích hoạt EVT-11–13 hoặc EVT-17–19. Dialog nói rõ xoá lớp là không khôi phục, gỡ học viên chỉ đặt `left_at` và giữ lịch sử. Chỉ nút xác nhận gọi #22/#27; thành công refetch lớp, tổng và bảng, xoá lớp chọn đưa tab về Tất cả; lỗi giữ dữ liệu. Hậu điều kiện: không có thao tác ghi khi huỷ. `[SoT: BD EVT-11–13/17–19]`

## 5. Kiểm tra phía client

Bảng 5.1 đối chiếu zod với BD/validation F1; server kiểm lại.

| Trường | Zod/client | Nguồn | Khớp |
| :--- | :--- | :--- | :--- |
| `name` | trim, không rỗng, tối đa 120 ký tự | BD Sheet 9 NO 3–4 | Theo đề xuất BD; F1 validation cần ghi rõ |
| `description` | tối đa 500 ký tự, tuỳ chọn | BD Sheet 9 NO 4 | Theo đề xuất BD |
| `schedule.days` | ISO 1–7, không trùng, sắp tăng | BD Sheet 9 NO 11; schema `entities/class` | Có |
| `schedule.startTime/endTime` | cặp HH:mm, kết thúc sau bắt đầu; có giờ phải có ngày | BD Sheet 9 NO 11 | Có |
| `schedule.startDate/endDate` | ngày ISO, kết thúc không trước bắt đầu | BD Sheet 9 NO 11 | Có |
| `schedule.room/note` | 100/200 ký tự | BD Sheet 7.1 NO 17c/e | Theo đề xuất BD |
| `classId/studentId/page` | UUID cho ID, page ≥1, `pageSize=20` | F1 #19/#27; BD EVT-20 | Có |

## 6. i18n và truy cập

Tiền tố `classManagement.*` và `apiError.*` hai locale. Thẻ lớp có tên/link riêng, menu hành động có tên đọc nêu lớp. Tab lớp theo phím của shared Tabs, chọn thẻ và tab đồng bộ trạng thái `aria-selected`. Popup form dùng label, mô tả lỗi `aria-describedby`, focus ô đầu sai. Dialog xoá/gỡ có focus trap và trả focus khi huỷ. Bảng học viên có header và link nêu tên/lớp; trang đổi cuộn bảng về đầu. `[SoT: BD Sheet 5, 8]`

## 7. Điểm kiểm thử

Bảng 7.1 bao phủ EVT-1–20.

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| SC-01 | Normal | EVT-1 ba vùng độc lập, học viên tổng DISTINCT, không có lớp empty | Vitest | F1 AC-51 |
| SC-02 | Normal | EVT-2/3/4/6 tạo/sửa bằng form chung, nạp draft đúng, tạo đi trang chi tiết | Vitest | `class_management` AC-SC-02 |
| SC-03 | Boundary | EVT-3 tên/lịch/độ dài sai không gửi, focus đúng | Vitest | TDD màn `AC-SC-03` |
| SC-04 | Normal | EVT-5/7 menu và tên lớp mở đúng `INS0205` | Playwright | `class_management` AC-SC-02 |
| SC-05 | Boundary | EVT-8–10 không còn nút mã mời ở màn này | Vitest | TDD màn `AC-SC-04` |
| SC-06 | Abnormal | EVT-11/12/13 xoá chỉ sau xác nhận, lỗi giữ lớp, thành công refetch | Vitest | F1 AC-43; `class_management` AC-SC-02 |
| SC-07 | Normal | EVT-14/15 chọn thẻ/tab đồng bộ, bấm lại về Tất cả | Vitest | `class_management` AC-SC-01 |
| SC-08 | Normal | EVT-16 mở đúng học viên của lớp | Playwright | `class_management` AC-SC-01 |
| SC-09 | Abnormal | EVT-17/18/19 gỡ giữ lịch sử, huỷ không gọi API, lỗi rollback | Vitest | F1 AC-21; `class_management` AC-SC-02 |
| SC-10 | Boundary | EVT-20 trang 2 có đúng 20 dòng, đổi tab reset trang 1 | Vitest | `class_management` AC-SC-01 |

## 8. Nợ prototype và câu hỏi mở

**Nợ prototype:** cần nối F1 #56/#19, form #20/#21, xoá #22, gỡ #27 và bảng học viên toàn lớp có phân trang; bỏ mọi popup mã mời còn sót. Đối chiếu `ClassCardDto.description`, lịch cấu trúc và tính học viên DISTINCT. Dòng `INS0201` trong `06-plan/PROTOTYPE_DEBT.md` giữ mở tới khi code khớp. `[SoT: BD Sheet 4.5; V]`

**Đã đóng:** bảng học viên tab Tất cả dùng F1 #59, phân trang sau lọc trên toàn bộ lớp phụ trách (`DEC-2026-1024-dd-teacher-student-list`). Các giới hạn tên/mô tả/phòng/ghi chú giữ theo BD Sheet 9; cần đối chiếu validation F1 trước khi code nối thật.
