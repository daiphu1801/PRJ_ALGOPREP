# BD — Database module `harness` (F3)

> Đọc cùng `02-bd/architecture/F3_harness.md` trước — đặc biệt mục 4 (lược đồ kiểu) và mục 5 (interface
> plugin). File này trả lời dứt khoát câu hỏi mở ở `.nexa/control/dependency-map.md` mục "Open items":
> *"Whether `harness` needs its own schema at all, or is stateless with templates living in `problem`, is
> undecided."*

## 1. Quyết định — schema `harness` tồn tại nhưng rỗng, không có bảng nào

**Chốt: schema `harness` vẫn được tạo (`CREATE SCHEMA harness`, để đặt tên module và schema nhất quán theo baseline) nhưng KHÔNG có bảng nào trong PostgreSQL** (`DEC-2026-1022-harness-empty-schema-kept`). Sửa 2026-10-22 (đồng bộ DB vòng 4, DEC-2026-1022-db-sync-round4): trước đây dòng này ghi "không có schema riêng", mâu thuẫn với mục 6 bên dưới và với quyết định trên; nay thống nhất là schema có, bảng không. Toàn bộ nội dung
domain-registry liệt kê ở scope `harness` (`type_schema`, `code_template`, `generated_harness`,
`match_strategy`, `error_mapping` — `.nexa/domain-registry.json` domains[2].scope) được xử lý như sau:

| Scope (theo domain-registry) | Nơi thực sự lưu | Vì sao không cần bảng riêng |
| :--- | :--- | :--- |
| `type_schema` | Mã nguồn (`algoprep-common`, shared kernel Java) — enum `TypeKind` + record cấu trúc lồng, **không phải dữ liệu runtime** | Đây là một tập hằng số đóng (mục 4.2 file architecture), thay đổi = thay đổi mã nguồn + release, không phải thao tác CRUD của người dùng cuối. Lưu trong Postgres sẽ đòi thêm một tầng cache + đồng bộ không cần thiết cho dữ liệu không bao giờ đổi lúc chạy |
| `code_template` | Tài nguyên tĩnh đóng gói cùng jar (`src/main/resources/harness-templates/{java,cpp,python}/...`), không phải bảng | Khung mã nguồn (phần "trước"/"sau" vùng chèn thân hàm người học) là **tài sản mã nguồn** của đội phát triển, đổi qua Pull Request + review + golden-file test (`01-rd/req/nfr.md` mục E), không qua thao tác nghiệp vụ nào của A2/A3. Đặt trong Postgres sẽ cho phép sửa template ngoài luồng review mã nguồn — rủi ro bảo mật (xem `02-bd/security/F3_harness.md` mục 1) |
| `generated_harness` | **Không lưu** — sinh mã theo yêu cầu (on-demand) mỗi lần cần chấm, không cache kết quả sinh mã vào Postgres | Mã sinh ra phụ thuộc chữ ký hàm hiện hành (các cột `function_name`/`return_type`/`parameters`/`name_overrides` của `problem_specs`, đọc qua shared kernel + `problem-bank`; sau gộp PB-10 không còn bảng `function_signatures`) và mã nguồn người học của chính submission đó — không có giá trị tái sử dụng giữa các submission khác nhau; cache (nếu cần vì hiệu năng) là **Redis**, không phải Postgres (mục 2 dưới) |
| `match_strategy` | **Không lưu ở `harness`** — `matching_strategy`/`epsilon_value` đã có bảng sở hữu là `problem_specs` bên schema `problem` (`02-bd/database/F2_problem-bank.md` mục 1.4). `harness` chỉ **đọc** giá trị này khi sinh mã so khớp, không sở hữu bản sao | Tránh trùng lặp dữ liệu hai schema; `harness` không có nhu cầu ghi/sửa chiến lược so khớp, chỉ tiêu thụ |
| `error_mapping` | Không lưu — là một phép biến đổi thuần (hàm số học trừ dòng, mục 7 file architecture), không có trạng thái cần persist | Không có "lịch sử ánh xạ lỗi" nào cần tra cứu lại sau này |

