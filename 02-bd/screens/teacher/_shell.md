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
> Hiện thực: shell Giảng viên đã dựng (cập nhật 2026-10-11, xem mục 3.3 "Hiện trạng bản dựng" và mục 9.4
> 9b); phần còn lại của file là **thiết kế**, không phải mô tả từng dòng code đang chạy.

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
| Cấu trúc nav | 4 nhóm gập (`navGroups`) + 1 mục đơn + nhóm KHÁC | **5 mục chính (từ 2026/10/14: 4 mục sau khi bỏ "Chấm bài" ngày 2026/10/13, cộng mục "Bài nộp" nay đã có BD `INS0206`) + nhóm Nội dung + nhóm Khác**, không có nhóm gập; prototype chỉ vẽ 5 mục phẳng (`navDefs`, còn mục "Chấm bài" đã bỏ khỏi thiết kế) [Nguồn: 09-layoutBase/Giáo viên - Tổng quan.dc.html:274-280], nhóm Nội dung bổ sung 2026-09-22 (mục 3.3) |
| Khối danh tính | Nằm ở **toolbar** trong `<main>` | Nằm ở **đáy sidebar**, kèm liên kết "Thoát" [Nguồn: 09-layoutBase/Giáo viên - Tổng quan.dc.html:100-109] |

Dùng chung được cái gì: `LiquidGlassBackdrop`, các primitive `shared/ui`, cơ chế token màu theo scope.
Không dùng chung: bản thân component sidebar.

## 3. Sidebar

### 3.1. Cấu trúc

> **Bố cục theo khu Admin (2026-10-11, chủ dự án chỉ đạo, `DEC-2026-1011-instructor-layout-follows-admin`, chưa duyệt hình).** Phần mô tả "danh sách phẳng 5 mục", "nhóm KHÁC" và "khối danh tính ở đáy sidebar" ở các mục 2, 3.1, 3.2 bên dưới là **bố cục mockup cũ**; bản dựng nay là: sidebar gồm liên kết Tổng quan và ba nhóm gập (Lớp học: Lớp của tôi, Danh sách học viên; Quản lý bài tập đã nộp: Bài giao cho lớp, Bài nộp (nhóm đổi nhãn từ "Giao và chấm bài" 2026/10/14 theo `DEC-2026-1014-instructor-overview-trim-and-nav-label`, đã đóng Q6; tên mục giữ đúng như trong mã, xem mục 3.2); Nội dung: Quản lý bài tập, Câu hỏi phỏng vấn), logo và nút thu gọn trong sidebar, chuyển theme ở chân sidebar; một hàng công cụ ở đầu vùng nội dung với menu người dùng (Cài đặt, Trang cá nhân, Thoát); chân trang chung. Mã: `widgets/app-shell/model/instructor-nav.ts`, `ui/grouped-sidebar.tsx`, `ui/instructor-toolbar.tsx`, `ui/area-toolbar.tsx`. Bảng màu giữ của giảng viên (mục 5). Nhóm tự mở khi route hiện tại thuộc nhóm đó; mục nav sáng theo tiền tố href dài nhất.

Bảy khối (nav chính, nhóm Nội dung và nhóm Khác tính riêng), từ trên xuống [Nguồn: 09-layoutBase/Giáo viên - Tổng quan.dc.html:62-110]:

| Khối | Nội dung |
| :--- | :--- |
| Thương hiệu | Ô logo `A` + "AlgoPrep" + dòng phụ "GIÁO VIÊN" (ẩn khi thu gọn) |
| Nút thu gọn | Nhãn "Thu gọn" + icon đảo chiều |
| Nav chính | 5 mục chính (mục 3.2), mỗi mục có ô icon 2 ký tự, không có badge số (`DEC-2026-1011-teacher-nav-badge-dropped`) |
| Nhóm Nội dung | Quản lý bài tập · Câu hỏi phỏng vấn (mục 3.3), đứng giữa nav chính và nhóm Khác |
| Nhóm KHÁC | Cài đặt · Trang cá nhân |
| Chuyển theme | 2 nút Sáng / Tối |
| Khối danh tính | Avatar chữ cái + tên + vai trò "Giáo viên" + liên kết "Thoát" |

### 3.2. Năm mục nav và đích của chúng

