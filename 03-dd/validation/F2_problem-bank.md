# DD — Validation module `problem-bank` (F2)

> Nguồn: `02-bd/screens/**` Sheet 5 (độ dài, bắt buộc) và Sheet 9 (đặc tả kiểm tra) của `SHR0201`, `SHR0202`, `USR0101`, `USR0103`, `INS0202`; `02-bd/database/F2_problem-bank.md` (ràng buộc cột);
> `02-bd/security/F2_problem-bank.md` mục 5. Hợp đồng endpoint ở `03-dd/api/F2_problem-bank.md` (số mục `§n` dưới đây là số mục ở file đó); quy tắc nghiệp vụ ở `03-dd/logic/F2_problem-bank.md`; tiêu chí nghiệm thu ở `04-tdd/modules/F2_problem-bank.md`.
> Cập nhật: 2026-10-23.
>
> Chỉ ghi quy tắc kiểm mà BD **chưa** tự suy ra được: số cụ thể, mã lỗi, tầng thực thi. Không lặp lại luật kiểm chữ ký hàm: luật đó của `harness` và nằm ở `03-dd/validation/F3_harness.md` (mã `HRN-Vnn`), `problem-bank` chỉ gọi
> `FunctionSpecEvaluator.evaluate` rồi gói kết quả (mục 3). Lược đồ zod phía giao diện (`03-dd/screens/`) **phải khớp** các bảng này; lệch phải nêu lý do.

## 1. Quy ước

Bảng 1.1: Ký hiệu cột "Tầng".

| Ký hiệu | Nghĩa |
| :--- | :--- |
| C | client (zod ở giao diện), kiểm sớm cho người dùng, **không phải** lớp bảo vệ duy nhất |
| S | server (tầng application), là lớp kiểm có thẩm quyền |
| DB | ràng buộc cột (CHECK, NOT NULL, UNIQUE, FK) làm lưới an toàn |

- Mọi chuỗi nhập được cắt khoảng trắng hai đầu trước khi kiểm độ dài, **trừ** `statementMd`, `constraintsMd`, `stdinFormatMd`, `stdoutFormatMd`, `sourceCode`, `input`, `expectedOutput`, `briefText` và
  `explanationMd` (giữ nguyên văn để không mất định dạng, `02-bd/security/F2_problem-bank.md:111-114`); riêng `briefText` và `explanationMd` rỗng sau cắt thì lưu `NULL`.
- Lỗi kiểm đầu vào ở endpoint ghi trả `400 PRB-001` với `details.fields[] = { field, rule }` (`rule` là một trong các khoá ở cột "Khoá rule" bên dưới). Endpoint danh sách cho học viên **không** trả `400`: giá trị lạ bị bỏ qua (mục 9).
- Các con số đánh dấu `[SoT: Suy luận]` là trần kỹ thuật do DD đề xuất vì RD/BD giao DD chốt (`02-bd/security/F2_problem-bank.md:115-117`, `Q-24` ở `03-dd/api/F2_problem-bank.md` mục 61); chủ dự án chỉnh được, đặt ở `application.yml`.

## 2. Nội dung bài toán (`SaveProblemContent`, §36, §37)

Bảng 2.1: Quy tắc kiểm trường nội dung. Cột "Khoá rule" dùng trong `details.fields[].rule`.

