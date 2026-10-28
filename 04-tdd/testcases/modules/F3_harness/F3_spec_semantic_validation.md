# Test case QA — F3: tên, ghi đè, chiến lược và chữ ký

> Trạng thái: 23 ca đã review thiết kế; chưa thực thi. Nguồn: `04-tdd/modules/F3_harness.md` AC-35–38; DD F3 validation Bảng 2–3; DD F3 API §6.3; DD F2 API §38.

## Fixture và oracle chung

- `P-SEM` là bản sao bài `UNPUBLISHED` của A2 có `PROBLEM_AUTHORING:UPDATE`, `spec_version=3`, `expectedUpdatedAt=T0=2026-10-26T03:00:00Z`. Đặc tả nền: `functionName="solve"`, `returnType={"kind":"INT"}`, `parameters=[{"name":"value","type":{"kind":"INT"}}]`, `nameOverrides={}`, `matchingStrategy="EXACT"`, `epsilonValue=null`. Mỗi ca dùng bản sao riêng và gửi nguyên các trường nền không đổi trừ trường ghi rõ.
- Lỗi cứng: P5 `violations` có mã/path cụ thể; F2 SaveProblemSpec trả `422 PRB-036` với `details.violations`; `problem_specs`, `problems.function_wrapper_supported`, `spec_version`, Redis spec key và Outbox không đổi. Các ca nhóm biến thể phải chạy từng biến thể trên bản sao riêng và giữ evidence từng response.
- Ca thành công: F2 trả `200`, so `spec`/cờ DB và P1/P2 mã khung cho ba ngôn ngữ. Xoá bài fixture, Outbox và cache sau ca. Không đánh dấu Đạt/Trượt trước khi có run record.

| AC | Nhánh | Test case | Review | Thực thi |
| :--- | :--- | :--- | :--- | :--- |
| AC-35 | Định danh sai, dài, trùng, keyword, tiền tố khung, số tham số | TC-F3-AC35-01 đến -09 | Đã review | Chưa thực hiện |
| AC-36 | Khoá override lạ, từ khoá, tiền tố khung, ghi đè hợp lệ | TC-F3-AC36-01 đến -04 | Đã review | Chưa thực hiện |
| AC-37 | EPSILON thiếu/không dương, UNORDERED_SET cây/list liên kết, cặp hợp lệ, suy biến, epsilon dư | TC-F3-AC37-01 đến -08 | Đã review | Chưa thực hiện |
| AC-38 | Chưa khai chữ ký và chữ ký nửa vời | TC-F3-AC38-01 đến -02 | Đã review | Chưa thực hiện |

## Test case

