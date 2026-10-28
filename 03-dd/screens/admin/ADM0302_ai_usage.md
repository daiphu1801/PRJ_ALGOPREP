# DD màn hình — Thống kê sử dụng AI (`ADM0302`)

## 0. Thông tin tài liệu

| Mục | Giá trị |
| :--- | :--- |
| Actor, route | A3 `ADMIN`; `/admin/ai-usage` |
| Nguồn | `01-rd/screens/admin/ADM0302_ai_usage.md`; `02-bd/screens/admin/ADM0302_ai_usage.md` Sheet 1–9; [F5](../../api/F5_ai-review.md) §18, §27–31 |
| Prototype | `05-coding/frontend/src/views/admin/ai-usage/ui/admin-ai-usage-view.tsx` |
| Phiên bản | V0.1, 2026-10-25; DD màn, Mode B |

## 1. Bản đồ slice FSD

| Lớp | Đường dẫn từ `src/` | Vai trò |
| :--- | :--- | :--- |
| app | `app/(admin)/admin/ai-usage/page.tsx` | Gắn view và kiểm quyền A3 |
| views | `views/admin/ai-usage/ui/admin-ai-usage-view.tsx` | Bố cục, khoảng 7/14/30 ngày, trạng thái từng khối |
| features | `features/review-ai-usage/` | Đánh dấu cảnh báo đã xem, đổi kỳ xem |
| entities | `entities/ai-usage/api/queries.ts`, `mutations.ts` | Query F5, cache theo kỳ; mutation cảnh báo |
| shared | `Card`, `Tabs`, biểu đồ, `ErrorState`, `toast` | Thành phần dùng lại; mọi số có nhãn văn bản |

## 2. API tiêu thụ

| EVT BD | API | Hành vi màn |
| :--- | :--- | :--- |
| 1 mở màn, 2 đổi kỳ | F5 §27 `GET /admin/usage/daily`, §18 `GET /admin/token-budget`, §28 `GET /admin/usage/alerts`, §30 `GET /admin/usage/top-users`, §31 `GET /admin/usage/top-problems` | Năm query độc lập; kỳ chỉ áp vào hợp đồng có tham số kỳ, không giả đổi số ngân sách tháng |
| 5 đánh dấu đã xem | F5 §29 `POST /admin/usage/alerts/{alertId}/review` | Chỉ cảnh báo bất thường có thao tác này; làm mới cảnh báo sau thành công |
| 6, 8 điều hướng | Không gọi API | Mở `ADM0303`, `SHR0201` |
| 9 thử lại | Endpoint của khối lỗi | Chỉ làm mới khối đó |

EVT-3 và EVT-7 đã bỏ ở BD. Không dựng lại xuất báo cáo hoặc liên kết không có đích.

## 3. Trạng thái màn

| Trạng thái | Xử lý |
| :--- | :--- |
| Một query đang tải/lỗi | Skeleton/lỗi trong đúng khối, khối khác vẫn đọc được; `error.code` ánh xạ `apiError.AIR-*` |
| Chuỗi ngày rỗng | Trục thời gian theo kỳ đã chọn, giá trị 0 và mô tả chưa có sử dụng |
| Thiếu đơn giá model | Ô chi phí `-`; không suy giá từ token |
| Không có cảnh báo | Trạng thái rỗng, không dựng thẻ mẫu “Prompt sinh testcase tốn hơn dự kiến” |
| Mutation đang gửi/lỗi | Vô hiệu đúng nút cảnh báo, lỗi giữ cảnh báo chưa xem |
| 401/403 | Đưa về đăng nhập / trang không đủ quyền theo khung A3 |

## 4. Quy tắc đặc thù màn

### BR-SC-01 Kỳ xem và ngân sách

