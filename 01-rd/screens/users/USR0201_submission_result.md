# RD (Yêu cầu hệ thống mới) — Kết quả nộp bài / `USR0201`

> Mã màn hình: `USR0201` [Nguồn: 02-bd/_rules/bd-template-9sheet.md — mục 8].
> Slug chính tắc: `submission_result` [SoT: 01-rd/overview/system_survey.md — mục 7.1 dòng `submission_result`].
> Phạm vi/Bounded Context: `judge-orchestration` (F4), có đọc thêm từ `problem-bank` (siêu dữ liệu bài toán,
> độ khó) [SoT: 01-rd/overview/system_survey.md mục 7.1 — cột "Bounded Context liên quan", dòng
> `submission_result`]. Actor: A1 (chính — chủ sở hữu bài nộp); A2 chỉ đọc ở vùng giảng viên, xem chương 10.
> Nguồn sự thật (SoT): `01-rd/req/judge-orchestration.md` (mục F4), `01-rd/req/user_stories/a1_student.md`
> (`US-A1-04`), `09-layoutBase/Kết quả nộp bài.dc.html` (prototype), `01-rd/overview/system_survey.md` (mục
> 7.1, ghi chú "`problem_detail` và `submission_result` là hai màn riêng, đừng gộp").
> Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

Trang xem lại kết quả của **một bài nộp cụ thể** sau khi đã có `submissionId`: verdict tổng (Accepted/Wrong
Answer/Time Limit Exceeded...), thống kê chạy (thời gian, bộ nhớ), chi tiết theo từng testcase, mã nguồn đã
nộp, và lối vào hai luồng AI sau khi `Accepted` (Phân tích bài giải, Phỏng vấn giả lập)
[SoT: 01-rd/req/user_stories/a1_student.md — US-A1-04; 01-rd/overview/system_survey.md — mục 7.1 dòng `submission_result`].

`system_survey.md` chốt rõ **`problem_detail` và `submission_result` là hai màn riêng, không gộp**: kết quả
một bài nộp cần URL riêng để mở lại được từ trang tiến độ/lịch sử [SoT: 01-rd/overview/system_survey.md —
mục 7.1, ghi chú "`problem_detail` và `submission_result` là hai màn riêng, đừng gộp"].

Đối chiếu prototype: `09-layoutBase/Kết quả nộp bài.dc.html`. File này mô tả **hành vi và UX ở mức yêu
cầu** — không lặp lại đặc tả chức năng đã có ở `01-rd/req/judge-orchestration.md` (mục F4) và
`01-rd/req/user_stories/a1_student.md` (`US-A1-04`), chỉ trỏ tới và bổ sung phần đặc thù của màn.

---

## 2. Phạm vi

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Nộp bài trả `submissionId` ngay, không chờ kết quả chấm | F4-01 | `01-rd/req/judge-orchestration.md` — F4-01 |
| Gọi `JudgeExecutionPort` từng testcase, chạy hết toàn bộ N testcase (fail-fast đã bỏ) | F4-03 | `01-rd/req/judge-orchestration.md` — F4-03, amendment 2026-08-31, `DEC-2026-0831-partial-score-testcase-ratio` |
| Điểm tỷ lệ testcase (thang 1đ/bài, không thay verdict) | F4-13 | `01-rd/req/judge-orchestration.md` — F4-13, amendment 2026-08-31, `DEC-2026-0831-partial-score-testcase-ratio` |
| Đẩy trạng thái từng testcase qua WebSocket (STOMP) theo kênh riêng của bài nộp | F4-08 | `01-rd/req/judge-orchestration.md` — F4-08 |
| Chống rò rỉ testcase ẩn: chỉ trả trạng thái và chỉ số, không trả input/diff chi tiết | F2-08 | `01-rd/req/problem-bank.md` — F2-08 |
| Ánh xạ lỗi biên dịch về đúng dòng mã người dùng; che giấu hoàn toàn mã harness | F3-11, F3-12 | `01-rd/req/harness.md` — F3-11, F3-12 |
| Sau `Accepted`, mở hai lựa chọn: Phân tích bài giải hoặc Phỏng vấn giả lập | F5-01, F5-09 | `01-rd/req/ai-review.md` — F5-01, F5-09 |
| Given-When-Then đầy đủ cho luồng nộp bài và xem kết quả thời gian thực | — | `01-rd/req/user_stories/a1_student.md` (`US-A1-04`) |

