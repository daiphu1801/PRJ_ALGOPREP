# RD (Yêu cầu hệ thống mới) — Thiết lập / `USR0503`

> Mã màn hình: `USR0503` [Nguồn: `02-bd/_rules/bd-template-9sheet.md` — mục 8].
> Slug chính tắc: `settings` [SoT: `01-rd/overview/system_survey.md` — mục 7.1 dòng `settings`].
> Phạm vi/Bounded Context: `identity` (F1) [SoT: `01-rd/overview/system_survey.md` — mục 7.1 dòng `settings`].
> Actor: A1.
> Nguồn sự thật (SoT): `01-rd/req/identity.md` (F1-16, F1-20, F1-21, F1-22), `01-rd/req/ai-review.md` (F5-28), `01-rd/req/user_stories/a1_student.md` (`US-A1-05`, `US-A1-07`), `09-layoutBase/Cài đặt.dc.html` (prototype).
> Tài liệu này mô tả hành vi và UX ở mức yêu cầu, không lặp lại đặc tả chức năng nguồn.
> Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

Cấu hình các tuỳ chọn của tài khoản: giao diện (đã chốt ở quyết định riêng), workspace giải bài, phỏng vấn giả lập, thông báo email, xuất dữ liệu, và khu vực tự xoá tài khoản (danger zone).

File này mô tả **hành vi và UX ở mức yêu cầu** — không lặp lại đặc tả chức năng đã có ở `01-rd/req/identity.md` (mục F1), chỉ trỏ tới và bổ sung phần đặc thù của màn, đối chiếu prototype `09-layoutBase/Cài đặt.dc.html`.

**Phát hiện quan trọng nhất của Phase 2, đã tự chốt 2026-08-25 — xem chương 4 Q1**: `system_survey.md` đã tự ghi nhận `settings` "chỉ gán được F1-16" — prototype thật có **5 nhóm cài đặt**, 4/5 nhóm (Workspace, Phỏng vấn giả lập, Thông báo, Xuất dữ liệu) không có mã `Fx-nn` nào. Đã lấp bằng **F1-20, F1-21, F1-22, F5-28** theo yêu cầu trực tiếp của chủ dự án — tự quyết vì prototype chỉ là bản dựng tham khảo, chi tiết UI thuần hình thức đánh dấu **[Đợi nextjs]**.

---

## 2. Phạm vi

| Hành vi | Mã | Nguồn |
|---|---|---|
| Tự xoá tài khoản (danger zone) | F1-16 | `01-rd/req/identity.md` — F1-16 |
| Theme Sáng/Tối, mặc định Sáng | `DEC-2026-0824-dark-light-theme` | Decision registry |
| Ngôn ngữ giao diện VI/EN | `DEC-2026-0824-i18n-vi-en` | Decision registry |
| Tuỳ chọn cá nhân hoá Workspace (ngôn ngữ mặc định, cỡ chữ, autosave, Vim) | F1-20 | `01-rd/req/identity.md` — F1-20 (khối bổ sung 2026-08-25) |
| Thông báo email định kỳ (nhắc luyện tập, báo cáo tuần) | F1-21 | `01-rd/req/identity.md` — F1-21 (khối bổ sung 2026-08-25) |
| Xuất dữ liệu cá nhân (CSV/JSON) | F1-22 | `01-rd/req/identity.md` — F1-22 (khối bổ sung 2026-08-25) |
| Tự chỉnh tham số phiên tự luyện (mức độ, số lượt, gợi ý) | F5-28 | `01-rd/req/ai-review.md` — F5-28 (khối F5.2, bổ sung 2026-08-25) |
| Given-When-Then liên quan | — | `01-rd/req/user_stories/a1_student.md` (`US-A1-05`, GWT cuối + 3 GWT mới; `US-A1-07` GWT F5-28) |

Đối chiếu `09-layoutBase/Cài đặt.dc.html`, năm nhóm cài đặt cộng khu nguy hiểm (dòng 214-261):

