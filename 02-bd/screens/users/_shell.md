# BD — Khung điều hướng dùng chung khu Người học (`_shell`)

> **Cập nhật 2026-11-26 (AI), phiên bản ghi chú V0.7:** theo `DEC-2026-1126-guest-browse-published-problems` (owner 2026-11-26, sửa một phần Q4 của `DEC-2026-1018-usr-open-questions-phase1-scope`), khung có **trạng thái khách**, xem mục 2.5A. Khách thấy đủ bảy mục nav; bấm mục cần phiên thì mở hộp thoại nhắc đăng nhập; khối người dùng ở góc phải thành nút Đăng nhập. Chỉ `/problems` và `/problems/{id}` mở cho khách; mọi đường dẫn khác của khu vẫn cần phiên. **Đã dựng 2026-11-26** (kế hoạch `06-plan/nexa-plan/261126-1108-khach-vang-lai-xem-bai.md`; kiểm trình duyệt `06-plan/reports/261126-1421-khach-vang-lai/report.md`; **owner chưa duyệt hình**).

> **Cập nhật 2026-11-23 (AI), phiên bản ghi chú V0.6:** theo yêu cầu owner 2026-11-23, sửa trình bày chân trang và mô hình cuộn của khu Người học (bản dựng; **Chưa duyệt hình**). (1) Chân trang nay **một dòng** (thương hiệu + thẻ GO-JUDGE, bản quyền, trạng thái cụm, phiên bản), cao khoảng 41px, trước đây khối hai dòng cao 79px; nội dung và ba vùng không đổi. (2) Khu Người học **cuộn theo trang** (cuộn của tài liệu, thanh cuộn của trình duyệt) và chân trang nối đuôi nội dung; không còn ghim dưới một hộp cuộn bên trong. Admin và Giảng viên giữ hộp cuộn trong. Màn toàn khung (Workspace, Mock Interview đang chạy, `data-fullscreen-view`) vẫn bị buộc vào khung nhìn và ẩn chân trang. Sửa mục 3 (thêm đoạn "Hiện trạng bản dựng 2026-11-23"). Câu hỏi mở của khung: không đổi; một lưu ý mới ghi ở cuối mục 3 (header dính chồng nhẹ lên nội dung khi cuộn), chưa quyết.

> **Cập nhật 2026-11-18 (Claude), phiên bản ghi chú V0.5:** áp `DEC-2026-1117-user-avatar-upload` (F1-35). Nút mở menu người dùng ở header nay hiện ảnh đại diện thật của học viên (hoặc chữ viết tắt khi chưa có ảnh hoặc ảnh lỗi); sửa dòng "Menu người dùng" ở mục 2.1. Chưa duyệt hình. Câu hỏi mở còn lại của khung: không có (mục 6 đã đóng cả 5 câu).

> **Cập nhật 2026-10-23 (Claude), phiên bản ghi chú V0.4:** ghi quyết định owner `DEC-2026-1023-student-class-screens` (owner yêu cầu 2026-10-23: giao diện học viên chưa có màn nào cho lớp). Thanh nav chính có **bảy mục**: thêm mục thứ 7 "Lớp của tôi" dẫn tới `my_classes` (`USR0701`, `/classes`), đặt ở **header, không đặt trong menu avatar**; mục sáng với mọi đường dẫn bắt đầu bằng `/classes/` (khớp tiền tố, `widgets/app-shell/ui/student-header.tsx:51-52`), gồm cả chi tiết lớp `my_class_detail` (`USR0702`, `/classes/{classId}`). Sửa mục 1, 2.1, 2.2 (tiêu đề, bảng, ghi chú), 2.3 và 2.5. Ngăn kéo hamburger dưới `lg` dùng chung danh sách `STUDENT_NAV_ITEMS` nên tự có mục thứ 7 (`student-header.tsx:83,177`), không có đoạn riêng liệt kê mục trong file này. Nguồn bản dựng: `widgets/app-shell/model/student-nav.ts:42-43`, nhãn `messages/vi.json:44`. Chưa duyệt hình.

