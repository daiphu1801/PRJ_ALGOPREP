## 1. Đặc tả Yêu cầu Chức năng (Functional Requirements - PRD)

> **Viết lại 2026-08-23.** Bản trước là nội dung của dự án cũ NestGame v2 (module IAM/Catalog/SaveState,
> OAuth Google, chơi game retro) — nợ tài liệu đã ghi ở `01-rd/README.md` mục 3 (bản sử). Bản này viết theo
> đúng sáu phân hệ **F1-F6** của `README.md` mục 4, chi tiết hoá từ bảng chức năng có mã `Fx-nn` ở
> `01-rd/overview/system_survey.md` mục 5. Mỗi yêu cầu dưới đây trỏ lại mã đó để truy vết sang BD, DD,
> `04-tdd/` và test — **không đánh số lại**, dùng nguyên mã `Fx-nn` làm khoá tham chiếu.
>
> Đây là yêu cầu **chức năng ở mức đặc tả nghiệp vụ** — không phải lược đồ database (`02-bd/database/`),
> không phải hợp đồng API (`03-dd/api/`). Thuật ngữ dùng đúng `01-rd/overview/glossary.md`.

NestGame v2 → thay bằng: AlgoPrep gồm **sáu phân hệ chức năng chính** theo Bounded Context
(`.nexa/domain-registry.json`), mỗi phân hệ có yêu cầu kỹ thuật và trải nghiệm người dùng riêng.

### F1 — Danh tính và phân quyền (`identity`)

- **Đăng ký và xác thực:**
  - Đăng ký tài khoản bằng email và mật khẩu (F1-01). Vai trò mặc định khi đăng ký là `STUDENT` (F1-05).
  - Đăng nhập cấp **Access Token** thời hạn ngắn và **Refresh Token** trong cookie HTTP-Only (F1-02,
    `README.md` mục 4-F1). Client tự làm mới Access Token bằng Refresh Token khi gặp `401` (F1-03).
  - Đăng xuất vô hiệu hoá Refresh Token của phiên hiện tại (F1-04).
- **Phân quyền theo vai trò (RBAC):**
  - Ba vai trò: `STUDENT` (A1) · `INSTRUCTOR` (A2) · `ADMIN` (A3) (F1-05, `overview.md` mục 1.I). Kiểm quyền
    ở **tầng ứng dụng**, không chỉ ở giao diện, và kiểm cả quyền sở hữu dữ liệu — một `STUDENT` không đọc
    được bài nộp hay phiên phỏng vấn của người khác.
  - **`ADMIN` và `INSTRUCTOR` là hai vai trò tách riêng, không lồng nhau** — chốt 2026-08-23 theo
    `06-plan/PROTOTYPE_DEBT.md` mục 1.2, thay cho đề xuất suy luận trước đó. `ADMIN` **không** tự động có
    quyền soạn nội dung của `INSTRUCTOR`; quyền của từng vai trò với từng nhóm chức năng quản trị/nội dung
    do **ma trận phân quyền** ở F1-10 quyết định, không phải do phân cấp vai trò.
- **Hai lớp quyền — đừng nhầm lẫn:**
  - **Lớp 1 — quyền học tập cơ bản.** Mọi tài khoản `STUDENT` có sẵn toàn bộ quyền dùng F2 (xem/nộp bài),
    F3/F4 (giải bài, chạy thử, nộp bài), F5 (phân tích bài giải, phỏng vấn giả lập), F6 (ôn câu hỏi) ngay
    khi đăng ký — đây là quyền theo **vai trò** (F1-05), không đi qua ma trận phân quyền ở F1-10. Bật/tắt ô
    trong ma trận phân quyền **không bao giờ** ảnh hưởng tới quyền học tập cơ bản này.
  - **Lớp 2 — ma trận phân quyền quản trị/nội dung.** Chỉ gác các thao tác thuộc nhóm quản lý (soạn đề,
    quản lý lớp, quản lý người dùng, cấu hình AI, giám sát hệ thống...) — xem F1-10 tới F1-14.
