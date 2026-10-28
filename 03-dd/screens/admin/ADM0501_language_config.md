# DD màn hình — Ngôn ngữ và giới hạn chấm (`ADM0501`)

## 0. Thông tin tài liệu

| Mục | Giá trị |
| :--- | :--- |
| Actor, route | A3 `ADMIN`; `/admin/language-config` |
| Nguồn | `01-rd/screens/admin/ADM0501_language_config.md`; `02-bd/screens/admin/ADM0501_language_config.md` V1.3; `03-dd/api/F4_judge-orchestration.md` §23–24 |
| Prototype | `05-coding/frontend/src/views/admin/language-config/` |
| Phiên bản | V0.1, 2026-10-25; DD trục màn, Mode B (agent-native) |

Màn có hai tab và một bản nháp chung. Ba ngôn ngữ `JAVA`, `CPP`, `PYTHON` cố định; không có thao tác thêm/xoá, không có “chấm lại”. [SoT: BD ADM0501 Sheet 4, Sheet 8; `DEC-2026-0828-remove-rejudge-scope`]

## 1. Bản đồ slice FSD

| Lớp | Đường dẫn từ `src/` | Vai trò |
| :--- | :--- | :--- |
| app | `app/(admin)/admin/language-config/page.tsx` | Chặn khu A3; quyền cụ thể vẫn do F4 kiểm |
| views | `views/admin/language-config/ui/admin-language-config-view.tsx` | Bố cục hai tab, bảng bốn cột, thanh lưu, hộp xác nhận tắt ngôn ngữ |
| features | `features/edit-language-config/` (tách khi nối API) | Bản `saved`/`draft`, kiểm đầu vào, xác nhận, chặn rời màn, xử lý `JDG-202` |
| entities | `entities/judge-config/api/queries.ts`, `model/adapter.ts` (tạo khi nối API) | GET/PUT F4, ánh xạ enum và tên trường của hợp đồng; không truyền mô hình mock trực tiếp vào request |
| shared | `Card`, `DataTable`, `SegmentedTabs`, `SaveBar`, `ConfirmDialog`, `UnsavedChangesDialog`, `useUnsavedChangesGuard`, `toast` | Dùng lại thành phần hiện có; không nâng quy tắc của riêng màn lên `shared` |

Prototype hiện đặt hook trong `views/admin/language-config/api/queries.ts` và tất cả trạng thái trong một view; tách biên giới khi nối API để `views` không sở hữu hợp đồng F4. Bộ chuyển đổi dữ liệu là cần thiết: mock dùng `key: py|cpp|java`, `timeMultiplier`, `outputLimitKb`, `compileTimeoutSec`, `networkAccess`; F4 dùng enum `PYTHON|CPP|JAVA`, `timeLimitMultiplier`, `maxOutputSizeKb`, `compileTimeoutSeconds`, `networkAccessEnabled`. `updatedAt` của bốn khối chỉ có ở F4, phải giữ đến lúc PUT. [SoT: `views/admin/language-config/model/types.ts:1-79`; F4 §23–24]

## 2. API tiêu thụ

| EVT theo BD | Endpoint | Hook / thao tác | Tình trạng |
| :--- | :--- | :--- | :--- |
| 1 mở màn | F4 #21 `GET /admin/language-config` (§23) | `useLanguageConfigPage` | Mock, `fetchReal` còn `NOT_IMPLEMENTED` |
| 2, 3, 4, 7, 8, 9, 10 sửa nháp/đổi tab/hoàn tác | Không gọi API | Cập nhật `draft`, giữ `saved` | Prototype có |
| 5 lưu sau xác nhận | F4 #22 `PUT /admin/language-config` (§24) | `useSaveLanguageConfigPage` | Mock, `fetchReal` còn `NOT_IMPLEMENTED` |
| 6 rời màn | Không gọi API | `useUnsavedChangesGuard` | Prototype có |
| 11 ghi nhật ký | Không gọi từ client | F4 §24 ghi qua `AuditLogPort` trong cùng giao dịch | Hợp đồng F4 có |

## 3. Trạng thái màn

