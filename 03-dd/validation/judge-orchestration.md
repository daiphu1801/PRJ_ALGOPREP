# DD Validation — Module `judge-orchestration` (F4)

> Chỉ ghi quy tắc kiểm mà BD chưa tự suy ra hoặc cần chốt thành mã lỗi cụ thể: trường, quy tắc, mã lỗi, **lớp** thực thi. Không lặp lại thông điệp giao diện đã có ở Sheet 9 các BD màn (`02-bd/screens/**`); file này là bản cố định **ở máy chủ** của các kiểm tra đó.
> Nguồn: `02-bd/database/judge-orchestration.md` (kiểu cột, CHECK), Sheet 9 của `USR0102`, `USR0201`, `USR0202`, `USR0501`, `INS0206`, `ADM0401`, `ADM0404`, `ADM0501`. Hợp đồng endpoint ở `03-dd/api/judge-orchestration.md`; mã lỗi tổng hợp ở mục 27 của file đó.
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
| `language` | một trong `JAVA`, `CPP`, `PYTHON`; không phân biệt hoa thường thì chuẩn hoá về chữ hoa trước khi so | `JDG-V03` | 400 | Presentation | `database/judge-orchestration.md:22` |
| `language` (đang bật) | `language_configs.enabled = true` | `JDG-102` | 422 | Application | `ADM0501` Sheet 9 NO 6, NO 14 |
| `submissionModel` | một trong `FUNCTION_WRAPPER`, `STANDARD_IO` | `JDG-V04` | 400 | Presentation | `database/judge-orchestration.md:23` |
| `submissionModel` (hợp lệ cho bài) | `FUNCTION_WRAPPER` chỉ khi bài có `functionWrapperSupported = true` | `JDG-103` | 422 | Application | F3-13, `USR0102` Sheet 9 NO 9 |
| `sourceCode` (không rỗng) | sau khi bỏ khoảng trắng đầu cuối phải còn ít nhất 1 ký tự | `JDG-V01` | 400 | Presentation | `USR0102` Sheet 9 NO 7 |
| `sourceCode` (kích thước) | `octet_length(sourceCode UTF-8) <= 65536`; **đo bằng byte, không bằng số ký tự**; đúng 65.536 byte được nhận, 65.537 byte bị từ chối | `JDG-V02` | 400 | Presentation, và `CHECK (octet_length(source_code) <= 65536)` ở DB (phòng thủ) | `database/judge-orchestration.md:24`; `USR0102` Q7 |
| `sourceCode` (nội dung) | không cắt, không chuẩn hoá xuống dòng, không lọc ký tự; lưu nguyên văn; mã luôn đi vào `harness` như **dữ liệu** | không có | | | `README.md` ràng buộc F5 và F3 |
| `sourceInputMethod` | một trong `EDITOR`, `FILE_UPLOAD` (chỉ cho nộp bài; run-sample bỏ qua) | `JDG-V05` | 400 | Presentation | `database/judge-orchestration.md:25` |
| `fileName` | bắt buộc khi `FILE_UPLOAD`; đuôi (không phân biệt hoa thường) phải là `.java` cho `JAVA`, `.cpp` cho `CPP`, `.py` cho `PYTHON`; tối đa 255 ký tự `[SoT: Suy luận]`; **chỉ dùng để kiểm đuôi, không lưu** | `JDG-V06` | 400 | Presentation | `USR0102` Sheet 9 NO 5; trường này là đề xuất (file API Q9) |
| Testcase của bài | số testcase đã duyệt lớn hơn 0 (nộp bài); số Sample đã duyệt lớn hơn 0 (chạy thử) | `JDG-104`, `JDG-105` | 422 | Application | |
| Giới hạn lượt nộp mỗi giờ | số lượt nộp của cặp (người, bài) trong **60 phút gần nhất** `< giới hạn`; giới hạn = `max_submissions_per_hour` của bài hoặc 60 khi `NULL`; đo ở `now()` của PostgreSQL | `JDG-106` | 429 | Application, dưới khoá tư vấn theo cặp | `USR0102` Sheet 9 NO 10; `database/problem-bank.md:295` |
| Suất chạy thử đồng thời | semaphore toàn cục `judge.run-sample.max-concurrent` = 8 | `JDG-502` | 429 | Application | file API Q5 |
| Phạm vi dữ liệu | `userId` luôn là người trong token; không có trường nào khác chọn người | | | Application | `USR0102` Sheet 9 NO 11 |

## 3. Danh sách của học viên (endpoint #4, #7, #8)