- **Trang tiến độ cá nhân:**
  - Bài đã giải theo chủ đề (F1-06), tỉ lệ chấp thuận — số bài nộp đạt `Accepted` trên tổng bài nộp
    (F1-07), lịch sử phỏng vấn mở lại được rubric của phiên cũ (F1-08).
- **Quản lý hồ sơ:**
  - Sửa thông tin cá nhân (F1-09) — `[SoT: Suy luận — README.md không nêu, nhưng đăng ký mà không sửa được
gì là thiếu]`.
- **Ma trận phân quyền Role × Function × Action** — chốt 2026-08-23 theo `06-plan/PROTOTYPE_DEBT.md` mục
  1.2. Giải quyết việc A2 (Giảng viên) và A3 (Quản trị viên) dùng **chung một khu Admin** trên giao diện,
  nhưng được tách quyền thật ở tầng ứng dụng thay vì chỉ ẩn/hiện menu:
  - **F1-10 — Ma trận phân quyền theo vai trò.** `ADMIN` xem và chỉnh ma trận quyền `CREATE`/`READ`/
    `UPDATE`/`DELETE` cho từng cặp (vai trò, chức năng quản trị). Đổi một ô có hiệu lực ngay, không cần
    khởi động lại dịch vụ.
  - **F1-11 — Function và Action là dữ liệu seed cố định, chỉ đọc trên giao diện; Role tạo/sửa/xoá được.**
    Lý do: mỗi `FUNCTION:ACTION` phải có một `@PreAuthorize` tương ứng trong mã nguồn mới có tác dụng thật
    — cho phép thêm Function tự do trên giao diện là tạo ra một ô tích không gác cửa gì cả. Không xoá được
    vai trò hệ thống (`STUDENT`, `INSTRUCTOR`, `ADMIN`) và không xoá được vai trò đang có người dùng.
  - **F1-12 — Danh sách chức năng quản trị/nội dung nằm trong phạm vi ma trận** (đề xuất khởi điểm,
    `[SoT: Suy luận]` — chốt số lượng chính xác khi viết BD):
    `PROBLEM_AUTHORING` (F2-01→04) · `TESTCASE_MANAGEMENT` (F2-05→09) · `CLASS_MANAGEMENT` (F2-12) ·
    `USER_MANAGEMENT` (F1-13) · `JUDGE_QUEUE_MONITOR` (F4-10) · `AI_CONFIG` (F5-23) ·
    `AI_TOKEN_BUDGET` (F5-21) · `SYSTEM_AUDIT_LOG` (F1-14) · `INTERVIEW_BANK_MANAGEMENT` (F6-11) ·
    `PERMISSION_MATRIX` (chính F1-10 — tự tham chiếu, mặc định chỉ `ADMIN` có toàn quyền).
  - **F1-13 — Quản lý tài khoản người dùng.** `ADMIN` đổi vai trò, khoá/mở khoá tài khoản, reset mật khẩu
    của người dùng khác — gác bởi `USER_MANAGEMENT` trong ma trận F1-10. Actor A3.
  - **F1-14 — Mọi thay đổi ma trận phân quyền và mọi thao tác quản trị đều ghi vào Nhật ký hệ thống**, kèm
    ai đổi, đổi gì, đổi lúc nào — không có ngoại lệ cho chính thao tác đổi quyền.

### F2 — Ngân hàng bài toán và testcase (`problem-bank`)

- **Soạn đề bài (A2):**
  - Soạn đề bài bằng Markdown kèm công thức LaTeX (F2-01); phân loại theo độ khó và chủ đề (F2-02).
  - Khai báo đặc tả bài toán: chữ ký hàm theo từng ngôn ngữ trong ba ngôn ngữ (Java, C++, Python), kiểu
    tham số và kiểu trả về (F2-03) — đây là **đầu vào bắt buộc** cho bộ sinh mã bọc hàm (F3).
  - Khai báo chiến lược so khớp kết quả cho bài toán: `EXACT` · `TRIMMED` · `EPSILON` · `UNORDERED_SET`
    (F2-04, `glossary.md` mục 2).
