# Danh mục chức năng hệ thống AlgoPrep (F1-F6)

> Tổng hợp sau khi RD hoàn tất (2026-09-08) — liệt kê toàn bộ mã chức năng `Fx-nn` đang **sống** trong
> `01-rd/req/*.md`, kèm giải thích ngắn gọn một dòng mỗi mã. Đây là bảng tra cứu nhanh, **không thay thế**
> đặc tả đầy đủ ở `01-rd/req/<module>.md` — mọi chi tiết điều kiện, ngưỡng, actor, quyết định liên quan vẫn
> lấy từ file gốc, file này chỉ tóm lược.
>
> Tổng: **111 mã sống** trên 6 Bounded Context. 8 mã đã cấp nhưng loại khỏi phạm vi
> (`F4-04`, `F4-09a` tới `F4-09e`, `F6-11`) không liệt kê ở đây — xem `01-rd/overview/system_survey.md`
> mục 5.7 nếu cần tra cứu mã đã khai tử.
>
> Cột **File liên quan** liệt kê các file RD theo trục màn hình (`01-rd/screens/`, đường dẫn rút gọn bỏ
> tiền tố `01-rd/screens/`) và sơ đồ (`08-diagram/01-rd/req/`, đường dẫn rút gọn bỏ tiền tố đó và đuôi
> `.spec.json`) **thật sự nhắc tới mã đó**, xác minh bằng Grep trên toàn bộ 2 cây thư mục — không suy đoán
> theo tên file. Một mã không có file nào ghi `(chưa có tham chiếu ở screens/sơ đồ)` — nghĩa là mã đó hiện
> mới có trong `01-rd/req/<module>.md`, chưa được screen hay sơ đồ nào trích dẫn trực tiếp.

## F1 — Danh tính và phân quyền (`identity`)

Nguồn: `01-rd/req/identity.md`. 30 mã (F1-01 tới F1-30).

