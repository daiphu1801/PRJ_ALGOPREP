# BD — Màn `auth` (Đăng nhập & Đăng ký)

> Trục màn hình (`shared` — dùng chung, không thuộc riêng actor nào). Slug khớp
> `01-rd/screens/shared/auth.md` [SoT: 01-rd/screens/shared/auth.md:1-6]. Bounded Context chủ quản:
> `identity` (F1) — kiến trúc/DB/security chi tiết ở `02-bd/architecture/identity.md`,
> `02-bd/database/identity.md`, `02-bd/security/identity.md`, không lặp lại ở đây (anti-drift, skill
> `bd-generation` Layer 3 mục "Must Not").

## 1. Layout regions

Đối chiếu `09-layoutBase/Đăng nhập & Đăng ký.dc.html` (SoT layout, chỉ-đọc):

| Vùng | Mô tả cấu trúc | Dòng nguồn |
| :--- | :--- | :--- |
| **Khung ngoài** | Toàn màn hình căn giữa, thẻ (card) bố cục 2 cột (`grid-template-columns: 1.06fr 1fr`), bo góc lớn, đổ bóng | dòng 48, 89 |
| **Cột trái — form panel** | Logo + nhãn engine, tiêu đề chế độ (eyebrow + heading + subheading), khối field động theo chế độ, khối phụ theo chế độ (điều khoản/ghi nhớ+quên mật khẩu), nút submit chính, khối OAuth (divider "hoặc" + 2 nút Google/GitHub) | dòng 91-146 |
| **Cột phải — aside panel** | Nền màu accent, badge ngang (3 badge: số bài, số chủ đề, số ngôn ngữ), tiêu đề + mô tả đổi theo chế độ, nút chuyển chế độ (switch), khối minh hoạ code mẫu tĩnh, dải chấm điều hướng (dots) | dòng 149-179 |
| **Thanh góc phải trên** | Công tắc theme (Sáng/Tối) và ngôn ngữ (VI/EN), nổi cố định (`position: fixed`) | dòng 80-87 |
| **Overlay loading** | Toàn màn hình, che khung chính khi `loading = true`: logo, tiêu đề, thanh tiến trình, danh sách 4 bước tuần tự có trạng thái | dòng 50-77 |

Hai cột chỉ đổi **nội dung field/văn bản** theo `mode` (`signup`/`login`), không đổi cấu trúc khung —
khớp mục 3 RD "chuyển đổi tại chỗ, không có URL riêng" [SoT: 01-rd/screens/shared/auth.md:43].

## 2. Component inventory

