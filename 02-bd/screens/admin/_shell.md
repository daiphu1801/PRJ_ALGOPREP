# BD — Khung điều hướng dùng chung khu Admin (`_shell`)

> Không phải một màn trong `01-rd/screens/` — đây là **khung bao** mà cả 11 đích nav của khu Admin dùng
> chung. Viết tách ra vì mỗi BD màn đang phải mô tả lại sidebar/toolbar cho "tự đủ", dẫn tới 8 bản mô tả
> gần giống nhau và sẽ lệch nhau khi khung đổi.
>
> Yêu cầu viết file này đến từ `07-review/bd_screens_admin_open_questions_260913.md` mục 4, dòng
> `admin_queue_monitor`: "Viết BD khung điều hướng dùng chung (`02-bd/screens/admin/_shell.md`) khi bắt
> đầu màn admin thứ hai". Viết 2026-09-17, sau khi cả 11 màn đã dựng — muộn hơn mốc đó, ghi nhận đúng
> như vậy thay vì im lặng.
>
> Bằng chứng layout: `09-layoutBase/Admin - Tổng quan.dc.html` và các file `Admin - *.dc.html` khác,
> phần `<aside>` và hàng đầu trong `<main>` — mọi file đều lặp lại cùng một khối.
> Mã nguồn hiện thực: `05-coding/frontend/src/widgets/app-shell/`.

## 1. Phạm vi

Khung này gồm bốn phần, áp cho **mọi** route `/admin/*` trừ `/admin/login`; chân trang chốt 2026-09-21, mô tả riêng ở mục 9 để không phải đánh số lại các mục sau (xem Câu hỏi mở Q4, mục 7):

| Phần | File hiện thực | Vai trò |
| :--- | :--- | :--- |
| Sidebar | `widgets/app-shell/ui/admin-sidebar.tsx` | Điều hướng 11 đích + thu gọn + chuyển theme/ngôn ngữ |
| Toolbar | `widgets/app-shell/ui/admin-toolbar.tsx` | Hàng đầu trong `<main>`: icon trang trí + khối danh tính |
| Nền và scope màu | `widgets/app-shell/ui/app-shell.tsx` + `shared/ui/liquid-glass-backdrop.tsx` + `app/globals.css` | Nền liquid-glass, scope `.admin-shell` ghi đè token màu |

`/admin/login` **không** dùng khung này — nó là màn đứng riêng theo
`DEC-2026-0915-admin-separate-login-route`, chỉ mượn lại scope `.admin-shell` để ăn cùng bảng màu.

## 2. Sidebar

### 2.1. Cấu trúc

Chiều rộng hai trạng thái: **224px** mở rộng, **72px** thu gọn (rail). Nguồn dữ liệu nav:
`widgets/app-shell/model/admin-nav.ts`, có test.

| Khối | Nội dung |
| :--- | :--- |
| Thương hiệu | Ô logo `A` + chữ "AlgoPrep" (ẩn khi thu gọn) |
| Nút thu gọn | Ghi trạng thái vào `localStorage` khoá `algoprep-admin-collapsed` |
| Mục đơn | Tổng quan (`/admin/overview`) |
| 4 nhóm gập | Nội dung · Vận hành · AI · Hệ thống — mỗi nhóm 2-3 đích |
| Nhóm KHÁC | Cài đặt · Đăng nhập & Đăng ký · Trang cá nhân |
| Chân sidebar | `ThemeLangSwitcher` (chuyển sáng/tối và vi/en) |

### 2.2. Hai hành vi đã chốt trong lúc dựng, khác prototype

1. **Nhiều nhóm mở cùng lúc.** Prototype chỉ cho một nhóm mở (`openGroup` là một biến đơn), chủ dự án
   yêu cầu 2026-09-14 cho phép mở nhiều nhóm. Đây là divergence có chủ đích.
2. **Nhóm chứa route hiện tại mặc định mở.** Thêm 2026-09-17: trước đó vào thẳng `/admin/system-log`
   thì mọi nhóm đều đóng và không có gì cho biết đang ở đâu. Trạng thái này **suy ra từ `pathname`**,
   không đồng bộ qua effect; `openGroups` chỉ giữ những nhóm người dùng tự bật/tắt.

### 2.3. Ràng buộc chiều rộng ở chế độ thu gọn

Rail 72px với `p-3` hai bên chỉ còn **48px** nội dung. Mọi thứ đặt trong rail phải nằm gọn trong 48px
đó — `ThemeLangSwitcher` từng vi phạm (hai vòng tròn 24px + gap 4px = 52px) và bị phát hiện bằng đo
`boundingBox()` chứ không phải nhìn mắt. Có e2e giữ ràng buộc này:
`05-coding/frontend/e2e/admin-sidebar-collapsed.spec.ts`.