| Trường | Quy tắc | Khoá rule | Mã lỗi | Tầng | Nguồn |
| :--- | :--- | :--- | :--- | :-: | :--- |
| `title` | bắt buộc, 1 đến 255 ký tự | `required`, `maxLength` | PRB-001 | C, S, DB (`VARCHAR(255) NOT NULL`) | `SHR0202` Sheet 5 Khu C NO 1; `database/F2_problem-bank.md:19` |
| `statementMd` | bắt buộc, không rỗng; tối đa 100.000 ký tự `[SoT: Suy luận]` | `required`, `maxLength` | PRB-001 | C, S | Sheet 9 NO 3 (`SHR0202:765`) |
| `constraintsMd` | không bắt buộc, mặc định `''`; tối đa 20.000 ký tự `[SoT: Suy luận]` | `maxLength` | PRB-001 | C, S, DB (`NOT NULL default ''`) | `database/F2_problem-bank.md:21` |
| `levelId` | tồn tại trong `problem_levels`; **tạo mới** thiếu thì lấy mức đầu theo `sort_order`; **cập nhật** bắt buộc | `required`, `exists` | PRB-001 (thiếu), PRB-039 (không còn tồn tại) | S, DB (FK `NOT NULL`) | `SHR0202` Q21 (dòng 806), Khu H NO 2 |
| `limits.timeLimitMs`, `limits.memoryLimitMb`, `limits.maxOutputSizeKb` | số nguyên dương; khi **tạo** thiếu thì điền từ `grading_defaults` (`JudgeConfigReadPort.getGradingDefaults`); khi **cập nhật** bắt buộc | `positiveInt`, `required` | PRB-001 | C, S, DB (`CHECK > 0`) | Sheet 9 NO 3, 4 (`SHR0202:765-766`); `database/F2_problem-bank.md:26-28,42` |
| `limits.maxSubmissionsPerHour` | `null` hoặc số nguyên dương; `null` = đọc mặc định hệ thống 60 lúc kiểm, **không** ghi 60 vào cột | `positiveInt` | PRB-001 | C, S | `database/F2_problem-bank.md:29`; Sheet 9 NO 4 |
| `topicIds[]` | mỗi id tồn tại trong `topics`; không trùng nhau (trùng thì gộp) | `exists` | PRB-039 | S, DB (FK) | `SHR0202` Sheet 5 Khu H NO 1 |
| `tags[]` | mỗi thẻ 1 đến 50 ký tự sau cắt, không phân biệt hoa thường khi so trùng; tối đa 20 thẻ mỗi bài `[SoT: Suy luận]`; thẻ chưa có thì tạo | `maxLength`, `maxItems` | PRB-001 | C, S, DB (`UNIQUE lower(tags.name)`) | `database/F2_problem-bank.md:112` |
| `referenceSolution.language` | một trong `JAVA`, `CPP`, `PYTHON` | `enum` | PRB-001 | C, S, DB (ENUM) | `database/F2_problem-bank.md:239` |
| `referenceSolution.sourceCode` | không rỗng khi có `referenceSolution`; tối đa 64 KB UTF-8 `[SoT: Suy luận]`; rỗng/`null` toàn bộ nghĩa là xoá dòng | `required`, `maxBytes` | PRB-001 | C, S, DB (`NOT NULL`) | Sheet 5 Khu C NO 9-10 |
| `workedExamples[].input`, `.output` | bắt buộc, không rỗng; văn bản ngắn tối đa 8 KB mỗi trường `[SoT: Suy luận]` (không có nhánh MinIO) | `required`, `maxBytes` | PRB-001 | C, S, DB (`NOT NULL`) | Sheet 5 Khu D NO 2-3; `database/F2_problem-bank.md:254-255` |
| `workedExamples[].explanationMd` | tuỳ chọn, tối đa 4.000 ký tự `[SoT: Suy luận]` | `maxLength` | PRB-001 | C, S | Sheet 5 Khu D NO 4 |
| `workedExamples` (bài đang `PUBLISHED`) | sau khi lưu phải còn **ít nhất 2** ví dụ | `minItemsWhilePublished` | PRB-053 | C (toast), S | Sheet 6 Khu D NO 6 (`SHR0202:517`) |
| `duplicatedFromProblemId` | chỉ khi tạo; bài nguồn tồn tại, chưa `deleted`, A2 chỉ bài của mình | `exists` | PRB-010, PRB-003 | S | `SHR0201` EVT-14 |
| `expectedUpdatedAt` | bắt buộc khi cập nhật; bằng `problems.updated_at` hiện tại | `versionMatch` | PRB-020 | S | Sheet 9 NO 10 (`SHR0202:772`) |

## 3. Đặc tả bài toán (`SaveProblemSpec`, §38)

Bảng 3.1: Phân vai kiểm giữa `problem-bank` và `harness`.

