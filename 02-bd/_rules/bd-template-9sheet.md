# Quy ước BD theo mẫu 9 sheet

Áp dụng cho mọi file BD **trục màn hình** trong `02-bd/screens/**`. Nguồn khung: mẫu tài liệu thiết kế
9 sheet ở `11-layoutDocx/bd/JKpi0910_screen_design.md`, đã Việt hoá và cắt bỏ phần chỉ có nghĩa với dự án gốc.

File này viết **một lần**, mọi BD chỉ trỏ tới, không chép lại. Trục Bounded Context
(`02-bd/architecture|database|security|storage`) **không** dùng mẫu này — vẫn viết theo cấu trúc hiện có.

## 1. Khung 9 sheet

| Sheet | Tên | Nội dung |
| :--- | :--- | :--- |
| 1 | Bìa | Hệ thống, phân hệ, công đoạn, tên và tên vật lý màn, actor, phiên bản, người và ngày tạo/cập nhật |
| 2 | Lịch sử sửa đổi | Ver, sheet bị sửa, nội dung sửa, ngày, người sửa |
| 3 | Sơ đồ chuyển màn | Từng cặp "từ đâu tới đâu" theo 6 thẻ cố định, kèm một sơ đồ Mermaid |
| 4 | Bố cục màn hình | 4.1 tổng quan theo thẻ · 4.2 DTO · 4.3 bảng dữ liệu · 4.4 vùng bố cục bám prototype · 4.5 cấu trúc slice FSD |
| 5 | Danh sách item màn hình | Từng item trên màn, chia theo khu vực |
| 6 | Đặc tả điều khiển item | Điều kiện hiển thị và kích hoạt của từng item |
| 7 | Danh sách truyền dữ liệu | 7.1 trường DTO · 7.2 truy cập bảng + CRUD · 7.3 danh sách endpoint |
| 8 | Danh sách sự kiện | Mọi sự kiện trên màn, đánh `EVT-nn` |
| 9 | Đặc tả kiểm tra | Kiểm quyền, kiểm nhập liệu, kiểm nghiệp vụ |
| — | Câu hỏi mở | Phần riêng của AlgoPrep, mẫu gốc không có. Giữ ở cuối file |

Mẫu gốc có phần "Quy ước ID và Checklist kiểm toán" đặt trong từng file. AlgoPrep đặt ở đây, file BD chỉ
ghi một dòng trỏ tới `02-bd/_rules/bd-template-9sheet.md`.

## 2. Bộ thẻ đóng

Ô ghi chú **không viết văn xuôi tự do**. Chỉ dùng các thẻ dưới đây, nối nhiều thẻ bằng `<br>`, và giữ đúng
thứ tự dòng đã quy định. Tự chế thẻ mới là sai quy ước.

| Sheet | Thẻ được dùng | Thứ tự dòng bắt buộc |
| :--- | :--- | :--- |
| 3 | `[Điều kiện mở]` `[Chế độ mở]` `[Thông tin truyền]` `[Giá trị trả về]` `[Khi thành công]` `[Khi huỷ]` | Đúng thứ tự này, đủ cả 6 thẻ, không có thì ghi "Không có" |
| 4.1 | `[Mục đích màn]` `[Luồng nghiệp vụ chính]` `[Người dùng]` `[Tệp liên quan]` `[Phạm vi]` `[Quyền sử dụng]` `[Số bản ghi tối đa]` | Đúng thứ tự này |
| 5 | `[Nguồn giá trị]` `[Công thức]` `[EVT liên quan]` | Dòng 1 ý nghĩa item (không có thẻ) · dòng 2 nguồn giá trị hoặc công thức · dòng 3 `[EVT liên quan]` |
| 6 | `[Điều kiện hiển thị]` `[Điều kiện kích hoạt]` `[Tự động đặt]` `[Tự động xoá]` | Dòng 1 hiển thị · dòng 2 kích hoạt · dòng 3 thay đổi giá trị tự động |
| 7.1 | `[Nguồn]` `[Đích]` `[Chuyển đổi]` | Chỉ ghi khi các cột còn lại chưa đủ xác định hướng truyền hoặc cách biến đổi |
| 8 | `[Các bước]` `[Khi thành công]` `[Khi lỗi]` `[Khi xác nhận]` `[Thông báo hoàn tất]` | `[Các bước]` trước, các thẻ còn lại theo thứ tự trên, bỏ thẻ không áp dụng |
| 9 | `[Nội dung kiểm]` `[Nơi thực thi]` `[Tiêu điểm]` | Đúng thứ tự này |

