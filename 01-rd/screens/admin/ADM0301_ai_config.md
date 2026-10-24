# RD (Yêu cầu hệ thống mới) — Prompt và chất lượng AI / `ADM0301`

> Mã màn hình: `ADM0301` [SoT: 02-bd/_rules/bd-template-9sheet.md — mục 8].
> Slug chính tắc: `admin_ai_config` [SoT: 01-rd/overview/system_survey.md — mục 7.3 dòng `admin_ai_config`].
> Phạm vi/Bounded Context: `ai-review` (F5) [SoT: 01-rd/overview/system_survey.md — mục 7.3 dòng `admin_ai_config`]. Actor: A3 (Quản trị viên).
> Nguồn sự thật (SoT): `01-rd/req/F5_ai-review.md`, `09-layoutBase/Admin - Cấu hình AI.dc.html` (prototype),
> `01-rd/req/user_stories/a3_admin.md`.
> Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

Màn chỉnh **chất lượng** của từng tính năng AI: prompt có phiên bản, model, ~~trọng số~~ (đã bỏ 2026-10-14, DEC-2026-1014-instructor-batch5-6-nav-pagination-rubric) ~~danh sách tiêu chí rubric chấm bài giải~~ (đã bỏ 2026-10-14, DEC-2026-1014-drop-admin-rubric-config; ~~năm tiêu chí cố định trong lược đồ F5.1~~ ĐÃ BỎ 2026-10-14, DEC-2026-1014-drop-solution-review-rubric: không còn rubric chấm bài giải nào) và nguyên tắc
trả lời [SoT: 01-rd/req/F5_ai-review.md — F5-17, F5-23]. Từ 2026-10-10 (`DEC-2026-1010-split-ai-admin-screens`) màn **chỉ
còn việc này**: giới hạn tần suất, ngân sách, đơn giá, hạn mức token và trần sinh testcase chuyển sang
`01-rd/screens/admin/ADM0303_ai_limits.md`.

Tên màn: "Prompt và chất lượng AI" (slug giữ `admin_ai_config`). Nội dung chia **tab theo tính năng** (Phân tích bài giải,
Phỏng vấn giả lập, Sinh testcase), mỗi tab chỉ hiện cấu hình của tính năng đó.

Đối chiếu prototype `09-layoutBase/Admin - Cấu hình AI.dc.html`: file này mô tả **hành vi và UX ở mức yêu cầu** — không
lặp lại đặc tả đã có ở `01-rd/req/F5_ai-review.md`. Mockup vẽ trước nhiều quyết định nên chỉ còn giá trị tham khảo.

Quyết định đã chốt ảnh hưởng màn này: cắt "Gợi ý theo bậc" khỏi phạm vi (`DEC-2026-0831-remove-tiered-hints-ai-config`,
Câu hỏi mở Q1 và Q2 bên dưới); admin chọn model trong danh sách dev khai báo, không hiển thị nhiệt độ
(`DEC-2026-1010-admin-selects-model-per-prompt`); nguyên tắc trả lời theo từng tính năng, "Không đưa lời giải đầy đủ"
mặc định bật, ADMIN tắt được kèm cảnh báo (`DEC-2026-1011-ai-no-full-solution-can-be-turned-off`, thay cho "luôn bật" của `DEC-2026-1010-split-ai-admin-screens`).

---

## 2. Phạm vi

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Cấu hình prompt AI, có phiên bản, xem lại/khôi phục (~~và rubric~~ ĐÃ BỎ 2026-10-14, DEC-2026-1014-drop-admin-rubric-config; xác nhận bỏ hẳn rubric bài giải, DEC-2026-1014-drop-solution-review-rubric) | F5-23 | `01-rd/req/F5_ai-review.md` — F5-23 |
| Chọn model cho từng bản prompt trong danh sách dev khai báo | F5-23 | `DEC-2026-1010-admin-selects-model-per-prompt` |
| Chống prompt injection ("Không đưa lời giải đầy đủ" là ứng dụng, mặc định bật, tắt được kèm cảnh báo) | F5-17 | `01-rd/req/F5_ai-review.md` — F5-17 |
| ~~Chạy đối chiếu prompt trước khi phát hành — giữ ở tầng giao diện, chưa cam kết backend~~ ĐÃ BỎ HẲN 2026-10-23 (DEC-2026-1023-drop-ai-regression-run) | — | `01-rd/req/F5_ai-review.md` — F5-23 |
| Given-When-Then liên quan | — | `01-rd/req/user_stories/a3_admin.md` (`US-A3-04`, GWT 2) |

