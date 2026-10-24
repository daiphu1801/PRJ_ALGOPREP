# RD (Yêu cầu hệ thống mới) — Giám sát hàng đợi / `ADM0401`

> Mã màn hình: `ADM0401` [SoT: 02-bd/_rules/bd-template-9sheet.md — mục 8].
> Slug chính tắc: `admin_queue_monitor` [SoT: 01-rd/overview/system_survey.md — mục 7.3 dòng `admin_queue_monitor`].
> Phạm vi/Bounded Context: `judge-orchestration` (F4) [SoT: 01-rd/overview/system_survey.md — mục 7.3 dòng `admin_queue_monitor`]. Actor: A3 (Quản trị viên).
> Nguồn sự thật (SoT): `01-rd/req/F4_judge-orchestration.md` (F4-07, F4-10), `09-layoutBase/Admin - Hàng đợi chấm.dc.html` (prototype gốc, chỉ để tham khảo), `01-rd/req/user_stories/a3_admin.md` (`US-A3-03`).
> Phiên bản RD: sửa 2026-10-10 theo `DEC-2026-1010-admin-operations-regroup`. Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

Giám sát trực tiếp độ dài hàng đợi, tình trạng cụm worker go-judge và job đang chờ/đang chấm, để quản trị
viên biết hệ thống có đang khoẻ không và xử lý kịp khi có bài nộp treo
[SoT: 01-rd/req/F4_judge-orchestration.md — F4-10].

Màn này **chỉ hiện cái bất thường trước**: một dải trạng thái một dòng cho biết hệ thống đang ổn hay không,
phần chi tiết (worker, job) nằm bên dưới và gọn. Log hạ tầng của máy móc (bản ghi Outbox kẹt,
lượt nộp lỗi sandbox) **không còn nằm ở màn này** mà ở màn riêng `ADM0404` Sự kiện hạ tầng
(`01-rd/screens/admin/ADM0404_infra_events.md`), vì nó chỉ đọc khi có sự cố và cần lọc, tìm kiếm, xem lùi.

Đối chiếu prototype gốc: `09-layoutBase/Admin - Hàng đợi chấm.dc.html` (chỉ tham khảo từ 2026-10-03; prototype
Next.js 2026-10-10 là sự thật về hình). File này mô tả **hành vi và UX ở mức yêu cầu** — không lặp lại đặc
tả chức năng đã có ở `01-rd/req/F4_judge-orchestration.md` (mục F4), chỉ trỏ tới và bổ sung phần đặc thù của màn.
Khái niệm **"kỳ thi"** **không tồn tại** trong hệ thống
(`DEC-2026-0831-judge-orchestration-ops-details`), và màn `admin_rejudge` đã loại khỏi phạm vi
(`DEC-2026-0828-remove-rejudge-scope`).

---

## 2. Phạm vi

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Giám sát hàng đợi và tình trạng cụm judge engine | F4-10 | `01-rd/req/F4_judge-orchestration.md` — F4-10 |
| Timeout sweep bài nộp treo (bỏ qua job `PENDING` khi hàng đợi đang tạm dừng) | F4-07 | `01-rd/req/F4_judge-orchestration.md` — F4-07, amendment 2026-10-10 |
| Given-When-Then liên quan | — | `01-rd/req/user_stories/a3_admin.md` (`US-A3-03`) |

Các khối của màn (thiết kế lại 2026-10-10, `DEC-2026-1010-admin-operations-regroup`):

1. **Dải trạng thái** — một dòng: "Bình thường" / "Đang dồn N job" / "Đang tạm dừng" / "Worker quá tải", kèm
   "Cập nhật lúc HH:mm:ss"; quá 60 giây không nhận dữ liệu mới thì ghi "Dữ liệu cũ". Đây **không phải thẻ
   KPI** nên không trái `DEC-2026-1001-single-overview-page-kpi` (chỉ trang tổng quan hiển thị KPI). Dải 4
   chỉ số tổng của mockup (Job đang chờ, Đang chấm, Thông lượng, Job lỗi 24 giờ) vẫn **không dựng**.
2. **Cụm worker go-judge** — bảng gọn, mỗi worker một hàng: mã, trạng thái (Đang chạy / Nhàn rỗi / Quá tải),
   thanh tải, số job. Uptime và vCPU không còn là phần chính của mỗi hàng. Số liệu cụm lấy từ Prometheus đã
   có trong stack, không thêm bảng `[SoT: Suy luận]` (chưa kiểm go-judge xuất được metric này).
