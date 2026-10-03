# BD — Khung điều hướng dùng chung khu Giảng viên (`_shell`)

> Không phải một màn trong `01-rd/screens/` — đây là **khung bao** mà mọi route `/instructor/*` dùng
> chung, nên không cấp mã màn và giữ nguyên tên file
> [Nguồn: 02-bd/_rules/bd-template-9sheet.md:174-175]. Vì không phải màn, file này **không** theo mẫu
> 9 sheet; nó theo đúng cấu trúc đã dùng cho `02-bd/screens/admin/_shell.md`.
>
> Viết **trước** 6 file BD màn Giảng viên, có chủ đích: khu Admin đã làm ngược thứ tự này — 8 file BD
> mỗi file tự mô tả lại sidebar/toolbar, đến 2026-09-17 mới tách được khung chung và chính file đó ghi
> nhận là muộn [Nguồn: 02-bd/screens/admin/_shell.md:7-10]. Làm khung trước thì 6 file sau chỉ trỏ tới.
>
> Bằng chứng layout: cả 5 file `09-layoutBase/Giáo viên - *.dc.html`, phần `<aside>` và `<main>`.
> Hiện thực: chưa có — 6 slice `05-coding/frontend/src/views/` của khu này đều còn là stub
> `pendingDesign`, khác khu Admin (đã dựng thật). Mọi mô tả dưới đây là **thiết kế cho UI sắp dựng**,
> không phải mô tả code đang chạy.

## 1. Phạm vi

Khung này gồm **ba phần**, áp cho mọi route `/instructor/*`; chân trang chốt 2026-09-21, mô tả riêng ở mục 9 để không phải đánh số lại các mục sau (xem Câu hỏi mở Q1, mục 7):

| Phần | File hiện thực dự kiến | Vai trò |
| :--- | :--- | :--- |
| Sidebar | `widgets/app-shell/ui/instructor-sidebar.tsx` | Điều hướng + thu gọn + chuyển theme + khối danh tính |
| Nền và scope màu | `widgets/app-shell/ui/app-shell.tsx` + `shared/ui/layout/liquid-glass-backdrop.tsx` + `app/globals.css` | Nền liquid-glass, scope `.instructor-shell` ghi đè token màu |

Hai thứ khu Admin có mà khu này **không** có, không phải thiếu sót mà là khác biệt thật của prototype:

| Không có | Bằng chứng | Hệ quả |
| :--- | :--- | :--- |
| **Toolbar dùng chung** | Hàng đầu trong `<main>` khác nhau hoàn toàn giữa các màn: `Giáo viên - Tổng quan.dc.html:115-125` là ô tìm kiếm + nút "Tạo lớp mới" + avatar, còn `Giáo viên - Chấm bài.dc.html:115-121` chỉ là tiêu đề + mô tả phụ | Mỗi màn tự sở hữu hàng đầu của mình. Khung **không** cung cấp toolbar, và BD từng màn phải tự mô tả hàng này |
| ~~Chân trang~~ — **đã có, xem mục 9** | Cả 5 file `Giáo viên - *.dc.html` đều có **0** thẻ `<footer>`, nhưng chủ dự án chốt 2026-09-21 là **vẫn dựng**, để hai khu nhất quán | Khung có chân trang, cùng cấu trúc với khu Admin (`02-bd/screens/admin/_shell.md` mục 9). Đây là divergence có chủ đích với prototype, chi tiết ở mục 9 |

## 2. Quan hệ với khung Admin: tách riêng, không dùng chung

