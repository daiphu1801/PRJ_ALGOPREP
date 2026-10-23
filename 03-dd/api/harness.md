# DD API — Module `harness` (F3)

> Nguồn: `01-rd/req/harness.md:11-42` · `02-bd/architecture/harness.md` (mục 2b, 4, 5, 6, 7) ·
> `02-bd/database/harness.md` (mục 1, 2, 3) · `02-bd/security/harness.md` (mục 1-3) ·
> `02-bd/database/problem-bank.md:117-137` (bảng `problem_specs`) ·
> `02-bd/screens/users/USR0102_problem_detail.md:745,761` (`GetStarterCode`).
> Bounded Context: F3 `harness` · Cập nhật: 2026-10-23 · Trạng thái: bản nháp DD, chờ chủ dự án chốt các câu hỏi mở ở mục 7.
>
> Đọc kèm `03-dd/logic/harness.md` (thuật toán và bảng sinh mã), `03-dd/validation/harness.md` (quy tắc
> kiểm đặc tả), `04-tdd/harness.md` (tiêu chí nghiệm thu). File này chỉ giữ **hợp đồng cổng**: tên cổng, chữ
> ký phương thức, DTO, lỗi. Thuật toán không lặp lại ở đây.

## 1. Phạm vi và hình dạng hợp đồng

`harness` là **thư viện tính toán thuần chạy trong cùng JVM**, không phải dịch vụ có API công khai. Hợp
đồng của nó vì vậy là **hợp đồng cổng** (Maven module `algoprep-harness`, kiểu dùng chung ở
`algoprep-common`), không phải bảng endpoint HTTP như `03-dd/api/identity.md`. Cùng một bảng "Hợp đồng /
Request / Response / Lỗi / Luồng / Test viewpoints" được dùng, chỉ thay "endpoint" bằng "phương thức cổng".

| Nội dung | Kết luận | Nguồn |
| :--- | :--- | :--- |
| Bảng PostgreSQL | **Không có**. Schema `harness` được tạo và để rỗng | `02-bd/database/harness.md:10`, `.nexa/control/decision-registry.md:4373` (`DEC-2026-1022-harness-empty-schema-kept`) |
| Truy cập dữ liệu | Chỉ đọc đặc tả từ `problem-bank` qua **một cổng ra** (mục 4); template là tài nguyên trong jar | `02-bd/database/harness.md:63-73`, `:17` |
| Endpoint HTTP | **Đúng một endpoint chỉ-đọc**: `GET .../starter-code`, vỏ mỏng bọc cổng vào `GetStarterCode`, vì BD màn gọi `GetStarterCode` như một endpoint thuộc `harness` (mục 3.1). Mọi cổng còn lại **không có HTTP** | `02-bd/screens/users/USR0102_problem_detail.md:745,761` |
| Redis | Không tạo khoá riêng. Chỉ có thể đọc `problem:spec:<problemId>` do `problem-bank` sở hữu, và chỉ **phía sau** cổng ra | `02-bd/database/harness.md:49-61` |
| Cổng nào ở đâu | Cổng vào: `application/ports/in`. Cổng ra: `domain/ports/out` (đúng quy ước BD). Kiểu dùng chung và bộ kiểm đặc tả: `algoprep-common` | `02-bd/architecture/harness.md:68-74`, `02-bd/database/harness.md:63-69` |

Ngoài phạm vi file này: sinh mã đọc/in theo từng kiểu (`logic/harness.md` mục 3-4), ánh xạ lỗi biên dịch
(logic mục 6), thuật toán so khớp (logic mục 7), việc **gửi** gói sang engine chấm, hàng đợi, trạng thái nộp bài
(`judge-orchestration`), nơi lưu đặc tả và testcase (`problem-bank`).

### 1.1. Danh mục cổng

Bảng 1: Danh mục cổng của `harness`. Bảng liệt kê năm phương thức cổng vào, một cổng ra và một sự kiện
tiêu thụ; mỗi dòng có mục chi tiết tương ứng bên dưới.

| # | Cổng / phương thức | Hướng | Bên gọi | Mô tả | Mã RD |
| :-: | :--- | :--- | :--- | :--- | :--- |
| P1 | `GetStarterCodeUseCase.getStarterCode` | Vào | Vỏ REST của chính `harness` (màn `problem_detail`) | Sinh mã khung cho cặp (ngôn ngữ, mô hình) | F3-02..F3-05, F3-13 |
| P2 | `PrepareSubmissionUseCase.prepare` | Vào | `judge-orchestration` | Ghép mã người học vào khung, tính bảng ánh xạ dòng, đóng gói gói chấm | F3-02..F3-06, F3-13 |
| P3 | `MapCompileErrorUseCase.map` | Vào | `judge-orchestration` | Dịch lỗi biên dịch về dòng của người học, che lỗi thuộc mã khung | F3-11, F3-12 |
| P4 | `MatchOutputUseCase.match` | Vào | `judge-orchestration` | So khớp output của người học với expected-output theo chiến lược của bài | F3-07..F3-10 |
| P5 | `FunctionSpecEvaluator.evaluate` | Vào (thư viện tĩnh trong `algoprep-common`) | `problem-bank` khi lưu đặc tả | Kiểm chữ ký hàm theo lược đồ kiểu, tự tính `function_wrapper_supported` | F3-01, F3-13 |
| O1 | `ProblemSpecReaderPort.findSpec` | Ra | `harness` | Đọc dòng `problem_specs` hiện hành của một bài | `02-bd/database/harness.md:63-69` |
| E1 | Sự kiện `ProblemSpecChanged` | Tiêu thụ | từ `problem-bank` | Bỏ cache đặc tả trong tiến trình (nếu bật) | `02-bd/architecture/problem-bank.md:67` |

