# DD — API module `identity` (F1)

> DD đầu tiên của dự án (`03-dd/` trước đây chỉ có `.gitkeep`). Đọc cùng `02-bd/architecture/F1_identity.md`,
> `02-bd/database/F1_identity.md`, `02-bd/security/F1_identity.md` — DD này **không lặp lại** nội dung đã chốt ở
> BD, chỉ ghi thêm phần BD chưa nói được: hợp đồng endpoint xuyên màn, quyết định kỹ thuật, và các khối quy
> tắc nghiệp vụ trải dài nhiều sự kiện. Request/response chi tiết theo trường đã có đủ ở Sheet 7.1 của từng
> file `02-bd/screens/**` — ở đây chỉ liệt kê tối thiểu tên trường/kiểu/bắt buộc cho endpoint mà BD màn
> chưa đủ để code thẳng (chủ yếu là các API xác thực chưa gắn với màn cụ thể nào).
>
> **Quy ước path chốt tại DD này** (chưa có tiền lệ trong dự án, đây là DD đầu tiên): base path
> `/api/v1/identity`, JSON, xác thực qua `Authorization: Bearer <access_token>` trừ các endpoint đánh dấu
> "Công khai". Áp dụng chung `04xx`: `400` lỗi validate, `401` chưa xác thực/token hết hạn, `403` không đủ
> quyền, `404` không tìm thấy, `409` xung đột trạng thái, `422` vi phạm quy tắc nghiệp vụ (mã lỗi cụ thể
> trong `error.code`, dạng `IDT-nnn`).

## 0. Danh sách endpoint (khoanh vùng theo BD screens)

> **Sửa 2026-10-22 (đồng bộ DB vòng 3, DEC-2026-1022-self-register-pending-login-blocked):** mục 0 này là danh mục endpoint chung cho đến khi module thứ hai có DD; lúc đó mới tách `03-dd/api/api.md`.

Quét toàn bộ `02-bd/screens/**` (Sheet 7.3 "Danh sách endpoint") tìm mọi endpoint ghi chú module sở hữu là
`identity`. Cột "Màn gọi" dùng mã màn `Fx`/`USR`/`INS`/`ADM`; cột "Nguồn BD" trỏ operation-name logic mà BD
đã đặt (BD không đặt path/method — đó là việc của DD này).

### 0.1. Xác thực và phiên (chưa gắn màn cụ thể — dùng chung bởi `SHR0101_auth` và mọi màn khác qua filter)

| # | Method | Path | Mô tả | Màn gọi | Ghi chú |
| :-: | :--- | :--- | :--- | :--- | :--- |
| 1 | POST | `/api/v1/identity/auth/register` | Đăng ký email + mật khẩu (F1-01) | `SHR0101_auth` | Công khai |
| 2 | POST | `/api/v1/identity/auth/login` | Đăng nhập, trả access token + set cookie refresh token (F1-02) | `SHR0101_auth` | Công khai; rate-limit BR-01 |
| 3 | POST | `/api/v1/identity/auth/refresh` | Xoay vòng refresh token, cấp access token mới (F1-03) | Toàn hệ thống (interceptor HTTP client) | Công khai nhưng bắt buộc header `X-Requested-With` (`02-bd/security/F1_identity.md:16-18`); BR-02 |
| 4 | POST | `/api/v1/identity/auth/logout` | Thu hồi refresh token hiện tại, xoá cookie (F1-04) | `SHR0101_auth`, mọi khung `_shell.md` (menu người dùng) | |
| 5 | GET | `/api/v1/identity/auth/oauth/{provider}/redirect` | Bắt đầu luồng OAuth (`provider` = `github`\|`google`), trả URL kèm `state` (F1-15) | `SHR0101_auth` | Công khai; BR-03 |
| 6 | GET | `/api/v1/identity/auth/oauth/{provider}/callback` | Nhận callback OAuth, đối chiếu `state`, tự liên kết hoặc tạo tài khoản (F1-15) | `SHR0101_auth` | Công khai; BR-03 |
| 7 | POST | `/api/v1/identity/auth/password/forgot` | Gửi OTP quên mật khẩu (F1-17) | `SHR0101_auth` | Công khai; luôn trả 200 (chống dò email, `02-bd/security/F1_identity.md:45-46`) |
| 8 | POST | `/api/v1/identity/auth/password/reset` | Xác nhận OTP + đặt mật khẩu mới (F1-17) | `SHR0101_auth` | Công khai; BR-04 |
| 60 | POST | `/api/v1/identity/auth/password/verify-otp` | Kiểm OTP quên mật khẩu trước bước đặt mật khẩu mới; không tiêu thụ mã | `SHR0101_auth` | Công khai; BR-04, `DEC-2026-1025-dd-auth-recovery-contracts` |
| 61 | POST | `/api/v1/identity/auth/deactivation/restore` | Huỷ yêu cầu tự xoá trong hạn 30 ngày bằng vé khôi phục sau xác minh đăng nhập | `SHR0101_auth` | Công khai; chỉ `deactivation_source = SELF`, BR-08, `DEC-2026-1025-dd-auth-recovery-contracts` |
| 53 | POST | `/api/v1/identity/auth/email/verify` | Xác nhận OTP xác thực email của tài khoản **tự đăng ký**, `PENDING_VERIFICATION` chuyển `ACTIVE` (F1-33, **đề xuất**) | `SHR0101_auth` | Công khai; mục 1 bên dưới. **Sửa 2026-10-22 (đồng bộ DB vòng 2, DEC-2026-1022-db-sync-round2) mục 1:** chỉ dùng cho tự đăng ký (#1); tài khoản Admin tạo (#50) không có OTP, xem BR-21. `[SoT: Suy luận — mã F1-33 và endpoint do phase 5 đề xuất; DEC-2026-1022-identity-schema-sync mục 3 chỉ chốt có "verify endpoint", chưa chốt path]` |
| 54 | POST | `/api/v1/identity/auth/email/verify/resend` | Gửi lại OTP xác thực email, **chỉ tự đăng ký** (F1-33, **đề xuất**; Sửa 2026-10-22 (đồng bộ DB vòng 2, DEC-2026-1022-db-sync-round2)) | `SHR0101_auth` | Công khai; luôn trả 200 (chống dò email, cùng tinh thần #7); cooldown 60 giây. `[SoT: Suy luận]` |
| 49 | POST | `/api/v1/identity/auth/instructor/login` | Đăng nhập riêng cho giảng viên — cùng contract endpoint #2, tự chặn tài khoản không phải `INSTRUCTOR` | `views/teacher/auth` (route `/instructor/login`, không có màn RD/BD — `DEC-2026-0925-instructor-separate-login-route`) | Công khai; rate-limit BR-01; đánh số #49 (nối cuối danh sách) để không đổi số các endpoint 9-48 đã được trích dẫn nơi khác |

### 0.2. Hồ sơ cá nhân — `USR0502_profile.md`

| # | Method | Path | Mô tả | Nguồn BD (operation name) | Ghi chú |
| :-: | :--- | :--- | :--- | :--- | :--- |
| 9 | GET | `/api/v1/identity/me/profile` | Tải hồ sơ, trạng thái mật khẩu, danh sách provider đã liên kết | `GetMyProfile` [SoT: `02-bd/screens/users/USR0502_profile.md:589`] | Sửa 2026-10-22 (đồng bộ DB vòng 2, DEC-2026-1022-db-sync-round2) mục 4: phản hồi mang thêm `mustChangePassword: boolean` để giao diện chuyển hướng khi vào lại bằng phiên cũ `[SoT: Suy luận — tên trường do DD đề xuất]` |
| 10 | PATCH | `/api/v1/identity/me/profile` | Cập nhật hồ sơ, **trừ** `email` | `UpdateMyProfile` [SoT: `02-bd/screens/users/USR0502_profile.md:590`] | Server bỏ qua `role_id`/`status` nếu client gửi lên [SoT: `02-bd/screens/users/USR0502_profile.md:661`] |
| 11 | POST | `/api/v1/identity/me/email-change/request` | Yêu cầu đổi email, gửi OTP tới email mới | `RequestMyEmailChange` [SoT: `02-bd/screens/users/USR0502_profile.md:591`] | Trả trực tiếp lỗi "email đã dùng" — khác nguyên tắc chống dò email vì đã đăng nhập [SoT: `07-review/bd_open_questions_with_solutions_260924.md:136`] |
| 12 | POST | `/api/v1/identity/me/email-change/confirm` | Xác nhận OTP, đổi `email` | `ConfirmMyEmailChange` [SoT: `02-bd/screens/users/USR0502_profile.md:592`] | |
| 13 | POST | `/api/v1/identity/me/password` | Đổi mật khẩu (đã có) hoặc đặt mật khẩu lần đầu (tài khoản chỉ OAuth) | `ChangeMyPassword` [SoT: `02-bd/screens/users/USR0503_settings.md:729`; từ 2026-10-20 gọi từ màn Cài đặt, không còn từ Trang cá nhân] | BR-05: đặt mật khẩu lần đầu không cần mật khẩu cũ, không cần OTP thêm [SoT: `07-review/...:135`]; BR-06: đổi mật khẩu thu hồi mọi refresh token khác [SoT: `07-review/...:137`]; `currentPassword` sai trả `422 IDT-115`, không ghi dữ liệu/thu hồi phiên (owner chốt 2026-10-27) |

### 0.2.1. Phản hồi hồ sơ (#9 `GetMyProfile`)

Response `200`: `{ userId, displayName, email, schoolOrCompany, currentPosition, targetPosition, defaultLanguage, phone, hasPassword, passwordChangedAt, linkedProviders, joinedAt, roleName, mustChangePassword }`. `schoolOrCompany`, `currentPosition`, `targetPosition`, `phone`, `passwordChangedAt` có thể `null`; `linkedProviders` là mảng mã provider, không trả `provider_user_id`, email provider hay hash mật khẩu. `hasPassword` suy từ `password_hash IS NOT NULL`; `defaultLanguage` chỉ đọc ở Hồ sơ, sửa tại Cài đặt #15. `joinedAt` và `passwordChangedAt` ISO-8601 UTC. Chỉ đọc tài khoản từ JWT. `[SoT: BD USR0502 Sheet 7.1, DEC-2026-1022-db-sync-round2]`

### 0.2.2. Đổi email đã đăng nhập (#11–12)

#11 nhận `{ newEmail }` (RFC 5322, tối đa 254 ký tự, khác email hiện tại); chỉ sau khi xác nhận email chưa thuộc tài khoản khác mới phát OTP sáu số tới email mới, TTL 10 phút trong Redis theo `userId`. Email trùng trả `409 IDT-415`; vượt ba lần yêu cầu trong 24 giờ trả `429 IDT-V05`. **Lần gửi lại cùng email mới vẫn gọi #11 và tính một lượt trong cùng hạn mức 3 lần/24 giờ** (`DEC-2026-1025-dd-email-resend-budget`); cooldown 60 giây vẫn kiểm riêng. Response `200 { pendingEmail, expiresInSeconds: 600 }`, không đổi `users.email`. #12 nhận `{ code }` đúng sáu chữ số; sai, hết hạn hoặc đã dùng trả cùng `422 IDT-416` để không phân biệt tình trạng mã. Đúng mã cập nhật `users.email` và thu hồi OTP nguyên tử, trả `200 { email }`; nếu email bị tài khoản khác chiếm trước khi xác nhận, trả `409 IDT-415` và giữ email cũ. Chỉ người dùng trong JWT đổi email của chính mình. Không đưa OTP vào phản hồi hay log. `[SoT: BD USR0502 Sheet 7–9; validation F1 §2–4]`

### 0.3. Cài đặt và dữ liệu cá nhân — `USR0503_settings.md`

| # | Method | Path | Mô tả | Nguồn BD | Ghi chú |
| :-: | :--- | :--- | :--- | :--- | :--- |
| 14 | GET | `/api/v1/identity/me/settings` | Đọc `user_preferences` và `users.default_language` (chủ đề màu/ngôn ngữ giao diện lưu thiết bị) | `GetMySettings` [SoT: Suy luận — theo cặp Get/Update đã dùng ở `GetMyProfile`] | Phản hồi có `preferredMaxTurns` (`8`/`12`/`16`, mặc định `12`), `preferredHintAllowed` (mặc định `true`) từ `user_preferences`; chưa có dòng thì trả mặc định. Theo `DEC-2026-1024-dd-student-language-contracts`, thêm `defaultLanguage` từ `users.default_language`, mặc định `PYTHON` theo schema, không trả `null`; tên JSON `[SoT: Suy luận]` |
| 15 | PATCH | `/api/v1/identity/me/settings` | Cập nhật `streak_reminder_enabled`, `weekly_report_enabled`, `preferred_interview_level`, `preferred_max_turns`, `preferred_hint_allowed` và `users.default_language` | `UpdateMySettings` [SoT: `07-review/bd_open_questions_with_solutions_260924.md:154`; `DEC-2026-1024-dd-student-language-contracts`] | Các trường gửi lên được cập nhật nguyên tử trong giao dịch `identity` (hai bảng khi có `defaultLanguage`). Trường vắng mặt giữ nguyên; phản hồi trả cài đặt sau cập nhật. Kiểm hợp lệ ở `03-dd/validation/F1_identity.md` mục 8 |
| 16 | GET | `/api/v1/identity/me/export?scope={profile\|submissions}` | Xuất CSV/JSON dữ liệu cá nhân (F1-22) | `ExportMyData` [SoT: `07-review/...:148`] | Đồng bộ, rate-limit 1 lần/5 phút/loại tệp [SoT: `07-review/...:148`]; kiểm `user_id` trong token khớp yêu cầu [SoT: `02-bd/security/F1_identity.md:58-60`] |
| 17 | GET | `/api/v1/identity/me/export/interview-transcripts` | Xuất hội thoại phỏng vấn (dữ liệu ở `ai-review`, `identity` đọc qua cổng ra) | `ExportMyInterviewTranscripts` [SoT: `07-review/...:149`] | `ai-review` lỗi → trả `503` báo lỗi tại chỗ, **không khoá nút** phía UI [SoT: `07-review/...:149`] |
| 18 | POST | `/api/v1/identity/me/account/delete` | Tự xoá tài khoản — yêu cầu gõ lại email của chính mình để xác nhận (F1-16) | `DeleteMyAccount` [SoT: `07-review/...:150`] | Xem BR-08 |

### 0.3.1. Hợp đồng cài đặt cá nhân (#14–15)

`GET /me/settings` trả `200` với `streakReminderEnabled`, `weeklyReportEnabled`, `preferredInterviewLevel`, `preferredMaxTurns`, `preferredHintAllowed` từ `identity.user_preferences` và `defaultLanguage` từ `identity.users.default_language`. Khi chưa có dòng preference, trả mặc định `true`, `false`, `MIDDLE`, `12`, `true`; `defaultLanguage` mặc định `PYTHON` theo DB. `PATCH /me/settings` nhận tập con của sáu trường này, trường vắng giữ nguyên; cập nhật các trường gửi lên trong cùng giao dịch và trả cùng DTO sau ghi. Không đọc schema `judge` tại F1; danh sách mã đang bật do F4 #24 cung cấp cho client. Quy tắc hợp lệ tại `03-dd/validation/F1_identity.md` mục 8. Nguồn: `02-bd/database/F1_identity.md:27,201`, `DEC-2026-1024-dd-student-language-contracts`.

### 0.4. Lớp học — `INS0201_class_management.md`, `INS0202_class_assignments.md`, `INS0203_class_progress.md`, `INS0204_class_student_detail.md`

| # | Method | Path | Mô tả | Màn gọi | Ghi chú |
| :-: | :--- | :--- | :--- | :--- | :--- |
| 19 | GET | `/api/v1/identity/classes/my` | Danh sách lớp của giảng viên đang đăng nhập | `INS0101_overview`, `INS0201_class_management` | |
| 20 | POST | `/api/v1/identity/classes` | Tạo lớp (F1-23) | `INS0201_class_management` | Tạo lớp và mã mời đầu tiên trong cùng giao dịch; trả `classId` để chuyển `INS0205` (`02-bd/screens/teacher/INS0201_class_management.md` EVT-3, Sheet 7.2) |
| 21 | PATCH | `/api/v1/identity/classes/{classId}` | Sửa tên/mô tả/lịch học (F1-23); `schedule` là jsonb có cấu trúc từ 2026-10-11, `DEC-2026-1011-class-schedule-structured` | `INS0201_class_management`, `INS0205_class_detail` | Kiểm sở hữu: `instructor_id` khớp người gọi |
| 22 | DELETE | `/api/v1/identity/classes/{classId}` | Xoá lớp — hard delete (F1-24) | `INS0201_class_management` | `02-bd/database/F1_identity.md:88-89`. **Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4):** sau commit phát `ClassDeleted` (mục 2), xem BR-22 |
| 23 | POST | `/api/v1/identity/classes/{classId}/invite-codes` | Sinh mã mời mới, TTL 7 ngày (F1-25) | `INS0201_class_management` | Nhiều mã hiệu lực song song [SoT: `02-bd/database/F1_identity.md:93-94`] |
| 24 | GET | `/api/v1/identity/classes/{classId}/invite-codes` | Liệt kê mã mời còn hiệu lực của lớp | `INS0201_class_management` | |
| 25 | POST | `/api/v1/identity/classes/join` | Học viên tham gia lớp bằng mã mời (F1-23) | `USR0101_problem_list` (khối "Bài tập lớp"), luồng nhập mã | BR-09: hết hạn/không tồn tại → `422 IDT-410`. ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-class-leave-details): học viên từng bị gỡ vào lại bằng mã → dùng lại dòng ghi danh cũ, đặt `left_at` = NULL, vẫn `200`; đã ở trong lớp → `200` không đổi gì |
| 25A | GET | `/api/v1/identity/me/classes` | `ListMyClasses` — lớp còn ghi danh của người gọi | `USR0701_my_classes` | Chỉ `left_at IS NULL`; không chứa bài giao; mục 0.4.1 |
| 25B | GET | `/api/v1/identity/me/classes/{classId}` | `GetMyClass` — thông tin một lớp còn ghi danh | `USR0702_my_class_detail` | Cùng `404 IDT-411` cho lớp không có, đã rời hoặc không thuộc người gọi; mục 0.4.1 |
| 26 | GET | `/api/v1/identity/classes/{classId}/students` | Danh sách học viên trong một lớp, kèm trạng thái phân loại (F1-27) | `INS0205_class_detail` | Ngưỡng phân loại theo BR-10; tab Tất cả của `INS0201`/`INS0203` dùng #59 |
| 27 | POST | `/api/v1/identity/classes/{classId}/students/{studentId}/leave` | Gỡ học viên khỏi lớp (F1-26) — ĐÃ ĐỔI 2026-10-14 (DEC-2026-1014-keep-learning-history-on-leave): đặt `left_at`, giữ lịch sử, không xoá dòng. ĐÃ CHỐT 2026-10-14 (DEC-2026-1014-class-leave-details): đổi từ ~~`DELETE /api/v1/identity/classes/{classId}/students/{studentId}`~~ sang `POST .../leave`, không có body; gọi lặp là idempotent (dòng đã có `left_at` thì không ghi đè mốc cũ, vẫn `200`). Chốt 2026-10-27: lần gọi lại phát lại event với `occurredAt=left_at` gốc. | `INS0201_class_management` | Phát `StudentRemovedFromClass`, xem BR-11; consumer khử bản sao theo `(classId,studentId,occurredAt)` |
| 28 | GET | `/api/v1/identity/classes/{classId}/students/{studentId}/detail` | Chi tiết một học viên trong lớp (F1-28) | `INS0204_class_student_detail` | DTO mục 0.4.2; metrics lớp qua cổng F4 P1 |
| 29 | GET | `/api/v1/identity/classes/{classId}/progress` | Dashboard tiến độ lớp: hoàn thành TB, phân loại 5 nhãn (F1-28) | `INS0203_class_progress` | `completionPct` tính khi đọc (BR-12; ~~đọc `class_completion_stats`~~ bảng này đã bỏ 2026-10-22, `DEC-2026-1022-identity-schema-sync` mục 4); nhãn theo `DEC-2026-0921-teacher-screens-conflict-resolutions` |

