# RD — Màn `instructor_grading` (Điểm AI tham khảo và chấm tay theo lớp)

> Slug: `instructor_grading` — khớp `01-rd/overview/system_survey.md` mục 7.2, dòng `instructor_grading`
> [SoT: 01-rd/overview/system_survey.md:526]. Bounded Context: `ai-review` + `problem-bank`
> [SoT: 01-rd/overview/system_survey.md:526]. Actor: A2 (Giảng viên)
> [SoT: 01-rd/req/req.md:401 — "Actor A2, gác bởi Function `CLASS_MANAGEMENT`..."].
>
> Slug phát sinh khi dựng prototype thật, không nằm trong 4 slug hạt giống ban đầu của khu Giảng viên
> [SoT: 01-rd/overview/system_survey.md:514-516].
>
> Đối chiếu prototype: `09-layoutBase/Giáo viên - Chấm bài.dc.html` (đã dựng thật, không phải hạt giống suy
> luận) [SoT: 01-rd/overview/system_survey.md:526].
>
> File này mô tả **hành vi và UX ở mức yêu cầu** của một màn cụ thể — không lặp lại đặc tả chức năng đã có
> ở `01-rd/req/req.md` (mục F5-27) hay `01-rd/req/user_stories.md` (`US-A2-06`), chỉ trỏ tới và bổ sung phần
> đặc thù của **một màn**: trạng thái màn, luồng chuyển màn, đối chiếu prototype, và các câu hỏi mở phát
> sinh khi đối chiếu với prototype thật mà bản mô tả chức năng chung chưa có.

## 1. Mục đích màn hình

Cho giảng viên (A2) một nơi lướt nhanh chất lượng bài làm của học viên trong (các) lớp mình phụ trách,
dựa trên một **điểm quy đổi tham khảo trên thang 10** tổng hợp từ báo cáo phân tích bài giải (F5.1/F5-07),
và cho phép giảng viên **chấm tay đè lên** kèm nhận xét khi cần — hoàn toàn tách biệt khỏi kết quả Pass/Fail
chính thức của bài nộp [SoT: 01-rd/req/req.md:391-402 — F5-27].

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Quy đổi báo cáo phân tích bài giải (F5.1) thành một điểm tham khảo thang 10 cho giảng viên | F5-27 | `01-rd/req/req.md:391-395` |
| Điểm AI 0-10 và điểm chấm tay là lớp tham khảo nội bộ, tách bạch hoàn toàn khỏi Pass/Fail chính thức (F4-04), không ghi đè/không đổi trạng thái submission | F5-27 | `01-rd/req/req.md:396-399` |
| Không phải "chấm điểm từng phần theo trọng số" (đã loại) | F5-27 | `01-rd/req/req.md:398` |
| Phạm vi hiển thị theo lớp giảng viên phụ trách, cùng cơ chế F2-12 | F5-27, F2-12 | `01-rd/req/req.md:400-401` |
| Gác bởi Function `CLASS_MANAGEMENT` trong ma trận phân quyền | F1-12, F1-10 | `01-rd/req/req.md:401-402, 64` |
| Nguồn của điểm quy đổi là báo cáo JSON có lược đồ của F5.1 | F5-07 | `01-rd/req/req.md:319` |
| Riêng biệt với điểm dễ đọc 1-5 do AI tự chấm trong cùng báo cáo F5.1 (không phải cùng một trường) | F5-05 | `01-rd/req/req.md:310-311` |
| Given-When-Then đầy đủ cho hành vi trên | — | `01-rd/req/user_stories.md:266-278` (`US-A2-06`) |
| Khu vực Giảng viên có layout/route riêng biệt khỏi khu Admin | — | `01-rd/overview/system_survey.md:508-512` |

## 3. Trạng thái và cấu trúc màn (đối chiếu prototype)

Đối chiếu `09-layoutBase/Giáo viên - Chấm bài.dc.html` — đây là hành vi UX thật đã dựng, trích dòng thật:

1. **Tiêu đề và mô tả màn** — "Chấm bài" / "Bài nộp AI chấm điểm thấp hoặc học viên yêu cầu review"
   [SoT: 09-layoutBase/Giáo viên - Chấm bài.dc.html:117-118]. Câu mô tả này ngụ ý màn hiển thị một **tập con
   đã lọc sẵn** (bài điểm AI thấp hoặc có yêu cầu review), không phải toàn bộ bài nộp của lớp như cách đọc
   "lướt nhanh chất lượng bài làm... trong lớp mình phụ trách" ở F5-27 gợi ý — xem Câu hỏi mở Q2.