| Mã | Chức năng | Giải thích ngắn gọn | File liên quan |
| :--- | :--- | :--- | :--- |
| F1-01 | Đăng ký tài khoản | Email + mật khẩu, vai trò mặc định `STUDENT` | shared/auth.md; users/profile.md; admin/admin_user_management.md — sơ đồ: sequence/user/sequence_user_dang_ky_dang_nhap; usecase/shared/usecase_shared_dang_nhap_dang_ky |
| F1-02 | Đăng nhập | Cấp Access Token ngắn hạn + Refresh Token trong cookie HTTP-Only | shared/auth.md — sơ đồ: sequence/user/sequence_user_dang_ky_dang_nhap; usecase/shared/usecase_shared_dang_nhap_dang_ky |
| F1-03 | Tự làm mới Access Token | Client tự đổi token mới khi gặp lỗi 401 | shared/auth.md — sơ đồ: sequence/user/sequence_user_dang_ky_dang_nhap |
| F1-04 | Đăng xuất | Vô hiệu hoá Refresh Token của phiên hiện tại | sơ đồ: usecase/shared/usecase_shared_dang_nhap_dang_ky |
| F1-05 | Phân quyền theo vai trò | Ba vai trò tách biệt `STUDENT`/`INSTRUCTOR`/`ADMIN`, không lồng nhau | shared/auth.md — sơ đồ: sequence/user/sequence_user_dang_ky_dang_nhap |
| F1-06 | Tiến độ cá nhân — bài đã giải | Theo chủ đề, kèm điểm tỷ lệ testcase tốt nhất từng bài (F4-13) | users/problem_list.md; users/profile.md; users/my_progress.md; teacher/class_progress.md; teacher/class_student_detail.md — sơ đồ: sequence/gv/sequence_gv_xem_tien_do_lop; usecase/user/usecase_user_tien_do_va_lich_su |
| F1-07 | Tỉ lệ chấp thuận | Số bài nộp đạt `Accepted` trên tổng số lượt nộp | users/submission_result.md; users/my_submissions.md; users/my_progress.md; users/profile.md; teacher/class_progress.md; teacher/class_student_detail.md — sơ đồ: sequence/gv/sequence_gv_xem_tien_do_lop; usecase/user/usecase_user_tien_do_va_lich_su |
| F1-08 | Lịch sử phỏng vấn | Xem lại rubric của phiên Mock Interview cũ | users/profile.md; users/my_progress.md; teacher/class_progress.md — sơ đồ: usecase/user/usecase_user_tien_do_va_lich_su |
| F1-09 | Sửa thông tin cá nhân | 6 trường sửa được; đổi email phải xác thực lại qua mã 6 số | users/profile.md — sơ đồ: usecase/user/usecase_user_ho_so_va_cai_dat |
| F1-10 | Ma trận phân quyền Role x Function x Action | `ADMIN` bật/tắt quyền từng cặp, có hiệu lực ngay | shared/problem_management.md; teacher/instructor_overview.md; shared/problem_authoring.md; admin/admin_permission_matrix.md; teacher/instructor_grading.md; teacher/class_management.md; teacher/class_progress.md; teacher/class_assignments.md; teacher/class_student_detail.md; admin/admin_system_log.md; admin/admin_overview.md — sơ đồ: usecase/admin/usecase_admin_quan_ly_nguoi_dung_phan_quyen |
| F1-11 | Function/Action là dữ liệu seed cố định | Chỉ đọc trên giao diện; Role tạo/sửa/xoá được | shared/problem_management.md; admin/admin_permission_matrix.md — sơ đồ: usecase/admin/usecase_admin_quan_ly_nguoi_dung_phan_quyen |
| F1-12 | Danh sách Function quản trị/nội dung | Liệt kê các nhóm chức năng bị ma trận F1-10 gác | shared/problem_management.md; teacher/instructor_overview.md; shared/interview_question_management.md; shared/problem_authoring.md; admin/admin_permission_matrix.md; teacher/instructor_grading.md; teacher/class_management.md; teacher/class_progress.md; teacher/class_assignments.md; teacher/class_student_detail.md; shared/interview_question_authoring.md; admin/admin_system_log.md; admin/admin_overview.md — (chưa có sơ đồ riêng) |
| F1-13 | Quản lý tài khoản người dùng | `ADMIN` đổi vai trò, khoá/mở khoá, reset mật khẩu hộ người khác | admin/admin_user_management.md; admin/admin_system_log.md; admin/admin_overview.md — sơ đồ: usecase/admin/usecase_admin_quan_ly_nguoi_dung_phan_quyen; sequence/admin/sequence_admin_doi_vai_tro_nguoi_dung |
| F1-14 | Nhật ký hệ thống | Ghi mọi thao tác quản trị của người (không gộp sự kiện hạ tầng) | shared/interview_question_management.md; admin/admin_user_management.md; admin/admin_queue_monitor.md; admin/admin_system_log.md; admin/admin_overview.md; shared/interview_question_authoring.md; teacher/instructor_overview.md — sơ đồ: sequence/admin/sequence_admin_cau_hinh_prompt_ai; usecase/admin/usecase_admin_nhat_ky_he_thong; sequence/admin/sequence_admin_doi_vai_tro_nguoi_dung; sequence/admin/sequence_admin_giam_sat_hang_doi |
| F1-15 | Đăng nhập/đăng ký OAuth | GitHub, Google — tự liên kết vào tài khoản có cùng email | shared/auth.md — sơ đồ: sequence/user/sequence_user_dang_ky_dang_nhap; usecase/shared/usecase_shared_dang_nhap_dang_ky |
| F1-16 | Tự xoá tài khoản | Khoá mềm ngay, ẩn danh hoá sau khoảng ân hạn, dữ liệu không mất | users/settings.md; shared/problem_management.md; users/profile.md; shared/problem_authoring.md; shared/auth.md; admin/admin_user_management.md; admin/admin_overview.md — sơ đồ: sequence/gv/sequence_gv_go_hoc_vien_khoi_lop; usecase/user/usecase_user_ho_so_va_cai_dat |
| F1-17 | Tự đặt lại mật khẩu | Mã xác nhận 6 số gửi qua email, không lộ email có tồn tại hay không | users/settings.md; users/profile.md; shared/auth.md; admin/admin_user_management.md — sơ đồ: sequence/user/sequence_user_dang_ky_dang_nhap; usecase/shared/usecase_shared_dang_nhap_dang_ky |
| F1-18 | Xem lịch sử nộp bài của chính mình | Lọc theo verdict, ngôn ngữ, tìm theo tên/mã bài | users/my_submissions.md; admin/admin_overview.md — sơ đồ: usecase/user/usecase_user_tien_do_va_lich_su |
| F1-19 | Tự đổi mật khẩu khi đã đăng nhập | Nhập mật khẩu hiện tại + mật khẩu mới, không qua email | users/profile.md — sơ đồ: usecase/user/usecase_user_ho_so_va_cai_dat |
| F1-20 | Tuỳ chọn cá nhân hoá Workspace | Ngôn ngữ mặc định, cỡ chữ, tự lưu nháp, phím tắt Vim | users/settings.md; admin/admin_overview.md — sơ đồ: usecase/user/usecase_user_ho_so_va_cai_dat |
| F1-21 | Thông báo email định kỳ | Nhắc chuỗi ngày sắp mất, báo cáo tiến độ hằng tuần | users/settings.md; teacher/class_progress.md — sơ đồ: usecase/user/usecase_user_ho_so_va_cai_dat |
| F1-22 | Xuất dữ liệu cá nhân | Lượt nộp (CSV), hội thoại phỏng vấn (JSON) của chính mình | users/settings.md — sơ đồ: usecase/user/usecase_user_ho_so_va_cai_dat |
| F1-23 | Tạo lớp học + mã mời | Giáo viên tạo lớp, hệ thống sinh mã mời; học viên tự tham gia bằng mã | teacher/class_management.md; teacher/class_student_detail.md — sơ đồ: sequence/gv/sequence_gv_tao_lop_va_ma_moi; usecase/gv/usecase_gv_quan_ly_lop_hoc |
| F1-24 | Xoá lớp học | Giáo viên xoá thật (hard delete), khác cách khoá mềm tài khoản | teacher/class_management.md — (chưa có sơ đồ riêng) |
| F1-25 | Mã mời có thời hạn | Tạo được nhiều mã mời mới cho cùng một lớp, mỗi mã có hạn riêng | teacher/class_management.md — sơ đồ: sequence/gv/sequence_gv_tao_lop_va_ma_moi; usecase/gv/usecase_gv_quan_ly_lop_hoc |
| F1-26 | Gỡ học viên khỏi lớp | Xoá cascade lịch sử làm bài của học viên đó trong phạm vi lớp | teacher/class_management.md; teacher/class_assignments.md; teacher/class_student_detail.md — sơ đồ: sequence/gv/sequence_gv_go_hoc_vien_khoi_lop; sequence/gv/sequence_gv_tao_lop_va_ma_moi; usecase/gv/usecase_gv_quan_ly_lop_hoc |
| F1-27 | Xem hồ sơ chi tiết học viên | Màn riêng `class_student_detail`, phạm vi trong lớp phụ trách | teacher/class_management.md; teacher/class_student_detail.md — sơ đồ: sequence/gv/sequence_gv_xem_tien_do_lop; usecase/gv/usecase_gv_theo_doi_tien_do_lop |
| F1-28 | Dashboard tiến độ lớp | `class_progress` — điểm TB, hoàn thành %, streak, nhãn cần chú ý | teacher/class_progress.md — sơ đồ: sequence/gv/sequence_gv_xem_tien_do_lop; usecase/gv/usecase_gv_theo_doi_tien_do_lop |
| F1-29 | Dashboard tổng quan vận hành | `admin_overview` — 8 khối thống kê hệ thống, một mã tổng hợp | admin/admin_overview.md; teacher/class_student_detail.md — (chưa có sơ đồ riêng) |
| F1-30 | Dashboard khối lượng công việc giảng viên | `instructor_overview` — lớp phụ trách, cần chấm tay, hoạt động gần đây | teacher/instructor_overview.md; teacher/class_student_detail.md — sơ đồ: usecase/gv/usecase_gv_theo_doi_tien_do_lop; sequence/gv/sequence_gv_xem_tien_do_lop |

