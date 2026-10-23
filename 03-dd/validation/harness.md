# DD — Validation module `harness` (F3)

> Nguồn: `02-bd/architecture/harness.md` (mục 4.2, 4.5, 6) · `02-bd/database/harness.md:90` ·
> `02-bd/database/problem-bank.md:128-136` · `02-bd/security/harness.md:12-26,44-57` · `01-rd/req/harness.md:30-34`.
> Bounded Context: F3 `harness` · Cập nhật: 2026-10-23 · Trạng thái: bản nháp DD.
>
> File này chỉ ghi quy tắc kiểm mà BD **chưa** tự suy ra được, theo cách đọc của `03-dd/validation/identity.md`.
> Không lặp lại ràng buộc đã có ở BD màn (ví dụ `epsilon_value > 0` đã là CHECK ở DB,
> `02-bd/database/problem-bank.md:129`; ở đây chỉ nói ai kiểm và báo mã nào).

## 1. Cách đọc và phân loại vi phạm

`harness` không có bảng và không có endpoint nhận form, nên "validation" ở đây là hai việc:

1. **Kiểm đặc tả khi lưu** (bước `SaveProblemSpec` của `problem-bank`), do hàm thuần `FunctionSpecEvaluator`
   trong `algoprep-common` thực hiện (`03-dd/api/harness.md` mục 6.3). Kết quả có hai loại:
   - **Cứng**: `problem-bank` **từ chối lưu**, trả `violations[]` (mã `HRN-V10` trở lên).
   - **Mềm** ("ngoài lược đồ"): **cho lưu**, hàm trả `wrapperSupported = false` và `unsupportedReasons[]` (mã
     `HRN-V01` đến `HRN-V07`); `problem-bank` đặt `problems.function_wrapper_supported = false` và báo A2 trong
     màn `problem_authoring`. Đúng quyết định ở `02-bd/architecture/harness.md:193-201`: "không phải lỗi cứng
     chặn A2".
2. **Kiểm đầu vào của cổng** lúc chạy (`harness` kiểm lại đặc tả trong P1 và P2 theo chiều sâu, kiểm mã nguồn người
   học, kiểm `LineMap`...). Vi phạm là ngoại lệ `HRN-nnn` ở `03-dd/api/harness.md` mục 7.1.

Cột "Tầng" dùng: **PB** là `problem-bank` gọi `FunctionSpecEvaluator` lúc lưu; **HR** là `harness` kiểm lại lúc dùng;
**FE** là lược đồ phía trình duyệt của tab "Đặc tả" (`SHR0202_problem_authoring`).

Mọi quy tắc ở các bảng dưới là **một nguồn duy nhất**. Lược đồ zod của tab "Đặc tả" phải khớp với file này; nếu buộc
phải khác (ví dụ FE không kiểm định danh bằng từ khoá) thì ghi lý do ở DD màn. Để giảm lệch, phần cấu trúc của lược đồ
kiểu (mục 2) được xuất thành **JSON Schema** đi kèm `TypeKind` (`.nexa/control/dependency-map.md:49` mô tả lược đồ
kiểu là "mô hình Java cộng JSON Schema"); FE dùng chính JSON Schema đó, quy tắc ngữ nghĩa (tên, trùng lặp, liên trường)
chỉ kiểm ở PB/HR. `[SoT: Suy luận]` hình thức xuất JSON Schema; BD chưa nói rõ cách FE nhận lược đồ.

## 2. Lược đồ kiểu `TypeNode` (return_type và parameters[].type)

Áp dụng cho `return_type` và từng `parameters[].type`, kiểm **đệ quy** từ gốc. Giới hạn lấy từ
`02-bd/architecture/harness.md:103` (ARRAY tối đa 3 chiều, độ sâu lồng tối đa 3).

Bảng 1: Quy tắc lược đồ kiểu.

