# RD (Yêu cầu hệ thống mới) — Tổng quan giảng viên / `INS0101`

> Mã màn hình: `INS0101` [Nguồn: 02-bd/_rules/bd-template-9sheet.md — mục 8].
> Slug chính tắc: `instructor_overview` [SoT: 01-rd/overview/system_survey.md — mục 7.2 dòng `instructor_overview`].
> Phạm vi/Bounded Context: `identity` (chính) — màn tổng hợp số liệu đọc thêm từ `problem-bank`,
> `judge-orchestration` và `ai-review` để dựng các thẻ thống kê, nhưng bản thân màn không sở hữu logic
> nghiệp vụ của các module đó [SoT: 01-rd/overview/system_survey.md — mục 7.2 dòng `instructor_overview`].
> Actor: A2 (Giảng viên).
> Nguồn sự thật (SoT): `01-rd/req/identity.md`, `01-rd/req/user_stories/a2_instructor.md`,
> `09-layoutBase/Giáo viên - Tổng quan.dc.html` (prototype), `01-rd/overview/system_survey.md` — mục 7.2.
> Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

Là trang đích mặc định khi `INSTRUCTOR` đăng nhập [SoT: 01-rd/screens/shared/SHR0101_auth.md:94 — Q3 đã chốt: vai
trò `INSTRUCTOR` điều hướng tới `instructor_overview` sau khi đăng nhập], cho giảng viên một cái nhìn tổng
hợp về khối lượng công việc và tình hình lớp mình phụ trách mà không phải mở lần lượt từng màn con: số lớp,
số học viên (~~điểm trung bình lớp~~ đã bỏ 2026-10-14), hoạt động gần đây,
tóm tắt các lớp và bài tập do mình phụ trách. Đây thuần là màn tổng hợp/điều hướng — hành vi nghiệp vụ chi
tiết (soạn bài, giao bài...) thuộc các màn con mà mỗi thẻ/widget trỏ tới.

Màn có shell/route riêng biệt khỏi khu Admin theo quyết định bố cục ở `01-rd/overview/system_survey.md` —
mục 7.2, khối "Chốt 2026-08-24 ... Phương án B". Đối chiếu prototype `09-layoutBase/Giáo viên - Tổng
quan.dc.html`. Đây là slug **mới phát sinh khi dựng prototype thật**, không nằm trong 4 slug hạt giống ban
đầu của khu Giảng viên (`problem_authoring`, `testcase_management`, `class_management`, `class_progress`)
— lý do đã ghi rõ tại nguồn: "mỗi khu vực có shell riêng thường cần một dashboard riêng"
[SoT: 01-rd/overview/system_survey.md — mục 7.2 dòng `instructor_overview`].

File này mô tả hành vi và UX ở mức yêu cầu — không lặp lại đặc tả chức năng đã có ở
`01-rd/req/identity.md`/`01-rd/req/user_stories/a2_instructor.md`, chỉ trỏ tới và bổ sung phần đặc thù của một màn.

---

## 2. Phạm vi

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Giao bài theo lớp phụ trách, phạm vi quản lý giới hạn theo lớp | F2-12 | `01-rd/req/problem-bank.md` — F2-12 |
| ~~Điểm AI tham khảo + chấm tay theo lớp~~ **ĐÃ BỎ 2026-10-13** (`DEC-2026-1013-remove-instructor-manual-grading`) | ~~F5-27~~ | `01-rd/req/ai-review.md` — F5-27 (mục đã bỏ) |
| Ma trận phân quyền, Function `CLASS_MANAGEMENT` gác quyền A2 theo lớp phụ trách | F1-10 tới F1-12 | `01-rd/req/identity.md` — F1-10 tới F1-12 |
| Given-When-Then cho các hành vi thuộc màn con (soạn bài, testcase, giao bài...) | US-A2-01 tới US-A2-05 (`US-A2-06` đã bỏ 2026-10-13) | `01-rd/req/user_stories/a2_instructor.md` — US-A2-01 tới US-A2-05 |
| Điều hướng theo vai trò sau đăng nhập — `INSTRUCTOR` → `instructor_overview` | (quyết định RD, chưa có mã `Fx-nn` riêng) | `01-rd/screens/shared/SHR0101_auth.md:94` (Q3, đã chốt) |
| Bảng slug khu Giảng viên, đối chiếu prototype `Giáo viên - Tổng quan.dc.html` | — | `01-rd/overview/system_survey.md` — mục 7.2, bảng slug khu giảng viên |