| Quy tắc | Do ai kiểm | Mã | Loại | Ghi chú |
| :--- | :--- | :--- | :--- | :--- |
| Kiểu JSON cấp ngoài: `parameters` là mảng, `nameOverrides` là đối tượng, `matchingStrategy` thuộc bốn giá trị | `problem-bank` (S) | PRB-001 | từ chối | |
| `stdinFormatMd`, `stdoutFormatMd` tối đa 20.000 ký tự | `problem-bank` (C, S) | PRB-001 | từ chối | `[SoT: Suy luận]` |
| Hình dạng nút kiểu, `kind` hợp lệ, `of`, số chiều, độ sâu lồng, kiểu node thống nhất, chữ ký vắng | `FunctionSpecEvaluator` | `HRN-V01` tới `HRN-V07` | **mềm**: lưu được, `wrapperSupported = false`, trả `PRB-W01` | `03-dd/validation/F3_harness.md` mục 2 và 3 |
| Nút kiểu hỏng cấu trúc, tên hàm, tên tham số, trùng tên, từ dành riêng, số tham số, chữ ký nửa vời, khoá và giá trị `nameOverrides` | `FunctionSpecEvaluator` | `HRN-V10` tới `HRN-V19` | **cứng**: `422 PRB-036` | định danh `^[A-Za-z][A-Za-z0-9_]{0,63}$`, tối đa 10 tham số (do `harness` chốt) |
| `EPSILON` thiếu `epsilonValue`; hoặc epsilon được lưu không dương, >999999.999999999999 hay không biểu diễn chính xác với scale <=12 | `FunctionSpecEvaluator` (và CHECK DB) | `HRN-V20` | cứng: `422 PRB-036` | `epsilonValue` ở chiến lược khác được NULL hoặc giữ giá trị hợp lệ theo cùng miền; chỉ bỏ qua khi so khớp, không bỏ qua validation. Scale tính sau bỏ số 0 thập phân dư; không làm tròn trước kiểm |
| `UNORDERED_SET` với `BINARY_TREE`/`LINKED_LIST` | `FunctionSpecEvaluator` | `HRN-V22` | cứng: `422 PRB-036` | câu chữ `SHR0202:768` |
| `epsilonValue` | `NUMERIC(18,12)`, `> 0` | `HRN-V20` | | `database/F2_problem-bank.md:129` |

`problem-bank` **không** tự viết lại các luật `HRN-Vnn` (tránh lệch luật hai nơi, `03-dd/api/F3_harness.md:450` Q1). Khoá `name_overrides` lưu chữ HOA (`Q-34`).

## 4. Ngữ cảnh AI của bài (`SaveAiAuthoringContext`, §39)

Bảng 4.1.

| Trường | Quy tắc | Khoá rule | Mã lỗi | Tầng | Nguồn |
| :--- | :--- | :--- | :--- | :-: | :--- |
| `briefText` | tuỳ chọn; tối đa 4.000 ký tự `[SoT: Suy luận]`; rỗng sau cắt lưu `NULL`; lưu **nguyên văn như dữ liệu**, không lọc từ khoá | `maxLength` | PRB-001 | C, S | `database/F2_problem-bank.md:38` |
| `noFullCode`, `questionOnly` | bắt buộc, kiểu boolean | `required` | PRB-001 | C, S, DB (`NOT NULL`) | `database/F2_problem-bank.md:39-40` |
| quyền sửa | `PROBLEM_AUTHORING:UPDATE` (không đủ với chỉ `TESTCASE_MANAGEMENT`) | | PRB-002 | S | `SHR0202` Sheet 6 Khu G NO 2 (`:564`) |

## 5. Testcase (§42 tới §47)

Bảng 5.1.

| Trường | Quy tắc | Khoá rule | Mã lỗi | Tầng | Nguồn |
| :--- | :--- | :--- | :--- | :-: | :--- |
| `visibility` | `SAMPLE` hoặc `HIDDEN`; mặc định `HIDDEN` | `enum` | PRB-001 | C, S, DB (ENUM) | Sheet 5 Khu E NO 11 |
| `input`, `expectedOutput` (nhập trực tiếp) | bắt buộc, không rỗng; tối đa 1 MB UTF-8 mỗi vế `[SoT: Suy luận]` | `required`, `maxBytes` | PRB-001 (rỗng), PRB-048 (quá 1 MB) | C, S | Sheet 5 Khu E NO 9-10 |
| ngưỡng lưu trữ mỗi vế | `<= 8.192` byte -> `INLINE`; lớn hơn -> `MINIO`; hai vế quyết định độc lập | | | S, DB (hai mode NOT NULL; hai CHECK bắt buộc đúng một nhánh, nhánh kia NULL) | `storage/F2_problem-bank.md:14`; `database/F2_problem-bank.md:166` |
| `category` | `null` hoặc một trong bảy giá trị `TestcaseCategory` | `enum` | PRB-001 | C, S, DB (ENUM) | `database/F2_problem-bank.md:162` |
| `approved` | chỉ nhận `true`; không hạ `true -> false` | `onlyTrue` | PRB-001 | S | BR-17 |
| số testcase mỗi bài | tối đa 500 `[SoT: Suy luận]` | `maxItems` | PRB-046 | S | `security/F2_problem-bank.md:115-117` |
| `orderedIds` (`ReorderTestcases`) | đúng bằng tập id testcase của bài, không trùng | `setEquals` | PRB-049 | S | `database/F2_problem-bank.md:163` |
| `position` | số nguyên, không unique; máy chủ tự đặt | | | S | `database/F2_problem-bank.md:163` |