Quy ước chung cho mọi phương thức cổng vào:

- **Thuần và tất định**: cùng đầu vào cho cùng đầu ra, byte-by-byte. Không ghi DB, không gọi mạng ngoài
  `ProblemSpecReaderPort`, không chạy mã người học (`02-bd/security/harness.md:39-42`).
- **Lỗi** là ngoại lệ miền kế thừa `AlgoPrepException` (đã có ở `algoprep-common`), mang `code` dạng
  `HRN-nnn`. Bên gọi tự dịch sang HTTP hoặc trạng thái nộp bài của mình. Riêng endpoint ở mục 3.1 harness tự
  trả mã HTTP. Danh sách mã đầy đủ ở mục 6.
- **Mã người học là dữ liệu**: chỉ xuất hiện trong trường `files[].content` của gói chấm. Không bao giờ
  nằm trong tham số dòng lệnh, tên tệp, tên biến hay chuỗi shell (`02-bd/security/harness.md:6-26`).
- **Không chứa dữ liệu testcase** trong chương trình sinh ra hay trong bất kỳ thông điệp lỗi nào. Chương
  trình sinh ra không nhúng expected-output và không in lại input (mục 8 của `logic/harness.md`).

### 1.2. Kiểu dùng chung (DTO và enum)

Bảng 2: Enum dùng chung. Hai enum đầu đã có trong BD, hai enum sau là kết quả của DD này.

| Enum | Giá trị | Nguồn |
| :--- | :--- | :--- |
| `Language` | `JAVA`, `CPP`, `PYTHON` (đúng ba, không thêm) | `02-bd/database/judge-orchestration.md:22` |
| `SubmissionModel` | `FUNCTION_WRAPPER`, `STANDARD_IO` | `02-bd/database/judge-orchestration.md:23` |
| `MatchingStrategy` | `EXACT`, `TRIMMED`, `EPSILON`, `UNORDERED_SET` | `02-bd/database/problem-bank.md:128` |
| `MatchVerdict` | `MATCH`, `NO_MATCH` | `02-bd/architecture/harness.md:226` |
| `CompileReportStatus` | `USER_ERRORS`, `HARNESS_INTERNAL` | `[SoT: Suy luận]` suy từ `02-bd/architecture/harness.md:272-279` (hai nhánh: lỗi của người học, lỗi thuộc mã khung) |

Bảng 3: `TypeNode` — lược đồ kiểu dùng chung giữa `problem_specs.return_type`/`parameters[].type` và
`harness`. Hình dạng JSON giữ nguyên `02-bd/architecture/harness.md:84-91`; ràng buộc chi tiết nằm ở
`validation/harness.md` mục 2.

| Trường | Kiểu | Bắt buộc | Ghi chú |
| :--- | :--- | :--- | :--- |
| `kind` | enum `TypeKind` | Có | `INT` `LONG` `DOUBLE` `BOOLEAN` `CHAR` `STRING` `ARRAY` `LIST` `LINKED_LIST` `BINARY_TREE` |
| `of` | `TypeNode` | Có với `ARRAY` `LIST` `LINKED_LIST` `BINARY_TREE`; **cấm** với kiểu nguyên thuỷ | Kiểu phần tử, hoặc kiểu giá trị mỗi node |
| `dimensions` | int | Có với `ARRAY`; **cấm** với kiểu khác | 1 đến 3 (`02-bd/architecture/harness.md:103`) |

`TypeKind.OTHER` chỉ là giá trị báo "không hỗ trợ" trả về từ `FunctionSpecEvaluator`; **không bao giờ**
xuất hiện trong JSON lưu ở `problem_specs` (`02-bd/architecture/harness.md:103`, `02-bd/database/harness.md:90`).

Bảng 4: `ProblemSpecView` — kết quả của cổng ra O1, cũng là dữ liệu đầu vào mọi phương thức sinh mã. Các cột
lấy từ `02-bd/database/problem-bank.md:128-136`; `functionWrapperSupported` nằm ở bảng `problems`
(`02-bd/architecture/problem-bank.md:132`) nên `problem-bank` phải gộp vào view.

| Trường | Kiểu | Nguồn cột | Ghi chú |
| :--- | :--- | :--- | :--- |
| `problemId` | uuid | `problem_specs.problem_id` | |
| `specVersion` | int | `problem_specs.spec_version` | Tăng khi **bất kỳ** cột nào của dòng đổi |
| `matchingStrategy` | `MatchingStrategy` | `matching_strategy` | |
| `epsilonValue` | decimal, nullable | `epsilon_value` | `> 0` và khác null khi `EPSILON` (CHECK ở DB) |
| `stdinFormatMd`, `stdoutFormatMd` | string | `stdin_format_md`, `stdout_format_md` | Mô tả tự do, `harness` **không** parse; chỉ `problem-bank` và màn hiển thị dùng |
| `functionName` | string, nullable | `function_name` | Null nghĩa là chưa khai chữ ký |
| `returnType` | `TypeNode`, nullable | `return_type` (JSONB) | |
| `parameters` | list của `{name: string, type: TypeNode}`, nullable | `parameters` (JSONB) | Thứ tự có ý nghĩa |
| `nameOverrides` | map `Language` -> string, nullable | `name_overrides` (JSONB) | Chỉ chứa ngôn ngữ có ghi đè |
| `functionWrapperSupported` | boolean | `problems.function_wrapper_supported` | `[SoT: Suy luận]` trường do DD đề xuất để cổng ra không phải đọc hai bảng |

