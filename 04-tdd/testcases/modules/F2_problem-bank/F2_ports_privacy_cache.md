# Test case QA — F2: cổng đọc, quyền, cache và bài lớp học viên

> 11 ca thiết kế; TDD F2 AC-81–88, DD API các cổng P và §5A, BR-02/05/06/11/13/25. Chưa thực thi. Dùng marker riêng cho từng bí mật Hidden để quét phản hồi/log.

### TC-F2-AC81-01 — Cổng chấm chỉ trả bộ đã duyệt và vẫn đọc được sau rút
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-81; DD ProblemGradingSpecPort, BR-11/13 |
| Kind / priority | Normal + security, P0 |
| Environment | Port integration, DB/snapshot |
| Preconditions | X published có 8 Hidden + 2 Sample approved, 2 AI draft; specVersion=3, setVersion=5; sau đó withdrawn. |
| Test data | Bài X; gọi `loadGradingSpec(X)`, `loadTestcaseSnapshot(X,5)`, `loadSampleTestcases(X)` trước/sau khi X chuyển `UNPUBLISHED`; 10 ca approved có position 1..10, hai draft dùng ID riêng ngoài snapshot. |
| Steps | `loadGradingSpec`, `loadTestcaseSnapshot(X,5)`, `loadSampleTestcases`; rút bài và đọc lại snapshot v5. |
| Expected | Grading spec có đúng 10 metadata testcase approved và hai version đồng bộ, không kèm nội dung; snapshot v5 có nội dung đúng 10 ca approved kể cả Hidden, không có hai draft; Sample chỉ có 2 approved. Sau rút `published=false`, lượt đã queue vẫn đọc bundle v5; nộp mới bị từ chối tại F4. |
| Cleanup | Dọn fixture |
| Evidence | port DTO, snapshot hash, queue trace. |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC82-01 — Đổi đặc tả làm mới view và phát event
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-82; DD ProblemSpecQuery, BR-05 |
| Kind / priority | Cache invalidation, P0 |
| Environment | Port, Redis, DB, Outbox |
| Preconditions | X specVersion=3 và view đủ Bảng 4 F3; Y chưa có spec; cache Redis chứa X v3. |
| Test data | `findSpec(X)`, `findSpec(Y)`; sửa `functionName` của X từ `solve` thành `solveV2` qua F2, kỳ vọng `specVersion=4`, rồi gọi lại `findSpec(X)`. |
| Steps | `findSpec(X/Y)`; đổi spec X; gọi `findSpec(X)` lần nữa. |
| Expected | X đầu có đủ fields Bảng 4 và `functionWrapperSupported`, Y rỗng; sau đổi Redis entry cũ bị xoá, view mới specVersion>3 và nội dung mới; `ProblemSpecChanged` có problemId=X, specVersion mới. |
| Cleanup | Dọn fixture |
| Evidence | DTO trước/sau, Redis, Outbox. |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC83-01 — Ngữ cảnh AI có ba trường công khai, không có bí mật chấm
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-83; DD ProblemContextPort, BR-06 |
| Kind / priority | Security, P0 |
| Environment | Port, DB |
| Preconditions | X có ai_brief marker B, noFullCode=true, socraticOnly=false, reference source marker R, Hidden marker H và object_key K. |
| Test data | `aiBrief="QA_AI_BRIEF"`, `referenceSource="QA_REFERENCE_SECRET"`, Hidden input/output chứa `QA_HIDDEN_SECRET`, object key chứa `QA_OBJECT_KEY`; gọi `getContext(X)` và quét cả JSON tuần tự hoá. |
| Steps | `getContext(X)`, kiểm schema/serialize và marker. |
| Expected | Có `aiBrief=B,noFullCode=true,socraticOnly=false`; không có mã đáp án, testcase, object_key hay marker R/H/K. |
| Cleanup | Dọn fixture |
| Evidence | DTO/schema dump đã che bí mật. |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC84-01 — Bài rút và assignment đã gỡ vẫn có trong cổng lịch sử
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-84; DD ProblemSummaryReadPort, ClassAssignmentReadPort |
| Kind / priority | History consistency, P0 |
| Environment | Hai port, DB, F4 consumer |
| Preconditions | X đã xoá mềm (`deleted=true`, `status=UNPUBLISHED`) nhưng còn submission cũ; X từng giao L và `removed_at=T0`; Y `PUBLISHED`, còn giao L. |
| Test data | `getSummaries([X,Y])`, `listAssignments([L])`; X có mã `qa-history-x`, tên `QA History X`, submission lúc T0-1d và assignment lúc T0-2d; Y có assignment `removed_at=null`. |
| Steps | Gọi `getSummaries([X,Y])`, `listAssignments([L])`; so DTO với hai dòng `problems` và `class_assignments` đã seed. |
| Expected | Summary X vẫn có `problemId=X`, `code=qa-history-x`, `title=QA History X`, `withdrawn=true`; Y `withdrawn=false`. Assignments trả cả dòng X `removedAt=T0` và Y `removedAt=null`, không tự lọc X khỏi cổng lịch sử. |
| Cleanup | Dọn fixture |
| Evidence | hai port DTO, dòng bài/assignment/submission đã seed. |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC84-02 — Quan hệ chủ đề lịch sử giữ bài xoá, tiến độ hiện hành loại bài đó

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-84; DD API §4 và §58 `ProblemTopicReadPort`; logic F2 BR-13 |
| Kind / priority | History consistency, P0: không làm mất lịch sử hoặc tăng sai tiến độ hiện hành |
| Environment | F2 port integration + `GET /topics/progress`, PostgreSQL, SolveStatePort stub |
| Preconditions | Chủ đề T có đúng hai bài X/Y; X `deleted=true`, `status=UNPUBLISHED`, Y `PUBLISHED`, `deleted=false`; học viên S có submission Accepted cũ của X, chưa giải Y |
| Test data | `problem_topics` có `(X,T)` và `(Y,T)`; `getProblemIdsByTopics([T])`; `SolveStatePort.listMyProblemSolveStates(S)` trả `{X:SOLVED}`; token S gọi `GET /topics/progress` |
| Steps | Gọi cổng lịch sử với `[T]`; gọi API tiến độ bằng S; đối chiếu kết quả với quan hệ DB. |
| Expected | Cổng lịch sử trả T chứa cả X/Y, không xoá quan hệ X–T; API `200`, item T có `total=1`, `solved=0`, `solveStateAvailable=true` vì chỉ Y còn xuất bản. Không đưa X vào tử số hoặc mẫu số hiện hành. |
| Cleanup | Dọn fixture |
| Evidence | port DTO, API response, `problem_topics` và trạng thái bài |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC85-01 — Quét tất cả endpoint học viên không rò Hidden
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-85; DD BR-13, API §2–14 |
| Kind / priority | Security, P0 |
| Environment | Student API, INFO log collector, DB/MinIO |
| Preconditions | Trong fixture cô lập, X là bài `PUBLISHED` duy nhất trong catalog, có Hidden INLINE marker HI/HO và MINIO marker MI/MO, object key K, hai Sample approved; học viên S có quyền đọc bài, thuộc một lớp đang giao X. Cổng gợi ý trả X; bookmark của S ban đầu rỗng. |
| Test data | Hidden INLINE input/output lần lượt `QA_HI_SECRET`/`QA_HO_SECRET`; Hidden MINIO input/output `QA_MI_SECRET`/`QA_MO_SECRET`, object key chứa `QA_OBJECT_KEY_SECRET`. Token S gọi đủ 15 route DD Bảng 0.1: `GET /problems`, `/problems/random`, `/topics/progress`, `/me/class-assignments`, `/problems/{X}/workspace`, `/problems/{X}/overview`, `/problems/{X}/sample-testcases`, `/me/bookmarks`, `/suggested-problems`, `/catalog/summary`, `/me/class-problems`, `/me/levels/progress`; `PUT /me/bookmarks/{X}` body rỗng, `DELETE /me/bookmarks/{X}` body rỗng, `PUT /me/bookmarks/{X}/state` body `{"bookmarked":true}`. Route #13A/#13B được tính riêng, không bỏ vì được bổ sung sau số #13. |
| Steps | Gửi `PUT /me/bookmarks/{X}` trước để `GET /me/bookmarks` thật sự trả X; gọi các GET còn lại (random/gợi ý đều trỏ X), rồi `PUT /me/bookmarks/{X}/state` và `DELETE /me/bookmarks/{X}`; ghi `requestId`, status và body của đủ 15 request. Serialize JSON sâu và quét INFO logs theo requestId, quét cả tên field nhạy cảm và năm marker bí mật. |
| Expected | Mỗi request được xử lý theo hợp đồng route; không response/log học viên chứa `QA_HI_SECRET`, `QA_HO_SECRET`, `QA_MI_SECRET`, `QA_MO_SECRET`, `QA_OBJECT_KEY_SECRET` hoặc presigned URL. DTO học viên không chứa `input`/`expectedOutput` của Hidden; INFO log của Hidden chỉ có `testcaseId` và độ dài, không nội dung. |
| Cleanup | Dọn bookmark/fixture |
| Evidence | giữ bảng 15 requestId/status, schema scan từng DTO và log scan theo requestId. |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC86-01 — Ma trận quyền Function được cưỡng chế ở server