Hành vi theo tab:

1. **Prompt theo tính năng** — mỗi tính năng một bản prompt riêng, có phiên bản (v3.8...), trạng thái Đang chạy / Bản
   nháp, mô tả, model (ô chọn), giới hạn token (chỉ đọc), ngày cập nhật, nút "Chỉnh sửa". Chỉ có ba tính năng: Phân tích
   bài giải (F5.1), Phỏng vấn giả lập (F5.2), Sinh testcase (F2-14).
2. ~~**Rubric chấm bài giải** — chỉ ở tab Phân tích bài giải: danh sách tiêu chí (ban đầu 5 tiêu chí: Tính đúng đắn, Độ phức tạp, Chất lượng mã, Xử lý
   biên, Diễn giải) mà ADMIN thêm, sửa, bật hoặc tắt được.~~ **ĐÃ BỎ 2026-10-14 (DEC-2026-1014-drop-admin-rubric-config):**
   màn **không còn mục rubric**; chỉ còn prompt, model, nguyên tắc trả lời, nhật ký phiên bản và phát hành. ~~Năm tiêu chí
   chấm bài giải (`CORRECTNESS`, `PERFORMANCE`, `CLEAN_CODE`, `SCALABILITY`, `DATA_STRUCTURE`) **cố định trong lược đồ F5.1**,
   đánh phiên bản cùng bản prompt; đổi tiêu chí = phát hành bản prompt/lược đồ mới.~~ **ĐÃ BỎ 2026-10-14
   (DEC-2026-1014-drop-solution-review-rubric):** năm tiêu chí cố định cũng bỏ; không còn rubric chấm bài giải nào, cả ở màn này lẫn
   trong báo cáo học viên [SoT: 01-rd/req/F5_ai-review.md — F5-23].
   ~~Trọng số bước 5%, tổng phải bằng 100%, ô nhập trọng số,
   quy đổi có trọng số.~~ **ĐÃ BỎ 2026-10-14 (DEC-2026-1014-instructor-batch5-6-nav-pagination-rubric):** không còn trọng số phần trăm.
   Rubric phỏng vấn giả lập (4 tiêu chí cố định, F5-15) không đổi; trọng số cố định 25/30/25/20 của F5-15 là quy tắc riêng, chủ BD kiểm tra lại.
3. **Nguyên tắc trả lời** — theo từng tính năng, ở tab Phân tích bài giải và Phỏng vấn giả lập: "Trích dẫn dòng mã người
   học", "Trả lời bằng tiếng Việt" bật tắt được; "Không đưa lời giải đầy đủ" mặc định bật, tắt được kèm cảnh báo (F5-17, F5-18).
4. ~~**Trước khi phát hành** — nút "Chạy đối chiếu" hiển thị nhưng vô hiệu hoá (backend hoãn, F5-23).~~ **ĐÃ BỎ 2026-10-23 (DEC-2026-1023-drop-ai-regression-run).** Giữ số mục để không đánh số lại.
5. **Nhật ký phiên bản** — theo tính năng đang chọn, xem và khôi phục.
6. **Phát hành** — gom mọi thay đổi chưa phát hành, hiển thị số lượng, xác nhận rồi mới thay bản đang chạy.

### 2.1 Cấu trúc hệ thống liên quan

| Màn/tài liệu | Vai trò | Bounded Context |
|---|---|---|
| Màn hiện tại | `ADM0301` / `admin_ai_config` | `ai-review` |
| Tài liệu yêu cầu | `01-rd/req/F5_ai-review.md` (F5-19, F5-23) | Nguồn SoT của màn |
| Prototype | `09-layoutBase/Admin - Cấu hình AI.dc.html` | Bằng chứng bố cục và trạng thái |

**Chi tiết trạng thái và cấu trúc màn:**

