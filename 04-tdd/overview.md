# Tổng quan TDD — AlgoPrep

> Cập nhật: 2026-10-27 · Nguồn: RD → BD → DD đã chốt · Trạng thái: [đợt 1 F2/F3/F4 đã đóng mốc thiết kế có điều kiện](testcases/AUDIT_DOT1.md), **476 ca trong 40 file cụm**, 221/221 AC đợt 1 có nguồn testcase; golden P2 chờ codegen, chưa có bằng chứng chạy. Quy tắc `position` và 6 oracle đã chốt.

## 1. Mục đích và ranh giới

TDD là hợp đồng nghiệm thu để QA chuyển thành testcase. Mỗi AC nêu **tiền đề, hành động, kết quả quan sát được và nguồn**. Chi tiết giao thức, mã lỗi, bảng dữ liệu, lịch job và thuật toán nằm ở DD; test case QA ghi dữ liệu kiểm và kết quả mong đợi cụ thể trước khi tự động hoá hoặc thực thi. Không lấy prototype làm nguồn quyết định nghiệp vụ.

Tài liệu chia hai trục: `modules/` kiểm hợp đồng máy chủ, quyền, bất biến và suy giảm; `screens/<nhóm>/` kiểm trạng thái màn, thao tác, điều hướng và phối hợp các module. Mã AC cũ được giữ nguyên để không làm hỏng trích dẫn; AC mới dùng số tiếp theo trong cùng file. Màn `USR0501` đã gộp vào `USR0601`, nên không có file DD/TDD độc lập.

## 2. Quy tắc QA trước khi sinh testcase

1. Mỗi điểm kiểm thử DD màn phải dẫn tới ít nhất một AC màn hoặc module; mỗi AC phải có một dòng truy vết và nguồn có thật.
2. Với thao tác ghi, kiểm **thành công, dữ liệu không hợp lệ, thiếu quyền, lỗi hạ tầng và gửi lặp/đồng thời** khi hợp đồng có các nhánh đó. Thao tác phá huỷ cần kiểm xác nhận trên màn, trạng thái sau thao tác và nhật ký/sự kiện ở module sở hữu.
3. Với số liệu, kiểm **tử số, mẫu số, tập rỗng, bản ghi đã rút/xoá, cửa sổ thời gian và trường hợp nguồn lỗi**. Giá trị không biết không được hiển thị là 0.
4. Với chấm bài, kiểm Accepted lẫn sai/biên dịch/lỗi hạ tầng, testcase ẩn không rò dữ liệu, snapshot tại lúc nộp, kết quả đầu tiên sai theo thứ tự và điểm từng phần.
5. Với AI, kiểm giới hạn ngân sách, lỗi nhà cung cấp, kết quả sai cấu trúc, dữ liệu riêng tư và khả năng các chức năng không dùng AI vẫn hoạt động.
6. Test case đã review ghi mã vào cột **Test case** của đúng AC; trạng thái `Chưa thực hiện` chỉ đổi khi có bằng chứng chạy. Một AC có thể cần nhiều testcase cho các nhánh; không đánh dấu Đạt chỉ vì đường thành công chạy qua.

## 3. Mức phủ hiện tại

| Hạng mục | Kết quả kiểm | Giới hạn còn lại |
| :--- | :--- | :--- |
| Tệp TDD | 6/6 module và 40/40 màn DD | Không suy ra chất lượng nội dung từ số tệp |
| AC | 400 module + 152 màn = **552** | Một AC có thể cần nhiều testcase để phủ đủ nhánh |
| Truy vết AC | 552/552 AC có dòng truy vết; 221/221 AC module F2/F3/F4 có nguồn testcase | Đợt 1 đóng thiết kế có điều kiện; AC06-02 còn golden byte sau codegen, không suy test pass từ số ca |
| Điểm kiểm DD màn | 461/461 dòng đã nối AC; 159 dòng từng thiếu liên kết đã được bổ sung | Một điểm DD hoặc AC gộp nhánh có thể cần nhiều testcase độc lập |
| Liên kết tài liệu | Không có đường dẫn TDD gãy trong RD/BD/DD/TDD | Chưa kiểm liên kết ngoài phạm vi TDD |

**Trạng thái baseline:** các AC module và màn đã được đối chiếu với DD hiện hành, toàn bộ điểm kiểm DD màn đã có AC. Xung đột về `position` giữa DD F2 API #45, BR-11 và thứ tự chấm F4 đã được giải quyết theo quyết định của chủ dự án: đổi thứ tự thực sự trên bài `PUBLISHED` tạo snapshot mới; xem [file test case](testcases/modules/F2_problem-bank/F2_testcase_set_version.md). [Biên bản đợt 1](testcases/AUDIT_DOT1.md) ghi kết quả đóng thiết kế, ngoại lệ golden P2 và cổng sau coding. Trạng thái `Chưa thực hiện` giữ nguyên tới giai đoạn kiểm thử. Chất lượng câu trả lời AI thật sẽ được đánh giá thủ công khi nối nhà cung cấp.

