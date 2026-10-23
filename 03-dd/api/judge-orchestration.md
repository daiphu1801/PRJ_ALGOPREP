# DD API — Module `judge-orchestration` (F4)

> Nguồn: `01-rd/req/judge-orchestration.md` (F4-01 đến F4-13) · `02-bd/architecture/judge-orchestration.md` · `02-bd/database/judge-orchestration.md` ·
> `02-bd/security/judge-orchestration.md` · Sheet 7.3 của các BD màn (`USR0102`, `USR0201`, `USR0202`, `USR0501`, `USR0601`, `USR0105`, `USR0101`, `INS0204`, `INS0206`, `ADM0401`, `ADM0404`, `ADM0501`, `ADM0101`).
> Bounded Context: F4 `judge-orchestration` (schema `judge`, Maven module `algoprep-judge`) · Cập nhật: 2026-10-23.
> Đọc cùng: `03-dd/logic/judge-orchestration.md` (khối BR-JD-nn), `03-dd/validation/judge-orchestration.md`, `03-dd/jobs/judge-orchestration.md` (JOB-JD-01 đến JOB-JD-04, **không viết lại ở đây**), `04-tdd/judge-orchestration.md`.
>
> BD chỉ đặt **tên nghiệp vụ** của endpoint (không có path, method). DD này đặt path, method, request, response, mã lỗi. Mọi con số, tên trường hoặc quy tắc mà RD/BD không nêu được đánh dấu `[SoT: Suy luận]` kèm lý do, và gom ở mục "Câu hỏi mở" cuối file. Hợp đồng endpoint chỉ nằm ở file này (quy tắc chống trôi lệch của `dd-generation`); các file `03-dd/screens/**` chỉ liệt kê và trỏ về đây.

## 1. Phạm vi và quy ước chung

### 1.1. Phạm vi

Bảng 1 liệt kê các nhóm giao tiếp mà DD này đặc tả. Đây là danh mục cho tới khi controller dựng `03-dd/api/api.md`; mỗi dòng có một mục chi tiết bên dưới.

**Bảng 1: Danh mục endpoint và kênh của `judge-orchestration`**

| # | Loại | Method và path (sau `/api/v1/judge`) | Tên nghiệp vụ (BD) | Màn gọi | Quyền |
| :-: | :--- | :--- | :--- | :--- | :--- |
| 1 | REST | `GET /problems/{problemId}/execution-limits` | `GetEffectiveExecutionLimits` | `USR0102` | STUDENT |
| 2 | REST | `POST /problems/{problemId}/run-sample` | `RunSampleTestcases` | `USR0102` | STUDENT |
| 3 | REST | `POST /problems/{problemId}/submissions` | `CreateSubmission` | `USR0102` | STUDENT |
| 4 | REST | `GET /problems/{problemId}/submissions/mine` | `ListMySubmissionsForProblem` | `USR0102` | STUDENT |
| 5 | REST | `GET /submissions/{submissionId}` | `GetSubmissionDetail` (A1) | `USR0201` | STUDENT, chủ bài nộp |
| 6 | REST | `GET /submissions/{submissionId}/progress` | ảnh chụp tiến trình, dự phòng kênh realtime | `USR0102`, `USR0201` | STUDENT, chủ bài nộp |
| 7 | REST | `GET /me/submissions` | `ListMySubmissions` | `USR0202` | STUDENT |
| 8 | REST | `GET /me/in-progress-problems` | `ListMyUnsolvedRecentProblems` | `USR0101`, `USR0105` | STUDENT |
| 9 | REST | `GET /me/metrics` | `GetMySubmissionMetrics` | `USR0501`, `USR0601` | STUDENT |
| 10 | REST | `GET /instructor/submissions` | `ListInstructorSubmissions` | `INS0206` | INSTRUCTOR, `CLASS_MANAGEMENT:READ`, phạm vi lớp |
| 11 | REST | `GET /instructor/submissions/{submissionId}` | `GetSubmissionDetail` (A2) | `USR0201` vùng giảng viên | `CLASS_MANAGEMENT:READ`, phạm vi lớp |
| 12 | REST | `GET /instructor/classes/{classId}/students/{studentId}/submissions` | `ListStudentClassSubmissions` | `INS0204` | `CLASS_MANAGEMENT:READ`, phạm vi lớp |
| 13 | REST | `GET /admin/queue/snapshot` | `GetQueueSnapshot` | `ADM0401` | `JUDGE_QUEUE_MONITOR:READ` |
| 14 | REST | `GET /admin/queue/jobs` | `ListQueueJobs` | `ADM0401` | `JUDGE_QUEUE_MONITOR:READ` |
| 15 | REST | `GET /admin/queue/infra-summary` | `GetInfraSummary` | `ADM0401` | `JUDGE_QUEUE_MONITOR:READ` |
| 16 | REST | `PUT /admin/queue/consumption` | `SetQueueConsumptionPaused` | `ADM0401` | ADMIN |
| 17 | REST | `PUT /admin/queue/autoscale` | `SetWorkerAutoscaleEnabled` | `ADM0401` | ADMIN |
| 18 | REST | `GET /admin/queue/settings` | `GetQueueSettings` | `ADM0401` | `JUDGE_QUEUE_MONITOR:READ` |
| 19 | REST | `PUT /admin/queue/settings` | `UpdateQueueSettings` | `ADM0401` | ADMIN |
| 20 | REST | `GET /admin/infra-events` | `ListInfraEvents` | `ADM0404` | `JUDGE_QUEUE_MONITOR:READ` |
| 21 | REST | `GET /admin/language-config` | `GetLanguageConfigs` | `ADM0501` | `LANGUAGE_CONFIG:READ` |
| 22 | REST | `PUT /admin/language-config` | `UpdateLanguageConfigs` | `ADM0501` | `LANGUAGE_CONFIG:UPDATE` |
| W1 | STOMP | đích `/user/queue/submissions` | `SubscribeSubmissionProgress` / `SubscribeSubmissionRealtime` | `USR0102`, `USR0201` | STUDENT, chủ bài nộp |
| W2 | STOMP | đích `/topic/admin/queue` | `SubscribeQueueMonitorStream` | `ADM0401` | `JUDGE_QUEUE_MONITOR:READ` |
| P1 | Cổng trong tiến trình | `GetClassStudentSubmissionMetrics` | như tên | `INS0201`, `INS0203`, `INS0204` (qua `identity`) | do `identity` gác |
| P2 | Cổng trong tiến trình | `GetAdminSubmissionStats` (cổng `JudgeStatsPort`) | cung cấp số liệu cho `ADM0101` qua `identity` | `ADM0101` (qua `identity`) | do `identity` gác |
| P3 | Cổng trong tiến trình | `JudgeConfigReadPort` | đọc giới hạn script sinh testcase cho `problem-bank` | `SHR0202` (qua `problem-bank`) | trong tiến trình |

Ngoài phạm vi (không có endpoint, đã loại): chấm lại (`DEC-2026-0828-remove-rejudge-scope`), webhook của `Judge0Adapter` (chi tiết nội bộ adapter, `02-bd/security/judge-orchestration.md:40-53`), xoá hay ẩn bài nộp (`USR0202` Q3: không bao giờ xoá, API không có thao tác ghi nào trên bài nộp ngoài tạo mới).

### 1.2. Quy ước chung

- **Base path** `/api/v1/judge`, JSON UTF-8, thời gian ISO-8601 UTC (`2026-10-23T03:15:00Z`). Múi giờ cắt ngày của mọi phép gom theo ngày là **UTC+7 cố định** (`02-bd/database/judge-orchestration.md:74-75`, `DEC-2026-1018-usr-open-questions-phase2-5`).
- **Xác thực**: `Authorization: Bearer <access_token>` cho mọi endpoint (không có endpoint công khai). Danh tính (`userId`, `baseCategory`) lấy **từ token**; không endpoint nào nhận `userId` làm tham số (quy ước tiền tố `My`, `USR0102` Sheet 7.3 dòng 755-757).
- **Mã trạng thái**: `400` lỗi định dạng, `401` thiếu hoặc hết hạn token (mã chung của hệ thống, như `identity`), `403` thiếu quyền, `404` không tìm thấy, `409` xung đột trạng thái, `422` vi phạm quy tắc nghiệp vụ, `429` vượt giới hạn tần suất hoặc công suất, `503` hạ tầng chấm không sẵn sàng.
- **Phong bì lỗi**: `{ "error": { "code": "JDG-106", "message": "<chuỗi dự phòng>", "details": { ... } } }`. Backend trả `code`; giao diện dịch qua `messages/{locale}.json` (hợp đồng i18n ở `03-dd/api/api.md`, chưa dựng). Mã lỗi nằm ở mục 27.
- **Phân trang** (mọi danh sách có `page`): `page` bắt đầu từ 1; không hợp lệ thì về 1; lớn hơn tổng số trang thì đưa về trang cuối hợp lệ (`02-bd/screens/teacher/INS0206_submission_management.md:548`). Kích thước trang là hằng số phía máy chủ **20** (`INS0206` dòng 225; `USR0202` và `INS0204` dùng cùng hằng số `[SoT: Suy luận]`, vì BD của hai màn đó không nêu số). Phản hồi danh sách luôn có dạng `{ "items": [...], "page": n, "pageSize": 20, "totalCount": n }`.
- **Bảng trạng thái** (`status`, chín giá trị, `02-bd/database/judge-orchestration.md:26`): `PENDING`, `COMPILING`, `JUDGING` là chưa cuối; `ACCEPTED`, `WRONG_ANSWER`, `TIME_LIMIT_EXCEEDED`, `RUNTIME_ERROR`, `COMPILE_ERROR`, `SYSTEM_ERROR` là **sáu trạng thái cuối**, không bao giờ bị ghi đè (`02-bd/architecture/judge-orchestration.md:157-173`).
- **Verdict từng testcase** (`verdict`, năm giá trị): `AC`, `WA`, `TLE`, `MLE`, `RE` (`02-bd/database/judge-orchestration.md:60`). `MLE` gộp thành `TIME_LIMIT_EXCEEDED` ở cấp bài nộp.
- **Quy tắc cứng về testcase ẩn** (áp cho mọi endpoint của học viên và giảng viên; chi tiết BR-JD-11): không bao giờ trả input, expected output, output thực tế hay diff của testcase `HIDDEN`; chỉ trả `order`, `verdict`, `runtimeMs`, `memoryKb`. `actualOutput` chỉ trả cho testcase `SAMPLE` (`USR0102` Sheet 9 NO 3 dòng 828; `USR0201` Sheet 9 NO 3 dòng 470; `README.md` F2).
- **Suy giảm êm**: không endpoint nào của module này gọi `ai-review` (`INS0206` Sheet 9 NO 14 dòng 555). Lỗi của `problem-bank`, `identity` hay Prometheus chỉ làm mất phần dữ liệu ghép thêm (tên bài, tên học viên, số liệu worker), không làm hỏng luồng nộp và chấm lõi (BR-JD-07, BR-JD-15).

### 1.3. Cổng ra mà module cần (tên theo BD, DD của module kia chốt chữ ký)

Bảng 2 liệt kê các cổng ra module này gọi. Chữ ký là **kỳ vọng của F4**; DD `problem-bank` và DD `harness` được viết song song cần đối chiếu với bảng này (danh sách cổng và sự kiện nằm ở báo cáo bàn giao của DD này). `[SoT: Suy luận]` cho mọi tên chưa có trong BD.

**Bảng 2: Cổng ra của `judge-orchestration`**

| Cổng | Chủ | Thao tác F4 cần | Nguồn |
| :--- | :--- | :--- | :--- |
| `JudgeExecutionPort` | F4 (adapter `GoJudgeAdapter`) | `compile(CompileRequest) -> CompileResult` (một lần mỗi bài nộp) và `run(RunRequest) -> RunResult` (một lần mỗi testcase) | `02-bd/architecture/judge-orchestration.md:45-63`; thao tác `compile` là đề xuất, xem câu hỏi mở Q3 |
| `ProblemGradingSpecPort` | `problem-bank` | `loadGradingSpec(problemId)`: trạng thái xuất bản, `functionWrapperSupported`, `timeLimitMs`, `memoryLimitMb`, `maxOutputSizeKb`, `maxSubmissionsPerHour` (nullable), `currentTestcaseSetVersion`, `specVersion`, `matchingStrategy`, `epsilonValue`, danh sách testcase chấm (`testcaseId`, `visibility`, `position`) không kèm nội dung; `loadTestcaseData(problemId, testcaseId)`: input và expected output (hỗ trợ cả `INLINE` và MinIO); `loadSampleTestcases(problemId)` | `02-bd/architecture/judge-orchestration.md:65-73` |
| Cổng vào của `harness` (không có HTTP) | `harness` | `PrepareSubmissionUseCase.prepare(problemId, language, model, sourceCode, expectedSpecVersion) -> PreparedProgram` (`payload` theo định dạng adapter, `lineMap`, `matchSpec`, `resultSentinel`, `harnessInputErrorExitCode`); `MapCompileErrorUseCase.map(language, model, rawCompilerOutput, lineMap) -> CompileErrorReport` (`USER_ERRORS` hoặc `HARNESS_INTERNAL`, `diagnostics[]`, `fixedMessageKey`); `MatchOutputUseCase.match(model, matchSpec, resultSentinel, actualStdout, expectedOutput) -> MatchResult` (`MATCH` hoặc `NO_MATCH`, `actualOutput`, `userConsole`) | tên và chữ ký theo `03-dd/api/harness.md` mục 1.1, 2, 6; BD `02-bd/architecture/harness.md:220-226` |
| `ProblemSummaryReadPort` | `problem-bank` | `getSummaries(problemIds) -> [{problemId, code, title, slug, levelDisplayName}]`; `searchProblemIds(keyword, limit)` | `USR0202` Sheet 5 khu vực C NO 2 (mã, tên, nhãn độ khó) |
| `ProblemTopicReadPort` | `problem-bank` | `getProblemIdsByTopics(topicIds) -> map topicId -> problemIds` | `USR0501` Sheet 7.3 NO 3 dòng 520 |
| `ClassAssignmentReadPort` | `problem-bank` | `listAssignments(classIds) -> [{classId, problemId, problemTitle, assignedAt, removedAt}]` kể cả bài đã gỡ mềm | `INS0206` Sheet 7.2 NO 5; `ListClassAssignments` ở `INS0204` NO 3 |
| `ClassScopeReadPort` | `identity` | `getInstructorScope(instructorId, classId?) -> [{classId, className, studentId, studentName, joinedAt, leftAt}]` | `INS0206` Sheet 7.2 NO 2 đến 4, dòng 480-482 |
| `AuditLogPort` | `identity` (cổng vào) | `record(actorUserId, actionType, targetType, targetId, beforeJson, afterJson)` trong cùng giao dịch | `02-bd/architecture/identity.md:80-94`; `03-dd/logic/identity.md` BR-24 |
| Prometheus HTTP API | hạ tầng | số liệu worker go-judge (mã worker, trạng thái, tải, số job, uptime, vCPU) | `ADM0401` Q2 dòng 688 |

## 2. Hợp đồng thời gian thực (WebSocket, STOMP)

Mục này đặc tả kênh theo `dd-generation` (Must Do: kênh realtime là một phần hợp đồng). Nguồn: F4-08 (`01-rd/req/judge-orchestration.md:47-49`), `USR0201` Q1 dòng 615 (đích `/user/queue/submissions`), `ADM0401` Q1 dòng 687 (`/topic/admin/queue`).

### 2.1. Kết nối và xác thực

| Mục | Giá trị |
| :--- | :--- |
| Điểm bắt tay | `GET /ws` nâng cấp lên WebSocket, giao thức con STOMP 1.2. Không dùng SockJS: đường dự phòng là REST (mục 2.5) `[SoT: Suy luận]` |
| Xác thực | Khung `CONNECT` mang header `Authorization: Bearer <access_token>`; một `ChannelInterceptor` kiểm chữ ký và hạn của token, đặt `Principal.name = userId`. Token sai hoặc thiếu: máy chủ trả khung `ERROR` rồi đóng, không nêu lý do chi tiết |
| Gửi lên máy chủ | Không có điểm đến ứng dụng nào (`/app/**` không tồn tại). Mọi khung `SEND` từ client bị từ chối `[SoT: Suy luận]`: kênh chỉ một chiều máy chủ tới client |
| Môi giới thông điệp | Một instance backend (Docker Compose) dùng broker đơn giản trong bộ nhớ; chạy nhiều instance thì chuyển sang broker relay RabbitMQ cùng cấu hình đích `[SoT: Suy luận]`: `README.md` liệt kê RabbitMQ và STOMP nhưng không chốt số instance |
| Hạn phiên | Token chỉ kiểm lúc `CONNECT`. Phiên của học viên ngắn (kết thúc cùng bài nộp). Với `/topic/admin/queue`, máy chủ đóng phiên khi `exp` của token tới; client nối lại bằng token mới trong lúc REST dự phòng làm tươi dữ liệu `[SoT: Suy luận]` |

### 2.2. Đích đăng ký và kiểm quyền

