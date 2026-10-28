# Testcase QA F1 — đăng ký, đăng nhập và phiên

> Phạm vi: [TDD F1](../../../modules/F1_identity.md) AC-01–11. Oracle từ `03-dd/api/F1_identity.md` #1–8, `03-dd/logic/F1_identity.md` BR-01–04, `03-dd/validation/F1_identity.md` §1–4, §6–7. Đây là **ca thiết kế, chưa thực thi**. Prefix API trong các bước: `/api/v1/identity`.

## Fixture chung

- Đồng hồ điều khiển `T0=2026-10-27T02:00:00Z`; email tạo mới `qa.f1.new@example.test`, email có sẵn `qa.f1.active@example.test`, email không tồn tại `qa.f1.none@example.test`. Mỗi ca reset DB, Redis, hộp thư giả và audit spy về snapshot riêng.
- `U_ACTIVE`: `ACTIVE`, `STUDENT`, email `qa.f1.active@example.test`, mật khẩu `ValidPass123`, không có lần đăng nhập sai. `U_OTHER`: cùng trạng thái, email `qa.f1.other@example.test`. IP A `192.0.2.10`, IP B `192.0.2.11`.
- Mật khẩu thay thế `NewPass456`; OTP do mail spy phát sinh được lấy bằng kênh test bảo mật, không cố định giá trị hoặc ghi vào bằng chứng chia sẻ. Với ca cần OTP sai, dùng sáu chữ số khác OTP thật. Provider OAuth là stub `google`, state/code do cổng OAuth sinh; không tự đặt state hợp lệ.
- Cookie refresh chỉ quan sát thuộc tính và hash/ID trong DB, không lưu giá trị thô vào log hay báo cáo. Mọi ca có kiểm không trả mật khẩu/OTP/refresh token trong JSON ngoài hợp đồng.

| AC | Nhánh thiết kế | ID | Review |
| :--- | :--- | :--- | :--- |
| 01 | Đăng ký, OTP xác thực, dữ liệu bí mật | TC-F1-AC01-01 | Đã review |
| 02 | Email trùng không phân biệt hoa/thường | TC-F1-AC02-01 | Đã review |
| 03 | Đăng nhập đúng và cookie | TC-F1-AC03-01 | Đã review |
| 04 | Ngưỡng sai, cặp email/IP, hết hạn, Redis lỗi | TC-F1-AC04-01/02/03 | Đã review |
| 05 | Rotation thành công | TC-F1-AC05-01 | Đã review |
| 06 | Reuse thu hồi family | TC-F1-AC06-01 | Đã review |
| 07 | Thiếu header CSRF | TC-F1-AC07-01 | Đã review |
| 08 | Quên mật khẩu chống dò email | TC-F1-AC08-01 | Đã review |
| 09 | Reset thành công, trạng thái Admin tạo | TC-F1-AC09-01/02 | Đã review |
| 10 | OAuth-only không được reset mật khẩu | TC-F1-AC10-01 | Đã review |
| 11 | Provider chưa xác thực email | TC-F1-AC11-01 | Đã review |

