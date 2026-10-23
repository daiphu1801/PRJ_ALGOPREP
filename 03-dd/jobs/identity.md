# DD — Jobs của module `identity`

**Phiên bản**: 0.1 (2026-10-23) · **Module**: `identity` (F1) · **Nguồn**: `DEC-2026-1023-db-actor-review-fixes`

Tài liệu này đặc tả các tác vụ nền của `identity`: khi nào chạy, chọn dòng nào, xử lý theo lô ra sao, lỗi thì làm gì. Logic nghiệp vụ gốc nằm ở `03-dd/logic/identity.md`; file này không lặp lại, chỉ cố định phần vận hành. Các job của hai module khác nằm ở `03-dd/jobs/judge-orchestration.md` và `03-dd/jobs/ai-review.md` (do tài liệu riêng của từng module quản lý).

Bảng 1 liệt kê toàn bộ job của `identity`. Lịch chạy là giá trị đề xuất `[SoT: Suy luận]` vì RD/BD chưa nêu giờ cụ thể, trừ chỗ có ghi nguồn.

| Mã | Job | Kích hoạt | Bảng đụng tới | Nguồn |
| :--- | :--- | :--- | :--- | :--- |
| JOB-ID-01 | Runner khởi tạo ADMIN đầu tiên | khi khởi động ứng dụng | `users` | `02-bd/database/identity.md:300`; BR-23 |
| JOB-ID-02 | Ẩn danh hoá tài khoản tự xoá | định kỳ hằng ngày | `users`, `oauth_identities` | F1-16; BR-08 |
| JOB-ID-03 | Dọn feed hoạt động quá 90 ngày | định kỳ hằng ngày | `identity_recent_activity` | `02-bd/architecture/identity.md:130` |
| JOB-ID-04 | Dọn refresh token hết hạn/thu hồi quá 6 tháng | định kỳ hằng ngày | `refresh_tokens` | `02-bd/database/identity.md:102-103` |
| JOB-ID-05 | Dọn nhật ký hệ thống quá hạn | định kỳ hằng ngày | `system_audit_logs` | BR-20 (`03-dd/logic/identity.md:41-46`) |

Nguyên tắc chung cho JOB-ID-02 đến JOB-ID-05: chạy bằng `@Scheduled` trong một instance (nếu triển khai nhiều instance thì dùng khoá tư vấn Postgres `pg_try_advisory_xact_lock(hashtext(<mã job>))` đầu giao dịch, không lấy được khoá thì bỏ lượt; chốt 2026-10-23, `DEC-2026-1023-open-questions-closed`, cùng cơ chế với `judge-orchestration` và `ai-review`); không tạo dòng `system_audit_logs` vì không có người thao tác (`actor_user_id` NOT NULL); mỗi lô một giao dịch riêng để lô lỗi không kéo lô đã xong; ghi log mức INFO số dòng xử lý mỗi lần chạy. Việc chạy job không phụ thuộc đường AI (`CLAUDE.md`: AI chết không được ảnh hưởng F1-F4).

## 1. JOB-ID-01 — Runner khởi tạo ADMIN đầu tiên