## F2 — Ngân hàng bài toán và testcase (`problem-bank`)

Nguồn: `01-rd/req/problem-bank.md`. 17 mã (F2-01 tới F2-17).

| Mã | Chức năng | Giải thích ngắn gọn | File liên quan |
| :--- | :--- | :--- | :--- |
| F2-01 | Soạn đề bài | Markdown kèm công thức LaTeX | teacher/instructor_overview.md; shared/problem_management.md; shared/problem_authoring.md — sơ đồ: usecase/shared/usecase_shared_soan_de_bai |
| F2-02 | Phân loại bài toán | Độ khó, chủ đề, và thẻ (tag) tự do nhiều thẻ mỗi bài | admin/admin_overview.md; shared/problem_management.md; shared/problem_authoring.md — sơ đồ: usecase/shared/usecase_shared_soan_de_bai |
| F2-03 | Đặc tả cho cả 2 mô hình nộp bài | Chữ ký hàm 3 ngôn ngữ (Bọc hàm) + định dạng đọc/ghi (Standard I/O) | users/problem_detail.md; shared/problem_management.md; shared/problem_authoring.md — sơ đồ: usecase/user/usecase_user_giai_va_nop_bai; usecase/shared/usecase_shared_soan_de_bai |
| F2-04 | Chiến lược so khớp kết quả | `EXACT` / `TRIMMED` / `EPSILON` / `UNORDERED_SET` | shared/problem_management.md; shared/problem_authoring.md — sơ đồ: usecase/shared/usecase_shared_soan_de_bai |
| F2-05 | Testcase Sample | Công khai, dùng cho Chạy thử | users/submission_result.md; shared/problem_management.md; shared/problem_authoring.md — sơ đồ: usecase/shared/usecase_shared_quan_ly_testcase |
| F2-06 | Testcase Hidden | Ẩn, dùng để chấm khi Nộp bài | users/submission_result.md; shared/problem_management.md; shared/problem_authoring.md — sơ đồ: usecase/shared/usecase_shared_quan_ly_testcase |
| F2-07 | Tải testcase theo lô | Bộ lớn lưu trên MinIO | shared/problem_authoring.md — sơ đồ: usecase/shared/usecase_shared_quan_ly_testcase |
| F2-08 | Chống rò rỉ testcase ẩn | Phản hồi chỉ trả trạng thái và chỉ số, không trả input/diff | users/submission_result.md; users/solution_review.md; teacher/instructor_grading.md; shared/problem_authoring.md — sơ đồ: usecase/shared/usecase_shared_quan_ly_testcase |
| F2-09 | Phiên bản hoá bộ testcase | Truy vết một lượt nộp cũ đã chấm theo phiên bản nào | shared/problem_management.md; shared/problem_authoring.md — sơ đồ: usecase/shared/usecase_shared_quan_ly_testcase |
| F2-10 | Giới hạn tài nguyên | Thời gian/bộ nhớ theo bài, hệ số ngôn ngữ, đầu ra tối đa, số lần nộp/giờ | admin/admin_language_config.md; shared/problem_authoring.md — sơ đồ: usecase/shared/usecase_shared_quan_ly_testcase |
| F2-11 | Tìm kiếm và lọc bài toán | Theo chủ đề, độ khó, trạng thái đã giải, thẻ | admin/admin_overview.md; users/problem_list.md; shared/problem_management.md; teacher/class_progress.md — sơ đồ: usecase/user/usecase_user_tim_va_luu_bai_toan |
| F2-12 | Giao bài theo lớp | Gán/gỡ bài cho lớp; gỡ không xoá lượt nộp cũ của học viên | teacher/instructor_overview.md; users/problem_list.md; teacher/instructor_grading.md; teacher/class_management.md; teacher/class_student_detail.md; teacher/class_assignments.md; teacher/class_progress.md; shared/problem_management.md — sơ đồ: usecase/user/usecase_user_tim_va_luu_bai_toan; sequence/gv/sequence_gv_giao_bai_cho_lop; usecase/gv/usecase_gv_quan_ly_lop_hoc |
| F2-13 | Bookmark riêng tư | Đánh dấu bài + ghi chú riêng, tuyệt đối riêng tư kể cả với giảng viên/admin | users/saved_problems.md — sơ đồ: usecase/user/usecase_user_tim_va_luu_bai_toan |
| F2-14 | AI sinh testcase tự động | AI chỉ sinh input; output chạy thật từ Đáp án mẫu, không để AI bịa | admin/admin_ai_config.md; admin/admin_ai_usage.md; admin/admin_overview.md; shared/problem_management.md; shared/problem_authoring.md — sơ đồ: usecase/shared/usecase_shared_quan_ly_testcase |
| F2-15 | Vòng đời bài toán | 2 trạng thái `Chưa xuất bản`/`Đã xuất bản`; xoá là ẩn mềm + cờ `deleted` | shared/problem_management.md; shared/problem_authoring.md — sơ đồ: usecase/shared/usecase_shared_quan_ly_danh_sach_bai_toan |
| F2-16 | Nhân bản bài toán | Sao chép toàn bộ nội dung thành bài mới ở trạng thái `Chưa xuất bản` | shared/problem_management.md; shared/interview_question_authoring.md — sơ đồ: usecase/shared/usecase_shared_quan_ly_danh_sach_bai_toan |
| F2-17 | Xuất CSV danh sách bài toán | Chỉ dữ liệu bảng, không xuất nội dung đề/testcase | shared/problem_management.md — sơ đồ: usecase/shared/usecase_shared_quan_ly_danh_sach_bai_toan |

