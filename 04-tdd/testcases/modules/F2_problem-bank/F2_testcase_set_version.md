# Test case QA — F2: tạo phiên bản bộ testcase

> Trạng thái: 28 ca đã review thiết kế; **chưa thực thi**. Quy tắc `position` đã chốt: một thứ tự dùng chung, đổi thứ tự thực sự trên bài `PUBLISHED` tạo snapshot mới.

## Phạm vi và fixture

- TDD: `04-tdd/modules/F2_problem-bank.md` AC-56, AC-57, AC-59, AC-60.
- DD: `03-dd/logic/F2_problem-bank.md` BR-11, BR-12, BR-17; `03-dd/api/F2_problem-bank.md` mục 42–45, 47; `02-bd/database/F2_problem-bank.md` mục 1.15.
- `P-V1`: bài của giảng viên `A2-VERSION` đang `PUBLISHED`; có 8 testcase đã duyệt: Sample S1 `1 → 2`, S2 `2 → 3`; Hidden H1–H6 lần lượt `3 → 4`, `4 → 5`, `5 → 6`, `6 → 7`, `7 → 8`, `9 → 10`. `current_testcase_set_version = 1`, snapshot `(problemId, 1)` có `testcase_count=8` và checksum SHA-256 hợp lệ. Ký hiệu `→` là cặp input/expected output, mỗi giá trị có xuống dòng cuối.
- `D-AI`: testcase Hidden do AI sinh, `approved=false`, `origin=AI`, input `11\n`, expected output `12\n`; chưa thuộc snapshot. Tạo bằng fixture test theo hợp đồng sinh testcase F2, không giả làm testcase tay.
- `P-DRAFT`: bản sao của bài ở trạng thái `UNPUBLISHED`, có 8 testcase trên; ghi nhận phiên bản ban đầu là `vDraft` và không giả định giá trị số cụ thể.
- Với mỗi ca, dùng fixture riêng bắt đầu từ trạng thái trên; ID thật do API/fixture tạo được lưu trong run record. Trước và sau thao tác, ghi nhận phiên bản, snapshot row, checksum object MinIO và số sự kiện `TestcaseSetVersionBumped`. Không dùng cùng một bài đã bị ca trước sửa.

## Ma trận nhánh

| AC | Nhánh | Test case | Review | Thực thi |
| :--- | :--- | :--- | :--- | :--- |
| F2 AC-56 | Thêm Sample đã duyệt | TC-F2-AC56-01 | Đã review | Chưa thực hiện |
| F2 AC-56 | Thêm Hidden đã duyệt | TC-F2-AC56-02 | Đã review | Chưa thực hiện |
| F2 AC-56, AC-57 | Duyệt nháp AI | TC-F2-AC56-03 | Đã review | Chưa thực hiện |
| F2 AC-56 | Sửa nội dung Sample đã duyệt | TC-F2-AC56-04 | Đã review | Chưa thực hiện |
| F2 AC-56 | Sửa nội dung Hidden đã duyệt | TC-F2-AC56-05 | Đã review | Chưa thực hiện |
| F2 AC-56, AC-59 | Xoá Sample khi vẫn đủ ngưỡng | TC-F2-AC56-06 | Đã review | Chưa thực hiện |
| F2 AC-56, AC-59 | Xoá Hidden khi vẫn đủ ngưỡng | TC-F2-AC56-07 | Đã review | Chưa thực hiện |
| F2 AC-56 | Chỉ đổi category | TC-F2-AC56-08 | Đã review | Chưa thực hiện |
| F2 AC-56 | Sửa nháp chưa duyệt | TC-F2-AC56-09 | Đã review | Chưa thực hiện |
| F2 AC-56 | Sửa bài chưa xuất bản | TC-F2-AC56-10 | Đã review | Chưa thực hiện |
| F2 AC-56 | Thêm testcase vào bài chưa xuất bản | TC-F2-AC56-14 | Đã review | Chưa thực hiện |
| F2 AC-56 | Thêm lô nhiều ca chỉ tăng một lần | TC-F2-AC56-11 | Đã review | Chưa thực hiện |
| F2 AC-56 | Ghi snapshot lỗi phải rollback | TC-F2-AC56-12 | Đã review | Chưa thực hiện |
| F2 AC-56 | Gửi lại PATCH cùng giá trị | TC-F2-AC56-13 | Đã review | Chưa thực hiện |
| F2 AC-56, AC-60 | Đổi thứ tự bài đã xuất bản, submission cũ/mới | TC-F2-AC56-15 | Đã review | Chưa thực hiện |
| F2 AC-56, AC-60 | Đổi thứ tự bài chưa xuất bản | TC-F2-AC56-16 | Đã review | Chưa thực hiện |
| F2 AC-56, AC-60 | Gửi lại cùng thứ tự | TC-F2-AC56-17 | Đã review | Chưa thực hiện |
| F2 AC-56, AC-60 | Thiếu id trong danh sách | TC-F2-AC56-18 | Đã review | Chưa thực hiện |
| F2 AC-56, AC-60 | Kéo nháp AI làm đổi `position` | TC-F2-AC56-19 | Đã review | Chưa thực hiện |
| F2 AC-56, AC-60 | Ghi bundle lỗi khi sắp thứ tự | TC-F2-AC56-20 | Đã review | Chưa thực hiện |
| F2 AC-60 | A2 không sở hữu bài | TC-F2-AC56-21 | Đã review | Chưa thực hiện |
| F2 AC-60 | Bài có một testcase | TC-F2-AC56-22 | Đã review | Chưa thực hiện |
| F2 AC-60 | Bài chưa có testcase | TC-F2-AC56-23 | Đã review | Chưa thực hiện |
| F2 AC-57 | Hạ cờ duyệt đã chấp nhận bị từ chối | TC-F2-AC57-01 | Đã review | Chưa thực hiện |
| F2 AC-59 | Xoá nháp, xoá lặp, ngưỡng xuất bản, dọn MinIO | TC-F2-AC59-01 đến -04 | Đã review | Chưa thực hiện |

