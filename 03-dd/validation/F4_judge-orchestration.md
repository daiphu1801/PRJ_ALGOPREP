# DD Validation — Module `judge-orchestration` (F4)

**Triển khai 2026-11-03, D02m — worker persistence:** năm cổng nội bộ cho
COMPILING/JUDGING, token, kết quả testcase và finalization chung worker/sweep tương lai.
REQUIRES_NEW/READ_COMMITTED, heartbeat guarded trước INSERT/consume; giữ pinned version,
duplicate/terminal-safe, privacy snippet; finalizer row lock, đủ N/first-failure/HALF_UP3,
Graded/Accepted Outbox nguyên tử và STOMP sau commit. Exception cố định không SQL/cause.
Focused verify 338 test đạt, ba mutation mới; Checkstyle0/SpotBugs0. Bootstrap coverage
chỉ suite chọn. Chưa consumer/ack, job lease, đọc cấu hình/snapshot, compile/run orchestration,
relay/sweep; không đóng D02 tổng/D03/D04/B2/B4/B3. Report261103-f4-worker-persistence.
Owner chốt khóa phiên riêng/job, giao dịch ghi ngắn; sweep khóa giao dịch
(DEC-2026-1103-f4-worker-job-lease); phần lease sẽ triển khai ở lát sau.


**Triển khai 2026-11-03, D02k — STOMP progress transport:** GET /ws, CONNECT1.2
JWT RS256/exp và account live; STUDENT chỉ exact /user/queue/submissions; admin exact
/topic/admin/queue cần ADMIN + JUDGE_QUEUE_MONITOR:READ kiểm lại tại SUBSCRIBE và đóng
socket tại exp. Mọi SEND/đích khác trả ERROR generic rồi đóng. Typed payload không có
source/input/output/stderr; seq tất định, lỗi CE/SYSTEM_ERROR có score/count null.
SubmissionProgressPort có adapter thật, afterCommit/rollback-safe/best-effort WARN.
Broker giữ thứ tự gửi; bearer header được bỏ trước validation/logging. Chỉ transport:
caller trusted phải lấy owner từ submissions; chưa nối HTTP submit/worker, progress REST,
FE recovery hoặc QUEUE_BATCH5s. Report `261103-f4-stomp-progress`.

**Triển khai 2026-11-03, D02i — admission kernel:** có trusted internal
RecordPendingSubmissionUseCase; caller tương lai xác thực STUDENT/lấy userId từ JWT.
Kiểm đầu vào/source UTF-8/file, published/approved/snapshot, enabled/wrapper; ghim phiên bản
testcase vào PENDING. Khóa advisory theo user/problem, đếm cửa sổ >1h rồi INSERT cùng
giao dịch riêng READ COMMITTED; đọc đồng hồ DB sau khóa. Quota riêng hoặc property mặc
định60, mọi status tính lượt. Program/command toString không in code/file; lỗi DB kể cả
commit bị chặn ở ngoài TransactionTemplate, không kèm SQL/cause. Chưa HTTP, queue confirms,
failure finalization/Outbox/STOMP hoặc worker. Report `261103-1105-f4-pending-admission-core`.


**D02e review fix:** owner chốt debug wrapper ≤4096 byte UTF-8 raw, độc lập resultLimit.
Kiểm trước P4 khi engine OK; không dùng độ dài console đã normalize/cắt snippet. Chọn last
marker theo dòng CR/LF/CRLF, chỉ bỏ LF đầu trusted frame; bytes CR/LF của learner vẫn tính.
Vượt trần → RE và không actual/debug/stderr. StdIO và missing-marker semantics không đổi.

**Checkpoint component D02e 2026-11-01:** Limits nội bộ nhận số nguyên dương (thời gian ms,
bộ nhớ byte, output byte, procLimit ≤60), kiểm tràn trước đổi ns hoặc envelope collector.
Đây là giới hạn đã tính do caller cấp, không thay validation cấu hình effective 100–30000 ms/
64–1024 MB tại API admin hay reader DB. Component chưa đọc admin settings/visibility hiện hành.

> Chỉ ghi quy tắc kiểm mà BD chưa tự suy ra hoặc cần chốt thành mã lỗi cụ thể: trường, quy tắc, mã lỗi, **lớp** thực thi. Không lặp lại thông điệp giao diện đã có ở Sheet 9 các BD màn (`02-bd/screens/**`); file này là bản cố định **ở máy chủ** của các kiểm tra đó.
> Nguồn: `02-bd/database/F4_judge-orchestration.md` (kiểu cột, CHECK), Sheet 9 của `USR0102`, `USR0201`, `USR0202`, `USR0501`, `INS0206`, `ADM0401`, `ADM0404`, `ADM0501`. Hợp đồng endpoint ở `03-dd/api/F4_judge-orchestration.md`; mã lỗi tổng hợp ở mục 27 của file đó.
> Bounded Context: F4 `judge-orchestration` · Cập nhật: 2026-10-23.

## 1. Quy ước lớp kiểm

Bảng 1 định nghĩa bốn lớp mà cột "Lớp" ở các bảng sau dùng.

**Bảng 1: Các lớp kiểm**

| Lớp | Vị trí | Ví dụ |
| :--- | :--- | :--- |
| Presentation | controller, bộ chặn STOMP, `@Valid`; lỗi định dạng trả `400` mã `JDG-Vnn` | độ dài mã, kiểu enum |
| Application | use case; vi phạm quy tắc nghiệp vụ trả `404`, `409`, `422`, `429` mã `JDG-nnn` | ngôn ngữ đang tắt, vượt số lượt nộp mỗi giờ |
| Domain | quy tắc bất biến của aggregate | không ghi đè trạng thái cuối |
| DB | `CHECK`, unique, khoá | `score_ratio` kiểu `DECIMAL(4,3)`, unique `(submission_id, testcase_id)` |

