# BD — Màn `interview_question_authoring` (Soạn câu hỏi phỏng vấn)

> Slug: `interview_question_authoring`. Bounded Context chủ: `interview-bank` (F6-13). Actor: A2 và A3 —
> màn dùng chung, cùng route pattern `/instructor/interview-questions/[id]` và
> `/admin/interview-questions/[id]` [SoT: 01-rd/screens/shared/interview_question_authoring.md:14-15].
> Gác bởi Function `INTERVIEW_BANK_MANAGEMENT` [SoT: 01-rd/req/identity.md:60, F1-12].
>
> **Màn này chưa có prototype** (`[Đợi nextjs]`) [SoT: 01-rd/screens/shared/interview_question_authoring.md:12].
> Cấu trúc dưới đây suy diễn song song theo tiền lệ `problem_authoring` — một màn soạn riêng cho một bản
> ghi phức tạp, không phải modal/drawer — đúng như RD đã dẫn
> [SoT: 01-rd/screens/shared/interview_question_authoring.md:16-20]. Vì chưa có
> `09-layoutBase` để đối chiếu, mục 1 mô tả layout theo **cấu trúc (regions/states)**, không suy diễn màu
> sắc/khoảng cách/typography cụ thể — đúng hướng dẫn Layer 3 của `bd-generation`
> [SoT: .claude/skills/bd-generation/SKILL.md:66-68]. Nợ prototype ghi lại ở mục 7.
>
> File này mô tả **layout, component, trạng thái màn, danh sách API tiêu thụ (chỉ tên + BC sở hữu)** —
> không lặp lại hành vi nghiệp vụ đã có ở RD, không viết request/response (thuộc DD,
> `03-dd/api/interview-bank.md`, chưa viết), không viết schema/logic backend (thuộc
> `02-bd/{architecture,database,security}/interview-bank.md`).

## 1. Bố cục màn (layout regions)

Suy diễn theo đúng khuôn `problem_authoring`: thanh đầu trang dính (sticky) + vùng nội dung chia theo 4
nhóm trường đã chốt ở RD + khu vực hành động quản trị. **Khác `problem_authoring` ở một điểm cấu trúc**:
RD của màn này không mô tả layout dạng "tab" hay "cột thuộc tính bên phải" — chỉ chốt "4 nhóm trường lồng
nhau" [SoT: 01-rd/screens/shared/interview_question_authoring.md:48-50]. BD đề xuất trình bày 4 nhóm theo
chiều dọc, tuần tự (không tab), vì khối lượng trường của một câu hỏi phỏng vấn nhỏ hơn nhiều so với một
bài toán (không có testcase, không có đặc tả ngôn ngữ) — không cần chia tab để tránh cuộn dài
`[SoT: Suy luận — quy mô dữ liệu nhỏ hơn problem_authoring, không có căn cứ RD nào ép buộc dùng tab]`.
Cách chia tab/không-tab là quyết định trình bày, không phải quyết định kiến trúc, nên không cần một DEC
riêng; ghi lại như một câu hỏi mở (mục 7, Q-BD1) để chủ dự án xác nhận khi dựng UI thật.

### 1.1. Thanh đầu trang (sticky)

| Region | Nội dung | Nguồn |
| :--- | :--- | :--- |
| Nút quay lại | Về `interview_question_management` | [SoT: 01-rd/screens/shared/interview_question_authoring.md:52] |
| Định danh | Mã/tiêu đề câu hỏi đang sửa; rỗng khi tạo mới | [SoT: 01-rd/screens/shared/interview_question_authoring.md:52] |
| Trạng thái lưu | Chỉ báo đã lưu / đang lưu — **không có khái niệm nháp/xuất bản riêng**, khác `problem_authoring` (RD đã chốt Q5: câu hỏi hiện ngay khi lưu) | [SoT: 01-rd/screens/shared/interview_question_authoring.md:54-56, mục 5 Q5] |
| Nút "Xem như học viên" | Preview cả Chế độ học và Chế độ luyện | [SoT: 01-rd/screens/shared/interview_question_authoring.md:53, mục 5 Q2] |
| Nút "Lưu" | Một hành động duy nhất — không có "Lưu và xuất bản" | [SoT: 01-rd/screens/shared/interview_question_authoring.md:54-56] |

