# RD (Yêu cầu hệ thống mới) — Ngân hàng câu hỏi / `USR0401`

> Mã màn hình: `USR0401` [Nguồn: 02-bd/_rules/bd-template-9sheet.md — mục 8].
> Slug chính tắc: `interview_bank_list` [SoT: 01-rd/overview/system_survey.md — mục 7.1 dòng `interview_bank_list`].
> Phạm vi/Bounded Context: `interview-bank` (F6). Actor: A1 (chính).
> Nguồn sự thật (SoT): `01-rd/req/interview-bank.md` (F6-01 đến F6-12), `01-rd/req/user_stories/a1_student.md`
> (`US-A1-08`), `09-layoutBase/Câu hỏi phỏng vấn.dc.html` (prototype).
> Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

Trang duyệt **toàn bộ kho câu hỏi phỏng vấn lý thuyết** (độc lập với bài nộp code): tìm kiếm, lọc theo chủ
đề và trạng thái ôn tập, xem nhanh gợi ý trả lời của một câu hỏi, tự chấm mức độ nhớ, và một chế độ "luyện
nhanh" dạng flashcard [SoT: 01-rd/req/user_stories/a1_student.md — `US-A1-08`;
01-rd/overview/system_survey.md — mục 7.1 dòng `interview_bank_list`].

Theo lộ trình Phase 4 của `06-plan/nexa-plan/260824-2043-bd-screens-common-first.md` dòng 44. Đối chiếu
prototype: `09-layoutBase/Câu hỏi phỏng vấn.dc.html`. File này mô tả **hành vi và UX ở mức yêu cầu** — không
lặp lại đặc tả chức năng đã có ở `01-rd/req/interview-bank.md` (mục F6) và `01-rd/req/user_stories/a1_student.md`
(`US-A1-08`), chỉ trỏ tới và bổ sung phần đặc thù của màn.

**Ghi chú liên phase:** báo cáo `06-plan/reports/260825-2100-ai2-solution-review-conflicts.md` (Phase 3) đã
chốt rằng `solution_review` sẽ điều hướng sang màn này kèm tham số lọc theo chủ đề/từ khoá (query string) —
xem mục 4 Tiền đề và ràng buộc, Câu hỏi mở Q3 dưới đây.

---

## 2. Phạm vi

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Danh sách câu hỏi phân loại theo chủ đề, mức độ khó; tìm kiếm và lọc; đánh dấu để xem lại | F6-01, F6-02, F6-03 | `01-rd/req/interview-bank.md` — F6-01, F6-02, F6-03 |
| Chế độ học: gợi ý hướng tiếp cận, khung trả lời chuẩn (STAR cho câu hành vi), từ khoá cốt lõi | F6-04, F6-05, F6-06 | `01-rd/req/interview-bank.md` — F6-04, F6-05, F6-06 |
| Lịch sử luyện tập, câu đến hạn ôn, tỉ lệ hoàn thành theo chủ đề | F6-09, F6-10 | `01-rd/req/interview-bank.md` — F6-09, F6-10. Từ 2026-10-18 màn này không còn thanh chỉ số (xem ghi chú đầu mục "Trạng thái `browse`" bên dưới); "câu cần ôn lại" còn qua tab lọc "Đang ôn" và hàng đợi Luyện nhanh, lịch sử các lượt ở `USR0402`; **tỉ lệ hoàn thành theo chủ đề (F6-10) do dashboard `USR0601` nhận** (owner chốt 2026-10-18, xem mục 10) — ghi chú đóng ở `02-bd/screens/users/USR0401_interview_bank_list.md` Câu hỏi mở Q11 |
| Tự chấm mức độ thuộc bài, hệ thống tự xếp lịch ôn lại (spaced repetition) | F6-12 | `01-rd/req/interview-bank.md` — F6-12 |
| Given-When-Then đầy đủ cho luồng ôn tập ngân hàng câu hỏi | — | `01-rd/req/user_stories/a1_student.md` (`US-A1-08`) |

