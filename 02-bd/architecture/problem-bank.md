# BD — Kiến trúc module `problem-bank` (F2)

> Trạng thái: **BD lần đầu**, 2026-09-12. Module thứ hai theo thứ tự
> `06-plan/260912-1055-bd-rollout-order.md`. Đọc cùng `02-bd/architecture/identity.md` để bám văn phong;
> module này khác `identity` ở một điểm quan trọng: **có** hạ tầng MinIO, nên có thêm
> `02-bd/storage/problem-bank.md`.

## 1. Vị trí trong kiến trúc

Module Maven `algoprep-problem-bank`, schema PostgreSQL `problem`
[SoT: `.nexa/domain-registry.json` — domains[1]]. Bốn tầng theo baseline chung
(`DEC-2026-0820-architecture-baseline`, giống `identity`): `domain` (aggregate, value object, outbound
port tại `domain/ports/out`) / `application` (inbound port tại `application/ports/in`, một interface một
phương thức, cộng `command/` `query/` `dto/`) / `infrastructure` / `presentation`. Không có
`domain/repository/`, không có `application/usecase/`.

Hạ tầng phụ thuộc: PostgreSQL + **MinIO/S3** (bộ testcase lớn) + Redis (cache danh sách bài toán)
[SoT: `.nexa/control/dependency-map.md` mục 2]. Vì có MinIO, `02-bd/storage/problem-bank.md` **bắt buộc
phải tồn tại** — khác `identity`.

## 2. Phạm vi nghiệp vụ module này bao trùm

Đọc từ `01-rd/req/problem-bank.md` (F2-01 → F2-17):

| Nhóm | Mã | Ghi chú |
| :--- | :--- | :--- |
| Soạn đề bài | F2-01, F2-02 | Markdown + LaTeX; độ khó, chủ đề, thẻ (tag) tự do nhiều-nhiều |
| Đặc tả song song hai mô hình nộp bài | F2-03 | Chữ ký hàm theo Java/C++/Python (đầu vào cho F3 — bọc hàm) **và** định dạng stdin/stdout theo dòng chuẩn (đầu vào cho mô hình Standard I/O). Cả hai luôn cùng tồn tại cho một bài, trừ ngoại lệ F3-13 (kiểu dữ liệu vượt lược đồ, ẩn tuỳ chọn Bọc hàm — cờ đặt ở `problem-bank`, đọc bởi `harness`) [SoT: `01-rd/req/problem-bank.md:11-21`; `02-bd/architecture/harness.md` mục 2b] |
| Chiến lược so khớp | F2-04 | `EXACT`/`TRIMMED`/`EPSILON`/`UNORDERED_SET`, khai theo bài toán, `harness` đọc để sinh mã so khớp |
| Testcase Sample/Hidden | F2-05, F2-06, F2-08 | Sample công khai (Chạy thử), Hidden ẩn (Nộp bài); chống rò rỉ — API/log không bao giờ trả input hay diff của Hidden testcase |
| Tải lên theo lô + MinIO | F2-07 | Chi tiết ở `02-bd/storage/problem-bank.md` |
| Versioning bộ testcase | F2-09 | Chỉ còn tác dụng **truy vết** (không phải input cho re-judge — đã cắt, `DEC-2026-0828-remove-rejudge-scope`) |
| Giới hạn tài nguyên | F2-10 | Thời gian/bộ nhớ theo bài × hệ số ngôn ngữ (hệ số đọc từ `judge-orchestration`/`admin_language_config`, không sở hữu ở đây), cộng kích thước đầu ra tối đa và số lần nộp tối đa/giờ theo từng bài (`DEC-2026-0831-problem-authoring-round2`) |
| Tìm kiếm/lọc | F2-11 | Theo chủ đề, độ khó, trạng thái đã giải (đọc từ `identity` qua event), thẻ |
| Giao bài theo lớp | F2-12 | Gán/gỡ bài cho lớp; gỡ là ẩn mềm, không xoá lịch sử (`DEC-2026-0831-class-assignments-round2`) |
| Bookmark riêng tư | F2-13 | Ghi chú theo (`user_id`, `problem_id`), tuyệt đối riêng tư — kể cả với `INSTRUCTOR`/`ADMIN` |
| Sinh testcase bằng AI | F2-14 | AI chỉ sinh input; output lấy từ chạy Đáp án mẫu thật qua `JudgeExecutionPort` (outbound port `problem-bank` tự khai báo, gọi sang hạ tầng judge engine dùng chung — không import module `judge-orchestration`) |
| Vòng đời bài toán | F2-15 | Hai trạng thái `Chưa xuất bản`/`Đã xuất bản` + cờ `deleted` (ẩn mềm) — `DEC-2026-0830-problem-lifecycle-two-states`, `DEC-2026-0831-problem-management-lifecycle-details` |
| Nhân bản bài toán | F2-16 | Sao chép toàn bộ nội dung, bản sao ở trạng thái `Chưa xuất bản` |
| Xuất CSV | F2-17 | Chỉ metadata bảng, không xuất đề/đặc tả/testcase |

