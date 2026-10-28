# Test case QA — F2/F4: snapshot bộ testcase khi chấm bài

> Trạng thái: đã review thiết kế; **chưa thực thi**. Đây là test case QA của hệ thống, không phải các testcase đầu vào/đầu ra dùng để chấm học viên.

## Phạm vi và nguồn

- TDD: `04-tdd/modules/F2_problem-bank.md` AC-93; `04-tdd/modules/F4_judge-orchestration.md` AC-74, AC-75.
- DD: `03-dd/logic/F2_problem-bank.md` BR-11; `03-dd/api/F2_problem-bank.md` mục 40, 43, 44, 58; `03-dd/logic/F4_judge-orchestration.md` BR-JD-01, BR-JD-03, BR-JD-05; `03-dd/api/F4_judge-orchestration.md` mục 6, 8; `02-bd/database/F2_problem-bank.md` mục 1.15.
- Môi trường chạy sau này: backend F2/F4, PostgreSQL, MinIO, RabbitMQ và worker chấm thật hoặc cổng sandbox được kiểm soát. Việc giữ bài ở `PENDING`, sửa/xoá object snapshot và trả đặc tả lỗi chỉ thực hiện trong môi trường kiểm thử cô lập.

## Fixture chung

| Mã | Dữ liệu cố định |
| :--- | :--- |
| `P-SNAP` | Bài Standard I/O đã `PUBLISHED`, ngôn ngữ Python bật, giới hạn chưa chạm; đề yêu cầu đọc một số nguyên `n` và in `n + 1`. Tác giả `A2-SNAP` có quyền sửa testcase; học viên `A1-SNAP` có quyền nộp. |
| Bộ A | 8 testcase đã duyệt theo `position`: Sample S1 `1 → 2`, Sample S2 `2 → 3`, Hidden H1–H6 lần lượt `3 → 4`, `4 → 5`, `5 → 6`, `6 → 7`, `7 → 8`, `9 → 10`. Input và expected output mỗi giá trị có ký tự xuống dòng cuối. Khi xuất bản lần đầu, ghi nhận phiên bản thực tế là `vA` và SHA-256/object key của snapshot A. |
| Bộ B | Dùng `PATCH` mục 43 đổi **S1 đã duyệt** từ `1 → 2` thành `8 → 9`, giữ 7 testcase còn lại. Phản hồi phải có `currentTestcaseSetVersion = vA + 1`; gọi phiên bản này là `vB`. |
| `SRC-EDGE` | Mã Python `n = int(input())\nprint(0 if n == 8 else n + 1)\n`. Với A: 8/8 AC, `ACCEPTED`, `scoreRatio = 1.000`. Với B: S1 là WA, 7/8 AC, `WRONG_ANSWER`, `scoreRatio = 0.875`, lỗi đầu tiên ở `testcase_order = 1`. |
| `SRC-OK` | Mã Python `n = int(input())\nprint(n + 1)\n`; đạt mọi ca hợp lệ của A/B. |
| Quy tắc thời gian | Tạm dừng consumer trước khi tạo bài nộp cần giữ `PENDING`; thời điểm nhận bài là lúc F4 ghi dòng `submissions`, không phải lúc worker bắt đầu chấm. Sau mỗi ca khôi phục consumer và xoá dữ liệu fixture cô lập. |

ID bài, testcase và submission do fixture tạo được lưu thành biến `problemId`, `testcaseId`, `submissionId` trong bằng chứng; không giả định UUID do hệ thống sinh sẵn. Các mũi tên trong bảng biểu diễn cặp input/expected output, không phải phản hồi API.

## Ma trận độ phủ thiết kế

