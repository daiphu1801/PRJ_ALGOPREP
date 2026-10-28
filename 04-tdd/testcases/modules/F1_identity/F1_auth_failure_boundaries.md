# Testcase QA F1 — biên và lỗi của xác thực

> Bổ sung nhánh độc lập cho [TDD F1](../../../modules/F1_identity.md) AC-01, 04, 05, 09, 11. Nguồn oracle: `03-dd/api/F1_identity.md` #1–3, #5–8; `03-dd/logic/F1_identity.md` BR-01–04; `03-dd/validation/F1_identity.md` §1–4, §6. Ca thiết kế, chưa thực thi. Prefix API `/api/v1/identity`.

Fixture chung: clock T0 `2026-10-27T02:00:00Z`; user ACTIVE `qa.f1.boundary@example.test`/`ValidPass123`, IP `192.0.2.20`, DB/Redis/mail/provider stub cô lập từng ca. OTP đúng lấy từ mail spy qua kênh test bảo mật; OTP sai là một chuỗi sáu số khác mã đúng, không đưa mã thật vào log/báo cáo.

| AC | Nhánh | ID | Review |
| :--- | :--- | :--- | :--- |
| 01 | Mật khẩu ngoài giới hạn | TC-F1-AC01-02 | Đã review |
| 04 | Thành công xoá bộ đếm sai | TC-F1-AC04-04 | Đã review |
| 05 | Token hết hạn tự nhiên/không tồn tại | TC-F1-AC05-02/03 | Đã review |
| 09 | OTP sai/hết hạn/ngưỡng năm lần | TC-F1-AC09-03/04 | Đã review |
| 06 | Hai refresh đồng thời một token | TC-F1-AC06-02 | Đã review |
| 11 | OAuth state sai/hết hạn | TC-F1-AC11-02 | Đã review |

| 01 | Email/displayName edges; password identity match | TC-F1-AC01-03/04 | Reviewed 2026-10-28 |
| 04 | Concurrent failed attempts at threshold | TC-F1-AC04-05 | Reviewed: final invariant only, no in-flight response order requirement |
| 08 | Forgot-password resend cooldown | TC-F1-AC08-02 | Reviewed 2026-10-28; 200/body chung đã chốt |

