# RD — Màn `submission_result` (Kết quả nộp bài)

> Slug: `submission_result` — khớp `01-rd/overview/system_survey.md` mục 7.1 dòng `submission_result`
> [SoT: 01-rd/overview/system_survey.md:476]. Bounded Context: `judge-orchestration` (F4), có đọc thêm từ
> `problem-bank` (siêu dữ liệu bài toán, độ khó) [SoT: .nexa/control/dependency-map.md:125 — dòng
> `submission_result`]. Actor: A1 (chính — chủ sở hữu bài nộp).
>
> `system_survey.md` chốt rõ **`problem_detail` và `submission_result` là hai màn riêng, không gộp**: kết
> quả một bài nộp cần URL riêng để mở lại được từ trang tiến độ/lịch sử [SoT:
> 01-rd/overview/system_survey.md:498-499].
>
> Đối chiếu prototype: `09-layoutBase/Kết quả nộp bài.dc.html`. File này mô tả **hành vi và UX ở mức yêu
> cầu** — không lặp lại đặc tả chức năng đã có ở `01-rd/req/req.md` (mục F4) và
> `01-rd/req/user_stories.md` (`US-A1-04`), chỉ trỏ tới và bổ sung phần đặc thù của màn.

## 1. Mục đích màn hình

Trang xem lại kết quả của **một bài nộp cụ thể** sau khi đã có `submissionId`: verdict tổng (Accepted/Wrong
Answer/Time Limit Exceeded...), thống kê chạy (thời gian, bộ nhớ), chi tiết theo từng testcase, mã nguồn đã
nộp, và lối vào hai luồng AI sau khi `Accepted` (Phân tích bài giải, Phỏng vấn giả lập)
[SoT: 01-rd/req/user_stories.md:82-96 — US-A1-04; 01-rd/overview/system_survey.md:476].

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Nộp bài trả `submissionId` ngay, không chờ kết quả chấm | F4-01 | `01-rd/req/req.md:207-211` |
| Gọi `JudgeExecutionPort` từng testcase, dừng sớm khi có lỗi (fail-fast) | F4-03, F4-04 | `01-rd/req/req.md:214-215` |
| Đẩy trạng thái từng testcase qua WebSocket (STOMP) theo kênh riêng của bài nộp | F4-08 | `01-rd/req/req.md:227` |
| Chống rò rỉ testcase ẩn: chỉ trả trạng thái và chỉ số, không trả input/diff chi tiết | F2-08 | `01-rd/req/req.md:127-129` |
| Ánh xạ lỗi biên dịch về đúng dòng mã người dùng; che giấu hoàn toàn mã harness | F3-11, F3-12 | `01-rd/req/req.md:177-178` |
| Sau `Accepted`, mở hai lựa chọn: Phân tích bài giải hoặc Phỏng vấn giả lập | F5-01, F5-09 | `01-rd/req/user_stories.md:95-96` |
| Given-When-Then đầy đủ cho luồng nộp bài và xem kết quả thời gian thực | — | `01-rd/req/user_stories.md:82-96` (`US-A1-04`) |

## 3. Trạng thái và cấu trúc màn (screen states)

Đối chiếu `09-layoutBase/Kết quả nộp bài.dc.html`:

1. **Banner verdict tổng** — chấm màu, nhãn verdict viết hoa, ghi chú (sai từ testcase số mấy / đúng toàn bộ
   kèm thời điểm nộp), tên bài và độ khó [SoT: 09-layoutBase/Kết quả nộp bài.dc.html:111-122]. Editor mẫu chỉ
   khai báo **ba** giá trị verdict: `Accepted`, `Wrong Answer`, `Time Limit Exceeded`
   [SoT: 09-layoutBase/Kết quả nộp bài.dc.html:238] — không có `Compile Error` hay `Runtime Error` dù cả hai
   đều là verdict hợp lệ theo F3-11/F3-12 (ánh xạ lỗi biên dịch) và theo F4-09a (đã nhắc "Runtime Error/TLE"
   như loại kết quả cũ có thể lọc khi chấm lại) — đã chốt ở Câu hỏi mở Q3 (đã đóng), chưa dựng vào prototype.
2. **Thanh thống kê chạy** (`runStats`) — 5 chỉ số: Testcase (đạt/tổng), Runtime, Memory, "Beats" (tỉ lệ % nhanh
   hơn các bài nộp khác), và Cách nộp (`wrapper`/`stdio`, khớp F3-13) [SoT:
   09-layoutBase/Kết quả nộp bài.dc.html:123-131, 314-320]. Bốn chỉ số rỗng (`—`) khi bài nộp không
   `Accepted` [SoT: 09-layoutBase/Kết quả nộp bài.dc.html:316-319]. Chỉ số "Beats" chưa có mã `Fx-nn` nào mô
   tả cách tính — đã chốt ở Câu hỏi mở Q4 (đã đóng): mã **F4-12** mới, xem `01-rd/req/req.md`.
