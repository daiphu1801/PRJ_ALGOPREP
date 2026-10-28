# Review sequence diagram theo kiến trúc mới

[Kế hoạch và checklist](PLAN-cai-tien-theo-actor.md) · [Chú giải chung](CHU_GIAI.md) · [Manifest 63 trang gốc](baseline_manifest.csv)

**Tiến độ:** 63/63 trang gốc đã được vẽ lại thành 94 trang; mỗi trang có spec, bản `.drawio` chỉnh sửa được và PNG 2x để review. Các tên lớp nghiệp vụ trong sơ đồ là thiết kế dự kiến, chưa phải code backend đã triển khai. Actor khởi phát đứng ở cột ngoài cùng bên trái.

## A1 — Sinh viên

- [Đăng ký 1a. Tạo tài khoản chờ xác minh](user/sequence_user_dang_ky_dang_nhap_p1.png)
- [Đăng ký 1b. Phát mã xác minh email](user/sequence_user_dang_ky_dang_nhap_p1b.png)
- [Đăng ký 1c. Xác minh email và kích hoạt](user/sequence_user_dang_ky_dang_nhap_p1c.png)
- [Đăng nhập 2a. Chặn dò mật khẩu](user/sequence_user_dang_ky_dang_nhap_p2.png)
- [Đăng nhập 2b. Kiểm mật khẩu và trạng thái](user/sequence_user_dang_ky_dang_nhap_p2b.png)
- [Đăng nhập 2c. Cấp phiên an toàn](user/sequence_user_dang_ky_dang_nhap_p2c.png)
- [Phiên 3a. Xoay vòng mã làm mới](user/sequence_user_dang_ky_dang_nhap_p3.png)
- [Phiên 3b. Phát hiện dùng lại mã](user/sequence_user_dang_ky_dang_nhap_p3b.png)

Ba trang cũ của luồng tài khoản đã thay theo DD mới: tự đăng ký cần OTP email trước khi ACTIVE; tài khoản chưa xác minh không nhận phiên đăng nhập; F1-03 là lần làm mới phiên sau đó, với quy tắc dùng refresh token một lần và khóa cả family khi phát hiện reuse.

- [OAuth 1. Bắt đầu đăng nhập và lưu trạng thái](user/sequence_user_lien_ket_oauth_p1.png)
- [OAuth 2a. Kiểm trạng thái phản hồi](user/sequence_user_lien_ket_oauth_p2.png)
- [OAuth 2b. Lấy hồ sơ đã xác minh](user/sequence_user_lien_ket_oauth_p2b.png)
- [OAuth 3. Liên kết tài khoản và cấp phiên](user/sequence_user_lien_ket_oauth_p3.png)

Luồng OAuth đã tách phần chuyển hướng, kiểm trạng thái, xác minh email từ nhà cung cấp và liên kết/cấp phiên. Chính sách xử lý tài khoản tự đăng ký đang chờ xác minh khi trùng email OAuth cần được chốt trong DD trước khi coding.

- [Quên mật khẩu 1a. Tìm tài khoản qua cổng dữ liệu](user/sequence_user_quen_mat_khau_otp_p1.png)
- [Quên mật khẩu 1b. Giới hạn gửi và phát mã](user/sequence_user_quen_mat_khau_otp_p1b.png)
- [Quên mật khẩu 1c. Gửi thư và trả lời chung](user/sequence_user_quen_mat_khau_otp_p1c.png)
- [Quên mật khẩu 2. Xác minh mã trước khi đổi mật khẩu](user/sequence_user_quen_mat_khau_otp_p2.png)
- [Quên mật khẩu 3a. Kiểm lại mã khi đặt mật khẩu](user/sequence_user_quen_mat_khau_otp_p3.png)
- [Quên mật khẩu 3b. Lưu mật khẩu và thu hồi phiên](user/sequence_user_quen_mat_khau_otp_p3b.png)