| Component | Thuộc vùng | Ghi chú hành vi |
| :--- | :--- | :--- |
| `AuthCard` | Khung ngoài | Container bố cục 2 cột, không mang state nghiệp vụ |
| `ModeSwitchTabs` (ẩn/implicit) | — | Không phải tab hiển thị — chuyển chế độ qua nút ở aside panel (`switchMode`), không phải thanh tab riêng [SoT: 09-layoutBase/Đăng nhập & Đăng ký.dc.html:159] |
| `AuthFormHeader` | Cột trái | Eyebrow + `formTitle` + `formSub`, đổi theo `mode` |
| `DynamicFieldList` | Cột trái | `sc-for` trên `fields`: `signup` = Tên đăng nhập, Mật khẩu (có nút hiện/ẩn), E-mail; `login` = E-mail/Tên đăng nhập, Mật khẩu — dòng 102, 236-238 |
| `PasswordRevealToggle` | Trong field Mật khẩu | Nút HIỆN/ẨN đổi `type="password"↔"text"` — dòng 107-109 |
| `TermsCheckbox` | Cột trái, chỉ khi `isSignup` | Ô đồng ý điều khoản — bắt buộc tick mới submit được (ràng buộc suy ra từ tồn tại toggle, chưa có Given-When-Then xác nhận validate ở prototype — `[SoT: Suy luận]`, xem Câu hỏi mở Q1) — dòng 114-119 |
| `RememberMeCheckbox` | Cột trái, chỉ khi `isLogin` | Ghi nhớ đăng nhập — dòng 121-126 |
| `ForgotPasswordLink` | Cột trái, chỉ khi `isLogin` | Mở màn con `forgot_email` [SoT: 01-rd/screens/shared/auth.md mục 3.6] — dòng 127 |
| `SubmitButton` | Cột trái | Nhãn đổi "Đăng ký"/"Đăng nhập" theo `mode` — dòng 131 |
| `OAuthButtonGroup` (Google, GitHub) | Cột trái, dưới cùng | 2 nút, cùng hành động submit ở prototype (thực tế phải redirect OAuth riêng — xem mục 4) — dòng 141-144 |
| `AsideModePanel` | Cột phải | Badge hàng ngang, tiêu đề/mô tả đổi theo `mode`, nút `switchMode`, khối minh hoạ code tĩnh, dots | dòng 149-179 |
| `ThemeLangSwitcher` | Góc phải trên | 2 cụm nút, không phải component riêng của màn `auth` — dùng chung mọi màn (`DEC-2026-0824-dark-light-theme`, `DEC-2026-0824-i18n-vi-en`) | dòng 80-87 |
| `AuthLoadingOverlay` | Overlay | Danh sách 4 bước tuần tự, mỗi bước có `mark` (`·`/`→`/dấu tick) và `meta` — dòng 50-77, 254-262 |
| `InlineFieldError` (mới, chưa dựng trong prototype) | Dưới mỗi field | Hiển thị lỗi theo field, không dùng modal/toast — theo quyết định Q1 đã chốt ở RD [SoT: 01-rd/screens/shared/auth.md:87] |
| `DeactivatedAccountBanner` (mới, chưa dựng trong prototype) | Trên cùng form, khi phát hiện tài khoản `DEACTIVATED` còn ân hạn | Thông báo + nút "Huỷ yêu cầu xoá tài khoản" [SoT: 01-rd/screens/shared/auth.md:88] |
| `OtpInputGroup` (mới, cho `forgot_otp`) | Màn con quên mật khẩu | 6 ô nhập số, đếm ngược hạn hiệu lực, nút "Gửi lại mã" có cooldown [SoT: 01-rd/screens/shared/auth.md mục 3.7; `02-bd/security/identity.md` mục 3 — cooldown 60 giây, tối đa 5 lần sai] |

## 3. Screen states

Kế thừa mục 3 RD [SoT: 01-rd/screens/shared/auth.md:33-63], BD chốt state machine hiển thị (không phải
state quản lý bởi backend — đó là DD):

```
signup (mặc định) ⇄ login
   │ (submit thành công)          │ (bấm "Quên mật khẩu?")
   ▼                              ▼
loading (overlay, 4 bước)     forgot_email
   │ (hết bước 4)                 │ (gửi email)
   ▼                              ▼
điều hướng theo vai trò        forgot_otp
(mục 5)                           │ (mã hợp lệ)
                                  ▼
                              forgot_reset
                                  │ (đặt xong)
                                  ▼
                                login
```

| State | Điều kiện vào | Điều kiện thoát | Ghi chú |
| :--- | :--- | :--- | :--- |
| `signup` | Vào màn lần đầu, hoặc bấm chuyển từ `login` | Submit hợp lệ → `loading`; bấm chuyển chế độ → `login` | Mặc định — dòng 186, 200 |
| `login` | Bấm chuyển từ `signup`, hoặc điều hướng trực tiếp có tham số chế độ | Submit hợp lệ → `loading`; bấm "Quên mật khẩu?" → `forgot_email` | dòng 200 |
| `error-inline` (không phải state riêng, là biến thể của `signup`/`login`) | Submit thất bại (sai mật khẩu / email đã tồn tại / mật khẩu yếu / OAuth thất bại) | Sửa field → xoá lỗi tương ứng | Q1 đã chốt: lỗi hiện dưới từng field, không che form [SoT: 01-rd/screens/shared/auth.md:87] |
| `deactivated-recovery` (biến thể của `login`) | Đăng nhập đúng thông tin nhưng tài khoản `status = DEACTIVATED` còn trong ân hạn 30 ngày [SoT: 02-bd/architecture/identity.md mục 5 — dòng "Ân hạn... 30 ngày"] | Bấm "Huỷ yêu cầu xoá tài khoản" → đăng nhập tiếp bình thường; không bấm → ở lại `login` | Q2 đã chốt [SoT: 01-rd/screens/shared/auth.md:88] |
| `loading` | Submit thành công (email/mật khẩu hoặc OAuth callback) | 4 bước hoàn tất → điều hướng | Không phải trạng thái chờ mạng đơn thuần — dựng 4 bước cố định theo UX đã thiết kế, dòng 191-197 |
| `forgot_email` | Bấm "Quên mật khẩu?" từ `login` | Gửi email → `forgot_otp` (luôn, kể cả email không tồn tại — không tiết lộ) | [SoT: 01-rd/screens/shared/auth.md:50-55] |
| `forgot_otp` | Sau `forgot_email` | Mã đúng → `forgot_reset`; hết hạn/sai quá số lần → buộc "Gửi lại mã" | Đếm ngược + rate limit — [SoT: 02-bd/security/identity.md mục 3] |
| `forgot_reset` | Mã OTP hợp lệ | Đặt mật khẩu xong → `login` (không tự đăng nhập) | [SoT: 01-rd/screens/shared/auth.md:58-59] |

