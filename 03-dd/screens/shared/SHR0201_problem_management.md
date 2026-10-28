# DD màn hình — Quản lý bài tập (`SHR0201`)

## 0. Thông tin tài liệu

| Mục | Giá trị |
| :--- | :--- |
| Actor, route | A2/A3; `/instructor/problems`, `/admin/problems` |
| Nguồn | `01-rd/screens/shared/SHR0201_problem_management.md`; `02-bd/screens/shared/SHR0201_problem_management.md` Sheet 1–9; [F2](../../api/F2_problem-bank.md) §15–33, §35 |
| Prototype | `05-coding/frontend/src/views/shared/problem-management/ui/problem-management-view.tsx` |
| Phiên bản | V0.1, 2026-10-25; DD màn, Mode B |

## 1. Bản đồ slice FSD

| Lớp | Đường dẫn từ `src/` | Vai trò |
| :--- | :--- | :--- |
| app | `app/(admin)/admin/problems/page.tsx`, `(instructor)/instructor/problems/page.tsx` | Truyền `basePath` và quyền danh mục |
| views | `views/shared/problem-management/ui/` | Danh sách, lọc, thao tác gộp, popup danh mục |
| features | `features/manage-problems/` | Lựa chọn dòng, xác nhận thao tác gộp, tải CSV |
| entities | `entities/problem/api/queries.ts`, `mutations.ts` | F2 §15–33, §35 |
| shared | `DataTable`, `ManagedListDialog`, `ConfirmDialog`, `ErrorState` | Dùng chung A2/A3 |

## 2. API tiêu thụ

| EVT BD | API | Hành vi màn |
| :--- | :--- | :--- |
| 1–8, 32 | F2 §25 `GET /authoring/problems`, §26 phân bố chủ đề, §27 bài cần chú ý, §15 mức, §20 chủ đề | Lọc/sắp/trang server; checkbox chỉ client |
| 9–10 | Không gọi API | Mở `new`, info, edit theo UUID và basePath |
| 11 | F2 §29 xuất bản, §30 rút xuống | Gửi tập ID đã chọn; hiển thị kết quả một phần |
| 12–13 | F2 §31 đổi độ khó, §32 gán chủ đề | Gửi tập ID và ID danh mục từ query |
| 14 | F2 §35 `GET /authoring/problems/{problemId}/duplicate-source` | Chỉ đúng một bài; chuyển `/new?from={id}` với form điền sẵn, chưa tạo bản ghi |
| 15 | F2 §28 `POST /authoring/problems/export` | CSV từ server, không xuất từ trang hiện tại |
| 16–19 | F2 §33 `POST /authoring/problems/bulk/delete` | Xoá mềm sau xác nhận |
| 20–24 | F2 §20–24 | A3 quản lý chủ đề |
| 26–30 | F2 §15–19 | A3 quản lý độ khó |

## 3. Trạng thái màn

| Trạng thái | Xử lý |
| :--- | :--- |
| Danh sách/khối phụ lỗi | Lỗi từng khối, không dùng store mock bù số |
| Bộ lọc rỗng | Phân biệt chưa có bài và không khớp bộ lọc, nút xoá lọc |
| Xoá/xuất bản một phần | Dùng danh sách thành công/thất bại từ response, giữ chọn ID thất bại |
| Danh mục đang dùng | Server từ chối xoá, popup giữ nguyên và nêu số bài tham chiếu |
| Không có quyền | A2 không có nút quản lý danh mục; API vẫn kiểm actor/quyền |

## 4. Quy tắc đặc thù màn

### BR-SC-01 Danh sách và hành động gộp

URL giữ từ khoá, mức, trạng thái, sort và trang; đổi lọc về trang đầu và bỏ chọn. “Chọn tất cả” chỉ trang hiện tại. Bấm dòng đổi chọn trừ vùng link/nút/checkbox. `problemId` UUID là khoá; mã/nhãn hiển thị không dùng làm route. Xoá là ẩn mềm `deleted=true`, không mất lịch sử nộp/lớp. Bài đã rút có thể vẫn nằm trong bài giao lớp. Mutation gộp chỉ gửi tập ID đang chọn và dùng kết quả từng ID từ server, không giả tất cả thành công. [SoT: BD Sheet 8, Q; F2 §25–33]

### BR-SC-02 Nhân bản và danh mục

Chỉ khi chọn đúng một bài mới bật Nhân bản; F2 §35 đọc nguồn cho form `SHR0202`, URL `/new?from={id}`; chưa `POST` tạo bài. Chủ đề/độ khó quản lý chỉ A3, `code` sinh phía server, `displayName` sửa không đổi code. Mức mặc định có màu theo UI, mức A3 thêm dùng màu trung tính; không có cột màu. Sau đổi/xoá danh mục thành công làm mới bảng, tab và popup. [SoT: BD Q, F2 §15–24, §35]

## 5. Kiểm tra phía client

| Trường | Quy tắc | Nguồn |
| :--- | :--- | :--- |
| `q`, tab, sort, page | Theo F2 §25; tham số URL lạ về mặc định | F2 §25, validation F2 |
| `ids` hành động gộp | Tập UUID không rỗng, không trùng; mức/chủ đề từ danh mục server | F2 §29–33 |
| Tên danh mục | Trim, không rỗng; server kiểm trùng và sinh code | F2 §16–19, §21–24 |

## 6. i18n và truy cập

Dùng `problemManagement.*`, `apiError.PRB-*`. Header sort có `aria-sort`, checkbox nêu “trang hiện tại”, badge trạng thái có chữ. Dialog nêu rõ số bài chịu tác động và trả focus. Kết quả một phần đọc bằng `role=alert`; tên/đề bài escape khi hiển thị.

## 7. Điểm kiểm thử

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| PM-01 | Normal | EVT-1–8/32 lọc/trang server, chọn trang hiện tại | Vitest | TDD màn `PM-AC-01` |
| PM-02 | Normal | EVT-9/10/14 route đúng; nhân bản chưa tạo bản ghi | Playwright | TDD màn `PM-AC-01` |
| PM-03 | Abnormal | EVT-11–13/16–19 kết quả một phần, giữ ID lỗi | Vitest | TDD màn `PM-AC-02` |
| PM-04 | Security | A2 không quản lý danh mục, A3 có quyền mới thao tác | Playwright | TDD màn `PM-AC-03` |
| PM-05 | Boundary | Xoá chủ đề/mức còn tham chiếu bị từ chối; mức cuối không xoá | Vitest | TDD màn `PM-AC-03` |
| PM-06 | Normal | EVT-15 CSV từ server gồm tập lọc, không chỉ trang hiện tại | Vitest | TDD màn `PM-AC-03` |

## 8. Nợ prototype và câu hỏi mở

Prototype còn mock store, phân trang/lọc cục bộ; nối F2 §25–33 và danh mục thật. Nhân bản theo Q đã chốt là mở form chưa lưu. Thẻ KPI đã bỏ theo quy ước chỉ một trang tổng quan. Q của BD về xoá bài có lịch sử đã chốt ẩn mềm; không còn quyết định màn chờ owner. Chỉ gỡ nợ `PROTOTYPE_DEBT.md` sau nối thật; TDD màn đã có tại `04-tdd/screens/shared/SHR0201_problem_management.md`.
