# DD màn hình — Thiết lập (`USR0503`)

> Màn cài đặt ghép tuỳ chọn thiết bị với tuỳ chọn tài khoản. Hợp đồng API chỉ nằm tại F1; các quy tắc tương tác ở đây không lặp request/response.

## 0. Thông tin tài liệu

**Bảng 0.1: Bìa.**

| Mục | Giá trị |
| :--- | :--- |
| Mã màn, slug | `USR0503`, `settings` |
| Actor | A1 `STUDENT`; một số khối dùng chung với giảng viên |
| Route | `/settings` (student shell) |
| Nguồn | `01-rd/screens/users/USR0503_settings.md`; `02-bd/screens/users/USR0503_settings.md` |
| Prototype | `05-coding/frontend/src/views/users/settings/` (mock) |
| Module | F1 `identity`; danh sách ngôn ngữ bật từ F4 `judge-orchestration` #24 |
| Phiên bản, ngày | V0.1, 2026-10-24 |
| TDD | `04-tdd/modules/F1_identity.md` AC-48; `04-tdd/modules/F4_judge-orchestration.md` AC-72; `04-tdd/screens/users/USR0503_settings.md` |

**Bảng 0.2: Lịch sử sửa.**

| Ver | Nội dung | Ngày |
| :--- | :--- | :--- |
| V0.1 | Tạo DD màn, ghi rõ phần lưu theo thiết bị và theo server, cập nhật V0.7 mật khẩu | 2026-10-24 |

**Bảng 0.3: Ký hiệu nguồn.**

| Ký hiệu | Nguồn |
| :--- | :--- |
| RD, BD | `01-rd/screens/users/USR0503_settings.md`, `02-bd/screens/users/USR0503_settings.md` |
| F1, V1, T1 | `03-dd/api/F1_identity.md`, `03-dd/validation/F1_identity.md`, `04-tdd/modules/F1_identity.md` |
| F4 | `03-dd/api/F4_judge-orchestration.md` mục 24B; `04-tdd/modules/F4_judge-orchestration.md` AC-72 |
| V | `05-coding/frontend/src/views/users/settings/` và `05-coding/frontend/src/features/update-account-preferences/model/use-account-preferences.ts` |
| DEBT | `06-plan/PROTOTYPE_DEBT.md` |

## 1. Bản đồ slice FSD

**Bảng 1.1: Slice của màn.**

| Lớp | Đường dẫn từ `src/` | Vai trò | Hiện trạng |
| :--- | :--- | :--- | :--- |
| app | `app/(student)/settings/page.tsx` | Render settings theo actor | Có |
| views | `views/users/settings/ui/settings-view.tsx` | Ghép các thẻ tuỳ chọn, tải hồ sơ và xử lý trạng thái lỗi | Có, mock |
| views | `views/users/settings/ui/preferences-form.tsx` | Gom thay đổi server/device và lưu theo nhóm | Có; rà soát theo BR-SC-01 |
| views | `views/users/settings/ui/{appearance-card,security-card,data-export-card,danger-zone-card}.tsx` | Giao diện/Ngôn ngữ, mật khẩu, xuất dữ liệu, xoá tài khoản | Có |
| features | `features/update-account-preferences/model/use-account-preferences.ts` | Quản lý draft và lưu; prototype còn tách lời gọi F5 cũ | Có; phải đổi theo F1 #15 [SoT: V `use-account-preferences.ts:1-129`] |
| features | `features/change-my-password`, `features/deactivate-my-account`, `features/export-my-data` | Dialog mật khẩu, xác nhận xoá, tải tệp | Có |
| entities | `entities/user/api/queries.ts` | F1 #14 `GetMySettings`, F1 #9 `GetMyProfile` | Mock |
| entities | `entities/user/api/mutations.ts` | F1 #15 `UpdateMySettings`, F1 #13 `ChangeMyPassword`, F1 #16-18 export/delete | Mock/chưa đủ |
| entities | `entities/language/api/queries.ts` (đích) | F4 #24 `ListEnabledSubmissionLanguages`, dùng cho Select ngôn ngữ mặc định | Chưa có |
| shared | `UnsavedChangesDialog`, `Modal`, `Button`, `TextField`, `Select`, `Switch`, `toast`, `useT`, `useLocale` | Thành phần chung | Tái sử dụng |

Không có widget mới. Tuỳ chọn thiết bị đi qua bộ lưu cục bộ; `defaultLanguage` đi F1 #15 cùng thông báo và mặc định phỏng vấn. Cần đưa state dirty của từng nguồn vào feature, không thêm logic này vào component chung. Chiều import giữ `app → views → widgets → features → entities → shared`.

## 2. API tiêu thụ

**Bảng 2.1: Ánh xạ sự kiện sang API.**

