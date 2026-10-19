# RD (Yêu cầu hệ thống mới) — Quản lý lớp / `INS0201`

> Mã màn hình: `INS0201` [Nguồn: 02-bd/_rules/bd-template-9sheet.md — mục 8].
> Slug chính tắc: `class_management` [SoT: 01-rd/overview/system_survey.md — mục 7.2 dòng `class_management`].
> Phạm vi/Bounded Context: `identity` (danh sách học viên theo lớp, phân quyền A2)
> [SoT: 01-rd/overview/system_survey.md — mục 7.2 dòng `class_management`]. Actor: A2 (Giáo viên).
> Nguồn sự thật (SoT): `01-rd/req/identity.md` (F1-10, F1-12, F1-23 tới F1-27),
> `09-layoutBase/Giáo viên - Lớp của tôi.dc.html` (prototype), `DEC-2026-0828-split-class-management-assignments`.
> Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

Cho giáo viên (A2) xem tổng quan các lớp mình phụ trách, danh sách học viên và tiến độ của từng lớp
[SoT: 01-rd/overview/system_survey.md — mục 7.2 dòng `class_management`; 01-rd/req/problem-bank.md — F2-12]. Mọi phạm vi hiển thị và thao tác đều
giới hạn trong lớp giáo viên đó phụ trách, gác bởi Function `CLASS_MANAGEMENT` trong ma trận phân quyền
[SoT: 01-rd/req/identity.md — F1-12]. (Tham chiếu F5-27 cũ đã bỏ 2026-10-13, `DEC-2026-1013-remove-instructor-manual-grading`.) Mô tả gốc: "Quản lý lớp — tổng quan và
danh sách học viên" [SoT: 01-rd/overview/system_survey.md — mục 7.2 dòng `class_management`]. (F6-11 và
Bounded Context `interview-bank` từng gắn với slug này đã loại khỏi phạm vi 2026-08-28 —
`DEC-2026-0828-remove-per-class-interview-set`.)

**Tách slug 2026-08-28 (`DEC-2026-0828-split-class-management-assignments`):** file này trước đó gộp chung
hai prototype có route/nav riêng biệt — `09-layoutBase/Giáo viên - Lớp của tôi.dc.html` và `09-layoutBase/
Giáo viên - Bài tập của tôi.dc.html` — dưới một slug `class_management`, theo đúng câu hỏi mở Q1 nêu ra ở
bản trước của file này. Chủ dự án đã chốt tách theo đề xuất: **file này giờ chỉ mô tả "Lớp của tôi"**; phần
"Bài tập của tôi" chuyển sang slug mới `01-rd/screens/teacher/INS0202_class_assignments.md`. Lý do và phạm
vi ảnh hưởng đầy đủ ở Quyết định: `DEC-2026-0828-split-class-management-assignments`.

File này mô tả **hành vi và UX ở mức yêu cầu** của màn "Lớp của tôi" — không lặp lại đặc tả chức năng chung
đã có ở `01-rd/req/identity.md` (F1), chỉ trỏ tới và bổ sung phần đặc thù của màn: trạng thái màn, cấu trúc
UI, và các câu hỏi mở phát sinh khi đối chiếu với prototype thật.

---

## 2. Phạm vi

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Tạo lớp học mới với thông tin cơ bản; sinh mã mời; học viên tự tham gia lớp bằng mã mời | F1-23 | `01-rd/req/identity.md` — F1-23 (bổ sung 2026-08-28) |
| Xoá lớp học (xoá thật, hard delete) | F1-24 | `01-rd/req/identity.md` — F1-24 (bổ sung 2026-08-28) |
| Mã mời có thời hạn dùng | F1-25 | `01-rd/req/identity.md` — F1-25 (bổ sung 2026-08-28) |
| ~~Gỡ học viên khỏi lớp — xoá luôn lịch sử làm bài của học viên đó trong lớp~~ **ĐÃ ĐỔI 2026-10-14 (DEC-2026-1014-keep-learning-history-on-leave):** gỡ học viên khỏi lớp — giữ lịch sử làm bài, chỉ đánh dấu đã rời lớp | F1-26 | `01-rd/req/identity.md` — F1-26 (bổ sung 2026-08-28) |
| Xem hồ sơ chi tiết một học viên trong lớp phụ trách (màn/route riêng, slug mới `class_student_detail`) | F1-27 | `01-rd/req/identity.md` — F1-27 (bổ sung 2026-08-28) |
| Given-When-Then xem hồ sơ chi tiết học viên | — | `01-rd/req/user_stories/a2_instructor.md` (`US-A2-08`) |
| Ma trận phân quyền — Function `CLASS_MANAGEMENT` gác quyền A2 theo phạm vi lớp phụ trách | F1-10, F1-12 | `01-rd/req/identity.md` — F1-10, F1-12 |
| Given-When-Then tạo lớp và tham gia bằng mã mời | — | `01-rd/req/user_stories/a2_instructor.md` (`US-A2-07`) |

