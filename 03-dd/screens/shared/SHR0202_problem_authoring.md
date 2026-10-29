# DD màn hình — Biên soạn bài tập (`SHR0202`)

## 0. Thông tin tài liệu

| Mục | Giá trị |
| :--- | :--- |
| Actor, route | A2/A3; `{basePath}/new`, `{basePath}/{problemId}/edit`, `basePath` là `/instructor/problems` hoặc `/admin/problems` |
| Nguồn | `01-rd/screens/shared/SHR0202_problem_authoring.md`; `02-bd/screens/shared/SHR0202_problem_authoring.md` Sheet 1–9; [F2](../../api/F2_problem-bank.md) §20, §15, §34–49 |
| Prototype | `05-coding/frontend/src/views/shared/problem-authoring/ui/problem-authoring-view.tsx` |
| Phiên bản | V0.1, 2026-10-25; DD màn, Mode B |

## 1. Bản đồ slice FSD

| Lớp | Đường dẫn từ `src/` | Vai trò |
| :--- | :--- | :--- |
| app | `app/(admin)/admin/problems/{new,[problemId]/edit}/page.tsx`, route tương ứng `(instructor)` | Truyền `problemId`, `basePath`, actor |
| views | `views/shared/problem-authoring/ui/` | Năm tab, cột thuộc tính, preview, trạng thái lưu/checklist |
| features | `features/author-problem/`, `features/manage-testcases/` | Bản nháp, mutation nội dung/spec/testcase, publish |
| entities | `entities/problem/api/queries.ts`, `mutations.ts` | F2 §15, §20, §34–49 |
| shared | `MarkdownEditor`, `CodeEditor`, `DataTable`, `ConfirmDialog`, `FilePicker` | Hai actor dùng chung, dialog trả focus |

## 2. API tiêu thụ

| EVT BD | API | Hành vi màn |
| :--- | :--- | :--- |
| 1 sửa | F2 §34 `GET /authoring/problems/{problemId}`, §41 stats, §20 topics, §15 levels | Tải độc lập theo khối; route UUID |
| 2 tạo mới/nhân bản | F2 §20, §15; §35 `GET /authoring/problems/{sourceId}/duplicate-source` nếu `from` | Form chưa lưu, không tạo bản ghi trước nút Lưu |
| 4–5 preview | Không gọi API | Render bản nháp form hiện tại đã sanitize |
| 7–14 nội dung/ví dụ | F2 §36 POST mới hoặc §37 PUT nội dung khi bấm Lưu | Không gửi từng phím gõ; bản nháp local |
| 15 tải testcase | F2 §46 URL upload, §47 confirm | Tệp lên kho theo URL được cấp, confirm sau upload thành công |
| 16 sinh testcase | F2 §49 `POST .../testcases/generate` | AI tạo nháp; không tự duyệt/xuất bản |
| 17, 19–24 testcase | F2 §42–45 tạo/sửa/xoá/đổi thứ tự | Mutation riêng theo testcase; duyệt AI qua §43 `approved=true` |
| 18, 22 chạy | F2 §48 chạy đáp án mẫu với testcase | Kết quả thật từ judge, không toast Pass giả |
| 25–27 đặc tả | F2 §38 `PUT .../spec` | Lưu spec của bài đã có ID |
| 25–27 xem trước chữ ký | F3 §3.7 `POST /function-signature/preview` | Gửi bản nháp đặc tả chưa lưu, nhận dòng khai báo JAVA/CPP/PYTHON; tab chọn ngôn ngữ dùng kết quả đã nhận |
| 28–29 AI context | F2 §39 `PUT .../ai-context` | Lưu chỉ dẫn/cờ của bài đã có ID |
| 31–33 thuộc tính/publish | F2 §37/§36 nội dung và §40 trạng thái | Lưu content trước, publish sau khi server xác nhận checklist |

EVT-30 “Nhờ AI soạn nháp” đã cắt. Không có ô “Stack (MB)” ở chi tiết hoặc biên soạn theo quyết định owner 2026-10-25; F2 DB/API không có trường này.

## 3. Trạng thái màn

| Trạng thái | Xử lý |
| :--- | :--- |
| Bài sửa không tồn tại/ngoài quyền | F2 404/403, trang lỗi với liên kết danh sách đúng actor |
| Form dirty | Cảnh báo khi rời nội bộ/reload, Hủy rời giữ form |
| Đang lưu | Khoá nút tương ứng, không ghi đè thay đổi người dùng gõ sau khi request bắt đầu |
| AI/runner lỗi | Giữ nội dung và testcase hiện có, báo `error.code` `PRB-*`/`JDG-*`; không hiện kết quả giả |
| Checklist chưa đủ | Nút vẫn focus/click được để nêu mục thiếu; không gọi publish nếu đã biết thiếu, server kiểm lại |
| Bài đã xuất bản có thay đổi | Server quyết định version testcase/spec; view làm mới checklist và baseline sau response |

