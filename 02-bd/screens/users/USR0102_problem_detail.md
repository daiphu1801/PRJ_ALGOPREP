# Tài liệu thiết kế cơ bản (BD) — Chi tiết bài tập / Workspace giải bài (`USR0102`)

## Phương châm tài liệu [Nội bộ — không cần khách duyệt]

- Viết theo **mẫu 9 sheet**. Bộ thẻ đóng, quy ước ID, ký hiệu chuẩn, ranh giới với tài liệu khác và
  checklist kiểm toán nằm ở `02-bd/_rules/bd-template-9sheet.md` — file này không chép lại.
- Phần gắn nhãn `[Nội bộ]` phục vụ sinh mã và tự kiểm toán, không phải yêu cầu của khách hàng: mục 4.5,
  mục 7.3 và các ghi chú quy ước.
- Mã màn `USR0102` theo bảng mã ở `02-bd/_rules/bd-template-9sheet.md` mục 8; tên file mang tiền tố mã.
- Màn này là **màn phức tạp nhất hệ thống**: chạm bốn Bounded Context cùng lúc — `problem-bank` (F2),
  `harness` (F3), `judge-orchestration` (F4), `ai-review` (F5)
  [Nguồn: 01-rd/screens/users/USR0102_problem_detail.md:7-9].
- Kế thừa quy ước đặt tên khối, ID item và tên nghiệp vụ endpoint từ `02-bd/screens/users/USR0101_problem_list.md`
  — file BD đầu tiên của khu Người học [Nguồn: 02-bd/screens/users/USR0101_problem_list.md phần Phương châm tài liệu].

> **Ranh giới bắt buộc giữ — đọc trước khi sửa file này.** Màn chạm bốn module nên rất dễ biến thành nơi
> chứa mọi thứ. File này **chỉ** mô tả giao diện, item, trạng thái màn, sự kiện và **tên nghiệp vụ** của
> endpoint. Những nội dung sau đã có nơi viết duy nhất và **chỉ được trỏ tới, tuyệt đối không chép lại**:
>
> | Nội dung | Nơi duy nhất |
> | :--- | :--- |
> | Lược đồ kiểu dữ liệu độc lập ngôn ngữ, cách sinh mã bao hàm, tiêm mã, ánh xạ lỗi biên dịch | `02-bd/architecture/harness.md` mục 2b, 4, 6 |
> | State machine bài nộp, hợp đồng `JudgeExecutionPort`, idempotency, Outbox, job sweep | `02-bd/architecture/judge-orchestration.md` mục 3, 4, 5, 6 |
> | Bảng, cột, khoá, index của `judge`/`problem`/`ai` | `02-bd/database/judge-orchestration.md`, `02-bd/database/problem-bank.md`, `02-bd/database/ai-review.md` |
> | Đường dẫn endpoint, request/response, mã lỗi | `03-dd/api/<module>.md` (chưa viết) |
>
> Nếu bạn đang viết về giao dịch, khoá, hàng đợi hay hợp đồng port trong file này thì bạn đang viết nhầm file.

> Đọc cùng `01-rd/screens/users/USR0102_problem_detail.md` (hành vi ở mức yêu cầu, không lặp lại ở đây) và
> `02-bd/screens/users/_shell.md` (khung điều hướng khu Người học).
>
> - **Đường dẫn của màn là `/workspace/{problemId}`** (từ 2026-10-20, `DEC-2026-1020-student-workspace-split-and-password-in-settings`
>   [Nguồn: .nexa/control/decision-registry.md:4239-4244]). `/problems/{problemId}` **không còn** là màn giải bài: đó là màn
>   tổng quan chỉ-đọc `USR0104` (`02-bd/screens/users/USR0104_problem_overview.md`), có nút "Giải bài toán" dẫn sang đây.
>   `/workspace` (không có id) là trang đầu `USR0105` (`02-bd/screens/users/USR0105_workspace_home.md`), nơi chọn tiếp một
>   bài đang làm dở. Nội dung và hành vi của màn này **giữ nguyên**, chỉ đổi đường dẫn và các lối vào
>   [Nguồn: 05-coding/frontend/src/app/(student)/workspace/[problemId]/page.tsx:3-5].
> - **Header dùng chung khung khu Người học nhưng ở biến thể thu gọn** — giữ nguyên thương hiệu và 6 mục
>   nav, lược tên và email trong khối người dùng, chỉ còn avatar
>   [Nguồn: 02-bd/screens/users/_shell.md:89-91]. Đây là khác biệt về mật độ, vẫn một component
>   [Nguồn: 02-bd/screens/users/_shell.md:94-95].
> - **Không có chân trang.** Prototype của màn này không có thẻ `<footer>`; bố cục chiếm trọn chiều cao
>   màn hình (`height: 100vh`, `overflow: hidden`)
>   [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:30,47]. Trạng thái cụm chấm được đưa vào chân panel
>   đề bài thay cho chân trang [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:254].
> - **KHÔNG thiết kế tab "Gợi ý".** Tính năng "Gợi ý theo bậc" đã bị cắt khỏi phạm vi hoàn toàn
>   (`DEC-2026-0831-remove-tiered-hints-ai-config`), trong đó gọi đích danh tab "Gợi ý" của
>   `problem_detail` là một trong ba điểm giao diện phải gỡ. Prototype tĩnh `.dc.html` vẫn còn nguyên tab này
>   và **đã lỗi thời ở điểm này** (chủ dự án chốt 2026-10-18, Câu hỏi mở Q1) — chỉ đánh dấu, không sửa file.
>   Màn còn **ba** tab.
> - **KHÔNG có nút "Yêu cầu review"** của học viên (`DEC-2026-0830-remove-student-review-request`).
> - **KHÔNG hiển thị nội dung testcase ẩn** ở bất kỳ trạng thái nào — không input, không kết quả mong đợi,
>   không diff. Chỉ trạng thái đạt/không đạt theo từng ô và chỉ số testcase hỏng đầu tiên. Ràng buộc này
>   thực thi ở tầng máy chủ, xem Sheet 9 NO 3.

---

## Sheet 1. Bìa

| Mục | Giá trị |
| :--- | :--- |
| Hệ thống | AlgoPrep |
| Phân hệ | `problem-bank` (F2) + `harness` (F3) + `judge-orchestration` (F4), đọc thêm từ `ai-review` (F5) |
| Công đoạn | BD — Thiết kế cơ bản |
| Phân loại | Màn hình |
| Tên màn | Chi tiết bài tập (Workspace giải bài) |
| Mã màn hình | `USR0102` |
| Tên vật lý (slug) | `problem_detail` |
| Đường dẫn | `/workspace/{problemId}` |
| Trục tài liệu | Màn hình (`02-bd/screens/`) |
| Actor | A1 (`STUDENT`) |
| Phiên bản | V0.13 |
| Người tạo | Nhóm phát triển AlgoPrep |
| Ngày tạo | 2026/09/22 |
| Người cập nhật | Claude |
| Ngày cập nhật | 2026/10/22 |

---

## Sheet 2. Lịch sử sửa đổi

| Ver | Sheet bị sửa | Nội dung sửa | Ngày | Người sửa |
| :--- | :--- | :--- | :--- | :--- |
| V0.1 | Toàn bộ | Tạo mới theo mẫu 9 sheet. Gỡ tab "Gợi ý" khỏi thiết kế theo `DEC-2026-0831-remove-tiered-hints-ai-config`, còn 3 tab. Chốt hai trục lựa chọn độc lập (mô hình nộp bài và phương thức nhập mã) theo `DEC-2026-0824-dual-submission-model-per-problem`. Chốt nguyên tắc không lộ testcase ẩn thực thi ở tầng máy chủ. Chốt hợp đồng chuyển màn sang bốn màn hệ quả của một lượt nộp. Phát sinh 13 câu hỏi mở | 2026/09/22 | Nhóm phát triển AlgoPrep |
| V0.2 | Sheet 8, 9 | Đổi phản hồi sau thao tác (đổi ngôn ngữ, tải file, Run Code, Submit, kết quả chấm, lưu bài) và `[Tiêu điểm]` kiểm nhập liệu sang toast + viền ô; giữ vùng lỗi tải panel, lỗi biên dịch trong bảng kết quả, hộp xác nhận rời màn. Theo `DEC-2026-1003-toast-feedback-channel`. | 2026/10/03 | AI |
| V0.3 | Sheet 4, 5, 7 | Đồng bộ `DEC-2026-1001-admin-configurable-settings` mục (7): độ khó bài tập là danh mục do ADMIN quản lý (bảng riêng `problem_levels`). Nhãn độ khó (thanh tác vụ, danh sách chọn bài, tab Đề bài) đọc `display_name` qua `problems.level_id`; DTO `difficulty` thành `levelCode`/`levelDisplayName`; thêm `problem_levels` vào bảng liên quan và Truy cập bảng. Màn chỉ hiển thị nhãn, không lọc theo độ khó và không gọi `ListProblemLevels` (nhãn đã nằm trong phản hồi `GetProblemForWorkspace`) | 2026/10/03 | AI |
| V0.4 | Sheet 4 (mục 4.4) | Bản dựng chuyển sang bố cục full màn hình kiểu LeetCode: khung student bỏ footer và giới hạn bề rộng khi màn gắn `data-fullscreen-view`, chiều cao chia bằng flex thay cho `calc(100vh - 84px)`; thanh chia đôi cột kéo được thật (`shared/ui` `SplitPane`, mặc định 47%, chặn 22-74, nhớ ở localStorage), đóng nợ "chưa có split-pane" của V0.3. Thêm một thanh kéo thứ hai giữa vùng mã và vùng kết quả (BD chưa quy định, xem mục Hiện trạng bản dựng). Chủ dự án yêu cầu 2026-10-18, chưa duyệt hình | 2026/10/18 | Claude |
| V0.5 | Sheet 3, 4, 5, 6, 7, 8, 9, Câu hỏi mở | Ghi quyết định của chủ dự án 2026-10-18 (`DEC-2026-1018-usr-open-questions-phase1-scope`), đề xuất mặc định được chấp nhận: đóng Q1 (giữ 3 tab, prototype tĩnh lỗi thời), Q2 (bỏ khối "Gợi ý theo điểm yếu": trạng thái chưa chọn bài chỉ còn bài đang làm dở và liên kết sang `problem_list`), Q3 (bỏ đồng hồ phiên làm bài), Q4 (bản nháp mã lưu ở trình duyệt theo khoá `(problem_id, language, submission_model)`, không có bảng ở máy chủ), Q5 (ràng buộc và câu hỏi mở rộng nằm trong `statement_md`), Q11 (Beats chỉ ở `submission_result`), Q12 (mục nav Workspace chuyển hướng sang bài đang làm dở gần nhất, không có thì sang danh sách), Q13 (hiển thị `problems.code` nguyên dạng chuỗi). Xoá item Khu vực A NO 4 (đồng hồ) và Khu vực B NO 4-7 (danh sách gợi ý), đánh số lại. Sửa trích dẫn toast về đúng dòng mã hiện tại. Thêm Q14 (hộp xác nhận rời màn và ghi đè mã khi đã có bản nháp). Q6-Q10 vẫn mở | 2026/10/18 | Claude |
| V0.6 | Sheet 3, 4, 5, 6, 7, 8, 9, Câu hỏi mở | Ghi quyết định của chủ dự án 2026-10-18 (`DEC-2026-1018-usr-open-questions-phase2-5`), đề xuất mặc định được chấp nhận: đóng Q6 (giải thích ví dụ lưu ở cột mới `testcases.explanation_md`), Q7 (mã gõ trực tiếp giới hạn 64 KB như file tải lên; bản dựng chặn Run Code và Submit bằng toast lỗi), Q8 (chấm trạng thái cụm chấm vẫn ẩn, không gọi định kỳ), Q9 (nhớ ngôn ngữ và mô hình gần nhất ở `localStorage`, khoá `algoprep-workspace-prefs`), Q10 (Run Code không tạo dòng `submissions`), Q14 (bỏ hộp xác nhận rời màn và hộp xác nhận ghi đè mã). **Giữ nguyên số EVT, không đánh số lại:** EVT-21 và Sheet 9 NO 16 gạch bỏ vì còn nhiều tham chiếu chéo (EVT-16 đến EVT-19, EVT-22); mọi bước "Chạy EVT-21" đổi thành điều hướng thẳng. Bỏ bước hỏi xác nhận ở EVT-4, EVT-5 | 2026/10/18 | Claude |
| V0.7 | Sheet 4, 5, 9, Câu hỏi mở | Rà soát bản dựng 2026-10-19. Owner chốt: bỏ trạng thái "chưa chọn bài" (Khu vực B) vì không còn đường vào (`/workspace` chuyển hướng, `/problems/[id]` luôn có id), id không có thì hiện "Không tìm thấy bài toán." (Sheet 9 NO 2); giữ việc ẩn chân trang từ `lg` ở màn này (điểm lệch 4). Bản dựng sửa theo BD: Run Code hiện từng testcase mẫu với tab "Case n", dữ liệu vào, output thực tế, kết quả mong đợi, thời gian và bộ nhớ (Khu vực I NO 3, 4, 6, 7, 11), giới hạn Time/Memory hiệu lực theo ngôn ngữ (Khu vực H NO 7); ràng buộc và câu hỏi mở rộng kết xuất từ `statement_md` (Q5); tải file đọc nội dung, kiểm phần mở rộng và 64 KB, thẻ file và "Xem nội dung" (Sheet 9 NO 5, 6), mã rỗng hoặc chưa chọn file thì không chạy (NO 7); nhãn lưu nháp chỉ hiện khi ghi `localStorage` thành công; trạng thái màn gắn theo `problemId`; nút "Lưu bài" lấy trạng thái đã lưu của bài; liên kết "Mở báo cáo đầy đủ" (EVT-17); nhãn, ngày giờ và kết quả lượt nộp đi qua i18n. Làm mới trích dẫn `problem-detail-view.tsx`. Khu vực B giữ lại trong Sheet 5, 6 dưới dạng đã bỏ | 2026/10/19 | Claude |
| V0.8 | Sheet 4 (4.4), 8 (EVT-20), Câu hỏi mở Q15 | Owner chốt 2026/10/19 (`DEC-2026-1019-usr-fe-audit-fixes`, chốt nốt câu hỏi mở còn lại sau rà prototype). Chỉ tài liệu, không đổi code, chưa duyệt hình. Chấp nhận ba điểm lệch bố cục (thanh kéo thứ hai giữa mã và kết quả, xếp dọc dưới `lg`, header vẫn hiện) làm thiết kế chính thức; viết lại mục 4.4 theo bản dựng (ponytail bậc 3). | 2026/10/19 | Claude |
| V0.9 | Phương châm, Sheet 1, 3, 4, 5, 7, 8, Câu hỏi mở Q12 | Theo `DEC-2026-1020-student-workspace-split-and-password-in-settings`: màn đổi đường dẫn từ `/problems/{id}` sang `/workspace/{problemId}`, nội dung và hành vi không đổi. Sheet 3 viết lại hai lối vào: từ nút "Giải bài toán" của `USR0104` và từ thẻ bài đang làm dở của `USR0105`; bỏ lối "nav chuyển hướng" (mục nav Workspace nay mở `USR0105`, `_shell` mục 2.2). Bỏ `ListMyUnsolvedRecentProblems` khỏi EVT-1 và Sheet 7.3 NO 10 vì màn này không còn dựng thẻ bài đang làm dở (việc đó thuộc `USR0105`). Q12 ghi nhận phần chuyển hướng đã thay, không xoá. Sửa các trích dẫn đã lệch tới `problem-bank.md`, `judge-orchestration.md`, `ai-review.md`, `identity.md`, `USR0101`, `_shell.md`, `01-rd/req/*`, `globals.css`, `problem-detail-view.tsx`. Chưa duyệt hình | 2026/10/20 | Claude |
| V0.10 | Sheet 5 (Khu vực B NO 3), Sheet 6 | Nút "Lưu bài" ở chân panel đổi sang màu vàng khi đã lưu (viền, nền nhạt, icon tô đặc, `aria-pressed`), cùng kiểu với nút Lưu của `USR0104`; `problem-detail-view.tsx` phần nút ở chân panel. Chưa duyệt hình | 2026/10/21 | Claude |
| V0.11 | Sheet 4 (Khu vực B), Sheet 8 | Bấm Run Code hoặc Submit mở thêm màn phụ (hộp thoại) liệt kê từng testcase: Run Code có danh sách testcase mẫu bên trái và chi tiết dữ liệu vào, thực tế, mong đợi bên phải (mở sẵn testcase sai đầu tiên); Submit liệt kê mọi testcase Đạt hoặc Không đạt, testcase ẩn không lộ dữ liệu. Bảng kết quả nhỏ chỉ còn tổng số đạt / không đạt, thời gian, bộ nhớ và nút "Xem chi tiết" mở lại màn phụ (bỏ hàng nút Case vì nhiều testcase bị tràn). `run-results-dialog.tsx`. Chưa duyệt hình | 2026/10/21 | Claude |
| V0.12 | Sheet 4 (4.3), 5 (Khu vực D NO 7-11), 7 (7.1 NO 10, 11, thêm NO 22, 23; 7.2 NO 4, 5, 6; 7.3 NO 2), 8 (EVT-12), 6 (Khu vực D NO 7, 10, 11), Câu hỏi mở Q5, Q6, thêm Q18, các dòng `[Nguồn]` | Sửa 2026-10-22 đồng bộ DB, `DEC-2026-1022-problem-bank-schema-sync` (PB-03, PB-08, PB-10) và `DEC-2026-1022-ai-judge-schema-sync`: (1) "Ràng buộc" là cột riêng `problems.constraints_md`, màn ghép với `statement_md` (Q5 viết lại; "Câu hỏi mở rộng" vẫn trong `statement_md`); (2) ví dụ mẫu và lời giải thích đọc từ bảng `problem.worked_examples`, không còn `testcases.is_worked_example`/`explanation_md` (Q6, thêm Q17 về endpoint mang ví dụ); (3) `function_signatures` đã gộp vào `problem_specs`, sửa 4.3, 7.1, 7.2; (4) EVT-12: "bảy" thành sáu trạng thái cuối `ACCEPTED`, `WRONG_ANSWER`, `TIME_LIMIT_EXCEEDED`, `RUNTIME_ERROR`, `COMPILE_ERROR`, `SYSTEM_ERROR`; (5) đọc lại số dòng trích dẫn `02-bd/database/{problem-bank,judge-orchestration,ai-review}.md`. Không đổi: Run Code không tạo dòng `submissions` (Q10), giới hạn 64 KB. Không đổi RD, không đổi code | 2026/10/22 | Claude |
| V0.13 | Sheet 7 (7.1 NO 23; 7.2 NO 5), Câu hỏi mở (Q5, Q18) | Sửa 2026-10-22 (đồng bộ DB vòng 2, DEC-2026-1022-db-sync-round2) mục 12 và 13: (1) đóng Q18, bỏ chữ `[Suy luận]` ở 7.1 NO 23 và 7.2 NO 5: ví dụ mẫu trả cùng `GetProblemForWorkspace`; (2) Q5 thêm một dòng: greenfield, bài cũ có mục `## Ràng buộc` (nếu có) tách một lần sang `constraints_md`. **Không đổi:** giao diện | 2026/10/22 | Claude |

---

## Sheet 3. Sơ đồ chuyển màn

> [Nội bộ] Quy ước vẽ: bố trí từ trái sang phải. Màn gọi tới tô tím, màn hiện tại tô xanh nhạt, màn đích
> tô tím. Màu và `[Nguồn:]` dùng để truy vết, không phải yêu cầu của khách hàng.

### 3.1 Danh sách chuyển màn

#### Tổng quan bài toán → Chi tiết bài tập

[Điều kiện mở] Bấm nút "Giải bài toán" ở đầu cột phải của màn `problem_overview` (`USR0104`)
[Nguồn: 02-bd/screens/users/USR0104_problem_overview.md:164,169,664]. Đây là lối vào chính: mọi đường "chọn một bài"
(bấm dòng hay "Vào giải" ở `problem_list`, "Bài ngẫu nhiên", mục trong hai khối phụ, `saved_problems`) đều mở `USR0104` trước
rồi mới sang đây [Nguồn: 02-bd/screens/users/USR0101_problem_list.md Sheet 3.1 và Sheet 4.1 bước 5; .nexa/control/decision-registry.md:4239-4243].

[Chế độ mở] Chế độ giải bài, có ghi (nộp bài, lưu bài).

[Thông tin truyền] `problem_id` trên đường dẫn `/workspace/{problemId}`. Không mang theo ngôn ngữ hay mô hình nộp nào
[Nguồn: 02-bd/screens/users/USR0104_problem_overview.md:169] và không mang theo bộ lọc của màn danh sách.

[Giá trị trả về] Không có.