> **Cập nhật 2026-10-20 (Claude), phiên bản ghi chú V0.3:** ghi quyết định owner `DEC-2026-1020-student-workspace-split-and-password-in-settings` [Nguồn: .nexa/control/decision-registry.md:4233-4261]. (1) Mục nav "Workspace" nay dẫn tới trang đầu `/workspace` (màn mới `USR0105`, slug `workspace_home`), **không còn chuyển hướng** vào bài đang làm dở; màn soạn mã `USR0102` chuyển sang `/workspace/{problemId}` và thanh nav vẫn sáng mục Workspace cho mọi đường dẫn bắt đầu bằng `/workspace/` (khớp tiền tố, `widgets/app-shell/ui/student-header.tsx:51-52`). Trang chỉ-đọc `/problems/{problemId}` là màn mới `USR0104` (slug `problem_overview`). Sửa mục 2.2, 2.3, 2.5, mục 6 Q2. (2) Thêm mục 5.1 ghi bộ token nền sáng của khu Người học (`app/globals.css:654-673`). (3) Sửa các dòng trích dẫn mã đã lệch: `globals.css:705-733` (trước ghi `684-712`), `student-header.tsx` và `student-footer.tsx` ở Q3, Q5 và mục 3. Chưa duyệt hình cho bảng token mục 5.1. File này không có bảng lịch sử sửa đổi (xem ghi chú ngay dưới), nên mốc phiên bản ghi ở đây.

> **Cập nhật 2026-10-18 (Claude):** ghi các quyết định owner `DEC-2026-1018-usr-open-questions-phase1-scope` và `DEC-2026-1018-usr-open-questions-phase2-5`: đóng Q3 (chân trang câu trạng thái tĩnh), Q4 (bắt buộc đăng nhập), Q5 (hamburger dưới ngưỡng hẹp); sửa mục 3 theo. File này không có bảng lịch sử sửa đổi, nên ghi chú cập nhật nằm ở đây.

> **Cập nhật 2026-10-19 (Claude):** rà soát bản dựng. (1) Ngăn kéo nav (hamburger) và menu avatar đóng bằng phím Escape và trả focus về nút mở; menu avatar là danh sách liên kết thường, không còn `role="menu"` vì chưa có điều hướng bằng phím mũi tên; lớp phủ bấm-để-đóng nằm ngoài thứ tự Tab (`widgets/app-shell/ui/student-header.tsx`). (2) Thẻ "GO-JUDGE" ở header và chân trang dùng chung một component `judge-engine-tag.tsx`. (3) **Ngoại lệ chân trang:** màn `problem_detail` gắn `data-fullscreen-view` nên chân trang bị ẩn từ `lg` trở lên (`globals.css:705-733`); owner chốt giữ, ghi ở `USR0102` mục 4.4 điểm lệch (4). Dưới `lg` chân trang vẫn hiện.
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
| **Người học** | **Header ngang dính trên, KHÔNG có sidebar** | 7 mục trên thanh ngang + menu người dùng 5 mục |

Không một file `.dc.html` nào của khu Người học có thẻ `<aside>`; cả 11 file đều có đúng một `<header>`.

Khung gồm ba phần, áp cho mọi màn của khu này:

| Phần | File hiện thực dự kiến | Vai trò |
| :--- | :--- | :--- |
| Header | `widgets/app-shell/ui/student-header.tsx` | Thương hiệu + 7 mục nav + menu người dùng |
| Chân trang | `widgets/app-shell/ui/app-footer.tsx` | Bản quyền, trạng thái cụm go-judge, phiên bản |
| Nền và scope màu | `widgets/app-shell/ui/app-shell.tsx` + `app/globals.css` | Scope màu riêng của khu Người học |

## 2. Header

### 2.1. Cấu trúc

Một hàng ngang, `position: sticky` bám đỉnh trang
[Nguồn: 09-layoutBase/Ngân hàng bài toán.dc.html:51]. Ba khối:

| Khối | Nội dung |
| :--- | :--- |
| Thương hiệu | "AlgoPrep" kèm thẻ phụ "GO-JUDGE" |
| Nav chính | 7 mục chính, xem mục 2.2 |
| Chuyển theme & ngôn ngữ | Hai nút chuyển nhanh Sáng/Tối và VI/EN đặt trước avatar (chốt 2026-09-22 bởi `DEC-2026-0922-users-and-admin-conflict-resolutions`) |
| Menu người dùng | Ảnh đại diện (hoặc chữ viết tắt) + tên + email, mở ra 5 mục, xem mục 2.3. Trước khi hồ sơ tải xong hiện biểu tượng người chung, không bịa tên; chưa có ảnh hoặc ảnh tải lỗi thì hiện chữ viết tắt, không hiện biểu tượng ảnh hỏng (V0.5, F1-35). Nguồn: `avatarVersion` của hồ sơ, hợp đồng ở `03-dd/api/F1_identity.md` mục 0.9.5. Hiện trạng bản dựng (chưa duyệt hình): `05-coding/frontend/src/widgets/app-shell/ui/student-header.tsx:126-142` |

### 2.2. Bảy mục nav chính

> **Đã chốt 2026-09-22** (`DEC-2026-0922-users-and-admin-conflict-resolutions`): Bỏ mục "Tổng quan" khỏi thanh nav (trong prototype chỉ trỏ file bản mẫu tĩnh, không có slug). `problem_list` là cửa vào chính của khu Người học; `my_progress` đóng vai trò dashboard cá nhân trong menu người dùng.

> **Cập nhật 2026-10-21:** mục "Tổng quan" đã trở lại làm mục đầu (`DEC-2026-0927-student-dashboard-home`), nên nav có sáu mục. Đoạn "Đã chốt 2026-09-22" ở trên chỉ còn là lịch sử.

> **Cập nhật 2026-10-23:** thêm mục thứ 7 "Lớp của tôi" (`DEC-2026-1023-student-class-screens`), nav có **bảy mục**.

| Thứ tự | Nhãn | Slug đích | Ghi chú |
| :-: | :--- | :--- | :--- |
| 1 | Tổng quan | `dashboard` (`USR0601`) | Trang chủ cá nhân của học viên; mục này đã trở lại thanh nav theo `DEC-2026-0927-student-dashboard-home` (thay nửa quyết định 2026-09-22 bỏ mục "Tổng quan"), `my_progress` đã gộp vào đây theo `DEC-2026-0927-student-area-merge-and-shared-shell`. Đồng bộ với `student-nav.ts` 2026-10-21 |
| 2 | Bài toán | `problem_list` | Cửa vào chính của khu Người học |
| 3 | Workspace | `workspace_home` (`USR0105`, `/workspace`) | Trang đầu Workspace: các bài đang làm dở, chọn bài, bài ngẫu nhiên (`DEC-2026-1020-student-workspace-split-and-password-in-settings`). Mục sáng với mọi đường dẫn `/workspace/*`, gồm màn soạn mã `problem_detail` (`USR0102`, `/workspace/{problemId}`). **Không còn chuyển hướng** sang bài dở gần nhất (thay phần Q2 cũ, xem mục 6) |
| 4 | Bài đã nộp | `my_submissions` | Lịch sử nộp bài |
| 5 | Phỏng vấn giả lập | `mock_interview` | Luyện phỏng vấn 1:1 với AI |
| 6 | Câu hỏi phỏng vấn | `interview_bank_list` | Ngân hàng câu hỏi F6 |
| 7 | Lớp của tôi | `my_classes` (`USR0701`, `/classes`) | Các lớp học viên đang tham gia và khung nhập mã mời (F1-23). Owner chốt 2026-10-23: nằm ở header, **không** trong menu avatar. Mục sáng với mọi đường dẫn `/classes/*`, gồm chi tiết lớp `my_class_detail` (`USR0702`, `/classes/{classId}`). Nhãn khoá `nav.myClasses` (`messages/vi.json:44`); `student-nav.ts:42-43` |

### 2.3. Menu người dùng

Năm mục: Trang cá nhân (`profile`) · Tiến độ của tôi (`my_progress`) · Bài đã lưu (`saved_problems`) ·
Cài đặt (`settings`) · Đăng xuất.

