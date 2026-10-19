# BD — Khung điều hướng dùng chung khu Người học (`_shell`)

> **Cập nhật 2026-10-18 (Claude):** ghi các quyết định owner `DEC-2026-1018-usr-open-questions-phase1-scope` và `DEC-2026-1018-usr-open-questions-phase2-5`: đóng Q3 (chân trang câu trạng thái tĩnh), Q4 (bắt buộc đăng nhập), Q5 (hamburger dưới ngưỡng hẹp); sửa mục 3 theo. File này không có bảng lịch sử sửa đổi, nên ghi chú cập nhật nằm ở đây.

> **Cập nhật 2026-10-19 (Claude):** rà soát bản dựng. (1) Ngăn kéo nav (hamburger) và menu avatar đóng bằng phím Escape và trả focus về nút mở; menu avatar là danh sách liên kết thường, không còn `role="menu"` vì chưa có điều hướng bằng phím mũi tên; lớp phủ bấm-để-đóng nằm ngoài thứ tự Tab (`widgets/app-shell/ui/student-header.tsx`). (2) Thẻ "GO-JUDGE" ở header và chân trang dùng chung một component `judge-engine-tag.tsx`. (3) **Ngoại lệ chân trang:** màn `problem_detail` gắn `data-fullscreen-view` nên chân trang bị ẩn từ `lg` trở lên (`globals.css:684-712`); owner chốt giữ, ghi ở `USR0102` mục 4.4 điểm lệch (4). Dưới `lg` chân trang vẫn hiện.
>
> Không phải một màn trong `01-rd/screens/` — đây là **khung bao** mà mọi màn của khu Người học (A1) dùng
> chung, nên không cấp mã màn và giữ nguyên tên file
> [Nguồn: 02-bd/_rules/bd-template-9sheet.md:174-175]. Vì không phải màn, file này **không** theo mẫu
> 9 sheet; nó theo đúng cấu trúc đã dùng cho `02-bd/screens/admin/_shell.md` và
> `02-bd/screens/teacher/_shell.md`.
>
> Viết **trước** 12 file BD màn của khu này, cùng lý do đã áp cho khu Giảng viên: khu Admin từng làm ngược
> thứ tự và phải tách khung ra muộn [Nguồn: 02-bd/screens/admin/_shell.md:7-10].
>
> Bằng chứng layout: 11 file `.dc.html` của khu Người học, phần `<header>` và `<footer>`.
> Hiện thực: chưa có khung riêng — `app-shell.tsx` mới chỉ có nhánh riêng cho `admin`, `student` đang
> dùng shell header-only tạm. Mọi mô tả dưới đây là **thiết kế cho UI sắp dựng**.

## 1. Phạm vi

Khung này là **kiểu thứ ba** trong dự án, khác cả hai khung đã có:

| Khu | Kiểu khung | Điều hướng |
| :--- | :--- | :--- |
| Admin | Sidebar dọc + toolbar trong `<main>` | 4 nhóm gập, 11 đích |
| Giảng viên | Sidebar dọc, không toolbar | Danh sách phẳng 5 mục |
| **Người học** | **Header ngang dính trên, KHÔNG có sidebar** | 6 mục trên thanh ngang + menu người dùng 5 mục |

Không một file `.dc.html` nào của khu Người học có thẻ `<aside>`; cả 11 file đều có đúng một `<header>`.

Khung gồm ba phần, áp cho mọi màn của khu này:

| Phần | File hiện thực dự kiến | Vai trò |
| :--- | :--- | :--- |
| Header | `widgets/app-shell/ui/student-header.tsx` | Thương hiệu + 6 mục nav + menu người dùng |
| Chân trang | `widgets/app-shell/ui/app-footer.tsx` | Bản quyền, trạng thái cụm go-judge, phiên bản |
| Nền và scope màu | `widgets/app-shell/ui/app-shell.tsx` + `app/globals.css` | Scope màu riêng của khu Người học |

## 2. Header

### 2.1. Cấu trúc

Một hàng ngang, `position: sticky` bám đỉnh trang
[Nguồn: 09-layoutBase/Ngân hàng bài toán.dc.html:51]. Ba khối:

| Khối | Nội dung |
| :--- | :--- |
| Thương hiệu | "AlgoPrep" kèm thẻ phụ "GO-JUDGE" |
| Nav chính | 5 mục chính, xem mục 2.2 |
| Chuyển theme & ngôn ngữ | Hai nút chuyển nhanh Sáng/Tối và VI/EN đặt trước avatar (chốt 2026-09-22 bởi `DEC-2026-0922-users-and-admin-conflict-resolutions`) |
| Menu người dùng | Avatar chữ cái + tên + email, mở ra 5 mục, xem mục 2.3 |

