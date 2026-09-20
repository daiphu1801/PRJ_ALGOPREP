# Quy chuẩn bố cục RD

> Mục đích: chuẩn hoá các tài liệu RD của AlgoPrep theo cấu trúc của mẫu
> `11-layoutDocx/rd/RD_JKpi0910_TransportProgressConfirmation.md`. Quy chuẩn này chỉ
> điều chỉnh bố cục và cách trình bày; không tự suy diễn hoặc thay đổi nội dung nghiệp vụ.

---

## 1. Phạm vi áp dụng

- Áp dụng cho một RD tổng, RD theo module, và RD theo màn hình trong `01-rd/`.
- Với tài liệu mức màn hình, thay “hệ thống hiện hành” của mẫu bằng prototype, yêu cầu
  chức năng, user story, và màn liên quan của AlgoPrep.
- Không tạo mã yêu cầu mới chỉ vì đổi bố cục. Nếu cần mã `REQ-xx` cục bộ, mã đó phải
  dẫn chiếu rõ đến mã SoT như `F1-09` hoặc `US-A1-05`.
- Với RD trục màn hình, bắt buộc dùng **mã màn hình** đã cấp trong
  `02-bd/_rules/bd-template-9sheet.md` mục 8. Đây là mã định danh của màn, khác với
  mã yêu cầu `Fx-nn`, user story `US-...` và mã cục bộ `REQ-xx`.
- Với RD hệ thống trong `01-rd/system/`, dùng **mã tài liệu** dạng `SYSxxxx` ở H1 và
  metadata. Mã `SYSxxxx` định danh tài liệu hệ thống, không phải mã màn hình và không
  thay thế slug tên file. Tên file chuẩn là `<SYSxxxx>_<slug>.md`, ví dụ
  `SYS0101_backend_architecture.md`; khi đổi tên phải cập nhật toàn bộ tham chiếu.

## 2. Khung bắt buộc

Mỗi tài liệu RD theo chuẩn dùng đúng thứ tự các phần sau. Một phần không có dữ liệu vẫn
phải giữ tiêu đề và ghi rõ `Chưa xác định — không tự suy luận`.

```markdown
# RD (Yêu cầu hệ thống mới) — <Tên phạm vi> / `<MÃ màn>`

> Mã màn hình: ...
> Slug chính tắc: ...
> Phạm vi/Bounded Context: ...
> Nguồn sự thật (SoT): ...
> Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh
## 2. Phạm vi
### 2.1 Cấu trúc hệ thống liên quan
## 3. Ngoài phạm vi (Out of Scope)
## 4. Tiền đề và ràng buộc
## 5. Danh sách yêu cầu (REQ)
## 6. Yêu cầu dữ liệu
### 6.1 Dữ liệu tham chiếu
### 6.2 Luồng dữ liệu
## 7. RACI và danh sách bàn giao
## 8. Kế hoạch và quy mô
## 9. Tham chiếu
```

---

## 3. Quy tắc trình bày

| Thành phần | Quy tắc |
|---|---|
| Tiêu đề | Dùng một `#`, nêu loại RD, tên phạm vi và định danh trong code span. |
| Metadata đầu tài liệu | Dùng blockquote; RD theo màn bắt buộc có `Mã màn hình` và `Slug chính tắc`; mỗi mệnh đề quan trọng phải có SoT hoặc ghi rõ suy luận/chưa chốt. |
| Phân cách | Có đường `---` sau phần metadata và giữa các chương cấp 2 khi cần tách khối lớn. |
| Chương | Đánh số liên tục 1–9; chỉ dùng `###` cho tiểu mục như 2.1, 6.1, 6.2. |
| Bảng | Dùng bảng Markdown cho phạm vi, yêu cầu, dữ liệu, RACI, kế hoạch và tham chiếu. Cột ngắn, nội dung căn trái. |
| Yêu cầu | Bảng tối thiểu gồm `ID`, `Yêu cầu`, `Loại`, `Nguồn/SoT`. Mã cục bộ dùng `REQ-01`, `REQ-02`… và không thay thế mã `Fx-nn`. |
| Nguồn | Dùng dạng `[SoT: đường-dẫn — neo ngữ nghĩa]`; không neo theo số dòng trong tài liệu RD thay đổi thường xuyên. |
| Khoảng trống | Nêu rõ `Chưa xác định — không tự suy luận`, không bịa API, bảng dữ liệu, ngày, nhân sự hay quyết định. |
| Thuật ngữ | Giữ tiếng Việt nhất quán; tên kỹ thuật, slug, mã và đường dẫn đặt trong backtick. |

