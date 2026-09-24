# RD (Yêu cầu hệ thống mới) — Quản lý người dùng / `ADM0201`

> Mã màn hình: `ADM0201` [Nguồn: 02-bd/_rules/bd-template-9sheet.md — mục 8].
> Slug chính tắc: `admin_user_management` [SoT: 01-rd/overview/system_survey.md — mục 7.3 dòng `admin_user_management`].
> Phạm vi/Bounded Context: `identity` (F1) [SoT: 01-rd/overview/system_survey.md — mục 7.3 dòng `admin_user_management`]. Actor: A3 (Quản trị viên).
> Nguồn sự thật (SoT): `01-rd/req/identity.md` (F1-13, F1-14), `09-layoutBase/Admin - Người dùng.dc.html` (prototype),
> `01-rd/req/user_stories/a3_admin.md` (`US-A3-02`).
> Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

Quản lý tài khoản người dùng — đổi vai trò, khoá/mở khoá, reset mật khẩu — để xử lý sự cố tài khoản
[SoT: 01-rd/req/identity.md — F1-13].

Đối chiếu prototype: `09-layoutBase/Admin - Người dùng.dc.html`. File này mô tả **hành vi và UX ở mức yêu
cầu** — không lặp lại đặc tả chức năng đã có ở `01-rd/req/identity.md` (F1-13), chỉ trỏ tới và bổ sung phần đặc
thù của màn.

**Phát hiện của Phase 6 — Câu hỏi mở Q1, ĐÃ CHỐT 2026-08-31**: khối "Cần xử lý" có mục "Báo cáo nghi gian
lận" (phát hiện trùng mã nguồn giữa hai tài khoản) hoàn toàn chưa có mã `Fx-nn`. **Chốt: ngoài phạm vi**
(`DEC-2026-0831-remove-plagiarism-report`) — xem mục 4 Q1.

**Q2 đã chốt 2026-08-25 qua yêu cầu trực tiếp của chủ dự án:** không có luồng tự yêu cầu nâng vai trò —
**chỉ ADMIN được đổi vai trò**, đúng nguyên văn F1-13. Mục "Đề nghị cấp quyền giảng viên" trong prototype
bị loại khỏi phạm vi, xoá khi dựng UI thật **[Đợi nextjs]**. **Sửa 2026-08-31:** file trước đó có một dòng
"Q2" thứ hai trùng lặp, hỏi lại y hệt câu đã chốt — đây là lỗi dữ liệu (report `rd_review_report_260826.html`
mục 6.2 đã ghi nhận mâu thuẫn này), đã xoá dòng thừa, chỉ giữ dòng "Q2 (đã chốt)" ở trên làm câu trả lời
duy nhất.

---

## 2. Phạm vi

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Đổi vai trò, khoá/mở khoá, reset mật khẩu tài khoản khác | F1-13 | `01-rd/req/identity.md` — F1-13 |
| Mọi thay đổi ghi vào Nhật ký hệ thống | F1-14 | `01-rd/req/identity.md` — F1-14 |
| Given-When-Then đầy đủ | — | `01-rd/req/user_stories/a3_admin.md` (`US-A3-02`) |

Đối chiếu `09-layoutBase/Admin - Người dùng.dc.html`:

1. **4 chỉ số tổng** — Tổng tài khoản, Đang hoạt động 24 giờ, Chờ xác thực email, Bị khoá (dòng 422-427) —
   không có mã riêng, tổng hợp hiển thị của F1-13 và trạng thái tài khoản (F1-01/F1-16).
2. **Bảng người dùng** — tìm kiếm, lọc theo vai trò/trạng thái, chọn nhiều dòng rồi hành động gộp (Đặt lại
   mật khẩu / Đổi vai trò / Khoá tài khoản, dòng 194-203) — khớp F1-13, mở rộng UX cho phép áp dụng hành
   động lên nhiều tài khoản cùng lúc (không có mã riêng, là cách trình bày của F1-13).
3. **Phân bố theo vai trò** — biểu đồ tỉ lệ Người học/Giảng viên/Quản trị/Đã vô hiệu (dòng 475-480) — không
   có mã riêng, minh hoạ số liệu.
4. **Khối "Cần xử lý"** — 4 mục: "Yêu cầu đặt lại mật khẩu" (khớp F1-17 tự đặt lại mật khẩu, hoặc F1-13 nếu
   là ADMIN reset hộ — cần phân biệt rõ ở Câu hỏi mở Q3), "Chờ xác thực email" (khớp F1-01, đăng ký chờ xác
   thực), **"Báo cáo nghi gian lận"** (trùng mã nguồn giữa hai tài khoản, dòng 485) — **không có mã `Fx-nn`
   nào cho phát hiện gian lận/đạo văn mã nguồn** — **đã chốt ngoài phạm vi 2026-08-31, xem Câu hỏi mở Q1**
   (`DEC-2026-0831-remove-plagiarism-report`), và **"Đề nghị cấp quyền giảng viên"**
   (cần quản trị viên phê duyệt, dòng 486) — **đã chốt loại khỏi phạm vi (xem banner đầu file)**: chỉ ADMIN
   được đổi vai trò theo F1-13, không có luồng tự yêu cầu; mục này xoá khi dựng UI thật **[Đợi nextjs]**.

### 2.1 Cấu trúc hệ thống liên quan