| Đích | Ai đăng ký được | Quy tắc kiểm (interceptor, khung `SUBSCRIBE`) |
| :--- | :--- | :--- |
| `/user/queue/submissions` | `baseCategory = STUDENT` | Chỉ đúng đích này; mọi đích `/user/**` khác bị từ chối. Máy chủ chỉ đẩy bản tin của bài nộp có `user_id = Principal.name` tới đích này của người đó (`convertAndSendToUser(userId, "/queue/submissions", ...)`), nên đoán `submissionId` của người khác không nghe được gì (`USR0102` Sheet 9 NO 12 dòng 837, `USR0201` Sheet 9 NO 7 dòng 474) |
| `/topic/admin/queue` | `baseCategory = ADMIN` và `JUDGE_QUEUE_MONITOR:READ` | Kiểm cả hai ở `SUBSCRIBE`. Người khác: từ chối im lặng |
| Mọi đích khác | không ai | Từ chối |

Từ chối là **im lặng**: khung `ERROR` generic, không xác nhận `submissionId` có tồn tại hay không (`USR0102` Sheet 9 NO 12).

**Một kênh theo người, khoá theo `submissionId` trong từng bản tin.** `USR0102` Sheet 7.3 (dòng 764) ghi "một kênh khoá theo `submissionId`"; `USR0201` Q1 (dòng 615, đã chốt 2026-10-18) chọn đích người dùng `/user/queue/submissions`. DD theo quyết định sau (có ngày, có chủ dự án): mọi bản tin của mọi bài nộp đang chạy của một người đi qua **một** đích, mỗi bản tin mang `submissionId`; phía client đối chiếu `submissionId` với bài đang theo dõi và bỏ bản tin lệch (`USR0102` EVT-12 bước 1, dòng 801). Câu chữ ở `USR0102` dòng 764 lệch nhẹ với quyết định này, ghi ở câu hỏi mở Q10.

### 2.3. Lược đồ bản tin

Mọi bản tin là JSON, có các trường chung:

| Trường | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `type` | enum | `STATUS_CHANGED`, `TESTCASE_FINISHED`, `SUBMISSION_FINISHED` |
| `submissionId` | UUID | |
| `seq` | int | Số thứ tự tăng ngặt trong một bài nộp, **tất định** (mục 2.4) |
| `isFinal` | boolean | `true` chỉ ở `SUBMISSION_FINISHED` (tương ứng `isCompleted` ở `USR0201` EVT-9 dòng 460) |
| `occurredAt` | ISO-8601 | Thời điểm máy chủ phát |

**Bảng 3: Trường riêng của từng loại bản tin**

| `type` | Phát khi | Trường riêng | Ghi chú |
| :--- | :--- | :--- | :--- |
| `STATUS_CHANGED` | worker chuyển `PENDING -> COMPILING` và `COMPILING -> JUDGING` | `status` (`COMPILING` hoặc `JUDGING`), `totalCount` (int; **khác null khi `JUDGING`**, null khi `COMPILING`) | Không phát bản tin cho `PENDING`: client đã biết từ phản hồi tạo bài nộp. `totalCount` là số testcase chấm, dùng dựng dải ô chờ |
| `TESTCASE_FINISHED` | ngay sau khi kết quả một testcase **đã commit** (F4-08) | `order` (int, `testcase_order` từ 1), `verdict` (`AC`, `WA`, `TLE`, `MLE`, `RE`), `completedCount`, `passedCount`, `totalCount` | **Chỉ** `order` và `verdict` mô tả testcase, đúng `USR0102` Sheet 9 NO 3. Không mang runtime, memory, output, stderr: các số đó đọc ở endpoint #5 sau khi kết thúc |
| `SUBMISSION_FINISHED` | bài nộp vào một trong sáu trạng thái cuối, kể cả `SYSTEM_ERROR` do sweep | `status` (trạng thái cuối), `passedCount` (null khi `COMPILE_ERROR`, `SYSTEM_ERROR`), `totalCount` (cùng quy tắc), `scoreRatio` (null khi `COMPILE_ERROR`, `SYSTEM_ERROR`), `firstFailingOrder` (int hoặc null), `runtimeMs`, `memoryKb` (null nếu chưa chạy testcase), `finishedAt` | `firstFailingOrder` là "chỉ số testcase hỏng đầu tiên" mà BD cho phép (`USR0102` Sheet 9 NO 3). Không mang `failureReason`, `compileErrorMessage`, `sourceCode`; client gọi endpoint #5 để lấy chi tiết sau bản tin cuối |

Ví dụ:

```json
{ "type": "TESTCASE_FINISHED", "submissionId": "b1f1...", "seq": 5, "isFinal": false,
  "order": 3, "verdict": "WA", "completedCount": 3, "passedCount": 2, "totalCount": 12,
  "occurredAt": "2026-10-23T03:15:02Z" }
```

### 2.4. Đảm bảo thứ tự

1. **Một người ghi duy nhất theo bài nộp.** Một bài nộp chỉ do một worker chấm tại một thời điểm (BR-JD-03, khoá tư vấn theo `submissionId`), mỗi bước phát bản tin **sau khi** giao dịch của bước đó commit. Vì vậy thứ tự phát khớp thứ tự ghi DB.
2. **`seq` tất định**: `COMPILING` = 1; `JUDGING` = 2; testcase thứ `i` = `2 + i`; `SUBMISSION_FINISHED` = `3 + N` khi biết `N` (đã vào `JUDGING`), bằng `3` khi chưa biết (`COMPILE_ERROR`, hoặc `SYSTEM_ERROR` trước `JUDGING`). Vì tất định, bản tin phát lại sau khi RabbitMQ giao lại message có cùng `seq` với bản gốc.
3. **Phía client**: áp bản tin có `seq` lớn hơn `seq` cuối đã áp; bản tin `seq` thấp hơn hoặc bằng thì bỏ (trừ bản `isFinal`, luôn áp). Hai bản tin cùng `seq` là bản phát lại, an toàn bỏ.
4. **Không đảm bảo**: không có bảo đảm giao đúng một lần, và không có bảo đảm không mất bản tin khi mất kết nối. Bản tin STOMP là gợi ý làm tươi; **nguồn sự thật là DB** (mục 2.5). Lý do khác với sự kiện miền (có Outbox): realtime chỉ phục vụ hiển thị, mất một bản tin được phục hồi bằng ảnh chụp; sự kiện miền nuôi `ai-review` và `identity` nên bắt buộc bền vững (`02-bd/architecture/judge-orchestration.md:263-270`).
5. **Phát sau commit, không trong giao dịch**: bản tin đẩy bằng `afterCommit`; lỗi đẩy chỉ ghi log `WARN`, không làm hỏng việc chấm (cùng cách `03-dd/jobs/judge-orchestration.md:69`).

### 2.5. Client nối lại giữa chừng nhận được gì

Broker đơn giản **không lưu bản tin**, nên nối lại không tự nhận lại bản tin đã bỏ lỡ. Quy trình bắt buộc cho client (và là đường REST dự phòng của `USR0201` Sheet 9 NO 5 dòng 472):