## Test case

### TC-F2-AC56-01 — Thêm Sample đã duyệt tạo snapshot mới

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-56; DD F2 mục 42, BR-11 |
| Kind / priority | Normal, P0: bộ chấm mới phải tồn tại trước khi công bố phiên bản |
| Environment | API F2 + PostgreSQL + MinIO + Outbox F2 |
| Preconditions | `P-V1` ở phiên bản 1; snapshot v1 đọc và kiểm checksum được |
| Test data | POST mục 42: `visibility=SAMPLE`, `input="12\n"`, `expectedOutput="13\n"` |
| Steps | Ghi metadata/checksum v1; POST testcase; đọc lại bài, testcase, snapshot và outbox sau commit. |
| Expected | 201, `origin=MANUAL`, `approved=true`; phiên bản đúng 2, snapshot v2 có 9 ca gồm Sample mới, checksum hợp lệ; v1 vẫn 8 ca và byte không đổi. Đúng một sự kiện `TestcaseSetVersionBumped` cho phiên bản 2 được ghi để chuyển phát; không công bố v2 nếu bundle chưa lưu. |
| Cleanup | Xoá bài và object fixture cô lập sau khi lưu bằng chứng |
| Evidence | Request/response, `problems`, `testcases`, hai dòng snapshot, SHA-256 và outbox |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC56-02 — Thêm Hidden đã duyệt cũng tạo snapshot mới

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-56; DD F2 mục 42, BR-11 |
| Kind / priority | Normal, P0: Hidden phải có trong bộ chấm mà không lộ ra học viên |
| Environment | API F2 + PostgreSQL + MinIO + Outbox F2 |
| Preconditions | `P-V1` ở v1 |
| Test data | POST mục 42: `visibility=HIDDEN`, `input="12\n"`, `expectedOutput="13\n"` |
| Steps | POST testcase; đọc lại phiên bản và hai bundle qua cổng kiểm thử nội bộ; gọi endpoint học viên xem Sample. |
| Expected | 201; phiên bản 2 có 9 ca, Hidden mới nằm trong v2 và không nằm trong v1; checksum cả hai hợp lệ. Một sự kiện tăng phiên bản; endpoint học viên không trả input/output hoặc URL của Hidden mới. |
| Cleanup | Dọn fixture cô lập |
| Evidence | API tác giả/học viên, snapshot v1/v2 và outbox |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC56-03 — Duyệt nháp AI chỉ tăng một phiên bản

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-56, AC-57; DD F2 mục 43, BR-11, BR-17 |
| Kind / priority | Normal, P0: nháp không được lẫn vào bộ chấm trước khi duyệt |
| Environment | API F2 + PostgreSQL + MinIO + Outbox F2 |
| Preconditions | `P-V1` ở v1 và có `D-AI` chưa duyệt; snapshot v1 không chứa `D-AI` |
| Test data | PATCH mục 43 cho `D-AI` với `{ "approved": true }` |
| Steps | Kiểm bundle v1, PATCH duyệt, đọc testcase và bundle v2. |
| Expected | 200; `approved=true`, `origin=AI` giữ nguyên; phiên bản 2 và snapshot v2 có 9 ca, v1 vẫn 8; đúng một sự kiện tăng phiên bản. Không có bước duyệt tự động trước PATCH. |
| Cleanup | Dọn fixture cô lập |
| Evidence | Testcase trước/sau, phản hồi PATCH, hai bundle, outbox |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC56-04 — Sửa Sample đã duyệt làm mới đúng nội dung bộ chấm

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-56; DD F2 mục 43 PB43-B1, BR-11 |
| Kind / priority | Normal, P0: bài nộp cũ phải còn Sample cũ |
| Environment | API F2 + PostgreSQL + MinIO + Outbox F2 |
| Preconditions | `P-V1` ở v1, S1 có `input="1\n"`, `expectedOutput="2\n"` |
| Test data | PATCH S1 thành `input="8\n"`, `expectedOutput="9\n"` |
| Steps | PATCH S1; so sánh S1 trong bundle v1, v2 và trong bảng hiện hành. |
| Expected | 200; phiên bản 2, v2 chứa S1 mới, v1 giữ S1 cũ; mỗi bundle có 8 ca và checksum riêng hợp lệ; đúng một sự kiện tăng phiên bản. Không ghi đè object v1. |
| Cleanup | Dọn fixture cô lập |
| Evidence | Response, S1 hiện hành, hai bundle/checksum, outbox |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC56-05 — Sửa Hidden đã duyệt làm mới bộ chấm

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-56; DD F2 mục 43, BR-11, BR-13 |
| Kind / priority | Normal, P0: Hidden mới phải nằm đúng snapshot nhưng không ra API học viên |
| Environment | API F2 + PostgreSQL + MinIO + Outbox F2 |
| Preconditions | `P-V1` ở v1; H1 hiện có `3 → 4` |
| Test data | PATCH H1 thành `input="13\n"`, `expectedOutput="14\n"` |
| Steps | PATCH H1; đọc v1/v2 qua cổng kiểm thử nội bộ; gọi API học viên đọc bài và Sample. |
| Expected | Phiên bản 2; v1 còn `3 → 4`, v2 có `13 → 14`; đúng một sự kiện tăng phiên bản. API học viên không trả nội dung/URL của H1 cũ hoặc mới. |
| Cleanup | Dọn fixture cô lập |
| Evidence | Response, bundle/checksum, outbox và phản hồi học viên |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC56-06 — Xoá Sample khi còn đủ hai Sample

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-56, AC-59; DD F2 mục 44 PB44-N1/B2, BR-11 |
| Kind / priority | Boundary, P0: không phá checklist của bài đang xuất bản |
| Environment | API F2 + PostgreSQL + MinIO + Outbox F2 |
| Preconditions | Từ `P-V1`, thêm Sample S3 `12 → 13` để có 9 ca, 3 Sample ở phiên bản `vBefore`; lưu bundle này |
| Test data | DELETE mục 44 cho S3 |
| Steps | DELETE S3; đọc bài, danh sách hiện hành và bundle `vBefore`/`vAfter`. |
| Expected | 200; `vAfter = vBefore + 1`, bộ hiện hành và snapshot mới còn 8 ca, 2 Sample; snapshot cũ vẫn 9 ca, 3 Sample. S3 không còn ở bảng hiện hành nhưng vẫn ở bundle cũ. Một sự kiện tăng phiên bản. |
| Cleanup | Dọn fixture cô lập |
| Evidence | Response, bảng testcase, hai bundle, outbox |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC56-07 — Xoá Hidden khi vẫn còn tám ca

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-56, AC-59; DD F2 mục 44 PB44-N1, BR-11 |
| Kind / priority | Boundary, P0: xoá hiện hành không xoá snapshot lịch sử |
| Environment | API F2 + PostgreSQL + MinIO + Outbox F2 |
| Preconditions | Từ `P-V1`, thêm Hidden H7 `12 → 13` để có 9 ca ở `vBefore` |
| Test data | DELETE mục 44 cho H7 |
| Steps | DELETE H7; đọc phiên bản, testcase hiện hành và bundle cũ/mới. |
| Expected | 200; `vAfter = vBefore + 1`, bundle mới 8 ca không có H7; bundle cũ 9 ca còn H7, byte/checksum cũ không đổi. Một sự kiện tăng phiên bản cho lần DELETE. |
| Cleanup | Dọn fixture cô lập |
| Evidence | Response, bảng testcase, hai bundle, outbox |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC56-08 — Chỉ đổi category không tăng phiên bản

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-56; DD F2 mục 43 PB43-N2, BR-11 |
| Kind / priority | Normal, P1: metadata không được tạo bộ chấm giả |
| Environment | API F2 + PostgreSQL + MinIO + Outbox F2 |
| Preconditions | `P-V1` ở v1; H1 có `category=NULL` |
| Test data | PATCH H1 với `{ "category": "BOUNDARY_LOW" }` |
| Steps | PATCH chỉ trường `category`; đọc lại bài, H1, snapshot và outbox. |
| Expected | Category đổi thành giá trị đã gửi; `currentTestcaseSetVersion` vẫn 1; không có dòng snapshot v2 hoặc sự kiện `TestcaseSetVersionBumped`; byte/checksum v1 giữ nguyên. |
| Cleanup | Dọn fixture cô lập |
| Evidence | Request/response, bài/testcase, snapshot và outbox |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC56-09 — Sửa nháp AI chưa duyệt không đổi bộ chấm

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-56; DD F2 mục 43, BR-11 |
| Kind / priority | Normal, P1: nháp không được đi vào snapshot |
| Environment | API F2 + PostgreSQL + MinIO + Outbox F2 |
| Preconditions | `P-V1` ở v1, có `D-AI` chưa duyệt và không nằm trong bundle v1 |
| Test data | PATCH `D-AI` input từ `11\n` thành `14\n`, expected từ `12\n` thành `15\n`; không gửi `approved` |
| Steps | PATCH nháp; đọc lại testcase, bundle và outbox. |
| Expected | Nháp giữ `approved=false`, `origin=AI`; nội dung nháp đổi; phiên bản vẫn 1, bundle v1 không có nháp, không có snapshot v2 hay sự kiện tăng phiên bản. |
| Cleanup | Dọn fixture cô lập |
| Evidence | Testcase trước/sau, response, snapshot và outbox |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC56-10 — Bài chưa xuất bản không tăng phiên bản khi sửa testcase

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-56; DD F2 mục 42–43, BR-11 |
| Kind / priority | Boundary, P1: phiên bản công bố chỉ thuộc bài xuất bản |
| Environment | API F2 + PostgreSQL + MinIO + Outbox F2 |
| Preconditions | `P-DRAFT` đang `UNPUBLISHED`, ghi nhận `vDraft` |
| Test data | PATCH Sample S1 từ `1 → 2` thành `8 → 9` |
| Steps | PATCH S1; đọc phiên bản, nội dung testcase hiện hành và tìm snapshot/sự kiện mới. |
| Expected | Nội dung S1 đổi; `currentTestcaseSetVersion` vẫn `vDraft`; không có snapshot phiên bản mới hay sự kiện `TestcaseSetVersionBumped`. Lần xuất bản đầu sau này mới tạo snapshot hiện hành theo BR-11. |
| Cleanup | Dọn fixture cô lập |
| Evidence | Response, bài/testcase, snapshot, outbox |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC56-11 — Xác nhận lô nhiều testcase chỉ tăng một phiên bản

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-56; DD F2 mục 47, BR-11, BR-14 |
| Kind / priority | Normal, P1: một giao dịch lô là một lần công bố bộ chấm |
| Environment | API F2 + Redis + MinIO + PostgreSQL + Outbox F2 |
| Preconditions | `P-V1` ở v1; đã xin URL và tải thành công đủ hai cặp testcase H7 `12 → 13`, H8 `13 → 14`, cùng `uploadId` còn hạn |
| Test data | POST mục 47 `{ "uploadId": <id của phiên tải> }` |
| Steps | Gọi confirm một lần; đọc hai testcase mới, phiên bản/snapshot và outbox. |
| Expected | 201, thêm đủ hai testcase đã duyệt; phiên bản đúng 2 chứ không phải 3; snapshot v2 có 10 ca, v1 còn 8; đúng một sự kiện tăng phiên bản cho lần confirm. |
| Cleanup | Dọn phiên Redis, object và bài fixture cô lập |
| Evidence | Request/response confirm, hai testcase, snapshot, outbox |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC56-12 — Ghi bundle lỗi không công bố phiên bản mới

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-56; DD F2 BR-11, BR-12 và API mục 42 |
| Kind / priority | Abnormal, P0: không được công bố phiên bản trỏ tới bundle thiếu |
| Environment | Tích hợp cô lập; fault injection MinIO tại bước ghi bundle snapshot |
| Preconditions | `P-V1` ở v1; API/DB hoạt động, MinIO được cấu hình lỗi đúng lúc tạo snapshot v2 |
| Test data | POST mục 42 thêm Hidden H7 `12 → 13` (nội dung ngắn, nên lỗi được tiêm vào ghi **bundle**, không phải lưu vế testcase) |
| Steps | Bật lỗi ghi bundle; gọi POST; tắt lỗi; đọc testcase, bài, snapshot và outbox. |
| Expected | API trả `503 PRB-052`; không có H7 trong DB, phiên bản vẫn 1, không có snapshot v2 hoặc sự kiện `TestcaseSetVersionBumped`. Snapshot v1 vẫn đọc/kiểm checksum được; object mồ côi nếu có chỉ được job dọn xử lý, không được coi là phiên bản công bố. |
| Cleanup | Tắt fault injection, dọn object mồ côi và fixture cô lập |
| Evidence | Lỗi API, DB trước/sau, danh sách object/snapshot, outbox và log lỗi đã che dữ liệu |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC56-13 — PATCH cùng nội dung không tăng phiên bản lần hai

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-56; DD F2 mục 43 (`Idempotent: có`), BR-11 (`thao tác làm đổi bộ testcase`) |
| Kind / priority | Concurrency/Retry, P1: retry không được tạo phiên bản giả |
| Environment | API F2 + PostgreSQL + MinIO + Outbox F2 |
| Preconditions | `P-V1` ở v1, S1 đang `1 → 2` |
| Test data | PATCH S1 thành `8 → 9`; gửi lại cùng payload một lần nữa |
| Steps | Lưu phiên bản/response sau lần PATCH thứ nhất; gọi lại cùng request và đọc lại phiên bản, snapshot, outbox. |
| Expected | Sau lần đầu là v2; lần hai vẫn thành công theo tính idempotent nhưng phiên bản vẫn 2; chỉ một snapshot v2 và một sự kiện tăng phiên bản cho thay đổi thực. Bundle v2 không bị ghi lại hoặc đổi checksum. |
| Cleanup | Dọn fixture cô lập |
| Evidence | Hai request/response, snapshot và outbox trước/sau retry |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC56-14 — Thêm testcase vào bài chưa xuất bản không tăng phiên bản

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-56; DD F2 mục 42 PB42-N1, BR-11 |
| Kind / priority | Normal, P1: bộ chấm chỉ công bố khi bài được xuất bản |
| Environment | API F2 + PostgreSQL + MinIO + Outbox F2 |
| Preconditions | `P-DRAFT` đang `UNPUBLISHED`, phiên bản `vDraft` |
| Test data | POST mục 42 thêm Hidden H7 với `input="12\n"`, `expectedOutput="13\n"` |
| Steps | POST H7; đọc phiên bản, testcase hiện hành, snapshot và outbox. |
| Expected | 201, H7 `approved=true` được lưu trong bảng hiện hành; phiên bản vẫn `vDraft`, không có snapshot phiên bản mới hay sự kiện `TestcaseSetVersionBumped`. |
| Cleanup | Dọn fixture cô lập |
| Evidence | Request/response, bài/testcase, snapshot, outbox |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC56-15 — Đổi thứ tự bài đã xuất bản tạo snapshot mới, giữ thứ tự của bài nộp cũ

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-56, AC-60; DD F2 API §45 PB45-N1, BR-11; DD F4 BR-JD-03 bước 5, BR-JD-05 |
| Kind / priority | Normal/Integration, P0: thứ tự mới phải được ghim đúng lúc nộp |
| Environment | API F2/F4 + PostgreSQL + MinIO + Outbox + RabbitMQ + worker go-judge; có thể tạm dừng consumer trong môi trường kiểm thử |
| Preconditions | `P-V1` có thứ tự `S1,S2,H1,H2,H3,H4,H5,H6`, snapshot v1 và checksum hợp lệ; học viên `A1-VERSION` được nộp; consumer tạm dừng trước khi tạo submission cũ |
| Test data | `orderedIds=[H1,S1,S2,H2,H3,H4,H5,H6]` với UUID thật của fixture; mã Python `n = int(input())\nprint(0 if n in (1, 3) else n + 1)\n` sai ở S1 và H1, đúng sáu ca còn lại |
| Steps | 1. Nộp mã, ghi `old` đang `PENDING` ghim v1. 2. PUT §45 với `orderedIds`, lưu response và checksum v2. 3. Nộp cùng mã, ghi `new` ghim v2. 4. Bật consumer, chờ cả hai kết thúc; đọc bảng testcase hiện hành, hai snapshot, kết quả testcase và Outbox. |
| Expected | PUT trả 200, `items` có `position=1..8` đúng danh sách gửi và `currentTestcaseSetVersion=2`. Snapshot v1 bất biến; snapshot v2 có H1 ở vị trí 1 và S1 ở vị trí 2; SHA-256 v2 hợp lệ; đúng một `TestcaseSetVersionBumped` cho thao tác. `old` dùng v1, kết quả đầu tiên là S1 tại `testcase_order=1`; `new` dùng v2, kết quả đầu tiên là H1 tại `testcase_order=1`; cả hai có 8 kết quả, 6 AC/2 WA, `scoreRatio=0.750`, trạng thái `WRONG_ANSWER`. Không có submission nào đọc `position` hiện hành thay snapshot đã ghim. |
| Cleanup | Bật lại consumer kể cả khi ca lỗi; dọn bài, submission và object fixture cô lập sau khi lưu bằng chứng |
| Evidence | Hai request nộp/response PUT, DB phiên bản và `position`, bundle/checksum v1-v2, hai submission và kết quả chi tiết, Outbox |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC56-16 — Đổi thứ tự bài chưa xuất bản không tăng phiên bản

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-56, AC-60; DD F2 API §45 PB45-N2, BR-11 |
| Kind / priority | Normal, P1: không công bố bộ chấm khi bài chưa xuất bản |
| Environment | API F2 + PostgreSQL + MinIO + Outbox F2 |
| Preconditions | `P-DRAFT` có tám id theo `S1,S2,H1,H2,H3,H4,H5,H6`, phiên bản `vDraft` |
| Test data | `orderedIds=[H1,S1,S2,H2,H3,H4,H5,H6]` |
| Steps | PUT §45; đọc response, các dòng testcase, phiên bản, snapshot và Outbox. |
| Expected | 200; `items` và bảng hiện hành có vị trí 1..8 theo thứ tự gửi; `currentTestcaseSetVersion=vDraft`; không tạo snapshot mới hoặc `TestcaseSetVersionBumped`. |
| Cleanup | Dọn fixture cô lập |
| Evidence | Request/response, DB trước/sau, danh sách snapshot và Outbox |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC56-17 — Gửi lại đúng thứ tự hiện hành là no-op

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-56, AC-60; DD F2 API §45 PB45-B2, BR-11 |
| Kind / priority | Retry/Boundary, P1: không tạo phiên bản giả khi gửi lặp |
| Environment | API F2 + PostgreSQL + MinIO + Outbox F2 |
| Preconditions | `P-V1` đã `PUBLISHED`, thứ tự `S1,S2,H1,H2,H3,H4,H5,H6`, v1 |
| Test data | `orderedIds=[S1,S2,H1,H2,H3,H4,H5,H6]`, gửi hai lần với cùng UUID thật |
| Steps | PUT §45 hai lần; sau mỗi lần đọc response, phiên bản, snapshot, checksum v1 và Outbox. |
| Expected | Cả hai lần trả 200, `items` giữ vị trí 1..8, `currentTestcaseSetVersion=1`; không có snapshot v2, không phát `TestcaseSetVersionBumped`; checksum v1 không đổi. |
| Cleanup | Dọn fixture cô lập |
| Evidence | Hai request/response, DB, checksum và Outbox trước/sau |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC56-18 — Thiếu một id bị từ chối nguyên tử

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-56, AC-60; DD F2 API §45 PB45-A1 |
| Kind / priority | Abnormal, P1: không ghi nửa danh sách khi dữ liệu gửi thiếu |
| Environment | API F2 + PostgreSQL + MinIO + Outbox F2 |
| Preconditions | `P-V1` ở v1, có tám id `S1,S2,H1,H2,H3,H4,H5,H6` |
| Test data | `orderedIds=[H1,S1,S2,H2,H3,H4,H5]`, thiếu H6 |
| Steps | PUT §45; đọc mã lỗi, thứ tự DB, phiên bản, snapshot và Outbox. |
| Expected | `422 PRB-049`; cả tám `position` giữ như trước; phiên bản vẫn 1, snapshot v1 bất biến; không có snapshot v2 hoặc `TestcaseSetVersionBumped`. |
| Cleanup | Dọn fixture cô lập |
| Evidence | Request/lỗi API, DB trước/sau, snapshot và Outbox |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC56-19 — Kéo nháp AI trên bài đã xuất bản vẫn tạo một snapshot mới

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-56, AC-60; DD F2 API §45 PB45-N1, BR-11; quyết định một `position` dùng chung |
| Kind / priority | Boundary, P1: đổi `position` của bảng chung phải có phiên bản mới dù nháp không vào bộ chấm |
| Environment | API F2 + PostgreSQL + MinIO + Outbox F2 |
| Preconditions | `P-V1` ở v1; thêm `D-AI` ở `position=9`, `approved=false`; sau thao tác thêm nháp, phiên bản vẫn 1 |
| Test data | `orderedIds=[D-AI,S1,S2,H1,H2,H3,H4,H5,H6]` |
| Steps | PUT §45; đọc bảng hiện hành, response, bundle v1/v2, phiên bản và Outbox. |
| Expected | 200; `D-AI` ở vị trí 1, S1 ở 2, H6 ở 9. Phiên bản tăng thành 2 đúng một lần; snapshot v2 chỉ gồm tám testcase đã duyệt với vị trí tuyệt đối 2..9, không chứa `D-AI`; snapshot v1 không đổi và có một `TestcaseSetVersionBumped`. Khi F4 sắp theo `position`, thứ tự đã duyệt vẫn S1,S2,H1..H6. |
| Cleanup | Dọn fixture cô lập |
| Evidence | Request/response, testcase hiện hành, bundle/checksum v1-v2, phiên bản và Outbox |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC56-20 — Ghi snapshot lỗi khi sắp thứ tự phải rollback

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-56, AC-60; DD F2 API §45 PB45-A2, BR-11/BR-12 |
| Kind / priority | Recovery, P0: không công bố thứ tự mới khi không có bundle tương ứng |
| Environment | Tích hợp F2 + PostgreSQL + MinIO + Outbox; fault injection lỗi ghi bundle mới |
| Preconditions | `P-V1` ở v1, thứ tự `S1,S2,H1,H2,H3,H4,H5,H6`; checksum v1 hợp lệ |
| Test data | `orderedIds=[H1,S1,S2,H2,H3,H4,H5,H6]`; chỉ gây lỗi MinIO tại bước ghi snapshot v2 |
| Steps | Bật fault injection, PUT §45, tắt fault; đọc vị trí DB, phiên bản, snapshot và Outbox. |
| Expected | `503 PRB-052`; tám vị trí và phiên bản 1 giữ nguyên, không có snapshot v2 hoặc `TestcaseSetVersionBumped`; bundle v1 vẫn hợp lệ. Object mồ côi nếu có không được coi là phiên bản công bố và được dọn theo BR-12. |
| Cleanup | Tắt fault injection kể cả khi ca lỗi; dọn object mồ côi và fixture cô lập |
| Evidence | Request/lỗi API, DB trước/sau, danh sách object/snapshot, Outbox, log dọn object đã che dữ liệu |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC56-21 — Tác giả khác không được đổi thứ tự bài

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-60; DD F2 API §45 PB45-S1, quyền `TESTCASE_MANAGEMENT:UPDATE` |
| Kind / priority | Security, P1: quyền A2 bị giới hạn theo bài sở hữu |
| Environment | API F2 + PostgreSQL + MinIO + Outbox F2 |
| Preconditions | `P-V1` thuộc `A2-VERSION`; A2 khác là `A2-OTHER` có quyền quản lý testcase nói chung nhưng không sở hữu bài |
| Test data | JWT của `A2-OTHER`; `orderedIds=[H1,S1,S2,H2,H3,H4,H5,H6]` |
| Steps | Dùng JWT của `A2-OTHER` PUT §45; đọc thứ tự, phiên bản, snapshot và Outbox. |
| Expected | `403 PRB-003`; thứ tự vẫn `S1,S2,H1,H2,H3,H4,H5,H6`, phiên bản vẫn 1; không có snapshot v2 hoặc sự kiện tăng phiên bản. |
| Cleanup | Dọn fixture và tài khoản thử cô lập |
| Evidence | JWT đã che, request/lỗi API, DB và Outbox trước/sau |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC56-22 — Một testcase không tạo thứ tự mới

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-60; DD F2 API §45 PB45-B1 |
| Kind / priority | Boundary, P2: không tạo phiên bản giả ở tập một phần tử |
| Environment | API F2 + PostgreSQL + MinIO + Outbox F2 |
| Preconditions | Bài `UNPUBLISHED` riêng `P-ONE` có đúng một testcase đã duyệt `S1`, `position=1`; ghi phiên bản ban đầu `vOne` |
| Test data | `orderedIds=[S1]` |
| Steps | PUT §45; đọc response, vị trí, phiên bản, snapshot và Outbox. |
| Expected | 200, `items=[{id:S1,position:1}]`, `currentTestcaseSetVersion=vOne`; không tạo snapshot/sự kiện mới. |
| Cleanup | Dọn fixture cô lập |
| Evidence | Request/response, DB, snapshot và Outbox |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC56-23 — Danh sách rỗng trên bài chưa có testcase

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-60; DD F2 API §45 PB45-B1 |
| Kind / priority | Boundary, P2: tập rỗng hợp lệ theo hợp đồng |
| Environment | API F2 + PostgreSQL + MinIO + Outbox F2 |
| Preconditions | Bài `UNPUBLISHED` riêng `P-EMPTY` chưa có testcase; ghi phiên bản ban đầu `vEmpty` |
| Test data | `orderedIds=[]` |
| Steps | PUT §45; đọc response, bảng testcase, phiên bản, snapshot và Outbox. |
| Expected | 200, `items=[]`, `currentTestcaseSetVersion=vEmpty`; bảng testcase vẫn rỗng, không tạo snapshot/sự kiện mới. |
| Cleanup | Dọn fixture cô lập |
| Evidence | Request/response, DB, snapshot và Outbox |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC57-01 — Không thể hạ cờ duyệt của testcase đã duyệt

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-57; DD F2 API §43, BR-17 |
| Kind / priority | Abnormal, P0: không làm bộ chấm mất ca qua PATCH |
| Environment | API F2 + PostgreSQL + MinIO + Outbox |
| Preconditions | Bản sao `P-V1`; S1 đã `approved=true`, `origin=MANUAL`, snapshot v1 hợp lệ |
| Test data | `PATCH /authoring/problems/{P-V1}/testcases/{S1}` body `{"approved":false}` |
| Steps | Chụp S1, version/snapshot/outbox; gửi PATCH rồi đọc lại cùng bề mặt. |
| Expected | `400 PRB-001`; S1 vẫn `approved=true`, `origin=MANUAL`, `position` và input/output không đổi; version vẫn 1, checksum snapshot v1 không đổi, không có snapshot v2 hoặc event tăng phiên bản. |
| Cleanup | Dọn bản sao |
| Evidence | Request/response, S1 trước–sau, version, checksum và Outbox |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC59-01 — Xoá nháp AI không tăng phiên bản

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-59; DD F2 API §44 PB44-N2, BR-11 |
| Kind / priority | Normal, P1: nháp không thuộc snapshot chấm |
| Environment | API F2 + PostgreSQL + MinIO + Outbox |
| Preconditions | Bản sao `P-V1` có `D-AI` Hidden chưa duyệt ở position 9; version vẫn 1 |
| Test data | `DELETE /authoring/problems/{P-V1}/testcases/{D-AI}` không body |
| Steps | Chụp row nháp và snapshot v1; DELETE; đọc response, row, version, snapshot/Outbox. |
| Expected | `200`, `D-AI` không còn ở bảng hiện hành; `currentTestcaseSetVersion=1`, bundle/checksum v1 không đổi; không có snapshot v2 hoặc `TestcaseSetVersionBumped`. |
| Cleanup | Dọn bản sao |
| Evidence | Request/response, row, version, bundle và Outbox |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC59-02 — DELETE lần hai trả không tìm thấy

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-59; DD F2 API §44 PB44-A1 |
| Kind / priority | Retry, P1: xoá cứng không thành công giả lần hai |
| Environment | API F2 + PostgreSQL + Outbox |
| Preconditions | Bản sao `P-DRAFT` có testcase Hidden H1 đã duyệt, version `vDraft` |
| Test data | Gửi hai `DELETE /authoring/problems/{P-DRAFT}/testcases/{H1}` không body bằng cùng A2, cùng testcaseId |
| Steps | Gửi lần một, chụp DB; gửi lần hai, so DB/Outbox trước–sau lần hai. |
| Expected | Lần một `200` và H1 bị xoá cứng; lần hai `404 PRB-011`, không xoá thêm dòng, không đổi `vDraft` hay Outbox. |
| Cleanup | Dọn bản sao |
| Evidence | Hai response, row count, version và Outbox delta |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC59-03 — Bài xuất bản không được tụt dưới tám ca hoặc hai Sample

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-59; DD F2 API §44 PB44-A2, BR-07 |
| Kind / priority | Boundary, P0: giữ checklist xuất bản nguyên tử |
| Environment | API F2 + PostgreSQL + MinIO + Outbox |
| Preconditions | A là bản sao `P-V1` đúng 8 approved (2 Sample, 6 Hidden); B là bản sao riêng thêm H7 `10 → 11` đã duyệt để có 9 approved (2 Sample, 7 Hidden). Ghi version/snapshot hiện hành riêng là `vA`/`vB` |
| Test data | A: DELETE H1 làm tổng còn 7; B: DELETE S1 làm tổng còn 8 nhưng Sample còn 1; mỗi request dùng bài/UUID riêng |
| Steps | Gửi DELETE riêng A/B; sau mỗi lỗi đọc số approved, Sample, row bị nhắm, version, checksum và Outbox. |
| Expected | Cả A/B `422 PRB-054`; A còn 8 approved, B còn 9 approved và 2 Sample; H1/S1 vẫn tồn tại, version lần lượt `vA`/`vB`, snapshot/checksum hiện hành bất biến; không dọn object hoặc phát event. |
| Cleanup | Dọn hai bản sao |
| Evidence | Hai request/response, counts, rows, bundle/checksum và Outbox |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC59-04 — Lỗi dọn MinIO sau commit không đổi kết quả DELETE

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-59; DD F2 API §44 PB44-B1 và §44.5 |
| Kind / priority | Recovery, P0: lỗi dọn object không rollback DB đã commit |
| Environment | API F2 + PostgreSQL + MinIO fault injection + log vận hành |
| Preconditions | Hai bản sao `P-DRAFT` độc lập; H1 ở `storage=MINIO`, input key và expected key có object thật; version `vDraft` |
| Test data | A: MinIO delete cả hai object thành công; B: MinIO delete ném lỗi kết nối sau DB commit, object còn tồn tại; gửi DELETE H1 trên mỗi bản |
| Steps | Gọi DELETE A/B, đọc DB/response; kiểm object và log, rồi gỡ fault injection. |
| Expected | Cả A/B trả `200`, row H1 bị xoá cứng, version vẫn `vDraft`; A không còn hai object, B còn object và có log vận hành về lỗi dọn để xử lý tiếp. B không trả 5xx hoặc dựng lại row đã xoá; không có snapshot/event mới do bài chưa xuất bản. |
| Cleanup | Gỡ lỗi MinIO; dọn object còn sót của B và hai bản sao |
| Evidence | Hai response, DB, HEAD từng object, log vận hành và Outbox |
| Design review | Đã review; chưa thực thi |
