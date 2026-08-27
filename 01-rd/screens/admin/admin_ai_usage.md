# RD — Màn `admin_ai_usage` (Tiêu thụ token AI)

> Slug: `admin_ai_usage` — khớp `01-rd/overview/system_survey.md` mục 7.3 [SoT: 01-rd/overview/system_survey.md:554].
> Bounded Context: `ai-review` (F5) [SoT: 01-rd/overview/system_survey.md:554]. Actor: A3.
>
> Đối chiếu prototype: `09-layoutBase/Admin - Token AI.dc.html`. File này mô tả **hành vi và UX ở mức yêu
> cầu** — không lặp lại đặc tả chức năng đã có ở `01-rd/req/req.md` (F5-21, F5-25), chỉ trỏ tới và bổ sung
> phần đặc thù của màn. **Cùng phát hiện Q1 của `admin_ai_config.md`**: dữ liệu "Gợi ý theo bậc" ở đây (45%
> ngân sách token, đứng đầu danh sách) tái khẳng định đây là tính năng có thật trong dữ liệu mẫu, không phải
> nhắc qua một lần.

## 1. Mục đích màn hình

Theo dõi lượng token AI đã tiêu thụ theo ngày/theo tính năng/theo người học, ngân sách còn lại, dự báo cạn
quota, và bảng xếp hạng nơi tốn token nhiều nhất [SoT: 01-rd/req/req.md:303, 305, 312-319 — F5-21, F5-25].

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Theo dõi lượng token tiêu thụ theo phiên | F5-21 | `01-rd/req/req.md:303` |
| Ngân sách token, dự báo cạn quota, tự động khoá khi vượt | F5-25 | `01-rd/req/req.md:312-319` |
| Given-When-Then liên quan | — | `01-rd/req/user_stories.md:327-332` (`US-A3-04`, GWT 3-4) |

## 3. Trạng thái và cấu trúc màn (screen states)

Đối chiếu `09-layoutBase/Admin - Token AI.dc.html`:

1. **4 chỉ số tổng** — Token tháng này, Trung bình mỗi ngày, Chi phí tạm tính, Lượt gọi AI (dòng 451-456) —
   khớp F5-21.
2. **Biểu đồ token theo ngày, tách theo tính năng** — 3 dải màu: Gợi ý, Phân tích, Phỏng vấn (dòng 476-480)
   — **thiếu dải cho "Sinh testcase" (F2-14) dù dải đó có xuất hiện ở khối "Theo tính năng" phía dưới** —
   không phải xung đột nghiêm trọng, chỉ là biểu đồ chọn hiện 3/4 tính năng có khối lượng lớn nhất; dải
   "Gợi ý" tiếp tục là bằng chứng cho Câu hỏi mở Q1 của `admin_ai_config.md`.
3. **Hạn mức tháng 8** — đã dùng / hạn mức, % thanh tiến độ, còn lại, dự kiến hết ngày nào (dòng 210-215,
   525-529) — khớp F5-25 (ngân sách + dự báo).
4. **Theo tính năng** — 4 dòng: "Gợi ý theo bậc" (45%), "Phân tích bài giải" (32%), "Phỏng vấn giả lập"
   (18%), "Sinh testcase" (6%) (dòng 482-487) — "Gợi ý theo bậc" chiếm tỉ trọng **lớn nhất** trong toàn bộ
   chi phí AI theo dữ liệu mẫu, không phải một tính năng phụ nhỏ — càng cần chủ dự án xác nhận sớm (Câu hỏi
   mở Q1 của `admin_ai_config.md`) vì ảnh hưởng trực tiếp tới ước lượng chi phí vận hành AI của toàn dự án.
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
| Q1 | Cảnh báo "Một tài khoản dùng bất thường" (214 lượt/24h, gấp 7 lần trung bình) ngụ ý một cơ chế phát hiện lạm dụng theo ngưỡng thống kê — chưa có mã `Fx-nn`. Đây có phải một phần mở rộng của F5-19 (giới hạn tần suất — chặn cứng) hay một lớp giám sát riêng (cảnh báo mềm, không tự chặn, để quản trị viên tự quyết)? | `req.md` F5-19 chỉ nói giới hạn tần suất (chặn khi vượt ngưỡng cứng), không nói tới việc phát hiện và cảnh báo khi một tài khoản có mức dùng cao bất thường nhưng vẫn dưới ngưỡng chặn. | Coi là mở rộng của F5-19/F5-25 (không cần mã mới) — thêm một cảnh báo mềm dựa trên độ lệch so với trung bình, không tự khoá tài khoản (khác hẳn khoá do vượt ngân sách ở F5-25). Chốt ngưỡng cụ thể khi viết BD. | Chủ dự án |

**Q1 chính của màn này thực chất là Q1 của `admin_ai_config.md` ("Gợi ý theo bậc") — không lặp lại ở đây, chỉ
tham chiếu, vì ảnh hưởng tới cả hai màn theo cùng một quyết định.**

## 6. Ngoài phạm vi file này

- Bảng màu, spacing, component cụ thể — thuộc BD (`02-bd/screens/admin/admin_ai_usage.md`, chưa viết).
- Hợp đồng API (số liệu token theo ngày/tính năng/người dùng, cảnh báo) — thuộc DD
  (`03-dd/api/ai-review.md`, chưa viết) — **không nên viết DD cho phần số liệu "Gợi ý theo bậc" trước khi
  `admin_ai_config.md` Q1 được chủ dự án chốt**.

## 7. Tham chiếu

- `01-rd/req/req.md:303, 305, 312-319` — F5-19, F5-21, F5-25.
- `01-rd/req/user_stories.md:327-332` — `US-A3-04` (GWT 3-4).
- `01-rd/overview/system_survey.md` mục 7.3 — dòng `admin_ai_usage`.
- `09-layoutBase/Admin - Token AI.dc.html` — prototype.
- `01-rd/screens/admin/admin_ai_config.md` mục 5 Q1 — câu hỏi trung tâm, hai file cùng chờ một quyết định.
