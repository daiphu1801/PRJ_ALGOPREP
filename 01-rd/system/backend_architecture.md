# Kiến Trúc Backend (Backend Architecture)

Tài liệu này đặc tả kiến trúc backend của AlgoPrep: kiểu kiến trúc, cách chia module theo Bounded Context,
bốn tầng của Clean Architecture, quy ước cổng vào và cổng ra, tách đường ghi và đường đọc, cách các module
nói chuyện với nhau, và cách hai hệ ngoài (judge engine — go-judge mặc định, LLM) được cách ly.

Nguyên lý và lý do chọn nằm ở `01-rd/overview/overview.md` mục 1.A đến 1.E. Tài liệu này là **bố cục cụ thể**.

| Nguồn | Vai trò |
| :--- | :--- |
| `README.md` mục 4, mục 5 | Phân hệ F1-F6 và công nghệ — nguồn của phạm vi |
| `.nexa/domain-registry.json` | Sáu Bounded Context, tên module, tên schema |
| `.nexa/control/decision-registry.md` | `DEC-2026-0820-stack-versions`, `DEC-2026-0820-architecture-baseline` |
| `01-rd/system/codebase_structure.md` | Cây thư mục thật của monorepo |

> **Trạng thái 2026-08-20:** `05-coding/` còn rỗng. Tài liệu này là **thiết kế phải theo**, chưa phải mô tả
> mã đang có. Khi mã tồn tại và lệch, ghi rõ lệch ở đâu rồi sửa một trong hai bên — đừng để trôi.

---

## 1. Kiểu kiến trúc: Modular Monolith

Một tiến trình Spring Boot, một database PostgreSQL, sáu module Maven tương ứng sáu Bounded Context.

**Vì sao không microservices.** Đây là đồ án tốt nghiệp một người làm. Microservices đổi độ phức tạp nghiệp
vụ thành độ phức tạp vận hành: sáu tiến trình, sáu pipeline, giao dịch phân tán, tracing xuyên dịch vụ. Chi
phí đó không mua lại được gì cho một hệ thống chưa có tải thật và không có đội vận hành. Modular Monolith
giữ **ranh giới thiết kế** của DDD mà không trả **giá vận hành** của microservices.

**Vì sao không monolith phẳng.** Vì trọng tâm kỹ thuật của đề tài (`harness` — F3) phải độc lập để test
được, và vì `ai-review` (F5) phải hỏng được mà không kéo theo phần chấm bài. Cả hai yêu cầu đó đều là yêu
cầu về ranh giới. Ranh giới không được kiểm tra tự động thì sau vài sprint sẽ biến mất.

**Ranh giới được thi hành bằng gì.** Bằng `mvn verify`: phụ thuộc khai trong `pom.xml` của từng module, cộng
với một test kiểm ranh giới trong module bootstrap. Import chéo trái phép (ví dụ `problem-bank` gọi thẳng
vào `infrastructure` của `identity`) làm **fail build**. Chi tiết công cụ ở `environment.md` mục 2.A.

---

## 2. Bảy module Maven

Sáu module nghiệp vụ theo `.nexa/domain-registry.json`, cộng một module dùng chung và một module khởi chạy.

