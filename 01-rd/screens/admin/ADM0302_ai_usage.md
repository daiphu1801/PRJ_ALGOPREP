# RD (Yêu cầu hệ thống mới) — Tiêu thụ token AI / `ADM0302`

> Mã màn hình: `ADM0302` [SoT: 02-bd/_rules/bd-template-9sheet.md — mục 8].
> Slug chính tắc: `admin_ai_usage` [SoT: 01-rd/overview/system_survey.md — mục 7.3 dòng `admin_ai_usage`].
> Phạm vi/Bounded Context: `ai-review` (F5) [SoT: 01-rd/overview/system_survey.md — mục 7.3 dòng `admin_ai_usage`]. Actor: A3 (Quản trị viên).
> Nguồn sự thật (SoT): `01-rd/req/ai-review.md`, `09-layoutBase/Admin - Token AI.dc.html` (prototype),
> `01-rd/req/user_stories/a3_admin.md`.
> Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

Theo dõi lượng token AI đã tiêu thụ theo ngày/theo tính năng/theo người học, ngân sách còn lại, dự báo cạn
quota, và bảng xếp hạng nơi tốn token nhiều nhất [SoT: 01-rd/req/ai-review.md — F5-21, F5-25].

Đối chiếu prototype: `09-layoutBase/Admin - Token AI.dc.html`. File này mô tả **hành vi và UX ở mức yêu
cầu** — không lặp lại đặc tả chức năng đã có ở `01-rd/req/ai-review.md` (F5-21, F5-25), chỉ trỏ tới và bổ sung
phần đặc thù của màn. **Cập nhật 2026-08-31:** "Gợi ý theo bậc" (từng chiếm 45% ngân sách token trong dữ
liệu mẫu ở đây) đã **cắt khỏi phạm vi** — xem `DEC-2026-0831-remove-tiered-hints-ai-config` — dải "Gợi ý"
trong biểu đồ theo ngày và dòng "Gợi ý theo bậc" (45%) trong khối "Theo tính năng" xoá khi dựng UI thật.

---

## 2. Phạm vi

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Theo dõi lượng token tiêu thụ theo phiên | F5-21 | `01-rd/req/ai-review.md` — F5-21 |
| Ngân sách token, dự báo cạn quota, tự động khoá khi vượt | F5-25 | `01-rd/req/ai-review.md` — F5-25 |
| Given-When-Then liên quan | — | `01-rd/req/user_stories/a3_admin.md` (`US-A3-04`, GWT 3-4) |

Đối chiếu `09-layoutBase/Admin - Token AI.dc.html`:

1. **4 chỉ số tổng** — Token tháng này, Trung bình mỗi ngày, Chi phí tạm tính, Lượt gọi AI (dòng 451-456) —
   khớp F5-21.
2. **Biểu đồ token theo ngày, tách theo tính năng** — 3 dải màu: Gợi ý, Phân tích, Phỏng vấn (dòng 476-480)
   — **thiếu dải cho "Sinh testcase" (F2-14) dù dải đó có xuất hiện ở khối "Theo tính năng" phía dưới** —
   không phải xung đột nghiêm trọng, chỉ là biểu đồ chọn hiện 3/4 tính năng có khối lượng lớn nhất. **Cập
   nhật 2026-08-31:** dải "Gợi ý" đã cắt khỏi phạm vi (`DEC-2026-0831-remove-tiered-hints-ai-config`) — xoá
   khi dựng UI thật.
3. **Hạn mức tháng 8** — đã dùng / hạn mức, % thanh tiến độ, còn lại, dự kiến hết ngày nào (dòng 210-215,
   525-529) — khớp F5-25 (ngân sách + dự báo).
4. **Theo tính năng** — 4 dòng: "Gợi ý theo bậc" (45%), "Phân tích bài giải" (32%), "Phỏng vấn giả lập"
   (18%), "Sinh testcase" (6%) (dòng 482-487). **Cập nhật 2026-08-31:** dòng "Gợi ý theo bậc" (từng chiếm tỉ
   trọng lớn nhất trong dữ liệu mẫu) đã cắt khỏi phạm vi cùng quyết định trên — xoá khi dựng UI thật, 3 tính
   năng còn lại giữ nguyên.
5. **Cảnh báo** — "Sắp cạn hạn mức", "Một tài khoản dùng bất thường" (214 lượt/24 giờ, gấp 7 lần trung
   bình), "Prompt sinh testcase tốn hơn dự kiến" (dòng 489-498). Cảnh báo thứ hai ("dùng bất thường") ngụ ý
   một ngưỡng phát hiện lạm dụng cấp tài khoản — chưa có mã `Fx-nn`, xem Câu hỏi mở Q1 dưới.