| EVT (BD) | ID hợp đồng | Hook/mutation đích | Nối |
| :--- | :--- | :--- | :--- |
| EVT-1 khởi tạo | F1 #14 `GetMySettings`; F1 #9 `GetMyProfile`; F4 #24 `ListEnabledSubmissionLanguages` | `useMySettings()`, `useMyProfile()`, `useEnabledSubmissionLanguages()` | Mock; danh sách ngôn ngữ hardcode |
| EVT-2, EVT-3 giao diện và locale | Không API; `NEXT_LOCALE`/theme theo thiết bị | Locale router, `next-themes` | Client/device |
| EVT-4 Workspace | Cỡ chữ, tự lưu, Vim: lưu thiết bị; `defaultLanguage`: F1 #15 khi EVT-7 | `useAccountPreferences` tách dirty theo nguồn | Mock; BD Sheet 5 Khu vực B NO 2-5 |
| EVT-5 preferences phỏng vấn | F1 #15 `UpdateMySettings` khi lưu | `updateMySettings(input)` | Mock; BD cần bỏ endpoint F5 cũ |
| EVT-6 thông báo | F1 #15 `UpdateMySettings` khi lưu | `updateMySettings(input)` | Mock |
| EVT-7 lưu cài đặt | F1 #15 `UpdateMySettings` | `updateMySettings(input)` | Mock |
| EVT-8 xuất lượt nộp | F1 #16 `ExportMyData?scope=submissions` | `exportMyData("submissions")` | Mock; trả tệp đồng bộ |
| EVT-9 xuất transcript | F1 #17 `ExportMyInterviewTranscripts` | `exportMyInterviewTranscripts()` | Mock; lỗi upstream độc lập |
| EVT-10 về hồ sơ | Không gọi API | Link `/profile` | Client |
| EVT-11 mở dialog xoá | Không gọi API | Local dialog state | Client |
| EVT-12 xác nhận xoá | F1 #18 `DeleteMyAccount` | `deleteMyAccount(email)` | Mock; logout sau thành công |
| EVT-13 huỷ xoá | Không gọi API | Local dialog state | Client |
| EVT-14 rời khi dirty | Không gọi API | `UnsavedChangesDialog` | Client |
| EVT-15 thử lại tải settings | F1 #14 `GetMySettings` | `query.refetch()` | Mock |
| EVT-16 mở dialog password | F1 #9 đã có profile state | Local dialog state | Client |
| EVT-17 gõ mật khẩu | Không gọi API | Client schema | Client |
| EVT-18 hiện/ẩn mật khẩu | Không gọi API | Local input type state | Client |
| EVT-19 xác nhận đổi/đặt mật khẩu | F1 #13 `ChangeMyPassword` | `changeMyPassword(input)` | Mock |
| EVT-20 huỷ dialog password | Không gọi API | Local dialog state | Client |

F4 #24 đã cung cấp danh sách ngôn ngữ đang bật theo `DEC-2026-1024-dd-student-language-contracts`; không dùng API quản trị `ADM0501`. Bản dựng còn gọi `useInterviewPreferences` và `updateInterviewPracticePreferences` ở F5; từ `DEC-2026-1022-db-sync-round4` hai thao tác này đã chuyển về F1 #14-15 [SoT: F1:54-55; BD:719-722].

## 3. Trạng thái màn

**Bảng 3.1: Trạng thái theo nhóm.**

| Nhóm | Trạng thái | Hiển thị/xử lý | Khoá i18n |
| :--- | :--- | :--- | :--- |
| Tải cài đặt | Loading | Skeleton, không ghi giá trị mặc định đè lên server trước khi tải xong | `settings.loading` |
| Tải hồ sơ | Lỗi | Khối Bảo mật/Vùng nguy hiểm hiển thị lỗi và retry riêng; các tuỳ chọn vẫn dùng được | `apiError.*` |
| Danh sách ngôn ngữ | F4 #24 lỗi | Giữ giá trị đã lưu để đọc; khoá Select cho đến khi tải lại được danh sách bật, không cho lưu một lựa chọn chưa kiểm | `apiError.*` |
| Ngôn ngữ đã lưu | F4 #24 không còn chứa mã F1 #14 trả về | Hiện mã cũ kèm thông báo cần chọn lại; chỉ mã đang bật được chọn để lưu | Khoá i18n mới dưới `settings.*` |
| Nhóm F1 | Lưu thành công | Xoá dirty của `defaultLanguage`, thông báo và mặc định phỏng vấn theo cùng phản hồi F1 #15 | `settings.saveBar.saved` |
| Nhóm F1 | Lưu lỗi | Giữ các thay đổi F1 để thử lại; lỗi theo `error.code` | `settings.saveBar.saveFailed`, `apiError.*` |
| Nhóm thiết bị | Lưu cục bộ | Commit cỡ chữ, tự lưu, Vim ở thiết bị khi bấm Lưu; dirty cục bộ độc lập với F1 | `settings.saveBar.saved` |
| Xuất dữ liệu | Đang tải | Khoá nút của cùng loại tệp; cho loại khác hoạt động | `settings.export.*` |
| Xuất transcript | F5 upstream lỗi | Toast lỗi, nút vẫn kích hoạt lại | `apiError.*` |
| Đổi mật khẩu | Validation/API lỗi | Giữ popup; lỗi field gắn đúng trường; không xoá mật khẩu đã nhập trừ khi chính sách bảo mật yêu cầu | `apiError.*`, `settings.password.*` |
| Xoá tài khoản | Đang gửi/lỗi | Khoá xác nhận khi gửi; lỗi không đóng dialog và không logout | `settings.delete.*`, `apiError.*` |

