# Tra cứu HTTP status và mã lỗi AlgoPrep

Ngày đối chiếu: **14/11/2026**. Dùng khi đọc report ZAP, Network của trình duyệt, log API và kết quả QA.

Đây là **tài liệu tham chiếu**, không thêm AC hay thay hợp đồng lỗi. Hợp đồng chính vẫn nằm ở [DD API](F:/prj_algoprep/03-dd/api/api.md) và từng module. Danh mục cuối file được tổng hợp từ DD, mã nguồn backend và thông báo tiếng Việt; không đồng nghĩa mọi mã đều đã được thực thi kiểm chứng.

Đọc nhanh: mục **1** cho 401/403/500; mục **3** cho danh mục HTTP; mục **5** để phân biệt `errorCode`; mục **7** để đọc report ZAP; mục **10** để `Ctrl+F` mã riêng như `IDT-102` hoặc `AIR-501`.

## 1. Tra nhanh các mã quan trọng nhất

| HTTP | Hiểu đơn giản | Ví dụ trong AlgoPrep | Khi đọc report |
|---|---|---|---|
| **200** | Xử lý thành công | Đọc hồ sơ/danh sách | Nếu người không có quyền vẫn đọc được dữ liệu, 200 có thể là dấu hiệu lỗi bảo mật. |
| **201** | Tạo mới thành công | Tạo đề, tạo lớp, tạo bài nộp | Tạo bài nộp xong chưa có nghĩa đã chấm AC. |
| **202** | Đã nhận, chưa xử lý xong | Tác vụ bất đồng bộ nếu endpoint quy định | Cần kiểm tra kết quả sau đó. Không tự coi tác vụ thành công cuối cùng. |
| **204** | Thành công, không có body | Logout | Không gọi parser JSON khi response không có nội dung. |
| **400** | Request không hợp lệ | Sai kiểu, thiếu trường, JSON hỏng, tham số lỗi | Có thể là kết quả mong đợi khi scanner gửi dữ liệu lỗi. |
| **401** | Không có thông tin xác thực hợp lệ cho tài nguyên | Thiếu JWT, JWT sai/hết hạn; dự án còn từ chối JWT của tài khoản không ACTIVE | Không có nghĩa là "sai quyền" trong mọi trường hợp. Kiểm tra token, trạng thái tài khoản, `WWW-Authenticate` nếu có. |
| **403** | Server từ chối quyền truy cập/thao tác | Student gọi API Admin; không có permission cần thiết | Thường là kết quả **đạt** của ca kiểm tra chặn truy cập trái quyền. Không phải mọi 403 đều do đã đăng nhập: WAF, Origin hoặc gateway cũng có thể chặn. |
| **404** | Không tìm thấy tài nguyên hoặc không công bố sự tồn tại | ID sai, đề chưa public, lớp/bài nộp của người khác | Có thể dùng để bảo vệ riêng tư; không tự kết luận ID đó không có trong DB. |
| **409** | Request xung đột với trạng thái hiện tại | Dữ liệu trùng, version cập nhật cũ | Cần đối chiếu `errorCode` và đọc lại dữ liệu trước khi sửa/gửi lại. |
| **413** | Nội dung gửi quá lớn | Upload/source/body vượt trần của endpoint | Cần xem trần theo byte hay ký tự và tầng nào từ chối. |
| **415** | Định dạng nội dung không được hỗ trợ | Gửi XML/form vào API chỉ nhận JSON | Kiểm tra `Content-Type`, không chỉ nhìn body có vẻ là JSON. |
| **422** | Hiểu được request nhưng không xử lý được nội dung/nghiệp vụ | Mã mời sai, vi phạm điều kiện nghiệp vụ | Trong AlgoPrep, thường phân biệt với 400 ở lớp validation/điều kiện nghiệp vụ; phải theo DD endpoint cụ thể. |
| **429** | Quá giới hạn tần suất/hạn mức áp dụng | Thử đăng nhập quá nhiều, quota AI | Có thể là cơ chế bảo vệ hoạt động đúng. Kiểm tra `Retry-After`, quota và thời điểm reset nếu có. |
| **500** | Lỗi nội bộ phía server | Exception chưa xử lý, lỗi cấp mã đề, NUL lọt tới JDBC | Cần điều tra request và log. **Không đủ chứng minh SQL injection, XSS hay tràn bộ nhớ.** |
| **502** | Gateway/proxy nhận phản hồi upstream không hợp lệ | Proxy không giao tiếp hợp lệ với backend | Tìm tầng phát mã: ZAP, reverse proxy hay ứng dụng. |
| **503** | Dịch vụ tạm không sẵn sàng/không thể phục vụ | DB/quyền không đọc được, provider AI lỗi, hạ tầng thiếu | Không mặc định coi token sai; lỗi phụ thuộc có thể được backend ánh xạ thành 503. |
| **504** | Gateway/proxy đợi upstream quá lâu | Proxy timeout trong lúc backend xử lý | Không giống 408. Kiểm tra timeout của từng chặng. |

