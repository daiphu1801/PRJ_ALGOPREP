# RD — Màn `interview_bank_list` (Danh sách câu hỏi phỏng vấn)

> Slug: `interview_bank_list` — khớp `01-rd/overview/system_survey.md` mục 7.1 dòng `interview_bank_list`
> [SoT: 01-rd/overview/system_survey.md:481]. Bounded Context: `interview-bank` (F6). Actor: A1 (chính).
>
> Theo lộ trình Phase 4 của `06-plan/nexa-plan/260824-2043-bd-screens-common-first.md` dòng 44. Đối chiếu
> prototype: `09-layoutBase/Câu hỏi phỏng vấn.dc.html`. File này mô tả **hành vi và UX ở mức yêu cầu** —
> không lặp lại đặc tả chức năng đã có ở `01-rd/req/interview-bank.md` (mục F6) và `01-rd/req/user_stories/a1_student.md`
> (`US-A1-08`), chỉ trỏ tới và bổ sung phần đặc thù của màn.
>
> **Ghi chú liên phase:** báo cáo `06-plan/reports/260825-2100-ai2-solution-review-conflicts.md` (Phase 3)
> đã chốt rằng `solution_review` sẽ điều hướng sang màn này kèm tham số lọc theo chủ đề/từ khoá (query
> string) — xem mục 5 Câu hỏi mở Q3 dưới đây.

## 1. Mục đích màn hình

Trang duyệt **toàn bộ kho câu hỏi phỏng vấn lý thuyết** (độc lập với bài nộp code): tìm kiếm, lọc theo chủ
đề và trạng thái ôn tập, xem nhanh gợi ý trả lời của một câu hỏi, tự chấm mức độ nhớ, và một chế độ "luyện
nhanh" dạng flashcard [SoT: 01-rd/req/user_stories/a1_student.md — `US-A1-08`;
01-rd/overview/system_survey.md:481].

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Danh sách câu hỏi phân loại theo chủ đề, mức độ khó; tìm kiếm và lọc; đánh dấu để xem lại | F6-01, F6-02, F6-03 | `01-rd/req/interview-bank.md` — F6-01, F6-02, F6-03 |
| Chế độ học: gợi ý hướng tiếp cận, khung trả lời chuẩn (STAR cho câu hành vi), từ khoá cốt lõi | F6-04, F6-05, F6-06 | `01-rd/req/interview-bank.md` — F6-04, F6-05, F6-06 |
| Lịch sử luyện tập, câu cần ôn lại, tỉ lệ hoàn thành theo chủ đề | F6-09, F6-10 | `01-rd/req/interview-bank.md` — F6-09, F6-10 |
| Tự chấm mức độ thuộc bài, hệ thống tự xếp lịch ôn lại (spaced repetition) | F6-12 | `01-rd/req/interview-bank.md` — F6-12 |
| Given-When-Then đầy đủ cho luồng ôn tập ngân hàng câu hỏi | — | `01-rd/req/user_stories/a1_student.md` (`US-A1-08`) |

## 3. Trạng thái và cấu trúc màn (screen states)

Đối chiếu `09-layoutBase/Câu hỏi phỏng vấn.dc.html`, màn có hai trạng thái (`isBrowse`/`isDrill`):

### 3.1. Trạng thái `browse` (mặc định) — bố cục hai cột: danh sách + xem nhanh

1. **Thanh chỉ số** — Tổng câu hỏi, Đã thuộc, Đang ôn, Ôn trong tuần [SoT:
   09-layoutBase/Câu hỏi phỏng vấn.dc.html:99-107, 373-378] — khớp F6-09/F6-10, tổng hợp từ trạng thái tự
   chấm (F6-12).
2. **Hai nút hành động đầu trang** — "Luyện nhanh N câu" (vào trạng thái `drill`) và "Vào phiên giả lập"
   (dẫn sang `mock_interview`) [SoT: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:108-111].
3. **Cột trái — danh sách câu hỏi**: ô tìm kiếm theo tiêu đề/tag (F6-02), tab lọc theo trạng thái (Tất
   cả/Chưa xem/Đang ôn/Đã thuộc — trạng thái do người dùng tự chấm ở F6-12), chip lọc theo chủ đề (Lý thuyết
   CS/System design/Database/Ngôn ngữ/Hành vi) [SoT: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:117-146]. Mỗi
   dòng câu hỏi hiện chủ đề, độ khó, nhãn trạng thái, tiêu đề; bấm vào để chọn xem nhanh ở cột phải [SoT:
   09-layoutBase/Câu hỏi phỏng vấn.dc.html:132-141].
