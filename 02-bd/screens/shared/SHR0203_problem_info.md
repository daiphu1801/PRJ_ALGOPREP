# Tài liệu thiết kế cơ bản (BD) — Chi tiết bài tập, chỉ đọc (`SHR0203`)

## Phương châm tài liệu [Nội bộ — không cần khách duyệt]

- Viết theo **mẫu 9 sheet**. Bộ thẻ đóng, quy ước ID, ký hiệu chuẩn, ranh giới với tài liệu khác và checklist kiểm toán
  nằm ở `02-bd/_rules/bd-template-9sheet.md` — file này không chép lại.
- Mã màn `SHR0203` là mã kế tiếp trong nhóm `SHR02` (Nội dung bài tập) [Nguồn: 02-bd/_rules/bd-template-9sheet.md:167-169].
  Bảng mã ở mục 8 của file quy ước và slug `problem_info` ở `01-rd/overview/system_survey.md` mục 7.0 **đã có**
  (`01-rd/overview/system_survey.md:540`); lúc tạo file chưa có, xem Câu hỏi mở Q1 (đã đóng); RD ghi nhận slug mới ở
  [Nguồn: 01-rd/screens/shared/SHR0203_problem_info.md:4].
- Màn **chỉ đọc**, **màn con của `SHR0201`** và là **màn anh em của `SHR0202`**: cùng một bản ghi, khác ở chỗ không có trường nhập
  nào [Nguồn: 01-rd/screens/shared/SHR0203_problem_info.md:13-19]. Không có popup, không có sự kiện nào ngoài điều hướng, không
  có kiểm nhập liệu, **không có endpoint riêng** — dùng lại DTO và cổng đọc của `SHR0202`
  [Nguồn: 01-rd/screens/shared/SHR0203_problem_info.md:59-60].
- Bounded Context sở hữu: **`problem-bank`** (F2) [Nguồn: 01-rd/screens/shared/SHR0203_problem_info.md:5].
- Bản dựng prototype: `05-coding/frontend/src/views/shared/problem-info/ui/problem-info-view.tsx`; route khu Admin
  `05-coding/frontend/src/app/(admin)/admin/problems/[problemId]/page.tsx:1-7`, khu Giảng viên (dựng 2026-10-03)
  `05-coding/frontend/src/app/(instructor)/instructor/problems/[problemId]/page.tsx:1-7`; cả hai truyền `problemId` và `basePath` vào view. Prototype là mock, **không có API thật**
  (`problem-info-view.tsx:10-13` — `useProblemDraft(id)` đọc bản lưu lần cuối, id chưa lưu hiện bài mẫu; nợ ghi ở
  `06-plan/PROTOTYPE_DEBT.md` mục 17.1). Endpoint chỉ mô tả bằng tên nghiệp vụ; hợp đồng thuộc `03-dd/api/F2_problem-bank.md`.

> Đọc cùng `01-rd/screens/shared/SHR0203_problem_info.md` (hành vi ở mức yêu cầu, không lặp lại ở đây), `02-bd/screens/shared/SHR0202_problem_authoring.md`
> (nơi định nghĩa DTO), `02-bd/architecture/F2_problem-bank.md`, `02-bd/database/F2_problem-bank.md`.
>
> **Không thiết kế** (ngoài phạm vi, chưa có trong `ProblemDraft`, không bịa số): tỷ lệ AC, số lượt nộp, người soạn, ngày cập nhật
> gần nhất — [Nguồn: 01-rd/screens/shared/SHR0203_problem_info.md:48; 06-plan/PROTOTYPE_DEBT.md mục 17.1].
> **Không thiết kế** lịch sử phiên bản bộ testcase (F2-09) — [Nguồn: 01-rd/screens/shared/SHR0203_problem_info.md:49].
> **Có thiết kế (cập nhật 2026-10-03)**: đề bài hiển thị Markdown + LaTeX đã render (REQ-6) qua thành phần dùng chung `MarkdownPreview` [Nguồn: 01-rd/screens/shared/SHR0203_problem_info.md:31,74; 05-coding/frontend/src/shared/ui/data/markdown-preview.tsx:20-41; 05-coding/frontend/src/views/shared/problem-info/ui/problem-info-view.tsx:99]. Mục "không thiết kế render Markdown/LaTeX" của bản V1.0 bị đảo.

---

## Sheet 1. Bìa

| Mục | Giá trị |
| :--- | :--- |
| Hệ thống | AlgoPrep |
| Phân hệ | `problem-bank` (F2) |
| Công đoạn | BD — Thiết kế cơ bản |
| Phân loại | Màn hình |
| Tên màn | Chi tiết bài tập (chỉ đọc) |
| Mã màn hình | `SHR0203` |
| Tên vật lý (slug) | `problem_info` |
| Trục tài liệu | Màn hình (`02-bd/screens/`) |
| Actor | A2 (`INSTRUCTOR`) và A3 (`ADMIN`) — dùng chung một view; dựng cho cả hai khu (khu A2 từ 2026-10-03) |
| Phiên bản | V1.11 |
| Người tạo | Nhóm phát triển AlgoPrep |
| Ngày tạo | 2026/10/02 |
| Người cập nhật | Nhóm phát triển AlgoPrep |
| Ngày cập nhật | 2026/10/22 |

---

## Sheet 2. Lịch sử sửa đổi

