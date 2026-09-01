# RD — Màn `solution_review` (Phân tích bài giải)

> Slug: `solution_review` — khớp `01-rd/overview/system_survey.md` mục 7.1 dòng `solution_review`
> [SoT: 01-rd/overview/system_survey.md:479]. Bounded Context: `ai-review` (F5.1)
> [SoT: .nexa/control/dependency-map.md:127 — dòng `solution_review` · `mock_interview`]. Actor: A1 (chính
> — chủ sở hữu bài nộp).
>
> Theo lộ trình Phase 3 của `06-plan/nexa-plan/260824-2043-bd-screens-common-first.md` dòng 44: "Một lượt,
> không có state hội thoại — rẻ hơn F5.2, theo Stage 3" của `dependency-map.md` mục 4.
>
> Đối chiếu prototype: `09-layoutBase/Phân tích bài giải.dc.html`. File này mô tả **hành vi và UX ở mức yêu
> cầu** — không lặp lại đặc tả chức năng đã có ở `01-rd/req/ai-review.md` (mục F5.1) và
> `01-rd/req/user_stories/a1_student.md` (`US-A1-06`), chỉ trỏ tới và bổ sung phần đặc thù của màn.

## 1. Mục đích màn hình

Trang hiện **báo cáo phân tích một lượt** (không hội thoại) do AI sinh ra sau khi một bài nộp đạt
`Accepted`: độ phức tạp thời gian/bộ nhớ thực tế đối chiếu tối ưu đã biết, điểm mạnh, điểm cần sửa, một bản
mã đề xuất cải tiến (diff), và lối vào tiếp sang Phỏng vấn giả lập cho cùng bài nộp
[SoT: 01-rd/req/user_stories/a1_student.md — `US-A1-06`; 01-rd/overview/system_survey.md:479].

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Yêu cầu phân tích bài giải vừa nộp | F5-01 | `01-rd/req/ai-review.md` — F5-01 |
| Phân tích độ phức tạp thời gian/bộ nhớ thực tế kèm lập luận, đối chiếu tối ưu đã biết | F5-02, F5-03 | `01-rd/req/ai-review.md` — F5-02, F5-03 |
| Trường hợp biên chưa phủ, giả định ngầm, rủi ro tràn số; nhận xét chất lượng mã; câu hỏi mở rộng | F5-04, F5-05, F5-06 | `01-rd/req/ai-review.md` — F5-04, F5-05, F5-06 |
| Trả JSON có lược đồ để render báo cáo tĩnh; lưu kèm bài nộp, tra cứu lại từ trang tiến độ | F5-07, F5-08 | `01-rd/req/ai-review.md` — F5-07, F5-08 |
| Chống prompt injection: mã nguồn là tham số dữ liệu, tách khỏi chỉ thị hệ thống | F5-17 | `01-rd/req/ai-review.md` — F5-17 |
| Định hướng giáo dục: báo cáo là phản hồi học tập, không phải điểm chính thức — phải hiện rõ trên giao diện | F5-18 | `01-rd/req/ai-review.md` — F5-18 |
| Cache theo hash mã nguồn — nộp lại đúng mã đó không gọi lại API | F5-20 | `01-rd/req/ai-review.md` — F5-20 |
| Vượt ngân sách token → tự động tạm khoá gọi AI cho `STUDENT`/`INSTRUCTOR` | F5-25 | `01-rd/req/ai-review.md` — F5-25 |
| Suy giảm có kiểm soát: AI hỏng/hết quota không ảnh hưởng F1-F4 | F5-22 | `01-rd/req/ai-review.md` — F5-22 |
| Áp dụng bản mã AI đề xuất vào Workspace, xác nhận trước khi ghi đè, giữ lại bản cũ | F5-26 | `01-rd/req/ai-review.md` — F5-26 |
| Given-When-Then đầy đủ cho luồng nhận và dùng báo cáo phân tích | — | `01-rd/req/user_stories/a1_student.md` (`US-A1-06`) |

## 3. Trạng thái và cấu trúc màn (screen states)

Đối chiếu `09-layoutBase/Phân tích bài giải.dc.html`:

1. **Breadcrumb** — `Kết quả nộp bài / SUB-2841 / Phân tích bài giải`, xác nhận màn này được vào từ
   `submission_result` với một `submissionId` cụ thể, đúng như `nextActions` đã mô tả ở
   `01-rd/screens/users/submission_result.md` mục 3.6 [SoT: 09-layoutBase/Phân tích bài giải.dc.html:99-105].
