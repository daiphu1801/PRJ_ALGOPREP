# Testcase QA F1 — hồ sơ và vòng đời tài khoản

> Phạm vi: [TDD F1](../../../modules/F1_identity.md) AC-12–18b; DD `03-dd/api/F1_identity.md` #10–13, #18; `03-dd/logic/F1_identity.md` BR-05/06/08; `03-dd/jobs/F1_identity.md` JOB-ID-02; `03-dd/validation/F1_identity.md` §1, §3–5. Ca thiết kế, **chưa thực thi**. Prefix API: `/api/v1/identity`.

Fixture chung: đồng hồ T0 `2026-10-27T02:00:00Z`; `U1` ACTIVE/STUDENT/email `qa.f1.profile@example.test`, mật khẩu `ValidPass123`, hai phiên A/B; `U2` ACTIVE/OAuth-only/email `qa.f1.oauthprofile@example.test`, `password_hash=NULL`; `U3` ACTIVE/STUDENT/email `qa.f1.otherprofile@example.test`. Mỗi ca dùng snapshot DB/Redis/mail/audit riêng; mail spy cho phép đọc OTP qua kênh test bảo mật. Không lưu OTP hoặc token thô vào bằng chứng chia sẻ.

| AC | Nhánh | ID | Review |
| :--- | :--- | :--- | :--- |
| 12 | Mass assignment | TC-F1-AC12-01 | Đã review |
| 13 | Đổi email đúng/sai/tranh chấp | TC-F1-AC13-01/02/03 | Đã review |
| 14 | Đổi mật khẩu, phiên khác | TC-F1-AC14-01 | Đã review |
| 15 | OAuth-only đặt mật khẩu | TC-F1-AC15-01 | Đã review |
| 16 | Xác nhận email sai | TC-F1-AC16-01 | Đã review |
| 17 | Tự xoá, chặn token cũ | TC-F1-AC17-01 | Đã review |
| 18 | Job sau mốc 30 ngày, idempotent và lỗi lô | TC-F1-AC18-01/02 | Đã review |
| 18b | Admin lock không bị ẩn danh | TC-F1-AC18b-01 | Đã review |

| 14 | Wrong current password | TC-F1-AC14-02 | Reviewed 2026-10-28; `422 IDT-115` đã chốt |

