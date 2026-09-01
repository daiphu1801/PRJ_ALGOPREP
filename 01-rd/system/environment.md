# Môi Trường Phát Triển & Cổng Chất Lượng (Environment & Tooling)

Phiên bản công nghệ đã chốt, hạ tầng local cần chạy, công cụ kiểm soát chất lượng, và các lệnh bắt buộc phải
chạy trước khi báo xong việc.

Nguồn phiên bản: `README.md` mục 5 và `DEC-2026-0820-stack-versions`.

---

## 1. Phiên bản đã chốt

| Thành phần | Phiên bản | Ghi chú |
| :--- | :--- | :--- |
| JDK | **Java 21 LTS** (Temurin hoặc GraalVM) | Chọn 21 chứ không phải 17 để dùng Virtual Threads ở trạng thái GA, không cần preview flag. Compile target `<release>21</release>` |
| Framework backend | **Spring Boot 4.0.x** | Nền Spring Framework 7, Jakarta EE 11, Spring Security 7. **Không dùng nhánh 3.x** |
| Build tool backend | **Maven** (dùng `mvnw` trong `05-coding/backend/`) | Multi-module. Gradle không phải phương án — để ngỏ hai build tool là mời người sau chọn sai |
| Node.js | **≥ 20.9 LTS** | |
| Package manager frontend | **pnpm** | Khai trong `packageManager` của `05-coding/package.json`. Không dùng `npm` hay `yarn` — lockfile là `pnpm-lock.yaml`, chạy `npm` sẽ sinh lockfile thứ hai và cây `node_modules` khác |
| Framework frontend | **Next.js 16.x** (App Router) | TypeScript chế độ `strict` |
| Docker | Docker Desktop hoặc Docker Engine + Compose v2 | Bắt buộc — go-judge chỉ chạy được bằng container |

**Ràng buộc hệ điều hành — đã đóng cho luồng mặc định.** Judge0 yêu cầu cgroup v1, trong khi Linux hiện đại
mặc định v2 — đây từng là **rủi ro số một của đề tài (R1)**. Đã đóng bằng
`DEC-2026-0823-go-judge-default-engine`: engine mặc định là **go-judge**, hỗ trợ cả cgroup v1 và v2, chạy
thẳng được trong container trên Windows/WSL2 mà không cần ép tham số kernel hay dựng VM riêng. Chi tiết cài
đặt: `01-rd/system/judge_engine.md`. Rủi ro cgroup v1 chỉ còn nếu `Judge0Adapter` được cắm lại làm engine
thay thế.

---

## 2. Hạ tầng local

Khởi chạy bằng `05-coding/docker-compose.yml` — **nằm trong `05-coding/`, không ở root repo**
(`codebase_structure.md` mục 2).

| Dịch vụ | Vai trò | Ghi chú |
| :--- | :--- | :--- |
| **PostgreSQL 16** | Database chính, **sáu schema** | Schema tạo sẵn bằng script trong `05-coding/database/init/` mount vào `/docker-entrypoint-initdb.d/`: `identity` · `problem` · `harness` · `judge` · `ai` · `interview_bank` |
| **Redis 7** | Cache, `ChatMemory` của phiên phỏng vấn, rate limit Token Bucket, khoá phân tán | `README.md` mục 5 |
| **RabbitMQ** | Hàng đợi bài nộp | Bật management plugin cho việc quan sát hàng đợi lúc phát triển |
| **MinIO** | Giả lập S3 cho bộ testcase lớn | Cần service khởi tạo bucket sẵn |
| **go-judge** | Thực thi mã trong sandbox `go-sandbox` (mặc định — `DEC-2026-0823-go-judge-default-engine`) | **Stateless** — không cần Postgres/Redis riêng như Judge0. Nếu `Judge0Adapter` được cắm lại sau này, nó vẫn cần Postgres/Redis riêng của Judge0 |
| **Prometheus + Grafana** | Giám sát | Có thể bật muộn, nhưng phải có trước khi báo cáo hiệu năng |

