# RD — Màn `instructor_grading` (Điểm AI tham khảo và chấm tay theo lớp)

> Slug: `instructor_grading` — khớp `01-rd/overview/system_survey.md` mục 7.2, dòng `instructor_grading`
> [SoT: 01-rd/overview/system_survey.md — mục 7.2 dòng `instructor_grading`]. Bounded Context: `ai-review` + `problem-bank`
> [SoT: 01-rd/overview/system_survey.md — mục 7.2 dòng `instructor_grading`]. Actor: A2 (Giảng viên)
> [SoT: 01-rd/req/ai-review.md — F5-27, "Actor A2, gác bởi Function `CLASS_MANAGEMENT`..."].
>
> Slug phát sinh khi dựng prototype thật, không nằm trong 4 slug hạt giống ban đầu của khu Giảng viên
> [SoT: 01-rd/overview/system_survey.md — mục 7.2, ghi chú slug mới phát sinh khi dựng prototype].
>
> Đối chiếu prototype: `09-layoutBase/Giáo viên - Chấm bài.dc.html` (đã dựng thật, không phải hạt giống suy
> luận) [SoT: 01-rd/overview/system_survey.md — mục 7.2 dòng `instructor_grading`].
>
> File này mô tả **hành vi và UX ở mức yêu cầu** của một màn cụ thể — không lặp lại đặc tả chức năng đã có
> ở `01-rd/req/ai-review.md` (mục F5-27) hay `01-rd/req/user_stories/a2_instructor.md` (`US-A2-06`), chỉ trỏ tới và bổ sung phần
> đặc thù của **một màn**: trạng thái màn, luồng chuyển màn, đối chiếu prototype, và các câu hỏi mở phát
> sinh khi đối chiếu với prototype thật mà bản mô tả chức năng chung chưa có.

## 1. Mục đích màn hình

Cho giảng viên (A2) một nơi lướt nhanh chất lượng bài làm của học viên trong (các) lớp mình phụ trách,
dựa trên một **điểm quy đổi tham khảo trên thang 10** tổng hợp từ báo cáo phân tích bài giải (F5.1/F5-07),
và cho phép giảng viên **chấm tay đè lên** kèm nhận xét khi cần — hoàn toàn tách biệt khỏi kết quả Pass/Fail
chính thức của bài nộp [SoT: 01-rd/req/ai-review.md — F5-27].

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Quy đổi báo cáo phân tích bài giải (F5.1) thành một điểm tham khảo thang 10 cho giảng viên | F5-27 | `01-rd/req/ai-review.md` — F5-27 |
| Điểm AI 0-10 và điểm chấm tay là lớp tham khảo nội bộ, tách bạch hoàn toàn khỏi Pass/Fail chính thức của bài nộp (và khỏi điểm tỷ lệ testcase F4-13 hiện cho học viên), không ghi đè/không đổi trạng thái submission | F5-27 | `01-rd/req/ai-review.md` — F5-27 |
| Không phải "chấm điểm từng phần theo trọng số" (đã loại) | F5-27 | `01-rd/req/ai-review.md` — F5-27 |
| Phạm vi hiển thị theo lớp giảng viên phụ trách, cùng cơ chế F2-12 | F5-27, F2-12 | `01-rd/req/ai-review.md` — F5-27, `01-rd/req/problem-bank.md` — F2-12 |
| Gác bởi Function `CLASS_MANAGEMENT` trong ma trận phân quyền | F1-12, F1-10 | `01-rd/req/identity.md` — F1-10, F1-12 |
| Nguồn của điểm quy đổi là báo cáo JSON có lược đồ của F5.1 | F5-07 | `01-rd/req/ai-review.md` — F5-07 |
| Riêng biệt với điểm dễ đọc 1-5 do AI tự chấm trong cùng báo cáo F5.1 (không phải cùng một trường) | F5-05 | `01-rd/req/ai-review.md` — F5-05 |
| Given-When-Then đầy đủ cho hành vi trên | — | `01-rd/req/user_stories/a2_instructor.md` (`US-A2-06`) |
| Khu vực Giảng viên có layout/route riêng biệt khỏi khu Admin | — | `01-rd/overview/system_survey.md` — mục 7.2, khối "Chốt 2026-08-24 ... Phương án B" |

## 3. Trạng thái và cấu trúc màn (đối chiếu prototype)

Đối chiếu `09-layoutBase/Giáo viên - Chấm bài.dc.html` — đây là hành vi UX thật đã dựng, trích dòng thật:

1. **Tiêu đề và mô tả màn** — "Chấm bài" / "Bài nộp AI chấm điểm thấp hoặc học viên yêu cầu review"
   [SoT: 09-layoutBase/Giáo viên - Chấm bài.dc.html:117-118]. **Cập nhật 2026-08-30:** nửa sau câu mô tả
   ("...hoặc học viên yêu cầu review") không còn đúng — tính năng học viên chủ động yêu cầu review đã loại
   khỏi phạm vi (`DEC-2026-0830-remove-student-review-request`, xem Q1). Câu mô tả UI thật cần sửa lại còn
   "Bài nộp AI chấm điểm thấp" khi dựng UI thật **[Đợi nextjs]**. Màn vẫn là một **tập con đã lọc sẵn** (bài
   điểm AI thấp), không phải toàn bộ bài nộp của lớp như cách đọc "lướt nhanh chất lượng bài làm... trong
   lớp mình phụ trách" ở F5-27 gợi ý — xem Câu hỏi mở Q2.
2. **4 thẻ thống kê** (`stats`): "Chờ chấm" (9, "3 chờ quá 24 giờ"), ~~"Yêu cầu review" (2, "Học viên chủ động
   gửi")~~, "Đã chấm tuần này" (31, +12 so với tuần trước), "Điểm TB sau chấm" (7.1, +0.4 "So với điểm AI")
   [SoT: 09-layoutBase/Giáo viên - Chấm bài.dc.html:122-133, 261-266]. **Cập nhật 2026-08-30:** thẻ "Yêu cầu
   review" loại bỏ cùng tính năng (Q1) — còn lại 3 thẻ. Không có mã `Fx-nn` nào mô tả các chỉ số tổng hợp
   còn lại — xem Câu hỏi mở Q4.
3. **Tab lọc trạng thái** (`statusTabs`): "Chờ chấm" / "Đã chấm" / "Tất cả" — trạng thái này là **trạng thái
   chấm của giảng viên** (đã lưu điểm chấm tay hay chưa), không phải trạng thái Pass/Fail của bài nộp
   [SoT: 09-layoutBase/Giáo viên - Chấm bài.dc.html:138-144, 268-280]. Xác nhận đúng tinh thần "tách bạch
   hoàn toàn khỏi Pass/Fail" của F5-27 [SoT: 01-rd/req/ai-review.md — F5-27] — không phát hiện vi phạm ở điểm này.
4. **Bảng danh sách bài chờ chấm** — cột Học viên, Bài tập, Lớp, Nộp lúc, Điểm AI, hành động
   [SoT: 09-layoutBase/Giáo viên - Chấm bài.dc.html:147-149]. Cột "Điểm AI" trong prototype nhận một trong
   hai dạng giá trị: một điểm số dạng `x/10` (ví dụ "4/10") hoặc chuỗi văn bản ~~"Yêu cầu review"~~ thay cho
   điểm số [SoT: 09-layoutBase/Giáo viên - Chấm bài.dc.html:157, 208-217, 284]. **ĐÃ CHỐT 2026-08-30 (owner
   instruction, xem Q1): loại bỏ hẳn cơ chế học viên chủ động yêu cầu review.** Cột "Điểm AI" từ nay luôn là
   một điểm số `x/10` — không còn giá trị dạng chuỗi "Yêu cầu review" nữa, vì mọi dòng trong bảng chỉ đến từ
   tiêu chí "điểm AI thấp" (Q2), không còn nguồn thứ hai từ phía học viên.
5. **Cột hành động "Chấm ngay"** — mở modal chấm điểm [SoT:
   09-layoutBase/Giáo viên - Chấm bài.dc.html:158-160, 171-186].
6. **Modal chấm điểm** — ô nhập "Điểm (0-10)" dạng text tự do (không có ràng buộc min/max/step hiển thị
   trong markup), ô "Nhận xét" dạng textarea tự do, nút "Huỷ"/"Lưu điểm"
   [SoT: 09-layoutBase/Giáo viên - Chấm bài.dc.html:176-183]. Sau khi lưu, điểm chấm tay thay thế **cách
   hiển thị** ở cột Điểm AI (hiện `<điểm chấm tay>/10`) và đổi trạng thái dòng sang "Đã chấm"
   [SoT: 09-layoutBase/Giáo viên - Chấm bài.dc.html:282-289, 304-308] — đúng tinh thần "điểm chấm tay lưu
   lại cạnh điểm AI" của `US-A2-06` [SoT: 01-rd/req/user_stories/a2_instructor.md — US-A2-06], nhưng prototype hiện tại **ghi
   đè cách hiển thị** thay vì hiện song song cả hai điểm — xem Câu hỏi mở Q3 (làm rõ có cần hiện lại điểm AI
   gốc sau khi đã chấm tay hay không, để giảng viên khác đối chiếu).
7. **Không có bộ lọc theo lớp cụ thể trên giao diện** (chỉ có cột "Lớp" hiển thị trong bảng, dữ liệu mẫu
   trộn nhiều lớp: "Thuật toán K21", "CTDL K22", "Luyện PV nâng cao")
   [SoT: 09-layoutBase/Giáo viên - Chấm bài.dc.html:208-217]. Phù hợp với việc một giảng viên phụ trách
   nhiều lớp cùng lúc, nhưng chưa thấy cơ chế lọc riêng theo từng lớp trên giao diện — không phải xung đột,
   chỉ là thiếu chi tiết UX, xem Câu hỏi mở Q5.
8. **Không phát hiện phần tử nào trên giao diện gợi ý điểm AI/điểm chấm tay ảnh hưởng tới trạng thái
   Pass/Fail chính thức của bài nộp** — không có cột/nhãn nào đổi verdict, không có liên kết ngược tới
   `submission_result`. Kết luận: **không có xung đột nghiêm trọng dạng vi phạm tách bạch** (khác với vi
   phạm F2-08 từng phát hiện ở `submission_result`, xem `01-rd/screens/users/submission_result.md:52-62`).

## 4. Given-When-Then bổ sung ở mức màn (không trùng `US-A2-06`)

`US-A2-06` đã có đủ Given-When-Then cho việc xem điểm tham khảo, chấm tay, và giới hạn phạm vi theo lớp
[SoT: 01-rd/req/user_stories/a2_instructor.md — US-A2-06]. Các mục dưới đây là hành vi **riêng của màn hình** phát hiện khi
đối chiếu prototype, mà mô tả chức năng ở mức module chưa nêu:

- **Cho** tôi mở màn `instructor_grading`, **Khi** danh sách tải xong, **Thì** tôi thấy tab "Chờ chấm" được
  chọn mặc định, chỉ liệt kê các bài nộp chưa có điểm chấm tay của tôi
  [SoT: 09-layoutBase/Giáo viên - Chấm bài.dc.html:192, 275-280].
- ~~**Cho** một dòng có cột Điểm AI hiện "Yêu cầu review" (không phải số)...~~ **Loại bỏ 2026-08-30** — không
  còn giá trị "Yêu cầu review" trong cột Điểm AI, xem Q1.
- **Cho** tôi đã lưu điểm chấm tay cho một bài nộp, **Khi** tôi chuyển sang tab "Đã chấm", **Thì** tôi thấy
  lại đúng bài đó với điểm đã lưu, không mất khi đổi tab hay đổi bộ lọc trạng thái
  [SoT: 09-layoutBase/Giáo viên - Chấm bài.dc.html:277-289, 304-308].
- **Cho** tôi không có quyền `CLASS_MANAGEMENT` trên một lớp cụ thể, **Khi** tôi mở màn `instructor_grading`,
  **Thì** bài nộp của lớp đó không xuất hiện trong danh sách, không phải bị ẩn bằng CSS trên client
  [SoT: 01-rd/req/ai-review.md — F5-27; 01-rd/req/identity.md — F1-12; suy diễn về nơi thực thi gác quyền phải ở tầng server, chưa
  có xác nhận riêng cho màn này — `[SoT: Suy luận]`].

## 5. Câu hỏi mở (chưa trả lời — không tự chọn thay)

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu / Ưu tiên |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | ~~Prototype có cơ chế học viên chủ động "yêu cầu review" từ giảng viên — không có mã `Fx-nn` nào mô tả, tính năng hai phía cần chốt.~~ **ĐÃ CHỐT 2026-08-30 (owner instruction): loại bỏ hoàn toàn khỏi phạm vi.** Học viên không có cách nào chủ động yêu cầu giảng viên review một bài nộp cụ thể. Màn `instructor_grading` chỉ còn một nguồn hàng đợi duy nhất — bài nộp có điểm AI thấp (ngưỡng chốt ở Q2) — không còn nguồn thứ hai từ phía học viên. Xoá khỏi UI khi dựng thật: cột Điểm AI không còn giá trị chuỗi "Yêu cầu review", thẻ thống kê "Yêu cầu review" bị gỡ (còn 3 thẻ), câu mô tả đầu trang sửa lại. Ghi quyết định `DEC-2026-0830-remove-student-review-request`. | — | Đã chốt — loại bỏ. Xem `DEC-2026-0830-remove-student-review-request`. | Đã đóng |
| Q2 | ~~Câu mô tả trên prototype...ngụ ý màn này là một hàng đợi đã lọc sẵn...~~ **ĐÃ CHỐT 2026-08-31 (theo đề xuất):** hàng đợi lọc sẵn theo ngưỡng cố định — bài `Accepted` có điểm AI dưới **6/10**. | — | Đã ghi vào `01-rd/req/ai-review.md` (amendment F5-27), `[SoT: Suy luận]`, BD/DD chỉnh được. Xem `DEC-2026-0831-instructor-grading-round2`. | Đã đóng |
| Q3 | ~~Sau khi giảng viên chấm tay, prototype ghi đè cách hiển thị cột Điểm AI...~~ **ĐÃ CHỐT 2026-08-31 (theo đề xuất):** hiện song song cả hai điểm ("AI: 4/10 · GV: 7/10"). | — | Đã ghi vào `01-rd/req/ai-review.md` (amendment F5-27). Xem `DEC-2026-0831-instructor-grading-round2`. | Đã đóng |
| Q4 | ~~Bốn chỉ số thống kê đầu trang...không có mã mô tả cách tính...~~ **ĐÃ CHỐT 2026-08-31 (theo đề xuất):** công thức chốt, "tuần này" = 7 ngày gần nhất (không phải tuần lịch), không cần mã mới. | — | Đã ghi vào `01-rd/req/ai-review.md` (amendment F5-27). Xem `DEC-2026-0831-instructor-grading-round2`. | Đã đóng |
| Q5 | ~~Giảng viên phụ trách nhiều lớp...giao diện không có bộ lọc riêng theo từng lớp...~~ **ĐÃ CHỐT 2026-08-31 (theo đề xuất):** bổ sung bộ lọc lớp, cùng kiểu `class_progress`. | — | Đã ghi vào `01-rd/req/ai-review.md` (amendment F5-27). Xem `DEC-2026-0831-instructor-grading-round2`. | Đã đóng |

## 6. Ngoài phạm vi file này

- Layout, vùng bố cục, bảng màu, component cụ thể — thuộc BD (`02-bd/screens/teacher/instructor_grading.md`,
  chưa viết).
- Hợp đồng API (request/response lấy danh sách bài chờ chấm, lưu điểm chấm tay) — thuộc DD
  (`03-dd/api/ai-review.md` và/hoặc `03-dd/api/problem-bank.md`, chưa viết).
- Thuật toán quy đổi báo cáo F5.1 (JSON) thành điểm tham khảo thang 10 — thuộc BD/DD của module `ai-review`,
  không thuộc file theo trục màn này; chỉ ghi nhận ở đây rằng công thức quy đổi cụ thể **chưa được mô tả ở
  `ai-review.md`** (F5-27 chỉ nói "hệ thống quy đổi thêm một điểm tham khảo trên thang 10" mà không nêu công
  thức) [SoT: 01-rd/req/ai-review.md — F5-27].
- Chi tiết ma trận phân quyền `CLASS_MANAGEMENT` (tạo/sửa/xoá Function, Action) — thuộc RD/BD của màn
  `admin_permission_matrix` (F1-10 tới F1-12), chỉ trỏ tới ở đây.

## 7. Tham chiếu

- `01-rd/req/ai-review.md` — F5-27, F5-01 tới F5-08 (nguồn báo cáo F5.1 dùng để quy đổi điểm).
- `01-rd/req/identity.md` — F1-10 tới F1-12, `CLASS_MANAGEMENT`.
- `01-rd/req/judge-orchestration.md` — F4-13 (điểm tỷ lệ testcase hiện cho học viên; `F4-04` fail-fast đã hết hiệu lực 2026-08-31, `DEC-2026-0831-partial-score-testcase-ratio`).
- `01-rd/req/problem-bank.md` — F2-12 (cơ chế phạm vi theo lớp, dùng chung).
- `01-rd/req/user_stories/a2_instructor.md` — `US-A2-06`.
- `01-rd/overview/system_survey.md` — mục 7.2 dòng `instructor_grading`.
- `09-layoutBase/Giáo viên - Chấm bài.dc.html` — prototype đã dựng thật.
- `01-rd/screens/users/submission_result.md:52-62` — mẫu cách ghi nhận và sửa một vi phạm tách bạch dữ liệu
  (F2-08), dùng để đối chiếu kết luận "không có xung đột nghiêm trọng" ở mục 3.8 của file này.
- `01-rd/screens/users/solution_review.md` — đã kiểm tra, không có mô tả cơ chế "yêu cầu review" (từng liên
  quan Câu hỏi mở Q1, nay đã đóng — tính năng loại bỏ khỏi phạm vi).
- Quyết định: `DEC-2026-0830-remove-student-review-request`.
