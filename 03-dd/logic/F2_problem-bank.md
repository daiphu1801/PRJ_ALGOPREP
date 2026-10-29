# DD — Logic module `problem-bank` (F2)

> Nguồn: `01-rd/req/F2_problem-bank.md` (F2-01 tới F2-18) · `02-bd/architecture/F2_problem-bank.md` · `02-bd/database/F2_problem-bank.md` · `02-bd/security/F2_problem-bank.md` · `02-bd/storage/F2_problem-bank.md`.
> Bounded Context: F2 `problem-bank` · Cập nhật: 2026-10-23.
>
> Hợp đồng endpoint, cổng và sự kiện: `03-dd/api/F2_problem-bank.md` (`§n` là số mục ở file đó). Quy tắc kiểm trường: `03-dd/validation/F2_problem-bank.md`. Tiêu chí nghiệm thu: `04-tdd/modules/F2_problem-bank.md`.
> File này ghi các **khối quy tắc nghiệp vụ** `BR-nn` trải dài nhiều endpoint: kích hoạt, tiền điều kiện, các bước, hậu điều kiện, sự kiện phát ra. Không lặp lại điều BD đã chốt, chỉ đặc tả thêm thuật toán và thứ tự.
> `[SoT: Suy luận]` là quyết định của DD, đã gom ở `03-dd/api/F2_problem-bank.md` mục 61.
>
> **Không có job nền** cho module này: BD không định nghĩa job nào (dọn object MinIO vật lý "ngoài phạm vi", `02-bd/storage/F2_problem-bank.md:58-60`), nên không có `03-dd/jobs/F2_problem-bank.md`.

Bảng 0.1: Danh mục khối quy tắc.

| Mã | Chủ đề | Endpoint (`§`) |
| :--- | :--- | :--- |
| BR-01 | Phạm vi hiển thị bài với học viên | 2-8, 13, 14 |
| BR-02 | Phân quyền ba lớp và phạm vi tác giả | mọi endpoint ghi, 34, 35 |
| BR-03 | Sinh slug `code` | 16, 21, 36 |
| BR-04 | Lưu nội dung bài (tạo, cập nhật, khoá lạc quan) | 36, 37 |
| BR-05 | Lưu đặc tả và cờ `function_wrapper_supported` | 38 |
| BR-06 | Ngữ cảnh AI là dữ liệu | 39, `ProblemContextPort` |
| BR-07 | Checklist xuất bản | 34, 40, 29 |
| BR-08 | Chuyển trạng thái xuất bản và rút xuống | 29, 30, 40 |
| BR-09 | Xoá mềm bài toán | 33 |
| BR-10 | Nhân bản bài toán | 35, 36 |
| BR-11 | Phiên bản bộ testcase | 42, 43, 44, 47 |
| BR-12 | Lưu trữ testcase `INLINE`/`MINIO` | 42, 43, 47 |
| BR-13 | Chống rò rỉ testcase ẩn | mọi endpoint học viên, cổng |
| BR-14 | Tải lên theo lô qua presigned URL | 46, 47 |
| BR-15 | Chạy đáp án mẫu | 48, 40, 49 |
| BR-16 | Sinh testcase bằng AI (F2-14) | 49 |
| BR-17 | Duyệt testcase và hiển thị | 43 |
| BR-18 | Giao bài và gỡ bài theo lớp | 50-55 |
| BR-19 | Xử lý `ClassDeleted` | sự kiện nhận |
| BR-20 | Cập nhật `problem_stats` từ `SubmissionGraded` | sự kiện nhận |
| BR-21 | Bookmark riêng tư | 9-12 |
| BR-22 | Quản lý danh mục chủ đề và độ khó | 15-24 |
| BR-23 | Lọc theo trạng thái giải, bốc ngẫu nhiên, gợi ý, tiến độ chủ đề | 2-4, 9, 13 |
| BR-24 | Số liệu quản lý (phân bố, cần chú ý, xuất CSV) | 25-28 |
| BR-25 | Cache Redis | 2, 14, 38 |
| BR-26 | Phát sự kiện và cổng đồng bộ | 59, 58 |
| BR-27 | Nhật ký hệ thống qua `AuditLogPort` | 16-24, 29-33, 36 |

## BR-01 — Phạm vi hiển thị bài với học viên (F2-15, G4)

- **Kích hoạt**: mọi endpoint phía học viên và mọi cổng đọc cho học viên.
- **Tiền điều kiện**: người gọi đã xác thực.
- **Các bước**:
  1. Điều kiện `status = 'PUBLISHED' AND deleted = false` nằm **trong truy vấn** (không lọc sau). Bài ngoài phạm vi coi như không tồn tại: trả `404 PRB-010`, thân phản hồi giống hệt id không tồn tại, **không** báo "không có quyền" (`USR0101_problem_list.md:624`).
  2. Mẫu số mọi tỉ lệ tiến độ chỉ đếm bài ở phạm vi này; điểm bài đã rút vẫn lưu ở `identity` nhưng không cộng vào "đã giải" (`02-bd/database/F2_problem-bank.md:57-59`).
  3. Ngoại lệ có chủ đích: bookmark (BR-21), danh sách giao bài của giảng viên (BR-18) và `ProblemSummaryReadPort` vẫn trả bài đã rút (có cờ `withdrawn`) vì dữ liệu lịch sử phải đọc được (`02-bd/database/F2_problem-bank.md:54-56`).
- **Hậu điều kiện**: không có đường nào lộ sự tồn tại của bài nháp hay bài đã xoá cho học viên.
- **Sự kiện**: không.

## BR-02 — Phân quyền ba lớp và phạm vi tác giả

- **Kích hoạt**: mọi endpoint có quyền.
- **Các bước** (theo thứ tự, dừng ở lỗi đầu tiên):
  1. JWT hợp lệ (`401`).
  2. `FUNCTION:ACTION` (`403 PRB-002`): vai trò lấy từ JWT, quyền tra qua cổng `identity` có đọc `permission_version` từ PostgreSQL primary trước khi tin cache. Bảng ánh xạ endpoint sang quyền ở `03-dd/api/F2_problem-bank.md` mục 0.
  3. Vai trò `ADMIN` cho ghi danh mục (`403 PRB-004`); lớp phụ trách (`403 PRB-005`, hỏi `InstructorClassPort`).
  4. Phạm vi tác giả cho A2 trên endpoint theo bài (hàm `resolveAccess(problem, actor, mode)`):
     | Người gọi | `mode = READ` | `mode = WRITE` |
     | :--- | :--- | :--- |
     | A3 | mọi bài chưa `deleted` | như vậy |
     | A2 là tác giả | bài của mình, mọi trạng thái | như vậy |
     | A2 không phải tác giả, bài `PUBLISHED` | chỉ đọc (`readOnly = true`, không nội dung testcase) | `403 PRB-003` |
     | A2 không phải tác giả, bài `UNPUBLISHED` | `404 PRB-010` | `404 PRB-010` |
     | bài `deleted` hoặc không tồn tại | `404 PRB-010` | `404 PRB-010` |