Đối chiếu `09-layoutBase/Câu hỏi phỏng vấn.dc.html`, màn có hai trạng thái (`isBrowse`/`isDrill`):

**Trạng thái `browse` (mặc định) — bố cục hai cột: danh sách + xem nhanh**

> **Đã chốt 2026-10-18 (owner), `DEC-2026-1018-usr-open-questions-phase1-scope` mục (1):** prototype có
> "Thanh chỉ số" 4 ô (Tổng câu hỏi, Đã thuộc, Đang ôn, Ôn trong tuần, `09-layoutBase/Câu hỏi phỏng vấn.dc.html:99-107,
> 373-378`) — **bỏ khỏi yêu cầu**, theo quy tắc owner 2026-10-13 và 2026-10-18 (không dải thẻ chỉ số ở màn
> danh sách). Người học vẫn thấy trạng thái tự chấm qua nhãn trạng thái trên từng dòng và menu lọc trạng thái
> (khoản 2 dưới đây). Ponytail bậc 1 (không nguồn yêu cầu nào đòi một dải số riêng trên màn này).

1. **Hai nút hành động đầu trang** — "Luyện nhanh N câu" (vào trạng thái `drill`) và "Vào phiên giả lập"
   (dẫn sang `mock_interview`) [SoT: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:108-111]. Nút "Vào phiên giả
   lập" ở đầu trang **giữ nguyên**; nút "Thêm vào phiên giả lập tới" ở cột phải **không có** (khoản 3).
2. **Cột trái — danh sách câu hỏi**: ô tìm kiếm theo tiêu đề/từ khoá cốt lõi (F6-02), menu thả xuống lọc theo trạng thái (Tất
   cả/Chưa xem/Đang ôn/Đã thuộc — trạng thái do người dùng tự chấm ở F6-12), menu thả xuống lọc theo chủ đề (Lý thuyết
   CS/System design/Database/Ngôn ngữ/Hành vi) [SoT: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:117-146]. Mỗi
   dòng câu hỏi hiện chủ đề, độ khó, nhãn trạng thái, tiêu đề (**không** hiện mã dạng `IQ-nnn`); bấm vào để
   chọn xem nhanh ở cột phải [SoT: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:132-141].
3. **Cột phải — khối "xem nhanh" của câu hỏi đang chọn**: tiêu đề đầy đủ, "Ý cần nói" (danh sách gạch đầu
   dòng đánh số; "Bẫy thường gặp" nếu có là một mục tiêu đề nằm trong chính nội dung này, không phải khối riêng), từ khoá cốt lõi (`core_keywords`, thay cho "tag" của prototype — đã chốt
   2026-10-18, owner, không thêm bảng tag), và khối "Mức độ nắm" để tự chấm trực tiếp [SoT:
   09-layoutBase/Câu hỏi phỏng vấn.dc.html:148-188]. **Không có** nút "Thêm vào phiên giả lập tới" (đã chốt
   2026-10-18, owner: bỏ, ponytail bậc 1, 2 — không mã `Fx-nn`, không bảng lưu hàng đợi). **Đây là điểm cần chốt cấu trúc màn** — xem Câu hỏi mở Q1 đã chốt bên dưới: khối này chỉ là **bản xem nhanh
   rút gọn**, không phải toàn bộ `interview_question_detail`.

**Trạng thái `drill` (luyện nhanh dạng flashcard)**

5. **Thanh tiến độ** (`drillCounter`/`drillPct`) và nút "Thoát" quay về `browse` [SoT:
   09-layoutBase/Câu hỏi phỏng vấn.dc.html:195-201].
6. **Thẻ câu hỏi** — hiện tiêu đề căn giữa; bấm "Xem ý cần nói" thì lộ ra danh sách gợi ý (giống cột phải ở
   `browse`, không có "Bẫy thường gặp") [SoT: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:202-219].
