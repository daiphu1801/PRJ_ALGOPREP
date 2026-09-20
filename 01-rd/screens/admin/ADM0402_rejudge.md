# RD (Yêu cầu hệ thống mới) — Chấm lại / `ADM0402` — ĐÃ LOẠI BỎ KHỎI PHẠM VI

> Mã màn hình: `ADM0402` [SoT: 02-bd/_rules/bd-template-9sheet.md — mục 8]. Mã được giữ để truy vết; màn ngoài phạm vi BD/DD.
> Slug chính tắc: `admin_rejudge` [SoT: 01-rd/overview/system_survey.md — mục 7.3 dòng `admin_rejudge`].
> Trạng thái phạm vi: ĐÃ LOẠI BỎ KHỎI PHẠM VI (`DEC-2026-0828-remove-rejudge-scope`). Giữ lại để lưu vết.
> Bounded Context: `judge-orchestration` (F4) [SoT: 01-rd/overview/system_survey.md — mục 7.3 dòng `admin_rejudge`]. Actor: A3 (Quản trị viên).
> Nguồn sự thật (SoT): `01-rd/req/judge-orchestration.md` (F4-09a→e, đã loại bỏ), `09-layoutBase/Admin - Chấm lại.dc.html` (prototype), `01-rd/req/user_stories/a3_admin.md` (`US-A3-05`).
> Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

Vận hành một phiên chấm lại (rejudge) quy mô lớn — chọn phạm vi, xem ước lượng ảnh hưởng, chạy thử trên mẫu
nhỏ, rồi chạy thật có kiểm soát (tạm dừng/huỷ giữa chừng), để áp dụng thay đổi cho hàng nghìn lượt nộp cũ một
cách an toàn [SoT: 01-rd/req/judge-orchestration.md — F4-09a→e, mã đã loại khỏi phạm vi].

**ĐÃ LOẠI BỎ KHỎI PHẠM VI (2026-08-28, qua hỏi trực tiếp chủ dự án) — `DEC-2026-0828-remove-rejudge-scope`.**
Slug `admin_rejudge`, mã `F4-09a` tới `F4-09e`, và Function `REJUDGE_MANAGEMENT` không còn hiệu lực. Lý do:
giảm chức năng không cần thiết — nếu một testcase bị lỗi, học viên báo cho giảng viên, giảng viên tự sửa
testcase; các lượt nộp cũ đã chấm theo testcase lỗi giữ nguyên kết quả cũ, không có cơ chế tự động chấm
lại hàng loạt. Nội dung dưới đây **giữ lại chỉ để lưu vết** (đã có đặc tả chi tiết trước khi bị loại bỏ) —
không dùng làm căn cứ viết BD/DD, không dựng route `/admin/rejudge` khi làm khung base FE.

Đối chiếu prototype: `09-layoutBase/Admin - Chấm lại.dc.html`. File này mô tả **hành vi và UX ở mức yêu
cầu** — không lặp lại đặc tả chức năng đã có ở `01-rd/req/judge-orchestration.md` (mục F4-09a→e, **mã đã
loại khỏi phạm vi, xem `DEC-2026-0828-remove-rejudge-scope`**), chỉ trỏ tới và bổ sung phần đặc thù của
màn. Khớp RD tốt nhất trong 8 màn Admin — F4-09a→e đã đặc tả rất chi tiết trước khi prototype được đối
chiếu.

---

## 2. Phạm vi

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Chọn phạm vi chấm lại (theo bài toán / theo lượt nộp / theo khoảng thời gian) | F4-09a | `01-rd/req/judge-orchestration.md` — F4-09a (đã loại khỏi phạm vi) |
| Ước lượng ảnh hưởng trước khi chạy thật (dry-run) | F4-09b | `01-rd/req/judge-orchestration.md` — F4-09b (đã loại khỏi phạm vi) |
| Tuỳ chọn khi chạy (giữ điểm cao hơn, không hạ điểm) | F4-09c | `01-rd/req/judge-orchestration.md` — F4-09c (đã loại khỏi phạm vi) |
| Điều khiển phiên đang chạy (tạm dừng/tiếp tục/huỷ), bất biến dữ liệu | F4-09d | `01-rd/req/judge-orchestration.md` — F4-09d (đã loại khỏi phạm vi) |
| Audit trail mỗi phiên chấm lại | F4-09e | `01-rd/req/judge-orchestration.md` — F4-09e (đã loại khỏi phạm vi) |
| Given-When-Then liên quan | — | `01-rd/req/user_stories/a3_admin.md` (`US-A3-05`, đã loại khỏi phạm vi) |

Đối chiếu `09-layoutBase/Admin - Chấm lại.dc.html`:

1. **Chọn phạm vi** — 3 tab: Theo bài toán / Theo lượt nộp / Theo khoảng thời gian, mỗi tab đổi hẳn bộ field
   nhập tương ứng (dòng 429-449) — khớp đúng F4-09a (ba kiểu phạm vi, mỗi kiểu tham số riêng).
2. **Tuỳ chọn khi chạy** — 3 toggle: "Giữ kết quả cũ nếu điểm mới thấp hơn" (khớp F4-09c — không hạ điểm đã
   công bố), "Thông báo cho người học" (chưa có mã — xem Câu hỏi mở Q1), "Chạy ở hàng đợi nền, không chèn
   trước job của người học" (khớp nguyên tắc ưu tiên trải nghiệm thời gian thực đã ghi ở `judge-orchestration.md` — F4-09c, đã loại khỏi phạm vi).
3. **Ước lượng** — số lượt bị ảnh hưởng, thời gian ước tính, số lần chạy testcase, cùng nút "Chạy thử 20
   lượt" và "Bắt đầu chấm lại" (dòng 199-207) — khớp F4-09b.