## 4. Bounded Context

| Mã | Phạm vi | TDD | Số AC |
| :--- | :--- | :--- | ---: |
| F1 | Định danh, tài khoản, lớp, quyền, nhật ký | [F1_identity.md](modules/F1_identity.md) | 80 |
| F2 | Ngân hàng bài, testcase, giao bài, sự kiện | [F2_problem-bank.md](modules/F2_problem-bank.md) | 94 |
| F3 | Mã khung, bọc hàm, so khớp kết quả | [F3_harness.md](modules/F3_harness.md) | 50 |
| F4 | Nộp bài, chấm, hàng đợi, cấu hình máy chấm | [F4_judge-orchestration.md](modules/F4_judge-orchestration.md) | 77 |
| F5 | Phân tích AI, phỏng vấn giả lập, ngân sách | [F5_ai-review.md](modules/F5_ai-review.md) | 58 |
| F6 | Ngân hàng câu hỏi, luyện, tiến độ ôn | [F6_interview-bank.md](modules/F6_interview-bank.md) | 41 |

## 5. Màn hình

| Nhóm | Số màn | Số AC | Danh sách |
| :--- | ---: | ---: | :--- |
| users | 16 | 82 | [USR0101](screens/users/USR0101_problem_list.md) · [USR0102](screens/users/USR0102_problem_detail.md) · [USR0103](screens/users/USR0103_saved_problems.md) · [USR0104](screens/users/USR0104_problem_overview.md) · [USR0105](screens/users/USR0105_workspace_home.md) · [USR0201](screens/users/USR0201_submission_result.md) · [USR0202](screens/users/USR0202_my_submissions.md) · [USR0301](screens/users/USR0301_solution_review.md) · [USR0302](screens/users/USR0302_mock_interview.md) · [USR0401](screens/users/USR0401_interview_bank_list.md) · [USR0402](screens/users/USR0402_interview_question_detail.md) · [USR0502](screens/users/USR0502_profile.md) · [USR0503](screens/users/USR0503_settings.md) · [USR0601](screens/users/USR0601_dashboard.md) · [USR0701](screens/users/USR0701_my_classes.md) · [USR0702](screens/users/USR0702_my_class_detail.md) |
| teacher | 7 | 17 | [INS0101](screens/teacher/INS0101_overview.md) · [INS0201](screens/teacher/INS0201_class_management.md) · [INS0202](screens/teacher/INS0202_class_assignments.md) · [INS0203](screens/teacher/INS0203_class_progress.md) · [INS0204](screens/teacher/INS0204_class_student_detail.md) · [INS0205](screens/teacher/INS0205_class_detail.md) · [INS0206](screens/teacher/INS0206_submission_management.md) |
| admin | 10 | 29 | [ADM0101](screens/admin/ADM0101_overview.md) · [ADM0201](screens/admin/ADM0201_user_management.md) · [ADM0202](screens/admin/ADM0202_permission_matrix.md) · [ADM0301](screens/admin/ADM0301_ai_config.md) · [ADM0302](screens/admin/ADM0302_ai_usage.md) · [ADM0303](screens/admin/ADM0303_ai_limits.md) · [ADM0401](screens/admin/ADM0401_queue_monitor.md) · [ADM0403](screens/admin/ADM0403_system_log.md) · [ADM0404](screens/admin/ADM0404_infra_events.md) · [ADM0501](screens/admin/ADM0501_language_config.md) |
| shared | 7 | 24 | [SHR0101](screens/shared/SHR0101_auth.md) · [SHR0201](screens/shared/SHR0201_problem_management.md) · [SHR0202](screens/shared/SHR0202_problem_authoring.md) · [SHR0203](screens/shared/SHR0203_problem_info.md) · [SHR0301](screens/shared/SHR0301_interview_question_management.md) · [SHR0302](screens/shared/SHR0302_interview_question_authoring.md) · [SHR0303](screens/shared/SHR0303_interview_question_info.md) |

## 6. Cách dùng để tạo testcase

Đọc [README](README.md) để xác định trục và [kế hoạch test case QA](testcases/README.md) để chọn thứ tự, mẫu ca và mốc thực thi; rồi mở AC của module hoặc màn liên quan. Với từng AC, tách các nhánh trong tiền đề/hành động thành testcase Normal, Abnormal, Boundary, Security hoặc Concurrency theo loại ở DD. Ghi mã testcase vào cột truy vết sau review; lưu bằng chứng khi thực thi; dùng DD để lấy endpoint, mã lỗi và dữ liệu kiểm chính xác. Nếu AC không đủ rõ để khẳng định Đạt/Trượt, sửa AC trước khi viết testcase.

