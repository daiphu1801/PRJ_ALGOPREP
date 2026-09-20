# RD (Yêu cầu hệ thống mới) — Giám sát hàng đợi / `ADM0401`

> Mã màn hình: `ADM0401` [SoT: 02-bd/_rules/bd-template-9sheet.md — mục 8].
> Slug chính tắc: `admin_queue_monitor` [SoT: 01-rd/overview/system_survey.md — mục 7.3 dòng `admin_queue_monitor`].
> Bounded Context: `judge-orchestration` (F4) [SoT: 01-rd/overview/system_survey.md — mục 7.3 dòng `admin_queue_monitor`]. Actor: A3 (Quản trị viên).
> Nguồn sự thật (SoT): `01-rd/req/judge-orchestration.md` (F4-07, F4-10), `09-layoutBase/Admin - Hàng đợi chấm.dc.html` (prototype), `01-rd/req/user_stories/a3_admin.md` (`US-A3-03`).
> Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

Giám sát trực tiếp độ dài hàng đợi, tình trạng cụm worker go-judge, và job đang chờ/đang chấm, để quản trị
viên biết hệ thống có đang khoẻ không và xử lý kịp khi có bài nộp treo
[SoT: 01-rd/req/judge-orchestration.md — F4-10].

Đối chiếu prototype: `09-layoutBase/Admin - Hàng đợi chấm.dc.html`. File này mô tả **hành vi và UX ở mức
yêu cầu** — không lặp lại đặc tả chức năng đã có ở `01-rd/req/judge-orchestration.md` (mục F4), chỉ trỏ tới và bổ sung
phần đặc thù của màn. Phát hiện Phase 6: khái niệm **"kỳ thi" (ưu tiên hàng đợi cho job thi)** xuất hiện ở
đây và ở `admin_language_config` — **đã chốt 2026-08-31: chỉ là nhãn phân loại ưu tiên, không phải tính
năng thật** (xem mục 5 Q2, `DEC-2026-0831-judge-orchestration-ops-details`). (Trước đó cũng nhắc
`admin_rejudge`, nhưng màn đó **đã loại khỏi phạm vi 2026-08-28** — `DEC-2026-0828-remove-rejudge-scope`.)

---

## 2. Phạm vi

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Giám sát hàng đợi và tình trạng cụm judge engine | F4-10 | `01-rd/req/judge-orchestration.md` — F4-10 |
| Timeout sweep bài nộp treo | F4-07 | `01-rd/req/judge-orchestration.md` — F4-07 |
| Given-When-Then liên quan | — | `01-rd/req/user_stories/a3_admin.md` (`US-A3-03`) |

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

### 2.1 Cấu trúc hệ thống liên quan

| Màn/tài liệu | Vai trò | Bounded Context |
|---|---|---|
| Màn hiện tại | `ADM0401` / `admin_queue_monitor` | `judge-orchestration` |
| Tài liệu yêu cầu | `01-rd/req/judge-orchestration.md` (F4-07, F4-10) | Nguồn SoT của màn |
| Prototype | `09-layoutBase/Admin - Hàng đợi chấm.dc.html` | Bằng chứng bố cục và trạng thái |

---

## 3. Ngoài phạm vi (Out of Scope)

- Bảng màu, spacing, component cụ thể — thuộc BD (`02-bd/screens/admin/ADM0401_queue_monitor.md`, chưa viết).
- Hợp đồng API (số liệu cụm, điều khiển pause/autoscale, WebSocket cập nhật hàng đợi) — thuộc DD
  (`03-dd/api/judge-orchestration.md`, chưa viết).
- ~~Khái niệm "kỳ thi" (Q2) — chờ chủ dự án quyết định phạm vi.~~ **Đã chốt 2026-08-31** (ghi nhận
  2026-09-05): đây chỉ là **nhãn ưu tiên hàng đợi**, không phải một module thi riêng — đổi tên lại cho
  chuẩn xác khi dựng UI thật (`DEC-2026-0831-judge-orchestration-ops-details`).