- **Hậu điều kiện**: quyền được kiểm ở tầng application của `problem-bank`, kể cả khi giao diện đã ẩn nút (`02-bd/security/F2_problem-bank.md:45-47`).
- **Căn cứ**: `DEC-2026-1011-a2-read-published-problems-of-others`; mở rộng 404 sang ghi là `[SoT: Suy luận]`.

## BR-03 — Sinh slug `code`

- **Kích hoạt**: tạo bài (`problems.code`), tạo chủ đề (`topics.code`), tạo độ khó (`problem_levels.code`); nhân bản bài (bản sao sinh mã mới).
- **Thuật toán** (`slugify`, một quy tắc cho cả ba, `[SoT: Suy luận]`, đóng "cách sinh slug chốt ở DD", `02-bd/database/F2_problem-bank.md:84,108`):
  1. Chuẩn hoá Unicode NFD, bỏ dấu kết hợp, đổi `đ`/`Đ` thành `d`/`D`.
  2. Chữ HOA; mọi chuỗi ký tự không thuộc `[A-Z0-9]` thành một dấu `-`; bỏ `-` ở hai đầu; cắt còn tối đa 64 ký tự (cắt rồi bỏ `-` cuối).
  3. Slug rỗng (tên toàn ký hiệu) dùng mã dự phòng `PROBLEM`, `TOPIC`, `LEVEL`.
  4. Trùng: thử `slug`, `slug-2`, `slug-3`...; unique tính **cả bài `deleted`** (`02-bd/database/F2_problem-bank.md:18`). Cách làm an toàn đồng thời: `INSERT` với `ON CONFLICT DO NOTHING`, không chèn được thì thử hậu tố kế (tối đa 20 lần, quá thì `500`).
  5. Hậu tố tính vào giới hạn 64 ký tự (cắt phần gốc trước khi thêm hậu tố).
- **Hậu điều kiện**: `code` bất biến, không đổi khi đổi tên/tiêu đề; ba mức seed giữ `EASY`, `MEDIUM`, `HARD` (`02-bd/database/F2_problem-bank.md:96`).

## BR-04 — Lưu nội dung bài (F2-01, F2-02, F2-10)

- **Kích hoạt**: `POST /authoring/problems` (§36), `PUT .../content` (§37).
- **Tiền điều kiện**: BR-02 `mode = WRITE`; kiểm đầu vào theo `validation` mục 2.
- **Các bước (tạo)**:
  1. Điền giá trị thiếu: `levelId` -> mức đầu theo `sort_order`; ba giới hạn -> `JudgeConfigReadPort.getGradingDefaults()` (lỗi cổng thì dùng hằng dự phòng ở `application.yml`).
  2. Kiểm `levelId`, `topicIds` còn tồn tại (`422 PRB-039`).
  3. Sinh `code` (BR-03); `INSERT problems` với `status = UNPUBLISHED`, `author_id` = người gọi (**bất biến**), `current_testcase_set_version = 1`, `function_wrapper_supported = true`, `ai_testcase_generations_used = 0`, `no_full_code = true`, `socratic_only = true`.
  4. Liên kết chủ đề; tìm-hoặc-tạo thẻ theo tên (so không phân biệt hoa thường); `INSERT worked_examples` (`position` 1..n); `INSERT problem_reference_solutions` nếu có mã.
  5. Có `duplicatedFromProblemId` thì thực hiện BR-10.
- **Các bước (cập nhật)**:
  1. `UPDATE problems SET ... , updated_by, updated_at = now() WHERE id = ? AND updated_at = :expectedUpdatedAt`. 0 dòng và bài còn tồn tại -> `409 PRB-020` (kèm `details.currentUpdatedAt`).
  2. Thay thế theo trạng thái cuối: `problem_topics` (diff thêm/xoá), `problem_tags`, `worked_examples` (id có sẵn -> cập nhật; `id = null` -> thêm; vắng mặt -> xoá; đánh số lại `position` trong cùng giao dịch), `problem_reference_solutions` (có mã -> upsert; rỗng -> xoá dòng).
  3. Bài đang `PUBLISHED` và số ví dụ sau lưu `< 2` -> `422 PRB-053`, rollback.
  4. Đổi dòng đáp án mẫu **không** tăng `current_testcase_set_version` hay `spec_version`; đổi thuộc tính AI cũng không (`02-bd/database/F2_problem-bank.md:40,243`).
- **Quy ước `updated_at`**: mọi ghi trong phạm vi một bài (nội dung, đặc tả, ngữ cảnh AI, trạng thái, testcase, thao tác lô) đặt `updated_by`, `updated_at`; mọi phản hồi ghi trả `problemUpdatedAt` để giao diện giữ token mới.
- **Hậu điều kiện**: bài đang `PUBLISHED` thì tăng phiên bản cache danh sách (BR-25).
- **Sự kiện**: không (nội dung đề không có sự kiện riêng).

## BR-05 — Lưu đặc tả và cờ `function_wrapper_supported` (F2-03, F2-04, F3-13)

- **Kích hoạt**: `PUT .../spec` (§38).
- **Các bước**:
  1. Dựng `FunctionSpecDraft` (`functionName`, `returnType`, `parameters`, `nameOverrides`, `matchingStrategy`, `epsilonValue`) và gọi `FunctionSpecEvaluator.evaluate` (`algoprep-common`, hàm thuần, cổng P5 `03-dd/api/F3_harness.md` mục 6.3).
  2. `violations` khác rỗng (cứng, `HRN-V10` tới `HRN-V22`) -> `422 PRB-036`, **không ghi gì**.
  3. Lấy `wrapperSupported` và `unsupportedReasons` (mềm, `HRN-V01` tới `HRN-V07`); đặt `problems.function_wrapper_supported = wrapperSupported`; trả `warnings[]` dạng `PRB-W01`.
  4. Upsert dòng `problem_specs` (unique `problem_id`; lần đầu `matching_strategy = EXACT` nếu thiếu). So sánh **tám cột** với dòng cũ (`matching_strategy`, `epsilon_value`, `stdin_format_md`, `stdout_format_md`, `function_name`, `return_type`, `parameters`, `name_overrides`): có bất kỳ cột nào đổi thì `spec_version = spec_version + 1` (`02-bd/database/F2_problem-bank.md:136`); không đổi thì giữ.
     `name_overrides` lưu khoá HOA (`JAVA`/`CPP`/`PYTHON`).
  5. Khoá lạc quan như BR-04; cập nhật `updated_*`.
  6. Xoá key Redis `problem:spec:<problemId>`.
- **Hậu điều kiện**: bài có kiểu ngoài lược đồ hiện ở người học chỉ còn mô hình Standard I/O (việc ẩn nút là của tầng trình bày, `02-bd/architecture/F2_problem-bank.md:133-134`).
- **Sự kiện**: nếu `spec_version` tăng, sau commit phát `ProblemSpecChanged { problemId, specVersion, changedColumns, signatureChanged, signatureBefore?, signatureAfter? }`. `harness` chỉ cần hai trường đầu (`03-dd/api/F3_harness.md` mục 5).
- **Ghi chú**: bài đã `PUBLISHED` vẫn lưu được; xuất bản chỉ kiểm `SPEC_DECLARED` lúc chuyển trạng thái (`Q-19`).

