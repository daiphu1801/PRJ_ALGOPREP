# DD API — Module `problem-bank` (F2)

> Nguồn: `01-rd/req/F2_problem-bank.md` (F2-01 tới F2-18) · `02-bd/architecture/F2_problem-bank.md` · `02-bd/database/F2_problem-bank.md` ·
> `02-bd/security/F2_problem-bank.md` · `02-bd/storage/F2_problem-bank.md` · Sheet 7.3 của các BD màn hình (`SHR0201`, `SHR0202`, `SHR0203`,
> `USR0101` tới `USR0104`, `USR0201`, `USR0601`, `INS0101`, `INS0202`, `ADM0303`) · `03-dd/api/api.md` (catalog, do controller dựng sau).
> Bounded Context: F2 `problem-bank`, schema `problem` · Cập nhật: 2026-10-23. Đã đối chiếu tên cổng và sự kiện với `03-dd/api/F3_harness.md` và `03-dd/api/F4_judge-orchestration.md` (mục 25, 26); điểm lệch ở Bảng 61.3.
>
> DD này **không lặp lại** nội dung đã chốt ở BD. Phần BD chưa nói được là việc của file này: path và method (BD màn chỉ đặt tên nghiệp vụ,
> `02-bd/screens/shared/SHR0201_problem_management.md:706`), hợp đồng request/response, mã lỗi `PRB-nnn`, tên và chữ ký các cổng, payload sự kiện.
> Nơi nào DD phải chọn khi BD để ngỏ hoặc hai nguồn BD lệch nhau, chỗ đó có nhãn `[SoT: Suy luận]` kèm lý do và được gom ở mục 61 (câu hỏi mở và đề xuất).
> Quy tắc nghiệp vụ nằm ở `03-dd/logic/F2_problem-bank.md` (BR-nn), quy tắc kiểm tra trường nằm ở `03-dd/validation/F2_problem-bank.md`, tiêu chí
> nghiệm thu `AC-nn` nằm ở `04-tdd/modules/F2_problem-bank.md`. Quy ước mô hình hai tầng ID: `BR-nn` là khối quy tắc, `AC-nn` là tiêu chí nghiệm thu.

## 0. Danh sách endpoint

Quét toàn bộ Sheet 7.3 của `02-bd/screens/**` tìm endpoint có "BC sở hữu" là `problem-bank`. Bảng dưới gán path và method cho từng tên
nghiệp vụ BD đã đặt. Cột "Mục" trỏ tới mục chi tiết trong file này. Cột "Quyền" ghi `FUNCTION:ACTION` theo seed 11 function của
`02-bd/database/F1_identity.md:64-66` (hoặc "đã xác thực" nếu chỉ cần đăng nhập), kèm phạm vi dữ liệu.

Bảng 0.1: Endpoint cho học viên (A1) và mọi người dùng đã xác thực.

| # | Method | Path (sau `/api/v1/problem-bank`) | Tên nghiệp vụ BD | Màn gọi | Quyền | Mục |
| :-: | :--- | :--- | :--- | :--- | :--- | :-: |
| 1 | GET | `/problems` | `ListPublishedProblems` | `USR0101` | đã xác thực; chỉ bài `PUBLISHED AND NOT deleted` | 2 |
| 2 | GET | `/problems/random` | `GetRandomProblem` | `USR0101`, `USR0105` | đã xác thực | 3 |
| 3 | GET | `/topics/progress` | `ListTopicsWithProgress` | `USR0101` | đã xác thực, dữ liệu của chính người gọi | 4 |
| 4 | GET | `/me/class-assignments` | `ListMyClassAssignments` | `USR0101` | đã xác thực, lớp của chính người gọi | 5 |
| 5 | GET | `/problems/{problemId}/workspace` | `GetProblemForWorkspace` | `USR0102` | đã xác thực; bài `PUBLISHED AND NOT deleted` | 6 |
| 6 | GET | `/problems/{problemId}/overview` | `GetProblemOverview` | `USR0104` | đã xác thực; như trên | 7 |
| 7 | GET | `/problems/{problemId}/sample-testcases` | `ListSampleTestcases`, `GetProblemSampleTestcases` | `USR0102`, `USR0201` | đã xác thực; chỉ testcase `SAMPLE` đã duyệt | 8 |
| 8 | GET | `/me/bookmarks` | `ListMySavedProblems` | `USR0103` | đã xác thực, bookmark của chính người gọi | 9 |
| 9 | PUT | `/me/bookmarks/{problemId}` | `SaveMyBookmark` | `USR0103` | như trên | 10 |
| 10 | DELETE | `/me/bookmarks/{problemId}` | `RemoveMyBookmark` | `USR0103` | như trên | 11 |
| 11 | PUT | `/me/bookmarks/{problemId}/state` | `ToggleMyProblemBookmark` | `USR0102`, `USR0104` | như trên | 12 |
| 12 | GET | `/suggested-problems` | `GetSuggestedProblems` | `USR0601` | đã xác thực | 13 |
| 13 | GET | `/catalog/summary` | `GetPublishedProblemCatalogSummary` | `USR0601`, `USR0501` (qua cổng, mục 58) | đã xác thực | 14 |
| 13A | GET | `/me/class-problems` | `ListMyClassProblems` | `USR0701`, `USR0702` | STUDENT, lớp còn ghi danh của người gọi | 5A |
| 13B | GET | `/me/levels/progress` | `ListMyLevelProgress` | `USR0601` | STUDENT, chỉ tiến độ của người gọi | 14A |

Bảng 0.2: Danh mục độ khó và chủ đề (F2-02). Đọc cho mọi người dùng đã xác thực, ghi chỉ ADMIN.

| # | Method | Path | Tên nghiệp vụ BD | Màn gọi | Quyền | Mục |
| :-: | :--- | :--- | :--- | :--- | :--- | :-: |
| 14 | GET | `/levels` | `ListProblemLevels` | `USR0101`, `USR0103`, `USR0104`, `USR0601`, `SHR0201`, `SHR0202`, `SHR0203` | đã xác thực | 15 |
| 15 | POST | `/levels` | `CreateProblemLevel` | `SHR0201` | `PROBLEM_AUTHORING:CREATE` + vai trò `ADMIN` | 16 |
| 16 | PATCH | `/levels/{levelId}` | `UpdateProblemLevel` | `SHR0201` | `PROBLEM_AUTHORING:UPDATE` + `ADMIN` | 17 |
| 17 | PUT | `/levels/order` | `ReorderProblemLevels` | `SHR0201` | `PROBLEM_AUTHORING:UPDATE` + `ADMIN` | 18 |
| 18 | DELETE | `/levels/{levelId}` | `DeleteProblemLevel` | `SHR0201` | `PROBLEM_AUTHORING:DELETE` + `ADMIN` | 19 |
| 19 | GET | `/topics` | `ListProblemTopics` | `SHR0201`, `SHR0202`, `SHR0203` | đã xác thực | 20 |
| 20 | POST | `/topics` | `CreateProblemTopic` | `SHR0201` | `PROBLEM_AUTHORING:CREATE` + `ADMIN` | 21 |
| 21 | PATCH | `/topics/{topicId}` | `UpdateProblemTopic` | `SHR0201` | `PROBLEM_AUTHORING:UPDATE` + `ADMIN` | 22 |
| 22 | PUT | `/topics/order` | `ReorderProblemTopics` | `SHR0201` | `PROBLEM_AUTHORING:UPDATE` + `ADMIN` | 23 |
| 23 | DELETE | `/topics/{topicId}` | `DeleteProblemTopic` | `SHR0201` | `PROBLEM_AUTHORING:DELETE` + `ADMIN` | 24 |

Bảng 0.3: Quản lý bài toán (A2 phạm vi tác giả, A3 toàn kho) — màn `SHR0201`, `SHR0202`, `SHR0203`.

| # | Method | Path | Tên nghiệp vụ BD | Quyền | Mục |
| :-: | :--- | :--- | :--- | :--- | :-: |
| 24 | GET | `/authoring/problems` | `ListProblemsAdmin` | `PROBLEM_AUTHORING:READ`; A2 chỉ bài của mình | 25 |
| 25 | GET | `/authoring/problems/topic-distribution` | `GetTopicDistribution` | `PROBLEM_AUTHORING:READ`; phạm vi theo actor | 26 |
| 26 | GET | `/authoring/problems/attention` | `GetAttentionList` | `PROBLEM_AUTHORING:READ`; phạm vi theo actor | 27 |
| 27 | POST | `/authoring/problems/export` | `ExportProblemsCsv` | `PROBLEM_AUTHORING:READ` | 28 |
| 28 | POST | `/authoring/problems/bulk/publish` | `PublishProblem` | `PROBLEM_AUTHORING:UPDATE` | 29 |
| 29 | POST | `/authoring/problems/bulk/unpublish` | `UnpublishProblem` | `PROBLEM_AUTHORING:UPDATE` | 30 |
| 30 | POST | `/authoring/problems/bulk/difficulty` | `ChangeDifficulty` | `PROBLEM_AUTHORING:UPDATE` | 31 |
| 31 | POST | `/authoring/problems/bulk/topic` | `AssignTopic` | `PROBLEM_AUTHORING:UPDATE` | 32 |
| 32 | POST | `/authoring/problems/bulk/delete` | `DeleteProblem` | `PROBLEM_AUTHORING:DELETE` | 33 |
| 33 | GET | `/authoring/problems/{problemId}` | `GetProblemForAuthoring` | `PROBLEM_AUTHORING:READ` (hoặc `TESTCASE_MANAGEMENT:READ`); A2: `author_id = người gọi` hoặc bài `PUBLISHED` (chỉ đọc) | 34 |
| 34 | GET | `/authoring/problems/{problemId}/duplicate-source` | `DuplicateProblem` (đọc nguồn) | `PROBLEM_AUTHORING:CREATE`; A2 chỉ bài của mình | 35 |
| 35 | POST | `/authoring/problems` | `SaveProblemContent` (tạo mới) | `PROBLEM_AUTHORING:CREATE` | 36 |
| 36 | PUT | `/authoring/problems/{problemId}/content` | `SaveProblemContent` (cập nhật) | `PROBLEM_AUTHORING:UPDATE`; A2 chỉ bài của mình | 37 |
| 37 | PUT | `/authoring/problems/{problemId}/spec` | `SaveProblemSpec` | `PROBLEM_AUTHORING:UPDATE` | 38 |
| 38 | PUT | `/authoring/problems/{problemId}/ai-context` | `SaveAiAuthoringContext` | `PROBLEM_AUTHORING:UPDATE` | 39 |
| 39 | PUT | `/authoring/problems/{problemId}/status` | `UpdateProblemPublishStatus` | `PROBLEM_AUTHORING:UPDATE` | 40 |
| 40 | GET | `/authoring/problems/{problemId}/stats` | `GetProblemStats` | `PROBLEM_AUTHORING:READ` | 41 |

Bảng 0.4: Testcase, đáp án mẫu và sinh testcase bằng AI (F2-05 tới F2-09, F2-14, F2-18). Mọi endpoint A2 chỉ trên bài của mình.

| # | Method | Path | Tên nghiệp vụ BD | Quyền | Mục |
| :-: | :--- | :--- | :--- | :--- | :-: |
| 41 | POST | `/authoring/problems/{problemId}/testcases` | `CreateTestcase` | `TESTCASE_MANAGEMENT:CREATE` | 42 |
| 42 | PATCH | `/authoring/problems/{problemId}/testcases/{testcaseId}` | `UpdateTestcase` (gồm duyệt nháp AI) | `TESTCASE_MANAGEMENT:UPDATE` | 43 |
| 43 | DELETE | `/authoring/problems/{problemId}/testcases/{testcaseId}` | `DeleteTestcase` | `TESTCASE_MANAGEMENT:DELETE` | 44 |
| 44 | PUT | `/authoring/problems/{problemId}/testcases/order` | `ReorderTestcases` | `TESTCASE_MANAGEMENT:UPDATE` | 45 |
| 45 | POST | `/authoring/problems/{problemId}/testcases/bulk-upload-url` | `RequestTestcaseBulkUploadUrl` | `TESTCASE_MANAGEMENT:CREATE` | 46 |
| 46 | POST | `/authoring/problems/{problemId}/testcases/bulk-upload/confirm` | `ConfirmTestcaseBulkUpload` | `TESTCASE_MANAGEMENT:CREATE` | 47 |
| 47 | POST | `/authoring/problems/{problemId}/reference-solution/run` | `RunSampleSolutionAgainstTestcases` | `TESTCASE_MANAGEMENT:READ` | 48 |
| 48 | POST | `/authoring/problems/{problemId}/testcases/generate` | `GenerateTestcasesWithAi` | `TESTCASE_MANAGEMENT:CREATE` | 49 |

Bảng 0.5: Giao bài theo lớp (F2-12) — màn `INS0202`, `INS0101`.

| # | Method | Path | Tên nghiệp vụ BD | Quyền | Mục |
| :-: | :--- | :--- | :--- | :--- | :-: |
| 49 | GET | `/instructor/class-assignments` | `ListClassAssignments` | `CLASS_MANAGEMENT:READ`; chỉ lớp người gọi phụ trách | 50 |
| 50 | GET | `/instructor/class-assignments/summary` | `GetInstructorAssignmentSummary` | `CLASS_MANAGEMENT:READ`; như trên | 51 |
| 51 | GET | `/instructor/classes/{classId}/assignable-problems` | `ListAssignableProblems` (do `DEC-2026-1011-class-assignment-popup-on-assignments-screen` đặt tên) | `CLASS_MANAGEMENT:READ`; lớp người gọi phụ trách | 52 |
| 52 | POST | `/instructor/classes/{classId}/assignments` | `AssignProblemsToClass` (cùng DEC) | `CLASS_MANAGEMENT:UPDATE`; như trên | 53 |
| 53 | DELETE | `/instructor/classes/{classId}/assignments/{problemId}` | `RemoveClassAssignment` | `CLASS_MANAGEMENT:UPDATE`; như trên | 54 |
| 54 | GET | `/instructor/assigned-problems/top` | `ListMyAssignedProblems` | `CLASS_MANAGEMENT:READ` | 55 |

Bảng 0.6: Cấu hình trần sinh testcase (F2-14) — màn `ADM0303` khu vực E.

| # | Method | Path | Tên nghiệp vụ BD | Quyền | Mục |
| :-: | :--- | :--- | :--- | :--- | :-: |
| 55 | GET | `/settings/ai-testcase-generation` | `GetAiTestcaseGenerationSettings` | `AI_CONFIG:READ` | 56 |
| 56 | PUT | `/settings/ai-testcase-generation` | `UpdateAiTestcaseGenerationSettings` | `AI_CONFIG:UPDATE` | 57 |

Tổng cộng **56 endpoint HTTP**. Ngoài ra module phơi **cổng vào nội bộ** (không có route, mục 58) và phát/nhận **sự kiện** (mục 59).

Ghi chú về đối chiếu tên: `ListSampleTestcases` (`USR0102`) và `GetProblemSampleTestcases` (`USR0201`) là cùng một việc, cùng dữ liệu, cùng quyền; DD gộp
thành endpoint #7 để không có hai route cho một dữ liệu `[SoT: Suy luận — ponytail bậc 4]`.

## 1. Phạm vi và quy ước chung

### 1.1. Phạm vi

Trong phạm vi: mọi endpoint ở mục 0, cổng nội bộ mục 58, sự kiện mục 59. Ngoài phạm vi, có chủ đích:

- Sinh mã khung (`GetStarterCode`) — thuộc `harness` [SoT: `02-bd/screens/users/USR0102_problem_detail.md:745`, `:761-763`].
- Chạy thử mã học viên (`RunSampleTestcases`), nộp bài (`CreateSubmission`), giới hạn hiệu dụng, kênh thời gian thực — thuộc `judge-orchestration`.
- Nhãn "đã có Solution Review" (`ListMyReviewedProblemIds`) — thuộc `ai-review`, gọi riêng để bảng bài vẫn hiển thị khi F5 chết
  [SoT: `02-bd/screens/users/USR0101_problem_list.md:526`].
- Nhập CSV bài toán — đã cắt khỏi phạm vi [SoT: `01-rd/req/F2_problem-bank.md:177-180`].
- Danh sách thẻ gợi ý (autocomplete `tags`): không có trong RD/BD; thẻ tạo khi lưu bài (mục 37). Tiêu chí ponytail bậc 1.
- Thống kê "Thời gian giải trung bình" và "Lượt xin gợi ý" ở `SHR0202` khu H NO 8-9: BD chưa có nguồn dữ liệu (`SHR0202` Q5, dòng 790), DD không dựng endpoint cho chúng. Tiêu chí ponytail bậc 2.
- Hàng đợi sinh bất đồng bộ cho F2-14: chạy đồng bộ (mục 49).

### 1.2. Quy ước chung

Kế thừa quy ước của `03-dd/api/F1_identity.md` (đầu file): JSON UTF-8, xác thực `Authorization: Bearer <accessToken>`, mã `4xx`:
`400` lỗi kiểm tra đầu vào, `401` chưa xác thực hoặc token hết hạn, `403` không đủ quyền, `404` không tìm thấy, `409` xung đột trạng thái,
`422` vi phạm quy tắc nghiệp vụ, thêm `5xx` cho lỗi hạ tầng ngoài (`503`). **Không endpoint nào của module là công khai**: màn người học bắt buộc đăng nhập
[SoT: `02-bd/screens/users/USR0101_problem_list.md:623`].

**Định dạng lỗi** `[SoT: Suy luận — identity DD chỉ nêu trường error.code, chưa định hình bao; DD này đề xuất, controller thống nhất ở api.md]`:

```json
{ "error": { "code": "PRB-030", "message": "Bài chưa đủ điều kiện xuất bản.", "details": { "...": "tuỳ mã lỗi" }, "traceId": "uuid" } }
```

Máy chủ trả `code`; giao diện dịch qua `messages/{locale}.json` (hợp đồng i18n ở `03-dd/api/api.md`). `message` chỉ là bản tiếng Việt dự phòng,
không phải thứ giao diện dựa vào.

**Phân trang**: `page` (từ 1) và `pageSize`; phản hồi `{ "items": [...], "page": { "number", "size", "totalItems", "totalPages" } }`.
`page` lớn hơn `totalPages` thì trả trang cuối, không báo lỗi [SoT: `02-bd/screens/users/USR0101_problem_list.md:629`]. Tên tham số `pageSize` thống nhất với
identity (`03-dd/api/F1_identity.md:90`); BD `INS0202` gọi là `size` (`02-bd/screens/teacher/INS0202_class_assignments.md:473`), DD đọc `size` = `pageSize`.
Tập giá trị `pageSize` cho phép: học viên 12/24/48 mặc định 12 [SoT: `USR0101_problem_list.md:241`]; bài đã lưu cố định 20 [SoT: `USR0103_saved_problems.md:212`];
quản trị 8/20/50 mặc định 8 [SoT: `SHR0201_problem_management.md:505`, Khu vực D NO 16]; giao bài theo lớp 20 [SoT: `INS0202_class_assignments.md:473`].

**Tham số lọc của endpoint danh sách cho học viên được đọc rộng rãi (lenient)**: giá trị `status`, `difficulty`, `topic`, `tag`, `sort`, `dir`, `page`, `pageSize` không hợp lệ
bị bỏ qua và dùng mặc định, **không** trả `400` [SoT: `USR0101_problem_list.md:628`, `USR0103_saved_problems.md:588`]. Endpoint ghi và endpoint quản trị đọc nghiêm (`400 PRB-001`).

**Thời gian** là ISO-8601 UTC. **Khoá lạc quan** (mục 37, 38, 39): client gửi `expectedUpdatedAt` là giá trị `updatedAt` đã nhận ở lần tải gần nhất;
lệch thì `409 PRB-020` `[SoT: Suy luận — chọn cột updated_at thay số phiên bản vì BD chưa có cột phiên bản, đóng một phần SHR0202 Q16]`.

**Tỉ lệ AC** (`acRate`) là số phần trăm 0 đến 100, hai chữ số thập phân, đúng thang cột `problem_stats.ac_rate NUMERIC(5,2)`
[SoT: `02-bd/database/F2_problem-bank.md:215`]; `null` khi bài chưa có dòng `problem_stats` hoặc `submission_count = 0`, giao diện hiện `-`, không hiện `0%`
[SoT: `02-bd/database/F2_problem-bank.md:218`].

### 1.3. Phân quyền ba lớp

1. **Lớp xác thực**: token hợp lệ, chưa hết hạn (`401` nếu không).
2. **Lớp Function:Action**: xác thực/định vai trò từ JWT; quyền tra qua cổng `identity` trong tiến trình, đọc `permission_version` từ PostgreSQL primary trước khi dùng cache (`DEC-2026-1028-qa-f1-permission-immediate`). Thiếu thì `403 PRB-002`.
   Kiểm ở tầng application, không chỉ ẩn nút (`02-bd/security/F2_problem-bank.md:45-47`).
3. **Lớp phạm vi dữ liệu**:
   - A2 (`INSTRUCTOR`) chỉ ghi lên bài có `author_id` = người gọi; ghi lên bài `PUBLISHED` của người khác thì `403 PRB-003`, còn **bản nháp của người khác thì `404 PRB-010`**
     (cùng quy tắc không lộ sự tồn tại như khi đọc, bullet kế tiếp) `[SoT: Suy luận — mở rộng quy tắc 404 của DEC-2026-1011 sang thao tác ghi]`. A3 (`ADMIN`) không bị ràng buộc này
     [SoT: `02-bd/screens/shared/SHR0201_problem_management.md:794` dòng Sheet 9 NO 3; `SHR0202_problem_authoring.md:764`].
   - **A2 đọc bài đã xuất bản của người khác ở chế độ chỉ đọc; bản nháp của người khác trả `404 PRB-010`** giống một id không tồn tại, để không lộ sự tồn tại
     [SoT: `DEC-2026-1011-a2-read-published-problems-of-others`, `.nexa/control/decision-registry.md:3177-3196`]. Phải có test cho cả hai nhánh.
   - Vai trò `ADMIN` bắt buộc cho ghi danh mục chủ đề và độ khó (`403 PRB-004`), cộng `PROBLEM_AUTHORING` [SoT: `02-bd/security/F2_problem-bank.md:17-41`].
     Ma trận phân quyền **không** điều khiển danh mục này (`02-bd/security/F2_problem-bank.md:25`).
   - Lớp học: người gọi phải phụ trách `classId`, hỏi `identity` qua `InstructorClassPort` (mục 58); không thì `403 PRB-005`
     [SoT: `INS0202_class_assignments.md:515`].
   - Bookmark: `user_id` lấy từ token, **không endpoint nào nhận `user_id`** từ phía gọi; không có route nào cho A2/A3 đọc bookmark của người khác
     [SoT: `02-bd/security/F2_problem-bank.md:73-81`].

### 1.4. Quy tắc cứng về testcase ẩn và dữ liệu nhạy cảm

Áp cho **mọi** endpoint và log của module (`01-rd/req/F2_problem-bank.md:32-33`, `02-bd/security/F2_problem-bank.md:49-71`):

1. Không phản hồi nào gửi cho học viên chứa `input`, `expectedOutput`, diff, `object_key` hay presigned URL của testcase `HIDDEN`. Với testcase ẩn, học viên chỉ nhận
   **trạng thái và chỉ số** (việc đó do `judge-orchestration` trả, không phải module này). Mọi DTO phía học viên là **danh sách trắng** (whitelist) các trường,
   dựng riêng, không bao giờ tuần tự hoá trực tiếp entity.
2. `ListSampleTestcases` chỉ đọc dòng `visibility = SAMPLE AND approved = true`. Điều kiện này nằm ở **truy vấn**, không lọc sau ở tầng trình bày.
3. Presigned URL của testcase `HIDDEN` chỉ cấp cho người có `TESTCASE_MANAGEMENT:READ` và là tác giả hoặc `ADMIN`; không bao giờ cấp cho A1
   (`02-bd/storage/F2_problem-bank.md:48-52`).
4. Số `hiddenTestcaseCount` (`USR0104`) là con số duy nhất về testcase ẩn lộ ra học viên [SoT: `USR0104_problem_overview.md:630`].
5. Log ứng dụng mức `INFO` trở lên chỉ ghi `testcase_id` và độ dài chuỗi, không ghi nội dung input hay expected-output (`02-bd/security/F2_problem-bank.md:61-65`).
6. Nội dung do người dùng nhập (đề, `ai_brief`, ghi chú bookmark) là **dữ liệu**: máy chủ không chèn vào câu lệnh hệ thống hay ghép vào chỉ thị của AI. Đường
   duy nhất tới AI là `ProblemContextPort` (mục 58) và `TestcaseScriptGenerationPort` (mục 58), cả hai trả/nhận kiểu có trường tách biệt, adapter đặt vào khối có nhãn
   (`02-bd/architecture/F5_ai-review.md:218`). Mã nguồn đáp án mẫu **không có chỗ** trong kiểu đầu vào của cổng sinh testcase (mục 49).
7. Markdown/LaTeX lưu nguyên văn; phần làm sạch HTML thô thuộc tầng hiển thị phía giao diện (`02-bd/security/F2_problem-bank.md:111-114`). Máy chủ không tuyên bố đã làm sạch.

### 1.5. DTO dùng chung

**`PageMeta`**: `{ number: int, size: int, totalItems: long, totalPages: int }`.

**`LevelDto`**: `{ id: uuid, code: string, displayName: string, sortOrder: int, problemCount: int | null }` — `problemCount` chỉ có khi người gọi là `ADMIN` (popup quản lý),
tính cả bài `UNPUBLISHED` và `deleted` [SoT: `SHR0201_problem_management.md:686`].

**`TopicDto`**: `{ id, code, name, sortOrder, problemCount | null }` — như trên [SoT: `SHR0201_problem_management.md:682`].

**`TypeRef`** (nút kiểu dữ liệu, dạng AST của `harness`, `02-bd/architecture/F3_harness.md:80-92`): `{ "kind": "INT"|"LONG"|"DOUBLE"|"BOOLEAN"|"CHAR"|"STRING"|"ARRAY"|"LIST"|"LINKED_LIST"|"BINARY_TREE",
"of"?: TypeRef, "dimensions"?: int>=1 }`. `of` bắt buộc với `ARRAY`, `LIST`, `LINKED_LIST`, `BINARY_TREE`; `dimensions` bắt buộc với `ARRAY`. Lồng tối đa 3 tầng
`[SoT: SHR0202_problem_authoring.md:795 (Q10), con số 3 là suy luận của BD]`; chỉ `LIST` chứa được kiểu chứa khác.

**`ProblemSpecDto`**: `{ matchingStrategy: "EXACT"|"TRIMMED"|"EPSILON"|"UNORDERED_SET", epsilonValue: decimal|null, stdinFormatMd: string, stdoutFormatMd: string,
functionName: string, returnType: TypeRef, parameters: [ { name: string, type: TypeRef } ], nameOverrides: { JAVA?: string, CPP?: string, PYTHON?: string } | null, specVersion: int (chỉ đọc) }`.
Khớp 8 cột của dòng `problem_specs` [SoT: `02-bd/database/F2_problem-bank.md:128-136`].

**`TestcaseAuthorDto`** (chỉ cho tác giả/ADMIN, không bao giờ cho học viên): `{ id, position: int, visibility: "SAMPLE"|"HIDDEN", origin: "MANUAL"|"AI", approved: bool,
category: "TYPICAL"|"BOUNDARY_LOW"|"BOUNDARY_HIGH"|"DEGENERATE"|"DUPLICATE"|"SORTED"|"EXTREME"|null,
input: string|null, inputStorage: "INLINE"|"MINIO", inputSizeBytes: long, inputDownloadUrl: string|null,
expectedOutput: string|null, expectedOutputStorage: "INLINE"|"MINIO", expectedOutputSizeBytes: long, expectedOutputDownloadUrl: string|null }`.
`INLINE` trả nội dung trong `input`/`expectedOutput`; `MINIO` trả `null` ở hai trường đó và cấp `*DownloadUrl` (presigned GET, hiệu lực 5 phút,
`02-bd/storage/F2_problem-bank.md:48`). Kích thước đo bằng byte UTF-8. Bảy giá trị `category` theo `02-bd/database/F2_problem-bank.md:162`.

**`WorkedExampleDto`**: `{ id: uuid|null, position: int, input: string, output: string, explanationMd: string|null }` [SoT: `02-bd/database/F2_problem-bank.md:249-256`].
`id = null` ở request nghĩa là dòng mới.

**`PublishChecklistDto`**: `{ items: [ { code: "MIN_APPROVED_TESTCASES"|"MIN_APPROVED_SAMPLES"|"MIN_WORKED_EXAMPLES"|"REFERENCE_SOLUTION_PASSES_ALL"|"SPEC_DECLARED",
met: bool|null, required: int|null, actual: int|null } ], testcaseCount, sampleTestcaseCount, workedExampleCount, specDeclared, sampleSolutionAllPass: bool|null }`.
Ngưỡng `required` đọc từ cấu hình ứng dụng (mặc định 8, 2, 2) để giao diện không chép số [SoT: `SHR0202_problem_authoring.md:618`]. Khi `GetProblemForAuthoring`
tải, `sampleSolutionAllPass` và item tương ứng là `null` (chưa kiểm, hiện "Chưa"): máy chủ **không chạy sandbox mỗi lần tải trang**, chỉ chạy lúc xuất bản (mục 40)
hoặc khi giao diện gọi mục 48 [SoT: `02-bd/database/F2_problem-bank.md:243`; `[SoT: Suy luận]` cách đọc `SHR0202` 7.1 NO 17 theo `database` mục 1.11].

**`BulkProblemActionResultDto`**: `{ succeededIds: [uuid], failedItems: [ { id: uuid, code: "PRB-nnn", message: string, details?: object } ] }`. Mỗi bài xử lý **độc lập**
(một bài hỏng không chặn bài khác), phản hồi `200` kể cả khi mọi bài hỏng; lỗi cấp yêu cầu (tập rỗng, thiếu quyền) mới trả mã lỗi HTTP
[SoT: `SHR0201_problem_management.md:683`; chốt DD, `[SoT: Suy luận]`].