[Khi thành công] Mở workspace ở trạng thái đã có bài: panel đề bài bên trái, vùng soạn mã và bảng kết quả
bên phải.

[Khi huỷ] Không có.

#### Trang đầu Workspace → Chi tiết bài tập

[Điều kiện mở] Bấm một thẻ trong khối "Bài đang làm dở" ở `workspace_home` (`USR0105`, đường dẫn `/workspace`)
[Nguồn: 02-bd/screens/users/USR0105_workspace_home.md:110,176]. Mục nav "Workspace" của khung khu Người học nay dẫn tới
`USR0105`, **không còn chuyển hướng** thẳng vào màn này [Nguồn: 02-bd/screens/users/_shell.md:63,163].

[Chế độ mở] Chế độ giải bài.

[Thông tin truyền] `problem_id` của thẻ được bấm, ghi lên đường dẫn `/workspace/{problemId}`. Nguồn danh sách bài đang
làm dở (lượt nộp gần nhất chưa đạt `ACCEPTED`) thuộc `USR0105`, không thuộc màn này.

[Giá trị trả về] Không có.

[Khi thành công] Mở workspace của đúng bài đã chọn.

[Khi huỷ] Không có.

#### Chi tiết bài tập → Danh sách bài tập

[Điều kiện mở] Bấm liên kết "Danh sách bài" trên thanh tác vụ
[Nguồn: 09-layoutBase/Workspace giải bài.dc.html:99; 05-coding/frontend/src/views/users/problem-detail/ui/problem-detail-view.tsx:782-787],
hoặc liên kết sang danh sách ở thông báo "Không tìm thấy bài toán."
[Nguồn: 05-coding/frontend/src/views/users/problem-detail/ui/problem-detail-view.tsx:94-96]. Liên kết đích là `/problems`
(danh sách), không phải `/problems/{id}` (tổng quan). Bản dựng không có liên kết ngược sang `USR0104` từ màn này
`[SoT: Suy luận]` (không tìm thấy trong `problem-detail-view.tsx`).

[Chế độ mở] Chế độ duyệt, chỉ đọc.

[Thông tin truyền] Không có.

[Giá trị trả về] Không có.

[Khi thành công] Mở màn `problem_list`.

[Khi huỷ] Không có. Không hỏi xác nhận khi rời màn vì mã đã được giữ ở trình duyệt (Câu hỏi mở Q14, EVT-21 đã bỏ).

#### Chi tiết bài tập → Kết quả nộp bài

[Điều kiện mở] Bấm liên kết "Xem kết quả chấm" trong bảng kết quả sau khi một lượt nộp đạt `ACCEPTED`
[Nguồn: 09-layoutBase/Workspace giải bài.dc.html:379], hoặc bấm một dòng trong tab "Bài nộp"
[Nguồn: 09-layoutBase/Workspace giải bài.dc.html:218-225].

[Chế độ mở] Chế độ xem kết quả, chỉ đọc.

[Thông tin truyền] `submission_id` của lượt nộp được chọn.

[Giá trị trả về] Không có.

[Khi thành công] Mở màn `submission_result`.

[Khi huỷ] Không có (không hỏi xác nhận, Câu hỏi mở Q14).

#### Chi tiết bài tập → Phân tích bài giải

[Điều kiện mở] Bấm liên kết "Phân tích bài giải" trong bảng kết quả khi lượt nộp đạt `ACCEPTED`
[Nguồn: 09-layoutBase/Workspace giải bài.dc.html:380], hoặc bấm liên kết mở báo cáo đầy đủ trong tab
"Solution Review" [Nguồn: 01-rd/screens/users/USR0102_problem_detail.md:116].

[Chế độ mở] Chế độ xem báo cáo, chỉ đọc. F5-01 là hành động **tự chọn**, hệ thống không tự chạy
(`DEC-2026-0831-outside-screens-closures`).

[Thông tin truyền] `submission_id` của lượt nộp `ACCEPTED`.

[Giá trị trả về] Không có.

[Khi thành công] Mở màn `solution_review`.

[Khi huỷ] Phân hệ AI không khả dụng thì liên kết **không hiển thị**, không báo lỗi — xem Sheet 9 NO 13.

#### Chi tiết bài tập → Phỏng vấn giả lập

[Điều kiện mở] Bấm liên kết "Phỏng vấn giả lập" trong bảng kết quả khi lượt nộp đạt `ACCEPTED`
[Nguồn: 09-layoutBase/Workspace giải bài.dc.html:381], hoặc nút "Bắt đầu Mock Interview với bài này" trong
tab "Solution Review" [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:245].

[Chế độ mở] Chế độ hội thoại nhiều lượt.

[Thông tin truyền] `submission_id` của lượt nộp `ACCEPTED`. Lối vào này ứng với
`interview_sessions.entry_type = SUBMISSION` [Nguồn: 02-bd/database/ai-review.md:101-102].

[Giá trị trả về] Không có.

[Khi thành công] Mở màn `mock_interview`.

[Khi huỷ] Phân hệ AI không khả dụng thì liên kết không hiển thị.

### 3.2 Sơ đồ

```mermaid
flowchart LR
    list["Danh sách bài tập<br/>problem_list"] -->|"chọn bài"| overview["Tổng quan bài toán<br/>problem_overview"]
    overview -->|"Giải bài toán"| main["Chi tiết bài tập<br/>problem_detail"]
    nav["Khung Người học<br/>mục Workspace"] -->|"mở trang đầu"| home["Trang đầu Workspace<br/>workspace_home"]
    home -->|"bấm thẻ bài đang làm dở"| main
    main -->|"Danh sách bài"| list
    main -->|"Xem kết quả chấm / tab Bài nộp"| result["Kết quả nộp bài<br/>submission_result"]
    main -->|"Phân tích bài giải"| review["Phân tích bài giải<br/>solution_review"]
    main -->|"Phỏng vấn giả lập"| interview["Phỏng vấn giả lập<br/>mock_interview"]

    classDef source fill:#F3E5F5,stroke:#9C5CC4,color:#000
    classDef screen fill:#E3F2FD,stroke:#3B82F6,color:#000

    class list,nav,overview,home,result,review,interview source
    class main screen
```

[Nguồn: 09-layoutBase/Workspace giải bài.dc.html:99,218-225,379,380,381,245;
02-bd/screens/users/_shell.md:63; 02-bd/screens/users/USR0104_problem_overview.md:164,169;
02-bd/screens/users/USR0105_workspace_home.md:110,176]

---

## Sheet 4. Bố cục màn hình

### 4.1 Tổng quan màn

[Mục đích màn] Nơi người học đọc đề, viết mã trong trình soạn thảo hoặc tải file mã nguồn lên, chạy thử
với testcase mẫu, và nộp bài để chấm với testcase ẩn — cho **cả hai** mô hình nộp bài song song
[Nguồn: 01-rd/screens/users/USR0102_problem_detail.md:18-20].

[Luồng nghiệp vụ chính]

1. **Hiển thị ban đầu**: vào màn với `problem_id`, hệ thống tải song song năm nhóm dữ liệu — đề bài và
   giới hạn, mã khung theo cặp (ngôn ngữ, mô hình) đang chọn, testcase mẫu, lịch sử nộp bài của chính
   người học cho bài này, và tóm tắt phân tích bài giải nếu đã có. Đường dẫn `/workspace/{problemId}` luôn mang
   `problem_id`; id không mở được thì hiện "Không tìm thấy bài toán." (Sheet 9 NO 2). Việc chọn bài đang làm dở thuộc `USR0105`. Mã người học đã
   gõ cho cặp (ngôn ngữ, mô hình) này được khôi phục từ bản nháp ở trình duyệt nếu có, thay cho mã khung.
2. **Chọn ngôn ngữ và mô hình**: ba tab ngôn ngữ (Java / C++ / Python) và hai nút mô hình ("Có hàm main" /
   "Bọc hàm"). Đổi một trong hai thì **mã khung được sinh lại** theo đúng cặp mới
   [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:594-601]. Người học chọn tự do **mỗi lượt làm bài**,
   không phải lựa chọn cố định theo bài (`DEC-2026-0824-dual-submission-model-per-problem`).
3. **Chọn phương thức nhập mã**: "Gõ trực tiếp" hoặc "Tải file" — **trục độc lập** với mô hình nộp bài,
   đổi cái này không đặt lại cái kia [Nguồn: 01-rd/screens/users/USR0102_problem_detail.md:136].
4. **Chạy thử**: bấm "Run Code" chạy với testcase Sample, hiển thị input, output thực tế và kết quả mong
   đợi cho từng case. Chạy thử **không ghi nhận vào tiến độ** (F4-02)
   [Nguồn: 01-rd/req/judge-orchestration.md:16].
5. **Nộp bài**: bấm "Submit", máy chủ trả về `submissionId` ngay và không chờ kết quả (F4-01)
   [Nguồn: 01-rd/req/judge-orchestration.md:10-11]; màn chuyển sang tab "Kết quả", mở kênh thời gian thực
   theo `submissionId` và cập nhật dần trạng thái từng testcase (F4-08)
   [Nguồn: 01-rd/req/judge-orchestration.md:48-49].
6. **Sau khi Accepted**: bảng kết quả hiện ba lối đi tiếp theo — xem kết quả chấm đầy đủ, phân tích bài
   giải, phỏng vấn giả lập [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:377-383].

[Người dùng] Người học đã đăng nhập (`STUDENT`). Màn có thao tác ghi (nộp bài, lưu bài) nên **bắt buộc
đăng nhập**, không có chế độ khách — khác `problem_list` vốn còn để ngỏ
[Nguồn: 02-bd/screens/users/USR0101_problem_list.md Sheet 4.1 mục [Người dùng]].

[Tệp liên quan] **Có nhập tệp**: file mã nguồn tải lên, giới hạn đúng phần mở rộng của ba ngôn ngữ
(`.java`, `.cpp`, `.py`) và tối đa 64 KB
[Nguồn: 01-rd/req/judge-orchestration.md:14-15; 09-layoutBase/Workspace giải bài.dc.html:307]. Nội dung
file nạp vào cùng một luồng nộp bài F4-01, **không phải một kênh nộp bài riêng**
[Nguồn: 01-rd/req/judge-orchestration.md:13-14]. Không có xuất tệp.

[Phạm vi]
- Chỉ mở được bài có `status = PUBLISHED` và `deleted = false`
  (`DEC-2026-0830-problem-lifecycle-two-states`) [Nguồn: 02-bd/database/problem-bank.md:19-20].
- **Ba tab** ở panel trái: "Đề bài", "Bài nộp", "Solution Review". Tab "Gợi ý" của prototype đã bị cắt
  khỏi phạm vi (`DEC-2026-0831-remove-tiered-hints-ai-config`); prototype tĩnh lỗi thời ở điểm này, Câu hỏi mở Q1
  đã chốt 2026-10-18.
- **Không có đồng hồ phiên làm bài** và **không có khối "Gợi ý theo điểm yếu của bạn"** (Câu hỏi mở Q3, Q2,
  chốt 2026-10-18).
- **Bản nháp mã chỉ lưu ở trình duyệt** (`localStorage`), không có bảng nháp ở máy chủ (Câu hỏi mở Q4).
- **Không có nút "Yêu cầu review"** (`DEC-2026-0830-remove-student-review-request`).
- Không hiển thị nội dung testcase ẩn dưới mọi hình thức. Testcase hiển thị được **chỉ** là
  `testcases.visibility = SAMPLE` [Nguồn: 02-bd/database/problem-bank.md:142].
- Không mô tả lại state machine bài nộp — chỉ ánh xạ chín trạng thái của `submissions.status` sang nhãn
  hiển thị [Nguồn: 02-bd/database/judge-orchestration.md:22].
- Không có fail-fast: mọi testcase đều chạy, nhãn kết quả luôn kèm tỉ lệ đạt
  (`DEC-2026-0831-partial-score-testcase-ratio`) [Nguồn: 02-bd/architecture/judge-orchestration.md:30,161].
- Chỉ số "Beats" (F4-12) **không** hiển thị ở màn này dù prototype có vẽ; chỉ có ở `submission_result` (Câu hỏi mở Q11, chốt 2026-10-18).

[Quyền sử dụng]
- Xem: được, với người học đã đăng nhập.
- Thêm: có — tạo một bài nộp (`judge.submissions`), tạo một dòng lưu bài (`problem.bookmarks`).
- Sửa: không.
- Xoá: có, giới hạn ở việc bỏ lưu bài (xoá dòng `bookmarks` của chính người dùng).

[Số bản ghi tối đa] Tab testcase mẫu: theo số dòng `testcases` có `visibility = SAMPLE` của bài, prototype
minh hoạ 3 case [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:509-513]. Ví dụ trong tab "Đề bài":
prototype minh hoạ 3 ví dụ [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:563-567]. Tab "Bài nộp": 10
lượt nộp gần nhất của chính người học cho chính bài này `[SoT: Suy luận]` — prototype minh hoạ 4 dòng
[Nguồn: 09-layoutBase/Workspace giải bài.dc.html:218], không nêu giới hạn; đề xuất 10 và có liên kết sang
`my_submissions` cho phần còn lại. Dải ô trạng thái testcase: bằng `submissions.total_testcase_count`,
prototype minh hoạ 40 ô [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:643]. Mã nguồn: tối đa 64 KB, xem
Câu hỏi mở Q7.

### 4.2 DTO liên quan

- `ProblemWorkspaceDto`
- `StarterCodeDto`
- `SampleTestcaseDto`
- `ExecutionLimitsDto`
- `MySubmissionSummaryDto`
- `SubmissionTicketDto`
- `SubmissionProgressDto`
- `SampleRunResultDto`
- `SolutionReviewQuickInsightDto`

`[Suy luận]` — tên DTO do BD này đề xuất, `03-dd/api/problem-bank.md`, `03-dd/api/harness.md` và
`03-dd/api/judge-orchestration.md` chốt lại. Đặt tên **khác** DTO của màn quản trị và của màn danh sách:
`ProblemWorkspaceDto` không dùng lại `ProblemListItemDto`
[Nguồn: 02-bd/screens/users/USR0101_problem_list.md Sheet 4.2] vì hai màn khác hẳn tập trường — màn này cần
`statement_md`, giới hạn thời gian/bộ nhớ, cờ `function_wrapper_supported`, chữ ký hàm; màn kia cần
`acRate`, `solveState`, `topicNames`.

### 4.3 Bảng dữ liệu liên quan (13)

| NO | Bảng | Ghi chú |
| --: | :--- | :--- |
| 1 | `problem.problems` | Đề bài, khoá `level_id` của độ khó, giới hạn cơ sở, cờ mô hình [Nguồn: 02-bd/database/problem-bank.md:8-37] |
| 2 | `problem.topics` | Nhãn chủ đề trên tab Đề bài [Nguồn: 02-bd/database/problem-bank.md:57-75] |
| 3 | `problem.problem_topics` | Nối bài với chủ đề [Nguồn: 02-bd/database/problem-bank.md:57-75] |
| 4 | `problem.problem_specs` | Định dạng `stdin`/`stdout` cho mô hình Standard I/O **và chữ ký hàm chung** (`function_name`, `return_type`, `parameters`, `name_overrides`; tên theo từng ngôn ngữ do hệ thống suy ra) — đầu vào sinh mã khung. Chữ ký hàm trước nằm ở bảng `function_signatures`, đã gộp vào đây (PB-10, `DEC-2026-1022-problem-bank-schema-sync` mục 5) [Nguồn: 02-bd/database/problem-bank.md:108-129] |
| 5 | `problem.worked_examples` | Ví dụ mẫu hiển thị kèm đề (`position`, `input`, `output`, `explanation_md`); **tách khỏi `testcases`**, không dùng để chấm (PB-08, `DEC-2026-1022-problem-bank-schema-sync` mục 4) [Nguồn: 02-bd/database/problem-bank.md:234-247] |
| 6 | `problem.testcases` | **Chỉ dòng `visibility = SAMPLE`** [Nguồn: 02-bd/database/problem-bank.md:136-155] |
| 7 | `problem.problem_stats` | Read model, nguồn chỉ số "AC rate" ở chân panel [Nguồn: 02-bd/database/problem-bank.md:195-208] |
| 8 | `problem.bookmarks` | Nút "Lưu bài" (F2-13) [Nguồn: 02-bd/database/problem-bank.md:189-194] |
| 9 | `judge.submissions` | Tạo lượt nộp, lịch sử nộp bài của bài này [Nguồn: 02-bd/database/judge-orchestration.md:12-34] |
| 10 | `judge.submission_testcase_results` | Dải ô trạng thái từng testcase — **chỉ đọc cột `verdict` và `testcase_order`** [Nguồn: 02-bd/database/judge-orchestration.md:50-61] |
| 11 | `judge.language_configs` | Hệ số nhân giới hạn theo ngôn ngữ, để hiện giới hạn hiệu lực [Nguồn: 02-bd/database/judge-orchestration.md:101-112] |
| 12 | `ai.solution_reviews` | Tóm tắt phân tích bài giải ở tab thứ ba [Nguồn: 02-bd/database/ai-review.md:35-50] |
| 13 | `problem.problem_levels` | Nhãn độ khó (`display_name`) của bài, đọc qua join `level_id` [Nguồn: 02-bd/database/problem-bank.md mục `problem_levels`] |

Bốn bảng NO 9-12 nằm ở schema của module khác. Màn không truy vấn thẳng: mỗi module tự phục vụ phần dữ liệu
của nó qua endpoint riêng, đúng nguyên tắc mỗi schema một chủ
[Nguồn: 02-bd/database/problem-bank.md:35-36].

### 4.4 Vùng bố cục

Đối chiếu `09-layoutBase/Workspace giải bài.dc.html` — bằng chứng bố cục chỉ-đọc, **không phải** design
system cuối cùng.

| Vùng | Vị trí trong prototype | Nội dung |
| :--- | :--- | :--- |
| Header khu Người học, biến thể thu gọn | `:49-96` | Thương hiệu, 6 mục nav, avatar không kèm tên và email — dùng lại khung chung, không mô tả lại |
| Thanh tác vụ bài toán | `:98-107` | Liên kết "Danh sách bài", mã và tên bài, độ khó, nút "Run Code" và "Submit" đẩy sang phải. Chỉ hiện khi đã chọn bài. Prototype còn vẽ đồng hồ phiên (`:104`) — **bỏ** (Q3) |
| ~~Trạng thái chưa chọn bài~~ (đã bỏ 2026-10-19) | `:112-152` | Tiêu đề hướng dẫn, thẻ bài đang làm dở kèm nút "Tiếp tục", liên kết sang `problem_list`. Danh sách "Gợi ý theo điểm yếu" của prototype (`:136-150`) — **bỏ** (Q2) |
| Panel thông tin bài toán (cột trái) | `:158-256` | Dải tab, nội dung tab đang chọn, chân panel (AC rate, nút "Lưu bài", trạng thái cụm chấm) |
| Tab "Đề bài" | `:166-202` | Tên bài, ba nhãn (độ khó, chủ đề, mô hình đang chọn), nội dung đề, câu giải thích mô hình, ví dụ, ràng buộc, câu hỏi mở rộng |
| Tab "Bài nộp" | `:216-227` | Mỗi dòng một lượt nộp: kết quả, ngôn ngữ, thời gian chạy, thời điểm |
| Tab "Solution Review" | `:229-247` | Hai ô chỉ số độ phức tạp, các khối nhận xét rút gọn, nút mở phỏng vấn giả lập |
| Thanh chia đôi bố cục | `:258-260` | Kéo ngang để đổi tỉ lệ hai cột |
| Vùng soạn mã (cột phải, hàng trên) | `:264-325` | Thanh công cụ (ngôn ngữ, mô hình, phương thức nhập), vùng soạn mã hoặc vùng tải file, chân vùng (nhãn "Đã lưu trên trình duyệt này", giới hạn) |
| Bảng kết quả (cột phải, hàng dưới) | `:327-392` | Hai tab "Testcase" và "Kết quả", nhãn verdict tổng hợp bên phải, nội dung tab đang chọn |

Bố cục hai cột chia đôi kéo được: cột trái rộng theo tỉ lệ mặc định 47%, giới hạn kéo trong khoảng
22%–74%, nhớ ở `localStorage` khoá `algoprep-workspace-split-x`. Cột phải **cũng chia bằng một thanh kéo thứ hai**
giữa vùng soạn mã (hàng trên) và bảng kết quả (hàng dưới): mặc định 62/38, giới hạn kéo 25%–85%, nhớ ở khoá
`algoprep-workspace-split-y`. Cả hai thanh là `shared/ui` `SplitPane` (kéo chuột, phím mũi tên, nhấp đúp để về mặc định).
Từ `lg` trở lên màn cao đúng phần còn lại của khung và không cuộn ở cấp trang; chỉ nội dung trong từng panel cuộn riêng.
**Dưới `lg`** hai cột xếp dọc (đề bài trên, vùng mã và kết quả dưới) và trang cuộn bình thường. **Header khu học viên vẫn
hiện** phía trên Workspace (không ẩn như prototype `.dc.html`). Không quy định màu sắc, khoảng cách hay typography ở BD.