- **Testcase:**
  - Hai loại: **Sample** — công khai, dùng cho Chạy thử (F2-05); **Hidden** — ẩn, dùng cho Nộp bài (F2-06).
  - Tải lên bộ testcase theo lô; bộ lớn lưu trên MinIO (F2-07).
  - **Chống rò rỉ testcase ẩn** (F2-08): phản hồi chỉ trả **trạng thái và chỉ số** của testcase sai, không
    trả input và không trả diff chi tiết — ràng buộc bảo mật nghiệp vụ, áp dụng cho mọi API và log của
    luồng nộp bài (`README.md` mục 4-F2).
  - Phiên bản hoá bộ testcase để Re-judge biết chấm lại theo phiên bản nào (F2-09).
- **Giới hạn tài nguyên:**
  - Giới hạn thời gian và bộ nhớ theo bài, kèm **hệ số nhân theo ngôn ngữ** — Java chậm hơn C++ nên cùng
    một bài phải khác hệ số (F2-10).
- **Khám phá và quản lý lớp:**
  - Tìm kiếm và lọc bài toán theo chủ đề, độ khó, trạng thái đã giải (F2-11, actor A1).
  - Giao bài tập theo lớp (F2-12, actor A2).

### F3 — Bộ sinh mã bọc hàm (`harness`) — trọng tâm kỹ thuật

Phân hệ cho phép mô hình bọc hàm hoạt động trên judge engine (go-judge mặc định) — vốn chỉ nhận stdin/stdout
(`overview.md` mục 1.G). Chi tiết codegen theo ngôn ngữ, ánh xạ lỗi biên dịch và đường lùi: xem
`.claude/skills/dd-generation/references/harness-codegen.md` khi viết DD.

- **Lược đồ kiểu dữ liệu độc lập ngôn ngữ** (F3-01): nguyên thuỷ, chuỗi, mảng nhiều chiều, danh sách lồng
  nhau, danh sách liên kết, cây nhị phân.
- **Sinh mã đa ngôn ngữ từ một đặc tả duy nhất** cho cả ba ngôn ngữ: đọc dữ liệu vào (F3-02), gọi hàm người
  dùng (F3-03), in kết quả (F3-04).
- **Tiêm mã và đóng gói:** ghép mã người dùng vào vùng chèn của mã khung (F3-05), đóng gói theo đúng định
  dạng adapter đang dùng yêu cầu trước khi gửi qua `JudgeExecutionPort` (F3-06; không hardcode Base64 —
  `DEC-2026-0823-go-judge-default-engine`).
- **So khớp kết quả linh hoạt:** exact (F3-07), chuẩn hoá khoảng trắng (F3-08), số thực theo sai số epsilon
  (F3-09), tập hợp không xét thứ tự (F3-10).
- **Ánh xạ lỗi biên dịch:** trả lỗi về đúng dòng trong mã người dùng (F3-11); **che giấu hoàn toàn** lỗi
  thuộc phần mã harness — người học không được thấy mã hệ thống (F3-12).
- **Đường lùi có chủ ý:** bài toán có kiểu dữ liệu mà lược đồ chưa phủ thì chuyển sang mô hình Standard I/O
  (F3-13, actor A2) — phương án xử lý rủi ro `README.md` mục 7, không phải chống chế lúc bí.

### F4 — Điều phối và giao tiếp judge engine (`judge-orchestration`)

Gọi qua cổng ra trung lập theo engine (`JudgeExecutionPort`), adapter mặc định là go-judge —
`DEC-2026-0823-go-judge-default-engine`.