| Điều kiện | Hành vi |
| :--- | :--- |
| GET đang tải | Skeleton cho thanh tiêu đề và khối nội dung; không dựng ba dòng giả |
| GET `403 JDG-301` | Trạng thái không đủ quyền; không hiện biểu mẫu |
| GET lỗi mạng/5xx | `ErrorState` và thử lại GET; không khởi tạo `draft` rỗng để tránh PUT ghi đè |
| Có dữ liệu, chưa sửa | Hai tab dùng chung `saved = draft`; thanh lưu ẩn, cột giới hạn hiệu dụng từ dữ liệu đang lưu |
| `draft` khác `saved` | Dấu thay đổi trên tab tương ứng, thanh lưu cho cả màn; đổi tab không làm mất nháp |
| Nút Lưu, có ngôn ngữ chuyển bật → tắt | Một hộp xác nhận gồm danh sách ngôn ngữ và `pendingJobCount` từ GET; huỷ hộp giữ nguyên nháp |
| PUT đang chạy | Khoá mọi ô và hai nút trên thanh lưu; không gửi lặp |
| PUT `409 JDG-202` | Giữ nháp, báo cấu hình đã đổi ở nơi khác, cho tải lại; không tự gửi lại với `updatedAt` mới |
| PUT lỗi kiểm `JDG-V10`–`V12`, `JDG-203`–`205` | Viền ô hoặc hàng gây lỗi và một toast theo mã; giữ nháp |
| PUT lỗi mạng/5xx | Giữ nháp và thanh lưu; toast lỗi, không báo thành công |
| PUT thành công | Lấy **response của PUT** làm `saved` và `draft` mới (kể cả `updatedAt` mới), xoá dirty, cập nhật cache GET, toast thành công |

## 4. Quy tắc đặc thù màn

### BR-SC-01 Một bản nháp xuyên hai tab

GET một lần cung cấp `languages`, `gradingDefaults`, `sandbox`, `generatorSandbox`. Bản nháp giữ cả bốn khối; đổi tab chỉ đổi nội dung nhìn thấy. `dirtyCount` đếm **chỉ các trường sửa được**, không đếm `updatedAt`, `pendingJobCount`, nhãn hoặc thứ tự hàng. Hoàn tác trả toàn bộ bản nháp về `saved`. Cột giới hạn hiệu dụng tính từ bản nháp bằng công thức của F4 §23/validation F4 §6, làm tròn như máy chủ, nhưng nút Lưu chỉ bật sau khi giá trị nhập qua kiểm. [SoT: BD Sheet 5 khu G; F4 §23; `model/validation.ts`]

### BR-SC-02 Lưu và xung đột

Bấm Lưu: kiểm toàn bộ bốn khối theo mục 5; nếu một ngôn ngữ chuyển bật → tắt so với `saved`, mở một hộp xác nhận, không mở ở mỗi toggle. Sau xác nhận gửi trọn request F4 §24 kèm `updatedAt` từng dòng. Lỗi `JDG-202` không tự nhập bản mới vào nháp vì có thể làm mất sửa của Admin; nút “Tải lại” GET bản mới, hỏi lại trước khi bỏ nháp cũ bằng hộp rời màn dùng chung. Thành công dùng response PUT làm baseline mới để lần Lưu kế tiếp có đúng `updatedAt`; không dùng `setSaved(draft)` như prototype vì draft mang phiên bản cũ. [SoT: BD EVT-5, Sheet 9 NO 7; F4 §24.3–24.4]

### BR-SC-03 Rời màn và quyền

`useUnsavedChangesGuard(dirtyCount > 0)` chặn điều hướng nội bộ, back và đóng/tải lại tab (hộp gốc của trình duyệt cho trường hợp cuối). “Lưu và rời đi” chạy BR-SC-02; nếu cần xác nhận tắt ngôn ngữ, hoàn tất cả hai hộp trước khi đi; PUT lỗi thì ở lại. Người có `LANGUAGE_CONFIG:READ` nhưng không `UPDATE` vẫn xem được cấu hình và không có nút Lưu khả dụng; F4 kiểm quyền ở PUT. [SoT: BD Sheet 8 NO 6, Sheet 9 NO 1]

## 5. Kiểm tra phía client

Schema khi nối thật đặt ở `features/edit-language-config/model/schema.ts`, đối chiếu [validation F4](../../validation/F4_judge-orchestration.md) §6:

| Trường | Quy tắc Zod | Mã F4 |
| :--- | :--- | :--- |
| `languages` | đúng ba enum khác nhau, đủ `JAVA`, `CPP`, `PYTHON` | `JDG-V10` |
| Hai hệ số mỗi ngôn ngữ | số 0,10–9,99, tối đa hai chữ số phần thập phân | `JDG-V10` |
| `enabled` | boolean; ít nhất một dòng bật | `JDG-203` |
| Bốn giới hạn mặc định | số nguyên ≥1; thời gian hiệu dụng mỗi dòng 100–30.000 ms, bộ nhớ 64–1.024 MB | `JDG-V11`, `JDG-204`, `JDG-205` |
| Ba công tắc sandbox | boolean; không gửi `maxChildProcesses` | F4 §24.2 |
| `generatorSandbox` | runtime nguyên 1–300 giây, output nguyên 1–1.024 MB | `JDG-V12` |
| `updatedAt` | chuỗi ISO-8601 giữ nguyên từ GET/PUT, không sửa bằng client | `JDG-202` khi xung đột |

