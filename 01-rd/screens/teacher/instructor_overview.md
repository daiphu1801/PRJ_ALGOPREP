# RD — Màn `instructor_overview` (Tổng quan khu Giảng viên)

> Slug: `instructor_overview` — khớp `01-rd/overview/system_survey.md` mục 7.2 dòng `instructor_overview`
> [SoT: 01-rd/overview/system_survey.md — mục 7.2 dòng `instructor_overview`]. Bounded Context: `identity` (chính) — màn tổng hợp số liệu đọc
> thêm từ `problem-bank`, `judge-orchestration` và `ai-review` để dựng các thẻ thống kê, nhưng bản thân màn
> không sở hữu logic nghiệp vụ của các module đó [SoT: 01-rd/overview/system_survey.md — mục 7.2 dòng `instructor_overview`]. Actor: A2
> (Giảng viên) — màn có shell/route riêng biệt khỏi khu Admin theo quyết định bố cục ở
> `01-rd/overview/system_survey.md` — mục 7.2, khối "Chốt 2026-08-24 ... Phương án B".
>
> Đối chiếu prototype: `09-layoutBase/Giáo viên - Tổng quan.dc.html`. Đây là slug **mới phát sinh khi dựng
> prototype thật**, không nằm trong 4 slug hạt giống ban đầu của khu Giảng viên (`problem_authoring`,
> `testcase_management`, `class_management`, `class_progress`) — lý do đã ghi rõ tại nguồn: "mỗi khu vực có
> shell riêng thường cần một dashboard riêng" [SoT: 01-rd/overview/system_survey.md — mục 7.2 dòng `instructor_overview`].
>
> File này mô tả hành vi và UX ở mức yêu cầu — không lặp lại đặc tả chức năng đã có ở
> `01-rd/req/identity.md`/`01-rd/req/user_stories/a2_instructor.md`, chỉ trỏ tới và bổ sung phần đặc thù của một màn.

## 1. Mục đích màn hình

Là trang đích mặc định khi `INSTRUCTOR` đăng nhập [SoT: 01-rd/screens/shared/auth.md:90 — Q3 đã chốt: vai
trò `INSTRUCTOR` điều hướng tới `instructor_overview` sau khi đăng nhập], cho giảng viên một cái nhìn tổng
hợp về khối lượng công việc và tình hình lớp mình phụ trách mà không phải mở lần lượt từng màn con: số lớp,
số học viên, số bài cần chấm tay, điểm trung bình lớp, danh sách bài nộp cần chấm gấp, hoạt động gần đây,
tóm tắt các lớp và bài tập do mình phụ trách. Đây thuần là màn tổng hợp/điều hướng — hành vi nghiệp vụ chi
tiết (soạn bài, giao bài, chấm bài...) thuộc các màn con mà mỗi thẻ/widget trỏ tới.

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Giao bài theo lớp phụ trách, phạm vi quản lý giới hạn theo lớp | F2-12 | `01-rd/req/problem-bank.md` — F2-12 |
| Điểm AI tham khảo + chấm tay theo lớp, gác bởi `CLASS_MANAGEMENT` | F5-27 | `01-rd/req/ai-review.md` — F5-27 |
| Ma trận phân quyền, Function `CLASS_MANAGEMENT` gác quyền A2 theo lớp phụ trách | F1-10 tới F1-12 | `01-rd/req/identity.md` — F1-10 tới F1-12 |
| Given-When-Then cho các hành vi thuộc màn con (soạn bài, testcase, giao bài, chấm bài...) | US-A2-01 tới US-A2-06 | `01-rd/req/user_stories/a2_instructor.md` — US-A2-01 tới US-A2-06 |
| Điều hướng theo vai trò sau đăng nhập — `INSTRUCTOR` → `instructor_overview` | (quyết định RD, chưa có mã `Fx-nn` riêng) | `01-rd/screens/shared/auth.md:90` (Q3, đã chốt) |

**Cập nhật 2026-08-28:** F6-11 (bộ câu hỏi phỏng vấn riêng theo lớp) đã loại khỏi phạm vi —
`DEC-2026-0828-remove-per-class-interview-set`. Bỏ khỏi tổng hợp F2-12/F5-27/F6-11 ở dashboard nếu có.
| Bảng slug khu Giảng viên, đối chiếu prototype `Giáo viên - Tổng quan.dc.html` | — | `01-rd/overview/system_survey.md` — mục 7.2, bảng slug khu giảng viên |

