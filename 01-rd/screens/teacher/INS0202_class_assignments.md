# RD (Yêu cầu hệ thống mới) — Giao bài cho lớp / `INS0202`

> Mã màn hình: `INS0202`, theo `02-bd/_rules/bd-template-9sheet.md` mục 8.
> Slug chính tắc: `class_assignments` — slug mới, tách khỏi `class_management` ngày 2026-08-28
> (`DEC-2026-0828-split-class-management-assignments`). Mô tả: "Giao bài tập theo lớp", mã liên quan F2-12.
> Bounded Context: `problem-bank` (gán bài toán cho lớp). Actor: A2 (Giáo viên). (F4-09a tới F4-09c và
> Bounded Context `judge-orchestration` từng gắn với slug này đã loại khỏi phạm vi 2026-08-28 —
> `DEC-2026-0828-remove-rejudge-scope`.)
> **Nguồn gốc:** trước 2026-08-28, nội dung file này nằm chung với `class_management` trong một file RD vì
> `01-rd/overview/system_survey.md` mục 7.2 gộp hai prototype có route/nav riêng biệt vào một slug hạt
> giống. Chủ dự án chốt tách theo đề xuất ở Câu hỏi mở Q1 (bản cũ của `class_management.md`): mỗi màn có
> route và component riêng ở FE Next.js nên mỗi màn một slug BD/DD. Lý do và phạm vi ảnh hưởng đầy đủ ở
> Quyết định: `DEC-2026-0828-split-class-management-assignments`.
> Quan hệ thật giữa hai màn, đọc trực tiếp từ code: đây là **hai mục điều hướng (nav item) độc lập trong
> cùng sidebar giáo viên**, không phải hai tab của một trang — mỗi file prototype có route riêng
> (`./Giáo viên - Lớp của tôi.dc.html`, `./Giáo viên - Bài tập của tôi.dc.html`) và mục nav riêng biệt
> ("Lớp của tôi" mã `LH`, "Bài tập của tôi" mã `BT`) [SoT: 09-layoutBase/Giáo viên - Lớp của tôi.dc.html:221-222].
> File này mô tả **hành vi và UX ở mức yêu cầu** của màn "Bài tập của tôi" — không lặp lại đặc tả chức năng
> chung đã có ở `01-rd/req/problem-bank.md` (F2) và `01-rd/req/user_stories/a2_instructor.md` (`US-A2-03`), chỉ trỏ tới và bổ
> sung phần đặc thù của màn: trạng thái màn, cấu trúc UI, và các câu hỏi mở phát sinh khi đối chiếu với
> prototype thật.
> Nguồn sự thật (SoT): các nguồn được dẫn chiếu trong bảng yêu cầu và chương 9.
> Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

Cho giáo viên (A2) quản lý danh sách bài toán đã gán cho lớp mình phụ trách, gán thêm bài từ ngân hàng bài
toán chung [SoT: 01-rd/overview/system_survey.md — mục 7.2 dòng `class_assignments` (trước tách); 01-rd/req/problem-bank.md — F2-12]. Mọi phạm vi hiển
thị và thao tác đều giới hạn trong lớp giáo viên đó phụ trách, gác bởi Function `CLASS_MANAGEMENT` trong ma
trận phân quyền [SoT: 01-rd/req/identity.md — F1-12; 01-rd/req/ai-review.md — F5-27]. (Yêu cầu chấm lại theo phạm vi lớp đã loại khỏi phạm
vi 2026-08-28 — `DEC-2026-0828-remove-rejudge-scope`.)

---

## 2. Phạm vi

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Giao bài toán đã công bố cho một lớp; chỉ sinh viên lớp đó thấy bài được giao | F2-12 | `01-rd/req/problem-bank.md` — F2-12 |
| Ma trận phân quyền — Function `CLASS_MANAGEMENT` gác quyền A2 theo phạm vi lớp phụ trách | F1-10, F1-12 | `01-rd/req/identity.md` — F1-10, F1-12 |
| Given-When-Then giao bài theo lớp | — | `01-rd/req/user_stories/a2_instructor.md` (`US-A2-03`) |

**Đã loại khỏi phạm vi 2026-08-28:** yêu cầu chấm lại (F4-09a→c) — xem
`DEC-2026-0828-remove-rejudge-scope`.

### 2.1 Cấu trúc hệ thống liên quan

| Màn/tài liệu | Vai trò | Bounded Context |
|---|---|---|
| Màn hiện tại | `INS0202` / `class_assignments` | `problem-bank` |
| Tài liệu yêu cầu | `01-rd/req/problem-bank.md` (F2-12) | Nguồn SoT của màn |
| Prototype | `09-layoutBase/Giáo viên - Bài tập của tôi.dc.html` | Bằng chứng bố cục và trạng thái |

**Chi tiết trạng thái và cấu trúc màn**

**Màn "Bài tập của tôi" (`09-layoutBase/Giáo viên - Bài tập của tôi.dc.html`)**

