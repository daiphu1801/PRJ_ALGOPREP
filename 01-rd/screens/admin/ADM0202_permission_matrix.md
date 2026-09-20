# RD (Yêu cầu hệ thống mới) — Ma trận phân quyền / `ADM0202`

> Mã màn hình: `ADM0202` [Nguồn: 02-bd/_rules/bd-template-9sheet.md — mục 8].
> Slug chính tắc: `admin_permission_matrix` [SoT: 01-rd/overview/system_survey.md — mục 7.3 dòng `admin_permission_matrix`].
> Phạm vi/Bounded Context: `identity` (F1) [SoT: 01-rd/overview/system_survey.md — mục 7.3 dòng `admin_permission_matrix`]. Actor: A3 (Quản trị viên).
> Nguồn sự thật (SoT): `01-rd/req/identity.md` (F1-10 tới F1-12), `09-layoutBase/Admin - Ma trận phân quyền.dc.html`
> (prototype), `01-rd/req/user_stories/a3_admin.md` (`US-A3-01`).
> Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

Cấu hình ma trận quyền Role × Function × Action (`CREATE`/`READ`/`UPDATE`/`DELETE`), có hiệu lực ngay, để
kiểm soát ai được làm gì trong khu vực quản trị mà không phải sửa mã
[SoT: 01-rd/req/identity.md — F1-10, F1-11, F1-12].

Đối chiếu prototype: `09-layoutBase/Admin - Ma trận phân quyền.dc.html`. File này mô tả **hành vi và UX ở
mức yêu cầu** — không lặp lại đặc tả chức năng đã có ở `01-rd/req/identity.md` (F1-10→F1-12), chỉ trỏ tới và bổ
sung phần đặc thù của màn. Khớp RD rất tốt — đây là màn được đặc tả kỹ nhất trong toàn bộ Phase 6, vì
chính RD đã có sẵn danh sách 11 `FUNCTION` trước khi đối chiếu prototype.

---

## 2. Phạm vi

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Ma trận phân quyền theo vai trò, đổi có hiệu lực ngay | F1-10 | `01-rd/req/identity.md` — F1-10 |
| Function/Action là seed cố định chỉ đọc; Role tạo/sửa/xoá được | F1-11 | `01-rd/req/identity.md` — F1-11 |
| Danh sách 11 Function trong phạm vi ma trận | F1-12 | `01-rd/req/identity.md` — F1-12 |
| Given-When-Then đầy đủ | — | `01-rd/req/user_stories/a3_admin.md` (`US-A3-01`) |

Đối chiếu `09-layoutBase/Admin - Ma trận phân quyền.dc.html`:

1. **Ghi chú đầu trang** — nhắc lại đúng "Lớp 1 — quyền học tập cơ bản không đi qua ma trận" (dòng 163-165)
   — khớp chính xác câu chữ đã chốt ở `identity.md` (Lớp 1/Lớp 2).
2. **Tab chọn Role** — STUDENT/INSTRUCTOR/ADMIN, nút "+ Vai trò mới", nút "Xoá vai trò này" (ẩn với vai trò
   hệ thống, hiện "Vai trò hệ thống · không thể xoá" thay vào đó) (dòng 168-189) — khớp đúng F1-11 (không
   xoá được vai trò hệ thống).
3. **Khi chọn STUDENT** — ghi chú riêng "STUDENT có sẵn toàn bộ quyền học tập cơ bản..., các ô dưới đây chỉ
   để tham khảo và không chỉnh được" (dòng 192-196), toàn bộ ô trong bảng bị `disabled` (dòng 425, 431) —
   khớp đúng Lớp 1/Lớp 2 đã chốt, một UX quyết định tốt: hiện ma trận cho STUDENT ở dạng chỉ đọc để không
   gây hiểu nhầm là STUDENT không có quyền gì, thay vì ẩn hẳn tab.
4. **Bảng ma trận** — **cập nhật 2026-08-28:** 10 hàng Function (F1-12 sau khi bỏ `REJUDGE_MANAGEMENT`,
   `DEC-2026-0828-remove-rejudge-scope` — prototype ghi 11 hàng vì dựng trước khi có quyết định này), 4 cột
   Action (Tạo/Xem/Sửa/Xoá), mỗi ô là nút toggle vuông (dòng 198-224, 286-298).
5. **Dữ liệu mẫu quyền INSTRUCTOR** — có quyền đầy đủ trên `PROBLEM_AUTHORING`, `TESTCASE_MANAGEMENT`,
   `CLASS_MANAGEMENT`, `INTERVIEW_BANK_MANAGEMENT`. Prototype còn có `C`/`R` trên `REJUDGE_MANAGEMENT` —
   Function này **đã loại bỏ 2026-08-28** cùng tính năng chấm lại, không dựng vào UI thật.

