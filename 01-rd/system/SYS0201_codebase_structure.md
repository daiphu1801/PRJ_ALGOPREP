# RD hệ thống — Cấu Trúc Codebase (Codebase Structure) / `SYS0201`

> Mã tài liệu: `SYS0201`
> Tên file chuẩn: `SYS0201_codebase_structure.md`
> Loại tài liệu: RD hệ thống — `codebase_structure`
> Phạm vi: kiến trúc, cấu trúc, môi trường hoặc công cụ nền của AlgoPrep; không phải RD theo màn hình.
> Nguồn sự thật (SoT): `README.md`; `DEC-2026-0819-agentconfig-retarget-algoprep`.
> Tài liệu này có thể đọc độc lập.

---

## 1. Mục đích và bối cảnh

Cấu trúc thư mục của repo AlgoPrep: trục tài liệu, trục mã nguồn, và quy ước đặt tên. Phân tầng bên trong một module backend nằm ở `SYS0101_backend_architecture.md` mục 3; phân tầng frontend ở `SYS0102_frontend_architecture.md` mục 2. Tài liệu này chỉ nói về **cây thư mục**.

> **Trạng thái 2026-09-01:** cả `05-coding/frontend/` (2026-08-25) và `05-coding/backend/` (2026-09-01) đã có khung base — xem `05-coding/frontend/README.md` và `05-coding/backend/README.md`. Hạ tầng local (`docker-compose.yml`, `database/init/`, `.env.example`, `judge-engine/`, `monitoring/`) đã chạy được. Từ đây **SoT của cấu trúc là CODE** — hai bên lệch thì code thắng, nhưng phải sửa tài liệu ngay để không trôi tiếp.

---

## 2. Phạm vi

| Màn/tài liệu | Vai trò | Bounded Context |
|---|---|---|
| Tài liệu hệ thống hiện tại | `codebase_structure` | Nền tảng dùng chung |
| Tài liệu nền | `README.md`; `DEC-2026-0819-agentconfig-retarget-algoprep` | Căn cứ và liên kết liên quan |

### 2.1 Cấu trúc hệ thống liên quan

| Màn/tài liệu | Vai trò | Bounded Context |
|---|---|---|
| Tài liệu này | Đặc tả hệ thống ở mức `system` | Nền tảng dùng chung |
| RD theo module/màn hình | Cung cấp yêu cầu nghiệp vụ chi tiết | Các Bounded Context tương ứng |
| BD/DD/TDD | Thiết kế, hợp đồng và kiểm thử tiếp nối | Theo phạm vi từng tài liệu |

---

## 3. Ngoài phạm vi (Out of Scope)

Không có mục ngoài phạm vi riêng; các giới hạn được nêu trong các tiểu mục đặc tả bên dưới.

---

## 4. Tiền đề và ràng buộc

Các quyết định kiến trúc, phiên bản công nghệ, môi trường và giới hạn được giữ nguyên theo nội dung đặc tả bên dưới. Những điểm chưa được chốt không được tự suy luận sang BD, DD hoặc mã nguồn.

---

## 5. Đặc tả hệ thống (REQ)

### 5.1 Repo là MỘT monorepo

Chốt bởi `DEC-2026-0819-agentconfig-retarget-algoprep`: tài liệu và mã nguồn nằm **cùng một repository**, `05-coding/` **không phải submodule**.

```text
prj_algoprep/
├── .claude/                # Cấu hình agent: skills, agents, commands, rules, hooks
├── .nexa/                  # SoT điều phối: control/, config.yaml, domain-registry.json
├── 01-rd/                  # Requirements: overview · req · system · screens
├── 02-bd/                  # Basic Design: architecture · database · security · storage · screens
├── 03-dd/                  # Detail Design: api · logic · validation · jobs · screens
├── 04-tdd/                 # Tiêu chí nghiệm thu AC-nn (phẳng, tên file mang trục)
├── 05-coding/              # TOÀN BỘ mã nguồn và hạ tầng local
├── 06-plan/                # Kế hoạch sống + reports/ của subagent
├── 07-review/              # Bản ghi review RD/BD/DD và review code
├── 09-layoutBase/          # Prototype giao diện tĩnh (.dc.html) làm đầu vào cho BD/DD
├── README.md               # Báo cáo khảo sát hệ thống — nguồn của phạm vi
└── CLAUDE.md               # Hướng dẫn agent
```

**Vì sao monorepo.** Đây là đồ án tốt nghiệp một người làm. Tách submodule thêm chi phí đồng bộ hai lịch sử commit mà không đổi lại gì; và tài liệu với mã nguồn ở đây thay đổi cùng nhau theo từng slice.

