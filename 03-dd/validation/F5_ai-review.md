# DD Validation — Module `ai-review` (F5)

> Chỉ ghi quy tắc kiểm mà BD chưa tự suy ra hoặc cần chốt thành mã lỗi cụ thể: trường, quy tắc, mã lỗi, **lớp** thực thi. Không lặp thông điệp giao diện ở Sheet 9 của BD màn; file này là bản cố định **ở máy chủ** của các kiểm tra đó.
> Nguồn: `02-bd/database/F5_ai-review.md` (kiểu cột, CHECK), `02-bd/security/F5_ai-review.md`, Sheet 9 của `USR0301`, `USR0302`, `ADM0301`, `ADM0302`, `ADM0303`. Hợp đồng endpoint ở `03-dd/api/F5_ai-review.md` (Bảng 0 giải nghĩa tên viết tắt nguồn); mã lỗi ở mục 34 của file đó.
> Bounded Context: F5 `ai-review` · Cập nhật: 2026-10-23.

## 1. Quy ước lớp kiểm

**Bảng 1: Các lớp kiểm**

| Lớp | Vị trí | Ví dụ |
| :--- | :--- | :--- |
| Presentation | controller, `@Valid`; lỗi định dạng trả `400` mã `AIR-Vnn` | độ dài, enum, khoảng số |
| Application | use case; quy tắc nghiệp vụ trả `403`, `404`, `409`, `422`, `429`, `502`, `503` mã `AIR-nnn` | sở hữu, trạng thái, ngân sách |
| Domain | quy tắc bất biến của aggregate và của `PromptComposer` | ba lớp instruction, trọng số rubric |
| DB | `CHECK`, unique, partial unique | `score` 0 đến 10, một `ACTIVE` mỗi feature |

Quy tắc chung: kiểm ở máy chủ là kiểm thật; schema zod phía giao diện phải khớp file này (anti-drift 3 của `dd-generation`). **Mọi chuỗi người dùng nhập là dữ liệu**, không bao giờ diễn giải như chỉ thị: không lọc từ khoá, không "làm sạch" nội dung câu trả lời hay mã nguồn (bộ lọc từ khoá dễ vượt qua; phòng thủ là ranh giới kiểu và transport, `ARCH5:229-230`); chỉ chặn độ dài.

## 2. Đầu vào người học (endpoint 4 đến 13)

**Bảng 2: Trường của luồng người học**

| Trường | Quy tắc | Mã lỗi | HTTP | Lớp | Nguồn |
| :--- | :--- | :--- | :-: | :--- | :--- |
| `submissionId` (path, mục 4) | UUID hợp lệ; sai định dạng coi như không tồn tại | `AIR-101` | 404 | Application | `U301` Sheet 9 |
| Bài nộp `ACCEPTED` | `status = ACCEPTED` (qua `SubmissionSnapshotPort`) | `AIR-102` | 422 | Application | `ARCH5:79-80`, `U301` Sheet 9 NO 3 |
| Chủ bài nộp, chủ phiên, chủ báo cáo | `userId` trong dữ liệu bằng `userId` trong token; kiểm trong truy vấn hoặc ngay sau khi đọc | `AIR-302` | 403 | Application | `SEC5:30-34` |
| `problemId` (query, mục 6) | UUID | `AIR-V01` | 400 | Presentation | |
| `limit` (mục 7, 13) | số nguyên 1 đến 10; mục 7 mặc định 3, mục 13 mặc định 5; mục 30, 31: 1 đến 20 mặc định 5 | `AIR-V02` | 400 | Presentation | `[SoT: Suy luận]`, BD không nêu trần |
| `range` (mục 13) | `7d`, `30d`, `all`; mặc định `all`; giá trị lạ **bị từ chối** (khác `judge`, không tự đưa về `all`) | `AIR-V10` | 400 | Presentation | `[SoT: Suy luận]` |
| `sessionId` (path) | UUID; sai định dạng coi như không tồn tại | `AIR-110` | 404 | Application | |

## 3. Kết quả LLM của Solution Review (lược đồ `result_json`, `DB5` mục 1.3a)

