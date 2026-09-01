# RD — Màn `auth` (Đăng nhập & Đăng ký)

> Slug: `auth` — khớp `01-rd/overview/system_survey.md` mục 7.1 dòng `auth`
> [SoT: 01-rd/overview/system_survey.md:469]. Bounded Context: `identity` (F1)
> [SoT: .nexa/domain-registry.json]. Actor: A1, A2, A3 (mọi vai trò đều đi qua màn này trước khi vào hệ
> thống) [SoT: 01-rd/overview/system_survey.md:465-482 — auth nằm ở mục 7.1 "Khu vực người học" nhưng không
> có gate theo vai trò; A2/A3 dùng chung màn này để đăng nhập, chỉ khác điểm đến sau khi xác thực].
>
> Đối chiếu prototype đã sửa sạch quy chuẩn (nhãn go-judge, khoá 3 ngôn ngữ, theme Light mặc định —
> `06-plan/PROTOTYPE_DEBT.md` mục 6.1): `09-layoutBase/Đăng nhập & Đăng ký.dc.html`.
>
> File này mô tả **hành vi và UX ở mức yêu cầu** của một màn cụ thể — không lặp lại đặc tả chức năng đã có
> ở `01-rd/req/identity.md` (mục F1) hay `01-rd/req/user_stories/a1_student.md` (`US-A1-01`), chỉ trỏ tới và bổ sung phần
> đặc thù của **một màn**: trạng thái màn, luồng chuyển màn, và các câu hỏi mở phát sinh khi đối chiếu với
> prototype thật mà bản mô tả chức năng chung chưa có.

## 1. Mục đích màn hình

Là điểm vào duy nhất của hệ thống cho cả ba vai trò (`STUDENT`, `INSTRUCTOR`, `ADMIN`) — đăng ký tài khoản
mới hoặc đăng nhập vào tài khoản đã có, bằng email/mật khẩu hoặc qua OAuth (GitHub, Google)
[SoT: 01-rd/req/identity.md — F1-01, F1-02, F1-15].

## 2. Nguồn yêu cầu (không lặp lại — chỉ trỏ)

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Đăng ký email + mật khẩu, vai trò mặc định `STUDENT` | F1-01, F1-05 | `01-rd/req/identity.md` — F1-01, F1-05 |
| Đăng nhập cấp Access Token + Refresh Token (cookie HTTP-Only) | F1-02 | `01-rd/req/identity.md` — F1-02 |
| Client tự làm mới Access Token khi gặp `401` | F1-03 | `01-rd/req/identity.md` — F1-03 |
| Đăng nhập/đăng ký qua OAuth (GitHub, Google), tự động liên kết theo email trùng | F1-15 | `01-rd/req/identity.md` — F1-15 |
| Tự đặt lại mật khẩu bằng mã 6 chữ số gửi qua email (Gmail), một lần dùng, có hạn hiệu lực | F1-17 | `01-rd/req/identity.md` — F1-17 |
| Given-When-Then đầy đủ cho các hành vi trên | — | `01-rd/req/user_stories/a1_student.md` (`US-A1-01`) |

## 3. Trạng thái màn (screen states)

Đối chiếu `09-layoutBase/Đăng nhập & Đăng ký.dc.html` — đây là hành vi UX thật đã dựng, không phải suy
diễn [SoT: 09-layoutBase/Đăng nhập & Đăng ký.dc.html:186, 216-279]:

1. **`signup`** (mặc định khi vào lần đầu) — form Tên đăng nhập, Mật khẩu, E-mail, ô đồng ý điều khoản sử
   dụng, nút "Đăng ký", cụm nút OAuth (Google, GitHub) — dòng 236-238, 246.
2. **`login`** — form E-mail/Tên đăng nhập, Mật khẩu, ô "Ghi nhớ đăng nhập", liên kết "Quên mật khẩu?", nút
   "Đăng nhập", cụm nút OAuth — dòng 121-129, 236-238.
3. **Chuyển đổi `signup` ↔ `login`** qua nút phụ ở panel bên phải, không rời khỏi màn (không có URL riêng
   cho từng chế độ trong prototype) — dòng 159, 272.
4. **`loading`** — overlay toàn màn hình sau khi bấm nộp, hiện 4 bước tuần tự (Xác thực thông tin → Tải
   tiến độ luyện tập → Kết nối go-judge → Dựng bảng tổng quan) rồi điều hướng sang màn `my_progress`
   (`Dashboard AlgoPrep.dc.html`) — dòng 191-197, 254-258.
5. **Trạng thái lỗi** — **chưa có trong prototype** (`fakeAuth()` luôn thành công, dòng 191-197) — xem Câu
   hỏi mở Q1.