## 3. Ký hiệu và giá trị chuẩn

- Cột `Hiển thị` ở Sheet 6: `Có` luôn hiển thị · `Không` không hiển thị · `Điều kiện` hiển thị có điều kiện.
  Giá trị `Điều kiện` **bắt buộc** kèm điều kiện ở cột ghi chú.
- Không có dữ liệu thì ghi `-`, **không để trống ô**.
- `Loại UI`: `Label` `TextBox` `TextArea` `NumberBox` `Button` `Link` `Toggle` `Badge` `List` `ListColumn`
  `ProgressBar` `Popup`.
- `Kiểu`: `String` `Number` `Boolean` `Date` `List` `Enum`.
- `I/O`: `I` người dùng nhập · `O` hệ thống hiển thị · `I/O` hệ thống đặt giá trị ban đầu, người dùng sửa được.
- Cột `Chuyển màn` ở Sheet 8: `Có` khi mở một màn có slug riêng; `Không` khi chỉ đổi nội dung trong cùng
  màn, kể cả mở popup.
- Thuật ngữ: có slug riêng trong `01-rd/screens/` là **màn hình**; không có slug riêng, mở chồng lên màn
  hiện tại là **popup**.
- **Kênh thông báo** (`DEC-2026-1003-toast-feedback-channel`, 2026-10-03): kết quả thao tác (lưu, xoá, tìm
  kiếm, xuất bản, lỗi hệ thống) và lỗi nhập liệu đều hiện bằng **toast** dùng chung, ở góc phải-trên, tự tắt
  sau vài giây, có nút X. Ô nhập sai chỉ **đổi viền đỏ**, không có chữ lỗi cạnh ô; mỗi lần gửi chỉ có **một**
  toast, nội dung là lỗi đầu tiên. Khi viết BD: `[Khi lỗi]` và `[Thông báo hoàn tất]` ở Sheet 8 ghi "toast",
  `[Tiêu điểm]` ở Sheet 9 ghi "viền ô + toast". **Không viết** "báo lỗi tại ô", "ngay dưới trường", "banner"
  cho kết quả thao tác. Ngoại lệ là nội dung giải thích trạng thái của trang (lý do một nút bị khoá, cảnh
  báo cấu hình đang có hiệu lực), vùng thay chỗ nội dung khi tải lỗi hoặc rỗng, và nhãn tĩnh: các thứ này
  là nội dung trang, không phải thông báo. Hai quy tắc đi kèm: (a) nút bị chặn bởi một trạng thái trang đang giải thích (lý do nút bị khoá) **trông như bị khoá nhưng vẫn bấm được**, bấm thì toast cảnh báo nêu lý do, nên Sheet 6 ghi `[Điều kiện kích hoạt]` là "bấm được, bị chặn kèm toast" thay vì "vô hiệu"; (b) người dùng bấm Thử lại, Làm mới mà vẫn lỗi thì có toast lỗi, còn lỗi tải lần đầu thì không. Tìm kiếm chỉ toast khi người dùng gửi (Enter), kèm số kết quả.

## 4. Quy ước ID

- **ID item (Sheet 5)** duy nhất trong màn, đặt theo `<slugChinhTacCamel>.<khối>.<trường>`; cột trong danh
  sách thêm đoạn `col`, ví dụ `adminAiConfig.prompt.col.version`. Dùng **slug chính tắc** chứ không phải tên
  rút gọn: `admin_overview` thành `adminOverview`, `instructor_overview` thành `instructorOverview` — hai màn
  này có cùng tên rút gọn `overview`, rút gọn sẽ đụng nhau. ID này vừa là ID kiểm toán, vừa là `data-testid`
  khi dựng Next.js, mà `data-testid` có phạm vi toàn ứng dụng nên phải duy nhất xuyên màn.
- **NO item** đánh từ 1 cho **từng khu vực**, không đánh liên tục xuyên khu vực. Sheet 6 dùng lại đúng NO,
  đúng tên item và đúng thứ tự của Sheet 5.
- **ID sự kiện (Sheet 8)** dạng `EVT-<NO>`, đánh liên tục trong toàn màn kể cả popup. Sheet 5 trỏ tới qua thẻ
  `[EVT liên quan]`, Sheet 9 trỏ tới qua cột `EVT gọi`. Tham chiếu một chiều, không tạo vòng.