- **Mục đích**: màn quản lý người dùng không tạo được ADMIN (`01-rd/req/identity.md:72-75`) nên cần một đường duy nhất để có quản trị viên đầu tiên.
- **Lịch**: chạy một lần lúc khởi động (`ApplicationRunner` của `algoprep-identity`), sau khi migration và seed ma trận quyền xong (cần role hệ thống `ADMIN` đã tồn tại).
- **Điều kiện chọn**: `NOT EXISTS (SELECT 1 FROM users u JOIN roles r ON r.id = u.role_id WHERE r.base_category = 'ADMIN')`.
- **Hành động**: `INSERT users` với email từ `ALGOPREP_BOOTSTRAP_ADMIN_EMAIL`, `password_hash` = BCrypt của `ALGOPREP_BOOTSTRAP_ADMIN_PASSWORD`, role hệ thống `ADMIN`, `status = 'PENDING_VERIFICATION'`, `must_change_password = true`.
- **Lô**: không áp dụng, một dòng.
- **Idempotent**: có. Đã có ADMIN thì thoát, không đọc giá trị biến mật khẩu, không đổi gì. Nếu hai instance khởi động cùng lúc, ràng buộc UNIQUE của `users.email` chặn dòng thứ hai; instance thua bắt `DataIntegrityViolationException` và coi như thành công.
- **Xử lý lỗi**: chưa có ADMIN và thiếu một trong hai biến, hoặc mật khẩu không đạt chuẩn: ném lỗi cấu hình nêu rõ tên biến hoặc quy tắc vi phạm, **ứng dụng dừng khởi động**. Không ghi mật khẩu vào log.
- **Audit**: không ghi `system_audit_logs`.

## 2. JOB-ID-02 — Ẩn danh hoá tài khoản tự xoá (F1-16)

- **Mục đích**: sau 30 ngày ân hạn, xoá định danh của tài khoản người dùng tự xoá (BR-08).
- **Lịch**: hằng ngày 03:00 theo giờ máy chủ (`cron = "0 0 3 * * *"`) `[SoT: Suy luận]`. Ân hạn 30 ngày là hằng số (`02-bd/architecture/identity.md:127`).
- **Điều kiện chọn**:

```sql
SELECT id FROM users
WHERE status = 'DEACTIVATED'
  AND deactivation_source = 'SELF'
  AND deactivated_at <= now() - interval '30 days'
  AND anonymized_at IS NULL
ORDER BY deactivated_at
LIMIT 200;
```

  Dòng `deactivation_source = 'ADMIN'` không bao giờ được chọn.
- **Hành động mỗi dòng** (một giao dịch cho cả lô, khoá dòng bằng `FOR UPDATE SKIP LOCKED` ở câu chọn): `UPDATE users SET email = 'deleted-user-' || id || '@anon.local', display_name = 'Người dùng đã xoá', phone = NULL, school_or_company = NULL, current_position = NULL, target_position = NULL, anonymized_at = now()`; rồi `DELETE FROM oauth_identities WHERE user_id = :id`. Không cascade sang bảng của module khác (BR-08 bước 3).
- **Lô**: 200 dòng mỗi lô, lặp đến khi câu chọn trả rỗng hoặc đủ 5 lô mỗi lần chạy (phần còn lại để lần sau) `[SoT: Suy luận]`.
- **Idempotent**: có. Điều kiện `anonymized_at IS NULL` loại dòng đã xử lý; chạy lại không đổi gì. Admin mở khoá giữa chừng (`status` về `ACTIVE`) làm dòng rơi khỏi điều kiện chọn; vì có khoá dòng, giao dịch của Admin và job không ghi đè nhau.
- **Xử lý lỗi**: lỗi một lô thì rollback lô đó, log ERROR kèm danh sách `id`, chuyển sang lô kế tiếp ở lần chạy sau (dòng vẫn thoả điều kiện). Email giả duy nhất theo `id` nên không đụng UNIQUE.
- **Bảng**: `users`, `oauth_identities`.
- **Nguồn**: `01-rd/req/identity.md` F1-16; `02-bd/database/identity.md` mục 1.1 và 1.11; `03-dd/logic/identity.md` BR-08; tiêu chí `04-tdd/identity.md` AC-42, AC-47.

## 3. JOB-ID-03 — Dọn feed hoạt động quá 90 ngày

