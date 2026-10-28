# F1 — inventory nhánh AC-01–24

Nguồn: [TDD F1](../../../modules/F1_identity.md), DD F1 API/logic/validation/jobs. Bảng này hỗ trợ audit **thiết kế** các ca trong bốn cụm nền tảng; không phải kết quả chạy. `N/A` nghĩa là AC/DD không định nghĩa nhánh độc lập trong phạm vi cụm đó. Các AC mở rộng AC-33–78 được kiểm riêng trong file tương ứng.

| AC | Normal | Abnormal / boundary | Security / scope | Concurrency / recovery | Ghi chú audit |
| :--- | :--- | :--- | :--- | :--- | :--- |
| 01 | AC01-01 | AC01-02; AC01-03/04: email/displayName bounds and password identity match | No password/OTP leak: AC01-01/03/04 | N/A | Xem [cụm biên xác thực](F1_auth_failure_boundaries.md). |
| 02 | N/A | AC02-01: trùng email khác hoa/thường | Không tạo tài khoản/OTP thứ hai | Unique DB | Đã có oracle lỗi và side effect. |
| 03 | AC03-01 | Sai mật khẩu ở AC04; trạng thái pending ở AC41 | Cookie/phiên: AC03-01 | N/A | Vai trò giảng viên riêng ở AC64. |
| 04 | N/A | AC04-01/02/04; AC04-05 concurrent failures at threshold | No token when blocked | AC04-03 Redis fail-open; AC04-05 concurrency | Xem [AC04-05](F1_auth_failure_boundaries.md); thứ tự request đã vào luồng không được bảo đảm. |
| 05 | AC05-01 | AC05-02: token hết hạn tự nhiên; AC05-03: token không tồn tại | Rotation và cookie | N/A | Đã có oracle lỗi và side effect. |
| 06 | N/A | AC06-01: token bị reuse | Thu hồi family/audit | AC06-02: hai refresh đồng thời bằng A | Đã có đối chứng family và audit. |
| 07 | N/A | AC07-01: thiếu header | Không tiêu thụ token | AC07-01: retry header đúng | Đã có oracle DB/phiên. |
| 08 | AC08-01: account exists | AC08-01: unknown email; AC08-02 resend cooldown | Same response; no account enumeration | AC08-02 cooldown is user-scoped | Xem [AC08-02](F1_auth_failure_boundaries.md); response 200/body chung đã chốt. |
| 09 | AC09-01/02 | AC09-03/04: sai, hết hạn, ngưỡng sai | Thu hồi phiên cũ | N/A | Verify OTP trước reset ở AC54. |
| 10 | N/A | AC10-01: OAuth-only | Không tự đặt mật khẩu | N/A | Đã có oracle DB/provider. |
| 11 | OAuth verified ở AC68 | AC11-01/02: email chưa verify, state lỗi | Không liên kết sai | N/A | Provider callback thành công ở AC68. |
| 12 | AC12-01: trường hồ sơ hợp lệ | N/A | Mass assignment role/status | N/A | Validation trường hồ sơ ngoài phạm vi AC này, cần kiểm trong ca màn hồ sơ. |
| 13 | AC13-01 | AC13-02/03: OTP sai, email bị chiếm | Chỉ chủ tài khoản đổi email | AC13-03: tranh chấp | Hạn mức/gửi lại cùng email ở AC70. |
| 14 | AC14-01 | AC14-02 wrong current password | Keep current session; revoke other session only on successful change | N/A | Xem [AC14-02](F1_profile_lifecycle.md); `422 IDT-115` đã chốt. |
| 15 | AC15-01 | N/A | JWT hợp lệ, email bảo mật/audit | N/A | AC15-01 có negative thiếu/hết hạn JWT và positive OAuth session. |
| 16 | N/A | AC16-01: xác nhận sai kể cả khác hoa/thường | Không đổi user/phiên | N/A | Đã có oracle side effect. |
| 17 | AC17-01 | N/A | Access cũ chặn ngay, lịch sử còn | N/A | Khôi phục trong ân hạn ở AC55. |
| 18 | AC18-01 | AC18-01: đúng mốc 30 ngày và trước mốc | PII/OAuth bị ẩn, lịch sử giữ | AC18-02: rollback lô và retry | Đã có oracle job/DB. |
| 18b | N/A | AC18b-01: Admin lock quá 30 ngày | Không ẩn danh Admin lock | N/A | Đã có đối chứng SELF dương. |
| 19 | N/A | AC19-01: mã hết hạn | Không ghi danh hoặc lộ lớp | N/A | Join hợp lệ mới ở AC20-02. |
| 20 | AC20-02: join mới | AC20-01: join trùng idempotent | Chỉ lớp theo mã hợp lệ | N/A | Có đối chứng số dòng/ngày tham gia. |
| 20b | AC20b-01: vào lại | N/A | Giữ lịch sử | Recovery sau bị gỡ | Có đối chứng dòng cũ. |
| 21 | AC21-01 | AC21-01: gọi lặp | Không xoá history F4 | AC21-02: publish lỗi rồi gọi lại phát event với `occurredAt` gốc | Đã chốt 2026-10-27, có đối chứng giữ history F4 và đọc current scope qua ClassScopeReadPort; F4 không tiêu thụ event xoá dữ liệu. |
| 22 | N/A | AC22-01: ngoài owner | Không đổi lớp/history/event | N/A | Có mã lỗi theo API §0.4.3. |
| 23 | AC23-02: có lượt trong 7 ngày | AC23-01: 8 ngày không có lượt trong lớp | Không dùng lượt ngoài lớp | N/A | Cửa sổ trượt, không theo tuần lịch. |
| 24 | AC24-01/02/03 | ABSENT>WATCH; INSUFFICIENT_DATA>ABSENT; NEEDS_SUPPORT>WATCH | Nguồn số liệu trong lớp | N/A | ON_TRACK đối chứng ở AC23-02. |

## Kết quả bổ sung và oracle còn mở

- **Coverage additions:** AC01-03/04, AC04-05 and AC08-02 are in [auth boundaries](F1_auth_failure_boundaries.md); AC14-02 is in [profile lifecycle](F1_profile_lifecycle.md). AC-05 missing token and AC-06 concurrent refresh remain in [auth boundaries](F1_auth_failure_boundaries.md).
- **Oracle đã chốt 2026-10-27:** AC-08 trong cooldown trả cùng 200/body chung, không gửi lại; AC-14 mật khẩu cũ sai trả `422 IDT-115`, không đổi phiên/DB.
- **Oracle AC-21 đã chốt:** khi RabbitMQ publish lỗi sau commit, gọi lại endpoint phát lại event với `occurredAt=left_at` gốc; xem [TC-F1-AC21-02](F1_class_membership_status.md).
- **Oracle AC-25 đã chốt:** thiếu ô `Function:Action` theo version primary ngay sau PATCH thành công trả `403 IDT-445`; xem [TC-F1-AC25-01/02](F1_rbac_audit.md).
- **Trạng thái chạy:** tất cả ca vẫn `Chưa thực hiện`; chưa có run record.
