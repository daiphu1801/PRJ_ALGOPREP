# Test case QA — F3: so khớp output EXACT, TRIMMED, EPSILON

> Trạng thái: 19 ca đã review thiết kế; chưa thực thi. Nguồn: `04-tdd/modules/F3_harness.md` AC-23–26; `03-dd/api/F3_harness.md` §6.2; `03-dd/logic/F3_harness.md` BR-08 §7. Nhánh `UNORDERED_SET` AC-27 nằm trong `F3_harness_contracts.md`.

## Fixture và cách kiểm

- Gọi trực tiếp cổng P4 `MatchOutputUseCase.match` bằng `MatchOutputCommand`: `model`, `matchSpec`, `resultSentinel`, `actualStdout`, `expectedOutput`. Mặc định `model=STANDARD_IO`, `resultSentinel=null`, `returnType=null`; đổi rõ ở từng ca. Không truy cập DB, sandbox hoặc queue vì P4 là cổng thuần.
- `MATCH`/`NO_MATCH` là verdict duy nhất. Trong mọi response, `actualOutput` là output sau chuẩn hoá xuống dòng, `userConsole=""` ở Standard I/O; không có `expectedOutput`, diff hoặc vị trí sai. Evidence gồm command và response đã che dữ liệu nhạy cảm khi cần.
- Với `FUNCTION_WRAPPER`, `returnType` là TypeNode hợp lệ theo DD F3. Dùng `resultSentinel="@@APH-RESULT@@"`; `actualStdout="@@APH-RESULT@@\n" + result` để tách riêng phần kết quả. Fixture sử dụng serialisation đúng ngữ pháp DD. Sau ca không có dữ liệu để dọn; reset stub/fixture P4 nếu runner giữ trạng thái.

| AC | Nhánh | Test case | Review | Thực thi |
| :--- | :--- | :--- | :--- | :--- |
| AC-23 | Giống hệt, sai ký tự, sai dấu cách, khác newline cuối | TC-F3-AC23-01 đến -04 | Đã review | Chưa thực hiện |
| AC-24 | Khoảng trắng, sai nội dung, dòng trống, LIST và TREE so như EXACT | TC-F3-AC24-01 đến -05 | Đã review | Chưa thực hiện |
| AC-25 | Bằng epsilon, vượt epsilon, số không hữu hạn | TC-F3-AC25-01 đến -03 | Đã review | Chưa thực hiện |
| AC-26 | Cấu trúc lồng đúng, sai hình dạng/phần tử, mảng 2D và Standard I/O token | TC-F3-AC26-01 đến -07 | Đã review | Chưa thực hiện |

## Test case

