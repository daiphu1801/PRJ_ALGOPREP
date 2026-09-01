# F4 — Điều phối và giao tiếp judge engine (`judge-orchestration`)

> Tách ra từ `01-rd/req/req.md` ngày 2026-08-31 để dễ đọc (theo Bounded Context). Nội dung dưới đây được
> chuyển nguyên văn từ `req.md` (không đổi ý nghĩa, không thêm/bớt), chỉ hạ heading `###` xuống `#`.

Gọi qua cổng ra trung lập theo engine (`JudgeExecutionPort`), adapter mặc định là go-judge —
`DEC-2026-0823-go-judge-default-engine`.

- **Tiếp nhận và chạy thử:**
  - Tiếp nhận bài nộp: ghi trạng thái `PENDING`, đẩy vào hàng đợi RabbitMQ, phản hồi ngay — **không chờ
    kết quả chấm** (F4-01, `overview.md` mục 1.E). **Bổ sung 2026-08-24** (`06-plan/PROTOTYPE_DEBT.md` mục
    2.12, đối chiếu `09-layoutBase/Workspace giải bài.dc.html`): ngoài gõ trực tiếp vào Monaco Editor, người
    học nộp bài được bằng cách **tải file mã nguồn lên** — nội dung file nạp vào editor rồi đi qua đúng cùng
    luồng F4-01, không phải một kênh nộp bài riêng. Giới hạn định dạng file theo đúng phần mở rộng của ba
    ngôn ngữ (`.java`, `.cpp`, `.py`) và giới hạn kích thước file `[SoT: Suy luận]`, chốt số cụ thể ở BD.
  - Chạy thử với testcase Sample, không ghi nhận vào tiến độ (F4-02).
- **Chấm từng testcase — chạy hết, không dừng sớm:**
  - Gọi `JudgeExecutionPort` một lần cho mỗi testcase, không dồn batch (F4-03). **Amendment 2026-08-31**
    (`DEC-2026-0831-partial-score-testcase-ratio`): **bỏ fail-fast** — testcase trước sai hoặc lỗi không còn
    làm dừng việc gọi các testcase sau; mọi bài nộp đều chạy hết toàn bộ N testcase để có đủ dữ liệu tính
    điểm tỷ lệ (F4-13). `F4-04` **không còn hiệu lực** — xem lý do và đánh đổi tài nguyên/thời gian ở
    `DEC-2026-0831-partial-score-testcase-ratio`.
  - **F4-13 — Điểm tỷ lệ testcase.** Sau khi chạy hết N testcase, tính `điểm = số testcase Pass / N`, hiển
    thị dạng phân số trên thang **1 điểm mỗi bài toán** (ví dụ "0.6/1"), không phụ thuộc số testcase của bài
    (15 hay 45 testcase vẫn quy về thang 1đ). **Không thay thế, không ảnh hưởng** verdict `Accepted`/`Wrong
    Answer` nhị phân hiện có — `Accepted` vẫn là Pass toàn bộ N testcase như trước, vẫn là điều kiện kích
    hoạt F5 (F5.1/F5.2), tính tỉ lệ chấp thuận F1-07, và ngưỡng "Bài cần chú ý" (AC &lt; 30%) của
    `problem_management`. Điểm tỷ lệ chỉ là một số hiển thị **thêm** cho người học ở `submission_result`,
    tính riêng cho từng lần nộp. Khác hoàn toàn với "chấm điểm từng phần theo trọng số do người soạn đề khai
    báo" đã bị loại ở `06-plan/PROTOTYPE_DEBT.md` mục 2.6 phần 1 (2026-08-24) — điểm tỷ lệ ở đây **đều nhau
    giữa mọi testcase, tính tự động**, không cần giảng viên khai báo trọng số nào, nên quyết định bỏ cột
    "Điểm"/khối trọng số khỏi UI `problem_authoring` (Q3, `07-review/rd_review_closure_260831.html`) vẫn
    đúng và không bị đảo lại.
- **Webhook và chống trùng — chỉ khi có adapter bất đồng bộ:**
  - Với adapter mặc định (go-judge, đồng bộ), kết quả trả về ngay trong lời gọi, không có callback nên
    không cần chức năng này. Nếu một adapter bất đồng bộ được cắm vào sau (ví dụ Judge0), xác thực webhook
    bằng token bí mật riêng theo từng bài nộp (F4-05) và chống trùng theo token của engine đó (F4-06) là
    chi tiết nội bộ của adapter, không phải yêu cầu ở port. Bất biến **không bao giờ ghi đè một trạng thái
    đã là trạng thái cuối** (`glossary.md` mục 3) giữ nguyên bất kể adapter.
  - Timeout sweep: quét bài nộp treo quá ngưỡng thời gian (F4-07) — bản nhẹ, dựa vào ack/nack và redelivery
    của RabbitMQ, không cần chủ động hỏi lại một hệ ngoài như khi dùng Judge0. **Amendment 2026-08-31** (lấp
    Câu hỏi mở Q4 của `01-rd/req/user_stories.md`, `DEC-2026-0831-outside-screens-closures`): ngưỡng "treo"
    = **5 phút** không nhận được cập nhật trạng thái nào cho một job `[SoT: Suy luận]`, BD/DD chỉnh được.
