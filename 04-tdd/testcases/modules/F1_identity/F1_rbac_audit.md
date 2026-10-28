# Testcase QA F1 — RBAC, quản trị và audit

> Phạm vi: [TDD F1](../../../modules/F1_identity.md) AC-25–32. Nguồn hợp đồng: [API F1](../../../../03-dd/api/F1_identity.md) #31, #34, #36–40; [Logic F1](../../../../03-dd/logic/F1_identity.md) BR-13–16; [Validation F1](../../../../03-dd/validation/F1_identity.md) §12. Thiết kế, **chưa thực thi**. Prefix API: `/api/v1/identity`.

## Fixture chung

- Đồng hồ cố định T0 `2026-10-27T02:00:00Z`; Redis pub/sub, hai backend instance `api-a`/`api-b`, PostgreSQL, mail/audit spy hoạt động. Mỗi ca dùng snapshot riêng.
- Roles: `R_STUDENT=10000000-0000-4000-8000-000000000001` (`STUDENT`, system), `R_INSTRUCTOR=10000000-0000-4000-8000-000000000002` (`INSTRUCTOR`, system), `R_ADMIN=10000000-0000-4000-8000-000000000003` (`ADMIN`, system), `R_ADMIN_ALT=10000000-0000-4000-8000-000000000004` (`ADMIN`, custom), `R_TEACHER_CUSTOM=10000000-0000-4000-8000-000000000005` (`INSTRUCTOR`, custom, có thể gán user).
- Users: `U_ADMIN_A=20000000-0000-4000-8000-000000000001` ADMIN/ACTIVE, gán `R_ADMIN_ALT`; `U_ADMIN_B=20000000-0000-4000-8000-000000000002` ADMIN/ACTIVE, gán `R_ADMIN`; `U_TEACHER=20000000-0000-4000-8000-000000000003` gán `R_TEACHER_CUSTOM`; `U_STUDENT=20000000-0000-4000-8000-000000000004` gán `R_STUDENT`. Mọi phiên đều hợp lệ, JWT không lưu vào evidence.
- AC-25 dùng lớp `C_RBAC=30000000-0000-4000-8000-000000000001` do `U_TEACHER` sở hữu, `U_STUDENT` còn ghi danh; endpoint đối chứng là `GET /classes/30000000-0000-4000-8000-000000000001/students` (#26). Không có ghi/sửa lớp trong hai ca này.
- Quyền: function `PERMISSION_MATRIX`, action `UPDATE`; function `SYSTEM_AUDIT_LOG`, action `READ`. Ma trận gồm 11 functions × 4 actions theo BR-13; riêng từng ca ghi trạng thái cần thiết của các ô ADMIN.
- Audit seed cho AC-32: `AUDIT_SECRET_1` (`actionType=USER_CREATED`, `targetId=U_STUDENT`), `created_at=2026-10-26T01:00:00Z`, actor `U_ADMIN_A`, có `before_json={"password_hash":"oldhash","note":"before"}`, `after_json={"password_hash":"newhash","note":"after","refreshToken":"raw-token-123","api_secret":"raw-secret-456","passwordResetCode":"otp-123456","token_hash":"hash-token-999","displayName":"QA Actor"}`. `AUDIT_SECRET_2` (`actionType=USER_CREATED`, target là một user fixture khác), `created_at=2026-10-26T02:00:00Z`, actor `U_ADMIN_B`, có `before_json={"SeCrEt":"another-secret","note":"second-before"}`, `after_json={"accessToken":"another-token","note":"second-after","displayName":"QA Admin B"}`. `safeParams` của hai dòng chỉ có tên hiển thị/target ID được BR-19 cho phép, không lấy khoá bí mật. Dùng log fixture tĩnh, không ghi bí mật thật.

| AC | Nhánh | ID | Design review |
| :--- | :--- | :--- | :--- |
| 25 | Tắt quyền và bật lại quyền qua cache liên instance | TC-F1-AC25-01/02 | Reviewed |
| 26 | Từ chối xoá từng role hệ thống | TC-F1-AC26-01 | Reviewed |
| 27 | Chặn role tuỳ biến đang gán; xóa role chưa gán; audit lỗi rollback | TC-F1-AC27-01/02/03 | Reviewed |
| 28 | Chặn gỡ quyền cuối; cho gỡ khi còn ADMIN khác | TC-F1-AC28-01/02 | Reviewed |
| 29 | ADMIN tự khoá sau xác nhận, hủy hoặc thiếu cờ | TC-F1-AC29-01/02/03 | Đã review theo quyết định 2026-10-27 |
| 30 | Giữ ADMIN cuối qua status/role change và tranh chấp đồng thời | TC-F1-AC30-01–04 | Reviewed |
| 31 | ADMIN đọc khi thiếu quyền READ/UPDATE; từ chối non-ADMIN | TC-F1-AC31-01/02 | Reviewed |
| 32 | Che khóa nhạy cảm khi đọc và export audit | TC-F1-AC32-01/02 | Reviewed theo content mô tả đã che và locale vi/en |

### TC-F1-AC25-01 — Tắt quyền được invalidate trên instance đã cache

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-25; API #31; logic BR-13 |
| Kind / priority | Security + recovery; P0 vì thu hồi quyền cần hiệu lực tức thời trên mọi instance |
| Environment | Integration: PostgreSQL primary, Redis pub/sub có cổng giữ delivery tới `api-b`, `api-a`/`api-b`, audit spy và trace truy vấn version. |
| Preconditions | `R_TEACHER_CUSTOM` có `CLASS_MANAGEMENT:READ=true`; `U_TEACHER` sở hữu `C_RBAC` và thấy `U_STUDENT` trong #26. `api-b` đã xử lý một request #26 thành công và có cache hit cho cặp role/function/action. Actor ADMIN có quyền `PERMISSION_MATRIX:UPDATE`. |
| Test data | PATCH `/rbac/matrix/10000000-0000-4000-8000-000000000005/CLASS_MANAGEMENT/READ` body `{"granted":false}` qua `api-a`; giữ message Redis `{roleId,permissionVersion}` chưa đến `api-b` đến sau GET đầu; đối chứng bằng GET #26 của `C_RBAC` qua `api-b` với JWT `U_TEACHER`. |
| Steps | 1. GET #26 qua `api-b`, xác nhận `200` và có `U_STUDENT`. 2. Giữ delivery Redis tới `api-b`; PATCH ô quyền qua `api-a` và chờ response thành công. 3. Trước khi giao message, GET #26 qua `api-b`; đọc version primary/cache và DB/audit. 4. Giao message rồi GET #26 lại, không restart instance. |
| Expected | PATCH commit `granted=false`, tăng `roles.permission_version` đúng một và ghi một audit before/after. GET bắt đầu sau PATCH thành công nhưng trước Redis delivery vẫn đọc version mới từ primary, không dùng grant cache cũ và trả `403 IDT-445`; sau delivery vẫn `403 IDT-445`. Use case đọc được gác không chạy, không có side effect. |
| Cleanup | Khôi phục ô về `true`; xác nhận cache invalidate; reset log fixture. |
| Evidence | PATCH/ba GET responses, transaction commit time, permission/version DB rows, audit row, Redis delay/delivery trace và version/cache decision log. |
| Design review | Reviewed; `403 IDT-445` được chủ dự án chốt 2026-10-28; chưa thực thi. |

### TC-F1-AC25-02 — Bật quyền được invalidate trên instance đã cache deny

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-25; API #31; logic BR-13 |
| Kind / priority | Recovery + security; P1 vì khôi phục đúng quyền sau cấp phép |
| Environment | Integration: PostgreSQL primary, Redis pub/sub có cổng giữ delivery tới `api-b`, `api-a`/`api-b`, audit spy và trace truy vấn version. |
| Preconditions | `R_TEACHER_CUSTOM` có `CLASS_MANAGEMENT:READ=false`; GET #26 của `C_RBAC` bằng `U_TEACHER` bị từ chối trên `api-b`, instance đã cache deny. Actor ADMIN được phép cập nhật ma trận. |
| Test data | PATCH `/rbac/matrix/10000000-0000-4000-8000-000000000005/CLASS_MANAGEMENT/READ` body `{"granted":true}` qua `api-a`; đối chứng bằng GET #26 của `C_RBAC` qua `api-b`. |
| Steps | 1. GET #26 qua `api-b`, xác nhận `403 IDT-445` và cache deny đã được nạp. 2. Giữ Redis delivery tới `api-b`; PATCH bật ô qua `api-a` và chờ response thành công. 3. Trước khi giao message, GET #26 qua `api-b` và đọc version primary/cache. 4. Giao message rồi GET #26 lại, không restart. |
| Expected | Lần gọi đầu trả `403 IDT-445`. PATCH commit `granted=true`, tăng `roles.permission_version` đúng một và ghi audit `false/true`. GET bắt đầu sau PATCH thành công nhưng trước Redis delivery đọc version mới từ primary, loại cache deny cũ và trả `200` có `U_STUDENT`; sau delivery vẫn `200`. |
| Cleanup | Khôi phục ô false; reset audit và cache. |
| Evidence | PATCH/ba GET responses, commit time, permission/version DB rows, audit, Redis delay/delivery trace và cache/version decision log. |
| Design review | Reviewed; `403 IDT-445` được chủ dự án chốt 2026-10-28; chưa thực thi. |

### TC-F1-AC26-01 — Từ chối xoá mọi role hệ thống

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-26; API #34; logic BR-14 |
| Kind / priority | Security; P0 vì không được phá role nền tảng |
| Environment | API, DB, audit spy |
| Preconditions | `R_STUDENT`, `R_INSTRUCTOR`, `R_ADMIN` tồn tại với `is_system=true`; `U_STUDENT` và `U_ADMIN_A` đang gán role tương ứng; actor `U_ADMIN_B` có quyền quản lý role. |
| Test data | Gọi DELETE `/rbac/roles/{roleId}` lần lượt với ba UUID system ở fixture. |
| Steps | Với mỗi role, chụp role, user assignments và permission rows; DELETE; đọc lại các hàng và thử gọi một endpoint được role đó cho phép. |
| Expected | Mỗi lần DELETE trả `422 IDT-421`; role vẫn tồn tại, assignments và permissions không đổi, user hiện có vẫn truy cập được endpoint trước đó. Không xóa dở hoặc audit một lần xóa thành công. |
| Cleanup | Không cần; reset snapshot. |
| Evidence | Ba response, DB snapshot diff, endpoint authorization trước/sau, audit diff. |
| Design review | Reviewed; `422 IDT-421` được chủ dự án chốt 2026-10-28; chưa thực thi. |

### TC-F1-AC27-02 — Xóa được role tuỳ biến không có user gán

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-27 (đối chiếu điều kiện chặn); API #34; logic BR-14 |
| Kind / priority | Normal + boundary; P1 vì kiểm tra role được quản lý đúng sau khi hết assignment |
| Environment | API, DB, audit spy |
| Preconditions | Tạo `R_CUSTOM_EMPTY=10000000-0000-4000-8000-000000000006`, `base_category=INSTRUCTOR`, `is_system=false`, không có users tham chiếu; có permission rows cho function/action seed. Actor ADMIN có quyền quản lý role. |
| Test data | DELETE `/rbac/roles/10000000-0000-4000-8000-000000000006`. |
| Steps | Đếm assignment bằng role ID; DELETE; truy vấn roles, permissions và assignments. |
| Expected | `204` không body; role và các permission rows của role không còn (`permissions.role_id ON DELETE CASCADE` theo DB F1 §1.4); không user nào bị chuyển role. Đúng một audit `ROLE_DELETED` có actor/target/before và commit cùng giao dịch xoá. |
| Cleanup | Xóa fixture nếu còn; reset snapshot. |
| Evidence | Response, role/permission/assignment diff, audit diff. |
| Design review | Reviewed; `204` và audit cùng giao dịch được chủ dự án chốt 2026-10-28; chưa thực thi. |

### TC-F1-AC27-01 — Không xoá role tuỳ biến khi còn user gán

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-27; API #34; logic BR-14 |
| Kind / priority | Abnormal + boundary; P0 vì phải giữ nguyên liên kết user-role |
| Environment | API, DB, audit spy |
| Preconditions | `R_TEACHER_CUSTOM` có `is_system=false`, ít nhất `U_TEACHER` gán role; permission rows hiện có. Actor ADMIN được phép quản lý role. |
| Test data | DELETE `/rbac/roles/10000000-0000-4000-8000-000000000005`. |
| Steps | Ghi nhận role, assignment và permissions; DELETE; đọc lại cả ba và gọi endpoint cần quyền theo role của U_TEACHER. |
| Expected | `422 IDT-422`; role, user assignment và permissions nguyên vẹn; quyền của U_TEACHER không đổi; không có bản ghi audit thành công/side effect xóa. |
| Cleanup | Reset snapshot. |
| Evidence | Response, DB diff, authorization result, audit diff. |
| Design review | Reviewed; `422 IDT-422` được chủ dự án chốt 2026-10-28; chưa thực thi. |

### TC-F1-AC27-03 — Ghi audit lỗi rollback xoá role và quyền

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-27; API #34, logic BR-14/BR-19; `DEC-2026-1028-qa-f1-role-delete-audit-failure`. |
| Kind / priority | Recovery/transaction; P0 vì không được mất role khi nhật ký bắt buộc hỏng. |
| Environment | F1 API + PostgreSQL với fault hook tại bước ghi `ROLE_DELETED`. |
| Preconditions | `R_CUSTOM_EMPTY=10000000-0000-4000-8000-000000000006` là role tuỳ biến không user gán; có hai permission rows; ADMIN `U_ADMIN_B` được phép xoá. Chụp snapshot role, permissions, assignments và audit trước lệnh. |
| Test data | DELETE `/rbac/roles/10000000-0000-4000-8000-000000000006`; audit writer ném lỗi xác định trước khi insert `ROLE_DELETED`. |
| Steps | Bật fault hook; gọi DELETE đúng một lần; đọc response và snapshot DB sau khi transaction kết thúc; tắt hook rồi gọi lại lệnh để kiểm đối chứng thành công. |
| Expected | Lần đầu trả `503 IDT-446`; role, hai permission rows và assignments giữ nguyên, không có audit `ROLE_DELETED` hoặc response `204` giả. Lần gọi lại sau khi bỏ lỗi trả `204`, role/quyền bị xoá và có đúng một audit `ROLE_DELETED`. |
| Cleanup | Tắt fault hook và khôi phục snapshot disposable DB. |
| Evidence | Response cả hai lần, transaction trace, role/permission/assignment hashes và audit delta. |
| Design review | Reviewed theo quyết định owner 2026-10-28; chưa thực thi. |

### TC-F1-AC28-01 — Không gỡ ô UPDATE cuối cùng trong mọi role ADMIN

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-28; API #31; logic BR-13 |
| Kind / priority | Security + boundary; P0 vì mất toàn quyền quản lý ma trận sẽ khóa hệ thống |
| Environment | API, DB, audit spy, Redis pub/sub |
| Preconditions | Trong mọi role có `base_category=ADMIN`, chỉ `R_ADMIN_ALT` có `PERMISSION_MATRIX:UPDATE=true`; `R_ADMIN` có ô false. Actor `U_ADMIN_A` có quyền cập nhật ô qua `R_ADMIN_ALT`. |
| Test data | PATCH `/rbac/matrix/10000000-0000-4000-8000-000000000004/PERMISSION_MATRIX/UPDATE` body `{"granted":false}`. |
| Steps | Đọc trạng thái mọi ô cùng function/action thuộc roles ADMIN; PATCH tắt ô duy nhất; đọc response, DB, audit và Redis. |
| Expected | Máy chủ từ chối với `422 IDT-420`; ô duy nhất vẫn true, các ô khác không đổi; không ghi audit thay đổi thành công và không publish invalidate cho thay đổi không xảy ra. Actor vẫn có quyền quản lý ma trận. |
| Cleanup | Reset snapshot. |
| Evidence | Response, query ma trận trước/sau, audit/Redis diff, GET matrix. |
| Design review | Reviewed. |

### TC-F1-AC28-02 — Gỡ một ô UPDATE khi role ADMIN khác vẫn giữ quyền

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-28 (đối chiếu với điều kiện “cuối cùng”); API #31; logic BR-13 |
| Kind / priority | Normal + boundary; P1 vì phải cho phép thay đổi khi invariant vẫn được giữ |
| Environment | API, DB, audit spy, Redis pub/sub |
| Preconditions | Hai role `base_category=ADMIN` có quyền UPDATE: `R_ADMIN` và `R_ADMIN_ALT`; actor `U_ADMIN_A` đang được cấp quyền qua `R_ADMIN_ALT`. |
| Test data | PATCH `/rbac/matrix/10000000-0000-4000-8000-000000000004/PERMISSION_MATRIX/UPDATE` body `{"granted":false}`. |
| Steps | Ghi nhận hai ô; U_ADMIN_A PATCH tắt quyền của R_ADMIN_ALT; đọc response, DB, audit và version. Sau response, U_ADMIN_A thử PATCH cùng ô về `true` và bị từ chối; U_ADMIN_B PATCH cùng ô về `true` để chứng minh role còn lại được phép. |
| Expected | PATCH đầu thành công; ô R_ADMIN_ALT false, ô R_ADMIN vẫn true; audit ghi đúng một lần và permission version tăng. U_ADMIN_A thuộc R_ADMIN_ALT mất quyền UPDATE, PATCH thứ hai trả `403 IDT-445` và không ghi thêm. U_ADMIN_B thuộc R_ADMIN PATCH về `true` thành công, ghi audit thứ hai và tăng version lần nữa. Không trả `422 IDT-420` cho PATCH đầu vì toàn hệ thống còn role ADMIN có quyền. |
| Cleanup | Khôi phục hai ô như ban đầu. |
| Evidence | Response, query tất cả role ADMIN × function/action, audit và Redis trace. |
| Design review | Reviewed. |

### TC-F1-AC29-01 — ADMIN tự khoá sau xác nhận khi còn ADMIN khác

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-29; API #37; logic BR-15 |
| Kind / priority | Security; P0 vì khóa tài khoản quản trị và phiên |
| Environment | API, DB, audit spy, hai tài khoản ADMIN hoạt động |
| Preconditions | `U_ADMIN_A` có phiên hợp lệ và ACTIVE; `U_ADMIN_B` cũng ADMIN/ACTIVE. Giao diện đã hiện cảnh báo tự khoá và người dùng xác nhận lần nữa. |
| Test data | PATCH `/admin/users/20000000-0000-4000-8000-000000000001/status` bằng chính JWT `U_ADMIN_A`, body `{"targetStatus":"DEACTIVATED","reason":"QA self lock","notifyByEmail":false,"acknowledgeSelfImpact":true}`. |
| Steps | Chụp user, phiên, audit; xác nhận cảnh báo ở UI; gửi yêu cầu qua API; kiểm response, trạng thái user, phiên và audit. |
| Expected | Thao tác thành công vì U_ADMIN_B còn ACTIVE; U_ADMIN_A thành `DEACTIVATED` với `deactivation_source=ADMIN`, phiên cũ mất hiệu lực, U_ADMIN_B vẫn quản trị được; audit ghi một lần. Không trả `IDT-430/IDT-431` hoặc gửi email vì `notifyByEmail=false`. |
| Cleanup | Khôi phục U_ADMIN_A ACTIVE và phiên trong snapshot. |
| Evidence | Request/response, DB/token/audit diff, mail spy. |
| Design review | Đã review theo DD API #37 và quyết định 2026-10-27; chưa thực thi. |

### TC-F1-AC29-02 — Hủy cảnh báo tự khoá không gửi yêu cầu

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-29; BD ADM0201 Q6/Sheet 9 NO 6–8; API #37 |
| Kind / priority | Security + abnormal; P1 vì lựa chọn hủy phải giữ nguyên tài khoản quản trị |
| Environment | UI/E2E với network spy, API, DB/token/audit spy |
| Preconditions | U_ADMIN_A và U_ADMIN_B đều ADMIN/ACTIVE; U_ADMIN_A đã đăng nhập. Màn quản trị có cảnh báo trước khi tự khóa và bước xác nhận lần nữa. |
| Test data | Mở thao tác khóa `U_ADMIN_A` bằng chính U_ADMIN_A; chọn Hủy ở cảnh báo. |
| Steps | Ghi nhận số request status-change; mở dialog/cảnh báo; chọn Hủy; chờ UI kết thúc thao tác; kiểm network, DB, phiên và audit. |
| Expected | Không gửi `ChangeUserStatus`; U_ADMIN_A vẫn ACTIVE, phiên hiện tại vẫn dùng được, U_ADMIN_B không đổi; không có audit lock. Tên/shape của cờ xác nhận không cần thiết để oracle hủy UI này. |
| Cleanup | Không cần ngoài reset snapshot. |
| Evidence | UI capture, network trace, DB/session/audit diff. |
| Design review | Reviewed; chưa thực thi. |

### TC-F1-AC29-03 — Thiếu hoặc từ chối cờ xác nhận không tự khoá

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-29; API #37 §0.6; logic BR-15; validation §13; DEC-2026-1027-qa-f1-self-impact-ack |
| Kind / priority | Security + abnormal; P0 vì API phải kiểm xác nhận máy chủ |
| Environment | API, DB, token/audit/mail spy |
| Preconditions | U_ADMIN_A và U_ADMIN_B đều ADMIN/ACTIVE; A gọi trên chính mình, B vẫn đủ để không vướng bất biến ADMIN cuối. |
| Test data | Hai request trên hai snapshot riêng: `{"targetStatus":"DEACTIVATED","reason":"QA","notifyByEmail":false}` và cùng body có `"acknowledgeSelfImpact":false`. |
| Steps | Gửi mỗi request bằng JWT A; đọc response, user, phiên, audit và mail. |
| Expected | Mỗi request `400 IDT-V21`; A vẫn ACTIVE, phiên A còn dùng được, không có audit thay đổi hoặc email, B không đổi. Không trả `IDT-432` vì vẫn còn ADMIN khác. |
| Cleanup | Reset hai snapshot. |
| Evidence | Hai response, DB/token/audit/mail diff. |
| Design review | Đã review; chưa thực thi |

### TC-F1-AC30-01 — Từ chối khóa ADMIN hoạt động cuối cùng

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-30; API #37; logic BR-15 |
| Kind / priority | Security + boundary; P0 vì phải luôn còn ít nhất một ADMIN hoạt động |
| Environment | API integration, PostgreSQL transaction/row lock, audit spy |
| Preconditions | Chỉ `U_ADMIN_A` có role `base_category=ADMIN` và `status=ACTIVE`; `U_ADMIN_B` được đặt `DEACTIVATED` hoặc không có role ADMIN. U_ADMIN_A có phiên A hợp lệ. |
| Test data | PATCH #37 cho `U_ADMIN_A` bằng phiên A, body `{"targetStatus":"DEACTIVATED","reason":"QA last admin","notifyByEmail":false,"acknowledgeSelfImpact":true}`. |
| Steps | Chụp số ADMIN ACTIVE, user, phiên; xác nhận self-impact nếu UI yêu cầu; gửi request; đọc lại DB và thử dùng phiên A/login; kiểm audit. |
| Expected | Request bị từ chối `422 IDT-432`; U_ADMIN_A vẫn ACTIVE và đăng nhập/quản trị được; không thu hồi phiên, không ghi audit thành công. |
| Cleanup | Reset snapshot. |
| Evidence | Response, DB count/state, login/auth result, session/audit diff. |
| Design review | Reviewed. |

### TC-F1-AC30-03 — ADMIN cuối tự yêu cầu khóa qua phiên khác vẫn bị chặn

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-30; API #37; logic BR-15 |
| Kind / priority | Security + boundary; P0 vì phải giữ lối quản trị cuối cùng |
| Environment | API, DB, session/audit spy |
| Preconditions | Chỉ U_ADMIN_A là ADMIN/ACTIVE; U_ADMIN_A có hai phiên A và B hợp lệ. Không có user khác thuộc base_category ADMIN ở ACTIVE. |
| Test data | Phiên B của U_ADMIN_A PATCH `/admin/users/20000000-0000-4000-8000-000000000001/status` với `{"targetStatus":"DEACTIVATED","reason":"QA last admin second session","notifyByEmail":false,"acknowledgeSelfImpact":true}`. |
| Steps | Chụp role/status/session; gửi status-change; đọc DB và thử dùng phiên A/B cùng login. |
| Expected | `422 IDT-432`; status vẫn ACTIVE, cả phiên A/B còn hiệu lực, login/admin access vẫn hoạt động, không có audit thao tác thành công. Không tự áp dụng ngoại lệ tự khóa AC-29 vì tại đây không còn ADMIN ACTIVE khác. |
| Cleanup | Reset snapshot. |
| Evidence | Response, DB admin count/status, hai phiên và audit diff. |
| Design review | Reviewed theo BR-15 và cờ xác nhận đã chốt; chưa thực thi. |

### TC-F1-AC30-04 — Hai thao tác đồng thời không thể khóa/hạ role cả hai ADMIN

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-30; API #36–37; logic BR-15 concurrency lock |
| Kind / priority | Concurrency + security; P0 vì hai giao dịch đua nhau không được để ADMIN ACTIVE = 0 |
| Environment | Integration: PostgreSQL, API, hai actor ADMIN, transaction tracing, audit spy |
| Preconditions | U_ADMIN_A và U_ADMIN_B là hai ADMIN/ACTIVE duy nhất; mỗi người có phiên hợp lệ, quyền cập nhật và là mục tiêu của request còn lại. Khóa dòng admin như BR-15 được bật. |
| Test data | U_ADMIN_B PATCH `/admin/users/20000000-0000-4000-8000-000000000001/status` và U_ADMIN_A PATCH `/admin/users/20000000-0000-4000-8000-000000000002/status`, cùng body `{"targetStatus":"DEACTIVATED","reason":"QA concurrent admin","notifyByEmail":false}`. Mỗi actor nhắm người kia nên không cần `acknowledgeSelfImpact`. Dùng barrier sau auth, trước bước lock/count. |
| Steps | Chụp trạng thái ban đầu; phóng đồng thời hai request; chờ cả hai hoàn tất; đọc DB, phiên và audit. |
| Expected | Không có trạng thái cuối nào có 0 ADMIN ACTIVE. Transaction tuần tự sau khi thấy thay đổi đầu tiên bị từ chối `422 IDT-432`; tối đa một thao tác thành công, tối thiểu một ADMIN ACTIVE. Audit chỉ ghi thao tác thành công. Nếu hệ thống chọn thứ tự giao dịch khác, cùng invariant vẫn phải đúng. |
| Cleanup | Khôi phục hai tài khoản/role và phiên. |
| Evidence | Request timing/barrier trace, response pair, transaction lock trace, DB final state, audit rows. |
| Design review | Reviewed. |

### TC-F1-AC30-02 — Từ chối hạ vai trò ADMIN cuối cùng

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-30; API #36; logic BR-15 |
| Kind / priority | Security + boundary; P0 vì phải giữ một ADMIN hoạt động sau đổi role |
| Environment | API integration, PostgreSQL transaction/row lock, audit spy |
| Preconditions | Chỉ `U_ADMIN_A` là ADMIN/ACTIVE; có role đích `R_TEACHER_CUSTOM` không thuộc ADMIN; U_ADMIN_A có phiên A. |
| Test data | `ChangeUserRole` cho U_ADMIN_A từ phiên A sang role UUID `10000000-0000-4000-8000-000000000005`, với self-impact acknowledgement theo giao diện/hợp đồng hiện hành. |
| Steps | Chụp role/count; qua UI hiện cảnh báo tự hạ role và xác nhận hoặc gửi request qua phiên A; đọc lại user/role/permissions; thử endpoint quản trị và kiểm audit. |
| Expected | Request bị từ chối `422 IDT-432`; role U_ADMIN_A vẫn ADMIN, count ACTIVE ADMIN vẫn 1, quản trị vẫn dùng được; không ghi audit thành công. Ràng buộc ADMIN cuối áp dụng dù có self-impact acknowledgement. |
| Cleanup | Reset snapshot. |
| Evidence | Response, DB role/count, authorization result, audit diff. |
| Design review | Reviewed. |

### TC-F1-AC31-02 — Không cho non-ADMIN đọc audit qua endpoint admin

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-31; API #39, #64–66: JWT ADMIN; logic/API §0.7.2 |
| Kind / priority | Security; P1 vì ngoại lệ bypass permission matrix chỉ dành cho ADMIN |
| Environment | API, DB audit fixture |
| Preconditions | `U_STUDENT` có session hợp lệ, role base_category STUDENT; có audit row trong phạm vi truy vấn. |
| Test data | GET `/admin/audit-logs?categoryCode=ALL&limit=50` bằng JWT U_STUDENT. |
| Steps | Gọi endpoint; đọc response và kiểm tra không có item/metadata audit nào rò rỉ. |
| Expected | `403 IDT-447` từ guard `baseCategory=ADMIN` trước khi đọc nội dung; response không chứa audit row, actor, target hoặc safeParams. Đây không phải lỗi thiếu ô `Function:Action` (`IDT-445`). |
| Cleanup | Reset snapshot. |
| Evidence | Response đã redact, security/audit log và DB fixture. |
| Design review | Reviewed; `403 IDT-447` được chủ dự án chốt 2026-10-28; chưa thực thi. |

### TC-F1-AC31-01 — ADMIN đọc được audit dù ma trận READ tắt

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-31; API #39; logic/API §0.7.2 |
| Kind / priority | Security; P0 vì audit là quyền giám sát độc lập với quyền cấu hình |
| Environment | API, DB audit fixture |
| Preconditions | `U_ADMIN_A` gán `R_ADMIN_ALT` và `U_ADMIN_B` gán `R_ADMIN` (cả hai `base_category=ADMIN`); trên cả hai role đặt `SYSTEM_AUDIT_LOG:READ=false`, `SYSTEM_AUDIT_LOG:UPDATE=false`, `PERMISSION_MATRIX:UPDATE=false`; có hai audit row fixture trong phạm vi ngày. |
| Test data | GET `/admin/audit-logs?categoryCode=ALL&limit=50` bằng JWT U_ADMIN_A. |
| Steps | Gọi endpoint bằng JWT U_ADMIN_A và U_ADMIN_B; đọc item/nextCursor và xác minh cả hai fixture rows xuất hiện ở mỗi kết quả. |
| Expected | Cả hai ADMIN trả `200 {items,nextCursor}` theo API; họ đọc log trong phạm vi quản trị dù ba permission nêu trên false trên role tương ứng. Kết quả chứa các row phù hợp; không yêu cầu quyền cập nhật ma trận hoặc cấu hình. |
| Cleanup | Xóa audit fixture nếu tạo riêng. |
| Evidence | HTTP response (đã che dữ liệu), permission rows và audit DB fixture. |
| Design review | Reviewed. |

### TC-F1-AC32-01 — CSV che toàn bộ giá trị khóa nhạy cảm, giữ cấu trúc còn lại

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-32; API #40; logic BR-16; validation §12 |
| Kind / priority | Security; P0 vì export có nguy cơ làm lộ credential/token |
| Environment | API, DB, streaming CSV capture, audit spy |
| Preconditions | `AUDIT_SECRET_1` nằm trong thời gian lưu; actor ADMIN có thể export; request ngày hợp lệ theo T0/UTC+7 và retention. |
| Test data | GET `/admin/audit-logs/export?format=csv&columns=time,actor,service,category,content,id&categoryCode=ALL&dateFrom=2026-10-26&dateTo=2026-10-26` cho hai dòng `AUDIT_SECRET_1/2`. Gửi lần lượt `Accept-Language: vi`, `en` và không có header; raw before/after giữ mọi marker bí mật trong catalog. Export audit mới tại T0 ngày 27 nằm ngoài bộ lọc nên không tự thêm dòng vào tệp. |
| Steps | Gửi ba export trên cùng snapshot; parse CSV theo cột; đối chiếu `content` với formatter `messageKey=USER_CREATED` và `safeParams` đã che theo từng locale; quét byte output tìm marker raw JSON/bí mật; kiểm audit export và response headers. |
| Expected | Mỗi response là `text/csv; charset=utf-8`, header đúng thứ tự `time,actor,service,category,content,id`, hai dòng nguồn xuất hiện đúng một lần theo thứ tự API. `content` là câu mô tả `USER_CREATED` với target/tên hiển thị an toàn; `vi` và `en` dùng bản dịch tương ứng, thiếu header giống `vi`. Không cell nào chứa raw `before_json`/`after_json`, khoá JSON hay chuỗi `oldhash`, `newhash`, `raw-token-123`, `raw-secret-456`, `otp-123456`, `hash-token-999`, `another-secret`, `another-token`. Nội dung dùng cùng `safeParams` đã che với #39; không có giá trị bí mật trong CSV hoặc log export. |
| Cleanup | Xóa fixture log và export audit riêng. |
| Evidence | Raw CSV lưu trong vùng test giới hạn truy cập, SHA-256, response headers, query DB và audit record; evidence chia sẻ phải redact fixture strings. |
| Design review | Reviewed theo quyết định owner 2026-10-28 về content mô tả và locale; chưa thực thi. |

### TC-F1-AC32-02 — Đọc danh sách audit cũng che before/after nhạy cảm

| Field | Value |
| :--- | :--- |
| AC / DD source | F1 AC-32; API #39; logic BR-16/BR-19; API §0.7.2 |
| Kind / priority | Security; P0 vì cùng dữ liệu audit được hiển thị trước khi export |
| Environment | API, DB audit fixture |
| Preconditions | `AUDIT_SECRET_1` và `AUDIT_SECRET_2` trong phạm vi retention và truy vấn; actor ADMIN. Serializer list #39 dùng safeParams theo BR-19/16. |
| Test data | GET `/admin/audit-logs?categoryCode=ALL&limit=50&dateFrom=2026-10-26&dateTo=2026-10-27` với hai row fixture gồm `password_hash`, `refreshToken`, `api_secret`, `passwordResetCode`, `token_hash`, `SeCrEt`, `accessToken`. |
| Steps | Gọi list; kiểm tra DTO item và mọi trường hiển thị derived từ before/after; quét serialized response cho các secret marker. |
| Expected | Hai fixture rows xuất hiện trong phạm vi truy vấn, không trả raw before_json/after_json. Nếu dữ liệu nhạy cảm được hiển thị từ before/after, giá trị thay bằng `***` theo cùng quy tắc: khóa `password_hash`, `token_hash`, và mọi tên khóa khớp `(?i)password|secret|token`; trường không nhạy cảm giữ nguyên. Không có `oldhash`, `newhash`, `raw-token-123`, `raw-secret-456`, `otp-123456`, `hash-token-999`, `another-secret`, `another-token` ở bất kỳ byte nào response. |
| Cleanup | Xóa fixture row hoặc reset snapshot. |
| Evidence | Raw response giới hạn truy cập, scan marker kết quả, DTO/DB diff; evidence chia sẻ phải redact fixture strings. |
| Design review | Reviewed cho serializer chung và no-leak; chưa thực thi. |
