# AlgoPrep — Backend

Java 21 LTS + Spring Boot 4.0.8 + Maven multi-module. Modular Monolith: **một tiến trình, một
PostgreSQL instance, tám module Maven**. Spec đầy đủ: `01-rd/system/SYS0101_backend_architecture.md`. Cây
thư mục monorepo: `01-rd/system/SYS0201_codebase_structure.md`.

## Trạng thái (cập nhật 2026-11-15)

Cả tám module đã có mã nghiệp vụ thật, kèm bộ test và bộ cổng chất lượng chặn thật. Đợt khung base
ban đầu (2026-09-01) dựng POM cha, tám module với cây 4 tầng, bộ cổng chất lượng và hạ tầng local;
khi đó các module nghiệp vụ chỉ có `pom.xml` và `package-info.java`. Hiện không còn file
`package-info.java` nào trong backend.

Năm quyết định kiến trúc nền của khung base ghi thành **`DEC-2026-0901-backend-base-architecture`**
trong `.nexa/control/decision-registry.md` (P1 theo `sot-precedence.md`) — đọc ở đó, không phải ở
file này. Kế hoạch thực hiện: `06-plan/nexa-plan/260831-2323-backend-base-scaffold.md`.

| Module Maven | Package Java | Schema | Trạng thái |
| :--- | :--- | :--- | :--- |
| `algoprep-common` | `com.algoprep.common` | (không) | **Có class thật** — ngoại lệ nền, `ClockPort`, `IdGeneratorPort`, `DomainEvent` |
| `algoprep-identity` | `com.algoprep.identity` | `identity` | **Có class thật** — F1, gom thành 10 nhóm tính năng |
| `algoprep-problem-bank` | `com.algoprep.problembank` | `problem` | **Có class thật** — F2, gom thành 11 nhóm tính năng |
| `algoprep-harness` | `com.algoprep.harness` | `harness` | **Có class thật** — F3 |
| `algoprep-judge` | `com.algoprep.judge` | `judge` | **Có class thật** — F4, gom thành 10 nhóm tính năng |
| `algoprep-ai-review` | `com.algoprep.aireview` | `ai` | **Có class thật** — F5, gom thành 9 nhóm tính năng |
| `algoprep-interview-bank` | `com.algoprep.interviewbank` | `interview_bank` | **Có class thật** — F6, `domain/model` gom thành 4 nhóm |
| `algoprep-bootstrap` | `com.algoprep.bootstrap` | (không) | **Có class thật** — khởi chạy, bảo mật, xử lý lỗi, bộ test ranh giới |

Package Java **không có dấu gạch** (`problembank`) dù tên module có (`algoprep-problem-bank`) — Java
không cho dấu gạch trong tên package. Tên schema **ngắn hơn** tên context ở ba chỗ
(`problem-bank` → `problem`, `judge-orchestration` → `judge`, `ai-review` → `ai`). Bảng ánh xạ đầy
đủ: `.nexa/domain-registry.json`; luật chống trôi: `01-rd/system/SYS0201_codebase_structure.md` mục 3.

## Quy ước gom nhóm package (sửa 2026-11-15)

Khi một thư mục trong tầng (`domain/model`, `application/command`, `application/ports/in`, `application/dto`, `domain/exception`) vượt khoảng 30 file thì tách thành package con theo nhóm tính năng đặt tên theo khái niệm nghiệp vụ. Cùng một tên nhóm dùng ở mọi tầng; kiểu dùng chung nhiều nhóm ở lại package cha; không giữ file `package-info.java`. Ví dụ đầy đủ: `02-bd/architecture/F5_ai-review.md` mục 1.1; quy ước chung: `01-rd/system/SYS0101_backend_architecture.md` mục 5.3.C.

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

**`./mvnw verify` cần Docker đang chạy.** Test tích hợp khởi tạo PostgreSQL, Redis và Mailpit riêng
bằng Testcontainers; không dùng dữ liệu trong Compose local. Profile `test` vẫn tắt kết nối hạ tầng
mặc định cho test chỉ nạp context; test tích hợp bật lại đúng thành phần cần dùng.

Lát F1 6b dùng Redis trong Compose và SMTP local qua Mailpit. Chạy `docker compose up -d redis mailpit`
từ `05-coding/`; hộp thư ở `http://localhost:8025`, SMTP ở `localhost:1025`.
Profile `local` nhận `ALGOPREP_SMTP_HOST`, `ALGOPREP_SMTP_PORT`, `ALGOPREP_MAIL_FROM`;
đổi cổng Compose thì đổi biến backend tương ứng trong `.env` (xem `.env.example`).

