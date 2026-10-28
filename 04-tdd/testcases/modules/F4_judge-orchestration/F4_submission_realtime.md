# Test case QA — F4: tiến trình chấm thời gian thực và đường REST dự phòng

> Trạng thái: 9 ca đã review thiết kế; chưa thực thi. Ca kiểm kênh STOMP cần backend thật; ca nối lại/poll cần màn học viên tích hợp với clock và kết nối có thể điều khiển.

## Phạm vi và fixture

- TDD: `04-tdd/modules/F4_judge-orchestration.md` AC-25–29; DD F4 API §2.1–2.5, §9 và logic BR-JD-06, BR-JD-11.
- `P-LIVE`: bài `PUBLISHED` có snapshot 12 testcase đã duyệt theo `position=1..12`, trong đó S1–S3 là Sample, H1–H9 là Hidden. `A1-LIVE` sở hữu submission; `B1-LIVE` là học viên khác. Worker có barrier sau mỗi commit để giữ/nhả sự kiện theo thứ tự kiểm thử.
- `W1=/user/queue/submissions`; message có `submissionId`, `seq`, `type`, `isFinal`. `COMPILING=1`, `JUDGING=2`, testcase thứ i `seq=2+i`, cuối `seq=15` khi đã biết N=12. Không xem STOMP là nguồn sự thật; DB và GET `/submissions/{id}/progress` là nguồn hồi phục.

## Ma trận nhánh

| AC | Nhánh | Test case | Review | Thực thi |
| :--- | :--- | :--- | :--- | :--- |
| F4 AC-25 | Thứ tự seq và số đếm 12 ca | TC-F4-AC25-01 | Đã review | Chưa thực hiện |
| F4 AC-26 | Học viên khác không nghe bài của A | TC-F4-AC26-01 | Đã review | Chưa thực hiện |
| F4 AC-26 | CONNECT không JWT | TC-F4-AC26-02 | Đã review | Chưa thực hiện |
| F4 AC-26 | SUBSCRIBE kênh admin sai quyền | TC-F4-AC26-03 | Đã review | Chưa thực hiện |
| F4 AC-26 | SEND từ client bị từ chối | TC-F4-AC26-04 | Đã review | Chưa thực hiện |
| F4 AC-27 | Mất ba message rồi nối lại | TC-F4-AC27-01 | Đã review | Chưa thực hiện |
| F4 AC-27 | Kết thúc trong lúc rớt kênh | TC-F4-AC27-02 | Đã review | Chưa thực hiện |
| F4 AC-28 | Poll 3 giây tối đa 10 lần | TC-F4-AC28-01 | Đã review | Chưa thực hiện |
| F4 AC-29 | Message/progress không lộ dữ liệu Hidden | TC-F4-AC29-01 | Đã review | Chưa thực hiện |

## Test case