## 2. Cổng vào P2 — `PrepareSubmissionUseCase.prepare`

Phương thức quan trọng nhất: biến (bài, ngôn ngữ, mô hình, mã người học) thành một **gói chấm** cộng với
mọi thứ cần để xử lý kết quả (bảng ánh xạ dòng, thông số so khớp).

### 2.1 Hợp đồng

| Mục | Giá trị |
| :--- | :--- |
| Chữ ký | `PreparedProgram prepare(PrepareSubmissionCommand cmd)` |
| Xác thực / quyền | Không áp dụng (gọi trong tiến trình, bên gọi đã xác thực người dùng) |
| Idempotent | Có, **tất định**: cùng `(problemId, specVersion, language, model, sourceCode)` cho `PreparedProgram` giống hệt từng byte. `judge-orchestration` có thể gọi lại thay vì lưu lại kết quả |
| Giao dịch / I/O | Một lần gọi `ProblemSpecReaderPort.findSpec` (có thể trúng cache). Không ghi gì |
| Tần suất | Một lần cho mỗi lượt nộp hoặc chạy thử, **không** mỗi testcase: gói chấm không chứa stdin, `judge-orchestration` thêm stdin và giới hạn tài nguyên cho từng testcase `[SoT: Suy luận]` |

### 2.2 Request — `PrepareSubmissionCommand`

| Trường | Kiểu | Bắt buộc | Ràng buộc | Ghi chú |
| :--- | :--- | :--- | :--- | :--- |
| `problemId` | uuid | Có | | |
| `language` | `Language` | Có | ba giá trị | `HRN-010` nếu không có plugin |
| `model` | `SubmissionModel` | Có | | `FUNCTION_WRAPPER` bị từ chối với `HRN-004` khi bài có `functionWrapperSupported = false` (BD màn: kiểm cả khi sửa tay tham số URL, `USR0102_problem_detail.md:834`) |
| `sourceCode` | string | Có | không rỗng sau khi bỏ khoảng trắng; tối đa 65 536 byte UTF-8; không chứa ký tự NUL | `HRN-020`, `HRN-021`, `HRN-022`. Ngưỡng 64 KB lấy từ đề xuất `02-bd/security/harness.md:53-57`, chốt cùng `judge-orchestration` (mục 7, Q5) |
| `expectedSpecVersion` | int, nullable | Không | | Khi khác null và khác `specVersion` hiện hành: `HRN-002`. `judge-orchestration` ghim `spec_version` trên lượt nộp (`02-bd/database/problem-bank.md:136`) `[SoT: Suy luận]` cách dùng |
| `correlationId` | string, nullable | Không | | Chỉ để gắn log nội bộ, không đi vào gói chấm |

### 2.3 Response — `PreparedProgram`

DTO bất biến, tuần tự hoá được thành JSON (để `judge-orchestration` đưa qua hàng đợi nếu cần).

| Trường | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `language`, `model` | enum | Lặp lại đầu vào |
| `specVersion` | int | Phiên bản đặc tả thực dùng để sinh |
| `payload` | `JudgePayload` | Gói theo định dạng adapter, mục 2.3.1 |
| `lineMap` | `LineMap` | Bảng ánh xạ dòng, mục 2.3.2. Bên gọi phải giữ cùng `PreparedProgram` để gọi P3 |
| `matchSpec` | `MatchSpec` | Mục 2.3.3. Bên gọi truyền lại nguyên vẹn cho P4 |
| `resultSentinel` | string, nullable | Dòng đánh dấu bắt đầu kết quả trong stdout. Có giá trị khi `model = FUNCTION_WRAPPER`, null khi `STANDARD_IO` (`logic/harness.md` mục 5.4) |
| `harnessInputErrorExitCode` | int | Mã thoát dành riêng khi chương trình sinh ra không đọc nổi input của testcase: 90. Mã này nghĩa là lỗi dữ liệu testcase hoặc lỗi harness, **không phải** lỗi chạy của người học (`logic/harness.md` mục 3.5) `[SoT: Suy luận]` |

#### 2.3.1. `JudgePayload`

Hình dạng chính xác theo engine chốt cùng `judge-orchestration` (mục 7, Q4), vì BD của F4 ghi hình dạng
`RunRequest` "chốt cùng `harness` khi cả hai vào DD" (`02-bd/architecture/judge-orchestration.md:317-318`).
DD này chốt phần trung lập:

| Trường | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `engineId` | string | `GO_JUDGE` (mặc định, `DEC-2026-0823-go-judge-default-engine`) hoặc `JUDGE0` |
| `body` | JSON | Do `JudgePayloadAssembler` của engine tương ứng dựng từ `ExecutionPlan` trung lập (`logic/harness.md` mục 5.3). **Không chứa stdin và giới hạn**: `judge-orchestration` ghép khi dựng `RunRequest` |

`ExecutionPlan` trung lập (nội bộ `harness`, phơi ra để test): `files[]` gồm `name` và `content` (mã nguồn
đầy đủ, là **dữ liệu**), `compile` gồm `argv[]` và `env[]` hoặc null, `run` gồm `argv[]` và `env[]`.
Các `argv[]` là **hằng số** theo bảng chuỗi công cụ ở `logic/harness.md` mục 5.2, không chứa dữ liệu người
dùng. Chi tiết định dạng (`cmd[]`, `copyIn`, Base64) chỉ xuất hiện trong lớp `infrastructure`, đúng
`02-bd/security/harness.md:44-52`.

