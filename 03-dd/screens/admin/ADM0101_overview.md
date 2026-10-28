# DD màn hình — Tổng quan Admin (`ADM0101`)

## 0. Thông tin tài liệu

| Mục | Giá trị |
| :--- | :--- |
| Actor, route | A3 `ADMIN`; `/admin` |
| Nguồn | `01-rd/screens/admin/ADM0101_overview.md`; `02-bd/screens/admin/ADM0101_overview.md` Sheet 1–9; [F1](../../api/F1_identity.md) §0.8 |
| Prototype | `05-coding/frontend/src/views/admin/overview/ui/admin-overview-view.tsx` |
| Phiên bản | V0.1, 2026-10-25; DD màn, Mode B |

## 1. Bản đồ slice FSD

| Lớp | Đường dẫn từ `src/` | Vai trò |
| :--- | :--- | :--- |
| app | `app/(admin)/admin/page.tsx` | Gắn khung A3, chặn actor khác |
| views | `views/admin/overview/ui/admin-overview-view.tsx` | KPI, biểu đồ, danh sách top bài, trạng thái từng khối |
| widgets | `widgets/admin-shell/` | Sidebar, toolbar, footer theo BD `_shell` |
| entities | `entities/admin-dashboard/api/queries.ts` | F1 #41–43; cache từng khối |
| shared | `StatCard`, `Chart`, `ErrorState`, `Button` | Biểu đồ có bảng/nhãn đọc được |

## 2. API tiêu thụ

| EVT BD | API | Hành vi màn |
| :--- | :--- | :--- |
| 1 mở màn | F1 #41–43 và #67–73, mười GET tại F1 §0.8.5 | Mỗi khối một query, dùng cổng F4/F2 qua F1 khi cần |
| 2 tải lại | Endpoint của đúng khối lỗi | Giữ các khối thành công |
| 3 xem tất cả bài | Không gọi API | Mở `/admin/problems` |
| 4 thanh bên | Không gọi API tại view | Khung A3 xử lý điều hướng |

Mười operation đọc ở BD Sheet 7.3 khớp mười HTTP endpoint F1. View lấy trường từ F1 §0.8.5 và không tự truy vấn F4 cho từng biểu đồ. [SoT: F1 BR-17; quyết định owner 2026-10-25]

## 3. Trạng thái màn

| Trạng thái | Xử lý |
| :--- | :--- |
| Một khối tải/lỗi | Skeleton/lỗi riêng, nút Thử lại đúng query |
| Chuỗi số liệu rỗng | Hiện 0 hoặc trục thời gian rỗng theo hợp đồng; không giữ số mock |
| Cổng chéo F1 lỗi | Chỉ endpoint của khối cần cổng đó hiện lỗi; các khối khác vẫn hiển thị |
| 401/403 | Đưa tới đăng nhập / không đủ quyền A3 |

## 4. Quy tắc đặc thù màn

### BR-SC-01 Số liệu và cửa sổ thời gian

“Đang hoạt động” dùng người có refresh token phát hành trong 30 ngày gần nhất từ F1 #41, không đọc `last_active_at` hoặc đếm 24 giờ. Các chuỗi verdict chỉ dùng lượt đã có kết quả cuối, có mẫu số rõ và nhãn văn bản; độ khó theo thứ tự `problem_levels.sort_order` và dùng mã mức từ server, không giả định cố định ba mức. Số của #43 có thể trễ theo cổng phụ thuộc; không ghép tổng từ các response tải khác thời điểm để dựng một số “chính xác” mới ở client. [SoT: BD ADM0101 Sheet 4, Q; F1 §0.8, BR-17]

## 5. Kiểm tra phía client

Không có form. Giá trị nhận từ server được kiểm DTO; số âm/NaN hoặc thiếu chuỗi bắt buộc là lỗi dữ liệu của đúng khối, không đổi thành 0. Ngày biểu đồ hiển thị theo khoảng server trả, không dùng ngày local để tự thêm mốc.

## 6. i18n và truy cập

Dùng `adminOverview.*`, `apiError.IDT-*`. Mỗi biểu đồ có tiêu đề, đơn vị, văn bản tóm tắt và bảng dữ liệu truy cập được bằng bàn phím. Delta có dấu và mốc so sánh bằng chữ, không chỉ màu/mũi tên. Nút tải lại có tên khối cụ thể. Footer theo `02-bd/screens/admin/_shell.md` hiện ở trang này.

## 7. Điểm kiểm thử

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| ADM-O-01 | Normal | EVT-1 ba query cung cấp mọi thẻ/biểu đồ, mức độ khó động | Vitest | TDD màn `O-AC-01` |
| ADM-O-02 | Abnormal | #43 lỗi không che #41/#42; EVT-2 chỉ thử lại đúng khối | Vitest | TDD màn `O-AC-01` |
| ADM-O-03 | Boundary | Không có verdict cuối: tỷ lệ không chia cho 0, không dùng số mock | Vitest | TDD màn `O-AC-02` |
| ADM-O-04 | Normal | EVT-3/4 điều hướng đúng, footer vẫn hiện | Playwright | TDD màn `O-AC-03` |

## 8. Nợ prototype và câu hỏi mở

Prototype ở `views/admin/overview/api` còn dữ liệu mẫu. Nối mười endpoint F1 #41–43 và #67–73, bỏ phép suy số từ seed và dùng danh mục độ khó động. Mỗi khối tải/thử lại riêng theo quyết định owner 2026-10-25. Nợ `PROTOTYPE_DEBT.md` chỉ gỡ khi nối thật. TDD màn đã có tại `04-tdd/screens/admin/ADM0101_overview.md`.