7. **Ba nút chấm nhanh** — "Biết rõ", "Mơ hồ", "Quên" (ba mức F6-12, người học tự chọn, không qua AI; đã chốt
   2026-10-18, owner, thay hai nút "Cần ôn lại"/"Đã thuộc" của prototype), mỗi lần bấm ghi nhận mức tự chấm rồi
   chuyển sang thẻ kế tiếp [SoT: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:220-226, 434-435]. Chế độ này chính
   là hiện thực hoá trực quan nhất của F6-12 trong toàn bộ prototype.

Hai chế độ hiển thị không đổi hành vi nghiệp vụ, chỉ đổi văn bản/field: `data-ui-lang="vi|en"` và
`data-theme="light|dark"`, theo đúng `DEC-2026-0824-i18n-vi-en` và `DEC-2026-0824-dark-light-theme`.

### 2.1 Cấu trúc hệ thống liên quan

| Màn/tài liệu | Vai trò | Bounded Context |
|---|---|---|
| Màn hiện tại | `USR0401` / `interview_bank_list` | `interview-bank` |
| Tài liệu yêu cầu | `01-rd/req/interview-bank.md` (F6) | Nguồn SoT của màn |
| Prototype | `09-layoutBase/Câu hỏi phỏng vấn.dc.html` | Bằng chứng bố cục và trạng thái |

**Chi tiết trạng thái và cấu trúc màn**

| Thành phần | Vai trò | Nguồn |
| :--- | :--- | :--- |
| `01-rd/screens/users/USR0402_interview_question_detail.md` | Trang chi tiết đầy đủ hơn Chế độ học/Chế độ luyện cho một câu hỏi cụ thể | [SoT: 01-rd/screens/users/USR0401_interview_bank_list.md — mục 4, Câu hỏi mở Q1] |
| `01-rd/screens/users/USR0302_mock_interview.md` | Đích của nút "Vào phiên giả lập" ở đầu trang | [SoT: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:108-111] |
| `01-rd/screens/users/USR0301_solution_review.md` | Điều hướng sang màn này kèm tham số lọc theo chủ đề/từ khoá (F5-06, đã chốt Phase 3) | [SoT: 06-plan/reports/260825-2100-ai2-solution-review-conflicts.md] |
| `09-layoutBase/Câu hỏi phỏng vấn.dc.html` | Prototype đối chiếu bố cục và trạng thái màn | [SoT: 09-layoutBase/Câu hỏi phỏng vấn.dc.html] |
| Bounded Context `interview-bank` (F6) | BC sở hữu dữ liệu và logic đứng sau màn | [SoT: 01-rd/overview/system_survey.md — mục 7.1] |

---

## 3. Ngoài phạm vi (Out of Scope)

- Bảng màu, spacing, component cụ thể, breakpoint responsive — thuộc BD
  (`02-bd/screens/users/USR0401_interview_bank_list.md`, chưa viết).
- Hợp đồng API (danh sách câu hỏi có lọc/tìm kiếm, ghi nhận tự chấm) — thuộc DD
  (`03-dd/api/interview-bank.md`, chưa viết).
- Thuật toán xếp lịch ôn lại (spaced repetition, F6-12) — thuộc logic của `interview-bank`, không thuộc file
  theo trục màn này.
- Nội dung Chế độ học đầy đủ và Chế độ luyện (F6-04 tới F6-08) — thuộc `01-rd/screens/users/USR0402_interview_question_detail.md`
  (file RD riêng, xem Câu hỏi mở Q1).
- Nội dung và bố cục của `mock_interview` (đích của nút "Vào phiên giả lập") — thuộc file RD riêng của nó.

---

