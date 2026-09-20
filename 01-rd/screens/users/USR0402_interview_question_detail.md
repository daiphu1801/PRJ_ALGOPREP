# RD (Yêu cầu hệ thống mới) — Chi tiết câu hỏi / `USR0402`

> Mã màn hình: `USR0402` [Nguồn: 02-bd/_rules/bd-template-9sheet.md — mục 8].
> Slug chính tắc: `interview_question_detail` [SoT: 01-rd/overview/system_survey.md — mục 7.1 dòng `interview_question_detail`].
> Phạm vi/Bounded Context: `interview-bank` (F6), phần Chế độ luyện (F6-07/F6-08) đi qua `ai-review`
> [SoT: 01-rd/req/interview-bank.md — F6-07, F6-08]. Actor: A1 (chính).
> Nguồn sự thật (SoT): `01-rd/req/interview-bank.md` (F6-01 đến F6-12), `01-rd/req/ai-review.md` (F5-17 đến
> F5-22), `01-rd/req/user_stories/a1_student.md` (`US-A1-08`).
> Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

Trang chi tiết của **một câu hỏi phỏng vấn cụ thể**, đầy đủ hai chế độ: **Chế độ học** (`STUDY`, xem gợi ý
và khung trả lời chuẩn) và **Chế độ luyện** (`PRACTICE`, tự soạn câu trả lời và nhận phản hồi từ AI) [SoT:
01-rd/req/user_stories/a1_student.md — `US-A1-08`; 01-rd/req/interview-bank.md — F6-04 tới F6-08].

Theo lộ trình Phase 4 của `06-plan/nexa-plan/260824-2043-bd-screens-common-first.md` dòng 44.

**Khác với ba file RD trước của Phase 3/4, màn này KHÔNG có prototype riêng.** `09-layoutBase/Câu hỏi
phỏng vấn.dc.html` chỉ dựng một khối "xem nhanh" rút gọn ngay trong `interview_bank_list` (xem
`01-rd/screens/users/USR0401_interview_bank_list.md` mục 2, hạng mục 4 của trạng thái `browse`, và mục 4
Câu hỏi mở Q1 — đã chốt 2026-08-25: hai màn tách riêng, `interview_question_detail` là trang đầy đủ hơn).
Nội dung dưới đây suy ra trực tiếp từ `interview-bank.md`/`user_stories/a1_student.md` (F6-04 tới F6-08) và
từ đúng phần dữ liệu đã có trong khối "xem nhanh", **không có UI thật nào để đối chiếu Chế độ luyện** —
đánh dấu rõ từng phần **[Đợi nextjs]**.

---

## 2. Phạm vi

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Chế độ học: gợi ý hướng tiếp cận | F6-04 | `01-rd/req/interview-bank.md` — F6-04 |
| Chế độ học: khung trả lời chuẩn, áp dụng STAR cho câu hành vi | F6-05 | `01-rd/req/interview-bank.md` — F6-05 |
| Chế độ học: danh sách từ khoá kỹ thuật cốt lõi | F6-06 | `01-rd/req/interview-bank.md` — F6-06 |
| Chế độ luyện: người dùng tự soạn câu trả lời | F6-07 | `01-rd/req/interview-bank.md` — F6-07 |
| Chế độ luyện: AI đối chiếu tiêu chí chuẩn, trả điểm đã đạt/điểm còn thiếu/hướng bổ sung | F6-08 | `01-rd/req/interview-bank.md` — F6-08 |
| Đánh dấu để xem lại; tự chấm mức độ thuộc bài | F6-03, F6-12 | `01-rd/req/interview-bank.md` — F6-03, F6-12 |
| Chống prompt injection, rate limit — Chế độ luyện đi qua phân hệ AI, chịu chung ràng buộc F5 | F5-17 tới F5-22 | `01-rd/req/ai-review.md` — F5-17 tới F5-22; `01-rd/req/interview-bank.md` — F6-08 |
| Given-When-Then Chế độ học/luyện | — | `01-rd/req/user_stories/a1_student.md` (`US-A1-08`) |

**Phần chung cho cả hai chế độ — có prototype tham khảo (khối "xem nhanh")**

