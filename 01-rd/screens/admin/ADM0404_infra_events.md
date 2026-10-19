# RD (Yêu cầu hệ thống mới) — Sự kiện hạ tầng / `ADM0404`

> Mã màn hình: `ADM0404` [SoT: 02-bd/_rules/bd-template-9sheet.md — mục 8, dòng thêm 2026-10-10].
> Slug chính tắc: `admin_infra_events` (màn mới, thêm 2026-10-10; chưa có trong `01-rd/overview/system_survey.md` trước ngày này, đã bổ sung mục 7.3).
> Phạm vi/Bounded Context: `judge-orchestration` (F4). Actor: A3 (Quản trị viên). Route: `/admin/infra-events`, nằm trong nhóm điều hướng "Vận hành", giữa "Hàng đợi chấm" và "Ngôn ngữ và giới hạn".
> Nguồn sự thật (SoT): `01-rd/req/judge-orchestration.md` (F4-10), `DEC-2026-1010-admin-operations-regroup`. Không có mockup `.dc.html` riêng: khối này trước đây là phần cuối của `09-layoutBase/Admin - Hàng đợi chấm.dc.html` (danh sách "Sự kiện hạ tầng gần đây").
> Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

Cho quản trị viên xem **log của máy móc** (bản ghi Outbox kẹt, lượt nộp lỗi hệ thống hoặc lỗi sandbox của
go-judge) để chẩn đoán sự cố, có lọc, tìm kiếm và xem lùi nhiều
ngày. Trước đây đây là khối cuối của màn Hàng đợi chấm; tách ra vì nó chỉ đọc khi có sự cố và cần tìm kiếm
riêng, còn màn Hàng đợi chấm phải ngắn để đọc nhanh.

Ranh giới đã chốt, không đổi: **sự kiện hạ tầng không phải hành động quản trị của người**. Hành động của người
(đổi quyền, khoá tài khoản, đổi cấu hình...) thuộc `ADM0403` Nhật ký hệ thống
(đúng phân định "chỉ ghi hành động quản trị của người, không gộp sự kiện hạ tầng" đã chốt ở F1-14,
`01-rd/req/identity.md`, và nhắc lại ở `01-rd/screens/admin/ADM0403_system_log.md` mục 1).

---

## 2. Phạm vi

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Xem danh sách sự kiện hạ tầng, có lọc theo mức độ, dịch vụ, khoảng ngày và tìm kiếm | F4-10 | `01-rd/req/judge-orchestration.md` — F4-10 (giám sát hàng đợi và tình trạng cụm), `DEC-2026-1010-admin-operations-regroup` |

Nội dung màn:

1. **Thanh lọc**: ô tìm kiếm nội dung, lọc mức độ (Tất cả / ERROR / WARN / INFO), lọc dịch vụ (Tất cả / `go-judge` /
   `outbox`), hai ô ngày "Từ ngày" "Đến ngày" (khoảng đóng hai đầu, khoảng ngược thì báo lỗi),
   kết hợp theo AND — cùng cách làm màn `ADM0403`.
2. **Danh sách sự kiện**, mới nhất trước: giờ (kèm ngày), mức độ, nội dung, dịch vụ nguồn. Hiện 20 dòng đầu,
   nút **"Tải thêm 20 dòng"** nạp tiếp, tắt khi đã hết.
3. **Chỉ đọc**: không có thao tác ghi, không sửa, không xoá.
4. Nguồn sự kiện (chốt 2026-10-10, tiêu chí ponytail bậc 2): hai nguồn thật: bản ghi Outbox kẹt (`outbox_events.publish_attempts`, ngưỡng hằng số trong `application.yml`: từ 3 lần thử là `WARN`, từ 5 lần là `ERROR`) và lượt nộp `SYSTEM_ERROR` hoặc lỗi sandbox (từ `judge.submissions` và timeout sweep). Không dùng AI Gateway, sao lưu, db, MinIO, Redis làm nguồn.
5. Không tự làm tươi; có nút "Làm mới". Mở từ thẻ liên kết ở tông lỗi thì đặt sẵn bộ lọc mức độ bằng `?level=error`.

Màn Hàng đợi chấm (`ADM0401`) giữ **một thẻ liên kết** hiển thị số cảnh báo và số lỗi gần đây, bấm sang màn này.

