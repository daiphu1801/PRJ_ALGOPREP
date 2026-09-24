# RD (Yêu cầu hệ thống mới) — Cấu hình trợ lý AI / `ADM0301`

> Mã màn hình: `ADM0301` [SoT: 02-bd/_rules/bd-template-9sheet.md — mục 8].
> Slug chính tắc: `admin_ai_config` [SoT: 01-rd/overview/system_survey.md — mục 7.3 dòng `admin_ai_config`].
> Phạm vi/Bounded Context: `ai-review` (F5) [SoT: 01-rd/overview/system_survey.md — mục 7.3 dòng `admin_ai_config`]. Actor: A3 (Quản trị viên).
> Nguồn sự thật (SoT): `01-rd/req/ai-review.md`, `09-layoutBase/Admin - Cấu hình AI.dc.html` (prototype),
> `01-rd/req/user_stories/a3_admin.md`.
> Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

Cấu hình prompt theo phiên bản, trọng số rubric chấm bài giải, giới hạn tần suất gọi AI, và nguyên tắc trả
lời chung của AI [SoT: 01-rd/req/ai-review.md — F5-19, F5-23].

Đối chiếu prototype: `09-layoutBase/Admin - Cấu hình AI.dc.html`. File này mô tả **hành vi và UX ở mức yêu
cầu** — không lặp lại đặc tả chức năng đã có ở `01-rd/req/ai-review.md` (F5-19, F5-23), chỉ trỏ tới và bổ sung
phần đặc thù của màn.

**Phát hiện quan trọng nhất của Phase 6 — Câu hỏi mở Q1, ĐÃ CHỐT 2026-08-31**: prototype liệt kê một prompt
"Gợi ý theo bậc" (v4.2, "Đang chạy") ngang hàng với Phân tích bài giải (F5.1) và Phỏng vấn giả lập (F5.2)
— không có tính năng "gợi ý theo bậc" nào được đặc tả ở bất kỳ đâu trong `ai-review.md`/`README.md`. **Chốt: cắt
khỏi phạm vi** (`DEC-2026-0831-remove-tiered-hints-ai-config`) — xem mục 4 Q1/Q2.

---

## 2. Phạm vi

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Giới hạn tần suất gọi AI theo người dùng | F5-19 | `01-rd/req/ai-review.md` — F5-19 |
| Cấu hình prompt và rubric AI, có phiên bản, xem lại/khôi phục | F5-23 | `01-rd/req/ai-review.md` — F5-23 |
| Chống prompt injection (nguyên tắc "Không đưa lời giải đầy đủ" là ứng dụng của quy tắc này) | F5-17 | `01-rd/req/ai-review.md` — F5-17 |
| Chạy đối chiếu prompt trước khi phát hành — giữ ở tầng giao diện, chưa cam kết backend | — | `01-rd/req/ai-review.md` — F5-23 (chốt phạm vi rõ) |
| Given-When-Then liên quan | — | `01-rd/req/user_stories/a3_admin.md` (`US-A3-04`, GWT 2) |

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
5. **Trước khi phát hành (Chạy đối chiếu)** — khớp đúng phần đã chốt phạm vi ở `ai-review.md` — F5-23 (giữ ở tầng
   giao diện, backend không nằm trong cam kết đợt này) — không phải khoảng trống, đã có quyết định rõ.

### 2.1 Cấu trúc hệ thống liên quan

| Màn/tài liệu | Vai trò | Bounded Context |
|---|---|---|
| Màn hiện tại | `ADM0301` / `admin_ai_config` | `ai-review` |
| Tài liệu yêu cầu | `01-rd/req/ai-review.md` (F5-19, F5-23) | Nguồn SoT của màn |
| Prototype | `09-layoutBase/Admin - Cấu hình AI.dc.html` | Bằng chứng bố cục và trạng thái |

**Chi tiết trạng thái và cấu trúc màn:**

