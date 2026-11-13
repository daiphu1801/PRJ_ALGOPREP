# Run Code/Sample — local single-instance

Hai endpoint cần JWT STUDENT:

- `GET /api/v1/judge/problems/{problemId}/execution-limits`: không truyền `language` thì trả ba ngôn ngữ; có thể truyền JAVA/CPP/PYTHON không phân biệt hoa thường. Ngôn ngữ tắt vẫn trả giới hạn và `enabled=false`.
- `POST /api/v1/judge/problems/{problemId}/run-sample`: JSON gồm `language`, `submissionModel`, `sourceCode`. Mã tối đa 65.536 byte UTF-8, không nhận tệp riêng. Owner lấy từ JWT; không có trường chọn người khác.

Run Code gọi cổng F2 chỉ lấy Sample đã duyệt trong SQL; F3 prepare với specVersion thật, compile/match qua execution kernel hiện có. `COMPILE_ERROR` trả HTTP 200, không có testcase result; lỗi testcase thông thường vẫn chạy hết các Sample. Không có submission ID, không ghi submission/results/run tokens/outbox và không phát STOMP.

Quota Redis `judge:run-sample:rate:<userId>` là sorted set; Lua dùng TIME của Redis và cửa sổ trượt 60 phút, nhận tối đa 60 lượt. `429 JDG-107` có `details.retryAfterSeconds`. Redis lỗi thì WARN cố định, tiếp tục dưới semaphore. `429 JDG-502` khi hết 8 suất; hoàn đúng request vừa giữ. Compile error tính lượt; `503 JDG-501` hoàn quota. Hoàn quota là best-effort khi Redis đang lỗi.

`judge.run-sample.max-concurrent` mặc định 8 (chỉ nhận 1–8), theo quyết định single-instance hiện có. `judge.run-sample.timeout-ms` mặc định 60000 (chỉ nhận 1–60000); deadline bao gồm đọc metadata, chuẩn bị và compile/run. Hủy Future và interrupt khi hết hạn; trạng thái hủy độc lập với cờ interrupt. Hoàn quota một lần cả khi Future hết hạn sát lúc tác vụ hoàn thành. Suất đồng thời chỉ được trả ở finally của tác vụ, tránh nhận lượt mới trong lúc tác vụ cũ chưa dừng. Sandbox vẫn ép thời gian/bộ nhớ/output/process; HTTP timeout không thay thế sandbox limits.

Giới hạn đọc từ cấu hình thật một lần/lượt, HALF_UP theo hệ số ngôn ngữ. Compile budget lấy compile_timeout_sec; process limit 60 khi bật, 0 khi tắt. Network-enabled đang hoãn: cấu hình bật mạng làm lượt Run Code bị từ chối an toàn, không thực thi với một chế độ mạng khác với cấu hình.

Chỉ có Sample output; actualOutput và stderr cắt 4096 ký tự bởi kernel. Stderr cần STANDARD_IO, cờ hiện tại bật và verdict khác AC. FUNCTION_WRAPPER luôn ẩn stderr; compile message qua F3 tối đa 10000 ký tự. Lỗi infrastructure không trả source/SQL/raw engine cause.

Chưa nối FE Run Code, modal kết quả, giới hạn hiển thị hay browser. Full regression chờ mốc clear theo chỉ đạo owner; mỗi lát chỉ chạy test code mới/sửa và hợp đồng liên quan trực tiếp. Chưa mở production nhiều API instance; cần thay semaphore bằng guard dùng chung trước mốc đó.