6. **`forgot_email`** (mới, chốt 2026-08-24 qua hỏi trực tiếp chủ dự án — F1-17, chưa có trong prototype)
   — mở từ liên kết "Quên mật khẩu?" ở chế độ `login` (dòng 127); form một trường email, nút gửi. Sau khi
   gửi, màn luôn hiện cùng một thông báo chung "nếu email tồn tại, hướng dẫn đã được gửi" bất kể tài khoản
   có tồn tại hay không (không lộ thông tin) — nhưng **nội dung email thực nhận khác nhau theo loại tài
   khoản**: tài khoản có mật khẩu (đăng ký thường hoặc OAuth-liên-kết) nhận mã 6 số; tài khoản chỉ đăng ký
   qua OAuth, chưa từng đặt mật khẩu, nhận email hướng dẫn quay lại đăng nhập bằng đúng provider cũ, **không
   nhận mã 6 số** (F1-17).
7. **`forgot_otp`** (mới) — nhập mã 6 chữ số vừa nhận qua email, có đếm ngược thời hạn hiệu lực và nút "Gửi
   lại mã" (giới hạn tần suất — F1-17). Sai quá số lần cho phép hoặc hết hạn → báo lỗi, buộc gửi lại mã mới.
8. **`forgot_reset`** (mới) — sau khi mã hợp lệ, form đặt mật khẩu mới (giống ràng buộc độ dài mật khẩu ở
   `signup`); xong thì quay về `login` để đăng nhập bằng mật khẩu mới, không tự động đăng nhập.

Hai chế độ hiển thị **không đổi hành vi nghiệp vụ**, chỉ đổi văn bản/field: `data-ui-lang="vi|en"` (i18n,
`DEC-2026-0824-i18n-vi-en`) và `data-theme="light|dark"` (theme, `DEC-2026-0824-dark-light-theme`, Light
là mặc định — khớp `state.theme: "light"` dòng 186).

## 4. Given-When-Then bổ sung ở mức màn (không trùng `US-A1-01`)

`US-A1-01` đã có đủ Given-When-Then cho đăng ký, đăng nhập, làm mới token, đăng xuất, và OAuth
[SoT: 01-rd/req/user_stories/a1_student.md — US-A1-01]. Các mục dưới đây là hành vi **riêng của màn hình** mà mô tả chức
năng ở mức module chưa nêu, phát hiện khi đối chiếu prototype:

- **Cho** tôi đang ở chế độ `signup`, **Khi** tôi bấm liên kết chuyển chế độ, **Thì** màn chuyển sang
  `login` tại chỗ, không tải lại trang và không mất trạng thái theme/ngôn ngữ đã chọn
  [SoT: 09-layoutBase/Đăng nhập & Đăng ký.dc.html:159, 272].
