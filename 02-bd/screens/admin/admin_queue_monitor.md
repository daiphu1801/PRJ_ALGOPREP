# BD — Màn `admin_queue_monitor` (Hàng đợi chấm & cụm go-judge)

> Trục: **màn hình** (không phải Bounded Context). Slug khớp `01-rd/screens/admin/admin_queue_monitor.md`
> [SoT: 01-rd/screens/admin/admin_queue_monitor.md:1]. Actor: A3 (quản trị viên). Bounded Context sở hữu
> dữ liệu/API: `judge-orchestration` (F4) [SoT: 01-rd/screens/admin/admin_queue_monitor.md:4].
>
> Đọc cùng: `02-bd/architecture/judge-orchestration.md` (state machine, Outbox, ops controls),
> `02-bd/database/judge-orchestration.md` (bảng `submissions`, `language_configs`). File này **không lặp
> lại** nội dung hai file trên — chỉ mô tả layout, state, và API nào màn hình gọi.

## 1. Mục đích và phạm vi

Giám sát trực tiếp độ dài hàng đợi, tình trạng cụm worker go-judge, và job đang chờ/đang chấm
[SoT: 01-rd/screens/admin/admin_queue_monitor.md:15-17]. Đây là màn **chỉ đọc + hai điều khiển vận hành cơ
bản** (tạm dừng tiêu thụ hàng đợi, autoscale) — không phải màn quản lý kỳ thi, không có luồng chấm lại
[SoT: 01-rd/screens/admin/admin_queue_monitor.md:35-38, 65].

## 2. Layout regions

Đối chiếu `09-layoutBase/Admin - Hàng đợi chấm.dc.html`, sáu vùng theo thứ tự từ trên xuống:

1. **Thanh tiêu đề màn** — tên màn + phụ đề "Giám sát trực tiếp · làm mới mỗi 5 giây" + nút hành động
   "Làm mới" [SoT: 09-layoutBase/Admin - Hàng đợi chấm.dc.html:533-536, 148-159].
2. **Dải 4 chỉ số tổng (KPI)** — lưới tự co giãn, mỗi ô: nhãn, giá trị lớn, mô tả phụ
   [SoT: 09-layoutBase/Admin - Hàng đợi chấm.dc.html:162-174, 440-445].
3. **Hai cột chính**:
   - Cột trái (rộng hơn, tỉ lệ ~1.6): **Cụm worker go-judge** — lưới thẻ, mỗi thẻ một worker
     [SoT: 09-layoutBase/Admin - Hàng đợi chấm.dc.html:177-195, 452-462].
   - Cột phải: xếp dọc hai khối — **Điều khiển cụm** (danh sách toggle) và **Độ trễ theo hàng đợi** (danh
     sách thanh tiến trình) [SoT: 09-layoutBase/Admin - Hàng đợi chấm.dc.html:197-228, 470-481].
4. **Bảng job trong hàng đợi** — tiêu đề + bộ lọc phân đoạn (Tất cả/Đang chờ/Lỗi) + bảng 6 cột
   [SoT: 09-layoutBase/Admin - Hàng đợi chấm.dc.html:231-268, 483-516].
5. **Sự kiện hạ tầng gần đây** — danh sách log dạng dòng, mỗi dòng: giờ, mức độ, nội dung, dịch vụ nguồn
   [SoT: 09-layoutBase/Admin - Hàng đợi chấm.dc.html:270-291, 523-530].
6. **Footer** — phiên bản, trạng thái tổng quan dịch vụ, liên kết phụ (ngoài phạm vi nghiệp vụ màn)
   [SoT: 09-layoutBase/Admin - Hàng đợi chấm.dc.html:294-307].

Sidebar/topbar admin dùng chung khung điều hướng toàn hệ thống — không mô tả lại ở đây (thuộc BD khung
admin chung, chưa viết; mục "Việc còn mở").

## 3. Component inventory

