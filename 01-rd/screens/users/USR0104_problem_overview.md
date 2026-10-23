# RD (Yêu cầu hệ thống mới) — Tổng quan bài toán / `USR0104`

> Mã màn hình: `USR0104`, theo `02-bd/_rules/bd-template-9sheet.md` mục 8 (màn mới lấy số thứ tự kế tiếp trong
> nhóm "Bài tập", sau `USR0103`) [Nguồn: 02-bd/_rules/bd-template-9sheet.md:193].
> Slug chính tắc: `problem_overview`. **Slug này chưa có trong** `01-rd/overview/system_survey.md` mục 7.1
> (bảng khu người học dừng ở dòng `saved_problems`) [Nguồn: 01-rd/overview/system_survey.md:571-584] **và chưa có
> trong bảng mã** `02-bd/_rules/bd-template-9sheet.md` mục 8 [Nguồn: 02-bd/_rules/bd-template-9sheet.md:145-185]; xem
> Câu hỏi mở Q1.
> Phạm vi/Bounded Context: `problem-bank` (F2) là chính; đọc thêm `judge-orchestration` (F4, bài nộp gần đây) và
> `ai-review` (F5, tóm tắt Solution Review). Actor: A1.
> Nguồn sự thật (SoT): quyết định owner `DEC-2026-1020-student-workspace-split-and-password-in-settings`
> [Nguồn: .nexa/control/decision-registry.md:4233-4261]; yêu cầu module F2 [Nguồn: 01-rd/overview/system_survey.md:283-295];
> bản dựng `05-coding/frontend/src/views/users/problem-overview/ui/problem-overview-view.tsx`.
> Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

Trước 2026-10-20 người học bấm một bài ở danh sách là vào thẳng màn soạn mã. Owner tách thành ba trang
[Nguồn: .nexa/control/decision-registry.md:4239-4243]: **trang tổng quan** `/problems/{problemId}` (màn này, chỉ đọc),
trang đích Workspace `/workspace` (`USR0105`) và trang soạn mã `/workspace/{problemId}` (`USR0102`, nội dung giữ nguyên).
Màn này cho người học **đọc đề, xem giới hạn, xem vài lượt nộp gần đây rồi mới quyết định bấm "Giải bài toán"**
để vào trình soạn mã. Màn không có trình soạn mã, không chạy mã, không nộp bài.

File này mô tả **hành vi và UX ở mức yêu cầu**. Hành vi đọc đề, lưu bài và giới hạn theo ngôn ngữ đã có yêu cầu gốc ở
F2-01, F2-10, F2-13 [Nguồn: 01-rd/overview/system_survey.md:283,292,295]; file này chỉ trỏ tới và bổ sung phần đặc thù
của trang tổng quan. Không có prototype tĩnh trong `09-layoutBase/` cho màn này: bố cục theo ảnh mẫu trang đề bài
kiểu online judge do owner gửi 2026-10-20 [Nguồn: 05-coding/frontend/src/views/users/problem-overview/ui/problem-overview-view.tsx:3-6],
nên chưa có dòng prototype nào để trích. Đây là màn **chưa có DD** (DD màn hình viết sau prototype).

---

## 2. Phạm vi

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Đề bài Markdown kèm LaTeX | F2-01 | `01-rd/overview/system_survey.md:283` |
| Giới hạn thời gian và bộ nhớ theo bài, hệ số theo ngôn ngữ | F2-10 | `01-rd/overview/system_survey.md:292` |
| Chỉ lộ số lượng testcase ẩn, không lộ nội dung | F2-08 | `01-rd/overview/system_survey.md:290` |
| Lưu bài (bookmark) | F2-13 | `01-rd/overview/system_survey.md:295` |
| Chỉ bài đã xuất bản mở được | — | `02-bd/screens/users/USR0102_problem_detail.md:803` |
| Tách Workspace thành tổng quan, trang đích, soạn mã | — | `.nexa/control/decision-registry.md:4239-4243` |

