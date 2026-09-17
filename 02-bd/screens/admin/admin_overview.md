# BD — Màn `admin_overview` (Tổng quan khu Quản trị)

> Trục màn hình theo `CLAUDE.md` mục "Hai trục tài liệu". File này **không** định nghĩa API request/response,
> schema DB hay cơ chế bảo mật — những phần đó thuộc `03-dd/api/*.md`, `02-bd/database/*.md`,
> `02-bd/security/*.md` tương ứng (anti-drift rule của `bd-generation/SKILL.md` Layer 2). Đây là file BD
> **đầu tiên** ở `02-bd/screens/` — thư mục này trước đó chỉ có `.gitkeep`.
>
> Nguồn RD: `01-rd/screens/admin/admin_overview.md` (toàn bộ 8 câu hỏi mở đã đóng 2026-08-31). Actor: A3
> (Quản trị viên). Bounded Context chính: `identity` (F1-29) — màn **tổng hợp và điều hướng**, đọc số liệu từ
> `problem-bank`, `judge-orchestration`, `ai-review` nhưng không sở hữu logic nghiệp vụ của các module đó
> [SoT: 01-rd/screens/admin/admin_overview.md:3-7].

## 1. Bounded Context và nguồn dữ liệu từng khối

Màn không sở hữu bảng dữ liệu riêng ngoài các bảng đã có ở `identity` — nó là một **read model tổng hợp**
(aggregation view), đúng khuôn F1-29 [SoT: 01-rd/req/identity.md:142-147]. Bảng dưới ánh xạ từng khối nội
dung sang Bounded Context cung cấp số liệu, để xác định đúng DD sẽ viết ở module nào:

| # | Khối nội dung | BC cung cấp số liệu | Mã RD |
| :-: | :--- | :--- | :--- |
| 1 | Thẻ "Tổng lượt nộp bài" | `judge-orchestration` (đếm submission) | F1-29 |
| 2 | Thẻ "Người dùng hoạt động" | `identity` (đếm user đăng nhập ≤ 30 ngày, loại `DEACTIVATED`) | F1-29 |
| 3 | "Lượt nộp theo ngôn ngữ" | `judge-orchestration` (nhóm theo `language`) | F1-29 |
| 4 | "Kết quả chấm" (5 verdict) | `judge-orchestration` (nhóm theo `verdict`) | F1-29, `DEC-2026-0831-admin-overview-ui-decisions` |
| 5 | "Độ khó bài toán" | `problem-bank` (nhóm theo `difficulty`) | F1-29, F2-02 |
| 6 | "Lượt nộp theo ngày" | `judge-orchestration` | F1-29 |
| 7 | "Lượt nộp theo tháng" | `judge-orchestration` | F1-29 |
| 8 | "Bài phổ biến nhất" | `problem-bank` (đếm submission theo `problem_id`, join sang `judge-orchestration`) | F1-29 |
| 9 | "Người dùng mới / quay lại" | `identity` (đếm theo `created_at` / `last_login_at`) | F1-29 |