**Kết luận:** `harness` là **pure computation module** đúng như đã ghi ở `.nexa/control/dependency-map.md`
mục 2 ("Pure computation — no external I/O in the generation path") — không tự sở hữu bảng Postgres nào.
Module Maven `algoprep-harness` vẫn tồn tại (bắt buộc theo `DEC-2026-0820-architecture-baseline`, tám module
cố định), nhưng tầng `infrastructure` của nó không có adapter Postgres nào, chỉ có adapter đọc tài nguyên
tĩnh (classpath resource loader cho `code_template`) và một outbound port gọi sang `problem-bank` (hoặc đọc
qua domain event, mục 3) để lấy chữ ký hàm từ `problem_specs`.

### 1.1. Vì sao không chọn phương án "có schema riêng"

Phương án loại: tạo schema `harness` với bảng `code_templates(language, model, template_source)` để A3 sửa
template qua UI Admin. Loại vì ba lý do:

1. **Không có yêu cầu RD nào** cho phép Admin tự sửa mã khung qua giao diện — `01-rd/req/F3_harness.md` không
   nhắc tới UI quản lý template; phần "Không chọn" ở `02-bd/architecture/F3_harness.md` mục 3 đã loại bỏ hẳn ý
   tưởng "Admin tự định nghĩa ngôn ngữ mới qua UI" với đúng lý do rủi ro bảo mật — sửa template runtime qua
   Postgres mang cùng rủi ro đó ở quy mô nhỏ hơn (không phải thêm ngôn ngữ mới, nhưng vẫn là "mã tuỳ ý chạy
   trong sandbox go-judge do một actor không phải lập trình viên soạn").
2. **Template không đổi theo môi trường** (dev/staging/prod dùng chung một bộ, không có "template riêng
   cho từng bài toán") — không có trục dữ liệu nào (`problem_id`, `class_id`...) để làm khoá chính hợp lý
   cho một bảng.
3. Test golden-file (`01-rd/req/nfr.md` mục E) đối chiếu template với kết quả mong đợi **tại thời điểm
   build/CI**, đòi hỏi template ổn định trong mã nguồn tại thời điểm đó — template sống trong Postgres của
   môi trường runtime phá vỡ giả định này.

`[SoT: Suy luận]` — đây là quyết định của BD này (câu hỏi mở tại `.nexa/control/dependency-map.md`), không
phải trích dẫn RD; RD không nói rõ nơi lưu, chỉ liệt kê scope ở domain-registry.

## 2. Redis — cache duy nhất liên quan tới `harness` (không phải nguồn sự thật)

`harness` không sở hữu Redis riêng nhưng **có thể** dùng cache đọc chữ ký hàm (nằm trong `problem_specs`) do `problem-bank`
cung cấp, để tránh gọi lại mỗi lần chấm testcase trong cùng một submission:

| Key | TTL | Dùng cho | Sở hữu |
| :--- | :--- | :--- | :--- |
| `problem:spec:<problemId>` | Invalidate chủ động khi dòng `problem_specs` đổi, đã gồm chữ ký hàm (sự kiện `ProblemSpecChanged`; sửa 2026-10-22 phase 2) | `harness` đọc để sinh mã, tránh join lại nhiều bảng bên schema `problem` mỗi lần sinh | **`problem-bank`** sở hữu key này (đã khai ở `02-bd/database/F2_problem-bank.md` mục 3) — `harness` chỉ là bên đọc, không tạo key mới |

`harness` **không cần tự thêm Redis key nào của riêng nó** — nếu về sau đo hiệu năng thấy sinh mã lặp lại
tốn kém, cân nhắc thêm cache in-process (JVM heap, ví dụ Caffeine) theo `problem_id` + `spec_version` thay vì
Redis, vì mã sinh ra chỉ dùng nội bộ một lần gọi trong cùng tiến trình JVM của `algoprep-judge`/`algoprep-
harness`, không cần chia sẻ giữa nhiều instance — chốt cụ thể ở DD nếu đo được nhu cầu thật.

## 3. Giao tiếp dữ liệu với `problem-bank` (không import schema)

Theo đúng nguyên tắc `DEC-2026-0820-architecture-baseline` (không import schema khác):

- `harness` nhận chữ ký hàm (bốn cột của `problem_specs`, đã ở dạng JSONB theo lược đồ kiểu mục 4 file architecture) qua lời
  gọi outbound port mà `harness` tự khai báo trong package port đầu ra của module (cấu trúc package chốt ở DD; ví dụ `ProblemSpecReaderPort` — tên đề
  xuất, chốt ở DD), adapter thật gọi sang `problem-bank` (cùng JVM, modular monolith — có thể là lời gọi Java
  trực tiếp qua interface dùng chung, không cần HTTP).
- `harness` lắng nghe sự kiện `ProblemSpecChanged` (`02-bd/architecture/F2_problem-bank.md` mục 3.1) chỉ để biết
  **khi nào bỏ cache in-process** (mục 2) nếu có — không lưu lại bản sao chữ ký hàm nào ở phía
  mình.

## 4. Migration

**Không có migration bảng nào cho schema `harness`** — schema được tạo rỗng (`CREATE SCHEMA harness`), không có bảng để migrate. Công cụ migration là Flyway (`DEC-2026-1029-flyway-migration-tool`), mỗi module một thư mục
`db/migration/<schema>`; `algoprep-harness` không có thư mục này và không chứa
file migration SQL nào, chỉ chứa tài nguyên template tĩnh dưới `src/main/resources/`.

## 5. Việc còn mở — chuyển sang DD

- Tên cụ thể outbound port đọc chữ ký hàm (mục 3) và có tái dùng `algoprep-common` DTO hay tự định
  nghĩa DTO riêng cho response.
- Có cần cache in-process (Caffeine) hay không — chỉ quyết định sau khi đo hiệu năng thật ở giai đoạn build
  (mục 2), không quyết định trước ở BD.
- Cấu trúc thư mục `src/main/resources/harness-templates/` cụ thể theo ngôn ngữ/mô hình (Bọc hàm vs Standard
  I/O) — chốt ở DD cùng lúc với interface plugin (`02-bd/architecture/F3_harness.md` mục 5).

- Sửa 2026-10-22 (đồng bộ DB, phase 1, HN-04): `02-bd/architecture/F3_harness.md` mục 4.2 hoặc DD `harness` phải ghi rõ số chiều mảng tối đa 3, độ sâu lồng tối đa 3, và kiểu `OTHER` **không bao giờ được lưu** vào `problem_specs.return_type`/`parameters` (trước phase 2 là `function_signatures`) (chỉ dùng như giá trị báo "không hỗ trợ" lúc tính `function_wrapper_supported`). File này không sửa kiến trúc.
- Sửa 2026-10-22 (đồng bộ DB, phase 2, DEC-2026-1022-problem-bank-schema-sync) (HN-06): đã chốt gộp `function_signatures` vào `problem_specs` (`02-bd/database/F2_problem-bank.md` mục 1.4; mục 1.5 chỉ còn con trỏ). Mọi tham chiếu tới mục 1.4, 1.5 của file kia (đầu file, mục 1 bảng `match_strategy`, mục 2, mục 6) đã đọc theo bảng gộp. `harness` vẫn không sở hữu bảng nào; `spec_version` nay tăng khi bất kỳ cột nào của dòng `problem_specs` đổi.

## 6. Tham chiếu

- `.nexa/domain-registry.json` — domain `harness`, schema khai `"harness"` nhưng BD này chốt: schema đó
  **không có bảng nào**, chỉ tồn tại trên danh nghĩa cho nhất quán đặt tên module/schema theo baseline tám
  module (`DEC-2026-0820-architecture-baseline`) — không phải mâu thuẫn với domain-registry, mà là làm rõ
  "schema tồn tại về mặt đặt tên, rỗng về mặt bảng dữ liệu".
- `.nexa/control/dependency-map.md` mục 2 (pure computation), mục "Open items" (câu hỏi được trả lời ở đây).
- `02-bd/architecture/F3_harness.md` mục 4 (lược đồ kiểu), mục 5 (interface plugin).
- `02-bd/database/F2_problem-bank.md` mục 1.4 (đã gộp 1.5), mục 3 (nơi thật sự lưu `matching_strategy`,
  chữ ký hàm gộp trong `problem_specs`, key Redis `problem:spec:<problemId>`).