- **Tiếp nhận và chạy thử:**
  - Tiếp nhận bài nộp: ghi trạng thái `PENDING`, đẩy vào hàng đợi RabbitMQ, phản hồi ngay — **không chờ
    kết quả chấm** (F4-01, `overview.md` mục 1.E).
  - Chạy thử với testcase Sample, không ghi nhận vào tiến độ (F4-02).
- **Chấm từng testcase và dừng sớm:**
  - Gọi `JudgeExecutionPort` một lần cho mỗi testcase, không dồn batch (F4-03); testcase trước sai hoặc lỗi
    theo điều kiện dừng thì **dừng gọi testcase sau** (fail-fast, F4-04) — tiết kiệm tài nguyên và phản hồi
    nhanh hơn.
- **Webhook và chống trùng — chỉ khi có adapter bất đồng bộ:**
  - Với adapter mặc định (go-judge, đồng bộ), kết quả trả về ngay trong lời gọi, không có callback nên
    không cần chức năng này. Nếu một adapter bất đồng bộ được cắm vào sau (ví dụ Judge0), xác thực webhook
    bằng token bí mật riêng theo từng bài nộp (F4-05) và chống trùng theo token của engine đó (F4-06) là
    chi tiết nội bộ của adapter, không phải yêu cầu ở port. Bất biến **không bao giờ ghi đè một trạng thái
    đã là trạng thái cuối** (`glossary.md` mục 3) giữ nguyên bất kể adapter.
  - Timeout sweep: quét bài nộp treo quá ngưỡng thời gian (F4-07) — bản nhẹ, dựa vào ack/nack và redelivery
    của RabbitMQ, không cần chủ động hỏi lại một hệ ngoài như khi dùng Judge0.
- **Thời gian thực:**
  - Đẩy trạng thái từng testcase qua WebSocket (STOMP) theo kênh riêng của từng bài nộp, ngay sau mỗi lần
    gọi cổng ra trả kết quả (F4-08).
- **Vận hành:**
  - Chấm lại (re-judge) theo một phiên bản bộ testcase (F4-09, actor A2/A3).
  - Giám sát hàng đợi và tình trạng cụm judge engine (F4-10); cấu hình ngôn ngữ và giới hạn tài nguyên
    (F4-11) — cả hai thuộc actor A3.

### F5 — Phân hệ AI (`ai-review`)

Kích hoạt **sau khi** bài nộp đạt `Accepted`. Hai chức năng độc lập, người dùng chủ động chọn
(`README.md` mục 4-F5).

**F5.1 — Phân tích bài giải (một lượt, không hội thoại):**

- Yêu cầu phân tích bài giải vừa nộp (F5-01).
- Phân tích độ phức tạp thời gian và bộ nhớ thực tế kèm lập luận (F5-02); đối chiếu với độ phức tạp tối ưu
  đã biết của bài toán, gợi ý hướng tiếp cận tốt hơn nếu chưa tối ưu (F5-03).
- Chỉ ra trường hợp biên bộ test chưa phủ, giả định ngầm trong mã, nguy cơ tràn số, rủi ro khi dữ liệu lớn
  hơn ràng buộc (F5-04); nhận xét chất lượng mã — đặt tên, phân rã, trùng lặp, độ dễ đọc (F5-05); đưa ra
  câu hỏi mở rộng (F5-06).
- Trả kết quả dưới dạng **JSON có lược đồ** để giao diện render báo cáo tĩnh (F5-07); lưu kèm bài nộp, tra
  cứu lại được từ trang tiến độ (F5-08).

**F5.2 — Phỏng vấn giả lập 1:1 (nhiều lượt, hội thoại):**

- Mở phiên phỏng vấn cho một bài nộp đã `Accepted` (F5-09).
- Ba giai đoạn: **Giải trình thuật toán** — người học trình bày ý tưởng và lý do chọn cấu trúc dữ liệu
  (F5-10); **Phản biện** — AI chất vấn điểm chưa tối ưu và trường hợp biên (F5-11); **Mở rộng quy mô** —
  tình huống dữ liệu tăng đột biến hoặc ràng buộc hệ thống đổi (F5-12).