## 4. Tiền đề và ràng buộc

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | ~~Khối "xem nhanh" ở cột phải của `browse` đã hiện gần như đầy đủ nội dung Chế độ học (gợi ý hướng tiếp cận, tag) — vậy `interview_question_detail` là một trang riêng hay `interview_bank_list` đã đủ, không cần trang chi tiết riêng?~~ **ĐÃ CHỐT (2026-08-25, tự quyết theo yêu cầu chủ dự án — prototype là bản dựng tham khảo)**: `interview_bank_list` chỉ giữ bản **xem nhanh rút gọn** (đúng như prototype hiện có) — đủ để lướt nhanh và tự chấm mà không rời trang danh sách. `interview_question_detail` là **trang riêng, đầy đủ hơn**: Chế độ học đầy đủ (bao gồm khung trả lời chuẩn dạng STAR cho câu hành vi — F6-05 — hiện **hoàn toàn chưa có** ở bản xem nhanh) và Chế độ luyện (F6-07/F6-08, soạn câu trả lời + AI chấm — **hoàn toàn chưa được dựng ở bất kỳ đâu trong prototype**). Xem file RD riêng `01-rd/screens/users/USR0402_interview_question_detail.md`. | — | Đã chốt cấu trúc 2 màn; nội dung `interview_question_detail` **[Đợi nextjs]** vì Chế độ luyện chưa có bản dựng nào để đối chiếu. | Đã đóng |
| Q2 | Chế độ "Luyện nhanh" có lấy đúng theo bộ lọc đang áp ở `browse` (chủ đề, trạng thái) hay luôn duyệt toàn bộ 148 câu theo thứ tự cố định? Mã nguồn mẫu cho thấy trạng thái thứ hai, có thể chỉ là giản lược của bản dựng tham khảo. | Không có tài liệu `interview-bank.md`/`user_stories/a1_student.md` nào mô tả chi tiết luồng chọn bộ câu hỏi cho "luyện nhanh". | **ĐÃ CHỐT (2026-08-25, tự quyết)**: hợp lý hơn cho người dùng là luyện nhanh lấy theo đúng bộ lọc đang áp ở `browse` (nếu không lọc gì thì mới lấy toàn bộ) — tránh người dùng bấm "luyện nhanh" sau khi đã lọc theo một chủ đề cụ thể mà lại nhận câu hỏi ở chủ đề khác. Cài đặt cụ thể **[Đợi nextjs]**. | Đợi nextjs |
| Q3 | **[Chốt khi viết DD, cập nhật 2026-10-19: tham số lọc trên URL còn là nợ prototype (ponytail bậc 3)]** Tham số lọc theo chủ đề/từ khoá khi điều hướng từ `solution_review` (F5-06, Phase 3) cần đi qua query string dạng gì (một chủ đề hay nhiều, khớp chính xác tên chủ đề hay tìm kiếm mờ)? | Đây là điểm nối giữa hai module (`ai-review` → `interview-bank`) qua tầng giao diện (không qua đọc dữ liệu chéo module ở backend, chỉ là điều hướng URL) — chưa có DD nào của cả hai module để chốt định dạng chính xác. | **[Đợi nextjs]** — chốt khi viết DD cho `interview-bank` (API danh sách câu hỏi cần hỗ trợ lọc theo chủ đề qua tham số truy vấn). Không cần chủ dự án quyết ngay vì không đổi kiến trúc, chỉ là hình dạng một query param. | Đợi nextjs |

---

## 5. Danh sách yêu cầu (REQ)

