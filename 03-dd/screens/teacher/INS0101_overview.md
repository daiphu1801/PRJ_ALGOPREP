# DD màn hình — Tổng quan giảng viên (`INS0101`)

## 0. Thông tin tài liệu

Bảng 0.1 là bìa.

| Mục | Giá trị |
| :--- | :--- |
| Mã, actor, route | `INS0101`, INSTRUCTOR, `/instructor/overview` |
| Nguồn | `01-rd/screens/teacher/INS0101_overview.md`; `02-bd/screens/teacher/INS0101_overview.md` |
| Prototype | `05-coding/frontend/src/views/teacher/overview/` |
| Module | F1 identity #56/#45; F2 problem-bank #54/§55 |
| TDD | `04-tdd/modules/F1_identity.md` AC-51; `04-tdd/modules/F2_problem-bank.md` AC-77; `04-tdd/screens/teacher/INS0101_overview.md` |
| Phiên bản, ngày | V0.1, 2026-10-24 |

Bảng 0.2 ghi lịch sử.

| Ver | Nội dung | Ngày |
| :--- | :--- | :--- |
| V0.1 | DD bốn khối dữ liệu sau khi bỏ chấm tay và biểu đồ tiến độ | 2026-10-24 |

Bảng 0.3 quy ước nguồn.

| Ký hiệu | Nguồn |
| :--- | :--- |
| BD | `02-bd/screens/teacher/INS0101_overview.md` Sheet 5, 8, 9 |
| F1/F2 | `03-dd/api/F1_identity.md` #45/#56; `03-dd/api/F2_problem-bank.md` §55 |
| V | `05-coding/frontend/src/views/teacher/overview/ui/instructor-overview-view.tsx` |
| DEC | `DEC-2026-1024-dd-teacher-f1-read-contracts` |

## 1. Bản đồ slice FSD

Bảng 1.1 là slice tính từ `05-coding/frontend/src/`.

| Lớp | Đường dẫn | Vai trò | Hiện trạng |
| :--- | :--- | :--- | :--- |
| app | `app/(instructor)/instructor/overview/page.tsx` | Route và view | Có |
| views | `views/teacher/overview/ui/instructor-overview-view.tsx` | Hai thẻ thống kê, hoạt động, lớp, năm bài giao | Có, mock |
| views | `views/teacher/overview/model/` | Adapter ba query và trạng thái từng khối | Đích DD |
| entities | `entities/class/api/queries.ts` | F1 #56 tóm tắt lớp | Mock |
| entities | `entities/user/api/queries.ts` | F1 #45 hoạt động gần đây | Mock |
| entities | `entities/class-assignment/api/queries.ts` | F2 §55 năm bài giao nhiều lượt nộp | Mock |
| shared | Card, RankedProgressList, Skeleton, ErrorState, `useT` | Tái dùng, không nâng cấp | Có |

Không có widget chấm tay hoặc biểu đồ điểm tuần. View không tính `studentCount` bằng cộng sĩ số lớp; dùng tổng DISTINCT của F1 #56. Import `app → views → entities → shared`. `[SoT: BD phần mở đầu V0.11–0.13, Sheet 4.5]`

## 2. API tiêu thụ

Bảng 2.1 ánh xạ đủ EVT Sheet 8 hiện hành.

| EVT | API ID | Hook/query đích | Hiện trạng |
| :--- | :--- | :--- | :--- |
| EVT-1 khởi tạo | F1 #56 `ListMyClassesSummary`, F1 #45 `ListInstructorRecentActivity`, F2 #54/§55 `ListMyAssignedProblems` | Ba query độc lập | Mock |
| EVT-2, EVT-3 đã bỏ | Không có điều khiển hay API | Không dựng | Gỡ theo BD |
| EVT-4 avatar | Không API màn; shell mở menu tài khoản | Shared shell | Có |
| EVT-5 Quản lý lớp | Không API; `/instructor/classes` | `next/link` | Có |
| EVT-6 Xem tất cả bài tập | Không API; `/instructor/assignments` | `next/link` | Có |
| EVT-7 quay lại/làm mới | Refetch F1 #56/#45 và F2 #54 | query invalidate/refetch | Cần nối thật |

Màn chỉ đọc. #56 trả tổng lớp/tổng học viên và `items` lớp để vẽ danh sách; #54 lấy năm bài nhiều lượt nộp nhất tính trong lớp của giảng viên, không dùng `problem_stats.submission_count`. `[SoT: BD Sheet 7.1, 8; F2 §55]`

## 3. Trạng thái màn

Bảng 3.1 là trạng thái theo khối; không dùng `error.message` server.

