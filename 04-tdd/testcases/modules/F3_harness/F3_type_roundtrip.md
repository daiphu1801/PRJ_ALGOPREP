# Test case QA — F3: đọc và in TypeNode trên chuỗi công cụ thật

> 7 ca thiết kế cho AC-13; chưa thực thi. Nguồn: TDD F3 AC-13, DD F3 logic §3.1–3.4 và §10.3, API §2 P2-T01. Mỗi hàng fixture chạy **ba lần**, bằng Java, C++ và Python trong image/compiler thật. Hàm người học chỉ trả lại đối số đầu vào đúng kiểu; nguồn của hàm là fixture đã review riêng cho từng ngôn ngữ. So **byte stdout sau sentinel** với chuỗi mong đợi, kể cả LF cuối; stdout trước sentinel là phần debug và không dùng làm kết quả.

## Dữ liệu chung

- Mỗi hàng dùng một bài `PUBLISHED`, `FUNCTION_WRAPPER`, `specVersion=3`, một tham số `value` có cùng `TypeNode` với `returnType`, `matchingStrategy=EXACT`, `functionWrapperSupported=true`. F2 lưu `stdin_format_md` theo DD F3 §3.3 và `expected_output` theo §3.4. Đối với `LINKED_LIST`/`BINARY_TREE`, lớp node do harness sinh; hàm trả chính node đầu vào. Không trộn nhiều kiểu node trong cùng một spec.
- Gửi stdin đúng byte UTF-8 trong bảng; `\n` biểu thị byte LF, dấu ngoặc kép biểu thị ký tự trong chuỗi khi nằm bên trong backtick. Kiểm `PreparedProgram.resultSentinel="@@APH-RESULT@@"`, thoát 0, không có marker `@@APH-HARNESS-INPUT-ERROR@@` ở stderr. Mỗi ngôn ngữ phải cho cùng byte kết quả. Giữ compiler log, stdout/stderr thô, source và spec snapshot.
- Chuỗi mong đợi trong bảng là **phần sau dòng sentinel**, có LF cuối. Các dòng trống có ý nghĩa được hiển thị bằng `\n` trong ô. Nếu compiler/image chưa sẵn, trạng thái vẫn “Chưa thực hiện”; golden thuần không thay thế lượt chạy thật.

| AC | Nhánh | ID | Review | Thực thi |
| :--- | :--- | :--- | :--- | :--- |
| 13 | Sáu kiểu nguyên thuỷ và chuỗi | TC-F3-AC13-01 | Đã review | Chưa thực hiện |
| 13 | ARRAY 1D/2D/3D, rỗng | TC-F3-AC13-02 | Đã review | Chưa thực hiện |
| 13 | ARRAY<STRING> giữ khoảng trắng | TC-F3-AC13-03 | Đã review | Chưa thực hiện |
| 13 | LIST phẳng/lồng, rỗng | TC-F3-AC13-04 | Đã review | Chưa thực hiện |
| 13 | LIST<STRING> escape | TC-F3-AC13-05 | Đã review | Chưa thực hiện |
| 13 | LINKED_LIST thường/rỗng | TC-F3-AC13-06 | Đã review | Chưa thực hiện |
| 13 | BINARY_TREE rỗng và khác hình dạng | TC-F3-AC13-07 | Đã review | Chưa thực hiện |