| Vùng | Component | Mô tả hành vi (không phải hợp đồng API) |
| :--- | :--- | :--- |
| KPI | `QueueKpiCard` × 4 | Hiển thị: Job đang chờ, Đang chấm, Thông lượng (job/phút), Job lỗi 24 giờ. Giá trị và mô tả phụ đọc từ snapshot realtime |
| Cụm worker | `WorkerStatusCard` (lặp N worker) | Mỗi thẻ: mã worker, badge trạng thái (`Đang chạy`/`Nhàn rỗi`/`Quá tải`), thanh % tải, số job đang xử lý, uptime, số vCPU. Badge và màu thanh tải đổi khi tải > 85% [SoT: 01-rd/screens/admin/admin_queue_monitor.md:53-55] |
| Điều khiển cụm | `OpsToggle` × 2 | "Tạm dừng tiêu thụ hàng đợi" và "Tự động mở rộng worker" — hai điều khiển vận hành cơ bản đã chốt thuộc phạm vi F4-10 [SoT: 01-rd/screens/admin/admin_queue_monitor.md:64]. **Không dựng toggle thứ ba "Ưu tiên bài thi/Ưu tiên cao"** trên màn thật cho tới khi có quyết định phạm vi rõ ràng — xem mục 7 |
| Độ trễ hàng đợi | `QueueLatencyBar` × N | Tên mức hàng đợi + giá trị độ trễ + thanh tỉ lệ. **Chỉ dựng mức "hàng đợi thường"** khi build FE thật — xem mục 7 về hai mức còn lại |
| Bảng job | `QueueJobTable` | Cột: Job (mã lượt nộp rút gọn), Lượt nộp (mã submission), Ngôn ngữ, Ưu tiên, Trạng thái, Chờ (thời gian đã chờ). Sắp xếp theo mức ưu tiên rồi thời gian chờ [SoT: 09-layoutBase/Admin - Hàng đợi chấm.dc.html:235] |
| Bảng job | `QueueTableFilter` | Bộ lọc phân đoạn 3 nút: Tất cả / Đang chờ / Lỗi — lọc theo `status` |
| Bảng job | Cột "Trạng thái" | Giá trị hiển thị **phải map đúng 3 trạng thái trung gian của state machine F4** (`PENDING`→"Đang chờ", `COMPILING`/`JUDGING`→"Đang chấm", cộng nhãn lỗi khi vào trạng thái cuối lỗi) — không hiển thị `ACCEPTED`/`WRONG_ANSWER`/... nguyên văn vì job đã rời hàng đợi khi vào trạng thái cuối; nhãn "Lỗi sandbox" trong prototype tương ứng job vừa rơi vào `SYSTEM_ERROR`/`COMPILE_ERROR` và còn hiển thị tạm trong danh sách trước khi bị dọn khỏi view (chi tiết vòng đời hiển thị này thuộc DD) |
| Bảng job | Cột "Ưu tiên" | Prototype có 3 giá trị `Ưu tiên cao`/`Thường`/`Chấm lại`. **`Chấm lại` không dựng** (rejudge đã loại khỏi phạm vi, `DEC-2026-0828-remove-rejudge-scope`). `Ưu tiên cao` là nhãn phân loại độ ưu tiên xử lý, không phải "kỳ thi" [SoT: 01-rd/screens/admin/admin_queue_monitor.md:65, `DEC-2026-0831-judge-orchestration-ops-details`] |
| Sự kiện hạ tầng | `InfraEventRow` | Giờ, badge mức độ (`ERROR`/`WARN`/`INFO`), nội dung, tên dịch vụ nguồn (`go-judge`/`ai-gateway`/`db`...). Đây là log hạ tầng, **không phải hành động quản trị của người** — tách khỏi Nhật ký hệ thống (`admin_system_log`) [SoT: 01-rd/screens/admin/admin_queue_monitor.md:46-49] |

## 4. Screen states

| Trạng thái | Điều kiện | Hiển thị |
| :--- | :--- | :--- |
| Đang tải lần đầu | Vừa vào màn, chưa có snapshot | Skeleton cho 4 KPI, khung thẻ worker, bảng job |
| Bình thường | Có dữ liệu, kênh realtime đang kết nối | Đầy đủ 6 vùng, cập nhật liên tục |
| Mất kết nối realtime | Kênh WebSocket/STOMP rớt | Banner cảnh báo "Mất kết nối thời gian thực — đang thử kết nối lại", dữ liệu đóng băng ở snapshot cuối, không tự động fallback sang REST polling liên tục [SoT: Suy luận] — lý do: README.md mục 5 chốt WebSocket/STOMP cho trạng thái theo `submissionId`; với dashboard tổng hợp nhiều luồng như màn này, hợp lý nhất là dùng cùng kênh thay vì mở thêm cơ chế polling song song, nhưng RD/DD chưa xác nhận, cần chốt ở DD |
| Rỗng | Không có job nào trong hàng đợi | Bảng job hiển thị dòng trống "Không có job nào đang chờ", 4 KPI vẫn hiển thị số 0/giá trị thật (Thông lượng, Job lỗi 24 giờ không phụ thuộc hàng đợi rỗng) |
| Toggle đang xử lý | Admin bấm một `OpsToggle`, chờ xác nhận từ backend | Toggle chuyển sang trạng thái "đang chờ" (disabled, có chỉ báo loading), không optimistic-update thẳng UI trước khi có phản hồi — tránh hiển thị sai trạng thái cụm nếu lệnh pause/autoscale thất bại [SoT: Suy luận] |
| Lỗi tải dữ liệu | API snapshot ban đầu lỗi | Thông báo lỗi toàn màn kèm nút thử lại, không hiển thị dữ liệu cũ giả định |