| Thành phần | Vai trò | Nguồn |
| :--- | :--- | :--- |
| `01-rd/screens/admin/ADM0101_overview.md` | Màn cha — nav nhóm "AI" trỏ tới màn này | [SoT: 01-rd/screens/admin/ADM0101_overview.md:160] |
| `01-rd/screens/admin/ADM0302_ai_usage.md` | Màn liên quan — cùng nhóm nav "AI"; quyết định cắt "Gợi ý theo bậc" đã chốt 2026-08-31 (Q1 bên dưới) | [SoT: 01-rd/screens/admin/ADM0302_ai_usage.md:112] |
| `09-layoutBase/Admin - Cấu hình AI.dc.html` | Prototype đối chiếu chính của file này | [SoT: 09-layoutBase/Admin - Cấu hình AI.dc.html] |
| Bounded Context `ai-review` | BC sở hữu toàn bộ hành vi của màn | [SoT: 01-rd/overview/system_survey.md — mục 7.3 dòng `admin_ai_config`] |

---

## 3. Ngoài phạm vi (Out of Scope)

- Bảng màu, spacing, component cụ thể — thuộc BD (`02-bd/screens/admin/ADM0301_ai_config.md`).
- Hợp đồng API (CRUD prompt theo phiên bản, ~~cấu hình rubric~~ ĐÃ BỎ 2026-10-14 DEC-2026-1014-drop-solution-review-rubric, giới hạn tần suất) — thuộc DD
  (`03-dd/api/F5_ai-review.md`, chưa viết). Không viết DD cho "Gợi ý theo bậc" — đã cắt khỏi phạm vi 2026-08-31.
- "Chạy đối chiếu" (regression test 30 bài mẫu) — đã bỏ hẳn cả giao diện lẫn backend 2026-10-23 (`DEC-2026-1023-drop-ai-regression-run`), xem `F5_ai-review.md` — F5-23.

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
| REQ-01 | ~~Giới hạn tần suất gọi AI theo người dùng (F5-19)~~ **Chuyển sang `ADM0303` REQ-03 (2026-10-10)** | Chức năng | `01-rd/screens/admin/ADM0303_ai_limits.md` |
| REQ-02 | Cấu hình prompt ~~và rubric~~ AI, có phiên bản, xem lại/khôi phục (F5-23). Phần rubric ĐÃ BỎ 2026-10-14 (DEC-2026-1014-drop-solution-review-rubric) | Chức năng | `01-rd/req/F5_ai-review.md` — F5-23 |
| REQ-03 | Chống prompt injection (nguyên tắc "Không đưa lời giải đầy đủ" là ứng dụng của quy tắc này) (F5-17) | Chức năng | `01-rd/req/F5_ai-review.md` — F5-17 |
| REQ-04 | ~~Chạy đối chiếu prompt trước khi phát hành — giữ ở tầng giao diện, chưa cam kết backend (—)~~ ĐÃ BỎ HẲN 2026-10-23 (DEC-2026-1023-drop-ai-regression-run) | Chức năng | `01-rd/req/F5_ai-review.md` — F5-23 |
| REQ-05 | Given-When-Then liên quan (—) | Chức năng | `01-rd/req/user_stories/a3_admin.md` (`US-A3-04`, GWT 2) |
| REQ-06 | ADMIN chọn model cho từng bản prompt trong danh sách model do dev khai báo; khoá API nằm ở hạ tầng backend, không qua giao diện; không hiển thị nhiệt độ (thêm 2026-10-10) | Chức năng | `DEC-2026-1010-admin-selects-model-per-prompt`; `01-rd/req/F5_ai-review.md` — F5-23 |

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
| BD màn hình | `02-bd/screens/admin/ADM0301_ai_config.md` V1.8 |
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
| Yêu cầu module | `01-rd/req/F5_ai-review.md` — F5-17, F5-19, F5-23. |
| User story | `01-rd/req/user_stories/a3_admin.md` — `US-A3-04` (GWT 2). |
| Khảo sát hệ thống | `01-rd/overview/system_survey.md` mục 7.3 — dòng `admin_ai_config`. |
| Prototype | `09-layoutBase/Admin - Cấu hình AI.dc.html` — prototype. |
| Kế hoạch | `06-plan/reports/260825-2100-ai2-solution-review-conflicts.md` — tiền lệ để một câu hỏi ảnh hưởng kiến trúc/phạm vi mở, không tự chốt. |
