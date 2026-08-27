# RD — Màn `admin_ai_config` (Cấu hình trợ lý AI)

> Slug: `admin_ai_config` — khớp `01-rd/overview/system_survey.md` mục 7.3 [SoT: 01-rd/overview/system_survey.md:553].
> Bounded Context: `ai-review` (F5) [SoT: 01-rd/overview/system_survey.md:553]. Actor: A3.
>
> Đối chiếu prototype: `09-layoutBase/Admin - Cấu hình AI.dc.html`. File này mô tả **hành vi và UX ở mức yêu
> cầu** — không lặp lại đặc tả chức năng đã có ở `01-rd/req/req.md` (F5-19, F5-23), chỉ trỏ tới và bổ sung
> phần đặc thù của màn.
> **Phát hiện quan trọng nhất của Phase 6 — xem Câu hỏi mở Q1**: prototype liệt kê một prompt "Gợi ý theo
> bậc" (v4.2, "Đang chạy") ngang hàng với Phân tích bài giải (F5.1) và Phỏng vấn giả lập (F5.2) — nhưng
> **không có tính năng "gợi ý theo bậc" nào được đặc tả ở bất kỳ đâu trong `req.md`/`README.md`**. Đây là
> một tính năng AI hoàn chỉnh (có ngân sách token riêng, thống kê riêng ở `admin_ai_usage`) mà toàn bộ RD
> hiện tại không hề biết tới.

## 1. Mục đích màn hình

Cấu hình prompt theo phiên bản, trọng số rubric chấm bài giải, giới hạn tần suất gọi AI, và nguyên tắc trả
lời chung của AI [SoT: 01-rd/req/req.md:303, 315-319, 322-335 — F5-19, F5-23].

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Giới hạn tần suất gọi AI theo người dùng | F5-19 | `01-rd/req/req.md:303` |
| Cấu hình prompt và rubric AI, có phiên bản, xem lại/khôi phục | F5-23 | `01-rd/req/req.md:315-319` |
| Chống prompt injection (nguyên tắc "Không đưa lời giải đầy đủ" là ứng dụng của quy tắc này) | F5-17 | `01-rd/req/req.md:305-306` |
| Chạy đối chiếu prompt trước khi phát hành — giữ ở tầng giao diện, chưa cam kết backend | — | `01-rd/req/req.md:322-334` (chốt phạm vi rõ) |
| Given-When-Then liên quan | — | `01-rd/req/user_stories.md:317-326` (`US-A3-04`, GWT 2) |

## 3. Trạng thái và cấu trúc màn (screen states)

Đối chiếu `09-layoutBase/Admin - Cấu hình AI.dc.html`:

1. **Prompt theo tính năng** — 4 thẻ: "Gợi ý theo bậc" (v4.2, Đang chạy), "Phân tích bài giải" (v3.8, Đang
   chạy — khớp F5.1), "Phỏng vấn giả lập" (v2.5, Thử nghiệm A/B — khớp F5.2), "Sinh testcase" (v1.9, Bản
   nháp — khớp F2-14). Mỗi thẻ: mô tả, model, nhiệt độ, giới hạn token, ngày cập nhật, nút "Chỉnh sửa" (dòng
   420-429) — khớp đúng cấu trúc "một bản prompt riêng, có phiên bản" của F5-23. **"Gợi ý theo bậc" là thẻ
   đầu tiên, có đầy đủ dữ liệu như ba thẻ có mã thật — xem Câu hỏi mở Q1.**
2. **Rubric chấm bài giải** — 5 tiêu chí (Tính đúng đắn, Độ phức tạp, Chất lượng mã, Xử lý biên, Diễn giải)
   kèm trọng số +/- và tổng phải bằng 100% (dòng 443-449) — **5 tiêu chí, khác 4 tiêu chí rubric phỏng vấn
   giả lập đã chốt ở F5-15** (độ rõ ràng, độ chính xác kỹ thuật, khả năng phản biện, nhận thức độ phức tạp)
   — hai rubric riêng cho hai tính năng khác nhau (F5.1 vs F5.2), không xung đột, chỉ ghi nhận để BD không
   nhầm dùng chung một rubric.
3. **Giới hạn tần suất** — 4 dòng: Gợi ý mỗi bài (3 lượt), Phân tích mỗi ngày (10 lượt), Phỏng vấn mỗi tuần
   (5 phiên), Chờ giữa hai yêu cầu (20 giây) (dòng 451-456) — dòng đầu ("Gợi ý mỗi bài") lại củng cố Câu hỏi
   mở Q1; ba dòng còn lại khớp F5-19.
4. **Nguyên tắc trả lời** — 4 toggle: "Không đưa lời giải đầy đủ", "Trích dẫn dòng mã người học", "Trả lời
   bằng tiếng Việt", "Chuyển giảng viên khi bí" (dòng 465-469). Ba toggle đầu khớp tinh thần chống lộ lời
   giải + định hướng giáo dục (F5-17, F5-18). **Toggle "Chuyển giảng viên khi bí" ngụ ý một luồng escalation
   từ AI sang giảng viên chưa từng được đặc tả** — xem Câu hỏi mở Q2.
