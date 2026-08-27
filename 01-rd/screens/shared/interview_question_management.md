# RD — Màn `interview_question_management` (Quản lý ngân hàng câu hỏi phỏng vấn)

> Slug: `interview_question_management` (đổi tên từ `admin_interview_question_management` — quyết định
> 2026-08-25, xem dưới). Bounded Context: `interview-bank` (F6). **Actor: A2 và A3 — màn dùng chung, phạm vi
> dữ liệu theo quyền.** Đã chốt ngày 2026-08-25 (owner instruction, kết thúc Câu hỏi mở Q1 cũ): mount ở cả
> `/instructor/interview-questions` và `/admin/interview-questions`, cùng một view/BD/DD, phạm vi do quyền
> `INTERVIEW_BANK_MANAGEMENT` quyết định (`req.md:67`, F1-10 tới F1-12) — A2 chỉ thấy/sửa bộ câu hỏi của lớp
> mình phụ trách, A3 thấy toàn bộ kho. Ghi quyết định:
> `DEC-2026-0825-shared-content-authoring-screens`.
>
> Đối chiếu prototype: `09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html` (504 dòng, dựng trong shell Admin —
> khu Giảng viên chưa có prototype tương ứng). Đây là nav cấp 1 trong nhóm "Nội dung" của khu Admin
> (`activeKey = 'iquestions'`) [SoT: 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:297, 303-306].
>
> **Lịch sử slug:** bổ sung 2026-08-25 với tên `admin_interview_question_management` (màn chưa từng có trong
> `system_survey.md` mục 7.3 dù prototype đã dựng thật), đổi tên cùng ngày sang `interview_question_management`
> và chuyển từ `01-rd/screens/admin/` sang `01-rd/screens/shared/` sau khi chốt dùng chung hai khu vực.
>
> File này mô tả **hành vi và UX ở mức yêu cầu** — không lặp lại đặc tả chức năng đã có ở `01-rd/req/req.md`
> mục F6, chỉ trỏ tới và bổ sung phần đặc thù của màn.

## 1. Mục đích màn hình

Bảng quản trị **nội dung** của ngân hàng câu hỏi phỏng vấn lý thuyết: xem toàn bộ câu hỏi kèm chủ đề, cấp
độ, câu hỏi đào sâu và tiêu chí đánh giá; lọc/tìm; thêm, sửa, nhân bản, xoá câu hỏi; nhập theo lô bằng CSV
[SoT: 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:140-141, 148-149, 226-228].

Phân biệt rạch ròi với hai màn phía người học, **không trùng lặp**:

| Màn | Actor | Việc làm ở đó |
| :--- | :--- | :--- |
| `interview_bank_list` | A1 | Duyệt/lọc kho câu hỏi để ôn, xem nhanh, tự chấm mức nhớ, luyện nhanh dạng flashcard [SoT: 01-rd/screens/users/interview_bank_list.md:17-20] |
| `interview_question_detail` | A1 | Chế độ học đầy đủ (F6-04 tới F6-06) và Chế độ luyện có AI đối chiếu (F6-07, F6-08) [SoT: 01-rd/overview/system_survey.md:482] |
| `interview_question_management` (file này) | A2 và A3 (dùng chung — đã chốt) | **Tạo và bảo trì chính nội dung** câu hỏi mà hai màn trên đọc. Không có chế độ học, không có chế độ luyện, không có tự chấm |

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Quản lý theo lớp: tạo bộ câu hỏi riêng và gán cho lớp phụ trách (actor A2) | F6-11 | `01-rd/req/req.md:422` |
| Tự chấm mức độ thuộc bài (spaced repetition) — mã thứ hai được `INTERVIEW_BANK_MANAGEMENT` gác | F6-12 | `01-rd/req/req.md:423-429` |
| Function `INTERVIEW_BANK_MANAGEMENT` gác cặp (F6-11, F6-12) trong ma trận phân quyền | F1-10, F1-12 | `01-rd/req/req.md:67` |
| Phân loại theo chủ đề và mức độ khó; tìm kiếm và lọc (đọc phía người học, màn này tái dùng cùng phân loại) | F6-01, F6-02 | `01-rd/req/req.md:413-414` |
| Tiêu chí chuẩn mà AI đối chiếu khi người học luyện — nội dung do màn này soạn ra | F6-08 | `01-rd/req/req.md:417-419` |
| Mọi thao tác quản trị ghi vào Nhật ký hệ thống | F1-14 | `01-rd/req/req.md:76-79` |
| Given-When-Then liên quan (phía giảng viên) | — | `01-rd/req/user_stories.md:243-249` (`US-A2-04`) |