**Cập nhật 2026-08-28:** F6-11 (bộ câu hỏi phỏng vấn riêng theo lớp) đã loại khỏi phạm vi —
`DEC-2026-0828-remove-per-class-interview-set`. Bỏ khỏi tổng hợp F2-12/F5-27/F6-11 ở dashboard nếu có.

**Cập nhật 2026-10-13 (`DEC-2026-1013-remove-instructor-manual-grading`):** bỏ thẻ "Cần chấm tay", widget "Cần chấm tay" và badge "Chấm bài" trên nav; màn `INS0301` và `F5-27` không còn.

**Ghi chú traceability:** không có `US-A2-nn` nào mô tả riêng hành vi của chính màn tổng quan (các thẻ thống
kê, widget "Hoạt động gần đây", ~~biểu đồ tiến độ~~ đã bỏ 2026-10-14) — `US-A2-01` tới `US-A2-05` đều mô tả các màn con
[SoT: 01-rd/req/user_stories/a2_instructor.md, đối chiếu toàn bộ mục "A2 — Giảng viên"]. Đây là khoảng trống
traceability giống mô hình các màn dashboard khác (ví dụ `my_progress` của học viên) — xem Câu hỏi mở Q1.

### 2.1 Cấu trúc hệ thống liên quan

| Màn/tài liệu | Vai trò | Bounded Context |
|---|---|---|
| Màn hiện tại | `INS0101` / `instructor_overview` | `identity` |
| Tài liệu yêu cầu | `01-rd/req/identity.md` (F1-10 tới F1-12, F1-30) | Nguồn SoT của màn |
| Prototype | `09-layoutBase/Giáo viên - Tổng quan.dc.html` | Bằng chứng bố cục và trạng thái |

**Chi tiết trạng thái và cấu trúc màn**

Đối chiếu `09-layoutBase/Giáo viên - Tổng quan.dc.html` — đây là hành vi UX thật đã dựng, không phải suy
diễn trừ khi ghi rõ `[SoT: Suy luận]`:

1. **Shell riêng khu Giảng viên** — sidebar cố định bên trái với logo AlgoPrep + nhãn "GIÁO VIÊN"
   (dòng 64-68), có nút thu gọn/mở rộng (dòng 71-74, `state.collapsed`), khác hẳn shell khu Admin — khớp
   quyết định bố cục ở `01-rd/overview/system_survey.md` — mục 7.2, khối "Chốt 2026-08-24 ... Phương án B".
2. **Điều hướng chính (nav)** — prototype `.dc.html` vẽ 5 mục phẳng, mục `overview` active mặc định trên
   chính màn này (dòng 271-291, `navDefs`): Tổng quan (không badge), Lớp của tôi (badge "3"), Bài tập của
   tôi (badge "18"), ~~Chấm bài (badge "9")~~ **đã bỏ 2026-10-13**, Tiến độ học viên (không badge). **Đã lỗi thời so với bản dựng
   (chủ dự án, 2026-10-11):** nav nay là Tổng quan + nhóm gập Nội dung + nhóm Khác, **không còn badge**
   (`DEC-2026-1011-teacher-nav-badge-dropped`), nhãn "Bài tập của tôi" đổi thành "Bài giao cho lớp" và
   "Tiến độ học viên" thành "Danh sách học viên" [SoT: 02-bd/screens/teacher/_shell.md mục 3]. Câu hỏi mở Q2
   về badge thời gian thực không còn áp dụng.
3. ~~**Thanh công cụ đầu trang** (dòng 115-125): ô tìm kiếm, nút CTA "+ Tạo lớp mới", avatar giảng viên.~~
   **Đã bỏ khỏi yêu cầu của màn này (chủ dự án, 2026-10-11, `DEC-2026-1011-teacher-overview-toolbar-dropped`).**
   Màn tổng quan mở thẳng vào dải thẻ thống kê. Ô tìm kiếm không có hành vi ở mọi lần dựng (ponytail bậc 3),
   tạo lớp là chức năng của `class_management` (bậc 4), avatar nằm ở menu người dùng của thanh công cụ chung
   của khu [SoT: 02-bd/screens/teacher/_shell.md]. Các dòng prototype 115-125 giữ làm lịch sử bằng chứng.