#### 2.3.2. `LineMap`

| Trường | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `prefixLineCount` | int | Số dòng mã khung nằm trước vùng mã người học. `0` với `STANDARD_IO` |
| `userLineCount` | int | Số dòng của mã người học sau chuẩn hoá xuống dòng (>= 1) |
| `callSiteStartLine`, `callSiteEndLine` | int, nullable | Dòng tuyệt đối (trong tệp đã ghép) của câu lệnh gọi hàm người học; null với `STANDARD_IO` |
| `sourceFileName` | string | Tên tệp mã nguồn trong gói (`Solution.java`, `Main.java`, `main.cpp`, `main.py`) |

Vùng mã người học là các dòng tuyệt đối `prefixLineCount + 1` đến `prefixLineCount + userLineCount`.

#### 2.3.3. `MatchSpec`

| Trường | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `matchingStrategy` | `MatchingStrategy` | Chụp từ cùng lần đọc đặc tả với mã khung, để không lệch phiên bản |
| `epsilonValue` | decimal, nullable | |
| `returnType` | `TypeNode`, nullable | Có khi `FUNCTION_WRAPPER`; null khi `STANDARD_IO` (so khớp theo token, không biết kiểu) |

### 2.4 Lỗi

| Điều kiện | `error.code` | Ghi chú cho bên gọi |
| :--- | :--- | :--- |
| Bài không có dòng `problem_specs` | `HRN-001` | Lỗi dữ liệu bài, báo A2 |
| `expectedSpecVersion` khác phiên bản hiện hành | `HRN-002` | Đặc tả đổi giữa lúc nộp và lúc chấm; bên gọi quyết định chấm lại hay huỷ (mục 7, Q3) |
| Cổng ra lỗi hoặc quá thời gian | `HRN-003` | Lỗi hạ tầng tạm thời, bên gọi có thể thử lại |
| `FUNCTION_WRAPPER` trên bài `functionWrapperSupported = false` | `HRN-004` | Thông điệp học viên theo BD màn: "Bài toán này chỉ hỗ trợ mô hình Standard I/O." |
| `FUNCTION_WRAPPER` nhưng chưa khai chữ ký (`functionName` hoặc `returnType` null) | `HRN-005` | Lỗi dữ liệu bài |
| Đặc tả lưu sẵn không qua được kiểm lại | `HRN-006` | Phòng thủ theo chiều sâu (`02-bd/architecture/harness.md:210-212`); ghi log `ERROR` |
| Ngôn ngữ không có plugin | `HRN-010` | |
| Mã nguồn rỗng / quá lớn / có NUL | `HRN-020` / `HRN-021` / `HRN-022` | |
| Template hỏng, sinh mã thất bại bất ngờ | `HRN-030` / `HRN-031` | Lỗi hệ thống, ghi log `ERROR` |

### 2.5 Luồng xử lý

1. Kiểm `sourceCode` (HRN-020..022). Kiểm `language` có plugin (HRN-010).
2. `spec = ProblemSpecReaderPort.findSpec(problemId)`; không có thì HRN-001; so `expectedSpecVersion` (HRN-002).
3. `FUNCTION_WRAPPER`: kiểm `functionWrapperSupported` (HRN-004), kiểm có chữ ký (HRN-005), chạy lại
   `FunctionSpecEvaluator` (HRN-006 nếu có vi phạm cứng hoặc kiểu ngoài lược đồ).
4. Chuẩn hoá mã người học, gọi `plugin.renderFunctionWrapper(spec, source)` hoặc
   `plugin.renderStandardIo(source)` (thuật toán: `logic/harness.md` mục 4).
5. Dựng `ExecutionPlan` rồi `JudgePayloadAssembler` dựng `JudgePayload` (mục 5 của logic).
6. Trả `PreparedProgram` (`lineMap`, `matchSpec`, `resultSentinel` tính trong bước 4).

### 2.6 Test viewpoints

| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| P2-T01 | Normal | Ba ngôn ngữ x hai mô hình cho bài mẫu `twoSum`: `PreparedProgram` khớp tệp golden |
| P2-T02 | Normal | Hai lần gọi cùng đầu vào cho kết quả giống hệt từng byte |
| P2-T03 | Abnormal | `FUNCTION_WRAPPER` trên bài `functionWrapperSupported = false`: `HRN-004`, không gọi sinh mã |
| P2-T04 | Abnormal | `expectedSpecVersion` lệch: `HRN-002` |
| P2-T05 | Abnormal | Mã rỗng, mã 65 537 byte, mã chứa NUL: lần lượt `HRN-020`, `HRN-021`, `HRN-022` |
| P2-T06 | Boundary | Mã đúng 65 536 byte được chấp nhận |
| P2-T07 | Boundary | Mã người học chứa nguyên văn chuỗi neo (`@@APH_USER_CODE@@`), chứa `{{FUNCTION_NAME}}`, chứa `}` thừa: vị trí chèn và `lineMap` không đổi so với mã vô hại cùng số dòng |
| P2-T08 | Boundary | Mã người học dùng `\r\n`, không kết thúc bằng xuống dòng, có BOM: `userLineCount` đúng, epilogue bắt đầu ở dòng mới |
| P2-T09 | Abnormal | Cổng ra ném lỗi: `HRN-003`, không có kết quả nửa vời |

## 3. Cổng vào P1 — `GetStarterCodeUseCase.getStarterCode`

### 3.1 Hợp đồng

