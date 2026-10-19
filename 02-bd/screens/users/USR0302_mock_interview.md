# Tài liệu thiết kế cơ bản (BD) — Phỏng vấn giả lập (`USR0302`)

## Phương châm tài liệu [Nội bộ — không cần khách duyệt]

- Viết theo **mẫu 9 sheet**. Bộ thẻ đóng, quy ước ID, ký hiệu chuẩn, ranh giới với tài liệu khác và
  checklist kiểm toán nằm ở `02-bd/_rules/bd-template-9sheet.md` — file này không chép lại.
- Phần gắn nhãn `[Nội bộ]` phục vụ sinh mã và tự kiểm toán, không phải yêu cầu của khách hàng: mục 4.5,
  mục 7.3 và các ghi chú quy ước.
- Mã màn `USR0302` theo bảng mã ở `02-bd/_rules/bd-template-9sheet.md` mục 8 dòng 143; tên file mang tiền
  tố mã.
- Khung điều hướng **không mô tả lại ở đây** — dùng chung `02-bd/screens/users/_shell.md`. Màn này có mục
  điều hướng trực tiếp trên thanh nav chính của header (Mục thứ 5: "Phỏng vấn giả lập")
  [Nguồn: 02-bd/screens/users/_shell.md:58].
- Màn này có **một popup**: xác nhận kết thúc phiên phỏng vấn sớm trước khi hết số lượt.

> Đọc cùng `01-rd/screens/users/USR0302_mock_interview.md` (hành vi ở mức yêu cầu, không lặp lại ở đây),
> `01-rd/req/ai-review.md` (F5-09→F5-16, F5-17, F5-18, F5-19, F5-22, F5-24, F5-25, F5-28),
> `01-rd/req/user_stories/a1_student.md` (`US-A1-07`), `02-bd/architecture/ai-review.md`,
> `02-bd/database/ai-review.md`, `02-bd/security/ai-review.md`.
>
> - **Mô hình 3 trạng thái**: màn hình quản lý 3 trạng thái rõ rệt trong cùng một URL:
>   1. `entry`: Cửa sổ chọn nguồn bài phỏng vấn và cấu hình phiên.
>   2. `running`: Giao diện hội thoại tương tác 1:1 trực tiếp với AI qua SSE streaming.
>   3. `result`: Báo cáo kết quả và chấm điểm Rubric 4 tiêu chí khi kết phiên.
> - **Ba giai đoạn phỏng vấn (F5-10..F5-12)**: Giải trình thuật toán → Phản biện ca biên & bẫy → Mở rộng quy mô
>   (Scale-up). AI tự quyết định chuyển giai đoạn dựa trên ngữ cảnh hội thoại.
> - **Ngữ cảnh qua Redis (`ChatMemory` F5-13)**: duy trì toàn bộ lịch sử các lượt thoại trong phiên để AI nắm
>   bắt mạch trao đổi; stream từng token phản hồi qua Server-Sent Events (SSE F5-14).
> - **Rubric 4 tiêu chí cố định (F5-15)**: Độ rõ ràng — `CLARITY` (25%), Độ chính xác kỹ thuật —
>   `TECHNICAL_ACCURACY` (30%), Khả năng phản biện — `PUSHBACK_HANDLING` (25%), Nhận thức độ phức tạp —
>   `COMPLEXITY_AWARENESS` (20%) — trọng số cố định, chốt tại `02-bd/database/ai-review.md:96-97`.
> - **Ba lối vào phiên (F5-24)**: (1) Bài nộp `Accepted` gần đây; (2) Ngân hàng câu hỏi phỏng vấn F6; (3) Tự chọn
>   chủ đề tự do.
> - **Bảo mật và chống injection (F5-17)**: mã nguồn và câu trả lời của người học luôn được bọc trong block
>   dữ liệu người dùng, tách biệt khỏi chỉ thị hệ thống của AI.

> **Quy ước đặt tên khối** [Nội bộ]. Tám khối (khối `entryStats` đã bỏ 2026/10/18, owner): `entryTabs`
> (3 tab chọn lối vào bài phỏng vấn), `entryConfig` (tham số cấu hình phiên F5-28), `runningHeader` (thanh tiến
> trình 3 giai đoạn và đồng hồ), `runningDrawer` (ngăn kéo xem lại đề bài và mã nguồn gốc), `runningChat` (khung
> chat dòng thời gian SSE), `runningControls` (ô nhập câu trả lời, nút gửi, nút xin gợi ý, nút dừng sớm),
> `earlyExitModal` (popup xác nhận kết thúc sớm), `resultSummary` (bảng điểm Rubric 4 tiêu chí và nhận xét chi tiết).
> Tiền tố ID item của toàn màn là `mockInterview.`.

---

## Sheet 1. Bìa

| Mục | Giá trị |
| :--- | :--- |
| Hệ thống | AlgoPrep |
| Phân hệ | `ai-review` (F5.2) — đọc từ `interview-bank` (F6), `problem-bank` (F2), liên kết từ `judge-orchestration` (F4) |
| Công đoạn | BD — Thiết kế cơ bản |
| Phân loại | Màn hình |
| Tên màn | Phỏng vấn giả lập |
| Mã màn hình | `USR0302` |
| Tên vật lý (slug) | `mock_interview` |
| Trục tài liệu | Màn hình (`02-bd/screens/`) |
| Actor | A1 (`STUDENT`) |
| Phiên bản | V0.10 |
| Người tạo | Nhóm phát triển AlgoPrep |
| Ngày tạo | 2026/09/22 |
| Người cập nhật | Claude |
| Ngày cập nhật | 2026/10/19 |

---

## Sheet 2. Lịch sử sửa đổi

