# RD (Yêu cầu hệ thống mới) — Nhật ký hệ thống / `ADM0403`

> Mã màn hình: `ADM0403` [SoT: 02-bd/_rules/bd-template-9sheet.md — mục 8].
> Slug chính tắc: `admin_system_log` [SoT: 01-rd/overview/system_survey.md — mục 7.3 dòng `admin_system_log`].
> Phạm vi/Bounded Context: `identity` (F1) [SoT: 01-rd/overview/system_survey.md — mục 7.3 dòng `admin_system_log`]. Actor: A3 (Quản trị viên).
> Nguồn sự thật (SoT): `01-rd/req/identity.md` (F1-14), `09-layoutBase/Admin - Nhật ký hệ thống.dc.html` (prototype), `01-rd/req/user_stories/a3_admin.md`.
> Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

Xem lại mọi hành động quản trị (đổi ma trận quyền, đổi vai trò, khoá/mở khoá, reset mật khẩu, thao tác nội
dung kho bài và kho câu hỏi...) kèm ai đổi, đổi gì, đổi lúc nào — không có ngoại lệ
[SoT: 01-rd/req/identity.md — F1-14]. **Sửa 2026-09-03:** bỏ "chấm lại" khỏi danh sách ví dụ — hành động
đó không còn tồn tại (`DEC-2026-0828-remove-rejudge-scope`), như mục 3 chỉ số 4 của file này đã ghi nhận.

Đối chiếu prototype: `09-layoutBase/Admin - Nhật ký hệ thống.dc.html`. File này mô tả **hành vi và UX ở
mức yêu cầu** — không lặp lại đặc tả chức năng đã có ở `01-rd/req/identity.md` (F1-14), chỉ trỏ tới và bổ sung
phần đặc thù của màn. Khớp RD tốt — đúng phân định "chỉ ghi hành động quản trị của người, không gộp sự
kiện hạ tầng" đã chốt trước ở `identity.md` — F1-14.

---

## 2. Phạm vi

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Ghi mọi thay đổi ma trận phân quyền và thao tác quản trị vào Nhật ký hệ thống | F1-14 | `01-rd/req/identity.md` — F1-14 |
| Given-When-Then liên quan | — | `01-rd/req/user_stories/a3_admin.md` (rải trong `US-A3-01/02/05`) |

Đối chiếu `09-layoutBase/Admin - Nhật ký hệ thống.dc.html`:

1. **Ghi chú tiêu đề** — "Hành động quản trị của con người · sự kiện hạ tầng xem ở Hàng đợi chấm" (dòng
   152) — khớp **chính xác từng chữ** với quyết định phạm vi đã chốt ở `identity.md` — F1-12, F1-14. Đây là bằng chứng
   mạnh nhất trong toàn Phase 6 rằng quyết định phân tách hai luồng dữ liệu (hành động quản trị vs. sự kiện
   hạ tầng) đã được phản ánh đúng vào UI thật.
2. **Theo dõi trực tiếp** — toggle bật/tắt live update (dòng 154-156) — không có mã riêng, chi tiết UX hợp
   lý cho một trang log.
3. **4 chỉ số tổng** — Hành động quản trị 24 giờ, Đổi ma trận phân quyền, Khoá/mở khoá tài khoản, Phiên
   chấm lại đã chạy (dòng 395-400) — tổng hợp từ F1-14 + F1-10 + F1-13, không cần mã riêng. **Cập nhật
   2026-08-28:** chỉ số "Phiên chấm lại đã chạy" hết ý nghĩa (F4-09e đã loại khỏi phạm vi,
   `DEC-2026-0828-remove-rejudge-scope`) — bỏ chỉ số này khi build FE thật, còn 3 chỉ số. **Cập nhật
   2026-10-01 (owner instruction):** cả dải chỉ số này **không được dựng** trên UI Next.js — chỉ một trang
   tổng quan mỗi khu hiển thị KPI, trang danh sách chỉ có tiêu đề, bộ lọc và danh sách. Hai khối bên phải
   (Quản trị viên hoạt động, Phân loại 7 ngày) vẫn giữ.