- **Cho** tài khoản của tôi đang ở trạng thái `DEACTIVATED` (đã tự xoá, còn trong khoảng ân hạn — F1-16),
  **Khi** tôi thử đăng nhập bằng email/mật khẩu hoặc OAuth, **Thì** hệ thống từ chối đăng nhập và báo rõ lý
  do (không phải sai mật khẩu) [SoT: 01-rd/req/identity.md — F1-16; suy ra từ việc F1-16 nói "không đăng
  nhập lại được" nhưng không mô tả màn `auth` phản ứng thế nào — `[SoT: Suy luận]`, xem Câu hỏi mở Q2].
- **Cho** đăng nhập/đăng ký thành công, **Khi** overlay tải xong 4 bước, **Thì** hệ thống điều hướng tới
  màn tiến độ cá nhân (`my_progress`) cho `STUDENT`, không phải một màn trung chuyển khác — hành vi hiện tại
  của prototype giống nhau cho mọi vai trò [SoT: 09-layoutBase/Đăng nhập & Đăng ký.dc.html:196; xem Câu hỏi
  mở Q3 — `INSTRUCTOR`/`ADMIN` có nên vào thẳng khu vực riêng của mình thay vì `my_progress` không].

## 5. Câu hỏi mở (chưa trả lời — không tự chọn thay)

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | ~~Màn `auth` xử lý lỗi đăng nhập/đăng ký thế nào...~~ **ĐÃ CHỐT (phiên 2026-08-25, chốt theo RD — prototype chỉ tham khảo, sẽ dựng lại khi làm FE Next.js thật):** thêm trạng thái lỗi inline dưới từng field bị sai (không dùng modal/toast che form), dùng token `--bad` nhất quán với theme. Áp dụng cho cả bốn tình huống: sai mật khẩu, email đã tồn tại, mật khẩu yếu, OAuth thất bại (thông báo riêng cho từng loại, không gộp chung một câu). **Chưa dựng vào `09-layoutBase/Đăng nhập & Đăng ký.dc.html`** — `fakeAuth()` vẫn luôn giả lập thành công, việc dựng UI lỗi thật để lúc build FE. | — | Đã chốt quyết định, chưa dựng prototype. | Đã đóng |
| Q2 | ~~Thông báo khi tài khoản `DEACTIVATED`...~~ **ĐÃ CHỐT (phiên 2026-08-25, chốt theo RD):** cho phép khôi phục ngay tại màn `auth` nếu còn trong khoảng ân hạn — một hành động "Huỷ yêu cầu xoá tài khoản" xuất hiện khi hệ thống phát hiện đăng nhập đúng mật khẩu/OAuth vào tài khoản đang `DEACTIVATED`, tận dụng đúng lúc người dùng đã quay lại thay vì bắt liên hệ hỗ trợ. **Chưa dựng vào prototype** — để lúc build FE. | — | Đã chốt quyết định, chưa dựng prototype. | Đã đóng |
| Q3 | ~~Sau khi `INSTRUCTOR`/`ADMIN` đăng nhập, có vào thẳng khu vực riêng...~~ **ĐÃ CHỐT (phiên 2026-08-25, chốt theo RD):** điều hướng theo vai trò — `STUDENT` → `my_progress`, `INSTRUCTOR` → `instructor_overview`, `ADMIN` → `admin_overview`; nếu người dùng đến `auth` từ một liên kết cụ thể cần đăng nhập trước (ví dụ bài toán được chia sẻ), ưu tiên quay lại đúng URL gốc đó thay vì đích mặc định theo vai trò. **Chưa dựng vào prototype** (hiện luôn điều hướng 1 đích `my_progress`) — để lúc build FE. **CẢNH BÁO 2026-08-25 (đã xử lý): `admin_overview` nay đã tồn tại** — `01-rd/screens/admin/admin_overview.md` được bổ sung 2026-08-25, đích `ADMIN` không còn treo. Cần đối chiếu lại code (`05-coding/frontend/src/entities/user/model/area.ts` → `HOME_PATH_BY_ROLE`) trỏ đúng `/admin/overview` khi build FE thật thay vì `/admin/queue` tạm thời. | — | Đã chốt quyết định, đã dựng đủ prototype. | Đã đóng |
| Q4 | ~~"Quên mật khẩu?" xuất hiện ở chế độ `login` trong prototype nhưng không có mã `Fx-nn` nào mô tả luồng đặt lại mật khẩu qua email.~~ **Đã trả lời 2026-08-24 — qua hỏi trực tiếp chủ dự án:** có tính năng quên mật khẩu, gửi mã 6 chữ số ngẫu nhiên qua Gmail. Đã ghi `F1-17` vào `01-rd/req/identity.md`, đồng bộ `system_survey.md` mục 5.1/5.7, thêm 3 Given-When-Then vào `US-A1-01` (`user_stories/a1_student.md`), và thêm 3 trạng thái màn mới ở mục 3 (`forgot_email`, `forgot_otp`, `forgot_reset`) — chưa có trong prototype, cần dựng thêm khi làm UI thật/BD. | — | — | (đã đóng) |
| Q4b | ~~Tài khoản chỉ từng đăng ký qua OAuth (chưa bao giờ đặt mật khẩu, F1-15) bấm "Quên mật khẩu" thì xử lý thế nào?~~ **Đã trả lời 2026-08-24 — qua hỏi trực tiếp chủ dự án:** đúng như chủ dự án chỉ ra — tài khoản chưa từng có mật khẩu thì không có gì để "quên", nên **từ chối tạo mật khẩu mới qua luồng này**; hệ thống gửi email báo tài khoản đang đăng nhập bằng GitHub/Google, hướng dẫn quay lại đúng provider đó. Đã ghi vào `identity.md` (mục F1-17), thêm Given-When-Then vào `US-A1-01`. | — | — | (đã đóng) |

## 6. Ngoài phạm vi file này

- Layout, vùng bố cục, bảng màu, component cụ thể — thuộc BD (`02-bd/screens/shared/auth.md`, chưa viết).
- Hợp đồng API (request/response của đăng nhập/đăng ký/OAuth) — thuộc DD (`03-dd/api/identity.md`,
  chưa viết).
- Cơ chế JWT, luồng OAuth chi tiết (redirect URI, PKCE...), bảo mật cookie — thuộc BD/DD của module
  `identity`, không thuộc file theo trục màn này.

## 7. Tham chiếu

- `01-rd/req/identity.md` — F1-01 tới F1-17.
- `01-rd/req/user_stories/a1_student.md` — `US-A1-01`.
- `01-rd/overview/system_survey.md:469` — dòng `auth` trong bảng màn mục 7.1.
- `09-layoutBase/Đăng nhập & Đăng ký.dc.html` — prototype đã sửa sạch (`06-plan/PROTOTYPE_DEBT.md` mục 6.1).
- `.nexa/control/decision-registry.md` — `DEC-2026-0824-dark-light-theme`, `DEC-2026-0824-i18n-vi-en`.
- `06-plan/nexa-plan/260824-2043-bd-screens-common-first.md` — kế hoạch Phase 0 sinh ra file này.