5. **Trước khi phát hành (Chạy đối chiếu)** — khớp đúng phần đã chốt phạm vi ở `req.md:322-334` (giữ ở tầng
   giao diện, backend không nằm trong cam kết đợt này) — không phải khoảng trống, đã có quyết định rõ.

## 4. Given-When-Then bổ sung ở mức màn

- **Cho** quản trị viên chỉnh trọng số rubric, **Khi** tổng các trọng số khác 100%, **Thì** màn hiện cảnh
  báo (số tổng đổi màu) — khớp `weightFg`/`weightTotal` trong prototype
  [SoT: 09-layoutBase/Admin - Cấu hình AI.dc.html:195-196, 449, 474].
- **Cho** một prompt đang ở trạng thái "Bản nháp" (ví dụ "Sinh testcase" v1.9), **Khi** quản trị viên bấm
  "Chỉnh sửa", **Thì** thay đổi không ảnh hưởng tới phiên bản "Đang chạy" cho tới khi được publish — khớp mô
  hình versioning đã chốt ở F5-23.

## 5. Câu hỏi mở

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | **"Gợi ý theo bậc" là một tính năng AI đầy đủ trong prototype (prompt riêng có phiên bản, giới hạn tần suất riêng, ngân sách token riêng ở `admin_ai_usage`) nhưng hoàn toàn không có trong `req.md`/`README.md`.** Đây có phải tính năng thật (gợi ý dần từng bậc khi người học đang giải một bài, trước khi nộp/Accepted — khác hẳn Solution Review F5.1 chỉ chạy sau Accepted) cần một mã `Fx-nn` mới (có thể F5-29 hoặc một nhóm F5.0 mới) và RD riêng, hay là tính năng đã bị cắt khỏi phạm vi đồ án nhưng chưa dọn khỏi prototype? | Đây là phát hiện có ảnh hưởng lớn nhất trong Phase 6 — nếu là tính năng thật, nó thêm hẳn một luồng AI thứ ba (ngoài F5.1, F5.2) chạy *trong khi giải bài*, khác vị trí kích hoạt với F5.1/F5.2 (đều chạy sau khi có bài nộp) — ảnh hưởng tới `problem_detail`/Workspace, không chỉ `ai-review`. Không tự quyết được vì mở/đóng một tính năng AI thứ ba là quyết định phạm vi lớn, giống việc AI-2 để mở Q2 của `solution_review` (`06-plan/reports/260825-2100-ai2-solution-review-conflicts.md`). | Nếu là tính năng thật: mở một RD riêng cho luồng "Gợi ý theo bậc" (có thể là một phần của `problem_detail`/Workspace hoặc một màn mới), gán mã mới, và xác nhận vị trí kích hoạt (trong Workspace, trước khi nộp). Nếu đã cắt khỏi phạm vi: xoá 3 thẻ liên quan (thẻ prompt ở đây, dòng "Gợi ý mỗi bài" ở giới hạn tần suất, cột "Gợi ý"/features "Gợi ý theo bậc" ở `admin_ai_usage`) khi dựng UI thật. | Chủ dự án |
| Q2 | Toggle "Chuyển giảng viên khi bí" (sau 3 lần gợi ý không hiệu quả) ngụ ý một luồng escalation từ AI sang giảng viên con người — chưa từng được đặc tả ở F5 hay ở bất kỳ US-A2 nào (giảng viên không có story nào về "nhận yêu cầu hỗ trợ từ AI"). | Phụ thuộc trực tiếp vào câu trả lời Q1 (toggle này thuộc khối "Gợi ý theo bậc") — nếu Q1 xác nhận tính năng gợi ý không tồn tại, Q2 tự động không áp dụng. | Gộp xử lý cùng Q1 — chỉ cần trả lời riêng nếu Q1 xác nhận "Gợi ý theo bậc" là tính năng thật và cần escalation tới giảng viên. | Chủ dự án |

## 6. Ngoài phạm vi file này

- Bảng màu, spacing, component cụ thể — thuộc BD (`02-bd/screens/admin/admin_ai_config.md`, chưa viết).
- Hợp đồng API (CRUD prompt theo phiên bản, cấu hình rubric, giới hạn tần suất) — thuộc DD
  (`03-dd/api/ai-review.md`, chưa viết) — **không nên viết DD cho phần "Gợi ý theo bậc" trước khi Q1 được
  chủ dự án chốt**.
- Backend cho "Chạy đối chiếu" (regression test 30 bài mẫu) — đã chốt ngoài cam kết đợt này ở `req.md:328-335`.

## 7. Tham chiếu

- `01-rd/req/req.md:303, 305-306, 315-319, 322-335` — F5-17, F5-19, F5-23.
- `01-rd/req/user_stories.md:317-326` — `US-A3-04` (GWT 2).
- `01-rd/overview/system_survey.md` mục 7.3 — dòng `admin_ai_config`.
- `09-layoutBase/Admin - Cấu hình AI.dc.html` — prototype.
- `06-plan/reports/260825-2100-ai2-solution-review-conflicts.md` — tiền lệ để một câu hỏi ảnh hưởng kiến
  trúc/phạm vi mở, không tự chốt.