**`ProblemAuthoringDetailDto`** (phản hồi mục 34, 35, 36, 37):
`{ id, code, title, statementMd, constraintsMd, levelId, status: "UNPUBLISHED"|"PUBLISHED", limits: { timeLimitMs, memoryLimitMb, maxOutputSizeKb, maxSubmissionsPerHour: int|null },
topicIds: [uuid], tags: [ { id, name } ], referenceSolution: { language: "JAVA"|"CPP"|"PYTHON", sourceCode: string } | null,
workedExamples: [WorkedExampleDto], testcases: [TestcaseAuthorDto], spec: ProblemSpecDto | null, functionWrapperSupported: bool,
currentTestcaseSetVersion: int, aiContext: { briefText: string|null, noFullCode: bool, questionOnly: bool },
aiGeneration: { used: int, limit: int }, checklist: PublishChecklistDto, authorId: uuid, updatedAt: datetime,
updatedBy: { userId: uuid, displayName: string|null } | null, duplicatedFromProblemId: uuid|null, readOnly: bool }`.
`readOnly = true` khi A2 mở bài `PUBLISHED` của người khác: khi đó `testcases` chỉ mang số đếm (`items` rỗng, thêm `testcaseSummary`), không mang `input`, `expectedOutput`,
`*DownloadUrl`; `referenceSolution` vẫn trả vì `SHR0203` hiển thị nó [SoT: `SHR0203_problem_info.md:382`] (xem Q-03 ở mục 61).
`limit` của `aiGeneration` đọc từ `problem_bank_settings.max_ai_generations_per_problem` [SoT: `02-bd/database/F2_problem-bank.md:227`].

## 2. GET /problems (`ListPublishedProblems`)

### 2.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT (access token) |
| Quyền | đã xác thực (mọi vai trò) |
| Idempotent | có (chỉ đọc) |
| Phân trang | có (`page`, `pageSize` 12/24/48, mặc định 12) |
| Nguồn BD | `02-bd/screens/users/USR0101_problem_list.md:560`; F2-11 (`01-rd/req/F2_problem-bank.md:45-47`) |

### 2.2 Request (query)
| Trường | Kiểu | Bắt buộc | Ràng buộc | Ghi chú |
| :--- | :--- | :-: | :--- | :--- |
| `q` | string | không | cắt khoảng trắng, quá 100 ký tự thì cắt còn 100 | khớp không phân biệt hoa thường trên `code` và `title` `[SoT: Suy luận — RD/BD chỉ nói "tìm kiếm"]` |
| `status` | enum | không | `SOLVED`, `ATTEMPTED`, `UNSOLVED`; giá trị lạ bị bỏ qua | lọc theo trạng thái của chính người gọi |
| `difficulty` | string | không | `levelCode` có trong `problem_levels`; không khớp thì coi như "Tất cả" | |
| `topic` | uuid | không | id chủ đề; không hợp lệ bị bỏ qua | |
| `tag` | string | không | tên thẻ, so khớp không phân biệt hoa thường | F2-11 amendment; chưa có màn dùng `[SoT: Suy luận]` |
| `sort` | enum | không | `level`, `acRate`; mặc định `code` | `level` sắp theo `problem_levels.sort_order`; `acRate` đặt `null` cuối ở cả hai chiều |
| `dir` | enum | không | `asc`, `desc`; mặc định `asc` | |
| `page`, `pageSize` | int | không | xem 1.2 | |

### 2.3 Response (2xx)
| Trường | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `items[].id` | uuid | |
| `items[].code`, `items[].title` | string | |
| `items[].topicNames` | string[] | theo `topics.sort_order` |
| `items[].levelCode`, `items[].levelDisplayName` | string | join `problem_levels` qua `level_id` |
| `items[].acRate` | number\|null | thang 0-100 |
| `items[].functionWrapperSupported` | bool | `false` thì giao diện vẽ nhãn "Chỉ Standard I/O" |
| `items[].solveState` | enum\|null | `SOLVED`/`ATTEMPTED`/`UNSOLVED`; `null` khi `identity` không đáp ứng |
| `page` | PageMeta | |
| `solveStateAvailable` | bool | `false` khi cổng `identity` lỗi |

Không có `hasSolutionReview`: nhãn đó do lời gọi riêng sang `ai-review`.

### 2.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 401 | (mã chung) | thiếu/hết hạn token | |

Không có `400`: tham số lạ bị bỏ qua (1.2).

### 2.5 Luồng xử lý
1. Chuẩn hoá tham số theo bảng 2.2 (giá trị lạ về mặc định).
2. Đọc map trạng thái giải của người gọi qua `SolveStatePort` (cache 60 giây theo người dùng, BR-23); lỗi thì `solveStateAvailable = false`, bỏ lọc `status`.
3. Dựng truy vấn `status = PUBLISHED AND deleted = false` (**trong truy vấn**, bài ngoài phạm vi coi như không tồn tại, `USR0101_problem_list.md:624`), thêm các điều kiện lọc.
   `status = SOLVED` -> `id IN (đã giải)`; `ATTEMPTED` -> `id IN (đang làm)`; `UNSOLVED` -> `id NOT IN (đã giải, đang làm)`.
4. Đếm tổng, lấy trang, kẹp `page` về trang cuối nếu vượt.
5. Ghép `topicNames`, `acRate` (`LEFT JOIN problem_stats`), `solveState`.
6. Cache Redis `problem:list:v<phiên bản>:<hash tham số trừ status>` TTL 60 giây cho phần không phụ thuộc người dùng (BR-25).

### 2.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `problems`, `problem_levels`, `problem_topics`, `topics`, `problem_tags`, `tags`, `problem_stats` | R | chỉ bài `PUBLISHED AND NOT deleted`; chỉ mục `problems(status, deleted)` |
| `identity` (qua `SolveStatePort`) | R | không đọc thẳng schema khác |

### 2.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB02-N1 | Normal | không tham số: trả 12 bài mới nhất theo `code`, chỉ bài `PUBLISHED`, `solveState` đúng theo `identity` |
| PB02-N2 | Normal | lọc `difficulty` + `topic` + `status=SOLVED` cộng dồn điều kiện |
| PB02-A1 | Abnormal | bài `UNPUBLISHED` hoặc `deleted = true` không bao giờ xuất hiện, kể cả khi `q` gõ đúng mã bài |
| PB02-A2 | Abnormal | `difficulty=khong-ton-tai`, `sort=abc`, `page=-4` -> `200`, dùng mặc định, không lỗi |
| PB02-A3 | Abnormal | cổng `identity` lỗi -> `200`, `solveState = null`, `solveStateAvailable = false`, bảng vẫn có dữ liệu |
| PB02-B1 | Boundary | `page` lớn hơn số trang -> trả trang cuối; `q` dài 101 ký tự -> cắt còn 100 |
| PB02-B2 | Boundary | bài chưa có dòng `problem_stats` -> `acRate = null` (không phải `0`) |

## 3. GET /problems/random (`GetRandomProblem`)

### 3.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | đã xác thực |
| Idempotent | không (kết quả ngẫu nhiên), không đổi dữ liệu |
| Nguồn BD | `USR0101_problem_list.md:562`, Sheet 8 EVT-11; `USR0105_workspace_home.md:66` (bốc trong bài chưa giải, máy chủ bốc) |

### 3.2 Request (query)
Cùng bộ lọc `q`, `status`, `difficulty`, `topic`, `tag` như mục 2 (đọc rộng rãi), không có `sort`/`page`. `status` nhận thêm giá trị `NOT_SOLVED` (= `UNSOLVED` hợp với `ATTEMPTED`)
cho màn `USR0105` bốc trong các bài chưa giải gồm "Đang làm" `[SoT: Suy luận — tên giá trị do DD đặt, BD chỉ mô tả hành vi]`.

### 3.3 Response (2xx)
`{ "problemId": uuid | null }`. `null` khi tập khớp rỗng; giao diện không chuyển màn và hiện toast cảnh báo (`USR0101_problem_list.md:630`).

### 3.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 401 | (mã chung) | thiếu token | tập rỗng **không** là lỗi, trả `200` với `null` |

### 3.5 Luồng xử lý
1. Chuẩn hoá bộ lọc, đọc map trạng thái giải như mục 2.
2. Chọn một `id` ngẫu nhiên trong tập `PUBLISHED AND NOT deleted` khớp bộ lọc (`ORDER BY random() LIMIT 1` trên tập đã lọc; quy mô đồ án không cần cách nhanh hơn, `ponytail`: nâng cấp bằng chọn offset ngẫu nhiên khi kho vượt vài chục nghìn bài).
3. Trả `problemId`.

### 3.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `problems`, `problem_topics`, `problem_tags`, `problem_levels` | R | |
| `identity` qua `SolveStatePort` | R | khi có lọc `status` |

### 3.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB03-N1 | Normal | không lọc: trả một `problemId` thuộc bài đã xuất bản |
| PB03-N2 | Normal | `status=NOT_SOLVED`: không bao giờ trả bài đã giải |
| PB03-A1 | Abnormal | bộ lọc không còn bài -> `200 { problemId: null }` |
| PB03-A2 | Abnormal | bài `UNPUBLISHED`/`deleted` không bao giờ được bốc |

## 4. GET /topics/progress (`ListTopicsWithProgress`)

### 4.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | đã xác thực; dữ liệu của chính người gọi |
| Idempotent | có |
| Nguồn BD | `USR0101_problem_list.md:561`, DTO NO 10-11 (dòng 527-528) |

### 4.2 Request
Không tham số.

### 4.3 Response (2xx)
`{ "items": [ { "topicId": uuid, "topicName": string, "sortOrder": int, "solved": int|null, "total": int } ], "solveStateAvailable": bool }`.
`total` = số bài `PUBLISHED AND NOT deleted` gắn chủ đề đó; `solved` = trong số đó, số bài người gọi đã giải; `null` khi `identity` lỗi. Trả mọi chủ đề, kể cả `total = 0`, theo `sort_order`.

### 4.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 401 | (mã chung) | thiếu token | |

### 4.5 Luồng xử lý
1. Đếm `total` theo chủ đề (`problem_topics` join `problems` đã lọc `PUBLISHED AND NOT deleted`).
2. Đọc map `SOLVED` qua `SolveStatePort`; đếm `solved` bằng giao với tập bài của chủ đề. Bài đã rút xuống **không** tính vào cả tử lẫn mẫu (`02-bd/database/F2_problem-bank.md:58-59`).
3. Trả danh sách.

### 4.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `topics`, `problem_topics`, `problems` | R | |
| `identity` qua `SolveStatePort` | R | |

### 4.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB04-N1 | Normal | người dùng đã giải 2/5 bài chủ đề "Mảng" -> `solved = 2`, `total = 5` |
| PB04-A1 | Abnormal | `identity` lỗi -> `200`, `solved = null`, `solveStateAvailable = false` |
| PB04-B1 | Boundary | chủ đề không có bài xuất bản -> `total = 0`, `solved = 0`; bài đã rút không tính |

## 5. GET /me/class-assignments (`ListMyClassAssignments`)

### 5.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | đã xác thực; lớp suy ra từ người gọi, **không nhận `classId`** |
| Idempotent | có |
| Nguồn BD | `USR0101_problem_list.md:563`, DTO NO 12-13 (dòng 529-530); Sheet 9 NO 3 (dòng 625) |

### 5.2 Request
Không tham số.

### 5.3 Response (2xx)
`{ "groups": [ { "classId": uuid, "className": string, "items": [ { "problemId": uuid, "title": string, "solveState": enum|null, "assignedAt": datetime } ] } ] }`.
Tối đa 3 nhóm, xếp theo `assigned_at` lớn nhất của nhóm giảm dần; trong nhóm, bài giao gần nhất trước. Không có cờ "còn lớp bị cắt", không có hạn nộp
[SoT: `USR0101_problem_list.md:529-530`].

### 5.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 401 | (mã chung) | thiếu token | |

### 5.5 Luồng xử lý
1. Hỏi `StudentEnrollmentPort` các lớp người gọi đang ghi danh (`left_at IS NULL`) cùng tên lớp. Lỗi cổng -> `200 { "groups": [] }` (suy giảm êm, ẩn đúng khối này).
2. Đọc `class_assignments WHERE class_id IN (...) AND removed_at IS NULL`, nối `problems` chỉ lấy bài `PUBLISHED AND NOT deleted` `[SoT: Suy luận — học viên không mở được bài đã rút, INS0202 Sheet 9 NO 7 dòng 520]`.
3. Nhóm theo lớp, cắt còn 3 nhóm, ghép `solveState` qua F4 P7 `MyClassAssignmentStatePort` theo đúng lần giao hiện hành; lỗi cổng thì trả `solveState=null` cho từng dòng, không đoán là `UNSOLVED`.

### 5.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `class_assignments`, `problems` | R | chỉ mục `class_assignments(class_id) WHERE removed_at IS NULL` |
| `identity` qua `StudentEnrollmentPort`; `judge` qua `MyClassAssignmentStatePort` | R | không join chéo schema; tiến độ lớp chỉ tính lượt sau khi giao |

### 5.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB05-N1 | Normal | học viên thuộc 2 lớp: trả 2 nhóm, bài giao gần nhất đứng đầu |
| PB05-A1 | Abnormal | bài đã gỡ (`removed_at` khác `NULL`) hoặc đã rút xuất bản không xuất hiện |
| PB05-A2 | Abnormal | học viên chưa thuộc lớp nào -> `{ "groups": [] }`, không lỗi |
| PB05-B1 | Boundary | học viên thuộc 4 lớp -> chỉ 3 nhóm gần nhất |
| PB05-B2 | Boundary | đã Accepted bài trước khi lớp giao: dòng bài của lớp vẫn `UNSOLVED`; Accepted sau khi giao mới `SOLVED` |

## 5A. GET /me/class-problems (`ListMyClassProblems`)

Thêm theo `DEC-2026-1024-dd-student-class-contracts`. Mục này tách khỏi mục 5: màn Lớp của tôi cần đủ lớp và bài đã rút; mục 5 chỉ lấy tối đa ba nhóm và bỏ bài đã rút.

| Mục | Hợp đồng |
| :--- | :--- |
| Xác thực | JWT STUDENT; `userId` chỉ lấy từ token |
| Query | `classId` UUID, không bắt buộc. Vắng mặt: tất cả lớp người gọi còn ghi danh. Có mặt: chỉ một lớp của người gọi |
| Response 200 | `{ "items": [ { "classId", "problemId", "code", "title", "levelDisplayName", "solveState", "assignedAt", "withdrawn" } ] }`; `solveState` là `SOLVED`, `ATTEMPTED` hoặc `UNSOLVED`; `withdrawn` là boolean; `assignedAt` ISO-8601 UTC |
| Thứ tự | Khi không lọc: `classId` tăng dần, rồi `assignedAt` giảm dần và `problemId` tăng dần. Khi lọc: `assignedAt` giảm dần, `problemId` tăng dần |
| Lỗi | `401` chung; `404 PRB-015` khi `classId` sai dạng, không tồn tại, người gọi đã rời hoặc chưa tham gia; `503` chung khi cổng thành viên/trạng thái giải không khả dụng |

Chỉ đọc `problem.class_assignments WHERE removed_at IS NULL` và `problem.problems`, bao gồm bài `UNPUBLISHED` hoặc `deleted = true`; hai trạng thái này trả `withdrawn = true`. Không trả nội dung bài đã rút, chỉ metadata đủ cho bảng lớp. Kiểm phạm vi qua `StudentEnrollmentPort` của F1 trước khi truy vấn bài, không join chéo schema. Ghép F4 P7 `MyClassAssignmentStatePort` theo đúng `assignedAt` của lần giao hiện hành; nếu cổng trạng thái lớp lỗi thì trả `503`, không gán `UNSOLVED` sai. Không cắt ba nhóm, không phân trang `[SoT: Suy luận — quy mô bài giao lớp chưa bị chặn trong BD]`. Tổng tiến độ ở client = số dòng `SOLVED` trên tổng dòng trả về của lớp, kể cả dòng `withdrawn`; bài đã gỡ khỏi lớp (`removed_at` khác NULL) không tính. Lượt Accepted trước khi giao không biến dòng lớp thành `SOLVED`. [Owner 2026-10-25]

**Test viewpoints:** bốn lớp đều có bài được trả đủ; bài rút vẫn có dòng và tính mẫu số; bài gỡ khỏi lớp biến mất; lượt Accepted trước khi giao không hoàn thành bài lớp, lượt sau khi giao có; lớp của người khác và UUID sai cùng `PRB-015`; cổng thành viên hoặc trạng thái giải lỗi trả `503`, không ngụy trang thành rỗng.

## 6. GET /problems/{problemId}/workspace (`GetProblemForWorkspace`)

### 6.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | đã xác thực; chỉ bài `PUBLISHED AND NOT deleted` |
| Idempotent | có |
| Nguồn BD | `USR0102_problem_detail.md:742`, DTO dòng 688-710 |

### 6.2 Request
Tham số đường dẫn `problemId` (uuid).