### 2.1 Cấu trúc hệ thống liên quan

| Màn/tài liệu | Vai trò | Bounded Context |
|---|---|---|
| Màn hiện tại | `ADM0404` / `admin_infra_events` | `judge-orchestration` |
| Màn liên quan | `ADM0401` / `admin_queue_monitor` — thẻ liên kết tới màn này | `judge-orchestration` |
| Màn đối chiếu | `ADM0403` / `admin_system_log` — hành động của người, không lẫn | `identity` |
| Tài liệu yêu cầu | `01-rd/req/judge-orchestration.md` (F4-10) | Nguồn SoT của màn |

---

## 3. Ngoài phạm vi (Out of Scope)

- Bảng màu, spacing, component cụ thể — thuộc BD (`02-bd/screens/admin/ADM0404_infra_events.md`).
- Hợp đồng API, nguồn dữ liệu chính xác và thời hạn lưu sự kiện — thuộc DD (`03-dd/api/judge-orchestration.md`, chưa viết).
- Xử lý sự cố từ màn này (khởi động lại worker, thử lại job...): không có, màn chỉ đọc.
- Cảnh báo chủ động (gửi thông báo khi có ERROR): ngoài phạm vi đồ án, theo dõi bằng Grafana.

---

## 4. Tiền đề và ràng buộc

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | ~~Thời hạn lưu sự kiện hạ tầng là bao lâu, có cùng 90 ngày của Nhật ký hệ thống không?~~ **ĐÃ CHỐT 2026-10-10 (chủ dự án đã xác nhận 2026-10-10, tiêu chí ponytail bậc 4 và 5):** **90 ngày**, dùng chung với Nhật ký hệ thống, không có ô cấu hình | - | Đã chốt, trùng BD Q3 | Đã đóng |

---

## 5. Danh sách yêu cầu (REQ)
| ID | Yêu cầu | Loại | Nguồn/SoT |
|---|---|---|---|
| REQ-01 | **Cho** quản trị viên mở màn, **Khi** tải xong, **Thì** hiện 20 sự kiện mới nhất, mỗi dòng có giờ và ngày, mức độ, nội dung, dịch vụ nguồn. | Chức năng | `DEC-2026-1010-admin-operations-regroup` |
| REQ-02 | **Cho** quản trị viên lọc theo mức độ hoặc dịch vụ hoặc tìm một từ khoá, **Khi** áp dụng, **Thì** danh sách chỉ còn dòng khớp mọi điều kiện đang chọn (AND). | Chức năng | Như trên |
| REQ-03 | **Cho** quản trị viên đặt "Từ ngày" và "Đến ngày", **Khi** khoảng hợp lệ, **Thì** chỉ còn dòng trong khoảng (gồm hai ngày biên); **Khi** từ ngày muộn hơn đến ngày, **Thì** hiện cảnh báo và không có dòng nào. | Chức năng | Cùng quy tắc REQ-05 của `01-rd/screens/admin/ADM0403_system_log.md` |
| REQ-04 | **Cho** còn sự kiện chưa hiển thị, **Khi** quản trị viên bấm "Tải thêm 20 dòng", **Thì** nạp thêm tối đa 20 dòng; hết dữ liệu thì nút không kích hoạt. | Chức năng | Như trên |

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
| BD màn hình | `02-bd/screens/admin/ADM0404_infra_events.md` (V0.1) |
| DD/API liên quan | Chưa viết — không tự suy luận |

---

## 8. Kế hoạch và quy mô

| Chỉ số | Giá trị |
|---|---|
| Quy mô hành vi | 4 yêu cầu ở chương 5. |
| Lịch, nhân sự, công sức | Chưa xác định — không tự suy luận. |

---

## 9. Tham chiếu

| Loại | Tham chiếu |
|---|---|
| Yêu cầu module | `01-rd/req/judge-orchestration.md` — F4-10. |
| Màn liên quan | `01-rd/screens/admin/ADM0401_queue_monitor.md`; `01-rd/screens/admin/ADM0403_system_log.md` (cùng mẫu thanh lọc). |
| Quyết định | `DEC-2026-1010-admin-operations-regroup`. |
| Báo cáo | `06-plan/reports/261010-1030-admin-van-hanh-userflow-va-mau-thuan/report.md`. |
