# DD — Logic module `harness` (F3)

> Nguồn: `01-rd/req/harness.md:11-42` (F3-01..F3-13) · `02-bd/architecture/harness.md` · `02-bd/database/harness.md` ·
> `02-bd/security/harness.md` · `.claude/skills/dd-generation/references/harness-codegen.md` (năm điểm DD phải chốt)
> · `01-rd/req/nfr.md:72` (test golden file bắt buộc). README mô tả F3 là trọng tâm kỹ thuật ở `README.md:94-96`.
> Bounded Context: F3 `harness` · Cập nhật: 2026-10-23 · Trạng thái: bản nháp DD.
>
> Hợp đồng cổng (tên, DTO, lỗi): `03-dd/api/harness.md`. Quy tắc kiểm đặc tả: `03-dd/validation/harness.md`.
> Tiêu chí nghiệm thu: `04-tdd/harness.md`. File này là chỗ chốt **thuật toán**: BD đã chuyển sang DD các
> việc ở `02-bd/architecture/harness.md:289-305`, mỗi việc có chỗ trả lời bên dưới (bảng ở mục 11).
>
> Quy ước nguồn: `[SoT: <path>:<dòng>]` là trích dẫn kiểm chứng được. `[SoT: Suy luận]` là quyết định của DD này,
> có chuỗi lập luận đi kèm; mọi mục như vậy được liệt kê lại ở mục 12 để chủ dự án duyệt.

## 1. Cấu trúc module và plugin ngôn ngữ

### 1.1. Vị trí mã

| Thành phần | Maven module / gói | Ghi chú |
| :--- | :--- | :--- |
| Lược đồ kiểu `TypeKind`, `TypeNode`, hằng giới hạn, `FunctionSpecEvaluator`, `NameConventions` | `algoprep-common` | Shared kernel thuần dữ liệu và hàm thuần (`02-bd/architecture/harness.md:67-78`) |
| Cổng vào (`GetStarterCodeUseCase`, `PrepareSubmissionUseCase`, `MapCompileErrorUseCase`, `MatchOutputUseCase`) | `algoprep-harness`, `application/ports/in` | Bộ khung gói đã có sẵn trong mã (bằng chứng, không phải nguồn) |
| Cổng ra `ProblemSpecReaderPort` | `algoprep-harness`, `domain/ports/out` | `02-bd/database/harness.md:63-69` |
| Plugin ngôn ngữ, registry, bộ so khớp, bộ ánh xạ lỗi | `algoprep-harness`, `domain` và `application` | Thuần Java, không phụ thuộc Spring trong `domain` |
| Template, bộ nạp tài nguyên, `JudgePayloadAssembler` | `algoprep-harness`, `infrastructure` | Template ở `src/main/resources/harness-templates/` (mục 4.1) |
| Vỏ REST `GetStarterCode`, bộ nghe sự kiện | `algoprep-harness`, `presentation` và `infrastructure` | `03-dd/api/harness.md` mục 3 và 5 |

### 1.2. Giao diện plugin và registry (chốt tên, BD để lại cho DD)

BD chốt chiến lược Strategy/Plugin, một interface cho cả hai mô hình (`02-bd/architecture/harness.md:43-48`),
tên đề xuất `JavaHarnessPlugin`, `CppHarnessPlugin`, `PythonHarnessPlugin` (`:216-218`). DD chốt:

```
interface LanguageHarnessPlugin {
    Language language();
    RenderedProgram renderFunctionWrapper(FunctionSpec spec, String normalizedUserSource);
    RenderedProgram renderStandardIo(String normalizedUserSource);
    String starterCode(FunctionSpec specOrNull, SubmissionModel model);
    ToolchainProfile toolchain(SubmissionModel model);          // mục 5.2
    List<RawDiagnostic> parseCompilerOutput(String raw);        // mục 6.2
}
```

- `RenderedProgram` gồm `sourceFileName`, `text`, và `LineMap` (api mục 2.3.2).
- **Registry**: `Map<Language, LanguageHarnessPlugin>` kiểu `EnumMap`, nạp khi khởi động. Thiếu bất kỳ plugin nào
  trong ba plugin là **lỗi khởi động** (fail fast), không phải lỗi lúc chạy. Ngoài `EnumMap` không dùng
  `switch` theo tên ngôn ngữ ở bất kỳ chỗ nào của `harness`, `judge-orchestration` hay `ai-review`
  (`02-bd/architecture/harness.md:30-32`).
- Ba plugin dùng chung phần **cây kiểu**: `TypeCodeEmitter` mỗi ngôn ngữ duyệt `TypeNode` đệ quy và phát
  mã đọc, mã in theo bảng ở mục 3. Khung chương trình (không đệ quy) là tệp template. Chia như vậy để bảng ở
  mục 3 là nguồn sự thật cho `TypeCodeEmitter`, đúng yêu cầu "render bảng, không để prose" của tham chiếu codegen.

## 2. Quy tắc đặt tên (BR-01)

**Kích hoạt**: mọi lần sinh mã khung hoặc mã bọc hàm. **Tiền điều kiện**: đặc tả đã qua `FunctionSpecEvaluator`.
**Nguồn**: `02-bd/database/problem-bank.md:132-135` (`function_name`, `name_overrides`),
`DEC-2026-1003-single-function-signature` (`.nexa/control/decision-registry.md:2666`).

Các bước, hàm thuần `NameConventions.forLanguage(rawName, language, overrides)`:

1. **Có ghi đè**: nếu `nameOverrides` có khoá `language` thì dùng đúng giá trị đó, không biến đổi gì.
2. **Tách từ** từ `rawName`: tách ở `_`; tách thêm ở ranh giới chữ thường hoặc số sang chữ hoa
   (`twoSum` thành `two`, `Sum`); tách thêm giữa chuỗi chữ hoa và một chữ hoa theo sau bởi chữ thường
   (`HTTPServer` thành `HTTP`, `Server`). Đổi mọi từ sang chữ thường. Chữ số dính vào từ đứng trước.
3. **Ghép theo ngôn ngữ**: Python `snake_case` (từ nối bằng `_`); Java và C++ `camelCase` (từ đầu chữ thường,
   từ sau viết hoa chữ đầu).
4. Quy tắc này áp dụng cho **tên hàm và tên tham số** hiển thị trong mã khung và biến cục bộ của mã bọc hàm.
   Tên tham số ở phần hiển thị testcase mẫu (`02-bd/database/problem-bank.md:134`) vẫn dùng chuỗi `name` gốc
   `[SoT: Suy luận]`: BD nói tên tham số "dùng cho mã khung và cho cách hiển thị testcase mẫu" nhưng không
   nói có đổi theo ngôn ngữ cho phần mã khung; DD chọn đổi theo ngôn ngữ để mã khung đúng quy ước từng ngôn
   ngữ như tên hàm, còn phần hiển thị testcase là chuỗi trung lập.

Ví dụ: `two_sum` cho `two_sum` (Python), `twoSum` (Java, C++). `HTTPServer` cho `http_server` và `httpServer`.
`name_overrides = {"JAVA": "solve"}` cho Java `solve`, Python `two_sum`, C++ `twoSum`.

**Hậu điều kiện**: ba tên có thể khác nhau; mỗi tên là một định danh hợp lệ của ngôn ngữ đó và không đụng từ
khoá hay tiền tố dành riêng (kiểm ở `validation/harness.md` mục 3).

Khoá `nameOverrides` được chuẩn hoá thành chữ hoa khi đọc (`JAVA`, `CPP`, `PYTHON`). BD ghi cả hai cách (mô tả
cột nói khoá chữ hoa, ví dụ trong cùng dòng dùng `"java"`, `02-bd/database/problem-bank.md:135`): xem câu hỏi
mở Q10.

## 3. Lược đồ kiểu và bảng sinh mã

### 3.1. Lược đồ đóng (closed schema) và lý do

Lược đồ là **tập đóng**, không phải generics tự do: số tổ hợp sinh mã nhân với ba ngôn ngữ và hai chiều đọc/in,
nên mỗi tổ hợp thêm vào là chi phí cho cả ba plugin (`02-bd/architecture/harness.md:203-208`, bảng `kind` ở
`:84-91`). Một tổ hợp ngoài tập đóng không bị chặn, mà đưa bài về Standard I/O (F3-13, mục 4.5 của BD,
`01-rd/req/harness.md:30-34`). **Điểm phát hiện là lúc A2 lưu đặc tả**, không phải lúc nộp bài hay lúc sinh mã
(`02-bd/architecture/harness.md:193-201`).

Tập tổ hợp được hỗ trợ (cụ thể hoá `kind` ở BD, chi tiết kiểm ở `validation/harness.md` mục 2):

| `kind` | Phần tử / giá trị node `of` được phép | Giới hạn |
| :--- | :--- | :--- |
| `INT` `LONG` `DOUBLE` `BOOLEAN` `CHAR` `STRING` | không có `of` | |
| `ARRAY` | `INT` `LONG` `DOUBLE` `BOOLEAN` `CHAR` `STRING` | `dimensions` 1 đến 3 |
| `LIST` | kiểu nguyên thuỷ (kể cả `STRING`) hoặc `LIST` | độ sâu lồng 1 đến 3 |
| `LINKED_LIST` | `INT` `LONG` `DOUBLE` `BOOLEAN` | |
| `BINARY_TREE` | `INT` `LONG` `DOUBLE` `BOOLEAN` | |