**Không có mã nào cho: quản trị nội dung ngân hàng câu hỏi ở phạm vi toàn hệ thống.** F6-11 chỉ nói tới việc
*giảng viên tạo bộ câu hỏi riêng và gán cho lớp*, không nói tới CRUD kho câu hỏi dùng chung [SoT:
01-rd/req/req.md:422]. Đây là phát hiện chính của file này — xem mục 5, Q1 và Q2. Không tự phát minh mã mới.

## 3. Trạng thái và cấu trúc màn (screen states)

Màn có **một trạng thái chính** (bảng thẻ có lọc và phân trang) cộng **một hộp thoại xác nhận xoá**
(`confirmOpen`) [SoT: 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:259-271].

1. **Thanh tiêu đề** — "Ngân hàng câu hỏi phỏng vấn" kèm dòng phụ "148 câu · chủ đề, cấp độ, câu hỏi đào sâu
   và tiêu chí đánh giá" (dòng 140-141). Bốn thành phần dữ liệu nêu ở dòng phụ chính là bốn nhóm trường của
   một câu hỏi mà màn này quản: chủ đề và cấp độ khớp F6-01; câu hỏi đào sâu và tiêu chí đánh giá **chưa có
   mã** (Q3).
2. **Hai hành động đầu trang** — "Nhập CSV" và "Câu hỏi mới" (dòng 148-149). Cả hai **không gắn được mã nào**:
   nhập theo lô không xuất hiện ở F6 trong `req.md` (Q2), và "Câu hỏi mới" không dẫn tới đâu trong prototype
   (Q4 — prototype không có màn soạn câu hỏi).
3. **Bốn thẻ chỉ số** (dòng 152-163, dữ liệu tại dòng 428-433): Tổng câu hỏi 148 (+9 trong tháng); Có tiêu chí
   đầy đủ 131 = 89% ("17 câu thiếu rubric"); Điểm trung bình 3.5 trên thang 5, tính 30 ngày; Chưa dùng lần nào
   11. Thẻ thứ hai và thứ tư là **chỉ số chất lượng nội dung** (thiếu tiêu chí, chưa được dùng) — hợp lý cho
   một bảng quản trị nội dung nhưng không có mã; thẻ thứ ba tổng hợp điểm từ kết quả luyện của người học
   (nguồn dữ liệu là F6-08). Xem Q5.
4. **Thanh lọc** (dòng 165-181): ô tìm "Tìm theo nội dung câu hỏi hoặc thẻ" (dòng 168), dải tab chủ đề (Tất
   cả / Lý thuyết CS / System design / Database / Ngôn ngữ / Hành vi — dòng 474), dải tab cấp độ (Tất cả / Dễ
   / Trung bình / Khó — dòng 475), và bộ đếm kết quả dạng "N / 148 câu" (dòng 478). Khớp F6-01, F6-02 — nhưng
   **danh mục chủ đề lệch với `req.md`**, xem Q6.
5. **Lưới thẻ câu hỏi** (dòng 183-233, dữ liệu mẫu dòng 383-410, sáu thẻ mỗi trang — dòng 450). Mỗi thẻ gồm:
   - **Mã câu hỏi** dạng `IQ-014`, **nhãn cấp độ** có màu theo Dễ/Trung bình/Khó, **chủ đề** (dòng 187-189) —
     F6-01.
   - **Nội dung câu hỏi** (dòng 192) — F6-01.
   - **Khối "Đào sâu"** — danh sách câu hỏi truy vấn tiếp theo cho cùng một câu hỏi gốc (dòng 194-204). Đây là
     dữ liệu phục vụ AI chất vấn sâu hơn, gần với giai đoạn Phản biện của phiên phỏng vấn (F5-11,
     `01-rd/req/req.md:332`) nhưng **không phải** F6-04 (gợi ý hướng tiếp cận), F6-05 (khung trả lời chuẩn)
     hay F6-06 (từ khoá cốt lõi) [SoT: 01-rd/req/req.md:415-416]. Không gán được mã — Q3.
   - **Khối "Tiêu chí đánh giá"** — 3 tới 4 tiêu chí, mỗi tiêu chí một trọng số phần trăm, các trọng số trong
     dữ liệu mẫu cộng đúng 100% (dòng 206-221; ví dụ `IQ-033`: 25 + 30 + 30 + 15, dòng 391). Đây hợp lý là
     "tiêu chí chuẩn" mà F6-08 đối chiếu [SoT: 01-rd/req/req.md:417-418], nhưng phần **trọng số** không có
     nguồn ở `req.md` và tạo ra **rubric thứ ba** trong hệ thống, bên cạnh rubric 5 tiêu chí của Phân tích bài
     giải và rubric 4 tiêu chí của Phỏng vấn giả lập (F5-23, F5-15) [SoT:
     01-rd/screens/admin/admin_ai_config.md:39-43; 01-rd/req/req.md:337-339]. Xem Q3.
   - **Chân thẻ** — thống kê sử dụng dạng "Dùng 184 lần · điểm TB 3.8/5" (dòng 224, 385) và ba hành động
     "Sửa" / "Nhân bản" / nút xoá (dòng 226-228).
