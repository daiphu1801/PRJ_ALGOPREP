# DD màn hình — Sự kiện hạ tầng (`ADM0404`)

## 0. Thông tin tài liệu

| Mục | Giá trị |
| :--- | :--- |
| Actor, route | A3 `ADMIN`; `/admin/infra-events`; `?level=error` là lối vào từ thẻ cảnh báo |
| Nguồn | `01-rd/screens/admin/ADM0404_infra_events.md`; `02-bd/screens/admin/ADM0404_infra_events.md` V0.5; `03-dd/api/F4_judge-orchestration.md` §22 |
| Prototype | `05-coding/frontend/src/views/admin/infra-events/` |
| Phiên bản | V0.1, 2026-10-25; DD trục màn, Mode B (agent-native) |

Màn chỉ đọc sự kiện máy từ F4. Nhật ký thao tác của người ở `ADM0403`; link tới đó là điều hướng, không ghép hai loại dữ liệu. [SoT: BD ADM0404 Sheet 4; F4 §22]

## 1. Bản đồ slice FSD

| Lớp | Đường dẫn từ `src/` | Vai trò |
| :--- | :--- | :--- |
| app | `app/(admin)/admin/infra-events/page.tsx` | Đọc `level` đầu vào, chỉ nhận `error|warn|info` |
| views | `views/admin/infra-events/ui/admin-infra-events-view.tsx` | Header, thanh lọc, timeline, nút Làm mới/Tải thêm, hai liên kết |
| entities | `entities/infra-event/api/queries.ts`, `model/types.ts` | Gọi F4 §22 bằng query key gồm toàn bộ bộ lọc và cursor; bỏ mock tải toàn bộ rồi lọc client |
| shared | `FilterBar`, `FilterMenu`, `QueryBoundary`, `EmptyState`, `ErrorState`, `Card`, toast | Dùng lại, không thêm component chỉ cho màn này vào `shared` |

`AdminInfraEventsView` không nhận mảng toàn bộ sự kiện từ ngoài. View sở hữu bộ lọc hiện hành và trang đã nạp; entity sở hữu request/response. `initialLevel` lấy từ URL đã chuẩn hoá. [SoT: `admin-infra-events-view.tsx:58-119`; BD Sheet 4.5]

## 2. API tiêu thụ

| EVT | Endpoint | Thao tác |
| :--- | :--- | :--- |
| 1 mở màn | F4 #20 `GET /admin/infra-events` (§22) | Tải trang đầu 20 dòng với `level` URL nếu hợp lệ |
| 2 đổi bộ lọc/làm mới | Cùng GET | Đổi lọc đặt lại cursor và các dòng cũ; Làm mới gọi lại đúng bộ lọc hiện hành |
| 3 khoảng ngày | Cùng GET khi hợp lệ | Chỉ gửi `from <= to`; sai thì cảnh báo tại client |
| 4 Tải thêm | Cùng GET kèm `nextCursor` | Ghép trang kế sau trang hiện có, không ghép khi cursor null |
| 5 Về hàng đợi; link Nhật ký hệ thống | Không gọi API | `/admin/queue`, `/admin/system-log` |

Không có polling hoặc SSE. F4 §22 trả `services` cùng trang, cho bộ chọn dịch vụ; chỉ có `go-judge`, `outbox`. Mức `info` hiện là bộ lọc hợp lệ nhưng trả rỗng theo nguồn sự kiện đã chốt.

## 3. Trạng thái màn

| Điều kiện | Hành vi |
| :--- | :--- |
| Trang đầu đang tải | Skeleton timeline, giữ bộ lọc hiện tại |
| Trang đầu rỗng | `EmptyState` phù hợp bộ lọc; `level=info` cũng là rỗng hợp lệ |
| Trang đầu `403 JDG-301` | Không đủ quyền, không hiện dòng sự kiện |
| Trang đầu lỗi mạng/5xx | `ErrorState` với thử lại trang đầu |
| Khoảng ngày sai | Giữ các dòng trước khi sửa, viền ô sai/toast; không gửi GET mới, không báo rỗng giả |
| Tải thêm đang chạy | Giữ 20n dòng đã có, khoá nút Tải thêm để không lặp cursor |
| Tải thêm lỗi | Giữ danh sách đã có, hiện thử lại trang kế cùng cursor |
| Làm mới lỗi | Giữ danh sách đang xem, báo lỗi; không toast “đã làm mới” |
| `nextCursor=null` | Ẩn Tải thêm; số dòng hiện lấy từ mảng đã nạp, không có `total` toàn bộ trong F4 §22 |

## 4. Quy tắc đặc thù màn

### BR-SC-01 Bộ lọc và phân trang con trỏ