4. **Danh sách sự kiện** — tìm kiếm, lọc theo phân loại (Xác thực/Ma trận quyền/Cấu hình/Nội dung), mỗi
   dòng: giờ, phân loại, nội dung, dịch vụ, người thực hiện, mã sự kiện (dòng 402-412) — khớp F1-14 ("ai
   đổi, đổi gì, đổi lúc nào"). Phân loại "Nội dung" (ví dụ "Thêm testcase biên", "Xuất bản bài toán") mở
   rộng hợp lý phạm vi F1-14 sang hành động quản trị nội dung của giảng viên, không chỉ hành động của ADMIN
   — khớp câu chữ gốc "mọi thao tác quản trị" (không giới hạn actor).
5. **Quản trị viên hoạt động** — số hành động gần nhất theo từng người (dòng 431-436) — không có mã riêng,
   tổng hợp hiển thị.
6. **Phân loại hành động 7 ngày** — biểu đồ cột ngang theo 4 phân loại (dòng 438-443) — không có mã riêng.
7. **Dòng log mẫu đáng chú ý**: "Khóa tài khoản hmtri do trùng mã nguồn với bklinh" (dòng 404) — chính là
   bằng chứng cho phát hiện gian lận mã nguồn đã ghi ở Câu hỏi mở Q1 của
   `01-rd/screens/admin/ADM0201_user_management.md`; không lặp lại chi tiết ở đây, chỉ xác nhận nó xuất hiện
   nhất quán ở cả hai màn (không phải lỗi dữ liệu mẫu một lần).

### 2.1 Cấu trúc hệ thống liên quan

| Màn/tài liệu | Vai trò | Bounded Context |
|---|---|---|
| Màn hiện tại | `ADM0403` / `admin_system_log` | `identity` |
| Tài liệu yêu cầu | `01-rd/req/identity.md` (F1-14) | Nguồn SoT của màn |
| Prototype | `09-layoutBase/Admin - Nhật ký hệ thống.dc.html` | Bằng chứng bố cục và trạng thái |

---

## 3. Ngoài phạm vi (Out of Scope)

- Bảng màu, spacing, component cụ thể — thuộc BD (`02-bd/screens/admin/ADM0403_system_log.md`, chưa viết).
- Hợp đồng API (ghi log, tìm kiếm/lọc, live update qua WebSocket hoặc polling) — thuộc DD
  (`03-dd/api/identity.md`, chưa viết).
- Thời hạn lưu log (footer ghi "Nhật ký giữ 90 ngày", dòng 256) — số ngày cụ thể `[SoT: Suy luận]` từ chính
  prototype, chốt chính xác khi viết BD.

---

## 4. Tiền đề và ràng buộc

Không có câu hỏi mở mới ở màn này. Màn này chỉ **xác nhận thêm** hai phát hiện đã ghi ở nơi khác (không lặp
lại để tránh trùng câu hỏi trên nhiều file):

- Dòng log "trùng mã nguồn" → Câu hỏi mở Q1 của `01-rd/screens/admin/ADM0201_user_management.md`.
- Dòng log "Cập nhật prompt Phỏng vấn giả lập lên bản v2.5" (dòng 409) → khớp bình thường F5-23, không phải
  câu hỏi mở.

---

## 5. Danh sách yêu cầu (REQ)
| ID | Yêu cầu | Loại | Nguồn/SoT |
|---|---|---|---|
| REQ-01 | Ghi mọi thay đổi ma trận phân quyền và thao tác quản trị vào Nhật ký hệ thống (F1-14) | Chức năng | `01-rd/req/identity.md` — F1-14 |
| REQ-02 | Given-When-Then liên quan (—) | Chức năng | `01-rd/req/user_stories/a3_admin.md` (rải trong `US-A3-01/02/05`) |
| REQ-03 | **Cho** một quản trị viên đổi một ô trong ma trận phân quyền, **Khi** thay đổi được lưu, **Thì** một dòng log mới xuất hiện ngay ở đầu danh sách (nếu đang "Theo dõi trực tiếp"), phân loại "Ma trận quyền", ghi rõ quyền nào đổi cho vai trò nào [SoT: `09-layoutBase/Admin - Nhật ký hệ thống.dc.html:406, 412` — dòng mẫu "Cấp quyền UPDATE trên TESTCASE_MANAGEMENT cho vai trò Trợ giảng"]. | Chức năng | GWT bổ sung trong RD |
| REQ-04 | **Cho** quản trị viên lọc theo phân loại "Cấu hình", **Khi** bộ lọc áp dụng, **Thì** danh sách chỉ hiện các hành động thuộc nhóm cấu hình (đổi giới hạn ngôn ngữ, cập nhật prompt AI...), không lẫn hành động xác thực hay ma trận quyền. | Chức năng | GWT bổ sung trong RD |

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
| Yêu cầu module | `01-rd/req/identity.md` — F1-14. |
| User story | `01-rd/req/user_stories/a3_admin.md` — GWT rải trong `US-A3-01/02/05`. |
| Khảo sát hệ thống | `01-rd/overview/system_survey.md` mục 7.3 — dòng `admin_system_log`. |
| Prototype | `09-layoutBase/Admin - Nhật ký hệ thống.dc.html` — prototype. |
| Màn liên quan | `01-rd/screens/admin/ADM0201_user_management.md` mục 4 Q1 — phát hiện gian lận mã nguồn, nguồn chính. |