1. **Đầu trang câu hỏi** — chủ đề, độ khó, tần suất xuất hiện, tiêu đề đầy đủ, tag liên quan [SoT:
   09-layoutBase/Câu hỏi phỏng vấn.dc.html:148-156, 174-178, 406-412] — tái dùng nguyên từ khối "xem nhanh"
   của `interview_bank_list`, không đổi.
2. **Toggle Chế độ học / Chế độ luyện** — **chưa có trong prototype**, tự thêm khi dựng UI thật
   **[Đợi nextjs]**. Đề xuất: tab hai lựa chọn ngay dưới đầu trang câu hỏi, mặc định mở ở Chế độ học.

**Chế độ học (`STUDY`) — một phần có prototype, một phần chưa**

3. **"Ý cần nói"** (gợi ý hướng tiếp cận, F6-04) — đã có nguyên trong khối "xem nhanh", danh sách gạch đầu
   dòng đánh số [SoT: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:157-165].
4. **"Bẫy thường gặp"** — đã có trong khối "xem nhanh", không có mã `Fx-nn` riêng, là một cách trình bày cụ
   thể của F6-04 (gợi ý hướng tiếp cận bao gồm cả lỗi thường gặp) [SoT:
   09-layoutBase/Câu hỏi phỏng vấn.dc.html:167-172].
5. **Khung trả lời chuẩn** (F6-05) — **hoàn toàn chưa có trong prototype**, kể cả cấu trúc STAR cho câu hỏi
   phân loại "Hành vi" (ví dụ `IQ-084`) [SoT: Suy luận — đối chiếu toàn bộ dữ liệu mẫu `Q` trong
   09-layoutBase/Câu hỏi phỏng vấn.dc.html:236-302, không có trường nào chứa khung trả lời đầy đủ, chỉ có
   "points" ngắn gọn]. **[Đợi nextjs]** — cần dựng thêm khối riêng biệt với "Ý cần nói": với câu hành vi hiện
   rõ 4 phần Situation/Task/Action/Result, với câu kỹ thuật hiện một đoạn mẫu trả lời đầy đủ hơn "Ý cần nói".
6. **Danh sách từ khoá kỹ thuật cốt lõi** (F6-06) — **chưa có trong prototype**, tag hiện tại trong khối "xem
   nhanh" (ví dụ "Hash map", "Cấu trúc dữ liệu") gần với ý này nhưng là tag phân loại/chủ đề, không phải danh
   sách từ khoá bắt buộc phải nêu khi trả lời [SoT: 09-layoutBase/Câu hỏi phỏng vấn.dc.html:174-178, 244].
   **[Đợi nextjs]** — cần một khối riêng, tách khỏi tag chủ đề.

**Chế độ luyện (`PRACTICE`) — hoàn toàn chưa có prototype**

7. **Ô soạn câu trả lời** (F6-07) — **[Đợi nextjs]**. Đề xuất tham khảo mẫu textarea đã có ở `mock_interview`
   (`09-layoutBase/Phỏng vấn giả lập.dc.html:308`) cho nhất quán trải nghiệm nhập liệu dài trong cùng dự án.
8. **Phản hồi AI sau khi gửi** (F6-08: điểm đã đạt, điểm còn thiếu, hướng bổ sung) — **[Đợi nextjs]**. Đề
   xuất tham khảo cấu trúc hiển thị dạng "Làm tốt"/"Nên sửa" đã dùng ở `solution_review`
   (`01-rd/screens/users/USR0301_solution_review.md` mục 3.7) để giữ ngôn ngữ hình ảnh nhất quán giữa các tính năng
   AI, dù đây là hai luồng dữ liệu độc lập (F6-08 không dùng chung schema với F5-07).
9. **Trạng thái loading/lỗi khi gọi AI** — **[Đợi nextjs]**, cùng loại khoảng trống đã ghi nhận ở
   `solution_review` (Phase 3, Câu hỏi mở Q3) và đã có mẫu tốt hơn ở `mock_interview` (khối `aiError`) — nên
   tái dùng mẫu của `mock_interview` làm chuẩn khi dựng UI thật.
10. **Câu hỏi mở về việc có sửa lại câu trả lời sau khi đã nhận phản hồi hay không** — đã được
    `01-rd/req/user_stories/open_questions.md` ghi nhận là câu hỏi mở ở mức yêu cầu (Q1), không phải phát
    hiện mới của phiên này [SoT: 01-rd/req/user_stories/open_questions.md — Q1].