#### Fixture ma trận quyền AC-86

- Mỗi hàng trong bảng dưới dùng **bản sao dữ liệu riêng** để thao tác thành công không đổi tiền điều kiện hàng sau. Thay `L1/L2`, `T1/T2`, `P0/P1/P2`, `TC1/TC2`, `C1`, `U1` bằng UUID thật do fixture tạo và lưu ánh xạ trong run record. `L1/L2` là hai mức chưa bị tham chiếu ở ca xoá; `T1/T2` là hai chủ đề chưa bị tham chiếu ở ca xoá. `P0` là bài nháp thuộc người gọi; `P1` là bài đã xuất bản có đề/ràng buộc `0<=n<=100`, limits hợp lệ, spec Standard I/O, 2 ví dụ mẫu, 2 Sample + 6 Hidden được duyệt, reference Python đọc `n` in `n+1` đạt 8/8; `P2` có cùng bộ dữ liệu đủ checklist như `P1` nhưng còn `UNPUBLISHED`. `TC1/TC2` là hai testcase thủ công đã duyệt của `P0`, ở position 1/2. `C1` là lớp còn hoạt động do giảng viên kiểm soát; `U1` là upload session mới của chính giảng viên đó với hai object chứa `1\n`/`2\n` (mỗi object 2 byte) đã PUT xong và HEAD thấy được. Trước từng hàng ghi `updatedAt` hiện hành là `T0`.
- `PA-R`: giảng viên sở hữu bài, chỉ có `PROBLEM_AUTHORING:READ`; `PA-F`: **cùng subject** sau khi cấp thêm `CREATE/UPDATE/DELETE`; `TC-R`: chủ bài, chỉ có `TESTCASE_MANAGEMENT:READ`; `TC-F`: cùng subject sau khi cấp thêm `CREATE/UPDATE/DELETE`; `TC-0`: cùng subject khi chưa có quyền TESTCASE_MANAGEMENT; `CL-R`: phụ trách `C1`, chỉ có `CLASS_MANAGEMENT:READ`; `CL-U`: cùng subject sau khi cấp `UPDATE`; `CFG-R`: ADMIN chỉ có `AI_CONFIG:READ`; `CFG-U`: cùng subject sau khi cấp `UPDATE`; `CAT-F`: ADMIN có `PROBLEM_AUTHORING:CREATE/UPDATE/DELETE`. Ở mũi tên R/0 → F/U, làm mới JWT và cache quyền từ F1 **sau** assertion 403 rồi mới gửi request đối chứng. Các actor không có quyền tương đương khác.
- `CONTENT-NEW={"title":"QA quyền tạo bài","statementMd":"Đọc n, in n+1"}`. `CONTENT-EDIT={"title":"QA quyền sửa bài","statementMd":"Đọc n, in n+1","constraintsMd":"0<=n<=100","levelId":L1,"limits":{"timeLimitMs":1000,"memoryLimitMb":128,"maxOutputSizeKb":1024,"maxSubmissionsPerHour":60},"topicIds":[],"tags":[],"referenceSolution":null,"workedExamples":[],"expectedUpdatedAt":T0}`. `SPEC={"functionName":null,"returnType":null,"parameters":null,"nameOverrides":{},"matchingStrategy":"EXACT","epsilonValue":null,"stdinFormatMd":"một số nguyên","stdoutFormatMd":"một số nguyên","expectedUpdatedAt":T0}`. `P0` là nháp nên danh sách ví dụ của `CONTENT-EDIT` được phép rỗng.
- Bảng ghi **một request hợp lệ** cho mỗi route, dùng cùng bản sao fixture và cùng body cho actor thiếu/đủ quyền. Thứ tự mỗi hàng: chụp DB/MinIO/Redis/Outbox, gửi bằng actor thiếu quyền, kiểm `403 PRB-002` và không mutation, rồi gửi cùng request bằng actor đủ quyền, kiểm 2xx và delta nghiệp vụ theo DD của route. Với POST tạo/sinh và upload, positive chạy một lần trên bản sao độc lập. Không dùng kết quả của hàng trước làm fixture.