**Không có `01-legacy/`** — dự án greenfield, không có hệ cũ để đối chiếu.
**`09-layoutBase/`** — chứa 31 màn prototype tĩnh (`.dc.html`) làm căn cứ đối chiếu và đầu vào để dựng màn BD/DD.
**Không có `plans/`** — mọi kế hoạch vào `06-plan/{YYMMDD-HHMM}-{slug}.md` (`CLAUDE.md` mục Rules).

---

### 5.2 Bên trong `05-coding/`

```text
05-coding/
├── backend/                    # Maven multi-module, Java 21 + Spring Boot 4
│   ├── pom.xml                 #   POM cha, khai 8 module
│   ├── mvnw · mvnw.cmd
│   ├── algoprep-common/
│   ├── algoprep-identity/
│   ├── algoprep-problem-bank/
│   ├── algoprep-harness/
│   ├── algoprep-judge/
│   ├── algoprep-ai-review/
│   ├── algoprep-interview-bank/
│   ├── algoprep-bootstrap/     #   @SpringBootApplication + test kiểm ranh giới module
│   └── config/                 #   checkstyle.xml, spotbugs-exclude.xml
│
├── frontend/                   # Next.js 16, TypeScript strict, FSD
│   ├── src/                    #   app · views · widgets · features · entities · shared
│   ├── e2e/                    #   Playwright
│   ├── package.json
│   └── next.config.ts
│
├── coding-packs/               # Artifact điều phối của skill nexa (TIP, task graph)
├── database/init/              # Script khởi tạo schema, mount vào entrypoint của Postgres
├── judge-engine/                # Cấu hình go-judge self-hosted (mặc định); có thể thêm cấu hình adapter khác
├── scripts/
├── docker-compose.yml          # Hạ tầng local — NẰM TRONG 05-coding, không ở root
├── .env.example
├── package.json                # pnpm workspace
└── README.md
```

**Hạ tầng local nằm trong `05-coding/`, không ở root repo.** Lý do: mọi thứ cần để *chạy* hệ thống nằm cạnh mã nguồn của hệ thống; root repo là nơi của tài liệu và cấu hình agent. Đặt `docker-compose.yml` ở root thì người mở `05-coding/` không thấy cách chạy, còn schema thì bị tách khỏi mã dùng nó.

**Tám module Maven, không phải sáu.** Sáu module nghiệp vụ theo `.nexa/domain-registry.json`, cộng `algoprep-common` và `algoprep-bootstrap` — vai trò từng module ở `SYS0101_backend_architecture.md` mục 2.

**Dựng vừa đúng lúc.** Module chỉ được tạo khi tới slice của nó (`CLAUDE.md` mục Process). Một module rỗng trong `pom.xml` là nợ, không phải tiến độ.

---

### 5.3 Quy ước đặt tên, và chỗ chúng phải khớp nhau

Đây là cơ chế chống trôi: cùng một khái niệm phải tra được bằng một lần `grep` xuyên cả bốn tầng tài liệu và mã.

| Khái niệm | Tài liệu | Backend | Frontend | Database |
| :--- | :--- | :--- | :--- | :--- |
| Bounded Context | `code` trong `domain-registry.json` (`problem-bank`) | Module Maven `algoprep-problem-bank`, package `com.algoprep.problembank` | (không tương ứng) | Schema `problem` |
| Màn hình | slug ở `01-rd/screens/<khu vực>/<slug>.md` (`users/problem_detail.md`) | (không tương ứng) | Slice `views/users/problem-detail/` | (không tương ứng) |
| Thực thể nghiệp vụ | Dòng trong `glossary.md` | Class trong `domain/model/` | Slice `entities/<name>/` | Bảng |

Ba lưu ý về dấu nối, vì chúng khác nhau và đây là chỗ hay sai:

* **Bounded Context** viết `kebab-case` trong tài liệu và trong tên module Maven, nhưng package Java **không có dấu** (`com.algoprep.problembank`) — Java không cho dấu gạch trong tên package.
* **Slug màn hình** viết `snake_case` trong tài liệu (`problem_detail`) nhưng slice frontend viết `kebab-case` (`problem-detail`) theo convention thư mục của FSD. Cùng một màn, hai cách viết, đổi `_` thành `-`.
* **Tên schema** ngắn hơn tên context ở ba chỗ: `problem-bank` → `problem`, `judge-orchestration` → `judge`, `ai-review` → `ai`. Bảng ánh xạ đầy đủ ở `.nexa/domain-registry.json`.

---

### 5.4 Tài liệu nào đi với mã nào