Kiểm sau khi nhận kết quả, **trước** khi ghi DB. Sai ở mục "từ chối" thì `502 AIR-502`, không ghi `solution_reviews`; mục "chuẩn hoá" thì sửa lặng lẽ, không lỗi (tránh bắt người học trả giá cho lỗi nhỏ của AI, `[SoT: Suy luận]`).

**Bảng 3: Kiểm kết quả F5.1**

| Khoá | Quy tắc | Xử lý khi sai |
| :--- | :--- | :--- |
| `approachTitle` | chuỗi không rỗng | từ chối nếu rỗng hoặc thiếu; cắt về 60 ký tự nếu dài hơn |
| `verdict` | một trong `OPTIMAL`, `IMPROVABLE`, `NOT_OPTIMAL` | từ chối |
| `isApproachOptimal` | không tin giá trị AI | luôn ghi đè bằng `verdict = OPTIMAL` (`DB5:68`) |
| `complexity.actualTime`, `actualSpace` | chuỗi không rỗng | từ chối nếu thiếu; cắt về 30 ký tự |
| `complexity.timeExplanation`, `spaceExplanation` | chuỗi không rỗng | từ chối nếu thiếu; cắt về 500 ký tự |
| `complexity.optimalTime`, `optimalSpace` | **máy chủ ghi `null`** khi `ProblemContext.knownOptimalComplexity` NULL (luôn vậy ở bản này); nếu không NULL thì cắt về 30 ký tự | chuẩn hoá |
| `strengths`, `improvements`, `edgeCases` | mảng chuỗi (có thể rỗng), tối đa 10 phần tử, mỗi phần tử cắt về 2.000 ký tự | thiếu khoá: coi là mảng rỗng; kiểu sai: từ chối |
| `followUpTopics` | mảng chuỗi (có thể rỗng), tối đa 10 phần tử, mỗi phần tử cắt về 100 ký tự; **không** nhận id câu hỏi (không kiểm, vì chỉ là chuỗi) | như trên |
| `readabilityScore` | số nguyên 1 đến 5 | từ chối nếu ngoài khoảng hoặc không phải số nguyên; CHECK DB `SMALLINT` |
| `codeDiff` | `null` khi `verdict = OPTIMAL` (máy chủ ép `null`); nếu có thì cả `suggestedCode` (tối đa 50.000 ký tự, không rỗng) lẫn `explanation` (tối đa 1.000, cắt) cùng có mặt | thiếu một trong hai khi `verdict` khác `OPTIMAL`: `codeDiff = null` (không từ chối); `suggestedCode` quá 50.000 ký tự: `codeDiff = null` |
| `schemaVersion` | **không** do AI sinh; máy chủ ghi `response_schema_version` của bản prompt | luôn ghi đè |
| Khoá lạ, `criteriaScores`, `summaryText` | bị bỏ, không lưu (`DB5:82-88`) | chuẩn hoá |

Các giới hạn "tối đa 10 phần tử" là `[SoT: Suy luận]`: Sheet 5 của `USR0301` chỉ nêu độ dài từng phần tử, không nêu số phần tử.

## 4. Mở phiên (endpoint 8)

**Bảng 4: Trường của `StartInterviewSession`**