## F3 — Bộ sinh mã bọc hàm (`harness`)

Nguồn: `01-rd/req/harness.md`. 13 mã (F3-01 tới F3-13) — trọng tâm kỹ thuật của đồ án.

| Mã | Chức năng | Giải thích ngắn gọn | File liên quan |
| :--- | :--- | :--- | :--- |
| F3-01 | Lược đồ kiểu độc lập ngôn ngữ | Nguyên thuỷ, chuỗi, mảng nhiều chiều, danh sách, cây nhị phân | shared/problem_authoring.md; users/problem_detail.md; users/problem_list.md — (chưa có tham chiếu sơ đồ) |
| F3-02 | Sinh mã đọc dữ liệu vào | Từ đặc tả, cho cả 3 ngôn ngữ | users/problem_detail.md — (chưa có tham chiếu sơ đồ) |
| F3-03 | Sinh mã gọi hàm người dùng | Ghép lời gọi đúng chữ ký hàm đã khai báo | (chưa có tham chiếu ở screens/sơ đồ) |
| F3-04 | Sinh mã in kết quả | Định dạng output theo đúng kiểu trả về | users/problem_detail.md — (chưa có tham chiếu sơ đồ) |
| F3-05 | Tiêm mã người dùng | Ghép mã người học vào vùng chèn của mã khung | (chưa có tham chiếu ở screens/sơ đồ) |
| F3-06 | Đóng gói gửi qua `JudgeExecutionPort` | Theo đúng định dạng adapter yêu cầu (go-judge mặc định) | shared/problem_authoring.md — (chưa có tham chiếu sơ đồ) |
| F3-07 | So khớp exact | So sánh chuỗi/giá trị chính xác tuyệt đối | users/submission_result.md — (chưa có tham chiếu sơ đồ) |
| F3-08 | So khớp chuẩn hoá khoảng trắng | Bỏ qua khác biệt whitespace | (chưa có tham chiếu ở screens/sơ đồ) |
| F3-09 | So khớp epsilon | Số thực, sai số cho phép | (chưa có tham chiếu ở screens/sơ đồ) |
| F3-10 | So khớp tập hợp không thứ tự | Kết quả đúng nhưng không cần đúng thứ tự phần tử | users/submission_result.md — (chưa có tham chiếu sơ đồ) |
| F3-11 | Ánh xạ lỗi biên dịch | Trả lỗi về đúng dòng trong mã người dùng | users/problem_detail.md; users/submission_result.md — sơ đồ: usecase/user/usecase_user_giai_va_nop_bai |
| F3-12 | Che giấu lỗi mã harness | Người học không bao giờ thấy lỗi thuộc phần mã hệ thống | users/problem_detail.md; users/submission_result.md — (chưa có tham chiếu sơ đồ) |
| F3-13 | Hai mô hình song song, tự chọn | Học viên chuyển đổi Bọc hàm/Standard I/O ngay lúc làm bài; trừ bài vượt lược đồ kiểu chỉ còn Standard I/O | users/problem_detail.md; users/problem_list.md; users/submission_result.md — sơ đồ: usecase/user/usecase_user_tim_va_luu_bai_toan; usecase/user/usecase_user_giai_va_nop_bai |