| Thứ tự | Nhãn | Icon | Slug đích |
| :-: | :--- | :--- | :--- |
| 1 | Tổng quan | `TQ` | `instructor_overview` |
| 2 | Lớp của tôi | `LH` | `class_management` |
| 3 | Bài giao cho lớp (tên theo mã; mockup ghi "Bài tập của tôi"); nằm trong nhóm gập "Quản lý bài tập đã nộp" | `BT` | `class_assignments` |
| 4 | Danh sách học viên (`/instructor/students`), gộp `class_progress` từ 2026-09-27 | `TĐ` | `class_progress` |
| 5 | Bài nộp (`/instructor/submissions`); nằm trong nhóm gập "Quản lý bài tập đã nộp", đứng sau "Bài giao cho lớp". Icon 2 ký tự chưa chốt: bản dựng chỉ có icon ở tiêu đề nhóm, mục con không có icon (`instructor-nav.ts:10`) | - | `submission_management` (`INS0206`) |

**Nhãn nhóm gập (chốt 2026-10-14, `DEC-2026-1014-instructor-overview-trim-and-nav-label`):** sidebar gom các mục bài tập vào nhóm gập "Quản lý bài tập đã nộp" (nhãn cũ "Giao và chấm bài"), gồm hai mục **Bài giao cho lớp** (`class_assignments`, dòng 3 của bảng trên) và **Bài nộp** (`/instructor/submissions`, `DEC-2026-1013-instructor-submissions-list`). **Chốt 2026/10/14 (`DEC-2026-1014-instructor-batch5-6-nav-pagination-rubric`, mục S-2):** tên mục giữ đúng như trong mã, tức "Bài giao cho lớp" và "Bài nộp" trong nhóm "Quản lý bài tập đã nộp" [Nguồn: 05-coding/frontend/messages/vi.json:354,368-369; 05-coding/frontend/src/widgets/app-shell/model/instructor-nav.ts]; nhãn "Bài tập" ghi trước đây ở đoạn này là cách gọi tắt của quyết định, không phải nhãn hiển thị, và không dùng nữa. Mọi BD màn khu Giảng viên gọi mục này là "Bài giao cho lớp". Từ 2026/10/14 bảng 3.2 có đủ **năm** mục chính: mục "Bài nộp" đã có dòng riêng (dòng 5) vì BD của màn `/instructor/submissions` đã viết xong (`02-bd/screens/teacher/INS0206_submission_management.md`, `DEC-2026-1014-instructor-submissions-list-rules`).

`class_student_detail` (INS0204) **không** có mục nav — đúng thiết kế, đây là màn khoan sâu vào từ
`class_progress`, và cũng là màn duy nhất không có prototype. **Quy tắc sáng mục nav cho màn khoan sâu (chốt 2026/10/14, `DEC-2026-1014-instructor-batch5-6-nav-pagination-rubric`, tiêu chí ponytail bậc 5):** màn không có mục nav riêng (`class_student_detail` INS0204, `class_detail` INS0205 ở `/instructor/classes/[classId]`) thì sidebar **sáng mục của màn gọi tới**: vào `INS0204` từ `class_progress` sáng "Danh sách học viên", vào từ `class_management` hoặc `INS0205` sáng "Lớp của tôi"; vào `INS0205` từ `class_management` sáng "Lớp của tôi", vào `INS0204` từ `INS0206` (bấm tên học viên ở danh sách bài nộp) sáng "Bài nộp" `[SoT: Suy luận]` theo cùng quy tắc, còn trang kết quả chỉ đọc `/instructor/submissions/{id}` (vùng giảng viên của `USR0201`) sáng mục của màn gọi tới, mặc định "Bài nộp". Khi màn gọi tới cũng là màn khoan sâu thì kế thừa mục đã sáng của chuỗi (ví dụ `INS0201` sang `INS0205` sang `INS0204` vẫn sáng "Lớp của tôi") `[SoT: Suy luận]`. Màn khoan sâu **phải mang tín hiệu màn gọi tới** (query string hoặc trạng thái điều hướng) để khung biết mục nào sáng; khi mở thẳng bằng URL không có tín hiệu thì mặc định sáng "Lớp của tôi" `[SoT: Suy luận]`. Nó mở rộng quy tắc "mục nav sáng theo tiền tố href dài nhất" ở mục 3.1, vốn không đủ khi `INS0204` không có tiền tố riêng.

