# RD (Yêu cầu hệ thống mới) — Đăng nhập và đăng ký / `SHR0101`

> Mã màn hình: `SHR0101` [SoT: 02-bd/_rules/bd-template-9sheet.md — mục 8].
> Slug chính tắc: `auth` [SoT: 01-rd/overview/system_survey.md — mục 7.0 dòng `auth`].
> Bounded Context: `identity` (F1) [SoT: README.md mục 4 — sáu phân hệ F1-F6]. Actor: A1, A2, A3 (mọi vai trò đều đi qua màn này trước khi vào hệ thống).
> Nguồn sự thật (SoT): `01-rd/req/identity.md` (F1-01, F1-02, F1-15, F1-17), `09-layoutBase/Đăng nhập & Đăng ký.dc.html` (prototype), `01-rd/req/user_stories/a1_student.md` (`US-A1-01`).
> Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

Là điểm vào duy nhất của hệ thống cho cả ba vai trò (`STUDENT`, `INSTRUCTOR`, `ADMIN`) — đăng ký tài khoản
mới hoặc đăng nhập vào tài khoản đã có, bằng email/mật khẩu hoặc qua OAuth (GitHub, Google)
[SoT: 01-rd/req/identity.md — F1-01, F1-02, F1-15].

Đối chiếu prototype đã sửa sạch quy chuẩn (nhãn go-judge, khoá 3 ngôn ngữ, theme Light mặc định —
`06-plan/PROTOTYPE_DEBT.md` mục 6.1): `09-layoutBase/Đăng nhập & Đăng ký.dc.html`.

File này mô tả **hành vi và UX ở mức yêu cầu** của một màn cụ thể — không lặp lại đặc tả chức năng đã có
ở `01-rd/req/identity.md` (mục F1) hay `01-rd/req/user_stories/a1_student.md` (`US-A1-01`), chỉ trỏ tới và bổ sung phần
đặc thù của **một màn**: trạng thái màn, luồng chuyển màn, và các câu hỏi mở phát sinh khi đối chiếu với
prototype thật mà bản mô tả chức năng chung chưa có.

---

## 2. Phạm vi

| Hành vi | Mã | Nguồn |
| :--- | :--- | :--- |
| Đăng ký email + mật khẩu, vai trò mặc định `STUDENT` | F1-01, F1-05 | `01-rd/req/identity.md` — F1-01, F1-05 |
| Đăng nhập cấp Access Token + Refresh Token (cookie HTTP-Only) | F1-02 | `01-rd/req/identity.md` — F1-02 |
| Client tự làm mới Access Token khi gặp `401` | F1-03 | `01-rd/req/identity.md` — F1-03 |
| Đăng nhập/đăng ký qua OAuth (GitHub, Google), tự động liên kết theo email trùng | F1-15 | `01-rd/req/identity.md` — F1-15 |
| Tự đặt lại mật khẩu bằng mã 6 chữ số gửi qua email (Gmail), một lần dùng, có hạn hiệu lực | F1-17 | `01-rd/req/identity.md` — F1-17 |
| Given-When-Then đầy đủ cho các hành vi trên | — | `01-rd/req/user_stories/a1_student.md` (`US-A1-01`) |

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

### 2.1 Cấu trúc hệ thống liên quan

| Màn/tài liệu | Vai trò | Bounded Context |
|---|---|---|
| Màn hiện tại | `SHR0101` / `auth` | `identity` |
| Tài liệu yêu cầu | `01-rd/req/identity.md` (F1) | Nguồn SoT của màn |
| Prototype | `09-layoutBase/Đăng nhập & Đăng ký.dc.html` | Bằng chứng bố cục và trạng thái |

---

## 3. Ngoài phạm vi (Out of Scope)

- Layout, vùng bố cục, bảng màu, component cụ thể — thuộc BD (`02-bd/screens/shared/SHR0101_auth.md`).
- Hợp đồng API (request/response của đăng nhập/đăng ký/OAuth) — thuộc DD (`03-dd/api/identity.md`,
  chưa viết).
