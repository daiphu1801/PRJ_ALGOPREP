# AlgoPrep — Backend

Java 21 LTS + Spring Boot 4.0.8 + Maven multi-module. Modular Monolith: **một tiến trình, một
PostgreSQL instance, tám module Maven**. Spec đầy đủ: `01-rd/system/backend_architecture.md`. Cây
thư mục monorepo: `01-rd/system/codebase_structure.md`.

## Trạng thái: khung base (2026-09-01)

Scaffold — POM cha, 8 module với cây 4 tầng, bộ 5 cổng chất lượng chặn thật, hạ tầng local chạy
được. **Chưa có một dòng logic nghiệp vụ nào**: `02-bd/` và `03-dd/` gần như trống nên mọi module
nghiệp vụ chỉ có `pom.xml` + `package-info.java` từng tầng.

Năm quyết định kiến trúc nền của khung base ghi thành **`DEC-2026-0901-backend-base-architecture`**
trong `.nexa/control/decision-registry.md` (P1 theo `sot-precedence.md`) — đọc ở đó, không phải ở
file này. Kế hoạch thực hiện: `06-plan/nexa-plan/260831-2323-backend-base-scaffold.md`.

| Module Maven | Package Java | Schema | Trạng thái |
| :--- | :--- | :--- | :--- |
| `algoprep-common` | `com.algoprep.common` | (không) | **Có class thật** — ngoại lệ nền, `ClockPort`, `IdGeneratorPort`, `DomainEvent` |
| `algoprep-identity` | `com.algoprep.identity` | `identity` | Khung 4 tầng, chờ BD/DD |
| `algoprep-problem-bank` | `com.algoprep.problembank` | `problem` | Khung 4 tầng, chờ BD/DD |
| `algoprep-harness` | `com.algoprep.harness` | `harness` | Khung 4 tầng, chờ BD/DD |
| `algoprep-judge` | `com.algoprep.judge` | `judge` | Khung 4 tầng, chờ BD/DD |
| `algoprep-ai-review` | `com.algoprep.aireview` | `ai` | Khung 4 tầng, chờ BD/DD |
| `algoprep-interview-bank` | `com.algoprep.interviewbank` | `interview_bank` | Khung 4 tầng, chờ BD/DD |
| `algoprep-bootstrap` | `com.algoprep.bootstrap` | (không) | **Có class thật** — khởi chạy, bảo mật, xử lý lỗi, bộ test ranh giới |

Package Java **không có dấu gạch** (`problembank`) dù tên module có (`algoprep-problem-bank`) — Java
không cho dấu gạch trong tên package. Tên schema **ngắn hơn** tên context ở ba chỗ
(`problem-bank` → `problem`, `judge-orchestration` → `judge`, `ai-review` → `ai`). Bảng ánh xạ đầy
đủ: `.nexa/domain-registry.json`; luật chống trôi: `01-rd/system/codebase_structure.md` mục 3.

## Lệnh

```bash
cd 05-coding/backend

./mvnw verify                     # G-CHECK — PHẢI PASS trước khi báo xong (CLAUDE.md)
./mvnw spotless:apply             # tự sửa định dạng (chạy cái này trước khi verify nếu vừa viết code)
./mvnw -q install -DskipTests     # cần một lần trước khi spring-boot:run được với -pl

# Chạy app với hạ tầng local:
cd ../ && docker compose up -d && set -a && . ./.env && set +a && cd backend
./mvnw -pl algoprep-bootstrap spring-boot:run -Dspring-boot.run.profiles=local
curl http://localhost:8080/actuator/health
```

**`./mvnw verify` KHÔNG cần Docker** — có chủ đích. Profile `test` loại trừ autoconfigure của
PostgreSQL/Redis/RabbitMQ, nên cổng chất lượng cho ra cùng một kết quả dù máy có bật hạ tầng hay
không. Test tích hợp có hạ tầng thật dùng Testcontainers và thuộc slice của từng module
(`01-rd/system/environment.md` mục 5 — **không** mock database bằng H2).

**Spring Boot không đọc `05-coding/.env`.** Phải `set -a && . ./.env && set +a` trước khi
`spring-boot:run`, nếu không app dùng giá trị mặc định trong `application-local.yml`.

## Điều `mvn verify` chặn thật (không phải quy ước)

Mỗi dòng dưới đây đã được kiểm bằng **một vi phạm cố tình** rồi xác nhận build đỏ, không phải bằng
đọc cấu hình. 14/14 chặn thật, chi tiết ở `06-plan/reports/260901-0230-backend-base-scaffold.md`.