Ba trang cũ đã thay bằng sáu trang để phân biệt gửi mã, xác minh mã và đổi mật khẩu. Bản vẽ bám quyết định owner 2026-10-27: tài khoản chỉ OAuth nhận email hướng dẫn, không phát OTP; bước xác minh đúng vẫn giữ mã và thời hạn gốc. Khi đổi mật khẩu, PostgreSQL khoá dòng tài khoản và kiểm mốc đổi mật khẩu với mốc phát OTP để chặn lần dùng thứ hai; lỗi ghi DB giữ mã, sau commit mới xoá Redis. Ghi chú chi tiết nằm trong spec trang 1c và 3b.

- [1a. Kiểm tra và lưu bài nộp](user/sequence_user_nop_bai_va_xem_ket_qua_realtime_p1.png)
- [1b. Xếp hàng chấm và trả lời](user/sequence_user_nop_bai_va_xem_ket_qua_realtime_p1b.png)
- [2a. Worker nhận bài và ghim bộ chấm](user/sequence_user_nop_bai_va_xem_ket_qua_realtime_p2.png)
- [2b. Tạo mã và biên dịch](user/sequence_user_nop_bai_va_xem_ket_qua_realtime_p2b.png)
- [3a. Chấm đủ N testcase](user/sequence_user_nop_bai_va_xem_ket_qua_realtime_p3.png)
- [3b. Chốt kết quả và phát sự kiện](user/sequence_user_nop_bai_va_xem_ket_qua_realtime_p3b.png)

Sáu trang mô tả từ lúc lưu `PENDING`, xếp hàng RabbitMQ, worker lấy đúng bản chụp testcase đã ghim, biên dịch qua F3/go-judge, chạy đủ N ca và chốt trạng thái cuối. Kết quả từng ca chỉ phát sau commit; sự kiện miền nằm trong outbox cùng giao dịch chốt bài. Các tên lớp và bridge giữa module là thiết kế dự kiến, có ánh xạ trong ghi chú từng spec.

- [Phân tích bài giải 1. Kiểm bài và bản đã lưu](user/sequence_user_phan_tich_bai_giai_ai_p1.png)
- [Phân tích bài giải 2. Dùng lại hoặc kiểm chi phí](user/sequence_user_phan_tich_bai_giai_ai_p2.png)
- [Phân tích bài giải 3. Gọi AI và lưu kết quả](user/sequence_user_phan_tich_bai_giai_ai_p3.png)

Ba trang F5.1 bám DD mới: bài phải đạt và thuộc người yêu cầu; báo cáo đã có hoặc trúng bản dùng lại được trả trước khi kiểm ngân sách. Bản dùng lại tạo dòng báo cáo cho bài nộp mới nhưng không ghi token. Chỉ khi cần sinh báo cáo mới mới kiểm ngân sách và giới hạn, lấy ngữ cảnh qua cổng F2, gọi AI rồi ghi báo cáo, mức tiêu thụ và sự kiện trong cùng giao dịch. Tên cổng rút ngắn trên hình được ánh xạ trong spec. Không vẽ chi tiết mọi nhánh lỗi; xem DD F5.

- [Phỏng vấn AI 1a. Chọn lối vào và kiểm chi phí](user/sequence_user_phong_van_gia_lap_ai_p1.png)
- [Phỏng vấn AI 1b. Tạo câu mở đầu và lưu phiên](user/sequence_user_phong_van_gia_lap_ai_p1b.png)
- [Phỏng vấn AI 2a. Nhận câu trả lời và giữ hội thoại](user/sequence_user_phong_van_gia_lap_ai_p2.png)
- [Phỏng vấn AI 2b. Phát luồng và chuyển giai đoạn](user/sequence_user_phong_van_gia_lap_ai_p2b.png)
- [Phỏng vấn AI 3. Chấm và kết thúc phiên](user/sequence_user_phong_van_gia_lap_ai_p3.png)

Năm trang F5.2 thể hiện ba lối vào: bài đã đạt, câu hỏi từ kho, hoặc chủ đề tự chọn. Câu mở đầu được tạo xong mới lưu phiên và trả lời đồng bộ; những lượt sau phát dần qua SSE, lưu câu người học trước khi mở luồng và lưu câu AI trước khi báo lượt hoàn tất. Redis chỉ giữ ngữ cảnh tạm; khi thiếu có thể dựng lại từ lịch sử trong DB. Kết thúc sớm vẫn chấm bốn tiêu chí và lưu phiên hoàn tất; bộ nhớ Redis tự hết hạn. Tên lớp chưa hiện thực được ghi rõ là thiết kế dự kiến trong spec.