## BR-06 — Ngữ cảnh AI của bài là dữ liệu

- **Kích hoạt**: `PUT .../ai-context` (§39); mọi lần `ai-review` gọi `ProblemContextPort.getContext`.
- **Các bước**:
  1. Lưu `ai_brief`, `no_full_code`, `socratic_only` **nguyên văn**; không thông dịch, không lọc từ khoá, không ghép chuỗi ở phía client.
  2. `getContext` trả các giá trị đó như **trường dữ liệu có tên** trong `ProblemContext`; adapter của `ai-review` đặt chúng vào khối có nhãn riêng, sau chỉ thị hệ thống cố định kèm câu nói rõ khối đó không phải chỉ thị (`02-bd/architecture/F5_ai-review.md:218`).
  3. `ProblemContext` **không bao giờ** chứa mã đáp án mẫu, nội dung testcase hay `object_key`.
  4. Cùng nguyên tắc cho F2-14: kiểu `GenerationInput` không có trường cho đáp án mẫu hay `ai_brief` (BR-16).
- **Hậu điều kiện**: không thay đổi `spec_version`, `current_testcase_set_version`.

## BR-07 — Checklist xuất bản (F2-15, F2-18)

- **Kích hoạt**: tải `GetProblemForAuthoring` (chỉ đếm), `PUT .../status -> PUBLISHED`, `bulk/publish`.
- **Các bước tính**:
  1. `MIN_APPROVED_TESTCASES`: `COUNT(*)` testcase `approved = true` `>= 8`.
  2. `MIN_APPROVED_SAMPLES`: `visibility = SAMPLE AND approved = true` `>= 2`.
  3. `MIN_WORKED_EXAMPLES`: `COUNT(worked_examples) >= 2`.
  4. `SPEC_DECLARED`: theo `validation` bảng 11.1 (chữ ký đã khai trừ kiểu ngoài lược đồ; định dạng stdin/stdout không rỗng; chiến lược hợp lệ).
  5. `REFERENCE_SOLUTION_PASSES_ALL`: **chỉ tính khi chuyển trạng thái** (hoặc khi giao diện gọi §48): chạy đáp án mẫu trên mọi testcase đã duyệt (BR-15) và đòi `allPass = true`. Không có đáp án mẫu hoặc không có testcase đã duyệt -> không đạt.
  6. Khi tải trang chỉ trả bốn điều kiện đếm được; điều kiện thứ năm `met = null`.
- **Quyết định**: đạt đủ năm thì cho xuất bản; thiếu thì `422 PRB-030` với `details.items` liệt kê **mọi** điều kiện thiếu. Testcase nháp AI chưa duyệt **không** được đếm (`01-rd/req/F2_problem-bank.md:113-116`).
- **Hậu điều kiện**: ngưỡng đọc từ `application.yml` (`publish.min-*`), trả kèm trong `PublishChecklistDto` để giao diện không chép số.
- **Bất biến sau xuất bản (owner 2026-10-25)**: §43 đổi Sample đã duyệt sang Hidden và §44 xoá testcase phải khoá dòng bài, kiểm số ca sau thao tác trong cùng giao dịch; bài `PUBLISHED` luôn giữ ít nhất 8 testcase đã duyệt và 2 Sample đã duyệt, vi phạm trả `422 PRB-054` không đổi dữ liệu/phiên bản. Rút bài xuống trước nếu cần tái cấu trúc bộ testcase dưới ngưỡng. Ví dụ mẫu vẫn dùng `PRB-053`.

## BR-08 — Chuyển trạng thái xuất bản và rút xuống (F2-15)

- **Kích hoạt**: `PUT .../status`, `bulk/publish`, `bulk/unpublish`.
- **Tiền điều kiện**: BR-02 `WRITE`; bài chưa `deleted`.
- **Chuẩn bị snapshot khi xuất bản**: Tái xuất bản: nếu bộ chấm hiện hành khác snapshot đã công bố của current_testcase_set_version (so id, visibility, position, byte input/output của ca đã duyệt theo thứ tự chuẩn hóa), tăng đúng một phiên bản, tạo bundle bất biến mới và ghi Outbox TestcaseSetVersionBumped cùng giao dịch công bố. Nếu giống thì dùng lại snapshot và không tăng; lần xuất bản đầu tạo snapshot phiên bản hiện hành. Chỉ đổi nháp chưa duyệt không ảnh hưởng bộ chấm thì không tăng lúc tái xuất bản. Giữ nguyên snapshot lịch sử, lỗi MinIO/DB/Outbox rollback toàn bộ, trả 503 PRB-052 cho lỗi snapshot. Không so checksum của archive chứa metadata biến động; so nội dung logic chuẩn hóa, tải snapshot qua cổng nội bộ khi cần.
- **Các bước (`UNPUBLISHED -> PUBLISHED`)**: BR-07 đạt; `status = PUBLISHED`, `published_at = now()` (đặt lại mỗi lần xuất bản), `updated_*`; `AuditLogPort.record(..., "PROBLEM_PUBLISHED", ...)` cùng giao dịch; tăng phiên bản cache.
- **Các bước (`PUBLISHED -> UNPUBLISHED`)**: không điều kiện; `status = UNPUBLISHED`, **giữ** `published_at` (để quy tắc "Đang ẩn khỏi người học" nhận ra bài từng xuất bản, `SHR0201:528`); nhật ký `PROBLEM_UNPUBLISHED`; tăng phiên bản cache. **Không xoá** lượt nộp, bookmark, phiên phỏng vấn, dòng giao bài.
- **Lặp lại**: đã ở trạng thái đích thì thành công, không đổi, không nhật ký, không sự kiện.
- **Thao tác lô**: mỗi bài là một giao dịch riêng; một bài hỏng (checklist, quyền, nhật ký) không chặn bài khác; phản hồi `BulkProblemActionResultDto`.
- **Sự kiện** (sau commit, mỗi bài một sự kiện): `ProblemPublished` hoặc `ProblemUnpublished { problemId, actorUserId, occurredAt }`.
- **Hai trạng thái duy nhất**: không có `HIDDEN`, không có trạng thái thứ ba (`DEC-2026-0830-problem-lifecycle-two-states`).

## BR-09 — Xoá mềm bài toán (F2-15)

- **Kích hoạt**: `bulk/delete` (§33).
- **Các bước** (mỗi bài một giao dịch): BR-02 `WRITE`; `UPDATE problems SET status = 'UNPUBLISHED', deleted = true, updated_*`; nhật ký `PROBLEM_DELETED` (`before`: trạng thái cũ); bài đang `PUBLISHED` thì sau commit phát `ProblemUnpublished`; tăng phiên bản cache.
- **Không làm**: không `DELETE` dòng nào; không xoá testcase, chủ đề, giao bài, lượt nộp, bookmark; **không từ chối** bài đã có lượt nộp hoặc đang giao lớp (Phương án A, `DEC-2026-1008-problem-delete-and-duplicate`). Object MinIO giữ nguyên (`02-bd/storage/F2_problem-bank.md:58-60`).
- **Hậu điều kiện**: bài `deleted = true` biến khỏi `problem_management` và `problem_list`; tên và mã vẫn đọc được qua `ProblemSummaryReadPort` cho lịch sử. Không có đường khôi phục ở phía người học; `code` vẫn chiếm chỗ (unique tính cả bài đã xoá).

