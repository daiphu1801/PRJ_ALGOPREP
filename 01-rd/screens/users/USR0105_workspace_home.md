# RD (Yêu cầu hệ thống mới) — Trang đầu Workspace / `USR0105`

> Mã màn hình: `USR0105`, theo `02-bd/_rules/bd-template-9sheet.md` mục 8 (màn mới, lấy số thứ tự kế tiếp
> trong nhóm "Bài tập"; bảng mã ở mục đó chưa có dòng này, xem BD Câu hỏi mở Q8).
> Slug chính tắc: `workspace_home`. Slug này **chưa** có dòng trong `01-rd/overview/system_survey.md` mục 7.1
> (bảng khu người học dừng ở `saved_problems`)
> [SoT: 01-rd/overview/system_survey.md:571-584], xem BD Câu hỏi mở Q8.
> Phạm vi/Bounded Context: `judge-orchestration` (F4, bài làm dở và kết quả lần nộp gần nhất) và
> `problem-bank` (F2, chọn bài ngẫu nhiên) [SoT: Suy luận] — chưa có dòng nào ở survey mục 7.1 để trích; suy
> từ nguồn dữ liệu của khối "Bài đang làm dở" ở `USR0101`
> [Nguồn: 02-bd/screens/users/USR0101_problem_list.md:252,384,544]. Actor: A1.
> Nguồn sự thật (SoT): quyết định của chủ dự án 2026-10-20 `DEC-2026-1020-student-workspace-split-and-password-in-settings`
> [Nguồn: .nexa/control/decision-registry.md:4233-4250]. Không có prototype tĩnh riêng trong `09-layoutBase/`; bản
> dựng là bằng chứng duy nhất (`05-coding/frontend/src/views/users/workspace-home/ui/workspace-home-view.tsx`).
> Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

Trước 2026-10-20, mục "Workspace" trên thanh điều hướng không phải một trang: nó **chuyển hướng** thẳng vào bài
làm dở gần nhất, không có thì về danh sách bài toán
[Nguồn: 02-bd/screens/users/_shell.md:163; 02-bd/screens/users/USR0102_problem_detail.md, Sheet 3 (phần lối vào từ khung Người học, V0.9 đã viết lại)]. Lối đó không cho
người học chọn một bài làm dở khác, cũng không cho họ bắt đầu bài mới từ đây. Chủ dự án quyết định tách màn bài
toán thành ba trang: tổng quan `/problems/{id}`, **trang đầu Workspace `/workspace`** (màn này), và màn viết mã
`/workspace/{id}` (`USR0102`) [Nguồn: .nexa/control/decision-registry.md:4239-4244].

Màn này là trang đích của mục "Workspace": cho người học thấy những bài mình đang làm dở để tiếp tục, hoặc đi
chọn một bài khác (từ danh sách, hoặc ngẫu nhiên) để bắt đầu.

File này mô tả **hành vi ở mức yêu cầu**, không mô tả giao diện (thuộc BD
`02-bd/screens/users/USR0105_workspace_home.md`). Không lặp lại định nghĩa "bài đang làm dở" đã chốt ở `USR0101`
[Nguồn: 02-bd/screens/users/USR0101_problem_list.md:384], chỉ trỏ tới.

---

## 2. Phạm vi

Màn không có mã `Fx-nn` riêng: nó tái sử dụng dữ liệu và hành vi đã có ở `USR0101` (khối "Bài đang làm dở",
nút "Bài ngẫu nhiên") nên không sinh yêu cầu chức năng mới ở mức module `[SoT: Suy luận]` — chưa tìm thấy mã
`Fx-nn` nào trong `01-rd/req/` nói về trang đầu Workspace.

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Mục "Workspace" mở một trang thay cho chuyển hướng | - | `DEC-2026-1020-student-workspace-split-and-password-in-settings` [Nguồn: .nexa/control/decision-registry.md:4239-4244] |
| Liệt kê bài đang làm dở của chính người học để tiếp tục | - | Cùng quyết định; định nghĩa dữ liệu ở `USR0101` [Nguồn: 02-bd/screens/users/USR0101_problem_list.md:384] |
| Đi chọn bài trong danh sách, hoặc bốc một bài chưa giải ngẫu nhiên và mở trang tổng quan | - | Cùng quyết định, mục (4) [Nguồn: .nexa/control/decision-registry.md:4249-4250] |

