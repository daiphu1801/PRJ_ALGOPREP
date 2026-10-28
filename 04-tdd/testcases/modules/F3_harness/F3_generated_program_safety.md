# Test case QA — F3: an toàn chương trình sinh và khởi động

> Trạng thái: 12 ca thiết kế; 10 ca đã review, 2 ca AC-45 P2/P3 chờ chốt ranh giới ngôn ngữ không hỗ trợ; chưa thực thi. Nguồn: `04-tdd/modules/F3_harness.md` AC-41–47; DD F3 logic §1.2, §3.5, §4.1, §8; DD F3 API §2–3, §6; DD F3 validation §5. Các ca cần build harness thật và môi trường khởi động cô lập, chưa thể kết luận từ FE mock.

## Fixture chung

- `P-SAFE` là bài Wrapper đã xuất bản với `solve(int value) -> int`, ba plugin `JAVA/CPP/PYTHON` hợp lệ, `specVersion=7`. `A-SAFE` có quyền đọc/nộp. Mã là hàm `solve` trả `value+1`; input hợp lệ `1\n`, expected-output `2\n`. P2 trả `PreparedProgram` cùng `ExecutionPlan` và `JudgePayload` cho từng ngôn ngữ.
- Các ca fault khởi động dùng process/container F3 cô lập và hoàn nguyên template/plugin sau ca; không sửa cấu hình môi trường chia sẻ. Chỉ thực thi chương trình được sinh trong sandbox go-judge. Kiểm dữ liệu testcase bằng token duy nhất `HIDDEN_INPUT_7b91` và `HIDDEN_EXPECTED_4c62`, không ghi chúng vào mã người học hay đặc tả.
- **Oracle còn thiếu của AC-45:** DD F3 API §3.4 chốt vỏ HTTP P1 trả `400 HRN-010` cho `GO`, nên ca P1 kiểm ở HTTP được. P2/P3 nhận `Language` enum ba giá trị và registry thiếu plugin thì fail startup (AC-44), nên chưa có cách hợp lệ đưa ngôn ngữ thứ tư vào hai cổng để quan sát `HRN-010`. Cần chốt ranh giới kiểm P2/P3; hai ca đó chưa được phê duyệt.

| AC | Nhánh | Test case | Review | Thực thi |
| :--- | :--- | :--- | :--- | :--- |
| AC-41 | Input số hỏng; BOOLEAN sai dạng | TC-F3-AC41-01 đến -02 | Đã review | Chưa thực hiện |
| AC-42 | Dữ liệu testcase không nằm trong gói prepare | TC-F3-AC42-01 | Đã review | Chưa thực hiện |
| AC-43 | Mã người học không đi vào lệnh | TC-F3-AC43-01 | Đã review | Chưa thực hiện |
| AC-44 | Thiếu plugin; template thiếu hoặc thừa neo | TC-F3-AC44-01 đến -03 | Đã review | Chưa thực hiện |
| AC-45 | P1 HTTP từ chối; adapter P2/P3 chặn trước enum | TC-F3-AC45-01 đến -03 | Đã review | Chưa thực hiện |
| AC-46 | Schema harness không có bảng/ghi | TC-F3-AC46-01 | Đã review | Chưa thực hiện |
| AC-47 | F5/F6 lỗi, bốn cổng F3 vẫn chạy | TC-F3-AC47-01 | Đã review | Chưa thực hiện |

## Test case