6. **Phân trang** (dòng 235-244) — cơ chế giao diện, không cần mã.
7. **Hộp thoại xác nhận xoá** (dòng 259-271): tiêu đề "Xoá câu hỏi?", nội dung nêu mã và trích 60 ký tự đầu
   của câu hỏi (dòng 460), kèm ghi chú **"Các phiên phỏng vấn đã dùng câu hỏi này vẫn giữ bản ghi cũ. Hành
   động không thể hoàn tác."** (dòng 264). Ghi chú này là một **quy tắc dữ liệu** (xoá câu hỏi không làm hỏng
   lịch sử phiên đã lưu — cùng tinh thần F5-16 "lưu phiên và rubric, mở lại được", `01-rd/req/req.md:338-339`)
   nhưng chưa được phát biểu ở đâu trong `req.md`. Xem Q7.
8. **Không có bất kỳ điều khiển nào liên quan tới lớp học hoặc "bộ câu hỏi"** trong toàn bộ prototype: không
   có bộ chọn lớp, không có khái niệm nhóm câu hỏi, không có hành động gán [SoT: Suy luận — đã đọc hết 504
   dòng của `09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html`; các nhãn duy nhất liên quan tới phạm vi là chủ
   đề và cấp độ, dòng 474-475]. Vì phần "gán cho lớp phụ trách" mới là nội dung cốt lõi của F6-11
   (`01-rd/req/req.md:422`) và của `US-A2-04` (`01-rd/req/user_stories.md:248-249`), màn này **chỉ phủ được
   một nửa F6-11** (soạn nội dung), nửa còn lại (bộ câu hỏi + gán lớp) đang thuộc `class_management` theo bảng
   slug [SoT: 01-rd/overview/system_survey.md:523]. Xem Q1.

Hai chế độ hiển thị (`data-theme="light|dark"`) không đổi hành vi nghiệp vụ [SoT: 09-layoutBase/Admin - Câu
hỏi phỏng vấn.dc.html:56, 358-366].

## 4. Given-When-Then bổ sung ở mức màn

- **Cho** tôi có quyền `INTERVIEW_BANK_MANAGEMENT` (F1-10, F1-12), **Khi** tôi mở màn quản lý ngân hàng câu
  hỏi, **Thì** tôi chỉ thấy và chỉ sửa được phần kho câu hỏi thuộc phạm vi vai trò của tôi — phạm vi cụ thể
  của A2 so với A3 chờ Q1 [SoT: 01-rd/req/req.md:67].
- **Cho** một câu hỏi chưa có đủ tiêu chí đánh giá, **Khi** tôi mở màn, **Thì** câu hỏi đó được đếm vào chỉ số
  "17 câu thiếu rubric" và phân biệt được với câu hỏi đã đủ tiêu chí [SoT: 09-layoutBase/Admin - Câu hỏi phỏng
  vấn.dc.html:430]. Hệ quả cần chốt ở DD: câu hỏi thiếu tiêu chí thì Chế độ luyện (F6-08) xử lý thế nào — cho
  luyện mà AI không có mốc đối chiếu, hay ẩn khỏi Chế độ luyện. Xem Q5.
- **Cho** tôi bấm xoá một câu hỏi đã được dùng trong các phiên phỏng vấn trước, **Khi** hộp thoại xác nhận
  hiện ra, **Thì** hệ thống nói rõ lịch sử phiên cũ vẫn được giữ và hành động không hoàn tác được, trước khi
  tôi xác nhận [SoT: 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:262-267].