4. **2 thẻ thống kê** (prototype dòng 127-138 vẽ 4 thẻ; dữ liệu mẫu dòng 305-310): "Lớp phụ trách" (3), "Tổng học viên" (82,
   ~~delta +4~~ **ĐÃ BỎ 2026-10-14 (DEC-2026-1014-instructor-batch1-counting-rules)**: bỏ `newStudentCount30d` và chênh lệch "học viên mới 30 ngày"; **Tổng học viên đếm theo người, tức `DISTINCT student_id` qua các lớp phụ trách, không đếm theo lượt ghi danh** — một học viên thuộc hai lớp của cùng giảng viên chỉ tính một lần), ~~"Điểm TB lớp" (7.4, delta +0.3)~~ **ĐÃ BỎ 2026-10-14 (`DEC-2026-1014-instructor-overview-trim-and-nav-label`)**. ~~Thẻ "Cần chấm tay" (9, meta "3 chờ quá 24 giờ")~~ **đã bỏ 2026-10-13** (`DEC-2026-1013-remove-instructor-manual-grading`). Không có mã
   `Fx-nn` nào định nghĩa công thức tính các con số này — xem Câu hỏi mở Q3.
5. ~~**Widget "Cần chấm tay"**~~ **Đã bỏ khỏi yêu cầu của màn này (chủ dự án, 2026-10-13, `DEC-2026-1013-remove-instructor-manual-grading`)**; số thứ tự 5 giữ chỗ để các tham chiếu cũ không lệch. Mô tả cũ (dòng prototype 140-158): danh sách bài nộp AI chấm điểm thấp (dòng 146, khớp `US-A2-06`),
   liên kết "Xem tất cả" trỏ sang `Giáo viên - Chấm bài.dc.html` (dòng 144) — tức màn `instructor_grading`
   (F5-27). **Cập nhật 2026-08-30:** prototype còn ghi "hoặc học viên yêu cầu review" — cụm này bỏ, tính năng
   học viên chủ động yêu cầu review đã loại khỏi phạm vi (`DEC-2026-0830-remove-student-review-request`, xem
   `01-rd/screens/teacher/INS0301_grading.md` Q1). Nguồn của widget giờ chỉ còn một: điểm AI thấp.
6. **Widget "Hoạt động gần đây"** (dòng 160-174, dữ liệu mẫu dòng 319-325): feed 24 giờ gần nhất, các mục
   mẫu gồm bài nộp mới, ~~cảnh báo lớp chưa nộp bài tuần~~, ~~yêu cầu review điểm~~, xuất bản bài mới, học viên
   đạt chuỗi ngày luyện tập. **Chốt 2026-10-14 (DEC-2026-1014-instructor-batch1-counting-rules):** feed chỉ giữ
   **sự kiện tức thời** — nộp bài (submit), xuất bản bài (publish), đạt chuỗi ngày luyện tập (streak), học viên
   vào lớp (join). ~~Loại sự kiện suy ra từ quét định kỳ ("vắng 9 ngày", "cần hỗ trợ", cảnh báo lớp chưa nộp bài)~~
   **ĐÃ BỎ 2026-10-14** và ~~cụm "kèm điểm AI" trong dòng bài nộp~~ **ĐÃ BỎ 2026-10-14** (điểm AI quy đổi đã bỏ,
   `DEC-2026-1014-drop-ai-score-10`). **Cập nhật 2026-08-30:** mục mẫu "yêu cầu review điểm" bỏ theo
   `DEC-2026-0830-remove-student-review-request`. Đây là tổng hợp sự kiện từ nhiều nguồn
   (`judge-orchestration`, `ai-review`, `problem-bank`) nhưng **không có mã `Fx-nn` nào mô tả cơ chế tổng hợp
   activity feed này** — xem Câu hỏi mở Q4.
7. **Widget "Lớp của tôi"** (dòng 177-196, dữ liệu mẫu dòng 327-331): danh sách lớp phụ trách kèm sĩ số và
   thanh tiến độ hoàn thành trung bình; liên kết "Quản lý lớp" trỏ sang `Giáo viên - Lớp của tôi.dc.html`
   (dòng 180) — tức màn `class_management`.