| Ver | Sheet bị sửa | Nội dung sửa | Ngày | Người sửa |
| :--- | :--- | :--- | :--- | :--- |
| V1.0 | Toàn bộ | Tạo mới theo mẫu 9 sheet, ghi lại bản dựng prototype 2026-10-02 theo `DEC-2026-1002-split-detail-and-edit-pages` (tách trang admin thành chi tiết chỉ đọc và form sửa). Phát hiện 4 khoảng trống nguồn dữ liệu so với `SHR0202` (kết quả chạy đáp án mẫu không lưu DB, ma trận độ phủ theo loại ca, giới hạn stack, số cờ AI), ghi thành câu hỏi mở Q2-Q5 thay vì mặc định có cột | 2026/10/02 | AI |
| V1.1 | Sheet 3, 4, 5; Câu hỏi mở; trích dẫn | (1) Khu A2 đã dựng (2026-10-03): `/instructor/problems/[problemId]` là `problem_info`, cùng view qua prop bắt buộc `basePath` [Nguồn: 05-coding/frontend/src/views/shared/problem-info/ui/problem-info-view.tsx:41-46, 65, 77]; bỏ các câu "khu A2 chưa tách". (2) Đề bài (`statement_md`) hiển thị Markdown + LaTeX đã render qua `MarkdownPreview` (react-markdown, remark-gfm, remark-math, rehype-katex; không phân tích HTML thô) thay cho văn bản thô; đổi Sheet 5 Khu vực C NO 1, thêm REQ-6 của RD. (3) Làm mới số dòng trích dẫn vào mã, RD `SHR0203`, BD `SHR0201`/`SHR0202`, `bd-template-9sheet.md`. (4) Theo quy ước chủ dự án 2026-10-03: mockup `09-layoutBase` chỉ tham chiếu, mã là hiện trạng | 2026/10/03 | AI |
| V1.2 | Sheet 4 (4.4, 4.5), Sheet 5 khu vực B | Chủ dự án yêu cầu 2026-10-03 (chưa duyệt hình): bỏ hàng 4 thẻ số liệu phía trên, chuyển các số liệu vào thẻ Thuộc tính ở cột phải để đề bài nằm đầu khung nhìn; header và các thẻ còn lại giữ như V1.1. Hành vi và dữ liệu không đổi. Làm mới số dòng trích dẫn ở mục 4.4 | 2026/10/03 | AI |
| V1.3 | Sheet 4 (4.5) | Đọc dữ liệu đi qua `useProblemDraft` (khuôn `queries.ts`); trang có trạng thái đang tải và lỗi tải | 2026/10/03 | AI |
| V1.4 | Câu hỏi mở | Đóng Q8: A2 mở bài của người khác nhận 404 như mã không tồn tại; prototype đã dựng nhánh "không tìm thấy" | 2026/10/03 | AI |
| V1.5 | Sheet 4 (4.2, 4.3, 4.4), Sheet 5, 6 (Khu vực C) | Thêm khối "Đặc tả" chỉ đọc theo `SHR0202` Q20 (chủ dự án đồng ý 2026-10-03): chữ ký hàm theo từng ngôn ngữ suy từ chữ ký chung, định dạng stdin/stdout, chiến lược so khớp; bài có kiểu ngoài lược đồ nêu "chỉ hỗ trợ Standard I/O". Thêm mục 9 đến 12 vào Khu vực C, hai bảng `problem_specs`, `function_signatures`, DTO `FunctionSignatureDto`, `ProblemSpecDto`. RD `SHR0203` thêm REQ-7 | 2026/10/03 | AI |
| V1.6 | Sheet 5 (Khu vực D NO 1), Sheet 7 (7.1 NO 4, 7.2), Sheet 4 (4.3) | Đã chốt 2026-10-03 (`DEC-2026-1001-admin-configurable-settings` mục 7): nhãn badge "Độ khó" đọc từ bảng `problem_levels` (do ADMIN quản lý, không còn ba mức cố định); `problems.difficulty` ENUM đổi thành `level_id`; DTO `difficulty` của `ProblemAuthoringDetailDto` đổi thành `levelId`, nhãn lấy qua `ListProblemLevels` (cùng mẫu với chủ đề); thêm bảng `problem_levels` vào 4.3 và 7.2. Màu badge do giao diện suy ra (ba mức seed giữ màu cũ, mức mới trung tính). Màn vẫn chỉ đọc | 2026/10/03 | AI |
| V1.7 | Sheet 4 (4.4, 4.5) | Phần thân trang chỉ đọc được phóng nhẹ theo yêu cầu của owner (2026-10-05, **chưa duyệt hình**, không đổi hành vi): `MarkdownPreview` dùng chung có thêm prop `size`, "base" (mặc định, 13.5 px, không đổi, màn soạn đề vẫn dùng) và "lg" (15 px, giãn dòng 1.75, tiêu đề xl/lg/base, mã 14 px); đề bài dùng `size="lg"`, ràng buộc dữ liệu 12.5 lên 14 px, các dòng đầu vào/đầu ra/giải thích của ví dụ mẫu 12.5 lên 14 px. Thẻ thuộc tính ở cột hẹp không đổi | 2026/10/05 | Nhóm phát triển AlgoPrep |
| V1.8 | Sheet 3, 5, 9 (NO 2), Câu hỏi mở (Q8) | Chủ dự án duyệt 2026-10-11 (`DEC-2026-1011-a2-read-published-problems-of-others`): A2 xem chỉ đọc bài `Đã xuất bản` của người khác; bản nháp của người khác vẫn 404; sửa/xoá chỉ bài của mình. Điều chỉnh Q8 và Sheet 9 NO 2; mã đang sửa song song | 2026/10/11 | AI |
| V1.9 | Sheet 6, 8, Câu hỏi mở | Theo báo cáo `06-plan/reports/261014-1054-admin-ux-review/` (B9): nút "Xem như người học" nay mở `/preview` ở tab mới, cùng đích với icon con mắt ở màn biên soạn theo `SHR0202` Q6 (đã chốt 2026-10-03). Q6 của màn này **đề xuất đóng** theo `SHR0202` Q6, chờ chủ dự án xác nhận | 2026-10-14 | Claude |
| V1.10 | Sheet 4 (4.3), 5 (Khu B, C), 7 (7.1), Câu hỏi mở | Sửa 2026-10-22 đồng bộ DB, `DEC-2026-1022-problem-bank-schema-sync`: (1) Q3 đóng: `testcases.approved` thay `is_ai_generated_draft` (mục 3); (2) Q2 đóng một phần: đáp án mẫu ở bảng `problem_reference_solutions`, kết quả chạy không lưu nên ô `{passed}/{total}` mất nguồn (đề xuất bỏ, chờ chủ dự án) (mục 2); (3) Q4 đóng một phần: `testcases.category` đã có cột, `stack_limit_mb` vẫn không có (mục 3); (4) ràng buộc dữ liệu đọc `problems.constraints_md` (mục 1); (5) ví dụ mẫu đọc bảng `worked_examples` thay cho cờ `testcases.is_worked_example` (mục 4); (6) khối Đặc tả đọc một bảng `problem_specs`, bảng `function_signatures` đã gộp (mục 5); (7) 4.3 đổi dòng "chưa tồn tại" thành `problem_reference_solutions` và thêm `worked_examples`. **Không đổi:** Q5 và hàng công tắc AI (PB-07 còn mở), `allowHiddenHint`. Giao diện không đổi | 2026/10/22 | Claude |
| V1.11 | Sheet 4 (4.1, 4.4), 5 (Khu B NO 3), 6 (Khu B NO 3), 7 (7.1 NO 11), Câu hỏi mở (Q2) | Sửa 2026-10-22 (đồng bộ DB vòng 2, DEC-2026-1022-db-sync-round2 mục 9): bỏ ô `{passed}/{total}` của đáp án mẫu (Khu B NO 3), chỉ giữ ngôn ngữ đáp án; bỏ `solutionCheck` khỏi Sheet 7.1 NO 11; đóng phần còn mở của Q2. Giữ nguyên các phần còn lại (Q4 stack, Q5 công tắc AI, Q6, Q7) | 2026/10/22 | Claude |
| V1.12 | Sheet 4 (4.3), 5 (Khu D NO 7-8), 7 (7.1 NO 10), Câu hỏi mở (Q5) | Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4), PB-07: (1) Q5 đóng: `allowHiddenHint` đã bỏ khỏi `AiAuthoringContextDto` của `SHR0202` (còn `briefText`, `noFullCode`, `questionOnly`); (2) nguồn dữ liệu khối AI là ba cột `problems.ai_brief`, `no_full_code`, `socratic_only` (`02-bd/database/F2_problem-bank.md` mục 1.1), không có bảng riêng. Giao diện không đổi | 2026/10/22 | Claude |

---

## Sheet 3. Sơ đồ chuyển màn

> [Nội bộ] Quy ước vẽ: bố trí từ trái sang phải. Màn gọi tới tô tím, màn hiện tại tô xanh nhạt.

### 3.1 Danh sách chuyển màn

#### Quản lý bài tập → Chi tiết bài tập

[Điều kiện mở] Bấm tiêu đề bài toán trong bảng danh sách ở `problem_management`
[Nguồn: 05-coding/frontend/src/views/shared/problem-management/ui/problem-management-view.tsx:167-169; 01-rd/screens/shared/SHR0203_problem_info.md:72].

[Chế độ mở] Chế độ chỉ đọc. Route `{basePath}/[problemId]`: khu A3 `/admin/problems/[problemId]`, khu A2 `/instructor/problems/[problemId]` (dựng 2026-10-03)
[Nguồn: 01-rd/screens/shared/SHR0203_problem_info.md:55-58; 05-coding/frontend/src/app/(instructor)/instructor/problems/[problemId]/page.tsx:4-6].

[Thông tin truyền] `problemId` — mã bài bỏ dấu `#` (ví dụ `121`)
[Nguồn: 05-coding/frontend/src/app/(admin)/admin/problems/[problemId]/page.tsx:4-6; problem-info-view.tsx:42-43].

[Giá trị trả về] Không có.

[Khi thành công] Tải chi tiết bài toán, hiển thị thanh đầu trang, hàng số liệu, cột nội dung và cột thuộc tính.

[Khi huỷ] Không có.

#### Chi tiết bài tập → Quản lý bài tập

[Điều kiện mở] Bấm nút quay lại ở đầu trang [Nguồn: problem-info-view.tsx:65].

[Chế độ mở] Không có.

[Thông tin truyền] Không có.

[Giá trị trả về] Không có.

[Khi thành công] Điều hướng về `problem_management` (`href={basePath}`, tức `/admin/problems` ở khu A3 hoặc `/instructor/problems` ở khu A2) [Nguồn: problem-info-view.tsx:65]. Màn này không có thay đổi chưa lưu nên không hỏi xác nhận.

[Khi huỷ] Không có.

#### Chi tiết bài tập → Biên soạn bài tập (chế độ sửa)

[Điều kiện mở] Bấm nút "Sửa bài" ở thanh đầu trang [Nguồn: problem-info-view.tsx:76-78; 01-rd/screens/shared/SHR0203_problem_info.md:71].

[Chế độ mở] Chế độ sửa, route `{basePath}/[problemId]/edit` (khu A3 `/admin/problems/[problemId]/edit`, khu A2 `/instructor/problems/[problemId]/edit`)
[Nguồn: 05-coding/frontend/src/app/(admin)/admin/problems/[problemId]/edit/page.tsx:1-5; 05-coding/frontend/src/app/(instructor)/instructor/problems/[problemId]/edit/page.tsx:1-5; problem-info-view.tsx:77].