Bảng 5.2: Tải lên theo lô (`RequestTestcaseBulkUploadUrl`, `ConfirmTestcaseBulkUpload`).

| Trường | Quy tắc | Mã lỗi | Tầng | Ghi chú |
| :--- | :--- | :--- | :-: | :--- |
| `items[]` | 1 đến 100 cặp mỗi yêu cầu `[SoT: Suy luận]` | PRB-001 (rỗng), PRB-047 (quá 100) | S | |
| `items[].clientKey` | 1 đến 100 ký tự, duy nhất trong yêu cầu | PRB-001 | C, S | chỉ để giao diện khớp kết quả |
| `inputSizeBytes`, `expectedOutputSizeBytes` | số nguyên dương, mỗi vế tối đa 64 MB `[SoT: Suy luận]` | PRB-001, PRB-047 | C, S | |
| tổng một yêu cầu | tối đa 256 MB `[SoT: Suy luận]` | PRB-047 | S | |
| tổng testcase sau khi thêm | tối đa 500 | PRB-046 | S | |
| `uploadId` | tồn tại ở Redis, thuộc đúng bài và đúng người, chưa quá 20 phút | PRB-044 | S | |
| object đã tải | tồn tại, kích thước **bằng** kích thước đã khai; vế `<= 8 KB` phải là UTF-8 hợp lệ | PRB-045 | S | không thêm testcase một phần |
| presigned PUT | hiệu lực 15 phút; khoá do máy chủ sinh từ `testcaseId`, không nhận khoá từ client | | S | `storage/F2_problem-bank.md:47` |
| presigned GET (tác giả) | hiệu lực 5 phút; chỉ với `TESTCASE_MANAGEMENT:READ` và là tác giả/ADMIN; **không bao giờ** cho học viên với testcase `HIDDEN` | PRB-002 | S | `storage/F2_problem-bank.md:48-52` |

## 6. Chạy đáp án mẫu và sinh testcase AI (§48, §49)

Bảng 6.1.

| Trường / điều kiện | Quy tắc | Mã lỗi | Tầng | Nguồn |
| :--- | :--- | :--- | :-: | :--- |
| `language` và `sourceCode` (ghi đè khi chạy) | cùng có hoặc cùng thiếu; mã tối đa 64 KB | PRB-001 | C, S | |
| đáp án mẫu có sẵn | cần có dòng đã lưu hoặc mã kèm yêu cầu | PRB-040 | S | F2-18 |
| `testcaseId` (chạy một dòng) | thuộc bài | PRB-011 | S | |
| điều kiện sinh: ràng buộc dữ liệu | `constraints_md` không rỗng sau cắt | PRB-041 (`NO_CONSTRAINTS`) | C (nút mờ), S | `SHR0202:770` |
| điều kiện sinh: Sample tự viết | có **ít nhất 2** testcase `SAMPLE`, `origin = MANUAL`, `approved = true` | PRB-041 (`NOT_ENOUGH_MANUAL_SAMPLES`) | C, S | RD F2-14 (`01-rd/req/F2_problem-bank.md:117-119`) |
| điều kiện sinh: đáp án mẫu pass | mọi testcase đã duyệt chạy `PASS` ở lần chạy lúc bấm | PRB-041 (`REFERENCE_NOT_PASSING`) | S | `SHR0202` Sheet 9 NO 8 |
| lượt sinh | `ai_testcase_generations_used < max_ai_generations_per_problem` | PRB-042 | C (hiện "Còn N/M lần"), S | `database/F2_problem-bank.md:37,227` |
| một lượt sinh tại một thời điểm | khoá Redis `problem:gen:lock:<problemId>` TTL 10 phút | PRB-023 | S | |
| script sinh | vượt `max_runtime_seconds`, vượt `max_output_mb`, lỗi chạy, output sai định dạng -> loại cả script, không tính lượt | PRB-043 | S | RD F2-14 bước 4 |
| từng input sinh ra | phải qua script kiểm ràng buộc; trùng input đã có bị bỏ; làm đáp án mẫu lỗi/quá giờ bị loại | (không lỗi, vào `dropped[]`) | S | RD bước 6, 7 |
| hạt giống | cố định, truyền vào script; lưu cùng script | | S | RD bước 5, 8 |
| dữ liệu gửi cho AI | chỉ đề bài, ràng buộc, đúng 2 Sample; **không** mã đáp án mẫu, `ai_brief`, testcase ẩn | | S (kiểu cổng không có trường đó) | RD bước 1-2; `security/F2_problem-bank.md:85-92` |