| # DD | Request hợp lệ trên `/api/v1/problem-bank` | Thiếu → đủ quyền |
| :--- | :--- | :--- |
| 15 | `POST /levels` `{"displayName":"QA Level 3"}` | PA-R → CAT-F |
| 16 | `PATCH /levels/{L1}` `{"displayName":"QA Level mới"}` | PA-R → CAT-F |
| 17 | `PUT /levels/order` `{"orderedIds":[L2,L1]}` | PA-R → CAT-F |
| 18 | `DELETE /levels/{L1}` không body | PA-R → CAT-F |
| 20 | `POST /topics` `{"name":"QA Topic 3"}` | PA-R → CAT-F |
| 21 | `PATCH /topics/{T1}` `{"name":"QA Topic mới"}` | PA-R → CAT-F |
| 22 | `PUT /topics/order` `{"orderedIds":[T2,T1]}` | PA-R → CAT-F |
| 23 | `DELETE /topics/{T1}` không body | PA-R → CAT-F |
| 28 | `POST /authoring/problems/bulk/publish` `{"problemIds":[P2]}` | PA-R → PA-F |
| 29 | `POST /authoring/problems/bulk/unpublish` `{"problemIds":[P1]}` | PA-R → PA-F |
| 30 | `POST /authoring/problems/bulk/difficulty` `{"problemIds":[P0],"levelId":L2}` | PA-R → PA-F |
| 31 | `POST /authoring/problems/bulk/topic` `{"problemIds":[P0],"topicId":T2}` | PA-R → PA-F |
| 32 | `POST /authoring/problems/bulk/delete` `{"problemIds":[P0]}` | PA-R → PA-F |
| 35 | `POST /authoring/problems` `CONTENT-NEW` | PA-R → PA-F |
| 36 | `PUT /authoring/problems/{P0}/content` `CONTENT-EDIT` | PA-R → PA-F |
| 37 | `PUT /authoring/problems/{P0}/spec` `SPEC` | PA-R → PA-F |
| 38 | `PUT /authoring/problems/{P0}/ai-context` `{"briefText":"QA brief","noFullCode":true,"questionOnly":false,"expectedUpdatedAt":T0}` | PA-R → PA-F |
| 39 | `PUT /authoring/problems/{P1}/status` `{"status":"UNPUBLISHED","expectedUpdatedAt":T0}` | PA-R → PA-F |
| 41 | `POST /authoring/problems/{P0}/testcases` `{"visibility":"HIDDEN","input":"3\n","expectedOutput":"4\n"}` | TC-R → TC-F |
| 42 | `PATCH /authoring/problems/{P0}/testcases/{TC1}` `{"input":"5\n","expectedOutput":"6\n"}` | TC-R → TC-F |
| 43 | `DELETE /authoring/problems/{P0}/testcases/{TC1}` không body | TC-R → TC-F |
| 44 | `PUT /authoring/problems/{P0}/testcases/order` `{"orderedIds":[TC2,TC1]}` | TC-R → TC-F |
| 45 | `POST /authoring/problems/{P0}/testcases/bulk-upload-url` `{"items":[{"clientKey":"qa-1","inputSizeBytes":2,"expectedOutputSizeBytes":2,"visibility":"HIDDEN"}]}` | TC-R → TC-F |
| 46 | `POST /authoring/problems/{P0}/testcases/bulk-upload/confirm` `{"uploadId":U1}` | TC-R → TC-F |
| 47 | `POST /authoring/problems/{P1}/reference-solution/run` `{}` | TC-0 → TC-F |
| 48 | `POST /authoring/problems/{P1}/testcases/generate` `{}`; AI stub sinh input `3\n`, reference in `4\n`, còn lượt sinh | TC-R → TC-F |
| 52 | `POST /instructor/classes/{C1}/assignments` `{"problemIds":[P1]}` | CL-R → CL-U |
| 53 | `DELETE /instructor/classes/{C1}/assignments/{P1}` không body; seed assignment active | CL-R → CL-U |
| 56 | `PUT /settings/ai-testcase-generation` `{"maxPerProblem":3}` | CFG-R → CFG-U |
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-86; DD BR-02 |
| Kind / priority | Authorization, P0 |
| Environment | API, auth fixture, DB |
| Preconditions | Fixture ma trận ở trên; mỗi hàng có subject sở hữu bài hoặc phụ trách lớp và bản sao dữ liệu riêng. |
| Test data | Đủ 29 request, body và cặp actor thiếu/đủ quyền trong bảng #15–56 ở trên; UUID/T0 thay bằng giá trị fixture thực và giữ nguyên giữa hai request của một hàng. |
| Steps | Với từng hàng: gửi request bằng actor thiếu quyền, đọc response và chụp delta; cấp đúng action còn thiếu cho cùng subject, làm mới JWT/cache quyền, gửi lại **cùng** request. |
| Expected | Lần đầu từng hàng `403 PRB-002` trước mutation: DB, MinIO, Redis cache và Outbox không đổi. Lần đối chứng là 2xx và có hiệu ứng nghiệp vụ theo mục DD của route; không nhận `PRB-002`. Ghi status và delta riêng từng hàng, không suy kết quả một route từ route khác. |
| Cleanup | Thu hồi grants thử, dọn fixture từng hàng và upload/AI stub, khôi phục cấu hình #56 |
| Evidence | lưu 58 request/response, claims/cache quyền trước–sau cấp, DB/MinIO/Redis/Outbox delta. |
| Design review | Đã review ma trận 29 route/body/actor; chưa thực thi |