**Chốt 2026-10-19 (owner, Q15):** ba điểm trên (thanh kéo thứ hai giữa mã và kết quả, xếp dọc dưới `lg`, header vẫn hiện) là
**thiết kế chính thức**; bản dựng là đặc tả, không còn là lệch BD. Prototype `Workspace giải bài.dc.html` (lưới hai hàng
`minmax(300px, 1fr) minmax(180px, 246px)` cố định, header thu gọn) chỉ còn là bằng chứng tham khảo.

**Hiện trạng bản dựng (2026-10-20, chưa duyệt hình)** [Nguồn: 05-coding/frontend/src/views/users/problem-detail/ui/problem-detail-view.tsx:810-841]:
bố cục đúng như đoạn đặc tả ở trên. **Đường dẫn (2026-10-20):** màn nằm ở `app/(student)/workspace/[problemId]/page.tsx`, chỉ render
`ProblemDetailView` (`:3-5`); view đọc `problemId` từ `useParams` (`05-coding/frontend/src/views/users/problem-detail/ui/problem-detail-view.tsx:80-81`) và khi id lạ thì hiện "Không tìm thấy bài toán." kèm liên
kết về `/problems` (`:89-99`). `/problems/[problemId]` giờ là màn tổng quan `USR0104` (`app/(student)/problems/[problemId]/page.tsx`), không còn
chứa Workspace. **Chưa khớp BD (nợ prototype):** Sheet 8 ghi `lang`, `model`, `tab` vào tham số truy vấn, nhưng bản dựng giữ chúng bằng
`useState` cục bộ (`:119-122`), không ghi vào URL. Điểm đã chốt trước đó: **chân trang khung chung bị ẩn hoàn toàn từ `lg` trở lên** khi màn gắn
`data-fullscreen-view` (`05-coding/frontend/src/app/globals.css:705-733`; thuộc tính đặt ở `05-coding/frontend/src/views/users/problem-detail/ui/problem-detail-view.tsx:773`), trong khi `_shell.md` mục 3 quy định chân trang có
trên mọi màn khu học viên; chủ dự án chốt 2026-10-19 là **giữ** như vậy cho màn này, dưới `lg` chân trang vẫn hiện. Màu nền sáng của khu này đã đổi ngày
2026-10-20 (xem `_shell.md` mục 5.1), chưa duyệt hình cho màn này.

**Bản nháp mã (2026-10-18, theo Câu hỏi mở Q4)** [Nguồn: 05-coding/frontend/src/views/users/problem-detail/ui/problem-detail-view.tsx:147,156-166,210-220,595-597; 05-coding/frontend/src/entities/problem/model/workspace-draft.ts:7-12]:
khoá `localStorage` là `algoprep-draft:{problemId}:{language}:{model}` (`workspaceDraftKey`, nay nằm ở `entities/problem`, `workspace-draft.ts:7-12`; view gọi
ở `:147`; bài chỉ có Standard I/O dùng `stdio`); mở màn thì khôi phục (`:156-166`), mỗi lần gõ thì ghi lại trong `setCode` qua `saveWorkspaceDraft` (`:210-220`),
lỗi ghi (trình duyệt chặn hoặc đầy) thì vẫn sửa được nhưng không nhớ. Từ 2026-10-19 nhãn chỉ hiện khi `saveWorkspaceDraft` ghi thành công (`persistedKeys`), không còn hiện khi `localStorage` bị chặn. Nhãn lấy từ i18n `toolbar.draftSaved`
("Đã lưu trên trình duyệt này", `05-coding/frontend/messages/vi.json:300`) và chỉ hiện khi phương thức nhập là gõ trực tiếp và cặp đang chọn đã có bản đã lưu
(`:595-597`). Bản dựng không có hộp xác nhận rời màn hay ghi đè mã, và theo Câu hỏi mở Q14 (đã chốt) cũng sẽ không có.

**Thiết lập nhớ, giới hạn mã và chạy thử (2026-10-18, theo Câu hỏi mở Q6 đến Q10)** [Nguồn: 05-coding/frontend/src/views/users/problem-detail/ui/problem-detail-view.tsx:69-71,172-192,194-210,260-275,289-313,396-398]:
khoá `localStorage` `algoprep-workspace-prefs` lưu `{language, mode}` của lần chọn gần nhất; ghi trong hai hàm đổi ngôn ngữ
và đổi mô hình (`:186-210`), khôi phục trong một effect sau khi màn gắn vào (`:172-184`), lỗi đọc hoặc ghi thì bỏ qua và dùng
mặc định. Bài chỉ có Standard I/O luôn dùng Standard I/O bất kể lựa chọn đã nhớ (`effectiveMode`, `:146-147`). Hằng `MAX_CODE_BYTES` = 64 KB
(`:69`); hàm `sourceToSend` (`:260-275`) kiểm mã rỗng, chưa chọn file và quá 64 KB (đo bằng `TextEncoder` qua `byteLength`, `:76-78`) rồi chặn Run Code, Submit bằng toast
(`problemDetail.toast.codeTooLarge` khi vượt); kiểm file tải lên ở `handleFile` (`:224-246`). Kiểm tra thật vẫn ở máy chủ. Run Code
(`runSample`, `:289-313`) chỉ giả lập kết quả ở phía trình duyệt và không tạo bản ghi nào, đúng với Q10. Bản dựng không có chấm
trạng thái cụm chấm go-judge nào, đúng với Q8. Giải thích ví dụ đã in khi dữ liệu có `example.explanation`
(`:396-398`), đúng với Q6 (cột nguồn nay là `worked_examples.explanation_md`, PB-08).

**Prototype tĩnh `09-layoutBase/Workspace giải bài.dc.html` đã lỗi thời ở các điểm sau** (chủ dự án chốt
2026-10-18; file chỉ-đọc nên chỉ đánh dấu ở đây, không sửa): tab "Gợi ý" (Q1), khối "Gợi ý theo điểm yếu" (Q2),
đồng hồ phiên (Q3), ô "Beats" (Q11), số thứ tự `1.` thay cho `problems.code` (Q13).

**Ba chỗ trong prototype là mã của bản mẫu, không đưa vào BD:**

1. Tab "Gợi ý" `:205-214`, khai báo tab `:552`, cờ `tabIsHint` `:559`, dữ liệu `hints` `:574-578` và nhãn
   "Gợi ý" trong dải badge `:173` — tính năng đã bị cắt, xem Câu hỏi mở Q1 (đã chốt).
2. Nhánh `pendingTitle` `:119-124` cùng hàm `choose()` `:426-432` — chỉ để bản mẫu báo rằng chỉ có bài
   Two Sum mới có đề đầy đủ, không phải hành vi của hệ thống thật.
3. Bộ chuyển sáng/tối và VI/EN ở header `:65-70` — thuộc khung chung, đã mô tả ở `_shell`.

### 4.5 Cấu trúc slice FSD [Nội bộ]

| Khối | Slice dự kiến | Căn cứ |
| :--- | :--- | :--- |
| Trang | `views/users/problem-detail` | Quy ước FSD của dự án |
| Khung Người học (thu gọn) | Dùng lại `widgets/app-shell` (biến thể `student-header`, tham số thu gọn) | `02-bd/screens/users/_shell.md` mục 2.5 |
| Thanh tác vụ bài toán | `widgets/problem-taskbar` | Prototype `:98-107` |
| Panel thông tin bài toán | `widgets/problem-info-panel` | Prototype `:158-256` |
| Nội dung đề bài (Markdown + LaTeX) | `entities/problem` (dùng lại với `problem_list`, `saved_problems`) | `02-bd/screens/users/USR0101_problem_list.md Sheet 4.5 (ghi chú sau bảng slice)` |
| Lịch sử nộp bài của bài này | `entities/submission` | Prototype `:216-227` |
| Tóm tắt phân tích bài giải | `widgets/solution-review-summary` | Prototype `:229-247` |
| Thanh chia đôi | `shared/ui/split-pane` | Prototype `:258-260` |
| Thanh công cụ soạn mã | `features/code-toolbar` | Prototype `:265-285` |
| Trình soạn mã Monaco | `features/code-editor` | Prototype `:287-301`, `README.md` mục 5 (Monaco Editor) |
| Tải file mã nguồn | `features/source-file-upload` | Prototype `:303-318` |
| Chạy thử và nộp bài | `features/submit-solution` | Prototype `:105-106` |
| Bảng kết quả và kênh thời gian thực | `widgets/judge-console` + `shared/api/stomp` | Prototype `:327-392` |

`[Suy luận]` — ánh xạ slice do BD đề xuất, DD màn hình chốt lại. `entities/submission` được
`submission_result` và `my_submissions` dùng lại, nên dòng lịch sử nộp bài phải là component nhận dữ liệu
từ ngoài, không tự gọi API.

> [Nội bộ] Ảnh minh hoạ đặt ở `08-diagram/02-bd/screens/users/`, chụp bằng Playwright trên ứng dụng
> Next.js thật khi đã có mã chạy được. Chưa có thì tham chiếu prototype, không vẽ tay.

---

## Sheet 5. Danh sách item màn hình

> Quy ước ID, bộ thẻ và giá trị chuẩn: `02-bd/_rules/bd-template-9sheet.md` mục 2, 3, 4.

### Khu vực A — Thanh tác vụ bài toán

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Thanh tác vụ bài toán | | | | | | | | | | | | | |
| | 1 | Liên kết Danh sách bài | `problemDetail.taskbar.linkProblemList` | - | - | Link | - | - | - | I | - | - | Quay về màn `problem_list` [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:99]<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] EVT-19 |
| | 2 | Mã và tên bài | `problemDetail.taskbar.problemTitle` | `problem.problems` | `code`, `title` | Label | String | 240 | - | O | - | `{mã}. {tên}` | Định danh bài đang mở [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:101]<br>[Nguồn giá trị] Cột `code` và `title` [Nguồn: 02-bd/database/problem-bank.md:14-15]. Hiển thị `code` nguyên dạng chuỗi, không dựng số thứ tự (Câu hỏi mở Q13, chốt 2026-10-18; prototype hiển thị `1.` là lỗi thời) [Nguồn: 05-coding/frontend/src/views/users/problem-detail/ui/problem-detail-view.tsx:788-790]<br>[EVT liên quan] EVT-1 |
| | 3 | Độ khó | `problemDetail.taskbar.difficulty` | `problem.problem_levels` | `display_name` | Badge | String | - | - | O | - | Nhãn `display_name` | Mức độ khó [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:102]<br>[Nguồn giá trị] **đọc từ dữ liệu** `problem_levels.display_name` (qua `problems.level_id`), không còn ánh xạ cứng enum; mức khởi tạo giữ màu badge cũ, mức mới màu trung tính `DEC-2026-1001-admin-configurable-settings` mục (7) [Nguồn: 02-bd/database/problem-bank.md mục `problem_levels`]<br>[EVT liên quan] - |
| | 4 | Nút Run Code | `problemDetail.taskbar.btnRun` | - | - | Button | - | - | - | I | - | - | Chạy thử mã hiện tại với toàn bộ testcase mẫu, không ghi nhận tiến độ (F4-02) [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:105]<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] EVT-10 |
| | 5 | Nút Submit | `problemDetail.taskbar.btnSubmit` | `judge.submissions` | - | Button | - | - | - | I | - | - | Tạo một lượt nộp và chấm với testcase ẩn (F4-01) [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:106]<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] EVT-11 |

### Khu vực B — Trạng thái chưa chọn bài

**Đã bỏ (owner chốt 2026-10-19).** Màn không còn đường vào trạng thái chưa chọn bài: đường dẫn `/workspace/{problemId}` luôn mang id (từ 2026-10-20 mục nav Workspace mở `USR0105`, không còn chuyển hướng, Q12). Id không mở được thì hiện "Không tìm thấy bài toán." (Sheet 9 NO 2). Bốn item cũ (`problemDetail.emptyState.*`) và widget `workspace-empty-state` bị gỡ; giữ tiêu đề mục để không lệch tham chiếu.

### Khu vực C — Panel thông tin bài toán

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Panel thông tin bài toán | | | | | | | | | | | | | |
| | 1 | Dải tab panel | `problemDetail.infoPanel.tabs` | - | - | List | Enum | - | - | I/O | `Đề bài` | - | **Ba** tab: Đề bài / Bài nộp / Solution Review [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:159-163]. Tab thứ tư "Gợi ý" của prototype [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:552] đã bị cắt (`DEC-2026-0831-remove-tiered-hints-ai-config`)<br>[Nguồn giá trị] Nhãn tĩnh i18n; tab đang chọn lấy từ tham số URL `tab`<br>[EVT liên quan] EVT-3 |
| | 2 | AC rate | `problemDetail.infoPanel.acRate` | `problem.problem_stats` | `ac_rate` | Label | Number | 5 | - | O | `-` | `{số}%` | Tỉ lệ bài nộp được chấp nhận trên toàn hệ thống cho bài này [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:251]<br>[Nguồn giá trị] Read model `problem_stats.ac_rate` [Nguồn: 02-bd/database/problem-bank.md:195-208]. Chưa có dòng read model thì hiển thị `-`, không hiển thị `0%` — cùng quy ước với `problem_list` [Nguồn: 02-bd/screens/users/USR0101_problem_list.md Sheet 5 Khu vực D, item AC rate]<br>[EVT liên quan] EVT-1 |
| | 3 | Nút Lưu bài | `problemDetail.infoPanel.btnBookmark` | `problem.bookmarks` | `user_id`, `problem_id` | Button | Boolean | - | - | I/O | Chưa lưu | - | Bật/tắt đánh dấu lưu bài của chính người học (F2-13) [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:253]<br>[Công thức] Đã lưu khi tồn tại dòng `bookmarks` khớp `(user_id hiện tại, problem_id)` [Nguồn: 02-bd/database/problem-bank.md:189-194]; nhãn đổi giữa "Lưu bài" và "Bỏ lưu"<br>[EVT liên quan] EVT-15 |
| | 4 | Trạng thái cụm chấm | `problemDetail.infoPanel.judgeClusterStatus` | - | - | Badge | Enum | - | - | O | Ẩn | - | Cho biết cụm go-judge có sẵn sàng nhận bài hay không [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:254]<br>[Nguồn giá trị] Ẩn cho tới khi có dữ liệu, không gọi định kỳ — theo kết luận của khung chung [Nguồn: 02-bd/screens/users/_shell.md:164], xem Câu hỏi mở Q8 (đã chốt 2026-10-18)<br>[EVT liên quan] EVT-1 |
| | 5 | Thanh chia đôi bố cục | `problemDetail.infoPanel.splitHandle` | - | - | Button | Number | - | - | I/O | `47` | `{số}%` | Kéo ngang để đổi tỉ lệ hai cột [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:258-260]<br>[Công thức] Tỉ lệ bề rộng cột trái, chặn trong khoảng 22–74 [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:441]; giá trị lưu ở phía trình duyệt, không lưu DB<br>[EVT liên quan] EVT-20 |

### Khu vực D — Tab Đề bài

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Tab Đề bài | | | | | | | | | | | | | |
| | 1 | Tên bài | `problemDetail.statement.title` | `problem.problems` | `code`, `title` | Label | String | 240 | - | O | - | `{mã}. {tên}` | Tiêu đề đề bài [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:168]<br>[Nguồn giá trị] Cùng nguồn Khu vực A NO 2<br>[EVT liên quan] - |
| | 2 | Nhãn độ khó | `problemDetail.statement.difficultyBadge` | `problem.problem_levels` | `display_name` | Badge | String | - | - | O | - | Nhãn `display_name` | [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:170]<br>[Nguồn giá trị] Như Khu vực A NO 3<br>[EVT liên quan] - |
| | 3 | Nhãn chủ đề | `problemDetail.statement.topicBadge` | `problem.topics` | `name` | Badge | String | - | - | O | `-` | - | Chủ đề của bài, nhiều chủ đề thì mỗi chủ đề một nhãn [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:171]<br>[Nguồn giá trị] `topics.name` qua `problem_topics` [Nguồn: 02-bd/database/problem-bank.md:57-75]<br>[EVT liên quan] - |
| | 4 | Nhãn mô hình đang chọn | `problemDetail.statement.modelBadge` | - | - | Badge | Enum | - | - | O | `Bọc hàm` | - | Cho biết mã đang viết theo mô hình nào [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:172]<br>[Công thức] Bám **trạng thái màn** của Khu vực G NO 3, không phải cột DB — mô hình do người học chọn mỗi lượt (`DEC-2026-0824-dual-submission-model-per-problem`)<br>[EVT liên quan] EVT-5 |
| | 5 | Nội dung đề | `problemDetail.statement.body` | `problem.problems` | `statement_md` | Label | String | - | - | O | - | Markdown + LaTeX | Toàn văn đề bài [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:176-177]<br>[Nguồn giá trị] Cột `statement_md`, kết xuất Markdown + LaTeX (F2-01) [Nguồn: 02-bd/database/problem-bank.md:16]<br>[EVT liên quan] - |
| | 6 | Câu giải thích mô hình | `problemDetail.statement.modelNote` | - | - | Label | String | - | - | O | - | - | Câu nói rõ người học phải viết gì ở mô hình đang chọn [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:178]<br>[Nguồn giá trị] Hai nhãn tĩnh i18n, chọn theo Khu vực G NO 3. Ở mô hình Standard I/O, câu này kèm định dạng `stdin`/`stdout` từ `problem_specs.stdin_format_md`/`stdout_format_md` [Nguồn: 02-bd/database/problem-bank.md:121-122]<br>[EVT liên quan] EVT-5 |
| | 7 | Danh sách ví dụ | `problemDetail.statement.exampleList` | `problem.worked_examples` | `position`, `input`, `output`, `explanation_md` | List | List | - | - | O | rỗng | - | Các ví dụ mẫu hiển thị kèm đề [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:180-189]<br>[Nguồn giá trị] Các dòng `worked_examples` của bài, sắp theo `position`; **không đọc `testcases`** (cột `testcases.is_worked_example` và `testcases.explanation_md` đã bỏ, PB-08, `DEC-2026-1022-problem-bank-schema-sync` mục 4) [Nguồn: 02-bd/database/problem-bank.md:149-150,234-247]<br>[EVT liên quan] EVT-1 |
| | 8 | Dữ liệu vào của ví dụ | `problemDetail.statement.col.exampleInput` | `problem.worked_examples` | `input` | ListColumn | String | - | - | O | - | - | [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:184]<br>[Nguồn giá trị] Cột `input` (TEXT NOT NULL, văn bản ngắn, không có nhánh MinIO) [Nguồn: 02-bd/database/problem-bank.md:240]<br>[EVT liên quan] - |
| | 9 | Kết quả của ví dụ | `problemDetail.statement.col.exampleOutput` | `problem.worked_examples` | `output` | ListColumn | String | - | - | O | - | - | [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:185]<br>[Nguồn giá trị] Cột `output` (TEXT NOT NULL) [Nguồn: 02-bd/database/problem-bank.md:241]<br>[EVT liên quan] - |
| | 10 | Giải thích ví dụ | `problemDetail.statement.col.exampleNote` | `problem.worked_examples` | `explanation_md` | ListColumn | String | - | - | O | Ẩn | - | Câu giải thích vì sao ví dụ cho kết quả đó [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:186]<br>[Nguồn giá trị] Cột `worked_examples.explanation_md` (TEXT nullable, Markdown), nơi lưu đã đổi từ `testcases.explanation_md` sang bảng `worked_examples` ngày 2026-10-22 — xem Câu hỏi mở Q6 [Nguồn: 02-bd/database/problem-bank.md:242]<br>[EVT liên quan] - |
| | 11 | Danh sách ràng buộc | `problemDetail.statement.constraintList` | `problem.problems` | `constraints_md` | List | List | - | - | O | rỗng | - | Các ràng buộc về miền giá trị của bài [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:191-196]<br>[Nguồn giá trị] Cột riêng `problems.constraints_md` (TEXT NOT NULL default rỗng, Markdown); màn ghép `statement_md` rồi `constraints_md` thành một trang đề; chuỗi rỗng thì không vẽ khối (Câu hỏi mở Q5, đổi 2026-10-22, PB-03) [Nguồn: 02-bd/database/problem-bank.md:17]<br>[EVT liên quan] - |
| | 12 | Khối câu hỏi mở rộng | `problemDetail.statement.followUp` | `problem.problems` | `statement_md` | Label | String | - | - | O | Ẩn | - | Gợi mở hướng tối ưu, dẫn sang phân tích bài giải [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:198-201]<br>[Nguồn giá trị] Không có cột riêng: là mục Markdown quy ước (`## Câu hỏi mở rộng`) trong `statement_md` (Câu hỏi mở Q5, chốt 2026-10-18)<br>[EVT liên quan] - |