Các ý nghĩa HTTP đối chiếu với [IANA HTTP Status Codes](https://www.iana.org/assignments/http-status-codes/) và [RFC 9110](https://www.rfc-editor.org/rfc/rfc9110.html). Ví dụ AlgoPrep lấy từ DD/handler và báo cáo thực tế, không phải định nghĩa chung của HTTP.

## 2. Năm nhóm mã HTTP

| Nhóm | Ý nghĩa | Có phải bug không? |
|---|---|---|
| 1xx | Thông tin trung gian/tiến trình giao thức | Thường không. 101 dùng khi nâng cấp sang WebSocket. |
| 2xx | Request được tiếp nhận/xử lý theo nghĩa của status tương ứng | Chưa bảo đảm đúng nghiệp vụ hay đúng quyền. |
| 3xx | Chuyển hướng hoặc chỉ dẫn về bản được cache | Thường không; cần xem `Location` hoặc cache. |
| 4xx | Request không thể được đáp ứng vì xác thực, quyền, dữ liệu hoặc điều kiện áp dụng | Không đồng nghĩa lập trình viên frontend có bug; scanner cố tình tạo 4xx, hoặc server có thể trả sai 4xx. |
| 5xx | Server/gateway không hoàn thành được yêu cầu | Cần tìm nguyên nhân; có thể là bug, quá tải, sự cố phụ thuộc hoặc cấu hình thử thiếu. |

Chỉ những giá trị được đăng ký/định nghĩa mới có ý nghĩa cụ thể. Không phải mọi số trong khoảng 100–599 đều có tên chuẩn.

## 3. Danh mục HTTP mở rộng

Phần này để tra cả các mã ít gặp. Không có nghĩa AlgoPrep đang sử dụng tất cả. Tên/đăng ký đối chiếu [IANA](https://www.iana.org/assignments/http-status-codes/); cần chi tiết giao thức xem RFC được IANA dẫn cho từng mã.

### 3.1. Nhóm 1xx

| Mã | Tên | Cách hiểu |
|---|---|---|
| 100 | Continue | Server cho tiếp tục gửi phần request còn lại. |
| 101 | Switching Protocols | Đồng ý chuyển giao thức; thường thấy ở handshake WebSocket. |
| 102 | Processing | Thông báo đang xử lý, thuộc mở rộng WebDAV; chưa phải kết quả cuối. |
| 103 | Early Hints | Gửi gợi ý sớm, ví dụ tài nguyên nên tải trước. |
| 104 | Upload Resumption Supported | Đăng ký **tạm thời** cho tiếp tục upload. Bản IANA đối chiếu ghi hết hạn 13/11/2026; không dùng làm kỳ vọng của AlgoPrep và phải kiểm lại trạng thái đăng ký khi gặp. |

### 3.2. Nhóm 2xx

| Mã | Tên | Cách hiểu |
|---|---|---|
| 200 | OK | Request thành công. |
| 201 | Created | Đã tạo tài nguyên. |
| 202 | Accepted | Đã nhận, xử lý chưa kết thúc. |
| 203 | Non-Authoritative Information | Thành công nhưng nội dung đã được proxy biến đổi so với origin. |
| 204 | No Content | Thành công, không có nội dung response. |
| 205 | Reset Content | Yêu cầu phía nhận đặt lại giao diện nhập liệu. |
| 206 | Partial Content | Trả phần nội dung theo request Range. |
| 207 | Multi-Status | Một response chứa nhiều kết quả, thường trong WebDAV. |
| 208 | Already Reported | Thành phần đã được báo trong kết quả WebDAV trước đó. |
| 226 | IM Used | Trả kết quả áp dụng phép biến đổi/đồng bộ delta được thương lượng. |

### 3.3. Nhóm 3xx

| Mã | Tên | Cách hiểu |
|---|---|---|
| 300 | Multiple Choices | Có nhiều lựa chọn tài nguyên. |
| 301 | Moved Permanently | Địa chỉ đã chuyển lâu dài; một số client có thể đổi POST thành GET. |
| 302 | Found | Chuyển hướng tạm; thường gặp ở OAuth/login; client có thể đổi POST thành GET. |
| 303 | See Other | Đi lấy kết quả ở URL khác, thường bằng GET. |
| 304 | Not Modified | Bản đã cache còn dùng được; không có body tài nguyên mới. |
| 305 | Use Proxy | Mã cũ đã bị deprecate; không tự cấu hình proxy theo mã này. |
| 306 | Unused | Không còn được sử dụng. |
| 307 | Temporary Redirect | Chuyển tạm, giữ nguyên method/body. |
| 308 | Permanent Redirect | Chuyển lâu dài, giữ nguyên method/body. |

### 3.4. Nhóm 4xx

| Mã | Tên | Cách hiểu |
|---|---|---|
| 400 | Bad Request | Request không hợp lệ. |
| 401 | Unauthorized | Thiếu/không có credentials hợp lệ; tên dễ gây nhầm với lỗi phân quyền. |
| 402 | Payment Required | Được dành cho mục đích thanh toán; cách dùng cụ thể tùy dịch vụ. |
| 403 | Forbidden | Server từ chối cho phép thao tác. |
| 404 | Not Found | Không tìm thấy/không công bố tài nguyên. |
| 405 | Method Not Allowed | URL không hỗ trợ method đang gửi; kiểm tra header `Allow`. |
| 406 | Not Acceptable | Không có định dạng response phù hợp với thương lượng của client. |
| 407 | Proxy Authentication Required | Proxy yêu cầu xác thực, không phải login của ứng dụng đích. |
| 408 | Request Timeout | Server đợi phần request từ client quá lâu. |
| 409 | Conflict | Xung đột với trạng thái tài nguyên. |
| 410 | Gone | Tài nguyên đã bị gỡ, thường có chủ ý lâu dài. |
| 411 | Length Required | Server yêu cầu chiều dài nội dung. |
| 412 | Precondition Failed | Điều kiện như `If-Match` không thỏa. |
| 413 | Content Too Large | Request body quá lớn; tên cũ thường là Payload Too Large. |
| 414 | URI Too Long | URL/URI quá dài. |
| 415 | Unsupported Media Type | Kiểu nội dung/encoding gửi vào không hỗ trợ. |
| 416 | Range Not Satisfiable | Khoảng byte yêu cầu không thể đáp ứng. |
| 417 | Expectation Failed | Server không đáp ứng điều kiện ở header `Expect`. |
| 418 | Unused | IANA hiện ghi không sử dụng; lịch sử nổi tiếng với tên I'm a teapot. |
| 421 | Misdirected Request | Request đi tới server không phục vụ đúng origin đó. |
| 422 | Unprocessable Content | Hiểu cú pháp/kiểu nội dung nhưng không xử lý được nội dung; tên cũ là Unprocessable Entity. |
| 423 | Locked | Tài nguyên bị khóa, thường trong WebDAV; không tự suy ra trạng thái user của AlgoPrep. |
| 424 | Failed Dependency | Thao tác thất bại vì một thao tác phụ thuộc đã thất bại. |
| 425 | Too Early | Server không muốn xử lý early data có nguy cơ bị replay. |
| 426 | Upgrade Required | Cần chuyển giao thức để tiếp tục. |
| 428 | Precondition Required | Server yêu cầu request có điều kiện, thường để tránh ghi đè mất cập nhật. |
| 429 | Too Many Requests | Vượt giới hạn request áp dụng. |
| 431 | Request Header Fields Too Large | Header riêng lẻ hoặc tổng header quá lớn. |
| 451 | Unavailable For Legal Reasons | Không cung cấp tài nguyên vì yêu cầu pháp lý. |

### 3.5. Nhóm 5xx

| Mã | Tên | Cách hiểu |
|---|---|---|
| 500 | Internal Server Error | Lỗi nội bộ chưa được chuyển thành phản hồi phù hợp hơn. |
| 501 | Not Implemented | Server không hỗ trợ chức năng cần để đáp ứng request. |
| 502 | Bad Gateway | Gateway/proxy nhận phản hồi upstream không hợp lệ. |
| 503 | Service Unavailable | Không thể phục vụ lúc này, ví dụ lỗi phụ thuộc/quá tải/bảo trì. |
| 504 | Gateway Timeout | Gateway/proxy chờ upstream quá thời gian. |
| 505 | HTTP Version Not Supported | Không hỗ trợ phiên bản HTTP đang yêu cầu. |
| 506 | Variant Also Negotiates | Lỗi cấu hình thương lượng nội dung tạo vòng phụ thuộc biến thể. |
| 507 | Insufficient Storage | Server không đủ khả năng lưu để hoàn thành thao tác, thường trong WebDAV. |
| 508 | Loop Detected | Phát hiện vòng lặp khi xử lý, thường trong WebDAV. |
| 510 | Not Extended | Đăng ký cũ đã bị obsolete; không coi là mã ứng dụng hiện hành của AlgoPrep. |
| 511 | Network Authentication Required | Cần xác thực để dùng mạng, ví dụ captive portal; không phải login API. |

## 4. Giá trị ngoài chuẩn và trường hợp không có HTTP response

| Giá trị/tình huống | Cách đọc đúng |
|---|---|
| `0`, `HTTP/1.0 0` trong history ZAP | **Không phải status HTTP hợp lệ.** Trong đợt quét này có node cây placeholder chưa có response thật. Ở công cụ khác, 0 có thể biểu thị lỗi mạng/CORS/cancel; phải xem nguồn. |
| Timeout, connection refused, DNS/TLS failure | Có thể chưa nhận được HTTP status từ ứng dụng. Đừng tự đổi tất cả thành 500. |
| `419`, `420`, `440`, `444`, `499`, `509` | Không có ý nghĩa HTTP chuẩn chung trong registry IANA hiện đối chiếu. Framework/proxy có thể dùng riêng; ví dụ 499 thường xuất hiện trong log khi client ngắt kết nối. Phải tra sản phẩm phát mã, không gán vào hợp đồng AlgoPrep. |
| `520`–`526` ở Cloudflare | Mã riêng mô tả sự cố origin/kết nối/timeout/TLS; đọc [tài liệu Cloudflare](https://developers.cloudflare.com/support/troubleshooting/http-status-codes/cloudflare-5xx-errors/). Không phải mã nghiệp vụ AlgoPrep. |
| STOMP `ERROR` | Frame ở tầng WebSocket/STOMP, không phải HTTP 500. Handshake có thể đã trả 101 trước khi STOMP từ chối JWT/quyền. |
| Judge `AC`, `WA`, `TLE`, `MLE`, `CE`, `RE`, `SYSTEM_ERROR` | Kết quả chấm/thực thi, không phải HTTP status. HTTP 200 có thể chứa kết quả không AC. |

## 5. HTTP status khác `errorCode`

| Thành phần | Ví dụ | Trả lời câu hỏi gì? |
|---|---|---|
| Status ở response HTTP | `HTTP/1.1 403` | Request được xử lý/thất bại theo nhóm nào? |
| Trường `status` trong body | `"status": 403` | Mô tả HTTP status trong payload; cần khớp response thật. |
| Mã nghiệp vụ | `"errorCode": "IDT-445"` | Nguyên nhân cụ thể theo hợp đồng AlgoPrep là gì? |
| Mức ZAP | High, Medium, Low, Informational | Công cụ đánh giá cảnh báo như thế nào? Không phải status hay kết luận đã có exploit. |
| Confidence của ZAP | Low, Medium, High | Mức tin cậy heuristic của công cụ; vẫn cần kiểm chứng. |

**Không lấy ba chữ số cuối của mã nghiệp vụ làm HTTP status.** Ví dụ: `AIR-501` có thể trả HTTP **503**; `JDG-504` ở luồng submit có thể trả HTTP **503**; `HRN-062` liên quan body preview quá lớn có HTTP **413**. Cùng HTTP 403 có thể có nhiều `errorCode` khác nhau tùy controller/handler.

### Ví dụ body theo hợp đồng AlgoPrep

```http
HTTP/1.1 403 Forbidden
Content-Type: application/problem+json

{
  "type": "about:blank",
  "title": "Forbidden",
  "status": 403,
  "detail": "Permission required",
  "instance": "/api/v1/identity/admin/users",
  "errorCode": "IDT-445"
}
```

Đây là ví dụ minh họa phong bì, không khẳng định mọi endpoint 403 đều dùng `IDT-445`. `type`, `title`, `status`, `detail`, `instance` thuộc mô hình [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457.html); `errorCode`, `details` là phần mở rộng của dự án. Frontend dịch theo `errorCode`, không dùng `detail` làm thông báo người dùng.

**Giới hạn triển khai cần nhớ:** lỗi từ security filter, Spring mặc định, proxy, Tomcat hoặc dịch vụ ngoài có thể không có body, trả HTML, hoặc không có `errorCode`. Một 500 thực tế có thể chỉ có `timestamp/status/error/path`. Mẫu hợp đồng không có nghĩa mọi tầng đều đang trả đủ trường.

## 6. Ánh xạ chung trong backend

Theo [GlobalExceptionHandler](F:/prj_algoprep/05-coding/backend/algoprep-bootstrap/src/main/java/com/algoprep/bootstrap/web/GlobalExceptionHandler.java). Các advice/controller chuyên biệt có thể ghi đè, nên phải tra cả endpoint.

| Loại exception chung | HTTP |
|---|---|
| `InvalidRequestException` | 400 |
| `UnauthenticatedException` | 401 |
| `PermissionDeniedException` | 403 |
| `ResourceNotFoundException` | 404 |
| `ResourceConflictException` | 409 |
| `DomainRuleViolationException` | 422 |
| `RateLimitExceededException` | 429 |
| `ServiceUnavailableException` | 503 |
| Exception không được advice thích hợp xử lý | Có thể rơi vào cơ chế lỗi mặc định và trả 500; không có một mã nghiệp vụ chung được suy ra tự động. |

DB/quyền không đọc được ở converter JWT có thể trả **503 `IDT-503`**, không phải 401; xem [ApiAuthenticationEntryPoint](F:/prj_algoprep/05-coding/backend/algoprep-bootstrap/src/main/java/com/algoprep/bootstrap/config/ApiAuthenticationEntryPoint.java). Lỗi 401 thông thường có thể do `BearerTokenAuthenticationEntryPoint` xử lý với body rỗng.

Các validation mật khẩu/tên/profile F1 được đối chiếu thêm tại [RegistrationValidation](F:/prj_algoprep/05-coding/backend/algoprep-identity/src/main/java/com/algoprep/identity/application/command/RegistrationValidation.java) và [UpdateMyProfileCommandHandler](F:/prj_algoprep/05-coding/backend/algoprep-identity/src/main/java/com/algoprep/identity/application/command/account/UpdateMyProfileCommandHandler.java): nhiều trường hợp ném `DomainRuleViolationException` và được handler chung trả **422**, kể cả mã bắt đầu bằng `IDT-V`. Không đổi tất cả validation thành 400 theo suy đoán.

## 7. Đọc report ZAP của dự án

| Quan sát | Kết luận phù hợp |
|---|---|
| Thiếu token → 401; Student vào API Admin → 403 | Lớp bảo vệ hoạt động ở các ca đó; không ghi thêm bug chỉ vì thấy 4xx. |
| Chủ bài đọc được 200, người khác 404 | Có thể là chặn IDOR và giấu sự tồn tại đúng; phải xác minh fixture có thật. |
| HTTP 200 chứa dữ liệu không thuộc quyền người gọi | Cần điều tra lỗi phân quyền dù status thuộc nhóm thành công. |
| Payload SQL/XSS gây 500 | Chỉ đủ nói có lỗi server cần phân tích; không đủ kết luận đã injection/thực thi script. |
| Title chứa nháy → 500, nhưng đổi title duy nhất → 201 | Đợt scan đã xác minh va chạm cấp mã sau 20 lần, không chứng minh SQL injection. |
| Redis lên: lần sai thứ 6 → 429; Redis xuống: lại 401 | Vẫn sai mật khẩu, nhưng lớp giới hạn đã fail-open. Không có nghĩa đăng nhập trái phép thành công. |
| Health local → 200 kèm chi tiết DB/path | Thành công về HTTP nhưng có thông tin hạ tầng bị công khai; cần đánh giá cấu hình public. |
| HTTP sau khóa Admin → 401, WebSocket vẫn nhận bản tin mới | Chặn HTTP đúng chưa đủ; phải xử lý thu hồi session WebSocket. |
| Provider OpenRouter → 429, API review → 503 `AIR-501` | Hai tầng trả hai status khác nhau. Đợt này hết quota; không đánh dấu prompt-injection guard PASS. |

[Report đã xác minh](F:/prj_algoprep/07-review/261114-zap-full/README.md) · [Backlog cải thiện](F:/prj_algoprep/07-review/261114-zap-full/SECURITY-BACKLOG.md).

## 8. Cách tra một lỗi cụ thể

1. Ghi lại **method + URL + vai trò + dữ liệu đầu vào + HTTP status thật**.
2. Đọc `errorCode`/`details` nếu có, rồi tìm mã trong danh mục dưới đây (`Ctrl+F`).
3. Mở nguồn DD của mã và endpoint: có thể cùng mã xuất hiện ở nhiều nhánh hoặc luồng nội bộ.
4. Nếu report khác hợp đồng, đối chiếu controller/advice/security filter và log ở đúng tầng; chưa tự thay kỳ vọng TDD bằng kết quả đang lỗi.
5. Khi đánh giá bảo mật, kiểm chứng dữ liệu/quyền/tác động thực tế, không cộng số response lỗi thành số bug.

## 9. Tiền tố và quy tắc tra mã riêng của dự án

| Tiền tố | Phạm vi |
|---|---|
| `IDT` | F1 — Identity / tài khoản và quyền |
| `PRB` | F2 — Problem bank / ngân hàng bài tập |
| `HRN` | F3 — Harness / mã khung và kiểm đặc tả |
| `JDG` | F4 — Judge / nộp và chấm bài |
| `AIR` | F5 — AI review / phỏng vấn AI |
| `INB` | F6 — Interview bank / câu hỏi lý thuyết |

`-Vnn` thường dùng cho kiểm dữ liệu; `-Wnn` cho cảnh báo không chặn theo quy ước DD. **Không suy ra HTTP chỉ từ chữ V/W hoặc dải số.** Một số `HRN-*`/`HRN-V*` là lỗi/cảnh báo cổng nội bộ, được endpoint gọi ánh xạ hoặc đưa vào kết quả; không có một HTTP duy nhất áp dụng toàn hệ thống.

## 10. Danh mục mã AlgoPrep theo nguồn hiện có

Đã đối chiếu **288 mã duy nhất** trong DD API/validation, mã nguồn Java chính và thông báo giao diện; **259 mã** được nhắc trong backend `src/main/java`. Đây là độ phủ tra cứu của các nguồn đã rà, không phải số mã lỗi đã test PASS. Mã chỉ có trong DD hoặc giao diện chưa chứng minh đã có API triển khai.

Cột HTTP ưu tiên số được ghi rõ trong DD/Javadoc; nếu có nhiều số phải đọc nguồn/endpoint để chọn nhánh. `Không ghi trực tiếp` có nghĩa nguồn tra chưa cho ánh xạ chắc chắn, **không** có nghĩa mã được trả HTTP 200. Các dòng nội bộ không được tự đổi thành lỗi HTTP. Mục thông tin giữ nhãn từ bảng nguồn để tránh bỏ mất điều kiện áp dụng.

### 10.1. F1 — Identity / tài khoản và quyền — 77 mã

| Mã | HTTP được ghi trong nguồn | Ý nghĩa / điều kiện theo nguồn | Nguồn để mở đối chiếu |
|---|---|---|---|
| `IDT-101` | 409 | Thông báo giao diện: Email này đã được đăng ký | [05-coding/frontend/messages/vi.json:25](F:/prj_algoprep/05-coding/frontend/messages/vi.json:25) |
| `IDT-102` | 401 | Thông báo giao diện: Sai email hoặc mật khẩu | [05-coding/frontend/messages/vi.json:26](F:/prj_algoprep/05-coding/frontend/messages/vi.json:26) |
| `IDT-103` | 429 | Luồng: Đăng nhập sai; Ngưỡng: 5 lần / 15 phút; Khoá: `(email, IP)` | [03-dd/validation/F1_identity.md:32](F:/prj_algoprep/03-dd/validation/F1_identity.md:32) |
| `IDT-104` | 403 | Điều kiện: `status = DEACTIVATED`; Kết quả: chặn | [03-dd/validation/F1_identity.md:81](F:/prj_algoprep/03-dd/validation/F1_identity.md:81) |
| `IDT-105` | 401 | Thông báo giao diện: Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại | [05-coding/frontend/messages/vi.json:29](F:/prj_algoprep/05-coding/frontend/messages/vi.json:29) |
| `IDT-106` | 401 | Thông báo giao diện: Phiên đăng nhập không còn an toàn. Vui lòng đăng nhập lại | [05-coding/frontend/messages/vi.json:30](F:/prj_algoprep/05-coding/frontend/messages/vi.json:30) |
| `IDT-107` | 422 | Thông báo giao diện: Mã đặt lại mật khẩu không đúng hoặc đã hết hạn | [05-coding/frontend/messages/vi.json:31](F:/prj_algoprep/05-coding/frontend/messages/vi.json:31) |
| `IDT-108` | 422 | #: 38; Method: POST; Path: `/api/v1/identity/admin/users/{userId}/reset-password`; Mô tả: Đặt lại mật khẩu hộ (F1-13); Nguồn BD: `ResetUserPassword` [SoT: Suy luận]; Ghi chú: Sinh mật khẩu tạm, gửi email, thu hồi mọi refresh token của user đó. Tài khoản `hasPassword = false` (chỉ OAuth): **không** sinh mật khẩu, gửi email hướng dẫn quay lại nhà cung cấp (cùng tinh thần `422 IDT-108` ở luồng quên mật khẩu) (H2) | [03-dd/api/F1_identity.md:184](F:/prj_algoprep/03-dd/api/F1_identity.md:184) |
| `IDT-109` | 401 | Ý nghĩa: OAuth `state` không có, hết hạn, đã dùng, hoặc provider từ chối `code` (**Triển khai 2026-11-05**); Dùng cho: #6 | [03-dd/api/F1_identity.md:156](F:/prj_algoprep/03-dd/api/F1_identity.md:156) |
| `IDT-110` | 422 | Ý nghĩa: Provider chưa xác minh email: không liên kết, không tạo tài khoản (**Triển khai 2026-11-05**); Dùng cho: #6 | [03-dd/api/F1_identity.md:157](F:/prj_algoprep/03-dd/api/F1_identity.md:157) |
| `IDT-111` | 403 | Thông báo giao diện: Tài khoản này không có quyền đăng nhập cổng giảng viên | [05-coding/frontend/messages/vi.json:32](F:/prj_algoprep/05-coding/frontend/messages/vi.json:32) |
| `IDT-112` | 422 | Luồng: Sai OTP xác thực email; Ngưỡng: tối đa 5 lần trước khi buộc gửi lại; Khoá: `userId` (bộ đếm trong khoá `identity:emailverify:<userId>`); Mã lỗi: `422 IDT-112` (`03-dd/api/F1_identity.md` mục 1) `[SoT: Suy luận]`, theo mẫu OTP quên mật khẩu | [03-dd/validation/F1_identity.md:38](F:/prj_algoprep/03-dd/validation/F1_identity.md:38) |
| `IDT-113` | 403 | Điều kiện: `status = PENDING_VERIFICATION` và `must_change_password = false` (tự đăng ký chưa nhập OTP); Kết quả: chặn, không phát token; Mã lỗi: `403 IDT-113` `[SoT: Suy luận — mã đề xuất]` | [03-dd/validation/F1_identity.md:82](F:/prj_algoprep/03-dd/validation/F1_identity.md:82) |
| `IDT-114` | 401 | Thông báo giao diện: Liên kết khôi phục không còn hiệu lực. Hãy đăng nhập lại để lấy liên kết mới | [05-coding/frontend/messages/vi.json:35](F:/prj_algoprep/05-coding/frontend/messages/vi.json:35) |
| `IDT-115` | 422 | #: 13; Method: POST; Path: `/api/v1/identity/me/password`; Mô tả: Đổi mật khẩu (đã có) hoặc đặt mật khẩu lần đầu (tài khoản chỉ OAuth); Nguồn BD (operation name): `ChangeMyPassword` [SoT: `02-bd/screens/users/USR0503_settings.md:729`; từ 2026-10-20 gọi từ màn Cài đặt, không còn từ Trang cá nhân]; Ghi chú: BR-05: đặt mật khẩu lần đầu không cần mật khẩu cũ, không cần OTP thêm [SoT: `07-review/...:135`]; BR-06: đổi mật khẩu thu hồi mọi refresh token khác [SoT: `07-review/...:137`]; `currentPassword` sai trả `422 IDT-115`, không ghi dữ liệu/thu hồi phiên (owner chốt 2026-10-27). **Triển khai 2026-10-31 (`DEC-2026-1031-access-token-session-id`):** "phiên đang thao tác" nhận ra bằng claim `sid` (family id refresh token) của access token; thu hồi mọi refresh token có `family_id <> sid`; token không có `sid` trả `401` để client làm mới rồi gọi… | [03-dd/api/F1_identity.md:50](F:/prj_algoprep/03-dd/api/F1_identity.md:50) |
| `IDT-201` | 422 | Trường: Xác nhận email tự xoá tài khoản (`emailConfirmation`); Bảng: không lưu; Giới hạn: ~~phải khớp tuyệt đối `users.email` hiện tại (case-sensitive theo giá trị đã lưu, không lower-case hoá để người dùng gõ đúng những gì họ thấy)~~ **Chủ dự án chốt 2026-11-01:** trim hai đầu rồi so với `users.email` hiện tại không phân biệt hoa thường (quyết định D38 của màn Cài đặt); Mã lỗi: `IDT-201`; `IDT-432` nếu là ADMIN hoạt động duy nhất | [03-dd/validation/F1_identity.md:67](F:/prj_algoprep/03-dd/validation/F1_identity.md:67) |
| `IDT-400` | 400 / 409 | Trường: `email` (#50); Quy tắc: Quy tắc chung (trim, chữ thường, RFC 5322, tối đa 254); trùng không phân biệt hoa thường; Mã lỗi: `400 IDT-400`, `409 IDT-440` | [03-dd/validation/F1_identity.md:141](F:/prj_algoprep/03-dd/validation/F1_identity.md:141) |
| `IDT-410` | 422 / 429 | Luồng: Thử mã mời lớp sai; Ngưỡng: 5 lần / 15 phút trượt; từ lần kế tiếp chặn đến hết cửa sổ, thành công xoá bộ đếm; Khoá: `identity:class-join:failed:<userId>` trong Redis; Mã lỗi: `429 IDT-412` (`DEC-2026-1024-dd-student-class-contracts`); mã sai thứ 1–5 là `422 IDT-410` | [03-dd/validation/F1_identity.md:39](F:/prj_algoprep/03-dd/validation/F1_identity.md:39) |
| `IDT-411` | 404 | Ý nghĩa: Lớp không hiện diện trong ghi danh hiện tại của học viên; Dùng cho: #25B | [03-dd/api/F1_identity.md:118](F:/prj_algoprep/03-dd/api/F1_identity.md:118) |
| `IDT-412` | 422 / 429 | Ý nghĩa: Đã đủ năm lần thử mã mời sai trong 15 phút; Dùng cho: #25 | [03-dd/api/F1_identity.md:119](F:/prj_algoprep/03-dd/api/F1_identity.md:119) |
| `IDT-413` | 422 | Ý nghĩa: `defaultLanguage` không thuộc ba mã hợp lệ; Dùng cho: #15 | [03-dd/api/F1_identity.md:120](F:/prj_algoprep/03-dd/api/F1_identity.md:120) |
| `IDT-414` | 404 | Ý nghĩa: Lớp không thuộc phạm vi giảng viên, không có hoặc UUID sai; Dùng cho: #28, #29, #57, #58, #59 | [03-dd/api/F1_identity.md:121](F:/prj_algoprep/03-dd/api/F1_identity.md:121) |
| `IDT-415` | 409 | Ý nghĩa: Email mới đã thuộc tài khoản khác; Dùng cho: #11, #12 | [03-dd/api/F1_identity.md:122](F:/prj_algoprep/03-dd/api/F1_identity.md:122) |
| `IDT-416` | 422 | Ý nghĩa: OTP đổi email sai, hết hạn hoặc đã dùng; Dùng cho: #12 | [03-dd/api/F1_identity.md:123](F:/prj_algoprep/03-dd/api/F1_identity.md:123) |
| `IDT-420` | 422 | Ý nghĩa: Không được gỡ ô `PERMISSION_MATRIX:UPDATE` khi sau thao tác không còn **tài khoản ACTIVE** nào thuộc role gốc ADMIN nắm ô đó (BR-14; sửa 2026-11-03: trước đây đếm role, nay đếm tài khoản); Dùng cho: #31 | [03-dd/api/F1_identity.md:124](F:/prj_algoprep/03-dd/api/F1_identity.md:124) |
| `IDT-421` | 422 | Ý nghĩa: Không được xoá role hệ thống; Dùng cho: #34 | [03-dd/api/F1_identity.md:125](F:/prj_algoprep/03-dd/api/F1_identity.md:125) |
| `IDT-422` | 422 | Ý nghĩa: Không được xoá role còn người dùng gán; Dùng cho: #34 | [03-dd/api/F1_identity.md:126](F:/prj_algoprep/03-dd/api/F1_identity.md:126) |
| `IDT-423` | 422 | Ý nghĩa: Ma trận của role STUDENT chỉ đọc; Dùng cho: #31 | [03-dd/api/F1_identity.md:127](F:/prj_algoprep/03-dd/api/F1_identity.md:127) |
| `IDT-424` | 404 | Ý nghĩa: Role, function hoặc action không tồn tại, hoặc id sai dạng; Dùng cho: #31, #34 | [03-dd/api/F1_identity.md:128](F:/prj_algoprep/03-dd/api/F1_identity.md:128) |
| `IDT-425` | 409 | Ý nghĩa: Tên role trùng (không phân biệt hoa thường, so với mã hoặc nhãn); Dùng cho: #33 | [03-dd/api/F1_identity.md:129](F:/prj_algoprep/03-dd/api/F1_identity.md:129) |
| `IDT-432` | 422 | Ý nghĩa: Không để lại 0 tài khoản ADMIN hoạt động (BR-15). **Dùng thêm 2026-11-01:** ADMIN hoạt động duy nhất tự xoá tài khoản; Dùng cho: #36, #37, #18 | [03-dd/api/F1_identity.md:130](F:/prj_algoprep/03-dd/api/F1_identity.md:130) |
| `IDT-440` | 400 / 409 | Ý nghĩa: Email tài khoản Admin tạo đã tồn tại; Dùng cho: #50 | [03-dd/api/F1_identity.md:131](F:/prj_algoprep/03-dd/api/F1_identity.md:131) |
| `IDT-441` | 422 | Ý nghĩa: Vai trò đích của tài khoản Admin tạo không hợp lệ; Dùng cho: #50 | [03-dd/api/F1_identity.md:132](F:/prj_algoprep/03-dd/api/F1_identity.md:132) |
| `IDT-442` | 422 | Ý nghĩa: Thời hạn lưu log không phải số nguyên từ 1 đến 1.000.000, thiếu trường, hoặc chính sách không phải `DELETE`/`ARCHIVE`; Dùng cho: #52 | [03-dd/api/F1_identity.md:133](F:/prj_algoprep/03-dd/api/F1_identity.md:133) |
| `IDT-443` | 400 | Ý nghĩa: Bộ lọc ngày/nhóm/cursor hoặc cột CSV của log sai; Dùng cho: #39, #40 | [03-dd/api/F1_identity.md:134](F:/prj_algoprep/03-dd/api/F1_identity.md:134) |
| `IDT-444` | 400 | Ý nghĩa: Từ khoá tìm log dài quá 100 ký tự; Dùng cho: #39, #40 | [03-dd/api/F1_identity.md:135](F:/prj_algoprep/03-dd/api/F1_identity.md:135) |
| `IDT-445` | 403 | Ý nghĩa: Người gọi đã xác thực nhưng thiếu quyền `Function:Action` bắt buộc trên endpoint F1; Dùng cho: Các endpoint F1 gác ma trận quyền | [03-dd/api/F1_identity.md:136](F:/prj_algoprep/03-dd/api/F1_identity.md:136) |
| `IDT-446` | 503 | Ý nghĩa: Ghi audit `ROLE_DELETED` lỗi; rollback xoá role/quyền; Dùng cho: #34 | [03-dd/api/F1_identity.md:137](F:/prj_algoprep/03-dd/api/F1_identity.md:137) |
| `IDT-447` | 403 | Ý nghĩa: JWT hợp lệ nhưng `baseCategory` không phải `ADMIN` ở endpoint nhật ký chỉ dành cho ADMIN; Dùng cho: #39, #40, #64–66 | [03-dd/api/F1_identity.md:138](F:/prj_algoprep/03-dd/api/F1_identity.md:138) |
| `IDT-448` | 422 | Ý nghĩa: Không thể mở lại tài khoản đã ẩn danh hoá (`anonymized_at IS NOT NULL`); không thay đổi dữ liệu/phiên/audit thành công. **Dùng thêm 2026-11-02:** #38 cho tài khoản đã ẩn danh (email `@anon.local`, không có nơi gửi); Dùng cho: #37, #38 | [03-dd/api/F1_identity.md:139](F:/prj_algoprep/03-dd/api/F1_identity.md:139) |
| `IDT-449` | 429 | Ý nghĩa: Quá 50 người xem luồng nhật ký trực tiếp trên một instance; Dùng cho: #66 | [03-dd/api/F1_identity.md:140](F:/prj_algoprep/03-dd/api/F1_identity.md:140) |
| `IDT-450` | 404 | Ý nghĩa: Không có tài khoản hoặc `userId` sai dạng ở thao tác quản trị một người dùng (**Triển khai 2026-11-02**, `[SoT: Suy luận]`, chủ dự án duyệt, `DEC-2026-1102-admin-user-management-contracts`); Dùng cho: #36, #37, #38 | [03-dd/api/F1_identity.md:141](F:/prj_algoprep/03-dd/api/F1_identity.md:141) |
| `IDT-503` | 503 | Ý nghĩa: Không đọc được trạng thái tài khoản hoặc ma trận quyền từ PostgreSQL khi xác thực token; request bị từ chối (fail closed), thân phản hồi `application/problem+json`, client thử lại thay vì đăng xuất (`DEC-2026-1031-auth-read-failure-503`, mã HTTP là suy luận `[SoT: Suy luận]`); Dùng cho: Mọi endpoint cần JWT | [03-dd/api/F1_identity.md:154](F:/prj_algoprep/03-dd/api/F1_identity.md:154) |
| `IDT-504` | 503 | Ý nghĩa: Không gửi được email xác thực (SMTP lỗi) khi đăng ký hoặc gửi lại mã (**Triển khai 2026-11-05**, owner chốt giữ lỗi hiện thay vì trả 200 như quên mật khẩu): đăng ký rollback không tạo tài khoản, mã phát hành được dọn; thân lỗi không chứa địa chỉ email; Dùng cho: #1, #53, #54 | [03-dd/api/F1_identity.md:155](F:/prj_algoprep/03-dd/api/F1_identity.md:155) |
| `IDT-505` | 503 | Ý nghĩa: Provider OAuth chưa cấu hình (thiếu client id hoặc secret) hoặc không trả lời (**Triển khai 2026-11-05**, `[SoT: Suy luận]`); Dùng cho: #5, #6 | [03-dd/api/F1_identity.md:158](F:/prj_algoprep/03-dd/api/F1_identity.md:158) |
| `IDT-506` | 503 | Ý nghĩa: Một khối Tổng quan quản trị không đọc được nguồn F4 hoặc F2 (hết hạn hoặc lỗi), chỉ khối đó lỗi (**Triển khai 2026-11-07**; **Owner chốt 2026-11-07**, A11); thân lỗi không có chi tiết nguồn; Dùng cho: #43, #67 đến #72 | [03-dd/api/F1_identity.md:159](F:/prj_algoprep/03-dd/api/F1_identity.md:159) |
| `IDT-507` | 503 | Ý nghĩa: Số liệu F4 hoặc danh sách bài giao F2 mà một lần đọc lớp cần không đọc được (hết hạn hoặc lỗi); thân lỗi không có chi tiết nguồn, không thay bằng số 0 (**Triển khai 2026-11-07**; **Owner chốt 2026-11-07**, A7; trước đó DD ghi "503 chung" chưa có mã); Dùng cho: #28, #29, #56 đến #59 (riêng #26 không trả mã này mà hạ thành `200` với số liệu `null`, xem ghi chú 2026-11-07) | [03-dd/api/F1_identity.md:160](F:/prj_algoprep/03-dd/api/F1_identity.md:160) |
| `IDT-508` | 503 | Ý nghĩa: Lịch sử nộp bài F4 mà tệp xuất `scope=submissions` cần không đọc được (hết hạn 20 giây hoặc lỗi); thân lỗi không có chi tiết nguồn, hạn mức được trả lại bằng `DEL` khoá Redis để học viên thử lại ngay (**Triển khai 2026-11-07**; **Owner chốt 2026-11-07**, B7); Dùng cho: #16 (`scope=submissions`) | [03-dd/api/F1_identity.md:161](F:/prj_algoprep/03-dd/api/F1_identity.md:161) |
| `IDT-509` | 503 | Ý nghĩa: Cổng đọc hội thoại phỏng vấn của F5 không trả lời (lỗi hoặc quá 5 giây) trong tệp xuất #17; thân lỗi không có chi tiết nguồn, hạn mức được trả lại để thử lại ngay (**Triển khai 2026-11-11**, F5-9; số mã `IDT-509` do code chọn, chờ owner xác nhận); Dùng cho: #17 | [03-dd/api/F1_identity.md:162](F:/prj_algoprep/03-dd/api/F1_identity.md:162) |
| `IDT-V01` | 422 | Quy tắc: Độ dài tối thiểu; Giá trị: 8 ký tự HTTP 422 đã đối chiếu RegistrationValidation/UpdateMyProfileCommandHandler và GlobalExceptionHandler; không suy ra từ tên mã. | [03-dd/validation/F1_identity.md:14](F:/prj_algoprep/03-dd/validation/F1_identity.md:14) |
| `IDT-V02` | 422 | Quy tắc: Độ dài tối đa; Giá trị: **72 byte UTF-8**, giữ BCrypt; không cắt chuỗi. Chủ dự án chốt 2026-10-30 (`DEC-2026-1030-bcrypt-password-byte-limit`), thay giới hạn 128 ký tự cũ. Ký tự ngoài ASCII có thể chiếm nhiều byte HTTP 422 đã đối chiếu RegistrationValidation/UpdateMyProfileCommandHandler và GlobalExceptionHandler; không suy ra từ tên mã. | [03-dd/validation/F1_identity.md:15](F:/prj_algoprep/03-dd/validation/F1_identity.md:15) |
| `IDT-V03` | 422 | Quy tắc: Thành phần bắt buộc; Giá trị: ít nhất 1 chữ cái, ít nhất 1 chữ số HTTP 422 đã đối chiếu RegistrationValidation/UpdateMyProfileCommandHandler và GlobalExceptionHandler; không suy ra từ tên mã. | [03-dd/validation/F1_identity.md:16](F:/prj_algoprep/03-dd/validation/F1_identity.md:16) |
| `IDT-V04` | 422 | Quy tắc: Không được trùng; Giá trị: không được trùng chính email/`display_name` của tài khoản (chống mật khẩu đoán ngay) HTTP 422 đã đối chiếu RegistrationValidation/UpdateMyProfileCommandHandler và GlobalExceptionHandler; không suy ra từ tên mã. | [03-dd/validation/F1_identity.md:17](F:/prj_algoprep/03-dd/validation/F1_identity.md:17) |
| `IDT-V05` | 429 | Luồng: Đổi email (đã đăng nhập); Ngưỡng: 3 lần / 24 giờ, **tính cả gửi lại OTP** qua cùng #11; Khoá: `userId`; Mã lỗi: `IDT-V05` (`429`); owner chốt `DEC-2026-1025-dd-email-resend-budget` | [03-dd/validation/F1_identity.md:35](F:/prj_algoprep/03-dd/validation/F1_identity.md:35) |
| `IDT-V06` | 429 | Luồng: Xuất dữ liệu cá nhân (`/me/export*`); Ngưỡng: 1 lần / 5 phút / loại tệp; Khoá: `userId + scope`; Mã lỗi: `IDT-V06` (`429`) [SoT: `07-review/...:148`] | [03-dd/validation/F1_identity.md:36](F:/prj_algoprep/03-dd/validation/F1_identity.md:36) |
| `IDT-V07` | Không ghi trực tiếp | Trường: `current_position`; Bảng: `users`; Giới hạn: 100 ký tự [SoT: `02-bd/screens/users/USR0502_profile.md:353`] | [03-dd/validation/F1_identity.md:59](F:/prj_algoprep/03-dd/validation/F1_identity.md:59) |
| `IDT-V08` | 422 | Trường: `target_position` (mới, xem `03-dd/api/F1_identity.md` mục 4 và `07-review/...:130`); Bảng: `users`; Giới hạn: 100 ký tự, cùng giới hạn `current_position` HTTP 422 đã đối chiếu RegistrationValidation/UpdateMyProfileCommandHandler và GlobalExceptionHandler; không suy ra từ tên mã. | [03-dd/validation/F1_identity.md:61](F:/prj_algoprep/03-dd/validation/F1_identity.md:61) |
| `IDT-V09` | Không ghi trực tiếp | Trường: `school_or_company`; Bảng: `users`; Giới hạn: 150 ký tự [SoT: `02-bd/screens/users/USR0502_profile.md:352`] | [03-dd/validation/F1_identity.md:62](F:/prj_algoprep/03-dd/validation/F1_identity.md:62) |
| `IDT-V10` | 422 | Ý nghĩa: `room`/`note` của lịch lớp quá dài (chủ dự án chốt 2026-11-01: 422 cho vi phạm quy tắc/trường, theo `IDT-V13`); Dùng cho: #20, #21 | [03-dd/api/F1_identity.md:142](F:/prj_algoprep/03-dd/api/F1_identity.md:142) |
| `IDT-V11` | 400 | Thông báo giao diện: Yêu cầu không hợp lệ. Vui lòng tải lại trang | [05-coding/frontend/messages/vi.json:83](F:/prj_algoprep/05-coding/frontend/messages/vi.json:83) |
| `IDT-V12` | 429 | Luồng: Gửi lại OTP xác thực email, chỉ tài khoản tự đăng ký (`POST /auth/email/verify/resend`; Sửa 2026-10-22 (đồng bộ DB vòng 2, DEC-2026-1022-db-sync-round2)); Ngưỡng: 1 lần / 60 giây; Khoá: `userId` (khoá Redis `identity:emailverify:resend:<userId>`); Mã lỗi: `IDT-V12` (`429`) — **đề xuất phase 5**, theo mẫu gửi lại OTP quên mật khẩu `[SoT: Suy luận]`; `02-bd/database/F1_identity.md:221` | [03-dd/validation/F1_identity.md:37](F:/prj_algoprep/03-dd/validation/F1_identity.md:37) |
| `IDT-V13` | Không ghi trực tiếp | Ý nghĩa: `room`/`note` của lịch lớp quá dài (chủ dự án chốt 2026-11-01: 422 cho vi phạm quy tắc/trường, theo `IDT-V13`); Dùng cho: #20, #21 | [03-dd/api/F1_identity.md:142](F:/prj_algoprep/03-dd/api/F1_identity.md:142) |
| `IDT-V14` | 422 | Ý nghĩa: Tên lớp rỗng hoặc quá 120 ký tự (như trên); Dùng cho: #20, #21 | [03-dd/api/F1_identity.md:143](F:/prj_algoprep/03-dd/api/F1_identity.md:143) |
| `IDT-V15` | 422 | Ý nghĩa: Mô tả lớp quá 500 ký tự (như trên); Dùng cho: #20, #21 | [03-dd/api/F1_identity.md:144](F:/prj_algoprep/03-dd/api/F1_identity.md:144) |
| `IDT-V16` | 422 | Ý nghĩa: Cấu trúc lịch học sai (như trên); Dùng cho: #20, #21 | [03-dd/api/F1_identity.md:145](F:/prj_algoprep/03-dd/api/F1_identity.md:145) |
| `IDT-V17` | 400 | Ý nghĩa: Bộ lọc hoặc phân trang của danh sách người dùng sai: `status`/`role` lạ, `q` quá 100 ký tự, `page` < 1, `pageSize` ngoài 1 đến 100 (**Triển khai 2026-11-02**; mã đã nêu ở danh sách lớp của giảng viên); Dùng cho: #35 | [03-dd/api/F1_identity.md:150](F:/prj_algoprep/03-dd/api/F1_identity.md:150) |
| `IDT-V18` | 422 | Trường: `preferredHintAllowed`; Quy tắc: Boolean | [03-dd/validation/F1_identity.md:92](F:/prj_algoprep/03-dd/validation/F1_identity.md:92) |
| `IDT-V19` | Không ghi trực tiếp | Trường: `displayName` (#50); Quy tắc: 2 đến 100 ký tự sau trim | [03-dd/validation/F1_identity.md:140](F:/prj_algoprep/03-dd/validation/F1_identity.md:140) |
| `IDT-V20` | 422 | Trường: `displayName` (#50); Quy tắc: 2 đến 100 ký tự sau trim HTTP 422 đã đối chiếu RegistrationValidation/UpdateMyProfileCommandHandler và GlobalExceptionHandler; không suy ra từ tên mã. | [03-dd/validation/F1_identity.md:140](F:/prj_algoprep/03-dd/validation/F1_identity.md:140) |
| `IDT-V21` | 400 | Ý nghĩa: Thiếu, `null`, `false` hoặc sai kiểu `acknowledgeSelfImpact` khi admin tự khoá hoặc tự hạ vai trò mình (xem mục 0.6 và BR-15); Dùng cho: #36, #37 | [03-dd/api/F1_identity.md:151](F:/prj_algoprep/03-dd/api/F1_identity.md:151) |
| `IDT-V22` | 422 | Ý nghĩa: Tên role rỗng sau khi trim; Dùng cho: #33 | [03-dd/api/F1_identity.md:146](F:/prj_algoprep/03-dd/api/F1_identity.md:146) |
| `IDT-V23` | 422 | Ý nghĩa: Tên role quá 50 ký tự sau khi trim; Dùng cho: #33 | [03-dd/api/F1_identity.md:147](F:/prj_algoprep/03-dd/api/F1_identity.md:147) |
| `IDT-V24` | 422 | Ý nghĩa: `baseCategory` không thuộc `STUDENT`, `INSTRUCTOR`, `ADMIN`; Dùng cho: #33 | [03-dd/api/F1_identity.md:148](F:/prj_algoprep/03-dd/api/F1_identity.md:148) |
| `IDT-V25` | 422 | Ý nghĩa: Thiếu cờ `granted`; Dùng cho: #31 | [03-dd/api/F1_identity.md:149](F:/prj_algoprep/03-dd/api/F1_identity.md:149) |
| `IDT-V26` | 422 | Ý nghĩa: Thân #36/#37 sai (**Triển khai 2026-11-02**, `[SoT: Suy luận]`): `targetRoleId` thiếu hoặc sai dạng, `targetStatus` không phải `ACTIVE`/`DEACTIVATED`, `reason` không phải chuỗi hoặc quá 500 ký tự, `notifyByEmail` không phải boolean, hoặc `notifyByEmail = true` mà thiếu `reason`; Dùng cho: #36, #37 | [03-dd/api/F1_identity.md:152](F:/prj_algoprep/03-dd/api/F1_identity.md:152) |
| `IDT-V27` | 422 | Ý nghĩa: `problemIds` của #48 thiếu, rỗng, có phần tử sai dạng UUID hoặc quá 100 id (**Triển khai 2026-11-05**, `[SoT: Suy luận]`, owner chốt ngày 2026-11-05); Dùng cho: #48 | [03-dd/api/F1_identity.md:153](F:/prj_algoprep/03-dd/api/F1_identity.md:153) |
| `IDT-V28` | 400 | Ý nghĩa: `provider` ở đường dẫn không phải `github` hoặc `google` (**Triển khai 2026-11-05**, `[SoT: Suy luận]`); Dùng cho: #5, #6 | [03-dd/api/F1_identity.md:163](F:/prj_algoprep/03-dd/api/F1_identity.md:163) |

### 10.2. F2 — Problem bank / ngân hàng bài tập — 49 mã

| Mã | HTTP được ghi trong nguồn | Ý nghĩa / điều kiện theo nguồn | Nguồn để mở đối chiếu |
|---|---|---|---|
| `PRB-001` | 400 | Tên enum: `VALIDATION_FAILED`; Ý nghĩa: trường sai, thiếu, quá độ dài; `details.fields[] = { field, rule }`; Mục dùng: mọi endpoint ghi | [03-dd/api/F2_problem-bank.md:3068](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3068) |
| `PRB-002` | 403 | Tên enum: `FORBIDDEN_FUNCTION`; Ý nghĩa: thiếu `FUNCTION:ACTION` cần thiết; Mục dùng: mọi endpoint có quyền | [03-dd/api/F2_problem-bank.md:3069](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3069) |
| `PRB-003` | 403 | Tên enum: `NOT_PROBLEM_AUTHOR`; Ý nghĩa: A2 ghi lên bài `PUBLISHED` của người khác; Mục dùng: 29-33, 35-45, 46-49 | [03-dd/api/F2_problem-bank.md:3070](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3070) |
| `PRB-004` | 403 | Tên enum: `ADMIN_ONLY`; Ý nghĩa: có Function nhưng không phải vai trò `ADMIN` (danh mục chủ đề, độ khó); Mục dùng: 16-19, 21-24 | [03-dd/api/F2_problem-bank.md:3071](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3071) |
| `PRB-005` | 403 | Tên enum: `NOT_CLASS_INSTRUCTOR`; Ý nghĩa: không phụ trách lớp; Mục dùng: 50-54 | [03-dd/api/F2_problem-bank.md:3072](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3072) |
| `PRB-010` | 404 | Tên enum: `PROBLEM_NOT_FOUND`; Ý nghĩa: bài không tồn tại, `deleted`, chưa xuất bản (với học viên) hoặc bản nháp của người khác (với A2); Mục dùng: nhiều | [03-dd/api/F2_problem-bank.md:3073](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3073) |
| `PRB-011` | 404 | Tên enum: `TESTCASE_NOT_FOUND`; Ý nghĩa: testcase không tồn tại hoặc không thuộc bài; Mục dùng: 43, 44, 48 | [03-dd/api/F2_problem-bank.md:3074](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3074) |
| `PRB-012` | 404 | Tên enum: `TOPIC_NOT_FOUND`; Ý nghĩa: chủ đề (ở đường dẫn) không tồn tại; Mục dùng: 22, 24 | [03-dd/api/F2_problem-bank.md:3075](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3075) |
| `PRB-013` | 404 | Tên enum: `LEVEL_NOT_FOUND`; Ý nghĩa: mức độ khó (ở đường dẫn) không tồn tại; Mục dùng: 17, 19 | [03-dd/api/F2_problem-bank.md:3076](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3076) |
| `PRB-014` | 404 | Tên enum: `BOOKMARK_NOT_FOUND`; Ý nghĩa: sửa ghi chú khi bookmark đã mất; Mục dùng: 10 | [03-dd/api/F2_problem-bank.md:3077](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3077) |
| `PRB-015` | 404 | Tên enum: `MY_CLASS_NOT_FOUND`; Ý nghĩa: `classId` sai dạng, lớp không có, người gọi đã rời hoặc không thuộc lớp; Mục dùng: 5A | [03-dd/api/F2_problem-bank.md:3078](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3078) |
| `PRB-020` | 409 | Tên enum: `CONTENT_VERSION_CONFLICT`; Ý nghĩa: `expectedUpdatedAt` lệch; `details.currentUpdatedAt`; Mục dùng: 37, 38, 39, 40 | [03-dd/api/F2_problem-bank.md:3079](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3079) |
| `PRB-021` | 409 | Tên enum: `TOPIC_NAME_DUPLICATE`; Ý nghĩa: tên chủ đề trùng; Mục dùng: 21, 22 | [03-dd/api/F2_problem-bank.md:3080](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3080) |
| `PRB-022` | 409 | Tên enum: `LEVEL_NAME_DUPLICATE`; Ý nghĩa: tên độ khó trùng; Mục dùng: 16, 17 | [03-dd/api/F2_problem-bank.md:3081](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3081) |
| `PRB-023` | 409 | Tên enum: `GENERATION_IN_PROGRESS`; Ý nghĩa: đang có lượt sinh testcase của bài; Mục dùng: 49 | [03-dd/api/F2_problem-bank.md:3082](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3082) |
| `PRB-024` | Không ghi trực tiếp | DỰ PHÒNG / ĐỂ TRỐNG, không phải mã lỗi đang được định nghĩa để trả: Mã `PRB-024`, `PRB-026` tới `PRB-029`, `PRB-034`, `PRB-035`, `PRB-038` để trống có chủ đích (dự phòng theo nhóm). | [03-dd/api/F2_problem-bank.md:3112](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3112) |
| `PRB-025` | 409 | Tên enum: `ASSIGNMENT_ALREADY_REMOVED`; Ý nghĩa: dòng giao bài không còn hoạt động; Mục dùng: 54 | [03-dd/api/F2_problem-bank.md:3083](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3083) |
| `PRB-026` | Không ghi trực tiếp | DỰ PHÒNG / ĐỂ TRỐNG, không phải mã lỗi đang được định nghĩa để trả: Mã `PRB-024`, `PRB-026` tới `PRB-029`, `PRB-034`, `PRB-035`, `PRB-038` để trống có chủ đích (dự phòng theo nhóm). | [03-dd/api/F2_problem-bank.md:3112](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3112) |
| `PRB-029` | Không ghi trực tiếp | DỰ PHÒNG / ĐỂ TRỐNG, không phải mã lỗi đang được định nghĩa để trả: Mã `PRB-024`, `PRB-026` tới `PRB-029`, `PRB-034`, `PRB-035`, `PRB-038` để trống có chủ đích (dự phòng theo nhóm). | [03-dd/api/F2_problem-bank.md:3112](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3112) |
| `PRB-030` | 422 | Tên enum: `PUBLISH_CHECKLIST_NOT_MET`; Ý nghĩa: chưa đạt checklist xuất bản; `details.items[]`; Mục dùng: 29, 40 | [03-dd/api/F2_problem-bank.md:3084](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3084) |
| `PRB-031` | 422 | Tên enum: `TOPIC_IN_USE`; Ý nghĩa: chủ đề còn bài tham chiếu; `details.referencingProblemCount`; Mục dùng: 24 | [03-dd/api/F2_problem-bank.md:3085](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3085) |
| `PRB-032` | 422 | Tên enum: `LEVEL_IN_USE`; Ý nghĩa: độ khó còn bài tham chiếu; `reason = IN_USE`, `referencingProblemCount`; Mục dùng: 19 | [03-dd/api/F2_problem-bank.md:3086](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3086) |
| `PRB-033` | 422 | Tên enum: `LEVEL_LAST_ONE`; Ý nghĩa: mức cuối cùng; `reason = LAST_LEVEL`; Mục dùng: 19 | [03-dd/api/F2_problem-bank.md:3087](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3087) |
| `PRB-034` | Không ghi trực tiếp | DỰ PHÒNG / ĐỂ TRỐNG, không phải mã lỗi đang được định nghĩa để trả: Mã `PRB-024`, `PRB-026` tới `PRB-029`, `PRB-034`, `PRB-035`, `PRB-038` để trống có chủ đích (dự phòng theo nhóm). | [03-dd/api/F2_problem-bank.md:3112](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3112) |
| `PRB-035` | Không ghi trực tiếp | DỰ PHÒNG / ĐỂ TRỐNG, không phải mã lỗi đang được định nghĩa để trả: Mã `PRB-024`, `PRB-026` tới `PRB-029`, `PRB-034`, `PRB-035`, `PRB-038` để trống có chủ đích (dự phòng theo nhóm). | [03-dd/api/F2_problem-bank.md:3112](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3112) |
| `PRB-036` | 422 | Tên enum: `FUNCTION_SPEC_REJECTED`; Ý nghĩa: `FunctionSpecEvaluator` báo vi phạm cứng; `details.violations[] = { code: "HRN-Vnn", path, message }` (gồm cả `HRN-V20`, `HRN-V22`); Mục dùng: 38 | [03-dd/api/F2_problem-bank.md:3088](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3088) |
| `PRB-037` | 422 | Tên enum: `PROBLEM_NOT_ASSIGNABLE`; Ý nghĩa: bài không còn xuất bản, không giao thêm cho lớp; Mục dùng: 53 | [03-dd/api/F2_problem-bank.md:3089](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3089) |
| `PRB-038` | Không ghi trực tiếp | DỰ PHÒNG / ĐỂ TRỐNG, không phải mã lỗi đang được định nghĩa để trả: Mã `PRB-024`, `PRB-026` tới `PRB-029`, `PRB-034`, `PRB-035`, `PRB-038` để trống có chủ đích (dự phòng theo nhóm). | [03-dd/api/F2_problem-bank.md:3112](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3112) |
| `PRB-039` | 422 | Tên enum: `REFERENCE_NOT_FOUND`; Ý nghĩa: `levelId`/`topicId`/`topicIds[i]` trong thân yêu cầu không còn tồn tại; `details.field`; Mục dùng: 31, 32, 36, 37 | [03-dd/api/F2_problem-bank.md:3090](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3090) |
| `PRB-040` | 422 | Tên enum: `REFERENCE_SOLUTION_MISSING`; Ý nghĩa: chưa có đáp án mẫu; Mục dùng: 48, 49 | [03-dd/api/F2_problem-bank.md:3091](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3091) |
| `PRB-041` | 422 | Tên enum: `GENERATION_PRECONDITION_FAILED`; Ý nghĩa: thiếu điều kiện sinh; `details.reasons[]`; Mục dùng: 49 | [03-dd/api/F2_problem-bank.md:3092](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3092) |
| `PRB-042` | 422 | Tên enum: `GENERATION_QUOTA_EXHAUSTED`; Ý nghĩa: hết lượt sinh của bài; `details = { used, limit }`; Mục dùng: 49 | [03-dd/api/F2_problem-bank.md:3093](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3093) |
| `PRB-043` | 422 | Tên enum: `GENERATOR_SCRIPT_REJECTED`; Ý nghĩa: script bị loại; `details.reason`; Mục dùng: 49 | [03-dd/api/F2_problem-bank.md:3094](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3094) |
| `PRB-044` | 422 | Tên enum: `UPLOAD_SESSION_INVALID`; Ý nghĩa: phiên tải lên không có, hết hạn, sai bài hoặc sai người; Mục dùng: 47 | [03-dd/api/F2_problem-bank.md:3095](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3095) |
| `PRB-045` | 422 | Tên enum: `UPLOAD_OBJECT_INVALID`; Ý nghĩa: thiếu object, sai kích thước hoặc sai mã hoá; `details.items[]`; Mục dùng: 47 | [03-dd/api/F2_problem-bank.md:3096](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3096) |
| `PRB-046` | 422 | Tên enum: `TESTCASE_LIMIT_EXCEEDED`; Ý nghĩa: bài sẽ vượt 500 testcase; Mục dùng: 42, 46 | [03-dd/api/F2_problem-bank.md:3097](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3097) |
| `PRB-047` | 422 | Tên enum: `BULK_UPLOAD_LIMIT_EXCEEDED`; Ý nghĩa: vượt số cặp hoặc dung lượng một lô; Mục dùng: 46 | [03-dd/api/F2_problem-bank.md:3098](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3098) |
| `PRB-048` | 422 | Tên enum: `TESTCASE_CONTENT_TOO_LARGE`; Ý nghĩa: một vế vượt 1 MB khi nhập trực tiếp; Mục dùng: 42, 43 | [03-dd/api/F2_problem-bank.md:3099](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3099) |
| `PRB-049` | 422 | Tên enum: `REORDER_SET_MISMATCH`; Ý nghĩa: danh sách thứ tự không bằng tập hiện có; Mục dùng: 18, 23, 45 | [03-dd/api/F2_problem-bank.md:3100](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3100) |
| `PRB-050` | 503 | Tên enum: `SANDBOX_UNAVAILABLE`; Ý nghĩa: go-judge hoặc bộ so khớp không đáp ứng; Mục dùng: 29, 40, 48, 49 | [03-dd/api/F2_problem-bank.md:3101](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3101) |
| `PRB-051` | 503 | Tên enum: `AI_UNAVAILABLE`; Ý nghĩa: đường AI không đáp ứng hoặc hết hạn mức; Mục dùng: 49 | [03-dd/api/F2_problem-bank.md:3102](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3102) |
| `PRB-052` | 503 | Tên enum: `OBJECT_STORAGE_UNAVAILABLE`; Ý nghĩa: MinIO không đáp ứng; Mục dùng: 42, 46, 47 | [03-dd/api/F2_problem-bank.md:3103](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3103) |
| `PRB-053` | 422 | Tên enum: `WORKED_EXAMPLES_BELOW_MINIMUM`; Ý nghĩa: bài `PUBLISHED` sẽ còn dưới 2 ví dụ mẫu; Mục dùng: 37 | [03-dd/api/F2_problem-bank.md:3104](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3104) |
| `PRB-054` | 422 | Tên enum: `APPROVED_TESTCASES_BELOW_MINIMUM`; Ý nghĩa: bài `PUBLISHED` sẽ còn dưới 8 ca đã duyệt hoặc 2 Sample đã duyệt; Mục dùng: 43, 44 | [03-dd/api/F2_problem-bank.md:3105](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3105) |
| `PRB-055` | 429 | Tên enum: `REFERENCE_RUN_RATE_LIMITED`; Ý nghĩa: quá 10 lượt chạy đáp án mẫu trong 60 giây trượt theo tài khoản; Mục dùng: 48 | [03-dd/api/F2_problem-bank.md:3106](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3106) |
| `PRB-056` | 503 | Tên enum: `OUTBOX_WRITE_FAILED`; Ý nghĩa: lỗi ghi Outbox; §38 rollback toàn bộ lưu spec, §53 rollback riêng bài và tiếp tục lô; Mục dùng: 38, 53 | [03-dd/api/F2_problem-bank.md:3107](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3107) |
| `PRB-057` | 503 | Tên enum: `SOLVE_STATE_UNAVAILABLE`; Ý nghĩa: không đọc được trạng thái giải của người gọi khi endpoint không được phép trả số giả (tiến độ theo mức độ); owner duyệt 2026-11-05; Mục dùng: 14A | [03-dd/api/F2_problem-bank.md:3108](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3108) |
| `PRB-058` | 503 | Tên enum: `CLASS_SERVICE_UNAVAILABLE`; Ý nghĩa: không đọc được danh sách lớp của giảng viên từ `identity` nên không kiểm được quyền sở hữu lớp; owner duyệt 2026-11-06; Mục dùng: 50 đến 55 | [03-dd/api/F2_problem-bank.md:3109](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3109) |
| `PRB-W01` | 200 | Tên enum: `TYPE_OUTSIDE_SCHEMA`; Ý nghĩa: vi phạm mềm `HRN-V01` tới `HRN-V07`, `functionWrapperSupported = false`; mã gốc trong `reasonCode`; Mục dùng: 38 | [03-dd/api/F2_problem-bank.md:3110](F:/prj_algoprep/03-dd/api/F2_problem-bank.md:3110) |

### 10.3. F3 — Harness / mã khung và kiểm đặc tả — 40 mã

| Mã | HTTP được ghi trong nguồn | Ý nghĩa / điều kiện theo nguồn | Nguồn để mở đối chiếu |
|---|---|---|---|
| `HRN-001` | 404 | Tên: `SPEC_NOT_FOUND`; Cổng phát: P1, P2; Bản chất: Dữ liệu bài | [03-dd/api/F3_harness.md:577](F:/prj_algoprep/03-dd/api/F3_harness.md:577) |
| `HRN-002` | Không ghi trực tiếp | Tên: `SPEC_VERSION_MISMATCH`; Cổng phát: P2; Bản chất: Đặc tả đổi giữa chừng | [03-dd/api/F3_harness.md:578](F:/prj_algoprep/03-dd/api/F3_harness.md:578) |
| `HRN-003` | 503 | Tên: `SPEC_READER_UNAVAILABLE`; Cổng phát: P1, P2; Bản chất: Hạ tầng tạm thời | [03-dd/api/F3_harness.md:579](F:/prj_algoprep/03-dd/api/F3_harness.md:579) |
| `HRN-004` | 422 | Tên: `WRAPPER_NOT_SUPPORTED`; Cổng phát: P1, P2; Bản chất: Quy tắc nghiệp vụ F3-13 | [03-dd/api/F3_harness.md:580](F:/prj_algoprep/03-dd/api/F3_harness.md:580) |
| `HRN-005` | 422 | Tên: `SPEC_SIGNATURE_MISSING`; Cổng phát: P1, P2; Bản chất: Dữ liệu bài | [03-dd/api/F3_harness.md:581](F:/prj_algoprep/03-dd/api/F3_harness.md:581) |
| `HRN-006` | 500 | Tên: `SPEC_INVALID_ON_READ`; Cổng phát: P1, P2; Bản chất: Lỗi dữ liệu cũ hoặc lỗi bên kia | [03-dd/api/F3_harness.md:582](F:/prj_algoprep/03-dd/api/F3_harness.md:582) |
| `HRN-010` | 400 | Tên: `LANGUAGE_UNSUPPORTED`; Cổng phát: P1, P2, P3; Bản chất: Đầu vào | [03-dd/api/F3_harness.md:583](F:/prj_algoprep/03-dd/api/F3_harness.md:583) |
| `HRN-020` | Không ghi trực tiếp | Tên: `SOURCE_BLANK`; Cổng phát: P2; Bản chất: Đầu vào | [03-dd/api/F3_harness.md:584](F:/prj_algoprep/03-dd/api/F3_harness.md:584) |
| `HRN-021` | Không ghi trực tiếp | Tên: `SOURCE_TOO_LARGE`; Cổng phát: P2; Bản chất: Đầu vào | [03-dd/api/F3_harness.md:585](F:/prj_algoprep/03-dd/api/F3_harness.md:585) |
| `HRN-022` | Không ghi trực tiếp | Tên: `SOURCE_INVALID_CHARS`; Cổng phát: P2; Bản chất: Đầu vào | [03-dd/api/F3_harness.md:586](F:/prj_algoprep/03-dd/api/F3_harness.md:586) |
| `HRN-030` | 500 | Tên: `TEMPLATE_CORRUPT`; Cổng phát: P1, P2; Bản chất: Lỗi hệ thống (đáng ra phát hiện lúc khởi động) | [03-dd/api/F3_harness.md:587](F:/prj_algoprep/03-dd/api/F3_harness.md:587) |
| `HRN-031` | 500 | Tên: `CODEGEN_INTERNAL`; Cổng phát: P1..P4; Bản chất: Lỗi hệ thống | [03-dd/api/F3_harness.md:588](F:/prj_algoprep/03-dd/api/F3_harness.md:588) |
| `HRN-040` | Không ghi trực tiếp | Tên: `LINEMAP_INVALID`; Cổng phát: P3; Bản chất: Đầu vào | [03-dd/api/F3_harness.md:589](F:/prj_algoprep/03-dd/api/F3_harness.md:589) |
| `HRN-050` | Không ghi trực tiếp | Tên: `EXPECTED_OUTPUT_UNPARSEABLE`; Cổng phát: P4; Bản chất: Lỗi dữ liệu testcase | [03-dd/api/F3_harness.md:590](F:/prj_algoprep/03-dd/api/F3_harness.md:590) |
| `HRN-051` | Không ghi trực tiếp | Tên: `MATCH_CONFIG_INVALID`; Cổng phát: P4; Bản chất: Lỗi cấu hình đặc tả | [03-dd/api/F3_harness.md:591](F:/prj_algoprep/03-dd/api/F3_harness.md:591) |
| `HRN-060` | 422 | Tên: `PREVIEW_SPEC_INVALID`; Cổng phát: P6; Bản chất: Bản nháp có vi phạm cứng của P5 | [03-dd/api/F3_harness.md:592](F:/prj_algoprep/03-dd/api/F3_harness.md:592) |
| `HRN-061` | 429 | Tên: `PREVIEW_RATE_LIMITED`; Cổng phát: P6; Bản chất: Quá 10 lượt/giây/người | [03-dd/api/F3_harness.md:593](F:/prj_algoprep/03-dd/api/F3_harness.md:593) |
| `HRN-062` | 413 | Tên: `PREVIEW_BODY_TOO_LARGE`; Cổng phát: Vỏ HTTP P6; Bản chất: Body JSON UTF-8 vượt 16.384 byte; HTTP 413 | [03-dd/api/F3_harness.md:594](F:/prj_algoprep/03-dd/api/F3_harness.md:594) |
| `HRN-V01` | Không ghi trực tiếp | Tên: `TYPE_KIND_UNSUPPORTED`; Loại: Mềm | [03-dd/validation/F3_harness.md:192](F:/prj_algoprep/03-dd/validation/F3_harness.md:192) |
| `HRN-V02` | Không ghi trực tiếp | Tên: `TYPE_OF_MISSING`; Loại: Mềm | [03-dd/validation/F3_harness.md:193](F:/prj_algoprep/03-dd/validation/F3_harness.md:193) |
| `HRN-V03` | Không ghi trực tiếp | Tên: `TYPE_OF_NOT_ALLOWED`; Loại: Mềm | [03-dd/validation/F3_harness.md:194](F:/prj_algoprep/03-dd/validation/F3_harness.md:194) |
| `HRN-V04` | Không ghi trực tiếp | Tên: `ARRAY_DIMENSIONS_EXCEEDED`; Loại: Mềm | [03-dd/validation/F3_harness.md:195](F:/prj_algoprep/03-dd/validation/F3_harness.md:195) |
| `HRN-V05` | Không ghi trực tiếp | Tên: `TYPE_NESTING_EXCEEDED`; Loại: Mềm | [03-dd/validation/F3_harness.md:196](F:/prj_algoprep/03-dd/validation/F3_harness.md:196) |
| `HRN-V06` | Không ghi trực tiếp | Tên: `NODE_VALUE_TYPE_MIXED`; Loại: Mềm | [03-dd/validation/F3_harness.md:197](F:/prj_algoprep/03-dd/validation/F3_harness.md:197) |
| `HRN-V07` | Không ghi trực tiếp | Tên: `SIGNATURE_ABSENT`; Loại: Mềm | [03-dd/validation/F3_harness.md:198](F:/prj_algoprep/03-dd/validation/F3_harness.md:198) |
| `HRN-V08` | Không ghi trực tiếp | DỰ PHÒNG / ĐỂ TRỐNG, không phải mã lỗi đang được định nghĩa để trả: Tổng: 19 mã kiểm đặc tả (7 mềm, 12 cứng), dải `HRN-V01` đến `HRN-V22`, `HRN-V08`, `HRN-V09`, `HRN-V21` để trống. | [03-dd/validation/F3_harness.md:212](F:/prj_algoprep/03-dd/validation/F3_harness.md:212) |
| `HRN-V09` | Không ghi trực tiếp | DỰ PHÒNG / ĐỂ TRỐNG, không phải mã lỗi đang được định nghĩa để trả: Tổng: 19 mã kiểm đặc tả (7 mềm, 12 cứng), dải `HRN-V01` đến `HRN-V22`, `HRN-V08`, `HRN-V09`, `HRN-V21` để trống. | [03-dd/validation/F3_harness.md:212](F:/prj_algoprep/03-dd/validation/F3_harness.md:212) |
| `HRN-V10` | Không ghi trực tiếp | Tên: `TYPE_NODE_MALFORMED`; Loại: Cứng | [03-dd/validation/F3_harness.md:199](F:/prj_algoprep/03-dd/validation/F3_harness.md:199) |
| `HRN-V11` | Không ghi trực tiếp | Tên: `FUNCTION_NAME_INVALID`; Loại: Cứng | [03-dd/validation/F3_harness.md:200](F:/prj_algoprep/03-dd/validation/F3_harness.md:200) |
| `HRN-V12` | Không ghi trực tiếp | Tên: `FUNCTION_NAME_RESERVED`; Loại: Cứng | [03-dd/validation/F3_harness.md:201](F:/prj_algoprep/03-dd/validation/F3_harness.md:201) |
| `HRN-V13` | Không ghi trực tiếp | Tên: `PARAM_NAME_INVALID`; Loại: Cứng | [03-dd/validation/F3_harness.md:202](F:/prj_algoprep/03-dd/validation/F3_harness.md:202) |
| `HRN-V14` | Không ghi trực tiếp | Tên: `PARAM_NAME_DUPLICATE`; Loại: Cứng | [03-dd/validation/F3_harness.md:203](F:/prj_algoprep/03-dd/validation/F3_harness.md:203) |
| `HRN-V15` | Không ghi trực tiếp | Tên: `PARAM_NAME_RESERVED`; Loại: Cứng | [03-dd/validation/F3_harness.md:204](F:/prj_algoprep/03-dd/validation/F3_harness.md:204) |
| `HRN-V16` | Không ghi trực tiếp | Tên: `PARAM_COUNT_EXCEEDED`; Loại: Cứng | [03-dd/validation/F3_harness.md:205](F:/prj_algoprep/03-dd/validation/F3_harness.md:205) |
| `HRN-V17` | Không ghi trực tiếp | Tên: `SIGNATURE_INCOMPLETE`; Loại: Cứng | [03-dd/validation/F3_harness.md:206](F:/prj_algoprep/03-dd/validation/F3_harness.md:206) |
| `HRN-V18` | Không ghi trực tiếp | Tên: `NAME_OVERRIDE_KEY_INVALID`; Loại: Cứng | [03-dd/validation/F3_harness.md:207](F:/prj_algoprep/03-dd/validation/F3_harness.md:207) |
| `HRN-V19` | Không ghi trực tiếp | Tên: `NAME_OVERRIDE_VALUE_INVALID`; Loại: Cứng | [03-dd/validation/F3_harness.md:208](F:/prj_algoprep/03-dd/validation/F3_harness.md:208) |
| `HRN-V20` | Không ghi trực tiếp | Tên: `EPSILON_VALUE_REQUIRED`; Loại: Cứng | [03-dd/validation/F3_harness.md:209](F:/prj_algoprep/03-dd/validation/F3_harness.md:209) |
| `HRN-V21` | Không ghi trực tiếp | DỰ PHÒNG / ĐỂ TRỐNG, không phải mã lỗi đang được định nghĩa để trả: Tổng: 19 mã kiểm đặc tả (7 mềm, 12 cứng), dải `HRN-V01` đến `HRN-V22`, `HRN-V08`, `HRN-V09`, `HRN-V21` để trống. | [03-dd/validation/F3_harness.md:212](F:/prj_algoprep/03-dd/validation/F3_harness.md:212) |
| `HRN-V22` | Không ghi trực tiếp | Tên: `UNORDERED_SET_TYPE_INVALID`; Loại: Cứng | [03-dd/validation/F3_harness.md:210](F:/prj_algoprep/03-dd/validation/F3_harness.md:210) |

### 10.4. F4 — Judge / nộp và chấm bài — 39 mã

| Mã | HTTP được ghi trong nguồn | Ý nghĩa / điều kiện theo nguồn | Nguồn để mở đối chiếu |
|---|---|---|---|
| `JDG-101` | 404 | Ý nghĩa: Không tìm thấy bài toán (chưa xuất bản, đã ẩn mềm hoặc không tồn tại); Endpoint: #1, #2, #3 | [03-dd/api/F4_judge-orchestration.md:1556](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1556) |
| `JDG-102` | 422 | Ý nghĩa: Ngôn ngữ đang tắt, không nhận bài nộp mới; Endpoint: #2, #3 | [03-dd/api/F4_judge-orchestration.md:1557](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1557) |
| `JDG-103` | 422 | Ý nghĩa: Bài chỉ hỗ trợ Standard I/O, không nhận mô hình Bọc hàm; Endpoint: #2, #3 | [03-dd/api/F4_judge-orchestration.md:1558](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1558) |
| `JDG-104` | 422 | Ý nghĩa: Bài không có testcase đã duyệt để chấm; Endpoint: #3 | [03-dd/api/F4_judge-orchestration.md:1559](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1559) |
| `JDG-105` | 422 | Ý nghĩa: Bài không có testcase Sample để chạy thử; Endpoint: #2 | [03-dd/api/F4_judge-orchestration.md:1560](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1560) |
| `JDG-106` | 429 | Ý nghĩa: Vượt số lượt nộp tối đa trong một giờ; Endpoint: #3 | [03-dd/api/F4_judge-orchestration.md:1561](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1561) |
| `JDG-107` | 429 | Ý nghĩa: Vượt 60 lượt Run Code trong 60 phút trượt theo tài khoản; Endpoint: #2 | [03-dd/api/F4_judge-orchestration.md:1562](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1562) |
| `JDG-110` | 403 / 404 | Ý nghĩa: Không tìm thấy bài nộp (không có, của người khác, hoặc ngoài phạm vi lớp); Endpoint: #5, #6, #11 | [03-dd/api/F4_judge-orchestration.md:1563](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1563) |
| `JDG-111` | 404 | Ý nghĩa: Không tìm thấy lớp (không phải lớp của giảng viên, hoặc `classId` sai định dạng); Endpoint: #10, #12 | [03-dd/api/F4_judge-orchestration.md:1564](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1564) |
| `JDG-112` | 404 | Ý nghĩa: Học viên không thuộc lớp; Endpoint: #12 | [03-dd/api/F4_judge-orchestration.md:1565](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1565) |
| `JDG-201` | 409 | Ý nghĩa: Trạng thái cụm đã bị người khác đổi; tải lại; Endpoint: #16, #17 | [03-dd/api/F4_judge-orchestration.md:1566](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1566) |
| `JDG-202` | 409 | Ý nghĩa: Cấu hình đã được người khác cập nhật; tải lại; Endpoint: #22 | [03-dd/api/F4_judge-orchestration.md:1567](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1567) |
| `JDG-203` | 422 | Ý nghĩa: Phải giữ ít nhất một ngôn ngữ đang bật; Endpoint: #22 | [03-dd/api/F4_judge-orchestration.md:1568](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1568) |
| `JDG-204` | 422 | Ý nghĩa: Giới hạn thời gian hiệu dụng ngoài 100 đến 30.000 ms; Endpoint: #22 | [03-dd/api/F4_judge-orchestration.md:1569](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1569) |
| `JDG-205` | 422 | Ý nghĩa: Giới hạn bộ nhớ hiệu dụng ngoài 64 đến 1.024 MB; Endpoint: #22 | [03-dd/api/F4_judge-orchestration.md:1570](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1570) |
| `JDG-301` | 403 / 404 | Ý nghĩa: Không đủ quyền (thiếu Function hoặc vai trò); Endpoint: #10 đến #22 | [03-dd/api/F4_judge-orchestration.md:1571](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1571) |
| `JDG-501` | 503 | Ý nghĩa: Judge engine không dùng được sau khi thử lại, hoặc vượt hạn tổng của lượt chạy thử; Endpoint: #2 | [03-dd/api/F4_judge-orchestration.md:1572](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1572) |
| `JDG-502` | 429 | Ý nghĩa: Cụm đang nhận quá số lượt chạy thử đồng thời; Endpoint: #2 | [03-dd/api/F4_judge-orchestration.md:1573](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1573) |
| `JDG-503` | 503 | Ý nghĩa: Hàng đợi chấm không nhận được bài nộp; Endpoint: #3 | [03-dd/api/F4_judge-orchestration.md:1575](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1575) |
| `JDG-504` | 503 | Ý nghĩa: Bundle testcase của phiên bản hiện hành chưa sẵn sàng; không tạo lượt nộp; Endpoint: #3 | [03-dd/api/F4_judge-orchestration.md:1574](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1574) |
| `JDG-505` | 503 | Ý nghĩa: Hạ tầng nhận bài nộp không dùng được; kết quả ghi có thể chưa xác định; Endpoint: #3; owner 2026-11-03 | [03-dd/api/F4_judge-orchestration.md:1576](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1576) |
| `JDG-506` | 503 | Ý nghĩa: Hạ tầng đọc kết quả/lịch sử hoặc dữ liệu phạm vi/tìm kiếm bắt buộc tạm không khả dụng; không bỏ bộ lọc; Endpoint: #4–7, #10–12; owner 2026-11-04 | [03-dd/api/F4_judge-orchestration.md:1577](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1577) |
| `JDG-507` | 503 | **Triển khai 2026-11-04 (D02x):** `503 JDG-507` khi database hoặc cổng mapping chủ đề F2 tạm thời không khả dụng; RFC9457 với `errorCode`, detail cố định, không trả số liệu0 giả hoặc chi tiết dependency. Owner duyệt trong `DEC-2026-1104-f4-my-metrics-unavailable`; JDG-506 giữ phạm vi kết quả/lịch sử. | [03-dd/api/F4_judge-orchestration.md:806](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:806) |
| `JDG-V01` | 400 | Ý nghĩa: Mã nguồn rỗng hoặc chỉ gồm khoảng trắng; Endpoint: #2, #3 | [03-dd/api/F4_judge-orchestration.md:1578](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1578) |
| `JDG-V02` | 400 | Ý nghĩa: Mã nguồn quá 65.536 byte UTF-8; Endpoint: #2, #3 | [03-dd/api/F4_judge-orchestration.md:1579](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1579) |
| `JDG-V03` | 400 | Ý nghĩa: `language` không hợp lệ; Endpoint: #1, #2, #3 | [03-dd/api/F4_judge-orchestration.md:1580](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1580) |
| `JDG-V04` | 400 | Ý nghĩa: `submissionModel` không hợp lệ; Endpoint: #2, #3 | [03-dd/api/F4_judge-orchestration.md:1581](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1581) |
| `JDG-V05` | 400 | Ý nghĩa: `sourceInputMethod` không hợp lệ; Endpoint: #3 | [03-dd/api/F4_judge-orchestration.md:1582](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1582) |
| `JDG-V06` | 400 | Ý nghĩa: Tệp tải lên thiếu tên hoặc sai phần mở rộng so với ngôn ngữ; Endpoint: #3 | [03-dd/api/F4_judge-orchestration.md:1583](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1583) |
| `JDG-V07` | 400 | Ý nghĩa: `range` không thuộc `7d`, `30d`, `all`; Endpoint: #9 | [03-dd/api/F4_judge-orchestration.md:1584](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1584) |
| `JDG-V08` | Không ghi trực tiếp | DỰ PHÒNG / ĐỂ TRỐNG, không phải mã lỗi đang được định nghĩa để trả: Khoảng đã dùng: `JDG-101` đến `JDG-505` (bổ sung `JDG-505` theo owner 2026-11-03; danh sách mã nghiệp vụ và hạ tầng ở bảng trên) và `JDG-V01` đến `JDG-V16` (15 mã, bỏ `V08`). `JDG-V08` để trống có chủ ý (dành chỗ, không đánh số lại các mã đã trích). | [03-dd/api/F4_judge-orchestration.md:1594](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1594) |
| `JDG-V09` | 400 | Ý nghĩa: Tham số hàng đợi không phải số nguyên từ 1 trở lên; Endpoint: #19 | [03-dd/api/F4_judge-orchestration.md:1585](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1585) |
| `JDG-V10` | 400 | Ý nghĩa: Hệ số thời gian hoặc bộ nhớ ngoài 0,10 đến 9,99 hoặc quá 2 chữ số thập phân; Endpoint: #22 | [03-dd/api/F4_judge-orchestration.md:1586](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1586) |
| `JDG-V11` | 400 | Ý nghĩa: Giới hạn mặc định không phải số nguyên từ 1 trở lên; Endpoint: #22 | [03-dd/api/F4_judge-orchestration.md:1587](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1587) |
| `JDG-V12` | 400 | Ý nghĩa: Giới hạn script sinh dữ liệu ngoài khoảng; Endpoint: #22 | [03-dd/api/F4_judge-orchestration.md:1588](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1588) |
| `JDG-V13` | 400 | Ý nghĩa: Khoảng ngày ngược (`from` sau `to`); Endpoint: #20 | [03-dd/api/F4_judge-orchestration.md:1589](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1589) |
| `JDG-V14` | 400 | Ý nghĩa: `limit` ngoài khoảng; Endpoint: #4, #8, #23 | [03-dd/api/F4_judge-orchestration.md:1590](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1590) |
| `JDG-V15` | 400 | Ý nghĩa: `topicIds` sai định dạng hoặc quá 50 phần tử; Endpoint: #9 | [03-dd/api/F4_judge-orchestration.md:1591](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1591) |
| `JDG-V16` | 400 | Ý nghĩa: Thiếu `paused`, `enabled` hoặc giá trị mong đợi; Endpoint: #16, #17 | [03-dd/api/F4_judge-orchestration.md:1592](F:/prj_algoprep/03-dd/api/F4_judge-orchestration.md:1592) |

### 10.5. F5 — AI review / phỏng vấn AI — 46 mã

| Mã | HTTP được ghi trong nguồn | Ý nghĩa / điều kiện theo nguồn | Nguồn để mở đối chiếu |
|---|---|---|---|
| `AIR-101` | 404 | Tên (BD): `SUBMISSION_NOT_FOUND`; Ý nghĩa: Bài nộp không tồn tại; Endpoint: 4, 8 | [03-dd/api/F5_ai-review.md:1894](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1894) |
| `AIR-102` | 422 | Tên (BD): `SUBMISSION_NOT_ACCEPTED`; Ý nghĩa: Bài nộp chưa `ACCEPTED` (`ARCH5:79-80`); Endpoint: 4, 8 | [03-dd/api/F5_ai-review.md:1895](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1895) |
| `AIR-103` | 409 | Tên (BD): ; Ý nghĩa: Báo cáo của bài nộp đang được sinh bởi lời gọi khác; Endpoint: 4 | [03-dd/api/F5_ai-review.md:1896](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1896) |
| `AIR-110` | 404 | Tên (BD): ; Ý nghĩa: Phiên phỏng vấn không tồn tại; Endpoint: 9, 10, 11, 12 | [03-dd/api/F5_ai-review.md:1897](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1897) |
| `AIR-111` | 409 | Tên (BD): ; Ý nghĩa: Phiên không còn chạy (`COMPLETED` hoặc `ABANDONED`); Endpoint: 10, 11, 12 | [03-dd/api/F5_ai-review.md:1898](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1898) |
| `AIR-112` | 409 | Tên (BD): ; Ý nghĩa: `expectedTurnCount` khác `turn_count`; Endpoint: 10 | [03-dd/api/F5_ai-review.md:1899](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1899) |
| `AIR-113` | 409 | Tên (BD): ; Ý nghĩa: Phiên đang xử lý một lượt khác; Endpoint: 10, 11, 12 | [03-dd/api/F5_ai-review.md:1900](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1900) |
| `AIR-114` | 409 | Tên (BD): ; Ý nghĩa: Đã đủ `max_turns` hoặc đạt trần giai đoạn cuối; gọi `complete`; Endpoint: 10 | [03-dd/api/F5_ai-review.md:1901](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1901) |
| `AIR-115` | 422 | Tên (BD): ; Ý nghĩa: Gợi ý không được phép (`hint_allowed` khác `true`); Endpoint: 11 | [03-dd/api/F5_ai-review.md:1902](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1902) |
| `AIR-116` | 422 | Tên (BD): ; Ý nghĩa: Chưa có câu trả lời nào để chấm; Endpoint: 12 | [03-dd/api/F5_ai-review.md:1903](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1903) |
| `AIR-117` | 422 | Tên (BD): ; Ý nghĩa: Đã dùng hết số lần gợi ý của phiên (`hint_count` bằng `ai.interview.max-hints-per-session`); Endpoint: 11 | [03-dd/api/F5_ai-review.md:1904](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1904) |
| `AIR-120` | 404 | Tên (BD): ; Ý nghĩa: Cảnh báo bất thường không tồn tại; Endpoint: 29 | [03-dd/api/F5_ai-review.md:1905](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1905) |
| `AIR-121` | 404 | Tên (BD): ; Ý nghĩa: Dòng đơn giá mô hình không tồn tại; Endpoint: 25 | [03-dd/api/F5_ai-review.md:1906](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1906) |
| `AIR-201` | 409 | Tên (BD): ; Ý nghĩa: Bản prompt đang chạy đã bị người khác đổi; tải lại; Endpoint: 17 | [03-dd/api/F5_ai-review.md:1907](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1907) |
| `AIR-202` | 422 | Tên (BD): ; Ý nghĩa: `modelId` ngoài `modelOptions`; bí danh `default` luôn hợp lệ; Endpoint: 16, 17 | [03-dd/api/F5_ai-review.md:1908](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1908) |
| `AIR-203` | 422 | Tên (BD): ; Ý nghĩa: Mở khoá bị từ chối: đã dùng chưa nhỏ hơn hạn mức; Endpoint: 20 | [03-dd/api/F5_ai-review.md:1909](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1909) |
| `AIR-204` | 409 | Tên (BD): ; Ý nghĩa: Tên mô hình trùng (không phân biệt hoa thường); Endpoint: 24, 25 | [03-dd/api/F5_ai-review.md:1910](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1910) |
| `AIR-205` | 422 | Tên (BD): ; Ý nghĩa: Không có thay đổi nào để phát hành; Endpoint: 17 | [03-dd/api/F5_ai-review.md:1911](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1911) |
| `AIR-301` | 403 | Tên (BD): ; Ý nghĩa: Thiếu Function hoặc không phải `ADMIN`; Endpoint: 14 đến 31 | [03-dd/api/F5_ai-review.md:1912](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1912) |
| `AIR-302` | 403 | Tên (BD): ; Ý nghĩa: Không phải chủ dữ liệu (bài nộp, phiên, báo cáo); Endpoint: 4, 8, 9, 10, 11, 12 | [03-dd/api/F5_ai-review.md:1913](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1913) |
| `AIR-401` | 429 | Ý nghĩa: Ngân sách token AI đã khoá (ánh xạ `AIR-401`); Endpoint: 6 | [03-dd/api/F6_interview-bank.md:1059](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1059) |
| `AIR-402` | 429 | Tên (BD): ; Ý nghĩa: Quá `review_per_hour`; Endpoint: 4 | [03-dd/api/F5_ai-review.md:1915](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1915) |
| `AIR-403` | 429 | Tên (BD): ; Ý nghĩa: Quá `interview_per_hour`; Endpoint: 8 | [03-dd/api/F5_ai-review.md:1916](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1916) |
| `AIR-404` | 429 | Tên (BD): ; Ý nghĩa: Còn trong thời gian chờ giữa hai yêu cầu; Endpoint: 4, 8 | [03-dd/api/F5_ai-review.md:1917](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1917) |
| `AIR-501` | 503 | Tên (BD): `AI_PROVIDER_UNAVAILABLE`; Ý nghĩa: LLM hoặc cổng ngoài không dùng được (`details.reason`: `TIMEOUT`, `PROVIDER_ERROR`, `UPSTREAM_PORT`); Endpoint: 4, 8, 10, 11, 12, P3 | [03-dd/api/F5_ai-review.md:1918](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1918) |
| `AIR-502` | 502 | Tên (BD): ; Ý nghĩa: LLM trả kết quả sai lược đồ; Endpoint: 4, 8, 10, 12, P3 | [03-dd/api/F5_ai-review.md:1919](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1919) |
| `AIR-503` | 503 | Tên (BD): `ADMIN_AUDIT_UNAVAILABLE`; Ý nghĩa: AuditLogPort ghi nhật ký quản trị lỗi; rollback toàn bộ mutation; Endpoint: 17, 19, 20 | [03-dd/api/F5_ai-review.md:1920](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1920) |
| `AIR-V01` | 400 | Tên (BD): ; Ý nghĩa: `problemId` thiếu hoặc không phải UUID; Endpoint: 6 | [03-dd/api/F5_ai-review.md:1921](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1921) |
| `AIR-V02` | 400 | Tên (BD): ; Ý nghĩa: `limit` ngoài khoảng (7: 1-10; 13: 1-10; 30, 31: 1-20); Endpoint: 7, 13, 30, 31 | [03-dd/api/F5_ai-review.md:1922](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1922) |
| `AIR-V03` | 400 | Tên (BD): ; Ý nghĩa: Thiếu hoặc thừa trường theo `entryType`; Endpoint: 8 | [03-dd/api/F5_ai-review.md:1923](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1923) |
| `AIR-V04` | 400 | Tên (BD): ; Ý nghĩa: `interviewerLevel` không hợp lệ; Endpoint: 8 | [03-dd/api/F5_ai-review.md:1924](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1924) |
| `AIR-V05` | 400 | Tên (BD): ; Ý nghĩa: `maxTurns` không thuộc `8`, `12`, `16`; Endpoint: 8 | [03-dd/api/F5_ai-review.md:1925](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1925) |
| `AIR-V06` | 400 | Tên (BD): ; Ý nghĩa: `topics` sai (rỗng, quá 10, phần tử quá 60 ký tự); Endpoint: 8 | [03-dd/api/F5_ai-review.md:1926](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1926) |
| `AIR-V07` | 400 | Tên (BD): ; Ý nghĩa: `questionRef` không phải UUID hoặc `questionText` rỗng hay quá 2.000 ký tự; Endpoint: 8 | [03-dd/api/F5_ai-review.md:1927](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1927) |
| `AIR-V08` | 400 | Tên (BD): ; Ý nghĩa: `content` rỗng hoặc quá 4.000 ký tự; Endpoint: 10 | [03-dd/api/F5_ai-review.md:1928](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1928) |
| `AIR-V09` | 400 | Tên (BD): ; Ý nghĩa: Thiếu hoặc sai `expectedTurnCount`; Endpoint: 10 | [03-dd/api/F5_ai-review.md:1929](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1929) |
| `AIR-V10` | 400 | Tên (BD): ; Ý nghĩa: `range` không thuộc `7d`, `30d`, `all`; Endpoint: 13 | [03-dd/api/F5_ai-review.md:1930](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1930) |
| `AIR-V11` | 400 | Tên (BD): ; Ý nghĩa: `featureCode` không hợp lệ; Endpoint: 15, 16, 17 | [03-dd/api/F5_ai-review.md:1931](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1931) |
| `AIR-V12` | 400 | Tên (BD): ; Ý nghĩa: `systemInstruction` rỗng hoặc quá 20.000 ký tự; Endpoint: 16, 17 | [03-dd/api/F5_ai-review.md:1932](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1932) |
| `AIR-V13` | 400 | Tên (BD): ; Ý nghĩa: `responseGuards` sai hình dạng so với `featureCode`; Endpoint: 16, 17 | [03-dd/api/F5_ai-review.md:1933](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1933) |
| `AIR-V14` | 400 | Tên (BD): ; Ý nghĩa: `changes` rỗng, trùng feature hoặc quá 3 phần tử; Endpoint: 17 | [03-dd/api/F5_ai-review.md:1934](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1934) |
| `AIR-V15` | 400 | Tên (BD): ; Ý nghĩa: `limitTokens` không phải số nguyên dương; Endpoint: 19 | [03-dd/api/F5_ai-review.md:1935](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1935) |
| `AIR-V16` | 400 | Tên (BD): ; Ý nghĩa: Giá trị của `limits-config` ngoài khoảng, không phải số nguyên, hoặc body rỗng; Endpoint: 22 | [03-dd/api/F5_ai-review.md:1936](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1936) |
| `AIR-V17` | 400 | Tên (BD): ; Ý nghĩa: `modelName` rỗng hoặc quá 100 ký tự; Endpoint: 24, 25 | [03-dd/api/F5_ai-review.md:1937](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1937) |
| `AIR-V18` | 400 | Tên (BD): ; Ý nghĩa: `usdPerMillionTokens` âm, không phải số hoặc quá khoảng; Endpoint: 25 | [03-dd/api/F5_ai-review.md:1938](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1938) |
| `AIR-V19` | 400 | Tên (BD): ; Ý nghĩa: `rangeDays` không thuộc `7`, `14`, `30`; Endpoint: 27 | [03-dd/api/F5_ai-review.md:1939](F:/prj_algoprep/03-dd/api/F5_ai-review.md:1939) |

### 10.6. F6 — Interview bank / câu hỏi lý thuyết — 37 mã

| Mã | HTTP được ghi trong nguồn | Ý nghĩa / điều kiện theo nguồn | Nguồn để mở đối chiếu |
|---|---|---|---|
| `INB-010` | 200 / 404 | Ý nghĩa: Câu hỏi không tồn tại, sai định dạng id hoặc đã `RETIRED`; Endpoint: 3 đến 7, 20, 22, 23 (trong `failedItems`) | [03-dd/api/F6_interview-bank.md:1046](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1046) |
| `INB-011` | 404 | Ý nghĩa: Chủ đề không tồn tại; Endpoint: 12, 14 | [03-dd/api/F6_interview-bank.md:1047](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1047) |
| `INB-012` | 404 | Ý nghĩa: Độ khó không tồn tại; Endpoint: 16, 18 | [03-dd/api/F6_interview-bank.md:1048](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1048) |
| `INB-020` | 409 | Ý nghĩa: Tên chủ đề trùng; Endpoint: 11, 12 | [03-dd/api/F6_interview-bank.md:1049](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1049) |
| `INB-021` | 409 | Ý nghĩa: Tên độ khó trùng; Endpoint: 15, 16 | [03-dd/api/F6_interview-bank.md:1050](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1050) |
| `INB-030` | 422 | Ý nghĩa: Chủ đề còn câu hỏi tham chiếu (`referencingQuestionCount`); Endpoint: 14 | [03-dd/api/F6_interview-bank.md:1051](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1051) |
| `INB-031` | 422 | Ý nghĩa: Độ khó còn câu hỏi tham chiếu (`reason = IN_USE`); Endpoint: 18 | [03-dd/api/F6_interview-bank.md:1052](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1052) |
| `INB-032` | 422 | Ý nghĩa: Độ khó là mức cuối cùng (`reason = LAST_LEVEL`); Endpoint: 18 | [03-dd/api/F6_interview-bank.md:1053](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1053) |
| `INB-033` | 422 | Ý nghĩa: Tổng trọng số tiêu chí khác 100; Endpoint: 21, 22 | [03-dd/api/F6_interview-bank.md:1054](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1054) |
| `INB-034` | 422 | Ý nghĩa: `orderedIds` không đúng tập hiện có; Endpoint: 13, 17 | [03-dd/api/F6_interview-bank.md:1055](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1055) |
| `INB-035` | 422 | Ý nghĩa: `topicId` hoặc `levelId` không tồn tại khi lưu câu hỏi; Endpoint: 21, 22 | [03-dd/api/F6_interview-bank.md:1056](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1056) |
| `INB-040` | 422 | Ý nghĩa: Câu hỏi chưa có tiêu chí, không luyện được; Endpoint: 6 | [03-dd/api/F6_interview-bank.md:1057](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1057) |
| `INB-041` | 429 | Ý nghĩa: Quá 20 lượt luyện mỗi giờ; Endpoint: 6 | [03-dd/api/F6_interview-bank.md:1058](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1058) |
| `INB-042` | 429 | Ý nghĩa: Ngân sách token AI đã khoá (ánh xạ `AIR-401`); Endpoint: 6 | [03-dd/api/F6_interview-bank.md:1059](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1059) |
| `INB-050` | 409 | Ý nghĩa: Trùng `attempt_no` sau 3 lần thử; Endpoint: 6 | [03-dd/api/F6_interview-bank.md:1060](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1060) |
| `INB-301` | 403 | Ý nghĩa: Thiếu Function `INTERVIEW_BANK_MANAGEMENT:<ACTION>`; Endpoint: 11 đến 24 | [03-dd/api/F6_interview-bank.md:1061](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1061) |
| `INB-302` | 403 | Ý nghĩa: Có Function nhưng không phải `ADMIN`; Endpoint: 11 đến 18 | [03-dd/api/F6_interview-bank.md:1062](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1062) |
| `INB-501` | 201 (mã nằm trong body) | Ý nghĩa: Phản hồi AI không có: AI, cổng ngân sách hỏng hoặc quá hạn; Endpoint: 6 | [03-dd/api/F6_interview-bank.md:1063](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1063) |
| `INB-502` | 201 (mã nằm trong body) | Ý nghĩa: AI trả sai lược đồ; Endpoint: 6 | [03-dd/api/F6_interview-bank.md:1064](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1064) |
| `INB-503` | 503 | Ý nghĩa: `AuditLogPort` không ghi được; rollback thao tác quản trị; Endpoint: 11–18, 21–24 | [03-dd/api/F6_interview-bank.md:1065](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1065) |
| `INB-V01` | 400 | Ý nghĩa: Tham số lọc của danh sách quản trị không hợp lệ; Endpoint: 19 | [03-dd/api/F6_interview-bank.md:1066](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1066) |
| `INB-V02` | 400 | Ý nghĩa: `ids` của xoá theo lô không hợp lệ; Endpoint: 23 | [03-dd/api/F6_interview-bank.md:1067](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1067) |
| `INB-V03` | 400 | Ý nghĩa: `displayName` rỗng hoặc quá 60 ký tự; Endpoint: 11, 12, 15, 16 | [03-dd/api/F6_interview-bank.md:1068](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1068) |
| `INB-V04` | 400 | Ý nghĩa: `rating` không hợp lệ; Endpoint: 4 | [03-dd/api/F6_interview-bank.md:1069](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1069) |
| `INB-V05` | 400 | Ý nghĩa: `bookmarked` thiếu hoặc không phải boolean; Endpoint: 5 | [03-dd/api/F6_interview-bank.md:1070](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1070) |
| `INB-V06` | 400 | Ý nghĩa: `answerText` rỗng; Endpoint: 6 | [03-dd/api/F6_interview-bank.md:1071](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1071) |
| `INB-V07` | 400 | Ý nghĩa: `answerText` quá 5000 ký tự; Endpoint: 6 | [03-dd/api/F6_interview-bank.md:1072](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1072) |
| `INB-V08` | 400 | Ý nghĩa: `title` rỗng hoặc quá 255; hoặc `before` không phải số nguyên dương (mục 7); Endpoint: 7, 21, 22 | [03-dd/api/F6_interview-bank.md:1073](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1073) |
| `INB-V09` | 400 | Ý nghĩa: `topicId` hoặc `levelId` thiếu hoặc không phải UUID; Endpoint: 21, 22 | [03-dd/api/F6_interview-bank.md:1074](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1074) |
| `INB-V10` | 400 | Ý nghĩa: Tiêu chí sai (tên rỗng hoặc quá 100, trọng số ngoài [0, 100] hoặc quá 2 chữ số thập phân); Endpoint: 21, 22 | [03-dd/api/F6_interview-bank.md:1075](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1075) |
| `INB-V11` | 400 | Ý nghĩa: Hai tiêu chí trùng tên; Endpoint: 21, 22 | [03-dd/api/F6_interview-bank.md:1076](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1076) |
| `INB-V12` | 400 | Ý nghĩa: `followUpQuestions` không phải mảng chuỗi; Endpoint: 21, 22 | [03-dd/api/F6_interview-bank.md:1077](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1077) |
| `INB-V13` | 400 | Ý nghĩa: `PATCH` chủ đề không có trường nào; Endpoint: 12 | [03-dd/api/F6_interview-bank.md:1078](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1078) |
| `INB-V14` | 400 | Ý nghĩa: `orderedIds` thiếu hoặc phần tử không phải UUID; Endpoint: 13, 17 | [03-dd/api/F6_interview-bank.md:1079](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1079) |
| `INB-V15` | 400 | Ý nghĩa: `rows` của nhập CSV thiếu, rỗng hoặc quá 500; Endpoint: 24 | [03-dd/api/F6_interview-bank.md:1080](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1080) |
| `INB-V16` | Không ghi trực tiếp | Thông báo giao diện: Dữ liệu gửi lên chưa hợp lệ. | [05-coding/frontend/messages/vi.json:206](F:/prj_algoprep/05-coding/frontend/messages/vi.json:206) |
| `INB-V17` | 413 | Ý nghĩa: Body quá trần kỹ thuật; Endpoint: 21, 22, 24 | [03-dd/api/F6_interview-bank.md:1081](F:/prj_algoprep/03-dd/api/F6_interview-bank.md:1081) |

## 11. Nguồn và cách cập nhật

- HTTP: [IANA registry](https://www.iana.org/assignments/http-status-codes/), [RFC 9110](https://www.rfc-editor.org/rfc/rfc9110.html), [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457.html).
- Quy ước mã, phong bì lỗi và tiền tố: [DD API index](F:/prj_algoprep/03-dd/api/api.md).
- Định nghĩa từng mã: nguồn được liên kết ở mỗi dòng; ưu tiên hợp đồng DD đang hiệu lực và quyết định owner, đối chiếu handler khi đọc kết quả thực thi.
- Khi thay hợp đồng mã/HTTP, cập nhật DD trước hoặc cùng thay đổi được duyệt rồi cập nhật bản tra cứu này. Không dùng bản tra cứu để tự ghi đè AC hay đánh dấu testcase đạt.
- Tài liệu không có nghĩa mọi mã ít gặp hoặc mã chỉ xuất hiện trong thiết kế đều đang được server phát ra. Hành vi thực tế cần được kiểm ở đúng bản build, endpoint, quyền và dữ liệu.
