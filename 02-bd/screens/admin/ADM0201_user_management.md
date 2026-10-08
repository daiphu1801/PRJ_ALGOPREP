# Tài liệu thiết kế cơ bản (BD) — Quản lý người dùng (`ADM0201`)

## Phương châm tài liệu [Nội bộ — không cần khách duyệt]

- Viết theo **mẫu 9 sheet**. Bộ thẻ đóng, quy ước ID, ký hiệu chuẩn, ranh giới với tài liệu khác và
  checklist kiểm toán nằm ở `02-bd/_rules/bd-template-9sheet.md` — file này không chép lại.
- Phần gắn nhãn `[Nội bộ]` phục vụ sinh mã và tự kiểm toán, không phải yêu cầu của khách hàng: mục 4.5, mục
  7.3 và các ghi chú quy ước.
- Mã màn `ADM0201` theo bảng mã ở `02-bd/_rules/bd-template-9sheet.md` mục 8; tên file mang tiền tố mã.
- Màn này không có màn con, có 3 popup xác nhận (Đặt lại mật khẩu, Đổi vai trò, Khoá tài khoản) và từ V0.7
  (2026-10-01) thêm popup nhập liệu "Thêm tài khoản". **Sửa V0.9 (2026-10-05):** sau khi bỏ hai hành động
  gộp, còn **2 popup**: Khoá tài khoản (có lý do + tuỳ chọn gửi email, F1-32) và Mở khoá tài khoản.
  Hai popup "Đặt lại mật khẩu" và "Đổi vai trò" không còn ở chế độ gộp — xem Q7. **Sửa V1.0 (2026-10-08):**
  hai popup đó **quay lại** nhưng chỉ cho **một tài khoản**, mở từ cột "Thao tác" cuối mỗi dòng; màn có **4 popup xác nhận**
  (Đặt lại mật khẩu, Đổi vai trò, Khoá, Mở khoá) và popup nhập liệu "Thêm tài khoản".

> Đọc cùng `01-rd/screens/admin/ADM0201_user_management.md` (hành vi ở mức yêu cầu, không lặp lại ở đây) và ba
> file BD module: `02-bd/architecture/identity.md`, `02-bd/database/identity.md`,
> `02-bd/security/identity.md`. Khung điều hướng, thanh công cụ và chân trang khu Admin dùng lại
> `02-bd/screens/admin/_shell.md`, không mô tả lại.
>
> **Phạm vi đã chốt trước khi viết BD** — kế thừa nguyên văn từ BD cũ (V0.1) và RD mục 5, không mở lại:
> - **Không thiết kế** mục "Báo cáo nghi gian lận" — ngoài phạm vi đồ án
>   (`DEC-2026-0831-remove-plagiarism-report`) [Nguồn: 01-rd/screens/admin/ADM0201_user_management.md:63].
> - **Không thiết kế** mục "Đề nghị cấp quyền giảng viên" và luồng tự yêu cầu nâng vai trò — chỉ ADMIN được
>   đổi vai trò, đúng nguyên văn F1-13 [Nguồn: 01-rd/screens/admin/ADM0201_user_management.md:62].
> - "Yêu cầu đặt lại mật khẩu" **tách khỏi** khối "Cần xử lý", chuyển sang dải chỉ số tổng
>   [Nguồn: 01-rd/screens/admin/ADM0201_user_management.md:64]. **Cập nhật 2026-10-01:** dải chỉ số tổng đã bị
>   bỏ khỏi UI (chỉ trang tổng quan hiển thị KPI, trang danh sách chỉ có bộ lọc và danh sách — xem
>   `02-bd/screens/admin/_shell.md`). Chỉ số này đã **chuyển lên trang tổng quan Admin làm thẻ thứ ba**
>   (`DEC-2026-1001-single-overview-page-kpi`, xem `02-bd/screens/admin/ADM0101_overview.md` Khu vực A NO 9-12); màn
>   này không còn hiển thị nó.

---

## Sheet 1. Bìa

| Mục | Giá trị |
| :--- | :--- |
| Hệ thống | AlgoPrep |
| Phân hệ | `identity` (F1) |
| Công đoạn | BD — Thiết kế cơ bản |
| Phân loại | Màn hình |
| Tên màn | Quản lý người dùng |
| Mã màn hình | `ADM0201` |
| Tên vật lý (slug) | `admin_user_management` |
| Trục tài liệu | Màn hình (`02-bd/screens/`) |
| Actor | A3 (`ADMIN`) |
| Phiên bản | V1.0 |
| Người tạo | Nhóm phát triển AlgoPrep |
| Ngày tạo | 2026/09/15 |
| Người cập nhật | Nhóm phát triển AlgoPrep |
| Ngày cập nhật | 2026/10/08 |

---

## Sheet 2. Lịch sử sửa đổi

| Ver | Sheet bị sửa | Nội dung sửa | Ngày | Người sửa |
| :--- | :--- | :--- | :--- | :--- |
| V0.1 | Toàn bộ | Tạo mới theo cấu trúc 8 mục văn xuôi (phạm vi đã chốt, layout, component inventory, screen states, API tiêu thụ, điều hướng, quyền truy cập, câu hỏi mở) | 2026/09/15 | Nhóm phát triển AlgoPrep |
| V0.2 | Toàn bộ | Chuyển sang mẫu 9 sheet. Bổ sung Sheet 3 sơ đồ chuyển màn, Sheet 5 và 6 danh sách item kèm nguồn giá trị từng trường, Sheet 8 danh sách sự kiện, Sheet 9 đặc tả kiểm tra (gồm các trường hợp nguy hiểm: tự khoá chính mình, tự hạ vai trò, ADMIN cuối cùng). Phát hiện thiếu nguồn dữ liệu cho trạng thái "Chờ xác thực email" — mở Q3 | 2026/09/20 | Nhóm phát triển AlgoPrep |
| V0.3 | 5, 6 | Áp `DEC-2026-0922-users-and-admin-conflict-resolutions`: điền nguồn cột `users.last_active_at` cho thẻ "Đang hoạt động 24 giờ" và cột "Hoạt động". Đóng Q4 | 2026-09-24 | AI |
| V0.4 | 3, 4, 5, 6, 7, 8; Câu hỏi mở | Bỏ dải 5 thẻ chỉ số tổng khỏi UI (Khu vực B còn ghi chú rỗng, giữ chữ cái C trở đi); `UserManagementStatsDto` chỉ còn `pendingVerificationCount`; đánh số lại DTO 9 → 11; Q5 hết áp dụng. Hai khối bên dưới (Phân bố theo vai trò, Cần xử lý) giữ nguyên. Theo quy ước "chỉ một trang tổng quan hiển thị KPI" | 2026-10-01 | AI |
| V0.5 | 4, Câu hỏi mở | Đồng bộ ngày 2026-10-01: chỉ số "Yêu cầu đặt lại mật khẩu" đã chuyển lên trang tổng quan Admin làm thẻ thứ ba (sửa các câu "chưa có chỗ hiển thị" và "nếu trang tổng quan dùng"); sửa trích dẫn khoá Redis `identity:pwreset:<userId>` từ dòng 132 sang đúng dòng 134 của `02-bd/database/identity.md`; thêm đề xuất cho Q1, Q6 theo nguyên tắc admin (chờ owner xác nhận) | 2026-10-01 | AI |
| V0.6 | Sheet 3, 5, 6, 8, 9, Câu hỏi mở | Đã chốt 2026-10-01 (owner uỷ quyền cân nhắc), xem `DEC-2026-1001-admin-configurable-settings`: (1) Q1: "Thêm tài khoản" **nằm trong phạm vi** (ADMIN tạo tài khoản INSTRUCTOR hoặc STUDENT, mật khẩu tạm qua email, ghi `system_audit_logs`), chưa dựng; cần mở rộng RD F1-13 (không sửa `01-rd/req` ở đợt này); (2) Q6: giữ **chặn cứng** "luôn còn ít nhất một ADMIN hoạt động" (bất biến bảo toàn khả năng cấu hình của admin); "không tự khoá" và "không tự hạ vai trò" đổi từ chặn cứng sang **cảnh báo xác nhận** (cho phép nếu xác nhận). Sửa Sheet 9 NO 6, 7 từ mức Lỗi sang Cảnh báo | 2026-10-01 | AI |
| V0.7 | Phương châm, Sheet 3, 4, 5, 6, 7, 8, 9, Câu hỏi mở | Đồng bộ với prototype dựng 2026-10-01 (kiểm ở vòng 5): popup "Thêm tài khoản" đã dựng nên bỏ các câu "chưa dựng", "prototype chỉ có nút", "chỉ dựng nút khi...". Mô tả đủ popup (họ tên bắt buộc, email đúng định dạng và không trùng, vai trò `INSTRUCTOR` hoặc `STUDENT`, thông báo "mật khẩu tạm đã gửi", tài khoản mới ở trạng thái chờ xác thực), thêm EVT-17 đến EVT-19, Sheet 9 NO 13-15, DTO NO 12, endpoint NO 8. RD F1-13 đã mở rộng (`01-rd/req/identity.md:69`) nên Q1 chỉ còn thiếu bảng và endpoint thật. Luồng khoá (chặn cứng ADMIN cuối cùng, cảnh báo khi tự khoá) đối chiếu `model/guards.ts` và khớp, không đổi quy tắc | 2026/10/01 | AI |
| V0.8 | Sheet 5, 6, 8, 9 | Áp `DEC-2026-1003-toast-feedback-channel`: kết quả thao tác và lỗi nhập liệu ghi là toast dùng chung, ô sai chỉ đổi viền đỏ. | 2026/10/03 | AI |
| V0.9 | Phương châm, Sheet 3, 4, 5, 6, 8, 9, Câu hỏi mở | Owner chỉ đạo 2026-10-05 khi review prototype. (1) Thanh hành động gộp chỉ còn **Khoá** và **Mở khóa**; xoá "Đặt lại mật khẩu" và "Đổi vai trò" khỏi chế độ gộp — phạm vi không mất, chuyển sang thao tác trên một tài khoản (đóng Q2, xem RD Q7). Đây cũng là câu trả lời cho Q2 về vị trí nút mở khoá. (2) Popup khoá dựng lại thành `Modal` có **lý do** + **tuỳ chọn gửi email** theo mã mới **F1-32** (`01-rd/req/identity.md`), kèm danh sách tên tài khoản bị ảnh hưởng và cảnh báo số tài khoản bị bỏ qua. (3) Lựa chọn bị xoá khi đổi bộ lọc (Sheet 6 NO 2 vốn đã yêu cầu, prototype quên). (4) Nhãn ô chọn tất cả mang số dòng đang hiện. | 2026/10/05 | AI |
| V1.0 | Phương châm, Sheet 3, 4, 5, 6, 7, 8, 9, Câu hỏi mở | Đồng bộ với prototype dựng 2026-10-08 (`DEC-2026-1008-admin-system-screens-review`). (1) Thêm cột **"Thao tác"** cuối bảng với ba nút trên mỗi dòng: Đổi vai trò, Đặt lại mật khẩu, Khoá/Mở khoá — đây là điểm chạm mà Q7 để lại cho DD; hai popup đổi vai trò và đặt lại mật khẩu quay lại ở dạng **một tài khoản**; khoá/mở khoá từ dòng dùng lại popup đã có. (2) Bộ lọc trạng thái thêm "Chờ xác thực" (nay 3 lựa chọn ngoài "Tất cả"). (3) Khối "Cần xử lý" không còn dòng "Đề nghị cấp quyền giảng viên" (tàn dư dữ liệu mẫu và i18n, RD Q2 đã chốt loại). (4) Sheet 8 EVT-8 đến EVT-11 viết lại theo một tài khoản; sửa mâu thuẫn V0.9 (ghi đã xoá nhưng bảng vẫn còn bốn dòng gộp cũ). (5) Thêm Q8: tài khoản chỉ đăng nhập OAuth chưa có cờ nhận biết nên nút đặt lại mật khẩu kích hoạt cho mọi dòng | 2026/10/08 | AI |

---

## Sheet 3. Sơ đồ chuyển màn

> [Nội bộ] Quy ước vẽ: bố trí từ trái sang phải. Màn gọi tới tô tím, màn hiện tại tô xanh nhạt, popup tô
> vàng. Màu và `[Nguồn:]` dùng để truy vết, không phải yêu cầu của khách hàng.

### 3.1 Danh sách chuyển màn

#### Khung điều hướng Admin → Quản lý người dùng

[Điều kiện mở] Chọn mục con "Người dùng" trong nhóm "Hệ thống" ở thanh điều hướng bên trái.

[Chế độ mở] Chế độ danh sách, không lọc sẵn — tab vai trò và tab trạng thái đều ở "Tất cả".

[Thông tin truyền] Không có.

[Giá trị trả về] Không có.

[Khi thành công] Tải trang đầu của bảng người dùng, khối phân bố theo vai trò và khối cần
xử lý.

[Khi huỷ] Không có.

#### Quản lý người dùng → Popup Xác nhận đặt lại mật khẩu (một tài khoản, mới 2026-10-08)

[Điều kiện mở] Bấm nút "Đặt lại mật khẩu" ở cột "Thao tác" của một dòng. Nút kích hoạt cho **mọi** dòng, kể cả
tài khoản chỉ đăng nhập OAuth (khoảng trống đã biết, xem Q8).

[Chế độ mở] Chế độ xác nhận, không nhập liệu; tiêu đề nêu tên tài khoản, nội dung nêu **email** nhận mật khẩu tạm
[Nguồn: 05-coding/frontend/src/views/admin/user-management/ui/admin-user-management-view.tsx:481-490].

[Thông tin truyền] Tài khoản của dòng được bấm.

[Giá trị trả về] Kết quả chọn "Đặt lại" hoặc "Huỷ".

[Khi thành công] Đóng popup, hiện toast "Đã gửi mật khẩu tạm tới {email}" và ghi một dòng `system_audit_logs` (F1-14).
Mật khẩu tạm không hiển thị cho quản trị viên.

[Khi huỷ] Đóng popup, không có gì thay đổi.

#### Quản lý người dùng → Popup Đổi vai trò (một tài khoản, mới 2026-10-08)

[Điều kiện mở] Bấm nút "Đổi vai trò" ở cột "Thao tác" của một dòng.

[Chế độ mở] Popup nhập liệu có một ô chọn vai trò (Người học / Giảng viên / Quản trị), khởi tạo bằng vai trò hiện
tại của tài khoản [Nguồn: 05-coding/frontend/src/views/admin/user-management/ui/change-role-dialog.tsx:22-30,59-70].

[Thông tin truyền] Tài khoản của dòng được bấm và cờ "đây là chính tài khoản đang đăng nhập".

[Giá trị trả về] Tài khoản kèm vai trò mới, hoặc không có gì nếu huỷ.

[Khi thành công] Nút "Đổi vai trò" chỉ kích hoạt khi vai trò chọn **khác** vai trò hiện tại [Nguồn: 05-coding/frontend/src/views/admin/user-management/ui/change-role-dialog.tsx:47]. Tự hạ vai
trò chính mình thì popup hiện cảnh báo nhưng vẫn cho làm (Sheet 9 NO 7) [Nguồn: 05-coding/frontend/src/views/admin/user-management/ui/change-role-dialog.tsx:72-76]. Hạ ADMIN hoạt động cuối cùng thì
**chặn cứng**: hiện toast cảnh báo, không đổi gì (Sheet 9 NO 8) [Nguồn: 05-coding/frontend/src/views/admin/user-management/ui/admin-user-management-view.tsx:190-195]. Đổi được thì
hiện toast "Đã đổi vai trò của {tên} thành {vai trò}" và ghi một dòng `system_audit_logs` (F1-14).

[Khi huỷ] Đóng popup, bỏ vai trò đã chọn dở.

#### Quản lý người dùng → Popup Xác nhận khoá tài khoản

[Điều kiện mở] Đang chọn ít nhất một tài khoản **chưa bị khóa** và bấm "Khóa tài khoản" trên thanh hành
động gộp, **hoặc** bấm nút "Khóa tài khoản" ở cột "Thao tác" của một dòng chưa bị khoá (tập đích là đúng dòng đó,
bỏ qua 0 dòng) [Nguồn: 05-coding/frontend/src/views/admin/user-management/ui/admin-user-management-view.tsx:313-320]. Tập chọn mà khoá xong sẽ không còn ADMIN hoạt động nào thì không mở popup, chỉ hiện toast chặn.

