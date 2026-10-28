# Test case QA — F4: lịch sử và số liệu của học viên

> 13 ca thiết kế cho AC-36–40; chưa thực thi. Nguồn: TDD F4 AC-36–40; DD F4 API §7, §10–12 và logic BR-JD-13. Kiểm JSON/API, DB và màn tương ứng; số liệu phải lấy theo `user_id` trong token. Dùng đồng hồ test cố định `2026-10-26T12:00:00+07:00`; mọi thời điểm bên dưới là UTC+7 và lưu timestamp tương ứng ở DB. `TC-F4-AC39-03` dùng thứ Hai đầu tuần UTC+7 cho nhãn nhóm tuần theo quyết định 2026-10-27.

| AC | Nhánh | ID | Review | Thực thi |
| :--- | :--- | :--- | :--- | :--- |
| 36 | 10 gần nhất, riêng chủ, chưa chấm, limit lỗi | TC-F4-AC36-01 đến -03 | Đã review | Chưa thực hiện |
| 37 | Phân trang/tìm/lọc, tham số lạ, cách ly | TC-F4-AC37-01 đến -03 | Đã review | Chưa thực hiện |
| 38 | Latest verdict, limit, withdrawn | TC-F4-AC38-01 đến -02 | Đã review | Chưa thực hiện |
| 39 | 7d/streak, 30d, all/rỗng | TC-F4-AC39-01 đến -03 | Đã review | Chưa thực hiện |
| 40 | Range lạ, không chọn user khác | TC-F4-AC40-01 đến -02 | Đã review | Chưa thực hiện |

