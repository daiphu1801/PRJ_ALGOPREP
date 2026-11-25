# DD màn hình — Giám sát hàng đợi (`ADM0401`)

## 0. Thông tin tài liệu

| Mục | Giá trị |
| :--- | :--- |
| Actor, route | A3 `ADMIN`; `/admin/queue-monitor` |
| Nguồn | `01-rd/screens/admin/ADM0401_queue_monitor.md`; `02-bd/screens/admin/ADM0401_queue_monitor.md` Sheet 1–9; [F4](../../api/F4_judge-orchestration.md) §16–21 |
| Prototype | `05-coding/frontend/src/views/admin/queue-monitor/ui/admin-queue-monitor-view.tsx` |
| Phiên bản | V0.2, 2026-11-22 (thêm khối Luồng chấm, thay BR-SC-03 và QMON-07); V0.1, 2026-10-25; DD màn, Mode B. **Khối Luồng chấm: đã dựng 2026-11-22** (`GradingCard`, `pnpm check` đạt; chưa xem trên màn hình thật bằng tài khoản quản trị) |

## 1. Bản đồ slice FSD

| Lớp | Đường dẫn từ `src/` | Vai trò |
| :--- | :--- | :--- |
| app | `app/(admin)/admin/queue-monitor/page.tsx` | Kiểm quyền A3 và gắn màn |
| views | `views/admin/queue-monitor/ui/admin-queue-monitor-view.tsx` | Ảnh chụp hàng đợi, bảng job, popup tham số |
| features | `features/manage-judge-queue/` | Tạm dừng, tự mở rộng, lưu tham số, xác nhận thao tác |
| entities | `entities/judge-queue/api/queries.ts`, `mutations.ts` | F4 §16–21; cache snapshot/jobs/settings riêng. Ánh xạ snapshot thật thêm `finishedLastMinute`, `estimatedClearSeconds`, `consumers` (F4 §16.2) |
| shared | `Switch`, `Tabs`, `ParamsDialog`, `ErrorState` | Điều khiển có nhãn và focus đúng |

## 2. API tiêu thụ

| EVT BD | API | Hành vi màn |
| :--- | :--- | :--- |
| 1, 2 mở/làm mới | F4 §16 `GET /admin/queue/snapshot` (kèm `consumers`, `finishedLastMinute`, `estimatedClearSeconds`), §17 `GET /admin/queue/jobs`, §18 `GET /admin/queue/infra-summary` | Tải riêng; làm mới thủ công invalidate cả ba |
| 3 làm tươi tự động, 15, 16 | Lô STOMP `/topic/admin/queue` (F4 §2.6) cùng hình dạng snapshot; REST §16 khi kênh rớt | Thay khối Luồng chấm tại chỗ; không gọi thêm API |
| 3 tạm dừng tiêu thụ | F4 §19 `PUT /admin/queue/consumption` | Ghi theo trạng thái mong muốn; chờ phản hồi |
| 4 tự mở rộng | F4 §20 `PUT /admin/queue/autoscale` | Ghi theo trạng thái mong muốn; chờ phản hồi |
| 6 tab/trang job | F4 §17 | Dùng tab, trang, kích thước từ cấu hình §21 |
| 10, 11 tham số | F4 §21 `GET`, `PUT /admin/queue/settings` | Mở popup từ baseline server; lưu nguyên bộ sau kiểm |
| 12, 13 | Không gọi API | Đóng popup / mở `ADM0404` |

## 3. Trạng thái màn

| Trạng thái | Xử lý |
| :--- | :--- |
| Snapshot lỗi | Dải trạng thái, khối Luồng chấm, công tắc và độ trễ cùng hiện lỗi và Thử lại (cùng một nguồn snapshot); bảng job còn đọc được |
| `consumers` khác `null` | Khối Luồng chấm hiện số đang chạy trên tối đa, mục tiêu, lần đổi gần nhất (BR-SC-03) |
| `consumers = null` (worker chưa báo hoặc báo cũ hơn 60 giây) | Khối Luồng chấm không dựng số luồng nào; hiện dòng "Chưa nhận được báo cáo từ worker chấm bài"; tốc độ chấm và dự kiến hết hàng đợi vẫn hiện (BR-SC-04) |
| `lastScaling = null` | Dòng lần đổi gần nhất hiện "Chưa có lần đổi nào" |
| `estimatedClearSeconds = null` | Hiện "Chưa đủ dữ liệu", không hiện 0 |
| Chế độ dữ liệu mẫu (`NEXT_PUBLIC_MOCK_DATA=true`) | Giữ danh sách worker minh hoạ cũ, không dựng khối Luồng chấm (BR-SC-05) |
| Jobs rỗng | Dùng số đếm F4 §17 và trạng thái rỗng theo tab, không giữ dòng cũ |
| Infra summary lỗi | Thẻ liên kết vẫn mở được `ADM0404`, không hiện số giả |
| Làm mới thủ công | Khóa nút Làm mới trong lúc tải. Lỗi một phần giữ các khối tải thành công và báo cập nhật một phần; toàn bộ thất bại báo lỗi. Chỉ báo hoàn tất khi tất cả truy vấn đang dùng thành công. |
| Đang tải bảng hoặc đổi bộ lọc/trang | Hiện trạng thái tải bảng. Số đếm hiện khi có kết quả của bộ lọc/trang hiện tại. |
| Mutation đang gửi | Khoá đúng công tắc/nút; không đổi UI trước phản hồi máy chủ |
| Mutation lỗi | Giữ trạng thái từ snapshot cũ, toast theo `error.code` `JDG-*` |
| Thiếu quyền ghi | Ẩn hoặc khoá điều khiển; server vẫn kiểm vai trò `ADMIN` |