[Chế độ mở] Popup nhập liệu trên nền xác nhận hành động phá huỷ — có lý do và công tắc gửi email, nên nút
xác nhận mang nhãn "Khóa tài khoản" chứ không phải "Xác nhận" chung [Nguồn: 01-rd/req/identity.md — F1-32].

[Thông tin truyền] Danh sách tài khoản thực sự bị khoá (đã tách khỏi tập chọn), số tài khoản bị bỏ qua, và
cờ tự khoá chính mình.

[Giá trị trả về] Kết quả gồm lý do và cờ gửi email, hoặc không có gì nếu huỷ.

[Khi thành công] Popup liệt kê **tên và email** từng tài khoản bị khoá (tối đa 8 dòng rồi gộp phần còn
lại thành "và {n} tài khoản khác"), cảnh báo các tài khoản bị khoá sẽ mất quyền đăng nhập ngay ở lần kiểm
quyền kế tiếp [Nguồn: 02-bd/security/identity.md:60-62], ô lý do rỗng và công tắc gửi email bật sẵn.

[Khi huỷ] Đóng popup, không tài khoản nào bị đổi trạng thái; lý do đã nhập bị xoá.

#### Quản lý người dùng → Popup Mở khóa tài khoản (mới 2026-10-05, đóng Q2)

[Điều kiện mở] Đang chọn ít nhất một tài khoản **đang bị khóa** và bấm "Mở khóa tài khoản" trên thanh hành
động gộp, **hoặc** bấm nút "Mở khóa tài khoản" ở cột "Thao tác" của một dòng đang bị khoá [Nguồn: 05-coding/frontend/src/views/admin/user-management/ui/admin-user-management-view.tsx:306-312].

[Chế độ mở] Chế độ xác nhận, không nhập liệu.

[Thông tin truyền] Danh sách tài khoản đang bị khóa trong tập chọn.

[Giá trị trả về] Kết quả chọn "Mở khóa" hoặc "Huỷ".

[Khi thành công] Popup liệt kê tên và email từng tài khoản sẽ mở khóa. **Không có ô lý do, không gửi
email** — mở khóa là hành động khôi phục quyền truy cập, người dùng vừa nhận thông báo bị khoá thì không
cần thông báo ngược lại [Nguồn: 01-rd/req/identity.md — F1-32].

[Khi huỷ] Đóng popup, giữ nguyên danh sách đang chọn.

#### Quản lý người dùng → Nhật ký hệ thống

[Điều kiện mở] Bấm liên kết "Nhật ký" ở góc phải khối "Cần xử lý".

[Chế độ mở] Không có.

[Thông tin truyền] Không có. Màn đích không được lọc sẵn theo tài khoản nào.

[Giá trị trả về] Không có.

[Khi thành công] Điều hướng sang màn `admin_system_log`. Danh sách đang chọn ở màn này bị bỏ, không mang
theo.

[Khi huỷ] Không có.

#### Quản lý người dùng → Popup Thêm tài khoản (đã chốt vào phạm vi 2026-10-01, prototype đã dựng)

[Điều kiện mở] Bấm nút "Thêm tài khoản" ở thanh tiêu đề.

[Chế độ mở] Popup nhập liệu, mỗi lần mở bắt đầu với họ tên và email rỗng, vai trò mặc định `STUDENT`.

[Thông tin truyền] Tập email các tài khoản đang có (prototype dùng để báo trùng ở phía giao diện).

[Giá trị trả về] Tài khoản vừa tạo, hoặc không có gì nếu người dùng huỷ.

[Khi thành công] Popup đóng và hiện toast "Đã tạo tài khoản" ghi rõ mật khẩu tạm đã gửi tới email vừa nhập [Nguồn: 05-coding/frontend/src/views/admin/user-management/ui/add-account-dialog.tsx:62-64]. Tài khoản mới hiện **đầu danh sách** với trạng thái "chờ xác thực". RD F1-13 đã mở rộng cho việc này (`01-rd/req/identity.md:69`); phần còn thiếu là bảng và endpoint thật, xem Câu hỏi mở Q1.

[Khi huỷ] Đóng popup, không tạo tài khoản.

[Nguồn: 05-coding/frontend/src/views/admin/user-management/ui/add-account-dialog.tsx:24-115; 05-coding/frontend/src/views/admin/user-management/ui/admin-user-management-view.tsx:323-328]

### 3.2 Sơ đồ

```mermaid
flowchart LR
    nav["Khung điều hướng Admin<br/>nhóm Hệ thống"] -->|"chọn Người dùng"| main["Quản lý người dùng<br/>admin_user_management"]
    main -->|"Khóa tài khoản"| lock["Popup Khóa tài khoản<br/>lý do, tuỳ chọn gửi email"]
    main -->|"Mở khóa tài khoản"| unlock["Popup Xác nhận<br/>mở khóa tài khoản"]
    main -->|"Đổi vai trò (nút trên dòng)"| role["Popup Đổi vai trò<br>một tài khoản"]
    main -->|"Đặt lại mật khẩu (nút trên dòng)"| reset["Popup Xác nhận<br>đặt lại mật khẩu"]
    main -->|"Khóa / Mở khóa (nút trên dòng)"| lock
    lock --> main
    unlock --> main
    role --> main
    reset --> main
    main -->|"Nhật ký"| log["Nhật ký hệ thống<br/>admin_system_log"]
    main -->|"Thêm tài khoản"| new["Popup Thêm tài khoản<br/>họ tên, email, vai trò"]
    new --> main

    classDef source fill:#F3E5F5,stroke:#9C5CC4,color:#000
    classDef screen fill:#E3F2FD,stroke:#3B82F6,color:#000
    classDef popup fill:#FFF7CC,stroke:#D4A72C,color:#000

    class nav,log source
    class main screen
    class lock,unlock,new,role,reset popup
```

[Nguồn: 09-layoutBase/Admin - Người dùng.dc.html:158,194-203,261,339-341; 01-rd/screens/admin/ADM0201_user_management.md:62-64; 01-rd/req/identity.md — F1-32; popup một tài khoản (2026-10-08): 05-coding/frontend/src/views/admin/user-management/ui/admin-user-management-view.tsx:286-325,472-492]

---

## Sheet 4. Bố cục màn hình

### 4.1 Tổng quan màn

[Mục đích màn] Cho quản trị viên tìm kiếm và lọc tài khoản người dùng, rồi khoá hoặc mở khoá một hoặc
nhiều tài khoản cùng lúc. F1-13 còn nêu đổi vai trò và đặt lại mật khẩu hộ: hai thao tác đó không đi qua thanh
chọn hàng loạt mà nằm ở **cột "Thao tác" trên từng dòng** (dựng 2026-10-08) — xem [Phạm vi] bên dưới và Q7.
[Nguồn: 01-rd/screens/admin/ADM0201_user_management.md:21-22; 01-rd/req/identity.md — F1-13, F1-32]

[Luồng nghiệp vụ chính]

1. **Hiển thị ban đầu**: vào màn, hệ thống tải song song ba nhóm dữ liệu — trang đầu bảng người dùng,
   khối phân bố theo vai trò và khối cần xử lý. Trong lúc chờ, mỗi khối hiển thị khung chờ đúng
   số dòng dự kiến.
2. **Thu hẹp danh sách**: quản trị viên gõ từ khoá theo tên hoặc email, chọn tab vai trò và tab trạng thái.
   Mỗi lần đổi điều kiện thì về trang 1 và bỏ toàn bộ lựa chọn đang có.
3. **Chọn tài khoản**: tích chọn từng dòng. Có ít nhất một dòng được chọn thì thanh hành động gộp hiện ra.
   Tập chọn có thể trộn dòng đang hoạt động và dòng đã bị khoá; mỗi nút chỉ nhận phần của mình và nút không
   có việc gì thì không kích hoạt.
4. **Thực hiện hành động**: bấm "Khóa tài khoản" hoặc "Mở khóa tài khoản", xác nhận trong popup. Cùng hai
   hành động đó (và đổi vai trò, đặt lại mật khẩu) còn có nút ở cột "Thao tác" của từng dòng, không cần chọn dòng
   trước: khoá/mở khoá dùng lại đúng hai popup của thanh gộp với tập đích là một tài khoản; đổi vai trò và đặt lại
   mật khẩu mở popup riêng cho **một** tài khoản. Popup khoá
   bắt nhập **lý do** và cho chọn **gửi email thông báo** (F1-32); popup mở khoá không có ô nào nhập.
   Hệ thống áp dụng hành động cho **từng** tài khoản thuộc phần đó.
5. **Ghi nhật ký**: mỗi tài khoản chịu tác động ghi **một dòng riêng** vào `system_audit_logs`, không gộp
   thành một dòng; dòng khoá kèm **lý do** [Nguồn: 01-rd/screens/admin/ADM0201_user_management.md:53-56;
   01-rd/req/identity.md — F1-32].
6. **Làm mới**: sau khi hành động thành công, tải lại bảng, xoá danh sách đang chọn.

[Người dùng] Quản trị viên đã đăng nhập, có Function `USER_MANAGEMENT`
[Nguồn: 02-bd/database/identity.md:47-49].

[Tệp liên quan] Không có. Màn này không xuất hay nhập tệp.

[Phạm vi]
- Không có "Báo cáo nghi gian lận" (`DEC-2026-0831-remove-plagiarism-report`).
- Không có luồng tự yêu cầu nâng vai trò; chỉ ADMIN đổi vai trò.
- **Không có trang chi tiết tài khoản.** Màn này là danh sách + hành động trên tập chọn + nút trên từng dòng.
  Hai hành động bị bỏ khỏi chế độ gộp (Q7) có điểm chạm thay thế là **cột "Thao tác" ở cuối mỗi dòng**, dựng
  2026-10-08 [Nguồn: 05-coding/frontend/src/views/admin/user-management/ui/admin-user-management-view.tsx:286-325]; DD chốt hợp đồng API, không mở `Fx-nn` mới [Nguồn: RD `ADM0201` Q7].
- **Chỉ hai hành động trạng thái được phép áp dụng hàng loạt** (khoá / mở khoá), theo F1-32.
- Không sửa hồ sơ người dùng (tên hiển thị, email, ngôn ngữ mặc định) — F1-13 chỉ nêu ba thao tác xử lý sự
  cố tài khoản.
- Không cấu hình ma trận phân quyền — thuộc màn `admin_permission_matrix` (`ADM0202`).
- Nút "Thêm tài khoản" **nằm trong phạm vi** (đã chốt 2026-10-01, RD F1-13 đã mở rộng `01-rd/req/identity.md:69`); prototype đã dựng popup, chưa có endpoint thật, xem Câu hỏi mở Q1.
- Nút mở khoá có hai chỗ: thanh gộp và nút trên dòng đang bị khoá (Câu hỏi mở Q2 đã đóng).

[Quyền sử dụng]
- Xem: được, khi có `USER_MANAGEMENT:READ`.
- Thêm: được, khi có `USER_MANAGEMENT:CREATE` (nút "Thêm tài khoản", đã chốt vào phạm vi 2026-10-01, prototype đã dựng popup, Q1).
- Sửa: được, khi có `USER_MANAGEMENT:UPDATE` — đổi vai trò, đổi trạng thái, đặt lại mật khẩu.
- Xoá: không. Khoá tài khoản là đặt `users.status = DEACTIVATED`, không xoá dòng
  [Nguồn: 02-bd/database/identity.md:21].

[Số bản ghi tối đa] Bảng người dùng phân trang phía máy chủ; prototype hiển thị 9 dòng một trang và nhãn
"Trang 1 trong 143" — kích thước trang thật chốt ở DD. Phân bố theo vai trò: đúng 4
dòng. Cần xử lý: 1 dòng sau khi cắt phạm vi.

[Nguồn: 01-rd/screens/admin/ADM0201_user_management.md:19-49; 02-bd/database/identity.md:8-24,45-49; 02-bd/security/identity.md:25-26]

### 4.2 DTO liên quan

- `AdminUserListItemDto`
- `UserManagementStatsDto` (chỉ còn `pendingVerificationCount` cho khối "Cần xử lý")
- `RoleDistributionItemDto`
- `RoleOptionDto`
- `BulkUserActionResultDto`

`[Suy luận]` — tên DTO do BD này đề xuất, `03-dd/api/identity.md` chốt lại.

### 4.3 Bảng dữ liệu liên quan (5)

| NO | Bảng | Ghi chú |
| --: | :--- | :--- |
| 1 | `users` | [Nguồn: 02-bd/database/identity.md:8-24] |
| 2 | `roles` | [Nguồn: 02-bd/database/identity.md:31-43] |
| 3 | `system_audit_logs` | [Nguồn: 02-bd/database/identity.md:103-107] |
| 4 | `user_problem_best_score` | [Nguồn: 02-bd/database/identity.md:111-112] |
| 5 | `user_submission_stats` | [Nguồn: 02-bd/database/identity.md:113-114] |

Chỉ số "Yêu cầu đặt lại mật khẩu" (thẻ đã bỏ khỏi màn 2026-10-01, nay thuộc trang tổng quan Admin) **không dùng
bảng PostgreSQL**: các khoá OTP quên mật khẩu nằm trên Redis theo mẫu `identity:pwreset:<userId>`, TTL 10 phút
[Nguồn: 02-bd/database/identity.md:134]; nguồn số liệu 24 giờ của thẻ mới là câu hỏi mở Q4 của `ADM0101`. Bảng `permissions` không liệt kê ở đây vì việc kiểm quyền do tầng
hạ tầng chung của `identity` thực hiện qua cache, không phải truy vấn của màn
[Nguồn: 02-bd/security/identity.md:35-37].

### 4.4 Vùng bố cục

Đối chiếu `09-layoutBase/Admin - Người dùng.dc.html` — bằng chứng bố cục chỉ-đọc, **không phải** design
system cuối cùng.

| Vùng | Vị trí trong prototype | Nội dung |
| :--- | :--- | :--- |
| Thanh điều hướng bên trái (khung chung Admin) | `:68-144` | Nhóm "Hệ thống" đang mở, mục con "Người dùng" đang chọn — dùng lại khung chung của mọi màn Admin |
| Thanh tiêu đề dính trên | `:148-159` | Tiêu đề, mô tả phụ, công tắc theme (khung chung), nút "Thêm tài khoản" |
| Dải chỉ số tổng | `:161-172`, dữ liệu `:422-428` | **Không dựng trên UI Next.js (2026-10-01)** — trang danh sách không hiển thị KPI |
| Khối bảng — hàng bộ lọc | `:176-192` | Ô tìm kiếm, tab vai trò 4 mục, tab trạng thái 3 mục, nhãn số kết quả. **Prototype Next.js (2026-10-08):** lọc trạng thái có "Tất cả" + 3 lựa chọn (Hoạt động, Chờ xác thực, Bị khóa) [Nguồn: 05-coding/frontend/src/views/admin/user-management/ui/admin-user-management-view.tsx:361-369] |
| Khối bảng — thanh hành động gộp | `:194-203` | Chỉ hiện khi `hasSelection`; nhãn số đã chọn và 3 nút hành động |
| Khối bảng — bảng người dùng | `:205-228`, tiêu đề cột `:206-207` | Lưới 7 cột, tối thiểu 700px, cuộn ngang khi hẹp. **Prototype Next.js (2026-10-08):** thêm cột thứ 8 "Thao tác" ở cuối, rộng 132px, căn phải, ba nút biểu tượng [Nguồn: 05-coding/frontend/src/views/admin/user-management/ui/admin-user-management-view.tsx:286-325] |
| Khối bảng — phân trang | `:230-236` | Nhãn trang và hai nút "Trước" / "Sau" |
| Cột trái dưới — "Phân bố theo vai trò" | `:240-256`, dữ liệu `:476-481` | 4 thanh ngang tỉ lệ |
| Cột phải dưới — "Cần xử lý" | `:258-274`, dữ liệu `:483-486` | Danh sách việc cần xử lý kèm liên kết "Nhật ký" |
| Chân trang (khung chung Admin) | `:277-290` | Phiên bản, trạng thái dịch vụ, liên kết phụ — dùng lại khung chung |

Hai khối dưới cùng nằm trong một lưới `repeat(auto-fit, minmax(320px, 1fr))` (`:239`): trên màn rộng là hai
cột ngang hàng, trên màn hẹp xếp chồng. Giữ nguyên cấu trúc này khi dựng Next.js; không quy định màu sắc,
khoảng cách hay typography ở BD.

### 4.5 Cấu trúc slice FSD [Nội bộ]