### 2.1 Cấu trúc hệ thống liên quan

| Màn/tài liệu | Vai trò | Bounded Context |
|---|---|---|
| Màn hiện tại | `USR0402` / `interview_question_detail` | `interview-bank` |
| Tài liệu yêu cầu | `01-rd/req/interview-bank.md` (F6-04 tới F6-08) | Nguồn SoT của màn |
| Prototype | Chưa có prototype riêng (đợi Next.js) | Bằng chứng bố cục và trạng thái |

**Chi tiết trạng thái và cấu trúc màn**

| Thành phần | Vai trò | Nguồn |
| :--- | :--- | :--- |
| `01-rd/screens/users/USR0401_interview_bank_list.md` | Màn nguồn — nơi dẫn vào màn này, chứa khối "xem nhanh" rút gọn dùng làm phần chung tham chiếu | [SoT: 01-rd/screens/users/USR0401_interview_bank_list.md — mục 2, mục 4 Câu hỏi mở Q1] |
| `01-rd/screens/users/USR0301_solution_review.md` | Tham chiếu mẫu trình bày phản hồi AI dạng "Làm tốt"/"Nên sửa" cho Chế độ luyện | [SoT: 01-rd/screens/users/USR0301_solution_review.md — mục 3.7] |
| `01-rd/screens/users/USR0302_mock_interview.md` | Tham chiếu mẫu textarea nhập liệu dài và mẫu trạng thái loading/lỗi AI (`aiError`) | [SoT: 09-layoutBase/Phỏng vấn giả lập.dc.html:308] |
| Bounded Context `interview-bank` (F6), `ai-review` (F5) | BC sở hữu dữ liệu và logic đứng sau màn — Chế độ luyện đi qua `ai-review` | [SoT: 01-rd/req/interview-bank.md — F6-07, F6-08] |

---

## 3. Ngoài phạm vi (Out of Scope)

- Bảng màu, spacing, component cụ thể, breakpoint responsive — thuộc BD
  (`02-bd/screens/users/USR0402_interview_question_detail.md`, chưa viết) — **sẽ cần dựng UI thật trước cho Chế độ
  luyện, vì chưa có prototype để BD đối chiếu**, khác với các màn khác của Phase 3/4.
- Hợp đồng API (lấy chi tiết câu hỏi, gửi câu trả lời Chế độ luyện, nhận điểm AI) — thuộc DD
  (`03-dd/api/interview-bank.md`, chưa viết).
- Nội dung và bố cục của `interview_bank_list` (nơi dẫn vào màn này) — thuộc file RD riêng của nó.

---

## 4. Tiền đề và ràng buộc

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | Toàn bộ Chế độ luyện (F6-07/F6-08) và khung trả lời chuẩn STAR (F6-05) chưa có bản dựng nào để đối chiếu — mức độ chi tiết UI phải suy luận hoàn toàn từ `interview-bank.md`. | Không có prototype; đây là phần duy nhất của Phase 3-4 phải suy luận UI từ đầu thay vì đối chiếu bản dựng có sẵn. | **[Đợi nextjs]** — dựng prototype hoặc UI thật cho Chế độ luyện trước khi viết BD cho phần này; RD ở đây chỉ đủ để mô tả hành vi nghiệp vụ, không mô tả bố cục cụ thể. | Đợi nextjs |
| Q2 | ~~Sinh viên có sửa được câu trả lời đã nộp ở Chế độ luyện sau khi đã nhận phản hồi AI không?~~ **ĐÃ CHỐT 2026-08-31 (owner instruction, theo đúng đề xuất):** không sửa được — mỗi lần nộp là một lượt độc lập. | — | Đã ghi vào `01-rd/req/interview-bank.md` (amendment F6-07) và đóng cùng lúc ở `01-rd/req/user_stories/open_questions.md` Q1. Xem `DEC-2026-0831-outside-screens-closures`. | Đã đóng |

---

## 5. Danh sách yêu cầu (REQ)