## 4. Quy tắc đặc thù màn

### BR-SC-01 Công tắc quản trị

Các công tắc nằm trong mục Điều khiển có thể mở bằng bàn phím, đóng khi vào màn. Dải trạng thái nêu rõ khi hàng đợi đang tạm dừng. Mục Điều khiển cũng chứa mục tiêu và lần đổi số luồng gần nhất, khi có báo cáo worker.

Mỗi công tắc gửi trạng thái đích và chỉ đổi sau F4 §19/20 thành công. Nếu snapshot mới đến lúc mutation đang chạy, vẫn lấy response mutation làm chuẩn rồi làm mới snapshot. Bật tự mở rộng khiến JOB-JD-05 điều chỉnh số RabbitMQ consumer 1–8 theo hàng đợi trong các chu kỳ 15 giây; response nút chỉ xác nhận cấu hình đã lưu, không cam kết scale xong ngay. Hai thao tác không làm thay đổi trần 8 (hằng số deployment, không có ô nhập). [SoT: BD ADM0401 Sheet 8, Q8; owner 2026-10-25]

### BR-SC-02 Job và tham số

Tab/trang job là trạng thái URL; chuyển tab về trang đầu. Số dòng/trang đọc `jobsPerPage` từ F4 §21, không dùng hằng số 20 khi admin đã đổi. Popup tham số có bốn số nguyên tối thiểu 1; mở lại thì lấy baseline mới nhất, Hủy bỏ bản nháp. Lưu thành công làm mới snapshot, jobs và settings; lỗi giữ popup cùng giá trị nhập. Thanh độ trễ dùng `latencyCapSeconds` từ settings để tính chiều rộng, kẹp 100%, vẫn hiện số giây thật khi vượt trần. [SoT: BD Q6–Q9; F4 §16–21]

### BR-SC-03 Khối "Luồng chấm" (thay quy tắc "ẩn toàn bộ khối worker", 2026-11-22)

Quy tắc V0.1 ("ẩn toàn bộ bảng worker, phụ đề số worker ở mọi trạng thái") **không còn**; nó được thay theo `DEC-2026-1122-f4-queue-consumers-and-throughput-on-adm0401`. Hai điều vẫn giữ: `QueueSnapshotDto` không có danh sách worker và dải trạng thái chỉ còn `PAUSED`, `BACKLOGGED`, `NORMAL` (không `WORKER_OVERLOADED`).

Ở chế độ dữ liệu thật, khối "Luồng chấm" nằm ở chỗ của thẻ cụm worker cũ và **hiện khi `consumers` khác `null`**: `consumers.active / workerAutoscaleMax` (ví dụ "3 / 8"), `consumers.target`, và dòng lần đổi gần nhất ghép từ `lastScaling` (giờ `at` theo múi giờ người dùng, tăng hay giảm theo `to` so `from`, `reason` đổi sang nhãn "Hàng đợi dồn" / "Hàng đợi trống" / "Tắt tự động mở rộng", `pendingCount` job chờ). `lastScaling = null` thì dòng đó ghi "Chưa có lần đổi nào". Hai số `finishedLastMinute` ("{số} lượt/phút") và `estimatedClearSeconds` luôn hiện ở chế độ thật; `estimatedClearSeconds = null` thì hiện "Chưa đủ dữ liệu". Cách đổi giây sang phút của dự kiến hết hàng đợi: dưới 60 giây hiện "khoảng {s} giây", từ 60 giây hiện "khoảng {m} phút" với m làm tròn lên `[SoT: Suy luận]`, chờ chủ dự án xác nhận (BD Q11).

**Không hiển thị** ở chế độ thật: mức tải %, uptime, số vCPU, mã hay trạng thái từng worker; dữ liệu cụm giả từ prototype. **Không làm**: bấm dòng job để xem chi tiết (dòng job chỉ đọc), Prometheus, lịch sử nhiều lần đổi. Nguồn hợp đồng: F4 §16.2.

### BR-SC-04 Khi worker không báo (`consumers = null`)

