# RD — Màn `admin_queue_monitor` (Hàng đợi chấm & cụm go-judge)

> Slug: `admin_queue_monitor` — khớp `01-rd/overview/system_survey.md` mục 7.3 [SoT: 01-rd/overview/system_survey.md:550].
> Bounded Context: `judge-orchestration` (F4) [SoT: 01-rd/overview/system_survey.md:550]. Actor: A3.
>
> Đối chiếu prototype: `09-layoutBase/Admin - Hàng đợi chấm.dc.html`. File này mô tả **hành vi và UX ở mức
> yêu cầu** — không lặp lại đặc tả chức năng đã có ở `01-rd/req/req.md` (mục F4), chỉ trỏ tới và bổ sung
> phần đặc thù của màn. Phát hiện Phase 6: khái niệm **"kỳ thi" (ưu tiên hàng đợi cho job thi)** xuất hiện ở
> đây và ở `admin_rejudge`/`admin_language_config` nhưng chưa từng có mã `Fx-nn` — xem Câu hỏi mở Q2.

## 1. Mục đích màn hình

Giám sát trực tiếp độ dài hàng đợi, tình trạng cụm worker go-judge, và job đang chờ/đang chấm, để quản trị
viên biết hệ thống có đang khoẻ không và xử lý kịp khi có bài nộp treo
[SoT: 01-rd/req/req.md:290 — F4-10].

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Giám sát hàng đợi và tình trạng cụm judge engine | F4-10 | `01-rd/req/req.md:290` |
| Timeout sweep bài nộp treo | F4-07 | `01-rd/req/req.md:223-225` |
| Given-When-Then liên quan | — | `01-rd/req/user_stories.md:307-315` (`US-A3-03`) |

## 3. Trạng thái và cấu trúc màn (screen states)

Đối chiếu `09-layoutBase/Admin - Hàng đợi chấm.dc.html`:

1. **4 chỉ số tổng** — Job đang chờ, Đang chấm, Thông lượng (job/phút), Job lỗi 24 giờ (dòng 440-445) — khớp
   F4-10.
2. **Cụm worker go-judge** — 4 worker, mỗi worker: trạng thái (Đang chạy/Nhàn rỗi/Quá tải), % tải, số job,
   uptime, vCPU (dòng 452-462) — khớp F4-10 (tình trạng cụm).
3. **Điều khiển cụm** — 3 toggle: "Tạm dừng tiêu thụ hàng đợi", "Tự động mở rộng worker", "Ưu tiên bài thi"
   (dòng 470-474). Toggle đầu và toggle hai (pause/autoscale) là hành vi vận hành hợp lý của F4-10 (giám sát
   kèm điều khiển cơ bản), không cần mã riêng. **Toggle ba ("Ưu tiên bài thi") ngụ ý một khái niệm "kỳ thi"
   chưa từng xuất hiện trong `req.md`** — xem Câu hỏi mở Q2.
4. **Độ trễ theo hàng đợi** — 3 mức: hàng đợi thường, hàng đợi ưu tiên, hàng đợi chấm lại (dòng 477-481) —
   cùng nhóm phát hiện Q2 ("hàng đợi ưu tiên" gắn với khái niệm kỳ thi ở mục 5).
5. **Bảng job trong hàng đợi** — cột Job/Lượt nộp/Ngôn ngữ/Ưu tiên/Trạng thái/Chờ; giá trị cột "Ưu tiên" có
   `Kỳ thi`/`Thường`/`Chấm lại` (dòng 505-516) — `Kỳ thi` lặp lại phát hiện Q2; `Chấm lại` khớp F4-09.
6. **Sự kiện hạ tầng gần đây** — log worker/AI Gateway/backup, **không phải hành động quản trị của người**
   (dòng 273-274, 523-530) — đúng đúng phân định đã chốt ở F1-14 (`req.md:74-77`): sự kiện hạ tầng thuộc
   `judge-orchestration`, tách khỏi Nhật ký hệ thống (`admin_system_log`). Không phải xung đột, chỉ ghi nhận
   khớp đúng thiết kế đã chốt trước.

## 4. Given-When-Then bổ sung ở mức màn