## 7. Danh mục độ khó và chủ đề (§15 tới §24)

Bảng 7.1.

| Trường / điều kiện | Quy tắc | Mã lỗi | Tầng | Nguồn |
| :--- | :--- | :--- | :-: | :--- |
| `displayName`, `name` | bắt buộc, 1 đến 60 ký tự sau cắt; không trùng không phân biệt hoa thường | PRB-001 (rỗng, dài), PRB-022 (độ khó trùng), PRB-021 (chủ đề trùng) | C, S, DB (unique `lower(...)`) | `SHR0201` Sheet 9 NO 12, 16 (dòng 803, 807) |
| vai trò | ghi: `PROBLEM_AUTHORING:<hành động>` **và** vai trò `ADMIN` | PRB-002 / PRB-004 | S | `security/F2_problem-bank.md:17-41` |
| `orderedIds` | đúng bằng tập id hiện có, không trùng | PRB-049 | S | `database/F2_problem-bank.md:75-77` |
| xoá chủ đề | không còn `problem_topics.topic_id` trỏ tới (mọi trạng thái bài) | PRB-031 | S, DB (FK `NO ACTION`) | Sheet 9 NO 13 |
| xoá độ khó | không còn `problems.level_id` trỏ tới; không phải mức cuối | PRB-032, PRB-033 | S, DB (FK) | Sheet 9 NO 17 |
| `code` | do máy chủ sinh, bất biến, duy nhất | | S, DB (UNIQUE) | BR-03 |
| `includeCounts` | chỉ vai trò `ADMIN` được hưởng, người khác bị bỏ qua | | S | |

## 8. Bookmark (§9 tới §12)

Bảng 8.1.

| Trường | Quy tắc | Khoá rule | Mã lỗi | Tầng | Nguồn |
| :--- | :--- | :--- | :--- | :-: | :--- |
| `note` | tối đa 1.000 ký tự sau cắt; rỗng sau cắt lưu `NULL`; **văn bản thuần**, không dựng Markdown/HTML khi hiển thị | `maxLength` | PRB-001 | C, S | Sheet 9 NO 5 (`USR0103:584`) |
| `createIfMissing` | mặc định `true`; `false` mà không có dòng | | PRB-014 | S | `USR0103:585` |
| tạo mới bookmark | bài phải `PUBLISHED AND NOT deleted` | | PRB-010 | S | |
| sửa ghi chú, bỏ lưu | **không** từ chối khi bài đã rút xuống | | | S | `USR0103:582` |
| `bookmarked` (`ToggleMyProblemBookmark`) | bắt buộc boolean | `required` | PRB-001 | C, S | |
| `user_id` | luôn lấy từ token; mọi tham số `userId` do client gửi bị bỏ qua | | | S | `security/F2_problem-bank.md:73-81` |

## 9. Tham số danh sách

Bảng 9.1: Học viên (đọc **rộng**, giá trị lạ bị bỏ qua và dùng mặc định, không báo lỗi; `USR0101` Sheet 9 NO 6, `USR0103` NO 9).