Đối chiếu `09-layoutBase/Kết quả nộp bài.dc.html`:

1. **Banner verdict tổng** — chấm màu, nhãn verdict viết hoa, ghi chú (sai từ testcase số mấy / đúng toàn bộ
   kèm thời điểm nộp), tên bài và độ khó [SoT: 09-layoutBase/Kết quả nộp bài.dc.html:111-122]. Editor mẫu chỉ
   khai báo **ba** giá trị verdict: `Accepted`, `Wrong Answer`, `Time Limit Exceeded`
   [SoT: 09-layoutBase/Kết quả nộp bài.dc.html:238] — không có `Compile Error` hay `Runtime Error` dù cả hai
   đều là verdict hợp lệ theo F3-11/F3-12 (ánh xạ lỗi biên dịch) — đã chốt ở Câu hỏi mở Q3 (đã đóng), chưa
   dựng vào prototype. (Tiền lệ F4-09a "Runtime Error/TLE" như loại kết quả cũ có thể lọc khi chấm lại đã
   loại khỏi phạm vi 2026-08-28, `DEC-2026-0828-remove-rejudge-scope` — không còn liên quan.)
2. **Thanh thống kê chạy** (`runStats`) — 5 chỉ số: Testcase (đạt/tổng), Runtime, Memory, "Beats" (tỉ lệ % nhanh
   hơn các bài nộp khác), và Cách nộp (`wrapper`/`stdio`, khớp F3-13) [SoT:
   09-layoutBase/Kết quả nộp bài.dc.html:123-131, 314-320]. Bốn chỉ số rỗng (`—`) khi bài nộp không
   `Accepted` [SoT: 09-layoutBase/Kết quả nộp bài.dc.html:316-319]. Chỉ số "Beats" chưa có mã `Fx-nn` nào mô
   tả cách tính — đã chốt ở Câu hỏi mở Q4 (đã đóng): mã **F4-12** mới, xem `01-rd/req/judge-orchestration.md`. **Ghi chú 2026-10-18** (chủ dự án, BD `USR0201` Q2): "Beats" chỉ hiển thị khi hệ thống có ít nhất 10 bài nộp `Accepted` cùng bài, cùng ngôn ngữ; ít hơn thì in "Chưa đủ dữ liệu so sánh" thay cho số %. **Amendment
   2026-08-31** (`DEC-2026-0831-partial-score-testcase-ratio`): thêm chỉ số thứ sáu **"Điểm"** (F4-13, ví dụ
   "0.6/1") ngay cạnh banner verdict, hiển thị cho **mọi** verdict (không rỗng khi không `Accepted`, khác
   bốn chỉ số kia) — vì bây giờ mọi testcase đều được chạy hết, luôn có đủ dữ liệu tính tỷ lệ.
3. **Khối "Testcase đầu tiên bị sai"** — chỉ hiện khi có lỗi, tách hai biến thể theo loại testcase gây sai
   (`hasFailureVisible`/`hasFailureHidden`): nếu testcase gây sai là **mẫu** (`sample: true`), hiện đủ ba ô
   Input/Expected/"Kết quả của bạn" như cũ; nếu là **ẩn** (`sample: false`), chỉ hiện một dòng trạng thái +
   chỉ số (ví dụ "Testcase #05 · WA"), không còn ba ô chi tiết [SoT:
   09-layoutBase/Kết quả nộp bài.dc.html:134-153]. **Đã sửa xong lỗi vi phạm F2-08 nêu ở Câu hỏi mở Q1** (đã
   chốt qua hỏi trực tiếp chủ dự án, đúng theo đề xuất trong bảng câu hỏi mở): trong dữ liệu mẫu, testcase gây
   sai (`failAt = 5`) là testcase ẩn thứ 5, nay chỉ hiện trạng thái, không còn lộ Input/Expected/Output
   [SoT: 09-layoutBase/Kết quả nộp bài.dc.html:329, 334].