### TC-F3-AC35-01 — Tên hàm bắt đầu bằng số bị chặn

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-35; DD validation F3 Bảng 2 `HRN-V11`; F2 API §38.4 |
| Kind / priority | Abnormal, P1: tên không tạo được định danh |
| Environment | P5, F2 API, PostgreSQL/Outbox/Redis |
| Preconditions | `P-SEM` theo fixture, chưa có thay đổi khác |
| Test data | `functionName="2solve"` |
| Steps | Gọi P5 rồi PUT §38 với bản nháp này; đối chiếu DB/cache/Outbox trước–sau. |
| Expected | `HRN-V11` tại `/functionName`; API `422 PRB-036`, không lưu hay phát event theo oracle chung. |
| Cleanup | Xoá fixture |
| Evidence | P5/API response, DB/cache/Outbox delta |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC35-02 — Ký tự đặc biệt trong tên tham số bị chặn

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-35; DD validation F3 Bảng 2 `HRN-V13` |
| Kind / priority | Abnormal, P1: tên tham số không hợp lệ |
| Environment | P5, F2 API, PostgreSQL/Outbox/Redis |
| Preconditions | Bản sao `P-SEM` |
| Test data | `parameters[0].name="bad-name"` |
| Steps | Gọi P5, PUT §38, kiểm bất biến từ chối. |
| Expected | `HRN-V13` tại `/parameters/0/name`, `422 PRB-036`; không lưu hay phát event. |
| Cleanup | Xoá fixture |
| Evidence | P5/API, DB/cache/Outbox delta |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC35-03 — Tên dài 65 ký tự bị chặn ở hàm và tham số

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-35; DD validation F3 §3 regex 1–64 ký tự, `HRN-V11/V13` |
| Kind / priority | Boundary, P1: chiều dài tối đa 64 |
| Environment | P5, F2 API, PostgreSQL/Outbox/Redis |
| Preconditions | Hai bản sao `P-SEM` độc lập |
| Test data | Biến thể A `functionName="a"` lặp 65 lần; biến thể B `parameters[0].name="a"` lặp 65 lần; đo độ dài trước gửi |
| Steps | Với từng biến thể: gọi P5 và PUT §38, ghi response/DB riêng. |
| Expected | A `HRN-V11` tại `/functionName`; B `HRN-V13` tại `/parameters/0/name`; cả hai `422 PRB-036`, không ghi/Outbox. |
| Cleanup | Xoá hai fixture |
| Evidence | Hai payload/response, độ dài, DB/cache/Outbox delta |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC35-04 — Hai tên tham số suy ra trùng nhau bị chặn

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-35; DD validation F3 Bảng 2 `HRN-V14`, logic F3 §2 |
| Kind / priority | Abnormal, P0: tránh mã khung không biên dịch |
| Environment | P5, F2 API, PostgreSQL/Outbox/Redis |
| Preconditions | Bản sao `P-SEM` |
| Test data | `parameters=[{"name":"maxVal","type":{"kind":"INT"}},{"name":"max_val","type":{"kind":"INT"}}]` |
| Steps | Gọi P5 và PUT §38; đối chiếu DB/Outbox. |
| Expected | `HRN-V14` tại `/parameters/1/name` hoặc đường dẫn tham số trùng do evaluator chỉ ra; `422 PRB-036`, không lưu/Outbox. Bằng chứng ghi rõ ngôn ngữ mà hai tên suy ra trùng. |
| Cleanup | Xoá fixture |
| Evidence | P5/API, tên suy ra và DB delta |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC35-05 — Từ khoá của bất kỳ ngôn ngữ nào không là tên tham số

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-35; DD validation F3 Bảng 2 `HRN-V15` và danh sách keyword |
| Kind / priority | Abnormal, P0: bảo đảm ba plugin sinh được mã |
| Environment | P5, F2 API, PostgreSQL/Outbox/Redis |
| Preconditions | Ba bản sao `P-SEM` độc lập |
| Test data | Lần lượt `parameters[0].name="class"`, `"new"`, `"lambda"` |
| Steps | Với mỗi tên, gọi P5 và PUT §38, kiểm riêng response/DB. |
| Expected | Mỗi lần có `HRN-V15` tại `/parameters/0/name`, `422 PRB-036`, không lưu/Outbox; keyword của một ngôn ngữ cũng đủ chặn toàn chữ ký. |
| Cleanup | Xoá ba fixture |
| Evidence | Ba response, DB/cache/Outbox delta |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC35-06 — Tiền tố dành riêng của harness bị chặn

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-35; DD validation F3 §3 `apH`/`ApH`, `HRN-V12/V15` |
| Kind / priority | Security, P0: tên người học không đụng tên khung |
| Environment | P5, F2 API, PostgreSQL/Outbox/Redis |
| Preconditions | Hai bản sao `P-SEM` độc lập |
| Test data | A `functionName="apHsolve"`; B `parameters[0].name="ApHvalue"` |
| Steps | Gọi P5/PUT từng biến thể, so dữ liệu trước–sau. |
| Expected | A `HRN-V12` tại `/functionName`, B `HRN-V15` tại `/parameters/0/name`; cả hai `422 PRB-036`, không lưu/Outbox. |
| Cleanup | Xoá hai fixture |
| Evidence | Hai response và DB/cache/Outbox delta |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC35-07 — Mười tham số nhận, tham số thứ mười một bị chặn

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-35; DD validation F3 Bảng 2 `HRN-V16` |
| Kind / priority | Boundary, P1: số tham số tối đa 10 |
| Environment | P5, F2 API, PostgreSQL/Outbox |
| Preconditions | Hai bản sao `P-SEM` độc lập, mỗi bản khởi đầu `spec_version=3` |
| Test data | A: `parameters=[{"name":"p0","type":{"kind":"INT"}},...,{"name":"p9","type":{"kind":"INT"}}]` với chỉ số 0–9 liên tục; B: A cộng `{"name":"p10","type":{"kind":"INT"}}` |
| Steps | Gọi P5 và PUT §38 riêng cho A/B; đối chiếu DB/Outbox của từng bản. |
| Expected | A `violations=[]`, `wrapperSupported=true`, API `200`; B có `HRN-V16` tại `/parameters`, API `422 PRB-036`, spec/version/cờ/Outbox không đổi. |
| Cleanup | Xoá hai bản sao |
| Evidence | Hai payload đầy đủ, P5/API response, DB/Outbox delta |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC35-08 — `self` và tên trùng hàm bị chặn riêng

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-35; DD validation F3 Bảng 2 `HRN-V15` |
| Kind / priority | Abnormal, P1: tên tham số không phá mã Python hoặc lời gọi hàm |
| Environment | P5, F2 API, PostgreSQL/Outbox |
| Preconditions | Hai bản sao `P-SEM` độc lập; `functionName="solve"` |
| Test data | A: `parameters=[{"name":"self","type":{"kind":"INT"}}]`; B: `parameters=[{"name":"solve","type":{"kind":"INT"}}]` |
| Steps | Gọi P5 và PUT §38 cho từng bản, lưu evidence riêng. |
| Expected | Cả A và B có `HRN-V15` tại `/parameters/0/name`, API `422 PRB-036`, không ghi spec/version/cờ/Outbox. |
| Cleanup | Xoá hai bản sao |
| Evidence | Hai payload/response, DB/Outbox delta |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC35-09 — Tên hàm là keyword sau suy ra tên bị chặn

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-35; DD validation F3 Bảng 2 `HRN-V12` |
| Kind / priority | Abnormal, P1: từ khoá hàm không tạo được mã khung |
| Environment | P5, F2 API, PostgreSQL/Outbox |
| Preconditions | Bản sao `P-SEM` độc lập; các trường khác theo fixture nền |
| Test data | `functionName="class"`, `returnType=INT`, `parameters=[]`, `nameOverrides={}` |
| Steps | Gọi P5 rồi PUT §38; so DB/Outbox trước–sau. |
| Expected | `HRN-V12` tại `/functionName`, API `422 PRB-036`, spec/version/cờ/Outbox không đổi. |
| Cleanup | Xoá fixture |
| Evidence | Request, P5/API response, DB/Outbox delta |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC36-01 — Khoá override RUBY không hợp lệ

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-36; DD validation F3 Bảng 2 `HRN-V18` |
| Kind / priority | Abnormal, P1: giới hạn ba ngôn ngữ |
| Environment | P5, F2 API, PostgreSQL/Outbox |
| Preconditions | Bản sao `P-SEM` |
| Test data | `nameOverrides={"RUBY":"solve_ruby"}` |
| Steps | Gọi P5 và PUT §38. |
| Expected | `HRN-V18` tại `/nameOverrides/RUBY`, API `422 PRB-036`; không lưu/Outbox. |
| Cleanup | Xoá fixture |
| Evidence | Payload, P5/API, DB/Outbox delta |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC36-02 — Override Java bằng keyword bị chặn

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-36; DD validation F3 Bảng 2 `HRN-V19` |
| Kind / priority | Abnormal, P1: tên hàm sinh cho Java phải hợp lệ |
| Environment | P5, F2 API, PostgreSQL/Outbox |
| Preconditions | Bản sao `P-SEM` |
| Test data | `nameOverrides={"JAVA":"class"}` |
| Steps | Gọi P5 và PUT §38. |
| Expected | `HRN-V19` tại `/nameOverrides/JAVA`, `422 PRB-036`; không lưu/Outbox. |
| Cleanup | Xoá fixture |
| Evidence | P5/API, DB/Outbox delta |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC36-03 — Override Python hợp lệ chỉ đổi mã Python

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-36; DD validation F3 Bảng 2; logic F3 §2 |
| Kind / priority | Normal, P1: ghi đè đúng ngôn ngữ |
| Environment | P5, F2 API, P1/P2 golden Java/C++/Python, PostgreSQL |
| Preconditions | Bản sao `P-SEM` |
| Test data | `nameOverrides={"PYTHON":"solve_py"}` |
| Steps | Gọi P5, PUT §38, đọc lại spec và sinh starter/gói chấm ba ngôn ngữ. |
| Expected | `violations=[]`, `wrapperSupported=true`, API `200`, `spec.nameOverrides.PYTHON="solve_py"`; Python khai báo/gọi `solve_py`, Java/C++ giữ tên suy ra từ `solve`; cờ DB true, không có warning. |
| Cleanup | Xoá fixture/artifact |
| Evidence | P5/F2 response, DB, ba mã sinh |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC36-04 — Override dùng tiền tố dành riêng bị chặn

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-36; DD validation F3 Bảng 2 `HRN-V19` |
| Kind / priority | Abnormal, P1: ghi đè không chiếm tên của khung |
| Environment | P5, F2 API, PostgreSQL/Outbox |
| Preconditions | Bản sao `P-SEM` độc lập |
| Test data | `functionName="solve"`, `nameOverrides={"JAVA":"apHsolve"}`; các trường khác theo fixture nền |
| Steps | Gọi P5 và PUT §38; so DB/Outbox trước–sau. |
| Expected | `HRN-V19` tại `/nameOverrides/JAVA`, API `422 PRB-036`, spec/version/cờ/Outbox không đổi. |
| Cleanup | Xoá fixture |
| Evidence | Request, P5/API response, DB/Outbox delta |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC37-01 — EPSILON thiếu sai số bị chặn

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-37; DD validation F3 Bảng 3 `HRN-V20`; F2 API §38.4 |
| Kind / priority | Abnormal, P0: cấu hình so khớp không xác định |
| Environment | P5, F2 API, PostgreSQL/Outbox |
| Preconditions | Bản sao `P-SEM` |
| Test data | `matchingStrategy=EPSILON`, `epsilonValue=null`, `returnType={"kind":"DOUBLE"}` |
| Steps | Gọi P5 và PUT §38, kiểm DB/Outbox. |
| Expected | `HRN-V20` tại `/epsilonValue`, `422 PRB-036`, không lưu/Outbox. |
| Cleanup | Xoá fixture |
| Evidence | P5/API, DB/Outbox delta |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC37-02 — EPSILON bằng 0 hoặc âm bị chặn

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-37; DD validation F3 Bảng 3 `HRN-V20` |
| Kind / priority | Boundary, P0: epsilon phải dương |
| Environment | P5, F2 API, PostgreSQL/Outbox |
| Preconditions | Hai bản sao `P-SEM` độc lập |
| Test data | `matchingStrategy=EPSILON`, `returnType=DOUBLE`, `epsilonValue=0` và `-0.001` |
| Steps | Với từng giá trị, gọi P5/PUT, đối chiếu DB/Outbox. |
| Expected | Cả hai có `HRN-V20` tại `/epsilonValue`, `422 PRB-036`; không lưu/Outbox, không để DB CHECK là lớp báo lỗi duy nhất. |
| Cleanup | Xoá hai fixture |
| Evidence | Hai P5/API response, DB/Outbox delta |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC37-03 — UNORDERED_SET với cây bị chặn

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-37; DD validation F3 Bảng 3 `HRN-V22` |
| Kind / priority | Abnormal, P1: thứ tự cây có nghĩa |
| Environment | P5, F2 API, PostgreSQL/Outbox |
| Preconditions | Bản sao `P-SEM` |
| Test data | `matchingStrategy=UNORDERED_SET`, `returnType=BINARY_TREE<INT>` |
| Steps | Gọi P5 và PUT §38. |
| Expected | `HRN-V22` tại `/matchingStrategy` hoặc `/returnType`, `422 PRB-036`; không lưu/Outbox. |
| Cleanup | Xoá fixture |
| Evidence | P5/API, DB/Outbox delta |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC37-04 — UNORDERED_SET với linked list bị chặn

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-37; DD validation F3 Bảng 3 `HRN-V22` |
| Kind / priority | Abnormal, P1: thứ tự danh sách liên kết có nghĩa |
| Environment | P5, F2 API, PostgreSQL/Outbox |
| Preconditions | Bản sao `P-SEM` |
| Test data | `matchingStrategy=UNORDERED_SET`, `returnType=LINKED_LIST<INT>` |
| Steps | Gọi P5 và PUT §38. |
| Expected | `HRN-V22` tại `/matchingStrategy` hoặc `/returnType`, `422 PRB-036`; không lưu/Outbox. |
| Cleanup | Xoá fixture |
| Evidence | P5/API, DB/Outbox delta |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC37-05 — EPSILON dương với DOUBLE được lưu

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-37; DD validation F3 Bảng 3 |
| Kind / priority | Normal, P1: biên hợp lệ của EPSILON |
| Environment | P5, F2 API, PostgreSQL |
| Preconditions | Bản sao `P-SEM` |
| Test data | `matchingStrategy=EPSILON`, `epsilonValue=0.001`, `returnType=DOUBLE` |
| Steps | Gọi P5, PUT §38, đọc lại spec/cờ DB. |
| Expected | `violations=[]`, API `200`, `epsilonValue=0.001` được lưu, `functionWrapperSupported=true`, không warning; không nhận `HRN-V20`. |
| Cleanup | Xoá fixture |
| Evidence | P5/API response, DB |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC37-06 — UNORDERED_SET với LIST một chiều được lưu

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-37; DD validation F3 Bảng 3 |
| Kind / priority | Normal, P1: không chặn nhầm kiểu tập hợp hợp lệ |
| Environment | P5, F2 API, PostgreSQL |
| Preconditions | Bản sao `P-SEM` |
| Test data | `matchingStrategy=UNORDERED_SET`, `returnType=LIST<INT>`, `epsilonValue=null` |
| Steps | Gọi P5, PUT §38, đọc lại spec/cờ DB. |
| Expected | `violations=[]`, API `200`, chiến lược và TypeNode giữ nguyên, `functionWrapperSupported=true`, không warning; không nhận `HRN-V22`. |
| Cleanup | Xoá fixture |
| Evidence | P5/API response, DB |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC37-07 — Epsilon có giá trị với EXACT được lưu nhưng không tác dụng

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-37; DD validation F3 Bảng 3; logic F3 §7.2 |
| Kind / priority | Boundary, P1: không tự áp dụng sai số ngoài chiến lược EPSILON |
| Environment | P5, F2 API, P4, PostgreSQL |
| Preconditions | Bản sao `P-SEM` độc lập; `returnType=DOUBLE` |
| Test data | `matchingStrategy=EXACT`, `epsilonValue=0.001`; P4 dùng `model=STANDARD_IO`, `actualStdout="1.0005\n"`, `expectedOutput="1.0000\n"`, `resultSentinel=null` |
| Steps | Gọi P5, PUT §38, đọc lại spec; gọi P4 với `matchSpec` đã lưu. |
| Expected | P5 không có violation, F2 `200` và lưu `epsilonValue=0.001`; P4 `NO_MATCH` vì EXACT so chuỗi, không có `HRN-051`, không có expected/diff trong response. |
| Cleanup | Xoá bản sao |
| Evidence | P5/F2/P4 response và spec DB |
| Design review | Đã review; chưa thực thi |
### TC-F3-AC37-08 — Các tổ hợp suy biến hợp lệ không bị chặn

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-37; DD validation F3 §4, logic §7.2 |
| Kind / priority | Boundary, P1: chỉ chặn đúng tổ hợp bị cấm |
| Environment | P5, F2 API, P4, PostgreSQL |
| Preconditions | Hai bản sao `P-SEM` độc lập; mọi trường khác theo fixture nền |
| Test data | A: `returnType=INT`, `matchingStrategy=EPSILON`, `epsilonValue=0.001`; B: `returnType=INT`, `matchingStrategy=UNORDERED_SET`, `epsilonValue=null`; cả hai P4 dùng `model=FUNCTION_WRAPPER`, `resultSentinel="@@APH-RESULT@@"`, `actualStdout="@@APH-RESULT@@\n2\n"`, `expectedOutput="2\n"` |
| Steps | Gọi P5 và PUT §38 riêng A/B; gọi P4 với stdout có sentinel và cặp output đã nêu. |
| Expected | Cả hai P5 `violations=[]`, `wrapperSupported=true`, API `200`, P4 `MATCH`; không có `HRN-V22`/`HRN-051` vì INT suy biến thành so bằng chính xác. |
| Cleanup | Xoá hai fixture |
| Evidence | Hai payload, P5/F2/P4 response và DB |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC38-01 — Không khai chữ ký vẫn lưu Standard I/O

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-38; DD validation F3 Bảng 2 `HRN-V07`; F2 API §38.2–38.3 |
| Kind / priority | Normal, P1: cho lưu bản nháp chỉ có định dạng I/O |
| Environment | P5, F2 API, PostgreSQL |
| Preconditions | Bản sao `P-SEM` |
| Test data | `functionName=null`, `returnType=null`, `parameters=null`, `stdinFormatMd="một số nguyên"`, `stdoutFormatMd="một số nguyên"` |
| Steps | Gọi P5, PUT §38, đọc lại spec/cờ DB. |
| Expected | P5 `violations=[]`, `wrapperSupported=false`, reason `HRN-V07`; API `200` với warning `PRB-W01`/`reasonCode=HRN-V07`, DB cờ false, ba trường chữ ký vẫn null. |
| Cleanup | Xoá fixture |
| Evidence | P5/API response và DB |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC38-02 — Khai tên hàm nhưng thiếu kiểu trả về bị chặn

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-38; DD validation F3 Bảng 2 `HRN-V17`; F2 API §38.4 |
| Kind / priority | Abnormal, P1: không lưu chữ ký nửa vời |
| Environment | P5, F2 API, PostgreSQL/Outbox |
| Preconditions | Bản sao `P-SEM` |
| Test data | `functionName="solve"`, `returnType=null`, `parameters=[]` |
| Steps | Gọi P5 và PUT §38, đối chiếu DB/Outbox. |
| Expected | `HRN-V17` tại `/returnType`, `422 PRB-036`; không lưu spec mới, cờ/phiên bản/Outbox không đổi. |
| Cleanup | Xoá fixture |
| Evidence | P5/API response, DB/Outbox delta |
| Design review | Đã review; chưa thực thi |