| Module Maven | Bounded Context | Schema PostgreSQL | Phân hệ | Vai trò |
| :--- | :--- | :--- | :--- | :--- |
| `algoprep-common` | (không) | (không) | — | Kiểu dùng chung: kiểu miền cơ sở, ngoại lệ nền, cổng ra dùng chung (đồng hồ, sinh id), tiện ích bảo mật. **Không chứa nghiệp vụ của context nào** |
| `algoprep-identity` | `identity` | `identity` | F1 | Xác thực JWT, phân quyền `STUDENT`/`INSTRUCTOR`/`ADMIN`, tiến độ cá nhân |
| `algoprep-problem-bank` | `problem-bank` | `problem` | F2 | Đề bài Markdown và LaTeX, đặc tả hàm theo ngôn ngữ, testcase Sample và Hidden, phiên bản bộ testcase, giới hạn tài nguyên |
| `algoprep-harness` | `harness` | `harness` | F3 | Lược đồ kiểu độc lập ngôn ngữ, sinh mã Java/C++/Python, tiêm mã người dùng, chiến lược so khớp, ánh xạ lỗi biên dịch |
| `algoprep-judge` | `judge-orchestration` | `judge` | F4 | Hàng đợi RabbitMQ, gọi `JudgeExecutionPort` từng testcase kèm fail-fast (adapter mặc định go-judge, đồng bộ), timeout sweep, đẩy trạng thái qua WebSocket. Xác thực webhook và idempotency theo token chỉ cần nếu một adapter bất đồng bộ được bật |
| `algoprep-ai-review` | `ai-review` | `ai` | F5 | Solution Review một lượt (JSON có cấu trúc) và Mock Interview nhiều lượt (SSE, `ChatMemory` trên Redis, rubric) |
| `algoprep-interview-bank` | `interview-bank` | `interview_bank` | F6 | Ngân hàng câu hỏi lý thuyết, chế độ học, chế độ luyện có AI đối chiếu, theo dõi tiến độ |
| `algoprep-bootstrap` | (không) | (không) | — | `@SpringBootApplication`, gom cấu hình toàn cục, chạy test kiểm ranh giới module |

**Thứ tự dựng, không dựng hết một lượt.** `CLAUDE.md` mục Process: chốt trước cái đắt tiền khi sai (lược đồ
DB, hợp đồng API, lược đồ kiểu của harness), phần còn lại viết vừa đúng lúc. Thứ tự đề xuất
[SoT: Suy luận — suy ra từ chuỗi phụ thuộc và từ thứ tự ưu tiên cắt giảm ở `README.md` mục 7]:

`common` → `identity` → `problem-bank` → `harness` → `judge` → `ai-review` → `interview-bank`

Lý do thứ tự này: `harness` phải có trước `judge` (không có mã sinh ra thì không có gì để gửi đi chấm);
`judge` phải chạy được trước `ai-review` (AI chỉ mở sau khi có bài nộp `Accepted`); `interview-bank` độc lập
hoàn toàn với đường chấm bài nên để cuối — cũng đúng với thứ tự cắt giảm nếu thiếu thời gian.

### 2.A. Ai được phụ thuộc vào ai

```text
                    algoprep-bootstrap
                            │  (phụ thuộc tất cả, chỉ để khởi chạy)
   ┌──────────┬─────────────┼─────────────┬──────────────┐
   ↓          ↓             ↓             ↓              ↓
identity  problem-bank  harness       judge        ai-review   interview-bank
   │          │             │             │              │            │
   └──────────┴─────────────┴─────────────┴──────────────┴────────────┘
                            ↓
                     algoprep-common
```

Ba luật, và cả ba đều phải kiểm được bằng máy:

1. **Mọi module nghiệp vụ chỉ được phụ thuộc `algoprep-common`.** Không module nghiệp vụ nào phụ thuộc module
   nghiệp vụ khác.
2. **`algoprep-bootstrap` phụ thuộc tất cả**, và không ai phụ thuộc nó.
3. **`algoprep-common` không phụ thuộc ai.**

Hệ quả: hai module cần nói chuyện thì đi bằng **sự kiện miền** hoặc bằng **cổng ra do module gọi tự khai**
(mục 5), không bằng cách import thẳng.

Ràng buộc chặt hơn ở tầng gói (package), không diễn đạt được bằng `pom.xml` nên phải để test kiểm ranh giới
bắt: **`presentation` và `infrastructure` của module A không bao giờ được xuất hiện trong import của module B.**

---

## 3. Bốn tầng trong một module

Bố cục dưới đây áp cho **mọi** module nghiệp vụ. Ví dụ dùng `algoprep-judge` vì nó có đủ cả bốn tầng và cả
hai đường ghi/đọc.