**Ghi chú traceability:** không có `US-A2-nn` nào mô tả riêng hành vi của chính màn tổng quan (các thẻ thống
kê, widget "Hoạt động gần đây", biểu đồ tiến độ) — `US-A2-01` tới `US-A2-06` đều mô tả các màn con
[SoT: 01-rd/req/user_stories/a2_instructor.md, đối chiếu toàn bộ mục "A2 — Giảng viên"]. Đây là khoảng trống
traceability giống mô hình các màn dashboard khác (ví dụ `my_progress` của học viên) — xem Câu hỏi mở Q1.

## 3. Trạng thái và cấu trúc màn (đối chiếu prototype)

Đối chiếu `09-layoutBase/Giáo viên - Tổng quan.dc.html` — đây là hành vi UX thật đã dựng, không phải suy
diễn trừ khi ghi rõ `[SoT: Suy luận]`:

1. **Shell riêng khu Giảng viên** — sidebar cố định bên trái với logo AlgoPrep + nhãn "GIÁO VIÊN"
   (dòng 64-68), có nút thu gọn/mở rộng (dòng 71-74, `state.collapsed`), khác hẳn shell khu Admin — khớp
   quyết định bố cục ở `01-rd/overview/system_survey.md` — mục 7.2, khối "Chốt 2026-08-24 ... Phương án B".
2. **Điều hướng chính (nav) gồm 5 mục**, mục `overview` đang active mặc định trên chính màn này
   (dòng 271-291, `navDefs`): Tổng quan (không badge), Lớp của tôi (badge "3"), Bài tập của tôi
   (badge "18"), Chấm bài (badge "9"), Tiến độ học viên (không badge). Badge là số đếm tĩnh trong dữ liệu
   giả lập, chưa rõ nguồn tính thời gian thực — xem Câu hỏi mở Q2.
3. **Thanh công cụ đầu trang** (dòng 115-125): ô tìm kiếm "Tìm lớp, học viên, bài tập…" (dòng 116-119,
   chưa có hành vi thật — `fakeAuth`/tìm kiếm không được lập trình trong prototype), nút CTA "+ Tạo lớp mới"
   (dòng 121, chưa gắn `href`/handler — trỏ về đâu chưa rõ, khả năng cao thuộc màn `class_management` theo
   F2-12), avatar giảng viên (dòng 122-124).
4. **4 thẻ thống kê** (dòng 127-138, dữ liệu mẫu dòng 305-310): "Lớp phụ trách" (3), "Tổng học viên" (82,
   delta +4), "Cần chấm tay" (9, meta "3 chờ quá 24 giờ"), "Điểm TB lớp" (7.4, delta +0.3). Không có mã
   `Fx-nn` nào định nghĩa công thức tính các con số này — xem Câu hỏi mở Q3.
5. **Widget "Cần chấm tay"** (dòng 140-158): danh sách bài nộp AI chấm điểm thấp (dòng 146, khớp `US-A2-06`),
   liên kết "Xem tất cả" trỏ sang `Giáo viên - Chấm bài.dc.html` (dòng 144) — tức màn `instructor_grading`
   (F5-27). **Cập nhật 2026-08-30:** prototype còn ghi "hoặc học viên yêu cầu review" — cụm này bỏ, tính năng
   học viên chủ động yêu cầu review đã loại khỏi phạm vi (`DEC-2026-0830-remove-student-review-request`, xem
   `01-rd/screens/teacher/instructor_grading.md` Q1). Nguồn của widget giờ chỉ còn một: điểm AI thấp.
6. **Widget "Hoạt động gần đây"** (dòng 160-174, dữ liệu mẫu dòng 319-325): feed 24 giờ gần nhất, các mục
   mẫu gồm bài nộp mới, cảnh báo lớp chưa nộp bài tuần, ~~yêu cầu review điểm~~, xuất bản bài mới, học viên
   đạt chuỗi ngày luyện tập. **Cập nhật 2026-08-30:** mục mẫu "yêu cầu review điểm" bỏ theo
   `DEC-2026-0830-remove-student-review-request`. Đây là tổng hợp sự kiện từ nhiều nguồn
   (`judge-orchestration`, `ai-review`, `problem-bank`) nhưng **không có mã `Fx-nn` nào mô tả cơ chế tổng hợp
   activity feed này** — xem Câu hỏi mở Q4.
7. **Widget "Lớp của tôi"** (dòng 177-196, dữ liệu mẫu dòng 327-331): danh sách lớp phụ trách kèm sĩ số và
   thanh tiến độ hoàn thành trung bình; liên kết "Quản lý lớp" trỏ sang `Giáo viên - Lớp của tôi.dc.html`
   (dòng 180) — tức màn `class_management`.