Lần chạy đầu trên DB chưa có ADMIN cần thêm `ALGOPREP_BOOTSTRAP_ADMIN_EMAIL` và
`ALGOPREP_BOOTSTRAP_ADMIN_PASSWORD` (DD `03-dd/jobs/F1_identity.md` mục 1); thiếu một biến thì
ứng dụng dừng khởi động. Đã có ADMIN thì hai biến bị bỏ qua.
Mailpit giữ thư trong bộ nhớ và không gửi thư ra ngoài. Lát 6c thêm ba endpoint
`POST /api/v1/identity/auth/register`, `/email/verify`, `/email/verify/resend`.
Đăng ký tạo STUDENT chờ xác thực; OTP 6 số được gửi vào Mailpit, có hạn 10 phút,
sai tối đa 5 lần và gửi lại sau 60 giây. Xác thực không tự đăng nhập.
Redis giữ digest của mã và bộ đếm sai. OTP bị xoá sau khi DB commit; lỗi Redis khi kiểm mã
không cho kích hoạt tài khoản. Lỗi SMTP khi đăng ký rollback tài khoản để người dùng thử lại.
Mật khẩu tối thiểu 8 ký tự, có chữ và số; tối đa **72 byte UTF-8**, vượt trả `IDT-V02`,
theo lựa chọn giữ BCrypt của chủ dự án (`DEC-2026-1030-bcrypt-password-byte-limit`). Không cắt mật khẩu.
Login với đầu vào vượt giới hạn trả `IDT-102`. Frontend mock còn dùng quy tắc cũ; đồng bộ ở task #7.

Lát 6d thêm `POST /api/v1/identity/auth/password/forgot`, `/verify-otp` và `/reset`.
Forgot trả câu chung cho email có/không có tài khoản và khi đang cooldown. OTP 10 phút,
tối đa 5 lần sai, gửi lại sau 60 giây; verify đúng giữ mã cho bước reset.
Reset đổi mật khẩu và thu hồi mọi refresh token trong cùng giao dịch ghi audit, không tự đăng nhập.
Thời điểm phát mã và đổi mật khẩu dùng đồng hồ PostgreSQL để chặn replay ngay cả khi dọn Redis lỗi.
OAuth-only nhận mail hướng dẫn đăng nhập lại bằng provider, không được phát OTP reset.
Pending tự đăng ký vẫn phải hoàn thành OTP xác thực email; reset không khôi phục tài khoản deactivated.

Lát 6e thêm `POST /api/v1/identity/auth/deactivation/restore` nhận `{ restoreTicket }`.
Đăng nhập đúng mật khẩu vào tài khoản tự xoá còn trong hạn 30 ngày, chưa ẩn danh sẽ nhận
`403 IDT-104` với `details.restoreTicket` và `expiresInSeconds=300`; không có token/cookie đăng nhập.
Vé chỉ dùng cho đúng lần tự xoá. Khôi phục trả `{ restored: true }`, rồi người dùng đăng nhập lại.
Admin khoá, quá hạn hoặc đã ẩn danh không được cấp vé; vé sai/hết hạn/đã dùng trả `401 IDT-114`.
Redis giữ hash vé, DB khoá dòng user và kiểm lại thời hạn. Lỗi DB giữ vé để thử lại;
lỗi dọn Redis sau commit không cho phép dùng vé lần hai. OAuth callback sẽ nối cơ chế này ở task #15.

BR-01 giới hạn theo email đã trim/chuyển chữ thường và IP kết nối (`getRemoteAddr`), dùng chung
cho login thường và login giảng viên. Header `X-Forwarded-For` từ client không được tin cậy.
Khi triển khai sau reverse proxy cần cấu hình proxy tin cậy trước khi dùng IP chuyển tiếp.
Counter hết hạn 15 phút từ lần sai đầu, đúng thuật toán `INCR`/`EXPIRE` trong DD BR-01 và BD database;
DD vẫn gọi đây là “cửa sổ trượt”, cần thống nhất thuật ngữ khi rà tài liệu.

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

## JaCoCo: ngưỡng 90%, chặn ở mọi module (cập nhật 2026-11-15)

POM cha đặt `jacoco.line.minimum` = **0,90** và `jacoco.haltOnFailure` = `true`, nên `mvn verify` đỏ
khi coverage dòng của **bất kỳ** module nào dưới 90%. Lộ trình cũ (ngưỡng 80%, `haltOnFailure=false`
và bật chặn theo từng module, `DEC-2026-0901-backend-base-architecture` điểm 3) đã kết thúc: cả tám
module có mã nghiệp vụ.

Báo cáo JaCoCo **không còn loại trừ nào**: `infrastructure`, `presentation`, `config` và các lớp
`*Application` được đo như phần còn lại, nên con số khớp với con số SonarQube báo (kế hoạch
`06-plan/261114-1645-sonarqube-clear-coverage-90.md`). Trước đó POM loại các tầng này khỏi bundle,
khiến Sonar vẫn tính chúng là chưa phủ và hiển thị 60,1% trong khi phần được đo đạt 95,1%.