**Đã loại khỏi phạm vi 2026-08-28:** F6-11 (bộ câu hỏi phỏng vấn riêng theo lớp) và `US-A2-04` — xem
`DEC-2026-0828-remove-per-class-interview-set`.

### 2.1 Cấu trúc hệ thống liên quan

| Màn/tài liệu | Vai trò | Bounded Context |
|---|---|---|
| Màn hiện tại | `INS0201` / `class_management` | `identity` |
| Màn con (thêm 2026-10-11) | `INS0205` / `class_detail` — `01-rd/screens/teacher/INS0205_class_detail.md` | `identity` |
| Tài liệu yêu cầu | `01-rd/req/identity.md` (F1-10, F1-12, F1-23 tới F1-27) | Nguồn SoT của màn |
| Prototype | `09-layoutBase/Giáo viên - Lớp của tôi.dc.html` | Bằng chứng bố cục và trạng thái |

**Chi tiết trạng thái và cấu trúc màn**

**Màn "Lớp của tôi" (`09-layoutBase/Giáo viên - Lớp của tôi.dc.html`)**

1. **Tổng quan lớp** — tiêu đề "Lớp của tôi" kèm số liệu tổng "3 lớp · 82 học viên"; 3 thẻ thống kê: tổng số
   lớp, hoàn thành trung bình (%, ~~so với tuần trước~~ **ĐÃ BỎ 2026-10-14 (DEC-2026-1014-instructor-batch1-counting-rules)**: không hiển thị chênh lệch "+5% so với tuần trước" vì không có số liệu chụp theo tuần; chỉ còn giá trị hiện tại), số học viên vắng bài (prototype dòng 117-121, 253-258 vẽ 4 thẻ,
   ~~thẻ "số bài cần chấm tay"~~ **đã bỏ 2026-10-13**, `DEC-2026-1013-remove-instructor-manual-grading`)
   [SoT: 09-layoutBase/Giáo viên - Lớp của tôi.dc.html:117-121, 253-258].
2. **Nút "+ Tạo lớp mới"** — hiện ở đầu trang nhưng **không có `onClick` hay hành vi nào được lập trình**
   trong `renderVals()`/`Component` (không phải link, không mở modal, không set state) — chỉ là phần tử
   tĩnh chưa nối hành vi [SoT: 09-layoutBase/Giáo viên - Lớp của tôi.dc.html:120]. Đây là bằng chứng
   prototype chưa dựng luồng tạo lớp. **Chốt 2026-08-28 (F1-23 tới F1-26, `US-A2-07`):** bấm nút này mở
   luồng tạo lớp với thông tin cơ bản; sau khi tạo, hệ thống trả về một mã mời (có thời hạn dùng) để giáo
   viên chia sẻ cho học viên. Giáo viên xoá được lớp (xoá thật) và gỡ được học viên khỏi lớp (~~xoá luôn lịch sử làm bài của học viên đó trong lớp~~ **ĐÃ ĐỔI 2026-10-14 (DEC-2026-1014-keep-learning-history-on-leave):** giữ lịch sử làm bài, chỉ đánh dấu đã rời lớp; **ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-class-leave-details):** học viên đã rời lớp không được tính vào số học viên, tiến độ của lớp và số liệu của `INS0202`; học viên vào lại bằng mã mời thì dùng lại dòng ghi danh cũ và được tính lại); giáo viên tạo được nhiều mã mời mới cho cùng lớp, mỗi mã có hạn
   dùng riêng. **Chốt 2026-10-14 (DEC-2026-1014-instructor-batch3-4-contracts-and-removal):** hạn dùng mã mời
   cố định 7 ngày, không có ô nhập để chọn hạn; giáo viên tạo mã mới được bất cứ lúc nào
   [SoT: .nexa/control/decision-registry.md:3734-3735]. Chưa dựng vào prototype — để lúc build FE thật.