- **Cho** tôi vừa thêm, sửa hoặc xoá một câu hỏi, **Khi** thao tác thành công, **Thì** thao tác được ghi vào
  Nhật ký hệ thống kèm ai làm, làm gì, lúc nào, theo F1-14 [SoT: 01-rd/req/req.md:76-79]. **Lưu ý:** F1-14
  chốt phạm vi là "hành động quản trị của người", và thao tác nội dung ở màn này đúng loại đó, nhưng
  `req.md:76-79` liệt kê ví dụ toàn về `identity` (đổi ma trận quyền, đổi vai trò, khoá/mở khoá) — cần xác nhận
  thao tác nội dung F6 có nằm trong phạm vi audit hay không. Xem Q7.

## 5. Câu hỏi mở

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu | Ưu tiên |
| :-: | :--- | :--- | :--- | :--- | :--- |
| Q1 | ~~`req.md` gán F6-11 cho A2, nhưng prototype dựng trong shell Admin — thuộc actor nào?~~ **ĐÃ CHỐT 2026-08-25 (owner instruction):** dùng chung một màn, mount ở cả `/instructor/interview-questions` và `/admin/interview-questions`, phạm vi dữ liệu do quyền `INTERVIEW_BANK_MANAGEMENT` quyết định [SoT: 01-rd/req/req.md:67, F1-10 tới F1-12] — A2 chỉ thấy/sửa bộ câu hỏi của lớp mình phụ trách, A3 thấy toàn bộ kho. Ghi quyết định `DEC-2026-0825-shared-content-authoring-screens`, chốt cùng lúc với `problem_management`/`problem_authoring` (cùng dạng lệch). `class_management` vẫn giữ phần "gán bộ câu hỏi cho lớp" của F6-11 — màn này chỉ phủ phần soạn nội dung. | — | Đã chốt. | Đã đóng | — |
| Q2 | Ngoài F6-11 (theo lớp, A2), có tồn tại **một kho câu hỏi dùng chung toàn hệ thống do A3 quản** hay không? Nếu có thì cần một mã `Fx-nn` mới cho hành vi CRUD kho dùng chung, vì F6-01 tới F6-12 hiện **không có mã nào** cho việc đó. | `req.md` mục F6 chỉ mô tả phía tiêu thụ (người học duyệt/học/luyện) và một dòng quản lý theo lớp; không có dòng nào nói ai tạo ra 148 câu hỏi dùng chung [SoT: 01-rd/req/req.md:413-429]. | Nếu Q1 chốt phương án dùng chung màn: cấp một mã mới (đề xuất `F6-13` — "quản trị nội dung ngân hàng câu hỏi dùng chung: tạo/sửa/nhân bản/xoá, phạm vi theo `INTERVIEW_BANK_MANAGEMENT`") và gán mã đó vào Function `INTERVIEW_BANK_MANAGEMENT` ở F1-12 cùng F6-11, F6-12. Không tự thêm mã trong file theo trục màn này. | Chủ dự án | Cao |
| Q3 | Hai trường dữ liệu **"Đào sâu" (câu hỏi truy vấn tiếp)** và **"Tiêu chí đánh giá có trọng số phần trăm"** của mỗi câu hỏi không gán được mã nào [SoT: 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:194-221]. "Tiêu chí đánh giá" ở đây là rubric **thứ ba** trong hệ thống, sau rubric 5 tiêu chí của Phân tích bài giải (F5-23) và rubric 4 tiêu chí của Phỏng vấn giả lập (F5-15) [SoT: 01-rd/screens/admin/admin_ai_config.md:39-43]. Ba rubric này là ba thứ độc lập, hay rubric theo câu hỏi ở đây chính là "tiêu chí chuẩn" của F6-08 và cần được nói rõ như vậy trong `req.md`? | F6-08 nói "AI đối chiếu với tiêu chí chuẩn" nhưng không nói tiêu chí đó gắn theo từng câu hỏi, cũng không nói có trọng số [SoT: 01-rd/req/req.md:417-418]. "Đào sâu" không khớp bất kỳ mã nào trong F6-04 tới F6-06 [SoT: 01-rd/req/req.md:415-416]. | **Đề xuất:** làm rõ trong `req.md` mục F6 rằng mỗi câu hỏi có (a) danh sách câu hỏi đào sâu và (b) bộ tiêu chí đánh giá theo câu hỏi — chính là "tiêu chí chuẩn" của F6-08, độc lập với hai rubric của F5. Về trọng số: chỉ giữ nếu Chế độ luyện thật sự trả điểm số theo trọng số; nếu F6-08 chỉ trả phản hồi định tính ("điểm đã đạt, điểm còn thiếu") thì bỏ cột trọng số cho gọn. | Chủ dự án | Trung bình |
| Q4 | **Việc thêm và sửa một câu hỏi diễn ra ở đâu?** Prototype có nút "Câu hỏi mới" (dòng 149) và nút "Sửa" trên từng thẻ (dòng 226) nhưng **không dựng bất kỳ biểu mẫu nào** — không modal, không drawer, không màn riêng; cả hai nút không có `onClick` [SoT: 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:148-149, 226-227 — chỉ có nút xoá là có `onClick="{{ c.askDelete }}"` ở dòng 228]. Trong `09-layoutBase/` cũng không có file prototype nào cho việc soạn câu hỏi phỏng vấn. Q1 đã chốt màn này dùng chung hai khu vực, nhưng **chưa quyết** hình thức soạn/sửa (modal/drawer hay màn riêng). | Đây là khoảng trống của prototype, không phải một lựa chọn thiết kế đã dựng. Không suy ra được từ tài liệu nào vì cả `req.md` và `system_survey.md` chưa có màn/mã tương ứng. | **Đề xuất:** một câu hỏi có 4 nhóm trường lồng nhau (nội dung, danh sách đào sâu, danh sách tiêu chí có trọng số, phân loại), quá nặng cho modal — nên là **màn soạn riêng** (đề xuất slug `interview_question_authoring`), song song với `problem_authoring` (tách khỏi màn danh sách `problem_management` theo đúng tiền lệ vừa chốt). Bản dựng cụ thể **[Đợi nextjs]**. | Chủ dự án | Cao |
| Q5 | "Nhập CSV" (nhập câu hỏi theo lô) và bốn thẻ chỉ số chất lượng nội dung (thiếu rubric, chưa dùng lần nào, điểm trung bình 30 ngày) có thuộc phạm vi đồ án hay không? Cả hai không có mã [SoT: 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:148, 428-433]. Kèm theo: **câu hỏi thiếu tiêu chí đánh giá thì Chế độ luyện (F6-08) xử lý thế nào** — cho luyện mà AI không có mốc đối chiếu, hay ẩn khỏi Chế độ luyện? | Nhập theo lô là một luồng nghiệp vụ riêng (định dạng, kiểm tra dữ liệu, xử lý lỗi từng dòng, trùng lặp), không thể coi là chi tiết giao diện của một mã sẵn có. Phần thiếu rubric là hệ quả trực tiếp lên hành vi AI, chưa có tài liệu nào nói. | **Đề xuất:** (a) Nhập CSV — hoãn, ghi vào ngoài phạm vi phiên bản đầu, giữ nút ở prototype nhưng không cam kết; nếu giữ thì cần mã riêng. (b) Chỉ số chất lượng nội dung — coi là chi tiết trình bày của màn quản trị, không cần mã. (c) Câu hỏi thiếu tiêu chí: **vẫn cho học (Chế độ học) nhưng ẩn khỏi Chế độ luyện**, vì F6-08 không có mốc đối chiếu thì phản hồi AI mất căn cứ. | Chủ dự án | Trung bình |
| Q6 | **Danh mục chủ đề lệch giữa `req.md` và prototype.** `req.md` ghi 4 chủ đề: Cấu trúc dữ liệu, Thuật toán, Thiết kế hệ thống, Câu hỏi hành vi [SoT: 01-rd/req/req.md:413-414]. Prototype (cả màn này và màn người học) dùng 5 chủ đề khác: Lý thuyết CS, System design, Database, Ngôn ngữ, Hành vi [SoT: 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:474; 01-rd/screens/users/interview_bank_list.md:44-45]. Danh mục nào là chuẩn? | Danh mục chủ đề là dữ liệu seed, ảnh hưởng cả bộ lọc ở 3 màn và cả trường phân loại khi soạn câu hỏi — không tự đổi được ở một file theo trục màn. | **Đề xuất:** lấy danh mục prototype làm chuẩn (5 chủ đề — bao phủ rộng hơn, có Database và Ngôn ngữ là hai nhóm câu hỏi phỏng vấn rất phổ biến mà bản `req.md` bỏ sót), rồi cập nhật lại F6-01 trong `req.md` cho khớp. Cần chủ dự án xác nhận vì đây là dữ liệu seed dùng chung. | Chủ dự án | Trung bình |
| Q7 | Xoá một câu hỏi đã được dùng: prototype cam kết "các phiên phỏng vấn đã dùng câu hỏi này vẫn giữ bản ghi cũ" và "không thể hoàn tác" [SoT: 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:264]. Đây là xoá mềm hay xoá cứng kèm sao chép nội dung câu hỏi vào bản ghi phiên? Và thao tác sửa/xoá nội dung F6 có ghi vào Nhật ký hệ thống (F1-14) không? | `req.md` không có dòng nào về vòng đời/xoá câu hỏi. F1-14 chốt phạm vi "hành động quản trị của người" nhưng ví dụ liệt kê toàn thuộc `identity` [SoT: 01-rd/req/req.md:76-79], chưa rõ có phủ thao tác nội dung F6. | **Đề xuất:** (a) xoá mềm (đánh dấu ngừng dùng, ẩn khỏi mọi màn người học) — vừa giữ được lịch sử phiên vừa khớp lời cam kết trên hộp thoại, và tránh phải nhân bản nội dung câu hỏi vào từng bản ghi phiên; (b) có, thao tác thêm/sửa/xoá câu hỏi ghi vào F1-14 vì đúng định nghĩa "hành động quản trị của người". Cả hai là đề xuất, cần xác nhận. | Chủ dự án | Trung bình |

