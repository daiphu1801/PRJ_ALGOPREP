# RD — Màn `instructor_overview` (Tổng quan khu Giảng viên)

> Slug: `instructor_overview` — khớp `01-rd/overview/system_survey.md` mục 7.2 dòng `instructor_overview`
> [SoT: 01-rd/overview/system_survey.md:525]. Bounded Context: `identity` (chính) — màn tổng hợp số liệu đọc
> thêm từ `problem-bank`, `judge-orchestration` và `ai-review` để dựng các thẻ thống kê, nhưng bản thân màn
> không sở hữu logic nghiệp vụ của các module đó [SoT: 01-rd/overview/system_survey.md:525]. Actor: A2
> (Giảng viên) — màn có shell/route riêng biệt khỏi khu Admin theo quyết định bố cục ở
> `01-rd/overview/system_survey.md:508-512`.
>
> Đối chiếu prototype: `09-layoutBase/Giáo viên - Tổng quan.dc.html`. Đây là slug **mới phát sinh khi dựng
> prototype thật**, không nằm trong 4 slug hạt giống ban đầu của khu Giảng viên (`problem_authoring`,
> `testcase_management`, `class_management`, `class_progress`) — lý do đã ghi rõ tại nguồn: "mỗi khu vực có
> shell riêng thường cần một dashboard riêng" [SoT: 01-rd/overview/system_survey.md:525].
>
> File này mô tả hành vi và UX ở mức yêu cầu — không lặp lại đặc tả chức năng đã có ở
> `01-rd/req/req.md`/`01-rd/req/user_stories.md`, chỉ trỏ tới và bổ sung phần đặc thù của một màn.

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
| Giao bài theo lớp phụ trách, phạm vi quản lý giới hạn theo lớp | F2-12 | `01-rd/req/req.md:160-166` |
| Điểm AI tham khảo + chấm tay theo lớp, gác bởi `CLASS_MANAGEMENT` | F5-27 | `01-rd/req/req.md:391-402` |
| Tạo bộ câu hỏi phỏng vấn riêng và gán cho lớp | F6-11 | `01-rd/req/req.md:417` |
| Ma trận phân quyền, Function `CLASS_MANAGEMENT` gác quyền A2 theo lớp phụ trách | F1-10 tới F1-12 | `01-rd/req/req.md:55-68` |
| Given-When-Then cho các hành vi thuộc màn con (soạn bài, testcase, giao bài, chấm bài...) | US-A2-01 tới US-A2-06 | `01-rd/req/user_stories.md:203-278` |
| Điều hướng theo vai trò sau đăng nhập — `INSTRUCTOR` → `instructor_overview` | (quyết định RD, chưa có mã `Fx-nn` riêng) | `01-rd/screens/shared/auth.md:90` (Q3, đã chốt) |
| Bảng slug khu Giảng viên, đối chiếu prototype `Giáo viên - Tổng quan.dc.html` | — | `01-rd/overview/system_survey.md:519-526` |

**Ghi chú traceability:** không có `US-A2-nn` nào mô tả riêng hành vi của chính màn tổng quan (các thẻ thống
kê, widget "Hoạt động gần đây", biểu đồ tiến độ) — `US-A2-01` tới `US-A2-06` đều mô tả các màn con
[SoT: 01-rd/req/user_stories.md:203-278, đối chiếu toàn bộ mục "2. A2 — Giảng viên"]. Đây là khoảng trống
traceability giống mô hình các màn dashboard khác (ví dụ `my_progress` của học viên) — xem Câu hỏi mở Q1.

## 3. Trạng thái và cấu trúc màn (đối chiếu prototype)

Đối chiếu `09-layoutBase/Giáo viên - Tổng quan.dc.html` — đây là hành vi UX thật đã dựng, không phải suy
diễn trừ khi ghi rõ `[SoT: Suy luận]`:

1. **Shell riêng khu Giảng viên** — sidebar cố định bên trái với logo AlgoPrep + nhãn "GIÁO VIÊN"
   (dòng 64-68), có nút thu gọn/mở rộng (dòng 71-74, `state.collapsed`), khác hẳn shell khu Admin — khớp
   quyết định bố cục ở `system_survey.md:508-512`.
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
5. **Widget "Cần chấm tay"** (dòng 140-158): danh sách bài nộp AI chấm điểm thấp hoặc học viên yêu cầu
   review (dòng 146, khớp `US-A2-06`), liên kết "Xem tất cả" trỏ sang `Giáo viên - Chấm bài.dc.html`
   (dòng 144) — tức màn `instructor_grading` (F5-27).