| Ver | Sheet bị sửa | Nội dung sửa | Ngày | Người sửa |
| :--- | :--- | :--- | :--- | :--- |
| V0.1 | Toàn bộ | Tạo mới theo mẫu 9 sheet. Chốt quản lý 3 trạng thái (entry/running/result), 3 lối vào F5-24, tiến trình 3 giai đoạn F5-10..F5-12, stream phản hồi SSE F5-14, rubric 4 tiêu chí F5-15 trọng số 25/30/25/20%, và popup xác nhận dừng sớm | 2026/09/22 | Nhóm phát triển AlgoPrep |
| V0.2 | Sheet 1 (header), Sheet 5, Sheet 6, Sheet 7 | Sửa lỗi review: (1) gỡ trích dẫn bịa `DEC-2026-0913-interview-rubric-fixed-weights` (không tồn tại trong decision-registry), thay bằng `02-bd/database/ai-review.md:96-97`; (2) đổi tên 4 tiêu chí rubric đúng theo RD/database — Kỹ thuật→Độ rõ ràng (`CLARITY`), Giao tiếp→Độ chính xác kỹ thuật (`TECHNICAL_ACCURACY`), Giải quyết vấn đề→Khả năng phản biện (`PUSHBACK_HANDLING`), Chất lượng mã→Nhận thức độ phức tạp (`COMPLEXITY_AWARENESS`), áp dụng cho toàn bộ field/ID/nhãn liên quan; (3) xác nhận `interviewer_level` 4 mức theo `DEC-2026-0922-users-and-admin-conflict-resolutions`, đóng Câu hỏi mở Q1 cũ; (4) viết lại Sheet 5 và Sheet 7 đúng khuôn mẫu 9 sheet (thêm cột Bảng DB/Cột DB ở Sheet 5 và 7.1, Sheet 7.3 bỏ method/path HTTP); (5) phát hiện và ghi Câu hỏi mở Q4 mới — schema chưa có cột lưu `overallScore`/`feedbackSummary` tổng hợp | 2026/09/24 | Nhóm phát triển AlgoPrep |
| V0.3 | Sheet 8, 9 | Bổ sung toast khi kết thúc phiên (EVT-14, EVT-18) và đổi báo hết hạn mức khi bắt đầu phiên sang toast; giữ nguyên nút Thử tải lại và popup xác nhận dừng sớm. Theo `DEC-2026-1003-toast-feedback-channel`. | 2026/10/03 | AI |
| V0.4 | Câu hỏi mở (Q4) | Áp `DEC-2026-1014-drop-ai-score-10`: câu so sánh Q4 với cột điểm AI quy đổi thang 10 của Solution Review không còn đúng vì cột đã bỏ, nên được viết lại; nội dung Q4 và đề xuất không đổi. Cách đánh số giữ nguyên | 2026/10/14 | AI |
| V0.5 | Sheet 5 (NO 2, 1), Sheet 7 (NO 5) | Áp `DEC-2026-1014-batch7-remaining-open-questions` (ponytail bậc 5): bỏ cột `rubric_scores.weight_percent_snapshot`; điểm tổng phiên dùng hằng số trọng số F5-15, cột nguồn chỉ còn `score`. Không đổi F5-15 | 2026/10/14 | AI |
| V0.6 | Quy ước đặt tên khối, Sheet 4 (4.4, 4.5), Sheet 5, Sheet 6, Sheet 7 (7.1, 7.3), Sheet 8 (EVT-1), Câu hỏi mở (thêm Q5) | Owner 2026/10/18: bỏ dải 3 thẻ thống kê (StatCard) ở trạng thái `entry` theo quy tắc owner 2026/10/13 "không dải KPI nhỏ trên màn prototype". Bỏ Khu vực A cũ (3 item `statsSessions`, `statsAvgScore`, `statsWeakest`), endpoint `GetMyInterviewStats`, khối `entryStats`, file `entry-stats-banner.tsx`; EVT-1 không còn gọi API. Khu vực B..I đánh lại thành A..H; sửa tham chiếu chéo. Ghi Câu hỏi mở Q5 (RD còn yêu cầu thanh chỉ số) | 2026/10/18 | AI |
| V0.7 | Sheet 4 (4.4), Câu hỏi mở Q5 | Owner 2026/10/18 (`DEC-2026-1018-usr-open-questions-phase1-scope` mục 1, ponytail bậc 1): RD `USR0302` đã bỏ mục "Thanh chỉ số"; đóng Q5, ghi F5-16 còn được đáp ứng ở bảng "Phiên gần đây" của RD và ở `USR0501`/`USR0601`. Trích dẫn `mock-interview-view.tsx:63-72` đã đọc lại. Không đổi Sheet 5-9 vì BD đã bỏ dải thống kê từ V0.6 | 2026/10/18 | Claude |
| V0.8 | Sheet 3, 4 (4.4), 6, 8 (EVT-1), Câu hỏi mở Q2, Q3, Q4 | Ghi các quyết định owner ngày 2026-10-18 (`DEC-2026-1018-usr-open-questions-phase2-5`, owner lấy đề xuất mặc định): đóng Q2 (tải lại trang khi phiên đang chạy: lưu `sessionId` vào `sessionStorage`, khôi phục bằng `GetSessionDetail` đọc từ Redis; bậc 5), Q3 (một lượt bỏ trống 15 phút thì đóng phiên và chuyển sang `result`; bậc 5), Q4 (`overallScore` tính ở tầng ứng dụng từ 4 dòng `rubric_scores`, `feedbackSummary` ghép từ 4 `comment`, không thêm cột; bậc 5, `02-bd/database/ai-review.md` do bên khác sửa nên chỉ tham chiếu theo tên). Owner bỏ lối vào "Phỏng vấn câu này" từ `USR0401`: bỏ dòng chuyển màn NO 4 và cạnh mermaid ở Sheet 3, bỏ tham số URL `question_id` (Sheet 6 A.2, EVT-1); các lối vào còn lại giữ nguyên (mục "Phỏng vấn giả lập" trên thanh nav, nút "Vào phiên giả lập" ở đầu `USR0401`, `submission_id` từ `USR0201`/`USR0301`). Lối "Kho câu hỏi" ngay trong màn (chọn một câu trong danh sách rút gọn rồi bấm "Bắt đầu phỏng vấn") giữ nguyên. Ghi nợ prototype ở mục 4.4: bản dựng giữ phiên trong bộ nhớ, chưa có `sessionStorage` và chưa có hẹn giờ 15 phút. Bản dựng không có đường tiền điền `question_id` trên URL nên không phải sửa mã | 2026/10/18 | Claude |
| V0.9 | Sheet 4 (4.4), Sheet 5 (Khu vực A NO 4, NO 5; Khu vực F NO 1) | Owner 2026/10/19 (rà soát prototype nhóm C): (1) tab "Tự chọn chủ đề" có **chip chủ đề** đọc từ danh mục chủ đề, chưa tick chủ đề nào thì không bắt đầu được (Sheet 9 NO 3 chạy thật); (2) danh sách nguồn chỉ hiện tiêu đề, **không hiện mã** `SUB-nnnn` hay `IQ-nnn`; (3) **hoãn** ngăn kéo "Xem đề và mã" ở trạng thái `running` (C20), ghi nợ prototype; (4) ô nhập tối đa 4000 ký tự, Ctrl+Enter để gửi, ô nhập `readOnly` thay vì `disabled` khi AI đang gõ; (5) vùng tin nhắn `role=log`, chỉ báo "AI đang gõ" `role=status`; (6) xoá ô nhập khi phiên kết thúc hoặc bắt đầu lại | 2026/10/19 | AI |
| V0.10 | Sheet 4 (4.4), Câu hỏi mở Q2, Q3 | Owner chốt 2026/10/19 (`DEC-2026-1019-usr-fe-audit-fixes`, chốt nốt câu hỏi mở còn lại sau rà prototype). Chỉ tài liệu, không đổi code, chưa duyệt hình. Q2 (khôi phục phiên khi tải lại) và Q3 (hết giờ 15 phút) không dựng ở prototype, đóng và hoãn sang API thật (ponytail bậc 3). Tab Trình độ: số mức lấy từ mức độ khó admin cấu hình động, RD F5-28 sửa theo. | 2026/10/19 | Claude |

---

## Sheet 3. Sơ đồ chuyển màn

| STT | Màn hình nguồn | Màn hình đích | Tham số truyền | Ghi chú |
| :-: | :--- | :--- | :--- | :--- |
| 1 | Khung điều hướng (`_shell`) | Màn hình hiện tại (`USR0302`) | Không có | [Điều kiện mở] Bấm mục "Phỏng vấn giả lập" trên thanh nav chính.<br>[Chế độ mở] Điều hướng URL `/mock-interview`.<br>[Thông tin truyền] Không có.<br>[Giá trị trả về] Không có.<br>[Khi thành công] Mở màn hình ở trạng thái `entry`.<br>[Khi huỷ] Không có. |
| 2 | `submission_result` (`USR0201`) | Màn hình hiện tại (`USR0302`) | `submission_id` | [Điều kiện mở] Bấm "Phỏng vấn giả lập" từ màn kết quả nộp bài.<br>[Chế độ mở] Điều hướng URL `/mock-interview?submission_id={submission_id}`.<br>[Thông tin truyền] `submission_id`.<br>[Giá trị trả về] Không có.<br>[Khi thành công] Tự động chọn tab "Bài nộp đã Accepted" và nạp sẵn cấu hình.<br>[Khi huỷ] Không có. |
| 3 | `solution_review` (`USR0301`) | Màn hình hiện tại (`USR0302`) | `submission_id` | [Điều kiện mở] Bấm "Phỏng vấn về bài này" từ màn phân tích.<br>[Chế độ mở] Điều hướng URL `/mock-interview?submission_id={submission_id}`.<br>[Thông tin truyền] `submission_id`.<br>[Giá trị trả về] Không có.<br>[Khi thành công] Tự động chọn tab "Bài nộp đã Accepted" và nạp sẵn cấu hình.<br>[Khi huỷ] Không có. |
| 4 | Màn hình hiện tại (`USR0302`) | `interview_bank_list` (`USR0401`) | Không có | [Điều kiện mở] Bấm link "Xem toàn bộ N câu hỏi" tại tab Kho câu hỏi.<br>[Chế độ mở] Điều hướng sang `/interview-bank`.<br>[Thông tin truyền] Không có.<br>[Giá trị trả về] Không có.<br>[Khi thành công] Mở danh sách ngân hàng câu hỏi.<br>[Khi huỷ] Giữ nguyên màn hiện tại. |

```mermaid
flowchart LR
    SHELL["_shell (Nav: Phỏng vấn)"] --> MI["USR0302 mock_interview"]
    SR["USR0201 submission_result"] -->|Nộp bài Accepted| MI
    SOL["USR0301 solution_review"] -->|Phỏng vấn bài này| MI
    MI -->|Xem toàn bộ kho| IB
```

[Ghi chú 2026/10/18] Lối vào "Phỏng vấn câu này" từ `USR0401` đã **bỏ** (owner, `DEC-2026-1018-usr-open-questions-phase2-5`, mục theo dõi): không có chuyển màn từ `USR0401` kèm `question_id`. Từ `USR0401` chỉ còn nút "Vào phiên giả lập" ở đầu trang mở `/mock-interview` không tham số, ngoài mục "Phỏng vấn giả lập" trên thanh nav. Câu hỏi F6 vẫn chọn được ngay trong màn, ở tab "Kho câu hỏi".

---

## Sheet 4. Bố cục màn hình

### 4.1. Tổng quan theo thẻ