Quy tắc chung: **kiểm ở máy chủ là kiểm thật**; kiểm ở giao diện chỉ để báo sớm (anti-drift quy tắc 3 của `dd-generation`: schema zod phía giao diện phải khớp file này). Mọi chuỗi người dùng nhập vào là **dữ liệu**, không bao giờ diễn giải như chỉ thị (từ khoá tìm kiếm ở `ADM0404`, mã nguồn).

## 2. Nộp bài và chạy thử (endpoint #2, #3)

**Bảng 2: Trường của `CreateSubmission` và `RunSampleTestcases`**

| Trường | Quy tắc | Mã lỗi | HTTP | Lớp | Nguồn |
| :--- | :--- | :--- | :-: | :--- | :--- |
| `problemId` (path) | UUID hợp lệ; bài `PUBLISHED`, `deleted = false`; sai định dạng hoặc không mở được trả như không tồn tại | `JDG-101` | 404 | Application | `USR0102` Sheet 9 NO 2 |
| `language` | một trong `JAVA`, `CPP`, `PYTHON`; không phân biệt hoa thường thì chuẩn hoá về chữ hoa trước khi so | `JDG-V03` | 400 | Presentation | `database/F4_judge-orchestration.md:22` |
| `language` (đang bật) | `language_configs.enabled = true` | `JDG-102` | 422 | Application | `ADM0501` Sheet 9 NO 6, NO 14 |
| `submissionModel` | một trong `FUNCTION_WRAPPER`, `STANDARD_IO` | `JDG-V04` | 400 | Presentation | `database/F4_judge-orchestration.md:23` |
| `submissionModel` (hợp lệ cho bài) | `FUNCTION_WRAPPER` chỉ khi bài có `functionWrapperSupported = true` | `JDG-103` | 422 | Application | F3-13, `USR0102` Sheet 9 NO 9 |
| `sourceCode` (không rỗng) | sau khi bỏ khoảng trắng đầu cuối phải còn ít nhất 1 ký tự | `JDG-V01` | 400 | Presentation | `USR0102` Sheet 9 NO 7 |
| `sourceCode` (kích thước) | `octet_length(sourceCode UTF-8) <= 65536`; **đo bằng byte, không bằng số ký tự**; đúng 65.536 byte được nhận, 65.537 byte bị từ chối | `JDG-V02` | 400 | Presentation, và `CHECK (octet_length(source_code) <= 65536)` ở DB (phòng thủ) | `database/F4_judge-orchestration.md:24`; `USR0102` Q7 |
| `sourceCode` (nội dung) | không cắt, không chuẩn hoá xuống dòng, không lọc ký tự; lưu nguyên văn; mã luôn đi vào `harness` như **dữ liệu** | không có | | | `README.md` ràng buộc F5 và F3 |
| `sourceInputMethod` | một trong `EDITOR`, `FILE_UPLOAD` (chỉ cho nộp bài; run-sample bỏ qua) | `JDG-V05` | 400 | Presentation | `database/F4_judge-orchestration.md:25` |
| `fileName` | bắt buộc khi `FILE_UPLOAD`; đuôi (không phân biệt hoa thường) phải là `.java` cho `JAVA`, `.cpp` cho `CPP`, `.py` cho `PYTHON`; tối đa 255 ký tự `[SoT: Suy luận]`; **chỉ dùng để kiểm đuôi, không lưu** | `JDG-V06` | 400 | Presentation | `USR0102` Sheet 9 NO 5; trường này là đề xuất (file API Q9) |
| Testcase của bài | số testcase đã duyệt lớn hơn 0 (nộp bài); số Sample đã duyệt lớn hơn 0 (chạy thử) | `JDG-104`, `JDG-105` | 422 | Application | |
| Giới hạn lượt nộp mỗi giờ | số lượt nộp của cặp (người, bài) trong **60 phút gần nhất** `< giới hạn`; giới hạn = `max_submissions_per_hour` của bài hoặc 60 khi `NULL`; đo ở `now()` của PostgreSQL | `JDG-106` | 429 | Application, dưới khoá tư vấn theo cặp | `USR0102` Sheet 9 NO 10; `database/F2_problem-bank.md:295` |
| Suất chạy thử đồng thời | semaphore toàn cục `judge.run-sample.max-concurrent` = 8 | `JDG-502` | 429 | Application | file API Q5 |
| Lượt chạy thử theo người | Redis sorted set cho từng `userId` từ token, tối đa 60 lượt được nhận trong 60 phút trượt; `retryAfterSeconds` từ mốc cũ nhất | `JDG-107` | 429 | Application | owner 2026-10-26, file API Q5; Redis lỗi thì chỉ áp trần đồng thời |
| Phạm vi dữ liệu | `userId` luôn là người trong token; không có trường nào khác chọn người | | | Application | `USR0102` Sheet 9 NO 11 |

## 3. Danh sách của học viên (endpoint #4, #7, #8, #23)

**Bảng 3: Tham số danh sách học viên**