### Khu vực E — Tab Bài nộp

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Tab Bài nộp | | | | | | | | | | | | | |
| | 1 | Danh sách lượt nộp | `problemDetail.submissionHistory.list` | `judge.submissions` | `user_id`, `problem_id` | List | List | - | - | O | rỗng | - | 10 lượt nộp gần nhất của **chính người học** cho **chính bài này** [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:216-227]<br>[Nguồn giá trị] Kết quả gọi `ListMySubmissionsForProblem`, dùng index `submissions(user_id, problem_id, created_at DESC)` [Nguồn: 02-bd/database/judge-orchestration.md:197]<br>[EVT liên quan] EVT-1, EVT-22 |
| | 2 | Kết quả lượt nộp | `problemDetail.submissionHistory.col.verdict` | `judge.submissions` | `status`, `passed_testcase_count`, `total_testcase_count` | ListColumn | String | - | - | O | - | `{nhãn} {đạt}/{tổng}` | Trạng thái cuối của lượt nộp kèm tỉ lệ testcase đạt [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:220]<br>[Công thức] Nhãn tĩnh i18n map từ `status`; trạng thái khác `ACCEPTED`/`COMPILE_ERROR`/`SYSTEM_ERROR` thì ghép thêm `passed_testcase_count`/`total_testcase_count` (F4-13, `DEC-2026-0831-partial-score-testcase-ratio`) [Nguồn: 02-bd/database/judge-orchestration.md:22,25-26]<br>[EVT liên quan] - |
| | 3 | Ngôn ngữ | `problemDetail.submissionHistory.col.language` | `judge.submissions` | `language` | ListColumn | Enum | - | - | O | - | Nhãn cố định | [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:221]<br>[Nguồn giá trị] `JAVA` thành "Java", `CPP` thành "C++", `PYTHON` thành "Python" [Nguồn: 02-bd/database/judge-orchestration.md:18]<br>[EVT liên quan] - |
| | 4 | Thời gian và bộ nhớ | `problemDetail.submissionHistory.col.metrics` | `judge.submissions` | `runtime_ms`, `memory_kb` | ListColumn | String | - | - | O | `-` | `{số} ms · {số} MB` | Chỉ số của lượt nộp [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:222]<br>[Nguồn giá trị] Cột `runtime_ms`, `memory_kb`; cả hai `NULL` khi chưa chấm xong hoặc dừng ở `COMPILE_ERROR` thì hiển thị `-` [Nguồn: 02-bd/database/judge-orchestration.md:28-29]<br>[EVT liên quan] - |
| | 5 | Thời điểm nộp | `problemDetail.submissionHistory.col.submittedAt` | `judge.submissions` | `submitted_at` | ListColumn | Date | - | - | O | - | `dd/MM HH:mm` | [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:223]<br>[Nguồn giá trị] Cột `submitted_at` [Nguồn: 02-bd/database/judge-orchestration.md:31]<br>[EVT liên quan] - |

### Khu vực F — Tab Solution Review

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Tab Solution Review | | | | | | | | | | | | | |
| | 1 | Chỉ số độ phức tạp | `problemDetail.reviewSummary.metrics` | `ai.solution_reviews` | `result_json` | Label | String | - | - | O | Ẩn | - | Hai ô "Thời gian" và "Bộ nhớ" dạng ký hiệu Big-O [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:231-238]<br>[Nguồn giá trị] Trường độ phức tạp trong `result_json` của báo cáo đã có (F5-07) [Nguồn: 02-bd/database/ai-review.md:46,64-67]. Đây là **bản tóm tắt nhanh đọc lại từ báo cáo đã có**, không tạo dữ liệu mới [Nguồn: 01-rd/screens/users/USR0102_problem_detail.md:116]<br>[EVT liên quan] EVT-1 |
| | 2 | Khối nhận xét rút gọn | `problemDetail.reviewSummary.blocks` | `ai.solution_reviews` | `result_json` | List | List | - | - | O | rỗng | - | 1-2 nhận xét chính rút từ báo cáo [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:239-244]<br>[Nguồn giá trị] Cùng nguồn NO 1. Bản đầy đủ nằm ở màn `solution_review` [Nguồn: 01-rd/screens/users/USR0102_problem_detail.md:116]<br>[EVT liên quan] EVT-1 |
| | 3 | Liên kết mở báo cáo đầy đủ | `problemDetail.reviewSummary.linkFullReport` | - | - | Link | - | - | - | I | - | - | Mở màn `solution_review` của lượt nộp tương ứng<br>[Nguồn giá trị] Nhãn tĩnh i18n. Prototype không vẽ liên kết này nhưng RD đã chốt "mở từ nút trong tab hoặc từ console kết quả" [Nguồn: 01-rd/screens/users/USR0102_problem_detail.md:116]<br>[EVT liên quan] EVT-17 |
| | 4 | Nút bắt đầu phỏng vấn giả lập | `problemDetail.reviewSummary.btnMockInterview` | - | - | Button | - | - | - | I | - | - | Mở màn `mock_interview` với lối vào từ bài nộp [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:245]<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] EVT-18 |

### Khu vực G — Thanh công cụ soạn mã

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Thanh công cụ soạn mã | | | | | | | | | | | | | |
| | 1 | Nhãn khối soạn mã | `problemDetail.codeToolbar.label` | - | - | Label | String | - | - | O | `Code` | - | Nhãn nhận dạng khối [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:266]<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] - |
| | 2 | Chọn ngôn ngữ | `problemDetail.codeToolbar.languageTabs` | `judge.submissions` | `language` | List | Enum | - | Bắt buộc | I/O | `PYTHON` | - | Đúng **ba** ngôn ngữ Java / C++ / Python, không hơn [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:267-271,594]<br>[Nguồn giá trị] Enum khoá cứng `JAVA`/`CPP`/`PYTHON` [Nguồn: 02-bd/database/judge-orchestration.md:18]; giá trị đang chọn lấy từ tham số URL `lang`. Mặc định khi không có tham số là ngôn ngữ gần nhất đã nhớ ở trình duyệt (`localStorage` khoá `algoprep-workspace-prefs`), chưa có thì `PYTHON` — xem Câu hỏi mở Q9 (đã chốt 2026-10-18)<br>[EVT liên quan] EVT-4 |
| | 3 | Chọn mô hình nộp bài | `problemDetail.codeToolbar.modelTabs` | `judge.submissions` | `submission_model` | List | Enum | - | Bắt buộc | I/O | `FUNCTION_WRAPPER` | - | Hai nút "Có hàm main" (`STANDARD_IO`) và "Bọc hàm" (`FUNCTION_WRAPPER`) [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:272-276,596-601]<br>[Nguồn giá trị] Enum `submission_model` [Nguồn: 02-bd/database/judge-orchestration.md:19]; giá trị đang chọn lấy từ tham số URL `model`. Nút "Bọc hàm" bị ẩn khi `problems.function_wrapper_supported = false` — xem Sheet 6. Mặc định là mô hình gần nhất đã nhớ ở trình duyệt (khoá `algoprep-workspace-prefs`), bài chỉ có Standard I/O thì luôn `STANDARD_IO` — Câu hỏi mở Q9<br>[EVT liên quan] EVT-5 |
| | 4 | Chọn phương thức nhập mã | `problemDetail.codeToolbar.inputMethodTabs` | `judge.submissions` | `source_input_method` | List | Enum | - | Bắt buộc | I/O | `EDITOR` | - | Hai nút "Gõ trực tiếp" (`EDITOR`) và "Tải file" (`FILE_UPLOAD`), tách khỏi nhóm mô hình bằng một đường kẻ dọc [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:277-282,602-607]<br>[Nguồn giá trị] Enum `source_input_method` [Nguồn: 02-bd/database/judge-orchestration.md:21]. **Trục độc lập** với NO 3: đổi mô hình không đặt lại phương thức nhập [Nguồn: 01-rd/screens/users/USR0102_problem_detail.md:136]<br>[EVT liên quan] EVT-6 |
| | 5 | Thông tin vùng soạn mã | `problemDetail.codeToolbar.editorMeta` | - | - | Label | String | - | - | O | - | - | Ở chế độ gõ trực tiếp hiển thị mã hoá và vị trí con trỏ; ở chế độ tải file hiển thị tên file kỳ vọng và giới hạn kích thước [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:284,612]<br>[Công thức] Chuỗi ghép ở tầng giao diện từ trạng thái trình soạn thảo và từ ngôn ngữ đang chọn; phần mở rộng theo bảng `.java`/`.cpp`/`.py` [Nguồn: 01-rd/req/judge-orchestration.md:15]<br>[EVT liên quan] EVT-4, EVT-6, EVT-7 |

### Khu vực H — Vùng nhập mã

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Vùng nhập mã | | | | | | | | | | | | | |
| | 1 | Ô soạn mã | `problemDetail.codeInput.editor` | `judge.submissions` | `source_code` | TextArea | String | 65536 | Bắt buộc | I/O | Mã khung | - | Trình soạn thảo Monaco có số dòng và tô màu cú pháp [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:287-301]<br>[Nguồn giá trị] Giá trị ban đầu là **mã khung** do `harness` sinh theo cặp (ngôn ngữ, mô hình) đang chọn (F3-13) [Nguồn: 01-rd/req/harness.md:27-28]; người học sửa được. Cơ chế sinh mã khung thuộc `02-bd/architecture/harness.md`, không mô tả ở đây<br>[EVT liên quan] EVT-4, EVT-5, EVT-7 |
| | 2 | Vùng kéo thả file | `problemDetail.codeInput.dropZone` | - | - | Label | String | - | - | I | - | - | Vùng kéo-thả file mã nguồn kèm câu mô tả giới hạn [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:305-307]<br>[Nguồn giá trị] Nhãn tĩnh i18n ghép với phần mở rộng theo ngôn ngữ đang chọn và hằng số 64 KB [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:307]<br>[EVT liên quan] EVT-8 |
| | 3 | Nút Chọn file | `problemDetail.codeInput.btnChooseFile` | - | - | Button | - | - | - | I | - | - | Mở hộp thoại chọn file của hệ điều hành [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:308]<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] EVT-8 |
| | 4 | Thẻ file đã tải | `problemDetail.codeInput.uploadedFileCard` | - | - | Label | String | - | - | O | Ẩn | `{tên file} · {kích thước}` | Thông tin file vừa tải lên [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:310-314]<br>[Nguồn giá trị] Siêu dữ liệu của file do trình duyệt cung cấp; **không lưu file lên máy chủ** — nội dung nạp vào cùng trường `source_code` của luồng nộp bài F4-01 [Nguồn: 01-rd/req/judge-orchestration.md:13-14]<br>[EVT liên quan] EVT-8 |
| | 5 | Nút Xem nội dung | `problemDetail.codeInput.btnViewUploaded` | - | - | Button | - | - | - | I | - | - | Xem lại nội dung file đã tải trước khi nộp [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:315]<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] EVT-9 |
| | 6 | Trạng thái lưu nháp | `problemDetail.codeInput.draftStatus` | - | - | Label | String | - | - | O | Ẩn | - | Cho biết mã đang soạn đã được giữ lại ở trình duyệt này [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:321,531]<br>[Nguồn giá trị] Nhãn tĩnh i18n "Đã lưu trên trình duyệt này" (`toolbar.draftSaved`), **không** phải "Đã lưu nháp" chung chung. Bản nháp nằm ở `localStorage` theo khoá `(problem_id, language, submission_model)`, **không có bảng nào ở máy chủ** (Câu hỏi mở Q4, chốt 2026-10-18) [Nguồn: 05-coding/frontend/src/views/users/problem-detail/ui/problem-detail-view.tsx:147,156-166,595-597]<br>[EVT liên quan] EVT-7 |
| | 7 | Giới hạn thời gian và bộ nhớ | `problemDetail.codeInput.limits` | `problem.problems`, `judge.language_configs` | `time_limit_ms`, `memory_limit_mb`, `time_limit_multiplier`, `memory_limit_multiplier` | Label | String | - | - | O | `-` | `Time {số}s · Memory {số}MB` | Giới hạn **hiệu lực** cho ngôn ngữ đang chọn [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:323,530]<br>[Công thức] `time_limit_ms` nhân `time_limit_multiplier` của ngôn ngữ đang chọn; tương tự cho bộ nhớ [Nguồn: 02-bd/database/problem-bank.md:22-23; 02-bd/database/judge-orchestration.md:109-110]<br>[EVT liên quan] EVT-1, EVT-4 |

### Khu vực I — Bảng kết quả

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Bảng kết quả | | | | | | | | | | | | | |
| | 1 | Dải tab bảng kết quả | `problemDetail.console.tabs` | - | - | List | Enum | - | - | I/O | `Testcase` | - | Hai tab "Testcase" và "Kết quả" [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:328-332,620]<br>[Nguồn giá trị] Nhãn tĩnh i18n; trạng thái tab là trạng thái màn, không ghi vào URL<br>[EVT liên quan] EVT-13 |
| | 2 | Nhãn kết quả tổng hợp | `problemDetail.console.verdict` | `judge.submissions` | `status`, `passed_testcase_count`, `total_testcase_count` | Badge | String | - | - | O | Ẩn | `{nhãn} · {đạt}/{tổng}` | Kết quả của lượt chạy thử hoặc lượt nộp gần nhất [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:334,638]<br>[Công thức] Như Khu vực E NO 2. Trong lúc chấm, hiển thị nhãn trạng thái trung gian (`PENDING`/`COMPILING`/`JUDGING`) kèm số testcase đã chạy [Nguồn: 02-bd/database/judge-orchestration.md:22]<br>[EVT liên quan] EVT-10, EVT-11, EVT-12 |
| | 3 | Tổng kết lượt chạy và nút "Xem chi tiết" | `problemDetail.console.summaryPassed`, `summaryFailed`, `btnDetail` | `problem.testcases` | `visibility` | Label + Button | Number | - | - | O | - | `{n} đạt · {m} không đạt` | Bảng nhỏ chỉ hiện tổng số testcase đạt và không đạt của lượt chạy gần nhất (Run Code: testcase Sample; Submit: mọi testcase), thời gian, bộ nhớ và nút "Xem chi tiết" mở lại màn phụ (owner 2026-10-21; bỏ hàng nút Case vì nhiều testcase bị tràn) [Nguồn: 05-coding/frontend/src/views/users/problem-detail/ui/problem-detail-view.tsx]<br>[Nguồn giá trị] Số dòng `testcases` có `visibility = SAMPLE` [Nguồn: 02-bd/database/problem-bank.md:142]<br>[EVT liên quan] EVT-14 |
| | 4 | Chi tiết testcase mẫu (trong màn phụ) | `problemDetail.console.sampleCaseBody` | `problem.testcases` | `input_inline` | Label | String | - | - | O | - | - | Dữ liệu vào, kết quả thực tế, kết quả mong đợi của testcase Sample đang chọn trong màn phụ (mở sẵn testcase sai đầu tiên), tách theo từng tham số ở mô hình Bọc hàm [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:346-355]<br>[Nguồn giá trị] Cột `input_inline`; tên tham số lấy từ `problem_specs.parameters` (đã gộp từ `function_signatures`) [Nguồn: 02-bd/database/problem-bank.md:125]. **Chỉ Sample** — testcase ẩn không bao giờ hiển thị ở đây<br>[EVT liên quan] EVT-14 |
| | 5 | Dải ô trạng thái từng testcase | `problemDetail.console.caseDots` | `judge.submission_testcase_results` | `verdict`, `testcase_order` | List | List | - | - | O | rỗng | - | Mỗi testcase một ô, màu theo trạng thái [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:363-367]<br>[Nguồn giá trị] Cột `verdict` và `testcase_order` [Nguồn: 02-bd/database/judge-orchestration.md:55-56]. **Chỉ trạng thái và số thứ tự** — chú giải của ô không được chứa dữ liệu vào, kết quả mong đợi hay diff, kể cả với testcase ẩn (Sheet 9 NO 3)<br>[EVT liên quan] EVT-10, EVT-11, EVT-12 |
| | 6 | Chỉ số lượt chạy | `problemDetail.console.runMetrics` | `judge.submissions` | `runtime_ms`, `memory_kb` | List | List | - | - | O | `-` | - | Thời gian chạy và bộ nhớ lớn nhất trong các testcase đã chạy [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:368-375]<br>[Nguồn giá trị] Cột `runtime_ms`, `memory_kb` [Nguồn: 02-bd/database/judge-orchestration.md:28-29]. Ô "Beats" của prototype [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:645] **không** đưa vào màn này, xem Câu hỏi mở Q11<br>[EVT liên quan] EVT-10, EVT-11, EVT-12 |
| | 7 | Khối kết quả dạng văn bản | `problemDetail.console.output` | `judge.submissions` | `compile_error_message` | Label | String | - | - | O | - | - | Với lượt chạy thử: bảng đối chiếu output thực tế và kết quả mong đợi của từng testcase Sample. Với lượt nộp: câu tổng kết và thông báo lỗi biên dịch nếu có [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:376,647-649]<br>[Nguồn giá trị] Cột `compile_error_message` **đã qua bước ánh xạ dòng của `harness`** (F3-11) [Nguồn: 02-bd/database/judge-orchestration.md:24]; phần thuộc mã khung bị lọc bỏ hoàn toàn (F3-12), xem Sheet 9 NO 15<br>[EVT liên quan] EVT-10, EVT-11 |
| | 8 | Liên kết Xem kết quả chấm | `problemDetail.console.linkFullResult` | - | - | Link | - | - | - | I | - | - | Sang màn `submission_result` của lượt nộp vừa xong [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:379]<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] EVT-16 |
| | 9 | Liên kết Phân tích bài giải | `problemDetail.console.linkSolutionReview` | - | - | Link | - | - | - | I | - | - | Sang màn `solution_review` (F5-01) [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:380]<br>[Nguồn giá trị] Nhãn tĩnh i18n. Hành động **tự chọn**, hệ thống không tự chạy (`DEC-2026-0831-outside-screens-closures`)<br>[EVT liên quan] EVT-17 |
| | 10 | Liên kết Phỏng vấn giả lập | `problemDetail.console.linkMockInterview` | - | - | Link | - | - | - | I | - | - | Sang màn `mock_interview` (F5-09) [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:381]<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] EVT-18 |
| | 11 | Hướng dẫn khi chưa chạy | `problemDetail.console.emptyHint` | - | - | Label | String | - | - | O | - | - | Câu hướng dẫn khi chưa có lượt chạy nào [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:386-388]<br>[Nguồn giá trị] Nhãn tĩnh i18n, chèn số testcase Sample và số testcase ẩn. **Số testcase ẩn là con số duy nhất được tiết lộ**, không kèm bất kỳ nội dung nào<br>[EVT liên quan] EVT-1 |

[Nguồn: 09-layoutBase/Workspace giải bài.dc.html:99-107,112-152,159-163,166-202,216-227,229-247,250-255,
258-260,265-285,287-324,327-392; 02-bd/database/problem-bank.md:8-37,57-76,108-129,136-155,189-208,234-247;
02-bd/database/judge-orchestration.md:12-34,50-61,101-112; 02-bd/database/ai-review.md:35-50]

---

## Sheet 6. Đặc tả điều khiển item

> Ký hiệu cột `Hiển thị` và thứ tự thẻ: `02-bd/_rules/bd-template-9sheet.md` mục 2, 3. Dùng đúng NO, tên
> item và thứ tự của Sheet 5. Lỗi nhập liệu ghi ở Sheet 9, xử lý nghiệp vụ ghi ở Sheet 8.

### Khu vực A — Thanh tác vụ bài toán

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Thanh tác vụ bài toán | | | | |
| | 1 | Liên kết Danh sách bài | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị khi đã chọn bài — cả thanh tác vụ nằm trong nhánh `hasProblem` [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:97]. |
| | 2 | Mã và tên bài | Điều kiện | [Điều kiện hiển thị] Như NO 1. Trong lúc tải hiển thị khung chờ một dòng, không để trống. |
| | 3 | Độ khó | Điều kiện | [Điều kiện hiển thị] Như NO 1. |
| | 4 | Nút Run Code | Điều kiện | [Điều kiện hiển thị] Như NO 1.<br>[Điều kiện kích hoạt] Không kích hoạt khi mã rỗng, khi đang có lượt chạy thử chưa xong, hoặc khi đang có lượt nộp chưa vào trạng thái cuối. |
| | 5 | Nút Submit | Điều kiện | [Điều kiện hiển thị] Như NO 1.<br>[Điều kiện kích hoạt] Không kích hoạt khi mã rỗng, khi đã vượt giới hạn số lượt nộp mỗi giờ (Sheet 9 NO 10), hoặc khi lượt nộp trước chưa vào trạng thái cuối. |

### Khu vực B — Trạng thái chưa chọn bài