Các khối của màn (theo bản dựng):

1. **Đầu trang** — tên bài (chữ lớn), mã bài, nhãn độ khó, nút "Quay về danh sách", đường kẻ phân cách
   [Nguồn: 05-coding/frontend/src/views/users/problem-overview/ui/problem-overview-view.tsx:86-110].
2. **Cột trái, khối đề bài** — nội dung Markdown của đề (phần "Ràng buộc" và "Câu hỏi mở rộng" là mục nằm
   trong Markdown, không phải trường riêng), rồi mục "Ví dụ": mỗi ví dụ có khối Input, khối Output đều có nút
   Copy, và lời giải thích nếu có [Nguồn: .../problem-overview-view.tsx:113-145].
3. **Cột phải (dính khi cuộn)** — nút "Giải bài toán" cỡ lớn, nút Lưu/Bỏ lưu, thẻ "Thông tin", thẻ "Dạng bài" và
   "Ngôn ngữ cho phép", thẻ "Bài nộp gần đây", thẻ tóm tắt Solution Review (chỉ khi có)
   [Nguồn: .../problem-overview-view.tsx:147-290].
4. **Bài không tìm thấy** — thay toàn bộ nội dung bằng thông báo "Không tìm thấy bài toán." kèm liên kết về danh sách
   [Nguồn: .../problem-overview-view.tsx:43-55].

### 2.1 Cấu trúc hệ thống liên quan

| Màn/tài liệu | Vai trò | Bounded Context |
|---|---|---|
| Màn hiện tại | `USR0104` / `problem_overview` | `problem-bank` (chính), `judge-orchestration`, `ai-review` |
| `USR0101` `problem_list` | Lối vào chính: bấm dòng bài, bấm "Bài ngẫu nhiên"; lối ra: "Quay về danh sách" | `problem-bank` |
| `USR0102` `problem_detail` | Lối ra "Giải bài toán" tới `/workspace/{id}` | `problem-bank`, `harness`, `judge-orchestration` |
| `USR0103` `saved_problems` | Lối vào (bấm tên bài) và lối ra (toast "Xem bài đã lưu" tới `/saved`) | `problem-bank` |
| `USR0601` `dashboard` | Lối vào từ khối bài gợi ý | `problem-bank` |
| `USR0105` `workspace_home` | Lối vào qua "Bài ngẫu nhiên" của trang đích Workspace | `problem-bank` |
| `USR0202` `my_submissions`, `USR0201` `submission_result` | Lối vào (tên bài trong lịch sử nộp, trang kết quả); lối ra "Xem tất cả bài nộp" tới `/submissions` | `judge-orchestration` |

Căn cứ lối vào: `problem-list-view.tsx:150,193,258,479`; `saved-problems-view.tsx:150,274`;
`dashboard/ui/blocks/suggested-problems-block.tsx:112,235`; `workspace-home-view.tsx:26-33`;
`my-submissions-view.tsx:105`; `submission-result-view.tsx:151` (đều dưới `05-coding/frontend/src/views/users/`).
"Bài ngẫu nhiên" mở trang tổng quan chứ không mở Workspace, owner đã xác nhận
[Nguồn: .nexa/control/decision-registry.md:4249-4250].

---

## 3. Ngoài phạm vi (Out of Scope)

- Bảng màu, spacing, component cụ thể, bề rộng cột phải, breakpoint — thuộc BD
  (`02-bd/screens/users/USR0104_problem_overview.md`).
- Hợp đồng API (đọc bài, lưu/bỏ lưu, bài nộp gần đây, tóm tắt Solution Review) — thuộc DD
  (`03-dd/api/problem-bank.md` và các module liên quan, chưa viết).
