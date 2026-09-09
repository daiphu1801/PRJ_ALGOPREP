# RD — Màn `interview_question_management` (Quản lý ngân hàng câu hỏi phỏng vấn)

> Slug: `interview_question_management` (đổi tên từ `admin_interview_question_management` — quyết định
> 2026-08-25, xem dưới). Bounded Context: `interview-bank` (F6). **Actor: A2 và A3 — màn dùng chung, phạm vi
> dữ liệu theo quyền.** Đã chốt ngày 2026-08-25 (owner instruction, kết thúc Câu hỏi mở Q1 cũ): mount ở cả
> `/instructor/interview-questions` và `/admin/interview-questions`, cùng một view/BD/DD, phạm vi do quyền
> `INTERVIEW_BANK_MANAGEMENT` quyết định (`01-rd/req/identity.md` — F1-10 tới F1-12) — cả A2 và A3 đều thấy và
> sửa toàn bộ kho câu hỏi hệ thống (không chia theo lớp phụ trách — xem mục 2, cập nhật 2026-08-28,
> `DEC-2026-0828-remove-per-class-interview-set`). Ghi quyết định:
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
> File này mô tả **hành vi và UX ở mức yêu cầu** — không lặp lại đặc tả chức năng đã có ở `01-rd/req/interview-bank.md`
> mục F6, chỉ trỏ tới và bổ sung phần đặc thù của màn.

## 1. Mục đích màn hình

Bảng quản trị **nội dung** của ngân hàng câu hỏi phỏng vấn lý thuyết: xem toàn bộ câu hỏi kèm chủ đề, cấp
độ, câu hỏi đào sâu và tiêu chí đánh giá; lọc/tìm; thêm, sửa, nhân bản, xoá câu hỏi; nhập theo lô bằng CSV
[SoT: 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:140-141, 148-149, 226-228].

Phân biệt rạch ròi với hai màn phía người học, **không trùng lặp**:

| Màn | Actor | Việc làm ở đó |
| :--- | :--- | :--- |
| `interview_bank_list` | A1 | Duyệt/lọc kho câu hỏi để ôn, xem nhanh, tự chấm mức nhớ, luyện nhanh dạng flashcard [SoT: 01-rd/screens/users/interview_bank_list.md:17-20] |
| `interview_question_detail` | A1 | Chế độ học đầy đủ (F6-04 tới F6-06) và Chế độ luyện có AI đối chiếu (F6-07, F6-08) [SoT: 01-rd/overview/system_survey.md — mục 7.1 dòng `interview_question_detail`] |
| `interview_question_management` (file này) | A2 và A3 (dùng chung — đã chốt) | **Tạo và bảo trì chính nội dung** câu hỏi mà hai màn trên đọc. Không có chế độ học, không có chế độ luyện, không có tự chấm |

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Tự chấm mức độ thuộc bài (spaced repetition) — mã được `INTERVIEW_BANK_MANAGEMENT` gác | F6-12 | `01-rd/req/interview-bank.md` — F6-12 |
| Function `INTERVIEW_BANK_MANAGEMENT` gác F6-12 trong ma trận phân quyền | F1-10, F1-12 | `01-rd/req/identity.md` — F1-10, F1-12 |
| Phân loại theo chủ đề và mức độ khó; tìm kiếm và lọc (đọc phía người học, màn này tái dùng cùng phân loại) | F6-01, F6-02 | `01-rd/req/interview-bank.md` — F6-01, F6-02 |
| Tiêu chí chuẩn mà AI đối chiếu khi người học luyện — nội dung do màn này soạn ra | F6-08 | `01-rd/req/interview-bank.md` — F6-08 |
| Mọi thao tác quản trị ghi vào Nhật ký hệ thống | F1-14 | `01-rd/req/identity.md` — F1-14 |