| Tham số | Giá trị hợp lệ | Mặc định khi lạ | Tầng |
| :--- | :--- | :--- | :-: |
| `q` | tối đa 100 ký tự, dài hơn thì cắt còn 100 (`USR0101:627`) | cắt | C (`maxLength`), S |
| `status` | `SOLVED`, `ATTEMPTED`, `UNSOLVED`; riêng `GetRandomProblem` thêm `NOT_SOLVED` | bỏ lọc | S |
| `difficulty` | `levelCode` có trong `problem_levels` | "Tất cả" | S |
| `topic` | uuid của chủ đề có thật | bỏ lọc | S |
| `tag` | tên thẻ | bỏ lọc | S |
| `sort`, `dir` | `level`, `acRate` / `asc`, `desc`; bookmark: `savedAt` | `code` tăng (danh sách), `savedAt` giảm (bookmark) | S |
| `page` | số nguyên từ 1; quá số trang thì trả trang cuối | 1 | S |
| `pageSize` | 12/24/48 (danh sách), cố định 20 (bookmark, giao bài) | 12 | S |

Bảng 9.2: Quản trị (đọc **nghiêm**, sai thì `400 PRB-001`).

| Tham số | Giá trị hợp lệ | Tầng |
| :--- | :--- | :-: |
| `q` | tối đa 100 ký tự (`SHR0201:795`) | C, S |
| `level` | uuid mức độ khó | S |
| `status` | `UNPUBLISHED`, `PUBLISHED` | S |
| `sort` | `code`, `title`, `level`, `status`, `submissionCount`, `acRate`, `updatedAt` | S |
| `dir` | `asc`, `desc` | S |
| `pageSize` | 8, 20, 50 | S |
| `problemIds` | 1 tới 200 id (tối đa 20 cho xuất bản lô; tối đa 50 cho giao bài) | S |

## 9A. Bài giao của lớp học viên (`§5A`)

`classId` tuỳ chọn phải là UUID. Sai dạng, lớp không tồn tại, người gọi không là thành viên hiện tại hoặc đã rời: cùng `404 PRB-015`; không trả `403` phân biệt. Không nhận `studentId`, không cắt còn ba lớp, không loại bài `withdrawn`; chỉ loại dòng giao bài có `removed_at IS NOT NULL`. `solveState` phải lấy từ `SolveStatePort` cho người gọi; cổng lỗi trả `503` chứ không thay bằng `UNSOLVED` (`DEC-2026-1024-dd-student-class-contracts`).

## 10. Thao tác lô (`§29` tới `§33`)

Bảng 10.1.

| Quy tắc | Mã lỗi | Mức | Nguồn |
| :--- | :--- | :--- | :--- |
| `problemIds` không rỗng | PRB-001 | cấp yêu cầu | Sheet 9 NO 5 (`SHR0201:796`) |
| `levelId`/`topicId` đích tồn tại | PRB-039 | cấp yêu cầu, **từ chối cả lô** | Sheet 9 NO 6, 19 (`SHR0201:797`, `:810`) |
| quyền `PROBLEM_AUTHORING:UPDATE`/`DELETE`/`CREATE` | PRB-002 | cấp yêu cầu | Sheet 9 NO 2 |
| A2 chỉ bài của mình | PRB-003 (bài `PUBLISHED` người khác), PRB-010 (bản nháp người khác) | từng bài, vào `failedItems` | Sheet 9 NO 3 |
| checklist xuất bản | PRB-030 | từng bài | Sheet 9 NO 7 (`SHR0201:798`) |
| ghi nhật ký cùng giao dịch | thất bại thì bài đó không đổi | từng bài | Sheet 9 NO 9 (`SHR0201:800`) |

## 11. Checklist xuất bản (`UpdateProblemPublishStatus`, §40)

Bảng 11.1: Năm điều kiện, kiểm ở máy chủ, giao diện chỉ hiển thị lại (`SHR0202:618`). Ngưỡng đặt trong `application.yml`, đã được chủ dự án xác nhận ở `02-bd/database/F2_problem-bank.md:289-293`.

