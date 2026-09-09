# RD — Màn `admin_queue_monitor` (Hàng đợi chấm & cụm go-judge)

> Slug: `admin_queue_monitor` — khớp `01-rd/overview/system_survey.md` mục 7.3 [SoT: 01-rd/overview/system_survey.md — mục 7.3 dòng `admin_queue_monitor`].
> Bounded Context: `judge-orchestration` (F4) [SoT: 01-rd/overview/system_survey.md — mục 7.3 dòng `admin_queue_monitor`]. Actor: A3.
>
> Đối chiếu prototype: `09-layoutBase/Admin - Hàng đợi chấm.dc.html`. File này mô tả **hành vi và UX ở mức
> yêu cầu** — không lặp lại đặc tả chức năng đã có ở `01-rd/req/judge-orchestration.md` (mục F4), chỉ trỏ tới và bổ sung
> phần đặc thù của màn. Phát hiện Phase 6: khái niệm **"kỳ thi" (ưu tiên hàng đợi cho job thi)** xuất hiện ở
> đây và ở `admin_language_config` — **đã chốt 2026-08-31: chỉ là nhãn phân loại ưu tiên, không phải tính
> năng thật** (xem mục 5 Q2, `DEC-2026-0831-judge-orchestration-ops-details`). (Trước đó cũng nhắc
> `admin_rejudge`, nhưng màn đó **đã loại khỏi phạm vi 2026-08-28** — `DEC-2026-0828-remove-rejudge-scope`.)

## 1. Mục đích màn hình

Giám sát trực tiếp độ dài hàng đợi, tình trạng cụm worker go-judge, và job đang chờ/đang chấm, để quản trị
viên biết hệ thống có đang khoẻ không và xử lý kịp khi có bài nộp treo
[SoT: 01-rd/req/judge-orchestration.md — F4-10].

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Giám sát hàng đợi và tình trạng cụm judge engine | F4-10 | `01-rd/req/judge-orchestration.md` — F4-10 |
| Timeout sweep bài nộp treo | F4-07 | `01-rd/req/judge-orchestration.md` — F4-07 |
| Given-When-Then liên quan | — | `01-rd/req/user_stories/a3_admin.md` (`US-A3-03`) |

## 3. Trạng thái và cấu trúc màn (screen states)

Đối chiếu `09-layoutBase/Admin - Hàng đợi chấm.dc.html`:

1. **4 chỉ số tổng** — Job đang chờ, Đang chấm, Thông lượng (job/phút), Job lỗi 24 giờ (dòng 440-445) — khớp
   F4-10.
2. **Cụm worker go-judge** — 4 worker, mỗi worker: trạng thái (Đang chạy/Nhàn rỗi/Quá tải), % tải, số job,
   uptime, vCPU (dòng 452-462) — khớp F4-10 (tình trạng cụm).
3. **Điều khiển cụm** — 3 toggle: "Tạm dừng tiêu thụ hàng đợi", "Tự động mở rộng worker", "Ưu tiên bài thi"
   (dòng 470-474). Toggle đầu và toggle hai (pause/autoscale) là hành vi vận hành hợp lý của F4-10 (giám sát
   kèm điều khiển cơ bản), không cần mã riêng. **Toggle ba ("Ưu tiên bài thi") ngụ ý một khái niệm "kỳ thi"
   chưa từng xuất hiện trong `judge-orchestration.md`** — xem Câu hỏi mở Q2.
4. **Độ trễ theo hàng đợi** — 3 mức: hàng đợi thường, hàng đợi ưu tiên, hàng đợi chấm lại (dòng 477-481) —
   mức "hàng đợi ưu tiên" cùng nhóm phát hiện Q2 ("hàng đợi ưu tiên" gắn với khái niệm kỳ thi ở mục 5); mức
   "hàng đợi chấm lại" **hết ý nghĩa sau 2026-08-28** (chấm lại đã loại khỏi phạm vi,
   `DEC-2026-0828-remove-rejudge-scope`) — không dựng khi build FE thật.
