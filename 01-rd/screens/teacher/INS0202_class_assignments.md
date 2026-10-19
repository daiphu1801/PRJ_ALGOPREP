# RD (Yêu cầu hệ thống mới) — Giao bài cho lớp / `INS0202`

> Mã màn hình: `INS0202` [Nguồn: 02-bd/_rules/bd-template-9sheet.md — mục 8].
> Slug chính tắc: `class_assignments` — slug mới, tách khỏi `class_management` ngày 2026-08-28
> [SoT: DEC-2026-0828-split-class-management-assignments].
> Phạm vi/Bounded Context: `problem-bank` (gán bài toán cho lớp). Actor: A2 (Giáo viên).
> Nguồn sự thật (SoT): `01-rd/req/problem-bank.md` (F2-12), `01-rd/req/identity.md` (F1-12),
> `09-layoutBase/Giáo viên - Bài tập của tôi.dc.html` (prototype), `01-rd/req/user_stories/a2_instructor.md` (`US-A2-03`).
> Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

Cho giáo viên (A2) quản lý danh sách bài toán đã gán cho lớp mình phụ trách, gán thêm bài từ ngân hàng bài
toán chung [SoT: 01-rd/overview/system_survey.md — mục 7.2 dòng `class_assignments` (trước tách); 01-rd/req/problem-bank.md — F2-12]. Mọi phạm vi hiển
thị và thao tác đều giới hạn trong lớp giáo viên đó phụ trách, gác bởi Function `CLASS_MANAGEMENT` trong ma
trận phân quyền [SoT: 01-rd/req/identity.md — F1-12]. (Tham chiếu F5-27 cũ đã bỏ 2026-10-13, `DEC-2026-1013-remove-instructor-manual-grading`.) (Yêu cầu chấm lại theo phạm vi lớp đã loại khỏi phạm
vi 2026-08-28 — `DEC-2026-0828-remove-rejudge-scope`; F4-09a tới F4-09c và Bounded Context
`judge-orchestration` từng gắn với slug này cũng đã loại khỏi phạm vi cùng đợt.)

**Nguồn gốc:** trước 2026-08-28, nội dung file này nằm chung với `class_management` trong một file RD vì
`01-rd/overview/system_survey.md` mục 7.2 gộp hai prototype có route/nav riêng biệt vào một slug hạt giống.
Chủ dự án chốt tách theo đề xuất ở Câu hỏi mở Q1 (bản cũ của `class_management.md`): mỗi màn có route và
component riêng ở FE Next.js nên mỗi màn một slug BD/DD. Lý do và phạm vi ảnh hưởng đầy đủ ở Quyết định:
`DEC-2026-0828-split-class-management-assignments`.

Quan hệ thật giữa hai màn, đọc trực tiếp từ code: đây là **hai mục điều hướng (nav item) độc lập trong cùng
sidebar giáo viên**, không phải hai tab của một trang — mỗi file prototype có route riêng (`./Giáo viên -
Lớp của tôi.dc.html`, `./Giáo viên - Bài tập của tôi.dc.html`) và mục nav riêng biệt ("Lớp của tôi" mã
`LH`, "Bài tập của tôi" mã `BT`) [SoT: 09-layoutBase/Giáo viên - Lớp của tôi.dc.html:221-222].

File này mô tả **hành vi và UX ở mức yêu cầu** của màn "Bài tập của tôi" — không lặp lại đặc tả chức năng
chung đã có ở `01-rd/req/problem-bank.md` (F2) và `01-rd/req/user_stories/a2_instructor.md` (`US-A2-03`), chỉ trỏ tới và bổ
sung phần đặc thù của màn: trạng thái màn, cấu trúc UI, và các câu hỏi mở phát sinh khi đối chiếu với
prototype thật.

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
   chưa được dựng đầy đủ ở prototype. Xem Câu hỏi mở Q7. ~~Luồng gán bài là modal tại `problem_list`.~~
   **Đã bị thay thế 2026-10-14 (`DEC-2026-1011-class-assignment-popup-on-assignments-screen`):** luồng gán là popup ngay trên `INS0202`.
