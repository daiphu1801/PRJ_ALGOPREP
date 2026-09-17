# BD — Màn `admin_ai_usage` (Tiêu thụ token AI)

> Trục màn hình (`02-bd/screens/`), không phải trục Bounded Context. Slug khớp
> `01-rd/screens/admin/admin_ai_usage.md` [SoT: 01-rd/screens/admin/admin_ai_usage.md:1-4]. Actor: A3
> (Quản trị viên). Bounded Context cấp dữ liệu: `ai-review` (F5) — đọc cùng `02-bd/architecture/ai-review.md`
> mục 6 (kiểm soát chi phí), `02-bd/database/ai-review.md` mục 1.6-1.9, `02-bd/security/ai-review.md` mục 4
> trước khi dùng file này, vì các con số/bảng ở đây tham chiếu trực tiếp tới đó.
>
> Đúng anti-drift rule của `bd-generation`: file này **không** chứa DB schema, backend logic, hay request/
> response API — chỉ layout, component, state, danh sách API tiêu thụ (tên + BC sở hữu), điều hướng, quyền
> truy cập.

## 0. Hai cơ chế hiển thị trên màn này — không gộp

Theo đúng lưu ý của agent điều phối: màn này hiển thị **hai cơ chế tách biệt** của `ai-review`, không phải
một:

| Cơ chế | Nguồn dữ liệu | Bản chất | Hành động khi vượt ngưỡng |
| :--- | :--- | :--- | :--- |
| Ngân sách token tháng | `ai_token_budget_configs` [SoT: 02-bd/database/ai-review.md:135-146] | Khách quan theo số, có `locked_at` | **Tự động khoá** mọi lời gọi AI của `STUDENT`/`INSTRUCTOR` khi vượt hạn mức (F5-25) — chỉ `ADMIN` không bị khoá [SoT: 02-bd/architecture/ai-review.md:267-275] |
| Cảnh báo dùng bất thường theo tài khoản | `ai_usage_anomaly_alerts` [SoT: 02-bd/database/ai-review.md:147-161] | Cảnh báo mềm, cần con người xem xét | **Không tự khoá** — `ADMIN` xem rồi tự quyết định khoá tay qua `USER_MANAGEMENT` (màn `admin_user_management`, ngoài phạm vi file này) [SoT: 02-bd/architecture/ai-review.md:277-288, DEC-2026-0831-ai-usage-anomaly-alert] |

Hệ quả cho layout: khối "Cảnh báo" (mục 2.5 dưới) hiển thị cả hai loại cảnh báo trong cùng một danh sách UI
(prototype không tách cột) nhưng **mỗi thẻ cảnh báo giữ nguyên loại nguồn của nó** ở tầng dữ liệu — thẻ
"Sắp cạn hạn mức" đọc từ `ai_token_budget_configs` (dự báo), thẻ "Một tài khoản dùng bất thường" đọc từ
`ai_usage_anomaly_alerts` — component inventory mục 2.5 ghi rõ để DD không gộp hai nguồn thành một API.

## 1. Bố cục màn hình (layout regions)

Đối chiếu `09-layoutBase/Admin - Token AI.dc.html`, khung admin dùng chung (sidebar cố định + main content),
mục `AI` trong sidebar đang active ở mục con "Token AI" [SoT: 09-layoutBase/Admin - Token AI.dc.html:365-368].

| # | Vùng | Vị trí trong prototype | Ghi chú |
| :-: | :--- | :--- | :--- |
| R1 | Sidebar admin (dùng chung mọi màn Admin) | dòng 67-143 | Không thiết kế lại ở đây — khung chung, tham chiếu `02-bd/screens/admin/admin_overview.md` nếu đã có |
| R2 | Thanh tiêu đề dính đỉnh (sticky header) | dòng 147-163 | Tiêu đề "Tiêu thụ token AI" + mô tả, bộ chọn khoảng thời gian (7/14/30 ngày), nút đổi theme, nút "Xuất báo cáo" |
| R3 | Hàng 4 thẻ chỉ số tổng | dòng 165-176 | Token tháng này / Trung bình mỗi ngày / Chi phí tạm tính / Lượt gọi AI |
| R4 | Lưới 2 cột: biểu đồ + panel bên phải | dòng 178-243 | Cột trái (1.6fr): biểu đồ token theo ngày; cột phải (1fr): "Hạn mức tháng" (gồm block "Theo tính năng") xếp trên "Cảnh báo" |
| R5 | Lưới 2 cột dưới: hai bảng xếp hạng | dòng 246-305 | "Người học dùng nhiều nhất" (trái) và "Bài toán tốn token nhất" (phải) |
| R6 | Footer | dòng 307-320 | Phiên bản, dòng trạng thái "Số liệu cập nhật 5 phút một lần", các link tài liệu |