### 0.4.1. Hợp đồng đọc lớp học viên và tham gia bằng mã

| Thao tác | Request | Response thành công | Lỗi |
| :--- | :--- | :--- | :--- |
| #25A `ListMyClasses` | JWT STUDENT; không tham số | `200 { items: MyClassDto[] }`; mỗi dòng gồm `classId`, `name`, `description` (chuỗi rỗng nếu NULL), `schedule` (object hoặc null), `instructorName`, `joinedAt` (ISO-8601). Sắp `joinedAt` mới trước, `classId` tăng dần để ổn định khi trùng thời điểm | `401` chung; lỗi hệ thống `503` chung |
| #25B `GetMyClass` | JWT STUDENT; `classId` UUID | `200 MyClassDto`, cùng trường như #25A | `404 IDT-411` cho UUID sai dạng, lớp không có, người gọi đã rời hoặc chưa từng thuộc; `401` chung; `503` chung |
| #25 `JoinClassByCode` (bổ sung) | JWT STUDENT; `{ code: string }`, client trim rồi uppercase; server chuẩn hoá lại | `200 { classId, className }` kể cả đã là thành viên; vào lại kích hoạt dòng cũ, giữ `joinedAt` | `422 IDT-410` cho mã không có, sai dạng hoặc hết hạn; `429 IDT-412` khi đạt năm lần sai trong 15 phút; `401` chung |

`MyClassDto` chỉ đọc `identity.class_enrollments` (`student_id = tokenUserId`, `left_at IS NULL`), `identity.classes` và `identity.users.display_name` của giảng viên; không đọc schema `problem`. #25B gắn điều kiện thành viên ngay trong truy vấn, không trả 403 cho người ngoài để tránh dò lớp. `schedule` theo JSONB của `02-bd/database/F1_identity.md` mục 1.14, không định dạng sẵn thành chuỗi. #25A không phân trang vì danh sách lớp đang tham gia nhỏ `[SoT: Suy luận]`.

Với #25, Redis giữ các mốc thử sai trong sorted set `identity:class-join:failed:<userId>`; thao tác xoá mốc cũ hơn 15 phút, đếm và thêm mốc mới chạy nguyên tử (Lua). Chỉ `IDT-410` tăng đếm; lần thử sai thứ năm trả `IDT-410` rồi các lần kế tiếp trả `IDT-412` đến khi hết cửa sổ trượt. Thành công xoá sorted set; lỗi hạ tầng không tăng đếm. Đặt TTL 15 phút kể từ lần thử sai mới nhất để dọn khoá. Không đưa `classId` hay tình trạng từng tồn tại của mã vào lỗi. Nguồn: `DEC-2026-1024-dd-student-class-contracts`, `02-bd/screens/users/USR0701_my_classes.md` Sheet 9.