### TC-F4-AC36-01 — Tab bài nộp trả mười lượt mới nhất của chủ bài

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-36; DD F4 API §7 T-LP-01/02 |
| Kind / priority | Normal + security, P0: không lộ lịch sử người khác |
| Environment | API #4, DB judge, FE `USR0102` |
| Preconditions | A có 12 submission bài X tại `10:00` đến `10:11` cùng ngày, ID `A01`–`A12`, mỗi phút một lượt; B có 3 lượt `B01`–`B03` cũng cho X; token A |
| Test data | `GET /problems/{X}/submissions/mine` không truyền limit |
| Steps | Gọi API token A; xem tab Bài nộp; đối chiếu DB theo userId/problemId. |
| Expected | `200`, đúng `A12` xuống `A03`, không có `A01/A02` hay `B*`; mỗi item có language, status, passedCount, totalCount, scoreRatio, runtimeMs, memoryKb, submittedAt; FE hiển thị mới nhất trước, không có mã nguồn/testcase chi tiết trong response. |
| Cleanup | Xoá fixture A/B/X |
| Evidence | Request/response, ảnh tab và DB query |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC36-02 — Lượt PENDING dùng null, không hiện 0/0

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-36; DD F4 API §7.3 T-LP-03 |
| Kind / priority | Boundary, P1: không gán điểm giả cho lượt chưa chấm |
| Environment | API #4 và FE `USR0102` |
| Preconditions | A có lượt `PENDING`, `total_testcase_count=NULL`, `passed_testcase_count=NULL`, runtime/memory null, thuộc bài X |
| Test data | GET của A với `limit=1` |
| Steps | Gọi API, xem item và tab. |
| Expected | `200`; `passedCount`, `totalCount`, `scoreRatio`, `runtimeMs`, `memoryKb` đều null; FE hiển thị dấu gạch cho số đếm, không hiện `0/0`. |
| Cleanup | Xoá fixture |
| Evidence | Response, DB row, ảnh tab |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC36-03 — Giới hạn tab chỉ nhận 1–10

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-36; DD F4 API §7.2/§7.4 T-LP-04 |
| Kind / priority | Boundary, P1: kiểm tham số trước truy vấn |
| Environment | API #4 |
| Preconditions | Token A hợp lệ, bài X tồn tại |
| Test data | `limit=1`, `limit=10`, `limit=0`, `limit=11` |
| Steps | Gọi bốn request độc lập; ghi status và số items. |
| Expected | 1 và 10 trả `200` với tối đa đúng limit; 0 và 11 trả `400 JDG-V14`, không đổi DB và không trả dữ liệu lịch sử. |
| Cleanup | Không có dữ liệu ghi |
| Evidence | Bốn request/response |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC37-01 — Lịch sử 45 lượt phân trang và trang vượt biên

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-37; DD F4 API §10 T-MS-01/04 |
| Kind / priority | Normal + boundary, P0: phân trang không mất hoặc lặp lượt |
| Environment | API #7, FE `USR0202`, DB judge |
| Preconditions | A có 45 lượt `A01`–`A45` trên nhiều bài tại 45 mốc phút khác nhau; B có 2 lượt; không có bộ lọc |
| Test data | `page=1`, `page=2`, `page=3`, `page=99` |
| Steps | Gọi bốn trang token A, gom ID và đối chiếu DB. |
| Expected | Trang 1/2 có 20 item, trang 3 có 5, `totalCount=45`; trang 99 được chuẩn về trang cuối hợp lệ và cùng năm item trang 3; 45 ID không lặp và theo `submittedAt` giảm dần, không có B. Item không có `sourceCode`/`testcaseDetail`. |
| Cleanup | Xoá fixture |
| Evidence | Bốn response, DB row order và ảnh phân trang |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC37-02 — Lọc verdict/ngôn ngữ và tìm không phân biệt hoa thường

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-37; DD F4 API §10.2/§10.5 T-MS-02/06 |
| Kind / priority | Normal, P1: điều kiện lọc giao nhau đúng |
| Environment | API #7, F2 ProblemSummaryReadPort, FE `USR0202` |
| Preconditions | A có bốn lượt: `S1` ACCEPTED/PYTHON bài `Two Sum`; `S2` ACCEPTED/JAVA cùng bài; `S3` WRONG_ANSWER/PYTHON cùng bài; `S4` PENDING/PYTHON bài `Tree Path` |
| Test data | `verdict=ACCEPTED&language=PYTHON&q=tWo`; rồi `verdict=ACCEPTED&q=two` |
| Steps | Gọi hai request, xem màn với cùng bộ lọc. |
| Expected | Request đầu chỉ trả S1; request sau trả S1 và S2, không lẫn S3/S4; q khớp tên bài không phân biệt hoa thường. Không có nguồn/testcase detail trong item. |
| Cleanup | Xoá fixture |
| Evidence | Fixture bài, request/response, ảnh màn |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC37-03 — Giá trị lọc lạ trở về ALL và không chọn được user khác

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-37; DD F4 API §10.2/§10.5 T-MS-03/05 |
| Kind / priority | Security + abnormal, P0: scope theo token |
| Environment | API #7, DB judge |
| Preconditions | A có S1/S2; B có B1; token A |
| Test data | `verdict=FOO&language=RUST&userId={B}` so với `verdict=ALL&language=ALL` |
| Steps | Gọi cả hai request với token A; so danh sách và tổng. |
| Expected | Cả hai `200` và cùng tập S1/S2, không có B1; tham số userId thừa không đổi phạm vi chủ sở hữu, không có lỗi enum và không trả dữ liệu B. |
| Cleanup | Xoá fixture |
| Evidence | Hai response và DB query theo user |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC38-01 — Trạng thái của lần nộp gần nhất quyết định đang làm dở

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-38; DD F4 API §11 T-IP-01/02/04 |
| Kind / priority | Normal + security, P0: danh sách không lấy best-ever thay latest |
| Environment | API #8, F2 ProblemSummaryReadPort, DB judge |
| Preconditions | Học viên A: bài A sai rồi Accepted; bài B sai; bài C Accepted rồi sai; bài D sai; bài E sai. Lượt mới nhất lần lượt A=10:01, B=10:05, C=10:04, D=10:03, E=10:02; học viên khác có lượt mới hơn trên các bài này |
| Test data | GET mặc định, rồi `limit=20`, token A |
| Steps | Gọi hai request; so `latestSubmissionId/status/submittedAt` với DB cho A. |
| Expected | Mặc định đúng B,C,D theo thời điểm giảm dần; limit 20 thêm E, không có A vì latest Accepted; C có mặt dù từng Accepted; không có lượt của user khác. Mỗi item mang thông tin lần nộp mới nhất của A, không phải điểm tốt nhất. |
| Cleanup | Xoá fixture |
| Evidence | Response, DB timeline và ảnh thẻ |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC38-02 — Lọc bài withdrawn trước limit và kiểm biên limit

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-38; DD F4 API §11.2–11.4 T-IP-03 |
| Kind / priority | Boundary, P1: bài rút không chiếm slot thẻ hợp lệ |
| Environment | API #8, F2 ProblemSummaryReadPort |
| Preconditions | A có bốn bài latest chưa Accepted, thời điểm giảm dần `W=10:04` đã withdrawn, `P=10:03`, `Q=10:02`, `R=10:01` còn công khai |
| Test data | `limit` vắng, `limit=1`, `limit=20`, `limit=0`, `limit=21` |
| Steps | Gọi năm request; so item và lỗi. |
| Expected | Mặc định đúng P,Q,R; limit 1 chỉ P; limit 20 cũng P,Q,R; W bị bỏ **trước** limit. 0 và 21 trả `400 JDG-V14`, không ghi DB. |
| Cleanup | Xoá fixture |
| Evidence | Năm response và trạng thái F2 |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC39-01 — 7d dùng ngày UTC+7, có cột 0 và streak tính từ hôm nay

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-39; DD F4 API §12.3 T-MM-01/02, logic BR-JD-13 |
| Kind / priority | Boundary, P0: cắt ngày đúng Việt Nam |
| Environment | API #9, DB judge, clock cố định |
| Preconditions | Token A; A nộp tại `2026-10-25 00:30+07` (UTC còn là ngày 24), `2026-10-25 23:30+07`, `2026-10-26 08:00+07`, `2026-10-22 12:00+07`; ngày 23,24,21 không có lượt; học viên B có lượt 23:45 ngày 25 |
| Test data | `GET /me/metrics?range=7d` tại `2026-10-26 12:00+07` |
| Steps | Gọi API; đối chiếu `dailyCounts`, `streakDays`, `lastSubmittedAt` với DB sau đổi UTC+7. |
| Expected | Đúng 7 phần tử ngày 20–26/10; count 22=1, 25=2, 26=1, các ngày còn lại 0; tổng 4; `streakDays=2` (ngày 26,25), `lastSubmittedAt=2026-10-26T08:00:00+07:00` hoặc cùng instant UTC; lượt 00:30 thuộc ngày 25 dù UTC là ngày 24; không đếm B. |
| Cleanup | Xoá fixture/khôi phục clock |
| Evidence | Response, DB UTC timestamps và phép đổi ngày |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC39-02 — 30d có 14 nhóm và bảo toàn tổng lượt

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-39; DD F4 API §12.3 T-MM-03, logic BR-JD-13 bước 4 |
| Kind / priority | Normal + boundary, P1: thuật toán gộp 30 ngày |
| Environment | API #9, DB judge, clock cố định |
| Preconditions | A có đúng ba lượt tại daysAgo=0, 2, 29 tính từ 26/10 UTC+7; một lượt ở daysAgo=30 nằm ngoài cửa sổ |
| Test data | `range=30d` |
| Steps | Gọi API, tính nhóm kỳ vọng `13 - floor(daysAgo * 14 / 30)` cho 0,2,29 và so 14 item. |
| Expected | Đúng 14 nhóm, nhóm 13 có 1, nhóm 13/12 theo công thức cho daysAgo=2 là 13, nên nhóm 13 có tổng 2; nhóm 0 có 1; các nhóm khác 0; tổng 3, lượt daysAgo=30 bị loại. `day` là ngày cuối mỗi nhóm theo UTC+7. |
| Cleanup | Xoá fixture/khôi phục clock |
| Evidence | Response và bảng tính nhóm 30 ngày |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC39-03 — All có tuần hiện tại; tài khoản rỗng trả streak 0

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-39; DD F4 API §12.3 T-MM-04, logic BR-JD-13 bước 4 |
| Kind / priority | Normal + boundary, P1: chuỗi tuần không bỏ tuần trống |
| Environment | API #9, clock cố định |
| Preconditions | A có một lượt thứ Hai `2026-10-12`, một lượt hôm nay `2026-10-26`; B chưa từng nộp bài |
| Test data | A gọi `range=all`; B gọi `range=7d` và `range=30d` |
| Steps | Gọi ba request; so `dailyCounts` và `streakDays`. |
| Expected | A có đúng ba phần tử tuần ISO `{day:"2026-10-12",count:1}`, `{day:"2026-10-19",count:0}`, `{day:"2026-10-26",count:1}` theo thứ Hai đầu tuần UTC+7; tuần hiện tại có mặt. B có `streakDays=0`, `lastSubmittedAt=null`, bảy cột 0 ở 7d, 14 nhóm 0 ở 30d. Không trả mã nguồn/testcase. |
| Cleanup | Xoá fixture |
| Evidence | Ba response và DB timeline |
| Design review | Đã review theo quyết định chủ dự án 2026-10-27; chưa thực thi |