Các khối của màn, theo bản dựng [Nguồn: 05-coding/frontend/src/views/users/workspace-home/ui/workspace-home-view.tsx:38-91]:

1. **Đầu trang** — tiêu đề, một dòng mô tả, hai hành động: "Chọn bài trong danh sách" và "Bài ngẫu nhiên" (`:38-59`).
2. **Khối "Bài đang làm dở"** — danh sách bài chưa xong, mỗi mục cho biết tên bài, ngôn ngữ và kết quả lần nộp
   gần nhất kèm số testcase đạt trên tổng (`:61-91`); không có mục nào thì hiện một câu hướng dẫn (`:63-66`).

**Chưa dựng, đang chờ chủ dự án** (Câu hỏi mở Q1 bên dưới): khối "Bài tập lớp" và khối "Bài đã lưu" trên trang
này. Mặc định đã xác nhận: trang đầu Workspace **chỉ** chứa bài đang làm dở
[Nguồn: .nexa/control/decision-registry.md:4250].

### 2.1 Cấu trúc hệ thống liên quan

| Màn/tài liệu | Vai trò | Bounded Context |
| :--- | :--- | :--- |
| Màn hiện tại | `USR0105` / `workspace_home` | `judge-orchestration`, `problem-bank` |
| `USR0102` / `problem_detail` | Đích khi bấm một bài đang làm dở: `/workspace/{problemId}` (màn viết mã) | `problem-bank` + `harness` + `judge-orchestration` |
| `USR0101` / `problem_list` | Đích của nút "Chọn bài trong danh sách"; đồng thời là nơi đã có khối "Bài đang làm dở" cùng nguồn | `problem-bank`, `identity` |
| `USR0104` / `problem_overview` | Đích của nút "Bài ngẫu nhiên": `/problems/{problemId}`. Màn do một tài liệu riêng mô tả, không thuộc file này | `problem-bank` |
| Khung điều hướng Người học | Mục "Workspace" là lối vào duy nhất [Nguồn: 02-bd/screens/users/_shell.md:63] | - |

---

## 3. Ngoài phạm vi (Out of Scope)

- Bảng màu, spacing, component cụ thể, lưới thẻ, breakpoint — thuộc BD (`02-bd/screens/users/USR0105_workspace_home.md`).
- Hợp đồng API (danh sách bài làm dở, bốc bài ngẫu nhiên) — thuộc DD; `03-dd/api/judge-orchestration.md` chưa viết
  [Nguồn: 05-coding/frontend/src/views/users/workspace-home/ui/workspace-home-view.tsx:7].
- Soạn mã, chạy thử, nộp bài — thuộc `USR0102`. Màn này chỉ dẫn người học tới đó.
- Đọc đề bài, ví dụ, giới hạn từng ngôn ngữ — thuộc `USR0104`. Màn này chỉ mở trang đó qua "Bài ngẫu nhiên".
- Khối "Bài tập lớp" và khối "Bài đã lưu" — chưa nằm trong phạm vi, chờ chủ dự án (Q1).
- Bản nháp mã: nằm ở trình duyệt, do `USR0102` khôi phục
  [Nguồn: 02-bd/screens/users/USR0102_problem_detail.md:270]; màn này không đọc và không hiển thị bản nháp.

---

## 4. Tiền đề và ràng buộc

| # | Câu hỏi | Quyết định | Ghi chú |
| :-: | :--- | :--- | :--- |
| Q1 | Mục "Workspace" làm gì khi bấm? | **Chốt 2026-10-20:** mở trang `/workspace` (màn này), không chuyển hướng nữa. | Thay phần "chuyển hướng" của `DEC-2026-0922-users-and-admin-conflict-resolutions` [Nguồn: .nexa/control/decision-registry.md:4244, 4260-4261] |
| Q2 | "Bài ngẫu nhiên" mở trang nào? | **Chốt 2026-10-20:** mở trang tổng quan `/problems/{id}`, không mở thẳng Workspace. | [Nguồn: .nexa/control/decision-registry.md:4249-4250] |
| Q3 | Trang này chứa những gì? | **Chốt 2026-10-20:** chỉ bài đang làm dở. | Hai khối còn lại chờ owner, xem BD Câu hỏi mở Q1 |

