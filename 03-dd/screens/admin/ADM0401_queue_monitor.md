# DD màn hình — Giám sát hàng đợi (`ADM0401`)

## 0. Thông tin tài liệu

| Mục | Giá trị |
| :--- | :--- |
| Actor, route | A3 `ADMIN`; `/admin/queue-monitor` |
| Nguồn | `01-rd/screens/admin/ADM0401_queue_monitor.md`; `02-bd/screens/admin/ADM0401_queue_monitor.md` Sheet 1–9; [F4](../../api/F4_judge-orchestration.md) §16–21 |
| Prototype | `05-coding/frontend/src/views/admin/queue-monitor/ui/admin-queue-monitor-view.tsx` |
| Phiên bản | V0.1, 2026-10-25; DD màn, Mode B |

## 1. Bản đồ slice FSD

| Lớp | Đường dẫn từ `src/` | Vai trò |
| :--- | :--- | :--- |
| app | `app/(admin)/admin/queue-monitor/page.tsx` | Kiểm quyền A3 và gắn màn |
| views | `views/admin/queue-monitor/ui/admin-queue-monitor-view.tsx` | Ảnh chụp hàng đợi, bảng job, popup tham số |
| features | `features/manage-judge-queue/` | Tạm dừng, tự mở rộng, lưu tham số, xác nhận thao tác |
| entities | `entities/judge-queue/api/queries.ts`, `mutations.ts` | F4 §16–21; cache snapshot/jobs/settings riêng |
| shared | `Switch`, `Tabs`, `ParamsDialog`, `ErrorState` | Điều khiển có nhãn và focus đúng |

## 2. API tiêu thụ

| EVT BD | API | Hành vi màn |
| :--- | :--- | :--- |
| 1, 2 mở/làm mới | F4 §16 `GET /admin/queue/snapshot`, §17 `GET /admin/queue/jobs`, §18 `GET /admin/queue/infra-summary` | Tải riêng; làm mới thủ công invalidate cả ba |
| 3 tạm dừng tiêu thụ | F4 §19 `PUT /admin/queue/consumption` | Ghi theo trạng thái mong muốn; chờ phản hồi |
| 4 tự mở rộng | F4 §20 `PUT /admin/queue/autoscale` | Ghi theo trạng thái mong muốn; chờ phản hồi |
| 6 tab/trang job | F4 §17 | Dùng tab, trang, kích thước từ cấu hình §21 |
| 10, 11 tham số | F4 §21 `GET`, `PUT /admin/queue/settings` | Mở popup từ baseline server; lưu nguyên bộ sau kiểm |
| 12, 13 | Không gọi API | Đóng popup / mở `ADM0404` |

## 3. Trạng thái màn

| Trạng thái | Xử lý |
| :--- | :--- |
| Snapshot lỗi | Chỉ dải trạng thái/công tắc/độ trễ hiện lỗi và Thử lại; bảng job còn đọc được |
| Jobs rỗng | Dùng số đếm F4 §17 và trạng thái rỗng theo tab, không giữ dòng cũ |
| Infra summary lỗi | Thẻ liên kết vẫn mở được `ADM0404`, không hiện số giả |
| Mutation đang gửi | Khoá đúng công tắc/nút; không đổi UI trước phản hồi máy chủ |
| Mutation lỗi | Giữ trạng thái từ snapshot cũ, toast theo `error.code` `JDG-*` |
| Thiếu quyền ghi | Ẩn hoặc khoá điều khiển; server vẫn kiểm vai trò `ADMIN` |

## 4. Quy tắc đặc thù màn

### BR-SC-01 Công tắc quản trị

Mỗi công tắc gửi trạng thái đích và chỉ đổi sau F4 §19/20 thành công. Nếu snapshot mới đến lúc mutation đang chạy, vẫn lấy response mutation làm chuẩn rồi làm mới snapshot. Bật tự mở rộng khiến JOB-JD-05 điều chỉnh số RabbitMQ consumer 1–8 theo hàng đợi trong các chu kỳ 15 giây; response nút chỉ xác nhận cấu hình đã lưu, không cam kết scale xong ngay. Hai thao tác không làm thay đổi trần 8 (hằng số deployment, không có ô nhập). [SoT: BD ADM0401 Sheet 8, Q8; owner 2026-10-25]