### 1.2. Nhóm 1 — Nội dung và phân loại

Trường: nội dung câu hỏi (Markdown), chủ đề (chọn 1 trong 5 giá trị cố định, không cho A2/A3 tự thêm chủ
đề mới ở màn này), độ khó [SoT: 01-rd/screens/shared/interview_question_authoring.md:57-58]. Danh sách 5
chủ đề là dữ liệu seed dùng chung, tải một lần từ `question_topics`, không phải trường nhập tự do
[SoT: 02-bd/architecture/interview-bank.md:56-62].

### 1.3. Nhóm 2 — Câu hỏi đào sâu

Danh sách văn bản tự do, không giới hạn số lượng, không có độ khó riêng cho từng câu đào sâu — thêm/xoá
từng dòng tự do [SoT: 01-rd/screens/shared/interview_question_authoring.md:59-62]. Component: danh sách
động (thêm dòng, xoá dòng), mỗi dòng một textarea ngắn.

### 1.4. Nhóm 3 — Bộ tiêu chí đánh giá có trọng số

Danh sách tiêu chí, mỗi dòng gồm tên/mã tiêu chí + mô tả + trọng số phần trăm; hiển thị **tổng trọng số
hiện tại** liên tục khi người dùng sửa, và **chặn Lưu nếu tổng khác 100**
[SoT: 01-rd/screens/shared/interview_question_authoring.md:63-65, 02-bd/database/interview-bank.md:56-62
(bảng `answer_rubrics`, ràng buộc tổng = 100 kiểm ở tầng ứng dụng)]. Khối này để trống hợp lệ (câu hỏi
thiếu rubric vẫn lưu được, chỉ bị ẩn khỏi Chế độ luyện) — không chặn Lưu khi trống, chỉ chặn khi có dòng
mà tổng khác 100 [SoT: 01-rd/screens/shared/interview_question_authoring.md:64, 02-bd/database/interview-bank.md:43-46].

### 1.5. Nhóm 4 — Hành động quản trị

Nút "Nhân bản" (sao chép toàn bộ 4 nhóm sang một bản ghi mới, chưa lưu) và "Xoá mềm" (đánh dấu ngừng
dùng, có hộp thoại xác nhận) [SoT: 01-rd/screens/shared/interview_question_authoring.md:66-67]. Đặt ở
cuối màn hoặc trong menu ngữ cảnh của thanh đầu trang — chi tiết vị trí cụ thể để ngỏ cho `[Đợi nextjs]`.

### 1.6. Ngoài phạm vi layout màn này

Bốn thẻ chỉ số chất lượng nội dung (tổng câu hỏi, thiếu rubric, điểm trung bình 30 ngày, chưa dùng lần
nào) **không thuộc màn này** — hiển thị ở màn cha `interview_question_management`, chỉ nêu lại trong RD
để tránh nhầm lẫn, không lặp lại ở BD này
[SoT: 01-rd/screens/shared/interview_question_authoring.md:68-71].

## 2. Component inventory