## 2. Kiểm kê thành phần (component inventory)

### 2.1. Thanh tiêu đề (R2)

- Tiêu đề tĩnh + mô tả tĩnh [SoT: 09-layoutBase/Admin - Token AI.dc.html:149-150].
- Segmented control chọn khoảng thời gian: `7 ngày` / `14 ngày` / `30 ngày`, mặc định `14 ngày`
  [SoT: 09-layoutBase/Admin - Token AI.dc.html:329, 443-449]. Đổi lựa chọn → nạp lại R3 và biểu đồ R4-trái
  theo khoảng đã chọn (`nDays`), không đổi phạm vi hai bảng xếp hạng R5 (hai bảng đó ghi chú cố định "30
  ngày gần nhất" [SoT: 09-layoutBase/Admin - Token AI.dc.html:252] — RD không nói rõ hai bảng này có đổi
  theo bộ lọc thời gian của header hay giữ cố định 30 ngày; BD giữ nguyên hành vi prototype: **cố định 30
  ngày, không đổi theo segmented control** — nếu chủ dự án muốn đồng bộ, cần một quyết định riêng).
- Nút "Xuất báo cáo" [SoT: 09-layoutBase/Admin - Token AI.dc.html:162] — RD không có mã `Fx-nn` cho hành vi
  xuất báo cáo; liệt kê Câu hỏi mở Q1 (mục 6).

### 2.2. Bốn thẻ chỉ số tổng (R3)

Mỗi thẻ: nhãn, giá trị lớn, delta phần trăm kèm màu (tăng/giảm so với kỳ trước), dòng meta phụ
[SoT: 09-layoutBase/Admin - Token AI.dc.html:451-456]:

| Thẻ | Nội dung mẫu trong prototype | Khớp mã RD |
| :--- | :--- | :--- |
| Token tháng này | `38,4 tr`, delta `+12%`, meta "Tính đến {giờ}" | F5-21 |
| Trung bình mỗi ngày | `1,74 tr`, delta `−4%` | F5-21 |
| Chi phí tạm tính | `182 USD`, meta "Hạn mức 240 USD mỗi tháng" | F5-21 (chi phí quy đổi từ token — cần tỉ giá/đơn giá theo model, chốt ở DD) |
| Lượt gọi AI | `9.418`, meta "N người học hoạt động" | F5-19, F5-21 |

Không thiết kế lại công thức delta ở đây (thuộc DD/logic).

### 2.3. Biểu đồ "Token theo ngày" (R4 trái)

Biểu đồ cột chồng (stacked bar), một cột mỗi ngày trong khoảng đã chọn, chú giải (legend) theo tính năng
[SoT: 09-layoutBase/Admin - Token AI.dc.html:180-206, 476-480]. **Cập nhật theo RD/decision đã chốt**:

- Dải "Gợi ý" **đã cắt khỏi phạm vi** (`DEC-2026-0831-remove-tiered-hints-ai-config`) — không dựng dải
  này khi lên Next.js thật, dù prototype còn vẽ [SoT: 01-rd/screens/admin/admin_ai_usage.md:8-10, 33-35].