```text
algoprep-judge/src/main/java/com/algoprep/judge/
├── domain/                     # --- LÕI NGHIỆP VỤ: Java thuần, không Spring, không JPA ---
│   ├── model/                  # Aggregate, entity, value object
│   │                           #   Submission (gốc), SubmissionTestcaseResult, SubmissionStatus
│   │                           #   EngineRunToken/CallbackSecret chỉ tồn tại nếu Judge0Adapter được bật —
│   │                           #   không thuộc mô hình mặc định (go-judge, gọi đồng bộ)
│   ├── ports/out/              # Cổng RA của đường ghi
│   │                           #   SubmissionRepository, JudgeExecutionPort (trung lập theo engine —
│   │                           #   "chạy một testcase, trả một kết quả", DEC-2026-0823-go-judge-default-engine),
│   │                           #   SubmissionEventPublisher, ClockPort
│   └── exception/              # Ngoại lệ nghiệp vụ
│                               #   FinalStateOverwriteException; UnknownEngineRunTokenException chỉ cần
│                               #   nếu một adapter bất đồng bộ (Judge0Adapter) được bật
│
├── application/                # --- CA SỬ DỤNG ---
│   ├── ports/in/               # Cổng VÀO: mỗi ca sử dụng một interface, một phương thức
│   │                           #   SubmitSolutionUseCase, RecordJudgeCallbackUseCase,
│   │                           #   RequestRejudgeUseCase, GetSubmissionDetailQuery
│   ├── ports/out/              # Cổng RA của đường ĐỌC — trả read model, không trả Aggregate
│   │                           #   SubmissionReadPort, QueueMetricsReadPort
│   ├── command/                # Handler làm ĐỔI trạng thái, mở transaction
│   │                           #   SubmitSolutionCommandHandler, RecordJudgeCallbackCommandHandler
│   ├── query/                  # Handler CHỈ đọc, không transaction ghi
│   │                           #   GetSubmissionDetailQueryHandler, ListMySubmissionsQueryHandler
│   └── dto/                    # Command · Query · Response (record)
│
├── infrastructure/             # --- ADAPTER VÀ CẤU HÌNH ---
│   ├── persistence/            # JPA @Entity + adapter hiện thực cổng ra + truy vấn đường đọc
│   ├── judgeengine/             # GoJudgeAdapter (mặc định) — ACL sang go-judge, hiện thực JudgeExecutionPort
│   │                            #   Judge0Adapter (nếu cắm lại) sống cùng thư mục, hiện thực cùng port
│   ├── messaging/               # Publisher và consumer RabbitMQ
│   ├── realtime/                # Đẩy trạng thái qua WebSocket/STOMP
│   ├── job/                     # Timeout sweep định kỳ (@Scheduled) — bản nhẹ, không đối soát hệ ngoài
│   └── config/                  # Spring Bean, cấu hình hàng đợi, cấu hình HTTP client
│
└── presentation/               # --- BỀ MẶT VÀO ---
    ├── controller/             # REST controller
    ├── webhook/                # Endpoint nhận callback — chỉ cần tồn tại nếu một adapter bất đồng bộ
    │                           #   (Judge0Adapter) được bật; không cần với adapter mặc định (go-judge)
    ├── ws/                     # Khai báo STOMP destination
    └── mapper/                 # DTO tầng ngoài ↔ Command/Query
```

### 3.A. Bốn luật của tầng, và cách phát hiện vi phạm

| Luật | Vi phạm trông như thế nào | Bắt bằng |
| :--- | :--- | :--- |
| `domain/` không import Spring, JPA, Jackson | `@Entity`, `@Component`, `@JsonProperty` trong `domain/` | Test kiểm ranh giới + review |
| Cổng RA ở `domain/ports/out`, cổng VÀO ở `application/ports/in` | Interface nhận `Command` nằm trong `domain/` | Review; xem `overview.md` mục 1.B |
| `presentation/` không gọi `infrastructure/` trực tiếp | Controller `@Autowired` một repository JPA | Test kiểm ranh giới |
| `application/` không biết HTTP | `HttpServletRequest`, `ResponseEntity` trong handler | Review |

**Không có `domain/repository/`.** Repository là một cổng ra, sống trong `domain/ports/out`.
**Không có `application/usecase/`.** Thay bằng cặp `command/` và `query/` — lý do ở `overview.md` mục 1.C.

### 3.B. Vì sao mỗi cổng vào chỉ một phương thức

Một interface một phương thức thì bề mặt của module đọc được bằng cách liệt kê `application/ports/in`: có bao
nhiêu file là có bấy nhiêu ca sử dụng. Nhồi mười phương thức vào một `SubmissionService` thì bề mặt biến
thành một cục, và mọi nơi gọi đều phụ thuộc vào cả mười thứ dù chỉ dùng một.