**Đã bỏ (owner chốt 2026-10-19).** Màn không còn đường vào trạng thái chưa chọn bài: đường dẫn `/workspace/{problemId}` luôn mang id (từ 2026-10-20 mục nav Workspace mở `USR0105`, không còn chuyển hướng, Q12). Id không mở được thì hiện "Không tìm thấy bài toán." (Sheet 9 NO 2). Bốn item cũ (`problemDetail.emptyState.*`) và widget `workspace-empty-state` bị gỡ; giữ tiêu đề mục để không lệch tham chiếu.

### Khu vực C — Panel thông tin bài toán

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Panel thông tin bài toán | | | | |
| | 1 | Dải tab panel | Điều kiện | [Điều kiện hiển thị] Chỉ khi đã chọn bài. Tab "Solution Review" **ẩn** khi bài chưa từng đạt `ACCEPTED` hoặc chưa có báo cáo, và khi gọi `ai-review` thất bại — ẩn im lặng, không báo lỗi.<br>[Tự động đặt] Mở màn thì chọn tab theo tham số URL `tab`; giá trị lạ thì về "Đề bài". |
| | 2 | AC rate | Điều kiện | [Điều kiện hiển thị] Chưa có dòng read model thì hiển thị `-`; gọi thất bại thì ẩn riêng chỉ số này, không chặn chân panel. |
| | 3 | Nút Lưu bài | Điều kiện | [Điều kiện hiển thị] Chỉ khi đã chọn bài.<br>[Điều kiện kích hoạt] Không kích hoạt trong lúc đang gửi yêu cầu bật/tắt.<br>[Tự động đặt] Nhãn và trạng thái nổi đổi ngay khi bấm (cập nhật lạc quan), hoàn lại nguyên trạng nếu máy chủ trả lỗi. |
| | 4 | Trạng thái cụm chấm | Điều kiện | [Điều kiện hiển thị] Ẩn khi chưa có dữ liệu sẵn (chốt Câu hỏi mở Q8, 2026-10-18: không gọi định kỳ), và ẩn khi lời gọi lấy trạng thái thất bại. Không hiển thị "sẵn sàng" khi không biết chắc. |
| | 5 | Thanh chia đôi bố cục | Điều kiện | [Điều kiện hiển thị] Ẩn ở bề rộng màn hẹp khi hai panel xếp chồng thay vì cạnh nhau `[SoT: Suy luận]` — prototype không định nghĩa điểm ngắt, xem `02-bd/screens/users/_shell.md` mục 6 Q5.<br>[Tự động đặt] Giá trị khôi phục từ lần dùng trước ở phía trình duyệt. |

### Khu vực D — Tab Đề bài

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Tab Đề bài | | | | |
| | 1 | Tên bài | Điều kiện | [Điều kiện hiển thị] Chỉ khi tab đang chọn là "Đề bài". |
| | 2 | Nhãn độ khó | Điều kiện | [Điều kiện hiển thị] Như NO 1. |
| | 3 | Nhãn chủ đề | Điều kiện | [Điều kiện hiển thị] Như NO 1; bài không gắn chủ đề thì không vẽ nhãn nào. |
| | 4 | Nhãn mô hình đang chọn | Điều kiện | [Điều kiện hiển thị] Như NO 1.<br>[Tự động đặt] Đổi ngay khi đổi mô hình ở Khu vực G NO 3, không cần tải lại đề. |
| | 5 | Nội dung đề | Điều kiện | [Điều kiện hiển thị] Như NO 1. Trong lúc tải hiển thị khung chờ dạng đoạn văn. |
| | 6 | Câu giải thích mô hình | Điều kiện | [Điều kiện hiển thị] Như NO 1.<br>[Tự động đặt] Đổi nội dung theo mô hình đang chọn: mô hình Bọc hàm hiển thị câu "chỉ viết thân hàm"; mô hình Có hàm main hiển thị câu kèm định dạng `stdin`/`stdout`. |
| | 7 | Danh sách ví dụ | Điều kiện | [Điều kiện hiển thị] Như NO 1; bài không có dòng `worked_examples` nào thì ẩn cả khối, không để tiêu đề trống. |
| | 8 | Dữ liệu vào của ví dụ | Có | - |
| | 9 | Kết quả của ví dụ | Có | - |
| | 10 | Giải thích ví dụ | Điều kiện | [Điều kiện hiển thị] Chỉ hiện khi `worked_examples.explanation_md` của ví dụ có giá trị (Câu hỏi mở Q6, đã chốt 2026-10-18, nơi lưu đổi 2026-10-22); rỗng thì ẩn. Không tự bịa câu giải thích. |
| | 11 | Danh sách ràng buộc | Điều kiện | [Điều kiện hiển thị] Như NO 1; `constraints_md` đi cùng phản hồi `GetProblemForWorkspace` nên không có trạng thái tải riêng; chuỗi rỗng thì không vẽ khối. |
| | 12 | Khối câu hỏi mở rộng | Điều kiện | [Điều kiện hiển thị] Như NO 11; đề không có mục này thì không vẽ khối. |

### Khu vực E — Tab Bài nộp

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Tab Bài nộp | | | | |
| | 1 | Danh sách lượt nộp | Điều kiện | [Điều kiện hiển thị] Chỉ khi tab đang chọn là "Bài nộp". Trong lúc tải hiển thị khung chờ 4 dòng; chưa nộp lần nào thì hiển thị một câu trống kèm hướng dẫn bấm Submit.<br>[Tự động đặt] Nạp lại sau mỗi lượt nộp vào trạng thái cuối (EVT-12), không cần người dùng làm mới. |
| | 2 | Kết quả lượt nộp | Có | [Điều kiện hiển thị] Lượt nộp còn đang chấm (`PENDING`, `COMPILING`, `JUDGING`) thì hiển thị nhãn "Đang chấm" thay cho tỉ lệ testcase — cùng quy ước đã dùng ở `problem_list` [Nguồn: 02-bd/screens/users/USR0101_problem_list.md Sheet 6 Khu vực F, item Kết quả lần nộp gần nhất]. |
| | 3 | Ngôn ngữ | Có | - |
| | 4 | Thời gian và bộ nhớ | Có | [Điều kiện hiển thị] `NULL` thì hiển thị `-`, không hiển thị `0 ms`. |
| | 5 | Thời điểm nộp | Có | - |

### Khu vực F — Tab Solution Review

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Tab Solution Review | | | | |
| | 1 | Chỉ số độ phức tạp | Điều kiện | [Điều kiện hiển thị] Chỉ khi tab đang chọn là "Solution Review" **và** đã có báo cáo cho một lượt nộp `ACCEPTED` của chính người học. Gọi `ai-review` thất bại thì ẩn cả tab (Khu vực C NO 1), không hiển thị thông báo lỗi. |
| | 2 | Khối nhận xét rút gọn | Điều kiện | [Điều kiện hiển thị] Như NO 1. Giới hạn 2 khối để giữ vai trò "tóm tắt nhanh"; bản đầy đủ ở màn `solution_review`. |
| | 3 | Liên kết mở báo cáo đầy đủ | Điều kiện | [Điều kiện hiển thị] Như NO 1. |
| | 4 | Nút bắt đầu phỏng vấn giả lập | Điều kiện | [Điều kiện hiển thị] Như NO 1.<br>[Điều kiện kích hoạt] Không kích hoạt khi phân hệ AI đang hết hạn mức; trạng thái này do `ai-review` trả về, màn chỉ hiển thị. |

### Khu vực G — Thanh công cụ soạn mã

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Thanh công cụ soạn mã | | | | |
| | 1 | Nhãn khối soạn mã | Điều kiện | [Điều kiện hiển thị] Chỉ khi đã chọn bài. |
| | 2 | Chọn ngôn ngữ | Điều kiện | [Điều kiện hiển thị] Đúng ba nút, luôn đủ ba — không ngôn ngữ nào bị ẩn theo bài.<br>[Điều kiện kích hoạt] Không kích hoạt trong lúc đang chấm một lượt nộp.<br>[Tự động đặt] Đổi ngôn ngữ thì mã trong ô soạn mã được thay bằng mã khung mới; không hỏi xác nhận vì mã của cặp cũ vẫn nằm ở bản nháp theo khoá `(problem_id, language, submission_model)` (Câu hỏi mở Q14). |
| | 3 | Chọn mô hình nộp bài | Điều kiện | [Điều kiện hiển thị] Nút "Bọc hàm" **ẩn hẳn** khi `problems.function_wrapper_supported = false` — trường hợp kiểu dữ liệu của bài vượt lược đồ kiểu của F3, chỉ còn Standard I/O (F3-13, phương án xử lý rủi ro R2) [Nguồn: 01-rd/req/harness.md:30-34]. Khi đó không vẽ nhóm hai nút mà hiển thị một nhãn tĩnh cho biết bài chỉ hỗ trợ Standard I/O.<br>[Điều kiện kích hoạt] Như NO 2.<br>[Tự động đặt] Như NO 2 — đổi mô hình cũng sinh lại mã khung. |
| | 4 | Chọn phương thức nhập mã | Điều kiện | [Điều kiện hiển thị] Chỉ khi đã chọn bài.<br>[Tự động đặt] Đổi từ "Tải file" về "Gõ trực tiếp" thì nạp lại mã khung theo cặp (ngôn ngữ, mô hình) đang chọn [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:606]; đổi theo chiều ngược lại **không** xoá mã đã gõ. Đổi mô hình hoặc ngôn ngữ **không** đặt lại giá trị của mục này [Nguồn: 01-rd/screens/users/USR0102_problem_detail.md:136]. |
| | 5 | Thông tin vùng soạn mã | Có | [Tự động đặt] Đổi theo phương thức nhập và theo ngôn ngữ đang chọn. |

### Khu vực H — Vùng nhập mã

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Vùng nhập mã | | | | |
| | 1 | Ô soạn mã | Điều kiện | [Điều kiện hiển thị] Chỉ khi phương thức nhập là "Gõ trực tiếp" [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:608].<br>[Điều kiện kích hoạt] Chỉ đọc trong lúc đang chấm một lượt nộp; vẫn cuộn và bôi đen để sao chép được. |
| | 2 | Vùng kéo thả file | Điều kiện | [Điều kiện hiển thị] Chỉ khi phương thức nhập là "Tải file" [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:609]. |
| | 3 | Nút Chọn file | Điều kiện | [Điều kiện hiển thị] Như NO 2. |
| | 4 | Thẻ file đã tải | Điều kiện | [Điều kiện hiển thị] Chỉ sau khi đã tải lên thành công một file hợp lệ. Lỗi từ chối file hiển thị **ngay trong vùng tải**, không dùng hộp thoại [Nguồn: 01-rd/screens/users/USR0102_problem_detail.md:118]. |
| | 5 | Nút Xem nội dung | Điều kiện | [Điều kiện hiển thị] Đi kèm NO 4. |
| | 6 | Trạng thái lưu nháp | Điều kiện | [Điều kiện hiển thị] Chỉ hiện khi cặp (ngôn ngữ, mô hình) đang chọn đã có mã do người học nhập hoặc đã khôi phục từ bản nháp; chưa có thì ẩn [Nguồn: 05-coding/frontend/src/views/users/problem-detail/ui/problem-detail-view.tsx:595-597]. Nhãn phải nói đúng nơi lưu ("trên trình duyệt này"), không hứa lưu ở máy chủ.<br>[Tự động đặt] Trình duyệt chặn hoặc đầy `localStorage` thì vẫn soạn được, mã chỉ giữ trong phiên (dòng 118-125). |
| | 7 | Giới hạn thời gian và bộ nhớ | Điều kiện | [Điều kiện hiển thị] Không lấy được hệ số ngôn ngữ thì hiển thị giới hạn cơ sở của bài kèm chú thích là giá trị chưa nhân hệ số, thay vì ẩn hẳn.<br>[Tự động đặt] Tính lại khi đổi ngôn ngữ. |

### Khu vực I — Bảng kết quả

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Bảng kết quả | | | | |
| | 1 | Dải tab bảng kết quả | Điều kiện | [Điều kiện hiển thị] Chỉ khi đã chọn bài.<br>[Tự động đặt] Tự chuyển tab theo thao tác (Run Code sang "Testcase", Submit sang "Kết quả") và mở thêm màn phụ liệt kê từng testcase ngay khi bấm (owner 2026-10-21) [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:640-641; 05-coding/frontend/src/views/users/problem-detail/ui/problem-detail-view.tsx:289-313,315-340]. |
| | 2 | Nhãn kết quả tổng hợp | Điều kiện | [Điều kiện hiển thị] Ẩn khi chưa có lượt chạy nào trong phiên.<br>[Tự động đặt] Cập nhật theo từng thông điệp thời gian thực (EVT-12), không chờ tới khi chấm xong. |
| | 3 | Tab chọn testcase mẫu | Điều kiện | [Điều kiện hiển thị] Chỉ khi tab đang chọn là "Testcase". Bài không có testcase Sample nào thì hiển thị một câu trống thay cho dải nút. |
| | 4 | Nội dung testcase mẫu | Điều kiện | [Điều kiện hiển thị] Như NO 3. |
| | 5 | Dải ô trạng thái từng testcase | Điều kiện | [Điều kiện hiển thị] Chỉ khi tab đang chọn là "Kết quả" và đã có ít nhất một testcase chạy xong.<br>[Tự động đặt] Ô được tô dần theo thứ tự testcase khi nhận cập nhật thời gian thực; testcase chưa chạy giữ trạng thái chờ. **Không** dừng ở testcase hỏng đầu tiên — mọi testcase đều chạy (`DEC-2026-0831-partial-score-testcase-ratio`). |
| | 6 | Chỉ số lượt chạy | Điều kiện | [Điều kiện hiển thị] Chỉ khi lượt nộp đã vào trạng thái cuối và có `runtime_ms`; dừng ở `COMPILE_ERROR` hoặc `SYSTEM_ERROR` thì ẩn cả khối. |
| | 7 | Khối kết quả dạng văn bản | Điều kiện | [Điều kiện hiển thị] Chỉ khi tab đang chọn là "Kết quả" và đã có lượt chạy.<br>[Tự động đặt] Với lượt chạy thử hiển thị đối chiếu từng testcase Sample; với lượt nộp **chỉ** hiển thị câu tổng kết và thông báo lỗi biên dịch — không có dòng nào dẫn tới nội dung testcase ẩn. |
| | 8 | Liên kết Xem kết quả chấm | Điều kiện | [Điều kiện hiển thị] Chỉ sau một lượt **nộp** đã vào trạng thái cuối. Hiển thị với mọi trạng thái cuối, không riêng `ACCEPTED` — người học cần xem chi tiết nhất là khi trượt. |
| | 9 | Liên kết Phân tích bài giải | Điều kiện | [Điều kiện hiển thị] Chỉ khi lượt nộp đạt `ACCEPTED` [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:377], **và** phân hệ AI khả dụng. AI chết thì ẩn liên kết, không báo lỗi (Sheet 9 NO 13). |
| | 10 | Liên kết Phỏng vấn giả lập | Điều kiện | [Điều kiện hiển thị] Như NO 9. |
| | 11 | Hướng dẫn khi chưa chạy | Điều kiện | [Điều kiện hiển thị] Chỉ khi tab đang chọn là "Kết quả" và chưa có lượt chạy nào trong phiên [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:386]. |

[Nguồn: 09-layoutBase/Workspace giải bài.dc.html:97,112,377,386,606,608,609,640-641;
02-bd/database/problem-bank.md:21; 01-rd/req/harness.md:30-34;
01-rd/screens/users/USR0102_problem_detail.md:118,129]

---

## Sheet 7. Danh sách truyền dữ liệu

### 7.1 Trường DTO

| NO | DTO | Trường DTO | Kiểu | Bảng DB | Cột DB | Item màn | Hiển thị | Ghi chú |
| --: | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :--- |
| 1 | `ProblemWorkspaceDto` | `id` | UUID | `problem.problems` | `id` | - | Không | [Nguồn] Phản hồi của `GetProblemForWorkspace`<br>[Đích] Tham số của mọi lời gọi còn lại của màn. |
| 2 | `ProblemWorkspaceDto` | `code`, `title` | String | `problem.problems` | `code`, `title` | Thanh tác vụ, tab Đề bài | Có | - |
| 3 | `ProblemWorkspaceDto` | `levelCode`, `levelDisplayName` | String, String | `problem.problem_levels` | `code`, `display_name` | Thanh tác vụ, tab Đề bài | Có | [Nguồn] Join qua `problems.level_id` (thay trường `difficulty` kiểu Enum cũ, theo `DEC-2026-1001-admin-configurable-settings` mục (7))<br>[Chuyển đổi] Đọc `display_name` từ dữ liệu, bỏ ánh xạ "EASY/MEDIUM/HARD thành Dễ/Trung bình/Khó". |
| 4 | `ProblemWorkspaceDto` | `topicNames` | List\<String\> | `problem.topics` | `name` | Tab Đề bài — nhãn chủ đề | Có | [Nguồn] `topics.name` qua `problem_topics.problem_id`<br>[Chuyển đổi] Mỗi chủ đề một nhãn, **không** nối bằng dấu phẩy như bảng của `problem_list`. |
| 5 | `ProblemWorkspaceDto` | `statementMd` | String | `problem.problems` | `statement_md` | Tab Đề bài — nội dung đề | Có | [Chuyển đổi] Kết xuất Markdown + LaTeX ở phía giao diện; máy chủ trả nguyên văn. |
| 6 | `ProblemWorkspaceDto` | `functionWrapperSupported` | Boolean | `problem.problems` | `function_wrapper_supported` | Thanh công cụ — nhóm nút mô hình | Có | [Chuyển đổi] `false` thì **ẩn** nút "Bọc hàm", chỉ còn Standard I/O (F3-13). |
| 7 | `ProblemWorkspaceDto` | `stdinFormatMd`, `stdoutFormatMd` | String | `problem.problem_specs` | `stdin_format_md`, `stdout_format_md` | Tab Đề bài — câu giải thích mô hình | Điều kiện | [Chuyển đổi] Chỉ hiển thị khi mô hình đang chọn là Standard I/O. |
| 8 | `ProblemWorkspaceDto` | `acRate` | Number | `problem.problem_stats` | `ac_rate` | Chân panel — AC rate | Có | [Nguồn] Read model<br>[Chuyển đổi] Không có dòng read model thì trả `null`, màn hiển thị `-`. |
| 9 | `ProblemWorkspaceDto` | `bookmarked` | Boolean | `problem.bookmarks` | `user_id`, `problem_id` | Chân panel — nút Lưu bài | Có | [Nguồn] Tính theo người dùng đăng nhập; endpoint **không** nhận `user_id`. |
| 10 | `StarterCodeDto` | `language`, `submissionModel`, `sourceCode` | Enum, Enum, String | - | - | Ô soạn mã | Có | [Nguồn] Phản hồi của `GetStarterCode`<br>[Chuyển đổi] Sinh từ `problem_specs` (gồm chữ ký hàm: `function_name`, `return_type`, `parameters`, `name_overrides`) bởi `harness`; thuật toán sinh mã thuộc `02-bd/architecture/harness.md`, không mô tả ở đây. |
| 11 | `SampleTestcaseDto` | `order`, `input`, `expectedOutput` | Number, String, String | `problem.testcases` | `testcase_order` suy từ thứ tự, `input_inline`, `expected_output_inline` | Tab Testcase | Có | [Nguồn] Phản hồi của `ListSampleTestcases` — **chỉ** dòng `visibility = SAMPLE`.<br>[Chuyển đổi] Ở mô hình Bọc hàm, tách input theo tên tham số trong `problem_specs.parameters`. |
| 12 | `ExecutionLimitsDto` | `effectiveTimeLimitMs`, `effectiveMemoryLimitMb` | Number, Number | `problem.problems`, `judge.language_configs` | `time_limit_ms`, `memory_limit_mb`, `time_limit_multiplier`, `memory_limit_multiplier` | Chân vùng soạn mã — giới hạn | Có | [Nguồn] Phản hồi của `GetEffectiveExecutionLimits`<br>[Chuyển đổi] Nhân hệ số theo ngôn ngữ ở phía máy chủ; màn không tự nhân để tránh hai nơi tính lệch nhau. |
| 13 | `MySubmissionSummaryDto` | `submissionId` | UUID | `judge.submissions` | `id` | - | Không | [Đích] Tham số điều hướng sang `submission_result`, `solution_review`, `mock_interview`. |
| 14 | `MySubmissionSummaryDto` | `status`, `passedTestcaseCount`, `totalTestcaseCount` | Enum, Number, Number | `judge.submissions` | `status`, `passed_testcase_count`, `total_testcase_count` | Tab Bài nộp — kết quả | Có | [Chuyển đổi] Ghép chuỗi hiển thị ở tầng giao diện; DTO trả số thô, không trả câu đã ghép — cùng quy ước với `problem_list` [Nguồn: 02-bd/screens/users/USR0101_problem_list.md Sheet 7.1, DTO `InProgressProblemDto`]. |
| 15 | `MySubmissionSummaryDto` | `language`, `runtimeMs`, `memoryKb`, `submittedAt` | Enum, Number, Number, Date | `judge.submissions` | `language`, `runtime_ms`, `memory_kb`, `submitted_at` | Tab Bài nộp — các cột còn lại | Có | - |
| 16 | `SubmissionTicketDto` | `submissionId`, `status` | UUID, Enum | `judge.submissions` | `id`, `status` | Nhãn kết quả tổng hợp | Có | [Nguồn] Phản hồi của `CreateSubmission`, trả **ngay** với `status = PENDING`, không chờ chấm (F4-01).<br>[Đích] `submissionId` dùng làm khoá kênh thời gian thực. |
| 17 | `SubmissionProgressDto` | `submissionId`, `status`, `testcaseOrder`, `testcaseVerdict`, `passedCount`, `totalCount` | UUID, Enum, Number, Enum, Number, Number | `judge.submissions`, `judge.submission_testcase_results` | `status`, `testcase_order`, `verdict`, `passed_testcase_count`, `total_testcase_count` | Dải ô trạng thái, nhãn kết quả | Có | [Nguồn] Thông điệp đẩy trên kênh thời gian thực theo `submissionId` (F4-08).<br>[Chuyển đổi] **Không** chứa dữ liệu vào, kết quả mong đợi, output thực tế hay diff của testcase ẩn — chỉ thứ tự và trạng thái (Sheet 9 NO 3). |
| 18 | `SubmissionProgressDto` | `compileErrorMessage` | String | `judge.submissions` | `compile_error_message` | Khối kết quả dạng văn bản | Điều kiện | [Chuyển đổi] Chỉ có giá trị khi `status = COMPILE_ERROR`, và **đã qua** bước ánh xạ dòng của `harness` (F3-11), phần thuộc mã khung đã bị lọc bỏ (F3-12). |
| 19 | `SampleRunResultDto` | `cases` | List | `problem.testcases` | - | Khối kết quả dạng văn bản, dải ô trạng thái | Có | [Nguồn] Phản hồi của `RunSampleTestcases`<br>[Chuyển đổi] Mỗi phần tử gồm `order`, `verdict`, `actualOutput`, `expectedOutput`, `runtimeMs`. Được phép trả kết quả mong đợi vì **toàn bộ là testcase Sample công khai**. |
| 20 | `SampleRunResultDto` | `compileErrorMessage` | String | - | - | Khối kết quả dạng văn bản | Điều kiện | [Chuyển đổi] Như NO 18. Lượt chạy thử **không** tạo dòng `submissions`, xem Câu hỏi mở Q10. |
| 21 | `SolutionReviewQuickInsightDto` | `submissionId`, `timeComplexity`, `spaceComplexity`, `highlights` | UUID, String, String, List\<String\> | `ai.solution_reviews` | `submission_id`, `result_json` | Tab Solution Review | Điều kiện | [Nguồn] Lời gọi **riêng** `GetMySolutionReviewQuickInsight` sang `ai-review`, không gộp vào lời gọi đề bài — để màn vẫn chạy đủ khi F5 chết (`CLAUDE.md` — AI suy giảm nhẹ nhàng), cùng nguyên tắc đã áp ở `problem_list` [Nguồn: 02-bd/screens/users/USR0101_problem_list.md Sheet 6 Khu vực D, item Nhãn đã có Solution Review]. |
| 22 | `ProblemWorkspaceDto` | `constraintsMd` | String | `problem.problems` | `constraints_md` | Tab Đề bài — danh sách ràng buộc | Có | [Nguồn] Phản hồi của `GetProblemForWorkspace`<br>[Chuyển đổi] Kết xuất Markdown ở phía giao diện; chuỗi rỗng thì không vẽ khối. Thêm 2026-10-22 (PB-03). |
| 23 | `WorkedExampleDto` | `order`, `input`, `output`, `explanationMd` | Number, String, String, String | `problem.worked_examples` | `position`, `input`, `output`, `explanation_md` | Tab Đề bài — danh sách ví dụ | Có | [Nguồn] trả cùng `GetProblemForWorkspace`, một lần tải tab Đề bài (đã chốt, Câu hỏi mở Q18)<br>[Chuyển đổi] `explanationMd` rỗng thì ẩn dòng giải thích; danh sách rỗng thì ẩn cả khối. Thêm 2026-10-22 (PB-08). |