- `[Mục đích màn]` Cung cấp môi trường luyện phỏng vấn kỹ thuật trực tiếp 1:1 với AI, mô phỏng quy trình phỏng vấn chuẩn của các công ty công nghệ lớn, đánh giá kỹ năng diễn giải và phản biện thuật toán.
- `[Luồng nghiệp vụ chính]` Người học chọn một chủ đề phỏng vấn (từ bài nộp, kho câu hỏi, hoặc chủ đề tự do), cấu hình tham số phiên rồi bắt đầu. Trong phiên, AI đặt câu hỏi qua 3 giai đoạn (Giải trình, Phản biện, Mở rộng); người học trả lời qua ô nhập liệu. Khi kết phiên (hết lượt hoặc chủ động dừng), AI chấm điểm theo Rubric 4 tiêu chí và đưa ra lộ trình cải thiện.
- `[Người dùng]` Học viên (A1).
- `[Tệp liên quan]` `01-rd/screens/users/USR0302_mock_interview.md`, `09-layoutBase/Phỏng vấn giả lập.dc.html`.
- `[Phạm vi]` Một phiên phỏng vấn đơn lẻ từ lúc bắt đầu cấu hình đến khi hoàn tất và xem điểm. Tải lại trang khi phiên đang chạy thì khôi phục phiên qua `sessionStorage` + `GetSessionDetail` (Q2); một lượt bỏ trống 15 phút thì đóng phiên và sang `result` (Q3).
- `[Quyền sử dụng]` Yêu cầu đăng nhập học viên; kiểm tra hạn mức ngân sách token cá nhân trước khi mở phiên.
- `[Số bản ghi tối đa]` Mỗi phiên tối đa 16 lượt hỏi đáp (turn); lịch sử chat lưu toàn văn trong phiên.

### 4.2. Danh sách cấu trúc dữ liệu màn hình (DTOs)

| STT | Tên DTO | Mô tả | Chi tiết trường |
| :-: | :--- | :--- | :--- |
| 1 | `InterviewSessionInitDto` | Yêu cầu khởi tạo phiên | `entryMode`, `submissionId`, `questionId` (chọn trong tab "Kho câu hỏi" ngay trong màn, không đến từ URL), `customTopic`, `difficultyLevel`, `maxTurns`, `hintAllowed` |
| 2 | `InterviewSessionDetailDto` | Chi tiết phiên đang chạy | `sessionId`, `currentStage`, `currentTurn`, `maxTurns`, `startTime`, `elapsedSeconds`, `problemContext`, `sourceCode` |
| 3 | `InterviewMessageDto` | Một tin nhắn trong khung chat | `id`, `sender` (AI/USER), `content`, `stage`, `turnIndex`, `sentAt`, `isStreaming` |
| 4 | `InterviewResultSummaryDto` | Kết quả tổng kết phiên | `sessionId`, `overallScore`, `clarityScore`, `technicalAccuracyScore`, `pushbackHandlingScore`, `complexityAwarenessScore`, `feedbackSummary`, `strengths`, `improvements` |

### 4.3. Danh sách bảng dữ liệu

| STT | Tên bảng | Mô tả | Thao tác | Bounded Context |
| :-: | :--- | :--- | :--- | :--- |
| 1 | `ai.interview_sessions` | Lưu trữ phiên phỏng vấn và kết quả rubric | C, R, U | `ai-review` |
| 2 | `ai.interview_turns` | Lưu trữ chi tiết từng lượt hỏi đáp | C, R | `ai-review` |
| 3 | `interview.interview_questions` | Câu hỏi từ kho F6 khi mở từ lối 2 | R | `interview-bank` |
| 4 | `judge.submissions` | Mã nguồn bài nộp khi mở từ lối 1 | R | `judge-orchestration` |

### 4.4. Vùng bố cục bám prototype

Màn hình thay đổi bố cục linh hoạt theo 3 trạng thái:

| Ký hiệu | Tên khu vực | Tham chiếu prototype | Ghi chú |
| :-: | :--- | :--- | :--- |
| **A** | 3 tab chọn lối vào (Entry Tabs) | `Phỏng vấn giả lập.dc.html:118-121` | Trạng thái `entry`: Bài Accepted / Kho câu hỏi / Tự chọn |
| **B** | Cấu hình phiên (Entry Config) | `Phỏng vấn giả lập.dc.html:166-196` | Trạng thái `entry`: Chọn cấp độ, số lượt, gợi ý (F5-28) |
| **C** | Thanh tiến trình 3 giai đoạn (Running Header) | `Phỏng vấn giả lập.dc.html:202-218` | Trạng thái `running`: Giải trình → Phản biện → Mở rộng |
| **D** | Ngăn kéo đề bài & mã nguồn (Running Drawer) | `Phỏng vấn giả lập.dc.html:220-235` | Trạng thái `running`: Drawer trượt ra xem lại mã đã nộp |
| **E** | Khung hội thoại streaming (Running Chat) | `Phỏng vấn giả lập.dc.html:240-310` | Trạng thái `running`: Luồng chat Markdown với AI |
| **F** | Thanh tương tác chat (Running Controls) | `Phỏng vấn giả lập.dc.html:315-340` | Trạng thái `running`: Ô nhập text, nút gửi, xin gợi ý, dừng sớm |
| **G** | Popup xác nhận dừng phiên sớm | `[SoT: Suy luận]` | Hộp thoại cảnh báo kết thúc trước khi đủ số lượt |
| **H** | Bảng điểm Rubric & nhận xét (Result Summary) | `Phỏng vấn giả lập.dc.html:350-410` | Trạng thái `result`: Scorecard 4 tiêu chí và đánh giá |

`[Hiện trạng bản dựng]` (đọc lại 2026/10/18) Trạng thái `entry` trong `05-coding/frontend/src/views/users/mock-interview/ui/mock-interview-view.tsx` mở đầu bằng thẻ chọn lối vào (3 tab và danh sách nguồn, dòng 66-143) rồi thẻ cấu hình phiên (dòng 145-200); **dải 3 thẻ thống kê (số phiên, điểm TB, tiêu chí yếu) đã bỏ 2026-10-18, owner** (quy tắc owner 2026/10/13). Khu vực A cũ, endpoint `GetMyInterviewStats` và EVT-1 bước tải thống kê bị bỏ theo; RD đã sửa theo, xem Câu hỏi mở Q5 (đã chốt 2026-10-18). Trạng thái `running` (khung chat) gắn `data-fullscreen-view` nên khung student cho nó toàn bộ chiều cao còn lại (bỏ `calc(100vh - 84px)` viết cứng, 2026-10-18); trạng thái `entry` và `result` vẫn là trang cuộn thường.

`[Hiện trạng bản dựng]` **Tab Trình độ (chốt 2026/10/19, owner):** số mức ở tab và ở danh sách "Mức người phỏng vấn" lấy từ danh sách mức độ khó **admin cấu hình động** (`DEC-2026-1001-admin-configurable-settings`), không cố định 3 hay 4 mức; RD F5-28 đã sửa câu chữ theo. Bản dựng hiện còn dùng hằng `INTERVIEWER_LEVELS` (`05-coding/frontend/src/entities/mock-interview/model/types.ts:71`), chưa đọc danh sách động: nợ prototype.

`[Hiện trạng bản dựng]` **Cập nhật 2026/10/19 (rà soát nhóm C):** tab "Tự chọn chủ đề" đã có chip chủ đề (`mock-interview-view.tsx`, nhóm `role=group`, nhãn từ `useInterviewTopics`) và nút Bắt đầu chỉ bật khi tick ít nhất một chủ đề (`use-mock-interview-session.ts`, `canStart`); hai danh sách nguồn chỉ hiện tiêu đề, không hiện mã. **Nợ còn lại:** (e) khu vực D (ngăn kéo "Xem đề và mã", nút C.6, EVT-11, EVT-12) **chưa có** ở trạng thái `running`: owner hoãn 2026/10/19, làm khi có dữ liệu đề bài và mã nộp thật; (f) câu hỏi kịch bản, gợi ý và nhận xét kết quả là chuỗi tiếng Việt cố định trong mock, không qua `t()`, hiện cả ở giao diện `en`.

`[Hiện trạng bản dựng]` **Nợ prototype so với các quyết định 2026/10/18** (đọc `model/use-mock-interview-session.ts` và `ui/mock-interview-view.tsx`, chưa đổi mã): (a) Q2 chưa có: phiên và tin nhắn nằm trong bộ nhớ (`useState`, `use-mock-interview-session.ts:34-52`), không có `sessionStorage`, tải lại trang là mất phiên và quay về `entry`; (b) Q3 chưa có: chỉ có đồng hồ đếm giờ phiên (`:96`) và hiệu ứng stream giả (`:69`), không có hẹn giờ 15 phút; (c) Q4: `computeInterviewResult` ở `entities/mock-interview/api/__mock__/mock-interview-mocks.ts:85` trả sẵn một đối tượng kết quả mock, chưa tính từ 4 dòng rubric; **Owner chốt 2026/10/19: (a) Q2 và (b) Q3 không dựng ở prototype, hoãn sang lúc nối API thật (ponytail bậc 3); câu hỏi đóng, hành vi đích vẫn ở Q2, Q3 và EVT-1.** (d) bỏ lối vào từ `USR0401`: bản dựng **khớp**, không đọc `question_id` hay `submission_id` từ URL (grep `searchParams` trong `views/users/mock-interview/**` không có kết quả); câu hỏi F6 chọn ngay trong tab "Kho câu hỏi" (`mock-interview-view.tsx:113-136`, `setQuestionId` ở `:119`).

### 4.5. Cấu trúc slice FSD [Nội bộ]