[Thông tin truyền] `problemId` của bài đang xem.

[Giá trị trả về] Không có.

[Khi thành công] Mở `problem_authoring` (`SHR0202`) đã nạp sẵn nội dung bài toán đó.

[Khi huỷ] Không có.

### 3.2 Sơ đồ

```mermaid
flowchart LR
    parent["Quản lý bài tập<br/>problem_management"] -->|"bấm tiêu đề bài"| main["Chi tiết bài tập<br/>problem_info"]
    main -->|"nút quay lại"| parent
    main -->|"Sửa bài"| edit["Biên soạn bài tập<br/>problem_authoring (/edit)"]

    classDef source fill:#F3E5F5,stroke:#9C5CC4,color:#000
    classDef screen fill:#E3F2FD,stroke:#3B82F6,color:#000

    class parent,edit source
    class main screen
```

[Nguồn: problem-info-view.tsx:65,76-78; 01-rd/screens/shared/SHR0203_problem_info.md:18-19]

Nút "Xem như người học" có hiện trên màn nhưng **chưa gắn hành động** (prototype `problem-info-view.tsx:73-75`; nợ
`06-plan/PROTOTYPE_DEBT.md` mục 17.1) nên không vẽ thành chuyển màn; xem Câu hỏi mở Q6.

---

## Sheet 4. Bố cục màn hình

### 4.1 Tổng quan màn

[Mục đích màn] Cho A2/A3 xem trọn một bài toán đã soạn (đề, giới hạn, ví dụ, tóm tắt testcase, ngôn ngữ đáp án mẫu, thuộc tính,
cấu hình AI) mà không có nguy cơ sửa nhầm; mọi thao tác sửa đi qua nút "Sửa bài" sang `problem_authoring`
[Nguồn: 01-rd/screens/shared/SHR0203_problem_info.md:13-19].

[Luồng nghiệp vụ chính]

1. **Mở màn**: từ `problem_management` bấm tiêu đề bài; kiểm quyền, tải chi tiết bài toán.
2. **Đọc**: xem hàng số liệu (testcase đã duyệt, công khai, ngôn ngữ đáp án mẫu, giới hạn thời gian), cột nội dung (đề, ràng buộc,
   ví dụ, độ phủ, đáp án mẫu thu gọn), cột thuộc tính (độ khó, trạng thái, giới hạn, cấu hình AI).
3. **Rời màn**: quay lại danh sách, hoặc bấm "Sửa bài" sang `problem_authoring`.

[Người dùng] A2 (`INSTRUCTOR`, bài của mình; bài đã xuất bản của người khác chỉ xem, 2026-10-11) và A3 (`ADMIN`, toàn kho), có Function `PROBLEM_AUTHORING`
[Nguồn: 01-rd/screens/shared/SHR0203_problem_info.md:37; 02-bd/security/F2_problem-bank.md:5-10].

[Tệp liên quan] Không có.

[Phạm vi]
- Không có trường nhập, không có thao tác ghi dữ liệu nào [Nguồn: 01-rd/screens/shared/SHR0203_problem_info.md:18].
- Không thiết kế tỷ lệ AC, số lượt nộp, người soạn, ngày cập nhật — xem đầu file, `06-plan/PROTOTYPE_DEBT.md` mục 17.
- Mã đáp án mẫu chỉ A2/A3 thấy, mặc định thu gọn, không bao giờ đưa vào prompt AI hay trả cho học viên (F2-18)
  [Nguồn: 01-rd/screens/shared/SHR0203_problem_info.md:39,73].

[Quyền sử dụng]
- Xem: được, khi có `PROBLEM_AUTHORING:READ` (A2: bài do mình tạo, dựa trên `problems.author_id`, **cộng** bài `Đã xuất bản` của người khác ở chế độ chỉ đọc, ngoại lệ chốt 2026-10-11 theo `DEC-2026-1011-a2-read-published-problems-of-others`; bản nháp của người khác trả 404; A3: toàn bộ)
  [Nguồn: 02-bd/screens/shared/SHR0201_problem_management.md:267 (Xem), 263 (phạm vi tác giả cho A2)].
- Thêm, Sửa, Xoá: không có trên màn này.

[Số bản ghi tối đa] Một bài toán mỗi lần mở. Ví dụ mẫu: không giới hạn; ma trận độ phủ: một ô cho mỗi loại ca trong danh mục
`TESTCASE_CATEGORIES` (prototype dùng hằng số này, `problem-info-view.tsx:57-60`); công tắc AI: 2 dòng trong prototype
(`AI_GUARD_KEYS`, `problem-info-view.tsx:185`).

[Nguồn: 01-rd/screens/shared/SHR0203_problem_info.md:26-39]

### 4.2 DTO liên quan

Dùng lại, **không định nghĩa lại**, từ `02-bd/screens/shared/SHR0202_problem_authoring.md` mục 4.2 và Sheet 7.1:

- `ProblemAuthoringDetailDto`
- `WorkedExampleDto`
- `TestcaseDto`
- `AiAuthoringContextDto`
- `FunctionSignatureDto`, `ProblemSpecDto` (đặc tả, thêm 2026-10-03)

`[SoT: Suy luận]` — `GetProblemForAuthoring` là cổng đọc chi tiết duy nhất của `problem-bank` nên màn đọc dùng chung; RD cũng nêu "cùng cổng
đọc chi tiết bài" nhưng ghi đây là suy luận thuộc DD [Nguồn: 01-rd/screens/shared/SHR0203_problem_info.md:59-60]. Nếu DD tách DTO đọc riêng
thì cập nhật mục này, không phải thêm trường ở đây.

### 4.3 Bảng dữ liệu liên quan (8)

| NO | Bảng | Ghi chú |
| --: | :--- | :--- |
| 1 | `problems` | [Nguồn: 02-bd/database/F2_problem-bank.md mục 1.1] |
| 2 | `problem_topics` / `topics` | [Nguồn: 02-bd/database/F2_problem-bank.md mục 1.2] — dòng phụ tiêu đề hiện tên chủ đề (`problemInfo.subtitle`) |
| 3 | `testcases` | Chỉ đọc các cột `visibility`, `approved`, `category` để đếm [Nguồn: 02-bd/database/F2_problem-bank.md:136-158 (mục 1.6)] |
| 4 | `problems.ai_brief`, `problems.no_full_code`, `problems.socratic_only` | Đã có từ 2026-10-22 (`DEC-2026-1022-db-sync-round4`, PB-07), `02-bd/database/F2_problem-bank.md` mục 1.1; thay cho phương án bảng `problem_ai_authoring_context` |
| 5 | `problem_reference_solutions` | Mã và ngôn ngữ đáp án mẫu, một dòng mỗi bài [Nguồn: 02-bd/database/F2_problem-bank.md:220-232 (mục 1.11)]; thay cho dòng "chưa tồn tại" cũ (Q2, đóng 2026-10-22) |
| 6 | `problem_specs` | Khối Đặc tả (thêm 2026-10-03); từ 2026-10-22 gồm cả chữ ký hàm [Nguồn: 02-bd/database/F2_problem-bank.md:108-128 (mục 1.4)] |
| 7 | `worked_examples` | Ví dụ mẫu kèm giải thích, bảng riêng [Nguồn: 02-bd/database/F2_problem-bank.md:234-247 (mục 1.12)] |
| 8 | `problem_levels` | [Nguồn: 02-bd/database/F2_problem-bank.md mục 1.2a] — nhãn badge "Độ khó" (thêm 2026-10-03) |

Dòng 4 là khoảng trống schema duy nhất còn lại ở `SHR0202`, màn này chỉ đọc tiếp nên không tự quyết nơi lưu. Không dùng `tags`/`problem_tags` và `problem_stats` — màn này không hiển thị thẻ hay số liệu bài.

**Thêm 2026-10-03 (`SHR0202` Q20):** màn đọc thêm `problem_specs` (định dạng stdin/stdout, chiến lược so khớp, epsilon) và chữ ký hàm chung, ghi đè tên (`02-bd/database/F2_problem-bank.md:108-128`, mục 1.4) để hiển thị khối đặc tả chỉ đọc; **sửa 2026-10-22:** bảng `function_signatures` đã gộp vào `problem_specs` (`DEC-2026-1022-problem-bank-schema-sync` mục 5, `02-bd/database/F2_problem-bank.md:130-134`), nên màn chỉ đọc một bảng cho khối này; không thêm khoảng trống mới.

