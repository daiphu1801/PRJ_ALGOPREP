# Test case QA — F4: lỗi biên dịch Run Code và giới hạn hiệu dụng

> 4 ca thiết kế cho AC-11, AC-14; chưa thực thi. Nguồn: TDD F4 AC-11/14; DD F4 API §4–5, logic BR-JD-02/09; DD F3 `MapCompileErrorUseCase`. Bài P-X đã xuất bản và có Sample; mọi ca dùng JWT STUDENT A, không tạo submission bền vững.

### TC-F4-AC11-01 — Lỗi cú pháp wrapper ánh xạ về dòng 3

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-11; DD F4 API §5.3/5.5; DD F3 MapCompileErrorUseCase |
| Kind / priority | Normal + security, P0: không lộ mã khung |
| Environment | Run Code API #2, harness F3, compiler thực, DB judge, FE USR0102 |
| Preconditions | P-X hỗ trợ FUNCTION_WRAPPER Java, signature hợp lệ; nguồn người học gồm dòng 1 khai hàm, dòng 2 hợp lệ, dòng 3 thiếu dấu `;`; mã khung sinh có marker `FRAME_SECRET_11`. Số submission của A trước chạy là N. |
| Test data | POST `/problems/{P-X}/run-sample` với `submissionModel=FUNCTION_WRAPPER`, `language=JAVA`, nguồn trên |
| Steps | Gọi API, đọc toàn response và màn, so mapping của F3 với file ghép; đếm DB submission sau chạy. |
| Expected | HTTP 200, `outcome=COMPILE_ERROR`, `compileErrorMessage` chỉ dòng 3 của nguồn người học và thông báo đã lọc; `results=[]`; không có marker, đường dẫn hay đoạn mã khung trong toàn response/màn. DB submission vẫn N, không ghi dòng testcase. |
| Cleanup | Dọn file biên dịch tạm nếu harness chưa tự dọn |
| Evidence | Request, source có số dòng, raw compiler output trong log nội bộ đã che dữ liệu, response, DB count |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC14-01 — Một GET trả giới hạn của cả ba ngôn ngữ kể cả đã tắt

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-14; DD F4 API §4.2/4.3 T-EL-01/03; logic BR-JD-09 |
| Kind / priority | Normal, P1: màn hiển thị đúng giới hạn và trạng thái |
| Environment | GET `/problems/{problemId}/execution-limits`, DB config, FE USR0102 |
| Preconditions | P-X giới hạn 1000 ms, 256 MB, output 1024 KB; JAVA hệ số time/memory 2.00/2.00; CPP 1.00/1.00; PYTHON 3.00/1.00 nhưng enabled=false |
| Test data | GET P-X không truyền language |
| Steps | Gọi API, xem ba tab ngôn ngữ và DB configs. |
| Expected | 200, ba phần tử: JAVA 2000 ms/512 MB, CPP 1000/256, PYTHON 3000/256 và `enabled=false`; cả ba output 1024 KB. FE đánh dấu Python tắt và không chọn để nộp mới; dữ liệu config không bị sửa. |
| Cleanup | Khôi phục config fixture |
| Evidence | Response, DB, ảnh ba tab |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC14-02 — Truy vấn một ngôn ngữ và mã lạ

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-14; DD F4 API §4.2/4.4 T-EL-05 |
| Kind / priority | Boundary + abnormal, P1 |
| Environment | API #1, DB config |
| Preconditions | P-X như ca trên |
| Test data | `?language=JAVA`, `?language=RUST` |
| Steps | Gọi hai request độc lập. |
| Expected | JAVA trả 200 với đúng một phần tử, 2000 ms/512 MB; RUST trả `400 JDG-V03`, không đoán ngôn ngữ thay thế, không ghi DB. |
| Cleanup | Không ghi dữ liệu |
| Evidence | Hai request/response |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC14-03 — Bài không mở được trả cùng lỗi

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-14; DD F4 API §4.4 T-EL-04 |
| Kind / priority | Security + abnormal, P1 |
| Environment | API #1, ProblemGradingSpecPort |
| Preconditions | Có bài DRAFT, bài đã ẩn mềm và UUID bài không tồn tại |
| Test data | GET execution-limits cho ba ID |
| Steps | Gọi ba request, so status/code/message. |
| Expected | Cả ba `404 JDG-101` với cùng thông điệp “không tìm thấy bài toán”; không trả giới hạn của bài DRAFT/ẩn mềm, không ghi DB. |
| Cleanup | Dọn fixture bài |
| Evidence | Ba response và trạng thái bài từ F2 |
| Design review | Đã review; chưa thực thi |
