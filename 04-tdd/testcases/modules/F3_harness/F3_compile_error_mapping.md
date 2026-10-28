# Test case QA — F3: ánh xạ lỗi compiler P3

> 13 ca thiết kế cho AC-14–22; chưa thực thi. Nguồn: TDD F3 AC-14–22; DD F3 API §6.1, logic §6.1–6.5. Bộ mẫu stderr trong ca kiểm parser và phân loại độc lập; khi image go-judge có sẵn, phải biên dịch source gây lỗi thật ở ba ngôn ngữ để đối chiếu regex của DD §10.3. Không đánh dấu Đạt chỉ bằng mẫu stderr tự dựng.

## Fixture chung

- `W-LANG`: `PreparedProgram` Wrapper từ P2 cho bài `twoSum`, `userLineCount=7`. Lấy `P=prefixLineCount`, `C=callSiteStartLine` **từ gói thật của từng ngôn ngữ**, không giả định prefix giống nhau. Các diagnostic mẫu trỏ đến `P+1`, `P+4`, `P+7`, `C`, hoặc dòng epilogue ngoài `[C,callSiteEndLine]`. `signatureText` lấy nguyên từ `PreparedProgram`; `correlationId=qa-f3-map-001`.
- `S-LANG`: `PreparedProgram` Standard I/O, `prefixLineCount=0`, `userLineCount=7`, call-site null. Tất cả ca so `CompileErrorReport` và kiểm log nội bộ. `rawCompilerOutput` không được trả trong response. `message` người học chỉ giữ nguyên phần văn bản compiler sau bước làm sạch đường dẫn, tên tệp, phiên bản và định danh `apH`/`ApH`/`@@APH`.
- Với test parser, dùng dòng tiêu đề đúng regex DD §6.2: Java `Solution.java:L: error: msg`; C++ `main.cpp:L:K: error: msg`; Python `File "main.py", line L` rồi `SyntaxError: msg` hoặc `IndentationError: msg`. `L` là số nguyên tính từ `P`; `K` là cột. Chạy lại trên output compiler thật của image để xác nhận format phụ thuộc phiên bản.

| AC | Nhánh | ID | Review | Thực thi |
| :--- | :--- | :--- | :--- | :--- |
| 14 | Java đầu/cuối/lồng | TC-F3-AC14-01 | Đã review | Chưa thực hiện |
| 15 | C++ đầu/cuối/lồng và cột | TC-F3-AC15-01 | Đã review | Chưa thực hiện |
| 16 | Python cú pháp/thụt lề đầu/cuối/lồng | TC-F3-AC16-01 | Đã review | Chưa thực hiện |
| 17 | Lỗi prefix/epilogue, output không parse được và log kín | TC-F3-AC17-01 đến -03 | Đã review | Chưa thực hiện |
| 18 | Sai chữ ký ở call-site | TC-F3-AC18-01 | Đã review | Chưa thực hiện |
| 19 | Thiếu/thừa `}` báo ngoài vùng | TC-F3-AC19-01 | Đã review | Chưa thực hiện |
| 20 | USER lẫn HARNESS, lặp, thứ tự và trần 10 | TC-F3-AC20-01 đến -02 | Đã review | Chưa thực hiện |
| 21 | Standard I/O EOF và thiếu main | TC-F3-AC21-01 đến -02 | Đã review | Chưa thực hiện |
| 22 | C++ template notes | TC-F3-AC22-01 | Đã review | Chưa thực hiện |

