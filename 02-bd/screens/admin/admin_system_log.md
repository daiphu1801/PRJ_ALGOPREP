# BD — Màn `admin_system_log` (Nhật ký hệ thống)

> Trục: **màn hình** (frontend), theo `.claude/skills/bd-generation/SKILL.md` Layer 1. Slug khớp
> `01-rd/screens/admin/admin_system_log.md` → sẽ khớp tiếp `03-dd/screens/admin/admin_system_log.md`
> khi viết DD (anti-drift rule 3, `bd-generation/SKILL.md` Layer 2).
> Actor: A3 (`ADMIN`) — không gác bởi Function trong ma trận phân quyền F1-10, vì F1-14 ghi rõ đây là nơi
> audit chính hành vi đổi quyền, nên không thể tự gác bởi chính ma trận nó ghi lại
> [SoT: 01-rd/req/identity.md:69-75 — F1-14]. Bounded Context sở hữu dữ liệu: `identity` (F1)
> [SoT: 01-rd/screens/admin/admin_system_log.md:4].
> Ngôn ngữ tài liệu: tiếng Việt theo `CLAUDE.md` — Directory map / Language policy.

## 0. Phạm vi và ranh giới (quan trọng — đọc trước khi thiết kế API/DB)

Màn này **chỉ đọc** một luồng dữ liệu duy nhất: **hành động quản trị của con người** (đổi ma trận quyền,
đổi vai trò, khoá/mở khoá tài khoản, reset mật khẩu, thao tác nội dung kho bài/kho câu hỏi của giảng
viên...) [SoT: 01-rd/req/identity.md:69-75 — F1-14]. **Không** gộp sự kiện hạ tầng/dịch vụ (lỗi go-judge,
worker mất kết nối, timeout sweep) — các sự kiện đó thuộc `judge-orchestration` (F4-10) và có màn riêng
`admin_queue_monitor` [SoT: 01-rd/req/identity.md:72-75 — F1-14; 09-layoutBase/Admin - Nhật ký hệ
thống.dc.html:152, 245]. Đây là ranh giới cứng, không phải gợi ý UX — nếu DD sau này định gộp hai nguồn
vào chung một bảng, đó là thay đổi kiến trúc cần quyết định mới.

Nguồn ghi log (ai được phép tạo một dòng `SystemAuditLog`) gồm hai nhóm hành vi, cả hai đều đi qua cùng
một cơ chế ghi (interceptor/aspect ở tầng ứng dụng `identity`, không phải do từng module tự gọi API ghi
log rời rạc — tránh bỏ sót):
1. Hành động quản trị thuần `identity` — đổi ô ma trận phân quyền (F1-10), đổi vai trò tài khoản (F1-13),
   khoá/mở khoá tài khoản (F1-13), reset mật khẩu hộ (F1-13), đổi cấu hình hệ thống (giới hạn ngôn ngữ,
   thời gian/bộ nhớ mặc định).
2. Hành động quản trị nội dung của giảng viên (A2) qua các Function khác trong ma trận F1-10 —
   `PROBLEM_AUTHORING`, `TESTCASE_MANAGEMENT`, `AI_CONFIG` — vì câu chữ gốc F1-14 là "mọi thao tác quản
   trị", không giới hạn actor A3 [SoT: 01-rd/screens/admin/admin_system_log.md:41-43]. Đây khớp với dòng
   log mẫu "Thêm testcase biên..." và "Xuất bản bài toán..." do `pthuong` (giảng viên) thực hiện
   [SoT: 09-layoutBase/Admin - Nhật ký hệ thống.dc.html:405, 410].

**Loại "Chấm lại" trong prototype (dòng mẫu `#RJ-0139`, chỉ số "Phiên chấm lại đã chạy") đã hết phạm vi**
theo `DEC-2026-0828-remove-rejudge-scope` [SoT: 01-rd/screens/admin/admin_system_log.md:15-16, 36-38].
BD này **loại bỏ** phân loại "Chấm lại" khỏi bộ lọc, khỏi chỉ số tổng, và khỏi dữ liệu mẫu khi build FE
thật — còn lại 4 phân loại: Xác thực, Ma trận quyền, Cấu hình, Nội dung.

## 1. Layout regions

