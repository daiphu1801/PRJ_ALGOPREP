# BD — Màn `interview_question_management` (Quản lý ngân hàng câu hỏi phỏng vấn)

> Trục: **màn hình** (không phải Bounded Context), nhóm `shared` — dùng chung giữa A2 (giảng viên) và A3
> (quản trị viên), mount ở cả `/instructor/interview-questions` và `/admin/interview-questions`, cùng một
> view/BD/DD [SoT: 01-rd/screens/shared/interview_question_management.md:5-10; `DEC-2026-0825-shared-content-authoring-screens`].
> Slug khớp `01-rd/screens/shared/interview_question_management.md`. Bounded Context sở hữu toàn bộ dữ
> liệu và logic của màn này: `interview-bank` (F6) — một màn, một module.
>
> Đọc cùng: `02-bd/architecture/interview-bank.md` mục 2 (phạm vi F6-13), mục 2.2 (`question_set` đã bỏ),
> mục 3.2 (`ClassScopeQueryPort` — không dùng để lọc phạm vi sửa), mục 5 (hai lớp instruction chống
> injection cho Chế độ luyện — màn này soạn dữ liệu Lớp 2 nhưng không tự nó gọi AI);
> `02-bd/database/interview-bank.md` mục 1.1-1.3 (`question_topics`, `interview_questions`,
> `answer_rubrics`); `02-bd/security/interview-bank.md` mục 2 (A2/A3 sửa toàn bộ ngân hàng, không giới
> hạn theo `created_by`), mục 1 (`answer_rubrics` chỉ sửa qua đúng một đường CRUD của chính module này).
> Anti-drift: mọi endpoint chỉ nêu tên + BC sở hữu ở đây; request/response thuộc `03-dd/api/interview-bank.md`
> (chưa viết tại thời điểm BD này).

## 1. Bối cảnh dữ liệu màn hình dựa trên

Đã chốt ở BD module `interview-bank` (không lặp lại thiết kế, chỉ tham chiếu):

- **5 chủ đề cố định** (`question_topics`, seed): Lý thuyết CS, System design, Database, Ngôn ngữ, Hành vi
  — không có use case tạo/xoá chủ đề ở màn này, đổi danh mục là quyết định kiến trúc
  [SoT: 02-bd/database/interview-bank.md:8-21; `DEC-2026-0830-interview-bank-crud` mục 4].
- **`interview_questions`** mang bốn nhóm trường mà màn quản: chủ đề + cấp độ (F6-01), nội dung câu hỏi,
  "Đào sâu" (`follow_up_questions`, F6-13 — khác F6-04/05/06 và khác giai đoạn Phản biện F5-11 của
  `ai-review`), và không hiển thị `suggested_approach`/`sample_answer_framework`/`core_keywords` (F6-04
  tới F6-06 — dữ liệu của Chế độ học, thuộc màn `interview_question_authoring`, không phải màn này)
  [SoT: 02-bd/database/interview-bank.md:22-38].
- **`answer_rubrics`** — tiêu chí đánh giá có trọng số phần trăm, tổng phải bằng 100 (kiểm ở tầng ứng
  dụng), là rubric thứ ba của hệ thống, độc lập với `ai_review.rubric_configs`/`rubric_scores`
  [SoT: 02-bd/database/interview-bank.md:48-66; `07-review/rd_drift_repair_260909.md` mục 3.1].
- **Xoá mềm** (`status = ACTIVE|RETIRED`) — `RETIRED` ẩn khỏi mọi màn phía học viên, giữ nguyên
  `answer_rubrics` và không cascade xoá `user_answers` cũ đã dùng câu hỏi này
  [SoT: 02-bd/database/interview-bank.md:35, 64-66; `DEC-2026-0830-interview-bank-crud` mục Q7].
- **A2 và A3 cùng phạm vi dữ liệu — sửa được toàn bộ ngân hàng, không giới hạn theo `created_by`**
  (`created_by` chỉ phục vụ hiển thị/audit, không phải điều kiện lọc quyền)
  [SoT: 02-bd/security/interview-bank.md:34-43; 01-rd/screens/shared/interview_question_management.md:110-112].
- **Không có khái niệm `question_sets`/"bộ câu hỏi theo lớp"** — đã bỏ hẳn khỏi phạm vi, màn này không có
  bộ chọn lớp, không có hành động gán [SoT: 02-bd/architecture/interview-bank.md mục 2.2;
  01-rd/screens/shared/interview_question_management.md:97-103].