Dưới 1024px (`lg`) sidebar **luôn** ở dạng rail bất kể tuỳ chọn đã lưu.

### 2.4. Sidebar ghim cố định (chốt 2026-10-01)

Sidebar dùng `sticky top-0 h-screen self-start` và tự cuộn dọc bên trong (`overflow-y-auto`), giống sidebar khu
Giảng viên: khi một bảng dài kéo trang cuộn xuống, thanh điều hướng vẫn đứng yên theo khung nhìn thay vì trôi đi
cùng nội dung [SoT: 05-coding/frontend/src/widgets/app-shell/ui/admin-sidebar.tsx:82-84]. Trước đó sidebar chỉ là
một phần tử flex cao bằng nội dung nên bị cuộn mất.

## 3. Toolbar

Không phải header toàn trang — là hàng đầu tiên **bên trong** vùng nội dung của `<main>`. Gồm: một
spacer co giãn, 3 icon trang trí (`layout-grid`/`moon`/`shield-check`), và khối danh tính hai dòng.

Ba icon **không có hành vi** và được đánh dấu `aria-hidden` + `disabled` để không thành điểm dừng bàn
phím chết. BD `admin_overview` mục 3 đã chốt chúng là trang trí, không đưa vào component inventory.

Khối danh tính hiện là placeholder: chưa có phiên đăng nhập thật
(`app/providers/auth-provider.tsx` chưa viết), nên nó hiển thị nhãn chung chứ không bịa tên người.

## 4. Vùng nội dung

`<main>` bọc nội dung trong một container `max-width: 1320px` căn giữa — con số lấy từ prototype, áp ở
khung thay vì mỗi màn tự khai, nếu không màn hình rộng sẽ kéo giãn card vô hạn.

Mỗi màn tự quyết có `PageHeader` hay không. `admin_overview` **cố ý không có tiêu đề hiển thị** (prototype
đi thẳng từ toolbar vào lưới), 10 màn còn lại đều có. Vì vậy `PageHeader` là primitive của `shared/ui`,
không nằm trong khung này.

### 4.1. Quy ước KPI: chỉ một trang tổng quan (chốt 2026-10-01, owner instruction)

**Chỉ một trang tổng quan hiển thị KPI; các trang danh sách chỉ có tiêu đề, bộ lọc, danh sách và phân trang.**
Trang tổng quan của khu Admin là `admin_overview` (`ADM0101`); khu Giảng viên có trang tổng quan riêng. Hệ quả:
dải thẻ chỉ số (`StatCard`) đã bị xoá khỏi quản lý bài tập, quản lý người dùng, nhật ký hệ thống và quản lý câu
hỏi phỏng vấn; trường `stats` bị xoá khỏi model và mock, các khoá i18n `stat.*` bị xoá. Các panel phụ cạnh danh
sách (phân bố theo vai trò, việc cần xử lý, quản trị viên hoạt động, biểu đồ phân loại, phân bố theo chủ đề và bài
cần chú ý) **không** bị xoá. Từ 2026-10-01 quy ước áp tiếp cho `admin_queue_monitor` và `admin_ai_usage` (owner
chốt trong ngày, hai dải thẻ đã xoá cùng `kpis` / `stats` trong model, mock và i18n; các số liệu hàng đợi và token
chưa có chỗ mới trên tổng quan). Thẻ "Yêu cầu đặt lại mật khẩu" chuyển lên tổng quan Admin làm thẻ thứ ba. Quy ước
chưa áp cho trang khu Người học và trang chi tiết lớp. Mỗi BD màn bị ảnh hưởng ghi lại ở Khu vực B
"đã bỏ" của Sheet 5 và 6.

### 4.2. Quy ước icon thao tác + tooltip (`IconAction`, chốt 2026-10-01)

Thao tác ở cuối dòng của bảng (sửa, xoá, nhân bản...) hiển thị bằng nút vuông chỉ có icon thay cho nút chữ, dùng
component `shared/ui/icon-action.tsx` [SoT: 05-coding/frontend/src/shared/ui/icon-action.tsx:1-80]:

| Thuộc tính | Quy ước |
| :--- | :--- |
| Hình dạng | Nút vuông 32x32px (`h-8 w-8`), viền theo `--color-border`; render `<button>`, hoặc `<a>` khi có `href` (ví dụ nút Sửa dẫn sang màn soạn) |
| Tên hành động | Một chuỗi `label` dùng cho cả `aria-label` và nội dung tooltip |
| Tooltip | Hộp nhỏ nằm dưới nút, hiện khi rê chuột hoặc focus bàn phím; gắn vào `<body>` bằng portal và định vị `fixed`, vì vùng bọc bảng có `overflow-x: auto` sẽ cắt tooltip của dòng cuối nếu để trong luồng |
| Tông | `default`; `danger` (màu tiêu cực của khu Admin) cho thao tác phá huỷ như Xoá |
| Màn đang dùng | `problem_management` (Sửa, Xoá) |

Giới hạn đã biết: tooltip chỉ hoạt động bằng chuột và focus, chưa có hành vi cho thiết bị cảm ứng; `aria-label`
là tên hành động chung ("Sửa"), không còn kèm tên bài như "Sửa bài {title}" — xem `06-plan/PROTOTYPE_DEBT.md`.

### 4.3. Khe `leading` của `PageHeader` (2026-10-01)

`PageHeader` có thêm prop tuỳ chọn `leading`, đặt ở đầu hàng tiêu đề trước phần tên màn; dùng cho icon quay lại ở
các màn chi tiết và màn soạn [SoT: 05-coding/frontend/src/shared/ui/page-header.tsx:19-35]. Không có `leading` thì
bố cục không đổi.

### 4.4. Nguyên tắc cấu hình của khu Admin (chốt 2026-10-01, owner uỷ quyền cân nhắc)

Nguyên tắc owner nêu ngày 2026-10-01: "Đã là admin hệ thống thì chủ động cấu hình mọi thứ, không bị giới hạn gì."
**Admin cấu hình được mọi tham số và danh mục; chỉ giữ các bất biến bảo toàn như ADMIN hoạt động cuối cùng và nhật ký
bất biến.** Hệ quả cho mọi BD màn Admin: ngưỡng, giới hạn, đơn giá, thời hạn lưu và danh mục (ví dụ chủ đề câu hỏi
phỏng vấn) là dữ liệu hoặc cấu hình do ADMIN đặt, không hard-code và không khoá cứng trong `application.yml`; một
ràng buộc chỉ còn chặn cứng khi nó bảo vệ chính khả năng cấu hình của admin hoặc tính toàn vẹn dấu vết kiểm toán, các
ràng buộc còn lại chuyển thành cảnh báo xác nhận. Căn cứ và danh sách quyết định cụ thể:
`DEC-2026-1001-admin-configurable-settings` (`.nexa/control/decision-registry.md`). Các màn cấu hình tương ứng chưa
dựng, ghi `06-plan/PROTOTYPE_DEBT.md` mục 16.2.

## 5. Bảng màu và token

Scope `.admin-shell` ghi đè các token ngữ nghĩa (`--color-background`, `--color-surface`, `--color-text`,
`--color-border`, `--color-primary`...) bằng giá trị riêng của khu Admin. Nhờ vậy mọi component đã đọc
`--color-*` tự động đổi sang bảng màu Admin mà không sửa dòng nào trong component.

Nguyên tắc khi thêm token: giá trị phải chép từ `.dc.html` kèm số dòng trong comment, hoặc đánh dấu
`[SoT: Suy luận]` ngay tại chỗ. Chi tiết danh sách token: `05-coding/frontend/src/app/globals.css`.

## 6. Trạng thái khung

| Trạng thái | Mô tả |
| :--- | :--- |
| `expanded` | Sidebar 224px, nhãn đầy đủ |
| `collapsed` | Rail 72px, chỉ icon, nhãn qua `title` |
| `narrow-viewport` | Dưới 1024px, ép rail bất kể tuỳ chọn |
| `group-open` / `group-closed` | Từng nhóm nav độc lập; nhóm chứa route hiện tại mặc định mở |

Khung **không có** trạng thái `loading`/`error` — nó không gọi API nào. Dữ liệu nav là hằng số biên dịch.

## 7. Câu hỏi mở

| # | Câu hỏi | Ghi chú |
| :-: | :--- | :--- |
| Q1 | Khối danh tính lấy dữ liệu từ đâu khi có phiên thật? | Chờ `app/providers/auth-provider.tsx` và `03-dd/api/identity.md` |
| Q2 | Ba icon trang trí ở toolbar cuối cùng thành gì, hay bỏ hẳn? | BD `admin_overview` chốt là trang trí; nếu bỏ hẳn thì toolbar chỉ còn khối danh tính |
| Q3 | Khu Giảng viên (`/instructor/*`) dùng lại khung này hay có khung riêng? | **ĐÃ TRẢ LỜI 2026-09-21 — khung riêng**, nav phẳng 5 mục thay vì 4 nhóm gập, khối danh tính ở đáy sidebar thay vì toolbar, và không có toolbar dùng chung. Chi tiết: `02-bd/screens/teacher/_shell.md` mục 2 |
| Q4 | Khung có cần chân trang (footer) không? | **ĐÃ CHỐT 2026-09-21 — có, dựng thật**, áp cho mọi route kể cả `admin_overview`. Cấu trúc, divergence với prototype và hiện thực: mục 9 |