**Hiện trạng bản dựng của quy tắc sáng mục màn khoan sâu (2026-10-14, chưa duyệt hình):** mới dựng **một** phần, là trang kết quả chỉ đọc `/instructor/submissions/{id}`: query `from` (`submissions`, `student`, `class`; thiếu hoặc lạ thì `submissions`) chọn mục sáng, tức `student` sáng "Danh sách học viên" (`/instructor/students`), `class` sáng "Lớp của tôi" (`/instructor/classes`), còn lại sáng "Bài nộp"; `GroupedSidebar` nhận prop `activeHrefOverride` và `instructorActiveHrefOverride` chỉ trả giá trị khi đường dẫn đúng dạng `/instructor/submissions/{id}` [Nguồn: 05-coding/frontend/src/widgets/app-shell/ui/instructor-sidebar.tsx:24-26,46-64; 05-coding/frontend/src/widgets/app-shell/ui/grouped-sidebar.tsx:35,60,100; 05-coding/frontend/src/entities/submission/model/origin.ts:4-17]. **Cập nhật 2026/10/18 (Dựng prototype Phase 4-5, chưa duyệt hình):** quy tắc đã dựng thêm cho `INS0204`. `instructorActiveHrefOverride` nay xử lý cả route `/instructor/classes/{classId}/students/{studentId}`: `from=students` sáng "Danh sách học viên" (`/instructor/students`), `from=submissions` sáng "Bài nộp" (`/instructor/submissions`), không có `from` hoặc giá trị lạ thì không ghi đè và mục "Lớp của tôi" sáng nhờ khớp tiền tố href [Nguồn: 05-coding/frontend/src/widgets/app-shell/ui/instructor-sidebar.tsx:46-64; test: 05-coding/frontend/src/widgets/app-shell/ui/instructor-sidebar.test.ts:21-32]. Nơi gắn `from=`: liên kết tên học viên ở bảng học viên `INS0203` mang `?from=students` [Nguồn: 05-coding/frontend/src/views/teacher/class-progress/ui/class-progress-view.tsx:134], ở danh sách bài nộp `INS0206` mang `?from=submissions` [Nguồn: 05-coding/frontend/src/views/teacher/submission-management/ui/submission-management-view.tsx:107]. `INS0205` (`/instructor/classes/{classId}`) chỉ được mở từ "Lớp của tôi" nên giữ mục đó sáng bằng khớp tiền tố, không cần mã riêng; liên kết tên học viên ở `INS0205` không mang `from=` nên `INS0204` mở từ đó cũng sáng "Lớp của tôi". Các nơi gọi `from=` còn lại: lịch sử nộp của `INS0204` (`from=student`) và danh sách `INS0206` tới trang kết quả (`from=submissions`); `from=class` chưa có màn gọi.

**Không có badge số trên mục nav** (chủ dự án duyệt 2026-10-11, `DEC-2026-1011-teacher-nav-badge-dropped`,
tiêu chí ponytail bậc 2: không có nguồn số thật). Ba badge của prototype không đi vào BD; xem Câu hỏi mở Q3.

### 3.3. Bốn route `/instructor/*` nội dung dùng chung — **Đã bổ sung vào nav (2026-09-22)**

> **Đã chốt 2026-09-22** (`DEC-2026-0922-users-and-admin-conflict-resolutions`): Bổ sung một nhóm nav **"Nội dung"** gồm 2 mục phía dưới 5 mục chính, giúp Giảng viên (A2) truy cập và thực thi quyền F2-01..F2-04 và F6-13 theo `DEC-2026-0825-shared-content-authoring-screens`:
> 1. **Quản lý bài tập**: `problem_management` (kèm đường dẫn vào `problem_authoring`)
> 2. **Câu hỏi phỏng vấn**: `interview_question_management` (kèm đường dẫn vào `interview_question_authoring`)

