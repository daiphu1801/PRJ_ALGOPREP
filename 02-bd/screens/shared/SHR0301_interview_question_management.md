# Tài liệu thiết kế cơ bản (BD) — Quản lý ngân hàng câu hỏi phỏng vấn (`SHR0301`)

## Phương châm tài liệu [Nội bộ — không cần khách duyệt]

- Viết theo **mẫu 9 sheet**. Bộ thẻ đóng, quy ước ID, ký hiệu chuẩn, ranh giới với tài liệu khác và
  checklist kiểm toán nằm ở `02-bd/_rules/bd-template-9sheet.md` — file này không chép lại.
- Phần gắn nhãn `[Nội bộ]` phục vụ sinh mã và tự kiểm toán, không phải yêu cầu của khách hàng: mục 4.5,
  mục 7.3 và các ghi chú quy ước.
- Mã màn `SHR0301` theo bảng mã ở `02-bd/_rules/bd-template-9sheet.md` mục 8; tên file mang tiền tố mã.
  Slug chính tắc `interview_question_management` không đổi.
- Màn dùng chung A2 (giảng viên) và A3 (quản trị viên), mount ở cả `/instructor/interview-questions` và
  `/admin/interview-questions`, cùng một view/BD/DD, phạm vi dữ liệu do quyền
  `INTERVIEW_BANK_MANAGEMENT` quyết định — **không** chia theo lớp phụ trách
  [Nguồn: 01-rd/screens/shared/SHR0301_interview_question_management.md:5, 17-22, 52-55;
  `DEC-2026-0825-shared-content-authoring-screens`].
- Bounded Context sở hữu toàn bộ dữ liệu và logic của màn này: `interview-bank` (F6) — một màn, một
  module. Không có màn con; chỉ có một popup xác nhận xoá.
- File thay thế bản BD cũ (7 mục văn xuôi) tại cùng đường dẫn cũ
  `02-bd/screens/shared/SHR0301_interview_question_management.md`, đã xoá sau khi chuyển sang mẫu 9 sheet.

> Đọc cùng `01-rd/screens/shared/SHR0301_interview_question_management.md` (hành vi ở mức yêu cầu, không lặp lại
> ở đây) và ba file BD module: `02-bd/architecture/interview-bank.md`,
> `02-bd/database/interview-bank.md`, `02-bd/security/interview-bank.md`. Khung điều hướng và chân trang
> khu Admin dùng lại `02-bd/screens/admin/_shell.md`; khu Giảng viên không có prototype HTML riêng; từ 2026-10-03 bản dựng Next.js đã mount
> cùng view tại `/instructor/interview-questions` (xem mục 4.5), khung điều hướng Giảng viên vẫn **[Đợi nextjs]**.
>
> **Phạm vi đã chốt trước khi viết BD** — kế thừa nguyên văn từ RD mục 2, 3, 5, không mở lại:
> - **Không có** khái niệm `question_sets`/"bộ câu hỏi theo lớp" — đã loại khỏi phạm vi
>   (`DEC-2026-0828-remove-per-class-interview-set`) [Nguồn: 02-bd/database/interview-bank.md:179-185].
> - **"Nhập CSV" ngoài phạm vi bản đầu** — không cấp mã nghiệp vụ
>   [Nguồn: 01-rd/screens/shared/SHR0301_interview_question_management.md:79-83]. Nút trên màn **vẫn hiển thị và
>   kích hoạt** (owner chốt 2026-10-01); bấm nút chưa có hành vi, là stub của bản dựng.
> - **A2 và A3 cùng phạm vi dữ liệu**, không lọc theo `created_by`
>   [Nguồn: 02-bd/security/interview-bank.md:34-43].
> - **Xoá là xoá mềm** (`status = RETIRED`), không cascade xoá `answer_rubrics`/`user_answers` đã dùng
>   câu hỏi đó [Nguồn: 02-bd/database/interview-bank.md:65, 94-96].

---

## Sheet 1. Bìa

| Mục | Giá trị |
| :--- | :--- |
| Hệ thống | AlgoPrep |
| Phân hệ | `interview-bank` (F6) |
| Công đoạn | BD — Thiết kế cơ bản |
| Phân loại | Màn hình |
| Tên màn | Quản lý ngân hàng câu hỏi phỏng vấn |
| Mã màn hình | `SHR0301` |
| Tên vật lý (slug) | `interview_question_management` |
| Trục tài liệu | Màn hình (`02-bd/screens/`) |
| Actor | A2 (`INSTRUCTOR`) và A3 (`ADMIN`) — dùng chung |
| Phiên bản | V1.14 |
| Người tạo | Nhóm phát triển AlgoPrep |
| Ngày tạo | 2026/09/20 |
| Người cập nhật | Nhóm phát triển AlgoPrep |
| Ngày cập nhật | 2026/10/05 |

---

## Sheet 2. Lịch sử sửa đổi

| Ver | Sheet bị sửa | Nội dung sửa | Ngày | Người sửa |
| :--- | :--- | :--- | :--- | :--- |
| V1.0 | Toàn bộ | Viết mới theo mẫu 9 sheet, thay thế bản BD cũ dạng 7 mục văn xuôi (`Layout regions` / `Component inventory` / `Screen states` / `APIs consumed` / `Navigation` / `Access rights` / `Câu hỏi mở`). Phát hiện hai khoảng trống nguồn dữ liệu mới so với bản cũ: (1) không có cột định danh dạng `IQ-014` trong schema, chỉ có `id` UUID; (2) `feedback_result_json` là phản hồi định tính, không có trường điểm số 1-5 rời rạc để tính "điểm trung bình" — mở Q1, Q2 | 2026/09/20 | Nhóm phát triển AlgoPrep |
| V1.1 | Sheet 3, 4, 5, 6, 7, 8, Câu hỏi mở | Đồng bộ với bản dựng UI ngày 2026-10-01: (1) màn là **danh sách dạng bảng** (`DataTable` trong một `Card`, 8 dòng/trang) thay cho lưới thẻ; cột Mã, Câu hỏi, Chủ đề, Mức độ, Đào sâu (số lượng), Tiêu chí (số lượng), Lượt dùng, Điểm TB, Thao tác; (2) **bỏ dải bốn thẻ chỉ số** (Khu vực B cũ, `GetInterviewQuestionBankStats`, `InterviewQuestionManagementStatsDto`) — chỉ còn một trang tổng quan Admin/Giảng viên, trang danh sách chỉ có bộ lọc và danh sách; Khu vực C, D, E cũ đổi thành B, C, D; (3) thao tác trên dòng là ba nút chỉ có biểu tượng (Sửa, Nhân bản, Xoá) qua `IconAction`, tooltip nhỏ bên dưới khi rê chuột hoặc focus; (4) khối câu hỏi đào sâu và thanh trọng số tiêu chí không còn hiển thị trên danh sách, chỉ còn số lượng — nội dung xem ở màn `SHR0302`; (5) Q1 (mốc biến động) đóng vì thẻ chỉ số đã bỏ, Q2 chỉ còn áp dụng cho cột Điểm TB | 2026/10/01 | Nhóm phát triển AlgoPrep |
| V1.2 | Sheet 4, 5, 6, 7, 8, Câu hỏi mở | Owner chốt ngày 2026-10-01: (1) nút "Nhập CSV" **giữ kích hoạt**, không vô hiệu hoá kèm tooltip "Sắp ra mắt" như đề xuất của V1.1 — bỏ đề xuất đó; bấm nút hiện **chưa làm gì** (stub của bản dựng, ghi vào `06-plan/PROTOTYPE_DEBT.md` mục 16); Q3 đóng; (2) `InterviewQuestionListItemDto` dùng hai số đếm `followUpCount` và `rubricCount` thay cho danh sách lồng — bỏ dấu `[Suy luận]`, ghi "owner xác nhận 2026-10-01" | 2026/10/01 | AI |
| V1.3 | Sheet 3, 4, 5, 6, 7, 8, 9, Câu hỏi mở | Đã chốt 2026-10-01 (owner uỷ quyền cân nhắc), xem `DEC-2026-1001-admin-configurable-settings`: **chủ đề câu hỏi là dữ liệu do ADMIN quản lý**, không còn 5 chủ đề cố định. (1) Thêm nút "Quản lý chủ đề" ở thanh tiêu đề (chỉ ADMIN thấy) mở popup quản lý chủ đề: thêm, đổi tên, sắp xếp lại, xoá; xoá bị từ chối khi còn câu hỏi tham chiếu, popup hiện số câu hỏi; (2) tab chủ đề của bộ lọc đọc từ dữ liệu `ListQuestionTopics`, số tab không cố định; (3) thêm bốn endpoint chỉ ADMIN `CreateQuestionTopic`, `RenameQuestionTopic`, `ReorderQuestionTopics`, `DeleteQuestionTopic`; `ListQuestionTopics` mở cho mọi người dùng đã xác thực; (4) Sheet 9 thêm các kiểm tra; (5) thay thế tiểu quyết định 4 (Q6, 5 chủ đề cố định) của `DEC-2026-0830-interview-bank-crud`. Q7 mới | 2026/10/01 | AI |
| V1.4 | Sheet 4, 5, 6, 7, 8, 9, Câu hỏi mở | Đã chốt 2026-10-01 (owner uỷ quyền), xem `DEC-2026-1001-admin-configurable-settings`: (1) khung trả lời chuẩn STAR không còn gắn mã chủ đề `BEHAVIORAL` mà gắn cờ `question_topics.uses_star_framework`; popup quản lý chủ đề thêm công tắc "Dùng khung STAR" trên mỗi dòng (Khu vực popup NO 9, EVT-20, trường `usesStarFramework` trong `QuestionTopicOptionDto` và `QuestionTopicManageResultDto`); (2) `RenameQuestionTopic` đổi thành `UpdateQuestionTopic` (đổi tên và bật/tắt cờ STAR); (3) Q7 đóng: dùng lại `INTERVIEW_BANK_MANAGEMENT` cộng kiểm vai trò `ADMIN`, không Function mới | 2026/10/01 | AI |
| V1.5 | Sheet 3, 4, 5, 8 | Theo `DEC-2026-1002-split-detail-and-edit-pages` (khu Admin): (1) bấm nội dung câu hỏi ở cột "Câu hỏi" chuyển sang màn mới `interview_question_info` (`SHR0303`, chỉ đọc) tại `/admin/interview-questions/[mã]`, không còn vào form soạn; (2) biểu tượng "Sửa" sang `interview_question_authoring` tại `/admin/interview-questions/[mã]/edit`; (3) "Câu hỏi mới" sang `/admin/interview-questions/new`. Tách chuyển màn "sửa" cũ thành hai: xem chi tiết và sửa. Khu Giảng viên chưa tách, giữ nguyên | 2026/10/02 | AI |
| V1.6 | Sheet 3, 4, 5, 8 | Đồng bộ với bản dựng ngày 2026-10-03: (1) khu Giảng viên đã tách cùng cấu trúc khu Admin (`DEC-2026-1002-split-detail-and-edit-pages`): `/instructor/interview-questions/[mã]` chỉ đọc, `/edit` và `/new` là màn soạn — bỏ ý "khu Giảng viên chưa tách" của V1.5; (2) view nhận prop bắt buộc `basePath`, nên liên kết ở cột "Câu hỏi", biểu tượng "Sửa" và nút "Câu hỏi mới" dựng từ `basePath` thay vì cố định `/admin`; (3) làm mới toàn bộ dẫn chiếu dòng tới bản dựng, `02-bd/database/interview-bank.md` (bảng `question_topics` được thêm lên đầu nên các dòng cũ lệch) và RD. Không đổi nội dung trường | 2026/10/03 | AI |
| V1.7 | Sheet 8, 9 | Đồng bộ `DEC-2026-1003-toast-feedback-channel` (2026-10-03): kết quả tìm kiếm bằng Enter, nhân bản, xoá, nhập CSV (bản dựng), bật/tắt STAR, thêm/đổi tên/xoá chủ đề và lỗi nhập ghi là toast; ô sai đổi viền đỏ; `[Tiêu điểm]` Sheet 9 ghi "viền ô + toast". Giữ nguyên lỗi tải danh sách ở vùng danh sách và thông báo rỗng | 2026/10/03 | Nhóm phát triển AlgoPrep |
| V1.8 | Sheet 3, 4, 5, 6, 7, 8, 9, Câu hỏi mở | Đã chốt 2026-10-03 (owner), xem `DEC-2026-1001-admin-configurable-settings` mục (6) "Round 5": **độ khó câu hỏi là dữ liệu do ADMIN quản lý** như chủ đề, không còn 3 mức cố định Dễ/Trung bình/Khó (ENUM `difficulty` đổi thành `level_id` trỏ `question_levels`). (1) Thêm nút "Quản lý độ khó" cạnh "Quản lý chủ đề" (chỉ ADMIN) mở popup mới: thêm, đổi tên, xoá; không có công tắc STAR; xoá bị từ chối khi còn câu hỏi dùng mức hoặc khi là mức cuối cùng; (2) tab cấp độ của bộ lọc và cột "Mức độ" đọc từ `question_levels`; badge màu: 3 mức seed giữ màu, mức mới màu trung tính (không có cột màu); (3) DTO `difficulty` đổi thành `levelCode`/`levelDisplayName`, thêm 5 endpoint `ListQuestionLevels`, `CreateQuestionLevel`, `UpdateQuestionLevel`, `ReorderQuestionLevels`, `DeleteQuestionLevel`; (4) EVT-22 đến EVT-27, Sheet 9 thêm NO 14 đến 18, quy tắc nhân bản giữ `level_id`; (5) ghi hiện trạng bản dựng (kho in-memory, chưa có sắp xếp lại); (6) mở Q8 (cách sinh `code` mức mới), đóng Q9 (màu không cấu hình được) | 2026/10/03 | AI |
| V1.9 | Sheet 3, 4, 5, 6, 7, 8, 9, Câu hỏi mở | Đồng bộ bản dựng cuối ngày 2026-10-03 và làm mới dẫn chiếu. (1) Dẫn chiếu: `02-bd/database/interview-bank.md` (mục 1.1a `question_levels` chèn ở dòng 30 làm các dòng sau lệch: `status` 65, `follow_up_questions` 64, không cascade 94-96, `answer_rubrics` 78-96, `user_answers` 110-132, `question_sets` 179-185), RD (Nhật ký F1-14 `:121-125`, thiếu rubric `:175`, không có bộ câu hỏi `:126-130`), toàn bộ dòng của `interview-question-management-view.tsx` (view dài thêm nên các dòng từ 48 trở đi đều dời), `managed-list-dialog.tsx`, `icon-action.tsx`, `topic-manager-dialog.tsx`, `level-manager-dialog.tsx`; prototype `09-layoutBase` đối chiếu đúng, chỉ ghi chú placeholder ô tìm kiếm là "hoặc thẻ". (2) Hiện trạng bản dựng: khoá độ khó seed đổi thành `EASY`/`MEDIUM`/`HARD`, mức mới có khoá slug viết hoa (`RAT_KHO`, hậu tố `_2` khi trùng, vẫn trong bộ nhớ); **đã dựng sắp xếp lại** (nút Lên/Xuống) cho độ khó câu hỏi, chủ đề câu hỏi và chủ đề bài toán; bất biến "tối thiểu một mức" nay cũng do kho thực thi (`minItems: 1`), không chỉ popup; cập nhật bảng hiện trạng Sheet 4.5, Sheet 5 NO 16, EVT-25, Sheet 9 NO 17; Q8 chuyển sang "đã dựng trong bản dựng, DD xác nhận lại". Không đổi số NO/EVT/Q | 2026/10/03 | AI |
| V1.10 | Sheet 4, 5, 6, 8, Câu hỏi mở | Đồng bộ các chỉnh sửa prototype ngày 2026-10-05 (owner chọn "phương án A cho F6" trong cuộc trò chuyện; chi tiết giao diện ghi **chưa duyệt**): (1) **danh sách khớp danh sách bài tập `SHR0201`**: mọi cột sắp xếp được (bấm tiêu đề, bấm lại đảo chiều, `aria-sort`), **khi mới vào màn chưa sắp theo cột nào** (giữ thứ tự tự nhiên); thêm Sheet 5 Khu vực C NO 13, Sheet 6 Khu vực C NO 13, EVT-28; (2) **chọn số dòng mỗi trang 8 / 20 / 50**, nhớ ở localStorage khoá `algoprep-interview-questions-page-size` (cùng cơ chế `usePersistedPageSize` với danh sách bài tập, thay cho 8 dòng cố định); thêm Sheet 5 Khu vực D NO 4, Sheet 6 Khu vực D NO 4, EVT-29, sửa các câu "8 dòng một trang"; (3) ~~bộ lọc chủ đề và bộ lọc cấp độ thu thành ô chọn có nhãn khi quá 6 lựa chọn (prop `maxInline`)~~ **đã gỡ và thay ở V1.11**; (4) **hành động chưa có API** ("Nhân bản" ở dòng, "Nhập CSV" ở tiêu đề) nay hiện toast thông tin "{hành động}: chưa nối API trong bản mẫu, chưa có thay đổi nào" thay cho toast thành công (sửa EVT-8, EVT-10); (5) **kẹp trang sau khi xoá** (không còn trang rỗng), thuộc tính `title` ở liên kết nội dung câu hỏi, tiêu đề cột `DataTable` màu muted (xuyên màn, ghi ở nợ prototype, không đổi hành vi BD); (6) làm mới toàn bộ số dòng trích dẫn vào `interview-question-management-view.tsx` (mã đã dời); thêm Câu hỏi mở Q10 | 2026/10/05 | Nhóm phát triển AlgoPrep |
| V1.11 | Sheet 4, 5, 6, Câu hỏi mở | Bộ lọc của danh sách đổi giao diện (2026-10-05, owner chỉ đạo, chi tiết **chưa duyệt**): (1) **gỡ** ô chọn gốc và ngưỡng 6 lựa chọn của V1.10 (prop `maxInline` đã xoá); (2) bộ lọc chủ đề và bộ lọc cấp độ nay là thành phần dùng chung `FilterMenu` (`shared/ui/data/filter-menu.tsx`): nút hiện "{nhãn}: {lựa chọn hiện tại}" kèm mũi tên, bấm mở pop-up kính nhỏ dạng danh sách lựa chọn, lựa chọn hiện tại có dấu tích, không giới hạn số lựa chọn; bàn phím: mũi tên, Home, End, Enter hoặc Space, Escape trả tiêu điểm về nút, Tab hoặc bấm ra ngoài thì đóng (Sheet 4.1, 4.4, 4.5, Sheet 5 và 6 Khu vực B NO 2 và NO 3); (3) cửa sổ "Quản lý chủ đề" và "Quản lý độ khó": danh sách cuộn từ mục thứ 6 (hiện 5 dòng) với thanh cuộn mỏng trong suốt (Sheet 4.4, 4.5, Sheet 6 Popup); (4) làm mới số dòng trích dẫn vào `interview-question-management-view.tsx` và `managed-list-dialog.tsx`; (5) Câu hỏi mở Q10 viết lại: bỏ câu ngưỡng 6, thêm câu hỏi về việc thay các dải nút còn lại | 2026/10/05 | Nhóm phát triển AlgoPrep |
| V1.12 | Sheet 3, 4, 5, 6, 8, 9, Câu hỏi mở | Danh sách có **chọn nhiều dòng và thanh thao tác theo lô** (2026-10-05; owner yêu cầu cột chọn "cho đồng bộ" với danh sách bài tập `SHR0201`; **hai thao tác theo lô do bên phát triển chọn từ các thao tác theo dòng, chưa duyệt**, RD F6-13 chưa định nghĩa chúng ở dạng theo lô): (1) thêm cột ô chọn gốc (thuộc tính `selection` của `DataTable` dùng chung), ô chọn ở tiêu đề chọn các dòng đang hiển thị (Sheet 5 Khu vực C NO 14 và NO 15, Sheet 6 tương ứng); (2) thêm Khu vực E "Thanh thao tác theo lô" dùng thành phần dùng chung `BulkActionBar` (pop-up kính nổi ở giữa mép trên màn hình khi có dòng được chọn, trượt vào 180 ms, `role="status"` luôn gắn sẵn đọc số lượng) với hai thao tác "Nhân bản" (bản dựng chưa có API: toast thông tin "chưa nối API", như nút theo dòng) và "Xoá" (mở popup xác nhận) (Sheet 5 và 6 Khu vực E, Sheet 4.4, 4.5); (3) thêm Popup NO 17 "Xác nhận xoá theo lô" (tiêu đề "Xoá {số} câu hỏi đã chọn?", cùng ghi chú giữ lịch sử phiên cũ như xoá một câu, thêm dòng cảnh báo khi tập chọn có câu đang bị bộ lọc hiện tại ẩn) (Sheet 3 mục chuyển màn mới, Sheet 5 và 6 Popup NO 17); (4) thêm EVT-30 đến EVT-35 (chọn một dòng, chọn tất cả dòng đang hiển thị, nhân bản theo lô, mở popup xoá theo lô, xác nhận xoá theo lô, huỷ) và Sheet 9 NO 19 (cảnh báo bài bị lọc ẩn, mức Cảnh báo); xoá mềm giữ nguyên nghĩa F6-13 (`status = RETIRED`, giữ lịch sử phiên cũ); (5) xoá một dòng cũng bỏ dòng đó khỏi tập đang chọn (Sheet 8 EVT-12); (6) làm mới số dòng trích dẫn vào `interview-question-management-view.tsx` (mã dời) và `messages/vi.json`; (7) Câu hỏi mở Q10 mục (d) viết lại | 2026/10/05 | Nhóm phát triển AlgoPrep |
| V1.13 | Sheet 4, 5, 6 | Bộ chọn số dòng mỗi trang đổi giao diện (2026-10-05, chi tiết **chưa duyệt**): không còn là ô chọn gốc mà là `FilterMenu` chế độ `compact` với `placement="top"` trong `Pagination` dùng chung (cùng `SHR0201` V1.16), pop-up kính mở lên phía trên; lựa chọn 8 / 20 / 50, nhớ localStorage, không đổi hành vi. Sửa Sheet 4.4 hàng Phân trang, Sheet 5 và 6 Khu vực D NO 4 | 2026/10/05 | Nhóm phát triển AlgoPrep |
| V1.14 | Sheet 4 | Thanh công cụ phía trên danh sách nay là thành phần dùng chung `FilterBar` (`shared/ui/data/filter-bar.tsx`, chưa duyệt, **không đổi hành vi**): ô tìm kiếm, hai `FilterMenu` (chủ đề, cấp độ) và nhãn "n / tổng" ở mép phải (Sheet 4.4 hàng Thanh lọc, Sheet 4.5). Làm mới số dòng trích dẫn vào `interview-question-management-view.tsx` | 2026/10/05 | Nhóm phát triển AlgoPrep |