| Khối | Slice dự kiến | Căn cứ |
| :--- | :--- | :--- |
| Trang | `views/admin/user-management` | Quy ước FSD của dự án |
| Khung Admin | Dùng lại `widgets/admin-shell` | `02-bd/screens/admin/_shell.md` |
| Bảng người dùng | `widgets/user-table` + `entities/user` | Prototype `:174-237` |
| Bộ lọc | `features/user-filter` | Prototype `:176-192` |
| Hành động gộp | `features/user-bulk-action` | Prototype `:194-203` |
| Hai khối thống kê | `widgets/role-distribution`, `widgets/admin-todo-list` | Prototype `:239-274` |

`[Suy luận]` — ánh xạ slice do BD đề xuất, DD màn hình chốt lại.

> [Nội bộ] Ảnh minh hoạ đặt ở `08-diagram/02-bd/screens/admin/`, chụp bằng Playwright trên ứng dụng Next.js
> thật khi đã có mã chạy được. Chưa có thì tham chiếu prototype, không vẽ tay.

---

## Sheet 5. Danh sách item màn hình

> Quy ước ID, bộ thẻ và giá trị chuẩn: `02-bd/_rules/bd-template-9sheet.md` mục 2, 3, 4.

### Khu vực A — Thanh tiêu đề

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Thanh tiêu đề | | | | | | | | | | | | | |
| | 1 | Tiêu đề màn | `adminUserManagement.header.title` | - | - | Label | String | - | - | O | Quản lý người dùng | - | Tên màn hiển thị cố định<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] - |
| | 2 | Mô tả phụ | `adminUserManagement.header.subtitle` | - | - | Label | String | - | - | O | Tài khoản, vai trò và trạng thái truy cập | - | Mô tả ngắn phạm vi màn<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] - |
| | 3 | Thêm tài khoản | `adminUserManagement.header.btnAddUser` | - | - | Button | - | - | - | I | - | - | Tạo tài khoản thủ công: ADMIN chọn vai trò `INSTRUCTOR` hoặc `STUDENT`, đặt mật khẩu tạm gửi qua email, bắt đổi ở lần đăng nhập đầu, mỗi lần tạo ghi một dòng `system_audit_logs`. **Đã chốt vào phạm vi 2026-10-01** (`DEC-2026-1001-admin-configurable-settings`); RD F1-13 đã mở rộng (`01-rd/req/identity.md:69`), xem Q1. Bấm nút chỉ mở popup "Thêm tài khoản" (Popup NO 4-11)<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-15 |

### Khu vực B — Dải chỉ số tổng (đã bỏ)

Khu vực này **không còn item nào** kể từ 2026-10-01: 5 thẻ chỉ số (Tổng tài khoản, Đang hoạt động 24 giờ, Chờ
xác thực email, Bị khoá, Yêu cầu đặt lại mật khẩu) cùng biến động và chú thích đã bị xoá khỏi UI theo quy ước
"chỉ một trang tổng quan hiển thị KPI" (`02-bd/screens/admin/_shell.md`). Giữ nguyên chữ cái khu vực C trở đi để
các tham chiếu hiện có không phải đánh số lại.

### Khu vực C — Bộ lọc và hành động gộp

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Bộ lọc và hành động gộp | | | | | | | | | | | | | |
| | 1 | Ô tìm kiếm | `adminUserManagement.filter.query` | `users` | `display_name`, `email` | TextBox | String | 100 | - | I | rỗng | - | Tìm theo tên hoặc email. Prototype ghi placeholder "Tìm theo tên, email hoặc tài khoản" nhưng `users` không có cột tên đăng nhập riêng, nên chỉ khớp hai cột này<br>[Nguồn giá trị] Giá trị người dùng nhập<br>[EVT liên quan] EVT-2 |
| | 2 | Tab vai trò | `adminUserManagement.filter.roleTabs` | `roles` | `base_category` | Button | Enum | - | - | I | Tất cả | - | 4 tab: Tất cả, Người học, Giảng viên, Quản trị. Lọc theo `roles.base_category` **chứ không phải** `roles.code`, vì role tuỳ biến vẫn phải rơi đúng nhóm cơ bản [Nguồn: 02-bd/architecture/identity.md:74-80]<br>[Nguồn giá trị] Nhãn tĩnh i18n map từ `base_category`<br>[EVT liên quan] EVT-3 |
| | 3 | Tab trạng thái | `adminUserManagement.filter.statusTabs` | `users` | `status` | Button | Enum | - | - | I | Tất cả | - | 4 lựa chọn: Tất cả, Hoạt động, Chờ xác thực, Bị khóa. **Sửa 2026-10-08:** trước đó thiếu "Chờ xác thực" dù bảng có nhãn đó; prototype Next.js đã dựng đủ [Nguồn: 05-coding/frontend/src/views/admin/user-management/ui/admin-user-management-view.tsx:361-369]. Lọc "Chờ xác thực" vẫn phụ thuộc nơi lưu trạng thái này (Q3)<br>[Nguồn giá trị] Nhãn tĩnh i18n map từ `users.status`<br>[EVT liên quan] EVT-4 |
| | 4 | Số kết quả | `adminUserManagement.filter.resultCount` | - | - | Label | String | - | - | O | - | `{số} / {số} tài khoản` | Số dòng khớp bộ lọc trên tổng số<br>[Công thức] Số bản ghi khớp điều kiện lọc, chia cho tổng số tài khoản — cả hai lấy từ phản hồi của `ListUsers`<br>[EVT liên quan] EVT-2, EVT-3, EVT-4 |
| | 5 | Nhãn số đã chọn | `adminUserManagement.bulk.selectionLabel` | - | - | Label | String | - | - | O | - | `Đã chọn {số} tài khoản` | Số tài khoản đang được tích chọn<br>[Công thức] Đếm số dòng đang tích chọn ở trạng thái màn hình<br>[EVT liên quan] EVT-5 |
| | 6 | Đặt lại mật khẩu | `adminUserManagement.bulk.btnResetPassword` | - | - | Button | - | - | - | I | - | - | **Đã bỏ khỏi thanh hành động gộp 2026-10-05 (Q7)** — reset mật khẩu là việc riêng cho từng người, và không áp dụng với tài khoản chỉ đăng nhập bằng OAuth (Sheet 9 NO 10). Phạm vi F1-13 không mất; điểm chạm mới chốt ở DD |
| | 7 | Đổi vai trò | `adminUserManagement.bulk.btnChangeRole` | - | - | Button | - | - | - | I | - | - | **Đã bỏ khỏi thanh hành động gộp 2026-10-05 (Q7)** — ép cả nhóm về một vai trò đích là thao tác sai nghĩa, không hoàn tác được. Phạm vi F1-13 không mất; điểm chạm mới chốt ở DD |
| | 8 | Khóa tài khoản | `adminUserManagement.bulk.btnLock` | - | - | Button | - | - | - | I | - | - | Mở popup xác nhận khoá các tài khoản đang hoạt động trong tập chọn (F1-13, F1-32)<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-12 |
| | 9 | Mở khoá tài khoản | `adminUserManagement.bulk.btnUnlock` | - | - | Button | - | - | - | I | - | - | Mở popup xác nhận mở khoá các tài khoản đang bị khoá trong tập chọn (F1-13). **Mới 2026-10-05**, đây là câu trả lời cho Q2<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-19 |

### Khu vực D — Bảng người dùng

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Bảng người dùng | | | | | | | | | | | | | |
| | 1 | Danh sách người dùng | `adminUserManagement.list` | `users` | - | List | List | - | - | O | rỗng | - | Mỗi dòng là một tài khoản. Phân trang phía máy chủ<br>[Nguồn giá trị] Kết quả gọi `ListUsers`<br>[EVT liên quan] EVT-1 |
| | 2 | Ô chọn dòng | `adminUserManagement.list.col.checkbox` | - | - | Button | Boolean | - | - | I | Không chọn | - | Tích chọn tài khoản trên dòng đó để đưa vào hành động gộp. Trạng thái chỉ tồn tại trên màn, không lưu máy chủ<br>[Nguồn giá trị] Trạng thái chọn của màn hình<br>[EVT liên quan] EVT-5 |
| | 2a | Chọn tất cả đang hiển thị | `adminUserManagement.list.col.selectAll` | - | - | Button | Boolean | - | - | I | Không chọn | - | Chọn mọi dòng **đang có trên trang**, không phải mọi tài khoản khớp bộ lọc. **Nhãn mang số dòng** ("Chọn 9 tài khoản đang hiển thị") — ghi "chọn tất cả" trên trang phân thật khiến người dùng tưởng đã chọn hết 143 tài khoản rồi bấm "Khóa tài khoản" **[SoT: Suy luận — sửa 2026-10-05, chưa có prototype nào vẽ nhãn này]**<br>[Công thức] Số dòng khớp bộ lọc hiện tại trên trang<br>[EVT liên quan] EVT-5 |
| | 3 | Chữ viết tắt | `adminUserManagement.list.col.initials` | `users` | `display_name` | ListColumn | String | 2 | - | O | - | 2 ký tự in hoa | Vòng tròn chữ cái đầu thay ảnh đại diện<br>[Công thức] Lấy chữ cái đầu của hai từ cuối trong `display_name` [Nguồn: 09-layoutBase/Admin - Người dùng.dc.html:459]<br>[EVT liên quan] - |
| | 4 | Tên người dùng | `adminUserManagement.list.col.displayName` | `users` | `display_name` | ListColumn | String | - | - | O | - | - | Tên hiển thị của tài khoản<br>[Nguồn giá trị] Cột `display_name`<br>[EVT liên quan] - |
| | 5 | Email | `adminUserManagement.list.col.email` | `users` | `email` | ListColumn | String | - | - | O | - | - | Địa chỉ email đăng nhập<br>[Nguồn giá trị] Cột `email`<br>[EVT liên quan] - |
| | 6 | Vai trò | `adminUserManagement.list.col.role` | `roles` | `name`, `base_category` | Badge | String | - | - | O | - | Nhãn tiếng Việt | Vai trò của tài khoản, hiển thị dạng thẻ<br>[Nguồn giá trị] `roles.name` qua `users.role_id`; nhóm màu lấy theo `roles.base_category`, không theo `roles.code` — role tuỳ biến vẫn hiển thị đúng nhóm cơ bản [Nguồn: 02-bd/architecture/identity.md:74-80]<br>[EVT liên quan] - |
| | 7 | Đã giải | `adminUserManagement.list.col.solvedCount` | `user_problem_best_score` | `best_verdict` | ListColumn | Number | 6 | - | O | 0 | Số nguyên | Số bài tập đã giải được<br>[Công thức] Đếm số `problem_id` của người dùng có `best_verdict = 'ACCEPTED'` [Nguồn: 02-bd/database/identity.md:111-112]<br>[EVT liên quan] - |
| | 8 | Lượt nộp | `adminUserManagement.list.col.submissionCount` | `user_submission_stats` | `total_submissions` | ListColumn | Number | 8 | - | O | 0 | Số nguyên phân cách nghìn | Tổng số lượt nộp bài<br>[Nguồn giá trị] Cột `total_submissions` [Nguồn: 02-bd/database/identity.md:113-114]<br>[EVT liên quan] - |
| | 9 | Hoạt động | `adminUserManagement.list.col.lastActive` | `users` | `last_active_at` | ListColumn | String | - | - | O | - | Mô tả tương đối | Mốc hoạt động gần nhất, dạng "5 phút trước" / "Đang trực tuyến"<br>[Nguồn giá trị] Cột `users.last_active_at` [Nguồn: 02-bd/database/identity.md:22]<br>[Công thức] Chưa có `last_active_at` thì hiển thị `-`; ngược lại đổi sang khoảng thời gian tương đối. Ngưỡng "Đang trực tuyến" (`last_active_at` trong 5 phút gần nhất) không có nguồn nào chốt số phút — `[SoT: Suy luận]` 5 phút, chọn theo cùng bậc với chu kỳ cập nhật hãm-theo-phút của cột này [Nguồn: 02-bd/database/identity.md:22], quá đó thì hiển thị dạng tương đối thường ("5 phút trước"). Đóng Q4<br>[EVT liên quan] - |
| | 10 | Trạng thái | `adminUserManagement.list.col.status` | `users` | `status` | Badge | Enum | - | - | O | - | Nhãn tiếng Việt kèm chấm màu | Trạng thái truy cập của tài khoản. Prototype có ba nhãn "Hoạt động" / "Chờ xác thực" / "Bị khóa" nhưng `users.status` chỉ có hai giá trị<br>[Nguồn giá trị] `ACTIVE` thành "Hoạt động", `DEACTIVATED` thành "Bị khóa"; nhãn "Chờ xác thực" **chưa có nguồn**, xem Q3<br>[EVT liên quan] - |
| | 11 | Nhãn trang | `adminUserManagement.paging.label` | - | - | Label | String | - | - | O | - | `Trang {số} trong {số} · hiển thị {số} dòng` | Vị trí trang hiện tại và số dòng đang hiển thị<br>[Công thức] Số trang hiện tại và tổng số trang lấy từ phần phân trang trong phản hồi của `ListUsers`<br>[EVT liên quan] EVT-6, EVT-7 |
| | 12 | Trang trước | `adminUserManagement.paging.btnPrev` | - | - | Button | - | - | - | I | - | - | Lùi về trang liền trước của kết quả lọc<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-6 |
| | 13 | Trang sau | `adminUserManagement.paging.btnNext` | - | - | Button | - | - | - | I | - | - | Tiến tới trang liền sau của kết quả lọc<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-7 |
| | 14 | Cột Thao tác | `adminUserManagement.list.col.actions` | - | - | ListColumn | - | - | - | I | - | - | **Mới 2026-10-08 (F1-13, đóng điểm chạm Q7).** Cột cuối bảng, căn phải, chứa ba nút biểu tượng của riêng dòng đó, mỗi nút có `aria-label` nêu tên tài khoản ("Đổi vai trò của {tên}"...) [Nguồn: 05-coding/frontend/src/views/admin/user-management/ui/admin-user-management-view.tsx:286-325]<br>[EVT liên quan] EVT-8, EVT-10, EVT-12, EVT-19 |
| | 15 | Nút Đổi vai trò (dòng) | `adminUserManagement.list.col.actions.btnChangeRole` | - | - | Button | - | - | - | I | - | - | Mở popup đổi vai trò cho đúng tài khoản của dòng (Popup NO 2) [Nguồn: 05-coding/frontend/src/views/admin/user-management/ui/admin-user-management-view.tsx:294-299]<br>[EVT liên quan] EVT-10 |
| | 16 | Nút Đặt lại mật khẩu (dòng) | `adminUserManagement.list.col.actions.btnResetPassword` | - | - | Button | - | - | - | I | - | - | Mở popup xác nhận đặt lại mật khẩu cho đúng tài khoản của dòng (Popup NO 1) [Nguồn: 05-coding/frontend/src/views/admin/user-management/ui/admin-user-management-view.tsx:300-305]<br>[EVT liên quan] EVT-8 |
| | 17 | Nút Khóa tài khoản (dòng) | `adminUserManagement.list.col.actions.btnLock` | - | - | Button | - | - | - | I | - | - | Chỉ hiện ở dòng **chưa** bị khoá, kiểu nguy hiểm. Mở popup khoá với tập đích là một tài khoản (Popup NO 3) [Nguồn: 05-coding/frontend/src/views/admin/user-management/ui/admin-user-management-view.tsx:313-320]<br>[EVT liên quan] EVT-12 |
| | 18 | Nút Mở khóa tài khoản (dòng) | `adminUserManagement.list.col.actions.btnUnlock` | - | - | Button | - | - | - | I | - | - | Chỉ hiện ở dòng **đang** bị khoá, thay chỗ nút Khoá. Mở popup mở khoá với tập đích là một tài khoản (Popup NO 3f) [Nguồn: 05-coding/frontend/src/views/admin/user-management/ui/admin-user-management-view.tsx:306-312]<br>[EVT liên quan] EVT-19 |