### 6.3 Response (2xx)
| Trường | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id`, `code`, `title` | uuid, string, string | |
| `levelCode`, `levelDisplayName` | string | |
| `topicNames` | string[] | mỗi chủ đề một nhãn |
| `statementMd` | string | nguyên văn, kết xuất ở giao diện |
| `constraintsMd` | string | chuỗi rỗng thì giao diện không vẽ khối (`USR0102:709`) |
| `functionWrapperSupported` | bool | `false` thì ẩn nút "Bọc hàm" |
| `stdinFormatMd`, `stdoutFormatMd` | string\|null | từ `problem_specs`; giao diện chỉ dùng khi chọn Standard I/O |
| `acRate` | number\|null | |
| `bookmarked` | bool | của chính người gọi |
| `workedExamples` | `[ { order, input, output, explanationMd|null } ]` | theo `position`, một lần tải với đề (`USR0102:710`) |

Không trả: testcase (kể cả `SAMPLE`, có endpoint #7 riêng), đáp án mẫu, `ai_brief`, thông tin vòng đời, giới hạn (giới hạn hiệu dụng do `judge-orchestration` trả,
`GetEffectiveExecutionLimits`) `[SoT: Suy luận — 7.3 NO 1 nhắc "giới hạn cơ sở" nhưng DTO 7.1 không có trường, DD theo DTO]`.

### 6.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 404 | PRB-010 | bài không tồn tại, `UNPUBLISHED` hoặc `deleted` | không lộ sự tồn tại của bài chưa xuất bản |

### 6.5 Luồng xử lý
1. Đọc `problems` với `id = :problemId AND status = PUBLISHED AND deleted = false`; không có -> `404 PRB-010`.
2. Nối `problem_levels`, `topics`, `problem_specs` (hai cột định dạng), `problem_stats`, `worked_examples ORDER BY position`, `bookmarks WHERE user_id = :người gọi`.
3. Trả DTO danh sách trắng (không đi qua entity đầy đủ).

### 6.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `problems`, `problem_levels`, `topics`, `problem_topics`, `problem_specs`, `problem_stats`, `worked_examples`, `bookmarks` | R | |

### 6.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB06-N1 | Normal | bài đã xuất bản: đủ đề, ràng buộc, ví dụ mẫu theo `position`, `bookmarked` đúng |
| PB06-A1 | Abnormal | bài `UNPUBLISHED`, `deleted`, id không tồn tại -> cùng `404 PRB-010`, thân phản hồi không khác nhau |
| PB06-A2 | Abnormal | phản hồi không chứa trường `testcases`, `referenceSolution`, `aiBrief` (kiểm danh sách trắng) |
| PB06-B1 | Boundary | `constraintsMd` rỗng, danh sách ví dụ rỗng -> `""` và `[]`, không lỗi |

## 7. GET /problems/{problemId}/overview (`GetProblemOverview`)

### 7.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | đã xác thực; bài `PUBLISHED AND NOT deleted` |
| Idempotent | có |
| Nguồn BD | `USR0104_problem_overview.md:669`, DTO dòng 624-638 |

BD để ngỏ giữa "endpoint riêng" và "dùng lại `GetProblemForWorkspace` thêm trường" (`USR0104:669`, Q4). **DD chọn endpoint riêng**: màn tổng quan không cần định dạng stdin/stdout, cờ mô hình;
cần `hiddenTestcaseCount` và `explanationMd`; tách ra để DTO mỗi màn không phình `[SoT: Suy luận — DD đóng USR0104 Q4 bằng endpoint riêng]`.

### 7.2 Request
`problemId` (uuid) ở đường dẫn.

### 7.3 Response (2xx)
`{ problemId, code, title, levelCode, levelDisplayName, statementMd, constraintsMd, topicNames: string[], acRate: number|null, hiddenTestcaseCount: int, bookmarked: bool,
examples: [ { input, output, explanationMd|null } ] }`.
`hiddenTestcaseCount` đếm dòng `visibility = HIDDEN AND approved = true` (chỉ số đếm, không trường nào khác của testcase ẩn). `examples` từ `worked_examples ORDER BY position`
(`USR0104:632`). Không trả `stdin/stdout`, không trả cờ mô hình.

### 7.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 404 | PRB-010 | bài không tồn tại / chưa xuất bản / đã ẩn mềm | |

### 7.5 Luồng xử lý
Như mục 6.5; thêm `SELECT count(*) FROM testcases WHERE problem_id = ? AND visibility = 'HIDDEN' AND approved = true` (không đọc nội dung dòng nào,
`USR0104:649`).

### 7.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `problems`, `problem_levels`, `topics`, `problem_topics`, `problem_stats`, `worked_examples`, `bookmarks` | R | |
| `testcases` | R (chỉ `count`) | **không** đọc cột nội dung |

### 7.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB07-N1 | Normal | trả ví dụ kèm giải thích và `hiddenTestcaseCount` đúng |
| PB07-A1 | Abnormal | bài chưa xuất bản -> `404 PRB-010` |
| PB07-A2 | Abnormal | không trường nào của testcase ẩn (input, output, id, key) lọt vào phản hồi |
| PB07-B1 | Boundary | bài không có testcase ẩn đã duyệt -> `hiddenTestcaseCount = 0` |

## 8. GET /problems/{problemId}/sample-testcases (`ListSampleTestcases`, `GetProblemSampleTestcases`)

### 8.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | đã xác thực; bài `PUBLISHED AND NOT deleted` |
| Idempotent | có |
| Nguồn BD | `USR0102_problem_detail.md:743`, DTO NO 11 (dòng 698); `USR0201_submission_result.md:444` |

### 8.2 Request
`problemId` (uuid) ở đường dẫn.

### 8.3 Response (2xx)
`{ "parameterNames": [string], "items": [ { "testcaseId": uuid, "order": int, "input": string|null, "inputDownloadUrl": string|null, "expectedOutput": string|null, "expectedOutputDownloadUrl": string|null } ] }`.
Chỉ dòng `visibility = SAMPLE AND approved = true`, theo `position`. `testcaseId` là khoá để client ghép kết quả của `judge-orchestration` (chạy thử mục 5 và chi tiết bài nộp mục 8, F4) với dữ liệu vào và kết quả mong đợi; công khai vì chỉ có ở testcase `SAMPLE`, không bao giờ xuất hiện cho testcase `HIDDEN` (`DEC-2026-1024-dd-screens-users-core-contract-changes`). Testcase `MINIO` (hiếm) trả `*DownloadUrl` hiệu lực ngắn (5 phút) thay cho nội dung, được phép vì đây là testcase công khai
(`02-bd/storage/F2_problem-bank.md:52-54`). `parameterNames` là tên tham số theo thứ tự của `problem_specs.parameters` để giao diện gán nhãn ở mô hình Bọc hàm. Máy chủ **không** tách input theo tham số:
cách tách phụ thuộc định dạng input chuẩn hoá của `harness` (`02-bd/architecture/F3_harness.md:122-135`) `[SoT: Suy luận, hỏi `harness` DD, Q-09 mục 61]`.

### 8.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 404 | PRB-010 | bài không tồn tại / chưa xuất bản / đã ẩn mềm | |

### 8.5 Luồng xử lý
1. Kiểm bài như mục 6.5 bước 1.
2. `SELECT ... FROM testcases WHERE problem_id = ? AND visibility = 'SAMPLE' AND approved = true ORDER BY position` — điều kiện lọc ở truy vấn (quy tắc 1.4 mục 2).
3. `INLINE`: trả nội dung. `MINIO`: ký presigned GET 5 phút qua `TestcaseObjectStorePort`.

### 8.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `testcases` | R | chỉ mục `testcases(problem_id, visibility)` |
| `problem_specs` | R | chỉ cột `parameters` |
| MinIO | presign GET | chỉ object của testcase `SAMPLE` |

### 8.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB08-N1 | Normal | bài có 2 Sample đã duyệt -> 2 phần tử theo `position`, đúng nội dung, mỗi phần tử có `testcaseId` trùng `testcaseId` mà F4 trả cho cùng testcase |
| PB08-A1 | Abnormal | bài có Sample chưa duyệt (nháp AI) hoặc testcase `HIDDEN` -> không xuất hiện; không có `object_key` nào của testcase ẩn trong phản hồi |
| PB08-A2 | Abnormal | bài chưa xuất bản -> `404 PRB-010` |
| PB08-B1 | Boundary | Sample `MINIO` -> `input = null` và có `inputDownloadUrl`; bài chưa có Sample -> `items = []` |

## 9. GET /me/bookmarks (`ListMySavedProblems`)

### 9.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | đã xác thực; chỉ bookmark của chính người gọi |
| Idempotent | có |
| Phân trang | có (20 dòng cố định) |
| Nguồn BD | `USR0103_saved_problems.md:508`, DTO dòng 471-484 |

### 9.2 Request (query)
| Trường | Kiểu | Bắt buộc | Ràng buộc | Ghi chú |
| :--- | :--- | :-: | :--- | :--- |
| `q` | string | không | cắt còn 100 ký tự | khớp `code`, `title` |
| `status` | enum | không | `SOLVED`/`ATTEMPTED`/`UNSOLVED` | lạ thì bỏ qua |
| `difficulty` | string | không | `levelCode` | không khớp thì "Tất cả" |
| `sort` | enum | không | chỉ `savedAt` (cột sắp xếp được duy nhất, `USR0103:429`) | |
| `dir` | enum | không | `asc`/`desc`; mặc định `desc` (mới lưu trước, `USR0103:556`) | |
| `page` | int | không | từ 1 | `pageSize` cố định 20 |

Tham số `topic` (nếu có) bị bỏ qua (`USR0103:588`).

### 9.3 Response (2xx)
`{ "items": [ { "problemId", "code", "title", "levelCode", "levelDisplayName", "topicNames": [string], "solveState": enum|null, "note": string, "savedAt": datetime, "withdrawn": bool } ],
"page": PageMeta, "meta": { "savedTotal": int, "solvedCount": int|null, "filteredTotal": int } }`.
`note` là chuỗi rỗng khi cột `NULL`. `withdrawn = status = UNPUBLISHED OR deleted` do máy chủ tính, **không trả hai cột thô** (`USR0103:479`); tên trường `withdrawn` chốt ở đây theo bản dựng
(`USR0103:479` giao cho DD). `savedTotal` tính trên **toàn bộ** bookmark kể cả bài đã rút. `solvedCount` đếm bookmark có `SOLVED` (kể cả bài đã rút, `USR0103:481`), `null` khi `identity` lỗi.
`filteredTotal` là số dòng khớp bộ lọc.

### 9.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 401 | (mã chung) | thiếu token | không có `403`: không có đường cho vai trò khác đọc bookmark người khác |

### 9.5 Luồng xử lý
1. `user_id` lấy từ token. Chuẩn hoá tham số.
2. Đọc mọi bookmark của người gọi (bounded bởi số bài đã lưu) nối `problems` (không lọc `status`/`deleted`: dòng đã rút vẫn trả), `problem_levels`, `topics`.
3. Đọc map trạng thái giải (`SolveStatePort`); áp lọc `status`, `difficulty`, `q`; tính `savedTotal`, `solvedCount`, `filteredTotal`.
4. Sắp xếp theo `bookmarks.created_at`, cắt trang, kẹp trang vượt phạm vi.

### 9.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `bookmarks` | R | PK `(user_id, problem_id)` phục vụ truy vấn theo `user_id` |
| `problems`, `problem_levels`, `topics`, `problem_topics` | R | |
| `identity` qua `SolveStatePort` | R | |

### 9.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB09-N1 | Normal | 3 bookmark -> 3 dòng, mới lưu trước, `note` đúng |
| PB09-A1 | Abnormal | tài khoản A2/A3 gọi -> chỉ thấy bookmark của chính mình; không có tham số `userId` nào được đọc (gửi `userId=...` bị bỏ qua) |
| PB09-A2 | Abnormal | `identity` lỗi -> `solveState = null`, `solvedCount = null`, vẫn có đủ bảng và ghi chú |
| PB09-B1 | Boundary | bài đã rút xuống -> `withdrawn = true`, vẫn nằm trong kết quả và trong `savedTotal` |
| PB09-B2 | Boundary | 21 bookmark -> trang 1 có 20 dòng; `page=9` trả trang 2 |

## 10. PUT /me/bookmarks/{problemId} (`SaveMyBookmark`)

### 10.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | đã xác thực; chỉ bookmark của chính người gọi (khoá `(user_id, problem_id)`) |
| Idempotent | có (cùng body cho cùng kết quả) |
| Nguồn BD | `USR0103_saved_problems.md:509`, ghi chú ranh giới dòng 518-521 |

### 10.2 Request
| Trường | Kiểu | Bắt buộc | Ràng buộc | Ghi chú |
| :--- | :--- | :-: | :--- | :--- |
| `problemId` (path) | uuid | có | | |
| `note` | string\|null | không | cắt khoảng trắng hai đầu; sau cắt rỗng thì lưu `NULL`; tối đa 1000 ký tự; văn bản thuần | `USR0103:584` |
| `createIfMissing` | bool | không | mặc định `true` | `false` dùng khi sửa ghi chú: bookmark không còn thì `404 PRB-014`, **không** tạo lại âm thầm (`USR0103:585`). `true` dùng khi lưu lần đầu và Hoàn tác `[SoT: Suy luận — BD gộp ba việc vào một upsert nhưng đòi hai hành vi khác nhau khi thiếu dòng, cần cờ phân biệt]` |

### 10.3 Response (2xx)
`201` khi tạo mới, `200` khi cập nhật: `{ "problemId", "note": string|null, "savedAt": datetime }`. `savedAt` = `created_at`; khi tạo lại sau Hoàn tác là ngày tạo lại (`USR0103:586`).

### 10.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 400 | PRB-001 | `note` quá 1000 ký tự | `details.fields[0] = note` |
| 404 | PRB-010 | cần **tạo mới** mà bài không tồn tại/chưa xuất bản/đã ẩn mềm | bookmark đã có thì sửa ghi chú vẫn được dù bài đã rút (`USR0103:582`) |
| 404 | PRB-014 | `createIfMissing = false` mà không có bookmark | giao diện hiện "Bài này đã được bỏ lưu ở nơi khác" rồi tải lại |

### 10.5 Luồng xử lý
1. `user_id` = người gọi. Chuẩn hoá `note`.
2. Có dòng: `UPDATE bookmarks SET note = ?, updated_at = now()`; không đổi `created_at`.
3. Không có dòng: `createIfMissing = false` -> `404 PRB-014`; ngược lại kiểm bài `PUBLISHED AND NOT deleted` (không thì `404 PRB-010`) rồi `INSERT` (xử lý đụng độ đồng thời bằng `ON CONFLICT (user_id, problem_id) DO UPDATE`).

### 10.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `bookmarks` | C, U | |
| `problems` | R | chỉ khi tạo mới |

### 10.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB10-N1 | Normal | lưu lần đầu kèm ghi chú -> `201`; sửa ghi chú -> `200`, `savedAt` giữ nguyên |
| PB10-N2 | Normal | Hoàn tác (`createIfMissing=true`) tạo lại bookmark kèm ghi chú cũ, `savedAt` là ngày tạo lại |
| PB10-A1 | Abnormal | `createIfMissing=false` khi dòng đã bị xoá -> `404 PRB-014`, không tạo dòng mới |
| PB10-A2 | Abnormal | ghi chú 1001 ký tự -> `400 PRB-001`; ghi chú chỉ gồm khoảng trắng -> lưu `NULL` |
| PB10-A3 | Abnormal | A2/A3 gọi bằng `problemId` của mình chỉ ghi bookmark của chính họ; không có cách trỏ vào bookmark người khác |
| PB10-B1 | Boundary | ghi chú đúng 1000 ký tự được chấp nhận; bài đã rút + bookmark có sẵn -> sửa ghi chú vẫn `200` |

## 11. DELETE /me/bookmarks/{problemId} (`RemoveMyBookmark`)

### 11.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | đã xác thực; bookmark của chính người gọi |
| Idempotent | **có**: bỏ lưu một bookmark đã không còn vẫn thành công (`USR0103_saved_problems.md:585`) |
| Nguồn BD | `USR0103_saved_problems.md:510` |

### 11.2 Request
`problemId` (uuid) ở đường dẫn. Không body.

### 11.3 Response (2xx)
`204 No Content`.

### 11.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 401 | (mã chung) | thiếu token | không có `404`: thiếu dòng cũng là `204` |

### 11.5 Luồng xử lý
`DELETE FROM bookmarks WHERE user_id = :người gọi AND problem_id = :problemId` (xoá thật, mất cả `note`, `USR0103:490`). Máy chủ không từ chối vì bài đã rút (`USR0103:582`).

### 11.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `bookmarks` | D | |

### 11.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB11-N1 | Normal | xoá bookmark có sẵn -> `204`, dòng biến mất |
| PB11-A1 | Abnormal | gọi lần hai (đã mất) -> vẫn `204` |
| PB11-A2 | Abnormal | bài đã rút xuống -> vẫn xoá được |

## 12. PUT /me/bookmarks/{problemId}/state (`ToggleMyProblemBookmark`)

### 12.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | đã xác thực; bookmark của chính người gọi |
| Idempotent | có: client gửi **trạng thái mong muốn**, không phải lệnh đảo `[SoT: Suy luận — tên BD là "Toggle", DD hiện thực hoá bằng trạng thái đích để chống chạy lặp/hai thiết bị]` |
| Nguồn BD | `USR0102_problem_detail.md:744`, `USR0104_problem_overview.md:670` |

### 12.2 Request
`{ "bookmarked": bool }` (bắt buộc).

### 12.3 Response (2xx)
`200 { "bookmarked": bool }`.

### 12.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 400 | PRB-001 | thiếu hoặc sai kiểu `bookmarked` | |
| 404 | PRB-010 | `bookmarked = true` mà bài không tồn tại/chưa xuất bản/đã ẩn mềm | |

### 12.5 Luồng xử lý
- `true`: nếu chưa có dòng thì `INSERT (user_id, problem_id, note = NULL, created_at = now())`; có rồi thì không đổi gì (giữ ghi chú cũ).
- `false`: `DELETE` dòng (kéo theo `note`, `USR0104:652`); không có dòng cũng thành công.

### 12.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `bookmarks` | C, D | |
| `problems` | R | khi tạo |

### 12.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB12-N1 | Normal | `true` rồi `false` -> tạo rồi xoá; `GetProblemForWorkspace.bookmarked` đổi tương ứng |
| PB12-A1 | Abnormal | gửi `true` hai lần liên tiếp -> không tạo hai dòng, không mất ghi chú |
| PB12-A2 | Abnormal | `true` trên bài chưa xuất bản -> `404 PRB-010` |

## 13. GET /suggested-problems (`GetSuggestedProblems`)

### 13.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | đã xác thực; xếp hạng theo dữ liệu của chính người gọi |
| Idempotent | có |
| Nguồn BD | `USR0601_dashboard.md:817`, DTO dòng 773, Khu vực I NO 5 (dòng 555) |

### 13.2 Request (query)
| Trường | Kiểu | Bắt buộc | Ràng buộc | Ghi chú |
| :--- | :--- | :-: | :--- | :--- |
| `difficulty` | string | không | `levelCode`; không khớp thì "Tất cả" | |
| `topic` | uuid | không | id chủ đề | |

Luôn trả đúng tối đa 7 dòng, không phân trang (`USR0601:555`).

### 13.3 Response (2xx)
`{ "items": [ { "problemId", "code", "title", "levelCode", "levelDisplayName", "topicName": string|null, "acRate": number|null, "solveState": enum|null, "functionWrapperSupported": bool } ], "topicScores": [ { "topicId", "topicName", "sortOrder", "score": int } ], "rankingAvailable": bool }`. `topicScores` là **cùng một vector** được dùng để xếp `items`, không chịu bộ lọc `difficulty`/`topic`; chỉ gồm chủ đề có lượt nộp ở cửa sổ `all` và `total > 0`, theo `sortOrder`, rồi `topicId`. `rankingAvailable=false` thì `topicScores=[]` báo nguồn suy giảm, khác trường hợp `rankingAvailable=true` và mảng rỗng do chưa có dữ liệu. `topicName` trong `items` là chủ đề đầu tiên của bài. `acRate` của bài là tỉ lệ toàn hệ thống, không phải của người gọi (`USR0601:773`).

### 13.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 401 | (mã chung) | thiếu token | lỗi cổng điểm chủ đề suy giảm thành `rankingAvailable=false`, vẫn trả bài gợi ý |
| 400 | `PRB-001` | `topic` không phải UUID | không gọi F1/F4 |

### 13.5 Luồng xử lý
1. Lọc bài `PUBLISHED AND NOT deleted` theo `difficulty`, `topic`.
2. F2 lấy `total`/`solved` từng chủ đề trên bài `PUBLISHED AND NOT deleted` (cùng tập với §4), lấy map giải qua F1 `SolveStatePort`. Để lấy tỉ lệ AC lịch sử `all`, F2 lập `Map<topicId, problemIds[]>` từ quan hệ bài–chủ đề **kể cả bài đã rút**, gửi qua cổng F4 P5 `MyTopicSubmissionMetricsPort`. F4 chỉ đếm trong `judge.submissions` và không gọi ngược F2. Nếu có hơn 50 chủ đề, F2 gửi nhiều lô tối đa 50; một lô lỗi thì toàn vector không khả dụng. Với mỗi chủ đề có `total > 0` và `submissionCount > 0`: `score = round(100 * (0.7 * acRate + 0.3 * solved / total))`, chặn về 0–100. Chủ đề chưa nộp hoặc chưa có bài xuất bản **không có điểm**; không chuyển `null` thành 0. F2 tính vector này một lần cho response và bước xếp hạng; client không tính lại.
3. Xếp hạng: bài **chưa giải** (`UNSOLVED` và `ATTEMPTED`) lên trước; trong mỗi nhóm, bài có điểm chủ đề thấp nhất lên trước (bài nhiều chủ đề lấy chủ đề có điểm thấp nhất `[SoT: Suy luận]`);
   chủ đề chưa có điểm coi như 100; cùng hạng thì theo `code` tăng dần. Không gọi AI.
4. Lấy tối đa 7 dòng. Nếu F4 P5 lỗi nhưng F1 `SolveStatePort` còn hoạt động, xếp theo trạng thái giải rồi `code`. Nếu F1 lỗi, `solveState=null` và xếp chỉ theo `code`, không suy đoán bài chưa giải. Cả hai trường hợp trả `rankingAvailable=false`, `topicScores=[]`; không trả điểm giả. Dữ liệu bài vẫn trả được.

### 13.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `problems`, `problem_levels`, `problem_topics`, `topics`, `problem_stats` | R | |
| `identity` qua `SolveStatePort`; F4 qua P5 `MyTopicSubmissionMetricsPort` | R | không join schema `identity`/`judge`; F4 P5 không gọi lại F2 |

### 13.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB13-N1 | Normal | bài chưa giải thuộc chủ đề yếu nhất đứng đầu; bài đã giải xuống cuối |
| PB13-A1 | Abnormal | F1 hoặc F4 cổng điểm lỗi -> `200`, `rankingAvailable = false`, `topicScores=[]`, vẫn có tối đa 7 bài |
| PB13-B1 | Boundary | kho có ít hơn 7 bài khớp -> trả đúng số đó; không có bài -> `items = []` |

## 14. GET /catalog/summary (`GetPublishedProblemCatalogSummary`)

### 14.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | đã xác thực |
| Idempotent | có |
| Nguồn BD | `USR0601_dashboard.md:815`; `USR0501_my_progress.md:521`; cùng use case với cổng `ProblemBankStatsPort.publishedCatalogSummary()` (mục 58) |

### 14.2 Request
Không tham số.

### 14.3 Response (2xx)
`{ "totalPublished": int, "byTopic": [ { "topicId", "topicName", "count": int } ], "byLevel": [ { "levelCode", "levelDisplayName", "sortOrder", "count": int } ] }`.
`byLevel` có **đúng một phần tử cho mỗi mức** trong `problem_levels` theo `sort_order`, kể cả mức `count = 0` (`USR0501:482`, `USR0601:815`). `byTopic` theo `sort_order` của chủ đề, chỉ chủ đề có bài xuất bản;
một bài nhiều chủ đề được đếm ở mỗi chủ đề (tổng `byTopic` có thể lớn hơn `totalPublished`).

### 14.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 401 | (mã chung) | thiếu token | |

### 14.5 Luồng xử lý
Ba truy vấn đếm trên `problems WHERE status = PUBLISHED AND deleted = false` (mẫu số chỉ tính bài còn hiển thị, `02-bd/database/F2_problem-bank.md:58-59`), nhóm theo `level_id` và theo `topic_id`.
Cache Redis ngắn theo phiên bản danh sách (BR-25).

### 14.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `problems`, `problem_levels`, `problem_topics`, `topics` | R | |

### 14.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB14-N1 | Normal | 3 mức seed -> `byLevel` có 3 phần tử theo `sort_order`, tổng `count` = `totalPublished` |
| PB14-A1 | Abnormal | bài `UNPUBLISHED`/`deleted` không được đếm vào mẫu số |
| PB14-B1 | Boundary | ADMIN thêm mức mới chưa có bài -> xuất hiện trong `byLevel` với `count = 0` |

## 14A. GET /me/levels/progress (`ListMyLevelProgress`)

### 14A.1 Hợp đồng
JWT STUDENT; `userId` luôn lấy từ token. Không nhận query, không đọc tiến độ của tài khoản khác. Nguồn: `DEC-2026-1024-dd-student-dashboard-and-interview-navigation`, BD `USR0601` Khu vực B/G.

### 14A.2 Response 200
`{ "totalPublished": int, "solved": int, "byLevel": [ { "levelCode": string, "levelDisplayName": string, "sortOrder": int, "total": int, "solved": int } ] }`. `byLevel` có đúng một dòng cho mỗi mức trong `problem_levels`, kể cả mức chưa có bài; sắp theo `sortOrder`, rồi `levelCode`. `totalPublished = sum(byLevel.total)` và `solved = sum(byLevel.solved)`. Mỗi bài chỉ thuộc một mức nên không đếm trùng. `solved` chỉ tính bài có trạng thái `SOLVED` trong tập `PUBLISHED AND deleted = false`; bài đã rút không thuộc cả tử lẫn mẫu. Mức không có bài trả `total = solved = 0`.

### 14A.3 Xử lý và lỗi
Truy vấn danh sách `problemId, levelId` đang xuất bản từ F2, lấy map của người gọi qua `SolveStatePort` F1 rồi giao hai tập theo từng mức. Lỗi cổng trạng thái giải trả `503` để tránh biến mất tiến độ thành 0; không dùng cache trạng thái quá TTL 60 giây của cổng. Thiếu/hết JWT trả `401` chung. F2 không đọc trực tiếp bảng `identity`. Không lấy tử số từ F1 #47 vì read model lịch sử ở đó còn giữ bài đã rút.

## 15. GET /levels (`ListProblemLevels`)

### 15.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | đã xác thực (mọi vai trò), `02-bd/security/F2_problem-bank.md:35` |
| Idempotent | có |
| Nguồn BD | `SHR0201_problem_management.md:725`; `USR0101_problem_list.md:567` |

### 15.2 Request (query)
| Trường | Kiểu | Bắt buộc | Ràng buộc | Ghi chú |
| :--- | :--- | :-: | :--- | :--- |
| `includeCounts` | bool | không | mặc định `false`; chỉ vai trò `ADMIN` được hưởng, vai trò khác bị bỏ qua | thêm `problemCount` cho popup quản lý |

### 15.3 Response (2xx)
`{ "items": [ LevelDto ] }` theo `sort_order` tăng dần. Không có trường màu (`02-bd/database/F2_problem-bank.md:100`).

### 15.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 401 | (mã chung) | thiếu token | |

### 15.5 Luồng xử lý
Đọc `problem_levels ORDER BY sort_order`; khi `includeCounts` hợp lệ, thêm `count(problems.level_id)` mọi trạng thái kể cả `deleted = true` (`02-bd/database/F2_problem-bank.md:104-105`).

### 15.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `problem_levels` | R | |
| `problems` | R (đếm) | chỉ mục `problems(level_id)` |

### 15.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB15-N1 | Normal | ba mức seed `EASY`, `MEDIUM`, `HARD` theo `sort_order` |
| PB15-A1 | Abnormal | A2 gửi `includeCounts=true` -> `problemCount = null` |
| PB15-B1 | Boundary | mức không có bài -> `problemCount = 0` (khi ADMIN gọi) |

## 16. POST /levels (`CreateProblemLevel`)

### 16.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `PROBLEM_AUTHORING:CREATE` + vai trò `ADMIN` (`02-bd/security/F2_problem-bank.md:36`) |
| Idempotent | không |
| Nguồn BD | `SHR0201_problem_management.md:726`, EVT-27 |

### 16.2 Request
| Trường | Kiểu | Bắt buộc | Ràng buộc | Ghi chú |
| :--- | :--- | :-: | :--- | :--- |
| `displayName` | string | có | cắt khoảng trắng, 1 đến 60 ký tự, không trùng mức khác không phân biệt hoa thường | `SHR0201:807` (Sheet 9 NO 16); 60 là suy luận của BD |

### 16.3 Response (2xx)
`201` + `LevelDto` (`code` do máy chủ sinh, `sortOrder` = lớn nhất hiện có cộng 1, không giới hạn số mức).

### 16.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 400 | PRB-001 | tên rỗng hoặc quá 60 ký tự | |
| 403 | PRB-002 | thiếu `PROBLEM_AUTHORING:CREATE` | |
| 403 | PRB-004 | có Function nhưng không phải vai trò `ADMIN` (A2) | `403 FORBIDDEN` theo BD |
| 409 | PRB-022 | tên trùng mức khác (không phân biệt hoa thường) | cũng khi vi phạm unique index `lower(display_name)` do tạo đồng thời |

### 16.5 Luồng xử lý
1. Kiểm quyền hai lớp. Cắt tên, kiểm độ dài.
2. Sinh `code` slug (BR-03), xếp `sort_order = max + 1` trong cùng giao dịch.
3. `INSERT`; vi phạm unique -> `409 PRB-022`.
4. `AuditLogPort.record(actor, "PROBLEM_LEVEL_CREATED", "PROBLEM_LEVEL", id, null, {...})` **cùng giao dịch**; ghi nhật ký lỗi thì cả thao tác rollback (`SHR0201:809`).

### 16.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `problem_levels` | C | |
| `identity.system_audit_logs` qua `AuditLogPort` | C | không ghi thẳng schema `identity` |

### 16.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB16-N1 | Normal | ADMIN thêm "Rất khó" -> `201`, `code = RAT-KHO`, xếp cuối, có một dòng nhật ký |
| PB16-A1 | Abnormal | A2 gọi trực tiếp API -> `403 PRB-004`, không có dòng nhật ký |
| PB16-A2 | Abnormal | trùng tên "dễ" (khác hoa thường) -> `409 PRB-022` |
| PB16-A3 | Abnormal | `AuditLogPort` ném lỗi -> không có mức mới nào được tạo (rollback) |
| PB16-B1 | Boundary | tên đúng 60 ký tự được chấp nhận; 61 ký tự bị `400 PRB-001` |

## 17. PATCH /levels/{levelId} (`UpdateProblemLevel`)

### 17.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `PROBLEM_AUTHORING:UPDATE` + `ADMIN` |
| Idempotent | có (cùng tên cho cùng kết quả) |
| Nguồn BD | `SHR0201_problem_management.md:727`, EVT-28 |

### 17.2 Request
`displayName` (string, như mục 16.2). `code` **không đổi** khi đổi tên (`02-bd/database/F2_problem-bank.md:96`).

### 17.3 Response (2xx)
`200` + `LevelDto`.

### 17.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 400 | PRB-001 | tên rỗng hoặc quá 60 ký tự | |
| 403 | PRB-002, PRB-004 | thiếu quyền / không phải `ADMIN` | |
| 404 | PRB-013 | mức không tồn tại | |
| 409 | PRB-022 | tên trùng mức khác | |

### 17.5 Luồng xử lý
Kiểm quyền; đọc dòng (404 nếu không có); cắt và kiểm tên; tên không đổi so với hiện tại thì trả `200` không ghi nhật ký; ngược lại `UPDATE display_name`,
`AuditLogPort.record(actor, "PROBLEM_LEVEL_RENAMED", "PROBLEM_LEVEL", id, {displayName: cũ}, {displayName: mới})` cùng giao dịch. Tăng phiên bản cache danh sách (BR-25).

### 17.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `problem_levels` | R, U | |
| `identity.system_audit_logs` qua `AuditLogPort` | C | |

### 17.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB17-N1 | Normal | đổi "Khó" thành "Nâng cao": `code` vẫn `HARD`, một dòng nhật ký có giá trị cũ và mới |
| PB17-A1 | Abnormal | id không tồn tại -> `404 PRB-013`; trùng tên mức khác -> `409 PRB-022` |
| PB17-B1 | Boundary | gửi lại đúng tên hiện tại -> `200`, không thêm dòng nhật ký |

## 18. PUT /levels/order (`ReorderProblemLevels`)

### 18.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `PROBLEM_AUTHORING:UPDATE` + `ADMIN` |
| Idempotent | có |
| Nguồn BD | `SHR0201_problem_management.md:728`, EVT-29 |

### 18.2 Request
`{ "orderedIds": [uuid] }` — danh sách **đầy đủ** id mức theo thứ tự mới.

### 18.3 Response (2xx)
`200 { "items": [ LevelDto ] }` — toàn bộ danh sách theo thứ tự mới (`SHR0201:688`).

### 18.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 403 | PRB-002, PRB-004 | thiếu quyền / không phải `ADMIN` | |
| 422 | PRB-049 | `orderedIds` không đúng bằng tập id hiện có (thiếu, thừa, trùng) | giao diện tải lại danh sách |

### 18.5 Luồng xử lý
Trong một giao dịch: khoá các dòng `problem_levels` (`FOR UPDATE`), so sánh tập id, ghi `sort_order = 1..n` theo thứ tự gửi lên, một dòng nhật ký `PROBLEM_LEVEL_REORDERED`
(`before`/`after` là danh sách id). Sắp xếp theo độ khó ở danh sách bài đi theo `sort_order` này (`02-bd/database/F2_problem-bank.md:98`). Tăng phiên bản cache.

### 18.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `problem_levels` | R, U | khoá dòng |
| `identity.system_audit_logs` qua `AuditLogPort` | C | |

### 18.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB18-N1 | Normal | đổi chỗ hai mức -> `sort_order` đổi, cột "Độ khó" của danh sách bài sắp theo thứ tự mới |
| PB18-A1 | Abnormal | thiếu một id hoặc có id lạ -> `422 PRB-049`, thứ tự cũ giữ nguyên |
| PB18-B1 | Boundary | chỉ còn một mức, gửi danh sách một phần tử -> `200` |

## 19. DELETE /levels/{levelId} (`DeleteProblemLevel`)

### 19.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `PROBLEM_AUTHORING:DELETE` + `ADMIN` |
| Idempotent | không (xoá lần hai trả `404`) |
| Nguồn BD | `SHR0201_problem_management.md:729`, EVT-30; `02-bd/database/F2_problem-bank.md:103-106` |

### 19.2 Request
`levelId` (uuid) ở đường dẫn.

### 19.3 Response (2xx)
`204 No Content`.

### 19.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 403 | PRB-002, PRB-004 | thiếu quyền / không phải `ADMIN` | |
| 404 | PRB-013 | mức không tồn tại | |
| 422 | PRB-032 | còn `problems.level_id` trỏ tới (mọi trạng thái, kể cả `deleted = true`) | `details = { "reason": "IN_USE", "referencingProblemCount": n }` |
| 422 | PRB-033 | đây là mức cuối cùng | `details = { "reason": "LAST_LEVEL" }` |

### 19.5 Luồng xử lý
1. Trong một giao dịch, khoá toàn bộ dòng `problem_levels` (`FOR UPDATE`) để hai lần xoá song song không cùng qua được kiểm "còn ít nhất một mức".
2. Đếm bài tham chiếu: `> 0` -> `422 PRB-032` kèm số đếm. Đếm số mức: `= 1` -> `422 PRB-033`.
3. `DELETE`; vi phạm FK (lưới an toàn thứ hai, `NO ACTION`) cũng dịch thành `422 PRB-032`.
4. Nhật ký `PROBLEM_LEVEL_DELETED` cùng giao dịch. Tăng phiên bản cache.

### 19.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `problem_levels` | R, D | khoá dòng |
| `problems` | R (đếm) | |
| `identity.system_audit_logs` qua `AuditLogPort` | C | |

### 19.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB19-N1 | Normal | xoá mức không còn bài -> `204`, mức biến khỏi `ListProblemLevels`, một dòng nhật ký |
| PB19-A1 | Abnormal | mức còn một bài `deleted = true` tham chiếu -> `422 PRB-032`, `referencingProblemCount = 1` |
| PB19-A2 | Abnormal | xoá mức cuối cùng -> `422 PRB-033` |
| PB19-A3 | Abnormal | A2 gọi -> `403 PRB-004` |
| PB19-B1 | Boundary | hai ADMIN cùng xoá hai mức khi chỉ còn hai mức -> đúng một lần thành công, lần kia `422 PRB-033` |

## 20. GET /topics (`ListProblemTopics`)

### 20.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | đã xác thực |
| Idempotent | có |
| Nguồn BD | `SHR0201_problem_management.md:713` |

### 20.2 Request (query)
`includeCounts` (bool, mặc định `false`, chỉ `ADMIN` được hưởng) như mục 15.2.

### 20.3 Response (2xx)
`{ "items": [ TopicDto ] }` theo `sort_order`.

### 20.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 401 | (mã chung) | thiếu token | |

### 20.5 Luồng xử lý
Đọc `topics ORDER BY sort_order`; `includeCounts` đếm `problem_topics.topic_id` mọi trạng thái bài (`02-bd/database/F2_problem-bank.md:80-82`).

### 20.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `topics`, `problem_topics` | R | chỉ mục `problem_topics(topic_id)` |

### 20.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB20-N1 | Normal | trả mọi chủ đề theo `sort_order`, không giới hạn số lượng |
| PB20-A1 | Abnormal | A2 gửi `includeCounts=true` -> `problemCount = null` |
| PB20-B1 | Boundary | kho chưa có chủ đề nào -> `items = []` |

## 21. POST /topics (`CreateProblemTopic`)

### 21.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `PROBLEM_AUTHORING:CREATE` + `ADMIN` (`02-bd/security/F2_problem-bank.md:17-24`) |
| Idempotent | không |
| Nguồn BD | `SHR0201_problem_management.md:721`, EVT-21 |

### 21.2 Request
`{ "name": string }` — cắt khoảng trắng, 1 đến 60 ký tự, không trùng chủ đề khác không phân biệt hoa thường (`SHR0201:803`, Sheet 9 NO 12; 60 là suy luận của BD).

### 21.3 Response (2xx)
`201` + `TopicDto`.

### 21.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 400 | PRB-001 | tên rỗng / quá 60 ký tự | |
| 403 | PRB-002, PRB-004 | thiếu quyền / không phải `ADMIN` | |
| 409 | PRB-021 | tên trùng | |

### 21.5 Luồng xử lý
Như mục 16.5, bảng `topics`, nhật ký `PROBLEM_TOPIC_CREATED`, `target_type = PROBLEM_TOPIC`.

### 21.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `topics` | C | unique `lower(name)` |
| `identity.system_audit_logs` qua `AuditLogPort` | C | |

### 21.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB21-N1 | Normal | thêm "Quy hoạch động" -> `201`, `code = QUY-HOACH-DONG`, xếp cuối |
| PB21-A1 | Abnormal | A2 gọi -> `403 PRB-004`; trùng tên "mảng" vs "Mảng" -> `409 PRB-021` |
| PB21-B1 | Boundary | tên 60/61 ký tự; tên chỉ gồm ký hiệu (slug rỗng) -> dùng mã dự phòng `TOPIC`, `TOPIC-2` (BR-03) |

## 22. PATCH /topics/{topicId} (`UpdateProblemTopic`)

### 22.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `PROBLEM_AUTHORING:UPDATE` + `ADMIN` |
| Idempotent | có |
| Nguồn BD | `SHR0201_problem_management.md:722`, EVT-22 |

### 22.2 Request
`{ "name": string }` như mục 21.2; `code` giữ nguyên.

### 22.3 Response (2xx)
`200` + `TopicDto`.

### 22.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 400 | PRB-001 | tên không hợp lệ | |
| 403 | PRB-002, PRB-004 | | |
| 404 | PRB-012 | chủ đề không tồn tại | |
| 409 | PRB-021 | tên trùng | |

### 22.5 Luồng xử lý
Như mục 17.5; nhật ký `PROBLEM_TOPIC_RENAMED`.

### 22.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `topics` | R, U | |
| `identity.system_audit_logs` qua `AuditLogPort` | C | |

### 22.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB22-N1 | Normal | đổi tên -> `code` giữ nguyên, danh sách bài hiển thị tên mới sau khi đóng popup |
| PB22-A1 | Abnormal | id lạ -> `404 PRB-012`; trùng tên -> `409 PRB-021` |
| PB22-B1 | Boundary | gửi đúng tên cũ -> `200`, không ghi nhật ký |

## 23. PUT /topics/order (`ReorderProblemTopics`)

### 23.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `PROBLEM_AUTHORING:UPDATE` + `ADMIN` |
| Idempotent | có |
| Nguồn BD | `SHR0201_problem_management.md:723`, EVT-23 |

### 23.2 Request
`{ "orderedIds": [uuid] }` — danh sách đầy đủ.

### 23.3 Response (2xx)
`200 { "items": [ TopicDto ] }` — toàn bộ danh sách thứ tự mới (`SHR0201:684`).

### 23.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 403 | PRB-002, PRB-004 | | |
| 422 | PRB-049 | tập id không khớp tập hiện có | |

### 23.5 Luồng xử lý
Như mục 18.5, bảng `topics`, nhật ký `PROBLEM_TOPIC_REORDERED`.

### 23.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `topics` | R, U | |
| `identity.system_audit_logs` qua `AuditLogPort` | C | |

### 23.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB23-N1 | Normal | đổi thứ tự -> `ListProblemTopics` trả đúng thứ tự mới |
| PB23-A1 | Abnormal | danh sách thiếu id -> `422 PRB-049`, thứ tự cũ giữ |
| PB23-B1 | Boundary | danh sách rỗng khi kho chưa có chủ đề -> `200 { items: [] }` |

## 24. DELETE /topics/{topicId} (`DeleteProblemTopic`)

### 24.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `PROBLEM_AUTHORING:DELETE` + `ADMIN` |
| Idempotent | không |
| Nguồn BD | `SHR0201_problem_management.md:724`, EVT-24, Sheet 9 NO 13 (dòng 804) |

### 24.2 Request
`topicId` (uuid) ở đường dẫn.

### 24.3 Response (2xx)
`204 No Content`.

### 24.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 403 | PRB-002, PRB-004 | | |
| 404 | PRB-012 | chủ đề không tồn tại | |
| 422 | PRB-031 | còn `problem_topics.topic_id` trỏ tới (mọi trạng thái, kể cả bài `deleted = true`) | `details = { "referencingProblemCount": n }`; giao diện hiện "Còn {n} bài toán thuộc chủ đề này..." |

### 24.5 Luồng xử lý
Đếm tham chiếu -> `PRB-031` nếu `> 0`; `DELETE`; FK `NO ACTION` là lưới an toàn (vi phạm FK -> `PRB-031`); nhật ký `PROBLEM_TOPIC_DELETED` cùng giao dịch; tăng phiên bản cache.

### 24.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `topics`, `problem_topics` | R, D | |
| `identity.system_audit_logs` qua `AuditLogPort` | C | |

### 24.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB24-N1 | Normal | xoá chủ đề chưa gán bài -> `204` |
| PB24-A1 | Abnormal | còn một bài `UNPUBLISHED` gắn chủ đề -> `422 PRB-031`, `referencingProblemCount = 1` |
| PB24-A2 | Abnormal | A2 gọi -> `403 PRB-004` |
| PB24-B1 | Boundary | chủ đề chỉ còn bài đã `deleted = true` tham chiếu -> vẫn từ chối |

## 25. GET /authoring/problems (`ListProblemsAdmin`)

### 25.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `PROBLEM_AUTHORING:READ`; A2 chỉ thấy bài có `author_id` = người gọi, A3 thấy toàn kho |
| Idempotent | có |
| Phân trang | có (`pageSize` 8/20/50, mặc định 8) |
| Nguồn BD | `SHR0201_problem_management.md:710`, DTO dòng 671-679; Sheet 9 NO 1-4 (dòng 792-795) |

### 25.2 Request (query)
| Trường | Kiểu | Bắt buộc | Ràng buộc | Ghi chú |
| :--- | :--- | :-: | :--- | :--- |
| `q` | string | không | tối đa 100 ký tự (`SHR0201:795`) | khớp `code`, `title` không phân biệt hoa thường |
| `level` | uuid | không | id mức độ khó | tab độ khó (`SHR0201` EVT-3) |
| `status` | enum | không | `UNPUBLISHED`, `PUBLISHED` | |
| `sort` | enum | không | `code`, `title`, `level`, `status`, `submissionCount`, `acRate`, `updatedAt`; mặc định `updatedAt` | bảy cột sắp xếp được; `level` theo `sort_order`; mặc định là suy luận `[SoT: Suy luận]` |
| `dir` | enum | không | `asc`, `desc`; mặc định `desc` | |
| `page`, `pageSize` | int | không | `pageSize` thuộc {8, 20, 50} | giá trị sai -> `400 PRB-001` (endpoint quản trị đọc nghiêm) |

### 25.3 Response (2xx)
| Trường | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `items[].id`, `code`, `title` | uuid, string, string | |
| `items[].topicNames` | string[] | |
| `items[].levelId`, `levelCode`, `levelName` | uuid, string, string | |
| `items[].status` | enum | |
| `items[].submissionCount` | int | `0` khi chưa có dòng `problem_stats` |
| `items[].assignedClassCount` | int | số lớp có dòng `class_assignments` với `removed_at IS NULL` (cảnh báo xoá, `SHR0201:677`) |
| `items[].acRate` | number\|null | |
| `items[].testcaseCount` | int | tổng dòng `testcases` của bài (mọi trạng thái duyệt) |
| `items[].updatedAt` | datetime | |
| `page` | PageMeta | |
| `totals` | `{ total: int, published: int }` | hai số tổng của dòng phụ tiêu đề, **trong phạm vi actor, trước khi lọc** (`SHR0201:710`) |

### 25.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 400 | PRB-001 | `pageSize`, `sort`, `dir`, `status` sai | |
| 403 | PRB-002 | thiếu `PROBLEM_AUTHORING:READ` | |

### 25.5 Luồng xử lý
1. Kiểm quyền; phạm vi: `deleted = false` luôn luôn (bài đã ẩn mềm không hiện ở màn quản lý, `02-bd/database/F2_problem-bank.md:24`); A2 thêm `author_id = :người gọi`.
2. Áp lọc `q`, `level`, `status`; tính `totals` trên phạm vi actor không lọc; đếm, sắp xếp, cắt trang (kẹp trang vượt phạm vi).
3. Ghép `topicNames`, `submissionCount`/`acRate` (`LEFT JOIN problem_stats`), `testcaseCount`, `assignedClassCount` bằng truy vấn gộp theo nhóm (không N+1).

### 25.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `problems`, `problem_levels`, `problem_topics`, `topics`, `problem_stats`, `testcases` (chỉ đếm), `class_assignments` (chỉ đếm) | R | chỉ mục `problems(author_id)`, `problems(status, deleted)` |

### 25.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB25-N1 | Normal | A3 thấy toàn kho; `totals.published` đếm đúng bài đã xuất bản |
| PB25-N2 | Normal | sắp theo `level` đi theo `problem_levels.sort_order`, không theo tên |
| PB25-A1 | Abnormal | A2 chỉ thấy bài của mình, kể cả khi truyền `q` khớp bài người khác |
| PB25-A2 | Abnormal | bài `deleted = true` không xuất hiện ở mọi bộ lọc trạng thái |
| PB25-A3 | Abnormal | `pageSize=7` -> `400 PRB-001`; thiếu `PROBLEM_AUTHORING:READ` -> `403 PRB-002` |
| PB25-B1 | Boundary | `q` đúng 100 ký tự được chấp nhận, 101 ký tự -> `400 PRB-001`; bài chưa có thống kê -> `submissionCount = 0`, `acRate = null` |

## 26. GET /authoring/problems/topic-distribution (`GetTopicDistribution`)

### 26.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `PROBLEM_AUTHORING:READ`; phạm vi theo actor như mục 25 |
| Idempotent | có |
| Nguồn BD | `SHR0201_problem_management.md:711`, DTO NO 10 (dòng 680) |

### 26.2 Request
Không tham số.

### 26.3 Response (2xx)
`{ "totalProblemsWithTopic": int, "totalTopicsUsed": int, "items": [ { "topicId", "topicName", "count": int, "percent": number } ] }`. Chỉ chủ đề đang được dùng trong phạm vi actor; `percent` = `count / totalProblemsWithTopic * 100`
(một chữ số thập phân), tính trên tổng số bài có ít nhất một chủ đề (`SHR0201` Khu E NO 2, 5).

### 26.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 403 | PRB-002 | thiếu quyền | |

### 26.5 Luồng xử lý
`GROUP BY topic_id` trên `problem_topics` nối `problems` với `deleted = false` và phạm vi tác giả của actor; `totalProblemsWithTopic = COUNT(DISTINCT problem_id)`.

### 26.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `problem_topics`, `topics`, `problems` | R | |

### 26.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB26-N1 | Normal | 10 bài, 5 gắn "Mảng": `count = 5`, `percent` tính trên bài có chủ đề |
| PB26-A1 | Abnormal | A2 chỉ đếm bài của mình |
| PB26-B1 | Boundary | kho không có bài nào gắn chủ đề -> `items = []`, `totalProblemsWithTopic = 0`, không chia cho 0 |

## 27. GET /authoring/problems/attention (`GetAttentionList`)

### 27.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `PROBLEM_AUTHORING:READ`; phạm vi theo actor |
| Idempotent | có |
| Nguồn BD | `SHR0201_problem_management.md:712`; công thức Sheet 5 Khu vực F NO 5 (dòng 528) |

### 27.2 Request
Không tham số.

### 27.3 Response (2xx)
`{ "items": [ { "ruleCode": "NO_TESTCASE"|"LOW_AC"|"HIDDEN_FROM_LEARNERS"|"STALE_DRAFT", "count": int } ] }` — luôn đủ bốn phần tử theo thứ tự đó, `count = 0` nếu không bài nào khớp. `ruleCode` là khoá tra nhãn i18n.

### 27.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 403 | PRB-002 | thiếu quyền | |

### 27.5 Luồng xử lý
Bốn truy vấn đếm trên bài `deleted = false` trong phạm vi actor:
- `NO_TESTCASE`: bài không có dòng `testcases` nào.
- `LOW_AC`: `problem_stats.ac_rate < 30` (ngưỡng cấu hình `problem-bank.attention.low-ac-percent`, mặc định 30, `DEC-2026-0831-problem-management-lifecycle-details`; BD ghi `0.30` nhưng cột là thang 0-100, `02-bd/database/F2_problem-bank.md:215`), bỏ qua `ac_rate IS NULL`.
- `HIDDEN_FROM_LEARNERS`: `status = UNPUBLISHED AND published_at IS NOT NULL`.
- `STALE_DRAFT`: `status = UNPUBLISHED AND published_at IS NULL AND updated_at < now() - interval '7 days'` (ngưỡng cấu hình `problem-bank.attention.stale-draft-days`, mặc định 7).
  **Lệch giữa BD và RD**: RD nói "kể từ lần sửa gần nhất" và DB nói `updated_at` dùng cho ngưỡng này (`01-rd/req/F2_problem-bank.md:165-167`, `02-bd/database/F2_problem-bank.md:35`), nhưng công thức Sheet 5 dùng `created_at`.
  DD theo RD và DB (`updated_at`) `[SoT: Suy luận, Q-12 mục 61]`.

### 27.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `problems`, `testcases`, `problem_stats` | R | |

### 27.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB27-N1 | Normal | bài có AC 20% và bài nháp 10 ngày không sửa -> `LOW_AC = 1`, `STALE_DRAFT = 1` |
| PB27-A1 | Abnormal | bài `deleted = true` không được đếm vào bất kỳ quy tắc nào |
| PB27-B1 | Boundary | AC đúng 30% không khớp `LOW_AC`; bài chưa có thống kê không khớp `LOW_AC`; nháp sửa đúng 7 ngày trước không khớp `STALE_DRAFT` |

## 28. POST /authoring/problems/export (`ExportProblemsCsv`)

### 28.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `PROBLEM_AUTHORING:READ` (`02-bd/security/F2_problem-bank.md:42-43`); phạm vi theo actor |
| Idempotent | có (chỉ đọc; dùng `POST` vì danh sách id có thể dài) |
| Nguồn BD | `SHR0201_problem_management.md:719` (EVT-15); F2-17 (`01-rd/req/F2_problem-bank.md:175-180`) |

### 28.2 Request
`{ "problemIds": [uuid] }` **hoặc** `{ "filter": { "q"?, "level"?, "status"? } }` — đúng một trong hai: bài đã chọn, hoặc toàn bộ kết quả đang lọc (RD). Cả hai cùng có hoặc cùng thiếu -> `400 PRB-001`.

### 28.3 Response (2xx)
`200`, `Content-Type: text/csv; charset=utf-8`, `Content-Disposition: attachment; filename="problems-YYYYMMDD.csv"`, có BOM UTF-8 để Excel đọc đúng tiếng Việt `[SoT: Suy luận]`.
Cột (một dòng tiêu đề tiếng Việt cố định): `Mã, Tiêu đề, Chủ đề, Độ khó, Trạng thái, Lượt nộp, Tỉ lệ AC, Số testcase` — **chỉ dữ liệu bảng**, không đề bài, đặc tả, testcase (F2-17). `Chủ đề` nối bằng `; `.
Ô bắt đầu bằng `=`, `+`, `-`, `@`, tab hoặc xuống dòng được thêm `'` ở đầu để chống CSV injection (tiêu đề do người soạn nhập, ponytail bậc 6).