---

## Sheet 3. Sơ đồ chuyển màn

> [Nội bộ] Quy ước vẽ: bố trí từ trái sang phải. Màn gọi tới tô tím, màn hiện tại tô xanh nhạt, popup tô
> vàng. Màu và `[Nguồn:]` dùng để truy vết, không phải yêu cầu của khách hàng.

### 3.1 Danh sách chuyển màn

#### Khung điều hướng (Admin hoặc Giảng viên) → Quản lý ngân hàng câu hỏi

[Điều kiện mở] Chọn mục con "Câu hỏi phỏng vấn" trong nhóm "Nội dung" ở thanh điều hướng bên trái.

[Chế độ mở] Chế độ danh sách, không lọc sẵn — tab chủ đề và tab cấp độ đều ở "Tất cả".

[Thông tin truyền] Không có.

[Giá trị trả về] Không có.

[Khi thành công] Tải trang đầu của danh sách câu hỏi, danh mục chủ đề và danh mục độ khó.

[Khi huỷ] Không có.

#### Quản lý ngân hàng câu hỏi → Màn Biên soạn câu hỏi (tạo mới)

[Điều kiện mở] Bấm nút "Câu hỏi mới" ở thanh tiêu đề.

[Chế độ mở] Chế độ tạo mới, chưa có dữ liệu điền sẵn.

[Thông tin truyền] Không có.

[Giá trị trả về] Không có — màn đích tự điều hướng ngược lại danh sách sau khi lưu hoặc huỷ.

[Khi thành công] Mở màn `interview_question_authoring` (`SHR0302`) ở chế độ tạo mới, tại `{basePath}/new` — khu Admin `/admin/interview-questions/new`, khu Giảng viên `/instructor/interview-questions/new` [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:59-64, 353-355].

[Khi huỷ] Không có.

#### Quản lý ngân hàng câu hỏi → Chi tiết câu hỏi (chỉ đọc)

[Điều kiện mở] Bấm vào nội dung câu hỏi ở cột "Câu hỏi" của một dòng.

[Chế độ mở] Chỉ đọc, mang theo mã câu hỏi đang chọn.

[Thông tin truyền] Mã câu hỏi của dòng.

[Giá trị trả về] Không có.

[Khi thành công] Mở màn `interview_question_info` (`SHR0303`) tại `{basePath}/[mã]` — khu Admin `/admin/interview-questions/[mã]`, khu Giảng viên `/instructor/interview-questions/[mã]` [Nguồn: 01-rd/screens/shared/SHR0301_interview_question_management.md:159-162; 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:59-64, 198-211].

[Khi huỷ] Không có.

#### Quản lý ngân hàng câu hỏi → Màn Biên soạn câu hỏi (sửa)

[Điều kiện mở] Bấm nút biểu tượng "Sửa" (bút chì) ở cột Thao tác của một dòng. Bấm nội dung câu hỏi **không** còn vào đây, xem chuyển màn ở trên.

[Chế độ mở] Chế độ sửa, mang theo mã câu hỏi đang chọn.

[Thông tin truyền] `id` của câu hỏi đang sửa.

[Giá trị trả về] Không có.

[Khi thành công] Mở màn `interview_question_authoring` (`SHR0302`) ở chế độ sửa, tại `{basePath}/[mã]/edit` (khu Admin `/admin/...`, khu Giảng viên `/instructor/...`), nạp sẵn dữ liệu của câu hỏi đó [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:59-64, 280-285].

[Khi huỷ] Không có.

#### Quản lý ngân hàng câu hỏi → Màn Biên soạn câu hỏi (nhân bản)

[Điều kiện mở] Bấm nút biểu tượng "Nhân bản" (hai tờ chồng nhau) ở cột Thao tác của một dòng — nhân bản theo từng dòng.

[Chế độ mở] Chế độ sửa một bản ghi vừa được tạo sẵn ở máy chủ (xem Sheet 8, EVT-10; hành vi chính xác
là một đề xuất chưa chốt, xem Câu hỏi mở Q4).

[Thông tin truyền] `id` của câu hỏi vừa nhân bản (do `DuplicateInterviewQuestion` trả về).

[Giá trị trả về] Không có.

[Khi thành công] Mở màn `interview_question_authoring` (`SHR0302`) ở chế độ sửa bản ghi mới, đã điền
sẵn toàn bộ nội dung, câu hỏi đào sâu và tiêu chí đánh giá sao chép từ bản gốc.

[Khi huỷ] Không có — nếu `DuplicateInterviewQuestion` lỗi thì không điều hướng, xem Sheet 8 EVT-10.

#### Quản lý ngân hàng câu hỏi → Popup Xác nhận xoá câu hỏi

[Điều kiện mở] Bấm nút biểu tượng "Xoá" (thùng rác) ở cột Thao tác của một dòng.

[Chế độ mở] Chế độ xác nhận, không nhập liệu.

[Thông tin truyền] `id` và nội dung rút gọn (mã + 60 ký tự đầu) của câu hỏi đang chọn
[Nguồn: 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:460].

[Giá trị trả về] Kết quả chọn "Xoá câu hỏi" hoặc "Huỷ".

[Khi thành công] Popup nêu rõ các phiên phỏng vấn đã dùng câu hỏi này vẫn giữ bản ghi cũ và hành động
không thể hoàn tác [Nguồn: 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:264].

[Khi huỷ] Đóng popup, không câu hỏi nào bị đổi trạng thái.

#### Quản lý ngân hàng câu hỏi → Popup Xác nhận xoá theo lô (mới 2026-10-05, chưa duyệt)

[Điều kiện mở] Đang chọn ít nhất một dòng và bấm "Xoá" trên thanh thao tác theo lô (pop-up nổi ở giữa mép trên màn hình).

[Chế độ mở] Chế độ xác nhận, không nhập liệu, áp dụng cho tập đã chọn tường minh.

[Thông tin truyền] Danh sách `id` câu hỏi đang chọn, số lượng, và số câu trong tập chọn đang bị bộ lọc hiện tại ẩn khỏi bảng (lựa chọn được giữ khi đổi bộ lọc hoặc trang).

[Giá trị trả về] Kết quả chọn "Xoá" hoặc "Huỷ".

[Khi thành công] Popup có tiêu đề "Xoá {số} câu hỏi đã chọn?", cùng ghi chú như xoá một câu (các phiên phỏng vấn đã dùng vẫn giữ lịch sử); khi có câu bị bộ lọc ẩn thì thêm dòng "Cảnh báo: {số} câu hỏi trong số này đang không hiển thị do bộ lọc hiện tại." Xoá mềm theo F6-13 (`status = RETIRED`) [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:150-154, 474-492; 05-coding/frontend/messages/vi.json:1159-1160].

[Khi huỷ] Đóng popup, không câu hỏi nào bị đổi trạng thái, giữ nguyên tập đang chọn.

#### Quản lý ngân hàng câu hỏi → Popup Quản lý chủ đề

[Điều kiện mở] Bấm nút "Quản lý chủ đề" ở thanh tiêu đề. Nút chỉ hiển thị với A3 (`ADMIN`); A2 không thấy.

[Chế độ mở] Chế độ quản lý, nạp sẵn danh sách chủ đề hiện có kèm số câu hỏi đang tham chiếu mỗi chủ đề.

[Thông tin truyền] Không có (danh sách lấy qua `ListQuestionTopics`).

[Giá trị trả về] Không có. Đóng popup thì danh sách chủ đề và tab chủ đề của màn được tải lại.

[Khi thành công] Mỗi thao tác thêm, đổi tên, bật/tắt khung STAR, sắp xếp, xoá có hiệu lực ngay trên máy chủ; popup cập nhật dòng tương ứng.

[Khi huỷ] Đóng popup, các thao tác đã hoàn tất vẫn giữ nguyên (mỗi thao tác là một lần ghi độc lập, không có nút "Lưu tất cả").

#### Quản lý ngân hàng câu hỏi → Popup Quản lý độ khó

[Điều kiện mở] Bấm nút "Quản lý độ khó" ở thanh tiêu đề, cạnh "Quản lý chủ đề". Nút chỉ hiển thị với A3 (`ADMIN`); A2 không thấy [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:312-321].

[Chế độ mở] Chế độ quản lý, nạp sẵn danh sách độ khó hiện có kèm số câu hỏi đang dùng mỗi mức.

[Thông tin truyền] Không có (danh sách lấy qua `ListQuestionLevels`).

[Giá trị trả về] Không có. Đóng popup thì danh sách độ khó, tab cấp độ và cột "Mức độ" của màn được tải lại.

[Khi thành công] Mỗi thao tác thêm, đổi tên, sắp xếp, xoá có hiệu lực ngay trên máy chủ; popup cập nhật dòng tương ứng. Khác popup chủ đề ở chỗ không có công tắc STAR.

[Khi huỷ] Đóng popup, các thao tác đã hoàn tất vẫn giữ nguyên (mỗi thao tác là một lần ghi độc lập).

### 3.2 Sơ đồ

```mermaid
flowchart LR
    nav["Khung điều hướng<br/>nhóm Nội dung"] -->|"chọn Câu hỏi phỏng vấn"| main["Quản lý ngân hàng câu hỏi<br/>interview_question_management"]
    main -->|"Câu hỏi mới"| create["Biên soạn câu hỏi<br/>interview_question_authoring — tạo mới"]
    main -->|"Bấm nội dung câu hỏi"| info["Chi tiết câu hỏi<br/>interview_question_info — chỉ đọc"]
    main -->|"Sửa"| edit["Biên soạn câu hỏi<br/>interview_question_authoring — sửa"]
    main -->|"Nhân bản"| dup["Biên soạn câu hỏi<br/>interview_question_authoring — sửa bản sao"]
    main -->|"Xoá"| confirm["Popup Xác nhận<br/>xoá câu hỏi"]
    confirm --> main
    main -->|"Xoá (theo lô)"| confirmBulk["Popup Xác nhận<br/>xoá theo lô"]
    confirmBulk --> main
    main -->|"Quản lý chủ đề (ADMIN)"| topics["Popup Quản lý<br/>chủ đề"]
    topics --> main
    main -->|"Quản lý độ khó (ADMIN)"| levels["Popup Quản lý<br/>độ khó"]
    levels --> main

    classDef source fill:#F3E5F5,stroke:#9C5CC4,color:#000
    classDef screen fill:#E3F2FD,stroke:#3B82F6,color:#000
    classDef popup fill:#FFF7CC,stroke:#D4A72C,color:#000

    class nav source
    class main screen
    class create,edit,dup,info screen
    class confirm,confirmBulk,topics,levels popup
```

[Nguồn: 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:148-149, 226-228, 259-271, 297, 303-306;
01-rd/screens/shared/SHR0301_interview_question_management.md:79-83, 126-130]

---

## Sheet 4. Bố cục màn hình

### 4.1 Tổng quan màn

[Mục đích màn] Cho giảng viên và quản trị viên xem toàn bộ ngân hàng câu hỏi phỏng vấn lý thuyết kèm chủ
đề, cấp độ, số câu hỏi đào sâu và số tiêu chí đánh giá; lọc/tìm; nhân bản và xoá mềm câu hỏi — là nơi **tạo và
bảo trì nội dung** mà hai màn phía học viên (`interview_bank_list`, `interview_question_detail`) đọc
[Nguồn: 01-rd/screens/shared/SHR0301_interview_question_management.md:13-15, 32-38].

[Luồng nghiệp vụ chính]

1. **Hiển thị ban đầu**: vào màn, hệ thống tải song song hai nhóm dữ liệu — trang đầu của danh sách câu
   hỏi và danh mục chủ đề (đọc từ dữ liệu, do ADMIN quản lý); danh mục độ khó cũng đọc từ dữ liệu (`ListQuestionLevels`). Màn **không có dải thẻ chỉ số**: chỉ có một trang tổng quan Admin/Giảng viên,
   trang danh sách chỉ gồm bộ lọc và danh sách. Trong lúc chờ, danh sách hiển thị khung chờ đúng số dòng
   dự kiến.
2. **Thu hẹp danh sách**: người dùng gõ từ khoá theo nội dung câu hỏi hoặc mã, chọn bộ lọc chủ đề và bộ lọc cấp
   độ (từ 2026-10-05 mỗi bộ lọc là một nút mở pop-up lựa chọn `FilterMenu`, chưa duyệt). Mỗi lần đổi điều kiện thì về trang 1. **Sắp xếp và cỡ trang (2026-10-05, chưa duyệt):** mọi cột bấm được để sắp xếp, bấm lại cùng cột thì đảo chiều, mới vào màn thì chưa sắp theo cột nào; người dùng chọn 8 / 20 / 50 dòng mỗi trang và lựa chọn được nhớ cho lần mở sau; sau khi xoá, nếu trang hiện tại vượt trang cuối mới thì lùi về trang cuối.
3. **Thao tác trên một dòng**: ba nút chỉ có biểu tượng ở cột Thao tác — "Sửa" và "Nhân bản" điều hướng
   sang màn `interview_question_authoring` (`SHR0302`); "Xoá" mở popup xác nhận. Bấm nội dung câu hỏi
   mở trang chỉ đọc `interview_question_info` (`SHR0303`) thay vì form soạn. Tên thao tác hiện ở
   tooltip nhỏ bên dưới nút khi rê chuột hoặc focus bàn phím.
3a. **Chọn nhiều dòng (mới 2026-10-05, chưa duyệt)**: cột đầu có ô chọn ở từng dòng; ô chọn ở tiêu đề chọn hoặc bỏ chọn các dòng đang hiển thị (không gồm dòng ở trang khác hay dòng bị bộ lọc ẩn). Có ít nhất một dòng được chọn thì pop-up thao tác theo lô hiện ở giữa mép trên màn hình (không đẩy bảng xuống) với hai thao tác "Nhân bản" và "Xoá"; lựa chọn được giữ khi đổi bộ lọc hoặc trang, nên "Xoá" theo lô có thể chạm tới câu đang bị lọc ẩn (popup xác nhận cảnh báo). Xoá một dòng cũng bỏ dòng đó khỏi tập đang chọn.
4. **Xoá mềm**: xác nhận trong popup thì đặt `status = RETIRED`, không xoá dòng, không cascade xoá
   `answer_rubrics`/`user_answers` đã dùng câu hỏi đó [Nguồn: 02-bd/database/interview-bank.md:65, 94-96].
5. **Ghi nhật ký**: nhân bản và xoá đều ghi vào `system_audit_logs`, theo F1-14
   [Nguồn: 01-rd/screens/shared/SHR0301_interview_question_management.md:121-125].
6. **Làm mới**: sau khi nhân bản hoặc xoá thành công, danh sách tải lại.
7. **Quản lý chủ đề (chỉ ADMIN)**: nút "Quản lý chủ đề" ở thanh tiêu đề mở popup liệt kê toàn bộ chủ đề
   kèm số câu hỏi đang dùng; ADMIN thêm chủ đề mới, đổi tên, bật/tắt công tắc "Dùng khung STAR" (cờ `uses_star_framework`, quyết định câu hỏi của chủ đề đó kết xuất khung trả lời chuẩn theo 4 mục STAR), đổi thứ tự, xoá. Chủ đề `BEHAVIORAL` seed bật sẵn cờ nhưng xoá/đổi tên được như mọi chủ đề. Xoá bị từ chối khi chủ đề còn
   câu hỏi tham chiếu — popup hiện số câu hỏi và yêu cầu chuyển hoặc xoá các câu đó trước. Không giới hạn số
   chủ đề. A2 không thấy nút này, chỉ chọn chủ đề có sẵn khi soạn câu hỏi ở `SHR0302`. Đóng popup thì tab
   chủ đề tải lại. Mọi thay đổi ghi `system_audit_logs` theo F1-14
   (`DEC-2026-1001-admin-configurable-settings`).
8. **Quản lý độ khó (chỉ ADMIN)**: nút "Quản lý độ khó" cạnh "Quản lý chủ đề" mở popup liệt kê toàn bộ mức độ
   khó kèm số câu hỏi đang dùng; ADMIN thêm mức mới, đổi tên, đổi thứ tự, xoá — cùng cách với chủ đề nhưng
   **không có** công tắc STAR. Ba mức khởi tạo Dễ / Trung bình / Khó chỉ là dữ liệu seed, xoá/đổi tên được như mọi
   mức. Hai bất biến: xoá bị từ chối khi còn câu hỏi dùng mức đó (kể cả câu `RETIRED`), và xoá bị từ chối khi đó là
   mức cuối cùng (form soạn câu hỏi luôn cần một mức để chọn). Không giới hạn số mức. Màu badge không cấu hình
   được: mức seed giữ màu cũ, mức mới màu trung tính. A2 không thấy nút, chỉ chọn mức có sẵn khi soạn ở `SHR0302`.
   Đóng popup thì tab cấp độ tải lại. Mọi thay đổi ghi `system_audit_logs` theo F1-14
   [Nguồn: .nexa/control/decision-registry.md:2441-2458; 02-bd/database/interview-bank.md:30-50].

[Người dùng] Giảng viên hoặc quản trị viên đã đăng nhập, có Function `INTERVIEW_BANK_MANAGEMENT`
[Nguồn: 01-rd/req/identity.md:60; 02-bd/security/interview-bank.md:34-43].

[Tệp liên quan] Không có — "Nhập CSV" ngoài phạm vi bản đầu, không có luồng nhập tệp nào được thiết kế
ở màn này [Nguồn: 01-rd/screens/shared/SHR0301_interview_question_management.md:79-83].

[Phạm vi]
- Không có bộ chọn lớp, khái niệm nhóm câu hỏi hay hành động gán theo lớp — `question_sets` đã loại khỏi
  phạm vi [Nguồn: 02-bd/database/interview-bank.md:179-185].
- Không có form tạo/sửa câu hỏi trên chính màn này — thuộc màn riêng `interview_question_authoring`
  (`SHR0302`), chỉ liên kết tới, không lặp lại đặc tả.
- Không hiển thị dữ liệu cá nhân của học viên (`user_answers`, `recall_ratings`)
  [Nguồn: 02-bd/security/interview-bank.md:27-29, 44-47].
- "Nhập CSV": nút **giữ kích hoạt** (owner chốt 2026-10-01), nhưng chưa có luồng nhập nào đứng sau; bấm nút là stub của bản dựng — xem EVT-8 và nợ prototype ở `06-plan/PROTOTYPE_DEBT.md` mục 16.
- Danh sách chỉ hiện **số lượng** câu hỏi đào sâu và số lượng tiêu chí; nội dung, trọng số tiêu chí
  (`criterion_code`/`description`/`weight_percent`) do màn `interview_question_authoring` soạn và hiển
  thị, màn này không đọc.
- Không có dải thẻ chỉ số tổng hợp (tổng câu hỏi, có tiêu chí đầy đủ, điểm trung bình, chưa dùng lần nào)
  — bỏ ngày 2026-10-01, chỉ giữ một trang tổng quan Admin/Giảng viên.

[Quyền sử dụng]
- Xem: được, khi có `INTERVIEW_BANK_MANAGEMENT:READ`.
- Thêm: không có form thêm câu hỏi ở màn này — điều hướng sang `interview_question_authoring`. Riêng chủ đề: thêm/đổi tên/sắp xếp/xoá chủ đề trong popup "Quản lý chủ đề" yêu cầu **vai trò `ADMIN`** cùng Function `INTERVIEW_BANK_MANAGEMENT` (`CREATE`/`UPDATE`/`DELETE` tương ứng thao tác) — dùng lại Function sẵn có, không tạo Function mới; ràng buộc "chỉ ADMIN" là kiểm vai trò bổ sung, không phải một quyền riêng `[SoT: Suy luận]` (xem Q7). Độ khó: thêm/đổi tên/sắp xếp/xoá mức trong popup "Quản lý độ khó" cũng yêu cầu vai trò `ADMIN` cộng `INTERVIEW_BANK_MANAGEMENT` theo thao tác, cùng quy tắc [Nguồn: 02-bd/security/interview-bank.md:72-80].
- Sửa: không có form sửa tại chỗ — "Sửa" điều hướng sang `interview_question_authoring`; "Nhân bản" gọi
  `DuplicateInterviewQuestion` khi có `INTERVIEW_BANK_MANAGEMENT:CREATE`.