---

## 4. Hai luồng dữ liệu điển hình

### 4.A. Đường ghi — nộp bài (luồng cốt lõi của hệ thống)

```text
[HTTP POST /submissions]
      ↓
presentation/controller          SubmissionController — kiểm DTO đầu vào, map sang Command
      ↓
application/ports/in             SubmitSolutionUseCase — hợp đồng cổng vào
      ↓
application/command              SubmitSolutionCommandHandler — mở transaction, điều phối
      ├─→ domain/model           Submission — bất biến: quyền sở hữu, ngôn ngữ trong ba ngôn ngữ
      │                          hỗ trợ. CallbackSecret chỉ sinh nếu adapter đang dùng là bất đồng bộ
      │                          (Judge0Adapter) — với adapter mặc định (go-judge) thì bỏ qua bước này
      ├─→ domain/ports/out       SubmissionRepository → infrastructure/persistence (ghi PENDING)
      └─→ domain/ports/out       JudgeDispatchPort → infrastructure/messaging (đẩy vào RabbitMQ)
      ↓
[HTTP 202 + submissionId]        Trả ngay, KHÔNG chờ kết quả chấm
```

Tiếp theo, worker lấy việc khỏi hàng đợi rồi gọi `JudgeExecutionPort` **đồng bộ, từng testcase một** (đây là
phần "bất đồng bộ" thật của hệ thống — nằm ở hàng đợi giữa client và worker, không còn nằm ở giao tiếp với
engine chấm như khi còn giả định Judge0):

```text
[RabbitMQ consumer]              infrastructure/messaging — nhận việc chấm
      ↓                          (consumer phải bất biến theo số lần gọi — RabbitMQ là at-least-once)
application/ports/in             DispatchSubmissionUseCase
      ↓
application/command              DispatchSubmissionCommandHandler
      ├─→ cổng ra sang harness   Lấy mã đã sinh cho bài nộp này (mục 5)
      ├─→ cổng ra sang problem   Lấy testcase theo đúng phiên bản bộ testcase
      └─→ lặp qua từng testcase:
            domain/ports/out       JudgeExecutionPort → infrastructure/judgeengine/GoJudgeAdapter
                                    Gọi ĐỒNG BỘ, một request một testcase — trả kết quả ngay trong response
            ↓
            application/command    RecordTestcaseResultCommandHandler
                  ├─→ domain/model  Submission cập nhật kết quả testcase này, tính lại trạng thái tổng,
                  │                 KHÔNG bao giờ ghi đè một trạng thái đã là trạng thái cuối
                  ├─→ infrastructure/realtime → đẩy trạng thái testcase qua WebSocket topic của bài nộp
                  └─→ nếu điều kiện dừng đạt (testcase sai/lỗi) → dừng lặp, không gọi testcase còn lại
      ↓ (khi hết testcase hoặc đã dừng sớm)
      SubmissionEventPublisher → phát SubmissionAcceptedEvent khi đạt Accepted
```

Nếu một adapter bất đồng bộ (`Judge0Adapter`) được cắm lại sau này, `GoJudgeAdapter` trong sơ đồ trên đổi
thành `Judge0Adapter`, và bước "trả kết quả ngay trong response" đổi thành: gửi request, nhận token, chờ
webhook callback riêng (`[POST /webhooks/judge/{token}]` — xác thực `CallbackSecret`, khoá idempotency theo
token) rồi mới gọi `RecordTestcaseResultCommandHandler`. Đây là khác biệt duy nhất giữa hai adapter —
domain/application phía trên không đổi.

Ba chỗ dễ sai nhất trong luồng này, ghi lại để đừng phải học bằng bug:

1. **Ghi đè trạng thái cuối.** Một kết quả testcase đến muộn hoặc xử lý lại (message redeliver) không được
   phép đưa một bài nộp đã `Accepted` (hoặc đã `Wrong Answer`) về lại trạng thái trung gian.
2. **Xác thực webhook — chỉ khi có adapter bất đồng bộ.** Với `GoJudgeAdapter` không có bề mặt này. Nếu
   `Judge0Adapter` được bật, endpoint webhook nằm ngoài luồng JWT, nên nếu không kiểm token bí mật theo bài
   nộp thì ai cũng tự gửi `Accepted` được.