| Mục kiểm | Quy tắc | Mã | Loại | Tầng |
| :--- | :--- | :--- | :--- | :--- |
| Hình dạng nút | Nút là một đối tượng JSON; `kind` là chuỗi; không có trường lạ; `of` chỉ có ở `ARRAY` `LIST` `LINKED_LIST` `BINARY_TREE` và phải là đối tượng; `dimensions` chỉ có ở `ARRAY` và phải là số nguyên `>= 1` | `HRN-V10` | Cứng | PB, HR, FE |
| `kind` hợp lệ | Thuộc tập `INT` `LONG` `DOUBLE` `BOOLEAN` `CHAR` `STRING` `ARRAY` `LIST` `LINKED_LIST` `BINARY_TREE`. Giá trị khác, kể cả `OTHER`, là ngoài lược đồ. **`OTHER` không bao giờ được lưu** | `HRN-V01` | Mềm | PB, HR |
| `of` bắt buộc | `ARRAY` `LIST` `LINKED_LIST` `BINARY_TREE` thiếu `of` | `HRN-V02` | Mềm | PB, HR |
| `of` được phép | `ARRAY.of` thuộc `{INT, LONG, DOUBLE, BOOLEAN, CHAR, STRING}`; `LIST.of` là kiểu nguyên thuỷ (kể cả `STRING`) hoặc `LIST`; `LINKED_LIST.of` và `BINARY_TREE.of` thuộc `{INT, LONG, DOUBLE, BOOLEAN}` | `HRN-V03` | Mềm | PB, HR |
| Số chiều | `ARRAY.dimensions` lớn hơn 3 | `HRN-V04` | Mềm | PB, HR |
| Độ sâu lồng | Trên một đường từ gốc tới lá, số nút container (`ARRAY`, `LIST`, `LINKED_LIST`, `BINARY_TREE`) vượt 3. Một `ARRAY` nhiều chiều tính **một** nút container (số chiều đã bị giới hạn riêng) | `HRN-V05` | Mềm | PB, HR |
| Kiểu giá trị node thống nhất | Trong **một đặc tả**, mọi `LINKED_LIST` có cùng `of`; mọi `BINARY_TREE` có cùng `of` (mỗi chương trình sinh ra chỉ có một lớp `ListNode` và một `TreeNode`) | `HRN-V06` | Mềm | PB, HR |

Giải thích phân loại: hai loại sai khác nhau. **Sai cấu trúc** (không phải đối tượng, trường lạ, `dimensions` âm hoặc
không nguyên) là dữ liệu hỏng, A2 sửa được, nên cứng. **Đúng cấu trúc nhưng ngoài tập đóng** (kiểu lạ, tổ hợp không
hỗ trợ, quá giới hạn) là đúng điều F3-13 mô tả, nên mềm và chuyển bài về Standard I/O
(`01-rd/req/harness.md:30-34`). `[SoT: Suy luận]` cho ranh giới hai loại ở trường hợp "thiếu `of`" và "độ sâu quá giới
hạn": BD xếp chúng vào "kiểu ngoài lược đồ" (`02-bd/architecture/harness.md:103,196-198`) nên DD giữ là mềm.
Bảng "of được phép" là chốt của DD (lập luận ở `03-dd/logic/harness.md` mục 3.1).

Hậu điều kiện cho `FunctionSpecEvaluator`: khi **không** có vi phạm V01 đến V06 và chữ ký đủ, `wrapperSupported = true`
và **bảo đảm** cả ba plugin sinh được mã (lược đồ đóng, `02-bd/architecture/harness.md:203-208`). Test golden (logic mục
10) là nơi kiểm bảo đảm này.

## 3. Chữ ký hàm

Áp dụng cho `function_name`, `return_type`, `parameters`, `name_overrides` của `problem_specs`
(`02-bd/database/problem-bank.md:132-135`), chỉ khi bài có khai chữ ký.

**Định danh hợp lệ** (dùng cho tên hàm, tên tham số, giá trị ghi đè): khớp
`^[A-Za-z][A-Za-z0-9_]{0,63}$`, và ngoài ra không bắt đầu bằng tiền tố dành riêng `apH` / `ApH` của khung (kiểm riêng ở
`HRN-V12`, `HRN-V15`, `HRN-V19`). Độ dài tối đa 64 là chốt của DD
(`[SoT: Suy luận]`, BD chỉ nói `function_name` là VARCHAR không nêu độ dài).

Bảng 2: Quy tắc chữ ký hàm.

