# Test case QA — F4: sự kiện và tóm tắt hạ tầng

> 4 ca thiết kế cho AC-55–56; chưa thực thi. Nguồn: TDD F4 AC-55–56; DD F4 API §18, §22, logic BR-JD-15. Đồng hồ cố định `T0=2026-10-26T12:00:00+07:00`; JWT có `JUDGE_QUEUE_MONITOR:READ`. Mỗi ca dùng dữ liệu riêng, kiểm `judge.outbox_events`, `judge.submissions`, JSON và màn ADM0404/ADM0401.

### TC-F4-AC55-01 — Hai nguồn, ngưỡng mức độ và vệ sinh thông điệp

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-55; DD F4 API §22.5/22.6 T-IE-01/02/06/07, BR-JD-15 |
| Kind / priority | Normal + security, P0: không lộ nội dung người dùng |
| Environment | API `GET /admin/infra-events`, DB, FE ADM0404, clock T0 |
| Preconditions | Outbox O2 thử 2 lần, O3 thử 3, O4 thử 4, O5 thử 5 trong 24h; OOLD thử 5 lần nhưng `created_at=T0-91 ngày`. Submissions E1/E2 `SYSTEM_ERROR` với reason `WORKER_TIMEOUT`/`SANDBOX_FAULT`, `finished_at` trong 24h; nguồn E1 chứa marker `STUDENT_SECRET`, testcase có marker `HIDDEN_SECRET`. |
| Test data | GET không lọc; rồi `level=info` |
| Steps | Gọi hai request; đối chiếu từng nguồn và tìm marker trong toàn JSON/ảnh màn. |
| Expected | Không lọc: O3/O4 là `WARN/outbox`, O5 `ERROR/outbox`, E1/E2 `ERROR/go-judge`; không có O2/OOLD. `occurredAt` của Outbox là created_at, của lỗi chấm là finished_at; message từ khuôn hệ thống chỉ có ID/reason an toàn, không có source, input, expected hay hai marker. `level=info` trả `200 events=[]`, không báo lỗi. |
| Cleanup | Dọn outbox/submission fixture |
| Evidence | DB rows/timestamps, hai JSON, ảnh màn, tìm marker |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC55-02 — Cursor 20 dòng và bộ lọc

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-55; DD F4 API §22.2/22.3/22.5 T-IE-03/04/08 |
| Kind / priority | Normal + boundary, P1: không mất/lặp sự kiện |
| Environment | API #20, DB, FE ADM0404 |
| Preconditions | Tạo 23 sự kiện hợp lệ với `occurredAt` giảm từng phút, một sự kiện `SANDBOX_FAULT` có khuôn chứa “go-judge”; thêm một sự kiện ngoài 90 ngày. |
| Test data | GET mặc định, GET bằng nextCursor; `q=GO-JUDGE&service=go-judge&level=ERROR`; `level=xyz&service=xyz` |
| Steps | Lấy hai trang, so ID/ngày và cursor; áp filter, rồi giá trị enum lạ; bấm Tải thêm. |
| Expected | Trang đầu 20, trang hai 3, mới nhất trước, không trùng/mất; `nextCursor` trang cuối null và `services=[go-judge,outbox]`. q không phân biệt hoa thường, filter giao nhau đúng; enum lạ bị bỏ qua nên trả cùng tập như không lọc. Sự kiện cũ không có ở trang nào. FE nối thêm 3 dòng, không nhân đôi 20 dòng đầu. |
| Cleanup | Dọn fixture |
| Evidence | Các request/response, DB order, ảnh hai trang |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC55-03 — Khoảng ngày ngược bị từ chối, ngày biên UTC+7 được nhận

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-55; DD F4 API §22.2/22.4 T-IE-05 |
| Kind / priority | Abnormal + boundary, P1 |
| Environment | API #20, DB, clock T0 |
| Preconditions | Một sự kiện `2026-10-25T00:30+07`, một sự kiện `2026-10-26T00:00+07` |
| Test data | `from=2026-10-26&to=2026-10-25`; rồi `from=2026-10-25&to=2026-10-25` |
| Steps | Gọi hai request, kiểm status, ngày và DB write count. |
| Expected | Khoảng ngược trả `400 JDG-V13`, không có events; khoảng ngày 25 trả đúng sự kiện 00:30 ngày 25, không có sự kiện ngày 26. Không ghi DB. |
| Cleanup | Dọn fixture |
| Evidence | Hai response và timestamps UTC/UTC+7 |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC56-01 — Thẻ 24 giờ bằng danh sách cùng nguồn

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-56; DD F4 API §18 T-IS-01–03, §22; BR-JD-15 |
| Kind / priority | Consistency + boundary, P1 |
| Environment | GET `/admin/queue/infra-summary`, GET `/admin/infra-events`, DB, FE ADM0401/0404, clock T0 |
| Preconditions | Trong 24h: outbox 3 lần WARN và 5 lần ERROR; một SYSTEM_ERROR go-judge; ngoài 24h: một SYSTEM_ERROR finished_at=T0-25h; các nguồn đều trong 90 ngày. |
| Test data | GET summary; GET events `from=2026-10-25&to=2026-10-26`, lọc thêm 24h bằng occurredAt chính xác |
| Steps | Đọc cả hai API cùng T0; đếm events WARN/ERROR có occurredAt >=T0-24h; mở thẻ và danh sách. |
| Expected | Summary `200 {warnCount:1,errorCount:2,windowHours:24}`; đúng bằng số dòng 24h theo từng mức từ danh sách. E25 không tính vào thẻ dù còn ở danh sách 90 ngày; thẻ liên kết tới màn sự kiện. Người thiếu JUDGE_QUEUE_MONITOR:READ nhận `403 JDG-301` ở cả hai API. |
| Cleanup | Dọn fixture |
| Evidence | Hai JSON, DB timestamps, ảnh hai màn và response thiếu quyền |
| Design review | Đã review; chưa thực thi |
