# Test case QA — F3: ghép mã, chuẩn hoá và ghim đặc tả P2

> 16 ca thiết kế cho AC-04 và AC-06–12; chưa thực thi. Nguồn: `04-tdd/modules/F3_harness.md` AC-04, AC-06–12; DD F3 API §2.1–2.6, logic §4.1–4.5. Mỗi ca chạy trên cả Java, C++ và Python khi nói “ba ngôn ngữ”. So nội dung nguồn bằng byte UTF-8 sau khi giải mã `ExecutionPlan.files[].content`, không dựa vào ảnh màn hình. Ca `TC-F3-AC06-02` có [handoff golden P2](F3_P2_golden_handoff.md) cho mốc sau codegen.

## Fixture và nhánh

- `P-TWO`: bài đã xuất bản, `specVersion=3`, `functionWrapperSupported=true`, hàm `two_sum(nums: ARRAY<INT,1>, target: INT) -> ARRAY<INT,1>`, `matchingStrategy=EXACT`; dùng plugin/golden riêng của Java, C++ và Python. `P-STD`: bài đã xuất bản chỉ dùng Standard I/O. Mỗi ca tạo bản sao độc lập của fixture.
- `U7` gồm đúng bảy dòng không rỗng, mỗi dòng có marker riêng `// u1`…`// u7` ở Java/C++, `# u1`…`# u7` ở Python, và kết thúc `\n`; `U10` tương tự gồm 10 dòng. Các marker không nằm trong template. Với ca yêu cầu biên dịch, thay marker bằng mã hợp lệ có cùng số dòng.
- Capture `PreparedProgram`, `ExecutionPlan`, `LineMap`, lệnh gọi `ProblemSpecReaderPort`, và số lần ghi DB/event. P2 là cổng nội bộ: `HRN-*` là mã lỗi có cấu trúc, **không suy diễn HTTP status**. Các ca lỗi yêu cầu không có `PreparedProgram` hay payload nửa vời.

| AC | Nhánh | ID | Review | Thực thi |
| :--- | :--- | :--- | :--- | :--- |
| 04 | P2 từ chối Wrapper khi bài chỉ hỗ trợ Standard I/O | TC-F3-AC04-03 | Đã review | Chưa thực hiện |
| 06 | Vị trí chèn, line map và golden 3×2 | TC-F3-AC06-01 đến -02 | Ca -02 chờ golden | Chưa thực hiện |
| 07 | Gọi lặp tất định từng byte | TC-F3-AC07-01 | Đã review | Chưa thực hiện |
| 08 | Neo/chỗ giữ chỗ; dấu đóng thừa | TC-F3-AC08-01 đến -02 | Đã review | Chưa thực hiện |
| 09 | CRLF/CR đơn; thiếu newline; BOM | TC-F3-AC09-01 đến -03 | Đã review | Chưa thực hiện |
| 10 | Trống, vượt ngưỡng, NUL, đúng ngưỡng | TC-F3-AC10-01 đến -04 | Đã review | Chưa thực hiện |
| 11 | Version lệch; version khớp | TC-F3-AC11-01 đến -02 | Đã review | Chưa thực hiện |
| 12 | Standard I/O không khung | TC-F3-AC12-01 | Đã review | Chưa thực hiện |