## 3.1 Quy tắc mã màn hình và tên file

Mã màn hình của RD phải dùng **chính mã trong bảng mã BD**, không tự cấp lại và không đổi mã đã có:
`ADM0101`, `USR0502`, `INS0201`, `SHR0101`… Nguồn chuẩn duy nhất là
`02-bd/_rules/bd-template-9sheet.md` mục 8, bao gồm cả khu vực, nhóm, thứ tự, tên rút gọn và slug chính tắc.

| Thành phần | Quy tắc RD |
|---|---|
| H1 | Dạng `# RD (Yêu cầu hệ thống mới) — <Tên màn> / \`<MÃ>\``. Ví dụ: `# RD (Yêu cầu hệ thống mới) — Hồ sơ / \`USR0502\``. |
| Metadata | Có hai dòng riêng: `Mã màn hình: \`<MÃ>\`` và `Slug chính tắc: \`<slug>\``. |
| Tên file RD | Bắt buộc dùng `<MÃ>_<tên-rút-gọn>.md`, ví dụ `01-rd/screens/users/USR0502_profile.md`. Tên rút gọn theo bảng mã BD: bỏ đúng tiền tố `admin_` hoặc `instructor_`, còn lại giữ nguyên. |
| Liên kết RD/BD/DD | Dùng đường dẫn có tiền tố mã ở cả ba trục, ví dụ `01-rd/screens/users/USR0502_profile.md` và `02-bd/screens/admin/ADM0101_overview.md`. Khi đổi tên, cập nhật toàn bộ tham chiếu Markdown và HTML liên quan trong cùng thay đổi. |
| Màn bị loại phạm vi | Giữ mã đã cấp nhưng ghi rõ trạng thái loại phạm vi; không tái sử dụng mã. |
| Màn/chức năng không phải màn hình | Không cấp mã màn hình cho tài liệu tổng quan, module, kiến trúc, NFR hoặc file quy tắc. |

Mã vùng đã cấp: `USR` (A1), `INS` (A2), `ADM` (A3), `SHR` (dùng chung). Mã màn **không thay thế**
slug: slug là khoá liên kết trong RD, còn mã là định danh ổn định để đồng bộ RD → BD → DD.

## 4. Ánh xạ từ cấu trúc RD cũ

| Cấu trúc cũ thường gặp | Vị trí trong khung chuẩn |
|---|---|
| Mục đích màn hình | 1. Mục đích và bối cảnh |
| Nguồn yêu cầu | 2. Phạm vi và 5. Danh sách yêu cầu |
| Screen states / Given-When-Then | 2. Phạm vi, 4. Tiền đề và ràng buộc, hoặc 5. Danh sách yêu cầu |
| Câu hỏi mở đã chốt | 4. Tiền đề và ràng buộc |
| Câu hỏi mở chưa chốt | 4. Tiền đề và ràng buộc, với trạng thái chưa xác định |
| Ngoài phạm vi file | 3. Ngoài phạm vi |
| API, dữ liệu, liên kết màn | 6. Yêu cầu dữ liệu |
| Deliverable, phase, việc chờ | 7. RACI và 8. Kế hoạch |
| Danh sách tài liệu nguồn | 9. Tham chiếu |

## 5. Danh sách kiểm trước khi duyệt

- [ ] Đủ 9 chương và đúng thứ tự.
- [ ] Chỉ có một `#`; metadata ở đầu tài liệu có SoT.
- [ ] Mọi yêu cầu trong chương 5 truy về được `Fx-nn`, `US-...`, quyết định hoặc prototype.
- [ ] Đã tách rõ phạm vi, ngoài phạm vi, ràng buộc và dữ liệu.
- [ ] Không biến chi tiết BD/DD/API chưa chốt thành yêu cầu RD.
- [ ] Phần 9 liệt kê toàn bộ nguồn đã dùng.
