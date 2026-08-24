# Cấu Trúc Codebase (Codebase Structure)

Cấu trúc thư mục của repo AlgoPrep: trục tài liệu, trục mã nguồn, và quy ước đặt tên. Phân tầng bên trong
một module backend nằm ở `backend_architecture.md` mục 3; phân tầng frontend ở `frontend_architecture.md`
mục 2. Tài liệu này chỉ nói về **cây thư mục**.

> **Trạng thái 2026-08-20:** `05-coding/` còn rỗng (chỉ có `.gitkeep`). Cây thư mục dưới đây là **đích phải
> dựng**, không phải mô tả hiện trạng. Khi mã đã tồn tại thì **SoT của cấu trúc là CODE** — hai bên lệch thì
> code thắng, nhưng phải sửa tài liệu ngay để không trôi tiếp.

---

## 1. Repo là MỘT monorepo

Chốt bởi `DEC-2026-0819-agentconfig-retarget-algoprep`: tài liệu và mã nguồn nằm **cùng một repository**,
`05-coding/` **không phải submodule**.

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
├── README.md               # Báo cáo khảo sát hệ thống — nguồn của phạm vi
└── CLAUDE.md               # Hướng dẫn agent
```

**Vì sao monorepo.** Đây là đồ án tốt nghiệp một người làm. Tách submodule thêm chi phí đồng bộ hai lịch sử
commit mà không đổi lại gì; và tài liệu với mã nguồn ở đây thay đổi cùng nhau theo từng slice.

**Không có `01-legacy/`** — dự án greenfield, không có hệ cũ để đối chiếu.
**Không có `09-layoutBase/`** — chưa chọn hệ thống thiết kế nào.
**Không có `plans/`** — mọi kế hoạch vào `06-plan/{YYMMDD-HHMM}-{slug}.md` (`CLAUDE.md` mục Rules).

---

## 2. Bên trong `05-coding/`

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

**Hạ tầng local nằm trong `05-coding/`, không ở root repo.** Lý do: mọi thứ cần để *chạy* hệ thống nằm cạnh
mã nguồn của hệ thống; root repo là nơi của tài liệu và cấu hình agent. Đặt `docker-compose.yml` ở root thì
người mở `05-coding/` không thấy cách chạy, còn schema thì bị tách khỏi mã dùng nó.

**Tám module Maven, không phải sáu.** Sáu module nghiệp vụ theo `.nexa/domain-registry.json`, cộng
`algoprep-common` và `algoprep-bootstrap` — vai trò từng module ở `backend_architecture.md` mục 2.

**Dựng vừa đúng lúc.** Module chỉ được tạo khi tới slice của nó (`CLAUDE.md` mục Process). Một module rỗng
trong `pom.xml` là nợ, không phải tiến độ.

---

## 3. Quy ước đặt tên, và chỗ chúng phải khớp nhau

Đây là cơ chế chống trôi: cùng một khái niệm phải tra được bằng một lần `grep` xuyên cả bốn tầng tài liệu và mã.

| Khái niệm | Tài liệu | Backend | Frontend | Database |
| :--- | :--- | :--- | :--- | :--- |
| Bounded Context | `code` trong `domain-registry.json` (`problem-bank`) | Module Maven `algoprep-problem-bank`, package `com.algoprep.problembank` | (không tương ứng) | Schema `problem` |
| Màn hình | slug ở `01-rd/screens/<slug>.md` (`problem_detail`) | (không tương ứng) | Slice `views/problem-detail/` | (không tương ứng) |
| Thực thể nghiệp vụ | Dòng trong `glossary.md` | Class trong `domain/model/` | Slice `entities/<name>/` | Bảng |

Ba lưu ý về dấu nối, vì chúng khác nhau và đây là chỗ hay sai:

* **Bounded Context** viết `kebab-case` trong tài liệu và trong tên module Maven, nhưng package Java **không
  có dấu** (`com.algoprep.problembank`) — Java không cho dấu gạch trong tên package.
* **Slug màn hình** viết `snake_case` trong tài liệu (`problem_detail`) nhưng slice frontend viết
  `kebab-case` (`problem-detail`) theo convention thư mục của FSD. Cùng một màn, hai cách viết, đổi `_`
  thành `-`.
* **Tên schema** ngắn hơn tên context ở ba chỗ: `problem-bank` → `problem`, `judge-orchestration` → `judge`,
  `ai-review` → `ai`. Bảng ánh xạ đầy đủ ở `.nexa/domain-registry.json`.

---

## 4. Tài liệu nào đi với mã nào

Hai trục tài liệu (`CLAUDE.md` mục Process), và chúng không được trộn:

| Trục | Đơn vị | Tài liệu | Mã nguồn |
| :--- | :--- | :--- | :--- |
| Bounded Context | sáu context | `02-bd/{architecture,database,security,storage}/<context>.md` · `03-dd/{api,logic,validation,jobs}/<context>.md` | `backend/algoprep-<context>/` |
| Màn hình | từng màn | `01-rd/screens/<slug>.md` · `02-bd/screens/<slug>.md` · `03-dd/screens/<slug>.md` | `frontend/src/views/<slug>/` |

**Vì sao hai trục.** Một màn thường chạm nhiều context: màn chi tiết bài toán cần `problem-bank` +
`harness` + `judge-orchestration` + `ai-review`. Không xếp được nó vào một context nào, nên nó có trục riêng.

**Luật chống trôi:** hợp đồng API định nghĩa ở **đúng một chỗ** (`03-dd/api/<context>.md`); `screens/` chỉ
liệt kê và liên kết tới. Không có logic backend trong `screens/`.

---

## 5. Việc còn phải làm

| Việc | Trạng thái |
| :--- | :--- |
| Tạo `05-coding/backend/pom.xml` và module `algoprep-common` + `algoprep-bootstrap` | Chưa có |
| Tạo `05-coding/frontend/` (Next.js 16, TypeScript strict, cấu hình ESLint boundaries) | Chưa có |
| Tạo `05-coding/docker-compose.yml`, `database/init/`, `.env.example`, cấu hình `judge-engine/` | Chưa có |
| ~~Kiểm chứng ràng buộc cgroup v1 của Judge0~~ | **Đã giải quyết** bằng `DEC-2026-0823-go-judge-default-engine` — engine mặc định đổi sang go-judge, hỗ trợ cả cgroup v1/v2, không còn là rủi ro số một |
| Chạy `npx gitnexus analyze` lần đầu | Chỉ làm sau khi backend đã có module thật (`CLAUDE.md` mục GitNexus) |