| Mã điều kiện | Quy tắc | Dữ liệu đếm |
| :--- | :--- | :--- |
| `MIN_APPROVED_TESTCASES` | `>= 8` | dòng `testcases` có `approved = true` (cả `SAMPLE` và `HIDDEN`) |
| `MIN_APPROVED_SAMPLES` | `>= 2` | `visibility = SAMPLE AND approved = true` |
| `MIN_WORKED_EXAMPLES` | `>= 2` | dòng `worked_examples` |
| `REFERENCE_SOLUTION_PASSES_ALL` | đáp án mẫu `PASS` mọi testcase đã duyệt trong **một lần chạy lúc bấm xuất bản** | không đọc cờ lưu sẵn |
| `SPEC_DECLARED` | `stdinFormatMd` và `stdoutFormatMd` không rỗng; chiến lược so khớp hợp lệ (có `epsilonValue` nếu `EPSILON`); **chữ ký đã khai** (`functionName`, `returnType`, `parameters` khác `null`), trừ khi chữ ký có kiểu ngoài lược đồ | theo `problem_specs` |

Điều kiện "tổng trọng số 100" **không còn áp dụng** (`01-rd/req/F2_problem-bank.md:162-163`). Hành vi: thiếu điều kiện thì `422 PRB-030` liệt kê **tất cả** điều kiện thiếu (`SHR0202` Q8, dòng 793).

## 12. Giao bài theo lớp (§50 tới §55)

Bảng 12.1.

| Trường / điều kiện | Quy tắc | Mã lỗi | Tầng | Nguồn |
| :--- | :--- | :--- | :-: | :--- |
| `classId` | là lớp người gọi phụ trách (hỏi `identity`) | PRB-005 | S | Sheet 9 NO 2 (`INS0202:515`) |
| `q` | cắt còn 100 ký tự | | C, S | Sheet 9 NO 3 (`INS0202:516`) |
| `problemIds` (giao) | 1 tới 50 id; mỗi bài `PUBLISHED AND NOT deleted` | PRB-001, PRB-037 | S | `INS0202:520` |
| giao trùng | bỏ qua (`skippedIds`), không tạo dòng thứ hai | | S, DB (partial unique) | `database/F2_problem-bank.md:195-196` |
| gỡ bài | `removed_at IS NULL` ngay trong lệnh cập nhật; dòng đã gỡ thì `409 PRB-025` | PRB-025 | S | Sheet 9 NO 6 (`INS0202:519`) |
| gỡ bài | không xoá lượt nộp, điểm, tiến độ | | S | `DEC-2026-0831-class-assignments-round2` |

## 13. Cấu hình trần sinh testcase (§56, §57)

| Trường | Quy tắc | Mã lỗi | Tầng | Nguồn |
| :--- | :--- | :--- | :-: | :--- |
| `maxPerProblem` | số nguyên từ 1 đến 10 | PRB-001 | C, S, DB (`CHECK`) | `ADM0303:434`; `database/F2_problem-bank.md:227` |
| quyền | đọc `AI_CONFIG:READ`, ghi `AI_CONFIG:UPDATE` | PRB-002 | S | `ADM0303:176` |

## 14. Bảng tổng hợp các trần kỹ thuật

Bảng 14.1: Mọi con số do DD đề xuất (không có nguồn RD/BD), gom một chỗ để chủ dự án rà soát. Đặt ở `application.yml`, khoá tiền tố `problem-bank.limits.`.

| Khoá | Giá trị | Dùng ở |
| :--- | :--- | :--- |
| `statement-max-chars` | 100.000 | `statementMd` |
| `constraints-max-chars` | 20.000 | `constraintsMd` |
| `format-max-chars` | 20.000 | `stdinFormatMd`, `stdoutFormatMd` |
| `ai-brief-max-chars` | 4.000 | `briefText` |
| `tag-max-chars`, `tags-max-per-problem` | 50, 20 | thẻ |
| `reference-source-max-bytes` | 65.536 | mã đáp án mẫu và mã ghi đè khi chạy |
| `example-field-max-bytes`, `example-explanation-max-chars` | 8.192, 4.000 | ví dụ mẫu |
| `testcase-direct-max-bytes` | 1.048.576 | nhập trực tiếp một vế |
| `testcases-max-per-problem` | 500 | số testcase mỗi bài |
| `bulk-max-items`, `bulk-max-side-bytes`, `bulk-max-total-bytes` | 100, 64 MB, 256 MB | tải lên theo lô |
| `bulk-publish-max`, `bulk-action-max`, `assign-max` | 20, 200, 50 | số bài mỗi lần thao tác lô |
| `inline-threshold-bytes` | 8.192 | ranh giới `INLINE`/`MINIO` (có nguồn BD) |
| `presign-put-minutes`, `presign-get-minutes`, `upload-session-minutes` | 15, 5, 20 | presigned và phiên tải lên (15 và 5 có nguồn BD) |
| `generation-lock-minutes`, `generation-timeout-seconds` | 10, 300 | sinh testcase AI |
| `reference-run.max-per-user`, `reference-run.window-seconds` | 10, 60 | Redis giới hạn chạy đáp án mẫu §48 theo tài khoản |
| `attention.low-ac-percent`, `attention.stale-draft-days` | 30, 7 | "Bài cần chú ý" (có nguồn RD) |
| `publish.min-testcases`, `.min-samples`, `.min-examples` | 8, 2, 2 | checklist (có nguồn RD, BD) |
| `list.cache-ttl-seconds`, `solvestate.cache-ttl-seconds` | 60, 60 | cache Redis |