3. **Bài nộp treo.** Với adapter đồng bộ, nguyên nhân chủ yếu là worker crash giữa lúc đang gọi engine —
   RabbitMQ tự redeliver message chưa ack. Timeout sweep (bản nhẹ, không đối soát hệ ngoài) là lưới an toàn
   còn lại cho trường hợp hiếm hơn.

### 4.B. Đường đọc — danh sách bài toán

```text
[HTTP GET /problems?topic=&difficulty=&page=]
      ↓
presentation/controller
      ↓
application/ports/in (query)     ListProblemsQuery
      ↓
application/query                ListProblemsQueryHandler — chỉ đọc, không transaction ghi
      ↓
application/ports/out            ProblemSummaryReadPort — trả read model phẳng
      ↓
infrastructure/persistence       Truy vấn có phân trang, chỉ chọn cột cần cho danh sách
```

Đường đọc **không** đi qua Aggregate `Problem` — nếu đi qua, một dòng danh sách sẽ nạp kèm toàn bộ đặc tả
hàm và testcase. Nhưng nó vẫn đi qua **một cổng có tên** trong `application/ports/out`; controller không gọi
thẳng Spring Data.

---

## 5. Hai module nói chuyện với nhau bằng gì

Luật ở mục 2.A cấm import chéo, nên phải có đúng ba cách hợp lệ. Không có cách thứ tư.

| Cách | Dùng khi | Ví dụ trong AlgoPrep |
| :--- | :--- | :--- |
| **Cổng ra do module gọi tự khai** | Cần dữ liệu **ngay**, đồng bộ, trong cùng transaction | `judge` khai `HarnessCodePort` và `TestcaseProviderPort` trong `domain/ports/out` của chính nó; adapter hiện thực nằm ở `algoprep-bootstrap` hoặc ở `infrastructure` của module gọi, gọi sang **cổng vào** của module kia |
| **Sự kiện miền** | Việc đã xảy ra, module khác phản ứng, **không cần trong cùng transaction** | `judge` phát `SubmissionAcceptedEvent`; `identity` nghe để cập nhật tiến độ, `ai-review` nghe để mở khoá F5 |
| **Đường đọc riêng** | Chỉ cần đọc, và dữ liệu thuộc context khác | Trang tiến độ cá nhân cần số bài đã giải theo chủ đề: `identity` khai một read port, không gọi Aggregate của `problem-bank` |

**Điểm mấu chốt:** module gọi **tự đặt tên cổng theo nhu cầu của mình**, không nhận nguyên interface của
module kia. `judge` cần "mã đã sinh cho bài nộp này" — nó khai `HarnessCodePort`, không import
`HarnessGeneratorUseCase` của `harness`. Nhờ vậy `harness` đổi bề mặt thì chỉ một adapter phải sửa.

**Sự kiện miền là bất đồng bộ có chủ ý.** Cập nhật tiến độ cá nhân chậm nửa giây thì không ai chết; nhưng
nếu ghép nó vào transaction của việc ghi nhận callback thì một lỗi ở `identity` sẽ làm rollback kết quả
chấm bài. Đây chính là suy giảm có kiểm soát ở mức mã nguồn.

---

## 6. Hai hệ ngoài và cách cách ly

### 6.A. Judge engine (go-judge mặc định)

* **Định vị:** dịch vụ ngoài, chạy bằng Docker, **không phải module trong monolith**
  (`.nexa/domain-registry.json`, phần `crosscutting`).
* **Cửa duy nhất:** `algoprep-judge/infrastructure/judgeengine/GoJudgeAdapter`, hiện thực cổng ra
  `JudgeExecutionPort` (`domain/ports/out`). Không module nào khác được gọi judge engine trực tiếp.
* **Cổng phải trung lập theo engine** (`DEC-2026-0823-go-judge-default-engine`) — chữ ký cổng chỉ có dạng
  "chạy một testcase, trả một kết quả", không có field kiểu `status.id`/token/webhook (hình dạng riêng của
  Judge0), không có field kiểu `cmd[]`/`copyIn` (hình dạng riêng của go-judge).