2. **Thanh chỉ số đầu trang** (`topStats`) — 4 ô: tên bài + độ khó, "Hướng giải" (tối ưu/chưa tối ưu + tên kỹ
   thuật), **"Dễ đọc" dạng điểm số `4 / 5`**, và thời điểm sinh báo cáo [SoT:
   09-layoutBase/Phân tích bài giải.dc.html:107-117, 288-293]. **Đã chốt 2026-08-25** (Câu hỏi mở Q1, cũ):
   giữ điểm số này, do AI tự chấm thang 1-5 trong cùng lượt phân tích, là một trường bổ sung của F5-07/F5-05
   [SoT: 01-rd/req/ai-review.md — F5-05].
3. **Nút "Phỏng vấn về bài này"** ở góc phải thanh chỉ số — dẫn thẳng sang `mock_interview` cho cùng bài nộp
   [SoT: 09-layoutBase/Phân tích bài giải.dc.html:116], khớp luồng "hai lựa chọn AI" của
   `01-rd/overview/system_survey.md` mục 3.2.
4. **Khối cảnh báo định hướng giáo dục** — dòng chữ "Báo cáo do AI sinh một lượt... Là phản hồi để học,
   không phải điểm chính thức" ngay đầu báo cáo [SoT: 09-layoutBase/Phân tích bài giải.dc.html:124-127],
   đúng yêu cầu bắt buộc hiện rõ của F5-18.
5. **Nhận xét tổng quan** (`summary`) — đoạn văn tự do do AI sinh [SoT:
   09-layoutBase/Phân tích bài giải.dc.html:128-131, 295-297] — ứng với F5-02/F5-03 gộp lại thành một đoạn
   văn, không tách hai trường JSON riêng trên giao diện.
6. **Bảng "Độ phức tạp"** — hai dòng Thời gian/Bộ nhớ, mỗi dòng có cột "Bài của bạn", "Tốt nhất đã biết", và
   "Ghi chú" [SoT: 09-layoutBase/Phân tích bài giải.dc.html:134-158, 299-304] — khớp trực tiếp F5-02/F5-03.
7. **Hai cột "Làm tốt" / "Nên sửa"** — cột trái liệt kê điểm mạnh dạng bullet tự do; cột phải liệt kê điểm
   cần sửa, mỗi dòng gắn nhãn số dòng mã nguồn (`L2`, `L4`, `L8`) [SoT:
   09-layoutBase/Phân tích bài giải.dc.html:160-183, 306-316] — khớp F5-04/F5-05. Việc gắn số dòng vào từng
   điểm cần sửa là chi tiết trình bày có thật trong prototype nhưng `req.md` không nêu — không phải xung đột,
   chỉ là một cách hiển thị hợp lý của F5-05.
8. **Khối "Bản đề xuất"** — hiện diff (thêm/bớt dòng, tô màu) giữa mã đã nộp và bản AI đề xuất, kèm ghi chú
   tóm tắt số dòng đổi và độ phức tạp không đổi [SoT: 09-layoutBase/Phân tích bài giải.dc.html:185-198,
   248-261, 318-323]. Đây là dữ liệu nguồn cho hành động F5-26 (nút "Áp bản đề xuất trong Workspace" ở cuối
   cột phải, dòng 229) — bản thân diff không phải một mã `Fx-nn` riêng, chỉ là cách trình bày trước khi áp
   dụng.
9. **Khối "Code đã nộp"** — hiện toàn bộ mã nguồn đã nộp kèm tô màu từ khoá và nhãn ngôn ngữ + thời gian chạy
   [SoT: 09-layoutBase/Phân tích bài giải.dc.html:201-215, 237-246]. Dữ liệu của chính người học, không thuộc
   phạm vi bảo vệ F2-08.