## BR-10 — Nhân bản bài toán (F2-16)

- **Kích hoạt**: `GET .../duplicate-source` (§35, chỉ đọc), rồi `POST /authoring/problems` với `duplicatedFromProblemId` (§36).
- **Các bước**:
  1. Đọc nguồn không ghi gì; giao diện thêm "(bản sao)" vào tiêu đề và giữ form ở trạng thái "có thay đổi chưa lưu"; **chưa có bản ghi** cho tới lần Lưu đầu (`DEC-2026-1008-problem-delete-and-duplicate` mục 3).
  2. Lần Lưu đầu: tạo bài mới như BR-04 với nội dung, ví dụ mẫu, đáp án mẫu lấy từ **thân yêu cầu**; rồi trong **cùng giao dịch** sao chép từ bài nguồn: dòng `problem_specs` (`spec_version` đặt 1), mọi dòng `testcases` (id mới; giữ `visibility`, `origin`, `approved`, `category`, `position`; vế `MINIO` thì `copy` object sang khoá `problems/<bài mới>/testcases/<testcase mới>/...` qua `TestcaseObjectStorePort`).
  3. Ghi `duplicated_from_problem_id`; `author_id` = người nhân bản; `status = UNPUBLISHED`; `current_testcase_set_version = 1`; `ai_testcase_generations_used = 0`.
  4. **Không** sao chép `ai_brief` và hai cờ AI (về mặc định) `[SoT: Suy luận, Q-20]`.
  5. Nhật ký `PROBLEM_DUPLICATED` cùng giao dịch.
- **Lỗi giữa chừng**: sao chép object thất bại -> rollback giao dịch và xoá các object đã sao chép (best-effort).
- **A2 chỉ nhân bản bài của mình**; bài nguồn đã `deleted` -> `404`.

## BR-11 — Phiên bản bộ testcase (F2-09)

- **Kích hoạt**: mọi thao tác làm đổi bộ testcase **đã duyệt** của bài `PUBLISHED`, gồm Sample và Hidden: thêm, duyệt, sửa input/output/visibility, xoá hoặc thêm lô qua §47; cùng với mọi lần đổi thứ tự `position` thực sự qua API §45 (thứ tự dùng chung cho hiển thị và chấm), kể cả khi kéo nháp chưa duyệt. Mỗi thao tác chỉ tăng một phiên bản; gửi lại cùng thứ tự không tăng. Lần xuất bản đầu tạo snapshot cho phiên bản hiện hành; tái xuất bản theo BR-08, bộ chấm khác snapshot thì tăng một phiên bản, giống thì dùng lại.
- **Các bước**: khoá dòng `problems` của bài; dựng bundle từ toàn bộ testcase đã duyệt sau thay đổi, gồm byte input/output thật, id, visibility, position; ghi object mới bất biến vào MinIO và lấy SHA-256; trong cùng giao dịch với thay đổi, tăng `current_testcase_set_version` đúng một lần và `INSERT testcase_set_snapshots` của số mới. Chỉ trả thành công sau commit; object mồ côi do rollback được dọn sau. Sau commit phát `TestcaseSetVersionBumped { problemId, testcaseSetVersion, bumpedAt }`.
- **Không tăng ngay khi sửa**: bài `UNPUBLISHED` (khi tái xuất bản phải so bộ chấm với snapshot theo BR-08); nháp chưa duyệt; đổi `category`; đổi đáp án mẫu hay ví dụ mẫu.
- **Điều kiện "đã có submission"** của `SHR0202` Sheet 6 **không áp** vì module không biết (`Q-04`): tăng thừa không nguy hại.
- **Truy vết và snapshot**: bảng `testcases` giữ trạng thái hiện hành; mỗi phiên bản đã công bố có bundle bất biến ở `testcase_set_snapshots` (BD database F2 mục 1.15). F4 ghi `currentTestcaseSetVersion` khi tạo lượt `PENDING`, rồi worker đọc đúng bundle bằng `loadTestcaseSnapshot`; lỗi checksum hoặc thiếu object kết thúc `SYSTEM_ERROR`, không chấm bằng bộ mới.
- **Lượt nộp đang dở**: đọc snapshot của phiên bản đã ghim lúc nộp, không đọc bộ hiện hành (`Q-25`).

## BR-12 — Lưu trữ testcase `INLINE`/`MINIO` (F2-07)

- **Quy tắc**: mỗi vế (input, expected output) quyết định **độc lập**: kích thước UTF-8 `<= 8.192` byte -> cột inline, ngược lại -> object `problems/<problemId>/testcases/<testcaseId>/input.txt` hoặc `expected_output.txt` trong bucket `algoprep-testcases` (`02-bd/storage/F2_problem-bank.md:14,27-32`).
- **Ghi**: object ghi **trước** khi commit DB; lỗi MinIO -> rollback (`503 PRB-052`) và xoá object đã ghi (best-effort). Đổi kiểu lưu khi sửa (vượt/hạ qua ngưỡng 8 KB): ghi mới, cập nhật hai cột, xoá object cũ **sau** commit.
- **Đọc**: `INLINE` đọc cột; `MINIO` đọc qua `TestcaseObjectStorePort` (nội bộ, không presigned) cho `judge`; presigned GET 5 phút chỉ cho tác giả/ADMIN (mọi testcase) hoặc cho học viên với testcase `SAMPLE`.
- **Xoá**: xoá object best-effort sau commit khi xoá testcase; bài `deleted` **không** xoá object.
- **Ràng buộc DB**: hai CHECK khớp `*_storage` với cột inline/key (`02-bd/database/F2_problem-bank.md` §1.6): đúng một nhánh mỗi vế, trường nhánh kia NULL; inline rỗng hợp lệ.

## BR-13 — Chống rò rỉ testcase ẩn (F2-08)

- **Danh sách trắng DTO**: mỗi DTO phía học viên khai tường minh từng trường; không tuần tự hoá entity. Kiểm tự động bằng test cấu trúc: không DTO học viên nào có trường tên `input`, `expectedOutput`, `objectKey`, `downloadUrl` cho testcase `HIDDEN`.
- **Truy vấn**: `ListSampleTestcases` và `loadSampleTestcases` lọc `visibility = SAMPLE AND approved = true` **trong SQL**.
- **Số đếm duy nhất** lộ ra: `hiddenTestcaseCount` ở `GetProblemOverview` (`USR0104:630`).
- **Cổng**: `loadTestcaseSnapshot(problemId, version)` chỉ trả bundle bất biến cho F4; `loadSampleTestcases(problemId)` lọc ca Sample đã duyệt cho Run Code. Cả hai là backend-to-backend, không có route công khai cho ca Hidden.
- **Presigned URL**: không bao giờ cấp cho A1 với testcase `HIDDEN`.
- **Log**: mức `INFO` trở lên chỉ ghi `testcase_id` và độ dài chuỗi; không ghi nội dung input/expected-output; không ghi mã đáp án mẫu hay `ai_brief` (`02-bd/security/F2_problem-bank.md:61-65`).
- **Lỗi và thông báo** của module không nhúng nội dung testcase (kể cả ở `details`).