### 28.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 400 | PRB-001 | thiếu/thừa `problemIds` và `filter`; `problemIds` rỗng | |
| 403 | PRB-002 | thiếu `PROBLEM_AUTHORING:READ` | id ngoài phạm vi A2 bị bỏ qua lặng lẽ, không trả `403` từng dòng |

### 28.5 Luồng xử lý
Dựng cùng truy vấn như mục 25 (không phân trang, phạm vi actor, `deleted = false`), ghi CSV theo luồng (streaming). Không ghi nhật ký (RD/BD không yêu cầu cho xuất CSV).

### 28.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| như mục 25.6 | R | |

### 28.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB28-N1 | Normal | xuất 3 bài đã chọn -> 3 dòng dữ liệu + 1 dòng tiêu đề, đúng 8 cột |
| PB28-N2 | Normal | xuất theo `filter` -> mọi bài khớp trong phạm vi actor |
| PB28-A1 | Abnormal | CSV không chứa nội dung đề, đặc tả hay testcase; A2 chọn id bài người khác -> dòng đó không có trong tệp |
| PB28-B1 | Boundary | tiêu đề bắt đầu bằng `=SUM(1,1)` -> ô được thêm `'` ở đầu |

## 29. POST /authoring/problems/bulk/publish (`PublishProblem`)

### 29.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `PROBLEM_AUTHORING:UPDATE`; A2 chỉ bài của mình (từng bài) |
| Idempotent | có (bài đã `PUBLISHED` tính là thành công, không đổi gì) |
| Nguồn BD | `SHR0201_problem_management.md:714`, EVT-11, Sheet 9 NO 7 (dòng 798) |

### 29.2 Request
`{ "problemIds": [uuid] }` — 1 đến 20 id (xuất bản chạy sandbox đáp án mẫu cho từng bài nên giới hạn thấp hơn các thao tác lô khác `[SoT: Suy luận]`).

### 29.3 Response (2xx)
`200` + `BulkProblemActionResultDto`. Mỗi bài xử lý **độc lập**, giao dịch riêng.

### 29.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 400 | PRB-001 | `problemIds` rỗng hoặc quá 20 phần tử | `SHR0201:796` |
| 403 | PRB-002 | thiếu `PROBLEM_AUTHORING:UPDATE` | cấp yêu cầu |
| (trong `failedItems`) | PRB-003 | A2 không phải tác giả bài `PUBLISHED` này (bản nháp của người khác trả `PRB-010`) | |
| (trong `failedItems`) | PRB-010 | bài không tồn tại hoặc đã `deleted` | |
| (trong `failedItems`) | PRB-030 | chưa đạt checklist; `details.items` liệt kê điều kiện thiếu | |
| (trong `failedItems`) | PRB-050 | sandbox không chạy được đáp án mẫu | bài đó giữ nguyên trạng thái |

### 29.5 Luồng xử lý
Với từng id, thực hiện BR-08 (chuyển `UNPUBLISHED -> PUBLISHED`): kiểm phạm vi, chạy checklist (BR-07, gồm một lần chạy đáp án mẫu), đặt `status = PUBLISHED`, `published_at = now()`, `updated_by`, `updated_at`,
ghi nhật ký `PROBLEM_PUBLISHED` qua `AuditLogPort` cùng giao dịch của bài đó (nhật ký lỗi thì bài đó không đổi, `SHR0201:800`), sau commit phát `ProblemPublished`, tăng phiên bản cache danh sách.

### 29.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `problems`, `testcases`, `worked_examples`, `problem_specs`, `problem_reference_solutions` | R, `problems` U | |
| `identity.system_audit_logs` qua `AuditLogPort` | C | một dòng mỗi bài |
| MinIO, go-judge qua `SampleSolutionRunnerPort` | R / chạy | đọc nội dung testcase để chạy đáp án mẫu |

### 29.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB29-N1 | Normal | lô 2 bài đạt checklist -> cả hai vào `succeededIds`, hai dòng nhật ký, hai sự kiện `ProblemPublished` |
| PB29-N2 | Normal | bài đã `PUBLISHED` gọi lại -> thành công, không đổi `published_at` |
| PB29-A1 | Abnormal | lô hỗn hợp 1 đạt + 1 thiếu testcase -> bài đạt được xuất bản, bài kia vào `failedItems` với `PRB-030` |
| PB29-A2 | Abnormal | A2 chọn bài người khác -> `failedItems` `PRB-003`, bài không đổi |
| PB29-A3 | Abnormal | `AuditLogPort` lỗi cho một bài -> bài đó vẫn `UNPUBLISHED` (rollback) |
| PB29-B1 | Boundary | 20 id được chấp nhận, 21 id bị `400 PRB-001` |

## 30. POST /authoring/problems/bulk/unpublish (`UnpublishProblem`)

### 30.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `PROBLEM_AUTHORING:UPDATE`; A2 chỉ bài của mình |
| Idempotent | có |
| Nguồn BD | `SHR0201_problem_management.md:715`, EVT-11 |

### 30.2 Request
`{ "problemIds": [uuid] }` — 1 đến 200 id.

### 30.3 Response (2xx)
`200` + `BulkProblemActionResultDto`.

### 30.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 400 | PRB-001 | rỗng / quá 200 | |
| 403 | PRB-002 | thiếu quyền | |
| (trong `failedItems`) | PRB-003, PRB-010 | không phải tác giả / không tồn tại | |

### 30.5 Luồng xử lý
Mỗi bài: `status = UNPUBLISHED` (không điều kiện tiên quyết, F2-15), giữ `published_at`; **không xoá** lượt nộp, bookmark, phiên phỏng vấn (`01-rd/req/F2_problem-bank.md:145-148`); nhật ký `PROBLEM_UNPUBLISHED`; sau commit phát `ProblemUnpublished`;
tăng phiên bản cache để bài biến mất khỏi `problem_list` ngay. Bài `UNPUBLISHED` sẵn thì bỏ qua, tính thành công.

### 30.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `problems` | R, U | |
| `identity.system_audit_logs` qua `AuditLogPort` | C | |

### 30.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB30-N1 | Normal | rút bài `PUBLISHED` -> biến khỏi `ListPublishedProblems` ngay; lượt nộp và bookmark còn nguyên |
| PB30-A1 | Abnormal | bài người khác với A2 -> `PRB-003` trong `failedItems` |
| PB30-B1 | Boundary | bài đã `UNPUBLISHED` -> thành công, không thêm dòng nhật ký |

## 31. POST /authoring/problems/bulk/difficulty (`ChangeDifficulty`)

### 31.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `PROBLEM_AUTHORING:UPDATE`; A2 chỉ bài của mình |
| Idempotent | có |
| Nguồn BD | `SHR0201_problem_management.md:716`, EVT-12, Sheet 9 NO 6 và 19 |

### 31.2 Request
`{ "problemIds": [uuid] (1..200), "levelId": uuid }`.

### 31.3 Response (2xx)
`200` + `BulkProblemActionResultDto`.

### 31.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 400 | PRB-001 | thiếu `levelId`, `problemIds` rỗng/quá 200 | |
| 403 | PRB-002 | thiếu quyền | |
| 422 | PRB-039 | `levelId` không còn tồn tại (ADMIN vừa xoá) | **từ chối cả lô**, `details.field = levelId`; "Mức độ khó này không còn tồn tại" (`SHR0201:810`) |
| (trong `failedItems`) | PRB-003, PRB-010 | không phải tác giả / không tồn tại | |

### 31.5 Luồng xử lý
Kiểm `levelId` tồn tại trước (cả lô). Mỗi bài: `UPDATE problems SET level_id`, `updated_by/at`; nhật ký `PROBLEM_DIFFICULTY_CHANGED` kèm `{levelId: cũ}` và `{levelId: mới}`. Bài đã đúng mức đó: thành công, không ghi nhật ký. Tăng phiên bản cache.

### 31.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `problem_levels` | R | |
| `problems` | R, U | |
| `identity.system_audit_logs` qua `AuditLogPort` | C | |

### 31.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB31-N1 | Normal | đổi 3 bài sang "Khó" -> cả 3 `levelCode = HARD`, 3 dòng nhật ký |
| PB31-A1 | Abnormal | `levelId` vừa bị xoá -> `422 PRB-039`, không bài nào đổi |
| PB31-A2 | Abnormal | thiếu `levelId` -> `400 PRB-001` |
| PB31-B1 | Boundary | bài đã đúng mức -> thành công không thêm nhật ký |

## 32. POST /authoring/problems/bulk/topic (`AssignTopic`)

### 32.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `PROBLEM_AUTHORING:UPDATE`; A2 chỉ bài của mình |
| Idempotent | có (quan hệ đã tồn tại thì bỏ qua) |
| Nguồn BD | `SHR0201_problem_management.md:717`, EVT-13 |

### 32.2 Request
`{ "problemIds": [uuid] (1..200), "topicId": uuid }`.

### 32.3 Response (2xx)
`200` + `BulkProblemActionResultDto`.

### 32.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 400 | PRB-001 | thiếu `topicId`, tập rỗng/quá 200 | `SHR0201:797` |
| 403 | PRB-002 | thiếu quyền | |
| 422 | PRB-039 | `topicId` không tồn tại | từ chối cả lô |
| (trong `failedItems`) | PRB-003, PRB-010 | | |

### 32.5 Luồng xử lý
Mỗi bài: `INSERT INTO problem_topics ... ON CONFLICT DO NOTHING`; chỉ ghi nhật ký `PROBLEM_TOPIC_ASSIGNED` khi thực sự thêm quan hệ mới; cập nhật `updated_by/at`. Tăng phiên bản cache.

### 32.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `topics` | R | |
| `problem_topics`, `problems` | C, U | |
| `identity.system_audit_logs` qua `AuditLogPort` | C | |

### 32.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB32-N1 | Normal | gán "Đồ thị" cho 2 bài -> 2 quan hệ mới, cột "Chủ đề" cập nhật |
| PB32-A1 | Abnormal | `topicId` không tồn tại -> `422 PRB-039` |
| PB32-B1 | Boundary | một bài đã có chủ đề đó -> thành công không nhân đôi quan hệ, không thêm nhật ký |

## 33. POST /authoring/problems/bulk/delete (`DeleteProblem`)

### 33.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `PROBLEM_AUTHORING:DELETE`; A2 chỉ bài của mình |
| Idempotent | không (bài đã xoá trả `PRB-010` trong `failedItems`) |
| Nguồn BD | `SHR0201_problem_management.md:720`, EVT-18; `DEC-2026-1008-problem-delete-and-duplicate` |

### 33.2 Request
`{ "problemIds": [uuid] }` — 1 đến 200 id.

### 33.3 Response (2xx)
`200` + `BulkProblemActionResultDto`.

### 33.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 400 | PRB-001 | tập rỗng/quá 200 | |
| 403 | PRB-002 | thiếu `PROBLEM_AUTHORING:DELETE` | |
| (trong `failedItems`) | PRB-003, PRB-010 | không phải tác giả / không tồn tại hoặc đã xoá | |

### 33.5 Luồng xử lý
Mỗi bài (BR-09): `UPDATE problems SET status = 'UNPUBLISHED', deleted = true, updated_by/at`; **không** xoá `testcases`, `problem_topics`, `class_assignments`, lượt nộp, bookmark (`SHR0201:799`); **không** từ chối bài đang có lượt nộp hay đang giao cho lớp
(Phương án A, `DEC-2026-1008-problem-delete-and-duplicate`). Nhật ký `PROBLEM_DELETED` cùng giao dịch. Bài đang `PUBLISHED` thì sau commit phát `ProblemUnpublished`. Tăng phiên bản cache.

### 33.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `problems` | R, U | chỉ bật cờ, không `DELETE` |
| `identity.system_audit_logs` qua `AuditLogPort` | C | |

### 33.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB33-N1 | Normal | xoá 1 bài có 50 lượt nộp và đang giao 2 lớp -> thành công; bài biến khỏi mọi bộ lọc quản lý; lượt nộp, bookmark, `class_assignments` còn nguyên |
| PB33-A1 | Abnormal | A2 xoá bài người khác -> `PRB-003` trong `failedItems` |
| PB33-A2 | Abnormal | xoá bài đã xoá -> `PRB-010` trong `failedItems` |
| PB33-B1 | Boundary | lô 200 bài một phần thành công -> `succeededIds` và `failedItems` đúng, popup vẫn đóng được |

## 34. GET /authoring/problems/{problemId} (`GetProblemForAuthoring`)

### 34.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `PROBLEM_AUTHORING:READ` hoặc `TESTCASE_MANAGEMENT:READ`; truy cập: A3 mọi bài chưa `deleted`; A2 bài của mình, hoặc bài `PUBLISHED` của người khác ở chế độ chỉ đọc (`DEC-2026-1011-a2-read-published-problems-of-others`) |
| Idempotent | có |
| Nguồn BD | `SHR0202_problem_authoring.md:648`, `SHR0203_problem_info.md:407`; `SHR0202` Sheet 9 NO 1-2 (dòng 763-764) |

### 34.2 Request
`problemId` (uuid) ở đường dẫn. Dùng chung cho màn soạn, màn chi tiết chỉ đọc và trang xem trước của tác giả (`SHR0202` Q13, dòng 798) — không có endpoint thứ tư cho xem trước.

### 34.3 Response (2xx)
`ProblemAuthoringDetailDto` (mục 1.5). Điều kiện đặc biệt:
- `readOnly = true` khi A2 mở bài `PUBLISHED` của người khác: `testcases[]` không mang `input`, `expectedOutput`, `*DownloadUrl`; thay bằng `testcaseSummary` `{ total, approved, sample, hidden }`. `referenceSolution = null` và không trả mã/URL tải đáp án mẫu; client ẩn thẻ đáp án mẫu thay vì hiện “Chưa có”. [Quyết định owner 2026-10-25]
- `*DownloadUrl` chỉ được ký khi người gọi có `TESTCASE_MANAGEMENT:READ` **và** là tác giả hoặc `ADMIN`.
- `checklist.sampleSolutionAllPass = null` (không chạy sandbox khi tải).
- `aiGeneration.used/limit` luôn có.

### 34.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 403 | PRB-002 | thiếu cả hai quyền `READ` | |
| 404 | PRB-010 | bài không tồn tại, đã `deleted`, hoặc A2 mở **bản nháp của người khác** | cùng thân phản hồi cho cả ba trường hợp, không lộ sự tồn tại |

### 34.5 Luồng xử lý
1. Đọc bài (`deleted = false`). Không có -> `404 PRB-010`.
2. A2 và `author_id` khác người gọi: nếu `status = UNPUBLISHED` -> `404 PRB-010`; nếu `PUBLISHED` -> `readOnly = true`.
3. Nối `problem_specs`, `problem_reference_solutions`, `worked_examples`, `testcases` (theo `position`), `topics`, `tags`, `problem_stats` (không cần ở DTO này), `problem_bank_settings`.
4. Tên người sửa cuối qua `UserDirectoryPort`; lỗi cổng -> `updatedBy.displayName = null`.
5. Tính `checklist` từ số đếm (BR-07, không chạy sandbox).

### 34.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `problems`, `problem_specs`, `problem_reference_solutions`, `worked_examples`, `testcases`, `problem_topics`, `topics`, `problem_tags`, `tags`, `problem_bank_settings` | R | |
| MinIO | presign GET | chỉ với testcase `MINIO` và người có quyền |
| `identity` qua `UserDirectoryPort` | R | tên hiển thị |

### 34.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB34-N1 | Normal | tác giả mở bài của mình: đủ 5 tab dữ liệu, testcase có nội dung, `checklist` đủ 5 mục |
| PB34-N2 | Security | A2 mở bài `PUBLISHED` của người khác -> `200`, `readOnly = true`, không có nội dung testcase hoặc mã đáp án mẫu |
| PB34-A1 | Abnormal | A2 mở bản nháp của người khác -> `404 PRB-010`, thân phản hồi giống id không tồn tại |
| PB34-A2 | Abnormal | bài `deleted` -> `404 PRB-010` kể cả với A3 |
| PB34-A3 | Abnormal | tài khoản không có `READ` nào -> `403 PRB-002` |
| PB34-B1 | Boundary | bài chưa có đặc tả / đáp án mẫu -> `spec = null`, `referenceSolution = null`; testcase `MINIO` -> `input = null` + `inputDownloadUrl` |

## 35. GET /authoring/problems/{problemId}/duplicate-source (`DuplicateProblem`)

### 35.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `PROBLEM_AUTHORING:CREATE` (`SHR0201:793`); A2 chỉ bài của mình, A3 mọi bài chưa `deleted` |
| Idempotent | có (chỉ đọc; không tạo bản ghi nào) |
| Nguồn BD | `SHR0201_problem_management.md:718`, EVT-14; `SHR0202_problem_authoring.md:685` (EVT-2 chế độ soạn mới có `from`) |

### 35.2 Request
`problemId` (uuid) của **bài nguồn**. Nhân bản đúng một bài mỗi lần (`SHR0201` EVT-14).

### 35.3 Response (2xx)
`{ "title", "statementMd", "constraintsMd", "levelId", "limits": {...}, "topicIds": [uuid], "tags": [string], "referenceSolution": {...}|null, "workedExamples": [ WorkedExampleDto (id = null) ],
"willCopyOnSave": { "testcaseCount": int, "hasSpec": bool }, "duplicatedFromProblemId": uuid }`. Không có `id`, `code`, `status` (luôn `UNPUBLISHED` khi tạo), `testcases`, `spec`.
Giao diện thêm hậu tố "(bản sao)" vào `title` (BD giao việc đó cho giao diện, `SHR0202:685`).
`testcases` và `spec` **không** nằm trong nội dung nạp sẵn vì chúng chỉ ghi được sau khi bài có `id`; máy chủ sao chép chúng ở lần Lưu đầu (BR-10, mục 36).

### 35.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 403 | PRB-002, PRB-003 | thiếu quyền / A2 nhân bản bài người khác | |
| 404 | PRB-010 | bài nguồn không tồn tại hoặc đã `deleted` | giao diện hiện trạng thái "không tìm thấy" (`SHR0201` EVT-14) |

### 35.5 Luồng xử lý
Đọc bài nguồn (như mục 34.5 nhưng không có chế độ `readOnly`), trả nội dung theo mục 35.3. **Không ghi gì** vào DB.

### 35.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `problems`, `problem_reference_solutions`, `worked_examples`, `problem_topics`, `problem_tags`, `testcases` (chỉ đếm), `problem_specs` (chỉ kiểm có hay không) | R | |

### 35.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB35-N1 | Normal | trả đủ đề, ràng buộc, ví dụ, đáp án mẫu của bài nguồn; DB không phát sinh bản ghi mới |
| PB35-A1 | Abnormal | A2 nhân bản bài người khác -> `403 PRB-003` |
| PB35-A2 | Abnormal | bài nguồn đã `deleted` -> `404 PRB-010` |
| PB35-B1 | Boundary | bài nguồn chưa có đáp án mẫu/đặc tả -> `referenceSolution = null`, `hasSpec = false` |

## 36. POST /authoring/problems (`SaveProblemContent` — tạo mới)

### 36.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `PROBLEM_AUTHORING:CREATE` |
| Idempotent | không |
| Nguồn BD | `SHR0202_problem_authoring.md:649`; EVT-2, EVT-7 tới EVT-14 (dòng 685-697) |

### 36.2 Request
| Trường | Kiểu | Bắt buộc | Ràng buộc | Ghi chú |
| :--- | :--- | :-: | :--- | :--- |
| `title` | string | có | 1 đến 255 ký tự sau khi cắt | `problems.title VARCHAR(255)` |
| `statementMd` | string | có | không rỗng; tối đa 100.000 ký tự `[SoT: Suy luận]` | Markdown + LaTeX |
| `constraintsMd` | string | không | mặc định `''`; tối đa 20.000 ký tự `[SoT: Suy luận]` | |
| `levelId` | uuid | không | phải tồn tại; thiếu thì dùng mức đầu theo `sort_order` | `SHR0202:442` (Khu H NO 2) |
| `limits.timeLimitMs`, `limits.memoryLimitMb`, `limits.maxOutputSizeKb` | int | không | số nguyên dương; thiếu thì lấy từ `grading_defaults` qua `JudgeConfigReadPort` | `02-bd/database/F2_problem-bank.md:42` giao "giá trị khởi tạo" cho DD |
| `limits.maxSubmissionsPerHour` | int\|null | không | số nguyên dương hoặc `null` | `null` = đọc mặc định hệ thống 60 lúc kiểm, không ghi 60 vào cột (`02-bd/database/F2_problem-bank.md:29`) |
| `topicIds` | uuid[] | không | mỗi id tồn tại | |
| `tags` | string[] | không | tên thẻ, mỗi thẻ 1 đến 50 ký tự, tối đa 20 thẻ `[SoT: Suy luận]` | thẻ chưa có thì tạo mới |
| `referenceSolution` | `{language, sourceCode}`\|null | không | `language` thuộc `JAVA`/`CPP`/`PYTHON`; `sourceCode` không rỗng, tối đa 64 KB `[SoT: Suy luận]` | rỗng/`null` thì không tạo dòng |
| `workedExamples` | WorkedExampleDto[] | không | mỗi `input` và `output` không rỗng | `id` phải là `null` khi tạo |
| `duplicatedFromProblemId` | uuid\|null | không | bài nguồn phải tồn tại, chưa `deleted`, A2 chỉ bài của mình | F2-16 |

### 36.3 Response (2xx)
`201` + `ProblemAuthoringDetailDto` (bài mới `status = UNPUBLISHED`, `author_id` = người gọi, bất biến; `code` do máy chủ sinh từ tiêu đề, BR-03).