4. **Đang chạy** — thanh tiến độ %, nút Tạm dừng/Huỷ cho từng phiên (dòng 216-231) — khớp F4-09d.
5. **Quy tắc áp dụng** — 4 dòng quy tắc tĩnh (chỉ chấm lại lượt đã có kết quả cuối; kết quả cũ khôi phục
   được trong 30 ngày; bảng xếp hạng cập nhật sau khi phiên kết thúc; mỗi phiên ghi Nhật ký hệ thống) — khớp
   đúng F4-09c/e và con số 30 ngày khớp `judge-orchestration.md` — F4-09d, F4-09e (đề xuất `[SoT: Suy luận]`, đã có mặt ở prototype
   với cùng con số — không phải bằng chứng độc lập, chỉ là cùng một nguồn suy luận ban đầu).
6. **Lịch sử chấm lại** — bảng: phiên/phạm vi/lý do/số lượt/đổi kết quả/trạng thái/người chạy, kèm "Xuất CSV"
   (dòng 249-280) — khớp F4-09e (audit trail).

### 2.1 Cấu trúc hệ thống liên quan

| Màn/tài liệu | Vai trò | Bounded Context |
|---|---|---|
| Màn hiện tại | `ADM0402` / `admin_rejudge` (đã loại khỏi phạm vi) | `judge-orchestration` |
| Tài liệu yêu cầu | `01-rd/req/judge-orchestration.md` (F4-09a→e, đã loại bỏ) | Nguồn SoT của màn |
| Prototype | `09-layoutBase/Admin - Chấm lại.dc.html` | Bằng chứng bố cục và trạng thái |

---

## 3. Ngoài phạm vi (Out of Scope)

- Bảng màu, spacing, component cụ thể — thuộc BD (không viết BD — màn đã cắt khỏi phạm vi theo `DEC-2026-0828-remove-rejudge-scope`).
- Hợp đồng API (chọn phạm vi, dry-run, điều khiển phiên, audit trail) — thuộc DD
  (`03-dd/api/judge-orchestration.md`, chưa viết).
- Khái niệm "kỳ thi" xuất hiện ở nhãn lý do "Kỳ thi giữa kỳ CS201" (dòng 495) — cùng phát hiện Q2 của
  `01-rd/screens/admin/ADM0401_queue_monitor.md`, không lặp lại chi tiết ở đây.

---

## 4. Tiền đề và ràng buộc

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| ~~Q1~~ | ~~Toggle "Thông báo cho người học" (gửi thông báo khi kết quả thay đổi do chấm lại) không có mã `Fx-nn`.~~ **MOOT (2026-08-28, ghi nhận 2026-09-03):** toàn bộ tính năng chấm lại đã loại khỏi phạm vi (`DEC-2026-0828-remove-rejudge-scope`), nên không có sự kiện "kết quả đổi do chấm lại" để thông báo. Câu hỏi và đề xuất cũ giữ lại chỉ để lưu vết, cùng cách `01-rd/req/user_stories/open_questions.md` xử lý Q2. | (hết hiệu lực) | ~~Coi là mở rộng nhỏ của F4-09c, không cần mã mới~~ — F4-09c không còn tồn tại. | Đã đóng (moot) |

---

## 5. Danh sách yêu cầu (REQ)
| ID | Yêu cầu | Loại | Nguồn/SoT |
|---|---|---|---|
| REQ-01 | Chọn phạm vi chấm lại (theo bài toán / theo lượt nộp / theo khoảng thời gian) (F4-09a) | Chức năng | `01-rd/req/judge-orchestration.md` — F4-09a (đã loại khỏi phạm vi) |
| REQ-02 | Ước lượng ảnh hưởng trước khi chạy thật (dry-run) (F4-09b) | Chức năng | `01-rd/req/judge-orchestration.md` — F4-09b (đã loại khỏi phạm vi) |
| REQ-03 | Tuỳ chọn khi chạy (giữ điểm cao hơn, không hạ điểm) (F4-09c) | Chức năng | `01-rd/req/judge-orchestration.md` — F4-09c (đã loại khỏi phạm vi) |
| REQ-04 | Điều khiển phiên đang chạy (tạm dừng/tiếp tục/huỷ), bất biến dữ liệu (F4-09d) | Chức năng | `01-rd/req/judge-orchestration.md` — F4-09d (đã loại khỏi phạm vi) |
| REQ-05 | Audit trail mỗi phiên chấm lại (F4-09e) | Chức năng | `01-rd/req/judge-orchestration.md` — F4-09e (đã loại khỏi phạm vi) |
| REQ-06 | Given-When-Then liên quan (—) | Chức năng | `01-rd/req/user_stories/a3_admin.md` (`US-A3-05`, đã loại khỏi phạm vi) |

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
| Yêu cầu module | `01-rd/req/judge-orchestration.md` — F4-09a→e (đã loại khỏi phạm vi). |
| User story | `01-rd/req/user_stories/a3_admin.md` — `US-A3-05` (đã loại khỏi phạm vi). |
| Khảo sát hệ thống | `01-rd/overview/system_survey.md` mục 7.3 — dòng `admin_rejudge`. |
| Prototype | `09-layoutBase/Admin - Chấm lại.dc.html` — prototype. |
| Quyết định | `DEC-2026-0828-remove-rejudge-scope` (loại bỏ rejudge khỏi phạm vi đồ án). |
| Màn liên quan | `01-rd/screens/admin/ADM0401_queue_monitor.md` mục 5 Q2 — khái niệm "kỳ thi" liên quan. |