`[SoT: Suy luận]` cho bảng "of được phép": BD chỉ nói `of` là "kiểu phần tử" (`02-bd/architecture/harness.md:88-91`)
và để DD chốt tổ hợp. Chuỗi lập luận: (1) `STRING` chứa khoảng trắng nên không dùng được trong định dạng
theo token của `LINKED_LIST` hay `BINARY_TREE`; (2) `CHAR` bị loại khỏi `BINARY_TREE` vì ký tự `#` trùng ký hiệu
null (BD chốt `#`, `:295-299`); (3) `LIST<ARRAY>`, `LIST<LINKED_LIST>`... không phải nhu cầu đã nêu ở RD
(`01-rd/req/harness.md:11-12` chỉ nêu "mảng nhiều chiều, danh sách lồng nhau"), nên ra ngoài tập đóng theo quy tắc
ponytail bậc 1 (RD không đòi). Tổ hợp ngoài bảng được tính là "kiểu ngoài lược đồ" (`wrapperSupported = false`).

Quy tắc bổ sung: trong **một** đặc tả mọi nút `LINKED_LIST` phải cùng kiểu `of`, mọi nút `BINARY_TREE` cũng vậy,
vì mã sinh ra chỉ có một lớp `ListNode` và một lớp `TreeNode` mỗi chương trình (mục 4.1). `[SoT: Suy luận]`.

**Null và rỗng theo kiểu** (tham chiếu codegen mục 1 yêu cầu chốt):

| Kiểu | Biểu diễn "không có" | Java | C++ | Python |
| :--- | :--- | :--- | :--- | :--- |
| `LINKED_LIST` rỗng | `n = 0` ở input; in `0` rồi một dòng trống | `null` | `nullptr` | `None` |
| `BINARY_TREE` rỗng | một token `#` | `null` | `nullptr` | `None` |
| `ARRAY` hoặc `LIST` rỗng | kích thước 0 | mảng/danh sách độ dài 0 | `vector` rỗng | `[]` |
| `STRING` rỗng | dòng trống | `""` | `""` | `""` |

Kiểu nguyên thuỷ và `STRING` **không bao giờ null**. Hàm của người học trả `null` cho `String`, mảng hay
danh sách (Java) gây lỗi chạy; harness không chuyển `null` thành rỗng (đó là lỗi của người học, không phải của
khung).

### 3.2. Bảng ánh xạ kiểu giữa ba ngôn ngữ

Bảng 1: Ánh xạ kiểu chuẩn sang kiểu ngôn ngữ. Cột "Java" dùng kiểu bao (`Integer`...) khi phần tử nằm trong
`List`, kiểu nguyên thuỷ ở những chỗ còn lại. `ARRAY` và `LIST` khác nhau thực sự chỉ ở Java (mảng so với
danh sách), đúng mô tả ở `02-bd/architecture/harness.md:93-98`.

| Kiểu chuẩn | Java | C++ | Python (gợi ý kiểu) | Ghi chú |
| :--- | :--- | :--- | :--- | :--- |
| `INT` | `int` / `Integer` | `int` | `int` | 32 bit có dấu |
| `LONG` | `long` / `Long` | `long long` | `int` | 64 bit có dấu; Python kiểm miền khi đọc |
| `DOUBLE` | `double` / `Double` | `double` | `float` | |
| `BOOLEAN` | `boolean` / `Boolean` | `bool` | `bool` | |
| `CHAR` | `char` / `Character` | `char` | `str` độ dài 1 | Python tự kiểm độ dài khi đọc (`02-bd/architecture/harness.md:117-119`) |
| `STRING` | `String` | `std::string` | `str` | |
| `ARRAY(of, k)` | `T[]`, `T[][]`, `T[][][]` | `vector<T>` lồng `k` lớp | `List[T]` lồng `k` lớp | Chữ nhật đều |
| `LIST(of)` | `List<T>` lồng | `vector<T>` lồng | `List[T]` lồng | Cho phép không đều |
| `LINKED_LIST(of)` | `ListNode` (`val`, `next`) | `ListNode*` | `ListNode` hoặc `None` | Khung định nghĩa lớp; mục 4.1 |
| `BINARY_TREE(of)` | `TreeNode` (`val`, `left`, `right`) | `TreeNode*` | `TreeNode` hoặc `None` | |

Tên trường node theo quy ước phổ biến: `val`, `next`, `left`, `right`. `val` có kiểu `of`.

### 3.3. Định dạng stdin (mã khung đọc input)

Đầu vào là **mô tả giữa `problem-bank` (viết testcase, `stdin_format_md`) và `harness` (đọc)**: hai module
phải dùng cùng định dạng này. Ba điểm đã được chủ dự án xác nhận 2026-09-13 và giữ nguyên
(`02-bd/architecture/harness.md:295-299`): ký hiệu null của cây là `#`; `BOOLEAN` là `true` hoặc `false`;
mảng nhiều chiều mỗi chiều một dòng. Phần còn lại dưới đây là chốt của DD (`[SoT: Suy luận]`, BD ghi "chốt ở DD",
`:127-129`).

Input là **chuỗi các khối**, mỗi tham số một khối, theo đúng thứ tự `parameters`. Chương trình sinh ra đọc
toàn bộ stdin một lần, đổi `\r\n` thành `\n`, tách thành danh sách dòng, rồi đọc tuần tự bằng con trỏ dòng.

Bảng 2: Định dạng khối stdin theo kiểu.

| Kiểu | Khối stdin | Lỗi đọc (mã thoát 90, mục 3.5) |
| :--- | :--- | :--- |
| `INT` `LONG` | 1 dòng, 1 số nguyên thập phân, có thể có khoảng trắng hai đầu | không parse được hoặc ngoài miền |
| `DOUBLE` | 1 dòng, 1 số thực thập phân | không parse được |
| `BOOLEAN` | 1 dòng `true` hoặc `false` (chữ thường, đúng hai giá trị) | khác hai giá trị |
| `CHAR` | 1 dòng, độ dài đúng 1 ký tự | độ dài khác 1 |
| `STRING` | 1 dòng, **nguyên dòng** (giữ khoảng trắng, không tách token); dòng trống là chuỗi rỗng | hết dòng |
| `ARRAY(of, k)` | Dòng tiêu đề `d1 d2 ... dk` (kích thước từng chiều). Rồi dữ liệu theo thứ tự hàng: `k = 1` một dòng gồm `d1` token (dòng trống nếu `d1 = 0`); `k = 2` đúng `d1` dòng, mỗi dòng `d2` token; `k = 3` đúng `d1 x d2` dòng, mỗi dòng `d3` token. Nếu `of = STRING` thì **mỗi phần tử một dòng**, tổng số dòng bằng `d1 x ... x dk` (không cần dòng gộp) | tiêu đề sai, thiếu hoặc thừa token |
| `LIST(of)` | Dòng `n`. Nếu `of` là nguyên thuỷ không phải `STRING`: một dòng `n` token. Nếu `of = STRING`: `n` dòng. Nếu `of = LIST`: `n` khối con, **mỗi khối tự khai kích thước riêng** (đệ quy theo cùng quy tắc) | |
| `LINKED_LIST(of)` | Dòng `n` rồi một dòng `n` token (dòng trống khi `n = 0`); code sinh ra tự nối `next` tuần tự (`02-bd/architecture/harness.md:152-156`) | |
| `BINARY_TREE(of)` | **Một dòng** gồm các token theo thứ tự mức (BFS), `#` đánh dấu không có con, **không liệt kê con của node null** (`:166-171`). Cây rỗng là một dòng `#` | token không parse được; thiếu token cho node đã khai |

Ghi chú: (1) `ARRAY` khai kích thước **tường minh trên một dòng tiêu đề**, đúng nguyên tắc BD "kích thước luôn
khai tường minh, không suy từ ký tự kết thúc dòng" (`:127-129`). Ví dụ BD tả "dòng đầu `n` rồi `n` dòng, mỗi dòng
`m` số" (`:126-127`) không khai `m`; DD thêm `m` vào dòng tiêu đề để khớp nguyên tắc (xem Q8). (2) Mọi kiểu đọc
dữ liệu từ **dòng**, nên dòng trống ở giữa chỉ hợp lệ ở các vị trí nói trên.

Ví dụ `twoSum(int[] nums, int target)` với `nums = [2,7,11,15]`, `target = 9`:

```
4
2 7 11 15
9
```

### 3.4. Định dạng stdout (mã khung in kết quả)

Chương trình in kết quả theo bảng dưới **sau** dòng đánh dấu `resultSentinel` (mục 5.4). Định dạng in
cũng là định dạng chuẩn của `expected_output` mà `problem-bank` lưu (testcase sinh từ chạy Đáp án mẫu qua đúng
khung này, `02-bd/architecture/problem-bank.md:89-95`).

Bảng 3: Định dạng in theo kiểu.

