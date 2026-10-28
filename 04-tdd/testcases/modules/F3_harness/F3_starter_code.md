# Test case QA — F3: mã khung khởi đầu P1

> Trạng thái: 8 ca đã review thiết kế; chưa thực thi. Nguồn: `04-tdd/modules/F3_harness.md` AC-01–05; DD F3 API §3; DD F3 logic §2, §4.5–4.6. Các ca codegen cần golden và compiler thật cho ba ngôn ngữ.

## Fixture chung

- `P-TWO`: bài `PUBLISHED`, `functionWrapperSupported=true`, `specVersion=7`, `functionName="two_sum"`, `returnType=ARRAY<INT,1>`, `parameters=[{"name":"nums","type":ARRAY<INT,1>},{"name":"target","type":INT}]`, `nameOverrides={}`. `P-STD`: bài `PUBLISHED` chỉ hỗ trợ Standard I/O do `returnType={"kind":"GRAPH"}` ngoài lược đồ. `P-NOSIG`: bài đã xuất bản có dòng đặc tả nhưng ba trường chữ ký đều null; dùng fault/fixture riêng nếu checklist xuất bản bình thường không cho trạng thái đó.
- Gọi `GET /api/v1/harness/problems/{problemId}/starter-code?language=...&model=...` bằng tài khoản có quyền đọc. `StarterCodeDto` có `language`, `submissionModel`, `sourceCode`; `specVersion` chỉ ở kết quả P1 nội bộ. Mỗi hàng ngôn ngữ chạy độc lập và lưu golden/compile log. Không chạy mã học viên trong F3.
- **Thứ tự lỗi AC-05 đã chốt 2026-10-27:** bài chưa khai chữ ký trả `HRN-005` trước khi kiểm cờ `functionWrapperSupported`; bài có chữ ký nhưng kiểu ngoài lược đồ trả `HRN-004`.

| AC | Nhánh | Test case | Review | Thực thi |
| :--- | :--- | :--- | :--- | :--- |
| AC-01 | twoSum ba ngôn ngữ; node list/tree | TC-F3-AC01-01 đến -02 | Đã review | Chưa thực hiện |
| AC-02 | Override Java | TC-F3-AC02-01 | Đã review | Chưa thực hiện |
| AC-03 | Standard I/O khi Wrapper bật/tắt | TC-F3-AC03-01 | Đã review | Chưa thực hiện |
| AC-04 | Wrapper bị chặn; Standard I/O vẫn có | TC-F3-AC04-01 đến -02 | Đã review | Chưa thực hiện |
| AC-05 | Không có spec; thiếu chữ ký | TC-F3-AC05-01 đến -02 | Đã review | Chưa thực hiện |

## Test case