Trường số cần giữ chuỗi gõ dở trong control (`""`, `"0,"`), rồi parse khi blur hoặc Lưu; không ép ô rỗng thành 0. `model/validation.ts` của prototype có một lệch cần sửa: `Object.values(page.defaults).some(badDefault)` sẽ coi `updatedAt` là số không hợp lệ nếu thêm thẳng DTO F4, nên schema và hàm đếm thay đổi chỉ lấy bốn trường số sửa được. [SoT: `model/validation.ts:69-74`; F4 §24.2]

## 6. i18n và truy cập

Dùng `adminLanguageConfig.*` và `apiError.JDG-*` trong hai ngôn ngữ; không in thông báo server thô. Bảng cần tiêu đề cột thực, nhãn hệ số gắn tên ngôn ngữ, giá trị hiệu dụng đọc được cùng đơn vị. Hai tab dùng `role=tablist`, `aria-selected`, điều khiển bằng mũi tên; dấu thay đổi cần văn bản cho trình đọc màn hình. Hộp xác nhận khi tắt ngôn ngữ đặt focus ở tiêu đề, Esc huỷ, đóng xong trả focus về Lưu. Lỗi đầu tiên được phát qua toast `role=alert`; sau lỗi kiểm đưa focus tới ô đầu tiên sai, kể cả ở tab khác.

## 7. Điểm kiểm thử

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| AL-01 | Normal | GET trả đúng ba ngôn ngữ và bốn khối; đổi tab giữ nháp | Vitest | TDD màn `AL-AC-01` |
| AL-02 | Boundary | Các biên hệ số và giới hạn hiệu dụng khớp validation F4 | Vitest | TDD màn `AL-AC-02` |
| AL-03 | Abnormal | Tắt ngôn ngữ cuối bị chặn; tắt một dòng chỉ hỏi một lần khi Lưu | Vitest | TDD màn `AL-AC-02` |
| AL-04 | Normal | PUT thành công cập nhật `saved`, `draft`, `updatedAt` từ response; lần Lưu sau không xung đột giả | Vitest | TDD màn `AL-AC-01` |
| AL-05 | Abnormal | `JDG-202` giữ nháp; tải lại bản mới cần xác nhận bỏ nháp | Vitest | TDD màn `AL-AC-03` |
| AL-06 | Abnormal | PUT lỗi mạng không toast thành công, không làm mất giá trị đã sửa | Vitest | TDD màn `AL-AC-03` |
| AL-07 | Security | READ thiếu trả màn không đủ quyền; UPDATE thiếu không gửi PUT dù đã sửa | Playwright | TDD màn `AL-AC-03` |
| AL-08 | Normal | Thay đổi ở cả hai tab được gửi trong một PUT; rời màn dirty có hộp chặn | Playwright | TDD màn `AL-AC-01`, `AL-AC-03` |
| AL-09 | Boundary | Tắt Python khi còn `PENDING` hiển thị số job, job đã chờ vẫn chạy theo F4 §24 | Vitest | TDD màn `AL-AC-02` |

## 8. Nợ prototype và câu hỏi mở

Dòng `views/admin/language-config` ở `06-plan/PROTOTYPE_DEBT.md` chưa tốt nghiệp: hook vẫn gọi mock, thiếu adapter F4 và giữ `saved = draft` sau PUT. `LanguageConfigPage` hiện không có các trường `updatedAt`/`pendingJobCount` của hợp đồng. `model/validation.ts` đếm khác biệt bằng mọi key; khi mở rộng DTO phải tách trường sửa được. Đây là nợ triển khai, không cần API mới.

| # | Câu hỏi | Kết luận DD |
| :--- | :--- | :--- |
| OQ-1 | Một PUT có phải mang cả hai tab và bốn khối? | Có, F4 §24.2 đã chốt |
| OQ-2 | Lưu xong dùng bản nháp hay phản hồi máy chủ? | Dùng phản hồi F4 §24.3 để nhận `updatedAt` mới |
| OQ-3 | **Đã chốt** `DEC-2026-1025-dd-admin-language-display-and-stack`: tên trình biên dịch đọc từ backend | F4 §23 GET và §24 PUT response có `languages[].compilerDisplayName` chỉ đọc; PUT request không nhận trường này |

TDD màn đã có tại `04-tdd/screens/admin/ADM0501_language_config.md`.
