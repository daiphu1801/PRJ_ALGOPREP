# F4 D02q — Đọc kết quả và lịch sử

Các API dưới base `/api/v1/judge` dùng profile backend hiện có, PostgreSQL đã migrate và Bearer JWT hợp lệ. Không cần bật worker/outbox/maintenance chỉ để đọc dữ liệu đã lưu. Để tạo dữ liệu qua luồng chấm thật, dùng các runbook worker/relay tương ứng.

| API | Người gọi |
| :--- | :--- |
| GET `/problems/{problemId}/submissions/mine?limit=10` | Học viên, tối đa 10 lượt của chính mình |
| GET `/submissions/{submissionId}` | Học viên, chỉ chủ bài nộp |
| GET `/submissions/{submissionId}/progress` | Học viên, ảnh chụp khôi phục tiến độ |
| GET `/me/submissions?q=&verdict=ALL&language=ALL&page=1` | Học viên, trang 20 dòng |
| GET `/instructor/submissions?classId=&result=all&q=&page=1` | CLASS_MANAGEMENT:READ, phạm vi lớp phụ trách |
| GET `/instructor/submissions/{submissionId}` | Cùng quyền/phạm vi; query breadcrumb không cấp quyền |
| GET `/instructor/classes/{classId}/students/{studentId}/submissions?page=1` | Cùng quyền, lịch sử học viên trong lớp cố định |

Không gửi `classId=` rỗng khi muốn xem tất cả lớp: bỏ tham số đó. Danh tính học viên lấy từ JWT; `userId` trong query không chọn được người khác.

Khi reconnect, client đọc `/progress` rồi áp các bản tin STOMP theo quy tắc `seq` ở DD F4 §2.4. Snapshot có `testcases` chỉ gồm `{order, verdict}`. Sequence cuối vẫn là `3+N` khi đã biết N, kể cả lỗi hệ thống giữa chừng. Việc nối UI/polling/browser còn thuộc task FE.

Chi tiết trả Sample khi cả cờ lúc chấm và visibility hiện tại đều Sample. Lookup visibility lỗi thì Hidden; summary bài toán lỗi thì `problem=null`. Stderr còn cần Standard I/O, cờ admin hiện tại bật và testcase khác AC. Không trả input/expected output. Quá 90 ngày ẩn chi tiết dù cleanup chưa chạy; nguồn và tổng kết vẫn ở submission.

`JDG-110/111/112`: 404 không có hoặc ngoài phạm vi; `JDG-301`: 403 thiếu quyền; `JDG-V14`: 400 limit không hợp lệ. `JDG-506`: 503 lỗi đọc dữ liệu bắt buộc, thử lại GET khi hạ tầng phục hồi. Không tự tạo lại submission từ lỗi đọc.

Tham chiếu: [Plan](F:/prj_algoprep/06-plan/261104-f4-submission-reads.md), DD API F4 §7–10/13–15 và decision `DEC-2026-1104-f4-submission-read-unavailable`.