| Vùng | Trạng thái | Xử lý | i18n |
| :--- | :--- | :--- | :--- |
| Toàn trang | 401/403 | Shell chuyển auth/không quyền | `apiError.*` |
| Lớp/thống kê | F1 #56 đang tải/lỗi | Skeleton hoặc lỗi riêng; không dựng tổng từ mock/lớp khác | `instructorOverview.*`, `apiError.*` |
| Hoạt động | F1 #45 rỗng/lỗi | Câu chưa có hoạt động hoặc retry riêng | `instructorOverview.*` |
| Bài giao | F2 §55 rỗng/lỗi | Câu chưa giao bài hoặc retry riêng; hai khối F1 vẫn hiện | `instructorOverview.*` |
| Mọi khối | Vừa trở lại màn | Làm mới đúng nguồn, không chớp toàn trang | `instructorOverview.*` |

## 4. Quy tắc đặc thù màn

**BR-SC-01 — Tải độc lập và thống kê đúng người.** Kích hoạt EVT-1/7 khi giảng viên vào hoặc quay lại. Gọi #56, #45, §55 song song; mỗi khối nhận loading/error riêng. Thẻ “Tổng học viên” lấy `studentCount` DISTINCT từ #56, không cộng sĩ số từng lớp; danh sách lớp sắp theo F1, tiến độ lớp lấy dữ liệu đã tính ở server. Hậu điều kiện: F2 lỗi không xoá lớp/hoạt động. `[SoT: BD Sheet 7.1, 8 EVT-1/7]`

**BR-SC-02 — Năm bài giao và điều hướng tĩnh.** Kích hoạt EVT-1/5/6. §55 trả tối đa năm bài, xếp theo lượt nộp **trong lớp của giảng viên**. Dòng bài và thẻ lớp ở Overview không có link khoan sâu theo BD; chỉ hai link “Quản lý lớp” và “Xem tất cả” điều hướng. Không sinh sự kiện từ hai EVT đã bỏ. `[SoT: BD V0.12, Sheet 8]`

## 5. Kiểm tra phía client

Không có form hoặc tham số route. Dữ liệu ID từ #56/§55 được schema entity kiểm là UUID, nhưng màn không dựng link theo ID này. Tham số `limit=5` của §55 do query cố định, không nhận đầu vào người dùng. Validation còn lại ở F1/F2 module. `[SoT: F2 §55; BD Sheet 8]`

## 6. i18n và truy cập

Tiền tố `instructorOverview.*` trong hai locale. Hai thẻ thống kê có tên và đơn vị đọc được; danh sách thanh xếp hạng phải có số/văn bản tương ứng, không chỉ chiều dài hoặc màu. Hoạt động gần đây dùng mốc thời gian theo locale. Link “Quản lý lớp” và “Xem tất cả” có đích rõ; avatar do shell gác phím/focus. Lỗi từng khối có nút retry trong tab order bình thường. `[SoT: BD Sheet 5; V]`

## 7. Điểm kiểm thử

Bảng 7.1 bao phủ EVT-1–7, kể cả sự kiện đã bỏ.

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| SC-01 | Normal | EVT-1 ba query, hai thẻ + ba widget, tổng học viên DISTINCT | Vitest | F1 AC-51; F2 AC-77 |
| SC-02 | Abnormal | EVT-1 F2 lỗi chỉ bài giao retry, F1 vẫn hiện | Vitest | `instructor_overview` AC-SC-01 |
| SC-03 | Boundary | EVT-2/3 không còn ô tìm/tạo lớp, không gọi API | Vitest | `instructor_overview` AC-SC-02 |
| SC-04 | Normal | EVT-4 avatar shell, EVT-5/6 tới đúng route | Playwright | `instructor_overview` AC-SC-02 |
| SC-05 | Normal | EVT-7 quay lại sau thay đổi lớp/bài thì refetch đúng nguồn | Vitest | `instructor_overview` AC-SC-01 |

## 8. Nợ prototype và câu hỏi mở

**Nợ prototype:** dùng mock và trước đây cộng `studentCount` từng lớp; cần F1 #56 để đếm DISTINCT, F1 #45 và F2 §55 thật, trạng thái lỗi từng widget, làm mới khi quay lại. Dòng `INS0101` trong `06-plan/PROTOTYPE_DEBT.md` còn mở đến khi code nối thật. `[SoT: BD Sheet 4.5; V]`

**Đã đóng ở DD:** `INS0101` dùng F1 #56 `ListMyClassesSummary`; F1 #44 được đánh dấu legacy, không còn consumer màn DD sau khi BD bỏ thẻ điểm TB. Giữ path trong danh mục để tránh thay đổi số endpoint cũ. `[SoT: BD V0.11; F1 #44/#56]`