Hai trục tài liệu (`CLAUDE.md` mục Process), và chúng không được trộn:

| Trục | Đơn vị | Tài liệu | Mã nguồn |
| :--- | :--- | :--- | :--- |
| Bounded Context | sáu context | `02-bd/{architecture,database,security,storage}/<context>.md` · `03-dd/{api,logic,validation,jobs}/<context>.md` | `backend/algoprep-<context>/` |
| Màn hình | từng màn | `01-rd/screens/<khu vực>/<slug>.md` · `02-bd/screens/<khu vực>/<slug>.md` · `03-dd/screens/<khu vực>/<slug>.md` — khu vực là `shared`/`users`/`teacher`/`admin` theo actor chính của màn | `frontend/src/views/<slug>/` |

**Vì sao hai trục.** Một màn thường chạm nhiều context: màn chi tiết bài toán cần `problem-bank` + `harness` + `judge-orchestration` + `ai-review`. Không xếp được nó vào một context nào, nên nó có trục riêng.

**Luật chống trôi:** hợp đồng API định nghĩa ở **đúng một chỗ** (`03-dd/api/<context>.md`); `screens/` chỉ liệt kê và liên kết tới. Không có logic backend trong `screens/`.

---

### 5.5 Việc còn phải làm

| Việc | Trạng thái |
| :--- | :--- |
| Tạo `05-coding/backend/pom.xml` và đủ 8 module Maven | **Đã có** (2026-09-01) — `DEC-2026-0901-backend-base-architecture`. Khai đủ 8 module ngay, lệch có ghi nhận với luật "dựng vừa đúng lúc" ở mục 2 của tài liệu này; lý do trong quyết định đó |
| Tạo `05-coding/frontend/` (Next.js 16, TypeScript strict, cấu hình ESLint boundaries) | **Đã có khung base** (2026-08-25) — nội dung nghiệp vụ từng màn chờ `02-bd/screens/` |
| Tạo `05-coding/docker-compose.yml`, `database/init/`, `.env.example`, cấu hình `judge-engine/` | **Đã có** (2026-09-01) — 5 service core `healthy`, 6 schema tạo đúng, go-judge đã kiểm chạy được mã thật. Thêm `monitoring/prometheus.yml` (profile `observability`) |
| ~~Kiểm chứng ràng buộc cgroup v1 của Judge0~~ | **Đã giải quyết** bằng `DEC-2026-0823-go-judge-default-engine` — engine mặc định đổi sang go-judge, hỗ trợ cả cgroup v1/v2, không còn là rủi ro số một |
| Chạy `gitnexus analyze` lần đầu cho backend | **Đã chạy** (2026-09-01), sau khi backend có module thật |

---

## 6. Yêu cầu dữ liệu

### 6.1 Dữ liệu tham chiếu

Chi tiết bảng, cột, kiểu dữ liệu và migration thuộc BD/DD; tài liệu hệ thống này chỉ mô tả dữ liệu ở mức kiến trúc khi nội dung gốc có nêu.

### 6.2 Luồng dữ liệu

Các luồng đọc/ghi, cổng vào/cổng ra, realtime hoặc công cụ ngoài được giữ nguyên trong các tiểu mục chương 5; hợp đồng API chi tiết thuộc DD.

---

## 7. RACI và danh sách bàn giao

| Bàn giao | Thực hiện | Rà soát | Trạng thái |
|---|---|---|---|
| RD hệ thống `codebase_structure` | Nhóm phát triển | Chủ dự án | Có tài liệu này |
| Cấu trúc thư mục mã nguồn / module | Nhóm phát triển | Chưa xác định — không tự suy luận | Đã có khung base |
| Quy ước đặt tên và đồng bộ trục tài liệu | Nhóm phát triển | Chưa xác định — không tự suy luận | Đang áp dụng |

---

## 8. Kế hoạch và quy mô

| Chỉ số | Giá trị |
|---|---|
| Phạm vi | Tài liệu hệ thống `codebase_structure` |
| Lịch, nhân sự, công sức | Chưa xác định — không tự suy luận |

---

## 9. Tham chiếu

| Loại | Tham chiếu |
|---|---|
| Nguồn SoT | `README.md`; `DEC-2026-0819-agentconfig-retarget-algoprep` |
| Quy chuẩn | `01-rd/RD_LAYOUT_RULES.md` |
| Tài liệu liên quan | `01-rd/system/SYS0101_backend_architecture.md`; `01-rd/system/SYS0102_frontend_architecture.md`; `.nexa/domain-registry.json` |
| Tài liệu hiện tại | `01-rd/system/SYS0201_codebase_structure.md` |