**Hiện trạng bản dựng (2026-10-11):** nhóm "Nội dung" đã vào sidebar (`widgets/app-shell/model/instructor-nav.ts`, `INSTRUCTOR_NAV_CONTENT`; render ở `instructor-sidebar.tsx`), tổng 9 liên kết (5 chính gồm cả "Chấm bài", 2 Nội dung, 2 Khác) lúc ghi mục này. **Cập nhật 2026/10/14:** thiết kế nay có 9 liên kết (5 chính gồm "Bài nộp" mà không còn "Chấm bài", 2 Nội dung, 2 Khác); mã `instructor-nav.ts` đã gỡ "Chấm bài", sidebar có 7 liên kết (tổng quan 1, nhóm Lớp học 2, nhóm Quản lý bài tập đã nộp 2, nhóm Nội dung 2) và menu người dùng có Cài đặt, Trang cá nhân, Thoát [Nguồn: 05-coding/frontend/src/widgets/app-shell/model/instructor-nav.ts:29-95]. Bản e2e `instructor-shell.spec.ts:24-25,29,31` vẫn đếm 8 liên kết, lệch với mã (chưa đối chiếu lý do, ghi nợ cho phía mã). Nhóm Nội dung đứng **giữa** nhóm bài tập và nhóm Khác. Mục "Chấm bài" bị bỏ theo `DEC-2026-1013-remove-instructor-manual-grading`. Nhãn mục thứ 3 đã đổi thành "Bài giao cho lớp". Hai mục Khác đã đổi đích sang `/instructor/settings` và `/instructor/profile` để không rời vỏ giảng viên; hai route dùng lại `ProfileView` / `SettingsView` với `area="instructor"` (dòng "Quyền" hiện "Giảng viên", ẩn thẻ "Luyện tập" của học viên). Các trường tên, vai trò hiện tại và trường học trong form hồ sơ vẫn là dữ liệu mẫu của học viên (chưa duyệt).

**Đã đồng bộ 2026-10-11 (chủ dự án duyệt):** mục nav thứ 5 ở bảng 3.2 ghi theo bản dựng "Danh sách học viên" (`/instructor/students`, gộp `class_progress` từ 2026-09-27); mục 2 và 3.1 từng viết "5 mục chính + nhóm Nội dung + nhóm Khác"; từ 2026/10/13 là 4 mục chính (đã bỏ "Chấm bài"), và mục "Danh sách học viên" đánh số 4; từ 2026/10/14 là 5 mục chính, "Bài nộp" đánh số 5.

**Hiện trạng bản dựng (2026-10-11), khối danh tính và chân trang:** đáy sidebar có khối danh tính (avatar chữ cái, tên, vai trò "Giảng viên", liên kết "Thoát"), thu gọn thì chỉ hiện avatar; chân trang dùng lại component chân trang của Admin đã đổi tên chung, nhận tham số nhãn. Mã đang được dựng song song, mục này ghi theo `DEC-2026-1011-teacher-shell-identity-footer-built`, chưa trích dòng mã.

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
| `expanded` | Sidebar mở, nhãn đầy đủ |
| `collapsed` | Rail, chỉ icon 2 ký tự, nhãn qua `title`; khối danh tính chỉ còn avatar |
| `narrow-viewport` | Ép rail dưới ngưỡng 1024px, cùng khu Admin — xem mục 7 Q4 |

Khung **không có** trạng thái `loading`/`error` cho phần điều hướng: các mục nav là hằng số biên dịch và
không có badge đọc từ API.

## 7. Câu hỏi mở

| # | Câu hỏi | Ghi chú | Chủ sở hữu |
| :-: | :--- | :--- | :--- |
| Q1 | Khu Giảng viên có chân trang không? | **ĐÃ CHỐT 2026-09-21 — có, dựng thật**, dù cả 5 prototype đều 0 thẻ `<footer>`. Chủ dự án ưu tiên nhất quán giữa hai khu hơn là bám prototype. Cấu trúc và divergence: mục 9 | Chủ dự án |
| Q2 | ~~4 route nội dung dùng chung~~ hai nhóm nav (Nội dung, Khác) có vào nav khu Giảng viên không? | **ĐÃ ĐÓNG 2026-09-22** (`DEC-2026-0922-users-and-admin-conflict-resolutions`): Bổ sung nhóm nav "Nội dung" (Quản lý bài tập, Câu hỏi phỏng vấn) trên sidebar giảng viên. Xem mục 3.3. | Đã đóng |
| Q3 | ~~Badge trên mục nav lấy số bằng API nào, làm mới lúc nào?~~ | **ĐÃ CHỐT 2026-10-11 (chủ dự án duyệt) theo `DEC-2026-1011-teacher-nav-badge-dropped`:** bỏ badge khỏi BD, không có endpoint tóm tắt cho khung (tiêu chí ponytail bậc 2: không có nguồn số thật; ba số đến từ ba Bounded Context khác nhau). Các câu về badge 3/18/9 đã xoá khỏi mục 2, 3.1, 3.2, 6 | Đã đóng |
| Q4 | ~~Ngưỡng ép rail của khu này~~ | **ĐÃ CHỐT 2026-10-11 (chủ dự án duyệt) theo `DEC-2026-1011-teacher-shell-identity-footer-built`:** giữ **1024px**, cùng mốc khu Admin (`02-bd/screens/admin/_shell.md:60`) để khỏi có hai ngưỡng | Đã đóng |
| Q5 | ~~Liên kết "Thoát" ở đáy sidebar là đăng xuất hay quay về trang chọn vai trò?~~ | **ĐÃ CHỐT 2026-10-11 (chủ dự án duyệt) theo `DEC-2026-1011-teacher-shell-identity-footer-built`:** "Thoát" là **Đăng xuất**, đưa về màn đăng nhập; prototype trỏ `./Dashboard AlgoPrep.dc.html` chỉ là điều hướng của bản mẫu | Đã đóng |
| Q6 | ~~Nhóm gập "Giao và chấm bài" chỉ còn một mục (Bài giao cho lớp): đổi nhãn thành "Giao bài" hay bỏ nhóm, đưa mục thành liên kết đơn?~~ **ĐÃ CHỐT 2026-10-14 (chủ dự án) theo `DEC-2026-1014-instructor-overview-trim-and-nav-label` (tiêu chí ponytail bậc 3, bậc 4):** đổi nhãn nhóm thành **"Quản lý bài tập đã nộp"**, giữ nhóm gập với hai mục bên trong (**Bài giao cho lớp**, **Bài nộp**; chốt tên 2026/10/14, mục 3.2). | Tiền đề cũ ("chỉ còn một mục") đã lỗi thời vì nhóm có hai mục. Đề xuất cũ "Giao bài" không dùng, giữ làm lịch sử | Đã đóng |