4. **Cột phải — khối "xem nhanh" của câu hỏi đang chọn**: tiêu đề đầy đủ, "Ý cần nói" (danh sách gạch đầu
   dòng đánh số), "Bẫy thường gặp", tag liên quan, và khối "Mức độ nắm" để tự chấm trực tiếp (Chưa xem/Đang
   ôn/Đã thuộc) cùng nút "Thêm vào phiên giả lập tới" [SoT:
   09-layoutBase/Câu hỏi phỏng vấn.dc.html:148-188]. **Đây là điểm cần chốt cấu trúc màn** — xem Câu hỏi mở
   Q1 đã chốt bên dưới: khối này chỉ là **bản xem nhanh rút gọn**, không phải toàn bộ `interview_question_detail`.

### 3.2. Trạng thái `drill` (luyện nhanh dạng flashcard)

5. **Thanh tiến độ** (`drillCounter`/`drillPct`) và nút "Thoát" quay về `browse` [SoT:
   09-layoutBase/Câu hỏi phỏng vấn.dc.html:195-201].
6. **Thẻ câu hỏi** — hiện tiêu đề căn giữa; bấm "Xem ý cần nói" thì lộ ra danh sách gợi ý (giống cột phải ở
   `browse`, không có "Bẫy thường gặp") [SoT: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:202-219].
7. **Hai nút chấm nhanh** — "Cần ôn lại" và "Đã thuộc", mỗi lần bấm ghi nhận trạng thái tự chấm (F6-12) rồi
   chuyển sang thẻ kế tiếp [SoT: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:220-226, 434-435]. Chế độ này chính
   là hiện thực hoá trực quan nhất của F6-12 trong toàn bộ prototype.

Hai chế độ hiển thị không đổi hành vi nghiệp vụ, chỉ đổi văn bản/field: `data-ui-lang="vi|en"` và
`data-theme="light|dark"`, theo đúng `DEC-2026-0824-i18n-vi-en` và `DEC-2026-0824-dark-light-theme`.

## 4. Given-When-Then bổ sung ở mức màn (không trùng `US-A1-08`)

- **Cho** tôi mở màn này kèm tham số chủ đề/từ khoá trên URL (điều hướng từ `solution_review`, F5-06 đã
  chốt ở Phase 3), **Khi** trang tải xong, **Thì** bộ lọc chủ đề/tìm kiếm áp sẵn đúng chủ đề/từ khoá đó,
  không bắt tôi tự gõ lại — cơ chế truyền tham số cụ thể **[Đợi nextjs]** [SoT:
  06-plan/reports/260825-2100-ai2-solution-review-conflicts.md].
- **Cho** tôi bấm "Luyện nhanh N câu", **Khi** trạng thái chuyển sang `drill`, **Thì** bộ câu hỏi trong phiên
  luyện nhanh lấy từ đúng bộ lọc tôi đang áp ở `browse` (chủ đề/trạng thái) hay luôn lấy ngẫu nhiên toàn bộ
  148 câu — **chưa xác nhận được từ prototype** [SoT: Suy luận — `drill()` trong mã nguồn mẫu chỉ lặp tuần
  tự qua toàn bộ mảng `Q`, không lọc theo state hiện tại của `browse`, 09-layoutBase/Câu hỏi phỏng
  vấn.dc.html:357, 422]. Xem Câu hỏi mở Q2.
- **Cho** tôi đã tự chấm một câu là "Đã thuộc", **Khi** tôi mở lại chế độ luyện nhanh sau đó, **Thì** hệ
  thống ưu tiên hiện lại các câu "Cần ôn lại"/"Chưa xem" trước, đúng tinh thần spaced-repetition của F6-12 —
  công thức xếp lịch cụ thể thuộc DD [SoT: 01-rd/req/interview-bank.md — F6-12].