## 4. Quy tắc đặc thù màn

### BR-SC-01 Thứ tự lưu và xuất bản

Tạo mới gọi F2 §36 lấy UUID rồi chuyển URL edit; mọi mutation phụ thuộc ID chỉ bật sau đó. Bấm “Lưu và xuất bản” lưu nội dung, spec và AI context đang dirty theo thứ tự cần thiết, đợi response, rồi gọi F2 §40. Nếu một bước lỗi, dừng và giữ trạng thái chưa xuất bản; không báo thành công. Chọn `PUBLISHED` ở cột thuộc tính dùng cùng checklist; không cho đường tắt qua việc đổi select. Checklist là gợi ý client; F2 §40 là thẩm quyền cuối. [SoT: BD EVT-32–33, Q8; F2 §36–40]

### BR-SC-02 Ví dụ, testcase và phiên bản

Xoá ví dụ khi bài PUBLISHED còn đúng hai ví dụ bị chặn với toast giải thích; nút vẫn hiển thị. Testcase AI sinh là nháp cho tới khi người soạn duyệt `approved=true` qua §43. Toggle Công khai/Ẩn nằm ngay trên dòng, gửi §43; nếu đổi Sample đã duyệt sang Hidden làm còn dưới hai Sample thì `PRB-054` giữ toggle cũ. Xoá testcase mà bài PUBLISHED sẽ còn dưới 8 ca duyệt hoặc 2 Sample cũng bị `PRB-054`; client có thể báo trước từ checklist nhưng máy chủ là nguồn quyết định. Kéo thả gửi §45 sau khi người dùng thả, lỗi trả về thứ tự server. Chạy đáp án mẫu cần mã và testcase đã duyệt; F2 §48 trả kết quả thật. Duyệt testcase Hidden của bài PUBLISHED có thể tăng `current_testcase_set_version`; lấy response và làm mới để tránh hiển thị phiên bản cũ. [SoT: BD EVT-14–24, F2 §42–49; quyết định owner 2026-10-25]

### BR-SC-03 Nhân bản và preview

`from` là UUID bài nguồn, F2 §35 điền bản nháp nhưng không giữ ID/đếm thống kê/status xuất bản của nguồn. Preview dựng từ bản nháp chưa lưu, sanitize Markdown, không gọi GET lại. Chỉ khi bấm Lưu mới POST §36 tạo bài mới. [SoT: BD EVT-2,4–5; F2 §35]

### BR-SC-04 Ghép cặp tệp testcase

EVT-15 chọn nhiều tệp văn bản có cùng tên gốc và hai hậu tố `.in`/`.out` (ví dụ `case01.in`, `case01.out`); một tên gốc tạo đúng một cặp `clientKey`. So khớp tên gốc phân biệt hoa thường, không chấp nhận tệp thiếu vế, trùng vế hoặc tên gốc rỗng; hiện lỗi trước khi xin URL F2 §46. Không nhận ZIP vì F2 không có hợp đồng giải nén. `visibility` mặc định HIDDEN cho cặp mới; người soạn có thể đổi từng cặp trước khi xin URL. Sau #46, PUT hai URL của mỗi cặp và chỉ gọi #47 khi mọi vế đã tải thành công; lỗi tải giữ danh sách để thử lại trong hạn URL. F2 #47 xác nhận nguyên lô, không thêm một phần. [SoT: F2 §46–47, Q-08; quy ước tệp là quyết định DD màn]

### BR-SC-05 Xem trước dòng khai báo hàm

Khi bản nháp `functionName`, kiểu trả về và tham số thay đổi, đợi 400 ms sau lần gõ cuối rồi gọi F3 §3.7; không gọi khi tab Đặc tả chưa mở. Phản hồi chứa cả ba ngôn ngữ; đổi tab ngôn ngữ chỉ đổi dòng đang hiển thị. Hủy request cũ khi sửa tiếp; chỉ áp kết quả nếu body gửi đi còn bằng bản nháp hiện tại. `wrapperSupported=false` hiển thị lý do và không bịa chữ ký; `HRN-060` đánh dấu trường theo `details.violations`; `429 HRN-061` giữ dòng gần nhất kèm trạng thái đang chờ, tự thử lại sau `retryAfterSeconds` nếu bản nháp không đổi. Preview không lưu đặc tả; F2 §38 vẫn là đường ghi duy nhất. [Owner 2026-10-25; F3 §3.7]

## 5. Kiểm tra phía client