### 7.2 Truy cập bảng dữ liệu (13)

| NO | Tên logic | Bảng | Repository | CRUD | Mục đích | Ghi chú |
| --: | :--- | :--- | :--- | :-: | :--- | :--- |
| 1 | Bài toán | `problem.problems` | `ProblemRepository` | R | Đọc đề bài, `level_id` của độ khó, giới hạn cơ sở, cờ mô hình | `GetProblemForWorkspace`: R |
| 2 | Chủ đề | `problem.topics` | `TopicRepository` | R | Nhãn chủ đề trên tab Đề bài | `GetProblemForWorkspace`: R |
| 3 | Gán chủ đề cho bài | `problem.problem_topics` | `ProblemTopicRepository` | R | Nối bài với chủ đề | `GetProblemForWorkspace`: R |
| 4 | Đặc tả bài toán | `problem.problem_specs` | `ProblemSpecRepository` | R | Định dạng `stdin`/`stdout` cho mô hình Standard I/O; chữ ký hàm (đã gộp từ `function_signatures`) làm đầu vào sinh mã khung và tách tên tham số của testcase mẫu | `GetProblemForWorkspace`: R<br>`GetStarterCode`: R |
| 5 | Ví dụ mẫu | `problem.worked_examples` | `WorkedExampleRepository` `[Suy luận]` tên do BD đề xuất | R | Đọc ví dụ mẫu kèm lời giải thích cho tab Đề bài | `GetProblemForWorkspace`: R, ví dụ mẫu trả cùng lần tải đề (đã chốt, xem Câu hỏi mở Q18) |
| 6 | Testcase | `problem.testcases` | `TestcaseRepository` | R | Đọc **chỉ** testcase `visibility = SAMPLE` cho tab Testcase và lượt chạy thử (ví dụ trong đề lấy từ `worked_examples`, không từ đây) | `ListSampleTestcases`: R<br>`RunSampleTestcases`: R |
| 7 | Thống kê bài toán | `problem.problem_stats` | `ProblemStatsRepository` | R | Đọc `ac_rate` cho chân panel | `GetProblemForWorkspace`: R |
| 8 | Bài đã lưu | `problem.bookmarks` | `BookmarkRepository` | C, R, D | Đọc trạng thái đã lưu; tạo dòng khi lưu, xoá dòng khi bỏ lưu | `GetProblemForWorkspace`: R<br>`ToggleMyProblemBookmark`: C, D |
| 9 | Bài nộp | `judge.submissions` | `SubmissionRepository` (module `judge-orchestration`) | C, R | Tạo lượt nộp mới; đọc lịch sử nộp bài của chính người học cho bài này | `CreateSubmission`: C<br>`ListMySubmissionsForProblem`: R<br>`ListMyUnsolvedRecentProblems`: R |
| 10 | Kết quả từng testcase | `judge.submission_testcase_results` | `SubmissionTestcaseResultRepository` (module `judge-orchestration`) | R | Dựng dải ô trạng thái theo thứ tự testcase — **chỉ** đọc `testcase_order` và `verdict` | `SubscribeSubmissionProgress`: R |
| 11 | Cấu hình ngôn ngữ | `judge.language_configs` | `LanguageConfigRepository` (module `judge-orchestration`) | R | Hệ số nhân giới hạn thời gian và bộ nhớ theo ngôn ngữ | `GetEffectiveExecutionLimits`: R |
| 12 | Báo cáo phân tích bài giải | `ai.solution_reviews` | `SolutionReviewRepository` (module `ai-review`) | R | Lấy tóm tắt nhanh cho tab thứ ba | `GetMySolutionReviewQuickInsight`: R |
| 13 | Danh mục độ khó | `problem.problem_levels` | `ProblemLevelRepository` | R | Đọc nhãn độ khó của bài qua join `level_id` | `GetProblemForWorkspace`: R, qua join. Màn này không ghi; ADMIN quản lý độ khó ở `SHR0201` [Nguồn: 02-bd/database/problem-bank.md mục `problem_levels`] |

Màn có đúng **hai** loại thao tác ghi: tạo một dòng `submissions` khi nộp bài, và tạo/xoá một dòng
`bookmarks` khi bật/tắt lưu bài. Mọi bảng còn lại chỉ đọc. Bốn bảng cuối thuộc module khác, truy cập qua
endpoint của module sở hữu chứ không qua repository của `problem-bank`.

`[Suy luận]` — tên repository do BD này đề xuất, DD module chốt lại.

### 7.3 Danh sách endpoint [Nội bộ]

> Chỉ tên nghiệp vụ và BC sở hữu. Hợp đồng chi tiết thuộc `03-dd/api/<module>.md`.

| NO | Endpoint (tên nghiệp vụ) | Mục đích | BC sở hữu |
| --: | :--- | :--- | :--- |
| 1 | `GetProblemForWorkspace` | Tải đề bài, độ khó, chủ đề, giới hạn cơ sở, cờ mô hình, AC rate và trạng thái đã lưu của một bài đã xuất bản | `problem-bank` |
| 2 | `ListSampleTestcases` | Tải toàn bộ testcase `SAMPLE` của bài để hiển thị tab Testcase (ví dụ trong đề đọc từ `worked_examples`, Câu hỏi mở Q18) | `problem-bank` |
| 3 | `ToggleMyProblemBookmark` | Bật hoặc tắt đánh dấu lưu bài của chính người dùng | `problem-bank` |
| 4 | `GetStarterCode` | Sinh mã khung cho cặp (ngôn ngữ, mô hình) đang chọn của một bài | `harness` |
| 5 | `GetEffectiveExecutionLimits` | Trả giới hạn thời gian và bộ nhớ đã nhân hệ số của ngôn ngữ đang chọn | `judge-orchestration` |
| 6 | `RunSampleTestcases` | Chạy thử mã hiện tại với toàn bộ testcase Sample, không ghi nhận tiến độ (F4-02) | `judge-orchestration` |
| 7 | `CreateSubmission` | Tạo một lượt nộp và trả `submissionId` ngay, không chờ kết quả chấm (F4-01) | `judge-orchestration` |
| 8 | `SubscribeSubmissionProgress` | Kênh thời gian thực đẩy trạng thái từng testcase theo `submissionId` (F4-08) | `judge-orchestration` |
| 9 | `ListMySubmissionsForProblem` | Trả các lượt nộp gần nhất của chính người dùng cho chính bài đang mở | `judge-orchestration` |
| 10 | ~~`ListMyUnsolvedRecentProblems`~~ (ĐÃ BỎ khỏi màn này 2026-10-20, giữ số NO) | Trước đây dùng cho thẻ bài đang làm dở và chuyển hướng của mục nav Workspace (Q12). Màn này không còn trạng thái chưa chọn bài và mục nav nay mở `USR0105`, nơi dùng endpoint này [Nguồn: 02-bd/screens/users/USR0101_problem_list.md Sheet 7.3 NO 6; 02-bd/screens/users/USR0105_workspace_home.md:176] | `judge-orchestration` |
| 11 | `GetMySolutionReviewQuickInsight` | Trả tóm tắt nhanh báo cáo phân tích bài giải của chính người dùng cho bài này | `ai-review` |

Ghi chú ranh giới:
- **Tiền tố `My` nghĩa là phạm vi khoá cứng theo người dùng đăng nhập**, không nhận `user_id` làm tham số.
  Quy ước kế thừa từ `problem_list` [Nguồn: 02-bd/screens/users/USR0101_problem_list.md Sheet 7.3, phần ghi chú ranh giới] và áp cho
  cả `ListMySubmissionsForProblem`, `ToggleMyProblemBookmark`, `GetMySolutionReviewQuickInsight`.
- **Không dùng lại tên nghiệp vụ của màn quản trị.** `GetProblemForWorkspace` (A1) khác hẳn endpoint đọc
  bài của `problem_management` (A2/A3) — khác phạm vi dữ liệu (chỉ bài đã xuất bản), khác tập trường
  (không có testcase ẩn, không có checklist xuất bản) và khác quyền.
- **`GetStarterCode` thuộc `harness`, không thuộc `problem-bank`**, vì mã khung là sản phẩm sinh mã chứ
  không phải dữ liệu lưu trữ; `problem-bank` chỉ giữ chữ ký hàm
  [Nguồn: 02-bd/architecture/harness.md:43-45].
- **Chỉ một kênh thời gian thực**, khoá theo `submissionId` — không có kênh theo `problem_id` hay theo
  người dùng [Nguồn: 02-bd/architecture/judge-orchestration.md:33].

[Nguồn: 02-bd/database/problem-bank.md:8-37,57-76,108-129,136-155,189-208,234-247;
02-bd/database/judge-orchestration.md:12-34,50-61,101-112,197; 02-bd/database/ai-review.md:35-50]

---

## Sheet 8. Danh sách sự kiện

> Bộ thẻ và quy ước cột `Chuyển màn`: `02-bd/_rules/bd-template-9sheet.md` mục 2, 3.
>
> **Trạng thái ghi vào URL ở màn này:** `problem_id` nằm trên đường dẫn `/workspace/{problemId}`; ba giá trị `lang`, `model`, `tab`
> nằm ở tham số truy vấn, dùng cơ chế thay thế mục lịch sử (replace) để không sinh một mục lịch sử cho mỗi
> lần bấm đổi tab. Lý do giữ đúng tinh thần đã chốt ở `problem_list`
> [Nguồn: 02-bd/screens/users/USR0101_problem_list.md Sheet 8, ghi chú "Quy ước trạng thái lọc của khu Người học"]: một đường dẫn phải mở lại được đúng ngữ
> cảnh cũ và chia sẻ được. **Phương thức nhập mã và mã nguồn không ghi vào URL** — mã nguồn là dữ liệu
> lớn và riêng tư `[SoT: Suy luận]`.
>
> **Màn này CÓ dữ liệu nhập chưa lưu lên máy chủ** (mã nguồn trong ô soạn mã), nhưng đã được giữ ở trình duyệt theo
> Câu hỏi mở Q4 nên **không có hộp xác nhận rời màn** (Q14, chốt 2026-10-18; EVT-21 gạch bỏ, giữ số).

### Màn chính: Chi tiết bài tập