1. **Giao diện** — Chủ đề màu (Sáng/Tối), Ngôn ngữ giao diện (Tiếng Việt/English), áp dụng ngay (không cần bấm Lưu) và ghi nhớ theo `localStorage` — khớp `DEC-2026-0824-dark-light-theme` và `DEC-2026-0824-i18n-vi-en`. **Đã có mã/quyết định, không phải khoảng trống.**
2. **Workspace** — Ngôn ngữ mặc định (Python 3/Java 21/C++ 17), Cỡ chữ editor, Tự lưu bản nháp (toggle), Phím tắt Vim (toggle) (dòng 227-238). Bốn tuỳ chọn này cấu hình hành vi của Monaco Editor ở màn `Workspace giải bài` (F3/F4) — **khớp F1-20** (chốt 2026-08-25).
3. **Phỏng vấn giả lập** — Mức người phỏng vấn (Junior/Middle/Senior), Số lượt tối đa mỗi phiên (8/12/16), Cho phép gợi ý (toggle) (dòng 240-250). Tham số này **khớp F5-28** (chốt 2026-08-25) — **chỉ áp dụng cho hai lối vào tự luyện của F5-24**, không áp dụng cho phiên mở từ bài nộp `Accepted` (F5-09), để giữ tính khách quan của phiên gắn với một bài giải thật.
4. **Thông báo** — Nhắc luyện tập (toggle, email khi chuỗi ngày sắp mất), Báo cáo hằng tuần (toggle, tiến độ theo chủ đề gửi thứ Hai) (dòng 253-259) — **khớp F1-21** (chốt 2026-08-25), là job định kỳ mới thuộc `identity`, khác hẳn F1-17 (email OTP theo yêu cầu người dùng).
5. **Xuất dữ liệu** (sidebar, dòng 144-153) — "Xuất lượt nộp" (CSV), "Xuất hội thoại phỏng vấn" (JSON) — **khớp F1-22** (chốt 2026-08-25), chỉ xuất dữ liệu của chính người dùng đang đăng nhập.
6. **Vùng nguy hiểm (Danger zone)** — nút "Xoá tài khoản" — hành vi thật phải đúng F1-16 (khoá mềm → ẩn danh hoá sau khoảng ân hạn, dữ liệu không mất). **Câu chữ hiện tại của prototype ("xoá vĩnh viễn... cùng toàn bộ lượt nộp, bài đã lưu, phiên phỏng vấn", dòng 128-129) sai so với hành vi đã chốt** — đã chốt sửa lại câu chữ khi dựng UI thật, xem chương 4 Q2 **[Đợi nextjs]**.

### 2.1 Cấu trúc hệ thống liên quan

| Màn/tài liệu | Vai trò | Bounded Context |
|---|---|---|
| Màn hiện tại | `USR0503` / `settings` | `identity` |
| Tài liệu yêu cầu | `01-rd/req/identity.md` (F1-16, F1-20, F1-21, F1-22), `01-rd/req/ai-review.md` (F5-28) | Nguồn SoT của màn |
| Prototype | `09-layoutBase/Cài đặt.dc.html` | Bằng chứng bố cục và trạng thái |

**Chi tiết trạng thái và cấu trúc màn**

| Thành phần | Vai trò | Nguồn |
|---|---|---|
| `09-layoutBase/Cài đặt.dc.html` | Prototype đối chiếu bố cục và trạng thái năm nhóm cài đặt | [SoT: 09-layoutBase/Cài đặt.dc.html] |
| Bounded Context `identity` (F1) | BC sở hữu dữ liệu và logic đứng sau màn | [SoT: 01-rd/overview/system_survey.md — mục 7.1 dòng `settings`] |
| `DEC-2026-0824-dark-light-theme`, `DEC-2026-0824-i18n-vi-en` | Quyết định chi phối nhóm cài đặt Giao diện | Decision registry |

---

## 3. Ngoài phạm vi (Out of Scope)