## 5. API tiêu thụ (chỉ liệt kê — hợp đồng đầy đủ ở DD)

| Mục đích | Kênh | Bounded Context sở hữu |
| :--- | :--- | :--- |
| Snapshot ban đầu: 4 KPI, danh sách worker, trạng thái 2 toggle, độ trễ hàng đợi, trang đầu bảng job | REST (GET) | `judge-orchestration` |
| Cập nhật realtime: KPI, trạng thái worker, danh sách job trong hàng đợi | **WebSocket/STOMP** — kênh tổng hợp cấp admin, khác kênh `submissionId` riêng lẻ dùng cho `submission_result` [SoT: README.md mục 5 (WebSocket/STOMP cho trạng thái theo từng lượt nộp); mở rộng sang kênh tổng hợp admin là suy luận — xem mục 4 "Mất kết nối realtime"] | `judge-orchestration` |
| Bật/tắt "Tạm dừng tiêu thụ hàng đợi" | REST (PATCH/POST) | `judge-orchestration` |
| Bật/tắt "Tự động mở rộng worker" | REST (PATCH/POST) | `judge-orchestration` |
| Sự kiện hạ tầng gần đây (danh sách log) | REST (GET), có thể kèm cập nhật qua cùng kênh WebSocket/STOMP ở trên | `judge-orchestration` |

Không có API nào của `problem-bank`, `ai-review`, hay `identity` được màn này gọi trực tiếp — đúng ranh giới
BC, vì toàn bộ dữ liệu hàng đợi/cụm worker thuộc sở hữu `judge-orchestration`
[SoT: 02-bd/architecture/judge-orchestration.md:1-22].

Tên endpoint cụ thể, request/response, mã lỗi: **không viết ở đây** — thuộc `03-dd/api/judge-orchestration.md`
(chưa viết), theo anti-drift rule của `bd-generation` Layer 3.

## 6. Navigation

- Vào từ mục "Vận hành" → "Hàng đợi chấm" trong sidebar admin [SoT: 09-layoutBase/Admin - Hàng đợi chấm.dc.html:352-353].
- Không có điều hướng ra màn khác từ trong các thành phần của màn này (không click-through job sang chi
  tiết submission của học viên) — RD không mô tả hành vi đó, không tự thêm.
- Liên kết footer ("Tài liệu API", "Trạng thái go-judge", "Nhật ký thay đổi", "Hỗ trợ")
  [SoT: 09-layoutBase/Admin - Hàng đợi chấm.dc.html:414-417] — ngoài phạm vi nghiệp vụ F4, không thiết kế
  đích đến ở đây.

## 7. Access rights

- Chỉ actor **A3 (Admin)** truy cập được màn này — không có vai trò A2 (Instructor) hay A1 (Student) nào
  được cấp quyền xem theo RD [SoT: 01-rd/screens/admin/admin_queue_monitor.md:4].
- Hai toggle điều khiển cụm (`OpsToggle`) là hành động ghi — yêu cầu RBAC `ADMIN` ở tầng API, không chỉ ẩn
  UI ở tầng FE [SoT: Suy luận — nguyên tắc RBAC chuẩn của `identity` (F1), áp dụng lại ở đây vì đây là hành
  động vận hành có tác dụng phụ thật (dừng nhận job mới)].
- Không có khái niệm chủ sở hữu dữ liệu cá nhân trên màn này (không phải dữ liệu riêng của một học viên) —
  không cần kiểm tra quyền sở hữu bản ghi, chỉ cần kiểm tra vai trò.