## 8. Tham chiếu

- `09-layoutBase/Giáo viên - Tổng quan.dc.html` — bằng chứng layout gốc của sidebar.
- `09-layoutBase/Giáo viên - Chấm bài.dc.html:115-121` — bằng chứng hàng đầu `<main>` khác nhau giữa các màn (màn Chấm bài đã bỏ 2026/10/13, file mockup giữ làm bằng chứng lịch sử).
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
| 9b | ~~Hiện thực~~ | **ĐÃ DỰNG (cập nhật 2026-10-11, chủ dự án duyệt):** shell Giảng viên đã có (`instructor-sidebar.tsx`, `app-shell.tsx`); chân trang tái dùng component chân trang của Admin sau khi đổi tên thành component chung nhận tham số nhãn, theo `DEC-2026-1011-teacher-shell-identity-footer-built`. Mã đang dựng song song nên chưa trích dòng |

## 10. Lịch sử sửa đổi

Bảng 10.1: Lịch sử sửa đổi của file khung (file chưa có bảng này trước 2026-10-11).

| Ver | Mục bị sửa | Nội dung sửa | Ngày | Người sửa |
| :--- | :--- | :--- | :--- | :--- |
| V1.1 | 2, 3.1, 3.2, 3.3, 6, 7 (Q2 đến Q5), 9.4 | Chủ dự án duyệt 2026-10-11: viết lại "5 mục chính + nhóm Nội dung + nhóm Khác"; mục nav 5 ghi "Danh sách học viên"; bỏ badge nav (`DEC-2026-1011-teacher-nav-badge-dropped`, đóng Q3); đóng Q4 (1024px) và Q5 ("Thoát" là đăng xuất) theo `DEC-2026-1011-teacher-shell-identity-footer-built`; Q2 sửa "bốn route" thành "hai nhóm nav"; ghi "Hiện trạng bản dựng" cho khối danh tính và chân trang; 9b ghi shell đã dựng | 2026/10/11 | AI |
| V1.2 | 3.1 (ghi chú đầu mục) | Bố cục khu Giảng viên theo khu Admin: sidebar nhóm gập, thanh công cụ có menu người dùng, chân trang chung (`DEC-2026-1011-instructor-layout-follows-admin`, chủ dự án chỉ đạo, chưa duyệt hình). Các mô tả bố cục cũ ở mục 2, 3.1, 3.2 giữ làm lịch sử | 2026/10/11 | AI |
| V1.3 | 3.1 (ghi chú đầu mục) | Chủ dự án chốt 2026-10-13 (Q-K, `DEC-2026-1013-teacher-open-questions-closed-round2`): ba nhóm nav khu Giảng viên **mở sẵn** lúc vào trang; bấm tiêu đề nhóm vẫn gập và mở từng nhóm. Admin giữ nguyên (chỉ nhóm chứa route hiện tại mở). Prototype: prop `defaultOpenAll` của `GroupedSidebar` | 2026-10-13 | AI |
| V1.4 | 2, 3.1, 3.2, 3.3, 7 (thêm Q6), 8 | Bỏ mục nav "Chấm bài" (`INS0301`) theo `DEC-2026-1013-remove-instructor-manual-grading`: nav chính còn 4 mục, đánh số lại; tổng liên kết thiết kế 8. Prototype và mã (`instructor-nav.ts`, e2e) vẫn còn mục này, chưa gỡ | 2026/10/13 | AI |
| V1.5 | 3.1 (ghi chú đầu mục), 3.2, 7 (đóng Q6) | Áp `DEC-2026-1014-instructor-overview-trim-and-nav-label`: nhóm nav "Giao và chấm bài" đổi nhãn thành "Quản lý bài tập đã nộp" (hai mục Bài tập, Bài nộp giữ nguyên); đóng Q6 vì tiền đề "chỉ còn một mục" đã lỗi thời. Mã `instructor-nav.ts` và khoá i18n `instructorNav.groupWork` do phía mã cập nhật, chưa kiểm tra bản dựng ở phiên này | 2026/10/14 | AI |
| V1.6 | 3.1 (ghi chú đầu mục), 3.2, 7 (Q6) | Áp `DEC-2026-1014-instructor-batch5-6-nav-pagination-rubric` (S-2, Q7 của `INS0204`, Q3 của `INS0205`): (1) thống nhất tên hai mục trong nhóm "Quản lý bài tập đã nộp" là "Bài giao cho lớp" và "Bài nộp", đúng như trong mã [Nguồn: 05-coding/frontend/messages/vi.json:368-369]; bỏ nhãn "Bài tập" ở 3.1, 3.2, Q6 (hàng V1.5 giữ nguyên làm lịch sử); không thêm dòng cho màn Bài nộp vì BD của nó viết sau cùng; (2) thêm quy tắc sáng mục nav cho màn khoan sâu: sáng mục của màn gọi tới, mặc định "Lớp của tôi" khi không có tín hiệu. Chưa đối chiếu mã `instructor-nav.ts` với quy tắc (2), ghi `06-plan/PROTOTYPE_DEBT.md` do tài liệu khác phụ trách | 2026/10/14 | AI |
| V1.7 | 1 (bảng khác biệt), 3.1, 3.2 (tiêu đề, bảng, quy tắc sáng mục), 3.3 | Áp `DEC-2026-1014-instructor-submissions-list-rules` (mục 8) và `DEC-2026-1013-instructor-submissions-list`: BD màn `INS0206` đã viết, nên bảng 3.2 thêm dòng 5 "Bài nộp" (`/instructor/submissions`, slug `submission_management`) và đổi "4 mục chính" thành "5 mục chính" ở mục 2 (bảng khác biệt), 3.1, tiêu đề 3.2; bỏ câu "chưa có dòng riêng vì BD viết sau cùng"; quy tắc sáng mục cho màn khoan sâu thêm đường `INS0206` sang `INS0204` (sáng "Bài nộp") và trang kết quả chỉ đọc (sáng mục màn gọi tới, mặc định "Bài nộp"); mục 3.3 cập nhật số liên kết thiết kế 9, mã sidebar 7, ghi e2e `instructor-shell.spec.ts` đếm 8 là lệch mã. Không đổi bố cục, không thêm câu hỏi mở | 2026/10/14 | AI |
| V1.8 | 3.2 (ghi chú hiện trạng quy tắc sáng mục màn khoan sâu) | Dựng prototype trang kết quả của giảng viên (`from=`, breadcrumb, sidebar): ghi phần đã dựng (trang kết quả giữ sáng mục màn gọi tới qua `from=`) và phần chưa dựng (`INS0204`, `INS0205`). Không đổi quy tắc hay quyết định thiết kế. **Chưa duyệt hình** | 2026/10/14 | AI |
| V1.9 | 3.2 (ghi chú hiện trạng quy tắc sáng mục màn khoan sâu) | Dựng prototype Phase 4-5 (bài giao theo lớp, phân trang 20, from=). Ghi phần đã dựng thêm: sáng mục theo `from=students` và `from=submissions` cho trang chi tiết học viên (`INS0204`); `INS0205` không cần mã vì chỉ vào từ "Lớp của tôi". Bỏ câu "Chưa dựng: INS0204 và INS0205" của V1.8. Không đổi quy tắc hay quyết định thiết kế. **Chưa duyệt hình** | 2026/10/18 | AI |