### 36.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 400 | PRB-001 | trường bắt buộc rỗng, quá độ dài, giới hạn không dương | `details.fields[]` |
| 403 | PRB-002, PRB-003 | thiếu quyền / A2 nhân bản bài người khác | |
| 404 | PRB-010 | `duplicatedFromProblemId` không tồn tại | |
| 422 | PRB-039 | `levelId` hoặc `topicIds[i]` không còn tồn tại | `SHR0202` Q21: người soạn chọn lại; không tự chuyển mức |

### 36.5 Luồng xử lý
BR-04 (tạo) và BR-10 (nhân bản): một giao dịch: sinh `code`, `INSERT problems`, liên kết chủ đề/thẻ, ví dụ mẫu, đáp án mẫu; nếu có `duplicatedFromProblemId` thì sao chép thêm `problem_specs`, mọi dòng `testcases` (kèm sao chép object MinIO sang khoá mới) từ bài nguồn,
ghi `duplicated_from_problem_id`, nhật ký `PROBLEM_DUPLICATED`. Ví dụ mẫu và đáp án mẫu lấy từ **thân yêu cầu** (giao diện đã nạp sẵn và có thể đã sửa); chỉ `problem_specs` và `testcases` do máy chủ sao chép.

### 36.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `problems`, `problem_topics`, `problem_tags`, `tags`, `worked_examples`, `problem_reference_solutions` | C | |
| `problem_specs`, `testcases` | C (khi nhân bản) | |
| MinIO | copy object | khi testcase nguồn là `MINIO` |
| `problem_levels`, `topics` | R | |
| `identity.system_audit_logs` qua `AuditLogPort` | C | chỉ khi nhân bản |

### 36.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB36-N1 | Normal | tạo bài với đủ trường -> `201`, `code` dạng slug, `status = UNPUBLISHED`, `author_id` = người gọi |
| PB36-N2 | Normal | tạo kèm `duplicatedFromProblemId` -> `problem_specs` và mọi testcase của nguồn được sao chép sang id mới (kể cả object MinIO), `current_testcase_set_version = 1`, `spec_version = 1` |
| PB36-A1 | Abnormal | `title` rỗng hoặc 256 ký tự -> `400 PRB-001` |
| PB36-A2 | Abnormal | `levelId` đã bị xoá -> `422 PRB-039`, không tạo bản ghi |
| PB36-A3 | Abnormal | hai bài cùng tiêu đề -> `code` thứ hai có hậu tố `-2`; trùng với bài đã `deleted` vẫn có hậu tố |
| PB36-B1 | Boundary | thiếu `limits` -> điền từ `grading_defaults`; `maxSubmissionsPerHour = null` giữ `NULL` trong cột |

## 37. PUT /authoring/problems/{problemId}/content (`SaveProblemContent` — cập nhật)

### 37.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `PROBLEM_AUTHORING:UPDATE`; A2 chỉ bài của mình |
| Idempotent | có (cùng thân cho cùng kết quả; `updatedAt` mới sau mỗi lần ghi thật) |
| Nguồn BD | `SHR0202_problem_authoring.md:649`, Sheet 9 NO 3, 4, 10 (dòng 765, 766, 772) |

### 37.2 Request
Cùng trường như mục 36.2 (không có `duplicatedFromProblemId`), thêm:
| Trường | Kiểu | Bắt buộc | Ràng buộc | Ghi chú |
| :--- | :--- | :-: | :--- | :--- |
| `expectedUpdatedAt` | datetime | có | giá trị `updatedAt` đã nhận ở lần tải/ghi gần nhất | khoá lạc quan, mục 1.2 |
| `limits.*` | int | có (cả ba) | số nguyên dương, không `null` | khi cập nhật không còn thiếu-lấy-mặc-định |
| `workedExamples[].id` | uuid\|null | không | `null` = dòng mới; id có sẵn = cập nhật; dòng cũ vắng mặt trong danh sách = xoá | |

Toàn bộ nhóm trường nội dung gửi dạng **trạng thái đầy đủ** (thay thế): `topicIds`, `tags`, `workedExamples`, `referenceSolution` là danh sách/giá trị cuối cùng muốn có.

### 37.3 Response (2xx)
`200` + `ProblemAuthoringDetailDto` mới (có `updatedAt` mới).

### 37.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 400 | PRB-001 | trường bắt buộc rỗng, giá trị không dương, quá độ dài | |
| 403 | PRB-002, PRB-003 | thiếu quyền / A2 sửa bài người khác | |
| 404 | PRB-010 | bài không tồn tại, đã `deleted`, hoặc A2 sửa **bản nháp của người khác** | bài `PUBLISHED` của người khác thì `403 PRB-003` (mục 1.3) |
| 409 | PRB-020 | `expectedUpdatedAt` lệch `problems.updated_at` | "Bài toán đã được người khác cập nhật. Vui lòng tải lại." (`SHR0202:772`); thân lỗi kèm `details.currentUpdatedAt` |
| 422 | PRB-039 | `levelId`/`topicIds[i]` không còn tồn tại | |
| 422 | PRB-053 | bài đang `PUBLISHED` mà danh sách ví dụ mẫu sau lưu còn dưới 2 | quy tắc `SHR0202` Khu vực D NO 6 phía máy chủ `[SoT: Suy luận — BD chỉ ghi chặn ở giao diện]` |

### 37.5 Luồng xử lý
BR-04 (cập nhật): một giao dịch; `UPDATE problems SET ... WHERE id = ? AND updated_at = :expectedUpdatedAt` (0 dòng + bài còn tồn tại -> `409 PRB-020`); thay thế `problem_topics`, `problem_tags`, `worked_examples` (đánh số lại `position`), upsert/xoá `problem_reference_solutions`;
đổi dòng đáp án mẫu **không** tăng `current_testcase_set_version` hay `spec_version` (`02-bd/database/F2_problem-bank.md:243`). Bài đang `PUBLISHED` thì tăng phiên bản cache danh sách.

### 37.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `problems` | U | khoá lạc quan |
| `problem_topics`, `problem_tags`, `tags`, `worked_examples`, `problem_reference_solutions` | C, U, D | |
| `problem_levels`, `topics` | R | kiểm tồn tại |

### 37.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB37-N1 | Normal | sửa đề và thêm 1 ví dụ -> `200`, `updatedAt` đổi, ví dụ mới có `position` cuối |
| PB37-N2 | Normal | xoá chữ trong ô đáp án mẫu -> dòng `problem_reference_solutions` bị xoá; đổi mã đáp án không đổi `spec_version`, `current_testcase_set_version` |
| PB37-A1 | Abnormal | hai người lưu cùng lúc: người sau với `expectedUpdatedAt` cũ -> `409 PRB-020`, dữ liệu người trước giữ nguyên |
| PB37-A2 | Abnormal | A2 sửa bài người khác -> `403 PRB-003` |
| PB37-A3 | Abnormal | bài `PUBLISHED` còn 2 ví dụ, gửi danh sách 1 ví dụ -> `422 PRB-053`, không đổi gì |
| PB37-B1 | Boundary | `title` đúng 255 ký tự được chấp nhận; `maxSubmissionsPerHour = null` giữ `NULL`; `timeLimitMs = 0` bị `400` |

## 38. PUT /authoring/problems/{problemId}/spec (`SaveProblemSpec`)

### 38.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `PROBLEM_AUTHORING:UPDATE`; A2 chỉ bài của mình |
| Idempotent | có (không đổi cột nào thì không tăng `spec_version`) |
| Nguồn BD | `SHR0202_problem_authoring.md:659`; Sheet 9 NO 5, 6, 7 (dòng 767-769); `02-bd/architecture/F3_harness.md` mục 4.5 |
| Bộ kiểm | `FunctionSpecEvaluator.evaluate(FunctionSpecDraft)` trong `algoprep-common` (`03-dd/api/F3_harness.md` mục 6.3, cổng P5); luật và mã `HRN-Vnn` ở `03-dd/validation/F3_harness.md` mục 2 tới 6, **không chép lại ở đây** |

### 38.2 Request
`ProblemSpecDto` (không có `specVersion`) + `expectedUpdatedAt`. Bản nháp được lưu từng phần ở mức "chưa khai chữ ký" (cả `functionName`, `returnType`, `parameters` đều `null` là hợp lệ, `HRN-V07` mềm); đã khai một phần chữ ký thì bị từ chối (`HRN-V17` cứng).
| Trường | Ràng buộc do `problem-bank` kiểm | Ràng buộc do `FunctionSpecEvaluator` kiểm |
| :--- | :--- | :--- |
| `matchingStrategy` | một trong bốn giá trị; mặc định `EXACT` khi tạo dòng lần đầu | `HRN-V22` (với `UNORDERED_SET`) |
| `epsilonValue` | số thập phân | `HRN-V20`: bắt buộc và `> 0` khi `EPSILON`; khi chiến lược khác thì lưu nguyên và bị bỏ qua |
| `functionName`, `returnType`, `parameters`, `nameOverrides` | kiểu JSON cấp ngoài đúng (`parameters` là mảng, `nameOverrides` là đối tượng) | toàn bộ `HRN-V01` tới `HRN-V19` |
| `stdinFormatMd`, `stdoutFormatMd` | văn bản tự do, tối đa 20.000 ký tự `[SoT: Suy luận]` | không |

### 38.3 Response (2xx)
`200 { "spec": ProblemSpecDto (có specVersion mới), "functionWrapperSupported": bool, "warnings": [ { "code": "PRB-W01", "reasonCode": "HRN-V01", "path": "/parameters/0/type", "message": "..." } ], "problemUpdatedAt": datetime }`.
`warnings` dựng từ `SpecEvaluation.unsupportedReasons` (mã `HRN-V01` tới `HRN-V07`); `functionWrapperSupported` = `SpecEvaluation.wrapperSupported` và được ghi vào `problems.function_wrapper_supported`.

### 38.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 400 | PRB-001 | sai kiểu JSON cấp ngoài; văn bản định dạng quá dài | |
| 403 | PRB-002, PRB-003 | | |
| 404 | PRB-010 | bài không tồn tại / `deleted` / bản nháp người khác với A2 | |
| 409 | PRB-020 | `expectedUpdatedAt` lệch | |
| 422 | PRB-036 | `SpecEvaluation.violations` khác rỗng (vi phạm **cứng**) | `details.violations = [ { code: "HRN-Vnn", path, message } ]`; **không lưu gì**. Gồm tổ hợp `UNORDERED_SET` với `BINARY_TREE`/`LINKED_LIST` (`HRN-V22`) và `EPSILON` thiếu giá trị (`HRN-V20`) |

**Kiểu vượt lược đồ không phải lỗi** (F3-13): vi phạm **mềm** `HRN-V01` tới `HRN-V07` -> `200`, `functionWrapperSupported = false`, kèm `warnings[]`. Đây khớp `SHR0202` Sheet 9 NO 5 và `harness` mục 4.5;
`SHR0202` EVT-25 còn nhắc "viền đỏ + toast lỗi" cho trường hợp thiếu `of`/`kind` lạ, DD coi viền đỏ là cách giao diện đánh dấu trường có cảnh báo (`Q-05` mục 61).

### 38.5 Luồng xử lý
BR-05: dựng `FunctionSpecDraft` từ request; `FunctionSpecEvaluator.evaluate`; có `violations` -> `422 PRB-036`. Ngược lại so sánh với dòng hiện có; nếu **bất kỳ cột nào** của dòng `problem_specs` đổi thì `spec_version = spec_version + 1`;
cập nhật `problems.function_wrapper_supported`, `updated_by/at`; xoá key Redis `problem:spec:<problemId>`; sau commit phát `ProblemSpecChanged` (mục 59).

### 38.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `problem_specs` | C, U | unique `problem_id` |
| `problems` | U | `function_wrapper_supported`, `updated_*` |
| Redis | DEL | `problem:spec:<problemId>` |

### 38.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB38-N1 | Normal | lưu chữ ký `twoSum(int[] nums, int target)` hợp lệ -> `functionWrapperSupported = true`, `specVersion` tăng 1, phát `ProblemSpecChanged` |
| PB38-N2 | Normal | gửi lại đúng spec cũ -> `200`, `specVersion` giữ nguyên, không phát sự kiện |
| PB38-A1 | Abnormal | `UNORDERED_SET` + kiểu trả về `BINARY_TREE` -> `422 PRB-036` với `HRN-V22`, không ghi gì |
| PB38-A2 | Abnormal | `EPSILON` thiếu epsilon -> `422 PRB-036` với `HRN-V20` |
| PB38-A3 | Abnormal | kiểu có `kind = "GRAPH"` -> `200`, `functionWrapperSupported = false`, một warning `HRN-V01` |
| PB38-A4 | Abnormal | hai tham số trùng tên -> `422 PRB-036` với `HRN-V14`; có `functionName` mà thiếu `parameters` -> `HRN-V17` |
| PB38-B1 | Boundary | chỉ đổi `stdinFormatMd` -> `specVersion` vẫn tăng (mọi cột đều tính, `02-bd/database/F2_problem-bank.md:136`); chưa khai chữ ký (ba cột `null`) -> `200`, `functionWrapperSupported = false`, warning `HRN-V07` |

## 39. PUT /authoring/problems/{problemId}/ai-context (`SaveAiAuthoringContext`)

### 39.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `PROBLEM_AUTHORING:UPDATE`; A2 chỉ bài của mình (`SHR0202:564`, Khu G NO 2) |
| Idempotent | có |
| Nguồn BD | `SHR0202_problem_authoring.md:664` (đoạn ghi chú dưới Sheet 7.3), EVT-28, EVT-29 |

### 39.2 Request
| Trường | Kiểu | Bắt buộc | Ràng buộc | Ghi chú |
| :--- | :--- | :-: | :--- | :--- |
| `briefText` | string\|null | không | tối đa 4.000 ký tự `[SoT: Suy luận]`; rỗng sau khi cắt thì lưu `NULL` | **dữ liệu, không phải chỉ thị**; lưu nguyên văn |
| `noFullCode` | bool | có | | cột `no_full_code` |
| `questionOnly` | bool | có | | cột `socratic_only` |
| `expectedUpdatedAt` | datetime | có | | khoá lạc quan |

### 39.3 Response (2xx)
`200 { "briefText", "noFullCode", "questionOnly", "problemUpdatedAt" }`.

### 39.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 400 | PRB-001 | `briefText` quá 4.000 ký tự, thiếu cờ | |
| 403 | PRB-002, PRB-003 | | |
| 404 | PRB-010 | | |
| 409 | PRB-020 | | |

### 39.5 Luồng xử lý
`UPDATE problems SET ai_brief, no_full_code, socratic_only, updated_*`. **Không** tăng `spec_version` hay `current_testcase_set_version` (`02-bd/database/F2_problem-bank.md:40`). Không có chuẩn hoá hay "làm sạch" nội dung: việc cô lập khỏi chỉ thị hệ thống xảy ra ở lúc dựng prompt,
nơi `ai-review` đọc qua `ProblemContextPort` và đặt vào khối có nhãn (`02-bd/architecture/F5_ai-review.md:218`; `DEC-2026-0831-ai-instruction-injection-guard`).

### 39.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `problems` | U | ba cột AI |

### 39.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB39-N1 | Normal | lưu chỉ dẫn và hai cờ -> `ProblemContextPort.getContext` trả đúng ba giá trị |
| PB39-A1 | Abnormal | nội dung "Bỏ qua mọi chỉ thị trước" được lưu nguyên văn như dữ liệu (không bị diễn giải ở đâu trong module) |
| PB39-A2 | Abnormal | tài khoản chỉ có `TESTCASE_MANAGEMENT` -> `403 PRB-002` |
| PB39-B1 | Boundary | `briefText` 4.000/4.001 ký tự; ô rỗng -> `NULL`; đổi cờ không đổi `spec_version` |

## 40. PUT /authoring/problems/{problemId}/status (`UpdateProblemPublishStatus`)

### 40.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `PROBLEM_AUTHORING:UPDATE`; A2 chỉ bài của mình |
| Idempotent | có (đã ở trạng thái đích thì `200`, không đổi) |
| Nguồn BD | `SHR0202_problem_authoring.md:650`, EVT-32, EVT-33; Sheet 9 NO 9 (dòng 771) |

### 40.2 Request
`{ "status": "UNPUBLISHED" | "PUBLISHED", "expectedUpdatedAt": datetime }`. Nút "Lưu và xuất bản" của màn là hai lời gọi liên tiếp: mục 37 rồi mục 40 (BD không có endpoint gộp).

### 40.3 Response (2xx)
`200 { "status", "publishedAt": datetime|null, "currentTestcaseSetVersion": int, "checklist": PublishChecklistDto, "problemUpdatedAt": datetime }`.

### 40.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 400 | PRB-001 | `status` sai giá trị | |
| 403 | PRB-002, PRB-003 | | |
| 404 | PRB-010 | | |
| 409 | PRB-020 | | |
| 422 | PRB-030 | chuyển sang `PUBLISHED` mà checklist chưa đủ 5 điều kiện | `details.items` liệt kê **tất cả** điều kiện thiếu (không chỉ điều kiện đầu, `SHR0202` Q8 dòng 793); bài giữ nguyên `UNPUBLISHED` |
| 503 | PRB-050 | sandbox không chạy được đáp án mẫu | không đổi trạng thái |

### 40.5 Luồng xử lý
- `-> PUBLISHED`: BR-07 (đếm, kiểm đặc tả, **chạy một lần** đáp án mẫu trên mọi testcase đã duyệt qua `SampleSolutionRunnerPort`, không đọc cờ lưu sẵn); tạo snapshot bất biến cho `current_testcase_set_version` nếu chưa có, xác minh object đã ghi trước khi BR-08 công bố bài. Xuất bản lại khi snapshot cùng phiên bản đã tồn tại thì dùng lại, không ghi đè.
- `-> UNPUBLISHED`: không điều kiện (BR-08).
Ghi nhật ký `PROBLEM_PUBLISHED`/`PROBLEM_UNPUBLISHED` cùng giao dịch, sau commit phát `ProblemPublished`/`ProblemUnpublished`, tăng phiên bản cache. Chỉ xuất bản/rút xuống ở endpoint này **không** phát `TestcaseSetVersionBumped`.

### 40.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `problems` | R, U | khoá lạc quan |
| `testcases`, `worked_examples`, `problem_specs`, `problem_reference_solutions` | R | |
| `identity.system_audit_logs` qua `AuditLogPort` | C | |
| MinIO, go-judge | R / chạy | |

### 40.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB40-N1 | Normal | bài đủ 8 testcase đã duyệt, 2 Sample, 2 ví dụ, đáp án mẫu pass hết, spec đủ -> `200`, `published_at` được đặt, phát `ProblemPublished` |
| PB40-N2 | Normal | rút xuống -> `200` không điều kiện, bài biến khỏi `problem_list`, dữ liệu lượt nộp còn nguyên |
| PB40-A1 | Abnormal | thiếu 3 điều kiện -> `422 PRB-030`, `details.items` có đủ 3 mục `met = false`, trạng thái vẫn `UNPUBLISHED` |
| PB40-A2 | Abnormal | testcase nháp AI chưa duyệt không được đếm vào ngưỡng 8 |
| PB40-A3 | Abnormal | đáp án mẫu fail một testcase -> `PRB-030` với `REFERENCE_SOLUTION_PASSES_ALL` `met = false` |
| PB40-B1 | Boundary | đúng 8 testcase đã duyệt, đúng 2 Sample, đúng 2 ví dụ -> đạt; 7/1/1 -> không đạt |

## 41. GET /authoring/problems/{problemId}/stats (`GetProblemStats`)

### 41.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `PROBLEM_AUTHORING:READ`; truy cập như mục 34 |
| Idempotent | có |
| Nguồn BD | `SHR0202_problem_authoring.md:660`, DTO NO 18 (dòng 619) |

### 41.2 Request
`problemId` (uuid) ở đường dẫn.

### 41.3 Response (2xx)
`{ "submissionCount": int, "acRate": number|null, "updatedAt": datetime|null }`. Bài chưa có dòng `problem_stats` -> `{ 0, null, null }`. Không có "thời gian giải trung bình", "lượt xin gợi ý" (chưa có nguồn, mục 1.1).

### 41.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 403 | PRB-002 | | |
| 404 | PRB-010 | bài không tồn tại / `deleted` / bản nháp người khác với A2 | |

### 41.5 Luồng xử lý
Kiểm truy cập như mục 34.5; `SELECT` một dòng `problem_stats` theo `problem_id`.

### 41.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `problem_stats` | R | |
| `problems` | R | kiểm truy cập |

### 41.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB41-N1 | Normal | bài có 100 lượt nộp, 40 Accepted -> `acRate = 40.00` |
| PB41-A1 | Abnormal | A2 mở bản nháp người khác -> `404 PRB-010` |
| PB41-B1 | Boundary | bài mới chưa ai nộp -> `submissionCount = 0`, `acRate = null` |

## 42. POST /authoring/problems/{problemId}/testcases (`CreateTestcase`)

### 42.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `TESTCASE_MANAGEMENT:CREATE`; A2 chỉ bài của mình (bản nháp người khác -> `404 PRB-010`) |
| Idempotent | không |
| Nguồn BD | `SHR0202_problem_authoring.md:651`, EVT-19 (dòng 702) |

### 42.2 Request
| Trường | Kiểu | Bắt buộc | Ràng buộc | Ghi chú |
| :--- | :--- | :-: | :--- | :--- |
| `visibility` | enum | không | `SAMPLE`/`HIDDEN`; mặc định `HIDDEN` | `SHR0202:399` (Sheet 5 Khu E NO 11) |
| `input` | string | có | không rỗng; tối đa 1 MB UTF-8 `[SoT: Suy luận]` | lớn hơn thì dùng mục 46 |
| `expectedOutput` | string | có | không rỗng; tối đa 1 MB | |
| `category` | enum\|null | không | bảy giá trị `TestcaseCategory` hoặc `null` | |

### 42.3 Response (2xx)
`201 { "testcase": TestcaseAuthorDto, "currentTestcaseSetVersion": int, "problemUpdatedAt": datetime }`. Testcase tay: `origin = MANUAL`, `approved = true`, `position = max(position) + 1` trong cùng bài.

### 42.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 400 | PRB-001 | `input`/`expectedOutput` rỗng, `visibility`/`category` sai | |
| 403 | PRB-002, PRB-003 | | |
| 404 | PRB-010 | | |
| 422 | PRB-046 | bài đã có 500 testcase | trần `[SoT: Suy luận]`, `02-bd/security/F2_problem-bank.md:115-117` giao DD chốt |
| 422 | PRB-048 | một vế vượt 1 MB | "hãy dùng tải lên hàng loạt" |
| 503 | PRB-052 | MinIO không đáp ứng khi cần ghi object | giao dịch rollback |

### 42.5 Luồng xử lý
BR-12: với mỗi vế, nội dung `<= 8 KB` (8.192 byte UTF-8) lưu `INLINE`, lớn hơn lưu `MINIO` tại `problems/<problemId>/testcases/<testcaseId>/input.txt` hoặc `expected_output.txt` qua `TestcaseObjectStorePort` (`02-bd/storage/F2_problem-bank.md:14,28-32`). Hai vế quyết định độc lập.
Bài đang `PUBLISHED` và testcase mới đã duyệt (Sample hoặc Hidden) thì tăng `current_testcase_set_version`, tạo snapshot bộ chấm mới (BR-11) và phát `TestcaseSetVersionBumped` sau commit.

### 42.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `testcases` | C | hai CHECK lưu trữ (`02-bd/database/F2_problem-bank.md:166`) |
| `problems` | R, U | `current_testcase_set_version`, `updated_*` |
| MinIO | put | chỉ vế lớn |

### 42.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB42-N1 | Normal | thêm testcase `HIDDEN` nhỏ vào bài `UNPUBLISHED` -> `INLINE`, `approved = true`, không tăng phiên bản |
| PB42-N2 | Normal | thêm vào bài `PUBLISHED` -> `current_testcase_set_version` tăng 1, phát `TestcaseSetVersionBumped` |
| PB42-A1 | Abnormal | `input` rỗng -> `400 PRB-001` |
| PB42-A2 | Abnormal | A2 thêm vào bài người khác -> `403 PRB-003` (bài đã xuất bản) hoặc `404 PRB-010` (bản nháp) |
| PB42-B1 | Boundary | input đúng 8.192 byte -> `INLINE`; 8.193 byte -> `MINIO`; input nhỏ + expected lớn -> hai vế khác kiểu lưu |
| PB42-B2 | Boundary | thêm `SAMPLE` đã duyệt vào bài `PUBLISHED` -> tăng một phiên bản và tạo snapshot mới |

## 43. PATCH /authoring/problems/{problemId}/testcases/{testcaseId} (`UpdateTestcase`)

### 43.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `TESTCASE_MANAGEMENT:UPDATE`; A2 chỉ bài của mình |
| Idempotent | có |
| Nguồn BD | `SHR0202_problem_authoring.md:652`, EVT-17, EVT-20, EVT-21 (dòng 700, 703, 704) |

### 43.2 Request
Mọi trường tuỳ chọn (cập nhật từng phần): `visibility`, `input`, `expectedOutput`, `category`, `approved`.
`approved` chỉ nhận `true` (xác nhận nháp AI); gửi `false` cho testcase đã duyệt -> `400 PRB-001` (loại nháp là xoá, mục 44, không phải hạ cờ) `[SoT: Suy luận]`. Ràng buộc nội dung như mục 42.2.

### 43.3 Response (2xx)
`200 { "testcase": TestcaseAuthorDto, "currentTestcaseSetVersion": int, "problemUpdatedAt": datetime }`.

### 43.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 400 | PRB-001 | giá trị sai, `approved = false` trên testcase đã duyệt | |
| 403 | PRB-002, PRB-003 | | |
| 404 | PRB-010, PRB-011 | bài / testcase không tồn tại hoặc không thuộc bài | |
| 422 | PRB-048 | nội dung mới vượt 1 MB | |
| 422 | PRB-054 | đổi Sample sang Hidden làm bài `PUBLISHED` còn dưới hai Sample đã duyệt | |

### 43.5 Luồng xử lý
BR-12, BR-17: khoá dòng bài và kiểm lại ngưỡng checklist trong cùng giao dịch; với bài `PUBLISHED`, đổi Sample đã duyệt sang Hidden làm số Sample dưới hai thì từ chối `PRB-054`. Sau đó áp dụng thay đổi, đặt lại kiểu lưu từng vế nếu kích thước vượt ngưỡng 8 KB (chuyển `INLINE` <-> `MINIO`, xoá object cũ sau commit). Nháp AI (`origin = AI`) đặt `approved = true` thì `origin` giữ `AI`.
**Tăng `current_testcase_set_version`** (bài `PUBLISHED`) khi bất kỳ testcase **đã duyệt** nào trong bộ chấm đổi: duyệt, thêm, xoá, sửa `input`/`expectedOutput`/`visibility`, hoặc đổi thứ tự chạy; áp dụng cho cả `SAMPLE` và `HIDDEN`. Tạo bundle snapshot mới trước khi công bố phiên bản (BR-11, BD database F2 mục 1.6, 1.15). Đổi `category` hay chỉ sửa nháp chưa duyệt thì không tăng.
Điều kiện tăng ở BD DB là "bài đã xuất bản", còn `SHR0202` Sheet 6 Khu E NO 2 thêm "và đã có submission"; DD theo DB vì module này không biết bài có lượt nộp hay chưa `[SoT: Suy luận, Q-04 mục 61]`.

### 43.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `testcases` | R, U | |
| `problems` | R, U | |
| MinIO | put / delete | khi đổi kiểu lưu |

### 43.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB43-N1 | Normal | duyệt nháp AI `HIDDEN` của bài `PUBLISHED` -> `approved = true`, `origin = AI`, phiên bản tăng 1 |
| PB43-N2 | Normal | đổi `category` -> không tăng phiên bản |
| PB43-A1 | Abnormal | `approved = false` trên testcase đã duyệt -> `400 PRB-001` |
| PB43-A2 | Abnormal | `testcaseId` thuộc bài khác -> `404 PRB-011` |
| PB43-B1 | Boundary | sửa nội dung testcase `SAMPLE` đã duyệt của bài `PUBLISHED` -> tăng một phiên bản và tạo snapshot mới; đổi `SAMPLE` đã duyệt sang `HIDDEN` mà vẫn đủ ngưỡng -> tăng một phiên bản |
| PB43-B2 | Boundary | sửa input từ 8 KB lên 9 KB -> chuyển `INLINE` sang `MINIO`, object cũ (nếu có) được dọn |
| PB43-B3 | Boundary | bài PUBLISHED còn đúng hai Sample, đổi một Sample sang Hidden -> `PRB-054`, không tăng phiên bản |

## 44. DELETE /authoring/problems/{problemId}/testcases/{testcaseId} (`DeleteTestcase`)

### 44.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `TESTCASE_MANAGEMENT:DELETE`; A2 chỉ bài của mình |
| Idempotent | không (lần hai trả `404 PRB-011`) |
| Nguồn BD | `SHR0202_problem_authoring.md:653`, EVT-23; 7.2 NO 4 (dòng 629) |

### 44.2 Request
Tham số đường dẫn `problemId`, `testcaseId`.

### 44.3 Response (2xx)
`200 { "currentTestcaseSetVersion": int, "problemUpdatedAt": datetime }`.

### 44.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 403 | PRB-002, PRB-003 | | |
| 404 | PRB-010, PRB-011 | | |
| 422 | PRB-054 | bài `PUBLISHED` sẽ còn dưới 8 testcase đã duyệt hoặc 2 Sample đã duyệt | |

### 44.5 Luồng xử lý
Khoá dòng bài và đếm testcase đã duyệt trong cùng giao dịch. Với bài `PUBLISHED`, nếu xoá sẽ làm `MIN_APPROVED_TESTCASES < 8` hoặc `MIN_APPROVED_SAMPLES < 2`, từ chối `422 PRB-054` trước khi xoá. Sau khi qua kiểm, `DELETE FROM testcases` (xoá thật dòng; **không** có cột ẩn mềm ở bảng này). Object MinIO của testcase xoá best-effort sau commit, lỗi chỉ ghi log. Bài `PUBLISHED` và testcase đã duyệt, gồm `SAMPLE` hoặc `HIDDEN`, thì tăng phiên bản và tạo snapshot mới (BR-11).
`judge.submission_testcase_results` giữ `testcase_id` cũ (không có khoá ngoại chéo schema). Ngưỡng trên là bất biến sau xuất bản theo quyết định owner 2026-10-25 (`Q-06` mục 61); muốn giảm sâu hơn phải rút bài xuống trước.
Lệch giữa câu "thực thể gốc không bao giờ xoá cứng" ở `02-bd/database/F2_problem-bank.md:6` (liệt kê `testcases`) và `SHR0202` 7.2 NO 4 (có thao tác `D`): DD theo 7.2 vì bảng không có cờ xoá mềm (`Q-07` mục 61).

### 44.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `testcases` | D | |
| `problems` | U | |
| MinIO | delete | best-effort sau commit |