### TC-F3-AC04-03 — P2 từ chối Wrapper trước khi sinh gói

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-04; DD F3 API §2.4–2.6 P2-T03, logic BR-05 |
| Kind / priority | Abnormal, P1: không sinh gói chấm cho mô hình bài không hỗ trợ |
| Environment | P2 component, F2 spec reader và plugin spy |
| Preconditions | `P-STD` có `specVersion=3`, `functionWrapperSupported=false` do kiểu ngoài lược đồ; reader trả cùng view cho cả hai lời gọi |
| Test data | Hai `PrepareSubmissionCommand` Java, `sourceCode="class Main { public static void main(String[] args) {} }\n"`, `expectedSpecVersion=3`; lần đầu `model=FUNCTION_WRAPPER`, lần sau `model=STANDARD_IO` |
| Steps | Gọi P2 với Wrapper; kiểm mã lỗi, kết quả, số lần gọi plugin và artifact. Gọi lại với Standard I/O cho cùng bài và source. |
| Expected | Lần Wrapper trả `HRN-004`, không có `PreparedProgram`, `ExecutionPlan`, `JudgePayload` hoặc file sinh ra; không gọi plugin render/assembler, không ghi DB/event. Lần Standard I/O thành công, tệp nguồn bằng source đã cho, `lineMap.prefixLineCount=0`, `resultSentinel=null`; không có `HRN-004`. |
| Cleanup | Xoá artifact fixture nếu có |
| Evidence | Hai command/result, reader/plugin/assembler trace và artifact inventory trước–sau |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC06-01 — Vùng mã bảy dòng nằm giữa prefix và epilogue

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-06; DD F3 API §2.3.2/P2-T01, logic §4.3 |
| Kind / priority | Normal, P0: sai line map làm sai dòng lỗi compiler |
| Environment | P2 và golden template của ba plugin |
| Preconditions | `P-TWO`, plugin nạp thành công; mỗi ngôn ngữ dùng `U7` cùng cú pháp comment của mình |
| Test data | Ba command Wrapper, `expectedSpecVersion=3`, `sourceCode=U7` |
| Steps | Gọi P2 từng ngôn ngữ; lấy tệp nguồn và `lineMap`; đếm newline trong prefix rendered, xác định marker `u1`/`u7` và dòng epilogue đầu tiên. |
| Expected | Từng tệp có đúng một bản `U7`, giữ nguyên thứ tự bảy marker. `prefixLineCount` bằng số newline prefix thực; `userLineCount=7`; `u1` ở dòng `prefixLineCount+1`, `u7` ở `prefixLineCount+7`, dòng tiếp là đầu epilogue. `callSiteStartLine`/`EndLine` trỏ đúng khối gọi sau `READ_BLOCK` đã render; payload không chứa stdin testcase. |
| Cleanup | Xoá fixture/artifact |
| Evidence | Ba `PreparedProgram`, tệp nguồn đánh số dòng và golden diff |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC06-02 — Sáu gói P2 khớp golden đã duyệt

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-06; DD F3 API §2.3/P2-T01, logic §4–5 |
| Kind / priority | Normal, P0: phát hiện thay đổi template, lệnh và line map ngoài ý muốn |
| Environment | P2 component và serializer JSON/UTF-8 cố định; bộ golden được duyệt theo phiên bản template và image |
| Preconditions | `P-TWO` version 3, `U7` đúng bảy dòng như fixture chung; plugin Java/C++/Python nạp thành công; `engineId=GO_JUDGE`; không đổi template/config giữa các lượt |
| Test data | Sáu command là tích Descartes `{JAVA,CPP,PYTHON} × {FUNCTION_WRAPPER,STANDARD_IO}`, mỗi command có `problemId=P-TWO`, `expectedSpecVersion=3`, `sourceCode=U7` theo cú pháp comment từng ngôn ngữ. Golden đặt theo ID cố định `p2-two-v3-{java,cpp,python}-{wrapper,stdio}.json`; mỗi tệp phải ghi hash SHA-256 và phiên bản template/image khi được duyệt. |
| Steps | Gọi P2 cho sáu command; serialize từng `PreparedProgram` bằng cùng serializer; so byte với golden tương ứng, rồi kiểm riêng nội dung `payload`, `lineMap`, `signatureText`, `matchSpec`, `resultSentinel` và thứ tự file/argv/env. |
| Expected | Mỗi gói có `specVersion=3`, đúng `language`/`model`, `payload.engineId=GO_JUDGE`; Wrapper có đúng một vùng `U7`, `lineMap.userLineCount=7`, `signatureText` và `resultSentinel` khác null, `matchSpec.returnType=ARRAY<INT,1>`; Standard I/O có source bằng `U7`, `prefixLineCount=0`, call-site/signature/sentinel/returnType null. Sáu gói phải khớp byte với sáu golden đã duyệt; hiện **chưa có tệp golden**, nên điều kiện byte chưa thể kết luận Đạt/Trượt. Không có stdin testcase hoặc giới hạn runtime trong gói. |
| Cleanup | Xoá artifact của lượt tạo gói; giữ golden đã duyệt trong repo |
| Evidence | Sáu command/response, manifest hash của golden, diff byte và phiên bản template/image |
| Design review | Đã review kịch bản và oracle cấu trúc; đối chứng byte còn chờ sáu golden theo handoff sau codegen; chưa thực thi |