Thiết kế test case và thực thi test là hai mốc riêng. Có thể review và gắn mã test case khi RD/BD/DD/TDD đã cho phép xác định đầu vào cùng kết quả đúng; để trạng thái `Chưa thực hiện` cho đến khi chạy thật và lưu bằng chứng. Với mỗi test case, ghi rõ phiên bản hợp đồng, fixture, thao tác, kết quả tại UI/API/DB/event/job khi áp dụng, cách khôi phục dữ liệu và môi trường cần có. Khi code hoặc giao diện chưa hoàn tất, đánh dấu ca `Chưa thể thực thi` trong bộ test case; không suy diễn Đạt/Trượt từ prototype.

## 7. Các cụm kiểm tra ưu tiên khi viết testcase

| Cụm | Trường hợp phải tách thành testcase riêng | Nguồn AC |
| :--- | :--- | :--- |
| Tài khoản và quyền | OTP đúng/sai/hết hạn, gửi lại tính hạn mức, huỷ tự xoá rồi đăng nhập lại, truy cập chéo vai trò, thử mã mời 5 lần/15 phút | [F1](modules/F1_identity.md), [màn xác thực](screens/shared/SHR0101_auth.md) |
| Bộ testcase và xuất bản | Thay đổi Sample/Hidden đã duyệt, nháp chưa duyệt, giữ ngưỡng 8 ca và 2 Sample, snapshot cũ/hỏng, phát lại và dọn Outbox | [F2](modules/F2_problem-bank.md) AC-56, AC-59, AC-89–94; [màn biên soạn](screens/shared/SHR0202_problem_authoring.md) |
| Mã bọc hàm | Kiểm chữ ký cho ba ngôn ngữ, bài chưa xuất bản, kết quả tập đa phần tử, đầu vào mảng nhiều chiều | [F3](modules/F3_harness.md) AC-27, AC-48–50 |
| Máy chấm | Hết 60 lượt chạy thử/60 phút, trần 8 lượt đồng thời, lỗi hàng đợi, kết quả của snapshot đã ghim, lỗi đặc tả, rò dữ liệu Hidden/stderr | [F4](modules/F4_judge-orchestration.md) AC-08, AC-73–76; [Workspace](screens/users/USR0102_problem_detail.md) |
| AI và ngân sách | Hết hạn mức, nhà cung cấp lỗi, phản hồi sai cấu trúc, không đưa mã/ca ẩn vào prompt, thao tác không dùng AI vẫn chạy | [F5](modules/F5_ai-review.md), [cấu hình AI](screens/admin/ADM0303_ai_limits.md) |
| Số liệu lớp và Dashboard | Bài giao rồi rút, lượt nộp trước/sau khoảng được giao, học viên trùng giữa lớp, tuần thiếu dữ liệu, nguồn lỗi không thành số 0 | [F1](modules/F1_identity.md), [F2](modules/F2_problem-bank.md), [Dashboard](screens/users/USR0601_dashboard.md), [tiến độ lớp](screens/teacher/INS0203_class_progress.md) |

Mỗi trường hợp trong bảng là nhắc việc chọn dữ liệu kiểm; kết quả đúng phải lấy từ AC và DD được dẫn. Không gộp nhiều nhánh vào một testcase chỉ vì chúng cùng nằm trong một AC.

### Các AC gộp nhánh phải tách khi lập test case

| AC | Các ca độc lập tối thiểu | Oracle cần đối chiếu |
| :--- | :--- | :--- |
| [F2 AC-56](modules/F2_problem-bank.md) | Thêm testcase đã duyệt; duyệt nháp; sửa Sample/Hidden đã duyệt; xoá testcase đã duyệt; đổi `position` kể cả kéo nháp; chỉ đổi nhãn; sửa nội dung nháp chưa duyệt; gửi lại thứ tự cũ; lặp với bài chưa xuất bản | Phiên bản tăng đúng một lần và snapshot mới bất biến khi bộ chấm hoặc thứ tự chung đổi trên bài `PUBLISHED`; các nhánh còn lại không tăng. Đối chiếu DD F2 BR-11 và AC-57/59/60 để dựng dữ liệu hợp lệ |
| [SHR0202 PA-AC-02](screens/shared/SHR0202_problem_authoring.md) | Tệp thiếu cặp; cặp trùng; tải một tệp lỗi; xác nhận lô; sinh AI lỗi; chạy đáp án mẫu lỗi; sửa/xoá làm bài xuất bản dưới ngưỡng | Không tạo dấu đạt giả, không tự duyệt, không ghi nửa lô, dữ liệu trước thao tác bị từ chối được giữ. Đối chiếu DD màn mục 3, 7 và DD F2 cho lỗi máy chủ |
| [F4 AC-77](modules/F4_judge-orchestration.md) | Bằng/vượt ngưỡng; vượt ngưỡng một/hai lượt; chạm trần 8; rỗng ba/bốn lượt; consumer còn việc; pause/resume; tắt tự mở rộng; hai leader/mất khoá | Mục tiêu và số consumer thực theo JOB-JD-05; bài đang chạy không bị huỷ và không nhận việc mới khi pause hoặc mất leader |
