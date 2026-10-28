# Test case QA — F3: cây, stdout debug và dữ liệu expected-output lỗi

> Trạng thái: 10 ca đã review thiết kế; chưa thực thi. Nguồn: `04-tdd/modules/F3_harness.md` AC-28–30; `03-dd/api/F3_harness.md` §6.2; `03-dd/logic/F3_harness.md` §3.4, §5.4, §7.1–7.4.

## Fixture chung

- Gọi trực tiếp `MatchOutputUseCase.match` (P4) bằng `MatchOutputCommand`. Mô hình `FUNCTION_WRAPPER` dùng `resultSentinel="@@APH-RESULT@@"`; phần sau dòng sentinel cuối cùng là `actualOutput`, phần trước là `userConsole`. `expectedOutput` chỉ là đầu vào của P4 và không xuất hiện trong `MatchResult`.
- Dùng `returnType=BINARY_TREE<INT>` cho cây và `returnType=INT` cho nhánh debug; `matchingStrategy=EXACT` trừ khi ca ghi khác. P4 là cổng thuần: không có DB, RabbitMQ hoặc sandbox. Mỗi ca độc lập, không dùng kết quả ca trước làm tiền đề.

| AC | Nhánh | Test case | Review | Thực thi |
| :--- | :--- | :--- | :--- | :--- |
| AC-28 | Cùng giá trị khác hình dạng; hai cây rỗng | TC-F3-AC28-01 đến -02 | Đã review | Chưa thực hiện |
| AC-29 | Debug trước kết quả; thoát sớm; dùng sentinel cuối | TC-F3-AC29-01 đến -03 | Đã review | Chưa thực hiện |
| AC-30 | Không lộ expected/diff; expected lỗi; actual lỗi; cấu hình cũ lỗi | TC-F3-AC30-01 đến -05 | Đã review | Chưa thực hiện |

## Test case