**Ghi chú:** Q1 đã được chốt cùng lúc với `problem_management`/`problem_authoring` (cùng dạng lệch — nội
dung do A2 soạn nhưng prototype dựng trong shell Admin), xem `DEC-2026-0825-shared-content-authoring-screens`.

## 6. Ngoài phạm vi file này

- Bảng màu, spacing, component cụ thể, breakpoint responsive, cách trình bày thanh trọng số — thuộc BD
  (`02-bd/screens/shared/interview_question_management.md`, chưa viết).
- Hợp đồng API (danh sách câu hỏi có lọc/tìm/phân trang, tạo/sửa/nhân bản/xoá, nhập CSV, chỉ số thống kê) —
  thuộc DD (`03-dd/api/interview-bank.md`, chưa viết).
- Lược đồ dữ liệu của một câu hỏi (trường đào sâu, trường tiêu chí có trọng số, cơ chế xoá mềm) — thuộc
  `02-bd/database/interview-bank.md`, chưa viết.
- Nội dung Chế độ học và Chế độ luyện (F6-04 tới F6-08) — thuộc
  `01-rd/screens/users/interview_question_detail.md`.
- Duyệt/lọc/tự chấm phía người học (F6-01 tới F6-03, F6-12) — thuộc
  `01-rd/screens/users/interview_bank_list.md`.