| Kiểu | In ra | Ghi chú |
| :--- | :--- | :--- |
| `INT` `LONG` | số thập phân | |
| `DOUBLE` | số thực **cố định 12 chữ số thập phân** (`%.12f`); số không âm in `0.000000000000` (không in `-0`); giá trị không hữu hạn in `NaN`, `Infinity`, `-Infinity` đồng nhất cả ba ngôn ngữ | 12 chữ số khớp độ chính xác cột `epsilon_value` NUMERIC(18,12) (`02-bd/database/problem-bank.md:129`); chốt số chữ số là việc BD để cho DD (`02-bd/architecture/harness.md:114-116`) `[SoT: Suy luận]` |
| `BOOLEAN` | `true` hoặc `false` | Python in `true` chỉ khi giá trị là `True` đúng kiểu `bool` |
| `CHAR` | đúng ký tự đó | |
| `STRING` | nguyên chuỗi, **không** thêm dấu nháy | |
| `ARRAY(of, k)` | Dữ liệu **không có dòng tiêu đề**: `k = 1` một dòng, phần tử cách nhau một khoảng trắng; `k = 2` mỗi hàng một dòng; `k = 3` mỗi hàng một dòng và giữa hai khối `d1` liên tiếp thêm đúng **một dòng trống**; `of = STRING`: mỗi phần tử một dòng | Theo BD "phân tách phần tử bằng khoảng trắng, các chiều bằng xuống dòng" (`:130-131`). Dòng trống giữa hai khối ở `k = 3` do DD thêm để hai hình dạng khác nhau không in ra cùng một chuỗi |
| `LIST(of)` | Ký pháp ngoặc vuông lồng, **không khoảng trắng**: `[1,2,3]`, `[[1,2],[3],[4,5,6]]`, `[]`. Phần tử `STRING` và `CHAR` đặt trong nháy kép, escape `\"` và `\\` và `\n`; `BOOLEAN` `true`/`false`; `DOUBLE` như trên | Theo BD (`:143-146`). Rỗng in `[]`, không in chuỗi rỗng (`:147-148`) |
| `LINKED_LIST(of)` | Giống input: dòng `n` rồi dòng `n` token. Danh sách rỗng: `0` rồi một dòng trống | Đối xứng đọc/ghi theo BD (`:157-158`). Duyệt tối đa 1 000 000 node để chương trình không lặp vô hạn nếu người học trả danh sách có chu trình (kết quả bị cắt, chấm sai) `[SoT: Suy luận]` |
| `BINARY_TREE(of)` | Một dòng token theo thứ tự mức, `#` cho vị trí không có node (chỉ liệt kê vị trí con của node thật), **cắt các `#` ở đuôi**; cây rỗng in `#` | Quy tắc cắt đuôi chốt ở đây, đúng điều BD để lại (`:172-174`). Hai cây cùng giá trị khác hình dạng cho hai chuỗi khác nhau (`:175-178`). Cùng giới hạn duyệt 1 000 000 node |

Khi kết thúc, mỗi dòng in ra kết thúc bằng một ký tự xuống dòng. Chương trình in toàn bộ phần kết quả trong **một
lần ghi** từ bộ đệm.

**Giới hạn đã biết**: hai mảng chữ nhật có một chiều bằng 0 mà khác ở các chiều còn lại có thể in ra cùng
một chuỗi (vì dòng trống cuối bị cắt khi chuẩn hoá, mục 7.1). Không có bài toán thuật toán thông thường cần
phân biệt hai trường hợp này, nên DD chấp nhận.

### 3.5. Lỗi đọc input là lỗi harness hoặc testcase, không phải lỗi người học

Mọi kiểm tra ở bảng 2 chạy **trước** khi gọi hàm người học. Khi thất bại, chương trình ghi một dòng cố định
`@@APH-HARNESS-INPUT-ERROR@@` ra **stderr** (không kèm bất kỳ phần nào của input, không kèm số dòng, để không rò
rỉ dữ liệu testcase) rồi thoát với **mã 90**. `judge-orchestration` coi mã 90 là lỗi hệ thống hoặc lỗi dữ liệu
testcase, **không** gán `RE` cho học viên. `[SoT: Suy luận]` BD yêu cầu bắt lỗi harness sớm "không phải lỗi của
người học" (`02-bd/architecture/harness.md:117-119`) nhưng không chọn cơ chế báo; mã 90 và dòng stderr cố định là
đề xuất của DD (quyết định đề xuất D2 ở mục 12).

### 3.6. Gọi hàm người học và cấu trúc chương trình sinh ra

Phía sau mã người học, khung (epilogue) làm đúng thứ tự: đọc input theo bảng 2 vào các biến cục bộ
`apH_p0 ... apH_pN`; khối **dòng gọi** (call-site) `apH_r = new Solution().<tên hàm>(apH_p0, ...)` gọi hàm
**theo vị trí tham số** (`02-bd/database/problem-bank.md:134`); in `resultSentinel` rồi in kết quả theo bảng 3.
Mọi định danh do harness tạo ra có **tiền tố dành riêng** `apH` hoặc `ApH`; đặc tả không được dùng tiền tố này
(`validation/harness.md` mục 3), nên không bao giờ đụng tên của người học hay của đặc tả.

Khung minh hoạ cho Java, mô hình `FUNCTION_WRAPPER` (tệp golden mới là nguồn sự thật):

```java
// ---- prefix (2 dòng, khung sinh, KHÔNG thuộc người học) ----
import java.util.*;
import java.io.*;
//@@APH_USER_CODE@@        <- chỉ là neo trong template, bị bỏ khi render
// ---- epilogue (sau mã người học) ----
class ApHRunner {
    public static void main(String[] args) throws Exception {
        ApHInput apH_in = ApHInput.fromStdin();       // đọc hết stdin, tách dòng
        int[] apH_p0 = apH_in.readIntArray1();        // theo bảng 2
        int apH_p1 = apH_in.readInt();
        //@@APH_CALL_BEGIN@@
        int[] apH_r = new Solution().twoSum(apH_p0, apH_p1);
        //@@APH_CALL_END@@
        StringBuilder apH_sb = new StringBuilder("\n@@APH-RESULT@@\n");
        ApHPrint.intArray1(apH_sb, apH_r);            // theo bảng 3
        System.out.print(apH_sb);
    }
}
```

Hai neo `@@APH_CALL_BEGIN@@` và `@@APH_CALL_END@@` đánh dấu dòng gọi hàm để tính `callSiteStartLine` và
`callSiteEndLine` (mục 6.3). Chúng là dòng riêng trong template, bị loại khi nạp template, không có mặt trong mã
sinh ra.

Quy ước cấu trúc theo ngôn ngữ:

| Điểm | Java | C++ | Python |
| :--- | :--- | :--- | :--- |
| Tệp mã nguồn (`FUNCTION_WRAPPER`) | `Solution.java` | `main.cpp` | `main.py` |
| Tệp mã nguồn (`STANDARD_IO`) | `Main.java` | `main.cpp` | `main.py` |
| Lớp / hàm vào của khung | `class ApHRunner` (không `public`) | `int main()` ở cuối tệp | `def apH_main()` rồi `apH_main()` ở cuối tệp |
| Lớp người học phải cung cấp | `Solution` (có hàm tên theo mục 2) | `class Solution` hoặc `struct Solution` | `class Solution` |
| Prefix (tối thiểu) | `import java.util.*;` `import java.io.*;` | `#include <bits/stdc++.h>`, `using namespace std;`, rồi định nghĩa `ListNode` và `TreeNode` nếu đặc tả dùng | `from typing import *`, rồi định nghĩa `ListNode` và `TreeNode` nếu đặc tả dùng |
| Epilogue chứa thêm | định nghĩa `ListNode`, `TreeNode` nếu dùng; lớp tiện ích `ApHInput`, `ApHPrint` | các hàm tiện ích tiền tố `apH` | các hàm tiện ích tiền tố `apH` |
| Đọc input | `BufferedReader` đọc hết stdin, `String[]` dòng | `std::getline` vào `vector<string>`; số bằng `std::stoi`, `std::stoll`, `std::stod` trong `try` | `sys.stdin.read()`, `.replace('\r','').split('\n')` |
| In output | một `StringBuilder`, một `System.out.print` | một `std::string`, một `fwrite` | một `str`, một `sys.stdout.write` |

Giải thích hai quyết định cấu trúc không có trong BD (`[SoT: Suy luận]`):

1. **Java: tệp `Solution.java`, lớp vào `ApHRunner` không `public`.** Nếu khung đặt `public class Main` thì mã
   người học có `class Main` hoặc khai báo trùng gây lỗi trùng định danh báo ở dòng của **khung** (bị ẩn thành lỗi hệ
   thống, mục 6). Đặt tên tệp theo `Solution` và lớp vào khác tên tránh được nhóm lỗi này, và còn cho người học
   viết cả `class Solution` lẫn `public class Solution`. Đổi lại, người học khai báo lại `ListNode` hoặc `TreeNode`
   (ví dụ bỏ chú thích mẫu) sẽ gặp lỗi trùng định danh báo ở dòng của khung vì node được định nghĩa ở epilogue
   (không thể ở prefix: `import` của người học phải đứng trước mọi khai báo kiểu trong Java). Trường hợp này rơi vào
   hành vi compiler đã biết ở mục 6.4.
2. Cả ba cách ghép đều giữ nguyên tắc BD: mã người học nằm trong **một vùng liền mạch có dòng bắt đầu xác định
   lúc sinh** (`02-bd/architecture/harness.md:263-267`).

## 4. Sinh mã và tiêm mã người học

### 4.1. Template, neo và quy tắc không nội suy (BR-02)

**Kích hoạt**: khởi động ứng dụng (nạp template) và mỗi lần sinh mã.

Template là tài nguyên tĩnh trong jar, **không** ở Postgres (`02-bd/database/harness.md:17`, `:34-44`):

```
src/main/resources/harness-templates/
  java/    function-wrapper.tmpl   standard-io.tmpl
  cpp/     function-wrapper.tmpl   standard-io.tmpl
  python/  function-wrapper.tmpl   standard-io.tmpl
```

Mỗi `function-wrapper.tmpl` có **đúng một** dòng neo `//@@APH_USER_CODE@@` (Java, C++) hoặc `#@@APH_USER_CODE@@`
(Python) đứng một mình trên một dòng, và đúng một cặp neo `APH_CALL_BEGIN` / `APH_CALL_END`. Mỗi
`standard-io.tmpl` chỉ có neo mã người học (prefix và epilogue rỗng, mục 4.5).

**Thuật toán nạp (một lần, khi khởi động):**

1. Đọc tệp; kiểm đúng một neo `APH_USER_CODE`, đúng một cặp `CALL`. Sai thì `HRN-030` và **ứng dụng không khởi động**.
2. Cắt tại dòng neo thành hai chuỗi `prefixTemplate` và `epilogueTemplate`; loại các dòng neo; ghi nhận chỉ số dòng
   (trong `epilogueTemplate`) của hai neo `CALL`.
3. Giữ `prefixTemplate` và `epilogueTemplate` như hai hằng riêng biệt trong bộ nhớ.