| Trường | Quy tắc | Mã lỗi | Lớp | Nguồn |
| :--- | :--- | :--- | :--- | :--- |
| `q` | bỏ khoảng trắng hai đầu; cắt ở 100 ký tự (không báo lỗi) | không có | Presentation | `USR0202` Sheet 9 NO 3 |
| `verdict` | `ALL`, `ACCEPTED`, `WRONG_ANSWER`, `TIME_LIMIT_EXCEEDED`, `COMPILE_ERROR`, `RUNTIME_ERROR`; giá trị lạ **đưa về `ALL`**, không lỗi | không có | Presentation | `USR0202` Sheet 9 NO 4 |
| `language` | `ALL`, `JAVA`, `CPP`, `PYTHON`; giá trị lạ về `ALL` | không có | Presentation | `USR0202` Sheet 9 NO 4 |
| `page` | số nguyên từ 1; không hợp lệ về 1; lớn hơn tổng số trang về trang cuối | không có | Presentation | `INS0206` Sheet 9 NO 7 (cùng quy tắc) |
| `limit` (endpoint #4, #8, #23) | số nguyên: #4 từ 1 đến 10 (mặc định 10); #8 từ 1 đến 20 (mặc định 3); #23 từ 1 đến 20 (mặc định 10) `[SoT: Suy luận]` | `JDG-V14` | Presentation | `USR0102` dòng 305-308; `USR0101` Q4 |

## 4. Truy xuất bài nộp (endpoint #5, #6, #11)

Owner chốt D02q 2026-11-04: lỗi hạ tầng đọc trả `503 JDG-506` (decision
`DEC-2026-1104-f4-submission-read-unavailable`). Phạm vi lớp/tìm kiếm bắt buộc lỗi thì fail-closed;
summary tùy chọn lỗi trả null, visibility lỗi coi Hidden. Không dùng lỗi của máy chấm cho nhánh này.

| Trường | Quy tắc | Mã lỗi | HTTP | Lớp | Nguồn |
| :--- | :--- | :--- | :-: | :--- | :--- |
| `submissionId` | UUID; sai định dạng, không có, hoặc của người khác: **cùng một mã và một thông điệp** | `JDG-110` | 404 | Application | `USR0201` Sheet 9 NO 2 |
| Quyền sở hữu (A1) | `submissions.user_id = tokenUserId` đặt trong điều kiện truy vấn, không lọc sau | `JDG-110` | 404 | Application | `USR0201` Sheet 9 NO 2 |
| Quyền A2 | `CLASS_MANAGEMENT:READ`; thiếu thì 403; có nhưng ngoài phạm vi lớp thì 404 (không phải 403) | `JDG-301`, `JDG-110` | 403, 404 | Application | `USR0201` A2-1 |
| `studentId`, `classId`, `from` ở URL chi tiết A2 | **bỏ qua** hoàn toàn khi quyết định quyền và dữ liệu | không có | | Application | `USR0201` A2-2 |
| Dữ liệu testcase ẩn và mã khung | không bao giờ có trong phản hồi (BR-JD-11): `actualOutput`, `testcaseId` chỉ khi testcase `SAMPLE` (hai lớp độc lập); `stderrSnippet` còn đòi `submissionModel = STANDARD_IO` và cờ admin bật. Expected output không bao giờ trả; `FUNCTION_WRAPPER` không lưu/trả stderr | không có | | Application, tầng dựng phản hồi | `USR0102` Sheet 9 NO 3; owner 2026-10-25 |
| Quá 90 ngày | `submitted_at < now() - 90 ngày` thì `testcaseDetail.available = false`, `RETENTION_EXPIRED`, `items = []` | không có | | Application | `database/F4_judge-orchestration.md:68-72` |
| Kiểm thử bằng token sai | token hết hạn hoặc thiếu: `401` mã chung | | 401 | Presentation | |

## 5. Danh sách giảng viên (endpoint #10, #12)

| Trường | Quy tắc | Mã lỗi | HTTP | Lớp | Nguồn |
| :--- | :--- | :--- | :-: | :--- | :--- |
| Quyền | Function `CLASS_MANAGEMENT`, hành động `READ` | `JDG-301` | 403 | Application | `INS0206` Sheet 9 NO 1 |
| `classId` | UUID hợp lệ và là lớp có `instructor_id` bằng người gọi; sai định dạng **xử như lớp không tồn tại**; không phải lớp của mình trả `404` chứ không trả danh sách rỗng | `JDG-111` | 404 | Application | `INS0206` Sheet 9 NO 2, NO 5 |
| `studentId` (endpoint #12) | phải có dòng ghi danh trong lớp (kể cả đã rời) | `JDG-112` | 404 | Application | |
| `result` | `all`, `accepted`, `failed`; lạ về `all` | không có | | Presentation | `INS0206` Sheet 9 NO 6 |
| `q` | bỏ khoảng trắng hai đầu; cắt ở 100 ký tự | không có | | Presentation | `INS0206` Sheet 9 NO 7 |
| `page` | như bảng 3; kích thước trang 20 cố định, **không có tham số `pageSize` từ phía gọi** | không có | | Presentation | `INS0206` dòng 225 |
| Cửa sổ thuộc lớp | `submitted_at >= joined_at` và (`left_at IS NULL` hoặc `submitted_at <= left_at`) | không có | | Application, trong truy vấn | `INS0206` Sheet 9 NO 3 |
| Chỉ bài đã giao | bài thuộc `class_assignments` của lớp, kể cả gỡ mềm; loại bài tự luyện | không có | | Application, trong truy vấn | `INS0206` Sheet 9 NO 8 |
| Một dòng mỗi bài nộp | lớp có `assigned_at` mới nhất không sau `submitted_at` | không có | | Application | `INS0206` Sheet 9 NO 9 |
| Phân loại `failed` | trạng thái cuối khác `ACCEPTED` và khác `SYSTEM_ERROR`; bài chưa cuối chỉ ở `all` | không có | | Application | `INS0206` Sheet 9 NO 10 |
| Tỉ lệ khi chưa chấm xong | `passedCount`, `totalCount` trả `null` (không phải `0/0`) khi `total_testcase_count IS NULL` hoặc `score_ratio IS NULL` | không có | | Application | `INS0206` Sheet 9 NO 11 |
| DTO danh sách | **không có** `source_code`, `compile_error_message`, nội dung testcase, dòng `submission_testcase_results` | không có | | Application, tầng dựng DTO | `INS0206` Sheet 9 NO 4 |

## 6. Số liệu nộp bài (endpoint #9 và hai cổng)

| Trường | Quy tắc | Mã lỗi | HTTP | Lớp | Nguồn |
| :--- | :--- | :--- | :-: | :--- | :--- |
| `range` | đúng `7d`, `30d`, `all`; giá trị khác bị từ chối (không đưa về mặc định), màn giữ lựa chọn cũ | `JDG-V07` | 400 | Presentation | `USR0501` Sheet 9 NO 3 |
| `topicIds` | tối đa 50 UUID hợp lệ `[SoT: Suy luận]` | `JDG-V15` | 400 | Presentation | |
| Danh tính (A1) | không có tham số danh tính; tham số `userId` thừa trên URL bị bỏ qua | không có | | Presentation, chữ ký cổng | `DEC-2026-0927-submission-metrics-two-ports` |
| Múi giờ ngày | UTC+7 cố định | không có | | Application | `DEC-2026-1018-usr-open-questions-phase2-5` |
| `weekCount` (cổng giảng viên) | số nguyên từ 1 đến 12 `[SoT: Suy luận]`; ngoài khoảng thì cổng ném lỗi lập trình (không phải lỗi người dùng) | không có | | Application | |
| `studentIds`, `problemIds` | UUID; `studentIds` tối đa 500, `problemIds` tối đa 500 `[SoT: Suy luận]`, để truy vấn và khoá cache không phình | không có | | Application | |

## 7. Vận hành hàng đợi (endpoint #13 đến #19)

| Trường | Quy tắc | Mã lỗi | HTTP | Lớp | Nguồn |
| :--- | :--- | :--- | :-: | :--- | :--- |
| Quyền xem | `JUDGE_QUEUE_MONITOR:READ` | `JDG-301` | 403 | Application | `ADM0401` Sheet 9 NO 1 |
| Quyền ghi (#16, #17, #19) | vai trò `ADMIN` kiểm lại ở **mỗi lệnh**; không thêm Function riêng | `JDG-301` | 403 | Application | `ADM0401` Sheet 9 NO 2, NO 9 |
| `paused`, `expectedPaused` (#16) | cả hai bắt buộc, kiểu boolean | `JDG-V16` | 400 | Presentation | |
| `enabled`, `expectedEnabled` (#17) | cả hai bắt buộc, kiểu boolean | `JDG-V16` | 400 | Presentation | |
| Trạng thái mong đợi | `expected*` khác trạng thái thật dưới khoá `FOR UPDATE` thì từ chối | `JDG-201` | 409 | Application | `ADM0401` Sheet 9 NO 5 |
| `autoscaleJobThreshold`, `jobsPerPage`, `errorRetentionHours`, `latencyCapSeconds` (#19) | **số nguyên từ 1 trở lên**; số thập phân, chuỗi, rỗng, số nhỏ hơn 1 đều bị từ chối; **không có giá trị tối đa nghiệp vụ**, chỉ bị chặn bởi kiểu `INT` (2.147.483.647) | `JDG-V09` | 400 | Presentation; và `CHECK > 0` ở DB | `ADM0401` Sheet 9 NO 8; `database/F4_judge-orchestration.md:150-153` |
| `tab` (#14) | `waiting`, `failed`, `all`; lạ về `waiting` | không có | | Presentation | |
| `page` (#14) | như bảng 3 | không có | | Presentation | |
| Kích thước trang (#14) | `cluster_settings.jobs_per_page`, không do phía gọi quyết định | không có | | Application | `ADM0401` Khu vực F NO 11 |

## 8. Sự kiện hạ tầng (endpoint #20)

| Trường | Quy tắc | Mã lỗi | HTTP | Lớp | Nguồn |
| :--- | :--- | :--- | :-: | :--- | :--- |
| `q` | tối đa 100 ký tự (dài hơn thì cắt); xử như **dữ liệu**; ký tự đặc biệt của `LIKE` được thoát (`%`, `_`, `\`) | không có | | Presentation, Application | `ADM0404` Sheet 9 NO 3 |
| `level` | `error`, `warn`, `info` không phân biệt hoa thường; giá trị khác **bị bỏ qua** | không có | | Presentation | `ADM0404` Q4 |
| `service` | `go-judge`, `outbox`; giá trị khác bị bỏ qua | không có | | Presentation | |
| `from`, `to` | định dạng `yyyy-MM-dd`; **`from` không được sau `to`**; khoảng đóng ở cả hai đầu theo UTC+7 | `JDG-V13` | 400 | Presentation | `ADM0404` Sheet 9 NO 2 |
| `cursor` | chuỗi do máy chủ cấp; không giải mã được thì coi như trang đầu | không có | | Application | |
| Quyền | `JUDGE_QUEUE_MONITOR:READ` | `JDG-301` | 403 | Application | `ADM0404` Sheet 9 NO 1 |
| Cửa sổ dữ liệu | 90 ngày gần nhất | không có | | Application | `ADM0404` Q3 |

## 9. Cấu hình ngôn ngữ và sandbox (endpoint #21, #22)

**Bảng 4: Trường của `UpdateLanguageConfigs`**

| Trường | Quy tắc | Mã lỗi | HTTP | Lớp | Nguồn |
| :--- | :--- | :--- | :-: | :--- | :--- |
| Quyền | `LANGUAGE_CONFIG:READ` cho xem; `LANGUAGE_CONFIG:UPDATE` và vai trò `ADMIN` cho lưu; `CREATE`, `DELETE` không có tác dụng | `JDG-301` | 403 | Application | `ADM0501` Sheet 9 NO 1 |
| `languages` | đúng 3 phần tử, đủ `JAVA`, `CPP`, `PYTHON`; không thêm, không xoá dòng | `JDG-V10` | 400 | Presentation | `database/F4_judge-orchestration.md:107-108` |
| `timeLimitMultiplier`, `memoryLimitMultiplier` | số thập phân, **tối đa 2 chữ số thập phân**, trong khoảng **0,10 đến 9,99**; bỏ trống, chữ, âm, 0 đều không hợp lệ; ví dụ 0,09 và 10,00 bị từ chối | `JDG-V10` | 400 | Presentation; DB `DECIMAL(3,2)` | `ADM0501` Sheet 9 NO 2, NO 3, NO 10 |
| `enabled` | boolean; sau thay đổi phải còn **ít nhất một** ngôn ngữ `true` | `JDG-203` | 422 | Application | `ADM0501` Sheet 9 NO 6 |
| `gradingDefaults.timeLimitMs`, `memoryLimitMb`, `maxOutputSizeKb`, `compileTimeoutSeconds` | số nguyên từ 1; ô rỗng, chữ, số thập phân đều bị từ chối | `JDG-V11` | 400 | Presentation; DB `CHECK > 0` | `ADM0501` Sheet 9 NO 13 |
| Giới hạn thời gian hiệu dụng | với **mỗi** ngôn ngữ: `ROUND(timeLimitMs * timeLimitMultiplier)` trong 100 đến 30.000 ms; kiểm cả khi chỉ `timeLimitMs` mặc định đổi (hai thừa số đổi độc lập) | `JDG-204` | 422 | Application | `ADM0501` Sheet 9 NO 4 |
| Giới hạn bộ nhớ hiệu dụng | với **mỗi** ngôn ngữ: `ROUND(memoryLimitMb * memoryLimitMultiplier)` trong 64 đến 1.024 MB | `JDG-205` | 422 | Application | `ADM0501` Sheet 9 NO 5 |
| `sandbox.*` | ba boolean; **không có** trường `maxChildProcesses` (trần 60 là hằng số `application.yml`); gửi thừa trường thì bỏ qua | không có | | Presentation | `database/F4_judge-orchestration.md:193` |
| `generatorSandbox.maxRuntimeSeconds` | số nguyên 1 đến 300 | `JDG-V12` | 400 | Presentation; DB `CHECK` | `ADM0501` Sheet 9 NO 9; `database/F4_judge-orchestration.md:164` |
| `generatorSandbox.maxOutputMb` | số nguyên 1 đến 1.024 | `JDG-V12` | 400 | Presentation; DB `CHECK` | `database/F4_judge-orchestration.md:165` |
| `*.updatedAt` | phải bằng `updated_at` hiện tại của từng dòng; lệch thì từ chối, **không ghi đè** | `JDG-202` | 409 | Application, dưới khoá `FOR UPDATE` | `ADM0501` Sheet 9 NO 7 |
| Tắt một ngôn ngữ | **không** đổi trạng thái hay huỷ job đang chờ của ngôn ngữ đó | không có | | Application | `ADM0501` Sheet 9 NO 14 |

Thứ tự kiểm của một lần lưu: định dạng và khoảng từng trường (`JDG-V10`..`V12`), rồi quy tắc liên trường (`JDG-203`, `JDG-204`, `JDG-205`), rồi xung đột phiên bản (`JDG-202`) dưới khoá. Một lỗi ở bất kỳ bước nào thì **không ghi gì** và không có dòng nhật ký.

## 9A. Danh sách ngôn ngữ đang bật (endpoint #24)

`GET /languages/enabled` không có tham số đầu vào. Chỉ cần JWT hợp lệ của STUDENT hoặc INSTRUCTOR. Bộ lọc `enabled = true` áp tại máy chủ; kết quả không chứa dòng đã tắt hoặc các giá trị cấu hình quản trị. Không dùng `LANGUAGE_CONFIG:READ` vì đây là danh sách cho chọn ngôn ngữ nộp bài (`DEC-2026-1024-dd-student-language-contracts`).

## 10. Kênh thời gian thực (STOMP)

| Kiểm | Quy tắc | Phản hồi | Lớp |
| :--- | :--- | :--- | :--- |
| `CONNECT` | header `Authorization: Bearer` hợp lệ, chưa hết hạn | khung `ERROR` generic rồi đóng | Presentation (`ChannelInterceptor`) |
| `SUBSCRIBE` học viên | chỉ `/user/queue/submissions`, chỉ khi `baseCategory = STUDENT` | từ chối im lặng (không nêu `submissionId` có tồn tại hay không) | Presentation |
| `SUBSCRIBE` quản trị | `/topic/admin/queue` cần `ADMIN` và `JUDGE_QUEUE_MONITOR:READ` | từ chối im lặng | Presentation |
| `SEND` từ client | không có đích ứng dụng nào | từ chối | Presentation |
| Nội dung bản tin | chỉ `order`, `verdict` và số đếm cho testcase; không runtime, output, stderr, input | không có | Application, bộ dựng bản tin |
| Quyền sở hữu khi đẩy | `convertAndSendToUser(chủ bài nộp)`; không đẩy bản tin của bài nộp tới người khác | không có | Application |

## 11. Hằng số cấu hình `application.yml`

Bảng 5 gom các hằng số mà DD chốt hoặc đề xuất. Theo tiêu chí ponytail bậc 5 chúng là hằng số, **không** thêm ô cấu hình admin mới (`DEC-2026-1001-admin-configurable-settings` chỉ giữ các ô đã có).

**Bảng 5: Hằng số của module**

| Khoá `application.yml` | Giá trị | Nguồn |
| :--- | :--- | :--- |
| `judge.submission.max-source-bytes` | 65536 | `database/F4_judge-orchestration.md:24`, `USR0102` Q7 |
| `judge.submission.default-hourly-limit` | 60 | `database/F2_problem-bank.md:295` |
| `judge.run-sample.max-per-user-window` / `judge.run-sample.window-minutes` | 60 / 60 | Owner 2026-10-26, DD F4 Q5, Redis sliding window |
| `judge.run-sample.max-concurrent` | 8 | DD F4 Q5 |
| `judge.sweep.stale-after` | 5 phút, `judge.sweep.interval` 60 giây | `03-dd/jobs/F4_judge-orchestration.md` mục 1 |
| `judge.outbox.relay-interval` | 2 giây; cỡ lô 100 | `03-dd/jobs/F4_judge-orchestration.md` mục 2 |
| `judge.infra.outbox-warn-attempts` | 3 | `database/F4_judge-orchestration.md:243` |
| `judge.infra.outbox-error-attempts` | 5 | `database/F4_judge-orchestration.md:243` |
| `judge.infra.event-retention-days` | 90 | `ADM0404` Q3, Q5 |
| `judge.results.retention-days` | 90 | `database/F4_judge-orchestration.md:68-72` |
| `judge.stderr.max-chars`, `judge.output-snippet.max-chars` | 4096 | `database/F4_judge-orchestration.md:63-64` |
| `judge.compile-error.max-chars` | 10000 | `USR0201` Sheet 5 khu vực E NO 4 (độ dài hiển thị 10.000) |
| `judge.beats.min-sample` | 10 | `USR0201` Sheet 9 NO 6 |
| `judge.sandbox.child-process-cap` | 60 | `DEC-2026-1014-sandbox-child-process-cap-constant` |
| `judge.autoscale.max-workers` | 8 | `ADM0401` Q8 |
| `judge.engine.connect-timeout` | 2 giây `[SoT: Suy luận]` | file API mục 3 |
| `judge.engine.run-call-slack` | 5 giây (cộng vào thời gian hiệu lực) `[SoT: Suy luận]` | file API mục 3 |
| `judge.engine.compile-call-slack` | 5 giây `[SoT: Suy luận]` | file API mục 3 |
| `judge.engine.retries` | 2 lần thử lại; chờ 500 ms rồi 1000 ms `[SoT: Suy luận]` | file API mục 3 |
| `judge.run-sample.deadline` | 60 giây `[SoT: Suy luận]` | file API mục 3 |
| `judge.run-sample.max-concurrent` | 8 `[SoT: Suy luận]` | file API mục 5 |
| `judge.worker.concurrency` | 4 `[SoT: Suy luận]`; `prefetch = 1` | BR-JD-03 |
| `judge.queue-gate.poll-interval` | 3 giây `[SoT: Suy luận]` | BR-JD-08 |
| `judge.admin-stream.interval` | 5 giây | `ADM0401` Q1 |
| `judge.admin-stream.rest-fallback` | 15 giây (phía giao diện) | `ADM0401` Q1 |
| `judge.class-metrics.cache-ttl` | 15 phút | `DEC-2026-1014-instructor-batch2-weekly-rules` |
| `judge.page-size` | 20 | `INS0206` dòng 225 |
| `judge.timezone-offset` | `+07:00` | `DEC-2026-1018-usr-open-questions-phase2-5` |

## 12. Quy tắc bảo mật xuyên suốt (nhắc lại để thành mục kiểm)

1. **Không lộ testcase ẩn**: không endpoint, bản tin hay cổng nào trả input, expected output, output thực tế hay diff của testcase `HIDDEN` (kiểm ở tầng dựng phản hồi). Test bắt buộc ở mọi endpoint có `testcaseDetail`.
2. **Mã nguồn là dữ liệu**: mã học viên đi qua `harness` và `JudgeExecutionPort` như tham số, không bao giờ nối vào chuỗi lệnh; message RabbitMQ không chứa mã; mã không đi vào bất kỳ lời nhắc AI nào ở module này (F5 là module khác, nhận mã qua `submissionId`, `USR0102` Sheet 9 NO 14).
3. **IDOR**: bài nộp của người khác trả 404 giống bài không tồn tại, ở endpoint học viên (#5, #6) và ngoài phạm vi lớp ở endpoint giảng viên (#11).
4. **Không có thao tác ghi nào lên bài nộp ngoài tạo mới và các chuyển trạng thái của worker và sweep**; không xoá, không ẩn (`USR0202` Q3).
5. **Lệnh ghi quản trị** luôn kiểm lại vai trò `ADMIN` ở máy chủ (`02-bd/security/F4_judge-orchestration.md:73-76`) và luôn có dòng nhật ký cùng giao dịch.
6. **Suy giảm êm**: không kiểm nào ở file này phụ thuộc đường AI; F5 hoặc F6 chết không ảnh hưởng việc kiểm và chấm của F1 đến F4 (`CLAUDE.md` mục Rules).

## Tham chiếu

`03-dd/api/F4_judge-orchestration.md` (hợp đồng, mã lỗi), `03-dd/logic/F4_judge-orchestration.md` (BR-JD-nn), `03-dd/jobs/F4_judge-orchestration.md`, `02-bd/database/F4_judge-orchestration.md` (kiểu cột, CHECK), `02-bd/security/F4_judge-orchestration.md`, Sheet 9 của các BD màn đã nêu.


## Ghi nhận triển khai D02n — 2026-11-03

D02n: broker body tối đa 128 byte, đúng một trường submissionId dạng UUID đầy đủ; chặn trường thừa/trùng và JSON nối đuôi. Worker xác minh problem/version snapshot, bundle không rỗng, ID và position duy nhất, position dương, UTF-8 hợp lệ. Không nhận verdict/testcase/source từ queue. Network-enabled chưa được engine topology hỗ trợ: lỗi hệ thống rõ ràng theo quyết định owner, không lờ cấu hình. Limits.processes=0 chỉ là sentinel nội bộ để tắt chính sách 60, không gửi nguyên số 0 tới go-judge.


## Triển khai D02r — 2026-11-04: Run Code/Sample và execution limits

**D02r — Task 8: Run Code/Sample và execution limits, 2026-11-04:** hai API backend dùng F2/F3/sandbox thật; 14 unit mới + 18 integration mới đạt, cùng 29 test hợp đồng trực tiếp/kiến trúc (61 test liên quan); 8/8 mutation bị bắt. Spotless/Checkstyle/SpotBugs đạt; LINE của từng file mới được đo ≥80%. Chỉ test lát và phụ thuộc trực tiếp theo owner; full regression chờ mốc clear. FE/browser, network-enabled và production nhiều instance còn hoãn. [Report](F:/prj_algoprep/06-plan/reports/261104-f4-run-sample/report.md).

API #1/#2 dùng dữ liệu F2/F3/cấu hình/sandbox thật; STUDENT/JWT, chỉ Sample approved từ SQL,
không ghi submission/progress/outbox. Redis Lua 60/60 phút/user, fail-open chỉ phần quota;
semaphore JVM 8 theo quyết định single-instance. JDG-107 có retryAfterSeconds, JDG-502 busy
hoàn mốc, JDG-501 hoàn mốc hạ tầng, compile error tính lượt. Deadline 60 giây và cancellation
giữ slot đến khi task thực sự dừng. Output/stderr qua kernel privacy hiện có, HALF_UP giới hạn.
[SoT: Suy luận — chi tiết lifecycle]: Future/virtual thread và callback hoàn quota một lần,
trạng thái hủy tách interrupt để xử lý race; không đổi hợp đồng/schema/auth. GET limits giữ
hợp đồng lỗi phụ thuộc hiện có của §4.5, không mở rộng mã JDG-506 từ D02q. Source/gates/log/hash
có trong report. Chỉ test phần mới và hợp đồng trực tiếp theo owner; coverage BUNDLE/full
regression, FE/browser và nghiệm thu module tổng chưa thực hiện ở lát này.


**D02v — 2026-11-04:** F4 B2 cổng thống kê lớp đã xong: đúng cửa sổ ghi danh/giao/gỡ, tuần đủ UTC+7, best theo lần giao, cache15 phút/fallback. 24 test liên quan đạt (16 mới), 4/4 mutation bị bắt; 124/124 dòng logic mới, static gates đạt. Không sửa F1/F2 hoặc chạy full regression. B3 đã chốt 7 khối, chưa triển khai. [Report](F:/prj_algoprep/06-plan/reports/261104-1540-f4-class-metrics/report.md).


D02v: ClassMetricsScope là đầu vào trusted trong JVM, immutable; weekCount 1..12, studentIds duy nhất, timestamps bắt buộc và end không trước start. Không tự đặt mã HTTP/JDG cho misuse cổng nội bộ. Không mở rộng JDG-506.


**D02w — 2026-11-04:** B3 backend F4 có đủ7 cổng thống kê admin theo owner; SQL đúng UTC+7/cửa sổ/trạng thái/scope bài. 23 test liên quan đạt (15 mới), 4/4 mutation, 191/191 dòng logic mới và static gates đạt. Chưa nối F1 HTTP/cache/catalogue hoặc FE; không chạy full regression/merge. [Report](F:/prj_algoprep/06-plan/reports/261104-1604-f4-admin-stats/report.md).


**D02x — 2026-11-04:** F4 GET metrics cá nhân và P5 topic đã xong; SQL/principal thật, range/streak/ratio/đọc topic lịch sử, không callback F2 từ P5. Owner chốt503 JDG-507. 29 test liên quan duy nhất đạt (21 mới),5/5 mutation, static gates đạt; coverage7 file logic không đổi115/115 dòng từ unit đầu. Không full regression/merge. [Report](F:/prj_algoprep/06-plan/reports/261104-1653-f4-my-metrics/report.md).


**D02y — 2026-11-05:** F4 P6/P7 backend xong: current-members/class attribution trước tab, cửa sổ giao/ghi danh; P7 principal STUDENT và trạng thái từng lần giao, không callback F2. 28 test liên quan đạt (16 mới),7/7 mutation,80/80 dòng logic mới; static gates đạt. Consumer F2/FE chưa nối, chưa full regression/merge. [Report](F:/prj_algoprep/06-plan/reports/261105-1020-f4-assignment-metrics/report.md).


P6/P7 là cổng SELECT nội bộ mới, không HTTP/quyền/schema mới. P6 kiểm ownership F1 và chỉ học viên còn ghi danh, rank mọi lớp trước tab; P7 lấy principal STUDENT, F2 kiểm membership đầu vào. Không callback F2, không source/testcase, không ghi dữ liệu; lỗi dependency cố định/no cause, caller F2 map theo DD. Các bound và kiểu cụ thể ghi tại DD API §26.6/26.7 và report D02y.


**D02z — 2026-11-05:** F4 P3 provider đủ generator limits/enabled languages/multipliers và defaults có sẵn; P4 snapshot7 trường chỉAC, lỗi missing/notAccepted riêng, no testcase/source trong log. 22 test liên quan đạt (14 mới),5/5 mutation,37/37 dòng logic mới; static gates đạt, Spotless judge/bootstrap apply+check đầy đủ. Consumer mới F2/F5/FE chưa nối; chưa full regression/merge. [Report](F:/prj_algoprep/06-plan/reports/261105-1045-f4-config-snapshot-reads/report.md).

P3 mới đọc DB mỗi lần, không fallback/cache ở F4; unknown language lỗi cổng. P4 chỉAccepted, snapshot7 trường và source redacted trong toString; không owner guard theo DD, F5 phải kiểm userId và map lỗi. Không testcase/quyền/schema/HTTP mới. Chi tiết chữ ký và test tại report D02z và DD API §26.3/26.4.


**D02aa — 2026-11-05:** API F4#8/#23/#24 xong: latest-own/withdrawn trước limit, Accepted history và enabled languages đúng thứ tự/quyền.30 test liên quan đạt (18 mới),6/6 mutation,83/83 dòng logic mới; static gates/Spotless judge+bootstrap đạt. F4-2 backend đóng; còn admin config/queue và nghiệm thu BE cuối, FE riêng. Chưa full regression/merge. [Report](F:/prj_algoprep/06-plan/reports/261105-1110-f4-learner-list-apis/report.md).

#8/#23 không nhận userId trong chữ ký usecase; STUDENT current principal và SQL own-only. #8 latest trước lọc status, metadata withdrawn trước limit, keyset32; F2 lỗi optional thìproblem=null. #23 giữAC lịch sử, metadata4 trường, khôngsource/testcase. #24 roleSTUDENT/INSTRUCTOR/P3 currentDB, thứ tựPYTHON/JAVA/CPP, khôngadminconfig. 400JDG-V14 cho limit1..20;503chung khôngthêmmã, khôngmởrộng506/507. Không write/audit/cache, không sửa schema. SoT chi tiết và giới hạn admin#21 tại report D02aa.


## D02ab — admin configuration backend, 2026-11-05

**D02ab — 2026-11-05:** API #21/#22 cấu hình ngôn ngữ/limits/sandbox/generator đã có backend thật; quyền riêng READ/UPDATE, khóa/version từng dòng, một audit diff và rollback hai chiều. 29 test liên quan đạt (21 mới), 6/6 mutation, 96/97 dòng logic mới; Spotless judge/bootstrap, Checkstyle phạm vi F4/bootstrap và SpotBugs đạt. F4-3 còn queue/infra/STOMP admin; chưa full regression/merge, FE riêng. [Report](F:/prj_algoprep/06-plan/reports/261105-1204-f4-admin-language-config/report.md).

JSON decimal giữ scale, kiểm exact3 languages/int dương/boolean/version bắt buộc trước business constraints; ROUND HALF_UP kiểm mọi ngôn ngữ kể cả disabled. Khóa/version/audit REQUIRED; audit lỗi rollback cả4 nhóm, lỗi sau audit cũng rollback audit; no-op không ghi. [SoT: Suy luận] boolean/version malformed→JDG-V10; DB/audit lỗi→503 chung không thêm mã, audit diff dotted field paths. Network mode và runtime admin→engine E2E giữ giới hạn ở report/FINAL.


## D02ac — queue controls, 2026-11-05

**D02ac — 2026-11-05:** API #16–19 pause/resume, autoscale và queue settings có backend thật; resume tái dùng kernel heartbeat D02p, quyền ADMIN riêng với Function READ cho GET, stale409, no-op và audit REQUIRED. 34 test liên quan đạt (22 mới),9/9 mutation,73/73 dòng logic mới; Spotless judge/bootstrap, Checkstyle đúng phạm vi và SpotBugs đạt. F4-3 còn snapshot/jobs/infra/STOMP admin, runtime cuối ở FINAL; chưa full regression/merge, FE riêng. [Report](F:/prj_algoprep/06-plan/reports/261105-1547-f4-queue-controls/report.md).

Kernel resume và worker gate/scaling không sửa. Outer transaction giữ khóa cluster qua heartbeat/actor/audit; audit lỗi rollback cả flag và heartbeat; lỗi sau audit rollback audit. JDG-V16 cho boolean bắt buộc/sai JSON, JDG-V09 cho int thiếu/sai kiểu/nonpositive/vượt INT. Không nhận số dạng chuỗi hoặc số thập phân. Mọi int1..2147483647 được nhận, không chặn100000. Settings last-writer-wins; không so updatedAt gửi lại. [SoT: Suy luận] lỗi hạ tầng503 chung không thêm mã; không fallback dữ liệu giả. Không thay lịch3s/15s, không đợi scale trong PUT; live Rabbit acceptance vẫn FINAL.


## D02ad — queue snapshot/jobs, 2026-11-05

**D02ad — 2026-11-05:** Backend snapshot queue #13 và danh sách job #14 xong: trạng thái/độ trễ từ SQL, counts và phân trang theo config thật, retention không xóa submission, quyền READ. 23 test liên quan đạt (14 mới),6/6 mutation,46/47 dòng logic mới; Spotless judge/bootstrap, scoped Checkstyle và SpotBugs đạt. F4-3 còn infra-summary/events và admin STOMP, rồi FINAL BE; chưa full regression/merge, FE riêng. [Report](F:/prj_algoprep/06-plan/reports/261105-1802-f4-queue-snapshot-jobs/report.md).

page/tab normalize theo bảng3/7; beyond-end clamp, empty page1, pageSize từ DB không nhận override URL. Snapshot ưu tiên PAUSED rồi pending>threshold; mean theo started time một giờ bao gồm cận dưới, loại future. Counts/page/retention cùng transaction time và REPEATABLE_READ; errors xếp cuối, tie UUID tăng. [SoT: Suy luận] round avg/truncate elapsed ms/64bit; generic503 không mã mới; max8 startup guard. Không xóa hoặc ẩn lịch sử bài nộp do hết retention hiển thị queue.


**D02ae — 2026-11-05:** Backend infra-summary #15 và infra-events #20 xong: cùng nguồn SQL thật, counts24h/danh sách90 ngày, message an toàn, lọc/ngày UTC+7 và cursor20 dòng. 20 test liên quan đạt (12 mới),6/6 mutation,78/78 dòng logic mới; Spotless judge/bootstrap, scoped Checkstyle và SpotBugs đạt. F4-3 còn admin STOMP W2, rồi FINAL BE; chưa full regression/merge, FE/QA riêng. [Report](F:/prj_algoprep/06-plan/reports/261105-f4-infra-summary-events/report.md).


**D02af — 2026-11-05:** STOMP admin W2 có QUEUE_BATCH thật theo chu kỳ5s, một lô chung cho mọi admin, không query khi không có subscriber hợp lệ. Snapshot/counts/infra-summary cùng transaction RR; outbound chặn phiên hết JWT hoặc đã unsubscribe.64 test liên quan đạt (18 mới),6/6 mutation,28/28 dòng logic mới; Spotless/Checkstyle/SpotBugs phạm vi chọn đạt. Backend F4-3 đủ API và W2; tiếp theo FINAL BE/full regression khi clear. FE/browser/C08 và production nhiều process giữ mốc riêng. Chưa commit/merge. [Report](F:/prj_algoprep/06-plan/reports/261105-f4-admin-queue-stream/report.md).
