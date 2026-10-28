# DD màn hình — Học viên trong lớp (`INS0204`)

## 0. Thông tin tài liệu

Bảng 0.1 là bìa.

| Mục | Giá trị |
| :--- | :--- |
| Mã, actor, route | `INS0204`, INSTRUCTOR, `/instructor/classes/{classId}/students/{studentId}` |
| Nguồn | `01-rd/screens/teacher/INS0204_class_student_detail.md`; `02-bd/screens/teacher/INS0204_class_student_detail.md` |
| Prototype | `05-coding/frontend/src/views/teacher/class-student-detail/` |
| Module | F1 identity #28/#27; F2 problem-bank #49/§50; F4 judge-orchestration #12 và cổng P1 qua F1 |
| TDD | `04-tdd/modules/F1_identity.md` AC-21–22; `04-tdd/modules/F4_judge-orchestration.md` AC-46–47; `04-tdd/screens/teacher/INS0204_class_student_detail.md` |
| Phiên bản, ngày | V0.1, 2026-10-24 |

Bảng 0.2 ghi lịch sử.

| Ver | Nội dung | Ngày |
| :--- | :--- | :--- |
| V0.1 | DD kết hợp hồ sơ lớp, bài giao và lịch sử nộp trong phạm vi lớp | 2026-10-24 |

Bảng 0.3 quy ước nguồn.

| Ký hiệu | Nguồn |
| :--- | :--- |
| BD | `02-bd/screens/teacher/INS0204_class_student_detail.md` Sheet 5, 8, 9 |
| F1/F2/F4 | `03-dd/api/F1_identity.md` #27–28; `F2_problem-bank.md` §50; `F4_judge-orchestration.md` #12, P1 |
| V | `05-coding/frontend/src/views/teacher/class-student-detail/ui/class-student-detail-view.tsx` |

## 1. Bản đồ slice FSD

Bảng 1.1 là slice từ `05-coding/frontend/src/`.

| Lớp | Đường dẫn | Vai trò | Hiện trạng |
| :--- | :--- | :--- | :--- |
| app | `app/(instructor)/instructor/classes/[classId]/students/[studentId]/page.tsx` | Route và view | Có |
| views | `views/teacher/class-student-detail/ui/class-student-detail-view.tsx` | Hồ sơ, tiến độ, lịch sử, xác nhận gỡ | Có, mock |
| views | `views/teacher/class-student-detail/model/` | Kết hợp bốn nguồn và retry theo khối | Đích DD |
| entities | `entities/class/api/queries.ts` | F1 #28 hồ sơ/tiến độ; #27 gỡ | Mock |
| entities | `entities/problem/api/queries.ts` | F2 §50 bài giao của lớp | Mock |
| entities | `entities/submission/api/queries.ts` | F4 #12 lịch sử có phân trang | Mock |
| shared | Card, Chart, DataTable, Pagination, ConfirmDialog, ErrorState | Tái dùng | Có |

F4 P1 là cổng **trong tiến trình** do F1 dùng để dựng F1 #28, không phải HTTP mà frontend gọi trực tiếp. View không đọc mã nguồn từ bảng lịch sử. Import `app → views → entities → shared`. `[SoT: F4 mục 26.1; BD Sheet 7.3]`

## 2. API tiêu thụ

Bảng 2.1 ánh xạ đủ EVT Sheet 8.

| EVT | API ID | Hook/mutation đích | Hiện trạng |
| :--- | :--- | :--- | :--- |
| EVT-1 tải | F1 #28 `GetClassStudentProfile`; F2 #49/§50 `ListClassAssignments?classId`; F4 #12 `ListStudentClassSubmissions` | `useClassStudentDetail`, `useClassAssignments`, `useStudentClassSubmissions` | Mock |
| EVT-2 quay lại | Không API; `router.back()` với fallback màn lớp | Navigation | Có |
| EVT-3 quản lý lớp | Không API; `/instructor/classes` | `next/link` | Có |
| EVT-4 mở gỡ | Không API | dialog state | Có |
| EVT-5 xác nhận gỡ | F1 #27 `RemoveStudentFromClass` | `removeStudent` | Mock |
| EVT-6 huỷ gỡ | Không API | dialog state | Có |
| EVT-7 đổi trang lịch sử | F4 #12 `page`, cỡ 20 | `useStudentClassSubmissions` | Mock |
| EVT-8 thử lại khối | Refetch đúng F1/F2/F4 đang lỗi | `refetch` theo khối | Cần tách |
| EVT-9 mở kết quả | Không API; `/instructor/submissions/{submissionId}` | `next/link` | Có |

F1 #28 gọi F4 P1 trong server để lấy metrics theo lớp; không thêm REST F4 cho P1. F2 §50 phục vụ tên/chủ đề bài giao, nhưng DTO này nhóm theo bài, không chứa điểm tốt nhất từng học viên; phần điểm tốt nhất thuộc F1 #28 theo BD. `[SoT: F1 #28; F2 §50; BD Sheet 7.1]`

## 3. Trạng thái màn

Bảng 3.1 ánh xạ lỗi theo khối và `error.code`.

| Vùng | Trạng thái | Xử lý | i18n |
| :--- | :--- | :--- | :--- |
| Toàn trang | UUID sai, F1 404, F4 `JDG-111/112` | Không tìm thấy học viên trong lớp; không lộ lớp ngoài quyền | `classStudentDetail.*` |
| Hồ sơ | F1 lỗi mạng/5xx | Lỗi thay hồ sơ và số liệu; khối khác còn nếu hợp lệ | `apiError.*` |
| Bài giao | F2 lỗi | Chỉ bảng bài giao/tiến độ theo bài báo lỗi | `apiError.*` |
| Lịch sử | F4 lỗi | Vùng lịch sử lỗi và retry; hồ sơ giữ nguyên | `apiError.*` |
| Lịch sử | Rỗng hoặc `scoreRatio=null` | Câu chưa có lượt nộp; tỉ lệ null hiển thị `-` | `classStudentDetail.*` |
| Gỡ học viên | F1 lỗi | Giữ người học, toast lỗi; không điều hướng | `apiError.*` |