| Trường | Quy tắc | Mã | Loại | Tầng |
| :--- | :--- | :--- | :--- | :--- |
| Chữ ký có hay không | Không có `function_name` lẫn `return_type` lẫn `parameters` (cả ba null): bài chưa khai chữ ký, `wrapperSupported = false` | `HRN-V07` | Mềm | PB, HR |
| Chữ ký đủ | Có `function_name` thì phải có `return_type` và `parameters` (mảng, có thể rỗng) và ngược lại; thiếu một phần là chữ ký nửa vời | `HRN-V17` | Cứng | PB, HR, FE |
| `function_name` | Khớp định danh hợp lệ | `HRN-V11` | Cứng | PB, HR, FE |
| `function_name` đụng chỗ dành riêng | Không bắt đầu bằng `apH` hoặc `ApH` (không phân biệt hoa thường); **sau** khi suy ra tên từng ngôn ngữ (`logic` mục 2), tên đó không được là từ khoá của ngôn ngữ đó | `HRN-V12` | Cứng | PB, HR |
| `parameters` số lượng | Tối đa 10 tham số | `HRN-V16` | Cứng | PB, HR, FE |
| `parameters[].name` | Khớp định danh hợp lệ | `HRN-V13` | Cứng | PB, HR, FE |
| `parameters[].name` trùng | Sau khi suy ra tên từng ngôn ngữ (`logic` mục 2), hai tham số không được trùng nhau **trong bất kỳ ngôn ngữ nào** (ví dụ `maxVal` và `max_val` trùng) | `HRN-V14` | Cứng | PB, HR, FE |
| `parameters[].name` dành riêng | Không bắt đầu bằng `apH`/`ApH`; không là từ khoá ở ngôn ngữ nào; không trùng `function_name` đã suy ra; Python còn cấm `self` | `HRN-V15` | Cứng | PB, HR |
| `name_overrides` khoá | Chỉ nhận `JAVA`, `CPP`, `PYTHON` (đọc không phân biệt hoa thường, ghi chữ hoa) | `HRN-V18` | Cứng | PB, HR |
| `name_overrides` giá trị | Khớp định danh hợp lệ, không đụng tiền tố dành riêng, không là từ khoá của **đúng ngôn ngữ ghi đè** | `HRN-V19` | Cứng | PB, HR |

Từ khoá tính theo hợp của ba bảng (kiểm bằng hằng `RESERVED_WORDS[Language]` trong `algoprep-common`):

- **Java**: `abstract assert boolean break byte case catch char class const continue default do double else enum extends final
  finally float for goto if implements import instanceof int interface long native new package private protected public
  return short static strictfp super switch synchronized this throw throws transient try void volatile while true false
  null var record yield`.
- **C++**: `alignas alignof and and_eq asm auto bitand bitor bool break case catch char char8_t char16_t char32_t class compl
  concept const consteval constexpr constinit const_cast continue co_await co_return co_yield decltype default delete do double
  dynamic_cast else enum explicit export extern false float for friend goto if inline int long mutable namespace new noexcept
  not not_eq nullptr operator or or_eq private protected public register reinterpret_cast requires return short signed sizeof
  static static_assert static_cast struct switch template this thread_local throw true try typedef typeid typename union
  unsigned using virtual void volatile wchar_t while xor xor_eq`.
- **Python**: `False None True and as assert async await break class continue def del elif else except finally for from global
  if import in is lambda nonlocal not or pass raise return try while with yield`.

`[SoT: Suy luận]` toàn bộ mục 3, trừ phần `name_overrides` và `function_name` mà BD đã có
(`02-bd/database/problem-bank.md:132-135`). Lý do giữ các kiểm này ở mức **cứng**: tên chỉ xuất hiện trong mã khung hiển
thị cho người học và trong lời gọi hàm; tên sai làm mã khung **không biên dịch** ngay từ đầu, một lỗi không phải của người
học. Tên tham số **không** là tên biến của khung (khung dùng `apH_p0`..., `03-dd/logic/harness.md` mục 3.6), nên các kiểm
`HRN-V13` đến `HRN-V15` chỉ để bảo đảm mã khung biên dịch được.

Số đo khác (độ dài tối đa tên 64, tối đa 10 tham số) là hằng trong `algoprep-common`, không phải ô cấu hình
(`.claude/rules/ponytail-criteria.md` bậc 5; `DEC-2026-1001-admin-configurable-settings` không áp dụng vì chúng không phải
ô nhập đã tồn tại).