| ID | Yêu cầu | Loại | Nguồn/SoT |
|---|---|---|---|
| REQ-01 | Danh sách câu hỏi phân loại theo chủ đề, mức độ khó; tìm kiếm và lọc; đánh dấu để xem lại (F6-01, F6-02, F6-03) | Chức năng | `01-rd/req/interview-bank.md` — F6-01, F6-02, F6-03 |
| REQ-02 | Chế độ học: gợi ý hướng tiếp cận, khung trả lời chuẩn (STAR cho câu hành vi), từ khoá cốt lõi (F6-04, F6-05, F6-06) | Chức năng | `01-rd/req/interview-bank.md` — F6-04, F6-05, F6-06 |
| REQ-03 | Lịch sử luyện tập, câu đến hạn ôn, tỉ lệ hoàn thành theo chủ đề (F6-09, F6-10) | Chức năng | `01-rd/req/interview-bank.md` — F6-09, F6-10 |
| REQ-04 | Tự chấm mức độ thuộc bài, hệ thống tự xếp lịch ôn lại (spaced repetition) (F6-12) | Chức năng | `01-rd/req/interview-bank.md` — F6-12 |
| REQ-05 | Given-When-Then đầy đủ cho luồng ôn tập ngân hàng câu hỏi (—) | Chức năng | `01-rd/req/user_stories/a1_student.md` (`US-A1-08`) |
| REQ-06 | **Cho** tôi mở màn này kèm tham số chủ đề/từ khoá trên URL (điều hướng từ `solution_review`, F5-06 đã chốt ở Phase 3), **Khi** trang tải xong, **Thì** bộ lọc chủ đề/tìm kiếm áp sẵn đúng chủ đề/từ khoá đó, không bắt tôi tự gõ lại — cơ chế truyền tham số cụ thể **[Đợi nextjs]** | Chức năng (GWT) | [SoT: 06-plan/reports/260825-2100-ai2-solution-review-conflicts.md] |
| REQ-07 | **Cho** tôi bấm "Luyện nhanh N câu", **Khi** trạng thái chuyển sang `drill`, **Thì** bộ câu hỏi trong phiên luyện nhanh lấy từ đúng bộ lọc tôi đang áp ở `browse` (chủ đề/trạng thái) hay luôn lấy ngẫu nhiên toàn bộ 148 câu — chưa xác nhận được từ prototype | Chức năng (GWT) | [SoT: Suy luận — `drill()` trong mã nguồn mẫu chỉ lặp tuần tự qua toàn bộ mảng `Q`, không lọc theo state hiện tại của `browse`, 09-layoutBase/Câu hỏi phỏng vấn.dc.html:357, 422]. Xem mục 4, Câu hỏi mở Q2. |
| REQ-08 | **Cho** tôi đã tự chấm một câu là "Đã thuộc", **Khi** tôi mở lại chế độ luyện nhanh sau đó, **Thì** hệ thống ưu tiên hiện lại các câu **đến hạn** (`next_review_at <= now()`) trước, rồi tới các câu **chưa xem** (chưa tự chấm lần nào), đúng tinh thần spaced-repetition của F6-12 — công thức xếp lịch cụ thể thuộc DD. **Sửa 2026-10-22** (mặc định chủ dự án xác nhận 2026-10-22, `DEC-2026-1022-interview-bank-schema-sync` mục 2; `02-bd/database/interview-bank.md:194-201`): ~~"Cần ôn lại"/"Chưa xem"~~; "đến hạn" khác tab **"đang ôn"** (lần chấm gần nhất `VAGUE`/`FORGOTTEN`), hai tập giao nhau nhưng không chứa nhau. **Sửa 2026-10-23** (`DEC-2026-1023-db-actor-review-fixes`; định nghĩa gốc `02-bd/database/interview-bank.md` mục 1.7, dòng 196-201): "đến hạn" = hàng đợi Luyện nhanh và danh sách F6-09, mọi câu có `next_review_at <= now()` bất kể mức tự chấm; "đang ôn" = chỉ là tab và nhãn trạng thái ở danh sách | Chức năng (GWT) | [SoT: 01-rd/req/interview-bank.md — F6-12] |
| REQ-09 | **Cho** tôi vừa tự chấm mức độ nhớ một câu, **Khi** hệ thống ghi nhận xong, **Thì** ngay dưới khối tự chấm tôi thấy một dòng ngắn "Sẽ nhắc ôn lại sau {n} ngày" (n lấy từ lịch ôn lại của F6-12) | Chức năng (GWT) | [SoT: owner 2026-10-18, `DEC-2026-1018-usr-open-questions-phase2-5`; 01-rd/req/interview-bank.md — F6-12] |
| REQ-10 | **Cho** tôi đã đánh dấu một số câu (F6-03), **Khi** tôi bật công tắc "Chỉ câu đã đánh dấu" cạnh menu lọc trạng thái, **Thì** danh sách chỉ còn các câu đã đánh dấu, kết hợp được với tìm kiếm, trạng thái, chủ đề và độ khó (đây là một trục lọc độc lập, không phải một lựa chọn thứ năm của menu trạng thái) | Chức năng (GWT) | [SoT: owner 2026-10-18, `DEC-2026-1018-usr-open-questions-phase2-5`; 01-rd/req/interview-bank.md — F6-03] |

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
| :--- | :--- |
| Yêu cầu module | `01-rd/req/interview-bank.md` — F6-01 tới F6-12. |
| User story | `01-rd/req/user_stories/a1_student.md` — `US-A1-08`. |
| Khảo sát hệ thống | `01-rd/overview/system_survey.md` — mục 7.1 dòng `interview_bank_list`. |
| Báo cáo liên quan | `06-plan/reports/260825-2100-ai2-solution-review-conflicts.md` — điểm nối từ `solution_review` (Phase 3). |
| Prototype | `09-layoutBase/Câu hỏi phỏng vấn.dc.html`. |
| Kế hoạch | `06-plan/nexa-plan/260824-2043-bd-screens-common-first.md` — kế hoạch Phase 4 sinh ra file này. |