## BR-14 — Tải lên theo lô qua presigned URL (F2-07)

- **Bước 1 (`bulk-upload-url`)**: kiểm giới hạn (`validation` bảng 5.2); sinh `testcaseId` cho từng cặp; ký presigned PUT 15 phút cho khoá do máy chủ dựng từ `testcaseId` (client không chọn khoá, chặn path traversal); lưu phiên Redis `problem:upload:<uploadId>` TTL 20 phút `{problemId, userId, items[{clientKey, testcaseId, sizes, visibility, category}], state: PENDING}`.
- **Bước 2 (`confirm`)**: đọc phiên (sai bài/người/hết hạn -> `422 PRB-044`); `HEAD` mọi object: thiếu hoặc kích thước khác khai -> `422 PRB-045`, **không thêm gì**; vế khai `<= 8 KB` thì `get` nội dung, kiểm UTF-8 (sai -> `PRB-045` `INVALID_TEXT`), ghi vào cột inline, xoá object sau commit.
- **Ghi**: một giao dịch `INSERT` mọi testcase (`origin = MANUAL`, `approved = true`, `position = max+1...`); BR-11 tăng phiên bản một lần nếu bài `PUBLISHED` và thêm ít nhất một ca đã duyệt, gồm SAMPLE hoặc HIDDEN; phiên chuyển `CONFIRMED` kèm id, giữ tới hết TTL để gọi lại trả cùng kết quả.
- **Dọn rác**: object của phiên hết hạn không được xác nhận để lại trong bucket (không có job dọn, `02-bd/storage/F2_problem-bank.md:58-60`); chấp nhận, ghi nhận ở `Q-08`.
- **Cơ chế xác nhận**: `HEAD` lúc gọi `confirm`, không dùng callback MinIO (đóng `02-bd/storage/F2_problem-bank.md:66`).

## BR-15 — Chạy đáp án mẫu (F2-18)

- **Kích hoạt**: §48 (giao diện), §40/§29 (xuất bản), §49 (điều kiện sinh).
- **Giới hạn ở lối vào §48**: Redis giới hạn 10 lượt/60 giây trượt theo người gọi, `429 PRB-055`; lỗi hạ tầng `PRB-050` hoàn mốc. Lời gọi BR-15 **nội bộ** từ xuất bản/sinh testcase không trừ hạn mức của nút chạy đáp án mẫu (đã có checklist và quota riêng), tránh chặn phát hành vì người soạn vừa chạy thử.
- **Các bước**:
  1. Chọn mã: mã kèm yêu cầu, hoặc dòng `problem_reference_solutions`. Không có -> `422 PRB-040`.
  2. Chọn tập testcase: một `testcaseId`, hoặc **mọi testcase `approved = true`** của bài.
  3. Giới hạn hiệu dụng: `timeLimitMs` và `memoryLimitMb` của bài nhân hệ số của ngôn ngữ (`JudgeConfigReadPort.getLanguageMultipliers`, `Q-32`); `maxOutputSizeKb` của bài.
  4. `SampleSolutionRunnerPort.runProgramBatch`: biên dịch một lần, chạy từng testcase với `stdin` là input (đọc qua `TestcaseObjectStorePort`); lỗi biên dịch -> mọi dòng `COMPILE_ERROR`, trả `compileError`.
  5. So khớp bằng P4 `MatchOutputUseCase.match` của F3 (`matchSpec` gồm `matchingStrategy`, `epsilonValue`, `returnType` từ `problem_specs`); không có dòng `problem_specs` thì dùng `EXACT`.
  6. Dựng kết quả `PASS/FAIL` kèm `failure`, `runtimeMs`, `memoryKb`; **không trả `actualOutput` hay diff**.
- **Hậu điều kiện**: kết quả tạm thời, không lưu (`02-bd/database/F2_problem-bank.md:243`); điều kiện xuất bản không đọc cờ cũ.
- **Hạ tầng lỗi**: go-judge không đáp ứng -> `503 PRB-050`, không đổi dữ liệu; thử lại một lần chỉ khi lỗi kết nối.
- **Quy ước đáp án mẫu**: chương trình `stdin`/`stdout` chạy với input chuẩn hoá (`Q-10`).

## BR-16 — Sinh testcase bằng AI (F2-14, F2-18)

- **Kích hoạt**: §49. **Nguyên tắc cứng**: AI chỉ viết **script sinh input**; output **luôn** lấy từ chạy đáp án mẫu thật; AI không bao giờ nhận đáp án mẫu; script là mã không tin cậy.
- **Tiền điều kiện**: BR-02 `WRITE` với `TESTCASE_MANAGEMENT:CREATE`; (a) `constraints_md` không rỗng; (b) có **ít nhất 2** testcase `SAMPLE`, `origin = MANUAL`, `approved = true`; (c) có đáp án mẫu và chạy `PASS` mọi testcase đã duyệt (BR-15); (d) `ai_testcase_generations_used < max_ai_generations_per_problem`.
  Thiếu (a)-(c) -> `422 PRB-041` với `details.reasons`; thiếu (d) -> `422 PRB-042`.
- **Các bước**:
  1. Khoá chống song song: `SET problem:gen:lock:<problemId> NX EX 600`; có rồi -> `409 PRB-023`. Luôn nhả khoá ở `finally`.
  2. Đọc giới hạn `JudgeConfigReadPort.getGeneratorSandboxLimits()` (`maxRuntimeSeconds`, `maxOutputMb`); lỗi cổng -> dùng hằng dự phòng 30 giây, 100 MB (mặc định BD, `02-bd/database/F4_judge-orchestration.md:157-165`).
  3. Dựng `GenerationInput = { problemId, statementMarkdown, constraintsMarkdown, samples (đúng 2 Sample tự viết đầu tiên theo position) }` và gọi `TestcaseScriptGenerationPort.generate` (**không** gửi đáp án mẫu, `ai_brief`, testcase ẩn). Lỗi AI -> `503 PRB-051`.
  4. Chạy `generatorScript` trong sandbox: `runGeneratorScript` (chặn mạng, giới hạn thời gian, giới hạn kích thước output, hạt giống cố định, ví dụ `seed = hash(problemId, used + 1)` `[SoT: Suy luận]`). Vượt giới hạn hoặc lỗi -> loại cả script, `422 PRB-043`, không tính lượt.
  5. Phân tích output của script thành danh sách ca `{category, input}`; sai định dạng -> `PRB-043 INVALID_FORMAT`.
  6. Loại input trùng với input đã có (`DUPLICATE_INPUT`).
  7. Với mỗi input còn lại, chạy `validatorScript` (nhận input qua `stdin`, thoát `0` nếu hợp lệ; `Q-15`); không hợp lệ -> loại (`INVALID_CONSTRAINT`).
  8. Chạy các input hợp lệ qua đáp án mẫu (BR-15, `runProgramBatch`): lỗi chạy hoặc quá giờ -> loại input (`REFERENCE_FAILED` / `TIMEOUT`). Riêng ca **lớn nhất** (theo kích thước input) bị loại do quá giờ hoặc lỗi -> thêm cảnh báo `LARGEST_CASE_TIMEOUT` (không lặng lẽ bỏ qua, `01-rd/req/F2_problem-bank.md:100-103`). Output vượt `max_output_size_kb` của bài cũng bị loại.
  9. Ghép cặp `(input, output thật)`; **một giao dịch**: `INSERT testcases` (`origin = AI`, `approved = false`, `visibility = HIDDEN`, `category` theo nhãn, BR-12 cho kiểu lưu), `INSERT testcase_generation_runs` (script, validator, hạt giống, mô hình, phiên bản prompt, số liệu; `Q-01`); tăng bộ đếm bằng một câu lệnh có điều kiện `UPDATE problems SET ai_testcase_generations_used = ai_testcase_generations_used + 1 WHERE id = ? AND ai_testcase_generations_used < (SELECT max_ai_generations_per_problem FROM problem_bank_settings WHERE id = 1)` **chỉ khi thêm được ít nhất 1 nháp**; 0 dòng được cập nhật -> rollback và `422 PRB-042`.
  10. Trả `added`, `testcases`, `dropped[]`, `warnings[]`, `aiGeneration`.