### TC-F2-AC87-01 — Rút bài vô hiệu cache ngay và suy giảm khi Redis lỗi
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-87; DD BR-25 |
| Kind / priority | Cache + recovery, P0 |
| Environment | API, Redis, DB |
| Preconditions | X published hiện trong cache `GET /problems`, cache version=v; Y published khác. |
| Test data | Token S; cache seed trang danh sách chứa X/Y với version v, rút X ở T0; hai request `GET /problems` cùng query trước và sau khi Redis bị ngắt. |
| Steps | Rút X; GET danh sách ngay; cho Redis unavailable rồi GET lại. |
| Expected | Version cache tăng, GET đầu không có X; khi Redis lỗi vẫn trả từ DB có Y và không có X, không phục hồi dữ liệu cache cũ. |
| Cleanup | Khôi phục Redis |
| Evidence | responses, cache key/version, DB. |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC88-01 — Bốn lớp, bài rút giữ tiến độ và bài gỡ biến khỏi danh sách
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-88; DD §5A |
| Kind / priority | Normal, P0 |
| Environment | Student API, F1 class port, F4 progress port |
| Preconditions | S thuộc L1–L4; L1 có P published, W withdrawn nhưng assignment active và S đã Accepted W, G removed; các lớp khác có bài active. |
| Test data | Token S; `GET /me/class-problems` không query và `GET /me/class-problems?classId=L1`; L1 có P/W/G, L2–L4 mỗi lớp có một bài published active. |
| Steps | GET `/me/class-problems` không filter và với classId=L1. |
| Expected | Response không lọc có đủ 4 lớp, không giới hạn 3 nhóm; W vẫn có dòng `withdrawn=true` và trạng thái đã giải; G vắng mặt; filter L1 chỉ có bài của L1. |
| Cleanup | Dọn fixture |
| Evidence | hai response, class/assignment rows. |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC88-02 — classId ngoài lớp và UUID sai cùng không tiết lộ dữ liệu
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-88; DD §5A |
| Kind / priority | Security, P0 |
| Environment | Student API |
| Preconditions | S thuộc L1; L2 thuộc người khác, có bài marker X. |
| Test data | Token S; ba query `classId=L2`, `classId=00000000-0000-4000-8000-000000000099` (UUID chưa tồn tại), `classId=L1&studentId=<ID người khác>`; L2 có bài mang marker `QA_FOREIGN_CLASS_SECRET`. |
| Steps | S GET với classId=L2; UUID không tồn tại; thêm query studentId của người khác. |
| Expected | Hai classId đều `404 PRB-015`, không có marker X; API không nhận studentId để đọc hộ, kết quả luôn gắn S. |
| Cleanup | Dọn fixture |
| Evidence | ba response và access logs. |
| Design review | Đã review; chưa thực thi |