## F4 — Điều phối judge engine (`judge-orchestration`)

Nguồn: `01-rd/req/judge-orchestration.md`. 11 mã sống (F4-01, 02, 03, 05 tới 08, 10 tới 13 — `F4-04` và
`F4-09a` tới `F4-09e` đã loại khỏi phạm vi).

| Mã | Chức năng | Giải thích ngắn gọn | File liên quan |
| :--- | :--- | :--- | :--- |
| F4-01 | Tiếp nhận bài nộp | Ghi `PENDING`, đẩy hàng đợi RabbitMQ, phản hồi ngay không chờ chấm; hỗ trợ tải file mã nguồn lên | users/my_submissions.md; users/submission_result.md; users/problem_detail.md — sơ đồ: sequence/user/sequence_user_nop_bai_va_xem_ket_qua_realtime; usecase/user/usecase_user_giai_va_nop_bai |
| F4-02 | Chạy thử | Với testcase Sample, không ghi nhận vào tiến độ | users/problem_detail.md — sơ đồ: usecase/user/usecase_user_giai_va_nop_bai |
| F4-03 | Gọi judge từng testcase | Một lần gọi mỗi testcase, chạy hết toàn bộ N testcase (không dừng sớm) | users/submission_result.md — (chưa có tham chiếu sơ đồ) |
| F4-05 | Xác thực webhook | Token bí mật riêng theo bài nộp — chỉ cần khi có adapter bất đồng bộ | (chưa có tham chiếu ở screens/sơ đồ) |
| F4-06 | Chống trùng callback | Theo token của engine — chỉ cần khi có adapter bất đồng bộ | (chưa có tham chiếu ở screens/sơ đồ) |
| F4-07 | Timeout sweep | Quét bài nộp treo quá 5 phút không có cập nhật trạng thái | admin/admin_queue_monitor.md — sơ đồ: usecase/admin/usecase_admin_giam_sat_judge_engine; sequence/admin/sequence_admin_giam_sat_hang_doi |
| F4-08 | Đẩy trạng thái realtime | Qua WebSocket (STOMP), theo kênh riêng từng bài nộp | users/my_submissions.md; users/submission_result.md — sơ đồ: sequence/user/sequence_user_nop_bai_va_xem_ket_qua_realtime; usecase/user/usecase_user_giai_va_nop_bai |
| F4-10 | Giám sát và điều khiển cụm judge | Xem hàng đợi, tạm dừng/tiếp tục tiêu thụ, bật/tắt autoscale | admin/admin_overview.md; admin/admin_queue_monitor.md — sơ đồ: usecase/admin/usecase_admin_giam_sat_judge_engine; sequence/admin/sequence_admin_giam_sat_hang_doi |
| F4-11 | Cấu hình ngôn ngữ và giới hạn | Hệ số theo ngôn ngữ + 3 tham số sandbox go-judge (mạng, tiến trình con, stderr) | admin/admin_language_config.md; admin/admin_overview.md; shared/problem_authoring.md — sơ đồ: usecase/admin/usecase_admin_cau_hinh_ngon_ngu_ai; sequence/admin/sequence_admin_cau_hinh_prompt_ai |
| F4-12 | Chỉ số "Beats" | % bài nộp khác chậm hơn, cùng bài toán và cùng ngôn ngữ, chỉ khi `Accepted` | users/submission_result.md — sơ đồ: usecase/user/usecase_user_giai_va_nop_bai |
| F4-13 | Điểm tỷ lệ testcase | Pass/N quy về thang 1 điểm, hiển thị thêm cạnh verdict nhị phân không đổi | users/submission_result.md; teacher/class_student_detail.md; teacher/instructor_grading.md; shared/problem_authoring.md — sơ đồ: sequence/user/sequence_user_nop_bai_va_xem_ket_qua_realtime; usecase/user/usecase_user_giai_va_nop_bai; sequence/gv/sequence_gv_cham_tay_de_len_diem_ai |