### Khu vực E — Phân bố theo vai trò

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Phân bố theo vai trò | | | | | | | | | | | | | |
| | 1 | Tiêu đề khối | `adminUserManagement.roleDist.title` | - | - | Label | String | - | - | O | Phân bố theo vai trò | - | Tên khối thống kê<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] - |
| | 2 | Chú thích tổng | `adminUserManagement.roleDist.subtitle` | `users` | `status` | Label | String | - | - | O | - | `Tổng {số} tài khoản đang hoạt động` | Tổng số tài khoản đang hoạt động<br>[Công thức] Đếm `users` có `status = 'ACTIVE'`<br>[EVT liên quan] EVT-1 |
| | 3 | Danh sách nhóm | `adminUserManagement.roleDist.list` | `users` | `role_id`, `status` | List | List | - | - | O | 4 dòng | - | Đúng 4 dòng: Người học, Giảng viên, Quản trị, Đã vô hiệu<br>[Nguồn giá trị] Kết quả gọi `GetRoleDistribution`<br>[EVT liên quan] EVT-1 |
| | 4 | Tên nhóm | `adminUserManagement.roleDist.col.groupName` | `roles` | `base_category` | ListColumn | String | - | - | O | - | Nhãn tiếng Việt | Tên nhóm hiển thị<br>[Nguồn giá trị] Ba nhóm đầu là nhãn tĩnh i18n map từ `base_category`; nhóm "Đã vô hiệu" là nhãn tĩnh i18n map từ `users.status = 'DEACTIVATED'`<br>[EVT liên quan] - |
| | 5 | Số lượng và tỉ lệ | `adminUserManagement.roleDist.col.value` | `users` | `role_id`, `status` | ListColumn | String | - | - | O | - | `{số} · {số}%` | Số tài khoản và tỉ lệ phần trăm của nhóm<br>[Công thức] Tài khoản có `status = 'DEACTIVATED'` đếm vào nhóm "Đã vô hiệu" và **không** đếm vào nhóm `base_category` của nó, để bốn nhóm loại trừ nhau và tổng đúng 100% `[Suy luận]` — prototype trình bày bốn nhóm ngang hàng nhưng `base_category` và `status` là hai chiều độc lập trong schema<br>[EVT liên quan] - |
| | 6 | Thanh tỉ lệ | `adminUserManagement.roleDist.col.bar` | - | - | ProgressBar | Number | - | - | O | - | - | Biểu diễn trực quan tỉ lệ nhóm<br>[Công thức] Chiều rộng bằng đúng tỉ lệ phần trăm của dòng<br>[EVT liên quan] - |

### Khu vực F — Cần xử lý

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Cần xử lý | | | | | | | | | | | | | |
| | 1 | Tiêu đề khối | `adminUserManagement.todo.title` | - | - | Label | String | - | - | O | Cần xử lý | - | Tên khối việc cần quản trị viên xử lý<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] - |
| | 2 | Nhật ký | `adminUserManagement.todo.linkAuditLog` | - | - | Link | - | - | - | I | - | - | Điều hướng sang màn `admin_system_log`<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-16 |
| | 3 | Danh sách việc | `adminUserManagement.todo.list` | - | - | List | List | - | - | O | 1 dòng | - | Còn đúng 1 dòng "Chờ xác thực email" sau khi cắt "Báo cáo nghi gian lận" và "Đề nghị cấp quyền giảng viên" khỏi phạm vi. **2026-10-08:** dữ liệu mẫu và i18n của prototype đã gỡ nốt dòng "Đề nghị cấp quyền giảng viên" [Nguồn: 05-coding/frontend/src/views/admin/user-management/api/__mock__/admin-user-mocks.ts:32-34]<br>[Nguồn giá trị] Dùng lại phản hồi của `GetUserManagementStats`, không gọi riêng<br>[EVT liên quan] EVT-1 |
| | 4 | Tên việc | `adminUserManagement.todo.col.title` | - | - | ListColumn | String | - | - | O | - | - | Tên việc cần xử lý<br>[Nguồn giá trị] Nhãn tĩnh i18n map từ mã việc<br>[EVT liên quan] - |
| | 5 | Chú thích việc | `adminUserManagement.todo.col.meta` | - | - | ListColumn | String | - | - | O | - | - | Câu giải thích cách xử lý, ví dụ "Quá hạn 7 ngày sẽ tự huỷ"<br>[Nguồn giá trị] Nhãn tĩnh i18n map từ mã việc<br>[EVT liên quan] - |
| | 6 | Số lượng | `adminUserManagement.todo.col.count` | - | - | ListColumn | Number | 6 | - | O | 0 | Số nguyên | Số bản ghi đang chờ xử lý<br>[Nguồn giá trị] Lấy từ `pendingVerificationCount` của `GetUserManagementStats` (thẻ chỉ số cùng tên đã bỏ 2026-10-01) — **chưa có nguồn**, xem Q3<br>[EVT liên quan] - |

### Popup

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Popup | | | | | | | | | | | | | |
| | 1 | Xác nhận đặt lại mật khẩu | `adminUserManagement.popup.resetPasswordConfirm` | - | - | Popup | - | - | - | I | - | Đặt lại / Huỷ | **Sửa 2026-10-08:** chỉ cho **một** tài khoản (không còn gộp, Q7), mở từ nút trên dòng. Tiêu đề nêu tên tài khoản, nội dung nêu email nhận mật khẩu tạm; người dùng phải đổi ở lần đăng nhập kế tiếp. Mật khẩu **không hiển thị cho quản trị viên** [Nguồn: 05-coding/frontend/src/views/admin/user-management/ui/admin-user-management-view.tsx:481-490]<br>[Nguồn giá trị] Tài khoản của dòng được bấm<br>[EVT liên quan] EVT-8, EVT-9, EVT-14 |
| | 2 | Đổi vai trò | `adminUserManagement.popup.changeRole` | `roles` | `id`, `name` | Popup | - | - | Có | I | - | - | **Sửa 2026-10-08:** chỉ cho **một** tài khoản, mở từ nút trên dòng. Chọn một vai trò đích rồi xác nhận. Prototype dùng ba giá trị cố định (Người học / Giảng viên / Quản trị) [Nguồn: 05-coding/frontend/src/views/admin/user-management/ui/change-role-dialog.tsx:59-70]; bản thật đọc `ListRoles` gồm cả role tuỳ biến `[SoT: Suy luận]`, DD chốt<br>[Nguồn giá trị] Kết quả gọi `ListRoles`<br>[EVT liên quan] EVT-10, EVT-11, EVT-14 |
| | 2a | Ô chọn vai trò mới | `adminUserManagement.popup.changeRole.role` | `roles` | `id`, `name` | ComboBox | Enum | - | Có | I | Vai trò hiện tại của tài khoản | - | **Mới 2026-10-08.** Khởi tạo bằng vai trò hiện tại; nút xác nhận chỉ kích hoạt khi chọn vai trò **khác** [Nguồn: 05-coding/frontend/src/views/admin/user-management/ui/change-role-dialog.tsx:34-35,47]. Kèm ghi chú "Thay đổi có hiệu lực ngay và được ghi vào Nhật ký hệ thống"<br>[EVT liên quan] EVT-11 |
| | 2b | Cảnh báo tự hạ vai trò | `adminUserManagement.popup.changeRole.selfWarning` | - | - | Label | String | - | - | Điều kiện | - | - | **Mới 2026-10-08.** Hiện khi quản trị viên đang sửa chính dòng của mình và chọn vai trò khác Quản trị: "Bạn đang hạ vai trò của chính mình: bạn sẽ mất quyền vào khu quản trị cho tới khi quản trị viên khác cấp lại." Chỉ cảnh báo, vẫn cho làm (Sheet 9 NO 7) [Nguồn: 05-coding/frontend/src/views/admin/user-management/ui/change-role-dialog.tsx:72-76]<br>[EVT liên quan] EVT-11 |
| | 3 | Xác nhận khoá tài khoản | `adminUserManagement.popup.lockConfirm` | - | - | Popup | - | - | - | I | - | Khóa tài khoản / Huỷ | Xác nhận trước khi đặt `status = 'DEACTIVATED'`. **Dựng lại thành popup nhập liệu có hai ô mới theo F1-32 (2026-10-05):** lý do và tuỳ chọn gửi email — vì vậy nút xác nhận mang nhãn "Khóa tài khoản" chứ không phải "Xác nhận" chung. Nếu khoá làm hệ thống không còn ADMIN hoạt động nào thì không mở popup mà hiện toast chặn cứng [Nguồn: 05-coding/frontend/src/views/admin/user-management/ui/admin-user-management-view.tsx:127-131] (Sheet 9 NO 8); nếu tập chọn có chính tài khoản đang đăng nhập thì thêm câu cảnh báo (Sheet 9 NO 6). **2026-10-08:** popup này cũng được nút "Khóa tài khoản" trên dòng mở, khi đó tập đích chỉ có đúng dòng đó<br>[Nguồn giá trị] Danh sách tài khoản đang chọn hoặc dòng được bấm<br>[EVT liên quan] EVT-12, EVT-13, EVT-14 |
| | 3a | Danh sách tài khoản bị khoá | `adminUserManagement.popup.lockTargets` | `users` | `display_name`, `email` | List | List | - | - | O | - | - | **Mới 2026-10-05.** Liệt kê tên và email của các tài khoản thực sự bị khoá, tối đa 8 dòng rồi gộp phần còn lại thành "và {n} tài khoản khác". Bảng được sắp theo hoạt động gần nhất nên một con số "9 tài khoản" không giúp quản trị viên đối chiếu với khuôn mặt nào — phải nêu tên [Nguồn: 01-rd/req/identity.md — F1-32]<br>[EVT liên quan] EVT-12 |
| | 3b | Cảnh báo bỏ qua | `adminUserManagement.popup.lockSkipped` | `users` | `status` | Label | Number | - | - | Điều kiện | - | - | **Mới 2026-10-05.** "N tài khoản đã bị khoá từ trước nên bị bỏ qua" — chỉ hiện khi tập chọn có dòng đã khoá (Sheet 9 NO 9)<br>[Công thức] Đếm dòng `status = 'DEACTIVATED'` trong tập đang chọn<br>[EVT liên quan] EVT-12 |
| | 3c | Lý do khoá | `adminUserManagement.popup.lockReason` | `system_audit_logs` | `detail` | TextArea | String | 500 | Điều kiện | I | rỗng | Văn bản tự do | **Mới 2026-10-05 (F1-32).** Bắt buộc khi bật gửi email — email nói "tài khoản bị khoá" mà không nói vì sao thì người dùng không biết phải làm gì. Vẫn được ghi vào nhật ký kể cả khi tắt gửi email. Độ dài 500 là `[Suy luận]`, DD chốt<br>[Ghi chú] Không tự động điền, không có danh sách lý do cố định: lý do là quyết định của quản trị viên trong từng sự cố<br>[Nguồn giá trị] Người dùng nhập<br>[EVT liên quan] EVT-12 |
| | 3d | Gửi email thông báo | `adminUserManagement.popup.lockNotify` | - | - | Toggle | Boolean | - | - | I | Bật | - | **Mới 2026-10-05 (F1-32).** Mặc định **bật** — quản trị viên phải chủ động tắt mới im lặng. Khi bật mà lý do rỗng thì chặn gửi (Sheet 9 NO 16)<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-12 |
| | 3e | Ghi chú về lý do | `adminUserManagement.popup.lockReasonHint` | - | - | Label | String | - | - | O | - | - | **Mới 2026-10-05.** Nói rõ lý do luôn được ghi vào Nhật ký hệ thống kể cả khi không gửi email, và email nêu tài khoản / thời điểm / lý do / cách liên hệ để mở khoá<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] - |
| | 3f | Xác nhận mở khoá | `adminUserManagement.popup.unlockConfirm` | - | - | Popup | - | - | - | I | - | Mở khoá / Huỷ | **Mới 2026-10-05.** Cũng liệt kê tên tài khoản như NO 3a. **Không có ô lý do và không gửi email** — mở khoá là hành động khôi phục quyền truy cập, chỉ ghi nhật ký [Nguồn: 01-rd/req/identity.md — F1-32]<br>[EVT liên quan] EVT-19, EVT-20 |
| | 4 | Popup Thêm tài khoản | `adminUserManagement.popup.addAccount` | `users` | - | Popup | - | - | - | I/O | - | Tạo tài khoản / Huỷ | Popup nhập liệu tạo tài khoản thủ công (F1-13 mở rộng). Mỗi lần mở khởi tạo lại; đóng thì bỏ nháp<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-15, EVT-17, EVT-18 |
| | 5 | Họ và tên | `adminUserManagement.popup.addAccount.name` | `users` | `display_name` | TextBox | String | 100 | Có | I | rỗng | Văn bản tự do, cắt khoảng trắng hai đầu | Tên hiển thị của tài khoản mới. Độ dài 100 theo `users.display_name` `[SoT: Suy luận]` (prototype không giới hạn)<br>[Nguồn giá trị] Người dùng nhập<br>[EVT liên quan] EVT-17 |
| | 6 | Email | `adminUserManagement.popup.addAccount.email` | `users` | `email` | TextBox | String | 255 | Có | I | rỗng | Địa chỉ email; hệ thống cắt khoảng trắng và đổi sang chữ thường | Email đăng nhập và nơi nhận mật khẩu tạm. Không được trùng với email tài khoản đang có<br>[Nguồn giá trị] Người dùng nhập<br>[EVT liên quan] EVT-17 |
| | 7 | Vai trò | `adminUserManagement.popup.addAccount.role` | `roles` | `id`, `name` | ComboBox | Enum | - | Có | I | Học viên (`STUDENT`) | Học viên / Giảng viên | Chỉ hai lựa chọn `STUDENT` và `INSTRUCTOR`; **không** cho tạo `ADMIN` ở popup này<br>[Nguồn giá trị] Hai giá trị cố định của popup (chưa dùng `ListRoles`), xem Q1<br>[EVT liên quan] EVT-17 |
| | 8 | Ghi chú mật khẩu tạm | `adminUserManagement.popup.addAccount.note` | - | - | Label | String | - | - | O | Mật khẩu tạm được gửi qua email; người dùng phải đổi ở lần đăng nhập đầu tiên. | - | Nhắc rằng quản trị viên không đặt hay xem mật khẩu<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] - |
| | 9 | Tạo tài khoản | `adminUserManagement.popup.addAccount.btnSubmit` | - | - | Button | - | - | - | I | - | - | Kiểm ba ô rồi tạo tài khoản<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-17 |
| | 10 | Huỷ hoặc Xong | `adminUserManagement.popup.addAccount.btnClose` | - | - | Button | - | - | - | I | - | - | Nhãn "Huỷ" trước khi tạo, đổi thành "Xong" sau khi tạo xong. Đóng popup, xoá nháp<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-18 |
| | 11 | Thông báo đã tạo | `adminUserManagement.popup.addAccount.createdNotice` | - | - | Label | String | - | - | O | - | `Đã tạo tài khoản` + `Mật khẩu tạm đã được gửi tới {email}. Tài khoản ở trạng thái chờ xác thực cho tới lần đăng nhập đầu tiên.` | Nội dung hiện bằng toast sau khi tạo, popup đóng ngay<br>[Nguồn giá trị] Phản hồi của `CreateUserAccount`; email lấy từ giá trị đã chuẩn hoá<br>[EVT liên quan] EVT-17 |

[Nguồn: 09-layoutBase/Admin - Người dùng.dc.html:148-159,161-172,176-192,194-203,205-228,230-236,240-256,258-274,422-428,459,476-481,483-486; Khu vực D NO 14-18 và Popup NO 1-2b (2026-10-08): 05-coding/frontend/src/views/admin/user-management/ui/admin-user-management-view.tsx:286-325,472-492; 02-bd/database/identity.md:8-24,31-43,111-114,132; 01-rd/screens/admin/ADM0201_user_management.md:36-49; Popup NO 3 (bổ sung chặn cứng, cảnh báo) và NO 4-11: 05-coding/frontend/src/views/admin/user-management/ui/add-account-dialog.tsx:22-115, 05-coding/frontend/src/views/admin/user-management/model/guards.ts:14-26, 05-coding/frontend/messages/vi.json:545-562]

---

## Sheet 6. Đặc tả điều khiển item

> Ký hiệu cột `Hiển thị` và thứ tự thẻ: `02-bd/_rules/bd-template-9sheet.md` mục 2, 3. Dùng đúng NO, tên
> item và thứ tự của Sheet 5. Lỗi nhập liệu ghi ở Sheet 9, xử lý nghiệp vụ ghi ở Sheet 8.

### Khu vực A — Thanh tiêu đề

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Thanh tiêu đề | | | | |
| | 1 | Tiêu đề màn | Có | - |
| | 2 | Mô tả phụ | Có | - |
| | 3 | Thêm tài khoản | Điều kiện | [Điều kiện hiển thị] Phạm vi đã chốt 2026-10-01 (Q1) nên nút thuộc thiết kế; RD F1-13 đã mở rộng (`01-rd/req/identity.md:69`) và prototype đã dựng popup đích, nên không còn là nút trống. Khi làm UI thật, nút vẫn chỉ nên hoạt động khi đã có endpoint `CreateUserAccount`.<br>[Điều kiện kích hoạt] Kích hoạt khi có quyền `USER_MANAGEMENT:CREATE`. |

