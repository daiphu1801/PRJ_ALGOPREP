# RD — Màn `my_progress` (Tiến độ của tôi)

> Slug: `my_progress` — khớp `01-rd/overview/system_survey.md:483` [SoT: 01-rd/overview/system_survey.md:483].
> Bounded Context: `identity` (F1), có đọc thêm từ `interview-bank` (F6) cho khối phỏng vấn giả lập/lý thuyết
> [SoT: 01-rd/overview/system_survey.md:483 — cột Bounded Context ghi `identity`, `interview-bank`]. Actor: A1.
>
> Đối chiếu prototype: `09-layoutBase/Tiến độ của tôi.dc.html`. File này mô tả **hành vi và UX ở mức yêu
> cầu** — không lặp lại đặc tả chức năng đã có ở `01-rd/req/req.md` (mục F1) và
> `01-rd/req/user_stories.md` (`US-A1-05`), chỉ trỏ tới và bổ sung phần đặc thù của màn.

## 1. Mục đích màn hình

Tổng hợp quá trình luyện tập của người học: bài đã giải theo chủ đề, tỉ lệ Accepted, và lịch sử phỏng vấn
giả lập mở lại được rubric của phiên cũ [SoT: 01-rd/req/req.md:37-39 — F1-06, F1-07, F1-08].

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Bài đã giải theo chủ đề | F1-06 | `01-rd/req/req.md:38` |
| Tỉ lệ chấp thuận (Accepted / tổng số đã nộp) | F1-07 | `01-rd/req/req.md:38-39` |
| Lịch sử phỏng vấn mở lại được rubric của phiên cũ | F1-08 | `01-rd/req/req.md:39` |
| Given-When-Then đầy đủ | — | `01-rd/req/user_stories.md:98-108` (`US-A1-05`, 2 GWT đầu) |

## 3. Trạng thái và cấu trúc màn (screen states)

Đối chiếu `09-layoutBase/Tiến độ của tôi.dc.html`:

1. **Dải chỉ số tổng (stats bar)** — 5 số: Đã giải (`182 / 640`), Lượt nộp (`418`, kèm `43% AC` — khớp
   F1-07), Chuỗi ngày (`12 ngày`), Số lần nộp / bài (`2.3`), Phỏng vấn (`23`, kèm `3.8 / 5`) (dòng 258-264).
   Ba trong năm số (Đã giải, Lượt nộp/AC, Phỏng vấn) khớp F1-06/F1-07/F1-08; hai số còn lại (**Chuỗi ngày**,
   **Số lần nộp / bài**) không có mã `Fx-nn` — xem Câu hỏi mở Q1.
2. **Bảng "Theo chủ đề"** — mỗi chủ đề: đã giải/tổng, thanh tiến độ, tỉ lệ AC riêng theo chủ đề, lần nộp cuối
   (dòng 121-149) — khớp F1-06, mở rộng thêm tỉ lệ AC theo từng chủ đề (không chỉ tổng, F1-07 áp theo từng
   dòng).
3. **Biểu đồ cột "Lượt nộp 14 ngày gần nhất"** (dòng 152-162) — không có mã `Fx-nn` riêng, là cách trình bày
   trực quan của cùng dữ liệu lượt nộp (F1-07); có 3 khoảng thời gian chọn được (7 ngày/30 ngày/Tất cả, dòng
   266) áp cho toàn màn (chủ đề + biểu đồ), không riêng biểu đồ.
4. **Khối "Theo độ khó"** — tiến độ Dễ/Trung bình/Khó (dòng 166-181) — khớp F1-06 (một cách chia khác của
   "theo chủ đề": theo độ khó).
5. **Khối "Nên ưu tiên" (Focus next)** — 3 gợi ý bài/chủ đề nên làm tiếp kèm lý do (`"Tỉ lệ AC 31% — thấp
   nhất"`, `"Còn 26 bài, tỉ lệ AC 44%"`, `"13 ngày chưa nộp bài nào"`, dòng 184-196) — không có mã `Fx-nn`,
   là một tính năng gợi ý/xếp hạng chưa từng được đặc tả ở `req.md` — xem Câu hỏi mở Q2.
6. **Khối "Phỏng vấn giả lập"** — số phiên, điểm trung bình, mục yếu nhất (`"Đánh đổi"` — trade-offs, dòng
   199-210) kèm liên kết bắt đầu phiên mới — khớp F1-08 (tóm tắt lịch sử), nhưng "mục yếu nhất" (rubric tổng
   hợp qua nhiều phiên) là suy luận tổng hợp chưa có mã `Fx-nn` — cùng nhóm với Q2.