- Route: `/mock-interview`
- View slice: `src/views/users/mock-interview/`
  - `ui/mock-interview-page.tsx`
  - `ui/entry/entry-source-tabs.tsx`
  - `ui/entry/session-config-panel.tsx`
  - `ui/running/running-stage-header.tsx`
  - `ui/running/chat-stream-timeline.tsx`
  - `ui/running/chat-input-toolbar.tsx`
  - `ui/running/code-reference-drawer.tsx`
  - `ui/running/early-exit-confirm-modal.tsx`
  - `ui/result/interview-rubric-card.tsx`
  - `model/use-mock-interview-session.ts` (quản lý state machine: `entry` -> `running` -> `result`)

---

## Sheet 5. Danh sách item màn hình

> Cột và bộ thẻ: `02-bd/_rules/bd-template-9sheet.md` mục 2, 3, 4.

### Khu vực A — 3 tab chọn lối vào (Trạng thái Entry)

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :-: | :--- | :--- | :--- |
| 3 tab chọn lối vào | | | | | | | | | | | | | |
| | 1 | Tab Bài nộp Accepted | `mockInterview.entry.tabAccepted` | - | - | Toggle | String | 30 | Có | I/O | `ACTIVE` | - | Lối vào từ bài giải đã nộp, ứng với `entry_type = SUBMISSION` (F5-24)<br>[Nguồn giá trị] Trạng thái tab chọn tại giao diện<br>[EVT liên quan] EVT-2 |
| | 2 | Tab Kho câu hỏi F6 | `mockInterview.entry.tabBank` | - | - | Toggle | String | 30 | Có | I/O | `` | - | Lối vào từ ngân hàng câu hỏi, ứng với `entry_type = QUESTION_BANK`<br>[Nguồn giá trị] Trạng thái tab chọn tại giao diện<br>[EVT liên quan] EVT-3 |
| | 3 | Tab Tự chọn chủ đề | `mockInterview.entry.tabCustom` | - | - | Toggle | String | 30 | Có | I/O | `` | - | Lối vào tự do, ứng với `entry_type = TOPIC_SELECTION`<br>[Nguồn giá trị] Trạng thái tab chọn tại giao diện<br>[EVT liên quan] EVT-4 |
| | 4 | Danh sách bài nộp mẫu | `mockInterview.entry.listAccepted` | `judge.submissions` | `id`, `problem_id`, `status`, `created_at` | List | String | 2000 | Không | I/O | rỗng | - | Danh sách bài nộp `ACCEPTED` gần đây của chính người dùng<br>[Nguồn giá trị] Kết quả gọi `ListMyAcceptedSubmissions`<br>[EVT liên quan] EVT-5 |
| | 5 | Danh sách câu hỏi F6 | `mockInterview.entry.listBankQuestions` | `interview.interview_questions` | `id`, `title` | List | String | 2000 | Không | I/O | rỗng | - | Danh sách câu hỏi gợi ý từ kho F6<br>[Nguồn giá trị] Kết quả gọi `ListInterviewBankShortlist`<br>[EVT liên quan] EVT-6 |

### Khu vực B — Cấu hình phiên (Trạng thái Entry)

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :-: | :--- | :--- | :--- |
| Cấu hình phiên | | | | | | | | | | | | | |
| | 1 | Cụm chọn Cấp độ | `mockInterview.entry.optLevel` | `ai.interview_sessions` | `interviewer_level` | Toggle | Enum | 20 | Có | I/O | `MIDDLE` | - | Chọn mức độ: Intern, Junior, Middle, Senior — 4 mức thống nhất toàn hệ thống<br>[Nguồn giá trị] Tham số F5-28, ghi vào cột `interviewer_level` (`02-bd/database/ai-review.md` mục 1.4, `DEC-2026-0922-users-and-admin-conflict-resolutions` mục (2))<br>[EVT liên quan] EVT-7 |
| | 2 | Cụm chọn Số lượt hỏi | `mockInterview.entry.optTurns` | `ai.interview_sessions` | `max_turns` | Toggle | Number | 10 | Có | I/O | `12` | Số nguyên | Số lượt tối đa: 8, 12 hoặc 16 lượt<br>[Nguồn giá trị] Tham số F5-28, ghi vào cột `max_turns`<br>[EVT liên quan] EVT-8 |
| | 3 | Toggle Cho phép gợi ý | `mockInterview.entry.togHint` | `ai.interview_sessions` | `hint_allowed` | Toggle | Boolean | 10 | Có | I/O | `true` | - | Bật/tắt nút xin gợi ý trong phiên<br>[Nguồn giá trị] Tham số F5-28, ghi vào cột `hint_allowed`<br>[EVT liên quan] EVT-9 |
| | 4 | Nút Bắt đầu phiên | `mockInterview.entry.btnStart` | - | - | Button | String | 30 | Có | I | - | - | Khởi tạo phiên và chuyển sang trạng thái `running`<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] EVT-10 |

### Khu vực C — Thanh tiến trình 3 giai đoạn (Trạng thái Running)

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :-: | :--- | :--- | :--- |
| Thanh tiến trình 3 giai đoạn | | | | | | | | | | | | | |
| | 1 | Chỉ báo Giai đoạn 1 | `mockInterview.running.stepExplain` | `ai.interview_sessions` | `stage` | Badge | String | 30 | Có | O | `1. Giải trình` | - | Giai đoạn giải trình thuật toán F5-10<br>[Nguồn giá trị] Cột `stage`, làm nổi khi `stage = EXPLAIN`<br>[EVT liên quan] - |
| | 2 | Chỉ báo Giai đoạn 2 | `mockInterview.running.stepChallenge` | `ai.interview_sessions` | `stage` | Badge | String | 30 | Có | O | `2. Phản biện` | - | Giai đoạn phản biện ca biên F5-11<br>[Nguồn giá trị] Cột `stage`, làm nổi khi `stage = CHALLENGE`<br>[EVT liên quan] - |
| | 3 | Chỉ báo Giai đoạn 3 | `mockInterview.running.stepScaleUp` | `ai.interview_sessions` | `stage` | Badge | String | 30 | Có | O | `3. Mở rộng` | - | Giai đoạn mở rộng quy mô dữ liệu F5-12<br>[Nguồn giá trị] Cột `stage`, làm nổi khi `stage = SCALE_UP`<br>[EVT liên quan] - |
| | 4 | Đếm số lượt hỏi | `mockInterview.running.lblTurnCount` | `ai.interview_sessions` | `turn_count`, `max_turns` | Label | String | 20 | Có | O | `Lượt 0 / 12` | `Lượt {số} / {số}` | Số lượt hiện tại trên tổng số lượt<br>[Nguồn giá trị] Cột `turn_count` / `max_turns`<br>[EVT liên quan] - |
| | 5 | Đồng hồ bấm giờ | `mockInterview.running.lblTimer` | `ai.interview_sessions` | `started_at` | Label | String | 20 | Có | O | `00:00` | `{phút}:{giây}` | Thời gian đã trôi qua trong phiên<br>[Công thức] Bộ đếm giây phía giao diện tính từ `started_at`<br>[EVT liên quan] - |
| | 6 | Nút Mở ngăn kéo mã | `mockInterview.running.btnOpenDrawer` | - | - | Button | String | 30 | Có | I | - | - | Mở drawer xem lại đề bài và mã nguồn gốc<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] EVT-11 |

### Khu vực D — Ngăn kéo đề bài & mã nguồn (Trạng thái Running)

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :-: | :--- | :--- | :--- |
| Ngăn kéo đề bài & mã nguồn | | | | | | | | | | | | | |
| | 1 | Vùng đề bài tham chiếu | `mockInterview.drawer.txtStatement` | `problem.problems` | `statement_md` | TextArea | String | 10000 | Có | O | - | Markdown | Nội dung đề bài dạng Markdown, chỉ có khi `entry_type = SUBMISSION`<br>[Nguồn giá trị] Cột `statement_md`, tra theo `submissions.problem_id`<br>[EVT liên quan] - |
| | 2 | Vùng mã nguồn đã nộp | `mockInterview.drawer.viewerCode` | `judge.submissions` | `source_code` | TextArea | String | 50000 | Có | O | - | - | Monaco Editor chỉ đọc hiển thị mã đã nộp<br>[Nguồn giá trị] Cột `source_code`<br>[EVT liên quan] - |
| | 3 | Nút Đóng ngăn kéo | `mockInterview.drawer.btnClose` | - | - | Button | String | 20 | Có | I | - | - | Đóng drawer, quay về khung chat<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] EVT-12 |

### Khu vực E — Khung hội thoại streaming (Trạng thái Running)

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :-: | :--- | :--- | :--- |
| Khung hội thoại streaming | | | | | | | | | | | | | |
| | 1 | Luồng tin nhắn chat | `mockInterview.chat.streamTimeline` | `ai.interview_turns` | `role`, `content`, `turn_index`, `stage_at_turn` | List | String | 100000 | Có | O | rỗng | - | Dòng thời gian các bubble tin nhắn F5-14; nguồn sự thật lâu dài là `interview_turns`, phiên đang mở đọc nhanh qua Redis `ChatMemory`<br>[Nguồn giá trị] Nhận qua Server-Sent Events (SSE) khi phiên đang chạy; nạp lại từ `interview_turns` khi mở lại phiên<br>[EVT liên quan] - |
| | 2 | Chỉ báo AI đang gõ | `mockInterview.chat.typingIndicator` | - | - | Label | String | 30 | Không | O | `` | - | Hiệu ứng hiển thị khi SSE đang stream token<br>[Nguồn giá trị] Trạng thái stream phía giao diện, không lưu DB<br>[EVT liên quan] - |