8. ~~**Widget "Tiến độ học viên"** (dòng 198-210, dữ liệu mẫu dòng 333-334): biểu đồ đường điểm trung bình 4
   tuần gần nhất, không có liên kết đi tiếp sang màn `class_progress` — chỉ là biểu đồ tóm tắt tĩnh, khác
   với widget "Lớp của tôi" có liên kết "Quản lý lớp" (widget "Cần chấm tay" từng có liên kết, nay đã bỏ). Không nhất quán
   điều hướng — xem Câu hỏi mở Q5.~~ **ĐÃ BỎ 2026-10-14 (`DEC-2026-1014-instructor-overview-trim-and-nav-label`)**, kèm liên kết "Xem tất cả" sang `class_progress`; số thứ tự 8 giữ chỗ để các tham chiếu cũ không lệch. `INS0203` và biểu đồ riêng của nó không đổi.
9. **Widget "Bài tập của tôi"** (dòng 212-228, dữ liệu mẫu dòng 336-341): danh sách bài toán do giảng viên
   phụ trách kèm số lượt học viên đã dùng — **chốt 2026-10-14 (DEC-2026-1014-instructor-batch1-counting-rules):
   số lượt nộp của một bài được tính trong phạm vi các lớp của chính giảng viên, không lấy bộ đếm toàn hệ thống**; **chốt 2026-10-14 (`DEC-2026-1014-instructor-batch5-6-nav-pagination-rubric`): khối này (nhãn nav nay là "Bài giao cho lớp") hiện 5 bài có nhiều lượt nộp nhất trong các lớp của giảng viên**; màn tổng quan giữ tĩnh, không thêm liên kết đi sâu mới; liên kết "Xem tất cả" trỏ sang `Giáo viên - Bài tập của tôi.dc.html`
   (dòng 215) — theo bảng slug, tệp này cùng thuộc `class_management`
   [SoT: 01-rd/overview/system_survey.md — mục 7.2 dòng `instructor_overview`].
10. **Theme** — chỉ có công tắc Sáng/Tối (dòng 96-100, 296-303), lưu `localStorage` khoá riêng
    `algoprep-teacher-theme` (dòng 242-248) khác khoá của khu học viên. **Không thấy scaffold ngôn ngữ
    (`data-ui-lang`/`data-lang`)** như các màn học viên và màn `auth` đã có — xem Câu hỏi mở Q6.
11. **Không có trạng thái rỗng/lỗi/loading** trong prototype — mọi dữ liệu là mảng tĩnh trả về ngay trong
    `renderVals()` (dòng 264-349), không có state cho "chưa có lớp nào", "API lỗi", hay "đang tải" — xem Câu
    hỏi mở Q7.

---

## 3. Ngoài phạm vi (Out of Scope)

- Layout, vùng bố cục, bảng màu, component cụ thể của từng widget — thuộc BD (`02-bd/screens/teacher/INS0101_instructor_overview.md`, chưa viết).
- Hợp đồng API tổng hợp số liệu (endpoint trả về 4 thẻ thống kê, activity feed, danh sách lớp/bài tập rút gọn) — thuộc DD (`03-dd/api/identity.md` và các module liên quan, chưa viết).
- Logic nghiệp vụ chi tiết của từng hành vi liên kết tới (soạn bài — F2-01 tới F2-04, giao bài — F2-12, ~~chấm bài — F5-27~~ đã bỏ 2026-10-13) — thuộc RD/BD/DD của các màn con tương ứng (`class_management`, `class_progress`, `problem_authoring`), không lặp lại ở đây.
- Cơ chế ma trận phân quyền `CLASS_MANAGEMENT` (F1-10 tới F1-12) — thuộc BD/DD của module `identity`.

---