| Component | Vai trò | Ghi chú |
| :--- | :--- | :--- |
| `AuthoringHeaderBar` | Thanh đầu trang sticky | Nút quay lại, định danh, trạng thái lưu, preview, Lưu |
| `MarkdownEditorField` | Soạn nội dung câu hỏi | Cùng loại component đã dùng ở `problem_authoring` cho đề bài Markdown — tái dùng, không dựng riêng `[SoT: Suy luận, nhất quán component giữa hai màn soạn nội dung]` |
| `TopicSelect` | Chọn 1 trong 5 chủ đề cố định | Dữ liệu nạp từ danh mục `question_topics`, không phải input tự do |
| `DifficultySelect` | Chọn độ khó | `EASY`/`MEDIUM`/`HARD` |
| `DynamicTextList` | Danh sách câu hỏi đào sâu | Thêm dòng/xoá dòng, không giới hạn số lượng |
| `WeightedRubricTable` | Bảng tiêu chí đánh giá có trọng số | Cột: tên/mã tiêu chí, mô tả, trọng số (%); dòng tổng luôn hiển thị, đổi màu cảnh báo khi khác 100 (đề xuất, không định màu cụ thể `[Đợi nextjs]`) |
| `DuplicateActionButton` | Nhân bản | Điều hướng sang bản ghi mới với dữ liệu đã sao chép, chưa lưu |
| `SoftDeleteConfirmDialog` | Xác nhận xoá mềm | Nội dung cảnh báo: không cascade xoá phiên phỏng vấn cũ |
| `PreviewAsStudentPanel` | "Xem như học viên" | Xem trước cả Chế độ học và Chế độ luyện, cùng khuôn `problem_authoring` |
| `SaveStatusIndicator` | Trạng thái lưu | Không có nhãn "đã xuất bản"/"bản nháp" — chỉ "đã lưu" / "đang lưu" / "lưu thất bại" |

## 3. Trạng thái màn (screen states)

| Trạng thái | Điều kiện | Hành vi |
| :--- | :--- | :--- |
| Tạo mới | Route không có `id` hợp lệ, hoặc từ nút "Thêm câu hỏi" ở màn cha | 4 nhóm trống, nút Lưu tạo bản ghi mới [SoT: 01-rd/screens/shared/interview_question_authoring.md:77-78] |
| Đang sửa | Route có `id` hợp lệ | Tải dữ liệu hiện có vào 4 nhóm, nút Lưu cập nhật cùng bản ghi |
| Lưu bị chặn — tổng trọng số sai | Có ít nhất một dòng tiêu chí và tổng khác 100 | Nút Lưu vô hiệu hoặc báo lỗi tại chỗ, không gửi request; thông báo "Tổng trọng số phải bằng 100" [SoT: 01-rd/screens/shared/interview_question_authoring.md:64] |
| Lưu thành công — thiếu rubric | Nhóm 3 trống khi lưu | Lưu bình thường; hiển thị cảnh báo không chặn: câu hỏi sẽ ẩn khỏi Chế độ luyện, vẫn hiện ở Chế độ học [SoT: 01-rd/screens/shared/interview_question_authoring.md:79-81] |
| Đang tải | Vào màn ở chế độ sửa, chờ dữ liệu | Khung xương (skeleton) toàn bộ 4 nhóm — không suy diễn chi tiết hoạt ảnh `[Đợi nextjs]` |
| Lỗi tải | API đọc chi tiết câu hỏi lỗi hoặc không tìm thấy | Thông báo lỗi + nút quay lại `interview_question_management` |
| Đang lưu | Sau khi bấm Lưu, trước khi có phản hồi | Vô hiệu hoá nút Lưu, tránh double-submit |
| Lưu thất bại | API lưu trả lỗi (validation khác, lỗi hệ thống) | Giữ nguyên dữ liệu đã nhập, hiển thị lỗi tại chỗ, không mất dữ liệu người dùng đã gõ |
| Xem trước | Bấm "Xem như học viên" | Mở panel/route riêng hiển thị câu hỏi như Chế độ học và Chế độ luyện, không rời khỏi trang soạn (dữ liệu chưa lưu hiển thị theo trạng thái hiện tại trong form, không phải bản đã lưu) `[SoT: Suy luận, song song tiền lệ problem_authoring]` |
| Xác nhận xoá mềm | Bấm "Xoá mềm" | Hộp thoại xác nhận; xác nhận xong điều hướng về `interview_question_management`, câu hỏi chuyển `RETIRED` [SoT: 01-rd/screens/shared/interview_question_authoring.md:66-67, 82-84] |
| Nhân bản | Bấm "Nhân bản" | Điều hướng sang bản ghi mới (state Tạo mới) với 4 nhóm đã điền sẵn dữ liệu sao chép, chưa có `id`, chưa lưu [SoT: 01-rd/screens/shared/interview_question_authoring.md:66-67, 93] |
| Không đủ quyền | Người dùng không có Function `INTERVIEW_BANK_MANAGEMENT` | Chặn truy cập route, cùng cơ chế gác các màn quản trị nội dung khác [SoT: 01-rd/req/identity.md:60, F1-12] |