- **Mọi thao tác tạo/sửa/nhân bản/xoá ghi vào Nhật ký hệ thống** (F1-14), gác bởi Function
  `INTERVIEW_BANK_MANAGEMENT` (F1-12) [SoT: 01-rd/req/identity.md:60; 01-rd/screens/shared/interview_question_management.md:121-124].

## 2. Layout regions

Đối chiếu `09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html` (cấu trúc, không lấy màu/spacing — chưa có
design-system SoT chính thức cho Next.js; khu Giảng viên (`/instructor/...`) chưa có prototype riêng, dùng
lại đúng cấu trúc này [SoT: 01-rd/screens/shared/interview_question_management.md:12-14]):

| Vùng | Nội dung | Nguồn |
| :--- | :--- | :--- |
| Sidebar điều hướng (dùng chung khung Admin; khu Giảng viên tương đương [Đợi nextjs]) | Nhóm "Nội dung" → mục "Câu hỏi phỏng vấn" đang active, badge đếm tổng số câu | [SoT: 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:297, 303-306] |
| Thanh tiêu đề màn (sticky) | Tiêu đề "Ngân hàng câu hỏi phỏng vấn" + phụ đề "N câu · chủ đề, cấp độ, câu hỏi đào sâu và tiêu chí đánh giá", hai nút "Nhập CSV" và "Câu hỏi mới" | [SoT: 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:138-150] |
| Bốn thẻ chỉ số | Tổng câu hỏi (+N trong tháng); Có tiêu chí đầy đủ (N/tổng, %, "N câu thiếu rubric"); Điểm trung bình (thang 5, 30 ngày); Chưa dùng lần nào | [SoT: 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:152-163, 428-433] |
| Thanh lọc | Ô tìm theo nội dung/thẻ, dải tab chủ đề (5 giá trị + "Tất cả"), dải tab cấp độ (Dễ/Trung bình/Khó + "Tất cả"), bộ đếm kết quả "N / tổng câu" | [SoT: 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:165-181, 474-478] |
| Lưới thẻ câu hỏi | Sáu thẻ mỗi trang, mỗi thẻ: mã câu hỏi + nhãn cấp độ + chủ đề, nội dung câu hỏi, khối "Đào sâu", khối "Tiêu chí đánh giá" (thanh trọng số), chân thẻ (thống kê sử dụng + Sửa/Nhân bản/Xoá) | [SoT: 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:183-233, 383-410, 450] |
| Phân trang | Nút Trước/Sau + số trang, nhãn "Trang X/Y · hiển thị N câu" | [SoT: 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:235-244, 479] |
| Hộp thoại xác nhận xoá (`confirmOpen`) | Tiêu đề "Xoá câu hỏi?", trích mã + 60 ký tự đầu câu hỏi, ghi chú giữ lịch sử phiên cũ và không hoàn tác, nút Huỷ/Xoá câu hỏi | [SoT: 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:259-271] |
| Footer trang | Phiên bản hệ thống, trạng thái dịch vụ | [SoT: 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:246-254] |

**Không có bộ chọn lớp, khái niệm nhóm câu hỏi, hay hành động gán** ở bất kỳ vùng nào — khớp
`01-rd/screens/shared/interview_question_management.md:97-103`.

## 3. Component inventory