### 0.4.2. Hồ sơ một học viên trong lớp (#28 `GetClassStudentProfile`)

JWT INSTRUCTOR và `CLASS_MANAGEMENT:READ`; `classId`, `studentId` là UUID. F1 kiểm lớp thuộc giảng viên và học viên còn ghi danh (`left_at IS NULL`) trước khi ghép dữ liệu. Lớp sai dạng/ngoài phạm vi và học viên không thuộc lớp cùng trả `404 IDT-414`; không lộ hồ sơ ngoài lớp. Response `200`: `{ classId, studentId, displayName, email, phone, className, joinedAt, status, assignmentProgress: [ { problemId, bestRatio, bestVerdict } ] }`. `status` thuộc `INSUFFICIENT_DATA|ABSENT|NEEDS_SUPPORT|WATCH|ON_TRACK`; `phone` và `bestRatio` có thể `null`, `bestVerdict` null khi chưa nộp. `assignmentProgress` chỉ có bài còn được giao (`removed_at IS NULL`), **kể cả bài đã rút khỏi ngân hàng**, sắp `assignedAt DESC`, `problemId` tăng khi hoà; F2 §50 cung cấp `problemTitle` và `topicNames` cho màn, ghép qua `problemId`. `bestRatio`/`bestVerdict` lấy từ F4 P1 `assignmentResults[]` theo lần giao hiện hành; không dùng `identity.user_problem_best_score` toàn cục vì lượt trước khi giao không làm bài hoàn thành trong lớp. F4 P1 cũng cung cấp metrics để phân loại `status`, không trả thành trường điểm TB riêng. Lỗi cổng cần cho nhãn trả `503`, không dựng nhãn giả. `[SoT: BD INS0204 Sheet 7.1; DEC-2026-1013-a2-reads-student-contact; DEC-2026-1024-dd-student-class-contracts; Owner 2026-10-25]`

### 0.4.3. Quy tắc chung API lớp giảng viên (#19–24, #26–29)

Mọi endpoint lớp của giảng viên kiểm JWT INSTRUCTOR và `CLASS_MANAGEMENT:READ` cho đọc, `CREATE`/`UPDATE`/`DELETE` cho thao tác tương ứng. `classId` sai dạng, không tồn tại hoặc ngoài phạm vi đều trả `404 IDT-414`; #27 idempotent khi học viên đã rời như hàng danh mục. Form #20/#21 kiểm `name`, `description`, `schedule` tại validation F1 §5: `IDT-V14`, `IDT-V15`, `IDT-V10` hoặc `IDT-V16` theo trường lỗi. #23/#24 chỉ đọc hoặc sinh mã cho lớp thuộc người gọi, không trả mã mời của lớp khác. `[SoT: BD INS0201/INS0205 Sheet 9; DEC-2026-1024-dd-teacher-student-list]`

**Bảng mã lỗi bổ sung Phase 5–6** (cùng phong bì ở `03-dd/api/api.md` mục 1.1):

| Mã | HTTP | Ý nghĩa | Dùng cho |
| :--- | :-: | :--- | :--- |
| `IDT-411` | 404 | Lớp không hiện diện trong ghi danh hiện tại của học viên | #25B |
| `IDT-412` | 429 | Đã đủ năm lần thử mã mời sai trong 15 phút | #25 |
| `IDT-413` | 422 | `defaultLanguage` không thuộc ba mã hợp lệ | #15 |
| `IDT-414` | 404 | Lớp không thuộc phạm vi giảng viên, không có hoặc UUID sai | #28, #57, #58, #59 |
| `IDT-415` | 409 | Email mới đã thuộc tài khoản khác | #11, #12 |
| `IDT-416` | 422 | OTP đổi email sai, hết hạn hoặc đã dùng | #12 |
| `IDT-421` | 422 | Không được xoá role hệ thống | #34 |
| `IDT-422` | 422 | Không được xoá role còn người dùng gán | #34 |
| `IDT-440` | 409 | Email tài khoản Admin tạo đã tồn tại | #50 |
| `IDT-441` | 422 | Vai trò đích của tài khoản Admin tạo không hợp lệ | #50 |
| `IDT-442` | 422 | Thời hạn lưu log không phải số nguyên tối thiểu 1 | #52 |
| `IDT-443` | 400 | Bộ lọc ngày/nhóm/cursor hoặc cột CSV của log sai | #39, #40 |
| `IDT-444` | 400 | Từ khoá tìm log dài quá 100 ký tự | #39, #40 |
| `IDT-445` | 403 | Người gọi đã xác thực nhưng thiếu quyền `Function:Action` bắt buộc trên endpoint F1 | Các endpoint F1 gác ma trận quyền |
| `IDT-446` | 503 | Ghi audit `ROLE_DELETED` lỗi; rollback xoá role/quyền | #34 |
| `IDT-447` | 403 | JWT hợp lệ nhưng `baseCategory` không phải `ADMIN` ở endpoint nhật ký chỉ dành cho ADMIN | #39, #40, #64–66 |
| `IDT-448` | 422 | Không thể mở lại tài khoản đã ẩn danh hoá (`anonymized_at IS NOT NULL`); không thay đổi dữ liệu/phiên/audit thành công | #37 |

### 0.5. Ma trận phân quyền và vai trò — `ADM0202_permission_matrix.md`

Khi JWT hợp lệ nhưng vai trò không có ô `Function:Action` mà endpoint F1 yêu cầu, tầng phân quyền trả `403 IDT-445` trước khi gọi use case; không ghi dữ liệu hay audit của lệnh bị từ chối. Mọi request bắt đầu sau PATCH quyền thành công đọc `permission_version` đã commit từ PostgreSQL primary trước khi dùng cache nên áp quyền mới ngay cả khi Redis invalidate chưa đến (chủ dự án chốt 2026-10-28). Các lỗi vai trò/trạng thái hoặc phạm vi sở hữu có mã riêng vẫn giữ hợp đồng của chúng.

| # | Method | Path | Mô tả | Nguồn BD | Ghi chú |
| :-: | :--- | :--- | :--- | :--- | :--- |
| 30 | GET | `/api/v1/identity/rbac/matrix` | Đọc toàn bộ ma trận Role × Function × Action | `GetPermissionMatrix` [SoT: Suy luận] | `functions`/`actions` chỉ đọc [SoT: `02-bd/database/F1_identity.md:54`] |
| 31 | PATCH | `/api/v1/identity/rbac/matrix/{roleId}/{functionId}/{actionId}` | Bật/tắt một ô, có hiệu lực ngay | `UpdateRolePermission` [SoT: `07-review/bd_open_questions_with_solutions_260924.md:278`] | BR-13: mỗi lần bấm ô gọi ngay, ghi `system_audit_logs`, publish invalidate cache |
| 32 | GET | `/api/v1/identity/rbac/roles` | Liệt kê role (3 hệ thống + tuỳ biến) | `ListRoles` [SoT: Suy luận] | |
| 33 | POST | `/api/v1/identity/rbac/roles` | Tạo role tuỳ biến, chọn `base_category` (F1-11) | `CreateRole` [SoT: `02-bd/architecture/F1_identity.md:98-104`] | Xem ghi chú mục 0.6 (mâu thuẫn giữa hai nguồn BD) |
| 34 | DELETE | `/api/v1/identity/rbac/roles/{roleId}` | Xoá role tuỳ biến (F1-11) | `DeleteRole` | `is_system=true` → `422 IDT-421`; còn user gán → `422 IDT-422`. Role tuỳ biến không có người gán: `204` không body, xoá role/quyền và ghi một audit `ROLE_DELETED` cùng giao dịch. Ghi audit lỗi → rollback, `503 IDT-446` (BR-14; owner chốt 2026-10-28) |

### 0.6. Quản lý người dùng (ADMIN) — `ADM0201_user_management.md`

| # | Method | Path | Mô tả | Nguồn BD | Ghi chú |
| :-: | :--- | :--- | :--- | :--- | :--- |
| 35 | GET | `/api/v1/identity/admin/users?status=&role=&q=&page=&pageSize=` | Danh sách người dùng, lọc + phân trang | `ListUsers` [SoT: Suy luận] | `status` nhận thêm `PENDING_VERIFICATION`. Mỗi phần tử trả thêm `hasPassword: boolean` = `password_hash IS NOT NULL`, **không thêm cột** (`DEC-2026-1011-admin-system-open-questions-closed`, H2). **Sửa 2026-10-22 (đồng bộ DB vòng 3, DEC-2026-1022-self-register-pending-login-blocked):** trả thêm `anonymized: boolean` = `anonymized_at IS NOT NULL` để `ADM0201` vô hiệu nút mở khoá (không thêm cột) `[SoT: Suy luận — tên trường do DD đề xuất]`. `pageSize` mặc định 20 (nợ `PROTOTYPE_DEBT.md` về phân trang giả) |
| 36 | PATCH | `/api/v1/identity/admin/users/{userId}/role` | Đổi vai trò (F1-13) | `ChangeUserRole` [SoT: Suy luận] | BR-15 (bảo vệ admin cuối cùng) |
| 37 | PATCH | `/api/v1/identity/admin/users/{userId}/status` | Khoá/mở khoá tài khoản (F1-13) | `ChangeUserStatus` [SoT: `07-review/bd_open_questions_with_solutions_260924.md:268`] | Đổi hai chiều cùng một endpoint; BR-15. **Phase 5 (ID-02):** khoá đặt `status = DEACTIVATED, deactivation_source = 'ADMIN', deactivated_at = now()`; mở khoá đặt `status = ACTIVE` và `deactivation_source`, `deactivated_at` về `NULL`. Tài khoản `ADMIN` khoá không bao giờ bị job ẩn danh hoá (BR-08). **Sửa 2026-10-22 (đồng bộ DB vòng 3, DEC-2026-1022-self-register-pending-login-blocked):** mở khoá một tài khoản có `anonymized_at IS NOT NULL` bị từ chối ở phía máy chủ (tính là một tài khoản thất bại trong kết quả một phần, lý do "đã ẩn danh hoá"; request đơn trả `422 IDT-448`, không ghi thay đổi/phiên/audit thành công; owner chốt 2026-10-28) |
| 38 | POST | `/api/v1/identity/admin/users/{userId}/reset-password` | Đặt lại mật khẩu hộ (F1-13) | `ResetUserPassword` [SoT: Suy luận] | Sinh mật khẩu tạm, gửi email, thu hồi mọi refresh token của user đó. Tài khoản `hasPassword = false` (chỉ OAuth): **không** sinh mật khẩu, gửi email hướng dẫn quay lại nhà cung cấp (cùng tinh thần `422 IDT-108` ở luồng quên mật khẩu) (H2) |
| 50 | POST | `/api/v1/identity/admin/users` | Admin tạo tài khoản thủ công (F1-13 mở rộng) | `CreateUserAccount` [SoT: `02-bd/screens/admin/ADM0201_user_management.md` Q1] | Xem mục 0.6.1. Đánh số #50 (nối cuối danh sách) để không đổi số các endpoint đã được trích dẫn |
| 62 | GET | `/api/v1/identity/admin/users/management-stats` | Số tài khoản chờ xác thực email | `GetUserManagementStats` [SoT: BD ADM0201 Sheet 7.3 NO 2] | `USER_MANAGEMENT:READ`; không suy số từ trang #35 |
| 63 | GET | `/api/v1/identity/admin/users/role-distribution` | Bốn nhóm phân bố vai trò loại trừ nhau | `GetRoleDistribution` [SoT: BD ADM0201 Sheet 7.3 NO 3] | `USER_MANAGEMENT:READ`; không suy số từ trang #35 |