- Bảng màu, spacing, component cụ thể, breakpoint responsive — thuộc BD (`02-bd/screens/users/USR0503_settings.md`, chưa viết — **không nên mở trước khi Q1/Q2 được chốt**, vì cả hai ảnh hưởng trực tiếp tới nội dung BD phải đặc tả).
- Hợp đồng API (lưu từng nhóm cài đặt, xuất dữ liệu, huỷ tài khoản) — thuộc DD (`03-dd/api/identity.md`, chưa viết).
- Job định kỳ gửi email nhắc luyện tập/báo cáo hằng tuần (nếu được xác nhận ở Q1) — thuộc `03-dd/jobs/`, chưa viết, và chưa tồn tại vì phụ thuộc câu trả lời Q1.

---

## 4. Tiền đề và ràng buộc

> Tự quyết theo yêu cầu trực tiếp của chủ dự án — prototype là bản dựng tham khảo, phần thuần UI đánh dấu **[Đợi nextjs]** để dựng lại đúng khi có frontend Next.js thật.

| # | Câu hỏi | Quyết định | Ghi chú |
|---|---|---|---|
| Q1 | 4/5 nhóm cài đặt (Workspace, Phỏng vấn giả lập, Thông báo, Xuất dữ liệu) không có mã `Fx-nn`. | **Chốt theo nhóm, tất cả giữ trong phạm vi đồ án:** (a) Workspace → **F1-20**; (b) Phỏng vấn giả lập tự chỉnh tham số → **F5-28**, chỉ áp dụng cho lối vào tự luyện (F5-24), không áp dụng cho phiên từ bài nộp `Accepted` (F5-09); (c) Thông báo email định kỳ → **F1-21**, job thuộc `identity`; (d) Xuất dữ liệu → **F1-22**. Cả bốn đã ghi vào `01-rd/req/identity.md` (F5-28 vào `ai-review.md`), GWT tương ứng vào `user_stories/a1_student.md`, đồng bộ `01-rd/overview/system_survey.md` — mục 7.1 các dòng `my_submissions`, `profile`, `settings`. | Đã chốt, không còn mở. |
| Q2 | Câu chữ "Vùng nguy hiểm" nói xoá vĩnh viễn, sai so với F1-16 đã chốt (dữ liệu không mất, chỉ ẩn danh hoá). | **Chốt: câu chữ đúng phải theo hành vi F1-16** — ví dụ "Tài khoản chuyển sang trạng thái ngừng hoạt động ngay; thông tin định danh của bạn sẽ được ẩn danh hoá sau [N] ngày, còn lượt nộp/bài đã lưu/phiên phỏng vấn của bạn vẫn được giữ lại". Đã ghi vào `identity.md`. | Sửa câu chữ thật trên UI **[Đợi nextjs]** — không sửa `09-layoutBase/Cài đặt.dc.html` (prototype tĩnh, không phải đích cuối). |

---

## 5. Danh sách yêu cầu (REQ)