3. **Danh sách thẻ lớp** — mỗi thẻ hiển thị tên lớp, lịch học, số học viên, thanh tiến độ hoàn thành trung
   bình, điểm trung bình, số vắng bài (~~số cần chấm~~ đã bỏ 2026-10-13); dữ liệu mẫu 3 lớp cố định trong code
   (`classDefs`) [SoT: 09-layoutBase/Giáo viên - Lớp của tôi.dc.html:138-155, 260-264].
   **Chốt 2026-10-14 (DEC-2026-1014-instructor-batch2-weekly-rules):** thẻ "Vắng bài" có dòng phụ "Chưa nộp 7
   ngày qua"; "vắng bài" là học viên không có lượt nộp nào trong cửa sổ trượt 7 ngày gần nhất (không theo tuần
   lịch) [SoT: .nexa/control/decision-registry.md:3709]. **Không có hành vi
   bấm vào thẻ để xem chi tiết lớp** — không có `onClick`, không có link — xem Câu hỏi mở Q3.
   **Cập nhật 2026-10-11 (`DEC-2026-1011-class-detail-page`):** bấm tên lớp hoặc icon "Xem chi tiết" mở
   trang chi tiết lớp `INS0205` (`01-rd/screens/teacher/INS0205_class_detail.md`); lịch học là dữ liệu có
   cấu trúc (`DEC-2026-1011-class-schedule-structured`).
   **Chốt 2026-10-14 (DEC-2026-1014-instructor-batch3-4-contracts-and-removal):** bấm lại vào thẻ lớp đang
   được chọn thì bỏ chọn và quay về tab "Tất cả" [SoT: .nexa/control/decision-registry.md:3741-3742].
4. **Bảng "Danh sách học viên"** — tab lọc theo lớp (Tất cả + tên từng lớp), bảng cột: học viên, lớp, điểm
   trung bình, hoàn thành, trạng thái (Đang tốt/Cần hỗ trợ/Vắng bài — suy ra từ điểm/tỉ lệ hoàn thành trong
   dữ liệu mẫu; ngưỡng phân loại cụ thể để BD/DD tự đề xuất, chốt 2026-08-28 qua Q4), hoạt động gần nhất
   [SoT: 09-layoutBase/Giáo viên - Lớp của tôi.dc.html:160-186, 274-284]. **Chốt 2026-08-28 (F1-26, F1-27,
   Q4):** giáo viên gỡ được học viên khỏi lớp ngay tại bảng này (F1-26). **Chốt 2026-10-14
   (DEC-2026-1014-instructor-batch3-4-contracts-and-removal):** nút "Gỡ khỏi lớp" nằm cuối mỗi dòng học viên,
   luôn hiện rõ (dùng được cả trên màn cảm ứng), và luôn có bước xác nhận trước khi gỡ
   [SoT: .nexa/control/decision-registry.md:3736-3737]. **Chốt 2026-10-14 (`DEC-2026-1014-instructor-batch5-6-nav-pagination-rubric`):**
   bảng này gộp học viên của nhiều lớp nên **phân trang 20 dòng mỗi trang**; cột "Xu hướng" theo quy tắc tuần ISO của
   `F1-28`; khi bấm sang màn khoan sâu (`INS0204`, `INS0205`), sidebar sáng mục của màn này
   [SoT: .nexa/control/decision-registry.md:3761-3768]. Bấm vào một học viên đưa sang màn
   hồ sơ chi tiết riêng — slug mới `class_student_detail` (F1-27), chưa có prototype, chưa dựng vào bảng
   này.
5. **Không có UI cho F6-11 — và sẽ không bao giờ có:** tính năng "bộ câu hỏi phỏng vấn riêng theo lớp" đã
   loại khỏi phạm vi 2026-08-28 (`DEC-2026-0828-remove-per-class-interview-set`). Học viên dùng chung ngân
   hàng câu hỏi phỏng vấn hệ thống, không cần gì thêm ở màn này.