### 2.2. Năm mục nav chính

> **Đã chốt 2026-09-22** (`DEC-2026-0922-users-and-admin-conflict-resolutions`): Bỏ mục "Tổng quan" khỏi thanh nav (trong prototype chỉ trỏ file bản mẫu tĩnh, không có slug). `problem_list` là cửa vào chính của khu Người học; `my_progress` đóng vai trò dashboard cá nhân trong menu người dùng.

| Thứ tự | Nhãn | Slug đích | Ghi chú |
| :-: | :--- | :--- | :--- |
| 1 | Bài toán | `problem_list` | Cửa vào chính của khu Người học |
| 2 | Workspace | `problem_detail` | Vào thẳng màn giải bài; mở bài nộp dở gần nhất, không có thì về `problem_list` |
| 3 | Bài đã nộp | `my_submissions` | Lịch sử nộp bài |
| 4 | Phỏng vấn giả lập | `mock_interview` | Luyện phỏng vấn 1:1 với AI |
| 5 | Câu hỏi phỏng vấn | `interview_bank_list` | Ngân hàng câu hỏi F6 |

### 2.3. Menu người dùng

Năm mục: Trang cá nhân (`profile`) · Tiến độ của tôi (`my_progress`) · Bài đã lưu (`saved_problems`) ·
Cài đặt (`settings`) · Đăng xuất.

Đây là lý do 4 màn cá nhân **không** nằm trên thanh nav chính: chúng thuộc menu người dùng. Ba màn còn
lại của khu (`submission_result`, `solution_review`, `interview_question_detail`) không có mục nav nào —
đúng thiết kế, cả ba đều là màn khoan sâu từ màn khác.

### 2.4. Nhãn song ngữ trong prototype

Prototype hiển thị **cả hai** nhãn tiếng Việt và tiếng Anh cạnh nhau cho mọi mục
(`Tổng quan` / `Overview`, `Bài toán` / `Problems`...). Đây là cách bản mẫu minh hoạ i18n, **không** phải
yêu cầu hiển thị song ngữ cùng lúc trên UI thật `[SoT: Suy luận]`. UI thật hiển thị một ngôn ngữ theo
lựa chọn của người dùng, đúng cơ chế `next-intl` đã chốt ở `DEC-2026-0825-frontend-base-architecture`.

### 2.5. Ngoại lệ màn Workspace

`problem_detail` dùng **cùng** header đó nhưng thu gọn: giữ nguyên thương hiệu và 6 mục nav, lược phần
tên và email trong khối người dùng, chỉ còn avatar
[Nguồn: 09-layoutBase/Workspace giải bài.dc.html:49-72]. Lý do suy ra được: màn này cần tối đa chiều cao
cho vùng soạn thảo `[SoT: Suy luận]`.

Đây là khác biệt về **mật độ**, không phải khác cấu trúc — nên vẫn là một component, nhận một tham số
thu gọn, không tách thành header thứ hai.

## 3. Chân trang

Áp cho mọi màn của khu, chốt 2026-09-21 cùng tinh thần đã áp cho khu Admin
(`DEC-2026-0921-admin-shell-footer`) và khu Giảng viên
(`DEC-2026-0921-teacher-screens-conflict-resolutions`).

**Lưu ý về bằng chứng:** chỉ **1 trên 11** prototype của khu này có thẻ `<footer>`
(`Ngân hàng bài toán.dc.html:428`). Tỉ lệ đó thấp hơn cả khu Admin (8/9) và ngược với khu Giảng viên
(0/5). Quyết định dựng chân trang ở cả ba khu là quyết định về tính nhất quán, không phải kết luận rút
từ bằng chứng prototype — ghi rõ như vậy để sau này đọc lại không tưởng là đọc sót.

Ba nội dung, lấy từ prototype duy nhất có chân trang
[Nguồn: 09-layoutBase/Ngân hàng bài toán.dc.html:428-450]:

| Khu | Nội dung |
| :--- | :--- |
| Trái | "AlgoPrep" + thẻ "GO-JUDGE" + dòng bản quyền "© 2026 AlgoPrep — đồ án tốt nghiệp" |
| Giữa | Trạng thái cụm, câu tĩnh: "go-judge cluster hoạt động" (không có số hàng đợi, Q3) |
| Phải | Số phiên bản |

Prototype tĩnh có thêm **số bài đang chờ trong hàng đợi** ở dòng trạng thái (một con số thật đọc từ
`judge-orchestration`). **Bản thiết kế bỏ con số đó**: dòng trạng thái là câu tĩnh, không polling máy chủ
(đã chốt 2026-10-18, xem mục 6 Q3). Bản dựng khớp: `05-coding/frontend/src/widgets/app-shell/ui/student-footer.tsx:39-45`.

