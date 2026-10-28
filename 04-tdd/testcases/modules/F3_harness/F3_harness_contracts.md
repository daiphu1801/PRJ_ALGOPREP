# Test case QA — F3: so khớp, quyền mã khung và chữ ký hàm

> Trạng thái: 19 ca đã review thiết kế; chưa thực thi. Đây là ca QA cho hợp đồng F3, không phải testcase đầu vào của học viên.

## Phạm vi và fixture

- TDD: `04-tdd/modules/F3_harness.md` AC-27, AC-48, AC-49, AC-50.
- DD: `03-dd/api/F3_harness.md` §3.1, §3.7, §6.2; `03-dd/logic/F3_harness.md` §2, §3.3, §3.5, §4.7, §7.2; `03-dd/validation/F3_harness.md` §3.
- `P-PRIVATE`: bài `UNPUBLISHED` của `A2-OWNER`, mô hình `FUNCTION_WRAPPER`, chữ ký `sumTwo(INT a, INT b) -> INT`, đặc tả hợp lệ. `A2-OTHER` là giảng viên không sở hữu, `A1-STUDENT` là học viên, `ADMIN-1` là quản trị viên. `P-PUBLIC` là bản sao đã `PUBLISHED`; `P-GONE` là UUID không tồn tại. Mọi JWT là tài khoản thử cô lập.
- `DRAFT-1`: `functionName=two_sum`, `returnType={kind:INT}`, `parameters=[{name:value,type:{kind:INT}}]`, `nameOverrides={}`, `matchingStrategy=EXACT`, `epsilonValue=null`. Với tên và kiểu này, Java/C++ dùng `twoSum(int value)`, Python dùng `two_sum(self, value: int) -> int`.
- `P-ARRAY`: bài `FUNCTION_WRAPPER` trả `INT`, có một tham số `matrix={kind:ARRAY,of:{kind:INT},dimensions:2}` và hàm người học trả tổng các phần tử. Khi input là `2 3\n1 2 3\n4 5 6\n`, expected output là `21\n`.
- Các ca dùng fixture riêng. Với ca F3 thuần, gọi cổng `MatchOutputUseCase` hoặc `PrepareSubmissionUseCase` bằng harness kiểm thử; ca HTTP dùng API tích hợp. Ghi phiên bản build, đặc tả và request thực tế trong run record.

## Ma trận nhánh

| AC | Nhánh | Test case | Review | Thực thi |
| :--- | :--- | :--- | :--- | :--- |
| F3 AC-27 | LIST đổi chỗ nhưng giữ số lần lặp | TC-F3-AC27-01 | Đã review | Chưa thực hiện |
| F3 AC-27 | LIST thiếu phần tử lặp | TC-F3-AC27-02 | Đã review | Chưa thực hiện |
| F3 AC-27 | Standard I/O dùng đa tập token | TC-F3-AC27-03 | Đã review | Chưa thực hiện |
| F3 AC-27 | ARRAY hai chiều đổi thứ tự hàng, giữ thứ tự trong hàng | TC-F3-AC27-04 | Đã review | Chưa thực hiện |
| F3 AC-48 | Tác giả và ADMIN đọc bài chưa xuất bản | TC-F3-AC48-01 | Đã review | Chưa thực hiện |
| F3 AC-48 | Người ngoài phạm vi giống bài không tồn tại | TC-F3-AC48-02 | Đã review | Chưa thực hiện |
| F3 AC-48 | Người đã đăng nhập đọc bài xuất bản | TC-F3-AC48-03 | Đã review | Chưa thực hiện |
| F3 AC-48 | Bài đã xoá trả như bài không tồn tại | TC-F3-AC48-04 | Đã review | Chưa thực hiện |
| F3 AC-49 | Bản nháp đổi tên/override, xem trước khớp bản lưu | TC-F3-AC49-01 | Đã review | Chưa thực hiện |
| F3 AC-49 | Chưa có chữ ký | TC-F3-AC49-02 | Đã review | Chưa thực hiện |
| F3 AC-49 | Chữ ký vi phạm cứng | TC-F3-AC49-03 | Đã review | Chưa thực hiện |
| F3 AC-49 | Học viên không được xem trước | TC-F3-AC49-04 | Đã review | Chưa thực hiện |
| F3 AC-49 | Anonymous, rate limit, Redis lỗi | TC-F3-AC49-05 đến -07 | Đã review | Chưa thực hiện |
| F3 AC-49 | Body vượt 16 KiB | TC-F3-AC49-08 | Đã review | Chưa thực hiện |
| F3 AC-50 | Ma trận 2×3 đúng định dạng | TC-F3-AC50-01 | Đã review | Chưa thực hiện |
| F3 AC-50 | Thiếu một hàng | TC-F3-AC50-02 | Đã review | Chưa thực hiện |
| F3 AC-50 | Thiếu một phần tử trong hàng | TC-F3-AC50-03 | Đã review | Chưa thực hiện |

