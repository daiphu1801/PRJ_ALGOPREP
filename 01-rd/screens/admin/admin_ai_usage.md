# RD — Màn `admin_ai_usage` (Tiêu thụ token AI)

> Slug: `admin_ai_usage` — khớp `01-rd/overview/system_survey.md` mục 7.3 [SoT: 01-rd/overview/system_survey.md — mục 7.3 dòng `admin_ai_usage`].
> Bounded Context: `ai-review` (F5) [SoT: 01-rd/overview/system_survey.md — mục 7.3 dòng `admin_ai_usage`]. Actor: A3.
>
> Đối chiếu prototype: `09-layoutBase/Admin - Token AI.dc.html`. File này mô tả **hành vi và UX ở mức yêu
> cầu** — không lặp lại đặc tả chức năng đã có ở `01-rd/req/ai-review.md` (F5-21, F5-25), chỉ trỏ tới và bổ sung
> phần đặc thù của màn. **Cập nhật 2026-08-31:** "Gợi ý theo bậc" (từng chiếm 45% ngân sách token trong dữ
> liệu mẫu ở đây) đã **cắt khỏi phạm vi** — xem `DEC-2026-0831-remove-tiered-hints-ai-config` — dải "Gợi ý"
> trong biểu đồ theo ngày và dòng "Gợi ý theo bậc" (45%) trong khối "Theo tính năng" xoá khi dựng UI thật.

## 1. Mục đích màn hình

Theo dõi lượng token AI đã tiêu thụ theo ngày/theo tính năng/theo người học, ngân sách còn lại, dự báo cạn
quota, và bảng xếp hạng nơi tốn token nhiều nhất [SoT: 01-rd/req/ai-review.md — F5-21, F5-25].

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Theo dõi lượng token tiêu thụ theo phiên | F5-21 | `01-rd/req/ai-review.md` — F5-21 |
| Ngân sách token, dự báo cạn quota, tự động khoá khi vượt | F5-25 | `01-rd/req/ai-review.md` — F5-25 |
| Given-When-Then liên quan | — | `01-rd/req/user_stories/a3_admin.md` (`US-A3-04`, GWT 3-4) |

## 3. Trạng thái và cấu trúc màn (screen states)

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

## 4. Given-When-Then bổ sung ở mức màn

- **Cho** ngân sách token tháng đã dùng vượt một ngưỡng cảnh báo (ví dụ 70%), **Khi** quản trị viên mở màn
  này, **Thì** khối "Cảnh báo" hiện dự báo ngày cạn quota dựa trên tốc độ tiêu thụ hiện tại — khớp F5-25
  [SoT: 09-layoutBase/Admin - Token AI.dc.html:489-491].
- **Cho** một tài khoản gọi AI với tần suất bất thường so với trung bình, **Khi** hệ thống phát hiện, **Thì**
  cảnh báo hiện ở màn này để quản trị viên xem xét — hành vi phát hiện cụ thể (ngưỡng, thuật toán) chưa có
  mã, xem Câu hỏi mở Q1.

## 5. Câu hỏi mở

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | ~~Cảnh báo "Một tài khoản dùng bất thường"...~~ **ĐÃ CHỐT 2026-08-31 (owner instruction, theo đúng đề xuất):** mở rộng F5-19/F5-25, không cần mã mới. | — | Cảnh báo mềm dựa trên độ lệch so với trung bình, không tự khoá tài khoản (khác khoá do vượt ngân sách). Đã ghi vào `01-rd/req/ai-review.md` (amendment F5-25). Xem `DEC-2026-0831-ai-usage-anomaly-alert`. Ngưỡng cụ thể chốt khi viết BD. | Đã đóng |

**Cập nhật 2026-08-31 — cả hai câu hỏi trung tâm của file này đã đóng:** Q1 ("Một tài khoản dùng bất
thường") chốt trực tiếp ở trên; câu hỏi tham chiếu từ `admin_ai_config.md` Q1 ("Gợi ý theo bậc") cũng đã
chốt — cắt khỏi phạm vi, xem `DEC-2026-0831-remove-tiered-hints-ai-config`.

## 6. Ngoài phạm vi file này

- Bảng màu, spacing, component cụ thể — thuộc BD (`02-bd/screens/admin/admin_ai_usage.md`, chưa viết).
- Hợp đồng API (số liệu token theo ngày/tính năng/người dùng, cảnh báo) — thuộc DD
  (`03-dd/api/ai-review.md`, chưa viết). Không viết DD cho "Gợi ý theo bậc" — đã cắt khỏi phạm vi.

## 7. Tham chiếu

- `01-rd/req/ai-review.md` — F5-19, F5-21, F5-25.
- `01-rd/req/user_stories/a3_admin.md` — `US-A3-04` (GWT 3-4).
- `01-rd/overview/system_survey.md` mục 7.3 — dòng `admin_ai_usage`.
- `09-layoutBase/Admin - Token AI.dc.html` — prototype.
- `01-rd/screens/admin/admin_ai_config.md` mục 5 Q1 — câu hỏi trung tâm, hai file cùng chờ một quyết định.