| NO | Loại | Sự kiện | Chi tiết | Chuyển màn | Gọi API | Tên xử lý | Ghi chú |
| --: | :--- | :--- | :--- | :-: | :-: | :--- | :--- |
| 1 | Màn hình | Khởi tạo màn | Vào màn, đọc `problem_id` trên đường dẫn và ba tham số `lang`, `model`, `tab`. | Không | Có | `GetProblemForWorkspace`, `GetStarterCode`, `ListSampleTestcases`, `GetEffectiveExecutionLimits`, `ListMySubmissionsForProblem`, `GetMySolutionReviewQuickInsight` | [Các bước]<br>1. Kiểm tra người dùng đã đăng nhập.<br>2. Đọc `problem_id` từ đường dẫn `/workspace/{problemId}`; id không mở được thì hiện "Không tìm thấy bài toán." (Sheet 9 NO 2) và dừng. Trạng thái chưa chọn bài và thẻ bài đang làm dở không còn ở màn này (thuộc `USR0105`, 2026-10-20).<br>3. Chuẩn hoá `lang`, `model`, `tab`; giá trị lạ thì bỏ qua và dùng mặc định.<br>4. Hiển thị khung chờ cho panel đề bài, vùng soạn mã và bảng kết quả.<br>5. Tải song song các nhóm dữ liệu; `GetStarterCode` chờ `GetProblemForWorkspace` xong vì cần biết `functionWrapperSupported` mới chốt được mô hình.<br>6. Đọc bản nháp ở `localStorage` theo khoá `(problem_id, language, submission_model)`; có thì dùng thay cho mã khung, không có thì dùng mã khung [Nguồn: 05-coding/frontend/src/views/users/problem-detail/ui/problem-detail-view.tsx:156-166].<br>[Khi thành công] Hiển thị đủ ba tab panel, mã khung trong ô soạn mã, testcase mẫu và hướng dẫn ở bảng kết quả.<br>[Khi lỗi] Lỗi ở `GetProblemForWorkspace` thì hiển thị lỗi toàn màn kèm nút thử lại — không có đề bài thì không làm gì được. Lỗi ở `ai-review` thì **ẩn tab Solution Review, không báo lỗi**. Lỗi ở `ListMySubmissionsForProblem` thì tab Bài nộp hiển thị lỗi riêng kèm nút thử lại, hai tab còn lại vẫn dùng được. Lỗi ở `GetEffectiveExecutionLimits` thì hiển thị giới hạn cơ sở chưa nhân hệ số. Vùng soạn mã và nút Submit **không bao giờ** bị chặn vì một module phụ chết. |
| 2 | ~~Liên kết~~ | ~~Chọn bài ở trạng thái chưa chọn bài~~ (ĐÃ BỎ 2026-10-19, giữ số EVT; Khu vực B đã bỏ) | Bấm nút "Tiếp tục" ở thẻ bài đang làm dở. | Không | Có | Như EVT-1 | [Các bước]<br>1. Ghi `problem_id` vào đường dẫn bằng cơ chế thay thế.<br>2. Chạy lại toàn bộ EVT-1 cho bài được chọn.<br>[Khi thành công] Màn chuyển từ trạng thái trống sang bố cục hai cột. Không rời màn nên **không hỏi xác nhận** — chưa có mã nào để mất. |
| 3 | Nút | Chuyển tab panel thông tin | Bấm một trong ba tab Đề bài / Bài nộp / Solution Review. | Không | Điều kiện | `ListMySubmissionsForProblem` | [Các bước]<br>1. Cập nhật tham số URL `tab` bằng cơ chế thay thế.<br>2. Tab "Bài nộp" mà dữ liệu đã cũ hơn lượt nộp gần nhất thì tải lại; hai tab còn lại dùng dữ liệu đã tải ở EVT-1.<br>[Khi thành công] Nội dung panel đổi, mã trong ô soạn mã và kết quả ở bảng dưới **không đổi**.<br>[Khi lỗi] Hiển thị lỗi trong phạm vi panel, giữ nguyên tab đang chọn. |
| 4 | Nút | Đổi ngôn ngữ | Bấm một trong ba tab Java / C++ / Python. | Không | Có | `GetStarterCode`, `GetEffectiveExecutionLimits` | [Các bước]<br>1. Không hỏi xác nhận ghi đè (Câu hỏi mở Q14): mã của cặp cũ nằm ở bản nháp riêng theo khoá `(problem_id, language, submission_model)`.<br>2. Cập nhật tham số URL `lang` và ghi ngôn ngữ vào `algoprep-workspace-prefs` (Câu hỏi mở Q9).<br>3. Khôi phục bản nháp của cặp mới nếu có, không có thì sinh lại mã khung cho cặp (ngôn ngữ mới, mô hình đang chọn) và tính lại giới hạn hiệu lực.<br>[Khi thành công] Ô soạn mã, nhãn thông tin và dòng giới hạn cập nhật.<br>[Khi lỗi] Không sinh được mã khung thì giữ nguyên ngôn ngữ cũ và hiển thị toast lỗi. |
| 5 | Nút | Đổi mô hình nộp bài | Bấm "Có hàm main" hoặc "Bọc hàm". | Không | Có | `GetStarterCode` | [Các bước]<br>1. Không hỏi xác nhận ghi đè (như EVT-4 bước 1).<br>2. Cập nhật tham số URL `model` và ghi mô hình vào `algoprep-workspace-prefs` (Câu hỏi mở Q9).<br>3. Khôi phục bản nháp của cặp mới nếu có, không có thì sinh lại mã khung cho cặp (ngôn ngữ đang chọn, mô hình mới).<br>[Khi thành công] Ô soạn mã đổi, nhãn mô hình trên tab Đề bài và câu giải thích mô hình đổi theo. **Phương thức nhập mã giữ nguyên** [Nguồn: 01-rd/screens/users/USR0102_problem_detail.md:136].<br>[Khi lỗi] Như EVT-4. |
| 6 | Nút | Đổi phương thức nhập mã | Bấm "Gõ trực tiếp" hoặc "Tải file". | Không | Điều kiện | `GetStarterCode` | [Các bước]<br>1. Đổi sang "Gõ trực tiếp" thì nạp lại mã khung nếu trước đó chưa có mã nào trong ô soạn.<br>2. Đổi sang "Tải file" thì **giữ nguyên** mã đã gõ, chỉ đổi vùng hiển thị.<br>[Khi thành công] Vùng soạn mã hoặc vùng tải file hiện lên. Mô hình và ngôn ngữ **không đổi**. |
| 7 | Nhập liệu | Soạn mã trong trình soạn thảo | Gõ hoặc dán mã vào ô soạn mã. | Không | Không | - | [Các bước]<br>1. Cập nhật trạng thái mã ở phía trình duyệt.<br>2. Ghi mã vào `localStorage` theo khoá `(problem_id, language, submission_model)` [Nguồn: 05-coding/frontend/src/views/users/problem-detail/ui/problem-detail-view.tsx:210-220].<br>3. Không đánh dấu "chưa nộp" vì không còn hộp xác nhận rời màn (EVT-21 đã bỏ).<br>[Khi thành công] Số dòng và nhãn thông tin cập nhật, nhãn "Đã lưu trên trình duyệt này" hiện ở chân vùng soạn mã. **Không gọi máy chủ theo từng phím** — mã chỉ đi lên máy chủ khi bấm Run Code hoặc Submit. Ghi `localStorage` thất bại thì bỏ qua, không báo lỗi, vẫn soạn được. |
| 8 | Nhập liệu | Tải file mã nguồn lên | Kéo-thả hoặc chọn một file. | Không | Không | - | [Các bước]<br>1. Kiểm tra phần mở rộng đúng ngôn ngữ đang chọn và kích thước tối đa 64 KB (Sheet 9 NO 5, NO 6).<br>2. Đọc nội dung file ở phía trình duyệt, nạp vào cùng trường mã nguồn của luồng nộp bài.<br>[Khi thành công] Hiện thẻ file đã tải kèm tên và kích thước.<br>[Khi lỗi] Hiển thị toast lỗi (một toast cho lỗi đầu tiên), viền vùng tải đổi sang đỏ, không dùng hộp thoại; giữ nguyên file đã tải trước đó nếu có. |
| 9 | Nút | Xem nội dung file đã tải | Bấm "Xem nội dung". | Không | Không | - | [Các bước]<br>1. Hiển thị nội dung file ở chế độ chỉ đọc trong chính vùng soạn mã.<br>[Khi thành công] Người học đối chiếu được nội dung trước khi nộp. Không đổi phương thức nhập mã. |
| 10 | Nút | Chạy thử với testcase mẫu | Bấm "Run Code". | Không | Có | `RunSampleTestcases` | [Các bước]<br>1. Kiểm tra mã không rỗng và không quá 64 KB (đo bằng byte, Sheet 9 NO 8); vượt thì hiện toast lỗi và dừng, không gửi [Nguồn: 05-coding/frontend/src/views/users/problem-detail/ui/problem-detail-view.tsx:258-273].<br>2. Chuyển bảng kết quả sang tab "Kết quả", hiển thị trạng thái đang chạy.<br>3. Gửi mã, ngôn ngữ và mô hình đang chọn lên máy chủ.<br>[Khi thành công] Hiển thị dải ô trạng thái từng testcase Sample, chỉ số thời gian và bộ nhớ, và bảng đối chiếu output thực tế với kết quả mong đợi. **Không tạo dòng `submissions` nào** (Câu hỏi mở Q10, chốt 2026-10-18): kết quả chỉ trả trong lời gọi đồng bộ và không lưu. **Không ghi nhận vào tiến độ** (F4-02) [Nguồn: 01-rd/req/judge-orchestration.md:16] — tab "Bài nộp" không đổi, dải chỉ số ở `my_progress` không đổi. Chạy xong thì hiện một toast thành công nêu số testcase mẫu đã chạy [Nguồn: 05-coding/frontend/src/views/users/problem-detail/ui/problem-detail-view.tsx:300-304].<br>[Khi lỗi] Lỗi biên dịch thì nội dung lỗi đã ánh xạ về dòng của người học (F3-11) hiển thị trong bảng kết quả (là nội dung kết quả chấm, không phải thông báo thao tác); lỗi hệ thống thì hiển thị toast lỗi, nút Run Code dùng lại được để chạy lại. |
| 11 | Nút | Nộp bài | Bấm "Submit". | Không | Có | `CreateSubmission` | [Các bước]<br>1. Kiểm tra mã không rỗng, không quá 64 KB (vượt thì toast lỗi và dừng) và chưa vượt giới hạn số lượt nộp mỗi giờ (Sheet 9 NO 10).<br>2. Gửi mã, ngôn ngữ, mô hình và phương thức nhập mã lên máy chủ.<br>3. Nhận `submissionId` ngay, **không chờ kết quả chấm** (F4-01) [Nguồn: 01-rd/req/judge-orchestration.md:10-11].<br>4. Chuyển bảng kết quả sang tab "Kết quả", mở kênh thời gian thực theo `submissionId` (EVT-12).<br>[Khi thành công] Nhãn kết quả hiển thị "Đang chờ", dải ô trạng thái dựng sẵn đủ số testcase ở trạng thái chờ.<br>[Khi lỗi] Vượt giới hạn số lượt nộp thì không tạo lượt nộp và hiển thị toast lỗi. Lỗi mạng thì hiển thị toast lỗi, giữ nguyên mã, cho nộp lại — **không** tự nộp lại để tránh tạo hai lượt nộp trùng. |
| 12 | Màn hình | Nhận cập nhật trạng thái theo thời gian thực | Kênh theo `submissionId` đẩy về một thông điệp trạng thái. | Không | Có | `SubscribeSubmissionProgress` | [Các bước]<br>1. Nhận thông điệp, đối chiếu `submissionId` với lượt nộp đang theo dõi; lệch thì bỏ qua.<br>2. Tô ô trạng thái của testcase tương ứng theo thứ tự.<br>3. Cập nhật nhãn kết quả tổng hợp và số testcase đạt trên tổng.<br>4. Thông điệp mang một trong sáu trạng thái cuối (`ACCEPTED`, `WRONG_ANSWER`, `TIME_LIMIT_EXCEEDED`, `RUNTIME_ERROR`, `COMPILE_ERROR`, `SYSTEM_ERROR`, `02-bd/database/judge-orchestration.md:22`) thì đóng kênh, nạp lại tab "Bài nộp", và hiển thị các liên kết đi tiếp.<br>[Khi thành công] Người học thấy tiến trình chấm chạy dần, **không dừng ở testcase hỏng đầu tiên** (`DEC-2026-0831-partial-score-testcase-ratio`). Khi nhận trạng thái cuối thì hiện một toast kết quả: Accepted là toast thành công, kết quả khác là toast lỗi kèm số testcase đạt trên tổng [Nguồn: 05-coding/frontend/src/views/users/problem-detail/ui/problem-detail-view.tsx:321-328].<br>[Khi lỗi] Mất kết nối thì thử nối lại vài lần; vẫn không được thì chuyển sang hỏi lại trạng thái theo chu kỳ và hiển thị một dòng nhắc rằng cập nhật đang chậm. **Không** kết luận bài trượt khi chỉ mất kênh. |
| 13 | Nút | Chuyển tab bảng kết quả | Bấm "Testcase" hoặc "Kết quả". | Không | Không | - | [Các bước]<br>1. Đổi nội dung bảng kết quả.<br>[Khi thành công] Trạng thái tab không ghi vào URL; mọi dữ liệu đã có sẵn ở phía trình duyệt. |
| 14 | Nút | Xem chi tiết lượt chạy (màn phụ) | Bấm "Xem chi tiết" ở bảng kết quả, hoặc tự mở khi bấm Run Code / Submit. Đóng bằng nút "Đóng", phím Esc hoặc bấm ra ngoài. | Không | Không | - | [Các bước]<br>1. Mở hộp thoại: Run Code có danh sách testcase Sample bên trái (nhãn Đạt / Không đạt) và chi tiết dữ liệu vào, thực tế, mong đợi bên phải, mở sẵn testcase sai đầu tiên; Submit liệt kê mọi testcase với Đạt / Không đạt.<br>[Khi thành công] Hiển thị đúng kết quả lượt gần nhất. **Chỉ testcase Sample hiện dữ liệu**; testcase ẩn chỉ hiện Đạt / Không đạt, không lộ dữ liệu [Nguồn: 05-coding/frontend/src/views/users/problem-detail/ui/run-results-dialog.tsx]. |
| 15 | Nút | Bật hoặc tắt lưu bài | Bấm "Lưu bài" / "Bỏ lưu". | Không | Có | `ToggleMyProblemBookmark` | [Các bước]<br>1. Đổi nhãn nút ngay (cập nhật lạc quan).<br>2. Gửi yêu cầu bật/tắt cho bài đang mở.<br>[Khi thành công] Nút giữ trạng thái mới, hiện toast thành công "đã lưu" hoặc "đã bỏ lưu" [Nguồn: 05-coding/frontend/src/views/users/problem-detail/ui/problem-detail-view.tsx:275-285]; bài xuất hiện hoặc biến mất ở màn `saved_problems`.<br>[Khi lỗi] Hoàn nguyên nhãn nút và hiển thị toast lỗi. Không rời màn, không mất mã đang soạn. |
| 16 | Liên kết | Mở kết quả chấm đầy đủ | Bấm "Xem kết quả chấm", hoặc bấm một dòng trong tab "Bài nộp". | Có | Không | - | [Các bước]<br>1. Điều hướng thẳng, không hỏi xác nhận (EVT-21 đã bỏ, Câu hỏi mở Q14); điều hướng sang `submission_result` kèm `submission_id`.<br>[Khi thành công] Mở màn `submission_result`. |
| 17 | Liên kết | Mở phân tích bài giải | Bấm "Phân tích bài giải" ở bảng kết quả, hoặc liên kết mở báo cáo đầy đủ trong tab Solution Review. | Có | Không | - | [Các bước]<br>1. Điều hướng thẳng, không hỏi xác nhận (EVT-21 đã bỏ, Câu hỏi mở Q14); điều hướng sang `solution_review` kèm `submission_id` của lượt nộp `ACCEPTED`.<br>[Khi thành công] Mở màn `solution_review`. Việc chạy phân tích là hành động tự chọn ở màn đích, màn này **không** tự kích hoạt F5 (`DEC-2026-0831-outside-screens-closures`). |
| 18 | Liên kết | Mở phỏng vấn giả lập | Bấm "Phỏng vấn giả lập" ở bảng kết quả, hoặc nút trong tab Solution Review. | Có | Không | - | [Các bước]<br>1. Điều hướng thẳng, không hỏi xác nhận (EVT-21 đã bỏ, Câu hỏi mở Q14); điều hướng sang `mock_interview` kèm `submission_id`, lối vào ứng với `entry_type = SUBMISSION` [Nguồn: 02-bd/database/ai-review.md:101-102].<br>[Khi thành công] Mở màn `mock_interview`. |
| 19 | Liên kết | Quay lại danh sách bài | Bấm "Danh sách bài" trên thanh tác vụ, hoặc liên kết ở thông báo "Không tìm thấy bài toán.". | Có | Không | - | [Các bước]<br>1. Điều hướng thẳng, không hỏi xác nhận (EVT-21 đã bỏ, Câu hỏi mở Q14); điều hướng sang `problem_list`.<br>[Khi thành công] Mở màn `problem_list`. Bộ lọc cũ của màn đó còn nguyên trên lịch sử trình duyệt. |
| 20 | Nút | Kéo thanh chia đôi | Kéo ngang thanh giữa hai cột. | Không | Không | - | [Các bước]<br>1. Cập nhật tỉ lệ bề rộng cột trái, chặn trong khoảng 22–74 [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:441].<br>2. Ghi lại giá trị ở phía trình duyệt khi thả chuột.<br>[Khi thành công] Bố cục đổi ngay; trình soạn thảo tính lại kích thước. Không gọi máy chủ, không ghi vào URL. Thanh kéo ngang thứ hai giữa vùng soạn mã và bảng kết quả hoạt động tương tự: mặc định 62/38, chặn 25–85%, nhớ ở `algoprep-workspace-split-y` (Q15, chốt 2026-10-19). |
| 21 | Màn hình | ~~Rời màn khi còn mã chưa nộp~~ (ĐÃ BỎ) | ~~Bất kỳ thao tác nào rời khỏi màn.~~ | - | - | - | **Đã bỏ theo Câu hỏi mở Q14 (chủ dự án chốt 2026-10-18, ponytail bậc 4).** Giữ lại số EVT để không phải đánh số lại các tham chiếu chéo. Lý do: mã được ghi vào `localStorage` theo khoá `(problem_id, language, submission_model)` ngay khi gõ nên rời màn hay đóng thẻ đều không mất mã; chỉ mất khi đổi trình duyệt hoặc xoá dữ liệu trang, và đó là chấp nhận được. Không có hộp xác nhận nào ở màn này. |
| 22 | Liên kết | Mở một lượt nộp trong lịch sử | Bấm một dòng trong tab "Bài nộp". | Có | Không | - | [Các bước]<br>1. Điều hướng thẳng, không hỏi xác nhận (EVT-21 đã bỏ, Câu hỏi mở Q14); điều hướng sang `submission_result` kèm `submission_id` của dòng.<br>[Khi thành công] Mở màn `submission_result`. Mã nguồn của lượt nộp cũ **không** được nạp ngược vào ô soạn mã của màn này — nếu cần chức năng đó thì đó là yêu cầu mới, chưa có mã `Fx-nn`. |

[Nguồn: 09-layoutBase/Workspace giải bài.dc.html:105,106,159-163,218-225,253,258-260,379,380,381,441,
606,640-641; 01-rd/req/judge-orchestration.md:10-11,16; 01-rd/screens/users/USR0102_problem_detail.md:118,129;
02-bd/database/ai-review.md:101-102]

---

## Sheet 9. Đặc tả kiểm tra

> Bộ thẻ và quy ước mã thông báo: `02-bd/_rules/bd-template-9sheet.md` mục 2, 4. Sheet này ghi **điều kiện
> kiểm và thông báo**; tiêu chí nghiệm thu `AC-nn` thuộc `04-tdd/problem_detail.md`, không lặp lại ở đây.

| NO | Loại | Tóm tắt kiểm | Chi tiết | Mức | Mã thông báo | Ghi chú | EVT gọi | Thứ tự |
| --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: |
| 1 | Kiểm quyền | Yêu cầu đăng nhập | [Nội dung kiểm] Người dùng chưa đăng nhập thì chuyển về màn `auth`.<br>[Nơi thực thi] Chặn ở cả tầng định tuyến phía giao diện và tầng phân quyền phía máy chủ. Màn có thao tác ghi nên **không có** chế độ khách. | Lỗi | Chưa có mã thông báo | Nội dung "Vui lòng đăng nhập để tiếp tục." | EVT-1 | 1 |
| 2 | Kiểm nghiệp vụ | Phạm vi bài toán mở được | [Nội dung kiểm] Chỉ mở được bài có `status = PUBLISHED` và `deleted = false`; bài chưa xuất bản hoặc đã ẩn mềm trả về như không tồn tại.<br>[Nơi thực thi] Máy chủ, trong điều kiện truy vấn. | Lỗi | Chưa có mã thông báo | Nội dung "Không tìm thấy bài toán." **Không** báo "bạn không có quyền" — tránh lộ sự tồn tại của bài chưa xuất bản (`DEC-2026-0830-problem-lifecycle-two-states`) [Nguồn: 02-bd/database/problem-bank.md:19-20]. | EVT-1, EVT-2 | 1 |
| 3 | Kiểm nghiệp vụ | **Không lộ testcase ẩn dưới mọi hình thức** | [Nội dung kiểm] Không endpoint nào của màn trả về dữ liệu vào, kết quả mong đợi, output thực tế hay diff của testcase có `visibility = HIDDEN`. Thông điệp thời gian thực chỉ mang `testcase_order` và `verdict`. Kết quả tổng hợp chỉ mang số testcase đạt trên tổng và chỉ số testcase hỏng đầu tiên.<br>[Nơi thực thi] **Máy chủ** — lọc ở tầng dựng phản hồi, không phải ẩn ở giao diện.<br>[Tiêu điểm] Dải ô trạng thái, khối kết quả dạng văn bản, kênh thời gian thực. | Lỗi | Không có thông báo | **Bắt buộc.** Ràng buộc cứng của dự án. Ẩn ở giao diện là không đủ: bất kỳ ai mở công cụ mạng của trình duyệt cũng đọc được phản hồi thô. Chú giải của mỗi ô chỉ được ghi dạng "Testcase {số}: {trạng thái}". | EVT-11, EVT-12 | 1 |
| 4 | Kiểm nghiệp vụ | Chỉ đọc testcase Sample | [Nội dung kiểm] `ListSampleTestcases` và `RunSampleTestcases` chỉ nhận dòng `testcases.visibility = SAMPLE`; điều kiện này đặt trong truy vấn, không phải lọc sau khi đã lấy hết.<br>[Nơi thực thi] Máy chủ. | Lỗi | Không có thông báo | Hai lớp độc lập với NO 3: một lớp chặn ở nguồn, một lớp chặn ở phản hồi [Nguồn: 02-bd/database/problem-bank.md:142]. | EVT-1, EVT-10 | 2 |
| 5 | Kiểm nhập liệu | Phần mở rộng file tải lên | [Nội dung kiểm] Chỉ nhận đúng phần mở rộng của ngôn ngữ đang chọn: `.java`, `.cpp`, `.py`.<br>[Nơi thực thi] Màn hình khi chọn file, và máy chủ khi nhận nội dung.<br>[Tiêu điểm] Viền vùng kéo thả file + toast. | Lỗi | Chưa có mã thông báo | Nội dung "File phải có phần mở rộng {ext} đúng với ngôn ngữ đang chọn." Hiển thị bằng toast, không dùng hộp thoại [Nguồn: 01-rd/req/judge-orchestration.md:14-15]. | EVT-8 | 1 |
| 6 | Kiểm nhập liệu | Kích thước file tải lên | [Nội dung kiểm] File lớn hơn 64 KB thì từ chối, không đọc nội dung.<br>[Nơi thực thi] Màn hình và máy chủ.<br>[Tiêu điểm] Viền vùng kéo thả file + toast. | Lỗi | Chưa có mã thông báo | Nội dung "File vượt quá 64 KB." [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:307]. | EVT-8 | 2 |
| 7 | Kiểm nhập liệu | Mã nguồn rỗng | [Nội dung kiểm] Mã chỉ gồm khoảng trắng thì không cho chạy thử và không cho nộp.<br>[Nơi thực thi] Màn hình và máy chủ.<br>[Tiêu điểm] Viền ô soạn mã + toast. | Cảnh báo | Chưa có mã thông báo | Nội dung "Hãy viết mã trước khi chạy thử hoặc nộp bài." Hai nút Run Code và Submit không kích hoạt. | EVT-10, EVT-11 | 1 |
| 8 | Kiểm nhập liệu | Độ dài mã nguồn | [Nội dung kiểm] Mã nguồn vượt 64 KB thì từ chối, kể cả khi gõ trực tiếp.<br>[Nơi thực thi] Máy chủ là nơi kiểm thật; màn hình đo bằng byte (`TextEncoder`) và chặn trước khi gửi [Nguồn: 05-coding/frontend/src/views/users/problem-detail/ui/problem-detail-view.tsx:67-68,258-273].<br>[Tiêu điểm] Viền ô soạn mã + toast. | Lỗi | Chưa có mã thông báo | Nội dung "Mã nguồn vượt 64 KB, hãy rút gọn trước khi chạy hoặc nộp." (khoá `problemDetail.toast.codeTooLarge`). Dùng cùng ngưỡng với file tải lên để hai đường vào không lệch nhau — đã chốt ở Câu hỏi mở Q7 (2026-10-18). | EVT-10, EVT-11 | 2 |
| 9 | Kiểm nghiệp vụ | Mô hình nộp bài hợp lệ cho bài | [Nội dung kiểm] Bài có `function_wrapper_supported = false` thì từ chối mọi lượt nộp có `submission_model = FUNCTION_WRAPPER`, kể cả khi tham số URL `model` bị sửa tay.<br>[Nơi thực thi] Máy chủ. | Lỗi | Chưa có mã thông báo | Nội dung "Bài toán này chỉ hỗ trợ mô hình Standard I/O." Giao diện đã ẩn nút nhưng ẩn ở giao diện không phải là kiểm tra (F3-13) [Nguồn: 02-bd/database/problem-bank.md:21; 01-rd/req/harness.md:30-34]. | EVT-5, EVT-10, EVT-11 | 1 |
| 10 | Kiểm nghiệp vụ | Giới hạn số lượt nộp mỗi giờ | [Nội dung kiểm] Số lượt nộp của người học cho bài này trong một giờ gần nhất vượt `problems.max_submissions_per_hour` thì từ chối; cột `NULL` thì dùng mặc định hệ thống 60 lần/giờ.<br>[Nơi thực thi] Máy chủ. | Lỗi | Chưa có mã thông báo | Nội dung "Bạn đã nộp quá số lần cho phép trong một giờ, thử lại sau." [Nguồn: 02-bd/database/problem-bank.md:25; 02-bd/database/problem-bank.md:282]. | EVT-11 | 2 |
| 11 | Kiểm nghiệp vụ | Phạm vi dữ liệu cá nhân | [Nội dung kiểm] Lịch sử nộp bài, trạng thái lưu bài và tóm tắt phân tích bài giải chỉ tính theo chính người dùng đăng nhập; không endpoint nào của màn nhận `user_id` từ phía gọi.<br>[Nơi thực thi] Máy chủ. | Lỗi | Không có thông báo | Quy ước tiền tố `My` của khu Người học [Nguồn: 02-bd/screens/users/USR0101_problem_list.md Sheet 7.3, phần ghi chú ranh giới]. Áp cho `ListMySubmissionsForProblem`, `ToggleMyProblemBookmark`, `GetMySolutionReviewQuickInsight`, `ListMyUnsolvedRecentProblems`. | EVT-1, EVT-3, EVT-15 | 1 |
| 12 | Kiểm quyền | Chủ sở hữu kênh thời gian thực | [Nội dung kiểm] Chỉ người tạo ra một lượt nộp mới đăng ký được kênh trạng thái của `submissionId` đó.<br>[Nơi thực thi] Máy chủ, tại bước bắt tay của kênh. | Lỗi | Không có thông báo | Không có kênh này thì đoán `submissionId` là đọc được tiến trình chấm của người khác. Từ chối im lặng, không xác nhận `submissionId` có tồn tại hay không. | EVT-12 | 1 |
| 13 | Kiểm nghiệp vụ | Suy giảm êm khi phân hệ AI lỗi | [Nội dung kiểm] Gọi `ai-review` thất bại hoặc hết hạn mức thì ẩn tab "Solution Review" và hai liên kết "Phân tích bài giải", "Phỏng vấn giả lập"; **không** hiển thị thông báo lỗi.<br>[Nơi thực thi] Màn hình. | Cảnh báo | Không có thông báo | **Bắt buộc.** Luồng đọc đề, soạn mã, chạy thử, nộp bài, xem kết quả phải chạy đủ khi F5 chết (`CLAUDE.md` mục Rules). Người học không có hành động khắc phục nào nên không báo lỗi. | EVT-1, EVT-3, EVT-12 | 2 |
| 14 | Kiểm nghiệp vụ | Mã nguồn là dữ liệu, không phải chỉ thị | [Nội dung kiểm] Mọi nơi mã nguồn của người học được đưa sang phân hệ AI, nó phải là tham số dữ liệu tách hẳn khỏi chỉ thị hệ thống.<br>[Nơi thực thi] Máy chủ, ở module `ai-review`. | Lỗi | Không có thông báo | **Bắt buộc.** Màn này không tự gọi mô hình ngôn ngữ; nó chỉ chuyển `submission_id` sang màn đích. Ghi ở đây để bên nhận không coi mã nguồn là một phần lời nhắc (`CLAUDE.md` mục Rules, `DEC-2026-0831-ai-instruction-injection-guard`). | EVT-17, EVT-18 | 1 |
| 15 | Kiểm nghiệp vụ | Che lỗi thuộc mã khung | [Nội dung kiểm] Thông báo lỗi biên dịch hiển thị cho người học phải đã qua bước ánh xạ về đúng dòng mã của họ (F3-11), và mọi lỗi thuộc phần mã khung do hệ thống sinh phải bị lọc bỏ hoàn toàn (F3-12).<br>[Nơi thực thi] Máy chủ, ở module `harness`; màn chỉ hiển thị chuỗi đã xử lý. | Lỗi | Không có thông báo | Không bao giờ hiển thị nguyên văn `stderr` của trình biên dịch [Nguồn: 02-bd/architecture/harness.md:274; 02-bd/database/judge-orchestration.md:24]. | EVT-10, EVT-12 | 2 |
| 16 | ~~Kiểm nghiệp vụ~~ | ~~Cảnh báo trước khi rời màn~~ (ĐÃ BỎ) | **Đã bỏ theo Câu hỏi mở Q14 (chốt 2026-10-18).** Mã được giữ ở `localStorage` nên không còn lý do hỏi xác nhận; giữ số NO để không lệch tham chiếu. | - | - | - | - | - |
| 17 | Kiểm nghiệp vụ | Lỗi hệ thống hoặc lỗi gọi máy chủ | [Nội dung kiểm] Gọi máy chủ thất bại thì giữ nguyên mã đang soạn và cho thử lại; không bao giờ xoá hay thay mã của người dùng vì một lỗi mạng.<br>[Nơi thực thi] Màn hình. | Lỗi | Mã lỗi trong phản hồi | Phản hồi có mã lỗi đã đăng ký thì hiển thị nội dung tương ứng; chưa đăng ký thì hiển thị "Không kết nối được máy chủ." Lỗi của EVT-1 và EVT-3 là vùng lỗi thay chỗ nội dung kèm nút thử lại; lỗi của EVT-4, EVT-5, EVT-10, EVT-11, EVT-15 hiện bằng toast. | EVT-1, EVT-3, EVT-4, EVT-5, EVT-10, EVT-11, EVT-15 | 1 |