4. **Bảng "Chi tiết testcase"** — tab lọc Tất cả/Ví dụ/Bị sai (`caseTabs`), cột #, Input, Expected, Output,
   Time, Kết quả (AC/WA/TLE). **Amendment 2026-08-31**: không còn giá trị `—` do bỏ qua vì fail-fast — mọi
   testcase đều thực sự được chạy nên luôn có kết quả thật [SoT:
   09-layoutBase/Kết quả nộp bài.dc.html:148-184]. **Đã sửa cùng lỗi F2-08 như mục 3**: với testcase
   `sample: false`, ba cột Input/Expected/Output nay hiện placeholder "Ẩn"/"Hidden" thay vì giá trị thật, ở
   mọi tab lọc kể cả "Bị sai" [SoT: 09-layoutBase/Kết quả nộp bài.dc.html:346-359]. Dòng tóm tắt cuối bảng
   ("3 testcase ví dụ hiển thị · 5 testcase ẩn chạy trên máy chấm") nay khớp đúng với hành vi bảng phía trên,
   không còn tự mâu thuẫn [SoT: 09-layoutBase/Kết quả nộp bài.dc.html:193, 360-362].
5. **Khối "Code đã nộp"** — hiện toàn bộ mã nguồn người học đã nộp, có tô màu từ khoá, kèm nhãn ngôn ngữ
   [SoT: 09-layoutBase/Kết quả nộp bài.dc.html:186-199]. Đây là dữ liệu của chính người học, không phải dữ
   liệu testcase ẩn, nên không vi phạm F2-08.
6. **Cột hành động tiếp theo** (`nextActions`) — đổi nội dung theo verdict: nếu sai, chỉ một thẻ "Xem lại
   testcase sai" dẫn về Workspace; nếu `Accepted`, hai thẻ dẫn sang `solution_review` (F5-01, nhãn nội bộ
   `F5.1` trong prototype) và `mock_interview` (F5-09, nhãn nội bộ `F5.2`) [SoT:
   09-layoutBase/Kết quả nộp bài.dc.html:202-213, 342-356]. Nhãn `F5.1`/`F5.2` là ký hiệu nội bộ của
   prototype, không khớp cách đánh mã `Fx-nn` của `req.md` — chỉ là khác biệt văn bản hiển thị, không phải
   xung đột nghiệp vụ.
7. **Khối "Lần nộp trước của bài này"** — tối đa 3 lượt nộp gần nhất **của cùng bài toán này**, kèm liên kết
   "Xem toàn bộ lịch sử nộp" dẫn sang `my_submissions` [SoT: 09-layoutBase/Kết quả nộp bài.dc.html:215-229].
   Đây là một lát cắt thu hẹp của lịch sử nộp bài đã có mã F1-07, không cần mã riêng
   [SoT: 01-rd/overview/system_survey.md — mục 7.1 dòng `my_submissions` — dòng `my_submissions`, F1-07].
8. **Nút "Sửa lại và nộp tiếp"** — quay về Workspace (`problem_detail`) để sửa mã và nộp lại
   [SoT: 09-layoutBase/Kết quả nộp bài.dc.html:231].
9. **Không có trạng thái "đang chấm" (`PENDING`/`RUNNING`)** — toàn bộ state trong prototype là tĩnh, tính từ
   một `verdict` cho sẵn (`Accepted`/`Wrong Answer`/`Time Limit Exceeded`, dòng 238, 277-278); không có biến
   trạng thái nào mô phỏng việc nhận cập nhật từng testcase qua WebSocket khi bài nộp còn đang chạy — đã
   chốt ở Câu hỏi mở Q2 (đã đóng): có tự subscribe, chưa dựng vào prototype tĩnh này.

Hai chế độ hiển thị không đổi hành vi nghiệp vụ, chỉ đổi văn bản/field: `data-ui-lang="vi|en"` và
`data-theme="light|dark"` [SoT: 09-layoutBase/Kết quả nộp bài.dc.html:262-271], theo đúng
`DEC-2026-0824-i18n-vi-en` và `DEC-2026-0824-dark-light-theme`.

### 2.1 Cấu trúc hệ thống liên quan

| Màn/tài liệu | Vai trò | Bounded Context |
|---|---|---|
| Màn hiện tại | `USR0201` / `submission_result` | `judge-orchestration` |
| Tài liệu yêu cầu | `01-rd/req/judge-orchestration.md` (F4) | Nguồn SoT của màn |
| Prototype | `09-layoutBase/Kết quả nộp bài.dc.html` | Bằng chứng bố cục và trạng thái |