5. **Bảng job trong hàng đợi** — cột Job/Lượt nộp/Ngôn ngữ/Ưu tiên/Trạng thái/Chờ; giá trị cột "Ưu tiên" có
   `Kỳ thi`/`Thường`/`Chấm lại` (dòng 505-516) — `Kỳ thi` lặp lại phát hiện Q2; `Chấm lại` **hết ý nghĩa sau
   2026-08-28** cùng lý do trên, không dựng khi build FE thật.
6. **Sự kiện hạ tầng gần đây** — log worker/AI Gateway/backup, **không phải hành động quản trị của người**
   (dòng 273-274, 523-530) — đúng đúng phân định đã chốt ở F1-14 (`01-rd/req/identity.md`): sự kiện hạ tầng thuộc
   `judge-orchestration`, tách khỏi Nhật ký hệ thống (`admin_system_log`). Không phải xung đột, chỉ ghi nhận
   khớp đúng thiết kế đã chốt trước.

## 4. Given-When-Then bổ sung ở mức màn

- **Cho** một worker go-judge đạt trạng thái "Quá tải" (tải > 85%), **Khi** quản trị viên mở màn giám sát,
  **Thì** worker đó hiện màu cảnh báo trong bảng cụm worker, phân biệt được với worker "Đang chạy" bình
  thường [SoT: 09-layoutBase/Admin - Hàng đợi chấm.dc.html:447-461].
- **Cho** quản trị viên bật "Tạm dừng tiêu thụ hàng đợi", **Khi** toggle có hiệu lực, **Thì** worker ngừng
  nhận job mới nhưng job đang chấm vẫn chạy hết, không bị ngắt giữa chừng — nguyên tắc "không đổ vỡ dữ liệu
  đang xử lý" áp dụng như mọi thao tác tạm dừng khác trong hệ thống.

## 5. Câu hỏi mở

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | ~~Hai toggle "Tạm dừng tiêu thụ hàng đợi" và "Tự động mở rộng worker"...~~ **ĐÃ CHỐT 2026-08-31 (owner instruction, theo đúng đề xuất):** mở rộng F4-10, không cần mã mới. | — | Điều khiển cơ bản là phần tự nhiên của một bảng giám sát vận hành. Đã ghi vào `01-rd/req/judge-orchestration.md` (amendment F4-10). Xem `DEC-2026-0831-judge-orchestration-ops-details`. | Đã đóng |
| Q2 | ~~**Khái niệm "kỳ thi"...**~~ **ĐÃ CHỐT 2026-08-31 (owner instruction, theo đúng đề xuất):** chỉ là nhãn phân loại độ ưu tiên, không có luồng "quản lý kỳ thi" nào. | — | Đổi nhãn "Kỳ thi"/"Ưu tiên bài thi" thành nhãn trung tính (ví dụ "Ưu tiên cao") khi dựng UI thật. Không mở RD mới. Đã ghi vào `01-rd/req/judge-orchestration.md` (amendment F4-10/F4-11). Xem `DEC-2026-0831-judge-orchestration-ops-details`. | Đã đóng |

## 6. Ngoài phạm vi file này

- Bảng màu, spacing, component cụ thể — thuộc BD (`02-bd/screens/admin/admin_queue_monitor.md`, chưa viết).
- Hợp đồng API (số liệu cụm, điều khiển pause/autoscale, WebSocket cập nhật hàng đợi) — thuộc DD
  (`03-dd/api/judge-orchestration.md`, chưa viết).
- ~~Khái niệm "kỳ thi" (Q2) — chờ chủ dự án quyết định phạm vi.~~ **Đã chốt 2026-08-31** (ghi nhận
  2026-09-05): đây chỉ là **nhãn ưu tiên hàng đợi**, không phải một module thi riêng — đổi tên lại cho
  chuẩn xác khi dựng UI thật (`DEC-2026-0831-judge-orchestration-ops-details`).

## 7. Tham chiếu

- `01-rd/req/judge-orchestration.md` — F4-07, F4-10.
- `01-rd/req/user_stories/a3_admin.md` — `US-A3-03`.
- `01-rd/overview/system_survey.md` mục 7.3 — dòng `admin_queue_monitor`.
- `09-layoutBase/Admin - Hàng đợi chấm.dc.html` — prototype.
- `06-plan/reports/260825-1700-report-ai1-phase6-conflicts.md` — báo cáo xung đột Phase 6.