## 4. Tiền đề và ràng buộc

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | ~~`instructor_overview` chưa có `US-A2-nn` riêng...~~ **ĐÃ CHỐT 2026-08-31 (theo đề xuất, xem cột Chủ sở hữu):** thêm `US-A2-11: Xem tổng quan khối lượng công việc`. | — | Đã ghi vào `01-rd/req/user_stories/a2_instructor.md`. Xem `DEC-2026-0831-instructor-overview-dashboard`. | Đã đóng |
| Q2 | ~~Badge số trên nav... có cần realtime...~~ **ĐÃ CHỐT 2026-08-31 (theo đề xuất):** làm mới khi chuyển màn/tải lại trang, không cần WebSocket riêng. | — | Đã ghi vào `01-rd/req/identity.md` (amendment F1-30). Xem `DEC-2026-0831-instructor-overview-dashboard`. | Đã đóng |
| Q3 | ~~Công thức tính 4 thẻ thống kê...~~ **ĐÃ CHỐT 2026-08-31 (theo đề xuất):** "Điểm TB lớp" = điểm AI quy đổi `ai_score_10`. **Lưu ý 2026-10-13:** F5-27 đã bỏ, thẻ "Điểm TB lớp" là chỗ duy nhất còn dùng `ai_score_10`; số phận thẻ này chờ OQ-F5-A (`01-rd/req/ai-review.md`). **Đã đóng: khối bị bỏ 2026-10-14** (thẻ "Điểm TB lớp" bỏ, `DEC-2026-1014-instructor-overview-trim-and-nav-label`; ~~OQ-F5-A vẫn mở ở `ai-review`~~ **ĐÃ BỎ 2026-10-14 (`DEC-2026-1014-drop-ai-score-10`):** cột `ai_score_10` bị bỏ, OQ-F5-A đã đóng). | — | Đã ghi vào `01-rd/req/identity.md` (amendment F1-30). Xem `DEC-2026-0831-instructor-overview-dashboard`. | Đã đóng |
| Q4 | ~~Cơ chế tổng hợp "Hoạt động gần đây"...~~ **ĐÃ CHỐT 2026-08-31 (theo đề xuất):** read model riêng của `identity`, không tái dùng F1-14. | — | Đã ghi vào `01-rd/req/identity.md` (amendment F1-30). Xem `DEC-2026-0831-instructor-overview-dashboard`. | Đã đóng |
| Q5 | ~~Widget "Tiến độ học viên"... không có liên kết "Xem tất cả"...~~ **ĐÃ CHỐT 2026-08-31 (theo đề xuất):** bổ sung liên kết sang `class_progress`. **Đã đóng: khối bị bỏ 2026-10-14** (widget "Tiến độ học viên" và liên kết bị bỏ, `DEC-2026-1014-instructor-overview-trim-and-nav-label`). | — | Đã ghi vào `01-rd/req/identity.md` (amendment F1-30). Xem `DEC-2026-0831-instructor-overview-dashboard`. | Đã đóng |
| Q6 | ~~Không thấy scaffold `data-ui-lang`/`data-lang`...~~ **ĐÃ CHỐT 2026-08-31:** cùng câu hỏi với Q7 của `admin_overview.md`, trả lời chung — mở rộng phạm vi debt của `DEC-2026-0824-i18n-vi-en` thành "mọi màn Admin và Giảng viên". | — | Xem `DEC-2026-0831-i18n-scope-expansion`. | Đã đóng |
| Q7 | ~~Màn chưa có trạng thái rỗng/lỗi/đang tải...~~ **ĐÃ CHỐT 2026-08-31 (theo đề xuất):** 3 trạng thái `empty`/`loading`/`error`, theo từng khối riêng. | — | Đã ghi vào `01-rd/req/identity.md` (amendment F1-30), cùng nguyên tắc đã áp cho `admin_overview`/F1-29. Xem `DEC-2026-0831-instructor-overview-dashboard`. | Đã đóng |
| Q8 | Đợt 1 câu hỏi mở giảng viên (2026-10-14): (a) "Tổng học viên" đếm theo người hay theo lượt ghi danh; (b) lượt nộp của bài giao tính theo lớp hay toàn hệ thống; (c) giữ "học viên mới 30 ngày" và delta không; (d) loại sự kiện của "Hoạt động gần đây". **ĐÃ CHỐT 2026-10-14:** (a) theo người (`DISTINCT student_id`); (b) theo các lớp của giảng viên; (c) bỏ cả số và delta, bỏ endpoint `GetInstructorOverviewStats`, màn đọc danh sách tóm tắt lớp; (d) chỉ sự kiện tức thời (submit, publish, streak, join), bỏ loại suy ra từ quét định kỳ và cụm "kèm điểm AI". | — | `DEC-2026-1014-instructor-batch1-counting-rules`. Ghi chú: RD file này chỉ có Q1-Q7; các số "Q5, Q9, Q7, Q15a, Q15c" trong quyết định là số câu hỏi mở của BD `02-bd/screens/teacher/INS0101_overview.md`, RD không có câu hỏi trùng số, nên gộp vào Q8 này. | Đã đóng |
| 101-Q12 | Ba lời gọi dữ liệu của màn tổng quan do module nào sở hữu. **ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-instructor-batch3-4-contracts-and-removal):** module sở hữu dữ liệu giữ endpoint của mình: danh sách tóm tắt lớp và hoạt động gần đây thuộc `identity`, bài tập đã giao thuộc `problem-bank`; không gộp hết vào `identity` [SoT: .nexa/control/decision-registry.md:3730-3732]. | — | Số thứ tự là của BD `02-bd/screens/teacher/INS0101_overview.md`; RD file này không có câu hỏi mang số đó. RD không nêu tên endpoint, chi tiết hợp đồng thuộc DD. | Đã đóng |