## F5 — Phân hệ AI (`ai-review`)

Nguồn: `01-rd/req/ai-review.md`. 28 mã (F5-01 tới F5-28; `F5-29` từng đề xuất nhưng không bao giờ cấp).

| Mã | Chức năng | Giải thích ngắn gọn | File liên quan |
| :--- | :--- | :--- | :--- |
| F5-01 | Yêu cầu phân tích bài giải | Hành động chủ động của học viên, chỉ hiện sau khi `Accepted` | users/submission_result.md; users/problem_detail.md; users/my_submissions.md; users/solution_review.md; users/problem_list.md — sơ đồ: usecase/user/usecase_user_phan_tich_bai_giai_ai; sequence/user/sequence_user_phan_tich_bai_giai_ai |
| F5-02 | Phân tích độ phức tạp | Thời gian và bộ nhớ thực tế, kèm lập luận | users/solution_review.md — (chưa có tham chiếu sơ đồ) |
| F5-03 | Gợi ý hướng tiếp cận tốt hơn | Đối chiếu với độ phức tạp tối ưu đã biết của bài toán | users/solution_review.md — (chưa có tham chiếu sơ đồ) |
| F5-04 | Chỉ ra rủi ro trong mã | Edge case chưa phủ, giả định ngầm, tràn số, dữ liệu lớn | users/solution_review.md — (chưa có tham chiếu sơ đồ) |
| F5-05 | Nhận xét chất lượng mã | Đặt tên, phân rã, trùng lặp, kèm điểm dễ đọc thang 1-5 | teacher/instructor_grading.md; users/solution_review.md — sơ đồ: sequence/gv/sequence_gv_cham_tay_de_len_diem_ai |
| F5-06 | Gợi ý chủ đề liên kết | Từ khoá/chủ đề gợi ý, dẫn sang `interview_bank_list` (F6) | users/solution_review.md; users/interview_bank_list.md — (chưa có tham chiếu sơ đồ) |
| F5-07 | Trả JSON có lược đồ | Để giao diện render báo cáo tĩnh | teacher/instructor_grading.md; users/solution_review.md — sơ đồ: usecase/user/usecase_user_phan_tich_bai_giai_ai; usecase/gv/usecase_gv_cham_va_nhan_xet_bai_nop; sequence/gv/sequence_gv_cham_tay_de_len_diem_ai |
| F5-08 | Lưu báo cáo | Kèm bài nộp, tra cứu lại từ trang tiến độ | users/solution_review.md — sơ đồ: usecase/user/usecase_user_phan_tich_bai_giai_ai; sequence/user/sequence_user_phan_tich_bai_giai_ai |
| F5-09 | Mở phiên phỏng vấn từ bài nộp | Lối vào mặc định của Mock Interview, cần `Accepted` | users/submission_result.md; users/problem_detail.md; users/mock_interview.md; users/settings.md — sơ đồ: usecase/user/usecase_user_phong_van_gia_lap_ai; sequence/user/sequence_user_phong_van_gia_lap_ai |
| F5-10 | Giai đoạn Giải trình thuật toán | Người học trình bày ý tưởng, lý do chọn cấu trúc dữ liệu | users/mock_interview.md — sơ đồ: usecase/user/usecase_user_phong_van_gia_lap_ai |
| F5-11 | Giai đoạn Phản biện | AI chất vấn điểm chưa tối ưu và trường hợp biên | shared/interview_question_management.md; shared/interview_question_authoring.md; users/mock_interview.md — (chưa có tham chiếu sơ đồ) |
| F5-12 | Giai đoạn Mở rộng quy mô | Tình huống dữ liệu tăng đột biến, ràng buộc hệ thống đổi | users/mock_interview.md — (chưa có tham chiếu sơ đồ) |
| F5-13 | Duy trì ngữ cảnh phiên | `ChatMemory` trên Redis xuyên cả 3 giai đoạn | users/mock_interview.md — sơ đồ: sequence/user/sequence_user_phong_van_gia_lap_ai |
| F5-14 | Stream phản hồi | Qua SSE | users/mock_interview.md — (chưa có tham chiếu sơ đồ) |
| F5-15 | Rubric 4 tiêu chí | Độ rõ ràng, độ chính xác kỹ thuật, khả năng phản biện, nhận thức độ phức tạp | shared/interview_question_management.md; shared/interview_question_authoring.md; users/mock_interview.md — (chưa có tham chiếu sơ đồ) |
| F5-16 | Lưu phiên và rubric | Mở lại được từ trang tiến độ | users/mock_interview.md — sơ đồ: usecase/user/usecase_user_phong_van_gia_lap_ai; sequence/user/sequence_user_phong_van_gia_lap_ai |
| F5-17 | Chống prompt injection | Mã nguồn/câu trả lời và chỉ dẫn AI của giảng viên đều là tham số dữ liệu, không phải chỉ thị hệ thống | shared/problem_authoring.md; users/solution_review.md; admin/admin_ai_config.md; users/interview_question_detail.md — sơ đồ: sequence/user/sequence_user_phan_tich_bai_giai_ai; sequence/user/sequence_user_on_tap_luyen_tap_ai |
| F5-18 | Định hướng giáo dục | Rubric/báo cáo là phản hồi hỗ trợ học tập, không phải điểm chính thức | users/solution_review.md; users/mock_interview.md; admin/admin_ai_config.md; users/interview_question_detail.md — (chưa có tham chiếu sơ đồ) |
| F5-19 | Giới hạn tần suất gọi AI | Theo người dùng, kèm cảnh báo mềm khi một tài khoản dùng bất thường | users/solution_review.md; admin/admin_ai_usage.md; admin/admin_ai_config.md; users/mock_interview.md — (chưa có tham chiếu sơ đồ) |
| F5-20 | Cache kết quả phân tích | Theo hash mã nguồn — nộp lại đúng mã thì không gọi lại AI | users/solution_review.md — sơ đồ: sequence/user/sequence_user_phan_tich_bai_giai_ai |
| F5-21 | Theo dõi token tiêu thụ | Theo từng phiên | admin/admin_overview.md; admin/admin_ai_usage.md — sơ đồ: usecase/admin/usecase_admin_ngan_sach_token_ai; sequence/admin/sequence_admin_dat_ngan_sach_token |
| F5-22 | Suy giảm có kiểm soát | AI hỏng/hết quota thì F1-F4 vẫn hoạt động bình thường | users/solution_review.md; users/mock_interview.md; users/interview_question_detail.md — sơ đồ: sequence/user/sequence_user_phong_van_gia_lap_ai; sequence/user/sequence_user_phan_tich_bai_giai_ai |
| F5-23 | Cấu hình prompt và rubric | Actor A3; rubric có phiên bản, chỉ phủ rubric của Solution Review | shared/problem_authoring.md; shared/interview_question_management.md; shared/interview_question_authoring.md; admin/admin_system_log.md; admin/admin_overview.md; admin/admin_ai_config.md; users/solution_review.md — sơ đồ: usecase/admin/usecase_admin_cau_hinh_ngon_ngu_ai; sequence/admin/sequence_admin_cau_hinh_prompt_ai |
| F5-24 | Ba lối vào phiên phỏng vấn | Từ bài nộp, từ kho câu hỏi F6, hoặc tự chọn chủ đề — không cần `Accepted` với 2 lối sau | users/mock_interview.md; users/settings.md — sơ đồ: usecase/user/usecase_user_phong_van_gia_lap_ai |
| F5-25 | Ngân sách token AI | Dự báo cạn quota, tự động khoá gọi AI khi vượt (trừ `ADMIN`) | users/solution_review.md; admin/admin_overview.md; admin/admin_ai_usage.md; users/interview_question_detail.md; users/mock_interview.md — sơ đồ: usecase/admin/usecase_admin_ngan_sach_token_ai; sequence/admin/sequence_admin_dat_ngan_sach_token; sequence/user/sequence_user_phan_tich_bai_giai_ai |
| F5-26 | Áp mã AI vào Workspace | Có xác nhận trước khi ghi đè, giữ lại bản mã cũ (snapshot) | users/solution_review.md — sơ đồ: usecase/user/usecase_user_phan_tich_bai_giai_ai |
| F5-27 | Điểm AI tham khảo + chấm tay | Thang 10, giảng viên chấm đè kèm nhận xét, tách bạch khỏi verdict chính thức | teacher/instructor_overview.md; teacher/instructor_grading.md; teacher/class_student_detail.md; teacher/class_progress.md; teacher/class_assignments.md; teacher/class_management.md — sơ đồ: usecase/gv/usecase_gv_cham_va_nhan_xet_bai_nop; sequence/gv/sequence_gv_cham_tay_de_len_diem_ai |
| F5-28 | Tự chỉnh tham số phiên tự luyện | Mức người phỏng vấn, số lượt tối đa, cho phép gợi ý khi bí | users/mock_interview.md; users/settings.md — sơ đồ: usecase/user/usecase_user_phong_van_gia_lap_ai |