## 4. API tiêu thụ (chỉ liệt kê tên + Bounded Context sở hữu — hợp đồng chi tiết ở DD)

| Hành động màn | Endpoint (tên nghiệp vụ, chưa chốt path) | BC sở hữu |
| :--- | :--- | :--- |
| Tải danh mục 5 chủ đề | Lấy danh sách `question_topics` | `interview-bank` |
| Tải chi tiết một câu hỏi để sửa | Lấy chi tiết câu hỏi theo `id` (kèm `answer_rubrics`, `follow_up_questions`) | `interview-bank` |
| Tạo câu hỏi mới | Tạo câu hỏi (F6-13) | `interview-bank` |
| Cập nhật câu hỏi | Cập nhật câu hỏi theo `id` (F6-13) | `interview-bank` |
| Nhân bản câu hỏi | Nhân bản câu hỏi theo `id` (F6-13) | `interview-bank` |
| Xoá mềm câu hỏi | Đánh dấu ngừng dùng theo `id` (F6-13) | `interview-bank` |
| Xem trước "như học viên" | Tái dùng endpoint đọc chi tiết câu hỏi (Chế độ học) — không cần endpoint riêng cho preview `[SoT: Suy luận]` | `interview-bank` |

Hợp đồng request/response cụ thể — bao gồm cách kiểm tổng trọng số 100 phía server, mã lỗi validation —
để ở `03-dd/api/interview-bank.md` (chưa viết), đúng anti-drift rule của `bd-generation`
[SoT: .claude/skills/bd-generation/SKILL.md:46-49].

## 5. Điều hướng (navigation)

- Vào màn: từ `interview_question_management` — nút "Thêm câu hỏi" (tạo mới, không có `id`) hoặc bấm vào
  một dòng câu hỏi trong danh sách (sửa, có `id`)
  [SoT: 01-rd/screens/shared/interview_question_management.md — màn cha/nguồn].
- Rời màn: nút "Quay lại" ở thanh đầu trang, hoặc tự động sau khi Xoá mềm/Lưu thành công (đề xuất ở lại
  màn sau Lưu để tiếp tục sửa, không tự điều hướng — khác hành vi sau Xoá mềm) `[SoT: Suy luận]`.
- Route mount kép: `/instructor/interview-questions/[id]` và `/admin/interview-questions/[id]`, cùng một
  view/BD, khác nhau chỉ ở tiền tố layout khu vực
  [SoT: 01-rd/screens/shared/interview_question_authoring.md:14-15,
  DEC-2026-0825-shared-content-authoring-screens].
- `[id]` bằng `new` hoặc rỗng (tuỳ quy ước Next.js dynamic route khi dựng UI thật) tương ứng trạng thái
  Tạo mới — chi tiết quy ước route cụ thể để ngỏ cho `[Đợi nextjs]`.

## 6. Quyền truy cập (access rights)

- Actor: A2 (Giảng viên) và A3 (Quản trị viên) — màn dùng chung, gác bởi Function
  `INTERVIEW_BANK_MANAGEMENT` [SoT: 01-rd/req/identity.md:60, F1-12].
- **Không có phạm vi "theo lớp" cho thao tác sửa** — A2 sửa được toàn bộ ngân hàng câu hỏi hệ thống, không
  giới hạn theo `created_by` hay lớp phụ trách; cột `created_by` chỉ phục vụ hiển thị/audit, không phải
  điều kiện lọc quyền. Quyết định này đã đóng 2026-09-13, căn cứ trực tiếp Given-When-Then của màn cha
  [SoT: 02-bd/security/interview-bank.md:34-43, 01-rd/screens/shared/interview_question_management.md:110-112].
- Học viên (A1) không truy cập màn này — màn phía học viên tương ứng là `interview_question_detail`
  (Chế độ học/Chế độ luyện), tách biệt hoàn toàn theo Function
  [SoT: 01-rd/req/identity.md:60, 02-bd/security/interview-bank.md mục 2].