- Thao tác theo lô (mới 2026-10-05, chưa duyệt): "Nhân bản" theo lô cần `INTERVIEW_BANK_MANAGEMENT:CREATE`, "Xoá" theo lô cần `INTERVIEW_BANK_MANAGEMENT:DELETE`, kiểm lại từng câu như thao tác theo dòng; Sheet 9 NO 2, NO 3 áp dụng nguyên.
- Xoá: được, khi có `INTERVIEW_BANK_MANAGEMENT:DELETE` — xoá mềm, đặt `status = RETIRED`, không xoá dòng
  [Nguồn: 02-bd/database/interview-bank.md:65].

[Số bản ghi tối đa] Danh sách phân trang phía máy chủ; người dùng chọn 8 / 20 / 50 dòng một trang, mặc định 8, nhớ ở localStorage khoá `algoprep-interview-questions-page-size` (2026-10-05, chưa duyệt; cùng cơ chế `SHR0201`)
[Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:50-51, 77]
(prototype HTML cũ dùng 6 thẻ một trang, `09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:450`, đã thay
bằng danh sách dạng bảng). Mỗi dòng câu hỏi: 0..n câu hỏi đào sâu, 0..n dòng tiêu chí đánh giá — chỉ hiện
số lượng.

[Nguồn: 01-rd/screens/shared/SHR0301_interview_question_management.md:17-22, 57-125; 02-bd/database/interview-bank.md:8-96;
02-bd/security/interview-bank.md:22-47]

### 4.2 DTO liên quan

- `InterviewQuestionListItemDto` (chứa hai số đếm `followUpCount` và `rubricCount`, không còn danh sách lồng — owner xác nhận 2026-10-01)
- `QuestionTopicOptionDto` (thêm `questionCount`, `sortOrder` cho popup quản lý chủ đề)
- `QuestionTopicManageResultDto`
- `QuestionTopicDeleteRefusedDto`
- `QuestionLevelOptionDto` (`code`, `displayName`, `sortOrder`, `questionCount` — dùng cho tab cấp độ và popup quản lý độ khó)
- `QuestionLevelManageResultDto`
- `QuestionLevelDeleteRefusedDto`
- `DuplicateQuestionResultDto`

`[Suy luận]` — tên DTO do BD này đề xuất, `03-dd/api/interview-bank.md` chốt lại.

### 4.3 Bảng dữ liệu liên quan (6)

| NO | Bảng | Ghi chú |
| --: | :--- | :--- |
| 1 | `interview_questions` | [Nguồn: 02-bd/database/interview-bank.md:52-69] — cột `level_id` (FK `question_levels.id`) thay cho ENUM `difficulty` [Nguồn: 02-bd/database/interview-bank.md:58] |
| 2 | `question_topics` | [Nguồn: 02-bd/database/interview-bank.md:8-28] — đọc cho bộ lọc; ghi (thêm, đổi tên, sắp xếp, xoá) trong popup quản lý chủ đề, chỉ ADMIN |
| 3 | `answer_rubrics` | [Nguồn: 02-bd/database/interview-bank.md:78-96] |
| 4 | `user_answers` | [Nguồn: 02-bd/database/interview-bank.md:110-132] — chỉ đọc để đếm lượt dùng, màn này không ghi bảng này |
| 5 | `system_audit_logs` | [Nguồn: 01-rd/screens/shared/SHR0301_interview_question_management.md:121-125; 02-bd/database/identity.md:104-108] — thuộc schema `identity`, ghi từ mọi hành động của `interview-bank` theo F1-14 |
| 6 | `question_levels` | [Nguồn: 02-bd/database/interview-bank.md:30-50] — đọc cho bộ lọc và cột "Mức độ"; ghi (thêm, đổi tên, sắp xếp, xoá) trong popup quản lý độ khó, chỉ ADMIN |

### 4.4 Vùng bố cục

Đối chiếu `09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html` (504 dòng) — bằng chứng bố cục chỉ-đọc,
**không phải** design system cuối cùng. Khu Giảng viên không có prototype HTML riêng; bản dựng Next.js dùng cùng một view cho cả hai khu (từ 2026-10-03), chỉ khác tiền tố `basePath`
[Nguồn: 05-coding/frontend/src/app/(instructor)/instructor/interview-questions/page.tsx:3-5].

| Vùng | Vị trí trong prototype | Nội dung |
| :--- | :--- | :--- |
| Thanh điều hướng bên trái (khung chung) | `:68-133`, dữ liệu nhóm `:301-321` | Nhóm "Nội dung" đang mở, mục con "Câu hỏi phỏng vấn" đang chọn (`activeKey = 'iquestions'`, `:297`), badge đếm tổng số câu (`148`, `:305`) |
| Thanh tiêu đề dính trên | `:138-150` | Tiêu đề, phụ đề động "N câu · ...", công tắc theme, nút "Nhập CSV", nút "Câu hỏi mới"; thêm nút "Quản lý chủ đề" và nút "Quản lý độ khó" (chỉ ADMIN) — **không có trong prototype**, bổ sung theo quyết định 2026-10-01 và 2026-10-03 |
| Thanh lọc (trong `Card` duy nhất của danh sách) | `:165-181`, dữ liệu tab `:474-475`, kết quả `:478` | Ô tìm kiếm, dải tab chủ đề (Tất cả + số chủ đề đọc từ dữ liệu; prototype vẽ 5 chủ đề), dải tab cấp độ (Tất cả + số mức đọc từ dữ liệu; prototype vẽ 3 mức Dễ/Trung bình/Khó làm ví dụ); từ 2026-10-05 bản dựng thay mỗi dải bằng một nút `FilterMenu` "{nhãn}: {lựa chọn hiện tại}" mở pop-up kính nhỏ liệt kê lựa chọn (chưa duyệt), nhãn số kết quả; cả thanh là `FilterBar` dùng chung (V1.14, chưa duyệt), không đổi hành vi — cùng nằm trong một `Card` với danh sách bên dưới |
| Danh sách câu hỏi (`DataTable`) | Bản dựng: `interview-question-management-view.tsx:414-437` (bố cục thẻ của prototype `:183-233` đã bỏ) | Bảng 9 cột: Mã, Câu hỏi (liên kết sang màn chi tiết chỉ đọc), Chủ đề, Mức độ (badge), Đào sâu (số lượng), Tiêu chí (số lượng), Lượt dùng, Điểm TB, Thao tác (ba nút biểu tượng Sửa/Nhân bản/Xoá, mỗi nút có tooltip nhỏ bên dưới). Từ 2026-10-05, 8 cột dữ liệu sắp xếp được (cột Thao tác thì không), khi mới vào màn chưa sắp theo cột nào; thêm cột ô chọn ở đầu (chưa duyệt); cột "Câu hỏi" rộng 38% và liên kết cắt bớt bằng dấu ba chấm nên câu dài không làm bảng rộng quá thẻ |
| Phân trang | `:235-244` (prototype), bản dựng `:395-415` | Nút "Trước" / "Sau", các nút số trang và nhãn "Trang x / y · hiển thị n câu"; từ 2026-10-05 có nút chọn số dòng mỗi trang 8 / 20 / 50 (mặc định 8) đứng trước các mũi tên, cùng thành phần `Pagination` với `SHR0201` (`pageSizeOptions`); nút là `FilterMenu` `compact` mở pop-up kính lên phía trên, không còn ô chọn gốc (chưa duyệt) |
| Thanh thao tác theo lô (mới 2026-10-05, chưa duyệt) | Không có trong prototype; bản dựng `05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:394-412` | Pop-up kính nhỏ (`BulkActionBar`) nổi cố định ở giữa mép trên màn hình khi có dòng được chọn, trượt vào 180 ms, không chèn phía trên bảng; nhãn "Đã chọn n câu hỏi" và hai nút "Nhân bản", "Xoá" |
| Hộp thoại xác nhận xoá (`confirmOpen`) | `:259-271` | Tiêu đề "Xoá câu hỏi?", nội dung nêu mã và trích 60 ký tự đầu câu hỏi (`:460`), ghi chú giữ lịch sử phiên cũ và không hoàn tác (`:264`), nút Huỷ/Xoá câu hỏi |
| Hộp thoại quản lý chủ đề | Không có trong prototype | Popup (chỉ ADMIN): danh sách dòng chủ đề (tên, số câu hỏi, nút lên/xuống, đổi tên, xoá), ô nhập tên chủ đề mới và nút Thêm **[Đợi nextjs]**; từ 2026-10-05 (chưa duyệt) danh sách cuộn từ mục thứ 6, hiện 5 dòng cùng thanh cuộn mỏng [Nguồn: 05-coding/frontend/src/shared/ui/overlay/managed-list-dialog.tsx:20-23, 120-126] |
| Hộp thoại quản lý độ khó | Không có trong prototype HTML; bản dựng `level-manager-dialog.tsx:25-66` | Popup (chỉ ADMIN), cùng khung `ManagedListDialog` với chủ đề, không có công tắc STAR: danh sách dòng độ khó (ô tên, "{n} câu", nút lưu tên, nút xoá), ô nhập tên mức mới và nút Thêm; từ 2026-10-05 (chưa duyệt) danh sách cuộn từ mục thứ 6, hiện 5 dòng cùng thanh cuộn mỏng [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/level-manager-dialog.tsx:25-66; 05-coding/frontend/src/shared/ui/overlay/managed-list-dialog.tsx:20-23, 120-126] |
| Chân trang (khung chung) | `:246-254` | Phiên bản, trạng thái dịch vụ |

Danh sách là bảng (`DataTable`) có độ rộng tối thiểu 1000px, cuộn ngang khi màn hẹp
[Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:428].
Chủ trương 2026-10-01 (owner): hai màn quản trị ngân hàng — bài toán và câu hỏi phỏng vấn — dùng cùng bố
cục danh sách để đọc giống nhau; dải thẻ chỉ số bị bỏ, chỉ giữ một trang tổng quan. Nút thao tác chỉ có
biểu tượng qua thành phần dùng chung `IconAction`; tooltip nhỏ hiện bên dưới nút khi rê chuột hoặc focus,
dựng trên `body` bằng portal để không bị cắt bởi vùng cuộn ngang của bảng
[Nguồn: 05-coding/frontend/src/shared/ui/primitives/icon-action.tsx:27-32, 94-107]. Không quy định màu sắc, khoảng
cách hay typography ở BD.

### 4.5 Cấu trúc slice FSD [Nội bộ]

| Khối | Slice dự kiến | Căn cứ |
| :--- | :--- | :--- |
| Trang | `views/shared/interview-question-management` | Quy ước FSD của dự án |
| Route (hai khu) | `app/(admin)/admin/interview-questions/page.tsx` truyền `basePath="/admin/interview-questions"` kèm `canManageTopics`; `app/(instructor)/instructor/interview-questions/page.tsx` truyền `basePath="/instructor/interview-questions"`, không có `canManageTopics` | Đã dựng; cùng một view, props `basePath` (bắt buộc) và `canManageTopics` [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:59-64; 05-coding/frontend/src/app/(admin)/admin/interview-questions/page.tsx:6; 05-coding/frontend/src/app/(instructor)/instructor/interview-questions/page.tsx:5] |
| Khung Admin/Giảng viên | Dùng lại `widgets/admin-shell` cho `/admin/...`; khung Giảng viên tương đương **[Đợi nextjs]** | `02-bd/screens/admin/_shell.md` |
| Bộ lọc | `features/interview-question-filter` | Prototype `:165-181` |
| Danh sách câu hỏi | `shared/ui/DataTable` + `entities/interview-question` (thẻ `InterviewQuestionCard` giữ lại cho màn học viên, không dùng ở màn này) | Bản dựng `interview-question-management-view.tsx:7-10, 414-437` |
| Nút thao tác trên dòng | `shared/ui/IconAction` | `05-coding/frontend/src/shared/ui/primitives/icon-action.tsx` |
| Nhân bản / Xoá | `features/interview-question-duplicate`, `features/interview-question-delete` | Prototype `:226-228, 259-271` |
| Phân trang | Dùng lại component phân trang chung nếu đã có ở `shared/` | Prototype `:235-244` |

`[Suy luận]` — ánh xạ slice do BD đề xuất, DD màn hình chốt lại.

**Hiện trạng bản dựng — quản lý độ khó (2026-10-03, bản cuối sau khi dựng sắp xếp lại).** Bảng sau ghi mã thực tế đã đối chiếu, là prototype chưa có DD
(`// PROTOTYPE — no DD yet`, [Nguồn: 05-coding/frontend/src/entities/interview-question/model/level-store.ts:1]).

| Thành phần | Vị trí trong mã | Hiện trạng |
| :--- | :--- | :--- |
| Kho độ khó | `entities/interview-question/model/level-store.ts:24-33` | Kho **in-memory** dựng bằng `createManagedListStore`, reset khi tải lại trang; thay cho bảng `question_levels`. Seed ba dòng khoá `EASY`/`MEDIUM`/`HARD` (viết hoa như `code` của BD; nhãn Dễ/Trung bình/Khó, tông `success`/`warn`/`negative`, `:26-28`). Mức mới: `addInterviewLevel` gán tông `neutral` (`:36`); `levelTone` trả `neutral` khi không tìm thấy (`:41-42`). Khoá mức mới là slug viết hoa, vẫn **chỉ nằm trong bộ nhớ** (chưa có cột `code` thật, chưa có DB) |
| Sinh khoá slug | `shared/lib/managed-list-store.ts:30-39, 54-60, 82`; `level-store.ts:32` | `slugKeys: true` bật sinh khoá: `slugify` bỏ dấu tiếng Việt, đổi `đ` thành `d`, viết hoa, ký tự không phải chữ-số thành `_` (`:30-39`); `uniqueSlug` gặp khoá trùng thì thêm hậu tố `_2`, `_3`... (`:54-60`); khoá chỉ sinh một lần ở `add` (`:82`), `update` không đổi khoá. Ví dụ "Rất khó" thành `RAT_KHO`, thêm "rất-khó!" thành `RAT_KHO_2` [Nguồn: 05-coding/frontend/src/entities/interview-question/model/level-store.test.ts:32-44] |
| Thứ tự | `managed-list-store.ts:102-110`; `level-store.ts:38` | **Đã dựng sắp xếp lại**: `moveInterviewLevel` là `store.move(key, -1 hoặc 1)`, đổi chỗ với dòng liền kề, bỏ qua ở hai đầu. Thứ tự là thứ tự mảng trong bộ nhớ (chưa có `sort_order` thật, chưa có `ReorderQuestionLevels` gọi máy chủ). Sắp xếp lại cũng đã dựng cho popup chủ đề câu hỏi (`topic-manager-dialog.tsx:40`) và chủ đề bài toán (`problem-management-view.tsx:557`) [Nguồn: 05-coding/frontend/src/entities/interview-question/model/level-store.test.ts:52-60] |
| Bất biến tối thiểu một mức | `managed-list-store.ts:26-27, 116-120`; `level-store.ts:32` | **Cũng được kho thực thi**, không chỉ popup: `remove` trả `false` và không đổi gì khi số mục `<= minItems`; `level-store.ts` đặt `minItems: 1` [Nguồn: 05-coding/frontend/src/entities/interview-question/model/level-store.test.ts:62-68] |
| Popup | `views/shared/interview-question-management/ui/level-manager-dialog.tsx:25-66` | Bọc `ManagedListDialog` với `onMove={moveInterviewLevel}` (`:38`), `keepAtLeast={1}` (`:39`), nhãn `usage` "{n} câu" (`:50`), `deleteBlocked` (`:51`), `deleteLast` (`:52`), `moveUp`/`moveDown` (`:53-54`), lỗi `empty`/`duplicate` (`:55-58`), toast hoàn tất `done.add`/`rename`/`remove` (`:59-63`). Không có công tắc STAR |
| Dùng chung | `shared/ui/overlay/managed-list-dialog.tsx:57-60, 129-130, 147-162, 183` | Prop `onMove` (`:51-52`): khi có thì mỗi dòng thêm hai nút `IconAction` Lên/Xuống, "Lên" vô hiệu ở dòng đầu, "Xuống" vô hiệu ở dòng cuối (`:134-149`). Prop `keepAtLeast` (mặc định 0, `:53-54`): khi số mục `<= keepAtLeast` thì nút xoá bị vô hiệu kèm lý do (`:117, 170`). Số câu đang dùng lấy từ prop `usage` theo khoá (`:116`) |
| Nút mở popup | `views/shared/interview-question-management/ui/interview-question-management-view.tsx:312-321` | Nút "Quản lý độ khó" (`manageLevels`) chỉ hiện khi `canManageTopics`; bấm đặt `managingLevels` (`:66`) |
| Số câu theo mức | `interview-question-management-view.tsx:137-143` | `levelUsage` đếm theo `question.level` trên **trang đang tải** (mock, `page.questions`), bỏ qua câu đã xoá trong phiên; với dữ liệu thật thì `questionCount` do máy chủ trả (gồm cả `RETIRED`) |
| Bộ lọc và cột | `interview-question-management-view.tsx:70, 75, 224-234, 383-391` | `useInterviewLevels()` đọc danh sách; tab cấp độ gồm "Tất cả" cộng một mục mỗi mức (`:318`); cột "Mức độ" dùng `levelLabel` và `levelTone` |
| Gắn popup | `interview-question-management-view.tsx:468-472` | `LevelManagerDialog` nhận `usage={levelUsage}` |
| Kiểm thử | `interview-question-management-view.test.tsx:34-53`; `level-store.test.ts:31-68`; `managed-list-store.test.ts:44-70`; `managed-list-dialog.test.tsx:85-118` | ADMIN thấy hai nút, A2 không thấy; xoá bị chặn ở cả 3 mức đang có câu hỏi dùng; thêm mức mới chạy được; kho: slug và hậu tố, đổi chỗ, từ chối xoá mức cuối; hộp thoại: `keepAtLeast` và nút Lên/Xuống |

**Hiện trạng bản dựng danh sách (2026-10-05, số dòng theo mã hiện tại; toàn bộ chi tiết dưới đây chưa được owner xem, ghi là chưa duyệt):**