| Component | Hành vi |
| :--- | :--- |
| `HeaderStatLine` | Tiêu đề + phụ đề động "N câu · ..." lấy từ tổng số `interview_questions` đang `ACTIVE` [SoT: 09-layoutBase/...:140-141] |
| `ImportCsvButton` | Nút "Nhập CSV" — **ngoài phạm vi bản đầu, không cấp mã**, hiển thị nhưng vô hiệu hoá hoặc dẫn tới thông báo "sắp ra mắt"; hành vi cụ thể để DD chốt [SoT: 01-rd/screens/shared/interview_question_management.md:65-67, mục 5 Q2] |
| `NewQuestionButton` | Nút "Câu hỏi mới" — điều hướng sang màn `interview_question_authoring` (không mở modal), xem mục 6 Navigation |
| `StatCard × 4` | Bốn thẻ dẫn xuất trình bày (không cần API riêng, tính từ cùng payload danh sách/tổng hợp): Tổng câu hỏi, Có tiêu chí đầy đủ, Điểm trung bình, Chưa dùng lần nào [SoT: 09-layoutBase/...:152-163, 428-433] |
| `SearchInput` | Tìm theo nội dung câu hỏi hoặc thẻ (mã câu hỏi) — debounce, reset về trang 1 khi đổi từ khoá [SoT: 09-layoutBase/...:166-169, 442-448] |
| `TopicTabGroup` | 6 tab (Tất cả + 5 chủ đề cố định), chọn 1 tại một thời điểm, reset trang 1 khi đổi [SoT: 09-layoutBase/...:171-174, 474] |
| `LevelTabGroup` | 4 tab (Tất cả/Dễ/Trung bình/Khó), chọn 1 tại một thời điểm, reset trang 1 khi đổi [SoT: 09-layoutBase/...:176-179, 475] |
| `ResultCountLabel` | "N / tổng câu" theo kết quả đã lọc [SoT: 09-layoutBase/...:180, 478] |
| `QuestionCard` | Một thẻ câu hỏi — hiển thị mã, nhãn cấp độ (màu theo Dễ/Trung bình/Khó), chủ đề, nội dung, khối Đào sâu, khối Tiêu chí đánh giá, chân thẻ [SoT: 09-layoutBase/...:185-231] |
| `FollowUpList` | Danh sách câu hỏi đào sâu của thẻ (0..n dòng, dữ liệu `follow_up_questions`) [SoT: 09-layoutBase/...:194-204] |
| `RubricWeightBar` | Mỗi tiêu chí một thanh trọng số phần trăm; **câu hỏi thiếu rubric hiển thị khối này rỗng** (không ẩn khối, chỉ không có dòng nào) — khớp F6-13 "câu hỏi thiếu tiêu chí vẫn hiện ở Chế độ học" áp dụng tương tự cho hiển thị quản trị [SoT: 09-layoutBase/...:206-221; 01-rd/screens/shared/interview_question_management.md:113-116] |
| `UsageStatLabel` | "Dùng N lần · điểm TB X/5" ở chân thẻ [SoT: 09-layoutBase/...:224] |
| `EditButton` | "Sửa" — điều hướng sang `interview_question_authoring` ở chế độ sửa, mang theo mã câu hỏi | [SoT: 09-layoutBase/...:226] |
| `DuplicateButton` | "Nhân bản" — tạo bản sao câu hỏi (nội dung + đào sâu + rubric), điều hướng sang `interview_question_authoring` ở chế độ tạo mới với dữ liệu đã điền sẵn; hành vi chính xác (tạo ngay ở backend rồi mở form sửa, hay chỉ điền sẵn form tạo mới chưa lưu) để DD chốt [SoT: 09-layoutBase/...:227] |
| `DeleteQuestionButton` + `DeleteConfirmDialog` | Nút xoá mở hộp thoại xác nhận, xác nhận gọi API xoá mềm | [SoT: 09-layoutBase/...:228, 259-271] |
| `PaginationBar` | Trước/Sau + số trang, 6 thẻ/trang | [SoT: 09-layoutBase/...:235-244, 450] |

**Không đưa vào BD (thuộc phạm vi khác):** màu sắc/token thiết kế cụ thể — chưa có design-system SoT
chính thức cho Next.js, chỉ mô tả cấu trúc theo layoutBase; request/response của các API — DD; form
tạo/sửa câu hỏi (4 nhóm trường lồng nhau) — thuộc RD/BD/DD riêng của `interview_question_authoring`, chỉ
liên kết ở đây, không lặp lại.

## 4. Screen states