| Trường | Quy tắc | Mã lỗi | HTTP | Lớp | Nguồn |
| :--- | :--- | :--- | :-: | :--- | :--- |
| `entryType` | một trong `SUBMISSION`, `QUESTION_BANK`, `TOPIC_SELECTION` | `AIR-V03` | 400 | Presentation | `DB5:107` |
| Tập trường theo `entryType` | `SUBMISSION`: bắt buộc `submissionId`, **cấm** `questionRef`, `questionText`, `topics`. `QUESTION_BANK`: bắt buộc `questionRef` và `questionText`, cấm `submissionId`, `topics`. `TOPIC_SELECTION`: bắt buộc `topics`, cấm `submissionId`, `questionRef`, `questionText` | `AIR-V03` | 400 | Presentation | `DB5:122-124`, `ARCH5:202-208` |
| `interviewerLevel` | `INTERN`, `JUNIOR`, `MIDDLE`, `SENIOR`; bắt buộc với hai lối tự luyện; với `SUBMISSION` **bỏ qua và lưu NULL** | `AIR-V04` | 400 | Presentation | `DB5:111`, F5-28 |
| `maxTurns` | `8`, `12`, `16`; bắt buộc với hai lối tự luyện; với `SUBMISSION` bỏ qua, lưu NULL | `AIR-V05` | 400 | Presentation | `U302` EVT-8 dòng 405; `03-dd/validation/F1_identity.md:81` |
| `hintAllowed` | boolean; bắt buộc với hai lối tự luyện; với `SUBMISSION` bỏ qua, lưu NULL | `AIR-V03` | 400 | Presentation | F5-28 |
| `topics` | mảng 1 đến 10 phần tử; mỗi phần tử sau khi cắt khoảng trắng 1 đến 60 ký tự; không trùng nhau (không phân biệt hoa thường; trùng thì gộp, không lỗi) | `AIR-V06` | 400 | Presentation | Trần 60 khớp tên chủ đề F6 (`03-dd/validation/F6_interview-bank.md:95`), `DEC-2026-1024-dd-screens-ai-topic-length-and-rd-trim` (nâng từ 50 `[SoT: Suy luận]`) |
| `questionRef` | chuỗi UUID 36 ký tự (là `interview_questions.id`, không phải `IQ-nnn`) | `AIR-V07` | 400 | Presentation | `DB5:109` |
| `questionText` | 1 đến 2.000 ký tự sau khi cắt khoảng trắng; lưu nguyên văn vào `interview_sessions.question_text` (chỉ `QUESTION_BANK`, thêm 2026-10-29, F5-07) và trong bộ nhớ hội thoại; không bao giờ là chỉ thị | `AIR-V07` | 400 | Presentation | `[SoT: Suy luận]` |
| Ràng buộc chéo cột | `SUBMISSION` thì `submission_id` có và ba cột F5-28 NULL; hai lối kia thì `submission_id` NULL | | | Application (không CHECK đơn giản) | `DB5:122-124` |

## 5. Lượt hội thoại, gợi ý, kết thúc (endpoint 10, 11, 12)

**Bảng 5: Trường của luồng phiên đang chạy**

| Trường | Quy tắc | Mã lỗi | HTTP | Lớp | Nguồn |
| :--- | :--- | :--- | :-: | :--- | :--- |
| `content` | sau khi bỏ khoảng trắng đầu cuối còn ít nhất 1 ký tự; `String.length() <= 4000` (đo như `MESSAGE_MAX_LENGTH` của giao diện); không cắt, không chuẩn hoá, không lọc ký tự, không lọc từ khoá | `AIR-V08` | 400 | Presentation | `SEC5:12-16`, `U302:169` |
| `expectedTurnCount` | số nguyên không âm, **bắt buộc** | `AIR-V09` | 400 | Presentation | file API mục 2.4 |
| `expectedTurnCount` khớp | bằng `interview_sessions.turn_count` hiện tại | `AIR-112` | 409 | Application | |
| Phiên đang chạy | `stage IN ('EXPLAIN','CHALLENGE','SCALE_UP')` | `AIR-111` | 409 | Application, và điều kiện `UPDATE` ở DB | `DB5:114` |
| Một lượt mỗi phiên | khoá tư vấn theo `sessionId` | `AIR-113` | 409 | Application | |
| Chưa đủ lượt tối đa | `turn_count < max_turns` (khi có) và chưa đạt trần giai đoạn cuối | `AIR-114` | 409 | Application | `U302` EVT-14 bước 7 |
| `hint_allowed` | phải là `true` (NULL của `SUBMISSION` coi là không) | `AIR-115` | 422 | Application | F5-28 |
| Có thể chấm | `turn_count >= 1` | `AIR-116` | 422 | Application | Q4 |
| `turn_count <= max_turns` | bất biến | | | Domain | `DB5:115` |
| `turn_index` | duy nhất theo phiên | | | DB unique `(interview_session_id, turn_index)` | `DB5:140` |
| `stage` | enum năm giá trị; `ABANDONED` và `COMPLETED` là cuối, **không bao giờ bị ghi đè**: mọi `UPDATE stage` kèm `AND stage IN ('EXPLAIN','CHALLENGE','SCALE_UP')` | `AIR-111` | 409 | Domain và DB | `JOB5` mục 4 |