- **Mã thông báo (Sheet 9)** chỉ ghi khi đã có trong bộ mã thông báo của dự án. Chưa có thì ghi
  `Chưa có mã thông báo` và viết nội dung ở cột ghi chú theo dạng `Nội dung "..."`.
- **Nhãn `[Nội bộ]`** đánh dấu phần phục vụ sinh mã và tự kiểm toán, không phải yêu cầu của khách: mục 4.5,
  mục 7.3, và các ghi chú quy ước đặt trong file.

## 5. Ranh giới với tài liệu khác

Không lặp nội dung giữa các trục. Sai ranh giới là trôi lệch tài liệu, không phải chi tiết trình bày.

| Nội dung | Nơi duy nhất được viết |
| :--- | :--- |
| Hành vi ở mức yêu cầu, Given-When-Then | `01-rd/screens/<slug>.md` |
| Bảng, cột, khoá, index, migration | `02-bd/database/<module>.md` |
| Đường dẫn endpoint, request, response, mã lỗi | `03-dd/api/<module>.md`. Sheet 7.3 **chỉ** ghi tên nghiệp vụ và BC sở hữu |
| Tiêu chí nghiệm thu `AC-nn` | `04-tdd/<slug>.md`. Sheet 9 ghi **điều kiện kiểm và thông báo**, không ghi tiêu chí nghiệm thu |
| Màu sắc, khoảng cách, typography | Chưa có nguồn nào. `09-layoutBase/**` là bằng chứng bố cục chỉ-đọc, **không phải** design system |

## 6. Hai luật bắt lỗi

Hai luật này là lý do mẫu 9 sheet đáng dùng. Bỏ chúng thì khung chỉ còn là bảng biểu.

1. **Mọi trường hiển thị phải nói rõ nguồn** — cột DB, file cấu hình, hay nhãn tĩnh i18n. Không chỉ ra được
   nguồn thì đó là một phát hiện: viết thành câu hỏi mở, không mặc định là "chắc có cột". Trường chỉ-đọc mà
   chưa ai yêu cầu sửa thì lấy nguồn rẻ nhất (nhãn tĩnh hoặc `application.yml`), không thêm cột kèm migration.
2. **Mọi nút và liên kết phải có một dòng hành vi** ở Sheet 8 — bấm vào thì gì đổi, có rời màn không, còn
   thay đổi chưa lưu thì xử lý sao.

## 7. Checklist kiểm toán

| ID | Nội dung kiểm | Tiêu chí đạt | Máy kiểm được |
| :--- | :--- | :--- | :-: |
| AUD-01 | Giá trị dùng chung | Mọi sheet dùng cùng tên hệ thống và cùng số phiên bản ghi ở Sheet 1 | Có |
| AUD-02 | ID item | ID item ở Sheet 5 không trùng; NO trong mỗi khu vực liên tục từ 1 | Có |
| AUD-03 | ID sự kiện | `EVT-n` liên tục, không trùng; mọi `EVT` được tham chiếu đều tồn tại ở Sheet 8 | Có |
| AUD-04 | Thông báo | Sheet 9 chỉ ghi mã thông báo có thật; chưa có mã thì ghi `Chưa có mã thông báo` kèm nội dung | Không |
| AUD-04b | Kênh thông báo | Kết quả thao tác và lỗi nhập liệu ghi là toast; không còn cụm "báo lỗi tại ô", "ngay dưới trường", "banner" cho kết quả thao tác (ngoại lệ nêu ở mục 3) | Không |
| AUD-05 | Item và điều khiển | Sheet 5 và Sheet 6 khớp NO, tên item, thứ tự; mọi dòng `Điều kiện` đều có điều kiện | Có |
| AUD-06 | Số bảng dữ liệu | Số bảng ở mục 4.3 khớp số dòng ở mục 7.2 | Có |
| AUD-07 | CRUD | Cột `CRUD` và cột ghi chú ở mục 7.2 mô tả cùng một tập thao tác | Không |
| AUD-08 | Endpoint | Mục 7.3 chỉ có tên nghiệp vụ và BC sở hữu, không có đường dẫn hay request/response | Không |
| AUD-09 | Căn cứ | Mọi `[Nguồn:]` trỏ file và số dòng có thật; suy luận đều đánh dấu `[Suy luận]` | Có |
| AUD-10 | Nguồn trường | Mọi item ở Sheet 5 có `[Nguồn giá trị]` hoặc `[Công thức]`, hoặc là một câu hỏi mở | Có |

Sáu mục `Máy kiểm được = Có` nên chạy bằng script khi có đủ vài file để đáng viết; bốn mục còn lại là việc
người đọc.

