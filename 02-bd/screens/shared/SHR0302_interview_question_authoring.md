# Tài liệu thiết kế cơ bản (BD) — Biên soạn câu hỏi phỏng vấn (`SHR0302`)

## Phương châm tài liệu [Nội bộ — không cần khách duyệt]

- Viết theo **mẫu 9 sheet**. Bộ thẻ đóng, quy ước ID, ký hiệu chuẩn, ranh giới với tài liệu khác và
  checklist kiểm toán nằm ở `02-bd/_rules/bd-template-9sheet.md` — file này không chép lại.
- Phần gắn nhãn `[Nội bộ]` phục vụ sinh mã và tự kiểm toán, không phải yêu cầu của khách hàng: mục 4.5,
  mục 7.3 và các ghi chú quy ước.
- Mã màn `SHR0302` theo bảng mã ở `02-bd/_rules/bd-template-9sheet.md` mục 8; tên file mang tiền tố mã.
  Màn dùng chung A2 (`INSTRUCTOR`) / A3 (`ADMIN`), không có phạm vi riêng theo lớp
  [Nguồn: 01-rd/screens/shared/SHR0302_interview_question_authoring.md:5, 19-23].
- Màn này **chưa có prototype** ở `09-layoutBase/` (`[Đợi nextjs]`), route chưa dựng mockup trung gian
  [Nguồn: 01-rd/screens/shared/SHR0302_interview_question_authoring.md:25, 78-79]. Đã có một bản dựng UI thật ở
  `05-coding/frontend/src/views/shared/interview-question-authoring/`, tự nhận là "PROTOTYPE — no DD yet"
  [Nguồn: 05-coding/frontend/src/views/shared/interview-question-authoring/ui/interview-question-authoring-view.tsx:1].
  BD này dùng bản dựng đó làm bằng chứng cấu trúc bổ sung khi RD không đủ chi tiết, ghi rõ từng chỗ dùng.
- Màn này không có màn con, chỉ có 2 popup: Xem như học viên, Xác nhận xoá mềm.
- Route theo khu vực (khu Admin từ 2026-10-02, khu Giảng viên theo sau 2026-10-03, `DEC-2026-1002-split-detail-and-edit-pages`): tạo mới tại
  `/admin/interview-questions/new` và `/instructor/interview-questions/new`, sửa tại `/admin/interview-questions/[id]/edit` và
  `/instructor/interview-questions/[id]/edit`; `/admin/interview-questions/[id]` và `/instructor/interview-questions/[id]`
  là màn chỉ đọc `interview_question_info` (`SHR0303`). View nhận `listHref` (đường dẫn danh sách của khu đang mount), không cố định `/admin`
  [Nguồn: 01-rd/screens/shared/SHR0302_interview_question_authoring.md:26-31;
  05-coding/frontend/src/app/(admin)/admin/interview-questions/[questionId]/edit/page.tsx:1-11;
  05-coding/frontend/src/app/(admin)/admin/interview-questions/new/page.tsx:1-7;
  05-coding/frontend/src/app/(instructor)/instructor/interview-questions/[questionId]/edit/page.tsx:1-11;
  05-coding/frontend/src/app/(instructor)/instructor/interview-questions/new/page.tsx:1-10;
  05-coding/frontend/src/views/shared/interview-question-authoring/ui/interview-question-authoring-view.tsx:67-72].

> Đọc cùng `01-rd/screens/shared/SHR0302_interview_question_authoring.md` (hành vi ở mức yêu cầu, không lặp lại ở
> đây), `01-rd/screens/shared/SHR0301_interview_question_management.md` (màn cha), và ba file BD module:
> `02-bd/architecture/interview-bank.md`, `02-bd/database/interview-bank.md`, `02-bd/security/interview-bank.md`.
>
> **Không thiết kế** vòng đời nháp/xuất bản riêng như `problem_authoring` — câu hỏi hiện ngay cho học viên
> khi lưu, trừ khi bị ẩn khỏi Chế độ luyện do thiếu tiêu chí
> [Nguồn: 01-rd/screens/shared/SHR0302_interview_question_authoring.md:56-58, 119].
> **Không thiết kế** khái niệm "bộ câu hỏi" (`question_sets`) — đã đóng, bỏ hẳn 2026-09-13
> [Nguồn: 02-bd/database/interview-bank.md:179-185].

---

## Sheet 1. Bìa

| Mục | Giá trị |
| :--- | :--- |
| Hệ thống | AlgoPrep |
| Phân hệ | `interview-bank` (F6) |
| Công đoạn | BD — Thiết kế cơ bản |
| Phân loại | Màn hình |
| Tên màn | Biên soạn câu hỏi phỏng vấn |
| Mã màn hình | `SHR0302` |
| Tên vật lý (slug) | `interview_question_authoring` |
| Trục tài liệu | Màn hình (`02-bd/screens/`) |
| Actor | A2 (`INSTRUCTOR`) / A3 (`ADMIN`) |
| Phiên bản | V0.11 |
| Người tạo | Nhóm phát triển AlgoPrep |
| Ngày tạo | 2026/09/20 |
| Người cập nhật | Nhóm phát triển AlgoPrep |
| Ngày cập nhật | 2026/10/03 |

---

## Sheet 2. Lịch sử sửa đổi

| Ver | Sheet bị sửa | Nội dung sửa | Ngày | Người sửa |
| :--- | :--- | :--- | :--- | :--- |
| V0.1 | Toàn bộ | Tạo mới theo mẫu 9 sheet, thay thế bản BD văn xuôi 7 mục cũ (layout regions, component inventory, screen states, API tiêu thụ, navigation, access rights, câu hỏi mở). Đối chiếu bản dựng UI thật ở `05-coding/frontend` để chốt vị trí khối Nhân bản/Xoá mềm và bước nhảy trọng số rubric; phát sinh câu hỏi mở mới về ba trường F6-04/05/06 chưa có nhóm biên soạn nào phụ trách | 2026/09/20 | Nhóm phát triển AlgoPrep |
| V0.2 | Sheet 3, 4, 5, 6, 8 | Đồng bộ với bản dựng UI ngày 2026-10-01: (1) bố cục đổi từ một cột dọc tối đa 860px sang **lưới hai cột rộng toàn trang** — cột rộng gồm Nội dung câu hỏi, Câu hỏi đào sâu, Bộ tiêu chí đánh giá; cột hẹp 320px cố định khi cuộn gồm Phân loại (chủ đề, độ khó) và, chỉ ở chế độ sửa, Hành động quản trị (Nhân bản, Xoá mềm); (2) thanh đầu trang: nút quay lại là biểu tượng mũi tên **bên trái** tiêu đề (tooltip "Quay lại"), tiêu đề "Soạn câu hỏi phỏng vấn" khi tạo mới và "Sửa câu hỏi {mã}" khi sửa, bên phải gồm trạng thái lưu, nút "Xem như học viên" dạng biểu tượng mắt (tooltip) và đúng một nút "Lưu"; (3) màn nạp câu hỏi theo mã trên route, mã không tồn tại thì hiện trạng thái không tìm thấy kèm nút quay lại; (4) bỏ nhắc tới thẻ chỉ số của màn cha; (5) cập nhật các dẫn chiếu dòng của file bản dựng | 2026/10/01 | Nhóm phát triển AlgoPrep |
| V0.3 | Câu hỏi mở | Thêm đề xuất cho Q1, Q4 theo nguyên tắc admin (chờ owner xác nhận). Không đổi thiết kế màn | 2026/10/01 | AI |
| V0.4 | Sheet 4, 5, 7, 8, Câu hỏi mở | Đã chốt 2026-10-01 (owner uỷ quyền cân nhắc), xem `DEC-2026-1001-admin-configurable-settings`: (1) **chủ đề đọc từ dữ liệu do ADMIN quản lý**, không còn "chọn 1 trong 5 chủ đề cố định"; màn này chỉ chọn chủ đề có sẵn, A2 không tạo được chủ đề, ADMIN thấy liên kết "Quản lý chủ đề" dẫn về `SHR0301`; giá trị mặc định của ô chọn là chủ đề đầu theo `sort_order`; (2) Q1 và Q4 đổi từ "Đề xuất (chờ owner xác nhận)" sang **đã chốt**: không giới hạn ký tự nghiệp vụ mỗi dòng đào sâu, không giới hạn số dòng tiêu chí; (3) sửa dẫn chiếu sai "xem Q4" ở ô nhập câu hỏi đào sâu thành "xem Q1" | 2026/10/01 | AI |
| V0.5 | Sheet 3, 4, 8 | Theo `DEC-2026-1002-split-detail-and-edit-pages` (chỉ khu Admin): route đổi từ `/admin/interview-questions/[id]` sang `/admin/interview-questions/[id]/edit` (sửa) và `/admin/interview-questions/new` (tạo mới); `/admin/interview-questions/[id]` nay là trang chỉ đọc `interview_question_info` (`SHR0303`). Thêm chuyển màn `interview_question_info` → `interview_question_authoring` (nút "Sửa câu hỏi"); bấm nội dung câu hỏi ở danh sách không còn vào màn này, chỉ biểu tượng "Sửa" mới vào. Phần khoá route của Q6 RD (cùng URL cho tạo và sửa) chỉ còn đúng khu Giảng viên, chưa tách. Không đổi nội dung 4 nhóm trường | 2026/10/02 | AI |
| V0.6 | Sheet 3, 4, 8 | Đồng bộ với bản dựng ngày 2026-10-03: khu Giảng viên đã tách cùng cấu trúc khu Admin (`/instructor/interview-questions/[id]` chỉ đọc, `/edit` và `/new` là màn soạn) — bỏ ý "khu Giảng viên chưa tách, vẫn một route cho cả hai chế độ" của V0.5; Q6 của RD hết hiệu lực khoá route ở cả hai khu. Làm mới dẫn chiếu dòng tới bản dựng, RD và `02-bd/database/interview-bank.md` (bảng `question_topics` thêm lên đầu nên các dòng cũ lệch). Không đổi nội dung 4 nhóm trường | 2026/10/03 | AI |
| V0.7 | Sheet 5, 6, 8, 9 | Đồng bộ `DEC-2026-1003-toast-feedback-channel` (2026-10-03): kết quả lưu, nhân bản (đã bỏ 2026-10-08), xoá mềm và lỗi nhập là toast; ô sai đổi viền đỏ; bỏ nhãn trạng thái lưu ở thanh đầu trang; `[Tiêu điểm]` Sheet 9 ghi "viền ô + toast". Giữ nguyên khối chặn lưu do tổng trọng số khác 100, khối bộ tiêu chí trống và trạng thái không tìm thấy (nội dung trạng thái trang) | 2026/10/03 | Nhóm phát triển AlgoPrep |
| V0.8 | Sheet 5, 6, 8, 9 | Đồng bộ `DEC-2026-1003-toast-feedback-channel` và chỉ đạo của chủ dự án 2026-10-03 "bỏ khối inline, người dùng bấm vào nút đang khoá thì hiện toast": bỏ khối chặn lưu "tổng trọng số khác 100" (`rubricBlockTitle`) khỏi trang, đảo lại điều đã giữ ở V0.7. Bộ đếm "Tổng {số}%" vẫn đỏ khi khác 100. Nút "Lưu" trông mờ (`aria-disabled`) nhưng vẫn bấm được; bấm thì toast cảnh báo nêu tổng trọng số hoặc nêu nội dung câu hỏi rỗng. Giữ nguyên ghi chú "bộ tiêu chí trống" (`rubricEmptyTitle`, là trạng thái rỗng) và trạng thái không tìm thấy. Không có item nào bị xoá nên NO và EVT giữ nguyên [Nguồn: 05-coding/frontend/src/views/shared/interview-question-authoring/ui/interview-question-authoring-view.tsx:97-100, 116-126, 162] | 2026/10/03 | AI |
| V0.9 | Sheet 4, 5, 7, 8 | Đồng bộ chốt 2026-10-03 (`DEC-2026-1001-admin-configurable-settings` mục 6, Round 5): **độ khó là danh mục do ADMIN quản lý** (bảng `question_levels`, `02-bd/database/interview-bank.md:30-50`), không còn enum cố định `EASY`/`MEDIUM`/`HARD`; ba mức Dễ/Trung bình/Khó chỉ là dữ liệu khởi tạo. Ô "Độ khó" (Sheet 5 Nhóm 1 NO 3) là ô chọn đọc từ `question_levels` qua `ListQuestionLevels`; màn này không tạo, đổi tên hay xoá mức. DTO đổi `difficulty` thành `levelId` (kèm `QuestionLevelDto.displayName` để hiện nhãn); cột DB `difficulty` thay bằng `level_id` FK. Thêm `ListQuestionLevels` vào tải màn (Sheet 8) và bảng `question_levels` vào Sheet 4.3, 7.2. Giá trị mặc định: mức đầu theo `sort_order` `[SoT: Suy luận]` (thay cho `MEDIUM` cố định, vì mức có thể bị xoá). Làm mới dẫn chiếu dòng `02-bd/database/interview-bank.md` (bảng `question_levels` chèn thêm nên `interview_questions` lệch dòng). Các hành vi khác giữ nguyên | 2026/10/03 | AI |
| V0.10 | Sheet 4, 5, 7, 8, Câu hỏi mở | Làm mới và kiểm chứng toàn bộ dẫn chiếu `file:line` (2026-10-03): (1) `02-bd/database/interview-bank.md` — mục 1.1a `question_levels` chèn thêm làm các dòng sau lệch, nay cập nhật về `interview_questions` :52-76 (`status` :65, `follow_up_questions` :64, `suggested_approach`/`sample_answer_framework`/`core_keywords` :61-63, `title` :59), `answer_rubrics` :78-96 (`criterion_code` :89, `description` :90, `weight_percent` :91, unique :94), mục 1.8 `question_sets` :179-185; (2) bản dựng `interview-question-authoring-view.tsx` lệch sau khi bỏ nhãn trạng thái lưu, nay cập nhật toàn bộ số dòng; `entities/interview-question/index.ts` không còn `QUESTION_LEVELS` mà có `useInterviewLevels`; Sheet 4.4 bỏ "trạng thái lưu" khỏi thanh đầu trang; (3) sửa dẫn chiếu Câu hỏi mở sai số: "xem Q8" (không có Q8) thành Q6 (gợi ý tên tiêu chí) và Q7 (mô tả tiêu chí); "Q3" của popup xem trước thành Q2; "Q5" của ba trường F6-04/05/06 thành Q3; dẫn chiếu "`database/interview-bank.md` mục 7" của Q6 không còn đúng nên đánh dấu `[SoT: Suy luận]`; (4) **đóng Q9** theo bản dựng: quản lý độ khó mở từ popup ở `SHR0301` (chỉ ADMIN), màn soạn không có liên kết; (5) ghi khoá mức độ khó ở ô chọn là slug chữ hoa (`EASY`/`MEDIUM`/`HARD`, mức mới dạng `VERY_HARD`). Không đổi NO/EVT/Q | 2026/10/03 | AI |
| V0.11 | Sheet 3, 4, 5, 6, 7, 8, 9 | Owner chốt 2026-10-08: **bỏ "Nhân bản" câu hỏi** khỏi màn soạn (khối "Hành động quản trị" chỉ còn "Xoá mềm"). Gạch bỏ giữ số: Sheet 3 chuyển màn, sơ đồ mermaid, Sheet 5 NO 1 của khối quản trị, Sheet 6, Sheet 7 endpoint `DuplicateInterviewQuestion`, Sheet 9 EVT-12; dịch cite `file:dòng` vào view (`:312-325`, `:329-342`) sau khi bỏ nút (`interview-question-authoring-view.tsx`); xoá khoá i18n `duplicate`, `toast.duplicated`; `group4Subtitle` còn "Ngừng dùng câu hỏi này". |