## 3. Giao tiếp liên module

Theo `DEC-2026-0820-architecture-baseline`: không import schema khác. `problem-bank` dùng ba kênh:

### 3.1. Phát domain event (cho module khác lắng nghe)

| Sự kiện phát ra | Ai lắng nghe | Payload tối thiểu | Dùng để |
| :--- | :--- | :--- | :--- |
| `ProblemSpecChanged` | `harness` | `problem_id`, `spec_version` (không phải `testcase_set_version` — hai khái niệm khác nhau, xem mục 2 database), diff chữ ký hàm | Vô hiệu hoá/tái sinh mã bọc hàm đã cache cho bài đó nếu `harness` có cache theo `problem_id` |
| `ProblemPublished` / `ProblemUnpublished` | `identity` (read model `identity_recent_activity`), `judge-orchestration` (nếu có cache `language_config` theo bài) | `problem_id`, `actor_user_id`, `occurred_at` | F1-30, và để `judge-orchestration` biết bài nào đang nhận submission |
| `TestcaseSetVersionBumped` | `judge-orchestration` | `problem_id`, `testcase_set_version`, `bumped_at` | Ghi lại submission mới được chấm theo version nào (F2-09 mục truy vết) — `judge-orchestration` tự lưu `testcase_set_version` trên bảng `submission` của nó khi nhận sự kiện, không đọc trực tiếp bảng `testcase` của `problem-bank` |
| `ProblemAssignedToClass` / `ProblemUnassignedFromClass` | `identity` (nếu `class_management` cần hiển thị) | `problem_id`, `class_id`, `actor_user_id` | F2-12, hiển thị khối bài giao trong `problem_list` |

`[SoT: Suy luận]` — tên sự kiện, payload chi tiết, cơ chế versioning (outbox hay không) chốt ở
`03-dd/api/problem-bank.md`; BD chỉ chốt **có kênh event, không import trực tiếp**.

### 3.2. Outbound port `problem-bank` tự khai báo (gọi ra ngoài)

- **`JudgeExecutionPort`** (dùng chung tên/khái niệm với `judge-orchestration`, nhưng `problem-bank` khai
  báo instance/interface **của riêng mình** trong `domain/ports/out` theo đúng nguyên tắc "outbound port
  nằm trong ngôn ngữ của bên gọi" — `.nexa/control/dependency-map.md` mục 1): dùng cho F2-14 (chạy Đáp án
  mẫu để lấy output thật) và cho bước "Chạy với đáp án mẫu" trong `problem_authoring`. Adapter thật trỏ
  thẳng tới go-judge (hạ tầng dùng chung, không phải gọi qua module `judge-orchestration`) — vì đây là một
  lượt chạy thử biệt lập, không phải một submission cần theo dõi trạng thái/hàng đợi.
  `[SoT: Suy luận]` — cần xác nhận: có nên tái dùng đúng interface `JudgeExecutionPort` của
  `judge-orchestration` (import kiểu dữ liệu chung qua `algoprep-common`) hay `problem-bank` định nghĩa
  lại một outbound port hẹp hơn chỉ để "chạy một chương trình với một input, trả output" (không cần khái
  niệm testcase/submission)? BD tạm chọn phương án thứ hai (port hẹp, tên đề xuất
  `SampleSolutionRunnerPort`) vì F2-14 không cần toàn bộ ngữ nghĩa `submission` của F4 — chốt cụ thể ở DD.