**Lược đồ phản hồi một lượt của AI** (`message`, `stageComplete`; BR-AI-07): `message` chuỗi không rỗng sau khi cắt khoảng trắng, `stageComplete` boolean thật (không nhận chuỗi `"true"`); thiếu hoặc sai thì `AIR-502` (Application). `message` không bị cắt độ dài ở máy chủ (trần do `maxTokens` của feature).

**Lược đồ kết quả chấm rubric** (BR-AI-09):

| Khoá | Quy tắc | Xử lý khi sai |
| :--- | :--- | :--- |
| `criteria` | đúng 4 phần tử | `AIR-502` |
| `criteria[].code` | đủ bốn mã `CLARITY`, `TECHNICAL_ACCURACY`, `PUSHBACK_HANDLING`, `COMPLEXITY_AWARENESS`, mỗi mã đúng một lần | `AIR-502` |
| `criteria[].score` | số trong `[0, 10]`; lấy 2 chữ số thập phân (làm tròn nửa lên); ngoài khoảng thì **không cắt về biên**, mà từ chối | `AIR-502`; DB: `DECIMAL(4,2)`, ứng dụng kiểm `0.00` đến `10.00` (`DB5:157`) |
| `criteria[].comment` | không rỗng sau khi cắt khoảng trắng (F5-15 bắt buộc); cắt về 1.000 ký tự `[SoT: Suy luận]` | rỗng: `AIR-502` |
| Unique | `(owner_type, owner_id, criterion_code)` | DB |

## 6. Kết quả sinh script testcase (cổng P3)

| Khoá | Quy tắc | Xử lý khi sai |
| :--- | :--- | :--- |
| `generatorScript`, `validatorScript` | chuỗi không rỗng | `AIR-502` |
| `categoryLabels` | mảng chuỗi (có thể rỗng) | thiếu: mảng rỗng; kiểu sai: `AIR-502` |
| `GenerationInput` | đúng 2 Sample, không có trường đáp án mẫu, `ai_brief`, testcase ẩn (kiểu không có trường đó) | kiểm ở `problem-bank`; ở đây không nhận trường lạ |

Script là mã **không tin cậy**: `ai-review` không thực thi, không phân tích nội dung (`DD-F2:2795`).

## 7. Quản trị prompt (endpoint 14 đến 17)

**Bảng 6: Trường của cấu hình prompt**