**Cập nhật 2026-08-28:** F6-11 (bộ câu hỏi riêng theo lớp) đã loại khỏi phạm vi —
`DEC-2026-0828-remove-per-class-interview-set`. Toàn bộ ngân hàng câu hỏi giờ là **một kho duy nhất, dùng
chung ở cấp hệ thống** — không còn khái niệm "nửa F6-11 thuộc màn này, nửa thuộc `class_management`" như
trước.

**Cập nhật 2026-08-30 (owner instruction):** khoảng trống mã cho việc quản trị nội dung ngân hàng câu hỏi
đã lấp — cấp **`F6-13`** (`01-rd/req/interview-bank.md`) và **`US-A2-10`** (`01-rd/req/user_stories/a2_instructor.md`). Đóng luôn một
gói Q2 → Q7 (trừ Q1 đã đóng từ trước) — xem mục 5.

## 3. Trạng thái và cấu trúc màn (screen states)

Màn có **một trạng thái chính** (bảng thẻ có lọc và phân trang) cộng **một hộp thoại xác nhận xoá**
(`confirmOpen`) [SoT: 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:259-271].

1. **Thanh tiêu đề** — "Ngân hàng câu hỏi phỏng vấn" kèm dòng phụ "148 câu · chủ đề, cấp độ, câu hỏi đào sâu
   và tiêu chí đánh giá" (dòng 140-141). Bốn thành phần dữ liệu nêu ở dòng phụ chính là bốn nhóm trường của
   một câu hỏi mà màn này quản: chủ đề và cấp độ khớp F6-01; câu hỏi đào sâu và tiêu chí đánh giá nay có mã
   **F6-13** (chốt 2026-08-30 — đóng Q3).
2. **Hai hành động đầu trang** — "Nhập CSV" và "Câu hỏi mới" (dòng 148-149). **Chốt 2026-08-30:** "Nhập CSV"
   ngoài phạm vi bản đầu, không cấp mã (đóng Q2 phần này); "Câu hỏi mới" trỏ sang màn soạn riêng
   `interview_question_authoring` (F6-13, đóng Q4) — chưa có prototype minh hoạ **[Đợi nextjs]**.
3. **Bốn thẻ chỉ số** (dòng 152-163, dữ liệu tại dòng 428-433): Tổng câu hỏi 148 (+9 trong tháng); Có tiêu chí
   đầy đủ 131 = 89% ("17 câu thiếu rubric"); Điểm trung bình 3.5 trên thang 5, tính 30 ngày; Chưa dùng lần nào
   11. **Chốt 2026-08-30:** cả 4 thẻ là dẫn xuất trình bày từ dữ liệu đã có, không cần mã riêng (đóng Q5,
   phần chỉ số).
4. **Thanh lọc** (dòng 165-181): ô tìm "Tìm theo nội dung câu hỏi hoặc thẻ" (dòng 168), dải tab chủ đề (Tất
   cả / Lý thuyết CS / System design / Database / Ngôn ngữ / Hành vi — dòng 474), dải tab cấp độ (Tất cả / Dễ
   / Trung bình / Khó — dòng 475), và bộ đếm kết quả dạng "N / 148 câu" (dòng 478). Khớp F6-01, F6-02 — danh
   mục chủ đề của prototype nay là chuẩn (F6-01 sửa lại 2026-08-30, đóng Q6).
