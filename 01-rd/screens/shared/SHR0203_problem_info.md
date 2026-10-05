# RD (Yêu cầu hệ thống mới) — Chi tiết bài tập (chỉ đọc) / `SHR0203`

> Mã màn hình: `SHR0203` [SoT: 02-bd/_rules/bd-template-9sheet.md — mục 8].
> Slug chính tắc: `problem_info`. Slug mới, thêm 2026-10-02 theo `DEC-2026-1002-split-detail-and-edit-pages`; chưa có dòng trong `01-rd/overview/system_survey.md` mục 7.0 trước đợt này.
> Phạm vi/Bounded Context: `problem-bank` (F2). Actor: A2, A3 (màn dùng chung, phạm vi dữ liệu theo quyền). Dựng cho cả khu A3 (Admin) và khu A2 (Giảng viên) — **cập nhật 2026-10-03:** khu Giảng viên đã dựng cùng khuôn, mount tại `/instructor/problems/[problemId]`.
> Nguồn sự thật (SoT): `01-rd/req/problem-bank.md` (F2-01 tới F2-04, F2-10, F2-14, F2-15, F2-18), `01-rd/screens/shared/SHR0202_problem_authoring.md` (màn con cùng bản ghi), `.nexa/control/decision-registry.md` mục `DEC-2026-1002-split-detail-and-edit-pages`.
> Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

Nơi A3 và A2 **xem một bài toán đã soạn mà không có nguy cơ sửa nhầm**: đề bài, giới hạn, ví dụ mẫu,
tóm tắt bộ testcase, kết quả chạy kiểm đáp án mẫu, thuộc tính và cấu hình AI của bài. Trước 2026-10-02 không có màn
này: URL gốc của bản ghi là form soạn `problem_authoring`, nên mọi cú nhấp vào một dòng danh sách đều mở một phiên sửa.
Từ đợt này, URL gốc là trang chỉ đọc; form soạn chuyển xuống `/edit` (`DEC-2026-1002-split-detail-and-edit-pages`).

Màn này **chỉ đọc**, không đổi dữ liệu nào. Hành động duy nhất dẫn ra khỏi màn là nút **"Sửa bài"** sang
`problem_authoring` và nút quay lại danh sách `problem_management`.

**Chưa có prototype trong `09-layoutBase/` (`[Đợi nextjs]`).** Bố cục do bản dựng prototype 2026-10-02 chọn, theo khuôn
màn soạn (thanh đầu trang, rồi nội dung rộng và cột thuộc tính hẹp) `[SoT: Suy luận]`; chủ dự án chưa duyệt hình ảnh.

---

## 2. Phạm vi

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Hiển thị tiêu đề, mã, chủ đề, độ khó, trạng thái `Chưa xuất bản` / `Đã xuất bản`. Độ khó do ADMIN quản lý (danh mục dữ liệu; ba mức Dễ/Trung bình/Khó chỉ là dữ liệu khởi tạo), không gắn logic. | F2-01, F2-02, F2-15 | `01-rd/req/problem-bank.md` |
| Hiển thị đề bài dưới dạng **Markdown + LaTeX đã render** (không phải văn bản thô; HTML thô không được phân tích), ràng buộc dữ liệu, các ví dụ mẫu | F2-01 | `01-rd/req/problem-bank.md` — F2-01; hiện thực ở `05-coding/frontend/src/views/shared/problem-info/ui/problem-info-view.tsx:98-104` |
| Hiển thị đặc tả bài toán ở dạng chỉ đọc: chữ ký hàm chung và dòng chữ ký ở từng ngôn ngữ (Bọc hàm), định dạng stdin/stdout (Standard I/O), chiến lược so khớp (thêm 2026-10-03, `SHR0202` Q20) | F2-03, F2-04 | `01-rd/req/problem-bank.md` — F2-03, F2-04 |
| Hiển thị giới hạn tài nguyên của bài (thời gian, bộ nhớ, output, stack) | F2-10 | `01-rd/req/problem-bank.md` — F2-10 |
| Hiển thị số testcase đã duyệt, số testcase công khai, số testcase chờ duyệt, ma trận độ phủ theo loại ca | F2-05, F2-06, F2-14 | `01-rd/req/problem-bank.md` |
| Hiển thị kết quả lần chạy kiểm đáp án mẫu gần nhất (đạt/tổng) và mã đáp án mẫu ở dạng thu gọn | F2-18 | `01-rd/req/problem-bank.md` — F2-18 |
| Hiển thị chỉ dẫn AI riêng của bài và hai công tắc bảo vệ ở dạng chỉ đọc (Bật/Tắt) | F2-14, F5-17 | `01-rd/req/problem-bank.md` |
| Điều hướng: quay lại danh sách, sang form sửa, "Xem như người học" | — | `DEC-2026-1002-split-detail-and-edit-pages` |
| Gác quyền: cùng Function `PROBLEM_AUTHORING` với `problem_authoring`; A2 chỉ thấy bài của mình, A3 thấy toàn kho | F1-10 tới F1-12 | `01-rd/req/identity.md` |

Mã đáp án mẫu **chỉ A2/A3 thấy**, không bao giờ đưa vào prompt AI hay trả cho học viên (F2-18).

---

## 3. Ngoài phạm vi (Out of Scope)