Hệ quả khi chạy lệnh: `mvn install -DskipTests` hay `package -DskipTests` sẽ đỏ ở cổng JaCoCo vì
không có dữ liệu coverage; thêm `-Djacoco.skip=true` khi chỉ cần dựng jar không chạy test.

## Còn thiếu (không dựng ở bước base, có chủ đích)

- **JWT và RBAC ba vai trò** — `SecurityConfig` mở health cơ bản; `/actuator/prometheus` yêu cầu dedicated monitoring bearer (không dùng JWT người dùng),
  chặn mọi thứ còn lại. Cần `03-dd/api/F1_identity.md`.
- **Danh mục mã lỗi** — `GlobalExceptionHandler` ánh xạ theo ba loại ngoại lệ nền, chưa theo danh
  mục. Cần `03-dd/api/api.md`.
- **Rate limiting Token Bucket trên Redis** cho ba nhóm endpoint (đăng nhập/đăng ký, nộp bài, gọi
  AI) — `01-rd/req/nfr.md` mục D chốt yêu cầu, nhưng cần đường dẫn endpoint thật mới cấu hình được
  filter theo nhóm.
- **CORS** — frontend Next.js khác origin nên sẽ cần, nhưng danh sách origin theo môi trường là một
  quyết định, không phải giá trị đoán được.
- **Entity JPA, migration** — bảng/cột/khoá/index thuộc `02-bd/database/<module>.md`. Công cụ
  migration đã chốt là **Flyway** (`DEC-2026-1029-flyway-migration-tool`), đã nối ở `algoprep-bootstrap`; chưa có file migration nào.
- **`GoJudgeAdapter`** — thuộc slice `judge-orchestration`. Hạ tầng go-judge đã chạy và đã kiểm chạy
  được mã thật; xem `05-coding/judge-engine/README.md`.
- **Pipeline GitHub Actions** — `01-rd/system/SYS0301_environment.md` mục 6 hẹn "khi có mã thật để chạy CI".
  Giờ đã có mã thật.

## Nợ kỹ thuật đã biết

| # | Nợ | Gỡ khi |
| :-: | :--- | :--- |
| 1 | `archunit.properties` đặt `archRule.failOnEmptyShould=false`. Cần thiết vì 5 module chưa có class nào nên phần lớn luật khớp rỗng một cách hợp lệ. **Để nguyên `false` vĩnh viễn nghĩa là một luật viết sai tên package sẽ im lặng mãi mãi** — đúng kiểu hỏng mà bộ test này tồn tại để chặn | Cả sáu module có class nghiệp vụ → đổi thành `true` |
| 2 | `application-test.yml` loại trừ autoconfigure của DataSource/JPA/Redis/RabbitMQ để `verify` không cần Docker | Module đầu tiên có Entity JPA thật → chuyển sang Testcontainers, xoá dần từng dòng loại trừ |
| 3 | `/actuator/prometheus` dùng dedicated monitoring bearer; health chỉ lộ trạng thái cơ bản | Cấp `ALGOPREP_MONITORING_TOKEN` trùng file bí mật của Prometheus; cổng Prometheus/Grafana local chỉ bind 127.0.0.1 |
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

## Lịch sử sửa đổi

Bảng 1: Lịch sử sửa đổi README backend.

| Ngày | Nội dung | Căn cứ |
| :--- | :--- | :--- |
| 2026-09-01 | Bản đầu: khung base, bộ cổng chất lượng, hạ tầng local | `06-plan/nexa-plan/260831-2323-backend-base-scaffold.md` |
| 2026-11-15 | Thêm quy ước gom nhóm package; cập nhật mục Trạng thái và bảng module (cả tám module đã có mã thật); viết lại mục JaCoCo (ngưỡng 0,90, chặn ở mọi module, không còn loại trừ); không còn `package-info.java`. Các mục "Còn thiếu" và "Nợ kỹ thuật đã biết" chưa rà lại, có thể còn mô tả cũ | `06-plan/261114-1645-sonarqube-clear-coverage-90.md` |

### Cấu hình monitoring sau bản vá 15/11/2026

Backend đọc `ALGOPREP_MONITORING_TOKEN`; Prometheus đọc `05-coding/monitoring/secrets/backend-metrics-token` qua mount chỉ đọc và `authorization.credentials_file`. Hai giá trị phải giống nhau. Thư mục secrets và `.env` được gitignore; không đưa token vào report/repository. Thiếu cấu hình sẽ từ chối metrics thay vì mở công khai. Đợt này đã cấp token local và kiểm job scrape UP trên backend cô lập. Khi xoay token, cập nhật cả hai đầu và reload/restart đúng dịch vụ liên quan.

Logout thu hồi ngay family theo `sid` qua migration identity V1.005; socket trên instance hiện tại đóng sau commit. Xem [hợp đồng F1](../../03-dd/api/F1_identity.md). Cụm nhiều backend cần phát sự kiện thu hồi liên instance để đóng ngay cả socket đang im lặng trên instance khác.