### TC-F3-AC13-01 — Scalar giữ miền và chuẩn in

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-13; DD F3 logic §3.1–3.4/P2-T01 |
| Kind / priority | Normal, P0: sai serializer làm sai verdict |
| Environment | P2, Java/C++/Python compiler và sandbox thật |
| Preconditions | Mỗi hàng có spec một tham số/một kết quả cùng kiểu và hàm identity |
| Test data | `INT`: `-7\n` → `-7\n`; `LONG`: `2147483648\n` → `2147483648\n`; `DOUBLE`: `1.25\n` → `1.250000000000\n`; `BOOLEAN`: `true\n` → `true\n`, `false\n` → `false\n`; `CHAR`: `Z\n` → `Z\n`; `STRING`: ` a b \n` → ` a b \n`, dòng trống `\n` → `\n`. Tổng tám hàng × ba ngôn ngữ. |
| Steps | Với từng hàng, prepare, compile/run identity bằng stdin đã định; tách stdout tại sentinel và so byte phần kết quả. |
| Expected | 24 lượt thoát 0, mỗi kết quả đúng chuỗi bên phải; `LONG` không bị ép INT 32 bit, `DOUBLE` có đúng 12 chữ số thập phân, BOOLEAN chữ thường, STRING giữ khoảng trắng hai đầu và dòng trống. Sentinel xuất hiện đúng một dòng trước kết quả. |
| Cleanup | Xoá source/build artifact và cached files engine |
| Evidence | 24 bộ spec, source, compiler log và raw stdout/stderr |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC13-02 — Mảng một đến ba chiều đọc tiêu đề và in không tiêu đề

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-13; DD F3 logic §3.3 bảng 2/§3.4 bảng 3/P2-T01 |
| Kind / priority | Normal + boundary, P0: hình dạng mảng và dòng trống |
| Environment | P2 và chuỗi công cụ ba ngôn ngữ |
| Preconditions | Mỗi hàng có return/parameter đúng `ARRAY<INT,k>` |
| Test data | 1D: `3\n4 5 6\n` → `4 5 6\n`; 1D rỗng: `0\n\n` → `\n`; 2D: `2 2\n1 2\n3 4\n` → `1 2\n3 4\n`; 3D: `2 1 2\n1 2\n3 4\n` → `1 2\n\n3 4\n`. |
| Steps | Chạy bốn hàng × ba ngôn ngữ; so byte output sau sentinel và kiểm chương trình đã đọc đúng toàn bộ khối input. |
| Expected | 12 lượt thoát 0, kết quả đúng từng chuỗi; dòng tiêu đề kích thước không xuất hiện ở output; 3D có đúng một dòng trống giữa hai khối, 1D rỗng in một dòng trống. |
| Cleanup | Xoá artifact/cache |
| Evidence | 12 raw stdout, stdin, spec và compiler log |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC13-03 — Mảng STRING dùng một dòng cho mỗi phần tử

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-13; DD F3 logic §3.3 bảng 2/§3.4 bảng 3 |
| Kind / priority | Normal, P1: không tách STRING theo dấu cách |
| Environment | P2 và chuỗi công cụ ba ngôn ngữ |
| Preconditions | Spec `ARRAY<STRING,1>` identity |
| Test data | `3\na b\n\nx y\n` → `a b\n\nx y\n` |
| Steps | Chạy ba ngôn ngữ; kiểm input gồm một dòng tiêu đề, ba dòng phần tử (phần tử giữa rỗng). |
| Expected | Ba lượt thoát 0 và cùng output đúng byte; `a b`/`x y` không bị tách thành hai phần tử, phần tử rỗng giữ một dòng trống, không in tiêu đề `3`. |
| Cleanup | Xoá artifact/cache |
| Evidence | Ba raw stdout và source/spec |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC13-04 — LIST lồng có kích thước từng khối và ký pháp ngoặc vuông

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-13; DD F3 logic §3.3 bảng 2/§3.4 bảng 3 |
| Kind / priority | Normal + boundary, P0: LIST không đều và rỗng |
| Environment | P2 và chuỗi công cụ ba ngôn ngữ |
| Preconditions | Spec identity lần lượt `LIST<INT>` và `LIST<LIST<INT>>` |
| Test data | Phẳng: `3\n1 2 3\n` → `[1,2,3]\n`; phẳng rỗng: `0\n\n` → `[]\n`; lồng: `3\n2\n1 2\n1\n3\n0\n\n` → `[[1,2],[3],[]]\n`. |
| Steps | Chạy ba hàng × ba ngôn ngữ; so byte stdout sau sentinel. |
| Expected | Chín lượt thoát 0, không thêm khoảng trắng trong ngoặc, `LIST` con có kích thước riêng, `0` in `[]` chứ không chuỗi trống. |
| Cleanup | Xoá artifact/cache |
| Evidence | Chín raw stdout, stdin, source/spec |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC13-05 — LIST<STRING> bao nháy và escape nhất quán

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-13; DD F3 logic §3.3/§3.4 bảng 3 |
| Kind / priority | Boundary, P1: chuỗi có dấu nháy và backslash |
| Environment | P2 và chuỗi công cụ ba ngôn ngữ |
| Preconditions | Spec `LIST<STRING>` identity |
| Test data | stdin `3\na b\na"b\na\b\n` (hàng ba là ký tự `a`, backslash, `b`); phần kết quả chính xác `['a b','a"b','a\b']` sau khi đổi dấu nháy đơn bao chuỗi thành nháy kép và escape dấu nháy/backslash theo DD: `["a b","a\"b","a\\b"]\n` |
| Steps | Chạy ba ngôn ngữ; parse phần kết quả theo ngữ pháp LIST ở DD và so raw byte với golden UTF-8 trong `Test data`. |
| Expected | Ba lượt thoát 0, parse lại được đúng ba chuỗi ban đầu; raw byte bằng cùng golden, trong đó nháy kép và backslash được escape đúng một lần, không đổi dấu cách trong `a b`. |
| Cleanup | Xoá artifact/cache |
| Evidence | Ba stdout dạng hex, golden và roundtrip parse |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC13-06 — Linked list giữ thứ tự node, rỗng có dòng trống

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-13; DD F3 logic §3.1 null/rỗng, §3.3/§3.4 bảng 3 |
| Kind / priority | Normal + boundary, P0: linked list không được mất node |
| Environment | P2 và chuỗi công cụ ba ngôn ngữ |
| Preconditions | Spec `LINKED_LIST<INT>` identity; harness cung cấp `ListNode` theo ngôn ngữ |
| Test data | Thường: `3\n4 5 6\n` → `3\n4 5 6\n`; rỗng: `0\n\n` → `0\n\n` |
| Steps | Chạy hai hàng × ba ngôn ngữ; so byte stdout sau sentinel. |
| Expected | Sáu lượt thoát 0; thứ tự `val/next` 4→5→6 giữ nguyên; rỗng là null/None ở hàm identity nhưng được in đúng `0` rồi một dòng trống, không gây RE. |
| Cleanup | Xoá artifact/cache |
| Evidence | Sáu raw stdout, source/spec |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC13-07 — Cây rỗng và hai hình dạng khác nhau

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-13; DD F3 logic §3.1 null/rỗng, §3.3/§3.4 bảng 3 |
| Kind / priority | Normal + boundary, P0: không được làm mất hình dạng cây |
| Environment | P2 và chuỗi công cụ ba ngôn ngữ |
| Preconditions | Spec `BINARY_TREE<INT>` identity; harness cung cấp `TreeNode` theo ngôn ngữ |
| Test data | Rỗng: `#\n` → `#\n`; con trái: `1 2 # # #\n` → `1 2\n`; con phải: `1 # 2 # #\n` → `1 # 2\n`. |
| Steps | Chạy ba hàng × ba ngôn ngữ; so byte stdout sau sentinel, kiểm không liệt kê con của node null và cắt các `#` ở đuôi. |
| Expected | Chín lượt thoát 0; cây rỗng in `#`, hai cây cùng giá trị `{1,2}` cho hai chuỗi khác nhau đúng như bảng; không cắt `#` nằm giữa chuỗi con phải. |
| Cleanup | Xoá artifact/cache |
| Evidence | Chín raw stdout, source/spec và cây fixture |
| Design review | Đã review; chưa thực thi |