### 4.4 Vùng bố cục

**Chưa có prototype tĩnh trong `09-layoutBase/`** (`[Đợi nextjs]`): bố cục do bản dựng 2026-10-02 chọn, theo khuôn màn soạn
[Nguồn: 01-rd/screens/shared/SHR0203_problem_info.md:21-22; problem-info-view.tsx:3-6]. Chủ dự án chưa duyệt hình ảnh. Bảng dưới bám dòng của file mã.

| Vùng | Vị trí trong mã | Nội dung |
| :--- | :--- | :--- |
| Thanh đầu trang | `problem-info-view.tsx:66-83` | Nút quay lại, tiêu đề bài, dòng phụ "Mã #{code} · chủ đề {topic}", nút "Xem như người học", nút "Sửa bài" |
| Cột nội dung (rộng) | `:86-142` | 5 thẻ: Đề bài + ràng buộc, Ví dụ mẫu, **Đặc tả** (thêm 2026-10-03: dòng chữ ký từng ngôn ngữ, định dạng stdin/stdout, chiến lược so khớp), Ma trận độ phủ, Đáp án mẫu (thu gọn); Đề bài nằm ngay dưới thanh đầu trang |
| Cột thuộc tính (hẹp, dính) | `:144-207` | Thẻ Thuộc tính gồm độ khó, trạng thái, **các số liệu của khu vực B** (Testcase đã duyệt kèm số chờ duyệt, Testcase công khai, Ngôn ngữ đáp án mẫu) và 4 giới hạn; thẻ Chỉ dẫn cho trợ lý AI (công tắc + ngữ cảnh) |

Từ V1.2 không còn hàng 4 thẻ số liệu phía trên cột nội dung: các số liệu chuyển vào thẻ Thuộc tính để đề bài nằm đầu khung nhìn (chủ dự án yêu cầu 2026-10-03, chưa duyệt hình). Giới hạn thời gian không còn thẻ riêng vì đã nằm trong 4 giới hạn. Các khu vực Sheet 5 vẫn chia theo nhóm dữ liệu, không đổi.

Lưới 2 cột `minmax(0,1fr) 300px` từ breakpoint `xl`, xếp chồng trên màn hẹp (`:96`). Khung điều hướng Admin dùng chung, không mô tả lại.

### 4.5 Cấu trúc slice FSD [Nội bộ]

| Khối | Slice | Căn cứ |
| :--- | :--- | :--- |
| Trang | `views/shared/problem-info` (`index.ts` xuất `ProblemInfoView`; `ui/problem-info-view.tsx`) | Có trong mã, 05-coding/frontend/src/views/shared/problem-info/index.ts:1 |
| Route | `app/(admin)/admin/problems/[problemId]/page.tsx`, truyền `problemId` và `basePath="/admin/problems"` | Có trong mã |
| Dữ liệu | `entities/problem` (`useProblemDraft`, `useProblemTopics`, `problemTopicLabel`, `AI_GUARD_KEYS`, `TESTCASE_CATEGORIES`) | `problem-info-view.tsx:19-28` |
| Khối giao diện | `shared/ui` (`PageHeader`, `Card`, `Badge`, `Button`, `IconAction`, `MarkdownPreview`) — không tạo widget riêng; `StatCard` không còn dùng ở màn này | `problem-info-view.tsx:31` |
| Khu A2 | Cùng view, mount ở `app/(instructor)/instructor/problems/[problemId]/page.tsx` truyền `basePath="/instructor/problems"` (dựng 2026-10-03) | `05-coding/frontend/src/app/(instructor)/instructor/problems/[problemId]/page.tsx:6` |
| Render đề bài | `shared/ui/data/markdown-preview.tsx` (`MarkdownPreview`; từ 2026-10-05 có prop `size` "base" hoặc "lg", màn này dùng "lg": 15 px, giãn dòng 1.75, mã 14 px, chưa duyệt hình; `05-coding/frontend/src/shared/ui/data/markdown-preview.tsx:17-18, 27-36`; dùng ở `05-coding/frontend/src/views/shared/problem-info/ui/problem-info-view.tsx:116`): react-markdown + remark-gfm + remark-math + rehype-katex; không bật `rehype-raw` nên HTML thô hiển thị như văn bản | 05-coding/frontend/src/shared/ui/data/markdown-preview.tsx:1-41 |
| Cỡ chữ thân trang (2026-10-05, chưa duyệt) | `05-coding/frontend/src/views/shared/problem-info/ui/problem-info-view.tsx:116, 120, 133` | Đề bài `MarkdownPreview size="lg"` (15 px); ràng buộc dữ liệu `text-[14px]` (trước 12.5 px); khối ví dụ mẫu `dl` `text-[14px]` (trước 12.5 px). Các khối khác (thẻ thuộc tính, đặc tả chữ ký, nhãn) giữ cỡ cũ |

Slug `problem_info` ↔ slice `views/shared/problem-info` ↔ `01-rd/screens/shared/SHR0203_problem_info.md`: thống nhất.

---

## Sheet 5. Danh sách item màn hình

> Quy ước ID, bộ thẻ và giá trị chuẩn: `02-bd/_rules/bd-template-9sheet.md` mục 2, 3, 4. Mọi item `I/O = O` trừ nút và liên kết.
> Nhãn lấy từ `messages/vi.json` khoá `problemInfo` (dòng 1175-1189) và khoá `problemAuthoring` dùng chung.

### Khu vực A — Thanh đầu trang

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Thanh đầu trang | | | | | | | | | | | | | |
| | 1 | Nút quay lại | `problemInfo.header.btnBack` | - | - | Link | - | - | - | I | - | Icon mũi tên, tooltip "Quay lại danh sách" | Về `problem_management`<br>[Nguồn giá trị] Nhãn tĩnh i18n `problemInfo.back` (`problem-info-view.tsx:65`)<br>[EVT liên quan] EVT-1 |
| | 2 | Tiêu đề bài | `problemInfo.header.title` | `problems` | `title` | Label | String | 255 | - | O | - | - | [Nguồn giá trị] Cột `title`, DTO `ProblemAuthoringDetailDto.title` (`problem-info-view.tsx:66`)<br>[EVT liên quan] - |
| | 3 | Mã và chủ đề | `problemInfo.header.subtitle` | `problems`, `topics` | `code`, `topics.name` | Label | String | - | - | O | - | `Mã #{code} · chủ đề {topic}` | [Công thức] `code` = tham số route `problemId` (prototype lấy từ route, không từ dữ liệu, `:67-70`); `topic` = tên chủ đề của bài (`problemTopicLabel`)<br>[EVT liên quan] - |
| | 4 | Xem như người học | `problemInfo.header.btnPreview` | - | - | Button | - | - | - | I | - | - | Hiện nhưng **chưa gắn hành động** (xem Q6)<br>[Nguồn giá trị] Nhãn tĩnh i18n `problemAuthoring.previewAsLearner` (`:73-75`)<br>[EVT liên quan] EVT-3 |
| | 5 | Sửa bài | `problemInfo.header.btnEdit` | - | - | Button | - | - | - | I | - | - | Điều hướng sang `/edit` của đúng bài<br>[Nguồn giá trị] Nhãn tĩnh i18n `problemInfo.edit` (`:76-78`)<br>[EVT liên quan] EVT-2 |

### Khu vực B — Hàng số liệu