## 8. Tham chiếu

- `09-layoutBase/Admin - Tổng quan.dc.html` — bằng chứng layout gốc của sidebar và toolbar.
- `02-bd/screens/admin/ADM0101_overview.md` mục 2 điểm 1 — nơi khung được mô tả lần đầu.
- `07-review/bd_screens_admin_open_questions_260913.md` mục 4 — nơi yêu cầu tách file này.
- `05-coding/frontend/src/widgets/app-shell/` — hiện thực.
- Quyết định: `DEC-2026-0825-frontend-base-architecture`, `DEC-2026-0915-admin-separate-login-route`.

## 9. Chân trang (chốt 2026-09-21)

Trả lời Câu hỏi mở Q4 (mục 7): chủ dự án chốt **dựng chân trang thật**, áp cho **mọi** route `/admin/*`
kể cả `admin_overview` — không phải phần tuỳ chọn theo từng màn. Phụ lục này đứng ở cuối file thay vì
chen vào mục 1-8 để giữ nguyên mọi số dòng đã bị 5 file BD khác trích dẫn (`_shell.md:19-21`, `:29-43`,
`:64`, `:67-68`, `:78-80`, `:109`...) — chen giữa sẽ làm lệch toàn bộ, biến trích dẫn đúng thành trích dẫn
sai một cách âm thầm.

### 9.1 Cấu trúc

Ba khu trong một hàng flex, nằm **bên trong** `<main>`, là phần tử cuối cùng sau nội dung màn — không phải
sibling của `<aside>`/`<main>` như sidebar/toolbar/nền
[Nguồn: 09-layoutBase/Admin - Người dùng.dc.html:146,277-290,292 — `<footer>` nằm giữa `<main>` và `</main>`].

| Khu | Nội dung | Nguồn |
| :--- | :--- | :--- |
| Trái | "AlgoPrep Admin · v2.4.1" (đậm) + "© 2026 AlgoPrep. Bảng điều khiển nội bộ." (phụ) | `Admin - Người dùng.dc.html:279-280` |
| Giữa/phải | Chấm tròn trạng thái + "Mọi dịch vụ hoạt động bình thường" | `Admin - Người dùng.dc.html:282-284` |
| Phải | 4 liên kết: "Tài liệu API", "Trạng thái go-judge", "Nhật ký thay đổi", "Hỗ trợ" | `Admin - Người dùng.dc.html:286-288,390-393` |

### 9.2 Divergence có chủ đích thứ ba, khác prototype

Nối tiếp 2 divergence đã ghi ở mục 2.2. Prototype riêng của `admin_overview`
(`09-layoutBase/Admin - Tổng quan.dc.html:296-297`) **không có** chân trang — tệp kết thúc ngay sau
`</main>`, đã ghi nhận ở `02-bd/screens/admin/ADM0101_overview.md` mục 4. Quyết định này cố ý phủ lên
bằng chứng đó: nhất quán toàn khu Admin (một khung, một chân trang) được ưu tiên hơn bám đúng 1:1 một
prototype không đầy đủ của riêng một màn.

### 9.3 Bốn liên kết là trang trí

`href="#"` trong mọi prototype — chưa có đích thật. Giữ đúng tinh thần đã áp cho 3 icon toolbar (mục 3):
không phải điểm dừng bàn phím chết. Cho tới khi có đích thật (`03-dd/api/*` hoặc route nội bộ), 4 liên kết
render dạng chữ tĩnh, không phải `<a>` sống, không `tabindex`.

### 9.4 Hiện thực

`widgets/app-shell/ui/admin-footer.tsx`, render trong `app-shell.tsx` ngay sau `{children}`, cùng bên
trong khối `max-width: 1320px` — đúng vị trí "cuối `<main>`" ở mục 9.1, không phải một `<footer>` cố định
đáy viewport.

### 9.5 Cần đồng bộ ngược

Hai file sau ghi "chân trang: không dựng" từ trước quyết định này, cần sửa theo quyết định 9 này:
`02-bd/screens/admin/ADM0101_overview.md` mục 4, `02-bd/screens/admin/ADM0202_permission_matrix.md`
đoạn ngay trước mục 4.5.