5. **Lưới thẻ câu hỏi** (dòng 183-233, dữ liệu mẫu dòng 383-410, sáu thẻ mỗi trang — dòng 450). Mỗi thẻ gồm:
   - **Mã câu hỏi** dạng `IQ-014`, **nhãn cấp độ** có màu theo Dễ/Trung bình/Khó, **chủ đề** (dòng 187-189) —
     F6-01.
   - **Nội dung câu hỏi** (dòng 192) — F6-01.
   - **Khối "Đào sâu"** — danh sách câu hỏi truy vấn tiếp theo cho cùng một câu hỏi gốc (dòng 194-204). Đây là
     dữ liệu phục vụ AI chất vấn sâu hơn, gần với giai đoạn Phản biện của phiên phỏng vấn (F5-11,
     `01-rd/req/ai-review.md`) nhưng **không phải** F6-04 (gợi ý hướng tiếp cận), F6-05 (khung trả lời chuẩn)
     hay F6-06 (từ khoá cốt lõi) [SoT: 01-rd/req/interview-bank.md — F6-04, F6-05, F6-06]. **Chốt 2026-08-30:** thuộc `F6-13` — đóng
     Q3.
   - **Khối "Tiêu chí đánh giá"** — 3 tới 4 tiêu chí, mỗi tiêu chí một trọng số phần trăm, các trọng số trong
     dữ liệu mẫu cộng đúng 100% (dòng 206-221; ví dụ `IQ-033`: 25 + 30 + 30 + 15, dòng 391). **Chốt
     2026-08-30:** đây chính là "tiêu chí chuẩn" mà F6-08 đối chiếu [SoT: 01-rd/req/interview-bank.md — F6-08], là
     rubric thứ ba trong hệ thống, độc lập với rubric F5-23/F5-15, trọng số giữ lại (`F6-13`) — đóng Q3.
   - **Chân thẻ** — thống kê sử dụng dạng "Dùng 184 lần · điểm TB 3.8/5" (dòng 224, 385) và ba hành động
     "Sửa" / "Nhân bản" / nút xoá (dòng 226-228).
6. **Phân trang** (dòng 235-244) — cơ chế giao diện, không cần mã.
7. **Hộp thoại xác nhận xoá** (dòng 259-271): tiêu đề "Xoá câu hỏi?", nội dung nêu mã và trích 60 ký tự đầu
   của câu hỏi (dòng 460), kèm ghi chú **"Các phiên phỏng vấn đã dùng câu hỏi này vẫn giữ bản ghi cũ. Hành
   động không thể hoàn tác."** (dòng 264). **Chốt 2026-08-30:** xoá mềm (đánh dấu ngừng dùng, ẩn khỏi màn
   phía học viên, không cascade xoá phiên cũ) — khớp đúng câu chữ hộp thoại; mọi thao tác thêm/sửa/nhân
   bản/xoá ghi vào Nhật ký hệ thống F1-14 (`F6-13`) — đóng Q7.
8. **Không có bất kỳ điều khiển nào liên quan tới lớp học hoặc "bộ câu hỏi"** trong toàn bộ prototype: không
   có bộ chọn lớp, không có khái niệm nhóm câu hỏi, không có hành động gán [SoT: Suy luận — đã đọc hết 504
   dòng của `09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html`; các nhãn duy nhất liên quan tới phạm vi là chủ
   đề và cấp độ, dòng 474-475]. **Cập nhật 2026-08-28:** đúng như prototype cho thấy — không cần khái niệm
   "bộ câu hỏi theo lớp" nữa, vì F6-11 đã loại khỏi phạm vi (`DEC-2026-0828-remove-per-class-interview-set`).
   Màn này giờ phủ **toàn bộ** việc quản trị nội dung ngân hàng câu hỏi, không còn "nửa F6-11 thuộc
   `class_management`" — xem Q1, Q2.

Hai chế độ hiển thị (`data-theme="light|dark"`) không đổi hành vi nghiệp vụ [SoT: 09-layoutBase/Admin - Câu
hỏi phỏng vấn.dc.html:56, 358-366].

## 4. Given-When-Then bổ sung ở mức màn

- **Cho** tôi có quyền `INTERVIEW_BANK_MANAGEMENT` (F1-10, F1-12), **Khi** tôi mở màn quản lý ngân hàng câu
  hỏi, **Thì** tôi thấy và sửa được toàn bộ kho câu hỏi hệ thống — không chia theo lớp (A2 và A3 cùng phạm
  vi dữ liệu, cập nhật 2026-08-28) [SoT: 01-rd/req/identity.md — F1-12; 01-rd/req/interview-bank.md — F6-13].