10. **Khối "Kiến thức nên ôn"** — 3 liên kết có `id` dạng `IQ-0xx` và tên câu hỏi cụ thể, mỗi liên kết trỏ
    sang `./Câu hỏi phỏng vấn.dc.html` (tức `interview_bank_list`) [SoT:
    09-layoutBase/Phân tích bài giải.dc.html:217-227, 327-331]. **Đã chốt 2026-08-25** (Câu hỏi mở Q2, cũ):
    các `id` cố định (`IQ-014`...) trong prototype chỉ là dữ liệu mẫu tĩnh của bản dựng tham khảo, **không**
    phải yêu cầu liên kết cứng tới bản ghi thật. F5-06 sinh **chủ đề/từ khoá gợi ý**, không phải id câu hỏi;
    giao diện tự dựng liên kết tìm kiếm sang `interview_bank_list` lọc theo chủ đề đó — `ai-review` không
    đọc dữ liệu của `interview-bank`, giữ đúng nguyên tắc "modules never import each other"
    (`DEC-2026-0820-architecture-baseline`) [SoT: 01-rd/req/ai-review.md — F5-06]. Số lượng liên kết hiện ra, bố
    cục cụ thể của khối này **[Đợi nextjs]** — dựng lại khi có frontend thật, không tiếp tục đối chiếu
    prototype tĩnh cho chi tiết này.
11. **Không có trạng thái đang chờ AI xử lý (loading) và không có trạng thái lỗi** (AI hết quota theo F5-22,
    bị khoá theo ngân sách F5-25, hoặc lỗi gọi model) — toàn bộ prototype là một báo cáo tĩnh đã có sẵn dữ
    liệu, không có biến trạng thái nào mô phỏng việc đang gọi AI hay việc gọi thất bại — xem Câu hỏi mở Q3.

Hai chế độ hiển thị không đổi hành vi nghiệp vụ, chỉ đổi văn bản/field: `data-ui-lang="vi|en"` và
`data-theme="light|dark"` [SoT: 09-layoutBase/Phân tích bài giải.dc.html:30-31, 264-286], theo đúng
`DEC-2026-0824-i18n-vi-en` và `DEC-2026-0824-dark-light-theme`.

## 4. Given-When-Then bổ sung ở mức màn (không trùng `US-A1-06`)

- **Cho** tôi mở `solution_review` cho một bài nộp đã có báo cáo (từ cache theo F5-20 hoặc đã sinh trước
  đó), **Khi** trang tải xong, **Thì** tôi thấy ngay đầy đủ báo cáo mà không cần chờ một cuộc gọi AI mới nào
  [SoT: 01-rd/req/ai-review.md — F5-20].
- **Cho** báo cáo chưa từng được sinh cho bài nộp này, **Khi** tôi vào màn từ `submission_result`, **Thì**
  tôi cần thấy một trạng thái đang xử lý trong lúc hệ thống gọi AI — trạng thái này **chưa có trong
  prototype**, xem Câu hỏi mở Q3 [SoT: Suy luận — hệ quả tất yếu của việc gọi AI là một thao tác bất đồng bộ
  có độ trễ, `ai-review.md` không mô tả UX chờ].
- **Cho** ngân sách token AI đã cạn cho vai trò của tôi (F5-25), **Khi** tôi bấm yêu cầu phân tích một bài
  nộp mới, **Thì** tôi cần thấy thông báo rõ ràng là tính năng AI đang tạm khoá, không phải một lỗi hệ thống
  chung chung — trạng thái này **chưa có trong prototype**, xem Câu hỏi mở Q3 [SoT: 01-rd/req/ai-review.md
  — F5-25].
- **Cho** báo cáo có đề xuất mã cải tiến, **Khi** tôi bấm "Áp bản đề xuất trong Workspace", **Thì** hệ thống
  hỏi xác nhận trước khi ghi đè và giữ lại bản mã cũ của tôi để khôi phục nếu đổi ý — hành vi xác nhận/lưu
  bản cũ diễn ra ở `problem_detail` (Workspace), không phải trên chính màn `solution_review` [SoT:
  01-rd/req/ai-review.md — F5-26; 01-rd/screens/users/problem_detail.md].