### Khu vực F — Thanh tương tác chat (Trạng thái Running)

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :-: | :--- | :--- | :--- |
| Thanh tương tác chat | | | | | | | | | | | | | |
| | 1 | Ô nhập câu trả lời | `mockInterview.controls.txtMessage` | `ai.interview_turns` | `content` | TextArea | String | 4000 | Có | I | `` | - | Người học gõ câu trả lời phỏng vấn, ghi thành một dòng `interview_turns` (`role = USER`) khi gửi<br>[Nguồn giá trị] Người dùng nhập<br>[EVT liên quan] EVT-13 |
| | 2 | Nút Gửi câu trả lời | `mockInterview.controls.btnSend` | - | - | Button | String | 20 | Có | I | - | - | Gửi câu trả lời lên máy chủ<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] EVT-14 |
| | 3 | Nút Xin gợi ý | `mockInterview.controls.btnHint` | - | - | Button | String | 30 | Không | I | - | - | Yêu cầu AI đưa ra gợi ý giải pháp (F5-28), chỉ hiện khi `hintAllowed = true`<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] EVT-15 |
| | 4 | Nút Kết thúc sớm | `mockInterview.controls.btnStopEarly` | - | - | Button | String | 30 | Có | I | - | - | Kích hoạt popup xác nhận kết thúc sớm<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] EVT-16 |

### Khu vực G — Popup xác nhận dừng sớm

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :-: | :--- | :--- | :--- |
| Popup xác nhận dừng sớm | | | | | | | | | | | | | |
| | 1 | Tiêu đề popup | `mockInterview.earlyExit.lblTitle` | - | - | Label | String | 50 | Có | O | `Kết thúc phiên phỏng vấn?` | - | Tiêu đề cảnh báo<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] - |
| | 2 | Nội dung cảnh báo | `mockInterview.earlyExit.lblMessage` | - | - | Label | String | 200 | Có | O | - | - | Giải thích hệ quả khi dừng sớm<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] - |
| | 3 | Nút Tiếp tục phỏng vấn | `mockInterview.earlyExit.btnResume` | - | - | Button | String | 20 | Có | I | - | - | Đóng popup, trở lại phiên chat<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] EVT-17 |
| | 4 | Nút Xác nhận dừng | `mockInterview.earlyExit.btnConfirmExit` | - | - | Button | String | 30 | Có | I | - | - | Dừng phiên và chuyển sang màn kết quả<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] EVT-18 |

### Khu vực H — Bảng điểm Rubric & nhận xét (Trạng thái Result)

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :-: | :--- | :--- | :--- |
| Bảng điểm Rubric & nhận xét | | | | | | | | | | | | | |
| | 1 | Điểm tổng kết phiên | `mockInterview.result.lblOverallScore` | `ai.rubric_scores` | `score` | Label | String | 30 | Có | O | `- / 10` | `{số} / 10` | Điểm tổng hợp 4 tiêu chí có trọng số F5-15<br>[Công thức] Tổng `score * trọng số hằng số F5-15 / 100` (25/30/25/20, cột `weight_percent_snapshot` đã bỏ 2026/10/14, `DEC-2026-1014-batch7-remaining-open-questions`) của 4 dòng `rubric_scores` cùng `owner_id = sessionId`; không có cột lưu sẵn (Câu hỏi mở Q4, đã chốt 2026-10-18)<br>[EVT liên quan] - |
| | 2 | Thẻ Điểm Độ rõ ràng (25%) | `mockInterview.result.cardClarity` | `ai.rubric_scores` | `criterion_code`, `score`, `comment` | Label | String | 150 | Có | O | - | `{số} / 10 · {nhận xét}` | Tiêu chí Độ rõ ràng — `CLARITY` (25%)<br>[Nguồn giá trị] Dòng `rubric_scores` với `criterion_code = CLARITY` (`02-bd/database/ai-review.md:96-97`)<br>[EVT liên quan] - |
| | 3 | Thẻ Điểm Độ chính xác kỹ thuật (30%) | `mockInterview.result.cardTechAccuracy` | `ai.rubric_scores` | `criterion_code`, `score`, `comment` | Label | String | 150 | Có | O | - | `{số} / 10 · {nhận xét}` | Tiêu chí Độ chính xác kỹ thuật — `TECHNICAL_ACCURACY` (30%)<br>[Nguồn giá trị] Dòng `rubric_scores` với `criterion_code = TECHNICAL_ACCURACY` (`02-bd/database/ai-review.md:96-97`)<br>[EVT liên quan] - |
| | 4 | Thẻ Khả năng phản biện (25%) | `mockInterview.result.cardPushback` | `ai.rubric_scores` | `criterion_code`, `score`, `comment` | Label | String | 150 | Có | O | - | `{số} / 10 · {nhận xét}` | Tiêu chí Khả năng phản biện — `PUSHBACK_HANDLING` (25%)<br>[Nguồn giá trị] Dòng `rubric_scores` với `criterion_code = PUSHBACK_HANDLING` (`02-bd/database/ai-review.md:96-97`)<br>[EVT liên quan] - |
| | 5 | Thẻ Nhận thức độ phức tạp (20%) | `mockInterview.result.cardComplexity` | `ai.rubric_scores` | `criterion_code`, `score`, `comment` | Label | String | 150 | Có | O | - | `{số} / 10 · {nhận xét}` | Tiêu chí Nhận thức độ phức tạp — `COMPLEXITY_AWARENESS` (20%)<br>[Nguồn giá trị] Dòng `rubric_scores` với `criterion_code = COMPLEXITY_AWARENESS` (`02-bd/database/ai-review.md:96-97`)<br>[EVT liên quan] - |
| | 6 | Đoạn nhận xét tổng quan | `mockInterview.result.txtFeedback` | - | - | TextArea | String | 2000 | Có | O | - | - | Đánh giá tổng thể từ AI, tổng hợp 4 nhận xét tiêu chí — không có cột riêng lưu đoạn tổng hợp này, và không thêm cột (Câu hỏi mở Q4, đã chốt 2026-10-18)<br>[Nguồn giá trị] Ghép từ `comment` của 4 dòng `rubric_scores` ở tầng ứng dụng<br>[EVT liên quan] - |
| | 7 | Nút Luyện tập lại | `mockInterview.result.btnRetry` | - | - | Button | String | 30 | Có | I | - | - | Reset màn hình về trạng thái `entry`<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] EVT-19 |
| | 8 | Nút Về danh sách | `mockInterview.result.btnBack` | - | - | Button | String | 30 | Có | I | - | - | Điều hướng về `problem_list`<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] EVT-20 |

---

## Sheet 6. Đặc tả điều khiển item

### Khu vực A, B: Trạng thái Entry

| NO | Tên item | Hiển thị | Kích hoạt | Ghi chú |
| --: | :--- | :--- | :--- | :--- |
| A.1 | Tab Bài nộp Accepted | Điều kiện | Có | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `entry`.<br>[Điều kiện kích hoạt] Kích hoạt.<br>[Tự động đặt] Tự chọn tab này nếu có query param `submission_id`. |
| A.2 | Tab Kho câu hỏi F6 | Điều kiện | Có | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `entry`.<br>[Điều kiện kích hoạt] Kích hoạt.<br>[Tự động đặt] Không có tự chọn theo URL: bỏ tham số `question_id` cùng lối vào "Phỏng vấn câu này" từ `USR0401` (2026/10/18). |
| A.3 | Tab Tự chọn chủ đề | Điều kiện | Có | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `entry`.<br>[Điều kiện kích hoạt] Kích hoạt.<br>[Tự động đặt] - |
| B.1 | Cụm chọn Cấp độ | Điều kiện | Có | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `entry`.<br>[Điều kiện kích hoạt] Luôn kích hoạt.<br>[Tự động đặt] Mặc định `MIDDLE`. |
| B.2 | Cụm chọn Số lượt hỏi | Điều kiện | Có | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `entry`.<br>[Điều kiện kích hoạt] Luôn kích hoạt.<br>[Tự động đặt] Mặc định 12 lượt. |
| B.3 | Toggle Cho phép gợi ý | Điều kiện | Có | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `entry`.<br>[Điều kiện kích hoạt] Luôn kích hoạt.<br>[Tự động đặt] Mặc định Bật (`true`). |
| B.4 | Nút Bắt đầu phiên | Điều kiện | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `entry`.<br>[Điều kiện kích hoạt] Chỉ kích hoạt khi đã chọn 1 bài nộp hoặc 1 câu hỏi hoặc chọn ít nhất 1 chủ đề.<br>[Tự động đặt] - |