### BR-SC-02 Job và tham số

Tab/trang job là trạng thái URL; chuyển tab về trang đầu. Số dòng/trang đọc `jobsPerPage` từ F4 §21, không dùng hằng số 20 khi admin đã đổi. Popup tham số có bốn số nguyên tối thiểu 1; mở lại thì lấy baseline mới nhất, Hủy bỏ bản nháp. Lưu thành công làm mới snapshot, jobs và settings; lỗi giữ popup cùng giá trị nhập. Thanh độ trễ dùng `latencyCapSeconds` từ settings để tính chiều rộng, kẹp 100%, vẫn hiện số giây thật khi vượt trần. [SoT: BD Q6–Q9; F4 §16–21]

### BR-SC-03 Khối worker không có nguồn chi tiết

Ẩn toàn bộ bảng worker, phụ đề số worker và chỉ báo từng worker ở mọi trạng thái. `QueueSnapshotDto` không có danh sách worker hay `WORKER_OVERLOADED`; dải trạng thái chỉ còn `PAUSED`, `BACKLOGGED`, `NORMAL`. Công tắc tự mở rộng và trần cấu hình vẫn có nguồn F4 và vẫn hoạt động. Số liệu job/độ trễ lấy từ F4 §16–17. Đây là chốt owner 2026-10-25 sau khi đối chiếu [metric go-judge](https://docs.goj.ac/api); không hiển thị dữ liệu cụm giả từ prototype.

## 5. Kiểm tra phía client

| Trường | Quy tắc | Nguồn |
| :--- | :--- | :--- |
| Bốn tham số popup | Số nguyên >= 1, rỗng/chữ/thập phân bị chặn; không đặt trần tuỳ ý | BD Q7–Q9, validation F4 |
| Tab/trang | Chỉ giá trị tab F4 §17; trang không âm | F4 §17 |

## 6. i18n và truy cập

Dùng `adminQueueMonitor.*`, `apiError.JDG-*`. Mỗi switch có nhãn đầy đủ và `aria-checked`; trạng thái hàng đợi có chữ ngoài màu. Tab job có `aria-selected`. Popup tham số giữ focus trong dialog, trả focus về nút Cấu hình. Thay đổi số job dùng `aria-live=polite` không đọc lại toàn bộ bảng.

## 7. Điểm kiểm thử

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| QMON-01 | Normal | EVT-1/2 tải/làm mới ba khối độc lập | Vitest | TDD màn `QM-AC-01` |
| QMON-02 | Abnormal | EVT-3/4 lỗi không đổi công tắc, không gửi lặp | Vitest | TDD màn `QM-AC-02` |
| QMON-03 | Normal | EVT-6 đổi tab/trang dùng số dòng cấu hình, không giữ job cũ | Vitest | TDD màn `QM-AC-01` |
| QMON-04 | Boundary | Bốn tham số chỉ nhận số nguyên >=1, không có trần mềm | Vitest | TDD màn `QM-AC-03` |
| QMON-05 | Normal | EVT-10/11/12 popup lưu, huỷ và trả focus | Playwright | TDD màn `QM-AC-03` |
| QMON-06 | Normal | EVT-13 mở màn sự kiện; không dựng cột ưu tiên/mã job | Playwright | TDD màn `QM-AC-03` |
| QMON-07 | Boundary | Không có bảng worker hay trạng thái `WORKER_OVERLOADED`; công tắc tự mở rộng và số liệu job vẫn hoạt động | Playwright | TDD màn `QM-AC-01`, `QM-AC-02` |

## 8. Nợ prototype và câu hỏi mở

Prototype vẫn đọc mock tại `views/admin/queue-monitor/api`; cần nối F4 và cấu hình thật. Bỏ các ô/cột “Ưu tiên”, “Mã job” theo BD Q4–Q5; danh sách sự kiện đã chuyển `ADM0404`, màn này chỉ có số và liên kết. Q1–Q9 BD đã đóng; không còn quyết định màn chờ chủ dự án. Nợ `PROTOTYPE_DEBT.md` chỉ gỡ sau khi nối API. TDD màn đã có tại `04-tdd/screens/admin/ADM0401_queue_monitor.md`.