## 8. Mã định danh màn hình

Mỗi màn có một mã cố định, dùng làm **tiền tố tên file** ở cả ba trục `01-rd/screens/`, `02-bd/screens/`,
`03-dd/screens/`, và ghi ở Sheet 1 cùng tiêu đề H1.

Dạng mã: `<KHU VỰC 3 chữ><nhóm 2 số><thứ tự 2 số>`. Tên file: `<MÃ>_<tên rút gọn>.md`.

**Tên rút gọn = slug bỏ tiền tố khu vực**, vì mã đã mang thông tin đó: `admin_ai_config` thành `ai_config`,
`instructor_grading` thành `grading`. Chỉ bỏ đúng tiền tố `admin_` và `instructor_`; phần còn lại giữ
nguyên, không tự viết tắt thêm (`permission_matrix` không rút thành `perm`).

**Slug chính tắc không đổi** — vẫn là `admin_ai_config`, ghi ở Sheet 1 dòng "Tên vật lý (slug)" và ở
`01-rd/overview/system_survey.md` mục 7.3. Tên file chỉ là cách đặt tên, không phải định danh. Slug vẫn là
thứ phải giống nhau xuyên `01-rd` → `02-bd` → `03-dd` theo luật chống trôi lệch của `CLAUDE.md`.

| Khu vực | Tiền tố | Actor |
| :--- | :--- | :--- |
| Người học | `USR` | A1 |
| Giảng viên | `INS` | A2 |
| Quản trị | `ADM` | A3 |
| Dùng chung nhiều vai trò | `SHR` | A1 / A2 / A3 |

### Bảng mã (33 màn)

| Mã | Tên file | Slug chính tắc | Tên màn | Nhóm |
| :--- | :--- | :--- | :--- | :--- |
| `USR0101` | `USR0101_problem_list.md` | `problem_list` | Danh sách bài tập | Bài tập |
| `USR0102` | `USR0102_problem_detail.md` | `problem_detail` | Chi tiết bài tập | Bài tập |
| `USR0103` | `USR0103_saved_problems.md` | `saved_problems` | Bài đã lưu | Bài tập |
| `USR0201` | `USR0201_submission_result.md` | `submission_result` | Kết quả nộp bài | Nộp bài |
| `USR0202` | `USR0202_my_submissions.md` | `my_submissions` | Lịch sử nộp bài | Nộp bài |
| `USR0301` | `USR0301_solution_review.md` | `solution_review` | Phân tích bài giải | AI |
| `USR0302` | `USR0302_mock_interview.md` | `mock_interview` | Phỏng vấn giả lập | AI |
| `USR0401` | `USR0401_interview_bank_list.md` | `interview_bank_list` | Ngân hàng câu hỏi | Câu hỏi phỏng vấn |
| `USR0402` | `USR0402_interview_question_detail.md` | `interview_question_detail` | Chi tiết câu hỏi | Câu hỏi phỏng vấn |
| `USR0501` | `USR0501_my_progress.md` | `my_progress` | Tiến độ của tôi — **đã gộp vào `USR0601`** 2026-09-27, RD giữ làm hồ sơ gốc, không viết BD/DD (`DEC-2026-0927-student-area-merge-and-shared-shell`) | Cá nhân |
| `USR0502` | `USR0502_profile.md` | `profile` | Hồ sơ | Cá nhân |
| `USR0503` | `USR0503_settings.md` | `settings` | Thiết lập | Cá nhân |
| `USR0601` | `USR0601_dashboard.md` | `dashboard` | Tổng quan người học | Tổng quan |
| `INS0101` | `INS0101_overview.md` | `instructor_overview` | Tổng quan giảng viên | Tổng quan |
| `INS0201` | `INS0201_class_management.md` | `class_management` | Quản lý lớp | Lớp học |
| `INS0202` | `INS0202_class_assignments.md` | `class_assignments` | Giao bài cho lớp | Lớp học |
| `INS0203` | `INS0203_class_progress.md` | `class_progress` | Tiến độ lớp | Lớp học |
| `INS0204` | `INS0204_class_student_detail.md` | `class_student_detail` | Chi tiết học viên | Lớp học |
| `INS0301` | `INS0301_grading.md` | `instructor_grading` | Chấm tay | Chấm bài |
| `ADM0101` | `ADM0101_overview.md` | `admin_overview` | Tổng quan quản trị | Tổng quan |
| `ADM0201` | `ADM0201_user_management.md` | `admin_user_management` | Quản lý người dùng | Người dùng và quyền |
| `ADM0202` | `ADM0202_permission_matrix.md` | `admin_permission_matrix` | Ma trận phân quyền | Người dùng và quyền |
| `ADM0301` | `ADM0301_ai_config.md` | `admin_ai_config` | Cấu hình trợ lý AI | AI |
| `ADM0302` | `ADM0302_ai_usage.md` | `admin_ai_usage` | Tiêu thụ token AI | AI |
| `ADM0401` | `ADM0401_queue_monitor.md` | `admin_queue_monitor` | Giám sát hàng đợi | Vận hành |
| `ADM0402` | `ADM0402_rejudge.md` | `admin_rejudge` | Chấm lại — **đã loại khỏi phạm vi**, chỉ còn RD, không viết BD/DD (`DEC-2026-0828-remove-rejudge-scope`) | Vận hành |
| `ADM0403` | `ADM0403_system_log.md` | `admin_system_log` | Nhật ký hệ thống | Vận hành |
| `ADM0501` | `ADM0501_language_config.md` | `admin_language_config` | Cấu hình ngôn ngữ | Hệ thống |
| `SHR0101` | `SHR0101_auth.md` | `auth` | Đăng nhập và đăng ký | Xác thực |
| `SHR0201` | `SHR0201_problem_management.md` | `problem_management` | Quản lý bài tập | Nội dung bài tập |
| `SHR0202` | `SHR0202_problem_authoring.md` | `problem_authoring` | Biên soạn bài tập | Nội dung bài tập |
| `SHR0203` | `SHR0203_problem_info.md` | `problem_info` | Chi tiết bài tập | Nội dung bài tập |
| `SHR0301` | `SHR0301_interview_question_management.md` | `interview_question_management` | Quản lý câu hỏi phỏng vấn | Nội dung câu hỏi |
| `SHR0302` | `SHR0302_interview_question_authoring.md` | `interview_question_authoring` | Biên soạn câu hỏi phỏng vấn | Nội dung câu hỏi |
| `SHR0303` | `SHR0303_interview_question_info.md` | `interview_question_info` | Chi tiết câu hỏi phỏng vấn | Nội dung câu hỏi |