## 4. Vùng nội dung

Không có ràng buộc `max-width` dùng chung như hai khu kia: mỗi màn của khu Người học tự quyết chiều rộng
vì hình dạng rất khác nhau (danh sách bài toán dạng lưới, workspace chia đôi màn hình, trang cá nhân hẹp).
BD từng màn tự khai trong Sheet 4.4.

## 5. Bảng màu và token

Scope màu riêng của khu Người học, cùng cơ chế token ngữ nghĩa đã dùng cho `.admin-shell` và
`.instructor-shell`. Không quy định màu sắc, khoảng cách, typography trong BD này —
`09-layoutBase/**` là bằng chứng bố cục chỉ-đọc, không phải design system
[Nguồn: 02-bd/_rules/bd-template-9sheet.md:82].

## 6. Câu hỏi mở — **ĐÃ ĐÓNG CẢ 5 CÂU**

| # | Câu hỏi | Ghi chú & Phương án đã chốt | Chủ sở hữu |
| :-: | :--- | :--- | :--- |
| Q1 | **Mục nav "Tổng quan" trỏ đi đâu?** | **ĐÃ ĐÓNG 2026-09-22** (`DEC-2026-0922-users-and-admin-conflict-resolutions`): Bỏ hẳn mục này; `problem_list` làm cửa vào chính của khu Người học; dashboard nằm tại `my_progress`. | Đã đóng |
| Q2 | **Mục nav "Workspace" mở bài nào khi người dùng chưa chọn bài?** | **ĐÃ ĐÓNG 2026-09-22** (`DEC-2026-0922-users-and-admin-conflict-resolutions`): Mở bài đang làm dở gần nhất từ `judge.submissions`; nếu chưa làm bài nào thì chuyển về `problem_list`. | Đã đóng |
| Q3 | **Số "hàng đợi {n} bài" ở chân trang lấy ở đâu, làm mới thế nào?** | **Đã chốt 2026-10-18 (owner), theo đề xuất** (`DEC-2026-1018-usr-open-questions-phase2-5`): chân trang giữ câu trạng thái cụm **tĩnh** ("go-judge cluster hoạt động"), bỏ số đếm động để mọi màn không phải polling máy chủ. Tiêu chí ponytail bậc 5 (câu hằng thay cho một lời gọi lặp). Bản dựng đã khớp (`widgets/app-shell/ui/student-footer.tsx:39-45`). | Đã đóng |
| Q4 | **Khu Người học có cần trạng thái đăng nhập hay không trên khung?** | **ĐÃ ĐÓNG 2026-09-22** (`DEC-2026-0922-users-and-admin-conflict-resolutions`): Bắt buộc đăng nhập cho mọi tính năng ở đợt này; chưa đăng nhập chuyển hướng về `auth`. **Đã chốt lại 2026-10-18 (owner), theo đề xuất**: "chưa đăng nhập: bắt buộc đăng nhập", không có trạng thái khách, cùng câu trả lời với `02-bd/screens/users/USR0101_problem_list.md` Q1 (`DEC-2026-1018-usr-open-questions-phase1-scope`). Tiêu chí ponytail bậc 3 (không người dùng demo nào xem khu này với tư cách khách). | Đã đóng |
| Q5 | **Header có thu gọn trên màn hẹp không?** | **Đã chốt 2026-10-18 (owner), theo đề xuất**: dưới ngưỡng hẹp (dưới `lg`) gom các mục nav vào menu hamburger, giữ thương hiệu và khối người dùng. Tiêu chí ponytail bậc 5 (một ngưỡng responsive thay cho cấu hình). Bản dựng đã khớp: nút hamburger chỉ hiện dưới `lg` (`student-header.tsx:41-49`), nav ngang ẩn dưới `lg` (`:63-66`), ngăn kéo nav mở bằng `navOpen` (`:149-158`) và thương hiệu cùng khối người dùng vẫn nằm trên thanh. | Đã đóng |

## 7. Tham chiếu

- `09-layoutBase/Ngân hàng bài toán.dc.html:51,428` — bằng chứng header và chân trang.
- `09-layoutBase/Workspace giải bài.dc.html:49-72` — bằng chứng biến thể header thu gọn.
- `02-bd/screens/admin/_shell.md` mục 9 · `02-bd/screens/teacher/_shell.md` mục 9 — hai khung còn lại.
- `02-bd/_rules/bd-template-9sheet.md` — quy ước mẫu 9 sheet cho 12 file BD màn của khu này.
- Quyết định: `DEC-2026-0825-frontend-base-architecture`, `DEC-2026-0921-admin-shell-footer`,
  `DEC-2026-0921-teacher-screens-conflict-resolutions`, `DEC-2026-0922-users-and-admin-conflict-resolutions`.