Đây là lý do 4 màn cá nhân **không** nằm trên thanh nav chính: chúng thuộc menu người dùng. Ba màn còn
lại của khu (`submission_result`, `solution_review`, `interview_question_detail`) không có mục nav nào —
đúng thiết kế, cả ba đều là màn khoan sâu từ màn khác. Từ 2026-10-23 thêm nhóm "Lớp học": `my_classes` có mục nav riêng (mục 7, mục 2.2); `my_class_detail` là màn khoan sâu từ `my_classes`, không có mục nav riêng nhưng làm sáng mục "Lớp của tôi" (khớp tiền tố `/classes`). Từ 2026-10-20 thêm hai màn mới quanh Workspace:
`problem_overview` (`USR0104`, `/problems/{problemId}`, chỉ-đọc, là đích của mọi liên kết "xem bài" và có nút
"Giải bài toán" sang `/workspace/{problemId}`) là màn khoan sâu, không có mục nav riêng và không làm sáng mục
nào ngoài "Bài toán" (khớp tiền tố `/problems`); `workspace_home` (`USR0105`) là đích của mục nav "Workspace".

### 2.4. Nhãn song ngữ trong prototype

Prototype hiển thị **cả hai** nhãn tiếng Việt và tiếng Anh cạnh nhau cho mọi mục
(`Tổng quan` / `Overview`, `Bài toán` / `Problems`...). Đây là cách bản mẫu minh hoạ i18n, **không** phải
yêu cầu hiển thị song ngữ cùng lúc trên UI thật `[SoT: Suy luận]`. UI thật hiển thị một ngôn ngữ theo
lựa chọn của người dùng, đúng cơ chế `next-intl` đã chốt ở `DEC-2026-0825-frontend-base-architecture`.

### 2.5. Ngoại lệ màn Workspace

`problem_detail` (nay ở `/workspace/{problemId}`) dùng **cùng** header đó nhưng thu gọn: giữ nguyên thương hiệu và 7 mục nav, lược phần
tên và email trong khối người dùng, chỉ còn avatar
[Nguồn: 09-layoutBase/Workspace giải bài.dc.html:49-72]. Lý do suy ra được: màn này cần tối đa chiều cao
cho vùng soạn thảo `[SoT: Suy luận]`.

Đây là khác biệt về **mật độ**, không phải khác cấu trúc — nên vẫn là một component, nhận một tham số
thu gọn, không tách thành header thứ hai. Hai màn mới `problem_overview` và `workspace_home` là trang cuộn thường, **không** dùng ngoại lệ này `[SoT: Suy luận]` — chỉ `problem_detail` gắn `data-fullscreen-view` trong nhóm màn bài toán (`05-coding/frontend/src/views/users/problem-detail/ui/problem-detail-view.tsx:778`).

### 2.5A. Trạng thái khách (chưa đăng nhập) — `DEC-2026-1126-guest-browse-published-problems`

Bảng sau là hành vi của khung khi **không có phiên**. Phiên hết hạn giữa chừng cũng rơi về trạng thái này ở hai đường dẫn công khai; ở đường dẫn còn lại đi theo luồng chuyển về `auth` hiện hành.

| Vùng | Khách thấy | Nguồn dữ liệu |
| :--- | :--- | :--- |
| Nav chính (bảy mục) | Hiện đủ. Mục `Bài toán` mở thẳng. Sáu mục còn lại (Tổng quan, Workspace, Bài đã nộp, Phỏng vấn giả lập, Câu hỏi phỏng vấn, Lớp của tôi) khi bấm **không chuyển trang** mà mở hộp thoại "Cần đăng nhập" | Nhãn i18n tĩnh |
| Menu người dùng (mục 2.3) | Thay bằng nút **Đăng nhập** ở đúng vị trí đó; không có tên, email, ảnh hay năm mục menu | Nhãn i18n tĩnh |
| Hộp thoại "Cần đăng nhập" | Một câu giải thích ("Bạn cần đăng nhập để dùng tính năng này. Đăng nhập xong bạn sẽ được đưa tới đúng trang cần dùng."), nút Đăng nhập, nút Đăng ký, nút Để sau (đóng, giữ nguyên trang). Đăng nhập và Đăng ký đều sang `auth` kèm `?redirect=` là đường dẫn hiện tại; sau khi xác thực thành công `auth` đưa về đúng trang đó (cơ chế đã có ở `01-rd/screens/shared/SHR0101_auth.md` Q3, lọc bằng `isSafeRedirect`). Với mục nav, đường dẫn quay lại là **trang đích của mục vừa bấm** | Nhãn i18n tĩnh |
| Thương hiệu trên màn hẹp (dưới `sm`) | Chỉ khi là khách: ẩn chữ "AlgoPrep", giữ logo (trình đọc màn hình vẫn đọc tên), để nút Đăng nhập không bị đẩy ra ngoài màn 390px | Tĩnh |
| Chuyển theme và ngôn ngữ | Giữ nguyên | Trình duyệt |
| Chân trang | Giữ nguyên | Tĩnh |