Quy tắc bổ sung:
- **Mã đã cấp thì không đổi, không tái sử dụng.** Màn bị cắt khỏi phạm vi thì mã đó bỏ trống vĩnh viễn.
  Ngoại lệ đã dùng một lần, ghi lại để không ai tưởng là tiền lệ: màn `dashboard` ban đầu được đặt nhầm là
  `USR0100` khi viết RD ngày 2026-09-27 — sai dạng mã (`<nhóm 2 số><thứ tự 2 số>` không có thứ tự `00`, và
  nhóm `01` của khu Người học đã là "Bài tập"). Sửa thành `USR0601` ngay trong ngày, trước khi có BD/DD nào
  trỏ tới. `USR0100` chưa bao giờ hợp lệ nên không phải "mã đã cấp"; nó bỏ trống vĩnh viễn như mọi mã khác.
- Màn mới lấy số thứ tự kế tiếp trong nhóm của nó. Nhóm mới lấy số nhóm kế tiếp trong khu vực.
- `02-bd/screens/admin/_shell.md` **không phải màn hình** mà là khung chung dùng lại, nên không cấp mã và
  giữ nguyên tên.
- Slug lấy từ `01-rd/overview/system_survey.md` mục 7.3, không tự đặt mới ở BD.

## 9. Cái mẫu gốc có mà AlgoPrep bỏ

Ghi lại để khỏi có người "bổ sung cho đủ" về sau.

- **Sheet 1 mục システム名 / プロジェクト名 lặp trên mọi sheet** — bỏ, chỉ ghi một lần ở Sheet 1.
- **Các cột 必須(検) 必須(登) 必須(訂) 必須(削)** ở Sheet 5 — gộp thành một cột `Bắt buộc`. Các màn của
  AlgoPrep không chạy mô hình bốn chế độ tìm kiếm/đăng ký/sửa/xoá như hệ thống gốc.
- **Cột `グリッド`** — bỏ, đã thể hiện qua `Loại UI = List` và `ListColumn`.
- **Quy ước màu ô Excel và mục chụp ảnh workbook** — bỏ. AlgoPrep không giao nộp workbook; ảnh minh hoạ chụp
  bằng Playwright trên ứng dụng Next.js thật, đặt ở `08-diagram/02-bd/screens/**`.