## 5. Câu hỏi mở

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | ~~"Dễ đọc: 4 / 5" ở thanh chỉ số đầu trang là điểm số định lượng, nhưng F5-05 chỉ mô tả "nhận xét chất lượng mã" ở dạng định tính...~~ **ĐÃ CHỐT (2026-08-25, tự quyết theo yêu cầu chủ dự án — prototype là bản dựng tham khảo)**: giữ điểm số, thang 1-5, do AI tự chấm trong cùng lượt phân tích (không gọi thêm), là một trường bổ sung của F5-05/F5-07 [SoT: 01-rd/req/ai-review.md — F5-05; 01-rd/overview/system_survey.md:321]. | — | Đã chốt, không cần hành động thêm. | Đã đóng |
| Q2 | ~~Khối "Kiến thức nên ôn" liên kết trực tiếp tới 3 câu hỏi cụ thể có `id` thật (`IQ-014`, `IQ-071`, `IQ-033`)...~~ **ĐÃ CHỐT (2026-08-25, tự quyết theo yêu cầu chủ dự án — prototype là bản dựng tham khảo, sẽ dựng lại khi có frontend Next.js thật)**: các `id` cố định trong prototype chỉ là dữ liệu mẫu; F5-06 sinh **chủ đề/từ khoá gợi ý**, không phải liên kết cứng tới bản ghi thật của `interview-bank` — `ai-review` không đọc dữ liệu `interview-bank`, giữ đúng "modules never import each other" (`DEC-2026-0820-architecture-baseline`); giao diện tự dựng liên kết tìm kiếm sang `interview_bank_list` theo chủ đề [SoT: 01-rd/req/ai-review.md — F5-06; 01-rd/overview/system_survey.md:322]. Số lượng liên kết hiện ra và bố cục cụ thể **[Đợi nextjs]**. | — | Đã chốt hướng kiến trúc; chi tiết trình bày chờ dựng UI thật. | Đã đóng |
| Q3 | Trạng thái đang chờ AI xử lý (loading) và trạng thái lỗi (hết quota F5-22, bị khoá ngân sách F5-25, lỗi gọi model) hoàn toàn chưa được dựng trong prototype — toàn bộ file là một báo cáo tĩnh đã có sẵn dữ liệu. | Prototype không có biến trạng thái nào mô phỏng việc đang gọi AI hay việc gọi thất bại; đây là khoảng trống UX cần bổ sung khi dựng UI thật, không phải xung đột với RD. | **[Đợi nextjs]** — thêm hai trạng thái màn khi dựng frontend thật: "đang phân tích" (skeleton/spinner, có thể kèm ước lượng thời gian) và "không thể phân tích" (phân biệt lỗi tạm thời — cho thử lại — với bị khoá do ngân sách F5-25 — không cho thử lại, chỉ hiện lý do). Không cần chốt nghiệp vụ gì thêm, chỉ là việc dựng UI còn thiếu. | Đợi nextjs |

## 6. Ngoài phạm vi file này

- Bảng màu, spacing, component cụ thể, breakpoint responsive — thuộc BD (`02-bd/screens/users/solution_review.md`,
  chưa viết).
- Hợp đồng API (yêu cầu phân tích, lấy lại báo cáo đã lưu) và lược đồ JSON chi tiết của báo cáo — thuộc DD
  (`03-dd/api/ai-review.md`, chưa viết).
- Cơ chế cache theo hash mã nguồn (F5-20), rate limit (F5-19), ngân sách token (F5-25), cấu hình prompt/rubric
  (F5-23) — thuộc logic của `ai-review`, không thuộc file theo trục màn này.
- Nội dung và bố cục của `mock_interview` (đích của nút "Phỏng vấn về bài này") và của `problem_detail`
  (đích của nút "Áp bản đề xuất trong Workspace") — thuộc các file RD riêng của chúng.
- Chi tiết trình bày cụ thể của khối "Kiến thức nên ôn" (số lượng liên kết, bố cục) và của hai trạng thái
  loading/lỗi (Q3) — hướng nghiệp vụ đã chốt ở mục 5, phần dựng UI cụ thể **[Đợi nextjs]**.

## 7. Tham chiếu

- `01-rd/req/ai-review.md` — F5-01 tới F5-08, F5-17, F5-18, F5-20, F5-22, F5-25, F5-26.
- `01-rd/req/user_stories/a1_student.md` — `US-A1-06`.
- `01-rd/overview/system_survey.md:479` — dòng `solution_review` trong bảng màn mục 7.1.
- `.nexa/control/dependency-map.md:127` — Bounded Context chạm bởi `solution_review`.
- `.nexa/control/decision-registry.md` — `DEC-2026-0820-architecture-baseline` (nguyên tắc modules không
  import lẫn nhau, liên quan Câu hỏi mở Q2).
- `09-layoutBase/Phân tích bài giải.dc.html` — prototype.
- `06-plan/nexa-plan/260824-2043-bd-screens-common-first.md` — kế hoạch Phase 3 sinh ra file này.
- `06-plan/reports/260825-2100-ai2-solution-review-conflicts.md` — báo cáo xung đột riêng (AI-2), để các
  phiên chạy song song khác (Phase 0-1, Phase 2) biết mà tránh giẫm chân.