## 4. Quy tắc liên trường (chiến lược so khớp và kiểu)

Chiến lược so khớp (`matching_strategy`, `epsilon_value`) thuộc `problem_specs`, nhưng quy tắc liên trường với kiểu trả
về nằm ở `harness` vì kiểu là của F3 (`02-bd/architecture/harness.md:257-259`, `:300-301`).

Bảng 3: Quy tắc liên trường.

| Điều kiện | Quy tắc | Mã | Loại | Tầng |
| :--- | :--- | :--- | :--- | :--- |
| `matching_strategy = EPSILON` | `epsilon_value` phải khác null và `> 0` (đã là CHECK ở DB, `02-bd/database/problem-bank.md:129`; kiểm thêm ở PB để báo lỗi trước khi đụng DB và ở HR phòng thủ) | `HRN-V20` | Cứng | PB, HR, FE; DB (CHECK) |
| `matching_strategy = UNORDERED_SET` và `return_type.kind` là `BINARY_TREE` hoặc `LINKED_LIST` | Tổ hợp không hợp lệ: khái niệm "không xét thứ tự" không có nghĩa với hai kiểu này | `HRN-V22` | Cứng | PB, HR, FE |
| `matching_strategy` khác `EPSILON` và `epsilon_value` có giá trị | Cho phép, giá trị bị bỏ qua (`02-bd/database/problem-bank.md:129`: "chỉ dùng khi EPSILON") | không có mã | Không phải vi phạm | |

Ghi chú: ở `HR`, vi phạm này được báo bằng `HRN-051` (`03-dd/api/harness.md` mục 7.1) vì ở lúc chạy không còn là lỗi
của A2 đang nhập. `HRN-V21` bỏ trống: giữ chỗ cho "epsilon có giá trị nhưng chiến lược khác" nếu sau này muốn cảnh báo.

Chưa chặn các tổ hợp sau (BD không nêu): `EPSILON` khi kiểu trả về không chứa lá `DOUBLE`; `UNORDERED_SET` khi kiểu
trả về là nguyên thuỷ. Cả hai có nghĩa toán học (suy biến thành so bằng) nên cho qua; xem câu hỏi mở Q15 ở
`03-dd/logic/harness.md` mục 12.

## 5. Đầu vào của các cổng lúc chạy

Quy tắc kiểm đầu vào của P2, P3, P4; lỗi là ngoại lệ `HRN-nnn` (`03-dd/api/harness.md` mục 7.1). Các tham số đã được kiểu
`enum` và kiểu dữ liệu chặn ở ranh giới trong tiến trình; bảng chỉ liệt kê điều vượt khỏi kiểu.

Bảng 4: Quy tắc đầu vào cổng.

| Cổng | Trường | Quy tắc | Mã | Tầng |
| :--- | :--- | :--- | :--- | :--- |
| P1, P2, P3 | `language` | Có plugin trong registry (ba giá trị) | `HRN-010` | HR |
| P2 | `sourceCode` | Sau khi bỏ khoảng trắng, không rỗng | `HRN-020` | HR |
| P2 | `sourceCode` | Tối đa 65 536 byte UTF-8 (đề xuất 64 KB của `02-bd/security/harness.md:53-57`, chốt cùng `judge-orchestration`; ở đây là phòng thủ theo chiều sâu, kiểm chính ở bước nhận bài nộp của F4) | `HRN-021` | HR |
| P2 | `sourceCode` | Không chứa ký tự NUL (`U+0000`) | `HRN-022` | HR |
| P2 | `expectedSpecVersion` | Nếu có, bằng `spec_version` hiện hành | `HRN-002` | HR |
| P1, P2 | `model = FUNCTION_WRAPPER` | `functionWrapperSupported = true`; có chữ ký đủ | `HRN-004`, `HRN-005` | HR |
| P1, P2 | đặc tả đọc từ cổng ra | Qua lại `FunctionSpecEvaluator` không có vi phạm cứng và không ngoài lược đồ | `HRN-006` | HR |
| P3 | `lineMap` | `userLineCount >= 1`; `prefixLineCount >= 0`; `callSiteStartLine <= callSiteEndLine` nếu có; hai giá trị gọi hàm đều ngoài vùng người học | `HRN-040` | HR |
| P3 | `rawCompilerOutput` | Cắt còn 65 536 ký tự đầu, không phải lỗi | không có mã | HR |
| P4 | `matchSpec` | `EPSILON` thì `epsilonValue > 0`; `UNORDERED_SET` không đi với kiểu `BINARY_TREE`/`LINKED_LIST` | `HRN-051` | HR |
| P4 | `expectedOutput` | Parse được theo `returnType` khi `EPSILON` hoặc `UNORDERED_SET` | `HRN-050` | HR |

