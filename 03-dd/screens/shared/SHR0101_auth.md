# DD màn hình — Đăng nhập và đăng ký (`SHR0101`)

## 0. Thông tin tài liệu

| Mục | Giá trị |
| :--- | :--- |
| Trục | Màn hình `shared/auth`; A1 và A2; không gồm `/admin/login` |
| Route | `/login` mở `login`, `/register` mở `signup`; các bước quên mật khẩu ở cùng màn |
| Nguồn | `01-rd/screens/shared/SHR0101_auth.md`; `02-bd/screens/shared/SHR0101_auth.md`; `03-dd/api/F1_identity.md` |
| Prototype | `05-coding/frontend/src/views/shared/auth/`; `features/auth-by-credentials/`; `entities/auth/` |
| Phiên bản | V0.1, 2026-10-25; DD trục màn, Mode B (agent-native) |

BD Sheet 3 còn ghi đích học viên `my_progress`; RD đã đổi thành `/dashboard` (`DEC-2026-0927-student-dashboard-home`). BD nêu A1/A2 dùng chung màn này; A3 dùng `/admin/login` theo `DEC-2026-0915-admin-separate-login-route`. Hợp đồng API chỉ dẫn đến F1, không khai lại DTO ở đây.

## 1. Bản đồ slice FSD

| Lớp | Đường dẫn tương đối từ `src/` | Vai trò và việc nối thật |
| :--- | :--- | :--- |
| app | `app/(public)/login/page.tsx`, `app/(public)/register/page.tsx`, `app/(public)/auth/callback/[provider]/page.tsx` | Chọn `initialMode`; callback OAuth ở route riêng theo BD Q2 |
| views | `views/shared/auth/ui/auth-view.tsx` | Bố cục, tiêu đề theo mode, panel giới thiệu, overlay bốn bước; bỏ `DevQuickLogin` khi nối thật |
| features | `features/auth-by-credentials/model/use-auth-flow.ts`; `ui/auth-form.tsx`, `otp-input-group.tsx`, `oauth-button-group.tsx`, `auth-loading-overlay.tsx` | Máy trạng thái `signup`, `verify_email`, `login`, `forgot_email`, `forgot_otp`, `forgot_reset`, banner khôi phục; biểu mẫu không tự sở hữu hợp đồng F1 |
| entities | `entities/auth/model/schema.ts`, `api/mutations.ts`; `entities/user/model/area.ts` | Zod, client F1, vai trò và đích mặc định; bỏ `fake-auth.ts` khỏi đường chạy thật |
| shared | `shared/ui/ThemeLangSwitcher`, toast; `app/providers/auth-provider.tsx` | Dùng chung i18n/theme và giữ phiên; chỉ một lần refresh cho các request `401` đồng thời |

Các props của `AuthView` chỉ gồm `initialMode`. `AuthForm` nhận trạng thái và callback từ `useAuthFlow`; các thành phần con không trực tiếp gọi API. `AuthLoadingOverlay` chỉ là trình diễn sau khi đã xác thực thành công, không thay phản hồi của máy chủ. `AuthAsidePanel` đã nằm ở `shared/ui`, dùng lại. [SoT: `views/shared/auth/ui/auth-view.tsx:1-113`; BD Sheet 4.5]

## 2. API tiêu thụ

Bảng 2.1: Ánh xạ sự kiện của BD Sheet 8; chi tiết request, response, mã lỗi nằm ở [F1](../../api/F1_identity.md).

| EVT | F1 | Gọi từ màn / tình trạng |
| :--- | :--- | :--- |
| 1 đăng ký | #1 `POST /auth/register` | `signup`; sau thành công phải vào bước xác thực email, không chạy overlay đăng nhập ngay |
| 1 đăng nhập | #2 `POST /auth/login` | `login`; dùng email theo hợp đồng F1 |
| 4 OAuth | #5, #6 redirect/callback | Hai provider; giữ `state` do F1 cấp, xử lý callback ở `/auth/callback/{provider}` |
| 5, 7 quên mật khẩu và gửi lại | #7 `POST /auth/password/forgot` | Cùng lời gọi, thông báo chung dù email không tồn tại |
| 6 nhập OTP quên mật khẩu | F1 #60 `POST /auth/password/verify-otp` | Kiểm mã trên server trước khi mở bước mật khẩu mới; không tiêu thụ mã |
| 9 đặt lại mật khẩu | #8 `POST /auth/password/reset` | Đổi mật khẩu, trở về `login`, không tự đăng nhập |
| 1 xác thực email tự đăng ký | #53, #54 `POST /auth/email/verify`, `/resend` | Luồng `verify_email` bắt buộc do #1 tạo `PENDING_VERIFICATION` |
| 8 huỷ tự xoá | F1 #61 `POST /auth/deactivation/restore` | Gửi vé khôi phục từ #2/#6 sau xác minh; chỉ `SELF` trong hạn 30 ngày |
| 10 làm mới phiên | #3 `POST /auth/refresh` | Client dùng chung toàn app; màn nhận kết quả điều hướng nếu thất bại |