- Cơ chế JWT, luồng OAuth chi tiết (redirect URI, PKCE...), bảo mật cookie — thuộc BD/DD của module
  `identity`, không thuộc file theo trục màn này.

---

## 4. Tiền đề và ràng buộc

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1 | ~~Màn `auth` xử lý lỗi đăng nhập/đăng ký thế nào...~~ **ĐÃ CHỐT (phiên 2026-08-25, chốt theo RD — prototype chỉ tham khảo, sẽ dựng lại khi làm FE Next.js thật):** thêm trạng thái lỗi inline dưới từng field bị sai (không dùng modal/toast che form), dùng token `--bad` nhất quán với theme. Áp dụng cho cả bốn tình huống: sai mật khẩu, email đã tồn tại, mật khẩu yếu, OAuth thất bại (thông báo riêng cho từng loại, không gộp chung một câu). **Chưa dựng vào `09-layoutBase/Đăng nhập & Đăng ký.dc.html`** — `fakeAuth()` vẫn luôn giả lập thành công, việc dựng UI lỗi thật để lúc build FE. | — | Đã chốt quyết định, chưa dựng prototype. | Đã đóng |
| Q2 | ~~Thông báo khi tài khoản `DEACTIVATED`...~~ **ĐÃ CHỐT (phiên 2026-08-25, chốt theo RD):** cho phép khôi phục ngay tại màn `auth` nếu còn trong khoảng ân hạn — một hành động "Huỷ yêu cầu xoá tài khoản" xuất hiện khi hệ thống phát hiện đăng nhập đúng mật khẩu/OAuth vào tài khoản đang `DEACTIVATED`, tận dụng đúng lúc người dùng đã quay lại thay vì bắt liên hệ hỗ trợ. **Chưa dựng vào prototype** — để lúc build FE. | — | Đã chốt quyết định, chưa dựng prototype. | Đã đóng |
| Q3 | ~~Sau khi `INSTRUCTOR`/`ADMIN` đăng nhập, có vào thẳng khu vực riêng...~~ **ĐÃ CHỐT (phiên 2026-08-25, chốt theo RD):** điều hướng theo vai trò — `STUDENT` → `my_progress`, `INSTRUCTOR` → `instructor_overview`, `ADMIN` → `admin_overview`; nếu người dùng đến `auth` từ một liên kết cụ thể cần đăng nhập trước (ví dụ bài toán được chia sẻ), ưu tiên quay lại đúng URL gốc đó thay vì đích mặc định theo vai trò. **Chưa dựng vào prototype** (hiện luôn điều hướng 1 đích `my_progress`) — để lúc build FE. **CẢNH BÁO 2026-08-25 (đã xử lý): `admin_overview` nay đã tồn tại** — `01-rd/screens/admin/ADM0101_overview.md` được bổ sung 2026-08-25, đích `ADMIN` không còn treo. Cần đối chiếu lại code (`05-coding/frontend/src/entities/user/model/area.ts` → `HOME_PATH_BY_ROLE`) trỏ đúng `/admin/overview` khi build FE thật thay vì `/admin/queue` tạm thời. | — | Đã chốt quyết định, đã dựng đủ prototype. | Đã đóng |
| Q4 | ~~"Quên mật khẩu?" xuất hiện ở chế độ `login` trong prototype nhưng không có mã `Fx-nn` nào mô tả luồng đặt lại mật khẩu qua email.~~ **Đã trả lời 2026-08-24 — qua hỏi trực tiếp chủ dự án:** có tính năng quên mật khẩu, gửi mã 6 chữ số ngẫu nhiên qua Gmail. Đã ghi `F1-17` vào `01-rd/req/identity.md`, đồng bộ `system_survey.md` mục 5.1/5.7, thêm 3 Given-When-Then vào `US-A1-01` (`user_stories/a1_student.md`), và thêm 3 trạng thái màn mới ở mục 3 (`forgot_email`, `forgot_otp`, `forgot_reset`) — chưa có trong prototype, cần dựng thêm khi làm UI thật/BD. | — | — | (đã đóng) |
| Q4b | ~~Tài khoản chỉ từng đăng ký qua OAuth (chưa bao giờ đặt mật khẩu, F1-15) bấm "Quên mật khẩu" thì xử lý thế nào?~~ **Đã trả lời 2026-08-24 — qua hỏi trực tiếp chủ dự án:** đúng như chủ dự án chỉ ra — tài khoản chưa từng có mật khẩu thì không có gì để "quên", nên **từ chối tạo mật khẩu mới qua luồng này**; hệ thống gửi email báo tài khoản đang đăng nhập bằng GitHub/Google, hướng dẫn quay lại đúng provider đó. Đã ghi vào `identity.md` (mục F1-17), thêm Given-When-Then vào `US-A1-01`. | — | — | (đã đóng) |