3. **~~3~~ 2 thẻ thống kê** — bài đã gán, ~~lượt nộp tuần này (kèm delta)~~ **ĐÃ BỎ 2026-10-14 (DEC-2026-1014-instructor-batch1-counting-rules)**, tỉ lệ AC trung bình ~~90 ngày (kèm delta)~~ **tính toàn thời gian (all-time), bỏ cụm "90 ngày" và delta "-3%"** (ĐÃ BỎ 2026-10-14, cùng quyết định; phạm vi tính theo các lớp của giảng viên, `[SoT: Suy luận]` để khớp với cột "Lượt nộp", chủ dự án xác nhận nếu muốn toàn hệ thống)
   (prototype dòng 259-264 vẽ 4 thẻ; ~~thẻ "số cần chấm tay (AI chấm điểm thấp)"~~ **đã bỏ 2026-10-13**, `DEC-2026-1013-remove-instructor-manual-grading`)
   [SoT: 09-layoutBase/Giáo viên - Bài tập của tôi.dc.html:259-264].
4. **Bộ lọc** — ô tìm theo tên bài, tab lọc theo lớp (Tất cả + 3 lớp), đếm số kết quả
   [SoT: 09-layoutBase/Giáo viên - Bài tập của tôi.dc.html:139-148, 266-271].
5. **Bảng danh sách bài đã gán** — cột: bài tập, chủ đề, độ khó (badge màu theo mức), **"Gán cho lớp"**
   hiển thị dạng chuỗi tĩnh nối tên các lớp được gán (ví dụ "Thuật toán K21, CTDL K22") — không phải danh
   sách có thể bấm vào từng lớp, lượt nộp, tỉ lệ AC, và nút "Chi tiết"
   [SoT: 09-layoutBase/Giáo viên - Bài tập của tôi.dc.html:152-167, 286-292]. **Chốt 2026-10-14
   (DEC-2026-1014-instructor-batch1-counting-rules):** cột "Lượt nộp" và "AC" của một bài được tính **trong
   phạm vi các lớp của chính giảng viên**, không phải bộ đếm toàn hệ thống. **Chốt 2026-10-14
   (DEC-2026-1014-instructor-batch2-weekly-rules):** khi một tab lớp đang được chọn, "Lượt nộp" và "AC" tính
   cho đúng lớp đó; tab "Tất cả" tổng hợp mọi lớp của giảng viên
   [SoT: .nexa/control/decision-registry.md:3710-3712]. **Chốt 2026-10-14
   (`DEC-2026-1014-instructor-batch5-6-nav-pagination-rubric`):** bảng bài đã giao gộp nhiều dòng nên **phân trang 20
   dòng mỗi trang**; cột "Chủ đề" hiện chủ đề đầu tiên theo thứ tự chữ cái kèm hậu tố "+n", danh sách đầy đủ ở tooltip;
   màn gọi từ màn này sang màn khoan sâu thì sidebar sáng mục của màn gọi tới
   [SoT: .nexa/control/decision-registry.md:3761-3768].
6. **Sai liên kết trong nút "Chi tiết"** — nút "Chi tiết" của mỗi dòng bài toán trỏ tới
   `./Câu hỏi phỏng vấn.dc.html` (màn ngân hàng câu hỏi phỏng vấn, F6), không phải màn chi tiết bài toán
   (`problem_detail`) — đây là lỗi copy-paste rõ ràng ở prototype, không phải một liên kết nghiệp vụ thật
   [SoT: 09-layoutBase/Giáo viên - Bài tập của tôi.dc.html:165]. Không sửa prototype (chỉ tham khảo), nhưng
   ghi nhận ở đây để không mang lỗi này sang BD/DD — xem Câu hỏi mở Q8.