### Khu vực B — Dải chỉ số tổng (đã bỏ)

Không còn item nào — xem Sheet 5, Khu vực B.

### Khu vực C — Bộ lọc và hành động gộp

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Bộ lọc và hành động gộp | | | | |
| | 1 | Ô tìm kiếm | Có | [Điều kiện kích hoạt] Không kích hoạt trong lúc đang tải trang đầu, kích hoạt sau khi tải xong.<br>[Tự động đặt] Gõ xong thì chờ một khoảng ngắn mới gọi máy chủ; mỗi lần gọi đưa về trang 1. |
| | 2 | Tab vai trò | Có | [Tự động đặt] Đổi tab thì đưa về trang 1 và bỏ toàn bộ lựa chọn đang có. |
| | 3 | Tab trạng thái | Có | [Tự động đặt] Đổi tab thì đưa về trang 1 và bỏ toàn bộ lựa chọn đang có. Có lựa chọn "Chờ xác thực" từ 2026-10-08. |
| | 4 | Số kết quả | Có | [Tự động đặt] Cập nhật lại sau mỗi lần tải danh sách. |
| | 5 | Nhãn số đã chọn | Điều kiện | [Điều kiện hiển thị] Cả thanh hành động gộp chỉ hiển thị khi có ít nhất một dòng được chọn [Nguồn: 09-layoutBase/Admin - Người dùng.dc.html:194]. |
| | 6 | Đặt lại mật khẩu | Không còn | Đã bỏ khỏi màn 2026-10-05 (Q7) |
| | 7 | Đổi vai trò | Không còn | Đã bỏ khỏi màn 2026-10-05 (Q7) |
| | 8 | Khóa tài khoản | Điều kiện | [Điều kiện hiển thị] Theo thanh hành động gộp.<br>[Điều kiện kích hoạt] Kích hoạt khi có quyền `USER_MANAGEMENT:UPDATE` **và** tập đang chọn có ít nhất một tài khoản chưa bị khoá. Nút **hiện nhưng không kích hoạt** chứ không ẩn: tập chọn có thể trộn cả hai loại dòng, ẩn nút khiến thanh nhảy lên mỗi lần tích — và nút "Mở khoá" luôn cần hiện khi đang chọn dòng đã khoá |
| | 9 | Mở khoá tài khoản | Điều kiện | [Điều kiện hiển thị] Theo thanh hành động gộp.<br>[Điều kiện kích hoạt] Kích hoạt khi có quyền `USER_MANAGEMENT:UPDATE` **và** tập đang chọn có ít nhất một tài khoản `status = 'DEACTIVATED'`. Đối xứng với NO 8 |

### Khu vực D — Bảng người dùng

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Bảng người dùng | | | | |
| | 1 | Danh sách người dùng | Có | [Điều kiện hiển thị] Trong lúc tải hiển thị khung chờ đúng số dòng của một trang. Tải xong mà không có dòng nào thì hiển thị thông báo rỗng thay cho bảng — đây là kết quả lọc rỗng, **không phải** lỗi. |
| | 2 | Ô chọn dòng | Có | [Điều kiện kích hoạt] Không kích hoạt trong lúc một hành động gộp đang chạy.<br>[Tự động xoá] Bỏ toàn bộ lựa chọn khi đổi trang, đổi bộ lọc, hoặc sau khi một hành động gộp kết thúc. |
| | 3 | Chữ viết tắt | Có | - |
| | 4 | Tên người dùng | Có | - |
| | 5 | Email | Có | - |
| | 6 | Vai trò | Có | - |
| | 7 | Đã giải | Có | - |
| | 8 | Lượt nộp | Có | - |
| | 9 | Hoạt động | Có | - |
| | 10 | Trạng thái | Có | [Điều kiện hiển thị] Nhãn "Chờ xác thực" chỉ hiển thị khi Q3 đã chốt nguồn dữ liệu; chưa chốt thì chỉ có hai nhãn "Hoạt động" và "Bị khóa". |
| | 11 | Nhãn trang | Có | [Tự động đặt] Cập nhật lại sau mỗi lần tải danh sách. |
| | 12 | Trang trước | Có | [Điều kiện kích hoạt] Không kích hoạt khi đang ở trang 1 hoặc đang tải. |
| | 13 | Trang sau | Có | [Điều kiện kích hoạt] Không kích hoạt khi đang ở trang cuối hoặc đang tải. |
| | 14 | Cột Thao tác | Có | [Điều kiện hiển thị] Luôn hiện cột; từng nút bên trong theo NO 15-18. |
| | 15 | Nút Đổi vai trò (dòng) | Điều kiện | [Điều kiện kích hoạt] Kích hoạt khi có quyền `USER_MANAGEMENT:UPDATE`. Kích hoạt cả với dòng của chính quản trị viên (cho tự hạ, chỉ cảnh báo, Sheet 9 NO 7) và cả dòng đang bị khoá. |
| | 16 | Nút Đặt lại mật khẩu (dòng) | Điều kiện | [Điều kiện kích hoạt] Kích hoạt khi có quyền `USER_MANAGEMENT:UPDATE`. **Khoảng trống:** prototype không có cờ cho biết tài khoản chỉ đăng nhập OAuth nên nút kích hoạt với mọi dòng (Q8, Sheet 9 NO 10). |
| | 17 | Nút Khóa tài khoản (dòng) | Điều kiện | [Điều kiện hiển thị] Chỉ ở dòng chưa bị khoá.<br>[Điều kiện kích hoạt] Kích hoạt khi có quyền `USER_MANAGEMENT:UPDATE`. |
| | 18 | Nút Mở khóa tài khoản (dòng) | Điều kiện | [Điều kiện hiển thị] Chỉ ở dòng đang bị khoá (thay chỗ NO 17).<br>[Điều kiện kích hoạt] Kích hoạt khi có quyền `USER_MANAGEMENT:UPDATE`. |

### Khu vực E — Phân bố theo vai trò

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Phân bố theo vai trò | | | | |
| | 1 | Tiêu đề khối | Có | - |
| | 2 | Chú thích tổng | Có | - |
| | 3 | Danh sách nhóm | Có | [Điều kiện hiển thị] Trong lúc tải hiển thị khung chờ đúng 4 dòng. |
| | 4 | Tên nhóm | Có | - |
| | 5 | Số lượng và tỉ lệ | Có | [Tự động đặt] Tính lại sau mỗi lần tải khối; **không** tính lại theo bộ lọc của bảng — khối này luôn thống kê toàn hệ thống. |
| | 6 | Thanh tỉ lệ | Có | [Tự động đặt] Chiều rộng cập nhật cùng lúc với tỉ lệ của dòng. |

### Khu vực F — Cần xử lý

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Cần xử lý | | | | |
| | 1 | Tiêu đề khối | Có | - |
| | 2 | Nhật ký | Có | [Điều kiện kích hoạt] Kích hoạt khi có quyền `SYSTEM_AUDIT_LOG:READ`; không có quyền thì không hiển thị liên kết. |
| | 3 | Danh sách việc | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị khi còn ít nhất một dòng việc. Q3 chưa chốt thì khối này rỗng và ẩn cả khối, vì dòng duy nhất còn lại phụ thuộc nguồn dữ liệu đó. |
| | 4 | Tên việc | Có | - |
| | 5 | Chú thích việc | Có | - |
| | 6 | Số lượng | Có | - |

### Popup

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Popup | | | | |
| | 1 | Xác nhận đặt lại mật khẩu | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị sau khi bấm nút "Đặt lại mật khẩu" ở cột "Thao tác" của một dòng (Sheet 6 Khu vực D NO 16). |
| | 2 | Đổi vai trò | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị sau khi bấm nút "Đổi vai trò" ở cột "Thao tác" của một dòng.<br>[Điều kiện kích hoạt] Nút xác nhận trong popup chỉ kích hoạt khi vai trò chọn **khác** vai trò hiện tại của tài khoản. |
| | 2a | Ô chọn vai trò mới | Có | [Tự động đặt] Khởi tạo bằng vai trò hiện tại mỗi lần mở popup cho một tài khoản. |
| | 2b | Cảnh báo tự hạ vai trò | Điều kiện | [Điều kiện hiển thị] Chỉ khi dòng đang sửa là chính tài khoản đăng nhập **và** vai trò chọn khác Quản trị. |
| | 3 | Xác nhận khoá tài khoản | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị sau khi bấm "Khóa tài khoản" trên thanh hành động gộp hoặc nút khoá trên dòng. Tập đích mà khoá xong sẽ không còn ADMIN hoạt động nào thì không mở hộp xác nhận mà hiện toast chặn cứng [Nguồn: 05-coding/frontend/src/views/admin/user-management/ui/admin-user-management-view.tsx:127-131]. Tập chọn có chính tài khoản đang đăng nhập thì hộp xác nhận thêm một dòng cảnh báo nổi bật. |
| | 3a | Danh sách tài khoản bị khoá | Có | [Điều kiện hiển thị] Chỉ hiển thị khi có ít nhất một dòng bị khoá thật sự — tập chọn toàn dòng đã khoá thì nút "Khoá" không kích hoạt nên popup không mở. [Tự động đặt] Vượt 8 dòng thì cuộn, phần còn lại gộp thành một dòng đếm |
| | 3b | Cảnh báo bỏ qua | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị khi tập chọn có dòng đã bị khoá từ trước |
| | 3c | Lý do khoá | Có | [Tự động đặt] Xoá khi đóng popup. [Điều kiện kích hoạt] Viền đỏ (nếu có) chỉ hiện sau lần bấm "Khóa tài khoản" đầu tiên, không hiện khi mới gõ — cùng nguyên tắc NO 5 |
| | 3d | Gửi email thông báo | Có | [Tự động đặt] Mặc định **bật**; trả về mặc định khi mở lại popup |
| | 3e | Ghi chú về lý do | Có | - |
| | 3f | Xác nhận mở khoá | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị sau khi bấm "Mở khoá tài khoản" trên thanh hành động gộp hoặc nút mở khoá trên dòng |
| | 4 | Popup Thêm tài khoản | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị sau khi bấm "Thêm tài khoản". |
| | 5 | Họ và tên | Có | [Tự động đặt] Viền đỏ (nếu có) chỉ hiện sau lần bấm "Tạo tài khoản" đầu tiên, không hiện khi mới gõ. |
| | 6 | Email | Có | [Tự động đặt] Như NO 5. |
| | 7 | Vai trò | Có | [Tự động đặt] Chọn sẵn "Học viên" mỗi lần mở popup. |
| | 8 | Ghi chú mật khẩu tạm | Có | - |
| | 9 | Tạo tài khoản | Điều kiện | [Điều kiện hiển thị] Ẩn sau khi tạo xong.<br>[Điều kiện kích hoạt] Luôn kích hoạt; bấm khi còn ô không hợp lệ thì ô đổi viền đỏ, hiện một toast lỗi và không tạo. |
| | 10 | Huỷ hoặc Xong | Có | [Tự động đặt] Nhãn "Huỷ"; popup đóng ngay khi tạo thành công nên không có nhãn "Xong" [Nguồn: 05-coding/frontend/src/views/admin/user-management/ui/add-account-dialog.tsx:62-64]. |
| | 11 | Thông báo đã tạo | Điều kiện | [Điều kiện hiển thị] Không hiển thị trong popup: nội dung đi qua toast, popup đóng ngay khi tạo xong. |

[Nguồn: 09-layoutBase/Admin - Người dùng.dc.html:194,230-236; 02-bd/database/identity.md:21,47-49; Popup NO 4-11: 05-coding/frontend/src/views/admin/user-management/ui/add-account-dialog.tsx:32-112]

---

## Sheet 7. Danh sách truyền dữ liệu

### 7.1 Trường DTO

| NO | DTO | Trường DTO | Kiểu | Bảng DB | Cột DB | Item màn | Hiển thị | Ghi chú |
| --: | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :--- |
| 1 | `AdminUserListItemDto` | `id` | UUID | `users` | `id` | - | Không | [Nguồn] Phản hồi của `ListUsers`<br>[Đích] Tham số của `ChangeUserRole`, `ChangeUserStatus`, `ResetUserPassword`. Không hiển thị trên màn. |
| 2 | `AdminUserListItemDto` | `displayName` | String | `users` | `display_name` | Bảng "Tên người dùng", "Chữ viết tắt" | Có | [Chuyển đổi] Chữ viết tắt sinh phía màn hình từ chính trường này, không có trường riêng trong DTO. |
| 3 | `AdminUserListItemDto` | `email` | String | `users` | `email` | Bảng "Email" | Có | [Nguồn] Phản hồi của `ListUsers` |
| 4 | `AdminUserListItemDto` | `roleId`, `roleName`, `baseCategory` | UUID, String, Enum | `roles` | `id`, `name`, `base_category` | Bảng "Vai trò" | Có | [Chuyển đổi] `roleName` hiển thị trên thẻ; `baseCategory` quyết định nhóm màu và là khoá lọc của tab vai trò. |
| 5 | `AdminUserListItemDto` | `status` | Enum | `users` | `status` | Bảng "Trạng thái" | Có | [Chuyển đổi] `ACTIVE` thành "Hoạt động", `DEACTIVATED` thành "Bị khóa". Nhãn thứ ba "Chờ xác thực" chưa có nguồn, xem Q3. |
| 6 | `AdminUserListItemDto` | `solvedCount` | Number | `user_problem_best_score` | `best_verdict` | Bảng "Đã giải" | Có | [Nguồn] Read model, tổng hợp từ domain event [Nguồn: 02-bd/database/identity.md:109-112]<br>[Chuyển đổi] Đếm số bài có `best_verdict = 'ACCEPTED'`. |
| 7 | `AdminUserListItemDto` | `totalSubmissions` | Number | `user_submission_stats` | `total_submissions` | Bảng "Lượt nộp" | Có | [Nguồn] Read model [Nguồn: 02-bd/database/identity.md:113-114] |
| 8 | `UserManagementStatsDto` | `pendingVerificationCount` | Number | - | - | Khối "Cần xử lý" | Điều kiện | [Nguồn] **Chưa chốt** — schema `identity` chưa có chỗ lưu trạng thái xác thực email, xem Q3. Thẻ "Chờ xác thực email" đã bỏ 2026-10-01 nên trường này chỉ còn phục vụ khối "Cần xử lý". |
| 9 | `RoleDistributionItemDto` | `groupCode`, `count`, `percent` | String, Number, Number | `users`, `roles` | `role_id`, `status`, `base_category` | Khối "Phân bố theo vai trò" | Có | [Nguồn] Phản hồi của `GetRoleDistribution`<br>[Chuyển đổi] `groupCode` là khoá tra nhãn tĩnh i18n; bốn nhóm loại trừ nhau, tài khoản `DEACTIVATED` chỉ đếm vào nhóm "Đã vô hiệu". |
| 10 | `RoleOptionDto` | `id`, `name`, `baseCategory` | UUID, String, Enum | `roles` | `id`, `name`, `base_category` | Popup "Đổi vai trò" | Có | [Nguồn] Phản hồi của `ListRoles` (prototype 2026-10-08 dùng ba giá trị cố định, chưa gọi)<br>[Đích] `id` là tham số `targetRoleId` của `ChangeUserRole`. |
| 11 | `BulkUserActionResultDto` | `succeededIds`, `failedItems` | List, List | - | - | Thông báo hoàn tất của EVT-9, EVT-11, EVT-13 | Có | [Nguồn] Phản hồi của ba endpoint hành động<br>[Chuyển đổi] Một hành động gộp phía màn là **N lệnh đơn** phía tầng ứng dụng, nên kết quả có thể thành công một phần; `failedItems` mang `id` kèm mã lỗi của từng tài khoản hỏng. |
| 12 | `CreateUserAccountRequestDto` | `displayName`, `email`, `roleCode` | String, String, Enum | `users`, `roles` | `display_name`, `email`, `role_id` | Popup Thêm tài khoản NO 5, 6, 7 | Không | [Nguồn] Giá trị người dùng nhập trong popup<br>[Đích] Tham số của `CreateUserAccount`<br>[Chuyển đổi] `email` được cắt khoảng trắng và đổi sang chữ thường trước khi gửi và so trùng; `roleCode` chỉ nhận `STUDENT` hoặc `INSTRUCTOR`. Mật khẩu tạm do máy chủ sinh và gửi qua email, **không** có trường mật khẩu trong DTO và không hiển thị cho quản trị viên. Tài khoản tạo ra có trạng thái chờ xác thực, nơi lưu trạng thái này là Q3. |
| 13 | `ChangeUserStatusRequestDto` | `targetStatus`, `reason`, `notifyByEmail`, `acknowledgeSelfImpact` | Enum, String, Boolean, Boolean | `system_audit_logs` | `detail` | Popup khoá NO 3c, 3d | Không | [Nguồn] **Mới 2026-10-05 (F1-32)**<br>[Đích] Tham số của `ChangeUserStatus`<br>[Chuyển đổi] `targetStatus` chỉ nhận `DEACTIVATED` hoặc `ACTIVE`. `reason` bắt buộc khi `notifyByEmail = true` (Sheet 9 NO 16), vẫn được ghi vào nhật ký kể cả khi không gửi email. `notifyByEmail` mặc định `true`; `acknowledgeSelfImpact` phải `true` khi tập đích có chính tài khoản đang thao tác (Sheet 9 NO 6, 7) — trường này đã có từ V0.6, ở đây chỉ nhắc lại vì nó nằm chung DTO |