### TC-F1-AC01-01 — Đăng ký tạo tài khoản chờ xác thực

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-01; API #1, logic BR-21, validation §1, §3, §5 |
| Kind / priority | Normal + security; P0 vì tạo tài khoản và bí mật |
| Environment | API, DB, Redis, mail spy, log collector |
| Preconditions | Email mới chưa có trong `users`; mail spy hoạt động; thời gian T0. |
| Test data | `{"email":"qa.f1.new@example.test","password":"ValidPass123","displayName":"QA New"}`. |
| Steps | POST `/auth/register`; đọc response, `users`, Redis `identity:emailverify:<userId>`, mail spy và log. |
| Expected | `201` có UUID `userId` và email; đúng một user `PENDING_VERIFICATION`, `must_change_password=false`, hash mật khẩu chứ không lưu plaintext; đúng một OTP xác thực được gửi và Redis TTL ban đầu 10 phút. Response/mail/log không chứa mật khẩu; response/log không chứa OTP. Không cấp access/refresh token. |
| Cleanup | Xoá fixture theo userId cùng OTP; khôi phục mail spy. |
| Evidence | Response đã che dữ liệu bí mật, truy vấn user, TTL Redis, số mail và log scan. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC02-01 — Đăng ký trùng email khác chữ hoa

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-02; API #1, validation §3 |
| Kind / priority | Abnormal + boundary; P0 vì tính duy nhất tài khoản |
| Environment | API, DB, mail spy |
| Preconditions | `U_ACTIVE` tồn tại; snapshot số user và email gửi. |
| Test data | `{"email":"QA.F1.ACTIVE@EXAMPLE.TEST","password":"ValidPass123","displayName":"Duplicate"}`. |
| Steps | POST `/auth/register`; đối chiếu `users` theo `LOWER(email)` và mail spy. |
| Expected | `409 IDT-101`; vẫn đúng một user cho email chuẩn hoá, không đổi hash/status, không gửi OTP/hướng dẫn mới và không cấp phiên. |
| Cleanup | Reset snapshot. |
| Evidence | Response, truy vấn unique theo lower(email), mail count trước/sau. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC03-01 — Đăng nhập đúng tạo phiên an toàn

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-03; API #2, logic BR-21 |
| Kind / priority | Normal + security; P0 vì phát phiên |
| Environment | API, DB, trình duyệt hoặc HTTP client lưu cookie |
| Preconditions | `U_ACTIVE`; không có rate-limit; không có token cũ trong client. |
| Test data | `{"email":"qa.f1.active@example.test","password":"ValidPass123"}`, IP A. |
| Steps | POST `/auth/login`; dùng access token gọi GET `/me/profile`; kiểm cookie và DB token. |
| Expected | `200`, `expiresIn=900`, `user.id` là U_ACTIVE, `baseCategory=STUDENT`, `mustChangePassword=false`; GET hồ sơ của chính U_ACTIVE thành công. Cookie `refresh_token` có `HttpOnly; Secure; SameSite=Strict`; DB chỉ lưu token hash. JSON/log không chứa refresh token thô hoặc mật khẩu. |
| Cleanup | Thu hồi phiên và reset fixture. |
| Evidence | Login/profile response đã redacted, cookie attributes, token-hash row, log scan. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC04-01 — Lần sai thứ năm và lần thử kế tiếp trong cửa sổ

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-04; API #2, logic BR-01, validation §2 |
| Kind / priority | Boundary + security; P0 vì chống dò mật khẩu |
| Environment | API, Redis, DB spy, clock |
| Preconditions | `U_ACTIVE`; Redis key `(email U_ACTIVE, IP A)` chưa có ở T0. |
| Test data | Năm POST sai `WrongPass123`, POST thứ sáu đúng `ValidPass123`, cùng email/IP. |
| Steps | Gửi năm POST sai tại T0, sau đó POST đúng tại T0+1 phút; đọc Redis counter và DB spy. |
| Expected | Cả năm lần sai `401 IDT-102` thông báo chung; counter=5. Lần thứ sáu `429 IDT-103`, không phát token/cookie, không tăng counter và không truy vấn DB ở nhánh đã bị khoá. |
| Cleanup | Xoá key rate-limit và token test. |
| Evidence | Sáu response, Redis counter/TTL, DB spy, token count. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC04-02 — Phạm vi IP và cửa sổ hết hạn

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-04; logic BR-01 |
| Kind / priority | Boundary; P0 vì khoá nhầm người dùng |
| Environment | API, Redis, clock |
| Preconditions | `U_ACTIVE` và `U_OTHER`; IP A của U_ACTIVE đã có 5 lỗi tại T0. |
| Test data | Mật khẩu đúng `ValidPass123`; IP B; email U_OTHER; thời điểm T0+15 phút+1 giây. |
| Steps | Thử đúng U_ACTIVE từ IP B trong lúc IP A bị chặn; thử đúng U_OTHER từ IP A; chuyển clock qua TTL của key IP A và thử đúng U_ACTIVE từ IP A. |
| Expected | Ba lần được `200` và cấp phiên riêng; IP/email khác không bị chặn lây. Sau khi key hết hạn, U_ACTIVE từ IP A vào được và counter cũ bị xoá. |
| Cleanup | Thu hồi ba phiên, reset Redis và clock. |
| Evidence | Ba response, ba key Redis trước/sau, clock trace. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC04-03 — Redis rate-limit lỗi vẫn cho kiểm mật khẩu

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-04; logic BR-01 xử lý lỗi |
| Kind / priority | Recovery; P1 vì suy giảm thành phần phụ trợ |
| Environment | API, fault injection Redis, log collector |
| Preconditions | U_ACTIVE tồn tại; Redis rate-limit trả lỗi kết nối ngay khi đọc key. |
| Test data | POST login đúng từ IP A với `ValidPass123`. |
| Steps | Ngắt Redis rate-limit; POST login; đọc log và DB phiên. |
| Expected | Login đúng vẫn `200` và có phiên; có WARN về rate-limit suy giảm nhưng không chứa mật khẩu/token thô. Không trả `429` giả hoặc tạo counter sai. |
| Cleanup | Khôi phục Redis, thu hồi phiên. |
| Evidence | Response, fault trace, WARN log đã redacted, DB token row. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC05-01 — Refresh xoay vòng một lần

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-05; API #3, logic BR-02, validation §6 |
| Kind / priority | Normal + security; P0 vì vòng đời phiên |
| Environment | API, DB, cookie client |
| Preconditions | U_ACTIVE đã login, refresh token A còn hạn và chưa revoke. |
| Test data | POST `/auth/refresh`, cookie A, header `X-Requested-With: XMLHttpRequest`. |
| Steps | Gọi refresh đúng một lần; dùng access token mới đọc hồ sơ; so bản ghi family trước/sau. |
| Expected | `200`, access token mới `expiresIn=900`, cookie B mới với thuộc tính an toàn; A có `revoked_at`, B active cùng `family_id`, đúng một token active trong family. Access token mới dùng được; không lộ token thô trong JSON/log. |
| Cleanup | Revoke family, reset fixture. |
| Evidence | Response redacted, cookie attributes, token-hash/family DB rows. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC06-01 — Tái sử dụng token A thu hồi cả family

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-06; API #3, logic BR-02 |
| Kind / priority | Security; P0 vì phát hiện token bị đánh cắp |
| Environment | API, DB, audit spy |
| Preconditions | Cùng family có A đã rotate, B đang active; không có audit reuse trước đó. |
| Test data | Cookie A cũ, header `X-Requested-With: XMLHttpRequest`; sau đó cookie B. |
| Steps | POST refresh bằng A; kiểm DB/audit; POST refresh bằng B. |
| Expected | A bị từ chối `401 IDT-106`; B bị revoke trong family, không cấp token C; đúng một audit `SECURITY_TOKEN_REUSE_DETECTED` cho lần phát hiện. B không thể gia hạn tiếp. |
| Cleanup | Reset family và audit fixture. |
| Evidence | Hai response, DB family diff, audit row. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC07-01 — Thiếu header chống CSRF không đổi phiên

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-07; API #3, validation §6 |
| Kind / priority | Security; P0 vì từ chối refresh sai nguồn |
| Environment | API, DB |
| Preconditions | Token A của U_ACTIVE active, chưa từng refresh. |
| Test data | Cookie A; POST `/auth/refresh` không có `X-Requested-With`. |
| Steps | POST không header; đọc response và token rows; sau đó POST cùng A có header đúng để xác nhận A còn dùng được. |
| Expected | Lần thiếu header `400 IDT-V11`, không cấp access/cookie mới, A vẫn active và không có token B/audit reuse. Lần có header `200`, chứng minh lần đầu không tiêu thụ A. |
| Cleanup | Revoke family. |
| Evidence | Hai response, DB diff, cookie headers. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC08-01 — Quên mật khẩu không tiết lộ sự tồn tại email

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-08; API #7, logic BR-04 |
| Kind / priority | Security; P0 vì chống dò tài khoản |
| Environment | API, Redis, mail spy |
| Preconditions | U_ACTIVE tồn tại; email `qa.f1.none@example.test` chưa tồn tại; cả hai không bị cooldown. |
| Test data | Hai POST `/auth/password/forgot` với từng email, cách nhau đủ để không đụng giới hạn cùng user. |
| Steps | POST email tồn tại; POST email không tồn tại; so response, mail spy, Redis. |
| Expected | Cả hai `200` cùng JSON `message` theo API #7, không có dấu hiệu tồn tại tài khoản trong body; chỉ U_ACTIVE nhận một OTP và một key `identity:pwreset:<userId>` TTL 10 phút; email không tồn tại không có mail/key. Không đổi mật khẩu hoặc phiên. |
| Cleanup | Xoá OTP, mail fixture. |
| Evidence | Hai response, mail count/recipient đã che, Redis key count, user/token DB diff. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC09-01 — OTP đúng đổi mật khẩu và thu hồi phiên cũ

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-09; API #7–8, logic BR-04, validation §1 |
| Kind / priority | Normal + security; P0 vì khôi phục tài khoản |
| Environment | API, Redis, mail spy, DB |
| Preconditions | U_ACTIVE có hai refresh token active từ hai thiết bị; OTP mới cấp tại T0 và chưa sai lần nào. |
| Test data | POST forgot U_ACTIVE; POST reset `{email:U_ACTIVE, otp:<mã mail spy>, newPassword:"NewPass456"}`. |
| Steps | Yêu cầu OTP; reset đúng trước T0+10 phút; thử refresh bằng hai token cũ; login bằng mật khẩu cũ rồi mới. |
| Expected | Reset `200` đúng message API #8, OTP bị tiêu thụ, hash đổi, `must_change_password=false`; hai refresh token cũ không gia hạn được, mật khẩu cũ `401 IDT-102`, mật khẩu mới login `200`. Không có token mới phát trực tiếp từ reset. |
| Cleanup | Thu hồi token mới, reset DB/Redis/mail. |
| Evidence | Reset/login/refresh response, token rows, hash change, OTP key absent. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC09-02 — OTP reset tài khoản Admin tạo chuyển trạng thái

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-09 mở rộng; API #8, logic BR-04 bước 4, BR-21 |
| Kind / priority | Normal + boundary; P0 vì trạng thái tài khoản |
| Environment | API, Redis, mail spy, DB |
| Preconditions | Hai user độc lập có mật khẩu: A `PENDING_VERIFICATION,must_change_password=true` do Admin tạo; B `ACTIVE,must_change_password=true`. Mỗi user có OTP quên mật khẩu còn hạn. |
| Test data | A `qa.f1.admincreated@example.test`, B `qa.f1.mustchange@example.test`; mỗi user reset bằng OTP riêng và `NewPass456`. |
| Steps | POST reset cho A và B; đọc DB; login từng user bằng mật khẩu mới. |
| Expected | Cả hai reset `200`, `must_change_password=false`; A chuyển `ACTIVE`, B vẫn `ACTIVE`; hai lần login `200` với `user.mustChangePassword=false`. Không còn OTP đã dùng. |
| Cleanup | Reset cả hai user, Redis/mail/token. |
| Evidence | Hai reset/login response, DB state diff, Redis key absence. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC10-01 — OAuth-only không được tạo mật khẩu qua reset

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-10; API #8, logic BR-04 |
| Kind / priority | Security; P0 vì đường cấp quyền truy cập |
| Environment | API, Redis, mail spy, DB |
| Preconditions | User OAuth-only `qa.f1.oauth@example.test` có `password_hash=NULL`, chưa từng đặt mật khẩu; không có OTP reset. |
| Test data | POST forgot với email OAuth-only; POST reset với email đó, mã sáu số bất kỳ và `NewPass456`. |
| Steps | POST forgot; kiểm mail hướng dẫn provider và không có key OTP; POST reset; đọc response, user, token và liên kết provider. |
| Expected | Forgot trả cùng `200`/body chung và gửi hướng dẫn provider, không tạo OTP/audit PASSWORD_RESET_REQUESTED. Reset trả chung `422 IDT-107`, không dùng IDT-108 hoặc tiết lộ phương thức đăng nhập; `password_hash` vẫn NULL, không phát phiên, không đổi provider link hoặc status. |
| Cleanup | Reset fixture OAuth và OTP. |
| Evidence | Response, DB user/provider/token diff. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC11-01 — OAuth provider chưa xác thực email bị từ chối

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-11; API #5–6, logic BR-03, validation §6 |
| Kind / priority | Security; P0 vì liên kết tài khoản |
| Environment | API, Redis state, provider stub, DB |
| Preconditions | State hợp lệ vừa lấy từ #5; provider stub chấp nhận code và trả email `qa.f1.oauthnew@example.test` với `email_verified=false`. |
| Test data | GET `/auth/oauth/google/redirect`; GET callback với `code=<provider stub code>&state=<state mới>` trước TTL 10 phút. |
| Steps | Lấy redirect/state; gọi callback; đọc response, users, oauth_identities và token rows. |
| Expected | Callback `422 IDT-110`; không tạo user, oauth identity hay refresh token; không phát access token. State/code/OTP không ghi vào log. |
| Cleanup | Xoá state và provider fixture. |
| Evidence | Callback response, provider trace đã redacted, DB diff, log scan. |
| Design review | Đã review; chưa thực thi |