| Mục | Giá trị |
| :--- | :--- |
| Chữ ký | `StarterCodeResult getStarterCode(GetStarterCodeQuery q)` |
| Idempotent | Có, tất định theo `(problemId, specVersion, language, model)` |
| I/O | Một lần `ProblemSpecReaderPort.findSpec`. Không ghi |
| Vỏ HTTP | `GET /api/v1/harness/problems/{problemId}/starter-code?language={JAVA\|CPP\|PYTHON}&model={FUNCTION_WRAPPER\|STANDARD_IO}` `[SoT: Suy luận]` đường dẫn do DD đề xuất, theo quy ước `/api/v1/<module>/` của `03-dd/api/identity.md` |
| Xác thực | JWT (access token), mọi vai trò đã đăng nhập `[SoT: Suy luận]` (mục 7, Q2) |
| Mã khung có lộ dữ liệu không | Không. Mã khung chỉ suy từ chữ ký hàm, vốn đã hiển thị cho học viên (RD `a1_student.md:50`) |

### 3.2 Request — `GetStarterCodeQuery`

| Trường | Kiểu | Bắt buộc | Ràng buộc | Ghi chú |
| :--- | :--- | :--- | :--- | :--- |
| `problemId` | uuid | Có | | Tham số đường dẫn ở vỏ HTTP |
| `language` | `Language` | Có | | Tham số truy vấn |
| `submissionModel` | `SubmissionModel` | Có | | Tham số truy vấn; tên trường khớp `StarterCodeDto.submissionModel` (`USR0102_problem_detail.md:697`) |

### 3.3 Response (2xx) — `StarterCodeResult` và vỏ HTTP `StarterCodeDto`

| Trường | Kiểu | Có ở vỏ HTTP | Ghi chú |
| :--- | :--- | :-: | :--- |
| `language` | `Language` | Có | |
| `submissionModel` | `SubmissionModel` | Có | |
| `sourceCode` | string | Có | Mã khung đầy đủ cho ô soạn. Hai trường vỏ HTTP và tên `StarterCodeDto` khớp BD màn `USR0102_problem_detail.md:697` |
| `specVersion` | int | Không | Chỉ ở kết quả cổng, để bên gọi trong tiến trình ghim phiên bản |

### 3.4 Lỗi

| HTTP (vỏ) | `error.code` | Khi nào | Ghi chú |
| :--- | :--- | :--- | :--- |
| 404 | `HRN-001` | Không có đặc tả cho bài | |
| 422 | `HRN-004` | `FUNCTION_WRAPPER` trên bài chỉ hỗ trợ Standard I/O | Giao diện đã ẩn nút; đây là kiểm phía máy chủ |
| 422 | `HRN-005` | `FUNCTION_WRAPPER` nhưng chưa khai chữ ký | |
| 400 | `HRN-010` | `language` không thuộc ba giá trị | Thường chặn sớm ở lớp ràng buộc enum |
| 503 | `HRN-003` | Cổng ra lỗi | |
| 500 | `HRN-006`, `HRN-030`, `HRN-031` | Lỗi hệ thống | Thông điệp chung, không lộ chi tiết |

Mã lỗi `error.code` phải được đăng ký vào hợp đồng i18n ở `03-dd/api/api.md` (do bên điều phối thêm; DD này
không sửa file đó).

### 3.5 Luồng xử lý

1. `findSpec`; kiểm HRN-001. `STANDARD_IO` bỏ qua các bước còn lại về chữ ký.
2. `FUNCTION_WRAPPER`: kiểm `functionWrapperSupported` (HRN-004), kiểm có chữ ký (HRN-005), chạy lại
   `FunctionSpecEvaluator` (HRN-006).
3. `plugin.starterCode(spec, model)`; thuật toán ở `logic/harness.md` mục 4.6.

### 3.6 Test viewpoints

| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| P1-T01 | Normal | Ba ngôn ngữ cho bài `twoSum`: mã khung khớp tệp golden, đúng tên hàm theo ngôn ngữ (`two_sum`, `twoSum`) |
| P1-T02 | Normal | `STANDARD_IO` cho cả ba ngôn ngữ: khung tối thiểu, không phụ thuộc chữ ký |
| P1-T03 | Normal | Bài có tham số `LINKED_LIST` hoặc `BINARY_TREE`: khung có chú thích định nghĩa node |
| P1-T04 | Abnormal | Bài `functionWrapperSupported = false`, `FUNCTION_WRAPPER`: `HRN-004` (và `STANDARD_IO` vẫn thành công) |
| P1-T05 | Abnormal | Bài chưa khai chữ ký, `FUNCTION_WRAPPER`: `HRN-005` |
| P1-T06 | Abnormal | `problemId` không tồn tại: `HRN-001` (HTTP 404) |
| P1-T07 | Boundary | `name_overrides` có `JAVA`: chỉ khung Java đổi tên hàm, hai ngôn ngữ kia giữ tên suy ra |

## 4. Cổng ra O1 — `ProblemSpecReaderPort.findSpec`

Khai báo trong `domain/ports/out` của `harness` (outbound port nằm trong ngôn ngữ của bên gọi,
`.nexa/control/dependency-map.md` mục 1). Adapter thật gọi sang `problem-bank` trong cùng JVM bằng lời gọi
Java, không dùng HTTP (`02-bd/database/harness.md:63-69`).

### 4.1 Hợp đồng