`ai-review` không cấp số liệu cho khối nào ở phiên bản hiện tại của RD (không có widget "chi phí AI"/"tỉ lệ
dùng AI" trên `admin_overview` — số liệu AI nằm ở `admin_ai_usage`, ngoài phạm vi màn này)
[SoT: 01-rd/req/identity.md:142-164 — không nhắc `ai-review`]. Tuy vậy nguyên tắc "AI subsystem degrades
gracefully" vẫn áp dụng cho toàn màn qua yêu cầu trạng thái lỗi theo từng khối (mục 4) — nếu về sau một khối
đọc từ `ai-review` được thêm vào, khối đó phải tự chịu lỗi độc lập, không kéo sập màn
[SoT: 01-rd/screens/admin/admin_overview.md:171-174].

**Chốt phạm vi module cho DD**: endpoint tổng hợp dữ liệu của màn này thuộc `identity` (module sở hữu màn,
vì đây là đích mặc định sau đăng nhập của `ADMIN`, không phải Function riêng trong ma trận F1-10
[SoT: 01-rd/req/identity.md:145-147]) — `identity` gọi read model/query nội bộ (hoặc cross-module query qua
outbound port đã có, theo baseline `DEC-2026-0820-architecture-baseline`) sang `problem-bank` và
`judge-orchestration` để lấy số liệu, KHÔNG expose route riêng ở hai module đó cho màn này. Chi tiết cơ chế
gọi (đồng bộ qua port, hay đọc bảng tổng hợp định kỳ) để `03-dd/logic/identity.md` quyết định.

## 2. Layout regions

Dựa 1:1 theo cấu trúc HTML của prototype [SoT: 09-layoutBase/Admin - Tổng quan.dc.html] — không có design
token/màu sắc cụ thể nào được chốt ở đây (chưa có design-system SoT theo `bd-generation/SKILL.md` Layer 3),
chỉ mô tả cấu trúc vùng và hành vi.

1. **Shell (khung chung mọi màn Admin)** — sidebar trái (nav 5 nhóm, có thể thu gọn) + vùng nội dung chính.
   Không đặc thù riêng cho `admin_overview`, dùng lại cho toàn bộ 11 đích nav
   [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:57-101, 361-378].
2. **Thanh công cụ đầu trang** — 3 nút biểu tượng không nhãn (`layout-grid`/`moon`/`shield-check`, không có
   handler ở prototype) + khối danh tính người dùng đang đăng nhập
   [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:106-122]. **Ô tìm kiếm liên thực thể đã bỏ khỏi UI thật**
   theo `DEC-2026-0831-admin-overview-ui-decisions` — không đưa vào layout khi build FE.
3. **Hàng 1 — lưới 3 cột** (`1fr 1.25fr 1fr`) [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:124]:
   - Cột trái: 2 thẻ chỉ số dọc (Tổng lượt nộp bài, Người dùng hoạt động), mỗi thẻ có giá trị + delta so kỳ
     trước + sparkline [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:126-139].
   - Cột giữa: widget "Lượt nộp theo ngôn ngữ" — legend 3 màu (Python/C++/Java) + biểu đồ cột 20 mốc
     [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:142-156]. Đơn vị trục hoành (20 ngày hay 20 tuần) do
     `03-dd/logic/identity.md` chốt [SoT: 01-rd/req/identity.md:150].
   - Cột phải: widget "Kết quả chấm" — đồng hồ nửa vòng + legend **5 verdict** (`AC`/`WA`/`TLE`/`RE`/`CE`)
     [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:158-177, số lượng verdict theo `DEC-2026-0831-admin-overview-ui-decisions`
     thay cho 3 nhóm gộp ở HTML gốc].
4. **Hàng 2 — lưới 2 cột** (`1.3fr 1fr`) [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:180]:
   - Trái: "Độ khó bài toán" — cột đôi 3 mức (Dễ/Trung bình/Khó). **Trục phân loại đổi nhãn** từ "AI sinh /
     Giảng viên soạn" (nhãn mẫu sai) sang một phân loại có thật, ví dụ tỉ lệ Accepted theo độ khó
     [SoT: Suy luận — BD chọn trục cụ thể theo uỷ quyền của `DEC-2026-0831-admin-overview-dashboard-stats`;
     lý do: hệ thống không có thuộc tính "nguồn gốc bài toán" trong `problem-bank`]. Đề xuất cụ thể: 2 cột mỗi
     mức độ khó là (a) tổng số lượt nộp và (b) số lượt `Accepted`, để suy ra tỉ lệ pass trực quan — tận dụng
     đúng số liệu đã có ở `judge-orchestration`/`problem-bank`, không cần thuộc tính mới.
   - Phải: "Lượt nộp theo ngày" — lưới dot 7 cột (T2→CN), chân widget hiện tổng lượt nộp trong tuần và trung
     bình/ngày [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:207-230]. **Chốt định nghĩa phạm vi số liệu**
     (mục 3.14 của RD nêu mâu thuẫn dữ liệu mẫu): `weekTotal` là tổng lượt nộp **trong 7 ngày hiển thị**, khác
     với thẻ "Tổng lượt nộp bài" ở hàng 1 (đó là tổng **toàn hệ thống**, không giới hạn thời gian)
     [SoT: 01-rd/screens/admin/admin_overview.md:143-148 — cảnh báo dữ liệu mẫu không nhất quán].
5. **Hàng 3 — lưới 3 cột đều** (`1fr 1fr 1fr`) [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:233]:
   - "Lượt nộp theo tháng" — tổng số + đường 6 tháng gần nhất
     [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:234-249]. `monthTotal` là tổng **6 tháng gần nhất**, một
     phạm vi thứ ba khác hai phạm vi trên (theo cùng cảnh báo mục 3.14 của RD).
   - "Bài phổ biến nhất" — 4 bài kèm thanh tỉ lệ, có liên kết **"Xem tất cả"** dẫn sang màn `problem_management`
     [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:251-268; liên kết theo `DEC-2026-0831-admin-overview-ui-decisions`].
     Đây là **widget duy nhất** trong màn có liên kết đi tiếp — các widget biểu đồ còn lại giữ nguyên là tóm
     tắt, không thêm liên kết.
   - "Người dùng mới / cũ" — cột đôi 6 tháng, legend "Người dùng mới"/"Quay lại"
     [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:270-292].
6. **Nhóm liên kết "KHÁC" và công tắc theme** — nằm trong shell (sidebar), không đặc thù riêng màn này
   [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:88-100]. Lưu ý nhãn "Cài đặt" trỏ tới `admin_language_config`
   chứ không phải `settings` cá nhân — giữ nguyên cảnh báo của RD, sửa nhãn khi build FE thật
   [SoT: 01-rd/screens/admin/admin_overview.md:122-127].

## 3. Component inventory

| Component | Dùng ở khối | Props/behavior cấp BD (không phải token màu) |
| :--- | :--- | :--- |
| `StatCardWithSparkline` | Hàng 1 cột trái (2 thẻ) | value, deltaPercent, deltaDirection (tăng/giảm), sparklineSeries, loading/error/empty state riêng |
| `HorizontalBarChart` (legend nhiều màu) | "Lượt nộp theo ngôn ngữ" | series theo 3 ngôn ngữ cố định (Java/C++/Python — khớp giới hạn phạm vi ngôn ngữ nộp bài), trục hoành cần nhãn thời gian (thiếu ở prototype, phải bổ sung khi build FE) |
| `HalfDonutGauge` | "Kết quả chấm" | 5 lát tương ứng 5 verdict, mỗi lát có icon + nhãn + phần trăm, tổng luôn quy về 100% |
| `GroupedBarChart` (2 cột/nhóm) | "Độ khó bài toán" | 3 nhóm (Dễ/Trung bình/Khó), mỗi nhóm 2 cột theo trục đã đổi nhãn (mục 2.4) |
| `DotGrid` (7 cột x 10 dot) | "Lượt nộp theo ngày" | mỗi cột là 1 ngày trong tuần, độ sáng dot theo bậc giá trị, footer 2 số tổng hợp |
| `LineChartWithTotal` | "Lượt nộp theo tháng" | 6 điểm dữ liệu (6 tháng gần nhất), số tổng lớn phía trên |
| `RankedProgressList` | "Bài phổ biến nhất" | danh sách xếp hạng, mỗi dòng có thanh tỉ lệ theo giá trị lớn nhất, item cuối cùng có liên kết "Xem tất cả" |
| `GroupedBarChart` (2 cột/nhóm, 6 nhóm) | "Người dùng mới / cũ" | 6 nhóm theo tháng, mỗi nhóm 2 cột (mới/quay lại) |
| `DashboardBlockState` (wrapper chung) | Cả 9 khối ở mục 1 | bọc từng khối riêng biệt, tự quản lý 3 trạng thái `loading`/`empty`/`error` độc lập — xem mục 4 |

**Không đưa vào component inventory**: ô tìm kiếm liên thực thể (đã loại bỏ theo quyết định), 3 nút biểu
tượng không handler ở thanh công cụ (giữ nguyên là placeholder trang trí, không gán hành vi mới vì RD không
định nghĩa — `[SoT: Suy luận]`, không có mã `Fx-nn` nào cho 3 nút này).

## 4. Trạng thái màn hình (screen states)

Theo `DEC-2026-0831-admin-overview-ui-decisions`: **mỗi khối trong 9 khối ở mục 1 tự quản lý 3 trạng thái
riêng**, không có trạng thái toàn màn chung [SoT: 01-rd/req/identity.md:157-159]. Cụ thể từng khối:

- **`loading`**: hiện skeleton (khung xám mờ theo đúng kích thước khối cuối cùng — tránh layout shift), không
  chặn các khối khác render.
- **`empty`**: khối trả về dữ liệu hợp lệ nhưng rỗng (ví dụ hệ thống mới cài, chưa có lượt nộp nào) — hiện
  một dòng chú thích ngắn thay vì biểu đồ trống trơn, ví dụ "Chưa có dữ liệu lượt nộp".
- **`error`**: nguồn số liệu của khối đó không phản hồi hoặc lỗi — hiện thông báo lỗi cục bộ trong đúng
  khung của khối + nút "Thử lại", các khối còn lại không bị ảnh hưởng
  [SoT: 01-rd/screens/admin/admin_overview.md:171-174 — nguyên tắc không sập cả màn].

**Trạng thái mức màn** (không thuộc 9 khối, áp dụng cho khung shell): màn luôn render được (nav + thanh công
cụ) ngay cả khi toàn bộ 9 khối đang `loading` hoặc `error`, vì shell không phụ thuộc bất kỳ API tổng hợp nào
— đây là phần tĩnh dùng chung mọi màn Admin.

Không có trạng thái "màn trống hoàn toàn" (empty-state cấp màn) vì không có mã `Fx-nn` hay quyết định nào
định nghĩa khi nào toàn hệ thống coi là "chưa có dữ liệu gì" — mỗi khối tự quyết theo dữ liệu riêng của nó.

## 5. API dự kiến (chỉ liệt kê tên + BC sở hữu — chốt request/response ở DD)

Theo anti-drift rule, KHÔNG viết request/response ở đây. API dự kiến, chốt ở `03-dd/api/<module>.md`:

| Khối cần dữ liệu | Endpoint dự kiến (tên, chưa chốt path) | BC sở hữu |
| :--- | :--- | :--- |
| Thẻ tổng lượt nộp + delta | `GET /admin/overview/submissions-summary` (tên tạm) | `identity` (tổng hợp, đọc chéo `judge-orchestration`) |
| Thẻ người dùng hoạt động + delta | `GET /admin/overview/active-users-summary` (tên tạm) | `identity` |
| Lượt nộp theo ngôn ngữ | `GET /admin/overview/submissions-by-language` (tên tạm) | `identity` (tổng hợp, đọc chéo `judge-orchestration`) |
| Kết quả chấm (5 verdict) | `GET /admin/overview/verdict-distribution` (tên tạm) | `identity` (tổng hợp, đọc chéo `judge-orchestration`) |
| Độ khó bài toán | `GET /admin/overview/difficulty-breakdown` (tên tạm) | `identity` (tổng hợp, đọc chéo `problem-bank`) |
| Lượt nộp theo ngày | `GET /admin/overview/submissions-by-day` (tên tạm) | `identity` |
| Lượt nộp theo tháng | `GET /admin/overview/submissions-by-month` (tên tạm) | `identity` |
| Bài phổ biến nhất | `GET /admin/overview/top-problems` (tên tạm) | `identity` (tổng hợp, đọc chéo `problem-bank`) |
| Người dùng mới / quay lại | `GET /admin/overview/user-retention` (tên tạm) | `identity` |

Tất cả 9 endpoint dự kiến thuộc `03-dd/api/identity.md` (chưa viết). Việc gộp thành 1 endpoint tổng hợp
(single dashboard payload) hay giữ 9 endpoint riêng để mỗi khối tự fetch/tự chịu lỗi độc lập
(khớp yêu cầu 3 trạng thái theo từng khối ở mục 4) là quyết định của DD — khuyến nghị **giữ riêng theo khối**
để một nguồn lỗi không kéo theo toàn bộ payload lỗi, nhưng đây chỉ là đề xuất, không phải chốt cứng
`[SoT: Suy luận]`.

## 6. Navigation

- **Điểm vào**: đích mặc định sau khi `ADMIN` đăng nhập thành công [SoT: 01-rd/screens/shared/auth.md:90 —
  Q3 đã chốt]; cũng là item đầu tiên của nav (`key: 'overview'`, không có item con)
  [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:359, 386-389].
- **Điều hướng đi (từ màn này)**:
  - Nav sidebar → 10 đích khác (Quản lý bài tập, Câu hỏi phỏng vấn, Hàng đợi chấm, Ngôn ngữ và giới hạn,
    Cấu hình AI, Token AI, Người dùng, Nhật ký hệ thống, Ma trận phân quyền — trừ "Chấm lại" đã loại khỏi
    phạm vi theo `DEC-2026-0828-remove-rejudge-scope`, KHÔNG đưa vào nav khi build FE thật)
    [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:361-378].
  - "Xem tất cả" (khối Bài phổ biến nhất) → `problem_management`
    [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:254; quyết định `DEC-2026-0831-admin-overview-ui-decisions`].
  - Nhóm "KHÁC": "Cài đặt" → `admin_language_config`, "Đăng nhập & Đăng ký" → `auth`, "Trang cá nhân" →
    `profile` [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:403-407].
- **Điều hướng đến (vào màn này)**: chỉ từ luồng đăng nhập (redirect theo vai trò) hoặc bấm mục "Tổng quan"
  trên nav từ bất kỳ màn Admin nào khác. Không có deep-link tham số (không có query string định nghĩa
  khoảng thời gian/lọc — mọi khối tự lấy dữ liệu mặc định).
- Bấm một **nhóm nav có item con** (Nội dung/Vận hành/AI/Hệ thống) chỉ mở/đóng nhóm, KHÔNG điều hướng và
  KHÔNG rời khỏi `admin_overview` [SoT: 09-layoutBase/Admin - Tổng quan.dc.html:386-389].

## 7. Access rights (RBAC)

Theo `02-bd/security/identity.md` mục 2: kiểm quyền ở tầng application, không chỉ ẩn/hiện UI.

- **Ai xem được màn**: mọi tài khoản có `role_code = ADMIN` và `status != DEACTIVATED`. **Không** là một
  `FUNCTION` riêng trong ma trận F1-10 — mọi vai trò `ADMIN` đều thấy trang này vô điều kiện, vì đây là đích
  mặc định sau đăng nhập, không phải màn con bị gác quyền [SoT: 01-rd/req/identity.md:145-147]. Đây là
  trường hợp duy nhất trong khu Admin không tra bảng `permissions` — kiểm bằng `base_category = ADMIN`
  (tương tự nguyên tắc Lớp 1 ở `02-bd/security/identity.md:31-34`, áp dụng chéo sang vai trò quản trị).
- **9 khối dữ liệu bên trong**: không tự thân bị gác Function riêng (F1-29 không phải một hàng trong ma trận
  F1-10), nhưng **RD đã ghi một Given-When-Then suy luận** rằng nếu ma trận quyền không cấp một Function
  liên quan (ví dụ `JUDGE_QUEUE_MONITOR` cho F4-10) cho vai trò hiện tại, đích nav và khối số liệu tương ứng
  không hiển thị hoặc hiện trạng thái không có quyền, thay vì lỗi kỹ thuật
  [SoT: 01-rd/screens/admin/admin_overview.md:166-170 — `[SoT: Suy luận]` nguyên gốc]. Vì `ADMIN` mặc định có
  toàn quyền trên mọi Function (F1-11 — chỉ `ADMIN` có toàn quyền theo seed mặc định
  [SoT: 01-rd/req/identity.md:48-54, 61]), trường hợp một `ADMIN` bị hạn chế Function chỉ xảy ra nếu chủ dự
  án admin khác chủ động sửa ma trận — hệ quả UI này để BD nêu, DD/FE hiện thực khi build.
- **Không kiểm quyền sở hữu dữ liệu** cho màn này — khác các màn con như `admin_user_management`, dữ liệu
  hiển thị ở đây luôn là số liệu **toàn hệ thống**, không theo phạm vi cá nhân/lớp của người xem
  (khác hẳn `class_progress`/F1-28, nơi phạm vi giới hạn theo lớp giảng viên phụ trách
  [SoT: 01-rd/req/identity.md:137]).
- **Audit**: bản thân việc xem `admin_overview` **không** được ghi vào Nhật ký hệ thống — F1-14 chỉ ghi hành
  động quản trị làm thay đổi trạng thái (đổi ma trận quyền, đổi vai trò, khoá/mở khoá, reset mật khẩu), không
  ghi lượt truy cập xem dashboard [SoT: 01-rd/req/identity.md:70-75 — phạm vi F1-14 giới hạn ở "hành động
  quản trị của người", không phải mọi lượt truy cập].

## 8. Câu hỏi mở

Không phát sinh câu hỏi mở mới — RD đã đóng toàn bộ 8 câu hỏi mở (Q1-Q8, 2026-08-31)
[SoT: 01-rd/screens/admin/admin_overview.md:176-192], và các quyết định liên quan
(`DEC-2026-0831-admin-overview-dashboard-stats`, `DEC-2026-0831-admin-overview-ui-decisions`) đã cho đủ căn
cứ để chốt layout/state/navigation ở mức BD. Một điểm để DD lưu ý khi viết `03-dd/logic/identity.md` (không
phải câu hỏi mở, chỉ là bàn giao): công thức chính xác cho "delta so kỳ trước" (khung thời gian so sánh) và
đơn vị trục "Lượt nộp theo ngôn ngữ" (20 ngày hay 20 tuần) chưa có giá trị cụ thể, để DD tự chốn
[SoT: 01-rd/req/identity.md:150].

---

**Status:** DONE
**Summary:** Đã viết BD cho màn `admin_overview` tại `02-bd/screens/admin/admin_overview.md` — layout 3 hàng
lưới theo đúng prototype, 9 khối dữ liệu map sang `identity`/`problem-bank`/`judge-orchestration`, 9 API dự
kiến (chỉ tên, không request/response), trạng thái loading/empty/error theo từng khối riêng, RBAC theo
`base_category = ADMIN` không qua ma trận F1-10.
**Concerns/Blockers:** Không có blocker. Một điểm cần DD quyết định cụ thể (không phải câu hỏi mở của RD):
đơn vị trục thời gian của 2 widget ("Lượt nộp theo ngôn ngữ" 20 mốc, khung so sánh delta) — đã ghi rõ ở mục 8
để `03-dd/logic/identity.md` xử lý, không chặn việc chấp nhận BD này.