- [Ôn tập F6 1. Đọc nội dung học](user/sequence_user_on_tap_luyen_tap_ai_p1.png)
- [Ôn tập F6 2a. Kiểm điều kiện và hạn mức](user/sequence_user_on_tap_luyen_tap_ai_p2.png)
- [Ôn tập F6 2b. Nhận xét và lưu lượt luyện](user/sequence_user_on_tap_luyen_tap_ai_p2b.png)
- [Ôn tập F6 3. Tự chấm và xem tiến độ](user/sequence_user_on_tap_luyen_tap_ai_p3.png)

Chế độ học đọc nội dung đã soạn. Chế độ luyện kiểm câu hỏi, tiêu chí, hạn mức giờ và ngân sách trước khi gọi AI; ngân sách bị khóa thì không lưu lượt. Lỗi AI vẫn lưu câu trả lời dưới dạng lượt thất bại; mỗi lượt nộp là một bản ghi riêng. Tự chấm đặt lại lịch ôn từ thời điểm hiện tại theo mức 1/3/7 ngày. Các cổng dữ liệu minh họa trong spec là thiết kế dự kiến.

## A2 — Giảng viên

- [Tạo lớp](gv/sequence_gv_tao_lop_va_ma_moi_p1.png)
- [Tạo mã mời lớp](gv/sequence_gv_tao_lop_va_ma_moi_p2.png)
- [Ghi danh bằng mã mời](gv/sequence_gv_tao_lop_va_ma_moi_p3.png)
- [1a. Đọc học viên trong lớp](gv/sequence_gv_xem_tien_do_lop_p1.png)
- [1b. Tính tiến độ khi đọc](gv/sequence_gv_xem_tien_do_lop_p1b.png)
- [2. Xem hồ sơ một học viên](gv/sequence_gv_xem_tien_do_lop_p2.png)

Luồng tiến độ tuân DD mới: đọc học viên còn ghi danh, lấy bài còn giao từ F2 và kết quả sau mốc giao từ F4, rồi tính tỉ lệ khi đọc. Bảng `class_completion_stats` trong sequence cũ đã bị loại khỏi thiết kế. Hồ sơ một học viên cũng chỉ đọc trong lớp mình phụ trách; nguồn bắt buộc lỗi thì báo lỗi tạm thời thay vì tạo nhãn giả.

- [Giao bài 1a. Kiểm lớp và lưu từng bài](gv/sequence_gv_giao_bai_cho_lop_p1.png)
- [Giao bài 1b. Phát sự kiện sau commit](gv/sequence_gv_giao_bai_cho_lop_p1b.png)
- [Gỡ bài khỏi lớp](gv/sequence_gv_go_bai_khoi_lop_p1.png)
- [Gỡ học viên 1. Giữ lịch sử](gv/sequence_gv_go_hoc_vien_khoi_lop_p1.png)
- [Gỡ học viên 2. Vào lại bằng mã mời](gv/sequence_gv_go_hoc_vien_khoi_lop_p2.png)
- [Sửa lớp](gv/sequence_gv_sua_va_xoa_lop_p1.png)
- [Xóa lớp](gv/sequence_gv_sua_va_xoa_lop_p2.png)
- [Bài nộp lớp 1. Danh sách](gv/sequence_gv_xem_bai_nop_cua_lop_p1.png)
- [Bài nộp lớp 2. Chi tiết](gv/sequence_gv_xem_bai_nop_cua_lop_p1b.png)

Giao bài xử lý từng bài trong giao dịch riêng và lưu sự kiện cùng dòng giao. Gỡ bài và gỡ học viên giữ lịch sử; vào lại lớp dùng dòng ghi danh cũ. Xóa lớp phát sự kiện để kho bài đánh dấu các bài đang giao. Danh sách bài nộp kiểm phạm vi qua cổng F1/F2; chi tiết ngoài phạm vi trả lỗi không tìm thấy và che dữ liệu testcase ẩn.

## A3 — Quản trị viên

