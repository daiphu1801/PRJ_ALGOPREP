# RD — Màn `admin_permission_matrix` (Ma trận phân quyền)

> Slug: `admin_permission_matrix` — khớp `01-rd/overview/system_survey.md` mục 7.3 [SoT: 01-rd/overview/system_survey.md — mục 7.3 dòng `admin_permission_matrix`].
> Bounded Context: `identity` (F1) [SoT: 01-rd/overview/system_survey.md — mục 7.3 dòng `admin_permission_matrix`]. Actor: A3.
>
> Đối chiếu prototype: `09-layoutBase/Admin - Ma trận phân quyền.dc.html`. File này mô tả **hành vi và UX ở
> mức yêu cầu** — không lặp lại đặc tả chức năng đã có ở `01-rd/req/identity.md` (F1-10→F1-12), chỉ trỏ tới và bổ
> sung phần đặc thù của màn. Khớp RD rất tốt — đây là màn được đặc tả kỹ nhất trong toàn bộ Phase 6, vì
> chính RD đã có sẵn danh sách 11 `FUNCTION` trước khi đối chiếu prototype.

## 1. Mục đích màn hình

Cấu hình ma trận quyền Role × Function × Action (`CREATE`/`READ`/`UPDATE`/`DELETE`), có hiệu lực ngay, để
kiểm soát ai được làm gì trong khu vực quản trị mà không phải sửa mã
[SoT: 01-rd/req/identity.md — F1-10, F1-11, F1-12].

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Ma trận phân quyền theo vai trò, đổi có hiệu lực ngay | F1-10 | `01-rd/req/identity.md` — F1-10 |
| Function/Action là seed cố định chỉ đọc; Role tạo/sửa/xoá được | F1-11 | `01-rd/req/identity.md` — F1-11 |
| Danh sách 11 Function trong phạm vi ma trận | F1-12 | `01-rd/req/identity.md` — F1-12 |
| Given-When-Then đầy đủ | — | `01-rd/req/user_stories/a3_admin.md` (`US-A3-01`) |

## 3. Trạng thái và cấu trúc màn (screen states)

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

## 4. Given-When-Then bổ sung ở mức màn

- **Cho** quản trị viên bấm "+ Vai trò mới" và đặt tên, **Khi** xác nhận tạo, **Thì** vai trò mới xuất hiện
  ở tab, mặc định không có quyền nào (mọi ô tắt) cho tới khi quản trị viên tự cấp
  [SoT: 09-layoutBase/Admin - Ma trận phân quyền.dc.html:464-473].
- **Cho** quản trị viên chọn vai trò STUDENT, **Khi** xem bảng ma trận, **Thì** mọi ô đều tắt tương tác
  (`disabled`), không phải vì STUDENT không có quyền gì mà vì quyền học tập cơ bản của STUDENT nằm ngoài
  phạm vi ma trận này (Lớp 1) — tránh hiểu nhầm STUDENT bị khoá toàn bộ.

## 5. Câu hỏi mở

Không có câu hỏi mở nào đáng kể ở màn này — RD (F1-10→F1-12) và prototype khớp gần như hoàn toàn, đây là
trường hợp hiếm trong toàn bộ dự án nơi RD được viết đủ chi tiết (danh sách 11 Function) trước khi đối chiếu
UI thật.

## 6. Ngoài phạm vi file này

- Bảng màu, spacing, component cụ thể — thuộc BD (`02-bd/screens/admin/admin_permission_matrix.md`, chưa
  viết).
- Hợp đồng API (CRUD Role, cập nhật ô ma trận, đọc Function/Action seed) — thuộc DD
  (`03-dd/api/identity.md`, chưa viết).
- Ánh xạ `FUNCTION:ACTION` sang `@PreAuthorize` cụ thể trong mã nguồn — thuộc thiết kế backend, không thuộc
  file RD theo trục màn này.

## 7. Tham chiếu

- `01-rd/req/identity.md` — F1-10, F1-11, F1-12.
- `01-rd/req/user_stories/a3_admin.md` — `US-A3-01`.
- `01-rd/overview/system_survey.md` mục 7.3 — dòng `admin_permission_matrix`.
- `09-layoutBase/Admin - Ma trận phân quyền.dc.html` — prototype.