Cột `Thứ tự` là thứ tự kiểm trong cùng một sự kiện.

[Nguồn: 02-bd/database/problem-bank.md:19-21,25,142,282; 02-bd/database/judge-orchestration.md:24;
02-bd/architecture/harness.md:274; 01-rd/req/judge-orchestration.md:14-15;
01-rd/req/harness.md:30-34; 09-layoutBase/Workspace giải bài.dc.html:307;
01-rd/screens/users/USR0102_problem_detail.md:118]

---

## Câu hỏi mở

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | **Prototype còn nguyên tab "Gợi ý" đã bị cắt khỏi phạm vi — cập nhật prototype hay đánh dấu lỗi thời?** | **Đã chốt 2026-10-18 (owner), theo đề xuất** (`DEC-2026-1018-usr-open-questions-phase1-scope`). Hai nguồn từng mâu thuẫn: prototype tĩnh khai tab "Gợi ý" ở `:552`, `:205-214`, `:559`, `:574-578`, `:173`; `DEC-2026-0831-remove-tiered-hints-ai-config` cắt hẳn tính năng. | Giữ **ba** tab (Đề bài / Bài nộp / Solution Review). `09-layoutBase/Workspace giải bài.dc.html` **lỗi thời** ở điểm này — chỉ đánh dấu ở mục 4.4, không sửa `.dc.html` (thư mục chỉ-đọc). Tiêu chí ponytail bậc 1 (không có yêu cầu nguồn). Dòng nợ prototype do bên khác ghi, BD này không sửa `06-plan/PROTOTYPE_DEBT.md` | Đã đóng |
| Q2 | **Khối "Gợi ý theo điểm yếu của bạn" ở trạng thái chưa chọn bài lấy dữ liệu ở đâu?** | **Đã chốt 2026-10-18 (owner), theo đề xuất** (`DEC-2026-1018-usr-open-questions-phase1-scope`). Không có mã `Fx-nn` nào cho gợi ý bài theo điểm yếu, không có bảng lưu "điểm yếu" `[SoT: Suy luận]` — rà các bảng ở `02-bd/database/identity.md` mục 1.1-1.15 (`:8-243`) không thấy bảng nào lưu điểm yếu. | **Bỏ** khối "Gợi ý theo điểm yếu của bạn". Trạng thái chưa chọn bài chỉ hiển thị bài đang làm dở gần nhất kèm liên kết sang `problem_list`. Tiêu chí ponytail bậc 1 và 2 | Đã đóng |
| Q3 | **Đồng hồ phiên làm bài lấy mốc từ đâu và có lưu lại không?** | **Đã chốt 2026-10-18 (owner), theo đề xuất** (`DEC-2026-1018-usr-open-questions-phase1-scope`). Prototype hiển thị `00:12:41` cứng; không có cột nào lưu mốc bắt đầu, không có mã `Fx-nn` nào đòi đo thời gian làm bài. | **Bỏ hẳn** đồng hồ phiên làm bài, không hiển thị số bịa. Đã xoá item Khu vực A NO 4 ở Sheet 5, 6. Tiêu chí ponytail bậc 1 và 2 | Đã đóng |
| Q4 | **Mã đang soạn có được lưu nháp không, lưu ở đâu?** | **Đã chốt 2026-10-18 (owner), theo đề xuất** (`DEC-2026-1018-usr-open-questions-phase1-scope`). Không có bảng nào lưu bản nháp mã nguồn trong BD database; cùng khoảng trống ở `problem_list` Q4. | Bản nháp lưu ở **trình duyệt** (`localStorage`) theo khoá `(problem_id, language, submission_model)`, nhãn "Đã lưu trên trình duyệt này"; **không có bảng nháp ở máy chủ**. Đã dựng [Nguồn: 05-coding/frontend/src/views/users/problem-detail/ui/problem-detail-view.tsx:147,156-166,210-220,595-597]. Tiêu chí ponytail bậc 4 và 5 (dùng sẵn trình duyệt, không thêm bảng). Hộp xác nhận rời màn đã bỏ ở Q14 | Đã đóng |
| Q5 | **"Ràng buộc" và "Câu hỏi mở rộng" là cột riêng hay nằm trong `statement_md`?** | **Đã chốt 2026-10-18 (owner), theo đề xuất** (`DEC-2026-1018-usr-open-questions-phase1-scope`), **phần "Ràng buộc" bị thay ngày 2026-10-22** (PB-03, `DEC-2026-1022-problem-bank-schema-sync` mục 1). Prototype tách hai khối riêng; lúc chốt, `problems` chỉ có cột `statement_md` [Nguồn: 02-bd/database/problem-bank.md:16]. | **"Ràng buộc" là cột riêng `problems.constraints_md`** (TEXT NOT NULL default rỗng, Markdown; RD F2-14 và AI sinh testcase cần nó tách khỏi đề). `statement_md` giữ nguyên là phần đề cho người học và **màn này ghép `statement_md` rồi `constraints_md`** thành một trang đề (`02-bd/database/problem-bank.md:16-17`). **"Câu hỏi mở rộng" vẫn nằm trong `statement_md`** như mục Markdown quy ước `## Câu hỏi mở rộng` (không có quyết định tách cột). Quyết định cũ "ràng buộc là mục `## Ràng buộc` trong `statement_md`" không còn đúng; dự án greenfield nên không có dữ liệu cũ; nếu sau này có bài cũ mang mục `## Ràng buộc` trong `statement_md` thì tách một lần sang `constraints_md` (DEC-2026-1022-db-sync-round2, mục 13) | Đã đóng |
| Q6 | **Câu giải thích cho từng ví dụ lưu ở đâu?** | **Đã chốt 2026-10-18 (owner), theo đề xuất** (`DEC-2026-1018-usr-open-questions-phase2-5`). Prototype hiển thị một dòng giải thích dưới mỗi ví dụ [Nguồn: 09-layoutBase/Workspace giải bài.dc.html:186,564-566]; trước đó bảng `testcases` không có cột nào cho câu giải thích. | Giải thích từng ví dụ ban đầu lưu ở cột `testcases.explanation_md`; **từ 2026-10-22 (PB-08) cột đó và `is_worked_example` đã bỏ, ví dụ mẫu và lời giải thích nằm ở bảng riêng `problem.worked_examples.explanation_md TEXT nullable`** (`02-bd/database/problem-bank.md:234-247`), tách khỏi `testcases` vì không thuộc bộ chấm. Bản dựng đã in `example.explanation` khi có [05-coding/frontend/src/views/users/problem-detail/ui/problem-detail-view.tsx:386-388]. Tiêu chí ponytail bậc 1. | Đã đóng |
| Q7 | **Giới hạn kích thước mã gõ trực tiếp có bằng 64 KB của file tải lên không?** | **Đã chốt 2026-10-18 (owner), theo đề xuất** (`DEC-2026-1018-usr-open-questions-phase2-5`). RD chốt 64 KB cho file tải lên nhưng chưa nói về mã gõ trực tiếp; hai đường vào cùng đổ vào `submissions.source_code`. | Mã gõ trực tiếp giới hạn **64 KB** như file tải lên; kiểm thật ở máy chủ (DD `judge-orchestration`). Bản dựng chặn Run Code và Submit bằng toast lỗi khi vượt, đo bằng byte [05-coding/frontend/src/views/users/problem-detail/ui/problem-detail-view.tsx:67-68,258-273]. Tiêu chí ponytail bậc 6. | Đã đóng |
| Q8 | **Trạng thái cụm chấm ở chân panel lấy ở đâu, làm mới thế nào?** | **Đã chốt 2026-10-18 (owner), theo đề xuất** (`DEC-2026-1018-usr-open-questions-phase2-5`). Số liệu trạng thái cụm chấm thuộc `judge-orchestration` và là cùng câu hỏi với chân trang khung chung. | Chấm trạng thái cụm chấm **vẫn ẩn cho tới khi có dữ liệu**, theo kết luận `_shell` Q3 (câu tĩnh, không gọi định kỳ). Bản dựng không có chấm nào (đã đọc toàn bộ `problem-detail-view.tsx`). Tiêu chí ponytail bậc 5. | Đã đóng |
| Q9 | **Ngôn ngữ và mô hình đang chọn có nhớ giữa các lần vào không, và mặc định là gì?** | **Đã chốt 2026-10-18 (owner), theo đề xuất** (`DEC-2026-1018-usr-open-questions-phase2-5`). Chưa có nguồn mặc định ngôn ngữ và mô hình khi vào bài mới. | Nhớ ngôn ngữ và mô hình gần nhất ở `localStorage` (khoá `algoprep-workspace-prefs`), dùng làm mặc định cho bài tiếp theo; bài chỉ có Standard I/O luôn dùng Standard I/O. Không thêm cột ở `identity`. Đã dựng [05-coding/frontend/src/views/users/problem-detail/ui/problem-detail-view.tsx:170-190,192-208]. Tiêu chí ponytail bậc 4. | Đã đóng |
| Q10 | **Lượt chạy thử có tạo dòng dữ liệu nào không?** | **Đã chốt 2026-10-18 (owner), theo đề xuất** (`DEC-2026-1018-usr-open-questions-phase2-5`). `submissions` không có cột phân biệt lượt chạy thử với lượt nộp. | Run Code **không** tạo dòng `submissions`; kết quả trả trong lời gọi đồng bộ và không lưu (ghi vào DD `judge-orchestration`, không thêm cột `is_trial`). Bản dựng chỉ giả lập ở phía trình duyệt [05-coding/frontend/src/views/users/problem-detail/ui/problem-detail-view.tsx:287-306]. Tiêu chí ponytail bậc 5. | Đã đóng |
| Q11 | **Chỉ số "Beats" (F4-12) hiển thị ở màn nào?** | **Đã chốt 2026-10-18 (owner), theo đề xuất** (`DEC-2026-1018-usr-open-questions-phase1-scope`). F4-12 không nói hiển thị ở màn nào. | "Beats" **chỉ ở `submission_result`**, không đưa vào màn này; ô "Beats" ở `:645` của prototype lỗi thời. Tiêu chí ponytail bậc 4 (dùng lại màn kết quả đầy đủ) | Đã đóng |
| Q12 | **Vào Workspace từ mục nav khi chưa chọn bài thì mở bài nào?** | **Đã chốt 2026-10-18 (owner), theo đề xuất** (`DEC-2026-1018-usr-open-questions-phase1-scope`): chuyển hướng sang bài đang làm dở gần nhất, không có thì sang `problem_list` (trùng `_shell` mục 6 Q2). **Phần chuyển hướng đã được thay thế 2026-10-20** bởi `DEC-2026-1020-student-workspace-split-and-password-in-settings` [Nguồn: .nexa/control/decision-registry.md:4244-4245; 02-bd/screens/users/_shell.md:163]. | Mục nav "Workspace" mở trang đầu `/workspace` (`USR0105`) liệt kê bài đang làm dở để chọn tiếp; màn này (nay `/workspace/{problemId}`) luôn có `problem_id`. Câu trả lời gốc giữ làm lịch sử; `workspace/page.tsx` nay chỉ render `WorkspaceHomeView` [Nguồn: 05-coding/frontend/src/app/(student)/workspace/page.tsx:1-5]. | Đã đóng (phần chuyển hướng đã thay) |
| Q13 | **Hiển thị "1. Two Sum" — số thứ tự bài lấy ở đâu?** | **Đã chốt 2026-10-18 (owner), theo đề xuất** (`DEC-2026-1018-usr-open-questions-phase1-scope`). `problems.code` là chuỗi tự do, không phải số thứ tự [Nguồn: 02-bd/database/problem-bank.md:14]. | Hiển thị `problems.code` **nguyên dạng chuỗi** [Nguồn: 05-coding/frontend/src/views/users/problem-detail/ui/problem-detail-view.tsx:788-790]; muốn kiểu đánh số LeetCode thì đặt `code` là `1`, `207`... Tiêu chí ponytail bậc 5 (mặc định hợp lý, không thêm cột). BD `problem_authoring` xác nhận quy ước đặt `code` | Đã đóng |
| Q14 | **Hộp xác nhận rời màn (EVT-21, Sheet 9 NO 16) và hộp xác nhận ghi đè mã khi đổi ngôn ngữ hoặc mô hình (EVT-4, EVT-5) còn cần không, khi mã đã có bản nháp ở trình duyệt?** | **Đã chốt 2026-10-18 (owner), theo đề xuất** (`DEC-2026-1018-usr-open-questions-phase2-5`). Bản nháp theo khoá `(problem_id, language, submission_model)` đã chặn mất mã khi đổi cặp hoặc rời màn cùng trình duyệt. | **Bỏ** hộp xác nhận rời màn (EVT-21, Sheet 9 NO 16) và hộp xác nhận ghi đè mã khi đổi ngôn ngữ hoặc mô hình (EVT-4, EVT-5); giữ nhãn "Đã lưu trên trình duyệt này". Bản dựng không có hộp nào. Đã bỏ khỏi Sheet 3, 5, 6, 8, 9, giữ số EVT/NO (gạch bỏ) thay vì đánh số lại. Tiêu chí ponytail bậc 4. | Đã đóng |
| Q15 | **Ba điểm bản dựng lệch BD ở bố cục Workspace (thanh kéo thứ hai giữa mã và kết quả, xếp dọc dưới `lg`, header vẫn hiện) giữ hay sửa?** | **Đã chốt 2026-10-19 (owner)**: chấp nhận cả ba làm thiết kế chính thức; viết lại mục 4.4 và Sheet 8 EVT-20 cho bản dựng là đặc tả. Ponytail bậc 3 (không có yêu cầu nguồn đòi lưới cố định; thanh kéo dùng lại `shared/ui` `SplitPane`, đã có). | - | Đã đóng |
| Q16 | **Màn phụ kết quả từng testcase (Run Code, Submit) mở lúc nào?** | Bản dựng thêm màn phụ ngày 2026-10-21; BD chưa nói thời điểm mở. | **Đã chốt 2026-10-21 (owner), theo đề xuất** (`DEC-2026-1021-student-open-questions-batch-and-run-results-dialog`): mở ngay khi bấm Run Code hoặc Submit, hiện "Đang chạy..." cho tới khi có kết quả; có thể mở lại bằng nút "Xem chi tiết" ở bảng kết quả. | Đã đóng |
| Q17 | **Khi Submit sai ở testcase ẩn, có cho xem dữ liệu vào, mong đợi, thực tế của testcase đó không?** | Kiểu LeetCode cho xem testcase sai đầu tiên, nhưng làm lộ dần bộ test ẩn. | **Đã chốt 2026-10-21 (owner), theo đề xuất**: **không**, testcase ẩn chỉ hiện Đạt hoặc Không đạt, kể cả khi nộp sai. Tiêu chí ponytail bậc 6 (lộ nội dung testcase ẩn làm hỏng việc chống học thuộc đáp án và phiên bản bộ testcase của F2). | Đã đóng |
| Q18 | **Ví dụ mẫu (`worked_examples`) do endpoint nào trả về?** (thêm 2026-10-22) | Trước đây ví dụ là dòng `testcases` nên đi cùng `ListSampleTestcases`; nay là bảng riêng không dính khoá nào với `testcases`. DB doc không nêu endpoint. Chưa sửa hợp đồng API (thuộc `03-dd/api/problem-bank.md`) | **ĐÃ CHỐT 2026-10-22 (chủ dự án, DEC-2026-1022-db-sync-round2 mục 12, tiêu chí ponytail bậc 4):** ví dụ mẫu trả cùng `GetProblemForWorkspace` (tab Đề bài tải một lần, đã có `statementMd` và `constraintsMd`), không có endpoint riêng; `ListSampleTestcases` chỉ còn tab Testcase và lượt chạy thử. Hợp đồng API vẫn thuộc `03-dd/api/problem-bank.md`, cần ghi `workedExamples` vào response khi viết DD | Đã đóng |