30 trang quản trị viên bao phủ tài khoản, quyền, chỉ thị AI, ngân sách, giới hạn, hàng đợi và ngôn ngữ. Các sơ đồ có nguồn DD trong trường `_` của spec.

- [Xem cấu hình ngôn ngữ và giới hạn (1/2)](admin/sequence_admin_cau_hinh_ngon_ngu_gioi_han_p1.png)
- [Lưu cấu hình ngôn ngữ và giới hạn (2/2)](admin/sequence_admin_cau_hinh_ngon_ngu_gioi_han_p2.png)
- [Xem chỉ thị AI đang chạy và bản nháp (1/4)](admin/sequence_admin_cau_hinh_prompt_ai_p1.png)
- [Lưu bản nháp chỉ thị AI (2/4)](admin/sequence_admin_cau_hinh_prompt_ai_p2.png)
- [Phát hành chỉ thị AI có chọn lọc (3/4)](admin/sequence_admin_cau_hinh_prompt_ai_p3.png)
- [Xem lịch sử và chép chỉ thị cũ vào nháp (4/4)](admin/sequence_admin_cau_hinh_prompt_ai_p4.png)
- [Mở khóa ngân sách token (1/5)](admin/sequence_admin_dat_han_muc_va_gioi_han_ai_p1.png)
- [Đổi hạn mức gọi AI và thời gian chờ (2/5)](admin/sequence_admin_dat_han_muc_va_gioi_han_ai_p2.png)
- [Đổi ngưỡng cảnh báo ngân sách (3/5)](admin/sequence_admin_dat_han_muc_va_gioi_han_ai_p3.png)
- [Quản lý đơn giá token theo mô hình (4/5)](admin/sequence_admin_dat_han_muc_va_gioi_han_ai_p4.png)
- [Đặt trần sinh testcase cho từng bài (5/5)](admin/sequence_admin_dat_han_muc_va_gioi_han_ai_p4b.png)
- [Quản trị viên đặt lại mật khẩu](admin/sequence_admin_dat_lai_mat_khau.png)
- [Đọc ngân sách token hiện tại (1/5)](admin/sequence_admin_dat_ngan_sach_token_p1.png)
- [Đặt hạn mức token tháng (2/5)](admin/sequence_admin_dat_ngan_sach_token_p1b.png)
- [Phát hiện lượt gọi AI bất thường (3/5)](admin/sequence_admin_dat_ngan_sach_token_p2.png)
- [Xem cảnh báo ngân sách và bất thường (4/5)](admin/sequence_admin_dat_ngan_sach_token_p2b.png)
- [Đánh dấu đã xem cảnh báo bất thường (5/5)](admin/sequence_admin_dat_ngan_sach_token_p3.png)
- [Xem ma trận phân quyền (1/2)](admin/sequence_admin_doi_ma_tran_phan_quyen_p1.png)
- [Đổi một ô quyền và áp dụng ngay (2/2)](admin/sequence_admin_doi_ma_tran_phan_quyen_p2.png)
- [Tìm tài khoản và vai trò có thể gán (1/2)](admin/sequence_admin_doi_vai_tro_nguoi_dung_p1.png)
- [Đổi vai trò và bảo vệ quản trị viên cuối (2/2)](admin/sequence_admin_doi_vai_tro_nguoi_dung_p2.png)
- [Xem trạng thái hàng đợi và bài chờ (1/3)](admin/sequence_admin_giam_sat_hang_doi_p1.png)
- [Tác vụ nền xử lý bài nộp treo (2/3)](admin/sequence_admin_giam_sat_hang_doi_p2.png)
- [Tạm dừng hoặc tiếp tục nhận bài chấm (3a/3)](admin/sequence_admin_giam_sat_hang_doi_p3.png)
- [Cổng nền áp dụng trạng thái hàng đợi (3b/3)](admin/sequence_admin_giam_sat_hang_doi_p3b.png)
- [Khóa tài khoản và thu hồi phiên (1/2)](admin/sequence_admin_khoa_mo_khoa_tai_khoan_p1.png)
- [Khóa tài khoản: làm mới quyền và thông báo (2a/2)](admin/sequence_admin_khoa_mo_khoa_tai_khoan_p2.png)
- [Mở khóa tài khoản (2b/2)](admin/sequence_admin_khoa_mo_khoa_tai_khoan_p2b.png)
- [Tạo tài khoản và nhật ký (1/2)](admin/sequence_admin_them_tai_khoan.png)
- [Gửi mật khẩu tạm (2/2)](admin/sequence_admin_them_tai_khoan_p2.png)