### 44.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB44-N1 | Normal | xoá testcase `HIDDEN` đã duyệt của bài `PUBLISHED` -> phiên bản tăng 1, dòng biến mất |
| PB44-N2 | Normal | xoá nháp AI chưa duyệt -> không tăng phiên bản |
| PB44-A1 | Abnormal | xoá hai lần -> lần hai `404 PRB-011` |
| PB44-A2 | Boundary | bài PUBLISHED còn đúng 8 ca duyệt hoặc 2 Sample: xoá một ca tương ứng trả `422 PRB-054`, dữ liệu/phiên bản không đổi |
| PB44-B1 | Boundary | xoá testcase `MINIO` -> object bị dọn; MinIO lỗi lúc dọn vẫn trả `200` và ghi log |

## 45. PUT /authoring/problems/{problemId}/testcases/order (`ReorderTestcases`)

### 45.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `TESTCASE_MANAGEMENT:UPDATE`; A2 chỉ bài của mình |
| Idempotent | có |
| Nguồn BD | `SHR0202_problem_authoring.md:654`, EVT-24 |

### 45.2 Request
`{ "orderedIds": [uuid] }` — toàn bộ id testcase của bài theo thứ tự mới dùng chung cho màn biên soạn và thứ tự chấm.

### 45.3 Response (2xx)
`200 { "items": [ { "id": uuid, "position": int } ], "problemUpdatedAt": datetime, "currentTestcaseSetVersion": int }`.

### 45.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 403 | PRB-002, PRB-003 | | |
| 404 | PRB-010 | | |
| 422 | PRB-049 | tập id không bằng tập testcase của bài | giao diện tải lại |
| 503 | PRB-052 | không ghi được bundle snapshot khi đổi thứ tự trên bài `PUBLISHED` | rollback toàn bộ thao tác |

### 45.5 Luồng xử lý
Khoá dòng bài và các dòng testcase, so tập id rồi ghi `position = 1..n` theo thứ tự gửi (không unique `(problem_id, position)` nên không vướng ràng buộc tạm thời). `position` là thứ tự dùng chung cho màn biên soạn và bộ chấm: F4 đánh số `testcase_order` theo `position` trong snapshot và chọn lỗi đầu tiên theo thứ tự đó. Nếu thứ tự thực sự đổi trên bài `PUBLISHED`, áp dụng BR-11 trong cùng giao dịch: tạo bundle bất biến chứa `position` mới, tăng `current_testcase_set_version` đúng một lần, ghi snapshot mới; sau commit phát một `TestcaseSetVersionBumped`. Submission đã ghim phiên bản cũ vẫn dùng thứ tự cũ; submission tạo sau commit ghim phiên bản mới. Với bài `UNPUBLISHED`, chỉ ghi thứ tự hiện hành, không tạo snapshot/không tăng phiên bản. Gửi lại đúng thứ tự hiện hành là no-op, không tạo snapshot, không tăng phiên bản hoặc phát sự kiện. `422 PRB-049` không thay đổi thứ tự hay phiên bản.

### 45.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `testcases` | R, U | chỉ mục `testcases(problem_id, position)` |
| `problems` | R, U | khoá bài; `updated_*`, `current_testcase_set_version` khi áp dụng BR-11 |
| `testcase_set_snapshots`, MinIO, Outbox | C | bài `PUBLISHED` và thứ tự thực sự đổi: snapshot mới, bundle mới, sự kiện sau commit theo BR-11 |

### 45.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB45-N1 | Normal | kéo testcase đã duyệt lên đầu trên bài `PUBLISHED` -> `position` đánh số lại 1..n, phiên bản tăng 1, snapshot mới phản ánh thứ tự mới; submission cũ giữ snapshot cũ |
| PB45-N2 | Normal | đổi thứ tự trên bài `UNPUBLISHED` -> lưu thứ tự hiện hành, không tăng phiên bản |
| PB45-A1 | Abnormal | thiếu một id -> `422 PRB-049`, thứ tự/phiên bản không đổi |
| PB45-B1 | Boundary | bài chỉ có 1 testcase -> `200`; bài có 0 testcase gửi danh sách rỗng -> `200`; không đổi thứ tự nên không tăng phiên bản |
| PB45-B2 | Boundary | gửi lại đúng thứ tự hiện hành -> `200`, không tạo snapshot/phiên bản/sự kiện mới |
| PB45-A2 | Recovery | ghi bundle snapshot lỗi khi sắp bài `PUBLISHED` -> `503 PRB-052`, vị trí/phiên bản/snapshot/Outbox giữ nguyên |
| PB45-S1 | Security | A2 đổi thứ tự bài không sở hữu -> `403 PRB-003`, không đổi dữ liệu |

## 46. POST /authoring/problems/{problemId}/testcases/bulk-upload-url (`RequestTestcaseBulkUploadUrl`)

### 46.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `TESTCASE_MANAGEMENT:CREATE`; A2 chỉ bài của mình |
| Idempotent | không (mỗi lần cấp một `uploadId` mới) |
| Nguồn BD | `SHR0202_problem_authoring.md:655`, EVT-15; `02-bd/storage/F2_problem-bank.md:47` |

### 46.2 Request
`{ "items": [ { "clientKey": string, "inputSizeBytes": long, "expectedOutputSizeBytes": long, "visibility": "SAMPLE"|"HIDDEN", "category"?: enum } ] }`.
Giao diện ghép cặp file đầu vào/đầu ra ở trình duyệt; máy chủ **không** định nghĩa định dạng gói nén (`[SoT: Suy luận]`, `Q-08` mục 61). Giới hạn (suy luận, `02-bd/security/F2_problem-bank.md:115-117` giao DD chốt):
mỗi yêu cầu tối đa 100 cặp; mỗi vế tối đa 64 MB; tổng một yêu cầu tối đa 256 MB; tổng testcase của bài sau khi thêm tối đa 500. `clientKey` duy nhất trong yêu cầu (chỉ để giao diện khớp kết quả, tối đa 100 ký tự).

### 46.3 Response (2xx)
`200 { "uploadId": uuid, "expiresAt": datetime, "items": [ { "clientKey", "testcaseId": uuid, "input": { "putUrl": string }, "expectedOutput": { "putUrl": string } } ] }`.
`putUrl` là presigned PUT hiệu lực **15 phút**, trỏ tới khoá do **máy chủ** sinh từ `testcaseId` (`problems/<problemId>/testcases/<testcaseId>/input.txt` và `expected_output.txt`); client không chọn được khoá. Phiên chờ lưu Redis `problem:upload:<uploadId>` TTL 20 phút
(không thêm bảng, ponytail bậc 4).

### 46.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 400 | PRB-001 | `items` rỗng, `clientKey` trùng, kích thước không dương, `visibility` sai | |
| 403 | PRB-002, PRB-003 | | |
| 404 | PRB-010 | | |
| 422 | PRB-046 | tổng testcase của bài sẽ vượt 500 | |
| 422 | PRB-047 | quá 100 cặp, vế quá 64 MB, hoặc tổng quá 256 MB | |
| 503 | PRB-052 | MinIO không ký được URL | |

### 46.5 Luồng xử lý
BR-14 bước 1: kiểm giới hạn, sinh `testcaseId` (UUID) cho từng cặp, ký hai presigned PUT, lưu phiên `{ problemId, userId, items[] }` vào Redis. Chưa ghi gì vào `testcases`.

### 46.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `testcases` | R (đếm) | |
| Redis | SET | `problem:upload:<uploadId>` |
| MinIO | presign PUT | |

### 46.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB46-N1 | Normal | 3 cặp -> 3 `testcaseId`, 6 `putUrl`, `expiresAt` sau 15 phút |
| PB46-A1 | Abnormal | 101 cặp -> `422 PRB-047`; vế 65 MB -> `422 PRB-047` |
| PB46-A2 | Abnormal | A2 xin URL cho bài người khác -> `403`/`404` theo quy tắc mục 1.3; URL không bao giờ trỏ ra ngoài `problems/<problemId>/` |
| PB46-B1 | Boundary | bài đang có 499 testcase xin thêm 2 cặp -> `422 PRB-046`; thêm 1 cặp -> thành công |

## 47. POST /authoring/problems/{problemId}/testcases/bulk-upload/confirm (`ConfirmTestcaseBulkUpload`)

### 47.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `TESTCASE_MANAGEMENT:CREATE`; A2 chỉ bài của mình |
| Idempotent | **có**: gọi lại với cùng `uploadId` trả đúng kết quả lần trước (phiên lưu trạng thái `CONFIRMED` trong 20 phút) |
| Nguồn BD | `SHR0202_problem_authoring.md:656`; `02-bd/storage/F2_problem-bank.md:47`, `:66` (xác nhận qua HEAD hay callback giao DD) |

### 47.2 Request
`{ "uploadId": uuid }`.
**Cơ chế xác nhận chốt tại DD**: máy chủ chủ động `HEAD` từng object (polling một lần lúc xác nhận), không dùng callback sự kiện MinIO `[SoT: Suy luận — ponytail bậc 4, đóng một phần storage mục 5]`.

### 47.3 Response (2xx)
`201 { "added": [ TestcaseAuthorDto ], "currentTestcaseSetVersion": int, "problemUpdatedAt": datetime }`.

### 47.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 403 | PRB-002, PRB-003 | | |
| 404 | PRB-010 | | |
| 422 | PRB-044 | phiên không tồn tại, hết hạn, không thuộc bài này, hoặc không thuộc người gọi | |
| 422 | PRB-045 | thiếu object, kích thước khác đã khai, hoặc nội dung vế nhỏ không phải UTF-8 hợp lệ | `details.items = [ { clientKey, side, problem: "MISSING"\|"SIZE_MISMATCH"\|"INVALID_TEXT" } ]`; **không thêm testcase nào** ("không thêm testcase một phần", `SHR0202:698`); phiên còn hiệu lực để tải lại rồi xác nhận lại |
| 503 | PRB-052 | MinIO không đáp ứng | |

### 47.5 Luồng xử lý
BR-14 bước 2: đọc phiên Redis; `HEAD` mọi object; một lỗi là dừng toàn bộ. Thành công: một giao dịch `INSERT` mọi testcase (`origin = MANUAL`, `approved = true`, `position = max + 1, ...`); vế khai `<= 8 KB` thì **đọc object vào cột inline** rồi xoá object sau commit (giữ đúng ranh giới 8 KB,
`02-bd/storage/F2_problem-bank.md:14`), vế lớn giữ `object_key`. Bài `PUBLISHED` và lô thêm ít nhất một testcase đã duyệt (Sample hoặc Hidden) thì tăng phiên bản **đúng một lần**, tạo snapshot mới và phát một `TestcaseSetVersionBumped`. Đánh dấu phiên `CONFIRMED` kèm danh sách id.

### 47.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `testcases` | C | |
| `problems` | R, U | |
| Redis | GET, SET | phiên tải lên |
| MinIO | head, get, delete | |

### 47.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB47-N1 | Normal | tải đủ 3 cặp rồi xác nhận -> 3 testcase mới ở cuối bảng, vế nhỏ thành `INLINE`, vế lớn giữ `MINIO` |
| PB47-N2 | Normal | gọi xác nhận lần hai cùng `uploadId` -> `201` cùng danh sách, không nhân đôi testcase |
| PB47-A1 | Abnormal | thiếu một object -> `422 PRB-045`, không testcase nào được thêm |
| PB47-A2 | Abnormal | `uploadId` của người khác hoặc đã hết hạn -> `422 PRB-044` |
| PB47-B1 | Boundary | lô gồm cả `SAMPLE` và `HIDDEN` vào bài `PUBLISHED` -> phiên bản tăng đúng 1 |

## 48. POST /authoring/problems/{problemId}/reference-solution/run (`RunSampleSolutionAgainstTestcases`)

### 48.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `TESTCASE_MANAGEMENT:READ`; A2 chỉ bài của mình |
| Idempotent | có (không đổi dữ liệu; kết quả tạm thời, không lưu) |
| Nguồn BD | `SHR0202_problem_authoring.md:657`, EVT-18, EVT-22; F2-18 (`01-rd/req/F2_problem-bank.md:126-135`) |

### 48.2 Request
| Trường | Kiểu | Bắt buộc | Ràng buộc | Ghi chú |
| :--- | :--- | :-: | :--- | :--- |
| `testcaseId` | uuid | không | thuộc bài | có thì chỉ chạy testcase đó ("Chạy thử" một dòng); không có thì chạy **mọi testcase đã duyệt** của bài |
| `language`, `sourceCode` | enum, string | không (cùng có hoặc cùng thiếu) | `sourceCode` tối đa 64 KB | ghi đè mã chưa lưu trong ô soạn; thiếu thì dùng dòng `problem_reference_solutions` đã lưu |

Đáp án mẫu là **một chương trình đọc `stdin` ghi `stdout`** (chạy với định dạng input chuẩn hoá của testcase), không phải hàm bọc `[SoT: Suy luận — RD F2-18 nói "chương trình"; Q-10 mục 61]`.

### 48.3 Response (2xx)
`200 { "compileError": string|null, "summary": { "passed": int, "total": int, "allPass": bool }, "results": [ { "testcaseId", "position", "visibility", "verdict": "PASS"|"FAIL",
"failure": "WRONG_ANSWER"|"TIME_LIMIT_EXCEEDED"|"MEMORY_LIMIT_EXCEEDED"|"RUNTIME_ERROR"|"OUTPUT_LIMIT_EXCEEDED"|"COMPILE_ERROR"|null, "runtimeMs": int|null, "memoryKb": int|null } ], "ranAt": datetime }`.
Không trả `actualOutput` hay diff, kể cả testcase `HIDDEN` (quy tắc 1.4; người soạn xem nội dung testcase ở mục 34). `allPass = (total > 0 AND passed = total)`.

### 48.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 400 | PRB-001 | chỉ có `language` hoặc chỉ có `sourceCode`; `sourceCode` quá 64 KB | |
| 403 | PRB-002, PRB-003 | | |
| 404 | PRB-010, PRB-011 | | |
| 422 | PRB-040 | chưa có đáp án mẫu và yêu cầu không kèm mã | |
| 429 | PRB-055 | người gọi đã chạy đáp án mẫu 10 lần trong 60 giây trượt | `details.retryAfterSeconds`; chỉ §48, không trừ trần sinh AI §49 |
| 503 | PRB-050 | go-judge không đáp ứng / hết thời gian chờ | toast lỗi hệ thống, giữ nguyên bảng |

### 48.5 Luồng xử lý
BR-15: sau kiểm quyền/bài/đầu vào, Redis sorted set `problem:reference-run:rate:<userId>` giữ tối đa 10 lượt được nhận trong 60 giây trượt (Lua nguyên tử); vượt trần trả `429 PRB-055`. Redis lỗi ghi WARN và tiếp tục; lỗi hạ tầng `503 PRB-050` hoàn lại mốc đã giữ. Sau đó chọn tập testcase; đọc nội dung (kể cả `MINIO`) qua `TestcaseObjectStorePort`; giới hạn hiệu dụng = giới hạn của bài nhân hệ số của ngôn ngữ (`JudgeConfigReadPort.getLanguageMultipliers`); biên dịch **một lần**, chạy từng testcase qua `SampleSolutionRunnerPort.runProgramBatch`;
so khớp output bằng `ReferenceOutputMatcherPort` theo `matching_strategy` của bài (không có dòng `problem_specs` thì dùng `EXACT` `[SoT: Suy luận]`). Thời gian chờ tổng 120 giây, không thử lại tự động. Không ghi gì vào DB.

### 48.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `problem_reference_solutions`, `testcases`, `problem_specs`, `problems` | R | |
| MinIO | get | |
| go-judge qua `SampleSolutionRunnerPort`; `judge` qua `JudgeConfigReadPort` | chạy / R | |

### 48.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB48-N1 | Normal | đáp án đúng, 8 testcase đã duyệt -> `allPass = true`, `passed = 8` |
| PB48-N2 | Normal | gửi `testcaseId` -> `results` có đúng 1 phần tử, kể cả testcase nháp chưa duyệt |
| PB48-A1 | Abnormal | đáp án sai ở testcase #3 -> `FAIL` kèm `WRONG_ANSWER` ở dòng đó, các dòng khác không bị ảnh hưởng; không có `actualOutput` trong phản hồi |
| PB48-A2 | Abnormal | lỗi biên dịch -> `compileError` có nội dung, mọi dòng `COMPILE_ERROR` |
| PB48-A3 | Abnormal | go-judge tắt -> `503 PRB-050`, không dữ liệu nào đổi |
| PB48-B1 | Boundary | bài chưa có testcase đã duyệt -> `total = 0`, `allPass = false`; chưa có đáp án mẫu và không kèm mã -> `422 PRB-040` |
| PB48-B2 | Boundary | Cùng người chạy 10 lượt trong 60 giây: lượt 11 nhận `PRB-055`; lỗi go-judge `PRB-050` không giữ mốc |

## 49. POST /authoring/problems/{problemId}/testcases/generate (`GenerateTestcasesWithAi`)

### 49.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `TESTCASE_MANAGEMENT:CREATE` (`02-bd/security/F2_problem-bank.md:11-14`); A2 chỉ bài của mình |
| Idempotent | không (mỗi lần là một script AI mới) |
| Phản hồi | đồng bộ, thời gian chờ tối đa 5 phút `[SoT: Suy luận — ponytail bậc 5, nâng lên hàng đợi khi cần]` |
| Nguồn BD | `SHR0202_problem_authoring.md:658`, EVT-16 (dòng 699); F2-14 (`01-rd/req/F2_problem-bank.md:71-125`); `DEC-2026-0928-f2-14-generator-script`; `DEC-2026-1003-ai-testcase-generation-quota` |

### 49.2 Request
Thân rỗng (`{}`): luôn sinh trọn bộ, không chọn loại ca hay số lượng (`SHR0202` Q17, dòng 802).

### 49.3 Response (2xx)
`200 { "added": int, "testcases": [ TestcaseAuthorDto ], "dropped": [ { "reason": "INVALID_CONSTRAINT"|"REFERENCE_FAILED"|"TIMEOUT"|"DUPLICATE_INPUT", "count": int } ],
"warnings": [ { "code": "LARGEST_CASE_TIMEOUT", "message": string } ], "aiGeneration": { "used": int, "limit": int }, "problemUpdatedAt": datetime }`.
Testcase mới ở trạng thái nháp: `origin = AI`, `approved = false`, `visibility = HIDDEN`, `category` lấy từ nhãn loại ca của script. `dropped[].reason` khớp ba lý do của prototype cộng `DUPLICATE_INPUT`
(`05-coding/frontend/src/entities/problem/model/draft-types.ts` `DroppedReason`; tên enum DTO là đề xuất). Cảnh báo `LARGEST_CASE_TIMEOUT`: ca lớn nhất làm đáp án mẫu quá giờ hoặc lỗi, giới hạn thời gian (F2-10) có thể đặt sai hoặc đáp án mẫu quá chậm (RD bước 7, `01-rd/req/F2_problem-bank.md:100-103`).

### 49.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 403 | PRB-002, PRB-003 | | |
| 404 | PRB-010 | | |
| 409 | PRB-023 | đang có một lượt sinh khác của cùng bài | khoá Redis `problem:gen:lock:<problemId>` TTL 10 phút |
| 422 | PRB-040 | chưa có đáp án mẫu | |
| 422 | PRB-041 | thiếu điều kiện tiên quyết | `details.reasons`: `NO_CONSTRAINTS` (chưa khai ràng buộc dữ liệu), `NOT_ENOUGH_MANUAL_SAMPLES` (dưới 2 Sample tự viết đã duyệt), `REFERENCE_NOT_PASSING` (đáp án mẫu không pass mọi testcase đã duyệt); thông báo "Cần chạy Đáp án mẫu Pass toàn bộ testcase trước khi sinh tự động" (`SHR0202:770`) |
| 422 | PRB-042 | hết lượt: `ai_testcase_generations_used >= max_ai_generations_per_problem` | `details = { used, limit }` |
| 422 | PRB-043 | script bị loại: vượt thời gian, vượt kích thước output, lỗi chạy hay output sai định dạng | `details.reason`: `TIMEOUT`, `OUTPUT_TOO_LARGE`, `SCRIPT_ERROR`, `INVALID_FORMAT`; **không** thêm testcase nào, **không** tính lượt |
| 503 | PRB-050 | sandbox không đáp ứng | không tính lượt |
| 503 | PRB-051 | đường AI không đáp ứng hoặc hết hạn mức | **không ảnh hưởng** chức năng nào khác của F1-F4 (`CLAUDE.md`, Rules); không tính lượt |

### 49.5 Luồng xử lý
BR-16 (pipeline đầy đủ): kiểm quyền, điều kiện tiên quyết và lượt; khoá chống chạy song song; chạy đáp án mẫu trên mọi testcase đã duyệt; gọi `TestcaseScriptGenerationPort` với đầu vào **chỉ gồm** đề bài, ràng buộc dữ liệu và đúng 2 Sample tự viết (tham số dữ liệu, **không có trường nào cho mã đáp án mẫu**);
chạy script sinh trong sandbox (chặn mạng, giới hạn thời gian và kích thước output lấy từ `JudgeConfigReadPort`, hạt giống cố định); kiểm từng input bằng script kiểm ràng buộc; chạy input hợp lệ qua đáp án mẫu để lấy output thật; lưu nháp, script và hạt giống; tăng bộ đếm đúng khi thêm được ít nhất 1 nháp.

### 49.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `problems` | R, U | `ai_testcase_generations_used` tăng bằng một câu lệnh có điều kiện `used < max` |
| `problem_bank_settings` | R | trần |
| `testcases` | C | nháp AI |
| `problem_reference_solutions`, `problem_specs` | R | |
| `testcase_generation_runs` | C | BD database F2 mục 1.13: lưu script và hạt giống làm xuất xứ (RD bước 8) |
| Redis | SET/DEL | khoá chống song song |
| MinIO | put | testcase lớn |

### 49.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB49-N1 | Normal | đủ điều kiện, script hợp lệ -> `added > 0`, mọi testcase mới `origin = AI`, `approved = false`, `HIDDEN`; `aiGeneration.used` tăng 1 |
| PB49-N2 | Normal | nháp chưa duyệt không được đếm vào checklist xuất bản (mục 40) và không có trong `ProblemGradingSpecPort.loadGradingSpec` |
| PB49-A1 | Abnormal | dưới 2 Sample tự viết -> `422 PRB-041` `NOT_ENOUGH_MANUAL_SAMPLES`, không gọi AI |
| PB49-A2 | Abnormal | script in quá `max_output_mb` -> `422 PRB-043` `OUTPUT_TOO_LARGE`, không testcase mới, `used` giữ nguyên |
| PB49-A3 | Abnormal | input sai ràng buộc bị loại (`INVALID_CONSTRAINT`), input làm đáp án mẫu lỗi bị loại (`REFERENCE_FAILED`), testcase hợp lệ còn lại vẫn được thêm |
| PB49-A4 | Abnormal | AI không đáp ứng -> `503 PRB-051`; chấm bài và các endpoint khác vẫn chạy bình thường |
| PB49-A5 | Abnormal | prompt gửi cho AI **không chứa** mã đáp án mẫu, `ai_brief` hay nội dung testcase ẩn (kiểm tại cổng) |
| PB49-B1 | Boundary | `used = limit - 1` -> chạy được, sau đó `used = limit`; lần kế `422 PRB-042`; hai lần bấm đồng thời -> lần hai `409 PRB-023` |
| PB49-B2 | Boundary | lượt chỉ thêm 0 testcase (mọi input bị loại) -> không tính lượt |

## 50. GET /instructor/class-assignments (`ListClassAssignments`)

### 50.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `CLASS_MANAGEMENT:READ`; chỉ lớp người gọi phụ trách |
| Idempotent | có |
| Phân trang | có (`pageSize` 20) |
| Nguồn BD | `INS0202_class_assignments.md:473`; cùng use case phục vụ `identity` qua cổng `ClassAssignmentReadPort` (mục 58, `INS0201`/`INS0203`/`INS0204`) |

### 50.2 Request (query)
| Trường | Kiểu | Bắt buộc | Ràng buộc | Ghi chú |
| :--- | :--- | :-: | :--- | :--- |
| `classId` | uuid | không | phải là lớp người gọi phụ trách | thiếu = tab "Tất cả" (mọi lớp phụ trách) |
| `q` | string | không | cắt còn 100 ký tự (`INS0202:516`) | khớp `title` |
| `page`, `pageSize` | int | không | `pageSize` cố định 20 | BD gọi `size` |

### 50.3 Response (2xx)
`{ "items": [ { "problemId", "title", "topicNames": [string], "levelCode", "levelDisplayName", "assignedClasses": [ { "classId", "className" } ], "submissionCount": int|null,
"acRatePercent": number|null, "withdrawn": bool } ], "page": PageMeta }`. Một dòng mỗi bài; `assignedClasses` là các lớp (trong phạm vi lọc) đang giao bài. `submissionCount`/`acRatePercent` tính **trong lớp đang lọc** (tab một lớp) hoặc gộp mọi lớp phụ trách (tab "Tất cả"),
chỉ tính học viên còn trong lớp (`left_at IS NULL`); chưa có lượt nộp thì `acRatePercent = null`; cổng thống kê lỗi thì cả hai `null`. `withdrawn = status = UNPUBLISHED OR deleted`.
Tên `withdrawn` theo bản dựng (`AssignedProblem.withdrawn`, `USR0103_saved_problems.md:479`); BD `INS0202` 7.1 NO 14 gọi `problemStatus` kiểu Enum `[SoT: Suy luận, Q-11 mục 61]`.

### 50.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 403 | PRB-002 | thiếu `CLASS_MANAGEMENT:READ` | |
| 403 | PRB-005 | `classId` không thuộc lớp người gọi phụ trách | "Bạn không phụ trách lớp này." |

### 50.5 Luồng xử lý
1. `InstructorClassPort.listClassesOfInstructor(người gọi)` lấy tập lớp phụ trách (kèm tên lớp). `classId` có thì phải thuộc tập đó.
2. Đọc `class_assignments WHERE class_id IN (...) AND removed_at IS NULL`, nhóm theo `problem_id`, nối `problems` (**không** lọc `status`/`deleted`: bài đã rút vẫn hiện kèm cờ), `topics`, `problem_levels`.
3. Lọc `q`, sắp theo `assigned_at` mới nhất của bài giảm dần `[SoT: Suy luận]`, cắt trang.
4. `ClassProblemMetricsPort` lấy `submissionCount`, `acRatePercent` cho các bài của trang.

### 50.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `class_assignments` | R | chỉ mục `class_assignments(class_id) WHERE removed_at IS NULL` |
| `problems`, `topics`, `problem_topics`, `problem_levels` | R | |
| `identity` qua `InstructorClassPort`; `judge` qua `ClassProblemMetricsPort` | R | không join chéo schema |

### 50.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB50-N1 | Normal | giảng viên phụ trách 2 lớp, tab "Tất cả" -> bài giao cho cả hai lớp là **một** dòng với 2 phần tử `assignedClasses` |
| PB50-N2 | Normal | `classId` một lớp -> `submissionCount` chỉ của lớp đó |
| PB50-A1 | Abnormal | `classId` của lớp giảng viên khác -> `403 PRB-005` |
| PB50-A2 | Abnormal | cổng thống kê lỗi -> `200`, hai chỉ số `null`, danh sách vẫn hiện |
| PB50-B1 | Boundary | bài đã gỡ khỏi lớp (`removed_at`) không hiện; bài đã rút xuất bản vẫn hiện với `withdrawn = true`; 21 bài -> trang 2 có 1 dòng |

## 51. GET /instructor/class-assignments/summary (`GetInstructorAssignmentSummary`)

### 51.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `CLASS_MANAGEMENT:READ`; lớp người gọi phụ trách |
| Idempotent | có |
| Nguồn BD | `INS0202_class_assignments.md:472`, DTO NO 1, 2, 5 (dòng 427-431) |

### 51.2 Request
Không tham số.

### 51.3 Response (2xx)
`{ "assignedProblemCount": int, "classCount": int, "avgAcRatePercent": int|null }`. `assignedProblemCount` = số bài khác nhau đang giao (`removed_at IS NULL`) trong các lớp phụ trách; `classCount` = số lớp phụ trách (từ `identity`);
`avgAcRatePercent` = trung bình cộng tỉ lệ Accepted của các bài đang giao có lượt nộp, toàn thời gian, trong các lớp phụ trách, làm tròn số nguyên (`INS0202:431`); `null` khi chưa bài nào có lượt nộp hoặc cổng thống kê lỗi.
Các số "lượt nộp tuần này" và chênh lệch đã bỏ (`INS0202:429-432`).

### 51.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 403 | PRB-002 | thiếu quyền | |

### 51.5 Luồng xử lý
`InstructorClassPort` -> tập lớp; đếm `DISTINCT problem_id`; `ClassProblemMetricsPort.getAssignedProblemMetrics` cho các bài đó; trung bình các `acRatePercent` khác `null`.

### 51.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `class_assignments` | R | |
| `identity` qua `InstructorClassPort`; `judge` qua `ClassProblemMetricsPort` | R | |

### 51.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB51-N1 | Normal | 3 bài giao trên 2 lớp -> `assignedProblemCount = 3`, `classCount = 2`, AC trung bình làm tròn đúng |
| PB51-A1 | Abnormal | cổng thống kê lỗi -> `avgAcRatePercent = null`, hai số còn lại vẫn đúng |
| PB51-B1 | Boundary | giảng viên chưa phụ trách lớp nào -> `0`, `0`, `null` |

## 52. GET /instructor/classes/{classId}/assignable-problems (`ListAssignableProblems`)

### 52.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `CLASS_MANAGEMENT:READ`; lớp người gọi phụ trách |
| Idempotent | có |
| Phân trang | có (`pageSize` 20) |
| Nguồn BD | `DEC-2026-1011-class-assignment-popup-on-assignments-screen` (đòi DD định nghĩa `ListAssignableProblems`); `INS0202_class_assignments.md:493` (EVT-2) |

### 52.2 Request (query)
`q` (cắt còn 100 ký tự, khớp `code`/`title`), `page`, `pageSize` (20). Tham số đường dẫn `classId`.
Quyền ở mức `READ` (liệt kê không đổi dữ liệu); hành động giao là mục 53 với `UPDATE` `[SoT: Suy luận]`.

### 52.3 Response (2xx)
`{ "items": [ { "problemId", "code", "title", "levelCode", "levelDisplayName", "topicNames": [string] } ], "page": PageMeta }` — bài `PUBLISHED AND NOT deleted` **chưa** được giao (`removed_at IS NULL`) cho lớp đó. Giảng viên chọn từ **ngân hàng chung**, không chỉ bài của mình (F2-12).

### 52.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 403 | PRB-002, PRB-005 | thiếu quyền / không phụ trách lớp | |