### TC-F1-AC01-02 — Mật khẩu đăng ký ngoài giới hạn không tạo user

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-01; API #1, validation §1 |
| Kind / priority | Boundary + security; P0 vì chính sách mật khẩu |
| Environment | API, DB, Redis, mail spy |
| Preconditions | Ba email mới độc lập, chưa có user. |
| Test data | Ba POST `/auth/register` với `displayName="QA Boundary"`: (A) `Short1` dài 6; (B) `A` lặp 128 lần rồi `1` dài 129; (C) `OnlyLetters` không có chữ số. Email lần lượt `qa.f1.short@example.test`, `qa.f1.long@example.test`, `qa.f1.no_digit@example.test`. |
| Steps | Gửi từng request; kiểm response, user/OTP/mail theo từng email. |
| Expected | A `400 IDT-V01`, B `400 IDT-V02`, C `400 IDT-V03` theo HTTP lỗi kiểm dữ liệu của `03-dd/api/api.md` §1.1; không tạo user, OTP, mail hoặc phiên ở cả ba. |
| Cleanup | Reset DB/Redis/mail. |
| Evidence | Ba response, DB/Redis/mail diff. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC04-04 — Login đúng xoá lịch sử sai của cặp email/IP

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-04; API #2, BR-01 |
| Kind / priority | Boundary; P1 vì không khoá sai sau thành công |
| Environment | API, Redis, clock |
| Preconditions | User ACTIVE; key `(email,IP)` đã có bốn lỗi `401 IDT-102` trong cửa sổ T0. |
| Test data | Một login đúng `ValidPass123`, rồi một login sai `WrongPass123`, cùng email/IP trước T0+15 phút. |
| Steps | Gọi đúng; đọc key; gọi sai; đọc key lần nữa. |
| Expected | Lần đúng `200` và xoá key; lần sai tiếp theo `401 IDT-102` với counter mới bằng 1, không bị `429 IDT-103`. |
| Cleanup | Thu hồi phiên và xoá key. |
| Evidence | Hai response, Redis counter/TTL. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC05-02 — Refresh hết hạn tự nhiên không bị coi là reuse

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-05; API #3, BR-02 |
| Kind / priority | Boundary + security; P0 vì thu hồi family sai |
| Environment | API, DB, clock, audit spy |
| Preconditions | Token A trong family F có `expires_at=T0−1 giây`, `revoked_at=NULL`; token B cùng family còn active; chưa có audit reuse. |
| Test data | POST `/auth/refresh` cookie A, header `X-Requested-With: XMLHttpRequest` tại T0. |
| Steps | Refresh bằng A; đọc DB family/audit; refresh bằng B. |
| Expected | A `401 IDT-105`, không cấp token mới; không có audit `SECURITY_TOKEN_REUSE_DETECTED`, B không bị revoke và vẫn refresh `200`. |
| Cleanup | Revoke family F. |
| Evidence | Hai response, DB family diff, audit spy. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC09-03 — OTP reset sai và hết hạn cùng lỗi chung

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-09; API #8, BR-04 |
| Kind / priority | Abnormal + security; P0 vì chống dò OTP |
| Environment | API, Redis, DB, clock |
| Preconditions | Hai bản sao user ACTIVE có OTP riêng: A còn hạn, B hết hạn tại T0−1 giây; mật khẩu ban đầu `ValidPass123`. |
| Test data | Reset A bằng OTP sai sáu số; reset B bằng OTP đúng nhưng đã hết TTL; cả hai `newPassword="NewPass456"`. |
| Steps | POST reset cho A và B; đọc response, hash, token rows và số lần sai. |
| Expected | Cả hai `422 IDT-107` cùng thông báo, không tiết lộ tình trạng mã; hash/token giữ nguyên, không cấp phiên. A tăng một lần sai; B không được khôi phục OTP đã hết hạn. |
| Cleanup | Reset Redis/DB/clock. |
| Evidence | Hai response, DB diff, Redis counter/TTL. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC09-04 — Lần sai thứ năm huỷ OTP

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-09; API #8, BR-04, validation §2 |
| Kind / priority | Boundary + security; P0 vì giới hạn OTP |
| Environment | API, Redis, DB |
| Preconditions | OTP còn hạn cho user ACTIVE, bộ đếm sai bằng 4, mật khẩu chưa đổi. |
| Test data | POST reset bằng mã sai thứ năm, sau đó POST bằng mã đúng cũ; cả hai `newPassword="NewPass456"`. |
| Steps | Gọi sai lần thứ năm; kiểm key; gọi lại bằng mã đúng trước khi gửi OTP mới. |
| Expected | Hai lần đều `422 IDT-107`; lần thứ năm xoá key OTP, mã đúng cũ không còn dùng được; hash và phiên cũ giữ nguyên, không có phiên mới. |
| Cleanup | Reset Redis/DB. |
| Evidence | Hai response, key absence, DB hash/token diff. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC11-02 — OAuth state sai hoặc hết hạn bị chặn trước liên kết

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-11; API #5–6, BR-03, validation §6 |
| Kind / priority | Security + boundary; P0 vì chống CSRF OAuth |
| Environment | API, Redis, provider spy, DB, clock |
| Preconditions | Hai state độc lập do #5 phát: A dùng giá trị khác, B hết TTL 10 phút tại T0; provider stub có code hợp lệ. |
| Test data | Callback A `state=forged-state`, B `state=<state B hết hạn>`, provider `google`. |
| Steps | Gọi callback cho A/B; đọc provider calls, user/link/token rows. |
| Expected | Cả hai `401 IDT-109`; không đổi code với provider trước khi state hợp lệ, không tạo user/link/token hoặc audit đăng nhập thành công. |
| Cleanup | Xoá state/stub fixture. |
| Evidence | Hai response, provider spy, Redis/DB diff. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC05-03 — Refresh token không có trong DB bị từ chối không ảnh hưởng family khác

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-05; API #3, BR-02 bước 2 |
| Kind / priority | Security; P0 vì phân biệt token lạ và reuse |
| Environment | API, DB, audit spy |
| Preconditions | U_ACTIVE có family F với token B active; token thử là chuỗi ngẫu nhiên khác mọi hash trong `refresh_tokens`. |
| Test data | POST `/auth/refresh` cookie token thử, header `X-Requested-With: XMLHttpRequest`. |
| Steps | Gọi refresh bằng token thử; đọc response, family F/audit; refresh bằng B hợp lệ. |
| Expected | Token thử nhận `401 IDT-105`, không tạo token/phiên hoặc audit reuse; family F không bị revoke, B vẫn refresh `200`. Không lộ token thử trong log. |
| Cleanup | Revoke family F và reset audit. |
| Evidence | Hai response, DB family diff, audit/log scan. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC06-02 — Hai request đồng thời dùng token A chỉ một lần thành công

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-06; API #3, BR-02 điều kiện token dùng một lần |
| Kind / priority | Concurrency + security; P0 vì rotation race |
| Environment | Hai API instance, DB transaction gate, audit spy |
| Preconditions | Token A active trong family F, chưa có token B; hai request giống hệt có header hợp lệ và cookie A. |
| Test data | Hai POST `/auth/refresh` đồng thời qua `api-a`/`api-b`, giữ tại cổng đọc token để tạo race, rồi cho tiếp tục. |
| Steps | Phát đồng thời; chờ cả hai kết thúc; đọc token rows/audit; thử refresh bằng token mới nếu có. |
| Expected | Tối đa một request `200` tạo một token kế nhiệm; request dùng lại A sau khi A bị revoke nhận `401 IDT-106`, toàn family F bị thu hồi và có một audit `SECURITY_TOKEN_REUSE_DETECTED`. Không có hai token kế nhiệm active hoặc hai lần cấp phiên hợp lệ từ cùng A. |
| Cleanup | Reset family/audit. |
| Evidence | Hai response theo thứ tự commit, DB token/family rows, transaction trace và audit. |
| Design review | Đã review; chưa thực thi |