| AC | Nhánh độc lập | Test case | Review | Thực thi |
| :--- | :--- | :--- | :--- | :--- |
| F2 AC-93, F4 AC-75 | Bài cũ ghim A, bài mới dùng B | TC-F2-AC93-01 | Đã review | Chưa thực hiện |
| F2 AC-93, F4 AC-75 | Xoá testcase hiện hành không đổi bài cũ | TC-F2-AC93-02 | Đã review | Chưa thực hiện |
| F2 AC-93, F4 AC-75 | Rút bài sau khi nhận lượt nộp | TC-F2-AC93-03 | Đã review | Chưa thực hiện |
| F2 AC-93, F4 AC-74 | Snapshot đã ghim thiếu object | TC-F2-AC93-04 | Đã review | Chưa thực hiện |
| F2 AC-93, F4 AC-74 | Snapshot đã ghim sai checksum | TC-F2-AC93-05 | Đã review | Chưa thực hiện |
| F4 AC-74 | Đặc tả bài không parse được sau khi nhận | TC-F4-AC74-01 | Đã review | Chưa thực hiện |

## Test case

### TC-F2-AC93-01 — Lượt cũ chấm bằng A, lượt mới chấm bằng B

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-93, F4 AC-75; F2 BR-11; F4 BR-JD-01 bước 6, BR-JD-03 bước 5, BR-JD-05 bước 1–3; F2 API mục 43 |
| Kind / priority | Concurrency, P0: sửa bộ chấm khi bài đã nằm trong hàng đợi có thể làm sai verdict |
| Environment | Tích hợp F2/F4 + PostgreSQL + MinIO + RabbitMQ + worker; kiểm soát được consumer |
| Preconditions | `P-SNAP` đang dùng bộ A; snapshot A có 8 ca và checksum hợp lệ; consumer tạm dừng nhưng API vẫn nhận bài |
| Test data | `SRC-EDGE`; bộ A và B ở mục Fixture chung |
| Steps | 1. A1 nộp `SRC-EDGE`, lưu `submissionId=old`; kiểm dòng `PENDING` ghim `vA`. 2. A2 `PATCH` S1 thành `8 → 9`, lưu `vB`. 3. Bật consumer, chờ `old` kết thúc. 4. A1 nộp cùng mã lần nữa, lưu `submissionId=new`, chờ kết thúc. |
| Expected | `vB = vA + 1`, snapshot A còn nguyên byte/checksum và snapshot B chứa S1 mới. `old.testcase_set_version_at_grading = vA`, `ACCEPTED`, 8/8, `scoreRatio=1.000`; `new` ghim `vB`, `WRONG_ANSWER`, 7/8, `scoreRatio=0.875`, `firstFailingOrder=1`. Không có testcase của B trong kết quả `old`, hoặc testcase của A trong kết quả `new`; mỗi submission có đúng một trạng thái cuối. |
| Cleanup | Xoá fixture cô lập sau khi lưu bằng chứng; bảo đảm consumer được bật lại kể cả khi ca trượt |
| Evidence | Phản hồi tạo/sửa, hai dòng `submissions`, hai bộ `submission_testcase_results`, metadata và SHA-256 của snapshot A/B, kết quả API mục 8 |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC93-02 — Xoá testcase hiện hành vẫn chấm đủ snapshot cũ

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-93, F4 AC-75; F2 BR-11, API mục 44; F4 BR-JD-03 bước 5 |
| Kind / priority | Recovery, P0: không được thay bộ đã ghim bằng bảng testcase hiện hành |
| Environment | Tích hợp F2/F4 + PostgreSQL + MinIO + RabbitMQ + worker |
| Preconditions | Bắt đầu từ A, thêm Hidden H7 `10 → 11` đã duyệt để có bộ 9 ca `v9`; consumer tạm dừng. Xoá H7 sau đó vẫn giữ 8 ca và 2 Sample, đủ checklist xuất bản. |
| Test data | `SRC-OK`; H7 có `visibility=HIDDEN`, `approved=true`, `position=9` |
| Steps | 1. A1 nộp `SRC-OK` lúc bộ 9 ca còn hiệu lực, lưu `old` và `v9`. 2. A2 gọi DELETE mục 44 cho H7, lưu phiên bản mới `v8`. 3. Bật consumer, chờ `old` xong. 4. Nộp `new` với cùng mã và chờ xong. |
| Expected | DELETE trả 200; `v8 = v9 + 1`; H7 không còn trong bảng testcase hiện hành, nhưng bundle `v9` vẫn có 9 ca và checksum không đổi. `old` ghim `v9`, `ACCEPTED`, 9/9; `new` ghim `v8`, `ACCEPTED`, 8/8. H7 chỉ có trong kết quả `old`; không có lỗi do testcase_id đã xoá khỏi bảng hiện hành. |
| Cleanup | Xoá fixture cô lập; bật lại consumer trong mọi nhánh kết thúc |
| Evidence | Phản hồi DELETE, phiên bản và metadata snapshot, dòng testcase hiện hành, hai submission và số dòng kết quả 9/8 |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC93-03 — Rút bài không làm mất bộ chấm của lượt đã nhận

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-93, F4 AC-75; F2 BR-11, API mục 40 và 58; F4 BR-JD-01 bước 2, BR-JD-03 bước 5 |
| Kind / priority | Recovery, P1: lịch sử nộp bài phải còn chấm và đọc được sau khi rút bài |
| Environment | Tích hợp F2/F4 + PostgreSQL + MinIO + RabbitMQ + worker |
| Preconditions | `P-SNAP` đang xuất bản với bộ A; consumer tạm dừng |
| Test data | `SRC-OK`, bộ A |
| Steps | 1. A1 nộp `SRC-OK`, nhận `old` ở `PENDING` ghim `vA`. 2. A2 chuyển bài sang `UNPUBLISHED` qua mục 40. 3. Bật consumer, chờ `old` kết thúc; A1 đọc kết quả và thử nộp lượt mới. |
| Expected | `old` vẫn chấm đủ 8 ca của A và kết thúc `ACCEPTED`, 8/8; snapshot A không bị xoá/đổi khi rút bài. Kết quả lịch sử của `old` vẫn đọc được; yêu cầu nộp mới bị từ chối theo `JDG-101` trước khi tạo submission mới. |
| Cleanup | Xoá fixture cô lập; bật lại consumer |
| Evidence | Phản hồi đổi trạng thái bài, snapshot A, kết quả `old`, lỗi nộp mới và số dòng submission trước/sau lỗi |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC93-04 — Snapshot A thiếu sau khi đã nhận bài

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-93, F4 AC-74; F2 BR-11; F4 BR-JD-03 bước 5, BR-JD-05 bước 5–7 |
| Kind / priority | Abnormal, P0: không được chấm nhầm bằng bundle hiện hành khi bundle đã ghim mất |
| Environment | Tích hợp cô lập; có quyền fault injection xoá object MinIO sau khi submission đã `PENDING` |
| Preconditions | `old` đã được nhận với `vA`; consumer tạm dừng; A2 đã công bố bộ B hợp lệ để có lựa chọn fallback sai cần phát hiện |
| Test data | `SRC-EDGE`; object key snapshot A lấy từ metadata test fixture, không lấy từ API học viên |
| Steps | 1. Ghi checksum/metadata `vA`, `vB`. 2. Chỉ trong môi trường test, xoá object bundle `vA` khỏi MinIO, giữ dòng metadata và bundle `vB`. 3. Bật consumer cho `old`; đọc trạng thái cuối và API kết quả. |
| Expected | `old` kết thúc `SYSTEM_ERROR`, `failure_reason=SPEC_UNAVAILABLE`, `score_ratio=NULL`; API kết quả trả `scoreRatio`, `passedCount`, `totalCount` là `null` và không lộ input/expected output/object key. Không gọi chấm bằng `vB`, không sinh kết quả `WRONG_ANSWER` hay `COMPILE_ERROR`; chỉ một lần kết thúc và một sự kiện `SubmissionGraded` cho lượt này. |
| Cleanup | Khôi phục object A từ bản fixture trước khi dọn dữ liệu; bật lại consumer; xoá fixture cô lập |
| Evidence | Metadata hai snapshot, dấu xoá object trong môi trường test, dòng `submissions`, API mục 8, outbox và log vận hành đã che dữ liệu nhạy cảm |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC93-05 — Snapshot A sai checksum sau khi đã nhận bài