**Chi tiết trạng thái và cấu trúc màn**

| Thành phần | Vai trò | Nguồn |
| :--- | :--- | :--- |
| `01-rd/screens/users/USR0102_problem_detail.md` | Đích của nút "Sửa lại và nộp tiếp" — quay lại Workspace để sửa mã | [SoT: 09-layoutBase/Kết quả nộp bài.dc.html:231] |
| `01-rd/screens/users/USR0202_my_submissions.md` | Đích của liên kết "Xem toàn bộ lịch sử nộp" | [SoT: 09-layoutBase/Kết quả nộp bài.dc.html:215-229] |
| `01-rd/screens/users/USR0301_solution_review.md` | Đích khi verdict `Accepted`, chọn "Phân tích bài giải" (F5-01) | [SoT: 09-layoutBase/Kết quả nộp bài.dc.html:202-213] |
| `01-rd/screens/users/USR0302_mock_interview.md` | Đích khi verdict `Accepted`, chọn "Phỏng vấn giả lập" (F5-09) | [SoT: 09-layoutBase/Kết quả nộp bài.dc.html:202-213] |
| `09-layoutBase/Kết quả nộp bài.dc.html` | Prototype đối chiếu bố cục và trạng thái màn | [SoT: 09-layoutBase/Kết quả nộp bài.dc.html] |
| Bounded Context `judge-orchestration` (F4), `problem-bank`, `harness`, `ai-review` | BC sở hữu dữ liệu và logic đứng sau màn | [SoT: 01-rd/overview/system_survey.md — mục 7.1] |

---

## 3. Ngoài phạm vi (Out of Scope)

- Bảng màu, spacing, component cụ thể, breakpoint responsive — thuộc BD (`02-bd/screens/users/USR0201_submission_result.md`,
  chưa viết).
- Hợp đồng API (lấy chi tiết một bài nộp, kênh WebSocket theo `submissionId`) — thuộc DD
  (`03-dd/api/judge-orchestration.md`, chưa viết).
- Thuật toán so khớp kết quả (exact/whitespace/epsilon/set — F3-07 tới F3-10), cơ chế chạy hết testcase,
  timeout sweep — thuộc logic của `judge-orchestration`/`harness`, không thuộc file theo trục màn này.
- Nội dung và bố cục của `solution_review` và `mock_interview` (hai màn đích của cột hành động tiếp theo) —
  thuộc các file RD riêng của chúng.

---