**Thuật toán sinh mã (BR-03 và BR-04 dùng chung):**

1. Thay các **chỗ giữ chỗ** (`{{FUNCTION_NAME}}`, `{{PARAM_DECLS}}`, `{{CALL_ARGS}}`, `{{READ_BLOCK}}`, `{{PRINT_BLOCK}}`,
   `{{NODE_DEFS}}`...) **chỉ trong `prefixTemplate` và `epilogueTemplate`**, bằng giá trị lấy từ đặc tả đã kiểm
   (định danh khớp `^[A-Za-z][A-Za-z0-9_]{0,63}$`, mục 2, và mã do `TypeCodeEmitter` phát ra). Thay xong **trước khi mã người
   học xuất hiện** trong bất kỳ chuỗi nào.
2. `text = prefixRendered + userSource + epilogueRendered` bằng **nối chuỗi** (`StringBuilder`), không bằng
   `String.replace` trên chuỗi đã chứa mã người học.
3. Tính `LineMap` (mục 4.3).

Ba điều đảm bảo (đúng `02-bd/security/harness.md:12-26`):

- Mã người học chứa nguyên văn neo, `{{...}}`, hay `}` thừa **không** đổi vị trí chèn hay `LineMap`: neo đã bị cắt khỏi
  hằng, và chỗ giữ chỗ chỉ được thay trước khi mã người học vào.
- **Không bao giờ nội suy từ mã người học vào template**: các tham số của bước 1 chỉ nhận chuỗi từ đặc tả (do
  A2/A3 khai, không do học viên) đã qua kiểm định danh. Tên hàm hay tên tham số **không** do học viên truyền
  (`02-bd/security/harness.md:17-21`).
- Hậu quả tối đa của mã người học phá cấu trúc là **lỗi biên dịch**, được ánh xạ theo mục 6, không phải khả năng
  ghi đè logic khung (`:22-32`).

### 4.2. Chuẩn hoá mã người học

Trước khi nối, `normalizedUserSource` được tạo bằng các bước thuần, theo thứ tự:

1. Bỏ BOM đầu tệp (nếu có).
2. Đổi `\r\n` và `\r` đơn lẻ thành `\n`.
3. Nếu chuỗi không kết thúc bằng `\n` thì thêm đúng một `\n` (để epilogue luôn bắt đầu ở dòng mới).
4. `userLineCount` = số ký tự `\n` trong chuỗi sau bước 3.

Không thay thế tab, không đổi thụt lề, không `trim` đầu hoặc cuối (làm vậy làm lệch số dòng).
Kích thước và NUL đã được kiểm trước đó (`HRN-020..022`).

### 4.3. Tính `LineMap` (BR-03, nửa đầu)

- `prefixLineCount` = số `\n` trong `prefixRendered` (prefix luôn kết thúc bằng `\n`). Giá trị này **thay đổi
  theo chữ ký hàm** (C++ và Python thêm định nghĩa node khi cần), nên phải tính lúc sinh, không là hằng
  (`02-bd/architecture/harness.md:265-267`).
- Vùng người học là các dòng tuyệt đối từ `prefixLineCount + 1` đến `prefixLineCount + userLineCount`.
- `callSiteStartLine` = `prefixLineCount + userLineCount + (chỉ số dòng neo CALL_BEGIN trong epilogueTemplate đã
  loại neo) + 1`; `callSiteEndLine` tương tự cho `CALL_END` (nếu hai neo kề nhau, hai giá trị bằng nhau).
  Số dòng thêm vào bên trong `epilogueRendered` trước dòng gọi (do `{{READ_BLOCK}}` phát nhiều dòng) được tính
  **sau khi thay chỗ giữ chỗ**, nên chỉ số dòng neo được tính trên chuỗi đã render, không trên template thô.

**Hậu điều kiện (bất biến, kiểm bằng test)**: `text.lines()[prefixLineCount]` là dòng đầu tiên của mã người học;
`text.lines()[prefixLineCount + userLineCount - 1]` là dòng cuối; dòng ngay sau là dòng đầu của epilogue.

### 4.4. Python: không cần re-indent

BD để việc re-indent cho DD (`02-bd/security/harness.md:33-38`, `:80-82`). Kết luận của DD: **không re-indent**.
Lý do: BD giả định vùng chèn là thân một hàm có mức thụt lề do khung sở hữu; với DD này vùng người học là **toàn bộ nội
dung ô soạn thảo ở cấp module** (bắt đầu từ cột 0, gồm cả `class Solution:` và chữ ký hàm, mục 4.6), epilogue cũng bắt
đầu ở cột 0, nên không có mức thụt lề nào của khung để khớp. Người học làm lệch thụt lề nhận `IndentationError` hoặc
`SyntaxError` ở **dòng của chính họ**; chuỗi nhiều dòng bị bỏ ngỏ (`"""` không đóng) cũng được Python báo tại dòng mở
chuỗi (trong vùng người học). Phần còn lại giữ nguyên: không `eval`/`exec` mã người học ở phía `harness`
(`02-bd/security/harness.md:39-42`).

`[SoT: Suy luận]` Hệ quả: mô tả BD "người học chỉ viết phần bên trong dấu `{` đã mở sẵn" (`:28-32`) không còn đúng
theo nghĩa đen, vì ô soạn thảo hiển thị cả chữ ký hàm (RD `a1_student.md:50`: "mã khung khớp chữ ký hàm"). Kết quả an
toàn không đổi: hàm vào của khung nằm **sau** mã người học và gọi hàm theo tên cố định; mã người học phá cấu trúc chỉ gây
lỗi biên dịch. Đề xuất cập nhật `02-bd/security/harness.md` mục 1 cho khớp (Q9 ở mục 12).

### 4.5. Mô hình Standard I/O — không có khung (BR-04)

**Kích hoạt**: `prepare` hoặc `starterCode` với `model = STANDARD_IO`. **Tiền điều kiện**: bài có dòng
`problem_specs`; `functionWrapperSupported` **không** ảnh hưởng (mô hình này luôn khả dụng,
`01-rd/req/harness.md:30-34`).

Các bước:

1. Chuẩn hoá mã người học (mục 4.2).
2. `prefixRendered` rỗng, `epilogueRendered` rỗng; `text = normalizedUserSource`. Người học tự viết `main`, tự
   khai báo `include` hoặc `import`, tự đọc input theo `stdin_format_md` (BD: "không sinh logic đọc/ghi",
   `02-bd/architecture/harness.md:223`). Tên tệp `Main.java`, `main.cpp`, `main.py`; Java dùng `public class Main`.
3. `LineMap`: `prefixLineCount = 0`, `userLineCount` như mục 4.2, `callSiteStartLine = callSiteEndLine = null`.
   Nguyên lý ánh xạ ở mục 6 áp dụng y hệt (`02-bd/architecture/harness.md:280-282`).
4. `resultSentinel = null`, `matchSpec.returnType = null`.

Mô hình này **không phụ thuộc lược đồ kiểu**: chữ ký hàm không được đọc, `stdin_format_md` và `stdout_format_md` là văn
bản tự do, `harness` không parse (`02-bd/database/problem-bank.md:130-131`). Người học nhận từ Standard I/O cùng bộ so
khớp ở mục 7.

### 4.6. Mã khung (`GetStarterCode`) (BR-05)

**Kích hoạt**: `getStarterCode`. **Tiền điều kiện**: như `prepare` (api mục 3.5). **Nguồn**:
`02-bd/screens/users/USR0102_problem_detail.md:697,745,761` (`StarterCodeDto`, `GetStarterCode` thuộc `harness`).

`FUNCTION_WRAPPER`: mã khung là **cái người học thấy và sửa**, tức vùng người học của mục 4.3; prefix và epilogue
(nhập, định nghĩa node, khung đọc/in) **ẩn**. Mã khung chứa: (a) một khối chú thích định nghĩa `ListNode` hoặc `TreeNode`
nếu đặc tả dùng (để người học biết các trường `val`, `next`, `left`, `right`; chú thích, không phải mã, nên không gây
trùng định danh); (b) lớp `Solution` với **đúng một** hàm, tên theo mục 2, tham số theo `parameters` (kiểu theo bảng 1),
thân gồm một dòng chú thích `Write your solution here` và một giá trị trả về mặc định.

Giá trị trả về mặc định: Java `0` / `0L` / `0.0` / `false` / `' '` cho kiểu nguyên thuỷ, `null` cho kiểu tham chiếu; C++
`return {};` cho mọi kiểu; Python `pass`. Mục đích: mã khung **biên dịch được** ngay. Chú thích bằng tiếng Anh, ASCII
thuần, để không phụ thuộc bộ mã và ngôn ngữ giao diện `[SoT: Suy luận]`.

Ví dụ Python cho `twoSum`:

```python
class Solution:
    def two_sum(self, nums: List[int], target: int) -> List[int]:
        # Write your solution here
        pass
```

`STANDARD_IO`: mã khung là một chương trình tối thiểu, **không** phụ thuộc chữ ký hàm: Java `public class Main` với
`main` trống, C++ `int main()` trống kèm `#include <bits/stdc++.h>`, Python hàm `main()` và đoạn
`if __name__ == "__main__"`. Chỉ mang tính gợi ý, mô tả định dạng đọc nằm ở `stdin_format_md` của đề.

Hậu điều kiện: bất biến `starterCode` đưa vào `prepare` (mô hình tương ứng) **biên dịch được** với cả ba ngôn ngữ
(kiểm bằng test chuỗi công cụ, mục 10.3).

## 5. Đóng gói theo định dạng adapter (BR-06)

### 5.1. `ExecutionPlan` trung lập

Sau khi sinh mã, plugin trả `ExecutionPlan`: `files[]` (`name`, `content` = `text` của mục 4.1), `compile`
(`argv[]`, `env[]`) hoặc null, `run` (`argv[]`, `env[]`). Không có trường nào đặc thù go-judge hay Judge0
(`02-bd/security/harness.md:44-52`).