**Judge engine là dịch vụ ngoài, có chủ đích cách ly.** go-judge (hay Judge0 nếu cắm lại) là dịch vụ ngoài
(`backend_architecture.md` mục 6.A); cho nó dùng chung database với AlgoPrep là phá luôn ranh giới vừa
dựng, và mọi lần nâng cấp engine sẽ chạm vào schema nghiệp vụ. go-judge không giữ state nên câu hỏi này
không phát sinh với engine mặc định, nhưng nguyên tắc cách ly vẫn giữ nguyên nếu đổi engine.

**Trạng thái 2026-08-20:** chưa file hạ tầng nào tồn tại — xem `codebase_structure.md` mục 5.

---

## 3. Công cụ kiểm soát chất lượng

### 3.A. Backend (Java / Spring Boot)

| Công cụ | Nhiệm vụ | Ngưỡng chặn |
| :--- | :--- | :--- |
| **Spotless** | Định dạng mã theo Google Java Style | Sai định dạng là **fail build** |
| **Checkstyle** (`config/checkstyle.xml`) | Kiểm cấu trúc mã | Phương thức ≤ 50 dòng · tham số ≤ 5 · cấm wildcard import |
| **SpotBugs + FindSecBugs** | Phân tích tĩnh tìm lỗi và lỗ hổng | Null pointer, resource leak, mật khẩu hard-code, SQL injection |
| **JaCoCo** | Đo độ bao phủ test | **≥ 80%** cho tầng `application` và `domain` |
| **Test kiểm ranh giới module** | Chặn import chéo giữa các Bounded Context | Import trái phép là **fail build** |

**Về test kiểm ranh giới module.** Đây là **cách duy nhất** ranh giới module được bảo vệ tự động — không có
nó thì Modular Monolith trôi thành monolith phẳng sau vài sprint, và toàn bộ lý do chia module ở
`backend_architecture.md` mục 1 thành trang trí. Cụ thể phải chặn được ba luật ở
`backend_architecture.md` mục 2.A, cộng luật `presentation`/`infrastructure` của module này không xuất hiện
trong import của module khác.

Công cụ hiện thực **đã chốt: ArchUnit, không dùng Spring Modulith** — `DEC-2026-0901-backend-base-architecture`
điểm 1, quyết định đúng lúc dựng `algoprep-bootstrap` như tài liệu này hẹn. Lý do: bốn luật tầng ở
`backend_architecture.md` mục 3.A là luật mức package **bên trong** một module, còn Spring Modulith chỉ hiểu
ranh giới **giữa** các module theo quy ước package riêng của nó. Bộ test nằm ở
`algoprep-bootstrap/src/test/java/com/algoprep/bootstrap/architecture/` (7 luật), và cả 7 đã được kiểm bằng
vi phạm cố tình — build đỏ đúng luật tương ứng.

**Riêng cho `algoprep-harness` (F3).** Bộ sinh mã phải có **test so khớp mã sinh ra (golden file)** cho cả ba
ngôn ngữ: một đặc tả kiểu vào, so mã sinh ra với mã đã chốt. Đây là bộ test rẻ nhất và bắt lỗi tốt nhất của
cả đề tài, vì sinh mã là hàm thuần và tất định. Không được thay nó bằng test tích hợp qua judge engine thật. [SoT: Suy luận]

### 3.B. Frontend (TypeScript / Next.js)

| Công cụ | Nhiệm vụ |
| :--- | :--- |
| **TypeScript** chế độ `strict` | Không `any` ngầm |
| **ESLint** + **`eslint-plugin-boundaries`** | Chặn import sai chiều tầng FSD — luật này **phải chặn thật**, không để ở mức cảnh báo |
| **Prettier** | Định dạng thống nhất |
| **Husky** + **lint-staged** | Hook `pre-commit` chạy ESLint và Prettier trên file đã stage |
| **Vitest** | Unit test cho hàm thuần, hook, mapper |
| **Playwright** | E2E theo luồng người dùng |