- **Cho** một câu hỏi chưa có đủ tiêu chí đánh giá, **Khi** tôi mở màn, **Thì** câu hỏi đó được đếm vào chỉ số
  "17 câu thiếu rubric" và phân biệt được với câu hỏi đã đủ tiêu chí [SoT: 09-layoutBase/Admin - Câu hỏi phỏng
  vấn.dc.html:430]. Chốt 2026-08-30 (F6-13): câu hỏi thiếu tiêu chí vẫn hiện ở Chế độ học, nhưng ẩn khỏi Chế
  độ luyện (F6-08) vì AI không có mốc đối chiếu để chấm.
- **Cho** tôi bấm xoá một câu hỏi đã được dùng trong các phiên phỏng vấn trước, **Khi** hộp thoại xác nhận
  hiện ra, **Thì** hệ thống nói rõ lịch sử phiên cũ vẫn được giữ và hành động không hoàn tác được, trước khi
  tôi xác nhận [SoT: 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:262-267] — khớp đúng cơ chế xoá mềm đã
  chốt ở F6-13.
- **Cho** tôi vừa thêm, sửa hoặc xoá một câu hỏi, **Khi** thao tác thành công, **Thì** thao tác được ghi vào
  Nhật ký hệ thống kèm ai làm, làm gì, lúc nào, theo F1-14 [SoT: 01-rd/req/identity.md — F1-14]. Chốt 2026-08-30
  (F6-13): thao tác nội dung F6 nằm trong phạm vi audit, cùng nhóm "hành động quản trị của người" như các
  thao tác khác đã liệt kê ở F1-14.

## 5. Câu hỏi mở

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu | Ưu tiên |
| :-: | :--- | :--- | :--- | :--- | :--- |
| Q1 | ~~`interview-bank.md` gán F6-11 cho A2, nhưng prototype dựng trong shell Admin — thuộc actor nào?~~ **ĐÃ CHỐT 2026-08-25 (owner instruction):** dùng chung một màn, mount ở cả `/instructor/interview-questions` và `/admin/interview-questions`, phạm vi dữ liệu do quyền `INTERVIEW_BANK_MANAGEMENT` quyết định [SoT: 01-rd/req/identity.md — F1-10 tới F1-12]. Ghi quyết định `DEC-2026-0825-shared-content-authoring-screens`, chốt cùng lúc với `problem_management`/`problem_authoring`. **Cập nhật 2026-08-28:** phần "class_management giữ phần gán bộ câu hỏi cho lớp" không còn đúng — F6-11 đã loại khỏi phạm vi (`DEC-2026-0828-remove-per-class-interview-set`), màn này giờ phủ toàn bộ, không chia nửa với `class_management` nữa. | — | Đã chốt. | Đã đóng | — |
| Q2 | ~~Có tồn tại một kho câu hỏi dùng chung toàn hệ thống do A2/A3 quản hay không? Cần mã mới cho CRUD kho dùng chung.~~ **ĐÃ CHỐT 2026-08-30 (owner instruction):** cấp `F6-13` — "quản trị nội dung ngân hàng câu hỏi dùng chung: tạo/sửa/nhân bản/xoá", gán vào Function `INTERVIEW_BANK_MANAGEMENT` (F1-12) cùng F6-12. Bổ sung `US-A2-10`. | — | Đã chốt — xem `F6-13`. | Đã đóng | — |
| Q3 | ~~"Đào sâu" và "Tiêu chí đánh giá có trọng số" không gán được mã nào — độc lập với rubric F5 hay chính là tiêu chí chuẩn của F6-08?~~ **ĐÃ CHỐT 2026-08-30:** cả hai thuộc `F6-13`. "Tiêu chí đánh giá" chính là tiêu chí chuẩn F6-08 đối chiếu — rubric thứ ba, độc lập với F5-15/F5-23; giữ trọng số vì Chế độ luyện cần mốc tính điểm theo từng tiêu chí. | — | Đã chốt — xem `F6-13`. | Đã đóng | — |
| Q4 | ~~Việc thêm/sửa câu hỏi diễn ra ở đâu — modal, drawer, hay màn riêng?~~ **ĐÃ CHỐT 2026-08-30:** một màn soạn riêng, slug `interview_question_authoring` (F6-13) — 4 nhóm trường lồng nhau quá nặng cho modal, song song tiền lệ `problem_authoring`. Chưa có prototype, đánh dấu **[Đợi nextjs]**; cần thêm slug vào `system_survey.md` khi viết BD/DD. | — | Đã chốt — xem `F6-13`. | Đã đóng | — |
| Q5 | ~~"Nhập CSV" và 4 thẻ chỉ số chất lượng nội dung có thuộc phạm vi đồ án không? Câu hỏi thiếu tiêu chí thì Chế độ luyện xử lý thế nào?~~ **ĐÃ CHỐT 2026-08-30:** Nhập CSV ngoài phạm vi bản đầu, không cấp mã. 4 thẻ chỉ số là dẫn xuất trình bày, không cần mã riêng. Câu hỏi thiếu tiêu chí vẫn hiện ở Chế độ học, ẩn khỏi Chế độ luyện (F6-13, F6-08). | — | Đã chốt — xem `F6-13`. | Đã đóng | — |
| Q6 | ~~Danh mục chủ đề lệch giữa `interview-bank.md` (4 chủ đề) và prototype (5 chủ đề) — danh mục nào chuẩn?~~ **ĐÃ CHỐT 2026-08-30:** lấy danh mục prototype làm chuẩn — 5 chủ đề (Lý thuyết CS, System design, Database, Ngôn ngữ, Hành vi). `F6-01` trong `interview-bank.md` đã sửa lại cho khớp. | — | Đã chốt. | Đã đóng | — |
| Q7 | ~~Xoá một câu hỏi đã dùng — xoá mềm hay cứng? Có ghi Nhật ký hệ thống không?~~ **ĐÃ CHỐT 2026-08-30:** xoá mềm (đánh dấu ngừng dùng, ẩn khỏi màn phía học viên, không cascade xoá phiên cũ) — khớp đúng câu chữ hộp thoại prototype. Có, mọi thao tác thêm/sửa/nhân bản/xoá ghi vào F1-14 (F6-13). | — | Đã chốt — xem `F6-13`. | Đã đóng | — |

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
- Màn soạn/sửa một câu hỏi (slug `interview_question_authoring`, chốt 2026-08-30 — F6-13) — RD riêng ở
  `01-rd/screens/shared/interview_question_authoring.md` (viết 2026-09-01), không đặc tả lại ở đây.
