# Handoff golden P2-T01 — F3 `PreparedProgram`

Liên quan [TC-F3-AC06-02](F3_prepare_source_and_version.md), DD F3 API §2.6 P2-T01 và DD F3 logic §10. Đây là **điều kiện kiểm sau codegen**, không phải kết quả test đã chạy.

## Input cố định

- `problemId=P-TWO` của fixture cô lập; `specVersion=expectedSpecVersion=3`; `functionWrapperSupported=true`; `functionName=two_sum`, `parameters=[nums:ARRAY<INT,1>,target:INT]`, `returnType=ARRAY<INT,1>`, `matchingStrategy=EXACT`.
- `sourceCode=U7`: Java/C++ gồm bảy dòng `// u1` tới `// u7`; Python gồm bảy dòng `# u1` tới `# u7`; mỗi dòng kết thúc LF. Cùng `U7` cho hai mô hình của một ngôn ngữ.
- Giữ nguyên phiên bản template, serializer JSON UTF-8 và image go-judge trong cả sáu lần gọi. Ghi các phiên bản này cùng commit codegen trong manifest lúc tạo.

| Ngôn ngữ | Mô hình | Tệp golden cần tạo dưới `algoprep-harness/src/test/resources/golden/` | SHA-256 |
| :--- | :--- | :--- | :--- |
| JAVA | FUNCTION_WRAPPER | `p2-two-v3-java-wrapper.json` | Chờ codegen |
| JAVA | STANDARD_IO | `p2-two-v3-java-stdio.json` | Chờ codegen |
| CPP | FUNCTION_WRAPPER | `p2-two-v3-cpp-wrapper.json` | Chờ codegen |
| CPP | STANDARD_IO | `p2-two-v3-cpp-stdio.json` | Chờ codegen |
| PYTHON | FUNCTION_WRAPPER | `p2-two-v3-python-wrapper.json` | Chờ codegen |
| PYTHON | STANDARD_IO | `p2-two-v3-python-stdio.json` | Chờ codegen |

## Cổng phê duyệt sau coding

1. Gọi P2 đúng sáu command bằng serializer cố định; lưu từng `PreparedProgram`, phiên bản template/image và SHA-256. Kiểm độc lập `specVersion`, `language`, `model`, `payload.engineId`, `lineMap`, `signatureText`, `matchSpec`, `resultSentinel`, file/argv/env và không có input/expected-output testcase trong gói.
2. Review diff của cả sáu tệp golden trong PR codegen, xác nhận không chứa dữ liệu nhạy cảm và không lấy output của một implementation lỗi làm oracle một cách tự động. Ghi người duyệt, commit và sáu hash vào run record/manifest.
3. Sau khi duyệt, chạy so byte từng `PreparedProgram` với golden tương ứng. Chỉ lúc đó mới kết luận Đạt/Trượt cho điều kiện byte của AC06-02.

Hiện `05-coding/backend/algoprep-harness` chỉ có package placeholder; sáu tệp và hash chưa tồn tại. Mốc thiết kế đợt 1 theo dõi ngoại lệ này, còn cổng so byte thuộc mốc kiểm sau codegen.