6. **Không có UI cho chấm lại** — và sẽ không bao giờ có: tính năng chấm lại đã loại khỏi phạm vi 2026-08-28
   (`DEC-2026-0828-remove-rejudge-scope`). Không cần dựng gì thêm ở đây.

---

## 3. Ngoài phạm vi (Out of Scope)

- Layout, spacing, bảng màu, component cụ thể (glass-morphism, breakpoint responsive) — thuộc BD
  (`02-bd/screens/teacher/INS0201_class_management.md`, chưa viết).
- Hợp đồng API (danh sách lớp, danh sách học viên theo lớp) — thuộc DD (`03-dd/api/identity.md`, chưa
  viết).
- Màn "Bài tập của tôi" (giao/gán bài từ ngân hàng cho lớp) — tách thành slug riêng
  `01-rd/screens/teacher/INS0202_class_assignments.md` (`DEC-2026-0828-split-class-management-assignments`).
- Màn "Tiến độ học viên" (`Giáo viên - Tiến độ học viên.dc.html`) — là slug màn khác theo mục 7.2 của
  `system_survey.md`, không thuộc phạm vi file này dù cùng actor A2. (Màn "Chấm bài", `F5-27`, `US-A2-06` đã bỏ
  2026-10-13, `DEC-2026-1013-remove-instructor-manual-grading`.)
- Thuật toán/ngưỡng cụ thể phân loại trạng thái học viên (Đang tốt/Cần hỗ trợ/Vắng bài) — chủ dự án chốt để
  BD/DD tự đề xuất (2026-08-28), không phải yêu cầu chức năng cứng, không thuộc file theo trục màn này.
- Luồng CRUD lớp học và hồ sơ học viên: **tạo lớp, xoá lớp, mã mời có hạn dùng + tạo lại được nhiều mã mới,
  gỡ học viên, xem hồ sơ chi tiết học viên đều đã có mã yêu cầu** (F1-23 tới F1-27, 2026-08-28).
- Màn hồ sơ chi tiết học viên (`class_student_detail`, F1-27) — slug riêng, RD chi tiết ở
  `01-rd/screens/teacher/INS0204_class_student_detail.md` (viết 2026-09-01), không thuộc phạm vi file này.

---