Đối chiếu `09-layoutBase/Admin - Nhật ký hệ thống.dc.html`, mô tả theo cấu trúc (không lấy màu/spacing cụ
thể — chưa có design-system token cho Next.js, theo đúng giới hạn `bd-generation/SKILL.md` Layer 3):

| Vùng | Nội dung | SoT |
| :--- | :--- | :--- |
| Thanh tiêu đề (sticky top) | Tên màn "Nhật ký hệ thống" + phụ đề ghi rõ ranh giới phạm vi ("Hành động quản trị của con người · sự kiện hạ tầng xem ở Hàng đợi chấm"); toggle Theo dõi trực tiếp; nút "Tải nhật ký" (export) | [SoT: 09-layoutBase/Admin - Nhật ký hệ thống.dc.html:149-163] |
| Dải chỉ số tổng | 3 thẻ chỉ số (sau khi bỏ "Chấm lại"): Hành động quản trị 24 giờ, Đổi ma trận phân quyền, Khoá/mở khoá tài khoản | [SoT: 09-layoutBase/Admin - Nhật ký hệ thống.dc.html:165-176, 395-400] |
| Khối danh sách sự kiện (cột trái, chiếm phần lớn chiều rộng) | Ô tìm kiếm (dịch vụ/tài khoản/mã sự kiện) + tab lọc phân loại (Tất cả/Xác thực/Ma trận quyền/Cấu hình/Nội dung) + đếm kết quả; danh sách dòng log dạng bảng phi cấu trúc (giờ, phân loại, nội dung, dịch vụ·người thực hiện·mã sự kiện); footer phân trang "Tải thêm 50 dòng" | [SoT: 09-layoutBase/Admin - Nhật ký hệ thống.dc.html:180-213] |
| Khối phụ (cột phải) — "Quản trị viên hoạt động" | Danh sách người thực hiện gần nhất kèm số hành động, vai trò, thời điểm | [SoT: 09-layoutBase/Admin - Nhật ký hệ thống.dc.html:216-231] |
| Khối phụ (cột phải) — "Phân loại hành động · 7 ngày" | Biểu đồ cột ngang theo 4 phân loại (bỏ "Chấm lại") kèm liên kết ghi chú trỏ sang `admin_queue_monitor` cho sự kiện hạ tầng | [SoT: 09-layoutBase/Admin - Nhật ký hệ thống.dc.html:233-246] |
| Footer | Thông tin phiên bản, thời hạn lưu log, liên kết phụ | [SoT: 09-layoutBase/Admin - Nhật ký hệ thống.dc.html:250-263] |

## 2. Component inventory

| Component | Vai trò | Ghi chú thiết kế |
| :--- | :--- | :--- |
| `AuditLogHeader` | Tiêu đề + toggle live + nút export | Toggle live chỉ là trạng thái UI (bật/tắt nhận cập nhật mới), không phải điều khiển server |
| `StatCard` (x3) | Thẻ chỉ số tổng | Tái dùng component đã có ở `admin_overview`/`class_progress` nếu FE đã có sẵn — không thiết kế lại từ đầu |
| `AuditLogSearchBar` | Ô tìm kiếm tự do (khớp dịch vụ, tên/username người thực hiện, mã sự kiện) | Debounce phía client trước khi gọi API; không tìm theo nội dung log để tránh full-text scan nặng trên bảng lớn — xem mục 5 |
| `AuditLogCategoryTabs` | 5 tab: Tất cả, Xác thực, Ma trận quyền, Cấu hình, Nội dung | Ánh xạ 1-1 với cột `category` trong DB (mục database) |
| `AuditLogTable` (hoặc `AuditLogList` dạng thẻ) | Danh sách dòng log, mỗi dòng: giờ (HH:mm:ss), badge phân loại, nội dung (message), dòng phụ: dịch vụ · người thực hiện · mã sự kiện | Badge màu theo phân loại — chi tiết bảng màu để lại cho Next.js thật, không hard-code hex ở BD |
| `AuditLogPagination` | Nút "Tải thêm N dòng" (infinite scroll dạng nút, không phải phân trang số trang) | Giữ đúng kiểu UX của prototype; khác kiểu phân trang số trang của `problem_list` (F2-11) — có chủ đích vì đây là luồng thời gian, không phải danh sách tĩnh |
| `ActiveAdminsPanel` | Danh sách người thực hiện gần nhất + số hành động | Đếm trong cùng khung thời gian với chỉ số tổng (24 giờ) — xem câu hỏi mở mục 7 |
| `CategoryBreakdownChart` | Biểu đồ cột ngang theo phân loại, 7 ngày gần nhất | Kèm dòng chú thích liên kết sang `admin_queue_monitor` cho sự kiện hạ tầng — giữ nguyên từ prototype vì đúng ranh giới BC |
| `LiveUpdateIndicator` | Chấm nhấp nháy + nhãn "Theo dõi trực tiếp"/"Đã tạm dừng" | Trạng thái kết nối kênh cập nhật thời gian thực — xem mục 3 State D |