| Field | Value |
| :--- | :--- |
| AC / DD source | F2 AC-93, F4 AC-74; F2 BR-11 và DB F2 mục 1.15; F4 BR-JD-03 bước 5, BR-JD-05 bước 5 |
| Kind / priority | Abnormal, P0: phát hiện bundle bị sửa sau công bố |
| Environment | Tích hợp cô lập; có quyền fault injection thay byte object MinIO nhưng giữ `content_sha256` trong DB |
| Preconditions | `old` đã được nhận với `vA`; consumer tạm dừng; bundle `vB` hợp lệ có sẵn |
| Test data | `SRC-EDGE`; thay một byte trong object của `vA` mà không đổi metadata/checksum lưu ở DB |
| Steps | 1. Lưu SHA-256 gốc của A. 2. Fault injection thay một byte của object A. 3. Bật consumer cho `old`; đọc trạng thái cuối và kiểm số lần gọi sandbox. |
| Expected | SHA-256 tính từ object khác `content_sha256`; `old` kết thúc `SYSTEM_ERROR/SPEC_UNAVAILABLE`, `score_ratio=NULL`, không có kết quả chấm testcase do bundle chưa qua xác minh. Không dùng bundle B hoặc testcase hiện hành; API học viên không lộ byte/object key/checksum nội bộ. |
| Cleanup | Khôi phục object A rồi xoá fixture cô lập; bật lại consumer |
| Evidence | SHA-256 trước/sau, dòng snapshot metadata, dòng submission, số lần gọi sandbox, API mục 8 và log vận hành |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC74-01 — Đặc tả bài hỏng sau khi nhận lượt nộp

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-74; F4 BR-JD-03 bước 5, 7, BR-JD-05 bước 5; F2 API mục 58 |
| Kind / priority | Abnormal, P0: lỗi dữ liệu bài không được tính là lỗi mã học viên hoặc lỗi sandbox |
| Environment | Tích hợp F4 với cổng `ProblemGradingSpecPort` được fault injection; F2/DB thật ở đường bình thường |
| Preconditions | A1 đã nộp `SRC-OK` vào `P-SNAP`, submission `PENDING` ghim `vA`; tại lúc nhận `snapshotReady=true`; consumer tạm dừng |
| Test data | Cổng đọc đặc tả trả payload không parse được ở lần đọc của worker; bundle A vẫn nguyên vẹn và có checksum đúng |
| Steps | 1. Xác nhận submission đã commit. 2. Cấu hình cổng đặc tả chỉ ở lần đọc của worker trả payload sai cấu trúc. 3. Bật consumer, chờ trạng thái cuối; đọc API kết quả và log. |
| Expected | Bài kết thúc `SYSTEM_ERROR/SPEC_UNAVAILABLE`; `score_ratio=NULL`; không chấm testcase, không báo `COMPILE_ERROR` hoặc `WRONG_ANSWER`, không gán lỗi cho mã học viên. Log vận hành phân biệt lỗi đặc tả với `SANDBOX_FAULT` nhưng phản hồi học viên không lộ dữ liệu nội bộ. Chỉ một `SubmissionGraded`, không có `SubmissionAccepted`. |
| Cleanup | Gỡ fault injection, bật lại consumer và xoá fixture cô lập |
| Evidence | Dòng submission trước/sau, dấu gọi cổng và sandbox, API mục 8, log vận hành, outbox |
| Design review | Đã review; chưa thực thi |

## Review hai chiều và phụ thuộc thực thi

- F2 AC-93: tách đủ bộ cũ/mới, testcase hiện hành bị xoá, bài bị rút, bundle cũ thiếu, checksum sai. F4 AC-75 được kiểm qua ba ca giữ phiên bản; F4 AC-74 được kiểm qua hai kiểu hỏng bundle và một kiểu hỏng đặc tả.
- Các ca này cần backend và hạ tầng tích hợp; frontend mock không thể chứng minh snapshot bất biến hoặc lỗi `SPEC_UNAVAILABLE`. Không ghi Đạt/Trượt cho tới khi có build, môi trường và bằng chứng chạy.
- Phạm vi tiếp theo của toàn bộ đợt 1: các nhánh F2/F3/F4 chưa có mã ca trong ma trận truy vết, gồm autoscaler F4, chi tiết kết quả hết hạn 90 ngày và các đường lỗi còn lại. Những ca đã thiết kế chưa được thực thi.