Không dùng `message` server. Người học đã rời lớp vẫn có lịch sử đúng cửa sổ nhưng F1 có thể cấm trang chi tiết theo BD; khi đó màn theo 404 F1, không vượt quyền bằng cách giữ F4 response cũ. `[SoT: BD Sheet 9; F4 #12]`

## 4. Quy tắc đặc thù màn

**BR-SC-01 — Khóa phạm vi theo cặp ID.** Kích hoạt EVT-1 khi route có hai UUID. Gọi F1/F2/F4 theo cùng `classId`, F1/F4 thêm `studentId` đúng route; đổi một ID huỷ/loại mọi kết quả cũ. Nếu F1 xác nhận không thuộc phạm vi, bỏ dữ liệu phụ và hiện không tìm thấy. Hậu điều kiện: không ghép bảng lớp A với học viên lớp B. `[SoT: BD Sheet 9 NO 2; F4 #12]`

**BR-SC-02 — Phân vùng lỗi và phân trang.** Kích hoạt EVT-1/7/8. Mỗi khối lỗi có retry riêng; phân trang chỉ gọi lại F4 #12, không tải lại hồ sơ và bài giao. Trang mới lỗi giữ trang đang xem, retry theo trang đã chọn. Hậu điều kiện: số liệu còn đọc được khi một nguồn suy giảm. `[SoT: BD Sheet 8 EVT-1, 7, 8]`

**BR-SC-03 — Gỡ học viên và điều hướng.** Kích hoạt EVT-4–6 sau xác nhận. Gọi F1 #27 một lần; thành công cập nhật lớp và về màn gọi/danh sách lớp; thất bại giữ trang và dữ liệu. EVT-9 mở kết quả với `submissionId` thật; trang đích F4 #11 tự kiểm quyền, không tin `classId` query. `[SoT: BD Sheet 8 EVT-4–6, 9]`

## 5. Kiểm tra phía client

Bảng 5.1 là kiểm URL/phân trang; server kiểm lại quyền.

| Trường | Zod/client | Nguồn | Khớp |
| :--- | :--- | :--- | :--- |
| `classId`, `studentId` | `z.string().uuid()`; sai dạng không gọi API, cùng màn không tìm thấy | F1 #28, F4 #12; BD Sheet 9 | Có |
| `page` | integer ≥1, `pageSize=20` | F4 #12, BD EVT-7 | Có |
| `submissionId` từ dòng | UUID trước link | F4 #12/#11 | Có |

## 6. i18n và truy cập

Tiền tố `classStudentDetail.*`, lỗi `apiError.*` hai locale. Mỗi khối số liệu có tiêu đề, đơn vị và trạng thái không có dữ liệu; biểu đồ tuần có bảng/văn bản thay thế. Trang lịch sử là bảng có header, nút trang có tên rõ. Dialog gỡ có tên học viên/lớp, focus trap, huỷ trả focus. Retry theo khối không cuộn toàn trang. `[SoT: BD Sheet 5; V]`

## 7. Điểm kiểm thử

Bảng 7.1 bao phủ EVT-1–9.

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| SC-01 | Security | EVT-1 hai UUID khác lớp hoặc ngoài quyền không ghép dữ liệu/lộ bài | Vitest | F4 AC-46; `class_student_detail` AC-SC-01 |
| SC-02 | Abnormal | EVT-1 F2/F4 lỗi giữ hồ sơ; F1 404 xoá dữ liệu phụ | Vitest | `class_student_detail` AC-SC-01 |
| SC-03 | Normal | EVT-2/3 về đúng danh sách và quản lý lớp | Playwright | `class_student_detail` AC-SC-02 |
| SC-04 | Normal | EVT-4/5/6 chỉ gỡ sau xác nhận, lịch sử được giữ | Vitest | F1 AC-21; `class_student_detail` AC-SC-02 |
| SC-05 | Boundary | EVT-7 lịch sử 25 dòng có trang 2; `scoreRatio=null` là `-` | Vitest | F4 AC-46 |
| SC-06 | Abnormal | EVT-8 retry đúng khối và trang, không reload toàn bộ | Vitest | `class_student_detail` AC-SC-01 |
| SC-07 | Security | EVT-9 link kết quả tới F4 #11, trang đích tự kiểm quyền | Playwright | F4 AC-45 |

## 8. Nợ prototype và câu hỏi mở

**Nợ prototype:** bốn nguồn của BD hiện được mock/gộp; cần đổi sang F1 #28, F2 §50, F4 #12, không gọi P1 trực tiếp từ browser, phân trang/retry từng vùng và UUID thật. Dòng `INS0204` của `06-plan/PROTOTYPE_DEBT.md` giữ mở tới khi code nối thật. `[SoT: BD Sheet 4.4; V]`

**Đã đóng ở DD:** F1 #28 mục 0.4.2 định nghĩa DTO, quyền đọc và nguồn `assignmentProgress`; F4 P1 là cổng nội bộ của F1, không có REST F4 mới. Điểm tốt nhất ghép với F2 §50 bằng `problemId`; nhãn tiến độ lấy từ metrics theo phạm vi lớp. `[SoT: BD Sheet 7.3; F1 #28; F4 P1]`