### 5.2. Bảng chuỗi công cụ (cờ biên dịch, đúng việc BD hệ thống để lại cho DD này)

`01-rd/system/SYS0401_judge_engine.md:51` ghi "danh sách compiler flag/version cụ thể: chốt khi viết DD cho F3".
Giá trị dưới đây là đề xuất của DD `[SoT: Suy luận]`, **phải xác nhận lại bằng test chuỗi công cụ** (mục 10.3) trên
image go-judge thật khi image được build (`SYS0401_judge_engine.md:283-295`).

Bảng 4: Chuỗi công cụ theo ngôn ngữ và mô hình. `argv` là hằng; tên tệp lấy từ mục 3.6.

| Ngôn ngữ | Biên dịch / kiểm cú pháp | Chạy | Biến môi trường |
| :--- | :--- | :--- | :--- |
| Java | `javac -encoding UTF-8 -proc:none -Xmaxerrs 20 -J-Duser.language=en <tệp>` | `java -XX:+UseSerialGC ApHRunner` (wrapper) hoặc `java -XX:+UseSerialGC Main` (Standard I/O) | `LC_ALL=C` |
| C++ | `g++ -std=c++20 -O2 -fno-diagnostics-show-caret -fno-diagnostics-color -fmax-errors=20 -o main main.cpp` | `./main` | `LC_ALL=C` |
| Python | `python3 -m py_compile main.py` | `python3 main.py` | `LC_ALL=C`, `PYTHONIOENCODING=utf-8`, `PYTHONDONTWRITEBYTECODE=1` |

Lý do: `LC_ALL=C` giữ thông điệp compiler bằng tiếng Anh cố định để bộ parse ở mục 6.2 ổn định; hai cờ
`-fno-diagnostics-*` bỏ đoạn trích mã kèm dấu mũ của g++, đoạn trích có thể là dòng mã khung. `javac` luôn in một dòng
mã và dấu mũ sau mỗi lỗi, nên bộ parse tự bỏ chúng cho lỗi ngoài vùng người học (mục 6.2). Python không có bước biên
dịch riêng, nên lỗi cú pháp lấy từ bước kiểm `py_compile` (đúng BD: "`python -c`/AST-parse",
`02-bd/architecture/harness.md:263-264`).

### 5.3. `JudgePayloadAssembler`

BD cho phép đặt việc đóng gói ở một collaborator dùng chung thay vì lặp trong ba plugin (`:228-231`). DD chốt:
interface `JudgePayloadAssembler { JudgePayload assemble(ExecutionPlan plan); }` đặt ở `application`, cài đặt
`GoJudgePayloadAssembler` (mặc định) và `Judge0PayloadAssembler` (thay thế hợp lệ) ở `infrastructure`; chọn bản cài đặt
theo **cùng công tắc engine** với adapter của `JudgeExecutionPort` (`DEC-2026-0823-go-judge-default-engine`). Đóng
gói đúng hình dạng engine yêu cầu: go-judge dùng `cmd[]`, `copyIn` (nội dung tệp), `copyOutCached`; **không dùng Base64**
(Base64 là yêu cầu riêng của Judge0, `01-rd/req/harness.md:16-18`). Bộ lắp ráp đặt `files[].content` vào trường dữ liệu JSON
của engine; **không bao giờ** nối nội dung tệp vào `argv`, tên tệp hay bất kỳ chuỗi shell nào. Gói không chứa stdin và
giới hạn: `judge-orchestration` ghép khi dựng `RunRequest` cho từng testcase (api mục 2.1).

### 5.4. Chuỗi đánh dấu kết quả `resultSentinel`

**Vấn đề**: mã người học ở mô hình bọc hàm có thể in debug ra stdout, làm stdout lẫn với kết quả của khung.
**Giải pháp** `[SoT: Suy luận]`: khung in dòng `@@APH-RESULT@@` ngay trước kết quả (mục 3.6); bộ so khớp tách:

- Tìm dòng **cuối cùng** bằng đúng `resultSentinel`. Phần sau nó là kết quả (`actualOutput`); phần trước là
  `userConsole`. Không tìm thấy dòng đánh dấu (chương trình thoát sớm, `System.exit`, quá thời gian) thì `NO_MATCH`
  (không lỗi).
- Chỉ áp dụng cho `FUNCTION_WRAPPER`. Standard I/O so khớp trên toàn bộ stdout.

Chuỗi này **không phải bí mật**: người học đọc được mã khung của mình, nhưng biết chuỗi không giúp qua testcase
vì bộ so khớp vẫn so phần sau dòng đánh dấu với expected-output; dữ liệu chấm không nằm trong chương trình
(mục 8). Phương án thay thế đã cân nhắc: chuyển hướng stdout của người học sang stderr khi gọi hàm. Loại vì không làm
được đồng nhất cả ba ngôn ngữ (C++ gọi trực tiếp `printf`). Đề xuất quyết định D1 ở mục 12.

## 6. Ánh xạ lỗi biên dịch về dòng của người học (BR-07)

**Kích hoạt**: `judge-orchestration` gọi `map` sau khi bước biên dịch hoặc kiểm cú pháp trả mã thoát khác 0.
**Tiền điều kiện**: có `lineMap` của đúng `PreparedProgram`. **Nguồn**: `02-bd/architecture/harness.md:261-287` (nguyên lý),
`02-bd/security/harness.md:59-70` (che giấu), `01-rd/req/harness.md:19-21` (F3-11, F3-12).

### 6.1. Nguyên lý (giữ nguyên BD)

Số dòng người học = số dòng tuyệt đối trừ `prefixLineCount`. Lỗi nằm ngoài vùng người học là lỗi thuộc mã khung:
**che hoàn toàn**, ghi log nội bộ `ERROR`, trả thông điệp cố định (`02-bd/architecture/harness.md:268-279`). DD bổ sung
vào đây hai điểm mà một phép trừ đơn lẻ không đủ (đúng cảnh báo của tham chiếu codegen mục 5): vùng có **hai phía**
(khung trước và khung sau mã người học), và ba loại cửa sổ thay cho hai.

### 6.2. Bộ parse từng compiler (bước 1 của BD, chốt ở DD)

Mỗi diagnostic gồm `severity`, `line` (có thể null), `column` (có thể null), `message`, `contextLines[]`.
Một **khối chẩn đoán** bắt đầu ở dòng tiêu đề khớp regex và kéo dài tới dòng tiêu đề kế tiếp.

| Compiler | Regex dòng tiêu đề (đã đặt `LC_ALL=C`) | Dòng cần bỏ |
| :--- | :--- | :--- |
| `javac` | `^(?<file>\S+\.java):(?<line>\d+): (?<sev>error\|warning): (?<msg>.*)$` | Hai dòng sau tiêu đề là đoạn mã và dấu mũ: **giữ** nếu `line` nằm trong vùng người học (mã của chính họ), **bỏ** nếu ngoài vùng. Dòng cuối `N error(s)` bỏ |
| `g++` | `^(?<file>[^:\n]+):(?<line>\d+):(?<col>\d+): (?<sev>fatal error\|error\|warning\|note): (?<msg>.*)$` | Dòng `In file included from`, `In function`, `In member function`, `In instantiation of`, `required from` bị bỏ (chứa đường dẫn và vị trí khung); `note:` đi theo khối `error:` ngay trước nó và áp quy tắc ở mục 6.4 |
| Python (`py_compile`) | Dòng `^\s*File "(?<file>[^"]+)", line (?<line>\d+)` rồi dòng kiểu lỗi `^(?<type>\w*Error): (?<msg>.*)$` | Dòng mã nguồn và dấu mũ ở giữa: giữ hoặc bỏ theo vùng như `javac` |

Chỉ `severity` `error` và `fatal error` đi tiếp; `warning` và `note` độc lập bị bỏ khỏi thông báo cho học viên.
`[SoT: Suy luận]` cho định dạng, vì định dạng thật phụ thuộc phiên bản compiler trong image go-judge chưa build;
bộ parse phải được kiểm bằng mẫu đầu ra thật (mục 10.3). Mẫu đầu ra nào không khớp regex nào đều không được "đoán":
nó rơi vào nhóm không có vị trí (mục 6.3 bước 3).

### 6.3. Thuật toán phân loại và dựng báo cáo

Với mỗi diagnostic mức lỗi, `L` là dòng tuyệt đối:

1. **`STANDARD_IO`** (`prefixLineCount = 0`, không có khung): `L` nằm trong `[1, userLineCount]` là lỗi người học; nếu
   `L > userLineCount` (một số compiler báo "hết tệp" ở dòng kế dòng cuối) thì **ép về `userLineCount`** (toàn bộ tệp
   là mã người học nên mọi lỗi đều là của họ).
2. **`FUNCTION_WRAPPER`**:
   - `prefixLineCount < L <= prefixLineCount + userLineCount`: loại `USER`; `userLine = L - prefixLineCount`.
   - `callSiteStartLine <= L <= callSiteEndLine`: loại `CALL_SITE` (hàm người học không có hoặc sai chữ ký, mục 6.4).
   - còn lại (`L <= prefixLineCount`, hoặc ở epilogue ngoài dòng gọi): loại `HARNESS`.
3. **Không có `line`**: nếu `STANDARD_IO` và khớp mẫu liên kết `undefined reference to 'main'` (hoặc `main` bị thiếu): loại
   `NO_MAIN`. Mọi trường hợp khác: loại `HARNESS`.

Dựng `CompileErrorReport`:

| Điều kiện | `status` | Nội dung |
| :--- | :--- | :--- |
| Có ít nhất một `USER` | `USER_ERRORS` | `diagnostics` gồm các lỗi `USER`: `line = userLine`, `column` giữ nguyên (cột không bị lệch vì mã người học bắt đầu ở cột 1), `message` nguyên văn đã làm sạch (mục 6.5); khử trùng lặp `(line, message)`; sắp theo `line`; tối đa 10. Lỗi `HARNESS` và `CALL_SITE` đi kèm bị bỏ khỏi thông báo (thường là lỗi dây chuyền do chính lỗi người học) nhưng vẫn ghi log |
| Không có `USER`, có `CALL_SITE` | `USER_ERRORS` | `diagnostics` rỗng, `fixedMessageKey = harness.compile.signatureMismatch` |
| Không có `USER`, có `NO_MAIN` | `USER_ERRORS` | `fixedMessageKey = harness.compile.noMain` |
| Còn lại (chỉ `HARNESS`, hoặc không nhận ra được chẩn đoán nào) | `HARNESS_INTERNAL` | `diagnostics` rỗng, `fixedMessageKey = harness.compile.internal`; toàn văn `rawCompilerOutput` ghi log `ERROR` kèm `correlationId` |

Thông điệp cố định (tiếng Việt, **không nội suy theo nội dung lỗi thật**, đúng `02-bd/security/harness.md:59-64`):

| Khoá | Nội dung mặc định |
| :--- | :--- |
| `harness.compile.internal` | "Lỗi hệ thống khi biên dịch. Hãy kiểm tra mã của bạn có thiếu hoặc thừa dấu đóng khối không, rồi thử lại; nếu vẫn lỗi hãy báo quản trị viên." |
| `harness.compile.signatureMismatch` | "Không tìm thấy hàm đúng chữ ký trong mã của bạn. Hãy kiểm tra tên hàm, số tham số và kiểu: `<signatureText>`." |
| `harness.compile.noMain` | "Chương trình chưa có hàm `main`." |

`<signatureText>` là chuỗi dựng từ **đặc tả** (kiểu và tên theo ngôn ngữ), không lấy từ đầu ra compiler. Chữ ký này
đã hiển thị cho học viên ở mã khung, nên không lộ thêm gì. `harness.compile.internal` có thêm câu gợi ý "thiếu hoặc thừa
dấu đóng khối" so với câu mẫu của BD (`02-bd/architecture/harness.md:275-276` viết "dạng"): lý do là phần lớn lỗi rơi vào
mã khung ở `FUNCTION_WRAPPER` thực ra do người học phá cấu trúc (thiếu `}`, bỏ ngỏ chuỗi nhiều dòng) làm lỗi báo ở dòng của
epilogue; một câu "hãy thử lại" thuần tuý khiến họ lặp lại vô ích. Câu cố định, không chứa mã khung (đề xuất D4).

### 6.4. Hành vi compiler đã biết mà phép trừ đơn lẻ không xử lý được

Bảng 5: Hành vi cần phòng. Cột "Cách xử lý" là hành vi của thuật toán mục 6.3.

| Ngôn ngữ | Hiện tượng | Vì sao phép trừ không đủ | Cách xử lý |
| :--- | :--- | :--- | :--- |
| Java | Thiếu `}` ở cuối: `reached end of file while parsing` báo ở dòng cuối của **tệp ghép** (epilogue) | Lỗi do người học nhưng dòng nằm ngoài vùng người học | Loại `HARNESS`, câu gợi ý "thiếu hoặc thừa dấu đóng khối" ở `harness.compile.internal` |
| Java | Khai báo lại `ListNode` hoặc `TreeNode` (đã có ở epilogue): `duplicate class` báo ở dòng khai báo **sau** (epilogue) | Dòng nằm ngoài vùng | Như trên (câu gợi ý cố định chỉ nhắc đóng khối; xem Q11 nếu muốn thêm gợi ý này) |
| Java | Hàm đúng tên nhưng sai tham số: `cannot find symbol` hoặc `method ... cannot be applied` ở dòng gọi hàm | Dòng nằm trong epilogue | Loại `CALL_SITE`: `harness.compile.signatureMismatch` |
| C++ | `error: 'class Solution' has no member named 'twoSum'` ở dòng gọi hàm | như trên | Loại `CALL_SITE` |
| C++ | Lỗi thể hiện mẫu (`In instantiation of ... required from here`): một lỗi kèm chuỗi `note` trỏ về nhiều vị trí (có cả mã khung) | Một diagnostic nhiều vị trí; phép trừ trên một dòng không biết dòng nào quyết định | **Dòng của khối `error:` quyết định loại**; mọi `note` và `required from` có vị trí ngoài vùng người học bị bỏ, mọi `note` trong vùng được giữ và đổi số dòng |
| C++ | Lỗi liên kết không có vị trí (`undefined reference to`) | Không có số dòng để trừ | Nhóm không có `line` (mục 6.3 bước 3) |
| Python | Chuỗi nhiều dòng không đóng: `unterminated triple-quoted string literal` | Python có thể báo ở dòng cuối tệp thay vì dòng mở chuỗi, tuỳ phiên bản | Theo mục 6.3; nếu báo ngoài vùng thì rơi vào `HARNESS` với câu gợi ý cố định; phiên bản Python của image phải được kiểm bằng mẫu thật (mục 10.3) |
| Cả ba | Thông điệp chứa đường dẫn tệp tạm hoặc tên tệp | Lộ hạ tầng | Mục 6.5 |

### 6.5. Làm sạch thông điệp

Áp dụng cho từng `message` và dòng ngữ cảnh được giữ, **sau** khi đã phân loại:

- Thay mọi chuỗi giống đường dẫn tuyệt đối (`/...`, `X:\...`) bằng `<path>`; bỏ tên tệp ra khỏi thông điệp (học viên chỉ thấy
  `line` và nội dung lỗi).
- Bỏ dòng thông tin phiên bản compiler (nếu xuất hiện).
- Số dòng xuất hiện **trong nội dung** thông điệp (ví dụ "variable declared at line 12", hoặc `note:` đi kèm) được đổi theo
  cùng phép `L - prefixLineCount` hoặc bị bỏ nếu ngoài vùng.
- `userLine` luôn trong `[1, userLineCount]` sau khi làm sạch; ngược lại báo `HRN-031` (bất biến bị phá, lỗi lập trình).
- Nếu một thông điệp (kể cả thuộc vùng người học) chứa chuỗi `apH`, `ApH` hoặc `@@APH`, tức nhắc tới định danh của khung,
  thì **cả chẩn đoán đó** bị loại khỏi `diagnostics` và tính như loại `HARNESS`. Mục đích: không để tên nội bộ của khung
  đến học viên qua thông điệp compiler (`02-bd/security/harness.md:68-70`).

## 7. So khớp kết quả (BR-08)

**Kích hoạt**: `match` cho từng testcase. **Tiền điều kiện**: `matchSpec` từ `PreparedProgram`. **Nguồn**:
`02-bd/architecture/harness.md:233-259` (chiến lược trên chuỗi đã chuẩn hoá theo định dạng in), `:226` (kết quả chỉ
`MATCH`/`NO_MATCH`), `01-rd/req/harness.md:16-18` (F3-07..F3-10). Bộ so khớp **dùng chung** cho hai mô hình
(`02-bd/architecture/harness.md:46-47`).

### 7.1. Chuẩn hoá chung (mọi chiến lược)

1. `FUNCTION_WRAPPER`: tách `userConsole` và `actualOutput` theo `resultSentinel` (mục 5.4). `STANDARD_IO`:
   `actualOutput` = toàn bộ stdout.
2. Cả `actualOutput` và `expectedOutput`: đổi `\r\n` và `\r` thành `\n`; **bỏ mọi `\n` ở cuối** chuỗi.
`[SoT: Suy luận]` bước 2: BD nói `EXACT` là "bằng nhau tuyệt đối trên chuỗi đã chuẩn hoá" (`:239-242`) nhưng không
nói xuống dòng cuối có tính không; không bỏ thì một chương trình Standard I/O quên `\n` cuối bị chấm sai vì lý do
vô hình với người học. Câu hỏi mở Q12: chủ dự án có muốn `EXACT` nghiêm hơn không.

### 7.2. Từng chiến lược

| Chiến lược | Thuật toán | Ghi chú |
| :--- | :--- | :--- |
| `EXACT` | `normalized(actual) == normalized(expected)` so chuỗi từng ký tự | Hai cấu trúc cùng giá trị khác hình dạng cho hai chuỗi in khác nhau nên bị coi khác nhau, đúng ý BD (`:239-242`) |
| `TRIMMED` | Với mỗi dòng: bỏ khoảng trắng (`' '`, `\t`) hai đầu, gộp mọi dãy khoảng trắng liên tiếp bên trong thành đúng một dấu cách; giữ nguyên dòng trống ở giữa; rồi so như `EXACT`. **Nếu `returnType.kind` là `LIST` hoặc `BINARY_TREE` thì bỏ qua bước gộp và so như `EXACT`** | BD: với hai kiểu này `TRIMMED` cho kết quả tương đương `EXACT` (`:243-246`). Với `LIST<STRING>` việc không gộp còn bảo vệ khoảng trắng bên trong chuỗi |
| `EPSILON` | `FUNCTION_WRAPPER` có `returnType`: parse cả hai chuỗi **theo ngữ pháp in của kiểu** (mục 3.4) thành cây lá, so hình dạng chính xác, so lá `DOUBLE` bằng `|a - e| <= epsilonValue` (**bao hàm biên**, đúng BD `:247-248`), so lá khác kiểu bằng bằng nhau chính xác. `STANDARD_IO` (không biết kiểu): xem 7.3 | Duyệt song song hai cấu trúc là điểm phức tạp nhất (`:249-252`). Giá trị không hữu hạn: `NaN` chỉ khớp `NaN`, `Infinity` chỉ khớp `Infinity` cùng dấu. Không dùng `epsilonValue` khi chiến lược khác `EPSILON` |
| `UNORDERED_SET` | Tách `actual` và `expected` thành **tập các phần tử cấp cao nhất** dưới dạng chuỗi chuẩn (xem dưới) rồi so hai tập (bằng nhau, không xét thứ tự) | Theo BD "so sánh tập hợp, không quan tâm thứ tự" (`:253-259`). Có thể là **tập** (trùng lặp gộp một) hay **đa tập** (đếm số lần): câu hỏi mở Q13 |