**Xác nhận tự ảnh hưởng của ADMIN (#36–37, chốt với chủ dự án 2026-10-27).** #36 nhận `{ targetRoleId: UUID, acknowledgeSelfImpact?: boolean }`; #37 nhận `{ targetStatus: "ACTIVE"|"DEACTIVATED", reason?: string, notifyByEmail?: boolean, acknowledgeSelfImpact?: boolean }` theo DTO `ADM0201` Sheet 7.1. Khi actor là chính `userId` đích và thao tác #36 hạ từ ADMIN sang role không thuộc `ADMIN`, hoặc #37 khoá chính tài khoản (`targetStatus=DEACTIVATED`), client phải gửi `acknowledgeSelfImpact: true` sau cảnh báo xác nhận. Thiếu/`false` trả `400 IDT-V21`, không đổi user, phiên hay audit và không gửi email. Với đích khác hoặc thao tác không tự ảnh hưởng, cờ không bắt buộc. Bất biến còn ít nhất một ADMIN `ACTIVE` được kiểm trước khi chấp nhận tự ảnh hưởng; nếu vi phạm trả `422 IDT-432` dù có cờ xác nhận. Mỗi endpoint là một lệnh cho một `userId`; màn thao tác nhiều người ghép kết quả từng lệnh. [SoT: BD ADM0201 Q6/Sheet 7.1 NO 13, Sheet 9 NO 6–8; quyết định chủ dự án 2026-10-27].

**Đã đưa vào phạm vi 2026-10-11** (`DEC-2026-1001-admin-configurable-settings` đã chốt "Thêm tài khoản";
`DEC-2026-1011-admin-system-open-questions-closed` chốt nơi lưu trạng thái "Chờ xác thực" là
`users.status = PENDING_VERIFICATION`). Thay cho đoạn "chưa đưa vào phạm vi" cũ.

#### 0.6.0. Hai số tổng hợp cho `ADM0201` (#62–63)

#62 trả `200 { pendingVerificationCount: int }`, đếm mọi `identity.users.status = PENDING_VERIFICATION`, không phụ thuộc bộ lọc/trang #35. #63 trả `200 { items: [{ groupCode, count, percent }] }` với đúng bốn mã `STUDENT`, `INSTRUCTOR`, `ADMIN`, `DEACTIVATED` theo thứ tự đó. Ba nhóm đầu chỉ đếm tài khoản chưa `DEACTIVATED` theo `roles.base_category`; nhóm cuối đếm mọi tài khoản `DEACTIVATED` đúng một lần, không còn trong nhóm vai trò. `percent = count / tổng bốn nhóm × 100`, tổng bằng 0 thì bốn tỷ lệ 0; server làm tròn một chữ số thập phân, client không cộng lại để sửa sai số làm tròn. Hai endpoint đòi JWT `ADMIN` và `USER_MANAGEMENT:READ`, lỗi 403 dùng mã quyền F1 hiện có. Cả hai chỉ đọc `identity.users`/`identity.roles`; lỗi DB trả 503 chung, không giả 0. [SoT: BD ADM0201 Sheet 7.1 NO 8–9, Sheet 7.3 NO 2–3, Sheet 8 EVT-1]

#### 0.6.1. `POST /admin/users` (`CreateUserAccount`)

- **Quyền**: vai trò `ADMIN` (Lớp 1) và Function quản lý người dùng, giống #36 đến #38 `[SoT: Suy luận]`.
- **Request**: `{ "displayName": string, "email": string, "roleId": uuid }`. `roleId` phải thuộc role có
  `base_category` khác `ADMIN`; danh sách vai trò của popup lấy từ `GET /rbac/roles` (#32) lọc cùng điều kiện.
- **Hành vi**: tạo `users` với `status = PENDING_VERIFICATION` (**không** phát OTP email; chuyển `ACTIVE` ở lần đăng nhập thành công đầu tiên bằng mật khẩu tạm, BR-21; Sửa 2026-10-22 (đồng bộ DB vòng 2, DEC-2026-1022-db-sync-round2) mục 1), sinh mật khẩu tạm gửi email, đặt
  `must_change_password = true` (bắt đổi mật khẩu ở lần đăng nhập đầu), ghi một dòng `system_audit_logs`
  (`action_type = USER_CREATED`, `target_id` = id mới). Không lưu mật khẩu tạm dạng rõ, không ghi nó vào `after_json` (BR-16).
  **Sửa 2026-10-22 (phase 5, ID-03):** cột `users.must_change_password` đã có [SoT: `02-bd/database/F1_identity.md:26`]; cờ **xoá về
  `false` ở lần đổi mật khẩu đầu tiên** qua `POST /me/password` (#13, biến thể BR-06, mật khẩu cũ chính là mật khẩu tạm), cùng
  giao dịch đặt `password_changed_at = now()`. Tự đăng ký và OAuth giữ `false`. `POST /auth/login` (#2) trả thêm
  `user.mustChangePassword: boolean` để giao diện chuyển thẳng tới màn đổi mật khẩu `[SoT: Suy luận — tên trường do DD đề xuất]`.
  **Sửa 2026-10-22 (đồng bộ DB vòng 2, DEC-2026-1022-db-sync-round2) mục 4 (đóng Q4 phase 5):** buộc đổi là **mềm** — máy chủ **không** chặn các endpoint khác; chỉ giao diện chuyển hướng theo cờ
  `mustChangePassword` ở phản hồi đăng nhập (và `GET /me/profile`, #9).
- **Response**: `201` với phần tử cùng dạng #35.
- **Lỗi**: `409 IDT-440` email đã tồn tại; `422 IDT-441` `roleId` thuộc nhóm `ADMIN` hoặc không tồn tại; kiểm định dạng
  email như `POST /auth/register` (mục 1).

### 0.7. Nhật ký hệ thống — `ADM0403_system_log.md`

| # | Method | Path | Mô tả | Nguồn BD | Ghi chú |
| :-: | :--- | :--- | :--- | :--- | :--- |
| 39 | GET | `/api/v1/identity/admin/audit-logs` | Tìm/lọc nhật ký, cursor 50 dòng | `SearchSystemAuditLogs` [SoT: BD ADM0403 Sheet 7.3] | Mọi `base_category = ADMIN` đọc được, **không qua** `permissions`; request/response mục 0.7.2 |
| 40 | GET | `/api/v1/identity/admin/audit-logs/export` | CSV streaming sáu cột tuỳ chọn và bộ lọc | `ExportSystemAuditLogs` [SoT: BD ADM0403 Sheet 7.3] | `created_at` và email actor đọc từ `system_audit_logs`/`users`, che theo BR-16; mục 0.7.2 |
| 64 | GET | `/api/v1/identity/admin/audit-logs/active-admins` | Năm người thực hiện gần nhất và số hành động 24 giờ | `ListActiveAdmins` [SoT: BD ADM0403 Sheet 7.3 NO 2] | Mọi ADMIN đọc, không cần `SYSTEM_AUDIT_LOG` |
| 65 | GET | `/api/v1/identity/admin/audit-logs/category-breakdown` | Bốn nhóm hành động trong bảy ngày | `GetAuditCategoryBreakdown` [SoT: BD ADM0403 Sheet 7.3 NO 3] | Mọi ADMIN đọc |
| 66 | GET | `/api/v1/identity/admin/audit-logs/stream` | SSE dòng audit mới | `StreamSystemAuditLog` [SoT: BD ADM0403 Sheet 7.3 NO 4] | Mọi ADMIN đọc; contract mục 0.7.2 |

#### 0.7.1. Cấu hình lưu nhật ký (popup "Lưu trữ")

| # | Method | Path | Mô tả | Nguồn BD | Ghi chú |
| :-: | :--- | :--- | :--- | :--- | :--- |
| 51 | GET | `/api/v1/identity/admin/settings/log-retention` | Đọc thời hạn và chính sách log quá hạn | `GetLogRetentionSettings` [SoT: `02-bd/database/F1_identity.md` mục 1.13] | Mọi `ADMIN` đọc được |
| 52 | PUT | `/api/v1/identity/admin/settings/log-retention` | Sửa thời hạn và chính sách | `UpdateLogRetentionSettings` [SoT: `02-bd/screens/admin/ADM0403_system_log.md` Q1, Q2, Q7] | Cần Function `SYSTEM_AUDIT_LOG` (chỉ gác việc **sửa** cấu hình, không gác việc xem log). Request `{ "retentionDays": int >= 1, "expiredPolicy": "DELETE" hoặc "ARCHIVE" }`. Lỗi `422 IDT-442` `retentionDays < 1`. Ghi `system_audit_logs` phân loại "Cấu hình" kèm giá trị cũ và mới |

Job dọn log đọc `audit_log.retention_days` và `audit_log.expired_policy` (BR-20 ở `03-dd/logic/F1_identity.md`). `ARCHIVE`
**hoãn** (`DEC-2026-1011-admin-system-open-questions-closed`): job giữ nguyên dòng, không xoá. Không bao giờ có
endpoint xoá tay từng dòng (BD `ADM0403` Q2).

#### 0.7.2. Đọc, xuất và luồng nhật ký (#39–40, #64–66)

#39 nhận `q` tối đa 100 ký tự, `categoryCode` một trong `ALL|AUTH|PERMISSION|CONFIG|CONTENT`, `dateFrom`/`dateTo` là ngày địa phương UTC+7, `cursor` opaque và `limit=50`. Lọc ở máy chủ, sắp `(created_at DESC, id DESC)`; response `200 { items: AuditLogItemDto[], nextCursor: string|null }`. Cursor mã hoá cặp thời gian/UUID và bộ lọc, không chứa giá trị nhạy cảm. Nếu dòng trỏ mốc bị dọn, dùng cặp thời gian/UUID để tiếp tục, không lỗi kỹ thuật. `AuditLogItemDto` gồm `id`, `createdAt`, `actorDisplayName`, `actorEmail` (có thể đã ẩn danh), `actionType`, `categoryCode`, `serviceCode`, `targetType`, `targetId`, `safeParams`; `safeParams` đã che theo BR-19. Giao diện dựng câu bằng i18n từ `actionType` và `safeParams`. Tổng số dòng không bắt buộc; không trả số giả. JWT hợp lệ nhưng không thuộc `ADMIN` trả `403 IDT-447` trước khi đọc dữ liệu. [SoT: BD ADM0403 Sheet 4, 7.1–7.3, 8; quyết định owner 2026-10-25/28]

#40 nhận cùng `q`, `categoryCode`, `dateFrom`, `dateTo` và `columns` là danh sách không rỗng từ `time|actor|service|category|content|id`; `format=csv`. Response stream `text/csv; charset=utf-8` có header theo thứ tự cột yêu cầu, không trần số dòng. Cột `content` là câu mô tả dựng từ `actionType`/`messageKey` và `safeParams` đã che; **không** xuất raw `before_json`/`after_json`. `Accept-Language: vi|en` chọn ngôn ngữ câu mô tả, thiếu header thì dùng `vi`; các cột khác không đổi theo locale. Server ghi log hành động xuất trước khi phát body và che dữ liệu nhạy cảm ở cùng lớp dựng item như #39; khi luồng lỗi, dừng tải và log lỗi, không trả tệp thành công giả. JWT hợp lệ nhưng không thuộc `ADMIN` trả `403 IDT-447` trước khi đọc/xuất dữ liệu. Khoảng ngày cho cả #39/#40 phải hợp lệ, không tương lai và không vượt `retentionDays` từ #51; `400 IDT-443` cho ngày/cột sai, `400 IDT-444` cho từ khoá quá dài. [SoT: BD ADM0403 Sheet 9 NO 3–6, 10; Q4/Q8; quyết định owner 2026-10-28]

#64 trả `200 { items: [{ actorUserId, displayName, roleName, actionCount, lastActionAt }] }`, tối đa năm ADMIN có hành động trong 24 giờ trượt, sắp `actionCount DESC, lastActionAt DESC, actorUserId ASC`; không có thì `[]`. #65 trả `200 { items: [{ categoryCode, count }] }` đủ bốn nhóm `AUTH|PERMISSION|CONFIG|CONTENT`, kể cả 0, cửa sổ bảy ngày gần nhất, không phụ thuộc bộ lọc #39. Khi tổng 0 client vẽ thanh 0. [SoT: BD ADM0403 Sheet 5 khu E/F, Sheet 7.3; quyết định owner 2026-10-25]

#66 là `text/event-stream`, JWT ADMIN, `Cache-Control: no-store`, một sự kiện `audit-log` mang cùng `AuditLogItemDto` đã che như #39, `id` là UUID log. `Last-Event-ID` được dùng khi nối lại, chỉ phát log mới hơn mốc trong hạn lưu; nếu mốc không còn, server phát sự kiện `resync` không có dữ liệu log để client tải lại #39 trang đầu. Heartbeat comment mỗi 30 giây; kết nối lại với backoff ở client. Không phát token/OTP/before_json/after_json thô. [SoT: BD ADM0403 Sheet 8 EVT-6/7; BR-19]

### 0.8. Dashboard — `ADM0101_overview.md`, `INS0101_overview.md`, `USR0501_my_progress.md`

`ADM0101` có đúng mười khối dữ liệu cần tải và thử lại độc lập. Theo quyết định owner 2026-10-25, mỗi khối có một HTTP endpoint F1; giữ số/path #41–43 đã được trích dẫn nhưng thu hẹp response của mỗi endpoint về đúng một khối, bổ sung #67–73. `INS0101` dùng các endpoint riêng bên dưới.

| # | Method | Path | Mô tả | Màn gọi | Ghi chú |
| :-: | :--- | :--- | :--- | :--- | :--- |
| 41 | GET | `/api/v1/identity/admin/dashboard/summary` | `GetActiveUsersSummary`: thẻ người dùng hoạt động | `ADM0101_overview` | `value` = `DISTINCT user_id` có `refresh_tokens.issued_at` trong 30 ngày gần nhất; không đọc `users.last_active_at` |
| 42 | GET | `/api/v1/identity/admin/dashboard/user-growth` | `GetUserRetention`: sáu tháng người dùng mới/quay lại | `ADM0101_overview` | Tên path giữ để tránh đổi trích dẫn cũ; quy tắc mục 0.8.5 |
| 43 | GET | `/api/v1/identity/admin/dashboard/cross-module` | `GetSubmissionsSummary`: thẻ tổng lượt nộp | `ADM0101_overview` | Tên path giữ để tránh đổi trích dẫn cũ; cổng F4, BR-17 |
| 67 | GET | `/api/v1/identity/admin/dashboard/submissions/by-language` | `GetSubmissionsByLanguage`: 20 mốc, ba ngôn ngữ | `ADM0101_overview` | Cổng F4 |
| 68 | GET | `/api/v1/identity/admin/dashboard/submissions/verdicts` | `GetVerdictDistribution`: năm verdict cuối | `ADM0101_overview` | Cổng F4 |
| 69 | GET | `/api/v1/identity/admin/dashboard/submissions/difficulty` | `GetDifficultyBreakdown`: lượt nộp/Accepted theo mức | `ADM0101_overview` | Cổng F4 và F2; mức động theo sort_order |
| 70 | GET | `/api/v1/identity/admin/dashboard/submissions/by-day` | `GetSubmissionsByDay`: bảy ngày | `ADM0101_overview` | Cổng F4 |
| 71 | GET | `/api/v1/identity/admin/dashboard/submissions/by-month` | `GetSubmissionsByMonth`: sáu tháng | `ADM0101_overview` | Cổng F4 |
| 72 | GET | `/api/v1/identity/admin/dashboard/top-problems` | `GetTopProblems`: bốn bài phổ biến | `ADM0101_overview` | Cổng F4 và F2, chỉ bài đang xuất bản |
| 73 | GET | `/api/v1/identity/admin/dashboard/password-resets` | `GetPasswordResetsSummary`: thẻ yêu cầu đổi mật khẩu | `ADM0101_overview` | Từ F1, chuyển khỏi ADM0201 |
| 44 | GET | `/api/v1/identity/instructor/dashboard/summary` | Hợp đồng cũ: tổng lớp, tổng học viên (DISTINCT), điểm TB toàn bộ lớp phụ trách | Không còn màn DD tiêu thụ | Legacy; `INS0101` dùng #56. Giữ mã/path để không đổi danh mục cũ; không nối vào màn mới. |
| 45 | GET | `/api/v1/identity/instructor/dashboard/recent-activity?classId=` | Feed "Hoạt động gần đây", lọc theo lớp qua `scope_class_id` | `INS0101_overview` | BR-18 |
| 46 | GET | `/api/v1/identity/me/recent-activity` | "Hoạt động gần đây" của cá nhân (10 mục, 90 ngày) | `USR0501_my_progress`, `USR0530_dashboard nếu có` | [SoT: `02-bd/database/F1_identity.md:116-117`] |
| 47 | GET | `/api/v1/identity/me/progress/summary` | `GetMyProgressOverview`: best-attempt theo bài, tỉ lệ Accepted, "Đúng ngay lần đầu", ngôn ngữ dùng nhiều nhất | `USR0202_my_submissions`, `USR0501_my_progress`, `USR0502_profile`, `USR0601_dashboard` | Response mục 0.8.4; `topLanguage` từ `language_counts`/`top_language` |
| 48 | GET | `/api/v1/identity/me/problem-solve-states?problemIds=` | Trạng thái giải theo từng `problem_id` của người gọi (map) | `ListMyProblemSolveStates` [SoT: `07-review/bd_open_questions_with_solutions_260924.md:16`] | Được `problem-bank` gọi qua cổng ra, cache 60 giây phía `problem-bank` |
| 55 | GET | `/api/v1/identity/me/activity-calendar` | Lưới hoạt động 12 tháng: số lượt nộp theo từng ngày (UTC+7) | `USR0601_dashboard` (Khu vực lưới hoạt động) | **Đề xuất Sửa 2026-10-22 (đồng bộ DB vòng 2, DEC-2026-1022-db-sync-round2) mục 3**; mục 0.8.1 bên dưới; đánh số #55 (nối cuối danh sách) để không đổi số các endpoint đã trích dẫn `[SoT: Suy luận — path và số do DD đề xuất]` |
| 56 | GET | `/api/v1/identity/instructor/classes/summary` | `ListMyClassesSummary`/`GetInstructorClassSummary` — tổng hợp và thẻ từng lớp | `INS0101_overview`, `INS0201_class_management` | Mục 0.8.2; theo `DEC-2026-1024-dd-teacher-f1-read-contracts` |
| 57 | GET | `/api/v1/identity/instructor/classes/score-trend` | `GetClassScoreTrend` — một đường sáu tuần ISO đã đủ | `INS0203_class_progress` | Mục 0.8.2; `classId` tuỳ chọn |
| 58 | GET | `/api/v1/identity/instructor/classes/attention-students` | `ListClassAttentionStudents` — tối đa năm học viên cần chú ý | `INS0203_class_progress` | Mục 0.8.2; `classId` tuỳ chọn |
| 59 | GET | `/api/v1/identity/instructor/students` | `ListInstructorStudents` — danh sách học viên phân trang trên toàn bộ lớp phụ trách | `INS0201_class_management`, `INS0203_class_progress` | Mục 0.8.3; `classId` tuỳ chọn |

#### 0.8.2. Ba hợp đồng đọc cho màn giảng viên (#56–58)

Các endpoint này yêu cầu JWT INSTRUCTOR và `CLASS_MANAGEMENT:READ`. Khi có `classId`, chỉ nhận lớp do người gọi phụ trách; UUID sai, lớp không có và lớp ngoài phạm vi cùng `404 IDT-414`, không lộ lớp tồn tại. `userId` lấy từ token, không có tham số đọc hộ. `identity` đọc bảng của mình và dùng `ClassAssignmentReadPort` (F2), `GetClassStudentSubmissionMetrics` (F4 P1) để tính tổng hợp, không join schema `problem`/`judge`. Lỗi cổng cần dữ liệu cho phép tính trả `503` chung, không biến thành số 0.

| #, thao tác | Request | Response 200 | Quy tắc nguồn |
| :--- | :--- | :--- | :--- |
| #56 `ListMyClassesSummary` / `GetInstructorClassSummary` | Không tham số | `{ classCount, studentCount, avgCompletionPercent, absentStudentCount, items: ClassSummaryDto[] }`; mỗi `items[]` có `classId`, `name`, `description`, `schedule`, `studentCount`, `completionPercent`, `avgScore`, `absentCount` | Chỉ lớp người gọi; `studentCount` tổng là `COUNT(DISTINCT student_id)` đang ghi danh, từng lớp đếm riêng; `absentCount` theo cửa sổ 7 ngày; `completionPercent` tính khi đọc theo bài còn được giao (`removed_at IS NULL`), kể cả bài đã rút khỏi ngân hàng, và F4 P1 `assignmentResults[].accepted` sau khi giao. `avgScore` null khi chưa có lượt chấm. Danh sách mới tạo trước (`created_at DESC`, `classId` tăng khi hoà). Nguồn BD `INS0101` Sheet 7.1, `INS0201` Sheet 7.1; `DEC-2026-1024-dd-student-class-contracts`; Owner 2026-10-25. |
| #57 `GetClassScoreTrend` | `classId` UUID tuỳ chọn; thiếu = tất cả lớp của người gọi | `{ classId: uuid|null, className: string|null, points: [ { weekStart, avgScore: number|null } ] }` đúng sáu phần tử, tuần ISO thứ Hai–Chủ nhật đã kết thúc, cũ đến mới, một đường duy nhất | Điểm theo F4 P1, chỉ lượt nộp trong lớp và tuần đủ; tuần không có bài chấm cho `avgScore=null`, không dùng 0. Cache Redis tối đa 15 phút theo phạm vi. `INS0203` Sheet 5 Khu B, `DEC-2026-1014-instructor-batch2-weekly-rules`. |
| #58 `ListClassAttentionStudents` | `classId` UUID tuỳ chọn; thiếu = tất cả lớp của người gọi | `{ items: [ { classId, studentId, displayName, status, reasonCode, reasonValue } ] }`, tối đa năm dòng | Chỉ thành viên hiện tại; `status` là `ABSENT`, `NEEDS_SUPPORT`, `WATCH`, ưu tiên theo thứ tự đó rồi mức độ và `studentId` để ổn định. `reasonCode`/`reasonValue` cho client dịch câu, không gửi chuỗi tự do. Dấu hiệu: không nộp 7 ngày trượt; điểm tuần đủ giảm từ hai mốc liên tiếp; hoàn thành <50% khi đã giao ≥1 bài. `INS0203` Sheet 5 Khu C. |

**Thứ tự mức độ trong #58 (chủ dự án chốt 2026-10-27):** trước hết `ABSENT` → `NEEDS_SUPPORT` → `WATCH`. Trong `ABSENT`, số ngày tròn từ lượt nộp cuối thuộc phạm vi lớp đến thời điểm đọc (UTC+7) **giảm dần**; học viên chưa từng nộp là `INSUFFICIENT_DATA` theo BR-10 và không vào nhóm này. Trong `NEEDS_SUPPORT`, lấy mức giảm tỉ lệ Accepted (điểm phần trăm) giữa mốc đầu và mốc cuối của chính chuỗi mốc tuần đã làm học viên khớp nhãn BR-10; **giảm dần theo độ lớn**. Trong `WATCH`, `completionPct` từng học viên **tăng dần**. Nếu cùng độ nghiêm trọng trong một nhãn, `studentId` tăng dần; lấy tối đa năm dòng **sau khi sắp xếp**. Không dùng thời gian nộp ngoài phạm vi lớp hoặc điểm trước khi giao bài để xếp. [SoT: quyết định chủ dự án 2026-10-27; BR-10, F4 class metrics].

#56 dùng được cho hai tên nghiệp vụ BD, không tạo endpoint thứ tư chỉ khác tên. `INS0101` dùng `items` và tổng `studentCount`; `INS0201` dùng cả ba thẻ tổng và thẻ từng lớp. #57 và #58 lọc theo tab cùng `classId`; từ khoá bảng học viên không áp vào hai endpoint này. `[SoT: DEC-2026-1024-dd-teacher-f1-read-contracts]`

Điểm TB trong #56, #57, #59 và bảng học viên lớp = `F4 P1.acceptedRatio * 10` theo thang 10 của BD, làm tròn một chữ số thập phân. Với #57, trước hết tính điểm từng học viên từ `weeklyTrend[].acceptedRatio`, sau đó lấy trung bình cộng các học viên có mẫu số hợp lệ trong tuần; không có học viên hợp lệ thì `avgScore=null`. F4 đã loại `SYSTEM_ERROR` khỏi mẫu số tỉ lệ AC; F1 không tính lại trên `totalSubmissions`. [SoT: BD INS0201 Sheet 5, INS0203 Sheet 5; DEC-2026-1025-dd-system-error-ac-denominator]

#### 0.8.4. Phản hồi tiến độ cá nhân (#47 `GetMyProgressOverview`)

Response `200`: `{ solvedProblemCount: int, totalSubmissions: int, acceptedCount: int, acceptanceRatePercent: number|null, firstTryAcceptedCount: int, topLanguage: "JAVA"|"CPP"|"PYTHON"|null }`. `solvedProblemCount` là số `problem_id` có `best_verdict = ACCEPTED` trong `identity.user_problem_best_score` (lịch sử, có thể gồm bài đã rút); `totalSubmissions`, `acceptedCount`, `firstTryAcceptedCount`, `topLanguage` từ `identity.user_submission_stats`. `totalSubmissions` vẫn đếm lỗi hệ thống; `acceptanceRatePercent = acceptedCount / ac_eligible_count * 100`, làm tròn số nguyên, `null` khi mẫu số 0; `ac_eligible_count` chỉ đếm lượt trạng thái cuối khác `SYSTEM_ERROR` và không cần trả riêng cho màn. `userId` lấy từ JWT, không nhận tham số người khác. Dashboard `USR0601` lấy tử số và mẫu số bài **đang xuất bản** từ F2 §14A, chỉ dùng #47 cho tỉ lệ và lượt nộp lịch sử. `[SoT: BD USR0502 Sheet 7.1; BD USR0601 Khu B; DEC-2026-1024-dd-student-dashboard-and-interview-navigation; Owner 2026-10-25 Q22]`

#### 0.8.3. `GET /instructor/students` (#59 `ListInstructorStudents`)

JWT INSTRUCTOR và `CLASS_MANAGEMENT:READ`. Query: `classId` UUID tuỳ chọn (vắng là mọi lớp do người gọi phụ trách), `q` chuỗi trim tối đa 100 ký tự, `page` số nguyên từ 1 (mặc định 1), `pageSize` số nguyên 1–100 (mặc định 20). `classId` sai dạng, không tồn tại hoặc ngoài phạm vi đều trả `404 IDT-414`; tham số `q`/phân trang sai trả `400 IDT-V17`. Không nhận `instructorId`. Chỉ lấy ghi danh hiện tại (`left_at IS NULL`); một học viên ở hai lớp tạo hai dòng phân biệt bằng cặp `(classId, studentId)`. Tìm `q` trên tên học viên, không lọc biểu đồ #57 hay khối #58.

Response `200`: `{ items: [ { classId, className, studentId, displayName, joinedAt, completionPercent, avgScore, status } ], page: { page, pageSize, totalCount, totalPages } }`. `status` thuộc `INSUFFICIENT_DATA|ABSENT|NEEDS_SUPPORT|WATCH|ON_TRACK`; `avgScore = null` khi chưa có lượt chấm, `completionPercent = 0` khi chưa giao bài. Mẫu số hoàn thành gồm bài còn được giao (`removed_at IS NULL`), kể cả bài đã rút khỏi ngân hàng; tử số là số dòng F4 P1 `assignmentResults[].accepted=true` của lần giao đang hiệu lực. `totalCount` là số cặp lớp–học viên sau khi lọc và trước khi cắt trang. Sắp theo `displayName` không phân biệt hoa thường, rồi `classId`, `studentId` tăng dần để trang ổn định. F1 đọc ghi danh của mình, ghép số liệu qua F2 `ClassAssignmentReadPort` và F4 `GetClassStudentSubmissionMetrics` theo lô cho các dòng của trang; lỗi cổng trả `503`, không thay số thiếu bằng 0. Không join trực tiếp schema F2/F4. `[SoT: DEC-2026-1024-dd-teacher-student-list; DEC-2026-1024-dd-student-class-contracts; BD INS0201/INS0203 Sheet 7–9; Owner 2026-10-25]`

#### 0.8.1. `GET /me/activity-calendar` (`GetMyActivityCalendar`, đề xuất)

`[SoT: Suy luận]` cho path, tên operation và tên trường phản hồi; nội dung theo `DEC-2026-1022-db-sync-round2` mục 3.

- **Quyền**: người dùng đã đăng nhập, chỉ đọc dữ liệu của chính mình (`user_id` lấy từ token, không nhận tham số `userId`).
- **Request**: không tham số. Cửa sổ cố định: 12 tháng gần nhất tính tới ngày hiện tại theo UTC+7 (cùng múi giờ cắt ngày ở
  `02-bd/database/F1_identity.md` mục 1.15); lưới của `USR0601` hiển thị 364 ngày cuối của cửa sổ này (52 tuần x 7 ngày,
  `02-bd/screens/users/USR0601_dashboard.md` Khu vực E NO 3).
- **Nguồn**: `identity.user_daily_submission_stats(user_id, day, submission_count)` [SoT: `02-bd/database/F1_identity.md:193`]; truy vấn
  đọc tối đa 366 dòng (chỉ những ngày có lượt nộp). Ngày không có dòng nghĩa là 0.
- **Response `200`**: `{ "from": "YYYY-MM-DD", "to": "YYYY-MM-DD", "days": [ { "day": "YYYY-MM-DD", "submissionCount": int } ] }`, `days` sắp
  xếp tăng dần và **luôn đủ 364 phần tử**: tầng ứng dụng của `identity` điền `submissionCount = 0` cho ngày không có dòng (khớp BD `USR0601`
  Khu vực E NO 3: "backend trả đủ chuỗi, màn không tự vá lỗ hổng"). **Sửa 2026-10-22 (đồng bộ DB vòng 3, DEC-2026-1022-self-register-pending-login-blocked):** endpoint chỉ trả
  `submissionCount` mỗi ngày, **không** có `level`; việc quy ra 5 mức đậm nhạt (0..4) do màn `USR0601` tính (mối quan tâm hiển thị).
- **Lỗi**: `401` thiếu/hết hạn token (mã chung); không có lỗi nghiệp vụ riêng.
- **Thay thế một phần quyết định cũ**: endpoint này **thay phần "lưới hoạt động"** của `DEC-2026-0927-submission-metrics-two-ports`
  (lưới trước đây đọc qua cổng ra `judge`); lưới nay đọc từ `identity`. Biểu đồ theo ngày ở Khu vực C của `USR0601` **giữ nguồn `judge`**
  vì nó lọc theo chủ đề mà `user_daily_submission_stats` không có.

#### 0.8.5. Mười hợp đồng độc lập cho `ADM0101` (#41–43, #67–73)

Mọi endpoint yêu cầu JWT `ADMIN` và quyền đọc dashboard theo BD ADM0101, không nhận `userId`; response của từng khối độc lập và lỗi cổng chỉ làm hỏng đúng endpoint đó (`503` chung), không trả số 0 thay cho lỗi. Các mốc ngày/tháng tính theo UTC+7, dãy sắp cũ đến mới, điền 0 cho mốc không có lượt. Giá trị số đều không âm. Quy tắc so sánh `deltaPercent`, `deltaDirection`, `sparklineSeries` của `StatSummary` theo BD ADM0101 Sheet 7.1; khi kỳ trước bằng 0, `deltaPercent = null` và direction `NONE`, tránh chia cho 0. [SoT: BD ADM0101 Sheet 7.1, 7.3, 8; quyết định owner 2026-10-25]

| Endpoint | Response `200` | Nguồn và quy tắc |
| :--- | :--- | :--- |
| #41 `GetActiveUsersSummary` | `StatSummary { value, deltaPercent, deltaDirection, sparklineSeries }` | `DISTINCT refresh_tokens.user_id` có `issued_at` trong 30 ngày; kỳ trước là 30 ngày liền trước. Không đọc `users.last_active_at` |
| #42 `GetUserRetention` | `{ groups: [{ month, newUsers, returningUsers }] }` đủ sáu tháng | `newUsers`: tạo trong tháng và chưa `DEACTIVATED`; `returningUsers`: DISTINCT token user trong tháng, tài khoản tạo trước tháng đó |
| #43 `GetSubmissionsSummary` | `StatSummary` | Tổng lượt nộp từ F4, gồm cả kết quả chưa cuối theo BD; kỳ so sánh/sparkline theo BD Sheet 5 khu A |
| #67 `GetSubmissionsByLanguage` | `{ pointLabels: string[20], series: [{ languageCode, values: int[20] }] }` | F4 phân theo ba mã ngôn ngữ được BD chốt; không tạo đường thứ tư từ giá trị không hợp lệ |
| #68 `GetVerdictDistribution` | `{ slices: [{ verdict, count, percent }] }` đủ năm verdict | Chỉ kết quả cuối, tổng 0 thì các percent 0; server làm tròn, client không tự tính lại |
| #69 `GetDifficultyBreakdown` | `{ groups: [{ levelId, levelCode, levelDisplayName, submissions, accepted }], legend }` | Mỗi mức F2 theo `sort_order`, kể cả count 0; F4 đếm lượt theo bài/mức, `accepted <= submissions` |
| #70 `GetSubmissionsByDay` | `{ columns: [{ day, count }], weekTotal, dailyAverage }` đúng bảy ngày | F4; `dailyAverage = weekTotal / 7`, không chia theo số ngày có lượt |
| #71 `GetSubmissionsByMonth` | `{ points: [{ month, count }], monthTotal }` đúng sáu tháng | F4; `monthTotal` là tổng sáu điểm |
| #72 `GetTopProblems` | `{ items: [{ problemId, title, submissionCount }] }`, tối đa bốn | Chỉ bài F2 `PUBLISHED` và `deleted=false`, F4 đếm lượt, sắp lượt giảm rồi `problemId` tăng |
| #73 `GetPasswordResetsSummary` | `StatSummary` | Số yêu cầu đặt lại mật khẩu từ F1, kỳ so sánh/sparkline theo BD ADM0101 Sheet 5 |

F1 gọi `JudgeStatsPort`/`ProblemBankStatsPort` theo lô trong phạm vi từng endpoint, không join schema F4/F2 trực tiếp. Mỗi endpoint cache tối đa 60 giây riêng theo ID khối. Nút Thử lại của một khối invalidate đúng query tương ứng; sự cố F4 khi tải #68 không che #42. Không có query string điều khiển kỳ trên màn này; các cửa sổ cố định như BD. [SoT: F1 BR-17; BD ADM0101 Sheet 8]

## 1. Yêu cầu/phản hồi tối thiểu (endpoint chưa gắn màn cụ thể, mục 0.1)

BD screens (Sheet 7.1) chỉ phủ endpoint có màn hình đích; nhóm 0.1 (auth) không thuộc màn hình nào (dùng
bởi `SHR0101_auth`, RD chưa vào chi tiết trường) nên DD chốt tối thiểu tại đây.

### `POST /auth/register`
Request: `{ "email": string (bắt buộc, RFC 5322), "password": string (bắt buộc), "displayName": string (bắt buộc, ≤100) }`
Response `201`: `{ "userId": uuid, "email": string }`
Lỗi: `409 IDT-101` (email đã tồn tại — không dùng ở luồng này vì người dùng đang tự đăng ký, không phải OWASP-enumeration case của quên mật khẩu).

### `POST /auth/login`
Request: `{ "email": string (bắt buộc), "password": string (bắt buộc) }`
Response `200`: `{ "accessToken": string, "expiresIn": 900, "user": { "id": uuid, "displayName": string, "baseCategory": "STUDENT"|"INSTRUCTOR"|"ADMIN", "mustChangePassword": boolean } }` + `Set-Cookie: refresh_token=...; HttpOnly; Secure; SameSite=Strict`
Lỗi: `401 IDT-102` (sai email/mật khẩu, thông báo chung không nói rõ sai cái nào), `429 IDT-103` (vượt rate-limit, BR-01), `403 IDT-104` (`status = DEACTIVATED`), `403 IDT-113` (tự đăng ký còn `PENDING_VERIFICATION`, chưa nhập OTP email). **Sau khi thông tin đăng nhập đã được xác minh**, nếu tài khoản tự xoá còn trong hạn 30 ngày, `IDT-104` có `details = { restoreTicket: string, expiresInSeconds: 300 }`; tài khoản do Admin khoá hoặc đã ẩn danh không nhận vé. Vé opaque dùng một lần, không phải access token, chỉ dùng ở #61 (`DEC-2026-1025-dd-auth-recovery-contracts`).
**Sửa 2026-10-22 (đồng bộ DB vòng 2, DEC-2026-1022-db-sync-round2) mục 1 và 4:** `user.mustChangePassword` = `users.must_change_password` (tên trường `[SoT: Suy luận]`). Tài khoản Admin tạo đang `PENDING_VERIFICATION` đăng nhập đúng mật khẩu tạm thì được chuyển `ACTIVE` trong cùng lần gọi (BR-21 bước 2).
**Sửa 2026-10-22 (đồng bộ DB vòng 3, DEC-2026-1022-self-register-pending-login-blocked):** tự đăng ký còn `PENDING_VERIFICATION` (`must_change_password = false`) đăng nhập đúng mật khẩu bị chặn `403 IDT-113` (BR-21 bước 3), không phát token; người dùng nhập OTP (#53) rồi đăng nhập lại. `IDT-113` `[SoT: Suy luận — mã đề xuất]`. Cùng bảng lỗi áp cho `POST /auth/instructor/login` (#49) và `callback` OAuth không bị ảnh hưởng (tài khoản OAuth tạo ra qua `email_verified = true`, không `PENDING_VERIFICATION` `[SoT: Suy luận]`).

### `POST /auth/instructor/login`
`DEC-2026-0925-instructor-separate-login-route`. Request/response giống hệt `POST /auth/login` ở trên
(cùng bảng lỗi `IDT-102`/`IDT-103`/`IDT-104`), cộng thêm một bước chặn sau khi mật khẩu đã khớp:
Lỗi thêm: `403 IDT-111` (`baseCategory != INSTRUCTOR` — tài khoản đúng nhưng không có quyền giảng viên;
không áp nguyên tắc chống dò tài khoản của `password/forgot` vì mật khẩu đã được xác minh đúng ở bước
này, không phải dò email chưa xác thực).

### `POST /auth/refresh`
Request: không body, đọc cookie `refresh_token`, bắt buộc header `X-Requested-With: XMLHttpRequest`.
Response `200`: `{ "accessToken": string, "expiresIn": 900 }` + `Set-Cookie` token mới (rotate).
Lỗi: `401 IDT-105` (revoked/hết hạn), `401 IDT-106` (phát hiện reuse — cả family bị revoke, xem BR-02).

### `POST /auth/password/forgot`
Request: `{ "email": string (bắt buộc) }`
Response `200` (luôn luôn, kể cả email không tồn tại): `{ "message": "Nếu email tồn tại, mã xác nhận đã được gửi." }`
Trong 60 giây cooldown theo `userId`, gọi lại vẫn trả **cùng 200/body chung**, không tạo hoặc gửi OTP mới và không gia hạn TTL mã hiện có. Hết cooldown, một lần gọi hợp lệ mới được gửi lại (owner chốt 2026-10-27).
Tài khoản chỉ OAuth nhận email hướng dẫn quay lại provider, **không phát OTP**; response vẫn dùng đúng body chung trên (owner chốt 2026-10-27).

### `POST /auth/password/reset`
Request: `{ "email": string (bắt buộc), "otp": string (bắt buộc, 6 số), "newPassword": string (bắt buộc, xem `03-dd/validation/F1_identity.md`) }`
Response `200`: `{ "message": "Đặt lại mật khẩu thành công." }`
Lỗi: `422 IDT-107` (OTP sai/hết hạn/quá 5 lần, email không tồn tại hoặc tài khoản chỉ OAuth — thông báo chung, không phân biệt). `IDT-108` không còn dùng ở luồng quên mật khẩu vì tài khoản chỉ OAuth không nhận OTP; hướng dẫn provider đã gửi riêng ở bước `forgot` (owner chốt 2026-10-27).
**Sửa 2026-10-22 (đồng bộ DB vòng 3, DEC-2026-1022-self-register-pending-login-blocked):** đặt lại thành công còn đặt `must_change_password = false` và chuyển tài khoản Admin tạo đang `PENDING_VERIFICATION` sang `ACTIVE` (BR-04 bước 4).

### `POST /auth/password/verify-otp` (#60)

Owner chốt `DEC-2026-1025-dd-auth-recovery-contracts` để màn Quên mật khẩu xác minh mã **trước** khi mở bước nhập mật khẩu mới. Request `{ email: string, otp: string }`, OTP đúng sáu chữ số; response `200 { verified: true }`. Sai, hết hạn, hết lượt hoặc email không có tài khoản đều trả cùng `422 IDT-107`; không phân biệt nguyên nhân và không tiết lộ sự tồn tại email. Máy chủ so khớp OTP và ghi số lần sai nguyên tử trên Redis, nhưng **không xoá mã khi đúng**; #8 kiểm lại cùng mã. TTL gốc 10 phút không kéo dài sau verify. Giới hạn gọi theo cùng khoá và ngân sách sai OTP của BR-04; không trả số lần còn lại. Tài khoản chỉ OAuth nhận cùng lỗi chung ở màn; email hướng dẫn provider đã gửi ở #7. Reset chỉ xoá mã **sau khi** giao dịch DB thành công; khoá dòng `users` và mốc `password_changed_at` chặn phát lại khi Redis chưa xoá được (BR-04, owner 2026-10-27).

### `POST /auth/deactivation/restore` (#61)

Owner chốt `DEC-2026-1025-dd-auth-recovery-contracts`. Request `{ restoreTicket: string }`; ticket opaque do #2 hoặc OAuth callback #6 cấp **chỉ sau** khi mật khẩu/provider đã xác minh và F1 xác định `status = DEACTIVATED`, `deactivation_source = SELF`, `anonymized_at IS NULL`, chưa quá 30 ngày từ `deactivated_at`. Ticket ở Redis, TTL 5 phút, dùng một lần, ràng buộc với `userId` và lần xác minh, không là JWT; không ghi vào URL, log hoặc local storage. Response `200 { restored: true }`, **không phát phiên đăng nhập**; màn báo thành công và yêu cầu đăng nhập lại. Ticket không hợp lệ/đã dùng/hết hạn hoặc tài khoản không còn đủ điều kiện trả cùng `401 IDT-114`; không cấp ticket cho `ADMIN` lock. Trong giao dịch khoá dòng tài khoản, đặt `status = ACTIVE`, `deactivation_source = NULL`, `deactivated_at = NULL`; job ẩn danh và thao tác khôi phục cùng kiểm dưới khoá, không có cửa sổ khôi phục sau ẩn danh. Cùng một ticket không thể dùng lần hai. [SoT: RD SHR0101 Q2; `DEC-2026-1018-account-deletion-grace-cancel`; owner 2026-10-25]

### `GET /auth/oauth/{provider}/redirect` và `.../callback`
`redirect` response: `{ "authorizationUrl": string, "state": string }`; URL uỷ quyền đăng ký callback của provider là route frontend `/auth/callback/{provider}`. Provider đưa `code`, `state` về route này; trang callback gọi F1 #6 với hai query đó rồi nhận JSON response giống `POST /auth/login`. Access token chỉ đi trong HTTPS response body của #6, refresh token chỉ qua cookie HTTP-Only, không đưa token hay restore ticket vào URL. Frontend xoá `code`/`state` khỏi thanh địa chỉ sau khi gọi và không ghi chúng vào log. Lỗi `401 IDT-109` (state không khớp), `422 IDT-110` (`email_verified = false` ở provider — từ chối tự liên kết, `02-bd/security/F1_identity.md:19-21`). Nếu provider xác minh đúng tài khoản tự xoá đang trong hạn 30 ngày, callback trả `403 IDT-104` với `details.restoreTicket` như #2; không phát access/refresh token. [SoT: BD SHR0101 Q2; DD SHR0101 BR-SC-05]

### `POST /auth/email/verify` và `POST /auth/email/verify/resend` (đề xuất, ID-04)

`[SoT: Suy luận]` toàn mục này. Nền đã chốt: giữ `PENDING_VERIFICATION` (OTP chỉ cho tự đăng ký, vòng 2), OTP ở Redis khoá `identity:emailverify:<userId>` TTL 10
phút kèm bộ đếm sai, cùng cơ chế F1-17, khoá chặn gửi lại `identity:emailverify:resend:<userId>` 60 giây
[SoT: `02-bd/database/F1_identity.md:24, 220-221`; `DEC-2026-1022-identity-schema-sync` mục 3]. Phần dưới là hình dạng endpoint
do DD chốt làm hợp đồng triển khai cho F1-33; mã endpoint và path là quy ước DD. Phần chặn đăng nhập đã được owner chốt ở `DEC-2026-1022-self-register-pending-login-blocked`.

- `verify` request: `{ "email": string (bắt buộc), "otp": string (bắt buộc, 6 số) }`. Đúng OTP và `status = PENDING_VERIFICATION` thì
  `UPDATE users SET status = 'ACTIVE'`, xoá khoá Redis. Response `200`: `{ "message": "Xác thực email thành công." }`.
  Lỗi: `422 IDT-112` (OTP sai/hết hạn/quá 5 lần sai, thông báo chung, không phân biệt, theo mẫu `IDT-107`).
  Tài khoản đã `ACTIVE` gọi lại: `200` idempotent, không đổi gì `[SoT: Suy luận]`.
- `resend` request: `{ "email": string (bắt buộc) }`. Luôn `200` với câu chung "Nếu email cần xác thực, mã đã được gửi" kể cả email
  không tồn tại hoặc đã `ACTIVE` (không tạo OTP); trong 60 giây từ lần gửi trước trả `429 IDT-V12` (`03-dd/validation/F1_identity.md` mục 2).
- Thời điểm phát OTP: ngay khi `POST /auth/register` (#1) tạo tài khoản `PENDING_VERIFICATION`. **Sửa 2026-10-22 (đồng bộ DB vòng 2, DEC-2026-1022-db-sync-round2) mục 1:** ~~hoặc `POST /admin/users` (#50)~~ — tài khoản Admin tạo không phát OTP, chuyển `ACTIVE` ở lần đăng nhập đầu bằng mật khẩu tạm (BR-21).
- Không ghi `system_audit_logs` cho luồng này (khác `PASSWORD_RESET_REQUESTED`), vì RD F1-14 chưa nêu `[SoT: Suy luận]`.
- Hành vi `POST /auth/login` với tài khoản tự đăng ký `PENDING_VERIFICATION`: **chặn**, `403 IDT-113` (**Sửa 2026-10-22 (đồng bộ DB vòng 3, DEC-2026-1022-self-register-pending-login-blocked)**, thay "chưa chốt Q3"; Q3 phần đăng nhập đã đóng, hình dạng endpoint verify vẫn là đề xuất).

## 2. Sự kiện domain — hợp đồng tiêu thụ

Payload tối thiểu, thống nhất một hình dạng cho mọi sự kiện `identity` lắng nghe (mục 3.1 kiến trúc BD):

```json
{
  "eventId": "uuid",
  "eventType": "SubmissionGraded | SubmissionAccepted | SolutionReviewCompleted | MockInterviewFinished | StudentRemovedFromClass(phát ra, không tiêu thụ) | ClassDeleted(phát ra, không tiêu thụ)",
  "occurredAt": "ISO-8601",
  "payload": { "...": "theo từng eventType, xem bảng dưới" }
}
```

| eventType | Nguồn | `payload` tối thiểu | Cập nhật read model nào |
| :--- | :--- | :--- | :--- |
| `SubmissionGraded` | `judge-orchestration` | `{ userId, problemId, submissionId, verdict, passedTestcaseRatio, language, submittedAt, firstAttemptForProblem }` | `user_problem_best_score` (nếu `passedTestcaseRatio` tốt hơn best hiện tại; `best_verdict` = `verdict` của lượt đang giữ `best_ratio`), `user_submission_stats.total_submissions`/`accepted_count`/`first_try_accepted_count`, `language_counts` (tăng bộ đếm của khoá `language`) và `top_language` (khoá có đếm lớn nhất, hoà thì giữ giá trị hiện có), `user_daily_submission_stats` (upsert `submission_count + 1` tại `day` = ngày của `submittedAt` quy theo UTC+7). **Sửa 2026-10-22 (phase 5, ID-06, ID-12):** thêm `language_counts` và `user_daily_submission_stats` [SoT: `02-bd/database/F1_identity.md:189-193`]. Mọi cập nhật trong cùng một giao dịch và idempotent theo `eventId` `[SoT: Suy luận]`. **Sửa 2026-10-22 (đồng bộ DB vòng 2, DEC-2026-1022-db-sync-round2) mục 6:** payload thêm `submittedAt` (ISO-8601, UTC; thời điểm nộp, không phải thời điểm phát sự kiện) và `firstAttemptForProblem: boolean` (`judge-orchestration` tính; `true` chỉ với lượt nộp đầu tiên từ trước tới nay của người dùng cho bài đó — mặc định chủ dự án 2026-10-22). `identity` suy ngày UTC+7 từ `submittedAt` (đổi từ `occurredAt` để ngày không lệch khi chấm chậm qua đêm) và tăng `first_try_accepted_count` thêm 1 khi `firstAttemptForProblem = true` **và** `verdict = ACCEPTED` |
| `SubmissionAccepted` | `judge-orchestration` | `{ userId, problemId, submissionId, classId? }` | `identity_recent_activity` (`activity_type = SUBMISSION_ACCEPTED`, `scope_class_id = classId` nếu bài thuộc một `class_assignment`) |
| `SolutionReviewCompleted` | `ai-review` | `{ userId, problemId, reviewId }` | `identity_recent_activity` (`activity_type = SOLUTION_REVIEW_COMPLETED`) |
| `MockInterviewFinished` | `ai-review` | `{ userId, sessionId, overallScore }` | `identity_recent_activity` (`activity_type = MOCK_INTERVIEW_FINISHED`) |

Với `SubmissionGraded`, `identity` luôn tăng `total_submissions` và bộ đếm ngôn ngữ/ngày đúng một lần theo `eventId`. Chỉ khi `verdict != SYSTEM_ERROR` mới tăng `ac_eligible_count`; `accepted_count` chỉ tăng khi `verdict = ACCEPTED`. Do đó `acceptanceRatePercent` của #47 không bị lỗi hạ tầng kéo xuống. [Owner 2026-10-25, Q22]

Tập giá trị `activity_type` gồm đúng **năm** giá trị, khớp `identity_recent_activity.activity_type VARCHAR(40)` của BD
[SoT: `02-bd/database/F1_identity.md:195`]: `SUBMISSION_ACCEPTED`, `SOLUTION_REVIEW_COMPLETED`, `MOCK_INTERVIEW_FINISHED`,
`CLASS_PROBLEM_PUBLISHED` (nhận từ `problem-bank` khi giao bài mới cho lớp), `STUDENT_STREAK_ACHIEVED` (tự phát sinh nội bộ khi
cập nhật `user_submission_stats`). **Sửa 2026-10-22 (phase 5, ID-11):** ~~hai giá trị dạng "quét định kỳ"~~ không còn; không có
`CLASS_HAS_ABSENT_STUDENTS` (RD đã bỏ, `01-rd/req/F1_identity.md:246-249`; BR-18 ở `03-dd/logic/F1_identity.md` đã bỏ) và **không có
giá trị cho sự kiện "tham gia lớp"** (RD cũ nêu "join" nhưng BD chưa có, xem câu hỏi Q2 trong báo cáo).
Lưu trong cột `VARCHAR`, không phải ENUM của DB; danh sách này là hằng số của mã nguồn.

`identity` là **producer** của `StudentRemovedFromClass` (không tiêu thụ event này) khi endpoint 27 chạy
xong. ĐÃ ĐỔI 2026-10-14 (DEC-2026-1014-keep-learning-history-on-leave): `judge-orchestration` không còn xử lý/xoá dữ liệu lượt nộp khi nhận sự kiện; ~~để `judge-orchestration` tự xử lý dữ liệu lượt nộp trong phạm vi lớp~~
[SoT: `02-bd/database/F1_identity.md` mục 1.10].

**Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4):** `identity` còn là **producer** của `ClassDeleted` khi endpoint 22 chạy xong (BR-22).

| eventType phát ra | `payload` | Người nhận và hành vi |
| :--- | :--- | :--- |
| `ClassDeleted` | `{ classId }` (kèm `eventId`, `occurredAt` ở phong bì) | `problem-bank` đặt `removed_at = now()` cho các dòng `class_assignments` có `class_id = classId` (xoá mềm, idempotent: dòng đã có `removed_at` thì bỏ qua). `identity` không tiêu thụ. `[SoT: DEC-2026-1022-db-sync-round4 mục 3]` |

## 3. Đặc tả khoá tranh chấp/giao dịch

Không áp dụng cho phần lớn endpoint — các thao tác ghi của `identity` (đổi hồ sơ, đổi mật khẩu, tạo/xoá
lớp) là thao tác đơn người dùng trên bản ghi của chính họ, xung đột ghi đồng thời không có ý nghĩa nghiệp
vụ thật (client cuối cùng thắng theo `updated_at`).

**Trường hợp có race-condition thật**: đổi vai trò/khoá tài khoản (endpoint 36, 37) khi hai quản trị viên
thao tác đồng thời trên cùng một `userId`, hoặc thao tác trên chính người đang giữ vai trò ADMIN cuối cùng.
Xem BR-15 ở `03-dd/logic/F1_identity.md` — dùng khoá bi quan mức dòng (`SELECT ... FOR UPDATE` trên `users`)
trong phạm vi transaction của endpoint, không dùng optimistic lock vì thao tác admin có tần suất thấp và
cần chặn cứng ngay tại thời điểm ghi, không chấp nhận retry.

## 4. Ghi chú xung đột giữa hai nguồn BD (đã đóng)

Mục 0.6 (endpoint 33 `CreateRole`): `02-bd/architecture/F1_identity.md:98-104` ghi "Đã xác nhận 2026-09-13 —
role tuỳ biến là yêu cầu thật" dựa trên bằng chứng UI ở `ADM0202_permission_matrix.md`, nhưng
`07-review/bd_open_questions_with_solutions_260924.md:367` (mục `02-bd/database/F1_identity.md` việc còn mở
#3) lại đề xuất "giữ 3 role cố định dạng enum, không cần bảng `role` riêng". Hai nguồn cùng cấp BD mâu
thuẫn trực tiếp. DD này **theo xác nhận có ngày tháng cụ thể** (`02-bd/architecture/F1_identity.md:98-104`,
2026-09-13) vì nó dẫn bằng chứng UI cụ thể (`ADM0202_permission_matrix.md:32-34,48`) và đến sau về mặt thời
gian so với đề xuất suy luận chung chung trong tài liệu tổng hợp câu hỏi mở — không phải một thay đổi kiến
trúc mới do DD tự đề xuất, nên không mở decision entry. `02-bd/database/F1_identity.md` mục "Việc còn mở" #3
đã được cập nhật 2026-10-22: cần bảng `roles` thật; dòng tổng hợp ở `07-review` là đề xuất lịch sử, không còn là nguồn quyết định.

## 5. Tham chiếu

- `02-bd/architecture/F1_identity.md`, `02-bd/database/F1_identity.md`, `02-bd/security/F1_identity.md`.
- `07-review/bd_open_questions_with_solutions_260924.md` — đề xuất mặc định dùng làm quyết định DD.
- `03-dd/logic/F1_identity.md` — các khối BR dẫn ở bảng trên.
- `03-dd/validation/F1_identity.md` — độ mạnh mật khẩu, rate-limit, độ dài trường.
- `04-tdd/modules/F1_identity.md` — tiêu chí nghiệm thu `AC-nn` tương ứng.