- **Cho** một worker go-judge đạt trạng thái "Quá tải" (tải > 85%), **Khi** quản trị viên mở màn giám sát,
  **Thì** worker đó hiện màu cảnh báo trong bảng cụm worker, phân biệt được với worker "Đang chạy" bình
  thường [SoT: 09-layoutBase/Admin - Hàng đợi chấm.dc.html:447-461].
- **Cho** quản trị viên bật "Tạm dừng tiêu thụ hàng đợi", **Khi** toggle có hiệu lực, **Thì** worker ngừng
  nhận job mới nhưng job đang chấm vẫn chạy hết, không bị ngắt giữa chừng — hành vi này cần khớp nguyên tắc
  "không đổ vỡ dữ liệu đang xử lý" đã áp dụng cho tạm dừng chấm lại (F4-09d).

## 5. Câu hỏi mở

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | Hai toggle "Tạm dừng tiêu thụ hàng đợi" và "Tự động mở rộng worker" là điều khiển vận hành trực tiếp trên cụm go-judge — có cần mã `Fx-nn` riêng, hay coi là chi tiết vận hành nằm trong F4-10 ("giám sát... và tình trạng cụm") không cần mã mới? | `req.md:290` chỉ nói "giám sát", không nói rõ có được *điều khiển* cụm từ màn này hay chỉ xem. | Coi là mở rộng hợp lý của F4-10 (giám sát luôn đi kèm điều khiển cơ bản ở một bảng vận hành), không cần mã mới — nhưng cần chủ dự án xác nhận vì đây vẫn là quyết định phạm vi (giám sát thuần vs. giám sát + điều khiển). | Chủ dự án |
| Q2 | **Khái niệm "kỳ thi" (ưu tiên hàng đợi cho job thi, hàng đợi ưu tiên riêng) xuất hiện ở đây và lặp lại ở `admin_rejudge` (nhãn "Kỳ thi giữa kỳ CS201" trong lịch sử chấm lại) và `admin_language_config` (toggle sandbox "Tắt trong mọi kỳ thi") — nhưng không có mã `Fx-nn` nào, không có màn nào cho việc tạo/quản lý một "kỳ thi", và `req.md`/`README.md` không nhắc "kỳ thi" ở đâu cả.** Đây có phải một khái niệm nghiệp vụ thật (chế độ thi có giám sát, giới hạn thời gian riêng, tắt truy cập mạng) cần một luồng RD riêng, hay chỉ là dữ liệu mẫu minh họa cho "độ ưu tiên xử lý cao hơn" mà không có nghĩa nghiệp vụ gì thêm? | Đây là phát hiện lặp lại ở 3 màn khác nhau trong Phase 6, mức độ ảnh hưởng rộng (có thể cần thêm cả actor mới hoặc luồng riêng "thi giữa kỳ"), không tự quyết được vì không rõ ranh giới tính năng. | Nếu chỉ là nhãn phân loại độ ưu tiên (không có luồng quản lý kỳ thi riêng): đổi nhãn "Kỳ thi" thành nhãn trung tính hơn (ví dụ "Ưu tiên cao") khi dựng UI thật, không cần RD riêng. Nếu là tính năng thật: cần một phase RD mới cho "chế độ thi" (có thể thuộc `judge-orchestration` + `identity`), ngoài phạm vi 30 màn đã liệt kê. | Chủ dự án |

## 6. Ngoài phạm vi file này

- Bảng màu, spacing, component cụ thể — thuộc BD (`02-bd/screens/admin/admin_queue_monitor.md`, chưa viết).
- Hợp đồng API (số liệu cụm, điều khiển pause/autoscale, WebSocket cập nhật hàng đợi) — thuộc DD
  (`03-dd/api/judge-orchestration.md`, chưa viết).
- Khái niệm "kỳ thi" (Q2) — không tự đặc tả trong file này, chờ chủ dự án quyết định phạm vi.

## 7. Tham chiếu

- `01-rd/req/req.md:223-225, 290` — F4-07, F4-10.
- `01-rd/req/user_stories.md:307-315` — `US-A3-03`.
- `01-rd/overview/system_survey.md` mục 7.3 — dòng `admin_queue_monitor`.
- `09-layoutBase/Admin - Hàng đợi chấm.dc.html` — prototype.
- `06-plan/reports/260825-XXXX-report-ai1-phase6-conflicts.md` — báo cáo xung đột Phase 6.