> Từ V1.2 các số liệu này hiển thị trong thẻ Thuộc tính ở cột phải, không còn là hàng thẻ riêng (xem 4.4). Danh sách item dưới đây giữ nguyên.

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Hàng số liệu | | | | | | | | | | | | | |
| | 1 | Testcase đã duyệt | `problemInfo.stats.approved` | `testcases` | `approved` | Label | Number | - | - | O | 0 | Số nguyên; dòng phụ `{n} testcase chờ duyệt` | [Công thức] Đã duyệt = số dòng `testcases` có `approved = true`; chờ duyệt = số dòng `approved = false` (nháp AI chưa duyệt, `origin = AI`) (`:54-55`). Testcase tay tạo với `approved = true` [Nguồn: 02-bd/database/F2_problem-bank.md:151-152]; Q3 đã đóng 2026-10-22<br>[EVT liên quan] - |
| | 2 | Testcase công khai | `problemInfo.stats.public` | `testcases` | `visibility` | Label | Number | - | - | O | 0 | Số nguyên | [Công thức] Số testcase có `approved = true` và `visibility = SAMPLE` (`:56`) [Nguồn: 02-bd/database/F2_problem-bank.md:142,152]<br>[EVT liên quan] - |
| | 3 | Đáp án mẫu | `problemInfo.stats.solution` | `problem_reference_solutions` | `language` | Label | String | - | - | O | `-` | Chỉ tên ngôn ngữ đáp án (Java, C++ hoặc Python) | Sửa 2026-10-22 (đồng bộ DB vòng 2, DEC-2026-1022-db-sync-round2 mục 9): **bỏ ô `{passed}/{total}`**, vì kết quả chạy đáp án mẫu không được lưu [Nguồn: 02-bd/database/F2_problem-bank.md:232]; chỉ hiện ngôn ngữ đọc từ cột `language` [Nguồn: 02-bd/database/F2_problem-bank.md:228]. Prototype còn hiển thị `{passed}/{total}` (`problem-info-view.tsx:86-92`), ghi nợ prototype<br>[Công thức] Có dòng đáp án mẫu thì hiện `language`, chưa có thì `-`<br>[EVT liên quan] - |
| | 4 | Giới hạn thời gian | `problemInfo.stats.timeLimit` | `problems` | `time_limit_ms` | Label | Number | - | - | O | - | `{n} ms` | [Nguồn giá trị] Cột `time_limit_ms` (`:93`)<br>[EVT liên quan] - |

### Khu vực C — Cột nội dung

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Cột nội dung | | | | | | | | | | | | | |
| | 1 | Đề bài | `problemInfo.content.statement` | `problems` | `statement_md` | Label | String | - | - | O | - | Markdown + LaTeX đã render (GFM, công thức inline và khối); HTML thô không được phân tích | Hiển thị bằng `MarkdownPreview` (cập nhật 2026-10-03, REQ-6); văn bản Markdown gốc chỉ có trong ô nhập của `SHR0202`<br>[Nguồn giá trị] Cột `statement_md`, DTO `statementMd` (`:98-99`); thành phần render `05-coding/frontend/src/shared/ui/data/markdown-preview.tsx:20-41`<br>[EVT liên quan] - |
| | 2 | Ràng buộc dữ liệu | `problemInfo.content.constraints` | `problems` | `constraints_md` | Label | String | - | - | O | - | Chữ đơn cách | Cột `TEXT NOT NULL` mặc định rỗng; chuỗi rỗng thì không hiện khối. Màn người học (`USR0102`) ghép với `statement_md`, màn này giữ hai khối riêng như prototype<br>[Nguồn giá trị] Cột `constraints_md` [Nguồn: 02-bd/database/F2_problem-bank.md:17]; DTO `constraintsText` (`:100-103`); `SHR0202` Q1 đã đóng 2026-10-22<br>[EVT liên quan] - |
| | 3 | Danh sách ví dụ mẫu | `problemInfo.examples.list` | `worked_examples` | `position` | List | List | - | - | O | rỗng | Mỗi ví dụ một khối "Ví dụ {n}" | [Nguồn giá trị] `worked_examples WHERE problem_id = :id ORDER BY position` [Nguồn: 02-bd/database/F2_problem-bank.md:234-247]; `WorkedExampleDto[]` (`:106-135`); `SHR0202` Q3 đã đóng 2026-10-22<br>[EVT liên quan] - |
| | 4 | Đầu vào | `problemInfo.examples.col.input` | `worked_examples` | `input` | ListColumn | String | - | - | O | - | Chữ đơn cách | [Nguồn giá trị] `WorkedExampleDto.input` (`:118-119`)<br>[EVT liên quan] - |
| | 5 | Đầu ra | `problemInfo.examples.col.output` | `worked_examples` | `output` | ListColumn | String | - | - | O | - | Chữ đơn cách | [Nguồn giá trị] `WorkedExampleDto.output` (`:122-123`)<br>[EVT liên quan] - |
| | 6 | Giải thích | `problemInfo.examples.col.explanation` | `worked_examples` | `explanation_md` | ListColumn | String | - | - | O | - | - | Cột `TEXT` nullable; không có giải thích thì không hiện dòng<br>[Nguồn giá trị] `WorkedExampleDto.explanation` (`:126-129`) [Nguồn: 02-bd/database/F2_problem-bank.md:245]; `SHR0202` Q3 đã đóng 2026-10-22<br>[EVT liên quan] - |
| | 7 | Ma trận độ phủ | `problemInfo.coverage.badges` | `testcases` | `category`, `approved` | List | List | - | - | O | - | `{loại ca}: {n}` | Cột `category` ENUM nullable (7 giá trị `TYPICAL`, `BOUNDARY_LOW`, `BOUNDARY_HIGH`, `DEGENERATE`, `DUPLICATE`, `SORTED`, `EXTREME`; NULL = testcase tay chưa gán nhãn) [Nguồn: 02-bd/database/F2_problem-bank.md:153]; Q4 phần loại ca đã đóng 2026-10-22<br>[Công thức] Với mỗi loại ca trong `TESTCASE_CATEGORIES`, đếm testcase đã duyệt (`approved = true`) thuộc loại đó; ô > 0 tô trạng thái thành công, ô = 0 tô trung tính (`:57-60,137-145`)<br>[EVT liên quan] - |
| | 8 | Mã đáp án mẫu | `problemInfo.solution.code` | `problem_reference_solutions` | `source_code`, `language` | Label | String | - | - | O | Thu gọn | Chữ đơn cách; mở bằng `<details>` | Đọc dòng đáp án mẫu của bài [Nguồn: 02-bd/database/F2_problem-bank.md:228-229]; `SHR0202` Q2 đã đóng 2026-10-22. Chỉ A2/A3 thấy (F2-18); không bao giờ đưa vào prompt AI<br>[Nguồn giá trị] DTO `sampleSolutionCode`, `sampleSolutionLanguage` (`:147-152`)<br>[EVT liên quan] - |
| | 9 | Chữ ký hàm theo ngôn ngữ | `problemInfo.spec.signatures` | `problem_specs` | `function_name`, `return_type`, `parameters`, `name_overrides` | List | List | - | - | O | 3 dòng (hiển thị) | Một dòng chữ ký mỗi ngôn ngữ (Java, C++, Python) | Suy từ MỘT chữ ký chung: tên hàm tự suy ra theo ngôn ngữ (có ghi đè thì dùng ghi đè), kiểu ánh xạ theo ngôn ngữ. Bài có kiểu ngoài lược đồ (`function_wrapper_supported = false`) thì thay ba dòng bằng câu "chỉ hỗ trợ Standard I/O"<br>[Nguồn giá trị] `FunctionSignatureDto` (bốn cột của dòng `problem_specs`, không còn bảng `function_signatures`) [Nguồn: 02-bd/database/F2_problem-bank.md:123-126]; cách dựng dòng giống mục 10 của Khu vực F ở `SHR0202`. Ba dòng là kết quả hiển thị suy từ một chữ ký chung, không phải ba dòng lưu trữ<br>[EVT liên quan] - |
| | 10 | Định dạng đọc stdin | `problemInfo.spec.stdinFormat` | `problem_specs` | `stdin_format_md` | Label | String | - | - | O | - | Văn bản, giữ xuống dòng | [Nguồn giá trị] `ProblemSpecDto.stdinFormat`<br>[EVT liên quan] - |
| | 11 | Định dạng in stdout | `problemInfo.spec.stdoutFormat` | `problem_specs` | `stdout_format_md` | Label | String | - | - | O | - | Văn bản, giữ xuống dòng | [Nguồn giá trị] `ProblemSpecDto.stdoutFormat`<br>[EVT liên quan] - |
| | 12 | Chiến lược so khớp | `problemInfo.spec.matchingStrategy` | `problem_specs` | `matching_strategy`, `epsilon_value` | Badge | Enum | - | - | O | - | `EXACT`/`TRIMMED`/`EPSILON`/`UNORDERED_SET`; kèm giá trị epsilon khi là `EPSILON` | [Nguồn giá trị] `ProblemSpecDto.matchingStrategy`, `epsilonValue`<br>[EVT liên quan] - |