Hai chế độ hiển thị `data-theme` và `data-ui-lang` là thuộc tính xuyên suốt mọi state, không phải state
riêng [SoT: 01-rd/screens/shared/auth.md:61-63].

## 4. APIs consumed (chỉ liệt kê tên — hợp đồng chốt ở `03-dd/api/identity.md`)

Tất cả thuộc Bounded Context `identity`:

| Hành động trên màn | Tên nghiệp vụ endpoint (DD sẽ đặt tên/route cụ thể) | Mã RD |
| :--- | :--- | :--- |
| Submit `signup` | Đăng ký tài khoản email/mật khẩu | F1-01, F1-05 |
| Submit `login` | Đăng nhập, cấp Access Token + Refresh Token (cookie HTTP-Only) | F1-02 |
| Làm mới token ngầm khi `401` | Refresh access token | F1-03 |
| Bấm nút Google/GitHub | Bắt đầu luồng OAuth (redirect tới provider) | F1-15 |
| Provider redirect về sau xác thực | Callback OAuth, tự liên kết theo email đã verify | F1-15 |
| Gửi form `forgot_email` | Yêu cầu đặt lại mật khẩu (gửi OTP hoặc email hướng dẫn provider) | F1-17 |
| Gửi mã ở `forgot_otp` | Xác thực OTP | F1-17 |
| Đặt mật khẩu mới ở `forgot_reset` | Đặt lại mật khẩu bằng OTP đã xác thực | F1-17 |
| Bấm "Huỷ yêu cầu xoá tài khoản" ở `deactivated-recovery` | Khôi phục tài khoản đang `DEACTIVATED` còn ân hạn | F1-16 (RD Q2) |

Không liệt kê request/response, mã lỗi HTTP cụ thể tại đây — thuộc `03-dd/api/identity.md`
(anti-drift rule 1, skill `bd-generation` Layer 2).

## 5. Navigation

| Từ | Đến | Điều kiện |
| :--- | :--- | :--- |
| Bất kỳ điểm vào chưa xác thực nào của hệ thống | `auth` (`signup`) | Không có Access Token hợp lệ |
| `auth` (`loading` xong) | `my_progress` | Vai trò `STUDENT` [SoT: 01-rd/screens/shared/auth.md:78-81, mục Q3 đã chốt] |
| `auth` (`loading` xong) | `instructor_overview` | Vai trò `INSTRUCTOR` (Q3) |
| `auth` (`loading` xong) | `admin_overview` | Vai trò `ADMIN` (Q3) — màn đích đã tồn tại [SoT: 01-rd/screens/admin/admin_overview.md] |
| `auth` (`loading` xong), có URL gốc lưu trước khi bị redirect vào `auth` | URL gốc đó (ví dụ bài toán được chia sẻ) | Có tham số `redirect`/`returnTo` — ưu tiên hơn đích mặc định theo vai trò (Q3) |
| `signup` | `login` | Bấm nút chuyển chế độ ở aside panel |
| `login` | `signup` | Bấm nút chuyển chế độ ở aside panel |
| `login` | `forgot_email` | Bấm "Quên mật khẩu?" |
| `forgot_email` → `forgot_otp` → `forgot_reset` | `login` | Hoàn tất đặt lại mật khẩu |
| Bất kỳ màn nào, Access Token hết hạn và refresh cũng thất bại (refresh-token reuse bị revoke cả family) | `auth` (`login`) | [SoT: 02-bd/security/identity.md mục 1 — "buộc đăng nhập lại"] |

## 6. Access rights (ai vào được màn này — không phải "vào được làm gì")

