# RD — Màn `admin_system_log` (Nhật ký hệ thống)

> Slug: `admin_system_log` — khớp `01-rd/overview/system_survey.md` mục 7.3 [SoT: 01-rd/overview/system_survey.md:557].
> Bounded Context: `identity` (F1) [SoT: 01-rd/overview/system_survey.md:557]. Actor: A3.
>
> Đối chiếu prototype: `09-layoutBase/Admin - Nhật ký hệ thống.dc.html`. File này mô tả **hành vi và UX ở
> mức yêu cầu** — không lặp lại đặc tả chức năng đã có ở `01-rd/req/req.md` (F1-14), chỉ trỏ tới và bổ sung
> phần đặc thù của màn. Khớp RD tốt — đúng phân định "chỉ ghi hành động quản trị của người, không gộp sự
> kiện hạ tầng" đã chốt trước ở `req.md:62-68`.

## 1. Mục đích màn hình

Xem lại mọi hành động quản trị (đổi ma trận quyền, đổi vai trò, khoá/mở khoá, reset mật khẩu, chấm lại...)
kèm ai đổi, đổi gì, đổi lúc nào — không có ngoại lệ [SoT: 01-rd/req/req.md:62-68 — F1-14].

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Ghi mọi thay đổi ma trận phân quyền và thao tác quản trị vào Nhật ký hệ thống | F1-14 | `01-rd/req/req.md:62-68` |
| Given-When-Then liên quan | — | `01-rd/req/user_stories.md:295-296, 304-305, 344-346` (rải trong `US-A3-01/02/05`) |

## 3. Trạng thái và cấu trúc màn (screen states)

Đối chiếu `09-layoutBase/Admin - Nhật ký hệ thống.dc.html`:

1. **Ghi chú tiêu đề** — "Hành động quản trị của con người · sự kiện hạ tầng xem ở Hàng đợi chấm" (dòng
   152) — khớp **chính xác từng chữ** với quyết định phạm vi đã chốt ở `req.md:64-68`. Đây là bằng chứng
   mạnh nhất trong toàn Phase 6 rằng quyết định phân tách hai luồng dữ liệu (hành động quản trị vs. sự kiện
   hạ tầng) đã được phản ánh đúng vào UI thật.
2. **Theo dõi trực tiếp** — toggle bật/tắt live update (dòng 154-156) — không có mã riêng, chi tiết UX hợp
   lý cho một trang log.
3. **4 chỉ số tổng** — Hành động quản trị 24 giờ, Đổi ma trận phân quyền, Khoá/mở khoá tài khoản, Phiên
   chấm lại đã chạy (dòng 395-400) — tổng hợp từ F1-14 + F1-10 + F1-13 + F4-09e, không cần mã riêng.
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
   `01-rd/screens/admin/admin_user_management.md`; không lặp lại chi tiết ở đây, chỉ xác nhận nó xuất hiện
   nhất quán ở cả hai màn (không phải lỗi dữ liệu mẫu một lần).

## 4. Given-When-Then bổ sung ở mức màn

- **Cho** một quản trị viên đổi một ô trong ma trận phân quyền, **Khi** thay đổi được lưu, **Thì** một dòng
  log mới xuất hiện ngay ở đầu danh sách (nếu đang "Theo dõi trực tiếp"), phân loại "Ma trận quyền", ghi rõ
  quyền nào đổi cho vai trò nào [SoT: 09-layoutBase/Admin - Nhật ký hệ thống.dc.html:406, 412 — dòng mẫu
  "Cấp quyền UPDATE trên TESTCASE_MANAGEMENT cho vai trò Trợ giảng"].
- **Cho** quản trị viên lọc theo phân loại "Cấu hình", **Khi** bộ lọc áp dụng, **Thì** danh sách chỉ hiện
  các hành động thuộc nhóm cấu hình (đổi giới hạn ngôn ngữ, cập nhật prompt AI...), không lẫn hành động xác
  thực hay ma trận quyền.

## 5. Câu hỏi mở

Không có câu hỏi mở mới ở màn này. Màn này chỉ **xác nhận thêm** hai phát hiện đã ghi ở nơi khác (không lặp
lại để tránh trùng câu hỏi trên nhiều file):

- Dòng log "trùng mã nguồn" → Câu hỏi mở Q1 của `01-rd/screens/admin/admin_user_management.md`.
- Dòng log "Cập nhật prompt Phỏng vấn giả lập lên bản v2.5" (dòng 409) → khớp bình thường F5-23, không phải
  câu hỏi mở.

## 6. Ngoài phạm vi file này

- Bảng màu, spacing, component cụ thể — thuộc BD (`02-bd/screens/admin/admin_system_log.md`, chưa viết).
- Hợp đồng API (ghi log, tìm kiếm/lọc, live update qua WebSocket hoặc polling) — thuộc DD
  (`03-dd/api/identity.md`, chưa viết).
- Thời hạn lưu log (footer ghi "Nhật ký giữ 90 ngày", dòng 256) — số ngày cụ thể `[SoT: Suy luận]` từ chính
  prototype, chốt chính xác khi viết BD.

## 7. Tham chiếu

- `01-rd/req/req.md:62-68` — F1-14.
- `01-rd/req/user_stories.md:295-296, 304-305, 344-346` — GWT rải trong `US-A3-01/02/05`.
- `01-rd/overview/system_survey.md` mục 7.3 — dòng `admin_system_log`.
- `09-layoutBase/Admin - Nhật ký hệ thống.dc.html` — prototype.
- `01-rd/screens/admin/admin_user_management.md` mục 5 Q1 — phát hiện gian lận mã nguồn, nguồn chính.