## F6 — Ngân hàng câu hỏi phỏng vấn (`interview-bank`)

Nguồn: `01-rd/req/interview-bank.md`. 12 mã sống (F6-01 tới F6-10, F6-12, F6-13 — `F6-11` đã loại khỏi
phạm vi).

| Mã | Chức năng | Giải thích ngắn gọn | File liên quan |
| :--- | :--- | :--- | :--- |
| F6-01 | Danh sách câu hỏi | Phân loại theo 5 chủ đề (CS, System design, Database, Ngôn ngữ, Hành vi) và mức độ | users/interview_bank_list.md; shared/interview_question_management.md; shared/interview_question_authoring.md; users/interview_question_detail.md — sơ đồ: usecase/user/usecase_user_on_tap_cau_hoi_phong_van |
| F6-02 | Tìm kiếm và lọc | Theo chủ đề, mức độ, thẻ | users/interview_bank_list.md; shared/interview_question_management.md — (chưa có tham chiếu sơ đồ) |
| F6-03 | Đánh dấu xem lại | Đánh dấu câu hỏi để xem lại sau | users/interview_bank_list.md; shared/interview_question_management.md; users/interview_question_detail.md — (chưa có tham chiếu sơ đồ) |
| F6-04 | Chế độ học — gợi ý hướng tiếp cận | Xem gợi ý cách trả lời | users/interview_bank_list.md; shared/interview_question_management.md; shared/interview_question_authoring.md; users/interview_question_detail.md — sơ đồ: sequence/user/sequence_user_on_tap_luyen_tap_ai |
| F6-05 | Chế độ học — khung trả lời chuẩn | Áp dụng mô hình STAR cho câu hỏi hành vi | users/interview_bank_list.md; shared/interview_question_management.md; users/interview_question_detail.md — (chưa có tham chiếu sơ đồ) |
| F6-06 | Chế độ học — từ khoá cốt lõi | Danh sách từ khoá kỹ thuật cần nêu | users/interview_bank_list.md; shared/interview_question_management.md; users/interview_question_detail.md — sơ đồ: usecase/user/usecase_user_on_tap_cau_hoi_phong_van |
| F6-07 | Chế độ luyện — tự soạn câu trả lời | Mỗi lần nộp là một lượt độc lập, không sửa lại được | shared/interview_question_management.md; users/interview_question_detail.md — sơ đồ: usecase/user/usecase_user_on_tap_cau_hoi_phong_van; sequence/user/sequence_user_on_tap_luyen_tap_ai |
| F6-08 | AI đối chiếu câu trả lời | So với tiêu chí chuẩn, trả điểm đã đạt/còn thiếu/hướng bổ sung | shared/interview_question_management.md; shared/interview_question_authoring.md; users/interview_question_detail.md — sơ đồ: sequence/user/sequence_user_on_tap_luyen_tap_ai |
| F6-09 | Lịch sử luyện tập | Danh sách câu hỏi cần ôn lại | users/interview_bank_list.md — (chưa có tham chiếu sơ đồ) |
| F6-10 | Tỉ lệ hoàn thành theo chủ đề | Theo dõi tiến độ ôn tập | users/interview_bank_list.md — sơ đồ: usecase/user/usecase_user_on_tap_cau_hoi_phong_van |
| F6-12 | Tự chấm mức độ thuộc bài | Spaced repetition — tự xếp lịch câu hỏi cần ôn lại sớm hơn | users/interview_bank_list.md; shared/interview_question_management.md; shared/interview_question_authoring.md; users/interview_question_detail.md; admin/admin_overview.md — sơ đồ: usecase/user/usecase_user_on_tap_cau_hoi_phong_van; sequence/user/sequence_user_on_tap_luyen_tap_ai |
| F6-13 | Quản trị nội dung ngân hàng câu hỏi | CRUD dùng chung hệ thống (A2 và A3), không còn phân theo lớp | shared/interview_question_management.md; shared/interview_question_authoring.md — sơ đồ: usecase/shared/usecase_shared_quan_ly_ngan_hang_cau_hoi |