## 14A. Tiến độ theo mức độ khó của người học

`GET /me/levels/progress` chỉ nhận JWT STUDENT; không có `userId` hay bộ lọc từ client. F2 chỉ tính bài `PUBLISHED AND deleted = false`, ghép `SOLVED` từ `SolveStatePort` cho người gọi; lỗi cổng trả `503`, không trả tử số 0. Response có mọi mức đang tồn tại, kể cả mức chưa có bài; `0 <= solved <= total` ở từng mức và toàn bộ. Tử số và mẫu số không bao gồm bài đã rút. Nguồn: `DEC-2026-1024-dd-student-dashboard-and-interview-navigation`, API F2 §14A.

## 14B. Vector điểm chủ đề ở bài gợi ý

F2 §13 nhận `difficulty` là mã mức đang tồn tại hoặc bỏ lọc nếu không khớp, `topic` phải là UUID hợp lệ; danh tính lấy từ JWT. `topicScores[].score` là số nguyên 0–100, chỉ tạo khi chủ đề có `total > 0`, `submissionCount > 0`, `acRate` hợp lệ 0–1 và `solved` hợp lệ 0–`total`; không điền 0 cho trường thiếu. Bộ lọc bài gợi ý không lọc vector điểm. Một lô F4 lỗi hoặc F1 `SolveStatePort` lỗi trả `rankingAvailable=false`, vector rỗng và tối đa bảy bài theo thứ tự dự phòng. Nguồn: `DEC-2026-1024-dd-f2-topic-score-source`, API F2 §13.

## 15. Đối chiếu với màn hình

Bảng 15.1: Điểm cần đồng bộ với `03-dd/screens/` (viết sau prototype).

| Màn | Điều cần khớp |
| :--- | :--- |
| `problem_authoring` (`SHR0202`) | bảng 2.1, 3.1, 4.1, 5.1, 11.1; viền đỏ ô vi phạm theo `details.fields[].field`; cảnh báo kiểu vượt lược đồ theo `warnings[]` của `§38`; checklist hiển thị từ `PublishChecklistDto.items` (không chép ngưỡng) |
| `problem_management` (`SHR0201`) | bảng 7.1, 9.2, 10.1; tên chủ đề/độ khó tối đa 60 ký tự |
| `problem_list` (`USR0101`), `saved_problems` (`USR0103`) | bảng 8.1, 9.1; ô tìm kiếm `maxLength = 100`; ghi chú `maxLength = 1000` |
| `class_assignments` (`INS0202`) | bảng 12.1 |
| `admin_ai_limits` (`ADM0303`) | bảng 13 |

## 16. Đóng hợp đồng trước migration

Chốt 2026-10-29 (`DEC-2026-1029-f2-migration-readiness`): DB NOT NULL/default/CHECK/seed theo BD database F2 §7; tag lower(name) không phân biệt hoa thường; signature đủ cả ba trường hoặc cùng NULL, [] là parameters hợp lệ. Epsilon hợp lệ có cùng miền khi EPSILON hay được giữ ở chiến lược khác. So snapshot lúc tái xuất bản theo BR-08; không có câu hỏi validation F2 chặn migration. Không dùng CHECK để thay validation nhiều dòng, quyền hay giao dịch.
