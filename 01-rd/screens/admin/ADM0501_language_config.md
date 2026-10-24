# RD (Yêu cầu hệ thống mới) — Cấu hình ngôn ngữ / `ADM0501`

> Mã màn hình: `ADM0501` [SoT: 02-bd/_rules/bd-template-9sheet.md — mục 8].
> Slug chính tắc: `admin_language_config` [SoT: 01-rd/overview/system_survey.md — mục 7.3 dòng `admin_language_config`].
> Phạm vi/Bounded Context: `judge-orchestration` + `problem-bank` [SoT: 01-rd/overview/system_survey.md — mục 7.3 dòng `admin_language_config`]. Actor: A3 (Quản trị viên).
> Nguồn sự thật (SoT): `01-rd/req/F2_problem-bank.md` (F2-10), `01-rd/req/F4_judge-orchestration.md` (F4-11), `09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html` (prototype), `01-rd/req/user_stories/a3_admin.md` (`US-A3-04`).
> Phiên bản RD: sửa 2026-10-10 theo `DEC-2026-1010-admin-operations-regroup`. Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

Cấu hình ba ngôn ngữ hỗ trợ (hệ số thời gian, giới hạn tài nguyên mặc định, sandbox), để vận hành đúng năng
lực hạ tầng thật [SoT: 01-rd/req/F2_problem-bank.md — F2-10; 01-rd/req/F4_judge-orchestration.md — F4-11].

Màn chia **hai tab** để phần dùng hằng ngày ngắn gọn: tab **"Ngôn ngữ và giới hạn"** (bảng ba ngôn ngữ và hàng
giới hạn mặc định) và tab **"Sandbox và sinh testcase"** (ba tham số sandbox, giới hạn script sinh testcase;
gần như không đổi sau khi cài). Một thanh lưu dính đáy cho cả hai tab.

Đối chiếu prototype: `09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html` (chỉ tham khảo từ 2026-10-03). File này mô tả **hành vi và UX
ở mức yêu cầu** — không lặp lại đặc tả chức năng đã có ở `01-rd/req/F2_problem-bank.md` (F2-10) và
`01-rd/req/F4_judge-orchestration.md` (F4-11), chỉ trỏ tới và bổ sung phần đặc thù của màn.

---

## 2. Phạm vi

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Hệ số nhân thời gian theo ngôn ngữ | F2-10 | `01-rd/req/F2_problem-bank.md` — F2-10 |
| Cấu hình ngôn ngữ và giới hạn tài nguyên mặc định | F4-11 | `01-rd/req/F4_judge-orchestration.md` — F4-11 |
| Given-When-Then liên quan | — | `01-rd/req/user_stories/a3_admin.md` (`US-A3-04`, GWT đầu) |

Đối chiếu `09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html`:

1. **Bảng ngôn ngữ hỗ trợ** — đúng 3 ngôn ngữ đã khoá (Python 3, C++ 17, Java 21, khớp `CLAUDE.md` mục
   "Submission languages"). **Sửa 2026-10-10: bảng 4 cột** — Ngôn ngữ (tên kèm trình biên dịch ở dòng phụ), Hệ số
   (thời gian và RAM), Giới hạn hiệu dụng ("x ms · y MB" trong một ô), Bật. Cột "Mã môi trường chấm"
   (`go-judge #71`...) **bỏ**: chỉ là chuỗi trang trí của bản mẫu, không có nguồn dữ liệu — khớp F2-10 (hệ số) + F4-11 (giới hạn theo ngôn ngữ). Hệ số mẫu (Python x3,
   C++ x1, Java x2) khớp đúng ví dụ ở `F2_problem-bank.md` — F2-10 ("Java chậm hơn C++ nên cùng một bài phải khác hệ
   số").
2. **Giới hạn mặc định** — thời gian/bộ nhớ/kích thước output/thời gian biên dịch, áp dụng khi bài toán
   không khai báo riêng — khớp F4-11. **Sửa 2026-10-10:** là **bốn ô nhập sửa được**, đặt thành một hàng gọn ngay
   trên bảng (chúng là đầu vào của cột "Giới hạn hiệu dụng"), không còn là thẻ riêng chỉ đọc
   (`DEC-2026-1001-admin-configurable-settings`).
3. **Sandbox** (chuyển sang tab "Sandbox và sinh testcase", **áp dụng toàn cục**, không theo ngôn ngữ) — 3 toggle: "Cho phép truy cập mạng" (mặc định tắt, ghi chú "Tắt trong mọi kỳ thi" — cùng
   phát hiện khái niệm "kỳ thi" ở `admin_queue_monitor.md` Q2, **đã chốt 2026-08-31: chỉ là nhãn ưu tiên**),
   "Giới hạn tiến trình con", "Trả stderr cho người học" (dòng 424-428). **Đã chốt 2026-08-31** — xem Câu
   hỏi mở Q1.
4. **Lưu ý khi đổi giới hạn** — prototype ghi đổi hệ số/giới hạn không tự áp dụng cho lượt nộp cũ, phải tạo
   phiên chấm lại riêng (dòng 241, liên kết sang `admin_rejudge`). **Cập nhật 2026-08-28:** `admin_rejudge`
   và F4-09 đã loại khỏi phạm vi (`DEC-2026-0828-remove-rejudge-scope`) — đổi giới hạn chỉ áp dụng cho lượt
   nộp mới; lượt nộp cũ giữ nguyên kết quả, không có luồng chấm lại để đồng bộ. **Sửa 2026-10-10:** thẻ lưu ý
   **bỏ**, thay bằng một dòng ghi chú cạnh nút Lưu: "Áp dụng cho lượt chấm bắt đầu từ lúc lưu". Mốc áp dụng chốt
   là **lúc worker bắt đầu chấm** job, không phải lúc nộp; job đã bắt đầu chấm thì chạy nốt cấu hình cũ.
5. **Thanh lưu dính đáy** — hiện số thay đổi chưa lưu, nút "Hoàn tác" và "Lưu". Hộp xác nhận **một lần duy nhất
   lúc Lưu**, chỉ khi có ngôn ngữ chuyển từ bật sang tắt. Luôn phải còn ít nhất một ngôn ngữ bật; tắt một
   ngôn ngữ không huỷ job đang chờ của ngôn ngữ đó, chỉ chặn bài nộp mới.
6. **Nhật ký** — mọi lần Lưu được ghi vào Nhật ký hệ thống, phân loại "Cấu hình", kèm giá trị cũ và mới
   (F1-14, `DEC-2026-1010-admin-operations-regroup`).

### 2.1 Cấu trúc hệ thống liên quan

| Màn/tài liệu | Vai trò | Bounded Context |
|---|---|---|
| Màn hiện tại | `ADM0501` / `admin_language_config` | `judge-orchestration` + `problem-bank` |
| Tài liệu yêu cầu | `01-rd/req/F2_problem-bank.md` (F2-10), `01-rd/req/F4_judge-orchestration.md` (F4-11) | Nguồn SoT của màn |
| Prototype | `09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html` | Bằng chứng bố cục và trạng thái |

---

## 3. Ngoài phạm vi (Out of Scope)

- Bảng màu, spacing, component cụ thể — thuộc BD (`02-bd/screens/admin/ADM0501_language_config.md`, V1.0).
- Hợp đồng API (đổi hệ số, giới hạn, sandbox) — thuộc DD (`03-dd/api/F4_judge-orchestration.md`, chưa viết).
- Khái niệm "kỳ thi" ở ghi chú "Tắt trong mọi kỳ thi" — cùng phát hiện Q2 của
  `01-rd/screens/admin/ADM0401_queue_monitor.md`, **đã chốt 2026-08-31** (nhãn ưu tiên, không phải tính năng
  thật), không lặp lại chi tiết ở đây.

---

## 4. Tiền đề và ràng buộc

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| — | Các câu hỏi mở cấp BD của màn này (cột `enabled`, nơi lưu giới hạn mặc định, sandbox toàn cục hay theo ngôn ngữ, hệ số RAM, xác nhận khi tắt, cảnh báo rời màn) **đã đóng ở BD `ADM0501` V1.0 ngày 2026-10-10**, xem `DEC-2026-1010-admin-operations-regroup`. | — | RD không lặp lại | Đã đóng |
| Q1 | ~~3 toggle Sandbox (mạng, giới hạn tiến trình con, kích thước stderr)...~~ **ĐÃ CHỐT 2026-08-31 (owner instruction, theo đúng đề xuất):** mở rộng F4-11, không cần mã mới. | — | Đã liệt kê rõ 3 tham số vào `01-rd/req/F4_judge-orchestration.md` (amendment F4-11). Xem `DEC-2026-0831-judge-orchestration-ops-details`. | Đã đóng |

---

## 5. Danh sách yêu cầu (REQ)
| ID | Yêu cầu | Loại | Nguồn/SoT |
|---|---|---|---|
| REQ-01 | Hệ số nhân thời gian theo ngôn ngữ (F2-10) | Chức năng | `01-rd/req/F2_problem-bank.md` — F2-10 |
| REQ-02 | Cấu hình ngôn ngữ và giới hạn tài nguyên mặc định (F4-11) | Chức năng | `01-rd/req/F4_judge-orchestration.md` — F4-11 |
| REQ-03 | Given-When-Then liên quan (—) | Chức năng | `01-rd/req/user_stories/a3_admin.md` (`US-A3-04`, GWT đầu) |
| REQ-04 | **Cho** quản trị viên đổi một ô hoặc một công tắc, **Khi** có thay đổi chưa lưu, **Thì** thanh lưu dính đáy hiện số thay đổi cùng nút "Hoàn tác" và "Lưu"; **Khi** bấm "Hoàn tác", **Thì** mọi thay đổi chưa lưu bị bỏ. | Chức năng | Chốt 2026-10-10 (`DEC-2026-1010-admin-operations-regroup`) |
| REQ-05 | **Cho** quản trị viên tắt một ngôn ngữ, **Khi** bấm "Lưu", **Thì** hiện **một** hộp xác nhận nêu hậu quả (người học không chọn được ngôn ngữ đó cho bài nộp mới, job đang chờ vẫn chạy nốt); **Khi** là ngôn ngữ cuối cùng còn bật, **Thì** không cho tắt. | Chức năng | Chốt 2026-10-10 |
| REQ-06 | **Cho** quản trị viên đổi hệ số, giới hạn mặc định hoặc sandbox, **Khi** lưu thành công, **Thì** cấu hình mới áp cho mọi job bắt đầu chấm từ lúc đó và một dòng được ghi vào Nhật ký hệ thống (phân loại "Cấu hình") kèm giá trị cũ và mới. | Chức năng | Chốt 2026-10-10; F1-14 |

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
| BD màn hình | `02-bd/screens/admin/ADM0501_language_config.md` (V1.0) |
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
| Yêu cầu module | `01-rd/req/F2_problem-bank.md` — F2-10; `01-rd/req/F4_judge-orchestration.md` — F4-11. |
| User story | `01-rd/req/user_stories/a3_admin.md` — `US-A3-04` (GWT đầu). |
| Khảo sát hệ thống | `01-rd/overview/system_survey.md` mục 7.3 — dòng `admin_language_config`. |
| Tiêu chuẩn kỹ thuật | `CLAUDE.md` mục "Locked stack" — khoá đúng 3 ngôn ngữ. |
| Prototype | `09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html` — prototype. |
| Quyết định | `DEC-2026-0828-remove-rejudge-scope`; `DEC-2026-0831-judge-orchestration-ops-details`. |
| Màn liên quan | `01-rd/screens/admin/ADM0401_queue_monitor.md` mục 4 Q2 — khái niệm "kỳ thi" liên quan. |