## 3. Trạng thái màn

| Vùng | Điều kiện | Hiển thị và hành vi |
| :--- | :--- | :--- |
| Form | Chưa gửi hoặc đổi mode | Xoá lỗi của mode trước; giữ `redirect` hợp lệ; form nhận focus ở tiêu đề rồi ô đầu |
| Form | Zod hoặc F1 trả lỗi trường | Viền ô lỗi + `aria-invalid`; đúng một toast lỗi đầu tiên, không có dòng lỗi dưới ô (`DEC-2026-1003-toast-feedback-channel`) |
| Form | Đang gọi F1 | Khoá nút nộp để không gửi lặp; giữ giá trị nhập khi lỗi mạng; toast mã lỗi qua `apiError.*` |
| Đăng ký | #1 thành công | Chờ nhập OTP xác thực email; #2 trả `IDT-113` cũng dẫn về bước này, không phát token |
| Đăng nhập | #2/#6 trả `IDT-104` | Banner huỷ yêu cầu xoá chỉ khi `details.restoreTicket` có; không mở cho tài khoản Admin khoá; vé giữ trong bộ nhớ màn, không ghi URL/storage |
| Quên mật khẩu | #7 luôn `200` | Cùng một toast chung và bước nhập OTP; không tiết lộ email có tài khoản hay không |
| OTP | Hết 10 phút hoặc vượt 5 lần sai | Khoá xác nhận mã cũ, cho gửi lại khi hết cooldown 60 giây; máy chủ là nguồn đúng của giới hạn |
| OAuth | Provider huỷ hoặc callback lỗi | Trở về form, một toast; không coi lỗi callback là lỗi ô email/mật khẩu |
| Toàn màn | Có phiên hợp lệ | Điều hướng theo vai trò hoặc `redirect` đã kiểm tra quyền, không hiện lại form |

## 4. Quy tắc đặc thù màn

### BR-SC-01 Đường đăng nhập và điều hướng

Khi #2 hoặc OAuth thành công, ghi access token vào bộ nhớ phiên; refresh token do F1 đặt cookie HTTP-Only. Chỉ nhận `redirect`/`returnTo` là đường nội bộ bắt đầu bằng đúng một dấu `/`, không nhận `//`, dấu `\\`, scheme hay ký tự điều khiển; kiểm quyền theo vai trò trước khi đi để URL cũ không đưa A1 vào khu A2/A3. Không có đích hợp lệ thì `STUDENT` tới `/dashboard`, `INSTRUCTOR` tới `/instructor/overview`; A3 đăng nhập ở màn khác. Bốn bước overlay chạy sau khi F1 trả thành công; overlay lỗi không thay đổi trạng thái xác thực. [SoT: BD Sheet 3; `entities/user/model/area.ts:26-38`; phần kiểm URL là suy luận để đáp ứng điều hướng an toàn]

### BR-SC-02 Đăng ký cần xác thực email

F1 #1 tạo `PENDING_VERIFICATION` và #2 chặn bằng `IDT-113` cho tới khi #53 thành công. Vì vậy `submitSignup` của prototype không thể dùng kết quả mock để gọi `runLoadingOverlayThenNavigate` ngay. Sau #1, giữ email đăng ký trong trạng thái màn, vào bước `verify_email`, gọi #53; thành công quay về `login` với email đã nhập sẵn và toast. #54 gửi lại theo cooldown 60 giây; không ghi mã vào URL, local storage hay log. BD chưa vẽ bước này; DD chốt state để hợp đồng F1 thực thi được, đồng bộ hình BD ở lượt rà BD.

### BR-SC-03 Quên mật khẩu