| ID | Yêu cầu | Loại | Nguồn/SoT |
|---|---|---|---|
| REQ-01 | Chế độ học: gợi ý hướng tiếp cận (F6-04) | Chức năng | `01-rd/req/interview-bank.md` — F6-04 |
| REQ-02 | Chế độ học: khung trả lời chuẩn, áp dụng STAR cho câu hành vi (F6-05) | Chức năng | `01-rd/req/interview-bank.md` — F6-05 |
| REQ-03 | Chế độ học: danh sách từ khoá kỹ thuật cốt lõi (F6-06) | Chức năng | `01-rd/req/interview-bank.md` — F6-06 |
| REQ-04 | Chế độ luyện: người dùng tự soạn câu trả lời (F6-07) | Chức năng | `01-rd/req/interview-bank.md` — F6-07 |
| REQ-05 | Chế độ luyện: AI đối chiếu tiêu chí chuẩn, trả điểm đã đạt/điểm còn thiếu/hướng bổ sung (F6-08) | Chức năng | `01-rd/req/interview-bank.md` — F6-08 |
| REQ-06 | Đánh dấu để xem lại; tự chấm mức độ thuộc bài (F6-03, F6-12) | Chức năng | `01-rd/req/interview-bank.md` — F6-03, F6-12 |
| REQ-07 | Chống prompt injection, rate limit — Chế độ luyện đi qua phân hệ AI, chịu chung ràng buộc F5 (F5-17 tới F5-22) | Chức năng | `01-rd/req/ai-review.md` — F5-17 tới F5-22; `01-rd/req/interview-bank.md` — F6-08 |
| REQ-08 | Given-When-Then Chế độ học/luyện (—) | Chức năng | `01-rd/req/user_stories/a1_student.md` (`US-A1-08`) |
| REQ-09 | **Cho** tôi đang xem một câu hỏi ở Chế độ học, **Khi** tôi chuyển sang Chế độ luyện, **Thì** tôi thấy một ô trống để tự soạn câu trả lời, không thấy trước "Ý cần nói"/khung trả lời chuẩn của Chế độ học (tránh nhìn đáp án trước khi tự làm) — **[Đợi nextjs]**, chưa có prototype xác nhận hành vi ẩn/hiện chéo hai chế độ | Chức năng (GWT) | [SoT: Suy luận — chưa có prototype cho Chế độ luyện] |
| REQ-10 | **Cho** tôi vừa gửi câu trả lời ở Chế độ luyện, **Khi** AI chấm xong, **Thì** tôi thấy điểm đã đạt/điểm còn thiếu/hướng bổ sung (F6-08), và giao diện ghi rõ đây là phản hồi học tập — áp dụng chung nguyên tắc F5-18 dù F6-08 không thuộc F5 | Chức năng (GWT) | [SoT: 01-rd/req/interview-bank.md — F6-08] |
| REQ-11 | **Cho** ngân sách token AI đã cạn cho vai trò của tôi (F5-25, áp dụng cả F6-08 theo `interview-bank.md`), **Khi** tôi gửi câu trả lời ở Chế độ luyện, **Thì** tôi thấy thông báo tính năng AI đang tạm khoá — cùng loại trạng thái đã ghi ở `01-rd/screens/users/USR0301_solution_review.md` mục 4 | Chức năng (GWT) | [SoT: 01-rd/req/ai-review.md — F5-25] |

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
| Yêu cầu module | `01-rd/req/ai-review.md` — F5-17 tới F5-22; `01-rd/req/interview-bank.md` — F6-01 tới F6-12. |
| User story | `01-rd/req/user_stories/a1_student.md` — `US-A1-08`; `01-rd/req/user_stories/open_questions.md` — Q1 gốc. |
| Khảo sát hệ thống | `01-rd/overview/system_survey.md` — mục 7.1 dòng `interview_question_detail`. |
| Màn hình liên quan | `01-rd/screens/users/USR0401_interview_bank_list.md` — màn nguồn, mục 2 và mục 4 Câu hỏi mở Q1 (quyết định tách hai màn). |
| Tài liệu tham khảo | `01-rd/screens/users/USR0301_solution_review.md`, `01-rd/screens/users/USR0302_mock_interview.md` — tham chiếu mẫu trình bày cho trạng thái loading/lỗi AI khi dựng UI thật. |
| Kế hoạch | `06-plan/nexa-plan/260824-2043-bd-screens-common-first.md` — kế hoạch Phase 4 sinh ra file này. |