- **Hậu điều kiện**: dữ liệu đã sinh là **chuẩn** (bất biến), script chỉ là xuất xứ; không bao giờ chạy lại script để thay dữ liệu đang chấm (`01-rd/req/F2_problem-bank.md:104-106`). Nháp **không** tính vào checklist, không vào bộ chấm, không tăng `current_testcase_set_version`.
- **Không tính lượt**: lỗi script, lỗi AI, lỗi sandbox, lượt thêm 0 nháp; duyệt hay xoá nháp sau đó không hoàn lượt (`database/F2_problem-bank.md:37`).
- **Suy giảm êm**: đường AI chết chỉ làm §49 trả `503 PRB-051`; mọi chức năng F1-F4 khác chạy bình thường.
- **Ngoài phạm vi (khai rõ)**: checker cho bài nhiều đáp án đúng; đối chiếu bằng lời giải thứ hai (`01-rd/req/F2_problem-bank.md:120-125`).

## BR-17 — Duyệt testcase và hiển thị (F2-05, F2-06, F2-14)

- **Duyệt**: `PATCH ... {approved: true}` đặt `approved = true`, **giữ** `origin = AI`; không có chiều ngược (loại nháp là xoá, BR-12 dọn object). Nháp mặc định `HIDDEN`; người soạn đổi `visibility` khi duyệt.
- **Testcase tay** tạo với `approved = true` (suy luận khớp prototype, `02-bd/database/F2_problem-bank.md:161`).
- **Chỉ testcase `approved = true`** được: đếm vào checklist, vào bộ chấm (`HIDDEN`), vào tập Chạy thử (`SAMPLE`), vào `hiddenTestcaseCount`.
- **Ma trận độ phủ**: đếm theo `category` và `approved` (bảy giá trị); có thể trả sẵn từ `GetProblemForAuthoring` (`SHR0202:620`), nếu không thì giao diện tự đếm.
- **Phiên bản**: duyệt/đổi hiển thị theo BR-11.

## BR-18 — Giao bài và gỡ bài theo lớp (F2-12)

- **Giao** (§53): BR-02 lớp phụ trách; mỗi bài phải `PUBLISHED AND NOT deleted` (`422 PRB-037` từng bài); `INSERT class_assignments (problem_id, class_id, assigned_by, assigned_at = now()) ON CONFLICT (problem_id, class_id) WHERE removed_at IS NULL DO NOTHING` (partial unique); chèn được -> `succeededIds` + Outbox `ProblemAssignedToClass` trong **một giao dịch riêng cho bài đó**; không chèn được -> `skippedIds`. Nếu ghi Outbox lỗi thì rollback riêng bài, trả `PRB-056` trong `failedItems` của lô HTTP 200; các bài khác vẫn xử lý. Giao lại sau gỡ **tạo dòng mới**, dòng cũ giữ `removed_at`. [Chủ dự án chốt 2026-10-27]
- **Gỡ** (§54): `UPDATE class_assignments SET removed_at = now() WHERE problem_id = ? AND class_id = ? AND removed_at IS NULL`; 0 dòng -> `409 PRB-025`; gỡ chỉ ẩn bài khỏi danh sách được giao **từ thời điểm gỡ**; **không xoá** lượt nộp, điểm, tiến độ (`DEC-2026-0831-class-assignments-round2`, khác hẳn cách gỡ học viên). Phát `ProblemUnassignedFromClass`.
- **Hiển thị**: học viên thấy bài (đang giao, còn xuất bản) trong khối riêng cạnh bảng chính, tối đa 3 lớp (BR-23). Giảng viên thấy cả bài đã rút kèm cờ `withdrawn`.
- **Danh sách đang giao** (§50, cổng `listAssignments`): nhóm theo bài, `assignedClasses` là các lớp còn dòng hoạt động trong phạm vi lọc; số liệu nộp bài lấy qua `ClassProblemMetricsPort` chỉ học viên còn ghi danh (`left_at IS NULL`).
- **Tóm tắt** (§51): `avgAcRatePercent` = trung bình cộng AC% của các bài đang giao **có lượt nộp**, làm tròn số nguyên.
- **Quyền**: không giữ `instructor_id` ở module này, mọi kiểm sở hữu lớp hỏi `InstructorClassPort` mỗi yêu cầu (`INS0202:515`).

## BR-19 — Xử lý `ClassDeleted`

- **Kích hoạt**: nhận sự kiện `ClassDeleted { classId }` từ `identity` (BR-22 của `identity`, `03-dd/logic/F1_identity.md`).
- **Các bước**: `UPDATE class_assignments SET removed_at = now() WHERE class_id = :classId AND removed_at IS NULL` (gỡ mềm như thao tác gỡ thường, không xoá dòng, không FK tới `identity.classes`).
- **Idempotent tự nhiên**: nhận lại thì không còn dòng khớp. Không cần bảng dedupe cho sự kiện này.
- **Không làm**: không phát `ProblemUnassignedFromClass` (lớp đã không còn, tránh ồn) `[SoT: Suy luận]`; không ghi nhật ký (không có người thao tác); không đụng lịch sử nộp.
- **Lỗi**: xử lý thất bại thì nack để broker giao lại; dòng treo tạm thời vô hại vì mọi truy vấn lớp đều đi qua `identity` để xác định lớp còn tồn tại.

## BR-20 — Cập nhật `problem_stats` từ `SubmissionGraded` (F2-11)