1. **Tiêu đề và tổng quan** — "Bài tập của tôi", phụ đề "18 bài đã gán cho lớp · từ ngân hàng bài toán
   chung" [SoT: 09-layoutBase/Giáo viên - Bài tập của tôi.dc.html:117-118] — xác nhận đúng tinh thần F2-12:
   bài toán được **gán từ** ngân hàng chung (`problem_list`/`problem_bank`), không tạo bài riêng ở đây.
2. **Nút "+ Gán từ ngân hàng bài toán"** — là một **thẻ `<a href>` điều hướng thẳng sang
   `./Ngân hàng bài toán.dc.html`**, không mở modal chọn lớp/bài ngay tại chỗ, không có tham số nào được
   truyền đi kèm [SoT: 09-layoutBase/Giáo viên - Bài tập của tôi.dc.html:120]. Nghĩa là luồng gán bài theo
   prototype hiện tại là: giáo viên rời màn này, sang màn ngân hàng bài toán, rồi (không rõ bằng cách nào,
   `problem_list.dc.html` cho học viên không có hành vi "gán cho lớp" nào) hoàn tất việc gán — luồng này
   chưa được dựng đầy đủ ở prototype. Xem Câu hỏi mở Q7.
3. **4 thẻ thống kê** — bài đã gán, lượt nộp tuần này (kèm delta), tỉ lệ AC trung bình 90 ngày (kèm delta),
   số cần chấm tay (AI chấm điểm thấp) [SoT: 09-layoutBase/Giáo viên - Bài tập của tôi.dc.html:259-264].
4. **Bộ lọc** — ô tìm theo tên bài, tab lọc theo lớp (Tất cả + 3 lớp), đếm số kết quả
   [SoT: 09-layoutBase/Giáo viên - Bài tập của tôi.dc.html:139-148, 266-271].
5. **Bảng danh sách bài đã gán** — cột: bài tập, chủ đề, độ khó (badge màu theo mức), **"Gán cho lớp"**
   hiển thị dạng chuỗi tĩnh nối tên các lớp được gán (ví dụ "Thuật toán K21, CTDL K22") — không phải danh
   sách có thể bấm vào từng lớp, lượt nộp, tỉ lệ AC, và nút "Chi tiết"
   [SoT: 09-layoutBase/Giáo viên - Bài tập của tôi.dc.html:152-167, 286-292].
6. **Sai liên kết trong nút "Chi tiết"** — nút "Chi tiết" của mỗi dòng bài toán trỏ tới
   `./Câu hỏi phỏng vấn.dc.html` (màn ngân hàng câu hỏi phỏng vấn, F6), không phải màn chi tiết bài toán
   (`problem_detail`) — đây là lỗi copy-paste rõ ràng ở prototype, không phải một liên kết nghiệp vụ thật
   [SoT: 09-layoutBase/Giáo viên - Bài tập của tôi.dc.html:165]. Không sửa prototype (chỉ tham khảo), nhưng
   ghi nhận ở đây để không mang lỗi này sang BD/DD — xem Câu hỏi mở Q8.
7. **Không có nút gỡ/thu hồi bài đã gán khỏi lớp** trong bảng — chỉ có "Chi tiết"
   [SoT: 09-layoutBase/Giáo viên - Bài tập của tôi.dc.html:164-166]. Xem Câu hỏi mở Q9.
8. **Không có UI cho chấm lại — và sẽ không bao giờ có:** tính năng chấm lại đã loại khỏi phạm vi 2026-08-28
   (`DEC-2026-0828-remove-rejudge-scope`).

**Đối chiếu hai chiều với `problem_list` (học viên)**

`01-rd/screens/users/USR0101_problem_list.md` mục 3.4 mô tả khu "Bài tập lớp" ở phía học viên là **một khối tổng
hợp riêng cạnh bảng chính**, không lồng vào từng dòng bài toán
[SoT: 01-rd/screens/users/USR0101_problem_list.md:44-48]. Ở phía giáo viên, cột "Gán cho lớp" trong bảng
`Bài tập của tôi.dc.html` liệt kê tên lớp dưới dạng text tĩnh trong từng dòng bài toán
[SoT: 09-layoutBase/Giáo viên - Bài tập của tôi.dc.html:161]. Hai cách trình bày này **không mâu thuẫn**
nhau về nghiệp vụ (cùng phản ánh quan hệ N-N bài toán ↔ lớp của F2-12) vì mỗi bên nhìn từ góc riêng
(học viên nhìn theo lớp mình học; giáo viên nhìn theo bài toán mình quản lý), nhưng **chưa có màn nào cho
giáo viên xem/gỡ chi tiết theo từng lớp** như phía học viên xem theo lớp — liên quan Câu hỏi mở Q9.

---

## 3. Ngoài phạm vi (Out of Scope)

- Layout, spacing, bảng màu, component cụ thể (glass-morphism, breakpoint responsive) — thuộc BD
  (`02-bd/screens/teacher/INS0202_class_assignments.md`, chưa viết).