- Duy trì ngữ cảnh phiên qua `ChatMemory` trên Redis xuyên ba giai đoạn (F5-13); stream phản hồi qua SSE
  (F5-14).
- Kết phiên xuất bảng đánh giá rubric bốn tiêu chí (độ rõ ràng, độ chính xác kỹ thuật, khả năng phản biện,
  nhận thức về độ phức tạp) kèm nhận xét từng tiêu chí (F5-15); lưu phiên và rubric, mở lại được từ trang
  tiến độ (F5-16).

**Ràng buộc chung của phân hệ AI (áp cho cả F5 và F6):**

- **Chống prompt injection** (F5-17): mã nguồn và câu trả lời người dùng là **tham số dữ liệu**, tách hoàn
  toàn khỏi chỉ thị hệ thống — OWASP LLM01 (`overview.md` mục 1.J, `CLAUDE.md` mục Rules).
- **Định hướng giáo dục** (F5-18): rubric và báo cáo là phản hồi hỗ trợ học tập, **không phải điểm số chính
  thức** — phải hiện rõ trên giao diện.
- **Kiểm soát chi phí:** giới hạn tần suất gọi theo người dùng (F5-19); cache kết quả F5.1 theo hash mã
  nguồn — nộp lại đúng mã đó thì không gọi lại API (F5-20); theo dõi lượng token tiêu thụ theo từng phiên
  (F5-21).
- **Suy giảm có kiểm soát** (F5-22): AI hỏng hoặc hết quota thì F1-F4 vẫn hoạt động bình thường — ràng buộc
  kiến trúc, không phải lời hứa. Không được gọi AI đồng bộ trong luồng ghi nhận kết quả chấm.
- Cấu hình prompt và rubric AI (F5-23, actor A3).

### F6 — Ngân hàng câu hỏi phỏng vấn (`interview-bank`)

Màn hình độc lập, không gắn với bài nộp code (`README.md` mục 4-F6).

- **Danh sách và khám phá:** câu hỏi phân loại theo chủ đề (Cấu trúc dữ liệu, Thuật toán, Thiết kế hệ
  thống, Câu hỏi hành vi) và mức độ khó (F6-01); tìm kiếm và lọc (F6-02); đánh dấu để xem lại (F6-03).
- **Chế độ học (`STUDY`):** xem gợi ý hướng tiếp cận (F6-04); xem khung trả lời chuẩn, áp dụng mô hình STAR
  cho câu hỏi hành vi (F6-05); xem danh sách từ khoá kỹ thuật cốt lõi cần nêu (F6-06).
- **Chế độ luyện (`PRACTICE`):** người dùng tự soạn câu trả lời (F6-07); AI đối chiếu với tiêu chí chuẩn,
  trả phản hồi ngắn — điểm đã đạt, điểm còn thiếu, hướng bổ sung (F6-08) — **đi qua phân hệ AI**, không tự
  gọi LLM, nên chịu chung ràng buộc F5-17 tới F5-22.
- **Theo dõi tiến độ:** lịch sử luyện tập và danh sách câu hỏi cần ôn lại (F6-09); tỉ lệ hoàn thành theo
  từng chủ đề (F6-10).
- **Quản lý theo lớp:** giảng viên tạo bộ câu hỏi riêng và gán cho lớp phụ trách (F6-11, actor A2).

---

## 2. Yêu cầu Phi chức năng (Non-Functional Requirements - NFR)

> Bản trước ghi số liệu SLA cụ thể của NestGame v2 (P95 ≤ 200ms, 500 CCU...) — số liệu đó **không** có
> nguồn ở AlgoPrep nên không mang sang. Mục này chỉ ghi số liệu có nguồn thật (`environment.md`,
> `README.md`) và đánh dấu `[SoT: Suy luận]` cho ngưỡng đề xuất chưa được chủ nhiệm đề tài chốt.