Giữ kỳ 7/14/30 ngày trong URL `days`, mặc định 7; tham số lạ quay về 7. Đổi kỳ huỷ hoặc bỏ kết quả query cũ để phản hồi đến muộn không thay biểu đồ mới. Khối ngân sách hiển thị cửa sổ tháng hiện tại từ F5 §18, không tính lại theo `days`. [SoT: BD ADM0302 Sheet 4, 8; F5 §18, §27]

### BR-SC-02 Cảnh báo và dữ liệu xếp hạng

Chỉ cho đánh dấu đã xem cảnh báo bất thường có `alertId` và trạng thái cho phép; cảnh báo ngân sách chỉ dẫn tới Hạn mức AI. Sau F5 §29 thành công mới cập nhật badge/danh sách. Hai bảng xếp hạng dùng dữ liệu tên do F5 trả, không gọi F1/F2 từng dòng; tên và đề bài là dữ liệu, escape khi hiển thị. Bài toán chưa có giá hiện `-`; chấm câu luyện và phỏng vấn không ghép vào bảng bài toán. Phụ đề bảng này là “Phân tích bài giải và sinh testcase trong 30 ngày” vì F5 §31 chỉ gom hai loại lượt có `problemId`. [SoT: BD Q2, Q3, Q5, Q6; F5 §28–31]

## 5. Kiểm tra phía client

| Trường | Quy tắc | Nguồn |
| :--- | :--- | :--- |
| `days` | Chỉ 7, 14, 30; chọn bằng nút, không có ô nhập tự do | BD Sheet 5–6, F5 §27 |
| `alertId` | Chỉ lấy từ item đã tải, server kiểm lại quyền/trạng thái | F5 §29, validation F5 |

Không có form nhập khác. Tiền USD và token định dạng khi hiển thị; request không gửi chuỗi đã định dạng.

## 6. i18n và truy cập

Dùng `adminAiUsage.*`, `apiError.AIR-*`. Tab kỳ có `aria-selected`, nhãn biểu đồ chứa đơn vị và khoảng ngày. Cảnh báo đọc được theo thứ tự thời gian, nút “Đánh dấu đã xem” có tên riêng từng cảnh báo. Sau mutation, dùng `aria-live=polite` báo thành công; lỗi dùng `role=alert`. Không dùng màu thanh ngân sách làm thông tin duy nhất.

## 7. Điểm kiểm thử

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| AI-U-01 | Normal | EVT-1 tải năm khối; một khối lỗi không che bốn khối kia | Vitest | TDD màn `AIU-AC-01` |
| AI-U-02 | Boundary | EVT-2 đổi kỳ nhanh; phản hồi cũ không ghi đè kỳ mới, ngân sách vẫn theo tháng | Vitest | TDD màn `AIU-AC-01` |
| AI-U-03 | Normal | EVT-5 đánh dấu cảnh báo bất thường sau phản hồi thành công | Vitest | TDD màn `AIU-AC-02` |
| AI-U-04 | Abnormal | F5 §29 lỗi giữ trạng thái chưa xem | Vitest | TDD màn `AIU-AC-02` |
| AI-U-05 | Normal | EVT-6/8 điều hướng đúng đích; EVT-9 thử lại riêng khối | Playwright | TDD màn `AIU-AC-01` |
| AI-U-06 | Boundary | Thiếu giá model hiện `-`, bảng không ghép sai lượt chấm câu luyện | Vitest | TDD màn `AIU-AC-02` |

## 8. Nợ prototype và câu hỏi mở

Prototype đọc mock từ `views/admin/ai-usage/api`; chuyển sang F5 query/mutation và phân quyền thật khi nối API. Hai hành động “Xuất báo cáo” và “Xem tất cả người học” đã bị BD loại; không thêm endpoint. Các Q1–Q8 ở BD đã có kết luận, Q6 được DD F5 §30–31 giải bằng cổng tổng hợp; không còn quyết định chủ dự án ở màn này. Dòng nợ `PROTOTYPE_DEBT.md` chỉ đóng sau khi nối API thật. TDD màn đã có tại `04-tdd/screens/admin/ADM0302_ai_usage.md`.