- Trình soạn mã, chạy thử, nộp bài, kết quả từng testcase — thuộc `USR0102` `problem_detail`.
- Báo cáo Solution Review đầy đủ — thuộc `USR0301` `solution_review`. Màn này chỉ hiện hai độ phức tạp.
- **Bình luận, Điểm, Nguồn bài** — có trong ảnh mẫu của owner nhưng **chưa dựng** và chưa có quyết định đưa vào
  phạm vi; ghi ở mục 4 Q5, không coi là yêu cầu.

---

## 4. Tiền đề và ràng buộc

> Tự quyết theo yêu cầu trực tiếp của chủ dự án. Câu nào chưa chốt được ghi rõ "Chưa chốt" và nằm ở BD
> (mục "Câu hỏi mở").

| # | Câu hỏi | Quyết định | Ghi chú |
| :-: | :--- | :--- | :--- |
| Q1 | "Bài ngẫu nhiên" và mọi liên kết "đi xem bài" mở trang nào? | **Chốt:** mọi liên kết có nghĩa "xem bài" trỏ `/problems/{id}`; chỉ liên kết có nghĩa "đi viết mã" mới trỏ `/workspace/{id}`. "Bài ngẫu nhiên" mở trang tổng quan, không mở Workspace | Owner 2026-10-20 [Nguồn: .nexa/control/decision-registry.md:4242-4250] |
| Q2 | Trang tổng quan tách khỏi Workspace hay giữ chung một màn? | **Chốt:** tách; `USR0102` giữ nguyên nội dung | Owner 2026-10-20 [Nguồn: .nexa/control/decision-registry.md:4239-4243] |
| Q3 | Bài không tồn tại hay chưa xuất bản thì báo gì? | **Chốt:** báo "Không tìm thấy bài toán.", **không** báo "không có quyền" — cùng quy tắc đã chốt cho `USR0102` | `DEC-2026-0830-problem-lifecycle-two-states` [Nguồn: 02-bd/screens/users/USR0102_problem_detail.md:803] |
| Q4 | Có cho khách chưa đăng nhập xem trang này không? | **Chốt:** không. Khu Người học bắt buộc đăng nhập, không có trạng thái khách; chưa đăng nhập thì chuyển về `auth` | Owner 2026-10-18, `DEC-2026-1018-usr-open-questions-phase1-scope` [Nguồn: 02-bd/screens/users/_shell.md:136] |
| Q5 | Bình luận, Điểm, Nguồn bài có đưa vào không? | **Chưa chốt**, chưa dựng | Xem BD `USR0104` Câu hỏi mở Q2 |

---

## 5. Danh sách yêu cầu (REQ)