| State | Điều kiện | Hiển thị |
| :--- | :--- | :--- |
| `S1 — Danh sách mặc định` | Mở màn lần đầu, chưa lọc | Toàn bộ câu hỏi `ACTIVE`, tab "Tất cả"/"Tất cả", trang 1, 4 thẻ chỉ số theo dữ liệu hệ thống |
| `S2 — Đang lọc/tìm` | Có từ khoá tìm hoặc chọn chủ đề/cấp độ khác "Tất cả" | Lưới thẻ và bộ đếm "N / tổng câu" cập nhật theo điều kiện lọc, trang reset về 1 |
| `S3 — Không có kết quả` | Bộ lọc/tìm không khớp câu hỏi nào | Lưới thẻ rỗng kèm thông báo trống — nội dung thông báo cụ thể để DD |
| `S4 — Câu hỏi thiếu rubric` | `answer_rubrics` rỗng cho một câu hỏi | Khối "Tiêu chí đánh giá" của thẻ đó hiển thị rỗng/thiếu, câu hỏi vẫn tính vào chỉ số "N câu thiếu rubric" của `StatCard` thứ hai [SoT: 09-layoutBase/...:430; 01-rd/screens/shared/interview_question_management.md:113-116] |
| `S5 — Xác nhận xoá` | Bấm nút xoá trên một thẻ | Hộp thoại `DeleteConfirmDialog` mở, nêu mã + trích 60 ký tự đầu câu hỏi, ghi chú giữ lịch sử phiên cũ và không hoàn tác [SoT: 09-layoutBase/...:262-267] |
| `S6 — Vừa xoá thành công` | Sau khi xác nhận xoá, API trả thành công | Thẻ biến mất khỏi lưới ngay (client-side), 4 thẻ chỉ số và bộ đếm kết quả cập nhật lại |
| `S7 — Lỗi thao tác` | API trả lỗi (xoá thất bại, tải danh sách thất bại) | Thông báo lỗi tại chỗ, không đổi trạng thái hiển thị hiện có — nội dung thông báo cụ thể để DD |
| `S8 — Quyền không đủ` | Người dùng không có `INTERVIEW_BANK_MANAGEMENT` cố mở route | Chặn ở route-level (không render màn) — xem mục 7 |

**Không thiết kế trạng thái nào cho "Nhập CSV"** — ngoài phạm vi bản đầu (mục 3, xem Q2 ở RD).

## 5. APIs consumed (chỉ tên endpoint + BC sở hữu — chi tiết ở `03-dd/api/interview-bank.md`)

| Hành động màn hình | Endpoint (tên nghiệp vụ, chưa chốt route) | BC sở hữu |
| :--- | :--- | :--- |
| Tải danh sách câu hỏi có lọc/tìm/phân trang | Lấy danh sách câu hỏi phỏng vấn (quản trị) | `interview-bank` |
| Tải 4 thẻ chỉ số tổng hợp | Lấy chỉ số ngân hàng câu hỏi | `interview-bank` |
| Tải danh mục 5 chủ đề (seed) | Lấy danh mục chủ đề | `interview-bank` |
| Nhân bản một câu hỏi | Nhân bản câu hỏi | `interview-bank` |
| Xoá mềm một câu hỏi | Xoá câu hỏi (đánh dấu `RETIRED`) | `interview-bank` |

Tạo/sửa nội dung câu hỏi (4 nhóm trường + rubric) thuộc endpoint của `interview_question_authoring`,
không liệt kê lại ở đây — xem `01-rd/screens/shared/interview_question_authoring.md` khi RD/BD/DD màn đó
hoàn tất. Không có endpoint nào ngoài `interview-bank` — màn này không đọc dữ liệu module khác.

## 6. Navigation

- Vào từ sidebar: nhóm "Nội dung" → mục "Câu hỏi phỏng vấn", mount ở cả `/instructor/interview-questions`
  (A2) và `/admin/interview-questions` (A3) — cùng view [SoT: 09-layoutBase/Admin - Câu hỏi phỏng
  vấn.dc.html:297, 303-306; `DEC-2026-0825-shared-content-authoring-screens`].
- "Câu hỏi mới" và "Sửa" điều hướng sang màn `interview_question_authoring` — **chưa có prototype**
  (`[Đợi nextjs]`), RD tại `01-rd/screens/shared/interview_question_authoring.md`
  [SoT: 01-rd/screens/shared/interview_question_management.md:66-67, 133, 153-154].
- "Nhân bản" điều hướng sang `interview_question_authoring` với dữ liệu điền sẵn (xem `DuplicateButton`
  mục 3) — không có màn trung gian nào khác.
- Không điều hướng sang màn phía người học (`interview_bank_list`, `interview_question_detail`) từ màn
  này — hai màn đó chỉ đọc dữ liệu do màn này tạo, không có liên kết ngược
  [SoT: 01-rd/screens/shared/interview_question_management.md:29-35].

## 7. Access rights