- Hợp đồng API (danh sách bài đã gán, gán/gỡ bài) — thuộc DD (`03-dd/api/problem-bank.md`, chưa viết).
- Màn "Lớp của tôi" (tổng quan lớp, danh sách học viên) — slug chị em
  `01-rd/screens/teacher/INS0201_class_management.md` (`DEC-2026-0828-split-class-management-assignments`).
- Màn "Chấm bài" (`Giáo viên - Chấm bài.dc.html`, F5-27, `US-A2-06`) và "Tiến độ học viên"
  (`Giáo viên - Tiến độ học viên.dc.html`) — là các slug màn khác theo mục 7.2 của `system_survey.md`,
  không thuộc phạm vi file này dù cùng actor A2 và có liên hệ số liệu.
- Luồng CRUD lớp học (tạo/sửa/xoá lớp, thêm/xoá học viên) — thuộc `class_management.md` Câu hỏi mở Q2/Q4,
  không thuộc file này.

---

## 4. Tiền đề và ràng buộc

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q7 | ~~Nút "+ Gán từ ngân hàng bài toán" chỉ điều hướng sang `Ngân hàng bài toán.dc.html`...~~ **ĐÃ CHỐT 2026-08-31 (owner instruction, theo đúng đề xuất):** nút "Giao cho lớp này" + modal chọn lớp ngay tại `problem_list` (góc nhìn giáo viên), không điều hướng rời trang. | — | Xem `DEC-2026-0831-class-assignments-round2`. Đụng tới cách `problem_list` hiển thị khác nhau theo vai trò — ghi nhận khi viết BD/DD cho cả hai màn. | Đã đóng |
| Q8 | ~~Nút "Chi tiết"...trỏ nhầm sang `Câu hỏi phỏng vấn.dc.html`...~~ **Xác nhận: lỗi kỹ thuật prototype (copy-paste), không phải liên kết nghiệp vụ.** | — | Không sửa prototype. Khi viết BD/DD, nút "Chi tiết" của giáo viên trên mỗi bài toán trỏ tới `problem_detail` (góc nhìn giáo viên) hoặc view xem-trước, không phải `interview-bank`. | Đã đóng |
| Q9 | ~~Không có nút gỡ/thu hồi bài đã gán khỏi lớp...~~ **ĐÃ CHỐT 2026-08-31 (owner instruction, theo đúng đề xuất):** bổ sung hành động gỡ, **giữ nguyên lịch sử nộp bài cũ** — chỉ ẩn khỏi danh sách được giao từ thời điểm gỡ. | — | Đã ghi vào `01-rd/req/problem-bank.md` (amendment F2-12), khác cách F1-26 xoá cascade khi gỡ học viên. Xem `DEC-2026-0831-class-assignments-round2`. | Đã đóng |

---

## 5. Danh sách yêu cầu (REQ)

| ID | Yêu cầu | Loại | Nguồn/SoT |
|---|---|---|---|
| REQ-01 | Giao bài toán đã công bố cho một lớp; chỉ sinh viên lớp đó thấy bài được giao (F2-12) | Chức năng | `01-rd/req/problem-bank.md` — F2-12 |
| REQ-02 | Ma trận phân quyền — Function `CLASS_MANAGEMENT` gác quyền A2 theo phạm vi lớp phụ trách (F1-10, F1-12) | Chức năng | `01-rd/req/identity.md` — F1-10, F1-12 |
| REQ-03 | Given-When-Then giao bài theo lớp (—) | Chức năng | `01-rd/req/user_stories/a2_instructor.md` (`US-A2-03`) |

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
| Khảo sát hệ thống | `01-rd/overview/system_survey.md` — mục 7.2 dòng `class_assignments`, cập nhật thành hai dòng theo `DEC-2026-0828-split-class-management-assignments`. |
| Yêu cầu module | `01-rd/req/identity.md` — F1-10 tới F1-12. |
| Yêu cầu module | `01-rd/req/problem-bank.md` — F2-12. |
| Yêu cầu module | `01-rd/req/ai-review.md` — F5-27. |
| User story | `01-rd/req/user_stories/a2_instructor.md` — `US-A2-03`. |
| Màn hình liên quan | `01-rd/screens/users/USR0101_problem_list.md:44-48` — mô tả khu "Bài tập lớp" phía học viên, đối chiếu hai chiều mục 2 của file này. |
| Màn hình liên quan | `01-rd/screens/teacher/INS0201_class_management.md` — slug chị em, tách ra 2026-08-28, mô tả "Lớp của tôi". |
| Prototype | `09-layoutBase/Giáo viên - Bài tập của tôi.dc.html` — prototype. |
| Quyết định | `DEC-2026-0828-split-class-management-assignments`. |
| Quyết định | `DEC-2026-0828-remove-rejudge-scope` (F4-09a→c loại khỏi phạm vi). |
| Quyết định | `DEC-2026-0831-class-assignments-round2`. |
| Tài liệu gốc | `README.md` mục 3 và mục 4 — định nghĩa Bounded Context và actor. |