- Thuật toán và công thức chấm của Chế độ luyện — thuộc logic `interview-bank` và `ai-review`.

## 7. Tham chiếu

- `01-rd/req/identity.md` — F1-12, Function `INTERVIEW_BANK_MANAGEMENT` gác F6-12 (F6-11 đã loại khỏi phạm vi,
  `DEC-2026-0828-remove-per-class-interview-set`), F1-14 (phạm vi Nhật ký hệ thống).
- `01-rd/req/interview-bank.md` — mục F6 đầy đủ, F6-01 tới F6-13.
- `01-rd/req/user_stories/a2_instructor.md` — `US-A2-10` (chốt 2026-08-30).
- Quyết định: `DEC-2026-0828-remove-per-class-interview-set`,
  `DEC-2026-0830-interview-bank-crud`.
- `01-rd/overview/system_survey.md` mục 7.2 (khu Giảng viên) và mục 7.0 (khu dùng chung).
- `01-rd/screens/users/interview_bank_list.md` · `01-rd/screens/users/interview_question_detail.md` — hai màn
  phía người học đọc dữ liệu do màn này tạo.
- `01-rd/screens/admin/admin_ai_config.md` — hai rubric của F5, để đối chiếu với rubric theo câu hỏi ở Q3.
- `06-plan/PROTOTYPE_DEBT.md` mục 6.2.b và mục 7 — tiền lệ và bản ghi đợt đối chiếu khu Admin.
- Quyết định: `DEC-2026-0825-shared-content-authoring-screens`.
- `09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html` — prototype (504 dòng).