| Thành phần | Vị trí trong mã | Hiện trạng |
| :--- | :--- | :--- |
| Sắp xếp mọi cột | `05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:54, 79-82, 101-124, 188-272, 429-436` | Kiểu `SortKey` có 8 khoá (`code`, `question`, `topic`, `level`, `followUps`, `rubric`, `usage`, `score`); trạng thái khởi đầu `key: ""` nghĩa là chưa sắp, `filtered` trả nguyên thứ tự tự nhiên (`if (!sort.key) return rows`). Cột "Mã" so sánh số tự nhiên; "Chủ đề" so theo nhãn; "Mức độ" theo thứ tự danh mục độ khó, không theo tên. Đổi cột sắp thì về trang 1. `DataTable` tự phát `aria-sort` (`shared/ui/data/data-table.tsx:161`) |
| Cỡ trang nhớ | `05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:50-51, 77, 439-447` | `usePersistedPageSize("algoprep-interview-questions-page-size", PAGE_SIZES, 8)` với `PAGE_SIZES = [8, 20, 50]`; đổi lựa chọn thì về trang 1. Cùng cơ chế `algoprep-problems-page-size` của `SHR0201` |
| Thanh công cụ danh sách (`FilterBar`) | `05-coding/frontend/src/shared/ui/data/filter-bar.tsx:3-7, 14-28, 30-57`; dùng ở `05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:361-392` | Mới 2026-10-05, **chưa duyệt**, không đổi hành vi. Cùng thành phần với `SHR0201`: `search` có `onSubmit: announceSearch` (Enter báo số kết quả bằng toast, gõ trực tiếp không toast), hai `FilterMenu` chủ đề và cấp độ là `children`, `resultCount` ở mép phải |
| Bộ lọc dạng nút mở pop-up (`FilterMenu`) | `05-coding/frontend/src/shared/ui/data/filter-menu.tsx:3-14, 21-28, 44-55, 63-80, 82-108, 110-143`; `05-coding/frontend/src/app/globals.css:628-653, 654-672`; dùng ở `05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:361-392` | Mới 2026-10-05, **chưa duyệt**. Thay `SegmentedTabs` cho hai bộ lọc chủ đề và cấp độ (prop `maxInline` đã xoá). Nút có `aria-haspopup="listbox"`, hiện "{nhãn}: {lựa chọn hiện tại}" kèm mũi tên; pop-up là `role="listbox"` gồm các `role="option"` (`aria-selected`, dấu tích ở lựa chọn hiện tại), cuộn khi quá cao, không giới hạn số lựa chọn. ArrowUp/ArrowDown/Home/End di chuyển, Enter hoặc Space chọn, Escape đóng và trả tiêu điểm về nút, Tab hoặc bấm ra ngoài thì đóng. Dựng tại chỗ (không portal) để token kính của vỏ Admin và Giảng viên có hiệu lực; kiểu `.glass-card--popover` và thanh cuộn `.scrollbar-glass`. Cùng thành phần với `SHR0201` |
| Kẹp trang | `05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:145-148` | `safePage = Math.min(currentPage, totalPages)`; sau khi xoá không còn trang rỗng |
| Hành động chưa có API | `05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:286-291, 333-352` | "Nhân bản" ở dòng và chọn tệp ở "Nhập CSV" cùng gọi `toast.info(t("toast.notWired", { action }))`, khoá `toast.notWired` ("{hành động}: chưa nối API trong bản mẫu, chưa có thay đổi nào", `messages/vi.json:1155`), không còn toast thành công |
| Cửa sổ quản lý có danh sách cuộn | `05-coding/frontend/src/shared/ui/overlay/managed-list-dialog.tsx:20-23, 120-126` | `ManagedListDialog` đặt `SCROLL_FROM = 6`: từ 6 mục trở lên thì danh sách có `max-height` đúng 5 dòng và cuộn dọc với `.scrollbar-glass`; dưới 6 mục thì dãn tự nhiên. Áp cho "Quản lý chủ đề" và "Quản lý độ khó" (chưa duyệt) |
| Chọn nhiều dòng | `05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:86-87, 156-163, 419-426`; `shared/ui/data/data-table.tsx` (thuộc tính `selection`) | `selected` là tập mã câu hỏi; `DataTable` dựng cột ô chọn gốc, ô ở tiêu đề gọi `onToggleAll` với các dòng `visible`; nhãn truy cập `selectAll`, `selectRow` (`05-coding/frontend/messages/vi.json:1152-1153`). Lựa chọn giữ khi đổi bộ lọc hoặc trang (chưa duyệt) |
| Thanh thao tác theo lô | `05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:394-412`; `05-coding/frontend/src/shared/ui/data/bulk-action-bar.tsx:3-15, 30-56`; `05-coding/frontend/src/app/globals.css:674-695` | Cùng `BulkActionBar` với `SHR0201`: pop-up `role="region"` dựng qua portal ra `.admin-shell` hoặc `.instructor-shell`, `fixed top-3`, lớp `.bulk-pop-in`; hai nút "Nhân bản" (`toast.info` khoá `toast.notWired`, chưa nối API, như nút theo dòng) và "Xoá" (đặt `confirmingBulkDelete`) (chưa duyệt) |
| Xoá theo lô qua hộp xác nhận | `05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:150-154, 165-170, 474-492`; `05-coding/frontend/messages/vi.json:1159-1160, 1164` | `deleteSelected` đánh dấu xoá trong dữ liệu giả, bỏ chọn, toast "Đã xoá {count} câu hỏi"; `hiddenSelectedCount` đếm câu đã chọn không còn trong `filtered`, lớn hơn 0 thì thêm dòng cảnh báo màu `--color-admin-warn-text`; thân hộp dùng lại `confirmDeleteBody` của xoá một câu (chưa duyệt) |
| Xoá một dòng bỏ chọn | `05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:494-510` | Xác nhận xoá một câu cũng gỡ mã đó khỏi `selected` (số đã chọn không còn tính dòng đã xoá) |
| Bố cục bảng | `05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:195, 199-212` | Cột "Câu hỏi" `width: "38%"` và liên kết `w-0 min-w-full truncate`; ô mã `whitespace-nowrap`, để cột Thao tác không bị đẩy ra ngoài thẻ khi thêm cột ô chọn (chỉ bố cục, không đổi hành vi BD) |
| Liên kết nội dung câu hỏi | `05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:198-211` | Thêm thuộc tính `title` bằng nội dung đầy đủ (hiện khi nội dung bị cắt) |

> [Nội bộ] Ảnh minh hoạ đặt ở `08-diagram/02-bd/screens/shared/`, chụp bằng Playwright trên ứng dụng
> Next.js thật khi đã có mã chạy được. Chưa có thì tham chiếu prototype, không vẽ tay.

---

## Sheet 5. Danh sách item màn hình

> Quy ước ID, bộ thẻ và giá trị chuẩn: `02-bd/_rules/bd-template-9sheet.md` mục 2, 3, 4.

### Khu vực A — Thanh tiêu đề

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Thanh tiêu đề | | | | | | | | | | | | | |
| | 1 | Tiêu đề màn | `interviewQuestionManagement.header.title` | - | - | Label | String | - | - | O | Ngân hàng câu hỏi phỏng vấn | - | Tên màn hiển thị cố định<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] - |
| | 2 | Mô tả phụ | `interviewQuestionManagement.header.subtitle` | `interview_questions` | `status` | Label | String | - | - | O | - | `{số} câu · chủ đề, cấp độ, câu hỏi đào sâu và tiêu chí đánh giá` | Số câu là tổng số câu hỏi `ACTIVE`, phần chữ còn lại là nhãn tĩnh i18n<br>[Công thức] `COUNT(interview_questions WHERE status = 'ACTIVE')` [Nguồn: 02-bd/database/interview-bank.md:65]<br>[EVT liên quan] EVT-1 |
| | 3 | Nhập CSV | `interviewQuestionManagement.header.btnImportCsv` | - | - | Button | - | - | - | I | - | - | Ngoài phạm vi bản đầu, không cấp mã nghiệp vụ [Nguồn: 01-rd/screens/shared/SHR0301_interview_question_management.md:79-83]. Nút **kích hoạt**, không vô hiệu hoá, không tooltip "Sắp ra mắt" (owner chốt 2026-10-01); bấm nút hiện chưa làm gì, xem EVT-8<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-8 |
| | 4 | Câu hỏi mới | `interviewQuestionManagement.header.btnNewQuestion` | - | - | Button | - | - | - | I | - | - | Điều hướng sang `interview_question_authoring` ở chế độ tạo mới (`{basePath}/new`; khu Admin `/admin/interview-questions/new`, khu Giảng viên `/instructor/interview-questions/new`) [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:353-355]<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-7 |
| | 5 | Quản lý chủ đề | `interviewQuestionManagement.header.btnManageTopics` | `question_topics` | - | Button | - | - | - | I | - | - | Mở popup quản lý chủ đề; chỉ ADMIN thấy (đã chốt 2026-10-01, `DEC-2026-1001-admin-configurable-settings`)<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-14 |
| | 6 | Quản lý độ khó | `interviewQuestionManagement.header.btnManageLevels` | `question_levels` | - | Button | - | - | - | I | - | - | Mở popup quản lý độ khó; chỉ ADMIN thấy, đặt cạnh "Quản lý chủ đề" (prop `canManageTopics` trong mã dùng chung cho cả hai nút) [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:312-321] (đã chốt 2026-10-03, `DEC-2026-1001-admin-configurable-settings` mục 6)<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-22 |

### Khu vực B — Bộ lọc

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Bộ lọc | | | | | | | | | | | | | |
| | 1 | Ô tìm kiếm | `interviewQuestionManagement.filter.query` | `interview_questions` | `title`, `content_markdown` | TextBox | String | 200 | - | I | rỗng | - | Tìm theo nội dung câu hỏi hoặc mã câu hỏi (placeholder prototype ghi "hoặc thẻ" [Nguồn: 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:168]; bản dựng lọc theo nội dung và mã [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:96-98])<br>[Nguồn giá trị] Giá trị người dùng nhập; khớp qua full-text search trên `title` + `content_markdown` [Nguồn: 02-bd/database/interview-bank.md:69-71]<br>[EVT liên quan] EVT-2 |
| | 2 | Tab chủ đề | `interviewQuestionManagement.filter.topicTabs` | `question_topics` | `code`, `display_name` | Button | String | - | - | I | Tất cả | - | Tab "Tất cả" + một tab cho mỗi chủ đề trong `question_topics`, sắp theo `sort_order`; số tab không cố định vì ADMIN thêm/xoá chủ đề được (5 chủ đề khởi tạo: Lý thuyết CS, System design, Database, Ngôn ngữ, Hành vi) [Nguồn: 02-bd/database/interview-bank.md:8-28]. **Cập nhật 2026-10-05 (chưa duyệt):** bộ lọc là nút `FilterMenu` mở pop-up kính liệt kê lựa chọn, không giới hạn số lựa chọn (thay cho dải nút bật và cho ô chọn gốc của V1.10) [Nguồn: 05-coding/frontend/src/shared/ui/data/filter-menu.tsx:82-143; 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:361-392]<br>[Nguồn giá trị] `question_topics.display_name`, lọc theo `interview_questions.topic_id`<br>[EVT liên quan] EVT-3 |
| | 3 | Tab cấp độ | `interviewQuestionManagement.filter.levelTabs` | `question_levels` | `code`, `display_name` | Button | String | - | - | I | Tất cả | - | Tab "Tất cả" + một tab cho mỗi mức trong `question_levels`, sắp theo `sort_order`; số tab không cố định vì ADMIN thêm/xoá mức được (3 mức khởi tạo, ví dụ: Dễ, Trung bình, Khó) [Nguồn: 02-bd/database/interview-bank.md:30-50]. **Cập nhật 2026-10-05 (chưa duyệt):** bộ lọc là nút `FilterMenu` mở pop-up kính liệt kê lựa chọn, không giới hạn số lựa chọn (thay cho dải nút bật và cho ô chọn gốc của V1.10) [Nguồn: 05-coding/frontend/src/shared/ui/data/filter-menu.tsx:82-143; 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:361-392]<br>[Nguồn giá trị] `question_levels.display_name`, lọc theo `interview_questions.level_id`. Bản dựng đọc từ kho độ khó [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:383-391]<br>[EVT liên quan] EVT-4 |
| | 4 | Số kết quả | `interviewQuestionManagement.filter.resultCount` | - | - | Label | String | - | - | O | - | `{số} / {số} câu` | Số câu khớp bộ lọc trên tổng số<br>[Công thức] Số bản ghi khớp điều kiện lọc, chia cho tổng số câu hỏi `ACTIVE` — cả hai lấy từ phản hồi của `ListInterviewQuestions`<br>[EVT liên quan] EVT-2, EVT-3, EVT-4 |

### Khu vực C — Danh sách câu hỏi

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Danh sách câu hỏi | | | | | | | | | | | | | |
| | 1 | Danh sách câu hỏi (bảng) | `interviewQuestionManagement.list` | `interview_questions` | - | List | List | - | - | O | rỗng | - | Bảng `DataTable` trong một `Card` cùng thanh lọc, số dòng mỗi trang chọn 8 / 20 / 50 (mặc định 8, Khu vực D NO 4), phân trang phía máy chủ [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:50-51, 77, 414-437]<br>[Nguồn giá trị] Kết quả gọi `ListInterviewQuestions`<br>[EVT liên quan] EVT-1 |
| | 2 | Mã câu hỏi | `interviewQuestionManagement.list.col.code` | `interview_questions` | `id` | ListColumn | String | - | - | O | - | `IQ-{số}` (minh hoạ) | Cột "Mã", chữ đơn cách đều. Prototype hiển thị mã dạng `IQ-014` [Nguồn: 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:383] nhưng schema chỉ có `id` UUID, không có cột định danh dễ đọc riêng [Nguồn: 02-bd/database/interview-bank.md:56]. **Chưa có nguồn cho định dạng đúng như prototype**, xem Q5<br>[EVT liên quan] - |
| | 3 | Nội dung câu hỏi | `interviewQuestionManagement.list.col.content` | `interview_questions` | `title` | Link | String | - | - | O | - | - | Cột "Câu hỏi": một dòng, cắt bớt khi dài, là liên kết sang màn chi tiết chỉ đọc `interview_question_info` (`SHR0303`) của câu hỏi đó, từ 2026-10-02 (trước đó sang màn sửa) [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:198-211]<br>[Nguồn giá trị] Cột `title` — độ dài ngắn phù hợp hiển thị nguyên văn trong một dòng bảng, khác `content_markdown` (nội dung Markdown đầy đủ, dùng ở màn `interview_question_authoring`) `[Suy luận]`<br>[EVT liên quan] EVT-21 |
| | 4 | Chủ đề | `interviewQuestionManagement.list.col.topic` | `question_topics` | `display_name` | Label | String | - | - | O | - | - | Cột "Chủ đề", tên chủ đề của câu hỏi [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:213-223]<br>[Nguồn giá trị] `question_topics.display_name` qua `interview_questions.topic_id`<br>[EVT liên quan] - |
| | 5 | Nhãn cấp độ | `interviewQuestionManagement.list.col.level` | `question_levels` | `display_name` | Badge | String | - | - | O | - | Nhãn kèm màu | Cột "Mức độ": tên mức của câu hỏi (ví dụ Dễ/Trung bình/Khó với dữ liệu seed). Màu: ba mức seed giữ màu cũ (thành công/cảnh báo/tiêu cực), mức ADMIN thêm hiện màu trung tính — màu không cấu hình được [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:224-234; 05-coding/frontend/src/entities/interview-question/model/level-store.ts:26-28, 36]<br>[Nguồn giá trị] `question_levels.display_name` qua `interview_questions.level_id`<br>[EVT liên quan] - |
| | 6 | Số câu hỏi đào sâu | `interviewQuestionManagement.list.col.followUps` | `interview_questions` | `follow_up_questions` | ListColumn | Number | 3 | - | O | 0 | Số nguyên | Cột "Đào sâu": chỉ hiện **số lượng** phần tử, không liệt kê nội dung — nội dung xem ở màn `interview_question_authoring` [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:7-9, 235-242]<br>[Công thức] Số phần tử mảng `follow_up_questions` [Nguồn: 02-bd/database/interview-bank.md:64]<br>[EVT liên quan] - |
| | 7 | Số tiêu chí đánh giá | `interviewQuestionManagement.list.col.rubricCount` | `answer_rubrics` | `question_id` | ListColumn | Number | 3 | - | O | 0 | Số nguyên | Cột "Tiêu chí": chỉ hiện **số dòng** `answer_rubrics`, không hiện tên, trọng số hay thanh trọng số; 0 khi câu hỏi thiếu rubric [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:243-250]<br>[Công thức] `COUNT(answer_rubrics WHERE question_id = iq.id)` [Nguồn: 02-bd/database/interview-bank.md:78-96]<br>[EVT liên quan] - |
| | 8 | Lượt dùng | `interviewQuestionManagement.list.col.usage` | `user_answers` | `question_id` | ListColumn | Number | - | - | O | 0 | Số nguyên, phân tách nghìn theo `vi-VN` | Cột "Lượt dùng" [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:251-262]<br>[Công thức] `COUNT(user_answers WHERE question_id = iq.id)` [Nguồn: 02-bd/database/interview-bank.md:110-132]<br>[EVT liên quan] - |
| | 9 | Điểm trung bình | `interviewQuestionManagement.list.col.avgScore` | - | - | ListColumn | Number | 3 | - | O | - | `{số}` một chữ số thập phân (thang 5) | Cột "Điểm TB" [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:263-272]<br>[Nguồn giá trị] **Chưa có nguồn** — `user_answers.feedback_result_json` là phản hồi định tính, schema không có trường điểm số 1-5 rời rạc [Nguồn: 02-bd/database/interview-bank.md:124], xem Q2<br>[EVT liên quan] - |
| | 10 | Sửa | `interviewQuestionManagement.list.col.btnEdit` | - | - | Button | - | - | - | I | - | Chỉ biểu tượng bút chì | Nút vuông chỉ có biểu tượng (`IconAction`, liên kết), tooltip "Sửa" nhỏ bên dưới khi rê chuột hoặc focus. Điều hướng sang `interview_question_authoring` ở chế độ sửa (`{basePath}/[mã]/edit`), mang theo `id` [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:280-285; 05-coding/frontend/src/shared/ui/primitives/icon-action.tsx:10-25, 70-82]<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-9 |
| | 11 | Nhân bản | `interviewQuestionManagement.list.col.btnDuplicate` | - | - | Button | - | - | - | I | - | Chỉ biểu tượng hai tờ chồng | Nút biểu tượng, tooltip "Nhân bản". Nhân bản theo từng dòng: tạo bản sao câu hỏi rồi điều hướng sang `interview_question_authoring` ở chế độ sửa bản sao [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:286-291]. Hành vi chính xác chưa chốt, xem Q4<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-10 |
| | 12 | Xoá | `interviewQuestionManagement.list.col.btnDelete` | - | - | Button | - | - | - | I | - | Chỉ biểu tượng thùng rác, tông cảnh báo | Nút biểu tượng, tooltip "Xoá". Mở popup xác nhận xoá mềm [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:292-298]<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-11 |
| | 13 | Sắp xếp theo cột | `interviewQuestionManagement.list.sort` | `interview_questions` | `code`, `title`, `topic_id`, `level_id` | Button | Enum | - | - | I | Chưa sắp theo cột nào | - | **Mới 2026-10-05, chưa duyệt (owner chọn phương án A cho F6, khớp `SHR0201`).** Bấm tiêu đề một trong 8 cột dữ liệu (Mã, Câu hỏi, Chủ đề, Mức độ, Đào sâu, Tiêu chí, Lượt dùng, Điểm TB) để sắp xếp tăng; bấm lại cùng cột thì đảo chiều; tiêu đề cột đang sắp có `aria-sort`. Khi mới vào màn **không cột nào được sắp**, danh sách giữ thứ tự tự nhiên của ngân hàng. Cột "Mức độ" sắp theo thứ tự danh mục độ khó (`question_levels.sort_order`), không theo tên. Đổi cột sắp thì về trang 1 [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:54, 79-82, 101-124, 429-436]<br>[Nguồn giá trị] Tham số sắp xếp của `ListInterviewQuestions` (tên tham số do DD chốt)<br>[EVT liên quan] EVT-28 |
| | 14 | Ô chọn dòng | `interviewQuestionManagement.list.col.checkbox` | - | - | Button | Boolean | - | - | I | Không chọn | - | **Mới 2026-10-05, chưa duyệt (owner yêu cầu cho đồng bộ với `SHR0201`).** Ô chọn gốc ở đầu mỗi dòng, nhãn truy cập "Chọn câu hỏi {mã}"; trạng thái chọn tồn tại trên màn, giữ nguyên qua đổi trang và đổi bộ lọc `[SoT: Suy luận]` (cùng cách `SHR0201`) [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:86, 156-163, 419-426]<br>[Nguồn giá trị] Trạng thái chọn của màn hình<br>[EVT liên quan] EVT-30 |
| | 15 | Ô chọn tất cả | `interviewQuestionManagement.list.col.checkboxAll` | - | - | Button | Boolean | - | - | I | Không chọn | - | **Mới 2026-10-05, chưa duyệt.** Ô ở tiêu đề cột chọn: chọn hoặc bỏ chọn **các dòng đang hiển thị** (một trang sau lọc và sắp xếp), không gồm dòng ở trang khác; nhãn truy cập "Chọn tất cả câu hỏi đang hiển thị" [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:422-425; 05-coding/frontend/messages/vi.json:1152]<br>[Nguồn giá trị] Trạng thái chọn của màn hình<br>[EVT liên quan] EVT-31 |

### Khu vực D — Phân trang

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Phân trang | | | | | | | | | | | | | |
| | 1 | Nhãn trang | `interviewQuestionManagement.paging.label` | - | - | Label | String | - | - | O | - | `Trang {số} / {số} · hiển thị {số} câu` | Vị trí trang hiện tại và số dòng đang hiển thị [Nguồn: 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:479]<br>[Công thức] Số trang hiện tại và tổng số trang lấy từ phần phân trang trong phản hồi của `ListInterviewQuestions`<br>[EVT liên quan] EVT-5, EVT-6 |
| | 2 | Trang trước | `interviewQuestionManagement.paging.btnPrev` | - | - | Button | - | - | - | I | - | - | Lùi về trang liền trước của kết quả lọc<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-5 |
| | 3 | Trang sau | `interviewQuestionManagement.paging.btnNext` | - | - | Button | - | - | - | I | - | - | Tiến tới trang liền sau của kết quả lọc<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-6 |
| | 4 | Chọn số dòng mỗi trang | `interviewQuestionManagement.paging.pageSize` | - | - | List | Number | 3 | - | I/O | 8 | 8 / 20 / 50 | **Mới 2026-10-05, chưa duyệt (khớp `SHR0201` Khu vực D NO 16).** Người dùng chọn số dòng mỗi trang; lựa chọn được nhớ cho lần mở sau ở localStorage khoá `algoprep-interview-questions-page-size` (prototype nhớ theo trình duyệt, chưa theo tài khoản `[SoT: Suy luận]`); đổi lựa chọn thì về trang 1; nút chọn đứng trước các mũi tên Trước/Sau, bộ chọn số dòng mỗi trang là `FilterMenu` ở chế độ `compact` (nút chỉ hiện giá trị kèm mũi tên, nhãn "Số dòng mỗi trang" giữ làm tên truy cập), `placement="top"` nên pop-up kính mở **lên phía trên** vì thanh phân trang nằm ở chân thẻ; lựa chọn 8 / 20 / 50, lựa chọn hiện tại có dấu tích (chưa duyệt hình) [Nguồn: 05-coding/frontend/src/shared/ui/data/pagination.tsx:71-82; 05-coding/frontend/src/shared/ui/data/filter-menu.tsx:28-30, 39-40, 105-109, 126] [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:50-51, 77, 439-447; 05-coding/frontend/src/shared/ui/data/pagination.tsx:71-82]<br>[Nguồn giá trị] Tham số `pageSize` của `ListInterviewQuestions`<br>[EVT liên quan] EVT-29 |