**Đây là câu trả lời dứt điểm cho Câu hỏi mở Q3 của `02-bd/screens/admin/_shell.md`** ("Khu Giảng viên
dùng lại khung này hay có khung riêng?"). Trả lời: **khung riêng**, hai căn cứ độc lập cùng chỉ một hướng.

1. **Quyết định đã có.** `system_survey.md` mục 7.2 chốt Phương án B — khu Giảng viên có layout riêng,
   tách khỏi shell của Admin; `DEC-2026-0825-shared-content-authoring-screens` khi cho 3 màn nội dung
   mount ở cả hai route đã nói rõ điều đó **không** đảo ngược Phương án B
   [Nguồn: DEC-2026-0825-shared-content-authoring-screens].
2. **Prototype khác cấu trúc thật, không chỉ khác màu.** Ba khác biệt không thể gộp vào một component:

| Điểm | Admin | Giảng viên |
| :--- | :--- | :--- |
| Cấu trúc nav | 4 nhóm gập (`navGroups`) + 1 mục đơn + nhóm KHÁC | **Danh sách phẳng 5 mục** (`navDefs`), không có nhóm gập [Nguồn: 09-layoutBase/Giáo viên - Tổng quan.dc.html:274-280] |
| Khối danh tính | Nằm ở **toolbar** trong `<main>` | Nằm ở **đáy sidebar**, kèm liên kết "Thoát" [Nguồn: 09-layoutBase/Giáo viên - Tổng quan.dc.html:100-109] |
| Badge trên mục nav | Không có | Có — "Lớp của tôi" 3, "Bài tập của tôi" 18, "Chấm bài" 9 [Nguồn: 09-layoutBase/Giáo viên - Tổng quan.dc.html:275-279] |

Dùng chung được cái gì: `LiquidGlassBackdrop`, các primitive `shared/ui`, cơ chế token màu theo scope.
Không dùng chung: bản thân component sidebar.

## 3. Sidebar

### 3.1. Cấu trúc

Sáu khối, từ trên xuống [Nguồn: 09-layoutBase/Giáo viên - Tổng quan.dc.html:62-110]:

| Khối | Nội dung |
| :--- | :--- |
| Thương hiệu | Ô logo `A` + "AlgoPrep" + dòng phụ "GIÁO VIÊN" (ẩn khi thu gọn) |
| Nút thu gọn | Nhãn "Thu gọn" + icon đảo chiều |
| Nav chính | 5 mục phẳng, mỗi mục có ô icon 2 ký tự và badge tuỳ chọn |
| Nhóm KHÁC | Cài đặt · Trang cá nhân |
| Chuyển theme | 2 nút Sáng / Tối |
| Khối danh tính | Avatar chữ cái + tên + vai trò "Giáo viên" + liên kết "Thoát" |

### 3.2. Năm mục nav và đích của chúng

| Thứ tự | Nhãn | Icon | Badge trong prototype | Slug đích |
| :-: | :--- | :--- | :--- | :--- |
| 1 | Tổng quan | `TQ` | không | `instructor_overview` |
| 2 | Lớp của tôi | `LH` | 3 | `class_management` |
| 3 | Bài tập của tôi | `BT` | 18 | `class_assignments` |
| 4 | Chấm bài | `CB` | 9 | `instructor_grading` |
| 5 | Tiến độ học viên | `TĐ` | không | `class_progress` |

`class_student_detail` (INS0204) **không** có mục nav — đúng thiết kế, đây là màn khoan sâu vào từ
`class_progress`, và cũng là màn duy nhất không có prototype.

Badge là **số thật đọc từ dữ liệu**, không phải nhãn tĩnh: 3 lớp đang phụ trách, 18 bài tập đã gán, 9 bài
chờ chấm tay. Nguồn của từng số thuộc BD của màn tương ứng, không thuộc file này — khung chỉ quy định
rằng mục nav có chỗ hiển thị badge. Ba con số cụ thể trong prototype là dữ liệu mẫu, không phải giá trị
mặc định.

### 3.3. Bốn route `/instructor/*` nội dung dùng chung — **Đã bổ sung vào nav (2026-09-22)**

> **Đã chốt 2026-09-22** (`DEC-2026-0922-users-and-admin-conflict-resolutions`): Bổ sung một nhóm nav **"Nội dung"** gồm 2 mục phía dưới 5 mục chính, giúp Giảng viên (A2) truy cập và thực thi quyền F2-01..F2-04 và F6-13 theo `DEC-2026-0825-shared-content-authoring-screens`:
> 1. **Quản lý bài tập**: `problem_management` (kèm đường dẫn vào `problem_authoring`)
> 2. **Câu hỏi phỏng vấn**: `interview_question_management` (kèm đường dẫn vào `interview_question_authoring`)

### 3.4. Trạng thái thu gọn

Hai trạng thái như khu Admin, nhưng **prototype khu này chỉ lưu theme, không lưu trạng thái thu gọn**:
chỉ có khoá `algoprep-teacher-theme` [Nguồn: 09-layoutBase/Giáo viên - Tổng quan.dc.html:245,251], không
có khoá `collapsed` tương ứng với `algoprep-admin-collapsed` của Admin.

Đề xuất của BD: **lưu, dùng khoá `algoprep-teacher-collapsed`** — người dùng thu gọn sidebar rồi chuyển
màn mà nó bật lại là hành vi khó chịu, và khu Admin đã lưu; để hai khu lệch nhau chỉ vì prototype vẽ
thiếu là tự tạo bất nhất `[SoT: Suy luận]`.

Ràng buộc chiều rộng ở chế độ thu gọn áp dụng y như khu Admin: mọi thứ đặt trong rail phải nằm gọn
trong bề rộng còn lại sau padding — ràng buộc này đã có e2e ở khu Admin
(`05-coding/frontend/e2e/admin-sidebar-collapsed.spec.ts`) và nên có bản tương ứng cho khu này khi dựng.

## 4. Vùng nội dung

`<main>` bọc nội dung trong container `max-width: 1320px` căn giữa, giống hệt khu Admin
[Nguồn: 09-layoutBase/Giáo viên - Tổng quan.dc.html:112-113].

Khác khu Admin ở chỗ: **không có toolbar dùng chung**, nên hàng đầu tiên trong container là nội dung
riêng của từng màn. Mỗi BD màn phải tự mô tả hàng đầu của mình trong Sheet 4.4 và Sheet 5.

## 5. Bảng màu và token

Scope `.instructor-shell` ghi đè các token ngữ nghĩa, cùng cơ chế đã dùng cho `.admin-shell`
(`02-bd/screens/admin/_shell.md` mục 5). Prototype khu Giảng viên dùng bảng màu riêng (biến `--accent`,
`--active-bg`, `--logo-bg`... có giá trị khác khu Admin), nên đây là một scope thứ hai, không phải dùng
lại `.admin-shell`.

Nguyên tắc khi thêm token giữ nguyên: giá trị phải chép từ `.dc.html` kèm số dòng trong comment, hoặc
đánh dấu `[SoT: Suy luận]` ngay tại chỗ. Không quy định màu sắc, khoảng cách, typography trong BD này —
`09-layoutBase/**` là bằng chứng bố cục chỉ-đọc, không phải design system
[Nguồn: 02-bd/_rules/bd-template-9sheet.md:82].

## 6. Trạng thái khung

| Trạng thái | Mô tả |
| :--- | :--- |
| `expanded` | Sidebar mở, nhãn đầy đủ, badge hiển thị |
| `collapsed` | Rail, chỉ icon 2 ký tự, nhãn qua `title`, **badge ẩn** [Nguồn: 09-layoutBase/Giáo viên - Tổng quan.dc.html:283 — `showBadge: !!badge && !collapsed`] |
| `narrow-viewport` | Ép rail dưới ngưỡng hẹp — prototype không định nghĩa ngưỡng, xem mục 7 Q4 |

Khung **không có** trạng thái `loading`/`error` cho phần điều hướng: 5 mục nav là hằng số biên dịch.
Riêng **badge thì có** — chúng là số đọc từ API, nên khung phải chịu được trường hợp chưa có số (ẩn
badge, không hiện `0` và không hiện khung xương).

## 7. Câu hỏi mở

| # | Câu hỏi | Ghi chú | Chủ sở hữu |
| :-: | :--- | :--- | :--- |
| Q1 | Khu Giảng viên có chân trang không? | **ĐÃ CHỐT 2026-09-21 — có, dựng thật**, dù cả 5 prototype đều 0 thẻ `<footer>`. Chủ dự án ưu tiên nhất quán giữa hai khu hơn là bám prototype. Cấu trúc và divergence: mục 9 | Chủ dự án |
| Q2 | 4 route nội dung dùng chung (`/instructor/problems`, `/instructor/interview-questions` và 2 route con) có vào nav khu Giảng viên không? | **ĐÃ ĐÓNG 2026-09-22** (`DEC-2026-0922-users-and-admin-conflict-resolutions`): Bổ sung nhóm nav "Nội dung" (Quản lý bài tập, Câu hỏi phỏng vấn) trên sidebar giảng viên. Xem mục 3.3. | Đã đóng |
| Q3 | Badge trên mục nav lấy số bằng API nào, làm mới lúc nào? | Ba badge là 3 con số từ 3 Bounded Context khác nhau (lớp từ `identity`, bài tập từ `problem-bank`, chờ chấm từ `ai-review`). Gọi 3 API riêng mỗi lần đổi màn là lãng phí; đề xuất gộp một endpoint tóm tắt cho khung, chốt khi viết DD | DD `identity` |
| Q4 | Ngưỡng ép rail của khu này | Prototype không định nghĩa breakpoint. Khu Admin chốt 1024px (`02-bd/screens/admin/_shell.md:60`); đề xuất dùng cùng mốc để khỏi có hai ngưỡng | Chủ dự án khi chốt design system |
| Q5 | Liên kết "Thoát" ở đáy sidebar là đăng xuất hay quay về trang chọn vai trò? | Trong prototype nó trỏ về `./Dashboard AlgoPrep.dc.html` — một file điều hướng của bản mẫu, không phải màn có slug trong `01-rd/screens/`. Đề xuất: đây là **Đăng xuất**, đưa về `auth` | Chủ dự án |

## 8. Tham chiếu

- `09-layoutBase/Giáo viên - Tổng quan.dc.html` — bằng chứng layout gốc của sidebar.
- `09-layoutBase/Giáo viên - Chấm bài.dc.html:115-121` — bằng chứng hàng đầu `<main>` khác nhau giữa các màn.
- `02-bd/screens/admin/_shell.md` — khung tương ứng của khu Admin; mục 7 Q3 được file này trả lời.
- `02-bd/_rules/bd-template-9sheet.md` — quy ước mẫu 9 sheet cho 6 file BD màn của khu này.
- Quyết định: `DEC-2026-0825-frontend-base-architecture`, `DEC-2026-0825-shared-content-authoring-screens`,
  `DEC-2026-0828-split-class-management-assignments`, `DEC-2026-0921-teacher-screens-conflict-resolutions`,
  `DEC-2026-0922-users-and-admin-conflict-resolutions`.

## 9. Chân trang (chốt 2026-09-21)

Trả lời Câu hỏi mở Q1 (mục 7): chủ dự án chốt **dựng chân trang thật** cho khu Giảng viên, áp cho mọi
route `/instructor/*`. Phụ lục đứng cuối file thay vì chen vào mục 1-8 để giữ nguyên mọi số dòng đã bị 6
file BD màn của khu này trích dẫn (`_shell.md:13-15`, `:30`, `:110-111`, `:113-114`, `:146`...).

### 9.1 Quyết định đi ngược bằng chứng prototype, có lý do

Đây là divergence **mạnh hơn** trường hợp khu Admin. Ở khu Admin, mọi prototype đều có chân trang trừ
`admin_overview`, nên dựng chân trang là theo đa số bằng chứng. Ở khu Giảng viên, **cả 5 prototype đều
không có** thẻ `<footer>` — bằng chứng thống nhất một chiều, và quyết định đi ngược lại toàn bộ.

Lý do chấp nhận: hai khu quản trị nội bộ đặt cạnh nhau, một khu có chân trang một khu không là bất nhất
mà người dùng thấy ngay. Giá trị của tính nhất quán ở đây lớn hơn giá trị của việc bám đúng một bộ
prototype vốn được vẽ ở các thời điểm khác nhau và đã lệch với quyết định ở nhiều chỗ khác (xem
`07-review/bd_screens_teacher_open_questions_260921.md` mục 6).

### 9.2 Cấu trúc

Dùng lại **nguyên** cấu trúc ba khu của chân trang khu Admin (`02-bd/screens/admin/_shell.md` mục 9.1) —
không thiết kế một bố cục thứ hai:

| Khu | Nội dung | Ghi chú cho khu Giảng viên |
| :--- | :--- | :--- |
| Trái | Tên sản phẩm kèm phiên bản + dòng bản quyền | Chữ "Admin" trong nhãn đổi thành "Giảng viên"; phần còn lại giữ nguyên |
| Giữa hoặc phải | Chấm tròn trạng thái + câu trạng thái dịch vụ | Giống hệt khu Admin |
| Phải | 4 liên kết phụ | Giữ đúng 4 liên kết nhưng **đổi tập liên kết**, chốt 2026-09-21: giữ "Nhật ký thay đổi" và "Hỗ trợ" của khu Admin; bỏ "Tài liệu API" và "Trạng thái go-judge" (thông tin vận hành hệ thống, giảng viên không vận hành hệ thống), thay bằng **"Hướng dẫn giảng viên"** và **"Phản hồi"** |

Vị trí: phần tử cuối cùng **bên trong** `<main>`, sau nội dung màn, cùng trong container
`max-width: 1320px` — giống khu Admin, không phải thanh cố định đáy viewport.

### 9.3 Bốn liên kết vẫn là trang trí

Chưa có đích thật, nên render dạng chữ tĩnh chứ không phải `<a>` sống — cùng cách đã áp ở khu Admin, để
không tạo điểm dừng bàn phím chết.

### 9.4 Việc còn mở

| # | Việc | Ghi chú |
| :-: | :--- | :--- |
| 9a | ~~Tập 4 liên kết cho khu Giảng viên~~ | **ĐÃ CHỐT 2026-09-21** (`DEC-2026-0921-class-completion-owned-by-identity` mục 2): "Hướng dẫn giảng viên / Phản hồi / Nhật ký thay đổi / Hỗ trợ". Đổi được rẻ ở thời điểm này vì cả 4 mới là chữ tĩnh chưa có đích thật; để đến lúc dựng UI mới đổi thì phải sửa cả component lẫn file i18n |
| 9b | Hiện thực | Khu Giảng viên **chưa có code shell** — 6 slice đều là stub `pendingDesign` và `app-shell.tsx` mới chỉ có nhánh riêng cho `admin`. Chân trang khu này dựng cùng lúc với `instructor-sidebar.tsx`, tái dùng component `admin-footer.tsx` sau khi đổi tên thành component chung nhận tham số nhãn |