| Trường | Quy tắc | Mã lỗi | HTTP | Lớp | Nguồn |
| :--- | :--- | :--- | :-: | :--- | :--- |
| `featureCode` | `SOLUTION_REVIEW`, `MOCK_INTERVIEW`, `GENERATE_TESTCASE` | `AIR-V11` | 400 | Presentation | `DB5:19` |
| `systemInstruction` | sau khi bỏ khoảng trắng đầu cuối không rỗng; tối đa 20.000 ký tự | `AIR-V12` | 400 | Presentation | `A301` Sheet 9 NO 5; trần `[SoT: Suy luận]` |
| `modelId` | thuộc `ai.models.allowed`, so khớp **chính xác**, tối đa 50 ký tự; máy chủ không tin giá trị từ client | `AIR-202` | 422 | Application | `SEC5:88`, `DB5:24` |
| `responseGuards` | `GENERATE_TESTCASE`: bắt buộc `null`; hai feature kia: object đúng ba khoá boolean `citeLines`, `answerInVietnamese`, `noFullSolution`, **không khoá lạ**; `noFullSolution` nhận cả `true` lẫn `false` (không ép) | `AIR-V13` | 400 | Presentation | `DB5:25`, `DEC-2026-1011-ai-no-full-solution-can-be-turned-off` |
| `changes[]` | 1 đến 3 phần tử, không trùng `featureCode` | `AIR-V14` | 400 | Presentation | `A301` EVT-13 |
| `expectedActivePromptId` | UUID, khớp id `ACTIVE` hiện tại | `AIR-201` | 409 | Application, dưới `FOR UPDATE` | `A301` Sheet 9 NO 6 |
| Có thay đổi thật | ít nhất một trường khác `ACTIVE` | `AIR-205` | 422 | Application | `[SoT: Suy luận]` |
| Một `ACTIVE` mỗi feature | partial unique `(feature_code) WHERE status = 'ACTIVE'` | `AIR-201` khi vi phạm do cuộc đua | 409 | DB | `DB5:275` |
| `version` | `v<số>.<số>`, tối đa 20 ký tự, unique `(feature_code, version)`; do máy chủ sinh, **không nhận từ client** | | | DB, Domain | `DB5:20` |
| Quyền | `AI_CONFIG:READ` (14, 15), `AI_CONFIG:UPDATE` (16, 17) và `baseCategory = ADMIN`; `AI_TOKEN_BUDGET` **không** thay thế | `AIR-301` | 403 | Application (`@PreAuthorize` độc lập) | `SEC5:68-70` |

Câu chống chèn và câu theo `response_guards` là **văn bản cố định của máy chủ**, không phải trường nhập: không có chỗ nào để admin sửa hay tắt câu chống chèn (`ARCH5:218`, `A301` dòng 442).

## 8. Ngân sách, giới hạn, đơn giá (endpoint 18 đến 26)

**Bảng 7: Trường cấu hình chi phí**

| Trường | Quy tắc | Mã lỗi | HTTP | Lớp | Nguồn |
| :--- | :--- | :--- | :-: | :--- | :--- |
| `limitTokens` | số nguyên dương, tối đa 9.000.000.000.000; `0` chỉ là giá trị seed "chưa cấu hình", **không** nhận qua API | `AIR-V15` | 400 | Presentation | `A303` Sheet 9 NO 2; trần `[SoT: Suy luận]` |
| Mở khoá | chỉ khi `usedTokens < limitTokens` | `AIR-203` | 422 | Application | `A303` Sheet 9 NO 3 |
| `reviewPerHour`, `interviewPerHour`, `cooldownSeconds` | số nguyên 1 đến 999; DB `CHECK` cùng khoảng | `AIR-V16` | 400 | Presentation, DB | `DB5:214-216`, `A303` Sheet 9 NO 4 |
| `warnPercent` | số nguyên 1 đến 100; DB `CHECK` | `AIR-V16` | 400 | Presentation, DB | `DB5:213`, `A303` Sheet 9 NO 5 |
| Body `PATCH /admin/limits-config` | ít nhất một trong bốn trường; trường lạ bị bỏ | `AIR-V16` | 400 | Presentation | |
| `modelName` | sau khi cắt khoảng trắng hai đầu: 1 đến 100 ký tự | `AIR-V17` | 400 | Presentation | `DB5:229`, `A303` Sheet 9 NO 6 |
| `modelName` duy nhất | `lower(model_name)` unique; so khớp không phân biệt hoa thường | `AIR-204` | 409 | Application, DB unique | `DB5:233` |
| `usdPerMillionTokens` | số không âm, tối đa 4 chữ số thập phân, giá trị `<= 999999.9999`; DB `CHECK (>= 0)` | `AIR-V18` | 400 | Presentation, DB | `DB5:230`; giao diện tự đưa âm hoặc không phải số về 0 (`A303` Q4), máy chủ vẫn kiểm |
| `priceId`, `alertId` | UUID có thật | `AIR-121`, `AIR-120` | 404 | Application | |
| `rangeDays` | `7`, `14`, `30`; mặc định `14` | `AIR-V19` | 400 | Presentation | `A302` EVT-1 |
| Quyền ngân sách | `AI_TOKEN_BUDGET:UPDATE` cho 19, 20, 29; `AI_TOKEN_BUDGET:READ` cho 27 đến 31; 18 và 21 chấp nhận `AI_TOKEN_BUDGET:READ` hoặc `AI_CONFIG:READ`; 22 đến 26 đòi `AI_CONFIG:UPDATE` (23 đòi `AI_CONFIG:READ`); mọi endpoint kèm `baseCategory = ADMIN` | `AIR-301` | 403 | Application | `SEC5:68-75` |
| Singleton | `ai_token_budget_configs.id = 1` (`CHECK (id = 1)`) | | | DB | `DB5:208` |