### 7.2 Truy cập bảng dữ liệu (5)

| NO | Tên logic | Bảng | Repository | CRUD | Mục đích | Ghi chú |
| --: | :--- | :--- | :--- | :-: | :--- | :--- |
| 1 | Tài khoản người dùng | `users` | `UserRepository` | C, R, U | Tìm kiếm, lọc, phân trang; tạo tài khoản mới; đổi `role_id`, đổi `status`, đặt `password_hash` mới | `CreateUserAccount`: C<br>`ListUsers`: R<br>`GetUserManagementStats`: R<br>`ChangeUserRole`: R, U<br>`ChangeUserStatus`: R, U<br>`ResetUserPassword`: R, U |
| 2 | Vai trò | `roles` | `RoleRepository` | R | Đọc tên và `base_category` để hiển thị thẻ vai trò; nạp danh sách vai trò đích cho popup | `ListUsers`: R<br>`ListRoles`: R<br>`GetRoleDistribution`: R |
| 3 | Nhật ký quản trị | `system_audit_logs` | `SystemAuditLogRepository` | C | Ghi một dòng cho **mỗi** tài khoản chịu tác động của mỗi hành động ghi; dòng khoá kèm **lý do** trong `detail` (F1-32) | `CreateUserAccount`: C<br>`ChangeUserRole`: C<br>`ChangeUserStatus`: C<br>`ResetUserPassword`: C |
| 4 | Điểm tốt nhất theo bài | `user_problem_best_score` | `UserProblemBestScoreRepository` | R | Đếm số bài đã giải cho cột "Đã giải" | `ListUsers`: R |
| 5 | Thống kê lượt nộp | `user_submission_stats` | `UserSubmissionStatsRepository` | R | Đọc tổng lượt nộp cho cột "Lượt nộp" | `ListUsers`: R |

Thao tác `C` trên `users` là của `CreateUserAccount` (thêm V0.7). Không có thao tác xoá (`D`) trên màn này: khoá tài khoản là đặt `users.status = 'DEACTIVATED'`, không xoá
dòng [Nguồn: 02-bd/database/identity.md:21]. Chỉ số "Yêu cầu đặt lại mật khẩu" (nay hiển thị ở trang tổng quan Admin) đọc trên Redis, không qua
repository JPA.

`[Suy luận]` — tên repository do BD này đề xuất, DD module `identity` chốt lại.

### 7.3 Danh sách endpoint [Nội bộ]

> Chỉ tên nghiệp vụ và BC sở hữu. Hợp đồng chi tiết thuộc `03-dd/api/identity.md`.

| NO | Endpoint (tên nghiệp vụ) | Mục đích | BC sở hữu |
| --: | :--- | :--- | :--- |
| 1 | `ListUsers` | Tìm kiếm, lọc và phân trang danh sách tài khoản | `identity` |
| 2 | `GetUserManagementStats` | Tải số liệu cho khối "Cần xử lý" (hiện chỉ số tài khoản chờ xác thực email) | `identity` |
| 3 | `GetRoleDistribution` | Tải số liệu phân bố theo vai trò | `identity` |
| 4 | `ListRoles` | Tải danh sách vai trò đích cho popup đổi vai trò của **một** tài khoản. Màn này gọi lại từ 2026-10-08 (popup quay lại ở dạng một tài khoản, mở từ nút trên dòng); từng bị gạch 2026-10-05 khi popup chỉ còn ở chế độ gộp | `identity` |
| 5 | `ChangeUserRole` | Đổi vai trò của **một** tài khoản — không còn là hành động gộp trên màn này (Q7); điểm chạm là nút trên dòng (dựng 2026-10-08), DD chốt hợp đồng | `identity` |
| 6 | `ChangeUserStatus` | Khoá hoặc mở khoá một hoặc nhiều tài khoản (thanh gộp hoặc nút trên dòng); nhận thêm **lý do** và cờ **gửi email** (F1-32) | `identity` |
| 7 | `ResetUserPassword` | Đặt lại mật khẩu hộ cho **một** tài khoản — không còn là hành động gộp (Q7); điểm chạm là nút trên dòng (dựng 2026-10-08), DD chốt hợp đồng | `identity` |
| 8 | `CreateUserAccount` | Tạo tài khoản `STUDENT` hoặc `INSTRUCTOR`, sinh mật khẩu tạm gửi qua email, bắt đổi ở lần đăng nhập đầu, ghi một dòng `system_audit_logs` | `identity` |

Tham số bổ sung của `ChangeUserStatus` (`[SoT: Suy luận]`, DD chốt tên): `reason` (bắt buộc khi
`notifyByEmail = true`), `notifyByEmail` (mặc định true) và `acknowledgeSelfImpact` (Sheet 9 NO 6, 7).

Endpoint số 8 do BD này đề xuất `[SoT: Suy luận]` (RD F1-13 đã mở rộng, `01-rd/req/identity.md:69`); tên và quyền thật do `03-dd/api/identity.md` chốt, xem Q1. Việc gộp nhiều tài khoản vào một lời gọi
hay gọi nhiều lần là quyết định kỹ thuật của DD, không phải BD.

[Nguồn: 02-bd/database/identity.md:8-24,31-43,103-114]

---

## Sheet 8. Danh sách sự kiện

> Bộ thẻ và quy ước cột `Chuyển màn`: `02-bd/_rules/bd-template-9sheet.md` mục 2, 3.

### Màn chính: Quản lý người dùng

| NO | Loại | Sự kiện | Chi tiết | Chuyển màn | Gọi API | Tên xử lý | Ghi chú |
| --: | :--- | :--- | :--- | :-: | :-: | :--- | :--- |
| 1 | Màn hình | Khởi tạo màn | Vào màn thì tải ba nhóm dữ liệu cùng danh sách vai trò. | Không | Có | `ListUsers`, `GetUserManagementStats`, `GetRoleDistribution`, `ListRoles` | [Các bước]<br>1. Kiểm tra quyền `USER_MANAGEMENT:READ`.<br>2. Hiển thị khung chờ cho bảng và hai khối thống kê.<br>3. Tải song song bốn lời gọi; `ListRoles` tải sẵn để popup đổi vai trò mở ra không phải chờ.<br>[Khi thành công] Bảng hiển thị trang 1 với bộ lọc "Tất cả", thanh hành động gộp không hiển thị vì chưa chọn dòng nào.<br>[Khi lỗi] Hiển thị thông báo lỗi kèm nút thử lại tại đúng khối tải thất bại; **không** hiển thị dữ liệu cũ của khối đó để tránh hiểu nhầm. Các khối tải được vẫn hiển thị bình thường, không rời màn. |
| 2 | Nhập liệu | Tìm kiếm theo từ khoá | Gõ vào ô tìm kiếm. | Không | Có | `ListUsers` | [Các bước]<br>1. Chờ một khoảng ngắn sau khi ngừng gõ.<br>2. Bỏ toàn bộ lựa chọn đang có, đưa về trang 1.<br>3. Gọi lại danh sách với từ khoá mới.<br>[Khi thành công] Bảng và nhãn số kết quả cập nhật theo từ khoá.<br>[Khi lỗi] Giữ nguyên từ khoá đã gõ, hiển thị lỗi ở vùng bảng. |
| 3 | Nút | Đổi tab vai trò | Bấm một tab trong nhóm vai trò. | Không | Có | `ListUsers` | [Các bước]<br>1. Đặt tab đang chọn.<br>2. Bỏ toàn bộ lựa chọn đang có, đưa về trang 1.<br>3. Gọi lại danh sách.<br>[Khi thành công] Bảng chỉ còn tài khoản thuộc `base_category` tương ứng; tab "Tất cả" bỏ điều kiện này.<br>[Khi lỗi] Giữ nguyên tab vừa chọn, hiển thị lỗi ở vùng bảng. |
| 4 | Nút | Đổi tab trạng thái | Bấm một tab trong nhóm trạng thái. | Không | Có | `ListUsers` | [Các bước]<br>1. Đặt tab đang chọn.<br>2. Bỏ toàn bộ lựa chọn đang có, đưa về trang 1.<br>3. Gọi lại danh sách.<br>[Khi thành công] Bảng chỉ còn tài khoản có `status` tương ứng.<br>[Khi lỗi] Giữ nguyên tab vừa chọn, hiển thị lỗi ở vùng bảng. |
| 5 | Nút | Chọn hoặc bỏ chọn một dòng | Bấm ô chọn ở đầu dòng. | Không | Không | - | [Các bước]<br>1. Đảo trạng thái chọn của dòng đó.<br>2. Tính lại số tài khoản đang chọn.<br>[Khi thành công] Có ít nhất một dòng được chọn thì thanh hành động gộp hiện ra; bỏ chọn hết thì thanh ẩn đi. Lựa chọn **không** được giữ khi đổi trang hoặc đổi bộ lọc. |
| 6 | Nút | Trang trước | Bấm "Trước" ở vùng phân trang. | Không | Có | `ListUsers` | [Các bước]<br>1. Giảm số trang một đơn vị.<br>2. Bỏ toàn bộ lựa chọn đang có.<br>3. Tải trang mới.<br>[Khi thành công] Bảng hiển thị trang liền trước, nhãn trang cập nhật.<br>[Khi lỗi] Giữ nguyên trang hiện tại, hiển thị lỗi ở vùng bảng. |
| 7 | Nút | Trang sau | Bấm "Sau" ở vùng phân trang. | Không | Có | `ListUsers` | [Các bước]<br>1. Tăng số trang một đơn vị.<br>2. Bỏ toàn bộ lựa chọn đang có.<br>3. Tải trang mới.<br>[Khi thành công] Bảng hiển thị trang liền sau, nhãn trang cập nhật.<br>[Khi lỗi] Giữ nguyên trang hiện tại, hiển thị lỗi ở vùng bảng. |
| 8 | Nút | Mở xác nhận đặt lại mật khẩu | Bấm nút "Đặt lại mật khẩu" ở cột "Thao tác" của một dòng. | Không | Không | - | [Các bước]<br>1. Ghi nhớ tài khoản của dòng.<br>2. Mở popup xác nhận.<br>[Khi thành công] Popup nêu tên và **email** của tài khoản, ghi rõ mật khẩu tạm gửi qua email và không hiển thị cho quản trị viên.<br>[Ghi chú] **Sửa 2026-10-08:** từ thanh gộp chuyển sang nút trên dòng (Q7); chỉ một tài khoản [Nguồn: 05-coding/frontend/src/views/admin/user-management/ui/admin-user-management-view.tsx:300-305,481-490] |
| 9 | Popup | Xác nhận đặt lại mật khẩu | Bấm "Đặt lại" trong popup đặt lại mật khẩu. | Không | Có | `ResetUserPassword` | [Các bước]<br>1. Kiểm tra quyền `USER_MANAGEMENT:UPDATE`.<br>2. Áp dụng cho **một** tài khoản và ghi một dòng `system_audit_logs` (F1-14). Tài khoản không có mật khẩu (chỉ OAuth) xử lý theo Sheet 9 NO 10.<br>3. Đóng popup.<br>[Khi thành công] Mật khẩu tạm gửi tới email của người dùng; màn không hiển thị mật khẩu.<br>[Khi lỗi] Hiện toast lỗi, giữ nguyên tài khoản.<br>[Thông báo hoàn tất] Toast "Đã gửi mật khẩu tạm tới {email}".<br>[Ghi chú] Prototype chỉ bắn toast, chưa gửi email và chưa ghi log thật [Nguồn: 05-coding/frontend/src/views/admin/user-management/ui/admin-user-management-view.tsx:202-205] |
| 10 | Nút | Mở popup đổi vai trò | Bấm nút "Đổi vai trò" ở cột "Thao tác" của một dòng. | Không | Không | - | [Các bước]<br>1. Ghi nhớ tài khoản của dòng và cờ "đây là chính mình".<br>2. Mở popup với ô chọn vai trò đặt sẵn vai trò hiện tại.<br>[Khi thành công] Popup hiển thị, nút xác nhận chưa kích hoạt vì chưa đổi vai trò [Nguồn: 05-coding/frontend/src/views/admin/user-management/ui/change-role-dialog.tsx:22-47]. |
| 11 | Popup | Xác nhận đổi vai trò | Chọn vai trò khác rồi bấm "Đổi vai trò". | Không | Có | `ChangeUserRole` | [Các bước]<br>1. Kiểm tra quyền `USER_MANAGEMENT:UPDATE`.<br>2. Kiểm các ràng buộc ở Sheet 9: hạ ADMIN hoạt động cuối cùng là **chặn cứng** (toast, không đổi gì); tự hạ vai trò chính mình chỉ là **cảnh báo** đã hiện sẵn trong popup, không chặn [Nguồn: 05-coding/frontend/src/views/admin/user-management/model/guards.ts:53-67; 05-coding/frontend/src/views/admin/user-management/ui/admin-user-management-view.tsx:190-195].<br>3. Áp dụng cho **một** tài khoản, ghi một dòng `system_audit_logs` (F1-14).<br>4. Đóng popup, cập nhật thẻ vai trò của dòng và khối phân bố theo vai trò.<br>[Khi lỗi] Vi phạm ràng buộc thì từ chối, toast nêu lý do.<br>[Thông báo hoàn tất] Toast "Đã đổi vai trò của {tên} thành {vai trò}".<br>[Ghi chú] **Sửa 2026-10-08:** viết lại cho một tài khoản, bỏ bước "hỏi xác nhận lần nữa" và "toast số tài khoản" của bản gộp. |
| 12 | Nút | Mở xác nhận khoá tài khoản | Bấm "Khóa tài khoản" trên thanh hành động gộp, hoặc nút khoá ở cột "Thao tác" của một dòng chưa bị khoá. | Không | Không | - | [Các bước]<br>1. Kiểm chặn cứng "luôn còn ít nhất một ADMIN hoạt động" (Sheet 9 NO 8) — vi phạm thì **chỉ** hiện toast chặn, không mở popup.<br>2. Tách tập đang chọn theo trạng thái: chỉ dòng **chưa** bị khoá mới vào popup; số dòng đã bị khoá đi vào dòng cảnh báo bỏ qua.<br>3. Mở popup với danh sách tên tài khoản, lý do rỗng, công tắc gửi email **bật**.<br>[Khi thành công] Popup hiển thị số tài khoản chịu tác động kèm tên từng tài khoản, và cảnh báo tài khoản bị khoá mất quyền đăng nhập ngay ở lần kiểm quyền kế tiếp [Nguồn: 02-bd/security/identity.md:60-62]. Tập có chính tài khoản đang đăng nhập thì thêm cảnh báo nổi bật.<br>[Ghi chú] **Sửa 2026-10-05:** thêm bước tách trạng thái và hai ô nhập lý do / gửi email theo F1-32. Nút không kích hoạt khi tập chọn không có dòng nào cần khoá. |
| 13 | Popup | Xác nhận khoá tài khoản | Bấm "Khóa tài khoản" trong popup khoá. | Không | Có | `ChangeUserStatus` | [Các bước]<br>1. Kiểm tra quyền `USER_MANAGEMENT:UPDATE`.<br>2. Kiểm các ràng buộc nghiệp vụ ở Sheet 9: ADMIN cuối cùng là **chặn cứng**; tự khoá chính mình chỉ là **cảnh báo xác nhận**, không chặn. Lý do rỗng khi còn bật gửi email là **lỗi nhập liệu** (Sheet 9 NO 16).<br>3. Đặt `status = 'DEACTIVATED'` và `deactivated_at` cho **từng** tài khoản thực sự bị khoá, mỗi tài khoản ghi một dòng `system_audit_logs` riêng, kèm **lý do**.<br>4. Nếu còn bật gửi email thì gửi thông báo cho mỗi tài khoản (cùng nội dung, kể cả tài khoản OAuth).<br>5. Đóng popup, bỏ lựa chọn, tải lại bảng và khối phân bố theo vai trò.<br>[Khi xác nhận] Trong tập đang chọn có chính tài khoản của người đang thao tác thì popup hiển thị cảnh báo rõ ("Bạn đang khoá chính tài khoản đang đăng nhập; phiên sẽ bị đăng xuất.") và yêu cầu xác nhận lần nữa trước khi gửi.<br>[Khi thành công] Các dòng liên quan hiển thị trạng thái "Bị khóa".<br>[Khi lỗi] Tài khoản vi phạm ràng buộc bị từ chối riêng, các tài khoản còn lại vẫn bị khoá; toast nêu rõ danh sách bị từ chối kèm lý do.<br>[Thông báo hoàn tất] Toast đếm **số tài khoản thực sự bị khoá** — nhỏ hơn số đang chọn khi có dòng bị bỏ qua; kèm "đã gửi email thông báo" nếu đã bật gửi email. |
| 14 | Nút | Huỷ trong popup xác nhận | Bấm "Huỷ" hoặc đóng một trong bốn popup xác nhận (khoá, mở khoá, đổi vai trò, đặt lại mật khẩu). | Không | Không | - | [Các bước]<br>1. Đóng popup; ở popup khoá thì xoá lý do đã nhập và trả công tắc gửi email về mặc định bật.<br>[Khi thành công] Không tài khoản nào bị đổi, danh sách đang chọn giữ nguyên để quản trị viên chọn hành động khác. Không cần hỏi xác nhận vì chưa có thay đổi nào được ghi. |
| 15 | Nút | Mở popup Thêm tài khoản | Bấm "Thêm tài khoản" ở thanh tiêu đề. | Không | Không | - | [Các bước]<br>1. Mở popup nhập liệu, khởi tạo họ tên và email rỗng, vai trò "Học viên".<br>[Khi thành công] Popup hiển thị (hành vi đã chốt vào phạm vi 2026-10-01, Q1; prototype đã dựng). |
| 16 | Liên kết | Mở nhật ký hệ thống | Bấm "Nhật ký" ở khối "Cần xử lý". | Có | Không | - | [Các bước]<br>1. Điều hướng sang màn `admin_system_log`.<br>[Khi thành công] Mở màn `admin_system_log`. Màn này không có thay đổi chưa lưu — mọi hành động ghi đều đi qua popup xác nhận và hoàn tất ngay — nên **không** hỏi xác nhận khi rời màn; danh sách đang chọn bị bỏ. |
| 17 | Nút | Tạo tài khoản | Bấm "Tạo tài khoản" trong popup Thêm tài khoản. | Không | Có | `CreateUserAccount` | [Các bước]<br>1. Kiểm quyền `USER_MANAGEMENT:CREATE` (máy chủ).<br>2. Kiểm họ tên không rỗng, email đúng định dạng và không trùng (Sheet 9 NO 13, 14); không hợp lệ thì ô vi phạm chỉ đổi viền đỏ, hiện **một toast** mang lỗi đầu tiên và dừng [Nguồn: 05-coding/frontend/src/views/admin/user-management/ui/add-account-dialog.tsx:48-53].<br>3. Tạo tài khoản với vai trò đã chọn, sinh mật khẩu tạm gửi qua email, ghi một dòng `system_audit_logs`.<br>[Khi thành công] Popup đóng và hiện toast "Đã tạo tài khoản"; tài khoản mới hiện đầu danh sách với trạng thái chờ xác thực. Prototype chỉ thêm dòng vào danh sách cục bộ và **không gửi email thật**.<br>[Khi lỗi] Giữ popup, giữ nội dung đã nhập, hiện toast lỗi.<br>[Thông báo hoàn tất] Toast "Mật khẩu tạm đã được gửi tới {email}." |
| 18 | Nút | Đóng popup Thêm tài khoản | Bấm "Huỷ" hoặc "Xong", hoặc đóng popup. | Không | Không | - | [Các bước]<br>1. Đóng popup, xoá họ tên, email và vai trò đã nhập.<br>[Khi thành công] Danh sách không đổi nếu đóng trước khi tạo. |
| 19 | Nút | Mở popup mở khoá tài khoản | Bấm "Mở khoá tài khoản" trên thanh hành động gộp, hoặc nút mở khoá ở cột "Thao tác" của một dòng đang bị khoá. | Không | Không | - | [Các bước]<br>1. Tách tập đang chọn thành hai phần theo trạng thái; chỉ phần đã bị khoá mới đi vào popup.<br>2. Mở popup, liệt kê tên và email các tài khoản đó.<br>[Khi thành công] Popup hiển thị; nút không kích hoạt khi tập chọn không có dòng nào đang bị khoá.<br>[Ghi chú] **Mới 2026-10-05**, đóng Q2 |
| 20 | Popup | Xác nhận mở khoá tài khoản | Bấm "Mở khoá" trong popup. | Không | Có | `ChangeUserStatus` | [Các bước]<br>1. Kiểm quyền `USER_MANAGEMENT:UPDATE`.<br>2. Đặt `status = 'ACTIVE'` cho **từng** tài khoản đã bị khoá trong tập chọn, mỗi tài khoản ghi một dòng `system_audit_logs` riêng.<br>3. Đóng popup, bỏ lựa chọn, tải lại bảng và khối phân bố theo vai trò.<br>[Khi thành công] Các dòng liên quan hiển thị trạng thái "Hoạt động".<br>[Khi lỗi] Thành công một phần thì vẫn đóng popup và hiện **một toast** nêu rõ số thành công, số thất bại kèm lý do; không hoàn tác phần đã thành công.<br>[Thông báo hoàn tất] Toast "Đã mở khoá {số} tài khoản."<br>[Ghi chú] **Không** có ô lý do và **không** gửi email — mở khoá là hành động khôi phục quyền truy cập [Nguồn: 01-rd/req/identity.md — F1-32] |