### Khu vực D — Cột thuộc tính

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Cột thuộc tính | | | | | | | | | | | | | |
| | 1 | Độ khó | `problemInfo.properties.difficulty` | `problems`, `problem_levels` | `level_id`; `display_name` | Badge | String | - | - | O | - | Nhãn `display_name` của mức | Nhãn đọc từ danh mục `problem_levels` do ADMIN quản lý, không còn ba mức cố định; màu badge do giao diện suy ra (mức seed giữ màu cũ, mức mới trung tính). [Hiện trạng bản dựng] `problemLevelLabel` và `problemLevelTone` trên `useProblemLevels()` (`problem-info-view.tsx`, khối `difficultyLabel`)<br>[Nguồn giá trị] Cột `problems.level_id` nối `problem_levels` qua `ListProblemLevels` đã chốt 2026-10-03; cũ: cột `difficulty` (`:158-165`)<br>[EVT liên quan] - |
| | 2 | Trạng thái | `problemInfo.properties.status` | `problems` | `status` | Badge | Enum | - | - | O | - | `UNPUBLISHED`/`PUBLISHED` hiển thị "Chưa xuất bản"/"Đã xuất bản" | [Nguồn giá trị] Cột `status` (`:166-173`)<br>[EVT liên quan] - |
| | 3 | Giới hạn thời gian (ms) | `problemInfo.properties.limit.timeLimitMs` | `problems` | `time_limit_ms` | Label | Number | - | - | O | - | Số nguyên | [Nguồn giá trị] Cột `time_limit_ms` (`:32-37,174-179`)<br>[EVT liên quan] - |
| | 4 | Bộ nhớ (MB) | `problemInfo.properties.limit.memoryLimitMb` | `problems` | `memory_limit_mb` | Label | Number | - | - | O | - | Số nguyên | [Nguồn giá trị] Cột `memory_limit_mb`<br>[EVT liên quan] - |
| | 5 | Output (KB) | `problemInfo.properties.limit.outputLimitKb` | `problems` | `max_output_size_kb` | Label | Number | - | - | O | - | Số nguyên | [Nguồn giá trị] Cột `max_output_size_kb`<br>[EVT liên quan] - |
| | 6 | Stack (MB) | `problemInfo.properties.limit.stackLimitMb` | - | - | Label | Number | - | - | O | - | Số nguyên | **Không có cột `stack` trong `problems`** (`02-bd/database/F2_problem-bank.md` mục 1.1 chỉ có thời gian, bộ nhớ, output, số lần nộp/giờ) — xem Q4<br>[Nguồn giá trị] `ProblemLimits.stackLimitMb` của mock (`draft-types.ts:55-60`)<br>[EVT liên quan] - |
| | 7 | Công tắc AI | `problemInfo.ai.guards` | - | - | List | List | - | - | O | - | Mỗi dòng: nhãn + Badge `Bật`/`Tắt` | Đọc `problems.no_full_code`, `problems.socratic_only`; **2** công tắc, khớp DTO của `SHR0202` sau khi bỏ `allowHiddenHint` (Q5 đã đóng)<br>[Nguồn giá trị] `AiAuthoringContextDto.noFullCode`, `questionOnly` (`:184-193`)<br>[EVT liên quan] - |
| | 8 | Ngữ cảnh AI riêng của bài | `problemInfo.ai.brief` | - | - | Label | String | - | - | O | "Chưa có ngữ cảnh riêng cho bài này." | Văn bản | [Nguồn giá trị] `AiAuthoringContextDto.briefText` (cột `problems.ai_brief`); rỗng thì hiện nhãn tĩnh `problemInfo.aiBriefEmpty` (`:194-196`)<br>[EVT liên quan] - |

---

## Sheet 6. Đặc tả điều khiển item

> Dùng đúng NO, tên item và thứ tự của Sheet 5. **Không có kiểm nhập liệu** (màn chỉ đọc). Item nhãn chỉ-đọc không có điều kiện kích hoạt nên ghi `-`.

### Khu vực A — Thanh đầu trang

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Thanh đầu trang | | | | |
| | 1 | Nút quay lại | Có | [Điều kiện kích hoạt] Luôn kích hoạt, không có thay đổi chưa lưu để hỏi xác nhận. |
| | 2 | Tiêu đề bài | Có | [Điều kiện kích hoạt] - |
| | 3 | Mã và chủ đề | Có | [Điều kiện kích hoạt] - |
| | 4 | Xem như người học | Có | [Điều kiện kích hoạt] Hiện nhưng chưa có hành động, xem Q6. |
| | 5 | Sửa bài | Có | [Điều kiện kích hoạt] Luôn kích hoạt khi người dùng có `PROBLEM_AUTHORING:UPDATE` `[SoT: Suy luận — prototype hiện nút vô điều kiện (problem-info-view.tsx:76-78), chưa có phân quyền thật; quyền sửa theo 02-bd/screens/shared/SHR0201_problem_management.md:269]`. |

### Khu vực B — Hàng số liệu

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Hàng số liệu | | | | |
| | 1 | Testcase đã duyệt | Có | [Điều kiện kích hoạt] - |
| | 2 | Testcase công khai | Có | [Điều kiện kích hoạt] - |
| | 3 | Đáp án mẫu | Có | [Điều kiện kích hoạt] -<br>[Tự động đặt] Chưa có đáp án mẫu thì giá trị là `-`. |
| | 4 | Giới hạn thời gian | Có | [Điều kiện kích hoạt] - |

### Khu vực C — Cột nội dung

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Cột nội dung | | | | |
| | 1 | Đề bài | Có | [Điều kiện kích hoạt] - |
| | 2 | Ràng buộc dữ liệu | Có | [Điều kiện kích hoạt] - |
| | 3 | Danh sách ví dụ mẫu | Có | [Điều kiện kích hoạt] - |
| | 4 | Đầu vào | Điều kiện | [Điều kiện hiển thị] Có ít nhất một ví dụ mẫu.<br>[Điều kiện kích hoạt] - |
| | 5 | Đầu ra | Điều kiện | [Điều kiện hiển thị] Có ít nhất một ví dụ mẫu.<br>[Điều kiện kích hoạt] - |
| | 6 | Giải thích | Điều kiện | [Điều kiện hiển thị] Có ít nhất một ví dụ mẫu.<br>[Điều kiện kích hoạt] - |
| | 7 | Ma trận độ phủ | Có | [Điều kiện kích hoạt] -<br>[Tự động đặt] Ô có `n > 0` tô thành công, `n = 0` tô trung tính (loại ca còn thiếu). |
| | 8 | Mã đáp án mẫu | Điều kiện | [Điều kiện hiển thị] Người xem có `PROBLEM_AUTHORING` (REQ-5, F2-18) — **kiểm quyền ở máy chủ, không chỉ ẩn UI**; prototype hiện vô điều kiện vì chưa có phân quyền.<br>[Điều kiện kích hoạt] Bấm tiêu đề "Xem mã đáp án mẫu" để mở/thu gọn.<br>[Tự động đặt] Mặc định thu gọn mỗi lần vào màn. |
| | 9 | Chữ ký hàm theo ngôn ngữ | Có | [Điều kiện hiển thị] Bài chưa có kiểu ngoài lược đồ; ngược lại hiện câu "chỉ hỗ trợ Standard I/O". |
| | 10 | Định dạng đọc stdin | Có | - |
| | 11 | Định dạng in stdout | Có | - |
| | 12 | Chiến lược so khớp | Có | - |

### Khu vực D — Cột thuộc tính

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Cột thuộc tính | | | | |
| | 1 | Độ khó | Có | [Điều kiện kích hoạt] - |
| | 2 | Trạng thái | Có | [Điều kiện kích hoạt] -<br>[Tự động đặt] `PUBLISHED` tô thành công, `UNPUBLISHED` tô trung tính. |
| | 3 | Giới hạn thời gian (ms) | Có | [Điều kiện kích hoạt] - |
| | 4 | Bộ nhớ (MB) | Có | [Điều kiện kích hoạt] - |
| | 5 | Output (KB) | Có | [Điều kiện kích hoạt] - |
| | 6 | Stack (MB) | Có | [Điều kiện kích hoạt] - |
| | 7 | Công tắc AI | Có | [Điều kiện kích hoạt] -<br>[Tự động đặt] Cờ bật tô thành công, tắt tô trung tính. |
| | 8 | Ngữ cảnh AI riêng của bài | Có | [Điều kiện kích hoạt] -<br>[Tự động đặt] Rỗng thì hiện nhãn "Chưa có ngữ cảnh riêng cho bài này.". |