## 9. Quota, ngân sách và hạn chờ (luồng sinh nội dung)

**Bảng 8: Quy tắc kiểm trước khi gọi LLM** (thứ tự thực thi ở BR-AI-04)

| Quy tắc | Điều kiện từ chối | Mã lỗi | HTTP | Lớp | Nguồn |
| :--- | :--- | :--- | :-: | :--- | :--- |
| Ngân sách khoá | **chỉ khi tạo báo cáo F5.1 mới (cache trượt) hoặc tạo phiên mới** (cổng P1 cho F6-08 cũng áp); **không** kiểm ở lượt, gợi ý, kết thúc, chấm điểm của phiên đã mở, sinh script testcase, job quét (chủ dự án chốt Q2, Q3). Từ chối khi `baseCategory` thuộc `STUDENT`, `INSTRUCTOR` và (`locked_at` khác null hoặc `usedTokens >= limit_tokens > 0`); `ADMIN` miễn | `AIR-401` | 429 | Application (trước `LlmProviderPort`) | `ARCH5:269-271`, `SEC5:63-66` |
| Thời gian chờ | `SET ai:cooldown:<userId> NX EX <cooldown_seconds>` thất bại; chỉ tạo báo cáo mới và tạo phiên | `AIR-404` | 429 | Application, Redis nguyên tử | `DB5:293` |
| Hạn mức giờ F5.1 | `INCR ai:ratelimit:<userId>:SOLUTION_REVIEW:<epochHour>` lớn hơn `review_per_hour`; cache hit và báo cáo đã có **không đếm** | `AIR-402` | 429 | Application, Redis nguyên tử | `DB5:292`, `ARCH5:246` |
| Hạn mức giờ F5.2 | `INCR ai:ratelimit:<userId>:MOCK_INTERVIEW:<epochHour>` lớn hơn `interview_per_hour`; chỉ đếm khi **tạo phiên** | `AIR-403` | 429 | Application, Redis nguyên tử | `ARCH5:247-249` |
| Redis lỗi | coi hai kiểm trên là qua (fail-open), ghi `WARN` | | | Application | BR-AI-04 bước 9 |
| Lời gọi LLM quá hạn hoặc lỗi | hạn chờ từng luồng (file API Bảng 4); không có bộ ngắt mạch (chủ dự án chốt Q6); hoàn lại hạn mức giờ và khoá chờ | `AIR-501` | 503 | Application | `DEC-2026-1023-dd-f5-budget-lock-and-breaker` |

Khoá Redis: `epochHour = floor(epochSecond / 3600)` UTC; khoá hạn mức hết hạn sau 3.600 giây đặt ở lần đếm đầu; khoá chờ hết hạn sau `cooldown_seconds`. Khoá `GENERATE_TESTCASE` không có hạn mức giờ (`DEC-2026-1010-ai-generate-testcase-feature-code`).

## 10. Chống prompt injection, dữ liệu không vào prompt

**Bảng 9: Quy tắc bảo mật dựng prompt** (kiểm ở `PromptComposer`, domain; mỗi dòng có kiểm thử đơn vị)