Frontend ánh xạ `error.code`, không dựa vào `message` (`03-dd/api/api.md` mục 1.1).

## 4. Quy tắc đặc thù màn

**BR-SC-01 — Lưu tách theo nguồn sở hữu.** Kích hoạt EVT-7 sau khi người học sửa Workspace/Phỏng vấn/Thông báo. Theme và locale do bộ chuyển chung ghi theo thiết bị; trong Workspace, chỉ cỡ chữ/tự lưu/Vim lưu cục bộ, còn `defaultLanguage` cùng hai cờ thông báo và ba mặc định phỏng vấn gửi **một** F1 #15. Feature theo dõi dirty của cục bộ và F1 riêng; ghi thành công nguồn nào thì xoá dirty nguồn đó, lỗi F1 không xoá thay đổi chờ lưu. Hậu điều kiện: một lần lưu F1 phản ánh cùng một snapshot của sáu tuỳ chọn tài khoản, không gọi endpoint F5 đã lỗi thời.

**BR-SC-02 — Xuất dữ liệu.** Kết quả API là tệp tải về, không render payload trong trang. Chặn thao tác trùng cùng loại khi đang tải và tôn trọng rate-limit 5 phút theo contract F1. Tên tệp/format lấy từ response; không tự dựng nội dung CSV/JSON phía client.

**BR-SC-03 — Mật khẩu.** Dựa vào `hasPassword` từ F1 #9 để chọn “Đổi mật khẩu” (yêu cầu mật khẩu hiện tại) hoặc “Đặt mật khẩu” lần đầu. Xác nhận mật khẩu lần hai chỉ là kiểm client. Sau lần đổi thành công hiển thị thông báo phù hợp; chỉ lời gọi server mới cập nhật phiên.

**BR-SC-04 — Xoá tài khoản.** Nút mở dialog xác nhận; người dùng phải nhập đúng email tài khoản. Chỉ khi khớp mới bật nút xác nhận. Sau response thành công, xóa dữ liệu nhạy cảm khỏi state client, đăng xuất và chuyển tới luồng auth/recovery theo contract. Không optimistic logout trước khi server xác nhận.

**BR-SC-05 — Rời trang khi còn thay đổi.** Dùng `UnsavedChangesDialog` ba lựa chọn chung. “Lưu và thoát” commit phần thiết bị và gọi F1 #15 cho phần tài khoản; chỉ điều hướng khi cả hai nguồn cần lưu đã thành công. Nếu F1 lỗi, giữ người dùng ở lại và giữ dirty F1; phần cục bộ đã commit không bị hoàn tác. Chỉ các đường rời do màn/guard kiểm soát mới được chặn theo khả năng thực của prototype; nếu có đường khác chưa chặn, ghi nợ thay vì hứa bảo vệ toàn bộ [SoT: BD Sheet 8 EVT-14].

## 5. Kiểm tra phía client

**Bảng 5.1: Ánh xạ kiểm tra.**

| Trường | Zod/kiểm tra | Quy tắc DD | Khớp |
| :--- | :--- | :--- | :--- |
| Mật khẩu hiện tại/mới | `entities/user/model/schema.ts` | F1 validation mục 1; mật khẩu mới 8–128 ký tự, chữ và số; trường hiện tại vắng khi OAuth chưa có password | Có, cần giữ đồng bộ |
| Xác nhận mật khẩu | schema client `passwordConfirmation` | So khớp mật khẩu mới; không gửi lên API | Client-only, đúng |
| Email xác nhận xoá | `z.string().refine(v => v === profile.email)` | V1 mục 5: khớp tuyệt đối, phân biệt hoa thường với email đang lưu; server xác minh lại | Có; **không trim/lower-case** |
| `preferredMaxTurns` | `z.union([z.literal(8),z.literal(12),z.literal(16)])` | V1 mục 8 | Cần đổi prototype đang dùng chuỗi `"12"` |
| `preferredHintAllowed` | `z.boolean()` | V1 mục 8 | Có sau khi gộp DTO F1 |
| `preferredInterviewLevel` | `z.enum(["INTERN","JUNIOR","MIDDLE","SENIOR"])` | V1 mục 8 | Có |
| `defaultLanguage` | `z.enum(["JAVA","CPP","PYTHON"])`; Select chỉ nhận mã từ F4 #24 | V1 mục 8; F4 mục 24B | Có; F1 kiểm enum, F4 là nguồn trạng thái bật |
| Hai cờ thông báo | `z.boolean()` | F1 #14-15; BD Sheet 5 | Có |