---

## 5. Danh sách yêu cầu (REQ)
| ID | Yêu cầu | Loại | Nguồn/SoT |
|---|---|---|---|
| REQ-01 | Đăng ký email + mật khẩu, vai trò mặc định `STUDENT` (F1-01, F1-05) | Chức năng | `01-rd/req/identity.md` — F1-01, F1-05 |
| REQ-02 | Đăng nhập cấp Access Token + Refresh Token (cookie HTTP-Only) (F1-02) | Chức năng | `01-rd/req/identity.md` — F1-02 |
| REQ-03 | Client tự làm mới Access Token khi gặp `401` (F1-03) | Chức năng | `01-rd/req/identity.md` — F1-03 |
| REQ-04 | Đăng nhập/đăng ký qua OAuth (GitHub, Google), tự động liên kết theo email trùng (F1-15) | Chức năng | `01-rd/req/identity.md` — F1-15 |
| REQ-05 | Tự đặt lại mật khẩu bằng mã 6 chữ số gửi qua email (Gmail), một lần dùng, có hạn hiệu lực (F1-17) | Chức năng | `01-rd/req/identity.md` — F1-17 |
| REQ-06 | Given-When-Then đầy đủ cho các hành vi trên (—) | Chức năng | `01-rd/req/user_stories/a1_student.md` (`US-A1-01`) |

---

## 6. Yêu cầu dữ liệu

### 6.1 Dữ liệu tham chiếu

Chi tiết bảng, cột, kiểu dữ liệu và ràng buộc lưu trữ thuộc BD/DD; tài liệu này không tự suy luận dữ liệu chưa được chốt.

### 6.2 Luồng dữ liệu

Hợp đồng API, request/response và mã lỗi thuộc DD. Luồng dữ liệu mức yêu cầu được thể hiện bởi các hành vi ở chương 5.

---

## 7. RACI và danh sách bàn giao

| Bàn giao | Trạng thái |
|---|---|
| RD màn hình | Có tài liệu này |
| BD màn hình | Chưa xác định — không tự suy luận |
| DD/API liên quan | Chưa xác định — không tự suy luận |

---

## 8. Kế hoạch và quy mô

| Chỉ số | Giá trị |
|---|---|
| Quy mô hành vi | Xem các yêu cầu tại chương 5. |
| Lịch, nhân sự, công sức | Chưa xác định — không tự suy luận. |

---

## 9. Tham chiếu

| Loại | Tham chiếu |
|---|---|
| Yêu cầu module | `01-rd/req/identity.md` — F1-01 tới F1-17. |
| User story | `01-rd/req/user_stories/a1_student.md` — `US-A1-01`. |
| Khảo sát hệ thống | `01-rd/overview/system_survey.md` — mục 7.0 dòng `auth`. |
| Prototype | `09-layoutBase/Đăng nhập & Đăng ký.dc.html` — prototype đã sửa sạch (`06-plan/PROTOTYPE_DEBT.md` mục 6.1). |
| Quyết định | `DEC-2026-0824-dark-light-theme`; `DEC-2026-0824-i18n-vi-en`. |
| Kế hoạch | `06-plan/nexa-plan/260824-2043-bd-screens-common-first.md` — kế hoạch Phase 0 sinh ra file này. |