| ID | Yêu cầu | Loại | Nguồn/SoT |
|---|---|---|---|
| REQ-01 | **Cho** tôi mở trang tổng quan của một bài đã xuất bản, **Khi** trang tải xong, **Thì** tôi thấy tên bài, mã bài, độ khó và nội dung đề bài (Markdown kèm công thức); các mục "Ràng buộc" và "Câu hỏi mở rộng" nằm ngay trong đề, không có ô riêng | Chức năng (GWT) | F2-01 [Nguồn: 01-rd/overview/system_survey.md:283]; bản dựng `problem-overview-view.tsx:86-98,114-115` |
| REQ-02 | **Cho** bài có ví dụ mẫu, **Khi** tôi xem mục "Ví dụ", **Thì** mỗi ví dụ hiện khối Input và khối Output đánh số, kèm lời giải thích nếu bài có; **Khi** tôi bấm "Copy" ở một khối, **Thì** nội dung khối đó vào bộ nhớ tạm và tôi nhận thông báo "Đã sao chép."; nếu trình duyệt không cho ghi bộ nhớ tạm thì tôi nhận thông báo lỗi "Không sao chép được, hãy chọn và copy thủ công." | Chức năng (GWT) | Bản dựng `problem-overview-view.tsx:69-79,118-144,296-325`; `messages/vi.json:2943-2947` |
| REQ-03 | **Cho** tôi đã đọc đề, **Khi** tôi bấm "Giải bài toán", **Thì** hệ thống chuyển sang màn soạn mã của đúng bài này (`/workspace/{problemId}`) | Chức năng (GWT) | Quyết định owner [Nguồn: .nexa/control/decision-registry.md:4242-4243]; bản dựng `problem-overview-view.tsx:148-153` |
| REQ-04 | **Cho** tôi chưa lưu bài, **Khi** tôi bấm "Lưu bài", **Thì** bài được đánh dấu đã lưu, nút đổi sang trạng thái "Bỏ lưu" (nổi bật, biểu tượng đặc), và tôi nhận thông báo có lối tắt "Xem bài đã lưu"; **Khi** tôi bấm "Bỏ lưu", **Thì** dấu lưu bị gỡ và tôi nhận thông báo "Đã bỏ lưu bài." Việc lưu là riêng tư theo từng người dùng | Chức năng (GWT) | F2-13 [Nguồn: 01-rd/overview/system_survey.md:295]; bản dựng `problem-overview-view.tsx:57-67,154-167`; `messages/vi.json:352-353,360` |
| REQ-05 | **Cho** tôi đang xem thẻ "Thông tin", **Khi** thẻ hiển thị, **Thì** tôi thấy tỉ lệ AC của bài (hiện dấu gạch ngang nếu chưa có), **số** testcase ẩn (không có nội dung của chúng), và bảng giới hạn thời gian và bộ nhớ đã tính theo từng ngôn ngữ Java, C++, Python | Chức năng (GWT) | F2-08, F2-10 [Nguồn: 01-rd/overview/system_survey.md:290,292]; bản dựng `problem-overview-view.tsx:169-214` |
| REQ-06 | **Cho** bài có chủ đề, **Khi** tôi xem thẻ "Dạng bài", **Thì** tôi thấy mỗi chủ đề một nhãn và danh sách ngôn ngữ được phép nộp | Chức năng (GWT) | F2-02 [Nguồn: 01-rd/overview/system_survey.md:284]; bản dựng `problem-overview-view.tsx:216-233` |
| REQ-07 | **Cho** tôi đã nộp bài này, **Khi** tôi xem thẻ "Bài nộp gần đây", **Thì** tôi thấy tối đa 3 lượt nộp mới nhất của chính tôi (kết quả, ngôn ngữ, thời điểm nộp); chưa nộp lần nào thì thấy câu "Bạn chưa nộp bài này lần nào."; luôn có liên kết "Xem tất cả bài nộp" tới `/submissions` | Chức năng (GWT) | F1-18 [Nguồn: 01-rd/overview/system_survey.md:264]; bản dựng `problem-overview-view.tsx:27,81,235-267` |
| REQ-08 | **Cho** tôi đã có báo cáo Solution Review cho bài này, **Khi** tôi xem cột phải, **Thì** tôi thấy thẻ tóm tắt chỉ gồm độ phức tạp thời gian và độ phức tạp bộ nhớ; **chưa có báo cáo** thì thẻ này **không xuất hiện**, không có chỗ trống, không có thông báo lỗi | Chức năng (GWT) | F5-01, F5-02 [Nguồn: 01-rd/overview/system_survey.md:352-353]; bản dựng `problem-overview-view.tsx:269-289` |
| REQ-09 | **Cho** hệ thống AI (`ai-review`) đang lỗi hoặc hết hạn mức, **Khi** tôi mở trang, **Thì** mọi phần còn lại của trang vẫn dùng được; chỉ thẻ Solution Review vắng mặt, không báo lỗi | Chức năng (GWT, suy giảm êm) | Tiền lệ `USR0102` [Nguồn: 02-bd/screens/users/USR0102_problem_detail.md:568,608] |
| REQ-10 | **Cho** tôi mở một đường dẫn có `problemId` không tồn tại (hoặc bài chưa xuất bản, đã ẩn), **Khi** trang tải, **Thì** tôi thấy "Không tìm thấy bài toán." và liên kết "Quay về danh sách bài toán"; **không** có câu nào gợi ý rằng bài tồn tại nhưng tôi thiếu quyền | Chức năng (GWT) | `DEC-2026-0830-problem-lifecycle-two-states` [Nguồn: 02-bd/screens/users/USR0102_problem_detail.md:803]; bản dựng `problem-overview-view.tsx:43-55` |
| REQ-11 | **Cho** tôi muốn quay lại danh sách, **Khi** tôi bấm "Quay về danh sách", **Thì** tôi về `/problems` | Chức năng (GWT) | Bản dựng `problem-overview-view.tsx:99-109` |
| REQ-12 | **Cho** tôi mở trang này bằng "Bài ngẫu nhiên", bấm một bài ở danh sách, bài đã lưu, bài gợi ý ở tổng quan, lịch sử nộp bài hoặc trang kết quả nộp bài, **Khi** liên kết kích hoạt, **Thì** tôi vào trang tổng quan `/problems/{id}`, **không** vào thẳng màn soạn mã | Chức năng (GWT) | Quyết định owner [Nguồn: .nexa/control/decision-registry.md:4242-4250] |
| REQ-13 | Trang **không** được hiển thị input, output mong đợi hay diff của testcase ẩn dưới mọi hình thức; chỉ số lượng testcase ẩn được phép | Bảo mật nghiệp vụ | F2-08 [Nguồn: 01-rd/overview/system_survey.md:290]; `02-bd/screens/users/USR0102_problem_detail.md:46` |