---

## Sheet 3. Sơ đồ chuyển màn

> [Nội bộ] Quy ước vẽ: bố trí từ trái sang phải. Màn gọi tới tô tím, màn hiện tại tô xanh nhạt, popup tô
> vàng. Màu và `[Nguồn:]` dùng để truy vết, không phải yêu cầu của khách hàng.

### 3.1 Danh sách chuyển màn

#### Danh sách câu hỏi phỏng vấn → Biên soạn câu hỏi phỏng vấn (tạo mới)

[Điều kiện mở] Bấm nút "Câu hỏi mới" ở thanh tiêu đề màn `interview_question_management`.

[Chế độ mở] Chế độ tạo mới. Route `/admin/interview-questions/new` (khu Admin) hoặc `/instructor/interview-questions/new` (khu Giảng viên), view nhận `questionId = "new"`; không có `id` hợp lệ.

[Thông tin truyền] Không có.

[Giá trị trả về] Không có.

[Khi thành công] Tiêu đề "Soạn câu hỏi phỏng vấn"; hiển thị 4 nhóm trường rỗng, sẵn sàng nhập; khối Hành động quản trị không hiển thị.

[Khi huỷ] Không có.

[Nguồn: 01-rd/screens/shared/SHR0302_interview_question_authoring.md:40, 53-55]

#### Danh sách câu hỏi phỏng vấn → Biên soạn câu hỏi phỏng vấn (đang sửa)

[Điều kiện mở] Bấm nút biểu tượng "Sửa" ở cột Thao tác của một dòng trong danh sách ở màn `interview_question_management`. Ở cả hai khu, bấm nội dung câu hỏi **không** vào đây mà vào `interview_question_info` (xem chuyển màn kế tiếp).

[Chế độ mở] Chế độ sửa. Route `/admin/interview-questions/[questionId]/edit` (khu Admin) hoặc `/instructor/interview-questions/[questionId]/edit` (khu Giảng viên). Tham số route `questionId` là mã câu hỏi hợp lệ.

[Thông tin truyền] `id` của câu hỏi được chọn.

[Giá trị trả về] Không có.

[Khi thành công] Tiêu đề "Sửa câu hỏi {mã}"; tải dữ liệu hiện có vào 4 nhóm trường; khối Hành động quản trị hiển thị ở cột hẹp. Mã không tồn tại thì hiện trạng thái "Không tìm thấy câu hỏi" thay cho form, vẫn có nút quay lại ở bên trái tiêu đề.

[Khi huỷ] Không có.

[Nguồn: 01-rd/screens/shared/SHR0302_interview_question_authoring.md:53-55 (suy diễn từ RD); 05-coding/frontend/src/views/shared/interview-question-authoring/ui/interview-question-authoring-view.tsx:74-85, 139-151, 157]

#### Chi tiết câu hỏi phỏng vấn → Biên soạn câu hỏi phỏng vấn (đang sửa)

[Điều kiện mở] Bấm nút "Sửa câu hỏi" ở thanh đầu trang màn `interview_question_info` (`SHR0303`), ở cả khu Admin và khu Giảng viên.

[Chế độ mở] Chế độ sửa, route `{khu}/interview-questions/[questionId]/edit` với `{khu}` là `/admin` hoặc `/instructor`.

[Thông tin truyền] Mã câu hỏi đang xem.

[Giá trị trả về] Không có.

[Khi thành công] Như chuyển màn "Danh sách câu hỏi phỏng vấn → Biên soạn (đang sửa)" ở trên: tiêu đề "Sửa câu hỏi {mã}", nạp 4 nhóm, hiển thị khối Hành động quản trị.

[Khi huỷ] Không có.

[Nguồn: 01-rd/screens/shared/SHR0303_interview_question_info.md:66; 05-coding/frontend/src/views/shared/interview-question-info/ui/interview-question-info-view.tsx:73-75]

#### Biên soạn câu hỏi phỏng vấn → Danh sách câu hỏi phỏng vấn

[Điều kiện mở] Bấm nút biểu tượng mũi tên "Quay lại" ở bên trái tiêu đề thanh đầu trang (cũng có ở trạng thái không tìm thấy).

[Chế độ mở] Không có.

[Thông tin truyền] Không có.

[Giá trị trả về] Không có.

[Khi thành công] Điều hướng về danh sách câu hỏi dưới khu vực đang mount (`/admin/interview-questions` hoặc `/instructor/interview-questions`), không lưu thay đổi dở dang.

[Khi huỷ] Không có.

[Nguồn: 01-rd/screens/shared/SHR0302_interview_question_authoring.md:53-54; 05-coding/frontend/src/views/shared/interview-question-authoring/ui/interview-question-authoring-view.tsx:67-72, 143, 156]

#### ~~Biên soạn câu hỏi phỏng vấn (đang sửa) → Biên soạn câu hỏi phỏng vấn (tạo mới, dữ liệu nhân bản)~~ (đã bỏ 2026-10-08)

[Điều kiện mở] Không còn: "Nhân bản" đã bỏ khỏi khối "Hành động quản trị" theo quyết định owner 2026-10-08. Giữ tiêu đề để không đánh số lại luồng chuyển màn.

#### Biên soạn câu hỏi phỏng vấn → Popup Xác nhận xoá mềm

[Điều kiện mở] Bấm nút "Xoá mềm" trong khối "Hành động quản trị" (cột hẹp bên phải, chỉ có ở chế độ sửa).

[Chế độ mở] Không có.

[Thông tin truyền] `id` của câu hỏi đang mở.

[Giá trị trả về] Kết quả chọn "Xác nhận" hoặc "Huỷ".

[Khi thành công] Hiển thị nội dung cảnh báo không cascade xoá phiên phỏng vấn cũ.

[Khi huỷ] Không có.

[Nguồn: 01-rd/screens/shared/SHR0302_interview_question_authoring.md:44, 72-73; 05-coding/frontend/src/views/shared/interview-question-authoring/ui/interview-question-authoring-view.tsx:329-342]

#### Popup Xác nhận xoá mềm → Danh sách câu hỏi phỏng vấn

[Điều kiện mở] Bấm "Xác nhận" trong popup.

[Chế độ mở] Không có.

[Thông tin truyền] Không có.

[Giá trị trả về] Kết quả xoá mềm.

[Khi thành công] Đóng popup, điều hướng về `interview_question_management`, câu hỏi chuyển trạng thái
`RETIRED`.

[Khi huỷ] Đóng popup, giữ nguyên màn biên soạn.