### A. Hiệu năng (Performance)

- **Chờ I/O là điểm nghẽn, không phải tính toán:** gọi judge engine (go-judge mặc định, đồng bộ một
  testcase một lần), giữ WebSocket cho từng bài nộp đang chấm, giữ SSE suốt một phiên phỏng vấn AI, truy
  vấn PostgreSQL — xử lý bằng Virtual Threads (`overview.md` mục 1.D). Việc tính toán nặng của chính bài
  giải người dùng chạy trong judge engine, ngoài JVM.
- **Kiểm chứng bằng tải, không bằng suy diễn:** luồng nộp bài đồng thời phải đo bằng **k6**
  (`environment.md` mục 5) — đây là chỗ Virtual Threads phải chứng minh được điều `overview.md` mục 1.D
  khẳng định, không phải một lời hứa kiến trúc.
- **Ngưỡng cụ thể (P95 độ trễ API, số phiên phỏng vấn đồng thời, số bài nộp đồng thời) chưa được chốt** —
  `[SoT: Suy luận]`: cần chủ nhiệm đề tài xác nhận trước khi viết vào `02-bd/architecture/`.

### B. Khả năng mở rộng (Scalability)

- **Modular Monolith, không phải microservices** (`overview.md` mục 1.A, `DEC-2026-0820-architecture-baseline`):
  một tiến trình, một PostgreSQL instance, sáu schema. Ranh giới module bảo vệ bằng kiểm tra tự động
  (test kiểm ranh giới module, `environment.md` mục 3.A), không phải ranh giới mạng.
- Judge engine (go-judge mặc định) và các dịch vụ hạ tầng (RabbitMQ, Redis, MinIO) chạy tách biệt qua
  Docker (`environment.md` mục 2) — mở rộng theo chiều ngang của tầng thực thi độc lập với việc mở rộng
  backend.

### C. Độ tin cậy (Reliability)

- **Không có bài nộp treo vĩnh viễn:** timeout sweep định kỳ (F4-07) là lưới an toàn bắt buộc, không phải
  tính năng tuỳ chọn (`overview.md` mục 1.E).
- **RabbitMQ là _ít nhất một lần_:** consumer phải bất biến theo số lần gọi — không bao giờ ghi đè trạng
  thái cuối. Nếu một adapter bất đồng bộ (Judge0) được cắm lại, khoá idempotency theo token của engine đó
  (F4-06) là chi tiết nội bộ adapter, không phải yêu cầu ở port.
- **AI không kéo sập đường chấm bài:** F5/F6 chết hoặc hết quota thì F1-F4 vẫn chạy đủ (F5-22,
  `README.md` mục 4).
- **Sao lưu và khôi phục dữ liệu** (bộ testcase, bài nộp, bài giải, phiên phỏng vấn) chưa được chốt tần suất
  và chính sách — `[SoT: Suy luận]`, chốt ở `02-bd/database/`.

### D. Bảo mật (Security)

Nguồn: `overview.md` mục 1.I (OWASP Top 10) và mục 1.J (LLM). Không lặp lại nội dung, chỉ liệt kê thành yêu
cầu kiểm chứng được:

- SQL Injection: truy vấn tham số hoá, không nối chuỗi SQL.
- XSS: làm sạch HTML sau khi render Markdown (đề bài do A2 soạn); LaTeX render bằng thư viện toán học,
  không nhúng HTML thô; cấu hình `Content-Security-Policy`.
- CSRF: Refresh Token trong cookie HTTP-Only kèm CSRF token và `SameSite`; Access Token đi trong header
  `Authorization` nên không mang rủi ro CSRF.
- Rate limiting theo IP và theo người dùng ở ba nhóm endpoint: đăng nhập/đăng ký, nộp bài, gọi AI.
- Phân quyền theo vai trò kiểm ở tầng ứng dụng, kèm kiểm quyền sở hữu dữ liệu.
- Chống rò rỉ testcase ẩn (F2-08) và chống prompt injection (F5-17) là hai ràng buộc bảo mật nghiệp vụ đặc
  thù của đề tài — không phải OWASP chung, phải kiểm riêng bằng test có chủ đích (`system_survey.md` mục
  10, tiêu chí S6, S8).