**Luồng E2E tối thiểu phải phủ** [SoT: Suy luận — suy ra từ `README.md` mục 2 và mục 4]:
đăng nhập → mở một bài toán → chạy thử với testcase Sample → nộp bài → thấy kết quả từng testcase cập nhật
realtime → đạt `Accepted` → mở một trong hai luồng AI. Đó là luồng xương sống của hệ thống; nó đỏ thì không
có gì khác đáng tin.

Cơ chế hook: **Husky là cơ chế duy nhất**, không dựng song song `.githooks/`. Tên branch và commit **không
dùng mã ticket**.

---

## 4. Cổng chất lượng phải chạy trước khi báo xong (G-CHECK)

| Phạm vi thay đổi | Lệnh bắt buộc |
| :--- | :--- |
| `05-coding/frontend/` | `pnpm check` — gộp typecheck + lint + Vitest |
| `05-coding/backend/` | `./mvnw verify` — gồm Spotless, Checkstyle, SpotBugs, JaCoCo ≥ 80%, test kiểm ranh giới module |
| Có thay đổi luồng người dùng | thêm `pnpm e2e` (Playwright) |

**Báo PASS không kèm output lệnh thì không tính.** Đây là ràng buộc quy trình, không phải khuyến nghị —
`CLAUDE.md` mục Rules.

---

## 5. Kiểm thử tích hợp và hiệu năng

| Loại | Công cụ | Dùng cho |
| :--- | :--- | :--- |
| Unit (backend) | **JUnit 5** | Logic `domain` và `application`, đặc biệt là bộ sinh mã F3 |
| Tích hợp | **Testcontainers** | PostgreSQL, Redis, RabbitMQ thật trong container. **Không** mock database bằng H2 — hành vi khác Postgres đủ để test xanh mà production đỏ |
| Tải | **k6** | Luồng nộp bài đồng thời — đây là chỗ Virtual Threads phải chứng minh được điều `overview.md` mục 1.D khẳng định |
| E2E | **Playwright** | Luồng người dùng trên trình duyệt |

**Judge engine trong test.** Vì go-judge nhẹ và không kén cgroup version (khác Judge0), test tích hợp của
`algoprep-judge` có thể chạy **go-judge thật** trong Testcontainers thay vì chỉ dùng giả (stub HTTP) — đây
là cải thiện thật so với thời còn Judge0, khi test tích hợp phải giả engine vì Judge0 thật khó dựng trong
CI (cần điều kiện cgroup v1 cụ thể). Vẫn nên giữ một lớp test dùng stub cho các trường hợp biên (timeout,
lỗi engine) mà khó tái tạo bằng engine thật. [SoT: Suy luận]

---

## 6. Điều tài liệu này KHÔNG chốt

| Chưa chốt | Sẽ chốt ở |
| :--- | :--- |
| ~~Công cụ kiểm ranh giới module~~ | **Đã chốt**: ArchUnit, `DEC-2026-0901-backend-base-architecture` điểm 1 |
| ~~Phiên bản image trong `docker-compose.yml`~~ | **Đã chốt** khi tạo file (2026-09-01), pin cứng: `postgres:16.15-alpine` · `redis:7.4-alpine` · `rabbitmq:4.2.9-management-alpine` · `criyle/go-judge:v1.12.3` · `minio/minio:RELEASE.2025-09-07T16-13-09Z` · `prom/prometheus:v3.14.0` · `grafana/grafana:13.0.7` |
| Công cụ migration schema (Flyway / Liquibase) | Cùng `02-bd/database/<module>.md` đầu tiên |
| Cấu hình pipeline GitHub Actions | Đã có mã thật để chạy CI — làm ở slice sau |
| Nhà cung cấp LLM và cách quản khoá API giữa các môi trường | Quyết định riêng |