Các điểm chưa chốt (định nghĩa số mục tối đa, bài đã rút xuống, trạng thái tải/lỗi) nằm ở mục Câu hỏi mở của BD;
RD không tự quyết thay chủ dự án.

---

## 5. Danh sách yêu cầu (REQ)

| ID | Yêu cầu | Loại | Nguồn/SoT |
|---|---|---|---|
| REQ-01 | **Cho** tôi là người học đã đăng nhập, **Khi** tôi chọn mục "Workspace" trên thanh điều hướng, **Thì** tôi thấy một trang có tiêu đề, dòng mô tả, hai hành động và khối bài đang làm dở; hệ thống **không** tự chuyển tôi vào một bài cụ thể | Chức năng (GWT) | [Nguồn: .nexa/control/decision-registry.md:4239-4244; 05-coding/frontend/src/views/users/workspace-home/ui/workspace-home-view.tsx:3-5,36-59] |
| REQ-02 | **Cho** tôi có bài đã nộp mà lần nộp gần nhất chưa đạt `Accepted`, **Khi** tôi mở trang, **Thì** bài đó nằm trong khối "Bài đang làm dở" kèm tên bài, ngôn ngữ của lần nộp gần nhất và kết quả lần nộp đó cùng số testcase đạt trên tổng số (ví dụ "Sai kết quả 6/10"); danh sách chỉ gồm bài của chính tôi | Chức năng (GWT) | [Nguồn: 05-coding/frontend/src/views/users/workspace-home/ui/workspace-home-view.tsx:69-86; 02-bd/screens/users/USR0101_problem_list.md:384] |
| REQ-03 | **Cho** tôi thấy một bài trong khối "Bài đang làm dở", **Khi** tôi bấm vào bài đó, **Thì** tôi vào màn viết mã `/workspace/{problemId}` của đúng bài đó | Chức năng (GWT) | [Nguồn: 05-coding/frontend/src/views/users/workspace-home/ui/workspace-home-view.tsx:71-74; .nexa/control/decision-registry.md:4242-4243] |
| REQ-04 | **Cho** tôi chưa có bài nào đang làm dở, **Khi** tôi mở trang, **Thì** khối thay danh sách bằng một câu hướng dẫn chọn bài trong danh sách; hai hành động ở đầu trang vẫn dùng được | Chức năng (GWT) | [Nguồn: 05-coding/frontend/src/views/users/workspace-home/ui/workspace-home-view.tsx:63-66; 05-coding/frontend/messages/vi.json:2956] |
| REQ-05 | **Cho** tôi muốn tự chọn bài, **Khi** tôi bấm "Chọn bài trong danh sách", **Thì** tôi sang trang danh sách bài toán `/problems` | Chức năng (GWT) | [Nguồn: 05-coding/frontend/src/views/users/workspace-home/ui/workspace-home-view.tsx:46-53] |
| REQ-06 | **Cho** tôi muốn thử một bài mới, **Khi** tôi bấm "Bài ngẫu nhiên", **Thì** hệ thống bốc ngẫu nhiên một bài tôi **chưa giải** và mở **trang tổng quan** `/problems/{problemId}` của bài đó, không mở thẳng màn viết mã | Chức năng (GWT) | [Nguồn: 05-coding/frontend/src/views/users/workspace-home/ui/workspace-home-view.tsx:26-34; .nexa/control/decision-registry.md:4249-4250] |
| REQ-07 | **Cho** tôi đã giải hết mọi bài (không còn bài chưa giải để bốc), **Khi** tôi bấm "Bài ngẫu nhiên", **Thì** tôi ở lại trang và thấy một thông báo cảnh báo, không bị chuyển trang | Chức năng (GWT) | [Nguồn: 05-coding/frontend/src/views/users/workspace-home/ui/workspace-home-view.tsx:29-32; 05-coding/frontend/messages/vi.json:190] |
| REQ-08 | Trang chỉ cho người học đã đăng nhập; dữ liệu bài đang làm dở chỉ là của chính người đăng nhập, không có đường xem của người khác | Ràng buộc | [SoT: Suy luận] — cùng nguyên tắc `USR0101` bắt buộc đăng nhập và dữ liệu cá nhân [Nguồn: 02-bd/screens/users/_shell.md:136; 02-bd/screens/users/USR0101_problem_list.md:605]. Bản dựng chưa có kiểm đăng nhập (prototype) |
| REQ-09 | Trang không phụ thuộc hệ AI (F5/F6): không có điểm nào của màn gọi `ai-review` hay `interview-bank` | Ràng buộc | `CLAUDE.md` — "AI subsystem degrades gracefully"; xác nhận bằng bản dựng, không import AI nào [Nguồn: 05-coding/frontend/src/views/users/workspace-home/ui/workspace-home-view.tsx:10-17] |