[Nguồn: 01-rd/screens/shared/SHR0302_interview_question_authoring.md:44, 72-73; 02-bd/database/interview-bank.md:65]

#### Biên soạn câu hỏi phỏng vấn → Popup Xem như học viên

[Điều kiện mở] Bấm nút biểu tượng mắt "Xem như học viên" ở bên phải thanh đầu trang.

[Chế độ mở] Chế độ chỉ xem.

[Thông tin truyền] Dữ liệu 4 nhóm đang có trong form (kể cả chưa lưu) — xem Câu hỏi mở Q2.

[Giá trị trả về] Không có.

[Khi thành công] Hiển thị câu hỏi như học viên sẽ thấy ở Chế độ học và Chế độ luyện.

[Khi huỷ] Không có.

[Nguồn: 01-rd/screens/shared/SHR0302_interview_question_authoring.md:55-56, mục 4 Q2 (dòng 116); 05-coding/frontend/src/views/shared/interview-question-authoring/ui/interview-question-authoring-view.tsx:161 (nút đã dựng, chưa gắn hành vi mở popup)]

### 3.2 Sơ đồ

```mermaid
flowchart LR
    list["Danh sách câu hỏi phỏng vấn<br/>interview_question_management"] -->|"Câu hỏi mới"| createNew["Biên soạn câu hỏi phỏng vấn<br/>tạo mới"]
    list -->|"Bấm biểu tượng Sửa"| editExisting["Biên soạn câu hỏi phỏng vấn<br/>đang sửa"]
    info["Chi tiết câu hỏi phỏng vấn<br/>interview_question_info"] -->|"Sửa câu hỏi"| editExisting
    createNew -->|"Quay lại"| list
    editExisting -->|"Quay lại"| list
    editExisting -->|"Xoá mềm"| confirmDelete["Popup Xác nhận xoá mềm"]
    confirmDelete -->|"Xác nhận"| list
    confirmDelete -->|"Huỷ"| editExisting
    createNew -->|"Xem như học viên"| preview["Popup Xem như học viên"]
    editExisting -->|"Xem như học viên"| preview
    preview --> editExisting

    classDef source fill:#F3E5F5,stroke:#9C5CC4,color:#000
    classDef screen fill:#E3F2FD,stroke:#3B82F6,color:#000
    classDef popup fill:#FFF7CC,stroke:#D4A72C,color:#000

    class list,info source
    class createNew,editExisting screen
    class confirmDelete,preview popup
```

[Nguồn: 01-rd/screens/shared/SHR0302_interview_question_authoring.md:49-76, 78-85]

---

## Sheet 4. Bố cục màn hình

### 4.1 Tổng quan màn

[Mục đích màn] Cho A2/A3 tạo mới hoặc sửa một câu hỏi phỏng vấn trong ngân hàng dùng chung: nội dung +
phân loại, danh sách câu hỏi đào sâu, và bộ tiêu chí đánh giá có trọng số, để câu hỏi sẵn sàng dùng ở Chế
độ học/Chế độ luyện phía học viên [Nguồn: 01-rd/screens/shared/SHR0302_interview_question_authoring.md:13-17,
F6-13].

[Luồng nghiệp vụ chính]

1. **Hiển thị ban đầu**: tạo mới (route `new`) thì 4 nhóm trống, tiêu đề "Soạn câu hỏi phỏng vấn"; sửa thì
   nạp câu hỏi theo mã trên route (nội dung, chủ đề, độ khó, câu hỏi đào sâu, bộ tiêu chí) vào 4 nhóm, tiêu
   đề "Sửa câu hỏi {mã}". Mã không tồn tại thì hiện trạng thái "Không tìm thấy câu hỏi" thay cho form.
2. **Sửa Nhóm 1 — Nội dung và phân loại**: nhập nội dung câu hỏi (Markdown), chọn 1 chủ đề trong danh mục chủ đề
   (đọc từ dữ liệu, do ADMIN quản lý; màn này không tạo chủ đề mới), chọn độ khó trong danh mục mức độ khó (đọc từ dữ liệu, do ADMIN quản lý; màn này không tạo mức mới).
3. **Sửa Nhóm 2 — Câu hỏi đào sâu**: thêm hoặc xoá từng dòng văn bản tự do, không giới hạn số lượng.
4. **Sửa Nhóm 3 — Bộ tiêu chí đánh giá**: thêm hoặc xoá từng tiêu chí, chỉnh trọng số bằng nút giảm/tăng
   hoặc nhập trực tiếp; tổng trọng số hiển thị liên tục, đổi màu cảnh báo khi khác 100.
5. **Xem trước**: bấm "Xem như học viên" để xem câu hỏi như học viên sẽ thấy ở Chế độ học và Chế độ luyện.
6. **Hành động quản trị (Nhóm 4)**: chỉ có ở chế độ sửa. "Nhân bản" đã bỏ (owner 2026-10-08); chỉ còn "Xoá mềm" chuyển câu hỏi sang trạng thái `RETIRED` sau khi xác nhận.
7. **Lưu**: bấm "Lưu" — không có khái niệm "xuất bản" riêng, câu hỏi hiện ngay cho học viên khi lưu, trừ
   khi thiếu tiêu chí (bị ẩn khỏi Chế độ luyện, vẫn hiện ở Chế độ học).

[Người dùng] A2 (Giảng viên) hoặc A3 (Quản trị viên) đã đăng nhập, có Function `INTERVIEW_BANK_MANAGEMENT`
[Nguồn: 01-rd/req/identity.md:60, F1-12].

[Tệp liên quan] Không có. Màn này không xuất hay nhập tệp.

[Phạm vi]
- Không có phạm vi riêng theo lớp — A2 sửa được toàn bộ ngân hàng câu hỏi hệ thống, không giới hạn theo
  lớp phụ trách [Nguồn: 01-rd/screens/shared/SHR0302_interview_question_authoring.md:5, 19-23,
  DEC-2026-0825-shared-content-authoring-screens].
- Không có vòng đời nháp/xuất bản như `problem_authoring` — chốt 2026-09-01
  [Nguồn: 01-rd/screens/shared/SHR0302_interview_question_authoring.md:56-58, 119].
- Các nhóm trường trình bày theo lưới hai cột, không dùng tab — khớp bản dựng UI hiện tại (các `Card` xếp
  dọc trong mỗi cột, không có điều khiển tab). Cột rộng: Nội dung câu hỏi (phần nội dung của Nhóm 1), Câu hỏi
  đào sâu (Nhóm 2), Bộ tiêu chí đánh giá (Nhóm 3). Cột hẹp 320px cố định khi cuộn: Phân loại (chủ đề, độ khó —
  phần phân loại của Nhóm 1) rồi Hành động quản trị (Nhóm 4) [Nguồn: 05-coding/frontend/src/views/shared/interview-question-authoring/ui/interview-question-authoring-view.tsx:9-12, 169, 290, 312-325].
- Khối "Hành động quản trị" (Nhóm 4, chỉ còn Xoá mềm; Nhân bản đã bỏ 2026-10-08) là một khối riêng ở cột hẹp bên phải, **chỉ hiển thị ở
  chế độ sửa**, không phải menu ngữ cảnh ở thanh đầu trang [Nguồn: 05-coding/frontend/src/views/shared/interview-question-authoring/ui/interview-question-authoring-view.tsx:312-325].
- Không thiết kế bảng "bộ câu hỏi" (`question_sets`) — đã loại khỏi phạm vi 2026-09-13
  [Nguồn: 02-bd/database/interview-bank.md:179-185].
- Ba trường `suggested_approach`/`sample_answer_framework`/`core_keywords` (F6-04/05/06) tồn tại trong
  bảng `interview_questions` nhưng **không có nhóm trường nào ở màn này biên soạn chúng** — RD chỉ chốt
  4 nhóm và bản dựng UI hiện tại cũng không có control cho ba trường này. Đây là một phát hiện, không phải
  suy đoán "chắc có cột" — ghi thành Câu hỏi mở Q3
  [Nguồn: 01-rd/screens/shared/SHR0302_interview_question_authoring.md:49-71; 02-bd/database/interview-bank.md:61-63].

[Quyền sử dụng]
- Xem: được, khi có Function `INTERVIEW_BANK_MANAGEMENT`.
- Thêm: được (tạo câu hỏi mới; không còn đường Nhân bản từ 2026-10-08).
- Sửa: được, toàn bộ ngân hàng, không giới hạn theo `created_by`.
- Xoá: không xoá vật lý. Chuyển trạng thái `RETIRED`, không cascade xoá `user_answers`/`recall_ratings` đã
  có [Nguồn: 02-bd/database/interview-bank.md:65, 94-96].

[Số bản ghi tối đa] Nhóm 2 (câu hỏi đào sâu): không giới hạn số dòng, RD chỉ chốt "không giới hạn số
lượng" [Nguồn: 01-rd/screens/shared/SHR0302_interview_question_authoring.md:65-68]. Nhóm 3 (tiêu chí đánh giá):
không giới hạn số dòng, RD không nêu trần (đã chốt 2026-10-01, xem Q4). Không phân trang — màn chỉ sửa một bản ghi tại một thời điểm.

[Nguồn: 01-rd/screens/shared/SHR0302_interview_question_authoring.md:13-23, 49-76; 02-bd/database/interview-bank.md:8-96;
01-rd/req/identity.md:55-60]

### 4.2 DTO liên quan

- `InterviewQuestionDto`
- `AnswerRubricDto`
- `QuestionTopicDto`
- `QuestionLevelDto`

`[Suy luận]` — tên DTO do BD này đề xuất, `03-dd/api/interview-bank.md` chốt lại.

### 4.3 Bảng dữ liệu liên quan (4)

| NO | Bảng | Ghi chú |
| --: | :--- | :--- |
| 1 | `interview_bank.interview_questions` | [Nguồn: 02-bd/database/interview-bank.md:52-76] |
| 2 | `interview_bank.answer_rubrics` | [Nguồn: 02-bd/database/interview-bank.md:78-96] |
| 3 | `interview_bank.question_topics` | Danh mục chỉ đọc ở màn này; ADMIN quản lý (thêm, đổi tên, sắp xếp, xoá) ở popup "Quản lý chủ đề" của `SHR0301`, không có thao tác ghi từ màn soạn [Nguồn: 02-bd/database/interview-bank.md:8-28] |
| 4 | `interview_bank.question_levels` | Danh mục độ khó chỉ đọc ở màn này; ADMIN quản lý (thêm, đổi tên, sắp xếp, xoá), không có thao tác ghi từ màn soạn [Nguồn: 02-bd/database/interview-bank.md:30-50] |

"Câu hỏi đào sâu" (Nhóm 2) là cột `follow_up_questions` (JSONB mảng chuỗi) bên trong `interview_questions`,
**không phải một bảng riêng** [Nguồn: 02-bd/database/interview-bank.md:64].

### 4.4 Vùng bố cục

Màn này **chưa có mockup** ở `09-layoutBase/` [Nguồn: 01-rd/screens/shared/SHR0302_interview_question_authoring.md:25, 78-79].
Bảng dưới đối chiếu bản dựng UI thật hiện có (tự nhận là "PROTOTYPE — no DD yet", không phải nguồn hành vi
chính thức, chỉ dùng để mô tả cấu trúc đã dựng) — khác `09-layoutBase`, đây không phải bằng chứng chỉ-đọc
cố định, có thể đổi khi có DD/prototype chính thức.