### Khu vực E — Thanh thao tác theo lô (mới 2026-10-05, chưa duyệt)

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Thanh thao tác theo lô | | | | | | | | | | | | | |
| | 1 | Nhãn số đã chọn | `interviewQuestionManagement.bulk.selectionLabel` | - | - | Label | String | - | - | O | - | `Đã chọn {số} câu hỏi` | Số câu đang được tích chọn, kể cả câu không nằm trong kết quả đang hiển thị. Nằm trong `BulkActionBar`: pop-up kính nổi cố định ở giữa mép trên màn hình, trượt vào 180 ms, chỉ gắn khi có dòng được chọn, không có nút đóng; vùng `role="status"` ẩn thị giác luôn gắn sẵn đọc số lượng [Nguồn: 05-coding/frontend/src/shared/ui/data/bulk-action-bar.tsx:3-15, 30-56; 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:394; 05-coding/frontend/messages/vi.json:1154]<br>[Công thức] Số mã đang tích chọn ở trạng thái màn hình<br>[EVT liên quan] EVT-30, EVT-31 |
| | 2 | Nhân bản theo lô | `interviewQuestionManagement.bulk.btnDuplicate` | - | - | Button | - | - | - | I | - | - | Nhân bản các câu đã chọn, từng câu như thao tác "Nhân bản" theo dòng (`DuplicateInterviewQuestion`). Bản dựng chưa có API: bấm chỉ hiện toast thông tin "chưa nối API" (khoá `toast.notWired`) [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:395-403]<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-32 |
| | 3 | Xoá theo lô | `interviewQuestionManagement.bulk.btnDelete` | - | - | Button | - | - | - | I | - | - | Mở popup xác nhận xoá theo lô (xoá mềm, F6-13); **không** xoá ngay [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:404-411]<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-33 |

### Popup

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Popup | | | | | | | | | | | | | |
| | 1 | Xác nhận xoá câu hỏi | `interviewQuestionManagement.popup.deleteConfirm` | `interview_questions` | `id`, `title` | Popup | - | - | - | I | - | Xoá câu hỏi / Huỷ | Xác nhận trước khi đặt `status = RETIRED`, nêu mã + 60 ký tự đầu câu hỏi, ghi chú giữ lịch sử phiên cũ và không hoàn tác [Nguồn: 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:262-267]<br>[Nguồn giá trị] Câu hỏi đang chọn để xoá<br>[EVT liên quan] EVT-11, EVT-12, EVT-13 |
| | 2 | Popup quản lý chủ đề | `interviewQuestionManagement.popup.topics` | `question_topics` | - | Popup | - | - | - | I | - | Đóng | Khung popup, tiêu đề "Quản lý chủ đề"; chỉ ADMIN mở được. Không có trong prototype<br>[Nguồn giá trị] Phản hồi `ListQuestionTopics`<br>[EVT liên quan] EVT-14, EVT-19 |
| | 3 | Danh sách chủ đề | `interviewQuestionManagement.popup.topics.list` | `question_topics` | `display_name`, `sort_order` | List | List | - | - | O | rỗng | - | Mỗi dòng: tên chủ đề, số câu hỏi đang tham chiếu (cả `ACTIVE` và `RETIRED`), nút lên/xuống, nút đổi tên, nút xoá. Không giới hạn số dòng<br>[Công thức] `COUNT(interview_questions WHERE topic_id = qt.id)`<br>[EVT liên quan] EVT-14 |
| | 4 | Ô tên chủ đề mới | `interviewQuestionManagement.popup.topics.newName` | `question_topics` | `display_name` | TextBox | String | 60 | - | I | rỗng | - | Tên hiển thị; không trùng tên chủ đề có sẵn (không phân biệt hoa thường). Giới hạn 60 là `[Suy luận]`, DD chốt<br>[Nguồn giá trị] Giá trị người dùng nhập<br>[EVT liên quan] EVT-15 |
| | 5 | Nút Thêm chủ đề | `interviewQuestionManagement.popup.topics.btnAdd` | `question_topics` | - | Button | - | - | - | I | - | - | Gọi `CreateQuestionTopic`; chủ đề mới xếp cuối danh sách<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-15 |
| | 6 | Nút Đổi tên | `interviewQuestionManagement.popup.topics.btnRename` | `question_topics` | `display_name` | Button | - | - | - | I | - | - | Sửa tên tại dòng, lưu bằng `UpdateQuestionTopic`. Mã `code` không đổi<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-16 |
| | 7 | Nút Lên/Xuống | `interviewQuestionManagement.popup.topics.btnReorder` | `question_topics` | `sort_order` | Button | - | - | - | I | - | - | Đổi chỗ dòng với dòng liền kề, gọi `ReorderQuestionTopics` với toàn bộ thứ tự mới<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-17 |
| | 8 | Nút Xoá chủ đề | `interviewQuestionManagement.popup.topics.btnDelete` | `question_topics` | - | Button | - | - | - | I | - | - | Gọi `DeleteQuestionTopic`. Bị từ chối nếu còn câu hỏi tham chiếu: popup hiện số câu hỏi và yêu cầu chuyển hoặc xoá các câu đó trước<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-18 |
| | 9 | Công tắc Dùng khung STAR | `interviewQuestionManagement.popup.topics.toggleStar` | `question_topics` | `uses_star_framework` | Toggle | Boolean | - | - | I | tắt | - | Bật/tắt cờ STAR của chủ đề tại dòng, lưu bằng `UpdateQuestionTopic`. Chủ đề bật cờ thì câu hỏi thuộc nó kết xuất khung trả lời chuẩn theo 4 mục STAR ở `USR0402`. Dòng `BEHAVIORAL` seed bật sẵn. Không có trong prototype **[Đợi nextjs]** (đã chốt 2026-10-01, `DEC-2026-1001-admin-configurable-settings`)<br>[Nguồn giá trị] `question_topics.uses_star_framework`<br>[EVT liên quan] EVT-20 |
| | 10 | Popup quản lý độ khó | `interviewQuestionManagement.popup.levels` | `question_levels` | - | Popup | - | - | - | I | - | Đóng | Khung popup, tiêu đề "Quản lý độ khó"; chỉ ADMIN mở được; cùng dạng popup chủ đề nhưng **không có** công tắc STAR. Không có trong prototype HTML<br>[Nguồn giá trị] Phản hồi `ListQuestionLevels`<br>[EVT liên quan] EVT-22, EVT-27 |
| | 11 | Danh sách độ khó | `interviewQuestionManagement.popup.levels.list` | `question_levels` | `display_name`, `sort_order` | List | List | - | - | O | rỗng | - | Mỗi dòng: ô tên mức, số câu hỏi đang dùng dạng "{n} câu" (cả `ACTIVE` và `RETIRED`), nút lưu tên, nút xoá. Không giới hạn số dòng<br>[Công thức] `COUNT(interview_questions WHERE level_id = ql.id)`<br>[EVT liên quan] EVT-22 |
| | 12 | Ô tên độ khó mới | `interviewQuestionManagement.popup.levels.newName` | `question_levels` | `display_name` | TextBox | String | 60 | - | I | rỗng | - | Tên hiển thị; không rỗng, không trùng mức có sẵn (không phân biệt hoa thường). Giới hạn 60 là `[Suy luận]` theo ô tên chủ đề, DD chốt<br>[Nguồn giá trị] Giá trị người dùng nhập<br>[EVT liên quan] EVT-23 |
| | 13 | Nút Thêm độ khó | `interviewQuestionManagement.popup.levels.btnAdd` | `question_levels` | - | Button | - | - | - | I | - | - | Gọi `CreateQuestionLevel`; mức mới xếp cuối danh sách, màu trung tính<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-23 |
| | 14 | Nút Lưu tên | `interviewQuestionManagement.popup.levels.btnSaveName` | `question_levels` | `display_name` | Button | - | - | - | I | - | - | Lưu tên sửa tại dòng bằng `UpdateQuestionLevel`; `code` không đổi. Gõ Enter trong ô tên cũng lưu [Nguồn: 05-coding/frontend/src/shared/ui/overlay/managed-list-dialog.tsx:141-143]<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-24 |
| | 15 | Nút Xoá độ khó | `interviewQuestionManagement.popup.levels.btnDelete` | `question_levels` | - | Button | - | - | - | I | - | - | Gọi `DeleteQuestionLevel`. **Vô hiệu hoá kèm lý do** khi mức còn câu hỏi dùng (nêu số câu) hoặc khi đây là mức cuối cùng; máy chủ kiểm lại [Nguồn: 05-coding/frontend/src/shared/ui/overlay/managed-list-dialog.tsx:130, 183; 05-coding/frontend/src/views/shared/interview-question-management/ui/level-manager-dialog.tsx:39, 51-52; 05-coding/frontend/src/shared/lib/managed-list-store.ts:116-117]<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-26 |
| | 16 | Nút Lên/Xuống độ khó | `interviewQuestionManagement.popup.levels.btnReorder` | `question_levels` | `sort_order` | Button | - | - | - | I | - | - | Đổi chỗ dòng với dòng liền kề, gọi `ReorderQuestionLevels` với toàn bộ thứ tự mới. **Bản dựng đã có** hai nút Lên/Xuống ở mỗi dòng, đổi chỗ trong kho in-memory chưa gọi máy chủ [Nguồn: 05-coding/frontend/src/shared/ui/overlay/managed-list-dialog.tsx:147-162; 05-coding/frontend/src/shared/lib/managed-list-store.ts:102-110], xem Sheet 4.5<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-25 |
| | 17 | Xác nhận xoá theo lô | `interviewQuestionManagement.popup.deleteBulkConfirm` | `interview_questions` | `id`, `title` | Popup | - | - | - | I | - | Xoá / Huỷ | **Mới 2026-10-05, chưa duyệt.** Xác nhận trước khi đặt `status = RETIRED` cho cả tập đã chọn; tiêu đề "Xoá {số} câu hỏi đã chọn?", thân dùng lại ghi chú giữ lịch sử phiên cũ của xoá một câu; khi tập chọn có câu đang bị bộ lọc hiện tại ẩn thì thêm dòng cảnh báo "Cảnh báo: {số} câu hỏi trong số này đang không hiển thị do bộ lọc hiện tại." [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:474-492; 05-coding/frontend/messages/vi.json:1159-1160]<br>[Nguồn giá trị] Danh sách đang chọn<br>[EVT liên quan] EVT-33, EVT-34, EVT-35 |

[Nguồn: 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:138-233, 259-271, 382-410, 428-433, 450, 474-479;
02-bd/database/interview-bank.md:8-132]

---

## Sheet 6. Đặc tả điều khiển item

> Ký hiệu cột `Hiển thị` và thứ tự thẻ: `02-bd/_rules/bd-template-9sheet.md` mục 2, 3. Dùng đúng NO, tên
> item và thứ tự của Sheet 5. Lỗi nhập liệu ghi ở Sheet 9, xử lý nghiệp vụ ghi ở Sheet 8.

### Khu vực A — Thanh tiêu đề

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Thanh tiêu đề | | | | |
| | 1 | Tiêu đề màn | Có | - |
| | 2 | Mô tả phụ | Có | [Điều kiện hiển thị] Trong lúc tải hiển thị khung chờ thay cho số câu. |
| | 3 | Nhập CSV | Có | [Điều kiện kích hoạt] Luôn kích hoạt, không phụ thuộc quyền hay dữ liệu (owner chốt 2026-10-01, thay cho đề xuất vô hiệu hoá của V1.1). Hành vi khi bấm chưa có, xem EVT-8. |
| | 4 | Câu hỏi mới | Có | [Điều kiện kích hoạt] Kích hoạt khi có quyền `INTERVIEW_BANK_MANAGEMENT:CREATE`. |
| | 5 | Quản lý chủ đề | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị khi vai trò là `ADMIN` và có `INTERVIEW_BANK_MANAGEMENT`; A2 không thấy.<br>[Điều kiện kích hoạt] Không kích hoạt trong lúc đang tải trang đầu. |
| | 6 | Quản lý độ khó | Điều kiện | [Điều kiện hiển thị] Cùng điều kiện với "Quản lý chủ đề": chỉ `ADMIN` có `INTERVIEW_BANK_MANAGEMENT`; A2 không thấy.<br>[Điều kiện kích hoạt] Không kích hoạt trong lúc đang tải trang đầu. |

### Khu vực B — Bộ lọc

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Bộ lọc | | | | |
| | 1 | Ô tìm kiếm | Có | [Điều kiện kích hoạt] Không kích hoạt trong lúc đang tải trang đầu.<br>[Tự động đặt] Gõ xong thì chờ một khoảng ngắn mới gọi máy chủ; mỗi lần gọi đưa về trang 1. |
| | 2 | Tab chủ đề | Có | [Tự động đặt] Đổi tab thì đưa về trang 1. |
| | 3 | Tab cấp độ | Có | [Tự động đặt] Đổi tab thì đưa về trang 1.<br>[Điều kiện hiển thị] Luôn hiển thị là nút `FilterMenu` (pop-up đóng sẵn), mọi số lượng lựa chọn; áp dụng cả cho Tab chủ đề (chưa duyệt, 2026-10-05).<br>[Điều kiện hiển thị] Số tab theo `question_levels` hiện có; nếu tab đang chọn vừa bị xoá thì quay về "Tất cả". |
| | 4 | Số kết quả | Có | [Tự động đặt] Cập nhật lại sau mỗi lần tải danh sách. |

### Khu vực C — Danh sách câu hỏi

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Danh sách câu hỏi | | | |
| | 1 | Danh sách câu hỏi (bảng) | Có | [Điều kiện hiển thị] Trong lúc tải hiển thị khung chờ đúng số dòng của một trang. Tải xong mà không có dòng nào thì hiển thị thông báo rỗng thay cho bảng — đây là kết quả lọc rỗng, **không phải** lỗi. |
| | 2 | Mã câu hỏi | Có | - |
| | 3 | Nội dung câu hỏi | Có | - |
| | 4 | Chủ đề | Có | - |
| | 5 | Nhãn cấp độ | Có | - |
| | 6 | Số câu hỏi đào sâu | Có | [Điều kiện hiển thị] Câu hỏi không có dòng đào sâu nào thì hiển thị `0`, không ẩn cột. |
| | 7 | Số tiêu chí đánh giá | Có | [Điều kiện hiển thị] Câu hỏi thiếu rubric thì hiển thị `0`, không ẩn cột — khớp nguyên tắc đã áp dụng cho Chế độ học của F6-13 [Nguồn: 01-rd/screens/shared/SHR0301_interview_question_management.md:175]. |
| | 8 | Lượt dùng | Có | - |
| | 9 | Điểm trung bình | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị số khi Q2 đã chốt nguồn dữ liệu điểm số; chưa chốt thì để ô trống, không hiển thị số 0 gây hiểu nhầm. |
| | 10 | Sửa | Có | [Điều kiện kích hoạt] Kích hoạt khi có quyền `INTERVIEW_BANK_MANAGEMENT:UPDATE`.<br>[Tự động đặt] Tooltip "Sửa" hiện bên dưới nút khi rê chuột hoặc focus bàn phím, ẩn khi rời nút. |
| | 11 | Nhân bản | Có | [Điều kiện kích hoạt] Kích hoạt khi có quyền `INTERVIEW_BANK_MANAGEMENT:CREATE`.<br>[Tự động đặt] Tooltip "Nhân bản" như trên. |
| | 12 | Xoá | Có | [Điều kiện kích hoạt] Kích hoạt khi có quyền `INTERVIEW_BANK_MANAGEMENT:DELETE`.<br>[Tự động đặt] Tooltip "Xoá" như trên. |
| | 13 | Sắp xếp theo cột | Có | [Điều kiện kích hoạt] Không kích hoạt trong lúc đang tải trang đầu.<br>[Tự động đặt] Đổi cột sắp thì đưa về trang 1; mới vào màn thì chưa sắp theo cột nào (chưa duyệt, 2026-10-05). |
| | 14 | Ô chọn dòng | Có | [Điều kiện kích hoạt] Không kích hoạt trong lúc một thao tác theo lô đang chạy (như `SHR0201`, chưa duyệt).<br>[Tự động xoá] Không xoá khi đổi trang hoặc đổi bộ lọc; chỉ xoá sau khi xoá theo lô xong, khi xoá một dòng (dòng đó rời khỏi tập chọn) hoặc khi bỏ chọn tay. |
| | 15 | Ô chọn tất cả | Có | [Điều kiện hiển thị] Luôn hiển thị ở tiêu đề cột chọn; chỉ tác động lên các dòng đang hiển thị (chưa duyệt, 2026-10-05). |

### Khu vực D — Phân trang

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Phân trang | | | | |
| | 1 | Nhãn trang | Có | [Tự động đặt] Cập nhật lại sau mỗi lần tải danh sách. |
| | 2 | Trang trước | Có | [Điều kiện kích hoạt] Không kích hoạt khi đang ở trang 1 hoặc đang tải. |
| | 3 | Trang sau | Có | [Điều kiện kích hoạt] Không kích hoạt khi đang ở trang cuối hoặc đang tải. |
| | 4 | Chọn số dòng mỗi trang | Có | [Tự động đặt] Đổi lựa chọn thì đưa về trang 1 và ghi nhớ lựa chọn mới (chưa duyệt, 2026-10-05). Sau khi xoá làm số trang giảm, trang hiện tại lùi về trang cuối. Hiển thị là nút `FilterMenu` `compact` mở pop-up lên phía trên (chưa duyệt, V1.13). |

### Khu vực E — Thanh thao tác theo lô (mới 2026-10-05, chưa duyệt)

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Thanh thao tác theo lô | | | | |
| | 1 | Nhãn số đã chọn | Điều kiện | [Điều kiện hiển thị] Cả pop-up thao tác theo lô chỉ hiển thị khi có ít nhất một dòng được chọn; nổi ở giữa mép trên màn hình, không chiếm chỗ trong luồng trang. |
| | 2 | Nhân bản theo lô | Điều kiện | [Điều kiện hiển thị] Theo pop-up thao tác theo lô.<br>[Điều kiện kích hoạt] Kích hoạt khi có quyền `INTERVIEW_BANK_MANAGEMENT:CREATE`. |
| | 3 | Xoá theo lô | Điều kiện | [Điều kiện hiển thị] Theo pop-up thao tác theo lô.<br>[Điều kiện kích hoạt] Kích hoạt khi có quyền `INTERVIEW_BANK_MANAGEMENT:DELETE`. Bấm thì mở popup xác nhận (Popup NO 17), chưa xoá ngay. |

### Popup

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Popup | | | | |
| | 1 | Xác nhận xoá câu hỏi | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị sau khi bấm nút Xoá trên một dòng. |
| | 2 | Popup quản lý chủ đề | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị sau khi ADMIN bấm "Quản lý chủ đề". |
| | 3 | Danh sách chủ đề | Có | [Điều kiện hiển thị] Trong lúc tải hiển thị khung chờ. Danh sách rỗng (ADMIN đã xoá hết) thì hiển thị thông báo rỗng kèm ô thêm chủ đề; khi đó tab chủ đề của màn chỉ còn "Tất cả". |
| | 4 | Ô tên chủ đề mới | Có | - |
| | 5 | Nút Thêm chủ đề | Có | [Điều kiện kích hoạt] Chỉ kích hoạt khi ô tên có nội dung hợp lệ. |
| | 6 | Nút Đổi tên | Có | - |
| | 7 | Nút Lên/Xuống | Có | [Điều kiện kích hoạt] Nút "Lên" không kích hoạt ở dòng đầu, nút "Xuống" không kích hoạt ở dòng cuối. |
| | 8 | Nút Xoá chủ đề | Có | [Điều kiện kích hoạt] Luôn kích hoạt; việc từ chối khi còn câu hỏi tham chiếu do máy chủ quyết, giao diện hiển thị số đếm đã có để báo trước. |
| | 9 | Công tắc Dùng khung STAR | Có | - |
| | 10 | Popup quản lý độ khó | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị sau khi ADMIN bấm "Quản lý độ khó". |
| | 11 | Danh sách độ khó | Có | [Điều kiện hiển thị] Trong lúc tải hiển thị khung chờ. Danh sách không bao giờ rỗng vì luôn còn ít nhất một mức (bất biến); nếu rỗng do lỗi dữ liệu thì hiển thị thông báo rỗng kèm ô thêm mức. |
| | 12 | Ô tên độ khó mới | Có | - |
| | 13 | Nút Thêm độ khó | Có | [Điều kiện kích hoạt] Chỉ kích hoạt khi ô tên có nội dung hợp lệ. |
| | 14 | Nút Lưu tên | Có | - |
| | 15 | Nút Xoá độ khó | Có | [Điều kiện kích hoạt] Vô hiệu hoá khi mức còn câu hỏi dùng (`questionCount` lớn hơn 0) hoặc khi chỉ còn một mức; tooltip nêu lý do (nhãn `deleteBlocked` kèm số câu, hoặc `deleteLast`; câu chữ chính xác thuộc i18n, DD chốt). Máy chủ vẫn kiểm lại. |
| | 16 | Nút Lên/Xuống độ khó | Có | [Điều kiện kích hoạt] Nút "Lên" không kích hoạt ở dòng đầu, nút "Xuống" không kích hoạt ở dòng cuối. |
| | 17 | Xác nhận xoá theo lô | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị sau khi bấm "Xoá" ở pop-up thao tác theo lô.<br>[Điều kiện kích hoạt] Nút "Xoá" luôn kích hoạt khi popup mở.<br>[Điều kiện hiển thị] Dòng cảnh báo bài bị lọc ẩn chỉ hiện khi số câu đã chọn đang bị bộ lọc ẩn lớn hơn 0 (Sheet 9 NO 19). |