- **Kích hoạt**: nhận `SubmissionGraded` (`03-dd/api/F4_judge-orchestration.md` mục 25.2). **Giao at-least-once**, nên khử trùng bắt buộc.
- **Các bước** (một giao dịch):
  1. `INSERT INTO consumed_events (event_id, consumed_at) VALUES (:eventId, now()) ON CONFLICT DO NOTHING`; 0 dòng chèn -> bỏ qua toàn bộ (`Q-02`, `Q-36`).
  2. `verdict = SYSTEM_ERROR` -> không đếm (`Q-18`), dừng sau bước 1.
  3. `problemId` không tồn tại trong `problems` -> ghi log cảnh báo, bỏ qua.
  4. `acc = 1` nếu `verdict = ACCEPTED`, ngược lại `0`. Upsert: `INSERT INTO problem_stats (problem_id, submission_count, accepted_count, ac_rate, updated_at) VALUES (:p, 1, :acc, :acc * 100.0, now()) ON CONFLICT (problem_id) DO UPDATE SET submission_count = problem_stats.submission_count + 1, accepted_count = problem_stats.accepted_count + :acc, ac_rate = round((problem_stats.accepted_count + :acc) * 100.0 / (problem_stats.submission_count + 1), 2), updated_at = now()`. `ac_rate` được ghi trong **cùng câu lệnh** với hai bộ đếm (`02-bd/database/F2_problem-bank.md:215`).
- **Không nghe `SubmissionAccepted`**: trùng thông tin với `verdict = ACCEPTED`, nghe cả hai là đếm đôi.
- **Hậu điều kiện**: dòng chưa tồn tại được tạo ở sự kiện đầu tiên; bài chưa ai nộp không có dòng; `ac_rate IS NULL` chỉ khi không có dòng. Cache danh sách lệch tối đa 60 giây (chấp nhận).
- **CHECK DB**: `accepted_count <= submission_count`.

## BR-21 — Bookmark riêng tư (F2-13)

- **Quyền sở hữu dữ liệu**: `user_id` luôn lấy từ token; không endpoint nào nhận `user_id`; không có route nào cho A2/A3 đọc, đếm hay thống kê bookmark của người khác, kể cả số tổng ẩn danh (`02-bd/security/F2_problem-bank.md:73-81`). Vai trò hay ma trận phân quyền không mở ngoại lệ.
- **Lưu/sửa** (§10): upsert theo `(user_id, problem_id)`; `createIfMissing = false` mà thiếu dòng -> `404 PRB-014` (không tạo lại âm thầm); tạo mới đòi bài `PUBLISHED AND NOT deleted`; sửa ghi chú và bỏ lưu **được phép** với bài đã rút.
- **Ghi chú**: cắt khoảng trắng; rỗng -> `NULL`; tối đa 1.000 ký tự; văn bản thuần (không dựng Markdown/HTML). Không đưa ghi chú vào bất kỳ prompt nào.
- **Bỏ lưu** (§11): xoá thật dòng (kéo theo `note`); idempotent.
- **Toggle** (§12): trạng thái đích; `true` không ghi đè ghi chú sẵn có; `false` xoá dòng.
- **`savedAt`** = `created_at`; Hoàn tác tạo lại dòng nên ngày lưu là ngày tạo lại (`USR0103:586`).
- **Đọc** (§9): `withdrawn` do máy chủ tính từ `status`/`deleted`, không lộ hai cột thô.

## BR-22 — Quản lý danh mục chủ đề và độ khó (F2-02)

- **Quyền**: `PROBLEM_AUTHORING:<hành động>` **và** vai trò `ADMIN` (`403 PRB-004`); đọc mở cho mọi người dùng đã xác thực.
- **Tạo**: kiểm tên (BR trong `validation` bảng 7.1), sinh `code` (BR-03), `sort_order = max + 1`, không giới hạn số lượng.
- **Đổi tên**: `code` giữ nguyên; tên không đổi thì không ghi nhật ký.
- **Sắp xếp lại**: nhận danh sách **đầy đủ** id; khoá các dòng; so tập (`422 PRB-049` nếu lệch); ghi `sort_order = 1..n`. Thứ tự này quyết định tab, ô chọn và **thứ tự sắp theo cột "Độ khó"** (`02-bd/database/F2_problem-bank.md:98`).
- **Xoá chủ đề**: từ chối khi còn `problem_topics.topic_id` trỏ tới (mọi trạng thái bài, kể cả `deleted`) -> `422 PRB-031` kèm số đếm; FK `NO ACTION` là lưới an toàn.
- **Xoá độ khó**: hai bất biến: (1) còn `problems.level_id` trỏ tới -> `422 PRB-032`; (2) mức cuối cùng -> `422 PRB-033`. Khoá toàn bộ dòng `problem_levels` trong giao dịch để hai lần xoá song song không cùng qua kiểm (2).
- **Độ khó không mang logic**: không điều khiển điểm, giới hạn, luồng chấm (`02-bd/architecture/F2_problem-bank.md:54`).
- **Nhật ký**: mỗi thao tác thành công đúng một dòng qua `AuditLogPort` cùng giao dịch (BR-27). Tăng phiên bản cache.

## BR-23 — Lọc theo trạng thái giải, bốc ngẫu nhiên, gợi ý, tiến độ chủ đề

- **Nguồn trạng thái giải**: `SolveStatePort` (`identity`), map `problemId -> SOLVED|ATTEMPTED`; thiếu trong map = `UNSOLVED`. Cache Redis `problem:solvestate:<userId>` TTL 60 giây phía `problem-bank`. Lỗi cổng: `solveState = null`, bỏ lọc `status`, `solveStateAvailable = false`; **bảng vẫn hiển thị** (F1-F4 không phụ thuộc thành phần phụ).
- **Lọc `status`**: `SOLVED` -> `id IN (...)`; `ATTEMPTED` -> `id IN (...)`; `UNSOLVED` -> `id NOT IN (cả hai)`; `NOT_SOLVED` (chỉ bốc ngẫu nhiên) -> `id NOT IN (SOLVED)`.
- **Bốc ngẫu nhiên** (§3): chọn đều trong tập khớp; tập rỗng -> `{ problemId: null }`.
- **Tiến độ chủ đề** (§4): `total` đếm bài `PUBLISHED AND NOT deleted` của chủ đề; `solved` giao với tập `SOLVED`.
- **Gợi ý** (§13): lọc `levelCode`/`topic`; F2 tính vector điểm chủ đề 70/30 một lần từ tiến độ bài đang xuất bản (F1 `SolveStatePort`) và số đếm nộp bài lịch sử `all` (F4 P5 `MyTopicSubmissionMetricsPort`, nhận bản đồ chủ đề–bài do F2 gửi, không gọi ngược F2). Trả vector trong `topicScores` cho lời chào/radar/ưu tiên của Dashboard và dùng chính nó để xếp bài: chưa giải (gồm `ATTEMPTED`) trước; trong nhóm theo điểm chủ đề nhỏ nhất của bài tăng dần, chủ đề chưa có điểm coi như 100; cùng hạng theo `code`; lấy 7. Không gọi AI. `[SoT: DEC-2026-1024-dd-f2-topic-score-source]`
- **Mẫu số** của mọi con số trên: BR-01.

## BR-24 — Số liệu quản lý (F2-11, F2-17)