---

## Sheet 7. Danh sách truyền dữ liệu

### 7.1 Trường DTO

Màn này **không định nghĩa DTO mới**. Toàn bộ trường lấy từ các DTO ở `02-bd/screens/shared/SHR0202_problem_authoring.md` Sheet 7.1
(dòng 560-579 của file đó); bảng dưới chỉ ánh xạ item của màn này sang trường đã có, hướng truyền duy nhất là Nguồn → màn (chỉ đọc).

| NO | DTO (định nghĩa ở `SHR0202`) | Trường DTO | Item màn | Ghi chú |
| --: | :--- | :--- | :--- | :--- |
| 1 | `ProblemAuthoringDetailDto` | `code`, `title` | Khu A NO 2-3 | [Nguồn] Phản hồi `GetProblemForAuthoring` |
| 2 | `ProblemAuthoringDetailDto` | `statementMd` | Khu C NO 1 | [Nguồn] như trên |
| 3 | `ProblemAuthoringDetailDto` | `constraintsText` | Khu C NO 2 | Cột `problems.constraints_md` (`SHR0202` Q1 đã đóng 2026-10-22) |
| 4 | `ProblemAuthoringDetailDto` | `levelId`, `status` | Khu D NO 1-2 | [Chuyển đổi] `levelId` đổi sang nhãn qua `ListProblemLevels` (cùng mẫu với `topicIds`; thay cho trường ENUM `difficulty` cũ); `UNPUBLISHED`/`PUBLISHED` đổi sang "Chưa xuất bản"/"Đã xuất bản" |
| 5 | `ProblemAuthoringDetailDto` | `timeLimitMs`, `memoryLimitMb`, `maxOutputSizeKb` | Khu B NO 4, Khu D NO 3-5 | [Chuyển đổi] Hiển thị nguyên giá trị mili-giây, khác `SHR0202` hiển thị giây |
| 6 | `ProblemAuthoringDetailDto` | `topicIds` | Khu A NO 3 | [Chuyển đổi] Id chủ đề đổi sang tên qua `ListProblemTopics` |
| 7 | `ProblemAuthoringDetailDto` | `sampleSolutionLanguage`, `sampleSolutionCode` | Khu B NO 3 (dòng phụ), Khu C NO 8 | Bảng `problem_reference_solutions` (`language`, `source_code`) (`SHR0202` Q2 đã đóng 2026-10-22) |
| 8 | `WorkedExampleDto` | `input`, `output`, `explanation` | Khu C NO 3-6 | Bảng `worked_examples` (`input`, `output`, `explanation_md`) (`SHR0202` Q3 đã đóng 2026-10-22) |
| 9 | `TestcaseDto` | `visibility`, `approved`, `origin`, `category` | Khu B NO 1-2, Khu C NO 7 | [Chuyển đổi] Đếm phía client trên danh sách trả về (prototype `:54-60`); DD cân nhắc trả số đếm có sẵn từ backend |
| 10 | `AiAuthoringContextDto` | `briefText`, `noFullCode`, `questionOnly` | Khu D NO 7-8 | `allowHiddenHint` đã bỏ khỏi DTO (2026-10-22, PB-07; prototype không có cờ này, `draft-types.ts:62-73`); xem Q5 (đã đóng) |
| 11 | - (chưa có DTO) | `stackLimitMb` | Khu D NO 6 | **Không có trường DTO tương ứng ở `SHR0202`, và không có nguồn lưu ở DB** (không có cột `stack`) — xem Q4. `solutionCheck {ran, passed, total}` đã bỏ 2026-10-22 (kết quả chạy đáp án mẫu không lưu, Q2 đã đóng). Loại ca (`category`) đã có cột, đã chuyển sang dòng 9 |

`[SoT: Suy luận]` — ánh xạ do BD đề xuất; `03-dd/api/F2_problem-bank.md` chốt lại.

### 7.2 Truy cập bảng dữ liệu (4)

| NO | Tên logic | Bảng | Repository | CRUD | Mục đích | Ghi chú |
| --: | :--- | :--- | :--- | :-: | :--- | :--- |
| 1 | Bài toán | `problems` | `ProblemRepository` | R | Đọc đề, giới hạn, độ khó, trạng thái | `GetProblemForAuthoring`: R |
| 2 | Chủ đề | `topics`, `problem_topics` | `TopicRepository`, `ProblemTopicRepository` | R | Đọc tên chủ đề để hiện ở dòng phụ tiêu đề | `GetProblemForAuthoring`: R; `ListProblemTopics`: R |
| 4 | Độ khó | `problem_levels` | `ProblemLevelRepository` | R | Đọc nhãn mức độ khó để hiện badge | `ListProblemLevels`: R |
| 3 | Testcase | `testcases` | `TestcaseRepository` | R | Đọc để đếm đã duyệt, công khai, ví dụ mẫu | `GetProblemForAuthoring`: R |

Không có thao tác `C`, `U`, `D` nào — màn chỉ đọc. Tên repository kế thừa `SHR0202` Sheet 7.2 (`[Suy luận]`, DD chốt lại).

### 7.3 Danh sách endpoint [Nội bộ]

> Chỉ tên nghiệp vụ và BC sở hữu. **Màn này không có endpoint của riêng nó**; hợp đồng chi tiết thuộc `03-dd/api/F2_problem-bank.md` (chưa viết).

| NO | Endpoint (tên nghiệp vụ) | Mục đích | BC sở hữu |
| --: | :--- | :--- | :--- |
| 1 | `GetProblemForAuthoring` (dùng chung với `SHR0202`) | Tải chi tiết bài toán để hiển thị chỉ đọc | `problem-bank` |
| 2 | `ListProblemTopics` (dùng chung với `SHR0202`) | Đổi id chủ đề sang tên | `problem-bank` |

[Nguồn: 02-bd/screens/shared/SHR0202_problem_authoring.md:600-632 (mục 7.3 của file đó)]

---

## Sheet 8. Danh sách sự kiện

> Màn chỉ đọc: chỉ có sự kiện điều hướng. Bộ thẻ và quy ước cột: `02-bd/_rules/bd-template-9sheet.md` mục 2, 3.

### Màn chính: Chi tiết bài tập

| NO | Loại | Sự kiện | Chi tiết | Chuyển màn | Gọi API | Tên xử lý | Ghi chú |
| --: | :--- | :--- | :--- | :-: | :-: | :--- | :--- |
| 1 | Liên kết | Quay lại danh sách bài tập | Bấm nút quay lại ở đầu trang. | Có | Không | - | [Các bước]<br>1. Điều hướng về `problem_management` (`basePath`).<br>[Khi thành công] Mở `problem_management`. |
| 2 | Nút | Mở form sửa bài | Bấm "Sửa bài". | Có | Không | - | [Các bước]<br>1. Điều hướng sang `{basePath}/{problemId}/edit`.<br>[Khi thành công] Mở `problem_authoring` (`SHR0202`) đã nạp nội dung bài đó. |
| 3 | Nút | Xem như người học | Bấm "Xem như người học". | Không | Không | - | Chưa gắn hành động trong prototype; đích chưa chốt, xem Q6. |

Tải dữ liệu khi vào màn không phải sự kiện người dùng; nó là bước khởi tạo của view, gọi `GetProblemForAuthoring` và `ListProblemTopics` (Sheet 7.3).
Khi mã bài không tồn tại, RD yêu cầu hiện trạng thái không tìm thấy kèm nút quay lại danh sách
[Nguồn: 01-rd/screens/shared/SHR0203_problem_info.md:61], nhưng prototype **chưa dựng** trạng thái này (`problem-info-view.tsx` không có nhánh nào) — xem Q7.

---

## Sheet 9. Đặc tả kiểm tra

> Không có kiểm nhập liệu (màn chỉ đọc). Chỉ có kiểm quyền. Tiêu chí nghiệm thu `AC-nn` thuộc `04-tdd/screens/shared/SHR0203_problem_info.md`, không lặp lại ở đây.

