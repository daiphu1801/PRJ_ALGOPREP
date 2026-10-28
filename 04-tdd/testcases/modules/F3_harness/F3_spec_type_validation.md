# Test case QA — F3: lược đồ kiểu và khả năng sinh mã Bọc hàm

> Trạng thái: 12 ca đã review thiết kế; chưa thực thi. Nguồn: `04-tdd/modules/F3_harness.md` AC-31–34; `03-dd/validation/F3_harness.md` §2, Bảng 1; `03-dd/api/F3_harness.md` §6.3; `03-dd/logic/F3_harness.md` §3, §10.

## Fixture và quy tắc đối chiếu

- `P-SPEC` là bài `UNPUBLISHED` thuộc tác giả `A-SPEC`, có quyền `PROBLEM_AUTHORING:UPDATE`; đặc tả ban đầu phiên bản 3, `function_wrapper_supported=false`. Mỗi ca dùng bản sao bài riêng để không lẫn `spec_version`. Đặc tả hợp lệ nền: `functionName="solve"`, `returnType={"kind":"INT"}`, `parameters=[{"name":"value","type":{"kind":"INT"}}]`, `nameOverrides={}`, `matchingStrategy="EXACT"`, `epsilonValue=null`.
- Ký pháp `ARRAY<T,k>` trong bảng fixture nghĩa là `{"kind":"ARRAY","of":T,"dimensions":k}`; `LIST<T>` là `{"kind":"LIST","of":T}`; `LINKED_LIST<T>` và `BINARY_TREE<T>` tương tự. Lá `INT/LONG/DOUBLE/BOOLEAN/CHAR/STRING` là object `{"kind":"..."}`. Mỗi dòng trong bảng được chạy **độc lập** trên bản sao đặc tả nền, không nối kết quả các dòng.
- `FunctionSpecEvaluator.evaluate` (P5) là cổng thuần. Với ca kiểm lưu, gọi thêm `SaveProblemSpec` F2 rồi đọc lại đặc tả và `problems.function_wrapper_supported`; với ca sinh mã, gọi P1/P2/golden harness cho `JAVA`, `CPP`, `PYTHON` với input hợp lệ theo kiểu. Không dùng hành vi FE làm oracle backend. Dọn bản sao bài, mã sinh và artifact sandbox sau ca.

| AC | Nhánh | Test case | Review | Thực thi |
| :--- | :--- | :--- | :--- | :--- |
| AC-31 | Nguyên thuỷ/chuỗi, ARRAY, LIST lồng, linked list/tree | TC-F3-AC31-01 đến -04 | Đã review | Chưa thực hiện |
| AC-32 | Kind lạ, thiếu `of`, tổ hợp không hỗ trợ, node khác kiểu, hình dạng JSON hỏng | TC-F3-AC32-01 đến -05 | Đã review | Chưa thực hiện |
| AC-33 | ARRAY bốn chiều, LIST bốn lớp | TC-F3-AC33-01 đến -02 | Đã review | Chưa thực hiện |
| AC-34 | Giữ nguyên TypeNode ngoài lược đồ khi lưu | TC-F3-AC34-01 | Đã review | Chưa thực hiện |

## Test case