## 4. Tiền đề và ràng buộc

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | ~~Prototype hiện hiển thị Input/Expected/Output đầy đủ cho testcase ẩn khi nó gây sai...~~ **ĐÃ CHỐT (phiên 2026-08-25)**: không có ngoại lệ nghiệp vụ nào — đây là lỗi dựng prototype, đã sửa trực tiếp `09-layoutBase/Kết quả nộp bài.dc.html` theo đúng đề xuất: testcase `sample: false` hiển thị placeholder "Ẩn"/"Hidden" ở cột Input/Expected/Output (giữ cột #, Time, Kết quả); khối "Testcase đầu tiên bị sai" khi rơi vào testcase ẩn chỉ còn một dòng trạng thái + chỉ số [SoT: 09-layoutBase/Kết quả nộp bài.dc.html:148-153, 346-359]. | — | Đã sửa, không cần hành động thêm. | Đã đóng |
| Q2 | ~~Màn `submission_result` có cần tự nhận cập nhật qua WebSocket khi người dùng mở đúng URL này lúc bài nộp còn đang ở trạng thái `PENDING`/đang chấm...~~ **ĐÃ CHỐT (phiên 2026-08-25, chốt theo RD):** có — `submission_result` tự subscribe kênh WebSocket của đúng `submissionId` khi tải trang nếu trạng thái bài nộp chưa phải trạng thái cuối, tái dùng đúng cơ chế F4-08, tránh phải bắt người dùng ở lại `problem_detail` mới thấy tiến độ. **Không có state nào cần dựng thêm ở prototype tĩnh này** — hành vi thuộc tầng kết nối thật, chỉ có ý nghĩa khi build FE Next.js thật. | — | Đã chốt quyết định. | Đã đóng |
| Q3 | ~~Verdict `Compile Error` và `Runtime Error` chưa có trạng thái nào được dựng trong prototype...~~ **ĐÃ CHỐT (phiên 2026-08-25, chốt theo RD):** thêm hai trạng thái verdict mới vào UI thật — `Compile Error` (thay khối "Testcase đầu tiên bị sai" bằng khối "Lỗi biên dịch" hiện thông điệp lỗi + số dòng, ẩn hẳn bảng testcase vì chưa có testcase nào chạy) và `Runtime Error` (giữ bảng testcase, thêm badge mô tả loại lỗi runtime ở testcase gây lỗi, vẫn tuân F2-08 nếu đó là testcase ẩn). **Chưa dựng vào `09-layoutBase/Kết quả nộp bài.dc.html`** (enum verdict vẫn chỉ 3 giá trị) — để lúc build FE Next.js thật. | — | Đã chốt quyết định, chưa dựng prototype. | Đã đóng |
| Q4 | ~~Chỉ số "Beats"...chưa có mã `Fx-nn` nào mô tả cách tính...~~ **ĐÃ CHỐT (phiên 2026-08-25, chốt theo RD):** đã thêm **F4-12** vào `01-rd/req/judge-orchestration.md` — tính theo runtime, so sánh trong tập bài nộp `Accepted` của cùng bài toán và cùng ngôn ngữ (không so chéo ngôn ngữ), chỉ hiển thị khi verdict là `Accepted` [SoT: 01-rd/req/judge-orchestration.md — F4-12]. | — | Đã thêm mã F4-12 vào `judge-orchestration.md`. | Đã đóng |
| Q5 | Chủ dự án muốn thêm điểm tỷ lệ testcase (mỗi bài toán tối đa 1đ, chia theo tỷ lệ testcase Pass), hiển thị cho người học — đảo lại phần "Pass/Fail toàn phần + fail-fast, không điểm từng phần" đã chốt 2026-08-24 (`06-plan/PROTOTYPE_DEBT.md` mục 2.6 phần 1). **ĐÃ CHỐT (phiên 2026-08-31, qua hỏi trực tiếp chủ dự án):** bỏ fail-fast (F4-04 hết hiệu lực), thêm mã **F4-13** — điểm tỷ lệ hiển thị thêm cạnh banner verdict, không thay thế verdict `Accepted`/`Wrong Answer` nhị phân (vẫn giữ nguyên mọi phụ thuộc: kích hoạt F5, F1-07, ngưỡng AC&lt;30% của `problem_management`). Không đảo lại quyết định bỏ cột "Điểm"/trọng số ở UI `problem_authoring` — điểm tỷ lệ ở đây tính tự động đều nhau, không cần giảng viên khai báo trọng số. Xem `DEC-2026-0831-partial-score-testcase-ratio`. | — | Đã thêm mã F4-13, sửa F4-03/04, xem mục 3.2 và 3.4. | Đã đóng |

---

## 5. Danh sách yêu cầu (REQ)

| ID | Yêu cầu | Loại | Nguồn/SoT |
|---|---|---|---|
| REQ-01 | Nộp bài trả `submissionId` ngay, không chờ kết quả chấm (F4-01) | Chức năng | `01-rd/req/judge-orchestration.md` — F4-01 |
| REQ-02 | Gọi `JudgeExecutionPort` từng testcase, chạy hết toàn bộ N testcase (fail-fast đã bỏ) (F4-03) | Chức năng | `01-rd/req/judge-orchestration.md` — F4-03, amendment 2026-08-31, `DEC-2026-0831-partial-score-testcase-ratio` |
| REQ-03 | Điểm tỷ lệ testcase (thang 1đ/bài, không thay verdict) (F4-13) | Chức năng | `01-rd/req/judge-orchestration.md` — F4-13, amendment 2026-08-31, `DEC-2026-0831-partial-score-testcase-ratio` |
| REQ-04 | Đẩy trạng thái từng testcase qua WebSocket (STOMP) theo kênh riêng của bài nộp (F4-08) | Chức năng | `01-rd/req/judge-orchestration.md` — F4-08 |
| REQ-05 | Chống rò rỉ testcase ẩn: chỉ trả trạng thái và chỉ số, không trả input/diff chi tiết (F2-08) | Chức năng | `01-rd/req/problem-bank.md` — F2-08 |
| REQ-06 | Ánh xạ lỗi biên dịch về đúng dòng mã người dùng; che giấu hoàn toàn mã harness (F3-11, F3-12) | Chức năng | `01-rd/req/harness.md` — F3-11, F3-12 |
| REQ-07 | Sau `Accepted`, mở hai lựa chọn: Phân tích bài giải hoặc Phỏng vấn giả lập (F5-01, F5-09) | Chức năng | `01-rd/req/ai-review.md` — F5-01, F5-09 |
| REQ-08 | Given-When-Then đầy đủ cho luồng nộp bài và xem kết quả thời gian thực (—) | Chức năng | `01-rd/req/user_stories/a1_student.md` (`US-A1-04`) |
| REQ-09 | **Cho** tôi mở trực tiếp URL của một bài nộp đã có kết quả cuối (từ lịch sử hoặc đường dẫn chia sẻ), **khi** trang tải xong, **thì** tôi thấy ngay đầy đủ banner verdict, thống kê chạy, bảng testcase và code đã nộp mà không cần chờ một sự kiện WebSocket nào — khác với lúc bài nộp còn đang chấm dở (US-A1-04 mô tả trường hợp đó) | Chức năng (GWT) | [SoT: Suy luận — hệ quả tất yếu của việc `submission_result` là trang xem lại có URL riêng (01-rd/overview/system_survey.md — mục 7.1, ghi chú "`problem_detail` và `submission_result` là hai màn riêng, đừng gộp"), chưa có Given-When-Then riêng nào xác nhận] |
| REQ-10 | **Cho** testcase gây sai là một testcase ẩn (`Hidden`, F2-06), **khi** tôi xem khối "Testcase đầu tiên bị sai" hoặc bảng "Chi tiết testcase", **thì** tôi chỉ thấy trạng thái (`WA`/`TLE`) và chỉ số thứ tự của testcase đó, không thấy Input, không thấy Expected, không thấy Output của tôi — đã khớp prototype sau khi sửa Câu hỏi mở Q1 | Chức năng (GWT) | [SoT: 01-rd/req/problem-bank.md — F2-08; 09-layoutBase/Kết quả nộp bài.dc.html:148-153, 346-359] |
| REQ-11 | **Cho** testcase gây sai là một testcase mẫu (`Sample`, F2-05), **khi** tôi xem hai khối trên, **thì** tôi được thấy đầy đủ Input/Expected/Output vì testcase mẫu vốn công khai, không thuộc phạm vi bảo vệ của F2-08 | Chức năng (GWT) | [SoT: 01-rd/req/problem-bank.md — F2-05, F2-06, phân biệt Sample/Hidden] |
| REQ-12 | **Cho** bài nộp có verdict `Compile Error`, **khi** tôi xem trang kết quả, **thì** tôi thấy lỗi biên dịch được ánh xạ về đúng dòng trong mã tôi đã viết, không lộ bất kỳ dòng mã harness nào (F3-11, F3-12) — trạng thái này chưa có trong prototype, đã chốt ở Câu hỏi mở Q3 (đã đóng): khối "Lỗi biên dịch" thay thế khối "Testcase đầu tiên bị sai" khi verdict là `Compile Error` | Chức năng (GWT) | [SoT: 01-rd/req/harness.md — F3-11, F3-12] |

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
| Yêu cầu module | `01-rd/req/problem-bank.md` — F2-08; `01-rd/req/harness.md` — F3-11, F3-12; `01-rd/req/judge-orchestration.md` — F4-01, F4-03, F4-08, F4-12, F4-13 _(Sửa 2026-09-05: bỏ `F4-04` khỏi danh sách — hết hiệu lực theo `DEC-2026-0831-partial-score-testcase-ratio`.)_ |
| User story | `01-rd/req/user_stories/a1_student.md` — `US-A1-04`. |
| Khảo sát hệ thống | `01-rd/overview/system_survey.md` — mục 7.1 dòng `submission_result`, cùng ghi chú "`problem_detail` và `submission_result` là hai màn riêng, đừng gộp"; mục 7.1 — Bounded Context chạm bởi `submission_result`. |
| Prototype | `09-layoutBase/Kết quả nộp bài.dc.html`. |
| Kế hoạch | `06-plan/nexa-plan/260824-2043-bd-screens-common-first.md` — kế hoạch Phase 1 sinh ra file này. |

---

## 10. Vùng giảng viên (A2) đọc bài nộp của học viên

> Mục thêm 2026-10-14, nối sau chương 9, không đánh số lại các chương trước. Chương 1 đến 9 mô tả vùng học viên (A1);
> chương này chỉ bổ sung vùng giảng viên của **cùng một trang kết quả**, không tạo tài liệu trùng
> (`DEC-2026-1014-instructor-submissions-list-rules`, mục (9) [SoT: .nexa/control/decision-registry.md:3908-3910]).
> Giảng viên đọc được bài nộp của học viên trong lớp mình phụ trách, chỉ đọc, ngoài lớp trả 404
> [SoT: .nexa/control/decision-registry.md:3412-3414] (`DEC-2026-1013-instructor-avatar-menu-and-read-source`).
> Điểm vào của vùng này là màn `INS0206` (`01-rd/screens/teacher/INS0206_submission_management.md`), `INS0204` hoặc `INS0205`.
> Route do prototype dùng: `/instructor/submissions/{id}` [SoT: 01-rd/screens/teacher/INS0206_submission_management.md:5].
> Cập nhật 2026-10-14: áp `DEC-2026-1014-instructor-submissions-list-details` [SoT: .nexa/control/decision-registry.md:3975-4004]: sửa REQ-14, REQ-16 và thêm REQ-17 đến REQ-19 (màn gọi tới qua `from`, không realtime, máy chủ lấy học viên và lớp từ lượt nộp).

Bảng 10.1 liệt kê các yêu cầu của vùng giảng viên.

| ID | Yêu cầu | Nguồn/SoT |
|---|---|---|
| REQ-13 | **Chỉ đọc.** Giảng viên không có hành động nào của học viên trên trang: không Phân tích bài giải (F5-01), không vào Phỏng vấn giả lập (F5-09), không "Sửa lại và nộp tiếp". **GWT:** Cho giảng viên mở một bài `Accepted` của học viên trong lớp, khi trang tải xong, thì thấy verdict, chỉ số chạy, bảng testcase và mã đã nộp, nhưng không có nút nào dẫn sang AI hay quay lại Workspace | `DEC-2026-1013-instructor-avatar-menu-and-read-source`; `DEC-2026-1014-instructor-submissions-list-rules`; prototype `05-coding/frontend/src/views/users/submission-result/ui/submission-result-view.tsx:312-313` (khu vực "Next actions" không dựng cho giảng viên) |
| REQ-14 | Trang hiện **học viên đã nộp và lớp** của bài nộp. **ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-instructor-submissions-list-details)**: tên học viên và lớp lấy từ **chính lượt nộp** ở máy chủ, không suy từ phiên của giảng viên và không tin `studentId`, `classId` trên URL (xem REQ-19). **GWT:** Cho giảng viên mở bài nộp từ danh sách, khi trang tải xong, thì thấy tên học viên và lớp của bài nộp đó | `DEC-2026-1013-instructor-submissions-list` (mang học viên và lớp trên query) [SoT: .nexa/control/decision-registry.md:3467-3469]; `DEC-2026-1014-instructor-submissions-list-details` mục (7); prototype `submission-result-view.tsx:59-61,145-151` (thẻ ngữ cảnh "ai đã nộp", hiện tra tên theo query) |
| REQ-15 | **Phạm vi:** bài nộp ngoài các lớp giảng viên phụ trách trả **404**, giống một mã không tồn tại, không lộ việc bài nộp có tồn tại. **Ngoại lệ:** học viên đã rời lớp vẫn cho giảng viên đọc các bài nộp làm lúc còn trong lớp. **GWT:** Cho bài nộp của học viên chưa từng ở lớp nào của giảng viên, khi giảng viên mở thẳng URL, thì nhận trang "không tìm thấy"; cho bài nộp học viên làm lúc còn trong lớp rồi rời lớp, thì trang mở bình thường | `DEC-2026-1013-instructor-avatar-menu-and-read-source`; `DEC-2026-1014-instructor-submissions-list-rules` mục (5); prototype `submission-result-view.tsx:73-85` (404 ngoài phạm vi, chưa mô phỏng ngoại lệ rời lớp) |
| REQ-16 | **Breadcrumb** dẫn về đúng màn đã gọi tới: chi tiết học viên (`INS0204`), chi tiết lớp (`INS0205`) hoặc danh sách bài nộp (`INS0206`); khi trang được mở từ màn đó thì quay lại bằng cách lùi lịch sử trình duyệt, nên bộ lọc, từ khoá, trang của màn gọi tới được khôi phục (**ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-instructor-submissions-list-details)** mục (2)). **Sidebar** giữ sáng mục của màn gọi tới (không sáng mục riêng cho trang kết quả). **GWT:** Cho giảng viên đi từ `INS0204` sang trang kết quả, khi bấm mục breadcrumb đầu, thì về `INS0204`, và sidebar vẫn sáng mục của `INS0204` | `.nexa/control/decision-registry.md:3761-3762` (quy tắc sáng sidebar theo màn gọi tới, trích ở `INS0206_submission_management.md` REQ-10); prototype `submission-result-view.tsx:56-59,105-122` (breadcrumb mặc định về `/instructor/submissions`, có thể thay bằng breadcrumb biết học viên và lớp). Việc có đủ ba đích gọi tới trong prototype chưa kiểm chứng ở file này: [SoT: Suy luận] từ prop `renderBreadcrumb` |

