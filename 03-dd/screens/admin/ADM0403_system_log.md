# DD màn hình — Nhật ký hệ thống (`ADM0403`)

## 0. Thông tin tài liệu

| Mục | Giá trị |
| :--- | :--- |
| Actor, route | A3 `ADMIN`; `/admin/system-log` |
| Nguồn | `01-rd/screens/admin/ADM0403_system_log.md`; `02-bd/screens/admin/ADM0403_system_log.md` Sheet 1–9; [F1](../../api/F1_identity.md) §0.7, BR-16/19/20 |
| Prototype | `05-coding/frontend/src/views/admin/system-log/ui/admin-system-log-view.tsx` |
| Phiên bản | V0.2, 2026-11-02 (nối API thật, task #12); V0.1, 2026-10-25; DD màn, Mode B |

## 1. Bản đồ slice FSD

| Lớp | Đường dẫn từ `src/` | Vai trò |
| :--- | :--- | :--- |
| app | `app/(admin)/admin/system-log/page.tsx` | Actor A3; đọc log không phụ thuộc ô `SYSTEM_AUDIT_LOG` |
| views | `views/admin/system-log/ui/` | Danh sách, biểu đồ, popup CSV/lưu trữ |
| features | `features/search-audit-logs/`, `features/manage-log-retention/` | Bộ lọc, tải tiếp, xuất, lưu cấu hình |
| entities | `entities/audit-log/api/queries.ts`, `mutations.ts` | F1 #39–40, #51–52; SSE nếu API F1 chốt kênh |
| shared | `DateRange`, `ParamsDialog`, `ErrorState`, `DataTable` | Không render JSON log thành HTML thô |

## 2. API tiêu thụ

| EVT BD | API | Hành vi màn |
| :--- | :--- | :--- |
| 1–5, 8, 11 | F1 #39 `GET /admin/audit-logs`, #64 `GET /admin/audit-logs/active-admins`, #65 `GET /admin/audit-logs/category-breakdown` | Bộ lọc từ khoá/nhóm/ngày, cursor tải thêm 50; hai khối phụ có nguồn riêng |
| 6–7 realtime | F1 #66 `GET /admin/audit-logs/stream` SSE | `Last-Event-ID`, sự kiện `resync` làm mới trang đầu |
| 9, 12–13 | F1 #40 `GET /admin/audit-logs/export` | Xuất CSV streaming theo bộ lọc chụp khi mở popup, cột đã chọn |
| 10 | Không gọi API | Mở `/admin/queue-monitor` |
| 14–16 | F1 #51 `GET`, #52 `PUT /admin/settings/log-retention` | Popup lưu trữ, xác nhận khi giảm hạn hoặc chuyển DELETE |

## 3. Trạng thái màn

| Trạng thái | Xử lý |
| :--- | :--- |
| Danh sách/trang tiếp lỗi | Giữ các dòng đã tải, báo lỗi cuối danh sách và Thử lại đúng cursor |
| Lọc rỗng | Nút Xoá bộ lọc; không hiện dòng từ query trước |
| Stream mất kết nối | Báo “đang nối lại”; không cộng dòng trùng ID, sau nối lại làm mới trang đầu |
| Xuất CSV lỗi | Giữ dialog/bộ lọc, không tải tệp rỗng như thành công |
| Cấu hình lỗi | Giữ draft trong popup, baseline server không đổi |
| Thiếu `SYSTEM_AUDIT_LOG` | Vẫn đọc/xuất log; chỉ vô hiệu ghi #52 |

## 4. Quy tắc đặc thù màn

### BR-SC-01 Lọc, cursor và stream

Bộ lọc từ khoá/nhóm/ngày đặt ở URL; đổi lọc xoá trang/cursor cũ. Khoảng ngày đóng hai đầu, không vượt thời hạn lưu hiện hành và không chọn tương lai; ngày bằng nhau hợp lệ. Tải thêm theo cursor F1 #39, khử trùng bằng UUID nếu sự kiện mới đẩy vào cùng lúc. Stream chỉ thêm dòng phù hợp bộ lọc, giữ tối đa số dòng đang xem; mất kết nối làm mới query để bù khoảng trống, không suy thứ tự chỉ bằng thời gian client. Mã hiển thị là 7 ký tự đầu UUID, khoá thật vẫn UUID. [SoT: BD Q3/Q5/Q6, Sheet 8]

### BR-SC-02 Xuất và lưu trữ

Popup xuất chụp từ khoá/tab/ngày lúc mở; ít nhất một trong sáu cột được chọn. CSV do server stream và che dữ liệu nhạy cảm trước khi trả; client không tự xuất các dòng đã tải. #52 chỉ gọi khi xác nhận; nếu giảm `retentionDays` hoặc chuyển `ARCHIVE` sang `DELETE`, hỏi lại lần hai. `ARCHIVE` hiện chọn được nhưng job chưa nén/lưu, giữ log quá hạn theo F1 BR-20; UI phải giải thích tình trạng này, tránh hứa có kho lưu trữ. Không có xoá tay dòng log. [SoT: BD Q2/Q4/Q7/Q8; F1 §0.7]

## 5. Kiểm tra phía client

| Trường | Quy tắc | Nguồn |
| :--- | :--- | :--- |
| `q` | Tối đa 100 ký tự | BD Sheet 9 NO 3 |
| `from`, `to` | Ngày hợp lệ, `from <= to <= hôm nay`, không trước cửa sổ `retentionDays` | BD Sheet 9 NO 4–6; F1 #39–40 |
| Cột CSV | Ít nhất một cột trong tập sáu cột F1 #40 | BD Sheet 9 NO 10 |
| `retentionDays` | Số nguyên >=1, trong kiểu server | F1 #52, `IDT-442` |
| `expiredPolicy` | `DELETE` hoặc `ARCHIVE` | F1 #52 |

## 6. i18n và truy cập

Dùng `adminSystemLog.*`, `apiError.IDT-*`. Mỗi dòng thời gian đọc theo UTC+7 có timezone rõ. Nội dung dựng từ `action_type` → khoá i18n và tham số đã server che; client escape mọi tham số. Không hiện before/after JSON thô. Stream thêm dòng công bố qua `aria-live=polite` theo lô, không đọc 50 dòng lại. Dialog xuất/lưu trữ giữ focus, trả focus khi đóng; lỗi gắn trường ngày/cấu hình tương ứng.

## 7. Điểm kiểm thử

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| LOG-01 | Normal | EVT-1–5/8 lọc server, cursor 50, đổi bộ lọc bỏ cursor cũ | Vitest | TDD màn `LOG-AC-01` |
| LOG-02 | Boundary | Khoảng ngược/quá hạn/tương lai bị chặn cả tìm và xuất | Vitest | TDD màn `LOG-AC-02` |
| LOG-03 | Abnormal | Stream trùng/mất kết nối không mất/nhân đôi dòng | Vitest | TDD màn `LOG-AC-01` |
| LOG-04 | Security | CSV và UI không lộ OTP/token/khoá, A3 đọc không cần quyền sửa | Playwright | TDD màn `LOG-AC-02` |
| LOG-05 | Normal | EVT-9/12/13 chụp filter, chọn cột và tải CSV | Playwright | TDD màn `LOG-AC-02` |
| LOG-06 | Boundary | EVT-14–16 giảm hạn hoặc DELETE hỏi lại, ARCHIVE giải thích trạng thái | Playwright | TDD màn `LOG-AC-03` |

**Triển khai 2026-11-02 (task #12, lát 3).** Màn nối API thật ở chế độ `NEXT_PUBLIC_MOCK_DATA=false`; chế độ mock giữ nguyên và dùng cùng một đường mã (mock cũng lọc và phân trang theo con trỏ).
- **Vị trí mã.** Giữ trong `views/admin/system-log/api/` (tiêu chí ponytail bậc 4: chưa có màn thứ hai dùng), không tách `entities/audit-log` và `features/*` như bảng mục 1. Gồm `real-audit-log.ts` (gọi #39, #40, #64, #65, #51, #52 và ánh xạ DTO), `queries.ts` (`useAuditLog` là truy vấn vô hạn theo con trỏ), `mutations.ts` (`useSaveLogRetention`, `useExportAuditLog`), `live.ts` (`useAuditLive`), `model/filters.ts` (`matchesFilters`). Phần dùng chung đưa lên `shared/`: `shared/api/sse-stream.ts` (SSE qua `fetch`), `api.download` và `api.put` ở `shared/api/client.ts`, `shared/lib/save-blob.ts`, `shared/lib/use-debounced.ts`, và `ParamsDialog` nhận `onSave` trả về promise (lỗi thì giữ hộp thoại và bản nháp).
- **Lọc và phân trang ở máy chủ.** Từ khoá (chờ 300 ms sau khi ngừng gõ), phân loại, khoảng ngày gửi lên #39; "Tải thêm 50 dòng" gửi `nextCursor` của trang trước. Khoảng ngày ngược không gọi máy chủ và hiện trạng thái rỗng. Không có tổng số dòng (máy chủ không trả số giả), nên nhãn chân danh sách chỉ ghi số dòng đã tải. **Lệch BR-SC-01:** bộ lọc giữ trong trạng thái màn, không đặt ở URL (tiêu chí ponytail bậc 3 và bậc 1: không ai dùng liên kết lọc sẵn khi demo, không nguồn yêu cầu); nợ nếu sau này cần chia sẻ liên kết.
- **Câu nội dung.** Dựng từ `actionType` và `safeParams` bằng khoá `adminSystemLog.action.<TYPE>` (16 loại hiện có, vi và en); loại chưa có khoá hiện "Hành động <mã>". Mã hiển thị là 7 ký tự đầu UUID, khoá thật vẫn là UUID. Dòng khoá tài khoản (`USER_LOCKED`) hiện thêm lý do từ `safeParams.reason`. Giờ đọc theo UTC+7 cố định.
- **Luồng trực tiếp (#66).** Mở khi công tắc bật, bằng `fetch` kèm Bearer; tự nối lại có lùi thời gian, gửi `Last-Event-ID`, làm mới token một lần khi gặp 401. Dòng mới chỉ thêm vào đầu trang một khi khớp bộ lọc hiện tại (`matchesFilters` làm giống máy chủ: email người thực hiện, mã sự kiện theo tiền tố, tên dịch vụ) và khử trùng theo UUID. Sự kiện `resync`, hoặc nối lại thành công sau khi rớt, làm mới danh sách. Khi rớt hiện "Đang nối lại luồng trực tiếp"; bị từ chối (`IDT-447`, `IDT-449`) báo toast, không thử lại. Dòng mới được đọc qua `aria-live=polite` thành một câu tổng. **Chưa làm:** cắt số dòng tối đa đang xem (nợ, ponytail bậc 3), không có luồng ở chế độ mock.
- **Xuất CSV (#40).** Tệp do máy chủ stream; màn nhận về Blob rồi lưu, tên tệp `audit-logs-YYYYMMDD-HHmm.csv` do client đặt (giờ UTC+7). Gửi `Accept-Language` theo ngôn ngữ giao diện. Lỗi (`IDT-443`, `IDT-444`, `IDT-447`) giữ hộp thoại và bộ lọc, toast nêu lý do, không lưu tệp rỗng. Thời hạn lưu trong kiểm tra ngày lấy từ #51.
- **Lưu trữ (#51, #52).** Đọc khi mở màn, nhãn chân trang và hộp thoại dùng giá trị máy chủ; lưu xong màn chỉ hiện giá trị máy chủ trả về. Thiếu ô `SYSTEM_AUDIT_LOG:UPDATE` thì máy chủ trả `IDT-445`, màn toast và giữ hộp thoại (màn chưa biết quyền của người xem nên chưa vô hiệu nút trước, nợ). `ARCHIVE` chọn được kèm dòng giải thích chưa nén và chưa xoá (BR-SC-02).
- **Proxy.** Lớp rewrite của Next.js nén gzip cho trình duyệt và giữ sự kiện SSE trong bộ đệm nén; backend #66 nay trả `Cache-Control: no-store, no-transform` (DD api F1 mục 0.7.2).
- Kiểm chứng: `pnpm check` 129 file 737 test; `e2e/admin-system-log-real.spec.ts` trên backend và frontend thật.

## 8. Nợ prototype và câu hỏi mở

F1 #39–40, #64–66 hiện đã có contract cho tìm, xuất, hai khối phụ và stream. Prototype lọc ở client và stream giả phải thay khi nối API. Hành động quản lý người dùng thuộc tab Ma trận quyền theo quyết định owner 2026-10-25; F1 BR-19 đã đồng bộ. Q1–Q8 BD đã đóng. TDD màn đã có tại `04-tdd/screens/admin/ADM0403_system_log.md`.