### Khu vực C, D, E, F: Trạng thái Running

| NO | Tên item | Hiển thị | Kích hoạt | Ghi chú |
| --: | :--- | :--- | :--- | :--- |
| C.1 | Chỉ báo Giai đoạn 1 | Điều kiện | Không | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `running`.<br>[Điều kiện kích hoạt] Không áp dụng.<br>[Tự động đặt] Highlight khi `currentStage = EXPLAIN`. |
| C.2 | Chỉ báo Giai đoạn 2 | Điều kiện | Không | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `running`.<br>[Điều kiện kích hoạt] Không áp dụng.<br>[Tự động đặt] Highlight khi `currentStage = CHALLENGE`. |
| C.3 | Chỉ báo Giai đoạn 3 | Điều kiện | Không | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `running`.<br>[Điều kiện kích hoạt] Không áp dụng.<br>[Tự động đặt] Highlight khi `currentStage = SCALE_UP`. |
| C.4 | Đếm số lượt hỏi | Điều kiện | Không | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `running`.<br>[Điều kiện kích hoạt] Không áp dụng.<br>[Tự động đặt] Cập nhật tăng mỗi lượt hội thoại. |
| C.5 | Đồng hồ bấm giờ | Điều kiện | Không | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `running`.<br>[Điều kiện kích hoạt] Không áp dụng.<br>[Tự động đặt] Tự đếm tăng mỗi giây. |
| C.6 | Nút Mở ngăn kéo mã | Điều kiện | Có | [Điều kiện hiển thị] Chỉ hiển thị khi phiên có gắn với bài toán/mã nguồn.<br>[Điều kiện kích hoạt] Luôn kích hoạt.<br>[Tự động đặt] - |
| E.1 | Luồng tin nhắn chat | Điều kiện | Có | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `running`.<br>[Điều kiện kích hoạt] Cho phép cuộn, chọn text.<br>[Tự động đặt] Tự cuộn xuống đáy khi có tin nhắn mới. |
| F.1 | Ô nhập câu trả lời | Điều kiện | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `running`.<br>[Điều kiện kích hoạt] Bị khóa khi AI đang stream câu trả lời.<br>[Tự động đặt] Tự focus khi AI hoàn tất trả lời. |
| F.2 | Nút Gửi câu trả lời | Điều kiện | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `running`.<br>[Điều kiện kích hoạt] Chỉ kích hoạt khi ô nhập có ít nhất 1 ký tự và AI không đang gõ.<br>[Tự động đặt] - |
| F.3 | Nút Xin gợi ý | Điều kiện | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị khi `hintAllowed = true` ở cấu hình phiên.<br>[Điều kiện kích hoạt] Kích hoạt khi AI không đang gõ.<br>[Tự động đặt] - |
| F.4 | Nút Kết thúc sớm | Điều kiện | Có | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `running`.<br>[Điều kiện kích hoạt] Luôn kích hoạt.<br>[Tự động đặt] - |

### Khu vực G, H: Trạng thái Result & Popup

| NO | Tên item | Hiển thị | Kích hoạt | Ghi chú |
| --: | :--- | :--- | :--- | :--- |
| G.1 | Popup dừng sớm | Điều kiện | Có | [Điều kiện hiển thị] Chỉ hiển thị khi bấm nút Kết thúc sớm.<br>[Điều kiện kích hoạt] Chứa nút Huỷ và Xác nhận.<br>[Tự động đặt] - |
| H.1 | Điểm tổng kết phiên | Điều kiện | Không | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `result`.<br>[Điều kiện kích hoạt] Không áp dụng.<br>[Tự động đặt] - |
| H.2 | Thẻ Điểm Độ rõ ràng (25%) | Điều kiện | Không | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `result`.<br>[Điều kiện kích hoạt] Không áp dụng.<br>[Tự động đặt] - |
| H.3 | Thẻ Điểm Độ chính xác kỹ thuật (30%) | Điều kiện | Không | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `result`.<br>[Điều kiện kích hoạt] Không áp dụng.<br>[Tự động đặt] - |
| H.4 | Thẻ Khả năng phản biện (25%) | Điều kiện | Không | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `result`.<br>[Điều kiện kích hoạt] Không áp dụng.<br>[Tự động đặt] - |
| H.5 | Thẻ Nhận thức độ phức tạp (20%) | Điều kiện | Không | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `result`.<br>[Điều kiện kích hoạt] Không áp dụng.<br>[Tự động đặt] - |
| H.6 | Đoạn nhận xét tổng quan | Điều kiện | Không | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `result`.<br>[Điều kiện kích hoạt] Không áp dụng.<br>[Tự động đặt] - |
| H.7 | Nút Luyện tập lại | Điều kiện | Có | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `result`.<br>[Điều kiện kích hoạt] Kích hoạt.<br>[Tự động đặt] - |
| H.8 | Nút Về danh sách | Điều kiện | Có | [Điều kiện hiển thị] Chỉ hiển thị ở trạng thái `result`.<br>[Điều kiện kích hoạt] Kích hoạt.<br>[Tự động đặt] - |

---

## Sheet 7. Danh sách truyền dữ liệu

### 7.1. Trường DTO

| NO | DTO | Trường DTO | Kiểu | Bảng DB | Cột DB | Item màn | Hướng | Ghi chú |
| --: | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :--- |
| 1 | `SendMessageRequest` | `message` | String | `ai.interview_turns` | `content` | `mockInterview.controls.txtMessage` | I | [Đích] Ghi thành một dòng `interview_turns` với `role = USER` |
| 2 | - | Server-Sent Event `token` | String | - | - | `mockInterview.chat.streamTimeline` | O | Stream từng token phản hồi của AI qua SSE (F5-14); tích luỹ đủ lượt thì máy chủ ghi một dòng `interview_turns` với `role = AI` |
| 3 | `InterviewSessionDetailDto` | `currentStage` | Enum | `ai.interview_sessions` | `stage` | `mockInterview.running.stepExplain`, `stepChallenge`, `stepScaleUp` | O | Đồng bộ giai đoạn hiện tại |
| 4 | `InterviewSessionDetailDto` | `currentTurn`, `maxTurns` | Number | `ai.interview_sessions` | `turn_count`, `max_turns` | `mockInterview.running.lblTurnCount` | O | - |
| 5 | `InterviewResultSummaryDto` | `overallScore` | Number | `ai.rubric_scores` | `score` | `mockInterview.result.lblOverallScore` | O | [Nguồn] Tính từ 4 dòng `rubric_scores` cùng `owner_id`, không đọc từ một cột lưu sẵn — xem Sheet 5 Khu vực H NO 1 |
| 6 | `InterviewResultSummaryDto` | `clarityScore` | Number | `ai.rubric_scores` | `score` (`criterion_code = CLARITY`) | `mockInterview.result.cardClarity` | O | Điểm tiêu chí Độ rõ ràng (25%), `02-bd/database/ai-review.md:96-97` |
| 7 | `InterviewResultSummaryDto` | `technicalAccuracyScore` | Number | `ai.rubric_scores` | `score` (`criterion_code = TECHNICAL_ACCURACY`) | `mockInterview.result.cardTechAccuracy` | O | Điểm tiêu chí Độ chính xác kỹ thuật (30%), `02-bd/database/ai-review.md:96-97` |
| 8 | `InterviewResultSummaryDto` | `pushbackHandlingScore` | Number | `ai.rubric_scores` | `score` (`criterion_code = PUSHBACK_HANDLING`) | `mockInterview.result.cardPushback` | O | Điểm tiêu chí Khả năng phản biện (25%), `02-bd/database/ai-review.md:96-97` |
| 9 | `InterviewResultSummaryDto` | `complexityAwarenessScore` | Number | `ai.rubric_scores` | `score` (`criterion_code = COMPLEXITY_AWARENESS`) | `mockInterview.result.cardComplexity` | O | Điểm tiêu chí Nhận thức độ phức tạp (20%), `02-bd/database/ai-review.md:96-97` |
| 10 | `InterviewResultSummaryDto` | `feedbackSummary` | String | - | - | `mockInterview.result.txtFeedback` | O | Ghép từ 4 `comment` của `rubric_scores` ở tầng ứng dụng, không có cột riêng (Câu hỏi mở Q4, đã chốt 2026-10-18) |

### 7.2. Truy cập bảng dữ liệu (4)