`consumers = null` nghĩa là máy chủ chưa có báo cáo nào hoặc báo cáo cũ hơn 60 giây (so giờ DB, F4 §16.2). Màn **không** giữ số của lô trước, không suy diễn từ `autoscaleEnabled` hay `workerAutoscaleMax`, và không hiện "0 / 8": phần số luồng được thay hoàn toàn bằng dòng "Chưa nhận được báo cáo từ worker chấm bài" (nội dung trạng thái của trang, không phải toast). Tốc độ chấm và dự kiến hết hàng đợi vẫn hiện vì tính từ bài nộp (BD Q11, `[SoT: Suy luận]`). Khi lô sau có `consumers` khác `null` thì khối chuyển lại ngay, không cần tải lại trang. Phân biệt với "Dữ liệu cũ" của dải trạng thái: nhãn đó nói về việc **màn** không nhận được lô nào quá 60 giây (kênh hoặc máy chủ), còn `consumers = null` nói về việc **worker** không báo dù màn vẫn nhận lô đều.

### BR-SC-05 Chế độ dữ liệu mẫu giữ danh sách worker

Ở `NEXT_PUBLIC_MOCK_DATA=true`, màn vẫn dựng bảng worker minh hoạ cũ (mã, trạng thái, mức tải, uptime, vCPU, BD Sheet 5 Khu vực C NO 9 đến 17) từ `__mock__/judge-queue-mocks.ts` và **không** dựng khối "Luồng chấm"; đó là dữ liệu mẫu, không phải số liệu cụm thật, và không được lẫn vào ánh xạ snapshot thật. Giữ lại để demo khi chưa có go-judge thật (lựa chọn của chủ dự án 2026-10-11 chỉ còn áp cho chế độ mẫu). Hai chế độ không đồng thời hiện hai khối.

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
| QMON-07 | Boundary | Chế độ thật: không có bảng worker, mức tải %, uptime, vCPU hay trạng thái `WORKER_OVERLOADED`; dòng job không bấm được; công tắc tự mở rộng và số liệu job vẫn hoạt động | Playwright | TDD màn `QM-AC-01`, `QM-AC-02`, `QM-AC-07` |
| QMON-08 | Normal | `consumers` khác `null`: hiện "đang chạy / tối đa", mục tiêu và dòng lần đổi gần nhất (BR-SC-03) | Vitest | TDD màn `QM-AC-04` |
| QMON-09 | Normal | Lô mới có `lastScaling` tăng rồi giảm: nhãn tăng hoặc giảm, từ mấy lên mấy và lý do (`BACKLOG`, `IDLE`, `DISABLED`) hiển thị đúng | Vitest | TDD màn `QM-AC-04` |
| QMON-10 | Boundary | `consumers = null`: không có số luồng, hiện "Chưa nhận được báo cáo từ worker chấm bài", tốc độ chấm và dự kiến vẫn hiện; lô sau có `consumers` thì khối trở lại (BR-SC-04) | Vitest | TDD màn `QM-AC-05` |
| QMON-11 | Boundary | `lastScaling = null` hiện "Chưa có lần đổi nào"; `estimatedClearSeconds = null` hiện "Chưa đủ dữ liệu", không hiện 0 | Vitest | TDD màn `QM-AC-04`, `QM-AC-06` |
| QMON-12 | Normal | `finishedLastMinute` và `estimatedClearSeconds` hiện đúng đơn vị lượt/phút và giây hoặc phút; làm mới thủ công và lô tự động cập nhật tại chỗ | Vitest | TDD màn `QM-AC-06` |
| QMON-13 | Normal | Chế độ mẫu: giữ bảng worker minh hoạ, không có khối Luồng chấm (BR-SC-05) | Vitest | TDD màn `QM-AC-07` |

## 8. Nợ prototype và câu hỏi mở

Prototype vẫn đọc mock tại `views/admin/queue-monitor/api`; cần nối F4 và cấu hình thật. Bỏ các ô/cột “Ưu tiên”, “Mã job” theo BD Q4–Q5; danh sách sự kiện đã chuyển `ADM0404`, màn này chỉ có số và liên kết. Q1–Q9 BD đã đóng. Còn chờ chủ dự án xác nhận đề xuất: BD Q10 (không giữ lịch sử đổi số luồng), BD Q11 (khi worker im lặng vẫn hiện tốc độ chấm và dự kiến; cách làm tròn sang phút ở BR-SC-03). Khối Luồng chấm và ánh xạ `consumers` chưa dựng (2026-11-22); nối snapshot thật là nợ mới của `PROTOTYPE_DEBT.md`, lead cập nhật sau khi có mã. Nợ `PROTOTYPE_DEBT.md` chỉ gỡ sau khi nối API. TDD màn đã có tại `04-tdd/screens/admin/ADM0401_queue_monitor.md`.