---

## 4. Tiền đề và ràng buộc

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | ~~Hai toggle "Tạm dừng tiêu thụ hàng đợi" và "Tự động mở rộng worker"...~~ **ĐÃ CHỐT 2026-08-31 (owner instruction, theo đúng đề xuất):** mở rộng F4-10, không cần mã mới. | — | Điều khiển cơ bản là phần tự nhiên của một bảng giám sát vận hành. Đã ghi vào `01-rd/req/judge-orchestration.md` (amendment F4-10). Xem `DEC-2026-0831-judge-orchestration-ops-details`. | Đã đóng |
| Q2 | ~~**Khái niệm "kỳ thi"...**~~ **ĐÃ CHỐT 2026-08-31 (owner instruction, theo đúng đề xuất):** chỉ là nhãn phân loại độ ưu tiên, không có luồng "quản lý kỳ thi" nào. | — | Đổi nhãn "Kỳ thi"/"Ưu tiên bài thi" thành nhãn trung tính (ví dụ "Ưu tiên cao") khi dựng UI thật. Không mở RD mới. Đã ghi vào `01-rd/req/judge-orchestration.md` (amendment F4-10/F4-11). Xem `DEC-2026-0831-judge-orchestration-ops-details`. | Đã đóng |

---

## 5. Danh sách yêu cầu (REQ)
| ID | Yêu cầu | Loại | Nguồn/SoT |
|---|---|---|---|
| REQ-01 | Giám sát hàng đợi và tình trạng cụm judge engine (F4-10) | Chức năng | `01-rd/req/judge-orchestration.md` — F4-10 |
| REQ-02 | Timeout sweep bài nộp treo (F4-07) | Chức năng | `01-rd/req/judge-orchestration.md` — F4-07 |
| REQ-03 | Given-When-Then liên quan (—) | Chức năng | `01-rd/req/user_stories/a3_admin.md` (`US-A3-03`) |

---

## 6. Yêu cầu dữ liệu

### 6.1 Dữ liệu tham chiếu

Chi tiết bảng, cột, kiểu dữ liệu và ràng buộc lưu trữ thuộc BD/DD; tài liệu này không tự suy luận dữ liệu chưa được chốt.

### 6.2 Luồng dữ liệu

Hợp đồng API, request/response và mã lỗi thuộc DD. Luồng dữ liệu mức yêu cầu được thể hiện bởi các hành vi ở chương 5.

---

## 7. RACI và danh sách bàn giao

| Bàn giao | Trạng thái |
|---|---|
| RD màn hình | Có tài liệu này |
| BD màn hình | Chưa xác định — không tự suy luận |
| DD/API liên quan | Chưa xác định — không tự suy luận |

---

## 8. Kế hoạch và quy mô

| Chỉ số | Giá trị |
|---|---|
| Quy mô hành vi | Xem các yêu cầu tại chương 5. |
| Lịch, nhân sự, công sức | Chưa xác định — không tự suy luận. |

---

## 9. Tham chiếu

| Loại | Tham chiếu |
|---|---|
| Yêu cầu module | `01-rd/req/judge-orchestration.md` — F4-07, F4-10. |
| User story | `01-rd/req/user_stories/a3_admin.md` — `US-A3-03`. |
| Khảo sát hệ thống | `01-rd/overview/system_survey.md` mục 7.3 — dòng `admin_queue_monitor`. |
| Prototype | `09-layoutBase/Admin - Hàng đợi chấm.dc.html` — prototype. |
| Quyết định | `DEC-2026-0828-remove-rejudge-scope`; `DEC-2026-0831-judge-orchestration-ops-details`. |
| Kế hoạch | `06-plan/reports/260825-1700-report-ai1-phase6-conflicts.md` — báo cáo xung đột Phase 6. |