6. **Người học dùng nhiều nhất / Bài toán tốn token nhất** — hai bảng xếp hạng (dòng 500-523) — khớp đúng
   F5-25 ("bảng xếp hạng người dùng/bài toán tốn token nhiều nhất").

### 2.1 Cấu trúc hệ thống liên quan

| Màn/tài liệu | Vai trò | Bounded Context |
|---|---|---|
| Màn hiện tại | `ADM0302` / `admin_ai_usage` | `ai-review` |
| Tài liệu yêu cầu | `01-rd/req/ai-review.md` (F5-21, F5-25) | Nguồn SoT của màn |
| Prototype | `09-layoutBase/Admin - Token AI.dc.html` | Bằng chứng bố cục và trạng thái |

**Chi tiết trạng thái và cấu trúc màn:**

| Thành phần | Vai trò | Nguồn |
| :--- | :--- | :--- |
| `01-rd/screens/admin/ADM0101_overview.md` | Màn cha — nav nhóm "AI" trỏ tới màn này | [SoT: 01-rd/screens/admin/ADM0101_overview.md:160] |
| `01-rd/screens/admin/ADM0301_ai_config.md` | Màn liên quan — cùng nhóm nav "AI", cùng chờ chung quyết định cắt "Gợi ý theo bậc" | [SoT: 01-rd/screens/admin/ADM0301_ai_config.md:67] |
| `09-layoutBase/Admin - Token AI.dc.html` | Prototype đối chiếu chính của file này | [SoT: 09-layoutBase/Admin - Token AI.dc.html] |
| Bounded Context `ai-review` | BC sở hữu toàn bộ hành vi của màn | [SoT: 01-rd/overview/system_survey.md — mục 7.3 dòng `admin_ai_usage`] |

---

## 3. Ngoài phạm vi (Out of Scope)

- Bảng màu, spacing, component cụ thể — thuộc BD (`02-bd/screens/admin/ADM0302_ai_usage.md`, chưa viết).
- Hợp đồng API (số liệu token theo ngày/tính năng/người dùng, cảnh báo) — thuộc DD
  (`03-dd/api/ai-review.md`, chưa viết). Không viết DD cho "Gợi ý theo bậc" — đã cắt khỏi phạm vi.

---

## 4. Tiền đề và ràng buộc

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | ~~Cảnh báo "Một tài khoản dùng bất thường"...~~ **ĐÃ CHỐT 2026-08-31 (owner instruction, theo đúng đề xuất):** mở rộng F5-19/F5-25, không cần mã mới. | — | Cảnh báo mềm dựa trên độ lệch so với trung bình, không tự khoá tài khoản (khác khoá do vượt ngân sách). Đã ghi vào `01-rd/req/ai-review.md` (amendment F5-25). Xem `DEC-2026-0831-ai-usage-anomaly-alert`. Ngưỡng cụ thể chốt khi viết BD. | Đã đóng |

**Cập nhật 2026-08-31 — cả hai câu hỏi trung tâm của file này đã đóng:** Q1 ("Một tài khoản dùng bất
thường") chốt trực tiếp ở trên; câu hỏi tham chiếu từ `admin_ai_config.md` Q1 ("Gợi ý theo bậc") cũng đã
chốt — cắt khỏi phạm vi, xem `DEC-2026-0831-remove-tiered-hints-ai-config`.

---

## 5. Danh sách yêu cầu (REQ)
| ID | Yêu cầu | Loại | Nguồn/SoT |
|---|---|---|---|
| REQ-01 | Theo dõi lượng token tiêu thụ theo phiên (F5-21) | Chức năng | `01-rd/req/ai-review.md` — F5-21 |
| REQ-02 | Ngân sách token, dự báo cạn quota, tự động khoá khi vượt (F5-25) | Chức năng | `01-rd/req/ai-review.md` — F5-25 |
| REQ-03 | Given-When-Then liên quan (—) | Chức năng | `01-rd/req/user_stories/a3_admin.md` (`US-A3-04`, GWT 3-4) |

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
| :--- | :--- |
| Yêu cầu module | `01-rd/req/ai-review.md` — F5-19, F5-21, F5-25. |
| User story | `01-rd/req/user_stories/a3_admin.md` — `US-A3-04` (GWT 3-4). |
| Khảo sát hệ thống | `01-rd/overview/system_survey.md` mục 7.3 — dòng `admin_ai_usage`. |
| Prototype | `09-layoutBase/Admin - Token AI.dc.html` — prototype. |
| Màn liên quan | `01-rd/screens/admin/ADM0301_ai_config.md` mục 4 Q1 — câu hỏi trung tâm, hai file cùng chờ một quyết định. |