- Ba dải còn lại thực dựng: **Phân tích** (Solution Review), **Phỏng vấn giả lập** (Mock Interview),
  **Sinh testcase** — ba `feature_code` có dữ liệu tiêu thụ thật theo `token_usage`
  [SoT: 02-bd/database/ai-review.md:104-118]. Prototype gốc chỉ vẽ 3 dải "Gợi ý/Phân tích/Phỏng vấn" và bỏ
  sót "Sinh testcase" dù khối 2.4 bên dưới có dòng riêng cho nó — RD đã ghi nhận đây là biểu đồ chỉ chọn
  hiện tính năng khối lượng lớn nhất, không phải xung đột dữ liệu [SoT: 01-rd/screens/admin/admin_ai_usage.md:31-35].
  Sau khi cắt "Gợi ý", BD đề xuất: **biểu đồ hiện đủ 3 tính năng còn lại** (Phân tích, Phỏng vấn, Sinh
  testcase) thay vì giữ nguyên cách chọn "3/4 lớn nhất" của prototype cũ — vì phạm vi giờ chỉ còn đúng 3
  tính năng AI, không cần lọc bớt. `[SoT: Suy luận]` — RD không nói rõ hành vi biểu đồ sau khi cắt "Gợi ý",
  đây là suy luận hợp lý nhất; xác nhận ở Câu hỏi mở Q2 (mục 6).
- Trục hoành: nhãn ngày `dd/mm`. Tooltip mỗi cột: tổng token quy đổi + ngày.

### 2.4. Panel "Hạn mức tháng" (R4 phải, trên)

[SoT: 09-layoutBase/Admin - Token AI.dc.html:208-229]

- Tiêu đề "Hạn mức tháng {tháng hiện tại}" + dòng phụ "Đã dùng X trong Y".
- Thanh tiến độ (progress bar) phần trăm đã dùng, nguồn `ai_token_budget_configs` (đã dùng lũy kế / hạn mức)
  [SoT: 02-bd/database/ai-review.md:135-146].
- Hai giá trị phụ: "Còn {số}" và "Dự kiến hết {ngày}" (dự báo tuyến tính theo tốc độ 7 ngày gần nhất)
  [SoT: 02-bd/architecture/ai-review.md:267-275]. Nếu đã `locked_at IS NOT NULL` (đã vượt hạn mức, đã tự
  khoá): dòng "Dự kiến hết {ngày}" đổi thành nhãn trạng thái tường minh **"Đã khoá lúc {locked_at}"** thay vì
  tiếp tục hiện ngày dự báo trong quá khứ — **screen state riêng, xem mục 3.4**. Prototype không có state
  này (dữ liệu mẫu luôn ở trạng thái chưa khoá, 74%) — bổ sung để khớp F5-25, ghi Câu hỏi mở Q3.
- Khối con "Theo tính năng" — danh sách N dòng, mỗi dòng: tên tính năng, giá trị token, thanh tiến độ %
  theo tổng tiêu thụ tháng [SoT: 09-layoutBase/Admin - Token AI.dc.html:482-487]. **Cắt dòng "Gợi ý theo
  bậc"** (`DEC-2026-0831-remove-tiered-hints-ai-config`) — 3 dòng còn lại giữ nguyên: Phân tích bài giải,
  Phỏng vấn giả lập, Sinh testcase [SoT: 01-rd/screens/admin/admin_ai_usage.md:38-41].
- Link "Sửa giới hạn tại Cấu hình AI" → điều hướng sang màn `admin_ai_config` (mục 4).

### 2.5. Panel "Cảnh báo" (R4 phải, dưới)

[SoT: 09-layoutBase/Admin - Token AI.dc.html:231-243, 488-497]

Danh sách thẻ cảnh báo, mỗi thẻ: tiêu đề, dòng mô tả (meta), màu nền/chữ theo mức độ (cảnh báo vàng /
nghiêm trọng đỏ). Ba loại thẻ xuất hiện trong dữ liệu mẫu, ánh xạ đúng hai nguồn của mục 0:

| Thẻ mẫu trong prototype | Nguồn dữ liệu thật | Mức độ hiển thị |
| :--- | :--- | :--- |
| "Sắp cạn hạn mức" — dự báo hết vào {ngày} | `ai_token_budget_configs` (dự báo, mục 2.4) | Vàng (cảnh báo, chưa khoá) |
| "Một tài khoản dùng bất thường" — {user} gọi {n} lượt/24h, gấp {x} lần trung bình | `ai_usage_anomaly_alerts` [SoT: 02-bd/database/ai-review.md:147-161] | Đỏ (nghiêm trọng — ngưỡng kích hoạt mặc định gấp 5 lần trung bình [SoT: 02-bd/database/ai-review.md:281] dù ví dụ minh hoạ ở đây dùng "gấp 7 lần" [SoT: 09-layoutBase/Admin - Token AI.dc.html:490]) |
| "Prompt sinh testcase tốn hơn dự kiến" | `[SoT: Suy luận]` — không khớp bảng nào đã thiết kế ở `02-bd/database/ai-review.md`; gần nhất là một ngưỡng chi phí trung bình/lượt theo `feature_code = TESTCASE_GENERATION`. Đây là tính năng "Sinh testcase" (F2-14), **không phải F5.1/F5.2** — chưa rõ module nào (`problem-bank` hay `ai-review`) sinh cảnh báo này. Liệt kê Câu hỏi mở Q4 (mục 6), không tự bịa bảng dữ liệu |

Mỗi thẻ loại `ai_usage_anomaly_alerts` có thể có một hành động phụ **"Đánh dấu đã xem"** (chuyển
`status: OPEN → REVIEWED`) — **không có trong prototype** (prototype chỉ hiển thị tĩnh, không có nút hành
động trên thẻ cảnh báo [SoT: 09-layoutBase/Admin - Token AI.dc.html:234-239]) nhưng cột `status`/
`reviewed_at` đã tồn tại ở schema với đúng mục đích này [SoT: 02-bd/database/ai-review.md:157]. BD đề xuất
bổ sung nút này khi dựng UI thật — nếu không có cách nào đóng một cảnh báo, danh sách chỉ dài dần vô hạn.
Câu hỏi mở Q5 (mục 6).

Dòng "Sửa giới hạn tại Cấu hình AI" cuối panel — cùng link điều hướng như mục 2.4.

### 2.6. Bảng "Người học dùng nhiều nhất" (R5 trái)

[SoT: 09-layoutBase/Admin - Token AI.dc.html:248-276] — bảng xếp hạng, cột: `#`, Người học (avatar chữ cái
đầu + tên + khoá học), Token, Lượt, Chi phí. Link "Xem tất cả" ở góc — đích đến chưa rõ, xem Câu hỏi mở Q6.
Khớp F5-25 ("bảng xếp hạng người dùng... tốn token nhiều nhất") [SoT: 01-rd/screens/admin/admin_ai_usage.md:45-46].

### 2.7. Bảng "Bài toán tốn token nhất" (R5 phải)

[SoT: 09-layoutBase/Admin - Token AI.dc.html:278-304] — cột: `#`, Bài toán (tên + độ khó + "N lượt gợi ý"),
Token, TB/lượt. Ghi chú phụ đề "Gợi ý và phân tích cộng lại" [SoT: 09-layoutBase/Admin - Token AI.dc.html:282]
— **prototype cũ**, cần đổi vì "Gợi ý" đã cắt khỏi phạm vi; BD đề xuất đổi phụ đề thành "Phân tích bài giải"
hoặc "Tất cả tính năng cộng lại" (bao gồm cả Sinh testcase) — chốt ở Câu hỏi mở Q7. Link "Ngân hàng bài" →
điều hướng sang `problem_management` (mục 4).

### 2.8. Footer (R6)

Tĩnh, không tiêu thụ API riêng — dòng trạng thái "Số liệu cập nhật 5 phút một lần" [SoT: 09-layoutBase/Admin - Token AI.dc.html:307-320]
ngụ ý toàn màn làm mới theo chu kỳ polling 5 phút, không phải realtime qua WebSocket/SSE — hợp lý vì đây là
dữ liệu tổng hợp thống kê, không phải trạng thái một submission đơn lẻ đang chấm. `[SoT: Suy luận]` — RD
không nói cơ chế làm mới, suy ra từ đúng câu chữ hiển thị trong prototype.

## 3. Trạng thái màn hình (screen states)