### TC-F2-AC81-02 — Snapshot ghim không bị draft hay phiên bản hiện hành chen vào
| Field | Value |
| :--- | :--- |
| AC / DD source | AC-81; DD ProblemGradingSpecPort, BR-11 |
| Kind / priority | Consistency, P0 |
| Environment | Port, DB/snapshot, F4 queue |
| Preconditions | Submission S ghim setVersion=4 có 10 ca; X hiện setVersion=5 có thêm 1 approved và 2 draft. |
| Test data | `submissionId=S`, `problemId=X`, `testcase_set_version_at_grading=4`; snapshot v4 có 10 ca ID T1..T10, v5 có T1..T11, hai draft D1/D2 ngoài cả hai snapshot. |
| Steps | F4 xử lý S qua `loadTestcaseSnapshot(X,4)`; đọc `loadGradingSpec(X)` rồi snapshot v5 cho lượt nộp mới. |
| Expected | S đọc đúng 10 ca của v4, không ca v5/draft; lượt mới nhận v5 gồm 11 approved. SpecVersion và testcaseSetVersion trong mỗi view khớp snapshot tương ứng. |
| Cleanup | Dọn fixture |
| Evidence | snapshot hashes, port DTO, submission result. |
| Design review | Đã review; chưa thực thi |