- **Phạm vi**: `deleted = false`; A2 thêm `author_id = người gọi`.
- **Phân bố theo chủ đề** (§26): `COUNT(DISTINCT problem_id)` theo `topic_id`; `percent = count / tổng bài có ít nhất một chủ đề x 100`; tổng 0 thì trả rỗng, không chia cho 0.
- **Bài cần chú ý** (§27): bốn quy tắc `NO_TESTCASE`, `LOW_AC` (`ac_rate < 30`, bỏ `NULL`), `HIDDEN_FROM_LEARNERS` (`UNPUBLISHED AND published_at IS NOT NULL`), `STALE_DRAFT` (`UNPUBLISHED AND published_at IS NULL AND updated_at` cũ hơn 7 ngày). Ngưỡng cấu hình `application.yml`; `LOW_AC` dùng thang 0-100 (`Q-12`).
- **Xuất CSV** (§28): chỉ tám cột bảng; không đề bài, đặc tả, testcase; ô bắt đầu `= + - @ \t \r` được thêm `'` ở đầu; id ngoài phạm vi A2 bị bỏ qua; không ghi nhật ký.
- **Danh sách** (§25): `assignedClassCount` đếm lớp có dòng hoạt động, chỉ để cảnh báo hộp xoá.

## BR-25 — Cache Redis

- **`problem:list:v<phiên bản>:<hash>`** TTL 60 giây (`02-bd/database/F2_problem-bank.md:279`): cache phần danh sách không phụ thuộc người dùng (không gồm `solveState` và không có `status`). Khoá phiên bản `problem:list:version` tăng (`INCR`) khi: xuất bản/rút xuống/xoá, lưu nội dung bài `PUBLISHED`, đổi độ khó/chủ đề theo lô, mọi thao tác danh mục. Nhờ đó bài rút xuống **biến ngay** khỏi `problem_list` (RD F2-15) trong khi `acRate` cũ tối đa 60 giây.
- **`problem:spec:<problemId>`**: sở hữu bởi `problem-bank`; xoá chủ động khi `problem_specs` đổi (BR-05); `harness` đọc qua `ProblemSpecQuery.findSpec`.
- **`problem:solvestate:<userId>`** TTL 60 giây; **`problem:upload:<uploadId>`** TTL 20 phút; **`problem:gen:lock:<problemId>`** TTL 10 phút.
- **Suy giảm êm**: Redis chết -> bỏ cache, đọc thẳng DB; khoá chống song song sinh testcase không lấy được thì từ chối sinh (`503`) thay vì chạy không khoá.

## BR-26 — Phát sự kiện và cổng đồng bộ

- **Ghi Outbox trong giao dịch** đổi dữ liệu: `INSERT problem.outbox_events` với UUID `eventId`, `aggregateId`, `eventType`, `routingKey`, phong bì payload và `createdAt`. Relay JOB-PB-01 phát sau commit lên exchange `algoprep.domain-events`, đánh dấu `published_at` chỉ khi broker xác nhận. Lỗi broker giữ dòng để thử lại; lỗi ghi Outbox rollback cả thay đổi nghiệp vụ. [DD F2 Q-17]
- **Sáu loại sự kiện**: `ProblemSpecChanged`, `ProblemPublished`, `ProblemUnpublished`, `TestcaseSetVersionBumped`, `ProblemAssignedToClass`, `ProblemUnassignedFromClass` (payload ở `03-dd/api/F2_problem-bank.md` mục 59).
- **Dữ liệu cần chính xác không đi bằng sự kiện**: `specVersion`, `currentTestcaseSetVersion` trả đồng bộ qua `loadGradingSpec`; tên bài qua `ProblemSummaryReadPort`. Mất sự kiện chỉ làm cache bên nhận cũ hơn tới TTL.
- **Nhận**: `SubmissionGraded` (BR-20), `ClassDeleted` (BR-19); consumer idempotent.

## BR-27 — Nhật ký hệ thống qua `AuditLogPort` (F1-14)

- **Quy tắc**: mọi thao tác quản trị danh mục và mọi hành động ghi trên bài ở màn quản lý gọi `AuditLogPort.record(actorUserId, actionType, targetType, targetId, beforeJson, afterJson)` **trong cùng JVM và cùng giao dịch DB**; không ghi thẳng schema `identity` (`02-bd/architecture/F1_identity.md:92`). Nhật ký lỗi thì thao tác rollback (`SHR0201` Sheet 9 NO 9, 14, 18).
- **Bảng `actionType`** `[SoT: Suy luận, Q-14]`:

| `actionType` | `targetType` | Khi nào |
| :--- | :--- | :--- |
| `PROBLEM_TOPIC_CREATED`, `PROBLEM_TOPIC_RENAMED`, `PROBLEM_TOPIC_REORDERED`, `PROBLEM_TOPIC_DELETED` | `PROBLEM_TOPIC` | §21-24 |
| `PROBLEM_LEVEL_CREATED`, `PROBLEM_LEVEL_RENAMED`, `PROBLEM_LEVEL_REORDERED`, `PROBLEM_LEVEL_DELETED` | `PROBLEM_LEVEL` | §16-19 |
| `PROBLEM_PUBLISHED`, `PROBLEM_UNPUBLISHED` | `PROBLEM` | §29, 30, 40 |
| `PROBLEM_DIFFICULTY_CHANGED` | `PROBLEM` | §31 (một dòng mỗi bài) |
| `PROBLEM_TOPIC_ASSIGNED` | `PROBLEM` | §32 (chỉ khi thêm quan hệ mới) |
| `PROBLEM_DUPLICATED` | `PROBLEM` | §36 có `duplicatedFromProblemId` |
| `PROBLEM_DELETED` | `PROBLEM` | §33 |

- **Đổi trần sinh testcase AI**: endpoint §57 ghi `AI_TESTCASE_GENERATION_LIMIT_CHANGED` (`PROBLEM_BANK_SETTINGS`) cùng giao dịch cấu hình, nhóm `CONFIG` ở F1; lỗi audit thì rollback. [Owner 2026-10-25, Q-13]
- **Không ghi nhật ký**: đọc, xuất CSV, lưu nội dung/đặc tả/testcase, giao/gỡ bài theo lớp, bookmark.
- **Dữ liệu nhạy cảm**: `beforeJson`/`afterJson` chỉ chứa id và giá trị cần so (tên, mức, trạng thái); **không** chứa nội dung đề, mã đáp án mẫu, testcase. Che khoá nhạy cảm là việc của tầng đọc nhật ký (`identity` BR-16).

## BR-28 — Baseline migration đã đóng (2026-10-29)

Theo `DEC-2026-1029-f2-migration-readiness`: tag find-or-create phải dùng lower(name) cùng unique expression index, an toàn đồng thời. Epsilon khi được gửi/lưu phải >0, <=999999.999999999999, biểu diễn chính xác với scale tối đa 12 sau bỏ số 0 dư; EPSILON bắt buộc có. Kiểm trước ghi, không để DB làm tròn epsilon âm thầm. Khi SaveProblemSpec vi phạm miền epsilon, trả 422 PRB-036 với HRN-V20. Trạng thái chữ ký cùng NULL hoặc đủ ba trường; parameters [] hợp lệ. DDL/seed/NOT NULL/CHECK theo BD database §7; application đặt updated_at trên mọi ghi, không dựa vào DEFAULT để cập nhật.
