# DD màn hình — Hồ sơ (`USR0502`)

> Tuân khung `03-dd/screens/README.md` mục 4 (chín mục 0 đến 8) và quy tắc cứng mục 5. Request, response, mã lỗi nằm ở file API của module và chỉ được liệt kê theo ID. Không có logic backend ở đây. Màn có đúng **một biểu mẫu** (năm trường hồ sơ cộng email đi đường riêng) và một hộp thoại OTP; hai khối còn lại (tài khoản, luyện tập) chỉ đọc. Mật khẩu **không** ở màn này, đã chuyển sang `USR0503`.

## 0. Thông tin tài liệu

Bảng 0.1: Bìa tài liệu.

| Mục | Giá trị |
| :--- | :--- |
| Mã màn, slug | `USR0502`, `profile` |
| Tên màn | Hồ sơ |
| Nhóm actor | `users` (A1 `STUDENT`). Bản dựng còn tham số `area` cho khu giảng viên và quản trị (BD Q10: màn chỉ dựng cho A1), xem P5 mục 8 |
| Route | `/profile` (`app/(student)/profile`) |
| Bounded Context đứng sau | F1 `identity` (hồ sơ, đổi email); đọc F5 `ai-review` cho một con số |
| RD nguồn | `01-rd/screens/users/USR0502_profile.md` |
| BD nguồn | `02-bd/screens/users/USR0502_profile.md` (V0.9) |
| Prototype | `05-coding/frontend/src/views/users/profile/`, `features/edit-my-profile/`, `entities/user/` (toàn bộ mock) |
| Phiên bản, ngày | V0.1, 2026-10-24 |
| Tiêu chí nghiệm thu | Không có file `04-tdd/screens/users/USR0502_profile.md`: mọi hành vi đã có ở `04-tdd/modules/F1_identity.md` (AC-12, AC-13) hoặc là hành vi trình duyệt ghi ở mục 7 (README mục 2) |

Bảng 0.2: Lịch sử sửa.

| Ver | Mục | Nội dung | Ngày |
| :--- | :--- | :--- | :--- |
| V0.1 | Toàn bộ | Tạo mới từ prototype đã dựng, BD V0.9 và DD F1, F5. Chốt hai đường lưu độc lập, vòng đời hộp thoại OTP, nguồn khối "Luyện tập", đối chiếu zod với `validation/F1`; nêu các khoảng trống của F1 (mã lỗi, `phone`) | 2026-10-24 |

Bảng 0.3: Viết tắt nguồn trích dẫn.

| Viết tắt | Đường dẫn |
| :--- | :--- |
| RD, BD | như bìa |
| F1, F5 | `03-dd/api/F1_identity.md` (`#nn`), `03-dd/api/F5_ai-review.md` (`mục`) |
| V1, L1 | `03-dd/validation/F1_identity.md`, `03-dd/logic/F1_identity.md` |
| API | `03-dd/api/api.md` |
| PRV | `05-coding/frontend/src/views/users/profile/ui/` |
| FEP | `05-coding/frontend/src/features/edit-my-profile/` |
| EU | `05-coding/frontend/src/entities/user/` |
| DEBT | `06-plan/PROTOTYPE_DEBT.md` |
| DD-DB | `03-dd/screens/users/USR0601_dashboard.md` (dùng chung hook luyện tập) |

## 1. Bản đồ slice FSD

Thứ tự import `app → views → widgets → features → entities → shared`.

Bảng 1.1: Slice của màn.