1. Nối lại STOMP và **đăng ký lại** `/user/queue/submissions` trước.
2. Gọi `GET /submissions/{submissionId}/progress` (endpoint #6): nhận `status`, `seq`, `totalCount`, `completedCount`, `passedCount` và danh sách `{order, verdict}` của mọi testcase đã xong.
3. Dựng lại dải ô từ ảnh chụp, rồi áp mọi bản tin có `seq` lớn hơn `seq` của ảnh chụp. Đăng ký trước, gọi REST sau, để không có khoảng hở giữa hai bước.
4. Ảnh chụp báo `isFinal = true` thì gọi endpoint #5, không cần kênh.
5. Khi kênh không nối lại được: hỏi lại endpoint #6 theo chu kỳ **3 giây, tối đa 10 lần** (`USR0201` Sheet 9 NO 5); hết 10 lần mà chưa kết thúc thì hiện dòng nhắc cập nhật đang chậm, **không kết luận bài trượt** (`USR0102` EVT-12 dòng 801). Hết bước này người học tải lại trang vẫn thấy trạng thái thật vì nó nằm ở DB.

Với `/topic/admin/queue`: ngoài việc nối lại, client dùng REST #13 đến #15 mỗi 15 giây khi kênh rớt hoặc quá 60 giây không có lô mới, và tải lại một lô đầy đủ khi nối lại được (`ADM0401` EVT-4, EVT-5 dòng 641-642).

### 2.6. Bản tin của kênh quản trị `/topic/admin/queue`

Mỗi 5 giây máy chủ đẩy một lô (`ADM0401` Q1 dòng 687), chỉ khi còn ít nhất một người đăng ký (tránh truy vấn DB khi không ai xem `[SoT: Suy luận]`):

| Trường | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `type` | string | `QUEUE_BATCH` |
| `generatedAt` | ISO-8601 | |
| `snapshot` | object | cùng hình dạng phản hồi endpoint #13 |
| `jobCounts` | object | `{ waiting, failed, all }`, cùng hình dạng `QueueJobCountsDto` |
| `infraSummary` | object | `{ warnCount, errorCount }` |

Bảng job không nằm trong lô: danh sách phụ thuộc tab và trang đang xem của từng admin, nên client gọi lại endpoint #14 cho tab và trang hiện tại mỗi khi nhận lô `[SoT: Suy luận]`, xem câu hỏi mở Q11.

## 3. Đường gọi ra judge engine: timeout, thử lại, khoá idempotency

Mục này trả lời yêu cầu của `dd-generation` cho mọi endpoint chạm engine ngoài. Thuật toán đầy đủ ở BR-JD-04 và BR-JD-07. Các con số là hằng số `application.yml` `[SoT: Suy luận]` vì BD không nêu (BD chỉ giao DD chốt ngưỡng nhận diện lỗi hạ tầng, `02-bd/architecture/judge-orchestration.md:175`).

**Bảng 4: Hợp đồng gọi `JudgeExecutionPort` (adapter mặc định go-judge)**

| Mục | Quy tắc |
| :--- | :--- |
| Cấu trúc gọi | `compile` một lần mỗi bài nộp, `run` một lần cho **mỗi** testcase, không gộp batch (F4-03, `DEC-2026-0823-go-judge-default-engine` điểm 3). **Chạy hết mọi testcase, không dừng sớm** (F4-04 đã bãi bỏ, `DEC-2026-0831-partial-score-testcase-ratio`) |
| Giới hạn do sandbox ép | Giới hạn thời gian và bộ nhớ đưa xuống go-judge làm tham số kill-process; sandbox là chốt chặn thật, không dựa vào timeout tầng ứng dụng (`02-bd/security/judge-orchestration.md:35-38`). Thời gian hiệu lực = `time_limit_ms` của bài nhân `time_limit_multiplier` của ngôn ngữ; bộ nhớ tương tự |
| Timeout kết nối | 2 giây |
| Timeout một lần `run` (lưới an toàn tầng ứng dụng) | thời gian hiệu lực của testcase cộng 5 giây. Chỉ để không treo luồng khi go-judge không phản hồi; không phải cách xác định `TLE` |
| Timeout một lần `compile` | `grading_defaults.compile_timeout_sec` cộng 5 giây (`database/judge-orchestration.md:179`: thời gian biên dịch không tính vào giới hạn chạy) |
| Phân loại kết quả | **Kết quả nghiệp vụ** (`OK`, hết thời gian, hết bộ nhớ, lỗi chạy, vượt output): đã có verdict, **không thử lại**. **Lỗi hạ tầng** (không nối được, `5xx`, quá timeout lưới an toàn, go-judge báo lỗi nội bộ, không khởi tạo được sandbox): được thử lại |
| Chính sách thử lại | Tối đa **2 lần thử lại** (3 lần gọi) với chờ 500 ms rồi 1 giây. Vì go-judge không có trạng thái, gọi lại là chạy lại, không tác dụng phụ |
| Hết lượt thử lại | Bài nộp sang `SYSTEM_ERROR` với `failure_reason = SANDBOX_FAULT` (`02-bd/architecture/judge-orchestration.md:175`), cùng giao dịch với dòng outbox. Không để RabbitMQ giao lại message (message được ack sau khi trạng thái cuối commit) |
| Khoá idempotency | `judge_run_tokens.token` (UUID) phát hành **trước** mỗi lần gọi `run` cho cặp `(submission_id, testcase_id)`, đánh dấu `consumed_at` trong **cùng giao dịch** ghi `submission_testcase_results`. Cặp đã có token tiêu thụ thì bỏ qua, không gọi lại engine, không ghi đè (`02-bd/architecture/judge-orchestration.md:192-205`). Các lần thử lại trong cùng một lượt dùng lại **một** token; token mới chỉ phát hành sau khi worker khác nhận lại message |
| Worker chết giữa chừng | Không có `consumed_at`; RabbitMQ giao lại message; worker mới bỏ qua testcase đã có kết quả và chạy tiếp phần còn lại. Nếu không ai nhận, sweep (JOB-JD-01, 5 phút) đưa về `SYSTEM_ERROR` với `WORKER_TIMEOUT` |
| Học viên thấy gì khi chờ | Trạng thái `PENDING` (nhãn "Đang chờ"), không có ước lượng thời gian vì không có nguồn dữ liệu (tiêu chí ponytail bậc 2). Khi worker nhận: `COMPILING`, rồi `JUDGING` kèm dải ô; từng ô tô màu khi bản tin `TESTCASE_FINISHED` tới. Nếu go-judge lỗi hạ tầng: nhãn lỗi hệ thống (`SYSTEM_ERROR`), không phải `WRONG_ANSWER`, và không tính là lỗi của học viên |
| Run Code (đồng bộ, endpoint #2) | Cùng timeout và thử lại ở trên, cộng **hạn tổng** 60 giây cho cả lượt; vượt hạn hoặc hết lượt thử lại thì trả `503 JDG-501`, học viên giữ nguyên mã và bấm chạy lại được (`USR0102` EVT-10 dòng 799). Không lưu gì |
| Adapter bất đồng bộ | Nếu `Judge0Adapter` cắm lại, mọi chi tiết webhook và token callback nằm trong adapter; chữ ký port không đổi (`02-bd/architecture/judge-orchestration.md:234-241`) |

## 4. GET /problems/{problemId}/execution-limits

### 4.1 Hợp đồng

| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `baseCategory = STUDENT` |
| Idempotent | có (đọc) |
| Tên BD | `GetEffectiveExecutionLimits` (`USR0102` Sheet 7.3 NO 5) |

### 4.2 Request

| Trường | Kiểu | Bắt buộc | Ràng buộc | Ghi chú |
| :--- | :--- | :-: | :--- | :--- |
| `problemId` | path UUID | Có | bài `PUBLISHED`, chưa xoá mềm | |
| `language` | query enum | Không | `JAVA`, `CPP`, `PYTHON` | Vắng thì trả cả ba. BD chỉ nói "ngôn ngữ đang chọn"; trả cả ba để một lần gọi đủ dựng cả ba tab, xem Q12 |

### 4.3 Response 200

| Trường | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `problemId` | UUID | |
| `limits[]` | array | một phần tử mỗi ngôn ngữ được hỏi |
| `limits[].language` | enum | |
| `limits[].enabled` | boolean | `language_configs.enabled`. Ngôn ngữ tắt thì giao diện không cho chọn cho bài nộp mới (`01-rd/req/judge-orchestration.md:74-83`) |
| `limits[].timeLimitMs` | int | `ROUND(problems.time_limit_ms * time_limit_multiplier)` |
| `limits[].memoryLimitMb` | int | `ROUND(problems.memory_limit_mb * memory_limit_multiplier)` |
| `limits[].maxOutputSizeKb` | int | `problems.max_output_size_kb` |

### 4.4 Lỗi

| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 404 | `JDG-101` | bài không tồn tại, chưa xuất bản hoặc đã ẩn mềm | Không phân biệt hai trường hợp (`USR0102` Sheet 9 NO 2 dòng 827) |
| 400 | `JDG-V03` | `language` không thuộc ba giá trị | |

### 4.5 Luồng xử lý

1. Gọi `ProblemGradingSpecPort.loadGradingSpec(problemId)`; không xuất bản thì `JDG-101`.
2. Đọc `language_configs` (ba dòng) trực tiếp từ DB, không cache dài hạn (`02-bd/security/judge-orchestration.md:77-80`).
3. Nhân hệ số, làm tròn nửa lên, trả về.
4. Lỗi của `problem-bank` thì trả lỗi; **riêng màn** hiển thị giới hạn cơ sở khi endpoint này lỗi (`USR0102` EVT-1 dòng 790), nên không có bản dự phòng ở máy chủ.

### 4.6 Truy cập dữ liệu

| Bảng hoặc cổng | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `judge.language_configs` | R | |
| `ProblemGradingSpecPort` | R | |

### 4.7 Test viewpoints

| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| T-EL-01 | Normal | Bài Java `time_limit_ms = 1000`, hệ số `2.00`: trả `timeLimitMs = 2000` |
| T-EL-02 | Boundary | Hệ số `0.10` và `9.99`: làm tròn đúng, không vượt kiểu số nguyên |
| T-EL-03 | Normal | Ngôn ngữ vừa bị tắt: `enabled = false`, giới hạn vẫn trả |
| T-EL-04 | Abnormal | Bài `DRAFT` hoặc đã ẩn mềm: `JDG-101` |
| T-EL-05 | Abnormal | `language=RUST`: `JDG-V03` |

## 5. POST /problems/{problemId}/run-sample

### 5.1 Hợp đồng

| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `baseCategory = STUDENT` |
| Idempotent | không (chạy tiêu tốn tài nguyên, nhưng không có tác dụng phụ lên dữ liệu) |
| Đồng bộ | có: chờ kết quả trong lời gọi (F4-02, `01-rd/req/judge-orchestration.md:16`) |
| Tên BD | `RunSampleTestcases` (`USR0102` Sheet 7.3 NO 6) |
| Không ghi | **không tạo dòng `submissions` nào**, không ghi tiến độ, không phát sự kiện miền (`database/judge-orchestration.md:46`, `USR0102` Q10) |

### 5.2 Request

| Trường | Kiểu | Bắt buộc | Ràng buộc | Ghi chú |
| :--- | :--- | :-: | :--- | :--- |
| `problemId` | path UUID | Có | bài `PUBLISHED` | |
| `language` | enum | Có | `JAVA`, `CPP`, `PYTHON`, và đang `enabled` | |
| `submissionModel` | enum | Có | `FUNCTION_WRAPPER`, `STANDARD_IO` | `FUNCTION_WRAPPER` chỉ hợp lệ khi bài có `functionWrapperSupported = true` |
| `sourceCode` | string | Có | không rỗng sau khi bỏ khoảng trắng; tối đa 65.536 byte UTF-8 | cùng ngưỡng với nộp bài (`USR0102` Q7, Sheet 9 NO 7, NO 8) |

### 5.3 Response 200

| Trường | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `outcome` | enum | `COMPLETED` hoặc `COMPILE_ERROR` |
| `compileErrorMessage` | string hoặc null | đã ánh xạ về dòng của học viên (F3-11) và lọc mã khung (F3-12); tối đa 10.000 ký tự; chỉ khác null khi `COMPILE_ERROR` |
| `passedCount` | int | số testcase Sample đạt |
| `totalCount` | int | số testcase Sample đã chạy |
| `results[]` | array | rỗng khi `COMPILE_ERROR`; theo thứ tự `position` của testcase Sample |
| `results[].testcaseId` | UUID | để client ghép với `ListSampleTestcases` của `problem-bank` lấy input và kết quả mong đợi (tránh lặp hợp đồng giữa hai module) |
| `results[].order` | int | thứ tự trong lượt chạy thử, từ 1 |
| `results[].verdict` | enum | `AC`, `WA`, `TLE`, `MLE`, `RE` |
| `results[].runtimeMs` | int | |
| `results[].memoryKb` | int | |
| `results[].actualOutput` | string | output thực tế, cắt 4.096 ký tự. Được phép vì chỉ chạy testcase `SAMPLE` |
| `results[].stderrSnippet` | string hoặc null | chỉ khác null khi `sandbox_settings.return_stderr_to_student = true` và `verdict != AC`; cắt 4.096 ký tự |

### 5.4 Lỗi

| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 404 | `JDG-101` | bài không mở được | |
| 422 | `JDG-102` | ngôn ngữ đang tắt | |
| 422 | `JDG-103` | `FUNCTION_WRAPPER` trên bài chỉ hỗ trợ Standard I/O | máy chủ kiểm dù giao diện đã ẩn nút (F3-13, `USR0102` Sheet 9 NO 9) |
| 422 | `JDG-105` | bài không có testcase Sample đã duyệt | |
| 400 | `JDG-V01`, `JDG-V02`, `JDG-V03`, `JDG-V04` | mã rỗng, quá 64 KB, ngôn ngữ hoặc mô hình sai | |
| 429 | `JDG-502` | cụm đang nhận quá số lượt chạy thử đồng thời cho phép | bảo vệ luồng chấm; xem Q5 |
| 503 | `JDG-501` | go-judge không dùng được sau thử lại, hoặc vượt hạn tổng 60 giây | |

### 5.5 Luồng xử lý

1. Kiểm đầu vào (`JDG-V01`..`V04`), kiểm quyền STUDENT.
2. `loadGradingSpec` (bài xuất bản, `functionWrapperSupported`, giới hạn) và `loadSampleTestcases`; **điều kiện `visibility = SAMPLE` đặt ngay trong truy vấn của `problem-bank`, không lọc sau** (`USR0102` Sheet 9 NO 4 dòng 829).
3. Xin một "suất chạy thử" từ bộ đếm đồng thời toàn cục (tối đa `judge.run-sample.max-concurrent` = 8 `[SoT: Suy luận]`); hết suất thì `JDG-502`.
4. Đọc cấu hình lúc này: `language_configs`, `grading_defaults.compile_timeout_sec`, `sandbox_settings` (mỗi lượt đọc một lần).
5. `PrepareSubmissionUseCase.prepare` (truyền `expectedSpecVersion` lấy từ `loadGradingSpec`), rồi `JudgeExecutionPort.compile`. Lỗi biên dịch: `MapCompileErrorUseCase.map`, dựng thông báo (BR-JD-03 bước 8), trả `outcome = COMPILE_ERROR`.
6. Với từng testcase Sample, tuần tự: `loadTestcaseData`, `run`, ánh xạ trạng thái engine sang verdict (BR-JD-04 bước 7), `MatchOutputUseCase.match` nếu chạy xong bình thường; `actualOutput` trả về lấy từ `MatchResult.actualOutput` (đã tách dòng đánh dấu kết quả).
7. Trả kết quả. Không ghi DB, không phát bản tin STOMP.
8. Trả suất chạy thử về bộ đếm trong khối `finally`.

### 5.6 Truy cập dữ liệu

| Bảng hoặc cổng | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `judge.language_configs`, `judge.grading_defaults`, `judge.sandbox_settings` | R | |
| `ProblemGradingSpecPort`, cổng vào của `harness`, `JudgeExecutionPort` | gọi | |
| `judge.submissions` | không chạm | |

### 5.7 Test viewpoints

| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| T-RS-01 | Normal | Lời giải đúng: `outcome = COMPLETED`, mọi Sample `AC`, `passedCount = totalCount`; bảng `submissions` không đổi số dòng |
| T-RS-02 | Normal | Một Sample sai: `WA` kèm `actualOutput`; các Sample còn lại vẫn chạy hết |
| T-RS-03 | Abnormal | Lỗi biên dịch: `COMPILE_ERROR`, `results` rỗng, số dòng trong thông báo là dòng của học viên |
| T-RS-04 | Abnormal | Gọi bằng `FUNCTION_WRAPPER` trên bài Standard I/O: `JDG-103` |
| T-RS-05 | Boundary | Mã đúng 65.536 byte chạy được; 65.537 byte `JDG-V02`. Mã toàn ký tự tiếng Việt 2 byte tính theo byte |
| T-RS-06 | Abnormal | go-judge tắt: `JDG-501` sau thử lại, mã của học viên không mất, không có dòng nào được ghi |
| T-RS-07 | Security | Phản hồi không chứa input hay expected output của testcase `HIDDEN` dù bài có testcase ẩn |
| T-RS-08 | Boundary | Vượt 8 lượt chạy thử đồng thời: lượt thứ 9 nhận `JDG-502` |

## 6. POST /problems/{problemId}/submissions

### 6.1 Hợp đồng

| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `baseCategory = STUDENT` |
| Idempotent | **không**, mỗi lần gọi tạo một bài nộp mới. Giao diện không tự nộp lại khi lỗi mạng (`USR0102` EVT-11 dòng 800) |
| Không chờ chấm | trả `PENDING` ngay (F4-01, `01-rd/req/judge-orchestration.md:10-11`) |
| Tên BD | `CreateSubmission` (`USR0102` Sheet 7.3 NO 7) |

### 6.2 Request

| Trường | Kiểu | Bắt buộc | Ràng buộc | Ghi chú |
| :--- | :--- | :-: | :--- | :--- |
| `problemId` | path UUID | Có | bài `PUBLISHED`, chưa xoá mềm | |
| `language` | enum | Có | `JAVA`, `CPP`, `PYTHON`; phải `enabled = true` ở `language_configs` | |
| `submissionModel` | enum | Có | `FUNCTION_WRAPPER`, `STANDARD_IO` | `FUNCTION_WRAPPER` bị từ chối khi `functionWrapperSupported = false` (F3-13) |
| `sourceCode` | string | Có | không rỗng sau khi bỏ khoảng trắng; **tối đa 65.536 byte UTF-8** (`octet_length`) | cùng ngưỡng cho gõ trực tiếp và tải file (`USR0102` Q7 dòng 863) |
| `sourceInputMethod` | enum | Có | `EDITOR`, `FILE_UPLOAD` | chỉ ghi nhận nguồn, không đổi luồng (`database/judge-orchestration.md:25`) |
| `fileName` | string | Có khi `FILE_UPLOAD` | đuôi `.java` cho `JAVA`, `.cpp` cho `CPP`, `.py` cho `PYTHON` | để máy chủ kiểm đuôi file như `USR0102` Sheet 9 NO 5 dòng 830 ("máy chủ khi nhận nội dung"); BD không nêu trường này, xem Q9 |

### 6.3 Response 201 (`SubmissionTicketDto`)

| Trường | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `submissionId` | UUID | |
| `status` | enum | luôn `PENDING` |
| `language`, `submissionModel` | enum | |
| `submittedAt` | ISO-8601 | = `submissions.submitted_at` |
| `totalCountHint` | int hoặc null | số testcase đã duyệt tại thời điểm nộp, **chỉ để dựng dải ô chờ** (`USR0102` EVT-11 "dải ô dựng sẵn đủ số testcase"). Con số chính thức là `totalCount` trong bản tin `STATUS_CHANGED` khi vào `JUDGING`, vì `total_testcase_count` ở DB là `NULL` cho tới lúc bắt đầu chấm (`database/judge-orchestration.md:30`). `[SoT: Suy luận]`, xem Q8 |

### 6.4 Lỗi

| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 404 | `JDG-101` | bài không mở được | |
| 422 | `JDG-102` | ngôn ngữ đang tắt | luôn còn ít nhất một ngôn ngữ bật |
| 422 | `JDG-103` | mô hình Bọc hàm trên bài chỉ hỗ trợ Standard I/O | |
| 422 | `JDG-104` | bài không có testcase nào đã duyệt để chấm | phòng thủ; bài đã xuất bản thì có ít nhất 8 (`database/problem-bank.md:161`) |
| 429 | `JDG-106` | số lượt nộp của người này cho bài này trong **một giờ gần nhất** đã đạt giới hạn | `details`: `{ "limit": 60, "windowMinutes": 60 }` |
| 400 | `JDG-V01` | mã chỉ gồm khoảng trắng | |
| 400 | `JDG-V02` | mã quá 65.536 byte UTF-8 | |
| 400 | `JDG-V03`, `JDG-V04`, `JDG-V05` | `language`, `submissionModel`, `sourceInputMethod` sai | |
| 400 | `JDG-V06` | `FILE_UPLOAD` thiếu `fileName` hoặc đuôi không khớp ngôn ngữ | |
| 503 | `JDG-503` | hàng đợi RabbitMQ không nhận được message sau 3 lần thử | xem luồng bước 8 và Q4 |

### 6.5 Luồng xử lý

1. Kiểm đầu vào (`JDG-V01`..`V06`) và quyền.
2. `loadGradingSpec`: bài xuất bản, `functionWrapperSupported`, `maxSubmissionsPerHour`, số testcase đã duyệt. Lỗi thì `JDG-101` hoặc `JDG-104`.
3. Kiểm ngôn ngữ `enabled` (`JDG-102`) và mô hình (`JDG-103`).
4. **Trong một giao dịch**: lấy khoá tư vấn `pg_advisory_xact_lock(hashtextextended(userId || ':' || problemId, 0))` để hai lượt nộp đồng thời không cùng vượt giới hạn (`[SoT: Suy luận]`, kiểm rồi ghi phải nguyên tử).
5. Đếm: `SELECT count(*) FROM judge.submissions WHERE user_id = :u AND problem_id = :p AND submitted_at > now() - interval '1 hour'` (chỉ mục `submissions(user_id, problem_id, submitted_at DESC)`). Giới hạn = `maxSubmissionsPerHour` của bài; nếu `NULL` thì dùng mặc định hệ thống **60** (hằng số `judge.submission.default-hourly-limit`, `database/problem-bank.md:295`). Đạt giới hạn thì `JDG-106` (`USR0102` Sheet 9 NO 10 dòng 835).
6. `INSERT judge.submissions` với `status = PENDING`, `submitted_at = now()`, `updated_at = now()`, `passed_testcase_count = 0`, `total_testcase_count = NULL`, `source_code`, `source_input_method`. Commit.
7. Sau commit, **publish** message `{ "submissionId": "<uuid>" }` lên hàng đợi `judge.submissions` (RabbitMQ) với publisher confirm, chờ tối đa 3 giây, tối đa 3 lần thử (200 ms, 500 ms). Message **không mang mã nguồn**: worker đọc từ DB (tránh đưa dữ liệu người dùng vào broker, và để mã đọc luôn là bản đã lưu).
8. Publish thất bại sau 3 lần: ghi log `ERROR`, trả `503 JDG-503`. Dòng `submissions` đã commit vẫn ở `PENDING` và sẽ được sweep đưa về `SYSTEM_ERROR` sau 5 phút (hậu quả của hai lần ghi không chung giao dịch, xem Q4).
9. Trả `201` với `SubmissionTicketDto`. Khi `queue_paused = true` vẫn nhận bài nộp: bài nằm chờ, nhãn "Đang chờ" (`ADM0401` EVT-7 dòng 644).

### 6.6 Truy cập dữ liệu

| Bảng hoặc cổng | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `judge.submissions` | C, R (đếm) | |
| `judge.language_configs` | R | |
| `ProblemGradingSpecPort` | R | |
| RabbitMQ | publish | hàng đợi `judge.submissions` |

### 6.7 Test viewpoints

| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| T-CS-01 | Normal | Nộp hợp lệ: nhận `PENDING` và `submissionId` ngay, không chờ chấm; dòng `submissions` có `total_testcase_count = NULL` |
| T-CS-02 | Boundary | 59 lượt trong giờ vừa qua: lượt 60 được nhận; lượt 61 `JDG-106` |
| T-CS-03 | Boundary | `max_submissions_per_hour = NULL`: áp 60; `= 5`: áp 5 |
| T-CS-04 | Boundary | Mã 65.536 byte nhận; 65.537 byte `JDG-V02`; khoảng trắng toàn bộ `JDG-V01` |
| T-CS-05 | Abnormal | Ngôn ngữ vừa bị tắt: `JDG-102`; bài đã vào hàng đợi của ngôn ngữ đó trước khi tắt vẫn chạy nốt (BR-JD-09) |
| T-CS-06 | Abnormal | `FUNCTION_WRAPPER` trên bài Standard I/O dù sửa tay tham số: `JDG-103` |
| T-CS-07 | Concurrency | Hai lượt nộp đồng thời khi còn đúng một suất: đúng một lượt thành công |
| T-CS-08 | Abnormal | RabbitMQ tắt: `JDG-503`; dòng ở `PENDING`; sweep đưa về `SYSTEM_ERROR` sau 5 phút |
| T-CS-09 | Normal | `FILE_UPLOAD` với `Main.py` cho ngôn ngữ `PYTHON`: nhận, `source_input_method = FILE_UPLOAD`; `Main.java` cho `PYTHON`: `JDG-V06` |

## 7. GET /problems/{problemId}/submissions/mine

### 7.1 Hợp đồng

| Mục | Giá trị |
| :--- | :--- |
| Xác thực, quyền | JWT, STUDENT |
| Idempotent | có |
| Tên BD | `ListMySubmissionsForProblem` (`USR0102` Sheet 7.3 NO 9); phạm vi khoá cứng theo người gọi |

### 7.2 Request

| Trường | Kiểu | Bắt buộc | Ràng buộc | Ghi chú |
| :--- | :--- | :-: | :--- | :--- |
| `problemId` | path UUID | Có | | không kiểm bài còn xuất bản: lịch sử của chính mình vẫn xem được |
| `limit` | query int | Không | 1 đến 10, mặc định 10 | "10 lượt nộp gần nhất" (`USR0102` dòng 305-308, `[SoT: Suy luận]` ở BD) |

### 7.3 Response 200 (`MySubmissionSummaryDto[]`)

`{ "items": [ { "submissionId", "language", "status", "passedCount", "totalCount", "scoreRatio", "runtimeMs", "memoryKb", "submittedAt" } ] }`, sắp `submitted_at` giảm dần. `passedCount`, `totalCount`, `scoreRatio` là `null` khi `total_testcase_count` còn `NULL` (chưa chấm), để giao diện hiển thị `-` thay vì `0/0` (`database/judge-orchestration.md:29`). `runtimeMs`, `memoryKb` là `null` khi chưa chấm xong hoặc `COMPILE_ERROR` (`USR0102` Sheet 5 dòng 505).

### 7.4 Lỗi

| HTTP | error.code | Khi nào |
| :-: | :--- | :--- |
| 400 | `JDG-V14` | `limit` ngoài khoảng |

### 7.5 Luồng xử lý

`SELECT ... FROM judge.submissions WHERE user_id = :tokenUserId AND problem_id = :problemId ORDER BY submitted_at DESC LIMIT :limit` (chỉ mục `(user_id, problem_id, submitted_at DESC)`).

### 7.6 Truy cập dữ liệu

`judge.submissions` (R).

### 7.7 Test viewpoints

| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| T-LP-01 | Normal | 12 lượt nộp: trả 10 mới nhất theo thứ tự giảm dần |
| T-LP-02 | Security | Người B không thấy lượt nộp của người A dù cùng bài |
| T-LP-03 | Boundary | Lượt đang `PENDING`: các số đếm `null`, không phải `0` |
| T-LP-04 | Abnormal | `limit=11`: `JDG-V14` |

## 8. GET /submissions/{submissionId}

### 8.1 Hợp đồng

| Mục | Giá trị |
| :--- | :--- |
| Xác thực, quyền | JWT, `baseCategory = STUDENT`, **chỉ chủ bài nộp** |
| Idempotent | có |
| Tên BD | `GetSubmissionDetail` (`USR0201` Sheet 7.3 NO 1) |

### 8.2 Request

| Trường | Kiểu | Bắt buộc | Ràng buộc |
| :--- | :--- | :-: | :--- |
| `submissionId` | path UUID | Có | |

### 8.3 Response 200 (`SubmissionDetailDto`)

| Trường | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `submissionId`, `status`, `language`, `submissionModel`, `submittedAt`, `finishedAt` | | `finishedAt` null khi chưa cuối |
| `problem` | object hoặc null | `{ problemId, code, title, slug, levelDisplayName }` từ `ProblemSummaryReadPort`; `null` nếu `problem-bank` lỗi (màn vẫn hiển thị phần còn lại) |
| `sourceCode` | string | mã đã nộp |
| `compileErrorMessage` | string hoặc null | chỉ khác null khi `COMPILE_ERROR`; đã ánh xạ dòng, lọc mã khung |
| `passedCount`, `totalCount`, `scoreRatio` | int, int, decimal | `null` khi `COMPILE_ERROR`, `SYSTEM_ERROR` hoặc chưa chấm; `scoreRatio` thang 0 đến 1, ba chữ số thập phân (F4-13) |
| `runtimeMs`, `memoryKb` | int hoặc null | `MAX` trên các testcase đã chạy |
| `beatsPercent` | int hoặc null | chỉ khác null khi `status = ACCEPTED` và `beatsSampleSize >= 10` (BR-JD-12) |
| `beatsSampleSize` | int hoặc null | số bài `ACCEPTED` cùng bài và ngôn ngữ, kể cả bài này; null khi không `ACCEPTED` |
| `testcaseDetail` | object | xem dưới |

`testcaseDetail`:

| Trường | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `available` | boolean | `false` khi bài nộp cũ hơn 90 ngày (`submitted_at < now() - 90 ngày`), khi `COMPILE_ERROR` hoặc khi `SYSTEM_ERROR` trước khi chạy testcase |
| `unavailableReason` | enum hoặc null | `RETENTION_EXPIRED` (quá 90 ngày), `NOT_APPLICABLE` (không có testcase nào chạy), null khi `available` |
| `items[]` | array | rỗng khi `available = false`; theo `testcase_order` tăng dần |
| `items[].order` | int | |
| `items[].visibility` | enum | `SAMPLE` hoặc `HIDDEN`; tra qua `problem-bank` tại lúc đọc, **không tra được thì coi là `HIDDEN`** (an toàn, xem Q7) |
| `items[].testcaseId` | UUID hoặc null | **chỉ có với testcase `SAMPLE`**, để ghép với `GetProblemSampleTestcases`; `null` với `HIDDEN` |
| `items[].verdict` | enum | |
| `items[].runtimeMs`, `items[].memoryKb` | int | |
| `items[].actualOutput` | string hoặc null | **chỉ `SAMPLE`**, cột `actual_output_snippet` (lưu tối đa 4.096 ký tự; màn hiển thị 2.000, `USR0201` Sheet 5 NO 10 dòng 315) |
| `items[].stderrSnippet` | string hoặc null | chỉ khi `sandbox_settings.return_stderr_to_student = true`, `verdict != AC` **và testcase `SAMPLE`**; xem Q6 |

Dạng khi hết hạn lưu giữ (90 ngày):

```json
{ "submissionId": "b1f1...", "status": "WRONG_ANSWER", "passedCount": 7, "totalCount": 12, "scoreRatio": 0.583,
  "testcaseDetail": { "available": false, "unavailableReason": "RETENTION_EXPIRED", "items": [] } }
```

Các số tổng (`passedCount`, `totalCount`, `scoreRatio`, `runtimeMs`, `memoryKb`, `status`) và `sourceCode` vẫn còn vì chúng nằm ở `submissions`, không bị job dọn (`database/judge-orchestration.md:68-72`).

### 8.4 Lỗi

| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 404 | `JDG-110` | không có bài nộp, hoặc bài nộp của người khác | **cùng một mã và cùng thông điệp**, không lộ sự tồn tại (chống IDOR, `USR0201` Sheet 9 NO 2 dòng 469) |

### 8.5 Luồng xử lý

1. `SELECT ... FROM judge.submissions WHERE id = :id AND user_id = :tokenUserId`; không có thì `JDG-110`.
2. Nếu chưa hết hạn lưu giữ và có testcase: đọc `submission_testcase_results` theo `testcase_order`; gọi `ProblemGradingSpecPort` (hoặc cổng đọc `visibility` theo `testcaseId`) lấy `visibility` hiện tại.
3. Dựng từng dòng theo BR-JD-11 (khoá kép cho testcase ẩn).
4. Nếu `ACCEPTED`: tính Beats (BR-JD-12). Ghép `problem` từ `ProblemSummaryReadPort` (lỗi thì `null`).

### 8.6 Truy cập dữ liệu

| Bảng hoặc cổng | Thao tác |
| :--- | :--- |
| `judge.submissions`, `judge.submission_testcase_results` | R |
| `ProblemSummaryReadPort`, `ProblemGradingSpecPort` | R |

### 8.7 Test viewpoints

| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| T-GD-01 | Normal | Bài `ACCEPTED` 12/12: đủ 12 dòng testcase, `beatsPercent` đúng công thức khi đủ 10 mẫu |
| T-GD-02 | Security | Bài `WRONG_ANSWER` fail ở testcase `HIDDEN` thứ 7: dòng 7 chỉ có `order`, `verdict`, `runtimeMs`, `memoryKb`; không có `actualOutput`, `testcaseId`, input, expected |
| T-GD-03 | Security | Gọi bằng token của người khác: `JDG-110`, thân giống hệt id không tồn tại |
| T-GD-04 | Boundary | Bài nộp đúng ngày thứ 90 còn chi tiết; ngày 91 trả `available=false`, `RETENTION_EXPIRED`, `items=[]`, các số tổng còn |
| T-GD-05 | Normal | `COMPILE_ERROR`: `testcaseDetail.available=false`, `NOT_APPLICABLE`, `compileErrorMessage` có giá trị, `scoreRatio=null` |
| T-GD-06 | Abnormal | `problem-bank` lỗi: `problem=null`, phần còn lại vẫn trả |
| T-GD-07 | Security | Testcase đã bị đổi từ `HIDDEN` sang `SAMPLE` sau khi chấm: `actualOutput` vẫn `null` vì cột chỉ ghi cho `SAMPLE` lúc chấm |
| T-GD-08 | Boundary | Dưới 10 bài `ACCEPTED` cùng bài, ngôn ngữ: `beatsPercent=null`, `beatsSampleSize<10` |

## 9. GET /submissions/{submissionId}/progress

### 9.1 Hợp đồng

| Mục | Giá trị |
| :--- | :--- |
| Xác thực, quyền | JWT, STUDENT, chỉ chủ bài nộp |
| Idempotent | có |
| Mục đích | ảnh chụp tiến trình để (a) nối lại kênh realtime, (b) làm đường REST dự phòng (mục 2.5). `[SoT: Suy luận]`: BD nêu đường "Polling REST mỗi 3 giây" (`USR0201` Sheet 9 NO 5) nhưng không đặt tên endpoint |

### 9.2 Response 200 (`SubmissionProgressDto`)

| Trường | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `submissionId` | UUID | |
| `status` | enum | |
| `seq` | int | quy tắc mục 2.4: `PENDING`=0, `COMPILING`=1, `JUDGING`=`2+completedCount`, cuối=`3+N` hoặc 3 |
| `isFinal` | boolean | |
| `totalCount` | int hoặc null | null trước khi vào `JUDGING` |
| `completedCount`, `passedCount` | int | đếm từ `submission_testcase_results` |
| `testcases[]` | array | `{ order, verdict }` của các testcase đã xong; **không có trường nào khác** (cùng giới hạn bản tin) |
| `scoreRatio`, `firstFailingOrder` | decimal, int | chỉ khi `isFinal` và không phải `COMPILE_ERROR`, `SYSTEM_ERROR` |

### 9.3 Lỗi

`404 JDG-110` (không có hoặc không phải của mình).

### 9.4 Luồng xử lý và dữ liệu

Một truy vấn `submissions` kèm một truy vấn `submission_testcase_results` (`submission_id, testcase_order`). Không gọi `problem-bank`. Với bài nộp quá 90 ngày, `testcases = []` và `isFinal = true` (không có chi tiết).

### 9.5 Test viewpoints

| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| T-PG-01 | Normal | Giữa lúc `JUDGING` đã xong 4/12: trả `completedCount=4`, `seq=6`, bốn phần tử `testcases` |
| T-PG-02 | Security | Chỉ `order` và `verdict` trong `testcases`; không runtime, không output |
| T-PG-03 | Security | Token người khác: `JDG-110` |
| T-PG-04 | Normal | Nối lại: ảnh chụp có `seq` bằng `seq` của bản tin cuối đã nhận được thì client không áp trùng |

## 10. GET /me/submissions

### 10.1 Hợp đồng

Xác thực JWT; quyền STUDENT; idempotent. Tên BD `ListMySubmissions` (`USR0202` Sheet 7.3 NO 1). Chỉ đọc bài nộp của người trong token (`USR0202` Sheet 9 NO 2 dòng 374).

### 10.2 Request (query)

| Trường | Kiểu | Bắt buộc | Ràng buộc | Ghi chú |
| :--- | :--- | :-: | :--- | :--- |
| `q` | string | Không | bỏ khoảng trắng hai đầu; cắt ở 100 ký tự | tìm theo mã hoặc tên bài |
| `verdict` | enum | Không | `ALL` (mặc định), `ACCEPTED`, `WRONG_ANSWER`, `TIME_LIMIT_EXCEEDED`, `COMPILE_ERROR`, `RUNTIME_ERROR` | Giá trị lạ thì đưa về `ALL`, không báo lỗi (`USR0202` Sheet 9 NO 4 dòng 376). Bài `PENDING`, `COMPILING`, `JUDGING`, `SYSTEM_ERROR` chỉ hiện ở `ALL` |
| `language` | enum | Không | `ALL` (mặc định), `JAVA`, `CPP`, `PYTHON` | Giá trị lạ về `ALL` |
| `page` | int | Không | mặc định 1 | |

### 10.3 Response 200 (`StudentSubmissionPageResponseDto`)

Dạng phân trang chung (mục 1.2). Mỗi `items[]` (`StudentSubmissionItemDto`): `submissionId`, `submittedAt`, `problem` (`{ problemId, code, title, slug, levelDisplayName }`, null nếu không ghép được), `language`, `status`, `passedCount`, `totalCount`, `runtimeMs`. Sắp `submitted_at` giảm dần (`USR0202` Sheet 7.2 dòng 332). Không có `sourceCode`, không có `testcaseDetail`. `canReview` không là trường: nút Phân tích suy ra từ `status = ACCEPTED` ở giao diện.

### 10.4 Lỗi

Không có lỗi nghiệp vụ riêng (tham số lạ được chuẩn hoá). `401` mã chung.

### 10.5 Luồng xử lý

1. Nếu có `q`: `ProblemSummaryReadPort.searchProblemIds(q, 500)` lấy tập `problemId` khớp mã hoặc tên bài (`[SoT: Suy luận]` trần 500 để truy vấn không phình); rỗng thì trả danh sách rỗng.
2. `SELECT ... FROM judge.submissions WHERE user_id = :tokenUserId [AND status IN (...)] [AND language = ...] [AND problem_id = ANY(:ids)] ORDER BY submitted_at DESC OFFSET/LIMIT` (chỉ mục `(user_id, submitted_at DESC)`); đếm tổng.
3. `ProblemSummaryReadPort.getSummaries(problemIds của trang)` ghép tên, mã, độ khó. Lỗi cổng: `problem = null`, vẫn trả danh sách.

### 10.6 Truy cập dữ liệu

`judge.submissions` (R); `ProblemSummaryReadPort` (R).

### 10.7 Test viewpoints

| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| T-MS-01 | Normal | 45 lượt nộp: trang 1 có 20 dòng, `totalCount=45`, trang 3 có 5 dòng |
| T-MS-02 | Normal | Lọc `verdict=ACCEPTED` và `language=PYTHON`: chỉ dòng khớp cả hai |
| T-MS-03 | Abnormal | `verdict=FOO`: xử như `ALL`, không lỗi |
| T-MS-04 | Boundary | `page=99`: về trang cuối hợp lệ |
| T-MS-05 | Security | Không có tham số nào chọn người khác; dữ liệu người khác không xuất hiện |
| T-MS-06 | Normal | `q=two` khớp theo tên bài, không phân biệt hoa thường |

## 11. GET /me/in-progress-problems

### 11.1 Hợp đồng

Xác thực JWT; quyền STUDENT; idempotent. Tên BD `ListMyUnsolvedRecentProblems` (`USR0101` Sheet 7.3 NO 6, `USR0105` Sheet 7.3 NO 1). Định nghĩa "đang làm dở" = **bài nộp gần nhất của chính người học cho bài đó mà chưa `ACCEPTED`** (`USR0101` Q4 dòng 647), không có bảng nháp.

### 11.2 Request và response

| Tham số | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `limit` | query int | 1 đến 20, mặc định 3. "Tối đa 3" là yêu cầu của `USR0101`; `USR0105` Q2 (dòng 447) chưa chốt trần cho màn đó, nên DD để tham số và đặt trần 20 `[SoT: Suy luận]` |

Response `200`: `{ "items": [ { "problemId", "problem": {code,title,slug,levelDisplayName}, "latestSubmissionId", "language", "status", "passedCount", "totalCount", "submittedAt" } ] }`, sắp theo `submittedAt` của bài nộp gần nhất giảm dần. `problem` null nếu `problem-bank` lỗi (giao diện bỏ thẻ).

### 11.3 Luồng xử lý và dữ liệu

`SELECT DISTINCT ON (problem_id) ... FROM judge.submissions WHERE user_id = :u ORDER BY problem_id, submitted_at DESC` rồi lọc `status <> 'ACCEPTED'`, sắp `submitted_at DESC`, cắt `limit` (chỉ mục `(user_id, problem_id, submitted_at DESC)`). Trạng thái chưa cuối và `SYSTEM_ERROR` cũng được tính là "chưa `ACCEPTED`" theo đúng chữ của định nghĩa.

### 11.4 Lỗi

`400 JDG-V14` khi `limit` ngoài khoảng.

### 11.5 Test viewpoints

| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| T-IP-01 | Normal | Bài A nộp sai rồi nộp `ACCEPTED`: không còn trong danh sách |
| T-IP-02 | Normal | Bài B nộp `ACCEPTED` rồi nộp lại sai: bài nộp gần nhất không `ACCEPTED` nên xuất hiện (đúng chữ định nghĩa, xem Q13) |
| T-IP-03 | Boundary | Mặc định trả tối đa 3; `limit=20` trả tối đa 20; `limit=21` lỗi |
| T-IP-04 | Security | Chỉ bài của người trong token |

## 12. GET /me/metrics

### 12.1 Hợp đồng

Xác thực JWT; quyền STUDENT; idempotent. Tên BD `GetMySubmissionMetrics` (cổng người học của `DEC-2026-0927-submission-metrics-two-ports`, `02-bd/architecture/judge-orchestration.md:91-113`). **Chữ ký không có chỗ truyền danh tính**: `userId` lấy từ token, chống IDOR là thuộc tính của chữ ký chứ không phải quy ước (dòng 117-122). Cùng một use case phục vụ hai đường: endpoint này (khi giao diện gọi thẳng) và cổng trong tiến trình `MySubmissionMetricsPort` của `identity` khi `identity` ghép thành `MyProgressOverviewDto` (`USR0501` dòng 476; `USR0601` dòng 764).

### 12.2 Request (query)

| Trường | Kiểu | Bắt buộc | Ràng buộc | Ghi chú |
| :--- | :--- | :-: | :--- | :--- |
| `range` | enum | Có | `7d`, `30d`, `all` | giá trị khác: `400 JDG-V07` (`USR0501` Sheet 9 NO 3 dòng 574) |
| `topicIds` | query UUID, lặp | Không | tối đa 50 `[SoT: Suy luận]` | tập chủ đề cần tỉ lệ AC; vắng thì bỏ qua khối `acRateByTopic` |

Tham số `dayCount = 364` ghi ở BD kiến trúc (dòng 132-136) cho lưới hoạt động 12 tháng **không còn dùng**: `DEC-2026-1022-db-sync-round2` mục 3 chuyển lưới sang `identity` (`03-dd/api/identity.md:169-171`). Biểu đồ theo ngày của `USR0601` khu vực C vẫn đọc endpoint này.

### 12.3 Response 200

| Trường | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `streakDays` | int | số ngày liên tiếp, tính tới hôm nay (UTC+7), có ít nhất một lượt nộp (RD `USR0501` Q1 dòng 75). Hôm nay chưa nộp thì 0 theo đúng chữ định nghĩa, xem Q14 |
| `lastSubmittedAt` | ISO-8601 hoặc null | |
| `dailyCounts[]` | array | `{ day, count }`; **đủ phần tử, ngày không có lượt nộp là `count = 0`** (`USR0501` Sheet 7.1 NO 14 dòng 485). `7d`: 7 phần tử mỗi ngày; `30d`: 14 phần tử (quy tắc gộp ở BR-JD-13); `all`: một phần tử mỗi tuần ISO (thứ Hai là ngày đầu) từ tuần có lượt nộp đầu tiên tới tuần hiện tại, **gồm cả tuần đang chạy**. `day` là ngày cuối của nhóm theo UTC+7 |
| `acRateByTopic[]` | array | chỉ khi có `topicIds`: `{ topicId, submissionCount, acceptedCount, acRate, lastSubmittedAt }`; `acRate = acceptedCount / submissionCount`, null khi `submissionCount = 0`. Giới hạn trong cửa sổ `range` cho `submissionCount`, `acceptedCount`, `lastSubmittedAt` (`USR0501` V0.7 dòng 73: bộ lọc khoảng thời gian áp cho biểu đồ, cột "Tỉ lệ AC" và "Lần cuối") |

Mẫu số của tỉ lệ là mọi bài nộp ở **trạng thái cuối** (cả sáu, kể cả `SYSTEM_ERROR`), cùng tập mà sự kiện `SubmissionGraded` gửi cho `identity` đếm (mục 25), để hai module không lệch số. Xem Q14.

### 12.4 Lỗi

`400 JDG-V07` (`range`), `400 JDG-V15` (`topicIds` sai định dạng hoặc quá 50).

### 12.5 Luồng xử lý và dữ liệu

BR-JD-13. `ProblemTopicReadPort.getProblemIdsByTopics(topicIds)` rồi truy vấn `judge.submissions` theo `user_id = :tokenUserId`, `problem_id = ANY(...)`, `submitted_at` trong cửa sổ, đổi ngày bằng `AT TIME ZONE` +07:00. Chỉ mục `(user_id, submitted_at DESC)`.

### 12.6 Test viewpoints

| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| T-MM-01 | Normal | `range=7d`: đúng 7 phần tử, ngày trống `count=0` |
| T-MM-02 | Boundary | Lượt nộp lúc 23:30 UTC+7 tính vào đúng ngày UTC+7, không phải ngày UTC |
| T-MM-03 | Normal | `range=30d`: đúng 14 phần tử, tổng bằng số lượt nộp trong 30 ngày |
| T-MM-04 | Normal | `range=all`: gom theo tuần ISO, tuần đang chạy có mặt |
| T-MM-05 | Abnormal | `range=1y`: `JDG-V07` |
| T-MM-06 | Security | Không có tham số `userId`; thêm `?userId=` bị bỏ qua và dữ liệu vẫn là của người trong token |
| T-MM-07 | Boundary | Chưa nộp bài nào: `streakDays=0`, `lastSubmittedAt=null`, `dailyCounts` toàn 0 |

## 13. GET /instructor/submissions

### 13.1 Hợp đồng

| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `CLASS_MANAGEMENT:READ` **và** phạm vi lớp phụ trách (`INS0206` dòng 222) |
| Idempotent | có |
| Tên BD | `ListInstructorSubmissions` (`INS0206` Sheet 7.3 NO 1) |
| Chỉ đọc | không ghi gì |

### 13.2 Request (query)

| Trường | Kiểu | Bắt buộc | Ràng buộc | Ghi chú |
| :--- | :--- | :-: | :--- | :--- |
| `classId` | UUID | Không | phải là lớp do người gọi phụ trách | sai định dạng hoặc không phải lớp của mình: `404 JDG-111` (`INS0206` Sheet 9 NO 2, NO 5) |
| `result` | enum | Không | `all` (mặc định), `accepted`, `failed` | lạ thì đưa về `all` |
| `q` | string | Không | bỏ khoảng trắng hai đầu, tối đa 100 ký tự | khớp tên học viên hoặc tên bài |
| `page` | int | Không | | trang 20 dòng cố định |

### 13.3 Response 200

Dạng phân trang chung. `items[]` (`SubmissionListItemDto`): `submissionId`, `studentId`, `studentName`, `classId`, `className`, `problemId`, `problemTitle`, `language`, `status`, `passedCount`, `totalCount` (cả hai null khi `scoreRatio` rỗng), `submittedAt`, `leftClass` (`INS0206` Sheet 7.1 dòng 457-469). **Không** có `sourceCode`, `compileErrorMessage`, nội dung testcase hay dòng `submission_testcase_results` (`INS0206` Sheet 9 NO 4 dòng 545). Sắp `submitted_at` giảm dần, cố định.

Phân loại `result`: `accepted` = `ACCEPTED`; `failed` = trạng thái cuối khác `ACCEPTED` và khác `SYSTEM_ERROR` (`INS0206` Sheet 9 NO 10 dòng 551); `PENDING`, `COMPILING`, `JUDGING`, `SYSTEM_ERROR` chỉ hiện ở `all`.

### 13.4 Lỗi

| HTTP | error.code | Khi nào |
| :-: | :--- | :--- |
| 403 | `JDG-301` | thiếu `CLASS_MANAGEMENT:READ` |
| 404 | `JDG-111` | `classId` không phải lớp của người gọi, hoặc sai định dạng |

### 13.5 Luồng xử lý

BR-JD-10: `ClassScopeReadPort.getInstructorScope`, `ClassAssignmentReadPort.listAssignments`, rồi một truy vấn trên `judge.submissions` với ba điều kiện phạm vi (học viên thuộc lớp do mình phụ trách; bài đã giao cho lớp, kể cả gỡ mềm; thời điểm nộp trong cửa sổ `joined_at..left_at`) và quy tắc chọn một lớp cho mỗi bài nộp. Tên học viên, tên lớp, tên bài ghép từ dữ liệu của hai cổng, **màn không gọi thêm cho từng dòng** (`INS0206` dòng 459).

### 13.6 Truy cập dữ liệu

`judge.submissions` (R); `ClassScopeReadPort`, `ClassAssignmentReadPort` (R).

### 13.7 Test viewpoints

| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| T-LI-01 | Normal | Học viên trong lớp nộp bài đã giao: có đúng một dòng |
| T-LI-02 | Security | Bài tự luyện không được giao cho lớp: không có dòng nào |
| T-LI-03 | Boundary | Học viên rời lớp ngày D: bài nộp trước D còn, sau D không, dòng còn có `leftClass=true` |
| T-LI-04 | Boundary | Học viên rời rồi vào lại: lượt nộp trong khoảng đã rời vẫn thuộc phạm vi (`left_at` về NULL) |
| T-LI-05 | Concurrency | Học viên ở hai lớp cùng giao một bài: mỗi lượt nộp đúng một dòng, gắn lớp có `assigned_at` mới nhất không sau `submitted_at` |
| T-LI-06 | Security | `classId` của giảng viên khác: `404 JDG-111`, không trả danh sách rỗng |
| T-LI-07 | Security | Mọi `items[]` không có `sourceCode`, `compileErrorMessage` |
| T-LI-08 | Normal | `result=failed` không gồm `SYSTEM_ERROR` và không gồm bài đang chấm |

## 14. GET /instructor/submissions/{submissionId}

### 14.1 Hợp đồng

| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `CLASS_MANAGEMENT:READ` và bài nộp nằm trong phạm vi lớp phụ trách (`USR0201` vùng giảng viên, A2-1 dòng 557) |
| Idempotent | có |
| Tên BD | `GetSubmissionDetail`, biến thể cho A2 (`USR0201` dòng 548) |
| Không có kênh realtime | tải một lần (`DEC-2026-1014-instructor-submissions-list-details`, `USR0201` dòng 549) |

### 14.2 Request

| Trường | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `submissionId` | path UUID | |
| `studentId`, `classId`, `from` | query | **bị bỏ qua hoàn toàn** ở máy chủ: chỉ là gợi ý breadcrumb phía giao diện; máy chủ lấy học viên và lớp từ chính bài nộp (A2-2 dòng 558) |

### 14.3 Response 200

Cùng `SubmissionDetailDto` (mục 8.3), **thêm** `studentId`, `studentName`, `classId`, `className`, `leftClass`; **bỏ** `beatsPercent`, `beatsSampleSize` (`USR0201` dòng 548, 539). Giữ `sourceCode` (A2 được đọc mã nguồn, A2-4 dòng 560). Quy tắc testcase ẩn **áp nguyên vẹn** như bài nộp của học viên.

### 14.4 Lỗi

| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 403 | `JDG-301` | thiếu `CLASS_MANAGEMENT:READ` | |
| 404 | `JDG-110` | không có bài nộp, hoặc ngoài phạm vi | **404, không phải 403**: không lộ bài nộp có thật (A2-1) |

### 14.5 Luồng xử lý

BR-JD-10 áp cho một `submissionId`: kiểm có lớp do người gọi phụ trách mà học viên ghi danh với `submitted_at >= joined_at` và (`left_at IS NULL` hoặc `submitted_at <= left_at`), và bài thuộc `class_assignments` của lớp (kể cả gỡ mềm). Ngoài các điều kiện này thì `JDG-110`. Lớp được báo trong phản hồi chọn theo cùng quy tắc "lần giao mới nhất không sau `submitted_at`", nếu không có lần giao nào không sau `submitted_at` thì lấy lớp có lần giao mới nhất `[SoT: Suy luận]`, xem Q15.

### 14.6 Test viewpoints

| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| T-AD-01 | Normal | Bài nộp của học viên trong lớp mình: đủ phần tử, có `studentName`, `className`, `leftClass=false` |
| T-AD-02 | Security | Bài nộp của lớp giảng viên khác: `JDG-110` giống id không tồn tại |
| T-AD-03 | Security | Giả mạo `studentId` và `classId` trên URL: dữ liệu trả về vẫn là của chủ bài nộp thật |
| T-AD-04 | Boundary | Học viên đã rời lớp, bài nộp làm trước `left_at`: mở được, `leftClass=true` |
| T-AD-05 | Security | Bài nộp sau `left_at`: `JDG-110` |
| T-AD-06 | Security | Testcase `HIDDEN` không có input, expected, output (như mục 8) |
| T-AD-07 | Normal | Không có `beatsPercent` trong phản hồi |
| T-AD-08 | Abnormal | Tài khoản `STUDENT` gọi: `JDG-301` |

## 15. GET /instructor/classes/{classId}/students/{studentId}/submissions

### 15.1 Hợp đồng

| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `CLASS_MANAGEMENT:READ`; lớp do người gọi phụ trách; học viên có ghi danh lớp đó |
| Idempotent | có |
| Tên BD | `ListStudentClassSubmissions` (`INS0204` Sheet 7.3 NO 4 dòng 666) |

### 15.2 Request

| Trường | Kiểu | Bắt buộc | Ràng buộc |
| :--- | :--- | :-: | :--- |
| `classId`, `studentId` | path UUID | Có | |
| `page` | query int | Không | trang 20 dòng `[SoT: Suy luận]` |

### 15.3 Response 200 (`StudentSubmissionRowDto`)

Dạng phân trang chung. `items[]`: `submissionId`, `problemTitle` (ghép sẵn, `INS0204` dòng 625), `language`, `status`, `scoreRatio`, `submittedAt`. Không có `sourceCode`, không có chi tiết testcase (`INS0204` dòng 632-634). `scoreRatio` null (hiển thị `-`) khi `COMPILE_ERROR`, `SYSTEM_ERROR` hoặc chưa chấm xong.

### 15.4 Lỗi

| HTTP | error.code | Khi nào |
| :-: | :--- | :--- |
| 403 | `JDG-301` | thiếu `CLASS_MANAGEMENT:READ` |
| 404 | `JDG-111` | lớp không phải của người gọi |
| 404 | `JDG-112` | học viên chưa từng ghi danh lớp này |

### 15.5 Luồng xử lý

`ClassScopeReadPort.getInstructorScope(instructorId, classId)` lọc `studentId`; không có thì `JDG-111` hoặc `JDG-112`. `ClassAssignmentReadPort.listAssignments([classId])`. Truy vấn `judge.submissions` với `user_id = :studentId`, `problem_id` thuộc bài đã giao cho **lớp này** (kể cả gỡ mềm), `submitted_at` trong cửa sổ `joined_at..left_at`, sắp `submitted_at` giảm dần. Quy tắc chọn lớp theo `assigned_at` của `INS0206` **không** áp ở đây vì lớp đã cố định theo đường dẫn; hệ quả: hai màn có thể đếm khác nhau, xem Q15.

### 15.6 Test viewpoints

| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| T-SC-01 | Normal | Học viên có 25 bài nộp bài đã giao: trang 1 có 20, `totalCount=25` |
| T-SC-02 | Security | Bài tự luyện không có trong lịch sử |
| T-SC-03 | Boundary | Học viên đã rời lớp: chỉ còn lượt nộp trước `left_at` |
| T-SC-04 | Security | Lớp của giảng viên khác: `JDG-111` |
| T-SC-05 | Security | Học viên không thuộc lớp: `JDG-112` |
| T-SC-06 | Security | Phản hồi không có `sourceCode` |

## 16. GET /admin/queue/snapshot

### 16.1 Hợp đồng

Xác thực JWT; quyền `JUDGE_QUEUE_MONITOR:READ` (`02-bd/security/judge-orchestration.md:70-72`); idempotent. Tên BD `GetQueueSnapshot` (`ADM0401` Sheet 7.3 NO 1). BD cho phép gộp `GetQueueSettings` (NO 7) vào `GetQueueSnapshot`; DD tách thành endpoint #18 vì popup tải riêng khi mở (`ADM0401` EVT-10).

### 16.2 Response 200 (`QueueSnapshotDto`)

| Trường | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `generatedAt` | ISO-8601 | |
| `status` | enum | `PAUSED`, `WORKER_OVERLOADED`, `BACKLOGGED`, `NORMAL`; ưu tiên khi nhiều điều kiện cùng đúng theo thứ tự đó (`ADM0401` Sheet 5 khu vực J dòng 384): `queue_paused = true`; có worker tải trên 85%; số job `PENDING` vượt `autoscale_job_threshold`; còn lại `NORMAL` |
| `pendingCount` | int | đếm `PENDING` (chỉ là chữ trong dải trạng thái, không phải chỉ số rời) |
| `queueConsumptionPaused`, `autoscaleEnabled` | boolean | từ `cluster_settings` |
| `autoscaleJobThreshold` | int | cho dòng mô tả của công tắc |
| `avgWaitMillis` | int hoặc null | trung bình `started_grading_at - submitted_at` của job đã bắt đầu chấm trong **một giờ gần nhất**; null khi không có job nào (`ADM0401` Sheet 5 khu vực E NO 4 dòng 325) |
| `latencyCapSeconds` | int | mẫu số thanh độ trễ |
| `workers` | array hoặc null | `{ nodeId, status (RUNNING, IDLE, OVERLOADED), loadPercent, runningJobCount, uptimeSeconds, vcpuCount }`; **null** kèm `workersAvailable = false` khi Prometheus không trả lời hoặc go-judge không xuất metric (`ADM0401` Q2: chưa kiểm go-judge xuất được); giao diện ẩn khối thay vì bịa số (tiêu chí ponytail bậc 2) |
| `workerCount`, `workerAutoscaleMax` | int | quy mô hiện tại và trần "tối đa" là hằng số `application.yml` (`ADM0401` Q8) |

### 16.3 Lỗi

`403 JDG-301`.

### 16.4 Luồng xử lý và dữ liệu

BR-JD-15. Đọc `cluster_settings` (một dòng), đếm `submissions` theo `status`, tính độ trễ, gọi Prometheus có timeout 2 giây; Prometheus lỗi thì `workers = null` và `status` bỏ điều kiện `WORKER_OVERLOADED`. Bảng: `judge.cluster_settings` (R), `judge.submissions` (R, chỉ mục `(status, updated_at)`).

### 16.5 Test viewpoints

| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| T-QS-01 | Normal | Hàng đợi trống: `NORMAL`, `avgWaitMillis=null` |
| T-QS-02 | Normal | 25 job `PENDING`, ngưỡng 20: `BACKLOGGED` |
| T-QS-03 | Boundary | `queue_paused=true` và 25 job: `PAUSED` thắng `BACKLOGGED` |
| T-QS-04 | Abnormal | Prometheus tắt: `workers=null`, `workersAvailable=false`, các phần khác vẫn đúng |
| T-QS-05 | Security | Tài khoản không có `JUDGE_QUEUE_MONITOR:READ`: `JDG-301` |

## 17. GET /admin/queue/jobs

### 17.1 Hợp đồng

Xác thực JWT; `JUDGE_QUEUE_MONITOR:READ`; idempotent. Tên BD `ListQueueJobs` (NO 2).

### 17.2 Request (query)

| Trường | Kiểu | Bắt buộc | Ràng buộc | Ghi chú |
| :--- | :--- | :-: | :--- | :--- |
| `tab` | enum | Không | `waiting` (mặc định), `failed`, `all` | giá trị lạ về mặc định |
| `page` | int | Không | | kích thước trang = `cluster_settings.jobs_per_page` (mặc định 20) |

### 17.3 Response 200

`{ "items": [QueueJobDto], "page", "pageSize", "totalCount", "counts": { "waiting", "failed", "all" } }`. `QueueJobDto`: `submissionId`, `language`, `status` (gộp: `PENDING` giữ nguyên, `COMPILING` và `JUDGING` gộp thành nhãn "Đang chấm" ở giao diện; máy chủ trả giá trị gốc), `waitingMillis` (null với `SYSTEM_ERROR`). `counts`: `waiting` = số `PENDING`; `failed` = số `SYSTEM_ERROR` có `finished_at >= now() - error_retention_hours`; `all` = số job chưa cuối cộng `failed` (`QueueJobCountsDto`, `ADM0401` dòng 587). Không có `priority`, không có "mã job" (đã bỏ, `ADM0401` NO 5, NO 8).

Thứ tự: theo thời gian chờ giảm dần: khoá sắp xếp là `COALESCE(started_grading_at, submitted_at)` tăng dần, **job `SYSTEM_ERROR` xếp cuối** (`ADM0401` dòng 342). `waitingMillis` = `now() - submitted_at` nếu chưa có `started_grading_at`, ngược lại `now() - started_grading_at`.

### 17.4 Lỗi và dữ liệu

`403 JDG-301`. `judge.submissions` (R); `judge.cluster_settings` (R).

### 17.5 Test viewpoints

| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| T-QJ-01 | Normal | Tab `waiting` chỉ có `PENDING`, job chờ lâu nhất đứng đầu |
| T-QJ-02 | Boundary | Job `SYSTEM_ERROR` kết thúc 25 giờ trước, `error_retention_hours=24`: không xuất hiện ở `failed` và `all`, `submissions` không bị xoá |
| T-QJ-03 | Normal | `jobs_per_page=5`: mỗi trang 5 dòng, hiệu lực ở lượt tải kế tiếp sau khi lưu |
| T-QJ-04 | Normal | Tab `all` xếp job lỗi sau job còn lại |

## 18. GET /admin/queue/infra-summary

Xác thực JWT; `JUDGE_QUEUE_MONITOR:READ`; idempotent. Tên BD `GetInfraSummary` (NO 3). Response `200`: `{ "warnCount": n, "errorCount": n, "windowHours": 24 }`: số sự kiện mức `WARN` và `ERROR` trong 24 giờ gần nhất, từ **cùng nguồn** với endpoint #20 (BR-JD-15 mục "nguồn sự kiện hạ tầng"), để thẻ ở `ADM0401` và danh sách ở `ADM0404` không lệch nhau (`ADM0404` dòng 320). Lỗi `403 JDG-301`. Dữ liệu: `judge.outbox_events` (R), `judge.submissions` (R). Test viewpoints: T-IS-01 (một dòng outbox 3 lần thử là một `WARN`, 5 lần là một `ERROR`), T-IS-02 (`SYSTEM_ERROR` cách đây 25 giờ không tính), T-IS-03 (số khớp với số dòng cùng mức của #20 trong 24 giờ).

## 19. PUT /admin/queue/consumption

### 19.1 Hợp đồng

| Mục | Giá trị |
| :--- | :--- |
| Xác thực, quyền | JWT, `baseCategory = ADMIN` kiểm lại ở mỗi lệnh, không tin giao diện đã ẩn nút (`ADM0401` Sheet 9 NO 2 dòng 666) |
| Idempotent | có (đặt giá trị tuyệt đối) |
| Tên BD | `SetQueueConsumptionPaused` |
| Kiểm toán | một dòng `system_audit_logs` qua `AuditLogPort` **trong cùng giao dịch** (`02-bd/architecture/identity.md:92`) |

### 19.2 Request

| Trường | Kiểu | Bắt buộc | Ràng buộc | Ghi chú |
| :--- | :--- | :-: | :--- | :--- |
| `paused` | boolean | Có | | trạng thái mới |
| `expectedPaused` | boolean | Có | | trạng thái màn đang thấy; khác trạng thái thật thì từ chối thay vì ghi đè (`ADM0401` Sheet 9 NO 5 dòng 669) |

### 19.3 Response 200

`{ "paused": boolean, "updatedAt": "..." }`.

### 19.4 Lỗi

| HTTP | error.code | Khi nào |
| :-: | :--- | :--- |
| 403 | `JDG-301` | không phải `ADMIN` |
| 409 | `JDG-201` | `expectedPaused` khác trạng thái thật; giao diện tải lại một lô đầy đủ |
| 400 | `JDG-V16` | thiếu `paused` hoặc `expectedPaused` |

### 19.5 Luồng xử lý

1. Một giao dịch: `SELECT ... FROM judge.cluster_settings WHERE id = 1 FOR UPDATE`.
2. `expectedPaused <> queue_paused` thì `JDG-201`. Bằng nhau mà `paused` cũng bằng thì trả 200, không ghi, **không** ghi nhật ký (không đổi gì).
3. `UPDATE cluster_settings SET queue_paused = :paused, updated_by = :admin, updated_at = now()`.
4. Khi **tiếp tục** (`paused: true -> false`): trong cùng giao dịch `UPDATE judge.submissions SET updated_at = now() WHERE status = 'PENDING'`, để đồng hồ 5 phút của sweep tính lại từ lúc mở (`02-bd/architecture/judge-orchestration.md:220-221`, `03-dd/jobs/judge-orchestration.md:64`).
5. `AuditLogPort.record(admin, 'QUEUE_PAUSE_CHANGED', 'CLUSTER_SETTINGS', '1', {queuePaused: old}, {queuePaused: new})`. Lỗi ghi nhật ký làm hỏng cả giao dịch (không nuốt lỗi, `03-dd/logic/identity.md` BR-24 dòng 356).
6. Sau commit, bộ điều khiển worker thấy cờ (chu kỳ kiểm tối đa 3 giây, cache trong tiến trình `[SoT: Suy luận]`, `ADM0401` Q3 dòng 689) và dừng hoặc mở lại việc nhận message; job đang chấm chạy hết (BR-JD-08).

### 19.6 Dữ liệu

`judge.cluster_settings` (R, U); `judge.submissions` (U, `updated_at` của `PENDING` khi tiếp tục); `AuditLogPort`.

### 19.7 Test viewpoints

| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| T-QC-01 | Normal | Tạm dừng: cờ `true`; bài nộp mới nằm `PENDING`; job đang chấm chạy hết |
| T-QC-02 | Normal | Tiếp tục: mọi `PENDING` có `updated_at` mới, sweep không đưa chúng về `SYSTEM_ERROR` trong 5 phút kế |
| T-QC-03 | Abnormal | Hai admin cùng bật: người thứ hai nhận `JDG-201` |
| T-QC-04 | Security | `INSTRUCTOR` gọi trực tiếp: `JDG-301` |
| T-QC-05 | Normal | Mỗi lệnh đổi trạng thái tạo đúng một dòng nhật ký có giá trị cũ và mới; gọi lặp không đổi gì thì không có dòng mới |
| T-QC-06 | Abnormal | `AuditLogPort` lỗi: cờ **không** đổi (cùng giao dịch) |

## 20. PUT /admin/queue/autoscale

Cùng khuôn endpoint #19. Tên BD `SetWorkerAutoscaleEnabled`. Quyền `ADMIN`. Request `{ "enabled": boolean, "expectedEnabled": boolean }`; response `{ "enabled": boolean, "updatedAt": "..." }`. Lỗi: `403 JDG-301`, `409 JDG-201` (`expectedEnabled` lệch trạng thái thật), `400 JDG-V16`. Luồng: khoá dòng `cluster_settings` `FOR UPDATE`, so `expectedEnabled`, `UPDATE autoscale_enabled`, `AuditLogPort.record(admin, 'QUEUE_AUTOSCALE_CHANGED', 'CLUSTER_SETTINGS', '1', before, after)` cùng giao dịch. **Tác dụng thật lên cụm** (thêm worker) thuộc thành phần mở rộng đọc cờ `autoscale_enabled` và `autoscale_job_threshold`; DD này chỉ lưu cờ và công bố qua `GetQueueSnapshot`, xem Q16. Bảng: `judge.cluster_settings` (R, U). Test viewpoints: T-QA-01 (tắt rồi đọc lại snapshot thấy `autoscaleEnabled=false`), T-QA-02 (`expectedEnabled` cũ: `JDG-201`), T-QA-03 (không phải ADMIN: `JDG-301`), T-QA-04 (một dòng nhật ký có giá trị cũ và mới).

## 21. GET /admin/queue/settings và PUT /admin/queue/settings

### 21.1 Hợp đồng

`GET`: JWT, `JUDGE_QUEUE_MONITOR:READ`, idempotent, tên BD `GetQueueSettings`. `PUT`: JWT, `baseCategory = ADMIN` (`ADM0401` Sheet 9 NO 9 dòng 673: xem bằng `JUDGE_QUEUE_MONITOR`, ghi bằng vai trò `ADMIN`, không thêm Function riêng), idempotent, tên BD `UpdateQueueSettings`, kiểm toán qua `AuditLogPort`.

### 21.2 Request và response

Cùng một đối tượng `QueueSettingsDto` cho response của `GET`, request và response của `PUT`:

| Trường | Kiểu | Ràng buộc | Cột |
| :--- | :--- | :--- | :--- |
| `autoscaleJobThreshold` | int | số nguyên tối thiểu 1, **không có trần nghiệp vụ** | `autoscale_job_threshold`, mặc định 20 |
| `jobsPerPage` | int | tối thiểu 1 | `jobs_per_page`, mặc định 20 |
| `errorRetentionHours` | int | tối thiểu 1 | `error_retention_hours`, mặc định 24 |
| `latencyCapSeconds` | int | tối thiểu 1 | `latency_cap_seconds`, mặc định 10 |
| `updatedAt` | ISO-8601 | chỉ ở response | |

Không có giá trị tối đa nghiệp vụ (`DEC-2026-1001-admin-configurable-settings`, `ADM0401` Sheet 9 NO 8 dòng 672); giới hạn duy nhất là kiểu `INT` của PostgreSQL (2.147.483.647), vượt thì coi là số không hợp lệ `JDG-V09`.

### 21.3 Lỗi

| HTTP | error.code | Khi nào |
| :-: | :--- | :--- |
| 403 | `JDG-301` | `PUT` không phải `ADMIN`; `GET` thiếu `JUDGE_QUEUE_MONITOR:READ` |
| 400 | `JDG-V09` | một trong bốn ô không phải số nguyên từ 1 trở lên (số thập phân, chuỗi, rỗng, nhỏ hơn 1, vượt kiểu `INT`) |

### 21.4 Luồng xử lý `PUT`

Khoá dòng `cluster_settings FOR UPDATE`; ghi chỉ các cột đổi; `AuditLogPort.record(admin, 'QUEUE_SETTINGS_CHANGED', 'CLUSTER_SETTINGS', '1', before, after)` (chỉ các trường đã đổi) trong cùng giao dịch; không đổi gì thì không ghi nhật ký. Không kiểm xung đột phiên bản (BD không đòi cho popup này; người ghi sau thắng), xem Q17. `jobsPerPage`, `errorRetentionHours`, `latencyCapSeconds` có hiệu lực ở **lượt đọc kế tiếp** của #13 và #14 (`ADM0401` EVT-11 dòng 648); `autoscaleJobThreshold` ảnh hưởng nhãn `BACKLOGGED` và dòng mô tả công tắc.

### 21.5 Dữ liệu và test viewpoints

`judge.cluster_settings` (R, U); `AuditLogPort`. T-QT-01 (đổi `jobsPerPage=5` thì #14 trả 5 dòng mỗi trang ngay lần đọc sau), T-QT-02 (`0` hoặc `1.5` hoặc `""`: `JDG-V09`, không ghi gì), T-QT-03 (`errorRetentionHours` lớn tuỳ ý, ví dụ 100000, được chấp nhận vì không có trần nghiệp vụ), T-QT-04 (`INSTRUCTOR` gọi `PUT`: `JDG-301`), T-QT-05 (một dòng nhật ký phân loại "Cấu hình" có giá trị cũ và mới của các trường đổi).

## 22. GET /admin/infra-events

### 22.1 Hợp đồng

Xác thực JWT; quyền `JUDGE_QUEUE_MONITOR:READ`; idempotent; chỉ đọc, không ghi bảng nào. Tên BD `ListInfraEvents` (`ADM0404` Sheet 7.3 NO 1). Không tự làm tươi: tải khi vào màn, khi đổi bộ lọc và khi bấm "Làm mới" (`ADM0404` Q1).

### 22.2 Request (query)

| Trường | Kiểu | Bắt buộc | Ràng buộc | Ghi chú |
| :--- | :--- | :-: | :--- | :--- |
| `q` | string | Không | tối đa 100 ký tự (dài hơn thì cắt) | tìm trong nội dung sự kiện, không phân biệt hoa thường; giá trị nhập là **dữ liệu**, không diễn giải như chỉ thị (`ADM0404` dòng 219) |
| `level` | enum | Không | `error`, `warn`, `info`; không phân biệt hoa thường | giá trị khác bị bỏ qua (`ADM0404` Q4) |
| `service` | enum | Không | `go-judge`, `outbox` | giá trị lạ bị bỏ qua |
| `from`, `to` | date `yyyy-MM-dd` | Không | khoảng đóng hai đầu theo UTC+7; `from <= to` | `from > to`: `400 JDG-V13` (`ADM0404` Sheet 9 NO 2 dòng 353) |
| `cursor` | string | Không | con trỏ do máy chủ cấp | trang 20 dòng cố định |

### 22.3 Response 200 (`InfraEventPageDto`)

`{ "events": [ { "occurredAt", "level", "message", "service" } ], "nextCursor": string hoặc null, "services": ["go-judge", "outbox"] }`. `nextCursor = null` nghĩa là hết. Mới nhất trước.

### 22.4 Lỗi

`403 JDG-301`; `400 JDG-V13`.

### 22.5 Luồng xử lý (BR-JD-15)

Hai nguồn thật, hợp bằng một truy vấn `UNION ALL`, lọc cửa sổ **90 ngày** gần nhất (`ADM0404` Q3, Q5):

1. `outbox_events` có `publish_attempts >= 3`: `level = WARN` khi 3 đến 4 lần, `ERROR` khi từ 5 lần (hằng số `judge.infra.outbox-warn-attempts = 3`, `outbox-error-attempts = 5`); `service = 'outbox'`; `occurredAt = created_at` `[SoT: Suy luận]` (không có cột thời điểm "kẹt").
2. `submissions` có `status = 'SYSTEM_ERROR'`: `level = ERROR`, `service = 'go-judge'`, `occurredAt = finished_at`.

`message` **dựng ở tầng ứng dụng** từ khuôn có sẵn, không có cột `message` lưu (`ADM0404` Q5 dòng 369): ví dụ `Bản ghi Outbox SubmissionGraded của lượt nộp {id} đã thử publish {n} lần, chưa được broker xác nhận`; `Lượt nộp {id} bị đưa về lỗi hệ thống vì worker không cập nhật quá 5 phút` (`WORKER_TIMEOUT`); `Lượt nộp {id}: go-judge báo lỗi hạ tầng` (`SANDBOX_FAULT`). Khuôn chỉ chứa mã định danh hệ thống, không chứa dữ liệu người dùng. Mức `INFO` chưa có nguồn nào phát ra (BD liệt kê ba mức nhưng chỉ hai nguồn lỗi), nên trả rỗng khi lọc `info` `[SoT: Suy luận]`.

Con trỏ: `(occurredAt, sourceKey)` mã hoá base64url; trang kế dùng điều kiện `(occurredAt, sourceKey) < cursor`.

### 22.6 Dữ liệu và test viewpoints

`judge.outbox_events` (R), `judge.submissions` (R). T-IE-01 (dòng outbox 3 lần thử là `WARN`, 5 lần là `ERROR`, service `outbox`), T-IE-02 (`SYSTEM_ERROR` là `ERROR`, service `go-judge`), T-IE-03 (mới nhất trước, 20 dòng mỗi trang, `nextCursor` null ở trang cuối), T-IE-04 (`q` tìm không phân biệt hoa thường), T-IE-05 (`from > to`: `JDG-V13`), T-IE-06 (sự kiện quá 90 ngày không xuất hiện), T-IE-07 (`level=INFO` trả danh sách rỗng, không lỗi), T-IE-08 (`level=xyz` bị bỏ qua).

## 23. GET /admin/language-config

### 23.1 Hợp đồng

Xác thực JWT; quyền `LANGUAGE_CONFIG:READ` (chỉ vai trò `ADMIN`, `ADM0501` Sheet 9 NO 1 dòng 491); idempotent. Tên BD `GetLanguageConfigs` (Sheet 7.3 NO 1). **Một lần gọi** trả mọi thứ màn cần.

### 23.2 Response 200

| Trường | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `languages[]` | array | đúng 3 phần tử `JAVA`, `CPP`, `PYTHON` |
| `languages[].language` | enum | |
| `languages[].timeLimitMultiplier`, `memoryLimitMultiplier` | decimal | hai chữ số thập phân |
| `languages[].enabled` | boolean | |
| `languages[].pendingJobCount` | int | số bài `PENDING` của ngôn ngữ, cho hộp xác nhận lúc tắt (`ADM0501` Sheet 7.3 NO 1) |
| `languages[].updatedAt` | ISO-8601 | để gửi lại khi lưu, phát hiện xung đột |
| `gradingDefaults` | object | `{ timeLimitMs, memoryLimitMb, maxOutputSizeKb, compileTimeoutSeconds, updatedAt }` |
| `sandbox` | object | `{ networkAccessEnabled, childProcessLimitEnabled, returnStderrToStudent, updatedAt }`; **không có** `maxChildProcesses`: trần 60 là hằng số `application.yml` (`DEC-2026-1014-sandbox-child-process-cap-constant`, `database/judge-orchestration.md:193`) |
| `generatorSandbox` | object | `{ maxRuntimeSeconds, maxOutputMb, updatedAt }` |

Giới hạn hiệu dụng (cột "Giới hạn hiệu dụng") là giá trị **tính ở giao diện** từ mặc định nhân hệ số; endpoint trả đủ thừa số để tính.

### 23.3 Lỗi và dữ liệu

`403 JDG-301`. `judge.language_configs`, `judge.grading_defaults`, `judge.sandbox_settings`, `judge.generator_sandbox_settings` (R); `judge.submissions` (R, đếm `PENDING` theo ngôn ngữ).

### 23.4 Test viewpoints

T-LC-01 (trả đủ ba ngôn ngữ và bốn khối), T-LC-02 (`pendingJobCount` đúng với số bài `PENDING` mỗi ngôn ngữ), T-LC-03 (`sandbox` không có trường số tiến trình con), T-LC-04 (thiếu `LANGUAGE_CONFIG:READ`: `JDG-301`).

## 24. PUT /admin/language-config

### 24.1 Hợp đồng

| Mục | Giá trị |
| :--- | :--- |
| Xác thực, quyền | JWT, `LANGUAGE_CONFIG:UPDATE` (và vai trò `ADMIN`, `02-bd/security/judge-orchestration.md:77-80`). `CREATE` và `DELETE` không có tác dụng ở màn này (`ADM0501` Sheet 9 NO 1) |
| Idempotent | có (đặt giá trị tuyệt đối) |
| Tên BD | `UpdateLanguageConfigs` (NO 2) |
| Kiểm toán | **một** dòng `system_audit_logs` qua `AuditLogPort` trong cùng giao dịch, phân loại "Cấu hình", chứa giá trị cũ và mới của mọi trường đã đổi (`ADM0501` EVT-11 dòng 476) |
| Thời điểm áp dụng | có hiệu lực với một job **từ lúc worker bắt đầu chấm** nó, không phải lúc nộp; job đã bắt đầu thì chạy nốt với cấu hình đã đọc (G3, `database/judge-orchestration.md:198`) |

### 24.2 Request

Toàn bộ thay đổi trong **một** lần gọi (cả hai tab dùng chung trạng thái biên soạn, `ADM0501` EVT-8):

| Trường | Kiểu | Bắt buộc | Ràng buộc |
| :--- | :--- | :-: | :--- |
| `languages[]` | array 3 phần tử | Có | đủ `JAVA`, `CPP`, `PYTHON`, không thêm, không bớt |
| `languages[].timeLimitMultiplier` | decimal | Có | `0,10` đến `9,99`, tối đa 2 chữ số thập phân |
| `languages[].memoryLimitMultiplier` | decimal | Có | như trên |
| `languages[].enabled` | boolean | Có | sau thay đổi phải còn **ít nhất một** ngôn ngữ `true` |
| `languages[].updatedAt` | ISO-8601 | Có | giá trị đã nhận ở `GET`, để phát hiện xung đột |
| `gradingDefaults.timeLimitMs` | int | Có | số nguyên từ 1; **giới hạn hiệu dụng** mỗi ngôn ngữ (`timeLimitMs * timeLimitMultiplier`) trong 100 đến 30.000 ms |
| `gradingDefaults.memoryLimitMb` | int | Có | từ 1; hiệu dụng (`* memoryLimitMultiplier`) trong 64 đến 1.024 MB |
| `gradingDefaults.maxOutputSizeKb`, `compileTimeoutSeconds` | int | Có | từ 1 |
| `gradingDefaults.updatedAt` | ISO-8601 | Có | |
| `sandbox.networkAccessEnabled`, `childProcessLimitEnabled`, `returnStderrToStudent` | boolean | Có | |
| `sandbox.updatedAt` | ISO-8601 | Có | |
| `generatorSandbox.maxRuntimeSeconds` | int | Có | 1 đến 300 |
| `generatorSandbox.maxOutputMb` | int | Có | 1 đến 1.024 |
| `generatorSandbox.updatedAt` | ISO-8601 | Có | |

### 24.3 Response 200

Cùng hình dạng response của `GET` (mục 23.2) sau khi lưu (giao diện tải lại cấu hình từ phản hồi, `ADM0501` EVT-5 bước 5).

### 24.4 Lỗi

| HTTP | error.code | Khi nào |
| :-: | :--- | :--- |
| 403 | `JDG-301` | thiếu `LANGUAGE_CONFIG:UPDATE` hoặc không phải `ADMIN` |
| 409 | `JDG-202` | `updatedAt` của một dòng đã đổi kể từ lúc tải (`ADM0501` Sheet 9 NO 7 dòng 497) |
| 422 | `JDG-203` | sau thay đổi không còn ngôn ngữ nào `enabled` |
| 422 | `JDG-204` | giới hạn thời gian hiệu dụng của một ngôn ngữ ngoài 100 đến 30.000 ms |
| 422 | `JDG-205` | giới hạn bộ nhớ hiệu dụng của một ngôn ngữ ngoài 64 đến 1.024 MB |
| 400 | `JDG-V10`, `JDG-V11`, `JDG-V12` | hệ số ngoài khoảng hoặc sai định dạng; giới hạn mặc định không phải số nguyên từ 1; giới hạn script ngoài khoảng |

### 24.5 Luồng xử lý (BR-JD-09)

1. Kiểm định dạng và khoảng từng trường (`JDG-V10`..`V12`), rồi quy tắc liên trường (`JDG-203`, `JDG-204`, `JDG-205`).
2. Một giao dịch: khoá các dòng liên quan `FOR UPDATE`; so `updatedAt` từng dòng, lệch thì `JDG-202`, không ghi gì.
3. `UPDATE` **chỉ các dòng có trường đổi**, đặt `updated_by`, `updated_at`.
4. `AuditLogPort.record(admin, 'LANGUAGE_CONFIG_CHANGED', 'LANGUAGE_CONFIG', 'ALL', before, after)` với `before`/`after` chỉ gồm các trường đã đổi, cùng giao dịch. Không có trường nào đổi: trả `200`, không ghi, không có dòng nhật ký.
5. **Không** chạm job đang chờ: tắt ngôn ngữ chỉ chặn bài nộp **mới** (`JDG-102` ở endpoint #3), job đã vào hàng đợi chạy nốt (G4, `ADM0501` Sheet 9 NO 14 dòng 504). Không cần khởi động lại dịch vụ, không cache tĩnh (worker đọc DB lúc bắt đầu chấm).

### 24.6 Dữ liệu

`judge.language_configs`, `judge.grading_defaults`, `judge.sandbox_settings`, `judge.generator_sandbox_settings` (R, U); `AuditLogPort`. Không có `C` và `D` (ba dòng ngôn ngữ seed sẵn, các bảng còn lại là dòng đơn).

### 24.7 Test viewpoints

| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| T-UL-01 | Normal | Đổi hệ số thời gian Java từ `2.00` sang `3.00`: lượt chấm bắt đầu sau đó dùng `3.00`; một dòng nhật ký có giá trị cũ và mới |
| T-UL-02 | Normal | Job đang chấm lúc lưu chạy nốt với giá trị cũ; job bắt đầu chấm sau đó dùng giá trị mới |
| T-UL-03 | Abnormal | Tắt cả ba ngôn ngữ: `JDG-203`, không ghi gì |
| T-UL-04 | Boundary | Hệ số `0.09` và `10.00`: `JDG-V10`; `0.10` và `9.99`: nhận |
| T-UL-05 | Boundary | Giới hạn mặc định 1000 ms nhân hệ số `9.99` ra 9.990: nhận; mặc định 4000 ms nhân `9.99` ra 39.960: `JDG-204` |
| T-UL-06 | Abnormal | Hai admin lưu đồng thời: người sau nhận `JDG-202`, không ghi đè |
| T-UL-07 | Normal | Tắt Python khi còn job Python đang chờ: job vẫn chạy nốt, bài nộp Python mới nhận `JDG-102` |
| T-UL-08 | Boundary | `maxRuntimeSeconds` 0 và 301: `JDG-V12`; 1 và 300: nhận. `maxOutputMb` 0 và 1.025: `JDG-V12`; 1 và 1.024: nhận |
| T-UL-09 | Security | Người chỉ có `READ`: `JDG-301`, không ghi |
| T-UL-10 | Abnormal | `AuditLogPort` lỗi: không có thay đổi nào ghi vào bảng cấu hình |

## 25. Sự kiện miền do module phát ra (Transactional Outbox)

Nguồn: `DEC-2026-0912-judge-outbox-pattern`, `02-bd/architecture/judge-orchestration.md:243-275`. Cơ chế phát: dòng `outbox_events` ghi **trong cùng giao dịch** đưa bài nộp vào trạng thái cuối; relay JOB-JD-02 publish lên RabbitMQ, chỉ đánh dấu `published_at` sau khi broker ack (`03-dd/jobs/judge-orchestration.md` mục 2, **không lặp lại**).

### 25.1 Phong bì và định tuyến

Phong bì thống nhất với hợp đồng tiêu thụ của `identity` (`03-dd/api/identity.md:238-247`):

```json
{ "eventId": "uuid", "eventType": "SubmissionGraded | SubmissionAccepted",
  "occurredAt": "ISO-8601", "schemaVersion": 1, "payload": { } }
```

| Mục | Giá trị |
| :--- | :--- |
| `eventId` | `outbox_events.id` (UUID). Mọi lần relay publish lại cùng một dòng mang cùng `eventId`, nên consumer khử trùng theo `eventId` |
| `occurredAt` | thời điểm bài nộp vào trạng thái cuối (= `finished_at`) |
| `schemaVersion` | `1`; thêm trường tuỳ chọn không đổi số, đổi hay bỏ trường thì tăng số `[SoT: Suy luận]`: BD để versioning cho DD (`02-bd/architecture/judge-orchestration.md:261`) |
| Exchange, khoá định tuyến | exchange `algoprep.domain-events` (topic); khoá `judge.submission.graded`, `judge.submission.accepted` `[SoT: Suy luận]`, đặt tên theo mẫu `module.aggregate.action` |
| `outbox_events.event_type` | `SubmissionGraded` hoặc `SubmissionAccepted` |

### 25.2 `SubmissionGraded`

Phát cho **mọi** trạng thái cuối, kể cả `ACCEPTED`, `COMPILE_ERROR`, `SYSTEM_ERROR` (do worker hoặc do sweep).

| Trường `payload` | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `submissionId`, `userId`, `problemId` | UUID | |
| `language` | enum | nuôi `user_submission_stats.top_language` và `language_counts` |
| `verdict` | enum | một trong sáu trạng thái cuối (tên `verdict`, theo `03-dd/api/identity.md:251`) |
| `passedCount`, `totalCount` | int hoặc null | null khi `COMPILE_ERROR`, `SYSTEM_ERROR` |
| `passedTestcaseRatio` | decimal hoặc null | = `score_ratio` (F4-13); tên khớp `identity`; null khi `COMPILE_ERROR`, `SYSTEM_ERROR` |
| `submittedAt` | ISO-8601 UTC | thời điểm **nộp**, không phải thời điểm chấm; `identity` quy ra ngày UTC+7 (`02-bd/architecture/judge-orchestration.md:261`) |
| `firstAttemptForProblem` | boolean | `true` chỉ với lượt nộp **đầu tiên từ trước đến nay** của người dùng cho bài đó, tính từ lịch sử `submissions` cùng `user_id` và `problem_id` tại lúc ghi sự kiện (BR-JD-14) |

### 25.3 `SubmissionAccepted`

Phát khi `status -> ACCEPTED`.

| Trường `payload` | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `submissionId`, `userId`, `problemId` | UUID | |
| `language` | enum | `02-bd/architecture/judge-orchestration.md:258` |

`03-dd/api/identity.md:252` còn kỳ vọng `classId?` trong payload này. Module `judge` **không sở hữu** quan hệ bài giao và lớp, nên `classId` không có trong payload; xem Q18.

### 25.4 Khử trùng ở phía tiêu thụ

| Consumer | Khử trùng | Ghi chú |
| :--- | :--- | :--- |
| `identity` | theo `eventId` (`03-dd/api/identity.md:251`) | cập nhật `user_problem_best_score`, `user_submission_stats`, `user_daily_submission_stats` trong một giao dịch; `user_submission_stats.total_submissions` phải đếm cả `SYSTEM_ERROR` và `COMPILE_ERROR` cho khớp mẫu số ở endpoint #9 |
| `problem-bank` | theo `eventId` | cập nhật `problem_stats` (`02-bd/architecture/problem-bank.md:105`) |
| `ai-review` | theo `eventId` và khoá nghiệp vụ `submissionId` | `SubmissionAccepted` chỉ **mở điều kiện** cho F5.1 và F5.2; chạy phân tích là hành động tự chọn của học viên (`USR0102` EVT-17 dòng 806). F5 hỏng không ảnh hưởng chấm |
| Giao hàng | at-least-once | consumer bắt buộc idempotent; relay có thể publish trùng (`03-dd/jobs/judge-orchestration.md` mục 2) |

### 25.5 Sự kiện module lắng nghe

Sau khi xét từng sự kiện `[SoT: Suy luận]`, **F4 không tiêu thụ sự kiện miền nào ở bản này**:

| Sự kiện | Kết luận | Lý do |
| :--- | :--- | :--- |
| `TestcaseSetVersionBumped` (`problem-bank`) | **Không tiêu thụ** | BD (`02-bd/architecture/judge-orchestration.md:146-153`) dùng nó để ghi `testcase_set_version_at_grading`, nhưng BD database không có bảng nào lưu số hiệu phiên bản của bài ở phía `judge`, nên không có nơi ghi. Lấy `currentTestcaseSetVersion` từ `loadGradingSpec` ngay lúc worker bắt đầu `COMPILING` (cùng lượt đọc với danh sách testcase) cho đúng mốc "bắt đầu compile" và khỏi cần consumer, cache hay nguy cơ lệch thứ tự. Đề xuất quyết định, xem Q2 |
| `ProblemSpecChanged` | Không tiêu thụ | chỉ `harness` cần vô hiệu cache; `judge` không cache đặc tả. `judge` ghim `spec_version` từ `loadGradingSpec` lúc chấm |
| `ProblemPublished`, `ProblemUnpublished` | Không tiêu thụ | BD ghi "nếu có cache `language_config` theo bài" (`02-bd/architecture/problem-bank.md:68`); không có cache nên không cần. Việc "bài có nhận bài nộp hay không" kiểm trực tiếp ở endpoint #3 qua `loadGradingSpec` |
| `ClassDeleted`, `StudentRemovedFromClass` (`identity`) | Không tiêu thụ | lịch sử nộp bài được giữ (`DEC-2026-1014-keep-learning-history-on-leave`); phạm vi lớp đọc qua `ClassScopeReadPort` lúc truy vấn |

## 26. Cổng trong tiến trình cho module khác

Ba cổng vào (inbound port, `application/ports/in`) mà module khác gọi qua outbound port của chính họ, không import (`DEC-2026-0820-architecture-baseline`). Không cổng nào mở route HTTP.

### 26.1 P1 `GetClassStudentSubmissionMetrics`

Luồng giảng viên của `DEC-2026-0927-submission-metrics-two-ports`. Gọi bởi `identity` qua `ClassStudentMetricsPort` cho `INS0201`, `INS0203`, `INS0204`. **Việc gác quyền (`CLASS_MANAGEMENT` và phạm vi lớp) thuộc `identity`** (dòng 125-126 của BD kiến trúc); cổng này tin `studentIds` mà `identity` đã lọc theo lớp nên không có route HTTP.

| | |
| :--- | :--- |
| Đầu vào | `classId` (UUID, dùng cho khoá cache), `studentIds[]` (UUID), `problemIds[]` (bài đã giao cho lớp), `weekCount` (int, 1 đến 12 `[SoT: Suy luận]`) |
| Đầu ra | `[ { studentId, streakDays, lastSubmittedAt, acceptedRatio, weeklyTrend[] } ]`, một phần tử mỗi `studentId` đầu vào (kể cả học viên chưa nộp: giá trị 0 và null) |
| `weeklyTrend[]` | đúng `weekCount` phần tử, **cũ nhất trước**: `{ weekStart (thứ Hai, ngày UTC+7), submissionCount, acceptedCount, acceptedRatio (null nếu 0 lượt nộp) }` |
| Quy tắc tuần | tuần ISO bắt đầu thứ Hai; **tuần đang chạy chưa đủ 7 ngày không được tính**, nên `weekCount` là số tuần đủ gần nhất (`DEC-2026-1014-instructor-batch2-weekly-rules`, `02-bd/architecture/judge-orchestration.md:113`) |
| Phạm vi dữ liệu | chỉ các lượt nộp của `studentIds` cho `problemIds` `[SoT: Suy luận]`, kể cả chuỗi ngày và `lastSubmittedAt` (khớp BR-10: nhãn `INSUFFICIENT_DATA` xét "trong phạm vi bài đã giao"); **không** áp cửa sổ `joined_at..left_at` vì chữ ký chưa mang cửa sổ, xem Q19 |
| `acceptedRatio` | `ACCEPTED / mọi lượt nộp trạng thái cuối` trong phạm vi |
| Mốc "Vắng bài" | cửa sổ trượt 7 ngày tính từ lúc truy vấn, do `identity` tự suy từ `lastSubmittedAt`, không theo tuần ISO |
| Bộ nhớ đệm | Redis **15 phút**, khoá `judge:classmetrics:{classId}:{hash studentIds}:{hash problemIds}:{weekCount}:{weekStartCuaTuanHienTai}`; không có bảng ảnh chụp theo tuần, không job định kỳ. Mâu thuẫn với BD database mục 3 ("module không dùng Redis") được ghi ở Q20 |
| Không trả | mã nguồn, nội dung testcase, kết quả từng testcase (`INS0203` Sheet 9 NO 3 dòng 514) |

### 26.2 P2 `GetAdminSubmissionStats` (`JudgeStatsPort`)

Gọi bởi `identity` (`03-dd/logic/identity.md` BR-17: đọc đồng bộ, cache 60 giây phía `identity`, lỗi thì phần số liệu này trả `null` kèm `unavailable`). `ADM0101` không có endpoint riêng của `judge` (`02-bd/screens/admin/ADM0101_overview.md` dòng 627). Hình dạng đầu ra là đề xuất `[SoT: Suy luận]`, chốt cùng DD màn `ADM0101`:

| Trường | Ghi chú |
| :--- | :--- |
| `totalSubmissions` | tổng lượt nộp |
| `byLanguage[]` | chuỗi thời gian theo ngôn ngữ (`ADM0101` tối đa 20 mốc, 3 chuỗi) |
| `verdictCounts` | **năm** verdict `ACCEPTED`, `WRONG_ANSWER`, `TIME_LIMIT_EXCEEDED`, `RUNTIME_ERROR`, `COMPILE_ERROR` và `totalFinal`; `SYSTEM_ERROR` không có lát riêng (`ADM0101` dòng 363); bài `PENDING`, `COMPILING`, `JUDGING` không tính |
| `perDay[7]`, `perMonth[6]` | số lượt nộp theo ngày và theo tháng (UTC+7) |
| `problemSubmissionCounts[]` | `{ problemId, submissionCount, acceptedCount }` của các bài phổ biến nhất (4 dòng); `problem-bank` ghép tên bài và độ khó phía `identity` |

### 26.3 P3 `JudgeConfigReadPort`

Gọi bởi `problem-bank` qua outbound port của họ, **đọc qua cổng, không đọc thẳng bảng** (`database/judge-orchestration.md:159`).

| Thao tác | Trả về | Dùng cho |
| :--- | :--- | :--- |
| `getGeneratorSandboxLimits()` | `{ maxRuntimeSeconds, maxOutputMb }` từ `generator_sandbox_settings` | `GenerateTestcasesWithAi` (F2-14) |
| `getEnabledLanguages()` | danh sách ngôn ngữ `enabled` | `problem-bank` khi cần |
| `getGradingDefaults()` | bốn giá trị mặc định từ `grading_defaults` | `problem-bank` điền sẵn giá trị khởi tạo cho nháp bài mới (`database/problem-bank.md:42` giao DD chốt) |

Đọc DB mỗi lần gọi, không cache tĩnh dài hạn (`02-bd/security/judge-orchestration.md:77-80`). Chữ ký thật do DD `problem-bank` chốt; DD này chỉ cam kết nội dung.

## 27. Danh mục mã lỗi

Mã chung: `401` thiếu hoặc hết hạn token (mã chung của hệ thống, không thuộc `JDG`).

**Bảng 5: Mã lỗi `JDG-nnn` (nghiệp vụ, hạ tầng) và `JDG-Vnn` (kiểm đầu vào)**

| Mã | HTTP | Ý nghĩa | Endpoint |
| :--- | :-: | :--- | :--- |
| `JDG-101` | 404 | Không tìm thấy bài toán (chưa xuất bản, đã ẩn mềm hoặc không tồn tại) | #1, #2, #3 |
| `JDG-102` | 422 | Ngôn ngữ đang tắt, không nhận bài nộp mới | #2, #3 |
| `JDG-103` | 422 | Bài chỉ hỗ trợ Standard I/O, không nhận mô hình Bọc hàm | #2, #3 |
| `JDG-104` | 422 | Bài không có testcase đã duyệt để chấm | #3 |
| `JDG-105` | 422 | Bài không có testcase Sample để chạy thử | #2 |
| `JDG-106` | 429 | Vượt số lượt nộp tối đa trong một giờ | #3 |
| `JDG-110` | 404 | Không tìm thấy bài nộp (không có, của người khác, hoặc ngoài phạm vi lớp) | #5, #6, #11 |
| `JDG-111` | 404 | Không tìm thấy lớp (không phải lớp của giảng viên, hoặc `classId` sai định dạng) | #10, #12 |
| `JDG-112` | 404 | Học viên không thuộc lớp | #12 |
| `JDG-201` | 409 | Trạng thái cụm đã bị người khác đổi; tải lại | #16, #17 |
| `JDG-202` | 409 | Cấu hình đã được người khác cập nhật; tải lại | #22 |
| `JDG-203` | 422 | Phải giữ ít nhất một ngôn ngữ đang bật | #22 |
| `JDG-204` | 422 | Giới hạn thời gian hiệu dụng ngoài 100 đến 30.000 ms | #22 |
| `JDG-205` | 422 | Giới hạn bộ nhớ hiệu dụng ngoài 64 đến 1.024 MB | #22 |
| `JDG-301` | 403 | Không đủ quyền (thiếu Function hoặc vai trò) | #10 đến #22 |
| `JDG-501` | 503 | Judge engine không dùng được sau khi thử lại, hoặc vượt hạn tổng của lượt chạy thử | #2 |
| `JDG-502` | 429 | Cụm đang nhận quá số lượt chạy thử đồng thời | #2 |
| `JDG-503` | 503 | Hàng đợi chấm không nhận được bài nộp | #3 |
| `JDG-V01` | 400 | Mã nguồn rỗng hoặc chỉ gồm khoảng trắng | #2, #3 |
| `JDG-V02` | 400 | Mã nguồn quá 65.536 byte UTF-8 | #2, #3 |
| `JDG-V03` | 400 | `language` không hợp lệ | #1, #2, #3 |
| `JDG-V04` | 400 | `submissionModel` không hợp lệ | #2, #3 |
| `JDG-V05` | 400 | `sourceInputMethod` không hợp lệ | #3 |
| `JDG-V06` | 400 | Tệp tải lên thiếu tên hoặc sai phần mở rộng so với ngôn ngữ | #3 |
| `JDG-V07` | 400 | `range` không thuộc `7d`, `30d`, `all` | #9 |
| `JDG-V09` | 400 | Tham số hàng đợi không phải số nguyên từ 1 trở lên | #19 |
| `JDG-V10` | 400 | Hệ số thời gian hoặc bộ nhớ ngoài 0,10 đến 9,99 hoặc quá 2 chữ số thập phân | #22 |
| `JDG-V11` | 400 | Giới hạn mặc định không phải số nguyên từ 1 trở lên | #22 |
| `JDG-V12` | 400 | Giới hạn script sinh dữ liệu ngoài khoảng | #22 |
| `JDG-V13` | 400 | Khoảng ngày ngược (`from` sau `to`) | #20 |
| `JDG-V14` | 400 | `limit` ngoài khoảng | #4, #8 |
| `JDG-V15` | 400 | `topicIds` sai định dạng hoặc quá 50 phần tử | #9 |
| `JDG-V16` | 400 | Thiếu `paused`, `enabled` hoặc giá trị mong đợi | #16, #17 |

Khoảng đã dùng: `JDG-101` đến `JDG-503` (18 mã nghiệp vụ và hạ tầng) và `JDG-V01` đến `JDG-V16` (15 mã, bỏ `V08`). `JDG-V08` để trống có chủ ý (dành chỗ, không đánh số lại các mã đã trích).

## 28. Câu hỏi mở và đề xuất cần chủ dự án quyết

| # | Câu hỏi | Đề xuất của DD | Căn cứ |
| :-: | :--- | :--- | :--- |
| Q1 | `USR0102` mô tả một kênh khoá theo `submissionId`, `USR0201` Q1 chốt đích người dùng. Hợp nhất | Một đích người dùng `/user/queue/submissions`, mỗi bản tin mang `submissionId` (mục 2.2) | `USR0201` dòng 615; `USR0102` dòng 764 |
| Q2 | BD để `judge` nghe `TestcaseSetVersionBumped` nhưng không có chỗ lưu | Đọc `currentTestcaseSetVersion` từ `loadGradingSpec` lúc bắt đầu `COMPILING`, bỏ consumer | mục 25.5; `02-bd/architecture/judge-orchestration.md:146-153` |
| Q3 | `JudgeExecutionPort` chỉ có `run` trong BD nhưng state machine có `COMPILING` quyết định một lần trước vòng testcase | Thêm thao tác `compile` vào cổng, vẫn trung lập theo engine; `run` vẫn một lần mỗi testcase | `DEC-2026-0912-judge-callback-contract` điểm 1 |
| Q4 | Tạo `PENDING` rồi publish RabbitMQ là hai lần ghi không chung giao dịch; publish lỗi để lại dòng `PENDING` mồ côi | Trả `503 JDG-503`, sweep dọn sau 5 phút; hoặc thêm giá trị `failure_reason` (ví dụ `QUEUE_UNAVAILABLE`) để kết thúc ngay | `database/judge-orchestration.md:27` (enum chỉ có hai giá trị) |
| Q5 | Không có giới hạn tần suất Run Code trong BD; Run Code dùng chung cụm với chấm bài | Trần đồng thời toàn cục 8 và (nếu chủ dự án muốn) giới hạn theo người, ví dụ 30 lượt mỗi giờ | tiêu chí ponytail bậc 6 (an toàn) |
| Q6 | `stderr_snippet` lưu cho mọi testcase `verdict != AC`; stderr của testcase ẩn có thể in lại input (ví dụ lỗi chuyển kiểu của Python), trái quy tắc cứng | Chỉ trả `stderrSnippet` cho testcase `SAMPLE`, dù DB vẫn lưu | `02-bd/security/judge-orchestration.md:60-63` nói stderr "không chứa nội dung testcase" nhưng không bảo đảm được |
| Q7 | `submission_testcase_results` không có cột chụp `visibility` lúc chấm; tra lại lúc đọc nên đổi `HIDDEN` thành `SAMPLE` hoặc xoá testcase làm đổi cách hiển thị bài cũ | Thêm cột `is_sample BOOLEAN` chụp lúc chấm; tạm thời: tra khi đọc, không tra được thì coi là `HIDDEN` | `database/judge-orchestration.md:48-66` |
| Q8 | `total_testcase_count` là `NULL` tới lúc chấm nhưng `USR0102` EVT-11 muốn dựng sẵn dải ô ngay khi nộp; ngoài ra cột ghi "testcase Hidden" trong khi chấm chạy mọi testcase đã duyệt (cả Sample) | Thêm `totalCountHint` vào phản hồi tạo bài nộp; đọc cột `total_testcase_count` là **mọi testcase đã duyệt** và sửa câu chữ BD | `database/judge-orchestration.md:30`; `USR0102` dòng 800 |
| Q9 | Máy chủ cần `fileName` để kiểm phần mở rộng tệp, BD không có trường này | Thêm `fileName` có điều kiện khi `FILE_UPLOAD` | `USR0102` Sheet 9 NO 5 dòng 830 |
| Q10 | Câu chữ `USR0102` dòng 764 lệch quyết định `USR0201` Q1 | Sửa câu chữ BD theo Q1 | |
| Q11 | Lô `/topic/admin/queue` có cần mang luôn dòng của bảng job không | Không; client gọi lại #14 theo tab và trang đang xem | `ADM0401` EVT-3 dòng 640 |
| Q12 | `GetEffectiveExecutionLimits` trả cho ngôn ngữ đang chọn hay cả ba | Trả cả ba (hoặc một nếu có `language`), một lần gọi đủ dựng cả ba tab và cờ `enabled` | `USR0102` Sheet 7.3 NO 5 |
| Q13 | "Bài đang làm dở" tính cả bài từng `ACCEPTED` rồi nộp lại sai | Theo đúng chữ định nghĩa (bài nộp gần nhất chưa `ACCEPTED`) | `USR0101` Q4 dòng 647 |
| Q14 | Chuỗi ngày về 0 khi hôm nay chưa nộp; mẫu số tỉ lệ AC có tính `SYSTEM_ERROR` không | Theo đúng chữ RD (không ân hạn) và tính mọi trạng thái cuối cho khớp `SubmissionGraded`; chủ dự án xác nhận có muốn ân hạn một ngày không | `01-rd/screens/users/USR0501_my_progress.md:75` |
| Q15 | `INS0206` loại bài nộp làm trước lần giao đầu tiên của bài cho lớp, `INS0204` thì không; hai màn đếm khác nhau | Thống nhất một quy tắc; DD hiện theo đúng chữ BD từng màn | `INS0206` dòng 213; `INS0204` dòng 666 |
| Q16 | Ai thêm worker khi bật autoscale (Docker Compose không có autoscaler sẵn) | DD chỉ lưu cờ và ngưỡng; thành phần mở rộng là việc hạ tầng, cần quyết định riêng | `ADM0401` Q8 |
| Q17 | `PUT /admin/queue/settings` không kiểm xung đột phiên bản, khác `PUT /admin/language-config` | Giữ như BD (người ghi sau thắng) | `ADM0401` Sheet 9 NO 8, NO 9 |
| Q18 | `identity` kỳ vọng `classId?` trong `SubmissionAccepted`, `judge` không sở hữu quan hệ bài giao | Bỏ `classId` khỏi payload; `identity` suy phạm vi lớp từ dữ liệu của chính nó và `problem-bank` | `03-dd/api/identity.md:252` |
| Q19 | Cổng thống kê lớp không mang cửa sổ `joined_at..left_at`, khác quy tắc của `INS0206` | Giữ chữ ký BD; đề xuất tuỳ chọn thêm cửa sổ từng học viên nếu cần khớp | `02-bd/architecture/judge-orchestration.md:104-107` |
| Q20 | BD kiến trúc ghi cache Redis 15 phút cho thống kê lớp, BD database nói module không dùng Redis | Theo quyết định mới hơn (`DEC-2026-1014-instructor-batch2-weekly-rules`), sửa BD database mục 3 | |
| Q21 | Verdict vượt giới hạn output (`OUTPUT_LIMIT`) chưa có giá trị riêng; thiếu giá trị `failure_reason` cho "đặc tả bài hoặc testcase không dùng được lúc chấm" (`HRN-004`, `HRN-005`, `HRN-006`, `HRN-050`, mã thoát 90) | Gộp vượt output vào `RE`; dùng `SANDBOX_FAULT` tạm cho các lỗi dữ liệu bài, đề xuất thêm giá trị enum (ví dụ `SPEC_UNAVAILABLE`) | `database/judge-orchestration.md:27,60` |
| Q22 | Lỗi biên dịch nằm trong mã khung (lỗi của harness) hiện trả thông báo chung với trạng thái `COMPILE_ERROR`, tính vào tỉ lệ AC như lỗi của học viên | Đề xuất chuyển sang `SYSTEM_ERROR` + `SANDBOX_FAULT`; chủ dự án chốt | `02-bd/architecture/harness.md:272-276` |
| Q23 | `harness` trả `diagnostics[]` có cấu trúc và `fixedMessageKey`, còn `submissions.compile_error_message` là một chuỗi văn bản | `judge` dựng chuỗi từ `diagnostics[]` (mỗi dòng `Dòng {line}: {message}`) và dùng khuôn tiếng Việt cố định cho `fixedMessageKey`; chủ dự án xác nhận khuôn câu và việc lưu văn bản đã dựng thay vì khoá i18n | `03-dd/api/harness.md` mục 6.1 |
| Q24 | `harness` trả `HRN-002` khi đặc tả đổi giữa lúc F4 đọc và lúc `prepare` (câu hỏi Q3 của `03-dd/api/harness.md`) | Đọc lại đặc tả và gọi lại `prepare` một lần; chấm cả bài bằng đúng một `PreparedProgram`; không chấm lại bài cũ khi đặc tả đổi sau đó (không có chấm lại) | `03-dd/api/harness.md` mục 7.2 Q3; `DEC-2026-0828-remove-rejudge-scope` |

## 29. Tham chiếu

- `01-rd/req/judge-orchestration.md` (F4-01 đến F4-13); `02-bd/architecture|database|security/judge-orchestration.md`.
- `02-bd/screens/users/USR0102_problem_detail.md` Sheet 7.3, 8, 9; `USR0201_submission_result.md` Sheet 7.3, 9 và vùng giảng viên; `USR0202`, `USR0501`, `USR0601`, `USR0101`, `USR0105`; `02-bd/screens/teacher/INS0204`, `INS0206`; `02-bd/screens/admin/ADM0401`, `ADM0404`, `ADM0501`, `ADM0101`.
- `03-dd/jobs/judge-orchestration.md` (JOB-JD-01 đến 04); `03-dd/api/identity.md` mục 2 (phong bì sự kiện); `03-dd/logic/identity.md` BR-17, BR-24.
- Quyết định: `DEC-2026-0823-go-judge-default-engine`, `DEC-2026-0912-judge-callback-contract`, `DEC-2026-0912-judge-outbox-pattern`, `DEC-2026-0831-partial-score-testcase-ratio`, `DEC-2026-0828-remove-rejudge-scope`, `DEC-2026-0927-submission-metrics-two-ports`, `DEC-2026-1010-admin-operations-regroup`, `DEC-2026-1018-usr-open-questions-phase2-5`, `DEC-2026-1023-db-actor-review-decisions`.
