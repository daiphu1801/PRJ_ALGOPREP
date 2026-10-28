# Bộ class diagram AlgoPrep

Mở ảnh PNG để xem nhanh. Mỗi ảnh có file `.drawio` và `.spec.json` cùng tên ở cùng thư mục; mở `.drawio` bằng diagrams.net hoặc tiện ích Draw.io của VS Code để chỉnh sửa. [Chú giải ký hiệu](CHU_GIAI.md) dùng chung cho cả bộ.

Các lớp nghiệp vụ mới trong ảnh là **thiết kế đề xuất từ RD/BD/DD**, có ghi `[SoT: Suy luận]`, vì backend hiện chưa hiện thực đầy đủ. [Bảng truy vết](actor_coverage.csv) nối từng tiêu chí nghiệm thu tới lớp/phương thức và hình cụ thể. [Ghi chú phạm vi](SCOPE_NOTES.md) ghi nguồn ưu tiên và các chức năng đã loại.

## A1 — Sinh viên

- [Đăng ký, đăng nhập, phiên](user/class_a1_auth_session.png)
- [Đăng nhập OAuth](user/class_a1_oauth.png)
- [Quên mật khẩu](user/class_a1_password_recovery.png)
- [Tìm bài và bài được giao](user/class_a1_problem_discovery.png)
- [Chạy thử, nộp bài, xem kết quả](user/class_a1_submission.png)
- [Tiến độ, hồ sơ, cài đặt](user/class_a1_profile_progress.png)
- [Phân tích bài giải AI](user/class_a1_ai_review.png)
- [Phỏng vấn giả lập](user/class_a1_mock_interview.png)
- [Học và luyện câu hỏi](user/class_a1_interview_bank.png)
- [Bookmark bài toán](user/class_a1_bookmark.png)
- [Lớp của tôi](user/class_a1_my_classes.png)

## A2 — Giảng viên

- [Soạn và xuất bản bài toán](gv/class_a2_problem_authoring.png)
- [Testcase và giới hạn chấm](gv/class_a2_testcases.png)
- [Giao bài cho lớp](gv/class_a2_assignments.png)
- [Sinh testcase AI](gv/class_a2_ai_testcases.png)
- [Quản lý lớp và mã mời](gv/class_a2_class_management.png)
- [Tiến độ lớp](gv/class_a2_class_progress.png)
- [Soạn câu hỏi phỏng vấn](gv/class_a2_interview_content.png)
- [Tổng quan giảng viên](gv/class_a2_overview.png)

## A3 — Quản trị viên

- [Ma trận phân quyền](admin/class_a3_permissions.png)
- [Quản lý tài khoản](admin/class_a3_users.png)
- [Giám sát máy chấm](admin/class_a3_judge_operations.png)
- [Cấu hình hệ thống và AI](admin/class_a3_configuration.png)

## A4 — Hệ thống tự động

- [Harness và lỗi biên dịch](system/class_a4_harness.png)
- [Worker chấm bài](system/class_a4_judge_worker.png)
- [Che testcase ẩn](system/class_a4_hidden_testcases.png)
- [Điều phối AI](system/class_a4_ai_processing.png)
- [Đối chiếu câu trả lời lý thuyết](system/class_a4_interview_feedback.png)

## Mô hình lớp dùng chung

Những hình này cho thấy thực thể, thuộc tính và quan hệ được nhiều actor sử dụng. Chúng bổ sung cấu trúc dữ liệu cho các sơ đồ service/port phía trên.

- [F1 — Tài khoản, quyền, lớp học](shared/class_shared_identity.png)
- [F2 — Bài toán và testcase](shared/class_shared_problem.png)
- [F4 — Bài nộp và kết quả](shared/class_shared_submission.png)
- [F5 — AI và phỏng vấn](shared/class_shared_ai.png)
- [F6 — Ngân hàng câu hỏi](shared/class_shared_interview_bank.png)

## Kiểm tra và giới hạn trình bày

- `python 08-diagram/class/tools/check_coverage.py --check`: đối chiếu 118 tiêu chí với file và phương thức có thật trong spec.
- `python .claude/skills/drawio-uml-architect/scripts/check_catalog.py`: đối chiếu `covers` với catalog.
- `python .claude/skills/drawio-uml-architect/scripts/lint_drawio.py <file.drawio>`: kiểm tra bố cục. PNG xuất tỉ lệ 2x, phù hợp xem màn hình hoặc in A3. Nếu ép vào bề rộng A4 6,53 inch, lint báo chữ dưới 6 pt; nên tách hình hoặc đặt trên trang A3 khi in.

Hai sơ đồ F3 chi tiết đã có ở thư mục cha: [plugin ngôn ngữ](class_f3_plugin_ngon_ngu.png), [đóng gói payload](class_f3_dong_goi_payload.png).