Không kiểm: nội dung mã người học (không parse, không duyệt "an toàn", `02-bd/security/harness.md:22-26`); độ dài
`stdin`, kích thước testcase (thuộc `problem-bank` và `judge-orchestration`).

## 6. Bảng mã `HRN-Vnn`

Bảng 5: Mã kiểm đặc tả. Mã thông báo cho người dùng (i18n) do `problem-bank` ánh xạ; `harness` chỉ trả mã và `path`.

| Mã | Tên | Loại |
| :--- | :--- | :--- |
| `HRN-V01` | `TYPE_KIND_UNSUPPORTED` | Mềm |
| `HRN-V02` | `TYPE_OF_MISSING` | Mềm |
| `HRN-V03` | `TYPE_OF_NOT_ALLOWED` | Mềm |
| `HRN-V04` | `ARRAY_DIMENSIONS_EXCEEDED` | Mềm |
| `HRN-V05` | `TYPE_NESTING_EXCEEDED` | Mềm |
| `HRN-V06` | `NODE_VALUE_TYPE_MIXED` | Mềm |
| `HRN-V07` | `SIGNATURE_ABSENT` | Mềm |
| `HRN-V10` | `TYPE_NODE_MALFORMED` | Cứng |
| `HRN-V11` | `FUNCTION_NAME_INVALID` | Cứng |
| `HRN-V12` | `FUNCTION_NAME_RESERVED` | Cứng |
| `HRN-V13` | `PARAM_NAME_INVALID` | Cứng |
| `HRN-V14` | `PARAM_NAME_DUPLICATE` | Cứng |
| `HRN-V15` | `PARAM_NAME_RESERVED` | Cứng |
| `HRN-V16` | `PARAM_COUNT_EXCEEDED` | Cứng |
| `HRN-V17` | `SIGNATURE_INCOMPLETE` | Cứng |
| `HRN-V18` | `NAME_OVERRIDE_KEY_INVALID` | Cứng |
| `HRN-V19` | `NAME_OVERRIDE_VALUE_INVALID` | Cứng |
| `HRN-V20` | `EPSILON_VALUE_REQUIRED` | Cứng |
| `HRN-V22` | `UNORDERED_SET_TYPE_INVALID` | Cứng |

Tổng: 19 mã kiểm đặc tả (7 mềm, 12 cứng), dải `HRN-V01` đến `HRN-V22`, `HRN-V08`, `HRN-V09`, `HRN-V21` để trống.

## 7. Điểm cần đồng bộ với module khác

| Với | Điều cần đồng bộ |
| :--- | :--- |
| `problem-bank` (`SaveProblemSpec`) | Gọi `FunctionSpecEvaluator.evaluate`; vi phạm cứng thì từ chối lưu; `wrapperSupported` ghi vào `problems.function_wrapper_supported`; `unsupportedReasons` trả cho màn `problem_authoring` hiển thị cảnh báo (`02-bd/screens/shared/SHR0202_problem_authoring.md` hàng "Cảnh báo vượt lược đồ", dòng 411, 546). Tăng `spec_version` khi bất kỳ cột nào đổi và phát `ProblemSpecChanged` |
| Tab "Đặc tả" của `problem_authoring` (FE) | Lược đồ zod khớp mục 2 đến 4 (nguồn một nơi); phần cấu trúc dùng JSON Schema xuất kèm `TypeKind` |
| `judge-orchestration` | Dịch `HRN-nnn` của cổng sang trạng thái nộp bài; tôn trọng `HRN-004`; gọi `prepare` chỉ một lần cho mỗi lượt nộp |