## 3. Screen states

| Mã | Tên trạng thái | Mô tả | SoT/Ghi chú |
| :--- | :--- | :--- | :--- |
| S1 | `loading` (tải lần đầu) | Khung skeleton cho 3 thẻ chỉ số + danh sách log + 2 khối phụ, tách riêng theo từng khối (không chặn cả màn nếu chỉ một khối lỗi) | Áp dụng cùng nguyên tắc "trạng thái theo từng khối riêng" đã chốt cho `admin_overview`/F1-29 [SoT: 01-rd/req/identity.md:158-159] |
| S2 | `success` (có dữ liệu) | Hiển thị đầy đủ như mục 1 | Mặc định |
| S3 | `empty` (chưa có log nào khớp bộ lọc) | Danh sách log rỗng, thông báo "Không có sự kiện nào khớp bộ lọc hiện tại" + nút "Xoá bộ lọc"; 3 thẻ chỉ số và 2 khối phụ vẫn hiển thị bình thường (không phụ thuộc bộ lọc danh sách) | [SoT: Suy luận — RD không mô tả rỗng, suy từ nguyên tắc UI chuẩn của dự án] |
| S4 | `error` (lỗi tải một khối) | Khối lỗi hiện thông báo + nút "Thử lại" riêng cho khối đó, các khối khác vẫn hoạt động | Cùng nguyên tắc đã chốt ở F1-29/F1-30 [SoT: 01-rd/req/identity.md:158-159, 185-186] |
| S5 | `live-streaming` | Khi toggle "Theo dõi trực tiếp" bật: dòng log mới xuất hiện ngay ở đầu danh sách khi có sự kiện mới, không cần người dùng bấm tải lại | [SoT: 01-rd/screens/admin/admin_system_log.md:54-57 — GWT bổ sung mục 4] |
| S6 | `live-paused` | Toggle tắt: danh sách đứng yên, không tự chèn dòng mới cho tới khi bật lại hoặc tải lại trang | [SoT: 09-layoutBase/Admin - Nhật ký hệ thống.dc.html:154-156, 445, 452-456] |
| S7 | `filtered` | Một hoặc nhiều tiêu chí lọc (phân loại + từ khoá tìm kiếm) đang áp dụng đồng thời — kết hợp AND, khớp hành vi prototype (lọc phân loại và tìm kiếm áp dụng cùng lúc) | [SoT: 09-layoutBase/Admin - Nhật ký hệ thống.dc.html:414-418] |
| S8 | `loading-more` | Đang tải thêm 50 dòng tiếp theo sau khi bấm nút cuối danh sách | [SoT: 09-layoutBase/Admin - Nhật ký hệ thống.dc.html:211] |
| S9 | `export-in-progress` / `export-error` | Sau khi bấm "Tải nhật ký": trạng thái đang chuẩn bị file, và trạng thái lỗi nếu export thất bại | [SoT: Suy luận — nút có trong prototype (dòng 162) nhưng không mô tả hành vi chi tiết; hợp lý cho một thao tác bất đồng bộ có thể lỗi] |

## 4. APIs consumed

Chỉ liệt kê tên + Bounded Context sở hữu — hợp đồng request/response thuộc DD, viết ở
`03-dd/api/identity.md` (chưa viết) theo đúng anti-drift rule 1 của `bd-generation/SKILL.md`.