- Gán bộ câu hỏi cho lớp (nửa còn lại của F6-11) — thuộc `class_management`
  [SoT: 01-rd/overview/system_survey.md mục 7.2].
- Màn soạn/sửa một câu hỏi — chưa có slug, chờ Q4.
- Thuật toán và công thức chấm của Chế độ luyện — thuộc logic `interview-bank` và `ai-review`.

## 7. Tham chiếu

- `01-rd/req/req.md:67` — F1-12, Function `INTERVIEW_BANK_MANAGEMENT` gác (F6-11, F6-12).
- `01-rd/req/req.md:76-79` — F1-14, phạm vi Nhật ký hệ thống.
- `01-rd/req/req.md:409-429` — mục F6 đầy đủ, F6-01 tới F6-12.
- `01-rd/req/user_stories.md:243-249` — `US-A2-04`.
- `01-rd/overview/system_survey.md` mục 7.2 (khu Giảng viên) và mục 7.0 (khu dùng chung).
- `01-rd/screens/users/interview_bank_list.md` · `01-rd/screens/users/interview_question_detail.md` — hai màn
  phía người học đọc dữ liệu do màn này tạo.
- `01-rd/screens/admin/admin_ai_config.md` — hai rubric của F5, để đối chiếu với rubric theo câu hỏi ở Q3.
- `06-plan/PROTOTYPE_DEBT.md` mục 6.2.b và mục 7 — tiền lệ và bản ghi đợt đối chiếu khu Admin.
- `.nexa/control/decision-registry.md` — `DEC-2026-0825-shared-content-authoring-screens`.
- `09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html` — prototype (504 dòng).