## 4. Tiền đề và ràng buộc

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | ~~`system_survey.md` gộp một slug `class_management` cho hai file prototype có route/nav riêng biệt...~~ **ĐÃ CHỐT (2026-08-28):** tách thành hai slug — `class_management` (file này, "Lớp của tôi") và `class_assignments` (`01-rd/screens/teacher/INS0202_class_assignments.md`, "Bài tập của tôi") — theo đúng đề xuất ban đầu, vì hai route Next.js khác nhau. Ghi thành `DEC-2026-0828-split-class-management-assignments`. | — | Đã chốt, đã tách file. | (đã đóng) |
| Q2 | ~~Nút "+ Tạo lớp mới" ở "Lớp của tôi" không có hành vi nào được lập trình trong prototype... Luồng tạo lớp chưa có mã `Fx-nn` nào.~~ **ĐÃ CHỐT MỘT PHẦN (2026-08-28, qua hỏi trực tiếp chủ dự án):** giáo viên (A2) tạo lớp mới với thông tin cơ bản; hệ thống sinh một **mã mời (invite code)** riêng cho lớp; học viên (A1) **tự tham gia lớp bằng cách nhập mã mời** — không phải A2/A3 thêm thủ công. Ghi thành `F1-23` (`01-rd/req/identity.md`), Given-When-Then ở `US-A2-07` (`01-rd/req/user_stories/a2_instructor.md`) _(sửa 2026-09-09: hai neo trước trỏ vào `req/req.md` và `req/user_stories.md`, nay là file chỉ mục rỗng nội dung sau `DEC-2026-0831-rd-req-split-by-module`)_. **Chưa dựng vào prototype** — để lúc build FE. Phần **CRUD lớp còn lại chưa chốt** — xem Q2b. | — | Đã chốt cơ chế tạo lớp + tham gia bằng mã mời. Phần còn lại xem Q2b. | Q2 đóng một phần, phát sinh Q2b |
| Q2b | ~~Sau khi chốt Q2 (tạo lớp + mã mời), vẫn còn: giáo viên có sửa/xoá lớp sau khi tạo không? Mã mời có thu hồi/tạo lại được không? Giáo viên có gỡ một học viên cụ thể khỏi lớp không, và gỡ rồi thì lịch sử bài nộp của học viên đó có giữ nguyên không?~~ **ĐÃ CHỐT (2026-08-28, qua hỏi trực tiếp chủ dự án):** (a) xoá lớp là **xoá thật** (hard delete), khác cách F1-16 xử lý xoá tài khoản (khoá mềm) — chủ đích, không phải thiếu nhất quán; (b) mã mời **có thời hạn dùng**; (c) giáo viên **gỡ được học viên** khỏi lớp, và gỡ thì ~~**xoá luôn lịch sử làm bài** của học viên đó trong lớp — không giữ lại ("đã gỡ rồi thì giữ cũng không làm gì", theo lời chủ dự án)~~ **ĐÃ ĐỔI 2026-10-14 (DEC-2026-1014-keep-learning-history-on-leave):** giữ lịch sử làm bài, chỉ đánh dấu đã rời lớp. Ghi thành `F1-24`, `F1-25`, `F1-26`; GWT bổ sung vào `US-A2-07`. **Chưa dựng vào prototype** — để lúc build FE. Phát sinh Q2c (nhỏ, không chặn BD). | — | Đã chốt cả ba điểm. | (đã đóng), phát sinh Q2c |
| Q2c | ~~F1-25 chỉ chốt việc mã mời có thời hạn dùng, chưa chốt: sau khi mã hết hạn, giáo viên có tạo lại/gia hạn mã mời cho cùng lớp đó không?~~ **ĐÃ CHỐT (2026-08-28, qua hỏi trực tiếp chủ dự án):** giáo viên có quyền **tạo nhiều mã mời mới** cho cùng lớp, mỗi mã mới có **thời hạn dùng riêng**, độc lập với các mã cũ. Ghi bổ sung vào `F1-25`, GWT thêm vào `US-A2-07`. | — | Đã chốt. | (đã đóng) |
| Q3 | ~~Thẻ lớp trong "Lớp của tôi" không có hành vi bấm để xem chi tiết lớp. Bảng học viên bên dưới đã lọc được theo lớp nhưng chưa rõ đây có phải là toàn bộ "chi tiết lớp" cần có hay còn thiếu.~~ ~~**ĐÃ CHỐT (2026-08-28, qua hỏi trực tiếp chủ dự án):** không cần trang con `/class/:id` riêng — tổng quan + bảng học viên lọc theo tab **đã đủ**. Đây chỉ là hành vi bấm-để-lọc chưa được nối trong prototype (`onClick` thiếu), **không phải khoảng trống nghiệp vụ**, không cần mã `Fx-nn` mới.~~ **ĐẢO NGƯỢC 2026-10-11 (chủ dự án chỉ đạo, `DEC-2026-1011-class-detail-page`):** **có** trang chi tiết lớp riêng — mã màn `INS0205`, slug `class_detail`, route prototype `/instructor/classes/[classId]`. Lý do chủ dự án nêu: "3 nút với popup không cung cấp đủ thông tin". Mô tả hành vi ở `01-rd/screens/teacher/INS0205_class_detail.md`. Không cần mã `Fx-nn` mới (gộp vào F1-23 tới F1-27). Ở danh sách "Lớp của tôi" chỉ còn icon Xem chi tiết, Sửa, Xoá; popup Mã mời và icon "Xem học viên" bỏ; tạo lớp xong thì chuyển thẳng sang trang chi tiết. | — | Đã chốt lại: thêm màn con `INS0205`; popup Tạo/Sửa lớp giữ nguyên, dùng chung. Trang học viên toàn bộ `/instructor/students` vẫn còn (lọc theo lớp). | (đã đóng) |
| Q4 | ~~Bảng "Danh sách học viên" không có nút thêm/xoá học viên khỏi lớp, không có hồ sơ chi tiết từng học viên (link sang màn nào?). Ngưỡng phân loại trạng thái "Đang tốt/Cần hỗ trợ/Vắng bài" không có công thức trong `req.md`.~~ **ĐÃ CHỐT (2026-08-28, qua hỏi trực tiếp chủ dự án):** (a) hồ sơ chi tiết học viên **cần một màn/route riêng** (không phải mở rộng ngay tại bảng) — ghi `F1-27`, GWT ở `US-A2-08`, thêm slug mới `class_student_detail` (chưa có prototype); (b) công thức phân loại trạng thái **để BD/DD tự đề xuất**, chủ dự án không chốt ngưỡng cụ thể ngay bây giờ `[SoT: Suy luận]`. (Gỡ học viên đã có mã `F1-26` qua Q2b.) | — | Đã chốt cả hai phần. | (đã đóng) |
| Q5 | ~~Màn "Lớp của tôi" không có bất kỳ UI nào cho F6-11 (tạo bộ câu hỏi phỏng vấn riêng và gán cho lớp)... Hoặc (a) dựng tab riêng trong `class_management`, hoặc (b) tách slug riêng?~~ **ĐÃ CHỐT (2026-08-28, qua hỏi trực tiếp chủ dự án):** bỏ hẳn khái niệm "bộ câu hỏi phỏng vấn riêng theo lớp" — học viên dùng chung một ngân hàng câu hỏi phỏng vấn duy nhất ở cấp hệ thống (`interview_bank_list`). Không cần tab, không cần slug riêng. `F6-11`, `US-A2-04` đã loại khỏi phạm vi — xem `DEC-2026-0828-remove-per-class-interview-set`. | — | Đã chốt: không cần UI cho tính năng này ở màn `class_management`. | (đã đóng) |
| Q6 | ~~Không có UI nào cho yêu cầu chấm lại theo lớp (F4-09a/b/c) trong cả hai prototype của slug này.~~ **ĐÃ CHỐT (2026-08-28, qua hỏi trực tiếp chủ dự án):** bỏ hẳn tính năng chấm lại — nếu testcase từng sai thì học viên báo cho giảng viên, giảng viên tự sửa testcase (không cần chấm lại hàng loạt lượt nộp cũ). _(Sửa 2026-09-09: căn cứ gốc ghi thêm "go-judge đã fail-fast trong một lần chấm" — `F4-04` hết hiệu lực từ 2026-08-31, `DEC-2026-0831-partial-score-testcase-ratio`, nên mệnh đề đó không còn đúng ở thể hiện tại. Kết luận bỏ chấm lại **không đổi**: nó dựa vào việc sửa testcase thủ công là đủ, không dựa vào fail-fast.)_ `F4-09a` tới `F4-09e`, màn `admin_rejudge`, và mọi UI liên quan đã loại khỏi phạm vi — xem `DEC-2026-0828-remove-rejudge-scope`. | — | Đã chốt: không cần UI, không cần mã yêu cầu nào cho chấm lại. | (đã đóng) |
| 201-Q1 | Thẻ "Hoàn thành TB" có giữ chênh lệch "+5% so với tuần trước" không. **ĐÃ CHỐT 2026-10-14:** bỏ chênh lệch, chỉ giữ giá trị hiện tại (tiêu chí ponytail bậc 2: chỉ có bộ đếm tích luỹ, không có ảnh chụp theo tuần). | — | `DEC-2026-1014-instructor-batch1-counting-rules`. RD file này không có câu hỏi mang số "201-Q1" trước đó; hàng này ghi nhận câu hỏi từ BD `02-bd/screens/teacher/INS0201_class_management.md`. | Đã đóng |
| 201-Q8 | Dòng phụ của thẻ "Vắng bài" ghi gì, cửa sổ tính thế nào. **ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-instructor-batch2-weekly-rules):** dòng phụ "Chưa nộp 7 ngày qua", giữ cửa sổ trượt 7 ngày. | — | Số thứ tự là của BD `02-bd/screens/teacher/INS0201_class_management.md`; RD file này không có câu hỏi mang số đó trước đây. | Đã đóng |
| 201-Q5, 201-Q6, 201-Q10 | Hạn dùng mã mời; vị trí và độ hiện của nút "Gỡ khỏi lớp"; bấm lại thẻ lớp đang chọn. **ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-instructor-batch3-4-contracts-and-removal):** mã mời cố định 7 ngày, không ô nhập; "Gỡ khỏi lớp" cuối dòng, luôn hiện, có xác nhận; bấm lại thẻ đang chọn thì bỏ chọn, về "Tất cả". | — | Số thứ tự là của BD `02-bd/screens/teacher/INS0201_class_management.md`; RD file này không có câu hỏi mang các số đó trước đây. | Đã đóng |