| Vùng | Vị trí trong bản dựng UI | Nội dung |
| :--- | :--- | :--- |
| Thanh đầu trang | `interview-question-authoring-view.tsx:155-167` | Nút quay lại (biểu tượng mũi tên, bên trái tiêu đề, tooltip "Quay lại"); tiêu đề "Soạn câu hỏi phỏng vấn" (tạo mới) hoặc "Sửa câu hỏi {mã}" (sửa); bên phải: nút "Xem như học viên" (biểu tượng mắt, tooltip), nút "Lưu" duy nhất |
| Cột rộng — Khối Nội dung câu hỏi (phần nội dung của Nhóm 1) | `:171-179` | Nội dung câu hỏi (textarea) |
| Cột rộng — Khối 2, Câu hỏi đào sâu | `:181-216` | Danh sách dòng văn bản tự do, nút thêm dòng, nút xoá từng dòng |
| Cột rộng — Khối 3, Bộ tiêu chí đánh giá | `:218-285` | Tổng trọng số, danh sách tiêu chí (tên, trọng số dạng stepper), nút thêm/xoá tiêu chí |
| Cột hẹp — Phân loại (phần phân loại của Nhóm 1) | `:291-306` | Chọn chủ đề (danh mục đọc từ dữ liệu), chọn độ khó (danh mục đọc từ `useInterviewLevels`, `:91`; ô chọn `:299-304`); với ADMIN có thêm liên kết "Quản lý chủ đề" dẫn về `SHR0301` (bản dựng chưa có, **[Đợi nextjs]**) |
| Cột hẹp — Khối 4, Hành động quản trị | `:312-325` | Nút "Xoá mềm" (nút "Nhân bản" đã bỏ 2026-10-08); **chỉ hiển thị ở chế độ sửa** |
| Popup Xác nhận xoá mềm | `:329-342` | Nội dung cảnh báo, nút Xác nhận/Huỷ |
| Trạng thái không tìm thấy | `:139-151` | Nút quay lại bên trái tiêu đề "Không tìm thấy câu hỏi", thông báo mã không tồn tại |

Bố cục là lưới hai cột rộng toàn trang: cột rộng co giãn, cột hẹp cố định 320px và dính dưới thanh đầu trang
khi cuộn (`lg:grid-cols-[minmax(0,1fr)_320px]`, `lg:sticky`) [Nguồn: 05-coding/frontend/src/views/shared/interview-question-authoring/ui/interview-question-authoring-view.tsx:169, 290]; dưới breakpoint `lg`
hai cột xếp thành một cột dọc. Phần nội dung nhập dài nằm ở cột rộng, phần phân loại và thao tác quản trị
nằm ở cột hẹp để luôn trong tầm nhìn khi danh sách tiêu chí dài. Giữ nguyên cấu trúc này khi hoàn thiện
Next.js theo DD; không quy định màu sắc, khoảng cách hay typography ở BD.

### 4.5 Cấu trúc slice FSD [Nội bộ]

| Khối | Slice dự kiến | Căn cứ |
| :--- | :--- | :--- |
| Trang | `views/shared/interview-question-authoring` | Đã dựng: `05-coding/frontend/src/views/shared/interview-question-authoring/` |
| Route (hai khu) | `app/(admin)/admin/interview-questions/{new,[questionId]/edit}/page.tsx` và `app/(instructor)/instructor/interview-questions/{new,[questionId]/edit}/page.tsx` | Đã dựng; mỗi route truyền `questionId` và `listHref` (`/admin/interview-questions` hoặc `/instructor/interview-questions`) [Nguồn: 05-coding/frontend/src/app/(instructor)/instructor/interview-questions/[questionId]/edit/page.tsx:1-11; 05-coding/frontend/src/views/shared/interview-question-authoring/ui/interview-question-authoring-view.tsx:67-72] |
| Dữ liệu miền | `entities/interview-question` | Đã dựng: cung cấp `useInterviewTopics`, `useInterviewLevels`, `findInterviewQuestionByCode`, kiểu `RubricCriterion` [Nguồn: 05-coding/frontend/src/entities/interview-question/index.ts:10, 12-22, 23-33, 39-42] |
| Khối tiêu chí đánh giá | Dùng lại `shared/ui/NumberStepper` cho ô trọng số | Đã dựng: `05-coding/frontend/src/shared/ui/primitives/number-stepper.tsx`, cùng component họ `admin_ai_config` dùng cho rubric Solution Review |
| Popup | `features/interview-question-preview`, `features/interview-question-soft-delete` | Xác nhận xoá mềm đã dựng bằng `shared/ui/ConfirmDialog`; popup xem trước chưa gắn hành vi |

`[Suy luận]` — ánh xạ slice tham chiếu code đã dựng, DD màn hình chốt lại khi viết hợp đồng API.

> [Nội bộ] Ảnh minh hoạ đặt ở `08-diagram/02-bd/screens/shared/`, chụp bằng Playwright trên ứng dụng
> Next.js thật khi màn có DD và API thật. Bản dựng hiện tại chưa gọi backend nên chưa chụp.

---

## Sheet 5. Danh sách item màn hình

> Quy ước ID, bộ thẻ và giá trị chuẩn: `02-bd/_rules/bd-template-9sheet.md` mục 2, 3, 4.

### Khu vực A — Thanh đầu trang

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Thanh đầu trang | | | | | | | | | | | | | |
| | 1 | Nút quay lại | `interviewQuestionAuthoring.header.btnBack` | - | - | Button | - | - | - | I | - | Chỉ biểu tượng mũi tên | Nút biểu tượng đặt bên trái tiêu đề, tooltip "Quay lại"; điều hướng về danh sách `interview_question_management` dưới khu vực đang mount<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-3 |
| | 2 | Tiêu đề màn | `interviewQuestionAuthoring.header.title` | `interview_questions` | `id` | Label | String | - | - | O | "Soạn câu hỏi phỏng vấn" | `Sửa câu hỏi {mã}` | "Soạn câu hỏi phỏng vấn" khi tạo mới, "Sửa câu hỏi {mã}" khi đang sửa (mã lấy từ tham số route, không phải nội dung `title`) [Nguồn: 05-coding/frontend/src/views/shared/interview-question-authoring/ui/interview-question-authoring-view.tsx:157]<br>[Nguồn giá trị] Nhãn tĩnh i18n; mã câu hỏi khi đang sửa<br>[EVT liên quan] EVT-2 |
| | 3 | Trạng thái lưu | `interviewQuestionAuthoring.header.saveStatus` | - | - | Label | Enum | - | - | O | "Chưa lưu" | - | "Đang lưu" / "Đã lưu lúc {giờ}" / "Chưa lưu" — trạng thái phía client, không phải cột DB. **Bản dựng 2026-10-03 không còn nhãn này**: kết quả lưu hiện bằng toast, nút "Lưu" trông mờ khi nội dung rỗng, tổng trọng số khác 100 hoặc đang lưu nhưng vẫn bấm được, bấm thì toast nêu lý do [Nguồn: 05-coding/frontend/src/views/shared/interview-question-authoring/ui/interview-question-authoring-view.tsx:99-100, 116-126, 162]<br>[Nguồn giá trị] Trạng thái client sau mỗi lần gọi Lưu<br>[EVT liên quan] EVT-10, EVT-11 |
| | 4 | Xem như học viên | `interviewQuestionAuthoring.header.btnPreview` | - | - | Button | - | - | - | I | - | Chỉ biểu tượng mắt | Nút biểu tượng, tooltip "Xem như học viên"; mở popup xem trước theo dữ liệu form hiện tại<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-16 |
| | 5 | Lưu | `interviewQuestionAuthoring.header.btnSave` | - | - | Button | - | - | - | I | - | - | Nút hành động chính duy nhất của màn, tạo mới hoặc cập nhật câu hỏi. **Bản dựng 2026-10-03:** khi bị chặn (nội dung rỗng hoặc tổng trọng số khác 100) nút trông mờ nhưng vẫn bấm được, bấm thì toast cảnh báo; không còn khối chặn lưu trên trang<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-10, EVT-11 |

### Khu vực B — Nhóm 1: Nội dung và phân loại

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Nhóm 1 — Nội dung và phân loại | | | | | | | | | | | | | |
| | 1 | Nội dung câu hỏi | `interviewQuestionAuthoring.content.contentMarkdown` | `interview_questions` | `content_markdown` | TextArea | String | - | Có | I/O | rỗng | Markdown | Nội dung câu hỏi phỏng vấn<br>[Nguồn giá trị] Cột `content_markdown`<br>[EVT liên quan] EVT-4 |
| | 2 | Chủ đề | `interviewQuestionAuthoring.content.topic` | `interview_questions` | `topic_id` | List | String | - | Có | I/O | Chủ đề đầu theo `sort_order` | Nhãn tiếng Việt | Chọn 1 chủ đề trong danh mục; số lựa chọn không cố định vì ADMIN thêm/xoá chủ đề được. A2 không có đường tạo chủ đề tại màn này; ADMIN thấy liên kết "Quản lý chủ đề" dẫn về `SHR0301`<br>[Nguồn giá trị] Danh mục `question_topics` đọc qua `ListQuestionTopics`, sắp theo `sort_order` (5 chủ đề khởi tạo: `CS_THEORY`, `SYSTEM_DESIGN`, `DATABASE`, `LANGUAGE`, `BEHAVIORAL`)<br>[EVT liên quan] EVT-4 |
| | 3 | Độ khó | `interviewQuestionAuthoring.content.level` | `interview_questions` | `level_id` | List | String | - | Có | I/O | Mức đầu theo `sort_order` `[SoT: Suy luận]` | Nhãn tiếng Việt | Chọn 1 mức trong danh mục; số lựa chọn không cố định vì ADMIN thêm/xoá mức được (không giới hạn trên). Màn này không có đường tạo mức; ADMIN quản lý danh mục ở popup "Quản lý độ khó" của `SHR0301` (chỉ ADMIN), màn soạn không có liên kết tới đó (xem Q9, đã đóng). Giá trị ô chọn là `key` của mức: slug chữ hoa, ba mức khởi tạo `EASY`/`MEDIUM`/`HARD`, mức ADMIN thêm sinh slug dạng `VERY_HARD`; nhãn hiện là `label` [Nguồn: 05-coding/frontend/src/views/shared/interview-question-authoring/ui/interview-question-authoring-view.tsx:299-304; 05-coding/frontend/src/entities/interview-question/model/level-store.ts:24-33; 05-coding/frontend/src/shared/lib/managed-list-store.ts:24]<br>[Nguồn giá trị] Danh mục `question_levels` đọc qua `ListQuestionLevels`, sắp theo `sort_order` (3 mức khởi tạo: `EASY`, `MEDIUM`, `HARD`)<br>[EVT liên quan] EVT-4 |