7. **Không có nút gỡ/thu hồi bài đã gán khỏi lớp** trong bảng — chỉ có "Chi tiết"
   [SoT: 09-layoutBase/Giáo viên - Bài tập của tôi.dc.html:164-166]. Xem Câu hỏi mở Q9.
   **Chốt 2026-10-14 (DEC-2026-1014-instructor-batch3-4-contracts-and-removal):** nút "Gỡ khỏi lớp" của một
   bài đã gán nằm sau nút "Chi tiết", luôn hiện rõ; mỗi lần gỡ chỉ gỡ **một lớp** (ở tab "Tất cả", lớp cần
   gỡ được chọn ngay trong hộp xác nhận); luôn có bước xác nhận. Luồng gán bài là popup ngay trên màn này
   `INS0202`, nên ý "truyền lớp đang chọn sang `problem_list`" đã đóng
   [SoT: .nexa/control/decision-registry.md:3737-3740].
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
- Màn "Tiến độ học viên" (`Giáo viên - Tiến độ học viên.dc.html`) — là slug màn khác theo mục 7.2 của
  `system_survey.md`, không thuộc phạm vi file này dù cùng actor A2 và có liên hệ số liệu. (Màn "Chấm bài",
  `F5-27`, `US-A2-06` đã bỏ 2026-10-13, `DEC-2026-1013-remove-instructor-manual-grading`.)
- Luồng CRUD lớp học (tạo/sửa/xoá lớp, thêm/xoá học viên) — thuộc `class_management.md` Câu hỏi mở Q2/Q4,
  không thuộc file này.

---