8. **Widget "Tiến độ học viên"** (dòng 198-210, dữ liệu mẫu dòng 333-334): biểu đồ đường điểm trung bình 4
   tuần gần nhất, không có liên kết đi tiếp sang màn `class_progress` — chỉ là biểu đồ tóm tắt tĩnh, khác
   với widget "Lớp của tôi" và "Cần chấm tay" đều có liên kết "Xem tất cả"/"Quản lý lớp". Không nhất quán
   điều hướng — xem Câu hỏi mở Q5.
9. **Widget "Bài tập của tôi"** (dòng 212-228, dữ liệu mẫu dòng 336-341): danh sách bài toán do giảng viên
   phụ trách kèm số lượt học viên đã dùng; liên kết "Xem tất cả" trỏ sang `Giáo viên - Bài tập của tôi.dc.html`
   (dòng 215) — theo bảng slug, tệp này cùng thuộc `class_management`
   [SoT: 01-rd/overview/system_survey.md — mục 7.2 dòng `instructor_overview`].
10. **Theme** — chỉ có công tắc Sáng/Tối (dòng 96-100, 296-303), lưu `localStorage` khoá riêng
    `algoprep-teacher-theme` (dòng 242-248) khác khoá của khu học viên. **Không thấy scaffold ngôn ngữ
    (`data-ui-lang`/`data-lang`)** như các màn học viên và màn `auth` đã có — xem Câu hỏi mở Q6.
11. **Không có trạng thái rỗng/lỗi/loading** trong prototype — mọi dữ liệu là mảng tĩnh trả về ngay trong
    `renderVals()` (dòng 264-349), không có state cho "chưa có lớp nào", "API lỗi", hay "đang tải" — xem Câu
    hỏi mở Q7.

## 4. Given-When-Then bổ sung ở mức màn

Vì **chưa có `US-A2-nn` nào phủ chính màn tổng quan** (xem mục 2), toàn bộ mục này là suy luận ở mức UX dựa
trên hành vi đã dựng trong prototype, không phải trích dẫn từ user story đã chốt — đánh dấu
`[SoT: Suy luận]` từng dòng theo đúng nguồn đối chiếu:

- **Cho** tôi là `INSTRUCTOR` vừa đăng nhập, **Khi** hệ thống điều hướng, **Thì** tôi được đưa thẳng tới
  `instructor_overview` thay vì `my_progress` [SoT: 01-rd/screens/shared/auth.md:90 — Q3 đã chốt, không phải
  suy luận].
- **Cho** tôi đang ở `instructor_overview`, **Khi** tôi bấm "Xem tất cả" ở widget "Cần chấm tay", **Thì**
  tôi được điều hướng sang màn `instructor_grading`, giữ nguyên phạm vi lớp mình phụ trách (không phải chọn
  lại) [SoT: Suy luận — suy từ liên kết tĩnh dòng 144 của prototype, chưa có mô tả hành vi truyền tham số
  lọc lớp giữa hai màn].
- **Cho** tôi đang ở `instructor_overview`, **Khi** tôi bấm "Quản lý lớp" ở widget "Lớp của tôi" hoặc "Xem
  tất cả" ở widget "Bài tập của tôi", **Thì** tôi được điều hướng sang màn `class_management`
  [SoT: 09-layoutBase/Giáo viên - Tổng quan.dc.html:180, 215; 01-rd/overview/system_survey.md — mục 7.2 dòng `instructor_overview`].
- **Cho** tôi phụ trách nhiều hơn một lớp, **Khi** các thẻ thống kê và widget hiển thị số liệu, **Thì** số
  liệu là tổng hợp trên **tất cả** các lớp tôi phụ trách, không phải của một lớp cụ thể — khớp cơ chế phạm vi
  "theo lớp giảng viên phụ trách" đã chốt ở F5-27/F2-12 nhưng áp dụng ở mức tổng hợp nhiều lớp
  [SoT: Suy luận — 01-rd/req/ai-review.md (F5-27) chỉ nói phạm vi hiển thị theo lớp phụ trách ở màn `instructor_grading`,
  chưa nói rõ cách tổng hợp khi có nhiều lớp ở màn tổng quan].