3. **Điều khiển** — một thẻ gồm độ trễ chờ trung bình (một con số) và hai công tắc: "Tạm dừng tiêu thụ hàng
   đợi", "Tự động mở rộng worker" (Q1 cũ đã chốt F4-10 bao gồm hai điều khiển này). Tạm dừng nghĩa là worker ngừng nhận job mới, job đang chấm vẫn chạy hết. Bật tạm dừng và tắt tự
   động mở rộng đều **hỏi xác nhận** trước khi gửi lệnh.
4. **Bảng job** — tab có đếm: `Đang chờ N · Lỗi N · Tất cả N`, mặc định "Đang chờ"; 4 cột (Lượt nộp, Ngôn
   ngữ, Trạng thái, Chờ), sắp xếp theo thời gian chờ, phân trang theo tham số "số dòng mỗi trang".
5. **Thẻ liên kết sự kiện hạ tầng** — số cảnh báo và số lỗi gần đây, bấm sang `ADM0404`.
6. **Cấu hình** — nút ở thanh tiêu đề mở popup "Tham số hàng đợi" (bốn số nguyên); cạnh nút "Làm mới" **tải lại
   thật**.

Đã **bỏ khỏi màn** (không phải bỏ chức năng):

- Mức độ trễ thứ hai "Hàng đợi ưu tiên cao" và cột "Ưu tiên" của bảng job: **chủ dự án đã xác nhận 2026-10-10**
  (`DEC-2026-1010-admin-operations-regroup`, đóng Q4 của BD). Lý do: bảng `submissions` không có cờ ưu tiên
  nào, hiển thị nó là bịa dữ liệu.
- Cột "Job" (`#J-...`): không có thực thể "job" riêng, chỉ có `submissions.id`.
- Mức "Hàng đợi chấm lại", giá trị `Chấm lại` và nút chấm lại: hết ý nghĩa sau `DEC-2026-0828-remove-rejudge-scope`.
- Khối "Sự kiện hạ tầng gần đây": chuyển sang `ADM0404`.

### 2.1 Cấu trúc hệ thống liên quan

| Màn/tài liệu | Vai trò | Bounded Context |
|---|---|---|
| Màn hiện tại | `ADM0401` / `admin_queue_monitor` | `judge-orchestration` |
| Màn liên quan | `ADM0404` / `admin_infra_events` — log máy | `judge-orchestration` |
| Tài liệu yêu cầu | `01-rd/req/F4_judge-orchestration.md` (F4-07, F4-10) | Nguồn SoT của màn |
| Prototype gốc | `09-layoutBase/Admin - Hàng đợi chấm.dc.html` | Bằng chứng bố cục ban đầu, chỉ tham khảo |

---

## 3. Ngoài phạm vi (Out of Scope)

- Bảng màu, spacing, component cụ thể — thuộc BD (`02-bd/screens/admin/ADM0401_queue_monitor.md`).
- Hợp đồng API (số liệu cụm, điều khiển pause/autoscale, kênh cập nhật hàng đợi) — thuộc DD
  (`03-dd/api/F4_judge-orchestration.md`, chưa viết).
- ~~Khái niệm "kỳ thi" (Q2) — chờ chủ dự án quyết định phạm vi.~~ **Đã chốt 2026-08-31**: không có khái niệm
  kỳ thi (`DEC-2026-0831-judge-orchestration-ops-details`).
- Chấm lại hàng loạt (`DEC-2026-0828-remove-rejudge-scope`).

---

## 4. Tiền đề và ràng buộc

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | ~~Hai toggle "Tạm dừng tiêu thụ hàng đợi" và "Tự động mở rộng worker"...~~ **ĐÃ CHỐT 2026-08-31 (owner instruction, theo đúng đề xuất):** mở rộng F4-10, không cần mã mới. | — | Đã ghi vào `01-rd/req/F4_judge-orchestration.md` (amendment F4-10). Xem `DEC-2026-0831-judge-orchestration-ops-details`. | Đã đóng |
| Q2 | ~~**Khái niệm "kỳ thi"...**~~ **ĐÃ CHỐT 2026-08-31 (owner instruction, theo đúng đề xuất):** chỉ là nhãn phân loại độ ưu tiên, không có luồng "quản lý kỳ thi" nào. **Cập nhật 2026-10-10:** nhãn "ưu tiên" nay đề xuất bỏ hẳn khỏi màn (xem mục 2, chờ chủ dự án xác nhận). | — | Xem `DEC-2026-0831-judge-orchestration-ops-details` và `DEC-2026-1010-admin-operations-regroup`. | Chờ xác nhận phần bỏ ưu tiên |
| Q3 | **Quy tắc mới 2026-10-10, đã chốt:** khi Admin tạm dừng tiêu thụ hàng đợi, timeout sweep (F4-07) bỏ qua job `PENDING`; mọi thao tác ghi vận hành của màn được ghi vào Nhật ký hệ thống, phân loại "Cấu hình", kèm giá trị cũ và mới (đúng F1-14). | — | `01-rd/req/F4_judge-orchestration.md` (amendment 2026-10-10); `DEC-2026-1010-admin-operations-regroup` | Đã đóng |
| Q4 | ~~Admin có cần trần số worker và ngưỡng thu hẹp cho tự động mở rộng không?~~ **ĐÃ CHỐT 2026-10-10 (chủ dự án đã xác nhận 2026-10-10, tiêu chí ponytail bậc 3 và 5):** không thêm; "tối đa 8" là hằng số `application.yml`. Rủi ro autoscale thật trên Docker Compose ghi ở BD `ADM0401` mục "Rủi ro kỹ thuật" (R2) | - | Đã chốt, trùng BD Q8 | Đã đóng |