| NO | Tên logic | Bảng | Repository | CRUD | Mục đích | Ghi chú |
| --: | :--- | :--- | :--- | :-: | :--- | :--- |
| 1 | Phiên phỏng vấn | `ai.interview_sessions` | `InterviewSessionRepository` | C, R, U | Khởi tạo phiên, đọc chi tiết phiên đang chạy, cập nhật `stage`/`turn_count`/`ended_at` | `StartInterviewSession`: C<br>`SendInterviewAnswerStream`: R, U<br>`CompleteInterviewSession`: U |
| 2 | Lượt hội thoại | `ai.interview_turns` | `InterviewTurnRepository` | C, R | Ghi mỗi lượt hỏi đáp theo thời gian thực, đọc lại khi mở phiên cũ | `SendInterviewAnswerStream`: C, R |
| 3 | Câu hỏi ngân hàng F6 | `interview.interview_questions` (module `interview-bank`) | `InterviewQuestionRepository` (module `interview-bank`) | R | Đọc tiêu đề rút gọn khi mở từ lối vào 2 | `ListInterviewBankShortlist`: R — gọi qua cổng ra, `ai-review` không đọc thẳng schema `interview` |
| 4 | Bài nộp | `judge.submissions` (module `judge-orchestration`) | `SubmissionRepository` (module `judge-orchestration`) | R | Đọc mã nguồn và đề bài liên quan khi mở từ lối vào 1 | `ListMyAcceptedSubmissions`: R — gọi qua cổng ra, `ai-review` không đọc thẳng schema `judge` |

`[Suy luận]` — tên repository do BD này đề xuất, DD module `ai-review` chốt lại.

### 7.3. Danh sách endpoint [Nội bộ]

> Chỉ tên nghiệp vụ và BC sở hữu. Hợp đồng chi tiết (đường dẫn, request, response, mã lỗi) thuộc
> `03-dd/api/ai-review.md`.

| NO | Endpoint (tên nghiệp vụ) | Mục đích | BC sở hữu |
| --: | :--- | :--- | :--- |
| 1 | `StartInterviewSession` | Khởi tạo phiên phỏng vấn mới và nhận câu hỏi mở đầu | `ai-review` |
| 2 | `SendInterviewAnswerStream` | Gửi câu trả lời của người học và stream phản hồi của AI qua SSE | `ai-review` |
| 3 | `RequestInterviewHint` | Yêu cầu AI cung cấp gợi ý ngắn | `ai-review` |
| 4 | `CompleteInterviewSession` | Kết thúc phiên và trả bảng điểm rubric tổng kết | `ai-review` |
| 5 | `ListMyAcceptedSubmissions` | Trả danh sách bài nộp `ACCEPTED` gần đây của chính người dùng | `judge-orchestration` |
| 6 | `ListInterviewBankShortlist` | Trả danh sách câu hỏi gợi ý rút gọn từ kho F6 | `interview-bank` |

---

## Sheet 8. Danh sách sự kiện

| NO | Loại | Sự kiện | Chi tiết | Chuyển màn | Gọi API | Tên xử lý | Ghi chú |
| --: | :--- | :--- | :--- | :-: | :-: | :--- | :--- |
| 1 | Khởi tạo | Mở màn hình | Đọc query params, hiển thị trạng thái `entry` (hoặc khôi phục phiên đang chạy). | Không | Có | `GetSessionDetail` (chỉ khi có `currentSessionId`) | [Các bước]<br>1. Kiểm tra đăng nhập.<br>2. Kiểm tra query param `submission_id` để active tab "Bài nộp đã Accepted"; không có `question_id` (đã bỏ 2026/10/18).<br>3. Nếu `sessionStorage` có `currentSessionId` của một phiên đang chạy thì gọi `GetSessionDetail` khôi phục tin nhắn từ Redis và vào thẳng `running` (Câu hỏi mở Q2); phiên đã quá hạn 15 phút thì vào `result` (Q3). |
| 2 | Lựa chọn | Bấm Tab Bài Accepted | Chuyển sang chọn bài nộp Accepted gần đây. | Không | Có | `ListMyAcceptedSubmissions` | [Các bước]<br>1. Active tab `tabAccepted`.<br>2. Tải danh sách bài nộp Accepted gần nhất. |
| 3 | Lựa chọn | Bấm Tab Kho câu hỏi | Chuyển sang chọn câu hỏi từ ngân hàng F6. | Không | Có | `ListInterviewBankShortlist` | [Các bước]<br>1. Active tab `tabBank`.<br>2. Tải danh sách câu hỏi phỏng vấn gợi ý. |
| 4 | Lựa chọn | Bấm Tab Tự chọn | Chuyển sang chọn chủ đề tự do. | Không | Không | - | [Các bước]<br>1. Active tab `tabCustom`.<br>2. Hiển thị danh sách chips chủ đề (Tree, Graph, DP...). |
| 5 | Lựa chọn | Chọn một bài Accepted | Bấm chọn một bài nộp trong danh sách. | Không | Không | - | [Các bước]<br>1. Highlight bài nộp được chọn.<br>2. Mở khóa nút "Bắt đầu phỏng vấn". |
| 6 | Lựa chọn | Chọn một câu hỏi F6 | Bấm chọn một câu hỏi trong danh sách. | Không | Không | - | [Các bước]<br>1. Highlight câu hỏi được chọn.<br>2. Mở khóa nút "Bắt đầu phỏng vấn". |
| 7 | Cấu hình | Đổi Cấp độ | Chọn mức độ Intern, Junior, Middle, Senior. | Không | Không | - | [Các bước]<br>1. Lưu giá trị cấp độ vào state cấu hình phiên. |
| 8 | Cấu hình | Đổi Số lượt hỏi | Chọn số lượt 8, 12, hoặc 16. | Không | Không | - | [Các bước]<br>1. Lưu số lượt vào state cấu hình phiên. |
| 9 | Cấu hình | Đổi Gợi ý | Bật hoặc tắt toggle xin gợi ý. | Không | Không | - | [Các bước]<br>1. Lưu cờ `hintAllowed` vào state cấu hình. |
| 10 | Hành động | Bấm Bắt đầu phỏng vấn | Khởi tạo phiên làm việc với AI. | Không | Có | `StartInterviewSession` | [Các bước]<br>1. Gửi cấu hình phiên lên `StartInterviewSession`.<br>2. Nhận `sessionId` và lời chào/câu hỏi đầu tiên của AI.<br>3. Chuyển state màn hình sang `running`.<br>4. Bắt đầu đồng hồ bấm giờ. |
| 11 | Giao diện | Bấm Xem đề & mã | Bấm nút mở drawer tham khảo. | Không | Không | - | [Các bước]<br>1. Mở drawer hiển thị đề bài và mã nguồn gốc. |
| 12 | Giao diện | Bấm Đóng drawer | Bấm nút đóng drawer. | Không | Không | - | [Các bước]<br>1. Đóng drawer trở lại khung chat. |
| 13 | Nhập liệu | Nhập tin nhắn | Gõ vào ô nhập câu trả lời. | Không | Không | - | [Các bước]<br>1. Cập nhật state input; nếu có ký tự thì mở khóa nút Gửi. |
| 14 | Hành động | Bấm Gửi câu trả lời | Gửi câu trả lời của người học qua SSE. | Không | Có | `SendInterviewAnswerStream` | [Các bước]<br>1. Thêm bubble tin nhắn của người học vào timeline.<br>2. Xóa trắng ô nhập.<br>3. Khóa ô nhập và nút gửi.<br>4. Mở kết nối SSE nhận stream phản hồi từ AI.<br>5. Append từng token vào bubble tin nhắn của AI.<br>6. Khi stream kết thúc: mở khóa ô nhập, tăng số lượt hỏi.<br>7. Nếu đạt `maxTurns`: tự động kích hoạt `CompleteInterviewSession`; khi sang màn kết quả thì hiện toast thành công "Phiên phỏng vấn đã kết thúc. Kết quả đánh giá đã sẵn sàng." [Nguồn: 05-coding/frontend/src/views/users/mock-interview/ui/mock-interview-view.tsx:50-55]. |
| 15 | Hành động | Bấm Xin gợi ý | Yêu cầu AI gợi ý hướng tư duy. | Không | Có | `RequestInterviewHint` | [Các bước]<br>1. Gửi yêu cầu gợi ý lên máy chủ.<br>2. AI trả về tin nhắn gợi ý đóng khung đặc biệt trong timeline. |
| 16 | Giao diện | Bấm Dừng phỏng vấn | Bấm nút kết thúc sớm. | Không | Không | - | [Các bước]<br>1. Mở popup xác nhận `earlyExitModal`. |
| 17 | Giao diện | Huỷ dừng sớm | Bấm "Tiếp tục" trong popup. | Không | Không | - | [Các bước]<br>1. Đóng popup, tiếp tục phiên phỏng vấn. |
| 18 | Hành động | Xác nhận dừng sớm | Bấm "Kết thúc & Chấm điểm" trong popup. | Không | Có | `CompleteInterviewSession` | [Các bước]<br>1. Đóng popup.<br>2. Gọi `CompleteInterviewSession` chấm điểm các lượt đã có.<br>3. Chuyển state màn hình sang `result`, hiện cùng toast kết thúc phiên như EVT-14 (chỉ một toast cho mỗi lần kết thúc, dù kết thúc bằng đường nào) [Nguồn: 05-coding/frontend/src/views/users/mock-interview/ui/mock-interview-view.tsx:50-55]. |
| 19 | Hành động | Bấm Luyện tập lại | Bấm nút Luyện tập lại ở màn kết quả. | Không | Không | - | [Các bước]<br>1. Reset toàn bộ state phiên chat.<br>2. Chuyển màn hình về trạng thái `entry`. |
| 20 | Điều hướng | Bấm Về danh sách | Bấm nút Về trang bài toán. | Có | Không | - | [Các bước]<br>1. Điều hướng về `problem_list` (`USR0101`). |