### TC-F1-AC12-01 — Hồ sơ đổi được, quyền và trạng thái giữ nguyên

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-12; API #10, validation §5 |
| Kind / priority | Security; P0 vì ngăn tự nâng quyền |
| Environment | API, DB, audit spy |
| Preconditions | U1 có `role_id=STUDENT`, `status=ACTIVE`, `display_name="QA Profile"`. |
| Test data | PATCH `/me/profile` body `{"displayName":"QA Updated","roleId":"ADMIN","role_id":"ADMIN","status":"ACTIVE","schoolOrCompany":"AlgoPrep QA"}` với JWT U1. |
| Steps | PATCH; GET `/me/profile`; đọc `users`/`roles` và audit. |
| Expected | Trường hồ sơ được phép đổi sang `QA Updated`/`AlgoPrep QA`; `role_id` vẫn STUDENT, `status` vẫn ACTIVE. GET không báo U1 là ADMIN; không có audit thay quyền hoặc sự kiện nâng quyền. |
| Cleanup | Khôi phục U1. |
| Evidence | PATCH/GET response, DB diff, audit/event diff. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC13-01 — Đổi email chỉ sau OTP đúng

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-13; API #11–12, validation §3–4 |
| Kind / priority | Normal + security; P0 vì đổi định danh đăng nhập |
| Environment | API, DB, Redis, mail spy |
| Preconditions | U1 đang đăng nhập; email mới `qa.f1.newmail@example.test` chưa thuộc ai. |
| Test data | POST `/me/email-change/request` body `{"newEmail":"qa.f1.newmail@example.test"}`; POST `/me/email-change/confirm` body `{"code":<OTP mail spy>}`. |
| Steps | Gửi request; kiểm response/DB; gửi confirm trước T0+10 phút; đọc DB và Redis. |
| Expected | Request `200 {pendingEmail,expiresInSeconds:600}`, OTP chỉ gửi tới email mới, U1 vẫn dùng email cũ trước confirm. Confirm `200 {email:"qa.f1.newmail@example.test"}`, DB đổi đúng email, OTP bị tiêu thụ nguyên tử. Không có OTP trong response/log. |
| Cleanup | Đổi lại email trong fixture, xoá OTP/mail. |
| Evidence | Hai response, recipient mail đã che, DB/Redis diff. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC13-02 — OTP đổi email sai không đổi email

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-13; API #11–12, validation §3–4 |
| Kind / priority | Abnormal; P0 vì xác nhận địa chỉ đăng nhập |
| Environment | API, DB, Redis, mail spy |
| Preconditions | U1 đã request đổi sang `qa.f1.newmail@example.test`, OTP còn hạn. |
| Test data | POST `/me/email-change/confirm` với code sáu số khác OTP thật. |
| Steps | Gọi confirm bằng mã sai; đọc response, U1 và Redis OTP. |
| Expected | `422 IDT-416`; U1 giữ email cũ, OTP đúng chưa bị tiêu thụ và không có phiên mới. Không lộ mã đúng hay số lần còn lại. |
| Cleanup | Reset U1/Redis. |
| Evidence | Response, DB/Redis diff. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC13-03 — Email bị chiếm trước khi xác nhận OTP

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-13; API #11–12, validation §3–4 |
| Kind / priority | Concurrency; P0 vì unique email |
| Environment | API, DB, Redis, mail spy |
| Preconditions | U1 và U3 là hai tài khoản độc lập; U1 đã request đổi sang `qa.f1.race@example.test`, OTP còn hạn; email này chưa thuộc ai lúc request. |
| Test data | U3 đổi sang `qa.f1.race@example.test` và xác nhận xong trước; U1 sau đó POST `/me/email-change/confirm` bằng OTP đúng của mình. |
| Steps | Hoàn tất thay đổi của U3; confirm của U1; truy vấn unique lower(email) và state U1. |
| Expected | U1 nhận `409 IDT-415`, giữ email cũ; U3 giữ email mới, chỉ một user có `LOWER(email)=qa.f1.race@example.test`. Không có update email một phần hoặc phiên mới cho U1. |
| Cleanup | Reset U1/U3/Redis/mail. |
| Evidence | Hai confirm response, DB unique/state diff, Redis state. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC14-01 — Đổi mật khẩu giữ phiên hiện tại, thu hồi phiên khác

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-14; API #13, logic BR-06, validation §1 |
| Kind / priority | Security; P0 vì thu hồi phiên |
| Environment | API, DB, mail/audit spy |
| Preconditions | U1 có hai phiên A/B active, A gọi API; mật khẩu hiện tại `ValidPass123`. |
| Test data | POST `/me/password` từ A: `{"currentPassword":"ValidPass123","newPassword":"NewPass456"}`. |
| Steps | Đổi mật khẩu từ A; refresh từ A và B; thử login mật khẩu cũ và mới. |
| Expected | Đổi mật khẩu thành công; A tiếp tục refresh được, B không gia hạn được; DB chỉ giữ phiên A active trước refresh. Mật khẩu cũ bị từ chối, mật khẩu mới dùng được; response không liệt kê số phiên bị revoke. |
| Cleanup | Reset mật khẩu/phiên. |
| Evidence | Response đổi/refresh/login, DB token rows và hash diff. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC15-01 — Tài khoản OAuth đặt mật khẩu lần đầu bằng phiên hợp lệ

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-15; API #13, logic BR-05, validation §1 |
| Kind / priority | Normal + security; P0 vì thêm phương thức đăng nhập |
| Environment | API, DB, mail spy, audit spy |
| Preconditions | U2 có phiên OAuth hợp lệ, `password_hash=NULL`, provider link google còn hiệu lực. |
| Test data | POST `/me/password` body `{"newPassword":"NewPass456"}`; không gửi `currentPassword`/OTP. Negative trials không có JWT và JWT hết hạn, sau đó positive bằng JWT U2 còn hạn. |
| Steps | Gửi hai negative requests trước; kiểm DB/mail/audit không đổi. Gửi positive; đọc DB/mail/audit; login U2 bằng mật khẩu mới. |
| Expected | Thiếu/hết hạn JWT trả `401` theo auth chung, không đặt password, đổi provider, gửi email hoặc audit thành công. Positive thành công, `password_hash` thành BCrypt, provider link giữ nguyên; login mật khẩu mới được; đúng một email thông báo bảo mật và một audit cho thay đổi. Không yêu cầu OTP hoặc mật khẩu cũ. |
| Cleanup | Reset U2 và phiên/mail/audit. |
| Evidence | Response, DB hash/provider diff, mail count, audit row. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC16-01 — Email xác nhận tự xoá sai không đổi tài khoản

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-16; API #18, logic BR-08, validation §5 |
| Kind / priority | Abnormal + security; P0 vì thao tác huỷ tài khoản |
| Environment | API, DB, audit spy |
| Preconditions | U1 ACTIVE, hai phiên A/B active. |
| Test data | POST `/me/account/delete` bằng A body `{"emailConfirmation":"QA.F1.PROFILE@EXAMPLE.TEST"}` khác chữ hoa so với email đã lưu. |
| Steps | POST; đọc user, token/audit; dùng A đọc hồ sơ. |
| Expected | `422 IDT-201` vì so khớp tuyệt đối; U1 vẫn ACTIVE, `deactivation_source/deactivated_at` không đổi, A/B active, không có audit `SELF_ACCOUNT_DELETE_REQUESTED`; A đọc hồ sơ được. |
| Cleanup | Reset fixture. |
| Evidence | Response, DB/audit diff, GET profile. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC17-01 — Tự xoá chặn access token cũ ngay lập tức

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-17; API #18, logic BR-08 |
| Kind / priority | Security; P0 vì ranh giới sau tự xoá |
| Environment | API, DB, audit spy, cổng đọc lịch sử F4/F2 |
| Preconditions | U1 có access token A còn hạn 15 phút, hai refresh token A/B, ít nhất một submission và một dòng học tập. |
| Test data | POST `/me/account/delete` body `{"emailConfirmation":"qa.f1.profile@example.test"}` từ A tại T0. |
| Steps | POST; lập tức GET `/me/profile` bằng access token A; thử refresh A/B; đối chiếu lịch sử và audit. |
| Expected | Tự xoá thành công; `status=DEACTIVATED`, `deactivation_source=SELF`, `deactivated_at=T0`; cả hai refresh token bị revoke, access token A bị filter chặn ngay dù chưa hết hạn; đúng một audit `SELF_ACCOUNT_DELETE_REQUESTED`. Submission và lịch sử học tập vẫn tồn tại trong ân hạn. |
| Cleanup | Reset U1, token, audit và lịch sử fixture. |
| Evidence | POST/GET/refresh response, DB user/token/history diff, audit row. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC18-01 — Job ẩn danh đúng mốc 30 ngày, chạy lại không đổi

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-18; logic BR-08, jobs JOB-ID-02 |
| Kind / priority | Boundary + job; P0 vì xoá định danh không thể đảo ngược |
| Environment | DB, job runner, clock, F4 history read port |
| Preconditions | U1 tự xoá tại T0, có phone/company/current/target khác NULL, oauth identity và submission; U2 tự xoá tại T0+1 giây. Cả hai chưa ẩn danh. |
| Test data | Clock T0+30 ngày; chạy JOB-ID-02 hai lần, lô dưới 200 dòng. |
| Steps | Chạy job tại mốc; đọc U1/U2, provider links và submission; chạy lại cùng mốc. |
| Expected | U1 được chọn (`deactivated_at <= now-30d`): email `deleted-user-<id>@anon.local`, tên `Người dùng đã xoá`, bốn trường tự khai NULL, `anonymized_at` được đặt, oauth identities bị xoá; submission giữ nguyên userId. U2 chưa đủ mốc nên không đổi. Lần chạy lại không đổi U1 hoặc tạo audit mới. |
| Cleanup | Chỉ dùng DB test cô lập, khôi phục snapshot. |
| Evidence | Hai job logs/count, DB diff trước/sau từng lượt, F4 history linkage. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC18b-01 — Job không ẩn danh tài khoản bị Admin khoá

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-18b; logic BR-08, jobs JOB-ID-02 |
| Kind / priority | Security + job; P0 vì giữ khả năng mở khoá |
| Environment | DB, job runner, clock |
| Preconditions | U3 `DEACTIVATED`, `deactivation_source=ADMIN`, `deactivated_at=T0-31 ngày`, `anonymized_at=NULL`, email/phone/provider link có giá trị. |
| Test data | Chạy JOB-ID-02 tại T0 với U3 và một user SELF đủ hạn làm đối chứng dương. |
| Steps | Chạy job; đọc hai user và provider links. |
| Expected | User SELF đối chứng bị ẩn danh; U3 không đổi email/phone/provider/status/source/date, `anonymized_at` vẫn NULL; job không chạm U3. |
| Cleanup | Khôi phục snapshot. |
| Evidence | Job log, DB diff cả hai user/provider. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC18-02 — Lỗi trong một lô ẩn danh rollback cả lô và lần sau xử lý lại

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-18; logic BR-08, jobs JOB-ID-02 §2 |
| Kind / priority | Recovery + job; P0 vì không được ẩn danh một phần |
| Environment | Job runner, DB, clock, fault injection |
| Preconditions | Hai user SELF đủ hạn T0−31 ngày, mỗi user có oauth identity và trường định danh; cả hai nằm trong cùng lô dưới 200 dòng. |
| Test data | Inject lỗi DB ở lệnh xoá `oauth_identities` của user thứ hai; chạy job tại T0, rồi bỏ fault và chạy lại. |
| Steps | Chạy job có fault; đọc cả hai user/provider; bỏ fault; chạy job lại và đọc DB. |
| Expected | Lần đầu rollback toàn lô: cả hai user vẫn giữ email/tên/provider và `anonymized_at=NULL`, log ERROR có ID lô nhưng không chứa thông tin bí mật. Lần sau hai user được ẩn danh đúng một lần, provider links bị xoá; không có bản ghi nửa ẩn danh. |
| Cleanup | Reset DB/job fixture. |
| Evidence | Hai job log, transaction/fault trace, DB diff của cả hai lần. |
| Design review | Đã review; chưa thực thi |