`forgot_email` gọi #7 và luôn cho chuyển sang `forgot_otp`; `forgot_otp` giữ email ở bộ nhớ màn. Sau khi nhập sáu chữ số, gọi F1 #60; chỉ phản hồi thành công mới chuyển `forgot_reset`. Bước cuối vẫn gửi OTP cùng mật khẩu mới tới #8 để kiểm lại và tiêu thụ một lần; #60 không kéo dài TTL 10 phút. Khi #8 trả `IDT-107`, giữ mật khẩu đã gõ, trở về bước OTP và thông báo chung, không phân biệt mã sai/hết hạn. Khi #8 thành công, xoá OTP trong bộ nhớ và về `login`. [SoT: RD SHR0101 §2; F1 §1; `DEC-2026-1025-dd-auth-recovery-contracts`]

### BR-SC-04 Huỷ yêu cầu tự xoá

Sau #2 hoặc OAuth callback #6 xác minh đúng tài khoản `SELF` còn ân hạn, F1 trả `403 IDT-104` cùng `restoreTicket` ngắn hạn. Màn giữ vé chỉ trong bộ nhớ, vẽ banner có nút huỷ; bấm nút gọi #61 đúng một lần. Thành công xoá vé, báo đã khôi phục và để người dùng đăng nhập lại (không phát token). Vé sai/hết hạn (`IDT-114`) xoá banner/vé và yêu cầu đăng nhập lại để xác minh mới; không dùng mật khẩu còn trong state để tự phát lại. Tài khoản bị Admin khoá không có vé thì chỉ báo không thể tự khôi phục. [SoT: RD SHR0101 Q2; F1 #61]

### BR-SC-05 Phiên và OAuth

F1 #3 refresh một lần cho một đợt request `401`, phát lại request gốc một lần; refresh thất bại xoá access token rồi về `/login` với `returnTo` nội bộ. F1 #5 cấp URL provider và `state`; trình duyệt điều hướng tới URL đó. F1 #6 xử lý callback, so `state`, sau đó đi cùng BR-SC-01. Callback không gửi `state` tự tạo ở client. Chế độ trước khi đi provider chỉ phục vụ hiển thị khi lỗi. [SoT: F1 §1; BD Sheet 8 EVT-4, EVT-10]

## 5. Kiểm tra phía client

| Trường | Zod đích | F1 validation / lệch prototype |
| :--- | :--- | :--- |
| `displayName` đăng ký | `z.string().trim().min(2).max(100)` | F1 §5 `IDT-V19`, `IDT-V20`; prototype dùng `username` 3–32, cần đổi tên và giới hạn |
| Email đăng ký/đăng nhập/quên | `z.string().trim().email().max(254)` | F1 §1 nhận `email`, không nhận `identifier` của mock; BD hiển thị “Email/Tên đăng nhập” cần đồng bộ |
| Mật khẩu mới | 8–128 ký tự, có chữ và số; không trùng email/tên hiển thị | F1 §1 `IDT-V01`–`V04`; kiểm trùng ở máy chủ, client chỉ kiểm điều kiện biết được |
| Đồng ý điều khoản | `z.literal(true)` | Yêu cầu BD Sheet 9; cờ UI không thuộc request #1, không gửi tới F1 |
| OTP | đúng sáu chữ số | F1 #8, #53; số lần sai và TTL do máy chủ quyết định |
| Xác nhận mật khẩu | bằng `newPassword` | Chỉ ở client; #8 nhận một mật khẩu mới |

## 6. i18n và truy cập

Dùng tiền tố `auth.*` ở `shared/i18n/messages/vi.json` và `en.json`; lỗi F1 dịch qua `apiError.<code>`, không in `message` của server. Thêm nhãn cho OTP xác thực email, gửi lại, hết hạn và trạng thái `PENDING_VERIFICATION` khi OQ-3 được đóng. Mọi nút Google/GitHub có tên provider; OTP sáu ô cần một nhãn nhóm và hỗ trợ dán sáu số. Toast đặt `role=alert` cho lỗi, `role=status` cho thành công; overlay thông báo bước hiện tại bằng `aria-live=polite`, không kéo focus rời form khi lỗi. Bàn phím Tab tới các điều khiển theo thứ tự thị giác; Enter nộp biểu mẫu đang mở; nút hiện/ẩn mật khẩu dùng `aria-pressed`.