## 5. Câu hỏi mở

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | ~~Khối "xem nhanh" ở cột phải của `browse` đã hiện gần như đầy đủ nội dung Chế độ học (gợi ý hướng tiếp cận, tag) — vậy `interview_question_detail` là một trang riêng hay `interview_bank_list` đã đủ, không cần trang chi tiết riêng?~~ **ĐÃ CHỐT (2026-08-25, tự quyết theo yêu cầu chủ dự án — prototype là bản dựng tham khảo)**: `interview_bank_list` chỉ giữ bản **xem nhanh rút gọn** (đúng như prototype hiện có) — đủ để lướt nhanh và tự chấm mà không rời trang danh sách. `interview_question_detail` là **trang riêng, đầy đủ hơn**: Chế độ học đầy đủ (bao gồm khung trả lời chuẩn dạng STAR cho câu hành vi — F6-05 — hiện **hoàn toàn chưa có** ở bản xem nhanh) và Chế độ luyện (F6-07/F6-08, soạn câu trả lời + AI chấm — **hoàn toàn chưa được dựng ở bất kỳ đâu trong prototype**). Xem file RD riêng `01-rd/screens/users/interview_question_detail.md`. | — | Đã chốt cấu trúc 2 màn; nội dung `interview_question_detail` **[Đợi nextjs]** vì Chế độ luyện chưa có bản dựng nào để đối chiếu. | Đã đóng |
| Q2 | Chế độ "Luyện nhanh" có lấy đúng theo bộ lọc đang áp ở `browse` (chủ đề, trạng thái) hay luôn duyệt toàn bộ 148 câu theo thứ tự cố định? Mã nguồn mẫu cho thấy trạng thái thứ hai, có thể chỉ là giản lược của bản dựng tham khảo. | Không có tài liệu `interview-bank.md`/`user_stories/a1_student.md` nào mô tả chi tiết luồng chọn bộ câu hỏi cho "luyện nhanh". | **ĐÃ CHỐT (2026-08-25, tự quyết)**: hợp lý hơn cho người dùng là luyện nhanh lấy theo đúng bộ lọc đang áp ở `browse` (nếu không lọc gì thì mới lấy toàn bộ) — tránh người dùng bấm "luyện nhanh" sau khi đã lọc theo một chủ đề cụ thể mà lại nhận câu hỏi ở chủ đề khác. Cài đặt cụ thể **[Đợi nextjs]**. | Đợi nextjs |
| Q3 | Tham số lọc theo chủ đề/từ khoá khi điều hướng từ `solution_review` (F5-06, Phase 3) cần đi qua query string dạng gì (một chủ đề hay nhiều, khớp chính xác tên chủ đề hay tìm kiếm mờ)? | Đây là điểm nối giữa hai module (`ai-review` → `interview-bank`) qua tầng giao diện (không qua đọc dữ liệu chéo module ở backend, chỉ là điều hướng URL) — chưa có DD nào của cả hai module để chốt định dạng chính xác. | **[Đợi nextjs]** — chốt khi viết DD cho `interview-bank` (API danh sách câu hỏi cần hỗ trợ lọc theo chủ đề qua tham số truy vấn). Không cần chủ dự án quyết ngay vì không đổi kiến trúc, chỉ là hình dạng một query param. | Đợi nextjs |

## 6. Ngoài phạm vi file này

- Bảng màu, spacing, component cụ thể, breakpoint responsive — thuộc BD
  (`02-bd/screens/users/interview_bank_list.md`, chưa viết).
- Hợp đồng API (danh sách câu hỏi có lọc/tìm kiếm, ghi nhận tự chấm) — thuộc DD
  (`03-dd/api/interview-bank.md`, chưa viết).
- Thuật toán xếp lịch ôn lại (spaced repetition, F6-12) — thuộc logic của `interview-bank`, không thuộc file
  theo trục màn này.
- Nội dung Chế độ học đầy đủ và Chế độ luyện (F6-04 tới F6-08) — thuộc `01-rd/screens/users/interview_question_detail.md`
  (file RD riêng, xem Câu hỏi mở Q1).
- Nội dung và bố cục của `mock_interview` (đích của nút "Vào phiên giả lập") — thuộc file RD riêng của nó.

## 7. Tham chiếu

- `01-rd/req/interview-bank.md` — F6-01 tới F6-12.
- `01-rd/req/user_stories/a1_student.md` — `US-A1-08`.
- `01-rd/overview/system_survey.md:481` — dòng `interview_bank_list` trong bảng màn mục 7.1.
- `06-plan/reports/260825-2100-ai2-solution-review-conflicts.md` — điểm nối từ `solution_review` (Phase 3).
- `09-layoutBase/Câu hỏi phỏng vấn.dc.html` — prototype.
- `06-plan/nexa-plan/260824-2043-bd-screens-common-first.md` — kế hoạch Phase 4 sinh ra file này.