| Lớp | Đường dẫn (từ `src/`) | Vai trò | Hiện trạng |
| :--- | :--- | :--- | :--- |
| app | `app/(student)/profile/page.tsx` | Render `ProfileView` | Có |
| views | `views/users/profile/ui/profile-view.tsx` | Dựng bố cục hai cột, chọn trạng thái lỗi của khối hồ sơ [SoT: PRV profile-view.tsx:35-90] | Có |
| views | `views/users/profile/ui/{identity-card,account-card,practice-card,security-card}.tsx` | Bốn khối chỉ đọc (danh tính, tài khoản, luyện tập, phương thức đăng nhập) | Có |
| features | `features/edit-my-profile/` (`personal-info-card`, `email-otp-dialog`, `model/use-edit-profile.ts`) | Biểu mẫu và hộp thoại OTP; sở hữu trạng thái `saved`, `fields`, `dirty`, `emailOtpTarget` [SoT: FEP model/use-edit-profile.ts:51-164] | Có |
| features | `features/edit-my-profile/model/use-profile-leave-guard.ts` | Gắn `useUnsavedChangesGuard(dirty)` và hộp thoại ba nút cho EVT-15 | Chưa có |
| entities | `entities/user/api/queries.ts` | `useMyProfile` (F1 #9) [SoT: EU api/queries.ts:82-89] | Có, mock |
| entities | `entities/user/api/mutations.ts` | `updateMyProfile` (F1 #10), `requestEmailChange` (F1 #11), `confirmEmailChange` (F1 #12) dạng hàm async thường, đúng quy ước của `entities/auth` [SoT: EU api/mutations.ts:1-5, 32-41] | Có, mock |
| entities | `entities/user/model/schema.ts` | zod: `profileFormSchema`, `emailChangeSchema`, `emailOtpSchema` [SoT: EU model/schema.ts:28-45, 79-84] | Có |
| entities | `entities/progress/api/queries.ts` | `useSolveSummary` (F1 #47), `useMyLevelProgress` (F2 §14A), `useInterviewSummary` (F5 mục 13); dùng chung với `USR0601` (DD-DB mục 1) | Có tên mock cũ, đổi theo DD-DB |
| shared | `Card`, `Skeleton`, `ErrorState`, `Button`, `TextField`, `Modal`, `UnsavedChangesDialog`, `shared/lib` (`toast`, `useUnsavedChangesGuard`, `formatDateVN`), `useErrorMessage` (DD-PD S3) | Dùng lại | Có, trừ `useErrorMessage` |

Không có thành phần mới nào cần nâng lên `shared/`.

## 2. API tiêu thụ

Bảng 2.1: Sự kiện BD, endpoint theo ID, hook, trạng thái nối. Hiện mọi dòng đều mock [SoT: EU api/queries.ts:76-80, api/mutations.ts:26-30]. Màn không dùng kênh thời gian thực.

| EVT (BD) | Endpoint theo ID | Hook | Nối |
| :--- | :--- | :--- | :--- |
| EVT-1 | F1 #9 `GetMyProfile` | `useMyProfile()` | mock |
| EVT-1 | F1 #47 (tổng lượt nộp) | `useSolveSummary()` | mock |
| EVT-1 | F2 §14A (số bài đã giải còn xuất bản) | `useMyLevelProgress()` | mock |
| EVT-1 | F5 mục 13 `GetMyInterviewSummary`, `limit = 1` (chỉ lấy `completedSessionCount`) | `useInterviewSummary("all", 1)` | mock |
| EVT-4 | F1 #10 `UpdateMyProfile` | `updateMyProfile(input)` | mock |
| EVT-5 | F1 #10 rồi F1 #11 `RequestMyEmailChange` (chạy độc lập, BR-SC-01) | `updateMyProfile`, `requestEmailChange` | mock |
| EVT-6 | F1 #12 `ConfirmMyEmailChange` | `confirmEmailChange(code, newEmail)` | mock |
| EVT-7 | F1 #11 (cùng endpoint gửi mã, gọi lại) | `requestEmailChange(newEmail)` | mock |
| EVT-16 | gọi lại đúng query của khối | `query.refetch()` | không áp dụng |
| EVT-2, EVT-3 (Hoàn tác), EVT-8, EVT-13, EVT-14, EVT-15 | không gọi API | state, `next/link`, `useUnsavedChangesGuard` | không áp dụng |
| EVT-9 đến EVT-12 | **đã chuyển hoặc bỏ**: mật khẩu sang `USR0503`, "Đổi ảnh" bỏ (BD:637-640) | không áp dụng | không áp dụng |

Điểm khác BD khi nối thật:

1. **Hai nguồn cho khối Luyện tập:** F1 #47 trả tổng lượt nộp lịch sử; F2 §14A trả số bài đã giải còn xuất bản. Không dùng `solvedProblemCount` của F1 #47 ở đây vì chỉ số lịch sử đó có thể gồm bài đã rút [SoT: F1 §0.8.4; F2 §14A; BD:557].
2. **Liên kết "Xem tiến độ chi tiết" đi `/dashboard`**, không phải `my_progress` như BD ghi [SoT: BD:392, 641; PRV practice-card.tsx:45]: `USR0501` đã gộp vào `USR0601` (`DEC-2026-0927-student-area-merge-and-shared-shell`). BD cần sửa chữ "my_progress" ở EVT-13 và Sheet 3.
3. **`passwordChangedAt` và `mustChangePassword`** là hai trường của phản hồi F1 #9 mà màn này **không hiển thị**: một cho `USR0503` (BD:548) và một cho việc chuyển hướng ở khung (F1:44). Màn này chỉ dùng `hasPassword` để vẽ thẻ "Email và mật khẩu" [SoT: BD:547].
4. Lưu hồ sơ không có endpoint "hồ sơ + email" gộp: F1 #10 loại `email` (`Server bỏ qua role_id/status` [SoT: F1:45]) nên hai đường tách đôi là bắt buộc (BD:607-609).

## 3. Trạng thái màn

Bảng 3.1: Trạng thái theo khối. Khoá i18n có tiền tố `profile.` hoặc `apiError.` (frontend không đọc `message`, API:22).

| Khối | Trạng thái | Điều kiện | Hiển thị | Khoá i18n |
| :--- | :--- | :--- | :--- | :--- |
| Toàn trang | Chưa đăng nhập | `401` sau khi làm mới token thất bại | Chuyển về `auth` | không áp dụng |
| A, B, C, D | Đang tải | F1 #9 chưa xong | `Skeleton` tại chỗ từng thẻ | không áp dụng |
| A, B, C, D | Lỗi | F1 #9 lỗi mạng hoặc `5xx` | **Một** trạng thái lỗi thay cả cột trái và thẻ "Tài khoản", kèm thử lại; khối E và lối ra Cài đặt vẫn hiện [SoT: PRV profile-view.tsx:48-78] | `loadError`, `retry` |
| B | Sạch | không trường nào khác giá trị đã lưu | Hai nút "Hoàn tác", "Lưu thay đổi" vô hiệu; câu gợi ý mặc định | `form.hint` |
| B | Có thay đổi chưa lưu | một trường khác giá trị đã lưu | Hai nút bật; câu "Có thay đổi chưa lưu." | `form.dirty` |
| B | Đang lưu | EVT-4 hoặc EVT-5 đang chạy | Nút "Lưu thay đổi" vô hiệu, các trường giữ nguyên giá trị đang nhập | không áp dụng |
| B | Lỗi trường | zod hoặc `422` từ F1 #10 | Viền ô sai đỏ + **một** toast lỗi đầu tiên; không xoá giá trị đang nhập | khoá `errors.*`, `apiError.IDT-V07` đến `V09`, `V13` |
| B | Email trùng, hết hạn mức | F1 #11 trả `409 IDT-415` hoặc `429 IDT-V05` | Ô Email viền đỏ + toast; **không** mở hộp thoại OTP; năm trường kia vẫn đã lưu | `apiError.IDT-415`, `apiError.IDT-V05` |
| G (OTP) | Chờ nhập | F1 #11 thành công | Hộp thoại, đếm ngược 10:00, ô nhập 6 số, "Gửi lại mã" khoá 60 giây | `emailOtp.*` |
| G (OTP) | Mã sai hoặc hết hạn | F1 #12 trả `422 IDT-416` | Hộp thoại **không đóng**, ô mã viền đỏ + toast; câu không phân biệt sai mã với hết hạn | `apiError.IDT-416` |
| G (OTP) | Hết 10 phút | bộ đếm về 0 | Vô hiệu ô nhập và nút "Xác nhận", chỉ còn "Gửi lại mã" | `emailOtp.expired` (mới) |
| C | Phương thức đăng nhập | `hasPassword` và `linkedProviders` | Thẻ "Email và mật khẩu" khi `hasPassword`, cộng một thẻ mỗi provider; tối đa 2 provider; không có dòng rỗng | `security.providerCredentials` |
| E | Số liệu rỗng | chưa nộp lần nào | Hiện `0`, đó là giá trị thật (khác `USR0601` hiện trạng thái rỗng vì nơi đó có mẫu số) | `practice.*` |
| E | F1 #47 lỗi | | Chỉ "Lượt nộp" in `-`, không toast | `practice.*` |
| E | F2 §14A lỗi | | Chỉ "Bài đã giải" in `-`, không toast | `practice.*` |
| E | **AI suy giảm** | F5 mục 13 lỗi hoặc quá hạn | "Phiên phỏng vấn" in `-`; **không** có thông báo lỗi nào, hồ sơ vẫn sửa và lưu được (BD Sheet 9 NO 12) | `practice.interviews` |
| Toàn trang | Quyền bị từ chối | không áp dụng | `403` không xảy ra: mọi endpoint là `/me/*`, phạm vi khoá theo token | không áp dụng |

Bảng 3.2: Ánh xạ lỗi ghi sang giao diện. Cột "Mã" chỉ có khi F1 đã cấp.

| Endpoint | HTTP, mã | Việc giao diện làm |
| :--- | :--- | :--- |
| F1 #10 | `400` hoặc `422`, `IDT-V07`, `IDT-V08`, `IDT-V09`, `IDT-V13` (độ dài, ký tự) | Đánh viền đúng trường theo mã, toast theo `apiError.<mã>`; kiểm client chặn trước nên hiếm |
| F1 #10 | thành công nhưng `roleId`, `status` gửi lên bị bỏ qua | Không áp dụng: màn **không bao giờ** gửi hai trường này (BD Sheet 9 NO 3) |
| F1 #11 | `429 IDT-V05` | Toast "bạn đã đổi email quá số lần cho phép, thử lại sau"; ô Email giữ giá trị, hộp thoại không mở |
| F1 #11 | email đã thuộc tài khoản khác | `409 IDT-415`; màn dùng `apiError.IDT-415` |
| F1 #12 | mã sai, hết hạn, đã dùng | `422 IDT-416`; cùng một câu thông báo, sau 5 lần sai bắt buộc "Gửi lại mã" (BD Sheet 9 NO 9) |
| F1 #9, #47, F5 mục 13 | `5xx`, mạng | Trạng thái khối theo Bảng 3.1 |

## 4. Quy tắc đặc thù màn

### BR-SC-01 Lưu hồ sơ và đổi email là hai đường độc lập

- **Kích hoạt:** EVT-4, EVT-5.
- **Tiền điều kiện:** mọi trường qua zod (mục 5) và email, nếu đổi, qua `emailChangeSchema`; lỗi client thì **không** gọi máy chủ.
- **Các bước:** (1) `emailChanged` = email đã cắt khoảng trắng khác email đã lưu **không phân biệt hoa thường** (khớp V1 mục 3; bản dựng đang so `!==` có phân biệt [SoT: FEP model/use-edit-profile.ts:101]); (2) gọi `Promise.allSettled([updateMyProfile(5 trường), emailChanged ? requestEmailChange(email) : null])`, hai lời gọi **không** phụ thuộc nhau (F1 #10 không có `email`); (3) F1 #10 thành công thì cập nhật `saved` (trừ email) và làm mới cache hồ sơ; (4) F1 #11 thành công thì mở hộp thoại OTP; (5) một đường lỗi có toast riêng, không làm mất kết quả đường kia [SoT: FEP model/use-edit-profile.ts:113-135].
- **Hậu điều kiện:** email đăng nhập **chỉ** đổi sau EVT-6; trong lúc chờ `GetMyProfile` vẫn trả email cũ (F1 AC-13, BD Sheet 9 NO 8). Chuỗi rỗng ở ba trường tự khai gửi nguyên là `""`, máy chủ lưu `NULL` `[SoT: Suy luận]`, vì V1 mục 5 chỉ nêu điều này cho `phone`.

### BR-SC-02 Vòng đời hộp thoại OTP đổi email

- **Kích hoạt:** EVT-5 (mở), EVT-6 (xác nhận), EVT-7 (gửi lại), EVT-8 (huỷ).
- **Quy tắc:** hai bộ đếm **ở client**, bắt đầu từ lúc F1 #11 trả thành công, còn nguồn thật là TTL 10 phút của khoá Redis [SoT: BD:410]: đếm ngược hiệu lực 10:00 và khoá nút "Gửi lại mã" 60 giây (khớp cooldown ở V1 mục 2). "Gửi lại mã" gọi lại **F1 #11 cho cùng email mới**, xoá ô mã, đặt lại cả hai bộ đếm. Bản dựng chưa có hai bộ đếm [SoT: FEP ui/email-otp-dialog.tsx:4-9].
- **Chống dò mã:** thông báo lỗi OTP **không** phân biệt sai mã với hết hạn (BD Sheet 9 NO 9); đếm số lần sai chỉ ở máy chủ, màn không đếm.
- **Huỷ (EVT-8):** đóng hộp thoại, giữ email cũ; ô Email trên biểu mẫu **giữ giá trị mới** và biểu mẫu trở lại "có thay đổi chưa lưu"; không tự hoàn tác (BD:636).
- **Thành công (EVT-6):** đóng hộp thoại, đặt `saved.email` = email mới, vô hiệu hoá cache `["user","my-profile"]` để khối danh tính và `USR0503` thấy email mới ngay [SoT: FEP model/use-edit-profile.ts:144-150].
- **Hộp thoại không tính vào kiểm "rời màn"** (BD:643): đóng bằng EVT-8.

### BR-SC-03 Rời màn khi còn thay đổi chưa lưu

- **Kích hoạt:** EVT-13, EVT-14, EVT-15 (liên kết của màn, mục nav, menu người dùng, nút Quay lại, đóng tab).
- **Các bước:** `useUnsavedChangesGuard(dirty)` với `dirty` của biểu mẫu; chặn thì mở hộp thoại ba nút dùng chung `UnsavedChangesDialog` ("Lưu và thoát" / "Thoát không lưu" / "Ở lại"), cùng hộp thoại đã chốt cho `USR0503` (`USR0503` BD Q12) thay cho hộp xác nhận hai nút của BD:643. "Lưu và thoát" chạy BR-SC-01; nếu việc lưu mở hộp thoại OTP thì **ở lại màn** (điều hướng bị huỷ), vì email chưa đổi xong; lưu lỗi thì ở lại.
- **Hiện trạng:** bản dựng **chưa** gắn khóa này (P3, mục 8) [SoT: FEP ui/personal-info-card.tsx không dùng guard].
- **Đóng tab:** chỉ có hộp thoại gốc của trình duyệt, không đổi được chữ (ghi chú đầu `useUnsavedChangesGuard`).

### BR-SC-04 Khối "Luyện tập" là ba con số độc lập

- **Quy tắc:** "Bài đã giải" từ F2 §14A, "Lượt nộp" từ F1 #47, "Phiên phỏng vấn" từ F5 mục 13 (`limit = 1`). Ba query độc lập; lỗi ở một nguồn chỉ làm ô tương ứng in `-`. Hiện số trần, **không** mẫu số, không chuỗi ngày hay tỉ lệ AC (BD:557). Liên kết "Xem tiến độ chi tiết" đi `/dashboard`.
- **Bài đã rút:** F2 §14A chỉ tính bài còn xuất bản, đúng yêu cầu không cộng bài đã rút (BD05:32); không dùng `solvedProblemCount` lịch sử của F1 #47.

### BR-SC-05 Làm mới cache sau thay đổi

- **Quy tắc:** sau EVT-4 thành công và sau EVT-6 thành công, vô hiệu hoá khoá `["user","my-profile"]` (mọi `area`). Cache này cũng là nguồn tên và email của khung (avatar) và của `USR0503`, và nguồn tên chào của `USR0601`; sửa tên ở đây phải thấy ngay ở các nơi đó.

### BR-SC-06 Dữ liệu tự khai là dữ liệu

- **Quy tắc:** `displayName`, `schoolOrCompany`, `currentPosition`, `targetPosition` hiện ở thẻ danh tính và ô nhập dưới dạng văn bản thuần (React escape), **không** dùng `dangerouslySetInnerHTML`. Chữ viết tắt avatar là hai chữ cái đầu của hai từ cuối `displayName`, viết hoa, tính ở màn (BD:340); mã người dùng là 8 ký tự đầu của `userId`, viết hoa (BD:379); **không** bao giờ hiện UUID đầy đủ.

## 5. Kiểm tra phía client

Bảng 5.1: Trường nhập, zod và quy tắc ở `validation/F1`.

| Trường | zod (đang có hoặc cần có) | Quy tắc ở `validation/F1` | Khớp |
| :--- | :--- | :--- | :--- |
| `displayName` | `z.string().trim().min(2, "errors.displayNameRequired").max(100)` | độ dài tối đa 100, nhắc từ BD (V1:3-4); **không nêu tối thiểu** | **Lệch có chủ đích:** bản dựng đang `min(1)` [SoT: EU model/schema.ts:8-12]; DD theo BD Sheet 9 NO 4 (tối thiểu 2 ký tự sau khi cắt, BD:662). Cần F1 `validation` ghi thêm tối thiểu 2 để máy chủ khớp (OQ-1) |
| `email` (khi đổi) | `.trim().min(1).max(254).email()` | RFC 5322, tối đa 254, so trùng không phân biệt hoa thường (V1:36-39); báo trùng trực tiếp, hạn mức `IDT-V05` 3 lần/24 giờ (V1:29) | Có về độ dài; `.email()` của zod chặt hơn RFC 5322 một chút, chấp nhận (`[SoT: Suy luận]`); so khớp "đã đổi chưa" không phân biệt hoa thường (sửa mã, BR-SC-01) |
| `schoolOrCompany` | `z.string().trim().max(150)` | `IDT-V09`, 150 (V1:52) | Có |
| `currentPosition` | `z.string().trim().max(100)` | `IDT-V07`, 100 (V1:50) | Có |
| `targetPosition` | `z.string().trim().max(100)` | `IDT-V08`, 100 (V1:51) | Có |
| `phone` | `z.string().trim().max(20).regex(/^[0-9 +\-()]*$/)` | `IDT-V13`, `VARCHAR(20)`, chỉ chữ số, khoảng trắng và `+ - ( )`, rỗng lưu `NULL` (V1:54) | **Thiếu ô trên giao diện** (P2); schema cần thêm cùng ô |
| Mã OTP đổi email | `z.string().length(6).regex(/^\d+$/)` | 6 chữ số (V1:43) | Có [SoT: EU model/schema.ts:79-84] |
| `roleId`, `status` | không có trong schema, **không** gửi | máy chủ bỏ qua (F1:45) | Có |

## 6. i18n và truy cập

**Tiền tố khoá.** `profile.*` ở `messages/vi.json:2603` và `messages/en.json:2603`; mã lỗi API ở `apiError.*` dùng chung. Khoá cần **thêm**: `form.hint`, `form.dirty`, `form.phone`, `form.emailTaken`, `errors.phoneInvalid`, `emailOtp.resend`, `emailOtp.expiresIn`, `emailOtp.expired`, `leaveConfirm.*` (dùng lại chuỗi của `USR0503` nếu có thể), `apiError.IDT-V05`, `apiError.IDT-V07`, `apiError.IDT-V08`, `apiError.IDT-V09`, `apiError.IDT-V13`. Không đặt tên endpoint, mã quyền hay mã lỗi vào nhãn hiển thị. Khoá `profile.passwordDialog.*` còn lại vì `USR0503` dùng lại [SoT: BD:83].

**Truy cập.**

- Mỗi ô nhập có nhãn; ô sai có `aria-invalid="true"` và nội dung lỗi nối bằng `aria-describedby` vào vùng lỗi (toast là bổ sung, không phải kênh duy nhất cho người dùng trình đọc màn hình).
- Hộp thoại OTP dùng `Modal` dùng chung (giữ focus, đóng bằng phím Escape); focus vào ô mã khi mở; bộ đếm 10:00 **không** nằm trong vùng `aria-live` (đọc từng giây gây nhiễu), chỉ thông báo một lần khi còn 1 phút và khi hết hạn `[SoT: Suy luận]`.
- Thứ tự focus: thẻ danh tính (không focus được), biểu mẫu theo thứ tự trường, "Hoàn tác", "Lưu thay đổi", thẻ phương thức đăng nhập (chỉ đọc), cột phải: liên kết "Xem tiến độ chi tiết", "Cài đặt tài khoản".
- Thẻ phương thức đăng nhập luôn kèm chữ, không chỉ biểu tượng.
- Trang cần đúng một `h1` (đã có dạng `sr-only` ở `profile.pageTitle`).

## 7. Điểm kiểm thử

Bảng 7.1: Điểm kiểm thử. "không có AC (trình bày)" là hành vi trình duyệt không có `AC` module.

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| T-PF-01 | Normal | EVT-1: hồ sơ đủ, khối luyện tập đủ ba số, thẻ phương thức đăng nhập đúng theo `hasPassword`, `linkedProviders` | Vitest | TDD màn `PF-AC-01` |
| T-PF-02 | Abnormal | EVT-1: F1 #9 lỗi, cột trái và thẻ "Tài khoản" thành một khối lỗi kèm thử lại; khối luyện tập và lối ra Cài đặt vẫn hiện | Vitest | TDD màn `PF-AC-01` |
| T-PF-03 | Abnormal | EVT-1: F5 mục 13 lỗi, ô "Phiên phỏng vấn" in `-`, **không** toast, hồ sơ vẫn sửa và lưu được | Vitest | TDD màn `PF-AC-01` |
| T-PF-04 | Normal | EVT-2, 3: sửa một trường bật hai nút; "Hoàn tác" trả mọi trường, không hỏi xác nhận | Vitest | TDD màn `PF-AC-02` |
| T-PF-05 | Normal | EVT-4: lưu năm trường gọi F1 #10, không gửi `email`, `roleId`, `status`; toast thành công; thẻ danh tính cập nhật tên | Vitest | F1 AC-12 |
| T-PF-06 | Boundary | EVT-4: tên chỉ có khoảng trắng hoặc một ký tự bị chặn ở client, không gọi máy chủ; 100 ký tự qua, 101 không | Vitest | TDD màn `PF-AC-02` |
| T-PF-07 | Abnormal | EVT-4: máy chủ từ chối, giữ nguyên giá trị đang nhập, viền ô sai + một toast | Vitest | TDD màn `PF-AC-02` |
| T-PF-08 | Normal | EVT-5: đổi email thì F1 #10 rồi F1 #11 chạy độc lập; #11 thành công mở hộp thoại OTP; email đăng nhập chưa đổi | Vitest | F1 AC-13 |
| T-PF-09 | Abnormal | EVT-5: F1 #10 lỗi nhưng F1 #11 thành công (và ngược lại): mỗi đường một toast, không mất kết quả đường kia | Vitest | TDD màn `PF-AC-03` |
| T-PF-10 | Boundary | EVT-5: chỉ đổi chữ hoa chữ thường của email không coi là đổi email | Vitest | F1 AC-13 |
| T-PF-11 | Abnormal | EVT-5: `429 IDT-V05`, không mở hộp thoại, toast theo mã | Vitest | TDD màn `PF-AC-03` |
| T-PF-12 | Normal | EVT-6: mã đúng đóng hộp thoại, cập nhật email ở biểu mẫu và khối danh tính, vô hiệu cache hồ sơ | Vitest | F1 AC-13 |
| T-PF-13 | Abnormal | EVT-6: mã sai, hộp thoại không đóng, ô mã viền đỏ, thông báo không phân biệt sai mã với hết hạn | Vitest | TDD màn `PF-AC-03` |
| T-PF-14 | Boundary | EVT-6, 7: bộ đếm 10:00 về 0 vô hiệu ô nhập; "Gửi lại mã" khoá 60 giây rồi mở; gửi lại xoá ô mã (giả đồng hồ) | Vitest | TDD màn `PF-AC-03` |
| T-PF-15 | Normal | EVT-8: huỷ hộp thoại giữ email cũ, ô Email giữ giá trị mới, biểu mẫu vẫn "chưa lưu" | Vitest | TDD màn `PF-AC-03` |
| T-PF-16 | Normal | EVT-13, 14: hai liên kết đi `/dashboard` và `/settings` | Vitest | TDD màn `PF-AC-04` |
| T-PF-17 | Normal | EVT-15: còn thay đổi chưa lưu thì mọi đường rời màn mở hộp thoại ba nút; "Lưu và thoát" mà phải nhập OTP thì ở lại | Vitest | TDD màn `PF-AC-04` |
| T-PF-18 | Normal | EVT-16: thử lại một khối chỉ gọi lại query của khối đó | Vitest | TDD màn `PF-AC-01` |
| T-PF-19 | Security | Trang không có đường nào gửi `user_id`; email, tên của tài khoản khác không hiện được | Vitest | F1 AC-12 |
| T-PF-20 | Normal | Luồng đầy đủ: sửa tên và email, nhập mã, thấy email mới ở khung và ở `USR0503` | Playwright | F1 AC-13 |

## 8. Nợ prototype và câu hỏi mở

**Dòng nợ sẽ gỡ.** Dòng `views/profile + entities/user (mở rộng)` của `DEBT` mục 9 (`06-plan/PROTOTYPE_DEBT.md:891`) có "Điều kiện tốt nghiệp: BE có identity endpoint 9-13 → viết DD theo trục màn → thay mock → test". DD xong chỉ hoàn thành bước "viết DD"; thay mock và test **chưa làm** nên dòng này **chưa gỡ**. Dòng `USR0502_profile` ở mục "Chưa có Prototype Next.js" (`:970`) lỗi thời từ lâu, không đụng.

Bảng 8.1: Lệch giữa prototype và thiết kế.

| # | Prototype hiện tại | Thiết kế | Nguồn |
| :-: | :--- | :--- | :--- |
| P1 | Toàn bộ mock | Nối F1 #9 đến #12, #47, F2 §14A, F5 mục 13 | EU api/queries.ts:76-80 |
| P2 | Biểu mẫu 5 ô, không có "Số điện thoại" | Thêm ô `phone` (BD V0.4 đã quyết, `DEC-2026-1013-a2-reads-student-contact`; BD Sheet 5 chưa chèn dòng) | BD:78; FEP model/use-edit-profile.ts:16-22 |
| P3 | Không có chặn rời màn | `useUnsavedChangesGuard` + hộp thoại ba nút (BR-SC-03) | BD:643 |
| P4 | Hộp thoại OTP không có đếm ngược và gửi lại | BR-SC-02 | FEP ui/email-otp-dialog.tsx:4-9 |
| P5 | `ProfileView` còn `area` cho giảng viên và quản trị | Gỡ khi A2, A3 có khung riêng (BD Q10) | PRV profile-view.tsx:35-40 |
| P6 | `displayName` `min(1)` | `min(2)` | EU model/schema.ts:8-12 |
| P7 | So `emailChanged` có phân biệt hoa thường | Không phân biệt hoa thường | FEP model/use-edit-profile.ts:101 |
| P8 | Khối luyện tập đọc hook mock tên BD cũ | Đọc `useSolveSummary`, `useMyLevelProgress`, `useInterviewSummary("all", 1)` | PRV practice-card.tsx:14-18 |
| P9 | Khối B hiện toast nhưng chưa nối `aria-describedby` cho lỗi từng ô | Mục 6 | FEP ui/personal-info-card.tsx |

**Khoảng trống của F1 cần DD module bổ sung** (DD màn không tự bù):

- **G1 đã đóng.** F1 #11/#12 mục 0.2.2 cấp `IDT-415` cho email trùng, `IDT-416` cho OTP sai/hết hạn/đã dùng; #10 áp validation F1 §5. "Gửi lại mã" gọi #11 và tính vào hạn mức `IDT-V05` theo `DEC-2026-1025-dd-email-resend-budget`.
- **G2 đã đóng.** F1 #47 mục 0.8.4 có DTO `GetMyProgressOverview`; F2 §14A cấp số bài đã giải chỉ trong tập còn xuất bản.
- **G3 đã đóng.** F1 #9 mục 0.2.1 liệt kê DTO hồ sơ và trường nullable.

**Câu hỏi mở mức màn.**

Bảng 8.2: Câu hỏi mở, mặc định đề xuất và bậc ponytail.

| # | Câu hỏi | Mặc định đề xuất | Bậc |
| :-: | :--- | :--- | :-: |
| OQ-1 | Đã đóng: `displayName` tối thiểu 2 ký tự sau trim | Validation F1 §5 cấp `IDT-V19`; tối đa 100 ký tự dùng `IDT-V20` | 6 |
| OQ-2 | **Đã chốt** `DEC-2026-1025-dd-email-resend-budget`: hạn mức đổi email 3 lần/24 giờ tính cả "Gửi lại mã" | Cùng #11, cùng bộ đếm theo `userId`; `IDT-V05` báo hết lượt | 6 |
| OQ-3 | Ô `phone` dựng ngay hay để lát cắt giảng viên | Dựng cùng đợt nối API; cột đã có, A2 đọc liên hệ học viên (`DEC-2026-1013-a2-reads-student-contact`) | 1 |