### Khu vực C — Nhóm 2: Câu hỏi đào sâu

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Nhóm 2 — Câu hỏi đào sâu | | | | | | | | | | | | | |
| | 1 | Danh sách câu hỏi đào sâu | `interviewQuestionAuthoring.followUp.list` | `interview_questions` | `follow_up_questions` | List | List | - | - | I/O | 1 dòng rỗng | - | Mỗi dòng là một câu hỏi đào sâu tự do, dùng ở giai đoạn Phản biện của Phỏng vấn giả lập (F5-11)<br>[Nguồn giá trị] Cột `follow_up_questions` (JSONB mảng chuỗi)<br>[EVT liên quan] EVT-5, EVT-6 |
| | 2 | Ô nhập câu hỏi đào sâu | `interviewQuestionAuthoring.followUp.item` | `interview_questions` | `follow_up_questions` | TextArea | String | - | Không | I/O | rỗng | - | Một phần tử của mảng `follow_up_questions`; không đặt giới hạn ký tự nghiệp vụ (đã chốt 2026-10-01, xem Q1), chỉ có trần kỹ thuật của request<br>[Nguồn giá trị] Phần tử mảng tương ứng<br>[EVT liên quan] EVT-4 |
| | 3 | Thêm dòng | `interviewQuestionAuthoring.followUp.btnAdd` | - | - | Button | - | - | - | I | - | - | Thêm một dòng trống vào cuối danh sách<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-5 |
| | 4 | Xoá dòng | `interviewQuestionAuthoring.followUp.btnRemove` | - | - | Button | - | - | - | I | - | - | Xoá dòng tương ứng khỏi danh sách<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-6 |

### Khu vực D — Nhóm 3: Bộ tiêu chí đánh giá có trọng số

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Nhóm 3 — Bộ tiêu chí đánh giá | | | | | | | | | | | | | |
| | 1 | Tổng trọng số | `interviewQuestionAuthoring.rubric.totalBadge` | - | - | Badge | Number | 3 | - | O | - | `Tổng {số}%` | Chỉ hiển thị khi có ít nhất một tiêu chí; đổi sang màu đỏ khi tổng khác 100 (dấu hiệu duy nhất trên trang, lý do chi tiết nằm ở toast khi bấm Lưu) [Nguồn: 05-coding/frontend/src/views/shared/interview-question-authoring/ui/interview-question-authoring-view.tsx:223-231]<br>[Công thức] Cộng `weight_percent` của mọi dòng đang hiển thị<br>[EVT liên quan] EVT-9 |
| | 2 | Danh sách tiêu chí | `interviewQuestionAuthoring.rubric.list` | `answer_rubrics` | - | List | List | - | - | I/O | 0 dòng | - | Câu hỏi thiếu tiêu chí vẫn lưu được hợp lệ, chỉ bị ẩn khỏi Chế độ luyện<br>[Nguồn giá trị] Các dòng `answer_rubrics` theo `question_id`<br>[EVT liên quan] EVT-7, EVT-8, EVT-9 |
| | 3 | Tên/mã tiêu chí | `interviewQuestionAuthoring.rubric.col.criterionCode` | `answer_rubrics` | `criterion_code` | TextBox | String | - | Có | I/O | rỗng | - | A2/A3 tự đặt tên tiêu chí, không có danh mục gợi ý sẵn — xem Q6<br>[Nguồn giá trị] Cột `criterion_code`, người dùng nhập<br>[EVT liên quan] EVT-4 |
| | 4 | Trọng số | `interviewQuestionAuthoring.rubric.col.weightPercent` | `answer_rubrics` | `weight_percent` | NumberBox | Number | 5 | Có | I/O | 0 | `{số}%`, bước nhảy 5 | Stepper dùng chung `shared/ui/NumberStepper`, khoảng 0-100, bước nhảy mặc định 5<br>[Nguồn giá trị] Cột `weight_percent`<br>[EVT liên quan] EVT-9 |
| | 5 | Thêm tiêu chí | `interviewQuestionAuthoring.rubric.btnAdd` | - | - | Button | - | - | - | I | - | - | Thêm một dòng tiêu chí trống (trọng số mặc định 0)<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-7 |
| | 6 | Xoá tiêu chí | `interviewQuestionAuthoring.rubric.btnRemove` | - | - | Button | - | - | - | I | - | - | Xoá dòng tiêu chí tương ứng<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-8 |

### Khu vực E — Nhóm 4: Hành động quản trị (cột hẹp, chỉ ở chế độ sửa)

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Nhóm 4 — Hành động quản trị | | | | | | | | | | | | | |
| | 1 | ~~Nhân bản~~ (đã bỏ 2026-10-08) | `interviewQuestionAuthoring.admin.btnDuplicate` | - | - | - | - | - | - | - | - | - | **Đã bỏ 2026-10-08** (owner); giữ NO để không đánh số lại<br>[EVT liên quan] EVT-12 (đã bỏ) |
| | 2 | Xoá mềm | `interviewQuestionAuthoring.admin.btnSoftDelete` | `interview_questions` | `status` | Button | - | - | - | I | - | - | Mở popup xác nhận, chuyển `status` sang `RETIRED` khi xác nhận<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-13 |

### Popup Xem như học viên

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Popup Xem như học viên | | | | | | | | | | | | | |
| | 1 | Xem trước Chế độ học | `interviewQuestionAuthoring.popup.previewStudy` | - | - | Popup | - | - | - | O | - | - | Hiển thị nội dung, câu hỏi đào sâu như học viên thấy ở Chế độ học; nguồn dữ liệu là form hiện tại, chưa chốt — xem Q2<br>[Nguồn giá trị] Dữ liệu form đang biên soạn<br>[EVT liên quan] EVT-16, EVT-17 |
| | 2 | Xem trước Chế độ luyện | `interviewQuestionAuthoring.popup.previewPractice` | - | - | Popup | - | - | - | O | - | - | Hiển thị câu hỏi và cảnh báo "sẽ ẩn khỏi Chế độ luyện" nếu Nhóm 3 đang trống<br>[Nguồn giá trị] Dữ liệu form đang biên soạn<br>[EVT liên quan] EVT-16, EVT-17 |
| | 3 | Đóng | `interviewQuestionAuthoring.popup.previewClose` | - | - | Button | - | - | - | I | - | - | Đóng popup, quay lại màn soạn<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-17 |

### Popup Xác nhận xoá mềm

| Khu vực | NO | Tên item | ID item | Bảng DB | Cột DB | Loại UI | Kiểu | Độ dài | Bắt buộc | I/O | Giá trị mặc định | Định dạng | Ghi chú |
| :--- | --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :-: | :--- | :--- | :--- |
| Popup Xác nhận xoá mềm | | | | | | | | | | | | | |
| | 1 | Nội dung cảnh báo | `interviewQuestionAuthoring.popup.softDeleteBody` | - | - | Label | String | - | - | O | - | - | Cảnh báo không cascade xoá phiên phỏng vấn cũ<br>[Nguồn giá trị] Nhãn tĩnh i18n<br>[EVT liên quan] - |
| | 2 | Xác nhận | `interviewQuestionAuthoring.popup.softDeleteConfirm` | - | - | Button | - | - | - | I | - | - | Xác nhận xoá mềm<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-14 |
| | 3 | Huỷ | `interviewQuestionAuthoring.popup.softDeleteCancel` | - | - | Button | - | - | - | I | - | - | Đóng popup, giữ nguyên câu hỏi<br>[Nguồn giá trị] -<br>[EVT liên quan] EVT-15 |

[Nguồn: 01-rd/screens/shared/SHR0302_interview_question_authoring.md:53-73; 02-bd/database/interview-bank.md:8-96;
05-coding/frontend/src/views/shared/interview-question-authoring/ui/interview-question-authoring-view.tsx:139-151, 155-346]

---

## Sheet 6. Đặc tả điều khiển item

> Ký hiệu cột `Hiển thị` và thứ tự thẻ: `02-bd/_rules/bd-template-9sheet.md` mục 2, 3. Dùng đúng NO, tên
> item và thứ tự của Sheet 5. Lỗi nhập liệu ghi ở Sheet 9, xử lý nghiệp vụ ghi ở Sheet 8.

### Khu vực A — Thanh đầu trang

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Thanh đầu trang | | | | |
| | 1 | Nút quay lại | Có | [Điều kiện hiển thị] Hiển thị cả ở trạng thái không tìm thấy câu hỏi. Tooltip "Quay lại" hiện khi rê chuột hoặc focus. |
| | 2 | Tiêu đề màn | Có | [Điều kiện hiển thị] Nội dung đổi theo chế độ: tạo mới hay sửa. |
| | 3 | Trạng thái lưu | Có | [Tự động đặt] Đổi sang "Đang lưu" ngay khi bấm Lưu, đổi sang "Đã lưu lúc {giờ}" khi có phản hồi thành công, quay về "Chưa lưu" khi có bất kỳ thay đổi nào sau lần lưu gần nhất. Bản dựng 2026-10-03 thay nhãn này bằng toast thành công hoặc toast lỗi khi lưu. |
| | 4 | Xem như học viên | Có | [Điều kiện kích hoạt] Tooltip "Xem như học viên" hiện khi rê chuột hoặc focus. Luôn kích hoạt, kể cả khi đang tạo mới và chưa nhập gì. |
| | 5 | Lưu | Có | [Điều kiện kích hoạt] Bấm được, bị chặn kèm toast: nút chỉ trông bình thường khi nội dung câu hỏi không rỗng **và** (Nhóm 3 trống hoặc tổng trọng số bằng 100) **và** không đang trong trạng thái đang lưu; ngược lại nút trông mờ nhưng vẫn bấm được, bấm thì không lưu mà hiện toast cảnh báo nêu tổng trọng số hiện tại hoặc nêu nội dung câu hỏi đang rỗng. |

### Khu vực B — Nhóm 1: Nội dung và phân loại

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Nhóm 1 — Nội dung và phân loại | | | | |
| | 1 | Nội dung câu hỏi | Có | - |
| | 2 | Chủ đề | Có | - |
| | 3 | Độ khó | Có | - |

### Khu vực C — Nhóm 2: Câu hỏi đào sâu

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Nhóm 2 — Câu hỏi đào sâu | | | | |
| | 1 | Danh sách câu hỏi đào sâu | Có | [Điều kiện hiển thị] Luôn có ít nhất một dòng — không cho xoá dòng cuối cùng còn lại. |
| | 2 | Ô nhập câu hỏi đào sâu | Có | - |
| | 3 | Thêm dòng | Có | - |
| | 4 | Xoá dòng | Có | [Điều kiện kích hoạt] Không kích hoạt khi danh sách chỉ còn đúng một dòng. |

### Khu vực D — Nhóm 3: Bộ tiêu chí đánh giá có trọng số

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Nhóm 3 — Bộ tiêu chí đánh giá | | | | |
| | 1 | Tổng trọng số | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị khi danh sách tiêu chí có ít nhất một dòng. [Tự động đặt] Đổi sang màu đỏ khi tổng khác 100. |
| | 2 | Danh sách tiêu chí | Có | [Điều kiện hiển thị] Trống thì hiển thị ghi chú "Chưa có tiêu chí — câu hỏi vẫn lưu được nhưng sẽ ẩn khỏi Chế độ luyện" thay cho danh sách. |
| | 3 | Tên/mã tiêu chí | Có | - |
| | 4 | Trọng số | Có | [Tự động đặt] Giá trị cập nhật ngay khi bấm nút giảm/tăng hoặc nhập trực tiếp; kéo theo tính lại Tổng trọng số. |
| | 5 | Thêm tiêu chí | Có | - |
| | 6 | Xoá tiêu chí | Có | - |