| Quy tắc | Lớp | Nguồn |
| :--- | :--- | :--- |
| `UserData` không có constructor nhận `SystemInstruction`; `SystemInstruction` không có hàm nhận chuỗi tuỳ ý từ `SubmissionSnapshot`, `ProblemContext`, request | Domain (kiểu) | `ARCH5:118-122` |
| Một system message và một user message mỗi lời gọi | Domain | `ARCH5:121` |
| Mã nguồn, câu trả lời, `questionText`, `topics`, thông tin bài toán, hội thoại: chỉ ở user message, mỗi khối bọc thẻ có nonce ngẫu nhiên mỗi lời gọi | Domain | `[SoT: Suy luận]` (nonce) |
| `aiBrief`: thay `<` thành `&lt;`, `>` thành `&gt;` trước khi bọc `<per_problem_context>`; đặt sau Lớp 1; câu ràng buộc cố định ngay trước khối | Domain | `ARCH5:218` |
| Không có `testcase`, `expectedOutput`, `actualOutput`, `diff`, `stderr` trong `SubmissionSnapshot`, `ProblemContext`, `GenerationInput` (kiểu không có trường) | Domain | `README.md` F2; `dd-generation` |
| `<session_state>` chỉ gồm enum và số do máy chủ tính | Domain | BR-AI-01 bước 3 |
| Độ dài: `content` 4.000 ký tự, `questionText` 2.000, mã nguồn theo giới hạn của F4 (65.536 byte, không kiểm lại), `aiBrief` theo `problem-bank` (4.000) | Presentation, Application | `SEC5:12-16`, `DD-F2:2918` |
| Không ghi nguyên văn mã nguồn, câu trả lời ở mức log `INFO` trở lên; `aiBrief` ghi tách riêng, chỉ `DEBUG`; khoá API không vào log, `token_usage`, `outbox_events` | Application | `SEC5:17-18,89-91` |
| Không có endpoint nào của `ai-review` ghi `problems.ai_brief` | Application | `SEC5:19-23` |

## 11. Dữ liệu lưu (kiểu cột và ràng buộc DB, tham chiếu)

Các ràng buộc DB là chốt chặn cuối; DD không đổi schema.

| Bảng, cột | Ràng buộc | Nguồn |
| :--- | :--- | :--- |
| `solution_reviews.submission_id` | unique | `DB5:97` |
| `solution_reviews.readability_score` | `SMALLINT`, ứng dụng kiểm 1 đến 5 | `DB5:52` |
| `solution_reviews.served_from_cache` | `BOOLEAN NOT NULL DEFAULT false` | `DB5:53` |
| `ai_cache_entries` | unique `(problem_id, language, source_hash, prompt_template_id)`; dòng cache hit **không** tạo dòng cache mới | `DB5:200-202` |
| `interview_sessions.stage` | enum 5 giá trị; index `(stage, updated_at)` | `DB5:114,120` |
| `interview_turns` | unique `(interview_session_id, turn_index)`; `role` `USER` hoặc `AI`; `stage_at_turn` thuộc `EXPLAIN`, `CHALLENGE`, `SCALE_UP` | `DB5:134-140` |
| `rubric_scores` | unique `(owner_type, owner_id, criterion_code)`; `score DECIMAL(4,2)` trong `[0,10]` | `DB5:157,161` |
| `token_usage` | `feature_code` bốn giá trị; `model_name VARCHAR(100) NOT NULL`; `problem_id` nullable | `DB5:177-182` |
| `ai_token_budget_configs` | `CHECK (id = 1)`; `CHECK` ba giới hạn 1 đến 999; `CHECK (warn_percent BETWEEN 1 AND 100)` | `DB5:208-216` |
| `ai_usage_anomaly_alerts.status` | `OPEN`, `REVIEWED`; một `OPEN` mỗi user kiểm ở ứng dụng (không unique cứng) | `DB5:245,248` |
| `outbox_events` | index một phần `(published_at) WHERE published_at IS NULL` | `DB5:269` |

## 12. Sự kiện phát ra

| Quy tắc | Lớp | Nguồn |
| :--- | :--- | :--- |
| Dòng outbox ghi cùng giao dịch với dòng nghiệp vụ; không dòng nào khi `ABANDONED` | Application | `DB5:265`, `ARCH5:188` |
| Payload không chứa mã nguồn, hội thoại, chỉ thị; `schemaVersion = 1`; `eventId` = `outbox_events.id` | Application | file API mục 33 |