## 7. Điểm kiểm thử

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| SH-01 | Normal | `signup` hợp lệ gọi #1 rồi hiện OTP email, không điều hướng như đã đăng nhập | Vitest | TDD màn `AUTH-AC-01` |
| SH-02 | Abnormal | `IDT-113` khi login đưa tới xác thực email, không tạo phiên | Vitest | TDD màn `AUTH-AC-01` |
| SH-03 | Boundary | displayName 2/100 ký tự hợp lệ, mật khẩu thiếu số bị chặn | Vitest | TDD màn `AUTH-AC-01` |
| SH-04 | Normal | login và OAuth hợp lệ đi đúng khu, `returnTo` nội bộ có quyền được ưu tiên | Playwright | TDD màn `AUTH-AC-02` |
| SH-05 | Security | `returnTo=//evil.example`, đường chứa dấu `\\`, hoặc khu sai quyền bị bỏ | Vitest | TDD màn `AUTH-AC-02` |
| SH-06 | Abnormal | Quên mật khẩu cho email không tồn tại vẫn hiện cùng thông báo với email tồn tại | Vitest | TDD màn `AUTH-AC-03` |
| SH-07 | Boundary | #60 kiểm OTP trước `forgot_reset`; OTP hết hạn/sai quá lượt; gửi lại chịu cooldown, không lộ loại lỗi | Vitest | TDD màn `AUTH-AC-03` |
| SH-08 | Normal | Reset xong về login, không tự tạo phiên | Vitest | TDD màn `AUTH-AC-03` |
| SH-09 | Abnormal | OAuth callback sai `state`, refresh token reuse, lỗi mạng: không giữ phiên giả | Vitest | TDD màn `AUTH-AC-02` |
| SH-10 | Normal | #2/#6 trả vé cho tài khoản `SELF`, #61 phục hồi trong ân hạn rồi yêu cầu đăng nhập lại; vé dùng một lần, không phát token | Playwright | TDD màn `AUTH-AC-04` |

## 8. Nợ prototype và câu hỏi mở

Dòng `views/auth` ở `06-plan/PROTOTYPE_DEBT.md:855` chưa tốt nghiệp: `fake-auth.ts` còn trả kết quả giả và `DevQuickLogin` còn hiện cả A3 trên màn A1/A2. `use-auth-flow.ts` gọi verify OTP mock riêng, cho đăng ký thành công đi thẳng vào hệ thống, và nhận `identifier` thay email. Những khác biệt này cần sửa khi nối API.

| # | Vấn đề | Hướng DD / trạng thái |
| :--- | :--- | :--- |
| OQ-1 | **Đã chốt** `DEC-2026-1025-dd-auth-recovery-contracts`: xác minh OTP quên mật khẩu trước bước mật khẩu mới | F1 #60 kiểm không tiêu thụ; F1 #8 kiểm lại và tiêu thụ |
| OQ-2 | Callback OAuth dùng route nào | Đã đóng ở DD: `/auth/callback/{provider}` theo BD Q2; F1 #6 là endpoint server, route frontend nhận kết quả rồi áp BR-SC-01 |
| OQ-3 | BD không vẽ OTP xác thực email tự đăng ký, F1 #53/#54 đã có | Đã đóng ở DD: bổ sung state `verify_email` và nội dung tương ứng, vì #2 chặn `PENDING_VERIFICATION`; đồng bộ BD sau |
| OQ-4 | **Đã chốt** `DEC-2026-1025-dd-auth-recovery-contracts`: banner huỷ tự xoá có endpoint F1 | F1 #61 nhận vé ngắn hạn chỉ sau #2/#6 xác minh; không gọi #37 của Admin |
| OQ-5 | Đã có phiên hợp lệ mở `/login`/`/register` | Đã đóng ở DD: chuyển về đích theo vai trò hoặc `returnTo` hợp lệ; không hiện form đăng nhập lần hai, theo BD Q3 |
| OQ-6 | Huỷ tự xoá xong có tự đăng nhập | Đã chốt 2026-10-25: yêu cầu đăng nhập lại theo BD EVT-8; phản hồi khôi phục không phải token, không dùng mật khẩu còn giữ trong state để tự gửi lại |

Các câu OQ-1–6 đã có kết luận DD hoặc quyết định của chủ dự án. TDD màn đã có tại `04-tdd/screens/shared/SHR0101_auth.md`.