- Judge engine (go-judge mặc định) chạy cách ly bằng `go-sandbox` — namespaces, cgroups (`overview.md` mục
  1.F). Rủi ro cgroup v1/v2 (R1) đã đóng cho luồng mặc định bằng `DEC-2026-0823-go-judge-default-engine`
  (`README.md` mục 7).

### E. Khả năng bảo trì (Maintainability)

Nguồn: `environment.md` mục 3-4 — đây là các cổng chất lượng **thật**, phải chạy được, không phải mục tiêu
định tính:

- Backend: Spotless, Checkstyle, SpotBugs + FindSecBugs, JaCoCo **≥ 80%** cho tầng `application` và
  `domain`, test kiểm ranh giới module (fail build khi import chéo Bounded Context).
- Frontend: TypeScript `strict`, ESLint + `eslint-plugin-boundaries` (chặn import sai chiều FSD),
  Prettier, Husky + lint-staged.
- Riêng `algoprep-harness` (F3): bắt buộc có test so khớp mã sinh ra (golden file) cho cả ba ngôn ngữ —
  đây là bộ test rẻ nhất và bắt lỗi tốt nhất của đề tài (`environment.md` mục 3.A).
- Cổng chất lượng bắt buộc trước khi báo xong việc (G-CHECK, `environment.md` mục 4): `pnpm check` cho
  frontend, `./mvnw verify` cho backend, thêm `pnpm e2e` khi có thay đổi luồng người dùng.

### F. Tiêu chí thành công và kiểm chứng

Không phải một mục UI/UX (đề tài chưa chọn design system — `01-rd/overview/overview.md` mục 3). Đây là tám
tiêu chí đo được, lấy nguyên từ `system_survey.md` mục 10 vì chúng đã là yêu cầu phi chức năng dưới dạng đo
lường được, không phải nhắc lại chức năng:

| #   | Tiêu chí                                                                  | Cách đo                                                                |
| :-- | :------------------------------------------------------------------------ | :--------------------------------------------------------------------- |
| S1  | Một bài toán bọc hàm chạy đúng trên cả ba ngôn ngữ từ một đặc tả duy nhất | So khớp mã sinh ra, cộng một lần chạy thật qua judge engine (go-judge) |
| S2  | Lỗi biên dịch ánh xạ đúng dòng, không lộ mã harness                       | Bộ test có mã sai cố ý, so số dòng báo lỗi                             |
| S3  | Trạng thái từng testcase hiện realtime                                    | Đo độ trễ từ khi `JudgeExecutionPort` trả kết quả tới lúc giao diện đổi |
| S4  | Bài nộp không treo vĩnh viễn                                              | Cố ý cho worker crash giữa lúc gọi engine, xác nhận RabbitMQ redeliver + timeout sweep dọn được |
| S5  | Message xử lý lại (redelivery) không làm sai kết quả                      | Gửi lại cùng message đã ack một phần, xác nhận không ghi đè trạng thái cuối |
| S6  | Testcase ẩn không rò rỉ qua bất kỳ bề mặt nào                             | Rà toàn bộ phản hồi API và log                                         |
| S7  | Tắt hoàn toàn AI thì F1-F4 vẫn chạy đủ                                    | Chạy lại luồng E2E chính với AI vô hiệu hoá                            |
| S8  | Mã nguồn người dùng không thể thành chỉ thị cho AI                        | Bộ test có mã chứa câu lệnh tiêm prompt                                |

Chi tiết đầy đủ (giới hạn phạm vi, rủi ro, danh sách màn) ở `01-rd/overview/system_survey.md` mục 8-10 —
không lặp lại ở đây để tránh hai bản trôi nhau.