3. **Khối "Testcase đầu tiên bị sai"** — chỉ hiện khi có lỗi, tách hai biến thể theo loại testcase gây sai
   (`hasFailureVisible`/`hasFailureHidden`): nếu testcase gây sai là **mẫu** (`sample: true`), hiện đủ ba ô
   Input/Expected/"Kết quả của bạn" như cũ; nếu là **ẩn** (`sample: false`), chỉ hiện một dòng trạng thái +
   chỉ số (ví dụ "Testcase #05 · WA"), không còn ba ô chi tiết [SoT:
   09-layoutBase/Kết quả nộp bài.dc.html:134-153]. **Đã sửa xong lỗi vi phạm F2-08 nêu ở Câu hỏi mở Q1** (đã
   chốt qua hỏi trực tiếp chủ dự án, đúng theo đề xuất trong bảng câu hỏi mở): trong dữ liệu mẫu, testcase gây
   sai (`failAt = 5`) là testcase ẩn thứ 5, nay chỉ hiện trạng thái, không còn lộ Input/Expected/Output
   [SoT: 09-layoutBase/Kết quả nộp bài.dc.html:329, 334].
4. **Bảng "Chi tiết testcase"** — tab lọc Tất cả/Ví dụ/Bị sai (`caseTabs`), cột #, Input, Expected, Output,
   Time, Kết quả (AC/WA/TLE/`—` khi bị bỏ qua do fail-fast) [SoT:
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
   [SoT: 01-rd/overview/system_survey.md:482 — dòng `my_submissions`, F1-07].
8. **Nút "Sửa lại và nộp tiếp"** — quay về Workspace (`problem_detail`) để sửa mã và nộp lại
   [SoT: 09-layoutBase/Kết quả nộp bài.dc.html:231].
9. **Không có trạng thái "đang chấm" (`PENDING`/`RUNNING`)** — toàn bộ state trong prototype là tĩnh, tính từ
   một `verdict` cho sẵn (`Accepted`/`Wrong Answer`/`Time Limit Exceeded`, dòng 238, 277-278); không có biến
   trạng thái nào mô phỏng việc nhận cập nhật từng testcase qua WebSocket khi bài nộp còn đang chạy — đã
   chốt ở Câu hỏi mở Q2 (đã đóng): có tự subscribe, chưa dựng vào prototype tĩnh này.

Hai chế độ hiển thị không đổi hành vi nghiệp vụ, chỉ đổi văn bản/field: `data-ui-lang="vi|en"` và
`data-theme="light|dark"` [SoT: 09-layoutBase/Kết quả nộp bài.dc.html:262-271], theo đúng
`DEC-2026-0824-i18n-vi-en` và `DEC-2026-0824-dark-light-theme`.

## 4. Given-When-Then bổ sung ở mức màn (không trùng `US-A1-04`)

- **Cho** tôi mở trực tiếp URL của một bài nộp đã có kết quả cuối (từ lịch sử hoặc đường dẫn chia sẻ), **Khi**
  trang tải xong, **Thì** tôi thấy ngay đầy đủ banner verdict, thống kê chạy, bảng testcase và code đã nộp mà
  không cần chờ một sự kiện WebSocket nào — khác với lúc bài nộp còn đang chấm dở (US-A1-04 mô tả trường hợp
  đó) [SoT: Suy luận — hệ quả tất yếu của việc `submission_result` là trang xem lại có URL riêng
  (01-rd/overview/system_survey.md:498-499), chưa có Given-When-Then riêng nào xác nhận].
- **Cho** testcase gây sai là một testcase ẩn (`Hidden`, F2-06), **Khi** tôi xem khối "Testcase đầu tiên bị
  sai" hoặc bảng "Chi tiết testcase", **Thì** tôi chỉ thấy trạng thái (`WA`/`TLE`) và chỉ số thứ tự của
  testcase đó, không thấy Input, không thấy Expected, không thấy Output của tôi — **đã khớp prototype sau khi
  sửa Câu hỏi mở Q1** [SoT: 01-rd/req/req.md:127-129; 09-layoutBase/Kết quả nộp bài.dc.html:148-153, 346-359].
- **Cho** testcase gây sai là một testcase mẫu (`Sample`, F2-05), **Khi** tôi xem hai khối trên, **Thì** tôi
  được thấy đầy đủ Input/Expected/Output vì testcase mẫu vốn công khai, không thuộc phạm vi bảo vệ của F2-08
  [SoT: 01-rd/req/req.md:125 — phân biệt Sample/Hidden].
- **Cho** bài nộp có verdict `Compile Error`, **Khi** tôi xem trang kết quả, **Thì** tôi thấy lỗi biên dịch
  được ánh xạ về đúng dòng trong mã tôi đã viết, không lộ bất kỳ dòng mã harness nào (F3-11, F3-12) — trạng
  thái này **chưa có trong prototype** — đã chốt ở Câu hỏi mở Q3 (đã đóng): khối "Lỗi biên dịch" thay thế
  khối "Testcase đầu tiên bị sai" khi verdict là `Compile Error` [SoT: 01-rd/req/req.md:177-178].

## 5. Câu hỏi mở

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | ~~Prototype hiện hiển thị Input/Expected/Output đầy đủ cho testcase ẩn khi nó gây sai...~~ **ĐÃ CHỐT (phiên 2026-08-25)**: không có ngoại lệ nghiệp vụ nào — đây là lỗi dựng prototype, đã sửa trực tiếp `09-layoutBase/Kết quả nộp bài.dc.html` theo đúng đề xuất: testcase `sample: false` hiển thị placeholder "Ẩn"/"Hidden" ở cột Input/Expected/Output (giữ cột #, Time, Kết quả); khối "Testcase đầu tiên bị sai" khi rơi vào testcase ẩn chỉ còn một dòng trạng thái + chỉ số [SoT: 09-layoutBase/Kết quả nộp bài.dc.html:148-153, 346-359]. | — | Đã sửa, không cần hành động thêm. | Đã đóng |
| Q2 | ~~Màn `submission_result` có cần tự nhận cập nhật qua WebSocket khi người dùng mở đúng URL này lúc bài nộp còn đang ở trạng thái `PENDING`/đang chấm...~~ **ĐÃ CHỐT (phiên 2026-08-25, chốt theo RD):** có — `submission_result` tự subscribe kênh WebSocket của đúng `submissionId` khi tải trang nếu trạng thái bài nộp chưa phải trạng thái cuối, tái dùng đúng cơ chế F4-08, tránh phải bắt người dùng ở lại `problem_detail` mới thấy tiến độ. **Không có state nào cần dựng thêm ở prototype tĩnh này** — hành vi thuộc tầng kết nối thật, chỉ có ý nghĩa khi build FE Next.js thật. | — | Đã chốt quyết định. | Đã đóng |
| Q3 | ~~Verdict `Compile Error` và `Runtime Error` chưa có trạng thái nào được dựng trong prototype...~~ **ĐÃ CHỐT (phiên 2026-08-25, chốt theo RD):** thêm hai trạng thái verdict mới vào UI thật — `Compile Error` (thay khối "Testcase đầu tiên bị sai" bằng khối "Lỗi biên dịch" hiện thông điệp lỗi + số dòng, ẩn hẳn bảng testcase vì chưa có testcase nào chạy) và `Runtime Error` (giữ bảng testcase, thêm badge mô tả loại lỗi runtime ở testcase gây lỗi, vẫn tuân F2-08 nếu đó là testcase ẩn). **Chưa dựng vào `09-layoutBase/Kết quả nộp bài.dc.html`** (enum verdict vẫn chỉ 3 giá trị) — để lúc build FE Next.js thật. | — | Đã chốt quyết định, chưa dựng prototype. | Đã đóng |
| Q4 | ~~Chỉ số "Beats"...chưa có mã `Fx-nn` nào mô tả cách tính...~~ **ĐÃ CHỐT (phiên 2026-08-25, chốt theo RD):** đã thêm **F4-12** vào `01-rd/req/req.md` — tính theo runtime, so sánh trong tập bài nộp `Accepted` của cùng bài toán và cùng ngôn ngữ (không so chéo ngôn ngữ), chỉ hiển thị khi verdict là `Accepted` [SoT: 01-rd/req/req.md — F4-12]. | — | Đã thêm mã F4-12 vào `req.md`. | Đã đóng |

## 6. Ngoài phạm vi file này

- Bảng màu, spacing, component cụ thể, breakpoint responsive — thuộc BD (`02-bd/screens/users/submission_result.md`,
  chưa viết).
- Hợp đồng API (lấy chi tiết một bài nộp, kênh WebSocket theo `submissionId`) — thuộc DD
  (`03-dd/api/judge-orchestration.md`, chưa viết).
- Thuật toán so khớp kết quả (exact/whitespace/epsilon/set — F3-07 tới F3-10), cơ chế fail-fast, timeout
  sweep — thuộc logic của `judge-orchestration`/`harness`, không thuộc file theo trục màn này.
- Nội dung và bố cục của `solution_review` và `mock_interview` (hai màn đích của cột hành động tiếp theo) —
  thuộc các file RD riêng của chúng.

## 7. Tham chiếu

- `01-rd/req/req.md:127-129, 177-178, 207-227` — F2-08, F3-11, F3-12, F4-01, F4-03, F4-04, F4-08.
- `01-rd/req/user_stories.md:82-96` — `US-A1-04`.
- `01-rd/overview/system_survey.md:476, 498-499` — dòng `submission_result` trong bảng màn mục 7.1.
- `.nexa/control/dependency-map.md:125` — Bounded Context chạm bởi `submission_result`.
- `09-layoutBase/Kết quả nộp bài.dc.html` — prototype.
- `06-plan/nexa-plan/260824-2043-bd-screens-common-first.md` — kế hoạch Phase 1 sinh ra file này.