| Mục | Giá trị |
| :--- | :--- |
| Chữ ký | `Optional<ProblemSpecView> findSpec(UUID problemId)` |
| Bên cung cấp | `problem-bank` phơi một giao diện đọc (tên đề xuất `ProblemSpecQuery`, `[SoT: Suy luận]`, chốt trong DD `problem-bank`); adapter của `harness` chỉ dịch kiểu |
| Nguồn dữ liệu bên kia | `problem_specs` cộng `problems.function_wrapper_supported`; được phép đi qua Redis `problem:spec:<problemId>` do `problem-bank` sở hữu (`02-bd/database/problem-bank.md:280`) |
| Ghi | Không |
| Cache phía `harness` | Mặc định **không có**. Chỉ thêm bộ trang trí `CachingProblemSpecReader` (cache trong tiến trình, khoá `problemId`, giá trị kèm `specVersion`) khi đo hiệu năng thấy cần, đúng `02-bd/database/harness.md:58-61`. Hợp đồng vô hiệu hoá ở mục 5 áp dụng cho bộ trang trí này |

### 4.2 Response và lỗi

Trả `Optional.empty()` khi bài chưa có dòng `problem_specs` (bên gọi dịch thành `HRN-001`). Mọi ngoại lệ hạ
tầng được bọc thành `HRN-003`. `ProblemSpecView` ở Bảng 4 mục 1.2.

### 4.3 Test viewpoints

| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| O1-T01 | Normal | Adapter dịch đủ các trường, JSONB `return_type` và `parameters` thành `TypeNode` đúng |
| O1-T02 | Abnormal | JSONB sai hình dạng (kind lạ): trả view nhưng `FunctionSpecEvaluator` báo ngoài lược đồ, không ném lỗi parse thô |
| O1-T03 | Abnormal | Bên `problem-bank` ném ngoại lệ: bọc thành `HRN-003` |

## 5. Sự kiện tiêu thụ E1 — `ProblemSpecChanged`

Nguồn: `problem-bank` phát (`02-bd/architecture/problem-bank.md:67`). Payload tối thiểu theo BD: `problem_id`,
`spec_version`, diff chữ ký hàm. Hình dạng JSON và cơ chế phát (outbox hay không) do DD `problem-bank`
chốt; `harness` chỉ cần hai trường đầu.

| Mục | Giá trị |
| :--- | :--- |
| Bộ nghe | `ProblemSpecChangedListener` trong `harness` |
| Trường dùng | `problemId`, `specVersion` |
| Hành vi | Nếu bộ trang trí cache đang bật và có mục cho `problemId` với `specVersion` nhỏ hơn giá trị trong sự kiện (hoặc không so sánh được): xoá mục đó. Không có cache hoặc không có mục: không làm gì |
| Idempotent | Có. Nhận trùng hoặc nhận sự kiện cũ (`specVersion` nhỏ hơn mục đang giữ) không gây hại |
| Không làm | Không lưu bản sao chữ ký hàm; không tái sinh mã (mã sinh theo yêu cầu, không lưu, `02-bd/database/harness.md:18`) |
| Phòng thủ khi mất sự kiện | Mỗi `prepare` có thể mang `expectedSpecVersion`; cache lệch phiên bản thì tải lại một lần rồi so tiếp |

Test viewpoint: E1-T01 sự kiện phiên bản mới xoá mục cũ; E1-T02 sự kiện trùng không lỗi; E1-T03 sự kiện cho
bài chưa có trong cache không lỗi; E1-T04 sự kiện cũ hơn mục đang giữ không xoá mục mới.

## 6. Cổng vào P3, P4, P5

### 6.1. P3 — `MapCompileErrorUseCase.map`

| Mục | Giá trị |
| :--- | :--- |
| Chữ ký | `CompileErrorReport map(MapCompileErrorCommand cmd)` |
| Idempotent | Có, thuần |
| I/O | Không (không đọc đặc tả; mọi thứ cần thiết đã nằm trong `PreparedProgram`) |

Request `MapCompileErrorCommand`:

| Trường | Kiểu | Bắt buộc | Ràng buộc | Ghi chú |
| :--- | :--- | :--- | :--- | :--- |
| `language` | `Language` | Có | | |
| `model` | `SubmissionModel` | Có | | |
| `rawCompilerOutput` | string | Có | cắt còn 65 536 ký tự đầu | stderr (và stdout) của bước biên dịch hoặc kiểm cú pháp |
| `lineMap` | `LineMap` | Có | `userLineCount >= 1` | Lấy từ `PreparedProgram`. Không nhất quán thì `HRN-040` |
| `functionSignatureText` | string, nullable | Không | | Dạng hiển thị chữ ký (ví dụ `int[] twoSum(int[] nums, int target)`) cho thông báo "sai chữ ký hàm"; do bên gọi lấy từ `PreparedProgram` mở rộng hoặc tự suy `[SoT: Suy luận]`, xem mục 7 Q6 |
| `correlationId` | string, nullable | Không | | Gắn log nội bộ |

Response `CompileErrorReport`:

| Trường | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `status` | `CompileReportStatus` | `USER_ERRORS` khi có ít nhất một lỗi quy được về mã người học; `HARNESS_INTERNAL` khi không |
| `diagnostics` | list của `{line: int, column: int?, message: string}` | `line` là **số dòng trong mã người học** (bắt đầu từ 1). Rỗng khi `HARNESS_INTERNAL`. Tối đa 10 phần tử, sắp theo `line` tăng dần |
| `fixedMessageKey` | string, nullable | Khoá thông điệp cố định: `harness.compile.internal`, `harness.compile.signatureMismatch`, `harness.compile.noMain`. Không nội suy theo nội dung lỗi thật (`02-bd/security/harness.md:59-64`) |
| `correlationId` | string, nullable | Trả lại để đối chiếu log nội bộ |