`q` cắt ở 100 ký tự, tìm không phân biệt hoa thường do F4 làm; `level` chỉ `error|warn|info`, `service` chỉ `go-judge|outbox`, ngày `yyyy-MM-dd` và khoảng đóng hai đầu UTC+7. Mọi đổi lọc huỷ yêu cầu cũ nếu có thể, xoá trang đã nạp và `nextCursor`, rồi tải trang 1. Truy vấn trang sau dùng đúng bộ lọc đã sinh cursor; response cũ sau khi đổi lọc bị bỏ. Ghép sự kiện theo thứ tự trả về, không sort lại ở client; vì DTO không có `eventId`, không tự suy ra khoá bền từ `message`. Danh sách vẽ key theo `(occurredAt, service, message, thứ tự trong trang)` trong một lượt tải, không dùng key đó cho nghiệp vụ. [SoT: F4 §22.2–22.5]

### BR-SC-02 Làm mới thủ công

Nút Làm mới bỏ các trang cũ và gọi lại trang đầu theo bộ lọc hiện hành. Chỉ toast thành công khi GET thành công; không có đồng hồ tự tải. Nút “Về hàng đợi chấm” và link “Nhật ký hệ thống” là liên kết bình thường. [SoT: BD Q1, Sheet 8 EVT-2, EVT-5]

## 5. Kiểm tra phía client

Màn không có form ghi. Schema bộ lọc: `q` chuỗi cắt tối đa 100; `level` enum `error|warn|info` hoặc vắng; `service` enum `go-judge|outbox` hoặc vắng; `from`, `to` là ngày thực dạng `yyyy-MM-dd`, `from <= to`; `cursor` chỉ lấy từ `nextCursor` của response. Khoảng sai không gửi lời gọi, máy chủ vẫn kiểm `JDG-V13`. Giá trị URL `level` lạ bị bỏ qua, không toast, khớp F4 §22.2. [SoT: `03-dd/validation/F4_judge-orchestration.md` §6; BD Sheet 9]

## 6. i18n và truy cập

Dùng `adminInfraEvents.*` và `apiError.JDG-*`; `ERROR`, `WARN`, `INFO`, `go-judge`, `outbox` giữ nguyên thuật ngữ. Timeline có danh sách ngữ nghĩa, mỗi dòng đọc thời điểm, mức, dịch vụ, nội dung bằng chữ; màu viền không là kênh duy nhất. Nút Làm mới/Tải thêm có trạng thái `aria-busy`; dòng thêm thông báo qua `aria-live=polite` bằng số dòng mới, không đọc lại cả danh sách. Nội dung `message` từ F4 hiển thị như văn bản, không HTML. Lỗi ngày gắn `aria-invalid` và focus ô “Đến ngày”.

## 7. Điểm kiểm thử

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| IE-01 | Normal | `?level=error` mở trang đầu đúng bộ lọc; URL lạ về Tất cả | Vitest | TDD màn `IE-AC-01` |
| IE-02 | Normal | Đổi `q`, level, service, ngày đặt lại cursor và bỏ response cũ | Vitest | TDD màn `IE-AC-01` |
| IE-03 | Boundary | `from > to` không gọi F4, viền ô và cảnh báo đúng | Vitest | TDD màn `IE-AC-02` |
| IE-04 | Normal | Tải thêm theo `nextCursor`, đúng thứ tự, null thì hết | Vitest | TDD màn `IE-AC-01` |
| IE-05 | Abnormal | Trang kế lỗi giữ dòng cũ; Làm mới lỗi không báo thành công | Vitest | TDD màn `IE-AC-02` |
| IE-06 | Security | Thiếu `JUDGE_QUEUE_MONITOR:READ` không thấy dữ liệu | Playwright | TDD màn `IE-AC-03` |
| IE-07 | Normal | Link Hàng đợi và Nhật ký hệ thống đi đúng route; không có polling | Playwright | TDD màn `IE-AC-03` |

## 8. Nợ prototype và câu hỏi mở

Prototype `entities/infra-event` tải danh sách mock rồi lọc/phân trang trong view; nối F4 §22 cần chuyển `q`, `level`, `service`, `from`, `to`, `cursor` vào query server, không lấy `filtered.length` làm tổng toàn bộ. Nút Làm mới hiện toast thành công ngay khi gọi `refetch()` chưa chờ kết quả; DD yêu cầu chờ thành công. Dòng nợ ở `06-plan/PROTOTYPE_DEBT.md` chỉ gỡ sau khi nối API thật.

| # | Câu hỏi | Kết luận DD |
| :--- | :--- | :--- |
| OQ-1 | Nguồn `INFO` và danh sách dịch vụ | F4 §22 đã chốt hai nguồn `go-judge`/`outbox`; `info` rỗng hợp lệ |
| OQ-2 | Làm mới tự động hay thủ công | Thủ công theo BD Q1; không polling |
| OQ-3 | Số tổng để hiện “x / y” trên mọi trang | F4 chỉ có `nextCursor`, không có tổng; dùng “Đã tải {n} sự kiện” và nút Tải thêm, đồng bộ nhãn BD khi rà màn |

TDD màn đã có tại `04-tdd/screens/admin/ADM0404_infra_events.md`.