**Bảng 3: Tham số danh sách học viên**

| Trường | Quy tắc | Mã lỗi | Lớp | Nguồn |
| :--- | :--- | :--- | :--- | :--- |
| `q` | bỏ khoảng trắng hai đầu; cắt ở 100 ký tự (không báo lỗi) | không có | Presentation | `USR0202` Sheet 9 NO 3 |
| `verdict` | `ALL`, `ACCEPTED`, `WRONG_ANSWER`, `TIME_LIMIT_EXCEEDED`, `COMPILE_ERROR`, `RUNTIME_ERROR`; giá trị lạ **đưa về `ALL`**, không lỗi | không có | Presentation | `USR0202` Sheet 9 NO 4 |
| `language` | `ALL`, `JAVA`, `CPP`, `PYTHON`; giá trị lạ về `ALL` | không có | Presentation | `USR0202` Sheet 9 NO 4 |
| `page` | số nguyên từ 1; không hợp lệ về 1; lớn hơn tổng số trang về trang cuối | không có | Presentation | `INS0206` Sheet 9 NO 7 (cùng quy tắc) |
| `limit` (endpoint #4, #8) | số nguyên: #4 từ 1 đến 10 (mặc định 10); #8 từ 1 đến 20 (mặc định 3) | `JDG-V14` | Presentation | `USR0102` dòng 305-308; `USR0101` Q4 |

## 4. Truy xuất bài nộp (endpoint #5, #6, #11)

| Trường | Quy tắc | Mã lỗi | HTTP | Lớp | Nguồn |
| :--- | :--- | :--- | :-: | :--- | :--- |
| `submissionId` | UUID; sai định dạng, không có, hoặc của người khác: **cùng một mã và một thông điệp** | `JDG-110` | 404 | Application | `USR0201` Sheet 9 NO 2 |
| Quyền sở hữu (A1) | `submissions.user_id = tokenUserId` đặt trong điều kiện truy vấn, không lọc sau | `JDG-110` | 404 | Application | `USR0201` Sheet 9 NO 2 |
| Quyền A2 | `CLASS_MANAGEMENT:READ`; thiếu thì 403; có nhưng ngoài phạm vi lớp thì 404 (không phải 403) | `JDG-301`, `JDG-110` | 403, 404 | Application | `USR0201` A2-1 |
| `studentId`, `classId`, `from` ở URL chi tiết A2 | **bỏ qua** hoàn toàn khi quyết định quyền và dữ liệu | không có | | Application | `USR0201` A2-2 |
| Dữ liệu testcase ẩn | không bao giờ có trong phản hồi (BR-JD-11): `actualOutput`, `testcaseId`, `stderrSnippet` chỉ khi testcase `SAMPLE` (hai lớp độc lập); expected output không bao giờ | không có | | Application, tầng dựng phản hồi | `USR0102` Sheet 9 NO 3; `README.md` F2 |
| Quá 90 ngày | `submitted_at < now() - 90 ngày` thì `testcaseDetail.available = false`, `RETENTION_EXPIRED`, `items = []` | không có | | Application | `database/judge-orchestration.md:68-72` |
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
| `autoscaleJobThreshold`, `jobsPerPage`, `errorRetentionHours`, `latencyCapSeconds` (#19) | **số nguyên từ 1 trở lên**; số thập phân, chuỗi, rỗng, số nhỏ hơn 1 đều bị từ chối; **không có giá trị tối đa nghiệp vụ**, chỉ bị chặn bởi kiểu `INT` (2.147.483.647) | `JDG-V09` | 400 | Presentation; và `CHECK > 0` ở DB | `ADM0401` Sheet 9 NO 8; `database/judge-orchestration.md:150-153` |
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
| `languages` | đúng 3 phần tử, đủ `JAVA`, `CPP`, `PYTHON`; không thêm, không xoá dòng | `JDG-V10` | 400 | Presentation | `database/judge-orchestration.md:107-108` |
| `timeLimitMultiplier`, `memoryLimitMultiplier` | số thập phân, **tối đa 2 chữ số thập phân**, trong khoảng **0,10 đến 9,99**; bỏ trống, chữ, âm, 0 đều không hợp lệ; ví dụ 0,09 và 10,00 bị từ chối | `JDG-V10` | 400 | Presentation; DB `DECIMAL(3,2)` | `ADM0501` Sheet 9 NO 2, NO 3, NO 10 |
| `enabled` | boolean; sau thay đổi phải còn **ít nhất một** ngôn ngữ `true` | `JDG-203` | 422 | Application | `ADM0501` Sheet 9 NO 6 |
| `gradingDefaults.timeLimitMs`, `memoryLimitMb`, `maxOutputSizeKb`, `compileTimeoutSeconds` | số nguyên từ 1; ô rỗng, chữ, số thập phân đều bị từ chối | `JDG-V11` | 400 | Presentation; DB `CHECK > 0` | `ADM0501` Sheet 9 NO 13 |
| Giới hạn thời gian hiệu dụng | với **mỗi** ngôn ngữ: `ROUND(timeLimitMs * timeLimitMultiplier)` trong 100 đến 30.000 ms; kiểm cả khi chỉ `timeLimitMs` mặc định đổi (hai thừa số đổi độc lập) | `JDG-204` | 422 | Application | `ADM0501` Sheet 9 NO 4 |
| Giới hạn bộ nhớ hiệu dụng | với **mỗi** ngôn ngữ: `ROUND(memoryLimitMb * memoryLimitMultiplier)` trong 64 đến 1.024 MB | `JDG-205` | 422 | Application | `ADM0501` Sheet 9 NO 5 |
| `sandbox.*` | ba boolean; **không có** trường `maxChildProcesses` (trần 60 là hằng số `application.yml`); gửi thừa trường thì bỏ qua | không có | | Presentation | `database/judge-orchestration.md:193` |
| `generatorSandbox.maxRuntimeSeconds` | số nguyên 1 đến 300 | `JDG-V12` | 400 | Presentation; DB `CHECK` | `ADM0501` Sheet 9 NO 9; `database/judge-orchestration.md:164` |
| `generatorSandbox.maxOutputMb` | số nguyên 1 đến 1.024 | `JDG-V12` | 400 | Presentation; DB `CHECK` | `database/judge-orchestration.md:165` |
| `*.updatedAt` | phải bằng `updated_at` hiện tại của từng dòng; lệch thì từ chối, **không ghi đè** | `JDG-202` | 409 | Application, dưới khoá `FOR UPDATE` | `ADM0501` Sheet 9 NO 7 |
| Tắt một ngôn ngữ | **không** đổi trạng thái hay huỷ job đang chờ của ngôn ngữ đó | không có | | Application | `ADM0501` Sheet 9 NO 14 |

Thứ tự kiểm của một lần lưu: định dạng và khoảng từng trường (`JDG-V10`..`V12`), rồi quy tắc liên trường (`JDG-203`, `JDG-204`, `JDG-205`), rồi xung đột phiên bản (`JDG-202`) dưới khoá. Một lỗi ở bất kỳ bước nào thì **không ghi gì** và không có dòng nhật ký.

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
| `judge.submission.max-source-bytes` | 65536 | `database/judge-orchestration.md:24`, `USR0102` Q7 |
| `judge.submission.default-hourly-limit` | 60 | `database/problem-bank.md:295` |
| `judge.sweep.stale-after` | 5 phút, `judge.sweep.interval` 60 giây | `03-dd/jobs/judge-orchestration.md` mục 1 |
| `judge.outbox.relay-interval` | 2 giây; cỡ lô 100 | `03-dd/jobs/judge-orchestration.md` mục 2 |
| `judge.infra.outbox-warn-attempts` | 3 | `database/judge-orchestration.md:243` |
| `judge.infra.outbox-error-attempts` | 5 | `database/judge-orchestration.md:243` |
| `judge.infra.event-retention-days` | 90 | `ADM0404` Q3, Q5 |
| `judge.results.retention-days` | 90 | `database/judge-orchestration.md:68-72` |
| `judge.stderr.max-chars`, `judge.output-snippet.max-chars` | 4096 | `database/judge-orchestration.md:63-64` |
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
5. **Lệnh ghi quản trị** luôn kiểm lại vai trò `ADMIN` ở máy chủ (`02-bd/security/judge-orchestration.md:73-76`) và luôn có dòng nhật ký cùng giao dịch.
6. **Suy giảm êm**: không kiểm nào ở file này phụ thuộc đường AI; F5 hoặc F6 chết không ảnh hưởng việc kiểm và chấm của F1 đến F4 (`CLAUDE.md` mục Rules).

## Tham chiếu

`03-dd/api/judge-orchestration.md` (hợp đồng, mã lỗi), `03-dd/logic/judge-orchestration.md` (BR-JD-nn), `03-dd/jobs/judge-orchestration.md`, `02-bd/database/judge-orchestration.md` (kiểu cột, CHECK), `02-bd/security/judge-orchestration.md`, Sheet 9 của các BD màn đã nêu.