2. **4 thẻ thống kê** (`stats`): "Chờ chấm" (9, "3 chờ quá 24 giờ"), "Yêu cầu review" (2, "Học viên chủ động
   gửi"), "Đã chấm tuần này" (31, +12 so với tuần trước), "Điểm TB sau chấm" (7.1, +0.4 "So với điểm AI")
   [SoT: 09-layoutBase/Giáo viên - Chấm bài.dc.html:122-133, 261-266]. Không có mã `Fx-nn` nào mô tả các chỉ
   số tổng hợp này — xem Câu hỏi mở Q4.
3. **Tab lọc trạng thái** (`statusTabs`): "Chờ chấm" / "Đã chấm" / "Tất cả" — trạng thái này là **trạng thái
   chấm của giảng viên** (đã lưu điểm chấm tay hay chưa), không phải trạng thái Pass/Fail của bài nộp
   [SoT: 09-layoutBase/Giáo viên - Chấm bài.dc.html:138-144, 268-280]. Xác nhận đúng tinh thần "tách bạch
   hoàn toàn khỏi Pass/Fail" của F5-27 [SoT: 01-rd/req/req.md:396-397] — không phát hiện vi phạm ở điểm này.
4. **Bảng danh sách bài chờ chấm** — cột Học viên, Bài tập, Lớp, Nộp lúc, Điểm AI, hành động
   [SoT: 09-layoutBase/Giáo viên - Chấm bài.dc.html:147-149]. Cột "Điểm AI" nhận một trong hai dạng giá trị:
   một điểm số dạng `x/10` (ví dụ "4/10") hoặc chuỗi văn bản **"Yêu cầu review"** thay cho điểm số
   [SoT: 09-layoutBase/Giáo viên - Chấm bài.dc.html:157, 208-217, 284]. Trường hợp thứ hai — học viên **chủ
   động yêu cầu giảng viên review** — không có mã `Fx-nn` nào mô tả, không xuất hiện ở `01-rd/req/req.md`
   hay bất kỳ file `01-rd/screens/*` nào đã có (đã kiểm tra `01-rd/screens/users/solution_review.md`, không
   có) — xem **Câu hỏi mở Q1 (ưu tiên cao)**.
5. **Cột hành động "Chấm ngay"** — mở modal chấm điểm [SoT:
   09-layoutBase/Giáo viên - Chấm bài.dc.html:158-160, 171-186].
6. **Modal chấm điểm** — ô nhập "Điểm (0-10)" dạng text tự do (không có ràng buộc min/max/step hiển thị
   trong markup), ô "Nhận xét" dạng textarea tự do, nút "Huỷ"/"Lưu điểm"
   [SoT: 09-layoutBase/Giáo viên - Chấm bài.dc.html:176-183]. Sau khi lưu, điểm chấm tay thay thế **cách
   hiển thị** ở cột Điểm AI (hiện `<điểm chấm tay>/10`) và đổi trạng thái dòng sang "Đã chấm"
   [SoT: 09-layoutBase/Giáo viên - Chấm bài.dc.html:282-289, 304-308] — đúng tinh thần "điểm chấm tay lưu
   lại cạnh điểm AI" của `US-A2-06` [SoT: 01-rd/req/user_stories.md:274-276], nhưng prototype hiện tại **ghi
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
[SoT: 01-rd/req/user_stories.md:271-278]. Các mục dưới đây là hành vi **riêng của màn hình** phát hiện khi
đối chiếu prototype, mà mô tả chức năng ở mức module chưa nêu:

- **Cho** tôi mở màn `instructor_grading`, **Khi** danh sách tải xong, **Thì** tôi thấy tab "Chờ chấm" được
  chọn mặc định, chỉ liệt kê các bài nộp chưa có điểm chấm tay của tôi
  [SoT: 09-layoutBase/Giáo viên - Chấm bài.dc.html:192, 275-280].
- **Cho** một dòng có cột Điểm AI hiện "Yêu cầu review" (không phải số), **Khi** tôi bấm "Chấm ngay", **Thì**
  modal vẫn mở bình thường và cho tôi nhập điểm 0-10 kèm nhận xét dù chưa có điểm AI quy đổi sẵn
  [SoT: 09-layoutBase/Giáo viên - Chấm bài.dc.html:171-186, 284-289] — hành vi này **giả định** báo cáo F5.1
  vẫn tồn tại phía sau dù giao diện không hiện số cụ thể; chưa xác nhận được với nguồn nào — xem Câu hỏi mở
  Q1.
- **Cho** tôi đã lưu điểm chấm tay cho một bài nộp, **Khi** tôi chuyển sang tab "Đã chấm", **Thì** tôi thấy
  lại đúng bài đó với điểm đã lưu, không mất khi đổi tab hay đổi bộ lọc trạng thái
  [SoT: 09-layoutBase/Giáo viên - Chấm bài.dc.html:277-289, 304-308].
- **Cho** tôi không có quyền `CLASS_MANAGEMENT` trên một lớp cụ thể, **Khi** tôi mở màn `instructor_grading`,
  **Thì** bài nộp của lớp đó không xuất hiện trong danh sách, không phải bị ẩn bằng CSS trên client
  [SoT: 01-rd/req/req.md:400-402 — F5-27, F1-12; suy diễn về nơi thực thi gác quyền phải ở tầng server, chưa
  có xác nhận riêng cho màn này — `[SoT: Suy luận]`].

## 5. Câu hỏi mở (chưa trả lời — không tự chọn thay)

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu / Ưu tiên |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | Prototype có cơ chế học viên **chủ động "yêu cầu review"** từ giảng viên (cột Điểm AI hiện "Yêu cầu review" thay vì điểm số, thẻ thống kê "Yêu cầu review · Học viên chủ động gửi") — không có mã `Fx-nn` nào mô tả tính năng này ở `01-rd/req/req.md`, không có Given-When-Then nào ở `01-rd/req/user_stories.md`, và không có nút/hành động tương ứng ở phía học viên trong `01-rd/screens/users/solution_review.md` hay `submission_result.md`. Đây là một tính năng **hai phía** (học viên bấm yêu cầu — giảng viên nhận việc) cần chốt ở mức `req.md` trước khi màn này có thể đặc tả đầy đủ. | Prototype thể hiện hành vi thật nhưng chưa có mã yêu cầu gốc; không được tự suy diễn thêm một tính năng mới vào `req.md`. | Bổ sung một mã `F5-xx` mới mô tả: điều kiện học viên được yêu cầu review (mọi bài `Accepted`? chỉ bài điểm AI thấp?), nút yêu cầu ở màn nào phía học viên, và cách nó đổ vào hàng đợi của màn này. | Chủ dự án / Cao |
| Q2 | Câu mô tả trên prototype ("Bài nộp AI chấm điểm thấp hoặc học viên yêu cầu review") ngụ ý màn này là một **hàng đợi đã lọc sẵn** theo tiêu chí (điểm AI dưới ngưỡng nào đó, hoặc có yêu cầu review) — trong khi F5-27 ở `req.md` mô tả rộng hơn là "lướt nhanh chất lượng bài làm của học viên trong lớp mình phụ trách", không nói rõ có lọc sẵn theo ngưỡng hay hiển thị toàn bộ kèm bộ lọc tuỳ chọn. Ngưỡng "điểm thấp" là bao nhiêu cũng chưa được định nghĩa ở đâu. | `req.md` không định nghĩa ngưỡng hay cơ chế lọc; prototype có hành vi cụ thể nhưng không có mã gốc xác nhận đây là chủ ý thiết kế hay chỉ là câu mô tả trang trí cho dữ liệu mẫu. | Làm rõ: (a) màn mặc định hiển thị toàn bộ bài `Accepted` có báo cáo F5.1 của lớp phụ trách, có thể lọc thêm theo điểm AI thấp/yêu cầu review dưới dạng tab tuỳ chọn; hoặc (b) mặc định đã lọc sẵn theo ngưỡng cố định. Nếu chọn ngưỡng, cần chốt giá trị cụ thể (ví dụ dưới 6/10). | Chủ dự án / Trung bình |
| Q3 | Sau khi giảng viên chấm tay, prototype **ghi đè cách hiển thị** cột Điểm AI thành điểm chấm tay, không còn thấy điểm AI gốc trên cùng một dòng. `US-A2-06` nói điểm chấm tay "lưu lại cạnh điểm AI, không ghi đè" — nhưng đó là nói về **lưu trữ dữ liệu** (không ghi đè bản ghi), còn đây là câu hỏi về **hiển thị** (có cần hiện song song cả hai điểm trên cùng dòng/modal hay không). | `req.md` và `user_stories.md` chỉ nói tới việc không ghi đè dữ liệu, không nói rõ yêu cầu hiển thị đồng thời cả hai điểm. | Hiện cả điểm AI gốc và điểm chấm tay cạnh nhau (ví dụ "AI: 4/10 · GV: 7/10") thay vì chỉ hiện một giá trị sau khi chấm, để giảng viên khác hoặc chính người chấm đối chiếu lại được. | Chủ dự án / Trung bình |
| Q4 | Bốn chỉ số thống kê đầu trang ("Chờ chấm", "Yêu cầu review", "Đã chấm tuần này", "Điểm TB sau chấm") không có mã `Fx-nn` nào mô tả cách tính, phạm vi thời gian ("tuần này" tính theo mốc nào), hay có cần lưu lịch sử để vẽ biểu đồ xu hướng hay chỉ là số tức thời. | Prototype chỉ có dữ liệu mẫu tĩnh, không có logic tính toán kèm theo. | Nếu giữ các thẻ này, cần một mã `Fx-nn` mới mô tả công thức tính từng chỉ số và mốc thời gian "tuần này" (theo tuần lịch hay 7 ngày gần nhất). | Chủ dự án / Thấp |
| Q5 | Giảng viên phụ trách nhiều lớp (dữ liệu mẫu trộn "Thuật toán K21", "CTDL K22", "Luyện PV nâng cao" trong cùng bảng) nhưng giao diện không có bộ lọc riêng theo từng lớp, chỉ có cột hiển thị tên lớp. Khi số lượng lớp/bài nộp lớn, có cần bộ lọc theo lớp trên chính màn này không, hay đã đủ vì luôn hiển thị gộp tất cả lớp được phân quyền? | Chưa có yêu cầu nào ở `req.md` nói rõ mức độ cần lọc theo lớp trên màn này (khác với `class_management`/`class_progress` vốn đã có bộ lọc lớp). | Bổ sung bộ lọc lớp dạng dropdown/tab nếu chủ dự án xác nhận cần, tương tự cách `class_progress` đã lọc theo lớp. | Chủ dự án / Thấp |

## 6. Ngoài phạm vi file này

- Layout, vùng bố cục, bảng màu, component cụ thể — thuộc BD (`02-bd/screens/teacher/instructor_grading.md`,
  chưa viết).
- Hợp đồng API (request/response lấy danh sách bài chờ chấm, lưu điểm chấm tay) — thuộc DD
  (`03-dd/api/ai-review.md` và/hoặc `03-dd/api/problem-bank.md`, chưa viết).
- Thuật toán quy đổi báo cáo F5.1 (JSON) thành điểm tham khảo thang 10 — thuộc BD/DD của module `ai-review`,
  không thuộc file theo trục màn này; chỉ ghi nhận ở đây rằng công thức quy đổi cụ thể **chưa được mô tả ở
  `req.md`** (F5-27 chỉ nói "hệ thống quy đổi thêm một điểm tham khảo trên thang 10" mà không nêu công
  thức) [SoT: 01-rd/req/req.md:393].
- Cơ chế "yêu cầu review" phía học viên (nếu được chốt theo Câu hỏi mở Q1) — thuộc RD của màn phía học viên
  (`submission_result` hoặc `solution_review`), không thuộc file theo trục màn `instructor_grading` này.
- Chi tiết ma trận phân quyền `CLASS_MANAGEMENT` (tạo/sửa/xoá Function, Action) — thuộc RD/BD của màn
  `admin_permission_matrix` (F1-10 tới F1-12), chỉ trỏ tới ở đây.

## 7. Tham chiếu

- `01-rd/req/req.md:391-402` — F5-27.
- `01-rd/req/req.md:55-68` — F1-10 tới F1-12, `CLASS_MANAGEMENT`.
- `01-rd/req/req.md:304-320` — F5-01 tới F5-08 (nguồn báo cáo F5.1 dùng để quy đổi điểm).
- `01-rd/req/req.md:244-246` — F4-04 (fail-fast, Pass/Fail chính thức không đổi).
- `01-rd/req/req.md:160-166` — F2-12 (cơ chế phạm vi theo lớp, dùng chung).
- `01-rd/req/user_stories.md:266-278` — `US-A2-06`.
- `01-rd/overview/system_survey.md:506-526` — mục 7.2 Khu vực giảng viên, dòng `instructor_grading`.
- `09-layoutBase/Giáo viên - Chấm bài.dc.html` — prototype đã dựng thật.
- `01-rd/screens/users/submission_result.md:52-62` — mẫu cách ghi nhận và sửa một vi phạm tách bạch dữ liệu
  (F2-08), dùng để đối chiếu kết luận "không có xung đột nghiêm trọng" ở mục 3.8 của file này.
- `01-rd/screens/users/solution_review.md` — đã kiểm tra, không có mô tả cơ chế "yêu cầu review" (liên quan
  Câu hỏi mở Q1).