[EVT 8-11 — V1.0, 2026-10-08] Bốn sự kiện đặt lại mật khẩu và đổi vai trò đã bỏ khỏi chế độ gộp ngày 2026-10-05 (Q7) và **quay lại ở dạng một tài khoản**, mở từ nút ở cột "Thao tác" (điểm chạm mà Q7 để lại cho DD). Giữ nguyên số EVT để các tham chiếu ở Sheet 5, 6, 9 không phải đánh số lại. Mọi thao tác ghi đều ghi một dòng Nhật ký hệ thống (F1-14) — prototype chưa ghi thật. `ListRoles` (Sheet 7.3 NO 4) lại được màn này dùng.

[Nguồn: 09-layoutBase/Admin - Người dùng.dc.html:158,177-190,194-203,212,230-236,261; EVT-8 đến EVT-11 (2026-10-08): 05-coding/frontend/src/views/admin/user-management/ui/admin-user-management-view.tsx:190-205, 05-coding/frontend/src/views/admin/user-management/ui/change-role-dialog.tsx:22-76; 01-rd/screens/admin/ADM0201_user_management.md:53-56; 02-bd/security/identity.md:51-53,60-62; 01-rd/req/identity.md — F1-32]

---

## Sheet 9. Đặc tả kiểm tra

> Bộ thẻ và quy ước mã thông báo: `02-bd/_rules/bd-template-9sheet.md` mục 2, 4. Sheet này ghi **điều kiện
> kiểm và thông báo**; tiêu chí nghiệm thu `AC-nn` thuộc `04-tdd/admin_user_management.md`, không lặp lại ở
> đây.

| NO | Loại | Tóm tắt kiểm | Chi tiết | Mức | Mã thông báo | Ghi chú | EVT gọi | Thứ tự |
| --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: |
| 1 | Kiểm quyền | Quyền xem màn | [Nội dung kiểm] Người dùng không có `USER_MANAGEMENT:READ` thì không được vào màn.<br>[Nơi thực thi] Chặn ở cả tầng định tuyến phía giao diện và tầng phân quyền phía máy chủ, không chỉ ẩn giao diện [Nguồn: 02-bd/security/identity.md:25-26]. | Lỗi | Chưa có mã thông báo | Nội dung "Bạn không có quyền truy cập chức năng này." Đây là chức năng **Lớp 2** — kiểm qua ma trận `permissions` chứ không hard-code theo `base_category`, vì `USER_MANAGEMENT` là một trong 10 function seed bật/tắt được [Nguồn: 02-bd/database/identity.md:47-49]. | EVT-1 | 1 |
| 2 | Kiểm quyền | Quyền thực hiện hành động ghi | [Nội dung kiểm] Mọi hành động đổi vai trò, khoá tài khoản, đặt lại mật khẩu yêu cầu `USER_MANAGEMENT:UPDATE`.<br>[Nơi thực thi] Máy chủ, kiểm lại từng lời gọi kể cả khi giao diện đã ẩn nút.<br>[Tiêu điểm] Thanh hành động gộp và nút ở cột "Thao tác". | Lỗi | Chưa có mã thông báo | Nội dung "Bạn không có quyền thực hiện thao tác này." | EVT-9, EVT-11, EVT-13 | 1 |
| 3 | Kiểm nhập liệu | Độ dài từ khoá tìm kiếm | [Nội dung kiểm] Từ khoá dài quá 100 ký tự thì không gửi lên máy chủ.<br>[Nơi thực thi] Màn hình.<br>[Tiêu điểm] Viền ô tìm kiếm + toast. | Lỗi | Chưa có mã thông báo | Nội dung "Từ khoá tìm kiếm tối đa 100 ký tự." Giới hạn 100 là `[Suy luận]` theo độ dài thực tế của tên và email, DD chốt số chính xác. | EVT-2 | 1 |
| 4 | Kiểm nhập liệu | Phải chọn vai trò đích khác vai trò hiện tại | [Nội dung kiểm] Chưa chọn vai trò đích, hoặc vai trò chọn trùng vai trò hiện tại của tài khoản, thì không cho xác nhận đổi vai trò.<br>[Nơi thực thi] Popup đổi vai trò và máy chủ.<br>[Tiêu điểm] Danh sách vai trò trong popup. | Lỗi | Chưa có mã thông báo | Nội dung "Hãy chọn vai trò muốn áp dụng." Nút xác nhận đã chặn sẵn theo Sheet 6 (prototype 2026-10-08 tắt nút khi vai trò chưa đổi [Nguồn: 05-coding/frontend/src/views/admin/user-management/ui/change-role-dialog.tsx:47]); kiểm này phòng trường hợp gọi thẳng máy chủ. | EVT-11 | 1 |
| 5 | Kiểm nhập liệu | Tập tài khoản không rỗng | [Nội dung kiểm] Danh sách tài khoản gửi lên rỗng thì từ chối lời gọi.<br>[Nơi thực thi] Máy chủ. | Lỗi | Chưa có mã thông báo | Nội dung "Chưa chọn tài khoản nào." Thanh hành động gộp chỉ hiện khi có lựa chọn, nên lỗi này chỉ xảy ra khi gọi thẳng máy chủ. | EVT-9, EVT-11, EVT-13 | 2 |
| 6 | Kiểm nghiệp vụ | Cảnh báo khi tự khoá chính mình | [Nội dung kiểm] Tài khoản đích trùng tài khoản đang thao tác thì **không chặn**; yêu cầu xác nhận lần nữa có cảnh báo rõ trước khi gửi. Máy chủ chỉ nhận khi cờ xác nhận tự ảnh hưởng (`acknowledgeSelfImpact`, tên đề xuất `[SoT: Suy luận]`) bằng true.<br>[Nơi thực thi] Màn hình (hộp xác nhận) và máy chủ (kiểm cờ).<br>[Tiêu điểm] Dòng vi phạm trong bảng. | Cảnh báo | Chưa có mã thông báo | Nội dung "Bạn đang khoá chính tài khoản đang đăng nhập; phiên sẽ bị đăng xuất. Tiếp tục?" Đã chốt 2026-10-01 (Q6): đổi từ chặn cứng sang cảnh báo xác nhận; nếu đó là ADMIN hoạt động cuối cùng thì vẫn bị chặn cứng bởi NO 8. | EVT-13 | 3 |
| 7 | Kiểm nghiệp vụ | Cảnh báo khi tự hạ vai trò chính mình | [Nội dung kiểm] Tài khoản đích trùng tài khoản đang thao tác và vai trò đích có `base_category` khác `ADMIN` thì **không chặn**; yêu cầu xác nhận lần nữa có cảnh báo rõ. Máy chủ chỉ nhận khi cờ `acknowledgeSelfImpact` bằng true.<br>[Nơi thực thi] Màn hình và máy chủ.<br>[Tiêu điểm] Dòng vi phạm trong bảng. | Cảnh báo | Chưa có mã thông báo | Nội dung "Bạn sắp tự hạ quyền tài khoản đang đăng nhập và sẽ mất truy cập khu Admin. Tiếp tục?" Đã chốt 2026-10-01 (Q6); nếu đó là ADMIN hoạt động cuối cùng thì vẫn bị chặn cứng bởi NO 8. **Prototype 2026-10-08:** cảnh báo hiện ngay trong popup đổi vai trò, không có bước hỏi lần hai, rồi cho xác nhận [Nguồn: 05-coding/frontend/src/views/admin/user-management/ui/change-role-dialog.tsx:72-76]. | EVT-11 | 3 |
| 8 | Kiểm nghiệp vụ | Giữ lại ít nhất một quản trị viên | [Nội dung kiểm] Thao tác làm cho hệ thống không còn tài khoản nào vừa có `roles.base_category = 'ADMIN'` vừa có `users.status = 'ACTIVE'` thì từ chối.<br>[Nơi thực thi] Máy chủ, tính trên trạng thái sau khi áp dụng **toàn bộ** tập đang chọn, không tính từng tài khoản riêng lẻ. | Lỗi | Chưa có mã thông báo | Nội dung "Thao tác này sẽ không còn quản trị viên nào hoạt động." Phải tính trên cả tập vì chọn hai quản trị viên cuối cùng rồi khoá gộp sẽ lọt nếu chỉ kiểm từng dòng. Chưa có trong F1-13, xem Q6. **Prototype 2026-10-08** chặn cứng cả ở khoá (`checkLock`) lẫn ở hạ vai trò một tài khoản (`checkRoleChange`, toast cảnh báo, không đổi gì) [Nguồn: 05-coding/frontend/src/views/admin/user-management/model/guards.ts:35-67; 05-coding/frontend/src/views/admin/user-management/ui/admin-user-management-view.tsx:155-160,190-195]. | EVT-11, EVT-13 | 4 |
| 9 | Kiểm nghiệp vụ | Tài khoản đã ở trạng thái đích | [Nội dung kiểm] Tài khoản đã có `status = 'DEACTIVATED'` thì bỏ qua, không ghi nhật ký, không tính là lỗi.<br>[Nơi thực thi] Máy chủ. | Cảnh báo | Chưa có mã thông báo | Nội dung "{số} tài khoản đã bị khoá từ trước, đã bỏ qua." Giữ hành động gộp mang tính bình thản khi quản trị viên quét chọn cả trang. | EVT-13 | 5 |
| 10 | Kiểm nghiệp vụ | Tài khoản chỉ đăng nhập bằng OAuth | [Nội dung kiểm] Tài khoản có `password_hash` rỗng (chỉ tạo qua OAuth, F1-15) thì không đặt lại mật khẩu, chỉ gửi email hướng dẫn quay lại đúng provider.<br>[Nơi thực thi] Máy chủ. | Cảnh báo | Chưa có mã thông báo | Nội dung "{số} tài khoản chỉ đăng nhập bằng nhà cung cấp ngoài, đã gửi hướng dẫn thay vì mật khẩu mới." Cùng nguyên tắc với luồng quên mật khẩu [Nguồn: 02-bd/security/identity.md:46-48]. **Khoảng trống prototype 2026-10-08:** dữ liệu mẫu không có cờ nhận biết tài khoản chỉ OAuth, nút "Đặt lại mật khẩu" trên dòng kích hoạt cho mọi dòng — xem Q8. | EVT-9 | 3 |
| 11 | Kiểm nghiệp vụ | Ghi nhật ký không ngoại lệ | [Nội dung kiểm] Mỗi tài khoản chịu tác động phải ghi đúng một dòng `system_audit_logs`, kể cả khi quản trị viên thao tác trên một quản trị viên khác.<br>[Nơi thực thi] Máy chủ, trong cùng giao dịch với thao tác ghi. | Lỗi | Chưa có mã thông báo | Ghi nhật ký thất bại thì thao tác trên tài khoản đó coi như thất bại và không áp dụng — không có đường ghi dữ liệu mà bỏ nhật ký [Nguồn: 02-bd/security/identity.md:51-53]. | EVT-9, EVT-11, EVT-13 | 6 |
| 12 | Kiểm nghiệp vụ | Lỗi hệ thống hoặc lỗi gọi máy chủ | [Nội dung kiểm] Gọi máy chủ thất bại hoặc trả lỗi nghiệp vụ thì dừng thao tác, không hiển thị dữ liệu cũ của khối lỗi.<br>[Nơi thực thi] Màn hình. | Lỗi | Mã lỗi trong phản hồi | Phản hồi có mã lỗi đã đăng ký thì hiện toast nội dung tương ứng; chưa đăng ký thì hiện toast "Không kết nối được máy chủ." (khối tải lỗi vẫn giữ nút thử lại tại chỗ). | EVT-1, EVT-2, EVT-3, EVT-4, EVT-6, EVT-7, EVT-9, EVT-11, EVT-13, EVT-17 | 1 |
| 13 | Kiểm nhập liệu | Họ tên bắt buộc | [Nội dung kiểm] Họ và tên rỗng (sau khi cắt khoảng trắng hai đầu) thì không tạo tài khoản.<br>[Nơi thực thi] Màn hình (hiện sau lần bấm "Tạo tài khoản" đầu tiên) và máy chủ.<br>[Tiêu điểm] Viền ô "Họ và tên" + toast. | Lỗi | Chưa có mã thông báo | Nội dung "Nhập họ và tên" (đúng chữ prototype). | EVT-17 | 1 |
| 14 | Kiểm nhập liệu | Email đúng định dạng và không trùng | [Nội dung kiểm] Email phải khớp dạng `chuỗi@chuỗi.chuỗi` không có khoảng trắng (prototype dùng mẫu `^[^\s@]+@[^\s@]+\.[^\s@]+$`) và, sau khi cắt khoảng trắng và đổi sang chữ thường, không trùng email của tài khoản đang có.<br>[Nơi thực thi] Màn hình (so với tập email đã tải) và máy chủ (ràng buộc duy nhất trên `users.email`, bắt buộc vì màn hình chỉ biết các tài khoản đã tải).<br>[Tiêu điểm] Viền ô "Email" + toast. | Lỗi | Chưa có mã thông báo | Hai nội dung: "Email không hợp lệ" và "Email này đã có tài khoản" (đúng chữ prototype). Kiểm định dạng chạy trước kiểm trùng. | EVT-17 | 2 |
| 15 | Kiểm quyền | Quyền tạo tài khoản và giới hạn vai trò | [Nội dung kiểm] Tạo tài khoản đòi `USER_MANAGEMENT:CREATE`; vai trò đích chỉ được là `STUDENT` hoặc `INSTRUCTOR`, vai trò khác (kể cả `ADMIN`) bị từ chối.<br>[Nơi thực thi] Máy chủ, không chỉ ẩn lựa chọn ở popup. | Lỗi | Chưa có mã thông báo | Nội dung "Bạn không có quyền thực hiện thao tác này." `[SoT: Suy luận]` — giới hạn hai vai trò lấy từ phạm vi đã chốt của Q1 (INSTRUCTOR hoặc STUDENT) và từ popup prototype. | EVT-17 | 3 |
| 16 | Kiểm nhập liệu | Lý do khoá rỗng khi bật gửi email | [Nội dung kiểm] Công tắc gửi email **đang bật** mà lý do để trống (chỉ khoảng trắng) thì không gửi hành động; ô lý do đổi viền đỏ và hiện **một toast** "Nhập lý do để thông báo cho người dùng". Tắt gửi email thì lý do để trống vẫn khoá được.<br>[Nơi thực thi] Màn hình (popup khoá) và máy chủ (kiểm lại).<br>[Tiêu điểm] Ô lý do + toast.<br>**Vì sao cần:** gửi email "tài khoản của bạn đã bị khoá" mà không kèm lý do không cho người nhận biết phải làm gì, và họ sẽ hỏi admin rồi admin lại tra log — mất đúng thứ mà nhật ký vốn phải giải quyết trong một bước [Nguồn: 01-rd/req/identity.md — F1-32]. | Lỗi | Chưa có mã thông báo | Nội dung "Nhập lý do để thông báo cho người dùng". Viền đỏ chỉ hiện sau lần bấm xác nhận đầu tiên, không hiện khi mới mở popup — cùng nguyên tắc NO 13 | EVT-12 | 1 |
| 17 | Kiểm nghiệp vụ | Nội dung email không lộ dữ liệu tài khoản khác | [Nội dung kiểm] Email thông báo khoá nêu tài khoản bị khoá, thời điểm, lý do, cách liên hệ quản trị viên; **không** nêu dữ liệu của tài khoản khác trong cùng tập chọn, không nêu chi tiết kỹ thuật. Tài khoản chỉ đăng nhập bằng OAuth nhận cùng nội dung.<br>[Nơi thực thi] Máy chủ khi dựng nội dung email.<br>[Ghi chú] Cùng nguyên tắc F1-17: gửi email hướng dẫn quay lại provider thay vì mã OTP cho tài khoản OAuth [Nguồn: 01-rd/req/identity.md — F1-15, F1-17, F1-32]. | Cảnh báo | Chưa có mã thông báo | — | EVT-12 | 2 |
| 18 | Kiểm nghiệp vụ | Mở khoá không cần lý do, không gửi email | [Nội dung kiểm] Popup mở khoá không có ô lý do và không gửi email cho người dùng; chỉ ghi một dòng `system_audit_logs` cho mỗi tài khoản.<br>[Nơi thực thi] Màn hình và máy chủ.<br>[Tiêu điểm] Popup mở khoá.<br>**Vì sao:** mở khoá là hành động khôi phục quyền truy cập — người dùng vừa nhận được thông báo bị khoá thì không cần thông báo ngược lại, và bắt quản trị viên nhập lý do "vì tôi đã bỏ nhầm" chỉ tạo thêm việc. | Lỗi | Chưa có mã thông báo | [Nguồn: 01-rd/req/identity.md — F1-32] | EVT-19 | 1 |
| 19 | Kiểm nghiệp vụ | Báo cáo đúng số tài khoản thực sự bị khoá | [Nội dung kiểm] Tập chọn có dòng đã bị khoá thì popup khoá phải nêu rõ số dòng bị bỏ qua, và thông báo hoàn tất cũng phải đếm **đúng số dòng thực sự bị khoá** — nhỏ hơn số đang chọn.<br>[Nơi thực thi] Màn hình (hiển thị) và máy chủ (tính, giống NO 9).<br>[Tiêu điểm] Dòng cảnh báo trong popup khoá và thông báo hoàn tất.<br>**Vì sao:** nếu thông báo hoàn tất đếm cả dòng bị bỏ qua thì quản trị viên tưởng đã khoá đủ số mình chọn trong khi thực tế không — đây là dạng nhầm lẫn nghiêm trọng với hành động phá huỷ. | Cảnh báo | Chưa có mã thông báo | [Nguồn: Sheet 9 NO 9; 01-rd/req/identity.md — F1-32] | EVT-12, EVT-13 | 3 |

