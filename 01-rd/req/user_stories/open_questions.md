# Câu hỏi mở

> Tách ra từ `01-rd/req/user_stories.md` ngày 2026-08-31 để dễ đọc. Nội dung dưới đây được chuyển nguyên
> văn từ `user_stories.md` mục 5 (không đổi ý nghĩa, không thêm/bớt), chỉ hạ heading `##` xuống `#`.

Giữ nguyên tinh thần "ghi ra thay vì lặng lẽ giả định" của `rd-analysis`:

| # | Câu hỏi | Vì sao chưa trả lời được | Người trả lời |
| :--- | :--- | :--- | :--- |
| Q1 | ~~Sinh viên có sửa được câu trả lời đã nộp ở Chế độ luyện (F6-07)...~~ **ĐÃ CHỐT 2026-08-31 (owner instruction, theo đúng đề xuất):** không sửa được — mỗi lần nộp là một lượt độc lập. | Đã ghi vào `01-rd/req/interview-bank.md` (amendment F6-07). Xem `DEC-2026-0831-outside-screens-closures`. | Đã đóng |
| ~~Q2~~ | ~~Giảng viên yêu cầu chấm lại (F4-09a) có giới hạn số lần hoặc cần quản trị viên duyệt không?~~ **Đã trả lời 2026-08-24: không cần duyệt, chỉ cần audit.** Phạm vi giảng viên vốn đã bị chặn trong lớp mình phụ trách (ma trận quyền F1-10, xem `US-A2-03`); thao tác quy mô lớn (toàn hệ thống) tách riêng cho quản trị viên (`US-A3-05`) và đã có dry-run + tạm dừng/huỷ giữa chừng (F4-09b, F4-09d) làm lớp an toàn trước/trong khi chạy. Audit trail (F4-09e) chỉ cần để truy vết sau khi chạy, không cần thêm bước duyệt trước. **MOOT 2026-08-28:** toàn bộ tính năng chấm lại (F4-09a→e, `US-A3-05`) đã loại khỏi phạm vi (`DEC-2026-0828-remove-rejudge-scope`) — câu hỏi và câu trả lời này không còn áp dụng, giữ lại chỉ để lưu vết. | (đã trả lời, nay hết hiệu lực) | Chủ nhiệm đề tài |
| Q3 | ~~US-A1-06 giả định "yêu cầu phân tích" là hành động chủ động...~~ **ĐÃ CHỐT 2026-08-31 (owner instruction, theo đúng đề xuất):** chỉ hiện lựa chọn/nút bấm, không tự động. | Đã ghi vào `01-rd/req/ai-review.md` (amendment F5-01). Xem `DEC-2026-0831-outside-screens-closures`. | Đã đóng |
| Q4 | ~~Ngưỡng thời gian cụ thể để timeout sweep (F4-07)...~~ **ĐÃ CHỐT 2026-08-31 (owner instruction, theo đúng đề xuất):** 5 phút. | Đã ghi vào `01-rd/req/judge-orchestration.md` (amendment F4-07), `[SoT: Suy luận]`, BD/DD chỉnh được. Xem `DEC-2026-0831-outside-screens-closures`. | Đã đóng |

Đọc thêm: yêu cầu ở mức đặc tả kỹ thuật `01-rd/req/req.md` · từ vựng chuẩn `01-rd/overview/glossary.md` ·
bảng chức năng đầy đủ `01-rd/overview/system_survey.md` mục 5.