### 3.3. Consume domain event (xây read model nội bộ)

| Sự kiện lắng nghe | Từ module | Dùng để cập nhật | Phục vụ |
| :--- | :--- | :--- | :--- |
| `SubmissionGraded` / `SubmissionAccepted` (verdict + tỉ lệ F4-13) | `judge-orchestration` | `problem_stats(problem_id, submission_count, accepted_count, ac_rate)` | F2-11 (trạng thái đã giải, dù việc "đã giải chưa" của **một học viên** cụ thể có thể đọc trực tiếp từ event theo `user_id` thay vì tổng hợp), ngưỡng "Bài cần chú ý" AC < 30% (`DEC-2026-0831-problem-management-lifecycle-details`) |

`identity` sở hữu "đã giải hay chưa" theo góc nhìn cá nhân học viên (F1-06) qua chính event này; `problem-bank`
chỉ cần góc nhìn tổng hợp theo bài toán (tỉ lệ AC toàn hệ thống) để phục vụ `problem_management` — hai read
model độc lập, không module nào đọc bảng của module kia.

## 4. Bọc hàm vs Standard I/O — trách nhiệm dừng ở đâu

`problem-bank` chỉ **lưu và xác thực cấu trúc** đặc tả (chữ ký hàm hợp lệ theo type schema mà `harness`
định nghĩa — xem `[SoT: Suy luận]` dưới; định dạng stdin/stdout là văn bản mô tả tự do, không parse máy).
`problem-bank` **không sinh mã** — đó là việc của `harness` (F3-02 → F3-06). Ranh giới: `problem-bank` trả
lời "bài này khai gì", `harness` trả lời "sinh mã gì từ khai báo đó".

`[SoT: Suy luận]` — việc xác thực chữ ký hàm khớp type schema của `harness` cần `problem-bank` đọc một bản
sao type schema (qua `algoprep-common` làm shared kernel chỉ chứa kiểu dữ liệu thuần, không chứa logic
sinh mã) hay gọi một outbound port validate-only sang `harness`? Đề xuất: shared kernel kiểu dữ liệu trong
`algoprep-common` (enum `PrimitiveType`, `ArrayType`, v.v.), vì đây là dữ liệu bất biến dùng chung, không
phải logic nghiệp vụ của riêng `harness` — không vi phạm nguyên tắc "không import module khác" (shared
kernel không phải là import module `harness`). Chốt ở DD của cả hai module.

## 5. Ngoại lệ F3-13 — bài chỉ hỗ trợ Standard I/O

`harness` cần biết trước bài nào rơi vào ngoại lệ (kiểu dữ liệu vượt lược đồ, ví dụ cấu trúc dữ liệu tuỳ
biến không có trong type schema của F3) để ẩn tuỳ chọn Bọc hàm ở `problem_detail`
[SoT: `02-bd/architecture/harness.md` mục 2b]. `problem-bank` chốt: khi A2/A3 khai chữ ký hàm, hệ thống
tự kiểm tra kiểu tham số/trả về có nằm trong type schema hay không (đọc shared kernel mục 4); nếu không,
đặt cờ `function_wrapper_supported = false` trên `problem_spec` ngay lúc lưu, không chờ tới khi học viên
mở bài mới phát hiện. Cờ này là dữ liệu, việc *ẩn UI* dựa vào cờ là trách nhiệm của tầng trình bày
(`problem_detail`), không phải của `problem-bank`.