### TC-F3-AC07-01 — Hai lần prepare cùng bộ đầu vào cho cùng byte

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-07; DD F3 API §2.1/P2-T02 |
| Kind / priority | Normal, P0: retry không được đổi gói chấm |
| Environment | P2, serializer chuẩn, ba plugin |
| Preconditions | `P-TWO` cố định version 3; không thay template hay config giữa hai lần |
| Test data | Với từng ngôn ngữ và từng model Wrapper/Standard I/O, gọi hai lần cùng `problemId`, `expectedSpecVersion=3`, `sourceCode=U7`; `correlationId` giữ nguyên |
| Steps | Gọi hai lần theo từng cặp; tuần tự hoá từng `PreparedProgram` cùng bộ serializer UTF-8 và so byte, đồng thời so từng trường `payload`, `lineMap`, `signatureText`, `matchSpec`, `resultSentinel`. |
| Expected | Cả sáu cặp giống nhau từng byte, kể cả thứ tự files/argv/env; không có timestamp/random ID trong gói. P2 không ghi DB/event; không lắp stdin hay giới hạn testcase vào payload. |
| Cleanup | Xoá artifact |
| Evidence | 12 response, hash SHA-256 từng cặp, trace DB/event |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC08-01 — Chuỗi neo và placeholder trong mã người học không bị render

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-08; DD F3 logic §4.1/P2-T07, security §1 |
| Kind / priority | Security, P0: nội suy mã người học vào template |
| Environment | P2, ba plugin |
| Preconditions | `P-TWO`; một baseline bảy dòng comment `U7` |
| Test data | Bản biến thể giữ bảy dòng, thay dòng 3 bằng `@@APH_USER_CODE@@`, dòng 5 bằng `{{FUNCTION_NAME}}` dưới dạng comment đúng ngôn ngữ |
| Steps | Prepare baseline và biến thể cho ba ngôn ngữ; so prefix, epilogue, `lineMap`, rồi so riêng vùng người học. |
| Expected | Prefix và epilogue từng byte bằng nhau; `lineMap` bằng nhau; hai chuỗi nguy hiểm xuất hiện nguyên văn đúng một lần ở vùng người học, không được thay bằng tên hàm và không chèn mã thêm tại neo giả. `argv`/`env` không chứa mã người học. |
| Cleanup | Xoá artifact |
| Evidence | Sáu tệp nguồn, diff ba vùng, payload và line map |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC08-02 — Dấu đóng khối thừa không đổi ranh giới ghép

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-08; DD F3 logic §4.1/P2-T07 |
| Kind / priority | Security, P1: mã phá cấu trúc phải ở vùng người học |
| Environment | P2, ba plugin; compiler/parse chạy riêng trong sandbox ở bước kiểm sau |
| Preconditions | `P-TWO`, baseline `U7` |
| Test data | Bản bảy dòng có `}` thừa ở dòng 4 Java/C++; Python có dấu `}` thừa ở dòng 4 |
| Steps | Prepare baseline và bản phá cấu trúc; so vị trí các marker, prefix/epilogue, line map; chuyển gói phá cấu trúc cho compiler/parser trong sandbox. |
| Expected | P2 vẫn nối đúng bảy dòng, prefix/epilogue và line map như baseline; không chạy mã người học trong tiến trình F3. Nếu compiler/parser báo lỗi cú pháp, lỗi thuộc bước sau; P2 không viết lại template để “sửa” dấu `}`. |
| Cleanup | Xoá artifact sandbox |
| Evidence | Tệp ghép, line map và log compiler/parser |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC09-01 — CRLF và CR đơn được chuẩn hoá thành LF

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-09; DD F3 logic §4.2–4.3/P2-T08 |
| Kind / priority | Boundary, P1: đếm dòng đa nền tảng |
| Environment | P2, ba plugin |
| Preconditions | `P-TWO` |
| Test data | Năm marker comment nối bằng chuỗi `\r\n`, `\r`, `\r\n`, `\r`, kết thúc `\r\n` |
| Steps | Gọi P2; lấy riêng vùng người học và kiểm byte/dòng. |
| Expected | Vùng người học đúng `u1\nu2\nu3\nu4\nu5\n` với comment prefix của ngôn ngữ; không còn byte CR; `userLineCount=5`, epilogue bắt đầu ngay dòng thứ sáu sau prefix; tab/thụt lề nếu có giữ nguyên. |
| Cleanup | Xoá artifact |
| Evidence | Hex dump nguồn đầu vào/đầu ra, line map |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC09-02 — Thiếu newline cuối được thêm đúng một lần

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-09; DD F3 logic §4.2–4.3/P2-T08 |
| Kind / priority | Boundary, P1: epilogue không dính mã người học |
| Environment | P2, ba plugin |
| Preconditions | `P-TWO` |
| Test data | Ba dòng comment `u1\nu2\nu3`, không có LF cuối |
| Steps | Prepare; xem byte kết thúc vùng người học và vị trí epilogue. |
| Expected | Chỉ thêm một LF sau `u3`, `userLineCount=3`, epilogue bắt đầu dòng kế; không có dòng trống thêm giữa hai vùng. |
| Cleanup | Xoá artifact |
| Evidence | Nguồn đánh số dòng và line map |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC09-03 — BOM đầu tệp bị bỏ trước khi đếm dòng

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-09; DD F3 logic §4.2–4.3/P2-T08 |
| Kind / priority | Boundary, P1: BOM không làm hỏng cú pháp đầu vùng |
| Environment | P2, ba plugin |
| Preconditions | `P-TWO` |
| Test data | `U+FEFF` + ba dòng comment `u1\r\nu2\r\nu3` không LF cuối |
| Steps | Prepare; so vùng người học với bản cùng nội dung không BOM đã chuẩn hoá. |
| Expected | BOM đầu tệp biến mất, không có BOM giữa prefix và `u1`; hai vùng nguồn giống nhau từng byte, `userLineCount=3`, epilogue ở dòng mới. |
| Cleanup | Xoá artifact |
| Evidence | Hex dump, diff và line map |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC10-01 — Nguồn chỉ có whitespace bị từ chối

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-10; DD F3 API §2.2/2.5/P2-T05 `HRN-020` |
| Kind / priority | Abnormal, P0: chặn nguồn rỗng trước sinh |
| Environment | P2 spy reader/plugin |
| Preconditions | `P-TWO` tồn tại |
| Test data | `sourceCode=" \t\r\n "`, Wrapper Java |
| Steps | Gọi P2, quan sát lỗi và số lần gọi reader/plugin. |
| Expected | `HRN-020 SOURCE_BLANK`, không trả `PreparedProgram`; reader/plugin chưa được gọi vì kiểm nguồn đầu luồng; không ghi DB/event. |
| Cleanup | Không có dữ liệu ghi |
| Evidence | Error object và spy call counts |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC10-02 — 65.537 byte UTF-8 bị từ chối

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-10; DD F3 API §2.2/P2-T05 `HRN-021` |
| Kind / priority | Boundary, P0: giới hạn byte chứ không phải ký tự |
| Environment | P2 spy reader/plugin |
| Preconditions | `P-TWO` tồn tại |
| Test data | `sourceCode="#" + "x" * 65_536` (65.537 byte ASCII), Python Standard I/O |
| Steps | Xác nhận fixture bằng UTF-8 byte count; gọi P2. |
| Expected | `HRN-021 SOURCE_TOO_LARGE`; không có gói hay gọi reader/plugin, không ghi DB/event. |
| Cleanup | Không có dữ liệu ghi |
| Evidence | Byte count, error object, spy counts |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC10-03 — NUL trong nguồn bị từ chối

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-10; DD F3 API §2.2/P2-T05 `HRN-022` |
| Kind / priority | Security, P0: không đưa ký tự NUL sang engine |
| Environment | P2 spy reader/plugin |
| Preconditions | `P-TWO` tồn tại |
| Test data | `sourceCode="print(1)\u0000\n"`, Python Standard I/O |
| Steps | Gọi P2; kiểm lỗi, payload và call counts. |
| Expected | `HRN-022 SOURCE_INVALID_CHARS`; không có `PreparedProgram` hoặc payload, không gọi reader/plugin, không ghi DB/event. |
| Cleanup | Không có dữ liệu ghi |
| Evidence | Escaped request, error object, spy counts |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC10-04 — Đúng 65.536 byte UTF-8 được nhận

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-10; DD F3 API §2.2/P2-T06 |
| Kind / priority | Boundary, P0: bao hàm ngưỡng tối đa |
| Environment | P2, Python plugin |
| Preconditions | `P-TWO` version 3 |
| Test data | `sourceCode="#" + "x" * 65_535` (65.536 byte ASCII), `model=STANDARD_IO` |
| Steps | Xác nhận byte count; gọi P2 và kiểm tệp nguồn. |
| Expected | Prepare thành công, `specVersion=3`, source trong gói bằng đầu vào cộng đúng một LF do thiếu newline; giới hạn kiểm trên **đầu vào** 65.536 byte, không áp lại giới hạn này cho nguồn sau chuẩn hoá. Không có lỗi `HRN-021`. |
| Cleanup | Xoá artifact |
| Evidence | Byte count và `PreparedProgram` |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC11-01 — Version 3 ghim trên lượt nộp, đặc tả hiện hành 4

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-11; DD F3 API §2.2/2.5/P2-T04 `HRN-002` |
| Kind / priority | Concurrency, P0: không chấm theo đặc tả mới âm thầm |
| Environment | P2, F2 spec reader spy |
| Preconditions | Request giữ `expectedSpecVersion=3`; spec reader trả `P-TWO` bản version 4 sau một lần sửa của A2 |
| Test data | Wrapper Java với `sourceCode=U7` |
| Steps | Gọi P2; ghi version từ reader, lỗi và số lần render/payload assembly. |
| Expected | `HRN-002 SPEC_VERSION_MISMATCH`; reader được gọi đúng một lần; không render, không trả `PreparedProgram` version 3 hay 4, không ghi DB/event. Chính sách chấm lại/hủy thuộc F4, không do P2 tự quyết. |
| Cleanup | Hoàn fixture version 3 |
| Evidence | Request, spec version từ reader, error, spy counts |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC11-02 — Version ghim khớp thì gói giữ đúng snapshot

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-11; DD F3 API §2.2–2.3/P2-T01 |
| Kind / priority | Normal, P1: cùng snapshot cho codegen và match spec |
| Environment | P2, F2 reader |
| Preconditions | Reader trả `P-TWO` version 3; `matchingStrategy=EXACT` |
| Test data | Wrapper Java, `expectedSpecVersion=3`, `sourceCode=U7` |
| Steps | Gọi P2, kiểm version trong response, matchSpec và số lần đọc. |
| Expected | Gói thành công có `specVersion=3`; mã ghép, chữ ký và `matchSpec.matchingStrategy=EXACT` cùng từ một bản spec đọc một lần; không đọc lại version khác giữa các bước. |
| Cleanup | Xoá artifact |
| Evidence | PreparedProgram, reader trace |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC12-01 — Standard I/O giữ nguyên nguồn mười dòng, không sinh khung

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-12; DD F3 API §2.3/P2-T01, logic §4.5 |
| Kind / priority | Normal, P0: Standard I/O không được thêm logic đọc/in |
| Environment | P2, ba plugin |
| Preconditions | `P-STD` tồn tại, `functionWrapperSupported=false`; source `U10` của từng ngôn ngữ |
| Test data | Ba command `model=STANDARD_IO`, `expectedSpecVersion=3`, source 10 dòng |
| Steps | Gọi P2; giải mã tệp nguồn; so byte với nguồn đã chuẩn hoá; kiểm `lineMap`, `matchSpec`, sentinel và payload. |
| Expected | Tệp nguồn bằng đúng `U10` sau chuẩn hoá, không có prefix/epilogue hay hàm đọc/in tự sinh; `prefixLineCount=0`, `userLineCount=10`, hai call-site null, `resultSentinel=null`, `signatureText=null`, `matchSpec.returnType=null`; source file lần lượt `Main.java`, `main.cpp`, `main.py`. `functionWrapperSupported=false` không chặn Standard I/O. |
| Cleanup | Xoá artifact |
| Evidence | Ba gói, source diff và line maps |
| Design review | Đã review; chưa thực thi |