---

## Sheet 9. Đặc tả kiểm tra

| NO | Loại | Tóm tắt kiểm | Chi tiết | Mức | Mã thông báo | Ghi chú | EVT gọi | Thứ tự |
| --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: |
| 1 | Kiểm quyền | Phải đăng nhập | [Nội dung kiểm] Học viên chưa đăng nhập không được mở phiên phỏng vấn.<br>[Nơi thực thi] Giao diện và API gateway.<br>[Tiêu điểm] Toàn màn. | Lỗi | Chưa có mã thông báo | Nội dung "Vui lòng đăng nhập để bắt đầu phỏng vấn giả lập." | EVT-1, EVT-10 | 1 |
| 2 | Kiểm nghiệp vụ | Hạn mức ngân sách token | [Nội dung kiểm] Kiểm tra hạn mức token AI của học viên trước khi bắt đầu phiên (F5-25). Nếu đã chạm trần, từ chối tạo phiên và báo bằng toast.<br>[Nơi thực thi] Máy chủ.<br>[Tiêu điểm] Toast; nút Bắt đầu. | Lỗi | Chưa có mã thông báo | Nội dung "Bạn đã đạt giới hạn sử dụng AI hôm nay. Vui lòng quay lại vào ngày mai." | EVT-10 | 2 |
| 3 | Kiểm nghiệp vụ | Bắt buộc chọn câu hỏi / chủ đề | [Nội dung kiểm] Không cho phép bấm bắt đầu khi chưa chọn bài nộp, chưa chọn câu hỏi hoặc chưa tick chủ đề.<br>[Nơi thực thi] Màn hình.<br>[Tiêu điểm] Nút Bắt đầu. | Cảnh báo | Chưa có mã thông báo | Nội dung "Vui lòng chọn một bài nộp, câu hỏi hoặc chủ đề để phỏng vấn." | EVT-10 | 1 |
| 4 | Kiểm nghiệp vụ | Chống tấn công Prompt Injection | [Nội dung kiểm] Kiểm tra nội dung câu trả lời của học viên; ngăn chặn các chỉ thị ghi đè prompt hệ thống (F5-17).<br>[Nơi thực thi] Máy chủ tầng AI gateway.<br>[Tiêu điểm] Ô nhập tin nhắn. | Lỗi | Mã lỗi trong phản hồi | Bảo vệ an toàn mô hình ngôn ngữ lớn. | EVT-14 | 1 |
| 5 | Kiểm nghiệp vụ | Xử lý đứt kết nối SSE giữa chừng | [Nội dung kiểm] Nếu kết nối SSE stream bị đứt trong khi AI đang trả lời, màn hình hiển thị nút "Thử tải lại câu trả lời" đọc từ Redis cache.<br>[Nơi thực thi] Màn hình.<br>[Tiêu điểm] Khung chat. | Cảnh báo | Không có thông báo | Giữ trải nghiệm liền mạch cho học viên khi mạng chập chờn. | EVT-14 | 2 |

---

## Câu hỏi mở

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| ~~Q1~~ | ~~Số cấp độ phỏng vấn là 3 hay 4 mức?~~ | **Đã đóng 2026-09-24**: `DEC-2026-0922-users-and-admin-conflict-resolutions` mục (2) chốt 4 mức enum (`INTERN`, `JUNIOR`, `MIDDLE`, `SENIOR`) thống nhất toàn hệ thống. Cột `ai.interview_sessions.interviewer_level` đã sửa theo (`02-bd/database/ai-review.md` mục 1.4 và mục 7) | Đã áp dụng vào Sheet 5 Khu vực B NO 1 | Đã đóng |
| Q2 | **[Đã chốt 2026-10-18 (owner), theo đề xuất; chốt lại 2026-10-19 (owner): không dựng ở prototype, hoãn sang API thật, nợ giữ nguyên, ponytail bậc 3)]** Học viên vô tình tải lại trang (F5) khi phiên đang chạy thì xử lý thế nào? | Quyết định: lưu `currentSessionId` vào `sessionStorage`; khi tải lại trang, gọi `GetSessionDetail` khôi phục toàn bộ tin nhắn từ Redis (`ChatMemory`) và tiếp tục phiên. Tiêu chí ponytail bậc 5 (dùng mặc định của trình duyệt và Redis đã có, không thêm bảng). `DEC-2026-1018-usr-open-questions-phase2-5`. **Nợ prototype**: bản dựng giữ phiên trong bộ nhớ, chưa có `sessionStorage` (mục 4.4) | Đóng. EVT-1 đã sửa theo; chữ ký `GetSessionDetail` do DD `ai-review` chốt | DD `ai-review` + Frontend |
| Q3 | **[Đã chốt 2026-10-18 (owner), theo đề xuất; chốt lại 2026-10-19 (owner): không dựng ở prototype, hoãn sang API thật, nợ giữ nguyên, ponytail bậc 3)]** Thời gian tối đa cho mỗi lượt trả lời của học viên là bao lâu? | Quyết định: một lượt bỏ trống **15 phút** thì tự động đóng phiên và chuyển sang trạng thái `result` (chấm các lượt đã có). Tiêu chí ponytail bậc 5 (hằng số máy chủ, không thêm ô cấu hình). `DEC-2026-1018-usr-open-questions-phase2-5`. **Nợ prototype**: chưa có hẹn giờ này (mục 4.4) | Đóng. DD `ai-review` ghi hằng số 15 phút và cách giải phóng khoá Redis của phiên | DD `ai-review` |
| Q4 | **[Đã chốt 2026-10-18 (owner), theo đề xuất]** `overallScore` và `feedbackSummary` của kết quả phỏng vấn lưu ở cột nào? | Quyết định: **không thêm cột**. `overallScore` tính ở tầng ứng dụng từ 4 dòng `rubric_scores` (không cache); `feedbackSummary` ghép từ 4 `comment` của 4 dòng đó. Tiêu chí ponytail bậc 5 (suy ra từ dữ liệu đã có thay vì lưu thêm). `DEC-2026-1018-usr-open-questions-phase2-5`. Tài liệu CSDL `02-bd/database/ai-review.md` do bên khác sửa cùng đợt, ở đây chỉ tham chiếu theo tên | Đóng. Sheet 5 Khu vực H NO 1, NO 6 và Sheet 7.1 NO 5, NO 10 đã sửa theo | DD `ai-review` |
| Q5 | ~~**RD `USR0302` còn yêu cầu "Thanh chỉ số" 3 ô ở trạng thái `entry`, prototype đã bỏ — giữ hay sửa RD?**~~ **Đã chốt 2026-10-18 (owner), RD đã sửa:** RD `USR0302` đã bỏ mục "Thanh chỉ số" (nay là ghi chú đầu mục "Trạng thái `entry`", các khoản đánh lại 1-12), `DEC-2026-1018-usr-open-questions-phase1-scope` mục (1), ponytail bậc 1. **F5-16 vẫn được đáp ứng ở chỗ khác:** RD `USR0302` giữ bảng "Phiên gần đây" mở lại rubric phiên cũ, và `USR0501`/`USR0601` giữ lịch sử phỏng vấn mở lại được (`01-rd/screens/users/USR0501_my_progress.md:20`, `01-rd/screens/users/USR0601_dashboard.md:43`). Bản dựng `entry` mở đầu bằng thẻ chọn lối vào, không có dải thống kê [Nguồn: 05-coding/frontend/src/views/users/mock-interview/ui/mock-interview-view.tsx:63-72]. | Owner 2026/10/18 bỏ dải thẻ thống kê khỏi prototype; `01-rd/screens/users/USR0302_mock_interview.md:46-48` (mục 1 của trạng thái `entry`) vẫn mô tả thanh chỉ số, và `README.md`/RD F5-16 chưa bị sửa. BD này không sửa RD | Cập nhật RD bỏ mục "Thanh chỉ số" (hoặc ghi rõ hoãn sau demo); dữ liệu tổng hợp F5-16 vẫn có ở `ai.interview_sessions`/`ai.rubric_scores`, nên khi cần chỉ thêm lại endpoint thống kê, không đổi schema | Owner (RD) |

---

## Tham chiếu

- `01-rd/screens/users/USR0302_mock_interview.md` — Yêu cầu màn hình.
- `09-layoutBase/Phỏng vấn giả lập.dc.html` — Bằng chứng bố cục prototype.
- `01-rd/req/ai-review.md` — F5-09→F5-16, F5-17, F5-18, F5-19, F5-22, F5-24, F5-25, F5-28.
- `02-bd/_rules/bd-template-9sheet.md` — Quy ước mẫu 9 sheet.
- `02-bd/database/ai-review.md` mục 1.4, 1.5, 96-97 — schema `interview_sessions`, `rubric_scores`, rubric 4 tiêu chí F5-15.
- Quyết định: `DEC-2026-0922-users-and-admin-conflict-resolutions`.