## 6. Ràng buộc bảo mật mang sang từ RD (chi tiết ở `02-bd/security/problem-bank.md`)

- F2-08: không API/log nào của `problem-bank` được trả nội dung input hoặc diff chi tiết của Hidden
  testcase — kể cả API nội bộ dùng cho `judge-orchestration` lấy input để chấm (đó là lấy để **chấm**,
  không phải trả về **cho học viên**; ranh giới này chốt rõ ở security).
- F2-13: bookmark/ghi chú riêng tư tuyệt đối — không có exception cho `INSTRUCTOR`/`ADMIN` dù có quyền
  `PROBLEM_AUTHORING`/`TESTCASE_MANAGEMENT` qua ma trận F1-10 (đây không phải một `FUNCTION` trong ma trận
  của `identity`, nó là quyền sở hữu dữ liệu cá nhân, kiểm ở tầng use case của `problem-bank`).
- Ai được soạn/sửa đề, quản lý testcase: gác bởi Function `PROBLEM_AUTHORING`/`TESTCASE_MANAGEMENT` trong
  ma trận của `identity` — `problem-bank` chỉ *tham chiếu* mã function, không tự thiết kế lại ma trận.

## 7. Việc còn mở — chuyển sang DD

- Tên sự kiện domain cụ thể, payload, có cần Transactional Outbox hay không (mục 3.1) — liên quan câu hỏi
  mở chung ở `.nexa/control/dependency-map.md` mục "Open items".
- **Chốt 2026-09-13 — outbound port F2-14: giữ nguyên `SampleSolutionRunnerPort` (port hẹp riêng)**, không
  dùng chung `JudgeExecutionPort` của `judge-orchestration` (mục 3.2). Không có RD screen nào quyết định
  chi tiết kiến trúc nội bộ này nên không chặn BD màn hình; giữ quyết định BD ban đầu vì tách port hẹp
  giảm rủi ro `problem-bank` vô tình phụ thuộc ngược vào `judge-orchestration` — xác nhận lại ở đầu DD nếu
  phát sinh lý do kỹ thuật khác.
- **Chốt 2026-09-13 — chia sẻ type schema: shared kernel `algoprep-common`**, không dùng port. Đã khớp
  quyết định phía `harness` (`02-bd/architecture/harness.md` mục 4) — cả hai module cùng đọc một enum/AST
  kiểu dữ liệu từ `algoprep-common`, không module nào sở hữu bản sao riêng.
- Ngưỡng cụ thể "kích thước đầu ra tối đa"/"số lần nộp/giờ" mặc định khi A2 không tự khai (RD để ngỏ,
  `[SoT: Suy luận]` — BD đề xuất giá trị cụ thể ở mục 5 file `database`).

## 8. Tham chiếu

- `01-rd/req/problem-bank.md` — đặc tả F2-01 → F2-17.
- `.nexa/domain-registry.json` — domain `problem-bank`, scope liệt kê bảng dữ liệu dự kiến.
- `.nexa/control/dependency-map.md` mục 2, 4 — hạ tầng phụ thuộc, thứ tự build (stage 1 cùng `identity`).
- `.nexa/control/decision-registry.md` — `DEC-2026-0824-dual-submission-model-per-problem`,
  `DEC-2026-0828-remove-rejudge-scope`, `DEC-2026-0830-problem-lifecycle-two-states`,
  `DEC-2026-0831-problem-management-lifecycle-details`, `DEC-2026-0831-problem-authoring-spec-tab`,
  `DEC-2026-0831-problem-authoring-round2`, `DEC-2026-0831-class-assignments-round2`,
  `DEC-2026-0831-partial-score-testcase-ratio`.
- `02-bd/architecture/harness.md` — phía tiêu thụ đặc tả hàm do module này cung cấp.
- `06-plan/260912-1055-bd-rollout-order.md` — thứ tự triển khai BD.