Lỗi: `HRN-040` (`lineMap` không nhất quán), `HRN-031` (lỗi bất ngờ). Quy tắc phân loại, danh sách hành vi
compiler đã biết và nội dung thông điệp cố định ở `logic/harness.md` mục 6. **Văn bản `rawCompilerOutput`
chỉ đi vào log nội bộ mức `ERROR`, không bao giờ đi vào response.**

Test viewpoints: lỗi ở dòng đầu, dòng cuối, và dòng trong khối lồng của mã người học, cho từng ngôn ngữ
(đúng danh sách kiểm của tham chiếu codegen); lỗi ở mã khung (phần trước và phần sau); lỗi ở dòng gọi hàm;
nhiều lỗi lẫn lộn; đầu ra không parse được; `STANDARD_IO` với lỗi sau dòng cuối. Chi tiết ở
`04-tdd/harness.md` AC-14 đến AC-22.

### 6.2. P4 — `MatchOutputUseCase.match`

| Mục | Giá trị |
| :--- | :--- |
| Chữ ký | `MatchResult match(MatchOutputCommand cmd)` |
| Idempotent | Có, thuần, không I/O |
| Tần suất | Một lần cho mỗi testcase (`judge-orchestration` gọi) |

Request `MatchOutputCommand`:

| Trường | Kiểu | Bắt buộc | Ghi chú |
| :--- | :--- | :--- | :--- |
| `model` | `SubmissionModel` | Có | Quyết định có tách `resultSentinel` hay không |
| `matchSpec` | `MatchSpec` | Có | Lấy từ `PreparedProgram` |
| `resultSentinel` | string, nullable | Có khi `FUNCTION_WRAPPER` | |
| `actualStdout` | string | Có | stdout nguyên văn của chương trình người học |
| `expectedOutput` | string | Có | expected-output của testcase (dữ liệu của `problem-bank`) |

Response `MatchResult`:

| Trường | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `verdict` | `MatchVerdict` | `MATCH` hoặc `NO_MATCH`. **Không có diff chi tiết, không có vị trí khác biệt** (`02-bd/architecture/harness.md:226`) |
| `actualOutput` | string | Phần kết quả của người học sau khi tách `resultSentinel` và chuẩn hoá xuống dòng, để `judge-orchestration` hiển thị cho testcase **Sample**. Việc có hiển thị cho Hidden hay không là quyết định của `judge-orchestration`; harness không bao giờ trả `expectedOutput` |
| `userConsole` | string | Phần stdout người học in ra **trước** `resultSentinel` (chỉ khi `FUNCTION_WRAPPER`; rỗng nếu không có) |

Lỗi: `HRN-050` (expected-output không parse được theo kiểu trả về khi `EPSILON` hoặc `UNORDERED_SET`: lỗi dữ
liệu testcase, **không** phải sai đáp án), `HRN-051` (chiến lược không hợp với kiểu hoặc thiếu epsilon, phòng
thủ), `HRN-031`. Việc `actualStdout` không có `resultSentinel` hoặc không parse được **không** là lỗi:
trả `NO_MATCH`. Thuật toán ở `logic/harness.md` mục 7. Test viewpoints: mỗi chiến lược với ca đối kháng của
nó (mục 7 của logic và `04-tdd/harness.md` AC-23 đến AC-30).

### 6.3. P5 — `FunctionSpecEvaluator.evaluate` (thư viện tĩnh, `algoprep-common`)

| Mục | Giá trị |
| :--- | :--- |
| Chữ ký | `SpecEvaluation evaluate(FunctionSpecDraft draft)` |
| Vị trí | `algoprep-common`, gói kiểu, **không** nằm trong `algoprep-harness`, vì BD chọn shared kernel thay vì cổng kiểm sang `harness` (`02-bd/architecture/harness.md:67-78`). Hàm thuần: không I/O, không trạng thái. `[SoT: Suy luận]` việc đặt bộ kiểm (không chỉ enum và record) vào shared kernel; BD ghi "không chứa logic sinh mã" và để hằng giới hạn cùng thông báo lỗi cho DD (`02-bd/architecture/harness.md:103`), kiểm cấu trúc không phải sinh mã. Xem mục 7 Q1 |
| Bên gọi | `problem-bank` ở bước lưu đặc tả (`SaveProblemSpec`); `harness` tự gọi lại trong P1 và P2 (phòng thủ theo chiều sâu) |

Request `FunctionSpecDraft`: `functionName`, `returnType`, `parameters`, `nameOverrides`, `matchingStrategy`,
`epsilonValue` — cùng nghĩa và kiểu với `ProblemSpecView` (Bảng 4), không có `specVersion`.

Response `SpecEvaluation`:

| Trường | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `violations` | list của `{code, path, message}` | **Vi phạm cứng**: `problem-bank` phải **từ chối lưu**. `code` là mã `HRN-Vnn` ở `validation/harness.md`; `path` kiểu JSON-pointer (`/parameters/1/name`) |
| `wrapperSupported` | boolean | `false` khi có kiểu ngoài lược đồ **hoặc** chưa khai chữ ký. **Không** chặn lưu: `problem-bank` đặt `problems.function_wrapper_supported = false` và báo A2 (`02-bd/architecture/harness.md:193-201`) |
| `unsupportedReasons` | list của `{code, path}` | Lý do `wrapperSupported = false`, mã `HRN-Vnn` loại "ngoài lược đồ"; để màn `problem_authoring` hiển thị lý do `[SoT: Suy luận]` |