| # | Trạng thái | Mô tả |
| :-: | :--- | :--- |
| S1 | Đang tải (loading) | Mỗi khối (R3/R4/R5) hiện skeleton độc lập — prototype dùng `hint-placeholder-count` cho từng danh sách (4 thẻ, N ngày, 4 tính năng, 3 cảnh báo, 6 dòng mỗi bảng) [SoT: 09-layoutBase/Admin - Token AI.dc.html:166, 193, 217, 234, 260, 290], nghĩa là các khối tải độc lập, không chờ nhau |
| S2 | Đã tải, có dữ liệu (mặc định) | Toàn bộ mục 2 hiển thị đầy đủ |
| S3 | Rỗng — không có cảnh báo | Panel "Cảnh báo" (2.5) không có thẻ nào — hiện một dòng trung tính "Không có cảnh báo nào" thay vì để trống hẳn. `[SoT: Suy luận]` — prototype luôn có 3 thẻ mẫu, không minh hoạ trạng thái rỗng |
| S4 | Đã khoá do vượt ngân sách (F5-25) | Khi `ai_token_budget_configs.locked_at IS NOT NULL` — panel "Hạn mức tháng" (2.4) đổi màu thanh tiến độ sang cảnh báo (đỏ/cam) và hiện nhãn "Đã khoá lúc {thời điểm}" thay cho "Dự kiến hết {ngày}". Đây là trạng thái riêng biệt với S5, không gộp — mục 0 |
| S5 | Có cảnh báo bất thường mới (chưa `REVIEWED`) | Không đổi bố cục, chỉ ảnh hưởng nội dung panel 2.5 — không tạo banner toàn màn để tránh nhầm với S4 (khoá thật) |
| S6 | Lỗi tải dữ liệu | Một khối lỗi (ví dụ hết hạn phiên, lỗi mạng) hiện thông báo lỗi cục bộ trong đúng khối đó kèm nút "Thử lại" — không kéo sập toàn màn vì các khối độc lập (đúng nguyên tắc suy giảm có kiểm soát của `ai-review`, mục kiến trúc 7, áp dụng tương tự cho tầng hiển thị: một API con lỗi không được chặn các khối khác) |

## 4. API tiêu thụ (chỉ liệt kê — hợp đồng đầy đủ ở `03-dd/api/ai-review.md`, chưa viết)

| # | Tên endpoint (đề xuất, chốt ở DD) | Phục vụ khối | BC sở hữu |
| :-: | :--- | :--- | :--- |
| A1 | `GetAiUsageSummary` | R3 (4 thẻ), R2 header meta | `ai-review` |
| A2 | `GetAiUsageDailyBreakdown` | R4 trái (biểu đồ theo ngày, theo tính năng) | `ai-review` |
| A3 | `GetAiTokenBudgetStatus` | R4 phải trên (Hạn mức tháng + Theo tính năng) | `ai-review` |
| A4 | `ListAiUsageAlerts` | R4 phải dưới (Cảnh báo) — trả cả hai loại nguồn của mục 0, phân biệt bằng một trường loại cảnh báo | `ai-review` |
| A5 | `MarkAiUsageAlertReviewed` | R4 phải dưới, hành động đề xuất mục 2.5 (chưa có trong prototype) | `ai-review` |
| A6 | `ListTopAiUsers` | R5 trái | `ai-review` |
| A7 | `ListTopAiProblems` | R5 phải | `ai-review` (đọc tên bài toán cần ghép với `problem-bank`, cách ghép — DTO đọc thẳng port hay `ai-review` tự cache tên bài — chốt ở DD) |
| A8 | `ExportAiUsageReport` | R2 (nút "Xuất báo cáo") | Chưa rõ — không có mã RD, Câu hỏi mở Q1 |

Không endpoint nào trong danh sách trên có quyền ghi ảnh hưởng `locked_at` hay dữ liệu `judge`/`problem` —
đúng ranh giới đã chốt ở `02-bd/security/ai-review.md` mục 3.

## 5. Điều hướng (navigation)

- Vào từ sidebar admin, nhóm "AI" → mục con "Token AI" (đang active)
  [SoT: 09-layoutBase/Admin - Token AI.dc.html:365-368].
- Ra: "Sửa giới hạn tại Cấu hình AI" (mục 2.4, 2.5) → màn `admin_ai_config`
  [SoT: 09-layoutBase/Admin - Token AI.dc.html:241].
- Ra: "Ngân hàng bài" (mục 2.7) → màn `problem_management`
  [SoT: 09-layoutBase/Admin - Token AI.dc.html:284].