| Thành phần | Vai trò | Nguồn |
| :--- | :--- | :--- |
| `01-rd/screens/admin/ADM0101_overview.md` | Màn cha — nav nhóm "AI" trỏ tới màn này | [SoT: 01-rd/screens/admin/ADM0101_overview.md:160] |
| `01-rd/screens/admin/ADM0302_ai_usage.md` | Màn liên quan — cùng nhóm nav "AI", cùng chờ chung quyết định cắt "Gợi ý theo bậc" | [SoT: 01-rd/screens/admin/ADM0302_ai_usage.md:112] |
| `09-layoutBase/Admin - Cấu hình AI.dc.html` | Prototype đối chiếu chính của file này | [SoT: 09-layoutBase/Admin - Cấu hình AI.dc.html] |
| Bounded Context `ai-review` | BC sở hữu toàn bộ hành vi của màn | [SoT: 01-rd/overview/system_survey.md — mục 7.3 dòng `admin_ai_config`] |

---

## 3. Ngoài phạm vi (Out of Scope)

- Bảng màu, spacing, component cụ thể — thuộc BD (`02-bd/screens/admin/ADM0301_ai_config.md`).
- Hợp đồng API (CRUD prompt theo phiên bản, cấu hình rubric, giới hạn tần suất) — thuộc DD
  (`03-dd/api/ai-review.md`, chưa viết). Không viết DD cho "Gợi ý theo bậc" — đã cắt khỏi phạm vi 2026-08-31.
- Backend cho "Chạy đối chiếu" (regression test 30 bài mẫu) — đã chốt ngoài cam kết đợt này ở `ai-review.md` — F5-23.

---

## 4. Tiền đề và ràng buộc

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | ~~**"Gợi ý theo bậc" là một tính năng AI đầy đủ trong prototype...**~~ **ĐÃ CHỐT 2026-08-31 (owner instruction, theo đúng đề xuất):** **cắt khỏi phạm vi** — không phải tính năng thật, không cấp mã mới (không có `F5-29`). | — | Khi dựng UI thật: xoá 3 chỗ liên quan — thẻ prompt "Gợi ý theo bậc" ở đây, dòng "Gợi ý mỗi bài" ở giới hạn tần suất, mục "Gợi ý theo bậc" ở `admin_ai_usage`. Xem `DEC-2026-0831-remove-tiered-hints-ai-config`. | Đã đóng |
| Q2 | ~~Toggle "Chuyển giảng viên khi bí"...~~ **ĐÃ CHỐT 2026-08-31 — moot theo Q1:** không có luồng escalation vì tính năng "Gợi ý theo bậc" đã cắt khỏi phạm vi. | — | Xoá toggle này cùng lúc xoá thẻ "Gợi ý theo bậc" khi dựng UI thật. | Đã đóng |

---

## 5. Danh sách yêu cầu (REQ)
| ID | Yêu cầu | Loại | Nguồn/SoT |
|---|---|---|---|
| REQ-01 | Giới hạn tần suất gọi AI theo người dùng (F5-19) | Chức năng | `01-rd/req/ai-review.md` — F5-19 |
| REQ-02 | Cấu hình prompt và rubric AI, có phiên bản, xem lại/khôi phục (F5-23) | Chức năng | `01-rd/req/ai-review.md` — F5-23 |
| REQ-03 | Chống prompt injection (nguyên tắc "Không đưa lời giải đầy đủ" là ứng dụng của quy tắc này) (F5-17) | Chức năng | `01-rd/req/ai-review.md` — F5-17 |
| REQ-04 | Chạy đối chiếu prompt trước khi phát hành — giữ ở tầng giao diện, chưa cam kết backend (—) | Chức năng | `01-rd/req/ai-review.md` — F5-23 (chốt phạm vi rõ) |
| REQ-05 | Given-When-Then liên quan (—) | Chức năng | `01-rd/req/user_stories/a3_admin.md` (`US-A3-04`, GWT 2) |

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
| Yêu cầu module | `01-rd/req/ai-review.md` — F5-17, F5-19, F5-23. |
| User story | `01-rd/req/user_stories/a3_admin.md` — `US-A3-04` (GWT 2). |
| Khảo sát hệ thống | `01-rd/overview/system_survey.md` mục 7.3 — dòng `admin_ai_config`. |
| Prototype | `09-layoutBase/Admin - Cấu hình AI.dc.html` — prototype. |
| Kế hoạch | `06-plan/reports/260825-2100-ai2-solution-review-conflicts.md` — tiền lệ để một câu hỏi ảnh hưởng kiến trúc/phạm vi mở, không tự chốt. |