| Endpoint (tên nghiệp vụ, chưa phải route cụ thể) | BC sở hữu | Dùng cho |
| :--- | :--- | :--- |
| Tìm kiếm/lọc/phân trang Nhật ký hệ thống | `identity` | Khối danh sách sự kiện (tải lần đầu + "Tải thêm 50 dòng" + tìm kiếm/lọc) |
| Lấy chỉ số tổng Nhật ký hệ thống (24 giờ) | `identity` | 3 thẻ chỉ số tổng |
| Lấy danh sách quản trị viên hoạt động gần nhất | `identity` | `ActiveAdminsPanel` |
| Lấy phân loại hành động theo 7 ngày | `identity` | `CategoryBreakdownChart` |
| Kênh cập nhật thời gian thực cho dòng log mới | `identity` | S5 `live-streaming` — WebSocket (STOMP) theo stack đã khoá, không phải SSE (SSE dành riêng cho luồng hội thoại AI theo `CLAUDE.md` mục Locked stack) |
| Xuất Nhật ký hệ thống (CSV hoặc tương đương) | `identity` | Nút "Tải nhật ký" ở tiêu đề |

Không có endpoint nào thuộc `judge-orchestration` hay `ai-review` trên màn này — đúng ranh giới mục 0.
Ghi log tự nó (tạo một dòng `SystemAuditLog` khi có hành động quản trị xảy ra) **không phải** một API mà
màn này gọi — đó là tác dụng phụ nội bộ của các API khác (đổi ma trận quyền, đổi vai trò...) khi chúng
thực thi, thuộc phạm vi kiến trúc/logic của DD `identity`, không phải API riêng cho màn log.

## 5. Ghi chú thiết kế đáng lưu ý cho DD/database (không phải bản thân DB schema — xem giới hạn Layer 2)

- Tìm kiếm theo "dịch vụ, tài khoản hoặc mã sự kiện" [SoT: 09-layoutBase/Admin - Nhật ký hệ thống.dc.html:184]
  — không tìm full-text trên nội dung log (`message`) để tránh quét toàn bảng trên tập log lớn; nếu sau
  này cần tìm theo nội dung, cân nhắc chỉ mục full-text riêng, đây là quyết định của DD/database khi viết
  `02-bd/database/identity.md`.
- Toggle "Theo dõi trực tiếp" chỉ là trạng thái hiển thị phía client — không tạo thêm một loại yêu cầu
  server; khi bật, client subscribe kênh WebSocket; khi tắt, client hủy subscribe nhưng dữ liệu server
  không đổi hành vi.
- 4 phân loại (`category`): `AUTH`, `PERMISSION`, `CONFIG`, `CONTENT` — ánh xạ trực tiếp các nhóm hành vi
  đã liệt kê ở mục 0; đây là danh sách đóng, thêm phân loại mới cần sửa cả BD lẫn code sinh log.

## 6. Navigation

- Vào từ nhóm điều hướng "Hệ thống" trong sidebar khu Admin, mục "Nhật ký hệ thống"
  [SoT: 09-layoutBase/Admin - Nhật ký hệ thống.dc.html:312-316].
- Không có điều hướng đi tiếp từ một dòng log cụ thể sang màn khác trong prototype (khác `admin_overview`
  nơi một số widget có liên kết "Xem tất cả") — mỗi dòng log là điểm cuối thông tin, không phải link.
- Khối "Phân loại hành động · 7 ngày" có một liên kết văn bản trỏ sang `admin_queue_monitor` cho sự kiện
  hạ tầng [SoT: 09-layoutBase/Admin - Nhật ký hệ thống.dc.html:245].

## 7. Access rights

- Chỉ actor A3 (`ADMIN`) truy cập được màn này — kiểm ở tầng ứng dụng (route guard + `@PreAuthorize` phía
  backend), không chỉ ẩn/hiện menu, theo nguyên tắc RBAC chung của `identity`
  [SoT: 01-rd/req/identity.md:12-14].
- **Không gác bởi Function `SYSTEM_AUDIT_LOG` trong ma trận phân quyền F1-10** như các Function quản trị
  khác — vì F1-12 liệt kê `SYSTEM_AUDIT_LOG` như một Function trong ma trận
  [SoT: 01-rd/req/identity.md:56-61], nhưng RD của chính màn này lại nói "mọi thay đổi ma trận... không có
  ngoại lệ" và F1-13/F1-10 đã chốt "chỉ ADMIN thực hiện được" đổi ma trận
  [SoT: 09-layoutBase/Admin - Nhật ký hệ thống.dc.html:397]. **BD nêu điểm mâu thuẫn này làm câu hỏi mở
  (mục 8, Q1)** thay vì tự chọn một phía — vì nếu gác bởi ma trận, một `ADMIN` có thể tự tắt quyền đọc
  audit log của chính mình, làm mất tác dụng "không có ngoại lệ" của F1-14.