| NO | Loại | Tóm tắt kiểm | Chi tiết | Mức | Mã thông báo | Ghi chú | EVT gọi | Thứ tự |
| --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: |
| 1 | Kiểm quyền | Quyền truy cập màn | [Nội dung kiểm] Không có Function `PROBLEM_AUTHORING` thì không được vào màn.<br>[Nơi thực thi] Cả tầng định tuyến phía giao diện và tầng phân quyền phía máy chủ.<br>[Tiêu điểm] Toàn màn. | Lỗi | Chưa có mã thông báo | Nội dung "Bạn không có quyền truy cập màn này." (theo mẫu `SHR0202` Sheet 9 NO 1) | - | 1 |
| 2 | Kiểm quyền | Quyền sở hữu bài (A2) | [Nội dung kiểm] A2 mở bài không do mình tạo **và chưa xuất bản** thì bị chặn (trả 404, như mã không tồn tại), dù có `PROBLEM_AUTHORING`; bài `Đã xuất bản` của người khác thì xem được ở chế độ chỉ đọc (2026-10-11, `DEC-2026-1011-a2-read-published-problems-of-others`). Sửa, xoá, `/edit`, `/preview` vẫn chỉ trên bài của mình.<br>[Nơi thực thi] Tầng application phía máy chủ, không chỉ ẩn UI.<br>[Tiêu điểm] Toàn màn. | Lỗi | Chưa có mã thông báo | Nội dung "Bạn không có quyền xem bài toán này." `[SoT: Suy luận — theo SHR0202 Sheet 9 NO 2]` | - | 2 |
| 3 | Kiểm quyền | Hiển thị mã đáp án mẫu | [Nội dung kiểm] Mã đáp án mẫu chỉ có trong phản hồi khi người gọi có `PROBLEM_AUTHORING`; học viên không bao giờ nhận.<br>[Nơi thực thi] Phía máy chủ khi dựng phản hồi (F2-18).<br>[Tiêu điểm] Khu vực C NO 8. | Lỗi | Chưa có mã thông báo | Không thông báo riêng — trường vắng khỏi phản hồi, mục ẩn khỏi màn | - | 3 |

Hàng "kiểm quyền" 1-2 giống `SHR0202`; không viết lại nội dung vượt quá những gì đã có ở file đó.
[Nguồn: 02-bd/screens/shared/SHR0202_problem_authoring.md:689-690 (Sheet 9 NO 1-2)]

---

## Sổ câu hỏi đã đóng

| # | Câu hỏi | Vì sao chưa trả lời được | Chủ sở hữu |
| :-: | :--- | :--- | :--- |
| Q1 | ~~Bảng mã ở `02-bd/_rules/bd-template-9sheet.md` mục 8 chưa có `SHR0203`/`SHR0303`, `system_survey.md` mục 7.0 chưa có slug `problem_info`.~~ **ĐÃ XỬ LÝ 2026-10-02:** đã thêm hai dòng vào bảng mã và hai slug vào `system_survey.md` mục 7.0. | — | Đã đóng |
| Q2 | ~~Kết quả "chạy với đáp án mẫu" (`{passed}/{total}`) và mã đáp án mẫu: lưu ở đâu?~~ **ĐÃ CHỐT 2026-10-22 (`DEC-2026-1022-problem-bank-schema-sync` mục 2, PB-04; `DEC-2026-1022-db-sync-round2` mục 9, tiêu chí ponytail bậc 2):** mã và ngôn ngữ đáp án mẫu lưu ở bảng `problem_reference_solutions`; kết quả chạy không lưu nên màn chỉ đọc **bỏ ô `{passed}/{total}`**, chỉ hiện ngôn ngữ đáp án [Nguồn: 02-bd/database/F2_problem-bank.md:232]. Đã sửa Khu B NO 3, Sheet 6, Sheet 7.1 NO 11 | Đã đóng | Đã đóng |
| Q3 | ~~"Testcase đã duyệt" ánh xạ vào cột nào? Prototype có cờ `approved` riêng, DB chỉ có `is_ai_generated_draft`; hai khái niệm trùng nhau hay khác nhau (testcase tác giả viết tay có mặc định "đã duyệt" không)?~~ **ĐÃ CHỐT 2026-10-22 (`DEC-2026-1022-problem-bank-schema-sync` mục 3, PB-05):** cột `testcases.approved` BOOLEAN riêng, kèm `origin` (`MANUAL`/`AI`) và `category`; thay cho `is_ai_generated_draft`. Testcase tay mặc định `approved = true` là suy luận của DB doc (RD chỉ nói rõ nháp AI chưa duyệt) [Nguồn: 02-bd/database/F2_problem-bank.md:151-153]. Đã sửa Khu B NO 1-2, Khu C NO 7, Sheet 7.1 NO 9 | Đã đóng | Đã đóng |
| Q4 | **Đã đóng 2026-10-29 (DEC-2026-1029-f2-migration-readiness).** Category nullable có 7 giá trị đã chốt; bỏ Stack (MB), không tạo problems.stack_limit_mb; theo quyết định owner 2026-10-25 và DD màn. | DD F2 §34 và DD màn | Đã đóng |
| Q5 | ~~Số cờ hành vi AI: prototype chỉ có 2, DTO của `SHR0202` có 3 (`allowHiddenHint`); bỏ khỏi DTO? Nơi lưu khối vẫn chưa chốt.~~ **Đã đóng 2026-10-22 (`DEC-2026-1022-db-sync-round4`, PB-07):** bỏ `allowHiddenHint` khỏi DTO (cờ thuộc tính năng gợi ý theo cấp độ đã cắt, `DEC-2026-0831-remove-tiered-hints-ai-config`); nơi lưu là ba cột `problems.ai_brief`, `no_full_code`, `socratic_only`. | Đã đóng cùng `SHR0202` Q4 | Đã đóng |
| Q6 | **Đã đóng 2026-10-29 (DEC-2026-1029-f2-migration-readiness).** Xem trước bằng route riêng của tác giả như SHR0202 Q13/Q15, bản lưu gần nhất; không có workspace nộp bài. | DD F2 §34 và DD màn | Đã đóng |
| Q7 | **Đã đóng 2026-10-29 (DEC-2026-1029-f2-migration-readiness).** Sai ID/không được thấy bài trả 404 PRB-010 và trạng thái không tìm thấy; lỗi mạng/5xx có trạng thái lỗi và thử lại, 403 PRB-002 là thiếu quyền chức năng theo DD F2 §34, không suy thành bài không tồn tại. | DD F2 §34 và DD màn | Đã đóng |
| Q8 | ~~Khu A2 chỉ xem bài của mình: A2 mở bài của người khác thì 404 hay 403?~~ **ĐÃ CHỐT 2026-10-03 (chủ dự án, theo đề xuất), ĐIỀU CHỈNH 2026-10-11 (chủ dự án duyệt, `DEC-2026-1011-a2-read-published-problems-of-others`, tiêu chí ponytail bậc 6):** A2 mở bài của người khác: bài **đã xuất bản** thì xem **chỉ đọc** (`problem_info`); bài **chưa xuất bản** thì vẫn trả **404 "Không tìm thấy bài tập"**, giống hệt mã không tồn tại, để câu trả lời không tiết lộ mã nào có thật; máy chủ kiểm ở tầng application, không chỉ ẩn nút. Sửa, xoá, `/edit`, `/preview` vẫn chỉ trên bài của mình; lý do điều chỉnh: giảng viên cần xem đề trước khi giao bài từ ngân hàng chung (F2-12). Phần 404 cho bài nháp người khác của bản chốt 2026-10-03 giữ nguyên. Áp cho cả `problem_info`, `problem_authoring` (`/edit`) và trang xem trước (`/preview`). Bản dựng: mock suy ra người xem từ khu vực của địa chỉ (`/instructor` là giảng viên mẫu, sở hữu một số mã), ném 404, ba màn hiện "Không tìm thấy bài tập."; `isNotFound` ở `shared/api` | Hợp đồng lỗi thật (mã lỗi, nội dung) thuộc `03-dd/api/F2_problem-bank.md` | Đã đóng |

**Hết tài liệu.** Mọi `[Nguồn: problem-info-view.tsx:n]` trỏ `05-coding/frontend/src/views/shared/problem-info/ui/problem-info-view.tsx`; `[Nguồn: ...problem-management-view.tsx:n]` trỏ `05-coding/frontend/src/views/shared/problem-management/ui/problem-management-view.tsx`.

### Baseline migration F2 — 2026-10-29

Không còn câu hỏi thiết kế của màn chặn schema F2. Giữ các hàng UI đã bỏ làm lịch sử, không tạo cột Stack hay cờ AI thứ ba. Quy tắc testcase/epsilon/snapshot/seed theo BD database F2 §7; trạng thái chạy thật còn chờ coding.