Trần sinh testcase thuộc `problem-bank`; giới hạn gọi AI và ngân sách thuộc `ai-review`. Quét hàng đợi và phát hiện lượt AI bất thường do tác vụ nền khởi phát.

## Dùng chung A2/A3 — Soạn đề và quản lý testcase

- [Soạn đề 1. Tạo bản nháp](shared/sequence_shared_soan_de_va_dac_ta_ham_p1.png)
- [Đặc tả 2a. Kiểm và lưu hai mô hình nộp](shared/sequence_shared_soan_de_va_dac_ta_ham_p2.png)
- [Đặc tả 2b. Làm mới bộ nhớ và báo thay đổi](shared/sequence_shared_soan_de_va_dac_ta_ham_p2b.png)
- [Xuất bản 3a. Kiểm checklist và chạy đáp án mẫu](shared/sequence_shared_soan_de_va_dac_ta_ham_p3.png)
- [Xuất bản 3b. Lưu trạng thái, nhật ký và sự kiện](shared/sequence_shared_soan_de_va_dac_ta_ham_p3b.png)

Luồng soạn đề bám DD F2/F3: một chữ ký hàm dùng chung cho Java/C++/Python cùng định dạng Standard I/O; kiểu ngoài lược đồ được lưu kèm cảnh báo và ẩn Bọc hàm. Xuất bản đòi ít nhất 8 testcase đã duyệt, 2 Sample, 2 ví dụ, đặc tả đầy đủ và đáp án mẫu qua mọi ca. Ghi nhật ký cùng giao dịch; sự kiện phát sau commit.

- [1a. Cấp đường tải tạm và tải trực tiếp](shared/sequence_shared_quan_ly_testcase_p1.png)
- [1b. Xác nhận đủ tệp](shared/sequence_shared_quan_ly_testcase_p1b.png)
- [1c. Lưu cả lô testcase](shared/sequence_shared_quan_ly_testcase_p1c.png)
- [2a. Sửa testcase và kiểm phiên bản](shared/sequence_shared_quan_ly_testcase_p2.png)
- [2b. Chụp bộ testcase mới](shared/sequence_shared_quan_ly_testcase_p2b.png)
- [3a. Kiểm điều kiện sinh testcase AI](shared/sequence_shared_quan_ly_testcase_p3.png)
- [3b. AI viết script, máy chạy lấy output](shared/sequence_shared_quan_ly_testcase_p3b.png)
- [3c. Lưu testcase AI ở trạng thái nháp](shared/sequence_shared_quan_ly_testcase_p3c.png)

DD hiện quy định trình duyệt tải trực tiếp lên MinIO bằng URL tạm; máy chủ chỉ lưu testcase sau khi xác nhận mọi tệp. Sửa testcase của bài đã công bố làm đổi bộ ca đã duyệt thì tạo snapshot bất biến và tăng phiên bản, kể cả chưa có lượt nộp. AI chỉ viết script sinh input; output lấy từ đáp án mẫu chạy thật, sau đó lưu nháp. Adapter Redis và một số kiểm tra giới hạn được lược khỏi ảnh để dễ đọc; các bước bắt buộc vẫn ghi trong DD và note của spec.

## Kiểm tra đang áp dụng

- `python 08-diagram/sequence/tools/check_migrated_sequences.py`: kiểm ánh xạ trang gốc, tên lifeline và lời gọi tắt qua tầng ở các trang đã migrate.
- `lint_drawio.py`: các trang đã vẽ lại có 0 `FAIL`; các trang có `WARN` cỡ chữ khi ép vào vùng rộng 6,53 inch của A4. PNG 2x đã mở xem, còn khi in các trang bị cảnh báo cần A3 để chữ đủ lớn.
- `check_catalog.py`: `covers` khớp catalog Markdown/HTML.
- Mỗi PNG được mở xem để kiểm chữ tiếng Việt, tên lifeline, thứ tự mũi tên và nhánh.