6. **Widget "Hoạt động gần đây"** (dòng 160-174, dữ liệu mẫu dòng 319-325): feed 24 giờ gần nhất, các mục
   mẫu gồm bài nộp mới, cảnh báo lớp chưa nộp bài tuần, yêu cầu review điểm, xuất bản bài mới, học viên đạt
   chuỗi ngày luyện tập. Đây là tổng hợp sự kiện từ nhiều nguồn (`judge-orchestration`, `ai-review`,
   `problem-bank`) nhưng **không có mã `Fx-nn` nào mô tả cơ chế tổng hợp activity feed này** — xem Câu hỏi
   mở Q4.
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
   [SoT: 01-rd/overview/system_survey.md:523].
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
  [SoT: 09-layoutBase/Giáo viên - Tổng quan.dc.html:180, 215; 01-rd/overview/system_survey.md:523].
- **Cho** tôi phụ trách nhiều hơn một lớp, **Khi** các thẻ thống kê và widget hiển thị số liệu, **Thì** số
  liệu là tổng hợp trên **tất cả** các lớp tôi phụ trách, không phải của một lớp cụ thể — khớp cơ chế phạm vi
  "theo lớp giảng viên phụ trách" đã chốt ở F5-27/F2-12 nhưng áp dụng ở mức tổng hợp nhiều lớp
  [SoT: Suy luận — 01-rd/req/req.md:401-402 chỉ nói phạm vi hiển thị theo lớp phụ trách ở màn `instructor_grading`,
  chưa nói rõ cách tổng hợp khi có nhiều lớp ở màn tổng quan].

## 5. Câu hỏi mở (chưa trả lời — không tự chọn thay)

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | `instructor_overview` chưa có `US-A2-nn` riêng mô tả Given-When-Then cho chính màn (thẻ thống kê, activity feed, biểu đồ tiến độ) — có nên bổ sung một `US-A2-07` mới, hay giữ nguyên coi đây là màn điều hướng thuần tuý không cần user story riêng (giống cách `US-A1-nn` không có story riêng cho `my_progress` dashboard, nếu đúng vậy)? | File RD này không được sửa `user_stories.md`; cần chủ dự án hoặc phiên đang giữ `user_stories.md` xác nhận có bổ sung story mới hay không. | Đề xuất thêm `US-A2-07: Xem tổng quan khối lượng công việc` mô tả 3-4 Given-When-Then cho các thẻ/widget chính, tách khỏi các story theo từng chức năng con đã có. | Chủ dự án |
| Q2 | Badge số trên nav ("3" lớp, "18" bài tập, "9" cần chấm) có cần realtime (cập nhật khi có bài nộp mới cần chấm) hay chỉ làm mới khi tải lại trang? | Prototype chỉ có dữ liệu tĩnh (`navDefs` dòng 271-277), không có cơ chế cập nhật; F4 có WebSocket cho trạng thái từng bài nộp (F4-08) nhưng chưa rõ có áp dụng cho badge tổng hợp cấp màn dashboard hay không. | Đề xuất mức tối thiểu: làm mới khi chuyển màn/tải lại trang (không cần WebSocket riêng cho badge); nếu chủ dự án muốn realtime thì cần ghi rõ thành yêu cầu và có thể chạm tới BD/DD của `judge-orchestration`. | Chủ dự án |
| Q3 | Công thức tính 4 thẻ thống kê chưa có mã `Fx-nn`: "Tổng học viên" tính join hay distinct theo lớp? "Điểm TB lớp" là điểm bài nộp (F4) hay điểm AI tham khảo (F5-27) hay cả hai gộp? Khung thời gian so sánh delta ("+4", "+0.3") là gì (tuần/tháng)? | Prototype chỉ hiển thị số mẫu tĩnh (dòng 305-310), không mô tả nguồn tính. | Đề xuất "Điểm TB lớp" lấy theo điểm AI tham khảo (F5-27) vì đây là điểm 0-10 khả dụng sẵn cho mọi bài Accepted, không phụ thuộc giảng viên đã chấm tay hay chưa; nhưng cần chủ dự án xác nhận trước khi đưa vào BD. | Chủ dự án |
| Q4 | Cơ chế tổng hợp "Hoạt động gần đây" — đây là bảng sự kiện tổng hợp (aggregated activity log) đọc từ nhiều Bounded Context, hay là dữ liệu suy ra tại tầng đọc (read model) riêng của `instructor_overview`? Có giới hạn số lượng/thời gian giữ log không? | Chưa có mã `Fx-nn` nào mô tả activity feed cho giảng viên (khác với "Nhật ký hệ thống" F1-14 vốn dành cho actor A3 và ghi hành động quản trị, không phải hoạt động học viên). | Đề xuất coi đây là read model riêng của module `identity` (hoặc một module tổng hợp mới), không tái dùng bảng audit log F1-14 (khác mục đích, khác actor xem). Cần chủ dự án xác nhận có cần một mã `Fx-nn` mới hay gộp vào diễn giải hiện có của F2-12/F5-27. | Chủ dự án |
| Q5 | Widget "Tiến độ học viên" (biểu đồ 4 tuần) không có liên kết "Xem tất cả"/"Quản lý" sang `class_progress` như hai widget còn lại — có phải thiếu sót cần bổ sung liên kết, hay đây là biểu đồ tóm tắt cố ý không cho đi tiếp? | Không nhất quán so với hai widget khác trong cùng màn (dòng 144, 180 có liên kết; dòng 198-210 không có); prototype không giải thích. | Đề xuất bổ sung liên kết trỏ sang `class_progress` cho nhất quán UX, trừ khi chủ dự án có lý do giữ nguyên (ví dụ: biểu đồ ở đây chỉ là tổng hợp toàn bộ lớp, còn `class_progress` xem theo từng lớp/học viên nên không phải "xem thêm" của cùng một tập dữ liệu). | Chủ dự án |
| Q6 | Không thấy scaffold `data-ui-lang`/`data-lang` trong `Giáo viên - Tổng quan.dc.html`, khác với hầu hết màn học viên và màn `auth` đã có. `DEC-2026-0824-i18n-vi-en` chỉ nêu tên 4 màn Admin còn thiếu scaffold, không nhắc tới màn khu Giảng viên — đây có phải một khoảng trống chưa được liệt kê trong quyết định đó không? | Không thể tự sửa `.nexa/control/decision-registry.md` (nằm ngoài phạm vi file này); cần xác nhận phạm vi thật của quyết định có bao trùm cả khu Giảng viên hay chỉ 4 màn Admin đã nêu tên. | Đề xuất mở rộng phạm vi debt của `DEC-2026-0824-i18n-vi-en` để bao gồm toàn bộ khu Giảng viên (ít nhất `instructor_overview`), tránh phát sinh thêm câu hỏi tương tự khi viết RD cho `instructor_grading`, `class_management`, `class_progress`. | Chủ dự án |
| Q7 | Màn chưa có trạng thái rỗng (giảng viên mới chưa có lớp nào), trạng thái lỗi tải dữ liệu, hay trạng thái đang tải — có cần bổ sung các trạng thái này trước khi viết BD/DD, theo đúng mẫu đã chốt cho màn `auth` (Q1 của `auth.md`, đã đóng bằng cách thêm trạng thái lỗi inline)? | Prototype dùng dữ liệu tĩnh, không mô phỏng các trạng thái này (dòng 264-349, không có nhánh rỗng/lỗi). | Đề xuất bổ sung tối thiểu 3 trạng thái: `empty` (chưa phụ trách lớp nào — hiện với giảng viên mới tạo tài khoản), `loading` (khi tải số liệu tổng hợp), `error` (khi một hoặc nhiều nguồn dữ liệu — `problem-bank`/`judge-orchestration`/`ai-review` — không phản hồi được, cần làm rõ màn hiển thị phần còn lại hay báo lỗi toàn màn). | Chủ dự án |