| Cổng | Luật | Cơ chế |
| :--- | :--- | :--- |
| 0 | JDK ngoài `[21,26)` hoặc Maven < 3.9.9 | `maven-enforcer-plugin` |
| 1 | Sai định dạng Google Java Style | Spotless + google-java-format, `lineEndings=UNIX` |
| 2 | Phương thức > 50 dòng · tham số > 5 · wildcard import | Checkstyle (`config/checkstyle.xml`) — mọi luật ở mức fail, không có luật cảnh báo |
| 3 | Null dereference, resource leak, mật khẩu hard-code, SQL injection | SpotBugs `effort=Max` + FindSecBugs |
| 4 | Module nghiệp vụ import module nghiệp vụ khác | ArchUnit `ModuleBoundaryTest` Luật 1 |
| 4 | Ai đó phụ thuộc `algoprep-bootstrap` | ArchUnit Luật 2 (Maven cũng chặn bằng phát hiện vòng) |
| 4 | `algoprep-common` phụ thuộc ra ngoài | ArchUnit Luật 3 (Maven cũng chặn bằng phát hiện vòng) |
| 4 | Chạm `presentation`/`infrastructure` của module khác | ArchUnit Luật 4 |
| 5 | Spring/JPA/Jackson/Hibernate xuất hiện trong `domain` | ArchUnit `LayerRuleTest` Luật tầng 1 |
| 5 | `presentation` gọi thẳng `infrastructure` | ArchUnit Luật tầng 2 |
| 5 | `application` biết HTTP (`jakarta.servlet`, `org.springframework.web`) | ArchUnit Luật tầng 3 |
| 5 | Tồn tại `domain/repository` hoặc `application/usecase`; class (không phải interface) trong `ports` | ArchUnit Luật tầng 4 |
| 6 | Request KHÔNG chạy trên virtual thread | `VirtualThreadsEnabledTest` |

Cổng 6 tồn tại vì bỏ `spring.threads.virtual.enabled` thì app **vẫn chạy** và `mvn verify` **vẫn
xanh**, chỉ có toàn bộ `DEC-2026-0820-stack-versions` (chọn Java 21 thay vì 17 CHỈ vì Virtual
Threads) bị vô hiệu trong im lặng cho tới lúc đo k6.

## JaCoCo: ngưỡng 80% đã cấu hình, chặn thì bật theo module

Ngưỡng `01-rd/system/environment.md` mục 3.A yêu cầu (**≥ 80%** cho `application` và `domain`) đã
nằm trong POM cha. `haltOnFailure` đang `false` và được bật **theo từng module** bằng cách ghi đè
`<jacoco.haltOnFailure>true</jacoco.haltOnFailure>` trong pom của module đó, đúng lúc module có class
nghiệp vụ đầu tiên (`DEC-2026-0901-backend-base-architecture` điểm 3).

Đây là **lộ trình bật chặn, không phải hạ chuẩn**: ở base hai tầng đó chưa có class nào, nên rule
sẽ hoặc pass rỗng (một con số vô nghĩa che mất suy giảm về sau) hoặc fail build trên một scaffold
đúng.

Tầng `infrastructure` và `presentation` bị loại khỏi bundle bằng `<excludes>` ở mức plugin — nhờ vậy
con số JaCoCo báo ra **chính là** con số của `domain` + `application`, không phải một trung bình pha
loãng.

## Còn thiếu (không dựng ở bước base, có chủ đích)

- **JWT và RBAC ba vai trò** — `SecurityConfig` chỉ mở `/actuator/health` và `/actuator/prometheus`,
  chặn mọi thứ còn lại. Cần `03-dd/api/identity.md`.
- **Danh mục mã lỗi** — `GlobalExceptionHandler` ánh xạ theo ba loại ngoại lệ nền, chưa theo danh
  mục. Cần `03-dd/api/api.md`.
- **Rate limiting Token Bucket trên Redis** cho ba nhóm endpoint (đăng nhập/đăng ký, nộp bài, gọi
  AI) — `01-rd/req/nfr.md` mục D chốt yêu cầu, nhưng cần đường dẫn endpoint thật mới cấu hình được
  filter theo nhóm.
- **CORS** — frontend Next.js khác origin nên sẽ cần, nhưng danh sách origin theo môi trường là một
  quyết định, không phải giá trị đoán được.
- **Entity JPA, migration** — bảng/cột/khoá/index thuộc `02-bd/database/<module>.md`. Công cụ
  migration (Flyway / Liquibase) **chưa chốt**.
- **`GoJudgeAdapter`** — thuộc slice `judge-orchestration`. Hạ tầng go-judge đã chạy và đã kiểm chạy
  được mã thật; xem `05-coding/judge-engine/README.md`.
