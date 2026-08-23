repo: daiphu1801/PRJ_ALGOPREP
branch: main
path: 01-rd

## Last sync
date: 2026-08-21T05:05:00Z

### Updated in this project
- Thêm 4 màn tài khoản: Trang cá nhân, Tiến độ của tôi, Bài đã lưu, Cài đặt.
- Đồng bộ menu người dùng trên toàn bộ màn (4 mục + Đăng xuất), bỏ các mục chỉ là nút giả.
- Bổ sung 3 màn của luồng nộp bài: Kết quả nộp bài (F4), Phân tích bài giải (F5.1), Bài đã nộp (F5-lịch sử).
- Nối kín luồng: Workspace → Kết quả nộp bài → Phân tích bài giải / Phỏng vấn giả lập; Bài đã nộp là lối vào lại.
- Nav chính thêm mục "Bài đã nộp" trên toàn bộ 8 màn.
- Vẫn lệch phạm vi ở Phỏng vấn giả lập (F5.2) và Câu hỏi phỏng vấn (F6) — chờ chủ dự án quyết.

## Screen map
| Screen | Slug spec | Repo files |
| :--- | :--- | :--- |
| Dashboard AlgoPrep | `my_progress` | system_survey.md 5.1 (F1-06→08), 5.6 (F6-09, F6-10) |
| Ngân hàng bài toán | `problem_list` | system_survey.md 5.2 (F2-11) |
| Workspace giải bài | `problem_detail` | system_survey.md 5.2, 5.3, 5.4; frontend_architecture.md 2.B |
| Kết quả nộp bài | `submission_result` | system_survey.md 5.4 (F4-01→08) |
| Phân tích bài giải | `solution_review` | system_survey.md 5.5 F5.1 (F5-01→08) |
| Bài đã nộp | `my_submissions` | system_survey.md 5.4, 5.5 (lịch sử nộp) |
| Trang cá nhân | `profile` | system_survey.md 5.1 (F1-04, F1-05) |
| Tiến độ của tôi | `my_progress` | system_survey.md 5.1 (F1-06→08) |
| Bài đã lưu | `saved_problems` | system_survey.md 5.2 (F2-12) |
| Cài đặt | `settings` | system_survey.md 5.1 (F1-05), mục 8.1 |
| Đăng nhập & Đăng ký | `auth` | system_survey.md 5.1 (F1-01, F1-02) |
| Phỏng vấn giả lập | `mock_interview` | system_survey.md 5.5 F5.2 (F5-09→16), mục 6.2 |
| Câu hỏi phỏng vấn | `interview_bank_list` + `interview_question_detail` | system_survey.md 5.6 (F6-01→10) |

## Sync history
### 2026-08-21T02:26:10Z
- Repo có thêm tầng `01-rd/` (overview, req, system) — vẫn chưa có source UI, `05-coding/frontend/` rỗng.
- Đối chiếu 5 trang prototype với `system_survey.md` mục 5, 6, 7 và `frontend_architecture.md`.
- Workspace đúng ràng buộc 3 ngôn ngữ (Python 3 / Java 21 / C++ 17) theo mục 8.1.

### 2026-08-20T03:44:23Z
- Đọc README / BAO_CAO_KHAO_SAT_HE_THONG.md làm đặc tả nguồn (F1–F6, actor A1–A4).
- Repo chưa có source code UI → không có screen nào để recreate.
- Dashboard người dùng cuối (A1) được thiết kế mới từ đặc tả.