- Giảng viên (A2) — dù là người thực hiện một phần hành động được ghi log (mục 0 nhóm 2) — **không** có
  quyền xem màn này; A2 không nằm trong RD của màn (chỉ actor A3 được nêu)
  [SoT: 01-rd/screens/admin/admin_system_log.md:4].
- Không hiển thị testcase ẩn hay diff chi tiết bài nộp trên màn này — không áp dụng vì màn không liên
  quan `problem-bank`/`judge-orchestration`, nêu để xác nhận không vi phạm quy tắc chung của dự án
  [SoT: CLAUDE.md — mục Rules, "Must Not" của bd-generation].

## 8. Câu hỏi mở

- **Q1 — Nhật ký hệ thống có bị gác bởi Function `SYSTEM_AUDIT_LOG` trong chính ma trận phân quyền
  không?** RD liệt kê `SYSTEM_AUDIT_LOG` là một Function trong ma trận F1-12
  [SoT: 01-rd/req/identity.md:60], nhưng nếu áp dụng, một `ADMIN` có thể tự tắt quyền đọc log của chính
  mình hoặc của `ADMIN` khác, mâu thuẫn với "không có ngoại lệ" của F1-14
  [SoT: 01-rd/req/identity.md:69-71]. Đề xuất: `SYSTEM_AUDIT_LOG` chỉ gác được thao tác **chỉnh sửa cấu
  hình liên quan tới log** (nếu có, ví dụ đổi thời hạn lưu), còn quyền **đọc** log luôn mặc định có sẵn
  cho mọi tài khoản vai trò `ADMIN`, không đi qua ma trận — giữ nguyên tinh thần "quyền học tập cơ bản
  không qua ma trận" đã áp dụng cho lớp 1 (F1-05) nhưng áp cho "quyền audit cơ bản của ADMIN". Cần chủ dự
  án chốt khi viết DD.
- **Q2 — Thời hạn lưu log 90 ngày trong footer prototype có phải giá trị chính thức không?**
  [SoT: 09-layoutBase/Admin - Nhật ký hệ thống.dc.html:256] ghi "Nhật ký giữ 90 ngày" nhưng RD chỉ đánh
  dấu đây là `[SoT: Suy luận]` cần chốt ở BD [SoT: 01-rd/screens/admin/admin_system_log.md:76-77]. BD này
  **tạm nhận 90 ngày làm giá trị mặc định** (giữ nguyên số của prototype thay vì suy luận số khác, vì
  không có căn cứ nào tốt hơn) — cần chủ dự án xác nhận hoặc chỉnh khi viết `02-bd/database/identity.md`
  (chính sách xoá/nén log cũ hơn 90 ngày, ví dụ archive job).
- **Q3 — Khung thời gian đếm "Quản trị viên hoạt động"** (mục 2) có phải cùng 24 giờ với 3 thẻ chỉ số tổng
  không, hay là một cửa sổ khác (ví dụ phiên đăng nhập gần nhất)? Prototype không nói rõ
  [SoT: 09-layoutBase/Admin - Nhật ký hệ thống.dc.html:216-231]. `[SoT: Suy luận]` — BD tạm chọn cùng 24
  giờ để nhất quán với thẻ chỉ số đầu tiên, chốt khi viết DD.

## 9. Tham chiếu

- `01-rd/screens/admin/admin_system_log.md` — RD của màn.
- `01-rd/req/identity.md` — F1-10 đến F1-14.
- `09-layoutBase/Admin - Nhật ký hệ thống.dc.html` — prototype, SoT layout.
- `.nexa/domain-registry.json` — BC `identity`, schema `identity`.
- `01-rd/screens/admin/admin_user_management.md` mục 5 Q1 — liên hệ dòng log "trùng mã nguồn" (không lặp
  lại nội dung, chỉ xác nhận xuất hiện nhất quán).