Một thành phần hộp thoại dùng chung cho khung và cho `USR0104` (nút Giải bài, Lưu bài). Đường dẫn khách vào thẳng bằng liên kết mà **không** thuộc hai trang công khai (ví dụ `/workspace/{id}`, `/classes`) vẫn bị chuyển về `auth` kèm `?redirect=`; hộp thoại chỉ dành cho thao tác bấm trên trang công khai. Trang gốc `/`: khách vào `/problems`, người đã đăng nhập vào trang theo vai trò.

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
(đã chốt 2026-10-18, xem mục 6 Q3). Bản dựng khớp: `05-coding/frontend/src/widgets/app-shell/ui/student-footer.tsx:40-46`.

### Hiện trạng bản dựng 2026-11-23 (**Chưa duyệt hình**)

Bảng 3.1 ghi hai thay đổi trình bày đã dựng ngày 2026-11-23 theo yêu cầu owner; bảng "Trái / Giữa / Phải" ở trên vẫn đúng về nội dung nhưng **không còn là ba cột trên hai dòng** mà là ba vùng trên một hàng.

Bảng 3.1: Hiện trạng chân trang và mô hình cuộn khu Người học

| Nội dung | Hiện trạng bản dựng | Nguồn |
| :--- | :--- | :--- |
| Chân trang một dòng | Thương hiệu + thẻ GO-JUDGE, dòng bản quyền, trạng thái cụm, phiên bản nằm cùng một hàng `flex-wrap`; phiên bản đẩy sang phải (`ml-auto`). Bản quyền chuyển lên cạnh thương hiệu thay vì xuống dòng thứ hai; chỉ xuống dòng khi cửa sổ quá hẹp. Cao khoảng 41px (trước đó 79px, đo ở 1280x640 và 1440x900) | `05-coding/frontend/src/widgets/app-shell/ui/student-footer.tsx:27-52` |
| Khu Người học cuộn theo trang | Các quy tắc khoá khung nhìn và hộp cuộn trong chỉ áp cho `.app-shell-viewport:not(.student-shell)` (Admin, Giảng viên); khu Người học dùng cuộn của tài liệu, chân trang nối sau nội dung. Đo ở 1280x640 trên trang Tổng quan: chiều cao tài liệu 2560, chân trang ở cuối, không có hộp cuộn trong | `05-coding/frontend/src/app/globals.css:712-749` |
| Màn toàn khung vẫn buộc vào khung nhìn | Màn gắn `data-fullscreen-view` đặt chiều cao bằng khung nhìn, ẩn chân trang (từ `lg` trở lên). Đo ở 1280x640: Workspace cao 640 bằng cửa sổ | `05-coding/frontend/src/app/globals.css:756-783` |
| Vị trí chân trang trong khung | Chân trang nằm ngoài `<main>`, trong `PageContainer.student-footer-wrap` | `05-coding/frontend/src/widgets/app-shell/ui/app-shell.tsx:106-108` |

Phần trên thay thế cách hiểu cũ "chân trang ghim dưới hộp cuộn trong" của khu này; đoạn cũ không còn đúng cho Người học và giữ lại ở đây làm lịch sử. Lưu ý chưa quyết (không đổi thiết kế): khi cuộn trang, header dính bám đỉnh chồng nhẹ lên nội dung.

## 4. Vùng nội dung

Không có ràng buộc `max-width` dùng chung như hai khu kia: mỗi màn của khu Người học tự quyết chiều rộng
vì hình dạng rất khác nhau (danh sách bài toán dạng lưới, workspace chia đôi màn hình, trang cá nhân hẹp).
BD từng màn tự khai trong Sheet 4.4.