- **Thời gian thực:**
  - Đẩy trạng thái từng testcase qua WebSocket (STOMP) theo kênh riêng của từng bài nộp, ngay sau mỗi lần
    gọi cổng ra trả kết quả (F4-08).
- **~~Vận hành — Chấm lại (re-judge), actor A2/A3~~ — ĐÃ LOẠI BỎ KHỎI PHẠM VI (2026-08-28, qua hỏi trực
  tiếp chủ dự án).** Trước đó mô tả một quy trình chấm lại nhiều bước (F4-09a chọn phạm vi, F4-09b dry-run,
  F4-09c tuỳ chọn khi chạy, F4-09d điều khiển phiên/bất biến dữ liệu, F4-09e audit trail), gắn với màn
  `admin_rejudge` (`09-layoutBase/Admin - Chấm lại.dc.html`) và Function `REJUDGE_MANAGEMENT`. **Quyết định:**
  bỏ hẳn cơ chế chấm lại tự động — nếu một testcase bị lỗi, học viên báo cho giảng viên, giảng viên tự sửa
  testcase; **các lượt nộp cũ đã chấm theo testcase lỗi giữ nguyên kết quả cũ, không có cơ chế tự động chấm
  lại hàng loạt.** Lý do: giảm chức năng không cần thiết cho phạm vi đồ án — cơ chế báo lỗi thủ công (qua
  `submission_result`/kênh liên hệ giảng viên) đã đủ xử lý trường hợp thực tế. Mã `F4-09a` tới `F4-09e` và
  màn `admin_rejudge` **không còn hiệu lực** — xem `.nexa/control/decision-registry.md` →
  `DEC-2026-0828-remove-rejudge-scope` để biết đầy đủ các file bị ảnh hưởng.
- Giám sát hàng đợi và tình trạng cụm judge engine (F4-10); cấu hình ngôn ngữ và giới hạn tài nguyên
  (F4-11) — cả hai thuộc actor A3. **Amendment 2026-08-31** (lấp Câu hỏi mở Q1/Q2 của
  `01-rd/screens/admin/admin_queue_monitor.md` và Q1 của `01-rd/screens/admin/admin_language_config.md`,
  `DEC-2026-0831-judge-orchestration-ops-details`):
  - **F4-10 bao gồm điều khiển vận hành cơ bản trên cụm**, không chỉ xem: tạm dừng/tiếp tục tiêu thụ hàng
    đợi, bật/tắt tự động mở rộng worker — hai điều khiển này là phần tự nhiên của một bảng giám sát vận
    hành, không cần mã riêng.
  - **F4-11 bao gồm 3 tham số sandbox go-judge**: cho phép/chặn truy cập mạng (mặc định chặn), giới hạn số
    tiến trình con, có trả `stderr` cho người học hay không — liệt kê rõ ở đây để BD/DD của
    `judge-orchestration` có căn cứ, không cần mã riêng.
  - **Không có khái niệm "kỳ thi"/"bài thi" nào trong hệ thống.** Nhãn "Ưu tiên bài thi" (toggle điều khiển
    cụm) và ghi chú "Tắt trong mọi kỳ thi" (sandbox mạng) trong prototype là **nhãn phân loại độ ưu tiên**,
    không phải một luồng nghiệp vụ "quản lý kỳ thi" riêng — đổi thành nhãn trung tính (ví dụ "Ưu tiên cao")
    khi dựng UI thật, không mở RD mới cho "chế độ thi".
  - **F4-12 — Chỉ số "Beats" trên trang kết quả nộp bài.** Bổ sung 2026-08-25 (chốt qua
    `01-rd/screens/users/submission_result.md` Câu hỏi mở Q4, chốt theo RD): với một bài nộp `Accepted`,
    tính tỉ lệ phần trăm bài nộp khác **nhanh hơn hoặc bằng** (theo runtime) chậm hơn bài nộp hiện tại, trong
    tập tất cả bài nộp `Accepted` của **cùng bài toán và cùng ngôn ngữ lập trình** (không so giữa các ngôn
    ngữ khác nhau vì tốc độ thực thi không tương đương). Chỉ hiển thị khi verdict là `Accepted`; rỗng (`—`)
    cho mọi verdict khác. Công thức và ngưỡng làm tròn cụ thể chốt khi viết BD/DD của `judge-orchestration`.