### Khu vực E — Nhóm 4: Hành động quản trị

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Nhóm 4 — Hành động quản trị | | | | |
| | 1 | ~~Nhân bản~~ (đã bỏ) | Không | Đã bỏ 2026-10-08; không có item này trên màn. |
| | 2 | Xoá mềm | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị ở chế độ đang sửa. |

### Popup Xem như học viên

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Popup Xem như học viên | | | | |
| | 1 | Xem trước Chế độ học | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị sau khi bấm "Xem như học viên". |
| | 2 | Xem trước Chế độ luyện | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị sau khi bấm "Xem như học viên"; kèm cảnh báo ẩn khỏi Chế độ luyện nếu Nhóm 3 đang trống. |
| | 3 | Đóng | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị khi popup đang mở. |

### Popup Xác nhận xoá mềm

| Khu vực | NO | Tên item | Hiển thị | Ghi chú |
| :--- | --: | :--- | :-: | :--- |
| Popup Xác nhận xoá mềm | | | | |
| | 1 | Nội dung cảnh báo | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị sau khi bấm "Xoá mềm". |
| | 2 | Xác nhận | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị khi popup đang mở. |
| | 3 | Huỷ | Điều kiện | [Điều kiện hiển thị] Chỉ hiển thị khi popup đang mở. |

---

## Sheet 7. Danh sách truyền dữ liệu

### 7.1 Trường DTO

| NO | DTO | Trường DTO | Kiểu | Bảng DB | Cột DB | Item màn | Hiển thị | Ghi chú |
| --: | :--- | :--- | :--- | :--- | :--- | :--- | :-: | :--- |
| 1 | `InterviewQuestionDto` | `id` | UUID | `interview_questions` | `id` | - | Không | [Nguồn] Phản hồi của `GetInterviewQuestionDetail`<br>[Đích] Tham số của `UpdateInterviewQuestion`, `RetireInterviewQuestion`. Không hiển thị trên màn. |
| 2 | `InterviewQuestionDto` | `topicId` | UUID | `interview_questions` | `topic_id` | Nhóm 1 "Chủ đề" | Có | [Nguồn] Phản hồi của `GetInterviewQuestionDetail` hoặc `ListQuestionTopics`<br>[Chuyển đổi] Khoá tra `QuestionTopicDto.displayName` khi hiển thị. |
| 3 | `InterviewQuestionDto` | `levelId` | UUID | `interview_questions` | `level_id` | Nhóm 1 "Độ khó" | Có | [Nguồn] Phản hồi của `GetInterviewQuestionDetail` hoặc `ListQuestionLevels`<br>[Chuyển đổi] Khoá tra `QuestionLevelDto.displayName` khi hiển thị. Thay cho `difficulty` enum (trước 2026-10-03). |
| 4 | `InterviewQuestionDto` | `title` | String | `interview_questions` | `title` | Không hiển thị trực tiếp (tiêu đề màn dùng mã câu hỏi, không dùng `title`) | Không | [Nguồn] Người dùng nhập cùng nội dung câu hỏi, hoặc suy ra từ `content_markdown` — cách xác định `title` cụ thể để DD chốt. |
| 5 | `InterviewQuestionDto` | `contentMarkdown` | String | `interview_questions` | `content_markdown` | Nhóm 1 "Nội dung câu hỏi" | Có | [Nguồn] Giá trị người dùng nhập<br>[Đích] Tham số của `CreateInterviewQuestion`/`UpdateInterviewQuestion`<br>[Chuyển đổi] Nội dung người dùng nhập là **dữ liệu**, không ghép chuỗi thành chỉ thị AI — cùng nguyên tắc chống prompt-injection của `02-bd/architecture/interview-bank.md` mục 5. |
| 6 | `InterviewQuestionDto` | `followUpQuestions` | `String[]` | `interview_questions` | `follow_up_questions` | Nhóm 2 "Danh sách câu hỏi đào sâu" | Có | [Nguồn] Giá trị người dùng nhập, mỗi phần tử một dòng<br>[Đích] Tham số của `CreateInterviewQuestion`/`UpdateInterviewQuestion`. |
| 7 | `InterviewQuestionDto` | `status` | Enum | `interview_questions` | `status` | Nhóm 4 "Xoá mềm" | Không | [Nguồn] Phản hồi của `GetInterviewQuestionDetail`<br>[Đích] Tham số của `RetireInterviewQuestion` đổi sang `RETIRED`. Không hiển thị trực tiếp, chỉ quyết định hiển thị Nhóm 4. |
| 8 | `AnswerRubricDto` | `questionId` | UUID | `answer_rubrics` | `question_id` | - | Không | [Nguồn] `id` của câu hỏi đang biên soạn<br>[Đích] Tham số của `CreateInterviewQuestion`/`UpdateInterviewQuestion` (gửi kèm danh sách tiêu chí). |
| 9 | `AnswerRubricDto` | `criterionCode` | String | `answer_rubrics` | `criterion_code` | Nhóm 3 "Tên/mã tiêu chí" | Có | [Nguồn] Người dùng tự đặt, không có danh mục gợi ý — xem Q6. |
| 10 | `AnswerRubricDto` | `description` | String | `answer_rubrics` | `description` | - | Không | [Nguồn] Cột `description` — RD và bản dựng UI hiện tại chưa có ô nhập riêng cho trường này ở màn, xem Q7. |
| 11 | `AnswerRubricDto` | `weightPercent` | Number | `answer_rubrics` | `weight_percent` | Nhóm 3 "Trọng số" | Có | [Nguồn] Giá trị người dùng chỉnh bằng stepper hoặc nhập trực tiếp<br>[Đích] Tham số của `CreateInterviewQuestion`/`UpdateInterviewQuestion`<br>[Chuyển đổi] Gửi lên dạng số, không kèm ký hiệu phần trăm. |
| 12a | `QuestionLevelDto` | `id`, `code`, `displayName` | UUID, String, String | `question_levels` | `id`, `code`, `display_name` | Nhóm 1 "Độ khó" (danh sách chọn) | Có | [Nguồn] Phản hồi của `ListQuestionLevels`, danh mục chỉ đọc ở màn này; `code` là slug (3 mức khởi tạo mang mã `EASY`/`MEDIUM`/`HARD`). Tên DTO do BD đề xuất `[SoT: Suy luận]`. |
| 12 | `QuestionTopicDto` | `id`, `code`, `displayName` | UUID, String, String | `question_topics` | `id`, `code`, `display_name` | Nhóm 1 "Chủ đề" (danh sách chọn) | Có | [Nguồn] Phản hồi của `ListQuestionTopics`, danh mục chỉ đọc ở màn này, đọc từ dữ liệu do ADMIN quản lý (không còn 5 dòng cố định); `code` là slug, không còn enum. |

### 7.2 Truy cập bảng dữ liệu (4)

| NO | Tên logic | Bảng | Repository | CRUD | Mục đích | Ghi chú |
| --: | :--- | :--- | :--- | :-: | :--- | :--- |
| 1 | Câu hỏi phỏng vấn | `interview_bank.interview_questions` | `InterviewQuestionRepository` | C, R, U | Tạo mới, đọc chi tiết để sửa, cập nhật nội dung/phân loại/câu hỏi đào sâu, chuyển `status` khi xoá mềm | `GetInterviewQuestionDetail`: R<br>`CreateInterviewQuestion`: C<br>`UpdateInterviewQuestion`: U<br>`RetireInterviewQuestion`: U |
| 2 | Bộ tiêu chí đánh giá | `interview_bank.answer_rubrics` | `AnswerRubricRepository` | C, R, U, D | Đọc, tạo, sửa, xoá từng dòng tiêu chí gắn với một câu hỏi | `GetInterviewQuestionDetail`: R<br>`CreateInterviewQuestion`/`UpdateInterviewQuestion`: C, U, D (đồng bộ toàn bộ danh sách theo trạng thái form) |
| 3 | Danh mục chủ đề | `interview_bank.question_topics` | `QuestionTopicRepository` | R | Đọc danh mục chủ đề, không có thao tác ghi từ màn này (ghi ở `SHR0301`) | `ListQuestionTopics`: R |
| 3a | Danh mục độ khó | `interview_bank.question_levels` | `QuestionLevelRepository` | R | Đọc danh mục độ khó, không có thao tác ghi từ màn này | `ListQuestionLevels`: R |

Không có thao tác xoá vật lý (`D`) trên `interview_questions`: chuyển `status` sang `RETIRED`, không cascade
xoá `answer_rubrics`, `user_answers`, `recall_ratings` đã có [Nguồn: 02-bd/database/interview-bank.md:65, 94-96].

`[Suy luận]` — tên repository do BD này đề xuất, DD module `interview-bank` chốt lại.

### 7.3 Danh sách endpoint [Nội bộ]

> Chỉ tên nghiệp vụ và BC sở hữu. Hợp đồng chi tiết thuộc `03-dd/api/interview-bank.md`.

| NO | Endpoint (tên nghiệp vụ) | Mục đích | BC sở hữu |
| --: | :--- | :--- | :--- |
| 1 | `ListQuestionTopics` | Tải danh mục chủ đề (đọc từ dữ liệu) | `interview-bank` |
| 1a | `ListQuestionLevels` | Tải danh mục độ khó (đọc từ dữ liệu) | `interview-bank` |
| 2 | `GetInterviewQuestionDetail` | Tải chi tiết một câu hỏi (kèm `answer_rubrics`, `follow_up_questions`) để sửa | `interview-bank` |
| 3 | `CreateInterviewQuestion` | Tạo câu hỏi mới, kèm danh sách tiêu chí đánh giá | `interview-bank` |
| 4 | `UpdateInterviewQuestion` | Cập nhật câu hỏi theo `id`, backend kiểm tổng trọng số bằng 100 nếu có tiêu chí | `interview-bank` |
| 5 | ~~`DuplicateInterviewQuestion`~~ (đã bỏ 2026-10-08) | Nhân bản câu hỏi bị bỏ khỏi phạm vi theo owner; giữ số thứ tự | `interview-bank` |
| 6 | `RetireInterviewQuestion` | Chuyển câu hỏi sang trạng thái `RETIRED`, không cascade xoá | `interview-bank` |

Không có endpoint riêng cho popup "Xem như học viên" ở đợt này — xem trước dùng dữ liệu form hiện tại,
không gọi máy chủ (đề xuất, chưa chốt — xem Q2).

[Nguồn: 02-bd/database/interview-bank.md:8-96]

---

## Sheet 8. Danh sách sự kiện

> Bộ thẻ và quy ước cột `Chuyển màn`: `02-bd/_rules/bd-template-9sheet.md` mục 2, 3.

### Màn chính: Biên soạn câu hỏi phỏng vấn