---

## 10. Ghi nhận quyết định 2026-10-18 (đợt phase 2 tới 5)

Owner chốt theo đề xuất, ghi ở `DEC-2026-1018-usr-open-questions-phase2-5`. Bảng dưới liệt kê phần quyết định chạm RD của màn này; chi tiết thiết kế ở `02-bd/screens/users/USR0401_interview_bank_list.md` (Câu hỏi mở Q7 đến Q11).

| Nội dung | Thay đổi ở RD | Tiêu chí ponytail |
| :--- | :--- | :-: |
| Ngày ôn lại kế tiếp sau khi tự chấm | Thêm REQ-09: dòng "Sẽ nhắc ôn lại sau {n} ngày" dưới khối tự chấm | Bậc 3 |
| Lọc câu đã đánh dấu | Thêm REQ-10: công tắc "Chỉ câu đã đánh dấu", trục lọc độc lập với bốn tab trạng thái | Bậc 3 |
| Số thẻ luyện nhanh | Nhãn "Luyện nhanh N câu" (mục 2, khoản 1): N cố định 10, hoặc ít hơn khi bộ lọc khớp ít câu hơn; không có điều khiển chọn số thẻ | Bậc 5 |
| Bộ lọc độ khó trên URL | Không đổi yêu cầu: tham số `level` do BD chốt cùng `topic`, `q`, `status`; REQ-06 giữ nguyên vì `solution_review` không gửi `level` | Bậc 4 |
| Nhãn ba nút tự chấm (mục 2 khoản 3 và 7) | Ba nút là ba mức F6-12 Biết rõ / Mơ hồ / Quên do người học chọn, không có AI; "Chưa xem" chỉ là giá trị bộ lọc (không có dòng tự chấm), không phải nút; luyện nhanh dùng đủ ba nút (đã chốt 2026-10-18, `DEC-2026-1018-usr-open-questions-last-four`) | Bậc 1 |
| "Bẫy thường gặp" (mục 2 khoản 3) | Là một mục trong `suggested_approach` (Markdown), không cột riêng, không khối riêng ở BD (`DEC-2026-1018-usr-open-questions-last-four`) | Bậc 4, 5 |
| F6-10 (tỉ lệ hoàn thành theo chủ đề) | Do dashboard `USR0601` nhận, không thuộc màn này (mục 2, dòng F6-09, F6-10) | Bậc 4 |
