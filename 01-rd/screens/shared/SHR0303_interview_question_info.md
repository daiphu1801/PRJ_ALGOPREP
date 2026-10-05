# RD (Yêu cầu hệ thống mới) — Chi tiết câu hỏi phỏng vấn (chỉ đọc) / `SHR0303`

> Mã màn hình: `SHR0303` [SoT: 02-bd/_rules/bd-template-9sheet.md — mục 8].
> Slug chính tắc: `interview_question_info`. Slug mới, thêm 2026-10-02 theo `DEC-2026-1002-split-detail-and-edit-pages`; chưa có dòng trong `01-rd/overview/system_survey.md` mục 7.0 trước đợt này.
> Phạm vi/Bounded Context: `interview-bank` (F6). Actor: A2, A3 (màn dùng chung, phạm vi dữ liệu theo quyền). Dựng cho cả khu A3 (Admin) từ 2026-10-02 và khu A2 (Giảng viên) từ 2026-10-03.
> Nguồn sự thật (SoT): `01-rd/req/interview-bank.md` (F6-01, F6-08, F6-13), `01-rd/screens/shared/SHR0302_interview_question_authoring.md` (màn con cùng bản ghi), `.nexa/control/decision-registry.md` mục `DEC-2026-1002-split-detail-and-edit-pages`.
> Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

Nơi A3 và A2 **xem một câu hỏi phỏng vấn trong ngân hàng mà không có nguy cơ sửa nhầm**: nội dung câu hỏi,
các câu hỏi đào sâu, bộ tiêu chí đánh giá có trọng số, phân loại, và số liệu sử dụng. Trước 2026-10-02 không có màn này:
URL `/admin/interview-questions/[mã]` là form soạn `interview_question_authoring` (RD `SHR0302` mục 1, Câu hỏi mở Q6,
chốt 2026-09-01), nên bấm vào một dòng danh sách là mở một phiên sửa. Từ đợt này URL đó là trang chỉ đọc; form soạn chuyển
xuống `/edit` (`DEC-2026-1002-split-detail-and-edit-pages`, thay riêng phần khoá route của Q6).

Màn này **chỉ đọc**. Hành động dẫn ra khỏi màn: nút **"Sửa câu hỏi"** sang `interview_question_authoring` và nút quay lại
danh sách `interview_question_management`.

**Chưa có prototype trong `09-layoutBase/` (`[Đợi nextjs]`).** Bố cục theo khuôn hai cột của `SHR0302` (nội dung bên rộng,
phân loại và sử dụng bên hẹp) `[SoT: Suy luận]`; chủ dự án chưa duyệt hình ảnh.

---

## 2. Phạm vi

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Hiển thị mã câu hỏi, chủ đề (nhãn đọc từ danh mục do ADMIN quản lý), độ khó (cũng do ADMIN quản lý — danh mục dữ liệu, giống chủ đề; ba mức Dễ/Trung bình/Khó chỉ là dữ liệu khởi tạo; cập nhật 2026-10-03) | F6-01 | `01-rd/req/interview-bank.md` — F6-01 |
| Hiển thị nội dung câu hỏi dưới dạng Markdown kèm công thức LaTeX đã dựng (cập nhật 2026-10-03) và danh sách câu hỏi đào sâu | F6-13 | `01-rd/req/interview-bank.md` — F6-13 |
| Hiển thị bộ tiêu chí đánh giá và tổng trọng số; bộ trống thì báo rõ câu hỏi chưa có mặt ở Chế độ luyện | F6-13, F6-08 | `01-rd/req/interview-bank.md` — F6-13, F6-08 |
| Hiển thị số lần dùng trong phiên, điểm trung bình trên thang 5, trạng thái Chế độ luyện (mở khi có ít nhất một tiêu chí) | F6-13 | `01-rd/req/interview-bank.md` |
| Điều hướng: quay lại danh sách, sang form sửa, "Xem như học viên" | — | `DEC-2026-1002-split-detail-and-edit-pages` |
| Gác quyền: cùng Function `INTERVIEW_BANK_MANAGEMENT`; ngân hàng dùng chung nên A2 và A3 đều thấy toàn bộ | F1-12 | `01-rd/req/identity.md`; `01-rd/req/interview-bank.md` — F6-13 |

---

## 3. Ngoài phạm vi (Out of Scope)

| Hạng mục | Lý do |
| :--- | :--- |
| Sửa, nhân bản, xoá mềm | Thuộc `interview_question_authoring` (`SHR0302`) và `interview_question_management` (`SHR0301`). |
| Các trường chỉ phục vụ học viên (ý cần nói, từ khoá cốt lõi, khung trả lời chuẩn, lịch sử luyện) | Là của `USR0402_interview_question_detail`; màn quản trị không đọc chúng. |
| Quản lý danh mục chủ đề | Hộp thoại "Quản lý chủ đề" của `SHR0301`, chỉ ADMIN. |