---

## 6. Yêu cầu dữ liệu

### 6.1 Dữ liệu tham chiếu

Chi tiết bảng, cột, kiểu và ràng buộc lưu trữ thuộc BD/DD. Ở mức yêu cầu, màn cần hai nguồn:

- **Bài đang làm dở của người đăng nhập** — nguồn thật là các lần nộp (`judge.submissions`), không có bảng nháp
  riêng; bản dựng hiện lấy từ dữ liệu mẫu
  [Nguồn: 05-coding/frontend/src/views/users/workspace-home/ui/workspace-home-view.tsx:7; 02-bd/screens/users/USR0101_problem_list.md:384].
- **Tập bài chưa giải** để bốc ngẫu nhiên — nguồn là ngân hàng bài kèm trạng thái làm bài theo người dùng. Bản
  dựng lọc `solveState !== "solved"` trên danh sách mẫu
  [Nguồn: 05-coding/frontend/src/views/users/workspace-home/ui/workspace-home-view.tsx:27].

### 6.2 Luồng dữ liệu

Hợp đồng API, request/response và mã lỗi thuộc DD. Luồng ở mức yêu cầu thể hiện qua REQ-01 đến REQ-07.

---

## 7. RACI và danh sách bàn giao

| Bàn giao | Trạng thái |
|---|---|
| RD màn hình | Có tài liệu này |
| BD màn hình | `02-bd/screens/users/USR0105_workspace_home.md` (V0.1, chưa duyệt hình) |
| DD/API liên quan | Chưa có — không tự suy luận; chờ `03-dd/api/judge-orchestration.md` |

---

## 8. Kế hoạch và quy mô

| Chỉ số | Giá trị |
|---|---|
| Quy mô hành vi | 9 yêu cầu (REQ-01 đến REQ-09), xem chương 5. |
| Lịch, nhân sự, công sức | Chưa xác định — không tự suy luận. |

---

## 9. Tham chiếu

| Loại | Tham chiếu |
| :--- | :--- |
| Quyết định | `DEC-2026-1020-student-workspace-split-and-password-in-settings` — `.nexa/control/decision-registry.md:4233-4262`. |
| Màn liên quan | `01-rd/screens/users/USR0101_problem_list.md` (khối "Bài đang làm dở", nút "Bài ngẫu nhiên"); `01-rd/screens/users/USR0102_problem_detail.md` (đích `/workspace/{id}`). |
| Khung điều hướng | `02-bd/screens/users/_shell.md` mục 2.2 (mục "Workspace") và mục 6 Q2 (phần chuyển hướng nay bị thay). |
| Khảo sát hệ thống | `01-rd/overview/system_survey.md` mục 7.1 (chưa có dòng `workspace_home`). |
| Bản dựng | `05-coding/frontend/src/views/users/workspace-home/ui/workspace-home-view.tsx`; `05-coding/frontend/src/app/(student)/workspace/page.tsx`; i18n `workspaceHome` (`messages/vi.json:2951-2957`). |