---

## 5. Danh sách yêu cầu (REQ)
| ID | Yêu cầu | Loại | Nguồn/SoT |
|---|---|---|---|
| REQ-01 | Giám sát hàng đợi và tình trạng cụm judge engine (F4-10) | Chức năng | `01-rd/req/F4_judge-orchestration.md` — F4-10 |
| REQ-02 | Timeout sweep bài nộp treo (F4-07), bỏ qua job `PENDING` khi hàng đợi đang tạm dừng | Chức năng | `01-rd/req/F4_judge-orchestration.md` — F4-07, amendment 2026-10-10 |
| REQ-03 | Given-When-Then liên quan (—) | Chức năng | `01-rd/req/user_stories/a3_admin.md` (`US-A3-03`) |
| REQ-04 | **Cho** hàng đợi đang bình thường, **Khi** quản trị viên mở màn, **Thì** dải trạng thái hiện "Bình thường" kèm "Cập nhật lúc"; **Khi** có từ một worker quá tải trở lên hoặc hàng đợi đang dồn hoặc đang tạm dừng, **Thì** dải đổi sang trạng thái tương ứng; **Khi** quá 60 giây không nhận dữ liệu mới, **Thì** hiện "Dữ liệu cũ". | Chức năng | Chốt 2026-10-10 (`DEC-2026-1010-admin-operations-regroup`) |
| REQ-05 | **Cho** quản trị viên bật "Tạm dừng tiêu thụ hàng đợi" hoặc tắt "Tự động mở rộng worker", **Khi** bấm công tắc, **Thì** hiện hộp xác nhận nêu hậu quả; chỉ gửi lệnh sau khi xác nhận; **Khi** lệnh thành công, **Thì** thao tác được ghi vào Nhật ký hệ thống (phân loại "Cấu hình"). Tắt tạm dừng và bật tự động mở rộng không cần hỏi. | Chức năng | Chốt 2026-10-10 |
| REQ-06 | **Cho** quản trị viên bấm "Làm mới", **Khi** tải xong, **Thì** mọi số liệu trên màn là dữ liệu mới và "Cập nhật lúc" đặt lại; **Khi** tải thất bại, **Thì** giữ dữ liệu cũ và báo lỗi. Dữ liệu cũng tự cập nhật qua kênh thời gian thực. | Chức năng | Chốt 2026-10-10 |

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
| BD màn hình | `02-bd/screens/admin/ADM0401_queue_monitor.md` (V1.0) |
| DD/API liên quan | Chưa viết — không tự suy luận |

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
| Yêu cầu module | `01-rd/req/F4_judge-orchestration.md` — F4-07, F4-10. |
| User story | `01-rd/req/user_stories/a3_admin.md` — `US-A3-03`. |
| Khảo sát hệ thống | `01-rd/overview/system_survey.md` mục 7.3 — dòng `admin_queue_monitor`. |
| Prototype gốc | `09-layoutBase/Admin - Hàng đợi chấm.dc.html` — chỉ tham khảo. |
| Màn liên quan | `01-rd/screens/admin/ADM0404_infra_events.md` — sự kiện hạ tầng. |
| Quyết định | `DEC-2026-0828-remove-rejudge-scope`; `DEC-2026-0831-judge-orchestration-ops-details`; `DEC-2026-1001-single-overview-page-kpi`; `DEC-2026-1010-admin-operations-regroup`. |
| Báo cáo | `06-plan/reports/261010-1030-admin-van-hanh-userflow-va-mau-thuan/report.md` — phát hiện F1-F12, lỗ hổng G1-G6. |