Quy tắc phân loại cứng hay mềm, danh sách mã và thông báo ở `validation/harness.md` mục 2 và 3. Test
viewpoints: `04-tdd/harness.md` AC-31 đến AC-38.

## 7. Mã lỗi và câu hỏi mở

### 7.1. Bảng mã lỗi `HRN-nnn` (ngoại lệ của cổng)

Bảng 5: Mã lỗi của các cổng. Tiền tố `HRN` theo sơ đồ tiền tố của dự án; mã kiểm đặc tả có dạng `HRN-Vnn` nằm ở
`validation/harness.md`, không lặp ở đây.

| Mã | Tên | Cổng phát | Bản chất |
| :--- | :--- | :--- | :--- |
| `HRN-001` | `SPEC_NOT_FOUND` | P1, P2 | Dữ liệu bài |
| `HRN-002` | `SPEC_VERSION_MISMATCH` | P2 | Đặc tả đổi giữa chừng |
| `HRN-003` | `SPEC_READER_UNAVAILABLE` | P1, P2 | Hạ tầng tạm thời |
| `HRN-004` | `WRAPPER_NOT_SUPPORTED` | P1, P2 | Quy tắc nghiệp vụ F3-13 |
| `HRN-005` | `SPEC_SIGNATURE_MISSING` | P1, P2 | Dữ liệu bài |
| `HRN-006` | `SPEC_INVALID_ON_READ` | P1, P2 | Lỗi dữ liệu cũ hoặc lỗi bên kia |
| `HRN-010` | `LANGUAGE_UNSUPPORTED` | P1, P2, P3 | Đầu vào |
| `HRN-020` | `SOURCE_BLANK` | P2 | Đầu vào |
| `HRN-021` | `SOURCE_TOO_LARGE` | P2 | Đầu vào |
| `HRN-022` | `SOURCE_INVALID_CHARS` | P2 | Đầu vào |
| `HRN-030` | `TEMPLATE_CORRUPT` | P1, P2 | Lỗi hệ thống (đáng ra phát hiện lúc khởi động) |
| `HRN-031` | `CODEGEN_INTERNAL` | P1..P4 | Lỗi hệ thống |
| `HRN-040` | `LINEMAP_INVALID` | P3 | Đầu vào |
| `HRN-050` | `EXPECTED_OUTPUT_UNPARSEABLE` | P4 | Lỗi dữ liệu testcase |
| `HRN-051` | `MATCH_CONFIG_INVALID` | P4 | Lỗi cấu hình đặc tả |

Tổng cộng 15 mã, dải `HRN-001` đến `HRN-051` (có khoảng trống giữa các nhóm để chèn thêm cùng nhóm).

### 7.2. Câu hỏi mở (đẩy lên bên điều phối)

| ID | Câu hỏi | Đề xuất mặc định đã dùng trong DD | Ai quyết |
| :--- | :--- | :--- | :--- |
| Q1 | Bộ kiểm `FunctionSpecEvaluator` đặt trong `algoprep-common` (logic nhỏ, không sinh mã) hay chỉ để enum và record ở đó, còn `problem-bank` tự viết bộ kiểm? | Đặt trong `algoprep-common` để hai module không lệch luật (chống drift) | Chủ dự án (cần bản ghi quyết định vì đổi phạm vi shared kernel) |
| Q2 | Ai được gọi `GetStarterCode`, và có kiểm bài đã xuất bản không? `ProblemSpecView` hiện không mang trạng thái xuất bản | Mọi người dùng đã đăng nhập; không kiểm xuất bản ở `harness` (mã khung không lộ gì ngoài chữ ký hàm). Nếu cần, thêm `published` vào `ProblemSpecView` | Chủ dự án |
| Q3 | Khi `expectedSpecVersion` lệch `specVersion` hiện hành lúc chấm, `judge-orchestration` chấm theo phiên bản mới, chấm lại, hay huỷ? `problem_specs` không lưu lịch sử phiên bản nên không thể sinh lại đúng phiên bản cũ | `harness` chỉ báo `HRN-002`; chính sách thuộc DD `judge-orchestration` | `judge-orchestration` DD, chủ dự án |
| Q4 | Hình dạng JSON chính xác của `JudgePayload.body` cho go-judge (`cmd[]`, `copyIn`, `copyOutCached`) và việc ai thêm stdin và giới hạn | Phần trung lập `ExecutionPlan` đã chốt ở 2.3.1; chi tiết engine chốt cùng DD `judge-orchestration` (BD của F4 đã đòi chốt cùng nhau) | Hai DD cùng chốt |
| Q5 | Ngưỡng kích thước mã nguồn: 65 536 byte | Dùng 64 KB theo đề xuất BD, hằng số trong mã | `judge-orchestration` DD |
| Q6 | `functionSignatureText` cho thông báo "sai chữ ký hàm": `PreparedProgram` mang thêm trường này hay bên gọi tự dựng? | Đề xuất thêm `signatureText` vào `PreparedProgram` để bên gọi không phải biết quy tắc đặt tên | Điều phối |
| Q7 | Đường dẫn và vai trò của endpoint `GetStarterCode` (mục 3.1); đồng thời có thêm một cổng "xem trước dòng khai báo theo ngôn ngữ" cho tab "Đặc tả" của `problem_authoring` (`DEC-2026-1003-single-function-signature`) hay màn tự suy bằng TypeScript? | Chưa thêm cổng xem trước (không có trong BD); quy tắc đặt tên nằm ở `logic/harness.md` mục 2 để màn dùng cùng quy tắc | Chủ dự án |