### TC-F3-AC23-01 — EXACT nhận chuỗi giống hệt

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-23; DD F3 logic §7.1–7.2, API §6.2 |
| Kind / priority | Normal, P1: mốc đối chứng |
| Environment | P4 component/integration |
| Preconditions | P4 sẵn sàng, `matchSpec.matchingStrategy=EXACT` |
| Test data | `actualStdout="alpha beta\n"`, `expectedOutput="alpha beta\n"` |
| Steps | Gọi `match`, ghi toàn bộ response. |
| Expected | `verdict=MATCH`, `actualOutput="alpha beta"`, `userConsole=""`; response không chứa expected hay diff. |
| Cleanup | Reset fixture |
| Evidence | Command, response |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC23-02 — EXACT từ chối khác một ký tự

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-23; DD F3 logic §7.2 EXACT |
| Kind / priority | Abnormal, P1: so từng ký tự |
| Environment | P4 component/integration |
| Preconditions | `matchingStrategy=EXACT` |
| Test data | `actualStdout="alpha betb\n"`, `expectedOutput="alpha beta\n"` |
| Steps | Gọi `match`. |
| Expected | `NO_MATCH`, `actualOutput="alpha betb"`; không có diff/vị trí ký tự sai hoặc expected trong response. |
| Cleanup | Reset fixture |
| Evidence | Command, response |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC23-03 — EXACT từ chối khác một dấu cách

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-23; DD F3 logic §7.2 EXACT |
| Kind / priority | Boundary, P1: EXACT không gộp khoảng trắng |
| Environment | P4 component/integration |
| Preconditions | `matchingStrategy=EXACT` |
| Test data | `actualStdout="alpha  beta\n"`, `expectedOutput="alpha beta\n"` |
| Steps | Gọi `match`. |
| Expected | `NO_MATCH`; `actualOutput="alpha  beta"`; không có expected/diff trong response. |
| Cleanup | Reset fixture |
| Evidence | Command, response |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC23-04 — EXACT chuẩn hoá CRLF và bỏ mọi newline ở cuối

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-23; DD F3 logic §7.1, Q12 đã đóng |
| Kind / priority | Boundary, P1: xuống dòng kết thúc không gây WA |
| Environment | P4 component/integration |
| Preconditions | `matchingStrategy=EXACT` |
| Test data | `actualStdout="alpha\r\n\r\n"`, `expectedOutput="alpha\n"` |
| Steps | Gọi `match`. |
| Expected | `MATCH`, `actualOutput="alpha"`; chỉ newline ở cuối bị bỏ, không trim khoảng trắng nội bộ. |
| Cleanup | Reset fixture |
| Evidence | Command, response |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC24-01 — TRIMMED nhận khoảng trắng thừa theo dòng

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-24; DD F3 logic §7.2 TRIMMED |
| Kind / priority | Normal, P1: bỏ khoảng trắng nhưng giữ cấu trúc dòng |
| Environment | P4 component/integration |
| Preconditions | `model=FUNCTION_WRAPPER`, `matchingStrategy=TRIMMED`, `returnType=ARRAY<INT,2>`, sentinel chung |
| Test data | `actualStdout="@@APH-RESULT@@\n  1   2  \n\t3\t 4 \n"`, `expectedOutput="1 2\n3 4\n"` |
| Steps | Gọi `match`. |
| Expected | `MATCH`; `actualOutput` trả chuỗi chuẩn hoá newline `"  1   2  \n\t3\t 4 "`, không trả expected/diff. |
| Cleanup | Reset fixture |
| Evidence | Command, response |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC24-02 — TRIMMED không che khác biệt giá trị

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-24; DD F3 logic §7.2 TRIMMED |
| Kind / priority | Abnormal, P1: giữ khác biệt nội dung |
| Environment | P4 component/integration |
| Preconditions | `model=FUNCTION_WRAPPER`, `matchingStrategy=TRIMMED`, `returnType=ARRAY<INT,2>`, sentinel chung |
| Test data | `actualStdout="@@APH-RESULT@@\n1 2\n3 5\n"`, `expectedOutput="1 2\n3 4\n"` |
| Steps | Gọi `match`. |
| Expected | `NO_MATCH`; không có expected/diff trong response. |
| Cleanup | Reset fixture |
| Evidence | Command, response |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC24-03 — TRIMMED với LIST giữ khoảng trắng trong chuỗi

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-24; DD F3 logic §7.2 TRIMMED ngoại lệ LIST |
| Kind / priority | Boundary, P1: bảo toàn nội dung STRING |
| Environment | P4 component/integration |
| Preconditions | `model=FUNCTION_WRAPPER`, `returnType=LIST<LIST<STRING>>`, `matchingStrategy=TRIMMED`, sentinel chung |
| Test data | `actualStdout="@@APH-RESULT@@\n[[\"a  b\"]]\n"`, `expectedOutput="[[\"a b\"]]\n"` |
| Steps | Gọi `match`. |
| Expected | `NO_MATCH`; `actualOutput="[[\"a  b\"]]"`, không gộp hai dấu cách trong chuỗi. |
| Cleanup | Reset fixture |
| Evidence | Command, response |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC24-04 — TRIMMED với cây so như EXACT

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-24; DD F3 logic §7.2 TRIMMED ngoại lệ BINARY_TREE |
| Kind / priority | Boundary, P1: không chuẩn hoá thêm cây |
| Environment | P4 component/integration |
| Preconditions | `model=FUNCTION_WRAPPER`, `returnType=BINARY_TREE<INT>`, `matchingStrategy=TRIMMED`, sentinel chung |
| Test data | `actualStdout="@@APH-RESULT@@\n1  2 3\n"`, `expectedOutput="1 2 3\n"` |
| Steps | Gọi `match`. |
| Expected | `NO_MATCH` dù chỉ khác dấu cách; response không chứa expected/diff. |
| Cleanup | Reset fixture |
| Evidence | Command, response |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC24-05 — TRIMMED giữ dòng trống ở giữa

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-24; DD F3 logic §7.2 TRIMMED |
| Kind / priority | Boundary, P1: không gộp hoặc xoá dòng trống nội bộ |
| Environment | P4 component/integration |
| Preconditions | `model=STANDARD_IO`, `matchingStrategy=TRIMMED`, `returnType=null`, `resultSentinel=null` |
| Test data | A: `actualStdout="a\n\nb\n"`, `expectedOutput="a\n\nb\n"`; B: cùng actual, `expectedOutput="a\nb\n"` |
| Steps | Gọi P4 độc lập cho A/B, lưu response. |
| Expected | A `MATCH`, B `NO_MATCH`; cả hai `actualOutput="a\n\nb"`, `userConsole=""`, không chứa expected/diff. |
| Cleanup | Reset fixture |
| Evidence | Hai command/response |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC25-01 — EPSILON nhận sai lệch đúng bằng 0,001

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-25; DD F3 logic §7.2 EPSILON |
| Kind / priority | Boundary, P0: biên bao hàm |
| Environment | P4 component/integration |
| Preconditions | `model=FUNCTION_WRAPPER`, `returnType=DOUBLE`, `matchingStrategy=EPSILON`, `epsilonValue=0.001`, sentinel chung |
| Test data | `actualStdout="@@APH-RESULT@@\n1.001000000000\n"`, `expectedOutput="1.000000000000\n"` |
| Steps | Gọi `match`. |
| Expected | `MATCH`; không có expected/diff trong response. |
| Cleanup | Reset fixture |
| Evidence | Command, response |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC25-02 — EPSILON từ chối sai lệch 0,0011

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-25; DD F3 logic §7.2 EPSILON |
| Kind / priority | Boundary, P0: ngoài biên |
| Environment | P4 component/integration |
| Preconditions | `model=FUNCTION_WRAPPER`, `returnType=DOUBLE`, `matchingStrategy=EPSILON`, `epsilonValue=0.001`, sentinel chung |
| Test data | `actualStdout="@@APH-RESULT@@\n1.001100000000\n"`, `expectedOutput="1.000000000000\n"` |
| Steps | Gọi `match`. |
| Expected | `NO_MATCH`; không có expected/diff trong response. |
| Cleanup | Reset fixture |
| Evidence | Command, response |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC25-03 — EPSILON so giá trị không hữu hạn theo quy tắc riêng

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-25; DD F3 logic §7.2 EPSILON, `NaN`/`Infinity` |
| Kind / priority | Boundary, P1: không áp dụng phép trừ cho số không hữu hạn |
| Environment | P4 component/integration |
| Preconditions | `model=FUNCTION_WRAPPER`, `returnType=DOUBLE`, `matchingStrategy=EPSILON`, `epsilonValue=0.001`, sentinel chung |
| Test data | Bốn lần gọi độc lập: (`NaN`,`NaN`), (`NaN`,`1.000000000000`), (`Infinity`,`Infinity`), (`-Infinity`,`Infinity`); mỗi `actualStdout="@@APH-RESULT@@\n" + actual + "\n"`, `expectedOutput=expected + "\n"` |
| Steps | Gọi `match` riêng cho từng cặp, lưu từng command/response. |
| Expected | Verdict lần lượt `MATCH`, `NO_MATCH`, `MATCH`, `NO_MATCH`; không ném `HRN-050` với các literal không hữu hạn hợp lệ theo DD và không lộ expected/diff. |
| Cleanup | Reset fixture |
| Evidence | Bốn command và response |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC26-01 — EPSILON so từng lá số thực trong cấu trúc lồng

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-26; DD F3 logic §7.2 EPSILON |
| Kind / priority | Normal, P0: so theo vị trí trong cấu trúc |
| Environment | P4 component/integration |
| Preconditions | `model=FUNCTION_WRAPPER`, `returnType=LIST<LIST<DOUBLE>>`, `matchingStrategy=EPSILON`, `epsilonValue=0.001`, sentinel chung |
| Test data | `actualStdout="@@APH-RESULT@@\n[[1.000500000000,2.000000000000],[3.000900000000]]\n"`, `expectedOutput="[[1.000000000000,2.000000000000],[3.000000000000]]\n"` |
| Steps | Gọi `match`. |
| Expected | `MATCH`, giữ đúng thứ tự/hình dạng; không trả expected/diff. |
| Cleanup | Reset fixture |
| Evidence | Command, response |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC26-02 — EPSILON từ chối cấu trúc thiếu phần tử

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-26; DD F3 logic §7.2 EPSILON |
| Kind / priority | Abnormal, P0: hình dạng không được phép lệch |
| Environment | P4 component/integration |
| Preconditions | `model=FUNCTION_WRAPPER`, `returnType=LIST<LIST<DOUBLE>>`, `matchingStrategy=EPSILON`, `epsilonValue=0.001`, sentinel chung |
| Test data | `actualStdout="@@APH-RESULT@@\n[[1.000000000000],[3.000000000000]]\n"`, `expectedOutput="[[1.000000000000,2.000000000000],[3.000000000000]]\n"` |
| Steps | Gọi `match`. |
| Expected | `NO_MATCH`, không ném `HRN-050` vì expected hợp lệ và actual chỉ thiếu phần tử. |
| Cleanup | Reset fixture |
| Evidence | Command, response |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC26-03 — EPSILON Standard I/O so số gần đúng và chữ chính xác

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-26; DD F3 logic §7.3 EPSILON Standard I/O |
| Kind / priority | Normal, P0: token số và chữ khác cách so |
| Environment | P4 component/integration |
| Preconditions | `model=STANDARD_IO`, `returnType=null`, `matchingStrategy=EPSILON`, `epsilonValue=0.001` |
| Test data | `actualStdout="score 1.0005\n"`, `expectedOutput="score 1.0000\n"` |
| Steps | Gọi `match`. |
| Expected | `MATCH`, vì token `score` giống chính xác và số lệch 0,0005; `actualOutput="score 1.0005"`. |
| Cleanup | Reset fixture |
| Evidence | Command, response |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC26-04 — EPSILON từ chối một lá vượt sai số

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-26; DD F3 logic §7.2 EPSILON |
| Kind / priority | Boundary, P0: từng lá phải đạt sai số |
| Environment | P4 component/integration |
| Preconditions | `model=FUNCTION_WRAPPER`, `returnType=LIST<LIST<DOUBLE>>`, `matchingStrategy=EPSILON`, `epsilonValue=0.001`, sentinel chung |
| Test data | `actualStdout="@@APH-RESULT@@\n[[1.000500000000,2.001100000000],[3.000900000000]]\n"`, `expectedOutput="[[1.000000000000,2.000000000000],[3.000000000000]]\n"` |
| Steps | Gọi `match`. |
| Expected | `NO_MATCH` vì lá thứ hai lệch 0,0011 dù các lá còn lại đạt; không có expected/diff trong response. |
| Cleanup | Reset fixture |
| Evidence | Command, response |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC26-05 — EPSILON so mảng hai chiều đúng vị trí

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-26; DD F3 logic §7.2 EPSILON và §3.4 định dạng ARRAY |
| Kind / priority | Normal, P1: mỗi hàng và cột có vị trí cố định |
| Environment | P4 component/integration |
| Preconditions | `model=FUNCTION_WRAPPER`, `returnType=ARRAY<DOUBLE,2>`, `matchingStrategy=EPSILON`, `epsilonValue=0.001`, sentinel chung |
| Test data | `actualStdout="@@APH-RESULT@@\n1.000500000000 2.000000000000\n3.000000000000 4.000900000000\n"`, `expectedOutput="1.000000000000 2.000000000000\n3.000000000000 4.000000000000\n"` |
| Steps | Gọi `match`. |
| Expected | `MATCH`, vì hình dạng 2×2 giữ nguyên và mọi lá lệch tối đa 0,001. |
| Cleanup | Reset fixture |
| Evidence | Command, response |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC26-06 — EPSILON Standard I/O không bỏ qua chữ khác

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-26; DD F3 logic §7.3 EPSILON Standard I/O |
| Kind / priority | Abnormal, P1: token chữ phải chính xác |
| Environment | P4 component/integration |
| Preconditions | `model=STANDARD_IO`, `returnType=null`, `matchingStrategy=EPSILON`, `epsilonValue=0.001` |
| Test data | `actualStdout="scores 1.0005\n"`, `expectedOutput="score 1.0000\n"` |
| Steps | Gọi `match`. |
| Expected | `NO_MATCH` dù số lệch trong sai số, vì `scores` khác `score`. Không có expected/diff trong response. |
| Cleanup | Reset fixture |
| Evidence | Command, response |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC26-07 — EPSILON Standard I/O từ chối thiếu token

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-26; DD F3 logic §7.3 EPSILON Standard I/O |
| Kind / priority | Boundary, P1: số dòng và token phải bằng nhau |
| Environment | P4 component/integration |
| Preconditions | `model=STANDARD_IO`, `returnType=null`, `matchingStrategy=EPSILON`, `epsilonValue=0.001` |
| Test data | `actualStdout="score 1.0005\n"`, `expectedOutput="score 1.0000 2.0000\n"` |
| Steps | Gọi `match`. |
| Expected | `NO_MATCH` vì actual có hai token, expected có ba; không có expected/diff trong response. |
| Cleanup | Reset fixture |
| Evidence | Command, response |
| Design review | Đã review; chưa thực thi |