[Nguồn: 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:259, 450; 01-rd/screens/shared/SHR0301_interview_question_management.md:175]

---

## Sheet 7. Danh sách truyền dữ liệu

### 7.1 Trường DTO

| NO | DTO | Trường DTO | Kiểu | Bảng DB | Cột DB | Item màn | Hiển thị | Ghi chú |
| --: | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :--- |
| 1 | `InterviewQuestionListItemDto` | `id` | UUID | `interview_questions` | `id` | Danh sách "Mã câu hỏi" | Có | [Chuyển đổi] Hiển thị theo định dạng minh hoạ `IQ-xxx` cho tới khi Q5 chốt nguồn định danh dễ đọc<br>[Đích] Tham số của `DuplicateInterviewQuestion`, `DeleteInterviewQuestion`, và tham số route sang màn sửa |
| 2 | `InterviewQuestionListItemDto` | `levelCode`, `levelDisplayName` | String, String | `question_levels` | `code`, `display_name` | Danh sách "Nhãn cấp độ" | Có | [Nguồn] Phản hồi của `ListInterviewQuestions`, join `interview_questions.level_id` (thay cho `difficulty` ENUM, đã chốt 2026-10-03). Giao diện hiển thị `levelDisplayName`, chọn màu badge theo `levelCode` (mức seed `EASY`/`MEDIUM`/`HARD` giữ màu cũ, mức khác màu trung tính) |
| 3 | `InterviewQuestionListItemDto` | `topicCode`, `topicName` | String, String | `question_topics` | `code`, `display_name` | Danh sách "Chủ đề" | Có | [Nguồn] Phản hồi của `ListInterviewQuestions`, join `interview_questions.topic_id` |
| 4 | `InterviewQuestionListItemDto` | `title` | String | `interview_questions` | `title` | Danh sách "Nội dung câu hỏi" | Có | [Nguồn] Cột `title` |
| 5 | `InterviewQuestionListItemDto` | `followUpCount` | Number | `interview_questions` | `follow_up_questions` | Danh sách "Số câu hỏi đào sâu" | Có | [Chuyển đổi] Số phần tử của mảng JSONB `follow_up_questions`; danh sách không cần nội dung từng dòng nên không trả mảng. Owner xác nhận 2026-10-01 |
| 6 | `InterviewQuestionListItemDto` | `rubricCount` | Number | `answer_rubrics` | `question_id` | Danh sách "Số tiêu chí đánh giá" | Có | [Nguồn] Phản hồi của `ListInterviewQuestions`, đếm `answer_rubrics.question_id`; `0` khi câu hỏi thiếu rubric. Owner xác nhận 2026-10-01 |
| 7 | `InterviewQuestionListItemDto` | `usageCount` | Number | `user_answers` | `question_id` | Danh sách "Lượt dùng" | Có | [Nguồn] Read model đếm `user_answers` theo `question_id` |
| 8 | `InterviewQuestionListItemDto` | `avgScore` | Number | - | - | Danh sách "Điểm trung bình" | Điều kiện | [Nguồn] **Chưa chốt** — không có trường điểm số rời rạc trong `user_answers.feedback_result_json`, xem Q2 |
| 9 | `QuestionTopicOptionDto` | `code`, `displayName`, `sortOrder`, `questionCount`, `usesStarFramework` | String, String, Number, Number, Boolean | `question_topics` | `code`, `display_name`, `sort_order`, `uses_star_framework` | Bộ lọc "Tab chủ đề"; popup "Danh sách chủ đề" | Có | [Nguồn] Phản hồi của `ListQuestionTopics`, đọc từ dữ liệu, không còn 5 dòng cố định. `questionCount` chỉ cần cho popup quản lý (ADMIN); `code` là slug, không còn enum |
| 10 | `DuplicateQuestionResultDto` | `newQuestionId` | UUID | `interview_questions` | `id` | Không hiển thị trên màn | Không | [Nguồn] Phản hồi của `DuplicateInterviewQuestion`<br>[Đích] Tham số điều hướng sang `interview_question_authoring` ở chế độ sửa |
| 11 | `QuestionTopicManageResultDto` | `topicId`, `code`, `displayName`, `sortOrder`, `usesStarFramework` | UUID, String, String, Number, Boolean | `question_topics` | `id`, `code`, `display_name`, `sort_order`, `uses_star_framework` | Popup "Danh sách chủ đề" | Có | [Nguồn] Phản hồi của `CreateQuestionTopic`, `UpdateQuestionTopic`; `ReorderQuestionTopics` trả lại toàn bộ danh sách thứ tự mới |
| 12 | `QuestionTopicDeleteRefusedDto` | `topicId`, `referencingQuestionCount` | UUID, Number | `interview_questions` | `topic_id` | Popup "Danh sách chủ đề" | Có | [Nguồn] Phản hồi lỗi của `DeleteQuestionTopic` khi còn tham chiếu; `referencingQuestionCount` hiển thị cho ADMIN |
| 13 | `QuestionLevelOptionDto` | `code`, `displayName`, `sortOrder`, `questionCount` | String, String, Number, Number | `question_levels` | `code`, `display_name`, `sort_order` | Bộ lọc "Tab cấp độ"; popup "Danh sách độ khó" | Có | [Nguồn] Phản hồi của `ListQuestionLevels`. `questionCount` chỉ cần cho popup quản lý (ADMIN), tính cả câu `RETIRED`; `code` là slug, không phải enum. Không có trường màu |
| 14 | `QuestionLevelManageResultDto` | `levelId`, `code`, `displayName`, `sortOrder` | UUID, String, String, Number | `question_levels` | `id`, `code`, `display_name`, `sort_order` | Popup "Danh sách độ khó" | Có | [Nguồn] Phản hồi của `CreateQuestionLevel`, `UpdateQuestionLevel`; `ReorderQuestionLevels` trả lại toàn bộ danh sách thứ tự mới |
| 15 | `QuestionLevelDeleteRefusedDto` | `levelId`, `reason`, `referencingQuestionCount` | UUID, String, Number | `interview_questions` | `level_id` | Popup "Danh sách độ khó" | Có | [Nguồn] Phản hồi lỗi của `DeleteQuestionLevel`; `reason` là `IN_USE` (kèm `referencingQuestionCount`) hoặc `LAST_LEVEL`. Tên giá trị `reason` là `[Suy luận]`, DD chốt |

### 7.2 Truy cập bảng dữ liệu (6)

| NO | Tên logic | Bảng | Repository | CRUD | Mục đích | Ghi chú |
| --: | :--- | :--- | :--- | :-: | :--- | :--- |
| 1 | Câu hỏi phỏng vấn | `interview_questions` | `InterviewQuestionRepository` | R, C, U | Tìm kiếm, lọc, phân trang; tạo bản sao khi nhân bản; đặt `status = 'RETIRED'` khi xoá mềm | `ListInterviewQuestions`: R<br>`DuplicateInterviewQuestion`: R, C<br>`DeleteInterviewQuestion`: R, U |
| 2 | Chủ đề câu hỏi | `question_topics` | `QuestionTopicRepository` | R, C, U, D | Đọc tên chủ đề để hiển thị và lọc; ADMIN thêm, đổi tên, sắp xếp lại, xoá | `ListInterviewQuestions`: R<br>`ListQuestionTopics`: R<br>`CreateQuestionTopic`: C<br>`UpdateQuestionTopic`: U<br>`ReorderQuestionTopics`: U<br>`DeleteQuestionTopic`: R (đếm tham chiếu), D |
| 3 | Tiêu chí đánh giá | `answer_rubrics` | `AnswerRubricRepository` | R, C | Đếm số tiêu chí cho cột "Tiêu chí" của danh sách; sao chép khi nhân bản | `ListInterviewQuestions`: R<br>`DuplicateInterviewQuestion`: R, C |
| 4 | Lượt luyện tập | `user_answers` | `UserAnswerRepository` | R | Đếm lượt dùng cho cột "Lượt dùng" của danh sách | `ListInterviewQuestions`: R |
| 5 | Nhật ký hệ thống | `system_audit_logs` | `SystemAuditLogRepository` | C | Ghi một dòng cho mỗi lần nhân bản, xoá câu hỏi, thay đổi chủ đề hoặc thay đổi độ khó | `DuplicateInterviewQuestion`: C<br>`DeleteInterviewQuestion`: C<br>`CreateQuestionTopic`, `UpdateQuestionTopic`, `ReorderQuestionTopics`, `DeleteQuestionTopic`: C<br>`CreateQuestionLevel`, `UpdateQuestionLevel`, `ReorderQuestionLevels`, `DeleteQuestionLevel`: C |
| 6 | Độ khó câu hỏi | `question_levels` | `QuestionLevelRepository` | R, C, U, D | Đọc tên mức để hiển thị và lọc; ADMIN thêm, đổi tên, sắp xếp lại, xoá | `ListInterviewQuestions`: R<br>`ListQuestionLevels`: R<br>`CreateQuestionLevel`: C<br>`UpdateQuestionLevel`: U<br>`ReorderQuestionLevels`: U<br>`DeleteQuestionLevel`: R (đếm tham chiếu, đếm số mức còn lại), D |

Không có thao tác xoá cứng trên `interview_questions` ở màn này: xoá là đặt `interview_questions.status =
'RETIRED'`, không xoá dòng, không cascade xoá `answer_rubrics`/`user_answers` liên quan
[Nguồn: 02-bd/database/interview-bank.md:65, 94-96]. Ngoại lệ duy nhất là `question_topics`: ADMIN xoá cứng một
dòng chủ đề, chỉ khi không còn `interview_questions.topic_id` nào trỏ tới (kể cả câu `RETIRED`). Tương tự với
`question_levels` (xoá cứng một dòng, chỉ khi không còn `interview_questions.level_id` trỏ tới và không phải mức cuối cùng)
[Nguồn: 02-bd/database/interview-bank.md:46-50].

`[Suy luận]` — tên repository do BD này đề xuất, DD module `interview-bank` chốt lại.

### 7.3 Danh sách endpoint [Nội bộ]

> Chỉ tên nghiệp vụ và BC sở hữu. Hợp đồng chi tiết thuộc `03-dd/api/interview-bank.md`.

| NO | Endpoint (tên nghiệp vụ) | Mục đích | BC sở hữu |
| --: | :--- | :--- | :--- |
| 1 | `ListInterviewQuestions` | Tìm kiếm, lọc và phân trang danh sách câu hỏi | `interview-bank` |
| 2 | `ListQuestionTopics` | Tải danh mục chủ đề (đọc từ dữ liệu) cho bộ lọc và popup quản lý; mọi người dùng đã xác thực gọi được | `interview-bank` |
| 3 | `DuplicateInterviewQuestion` | Tạo bản sao một câu hỏi kèm đào sâu và rubric | `interview-bank` |
| 4 | `DeleteInterviewQuestion` | Xoá mềm một câu hỏi (đặt `status = 'RETIRED'`) | `interview-bank` |
| 5 | `CreateQuestionTopic` | Thêm chủ đề mới (chỉ ADMIN); sinh `code` slug duy nhất, xếp cuối `sort_order` | `interview-bank` |
| 6 | `UpdateQuestionTopic` | Đổi `display_name` và/hoặc bật/tắt `uses_star_framework` (chỉ ADMIN); `code` giữ nguyên. Đổi tên từ `RenameQuestionTopic` ngày 2026-10-01 | `interview-bank` |
| 7 | `ReorderQuestionTopics` | Ghi lại `sort_order` theo danh sách thứ tự mới (chỉ ADMIN) | `interview-bank` |
| 8 | `DeleteQuestionTopic` | Xoá chủ đề (chỉ ADMIN); từ chối khi còn câu hỏi tham chiếu, trả `referencingQuestionCount` | `interview-bank` |
| 9 | `ListQuestionLevels` | Tải danh mục độ khó (đọc từ dữ liệu) cho bộ lọc, cột "Mức độ" và popup quản lý; mọi người dùng đã xác thực gọi được [Nguồn: 02-bd/security/interview-bank.md:76] | `interview-bank` |
| 10 | `CreateQuestionLevel` | Thêm mức độ khó mới (chỉ ADMIN); sinh `code` slug duy nhất, xếp cuối `sort_order`, không giới hạn số mức | `interview-bank` |
| 11 | `UpdateQuestionLevel` | Đổi `display_name` (chỉ ADMIN); `code` giữ nguyên | `interview-bank` |
| 12 | `ReorderQuestionLevels` | Ghi lại `sort_order` theo danh sách thứ tự mới (chỉ ADMIN) | `interview-bank` |
| 13 | `DeleteQuestionLevel` | Xoá mức độ khó (chỉ ADMIN); từ chối khi còn câu hỏi tham chiếu (trả `referencingQuestionCount`) hoặc khi là mức cuối cùng [Nguồn: 02-bd/security/interview-bank.md:80] | `interview-bank` |

Tạo/sửa nội dung câu hỏi (4 nhóm trường + rubric) thuộc endpoint của màn `interview_question_authoring`
(`SHR0302`), không liệt kê lại ở đây. Endpoint `GetInterviewQuestionBankStats` (bốn thẻ chỉ số) đã bỏ cùng dải
thẻ chỉ số ngày 2026-10-01; nếu sau này trang tổng quan Admin/Giảng viên cần các số liệu đó thì khai báo ở BD
của trang tổng quan, không thêm lại vào màn danh sách này.

[Nguồn: 02-bd/database/interview-bank.md:8-132]

---

## Sheet 8. Danh sách sự kiện

> Bộ thẻ và quy ước cột `Chuyển màn`: `02-bd/_rules/bd-template-9sheet.md` mục 2, 3.

### Màn chính: Quản lý ngân hàng câu hỏi