---

## 4. Tiền đề và ràng buộc

- Route: `/admin/interview-questions/[questionId]` (khu Admin) và `/instructor/interview-questions/[questionId]` (khu Giảng viên, tách cùng cấu trúc từ 2026-10-03); `questionId` là mã câu hỏi (ví dụ `IQ-014`).
  Cả hai route cùng dùng một view, khác nhau ở tiền tố khu vực truyền vào view qua prop `basePath` [SoT: 05-coding/frontend/src/views/shared/interview-question-info/ui/interview-question-info-view.tsx:31-36].
- `/admin/interview-questions/new` và `/instructor/interview-questions/new` là form tạo mới (`SHR0302`), không phải màn này.
- Mã không tồn tại thì hiện trạng thái không tìm thấy kèm nút quay lại danh sách (dùng chung chuỗi thông báo với `SHR0302`).
- Câu hỏi đã xoá mềm không xuất hiện ở danh sách; cách màn này xử lý mã của câu hỏi đã xoá mềm chưa chốt (Câu hỏi mở Q1).

---

## 5. Danh sách yêu cầu (REQ)

| REQ | Yêu cầu | Mã gốc |
| :--- | :--- | :--- |
| REQ-1 | Mở URL gốc của một câu hỏi thấy đầy đủ nội dung chỉ đọc, không có trường nhập nào. | F6-13 |
| REQ-2 | Bộ tiêu chí hiển thị kèm tổng trọng số; bộ trống hiện thông báo thay vì bảng rỗng. | F6-13, F6-08 |
| REQ-3 | Nút "Sửa câu hỏi" dẫn tới `/edit` của đúng câu hỏi đó. | `DEC-2026-1002-split-detail-and-edit-pages` |
| REQ-4 | Danh sách dẫn vào màn này khi bấm nội dung câu hỏi, dẫn vào `/edit` khi bấm biểu tượng Sửa; tiền tố URL theo khu vực đang đứng (Admin hoặc Giảng viên). | `DEC-2026-1002-split-detail-and-edit-pages` |
| REQ-5 | Nội dung câu hỏi hiển thị dưới dạng Markdown và công thức LaTeX đã dựng (GFM, `$...$`, `$$...$$`); HTML thô gõ trong nội dung hiện như văn bản, không được dựng thành phần tử. Văn bản gốc chỉ nằm ở form soạn `SHR0302`. (Cập nhật 2026-10-03.) [SoT: 05-coding/frontend/src/shared/ui/data/markdown-preview.tsx:1-6, 36-38; 05-coding/frontend/src/views/shared/interview-question-info/ui/interview-question-info-view.tsx:80-82] | F6-13 |

---

## 6. Yêu cầu dữ liệu

Cùng bộ trường với `SHR0302` mục 6 cộng hai chỉ số đọc: số lần dùng và điểm trung bình. Chi tiết DTO thuộc BD
`02-bd/screens/shared/SHR0303_interview_question_info.md`.

---

## 7. RACI và danh sách bàn giao

| Hạng mục | Người chịu trách nhiệm |
| :--- | :--- |
| Duyệt giao diện chi tiết câu hỏi | Chủ dự án (chưa duyệt) |
| BD, DD, mã | Nhóm phát triển AlgoPrep |

---

## 8. Kế hoạch và quy mô

Chưa xác định lịch — không tự suy luận. Quy mô hành vi: năm yêu cầu ở mục 5.

### Câu hỏi mở

| ID | Câu hỏi | Trạng thái |
| :--- | :--- | :--- |
| Q1 | Mở trang chi tiết bằng mã của một câu hỏi đã xoá mềm thì hiện không tìm thấy, hay hiện chỉ đọc kèm nhãn "đã ngừng dùng"? | Mở |

---

## 9. Tham chiếu

| Loại | Tham chiếu |
| :--- | :--- |
| Yêu cầu module | `01-rd/req/interview-bank.md` — F6-01, F6-08, F6-13; `01-rd/req/identity.md` — F1-12. |
| Quyết định | `DEC-2026-1002-split-detail-and-edit-pages`; `DEC-2026-0830-interview-bank-crud`; `DEC-2026-1001-admin-configurable-settings`. |
| Màn liên quan | `SHR0301_interview_question_management.md` — màn cha (danh sách); `SHR0302_interview_question_authoring.md` — màn anh em (form sửa). |
| Bản dựng prototype | `05-coding/frontend/src/views/shared/interview-question-info/ui/interview-question-info-view.tsx`; thành phần dựng Markdown dùng chung `05-coding/frontend/src/shared/ui/data/markdown-preview.tsx`. |