### TC-F4-AC25-01 — Mười hai kết quả phát theo commit và seq tăng ngặt

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-25; DD F4 API §2.3–2.4, logic BR-JD-06 |
| Kind / priority | Normal, P1: dải ô và số đếm khớp DB |
| Environment | F4 worker + DB + STOMP 1.2 + màn kết quả tích hợp |
| Preconditions | `A1-LIVE` đã CONNECT với JWT và SUBSCRIBE W1 trước khi worker bắt đầu; `P-LIVE` N=12 |
| Test data | Verdict thứ tự `AC,AC,WA,AC,AC,AC,AC,AC,AC,AC,AC,AC` (11 AC, 1 WA ở vị trí 3) |
| Steps | Cho worker commit lần lượt trạng thái và 12 testcase qua barrier; thu message và trạng thái 12 ô; sau cùng đọc DB/progress. |
| Expected | `STATUS_CHANGED(COMPILING)` seq1, `STATUS_CHANGED(JUDGING,totalCount=12)` seq2; 12 `TESTCASE_FINISHED` có `order=1..12`, `seq=3..14`, `completedCount=1..12`, `passedCount` 1,2,2,...,11; mỗi message chỉ phát sau commit tương ứng. `SUBMISSION_FINISHED` seq15, `WRONG_ANSWER`, `scoreRatio=0.917`, `firstFailingOrder=3`. Màn có 12 ô đúng thứ tự và số đếm 12/11, không tô lại ô sai. |
| Cleanup | Đóng STOMP, dọn submission/fixture |
| Evidence | Message timeline/seq, DB commit timestamps, progress response và ảnh màn |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC26-01 — Học viên B không nhận message bài nộp của A

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-26; DD F4 API §2.2, BR-JD-06 |
| Kind / priority | Security, P0: không phát dữ liệu chấm sai người |
| Environment | STOMP F4 + hai JWT học viên + worker |
| Preconditions | A và B CONNECT/SUBSCRIBE cùng đích W1 bằng JWT riêng; submission X thuộc A, Y thuộc B |
| Test data | Worker phát một `TESTCASE_FINISHED` cho X và một cho Y, `seq=3` mỗi submission |
| Steps | Theo dõi message ở hai phiên; B thử SUBSCRIBE `/user/queue/submissions/{X}` như kênh đoán ID. |
| Expected | A chỉ nhận X, B chỉ nhận Y; đích ghép ID bị từ chối generic, không xác nhận X tồn tại. Không payload của X xuất hiện ở phiên B, kể cả khi B biết UUID X. |
| Cleanup | Đóng hai phiên và dọn X/Y |
| Evidence | STOMP frames ở A/B, kết quả SUBSCRIBE sai đích |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC26-02 — CONNECT không token bị đóng trước khi nhận dữ liệu

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-26; DD F4 API §2.1 |
| Kind / priority | Security, P0: chặn người không đăng nhập |
| Environment | STOMP F4 qua `/ws` |
| Preconditions | Có một submission của A đang chấm |
| Test data | CONNECT không header `Authorization` |
| Steps | Mở WebSocket `/ws`, gửi CONNECT thiếu token, theo dõi frame và đóng kết nối. |
| Expected | Nhận ERROR generic rồi kết nối đóng; không đăng ký W1 hoặc nhận message của A, lỗi không nêu ID bài nộp hay chi tiết xác thực. |
| Cleanup | Đóng kết nối và dọn fixture |
| Evidence | Handshake/ERROR frame và payload đã nhận |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC26-03 — STUDENT không đăng ký được kênh queue của ADMIN

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-26; DD F4 API §2.2 |
| Kind / priority | Security, P1: quyền được kiểm tại SUBSCRIBE |
| Environment | STOMP F4 qua `/ws` |
| Preconditions | B CONNECT thành công bằng JWT STUDENT; một ADMIN hợp lệ đang SUBSCRIBE `/topic/admin/queue` để máy chủ phát `QUEUE_BATCH` trong môi trường thử |
| Test data | B SUBSCRIBE `/topic/admin/queue` |
| Steps | Gửi SUBSCRIBE, chờ một chu kỳ 5 giây của kênh admin, theo dõi frame. |
| Expected | Đăng ký bị từ chối generic, B không nhận `QUEUE_BATCH` hoặc dữ liệu queue; lỗi không xác nhận có người admin đang xem hay không. |
| Cleanup | Đóng kết nối, dọn fixture |
| Evidence | CONNECT/SUBSCRIBE/ERROR frames và message đã nhận |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC26-04 — Client không được SEND lên kênh một chiều

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-26; DD F4 API §2.1 (không có `/app/**`) |
| Kind / priority | Security, P1: client không tự phát trạng thái chấm |
| Environment | STOMP F4 qua `/ws` |
| Preconditions | B CONNECT thành công bằng JWT STUDENT |
| Test data | Frame SEND tới `/app/anything` với body `{"type":"SUBMISSION_FINISHED","status":"ACCEPTED"}` |
| Steps | Gửi frame, quan sát ERROR, DB và message của các phiên khác. |
| Expected | SEND bị từ chối; không có API/DB/event/STOMP hợp lệ nào đổi bài nộp thành `ACCEPTED` do frame này. |
| Cleanup | Đóng kết nối, dọn fixture |
| Evidence | Frame SEND/ERROR, DB trước/sau và message các phiên |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC27-01 — Rớt kênh sau 4 ca rồi nối lại ở 7 ca

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-27; DD F4 API §2.4–2.5, §9 T-PG-01/04 |
| Kind / priority | Recovery, P0: không mất hoặc lặp ô khi bỏ lỡ message |
| Environment | Màn học viên tích hợp F4 REST/STOMP; có thể ngắt/mở kênh và giữ message |
| Preconditions | `P-LIVE` N=12; bốn ca đầu đều `AC`, client đang có ô 1..4 và `seq=6`; worker tiếp tục chạy |
| Test data | Trong lúc kênh rớt, ca 5=`WA`, ca 6–7=`AC`; GET progress khi 7 ca đã commit trả `seq=9`, `completedCount=7`, `passedCount=6`, `testcases` order 1..7 |
| Steps | Ngắt STOMP, cho worker commit ca 5..7; nối và SUBSCRIBE W1 trước, giữ một message trùng `seq=9`; GET progress; dựng lại UI, thả message trùng và cho ca 8..12 hoàn tất. |
| Expected | UI lập tức có 7 ô đúng màu, đếm 7/6; bỏ message `seq<=9` đã bao gồm trong snapshot, chỉ áp các message mới `seq>9`. Sau cùng 12 ô xuất hiện mỗi ô một lần, không mất vị trí 5 và không tô trùng; trạng thái cuối từ DB/bản tin đúng verdict. |
| Cleanup | Khôi phục kênh, dọn fixture |
| Evidence | Message/snapshot seq, request order SUBSCRIBE→GET, UI trước/sau, DB kết quả |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC27-02 — Bài kết thúc lúc mất kênh: ảnh chụp đưa thẳng tới kết quả cuối

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-27; DD F4 API §2.5 bước 4, §9 |
| Kind / priority | Recovery, P1: không mắc kẹt ở trạng thái chấm cũ |
| Environment | Màn học viên tích hợp F4 REST/STOMP |
| Preconditions | Client rớt W1 khi submission còn `JUDGING` và mới có 4/12 kết quả |
| Test data | Trong lúc rớt, worker hoàn thành cả 12 ca `AC`; GET progress trả `status=ACCEPTED`, `isFinal=true`, `seq=15` |
| Steps | Nối lại, SUBSCRIBE W1 rồi GET progress; quan sát lời gọi kế tiếp và UI. |
| Expected | Client gọi GET `/submissions/{id}` để lấy chi tiết cuối, hiển thị `ACCEPTED`, 12/12 và `scoreRatio=1.000`; không chờ thêm message từ broker hoặc duy trì dải ô đang chấm. |
| Cleanup | Khôi phục kênh, dọn fixture |
| Evidence | REST/STOMP timeline, response progress/detail và ảnh màn |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC28-01 — Không nối lại được: poll tối đa 10 lần và không tự kết luận trượt

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-28; DD F4 API §2.5 bước 5, §9 |
| Kind / priority | Degraded, P1: lỗi kênh không biến thành verdict sai |
| Environment | Màn học viên + F4 REST, clock ảo; STOMP bị ngắt |
| Preconditions | Submission đang `JUDGING`, GET progress ban đầu có 3/12 ca, `seq=5` |
| Test data | Mười response poll liên tiếp cách 3 giây: lần 1..5 `completedCount=3`, lần 6..10 `completedCount=4`, đều `isFinal=false`; sau đó tải lại trang khi DB vẫn `JUDGING` |
| Steps | Ngắt STOMP, đo mười lần GET, UI ở mỗi mốc; sau lần 10 chờ thêm 3 giây rồi reload. |
| Expected | Đúng 10 poll ở nhịp 3 giây, không có lần 11 tự động; UI cập nhật từ 3 lên 4 ô sau poll 6, sau poll 10 hiện nhắc cập nhật chậm chứ không báo `WRONG_ANSWER`/`SYSTEM_ERROR`. Reload lấy status thật `JUDGING` từ DB. |
| Cleanup | Khôi phục STOMP, dọn fixture |
| Evidence | Network timeline, 10 response, ảnh màn và status DB |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC29-01 — STOMP và progress chỉ chứa order/verdict, không chứa dữ liệu ẩn

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-29; DD F4 API §2.3, §9.2, BR-JD-11 |
| Kind / priority | Security, P0: không rò input/output Hidden qua đường tiến trình |
| Environment | F4 STOMP + REST progress + DB |
| Preconditions | `P-LIVE` có H1 ở vị trí 4; H1=`WA`, input marker `HIDDEN_INPUT_41`, expected `HIDDEN_EXPECTED_41`, actual output `HIDDEN_ACTUAL_41`, stderr `HIDDEN_STDERR_41`; A đang nghe W1 |
| Test data | Worker commit kết quả H1, sau đó A GET `/submissions/{id}/progress` |
| Steps | Thu mọi frame trong lượt, body progress, kiểm DB nội bộ rồi tìm bốn marker trên dữ liệu trả học viên. |
| Expected | `TESTCASE_FINISHED` của H1 chỉ có `order=4`, `verdict=WA` và số đếm; progress `testcases[]` chỉ có `{order,verdict}`. Không frame/response nào có input, expected, actual, stderr, testcaseId, runtime/memory của H1 hoặc bốn marker. DB vẫn giữ verdict để tính điểm; không ghi actual/stderr Hidden vào snippet. |
| Cleanup | Đóng W1, dọn fixture |
| Evidence | STOMP frames, response progress, DB nội bộ, kết quả dò marker |
| Design review | Đã review; chưa thực thi |