| NO | Loại | Sự kiện | Chi tiết | Chuyển màn | Gọi API | Tên xử lý | Ghi chú |
| --: | :--- | :--- | :--- | :-: | :-: | :--- | :--- |
| 1 | Màn hình | Khởi tạo màn | Vào màn thì tải ba nhóm dữ liệu. | Không | Có | `ListInterviewQuestions`, `ListQuestionTopics`, `ListQuestionLevels` | [Các bước]<br>1. Kiểm tra quyền `INTERVIEW_BANK_MANAGEMENT:READ`.<br>2. Hiển thị khung chờ cho danh sách.<br>3. Tải song song danh sách câu hỏi, danh mục chủ đề và danh mục độ khó.<br>[Khi thành công] Danh sách hiển thị trang 1 với bộ lọc "Tất cả".<br>[Khi lỗi] Hiển thị thông báo lỗi kèm nút thử lại tại đúng khối tải thất bại; **không** hiển thị dữ liệu cũ của khối đó. |
| 2 | Nhập liệu | Tìm kiếm theo từ khoá | Gõ vào ô tìm kiếm. | Không | Có | `ListInterviewQuestions` | [Các bước]<br>1. Chờ một khoảng ngắn sau khi ngừng gõ.<br>2. Đưa về trang 1.<br>3. Gọi lại danh sách với từ khoá mới.<br>[Khi thành công] Danh sách và nhãn số kết quả cập nhật theo từ khoá.<br>[Khi lỗi] Giữ nguyên từ khoá đã gõ, hiển thị lỗi ở vùng danh sách (trạng thái thay chỗ nội dung, giữ nguyên).<br>[Thông báo hoàn tất] Chỉ khi gửi tường minh bằng Enter thì hiện toast thông tin nêu số kết quả, hoặc "không có kết quả" khi rỗng; gõ trực tiếp để lọc **không** hiện toast [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:173-179, 370]. |
| 3 | Nút | Đổi tab chủ đề | Bấm một tab trong nhóm chủ đề. | Không | Có | `ListInterviewQuestions` | [Các bước]<br>1. Đặt tab đang chọn.<br>2. Đưa về trang 1.<br>3. Gọi lại danh sách.<br>[Khi thành công] Danh sách chỉ còn câu hỏi thuộc `topic_id` tương ứng; tab "Tất cả" bỏ điều kiện này.<br>[Khi lỗi] Giữ nguyên tab vừa chọn, hiển thị lỗi ở vùng danh sách. |
| 4 | Nút | Đổi tab cấp độ | Bấm một tab trong nhóm cấp độ. | Không | Có | `ListInterviewQuestions` | [Các bước]<br>1. Đặt tab đang chọn.<br>2. Đưa về trang 1.<br>3. Gọi lại danh sách.<br>[Khi thành công] Danh sách chỉ còn câu hỏi có `level_id` tương ứng; tab "Tất cả" bỏ điều kiện này.<br>[Khi lỗi] Giữ nguyên tab vừa chọn, hiển thị lỗi ở vùng danh sách. |
| 5 | Nút | Trang trước | Bấm "Trước" ở vùng phân trang. | Không | Có | `ListInterviewQuestions` | [Các bước]<br>1. Giảm số trang một đơn vị.<br>2. Tải trang mới.<br>[Khi thành công] Danh sách hiển thị trang liền trước, nhãn trang cập nhật.<br>[Khi lỗi] Giữ nguyên trang hiện tại, hiển thị lỗi ở vùng danh sách. |
| 6 | Nút | Trang sau | Bấm "Sau" ở vùng phân trang. | Không | Có | `ListInterviewQuestions` | [Các bước]<br>1. Tăng số trang một đơn vị.<br>2. Tải trang mới.<br>[Khi thành công] Danh sách hiển thị trang liền sau, nhãn trang cập nhật.<br>[Khi lỗi] Giữ nguyên trang hiện tại, hiển thị lỗi ở vùng danh sách. |
| 7 | Nút | Mở màn tạo câu hỏi mới | Bấm "Câu hỏi mới" ở thanh tiêu đề. | Có | Không | - | [Các bước]<br>1. Điều hướng sang màn `interview_question_authoring` ở chế độ tạo mới, route `{basePath}/new` (khu Admin `/admin/interview-questions/new`, khu Giảng viên `/instructor/interview-questions/new`).<br>[Khi thành công] Mở màn `interview_question_authoring`. |
| 8 | Nút | Bấm "Nhập CSV" | Bấm nút "Nhập CSV" ở thanh tiêu đề. | Không | Không | - | [Các bước]<br>1. Hiện tại **không có bước nào**: nút kích hoạt nhưng bản dựng chưa gắn xử lý (stub, ghi ở `06-plan/PROTOTYPE_DEBT.md` mục 16).<br>[Khi thành công] Không đổi dữ liệu, không rời màn. Bản dựng có ô chọn tệp `.csv` ẩn phía sau nút: chọn tệp thì hiện toast **thông tin** "Nhập CSV: chưa nối API trong bản mẫu, chưa có thay đổi nào" (từ 2026-10-05, trước đó là toast thành công nêu tên tệp), chưa đọc nội dung tệp [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:333-352].<br>[Thông báo hoàn tất] Toast thông tin "chưa nối API" (chỉ ở bản dựng, không báo thành công giả). Luồng nhập CSV thật chưa được thiết kế vì tính năng ngoài phạm vi bản đầu [Nguồn: 01-rd/screens/shared/SHR0301_interview_question_management.md:79-83]. |
| 9 | Nút | Mở màn sửa câu hỏi | Bấm nút biểu tượng "Sửa" ở cột Thao tác của một dòng. | Có | Không | - | [Các bước]<br>1. Điều hướng sang màn `interview_question_authoring` ở chế độ sửa, route `{basePath}/[mã]/edit`, mang theo `id` của câu hỏi.<br>[Khi thành công] Mở màn `interview_question_authoring`, nạp sẵn dữ liệu câu hỏi đó. |
| 10 | Nút | Nhân bản câu hỏi | Bấm nút biểu tượng "Nhân bản" ở cột Thao tác của một dòng. | Có | Có | `DuplicateInterviewQuestion` | [Các bước]<br>1. Kiểm tra quyền `INTERVIEW_BANK_MANAGEMENT:CREATE`.<br>2. Gọi máy chủ tạo bản sao — sao chép nội dung, câu hỏi đào sâu và toàn bộ `answer_rubrics` của bản gốc, gán `status = 'ACTIVE'`.<br>3. Ghi một dòng `system_audit_logs`.<br>4. Điều hướng sang `interview_question_authoring` ở chế độ sửa bản sao vừa tạo.<br>[Khi thành công] Danh sách ở màn danh sách được tải lại **sau khi** người dùng quay lại (không tải lại ngay vì đã rời màn).<br>[Khi lỗi] Không điều hướng, hiện toast lỗi.<br>[Thông báo hoàn tất] Toast thành công nêu mã câu hỏi vừa nhân bản; bản dựng prototype (từ 2026-10-05) **chỉ** hiện toast thông tin "Nhân bản: chưa nối API trong bản mẫu, chưa có thay đổi nào" thay cho toast thành công, chưa tạo bản sao và chưa điều hướng [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:286-291; 05-coding/frontend/messages/vi.json:1165]. |
| 11 | Nút | Mở popup xác nhận xoá | Bấm nút biểu tượng "Xoá" ở cột Thao tác của một dòng. | Không | Không | - | [Các bước]<br>1. Lấy `id` và trích nội dung câu hỏi đang chọn.<br>2. Mở popup xác nhận.<br>[Khi thành công] Popup nêu rõ mã và trích 60 ký tự đầu câu hỏi, kèm ghi chú giữ lịch sử phiên cũ và không hoàn tác. |
| 12 | Popup | Xác nhận xoá | Bấm "Xoá câu hỏi" trong popup. | Không | Có | `DeleteInterviewQuestion` | [Các bước]<br>1. Kiểm tra quyền `INTERVIEW_BANK_MANAGEMENT:DELETE`.<br>2. Đặt `status = 'RETIRED'` cho câu hỏi, không xoá dòng, không cascade xoá `answer_rubrics`/`user_answers`.<br>3. Ghi một dòng `system_audit_logs`.<br>4. Đóng popup, tải lại danh sách.<br>[Khi thành công] Dòng biến mất khỏi danh sách ngay.<br>[Khi lỗi] Đóng popup, hiện toast lỗi, giữ nguyên danh sách hiện có.<br>[Thông báo hoàn tất] Toast thành công "Đã xoá câu hỏi." (xoá một dòng cũng bỏ câu đó khỏi tập đang chọn nên số "Đã chọn n câu hỏi" không còn tính câu đã xoá, chưa duyệt, V1.12; bản dựng nêu mã câu hỏi [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:506]) |
| 13 | Nút | Huỷ trong popup xác nhận xoá | Bấm "Huỷ" hoặc đóng popup. | Không | Không | - | [Các bước]<br>1. Đóng popup.<br>[Khi thành công] Không câu hỏi nào bị đổi trạng thái. |
| 14 | Nút | Mở popup quản lý chủ đề | Bấm "Quản lý chủ đề" ở thanh tiêu đề. | Không | Có | `ListQuestionTopics` | [Các bước]<br>1. Kiểm tra vai trò `ADMIN`.<br>2. Mở popup, tải danh sách chủ đề kèm `questionCount`.<br>[Khi thành công] Popup hiển thị các dòng chủ đề theo `sort_order`.<br>[Khi lỗi] Hiển thị lỗi trong popup kèm nút thử lại (trạng thái thay chỗ nội dung danh sách, giữ nguyên). |
| 15 | Nút | Thêm chủ đề | Nhập tên và bấm "Thêm". | Không | Có | `CreateQuestionTopic` | [Các bước]<br>1. Kiểm tra tên hợp lệ và không trùng.<br>2. Gọi máy chủ tạo chủ đề, sinh `code`, xếp cuối danh sách.<br>3. Ghi một dòng `system_audit_logs`.<br>[Khi thành công] Dòng mới xuất hiện cuối danh sách, ô nhập được xoá.<br>[Khi lỗi] Giữ nguyên nội dung ô nhập, ô nhập đổi viền đỏ và hiện toast lỗi.<br>[Thông báo hoàn tất] Toast thành công [Nguồn: 05-coding/frontend/src/shared/ui/overlay/managed-list-dialog.tsx:83-92]. |
| 16 | Nút | Đổi tên chủ đề | Sửa tên tại dòng và xác nhận. | Không | Có | `UpdateQuestionTopic` | [Các bước]<br>1. Kiểm tra tên hợp lệ và không trùng.<br>2. Gọi máy chủ cập nhật `display_name`.<br>3. Ghi một dòng `system_audit_logs`.<br>[Khi thành công] Dòng hiển thị tên mới; tab chủ đề cập nhật khi đóng popup.<br>[Khi lỗi] Giữ tên cũ, ô tên của dòng đổi viền đỏ và hiện toast lỗi.<br>[Thông báo hoàn tất] Toast thành công [Nguồn: 05-coding/frontend/src/shared/ui/overlay/managed-list-dialog.tsx:94-100]. |
| 17 | Nút | Đổi thứ tự chủ đề | Bấm "Lên" hoặc "Xuống" ở một dòng. | Không | Có | `ReorderQuestionTopics` | [Các bước]<br>1. Đổi chỗ hai dòng trên giao diện.<br>2. Gửi toàn bộ thứ tự mới lên máy chủ.<br>3. Ghi một dòng `system_audit_logs`.<br>[Khi thành công] Thứ tự mới giữ nguyên.<br>[Khi lỗi] Trả lại thứ tự cũ, hiện toast lỗi. |
| 18 | Nút | Xoá chủ đề | Bấm "Xoá" ở một dòng chủ đề. | Không | Có | `DeleteQuestionTopic` | [Các bước]<br>1. Gọi máy chủ xoá chủ đề.<br>2. Máy chủ đếm câu hỏi tham chiếu; còn bất kỳ câu nào thì từ chối.<br>3. Nếu được phép xoá thì ghi một dòng `system_audit_logs`.<br>[Khi thành công] Dòng biến mất khỏi popup.<br>[Khi lỗi] Khi bị từ chối, hiển thị "Còn {số} câu hỏi thuộc chủ đề này. Hãy chuyển hoặc xoá các câu đó trước." và giữ nguyên dòng. |
| 19 | Popup | Đóng popup quản lý chủ đề | Bấm "Đóng" hoặc ra ngoài popup. | Không | Có | `ListQuestionTopics`, `ListInterviewQuestions` | [Các bước]<br>1. Đóng popup.<br>2. Tải lại danh sách chủ đề và danh sách câu hỏi để tab chủ đề và cột "Chủ đề" khớp dữ liệu mới.<br>[Khi thành công] Tab chủ đề phản ánh thay đổi; nếu tab đang chọn vừa bị xoá thì quay về "Tất cả". |
| 20 | Công tắc | Bật/tắt khung STAR của chủ đề | Bật hoặc tắt công tắc "Dùng khung STAR" ở một dòng chủ đề. | Không | Có | `UpdateQuestionTopic` | [Các bước]<br>1. Gọi máy chủ cập nhật `uses_star_framework`.<br>2. Ghi một dòng `system_audit_logs`.<br>[Khi thành công] Công tắc giữ trạng thái mới; câu hỏi thuộc chủ đề đó kết xuất khung STAR (hoặc khung tự do) ở `USR0402` từ lần tải sau.<br>[Khi lỗi] Trả công tắc về trạng thái cũ, hiện toast lỗi.<br>[Thông báo hoàn tất] Toast thành công nêu tên chủ đề và trạng thái STAR vừa đặt [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/topic-manager-dialog.tsx:44-49]. |
| 21 | Nút | Mở màn chi tiết câu hỏi | Bấm vào nội dung câu hỏi ở cột "Câu hỏi" của một dòng. | Có | Không | - | [Các bước]<br>1. Điều hướng sang màn `interview_question_info` (`SHR0303`, chỉ đọc), route `{basePath}/[mã]`, mang theo mã câu hỏi.<br>[Khi thành công] Mở màn `interview_question_info`; từ đó mới sang form soạn qua nút "Sửa câu hỏi". |
| 22 | Nút | Mở popup quản lý độ khó | Bấm "Quản lý độ khó" ở thanh tiêu đề. | Không | Có | `ListQuestionLevels` | [Các bước]<br>1. Kiểm tra vai trò `ADMIN`.<br>2. Mở popup, tải danh sách độ khó kèm `questionCount`.<br>[Khi thành công] Popup hiển thị các dòng độ khó theo `sort_order`.<br>[Khi lỗi] Hiển thị lỗi trong popup kèm nút thử lại (trạng thái thay chỗ nội dung danh sách, giữ nguyên). |
| 23 | Nút | Thêm độ khó | Nhập tên và bấm "Thêm". | Không | Có | `CreateQuestionLevel` | [Các bước]<br>1. Kiểm tra tên không rỗng và không trùng.<br>2. Gọi máy chủ tạo mức, sinh `code` (xem Q8), xếp cuối danh sách, không kiểm số lượng tối đa.<br>3. Ghi một dòng `system_audit_logs`.<br>[Khi thành công] Dòng mới xuất hiện cuối danh sách, ô nhập được xoá; mức mới hiện màu trung tính.<br>[Khi lỗi] Giữ nguyên nội dung ô nhập, ô nhập đổi viền đỏ và hiện toast lỗi (rỗng hoặc trùng).<br>[Thông báo hoàn tất] Toast thành công (`levelManager.done.add`) [Nguồn: 05-coding/frontend/src/shared/ui/overlay/managed-list-dialog.tsx:83-92; 05-coding/frontend/src/views/shared/interview-question-management/ui/level-manager-dialog.tsx:60]. |
| 24 | Nút | Đổi tên độ khó | Sửa tên tại dòng và bấm lưu (hoặc Enter). | Không | Có | `UpdateQuestionLevel` | [Các bước]<br>1. Kiểm tra tên không rỗng và không trùng.<br>2. Gọi máy chủ cập nhật `display_name`.<br>3. Ghi một dòng `system_audit_logs`.<br>[Khi thành công] Dòng hiển thị tên mới; tab cấp độ cập nhật khi đóng popup.<br>[Khi lỗi] Giữ tên cũ, ô tên của dòng đổi viền đỏ và hiện toast lỗi.<br>[Thông báo hoàn tất] Toast thành công (`levelManager.done.rename`) [Nguồn: 05-coding/frontend/src/shared/ui/overlay/managed-list-dialog.tsx:83-100; 05-coding/frontend/src/views/shared/interview-question-management/ui/level-manager-dialog.tsx:61]. |
| 25 | Nút | Đổi thứ tự độ khó | Bấm "Lên" hoặc "Xuống" ở một dòng. | Không | Có | `ReorderQuestionLevels` | [Các bước]<br>1. Đổi chỗ hai dòng trên giao diện.<br>2. Gửi toàn bộ thứ tự mới lên máy chủ.<br>3. Ghi một dòng `system_audit_logs`.<br>[Khi thành công] Thứ tự mới giữ nguyên, tab cấp độ và ô chọn ở `SHR0302` theo thứ tự này.<br>[Khi lỗi] Trả lại thứ tự cũ, hiện toast lỗi.<br>[Hiện trạng bản dựng] Đã dựng: hai nút Lên/Xuống ở mỗi dòng đổi chỗ trong kho in-memory, bỏ qua ở hai đầu, không toast; chưa gọi máy chủ và chưa có `sort_order` thật [Nguồn: 05-coding/frontend/src/shared/ui/overlay/managed-list-dialog.tsx:147-162; 05-coding/frontend/src/shared/lib/managed-list-store.ts:102-110]. |
| 26 | Nút | Xoá độ khó | Bấm "Xoá" ở một dòng độ khó. | Không | Có | `DeleteQuestionLevel` | [Các bước]<br>1. Nút đã vô hiệu hoá ở giao diện khi mức còn câu hỏi dùng hoặc là mức cuối cùng (bản dựng); nếu vẫn gọi tới máy chủ (ví dụ dữ liệu giao diện cũ):<br>2. Máy chủ đếm câu hỏi tham chiếu `level_id`; còn bất kỳ câu nào thì từ chối.<br>3. Máy chủ đếm số mức hiện có; chỉ còn một thì từ chối.<br>4. Nếu được phép xoá thì ghi một dòng `system_audit_logs`.<br>[Khi thành công] Dòng biến mất khỏi popup.<br>[Khi lỗi] Khi bị từ chối vì còn câu hỏi, hiển thị số câu đang dùng và yêu cầu chuyển hoặc xoá các câu đó trước; khi bị từ chối vì là mức cuối cùng, hiển thị "phải còn ít nhất một mức" (câu chữ chính xác do DD chốt). Giữ nguyên dòng.<br>[Thông báo hoàn tất] Toast thành công (`levelManager.done.remove`) [Nguồn: 05-coding/frontend/src/shared/ui/overlay/managed-list-dialog.tsx:183-187; 05-coding/frontend/src/views/shared/interview-question-management/ui/level-manager-dialog.tsx:62]. |
| 27 | Popup | Đóng popup quản lý độ khó | Bấm "Đóng" hoặc ra ngoài popup. | Không | Có | `ListQuestionLevels`, `ListInterviewQuestions` | [Các bước]<br>1. Đóng popup.<br>2. Tải lại danh sách độ khó và danh sách câu hỏi để tab cấp độ và cột "Mức độ" khớp dữ liệu mới.<br>[Khi thành công] Tab cấp độ phản ánh thay đổi; nếu tab đang chọn vừa bị xoá thì quay về "Tất cả". |
| 28 | Nút | Sắp xếp theo cột (mới 2026-10-05, chưa duyệt) | Bấm tiêu đề một cột dữ liệu của bảng. | Không | Có | `ListInterviewQuestions` | [Các bước]<br>1. Đặt cột và chiều sắp xếp; lần bấm đầu sắp tăng, bấm lại cùng cột thì đảo chiều (mới vào màn chưa sắp theo cột nào).<br>2. Đưa về trang 1, giữ nguyên bộ lọc.<br>3. Gọi lại danh sách.<br>[Khi thành công] Danh sách sắp theo cột và chiều đã chọn; tiêu đề cột có `aria-sort` tương ứng. Cột "Mức độ" sắp theo thứ tự danh mục độ khó, không theo tên.<br>[Khi lỗi] Giữ nguyên thứ tự trước đó, hiển thị lỗi ở vùng danh sách [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:429-436]. |
| 29 | Nút | Chọn số dòng mỗi trang (mới 2026-10-05, chưa duyệt) | Chọn 8, 20 hoặc 50 ở ô chọn trong thanh phân trang. | Không | Có | `ListInterviewQuestions` | [Các bước]<br>1. Ghi nhớ lựa chọn (localStorage khoá `algoprep-interview-questions-page-size`; prototype chưa theo tài khoản).<br>2. Đưa về trang 1.<br>3. Gọi lại danh sách với `pageSize` mới.<br>[Khi thành công] Danh sách hiển thị đúng số dòng đã chọn; lần mở sau dùng lại lựa chọn này.<br>[Khi lỗi] Giữ nguyên số dòng trước đó, hiển thị lỗi ở vùng danh sách [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:439-447]. |
| 30 | Nút | Chọn hoặc bỏ chọn một dòng (mới 2026-10-05, chưa duyệt) | Bấm ô chọn ở đầu dòng. | Không | Không | - | [Các bước]<br>1. Đảo trạng thái chọn của mã câu hỏi đó trong tập chọn của màn.<br>2. Tính lại số đang chọn, gồm cả câu không nằm trong trang hiện tại.<br>[Khi thành công] Có ít nhất một dòng được chọn thì pop-up thao tác theo lô hiện ra; bỏ chọn hết thì pop-up ẩn đi. Lựa chọn được giữ khi đổi trang hoặc bộ lọc `[SoT: Suy luận]` [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:156-163]. |
| 31 | Nút | Chọn tất cả dòng đang hiển thị (mới 2026-10-05, chưa duyệt) | Bấm ô chọn ở tiêu đề cột chọn. | Không | Không | - | [Các bước]<br>1. Chọn: thay tập chọn bằng các dòng đang hiển thị (một trang sau lọc và sắp xếp). Bỏ chọn: xoá toàn bộ tập chọn.<br>[Khi thành công] Số "Đã chọn n câu hỏi" cập nhật; chọn tất cả **không** gồm dòng ở trang khác [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:419-426]. |
| 32 | Nút | Nhân bản theo lô (mới 2026-10-05, chưa duyệt) | Bấm "Nhân bản" ở pop-up thao tác theo lô. | Không | Có | `DuplicateInterviewQuestion` | [Các bước]<br>1. Kiểm tra quyền `INTERVIEW_BANK_MANAGEMENT:CREATE`.<br>2. Với mỗi câu trong tập chọn, tạo bản sao như EVT-10 (sao chép nội dung, đào sâu, `answer_rubrics`, `status = 'ACTIVE'`), ghi một dòng `system_audit_logs` riêng; không điều hướng sang màn soạn.<br>3. Tải lại danh sách.<br>[Khi thành công] Các bản sao xuất hiện trong danh sách.<br>[Khi lỗi] Thành công một phần thì toast nêu số câu thành công, số câu thất bại kèm lý do.<br>[Thông báo hoàn tất] Toast thành công nêu số bản sao. [Hiện trạng bản dựng] Chưa gọi API: bấm chỉ hiện toast thông tin "Nhân bản: chưa nối API trong bản mẫu, chưa có thay đổi nào" (như nút theo dòng) [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:395-403; 05-coding/frontend/messages/vi.json:1165]. |
| 33 | Nút | Mở popup xác nhận xoá theo lô (mới 2026-10-05, chưa duyệt) | Bấm "Xoá" ở pop-up thao tác theo lô. | Không | Không | - | [Các bước]<br>1. Tổng hợp danh sách câu đang chọn và đếm số câu bị bộ lọc hiện tại ẩn.<br>2. Mở popup xác nhận.<br>[Khi thành công] Popup nêu số câu, ghi chú giữ lịch sử phiên cũ, và dòng cảnh báo nếu có câu bị lọc ẩn [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:150-154, 404-411, 474-492]. |
| 34 | Popup | Xác nhận xoá theo lô (mới 2026-10-05, chưa duyệt) | Bấm "Xoá" trong popup xoá theo lô. | Không | Có | `DeleteInterviewQuestion` | [Các bước]<br>1. Kiểm tra quyền `INTERVIEW_BANK_MANAGEMENT:DELETE`.<br>2. Với từng câu trong tập, đặt `status = 'RETIRED'`, không xoá dòng, không cascade (như EVT-12); ghi một dòng `system_audit_logs` riêng cho mỗi câu.<br>3. Đóng popup, bỏ lựa chọn, tải lại danh sách.<br>[Khi thành công] Các câu biến mất khỏi danh sách; nếu trang hiện tại vượt trang cuối mới thì lùi về trang cuối.<br>[Khi lỗi] Thành công một phần thì vẫn đóng popup và toast nêu số câu thành công, số câu thất bại kèm lý do.<br>[Thông báo hoàn tất] Toast thành công "Đã xoá {số} câu hỏi" [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/interview-question-management-view.tsx:165-170, 474-492; 05-coding/frontend/messages/vi.json:1164]. |
| 35 | Nút | Huỷ popup xoá theo lô (mới 2026-10-05, chưa duyệt) | Bấm "Huỷ" hoặc đóng popup. | Không | Không | - | [Các bước]<br>1. Đóng popup.<br>[Khi thành công] Không câu hỏi nào bị đổi, tập đang chọn giữ nguyên. |

[Nguồn: 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:148-149, 165-181, 226-228, 259-271;
01-rd/screens/shared/SHR0301_interview_question_management.md:79-83, 121-125]

---

## Sheet 9. Đặc tả kiểm tra

> Bộ thẻ và quy ước mã thông báo: `02-bd/_rules/bd-template-9sheet.md` mục 2, 4. Sheet này ghi **điều
> kiện kiểm và thông báo**; tiêu chí nghiệm thu `AC-nn` thuộc `04-tdd/interview_question_management.md`,
> không lặp lại ở đây.