---

## 5. Danh sách yêu cầu (REQ)

| ID | Yêu cầu | Loại | Nguồn/SoT |
|---|---|---|---|
| REQ-01 | Tạo lớp học mới với thông tin cơ bản; sinh mã mời; học viên tự tham gia lớp bằng mã mời (F1-23) | Chức năng | `01-rd/req/identity.md` — F1-23 (bổ sung 2026-08-28) |
| REQ-02 | Xoá lớp học (xoá thật, hard delete) (F1-24) | Chức năng | `01-rd/req/identity.md` — F1-24 (bổ sung 2026-08-28) |
| REQ-03 | Mã mời có thời hạn dùng (F1-25) | Chức năng | `01-rd/req/identity.md` — F1-25 (bổ sung 2026-08-28) |
| REQ-04 | ~~Gỡ học viên khỏi lớp — xoá luôn lịch sử làm bài của học viên đó trong lớp~~ **ĐÃ ĐỔI 2026-10-14 (DEC-2026-1014-keep-learning-history-on-leave):** gỡ học viên khỏi lớp — giữ lịch sử làm bài, chỉ đánh dấu đã rời lớp; học viên đã rời không tính vào tổng số học viên và tiến độ lớp (F1-26) | Chức năng | `01-rd/req/identity.md` — F1-26 (bổ sung 2026-08-28) |
| REQ-05 | Xem hồ sơ chi tiết một học viên trong lớp phụ trách (màn/route riêng, slug mới `class_student_detail`) (F1-27) | Chức năng | `01-rd/req/identity.md` — F1-27 (bổ sung 2026-08-28) |
| REQ-06 | Given-When-Then xem hồ sơ chi tiết học viên (—) | Chức năng | `01-rd/req/user_stories/a2_instructor.md` (`US-A2-08`) |
| REQ-07 | Ma trận phân quyền — Function `CLASS_MANAGEMENT` gác quyền A2 theo phạm vi lớp phụ trách (F1-10, F1-12) | Chức năng | `01-rd/req/identity.md` — F1-10, F1-12 |
| REQ-08 | Given-When-Then tạo lớp và tham gia bằng mã mời (—) | Chức năng | `01-rd/req/user_stories/a2_instructor.md` (`US-A2-07`) |

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
| Khảo sát hệ thống | `01-rd/overview/system_survey.md` — mục 7.2 dòng `class_management`. |
| Yêu cầu module | `01-rd/req/identity.md` — F1-10 tới F1-12; F1-23 tới F1-27 (CRUD lớp học + mã mời + hồ sơ học viên, bổ sung 2026-08-28). |
| User story | `01-rd/req/user_stories/a2_instructor.md` — `US-A2-07` (tạo lớp + mã mời); `US-A2-08` (hồ sơ chi tiết học viên). |
| Quyết định | `DEC-2026-0828-remove-per-class-interview-set` — F6-11, `US-A2-04` loại khỏi phạm vi. |
| Quyết định | `DEC-2026-0828-remove-rejudge-scope` — F4-09a→e, `admin_rejudge` loại khỏi phạm vi. |
| Màn hình liên quan | `01-rd/screens/teacher/INS0202_class_assignments.md` — slug chị em, tách ra 2026-08-28, mô tả "Bài tập của tôi". |
| Prototype | `09-layoutBase/Giáo viên - Lớp của tôi.dc.html` — prototype. |
| Quyết định | `DEC-2026-0828-split-class-management-assignments`. |
| Quyết định | `DEC-2026-1011-class-detail-page` — thêm màn con `INS0205`, đảo Q3. |
| Quyết định | `DEC-2026-1011-class-schedule-structured` — lịch học có cấu trúc. |
| Màn hình liên quan | `01-rd/screens/teacher/INS0205_class_detail.md` — màn con "Chi tiết lớp", thêm 2026-10-11. |
| Tài liệu gốc | `README.md` mục 3 và mục 4 — định nghĩa Bounded Context và actor. |