- Ra: "Xem tất cả" (mục 2.6) → đích chưa xác định, Câu hỏi mở Q6.
- Không có điều hướng vào chi tiết một `solution_review`/`interview_session` cụ thể từ màn này — đúng giới
  hạn "không hiển thị testcase ẩn/nội dung chi tiết" tinh thần chung của README (áp dụng loại suy: màn thống
  kê AI cấp quản trị không cần xem lại toàn văn một phiên phỏng vấn cụ thể của học viên, tránh vượt phạm vi
  `AI_CONTENT_AUDIT` chưa được thiết kế — `02-bd/security/ai-review.md` mục 2, mục 6).

## 6. Quyền truy cập (access rights)

- Actor duy nhất: **A3 (Quản trị viên)**. Không có lối vào cho A1/A2/A4
  [SoT: 01-rd/screens/admin/admin_ai_usage.md:4].
- Gác theo ma trận phân quyền Role × Function × Action (F1-10 → F1-12): Function `AI_TOKEN_BUDGET`,
  action `READ`, cho toàn bộ nội dung xem trên màn này (R2-R6) — phân biệt với action `UPDATE` của cùng
  Function dùng ở màn `admin_ai_config` để **sửa** hạn mức [SoT: 01-rd/req/identity.md:55-61,
  02-bd/security/ai-review.md:79-82]. Hành động A5 (`MarkAiUsageAlertReviewed`, mục 4) là một action ghi
  nhẹ (đổi `status`) — đề xuất gác bằng `AI_TOKEN_BUDGET:UPDATE` thay vì tạo action mới, vì bản chất vẫn là
  "quản trị cấu hình/giám sát chi phí AI"; nếu chủ dự án muốn tách bạch xem/đánh dấu, cần một action riêng
  (`AI_TOKEN_BUDGET:REVIEW_ALERT`) — mở ở Câu hỏi mở Q5.
- Không có Function `AI_CONFIG` nào gác nội dung màn này — đó là Function của `admin_ai_config` (sửa
  prompt/rubric), một Function khác dù cùng nhóm sidebar "AI"
  [SoT: 02-bd/security/ai-review.md:79-82].
- Dữ liệu hiển thị không có phạm vi "theo lớp" như `instructor_grading` — đây là cấu hình/thống kê toàn hệ
  thống, không lọc theo lớp giảng viên phụ trách [SoT: 02-bd/security/ai-review.md:47-49].

## 7. Câu hỏi mở

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | Nút "Xuất báo cáo" (R2) xuất định dạng gì (CSV/PDF), phạm vi dữ liệu nào? | RD không có mã `Fx-nn` cho hành vi này, chỉ có trong prototype [SoT: 09-layoutBase/Admin - Token AI.dc.html:162] | Đề xuất CSV, phạm vi = đúng khoảng thời gian đang chọn ở R2 (7/14/30 ngày) | Chủ dự án |
| Q2 | Biểu đồ R4-trái sau khi cắt "Gợi ý" nên hiện đủ 3 tính năng còn lại hay giữ logic "top N tính năng lớn nhất"? | Prototype vẽ trước khi quyết định cắt "Gợi ý theo bậc"; RD chỉ ghi nhận việc cắt, không nói lại hành vi biểu đồ | BD đề xuất hiện đủ 3 tính năng còn lại (mục 2.3) | Chủ dự án |
| Q3 | Khi đã khoá do vượt ngân sách (S4), panel "Hạn mức tháng" có cần thêm hành động nhanh (ví dụ nút "Mở khoá") ngay tại đây, hay bắt buộc đi qua `admin_ai_config`? | RD/prototype không có state này | BD đề xuất giữ chỉ đọc ở màn này, hành động mở khoá thuộc `admin_ai_config` — tránh hai nơi cùng sửa `ai_token_budget_configs` | Chủ dự án |
| Q4 | Thẻ cảnh báo "Prompt sinh testcase tốn hơn dự kiến" (mục 2.5) thuộc cơ chế/module nào — có bảng dữ liệu chưa? | Không khớp bảng nào ở `02-bd/database/ai-review.md`; có thể thuộc `problem-bank` (F2-14) chứ không phải `ai-review` | Cần một phiên riêng xác định module sở hữu trước khi đưa vào DD | Chủ dự án + agent phụ trách `problem-bank`/`ai-review` |
| Q5 | Cảnh báo bất thường (`ai_usage_anomaly_alerts`) có cần nút "Đánh dấu đã xem" ngay trên màn này không, hay chỉ xem thụ động? | Prototype không có nút hành động trên thẻ cảnh báo dù schema đã có cột `status`/`reviewed_at` | BD đề xuất bổ sung nút, action `AI_TOKEN_BUDGET:UPDATE` hoặc action riêng | Chủ dự án |
| Q6 | Link "Xem tất cả" (mục 2.6, bảng "Người học dùng nhiều nhất") dẫn tới đâu — một trang danh sách đầy đủ riêng, hay `admin_user_management` có lọc theo token? | Prototype chỉ có `href="#"` [SoT: 09-layoutBase/Admin - Token AI.dc.html:254] | Đề xuất: trang con cùng màn (phân trang), tránh văng sang màn quản lý người dùng khác ngữ cảnh | Chủ dự án |
| Q7 | Phụ đề bảng "Bài toán tốn token nhất" ("Gợi ý và phân tích cộng lại") cần đổi thành gì sau khi cắt "Gợi ý"? | Câu chữ prototype cũ không còn đúng phạm vi | BD đề xuất "Phân tích và phỏng vấn cộng lại" hoặc liệt kê rõ 3 tính năng | Chủ dự án |