### TC-F4-AC40-01 — Range ngoài tập đóng bị từ chối

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-40; DD F4 API §12.2/12.4 T-MM-05 |
| Kind / priority | Abnormal, P1: không đoán cửa sổ thời gian |
| Environment | API #9 |
| Preconditions | Token A hợp lệ |
| Test data | `range=1y`, `range=7d` |
| Steps | Gọi hai request độc lập; kiểm status và DB write count. |
| Expected | `1y` trả `400 JDG-V07`, không có payload metrics; `7d` trả `200`; cả hai chỉ đọc, không ghi DB. |
| Cleanup | Không có dữ liệu ghi |
| Evidence | Request/response và DB write trace |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC40-02 — Query userId thừa không đổi chủ số liệu

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-40; DD F4 API §12.1, logic BR-JD-13 bước 1 |
| Kind / priority | Security, P0: chống IDOR qua query |
| Environment | API #9, DB judge |
| Preconditions | A có 1 lượt hôm nay; B có 5 lượt hôm nay; token A |
| Test data | `GET /me/metrics?range=7d&userId={B}` và cùng URL không `userId` |
| Steps | Gọi hai request token A; so response và DB query user_id. |
| Expected | Hai response bằng nhau và chỉ tính 1 lượt của A; use case lấy userId từ token, không dùng B từ query. Không có nguồn/testcase chi tiết. |
| Cleanup | Xoá fixture |
| Evidence | Hai response và query trace |
| Design review | Đã review; chưa thực thi |