| ID | Yêu cầu | Loại | Nguồn/SoT |
|---|---|---|---|
| REQ-01 | Tự xoá tài khoản (danger zone) (F1-16) | Chức năng | `01-rd/req/identity.md` — F1-16 |
| REQ-02 | Theme Sáng/Tối, mặc định Sáng (`DEC-2026-0824-dark-light-theme`) | Chức năng | Decision registry |
| REQ-03 | Ngôn ngữ giao diện VI/EN (`DEC-2026-0824-i18n-vi-en`) | Chức năng | Decision registry |
| REQ-04 | Tuỳ chọn cá nhân hoá Workspace (ngôn ngữ mặc định, cỡ chữ, autosave, Vim) (F1-20) | Chức năng | `01-rd/req/identity.md` — F1-20 (khối bổ sung 2026-08-25) |
| REQ-05 | Thông báo email định kỳ (nhắc luyện tập, báo cáo tuần) (F1-21) | Chức năng | `01-rd/req/identity.md` — F1-21 (khối bổ sung 2026-08-25) |
| REQ-06 | Xuất dữ liệu cá nhân (CSV/JSON) (F1-22) | Chức năng | `01-rd/req/identity.md` — F1-22 (khối bổ sung 2026-08-25) |
| REQ-07 | Tự chỉnh tham số phiên tự luyện (mức độ, số lượt, gợi ý) (F5-28) | Chức năng | `01-rd/req/ai-review.md` — F5-28 (khối F5.2, bổ sung 2026-08-25) |
| REQ-08 | Given-When-Then liên quan (—) | Chức năng | `01-rd/req/user_stories/a1_student.md` (`US-A1-05`, GWT cuối + 3 GWT mới; `US-A1-07` GWT F5-28) |
| REQ-09 | **Cho** tôi đổi Chủ đề màu hoặc Ngôn ngữ giao diện, **Khi** tôi chọn giá trị mới, **Thì** thay đổi áp dụng ngay lập tức trên mọi màn hình, không cần bấm nút "Lưu cài đặt" | Chức năng (GWT) | [SoT: `09-layoutBase/Cài đặt.dc.html`:176, 209-212, 264-266] |
| REQ-10 | **Cho** tôi đổi các mục còn lại (Workspace, Phỏng vấn giả lập, Thông báo), **Khi** tôi chưa bấm "Lưu cài đặt", **Thì** thay đổi chưa có hiệu lực thật — khác nhóm Giao diện vốn áp dụng ngay | Chức năng (GWT) | [SoT: `09-layoutBase/Cài đặt.dc.html`:140, 264-266] |
| REQ-11 | **Cho** tôi bấm "Xoá tài khoản" ở vùng nguy hiểm, **Khi** tôi xác nhận, **Thì** hành vi phải đúng F1-16 đã chốt (khoá mềm → ẩn danh hoá sau khoảng ân hạn, dữ liệu không mất) — không đúng như câu chữ hiện tại của prototype | Chức năng (GWT) | [SoT: `01-rd/req/identity.md` — F1-16; xem chương 4 Q2] |

---

## 6. Yêu cầu dữ liệu

### 6.1 Dữ liệu tham chiếu

Chi tiết bảng, cột, kiểu dữ liệu và ràng buộc lưu trữ thuộc BD/DD; tài liệu này không tự suy luận dữ liệu chưa được chốt.

### 6.2 Luồng dữ liệu

Hợp đồng API, request/response và mã lỗi thuộc DD. Luồng dữ liệu mức yêu cầu được thể hiện bởi các hành vi ở chương 5.

---

## 7. RACI và danh sách bàn giao

| Bàn giao | Thực hiện | Rà soát | Trạng thái |
|---|---|---|---|
| RD màn `settings` | Nhóm phát triển | Chủ dự án | Có tài liệu này |
| BD màn `settings` | Nhóm phát triển | Chưa xác định — không tự suy luận | Chưa viết |
| DD API `identity` | Nhóm phát triển | Chưa xác định — không tự suy luận | Chưa viết |

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
| Yêu cầu module | `01-rd/req/identity.md` — F1-16 (đã chốt), F1-20, F1-21, F1-22; `01-rd/req/ai-review.md` — F5-28 |
| User story | `01-rd/req/user_stories/a1_student.md` — `US-A1-05` (GWT cuối), `US-A1-07` |
| Khảo sát hệ thống | `01-rd/overview/system_survey.md` — mục 7.1 dòng `settings`, cùng ghi chú tách `profile`/`settings` ngay dưới bảng |
| Quyết định | `DEC-2026-0824-dark-light-theme`, `DEC-2026-0824-i18n-vi-en` |
| Prototype | `09-layoutBase/Cài đặt.dc.html` |
| Báo cáo kế hoạch | `06-plan/reports/260825-1500-report-ai1-phase2-conflicts.md` — báo cáo xung đột + quyết định đã chốt, đánh dấu AI-1 |
| Quy chuẩn | `01-rd/RD_LAYOUT_RULES.md` |