### TC-F3-AC28-01 — Hai cây cùng giá trị nhưng khác hình dạng không khớp

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-28; DD F3 logic §3.4 BINARY_TREE, §7.2 EXACT |
| Kind / priority | Abnormal, P1: không so cây như tập giá trị |
| Environment | P4 component/integration |
| Preconditions | `model=FUNCTION_WRAPPER`, `returnType=BINARY_TREE<INT>`, `matchingStrategy=EXACT`, sentinel chung |
| Test data | `actualStdout="@@APH-RESULT@@\n1 2\n"` (node 2 là con trái); `expectedOutput="1 # 2\n"` (node 2 là con phải) |
| Steps | Gọi `match`, lưu response. |
| Expected | `NO_MATCH`, `actualOutput="1 2"`; response không chứa expected-output hoặc diff. |
| Cleanup | Reset fixture |
| Evidence | Command gồm mô tả hai cây, response |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC28-02 — Cây rỗng khớp cây rỗng

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-28; DD F3 logic §3.4 BINARY_TREE, §7.2 EXACT |
| Kind / priority | Boundary, P1: ký hiệu cây rỗng |
| Environment | P4 component/integration |
| Preconditions | Cùng cấu hình cây với AC28-01 |
| Test data | `actualStdout="@@APH-RESULT@@\n#\n"`, `expectedOutput="#\n"` |
| Steps | Gọi `match`. |
| Expected | `MATCH`, `actualOutput="#"`, `userConsole=""`. |
| Cleanup | Reset fixture |
| Evidence | Command, response |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC29-01 — Dòng debug đi vào console, kết quả đúng vẫn khớp

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-29; DD F3 logic §5.4, API §6.2 |
| Kind / priority | Normal, P1: debug không làm sai đáp án |
| Environment | P4 component/integration |
| Preconditions | `model=FUNCTION_WRAPPER`, `returnType=INT`, `matchingStrategy=EXACT`, sentinel chung |
| Test data | `actualStdout="debug: start\ndebug: n=3\n@@APH-RESULT@@\n7\n"`, `expectedOutput="7\n"` |
| Steps | Gọi `match`; đọc ba trường response. |
| Expected | `MATCH`, `actualOutput="7"`, `userConsole` chứa đúng hai dòng debug theo thứ tự và không chứa dòng kết quả `7`. Không có expected/diff. |
| Cleanup | Reset fixture |
| Evidence | Command, response |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC29-02 — Thoát trước khi in sentinel là NO_MATCH

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-29; DD F3 logic §5.4, §7.4 |
| Kind / priority | Abnormal, P1: thiếu kết quả không thành lỗi hệ thống |
| Environment | P4 component/integration |
| Preconditions | `model=FUNCTION_WRAPPER`, `returnType=INT`, `matchingStrategy=EXACT`, sentinel chung |
| Test data | `actualStdout="debug: start\n"`, `expectedOutput="7\n"`; không có dòng `@@APH-RESULT@@` |
| Steps | Gọi `match`. |
| Expected | `NO_MATCH`, không ném `HRN-050`, `HRN-031` hoặc lỗi hệ thống; response không chứa expected/diff. |
| Cleanup | Reset fixture |
| Evidence | Command, response/error log |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC29-03 — Dùng sentinel cuối cùng khi debug in giả sentinel

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-29; DD F3 logic §5.4 chọn dòng sentinel cuối |
| Kind / priority | Security, P1: không tách nhầm output debug |
| Environment | P4 component/integration |
| Preconditions | `model=FUNCTION_WRAPPER`, `returnType=INT`, `matchingStrategy=EXACT`, sentinel chung |
| Test data | `actualStdout="@@APH-RESULT@@\n999\n@@APH-RESULT@@\n7\n"`, `expectedOutput="7\n"` |
| Steps | Gọi `match`; đối chiếu `actualOutput` và `userConsole`. |
| Expected | `MATCH`, `actualOutput="7"`; `userConsole` giữ phần trước sentinel cuối gồm dòng sentinel đầu và `999`, không lấy `999` để so đáp án. |
| Cleanup | Reset fixture |
| Evidence | Command, response |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC30-01 — Kết quả sai không lộ expected-output hoặc diff

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-30; DD F3 API §6.2 `MatchResult`, logic §7.4 |
| Kind / priority | Security, P0: bảo vệ testcase Hidden |
| Environment | P4 component/integration; fixture testcase `HIDDEN` ở caller |
| Preconditions | `model=STANDARD_IO`, `matchingStrategy=EXACT`, `returnType=null`, `resultSentinel=null` |
| Test data | `actualStdout="wrong\n"`, `expectedOutput="SECRET_EXPECTED_9237\n"` |
| Steps | Gọi `match`; serialize toàn bộ response và kiểm tên field/giá trị. |
| Expected | `NO_MATCH`, `actualOutput="wrong"`, `userConsole=""`; response không có `expectedOutput`, chuỗi `SECRET_EXPECTED_9237`, diff, vị trí sai hoặc số ký tự khác. P4 không ghi log chứa expected-output ra kênh người học. |
| Cleanup | Reset fixture |
| Evidence | Command được giữ trong kho QA hạn chế, response đã serialize và log quan sát được |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC30-02 — Expected-output sai định dạng là lỗi dữ liệu testcase

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-30; DD F3 API §6.2 `HRN-050`, logic §7.4 |
| Kind / priority | Abnormal, P0: không gán WA cho người học khi dữ liệu testcase hỏng |
| Environment | P4 component/integration |
| Preconditions | `model=FUNCTION_WRAPPER`, `returnType=DOUBLE`, `matchingStrategy=EPSILON`, `epsilonValue=0.001`, sentinel chung |
| Test data | `actualStdout="@@APH-RESULT@@\n1.000000000000\n"`, `expectedOutput="not-a-number\n"` |
| Steps | Gọi `match`; kiểm mã lỗi thay vì verdict. |
| Expected | Ném/trả lỗi `HRN-050 EXPECTED_OUTPUT_UNPARSEABLE`; không trả `NO_MATCH`, không tạo verdict WA tại P4; lỗi trả ra không chứa expected-output thô. Caller F4 phải xử lý như lỗi dữ liệu testcase theo hợp đồng chấm, được kiểm riêng ở ca tích hợp F4. |
| Cleanup | Reset fixture |
| Evidence | Command, mã lỗi, response/log đã lọc |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC30-03 — Actual-output sai định dạng là NO_MATCH

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-30; DD F3 logic §7.4, API §6.2 |
| Kind / priority | Abnormal, P1: phân biệt output học viên với expected hỏng |
| Environment | P4 component/integration |
| Preconditions | `model=FUNCTION_WRAPPER`, `returnType=DOUBLE`, `matchingStrategy=EPSILON`, `epsilonValue=0.001`, sentinel chung |
| Test data | `actualStdout="@@APH-RESULT@@\nnot-a-number\n"`, `expectedOutput="1.000000000000\n"` |
| Steps | Gọi `match`. |
| Expected | `NO_MATCH`, không `HRN-050` vì expected hợp lệ; không chứa expected/diff trong response. |
| Cleanup | Reset fixture |
| Evidence | Command, response |
| Design review | Đã review; chưa thực thi |
### TC-F3-AC30-04 — P4 chặn cấu hình EPSILON hoặc UNORDERED_SET cũ không hợp lệ

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-30; DD F3 logic §7.4, validation §5 `HRN-051` |
| Kind / priority | Abnormal, P0: phòng thủ khi đặc tả cũ vượt qua P5 |
| Environment | P4 component/integration; không gọi F2 SaveProblemSpec |
| Preconditions | Gọi trực tiếp P4 bằng `MatchOutputCommand` để mô phỏng `matchSpec` lưu trước khi áp dụng validation |
| Test data | A: `model=FUNCTION_WRAPPER`, `returnType=DOUBLE`, `matchingStrategy=EPSILON`, `epsilonValue=0`, `actualStdout="@@APH-RESULT@@\n1.0\n"`, `expectedOutput="1.0\n"`. B: giống A nhưng `matchingStrategy=UNORDERED_SET`, `epsilonValue=null`, `returnType=BINARY_TREE<INT>`, actual/expected đều `"@@APH-RESULT@@\n1\n"`/`"1\n"` |
| Steps | Gọi P4 độc lập cho A và B, lưu mã lỗi và kiểm không có verdict. |
| Expected | Cả hai trả/ném `HRN-051 MATCH_CONFIG_INVALID` thay vì `MATCH`/`NO_MATCH`; response lỗi không chứa expected-output thô. |
| Cleanup | Reset fixture |
| Evidence | Hai command, error code và response/log đã lọc |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC30-05 — Expected LIST hỏng dưới UNORDERED_SET là HRN-050

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-30; DD F3 logic §7.2, §7.4 `HRN-050` |
| Kind / priority | Abnormal, P1: lỗi expected cũng được nhận diện ở UNORDERED_SET |
| Environment | P4 component/integration |
| Preconditions | `model=FUNCTION_WRAPPER`, `returnType=LIST<INT>`, `matchingStrategy=UNORDERED_SET`, `epsilonValue=null`, sentinel chung |
| Test data | `actualStdout="@@APH-RESULT@@\n[1,2]\n"`, `expectedOutput="[1,2\n"` (thiếu ngoặc đóng) |
| Steps | Gọi `match`, kiểm mã lỗi và DTO/log. |
| Expected | `HRN-050 EXPECTED_OUTPUT_UNPARSEABLE`, không có verdict hay WA; DTO/log ra kênh người học không chứa chuỗi expected thô. |
| Cleanup | Reset fixture |
| Evidence | Command trong kho QA hạn chế, error response/log đã lọc |
| Design review | Đã review; chưa thực thi |