## 5. Bảng màu và token

Scope màu riêng của khu Người học, cùng cơ chế token ngữ nghĩa đã dùng cho `.admin-shell` và
`.instructor-shell`. Không quy định màu sắc, khoảng cách, typography trong BD này —
`09-layoutBase/**` là bằng chứng bố cục chỉ-đọc, không phải design system
[Nguồn: 02-bd/_rules/bd-template-9sheet.md:82].

### 5.1. Token nền sáng của khu Người học (owner 2026-10-20, **Chưa duyệt hình**)

Owner chỉnh nền sáng của khu Người học 2026-10-20 vì các thẻ gần như dính vào nền trắng của trang. Bảng 5.1 ghi
các token đã dựng ở khối `.student-shell` của `app/globals.css`; đây là **bằng chứng bản dựng** để các BD màn
trích khi cần, không phải design system và không phủ nền tối (nền tối giữ nguyên, khối `.dark .student-shell` ở
`app/globals.css:675-697`) [Nguồn: 05-coding/frontend/src/app/globals.css:654-673].

Bảng 5.1: Token nền sáng của khu Người học

| Token | Giá trị | Dùng cho | Nguồn |
| :--- | :--- | :--- | :--- |
| `--color-background` | `#eaeff6` | Nền trang, tông xám lạnh thay cho nền trắng | `app/globals.css:660` |
| `--color-surface` | `#ffffff` | Thẻ và bảng, trắng đặc | `app/globals.css:661` |
| `--color-surface-hover` | `#e3e9f2` | Trạng thái rê chuột trên bề mặt | `app/globals.css:662` |
| `--color-row-hover` | `#eef2f8` | Hàng bảng khi rê chuột | `app/globals.css:663` |
| `--color-border` | `#cbd5e1` | Viền thẻ, đậm hơn bản cũ để thẻ nổi khỏi nền | `app/globals.css:664` |
| `--color-track` | `#d5dde9` | Rãnh thanh tiến độ | `app/globals.css:665` |
| Nền `.glass-card` | `color-mix` 96% `--color-surface` | Thẻ gần đặc, không còn trong mờ 55% | `app/globals.css:668-669` |
| Bóng `.glass-card` | `0 1px 2px rgba(15,23,42,.08)`, `0 8px 24px rgba(15,23,42,.08)` | Thẻ nổi nhẹ khỏi nền | `app/globals.css:670-672` |

Giá trị cũ (`--color-surface` `#f8fafc` trên nền `#ffffff`, nền thẻ 55%) lấy từ chú thích trong mã
[Nguồn: 05-coding/frontend/src/app/globals.css:655-657]. Admin và Giảng viên không đổi vì họ ghi đè các token này
bằng khối `.admin-shell` và `.instructor-shell` riêng `[SoT: Suy luận]` theo chú thích ở `app/globals.css:650-652`.

## 6. Câu hỏi mở — **ĐÃ ĐÓNG CẢ 5 CÂU**