### TC-F3-AC01-01 — two_sum sinh đúng tên, kiểu và thứ tự tham số ở ba ngôn ngữ

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-01; DD F3 API §3.3/3.6 P1-T01, logic §2/§4.6 |
| Kind / priority | Normal, P0: mã khung học viên khớp chữ ký chấm |
| Environment | P1 API, golden và compiler Java/C++/Python |
| Preconditions | `P-TWO` hợp lệ, người gọi đã đăng nhập |
| Test data | Ba request `language=JAVA`, `CPP`, `PYTHON`, đều `model=FUNCTION_WRAPPER` |
| Steps | Gọi P1 từng ngôn ngữ; so `sourceCode` với golden cùng specVersion; biên dịch/parse nguyên mã khung chưa sửa. |
| Expected | Cả ba `200`, `submissionModel=FUNCTION_WRAPPER`; Python khai `two_sum`, Java/C++ `twoSum`. Mỗi khung có đúng một hàm với `nums` trước `target`, kiểu mảng INT một chiều và INT theo ngôn ngữ; không có mã đọc stdin/in stdout trong starter. Cả ba compile/parse được khi chưa sửa; P1 nội bộ ghi `specVersion=7`. |
| Cleanup | Xoá artifact compiler |
| Evidence | Ba response, golden diff và compiler/parse log |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC01-02 — Starter cho linked list và cây có chú thích trường node

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-01; DD F3 API §3.6 P1-T03, logic §4.6 |
| Kind / priority | Normal, P1: người học hiểu cấu trúc node |
| Environment | P1 API, golden Java/C++/Python |
| Preconditions | Hai bản sao `P-TWO` độc lập thay `parameters[0].type` lần lượt `LINKED_LIST<INT>` và `BINARY_TREE<INT>`; returnType=INT |
| Test data | Sáu request: hai kiểu × ba ngôn ngữ, model Wrapper |
| Steps | Gọi P1 và so golden từng cặp kiểu/ngôn ngữ. |
| Expected | Mỗi response `200`, mã khung có chú thích mô tả `val/next` cho linked list hoặc `val/left/right` cho tree, không nhúng input/output testcase; không thiếu hoặc đổi thứ tự tham số. |
| Cleanup | Xoá bản sao/artifact |
| Evidence | Sáu response và golden diff |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC02-01 — Override Java không đổi Python/C++

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-02; DD F3 API §3.6 P1-T07, logic §2 |
| Kind / priority | Normal, P1: override đúng phạm vi |
| Environment | P1 API, golden Java/C++/Python |
| Preconditions | Bản sao `P-TWO` có `nameOverrides={"JAVA":"solve"}` |
| Test data | Ba request Wrapper Java/CPP/Python |
| Steps | Gọi P1 từng ngôn ngữ; so dòng khai báo với golden. |
| Expected | Java dùng `solve`, C++ dùng `twoSum`, Python dùng `two_sum`; kiểu và thứ tự tham số giữ nguyên. Không có tên Java `solve` trong mã hai ngôn ngữ còn lại. |
| Cleanup | Xoá fixture |
| Evidence | Ba response, spec và golden diff |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC03-01 — Standard I/O có starter cho cả bài hỗ trợ và không hỗ trợ Wrapper

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-03; DD F3 API §3.6 P1-T02, logic §4.5 |
| Kind / priority | Normal, P0: bài Standard I/O luôn dùng được |
| Environment | P1 API, compiler/parse ba ngôn ngữ |
| Preconditions | `P-TWO` và `P-STD` đã xuất bản, tài khoản đăng nhập |
| Test data | Sáu request = hai bài × JAVA/CPP/PYTHON, `model=STANDARD_IO` |
| Steps | Gọi P1 từng cặp; compile/parse starter nhận được. |
| Expected | Cả sáu `200`, chương trình tối thiểu có entrypoint/main theo ngôn ngữ, không phụ thuộc chữ ký hay `functionWrapperSupported`; không có mã đọc/in theo TypeNode của bài. Mã chưa sửa hợp lệ. |
| Cleanup | Xoá artifact |
| Evidence | Sáu response và compiler/parse log |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC04-01 — Bài chỉ Standard I/O từ chối Wrapper ở server

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-04; DD F3 API §3.4 `HRN-004`, P1-T04 |
| Kind / priority | Security, P0: không tin nút FE đã ẩn |
| Environment | P1 HTTP API |
| Preconditions | `P-STD.functionWrapperSupported=false` do kiểu ngoài lược đồ |
| Test data | GET trực tiếp `P-STD`, `language=PYTHON`, `model=FUNCTION_WRAPPER` |
| Steps | Gọi API bỏ qua FE. |
| Expected | `422 HRN-004`, thông điệp bài chỉ hỗ trợ Standard I/O; response không có `sourceCode` rỗng hay khung đoán. |
| Cleanup | Không có dữ liệu ghi |
| Evidence | Request/response |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC04-02 — Cùng bài vẫn trả Standard I/O

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-04; DD F3 API §3.6 P1-T04 |
| Kind / priority | Normal, P1: đường thay thế khả dụng |
| Environment | P1 HTTP API |
| Preconditions | `P-STD.functionWrapperSupported=false` |
| Test data | GET `P-STD`, `language=PYTHON`, `model=STANDARD_IO` |
| Steps | Gọi P1, kiểm mã trả về. |
| Expected | `200`, `submissionModel=STANDARD_IO`, `sourceCode` là starter Python tối thiểu hợp lệ; không trả `HRN-004`. |
| Cleanup | Không có dữ liệu ghi |
| Evidence | Request/response, parse log |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC05-01 — Bài không tồn tại trả 404, không có mã khung

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-05; DD F3 API §3.4 `HRN-001`, P1-T06 |
| Kind / priority | Abnormal, P1: không đoán đặc tả |
| Environment | P1 HTTP API, F2 reader |
| Preconditions | UUID `00000000-0000-4000-8000-000000000099` không tồn tại |
| Test data | GET Wrapper Java cho UUID trên |
| Steps | Gọi API, kiểm response và DB write count. |
| Expected | `404 HRN-001`, không có `sourceCode`/mã rỗng; không ghi DB. |
| Cleanup | Không có dữ liệu ghi |
| Evidence | Request/response, DB query |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC05-02 — Bài có spec nhưng chưa khai chữ ký trả HRN-005

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-05; DD F3 API §3.4 `HRN-005`, P1-T05 |
| Kind / priority | Abnormal, P1: không tạo khung Wrapper trống |
| Environment | P1 HTTP API, F2 spec reader |
| Preconditions | `P-NOSIG` có dòng spec với `functionName`, `returnType`, `parameters` đều null; F2 lưu hợp lệ với `functionWrapperSupported=false`; O2 cho đọc |
| Test data | GET Wrapper Java cho `P-NOSIG` |
| Steps | Gọi P1, kiểm response. |
| Expected | `422 HRN-005 SPEC_SIGNATURE_MISSING`, không có mã khung rỗng/đoán; không trả `HRN-004` dù `functionWrapperSupported=false`, vì kiểm chữ ký trước cờ hỗ trợ. Không ghi DB/cache mã khung. |
| Cleanup | Xoá fixture |
| Evidence | Spec/cờ DB, request/response và trace thứ tự kiểm |
| Design review | Đã review theo quyết định chủ dự án 2026-10-27; chưa thực thi |