## Additional password-change failure case

### TC-F1-AC14-02 — Mật khẩu hiện tại sai không đổi mật khẩu hoặc thu hồi phiên

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-14; API #13; logic BR-06; validation §1 |
| Kind / priority | Abnormal, Security; P0 (ngăn thay mật khẩu trái phép) |
| Environment | API integration, PostgreSQL, Redis/session store, mail/audit spy |
| Preconditions | U1 ACTIVE; hash hiện tại là cho `ValidPass123`; refresh session A và B đang hiệu lực; request xác thực bằng A. |
| Test data | POST `/me/password` với `currentPassword=WrongPass123`, `newPassword=NewPass456` (mật khẩu mới thỏa validation); chụp password_hash/password_changed_at và hai token rows trước request. |
| Steps | Gửi request; đọc lại user/token state; login bằng mật khẩu cũ/mới và thử refresh A/B. |
| Expected | `422 IDT-115`; password hash/timestamp không đổi; A và B giữ nguyên trạng thái trước request, không có session/token mới. Mật khẩu cũ vẫn login được, mật khẩu mới chưa được phép login. Không echo mật khẩu, không gửi email hoặc audit thành công. |
| Cleanup | Khôi phục snapshot mật khẩu/phiên và xóa mail/audit artifact. |
| Evidence | Response đã che dữ liệu, DB/token before-after, login/refresh outcomes, log/mail scan. |
| Design review | Reviewed 2026-10-28; oracle chốt 2026-10-27; `Chưa thực hiện` |