## 4. Given-When-Then bổ sung ở mức màn (không trùng `US-A1-05`)

- **Cho** tôi đã có ít nhất một phiên phỏng vấn giả lập trước đó (F1-08), **Khi** tôi xem khối "Phỏng vấn giả
  lập" ở trang tiến độ, **Thì** tôi thấy số phiên, điểm trung bình, và tiêu chí rubric tôi yếu nhất tính trên
  toàn bộ lịch sử — không phải của riêng phiên gần nhất
  [SoT: 09-layoutBase/Tiến độ của tôi.dc.html:199-210].
- **Cho** tôi chọn khoảng thời gian khác ("7 ngày"/"Tất cả") ở góc trên bên phải, **Khi** tôi đổi lựa chọn,
  **Thì** bảng "Theo chủ đề" và biểu đồ lượt nộp cùng lọc lại theo khoảng đó
  [SoT: 09-layoutBase/Tiến độ của tôi.dc.html:106-108, 266].

## 5. Câu hỏi mở — đã tự chốt 2026-08-25

> Prototype này là bản dựng tham khảo; chủ dự án yêu cầu tự chốt phần suy luận thấp rủi ro thay vì để mở,
> phần thuần UI đánh dấu **[Đợi nextjs]** để dựng lại đúng khi có frontend Next.js thật.

| # | Câu hỏi | Quyết định | Ghi chú |
| :-: | :--- | :--- | :--- |
| Q1 | Chỉ số **Chuỗi ngày luyện tập** (streak) và **Số lần nộp trung bình / bài** không có mã `Fx-nn` riêng. | **Không cần mã mới** — là suy luận hiển thị từ dữ liệu lượt nộp đã có (F1-07), không phải tính năng mới. Định nghĩa: streak = số ngày liên tiếp tính đến hôm nay có ít nhất một lượt nộp; trung bình lần nộp/bài = tổng lượt nộp / số bài đã giải. | `[SoT: Suy luận]`. Cách tính chính xác cạnh biên (múi giờ tính "ngày", làm tròn số trung bình) **[Đợi nextjs]**. |
| Q2 | Khối "Nên ưu tiên" và "mục yếu nhất" (phỏng vấn) là logic gợi ý/xếp hạng chưa có mã. | **Chốt: quy tắc xếp hạng đơn giản trên dữ liệu đã có, không gọi AI.** "Nên ưu tiên": ưu tiên chủ đề có tỉ lệ AC thấp nhất, sau đó chủ đề còn nhiều bài chưa giải nhất, sau đó chủ đề lâu chưa nộp bài nhất — dẫn xuất từ F1-06/F1-07, không cần mã `Fx-nn` mới. "Mục yếu nhất" (phỏng vấn): tiêu chí rubric có điểm trung bình thấp nhất tính trên toàn bộ lịch sử phiên (F1-08 + F5-15), cũng là suy luận hiển thị, không gọi AI thêm. | `[SoT: Suy luận]`. Nếu sau này muốn nâng cấp thành gợi ý có AI, đó là mở rộng phạm vi F5/F6 mới, cần quyết định riêng — không nằm trong phạm vi hiện tại. |

## 6. Ngoài phạm vi file này

- Bảng màu, spacing, component cụ thể, breakpoint responsive — thuộc BD (`02-bd/screens/users/my_progress.md`,
  chưa viết).
- Hợp đồng API (tính tỉ lệ AC, dữ liệu biểu đồ 14 ngày, quy tắc gợi ý) — thuộc DD (`03-dd/api/identity.md`
  và/hoặc `03-dd/api/interview-bank.md`, chưa viết).
- Định nghĩa chính xác "streak" và thuật toán gợi ý "Nên ưu tiên" — phụ thuộc câu trả lời Q1/Q2, không tự
  quyết trong file RD này.

## 7. Tham chiếu

- `01-rd/req/req.md:37-39` — F1-06, F1-07, F1-08.
- `01-rd/req/user_stories.md:98-108` — `US-A1-05` (2 GWT đầu).
- `01-rd/overview/system_survey.md:483` — dòng `my_progress` trong bảng màn mục 7.
- `09-layoutBase/Tiến độ của tôi.dc.html` — prototype.
- `06-plan/nexa-plan/260824-2043-bd-screens-common-first.md` — kế hoạch Phase 0-1; Phase 2 tiếp tục ở đây.