| NO | Loại | Tóm tắt kiểm | Chi tiết | Mức | Mã thông báo | Ghi chú | EVT gọi | Thứ tự |
| --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: |
| 1 | Kiểm quyền | Quyền xem màn | [Nội dung kiểm] Người dùng không có `INTERVIEW_BANK_MANAGEMENT:READ` thì không được vào màn.<br>[Nơi thực thi] Chặn ở cả tầng định tuyến phía giao diện và tầng phân quyền phía máy chủ [Nguồn: 01-rd/req/identity.md:60]. | Lỗi | Chưa có mã thông báo | Nội dung "Bạn không có quyền truy cập chức năng này." | EVT-1 | 1 |
| 2 | Kiểm quyền | Quyền nhân bản | [Nội dung kiểm] Nhân bản câu hỏi yêu cầu `INTERVIEW_BANK_MANAGEMENT:CREATE`.<br>[Nơi thực thi] Máy chủ, kiểm lại kể cả khi giao diện đã ẩn nút.<br>[Tiêu điểm] Toast; nút "Nhân bản". | Lỗi | Chưa có mã thông báo | Nội dung "Bạn không có quyền thực hiện thao tác này." | EVT-10 | 1 |
| 3 | Kiểm quyền | Quyền xoá | [Nội dung kiểm] Xoá câu hỏi yêu cầu `INTERVIEW_BANK_MANAGEMENT:DELETE`.<br>[Nơi thực thi] Máy chủ.<br>[Tiêu điểm] Toast; popup xác nhận xoá. | Lỗi | Chưa có mã thông báo | Nội dung "Bạn không có quyền thực hiện thao tác này." | EVT-12 | 1 |
| 4 | Kiểm nhập liệu | Độ dài từ khoá tìm kiếm | [Nội dung kiểm] Từ khoá dài quá 200 ký tự thì không gửi lên máy chủ.<br>[Nơi thực thi] Màn hình.<br>[Tiêu điểm] Viền ô + toast; ô tìm kiếm. | Lỗi | Chưa có mã thông báo | Nội dung "Từ khoá tìm kiếm tối đa 200 ký tự." Giới hạn 200 là `[Suy luận]` — nội dung câu hỏi dài hơn tên/email nên nới hơn giới hạn 100 ký tự đã dùng ở `admin_user_management`; DD chốt số chính xác. | EVT-2 | 1 |
| 5 | Kiểm nghiệp vụ | Tồn tại câu hỏi trước khi thao tác | [Nội dung kiểm] `id` gửi lên không khớp câu hỏi đang tồn tại hoặc đã `RETIRED` thì từ chối nhân bản/xoá.<br>[Nơi thực thi] Máy chủ. | Lỗi | Chưa có mã thông báo | Nội dung "Không tìm thấy câu hỏi hoặc câu hỏi đã bị xoá." | EVT-10, EVT-12 | 2 |
| 6 | Kiểm nghiệp vụ | Nhân bản sao chép trọn vẹn | [Nội dung kiểm] Bản sao phải giữ nguyên `topic_id`, `level_id`, `title`, `content_markdown`, `follow_up_questions` và toàn bộ dòng `answer_rubrics` của bản gốc, gán `status = 'ACTIVE'` và `id` mới.<br>[Nơi thực thi] Máy chủ, trong cùng giao dịch. | Lỗi | Chưa có mã thông báo | Ghi thất bại một phần (ví dụ sao `answer_rubrics` lỗi giữa chừng) thì huỷ toàn bộ giao dịch, không tạo bản sao thiếu tiêu chí. Hành vi chi tiết của "nhân bản" là một đề xuất chưa chốt, xem Q4. | EVT-10 | 3 |
| 7 | Kiểm nghiệp vụ | Xoá mềm không cascade | [Nội dung kiểm] Xoá chỉ đặt `status = 'RETIRED'`, không xoá `answer_rubrics`/`user_answers` liên quan, không xoá dòng `interview_questions`.<br>[Nơi thực thi] Máy chủ. | Lỗi | Chưa có mã thông báo | Vi phạm nguyên tắc xoá mềm đã chốt [Nguồn: 02-bd/database/interview-bank.md:65, 94-96] là lỗi thiết kế, chặn ở code review, không chỉ ở tài liệu. | EVT-12 | 2 |
| 8 | Kiểm nghiệp vụ | Ghi nhật ký không ngoại lệ | [Nội dung kiểm] Mỗi lần nhân bản hoặc xoá phải ghi đúng một dòng `system_audit_logs`.<br>[Nơi thực thi] Máy chủ, trong cùng giao dịch với thao tác ghi. | Lỗi | Chưa có mã thông báo | Ghi nhật ký thất bại thì thao tác coi như thất bại, không áp dụng — theo F1-14 [Nguồn: 01-rd/screens/shared/SHR0301_interview_question_management.md:121-125]. | EVT-10, EVT-12 | 4 |
| 9 | Kiểm nghiệp vụ | Lỗi hệ thống hoặc lỗi gọi máy chủ | [Nội dung kiểm] Gọi máy chủ thất bại hoặc trả lỗi nghiệp vụ thì dừng thao tác, không hiển thị dữ liệu cũ của khối lỗi.<br>[Nơi thực thi] Màn hình. | Lỗi | Mã lỗi trong phản hồi | Phản hồi có mã lỗi đã đăng ký thì nêu nội dung tương ứng; chưa đăng ký thì nêu "Không kết nối được máy chủ." — với thao tác ghi thì nội dung đi qua toast lỗi, với lỗi tải một khối dữ liệu thì hiển thị ở khối đó kèm nút thử lại. | EVT-1, EVT-2, EVT-3, EVT-4, EVT-5, EVT-6, EVT-10, EVT-12, EVT-14, EVT-15, EVT-16, EVT-17, EVT-18, EVT-19, EVT-20, EVT-22, EVT-23, EVT-24, EVT-25, EVT-26, EVT-27 | 1 |
| 10 | Kiểm quyền | Quyền quản lý chủ đề | [Nội dung kiểm] Thêm, đổi tên, sắp xếp, xoá chủ đề chỉ cho vai trò `ADMIN` có `INTERVIEW_BANK_MANAGEMENT`; A2 bị từ chối kể cả khi gọi thẳng API.<br>[Nơi thực thi] Máy chủ. | Lỗi | Chưa có mã thông báo | Nội dung "Bạn không có quyền thực hiện thao tác này." | EVT-14, EVT-15, EVT-16, EVT-17, EVT-18, EVT-20 | 1 |
| 11 | Kiểm nhập liệu | Tên chủ đề | [Nội dung kiểm] Tên không rỗng sau khi cắt khoảng trắng, tối đa 60 ký tự, không trùng tên chủ đề khác (không phân biệt hoa thường).<br>[Nơi thực thi] Màn hình và máy chủ. | Lỗi | Chưa có mã thông báo | Nội dung "Tên chủ đề không hợp lệ hoặc đã tồn tại." Giới hạn 60 là `[Suy luận]`, DD chốt. | EVT-15, EVT-16 | 2 |
| 12 | Kiểm nghiệp vụ | Xoá chủ đề còn được tham chiếu | [Nội dung kiểm] Từ chối xoá khi tồn tại `interview_questions.topic_id` trỏ tới chủ đề, tính cả câu `RETIRED`; phản hồi kèm `referencingQuestionCount`.<br>[Nơi thực thi] Máy chủ; khoá ngoại không `ON DELETE CASCADE` là lưới an toàn thứ hai. | Lỗi | Chưa có mã thông báo | Nội dung "Còn {số} câu hỏi thuộc chủ đề này. Hãy chuyển hoặc xoá các câu đó trước." | EVT-18 | 2 |
| 13 | Kiểm nghiệp vụ | Ghi nhật ký thay đổi chủ đề | [Nội dung kiểm] Mỗi lần thêm, đổi tên, bật/tắt khung STAR, sắp xếp, xoá chủ đề thành công ghi đúng một dòng `system_audit_logs`, cùng giao dịch.<br>[Nơi thực thi] Máy chủ. | Lỗi | Chưa có mã thông báo | Ghi nhật ký thất bại thì thao tác coi như thất bại, theo F1-14. | EVT-15, EVT-16, EVT-17, EVT-18, EVT-20 | 4 |
| 14 | Kiểm quyền | Quyền quản lý độ khó | [Nội dung kiểm] Thêm, đổi tên, sắp xếp, xoá độ khó chỉ cho vai trò `ADMIN` có `INTERVIEW_BANK_MANAGEMENT` (`CREATE`/`UPDATE`/`DELETE` theo thao tác); A2 bị từ chối kể cả khi gọi thẳng API.<br>[Nơi thực thi] Máy chủ [Nguồn: 02-bd/security/interview-bank.md:72-80]. | Lỗi | Chưa có mã thông báo | Nội dung "Bạn không có quyền thực hiện thao tác này." | EVT-22, EVT-23, EVT-24, EVT-25, EVT-26 | 1 |
| 15 | Kiểm nhập liệu | Tên độ khó | [Nội dung kiểm] Tên không rỗng sau khi cắt khoảng trắng, tối đa 60 ký tự, không trùng tên mức khác (không phân biệt hoa thường).<br>[Nơi thực thi] Màn hình và máy chủ (`display_name` duy nhất [Nguồn: 02-bd/database/interview-bank.md:40]).<br>[Tiêu điểm] Viền ô + toast; ô tên mức mới hoặc ô tên của dòng. | Lỗi | Chưa có mã thông báo | Hai thông báo tách theo bản dựng: tên rỗng (`levelManager.error.empty`) và tên trùng (`levelManager.error.duplicate`) [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/level-manager-dialog.tsx:55-58]; câu chữ do i18n/DD chốt. Giới hạn 60 là `[Suy luận]`, DD chốt. | EVT-23, EVT-24 | 2 |
| 16 | Kiểm nghiệp vụ | Xoá độ khó còn được tham chiếu | [Nội dung kiểm] Từ chối xoá khi tồn tại `interview_questions.level_id` trỏ tới mức, tính cả câu `RETIRED`; phản hồi kèm `referencingQuestionCount`.<br>[Nơi thực thi] Máy chủ; FK không `ON DELETE CASCADE` là lưới an toàn thứ hai [Nguồn: 02-bd/database/interview-bank.md:47-50]. | Lỗi | Chưa có mã thông báo | Nội dung nêu số câu đang dùng mức và yêu cầu chuyển hoặc xoá các câu đó trước; câu chữ do DD chốt. Giao diện đã vô hiệu hoá nút xoá để báo trước (`deleteBlocked`). | EVT-26 | 2 |
| 17 | Kiểm nghiệp vụ | Không xoá mức độ khó cuối cùng | [Nội dung kiểm] Từ chối xoá khi `question_levels` chỉ còn một dòng, vì form soạn câu hỏi bắt buộc chọn độ khó.<br>[Nơi thực thi] Máy chủ, ở tầng use case (SQL không có ràng buộc "tối thiểu một dòng") [Nguồn: 02-bd/database/interview-bank.md:48-50]. Giao diện vô hiệu hoá nút xoá khi chỉ còn một mức (`keepAtLeast={1}`) [Nguồn: 05-coding/frontend/src/views/shared/interview-question-management/ui/level-manager-dialog.tsx:39]; kho cũng tự từ chối: `remove` trả `false` khi số mục `<= minItems`, `level-store.ts` đặt `minItems: 1` [Nguồn: 05-coding/frontend/src/shared/lib/managed-list-store.ts:116-117; 05-coding/frontend/src/entities/interview-question/model/level-store.ts:32]. | Lỗi | Chưa có mã thông báo | Nội dung nêu phải còn ít nhất một mức (`deleteLast`); câu chữ do DD chốt. | EVT-26 | 3 |
| 18 | Kiểm nghiệp vụ | Ghi nhật ký thay đổi độ khó | [Nội dung kiểm] Mỗi lần thêm, đổi tên, sắp xếp, xoá độ khó thành công ghi đúng một dòng `system_audit_logs`, cùng giao dịch.<br>[Nơi thực thi] Máy chủ. | Lỗi | Chưa có mã thông báo | Ghi nhật ký thất bại thì thao tác coi như thất bại, theo F1-14. | EVT-23, EVT-24, EVT-25, EVT-26 | 4 |
| 19 | Kiểm nghiệp vụ | Cảnh báo xoá theo lô trúng câu hỏi đang bị bộ lọc ẩn (mới, chưa duyệt) | [Nội dung kiểm] Khi mở popup xoá theo lô, đếm số mã đã chọn không có trong kết quả đã lọc (không chỉ trang hiện tại); lớn hơn 0 thì hiện dòng cảnh báo.<br>[Nơi thực thi] Màn hình.<br>[Tiêu điểm] Popup xác nhận xoá theo lô, dưới ghi chú giữ lịch sử phiên cũ. | Cảnh báo | Chưa có mã thông báo | Nội dung "Cảnh báo: {số} câu hỏi trong số này đang không hiển thị do bộ lọc hiện tại." Chỉ báo trước, **không chặn** nút "Xoá" (cùng cách `SHR0201` Sheet 9 NO 20; nếu owner muốn chặn cứng hoặc bỏ lựa chọn khi đổi bộ lọc thì đổi ở cả hai màn). | EVT-33, EVT-34 | 1 |

Cột `Thứ tự` là thứ tự kiểm trong cùng một sự kiện.

[Nguồn: 02-bd/database/interview-bank.md:65, 94-96; 01-rd/req/identity.md:60;
01-rd/screens/shared/SHR0301_interview_question_management.md:121-125]

---

## Câu hỏi mở

| # | Câu hỏi | Vì sao chưa trả lời được | Chủ sở hữu |
| :-: | :--- | :--- | :--- |
| Q1 | ~~Con số biến động của các thẻ chỉ số ("+9", "−0,2") tính theo mốc so sánh nào?~~ **ĐÃ ĐÓNG 2026-10-01:** dải bốn thẻ chỉ số đã bỏ khỏi màn danh sách (chỉ giữ một trang tổng quan Admin/Giảng viên), không còn con số biến động nào để định nghĩa mốc. Nếu trang tổng quan cần lại thì mở câu hỏi ở BD của trang đó. | Đã đóng. | Đã đóng |
| Q2 | "Điểm trung bình" (cột "Điểm TB" của danh sách; thẻ chỉ số cùng tên đã bỏ 2026-10-01) lấy từ đâu? Schema `interview-bank` chỉ có `user_answers.feedback_result_json` — phản hồi định tính (điểm đã đạt/còn thiếu), **không có** trường điểm số 1-5 rời rạc [Nguồn: 02-bd/database/interview-bank.md:124]. | Đây là phát hiện mới của V1.0, không có trong bản BD cũ (bản cũ không đặc tả tới mức trường dữ liệu). Ba hướng khả dĩ: (1) thêm một trường điểm số rời rạc vào `feedback_result_json` do AI tự chấm kèm phản hồi định tính, (2) suy ra điểm số từ tỉ lệ tiêu chí đạt trong `feedback_result_json` bằng công thức tầng ứng dụng, (3) bỏ hẳn hai chỉ số này khỏi phạm vi bản đầu. Ba phương án khác nhau về chi phí migration và độ chính xác, BD này **không tự chọn**. | BD `database/interview-bank.md` + Chủ dự án |
| Q3 | ~~Nút "Nhập CSV" hiển thị dạng vô hiệu hoá (disabled + tooltip) hay ẩn hẳn khỏi màn cho tới khi triển khai?~~ **ĐÃ ĐÓNG 2026-10-01 (owner):** nút giữ kích hoạt; bấm nút chưa làm gì (stub). Đề xuất vô hiệu hoá kèm tooltip "Sắp ra mắt" của V1.1 bị bỏ. | Đã đóng. | Đã đóng |
| Q4 | "Nhân bản" tạo bản ghi ngay ở máy chủ hay chỉ điền sẵn form tạo mới chưa lưu? | Prototype chỉ có nút, không có luồng chi tiết [Nguồn: 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:227]. `[SoT: Suy luận]` — BD này đề xuất tạo bản ghi mới ngay ở máy chủ (`id` mới, `status = ACTIVE`, sao chép toàn bộ đào sâu + rubric) rồi mở `interview_question_authoring` ở chế độ sửa bản ghi vừa tạo — tránh trạng thái "nháp chưa lưu" phức tạp thêm cho form vốn đã nặng (4 nhóm trường), kế thừa đúng đề xuất của bản BD cũ. Cần chốt ở DD trước khi viết `03-dd/api/interview-bank.md`. | Chủ dự án + DD `interview-bank` |
| Q5 | Mã câu hỏi hiển thị dạng `IQ-014` — schema chỉ có `id` UUID, không có cột định danh dễ đọc riêng [Nguồn: 02-bd/database/interview-bank.md:56]. Sinh mã hiển thị từ đâu? | Prototype dùng dữ liệu mẫu cứng dạng chuỗi, không phải giá trị tính từ `id` [Nguồn: 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:383-409]. Hai hướng: (1) thêm cột `display_code` tự tăng kèm migration và backfill dữ liệu cũ, (2) suy ra một mã ngắn từ `id` (ví dụ 6 ký tự đầu của UUID viết hoa) mà không cần migration nhưng không liên tục/không dễ nhớ như prototype gợi ý. Chi phí và trải nghiệm khác nhau rõ rệt, BD **không tự chọn**. | BD `database/interview-bank.md` + Chủ dự án |
| Q6 | Nội dung thông báo lỗi/trống cụ thể (kết quả lọc rỗng, lỗi tải danh sách) — RD/prototype không đặc tả câu chữ. | Để DD chốt theo chuẩn thông báo chung của hệ thống, cùng cách xử lý đã áp dụng ở các BD màn khác (ví dụ `ADM0201_user_management.md` Sheet 9 NO 12). | DD `interview-bank` |
| Q7 | ~~Quyền quản lý chủ đề: dùng Function `INTERVIEW_BANK_MANAGEMENT` kết hợp kiểm vai trò `ADMIN` (đề xuất của BD) hay tách một Function riêng?~~ **ĐÃ ĐÓNG 2026-10-01 (owner uỷ quyền), xem `DEC-2026-1001-admin-configurable-settings`:** dùng lại `INTERVIEW_BANK_MANAGEMENT` cộng kiểm vai trò `ADMIN`, không Function mới. Dấu `[SoT: Suy luận]` giữ nguyên vì chưa dẫn được ma trận quyền thực tế. | Quyết định 2026-10-01 chỉ nói "dùng lại quyền quản lý ngân hàng câu hỏi cho A3". `01-rd/req/identity.md:60` chỉ liệt kê `INTERVIEW_BANK_MANAGEMENT`, không có Function riêng cho chủ đề. `[SoT: Suy luận]` — kiểm vai trò thêm vào Function sẵn có tránh phải sửa ma trận F1-12; nếu muốn ADMIN gán quyền chủ đề cho từng tài khoản thì mới cần Function mới. | Chủ dự án + BD `security/interview-bank.md` |
| Q8 | ~~`code` của độ khó ADMIN thêm mới sinh thế nào (slug từ `display_name`, xử lý dấu tiếng Việt, đụng mã thì thêm hậu tố)?~~ **ĐÃ DỰNG TRONG BẢN DỰNG 2026-10-03, DD xác nhận lại:** bản dựng cài đúng quy tắc của BD (viết hoa, gạch dưới, hậu tố số khi trùng): `slugify` bỏ dấu và đổi `đ` thành `d`, ký tự không phải chữ-số thành `_`, cắt `_` đầu cuối; trùng thì `_2`, `_3` [Nguồn: 05-coding/frontend/src/shared/lib/managed-list-store.ts:30-39, 54-60; 05-coding/frontend/src/entities/interview-question/model/level-store.test.ts:32-44; .nexa/control/decision-registry.md:2454-2456]. Trạng thái: đóng cho prototype; thuật toán chính xác (tên chỉ gồm ký tự đặc biệt thì khoá là gì, giới hạn độ dài khoá, sinh phía máy chủ) do DD `interview-bank` xác nhận lại. | BD chốt `code` là slug duy nhất, sinh một lần, cùng quy tắc với `question_topics.code`, cách sinh chốt ở DD [Nguồn: 02-bd/database/interview-bank.md:39]. Khoá bản dựng vẫn chỉ nằm trong bộ nhớ; kho chủ đề câu hỏi chưa bật `slugKeys` (khoá seed dạng `csTheory`, khoá mới theo thời gian) [Nguồn: 05-coding/frontend/src/entities/interview-question/model/topic-store.ts:28-37], nên quy tắc cần đồng bộ cho chủ đề khi viết `03-dd/api/interview-bank.md`. | DD `interview-bank` |
| Q9 | ~~Màu badge của độ khó có cho ADMIN chọn không?~~ **ĐÃ ĐÓNG 2026-10-03 (owner):** không có cột màu; ba mức seed giữ màu cũ ở giao diện, mức ADMIN thêm hiện màu trung tính [Nguồn: 02-bd/database/interview-bank.md:43-44; .nexa/control/decision-registry.md:2448-2449]. Nếu sau này cần chọn màu thì thêm cột `color_token` kèm danh sách giá trị cho phép. | Đã đóng. | Đã đóng |
| Q10 | **Các chi tiết prototype 2026-10-05 chưa được owner xem, cần xác nhận:** (a) sắp xếp mọi cột và việc mới vào màn chưa sắp cột nào (thay vì mặc định theo mã hay theo lượt dùng); (b) lưu cỡ trang theo trình duyệt (prototype) hay theo tài khoản (như `SHR0201` Q4); (c) đổi màu muted cho tiêu đề cột `DataTable` và đổi `--color-success-text` khu Admin và Giảng viên (xuyên màn, ghi ở `06-plan/PROTOTYPE_DEBT.md` mục 25); (d) `FilterMenu` có nên thay luôn các dải nút còn giữ có chủ ý ở màn khác (xem `SHR0201` Q6 (c)) không; chưa có trả lời; (e) bộ hai thao tác theo lô "Nhân bản" và "Xoá" (do bên phát triển chọn từ các thao tác theo dòng, chưa có trong RD F6-13) có đúng bộ cần không, và "Nhân bản" theo lô nên làm thật hay giữ là bản mẫu chưa nối API; chưa có trả lời. (Câu hỏi về ngưỡng 6 lựa chọn đã bỏ vì không còn ngưỡng.) | Prototype là bản mẫu một phía, chưa qua duyệt giao diện; các điểm này đổi theo ý owner mà không ảnh hưởng schema hay API. | Chủ dự án |

---

## Tham chiếu

- `01-rd/screens/shared/SHR0301_interview_question_management.md` — RD màn hình.
- `09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html` — bằng chứng layout (504 dòng).
- `02-bd/architecture/interview-bank.md`, `02-bd/database/interview-bank.md`,
  `02-bd/security/interview-bank.md` — BD module `interview-bank`.
- `02-bd/screens/admin/ADM0201_user_management.md` — mẫu cấu trúc BD 9 sheet đã dùng trước đó, cùng dạng
  màn danh sách/quản lý.
- `02-bd/_rules/bd-template-9sheet.md` — quy ước 9 sheet, bộ thẻ đóng, quy ước ID, checklist kiểm toán.
- `.nexa/control/decision-registry.md` — `DEC-2026-0825-shared-content-authoring-screens`,
  `DEC-2026-0828-remove-per-class-interview-set`, `DEC-2026-0830-interview-bank-crud`,
  `DEC-2026-0831-outside-screens-closures`.
- `01-rd/screens/shared/SHR0302_interview_question_authoring.md` — màn soạn/sửa câu hỏi (`SHR0302`), liên kết từ
  đây, không lặp lại đặc tả.
- `02-bd/screens/shared/SHR0303_interview_question_info.md` — màn chi tiết chỉ đọc (`SHR0303`), đích của liên kết nội dung câu hỏi từ 2026-10-02.