## Additional validation and concurrency cases

### TC-F1-AC01-03 — Biên email 254 và displayName 2/100 ký tự

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-01; API #1; validation §§3,5; BD USR0502 Sheet 9 |
| Kind / priority | Boundary; P1 (danh tính bị cắt hoặc biên hợp lệ bị từ chối) |
| Environment | API integration, PostgreSQL, mail spy |
| Preconditions | Account email chưa tồn tại; rate limits sạch; validator RFC 5322 dùng cùng contract với API. |
| Test data | Email 254: local part 64 chars + @ + domain labels 63.63.61 chars; email 255: local part 64 chars + @ + labels 63.63.62 chars. displayName `Li` (2), `D` lặp 100 lần, rỗng, 1 và 101 ký tự. Mỗi attempt có email mới; giữ lại số code point thực tế. |
| Steps | Gửi đăng ký với email/name tại biên hợp lệ; sau đó gửi mỗi biến thể vượt/thiếu biên trong request riêng. |
| Expected | Email 254 và displayName 2/100 được chấp nhận, lưu đủ nguyên giá trị sau trim, không cắt. Email 255 và displayName rỗng/1/101 bị từ chối; không tạo user, OTP/mail, token hay session. Mã lỗi field cụ thể chỉ kiểm theo contract response đã chốt; không tự đặt mã. |
| Cleanup | Xóa user và OTP của các request hợp lệ; reset mail/Redis fixture. |
| Evidence | Request cùng độ dài code point, response, DB diff, Redis/mail/token state. |
| Design review | Reviewed 2026-10-28; execution `Chưa thực hiện` |