## 6. Ngoài phạm vi file này

- Layout, vùng bố cục, bảng màu, component cụ thể của từng widget — thuộc BD (`02-bd/screens/teacher/instructor_overview.md`, chưa viết).
- Hợp đồng API tổng hợp số liệu (endpoint trả về 4 thẻ thống kê, activity feed, danh sách lớp/bài tập rút gọn) — thuộc DD (`03-dd/api/identity.md` và các module liên quan, chưa viết).
- Logic nghiệp vụ chi tiết của từng hành vi liên kết tới (soạn bài — F2-01 tới F2-04, giao bài — F2-12, chấm bài — F5-27, câu hỏi phỏng vấn theo lớp — F6-11) — thuộc RD/BD/DD của các màn con tương ứng (`class_management`, `class_progress`, `instructor_grading`, `problem_authoring`, `testcase_management`), không lặp lại ở đây.
- Cơ chế ma trận phân quyền `CLASS_MANAGEMENT` (F1-10 tới F1-12) — thuộc BD/DD của module `identity`.

## 7. Tham chiếu

- `01-rd/overview/system_survey.md:508-526` — mục 7.2 Khu vực giảng viên, bảng slug và dòng `instructor_overview`.
- `01-rd/req/req.md:55-68` — F1-10 tới F1-12 (ma trận phân quyền, Function `CLASS_MANAGEMENT`).
- `01-rd/req/req.md:160-166` — F2-12 (giao bài theo lớp).
- `01-rd/req/req.md:391-402` — F5-27 (điểm AI tham khảo + chấm tay theo lớp).
- `01-rd/req/req.md:417` — F6-11 (bộ câu hỏi phỏng vấn riêng cho lớp).
- `01-rd/req/user_stories.md:203-278` — `US-A2-01` tới `US-A2-06`.
- `01-rd/screens/shared/auth.md:90` — Q3 đã chốt, điều hướng theo vai trò sau đăng nhập.
- `01-rd/screens/users/problem_list.md` — tham khảo văn phong và cách trích dẫn.
- `09-layoutBase/Giáo viên - Tổng quan.dc.html` — prototype đối chiếu chính của file này.
- `.nexa/control/decision-registry.md:303-325` — `DEC-2026-0824-i18n-vi-en`.
- `.nexa/control/decision-registry.md:377` — `DEC-2026-0824-dark-light-theme`.