### TC-F3-AC41-01 — Input ký tự ở vị trí INT dừng trước hàm học viên

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-41; DD F3 logic §3.5, API §2.3 `harnessInputErrorExitCode` |
| Kind / priority | Abnormal, P0: testcase hỏng không thành RE của học viên |
| Environment | P2, sandbox go-judge, F4 adapter quan sát exit code |
| Preconditions | `P-SAFE` Wrapper Java đã prepare; hàm `solve` có marker quan sát khi được gọi |
| Test data | Input testcase `secretWord\n` thay cho tham số INT, expected-output `2\n` |
| Steps | Chạy program đã sinh với input sai trong sandbox; kiểm exit/stderr và marker gọi hàm; cho F4 phân loại exit. |
| Expected | Exit code **90**, stderr đúng một dòng cố định `@@APH-HARNESS-INPUT-ERROR@@`, không chứa `secretWord`, vị trí input hoặc expected; hàm học viên chưa được gọi. F4 phân loại lỗi dữ liệu testcase/harness, không ghi verdict RE cho học viên. |
| Cleanup | Xoá sandbox artifact/fixture |
| Evidence | Exit/stderr, marker, F4 classification và log đã lọc |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC41-02 — BOOLEAN viết hoa bị báo lỗi input riêng

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-41; DD F3 logic §3.3, §3.5 |
| Kind / priority | Boundary, P1: chỉ `true`/`false` chữ thường |
| Environment | P2, sandbox go-judge, F4 adapter |
| Preconditions | Bản sao `P-SAFE` có chữ ký `solve(boolean flag) -> int`, cả ba plugin đã prepare |
| Test data | Input `True\n` (khác `true\n`), expected `2\n` |
| Steps | Chạy từng chương trình Java/C++/Python với input trên; kiểm exit/stderr và việc gọi hàm. |
| Expected | Mỗi ngôn ngữ exit 90, stderr chỉ `@@APH-HARNESS-INPUT-ERROR@@` một dòng, không nhắc `True`; hàm không chạy; F4 không gán RE. |
| Cleanup | Xoá artifact |
| Evidence | Ba log sandbox/exit và F4 classification |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC42-01 — Chuỗi testcase ẩn không xuất hiện trong chương trình/gói prepare

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-42; DD F3 logic §8 điểm 1, API §2.3.1 |
| Kind / priority | Security, P0: không nhúng bí mật testcase |
| Environment | P2, F2 snapshot fixture, inspector ExecutionPlan/JudgePayload |
| Preconditions | `P-SAFE` có Sample và Hidden; mã người học/đặc tả không chứa token kiểm; P2 chỉ nhận spec và mã nguồn |
| Test data | Hidden input chứa `HIDDEN_INPUT_7b91`, expected chứa `HIDDEN_EXPECTED_4c62`; Sample có token riêng khác |
| Steps | Gọi P2; serialize tất cả `files[].name/content`, `compile/run argv/env`, `JudgePayload.body`, `signatureText`, `matchSpec`; tìm token Hidden và nội dung Sample. |
| Expected | Không token/input/expected testcase nào trong mã nguồn, tệp, argv/env hoặc gói prepare; `JudgePayload` không chứa stdin, vì F4 ghép riêng khi chạy từng testcase. `matchSpec` chỉ có quy tắc so, không có expected. |
| Cleanup | Xoá fixture/artifact |
| Evidence | Serialized plan/payload đã kiểm, danh sách token tìm kiếm |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC43-01 — Source đối kháng chỉ đi trong nội dung tệp

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-43; DD F3 logic §4.1, §8 điểm 3; API §2.3.1 |
| Kind / priority | Security, P0: tránh command injection trong service harness |
| Environment | P2, inspector ExecutionPlan/JudgePayload, spy process/AI |
| Preconditions | `P-SAFE` chuẩn bị được hai request; không cho harness tự gọi shell/AI |
| Test data | Mã đối chứng Java bình thường; mã đối kháng chứa trong comment/string: `; rm -rf /`, backtick, `$(whoami)`, `"`, newline, `weird.java`; cả hai request cùng language/model/spec |
| Steps | Gọi P2 cho hai mã; so `files[].name`, compile/run `argv/env`, `JudgePayload.body` ngoài các trường `content`; kiểm spy không có tiến trình/chạy mã/AI. |
| Expected | Chuỗi đối kháng chỉ có trong `files[].content` hoặc `copyIn.*.content` của mã nguồn; tên tệp, argv, env và cấu hình sandbox giống đối chứng. P2 không biên dịch, chạy, `eval`/`exec` hoặc gọi AI trong service. |
| Cleanup | Xoá artifact/spy fixture |
| Evidence | Diff hai plan/payload, spy process/AI |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC44-01 — Thiếu một plugin làm startup thất bại

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-44; DD F3 logic §1.2 registry |
| Kind / priority | Recovery, P0: fail fast trước khi nhận bài |
| Environment | Process F3 cô lập, cấu hình plugin có thể fault inject |
| Preconditions | Registry bình thường có JAVA, CPP, PYTHON; bỏ đăng ký PYTHON trong process thử |
| Test data | Danh sách plugin chỉ `{JAVA,CPP}` |
| Steps | Khởi động process thử; kiểm trạng thái readiness/log và thử endpoint health. |
| Expected | Startup không hoàn tất, readiness không báo sẵn sàng; log nêu thiếu PYTHON/plugin registry, không chờ request của học viên mới lỗi. |
| Cleanup | Khôi phục plugin; dừng process thử |
| Evidence | Startup log, exit/readiness trạng thái |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC44-02 — Template không có neo mã học viên làm startup thất bại

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-44; DD F3 logic §4.1 template anchor |
| Kind / priority | Recovery, P0: template lỗi không ra production |
| Environment | Process F3 cô lập với template test |
| Preconditions | Bản sao `java/function-wrapper.tmpl` bỏ dòng `//@@APH_USER_CODE@@`; các template khác đúng |
| Test data | 0 neo user code ở một template |
| Steps | Khởi động process thử, kiểm readiness/log. |
| Expected | Startup thất bại rõ template Java Wrapper thiếu neo, không sinh mã/nhận request; không có lỗi muộn khi nộp bài. |
| Cleanup | Khôi phục template, dừng process |
| Evidence | Hash/template fixture, startup log/readiness |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC44-03 — Template có hai neo mã học viên làm startup thất bại

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-44; DD F3 logic §4.1 template anchor |
| Kind / priority | Recovery, P0: không chèn mã hai lần |
| Environment | Process F3 cô lập với template test |
| Preconditions | Bản sao `python/function-wrapper.tmpl` có hai dòng `#@@APH_USER_CODE@@`; các template khác đúng |
| Test data | 2 neo user code trong một template |
| Steps | Khởi động process thử, kiểm readiness/log. |
| Expected | Startup thất bại rõ template Python Wrapper thừa neo; không nhận request hoặc sinh chương trình bị chèn hai lần. |
| Cleanup | Khôi phục template, dừng process |
| Evidence | Template fixture, startup log/readiness |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC45-01 — P1 từ chối ngôn ngữ GO

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-45; DD F3 validation §5, API P1 `HRN-010` |
| Kind / priority | Abnormal, P1: registry chỉ ba ngôn ngữ |
| Environment | P1 HTTP API |
| Preconditions | `P-SAFE` mở được, người gọi đủ quyền; request query giữ nguyên chuỗi `GO` trước bước parse enum |
| Test data | `language="GO"`, `model=FUNCTION_WRAPPER` |
| Steps | Gọi HTTP GET starter-code với `language=GO` trực tiếp, bỏ qua FE. |
| Expected | `400 HRN-010 LANGUAGE_UNSUPPORTED` theo DD F3 API §3.4; không có starter code, không chọn plugin mặc định. Đây là kiểm vỏ HTTP, không khẳng định P1 domain nhận được giá trị `GO`. |
| Cleanup | Reset fixture |
| Evidence | Request, error code/tầng phát lỗi, plugin spy |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC45-02 — Adapter chặn GO trước khi gọi P2

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-45; DD F3 validation §5, API P2 `HRN-010` |
| Kind / priority | Abnormal, P1: không tạo gói ngôn ngữ lạ |
| Environment | Adapter nhận lệnh chấm thô trước P2, P2 spy và plugin spy |
| Preconditions | `P-SAFE` hợp lệ; adapter nhận chuỗi ngôn ngữ trước bước chuyển thành enum `Language` |
| Test data | Lệnh thô `language="GO"`, `sourceCode="class Solution {}\n"`, `model=FUNCTION_WRAPPER` |
| Steps | Đưa lệnh thô qua adapter; kiểm lỗi, số lần gọi P2 và artifact. |
| Expected | Adapter trả mã miền `HRN-010 LANGUAGE_UNSUPPORTED` trước parse enum; P2 không được gọi, không có `PreparedProgram`, `ExecutionPlan` hoặc `JudgePayload`; không chọn plugin khác thay thế. Chữ ký P2 vẫn nhận enum ba giá trị. |
| Cleanup | Reset fixture |
| Evidence | Error, artifact/plugin spy |
| Design review | Đã review theo quyết định chủ dự án 2026-10-27; chưa thực thi |