- Màn `auth` **không yêu cầu đăng nhập** — là điểm vào công khai cho mọi actor A1/A2/A3
  [SoT: 01-rd/screens/shared/auth.md:5, 18].
- Người dùng **đã có Access Token hợp lệ** truy cập lại `auth` (ví dụ gõ URL trực tiếp): BD đề xuất
  redirect thẳng về đích theo vai trò (mục 5) thay vì hiển thị lại form đăng nhập — tránh đăng nhập lại
  không cần thiết `[SoT: Suy luận]` — RD không nêu rõ hành vi này, cần xác nhận (Câu hỏi mở Q5).
- Tài khoản `status = DEACTIVATED` **được phép** thử đăng nhập tại đây (khác với các module nghiệp vụ
  khác nơi token bị coi hết hiệu lực ngay) — vì đây chính là nơi cho phép khôi phục
  [SoT: 02-bd/security/identity.md mục 4; 01-rd/screens/shared/auth.md:88].
- Không có phân biệt actor ở tầng hiển thị màn `auth` — RBAC theo `base_category`/`role` chỉ áp dụng
  **sau khi** xác thực thành công, quyết định đích điều hướng (mục 5), không quyết định có hiển thị form
  hay không [SoT: 01-rd/screens/shared/auth.md:6].

## 7. Ngoài phạm vi file này (chốt ở DD/module identity)

- Request/response cụ thể của các endpoint ở mục 4 — `03-dd/api/identity.md`.
- Thuật toán JWT, cấu trúc claim, cơ chế rotation refresh-token, redirect URI/PKCE của OAuth —
  `02-bd/security/identity.md`, `03-dd/logic/identity.md`.
- Bảng dữ liệu (`users`, `refresh_tokens`, `password_reset_otps`...) — `02-bd/database/identity.md`.
- Validate cụ thể (độ mạnh mật khẩu, định dạng email, giới hạn ký tự) — `03-dd/validation/identity.md`.

## 8. Câu hỏi mở

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q1(BD) | `TermsCheckbox` chưa tick có chặn submit `signup` không, và thông báo lỗi hiển thị thế nào (đã có state lỗi field chung, nhưng đây là lỗi ở checkbox chứ không phải input text)? | Prototype chỉ có toggle, không có validate (`fakeAuth()` luôn thành công) [SoT: 09-layoutBase/Đăng nhập & Đăng ký.dc.html:191-197] | Chặn submit, hiện cảnh báo dưới checkbox cùng cơ chế `InlineFieldError` | DD/UI khi build FE |
| Q2(BD) | Hai nút Google/GitHub trong prototype cùng gọi `submit` (dùng chung `fakeAuth`) — khi build thật, mỗi nút phải khởi tạo redirect OAuth riêng theo provider. Route callback OAuth có ở lại đúng URL `auth` hay là một URL riêng (`/auth/callback/google`)? | Prototype không phân biệt hai nút, chỉ RD F1-15 mô tả hành vi nghiệp vụ, không mô tả URL màn | Đề xuất route callback riêng (`/auth/callback/{provider}`), không tái dùng URL `/auth` chính, để tách lỗi callback khỏi lỗi form | DD (`03-dd/api/identity.md`, `03-dd/screens/shared/auth.md`) |
| Q3(BD) | Đã có Access Token hợp lệ mà quay lại `/auth` — redirect thẳng hay vẫn cho xem lại form? | RD không đề cập, prototype không có khái niệm "đã đăng nhập" khi vào lại trang này | Redirect thẳng về đích theo vai trò (mục 5) | Chủ dự án / DD |

**Status:** DONE
**Summary:** Đã viết BD màn `auth` (trục `shared`) tại `02-bd/screens/shared/auth.md` — layout regions, component inventory, screen states (bao gồm 3 state mới `forgot_email`/`forgot_otp`/`forgot_reset` và biến thể `deactivated-recovery` chưa có trong prototype), danh sách API tên thuần thuộc `identity` (không viết request/response), navigation theo vai trò, access rights (không cần đăng nhập), và 3 câu hỏi mở mới phát sinh ở tầng BD (Q1-Q3, không trùng Q1-Q4b đã đóng ở RD).
**Concerns/Blockers:** Không có blocker. Lưu ý cho DD: Q2(BD) về route callback OAuth nên được xác nhận trước khi viết `03-dd/api/identity.md` phần OAuth, vì ảnh hưởng cấu trúc route ở tầng Next.js.