---

## 5. Danh sách yêu cầu (REQ)

| ID | Yêu cầu | Loại | Nguồn/SoT |
|---|---|---|---|
| REQ-01 | Giao bài theo lớp phụ trách, phạm vi quản lý giới hạn theo lớp (F2-12) | Chức năng | `01-rd/req/problem-bank.md` — F2-12 |
| REQ-02 | ~~Điểm AI tham khảo + chấm tay theo lớp (F5-27)~~ **Đã bỏ 2026-10-13** (`DEC-2026-1013-remove-instructor-manual-grading`) | Chức năng | `01-rd/req/ai-review.md` — F5-27 (mục đã bỏ) |
| REQ-03 | Ma trận phân quyền, Function `CLASS_MANAGEMENT` gác quyền A2 theo lớp phụ trách (F1-10 tới F1-12) | Chức năng | `01-rd/req/identity.md` — F1-10 tới F1-12 |
| REQ-04 | Given-When-Then cho các hành vi thuộc màn con (soạn bài, testcase, giao bài...) (US-A2-01 tới US-A2-05) | Chức năng | `01-rd/req/user_stories/a2_instructor.md` — US-A2-01 tới US-A2-05 |
| REQ-05 | Điều hướng theo vai trò sau đăng nhập — `INSTRUCTOR` → `instructor_overview` ((quyết định RD, chưa có mã `Fx-nn` riêng)) | Chức năng | `01-rd/screens/shared/SHR0101_auth.md:94` (Q3, đã chốt) |
| REQ-06 | Bảng slug khu Giảng viên, đối chiếu prototype `Giáo viên - Tổng quan.dc.html` (—) | Chức năng | `01-rd/overview/system_survey.md` — mục 7.2, bảng slug khu giảng viên |

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
| Khảo sát hệ thống | `01-rd/overview/system_survey.md` — mục 7.2 dòng `instructor_overview`. |
| Yêu cầu module | `01-rd/req/identity.md` — F1-10 tới F1-12 (ma trận phân quyền, Function `CLASS_MANAGEMENT`), F1-30 (amendment cho overview). |
| Yêu cầu module | `01-rd/req/problem-bank.md` — F2-12 (giao bài theo lớp). |
| Yêu cầu module | `01-rd/req/ai-review.md` — F5-27 (đã bỏ 2026-10-13; xem mục đó; OQ-F5-A đã đóng 2026-10-14, `DEC-2026-1014-drop-ai-score-10`). |
| Quyết định | `DEC-2026-1013-remove-instructor-manual-grading` — bỏ chấm tay của giảng viên. |
| Quyết định | `DEC-2026-1014-instructor-overview-trim-and-nav-label` — bỏ thẻ "Điểm TB lớp" và widget "Tiến độ học viên"; nhóm nav "Giao và chấm bài" đổi thành "Quản lý bài tập đã nộp" (bản RD này chỉ nêu tên nhóm nav ở đây, không có mục nào khác dùng nhãn cũ). |
| Quyết định | `DEC-2026-0828-remove-per-class-interview-set` (F6-11 loại khỏi phạm vi). |
| User story | `01-rd/req/user_stories/a2_instructor.md` — `US-A2-01` tới `US-A2-05`, `US-A2-11` (`US-A2-06` đã bỏ). |
| Màn hình liên quan | `01-rd/screens/shared/SHR0101_auth.md:94` — Q3 đã chốt, điều hướng theo vai trò sau đăng nhập. |
| Tài liệu tham khảo | `01-rd/screens/users/USR0101_problem_list.md` — tham khảo văn phong và cách trích dẫn. |
| Prototype | `09-layoutBase/Giáo viên - Tổng quan.dc.html` — prototype đối chiếu chính của file này. |
| Quyết định | `DEC-2026-0824-i18n-vi-en` — quyết định song ngữ VI/EN. |
| Quyết định | `DEC-2026-0824-dark-light-theme` — quyết định theme Sáng/Tối. |
| Quyết định | `DEC-2026-0831-instructor-overview-dashboard` — quyết định chốt dashboard overview giảng viên. |
| Quyết định | `DEC-2026-0831-i18n-scope-expansion` — mở rộng phạm vi i18n cho giảng viên và admin. |