- **Pipeline GitHub Actions** — `01-rd/system/environment.md` mục 6 hẹn "khi có mã thật để chạy CI".
  Giờ đã có mã thật.

## Nợ kỹ thuật đã biết

| # | Nợ | Gỡ khi |
| :-: | :--- | :--- |
| 1 | `archunit.properties` đặt `archRule.failOnEmptyShould=false`. Cần thiết vì 5 module chưa có class nào nên phần lớn luật khớp rỗng một cách hợp lệ. **Để nguyên `false` vĩnh viễn nghĩa là một luật viết sai tên package sẽ im lặng mãi mãi** — đúng kiểu hỏng mà bộ test này tồn tại để chặn | Cả sáu module có class nghiệp vụ → đổi thành `true` |
| 2 | `application-test.yml` loại trừ autoconfigure của DataSource/JPA/Redis/RabbitMQ để `verify` không cần Docker | Module đầu tiên có Entity JPA thật → chuyển sang Testcontainers, xoá dần từng dòng loại trừ |
| 3 | `/actuator/prometheus` mở công khai để Prometheus scrape được | Trước khi triển khai thật → chuyển actuator sang `management.server.port` riêng, không publish |
| 4 | Spring Security sinh user `user` + mật khẩu ngẫu nhiên mỗi lần khởi động (log có cảnh báo của chính Spring) | Có `UserDetailsService` thật từ slice `identity` |
| 5 | `CommonPortsConfig` dùng anonymous class cho `ClockPort`/`IdGeneratorPort` thay vì class adapter có tên | Khi cần fake trong test tích hợp, hoặc khi cần đặt tên để log |
| 6 | Hikari `maximum-pool-size: 10` là mặc định, chưa tính theo tải | Có số liệu k6 thật; ngưỡng đồng thời cũng chưa được chốt (`01-rd/req/nfr.md` mục A) |
| 7 | CSRF vẫn bật với `SessionCreationPolicy.STATELESS` — endpoint `POST` đầu tiên sẽ trả 403 | Có luồng refresh-token thật → cấu hình `CookieCsrfTokenRepository` theo `nfr.md` mục D. **Cố tình để vậy**: kiểu hỏng ồn ào tốt hơn một lỗ bảo mật im lặng |
| 8 | Image `criyle/go-judge` mặc định **không có compiler nào** (không Python, không g++, không javac) | Slice `judge-orchestration` → dựng image riêng. Chi tiết: `05-coding/judge-engine/README.md` |

## Bẫy môi trường đã gặp thật

Ba thứ mất thời gian nhất khi dựng base, ghi lại để không ai phải tìm lại.

1. **`core.autocrlf=true` đánh nhau với Spotless.** Máy phát triển đổi LF thành CRLF lúc checkout,
   Spotless chốt LF, nên vừa `git clone` xong là `verify` đỏ trên một cây mã không ai sửa gì. Đã
   đóng bằng `05-coding/backend/.gitattributes` (`eol=lf` thắng `core.autocrlf`). `mvnw` bắt buộc LF
   — CRLF làm nó chết trên Linux với thông báo `bad interpreter`.
2. **PostgreSQL cài native chiếm cổng 5432.** Docker proxy cùng listen 5432 nhưng thua, nên mọi kết
   nối từ host đi vào bản native. Triệu chứng không hề gọi tên cổng:
   `FATAL: password authentication failed for user "algoprep"` — đọc lên tưởng sai mật khẩu. Kiểm
   bằng `netstat -ano | grep ":5432 "`, nhiều hơn một PID là có xung đột. Sửa: đổi `POSTGRES_PORT`
   **và** `ALGOPREP_DB_PORT` trong `.env`.
3. **Quên bật Docker thì lỗi không hề nói là quên bật Docker.** `spring-boot:run` với profile `local`
   chết bằng stack trace Hibernate `Unable to determine Dialect without JDBC metadata`, và nguyên nhân
   thật (`Connection refused` tới PostgreSQL) nằm sâu trong phần `Caused by`. Nếu app không khởi động
   được với profile `local`, kiểm `docker compose ps` **trước** khi đọc stack trace.

4. **`TestRestTemplate` đã bị dời khỏi `spring-boot-test` ở Spring Boot 4.** Package
   `org.springframework.boot.test.web.client` không còn tồn tại. `VirtualThreadsEnabledTest` dùng
   `java.net.http.HttpClient` của JDK — không thêm dependency, không phụ thuộc vào việc lớp tiện ích
   đó nằm ở artifact nào.