### TC-F3-AC45-03 — Adapter chặn GO trước khi gọi P3

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-45; DD F3 validation §5, API P3 `HRN-010` |
| Kind / priority | Abnormal, P1: không parse compiler bằng plugin sai |
| Environment | Adapter nhận kết quả compiler thô trước P3, P3/parser spy |
| Preconditions | LineMap hợp lệ, adapter nhận chuỗi ngôn ngữ trước bước chuyển thành enum `Language` |
| Test data | `language="GO"`, `rawCompilerOutput="error: bad syntax"` |
| Steps | Đưa kết quả compiler thô qua adapter, kiểm lỗi và số lần gọi P3/parser. |
| Expected | Adapter trả mã miền `HRN-010 LANGUAGE_UNSUPPORTED` trước parse enum; P3 không được gọi, không có báo lỗi biên dịch được ánh xạ, không dùng parser Java/CPP/Python thay thế. Chữ ký P3 vẫn nhận enum ba giá trị. |
| Cleanup | Reset fixture |
| Evidence | Error, parser spy |
| Design review | Đã review theo quyết định chủ dự án 2026-10-27; chưa thực thi |

### TC-F3-AC46-01 — Schema harness không có bảng và cổng không ghi DB

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-46; BD database F3 mục 1; DD F3 API §1 |
| Kind / priority | Architecture, P1: module thuần không sở hữu dữ liệu |
| Environment | PostgreSQL catalog, DB write tracer, P1/P2/P3/P4 |
| Preconditions | Migration đầy đủ đã chạy; snapshot catalog/tables trước ca; các cổng F3 dùng fixture hợp lệ |
| Test data | Một lần gọi hợp lệ cho mỗi P1 starter, P2 prepare, P3 map lỗi, P4 match |
| Steps | Query schema `harness`; bật write tracer theo module; gọi bốn cổng; query lại catalog và SQL writes. |
| Expected | Schema `harness` tồn tại với **0 bảng**; không có INSERT/UPDATE/DELETE do F3 thực hiện. Các cổng vẫn trả đúng kết quả; việc F2 đọc đặc tả không biến thành thao tác ghi của F3. |
| Cleanup | Tắt tracer; xoá artifact fixture |
| Evidence | Catalog query trước–sau, SQL trace và bốn response |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC47-01 — F5/F6 lỗi không cản bốn cổng F3

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-47; DD F3 logic §8 điểm 3; TDD F3 AC-01/06/14/23 |
| Kind / priority | Recovery, P0: harness độc lập với AI |
| Environment | F3 P1/P2/P3/P4, F2 reader, F5/F6 fault, AI call spy |
| Preconditions | Lưu baseline bốn response hợp lệ khi AI hoạt động; sau đó tắt F5/F6 hoặc trả hết quota AI |
| Test data | `P-SAFE`; P1 Java starter; P2 Java prepare; P3 một lỗi compiler ở dòng người học; P4 EXACT `actualStdout="2\n"`, `expectedOutput="2\n"` |
| Steps | Với AI lỗi, gọi lại bốn cổng cùng fixture; so kết quả với baseline và AI spy. |
| Expected | P1/P2/P3/P4 trả cùng kết quả nghiệp vụ như baseline, không lỗi do AI; P4 `MATCH`. Không có request từ F3 sang F5/F6 hoặc quota AI. |
| Cleanup | Khôi phục F5/F6, reset spy/artifact |
| Evidence | Baseline/fault responses, AI spy và service status |
| Design review | Đã review; chưa thực thi |