Không có form cho các thẻ không ghi dữ liệu. Không tự giới hạn/export file dựa vào client ngoài rate-limit hiển thị.

## 6. i18n và truy cập

Tiền tố `settings.` và `apiError.`. Switch có tên truy cập gắn với nhãn, trạng thái bật/tắt được đọc bằng công nghệ hỗ trợ. Các dialog có tiêu đề, mô tả hành động, focus trap và trả focus. Vùng phản hồi lưu/xuất dùng `aria-live="polite"`; lỗi kiểm tra gắn qua `aria-describedby`. Nút nguy hiểm có nhãn xác nhận cụ thể, không chỉ màu đỏ.

## 7. Điểm kiểm thử

**Bảng 7.1: Điểm kiểm thử và tiêu chí.**

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| SC-01 | Normal | EVT-1 tải cài đặt/hồ sơ/ngôn ngữ bật; EVT-2..6 phân nguồn giá trị | Vitest | F1 AC-44, AC-48; F4 AC-72; `settings` AC-SC-01 |
| SC-02 | Normal | EVT-7 lưu preferences, chỉ clear dirty nhóm thành công | Vitest | F1 AC-44, AC-48; `settings` AC-SC-02 |
| SC-03 | Abnormal | EVT-7 một nhóm lưu lỗi giữ dirty; phần còn lại vẫn dùng được | Vitest | `settings` AC-SC-02; BD Q11 |
| SC-04 | Normal | EVT-8/9 tải file thay vì render payload | Vitest | F1-22; F5 AC-51 |
| SC-05 | Boundary | EVT-8/9 rate limit export hiển thị lỗi và cho thử lại sau thời hạn | Vitest | TDD màn `AC-SC-03` |
| SC-06 | Normal | EVT-16..20 đặt/đổi mật khẩu, OAuth không cần mật khẩu cũ | Vitest | F1 AC-14, AC-15 |
| SC-07 | Abnormal | EVT-12 email sai không submit; lỗi giữ dialog; thành công logout | Playwright | F1 AC-16, AC-17 |
| SC-08 | Boundary | EVT-14 rời màn khi dirty mở dialog đúng, focus được giữ | Playwright | `settings` AC-SC-02 |
| SC-09 | Abnormal | EVT-9 transcript lỗi không khoá nút; EVT-15 retry refetch | Vitest | F5 AC-51; TDD màn `AC-SC-04` |
| SC-10 | Boundary | EVT-10..13 hồ sơ link và dialog xoá có thể mở/huỷ | Playwright | F1 AC-16 |

## 8. Nợ prototype và câu hỏi mở

Nợ ở `06-plan/PROTOTYPE_DEBT.md:892` và mục 16.3 còn mở. Prototype vẫn gọi `useInterviewPreferences`/`updateInterviewPracticePreferences` sang F5, dùng danh sách ngôn ngữ bật hardcode, và một dirty flag cho các nhóm. Khi nối thật, bỏ F5 call cũ, dùng F1 #14-15, lấy ngôn ngữ đang bật qua F4 #24, quản lý dirty theo nguồn, và tải tệp F1 #16-17. Chỉ gỡ debt sau khi code khớp DD.

| ID | Nội dung | Đề xuất mặc định | Bậc ponytail |
| :--- | :--- | :--- | :--- |
| D-1 | Prototype hardcode ngôn ngữ trong khi DD đã có F4 #24 | Đổi sang F4 #24; nếu mã đã lưu bị tắt, yêu cầu chọn mã đang bật trước khi lưu trường này | 2 |
| D-2 | BD Q11 và prototype còn mô tả hai lời gọi F1/F5; DB sync 2026-10-22 đã chuyển preferences phỏng vấn về `identity` | Tuân theo F1 #15 một lần lưu tài khoản, giữ dirty state thiết bị riêng; đồng bộ BD và code ở lượt sau | 3 |
| D-3 | Câu chữ toast đổi mật khẩu lần đầu/đổi mật khẩu khác nhau theo quyết định 2026-10-21 | Giữ phân biệt `Đã đặt mật khẩu` và `Đã đổi mật khẩu...`; đối chiếu khóa i18n hiện có | 3 |