## 8. Việc còn mở

| # | Câu hỏi | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- |
| Q1 | Khung sidebar/topbar chung cho toàn bộ màn admin (logo, nav groups, theme switch) chưa có BD riêng — mọi màn admin đều lặp lại khung này | Viết một BD khung điều hướng admin dùng chung (ví dụ `02-bd/screens/admin/_shell.md`) khi bắt đầu màn admin thứ hai, tránh lặp lại ở từng file | BD tiếp theo |
| Q2 | Kênh realtime cho dashboard admin (KPI + worker + bảng job) là WebSocket/STOMP kênh tổng hợp riêng hay tái sử dụng cơ chế đẩy theo `submissionId` cho nhiều submission cùng lúc — README.md mục 5 chỉ nói rõ trường hợp per-submission | Chốt tại DD `03-dd/api/judge-orchestration.md`: đề xuất một kênh STOMP topic riêng dạng `/topic/admin/queue` phát snapshot tổng hợp định kỳ, tách khỏi topic `/topic/submissions/{id}` | DD |
| Q3 | Ngưỡng "cảnh báo" cho `publish_attempts` của `outbox_events` kẹt nhiều lần — đã nêu ở `02-bd/database/judge-orchestration.md` mục 6 là hiển thị qua màn này (F4-10) nhưng chưa có số cụ thể và chưa rõ hiển thị ở vùng nào (KPI mới, hay gộp vào "Sự kiện hạ tầng gần đây") | Đề xuất gộp vào "Sự kiện hạ tầng gần đây" dạng dòng `WARN` khi `publish_attempts >= 5` (số đề xuất) thay vì thêm KPI mới | Chủ dự án xác nhận số cụ thể |
| Q4 | Mức "hàng đợi ưu tiên" trong khối "Độ trễ theo hàng đợi" — sau khi xác nhận "Ưu tiên cao" chỉ là nhãn phân loại (không phải kỳ thi), mức độ trễ theo nhãn này có còn ý nghĩa hiển thị hay gộp chung vào "hàng đợi thường" | Đề xuất giữ 2 mức khi build thật: "Hàng đợi thường" và "Hàng đợi ưu tiên cao" (đổi tên, không xoá) — vì F4-10 vẫn phân biệt được job có cờ ưu tiên hay không | Chủ dự án |

## 9. Tham chiếu

- `01-rd/screens/admin/admin_queue_monitor.md` — RD của màn.
- `09-layoutBase/Admin - Hàng đợi chấm.dc.html` — prototype (SoT layout, không phải design system cuối
  cùng).
- `02-bd/architecture/judge-orchestration.md` — state machine (7 trạng thái cuối + 3 trạng thái trung
  gian), Outbox, ops controls F4-10/F4-11.
- `02-bd/database/judge-orchestration.md` mục 1.1, 1.5, 1.6 — bảng `submissions`, `language_configs`,
  `outbox_events`.
- `.nexa/control/decision-registry.md` — `DEC-2026-0912-judge-callback-contract`,
  `DEC-2026-0912-judge-outbox-pattern`, `DEC-2026-0831-judge-orchestration-ops-details`,
  `DEC-2026-0831-partial-score-testcase-ratio`, `DEC-2026-0828-remove-rejudge-scope`.
- `README.md` mục 5 — WebSocket/STOMP cho trạng thái theo lượt nộp, RabbitMQ cho hàng đợi.

**Status:** DONE
**Summary:** Đã viết BD cho màn `admin_queue_monitor` tại `02-bd/screens/admin/admin_queue_monitor.md` — layout 6 vùng, component inventory, 6 screen states, API tiêu thụ (chỉ liệt kê tên + BC sở hữu, có phân biệt REST snapshot vs WebSocket/STOMP realtime), navigation, access rights (RBAC ADMIN), và 4 câu hỏi mở.
**Concerns/Blockers:** Không có blocker cứng. Hai điểm cần chủ dự án xác nhận trước khi vào DD: (1) kênh realtime tổng hợp cấp admin (Q2) hiện là suy luận mở rộng từ README.md mục 5, chưa có xác nhận rõ; (2) khung sidebar/topbar admin dùng chung (Q1) chưa có BD riêng, sẽ lặp lại nếu không xử lý trước khi viết BD màn admin tiếp theo.