| NO | Loại | Sự kiện | Chi tiết | Chuyển màn | Gọi API | Tên xử lý | Ghi chú |
| --: | :--- | :--- | :--- | :-: | :-: | :--- | :--- |
| 1 | Màn hình | Khởi tạo màn — Tạo mới | Vào màn từ nút "Câu hỏi mới", tham số route `new`. | Không | Có | `ListQuestionTopics`, `ListQuestionLevels` | [Các bước]<br>1. Kiểm tra quyền `INTERVIEW_BANK_MANAGEMENT`.<br>2. Tải danh mục chủ đề và danh mục độ khó.<br>3. Hiển thị 4 nhóm trống, tiêu đề "Soạn câu hỏi phỏng vấn", không có khối Hành động quản trị.<br>[Khi thành công] Hiển thị form trống, nút Lưu chưa kích hoạt vì nội dung câu hỏi còn rỗng.<br>[Khi lỗi] Hiển thị lỗi tải dạng trạng thái thay chỗ nội dung, không rời màn. |
| 2 | Màn hình | Khởi tạo màn — Đang sửa | Vào màn từ biểu tượng "Sửa" ở danh sách hoặc nút "Sửa câu hỏi" của `interview_question_info`, route `/[mã]/edit` (cả hai khu), tham số route là mã câu hỏi. | Không | Có | `ListQuestionTopics`, `ListQuestionLevels`, `GetInterviewQuestionDetail` | [Các bước]<br>1. Kiểm tra quyền `INTERVIEW_BANK_MANAGEMENT`.<br>2. Tải song song danh mục chủ đề, danh mục độ khó và chi tiết câu hỏi.<br>3. Điền dữ liệu vào 4 nhóm, tiêu đề "Sửa câu hỏi {mã}", hiển thị khối Hành động quản trị.<br>[Khi thành công] Hiển thị đầy đủ dữ liệu hiện có.<br>[Khi lỗi] Không tìm thấy câu hỏi hoặc lỗi tải: hiển thị trạng thái thay chỗ nội dung (không tìm thấy) kèm nút quay lại `interview_question_management`. |
| 3 | Nút | Quay lại | Bấm nút biểu tượng mũi tên "Quay lại" ở bên trái tiêu đề. | Có | Không | - | [Các bước]<br>1. Điều hướng về `interview_question_management`.<br>[Khi thành công] Rời màn, không lưu thay đổi dở dang. |
| 4 | Nhập liệu | Sửa Nhóm 1, 2 hoặc tên/trọng số tiêu chí | Gõ nội dung, đổi chủ đề/độ khó, sửa dòng câu hỏi đào sâu, sửa tên tiêu chí. | Không | Không | - | [Các bước]<br>1. Ghi nhận giá trị mới vào trạng thái biên soạn.<br>2. Đặt cờ "có thay đổi chưa lưu" (bản dựng không hiện nhãn).<br>[Khi thành công] Nút Lưu kích hoạt lại khi điều kiện Sheet 6 khu vực A dòng 5 thoả. |
| 5 | Nút | Thêm dòng câu hỏi đào sâu | Bấm "Thêm dòng" ở Nhóm 2. | Không | Không | - | [Các bước]<br>1. Thêm một phần tử rỗng vào cuối `followUpQuestions`.<br>[Khi thành công] Hiển thị thêm một ô nhập trống. |
| 6 | Nút | Xoá dòng câu hỏi đào sâu | Bấm "Xoá" trên một dòng ở Nhóm 2. | Không | Không | - | [Các bước]<br>1. Xoá phần tử tương ứng khỏi `followUpQuestions`.<br>[Khi thành công] Danh sách còn lại đúng số dòng, không xoá được nếu chỉ còn một dòng (Sheet 6). |
| 7 | Nút | Thêm tiêu chí | Bấm "Thêm tiêu chí" ở Nhóm 3. | Không | Không | - | [Các bước]<br>1. Thêm một dòng tiêu chí mới, `weightPercent = 0`.<br>[Khi thành công] Tổng trọng số tính lại, hiển thị dòng mới trống. |
| 8 | Nút | Xoá tiêu chí | Bấm "Xoá" trên một dòng tiêu chí. | Không | Không | - | [Các bước]<br>1. Xoá dòng tiêu chí tương ứng.<br>2. Tính lại tổng trọng số.<br>[Khi thành công] Danh sách còn lại đúng số dòng; hết dòng thì hiển thị lại ghi chú "Chưa có tiêu chí". |
| 9 | Nút/Nhập liệu | Đổi trọng số tiêu chí | Bấm nút giảm/tăng của stepper, hoặc nhập trực tiếp giá trị. | Không | Không | - | [Các bước]<br>1. Cập nhật `weightPercent` của dòng đó, giới hạn 0-100.<br>2. Tính lại Tổng trọng số.<br>[Khi thành công] Tổng bằng 100 thì hiển thị màu trung tính và cho phép kích hoạt Lưu (cùng điều kiện Nội dung câu hỏi không rỗng). Tổng khác 100 (khi có ít nhất một dòng) thì bộ đếm "Tổng {số}%" đổi sang màu đỏ và nút Lưu trông mờ, bấm vào thì toast cảnh báo nêu tổng hiện tại. |
| 10 | Nút | Lưu — Tạo mới | Bấm "Lưu" khi đang ở chế độ tạo mới. | Không | Có | `CreateInterviewQuestion` | [Các bước]<br>1. Kiểm nội dung câu hỏi không rỗng.<br>2. Kiểm tổng trọng số bằng 100 nếu Nhóm 3 không trống.<br>3. Gửi dữ liệu 4 nhóm lên máy chủ.<br>4. Nhận `id` mới, chuyển màn sang chế độ đang sửa (giữ nguyên route/URL theo `id` mới).<br>[Khi thành công] Toast thành công; Nhóm 4 (Xoá mềm) xuất hiện.<br>[Khi lỗi] Giữ nguyên dữ liệu đã nhập, ô hoặc khối gây lỗi đổi viền đỏ và hiện toast lỗi.<br>[Thông báo hoàn tất] Toast thành công "Đã lưu câu hỏi." [Nguồn: 05-coding/frontend/src/views/shared/interview-question-authoring/ui/interview-question-authoring-view.tsx:131] |
| 11 | Nút | Lưu — Cập nhật | Bấm "Lưu" khi đang ở chế độ đang sửa. | Không | Có | `UpdateInterviewQuestion` | [Các bước]<br>1. Kiểm nội dung câu hỏi không rỗng.<br>2. Kiểm tổng trọng số bằng 100 nếu Nhóm 3 không trống.<br>3. Gửi dữ liệu 4 nhóm lên máy chủ.<br>[Khi thành công] Toast thành công. Nhóm 3 trống thì khối thông báo "bộ tiêu chí trống" trên trang (trạng thái trang, giữ nguyên) nêu cảnh báo không chặn: câu hỏi sẽ ẩn khỏi Chế độ luyện, vẫn hiện ở Chế độ học.<br>[Khi lỗi] Giữ nguyên dữ liệu đã nhập, ô hoặc khối gây lỗi đổi viền đỏ và hiện toast lỗi.<br>[Thông báo hoàn tất] Toast thành công "Đã lưu câu hỏi." [Nguồn: 05-coding/frontend/src/views/shared/interview-question-authoring/ui/interview-question-authoring-view.tsx:131] |
| 12 | Nút | ~~Nhân bản~~ (đã bỏ 2026-10-08) | - | Không | Không | - | **Đã bỏ** theo owner 2026-10-08; giữ số EVT để không đánh số lại. |
| 13 | Nút | Mở xác nhận xoá mềm | Bấm "Xoá mềm" ở Nhóm 4 (cột hẹp, chỉ ở chế độ sửa). | Không | Không | - | [Các bước]<br>1. Mở popup xác nhận.<br>[Khi thành công] Popup hiển thị nội dung cảnh báo không cascade. |
| 14 | Popup | Xác nhận xoá mềm — "Xác nhận" | Bấm "Xác nhận" trong popup. | Có | Có | `RetireInterviewQuestion` | [Các bước]<br>1. Gửi yêu cầu chuyển `status` sang `RETIRED`.<br>2. Đóng popup.<br>3. Điều hướng về `interview_question_management`.<br>[Khi thành công] Câu hỏi biến mất khỏi danh sách phía học viên, vẫn còn trong dữ liệu lịch sử.<br>[Khi lỗi] Giữ popup mở, hiện toast lỗi, không rời màn.<br>[Thông báo hoàn tất] Toast thành công "Đã chuyển câu hỏi sang ngừng dùng." [Nguồn: 05-coding/frontend/src/views/shared/interview-question-authoring/ui/interview-question-authoring-view.tsx:338] |
| 15 | Popup | Xác nhận xoá mềm — "Huỷ" | Bấm "Huỷ" trong popup. | Không | Không | - | [Các bước]<br>1. Đóng popup.<br>[Khi thành công] Câu hỏi giữ nguyên trạng thái. |
| 16 | Nút | Mở xem như học viên | Bấm nút biểu tượng mắt "Xem như học viên" ở thanh đầu trang. | Không | Không | - | [Các bước]<br>1. Mở popup với dữ liệu form hiện tại (kể cả chưa lưu).<br>[Khi thành công] Popup hiển thị nội dung như Chế độ học và Chế độ luyện; Nhóm 3 trống thì hiển thị kèm cảnh báo ẩn khỏi Chế độ luyện. |
| 17 | Popup | Đóng xem như học viên | Bấm "Đóng" trong popup xem trước. | Không | Không | - | [Các bước]<br>1. Đóng popup.<br>[Khi thành công] Quay lại màn soạn, dữ liệu form giữ nguyên. |

[Nguồn: 01-rd/screens/shared/SHR0302_interview_question_authoring.md:53-85, 115-119;
02-bd/database/interview-bank.md:8-96;
05-coding/frontend/src/views/shared/interview-question-authoring/ui/interview-question-authoring-view.tsx:59-65, 74-137]

---

## Sheet 9. Đặc tả kiểm tra

> Bộ thẻ và quy ước mã thông báo: `02-bd/_rules/bd-template-9sheet.md` mục 2, 4. Sheet này ghi **điều kiện
> kiểm và thông báo**; tiêu chí nghiệm thu `AC-nn` thuộc `04-tdd/interview_question_authoring.md`, không
> lặp lại ở đây.