Tách phần tử cấp cao nhất cho `UNORDERED_SET`: `ARRAY` một chiều là các token của dòng; `ARRAY` nhiều chiều là các
hàng (dòng); `LIST` là các phần tử cấp ngoài của ngoặc vuông; kiểu nguyên thuỷ là một phần tử duy nhất; `STANDARD_IO`
là các token tách theo khoảng trắng. `DOUBLE` trong tập so sánh bằng chuỗi in (12 chữ số), không bằng epsilon.
`UNORDERED_SET` với `BINARY_TREE` hay `LINKED_LIST` bị chặn lúc lưu đặc tả (`validation/harness.md`); nếu vẫn đến được
`match` (dữ liệu cũ) thì `HRN-051`.

### 7.3. `EPSILON` khi không có kiểu (Standard I/O)

Bộ so khớp không biết cấu trúc nên làm theo từng **token**: tách `actual` và `expected` thành dòng, mỗi dòng thành token
theo khoảng trắng; hai bên phải có **cùng số dòng và cùng số token từng dòng**; mỗi cặp token: nếu **cả hai** là số thực thập
phân hợp lệ (regex `[-+]?\d+(\.\d+)?([eE][-+]?\d+)?`) thì so bằng epsilon, ngược lại so chuỗi chính xác. `[SoT: Suy
luận]`: BD nói ma trận so khớp dùng chung cho cả hai mô hình (`:46-47`) nhưng chỉ mô tả `EPSILON` theo cấu trúc kiểu
(`:247-252`); DD chọn thuật toán token vì Standard I/O không khai kiểu.

### 7.4. Lỗi và độ tin cậy

- `expectedOutput` không parse được theo `returnType` khi `EPSILON` hoặc `UNORDERED_SET`: `HRN-050` (lỗi dữ liệu testcase,
  `judge-orchestration` không được tính thành `WA` cho học viên).
- `actualOutput` không parse được (người học in rác hoặc thiếu kết quả): `NO_MATCH`, **không** phải lỗi.
- `EPSILON` thiếu `epsilonValue` hoặc `<= 0`, hoặc `UNORDERED_SET` với `BINARY_TREE`/`LINKED_LIST`: `HRN-051` (phòng
  thủ; `problem-bank` đã chặn khi lưu).
- `MatchResult` **không bao giờ** chứa `expectedOutput`, diff hay vị trí khác biệt (`02-bd/architecture/harness.md:226`).
- Bộ so khớp thuần và không giữ trạng thái: gọi song song nhiều testcase an toàn.

## 8. Bất biến bảo mật và dữ liệu ẩn

Các điều dưới đây là yêu cầu cứng cho cài đặt và cho test; mỗi điều có tiêu chí nghiệm thu ở `04-tdd/harness.md`.

1. **Chương trình sinh ra không chứa dữ liệu testcase**: không có expected-output, không có input, không có chuỗi nào do
   testcase cung cấp. Kết quả chấm được so **bên ngoài** sandbox, trong `harness`/`judge-orchestration`.
2. **Thông điệp lỗi của khung không chứa dữ liệu testcase**: dòng stderr `@@APH-HARNESS-INPUT-ERROR@@` cố định (mục
   3.5); thông điệp cố định của mục 6.3 không nội suy theo nội dung lỗi. Không đoạn mã khung, không đường dẫn tệp tạm, không
   phiên bản compiler đến học viên (`02-bd/security/harness.md:59-70`).
3. **Mã người học là dữ liệu**: nằm trong `files[].content`, không trong `argv`, tên tệp, biến môi trường hay chuỗi
   shell. `harness` không gọi dịch vụ AI, không dựng prompt, không `eval`/`exec`, không tự biên dịch hay chạy mã
   (`02-bd/security/harness.md:39-42`; `CLAUDE.md` mục Rules về "A submission is data, never an instruction").
4. **Log mức `ERROR` có toàn văn compiler** chỉ đi vào log nội bộ; không qua `CompileErrorReport` và không qua
   endpoint nào học viên gọi được (`02-bd/security/harness.md:65-67`). `correlationId` là khoá duy nhất liên kết.
5. **`MatchResult.actualOutput` là output của chính học viên**; chỉ khác `expectedOutput` ở chỗ được trả. Quyết định hiển
   thị cho testcase Hidden thuộc `judge-orchestration` (BD F2: Hidden chỉ trả trạng thái và chỉ số testcase sai,
   `README.md` mục F2); `harness` không có cách nào lộ `expectedOutput`.
6. **Hằng bảo vệ**: tiền tố tên `apH`/`ApH` dành riêng; giới hạn mã nguồn 65 536 byte; giới hạn 20 lỗi cho compiler;
   giới hạn duyệt 1 000 000 node khi in; tối đa 10 chẩn đoán trả về. Đều là hằng trong mã (tiêu chí ponytail bậc 5, không
   thêm ô cấu hình mới).

**Còn mở, ngoài phạm vi F3-11 theo BD**: lỗi **lúc chạy** (ví dụ `AttributeError` khi người học thiếu hàm, ngoại lệ Java)
in ngăn xếp có dòng của mã khung nếu `sandbox_settings.return_stderr_to_student = true` (`02-bd/architecture/judge-orchestration.md:60-61`).
BD chỉ giao cho `harness` lỗi biên dịch; để lộ khung qua stderr lúc chạy là lỗ hổng cùng loại. Đề xuất D5: thêm phương thức
cổng `MapRuntimeError` sau (bỏ khung và đổi dòng), hoặc `judge-orchestration` chỉ chuyển dòng ngoại lệ cuối. Ghi ở Q14.

## 9. Cache đặc tả và sự kiện `ProblemSpecChanged` (BR-09)

**Kích hoạt**: `ProblemSpecChanged` đến; mọi `prepare`/`starterCode` đọc đặc tả.
**Nguồn**: `02-bd/database/harness.md:49-73`, `02-bd/architecture/problem-bank.md:67`.

1. Mặc định **không có** cache trong tiến trình. Mỗi `prepare` đọc đặc tả một lần qua `ProblemSpecReaderPort`, vốn đã
   đi qua Redis `problem:spec:<problemId>` do `problem-bank` sở hữu; `prepare` chạy một lần cho mỗi lượt nộp, không mỗi
   testcase (api mục 2.1), nên chi phí đọc nhỏ. BD chỉ cho thêm cache khi đo hiệu năng thấy cần
   (`02-bd/database/harness.md:58-61`).
2. Nếu bật: `CachingProblemSpecReader` giữ `problemId` -> `ProblemSpecView` (kèm `specVersion`), thư viện cache trong tiến
   trình, một JVM một bản (không chia sẻ giữa instance, `:58-61`).
3. **Sự kiện**: nhận `ProblemSpecChanged{problemId, specVersion}` -> nếu có mục cho `problemId` mà phiên bản mục nhỏ hơn
   `specVersion` của sự kiện thì xoá mục. Idempotent; sự kiện cũ hoặc trùng không gây hại.
4. **Phòng thủ**: khi `expectedSpecVersion` khác phiên bản của mục cache, tải lại từ cổng ra một lần rồi so tiếp, sau đó mới
   trả `HRN-002` nếu vẫn lệch. Cách này che việc mất hoặc trễ sự kiện.
5. `harness` **không lưu bản sao** chữ ký hàm và **không tái sinh** mã khi nhận sự kiện (mã sinh theo yêu cầu, không lưu,
   `02-bd/database/harness.md:18`).

`spec_version` tăng khi **bất kỳ cột nào** của dòng `problem_specs` đổi (`02-bd/database/problem-bank.md:136`), nên
việc vô hiệu cache đúng cả khi chỉ `matching_strategy` hay `stdin_format_md` đổi.

## 10. Chiến lược test golden file

Bắt buộc theo `01-rd/req/nfr.md:72` ("test so khớp mã sinh ra (golden file) cho cả ba ngôn ngữ"); đây là bộ test rẻ nhất và bắt
lỗi tốt nhất của đề tài.

### 10.1. Cấu trúc

```
algoprep-harness/src/test/resources/golden/
  specs/<case>.json                 # FunctionSpec đầu vào
  java|cpp|python/<case>/
    user.<ext>                      # mã người học mẫu (nội dung không quan trọng, đủ để biên dịch)
    wrapper.<ext>                   # text mong đợi của renderFunctionWrapper
    starter.<ext>                   # mong đợi của starterCode
    linemap.json                    # lineMap mong đợi
```

Mỗi cặp (đặc tả, ngôn ngữ) một thư mục. Test so **từng byte**; cập nhật tệp golden bằng cờ
`-Dgolden.update=true`, và diff của tệp golden phải được review trong Pull Request (đây chính là nơi review template,
`02-bd/database/harness.md:17`).

### 10.2. Bộ ca bắt buộc (Bảng 6)

Bảng 6: Bộ ca golden. Mỗi dòng phải có thư mục cho cả ba ngôn ngữ.