---

## Ghi chú phạm vi

- File này chỉ liệt kê chức năng **nghiệp vụ** (`Fx-nn`). Yêu cầu phi chức năng (hiệu năng, bảo mật OWASP,
  khả năng mở rộng...) nằm ở `01-rd/req/nfr.md`, không lặp lại ở đây.
- Các tính năng đã cắt khỏi phạm vi trong quá trình RD (chấm lại/rejudge, bộ câu hỏi phỏng vấn theo lớp,
  gợi ý theo bậc, báo cáo nghi gian lận, học viên yêu cầu review) không xuất hiện ở bảng trên — xem
  `.nexa/control/decision-registry.md` nếu cần lý do loại bỏ.
- Nguồn duy nhất cho từng mã vẫn là file `01-rd/req/<module>.md` tương ứng — bảng này có thể lệch nếu RD
  được sửa sau ngày 2026-09-08 mà file này chưa cập nhật theo.
- Cột "File liên quan" liệt kê **screen RD** và **sơ đồ** hiện có trên đĩa tại thời điểm 2026-09-09, xác
  minh bằng Grep theo đúng chuỗi mã — không phải danh sách "nơi lẽ ra nên trích dẫn". Một mã chưa có sơ đồ
  hoặc chưa có screen trích dẫn không có nghĩa là mã đó sai hay thiếu — nhiều mã (đặc biệt F3, F4 phần
  hạ tầng) thuộc lớp kỹ thuật nội bộ, không cần một màn hình riêng để minh hoạ.
