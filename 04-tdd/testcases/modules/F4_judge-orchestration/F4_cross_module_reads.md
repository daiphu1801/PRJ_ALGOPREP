# Test case QA — F4: cổng đọc cho module khác và danh sách người học

> 11 ca thiết kế cho AC-66–72; chưa thực thi. Nguồn: TDD F4 AC-66–72; DD F4 API §8, §24A/24B, §26.2–26.4; logic BR-JD-09 và F1 BR-17. Cổng P2/P3/P4 kiểm trong tiến trình; API kiểm JSON thô và màn liên quan. Mỗi ca có fixture cô lập.

### TC-F4-AC66-01 — Giới hạn generator đọc giá trị mới qua cổng P3

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-66; DD F4 API §26.3 P3; BR-JD-09 |
| Kind / priority | Integration, P1: không giữ cấu hình cũ |
| Environment | JudgeConfigReadPort, DB judge, F2 GenerateTestcasesWithAi |
| Preconditions | `generator_sandbox_settings` có `max_runtime_seconds=45`, `max_output_mb=200`; F2 chỉ nối qua P3 |
| Test data | Gọi `getGeneratorSandboxLimits()`, sau admin PUT đổi runtime thành 60, gọi lại |
| Steps | Gọi hai lần qua P3, đối chiếu DB và trace truy cập F2. |
| Expected | Lần đầu `{maxRuntimeSeconds:45,maxOutputMb:200}`, lần sau `{60,200}` ngay, không restart; F2 không truy vấn trực tiếp schema judge. Hai lời gọi chỉ đọc và không lộ thông số khác. |
| Cleanup | Khôi phục 45/200 |
| Evidence | Hai output cổng, DB/audit config, query trace |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC66-02 — P3 trả defaults, ngôn ngữ và hệ số đúng dòng

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-66; DD F4 API §26.3 P3 |
| Kind / priority | Normal + abnormal, P1: cổng F2 không dùng giá trị sai |
| Environment | P3, DB configs |
| Preconditions | JAVA enabled, hệ số time/memory 2.00/1.50; PYTHON disabled; gradingDefaults có bốn giá trị khác 0 |
| Test data | `getEnabledLanguages`, `getGradingDefaults`, `getLanguageMultipliers(JAVA)`, rồi `getLanguageMultipliers(RUST)` |
| Steps | Gọi bốn thao tác qua P3, đối chiếu cột DB. |
| Expected | Danh sách enabled không có PYTHON; defaults đúng bốn giá trị DB; JAVA trả `{2.00,1.50}`; RUST trả lỗi cổng ngôn ngữ lạ, không thay bằng defaults hoặc hệ số của ngôn ngữ khác. Không ghi DB. |
| Cleanup | Khôi phục config |
| Evidence | Inputs/outputs cổng, DB rows |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC67-01 — P2 thống kê năm verdict trên mẫu cuối hợp lệ

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-67; DD F4 API §26.2 P2; F1 logic BR-17 |
| Kind / priority | Normal + degraded, P1: biểu đồ không đếm trạng thái sai |
| Environment | JudgeStatsPort P2, DB judge, F1 ADM0101 |
| Preconditions | Có 2 ACCEPTED, 1 WRONG_ANSWER, 1 TIME_LIMIT_EXCEEDED, 1 RUNTIME_ERROR, 1 COMPILE_ERROR, 1 SYSTEM_ERROR, 1 PENDING, 1 JUDGING; timestamp trải qua hai ngày UTC+7. F1 các khối khác hoạt động. |
| Test data | Gọi P2, tải ADM0101; sau đó cưỡng bức P2 không trả lời |
| Steps | Đối chiếu `verdictCounts`, `totalFinal`, perDay/perMonth và màn; rồi kiểm màn khi P2 lỗi. |
| Expected | Năm lát verdict là 2/1/1/1/1, `totalFinal=6` cho mẫu biểu đồ; SYSTEM_ERROR không có lát riêng, PENDING/JUDGING không tính. `totalSubmissions=9` đếm mọi dòng; perDay/perMonth chia theo UTC+7. Khi P2 lỗi, F1 trả phần số liệu này `null` kèm `unavailable`, các khối tổng quan khác còn hiển thị, không hiện số 0 giả. |
| Cleanup | Dọn submissions fixture, bỏ lỗi cổng |
| Evidence | DB rows, P2 output, F1 response, ảnh màn trước/sau |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC68-01 — AI ngừng hoạt động không chặn luồng chấm

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-68; DD F4 logic BR-JD-05/14; API §6, §8, §13, queue snapshot |
| Kind / priority | Degraded + E2E, P0: lỗi AI không lan sang chấm |
| Environment | F4/F2/F1/FE, AI service cưỡng bức 503 hoặc hết quota, RabbitMQ/DB hoạt động |
| Preconditions | P-X xuất bản, học viên A có mã đúng; G có lớp với P-X đã giao; admin có quyền queue monitor |
| Test data | AI service 503 suốt vòng nộp `S-A` |
| Steps | A nộp, theo dõi progress, mở chi tiết; G mở danh sách; admin mở queue; kiểm Outbox sau ACCEPTED. |
| Expected | Nộp nhận PENDING, chấm tới ACCEPTED và chi tiết đúng; G thấy dòng bài nộp, admin thấy queue; không API/màn nào trong các luồng này trả lỗi AI. `SubmissionAccepted` vẫn được ghi/phát theo hợp đồng Outbox; yêu cầu phân tích/phỏng vấn là luồng riêng và có thể lỗi mà không đổi status/score của S-A. |
| Cleanup | Khôi phục AI, dọn fixture |
| Evidence | Timeline API/UI, DB submission, Outbox, log lỗi AI |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC69-01 — Chỉ Accepted của chủ, mới nhất trước và không lộ nguồn

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-69; DD F4 API §24A.3/24A.7 T-AS-01/02/06 |
| Kind / priority | Normal + security, P0: không lộ bài của người khác |
| Environment | GET `/me/accepted-submissions`, DB, FE USR0302 |
| Preconditions | A có 12 lượt trong 12 phút, đúng 5 ACCEPTED trên 3 bài (một bài Accepted hai lần), 7 lượt trạng thái khác; B có 4 ACCEPTED. Một bài của A sau đó bị rút khỏi ngân hàng. |
| Test data | GET mặc định bằng JWT A, thêm query `userId=B` |
| Steps | Gọi hai URL, so ID/timestamp và payload; mở tab Bài nộp Accepted. |
| Expected | Hai response chỉ có 5 lượt Accepted của A, mới nhất trước, mỗi dòng có submissionId/problemId/problem metadata/language/runtime/memory/submittedAt; bài rút vẫn có mặt. Không có lượt B hay status khác; không có sourceCode/testcase/input/expected trong toàn JSON. Query userId không đổi chủ vì chỉ lấy từ token. |
| Cleanup | Dọn fixture |
| Evidence | DB IDs, hai response, ảnh tab |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC69-02 — Empty, limit và summary lỗi

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-69; DD F4 API §24A.2/24A.7 T-AS-03–05 |
| Kind / priority | Boundary + degraded, P1 |
| Environment | API #23, ProblemSummaryReadPort có thể lỗi |
| Preconditions | A chưa có Accepted; B có hai Accepted; riêng bước cuối A có một Accepted của P-X |
| Test data | A gọi `limit=0`, `limit=21`, `limit=1`, rồi cổng summary lỗi khi A có Accepted |
| Steps | Gọi bốn request, kiểm status/items và DB write count. |
| Expected | 0 và 21 trả `400 JDG-V14`; khi rỗng `limit=1` trả `200 items=[]`, không báo lỗi; khi summary lỗi vẫn 200 với một item A và `problem=null`, các trường submission khác đủ. Không ghi DB. |
| Cleanup | Khôi phục cổng, dọn fixture |
| Evidence | Bốn response và fault injection |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC70-01 — P4 chỉ trả snapshot Accepted, không có testcase

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-70; DD F4 API §26.4 P4; F5 API §1.3 |
| Kind / priority | Normal + security, P0: không mở đường Hidden cho AI |
| Environment | SubmissionSnapshotPort P4, DB judge, F5 caller |
| Preconditions | A có submission SA `ACCEPTED` P-X/JAVA, source `SOURCE_A`, runtime 22ms, memory 1024KB; P-X có Hidden chứa marker `HIDDEN_70` |
| Test data | `getAccepted(SA)` |
| Steps | Gọi cổng qua F5, kiểm object và truy vấn DB. |
| Expected | Trả đúng `{submissionId:SA,userId:A,problemId:P-X,language:JAVA,sourceCode:SOURCE_A,runtimeMs:22,memoryKb:1024}`; không có testcaseId/input/expected/output/verdict từng ca hoặc marker Hidden. Cổng chỉ đọc submissions theo khoá chính, không kiểm chủ ở đây; F5 dùng `userId` để gác quyền. |
| Cleanup | Dọn fixture |
| Evidence | P4 output, DB row, F5 auth trace |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC70-02 — P4 phân biệt chưa Accepted và không tồn tại

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-70; DD F4 API §26.4 P4 |
| Kind / priority | Abnormal, P1: F5 ánh xạ đúng lỗi |
| Environment | P4, F5 caller, DB |
| Preconditions | SA ACCEPTED, SW WRONG_ANSWER, SC COMPILE_ERROR; UUID SX không tồn tại |
| Test data | `getAccepted(SW)`, `(SC)`, `(SX)` |
| Steps | Gọi ba lần qua cổng, xem lỗi F5 ánh xạ. |
| Expected | SW/SC trả `SubmissionNotAccepted` để F5 ánh xạ `AIR-102`; SX trả `SubmissionNotFound` để ánh xạ `AIR-101`. Không trả sourceCode hay testcase trong cả ba lỗi, không ghi DB. |
| Cleanup | Dọn fixture |
| Evidence | Inputs/outputs cổng, F5 response, DB |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC71-01 — Bài rút, bài còn xuất bản và lỗi summary

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-71; DD F4 API §8.3 T-GD-06/09 |
| Kind / priority | Degraded + normal, P1: phân biệt rút bài với dịch vụ lỗi |
| Environment | GET `/submissions/{submissionId}`, ProblemSummaryReadPort, FE USR0201 |
| Preconditions | A có SX của bài X đã ẩn mềm, SY của bài Y còn xuất bản; cả hai có source và điểm lưu; cổng summary có thể cưỡng bức lỗi |
| Test data | GET SX/SY khi bình thường, rồi GET SX khi cổng lỗi |
| Steps | Gọi ba lần, xem JSON và trang. |
| Expected | SX bình thường: `problem` object khác null, `withdrawn=true`, FE hiện “Đã rút khỏi ngân hàng”, nguồn/điểm/chi tiết vẫn có. SY: `withdrawn=false`, không hiện nhãn. Khi cổng lỗi: SX vẫn 200 với `problem=null`, kết quả/nguồn còn, FE không hiện nhãn đã rút và không có tên bài; không dùng null để suy ra withdrawn. |
| Cleanup | Khôi phục cổng, dọn fixture |
| Evidence | Ba response, ảnh màn, DB/F2 trạng thái bài |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC72-01 — Ngôn ngữ bật theo thứ tự cố định, quyền đọc hẹp

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-72; DD F4 API §24B |
| Kind / priority | Normal + security, P1 |
| Environment | GET `/languages/enabled`, GET `/admin/language-config`, DB |
| Preconditions | PYTHON/JAVA/CPP ban đầu enabled; admin tắt JAVA qua PUT #22; JWT STUDENT A, INSTRUCTOR I |
| Test data | A/I gọi #24 trước/sau tắt, A gọi #21 |
| Steps | Gọi các request, đối chiếu DB và payload. |
| Expected | Trước tắt `{items:[PYTHON,JAVA,CPP]}`; sau tắt A và I đều `{items:[PYTHON,CPP]}` đúng thứ tự. #24 không có hệ số/giới hạn/sandbox; JWT A không được #21 (`403 JDG-301`). Không ghi DB khi đọc. |
| Cleanup | Bật lại JAVA |
| Evidence | Responses, DB config, ảnh màn Cài đặt |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC72-02 — Không có JWT và lỗi kho cấu hình

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-72; DD F4 API §24B lỗi và test viewpoints |
| Kind / priority | Abnormal + degraded, P1 |
| Environment | GET #24, DB fault injection |
| Preconditions | Có ít nhất PYTHON enabled; sau request không JWT, cưỡng bức lỗi đọc `language_configs` |
| Test data | GET không Authorization; GET JWT A khi DB lỗi |
| Steps | Gọi hai request, kiểm toàn body. |
| Expected | Không JWT nhận 401 phong bì chung; lỗi DB nhận 503 phong bì chung, không trả `items=[]` gây hiểu sai là không còn ngôn ngữ. Không thay DB. |
| Cleanup | Khôi phục DB fault |
| Evidence | Hai response, fault injection trace |
| Design review | Đã review; chưa thực thi |