| Ca | Đặc tả | Mục đích |
| :--- | :--- | :--- |
| `scalars` | một tham số mỗi `kind` nguyên thuỷ và `STRING`; trả về từng kiểu | Mọi hàng của bảng 2 và 3 cho kiểu nguyên thuỷ |
| `array1`, `array2`, `array3` | `ARRAY` một, hai, ba chiều | Dòng tiêu đề, chiều chữ nhật, dòng trống giữa khối ở `k = 3` |
| `array-string` | `ARRAY(STRING, 1)` | Mỗi phần tử một dòng |
| `list-nested` | `LIST<LIST<INT>>`, `LIST<STRING>` | Khối tự khai kích thước, nháy kép, escape |
| `linked-list` | tham số và kết quả `LINKED_LIST(INT)` | Dựng nối `next`, in đối xứng, danh sách rỗng |
| `binary-tree` | tham số và kết quả `BINARY_TREE(INT)` | BFS, `#`, cắt đuôi, cây rỗng, cây lệch |
| `two-sum` | `twoSum(int[] nums, int target) -> int[]` | Ca minh hoạ chính, tên hàm theo ba ngôn ngữ |
| `name-override` | `name_overrides = {"JAVA":"solve"}` | Chỉ Java đổi tên |
| `camel-in` | `function_name = "twoSum"` và `"HTTPServer"` | Tách từ theo mục 2 |
| `no-param` | không tham số | Khung gọi không đối số |
| `standard-io` | (không đặc tả) | `prefix` rỗng, `starter` tối thiểu |
| `adversarial-user` | mã người học chứa neo, `{{...}}`, `}` thừa | Vị trí chèn bất biến (mục 4.1) |

### 10.3. Ba tầng test

1. **Tầng thuần (luôn chạy, không cần Docker)**: so golden, kiểm `lineMap` (bất biến mục 4.3), kiểm template nạp được
   (đúng một neo), kiểm tên (mục 2), kiểm `FunctionSpecEvaluator`, kiểm bộ parse compiler bằng **mẫu đầu ra compiler đã chụp
   sẵn** (`src/test/resources/compiler-output/<ngôn ngữ>/*.txt`), kiểm bộ so khớp bằng bảng ca.
2. **Tầng chuỗi công cụ (thẻ `toolchain`, chạy ở CI với container go-judge dựng bằng Testcontainers)**: với mỗi ca golden,
   (a) biên dịch `wrapper` và `starter` bằng đúng lệnh ở bảng 4; (b) **round-trip**: đặc tả hàm `echo(p) -> p` cùng kiểu, mã người
   học trả lại đúng tham số, kiểm `stdout` sau dòng đánh dấu bằng đúng chuỗi in chuẩn của `stdin` (đọc rồi in cho ra
   cùng giá trị, bắt lệch giữa bảng 2 và bảng 3 ở cả ba ngôn ngữ); (c) chụp lại mẫu đầu ra lỗi biên dịch thật bằng mã
   người học cố ý hỏng, cập nhật `compiler-output/` và kiểm bộ parse. Tầng này cũng xác nhận cờ ở bảng 4.
3. **Tầng tích hợp** nằm ngoài DD này: chạy cùng `judge-orchestration` (giai đoạn xây 2 của
   `.nexa/control/dependency-map.md`).

## 11. Đối chiếu việc BD chuyển sang DD

| Việc BD để cho DD | Nơi trả lời |
| :--- | :--- |
| Tên và chữ ký interface plugin, registry, vị trí mã (`02-bd/architecture/harness.md:291-292`) | Mục 1 |
| Cách tổ chức golden-file theo plugin (`:293-294`) | Mục 10 |
| Quy ước ký tự phân tách và định dạng stdin (`:127-129`, `:295-299`) | Mục 3.3 |
| Số chữ số thập phân khi in `DOUBLE` (`:114-116`) | Mục 3.4 |
| Quy tắc cắt đuôi `#` của cây (`:172-174`) | Mục 3.4 |
| Giới hạn số chiều, độ sâu, thông báo lỗi, `OTHER` (`:103`, `02-bd/database/harness.md:90`) | Mục 3.1 và `validation/harness.md` |
| Quy tắc liên trường `UNORDERED_SET` với cây/danh sách liên kết (`:300-301`) | `validation/harness.md` mục 4 |
| Regex và parser lỗi từng compiler (`:302`) | Mục 6.2 |
| Thuật toán duyệt song song cho `EPSILON` (`:249-252`) | Mục 7.2 |
| Re-indent Python (`02-bd/security/harness.md:81-82`) | Mục 4.4 |
| Giới hạn kích thước mã nguồn (`02-bd/security/harness.md:80`) | Mục 8 điểm 6, `validation/harness.md` |
| Danh sách trường bị lọc khỏi thông báo lỗi (`02-bd/security/harness.md:83-84`) | Mục 6.5 |
| Tên cổng ra và DTO (`02-bd/database/harness.md:83-84`) | `03-dd/api/harness.md` mục 4 và Bảng 4 |
| Cache in-process hay không (`02-bd/database/harness.md:85-86`) | Mục 9 (mặc định không) |
| Cấu trúc thư mục template (`02-bd/database/harness.md:87-88`) | Mục 4.1 |
| Cờ báo bài chỉ còn Standard I/O (`02-bd/architecture/harness.md:50-52`) | `validation/harness.md` mục 3 và `api` mục 6.3 (P5) |
| Cờ compiler (`01-rd/system/SYS0401_judge_engine.md:51`) | Mục 5.2 |

## 12. Quyết định đề xuất và câu hỏi mở của file logic

Quyết định đề xuất (bên điều phối ghi vào `.nexa/control/decision-registry.md`; DD này không sửa file đó):

| ID | Nội dung | Loại | Lập luận |
| :--- | :--- | :--- | :--- |
| D1 | Dòng đánh dấu `@@APH-RESULT@@` tách kết quả khỏi stdout debug của người học (mục 5.4) | Kiến trúc nhỏ, ảnh hưởng hợp đồng giữa `harness` và `judge-orchestration` | Không có thì mọi bài bọc hàm có `print` debug đều bị chấm sai; ponytail bậc 6 (mất dữ liệu đúng) |
| D2 | Mã thoát 90 và dòng stderr cố định cho lỗi đọc input (mục 3.5) | Hợp đồng chéo module | F4 cần phân biệt lỗi harness với lỗi chạy của người học |
| D3 | Ba loại vùng lỗi biên dịch (`USER`, `CALL_SITE`, `HARNESS`) thay cho hai của BD (mục 6.3) | Mở rộng nguyên lý BD | Lỗi sai chữ ký hàm là lỗi thường gặp nhất, hai loại sẽ cho họ thông báo "lỗi hệ thống" |
| D4 | Câu thông báo cố định của `harness.compile.internal` có gợi ý đóng khối (mục 6.3) | Nhỏ | BD ghi "dạng", câu chữ chốt ở DD |
| D5 | Bổ sung sau cổng `MapRuntimeError` hoặc quy tắc cho `judge-orchestration` về stderr lúc chạy (mục 8) | Bảo mật | Ngăn xếp lúc chạy lộ mã khung, cùng loại rủi ro F3-12 |
| D6 | Bộ kiểm `FunctionSpecEvaluator` và `NameConventions` đặt trong `algoprep-common` | Đổi phạm vi shared kernel | Tránh lệch luật giữa `problem-bank` và `harness` |
| D7 | Java: tệp `Solution.java`, lớp vào `ApHRunner` (mục 3.6) | Chi tiết codegen | Tránh trùng định danh với lớp `Main` của người học |
| D8 | Định dạng stdin có dòng tiêu đề kích thước cho `ARRAY`; stdout không tiêu đề (mục 3.3, 3.4) | Hợp đồng với `problem-bank` (định dạng testcase) | BD ghi "chốt ở DD", cần `problem-bank` dùng cùng |

Câu hỏi mở (chủ dự án hoặc DD module khác quyết). Đánh số tiếp nối bảng ở `03-dd/api/harness.md` mục 7.2 (Q1 đến Q7):

| ID | Câu hỏi | Mặc định đã dùng |
| :--- | :--- | :--- |
| Q8 | Mảng nhiều chiều khai kích thước trên một dòng tiêu đề (`n m`), hay `n` rồi `m` suy từ độ dài dòng như ví dụ BD (`02-bd/architecture/harness.md:126-127`)? | Dòng tiêu đề, theo nguyên tắc "khai tường minh" của cùng BD |
| Q9 | Cập nhật `02-bd/security/harness.md` mục 1 (vùng chèn là toàn bộ nội dung ô soạn thảo, không phải thân hàm) và `02-bd/architecture/harness.md:222` ("đoạn thân hàm")? | Đề xuất cập nhật; DD đã dùng nghĩa "toàn bộ nội dung" |
| Q10 | `name_overrides`: khoá `JAVA` hay `java`? BD ghi cả hai (`02-bd/database/problem-bank.md:135`) | Chuẩn hoá thành chữ hoa khi đọc; đề nghị BD sửa ví dụ |
| Q11 | Thêm gợi ý "đừng khai báo lại ListNode/TreeNode" vào thông báo cố định? | Chưa, để một câu cố định chung |
| Q12 | `EXACT` có bỏ qua xuống dòng ở cuối không? | Có (mục 7.1) |
| Q13 | `UNORDERED_SET` là tập hay đa tập (đếm số lần)? Với tập, `[1,1,2]` bằng `[1,2]` | Tập, đúng chữ BD ("tập hợp"). Đề xuất đa tập vì chặt hơn; chủ dự án quyết |
| Q14 | Lỗi lúc chạy có thông qua `harness` để loại dòng mã khung không (mục 8)? | Chưa, `harness` chỉ xử lý lỗi biên dịch như BD |
| Q15 | `EPSILON` khi kiểu trả về không có lá `DOUBLE`: chặn lúc lưu hay cho qua? | Cho qua (so như bằng nhau chính xác); chưa có luật chặn trong BD |
| Q16 | Chưa khai chữ ký (`function_name` rỗng): có tự đặt `function_wrapper_supported = false` không? | Có (`validation/harness.md` mục 3, ngoại lệ nhẹ của BD `:193-201`) |