| Hạng mục | Lý do |
| :--- | :--- |
| Mọi thao tác sửa: đổi trường, duyệt/loại testcase nháp, sinh testcase bằng AI, xuất bản, nhân bản, xoá | Thuộc `problem_authoring` (`SHR0202`) và `problem_management` (`SHR0201`). Màn này chỉ đọc. |
| Tỷ lệ AC, số lượt nộp, người soạn, ngày cập nhật gần nhất | Chưa có trong mô hình dữ liệu của màn soạn; thêm khi BD/DD chốt DTO, không bịa số. Ghi ở `06-plan/PROTOTYPE_DEBT.md` mục 17. |
| Lịch sử các phiên bản bộ testcase (F2-09) | Là panel trong tab Testcase của `problem_authoring`. |

---

## 4. Tiền đề và ràng buộc

- Route: `/admin/problems/[problemId]` (khu Admin) và `/instructor/problems/[problemId]` (khu Giảng viên, dựng 2026-10-03).
  Tham số `problemId` là mã bài bỏ dấu `#` (ví dụ `121`). Màn nhận prop bắt buộc `basePath` (gốc của khu) cho nút quay lại
  và nút "Sửa bài" `[SoT: 05-coding/frontend/src/views/shared/problem-info/ui/problem-info-view.tsx:41-46, 65, 77]`.
- `/admin/problems/new` và `/instructor/problems/new` là form tạo mới (`problem_authoring`), không phải màn này.
- Dữ liệu đọc qua cùng cổng đọc chi tiết bài của `problem-bank` mà `problem_authoring` dùng; màn này **không** có endpoint riêng
  `[SoT: Suy luận — thuộc DD]`.
- Mã không tồn tại thì hiện trạng thái không tìm thấy kèm nút quay lại danh sách `[SoT: Suy luận, theo `interview_question_authoring`]`.

---

## 5. Danh sách yêu cầu (REQ)

| REQ | Yêu cầu | Mã gốc |
| :--- | :--- | :--- |
| REQ-1 | Mở URL gốc của một bài thấy đầy đủ nội dung chỉ đọc, không có trường nhập nào. | F2-01 |
| REQ-2 | Số testcase đã duyệt và số chờ duyệt phản ánh đúng quy tắc: chỉ testcase đã duyệt được tính (RD sửa 2026-09-28). | F2-14 |
| REQ-3 | Nút "Sửa bài" dẫn tới `/edit` của đúng bài đó; không có cách nào sửa dữ liệu ngay trên màn này. | `DEC-2026-1002-split-detail-and-edit-pages` |
| REQ-4 | Danh sách `problem_management` dẫn vào màn này khi bấm tiêu đề, dẫn vào `/edit` khi bấm biểu tượng Sửa. | `DEC-2026-1002-split-detail-and-edit-pages` |
| REQ-5 | Mã đáp án mẫu ẩn mặc định (thu gọn), và chỉ hiển thị với người có quyền `PROBLEM_AUTHORING`. | F2-18 |
| REQ-6 | Đề bài (`body`) hiển thị dưới dạng Markdown + LaTeX đã render (GFM, công thức); HTML thô trong đề bài không được phân tích. Văn bản Markdown gốc chỉ xuất hiện trong ô nhập của form soạn (`SHR0202`). Cập nhật 2026-10-03. | F2-01 |
| REQ-7 | Màn hiển thị đặc tả bài toán ở dạng chỉ đọc (chữ ký hàm chung kèm dòng chữ ký từng ngôn ngữ, định dạng stdin/stdout, chiến lược so khớp); bài có kiểu ngoài lược đồ thì nêu rõ chỉ hỗ trợ Standard I/O. Sửa đặc tả chỉ ở `/edit`. | F2-03, F2-04 |

---

## 6. Yêu cầu dữ liệu

Màn đọc cùng bộ trường với `SHR0202` mục 6 (đề bài, ràng buộc, giới hạn, ví dụ, đáp án mẫu, testcase, chỉ dẫn AI); không
thêm trường nào. Chi tiết DTO thuộc BD `02-bd/screens/shared/SHR0203_problem_info.md`.

---

## 7. RACI và danh sách bàn giao

| Hạng mục | Người chịu trách nhiệm |
| :--- | :--- |
| Duyệt giao diện chi tiết bài tập | Chủ dự án (chưa duyệt) |
| BD, DD, mã | Nhóm phát triển AlgoPrep |

---

## 8. Kế hoạch và quy mô

Chưa xác định lịch — không tự suy luận. Quy mô hành vi: sáu yêu cầu ở mục 5.

---

## 9. Tham chiếu

| Loại | Tham chiếu |
| :--- | :--- |
| Yêu cầu module | `01-rd/req/problem-bank.md` — F2-01, F2-02, F2-05, F2-06, F2-10, F2-14, F2-15, F2-18; `01-rd/req/identity.md` — F1-10 tới F1-12. |
| Quyết định | `DEC-2026-1002-split-detail-and-edit-pages`; `DEC-2026-0825-shared-content-authoring-screens`. |
| Màn liên quan | `SHR0201_problem_management.md` — màn cha (danh sách); `SHR0202_problem_authoring.md` — màn anh em (form sửa). |
| Bản dựng prototype | `05-coding/frontend/src/views/shared/problem-info/ui/problem-info-view.tsx`; `05-coding/frontend/src/shared/ui/data/markdown-preview.tsx` (render đề bài). |