Cột `Thứ tự` là thứ tự kiểm trong cùng một sự kiện.

[Nguồn: 02-bd/security/identity.md:25-26,46-48,51-53; 02-bd/database/identity.md:15,21,47-49]

---

## Câu hỏi mở

| # | Câu hỏi | Vì sao chưa trả lời được | Chủ sở hữu |
| :-: | :--- | :--- | :--- |
| Q1 | ~~Nút "Thêm tài khoản" ở thanh tiêu đề có nằm trong phạm vi không?~~ F1-13 chỉ nêu đổi vai trò, khoá/mở khoá, đặt lại mật khẩu — **không** nêu việc quản trị viên tạo tài khoản thay người dùng [Nguồn: 01-rd/screens/admin/ADM0201_user_management.md:21-22]. Prototype có nút nhưng không có màn đích [Nguồn: 09-layoutBase/Admin - Người dùng.dc.html:158]. **ĐÃ CHỐT 2026-10-01 (owner uỷ quyền cân nhắc), xem `DEC-2026-1001-admin-configurable-settings`:** đưa vào phạm vi — ADMIN tạo tài khoản thủ công (chọn vai trò `INSTRUCTOR` hoặc `STUDENT`, đặt mật khẩu tạm gửi qua email, bắt đổi ở lần đăng nhập đầu), mỗi lần tạo ghi một dòng `system_audit_logs`. **Cập nhật V0.7 (vòng 5):** RD F1-13 đã mở rộng ở `01-rd/req/identity.md:69` (vòng 4, E7); prototype đã dựng popup (họ tên, email, vai trò, thông báo mật khẩu tạm, tài khoản mới ở trạng thái chờ xác thực, chỉ thêm cục bộ, không gửi email). **Còn lại:** endpoint `CreateUserAccount` thật, nơi lưu trạng thái "chờ xác thực" (Q3) và danh sách vai trò của popup (prototype cố định hai giá trị, BD đề xuất cuối cùng đọc từ `ListRoles` lọc `base_category` khác `ADMIN`, `[SoT: Suy luận]`). | Hai việc cuối thuộc DD `identity` và Q3 | Chủ dự án + DD `identity` |
| Q2 | ~~Mở khoá tài khoản làm ở đâu? Thanh hành động gộp chỉ có "Khóa tài khoản", prototype không có nút mở khoá nào [Nguồn: 09-layoutBase/Admin - Người dùng.dc.html:194-203].~~ **ĐÃ CHỐT 2026-10-05 (owner chỉ đạo khi review prototype):** nút **"Mở khoá tài khoản"** đặt cạnh "Khoá tài khoản" trên thanh hành động gộp, **hiện luôn nhưng không kích hoạt** khi tập chọn không có dòng nào đang bị khoá (và đối xứng, "Khoá" không kích hoạt khi tập chọn toàn dòng đã khoá). Không dùng một nút đảo chiều: tên nút phải nói đúng hướng đang làm, và một tập chọn có cả hai loại dòng thì nút đảo chiều sẽ không biết đang nói tới loại nào. Endpoint `ChangeUserStatus` đã tính cả hai chiều, chỉ thiếu điểm chạm trên giao diện. | Đã đóng | Đã đóng |
| Q7 | **Đặt lại mật khẩu và đổi vai trò có còn áp dụng hàng loạt không?** Prototype dựng ba nút trên thanh gộp nhưng hai nút kia chỉ bắn toast giả. | Không có bằng chứng nghiệp vụ cho thao tác gộp: reset mật khẩu là việc riêng cho từng người (và không áp dụng với tài khoản OAuth, Sheet 9 NO 10), đổi vai trò ép cả nhóm về một vai trò đích là thao tác không hoàn tác được. **ĐÃ CHỐT 2026-10-05 (owner chỉ đạo):** bỏ cả hai khỏi thanh gộp; phạm vi không mất, chuyển sang thao tác trên một tài khoản. | Đã đóng. **Điểm chạm: nút trên dòng, đã dựng prototype 2026-10-08** (cột "Thao tác", Sheet 5 Khu vực D NO 14-18) [Nguồn: 05-coding/frontend/src/views/admin/user-management/ui/admin-user-management-view.tsx:286-325]; DD chốt hợp đồng API. Xem RD `ADM0201` Q7. |
| Q3 | Trạng thái "Chờ xác thực email" lưu ở đâu? Màn dùng nó ở hai chỗ: nhãn trạng thái thứ ba trong bảng, và dòng duy nhất còn lại của khối "Cần xử lý" (chỗ thứ ba, thẻ chỉ số, đã bỏ 2026-10-01). Nhưng `02-bd/database/identity.md` mục 1.1 **không có cột `email_verified`** và `users.status` chỉ có hai giá trị `ACTIVE`/`DEACTIVATED` [Nguồn: 02-bd/database/identity.md:11-24]. | Đây là phát hiện mới của V0.2: BD cũ ghi công thức `COUNT(users WHERE email_verified = false)` nhưng cột đó chưa từng được thiết kế. F1-01 có bước xác thực email nên dữ liệu này phải tồn tại ở đâu đó — có thể là cột mới trên `users`, có thể là một giá trị thứ ba của `users.status`, có thể là khoá Redis có TTL 7 ngày như prototype gợi ý ("Quá hạn 7 ngày sẽ tự huỷ"). Ba phương án khác nhau về chi phí migration, BD này **không tự chọn**. | BD `database/identity.md` + Chủ dự án |
| Q4 | ~~Mốc "hoạt động gần nhất" của một tài khoản lấy từ đâu?~~ **ĐÃ CHỐT** theo `DEC-2026-0922-users-and-admin-conflict-resolutions`: `identity` đã có cột `users.last_active_at` (TIMESTAMPTZ nullable, ghi nhận đăng nhập/nộp bài, cập nhật hãm theo phút) [Nguồn: 02-bd/database/identity.md:22]. Cột này phục vụ đúng cả hai chỗ: cột "Hoạt động" trong bảng và thẻ chỉ số "Đang hoạt động 24 giờ" (đếm `users WHERE last_active_at >= now() - interval '24 hours'`). Ngưỡng "đang hoạt động" tức thời (khác 24 giờ) dùng cho việc khác không phát sinh ở màn này. Sheet 5, 6 và 7.1 đã cập nhật. | Đã đóng | Đã đóng |
| Q5 | **Không còn áp dụng từ 2026-10-01** (dải thẻ chỉ số đã bỏ khỏi màn). Con số biến động (`+42`, `−3`...) và phần số trong câu chú thích của mỗi thẻ chỉ số tính theo mốc so sánh nào — so với hôm qua, so với tuần trước, hay so với đầu tháng? | Prototype ghi số minh hoạ cứng [Nguồn: 09-layoutBase/Admin - Người dùng.dc.html:422-428], RD không nêu yêu cầu nào về biến động. Chưa chốt mốc thì không có công thức, và BD không bịa ra một cửa sổ thời gian. | Chủ dự án |
| Q6 | ~~Ba ràng buộc bảo vệ ở Sheet 9 (không tự khoá chính mình, không tự hạ vai trò, luôn còn ít nhất một quản trị viên hoạt động) có được chấp nhận làm quy tắc nghiệp vụ chính thức không?~~ **ĐÃ CHỐT 2026-10-01 (owner uỷ quyền cân nhắc), xem `DEC-2026-1001-admin-configurable-settings`:** (a) **giữ chặn cứng** "luôn còn ít nhất một quản trị viên hoạt động" — đây là bất biến bảo toàn khả năng vào lại và cấu hình hệ thống của admin, nhất quán với nguyên tắc "admin chủ động cấu hình mọi thứ" chứ không phải một giới hạn quyền; (b) "không tự khoá chính mình" và "không tự hạ vai trò chính mình" đổi từ chặn cứng sang **cảnh báo xác nhận có cảnh báo rõ** (cho phép làm, không chặn). Sheet 9 NO 6, 7 đã đổi sang mức Cảnh báo; NO 8 giữ nguyên mức Lỗi. `04-tdd/admin_user_management.md` cần tiêu chí tương ứng. | Đã đóng | Đã đóng |
| Q8 | **Tài khoản chỉ đăng nhập bằng OAuth nhận "mật khẩu tạm" thế nào?** Sheet 9 NO 10 yêu cầu tài khoản không có `password_hash` thì không đặt lại mật khẩu mà gửi email hướng dẫn quay lại provider; nhưng nút "Đặt lại mật khẩu" trên dòng (Sheet 5 Khu vực D NO 16) kích hoạt cho mọi dòng. | Dữ liệu mẫu của prototype không có cờ cho biết tài khoản chỉ đăng nhập bằng OAuth [Nguồn: 05-coding/frontend/src/views/admin/user-management/api/__mock__/admin-user-mocks.ts:32-47], nên giao diện không thể tắt nút hay đổi nội dung popup theo loại tài khoản. Cần quyết định: `ListUsers` có trả cờ (ví dụ `hasPassword`) để tắt nút hay đổi chữ popup, hay cứ cho bấm và để máy chủ chọn email phù hợp (NO 10)? `[SoT: Suy luận]` | Chủ dự án + DD `identity` |