## 5. Câu hỏi mở (chưa trả lời — không tự chọn thay)

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | ~~`instructor_overview` chưa có `US-A2-nn` riêng...~~ **ĐÃ CHỐT 2026-08-31 (theo đề xuất, xem cột Chủ sở hữu):** thêm `US-A2-11: Xem tổng quan khối lượng công việc`. | — | Đã ghi vào `01-rd/req/user_stories/a2_instructor.md`. Xem `DEC-2026-0831-instructor-overview-dashboard`. | Đã đóng |
| Q2 | ~~Badge số trên nav... có cần realtime...~~ **ĐÃ CHỐT 2026-08-31 (theo đề xuất):** làm mới khi chuyển màn/tải lại trang, không cần WebSocket riêng. | — | Đã ghi vào `01-rd/req/identity.md` (amendment F1-30). Xem `DEC-2026-0831-instructor-overview-dashboard`. | Đã đóng |
| Q3 | ~~Công thức tính 4 thẻ thống kê...~~ **ĐÃ CHỐT 2026-08-31 (theo đề xuất):** "Điểm TB lớp" = điểm AI tham khảo (F5-27). | — | Đã ghi vào `01-rd/req/identity.md` (amendment F1-30). Xem `DEC-2026-0831-instructor-overview-dashboard`. | Đã đóng |
| Q4 | ~~Cơ chế tổng hợp "Hoạt động gần đây"...~~ **ĐÃ CHỐT 2026-08-31 (theo đề xuất):** read model riêng của `identity`, không tái dùng F1-14. | — | Đã ghi vào `01-rd/req/identity.md` (amendment F1-30). Xem `DEC-2026-0831-instructor-overview-dashboard`. | Đã đóng |
| Q5 | ~~Widget "Tiến độ học viên"... không có liên kết "Xem tất cả"...~~ **ĐÃ CHỐT 2026-08-31 (theo đề xuất):** bổ sung liên kết sang `class_progress`. | — | Đã ghi vào `01-rd/req/identity.md` (amendment F1-30). Xem `DEC-2026-0831-instructor-overview-dashboard`. | Đã đóng |
| Q6 | ~~Không thấy scaffold `data-ui-lang`/`data-lang`...~~ **ĐÃ CHỐT 2026-08-31:** cùng câu hỏi với Q7 của `admin_overview.md`, trả lời chung — mở rộng phạm vi debt của `DEC-2026-0824-i18n-vi-en` thành "mọi màn Admin và Giảng viên". | — | Xem `DEC-2026-0831-i18n-scope-expansion`. | Đã đóng |
| Q7 | ~~Màn chưa có trạng thái rỗng/lỗi/đang tải...~~ **ĐÃ CHỐT 2026-08-31 (theo đề xuất):** 3 trạng thái `empty`/`loading`/`error`, theo từng khối riêng. | — | Đã ghi vào `01-rd/req/identity.md` (amendment F1-30), cùng nguyên tắc đã áp cho `admin_overview`/F1-29. Xem `DEC-2026-0831-instructor-overview-dashboard`. | Đã đóng |

## 6. Ngoài phạm vi file này

- Layout, vùng bố cục, bảng màu, component cụ thể của từng widget — thuộc BD (`02-bd/screens/teacher/instructor_overview.md`, chưa viết).
- Hợp đồng API tổng hợp số liệu (endpoint trả về 4 thẻ thống kê, activity feed, danh sách lớp/bài tập rút gọn) — thuộc DD (`03-dd/api/identity.md` và các module liên quan, chưa viết).
- Logic nghiệp vụ chi tiết của từng hành vi liên kết tới (soạn bài — F2-01 tới F2-04, giao bài — F2-12, chấm bài — F5-27) — thuộc RD/BD/DD của các màn con tương ứng (`class_management`, `class_progress`, `instructor_grading`, `problem_authoring`), không lặp lại ở đây.
- Cơ chế ma trận phân quyền `CLASS_MANAGEMENT` (F1-10 tới F1-12) — thuộc BD/DD của module `identity`.

## 7. Tham chiếu

- `01-rd/overview/system_survey.md` — mục 7.2 dòng `instructor_overview`.
- `01-rd/req/identity.md` — F1-10 tới F1-12 (ma trận phân quyền, Function `CLASS_MANAGEMENT`).
- `01-rd/req/problem-bank.md` — F2-12 (giao bài theo lớp).
- `01-rd/req/ai-review.md` — F5-27 (điểm AI tham khảo + chấm tay theo lớp).
- Quyết định: `DEC-2026-0828-remove-per-class-interview-set` (F6-11 loại khỏi phạm vi).
- `01-rd/req/user_stories/a2_instructor.md` — `US-A2-01` tới `US-A2-06`.
- `01-rd/screens/shared/auth.md:90` — Q3 đã chốt, điều hướng theo vai trò sau đăng nhập.
- `01-rd/screens/users/problem_list.md` — tham khảo văn phong và cách trích dẫn.
- `09-layoutBase/Giáo viên - Tổng quan.dc.html` — prototype đối chiếu chính của file này.
- `DEC-2026-0824-i18n-vi-en` — quyết định song ngữ VI/EN.
- `DEC-2026-0824-dark-light-theme` — quyết định theme Sáng/Tối.