### TC-F3-AC14-01 — Java quy dòng đầu, cuối và lồng của người học

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-14; DD F3 logic §6.2–6.3, API §6.1 |
| Kind / priority | Normal, P0: chẩn đoán Java đúng dòng |
| Environment | P3 parser và javac thật trong image |
| Preconditions | `W-JAVA`; tạo ba bản mã bảy dòng độc lập, mỗi bản có một lỗi ở dòng 1, 7, hoặc dòng 4 trong vòng lặp lồng; không có lỗi khác của người học |
| Test data | Ba stderr mẫu `Solution.java:(P+1): error: cannot find symbol`, `Solution.java:(P+7): error: cannot find symbol`, `Solution.java:(P+4): error: cannot find symbol`; thay `P` bằng giá trị thực trước khi gọi P3 |
| Steps | Với từng bản: compile thật, lưu stderr; gọi P3 bằng lineMap cùng gói; chạy lại ba mẫu stderr để tách parser khỏi compiler. |
| Expected | Mỗi báo cáo `USER_ERRORS`, đúng một diagnostic `line=1`, `7`, `4` tương ứng; message giữ văn bản `cannot find symbol` của mẫu sau làm sạch, không cộng `P`; không trả dòng lỗi thuộc epilogue nếu compiler phát thêm. Raw stderr chỉ ở log nội bộ. |
| Cleanup | Xoá artifact/cache |
| Evidence | Ba source đánh số dòng, stderr thật/mẫu, lineMap và report |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC15-01 — C++ giữ số cột khi trừ prefix

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-15; DD F3 logic §6.2–6.3, API §6.1 |
| Kind / priority | Normal, P0: dòng/cột C++ chỉ đúng vị trí người học |
| Environment | P3 và g++ thật trong image |
| Preconditions | `W-CPP`; ba source bảy dòng gây lỗi lần lượt ở dòng 1, 7, 4 trong khối lồng |
| Test data | Ba header `main.cpp:(P+1):3: error: unknown name`, `main.cpp:(P+7):9: error: unknown name`, `main.cpp:(P+4):13: error: unknown name` |
| Steps | Compile thật từng source; gọi P3 bằng stderr thật rồi bằng từng mẫu cùng lineMap. |
| Expected | Ba report mẫu `USER_ERRORS`, đúng một diagnostic `(line,column)` lần lượt `(1,3)`, `(7,9)`, `(4,13)`; số cột không trừ `P`. Với stderr thật, diagnostic trỏ dòng/cột lỗi đúng theo source, không lộ đường dẫn. |
| Cleanup | Xoá artifact/cache |
| Evidence | Source, stderr thật/mẫu, report và compiler version nội bộ |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC16-01 — Python ánh xạ SyntaxError và IndentationError

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-16; DD F3 logic §4.4, §6.2–6.3 |
| Kind / priority | Normal, P0: lỗi Python không lệch thụt lề |
| Environment | P3 và py_compile thật trong image |
| Preconditions | `W-PYTHON`; ba source bảy dòng: SyntaxError dòng 1, SyntaxError dòng 7, IndentationError dòng 4 trong khối lồng; P2 không re-indent |
| Test data | Ba block `File "main.py", line P+1` + `SyntaxError: invalid syntax`; `File "main.py", line P+7` + `SyntaxError: invalid syntax`; `File "main.py", line P+4` + `IndentationError: unexpected indent`, điền số thật |
| Steps | Kiểm cú pháp từng source thật; gọi P3 với stderr thật và block mẫu. |
| Expected | Ba report mẫu `USER_ERRORS`, một diagnostic có line lần lượt 1, 7, 4; không thêm mức thụt lề nhân tạo. Message giữ loại lỗi và lời compiler sau làm sạch; stderr thật được ánh xạ nếu phiên bản image báo cùng vùng. |
| Cleanup | Xoá artifact/cache |
| Evidence | Source, stderr thật/mẫu, lineMap và report |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC17-01 — Lỗi chỉ ở prefix trả thông điệp hệ thống cố định

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-17; DD F3 logic §6.3/§6.5, API §6.1 |
| Kind / priority | Security, P0: không lộ mã khung |
| Environment | P3 và log sink nội bộ |
| Preconditions | `W-JAVA`, `P>=1`; không có diagnostic vùng người học |
| Test data | `Solution.java:1: error: /tmp/build/ApHRunner internal failure`, `correlationId=qa-f3-map-001` |
| Steps | Gọi P3; kiểm DTO người học và log ERROR bằng correlationId. |
| Expected | `HARNESS_INTERNAL`, `diagnostics=[]`, `fixedMessageKey=harness.compile.internal`; thông điệp hiển thị cố định đúng DD, không có `ApHRunner`, `/tmp/build`, source khung, filename hay compiler version. Raw output đầy đủ chỉ nằm trong log nội bộ ERROR gắn correlationId. |
| Cleanup | Dọn log fixture theo retention test |
| Evidence | Report người học và log nội bộ đã giới hạn quyền |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC17-02 — Lỗi chỉ ở epilogue ngoài call-site cũng bị che

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-17; DD F3 logic §6.3/§6.5 |
| Kind / priority | Security, P0: phần cuối khung không lộ |
| Environment | P3 và log sink nội bộ |
| Preconditions | `W-CPP`; chọn `L > P+7` và `L` nằm ngoài `[callSiteStartLine,callSiteEndLine]` từ lineMap thật |
| Test data | `main.cpp:L:2: error: ApHInput /work/private/main.cpp` |
| Steps | Gọi P3; so report và log. |
| Expected | `HARNESS_INTERNAL`, không diagnostic, `harness.compile.internal`; không có `ApHInput`, đường dẫn hay dòng epilogue trong response. Toàn văn stderr nằm ở log ERROR nội bộ. |
| Cleanup | Dọn fixture |
| Evidence | LineMap, report và log |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC17-03 — Output compiler không parse được chỉ trả thông điệp hệ thống

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-17; DD F3 API §6.1 test viewpoints, logic §6.2–6.3 |
| Kind / priority | Abnormal + Security, P0: stderr lạ không được lộ nguyên văn |
| Environment | P3, log sink nội bộ |
| Preconditions | `W-JAVA` có lineMap hợp lệ, `correlationId=qa-f3-unparsed-001`; không có header nào khớp regex Java/C++/Python |
| Test data | `rawCompilerOutput="compiler panic /tmp/build/ApHRunner version 99\n"` |
| Steps | Gọi P3 với output trên; kiểm report, nội dung thông báo người học và log theo correlationId. |
| Expected | `HARNESS_INTERNAL`, `diagnostics=[]`, `fixedMessageKey=harness.compile.internal`; response và thông báo người học không chứa raw text, đường dẫn, `ApHRunner` hoặc version. Log ERROR nội bộ giữ raw text với correlationId để chẩn đoán. |
| Cleanup | Dọn log fixture theo retention test |
| Evidence | Request, report, thông báo người học và log nội bộ đã giới hạn quyền |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC18-01 — Sai tên hoặc số tham số tại call-site dùng chữ ký từ gói

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-18; DD F3 logic §6.3–6.4, API §6.1 |
| Kind / priority | Normal, P0: lỗi học viên không bị coi là lỗi hệ thống |
| Environment | P3 và compiler ba ngôn ngữ |
| Preconditions | Ba `W-LANG` cùng spec `twoSum`; source đúng cú pháp nhưng tên hàm là `otherName` hoặc thiếu tham số, gây lỗi tại call-site; `functionSignatureText=PreparedProgram.signatureText` từng ngôn ngữ |
| Test data | Một diagnostic mức error ở dòng `C` của từng ngôn ngữ; không có USER diagnostic |
| Steps | Compile source thật và gọi P3 bằng stderr; thử thêm block mẫu đúng regex với line `C`. |
| Expected | `USER_ERRORS`, `diagnostics=[]`, `fixedMessageKey=harness.compile.signatureMismatch`; thông điệp yêu cầu kiểm tên, số tham số, kiểu, chứa đúng `signatureText` của ngôn ngữ từ gói; không chứa source hoặc tên biến nội bộ tại call-site. |
| Cleanup | Xoá artifact/cache |
| Evidence | Ba gói P2, source, stderr và report |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC19-01 — Thiếu hoặc thừa dấu đóng khối báo tại epilogue

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-19; DD F3 logic §6.3–6.4 bảng 5 |
| Kind / priority | Abnormal, P1: gợi ý cố định cho lỗi phá cấu trúc |
| Environment | P3 và Java/C++ compiler thật |
| Preconditions | Bốn source độc lập: Java/C++ × thiếu `}` cuối vùng người học hoặc thừa `}`; chỉ chọn bản stderr thật có vị trí lỗi nằm ở epilogue ngoài call-site để kiểm nhánh này |
| Test data | Bốn diagnostic ở `L > P+7`, ngoài `[callSiteStartLine,callSiteEndLine]`, message compiler tùy tình huống |
| Steps | Compile từng source, xác nhận vị trí lỗi; gọi P3 bằng stderr thật và block mẫu ở dòng epilogue. |
| Expected | Nhánh lỗi ngoài vùng trả `HARNESS_INTERNAL`, `diagnostics=[]`, `harness.compile.internal` với lời gợi ý cố định kiểm thiếu/thừa dấu đóng khối; không trỏ số dòng epilogue hoặc trích mã khung. Nếu compiler báo trong vùng người học, kết quả theo quy tắc USER ở §6.3 và lưu đó là nhánh riêng của output thực, không ép thành HARNESS. |
| Cleanup | Xoá artifact/cache |
| Evidence | Bốn source, compiler output thật, vị trí đã phân loại và report |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC20-01 — Hai lỗi USER, một lỗi lặp và ba lỗi HARNESS

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-20; DD F3 logic §6.3 |
| Kind / priority | Normal + security, P0: ưu tiên lỗi người học và che lỗi khung |
| Environment | P3 và log sink nội bộ |
| Preconditions | `W-JAVA`, `P>=1`; mọi block dùng đúng regex javac |
| Test data | Sáu block: USER `P+6` message `cannot find symbol B`; USER `P+2` message `cannot find symbol A` lặp hai lần; HARNESS dòng `1` và hai dòng epilogue ngoài call-site; đưa vào theo thứ tự trộn |
| Steps | Gọi P3; kiểm report, thứ tự và log. |
| Expected | Bộ sáu block trả `USER_ERRORS`, đúng hai diagnostics ở dòng 2 rồi 6, không lặp message A, không có ba lỗi khung trong response; lỗi khung vẫn ở log nội bộ. |
| Cleanup | Dọn fixture log |
| Evidence | Raw blocks, report và log |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC20-02 — Trần 10 diagnostic sau khi sắp theo dòng

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-20; DD F3 API §6.1 và logic §6.3 |
| Kind / priority | Boundary, P1: giới hạn báo lỗi ổn định khi compiler trả nhiều lỗi |
| Environment | P3 parser với `W-JAVA` và log sink nội bộ |
| Preconditions | `W-JAVA` có `userLineCount=12`, `P=prefixLineCount`; mỗi block lỗi thuộc vùng người học, không có lỗi HARNESS |
| Test data | 12 header Java ở dòng tuyệt đối `P+12` xuống `P+1`, message lần lượt `error-12` xuống `error-01`; mỗi cặp `(line,message)` khác nhau |
| Steps | Gọi P3 một lần với 12 block theo thứ tự giảm dần; đọc toàn bộ `diagnostics[]` và log. |
| Expected | `USER_ERRORS`, đúng 10 phần tử cho dòng người học 1..10 tăng dần với message tương ứng `error-01`..`error-10`; dòng 11 và 12 không có trong response; không có raw compiler output trong response. |
| Cleanup | Dọn log fixture |
| Evidence | 12 block đầu vào, lineMap và report |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC21-01 — Standard I/O ép lỗi EOF về dòng cuối của người học

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-21; DD F3 logic §6.3 bước 1 |
| Kind / priority | Boundary, P0: Standard I/O không có lỗi khung |
| Environment | P3 và compiler Java/C++ thật |
| Preconditions | `S-JAVA` và `S-CPP`, `userLineCount=7`; source lỗi ở dòng 7 và source thiếu dấu đóng khối khiến compiler báo dòng 8 |
| Test data | Header compiler mức error lần lượt `L=7` và `L=8` theo regex từng ngôn ngữ |
| Steps | Compile thật; gọi P3 bằng stderr thật rồi bằng block mẫu hai vị trí. |
| Expected | Cả hai trường hợp `USER_ERRORS`; diagnostic dòng 7 kể cả khi raw line=8; không `HARNESS_INTERNAL`, không số dòng 8 trong report. |
| Cleanup | Xoá artifact/cache |
| Evidence | Source, stderr, lineMap và report |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC21-02 — C++ Standard I/O thiếu main có thông báo riêng

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-21; DD F3 logic §6.3 bước 3 |
| Kind / priority | Abnormal, P1: lỗi liên kết không có dòng |
| Environment | P3 và g++ linker thật |
| Preconditions | `S-CPP`; source bảy dòng hợp lệ cú pháp nhưng không khai `main` |
| Test data | Stderr chứa `undefined reference to 'main'` không có file:line theo regex g++ |
| Steps | Compile/link thật và gọi P3; so với mẫu không có line. |
| Expected | `USER_ERRORS`, `diagnostics=[]`, `fixedMessageKey=harness.compile.noMain`, hiển thị "Chương trình chưa có hàm `main`."; không lộ đường dẫn linker. |
| Cleanup | Xoá artifact/cache |
| Evidence | Source, raw linker output và report |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC22-01 — C++ template note của khung bị bỏ

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-22; DD F3 logic §6.2/§6.4 bảng 5/§6.5 |
| Kind / priority | Security, P0: note template không rò mã khung |
| Environment | P3 và g++ thật trong image |
| Preconditions | `W-CPP`; source gây lỗi thể hiện mẫu tại dòng người học 4; raw output có lỗi chính ở `P+4`, note người học ở `P+3`, note và `required from` ở epilogue |
| Test data | Error header `main.cpp:(P+4):9: error: invalid conversion`; note `main.cpp:(P+3):5: note: candidate`; note `main.cpp:L:2: note: ApHRunner` với L ngoài vùng; dòng `required from /tmp/build/main.cpp:L` |
| Steps | Compile fixture thật; gọi P3 bằng stderr thật và block mẫu; kiểm report và log. |
| Expected | `USER_ERRORS`; `diagnostics[]` có phần tử riêng cho note người học `{line:3,column:5,message:"note: candidate"}` và lỗi chính `{line:4,column:9,message:"invalid conversion"}`, theo thứ tự dòng 3 rồi 4. Note/required-from của khung bị bỏ khỏi response, không lộ `ApHRunner` hoặc `/tmp/build`. Lỗi chính quyết định phân loại, không đổi sang HARNESS do note dây chuyền; note đứng một mình không tạo `USER_ERRORS`. |
| Cleanup | Xoá artifact/cache |
| Evidence | Source, stderr thật/mẫu, report và log |
| Design review | Đã review theo quyết định chủ dự án 2026-10-27; chưa thực thi |