## 8. Tham chiếu

- `01-rd/screens/admin/admin_ai_usage.md` — RD của màn.
- `09-layoutBase/Admin - Token AI.dc.html` — prototype, SoT bằng chứng layout.
- `02-bd/architecture/ai-review.md` — mục 6 (kiểm soát chi phí), mục 7 (suy giảm có kiểm soát).
- `02-bd/database/ai-review.md` — mục 1.6-1.9 (`token_usage`, `ai_token_budget_configs`,
  `ai_usage_anomaly_alerts`).
- `02-bd/security/ai-review.md` — mục 2, 4 (quyền sở hữu dữ liệu, kiểm soát chi phí khía cạnh bảo mật).
- `01-rd/req/identity.md` — F1-10 → F1-12 (ma trận phân quyền, Function `AI_TOKEN_BUDGET`/`AI_CONFIG`).
- `.nexa/control/decision-registry.md` — `DEC-2026-0831-ai-usage-anomaly-alert`,
  `DEC-2026-0831-remove-tiered-hints-ai-config`.

## 9. Việc còn mở — chuyển sang DD

- Hợp đồng API đầy đủ (request/response) cho 8 endpoint mục 4 — `03-dd/api/ai-review.md`.
- Công thức "chi phí tạm tính" (USD) quy đổi từ token theo `model_name`/đơn giá — DD/logic.
- Cách A7 (`ListTopAiProblems`) lấy tên bài toán — gọi trực tiếp `ProblemContextPort` hay một port đọc riêng
  cho mục đích thống kê (khác port đọc ngữ cảnh chấm bài) — chốt cùng `problem-bank` khi cả hai vào DD.
- Bảy câu hỏi mở ở mục 7.

**Status:** DONE_WITH_CONCERNS
**Summary:** Đã viết BD cho màn `admin_ai_usage` tại `02-bd/screens/admin/admin_ai_usage.md` — layout 6 vùng, kiểm kê thành phần theo từng khối, 6 trạng thái màn hình (gồm trạng thái đã khoá ngân sách S4 và cảnh báo bất thường S5, giữ tách biệt đúng yêu cầu), 8 API tiêu thụ (chỉ tên + BC sở hữu), điều hướng, và quyền truy cập theo Function `AI_TOKEN_BUDGET`.
**Concerns/Blockers:** Bảy câu hỏi mở cần chủ dự án xác nhận trước khi viết DD, trong đó Q4 (cảnh báo "Prompt sinh testcase tốn hơn dự kiến") là quan trọng nhất vì chưa rõ module sở hữu — có thể là khoảng trống giữa `ai-review` và `problem-bank` tương tự khoảng trống `known_optimal_complexity` đã ghi ở BD kiến trúc `ai-review` mục 3.3. Không có gì chặn cứng để tiếp tục sang DD nếu chủ dự án chấp nhận các đề xuất `[SoT: Suy luận]` tạm thời.