- Mọi thao tác tạo/sửa/nhân bản/xoá ghi vào Nhật ký hệ thống (F1-14)
  [SoT: 01-rd/screens/shared/interview_question_authoring.md:42, F1-14].

## 7. Câu hỏi mở

| # | Câu hỏi | Vì sao chưa trả lời được | Đề xuất | Chủ sở hữu |
| :-: | :--- | :--- | :--- | :--- |
| Q-BD1 | Trình bày 4 nhóm trường theo tab (như `problem_authoring`) hay tuần tự theo chiều dọc (đề xuất của BD này, mục 1)? | RD chỉ chốt "4 nhóm trường lồng nhau", không chốt cách trình bày; `problem_authoring` dùng tab nhưng khối lượng dữ liệu khác nhau đáng kể | Giữ tuần tự dọc do khối lượng nhỏ hơn; xác nhận khi dựng Next.js | Chủ dự án |
| Q-BD2 | Vị trí cụ thể của khối "Nhân bản"/"Xoá mềm" (nhóm 4) trên màn — cuối trang hay menu ngữ cảnh ở thanh đầu trang? | Không có prototype để đối chiếu, RD không chốt vị trí | Menu ngữ cảnh (kebab menu) ở thanh đầu trang, gần nút Lưu — nhất quán với các màn quản trị khác | Chủ dự án khi `[Đợi nextjs]` |
| Q-BD3 | "Xem như học viên" hiển thị dữ liệu form hiện tại (chưa lưu) hay bắt buộc lưu trước rồi mới xem? | RD chỉ nói có nút preview, không nói rõ nguồn dữ liệu preview | Preview theo dữ liệu form hiện tại (chưa lưu), giống hành vi hợp lý nhất của một preview tại chỗ | Chủ dự án |
| Q-BD4 | Ngưỡng độ dài tối đa cho danh sách "câu hỏi đào sâu" (nhóm 2) — RD nói "không giới hạn số lượng" nhưng không nói giới hạn ký tự mỗi dòng | Không có căn cứ RD | Áp cùng ngưỡng độ dài văn bản tự do hợp lý (ví dụ 500 ký tự/dòng), chốt ở DD validation | Chủ dự án / DD |

**Nợ prototype**: màn này chưa có mockup ở `09-layoutBase/`; ghi nhận để bổ sung vào
`06-plan/PROTOTYPE_DEBT.md` nếu chưa có mục riêng cho slug `interview_question_authoring` — tại thời điểm
viết BD này chưa xác minh được file đó đã liệt kê slug này hay chưa `[SoT: Suy luận — chưa đọc toàn bộ
06-plan/PROTOTYPE_DEBT.md để xác nhận, cần rà soát riêng]`.

## 8. Tham chiếu

- `01-rd/screens/shared/interview_question_authoring.md` — RD nguồn, toàn bộ mục 1-7.
- `01-rd/screens/shared/interview_question_management.md` — màn cha, dòng 110-112 (phạm vi quyền A2).
- `01-rd/screens/shared/problem_authoring.md` — tiền lệ cấu trúc màn soạn riêng (dẫn chiếu song song).
- `02-bd/architecture/interview-bank.md` — kiến trúc module, mục 2.1 (5 chủ đề), mục 4.2 (luồng Chế độ
  luyện), mục 5 (chống prompt injection hai lớp).
- `02-bd/database/interview-bank.md` — bảng `interview_questions`, `answer_rubrics`,
  `follow_up_questions` (mục 1.2, 1.3).
- `02-bd/security/interview-bank.md` — mục 2 (quyền sở hữu dữ liệu, phạm vi A2 toàn bộ ngân hàng).
- `01-rd/req/identity.md` — F1-10 tới F1-12, F1-14 (Function, Nhật ký hệ thống).
- `.claude/skills/bd-generation/SKILL.md` — khuôn Layer 2/3 cho `02-bd/screens/`.
- Quyết định: `DEC-2026-0825-shared-content-authoring-screens`, `DEC-2026-0830-interview-bank-crud`.