* **ACL dịch những gì (với `GoJudgeAdapter`):** kết quả trả về của go-judge (`status`, `exitStatus`,
  `stdout`/`stderr`, thời gian, bộ nhớ) sang value object kết quả của AlgoPrep. Miền của AlgoPrep không bao
  giờ thấy trực tiếp hình dạng response của go-judge.
* **Adapter thay thế:** `Judge0Adapter` hiện thực cùng `JudgeExecutionPort` — nếu cắm lại, nó tự quản token/
  webhook/idempotency bên trong, domain/application không đổi. Điều kiện: cổng phải được giữ trung lập như
  trên, đây không phải tính chất tự động có sẵn.
* **Ràng buộc kỹ thuật đã đóng:** go-judge hỗ trợ cả cgroup v1 và v2, không cần kiểm chứng/ép version như
  Judge0 từng cần (`README.md` mục 7; `01-rd/system/judge_engine.md` mục 3).

### 6.B. Nhà cung cấp LLM (qua Spring AI)

* **Cửa duy nhất:** `algoprep-ai-review/infrastructure/ai/`. `interview-bank` (F6) cũng cần AI để đối chiếu
  câu trả lời — nó **không** tự gọi LLM mà đi qua cổng vào của `ai-review`. Một cửa thì rate limit, cache,
  đếm token và phòng thủ prompt injection chỉ phải làm một lần.
* **Bài nộp là dữ liệu, không phải chỉ thị.** Mã nguồn và câu trả lời người dùng luôn là **tham số dữ liệu**
  của prompt template, tách khỏi chỉ thị hệ thống (`CLAUDE.md` mục Rules).
* **Suy giảm có kiểm soát.** LLM lỗi hoặc hết quota thì `ai-review` trả trạng thái không khả dụng; F1-F4
  không bị ảnh hưởng. Ràng buộc mã nguồn: **không lời gọi AI nào nằm trong luồng ghi nhận kết quả chấm.**

---

## 7. Các mối quan tâm xuyên suốt

| Mối quan tâm | Đặt ở đâu | Ghi chú |
| :--- | :--- | :--- |
| Xác thực và phân quyền | Cấu hình Spring Security ở `algoprep-bootstrap`; kiểm quyền sở hữu dữ liệu ở `application/` của từng module | Kiểm quyền chỉ ở giao diện là không kiểm |
| Xử lý lỗi và mã lỗi | `@RestControllerAdvice` toàn cục ở `bootstrap`, ánh xạ ngoại lệ nghiệp vụ sang mã lỗi ổn định | Frontend dịch theo mã lỗi, không hiển thị message thô |
| Giao dịch (transaction) | Mở ở `application/command`, không ở controller, không ở repository | Một ca sử dụng một transaction |
| Rate limiting | Filter ở tầng presentation, Token Bucket trên Redis | Ba nhóm endpoint: đăng nhập, nộp bài, gọi AI |
| Cache | Redis; đáng chú ý nhất là cache F5.1 theo hash mã nguồn | `README.md` mục 4 |
| Ghi log và giám sát | Prometheus và Grafana; số liệu tối thiểu: độ dài hàng đợi, độ trễ chấm bài, tỉ lệ callback lỗi, token AI đã dùng | `README.md` mục 5 |
| Lưu tệp lớn | MinIO cho bộ testcase lớn; metadata vẫn ở PostgreSQL | `README.md` mục 5 |

---

## 8. Điều tài liệu này KHÔNG chốt

| Chưa chốt | Sẽ chốt ở |
| :--- | :--- |
| Bảng, cột, khoá, index của sáu schema | `02-bd/database/<module>.md` |
| Đường dẫn endpoint, payload, mã lỗi | `03-dd/api/<module>.md` và `03-dd/api/api.md` |
| Tên chính thức của các sự kiện miền và payload của chúng | `02-bd/architecture/<module>.md` |
| Cấu trúc lược đồ kiểu của F3 | `02-bd/architecture/harness.md` |
| Kích thước đợt testcase, ngưỡng treo của job đối soát | `03-dd/jobs/judge-orchestration.md` |
| Tên STOMP destination và định dạng message realtime | `03-dd/api/judge-orchestration.md` |