- **Mục đích**: `identity_recent_activity` chỉ giữ 90 ngày, giao diện chỉ hiện 10 mục gần nhất (`02-bd/architecture/identity.md:130`).
- **Lịch**: hằng ngày 03:30 (`cron = "0 30 3 * * *"`) `[SoT: Suy luận]`.
- **Điều kiện chọn**: `occurred_at < now() - interval '90 days'`.
- **Hành động**: xoá cứng theo lô: `DELETE FROM identity_recent_activity WHERE id IN (SELECT id FROM identity_recent_activity WHERE occurred_at < now() - interval '90 days' ORDER BY id LIMIT 5000)`.
- **Lô**: 5.000 dòng mỗi lô (cùng cỡ lô BR-20), lặp đến khi số dòng xoá nhỏ hơn 5.000.
- **Idempotent**: có, xoá theo điều kiện thời gian.
- **Xử lý lỗi**: rollback lô lỗi, log ERROR, lần chạy sau tiếp tục; feed chỉ là hiển thị nên lỗi dọn không ảnh hưởng nghiệp vụ.
- **Lưu ý**: không có job nào **ghi** feed theo lịch (BR-18 đã bỏ); feed chỉ do event ghi. Cần chỉ mục theo `occurred_at` (BD mục 2 chỉ có chỉ mục ghép `(actor_user_id, occurred_at DESC)` và `(scope_class_id, occurred_at DESC)`, không có chỉ mục riêng `occurred_at`; với quy mô đồ án quét tuần tự chấp nhận được, thêm chỉ mục khi bảng lớn) `[SoT: Suy luận]`.
- **Bảng**: `identity_recent_activity`.

## 4. JOB-ID-04 — Dọn refresh token hết hạn hoặc đã thu hồi

- **Mục đích**: giữ bảng `refresh_tokens` gọn nhưng không làm sai số liệu "người dùng hoạt động" của `ADM0101`, vốn đếm `user_id` phân biệt theo `issued_at` đến 6 tháng (`02-bd/database/identity.md:99-104`).
- **Lịch**: hằng ngày 04:00 (`cron = "0 0 4 * * *"`) `[SoT: Suy luận]`.
- **Điều kiện chọn** (dòng đã hết hiệu lực **và** đủ già theo `issued_at`):

```sql
issued_at < now() - interval '6 months'
AND (expires_at < now() OR revoked_at IS NOT NULL)
```

  Dòng còn hiệu lực không bao giờ bị xoá. Giữ tối thiểu 6 tháng tính theo `issued_at` để số liệu giữ chân 6 tháng không bị cắt.
- **Hành động**: xoá cứng theo lô 5.000 dòng như JOB-ID-03, dùng chỉ mục `refresh_tokens(issued_at)`.
- **Idempotent**: có.
- **Xử lý lỗi**: như JOB-ID-03. Không được xoá dòng của một `family_id` còn có dòng hiệu lực chưa quá 6 tháng làm hỏng phát hiện tái sử dụng: vì điều kiện xét từng dòng theo `issued_at` cũ hơn 6 tháng và token chỉ sống 30 ngày, một token bị thu hồi cũ hơn 6 tháng không còn khả năng bị phát lại có ý nghĩa `[SoT: Suy luận]`.
- **Bảng**: `refresh_tokens`.

## 5. JOB-ID-05 — Dọn nhật ký hệ thống quá hạn

Giữ nguyên đặc tả ở BR-20 (`03-dd/logic/identity.md:41-46`): hằng ngày, đọc `system_settings` (`retention_days`, `expired_policy`), `DELETE` thì xoá dòng `created_at < now() - retention_days` theo lô 5.000; `ARCHIVE` hoãn, chỉ ghi cảnh báo. Lịch đề xuất 04:30 (`cron = "0 30 4 * * *"`) `[SoT: Suy luận]`. Mục này chỉ để bảng 1 đủ các job của module; không đặc tả lại.

## Tham chiếu

`03-dd/logic/identity.md` (BR-08, BR-20, BR-23), `02-bd/database/identity.md` (mục 1.1, 1.6, 1.11, 4), `02-bd/architecture/identity.md` (mục 5), `04-tdd/identity.md` (AC-42, AC-46, AC-47), `DEC-2026-1023-db-actor-review-fixes`. Các file job khác: `03-dd/jobs/judge-orchestration.md`, `03-dd/jobs/ai-review.md`.