Chưa có yêu cầu cho Bình luận, Điểm, Nguồn bài (xem mục 3 và mục 4 Q5).

---

## 6. Yêu cầu dữ liệu

### 6.1 Dữ liệu tham chiếu

Bản dựng đọc một đối tượng `ProblemDetail` từ dữ liệu mẫu qua `fetchProblemDetail`
[Nguồn: 05-coding/frontend/src/entities/problem/api/__mock__/problem-mocks.ts:423-427; 05-coding/frontend/src/entities/problem/model/types.ts:140-164];
nguồn dữ liệu thật của từng trường (bảng, cột) thuộc BD `USR0104` Sheet 5 và 7. Chi tiết bảng, cột, kiểu dữ liệu
thuộc `02-bd/database/problem-bank.md`; tài liệu này không tự suy luận dữ liệu chưa được chốt.

### 6.2 Luồng dữ liệu

Hợp đồng API, request/response và mã lỗi thuộc DD. Luồng ở mức yêu cầu thể hiện bởi các hành vi ở mục 5.

---

## 7. RACI và danh sách bàn giao

| Bàn giao | Trạng thái |
|---|---|
| RD màn hình | Có tài liệu này |
| BD màn hình | `02-bd/screens/users/USR0104_problem_overview.md` (V0.1, 2026/10/20) |
| DD/API liên quan | Chưa xác định — không tự suy luận |

---

## 8. Kế hoạch và quy mô

| Chỉ số | Giá trị |
|---|---|
| Quy ước hành vi | 13 yêu cầu ở mục 5 |
| Lịch, nhân sự, công sức | Chưa xác định — không tự suy luận. |

---

## 9. Tham chiếu

| Loại | Tham chiếu |
| :--- | :--- |
| Quyết định | `.nexa/control/decision-registry.md:4233-4261` — `DEC-2026-1020-student-workspace-split-and-password-in-settings`. |
| Yêu cầu module | `01-rd/overview/system_survey.md:283-295` — F2-01, F2-08, F2-10, F2-13. |
| Khảo sát hệ thống | `01-rd/overview/system_survey.md:569-584` — mục 7.1 (chưa có slug `problem_overview`). |
| Màn lân cận | `02-bd/screens/users/USR0102_problem_detail.md`, `02-bd/screens/users/USR0101_problem_list.md`. |
| Bản dựng | `05-coding/frontend/src/views/users/problem-overview/ui/problem-overview-view.tsx`. |