### 52.5 Luồng xử lý
Kiểm `InstructorClassPort.isInstructorOfClass`; truy vấn `problems` đã xuất bản `NOT EXISTS (SELECT 1 FROM class_assignments WHERE problem_id = p.id AND class_id = :classId AND removed_at IS NULL)`; lọc, phân trang. Bài đã gỡ trước đây (`removed_at` khác `NULL`) **được liệt kê lại** để giao lại.

### 52.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `problems`, `problem_levels`, `topics`, `problem_topics`, `class_assignments` | R | |
| `identity` qua `InstructorClassPort` | R | |

### 52.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB52-N1 | Normal | bài đã xuất bản chưa giao -> có trong danh sách; bài đã giao cho lớp -> không có |
| PB52-A1 | Abnormal | bài `UNPUBLISHED`/`deleted` không bao giờ xuất hiện; lớp không phụ trách -> `403 PRB-005` |
| PB52-B1 | Boundary | bài từng giao rồi gỡ -> xuất hiện lại |

## 53. POST /instructor/classes/{classId}/assignments (`AssignProblemsToClass`)

### 53.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `CLASS_MANAGEMENT:UPDATE`; lớp người gọi phụ trách |
| Idempotent | có (bài đã giao được bỏ qua) |
| Nguồn BD | `DEC-2026-1011-class-assignment-popup-on-assignments-screen`; F2-12 (`01-rd/req/F2_problem-bank.md:48-60`) |

### 53.2 Request
`{ "problemIds": [uuid] }` — 1 đến 50 id (popup "Giao n bài", `INS0202:493`).

### 53.3 Response (2xx)
`200 { "succeededIds": [uuid], "skippedIds": [uuid], "failedItems": [ { "id", "code", "message" } ] }`. `succeededIds` là bài vừa được giao mới; `skippedIds` là bài đã đang giao cho lớp (không tạo dòng thứ hai); mỗi bài độc lập.

### 53.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 400 | PRB-001 | `problemIds` rỗng hoặc quá 50 | |
| 403 | PRB-002, PRB-005 | thiếu quyền / không phụ trách lớp | |
| (trong `failedItems`) | PRB-010 | bài không tồn tại | |
| (trong `failedItems`) | PRB-037 | bài không còn xuất bản (`UNPUBLISHED` hoặc `deleted`), không được giao thêm (`INS0202:520`) | |
| (trong `failedItems`) | PRB-056 | ghi Outbox của bài đó thất bại; rollback riêng bài, các bài khác vẫn xử lý; HTTP 200 cho lô | `message` chung, không chứa lỗi SQL hoặc stack trace |

### 53.5 Luồng xử lý
BR-18: mỗi bài kiểm `PUBLISHED AND NOT deleted`, rồi `INSERT INTO class_assignments (problem_id, class_id, assigned_by, assigned_at) ... ON CONFLICT (problem_id, class_id) WHERE removed_at IS NULL DO NOTHING`; dòng giao bài mới và Outbox `ProblemAssignedToClass` (kèm tiêu đề bài) ghi **cùng giao dịch riêng của bài đó**, relay phát sau commit. Nếu ghi Outbox thất bại, rollback dòng giao của bài ấy và đưa `{id, code:"PRB-056", message}` vào `failedItems`; lô vẫn trả `200`, các bài khác tiếp tục xử lý và có thể thành công. Không đưa bài lỗi vào `succeededIds` hoặc `skippedIds`; retry sau khi sửa lỗi được giao đúng một lần. Giao lại sau khi gỡ tạo **dòng mới**, dòng cũ giữ `removed_at`. [Chủ dự án chốt 2026-10-27]

### 53.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `class_assignments` | C | partial unique `(problem_id, class_id) WHERE removed_at IS NULL` |
| `problems` | R | |
| `problem.outbox_events` | C | Cùng giao dịch riêng của từng bài được giao mới; lỗi ghi rollback assignment của bài đó |
| `identity` qua `InstructorClassPort` | R | |

### 53.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB53-N1 | Normal | giao 3 bài mới -> `succeededIds` 3 phần tử, 3 sự kiện `ProblemAssignedToClass` |
| PB53-N2 | Normal | giao lại bài từng gỡ -> dòng mới, dòng cũ giữ `removed_at` |
| PB53-A1 | Abnormal | bài `UNPUBLISHED` -> `PRB-037` trong `failedItems`, các bài khác vẫn giao |
| PB53-A2 | Abnormal | lớp không phụ trách -> `403 PRB-005`, không ghi gì |
| PB53-B1 | Boundary | giao hai lần liên tiếp, hoặc hai giảng viên đồng thời -> chỉ một dòng hoạt động, lần sau vào `skippedIds`; 50 id được chấp nhận, 51 bị `400` |
| PB53-R1 | Recovery | một bài ghi Outbox lỗi: rollback riêng bài và trả `PRB-056` trong `failedItems` của response 200; bài khác thành công; retry bài lỗi sau phục hồi tạo đúng một assignment/event |

## 54. DELETE /instructor/classes/{classId}/assignments/{problemId} (`RemoveClassAssignment`)

### 54.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `CLASS_MANAGEMENT:UPDATE` (`02-bd/security/F2_problem-bank.md:15-16`); lớp người gọi phụ trách |
| Idempotent | không đổi dữ liệu khi lặp (lần hai trả `409 PRB-025`, trạng thái không đổi) |
| Nguồn BD | `INS0202_class_assignments.md:474`, EVT-8, Sheet 9 NO 6 (dòng 519) |

### 54.2 Request
Tham số đường dẫn `classId`, `problemId`. Không body.

### 54.3 Response (2xx)
`204 No Content`.

### 54.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 403 | PRB-002, PRB-005 | thiếu quyền / không phụ trách lớp | |
| 409 | PRB-025 | dòng giao bài không còn hoạt động (đã gỡ ở phiên khác hoặc chưa từng giao) | "Bài này đã được gỡ khỏi lớp trước đó. Đang tải lại danh sách." |

### 54.5 Luồng xử lý
BR-18: `UPDATE class_assignments SET removed_at = now() WHERE problem_id = ? AND class_id = ? AND removed_at IS NULL`; `0` dòng -> `409 PRB-025`; ngược lại phát `ProblemUnassignedFromClass`.
**Gỡ chỉ ẩn bài khỏi danh sách giao của lớp từ thời điểm gỡ; không xoá lượt nộp, điểm, tiến độ của học viên** (`DEC-2026-0831-class-assignments-round2`). Không chặn bài đã rút xuất bản.

### 54.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `class_assignments` | U | chỉ đặt `removed_at` |
| `identity` qua `InstructorClassPort` | R | |

### 54.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB54-N1 | Normal | gỡ bài đang giao -> `204`, `removed_at` được đặt, bài biến khỏi `ListMyClassAssignments` của học viên; lượt nộp, điểm của học viên còn nguyên |
| PB54-A1 | Abnormal | gỡ lần hai -> `409 PRB-025`, dòng không bị ghi đè `removed_at` |
| PB54-A2 | Abnormal | lớp giảng viên khác -> `403 PRB-005` |
| PB54-B1 | Boundary | gỡ bài đã `deleted`/`UNPUBLISHED` -> vẫn gỡ được |

## 55. GET /instructor/assigned-problems/top (`ListMyAssignedProblems`)

### 55.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `CLASS_MANAGEMENT:READ`; lớp người gọi phụ trách |
| Idempotent | có |
| Nguồn BD | `INS0101_overview.md:554`; khối "Bài tập của tôi" (V0.9: năm bài có lượt nộp cao nhất, `INS0101_overview.md:413`) |

### 55.2 Request
Không tham số. Số dòng cố định 5.

### 55.3 Response (2xx)
`{ "items": [ { "problemId", "title", "submissionCount": int } ] }`, tối đa 5 phần tử, xếp `submissionCount` giảm dần rồi `title`. `submissionCount` tính trong các lớp của giảng viên, chỉ học viên còn ghi danh (`INS0101:416`). Cổng thống kê lỗi -> `items = []`
(giao diện ẩn khối).

### 55.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 403 | PRB-002 | thiếu quyền | |

### 55.5 Luồng xử lý
Tập bài đang giao (`removed_at IS NULL`) của các lớp phụ trách; `ClassProblemMetricsPort` lấy `submissionCount`; sắp xếp và lấy 5.

### 55.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `class_assignments`, `problems` | R | |
| `identity` qua `InstructorClassPort`; `judge` qua `ClassProblemMetricsPort` | R | |

### 55.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB55-N1 | Normal | 7 bài giao -> trả đúng 5 bài nhiều lượt nộp nhất, đúng thứ tự |
| PB55-A1 | Abnormal | cổng thống kê lỗi -> `200 { items: [] }` |
| PB55-B1 | Boundary | giảng viên có 2 bài -> trả 2; không có bài -> `[]` |

## 56. GET /settings/ai-testcase-generation (`GetAiTestcaseGenerationSettings`)

### 56.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `AI_CONFIG:READ` |
| Idempotent | có |
| Nguồn BD | `ADM0303_ai_limits.md:433`, DTO NO 9 (dòng 402); `02-bd/database/F2_problem-bank.md:222-229` |

### 56.2 Request
Không tham số.

### 56.3 Response (2xx)
`{ "maxPerProblem": int }` — giá trị `max_ai_generations_per_problem` (mặc định 2).

### 56.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 403 | PRB-002 | thiếu `AI_CONFIG:READ` | |

### 56.5 Luồng xử lý
Đọc dòng singleton `problem_bank_settings WHERE id = 1`.

### 56.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `problem_bank_settings` | R | |

### 56.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB56-N1 | Normal | cài đặt mới -> `maxPerProblem = 2` |
| PB56-A1 | Abnormal | tài khoản không có `AI_CONFIG:READ` -> `403 PRB-002` |
| PB56-B1 | Boundary | dòng singleton luôn tồn tại sau migration (seed `id = 1`) |

## 57. PUT /settings/ai-testcase-generation (`UpdateAiTestcaseGenerationSettings`)

### 57.1 Hợp đồng
| Mục | Giá trị |
| :--- | :--- |
| Xác thực | JWT |
| Quyền | `AI_CONFIG:UPDATE` |
| Idempotent | có |
| Nguồn BD | `ADM0303_ai_limits.md:434`; `DEC-2026-1003-ai-testcase-generation-quota` |

### 57.2 Request
`{ "maxPerProblem": int }` — số nguyên từ 1 đến 10 (CHECK của cột, `02-bd/database/F2_problem-bank.md:227`; backend kiểm lại, `ADM0303:434`).

### 57.3 Response (2xx)
`200 { "maxPerProblem": int }`.

### 57.4 Lỗi
| HTTP | error.code | Khi nào | Ghi chú |
| :-: | :--- | :--- | :--- |
| 400 | PRB-001 | ngoài khoảng 1 đến 10 hoặc không phải số nguyên | |
| 403 | PRB-002 | thiếu `AI_CONFIG:UPDATE` | |
| 503 | PRB-050 | Không ghi được nhật ký hệ thống; cấu hình rollback | |

### 57.5 Luồng xử lý
Khoá dòng `problem_bank_settings` id 1, ghi giá trị cũ; `UPDATE ... SET max_ai_generations_per_problem = ?, updated_by = ?, updated_at = now()` rồi gọi `AuditLogPort.record(actorUserId, "AI_TESTCASE_GENERATION_LIMIT_CHANGED", "PROBLEM_BANK_SETTINGS", id, {maxPerProblem: cũ}, {maxPerProblem: mới})` **trong cùng giao dịch**. Lỗi ghi audit làm rollback cả cấu hình. Giảm giá trị **không** xoá bộ đếm đã dùng của từng bài: bài có `used >= max` mới sẽ bị `PRB-042` ở lần sinh kế tiếp. [Owner 2026-10-25, Q-13]

### 57.6 Truy cập dữ liệu
| Bảng/schema | Thao tác | Ghi chú |
| :--- | :--- | :--- |
| `problem_bank_settings` | U | singleton |
| `identity.system_audit_logs` qua `AuditLogPort` | C | một dòng cho thay đổi thành công; không ghi trực tiếp schema F1 |

### 57.7 Test viewpoints
| ID | Loại | Nội dung |
| :--- | :--- | :--- |
| PB57-N1 | Normal | đổi 2 thành 5 -> bài đã dùng 2 lần sinh thêm được 3 lần |
| PB57-A1 | Abnormal | `0` hoặc `11` -> `400 PRB-001`, giá trị cũ giữ nguyên |
| PB57-B1 | Boundary | `1` và `10` được chấp nhận; giảm xuống dưới số đã dùng không xoá bộ đếm của bài |
| PB57-N2 | Normal | đổi trần thành công ghi đúng một `AI_TESTCASE_GENERATION_LIMIT_CHANGED` với giá trị cũ/mới và người sửa |
| PB57-A2 | Abnormal | `AuditLogPort` lỗi -> `503 PRB-050`, cấu hình và `updated_by` đều rollback |

## 58. Cổng (port) nội bộ

Module là modular monolith: hai module nói chuyện bằng **cổng có tên**, sự kiện miền hoặc đường đọc riêng, không import chéo (`DEC-2026-0820-architecture-baseline`, `01-rd/system/SYS0101_backend_architecture.md` mục 5). Cổng do **bên gọi** khai trong `domain/ports/out` của mình
(`.nexa/control/dependency-map.md` mục 1); module sở hữu dữ liệu viết adapter. Mục này liệt kê cả hai chiều. Tên và chữ ký chốt ở đây; module bên kia đối chiếu khi viết DD của họ (xem `Q-16` mục 61).

### 58.1. Cổng vào do `problem-bank` phơi ra (adapter nằm trong `problem-bank`)

Tên cổng và phương thức dưới đây **dùng đúng tên các DD anh em đã chốt** (`03-dd/api/F4_judge-orchestration.md` mục 26 và dòng 71-77; `03-dd/api/F3_harness.md` mục 4, 6.3); không đặt tên mới ở phía họ.

Bảng 58.1: Cổng vào của `problem-bank` (không có route HTTP, lời gọi Java trong cùng JVM).

| Cổng (bên gọi khai) | Bên gọi | Phương thức | Trả về | Ghi chú |
| :--- | :--- | :--- | :--- | :--- |
| `ProblemGradingSpecPort` | `judge-orchestration` | `loadGradingSpec(problemId)`, `loadTestcaseSnapshot(problemId, version)` | `GradingSpec`, bundle testcase bất biến | `loadGradingSpec` gồm trạng thái xuất bản (`published: bool`, `deleted` coi như không xuất bản), `currentTestcaseSetVersion`, `snapshotReady` (metadata/object của phiên bản hiện hành đã có) và số ca đã duyệt; không kèm nội dung testcase. F4 ghim phiên bản lúc nộp; worker dùng `loadTestcaseSnapshot` để đọc nội dung và checksum theo đúng phiên bản, kể cả khi bài đã rút/xoá mềm |
| | | `loadSampleTestcases(problemId)` | `[ {testcaseId, position, input, expectedOutput} ]` | điều kiện `visibility = SAMPLE AND approved = true` nằm trong truy vấn |
| `ProblemSpecReaderPort` (`harness` khai, `03-dd/api/F3_harness.md:279-293`) | `harness` | `ProblemSpecQuery.findSpec(problemId)` | `Optional<ProblemSpecView>` | `ProblemSpecQuery` là **giao diện đọc `problem-bank` phơi ra** (tên `harness` đề xuất, DD này nhận); rỗng khi bài chưa có dòng `problem_specs`. Đọc qua Redis `problem:spec:<problemId>` (`02-bd/database/F2_problem-bank.md:280`) |
| `StarterCodeAccessPort` (`harness` khai, `03-dd/api/F3_harness.md` §4.4) | `harness` | `canRead(problemId, callerUserId, baseCategory)` | boolean | đọc trực tiếp `problems.status`, `deleted`, `author_id`; bài chưa xuất bản chỉ tác giả hoặc ADMIN, bài xuất bản mọi tài khoản đã đăng nhập. Không cache quyền theo `specVersion` |
| `ProblemContextPort` (`02-bd/architecture/F5_ai-review.md:86-103`) | `ai-review` | `getContext(problemId, language)` | `ProblemContext` | **dữ liệu, không phải chỉ thị**; xem dưới |
| `ProblemSummaryReadPort` (`judge` khai, dòng 75) | `judge-orchestration` | `getSummaries(problemIds)` -> `[ {problemId, code, title, slug, levelDisplayName, withdrawn} ]`; `searchProblemIds(keyword, limit)` | | `slug` trùng `code` (mã là slug, `02-bd/database/F2_problem-bank.md:18`); bài đã ẩn mềm vẫn trả để lịch sử hiển thị tên (`:56`) |
| `ProblemTopicReadPort` (`judge` khai, dòng 76) | `judge-orchestration` | `getProblemIdsByTopics(topicIds)` -> `Map<topicId, [problemId]>` | | trả quan hệ bài–chủ đề còn lưu, kể cả bài đã rút, cho tỉ lệ nộp bài lịch sử F4 §12; tiến độ hiện hành F2 §4/§14A tự lọc `PUBLISHED AND NOT deleted` |
| `ClassAssignmentReadPort` (`judge` khai, dòng 75) | `judge-orchestration`, `identity` (`INS0201`, `INS0203`, `INS0204`) | `listAssignments(classIds)` | `[ {classId, problemId, problemTitle, assignedAt, removedAt} ]` | **kể cả bài đã gỡ mềm** (`removedAt` khác `null`); bên cần chỉ bài đang giao tự lọc `removedAt == null`. Cùng use case với mục 50, không kiểm giảng viên |
| `ProblemBankStatsPort` (`03-dd/logic/F1_identity.md:312`) | `identity` (dashboard admin, tiến độ cá nhân), `ai-review` (tên bài ở `ADM0302`) | `publishedCatalogSummary()` (như mục 14.3); `problemSummaries(problemIds)`; `levelCatalog()` | | tên do `identity` DD đặt; gần trùng `ProblemSummaryReadPort`, xem `Q-33` |

`GradingSpec` = `{ problemId, published: bool, functionWrapperSupported, timeLimitMs, memoryLimitMb, maxOutputSizeKb, maxSubmissionsPerHour: int|null (thô; `judge` áp mặc định 60 khi `null`), currentTestcaseSetVersion, snapshotReady: bool, specVersion, matchingStrategy, epsilonValue,
testcases: [ { testcaseId, visibility, position } ] }` — đúng danh sách `judge` liệt kê ở dòng 73 của DD của họ. `testcases` chỉ gồm testcase `approved = true`: nháp AI chưa duyệt **không bao giờ** vào bộ chấm (`02-bd/security/F2_problem-bank.md:104-107`).
`specVersion` và `currentTestcaseSetVersion` trả **đồng bộ cùng lần đọc** khi F4 nhận bài nộp; F4 ghim phiên bản testcase lúc tạo dòng `PENDING` mà không cần sự kiện.
`ProblemSpecView` = `{ problemId, specVersion, matchingStrategy, epsilonValue, stdinFormatMd, stdoutFormatMd, functionName, returnType, parameters, nameOverrides, functionWrapperSupported }` — đúng Bảng 4 của `03-dd/api/F3_harness.md:84-99`
(chín cột của `problem_specs` cộng `problems.function_wrapper_supported`); `nameOverrides` là map `Language -> string` (khoá hiện lưu chữ HOA `JAVA`/`CPP`/`PYTHON`; câu hỏi chữ hoa-thường ở `Q-34`).
`ProblemContext` = `{ title, statementMarkdown, constraintsMarkdown, topics: [string], difficulty: string (display_name), functionSignature: { functionName, returnType, parameters }|null, knownOptimalComplexity: string|null,
aiBrief: string|null, noFullCode: bool, socraticOnly: bool }`; **không có** mã đáp án mẫu, testcase hay `object_key` (quy tắc 1.4).
`ProblemGradingSpecPort.loadTestcaseSnapshot(problemId, version)` là cổng backend-to-backend đọc bundle bất biến chứa cả Sample và Hidden; không có route công khai để đọc nội dung Hidden (`02-bd/security/F2_problem-bank.md:66-71`).

### 58.2. Cổng ra do `problem-bank` khai (`domain/ports/out`)

Bảng 58.2: Cổng ra của `problem-bank`. Cột "Thời gian chờ và thử lại" là hợp đồng lỗi bắt buộc cho mọi lời gọi ra ngoài tiến trình.