## 4. Tiền đề và ràng buộc

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q7 | ~~Nút "+ Gán từ ngân hàng bài toán" chỉ điều hướng sang `Ngân hàng bài toán.dc.html`...~~ **ĐÃ CHỐT 2026-08-31 (owner instruction, theo đúng đề xuất):** ~~nút "Giao cho lớp này" + modal chọn lớp ngay tại `problem_list` (góc nhìn giáo viên), không điều hướng rời trang.~~ **ĐÃ BỊ THAY THẾ 2026-10-14 bởi `DEC-2026-1011-class-assignment-popup-on-assignments-screen`:** popup "Giao bài cho lớp" nằm ngay trên màn `INS0202` (chọn một lớp, tick các bài đã công bố chưa giao cho lớp đó, nút "Giao n bài"), không còn ở `problem_list` [SoT: .nexa/control/decision-registry.md:3159-3165]. | — | ~~Xem `DEC-2026-0831-class-assignments-round2`. Đụng tới cách `problem_list` hiển thị khác nhau theo vai trò — ghi nhận khi viết BD/DD cho cả hai màn.~~ Phần "modal tại `problem_list`" của `DEC-2026-0831-class-assignments-round2` hết hiệu lực; phần gỡ mềm (F2-12) giữ nguyên. | Đã đóng |
| Q8 | ~~Nút "Chi tiết"...trỏ nhầm sang `Câu hỏi phỏng vấn.dc.html`...~~ **Xác nhận: lỗi kỹ thuật prototype (copy-paste), không phải liên kết nghiệp vụ.** | — | Không sửa prototype. Khi viết BD/DD, nút "Chi tiết" của giáo viên trên mỗi bài toán trỏ tới `problem_detail` (góc nhìn giáo viên) hoặc view xem-trước, không phải `interview-bank`. | Đã đóng |
| Q9 | ~~Không có nút gỡ/thu hồi bài đã gán khỏi lớp...~~ **ĐÃ CHỐT 2026-08-31 (owner instruction, theo đúng đề xuất):** bổ sung hành động gỡ, **giữ nguyên lịch sử nộp bài cũ** — chỉ ẩn khỏi danh sách được giao từ thời điểm gỡ. | — | Đã ghi vào `01-rd/req/problem-bank.md` (amendment F2-12), khác cách F1-26 xoá cascade khi gỡ học viên. Xem `DEC-2026-0831-class-assignments-round2`. | Đã đóng |
| 202-Q1 | Thẻ "Lượt nộp tuần này" có giữ không. **ĐÃ CHỐT 2026-10-14:** bỏ thẻ (bậc 2: không có số đếm theo tuần). | — | `DEC-2026-1014-instructor-batch1-counting-rules` | Đã đóng |
| 202-Q2 | Thẻ "AC trung bình" tính bao nhiêu ngày, có delta không. **ĐÃ CHỐT 2026-10-14:** toàn thời gian, bỏ "90 ngày" và delta "-3%". | — | `DEC-2026-1014-instructor-batch1-counting-rules` | Đã đóng |
| 202-Q3 | Cột "Lượt nộp"/"AC" tính toàn hệ thống hay theo lớp giảng viên. **ĐÃ CHỐT 2026-10-14:** theo các lớp của giảng viên (chủ dự án chọn "tính theo lớp ngay"). Hệ quả cho DD: cần một phép tổng hợp theo lớp ở `judge-orchestration`. | — | `DEC-2026-1014-instructor-batch1-counting-rules`. Ghi chú: RD file này chỉ có Q7-Q9; ba hàng này ghi nhận các câu hỏi cùng tên từ BD `02-bd/screens/teacher/INS0202_class_assignments.md`. | Đã đóng |
| 202-Q11 | Với tab lớp đang chọn, "Lượt nộp"/"AC" tính theo gì. **ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-instructor-batch2-weekly-rules):** theo lớp đang chọn; "Tất cả" tổng hợp mọi lớp của giảng viên. | — | Số thứ tự là của BD `02-bd/screens/teacher/INS0202_class_assignments.md`; RD file này không có câu hỏi mang số đó trước đây. | Đã đóng |
| 202-Q6, 202-Q7 | Vị trí, độ hiện, phạm vi lớp của nút "Gỡ khỏi lớp" ở bài đã gán; có truyền lớp sang `problem_list` không. **ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-instructor-batch3-4-contracts-and-removal):** sau nút "Chi tiết", luôn hiện; gỡ một lớp mỗi lần (tab "Tất cả" chọn lớp trong hộp xác nhận); không truyền lớp sang `problem_list` vì luồng gán là popup trên `INS0202`. | — | Số thứ tự là của BD `02-bd/screens/teacher/INS0202_class_assignments.md`. ~~Cảnh báo: Q7 cũ của RD (2026-08-31) ghi nút "Giao cho lớp này" ở `problem_list`; quyết định mới ghi luồng gán là popup trên `INS0202` — hai câu chưa khớp, cần chủ dự án xác nhận Q7 cũ có bị thay thế hay không.~~ **Đã xử lý 2026-10-14:** Q7 cũ bị thay thế bởi `DEC-2026-1011-class-assignment-popup-on-assignments-screen`, hai câu đã khớp. | Đã đóng |

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
| Yêu cầu module | `01-rd/req/ai-review.md` — F5-27 (đã bỏ 2026-10-13, `DEC-2026-1013-remove-instructor-manual-grading`). |
| User story | `01-rd/req/user_stories/a2_instructor.md` — `US-A2-03`. |
| Màn hình liên quan | `01-rd/screens/users/USR0101_problem_list.md:44-48` — mô tả khu "Bài tập lớp" phía học viên, đối chiếu hai chiều mục 2 của file này. |
| Màn hình liên quan | `01-rd/screens/teacher/INS0201_class_management.md` — slug chị em, tách ra 2026-08-28, mô tả "Lớp của tôi". |
| Prototype | `09-layoutBase/Giáo viên - Bài tập của tôi.dc.html` — prototype. |
| Quyết định | `DEC-2026-0828-split-class-management-assignments`. |
| Quyết định | `DEC-2026-0828-remove-rejudge-scope` (F4-09a→c loại khỏi phạm vi). |
| Quyết định | `DEC-2026-0831-class-assignments-round2`. |
| Tài liệu gốc | `README.md` mục 3 và mục 4 — định nghĩa Bounded Context và actor. |