| NO | Loại | Tóm tắt kiểm | Chi tiết | Mức | Mã thông báo | Ghi chú | EVT gọi | Thứ tự |
| --: | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :-: |
| 1 | Kiểm quyền | Quyền truy cập màn | [Nội dung kiểm] Người dùng không có Function `INTERVIEW_BANK_MANAGEMENT` thì không được vào màn.<br>[Nơi thực thi] Chặn ở cả tầng định tuyến phía giao diện và tầng phân quyền phía máy chủ. | Lỗi | Chưa có mã thông báo | Nội dung "Bạn không có quyền truy cập chức năng này." | EVT-1, EVT-2 | 1 |
| 2 | Kiểm nhập liệu | Nội dung câu hỏi bắt buộc | [Nội dung kiểm] Nội dung câu hỏi rỗng thì không cho lưu.<br>[Nơi thực thi] Màn hình và máy chủ.<br>[Tiêu điểm] Viền ô + toast; ô nội dung câu hỏi. | Lỗi | Chưa có mã thông báo | Nội dung "Nội dung câu hỏi không được để trống." | EVT-10, EVT-11 | 1 |
| 3 | Kiểm nhập liệu | Tổng trọng số bộ tiêu chí | [Nội dung kiểm] Có ít nhất một dòng tiêu chí mà tổng trọng số khác 100 thì không cho lưu; Nhóm 3 trống thì không kiểm mục này.<br>[Nơi thực thi] Kiểm ở màn hình khi đổi trọng số, kiểm lại ở máy chủ khi lưu.<br>[Tiêu điểm] Toast khi bấm Lưu lúc nút đang trông mờ; nhãn Tổng trọng số đổi sang màu đỏ (không còn khối chặn lưu trên trang, bỏ 2026-10-03). | Lỗi | Chưa có mã thông báo | Nội dung "Tổng trọng số phải bằng 100%." Máy chủ kiểm lại vì bất biến "tổng bằng 100" thuộc tầng ứng dụng [Nguồn: 02-bd/database/interview-bank.md:91]. | EVT-9, EVT-10, EVT-11 | 2 |
| 4 | Kiểm nhập liệu | Khoảng giá trị trọng số | [Nội dung kiểm] Trọng số của một tiêu chí nằm ngoài khoảng 0 đến 100 thì không cho đặt.<br>[Nơi thực thi] Màn hình.<br>[Tiêu điểm] Viền ô + toast; ô trọng số của dòng vi phạm. | Lỗi | Chưa có mã thông báo | Nội dung "Trọng số phải nằm trong khoảng 0 đến 100." Stepper đã chặn sẵn theo `min`/`max` của component; kiểm này phòng trường hợp nhập trực tiếp [Nguồn: 05-coding/frontend/src/shared/ui/primitives/number-stepper.tsx:17-19]. | EVT-9 | 1 |
| 5 | Kiểm nhập liệu | Tên/mã tiêu chí trùng | [Nội dung kiểm] Hai dòng tiêu chí cùng `criterion_code` trong cùng câu hỏi thì không cho lưu.<br>[Nơi thực thi] Máy chủ.<br>[Tiêu điểm] Viền ô + toast; dòng tiêu chí vi phạm. | Lỗi | Chưa có mã thông báo | Nội dung "Tên tiêu chí bị trùng." Ràng buộc unique `(question_id, criterion_code)` [Nguồn: 02-bd/database/interview-bank.md:94]. | EVT-10, EVT-11 | 3 |
| 6 | Kiểm nghiệp vụ | Xoá mềm không cascade | [Nội dung kiểm] Xoá mềm chỉ chuyển `status` sang `RETIRED`, không xoá `answer_rubrics`/`user_answers`/`recall_ratings` đã có.<br>[Nơi thực thi] Máy chủ. | Thông tin | Chưa có mã thông báo | Không phải lỗi — là bất biến nghiệp vụ, ghi lại để không nhầm sang xoá vật lý [Nguồn: 02-bd/database/interview-bank.md:65, 94-96]. | EVT-14 | 1 |
| 7 | Kiểm nghiệp vụ | Lỗi hệ thống hoặc lỗi gọi máy chủ | [Nội dung kiểm] Gọi máy chủ thất bại hoặc trả lỗi nghiệp vụ thì dừng thao tác, giữ nguyên dữ liệu đang hiển thị.<br>[Nơi thực thi] Màn hình. | Lỗi | Mã lỗi trong phản hồi | Phản hồi có mã lỗi đã đăng ký thì hiển thị nội dung tương ứng; chưa đăng ký thì hiển thị "Không kết nối được máy chủ." | EVT-1, EVT-2, EVT-10, EVT-11, EVT-12, EVT-14 | 1 |

Cột `Thứ tự` là thứ tự kiểm trong cùng một sự kiện.

[Nguồn: 02-bd/database/interview-bank.md:91, 94-96; 01-rd/req/identity.md:55-60]

---

## Câu hỏi mở

| # | Câu hỏi | Vì sao chưa trả lời được | Chủ sở hữu |
| :-: | :--- | :--- | :--- |
| Q1 | ~~Ngưỡng độ dài tối đa cho mỗi dòng "câu hỏi đào sâu" (Nhóm 2) — RD chỉ chốt "không giới hạn số lượng", không chốt giới hạn ký tự mỗi dòng.~~ | RD không nêu con số, bản dựng UI hiện tại cũng không đặt `maxLength` [Nguồn: 05-coding/frontend/src/views/shared/interview-question-authoring/ui/interview-question-authoring-view.tsx:185-192]. **ĐÃ CHỐT 2026-10-01 (owner uỷ quyền cân nhắc), xem `DEC-2026-1001-admin-configurable-settings`:** không đặt giới hạn ký tự nghiệp vụ cho mỗi dòng đào sâu; chỉ giữ trần kỹ thuật (kích thước body request) và báo lỗi rõ khi vượt. Màn dùng chung cho A2 và A3 nên áp cùng một quy tắc cho cả hai. Câu hỏi đóng. | Đã đóng (DD `interview-bank` chốt trần kỹ thuật) |
| Q2 | Popup "Xem như học viên" hiển thị dữ liệu form hiện tại (chưa lưu) hay bắt buộc lưu trước? Có gọi `GetInterviewQuestionDetail` hay hoàn toàn dựng phía client từ state đang có? | RD chỉ nói có nút preview, không nói rõ nguồn dữ liệu; bản dựng UI hiện tại có nút nhưng chưa gắn hành vi [Nguồn: 01-rd/screens/shared/SHR0302_interview_question_authoring.md:55-56, 116; 05-coding/frontend/src/views/shared/interview-question-authoring/ui/interview-question-authoring-view.tsx:161]. | DD `interview-bank` + DD màn hình |
| Q3 | Ba trường `suggested_approach` (F6-04), `sample_answer_framework` (F6-05), `core_keywords` (F6-06) tồn tại trong `interview_questions` nhưng không có nhóm trường nào ở màn này (RD chỉ chốt 4 nhóm) hay ở bản dựng UI hiện tại biên soạn chúng. Ai/màn nào tạo dữ liệu cho ba trường này — seed thủ công, một nhóm thứ 5 còn thiếu trong RD, hay một cơ chế khác (ví dụ AI sinh tự động)? | Đây là khoảng trống thật giữa schema DB (F6-04/05/06 đã có cột) và phạm vi màn soạn nội dung duy nhất của Bounded Context này (F6-13) — không suy đoán "chắc có nhóm ẩn nào đó" [Nguồn: 01-rd/screens/shared/SHR0302_interview_question_authoring.md:49-71; 02-bd/database/interview-bank.md:61-63]. | Chủ dự án (có thể cần một DEC nếu quyết định thêm Nhóm 5) |
| Q4 | ~~Có ngưỡng số dòng tối đa cho bộ tiêu chí đánh giá (Nhóm 3) không, hay hoàn toàn không giới hạn như Nhóm 2?~~ | RD không nêu trần cho Nhóm 3, chỉ nêu rõ cho Nhóm 2. **ĐÃ CHỐT 2026-10-01 (owner uỷ quyền cân nhắc), xem `DEC-2026-1001-admin-configurable-settings`:** không giới hạn số dòng tiêu chí; ràng buộc duy nhất là tổng trọng số bằng 100% (RD đã chốt ở `01-rd/screens/shared/SHR0302_interview_question_authoring.md:69-71, 115`, Q1 của RD) và mã tiêu chí không trùng trong cùng một câu hỏi. Cùng quy tắc cho A2 và A3 vì màn dùng chung. Câu hỏi đóng. | Đã đóng |
| Q5 | `title` của câu hỏi lấy từ đâu — người dùng nhập riêng, hay hệ thống tự sinh từ vài từ đầu của `content_markdown`? Bản dựng UI hiện tại không có ô nhập tiêu đề riêng, chỉ có ô nội dung câu hỏi. | RD nói "mã/tiêu đề câu hỏi đang sửa" ở thanh đầu trang nhưng không mô tả cách nhập; DB có cột `title` độc lập với `content_markdown` [Nguồn: 01-rd/screens/shared/SHR0302_interview_question_authoring.md:53-55; 02-bd/database/interview-bank.md:59; 05-coding/frontend/src/views/shared/interview-question-authoring/ui/interview-question-authoring-view.tsx:171-179]. | DD `interview-bank` + DD màn hình |
| Q6 | Danh sách `criterion_code` gợi ý mặc định cho A2/A3 khi soạn tiêu chí mới — có seed sẵn vài tiêu chí phổ biến (ví dụ "Tính đúng đắn", "Độ đầy đủ", "Rõ ràng") hay để trống hoàn toàn tự đặt như bản dựng UI hiện tại? | Schema chỉ ghi `criterion_code` do A2/A3 tự đặt [Nguồn: 02-bd/database/interview-bank.md:89]; file BD database hiện không có mục 7 nào nêu câu hỏi này (dẫn chiếu cũ "mục 7" không còn đúng) `[SoT: Suy luận]`; chưa có câu trả lời | DD `interview-bank` |
| Q7 | Trường `description` của `answer_rubrics` (mô tả tiêu chí) không có ô nhập ở màn này theo cả RD lẫn bản dựng UI hiện tại — có bổ sung ô nhập không, hay để trống vĩnh viễn (NULL) và trường này chỉ phục vụ mục đích khác? | RD (dòng 69-71) chỉ nói "mỗi tiêu chí có trọng số phần trăm", không nhắc tới mô tả; DB có cột `description` [Nguồn: 02-bd/database/interview-bank.md:90]. | DD `interview-bank` |
| Q9 | ~~Quản lý danh mục độ khó (thêm, đổi tên, sắp xếp, xoá) của ADMIN nằm ở đâu — popup trên `SHR0301` như "Quản lý chủ đề", hay màn riêng? Màn soạn này có hiện liên kết tới đó cho ADMIN như với chủ đề không?~~ | **ĐÃ CHỐT theo bản dựng, 2026-10-03, theo uỷ quyền của chủ dự án ("xử lý các nợ còn lại"):** quản lý độ khó mở từ popup ở `SHR0301` (chỉ ADMIN), màn soạn không có liên kết; chờ chủ dự án xác nhận nếu muốn khác. Căn cứ: `SHR0301` có mục chuyển màn "Popup Quản lý độ khó"; màn soạn chỉ đọc danh mục qua `levelList` và không có liên kết quản lý [Nguồn: 05-coding/frontend/src/views/shared/interview-question-authoring/ui/interview-question-authoring-view.tsx:91, 299-304; 02-bd/screens/shared/SHR0301_interview_question_management.md, mục 3.1 "Quản lý ngân hàng câu hỏi → Popup Quản lý độ khó"]. | Đã đóng (chờ chủ dự án xác nhận nếu muốn khác) |

**Nợ prototype**: đã có mục ghi nhận cho slug `interview_question_authoring` ở `06-plan/PROTOTYPE_DEBT.md`
[Nguồn: 06-plan/PROTOTYPE_DEBT.md:863].