- Chỉ actor có Function `INTERVIEW_BANK_MANAGEMENT` (A2 hoặc A3) truy cập được toàn bộ màn — kiểm ở
  route-level trước khi render [SoT: 01-rd/req/identity.md — F1-10 tới F1-12;
  01-rd/screens/shared/interview_question_management.md:110-112].
- **A2 và A3 cùng phạm vi dữ liệu** — không chia theo lớp phụ trách, không lọc theo `created_by`; A2 sửa
  được toàn bộ ngân hàng câu hỏi hệ thống, không chỉ câu hỏi tự tạo
  [SoT: 02-bd/security/interview-bank.md:34-43]. `ClassScopeQueryPort` (nếu dùng ở tầng backend) chỉ xác
  định A2 có phụ trách ít nhất một lớp (điều kiện được cấp Function nói chung), **không** dùng để lọc câu
  hỏi hiển thị trên màn này [SoT: 02-bd/architecture/interview-bank.md mục 3.2].
- Thao tác ghi (nhân bản, xoá) qua kiểm `INTERVIEW_BANK_MANAGEMENT:UPDATE`/`:DELETE` ở tầng ứng dụng theo
  Lớp 2 RBAC, không giả định "vào được màn nghĩa là toàn quyền" — cùng nguyên tắc đã áp dụng ở
  `admin_permission_matrix` [SoT: 02-bd/screens/admin/admin_permission_matrix.md:100-105].
- Mọi thao tác tạo/sửa/nhân bản/xoá ghi vào `system_audit_logs` không ngoại lệ, theo F1-14
  [SoT: 01-rd/req/identity.md — F1-14; 01-rd/screens/shared/interview_question_management.md:121-124].
- Không hiển thị dữ liệu cá nhân của học viên (`user_answers`, `recall_ratings`) trên màn này — màn chỉ
  quản trị nội dung câu hỏi, không phải dữ liệu luyện tập cá nhân
  [SoT: 02-bd/security/interview-bank.md:44-47].

## 8. Câu hỏi mở

1. **Hành vi cụ thể của "Nhập CSV"** — RD đã chốt ngoài phạm vi bản đầu, nhưng chưa nói nút hiển thị dạng
   vô hiệu hoá (disabled + tooltip) hay ẩn hẳn khỏi màn cho tới khi triển khai
   [SoT: 01-rd/screens/shared/interview_question_management.md:65-67, mục 5 Q2]. Đề xuất: giữ hiển thị,
   `disabled`, tooltip "Sắp ra mắt" — nhất quán với việc prototype đã dựng nút này; chốt ở DD.
2. **"Nhân bản" tạo bản ghi ngay hay chỉ điền sẵn form** — prototype chỉ có nút, không có luồng chi tiết
   [SoT: 09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html:227]. `[SoT: Suy luận]` — đề xuất tạo bản ghi mới
   ngay ở backend (mã `IQ-xxx` mới, `status = ACTIVE`, sao chép toàn bộ đào sâu + rubric) rồi mở
   `interview_question_authoring` ở chế độ sửa bản ghi vừa tạo — tránh trạng thái "nháp chưa lưu" phức
   tạp thêm cho form vốn đã nặng (4 nhóm trường); chốt ở DD trước khi viết `03-dd/api/interview-bank.md`.
3. **Nội dung thông báo lỗi/trống cụ thể** (`S3`, `S7`) — RD/prototype không đặc tả câu chữ, để DD chốt
   theo chuẩn thông báo chung của hệ thống.

## 9. Tham chiếu

- `01-rd/screens/shared/interview_question_management.md` — RD màn hình.
- `09-layoutBase/Admin - Câu hỏi phỏng vấn.dc.html` — bằng chứng layout (504 dòng).
- `02-bd/architecture/interview-bank.md`, `02-bd/database/interview-bank.md`,
  `02-bd/security/interview-bank.md` — BD module.
- `02-bd/screens/admin/admin_permission_matrix.md` — mẫu cấu trúc BD trục màn hình đã dùng trước đó.
- `.nexa/control/decision-registry.md` — `DEC-2026-0825-shared-content-authoring-screens`,
  `DEC-2026-0830-interview-bank-crud`, `DEC-2026-0828-remove-per-class-interview-set`.
- `01-rd/screens/shared/interview_question_authoring.md` — màn soạn/sửa câu hỏi, liên kết từ đây, không
  lặp lại đặc tả.