| Cổng | Bên cung cấp (adapter) | Phương thức | Thời gian chờ và thử lại | Khi lỗi |
| :--- | :--- | :--- | :--- | :--- |
| `SampleSolutionRunnerPort` (`02-bd/architecture/F2_problem-bank.md:77-95`, chốt giữ port hẹp ở `:151-155`) | go-judge trực tiếp (hạ tầng dùng chung, không qua hàng đợi `judge-orchestration`) | `runProgramBatch(language, sourceCode, stdins[], limits)` -> `RunProgramResult[]` (`status`, `stdout`, `stderrSnippet`, `runtimeMs`, `memoryKb`, `compileMessage`); biên dịch **một lần**; `runGeneratorScript(sourceCode, seed, maxRuntimeSeconds, maxOutputMb)` -> `RunScriptResult` (`status`: `OK`/`TIMEOUT`/`OUTPUT_TOO_LARGE`/`SCRIPT_ERROR`, `stdout`, `stderrSnippet`) | chờ = giới hạn chạy cộng 10 giây mỗi lần gọi; tổng 120 giây cho lượt chạy đáp án mẫu; thử lại **một lần** chỉ khi lỗi kết nối, không thử lại khi `TIMEOUT` hay lỗi chương trình | `503 PRB-050`, không đổi dữ liệu |
| | | script sinh dữ liệu chạy **không mạng**, không input, hạt giống cố định truyền như biến môi trường `SEED`; hai hạn mức riêng (`max_runtime_seconds`, `max_output_mb`) lấy từ `JudgeConfigReadPort`, **không dùng chung** với hạn mức bài nộp (`01-rd/req/F2_problem-bank.md:89-92`) | | |
| `ReferenceOutputMatcherPort` | `harness` (hàm thuần, không I/O; `.nexa/control/dependency-map.md` dòng 49) | `matches(strategy, epsilon, expected, actual)` -> `bool` | hàm đồng bộ trong JVM, không chờ | lỗi lập trình thì `503 PRB-050` `[SoT: Suy luận, tên cổng do DD đặt, cần đối chiếu DD harness]` |
| `TestcaseScriptGenerationPort` | `ai-review` (`feature_code = GENERATE_TESTCASE`, `DEC-2026-1010-ai-generate-testcase-feature-code` để DD chốt đường tới LLM) | `generate(GenerationInput)` -> `GenerationOutput` | chờ 120 giây, **không** thử lại (tốn token, kết quả không tất định) | `503 PRB-051`; không tính lượt, không đổi dữ liệu |
| | | `GenerationInput` = `{ problemId, statementMarkdown, constraintsMarkdown, samples: [ {input, expectedOutput} x2 ] }`; **kiểu này không có trường cho mã đáp án mẫu, `ai_brief`, testcase ẩn**; adapter đặt mọi trường vào khối có nhãn tách khỏi chỉ thị hệ thống (`02-bd/architecture/F5_ai-review.md:218`, `DEC-2026-0831-ai-instruction-injection-guard`) | | |
| | | `GenerationOutput` = `{ generatorScript, validatorScript, categoryLabels: [string], modelId, promptVersion, tokensUsed }`; script là mã **không tin cậy**, chỉ được chạy trong sandbox | | |
| `JudgeConfigReadPort` | `judge-orchestration` mục 26.3 của `03-dd/api/F4_judge-orchestration.md` (P3, đọc qua cổng, không đọc thẳng bảng) | `getGeneratorSandboxLimits()` -> `{ maxRuntimeSeconds, maxOutputMb }`; `getGradingDefaults()` (bốn giá trị mặc định); `getLanguageMultipliers(language)` -> `{ timeMultiplier, memoryMultiplier }` (đã có ở F4 P3) | chờ 3 giây, không thử lại | dùng hằng số dự phòng trong `application.yml` và ghi log cảnh báo (không chặn soạn đề) |
| `TestcaseObjectStorePort` | MinIO/S3 (bucket `algoprep-testcases`, `02-bd/storage/F2_problem-bank.md:27-32`) | `presignPut(key, ttl)`, `presignGet(key, ttl)`, `head(key)`, `get(key)`, `put(key, bytes)`, `copy(src, dst)`, `delete(key)` | chờ 10 giây; thử lại một lần cho thao tác đọc | `503 PRB-052`; ghi bên trong giao dịch DB thì rollback |
| `AuditLogPort` (`identity`, `02-bd/architecture/F1_identity.md:80-92`) | `identity` | `record(actorUserId, actionType, targetType, targetId, beforeJson, afterJson)` | cùng JVM, **cùng giao dịch DB** | thất bại thì thao tác nghiệp vụ rollback |
| `SolveStatePort` | `identity` (`GET /me/problem-solve-states`, mục #48 ở `03-dd/api/F1_identity.md:151`) | `listMyProblemSolveStates(userId)` -> `Map<problemId, SOLVED\|ATTEMPTED>` | chờ 2 giây; cache Redis `problem:solvestate:<userId>` TTL 60 giây phía `problem-bank` (`USR0101_problem_list.md:564`) | suy giảm êm: `solveState = null` (mục 2, 9, 13) |
| `StudentEnrollmentPort` | `identity` | `listActiveClassesOfStudent(userId)` -> `[ {classId, className} ]` | chờ 2 giây | `groups = []` (mục 5) |
| `InstructorClassPort` (`INS0202_class_assignments.md:455`) | `identity` | `listClassesOfInstructor(instructorId)`; `isInstructorOfClass(instructorId, classId)`; `getClassNames(classIds)` | chờ 2 giây | các endpoint cần kiểm sở hữu lớp trả `503`-tương đương; không giả định có quyền |
| `ClassProblemMetricsPort` | `judge-orchestration` (F4 P6) | `getAssignedProblemMetrics(instructorId, classIds, problemIds, assignments[])` -> `Map<problemId, {submissionCount, acRatePercent|null}>`; `assignments[]` gồm `{classId, problemId, assignedAt, removedAt?}` của **mọi lớp thuộc giảng viên** cho các bài yêu cầu, dù tab lọc một lớp, để F4 gắn mỗi lượt đúng một lớp trước khi lọc và không gọi ngược F2; chỉ học viên còn ghi danh và lượt trong khoảng đã giao | chờ 3 giây | hai chỉ số `null` (mục 50, 51, 55) |
| `MyClassAssignmentStatePort` | `judge-orchestration` (F4 P7) | `getMyAssignmentStates(assignments[])` theo principal STUDENT -> Map theo `(classId, problemId, assignedAt)` trả `SOLVED|ATTEMPTED|UNSOLVED`; chỉ lượt từ khi giao, F2 kiểm thành viên trước | chờ 3 giây | §5: `solveState=null`; §5A: `503` |
| `MyTopicSubmissionMetricsPort` | `judge-orchestration` (F4 P5) | `getMyTopicMetrics(topicProblemIds: Map<topicId, problemIds[]>)` theo principal hiện hành -> `Map<topicId, {submissionCount, acceptedCount, acRate|null}>` | chờ 3 giây/lô tối đa 50 chủ đề; không nhận `userId` từ client | `rankingAvailable=false`, `topicScores=[]` (mục 13) |
| `UserDirectoryPort` | `identity` | `getDisplayNames(userIds)` -> `Map<userId, string>` | chờ 2 giây | `displayName = null` (mục 34) |

Các cổng `UserDirectoryPort`, `ReferenceOutputMatcherPort`, `ClassProblemMetricsPort`, `MyTopicSubmissionMetricsPort` và phương thức `getLanguageMultipliers` là tên do DD này đặt cho adapter/use case module cung cấp (`Q-16`, `Q-32`); F4 P5 định nghĩa rõ đầu vào để không gọi ngược F2. `SolveStatePort`, `InstructorClassPort`, `StudentEnrollmentPort`, `AuditLogPort` bám tên hoặc endpoint BD/DD đã có.

## 59. Sự kiện miền

Phong bì thống nhất với `03-dd/api/F1_identity.md` mục sự kiện và `03-dd/api/F4_judge-orchestration.md` mục 25.1: `{ "eventId": uuid, "eventType": string, "occurredAt": ISO-8601, "schemaVersion": 1, "payload": {...} }`. F2 ghi dòng `problem.outbox_events` **trong cùng giao dịch** đổi dữ liệu; relay JOB-PB-01 phát lên exchange `algoprep.domain-events` (topic) với khoá `problem.spec.changed`, `problem.problem.published`, `problem.problem.unpublished`, `problem.testcase-set.version-bumped`, `problem.assignment.assigned`, `problem.assignment.removed`. Chỉ đánh dấu `published_at` sau publisher confirm; broker lỗi không làm mất sự kiện đã commit. Mỗi lần phát lại giữ nguyên `eventId`, consumer khử trùng theo `eventId` hoặc phiên bản tự nhiên. [Owner chốt 2026-10-26, Q-17]

### 59.1. Sự kiện `problem-bank` phát ra

Bảng 59.1: Sự kiện phát ra.

| `eventType` | Khi nào | `payload` | Bên nghe | Dùng để |
| :--- | :--- | :--- | :--- | :--- |
| `ProblemSpecChanged` | mục 38: **bất kỳ cột nào** của `problem_specs` đổi (`spec_version` tăng) | `{ problemId, specVersion, changedColumns: [string], signatureChanged: bool, signatureBefore?: {functionName, returnType, parameters}, signatureAfter?: {...} }` | `harness` | vô hiệu cache mã bọc hàm theo `problem_id`, so `specVersion` để bỏ qua sự kiện cũ (`02-bd/architecture/F2_problem-bank.md:67`) |
| `ProblemPublished` | mục 29, 40: `UNPUBLISHED -> PUBLISHED` | `{ problemId, actorUserId, occurredAt }` | Chưa có consumer | Dành cho tích hợp về sau; F1 chưa có `activity_type`, F4 kiểm xuất bản đồng bộ |
| `ProblemUnpublished` | mục 30, 33, 40: `PUBLISHED -> UNPUBLISHED` (kể cả do xoá mềm) | `{ problemId, actorUserId, occurredAt }` | Chưa có consumer | Dành cho tích hợp về sau; F1 chưa có `activity_type`, F4 kiểm xuất bản đồng bộ |
| `TestcaseSetVersionBumped` | mục 42, 43, 44, 47: bộ testcase đã duyệt (Sample hoặc Hidden) của bài `PUBLISHED` đổi | `{ problemId, testcaseSetVersion, bumpedAt }` | Chưa có consumer | Dành cho quan sát/cache về sau; F4 ghim phiên bản lúc nộp bằng lời gọi đồng bộ, bundle bất biến ở `testcase_set_snapshots` |
| `ProblemAssignedToClass` | mục 53: tạo dòng giao bài mới | `{ problemId, classId, actorUserId, problemTitle, assignedAt }` | `identity` | `activity_type = CLASS_PROBLEM_PUBLISHED` (`03-dd/api/F1_identity.md:258`); `problemTitle` là trường thêm `[SoT: Suy luận]` để feed khỏi phải hỏi lại |
| `ProblemUnassignedFromClass` | mục 54: gỡ mềm | `{ problemId, classId, actorUserId, removedAt }` | Chưa có consumer | Lịch sử lớp đọc `removed_at` đồng bộ; dành cho tích hợp về sau |

Idempotency phía người nghe: mỗi sự kiện có `eventId` (UUID v4); bên nghe dedupe theo `eventId` hoặc so khoá tự nhiên (`specVersion`, `testcaseSetVersion`) và bỏ qua giá trị không lớn hơn giá trị đã biết. Hai sự kiện `ProblemPublished`/`ProblemUnpublished` hiện **chưa có giá trị `activity_type` tương ứng** ở `identity`
(năm giá trị cố định, `03-dd/api/F1_identity.md:256-262`), nên `identity` bỏ qua chúng cho tới khi RD/BD thêm một loại (`Q-29` mục 61).

### 59.2. Sự kiện `problem-bank` nhận vào

Bảng 59.2: Sự kiện nhận vào.

| `eventType` | Nguồn | `payload` | Cập nhật | Idempotency |
| :--- | :--- | :--- | :--- | :--- |
| `SubmissionGraded` | `judge-orchestration` | `{ submissionId, userId, problemId, language, verdict, passedCount, totalCount, passedTestcaseRatio, submittedAt, firstAttemptForProblem }` (`03-dd/api/F4_judge-orchestration.md` mục 25.2; phát cho mọi trạng thái cuối) | `problem_stats` (BR-20) | theo `eventId` qua bảng dedupe `problem.consumed_events` (BD database F2 mục 1.14), cùng giao dịch với cập nhật bộ đếm; bắt buộc vì `judge` giao at-least-once qua outbox (`F4_judge-orchestration.md:1220`) |
| `ClassDeleted` | `identity` (`03-dd/api/F1_identity.md:268-272`, BR-22) | `{ classId }` | `class_assignments`: `removed_at = now()` cho dòng `class_id` khớp và `removed_at IS NULL` (BR-19) | tự nhiên: dòng đã có `removed_at` không còn khớp điều kiện, nhận lại không đổi gì |

`SubmissionAccepted` **không được tiêu thụ**: nó trùng thông tin với `verdict = ACCEPTED` của `SubmissionGraded`, nghe cả hai sẽ đếm đôi (`[SoT: Suy luận]`, khác với việc `02-bd/architecture/F2_problem-bank.md:105` liệt kê cả hai).

## 60. Bảng mã lỗi `PRB`

Bảng 60.1: Mã lỗi của module (`error.code`, backend trả mã, giao diện dịch qua `messages/{locale}.json`). Dải dùng: `PRB-001` tới `PRB-056` (`PRB-034`, `PRB-035` đã bỏ vì trùng `HRN-V22`, `HRN-V20`). Cảnh báo không chặn dùng tiền tố `PRB-W`.

| Mã | HTTP | Tên enum | Ý nghĩa | Mục dùng |
| :--- | :-: | :--- | :--- | :--- |
| PRB-001 | 400 | `VALIDATION_FAILED` | trường sai, thiếu, quá độ dài; `details.fields[] = { field, rule }` | mọi endpoint ghi |
| PRB-002 | 403 | `FORBIDDEN_FUNCTION` | thiếu `FUNCTION:ACTION` cần thiết | mọi endpoint có quyền |
| PRB-003 | 403 | `NOT_PROBLEM_AUTHOR` | A2 ghi lên bài `PUBLISHED` của người khác | 29-33, 35-45, 46-49 |
| PRB-004 | 403 | `ADMIN_ONLY` | có Function nhưng không phải vai trò `ADMIN` (danh mục chủ đề, độ khó) | 16-19, 21-24 |
| PRB-005 | 403 | `NOT_CLASS_INSTRUCTOR` | không phụ trách lớp | 50-54 |
| PRB-010 | 404 | `PROBLEM_NOT_FOUND` | bài không tồn tại, `deleted`, chưa xuất bản (với học viên) hoặc bản nháp của người khác (với A2) | nhiều |
| PRB-011 | 404 | `TESTCASE_NOT_FOUND` | testcase không tồn tại hoặc không thuộc bài | 43, 44, 48 |
| PRB-012 | 404 | `TOPIC_NOT_FOUND` | chủ đề (ở đường dẫn) không tồn tại | 22, 24 |
| PRB-013 | 404 | `LEVEL_NOT_FOUND` | mức độ khó (ở đường dẫn) không tồn tại | 17, 19 |
| PRB-014 | 404 | `BOOKMARK_NOT_FOUND` | sửa ghi chú khi bookmark đã mất | 10 |
| PRB-015 | 404 | `MY_CLASS_NOT_FOUND` | `classId` sai dạng, lớp không có, người gọi đã rời hoặc không thuộc lớp | 5A |
| PRB-020 | 409 | `CONTENT_VERSION_CONFLICT` | `expectedUpdatedAt` lệch; `details.currentUpdatedAt` | 37, 38, 39, 40 |
| PRB-021 | 409 | `TOPIC_NAME_DUPLICATE` | tên chủ đề trùng | 21, 22 |
| PRB-022 | 409 | `LEVEL_NAME_DUPLICATE` | tên độ khó trùng | 16, 17 |
| PRB-023 | 409 | `GENERATION_IN_PROGRESS` | đang có lượt sinh testcase của bài | 49 |
| PRB-025 | 409 | `ASSIGNMENT_ALREADY_REMOVED` | dòng giao bài không còn hoạt động | 54 |
| PRB-030 | 422 | `PUBLISH_CHECKLIST_NOT_MET` | chưa đạt checklist xuất bản; `details.items[]` | 29, 40 |
| PRB-031 | 422 | `TOPIC_IN_USE` | chủ đề còn bài tham chiếu; `details.referencingProblemCount` | 24 |
| PRB-032 | 422 | `LEVEL_IN_USE` | độ khó còn bài tham chiếu; `reason = IN_USE`, `referencingProblemCount` | 19 |
| PRB-033 | 422 | `LEVEL_LAST_ONE` | mức cuối cùng; `reason = LAST_LEVEL` | 19 |
| PRB-036 | 422 | `FUNCTION_SPEC_REJECTED` | `FunctionSpecEvaluator` báo vi phạm cứng; `details.violations[] = { code: "HRN-Vnn", path, message }` (gồm cả `HRN-V20`, `HRN-V22`) | 38 |
| PRB-037 | 422 | `PROBLEM_NOT_ASSIGNABLE` | bài không còn xuất bản, không giao thêm cho lớp | 53 |
| PRB-039 | 422 | `REFERENCE_NOT_FOUND` | `levelId`/`topicId`/`topicIds[i]` trong thân yêu cầu không còn tồn tại; `details.field` | 31, 32, 36, 37 |
| PRB-040 | 422 | `REFERENCE_SOLUTION_MISSING` | chưa có đáp án mẫu | 48, 49 |
| PRB-041 | 422 | `GENERATION_PRECONDITION_FAILED` | thiếu điều kiện sinh; `details.reasons[]` | 49 |
| PRB-042 | 422 | `GENERATION_QUOTA_EXHAUSTED` | hết lượt sinh của bài; `details = { used, limit }` | 49 |
| PRB-043 | 422 | `GENERATOR_SCRIPT_REJECTED` | script bị loại; `details.reason` | 49 |
| PRB-044 | 422 | `UPLOAD_SESSION_INVALID` | phiên tải lên không có, hết hạn, sai bài hoặc sai người | 47 |
| PRB-045 | 422 | `UPLOAD_OBJECT_INVALID` | thiếu object, sai kích thước hoặc sai mã hoá; `details.items[]` | 47 |
| PRB-046 | 422 | `TESTCASE_LIMIT_EXCEEDED` | bài sẽ vượt 500 testcase | 42, 46 |
| PRB-047 | 422 | `BULK_UPLOAD_LIMIT_EXCEEDED` | vượt số cặp hoặc dung lượng một lô | 46 |
| PRB-048 | 422 | `TESTCASE_CONTENT_TOO_LARGE` | một vế vượt 1 MB khi nhập trực tiếp | 42, 43 |
| PRB-049 | 422 | `REORDER_SET_MISMATCH` | danh sách thứ tự không bằng tập hiện có | 18, 23, 45 |
| PRB-050 | 503 | `SANDBOX_UNAVAILABLE` | go-judge hoặc bộ so khớp không đáp ứng | 29, 40, 48, 49 |
| PRB-051 | 503 | `AI_UNAVAILABLE` | đường AI không đáp ứng hoặc hết hạn mức | 49 |
| PRB-052 | 503 | `OBJECT_STORAGE_UNAVAILABLE` | MinIO không đáp ứng | 42, 46, 47 |
| PRB-053 | 422 | `WORKED_EXAMPLES_BELOW_MINIMUM` | bài `PUBLISHED` sẽ còn dưới 2 ví dụ mẫu | 37 |
| PRB-054 | 422 | `APPROVED_TESTCASES_BELOW_MINIMUM` | bài `PUBLISHED` sẽ còn dưới 8 ca đã duyệt hoặc 2 Sample đã duyệt | 43, 44 |
| PRB-055 | 429 | `REFERENCE_RUN_RATE_LIMITED` | quá 10 lượt chạy đáp án mẫu trong 60 giây trượt theo tài khoản | 48 |
| PRB-056 | trong `failedItems` của HTTP 200 | `ASSIGNMENT_OUTBOX_WRITE_FAILED` | ghi Outbox khi giao bài lỗi; rollback riêng bài, các bài khác vẫn xử lý | 53 |
| PRB-W01 | (cảnh báo, `200`) | `TYPE_OUTSIDE_SCHEMA` | vi phạm mềm `HRN-V01` tới `HRN-V07`, `functionWrapperSupported = false`; mã gốc trong `reasonCode` | 38 |

Mã `PRB-024`, `PRB-026` tới `PRB-029`, `PRB-034`, `PRB-035`, `PRB-038` để trống có chủ đích (dự phòng theo nhóm).

## 61. Câu hỏi mở và đề xuất của DD

Mục này gom mọi chỗ DD phải suy luận, mọi mâu thuẫn giữa các nguồn, và mọi đề xuất cần chủ dự án quyết hoặc cần ghi decision registry. DD **không** tự đổi BD hay RD (ngoài phạm vi của lượt này); các điểm dưới đây chuyển cho controller.

Bảng 61.1: Câu hỏi mở.

| ID | Câu hỏi hoặc mâu thuẫn | Nguồn | Đề xuất của DD |
| :--- | :--- | :--- | :--- |
| Q-01 | **Đã đóng tại BD database F2 mục 1.13:** RD F2-14 bước 8 đòi lưu script kèm hạt giống làm xuất xứ | `01-rd/req/F2_problem-bank.md:104-106` | Bảng `problem.testcase_generation_runs` ghi cùng giao dịch với testcase nháp và bộ đếm; DD mục 49 dùng bảng này |
| Q-02 | **Đã đóng tại BD database F2 mục 1.14:** nhận `SubmissionGraded` phải khử trùng theo `eventId` | `03-dd/api/F1_identity.md:251` ("idempotent theo eventId") | Bảng `problem.consumed_events(event_id uuid PK, consumed_at)`; chèn khoá và tăng số liệu trong một giao dịch, không dọn theo thời gian cố định để tránh đếm trùng khi replay |
| Q-03 | **Đã chốt owner 2026-10-25:** A2 mở bài `PUBLISHED` của người khác không thấy mã đáp án mẫu | `SHR0203_problem_info.md:382`; `DEC-2026-1011-a2-read-published-problems-of-others` | F2 §34 đặt `referenceSolution = null` và không trả mã/URL; DD `SHR0203` ẩn thẻ |
| Q-04 | Khi nào tăng `current_testcase_set_version`: DB nói "bài đã xuất bản", `SHR0202` Sheet 6 thêm "và đã có submission" | `02-bd/database/F2_problem-bank.md:171`; `SHR0202_problem_authoring.md` Sheet 6 Khu E NO 2 (dòng 525) | DD theo DB (chỉ cần `PUBLISHED`), vì không biết bài có lượt nộp hay chưa. Lệch là tăng thừa, không nguy hiểm |
| Q-05 | Kiểu vượt lược đồ: Sheet 9 NO 5 và `harness` mục 4.5 nói **cảnh báo, không chặn**, còn EVT-25 nói "viền đỏ + toast lỗi" | `SHR0202_problem_authoring.md:767` và `:708` | DD theo `FunctionSpecEvaluator`: vi phạm mềm `HRN-V01` tới `V07` lưu được kèm `PRB-W01`, vi phạm cứng `HRN-V10` trở lên `422 PRB-036` |
| Q-06 | **Đã chốt owner 2026-10-25:** bài `PUBLISHED` không được tụt dưới ngưỡng testcase checklist do xoá/đổi ca | `SHR0202` Sheet 6 Khu D NO 6 (dòng 517) | F2 §43–44 và BR-07 chặn dưới 8 ca duyệt hoặc 2 Sample bằng `PRB-054`; ví dụ mẫu vẫn dùng `PRB-053` |
| Q-07 | **Đã đóng tại BD database F2 phần mở đầu:** testcase có thao tác xoá cứng khi tác giả xoá ca | `02-bd/database/F2_problem-bank.md` phần mở đầu | DD §44 kiểm checklist của bài đã xuất bản trước khi xoá; snapshot phiên bản cũ vẫn giữ cho lượt nộp đã ghim |
| Q-08 | Định dạng "tải lên theo lô" (F2-07) chưa được RD/BD định nghĩa (zip, cặp `.in`/`.out`...) | `01-rd/req/F2_problem-bank.md:31` | DD không định nghĩa định dạng gói: giao diện ghép cặp file ở trình duyệt, API nhận từng cặp. Quy ước ghép cặp thuộc DD màn `problem_authoring` |
| Q-09 | **DD đóng ở USR0102 OQ-10:** ai tách input testcase mẫu theo tên tham số ở mô hình Bọc hàm | F2 trả `parameterNames` và nội dung nguyên văn; màn hiện một khối input cùng nhãn tên tham số, không tách ở client. F3 đã định nghĩa định dạng stdin chuẩn ở logic §3.3; không cần endpoint tách mới |
| Q-10 | Đáp án mẫu (F2-18) là chương trình `stdin`/`stdout` hay hàm bọc? RD chỉ nói "một chương trình" | `01-rd/req/F2_problem-bank.md:131` | DD coi là chương trình `stdin`/`stdout`, chạy với input chuẩn hoá; nếu cần chạy qua mã bọc hàm thì thêm phụ thuộc `harness` |
| Q-15 | Kiểm "input thoả Ràng buộc dữ liệu" (F2-14 bước 6): ràng buộc là Markdown tự do, RD không nêu **cơ chế máy kiểm** | `01-rd/req/F2_problem-bank.md:96-99` | DD yêu cầu AI trả thêm `validatorScript` (đọc một input từ `stdin`, thoát `0` nếu hợp lệ), chạy trong cùng sandbox. Hạn chế đã biết: validator cũng do AI viết nên không độc lập |
| Q-11 | Tên trường: `withdrawn` (bản dựng) hay `problemStatus` (BD `INS0202` 7.1 NO 14) | `INS0202_class_assignments.md:440` | DD dùng `withdrawn: bool` |
| Q-12 | "Bài cần chú ý": `STALE_DRAFT` dùng `created_at` (Sheet 5) hay `updated_at` (RD, DB)? `LOW_AC` ghi `< 0.30` nhưng cột là thang 0-100 | `SHR0201_problem_management.md:528`; `01-rd/req/F2_problem-bank.md:165-167`; `02-bd/database/F2_problem-bank.md:35,215` | DD dùng `updated_at` và `< 30` |
| Q-13 | **Đã chốt owner 2026-10-25:** ADMIN đổi trần sinh testcase có ghi nhật ký | `ADM0303_ai_limits.md:494` (Q2) | Gọi F1 `AuditLogPort` cùng giao dịch với cấu hình, `actionType = AI_TESTCASE_GENERATION_LIMIT_CHANGED`, nhóm `CONFIG`; vẫn lưu `updated_by` |
| Q-14 | **Đã đóng tại F1 BR-19/BR-24:** `AuditLogPort` nhận cả hành động ghi trên bài và danh mục F2 | `02-bd/architecture/F1_identity.md` §3.3; `SHR0201_problem_management.md` Sheet 9 NO 9 | F1 `AuditAction` có đủ `PROBLEM_PUBLISHED`, `PROBLEM_UNPUBLISHED`, `PROBLEM_DIFFICULTY_CHANGED`, `PROBLEM_TOPIC_ASSIGNED`, `PROBLEM_DUPLICATED`, `PROBLEM_DELETED`, `PROBLEM_TOPIC_*`, `PROBLEM_LEVEL_*`; nhóm hiển thị `CONTENT` |
| Q-16 | Các cổng do module khác viết adapter: `SolveStatePort`, `StudentEnrollmentPort`, `InstructorClassPort`, `UserDirectoryPort`, `AuditLogPort` (identity); `MyTopicSubmissionMetricsPort` (F4 P5), `ClassProblemMetricsPort` (P6), `MyClassAssignmentStatePort` (P7), `JudgeConfigReadPort` (judge); `TestcaseScriptGenerationPort` (ai-review); `ReferenceOutputMatcherPort` (harness) | mục 58.2 | Chữ ký P5–P7 đã đối chiếu hai chiều với F4 §26; P7 thay nguồn trạng thái toàn cục cho §5/§5A |
| Q-17 | **Đã chốt owner 2026-10-26:** sự kiện F2 phải bền qua lỗi RabbitMQ | mục 59; BD database F2 mục 1.16 | Ghi `problem.outbox_events` cùng giao dịch, relay JOB-PB-01 publish có confirm; không còn `afterCommit` phát trực tiếp |
| Q-18 | Verdict `SYSTEM_ERROR` có tính vào `submission_count`/`ac_rate` không? | `02-bd/database/F4_judge-orchestration.md:26,31` | DD **không** tính `SYSTEM_ERROR` (lỗi hệ thống không phản ánh bài làm); các verdict cuối còn lại đều tính |
| Q-19 | Sửa đặc tả/đáp án mẫu của bài đang `PUBLISHED` làm bài không còn "đã khai đặc tả" thì sao? | `02-bd/database/F2_problem-bank.md:136` | DD cho phép lưu; checklist chỉ chặn ở bước chuyển sang `PUBLISHED`. `judge-orchestration` ghim theo `spec_version` |
| Q-20 | Nhân bản có sao chép ngữ cảnh AI (`ai_brief`, hai cờ) và reset bộ đếm `ai_testcase_generations_used` không? RD liệt kê nội dung sao chép nhưng không nêu hai thứ này | `01-rd/req/F2_problem-bank.md:168-174` | DD **không** sao chép ngữ cảnh AI (về mặc định) và bộ đếm của bản sao bắt đầu từ 0 |
| Q-21 | Bài **chưa khai chữ ký** (`HRN-V07`, mềm) đặt `wrapperSupported = false`; nếu `SPEC_DECLARED` miễn kiểm chữ ký khi `wrapperSupported = false` thì bài không có chữ ký vẫn "đã khai đặc tả" | `03-dd/validation/F3_harness.md:76`; `SHR0202` Sheet 5 Khu H NO 5 | DD: `SPEC_DECLARED` đòi **chữ ký đã khai** (ba cột khác `null`), chỉ miễn khi chữ ký có kiểu ngoài lược đồ `HRN-V01` tới `V06`; tham số rỗng được phép theo `harness` |
| Q-22 | Tìm kiếm có phân biệt dấu không; thứ tự mặc định các danh sách; sắp theo `code` ở màn quản lý (`SHR0201` NO 3 sắp theo "giá trị số của mã") khi `code` nay là slug | `SHR0201_problem_management.md` Khu D NO 3 | DD: khớp không phân biệt hoa thường, **không** bỏ dấu; mặc định học viên `code` tăng, quản trị `updatedAt` giảm; sắp `code` theo thứ tự từ điển không phân biệt hoa thường |
| Q-23 | Tên giá trị `NOT_SOLVED` (cho `USR0105`) và tham số `tag` chưa có nguồn tên | `USR0105_workspace_home.md:66`; `01-rd/req/F2_problem-bank.md:45-47` | DD đặt tên như mục 2, 3 |
| Q-24 | Các trần kỹ thuật chưa có nguồn: đề 100.000 ký tự, ràng buộc 20.000, `ai_brief` 4.000, thẻ 50 ký tự và 20 thẻ/bài, mã nguồn đáp án 64 KB, 500 testcase/bài, 1 MB nhập trực tiếp, 100 cặp/64 MB một vế/256 MB một lô | `02-bd/security/F2_problem-bank.md:115-117` giao DD chốt số | Đề xuất như trên, ghi ở `03-dd/validation/F2_problem-bank.md`; chủ dự án chỉnh được |
| Q-25 | **Đã chốt owner 2026-10-25:** lượt nộp chấm theo đúng testcase tại lúc nộp, dù worker nhận sau khi tác giả sửa bài | `02-bd/database/F2_problem-bank.md` mục 1.6, 1.15 | F2 công bố bundle bất biến cho mỗi phiên bản trên bài đã xuất bản; F4 ghim phiên bản lúc nộp và worker đọc bằng `loadTestcaseSnapshot`, xác minh checksum. Không đọc bộ hiện hành để chấm lượt cũ |
| Q-26 | Sinh testcase AI chạy đồng bộ tới 5 phút: có cần hàng đợi bất đồng bộ không? | `SHR0202_problem_authoring.md:699` | Giữ đồng bộ (ponytail bậc 5); nâng lên tác vụ nền khi đo được vượt thời gian chờ của hạ tầng |
| Q-29 | `ProblemPublished`/`ProblemUnpublished`: BD liệt `identity` là bên nghe nhưng `identity` không có `activity_type` tương ứng | `02-bd/architecture/F2_problem-bank.md:68`; `03-dd/api/F1_identity.md:256-262` | Vẫn phát; `identity` bỏ qua đến khi RD thêm loại hoạt động |
| Q-30 | "Chạy với đáp án mẫu" chạy mọi testcase **đã duyệt** hay "toàn bộ testcase hiện có" (kể cả nháp)? | `01-rd/req/F2_problem-bank.md:132-134`; `SHR0202` Khu E NO 5 | DD: mọi testcase đã duyệt; chạy từng dòng thì được cả nháp |
| Q-31 | **DD chốt kỹ thuật:** hai endpoint dùng sandbox cần kiểm mức tiêu thụ | `02-bd/security/F2_problem-bank.md` mục 5 | §48 giới hạn 10 lượt/60 giây trượt/người bằng Redis, `429 PRB-055`; §49 đã có trần theo bài, không thêm trần theo người |

Bảng 61.2: Quyết định đề xuất ghi vào `.nexa/control/decision-registry.md` (controller ghi, DD này không sửa file đó).

| ID | Chủ đề | Loại | Nội dung | Tiêu chí ponytail |
| :--- | :--- | :--- | :--- | :--- |
| D-PB-01 | Outbox F2 cho sự kiện miền | architecture | `problem-bank` ghi outbox cùng giao dịch; JOB-PB-01 phát ít nhất một lần và giữ nguyên `eventId` khi thử lại | tránh mất `ProblemAssignedToClass` và sự kiện cache khi broker lỗi |
| D-PB-02 | Danh mục cổng của `problem-bank` | architecture | chốt tên và chữ ký ở mục 58 (hai chiều), gồm cổng thêm `SampleSolutionRunnerPort.runGeneratorScript` (nới port theo `02-bd/architecture/F2_problem-bank.md:89-95`) | bậc 4 |
| D-PB-03 | Hai bảng bổ sung | schema | thêm `testcase_generation_runs` (Q-01) và `consumed_events` (Q-02) vào schema `problem` | bậc 1 (RD F2-14 bước 8), bậc 6 |
| D-PB-04 | Khoá lạc quan | API contract | token là `problems.updated_at` gửi qua `expectedUpdatedAt`; mọi ghi trong phạm vi bài trả `problemUpdatedAt`; đóng `SHR0202` Q16 | bậc 4 |
| D-PB-05 | Thao tác lô độc lập từng bài | API contract | N lệnh đơn, phản hồi `200` với `succeededIds`/`failedItems` | bậc 3 |
| D-PB-06 | Endpoint tổng quan riêng | API contract | `GetProblemOverview` tách khỏi `GetProblemForWorkspace`; đóng `USR0104` Q4 | bậc 3 |
| D-PB-07 | Cách sinh slug | schema | một quy tắc cho `problems.code`, `topics.code`, `problem_levels.code` (BR-03); đóng câu hỏi "chốt ở DD" của `02-bd/database/F2_problem-bank.md:84,108,306` | bậc 4 |
| D-PB-08 | Luồng tải lên theo lô | API contract | presign theo từng cặp, phiên Redis, xác nhận bằng `HEAD`, tất cả hoặc không; đóng `02-bd/storage/F2_problem-bank.md:66` | bậc 4, 5 |
| D-PB-09 | Hai kiểu "chương trình" của F2-14/F2-18 | scope | đáp án mẫu là chương trình `stdin`/`stdout`; AI trả thêm script kiểm ràng buộc (Q-10, Q-15) | bậc 1 |
| D-PB-10 | Bookmark idempotent | API contract | `SaveMyBookmark` có `createIfMissing`; `ToggleMyProblemBookmark` nhận trạng thái đích | bậc 5 |
| D-PB-11 | Điều kiện tăng phiên bản bộ testcase | API contract | bài `PUBLISHED` và bất kỳ testcase đã duyệt trong bộ chấm đổi, gồm Sample và Hidden; tạo snapshot bất biến (Q-25) | bậc 2 |
| D-PB-12 | Mở rộng `AuditLogPort` | API contract | thêm các giá trị `actionType` của bài toán (Q-14) vào `identity` | bậc 6 |

## 62. Tham chiếu

- RD: `01-rd/req/F2_problem-bank.md` (F2-01 tới F2-18).
- BD: `02-bd/architecture/F2_problem-bank.md`, `02-bd/database/F2_problem-bank.md`, `02-bd/security/F2_problem-bank.md`, `02-bd/storage/F2_problem-bank.md`; BD màn `SHR0201`, `SHR0202`, `SHR0203`, `USR0101` tới `USR0104`, `USR0201`, `USR0601`, `INS0101`, `INS0202`, `ADM0303`.
- Quyết định: `DEC-2026-0830-problem-lifecycle-two-states`, `DEC-2026-0831-problem-management-lifecycle-details`, `DEC-2026-0831-class-assignments-round2`, `DEC-2026-0928-f2-14-generator-script`, `DEC-2026-1003-ai-testcase-generation-quota`,
  `DEC-2026-1003-single-function-signature`, `DEC-2026-1008-problem-delete-and-duplicate`, `DEC-2026-1010-ai-generate-testcase-feature-code`, `DEC-2026-1011-a2-read-published-problems-of-others`,
  `DEC-2026-1011-class-assignment-popup-on-assignments-screen`, `DEC-2026-1022-problem-bank-schema-sync`, `DEC-2026-1023-db-actor-review-decisions`.
- DD: `03-dd/logic/F2_problem-bank.md` (BR-nn), `03-dd/validation/F2_problem-bank.md`, `04-tdd/modules/F2_problem-bank.md` (AC-nn). Không có `03-dd/jobs/F2_problem-bank.md`: BD không định nghĩa job nền nào cho module này.
- Evidence (đọc sau cùng, không phải SoT): `05-coding/frontend/src/entities/problem/model/draft-types.ts`.

Bảng 61.3: Điểm lệch với DD của hai module anh em (đã đọc `03-dd/api/F3_harness.md`, `03-dd/validation/F3_harness.md`, `03-dd/api/F4_judge-orchestration.md`; DD này **không sửa** các file đó).

| ID | Điểm lệch | Bên kia nói gì | DD `problem-bank` làm gì |
| :--- | :--- | :--- | :--- |
| Q-32 | **Đã đóng tại F4 P3:** F2 cần hệ số nhân ngôn ngữ để chạy đáp án mẫu đúng giới hạn | `F4_judge-orchestration.md` §26.3 | Thêm `getLanguageMultipliers(language)` trả hệ số thời gian/bộ nhớ từ `language_configs`; F2 dùng ở §48/§49 |
| Q-33 | **Đã đóng tại DD:** F4 dùng `ProblemSummaryReadPort`, F1 dùng `ProblemBankStatsPort` cho một phần dữ liệu trùng nhau | `F4_judge-orchestration.md` §1; `F1_identity.md` cổng | Giữ hai cổng theo ngôn ngữ bên gọi, F2 adapter dùng chung một truy vấn nội bộ cho tên/độ khó; không thêm route và không ép hai DTO khác nhau thành một |
| Q-34 | Khoá `name_overrides`: `harness` đọc không phân biệt hoa thường, ghi chữ HOA (`HRN-V18`); `harness` nêu đây là câu hỏi mở | `03-dd/validation/F3_harness.md:84` | `problem-bank` lưu **chữ HOA** (khớp `02-bd/database/F2_problem-bank.md:135`), `ProblemSpecView.nameOverrides` trả khoá HOA |
| Q-35 | **Đã đóng:** `ClassProblemMetricsPort` cho số lượt nộp và AC theo lớp/bài | `F4_judge-orchestration.md` §26.6 | F4 P6 nhận giảng viên, tập lớp và bài; lọc khoảng giao/gỡ và ghi danh, trả đủ Map để F2 §50/§51/§55 dùng |
| Q-36 | **Đã đóng cùng Q-02:** `judge` phát `SubmissionGraded` ít nhất một lần qua outbox | `F4_judge-orchestration.md:1213-1220` | `problem.consumed_events` ở BD database F2 mục 1.14 khử trùng theo `eventId` trong cùng giao dịch cập nhật `problem_stats` |
| Q-37 | `identity` tính `SYSTEM_ERROR` và `COMPILE_ERROR` vào `total_submissions`; `problem-bank` loại `SYSTEM_ERROR` khỏi `submission_count` | `F4_judge-orchestration.md:1217` | giữ `Q-18`; hai con số "tổng lượt nộp" có thể lệch giữa `identity` và `problem_stats` |
| Q-38 | **Đã đóng tại BD kiến trúc F2 §sự kiện:** `judge` không nghe `TestcaseSetVersionBumped`, `ProblemSpecChanged`, `ProblemPublished/Unpublished` | `F4_judge-orchestration.md` §25.5 | F2 vẫn phát theo hợp đồng; F4 đọc phiên bản đồng bộ lúc nộp, `harness` nghe `ProblemSpecChanged`, `identity` nghe `ProblemAssignedToClass` |
