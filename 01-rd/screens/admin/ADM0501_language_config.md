# RD (Yêu cầu hệ thống mới) — Cấu hình ngôn ngữ / `ADM0501`

> Mã màn hình: `ADM0501` [SoT: 02-bd/_rules/bd-template-9sheet.md — mục 8].
> Slug chính tắc: `admin_language_config` [SoT: 01-rd/overview/system_survey.md — mục 7.3 dòng `admin_language_config`].
> Bounded Context: `judge-orchestration` + `problem-bank` [SoT: 01-rd/overview/system_survey.md — mục 7.3 dòng `admin_language_config`]. Actor: A3 (Quản trị viên).
> Nguồn sự thật (SoT): `01-rd/req/problem-bank.md` (F2-10), `01-rd/req/judge-orchestration.md` (F4-11), `09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html` (prototype), `01-rd/req/user_stories/a3_admin.md` (`US-A3-04`).
> Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

Cấu hình ba ngôn ngữ hỗ trợ (hệ số thời gian, giới hạn tài nguyên mặc định, sandbox), để vận hành đúng năng
lực hạ tầng thật [SoT: 01-rd/req/problem-bank.md — F2-10; 01-rd/req/judge-orchestration.md — F4-11].

Đối chiếu prototype: `09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html`. File này mô tả **hành vi và UX
ở mức yêu cầu** — không lặp lại đặc tả chức năng đã có ở `01-rd/req/problem-bank.md` (F2-10) và
`01-rd/req/judge-orchestration.md` (F4-11), chỉ trỏ tới và bổ sung phần đặc thù của màn.

---

## 2. Phạm vi

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Hệ số nhân thời gian theo ngôn ngữ | F2-10 | `01-rd/req/problem-bank.md` — F2-10 |
| Cấu hình ngôn ngữ và giới hạn tài nguyên mặc định | F4-11 | `01-rd/req/judge-orchestration.md` — F4-11 |
| Given-When-Then liên quan | — | `01-rd/req/user_stories/a3_admin.md` (`US-A3-04`, GWT đầu) |

Đối chiếu `09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html`:

1. **Bảng ngôn ngữ hỗ trợ** — đúng 3 ngôn ngữ đã khoá (Python 3, C++ 17, Java 21, khớp `CLAUDE.md` mục
   "Submission languages"), mỗi dòng: trình biên dịch, hệ số thời gian, giới hạn thời gian tuyệt đối, bộ
   nhớ, bật/tắt (dòng 402-414) — khớp F2-10 (hệ số) + F4-11 (giới hạn theo ngôn ngữ). Hệ số mẫu (Python x3,
   C++ x1, Java x2) khớp đúng ví dụ ở `problem-bank.md` — F2-10 ("Java chậm hơn C++ nên cùng một bài phải khác hệ
   số").
2. **Giới hạn mặc định** — thời gian/bộ nhớ/kích thước output/thời gian biên dịch, áp dụng khi bài toán
   không khai báo riêng (dòng 416-421) — khớp F4-11.
3. **Sandbox** — 3 toggle: "Cho phép truy cập mạng" (mặc định tắt, ghi chú "Tắt trong mọi kỳ thi" — cùng
   phát hiện khái niệm "kỳ thi" ở `admin_queue_monitor.md` Q2, **đã chốt 2026-08-31: chỉ là nhãn ưu tiên**),
   "Giới hạn tiến trình con", "Trả stderr cho người học" (dòng 424-428). **Đã chốt 2026-08-31** — xem Câu
   hỏi mở Q1.
4. **Lưu ý khi đổi giới hạn** — prototype ghi đổi hệ số/giới hạn không tự áp dụng cho lượt nộp cũ, phải tạo
   phiên chấm lại riêng (dòng 241, liên kết sang `admin_rejudge`). **Cập nhật 2026-08-28:** `admin_rejudge`
   và F4-09 đã loại khỏi phạm vi (`DEC-2026-0828-remove-rejudge-scope`) — đổi giới hạn chỉ áp dụng cho lượt
   nộp mới từ lúc đổi trở đi (F4-11); lượt nộp cũ giữ nguyên kết quả, không có luồng chấm lại để đồng bộ.

### 2.1 Cấu trúc hệ thống liên quan

| Màn/tài liệu | Vai trò | Bounded Context |
|---|---|---|
| Màn hiện tại | `ADM0501` / `admin_language_config` | `judge-orchestration` + `problem-bank` |
| Tài liệu yêu cầu | `01-rd/req/problem-bank.md` (F2-10), `01-rd/req/judge-orchestration.md` (F4-11) | Nguồn SoT của màn |
| Prototype | `09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html` | Bằng chứng bố cục và trạng thái |

---

## 3. Ngoài phạm vi (Out of Scope)

- Bảng màu, spacing, component cụ thể — thuộc BD (`02-bd/screens/admin/ADM0501_language_config.md`, chưa viết).
- Hợp đồng API (đổi hệ số, giới hạn, sandbox) — thuộc DD (`03-dd/api/judge-orchestration.md`, chưa viết).
- Khái niệm "kỳ thi" ở ghi chú "Tắt trong mọi kỳ thi" — cùng phát hiện Q2 của
  `01-rd/screens/admin/ADM0401_queue_monitor.md`, **đã chốt 2026-08-31** (nhãn ưu tiên, không phải tính năng
  thật), không lặp lại chi tiết ở đây.

---

## 4. Tiền đề và ràng buộc

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | ~~3 toggle Sandbox (mạng, giới hạn tiến trình con, kích thước stderr)...~~ **ĐÃ CHỐT 2026-08-31 (owner instruction, theo đúng đề xuất):** mở rộng F4-11, không cần mã mới. | — | Đã liệt kê rõ 3 tham số vào `01-rd/req/judge-orchestration.md` (amendment F4-11). Xem `DEC-2026-0831-judge-orchestration-ops-details`. | Đã đóng |

---

## 5. Danh sách yêu cầu (REQ)
| ID | Yêu cầu | Loại | Nguồn/SoT |
|---|---|---|---|
| REQ-01 | Hệ số nhân thời gian theo ngôn ngữ (F2-10) | Chức năng | `01-rd/req/problem-bank.md` — F2-10 |
| REQ-02 | Cấu hình ngôn ngữ và giới hạn tài nguyên mặc định (F4-11) | Chức năng | `01-rd/req/judge-orchestration.md` — F4-11 |
| REQ-03 | Given-When-Then liên quan (—) | Chức năng | `01-rd/req/user_stories/a3_admin.md` (`US-A3-04`, GWT đầu) |

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
| Yêu cầu module | `01-rd/req/problem-bank.md` — F2-10; `01-rd/req/judge-orchestration.md` — F4-11. |
| User story | `01-rd/req/user_stories/a3_admin.md` — `US-A3-04` (GWT đầu). |
| Khảo sát hệ thống | `01-rd/overview/system_survey.md` mục 7.3 — dòng `admin_language_config`. |
| Tiêu chuẩn kỹ thuật | `CLAUDE.md` mục "Locked stack" — khoá đúng 3 ngôn ngữ. |
| Prototype | `09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html` — prototype. |
| Quyết định | `DEC-2026-0828-remove-rejudge-scope`; `DEC-2026-0831-judge-orchestration-ops-details`. |
| Màn liên quan | `01-rd/screens/admin/ADM0401_queue_monitor.md` mục 5 Q2 — khái niệm "kỳ thi" liên quan. |