### TC-F1-AC01-04 — Mật khẩu trùng email hoặc displayName bị từ chối

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-01; API #1; validation §1, IDT-V04 |
| Kind / priority | Security, Boundary; P1 (credential dễ đoán) |
| Environment | API integration, PostgreSQL, Redis/mail spy |
| Preconditions | Email chưa tồn tại; field còn lại hợp lệ. |
| Test data | Email `identity.boundary1@example.test`, displayName `AliceDev123`; password candidate bằng chính xác email và candidate bằng chính xác displayName. Mỗi trial reset DB, cả hai candidate có chữ và số, dài 8–128 nên chỉ vi phạm IDT-V04. |
| Steps | Gửi POST register riêng cho từng password candidate. |
| Expected | Cả hai trả `400 IDT-V04` vì trùng dữ liệu định danh; không tạo user/OTP/mail/session và response/log không echo secret. |
| Cleanup | Xóa mọi artifact phát sinh; reset Redis/mail fixture. |
| Evidence | Response, user/OTP/mail/session diffs và log scan đã che bí mật. |
| Design review | Reviewed 2026-10-28; execution `Chưa thực hiện` |

### TC-F1-AC04-05 — Hai lần đăng nhập sai đồng thời quanh ngưỡng không vượt giới hạn

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-04; API #2; logic BR-01; validation §2 |
| Kind / priority | Concurrency, Security; P1 (race bypass login throttling) |
| Environment | API integration, Redis, fixed clock, parallel request barrier |
| Preconditions | Account ACTIVE; key `(email, IP)` không có counter; Redis khỏe; thời điểm T0. |
| Test data | `race.login@example.test`, `198.51.100.41`; gửi bốn lần sai tuần tự, hai lần sai đồng thời, sau đó thử đúng mật khẩu trong cửa sổ và sau khi hết 15 phút. |
| Steps | Xác nhận bốn phản hồi đầu sai; mở barrier cho hai request sai tiếp theo; thử mật khẩu đúng trước T0+15 phút; thử lại tại/sau T0+15 phút. |
| Expected | Không request sai nào cấp token/session. Khi ngưỡng đạt, lần thử đúng đến sau bị chặn trong cùng cửa sổ; sau hết cửa sổ login đúng được phép. Không đòi thứ tự response cụ thể cho hai request đã bay đồng thời vì DD không chốt cách xử lý request in-flight; kiểm invariant cuối và không cho phép thêm lần dò sau ngưỡng. |
| Cleanup | Xóa rate-limit key, revoke session nếu có và reset account fixture. |
| Evidence | Timestamps/barrier, responses, Redis counter/TTL, session rows/cookies và kết quả retry sau cửa sổ. |
| Design review | Reviewed 2026-10-28; execution `Chưa thực hiện` |

### TC-F1-AC08-02 — Cooldown resend OTP quên mật khẩu theo userId

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-08; API #7; validation §2; BD database F1 §1.9 |
| Kind / priority | Boundary, Security; P1 (bypass OTP throttle hoặc dò account) |
| Environment | API integration, Redis, controllable clock, mail spy |
| Preconditions | U1/U2 ACTIVE, không có OTP/cooldown reset; clock cố định ở T0. |
| Test data | U1 `forgot.cooldown@example.test`, U2 `forgot.other@example.test`, địa chỉ chưa đăng ký `forgot.none@example.test`; cooldown 60 giây theo userId. |
| Steps | Gọi #7 cho U1 tại T0; gọi lại ngay; gọi U2 và địa chỉ chưa đăng ký; thử U1 tại T0+59s và tại T0+60s. |
| Expected | Mọi lời gọi #7, kể cả U1 trong cooldown và email không tồn tại, trả cùng `200 {"message":"Nếu email tồn tại, mã xác nhận đã được gửi."}`. U1 không nhận OTP/mail thứ hai trong cooldown và TTL OTP đầu không bị gia hạn; U2 không bị cooldown của U1. Tại/sau 60 giây U1 được resend. Email không tồn tại không tạo OTP/mail. |
| Cleanup | Xóa OTP/cooldown Redis keys và mail fixtures U1/U2. |
| Evidence | Responses, mail count, Redis key/TTL trước/sau mỗi request, account/token diff. |
| Design review | Reviewed 2026-10-28; oracle chốt 2026-10-27; `Chưa thực hiện` |