| Màn/tài liệu | Vai trò | Bounded Context |
|---|---|---|
| Màn hiện tại | `ADM0201` / `admin_user_management` | `identity` |
| Tài liệu yêu cầu | `01-rd/req/identity.md` (F1-13, F1-14) | Nguồn SoT của màn |
| Prototype | `09-layoutBase/Admin - Người dùng.dc.html` | Bằng chứng bố cục và trạng thái |

**Chi tiết trạng thái và cấu trúc màn:**

| Thành phần | Vai trò | Nguồn |
| :--- | :--- | :--- |
| `01-rd/screens/admin/ADM0101_overview.md` | Màn cha — đích nav "Người dùng" trỏ tới màn này | [SoT: 01-rd/screens/admin/ADM0101_overview.md:158] |
| `01-rd/screens/admin/ADM0403_system_log.md` | Màn liên quan — mục 3 ghi dòng log "trùng mã nguồn" củng cố Câu hỏi mở Q1 | [SoT: 01-rd/screens/admin/ADM0201_user_management.md — mục 9 "Tham chiếu" của chính file này] |
| `09-layoutBase/Admin - Người dùng.dc.html` | Prototype đối chiếu bố cục và hành vi | [SoT: 09-layoutBase/Admin - Người dùng.dc.html] |
| Bounded Context `identity` | BC sở hữu quản lý tài khoản người dùng | [SoT: 01-rd/overview/system_survey.md — mục 7.3 dòng `admin_user_management`] |

---

## 3. Ngoài phạm vi (Out of Scope)

- Bảng màu, spacing, component cụ thể — thuộc BD (`02-bd/screens/admin/ADM0201_user_management.md`, chưa
  viết). **Cập nhật 2026-08-31: Q1, Q2, Q3 đều đã đóng, không còn gì chặn BD.**
- Hợp đồng API (tìm kiếm/lọc người dùng, hành động gộp, reset mật khẩu) — thuộc DD (`03-dd/api/identity.md`,
  chưa viết).
- ~~Cơ chế so khớp mã nguồn chống gian lận (Q1) — chờ chủ dự án quyết định phạm vi.~~ **Đã chốt
  2026-08-31** (ghi nhận 2026-09-05): **ngoài phạm vi đồ án**, không phát triển
  (`DEC-2026-0831-remove-plagiarism-report`).

---

## 4. Tiền đề và ràng buộc

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q2 (đã chốt) | "Đề nghị cấp quyền giảng viên" — luồng tự yêu cầu nâng vai trò từ STUDENT lên INSTRUCTOR. | Chủ dự án đã trả lời trực tiếp: **không có luồng self-service**. | **Chốt: chỉ ADMIN được đổi vai trò**, đúng nguyên văn F1-13 (`01-rd/req/identity.md`), không sửa mã này. Không mở RD/BD cho một luồng yêu cầu-duyệt. Xoá mục "Đề nghị cấp quyền giảng viên" khỏi khối "Cần xử lý" khi dựng UI thật **[Đợi nextjs]**. | Đã chốt — không còn mở. |
| Q1 | ~~**"Báo cáo nghi gian lận"...**~~ **ĐÃ CHỐT 2026-08-31 (owner instruction, theo đúng đề xuất):** **ngoài phạm vi đồ án** — không cấp mã mới, không mở RD cho thuật toán so khớp mã nguồn. | — | Xoá mục "Báo cáo nghi gian lận" khỏi khối "Cần xử lý" và dòng log liên quan ở `admin_system_log` khi dựng UI thật. Xem `DEC-2026-0831-remove-plagiarism-report`. | Đã đóng |
| Q3 | ~~"Yêu cầu đặt lại mật khẩu" trong khối "Cần xử lý"...~~ **ĐÃ CHỐT 2026-08-31 (owner instruction, theo đúng đề xuất):** tách ra khỏi khối "Cần xử lý". | — | Chuyển vào một khối thống kê riêng khi dựng UI thật (F1-17 không cần ADMIN hành động), tránh nhầm với các mục thật sự cần xử lý (xác thực email, cấp quyền). | Đã đóng |

---

## 5. Danh sách yêu cầu (REQ)
| ID | Yêu cầu | Loại | Nguồn/SoT |
|---|---|---|---|
| REQ-01 | Đổi vai trò, khoá/mở khoá, reset mật khẩu tài khoản khác (F1-13) | Chức năng | `01-rd/req/identity.md` — F1-13 |
| REQ-02 | Mọi thay đổi ghi vào Nhật ký hệ thống (F1-14) | Chức năng | `01-rd/req/identity.md` — F1-14 |
| REQ-03 | Given-When-Then đầy đủ (—) | Chức năng | `01-rd/req/user_stories/a3_admin.md` (`US-A3-02`) |

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
| Yêu cầu module | `01-rd/req/identity.md` — F1-13, F1-14 |
| User story | `01-rd/req/user_stories/a3_admin.md` — `US-A3-02` |
| Khảo sát hệ thống | `01-rd/overview/system_survey.md` mục 7.3 — dòng `admin_user_management` |
| Màn liên quan | `01-rd/screens/admin/ADM0403_system_log.md` mục 3 — dòng log "trùng mã nguồn" củng cố Câu hỏi mở Q1 |
| Quyết định | `DEC-2026-0831-remove-plagiarism-report` |
| Prototype | `09-layoutBase/Admin - Người dùng.dc.html` — prototype đối chiếu chính của file này |