| Trường | Quy tắc | Nguồn |
| :--- | :--- | :--- |
| Tiêu đề, Markdown, ví dụ | Theo validation F2; dữ liệu rỗng/giới hạn server kiểm lại | F2 §36–37 |
| Giới hạn thời gian/bộ nhớ/output/lượt nộp | Số nguyên dương và cận theo F2; **không có Stack** | F2 §36–37, quyết định owner 2026-10-25 |
| Chủ đề, độ khó | ID từ F2 §20/§15, không dùng mã mock | F2 §36–37 |
| Spec/chữ ký/epsilon | Theo validation F2 và lược đồ harness; epsilon bắt buộc khi EPSILON; giá trị giữ ở chiến lược khác cũng phải dương, biểu diễn chính xác theo NUMERIC(18,12) (validation F2) | F2 §38 |
| Testcase | Input/output bắt buộc theo F2; vị trí và cờ server kiểm | F2 §42–45 |
| Tệp testcase | Loại/kích thước/header theo F2 §46–47 | validation F2 |
| Ghép cặp upload | `.in`/`.out` cùng tên gốc, đúng hai vế, không trùng; tối đa 100 cặp và 256 MB toàn lô | F2 §46–47; BR-SC-04 |

## 6. i18n và truy cập

Dùng `problemAuthoring.*`, `apiError.PRB-*`, `apiError.JDG-*`. Năm tab có `aria-selected`; trạng thái lưu/checklist phát qua `aria-live=polite`. Drag-drop testcase có nút Lên/Xuống dùng bàn phím với cùng mutation §45. Editor có nhãn, lỗi gắn trường. Preview sanitize Markdown/HTML; mã và lời giải người học là dữ liệu, không nối vào chỉ thị AI. Không đưa tên API hay mã quyền vào nhãn người dùng.

## 7. Điểm kiểm thử

| ID | Loại | Nội dung | Công cụ | AC |
| :--- | :--- | :--- | :--- | :--- |
| PA-01 | Normal | EVT-1/2 sửa/tạo/nhân bản; nhân bản chưa POST | Vitest | TDD màn `PA-AC-01` |
| PA-02 | Normal | EVT-4–14 preview bản nháp, ví dụ và chặn sàn hai ví dụ | Vitest | TDD màn `PA-AC-01` |
| PA-03 | Abnormal | EVT-15–18 upload/AI/runner lỗi không tạo Pass giả hoặc duyệt ngầm | Vitest | TDD màn `PA-AC-02` |
| PA-04 | Normal | EVT-19–24 CRUD, toggle trên dòng, sắp xếp, bàn phím | Playwright | TDD màn `PA-AC-02`, `PA-AC-04` |
| PA-05 | Boundary | EVT-25–29 spec/AI context, epsilon và version | Vitest | TDD màn `PA-AC-05` |
| PA-06 | Abnormal | EVT-31–33 lưu từng bước, publish lỗi giữ chưa xuất bản | Vitest | TDD màn `PA-AC-03` |
| PA-07 | Normal | EVT-34 guard dirty khi quay lại/reload, Hủy rời giữ form | Playwright | TDD màn `PA-AC-03` |
| PA-08 | Boundary | Không có ô Stack ở form và chi tiết | Playwright | TDD màn `PA-AC-03` |
| PA-09 | Boundary | Thiếu/trùng `.in`/`.out` bị chặn trước #46; #47 chỉ gọi sau mọi PUT thành công | Vitest | TDD màn `PA-AC-02` |
| PA-10 | Boundary | Bài PUBLISHED ở ngưỡng 8 ca/2 Sample: đổi hoặc xoá làm tụt ngưỡng trả `PRB-054`, không đổi UI | Vitest | TDD màn `PA-AC-02` |
| PA-11 | Normal | Bản nháp chữ ký xem trước từ F3 cho ba ngôn ngữ; đổi tab không gọi lại, phản hồi cũ không ghi đè bản nháp mới | Vitest | TDD màn `PA-AC-01` |

## 8. Nợ prototype và câu hỏi mở

Prototype còn mock store, kết quả chạy mẫu giả, toggle công khai trong dialog và chọn PUBLISHED chưa áp checklist. Cần nối F2 §34–49, sửa các chỗ này và bỏ ô Stack theo owner. Các Q đã chốt ở BD được phản ánh ở mục 4; không còn câu hỏi nghiệp vụ mức màn. `PROTOTYPE_DEBT.md` chỉ gỡ khi nối API thật. TDD màn đã có tại `04-tdd/screens/shared/SHR0202_problem_authoring.md`.

### Đồng bộ baseline 2026-10-29

Theo `DEC-2026-1029-f2-migration-readiness`, xuất bản lại sau khi sửa testcase trong lúc rút bài phải dùng phiên bản bộ chấm mới nếu nội dung đã duyệt khác snapshot; giống thì không tăng. Giao diện dùng version backend trả, không tự tính. Tag khác case là cùng một tag. Epsilon ngoài miền trả PRB-036/HRN-V20, không âm thầm làm tròn. Các câu hỏi backend của màn đã đóng theo DD F2 §61; nợ prototype/nối API không phải câu hỏi thiết kế.
