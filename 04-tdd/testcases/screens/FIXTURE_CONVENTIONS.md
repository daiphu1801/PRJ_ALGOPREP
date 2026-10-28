# Fixture và cách chạy lại testcase màn

Đây là quy ước thiết kế cho 40 màn; chưa tạo seed hoặc thực thi. Dùng cùng [bộ chọn smoke/E2E](../PHASE5_SMOKE_E2E_SELECTION.md) khi dựng môi trường.

- Mỗi biến thể lỗi/biên trong một testcase chạy từ baseline sạch riêng. Không chạy tiếp biến thể sau trên trạng thái đã bị biến thể trước thay đổi, trừ khi Steps chỉ rõ chuỗi thao tác.
- Giá trị trong `Test data` của ca ghi đè fixture chung của file. Định danh như `P-201`, `SUB-901`, `c-101` là alias, không phải giá trị hợp lệ gửi vào API UUID. Nếu ca đã ghi UUID cụ thể thì dùng nguyên UUID đó.
- Với alias chưa có UUID, seed và browser stub cùng dùng `uuid.uuid5(uuid.NAMESPACE_URL, "algoprep-qa-screen/{screenId}/{alias}")`; giữ nguyên hoa/thường của alias. Lưu bảng ánh xạ alias → UUID vào evidence của run. Với hành trình qua nhiều màn, dùng chung `screenId` của fixture hành trình cho mọi bước, không sinh lại ID khi chuyển màn.
- Actor mặc định Alice là STUDENT có dữ liệu của chính mình; Bob là STUDENT đối chứng ngoài quyền. `ins-a`/`ins-b` phụ trách các lớp riêng được nêu. `adm-a` có quyền đúng hành động; `adm-b` là ADMIN còn hoạt động cho ca tự tác động. `viewer-a` là ADMIN chỉ đọc, trừ ca ghi rõ actor thiếu quyền giám sát hoặc không phải ADMIN. Seed phải ghi role được gán và Function:Action thật; không dùng tên actor làm bằng chứng quyền.
- Baseline tích hợp phải hợp lệ: bài PUBLISHED có tối thiểu 8 ca duyệt/2 Sample; ACCEPTED có mọi ca đạt; F6 câu hỏi chỉ ACTIVE/RETIRED, feedback chỉ COMPLETED/FAILED. DTO cố ý trái hợp đồng chỉ dùng ở test phòng vệ FE được ghi rõ, không coi là phản hồi hợp lệ của server.
- Dùng clock cố định trong ca, hoặc `2026-10-28T12:00:00+07:00` khi ca không quy định. Timestamp biên tính bằng instant, không dựa vào thời điểm máy người chạy. Chờ event/job theo correlation ID và điều kiện đã ghi, không dùng sleep tuỳ ý để tuyên bố thành công.
- Nháp Workspace và lịch sử trước khi áp dụng AI lưu trong trình duyệt; fault tại adapter/localStorage. Chỉ xác minh DB ở phần API thực sự sở hữu dữ liệu. Không tạo endpoint hay bảng draft để đáp ứng một fixture giả.
- FE mock chứng minh UI, route, số request và xử lý dữ liệu đã cho. API/DB/event/job/audit/quyền cần build và hạ tầng tương ứng. Chạy mock và tích hợp có run record riêng.
- Cleanup khôi phục đúng role/quyền/cấu hình đã đổi và xoá dữ liệu theo UUID của run; không xoá dữ liệu toàn môi trường. Ca mất phiên dùng `adm-b`/setup channel riêng để khôi phục. Evidence chỉ chứa canary và dữ liệu test, không chứa token/OTP/mật khẩu thật.

Trước khi chạy phải materialize seed, mail sink, fault controls, storage isolation và build/version theo ca. Đây là cổng thực thi; không thay thế oracle đã chốt trong DD/TDD.