### 2.1 Cấu trúc hệ thống liên quan

| Màn/tài liệu | Vai trò | Bounded Context |
|---|---|---|
| Màn hiện tại | `ADM0202` / `admin_permission_matrix` | `identity` |
| Tài liệu yêu cầu | `01-rd/req/identity.md` (F1-10 tới F1-12) | Nguồn SoT của màn |
| Prototype | `09-layoutBase/Admin - Ma trận phân quyền.dc.html` | Bằng chứng bố cục và trạng thái |

**Chi tiết trạng thái và cấu trúc màn:**

| Thành phần | Vai trò | Nguồn |
| :--- | :--- | :--- |
| `01-rd/screens/admin/ADM0101_overview.md` | Màn cha — đích nav "Ma trận phân quyền" trỏ tới màn này | [SoT: 01-rd/screens/admin/ADM0101_overview.md:159] |
| `09-layoutBase/Admin - Ma trận phân quyền.dc.html` | Prototype đối chiếu bố cục và hành vi | [SoT: 09-layoutBase/Admin - Ma trận phân quyền.dc.html] |
| Bounded Context `identity` | BC sở hữu ma trận Role × Function × Action | [SoT: 01-rd/overview/system_survey.md — mục 7.3 dòng `admin_permission_matrix`] |

---

## 3. Ngoài phạm vi (Out of Scope)

- Bảng màu, spacing, component cụ thể — thuộc BD (`02-bd/screens/admin/ADM0202_permission_matrix.md`, chưa
  viết).
- Hợp đồng API (CRUD Role, cập nhật ô ma trận, đọc Function/Action seed) — thuộc DD
  (`03-dd/api/identity.md`, chưa viết).
- Ánh xạ `FUNCTION:ACTION` sang `@PreAuthorize` cụ thể trong mã nguồn — thuộc thiết kế backend, không thuộc
  file RD theo trục màn này.

---

## 4. Tiền đề và ràng buộc

Không có câu hỏi mở nào đáng kể ở màn này — RD (F1-10→F1-12) và prototype khớp gần như hoàn toàn, đây là
trường hợp hiếm trong toàn bộ dự án nơi RD được viết đủ chi tiết (danh sách 11 Function) trước khi đối chiếu
UI thật.

---

## 5. Danh sách yêu cầu (REQ)
| ID | Yêu cầu | Loại | Nguồn/SoT |
|---|---|---|---|
| REQ-01 | Ma trận phân quyền theo vai trò, đổi có hiệu lực ngay (F1-10) | Chức năng | `01-rd/req/identity.md` — F1-10 |
| REQ-02 | Function/Action là seed cố định chỉ đọc; Role tạo/sửa/xoá được (F1-11) | Chức năng | `01-rd/req/identity.md` — F1-11 |
| REQ-03 | Danh sách 11 Function trong phạm vi ma trận (F1-12) | Chức năng | `01-rd/req/identity.md` — F1-12 |
| REQ-04 | Given-When-Then đầy đủ (—) | Chức năng | `01-rd/req/user_stories/a3_admin.md` (`US-A3-01`) |
| REQ-05 | Tạo vai trò mới: quản trị viên bấm "+ Vai trò mới" và đặt tên, khi xác nhận tạo thì vai trò mới xuất hiện ở tab, mặc định không có quyền nào (mọi ô tắt) cho tới khi quản trị viên tự cấp | Chức năng | `09-layoutBase/Admin - Ma trận phân quyền.dc.html:464-473` |
| REQ-06 | Khi quản trị viên chọn vai trò STUDENT và xem bảng ma trận, mọi ô đều tắt tương tác (`disabled`) — không phải vì STUDENT không có quyền gì mà vì quyền học tập cơ bản của STUDENT nằm ngoài phạm vi ma trận này (Lớp 1), tránh hiểu nhầm STUDENT bị khoá toàn bộ | Chức năng | `09-layoutBase/Admin - Ma trận phân quyền.dc.html:192-196, 425, 431` (đã đối chiếu ở mục 2.1, hạng mục 3) |

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
| Yêu cầu module | `01-rd/req/identity.md` — F1-10, F1-11, F1-12 |
| User story | `01-rd/req/user_stories/a3_admin.md` — `US-A3-01` |
| Khảo sát hệ thống | `01-rd/overview/system_survey.md` mục 7.3 — dòng `admin_permission_matrix` |
| Quyết định | `DEC-2026-0828-remove-rejudge-scope` |
| Prototype | `09-layoutBase/Admin - Ma trận phân quyền.dc.html` — prototype đối chiếu chính của file này |
