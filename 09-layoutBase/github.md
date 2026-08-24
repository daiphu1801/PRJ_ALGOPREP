repo: daiphu1801/PRJ_ALGOPREP
branch: main
path: 01-rd

## Last sync
date: 2026-08-24T20:14:00Z

### Updated in this project
- Thêm 5 màn Giáo viên (Phương án B, mục 3.3): `Giáo viên - Tổng quan`, `Giáo viên - Lớp của tôi`,
  `Giáo viên - Tiến độ học viên`, `Giáo viên - Bài tập của tôi`, `Giáo viên - Chấm bài` — shell/palette
  riêng, không dùng chung Admin.
- Màn mới "Giáo viên - Chấm bài": điểm AI quy đổi thang 10 từ báo cáo F5.1 + chấm tay của giảng viên — ghi
  mã mới **F5-27** (chốt 2026-08-24, chỉ là lớp tham khảo nội bộ, không phải điểm chính thức, tách bạch khỏi
  Pass/Fail F4-04).
- Rà lại toàn bộ đợt cập nhật Admin trước đó (Chấm lại, Câu hỏi phỏng vấn, Cấu hình AI, Hàng đợi chấm, Ngôn
  ngữ và giới hạn, Người dùng, Nhật ký hệ thống, Quản lý bài tập, Soạn đề bài, Token AI) và phát hiện một
  đợt revert ngoài ý muốn (nhãn Judge0 quay lại, khoá 3 ngôn ngữ bị mở lại thành 6, `Phỏng vấn giả lập v1`
  hồi sinh, `Admin - Tổng quan` mới mặc định theme dark) — đã xác nhận là lỗi merge từ baseline cũ và sửa lại
  đúng các quyết định đã chốt trước đó (`06-plan/PROTOTYPE_DEBT.md` mục 6.1).
- Thêm màn "Admin - Ma trận phân quyền" (F1-10/11/12): tab theo vai trò, 11 chức năng × CRUD, tạo/xoá vai
  trò tuỳ chỉnh (không xoá được 3 vai trò hệ thống).
- Tách Nhật ký hệ thống (F1-14, chốt 2026-08-24): chỉ còn hành động quản trị của người, bỏ sự kiện hạ tầng.
- Sự kiện hạ tầng (worker, AI, sao lưu) chuyển sang Admin - Hàng đợi chấm.

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
| Admin - Tổng quan | `admin_overview` | system_survey.md 7.3 |
| Admin - Quản lý bài tập | `problem_authoring` | system_survey.md 5.2 (F2-01→04), 7.2/7.3 |
| Admin - Soạn đề bài | `problem_authoring` + `testcase_management` | system_survey.md 5.2 (F2-01→10, F2-14), 7.2/7.3 |
| Admin - Câu hỏi phỏng vấn | `interview_bank_management` | system_survey.md 5.6 (F6-11, F6-12), 7.3 |
| Admin - Hàng đợi chấm | `admin_queue_monitor` | system_survey.md 5.4 (F4-10), 7.3 |
| Admin - Chấm lại | `admin_rejudge` | system_survey.md 5.4 (F4-09a→e), 7.3 |
| Admin - Ngôn ngữ và giới hạn | `admin_language_config` | system_survey.md 5.4 (F4-11), 5.2 (F2-10), 7.3 |
| Admin - Cấu hình AI | `admin_ai_config` | system_survey.md 5.5 (F5-19, F5-23), 7.3 |
| Admin - Token AI | `admin_ai_usage` | system_survey.md 5.5 (F5-21, F5-25), 7.3 |
| Admin - Ma trận phân quyền | `admin_permission_matrix` | system_survey.md 5.1 (F1-10→12), 7.3 |
| Admin - Người dùng | `admin_user_management` | system_survey.md 5.1 (F1-13), 7.3 |
| Admin - Nhật ký hệ thống | `admin_system_log` | system_survey.md 5.1 (F1-14), 7.3 |
| Giáo viên - Tổng quan | `instructor_overview` | system_survey.md 7.2 |
| Giáo viên - Lớp của tôi | `class_management` | system_survey.md 5.2 (F2-12, F6-11), 7.2 |
| Giáo viên - Bài tập của tôi | `class_management` | system_survey.md 5.2 (F2-12), 7.2 (view gán bài từ ngân hàng) |
| Giáo viên - Tiến độ học viên | `class_progress` | system_survey.md 7.2 |
| Giáo viên - Chấm bài | `instructor_grading` | system_survey.md 5.5 (F5-27), 7.2 |

## Sync history
### 2026-08-24T20:14:00Z
- Xem "Updated in this project" ở trên.

### 2026-08-21T02:26:10Z
- Repo có thêm tầng `01-rd/` (overview, req, system) — vẫn chưa có source UI, `05-coding/frontend/` rỗng.
- Đối chiếu 5 trang prototype với `system_survey.md` mục 5, 6, 7 và `frontend_architecture.md`.
- Workspace đúng ràng buộc 3 ngôn ngữ (Python 3 / Java 21 / C++ 17) theo mục 8.1.

### 2026-08-20T03:44:23Z
- Đọc README / BAO_CAO_KHAO_SAT_HE_THONG.md làm đặc tả nguồn (F1–F6, actor A1–A4).
- Repo chưa có source code UI → không có screen nào để recreate.
- Dashboard người dùng cuối (A1) được thiết kế mới từ đặc tả.