| REQ-17 | **Màn gọi tới** được báo qua tham số query `from` với giá trị `submissions`, `student` hoặc `class` (mặc định `submissions` khi vắng hoặc giá trị lạ). `from` chỉ dùng để dựng breadcrumb và sáng đúng mục sidebar, không cấp quyền, không đổi dữ liệu. **GWT:** Cho giảng viên mở lượt nộp từ `INS0204`, khi trang tải xong, thì breadcrumb về `INS0204` và sidebar sáng mục của `INS0204`; cho giảng viên mở thẳng URL không có `from`, thì breadcrumb về danh sách bài nộp | `DEC-2026-1014-instructor-submissions-list-details` mục (5) [SoT: .nexa/control/decision-registry.md:3975-4004] |
| REQ-18 | **Không realtime.** Trang kết quả vùng giảng viên không đăng ký kênh realtime (STOMP): tải một lần và hiện trạng thái hiện tại của lượt nộp; muốn cập nhật thì tải lại trang. **GWT:** Cho giảng viên mở lượt nộp đang chấm, khi trang tải xong, thì thấy trạng thái đang chấm hiện tại và bảng testcase không tự cập nhật khi chấm xong | `DEC-2026-1014-instructor-submissions-list-details` mục (6) |
| REQ-19 | **Máy chủ là nguồn của học viên và lớp.** Học viên và lớp hiện trên trang lấy từ lượt nộp; `studentId`, `classId` trên URL bị bỏ qua (chỉ gợi ý breadcrumb), nên URL giả mạo không làm hiện sai học viên hay lớp. **GWT:** Cho giảng viên mở lượt nộp của học viên H ở lớp L nhưng URL ghi `studentId` và `classId` của học viên khác, khi trang tải xong, thì thẻ ngữ cảnh vẫn hiện H và L | `DEC-2026-1014-instructor-submissions-list-details` mục (7) |

Bảng 10.1: Yêu cầu của vùng giảng viên trên trang kết quả bài nộp.

Quy tắc bảo vệ testcase ẩn (REQ-10, REQ-11 ở chương 5, F2-08) áp dụng nguyên vẹn cho giảng viên: vùng này không mở thêm
dữ liệu testcase nào. Phần này là [SoT: Suy luận]: các chương trước không nói về A2 và quyết định không nêu ngoại lệ.

BD `02-bd/screens/users/USR0201_submission_result.md` đã bổ sung mục tương ứng (V0.5, V0.6). Chưa xác định (không tự suy luận): mã `Fx-nn` cho vùng này chưa cấp (`DEC-2026-1014-instructor-submissions-list-rules`, mục (10)).