## Test case

### TC-F3-AC27-01 — LIST đổi vị trí vẫn khớp khi giữ số lần lặp

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-27; DD F3 API §6.2 P4, logic §7.2 `UNORDERED_SET` |
| Kind / priority | Normal, P1: thứ tự không làm sai kết quả |
| Environment | F3 `MatchOutputUseCase`, không cần AI/sandbox |
| Preconditions | `model=FUNCTION_WRAPPER`, `returnType=LIST<INT>`, `matchingStrategy=UNORDERED_SET`; một `resultSentinel` hợp lệ được truyền cùng request |
| Test data | `expectedOutput="[1,1,2]\n"`; `actualStdout` sau sentinel là `[2,1,1]\n` |
| Steps | Gọi P4 với dữ liệu trên, đọc toàn bộ `MatchResult`. |
| Expected | `verdict=MATCH`; kết quả không chứa `expectedOutput`, diff hoặc vị trí phần tử. |
| Cleanup | Không ghi dữ liệu; dọn fixture trong bộ nhớ |
| Evidence | Request đã che sentinel nếu cần, `MatchResult` và phiên bản thư viện |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC27-02 — LIST thiếu một bản sao của phần tử phải sai

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-27; DD F3 logic §7.2 `UNORDERED_SET` |
| Kind / priority | Abnormal, P1: không được biến đa tập thành tập hợp |
| Environment | F3 `MatchOutputUseCase` |
| Preconditions | Cùng `MatchSpec` và sentinel như TC-F3-AC27-01 |
| Test data | `expectedOutput="[1,1,2]\n"`; output người học sau sentinel `[1,2]\n` |
| Steps | Gọi P4; đọc verdict và các trường response. |
| Expected | `verdict=NO_MATCH`; không có `expectedOutput`, số lần lặp bị thiếu hay diff trong response. Đây là đáp án sai, không phải `HRN-050`. |
| Cleanup | Dọn fixture trong bộ nhớ |
| Evidence | Request và `MatchResult` |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC27-03 — Standard I/O giữ số lần lặp của token

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-27; DD F3 logic §7.2 `UNORDERED_SET`, API §6.2 |
| Kind / priority | Boundary, P1: mô hình không có kiểu vẫn dùng đa tập |
| Environment | F3 `MatchOutputUseCase` |
| Preconditions | `model=STANDARD_IO`, `matchingStrategy=UNORDERED_SET`, không có `returnType` hoặc sentinel |
| Test data | `expectedOutput="1 1 2\n"`; hai lời gọi độc lập với `actualStdout="2 1 1\n"` và `actualStdout="1 2\n"` |
| Steps | Gọi P4 cho từng output trên với cùng expected, lưu hai kết quả. |
| Expected | Output `2 1 1` là `MATCH`; output `1 2` là `NO_MATCH`. Cả hai kết quả không chứa expected hoặc diff. |
| Cleanup | Dọn fixture trong bộ nhớ |
| Evidence | Hai request và hai `MatchResult` |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC27-04 — ARRAY hai chiều chỉ được đổi thứ tự hàng

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-27; DD F3 logic §7.2 `UNORDERED_SET` với ARRAY nhiều chiều |
| Kind / priority | Boundary, P1: phần tử cấp cao nhất là hàng |
| Environment | F3 `MatchOutputUseCase` |
| Preconditions | `model=FUNCTION_WRAPPER`, `returnType=ARRAY<INT,2>`, `matchingStrategy=UNORDERED_SET`, `epsilonValue=null`, `resultSentinel="@@APH-RESULT@@"` |
| Test data | `expectedOutput="1 2\n3 4\n"`; A `actualStdout="@@APH-RESULT@@\n3 4\n1 2\n"`; B `actualStdout="@@APH-RESULT@@\n2 1\n3 4\n"` |
| Steps | Gọi P4 độc lập cho A/B, lưu command và response. |
| Expected | A `MATCH` vì đổi hàng; B `NO_MATCH` vì đổi thứ tự trong hàng. Không có expected/diff trong response. |
| Cleanup | Dọn fixture trong bộ nhớ |
| Evidence | Hai command và hai `MatchResult` |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC48-01 — Tác giả và ADMIN đọc mã khung bài chưa xuất bản

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-48; DD F3 API §3.1, §3.5, P1-T08 |
| Kind / priority | Security/Normal, P0: hai vai trò hợp lệ được đọc nội dung chưa công khai |
| Environment | HTTP F3 + F2 `StarterCodeAccessPort` + DB fixture |
| Preconditions | `P-PRIVATE` còn `UNPUBLISHED`, chữ ký hợp lệ; `A2-OWNER` và `ADMIN-1` có JWT hợp lệ |
| Test data | GET `/api/v1/harness/problems/{P-PRIVATE}/starter-code?language=PYTHON&model=FUNCTION_WRAPPER` với từng JWT |
| Steps | Gọi GET bằng tác giả rồi ADMIN; đọc response và audit truy cập nếu hệ thống có. |
| Expected | Cả hai trả 200 với `language=PYTHON`, `submissionModel=FUNCTION_WRAPPER`, `sourceCode` chứa đúng hàm `sum_two(self, a: int, b: int)` và không chứa testcase hoặc đáp án mẫu; hai mã khung giống nhau. Không ghi thay đổi bài. |
| Cleanup | Dọn bài và tài khoản fixture |
| Evidence | Hai request/response, trạng thái bài trước/sau |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC48-02 — Người ngoài phạm vi không phân biệt bài riêng với bài không tồn tại

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-48; DD F3 API §3.1, §3.4, P1-T08 |
| Kind / priority | Security, P0: không dò được chữ ký hoặc sự tồn tại của bài chưa xuất bản |
| Environment | HTTP F3 + F2 `StarterCodeAccessPort` + DB fixture |
| Preconditions | `P-PRIVATE` thuộc `A2-OWNER`; `P-GONE` không tồn tại; `A2-OTHER` và `A1-STUDENT` đăng nhập |
| Test data | Cùng query `language=PYTHON&model=FUNCTION_WRAPPER` cho `P-PRIVATE` và `P-GONE` |
| Steps | Mỗi tài khoản gọi GET cho cả hai UUID; so status, `error.code` và body, kiểm không có mã khung trong log/response. |
| Expected | Cả bốn lời gọi trả 404 `HRN-001` với body lỗi cùng dạng, không chứa chữ ký, mã khung hoặc dấu hiệu phân biệt bài riêng với bài không tồn tại; không ghi dữ liệu. |
| Cleanup | Dọn fixture cô lập |
| Evidence | Bốn request/response đã che JWT, DB bài trước/sau |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC48-03 — Học viên đọc được mã khung bài đã xuất bản

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-48; DD F3 API §3.1, P1-T08 |
| Kind / priority | Normal, P1: quyền đọc bài công khai không bị chặn nhầm |
| Environment | HTTP F3 + F2 `StarterCodeAccessPort` + DB fixture |
| Preconditions | `P-PUBLIC` đang `PUBLISHED`, Python bật; `A1-STUDENT` có JWT hợp lệ |
| Test data | GET mã khung Python `FUNCTION_WRAPPER` của `P-PUBLIC` |
| Steps | Gọi GET với JWT học viên; đọc response và trạng thái DB. |
| Expected | 200, `sourceCode` chứa chữ ký `sum_two(self, a: int, b: int)` theo đặc tả, không có testcase hoặc đáp án; không ghi DB. |
| Cleanup | Dọn fixture cô lập |
| Evidence | Request/response, trạng thái bài |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC48-04 — Bài đã xoá không lộ mã khung hoặc sự tồn tại

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-48; DD F3 API §3.4–3.6 P1-T08 |
| Kind / priority | Security, P0: bài đã xoá không được lộ mã khung |
| Environment | HTTP F3, F2 `StarterCodeAccessPort`, DB fixture |
| Preconditions | `P-DELETED` từng có đặc tả và mã khung hợp lệ, hiện ở trạng thái đã xoá; `P-GONE` chưa từng tồn tại; `A2-OWNER` và `A1-STUDENT` có JWT hợp lệ |
| Test data | Cùng query `language=PYTHON&model=FUNCTION_WRAPPER` cho `P-DELETED` và `P-GONE`, mỗi UUID được gọi bằng cả hai tài khoản |
| Steps | Gọi bốn request; so status, `error.code`, body và kiểm dữ liệu bài, cache mã khung sau các lần gọi. |
| Expected | Cả bốn trả 404 `HRN-001` cùng dạng body lỗi; không có `sourceCode`, chữ ký, tên hàm hoặc dấu hiệu phân biệt bài đã xoá với UUID chưa từng có. Không phục hồi bài, không ghi DB hoặc tạo cache mã khung. |
| Cleanup | Dọn hai UUID và JWT fixture |
| Evidence | Trạng thái xoá ban đầu, bốn request/response đã che JWT, DB/cache diff |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC49-01 — Xem trước phản ánh bản nháp mới và khớp mã khung sau lưu

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-49; DD F3 API §3.7, logic §2 và §4.7 |
| Kind / priority | Normal, P1: không dùng bản đặc tả đã lưu để xem trước |
| Environment | HTTP F3/F2 + PostgreSQL; A2 có quyền biên soạn bài fixture |
| Preconditions | Bài của `A2-OWNER` đang lưu chữ ký cũ `oldName(INT x) -> INT`; mở bản nháp mới `DRAFT-1` trong client nhưng chưa lưu |
| Test data | POST `/api/v1/harness/function-signature/preview` với `DRAFT-1`; POST lần hai đổi `nameOverrides={"JAVA":"solve"}`; sau đó lưu đúng draft lần hai bằng API F2 |
| Steps | Gọi preview hai lần; so ba khai báo và DB trước/sau preview; lưu draft lần hai; GET starter code ba ngôn ngữ, đối chiếu dòng khai báo. |
| Expected | Hai lần preview trả 200 `wrapperSupported=true`, không có `unsupportedReasons`. Lần đầu Java/C++ có `twoSum(int value)`, Python có `two_sum(self, value: int) -> int`; lần hai chỉ Java đổi thành `solve(int value)`, C++/Python giữ nguyên. Trước bước lưu, DB vẫn `oldName`; sau lưu, ba dòng trong mã khung khớp khai báo lần hai theo tên, kiểu và thứ tự tham số. Preview không gọi sandbox/AI hoặc tạo cache/ghi DB. |
| Cleanup | Dọn bài fixture sau khi lưu bằng chứng |
| Evidence | Hai request/response preview, DB trước/sau, response lưu và ba mã khung |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC49-02 — Chưa khai chữ ký trả ba khai báo null

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-49; DD F3 API §3.7, logic §4.7; validation `HRN-V07` |
| Kind / priority | Boundary, P1: bản nháp trống không hiện mã giả |
| Environment | HTTP F3; quan sát được stub/call count F2 |
| Preconditions | `A2-OWNER` có JWT hợp lệ; không cần bài hoặc `problemId` |
| Test data | Body có `functionName=null`, `returnType=null`, `parameters=null`, `nameOverrides={}`, `matchingStrategy=EXACT`, `epsilonValue=null` |
| Steps | POST preview; đọc response và số lần gọi F2/DB. |
| Expected | 200, `wrapperSupported=false`, `unsupportedReasons` chứa `HRN-V07`, `declarations.JAVA/CPP/PYTHON` đều null; không gọi F2, không ghi DB. |
| Cleanup | Không có dữ liệu cần dọn |
| Evidence | Request/response, call count F2 |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC49-03 — Tên hàm sai trả vi phạm cứng theo đường dẫn

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-49; DD F3 API §3.7; validation F3 §3 `HRN-V11` |
| Kind / priority | Abnormal, P1: vi phạm cứng không được sinh chữ ký |
| Environment | HTTP F3; quan sát được DB fixture |
| Preconditions | `A2-OWNER` có JWT hợp lệ, bài đang lưu chữ ký cũ `oldName` |
| Test data | `DRAFT-1` nhưng `functionName="2bad"` |
| Steps | POST preview, đọc mã lỗi và `details.violations`, kiểm DB. |
| Expected | 422 `HRN-060`; `details.violations` có `code=HRN-V11`, `path=/functionName`; không trả ba khai báo hợp lệ, DB vẫn giữ `oldName`. |
| Cleanup | Dọn bài fixture |
| Evidence | Request/lỗi API, DB trước/sau |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC49-04 — Học viên không được dùng preview

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-49; DD F3 API §3.7 (chỉ INSTRUCTOR/ADMIN) |
| Kind / priority | Security, P1: không cấp công cụ biên soạn cho học viên |
| Environment | HTTP F3 |
| Preconditions | `A1-STUDENT` đăng nhập hợp lệ |
| Test data | Body `DRAFT-1`, JWT học viên |
| Steps | POST preview; đọc status và body, kiểm không gọi P5 nếu có probe. |
| Expected | 403; không có `declarations` hoặc chữ ký trong body; không ghi DB hay gọi sandbox/AI. |
| Cleanup | Dọn tài khoản fixture |
| Evidence | Request/response đã che JWT, call count nếu có |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC49-05 — Preview không có JWT bị từ chối

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-49; DD API §3.7, xác thực chung |
| Kind / priority | Security, P1 |
| Environment | HTTP F3, auth spy |
| Preconditions | DRAFT-1 hợp lệ; không có Authorization header. |
| Test data | Body JSON `DRAFT-1` trong fixture chung; gửi nguyên body, không có header `Authorization` |
| Steps | POST preview, kiểm response và call count P5/DB. |
| Expected | 401, không gọi P5, không ghi DB hoặc cache. |
| Cleanup | Không có dữ liệu ghi |
| Evidence | Request/response và call count |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC49-06 — Mười preview mỗi giây được nhận, lượt 11 bị giới hạn

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-49; DD API §3.7 |
| Kind / priority | Boundary + concurrency, P1 |
| Environment | HTTP F3, Redis, clock T0 |
| Preconditions | INSTRUCTOR A và ADMIN B có JWT; Redis keys rỗng. |
| Test data | Body `DRAFT-1`; A gửi 10 request tại `T0..T0+90ms` (cách nhau 10ms), request thứ 11 tại `T0+100ms`; B gửi một request tại `T0+100ms`; A gửi lại tại `T0+1100ms` sau khi cửa sổ trượt |
| Steps | A gửi 10 POST DRAFT-1 trong cùng cửa sổ 1 giây rồi gửi lượt 11; B gửi một POST; A thử lại sau cửa sổ. |
| Expected | 10 lượt đầu của A và lượt B trả 200; lượt 11 A `429 HRN-061` có `retryAfterSeconds`; A được nhận sau cửa sổ. Không ghi DB/cache. |
| Cleanup | Dọn Redis keys |
| Evidence | Request timestamps, responses, Redis state |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC49-07 — Redis rate limit lỗi thì preview vẫn chạy

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-49; DD API §3.7 |
| Kind / priority | Recovery, P1 |
| Environment | HTTP F3, Redis fault injection |
| Preconditions | ADMIN B có JWT, DRAFT-1 hợp lệ, Redis unavailable. |
| Test data | Một POST của ADMIN B với body `DRAFT-1`; Redis trả lỗi kết nối ở lệnh kiểm hạn mức |
| Steps | POST preview, quan sát response và DB/cache. |
| Expected | 200 với ba declarations hợp lệ theo TC-F3-AC49-01; Redis lỗi không chặn tác vụ thuần CPU; không ghi DB/cache. |
| Cleanup | Khôi phục Redis |
| Evidence | Response, Redis fault trace, DB/cache diff |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC49-08 — Ranh giới body preview 16 KiB

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-49; DD API §3.7 |
| Kind / priority | Boundary, P1 |
| Environment | HTTP F3, P5 spy |
| Preconditions | INSTRUCTOR A có JWT; hai body JSON UTF-8 được pad bằng whitespace hợp lệ đến đúng 16,384 và 16,385 byte; Redis key rỗng. |
| Test data | Serialize `DRAFT-1` thành JSON UTF-8 cố định; thêm dấu cách hợp lệ sau dấu `}` tới đúng 16.384 byte, rồi thêm một dấu cách nữa cho body 16.385 byte. Xác nhận byte count trước khi gửi. |
| Steps | POST body 16,384 byte rồi 16,385 byte bằng cùng actor, kiểm độ dài raw body tại ingress và call count P5. |
| Expected | 16.384 byte được xử lý theo DRAFT-1; 16.385 byte trả `413 HRN-062 PREVIEW_BODY_TOO_LARGE` trước P5, không ghi DB/cache. Response lỗi không chứa body bản nháp hay chữ ký. |
| Cleanup | Dọn Redis key |
| Evidence | Raw byte counts, hai response, P5 spy |
| Design review | Đã review theo quyết định chủ dự án 2026-10-27; chưa thực thi |
### TC-F3-AC50-01 — Dòng tiêu đề 2 3 tạo ma trận đúng hai hàng ba cột

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-50; DD F3 logic §3.3 bảng 2, §3.5; Q8 |
| Kind / priority | Normal, P0: parser thống nhất định dạng nhiều chiều giữa F2/F3 |
| Environment | F3 `PrepareSubmissionUseCase` + go-judge/toolchain một ngôn ngữ; bài fixture `P-ARRAY` |
| Preconditions | `P-ARRAY` có chữ ký và testcase hợp lệ; hàm `sumMatrix(matrix)` trả tổng phần tử |
| Test data | Input `2 3\n1 2 3\n4 5 6\n`; expected output `21\n`; mã người học trả tổng tất cả phần tử |
| Steps | Prepare và chạy chương trình bọc hàm với input; đọc exit code, stdout, kết quả match. |
| Expected | Parser truyền ma trận `[[1,2,3],[4,5,6]]`; chương trình trả 21, exit code 0, P4 trả `MATCH`; không có marker `@@APH-HARNESS-INPUT-ERROR@@`. |
| Cleanup | Dọn bài và file/container fixture |
| Evidence | Đặc tả, input, mã người học, execution plan, exit code/stdout và `MatchResult` |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC50-02 — Thiếu hàng thứ hai là lỗi input của testcase

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-50; DD F3 logic §3.3 bảng 2, §3.5 |
| Kind / priority | Abnormal, P0: dữ liệu testcase hỏng không được gán RE cho học viên |
| Environment | F3 + go-judge/toolchain; `P-ARRAY` |
| Preconditions | Cùng mã người học đúng như TC-F3-AC50-01; bộ kiểm thử có thể cung cấp input lỗi trực tiếp, không lưu vào bài xuất bản |
| Test data | Input `2 3\n1 2 3\n` (thiếu toàn bộ hàng thứ hai) |
| Steps | Prepare và chạy chương trình với input lỗi; đọc exit code/stderr và số lần hàm người học được gọi. |
| Expected | Thoát mã 90 trước khi gọi hàm người học; stderr chỉ có dòng cố định `@@APH-HARNESS-INPUT-ERROR@@`, không chứa `1 2 3` hay vị trí lỗi. F4 nếu nhận kết quả này phân loại lỗi dữ liệu/hệ thống, không gán `RUNTIME_ERROR` cho học viên. |
| Cleanup | Dọn container/file fixture |
| Evidence | Input, exit code/stderr, probe hàm người học, mapping F4 nếu có |
| Design review | Đã review; chưa thực thi |

### TC-F3-AC50-03 — Hàng thiếu một phần tử không được tự suy số cột

| Field | Value |
| :--- | :--- |
| AC / DD source | F3 AC-50; DD F3 logic §3.3 bảng 2, §3.5; Q8 |
| Kind / priority | Boundary, P0: mỗi hàng phải có đúng số token từ tiêu đề |
| Environment | F3 + go-judge/toolchain; `P-ARRAY` |
| Preconditions | Cùng mã người học đúng như TC-F3-AC50-01; input lỗi được cấp trực tiếp trong harness kiểm thử |
| Test data | Input `2 3\n1 2 3\n4 5\n` (hàng thứ hai chỉ có hai phần tử) |
| Steps | Prepare và chạy chương trình; đọc exit code/stderr, probe hàm người học. |
| Expected | Thoát mã 90, stderr chỉ có marker cố định; không gọi hàm người học, không lấy số cột là 2 từ hàng thứ hai và không gán `RUNTIME_ERROR` cho học viên. |
| Cleanup | Dọn container/file fixture |
| Evidence | Input, exit code/stderr, probe hàm người học |
| Design review | Đã review; chưa thực thi |