### TC-F3-AC31-01 — Sáu kiểu lá hợp lệ sinh mã cho ba ngôn ngữ

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-31; DD validation F3 Bảng 1; API F3 §6.3 |
| Kind / priority | Normal, P0: lược đồ đóng phải sinh được mã |
| Environment | P5 component, F2 SaveProblemSpec, P1/P2 golden Java/C++/Python |
| Preconditions | Bản sao `P-SPEC` riêng cho từng dòng fixture; ba plugin sẵn sàng |
| Test data | Sáu `returnType` độc lập: `{"kind":"INT"}`, `LONG`, `DOUBLE`, `BOOLEAN`, `CHAR`, `STRING` theo cùng dạng object; `parameters=[]`; các trường khác theo nền |
| Steps | Với từng kiểu: gọi P5, lưu qua F2, gọi P1/P2 cho Java/C++/Python bằng giá trị mẫu hợp kiểu (INT 2, LONG 2, DOUBLE 2.5, BOOLEAN true, CHAR `a`, STRING `abc`); ghi kết quả từng dòng. |
| Expected | Cả sáu dòng có `violations=[]`, `unsupportedReasons=[]`, `wrapperSupported=true`; F2 lưu cờ true. Mã khung/gói chấm được sinh cho cả ba ngôn ngữ theo kiểu đang thử, không báo lỗi đặc tả hoặc thiếu plugin. Một dòng lỗi không được che bởi kết quả dòng khác. |
| Cleanup | Xoá sáu bản sao bài và artifact |
| Evidence | Bảng sáu dòng × ba ngôn ngữ, P5 response, DB và golden diff |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC31-02 — ARRAY một đến ba chiều đều được hỗ trợ

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-31; DD validation F3 Bảng 1, logic F3 §3.3–3.4 |
| Kind / priority | Boundary, P0: số chiều được phép |
| Environment | P5, F2 SaveProblemSpec, P1/P2 golden ba ngôn ngữ |
| Preconditions | Bản sao `P-SPEC` cho mỗi kích thước; plugin sẵn sàng |
| Test data | Ba `returnType` độc lập: `ARRAY<INT,1>`, `ARRAY<INT,2>`, `ARRAY<INT,3>`; `parameters=[]` |
| Steps | Với k=1,2,3, gọi P5, lưu, tạo input/output mẫu hợp định dạng DD F3 và sinh mã/gói chấm ở ba ngôn ngữ. |
| Expected | Mỗi k: `violations=[]`, `wrapperSupported=true`, cờ DB true; cả ba plugin tạo mã hợp lệ. k=3 là hợp lệ dù có ba chiều; không nhận `HRN-V04` hay `HRN-V05` (ARRAY nhiều chiều là một node container). |
| Cleanup | Xoá bản sao và artifact |
| Evidence | P5/DB, ba fixture kích thước, golden outputs theo ngôn ngữ |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC31-03 — LIST lồng tối đa ba lớp được hỗ trợ

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-31; DD validation F3 Bảng 1 độ sâu; logic F3 §3.4 |
| Kind / priority | Boundary, P0: danh sách lồng đúng giới hạn |
| Environment | P5, F2 SaveProblemSpec, P1/P2 golden ba ngôn ngữ |
| Preconditions | Bản sao `P-SPEC` cho từng độ sâu |
| Test data | `LIST<INT>`, `LIST<LIST<INT>>`, `LIST<LIST<LIST<INT>>>`, mỗi kiểu là TypeNode lồng đúng số lớp |
| Steps | Với từng độ sâu 1,2,3, gọi P5, lưu, sinh mã/gói chấm Java/C++/Python với giá trị mẫu `[1]`, `[[1]]`, `[[[1]]]` tương ứng. |
| Expected | Mỗi kiểu có `wrapperSupported=true`, không có violation/reason; F2 lưu cờ true; ba plugin sinh mã được và output mẫu theo đúng độ sâu. Không coi ba lớp là vượt giới hạn. |
| Cleanup | Xoá bản sao/artifact |
| Evidence | TypeNode JSON, P5 response, DB, golden diff |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC31-04 — LINKED_LIST và BINARY_TREE hợp lệ sinh được mã

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-31; DD validation F3 Bảng 1 `of`; logic F3 §3.3–3.4 |
| Kind / priority | Normal, P0: kiểu node dùng lớp sinh ra |
| Environment | P5, F2 SaveProblemSpec, P1/P2 golden ba ngôn ngữ |
| Preconditions | Hai bản sao `P-SPEC` độc lập |
| Test data | `returnType=LINKED_LIST<INT>` với output `2\n1 2\n`; `returnType=BINARY_TREE<INT>` với output `1 2 #\n`; mỗi kiểu dùng input hợp lệ cho hàm `solve` |
| Steps | Gọi P5/lưu/sinh mã ba ngôn ngữ cho từng kiểu; chạy golden trên giá trị mẫu. |
| Expected | Cả hai có `violations=[]`, `wrapperSupported=true`, cờ DB true; mã sinh ra có cấu trúc node và serialize đúng fixture, không báo `HRN-V03` hoặc thiếu class node. |
| Cleanup | Xoá bản sao/artifact |
| Evidence | TypeNode, P5/DB, mã sinh và output golden |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC32-01 — Kind ngoài lược đồ là cảnh báo mềm

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-32; DD validation F3 Bảng 1 `HRN-V01`, API F3 §6.3 |
| Kind / priority | Abnormal, P0: không chặn tác giả nhưng tắt Wrapper |
| Environment | P5, F2 SaveProblemSpec, PostgreSQL |
| Preconditions | Bản sao `P-SPEC` chưa có chữ ký khác; gọi thẳng API để gửi kind chưa thuộc JSON Schema FE |
| Test data | `returnType={"kind":"GRAPH"}`, các trường khác theo nền |
| Steps | Gọi P5, lưu qua F2, đọc response và cờ DB. |
| Expected | `violations=[]`, `wrapperSupported=false`, `unsupportedReasons` có `{code:HRN-V01,path:/returnType}`; lưu thành công và `function_wrapper_supported=false`, cảnh báo có lý do cho A2; không gọi codegen Wrapper. |
| Cleanup | Xoá bản sao |
| Evidence | Request/response P5/F2, DB |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC32-02 — LIST thiếu `of` là cảnh báo mềm

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-32; DD validation F3 Bảng 1 `HRN-V02` |
| Kind / priority | Abnormal, P0: phân biệt thiếu kiểu phần tử với JSON hỏng |
| Environment | P5, F2 SaveProblemSpec, PostgreSQL |
| Preconditions | Bản sao `P-SPEC` độc lập |
| Test data | `returnType={"kind":"LIST"}` không có khoá `of` |
| Steps | Gọi P5, lưu, đọc cờ DB và cảnh báo. |
| Expected | `violations=[]`, `wrapperSupported=false`, `unsupportedReasons` có `{code:HRN-V02,path:/returnType/of}`; F2 lưu thành công, cờ false, cảnh báo nêu thiếu kiểu phần tử. Không thay thế TypeNode bằng `OTHER`. |
| Cleanup | Xoá bản sao |
| Evidence | P5/F2 response, DB raw JSON |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC32-03 — LIST của ARRAY là tổ hợp ngoài lược đồ

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-32; DD validation F3 Bảng 1 `HRN-V03` |
| Kind / priority | Abnormal, P0: tổ hợp ngoài tập đóng không làm lỗi lưu |
| Environment | P5, F2 SaveProblemSpec, PostgreSQL |
| Preconditions | Bản sao `P-SPEC` độc lập |
| Test data | `returnType=LIST<ARRAY<INT,1>>`, tức `{"kind":"LIST","of":{"kind":"ARRAY","of":{"kind":"INT"},"dimensions":1}}` |
| Steps | Gọi P5, lưu, đọc DB và cảnh báo. |
| Expected | `violations=[]`, `wrapperSupported=false`, `unsupportedReasons` có `HRN-V03` tại `/returnType/of`; F2 lưu đúng cấu trúc, cờ false; không sinh Wrapper. |
| Cleanup | Xoá bản sao |
| Evidence | P5/F2 response và DB JSON |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC32-04 — Hai linked list khác kiểu node là cảnh báo mềm

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-32; DD validation F3 Bảng 1 `HRN-V06` |
| Kind / priority | Abnormal, P1: một chương trình chỉ sinh một lớp `ListNode` |
| Environment | P5, F2 SaveProblemSpec, PostgreSQL |
| Preconditions | Bản sao `P-SPEC` độc lập; các trường khác theo đặc tả nền |
| Test data | `returnType=LINKED_LIST<INT>`; `parameters=[{"name":"items","type":{"kind":"LINKED_LIST","of":{"kind":"LONG"}}}]` |
| Steps | Gọi P5, lưu qua F2, đọc lại `return_type`, `parameters` và cờ DB. |
| Expected | `violations=[]`, `wrapperSupported=false`, `unsupportedReasons` có `HRN-V06` tại `/parameters/0/type` hoặc đường dẫn node xung đột được evaluator xác định; API lưu thành công và giữ nguyên hai kiểu node, cờ false, không sinh Wrapper. |
| Cleanup | Xoá bản sao |
| Evidence | P5/F2 response, raw JSONB của hai kiểu và cờ DB |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC32-05 — TypeNode sai hình dạng bị từ chối lúc lưu

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-32; DD validation F3 Bảng 1 `HRN-V10`, ranh giới mềm/cứng |
| Kind / priority | Abnormal, P0: JSON hỏng không được xem là kiểu ngoài lược đồ có thể lưu |
| Environment | P5, F2 SaveProblemSpec, PostgreSQL/Outbox/Redis |
| Preconditions | Bản sao `P-SPEC` độc lập tại `spec_version=3`; chụp DB/cache/Outbox trước request |
| Test data | `returnType={"kind":"ARRAY","of":{"kind":"INT"},"dimensions":0}`; các trường khác theo đặc tả nền |
| Steps | Gọi P5 rồi PUT lưu đặc tả; so trạng thái DB/cache/Outbox trước–sau. |
| Expected | P5 có violation `HRN-V10` tại `/returnType/dimensions`; F2 trả `422 PRB-036` với violation tương ứng. Spec, cờ hỗ trợ, version, cache và Outbox không đổi; không chuyển thành cảnh báo mềm. |
| Cleanup | Xoá bản sao |
| Evidence | Request, P5/F2 response, DB/cache/Outbox delta |
| Design review | Đã review; chưa thực thi |
### TC-F3-AC33-01 — ARRAY bốn chiều vượt giới hạn mềm

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-33; DD validation F3 Bảng 1 `HRN-V04` |
| Kind / priority | Boundary, P1: 3 nhận, 4 chỉ Standard I/O |
| Environment | P5, F2 SaveProblemSpec, PostgreSQL |
| Preconditions | Bản sao `P-SPEC` độc lập; đối chứng ARRAY 3 chiều ở AC31-02 |
| Test data | `returnType=ARRAY<INT,4>` |
| Steps | Gọi P5, lưu, đọc cờ và JSON. |
| Expected | `violations=[]`, `wrapperSupported=false`, reason `HRN-V04` tại `/returnType/dimensions`; F2 vẫn lưu `dimensions=4` và cờ false, không đổi thành 3. |
| Cleanup | Xoá bản sao |
| Evidence | P5/F2 response, DB JSON |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC33-02 — LIST bốn lớp vượt độ sâu mềm

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-33; DD validation F3 Bảng 1 `HRN-V05` |
| Kind / priority | Boundary, P1: 3 nhận, 4 chỉ Standard I/O |
| Environment | P5, F2 SaveProblemSpec, PostgreSQL |
| Preconditions | Bản sao `P-SPEC` độc lập; đối chứng 3 lớp ở AC31-03 |
| Test data | `returnType=LIST<LIST<LIST<LIST<INT>>>>` |
| Steps | Gọi P5, lưu, đọc cờ và JSON. |
| Expected | `violations=[]`, `wrapperSupported=false`, reason `HRN-V05` với path trong `/returnType`; F2 giữ đủ 4 lớp trong JSON, cờ false. |
| Cleanup | Xoá bản sao |
| Evidence | P5/F2 response và DB JSON |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC34-01 — Lưu và mở lại TypeNode ngoài lược đồ không thêm OTHER

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-34; DD validation F3 §2, database F3 quy tắc TypeNode |
| Kind / priority | Recovery, P1: round-trip không biến dạng bản nháp tác giả |
| Environment | F2 SaveProblemSpec/GetProblemSpec, PostgreSQL, P5 |
| Preconditions | Bản sao `P-SPEC` độc lập, `function_wrapper_supported=false` ban đầu |
| Test data | `returnType={"kind":"GRAPH"}`, `functionName="solve"`, `parameters=[]`; `matchingStrategy=EXACT` |
| Steps | Lưu qua F2, mở lại bằng API và truy vấn raw JSONB, so với request. |
| Expected | Lưu thành công, `return_type` vẫn chính xác `{"kind":"GRAPH"}` cả API và DB; không có `kind=OTHER` hoặc chuỗi placeholder, `function_wrapper_supported=false`; `unsupportedReasons` nêu `HRN-V01`. |
| Cleanup | Xoá bản sao |
| Evidence | Request, save/get response, raw JSONB và cờ DB |
| Design review | Đã review; chưa thực thi |