| # | Câu hỏi | Ghi chú & Phương án đã chốt | Chủ sở hữu |
| :-: | :--- | :--- | :--- |
| Q1 | **Mục nav "Tổng quan" trỏ đi đâu?** | **ĐÃ ĐÓNG 2026-09-22** (`DEC-2026-0922-users-and-admin-conflict-resolutions`): Bỏ hẳn mục này; `problem_list` làm cửa vào chính của khu Người học; dashboard nằm tại `my_progress`. | Đã đóng |
| Q2 | **Mục nav "Workspace" mở bài nào khi người dùng chưa chọn bài?** | **ĐÃ ĐÓNG 2026-09-22** (`DEC-2026-0922-users-and-admin-conflict-resolutions`): Mở bài đang làm dở gần nhất từ `judge.submissions`; nếu chưa làm bài nào thì chuyển về `problem_list`. **Phần chuyển hướng này đã được thay thế 2026-10-20** bởi `DEC-2026-1020-student-workspace-split-and-password-in-settings`: mục nav "Workspace" mở trang đầu `/workspace` (`USR0105`, `02-bd/screens/users/USR0105_workspace_home.md`), nơi liệt kê các bài làm dở để chọn tiếp và có "Bài ngẫu nhiên"; không còn tự nhảy vào một bài. Câu trả lời gốc giữ lại làm lịch sử. | Đã đóng (phần chuyển hướng đã thay) |
| Q3 | **Số "hàng đợi {n} bài" ở chân trang lấy ở đâu, làm mới thế nào?** | **Đã chốt 2026-10-18 (owner), theo đề xuất** (`DEC-2026-1018-usr-open-questions-phase2-5`): chân trang giữ câu trạng thái cụm **tĩnh** ("go-judge cluster hoạt động"), bỏ số đếm động để mọi màn không phải polling máy chủ. Tiêu chí ponytail bậc 5 (câu hằng thay cho một lời gọi lặp). Bản dựng đã khớp (`widgets/app-shell/ui/student-footer.tsx:40-46`). | Đã đóng |
| Q4 | **Khu Người học có cần trạng thái đăng nhập hay không trên khung?** | **ĐÃ ĐÓNG 2026-09-22** (`DEC-2026-0922-users-and-admin-conflict-resolutions`): Bắt buộc đăng nhập cho mọi tính năng ở đợt này; chưa đăng nhập chuyển hướng về `auth`. **Đã chốt lại 2026-10-18 (owner), theo đề xuất**: "chưa đăng nhập: bắt buộc đăng nhập", không có trạng thái khách, cùng câu trả lời với `02-bd/screens/users/USR0101_problem_list.md` Q1 (`DEC-2026-1018-usr-open-questions-phase1-scope`). Tiêu chí ponytail bậc 3 (không người dùng demo nào xem khu này với tư cách khách). **Đổi một phần 2026-11-26 (`DEC-2026-1126-guest-browse-published-problems`):** `/problems` và `/problems/{id}` mở cho khách; khung có trạng thái khách (mục 2.5A). Phần "mọi tính năng khác bắt buộc đăng nhập" giữ nguyên. | Đã đóng |
| Q5 | **Header có thu gọn trên màn hẹp không?** | **Đã chốt 2026-10-18 (owner), theo đề xuất**: dưới ngưỡng hẹp (dưới `lg`) gom các mục nav vào menu hamburger, giữ thương hiệu và khối người dùng. Tiêu chí ponytail bậc 5 (một ngưỡng responsive thay cho cấu hình). Bản dựng đã khớp: nút hamburger chỉ hiện dưới `lg` (`student-header.tsx:59-67`), nav ngang ẩn dưới `lg` (`:79-82`), ngăn kéo nav mở bằng `navOpen` (`:167-175`) và thương hiệu cùng khối người dùng vẫn nằm trên thanh. **Bổ sung 2026-11-14 (F-03):** dưới `sm` (390 px) thẻ GO-JUDGE ẩn, nút Sáng/Tối chỉ còn biểu tượng (nhãn nằm ở `title`/`aria-label`, prop `iconOnlyBelowSm` của `ThemeLangSwitcher`), khoảng cách và lề thanh thu lại; thanh không còn tràn ngang. Số dòng trích dẫn `student-header.tsx` ở trên đã lệch vài dòng, chưa soát lại. | Đã đóng |

## 7. Tham chiếu

- `09-layoutBase/Ngân hàng bài toán.dc.html:51,428` — bằng chứng header và chân trang.
- `09-layoutBase/Workspace giải bài.dc.html:49-72` — bằng chứng biến thể header thu gọn.
- `02-bd/screens/admin/_shell.md` mục 9 · `02-bd/screens/teacher/_shell.md` mục 9 — hai khung còn lại.
- `02-bd/_rules/bd-template-9sheet.md` — quy ước mẫu 9 sheet cho các file BD màn của khu này.
- `05-coding/frontend/src/app/globals.css:654-697` — token nền sáng và nền tối của `.student-shell` (mục 5.1).
- Quyết định: `DEC-2026-1023-student-class-screens`, `DEC-2026-1020-student-workspace-split-and-password-in-settings`, `DEC-2026-0825-frontend-base-architecture`, `DEC-2026-0921-admin-shell-footer`,
  `DEC-2026-0921-teacher-screens-conflict-resolutions`, `DEC-2026-0922-users-and-admin-conflict-resolutions`.

