# Decision Registry — AlgoPrep

> Where settled project decisions are recorded. Read the `ACTIVE` section before touching module scope, API contracts, DB schema, authentication, or core libraries.
> Companion rule: [`sot-precedence.md`](sot-precedence.md). **This is a soft P1 tier** — a newer decision with a better reason may replace an older one, as long as it is written down.
> Updated: 2026-08-31.

---

## Machine-readable

```yaml
version: 1
project: algoprep
updated: 2026-10-27
decisions:
  - id: DEC-2026-0819-agentconfig-retarget-algoprep
    kind: process-policy
    status: ACTIVE
    date: 2026-08-19
    subject: "Agent config retargeted to AlgoPrep; monorepo; taxonomy 01-rd to 07-review; plans go to 06-plan"
    decision: >
      The whole agent configuration (`.claude/`, `.nexa/`, `CLAUDE.md`) previously belonged to the
      NestGame v2 project and is now retargeted to AlgoPrep — an algorithm-practice and technical
      interview platform with AI integration (self-hosted Judge0 + Spring AI). Four locked points:
      (1) MONOREPO — code lives in `05-coding/` inside this same repo, NOT a submodule;
      (2) TAXONOMY — `01-rd` / `02-bd` / `03-dd` / `04-tdd` / `05-coding` / `06-plan` / `07-review`,
      with no `01-legacy` and no `09-layoutBase`;
      (3) PLANS — every plan is written to `06-plan/{YYMMDD-HHMM}-{slug}.md`; the `plans/` directory
      is dropped entirely;
      (4) SoT remains a SOFT ladder as in the previous project: diverging from a document is valid
      provided the reason is stated.
    rationale: >
      The config was copied verbatim from NestGame, so every skill pointed at the wrong paths and
      described the wrong domain. Worse: all seven hooks in `.claude/settings.local.json` invoked
      scripts through absolute paths under `d:/prj_nestgameV2/.claude/hooks/`, and the hooks resolve
      the project root via `path.join(__dirname, '..', '..')` — so every working session in this repo
      was injected with roughly forty decisions about a retro-game platform. Monorepo was chosen
      because this is a one-person capstone project; splitting a submodule would add synchronisation
      cost for no benefit.
    source: "06-plan/260819-1811-retarget-agentconfig-algoprep.md"
    impacted_paths:
      - .claude/
      - .nexa/
      - CLAUDE.md

  - id: DEC-2026-0819-language-config-en-docs-vi
    kind: process-policy
    status: ACTIVE
    date: 2026-08-19
    subject: "Agent config is written in English; deliverable documents stay Vietnamese; skill templates stay Vietnamese"
    decision: >
      Two separate language rules. (1) AGENT CONFIG IN ENGLISH: `CLAUDE.md`, `.claude/skills/**`,
      `.claude/agents/**`, `.claude/commands/**`, `.claude/rules/**`, `.nexa/control/**`, comments in
      `.nexa/config.yaml`, and `.nexa/domain-registry.json`. (2) DELIVERABLE DOCS IN VIETNAMESE:
      `01-rd/`, `02-bd/`, `03-dd/`, `04-tdd/`, `06-plan/`, `07-review/`, and `README.md`; chat replies
      to the owner are Vietnamese too.
      TWO EXCEPTIONS inside a skill file, both functional rather than stylistic, so skills are
      deliberately bilingual and must not be normalised to one language:
      (a) Template blocks and sample headings that land verbatim in a generated document REMAIN
      VIETNAMESE — `tdd-criteria-template.md` keeps "Tiền điều kiện / Hành động / Kết quả mong đợi",
      `rri-report.md` keeps its report template, `questions-by-type.md` keeps the question text
      because that text is asked to the owner verbatim, and the diagram labels inside the UML
      skills stay Vietnamese because they land in the printed report.
      (b) Trigger phrases in the `description` frontmatter REMAIN VIETNAMESE. They are what the
      owner types ("vẽ use case đi", "viết BD", "sửa cho pass"); translated, the skill stops
      firing. That is a matching requirement, not prose.
    rationale: >
      Skills are instructions for a model, where English is terser and less ambiguous. Documents are
      submitted to a capstone review board and must be in Vietnamese. Translating the template blocks
      would scatter English headings through otherwise Vietnamese content in `01-rd` to `04-tdd`,
      which is worse than accepting bilingual skill files.
    source: "Owner instruction, session 2026-08-19"
    impacted_paths:
      - .claude/
      - .nexa/
      - CLAUDE.md

  - id: DEC-2026-0819-gitnexus-hard-rules-after-index
    kind: process-policy
    status: ACTIVE
    date: 2026-08-19
    subject: "GitNexus MUST/NEVER rules are hard, but only take effect after the first index"
    decision: >
      The GitNexus rules in `CLAUDE.md` (run `impact` before editing a symbol, run `detect_changes`
      before committing, never rename by find-and-replace, never ignore a HIGH/CRITICAL risk result)
      are HARD rules, deliberately outside the soft SoT ladder. The ladder governs which source of
      truth wins when two disagree; these govern which step you must take. Different category, so
      "the ladder is soft" is not a valid reason to skip them.
      HOWEVER they are dormant in this repo: `.gitnexus/` does not exist and `05-coding/` is empty,
      so there is nothing to query. They activate at the first `npx gitnexus analyze` run. Until
      then, a skill must say the step was unavailable rather than reporting a clean graph it never
      consulted.
    rationale: >
      Carried forward from the previous project, where the same reasoning was recorded. The
      distinction is worth keeping because agents otherwise treat every rule as negotiable once they
      learn the SoT ladder is soft. The "dormant until indexed" half is new and specific to AlgoPrep:
      without it, six skills instruct a step that cannot run and the agent either fabricates a result
      or stalls.
    source: ".claude/skills/_shared/gitnexus-contract.md · CLAUDE.md (GitNexus section)"
    impacted_paths:
      - .claude/skills/_shared/gitnexus-contract.md
      - .claude/skills/gitnexus/
      - CLAUDE.md

  - id: DEC-2026-0819-zflow-model-tiers-claude5
    kind: tech-choice
    status: ACTIVE
    date: 2026-08-19
    subject: "zflow model tiers: the Claude 5 family is TIER0/TIER1; haiku stays TIER2"
    decision: >
      `.claude/skills/zflow/.knowledge/model-tiers.json` places `claude-opus-5` and `claude-fable-5`
      in TIER0, `claude-sonnet-5` in TIER1, and leaves `claude-haiku-4-5` in TIER2 through the
      existing `claude-haiku-4` entry. `fallback_tier` is TIER2 and `unknown_model_policy` is
      conservative, so an unrecognised model degrades rather than being trusted at TIER0.
    rationale: >
      Tier assignment decides how much of zflow's pipeline runs inline versus being delegated, so a
      wrong tier either wastes a strong model or overloads a weak one. Conservative fallback is the
      safe default: under-trusting a capable model costs tokens, over-trusting a weak one costs
      correctness. This carries forward unchanged from the previous project because it is a property
      of the models, not of the product.
    source: ".claude/skills/zflow/.knowledge/model-tiers.json"
    impacted_paths:
      - .claude/skills/zflow/.knowledge/model-tiers.json
      - .claude/skills/zflow/scripts/

  - id: DEC-2026-0819-mermaid-supersedes-md-builder
    kind: process-policy
    status: ACTIVE
    date: 2026-08-19
    subject: "mermaid-uml-architect is the only route to a Mermaid sequence diagram; nexa-md-builder is archived"
    decision: >
      `mermaid-uml-architect` replaces `nexa-md-builder`, which is archived at
      `.nexa/_archive-skills/nexa-md-builder/`. All Mermaid sequence, use-case, and activity diagrams
      go through `mermaid-uml-architect`. A sequence diagram that needs more detail than the
      overview level is still drawn there — say explicitly that the complexity ceiling is being
      exceeded and why.
      Division of labour for this project: Mermaid for diagrams embedded in Markdown; monochrome
      PlantUML via `vp-uml-architect` for anything going into the printed capstone report; coloured
      `.drawio` activity diagrams via `nexa-ad-builder` when the owner needs to edit by hand.
    rationale: >
      Two skills owning the same output format produced inconsistent diagrams in the previous
      project. Since AlgoPrep is a capstone project whose diagrams must be regenerable when the
      design changes, one owner per format matters more here than it did there.
    source: ".claude/skills/mermaid-uml-architect/SKILL.md · .nexa/_archive-skills/nexa-md-builder/"
    impacted_paths:
      - .claude/skills/mermaid-uml-architect/
      - .claude/skills/vp-uml-architect/
      - .claude/skills/nexa-ad-builder/

  - id: DEC-2026-0820-stack-versions
    kind: tech-choice
    status: ACTIVE
    date: 2026-08-20
    subject: "Pinned major versions: Java 21 LTS + Spring Boot 4.0.x on the backend, Next.js 16.x on the frontend"
    decision: >
      `README.md` section 5 names the technologies but not their versions. Pinned now, at RD level:
      BACKEND is JDK 21 LTS with Spring Boot 4.0.x (Spring Framework 7, Jakarta EE 11, Spring
      Security 7) — the 3.x line is NOT an option; build tool is Maven multi-module via `mvnw`, and
      Gradle is not an alternative. FRONTEND is Next.js 16.x on the App Router with TypeScript in
      `strict` mode and pnpm as the only package manager. Node is >= 20.9 LTS.
      Owner instruction, session 2026-08-20.
    rationale: >
      Spring Boot 4 takes JDK 17 as its floor, so 21 is a deliberate choice, not a default: Virtual
      Threads are GA in 21 (no preview flag), and record patterns plus pattern matching for `switch`
      are used directly by the F3 type schema. Virtual Threads are the load-bearing reason — every
      bottleneck in this system is blocking I/O (REST calls into Judge0, one WebSocket per grading
      submission, one SSE stream per AI interview turn), so 21 lets the code stay synchronous and
      readable instead of being rewritten reactive. Leaving versions unpinned in an RD invites a
      later session to scaffold on Spring Boot 3 and discover the Jakarta EE 11 / Security 7 break
      halfway through.
    source: "Owner instruction, session 2026-08-20 · README.md section 5"
    impacted_paths:
      - 01-rd/system/environment.md
      - 01-rd/system/backend_architecture.md
      - 01-rd/system/frontend_architecture.md
      - 05-coding/

  - id: DEC-2026-0820-architecture-baseline
    kind: architecture
    status: ACTIVE
    date: 2026-08-20
    subject: "Architecture baseline: Modular Monolith with DDD + Clean Architecture + ports in/out + command/query on the backend; FSD on the frontend"
    decision: >
      BACKEND is a Modular Monolith: one process, one PostgreSQL instance, one Maven child module and
      one schema per Bounded Context (six from `.nexa/domain-registry.json`), plus
      `algoprep-common` and `algoprep-bootstrap` — eight modules total. Inside a module, Clean
      Architecture in four layers: `domain` (aggregates, value objects, and OUTBOUND ports) /
      `application` (INBOUND ports, one interface one method, plus `command/` and `query/` and
      `dto/`) / `infrastructure` (adapters) / `presentation`. Two placements are fixed: outbound
      ports live in `domain/ports/out`, inbound ports in `application/ports/in`; there is no
      `domain/repository/` and no `application/usecase/`. Modules never import each other — they
      communicate through domain events, or through an outbound port the CALLING module declares in
      its own vocabulary. Judge0 and the LLM provider each get exactly one anti-corruption adapter.
      FRONTEND is Feature-Sliced Design with a one-way import chain
      `app -> views -> widgets -> features -> entities -> shared` and one `views` slice per screen
      slug. Owner instruction, session 2026-08-20.
    rationale: >
      Not microservices: a one-person capstone would pay distributed-systems operational cost for no
      return. Not a flat monolith either, because the two most expensive parts of the thesis need
      boundaries to exist at all — F3 (the harness generator) must be unit-testable without Judge0
      running, and F5 (the AI subsystem) must be able to fail without taking grading down, which is
      the graceful-degradation rule in `CLAUDE.md`. Command/query split is bought by the read-heavy
      contexts: a problem list with a "solved" flag joins problem data to progress data with paging
      and filters, and forcing that through an aggregate means loading every function spec and
      testcase per list row plus N+1 queries. The split is LAYOUT ONLY — shared database, shared
      storage model, no event sourcing, no prebuilt projections; a separate read store would be
      another decision. The boundary must be enforced by a build-failing test, otherwise the modular
      monolith degrades into a flat one within a few sprints and the whole rationale becomes
      decoration.
    source: "Owner instruction, session 2026-08-20 · .nexa/domain-registry.json"
    impacted_paths:
      - 01-rd/overview/overview.md
      - 01-rd/system/backend_architecture.md
      - 01-rd/system/frontend_architecture.md
      - 01-rd/system/codebase_structure.md
      - 02-bd/
      - 05-coding/

  - id: DEC-2026-0823-go-judge-default-engine
    kind: architecture
    status: ACTIVE
    date: 2026-08-23
    subject: "go-judge replaces Judge0 as the default code-execution engine; the outbound port stays engine-neutral so Judge0 can be re-plugged later"
    decision: >
      The default self-hosted code-execution engine is now **go-judge** (`github.com/criyle/go-judge`,
      image `criyle/go-judge`), not Judge0. Four points, all binding on F3/F4 documents and on the eventual
      `algoprep-judge` module:
      (1) ENGINE — go-judge's own sandbox (`go-sandbox`) supports both cgroup v1 and v2, so the local dev
      environment (WSL2 on Windows) needs no kernel-parameter forcing at all, closing risk R1 for the
      default path. go-judge is stateless (no Postgres/Redis of its own) and is the judging core behind
      Hydro, a live online-judge platform.
      (2) PORT SHAPE — `DEC-2026-0820-architecture-baseline` already requires "Judge0 ... gets exactly one
      anti-corruption adapter"; that outbound port must be named and shaped in BUSINESS vocabulary, not
      after either engine's transport. Canonical name: `JudgeExecutionPort`, with a method shaped as "run
      one testcase, return one result" — no webhook/token fields (Judge0's shape), no `cmd[]`/`copyIn`
      fields (go-judge's shape) leaking past the port. The default adapter is `GoJudgeAdapter`
      (infrastructure layer); a `Judge0Adapter` implementing the SAME port remains a valid future adapter
      if the team ever needs Judge0 again (e.g. ICPC-style batch judging) — swapping is adapter-only, no
      domain/application change, PROVIDED the port stays engine-neutral.
      (3) CALL GRANULARITY — the port is called once per testcase, never once per submission with a
      batched `cmd[]`. This preserves the existing per-testcase realtime WebSocket UX and is what makes
      point (2) possible: both a synchronous engine (go-judge, result returned in the same call) and an
      async engine (Judge0, result arriving later via webhook) can implement "run one testcase, return one
      result" without forcing either shape onto the port.
      (4) F4 CONSEQUENCES — with a synchronous default adapter, webhook authentication and callback-token
      idempotency move from PORT-LEVEL concerns to ADAPTER-LEVEL internals (only relevant if an async
      adapter like `Judge0Adapter` is active). The stuck-submission reconciliation job is kept but
      lightened: with RabbitMQ's own ack/nack and redelivery plus a synchronous call, a stuck submission is
      almost always a crashed worker, not a lost external callback, so the job becomes a timeout sweep, not
      a "re-ask the external system" job. F4's "High difficulty" rating in `system_survey.md` is kept, but
      the reason changes from "asynchronous, many states, hard to test" to "coordinating concurrent Virtual
      Thread calls into the engine plus per-testcase realtime fan-out." F3's Base64-encoding requirement is
      generalised to "package the source per the active adapter's required format" — Base64 was a
      Judge0-specific requirement, not a project-wide one.
    rationale: >
      Triggered by the Windows dev-environment pain of forcing cgroup v1 for Judge0 under WSL2 (documented
      in `01-rd/system/judge0.md`, itself renamed `judge_engine.md` by this decision): recent WSL2 releases
      actively enforce cgroup v2 for their own stability and there is no documented, reliable way back to
      v1, so risk R1 was not a one-time setup cost but an open-ended one. go-judge removes the dependency on
      either cgroup version entirely. The port-neutrality requirement (point 2) is what makes this decision
      safe to reverse: `DEC-2026-0820-architecture-baseline` already paid for ports/adapters as a pattern,
      and this decision spends that investment rather than re-opening it — if go-judge turns out to be the
      wrong call (e.g. the thesis defense wants ICPC-grade rigor that only Judge0's track record
      satisfies), reverting is an adapter swap, not a redesign. Owner instruction, session 2026-08-23, after
      a working comparison of Judge0 / Piston / go-judge on Windows dev-environment fit, architecture fit,
      and academic citability.
    source: "Owner instruction, session 2026-08-23"
    impacted_paths:
      - README.md
      - .nexa/domain-registry.json
      - 01-rd/overview/overview.md
      - 01-rd/overview/system_survey.md
      - 01-rd/overview/glossary.md
      - 01-rd/system/backend_architecture.md
      - 01-rd/system/environment.md
      - 01-rd/system/codebase_structure.md
      - 01-rd/system/judge_engine.md
      - 01-rd/req/req.md
      - 01-rd/req/user_stories.md

  - id: DEC-2026-0824-dark-light-theme
    kind: process-policy
    status: ACTIVE
    date: 2026-08-24
    subject: "AlgoPrep supports both Dark and Light theme; Light is the default"
    decision: >
      AlgoPrep ships both a Dark and a Light theme, switchable per-user from the `settings` screen; Light
      is the default for a new account. This replaces the "chưa chọn design system" open item in
      `01-rd/overview/overview.md` section 3, scoped narrowly to theme MODE only — the rest of the design
      system (color tokens, typography, component library) remains unchosen and is deferred to
      `02-bd/screens/`, per `06-plan/PROTOTYPE_DEBT.md` item 4.1.
    rationale: >
      The static prototype (`09-layoutBase/*.dc.html`) already ships a live `data-theme="dark"` attribute
      and a working toggle on nearly every screen, plus a real switch on `Cài đặt.dc.html` — the prototype
      had already made this call in practice before RD caught up. Confirming it in RD instead of leaving it
      open avoids `02-bd/screens/` re-deriving a decision the UI already committed to. Owner instruction,
      session 2026-08-24.
    source: "Owner instruction, session 2026-08-24 · 06-plan/PROTOTYPE_DEBT.md item 4.1"
    impacted_paths:
      - 01-rd/overview/overview.md
      - 01-rd/overview/system_survey.md

  - id: DEC-2026-0824-i18n-vi-en
    kind: process-policy
    status: ACTIVE
    date: 2026-08-24
    subject: "AlgoPrep UI supports Vietnamese and English (i18n); four Admin prototype screens still need the bilingual scaffold"
    decision: >
      AlgoPrep's UI supports both Vietnamese and English, replacing the "chưa quyết" (undecided) status of
      i18n in `01-rd/overview/system_survey.md` section 8.2. This is scoped to UI-string translation only —
      it does not decide locale-specific formatting, RTL, or additional languages beyond vi/en. Four Admin
      prototype screens (`Chấm lại`, `Cấu hình AI`, `Hàng đợi chấm`, `Ngôn ngữ và giới hạn`) currently lack
      the `data-ui-lang`/`data-lang` scaffold that nearly every other screen already has; bringing them into
      line is deferred to when those screens' real UI is built or their DD is written — it does not block
      RD, per `06-plan/PROTOTYPE_DEBT.md` item 4.2.
    rationale: >
      Nearly every learner-facing screen in the static prototype already carries `data-ui-lang`/
      `data-lang="vi|en"` scaffolding — the prototype had effectively already committed to bilingual
      support before RD formally decided it. Confirming now prevents `02-bd/screens/` from treating i18n as
      an open question it must re-litigate, and flags the four Admin screens that need to catch up instead
      of silently staying Vietnamese-only. Owner instruction, session 2026-08-24.
      **AMENDED 2026-08-28:** one of the four named screens, `Chấm lại` (`admin_rejudge`), is removed from
      scope entirely by `DEC-2026-0828-remove-rejudge-scope` — it no longer needs the bilingual scaffold
      because it is not being built at all. The remaining three (`Cấu hình AI`, `Hàng đợi chấm`, `Ngôn ngữ
      và giới hạn`) are unaffected and still need the scaffold as originally decided.
    source: "Owner instruction, session 2026-08-24 · 06-plan/PROTOTYPE_DEBT.md item 4.2"
    impacted_paths:
      - 01-rd/overview/overview.md
      - 01-rd/overview/system_survey.md

  - id: DEC-2026-0824-dual-submission-model-per-problem
    kind: architecture
    status: ACTIVE
    date: 2026-08-24
    subject: "Every problem supports both submission models in parallel; the learner picks Function Wrapper or Standard I/O per attempt, not the instructor per problem"
    decision: >
      `01-rd/req/req.md` F3-13 previously framed Standard I/O as a deliberate fallback the instructor
      (A2) opts into per problem, only when the harness type schema cannot cover that problem's data
      types — each problem fixed to exactly one model. That framing is corrected: every problem declares
      BOTH a function signature (F2-03, feeds Function Wrapper codegen) AND a stdin/stdout format spec
      (also F2-03, feeds Standard I/O), and the learner freely switches between the two models per attempt
      at the `problem_detail` screen — matching `README.md` section 5 line 179 ("Hỗ trợ cả 2 mô hình: Bọc
      hàm và Nhập/Xuất chuẩn") and the reference-platform comparison at section 1.1 line 15 (HackerRank
      column: "Cả hai"). The only remaining case where a problem shows a single model is when its data
      type genuinely exceeds F3's type schema (F3-01) — then Function Wrapper is hidden entirely and only
      Standard I/O is offered, since Standard I/O never depends on the type schema. This roughly doubles
      the codegen surface of F3 (harness) per problem per language, versus the narrower "rare fallback"
      reading it replaces.
    rationale: >
      Discovered while writing `01-rd/screens/problem_detail.md`: the static prototype
      (`09-layoutBase/Workspace giải bài.dc.html` lines 590-596) already lets the learner freely toggle
      between "Bọc hàm" and "Có hàm main" for the same problem (Two Sum), with no per-problem lock — this
      is the higher-precedence source (`README.md`, P2) being right and `req.md`'s F3-13 (P3) having
      narrowed the scope incorrectly when it was first written. Confirmed with the project owner before
      writing the correction into `req.md`, since it changes the harness (F3) codegen surface — the most
      expensive-to-get-wrong area per `CLAUDE.md` Rules. Flagging now, before BD/DD for `harness` goes
      deep, is cheaper than discovering it after the type-schema BD is written against the narrower
      reading.
    source: "Owner instruction, session 2026-08-24 · 01-rd/screens/problem_detail.md open questions"
    impacted_paths:
      - 01-rd/req/req.md
      - 01-rd/overview/system_survey.md
      - 01-rd/req/user_stories.md
      - 02-bd/packages/harness/architecture.md

  - id: DEC-2026-0825-frontend-base-architecture
    kind: architecture
    status: ACTIVE
    date: 2026-08-25
    subject: "Frontend base: route groups with /admin and /instructor URL prefixes, next-intl without a locale URL segment, next-themes with CSS-variable tokens, pnpm workspace root at 05-coding/"
    decision: >
      Four structural choices for `05-coding/frontend/`, made when scaffolding the FSD base and
      recorded here because each one is expensive to reverse once 25 screens are built on top.
      (1) ROUTING BY ACTOR: Next.js route groups `(public)` `(student)` `(instructor)` `(admin)`,
      each with its own `layout.tsx`. The instructor and admin areas additionally carry a REAL URL
      segment — `/instructor/*` and `/admin/*` — while the student area stays at the root
      (`/problems`, `/progress`, `/submissions`). Route groups alone add no URL segment, which would
      have put `/users` (admin user management) and `/overview` (instructor) in the same flat
      namespace as the student screens. The role required by each area lives in exactly one place,
      `entities/user/model/area.ts` → `ROLE_BY_AREA`, which the future `middleware.ts` must read
      rather than restate.
      (2) i18n WITHOUT A LOCALE URL SEGMENT: `next-intl` configured with no `routing`/`[locale]`
      segment. The locale comes from the `NEXT_LOCALE` cookie and is mirrored onto `<html
      data-ui-lang>`; the URL never changes when the language does. `next-intl` is reachable only
      through the facade `shared/i18n` (client) and `shared/i18n/server` (server) — every other
      layer importing it directly fails `pnpm lint`.
      (3) THEME: `next-themes` with `attribute="class"` and `defaultTheme="light"`; semantic colour
      tokens are CSS variables in `app/globals.css` (`:root` light, `.dark` dark), Tailwind v4.
      (4) BUILD LAYOUT: `05-coding/` is the pnpm workspace root (`pnpm-workspace.yaml` listing only
      `frontend`); `05-coding/package.json` holds the aggregate scripts, so `pnpm check` runs from
      one place. The backend is Maven and outside the Node ecosystem.
    rationale: >
      (1) URL prefixes: middleware role guards match on path prefix (`/admin/:path*`). Without the
      prefix, guarding requires a hand-maintained route-to-role table that silently goes stale every
      time a screen is added — a permission hole invisible in the diff. Flat URLs also read wrong:
      `/users` and `/overview` say nothing about which area owns them.
      (2) No locale segment: this preserves `01-rd/system/frontend_architecture.md:34-37`, which had
      already settled the `data-ui-lang` / `data-lang` mechanism, and keeps every URL stable and
      shareable regardless of the reader's language — a problem link is the same link in both
      locales. A `[locale]` prefix was proposed twice during the session and declined by the owner
      both times. Verified end-to-end by a Playwright test that flips the cookie and asserts the
      heading changes from "Danh sách bài toán" to "Problems" with the URL untouched.
      (3) `next-themes` handles the FOUC and hydration-mismatch problem that a hand-rolled
      class toggle gets wrong in the App Router; CSS variables keep the token names stable while the
      design system itself is still unchosen (`frontend_architecture.md` section 6).
      (4) Workspace root: one `pnpm check` entry point for the G-CHECK gate in `CLAUDE.md`, and room
      to add a second Node package (shared tooling, standalone e2e) without moving the lockfile.
      Recorded as a decision rather than left in `05-coding/frontend/README.md` because routing
      structure and the i18n mechanism are architecture, and a README sits at P4 (code) in
      `sot-precedence.md` — the lowest rung, where the next reader of `frontend_architecture.md`
      would not find it.
    source: "Owner instruction, session 2026-08-25 (frontend base scaffold + review round)"
    impacted_paths:
      - 05-coding/frontend/README.md
      - 05-coding/frontend/eslint.config.mjs
      - 05-coding/frontend/src/entities/user/model/area.ts
      - 01-rd/system/frontend_architecture.md
      - 01-rd/system/codebase_structure.md

  - id: DEC-2026-0825-shared-content-authoring-screens
    kind: architecture
    status: ACTIVE
    date: 2026-08-25
    subject: "problem_authoring, problem_management, and interview_question_management are shared screens mounted at both /instructor and /admin, scoped by permission, not owned by one actor area"
    decision: >
      Three content-authoring screens are mounted as ONE view at two routes — `/instructor/problems`,
      `/instructor/problems/[id]`, `/instructor/interview-questions` in the Instructor area, and
      `/admin/problems`, `/admin/problems/[id]`, `/admin/interview-questions` in the Admin area — instead of
      building two separate screens or forcing the content into a single actor's area. Data scope is
      permission-driven: A2 (instructor) sees/edits problems and interview questions belonging to
      themself or the classes they teach; A3 (admin) sees the entire bank. The gating mechanism already
      exists in the permission matrix: `PROBLEM_AUTHORING` and `TESTCASE_MANAGEMENT`
      (`01-rd/req/F1_identity.md` — F1-12) and `INTERVIEW_BANK_MANAGEMENT` (`01-rd/req/F1_identity.md` — F1-12), each governed by
      F1-10 through F1-12 — no new Function is introduced by this decision.
      Two renames follow from mounting in both areas: `admin_problem_management` → `problem_management`
      and `admin_interview_question_management` → `interview_question_management` (both moved from
      `01-rd/screens/admin/` to `01-rd/screens/shared/`; `problem_authoring` moved from
      `01-rd/screens/teacher/` to `01-rd/screens/shared/`, slug unchanged). `01-rd/overview/system_survey.md`
      gained a new "7.0. Khu vực dùng chung (nhiều actor)" section holding these three; the Instructor
      section (7.2) dropped from 6 to 4 slugs, the Admin section (7.3) from 11 to 9, total screens 30 → 29.
      `testcase_management` is retired as a slug entirely — it was never built as a separate screen in the
      prototype (its content is a tab inside `problem_authoring`), and this decision confirms the merge is
      permanent rather than a placeholder.
    rationale: >
      Discovered during the Admin-area prototype reconciliation (`06-plan/PROTOTYPE_DEBT.md` section 7):
      `req.md` assigns F2-01 through F2-04 (problem authoring) and F6-11 (interview question banks) to actor
      A2, and `system_survey.md` section 7.2 had already committed to Phương án B — the Instructor area gets
      its own layout, separate from Admin's shell (`06-plan/PROTOTYPE_DEBT.md` section 3.3). But the only
      prototypes ever built for these two content flows (`Admin - Soạn đề bài.dc.html`,
      `Admin - Quản lý bài tập.dc.html`, `Admin - Câu hỏi phỏng vấn.dc.html`) live in the Admin shell, and no
      corresponding prototype exists anywhere under `09-layoutBase/Giáo viên - *.dc.html`. Rebuilding a
      second, Instructor-shelled prototype for content A2 must actually be able to create was rejected as
      wasted design work when the existing screen already does the job; keeping the screens Admin-only was
      rejected because it silently strips A2 of a capability `req.md` explicitly grants (F2-01 to F2-04,
      F6-11) and that `US-A2-01`/`US-A2-05` describe. Mounting one view at two routes, gated by the
      already-declared permission Functions, satisfies both constraints without inventing new scope. Does
      NOT reverse Phương án B: Instructor keeps its own layout and route prefix for every screen that is not
      one of these three; this decision only says two specific content-management flows are shared data,
      not shared UI ownership by area.
    source: "Owner instruction, session 2026-08-25 (admin-area prototype reconciliation, RD review round)"
    impacted_paths:
      - 01-rd/overview/system_survey.md
      - 01-rd/screens/shared/SHR0202_problem_authoring.md
      - 01-rd/screens/shared/SHR0201_problem_management.md
      - 01-rd/screens/shared/SHR0301_interview_question_management.md
      - 01-rd/screens/admin/ADM0101_overview.md
      - 06-plan/PROTOTYPE_DEBT.md
      - 05-coding/frontend/src/app
  - id: DEC-2026-0825-uml-skill-parity
    kind: process-policy
    status: ACTIVE
    date: 2026-08-25
    subject: "vp-uml-architect brought to parity with mermaid-uml-architect: shared complexity ceilings enforced by lint, shared colour palette as an optional mode, stick-figure actors as the deciding factor between the two skills"
    decision: >
      The two UML skills now share everything except output format. Four binding points:
      (1) COMPLEXITY CEILINGS — `vp-uml-architect/scripts/render.py` gained the same `BUDGET` table as
      `mermaid-uml-architect` (Use Case 16 nodes / 16 edges, Sequence 6 participants / 14 messages,
      Activity 14 nodes / 18 edges) and prints a `LINT` line before it calls Java. A warning does NOT block
      the render — exceeding is sometimes correct — but a hand-off that leaves a warning unmentioned is
      incomplete. Changing a ceiling means changing it in BOTH scripts and in BOTH SKILL.md files.
      (2) EDGE/NODE RATIO SCOPE — the "edges <= 1.5x nodes" rule now applies to Use Case and Activity only,
      in both skills. It was previously applied to Sequence too, where it contradicted that type's own
      ceiling (6 participants / 14 messages is already a ratio of 2.3) and therefore fired on every
      correctly-sized sequence diagram, including the skill's own template.
      (3) COLOUR IS NO LONGER A DISCRIMINATOR — `vp-uml-architect` gained an optional colour mode using the
      identical palette (`#FFF3B0` use-case oval, `#DCEBFC` actor, `#F7F7F7` system frame, `#E8E8E8`
      external system), and a Sequence colour-by-role palette identical to the Mermaid one (`#C6EFCE`
      controller, `#FFF2CC` service, `#FCD5B4` encoder/util, `#FFC7CE` repository, `#BDD7EE` external or
      async, `#D9D9D9` bare datastore, human actor unboxed). Monochrome stays the DEFAULT because the
      capstone report is printed. Choose by destination: print → monochrome, screen or a document that
      also carries Mermaid figures → colour. ACTIVITY has no colour mode and is not getting one: the
      Mermaid skill draws Activity monochrome, so monochrome IS the parity state; a coloured activity
      diagram with responsibility lanes remains `nexa-ad-builder`'s job.
      (4) WHAT ACTUALLY DECIDES THE SKILL — stick-figure actors and print-ready greyscale are PlantUML only;
      diagram source living inside the Markdown document, diffable and unable to drift out of sync with an
      image, is Mermaid only. Mermaid CANNOT draw a stick figure in a use case diagram: its `flowchart` has
      no such shape and the `image` shape that could carry an SVG is absent from the bundled renderer
      (`mermaid-cli` 11.16 answers `Error: No such shape: image`). When the owner asks for actors drawn as
      people, the answer is `vp-uml-architect` regardless of destination.
      Two PlantUML facts were verified and written into the skill during this work, because both fail in
      ways that do not point at the cause: a one-line `skinparam actor { ... }` block is INVALID and reports
      only `Some diagram description contains errors` (multi-line braces are required), and an external
      system declared as `actor` renders as a human stick figure and must be a `rectangle` instead.
    rationale: >
      `mermaid-uml-architect` was improved over several sessions (colour palettes, the complexity budget,
      the render-verified layout rules) while `vp-uml-architect` stayed at its original monochrome-only
      shape. The drift was not cosmetic: the same business content passed one skill's review and failed the
      other's, the PlantUML skill had no mechanism at all to catch the most common failure (drawing too
      much), and `vp-uml-architect`'s own "Boundaries with other skills" table still described the Mermaid
      skill as monochrome — reading it led to the wrong tool choice. Rejected alternative: leaving PlantUML
      monochrome-only and telling the owner to override `skinparam` by hand each time colour is needed —
      that is how two figures in the same report end up in different colour languages. Rejected also:
      making colour the default, which would quietly degrade every greyscale print of the capstone report.
    source: "Owner instruction, session 2026-08-25 (use case overview diagram for 01-rd/req)"
    impacted_paths:
      - .claude/skills/vp-uml-architect/SKILL.md
      - .claude/skills/vp-uml-architect/scripts/render.py
      - .claude/skills/vp-uml-architect/assets/usecase.template.puml
      - .claude/skills/vp-uml-architect/assets/usecase-detail.template.puml
      - .claude/skills/mermaid-uml-architect/scripts/render.py
  - id: DEC-2026-0827-drawio-uml-architect
    kind: process-policy
    status: ACTIVE
    date: 2026-08-27
    subject: "drawio-uml-architect added as the third UML skill: .drawio for Use Case / Sequence / Activity, spec-driven with a layout engine; the complexity ceiling now lives in three scripts; the boundary with nexa-ad-builder is audience, not file format"
    decision: >
      A third UML skill, `drawio-uml-architect`, owns editable `.drawio` output for Use Case, Sequence
      and Activity. Four binding points.
      (1) SPEC-DRIVEN, NOT HAND-AUTHORED. The skill never writes `mxCell` XML directly. A semantic spec
      JSON (`<name>.spec.json`, committed beside the diagram) declares only relationships, and
      `scripts/gen_drawio.py` computes every coordinate. The spec has no `x`/`y` fields at all. For a use
      case diagram the load-bearing field is `groups` — a set of use cases plus the actors entitled to
      them — from which the engine derives actor placement, seating each actor beside its own cluster so
      an association is a short horizontal line rather than a diagonal crossing the page. The engine emits
      one or two columns and never three, because a middle column has actors on both sides and every edge
      from the far column must cross one; at that size the answer is to split the diagram.
      (2) THE CEILING NOW LIVES IN THREE PLACES. `BUDGET` in `drawio-uml-architect/scripts/gen_drawio.py`
      and in `scripts/lint_drawio.py` carries the same numbers as `vp-uml-architect/scripts/render.py` and
      `mermaid-uml-architect/scripts/render.py` (Use Case 16 node / 16 edge, Sequence 6 / 14, Activity
      14 / 18, plus edges <= 1.5x nodes for Use Case and Activity only). This extends
      `DEC-2026-0825-uml-skill-parity` point (1): changing a ceiling now means changing FOUR scripts and
      THREE SKILL.md files.
      (3) COLOUR IS THE DEFAULT HERE, unlike `vp-uml-architect` where monochrome is. The palette is
      byte-identical to the shared one (`#FFF3B0` use-case oval, `#DCEBFC` actor, `#F7F7F7` system frame,
      `#E8E8E8` external system; Sequence by role `#C6EFCE` controller, `#FFF2CC` service, `#FCD5B4`
      util, `#FFC7CE` repository, `#BDD7EE` external, `#D9D9D9` bare datastore, human actor unboxed).
      `--mono` switches to print mode. ACTIVITY STAYS MONOCHROME even in colour mode, preserving the
      parity state set by `DEC-2026-0825-uml-skill-parity` point (3).
      (3b) AMENDED 2026-08-28 after a self-audit found four defects in the first cut, all fixed:
      sequence messages now anchor to the lifeline cells at both ends instead of to free-floating
      waypoints (previously dragging a participant left the arrows behind, defeating the one reason to
      choose `.drawio`); a self-call now exits and re-enters the same side so it loops back to its own
      lifeline instead of rendering as an L; activity node y-position now comes from graph DEPTH rather
      than list position, so two branches of one decision sit side by side directly beneath it instead of
      one being pushed to the bottom of the page, and a forward edge skipping more than one depth level is
      routed out to the margin rather than straight down through the nodes it skips; and SKILL.md's claim
      that group frames are true containers was corrected — they are background rectangles, so dragging a
      package does NOT carry its use cases, and making them real `container=1` parents remains an open
      improvement requiring all three diagram types to change together. Scope also grew to match the
      shared ceiling it already cited: ONE `alt` block for Sequence and ONE backward edge (loop) for
      Activity are now expressible, and the generator refuses a second of either.
      (4) BOUNDARY WITH nexa-ad-builder IS AUDIENCE, NOT FILE FORMAT. Both skills emit `.drawio` activity
      diagrams and pretending otherwise would make both descriptions lie. `nexa-ad-builder` keeps the
      coloured stakeholder/Confluence flowchart and User Flow, with its 9-colour semantic palette and
      mandatory legend. `drawio-uml-architect` takes report-grade monochrome UML activity plus Use Case
      and Sequence, which nothing previously served in `.drawio` at all. Reciprocal `Do NOT use for` lines
      go in both frontmatters. The discriminator against the other two UML skills is likewise not colour
      (all three now share the palette) but the recipient: editable by hand is `.drawio`, print-ready
      greyscale is PlantUML, source embedded in Markdown is Mermaid.
    rationale: >
      The owner asked three times in one session for the same use case diagram's layout to be fixed —
      actors placed beside their own functions, no crossing lines, fitting A4 — and each round was a
      hand-recomputation of coordinates that introduced a fresh misalignment. That is the argument for the
      spec-driven design: hand-placed geometry does not survive the second edit, because adding one actor
      shifts every column and each pass is another chance to omit `as="geometry"` and collapse the drawing
      to the origin. Encoding the layout as a function makes the same spec produce the same file, so the
      diagram is regenerable when the design changes rather than redrawn.
      A validator was the second gap. `.drawio` fails silently in ways no other diagram format does — a
      missing `as="geometry"` discards all coordinates, an edge naming a non-existent id loses the
      connector, a `--` inside an XML comment stops the file parsing, and draw.io re-saves as
      base64-compressed content that is no longer diffable. None of these error at write time; all of them
      surface only when somebody opens the file, typically after "done" was reported. `nexa-ad-builder`
      had no validation step at all, and its only complete XML sample omits `as="geometry"` and would
      import broken. `scripts/lint_drawio.py` checks all of these plus geometric node overlap, and runs on
      any `.drawio` file including output from the other skill.
      Overlap detection specifically exists because this format has no layout engine. PlantUML has
      Graphviz and Mermaid has dagre pushing nodes apart; neither will let two ovals sit on top of each
      other. draw.io will, in silence. The linter is doing the job the engine does for free elsewhere.
      Rejected alternative: teaching the model to hand-write `mxCell` from a template, the way
      `nexa-ad-builder` does. It is more flexible for unusual layouts and measurably worse at the thing
      actually being asked for — repeating a tidy layout after an edit. Rejected also: ceding Activity
      entirely to `nexa-ad-builder` to avoid overlap, which would have left the project unable to produce
      a monochrome report-grade activity diagram in an editable format. Rejected also: making the
      complexity ceiling advisory here to avoid a fourth sync point; the same business content must hit
      the same ceiling whichever tool draws it, or switching tools silently changes the scope of the
      diagram.
      On the validator earning its place: the one baseline run that completed before the session hit its
      quota produced a structurally complete 18KB hand-written sequence diagram — closing `</mxfile>` and
      all — that will not open in draw.io, because a label read `value="bấm "Đăng nhập""` and the
      unescaped quote closed the attribute early. Nothing in that run would have caught it before
      hand-off. Vietnamese UI copy quotes button names constantly, so this is a routine failure rather
      than an edge case, and it is now a row in the silent-failure catalogue.
      Honest limits recorded in the skill rather than hidden: no draw.io CLI exists on this machine, so
      the skill cannot render an image and inspect it, and it is explicitly forbidden from claiming the
      layout was visually verified — it must ask the owner to open the file. The overlap detector is a
      mechanical substitute that catches shapes on top of each other, not an edge routed around half the
      diagram.
    source: "Owner instruction, session 2026-08-27 (skill-creator run: draw.io skill following the mermaid and puml rules, layout as tidy as the use case overview)"
    impacted_paths:
      - .claude/skills/drawio-uml-architect/SKILL.md
      - .claude/skills/drawio-uml-architect/scripts/gen_drawio.py
      - .claude/skills/drawio-uml-architect/scripts/lint_drawio.py
      - .claude/skills/drawio-uml-architect/references/spec-schema.md
      - .claude/skills/drawio-uml-architect/references/layout-and-limits.md
      - .claude/skills/drawio-uml-architect/assets/usecase.spec.json
      - .claude/skills/drawio-uml-architect/assets/sequence.spec.json
      - .claude/skills/drawio-uml-architect/assets/activity.spec.json
      - .claude/skills/nexa-ad-builder/SKILL.md
      - .claude/skills/vp-uml-architect/SKILL.md
      - .claude/skills/mermaid-uml-architect/SKILL.md
  - id: DEC-2026-0828-split-class-management-assignments
    kind: architecture
    status: ACTIVE
    date: 2026-08-28
    subject: "class_management screen split into two slugs — class_management (Lop cua toi) and class_assignments (Bai tap cua toi) — one per Next.js route"
    decision: >
      The seed slug `class_management` (`01-rd/screens/teacher/INS0201_class_management.md`) described two
      prototypes with independent nav entries and routes — `Giáo viên - Lớp của tôi.dc.html` (class
      roster/overview) and `Giáo viên - Bài tập của tôi.dc.html` (assignment list, gán bài từ ngân
      hàng cho lớp) — under one RD file, per Câu hỏi mở Q1 in that file. Resolved: split into two
      slugs for the screen axis (`01-rd/screens/` → `02-bd/screens/` → `03-dd/screens/`), matching
      the two distinct Next.js routes/components:
      (1) `class_management` keeps the "Lớp của tôi" content only — class overview, student roster,
      Q2 (CRUD lớp — vẫn mở), Q3 (chi tiết lớp), Q4 (quản lý thành viên), Q5 (F6-11 chưa có UI), Q6
      (chấm lại chưa có UI).
      (2) `class_assignments` is a new slug carrying the former "Bài tập của tôi" content — assignment
      list gán từ ngân hàng bài toán, Q7 (luồng gán bài chưa dựng đủ), Q8 (lỗi liên kết "Chi tiết"
      trỏ nhầm sang câu hỏi phỏng vấn), Q9 (không có nút gỡ bài đã gán), plus mục 3.3 (đối chiếu hai
      chiều với `problem_list`).
      Teacher-area screen count moves 4 → 5, total screen count moves 29 → 30. `system_survey.md`
      mục 7.2 and the summary line at the end of mục 7.3 are updated to match. No frontend code slice
      exists yet for either half (only `views/class-management/` is scaffolded, empty) — `class_assignments`
      needs a new `views/class-assignments/` slice, an `/instructor/assignments`-style route, and a
      `frontend_architecture.md`/`frontend/README.md` slice-count update when that scaffold work happens;
      this decision covers the RD/BD/DD document split only, not the code scaffold.
    rationale: >
      `system_survey.md` mục 7.2 gộp hai prototype có route và nav item độc lập vào một slug vì cùng
      chủ đề nghiệp vụ ("quản lý lớp + giao bài"), nhưng BD/DD viết theo trục màn hình — mỗi màn có
      route và component riêng ở FE Next.js (`CLAUDE.md` — hai trục BD/DD, "screens cannot be filed
      under a single context/route either"). Giữ gộp sẽ buộc một file BD/DD phải mô tả hai component
      không chia sẻ state hay layout, làm hỏng tính 1-slug-1-route mà mọi slug khác trong dự án đang
      tuân theo (đối chiếu bảng 29 màn ở `01-rd/README.md` mục 4 / báo cáo review
      `07-review/rd_review_report_260826.html` mục 4 — 29/29 slug đều 1-1 với 1 route). Owner chấp
      nhận đề xuất ở Câu hỏi mở Q1 của `class_management.md` mục 5.
    source: "Owner instruction, session 2026-08-28 — chấp nhận đề xuất tách slug ở Câu hỏi mở Q1, 01-rd/screens/teacher/INS0201_class_management.md:132"
    impacted_paths:
      - 01-rd/overview/system_survey.md
      - 01-rd/screens/teacher/INS0201_class_management.md
      - 01-rd/screens/teacher/INS0202_class_assignments.md
      - 07-review/rd_review_report_260826.html
  - id: DEC-2026-0828-remove-rejudge-scope
    kind: architecture
    status: ACTIVE
    date: 2026-08-28
    subject: "Rejudge (re-judge / chấm lại) feature removed from scope entirely — F4-09a to F4-09e, the admin_rejudge screen, and the REJUDGE_MANAGEMENT permission function are all out of scope"
    decision: >
      The rejudge feature (re-run historical submissions against an updated testcase set) is removed
      entirely from project scope, answering Câu hỏi mở Q6 of `01-rd/screens/teacher/INS0201_class_management.md`.
      Removed: `F4-09a` (choose rejudge scope) through `F4-09e` (audit trail) in `01-rd/req/req.md`; the
      `admin_rejudge` screen (`01-rd/screens/admin/ADM0402_rejudge.md`, kept only as a struck-through
      historical record, not a live spec); the `REJUDGE_MANAGEMENT` permission function in F1-12's function
      list; `US-A3-05` and the rejudge-related GWT bullets in `US-A2-03` (`01-rd/req/user_stories.md`).
      New behavior in place of rejudge: if a testcase turns out to be wrong after submissions already ran
      against it, the student reports the issue to the instructor, the instructor fixes the testcase (still
      bumping the testcase-set version per F2-09) — **old submissions keep their old (possibly wrong)
      result; there is no automatic mass re-grading.** Screen/code count impact: admin screens 9 → 8, total
      screens 30 (net zero this session — offset by the `class_student_detail` addition from the same
      session, see `DEC-2026-0828-split-class-management-assignments`); F4 code count 15 → 10.
    rationale: >
      Owner's own words: "kcan chấm lại vì mỗi học viên khi làm 1 bài go judge đã fail ngừng ngay khi có
      test case fail rồi nên chỉ hoàn thành bài khi mà đạt full bài nên bỏ cho tôi ycau chấm lại" — go-judge
      fail-fast within a single run means a submission only completes when it passes every testcase, so the
      owner's intuition was that rejudge is redundant. This session flagged the distinction before
      recording the decision: fail-fast governs ONE run of ONE submission; rejudge exists for a DIFFERENT
      situation — an instructor fixes a testcase bug AFTER submissions were already graded against the
      buggy version, and historically needs those old submissions re-evaluated against the fixed version.
      The owner explicitly considered this distinction and still chose to drop rejudge, with an explicit
      accepted trade-off: "bỏ đi vì cái đó hsinh có thể report để giảng viên sửa testcase là được, giảm
      chức năng không cần thiết" — manual student-reports-bug + instructor-fixes-testcase replaces the
      automated mass re-grading; old submissions simply keep stale results. This is a legitimate scope cut
      for a capstone project, not an oversight — recorded here specifically because it removes an entire
      screen, an entire F4-09 code family, and a permission function across roughly a dozen RD files, which
      crosses the "architecture-level divergence" threshold in `CLAUDE.md` even though no BD/DD/code existed
      yet for it.
    source: "Owner instruction, session 2026-08-28, in response to a clarifying question about the fail-fast vs. rejudge distinction"
    impacted_paths:
      - 01-rd/req/req.md
      - 01-rd/req/user_stories.md
      - 01-rd/overview/system_survey.md
      - 01-rd/screens/admin/ADM0402_rejudge.md
      - 01-rd/screens/admin/ADM0101_overview.md
      - 01-rd/screens/admin/ADM0401_queue_monitor.md
      - 01-rd/screens/admin/ADM0202_permission_matrix.md
      - 01-rd/screens/admin/ADM0501_language_config.md
      - 01-rd/screens/admin/ADM0403_system_log.md
      - 01-rd/screens/shared/SHR0202_problem_authoring.md
      - 01-rd/screens/users/USR0201_submission_result.md
      - 01-rd/screens/teacher/INS0201_class_management.md
      - 01-rd/screens/teacher/INS0202_class_assignments.md
  - id: DEC-2026-0828-remove-per-class-interview-set
    kind: architecture
    status: ACTIVE
    date: 2026-08-28
    subject: "Per-class interview question sets removed from scope — F6-11 and US-A2-04 dropped; students use one shared, system-wide interview bank"
    decision: >
      The feature letting an instructor curate a private interview-question set from the shared bank and
      assign it to their class (`F6-11` in `01-rd/req/req.md`, `US-A2-04` in `01-rd/req/user_stories.md`) is
      removed from scope, answering Câu hỏi mở Q5 of `01-rd/screens/teacher/INS0201_class_management.md`. Students
      use the one shared, system-wide interview bank directly (`interview_bank_list`,
      `interview_question_management`) — there is no "per-class question set" concept anywhere in the
      product. `F6-11` is struck through in `req.md`; `INTERVIEW_BANK_MANAGEMENT` in F1-12's function list
      keeps only `F6-12` (spaced-repetition self-assessment, unaffected — that is a per-student concept, not
      per-class). `US-A2-04` is struck through in `user_stories.md`.
    rationale: >
      Owner's own words, answering Q5: "kcan mỗi lớp có pvan riêng dùng chung với hệ thống luôn cx đc" — no
      need for each class to have its own interview set; sharing the system-wide bank directly is fine. This
      is a straightforward scope cut with a small, well-contained blast radius (no dedicated screen existed
      for it — F6-11 was always going to be built as extra behavior bolted onto `class_management`, per
      that file's Câu hỏi mở Q5 proposal of "(a) a tab in class_management or (b) a separate screen" — the
      owner's answer means neither is needed). Recorded as a decision because it removes a requirement code
      and a user story that were already locked in `req.md`/`user_stories.md`, not because of any unusual
      complexity.
    source: "Owner instruction, session 2026-08-28, answering Câu hỏi mở Q5 of class_management.md directly"
    impacted_paths:
      - 01-rd/req/req.md
      - 01-rd/req/user_stories.md
      - 01-rd/overview/system_survey.md
      - 01-rd/screens/teacher/INS0201_class_management.md
      - 01-rd/screens/teacher/INS0202_class_assignments.md
      - 01-rd/screens/teacher/INS0101_instructor_overview.md
      - 01-rd/screens/admin/ADM0101_overview.md
      - 01-rd/screens/shared/SHR0301_interview_question_management.md

  - id: DEC-2026-0830-problem-lifecycle-two-states
    kind: architecture
    status: ACTIVE
    date: 2026-08-30
    subject: "Problem lifecycle simplified to exactly two states — Chưa xuất bản (unpublished) / Đã xuất bản (published) — dropping the third Đã ẩn (hidden) state the prototype used"
    decision: >
      Answers Câu hỏi mở Q2 of `01-rd/screens/shared/SHR0201_problem_management.md`. The problem lifecycle has
      exactly two states, `Chưa xuất bản` and `Đã xuất bản` — the prototype's third state `Đã ẩn` (a
      previously-published problem pulled back down) is dropped; pulling a published problem down now
      returns it to plain `Chưa xuất bản`, with no separate state recording that it was once public.
      New code `F2-15` in `01-rd/req/req.md`: default on creation is `Chưa xuất bản`; publish
      (`Chưa xuất bản` → `Đã xuất bản`) requires at least one Hidden testcase (F2-06) and a complete spec
      per the declared submission model (F2-03), hard-blocked with a reason otherwise; unpublish
      (`Đã xuất bản` → `Chưa xuất bản`) has no precondition and does not delete related data — old
      submissions, bookmarks, interview sessions stay intact, same "soft-hide, no data loss" principle as
      `F1-16`. Only `Đã xuất bản` problems appear in `problem_list` (F2-11, actor A1);
      `problem_management` (A2/A3) always sees both states. New user story `US-A2-01b` in
      `01-rd/req/user_stories.md`. `problem_management.md` updated: Q2 closed; the stale "filter tab missing
      a 4th value for Đã ẩn" warning is resolved by construction (the prototype's 3 tabs — Tất cả / Đã xuất
      bản / Bản nháp — already match 2 states + "Tất cả"); the 4 summary stat cards, the "Bài cần chú ý"
      block, and every GWT bullet that named `Bản nháp`/`Đã ẩn` are relabelled to the 2-state model; Q3
      (delete semantics) is narrowed — it can no longer reuse `Đã ẩn` as its soft-delete mechanism since that
      state no longer exists, so the open question is now "a third lifecycle state (`Đã lưu trữ`) vs. a
      `deleted` flag layered on `Chưa xuất bản`"; Q5's dependency on Q2 is lifted (its "Bản nháp quá 7 ngày"
      threshold becomes "Chưa xuất bản quá 7 ngày", no longer blocked, only the numeric threshold itself
      stays open).
    rationale: >
      Owner instruction, directly answering Q2: keep only "đã xuất bản" and "chưa xuất bản" so the lifecycle
      stays lean instead of carrying a third state whose only real difference from Bản nháp was history (had
      it ever been public before). Recorded as a decision because it assigns a new requirement code (F2-15)
      spanning 5+ screen blocks in `problem_management.md`, closes one open question while narrowing a
      second (Q3), and changes labels the prototype hard-coded — the kind of RD-locking change
      `CLAUDE.md` requires a decision entry for.
    source: "Owner instruction, session 2026-08-30, answering Câu hỏi mở Q2 of problem_management.md directly"
    impacted_paths:
      - 01-rd/req/req.md
      - 01-rd/req/user_stories.md
      - 01-rd/screens/shared/SHR0201_problem_management.md

  - id: DEC-2026-0830-remove-student-review-request
    kind: architecture
    status: ACTIVE
    date: 2026-08-30
    subject: "Student-initiated review request removed from scope entirely — the two-way 'học viên yêu cầu review, giảng viên nhận việc' feature named in Câu hỏi mở Q1 of instructor_grading.md never gets a Fx-nn code"
    decision: >
      The feature where a student actively requests instructor review of a submission (prototype: cột
      "Điểm AI" showing the string "Yêu cầu review" instead of a score, stat card "Yêu cầu review · Học viên
      chủ động gửi", part of the page description "Bài nộp AI chấm điểm thấp hoặc học viên yêu cầu review")
      is removed from scope entirely, answering Câu hỏi mở Q1 of
      `01-rd/screens/teacher/INS0301_grading.md`. No `F5-xx` code is ever assigned for it — unlike the
      rejudge/per-class-interview-set removals, this feature was never coded into `req.md` or
      `user_stories.md` in the first place, so there is nothing to strike in those files. The
      `instructor_grading` queue now has exactly one source: submissions with a low AI score
      (threshold to be settled per that file's Q2). Struck through in
      `01-rd/screens/teacher/INS0301_grading.md`: the "Yêu cầu review" stat card (§3.2, now 3 cards
      instead of 4), the "Yêu cầu review" string value of the Điểm AI column (§3.4, that column is now
      always a numeric score), the GWT bullet exercising that column value (§4), and the "học viên yêu cầu
      review" clause of the page description (§3.1, flagged `[Đợi nextjs]` for the real UI copy).
      `01-rd/screens/teacher/INS0101_instructor_overview.md` updated at two points: the "Cần chấm tay" widget
      description (§3.5) and the "Hoạt động gần đây" sample feed item "yêu cầu review điểm" (§3.6).
    rationale: >
      Owner instruction, directly answering Q1 (grouped with two other RD-review follow-ups from the same
      session: CRUD lớp học, already closed 2026-08-28; vòng đời bài toán, closed same session as this one —
      `DEC-2026-0830-problem-lifecycle-two-states`). Recorded as a decision because, even though no code or
      user story existed yet to delete, this closes a "Cao — Chặn BD" priority open question by cutting an
      entire two-sided feature (a student-facing trigger plus an instructor-facing queue source) rather than
      specifying it — the kind of scope-narrowing choice `CLAUDE.md` asks to be logged so a future session
      does not re-invent the "Yêu cầu review" flow from the prototype without knowing it was deliberately
      dropped.
    source: "Owner instruction, session 2026-08-30, answering Câu hỏi mở Q1 of instructor_grading.md directly"
    impacted_paths:
      - 01-rd/screens/teacher/INS0301_grading.md
      - 01-rd/screens/teacher/INS0101_instructor_overview.md

  - id: DEC-2026-0830-class-progress-dashboard
    kind: architecture
    status: ACTIVE
    date: 2026-08-30
    subject: "class_progress aggregate dashboard settled at a 'good enough' level — new code F1-28, US-A2-09 — closing Q1-Q6 of class_progress.md with sensible default formulas/thresholds rather than negotiating each number"
    decision: >
      Answers Câu hỏi mở Q1 through Q6 of `01-rd/screens/teacher/INS0203_class_progress.md` in one pass. New code
      `F1-28` in `01-rd/req/req.md` (added to the `CLASS_MANAGEMENT` function's code list, alongside
      F1-23→27): giảng viên xem tổng hợp tiến độ nhiều học viên trong lớp phụ trách. Formulas settled:
      **Điểm trung bình** (thang 10, per student) = Accepted/total-submission ratio (F1-07) ×10 — explicitly
      NOT the F5-27 AI reference score, which is per-submission, not per-student; **Hoàn thành** (%) =
      assigned problems (F2-12) the student has Accepted at least once, divided by total assigned — a
      different formula from Điểm trung bình, despite both starting from F1-07/F2-12 data; **Chuỗi ngày**
      reuses the streak concept already implied by F1-21's email reminders, not a new concept; **Xu hướng**
      (sparkline) = the same Điểm trung bình formula computed per-student over the last 6 weekly snapshots.
      "Cần chú ý" labels get default thresholds marked `[SoT: Suy luận]` in `req.md` for BD/DD to tune: Vắng
      bài = no submission in the last 7 days, Theo dõi = completion below 50%, Cần hỗ trợ = Điểm trung bình
      dropping for 2+ consecutive weekly snapshots. The "Cần chú ý" block must filter by the selected class
      tab (prototype bug fixed, not a deliberate design — was Q6). Student list paginates like `problem_list`
      (F2-11) and gets an empty state + error-with-retry state. New user story `US-A2-09` in
      `01-rd/req/user_stories.md`. `class_progress.md` updated throughout: Q1-Q6 all closed; the file's
      earlier session-scoped restriction ("do not edit req.md/user_stories.md/system_survey.md/
      decision-registry.md") is noted as lifted for this session.
    rationale: >
      Owner instruction: "tiến độ bạn tạo dashboard ở mức tốt là được" (for progress, just build the
      dashboard at a good level) — explicit delegation to settle the six open questions with reasonable
      defaults rather than negotiating every number, unlike prior decisions in this session where the owner
      gave the exact answer. Recorded as a decision because it assigns a new requirement code spanning 8
      screen blocks plus 6 open questions, and because the numeric thresholds, while marked `[SoT: Suy
      luận]` and BD/DD-tunable, are a judgment call made without the owner naming exact figures — future
      sessions need to know these came from an explicit "good enough" mandate, not from a verified
      requirement.
    source: "Owner instruction, session 2026-08-30 — 'tiến độ bạn tạo dashboard ở mức tốt là đc rồi', answering Câu hỏi mở Q1-Q6 of class_progress.md"
    impacted_paths:
      - 01-rd/req/req.md
      - 01-rd/req/user_stories.md
      - 01-rd/screens/teacher/INS0203_class_progress.md

  - id: DEC-2026-0830-interview-bank-crud
    kind: architecture
    status: ACTIVE
    date: 2026-08-30
    subject: "System-wide interview question bank CRUD — new code F6-13, US-A2-10, separate authoring screen, closing Q2-Q7 of interview_question_management.md as a package"
    decision: >
      Answers Câu hỏi mở Q2 through Q7 of `01-rd/screens/shared/SHR0301_interview_question_management.md` in one
      pass (Q1 was already closed on 2026-08-25). New code `F6-13` in `01-rd/req/req.md` (added to the
      `INTERVIEW_BANK_MANAGEMENT` function's code list, alongside F6-12): quản trị nội dung ngân hàng câu
      hỏi phỏng vấn dùng chung — tạo/sửa/nhân bản/xoá, actor A2 (scoped to their classes) and A3 (whole
      bank). Six sub-decisions bundled:
      1. **Q3 — data model:** each question carries 4 field groups — content+classification (F6-01), a
         "đào sâu" follow-up-question list (distinct from F6-04/05/06 and from F5-11's Phản biện stage),
         and a weighted grading rubric that IS F6-08's "tiêu chí chuẩn" — a third rubric in the system,
         independent of F5-15/F5-23, weights kept because Chế độ luyện needs a per-criterion score anchor.
      2. **Q4 — authoring surface:** a dedicated screen, new slug `interview_question_authoring` (not a
         modal/drawer — 4 nested field groups are too heavy), mirroring the `problem_authoring` precedent.
         No prototype exists yet, flagged `[Đợi nextjs]`; needs adding to `system_survey.md` at BD/DD time.
      3. **Q5 — CSV import:** out of scope for this project's first version, no code assigned. The 4 content-
         quality stat cards are presentation-only derivations, no code needed. A question missing its rubric
         still shows in Chế độ học but is hidden from Chế độ luyện (no AI grading anchor).
      4. **Q6 — topic taxonomy:** the prototype's 5 topics (Lý thuyết CS, System design, Database, Ngôn ngữ,
         Hành vi) replace `req.md`'s stale 4-topic list (Cấu trúc dữ liệu, Thuật toán, Thiết kế hệ thống,
         Câu hỏi hành vi) in `F6-01` — wider coverage, and it's shared seed data across
         `interview_bank_list`/`interview_question_detail`/this screen, so all three inherit the change.
      5. **Q7 — delete semantics:** soft delete (mark retired, hide from student-facing screens, no cascade
         into past interview sessions) — matches the prototype's confirm-dialog wording exactly. Content
         create/edit/duplicate/delete all log to Nhật ký hệ thống (F1-14).
      New user story `US-A2-10` in `01-rd/req/user_stories.md`. The `interview_question_management.md` GWT
      bullet that still referenced the already-closed Q1 as open is also corrected while this file was
      being edited.
    rationale: >
      Owner instruction, following up on a proposal this session drafted from the six "Đề xuất" already
      written inline in `interview_question_management.md` Q2-Q7 — the owner's "ok" adopted that proposal
      wholesale rather than negotiating each sub-point individually. Recorded as a decision because it
      assigns a new requirement code touching 8 screen blocks, closes 6 open questions in one motion,
      introduces a not-yet-built screen slug, and changes shared seed data (topic taxonomy) used by three
      other screens — any of which crosses the RD-locking threshold `CLAUDE.md` asks to be logged for.
    source: "Owner instruction, session 2026-08-30 — proposal drafted by this session then confirmed with 'ok', answering Câu hỏi mở Q2-Q7 of interview_question_management.md"
    impacted_paths:
      - 01-rd/req/req.md
      - 01-rd/req/user_stories.md
      - 01-rd/screens/shared/SHR0301_interview_question_management.md

  - id: DEC-2026-0831-admin-overview-dashboard-stats
    kind: architecture
    status: ACTIVE
    date: 2026-08-31
    subject: "admin_overview gets one aggregate code F1-29 for its 8 uncoded stat blocks; the difficulty-chart 'AI sinh'/'Giảng viên soạn' legend is a mislabeled prototype, not a real feature"
    decision: >
      Answers Câu hỏi mở Q2 and Q5 of `01-rd/screens/admin/ADM0101_overview.md` (both Cao priority, from
      `07-review/rd_review_report_260826.html` mục 6.2). New code `F1-29` in `01-rd/req/req.md`: one
      aggregate dashboard code covering the 8 stat blocks that had no `Fx-nn` (total-submissions/active-users
      cards, submissions by language/day/month, kết quả chấm, độ khó bài toán, bài phổ biến nhất, người dùng
      mới/quay lại) — one code, not eight, mirroring how `F1-28` covers `class_progress`. Not added as a
      `FUNCTION` in the F1-10 permission matrix, since every `ADMIN` sees this page unconditionally (it is
      the post-login landing page per `auth.md` Q3, not a gated sub-screen). "Người dùng hoạt động" defined
      as logged in within the last 30 days (`[SoT: Suy luận]`, BD tunable), excluding `DEACTIVATED` accounts.
      Verdict-grouping (Q4) stays open, not bundled into this decision. Separately, the "AI sinh"/"Giảng
      viên soạn" legend on the difficulty chart is confirmed a **mislabeled prototype sample**, not a real
      feature — `req.md` only has F2-14 (AI-generated testcases, not problems), no code or `problem-bank`
      attribute exists for AI-authored problems. When the real UI is built: relabel the axis to something
      real (e.g. Accepted-rate by difficulty), no new F2 code, no new "provenance" attribute added.
    rationale: >
      Owner picked the recommended option on both sub-questions. Recorded as a decision because it assigns
      a new requirement code spanning 8 screen blocks (crosses the RD-locking threshold) and because the
      legend question, if answered the other way, would have opened a whole new AI-generated-problems
      feature — worth logging which way it went so a future session does not re-open it from the prototype.
    source: "Owner instruction via AskUserQuestion, session 2026-08-31, answering Câu hỏi mở Q2 and Q5 of admin_overview.md"
    impacted_paths:
      - 01-rd/req/req.md
      - 01-rd/screens/admin/ADM0101_overview.md
      - 06-plan/PROTOTYPE_DEBT.md

  - id: DEC-2026-0831-remove-tiered-hints-ai-config
    kind: architecture
    status: ACTIVE
    date: 2026-08-31
    subject: "'Gợi ý theo bậc' (tiered in-progress AI hints) cut from scope entirely — full prototype feature with no req.md code, would have been a third AI flow"
    decision: >
      Answers Câu hỏi mở Q1 and Q2 of `01-rd/screens/admin/ADM0301_ai_config.md` (Q1 Cao priority). "Gợi ý
      theo bậc" — a fully-built prototype AI feature (own prompt with versioning, own rate limit, own token
      budget line on `admin_ai_usage`, three UI touch-points: `admin_ai_config`, `problem_authoring`'s
      "Gợi ý theo cấp độ" tab, `problem_detail`'s "Gợi ý" tab) — has no `Fx-nn` code anywhere in `req.md`/
      `user_stories.md`. Confirmed: **cut from scope**, not a real feature. No `F5-29` or any new code
      assigned. When the real UI is built: remove the "Gợi ý theo bậc" prompt card and its rate-limit row
      from `admin_ai_config`, remove the token-budget line from `admin_ai_usage`. The dependent toggle
      "Chuyển giảng viên khi bí" (Q2, AI-to-instructor escalation) is moot as a direct consequence — no
      escalation flow exists without the feature it would escalate from.
    rationale: >
      Owner picked the recommended option: this project already carries two AI flows (F5.1 Solution Review,
      F5.2 Mock Interview); a third flow running *during* problem-solving (before submission, unlike F5.1/F5.2
      which both run after a submission) would touch `problem_detail`/Workspace and is out of proportion for
      a capstone scope. Recorded as a decision because cutting a fully-specced feature that appears live in
      three prototype screens is exactly the kind of scope-narrowing `CLAUDE.md` asks to be logged — a future
      session must not re-invent "Gợi ý theo bậc" from the prototype without knowing it was deliberately cut.
    source: "Owner instruction via AskUserQuestion, session 2026-08-31, answering Câu hỏi mở Q1 (and dependent Q2) of admin_ai_config.md"
    impacted_paths:
      - 01-rd/screens/admin/ADM0301_ai_config.md
      - 06-plan/PROTOTYPE_DEBT.md

  - id: DEC-2026-0831-remove-plagiarism-report
    kind: architecture
    status: ACTIVE
    date: 2026-08-31
    subject: "'Báo cáo nghi gian lận' (source-code plagiarism detection between accounts) cut from scope — no code, would need a source-matching algorithm outside go-judge's capability"
    decision: >
      Answers Câu hỏi mở Q1 of `01-rd/screens/admin/ADM0201_user_management.md` (Cao priority). "Báo cáo nghi
      gian lận" — appears as a real operational item in the "Cần xử lý" block of `admin_user_management` and
      as a concrete log line in `admin_system_log` ("Khóa tài khoản hmtri do trùng mã nguồn với bklinh") —
      has no `Fx-nn` code anywhere. Confirmed: **out of scope**, not a real feature, and not reduced to a
      manual-report-only surface either (the middle option offered was declined). No new code assigned. When
      the real UI is built: remove this item from the "Cần xử lý" block and the matching sample log line from
      `admin_system_log`.
    rationale: >
      Owner picked the recommended option. A source-matching plagiarism detector (string/AST comparison
      across submissions) is a separate technical module outside go-judge's scope and disproportionate to a
      capstone timeline. Recorded as a decision because this is an academic-integrity feature that reads as
      operationally real in two different screens — a future session must not assume it is in scope just
      because the prototype and a sample log line make it look real.
    source: "Owner instruction via AskUserQuestion, session 2026-08-31, answering Câu hỏi mở Q1 of admin_user_management.md"
    impacted_paths:
      - 01-rd/screens/admin/ADM0201_user_management.md
      - 06-plan/PROTOTYPE_DEBT.md

  - id: DEC-2026-0831-problem-authoring-spec-tab
    kind: architecture
    status: ACTIVE
    date: 2026-08-31
    subject: "problem_authoring gets a fifth tab 'Đặc tả' for F2-03/F2-04/F2-09 — the prototype has zero UI for function signature, matching strategy, or testcase versioning, which was blocking harness BD/DD"
    decision: >
      Answers Câu hỏi mở Q4 of `01-rd/screens/shared/SHR0202_problem_authoring.md` (Cao — Chặn BD harness). F2-03
      (function signature per language + stdin/stdout spec, mandatory input for F3's code-skeleton
      generator), F2-04 (matching strategy `EXACT`/`TRIMMED`/`EPSILON`/`UNORDERED_SET`), and F2-09 (testcase
      version) are all already-locked requirements with zero UI anywhere in the 736-line prototype (grep for
      "chữ ký"/"so khớp"/"EXACT"/"stdin"/"phiên bản": no hits). Decided shape: a **fifth tab "Đặc tả"**
      (separate from the existing "Nội dung đề" tab), containing — per language among Java/C++/Python: the
      function signature, parameter/return type schema; the stdin/stdout read/write format for the Standard
      I/O model; a matching-strategy selector. Testcase version shown at the top of the existing "Testcase"
      tab with a link to open version history (tied to Q2, already closed). Publish is blocked ("Sẵn sàng
      xuất bản" checklist gets a new condition) if this tab is empty.
    rationale: >
      Owner picked the recommended option: this data is what F3 reads to generate code — a wrong field means
      wrong generated skeletons for all three languages, so mixing it into the existing content tab risks it
      being overlooked. Recorded as a decision because it is the single most severe finding of the RD review
      (directly blocks `harness` BD/DD) and settles a UI-shape question (separate tab vs. inline field) that
      later DD work needs to build against without re-litigating.
    source: "Owner instruction via AskUserQuestion, session 2026-08-31, answering Câu hỏi mở Q4 of problem_authoring.md"
    impacted_paths:
      - 01-rd/screens/shared/SHR0202_problem_authoring.md
      - 06-plan/PROTOTYPE_DEBT.md

  - id: DEC-2026-0831-ai-instruction-injection-guard
    kind: architecture
    status: ACTIVE
    date: 2026-08-31
    subject: "F5-17 amended to cover instructor-authored per-problem AI context text — 'Chỉ dẫn cho trợ lý AI' stays a real feature but is explicitly a second-tier instruction, never a system instruction"
    decision: >
      Answers Câu hỏi mở Q6 of `01-rd/screens/shared/SHR0202_problem_authoring.md` (Cao — real prompt-injection risk,
      not just a UX question). The "Chỉ dẫn cho trợ lý AI" field (free text per problem, authored by A2/A3,
      concatenated into the `ai-review` system prompt per the prototype's own subtitle) is **kept as a real
      feature** — it improves interview/analysis quality — but F5-17 ("mã nguồn và câu trả lời người dùng là
      tham số dữ liệu, tách hoàn toàn khỏi chỉ thị hệ thống") is amended in `01-rd/req/req.md` to state this
      instructor-authored text is likewise never a system instruction: it is concatenated into the prompt
      inside its own labeled block (e.g. `<per_problem_context>`), must never override F5-17's or F5-18's
      hard constraints, and only a role holding `PROBLEM_AUTHORING:UPDATE` (F1-10 matrix) may edit it.
    rationale: >
      Owner picked the recommended option. Before this amendment, F5-17 only named "user-supplied code and
      answers" as data — it said nothing about privileged-actor-authored text concatenated into the same
      prompt, which is exactly the kind of instruction-vs-data confusion prompt injection exploits. Recorded
      as a decision, and as an amendment to a security-relevant requirement (`CLAUDE.md` OWASP top-10 rule),
      because it changes a locked requirement's text, not just a screen spec.
    source: "Owner instruction via AskUserQuestion, session 2026-08-31, answering Câu hỏi mở Q6 of problem_authoring.md"
    impacted_paths:
      - 01-rd/req/req.md
      - 01-rd/screens/shared/SHR0202_problem_authoring.md

  - id: DEC-2026-0831-admin-overview-ui-decisions
    kind: architecture
    status: ACTIVE
    date: 2026-08-31
    subject: "admin_overview: 5-verdict display instead of 3-group merge, entity search dropped, one 'View all' link added, per-block empty/loading/error states"
    decision: >
      Answers Câu hỏi mở Q3, Q4, Q6, Q8 of `01-rd/screens/admin/ADM0101_overview.md` (all Trung bình/Thấp
      priority, second round after the Cao-priority pass). Bundled into one amendment of `F1-29` in
      `01-rd/req/req.md`: (Q4) the "Kết quả chấm" widget shows all 5 real verdicts (AC/WA/TLE/CE/RE, same set
      as F1-18) instead of merging into 3 groups — keeps CE (harness bug signal) and RE (sandbox bug signal)
      distinguishable, which a merged view would hide; (Q3) the unwired "Tìm người dùng, bài toán…" search
      box is dropped from the real UI, not promised as a feature; (Q6) only "Bài phổ biến nhất" gets a "Xem
      tất cả" link (to `problem_management`), the other chart widgets stay summary-only; (Q8) the screen
      needs `empty`/`loading`/`error` states **per block**, not screen-wide — a dead `ai-review` data source
      must not take down blocks reading from other Bounded Contexts, per the AI-degrades-gracefully rule.
    rationale: >
      Owner picked the recommended option on all four. Recorded as one decision because all four amend the
      same `F1-29` requirement text added in the Cao-priority pass earlier this session — bundling avoids
      four near-identical registry entries for edits to the same paragraph.
    source: "Owner instruction via AskUserQuestion, session 2026-08-31, answering Câu hỏi mở Q3/Q4/Q6/Q8 of admin_overview.md"
    impacted_paths:
      - 01-rd/req/req.md
      - 01-rd/screens/admin/ADM0101_overview.md
      - 06-plan/PROTOTYPE_DEBT.md

  - id: DEC-2026-0831-judge-orchestration-ops-details
    kind: architecture
    status: ACTIVE
    date: 2026-08-31
    subject: "F4-10 gets basic cluster controls (pause/autoscale), F4-11 gets the 3 go-judge sandbox params spelled out, and the prototype's 'kỳ thi' (exam) concept is confirmed a priority label, not a real feature"
    decision: >
      Answers Câu hỏi mở Q1 and Q2 of `01-rd/screens/admin/ADM0401_queue_monitor.md` and Q1 of
      `01-rd/screens/admin/ADM0501_language_config.md` (all Trung bình/không xếp hạng, second round). Amends
      `01-rd/req/req.md`: (queue Q1) `F4-10` ("giám sát") is confirmed to include basic operational controls
      on the cluster — pause/resume queue consumption, toggle worker autoscaling — no new code, these are a
      natural extension of a monitoring screen that also controls what it monitors; (lang config Q1) `F4-11`
      is confirmed to include the 3 go-judge sandbox parameters (network access — off by default, child-
      process limit, whether stderr is returned to the student) — spelled out explicitly so BD/DD has a
      concrete list, no new code; (queue Q2) the "Kỳ thi"/"Ưu tiên bài thi" label (queue priority toggle) and
      "Tắt trong mọi kỳ thi" note (sandbox network toggle) are confirmed **mislabeled priority tiers**, not a
      real "exam mode" feature — no separate RD opened for exam management, relabel to something neutral
      (e.g. "Ưu tiên cao") when the real UI is built.
    rationale: >
      Owner picked the recommended option on all three. Recorded as one decision because all three concern
      the same Bounded Context (`judge-orchestration`) and the same theme (operational detail vs. a new
      scope item) — bundling keeps the registry from fragmenting into near-duplicate entries.
    source: "Owner instruction via AskUserQuestion, session 2026-08-31, answering Câu hỏi mở Q1/Q2 of admin_queue_monitor.md and Q1 of admin_language_config.md"
    impacted_paths:
      - 01-rd/req/req.md
      - 01-rd/screens/admin/ADM0401_queue_monitor.md
      - 01-rd/screens/admin/ADM0501_language_config.md

  - id: DEC-2026-0831-ai-usage-anomaly-alert
    kind: architecture
    status: ACTIVE
    date: 2026-08-31
    subject: "F5-19/F5-25 amended: per-account anomalous AI usage gets a soft admin-facing warning, not an automatic lock — distinct from the budget-exceeded auto-lock"
    decision: >
      Answers Câu hỏi mở Q1 of `01-rd/screens/admin/ADM0302_ai_usage.md`. Amends `F5-25` in `01-rd/req/req.md`:
      when one account's AI call frequency deviates abnormally from the average (prototype example: 214
      calls/24h, 7x average), the system shows a warning on `admin_ai_usage` for `ADMIN` to review — it does
      **not** auto-lock the account, which is a different mechanism from the budget-exceeded auto-lock
      already specified for F5-25. No new `Fx-nn` code; exact threshold left to BD.
    rationale: >
      Owner picked the recommended option. Recorded as a decision because it amends locked requirement text
      (`F5-25`) with a new distinguishable behavior (soft warning vs. hard lock) that a future session must
      not conflate with the existing budget-lock mechanism.
    source: "Owner instruction via AskUserQuestion, session 2026-08-31, answering Câu hỏi mở Q1 of admin_ai_usage.md"
    impacted_paths:
      - 01-rd/req/req.md
      - 01-rd/screens/admin/ADM0302_ai_usage.md

  - id: DEC-2026-0831-i18n-scope-expansion
    kind: process-policy
    status: ACTIVE
    date: 2026-08-31
    subject: "DEC-2026-0824-i18n-vi-en's bilingual-scaffold debt scope widened from 4 named Admin screens to 'every Admin and Instructor screen' — the named list had already gone stale"
    decision: >
      Answers Câu hỏi mở Q7 of `01-rd/screens/admin/ADM0101_overview.md`. `DEC-2026-0824-i18n-vi-en` named 4
      specific Admin screens still needing the `data-ui-lang`/`data-lang` bilingual scaffold. `admin_overview`
      was rebuilt after that decision and also lacks the scaffold, but was never in the named list — and
      `instructor_overview` raised the identical question independently (`01-rd/screens/teacher/
      instructor_overview.md` Q6), confirming the named-file approach goes stale every time a screen is
      rebuilt or a new one is added. **Scope widened**: the i18n-scaffold debt now applies to every screen in
      the Admin and Instructor areas, not a fixed file list — `DEC-2026-0824-i18n-vi-en` remains ACTIVE and is
      not superseded, this decision only amends its scope description.
    rationale: >
      Owner picked the recommended option. Recorded as a decision because it changes the scope of a prior
      ACTIVE decision (soft-ladder amendment, `sot-precedence.md`) rather than closing a screen-local
      question — a future session must know the named-4-file list is no longer authoritative.
    source: "Owner instruction via AskUserQuestion, session 2026-08-31, answering Câu hỏi mở Q7 of admin_overview.md"
    impacted_paths:
      - 01-rd/screens/admin/ADM0101_overview.md
      - .nexa/control/decision-registry.md

  - id: DEC-2026-0831-problem-management-lifecycle-details
    kind: architecture
    status: ACTIVE
    date: 2026-08-31
    subject: "problem_management: delete is soft (Chưa xuất bản + deleted flag), publish checklist thresholds settled (8/2/2), catalog-health thresholds settled (AC<30%/7 days), new codes F2-16 (duplicate) and F2-17 (CSV export) — CSV import of whole problems cut"
    decision: >
      Answers Câu hỏi mở Q3, Q4, Q5 of `01-rd/screens/shared/SHR0201_problem_management.md`, and Q7(b) of
      `01-rd/screens/shared/SHR0202_problem_authoring.md` (bundled — Q7(b)'s publish checklist reuses the same F2-15
      paragraph). Amends `F2-15` in `01-rd/req/req.md`: (Q3) deleting a problem is a soft action — the
      problem moves to `Chưa xuất bản` plus a separate `deleted` flag (distinguishes "not yet published" from
      "deleted"), matching the F1-16 precedent (no data loss: submissions/bookmarks/interview sessions stay);
      applies to both single and bulk delete. (Q7(b)) the "Sẵn sàng xuất bản" checklist's 4 remaining
      thresholds (after the 5th, weight-sum-100, was already dropped) are locked to the prototype's own
      numbers: ≥8 testcases, ≥2 Sample testcases, ≥2 worked examples, sample solution passing all current
      testcases — all `[SoT: Suy luận]`, BD/DD-tunable. (Q5) the "Bài cần chú ý" catalog-health block stays a
      presentation derivation, no new code, with thresholds locked to the prototype's numbers: AC rate below
      30%, unpublished for more than 7 days — both `[SoT: Suy luận]`. New codes `F2-16` (duplicate a problem,
      full content, resulting copy starts `Chưa xuất bản`) and `F2-17` (export the problem list's tabular
      data as CSV — metadata only, never problem content/spec/testcases) answer the "keep" half of Q4;
      **CSV import of whole problems is cut from scope** — a flat CSV cannot carry F2-03's per-language
      function signatures or the Markdown/LaTeX problem body.
    rationale: >
      Owner picked the recommended option on all four. Bundled into one decision because Q3, Q4, and Q7(b)
      all amend the same `F2-15` paragraph in the same session — separate entries would fragment one
      coherent edit to the problem lifecycle's requirement text.
    source: "Owner instruction via AskUserQuestion, session 2026-08-31, answering Câu hỏi mở Q3/Q4/Q5 of problem_management.md and Q7(b) of problem_authoring.md"
    impacted_paths:
      - 01-rd/req/req.md
      - 01-rd/screens/shared/SHR0201_problem_management.md
      - 01-rd/screens/shared/SHR0202_problem_authoring.md

  - id: DEC-2026-0831-problem-authoring-round2
    kind: architecture
    status: ACTIVE
    date: 2026-08-31
    subject: "problem_authoring Q5 cut for consistency with the tiered-hints removal in admin_ai_config; Q7(d) tags and Q7(e) two extra F2-10 limits kept and spelled out; Q7(g) last-edited-by kept as a plain field, no new audit trail"
    decision: >
      Answers Câu hỏi mở Q5 and the remaining Q7 sub-items (b already covered by
      `DEC-2026-0831-problem-management-lifecycle-details`; a/c/f were already settled with no new code) of
      `01-rd/screens/shared/SHR0202_problem_authoring.md`. (Q5) "Gợi ý theo cấp độ" (the per-problem authoring UI for
      tiered in-progress hints, with point-deduction labels) is **cut from scope**, recognizing it as the
      same underlying feature as "Gợi ý theo bậc" already cut in `admin_ai_config`
      (`DEC-2026-0831-remove-tiered-hints-ai-config`) — no new F2 code, remove the "Gợi ý theo cấp độ"
      block/tab when the real UI is built. (Q7(d)) the free-text "Thẻ" field is kept — amends `F2-02` (tag
      authored per problem) and `F2-11` (filter by tag), in `01-rd/req/req.md`. (Q7(e)) the "Kích thước đầu
      ra" and "Số lần nộp / giờ" fields are kept as per-problem parameters of `F2-10`, distinct from the
      system-wide defaults configured in `admin_language_config`/`F4-11`. (Q7(g)) the "Sửa lần cuối bởi X"
      line is kept as a plain `updated_by`/`updated_at` field on the problem record — not a full audit trail
      like `F1-14`, no new code needed.
    rationale: >
      Owner picked the recommended option on all four. The Q5 finding — that "Gợi ý theo cấp độ" is the same
      feature as the already-cut "Gợi ý theo bậc" viewed from the authoring screen — was surfaced during this
      session's own analysis, not from the original RD review report; recording it here so a future session
      does not reopen tiered hints from this screen without knowing the connection to the admin_ai_config cut.
    source: "Owner instruction via AskUserQuestion, session 2026-08-31, answering Câu hỏi mở Q5 and Q7(d)/(e)/(g) of problem_authoring.md"
    impacted_paths:
      - 01-rd/req/req.md
      - 01-rd/screens/shared/SHR0202_problem_authoring.md

  - id: DEC-2026-0831-class-assignments-round2
    kind: architecture
    status: ACTIVE
    date: 2026-08-31
    subject: "class_assignments Q7/Q9: assign-from-bank flow gets an inline 'Giao cho lớp này' modal instead of a page redirect; removing an assigned problem is soft (no submission history loss)"
    decision: >
      Answers Câu hỏi mở Q7 and Q9 of `01-rd/screens/teacher/INS0202_class_assignments.md`. (Q7) the "+ Gán từ ngân
      hàng bài toán" flow, currently a bare page redirect to the problem bank with no class context, becomes
      a "Giao cho lớp này" button per row on the instructor's view of the problem list, opening a class-
      picker modal inline — no page navigation. (Q9) `F2-12` in `01-rd/req/req.md` gains a **remove-from-
      class** action, actor A2, `CLASS_MANAGEMENT:UPDATE`: removing hides the problem from the class's
      assigned list going forward only — it does **not** delete the student's existing submissions/scores/
      progress for that problem in that class, deliberately unlike `F1-26`'s cascade delete when removing a
      **student** from a class (different object being removed, not the same template).
    rationale: >
      Owner picked the recommended option on both (via AskUserQuestion this session). Recorded as a decision
      because Q9 amends locked requirement text (`F2-12`) with a data-retention rule that a future session
      must not conflate with the F1-26 cascade-delete precedent for a different removal action.
    source: "Owner instruction via AskUserQuestion, session 2026-08-31, answering Câu hỏi mở Q7/Q9 of class_assignments.md"
    impacted_paths:
      - 01-rd/req/req.md
      - 01-rd/screens/teacher/INS0202_class_assignments.md

  - id: DEC-2026-0831-instructor-overview-dashboard
    kind: architecture
    status: ACTIVE
    date: 2026-08-31
    subject: "instructor_overview gets one aggregate code F1-30 and a new story US-A2-11, closing Q1-Q5/Q7 with the RD's own recommended defaults — owner delegated rather than reviewing line by line, mirroring the class_progress/F1-28 and admin_overview/F1-29 precedent"
    decision: >
      Answers Câu hỏi mở Q1 through Q5 and Q7 of `01-rd/screens/teacher/INS0101_instructor_overview.md` (Q6 already
      closed separately by `DEC-2026-0831-i18n-scope-expansion`). New code `F1-30` in `01-rd/req/req.md`
      (mirrors `F1-29` for `admin_overview`): covers the 4 stat cards, "Cần chấm tay", "Hoạt động gần đây",
      "Lớp của tôi", "Tiến độ học viên", "Bài tập của tôi" — one aggregate code, not gated as a separate
      `FUNCTION` (every `INSTRUCTOR` sees this landing page). Sub-decisions: (Q1) new story `US-A2-11` in
      `01-rd/req/user_stories.md`. (Q2) nav badges refresh on page load/navigation, no dedicated WebSocket
      channel. (Q3) "Điểm TB lớp" uses the F5-27 AI reference score, available for every `Accepted` submission
      regardless of manual grading status. (Q4) "Hoạt động gần đây" is a dedicated `identity` read model
      aggregating events from `problem-bank`/`judge-orchestration`/`ai-review` — not a reuse of `F1-14`
      (admin audit log, wrong actor and purpose). (Q5) "Tiến độ học viên" gets a "Xem tất cả" link to
      `class_progress`, matching the other two widgets. (Q7) three states — `empty`/`loading`/`error` — per
      block, same principle as `admin_overview`/F1-29.
    rationale: >
      All six sub-questions were answered by adopting the "Đề xuất" already drafted inline in
      `instructor_overview.md`'s own Câu hỏi mở table — the same self-consistent recommended-default pattern
      used throughout this session's Cao/Trung bình/Thấp closure passes, and directly analogous to the
      2026-08-30 `class_progress` precedent where the owner said "build it at a good-enough level" rather
      than negotiating every number. The confirmation round for this batch was interrupted mid-session on
      2026-08-31; owner explicitly re-confirmed all six sub-decisions (Q1-Q5, Q7) one by one via
      AskUserQuestion later the same day. Q3 ("Điểm TB lớp" uses the F5-27 AI reference score) was confirmed
      as-is after the owner raised and then set aside a separate topic — a per-problem judge scoring scheme
      capped at 1 point with partial credit by testcase pass ratio — which belongs to judge-orchestration/
      problem-bank scoring design, not to this dashboard widget; logged as a new open question, not yet
      resolved.
    source: "Session 2026-08-31 — owner confirmed all six sub-decisions individually via AskUserQuestion"
    open_questions:
      - "Judge/problem-bank scoring: owner wants each problem capped at 1 point regardless of testcase count
        (15 vs 45 testcases), with partial credit split by which testcases fail. Not yet designed — raised
        2026-08-31 while confirming instructor_overview Q3, explicitly out of scope for that widget. Needs a
        follow-up decision under F2/F4 (problem-bank / judge-orchestration) before BD/DD for submission
        scoring can be written."
    approval: >
      Content settled 2026-08-31, but the AskUserQuestion confirmation round for this batch was interrupted
      partway, so it carried a "pending owner approval" flag from 2026-08-31 to 2026-09-05. **Owner reviewed
      and approved it on 2026-09-05, unchanged** ("ok sửa nốt cho tôi", after the batch was re-presented in
      full). Numeric thresholds inside it stay `[SoT: Suy luận]` — BD/DD may still tune them without
      reopening this decision.
    impacted_paths:
      - 01-rd/req/F1_identity.md
      - 01-rd/req/user_stories/a2_instructor.md
      - 01-rd/screens/teacher/INS0101_instructor_overview.md
      - 01-rd/overview/system_survey.md
      - 07-review/rd_review_closure_260831.md

  - id: DEC-2026-0831-instructor-grading-round2
    kind: architecture
    status: SUPERSEDED
    superseded_by: DEC-2026-1013-remove-instructor-manual-grading
    date: 2026-08-31
    subject: "instructor_grading Q2-Q5 closed with F5-27 amendments: pre-filtered queue at AI score <6/10, AI+manual scores shown side by side, 7-day windows for the two time-based stat cards, a class filter added"
    decision: >
      Answers Câu hỏi mở Q2 through Q5 of `01-rd/screens/teacher/INS0301_grading.md`. Amends `F5-27` in
      `01-rd/req/req.md`: (Q2) the screen is a **pre-filtered queue**, not the full submission list — only
      `Accepted` submissions with an AI reference score **below 6/10** appear, `[SoT: Suy luận]`, BD/DD-
      tunable. (Q3) after manual grading, both scores show side by side (e.g. "AI: 4/10 · GV: 7/10") instead
      of the AI score being overwritten in the display — extends `US-A2-06`'s "kept alongside, not
      overwritten" rule to the display layer, not just storage. (Q4) the 4 header stat cards get formulas, no
      new code: "Chờ chấm" = count meeting the Q2 filter not yet manually graded; "Đã chấm tuần này" and
      "Điểm TB sau chấm" both use a **rolling 7-day window**, not the calendar week. (Q5) a class-filter
      dropdown/tab is added, matching `class_progress`'s pattern, for instructors with multiple classes.
    rationale: >
      Same recommended-default pattern as `DEC-2026-0831-instructor-overview-dashboard` — this batch's
      per-question confirmation round was also interrupted mid-session on 2026-08-31; owner explicitly
      re-confirmed all four sub-decisions (Q2-Q5) one by one via AskUserQuestion later the same day.
    source: "Session 2026-08-31 — owner confirmed all four sub-decisions individually via AskUserQuestion"
    approval: >
      Same interruption as `DEC-2026-0831-instructor-overview-dashboard`: content settled 2026-08-31, the
      confirmation round cut off partway, flag carried until **owner reviewed and approved on 2026-09-05,
      unchanged**. The AI-score threshold (<6/10) filtering the manual-grading queue and the 7-day stat
      window stay `[SoT: Suy luận]` — tunable at BD/DD without reopening this decision.
    impacted_paths:
      - 01-rd/req/F5_ai-review.md
      - 01-rd/screens/teacher/INS0301_grading.md
      - 01-rd/overview/system_survey.md
      - 07-review/rd_review_closure_260831.md

  - id: DEC-2026-0831-partial-score-testcase-ratio
    kind: architecture
    status: ACTIVE
    date: 2026-08-31
    subject: "New F4-13: per-submission score = testcases passed / total, capped at 1 point per problem, shown to the learner in submission_result alongside the unchanged binary Accepted/Wrong Answer verdict — requires dropping fail-fast (F4-04) to always run every testcase; partially reverses the 2026-08-24 mục 2.6 phần 1 decision"
    decision: >
      Amends `01-rd/req/req.md` F4: `F4-04` (fail-fast) is retired — `F4-03` now always calls
      `JudgeExecutionPort` for every testcase of a submission, never stopping early, so the pass ratio is
      always known. New code `F4-13`: score = (count of `AC` testcases) / (total testcases), displayed on a
      **1-point-per-problem scale** regardless of how many testcases the problem has (15 or 45 both reduce
      to the same /1 scale), shown to the learner as a sixth stat on `submission_result` next to the verdict
      banner, computed per submission. Does **not** replace or redefine the binary `Accepted`/`Wrong Answer`
      verdict — `Accepted` still requires passing 100% of testcases and still gates F5.1/F5.2, `F1-07`
      (acceptance rate), and the `problem_management` "Bài cần chú ý" threshold (AC < 30%). `F1-06` (personal
      progress, problems solved by topic) amended to also surface the learner's **best** `F4-13` score per
      problem — the maximum across all of that student's submission attempts for the problem, not the
      latest attempt. `F5-27` (AI reference score, instructor-facing, 0-10 scale) is unaffected and
      unrelated — two independent scores, never converted into each other.
    rationale: >
      Owner-requested reversal, confirmed explicitly after two rounds of clarification (2026-08-31): (1)
      scope — shown to the learner, not internal-only; (2) fail-fast trade-off — accepted running every
      testcase (go-judge itself is unaffected, each testcase call is already an independent sandboxed
      invocation; the cost is orchestration-level: more RabbitMQ load and longer per-submission wall time
      under concurrent load, bounded per-submission by `per-problem time limit × testcase count` from `F2`;
      acceptable at capstone-project scale, `[SoT: Suy luận]` — no production telemetry exists yet); (3)
      best-attempt aggregation, not latest-attempt; (4) the binary verdict and everything downstream of it
      (F5 trigger, F1-07, AC-rate threshold) stays exactly as-is — the new score is additive, not a
      replacement, keeping blast radius contained to `submission_result` display and `F1-06`.

      This **partially reverses** `06-plan/PROTOTYPE_DEBT.md` mục 2.6 phần 1 (2026-08-24, reaffirmed
      2026-08-31 alongside `problem_authoring` Q3): that decision rejected "chấm điểm từng phần theo trọng
      số do người soạn đề khai báo" (author-declared per-testcase weights, with a weight column and a
      "total weight = 100" publish-checklist rule in the `problem_authoring` UI) specifically because it
      conflicted with fail-fast. This decision reopens only the fail-fast half of that conflict — the score
      here is a **uniform, automatically-computed** ratio (every testcase counts equally, 1/N), requiring no
      author input. The `problem_authoring` decision to omit a weight column / "Điểm" column from the
      authoring UI (`DEC-2026-0831-problem-authoring-round2`'s sibling closure, Q3 of
      `01-rd/screens/shared/SHR0202_problem_authoring.md`) stays correct and is **not** reversed — there is nothing
      for an author to configure under this mechanism.
    source: "Owner instruction via AskUserQuestion, session 2026-08-31, after two clarification rounds (scope/fail-fast trade-off/best-attempt) triggered while confirming DEC-2026-0831-instructor-overview-dashboard Q3"
    impacted_paths:
      - 01-rd/req/req.md
      - 01-rd/screens/users/USR0201_submission_result.md
      - 01-rd/overview/system_survey.md
      - 06-plan/PROTOTYPE_DEBT.md

  - id: DEC-2026-0831-outside-screens-closures
    kind: architecture
    status: ACTIVE
    date: 2026-08-31
    subject: "Three genuinely open cross-cutting questions closed: no re-edit after submitting a Chế độ luyện answer (F6-07), Solution Review stays opt-in not auto-triggered (F5-01), F4-07 timeout sweep threshold set to 5 minutes — plus two stale report entries (email notifications, harness Strategy/Plugin architecture) found already answered"
    decision: >
      Answers Câu hỏi mở Q1, Q3, Q4 of `01-rd/req/user_stories.md` mục 5 (Q2 already moot). (Q1) amends
      `F6-07` in `01-rd/req/req.md`: a Chế độ luyện answer cannot be edited after submission — each submit is
      an independent attempt, retrying means submitting a new attempt; keeps full history for F6-09/F6-10
      progress tracking. Same question also closes `01-rd/screens/users/USR0402_interview_question_detail.md` Q2,
      which was the identical question asked at screen level. (Q3) amends `F5-01`: requesting Solution
      Review stays an opt-in action — the system only shows the option/button after `Accepted`, never
      auto-runs, saving AI cost and matching the `submission_result` prototype's own button-driven behavior.
      (Q4) amends `F4-07`: the "treo" (stuck) threshold for timeout sweep is **5 minutes** `[SoT: Suy luận]`,
      BD/DD-tunable.
      Separately, this session found two more items from `07-review/rd_review_report_260826.html` mục 6.5
      were **already answered before the report was written**, not genuinely open: email notifications
      (`01-rd/overview/system_survey.md` mục 8.2 said "chưa quyết", but `F1-21` — added 2026-08-25 — already
      puts email notifications in scope) and the `harness` Strategy/Plugin architecture confirmation
      (`02-bd/packages/harness/architecture.md` mục 2b, written 2026-08-24, already confirms the strategy is
      unchanged). Both corrected to point at their existing answers rather than re-asking the owner.
    rationale: >
      Owner picked the recommended option on all three real questions (via AskUserQuestion). The two stale
      report entries are recorded here rather than as separate decisions since no new judgment call was
      made — only a citation correction pointing at answers that already existed. Bundling all five into one
      decision because they share the same "closing the RD-review report's outside-screens/ backlog" origin
      and touch only cross-cutting requirement text, not a single screen.
    source: "Owner instruction via AskUserQuestion, session 2026-08-31, answering Câu hỏi mở Q1/Q3/Q4 of user_stories.md mục 5; the other two items were found already answered during the same pass, not re-asked"
    impacted_paths:
      - 01-rd/req/req.md
      - 01-rd/req/user_stories.md
      - 01-rd/overview/system_survey.md
      - 01-rd/screens/users/USR0402_interview_question_detail.md

  - id: DEC-2026-0831-rd-req-split-by-module
    kind: process-policy
    status: ACTIVE
    date: 2026-08-31
    subject: "req.md (764 lines) split into 7 per-Bounded-Context files, user_stories.md (517 lines) split into 5 per-actor files; both originals kept as thin index files; downstream citations converted from line numbers to Fx-nn/US-Axx-nn code anchors"
    decision: >
      `01-rd/req/req.md` split by Bounded Context into `01-rd/req/F1_identity.md` (F1), `F2_problem-bank.md` (F2),
      `F3_harness.md` (F3), `F4_judge-orchestration.md` (F4), `F5_ai-review.md` (F5), `F6_interview-bank.md` (F6), and
      `nfr.md` (cross-cutting non-functional requirements). `01-rd/req/req.md` itself becomes a thin index —
      one-line framing intro plus a pointer table (module code, Bounded Context, file path, one-line scope),
      no `Fx-nn` content duplicated.
      `01-rd/req/user_stories.md` (517 lines) split by actor into `01-rd/req/user_stories/a1_student.md`,
      `a2_instructor.md`, `a3_admin.md`, `a4_system.md`, and `open_questions.md`. `user_stories.md` itself
      becomes a thin index — intro/legend plus a pointer table (actor, file path, story-ID range), no story
      content duplicated.
      Every downstream citation across `01-rd/screens/**`, `02-bd/packages/harness/architecture.md`,
      `05-coding/frontend/README.md`, and the one live citation in this registry (this entry) that pointed at
      `01-rd/req/req.md:<line>` or `01-rd/req/user_stories.md:<line>` was rewritten to point at the correct
      new file, anchored by the `Fx-nn`/`US-Axx-nn` code already named next to the citation rather than a
      line number.
    rationale: >
      Owner found the flat 764-line and 517-line files too long to navigate for day-to-day citation work.
      This mirrors the Bounded-Context split already used for `02-bd/` and `03-dd/` (`CLAUDE.md` "Two
      documentation axes"), so the RD layer now follows the same shape as BD/DD. Line-number citations go
      stale on every edit to the cited file; `Fx-nn`/`US-Axx-nn` code anchors do not, because the code is a
      permanent identifier of the requirement itself, not of its position in a file. Six files were
      explicitly excluded from the citation rewrite because they are illustrative templates or frozen
      historical snapshots, not live citations: `.claude/commands/brutal-review.md`,
      `.claude/skills/rri/references/rri-report.md`, `.claude/skills/tdd-generation/SKILL.md`,
      `06-plan/reports/260825-2100-ai2-solution-review-conflicts.md`,
      `07-review/rd_review_report_260826.html`, `07-review/rd_review_closure_260831.html`.
      `06-plan/PROTOTYPE_DEBT.md` retains its own line-number citations to the pre-split `req.md` as a
      historical planning log and was intentionally left untouched — it documents what was true at the time
      each entry was written, not a live cross-reference.
    source: "Owner instruction, session 2026-08-31, RD readability cleanup"
    impacted_paths:
      - 01-rd/req/req.md
      - 01-rd/req/F1_identity.md
      - 01-rd/req/F2_problem-bank.md
      - 01-rd/req/F3_harness.md
      - 01-rd/req/F4_judge-orchestration.md
      - 01-rd/req/F5_ai-review.md
      - 01-rd/req/F6_interview-bank.md
      - 01-rd/req/nfr.md
      - 01-rd/req/user_stories.md
      - 01-rd/req/user_stories/a1_student.md
      - 01-rd/req/user_stories/a2_instructor.md
      - 01-rd/req/user_stories/a3_admin.md
      - 01-rd/req/user_stories/a4_system.md
      - 01-rd/req/user_stories/open_questions.md
      - 01-rd/screens/
      - 02-bd/packages/harness/architecture.md
      - 05-coding/frontend/README.md

  - id: DEC-2026-0901-backend-base-architecture
    kind: architecture
    status: ACTIVE
    date: 2026-09-01
    subject: "Backend base scaffold: ArchUnit (not Spring Modulith) as the boundary-test tool, all 8 Maven modules declared up front, JaCoCo 80% configured now but enforced per module on first business class, Spring Boot 4.0.8 pinned, virtual threads on"
    decision: >
      Five choices made while scaffolding `05-coding/backend/`, each expensive to reverse once six business
      modules are built on top. Plan: `06-plan/nexa-plan/260831-2323-backend-base-scaffold.md`.
      (1) BOUNDARY-TEST TOOL — **ArchUnit only** (`archunit-junit5` 1.5.0, test scope, in
      `algoprep-bootstrap`). Spring Modulith is NOT used. `01-rd/system/environment.md` section 6 left this
      open ("decide when building `algoprep-bootstrap`"); this entry closes it. The suite must fail the
      build, not warn, and covers the three module laws (`backend_architecture.md` section 2.A) plus the
      four layer laws (section 3.A) plus the package law that module A's `presentation`/`infrastructure`
      never appears in module B's imports.
      (2) MODULE INSTANTIATION — all **eight** Maven modules are declared in the parent POM immediately.
      The five business modules whose slice has not started carry only a `pom.xml` and one
      `package-info.java` per layer, with zero business classes. This KNOWINGLY DIVERGES from
      `01-rd/system/codebase_structure.md` section 2 ("a module only gets created when its slice arrives; an
      empty module in `pom.xml` is debt, not progress") — see rationale.
      (3) JACOCO PHASE-IN — the 80% line-coverage rule required by `environment.md` section 3.A is
      configured now with `includes` restricted to `**/domain/**` and `**/application/**`, but
      `haltOnFailure` is switched on per module at the moment that module gets its first business class. At
      base there are no classes in either layer, so the rule would either pass vacuously or fail the build
      on an empty scaffold. This is a phase-in of enforcement, NOT a lowered threshold; the threshold stays
      80%.
      (4) VERSIONS — `spring-boot-starter-parent` **4.0.8** (highest patch on the 4.0.x line pinned by
      `DEC-2026-0820-stack-versions`; 4.1.x and 4.2.0-M1 exist on Maven Central and are deliberately NOT
      taken). Compile `<release>21</release>`. Toolchain versions verified against Maven Central on
      2026-09-01: ArchUnit 1.5.0, Spotless 3.10.1 with google-java-format 1.36.1, maven-checkstyle-plugin
      3.6.0 with Checkstyle 14.1.0, spotbugs-maven-plugin 4.10.4.0 with findsecbugs-plugin 1.14.0, JaCoCo
      0.8.15, maven-enforcer-plugin 3.6.3.
      (5) VIRTUAL THREADS ON AT BASE — `spring.threads.virtual.enabled=true` in the base
      `application.yml`, plus a test asserting a request handler actually runs on a virtual thread. Not
      deferred to a later slice.
    rationale: >
      (1) ArchUnit over Spring Modulith: four of the seven laws to enforce are PACKAGE-level laws INSIDE one
      module (`domain/` imports no Spring/JPA/Jackson; outbound ports in `domain/ports/out` and inbound in
      `application/ports/in`; `presentation/` never calls `infrastructure/`; `application/` knows no HTTP).
      Spring Modulith only understands BETWEEN-module boundaries under its own `api`/`internal` package
      convention, which cannot express those four and whose conventions clash with the settled `algoprep-*`
      layout. ArchUnit expresses all seven and is a test-scope dependency with no runtime footprint. Using
      both would pay twice for one job.
      (2) All eight modules now: the parent POM, the dependency graph, and the ArchUnit suite are precisely
      the three things this base exists to lock down. Declaring modules piecemeal means editing the parent
      POM five more times, and — worse — the boundary suite would only get exercised once a real cross-module
      import exists, which is exactly when it is too late. The cost of a not-yet-started module is one
      `pom.xml` plus four `package-info.java` files and no compiled business code. The divergence from
      `codebase_structure.md` is recorded here rather than left silent because that document is P2 in
      `sot-precedence.md` and this entry is P1; `codebase_structure.md` is updated in the same slice so the
      two do not drift.
      (3) JaCoCo phase-in: `haltOnFailure` on an empty `domain`/`application` either reports 100% of zero
      (a number that means nothing and hides real regressions later) or reds the build for a scaffold that
      is correct. Configuring the rule now and arming it per module keeps the 80% bar honest and visible in
      the POM from day one instead of being "added later" and forgotten.
      (4) 4.0.8 not 4.1.x: `DEC-2026-0820-stack-versions` pins the line, not the patch, and its stated fear
      was a later session scaffolding on the wrong major. Taking the highest 4.0.x patch honours the pin and
      gets the security fixes; jumping to 4.1.x would silently widen a decision the owner made explicitly.
      (5) Virtual threads at base: they are the SOLE reason Java 21 was chosen over 17
      (`DEC-2026-0820-stack-versions` rationale; `01-rd/overview/overview.md` section 1.D), and
      `01-rd/req/nfr.md` section A makes blocking I/O the system's whole performance story — synchronous
      go-judge calls, one WebSocket per grading submission, one SSE stream per interview turn. Omitting the
      flag leaves the app running and `mvn verify` green while the entire stack decision is silently void
      until someone runs k6 and cannot explain the numbers. That failure mode is why it gets an asserting
      test rather than just a YAML line.
    source: "Owner instruction, session 2026-09-01 ('ok triển khai base BE theo plan') · plan 06-plan/nexa-plan/260831-2323-backend-base-scaffold.md · versions verified against repo1.maven.org 2026-09-01"
    impacted_paths:
      - 05-coding/backend/
      - 01-rd/system/codebase_structure.md
      - 01-rd/system/environment.md
  - id: DEC-2026-0901-bd-layout-by-concern
    kind: process-policy
    status: ACTIVE
    date: 2026-09-01
    subject: "02-bd/ is laid out BY CONCERN (architecture/database/security/storage), not by Bounded Context directory; the on-disk packages/ commons/ jobs/ system-docs/ shape is retired"
    decision: >
      `02-bd/` follows the shape already documented in `CLAUDE.md` and
      `01-rd/system/codebase_structure.md` section 4: **one directory per concern, one file per Bounded
      Context inside it** — `02-bd/{architecture,database,security,storage}/<context>.md`, plus
      `02-bd/screens/<area>/<slug>.md` for the screen axis. The competing on-disk layout
      (`02-bd/packages/<context>/<concern>.md` plus `commons/`, `jobs/`, `system-docs/`) is retired; those
      four directories are removed and the one file that existed,
      `02-bd/packages/harness/architecture.md`, moves to `02-bd/architecture/F3_harness.md`. Downstream
      citations updated in `01-rd/req/F3_harness.md`, `01-rd/screens/users/USR0102_problem_detail.md`, and
      `05-coding/judge-engine/README.md`. `03-dd/` keeps the matching shape:
      `03-dd/{api,logic,validation,jobs}/<context>.md`.
      Owner decision, session 2026-09-01, asked before the first `identity` BD file was written.
    rationale: >
      The divergence was found while planning the backend base and had to be closed BEFORE the first BD
      file, because it decides the path of 24 files (6 contexts x 4 concerns) and moving them later breaks
      every citation pointing at them. Three reasons for choosing the documented shape over the on-disk one:
      (1) `CLAUDE.md`, `codebase_structure.md` section 4, and the `bd-generation` / `dd-generation` skills
      all already specify it, so the alternative would mean editing agent config and two skills to match a
      directory tree that was never decided; (2) BD is read BY CONCERN in practice — designing the database
      means opening every context's schema together, and the by-context layout scatters those six files
      across six directories; (3) it keeps `02-bd/` and `03-dd/` the same shape, which is what makes the
      anti-drift rule in `codebase_structure.md` section 3 checkable by a single grep across all four
      documentation layers. Note this is NOT the same axis as the RD split
      (`DEC-2026-0831-rd-req-split-by-module`): that split moved RD to one FILE per context in one
      directory, which is a different structure from a directory per context.
    source: "Owner decision, session 2026-09-01 · CLAUDE.md Directory map · 01-rd/system/codebase_structure.md section 4"
    impacted_paths:
      - 02-bd/
      - 01-rd/req/F3_harness.md
      - 01-rd/screens/users/USR0102_problem_detail.md
      - 05-coding/judge-engine/README.md
  - id: DEC-2026-0903-sot-citation-scope
    kind: process-policy
    status: ACTIVE
    date: 2026-09-03
    subject: "Deliverable docs MAY name a DEC by id, but MUST NOT cite .claude/ or .nexa/ by path plus line number"
    decision: >
      Narrows the blanket rule written in `01-rd/README.md` section 4 on 2026-09-01 ("documents in `01-rd/`
      through `04-tdd/` must not cite `.claude/` or `.nexa/` at all"). The rule now has two tiers:
      **allowed** — naming a decision by its identifier, e.g. "chốt theo `DEC-2026-0828-remove-rejudge-scope`",
      with no path and no line number; **banned** — any `.claude/**` or `.nexa/**` citation written as a
      path, and especially one carrying line numbers such as
      `[SoT: .nexa/control/decision-registry.md:281-285]`. Exception unchanged: a document may show those
      paths when it is **describing the repo directory tree**, as `01-rd/system/codebase_structure.md`
      section 1 does. Applies to `01-rd/` through `04-tdd/`, `06-plan/`, `07-review/` and the root
      `README.md`.
    rationale: >
      The 2026-09-01 blanket ban was written while removing two `.claude/` citations, and was never applied
      to the ~30 RD files holding 50+ `.nexa/` citations — so the layer's own rule was broken repo-wide the
      day after it was written, which is worse than having no rule. Three reasons for the two-tier version
      over enforcing the ban: (1) the decision registry is tier **P1** of the SoT ladder
      (`.nexa/control/sot-precedence.md`), directly above RD itself, so forbidding RD from referring to it
      at all would cut a document off from the layer that overrides it; (2) what actually breaks for a
      report reader is a **path they cannot open plus a line number that has already drifted** — a bare
      `DEC-` id reads as a decision reference and stays valid when the registry is reordered, exactly the
      same argument that moved RD citations from line numbers to `Fx-nn` anchors in
      `DEC-2026-0831-rd-req-split-by-module`; (3) rewriting 50+ citations to point at deliverable-layer
      sources would either lose the decision trail or duplicate each decision's text into RD, and duplicated
      text is the drift this repo keeps paying for.
      Owner instruction, session 2026-09-03: "theo phương án bạn thấy phù hợp xử lý cho tôi", after the
      option was put as (A) relax to id-only vs (B) strip all 50+ citations, with (A) recommended.
    source: "Owner instruction, session 2026-09-03 · plan 06-plan/nexa-plan/260903-2031-rd-drift-repair.md · rule text at 01-rd/README.md section 4"
    impacted_paths:
      - 01-rd/README.md
      - 01-rd/
      - 07-review/
  - id: DEC-2026-0908-diagram-per-actor-subfolders
    kind: process-policy
    status: ACTIVE
    date: 2026-09-08
    subject: "08-diagram/ usecase/ and sequence/ go back to per-actor subfolders (admin/ gv/ user/ shared/), reversing the 2026-09-07 flatten"
    decision: >
      `08-diagram/01-rd/req/usecase/` and `08-diagram/01-rd/req/sequence/` are split into per-actor
      subfolders again: `admin/`, `gv/`, `user/`, and (for use case only) `shared/` for diagrams genuinely
      mounted for more than one actor. A diagram's overview file with no single owning actor
      (`usecase_tong_quan_actor.*`) stays directly under `usecase/`, not inside any actor subfolder. This
      reverses the flatten described in `drawio-uml-architect/SKILL.md` "Naming and location" as
      "Established 2026-09-07 on owner instruction" — that flatten itself was never entered in this
      registry, only documented inline in the skill file, which is why this reversal needed its own entry
      to be findable. `drawio-uml-architect/SKILL.md` "Naming and location" is updated in the same pass so
      the skill does not silently re-flatten the next time it runs. `CLAUDE.md`'s own `08-diagram/` row is
      generic ("mirroring the path of the document each one serves") and does not specify flat vs.
      per-actor, so it needed no edit — checked, not assumed.
      Owner instruction, session 2026-09-08: "đúng ý tôi là như thế chia sâu folder theo actor", given
      directly after being shown the conflict with the already-staged flatten rename.
    rationale: >
      No independent reason is recorded beyond the owner's direct instruction — the 2026-09-07 flatten and
      this 2026-09-08 reversal are both owner calls on the same axis (findability of "all sequence
      diagrams" vs. findability of "all diagrams for one actor"), not a correctness fix. Recorded as a
      decision anyway because it is the second flip on this exact question in two days: the next session
      needs one place to check current direction instead of trusting whichever skill-file prose it reads
      first, and because it reverses a git rename that was already staged in the working tree when the
      owner asked for this.
    source: "Owner instruction, session 2026-09-08"
    impacted_paths:
      - 08-diagram/01-rd/req/usecase/
      - 08-diagram/01-rd/req/sequence/
      - .claude/skills/drawio-uml-architect/SKILL.md
  - id: DEC-2026-0912-judge-callback-contract
    kind: architecture
    status: ACTIVE
    date: 2026-09-12
    subject: "judge-orchestration BD locks the submission state machine, the port-neutral idempotency key, and the timeout-sweep parameters that the Judge0 callback contract requires"
    decision: >
      Answers the mandatory "Judge0 callback contract (F4)" question of `sot-precedence.md` section 5 and
      `bd-generation/SKILL.md`. Four points, binding on `02-bd/architecture/F4_judge-orchestration.md` and
      `02-bd/database/F4_judge-orchestration.md`:
      (1) STATE MACHINE — a submission moves through non-terminal states `PENDING` -> `COMPILING` ->
      `JUDGING`, then into exactly one of seven terminal states that are never overwritten once reached:
      `ACCEPTED`, `WRONG_ANSWER`, `TIME_LIMIT_EXCEEDED`, `RUNTIME_ERROR`, `COMPILE_ERROR`, `SYSTEM_ERROR`
      (worker crash, sandbox fault, or timeout-sweep write). `COMPILE_ERROR` is decided once, before the
      testcase loop starts; the other five judged terminals are decided only after all N testcases have run
      (`DEC-2026-0831-partial-score-testcase-ratio` — no early exit), with the submission-level verdict
      taken from the first failing testcase in declared order when not all pass.
      (2) IDEMPOTENCY — `judge_run_tokens` is the port-neutral correlation/idempotency key: one row is
      issued per `JudgeExecutionPort.run()` attempt for a given `(submission_id, testcase_id)` pair, marked
      `consumed_at` the moment a result is durably written in the same transaction. Before a worker (re)calls
      the port for a testcase — including after a RabbitMQ redelivery — it checks for an already-consumed
      token for that pair and skips the call if found, so a redelivered message can never overwrite a
      recorded result. `callback_logs` is adapter-internal bookkeeping, populated only when an asynchronous
      adapter (Judge0Adapter) is active; it stays empty under the synchronous go-judge default.
      (3) TIMEOUT SWEEP — the reconciliation job scans submissions in a non-terminal state whose
      `updated_at` is older than 5 minutes (F4-07, `DEC-2026-0831-outside-screens-closures`), polling every
      60 seconds `[SoT: Suy luận]`, and moves each match to `SYSTEM_ERROR` with `failure_reason =
      WORKER_TIMEOUT` via a conditional UPDATE guarded by `WHERE status NOT IN (<terminal states>)` — never
      a blind overwrite. With the synchronous go-judge adapter this is almost always a crashed worker, not a
      lost external callback (`dependency-map.md` section 3).
      (4) WEBHOOK AUTH — stays entirely inside `Judge0Adapter`'s own controller and token scheme; the
      outbound port `JudgeExecutionPort` never carries webhook-shaped parameters, so re-plugging Judge0
      later touches only the adapter, not the port contract or its callers.
    rationale: >
      The RD (`01-rd/req/F4_judge-orchestration.md` F4-05 through F4-07) intentionally left the callback
      contract loose because the synchronous go-judge default has no callback at all — but the port must
      still be designed so an asynchronous adapter can be re-plugged without a port-signature change, per
      `DEC-2026-0823-go-judge-default-engine`. Deciding this now, once, in the BD is cheaper than discovering
      a state-machine gap mid-build when `harness` + `judge-orchestration` are built together (build stage 2,
      `dependency-map.md` section 4) and in-flight submissions already exist.
    source: "02-bd/architecture/F4_judge-orchestration.md section on the callback contract, session 2026-09-12"
    impacted_paths:
      - 02-bd/architecture/F4_judge-orchestration.md
      - 02-bd/database/F4_judge-orchestration.md
      - 03-dd/logic/F4_judge-orchestration.md
  - id: DEC-2026-0912-judge-outbox-pattern
    kind: architecture
    status: ACTIVE
    date: 2026-09-12
    subject: "judge-orchestration publishes SubmissionAccepted/SubmissionGraded via a Transactional Outbox (outbox_events table + polling relay), not @TransactionalEventListener, to survive a broker outage after commit"
    decision: >
      Closes the open item in `dependency-map.md` section 1/"Open items": whether the F4-to-F5 (and F4-to-F1)
      domain-event hand-off is guaranteed by a Transactional Outbox. **Chosen: yes.** `judge-orchestration`
      writes a row to its own `outbox_events` table (schema `judge`) in the SAME database transaction that
      moves a submission to a terminal state; a separate polling relay (a `@Scheduled` component, not tied to
      the request thread) reads unpublished rows in order, publishes `SubmissionAccepted` (on `ACCEPTED`) and
      `SubmissionGraded` (on every terminal state, carrying the F4-13 pass ratio) to a RabbitMQ topic
      exchange, and marks `published_at` only after the broker acknowledges the publish. `identity` consumes
      both to update `user_problem_best_score` / `user_submission_stats` / `identity_recent_activity`
      (`02-bd/architecture/F1_identity.md` section 3.1); `ai-review` consumes `SubmissionAccepted` to trigger
      F5.1/F5.2 eligibility.
      Rejected alternative: Spring's `@TransactionalEventListener(phase = AFTER_COMMIT)` publishing straight
      to RabbitMQ. It guarantees the listener only runs after the database commit succeeds, but it does
      **not** guarantee the publish itself succeeds — if RabbitMQ is unreachable at that instant, the event
      is dropped with no durable record and no retry, exactly the failure this decision exists to close. The
      Outbox table is itself part of the same transaction as the state change, so "submission reached a
      terminal state" and "event exists to be published" can never diverge, and the relay can retry
      indefinitely against a durable row instead of a lost in-memory callback.
    rationale: >
      Losing the `SubmissionAccepted` trigger silently breaks the AI path (F5) exactly during the window
      it matters most — right after a learner's first Accepted submission — while `identity`'s read models
      (F1-06/F1-07) would quietly go stale with no visible error. `CLAUDE.md`'s graceful-degradation rule
      protects F1-F4 from an AI outage, but says nothing about protecting F5's trigger from an F4-side broker
      outage; this decision closes that gap on the publishing side. The relay is intentionally a poller
      rather than a blocking call inside the grading transaction, so a slow or down broker never lengthens
      the synchronous per-testcase judging path itself.
    source: "02-bd/architecture/F4_judge-orchestration.md section on the F4-to-F5 hand-off, session 2026-09-12"
    impacted_paths:
      - 02-bd/architecture/F4_judge-orchestration.md
      - 02-bd/database/F4_judge-orchestration.md
      - 02-bd/architecture/F1_identity.md
  - id: DEC-2026-0915-admin-separate-login-route
    kind: architecture
    status: ACTIVE
    date: 2026-09-15
    subject: "Admin gets its own /admin/login route and screen, separate from the shared /login auth screen"
    decision: >
      Diverges from `02-bd/screens/shared/SHR0101_auth.md:106` ("1 màn `auth` dùng chung cho cả 3 vai trò,
      chỉ redirect khác nhau sau đăng nhập"). Owner asked explicitly for a dedicated admin login
      entry point instead of a styling-only change. `/admin/login` (route group `(public)`, so it
      does NOT inherit the `(admin)` group's `AppShell`/sidebar) gets its own view
      (`views/admin-auth`), login-only — no signup, no OAuth buttons, no marketing aside panel —
      styled with the Admin liquid-glass palette (`.admin-shell` CSS scope, `LiquidGlassBackdrop`,
      cyan/teal tokens from `09-layoutBase/Admin - Tổng quan.dc.html`) instead of the shared
      screen's neutral glass style. It calls the exact same `login` mutation and `loginSchema` as
      the shared screen (`entities/auth`) — only the front door and its skin change, not the
      backend contract or the mock. The shared `/login` + `/register` screen is untouched and keeps
      serving student/instructor/public signup+login exactly as before.
    rationale: >
      [SoT: Suy luận] — no RD/BD line asks for this split; it is a UX/security-posture preference
      (privileged-account login kept off the same screen as public self-service signup, plus a
      distinct visual identity matching the Admin area's own liquid-glass shell) that the owner
      confirmed over the styling-only alternative when asked directly. Recorded as a decision
      because it changes routing/screen architecture, not styling, per `CLAUDE.md` "Rules" (any
      architecture divergence from BD needs a decision entry). Does not reopen
      `02-bd/architecture/F1_identity.md`'s actual auth contract — the login endpoint/schema is
      unchanged, only which front-end route reaches it.
    source: "chat request 2026-09-15, confirmed via AskUserQuestion (chose 'Tạo route /admin/login tách biệt hẳn' over the styling-only option)"
    impacted_paths:
      - 02-bd/screens/shared/SHR0101_auth.md
      - 05-coding/frontend/src/app/(public)/admin/login/page.tsx
      - 05-coding/frontend/src/views/admin-auth
      - 05-coding/frontend/src/features/auth-by-credentials/ui/auth-form.tsx
      - 05-coding/frontend/src/shared/ui/theme-lang-switcher.tsx

  - id: DEC-2026-0921-admin-shell-footer
    kind: architecture
    status: ACTIVE
    date: 2026-09-21
    subject: "The shared Admin shell gets a real footer, applied to every /admin/* route including admin_overview"
    decision: >
      Closes Q4 of `02-bd/screens/admin/_shell.md`. The Admin shell renders a footer on every
      `/admin/*` route: product name plus version, a service-status line with a status dot, and four
      secondary links. It sits as the last element INSIDE `<main>`, within the same 1320px container
      as the content — not a sibling of `<aside>`/`<main>` and not pinned to the viewport bottom,
      matching where `<footer>` actually sits in the prototypes. The four links have no real
      destination yet (`href="#"` everywhere in the prototypes) so they render as plain text, the
      same decorative treatment already applied to the three toolbar icons. Deliberate divergence:
      `admin_overview`'s own prototype has no footer at all (the file ends right after `</main>`),
      and it gets one anyway, because one consistent shell across the whole Admin area beats matching
      a single screen's incomplete prototype.
    rationale: >
      Q4 had been left open since the shell BD was written on 2026-09-17, and three screen BDs
      (`ADM0201`, `ADM0302`, `ADM0501`) had meanwhile been written assuming the footer existed —
      `ADM0501` even designed a leave-confirmation flow around clicking a footer link, and `ADM0302`
      described a refresh cadence as matching "the status line in the footer". Two other files
      (`ADM0101`, `ADM0202`) correctly recorded that no footer was built. So the area was documented
      both ways at once. The owner chose to build it rather than strip the references.
    source: "chat 2026-09-21, owner answered 'có chân trang' via AskUserQuestion after the conflict was reported"
    impacted_paths:
      - 02-bd/screens/admin/_shell.md
      - 02-bd/screens/admin/ADM0101_overview.md
      - 02-bd/screens/admin/ADM0202_permission_matrix.md
      - 05-coding/frontend/src/widgets/app-shell/ui/admin-footer.tsx
      - 05-coding/frontend/src/widgets/app-shell/ui/app-shell.tsx
      - 05-coding/frontend/messages/vi.json
      - 05-coding/frontend/messages/en.json

  - id: DEC-2026-0921-teacher-screens-conflict-resolutions
    kind: architecture
    status: ACTIVE
    date: 2026-09-21
    subject: "Five conflicts found while writing the Instructor-area screen BDs, resolved in one pass: shell footer, unified student-status label set, per-class average score, grading queue tab conditions, grading student filter"
    decision: >
      (1) INSTRUCTOR SHELL FOOTER: the Instructor area gets a footer too, reusing the Admin footer's
      three-zone structure. This goes AGAINST the prototype evidence — all five
      `09-layoutBase/Giáo viên - *.dc.html` files contain zero `<footer>` elements — and is accepted
      because two adjacent internal areas, one with a footer and one without, is an inconsistency
      users see immediately. The exact link set for the Instructor footer is still open (Admin's
      "go-judge status" and "API docs" are operator concerns, likely wrong for A2).
      (2) STUDENT-STATUS LABELS: one five-value set for all four class-cluster screens —
      `INSUFFICIENT_DATA` "Chưa đủ dữ liệu", `ABSENT` "Vắng bài", `NEEDS_SUPPORT` "Cần hỗ trợ",
      `WATCH` "Theo dõi", `ON_TRACK` "Đang tốt" — evaluated in that severity order, first match wins.
      Thresholds are unchanged from `DEC-2026-0830-class-progress-dashboard`; what changes is that
      `class_management` had attached the label "Cần hỗ trợ" to the "completion below 50%" condition,
      which that decision calls "Theo dõi". `INSUFFICIENT_DATA` is new: a student who just joined and
      has submitted nothing must not be labelled "Đang tốt".
      (3) AVERAGE SCORE IS PER-CLASS: computed by restricting the submission set to the problems
      assigned to that class and counting via `GetStudentSubmissionMetrics` (`judge-orchestration`),
      NOT read from `identity.user_submission_stats`. No `class_id` dimension is added to that read
      model — the assigned-problem set changes over time, so a materialized per-class stat would go
      stale the moment an instructor removes a problem. `user_submission_stats` keeps its global role
      (F1-07); it simply stops being the source for the three class-cluster screens.
      (4) GRADING QUEUE TABS: `manual_graded_at IS NULL` moves out of the base queue query in
      `02-bd/database/F5_ai-review.md` section 4 and becomes a per-tab condition (Chờ chấm = IS NULL,
      Đã chấm = IS NOT NULL ordered by `manual_graded_at DESC`, Tất cả = no extra condition). The
      existing partial index still serves the default tab.
      (5) GRADING STUDENT FILTER: `instructor_grading` accepts a student parameter and shows it as a
      removable "Học viên: {tên}" chip, not a third tab row. Clearing it falls back to the class tab
      scope, not to "Tất cả".
    rationale: >
      (2) and (3) are cases where two settled sources said different things, so they could not be
      left to DD. For (3) the SoT ladder decides it outright: `01-rd/req/F1_identity.md` F1-28 says the
      score is "trong lớp" and RD sits at tier P2, above BD at P3 — when they disagree the BD moves.
      (4) was a latent defect rather than a disagreement: the query as written made two of the
      screen's three tabs permanently empty, which contradicts `DEC-2026-0831-instructor-grading-round2`
      Q3's requirement to show AI and manual scores side by side. (5) closes a gap where the RD of
      `class_student_detail` mandated a link into a filter the target screen had no way to receive.
      (1) is the only one decided against the evidence rather than from it, and is recorded as such.
    source: "chat 2026-09-21, owner instruction 'giảng viên có chân trang, rồi xử lý các xung đột mâu thuẫn còn lại' after reviewing 07-review/bd_screens_teacher_open_questions_260921.md"
    impacted_paths:
      - 02-bd/screens/teacher/_shell.md
      - 02-bd/screens/teacher/INS0201_class_management.md
      - 02-bd/screens/teacher/INS0203_class_progress.md
      - 02-bd/screens/teacher/INS0204_class_student_detail.md
      - 02-bd/screens/teacher/INS0301_grading.md
      - 02-bd/database/F5_ai-review.md
      - 07-review/bd_screens_teacher_open_questions_260921.md

  - id: DEC-2026-0921-class-completion-owned-by-identity
    kind: architecture
    status: ACTIVE
    date: 2026-09-21
    subject: "Class completion percentage is computed inside identity, not served by problem-bank: GetClassAssignmentCompletion is dropped. Plus the Instructor footer's four links"
    decision: >
      (1) COMPLETION OWNERSHIP: `GetClassAssignmentCompletion` is dropped before it was ever written.
      `problem-bank` exposes only the assigned-problem list through `ListClassAssignments` — an
      endpoint the BD already defines — and `identity` computes the completion ratio itself:
      denominator = that list, numerator = how many of those problems the student has
      `user_problem_best_score.best_verdict = ACCEPTED` for. Applies to all four class-cluster
      screens; `class_student_detail` had already been designed this way and the other three now
      follow it. Amendment 2026-10-25: `DEC-2026-1025-dd-class-assignment-time-window`
      replaces only the numerator's global best-score source with F4 assignmentResults for
      submissions after the current assignment. Identity still owns the computation and the
      Instructor footer decision below is unchanged.
      (2) INSTRUCTOR FOOTER LINKS: the Instructor footer keeps four links but not the Admin set.
      Keep "Nhật ký thay đổi" and "Hỗ trợ"; drop "Tài liệu API" and "Trạng thái go-judge"; add
      "Hướng dẫn giảng viên" and "Phản hồi".
    rationale: >
      (1) Three reasons, in order of weight. First, it needs no new cross-module endpoint at all,
      while the alternative needs one nobody has written. Second, `DEC-2026-0921-teacher-screens-conflict-resolutions`
      already commits `identity` to fetching the assigned-problem list in order to scope the average
      score to the class — so the list is in hand anyway and computing completion from it costs
      nothing extra, whereas the alternative does the same work twice in two modules. Third, it puts
      the metric with its owner: student progress is `identity`'s domain (F1-06, F1-28), and the
      alternative would have forced `problem-bank` to read two `identity` tables
      (`class_enrollments` and `user_problem_best_score`) to compute a number about students. The
      cost is sending a list of a few dozen problems instead of one number, which is not material.
      (2) The Admin link set is operator-facing; instructors do not operate the system, so two of the
      four links would be dead weight. Changing it now is cheap because all four are still static
      text with no real destination; after the UI is built it would mean editing the component and
      both i18n files.
    source: "chat 2026-09-21, owner asked for a recommendation then approved both ('theo bạn phương án là gì' -> 'ok')"
    impacted_paths:
      - 02-bd/screens/teacher/_shell.md
      - 02-bd/screens/teacher/INS0201_class_management.md
      - 02-bd/screens/teacher/INS0203_class_progress.md
      - 02-bd/screens/teacher/INS0204_class_student_detail.md
      - 07-review/bd_screens_teacher_open_questions_260921.md
  - id: DEC-2026-0922-users-and-admin-conflict-resolutions
    date: 2026-09-22
    status: ACTIVE
    subject: "Gộp 7 quyết định chốt mâu thuẫn SoT và 17 khoảng trống schema khu Người học và Quản trị theo tiêu chí ponytail tinh gọn"
    decision: >
      (1) Header khu Người học giữ bộ chuyển Dark/Light Theme và Locale VI/EN đặt cạnh avatar menu người dùng (nhất quán 3 khu).
      (2) Cấp độ Phỏng vấn giả lập thống nhất 4 mức enum (INTERN, JUNIOR, MIDDLE, SENIOR) toàn hệ thống.
      (3) Cột users.default_language thuộc về màn Thiết lập (USR0503, tuỳ chọn hành vi), bỏ ô chọn ở Hồ sơ (USR0502).
      (4) Theme & Locale lưu theo thiết bị (Cookie NEXT_LOCALE + LocalStorage next-themes), không lưu vào bảng users.
      (5) Khối "Bài đang làm dở" định nghĩa là bài nộp gần nhất có verdict khác Accepted (judge.submissions); không thêm bảng nháp DB.
      (6) Tham số Sandbox (chặn mạng, subprocess, stderr) chuyển thành cấu hình toàn cục dùng chung, không cấu hình per-language.
      (7) Quyền đọc nhật ký audit log SYSTEM_AUDIT_LOG mặc định mở cho mọi Admin; ma trận phân quyền chỉ gác cấu hình log.
      (8) Khung Giảng viên bổ sung nhóm nav "Nội dung" (Quản lý bài tập + Câu hỏi phỏng vấn) trên sidebar.
      (9) Không thêm bảng snapshot tuần và không thêm bảng hạn nộp bài tập lớp; tính động từ judge.submissions khi truy vấn.
    rationale: >
      Theo tiêu chí ponytail (tối giản, tinh gọn, không vẽ nghiệp vụ thừa):
      - Tận dụng tối đa dữ liệu và cơ chế hiện có (Cookie, LocalStorage, bảng submissions) thay vì đẻ thêm bảng nháp hoặc cron job định kỳ.
      - Nhất quán trải nghiệm người dùng giữa 3 khu vực (Admin, Giảng viên, Người học).
      - Ranh giới rõ ràng giữa dữ liệu nhận dạng cá nhân (profile) và tuỳ chọn ứng dụng (settings).
    source: "chat 2026-09-22, owner approved recommendation ('ok tôi đồng ý với đề xuất bạn xử lý cho tôi các điểm còn thiếu trong báo cáo, nhớ theo tiêu chí ponytail càng tinh gọn tối giản, k vẽ thêm nghiệp vụ thừa')"
    impacted_paths:
      - 02-bd/screens/users/_shell.md
      - 02-bd/screens/teacher/_shell.md
      - 02-bd/screens/users/USR0503_settings.md
      - 02-bd/screens/users/USR0502_profile.md
      - 02-bd/database/F1_identity.md
      - 02-bd/database/F4_judge-orchestration.md
      - 02-bd/security/F1_identity.md
      - 07-review/bd_screens_unified_open_questions_260922.md
      - 07-review/bd_screens_unified_open_questions_260922.html

  - id: DEC-2026-0925-instructor-separate-login-route
    kind: architecture
    status: ACTIVE
    date: 2026-09-25
    subject: "Instructor gets its own /instructor/login route AND a dedicated backend endpoint that rejects non-instructor accounts, not just a separate screen"
    decision: >
      Extends the `DEC-2026-0915-admin-separate-login-route` pattern one step further. Diverges from
      `02-bd/screens/shared/SHR0101_auth.md:106` the same way that decision already does. Owner
      explicitly asked for instructor to have its own login API, not only its own screen (unlike
      admin, which reuses `POST /auth/login` unchanged). New endpoint
      `POST /api/v1/identity/auth/instructor/login` (`03-dd/api/F1_identity.md` endpoint #49): same
      request/response shape as `/auth/login`, but the server checks `baseCategory = INSTRUCTOR`
      after credentials verify and rejects any other role with `403 IDT-111` — the credentials
      themselves are already proven correct at that point (this is not the account-enumeration case
      `/auth/password/forgot` guards against), so the error message may say plainly that the account
      is not an instructor account. `/admin/login`'s existing shared-endpoint choice is untouched.
      Frontend: `/instructor/login` (route group `(public)`, no `AppShell`), its own view
      `views/instructor-auth`, calling a new `instructorLogin` mutation
      (`entities/auth/api/mutations.ts`) instead of `login`. No dedicated `09-layoutBase` mockup
      exists for an instructor login screen (only Admin has one, which is why `admin-auth` could
      borrow its cyan/teal palette) — per the project's "bám 09-layoutBase, không bịa" rule, this
      screen reuses the shared screen's neutral glass styling rather than inventing a new palette,
      distinguished only by its own heading/copy and by hiding signup (`useAuthFlow("login")`) —
      OAuth stays hidden too (`showOAuth={false}`), mirroring `admin-auth`'s reasoning: an OAuth
      linked-account flow would need its own role-check design this decision does not cover.
    rationale: >
      [SoT: Suy luận] — no RD/BD line asks for this split; owner confirmed via AskUserQuestion that
      the point of a separate instructor endpoint (as opposed to just a separate route reusing
      `/auth/login`) is defense-in-depth: isolating the instructor login surface so a
      student/admin credential pair cannot authenticate through it at all, not merely a URL/skin
      preference. Recorded as a decision because it adds a new API contract entry and a role-restriction
      rule, per `CLAUDE.md` "Rules" (architecture/API-contract divergence needs a decision entry).
    source: "chat request 2026-09-25, confirmed via AskUserQuestion (chose 'Có, chặn ở API' over 'chỉ tách route, logic y hệt /auth/login')"
    impacted_paths:
      - 03-dd/api/F1_identity.md
      - 02-bd/screens/shared/SHR0101_auth.md
      - 05-coding/frontend/src/app/(public)/instructor/login/page.tsx
      - 05-coding/frontend/src/views/instructor-auth
      - 05-coding/frontend/src/entities/auth
      - 05-coding/frontend/src/features/auth-by-credentials

  - id: DEC-2026-0926-sequence-detail-tier
    kind: process-policy
    status: ACTIVE
    date: 2026-09-26
    subject: "drawio-uml-architect gets a second, higher complexity ceiling for MULTI-TIER sequence diagrams; the shared ceiling still governs every other diagram"
    decision: >
      `DEC-2026-0825-uml-skill-parity` pins one shared complexity ceiling across
      `vp-uml-architect`, `mermaid-uml-architect` and `drawio-uml-architect` so the same business
      content hits the same limit whichever tool draws it. For sequence that ceiling is 6
      participants / 14 messages / 1 alt block — an OVERVIEW ceiling. The owner asked (2026-09-26)
      for the per-actor sequence diagrams to be redrawn multi-tier, following the layout of
      `08-diagram/layout_sequence.png`: controller / service / repository / datastore as separate
      lifelines instead of one merged "Máy chủ AlgoPrep" box, one labelled band per business phase,
      and UML nested numbering. That does not fit in 14 messages, and merging the tiers back is
      exactly what the request rules out. New: a spec may set `sequence.detail = true`, which
      switches the ceiling to `sequence_detail` — 10 participants / 56 messages / unlimited alt
      blocks — and narrows the lifeline head to 130px so 7 columns still fit the long edge of A4.
      Without the flag nothing changes: the shared 6/14/1 ceiling and its single-alt refusal stay
      exactly as they were, so every existing spec keeps rendering the same picture. The shared
      ceiling is NOT raised and `vp-uml-architect` / `mermaid-uml-architect` are NOT touched —
      parity still holds for overview diagrams, which is what that decision is about.
    rationale: >
      Two ceilings, not one raised ceiling, because they answer different questions. The shared
      ceiling answers "is this readable as an overview" — raising it would silently let overview
      diagrams grow in all three tools. The detail ceiling answers "is this still one diagram
      rather than three" for a report figure whose whole job is showing the layering. Splitting a
      multi-tier flow to fit 14 messages would produce diagrams that each show a fragment of a
      request with no beginning or end, which is worse for the report than one tall figure. Cost
      recorded honestly: a detail diagram with 8 lifelines is 1315px wide, past the 1169px long
      edge of A4 — the linter warns on exactly that, and those figures print A3 or ship as SVG.
    source: "owner request 2026-09-26 (\"làm lại các bản vẽ từng actor của sơ đồ sequence chi tiết nhiều tầng hơn ... chia từng phase actor\"), sample layout 08-diagram/layout_sequence.png"
    impacted_paths:
      - .claude/skills/drawio-uml-architect/SKILL.md
      - .claude/skills/drawio-uml-architect/references/spec-schema.md
      - .claude/skills/drawio-uml-architect/scripts/gen_drawio.py
      - .claude/skills/drawio-uml-architect/scripts/lint_drawio.py
      - 08-diagram/01-rd/req/sequence/

  - id: DEC-2026-0927-diagram-tree-flatten-by-type
    kind: process-policy
    status: ACTIVE
    date: 2026-09-27
    subject: "08-diagram/ is filed by diagram type and actor, NOT by mirroring the path of a source document"
    decision: >
      The diagram tree drops its document-path prefix. `08-diagram/01-rd/req/usecase/user/...` becomes
      `08-diagram/usecase/user/...`; same for `sequence/` and `activity/`. The per-actor subfolders of
      `DEC-2026-0908-diagram-per-actor-subfolders` stay exactly as they are -- only the prefix above them
      is removed, and that decision remains ACTIVE with its paths re-rooted. The overview diagram
      (`usecase_tong_quan_actor.*`) still sits directly under `usecase/`. All 57 `.drawio` + 57
      `.spec.json` moved; no file content changed, so lint is unchanged (0 errors, 9 pre-existing
      warnings).
    rationale: >
      Owner instruction 2026-09-27. The mirroring rule assumed each diagram illustrates one document.
      That stopped being true: a sequence diagram like `sequence_shared_quan_ly_testcase` takes its scope
      from `01-rd/req/F2_problem-bank.md`, its module boundaries from `02-bd/architecture/`, and its
      endpoint and port names from `03-dd/` -- three parents, all equally valid. Mirroring forced a pick,
      and the deepest prefix `01-rd/req/` won by habit, quietly filing diagrams under RD whose real
      source was mostly BD and DD. Filing by type and actor matches how the diagrams are actually looked
      up (which actor, which kind of picture), which is also how `feature_catalog.md` already prints them
      once the prefix is stripped -- the catalog's listed values did not change at all, only the prefix
      sentence in its header.
    source: "owner request 2026-09-27 (xoa folder 01-rd, cay se la 08-diagram/<loai>/<actor>/)"
    impacted_paths:
      - 08-diagram/
      - CLAUDE.md
      - .claude/skills/drawio-uml-architect/SKILL.md
      - .claude/skills/mermaid-uml-architect/SKILL.md
      - .claude/skills/vp-uml-architect/SKILL.md
      - 01-rd/overview/feature_catalog.md
      - 01-rd/overview/feature_catalog.html
      - BAO_CAO_KHAO_SAT_HE_THONG.md

  - id: DEC-2026-0927-usecase-overview-tier
    kind: process-policy
    status: ACTIVE
    date: 2026-09-27
    subject: "drawio-uml-architect gets a separate complexity ceiling for the SYSTEM-WIDE use case diagram; the shared per-feature ceiling still governs every other use case diagram"
    decision: >
      A use case spec may set `usecase.scope = "overview"`. That switches the ceiling from the
      shared `usecase` tier (16 nodes / 16 edges, and A4) to a new `usecase_overview` tier --
      24 nodes / 28 edges, page budget A3 landscape (1654x1169) instead of A4. The generator marks
      the system boundary box with `algoprep=overview` so `lint_drawio.py` can classify a `.drawio`
      on its own, the same way `algoprep=phase` identifies a detailed sequence diagram. Without the
      key nothing changes: all 21 per-feature use case diagrams keep the 16/16 ceiling and the A4
      page check exactly as before. The shared ceiling is NOT raised, and `vp-uml-architect` /
      `mermaid-uml-architect` are NOT touched -- same split as
      `DEC-2026-0926-sequence-detail-tier`, for the same reason.
    rationale: >
      `08-diagram/usecase/usecase_tong_quan_actor` measured 22 nodes / 24 edges at 1300x895 and
      emitted 3 warnings on every lint run. Those warnings were never actionable: the 16-node
      ceiling is written for a diagram of ONE feature ("<= 10 goi chuc nang + <= 4 actor + <= 2 he
      thong ngoai"), while an overview diagram has to carry all four actor groups by definition.
      A permanent warning is worse than no warning, because a chronic warning trains the reader to
      skim past the real ones -- and the linter is the only automated check these diagrams get.
      Splitting the overview in two was rejected: it would fit the tool by destroying the one thing
      the diagram is for, seeing the whole system at once, and the report would lose a load-bearing
      figure. 24/28 is the measured 22/24 plus margin for one or two more features; past that a
      split really is the right answer. A3 landscape rather than A4 because a full-system figure is
      normally printed as a single full-page plate anyway -- the same cost already accepted in
      writing for detailed sequence diagrams.
    source: "owner request 2026-09-27 (\"ok thêm bậc trần cho tôi\"); measurement: lint_drawio.py on 08-diagram/usecase/usecase_tong_quan_actor.drawio before the change (22 node / 24 canh / 1300x895, 3 WARN)"
    impacted_paths:
      - .claude/skills/drawio-uml-architect/scripts/gen_drawio.py
      - .claude/skills/drawio-uml-architect/scripts/lint_drawio.py
      - 08-diagram/usecase/usecase_tong_quan_actor.spec.json
      - 08-diagram/usecase/usecase_tong_quan_actor.drawio

  - id: DEC-2026-0927-student-dashboard-home
    kind: scope
    status: ACTIVE
    date: 2026-09-27
    subject: "Student area gets its own overview screen (USR0601_dashboard); the STUDENT post-login destination moves from my_progress to dashboard"
    decision: >
      Adds a 13th screen to the Student area: `USR0601_dashboard`, built from
      `09-layoutBase/Dashboard AlgoPrep.dc.html`. This also amends part of Q3 in
      `01-rd/screens/shared/SHR0101_auth.md` (closed 2026-08-25): the `STUDENT` post-login
      destination changes from `my_progress` to `dashboard`. The other two destinations are
      unchanged (`INSTRUCTOR` -> `instructor_overview`, `ADMIN` -> `admin_overview`), and the
      returnUrl-wins rule is unchanged.
      Boundary between the two screens, so they do not become duplicates: `dashboard` answers
      "what do I do next" (greeting + weakest topic this week, Resume-last-draft button, suggested
      problems, recent Solution Reviews, 12-month activity, skill radar); `my_progress` keeps its
      "where do I stand" role (the full topic table with AC rate and last-submitted, by-difficulty,
      focus-next). The blocks that genuinely overlap (stat strip, submissions-per-day chart, topic
      progress) stay on both exactly as the prototypes draw them, but `dashboard` carries the
      shorter version.
    rationale: >
      A direct comparison of the two prototype files showed roughly 60% of the blocks overlap, and
      the owner was advised to keep `/progress` and graft the three action blocks onto the top of
      it instead — cheaper, no RD change, no need for this decision. The owner heard that and still
      chose a separate dashboard screen ("vay them cho toi dashboard"). Recorded as a decision
      because it overturns a closed question (Q3) and adds a screen to the RD — two things a later
      session must not silently infer in reverse. Worth noting: `ADMIN` and `INSTRUCTOR` each
      already have their own overview screen (ADM0101, INS0101); before this decision `STUDENT` was
      the only role without one, which is the strongest argument in favour.
    source: "Direct owner instruction, session 2026-09-27, after being advised against it and restating the request unchanged"
    open_questions:
      - "Whether the Student area gets a shared footer: only 2 of the 12 prototypes carry the
        3-column footer (`Dashboard AlgoPrep`, `Ngan hang bai toan`). Not built. The Admin area
        already settled this as yes (02-bd/screens/admin/_shell.md section 9)."
      - "No BD (`02-bd/screens/users/USR0601_dashboard.md`) and no DD for this screen yet — the
        other 12 all have a BD. Logged in 06-plan/PROTOTYPE_DEBT.md section 11."
    impacted_paths:
      - 01-rd/screens/users/USR0601_dashboard.md
      - 01-rd/screens/shared/SHR0101_auth.md
      - 05-coding/frontend/src/entities/user/model/area.ts
      - 05-coding/frontend/src/views/dashboard/
      - 06-plan/PROTOTYPE_DEBT.md

  - id: DEC-2026-0927-diagram-covers-field
    kind: process-policy
    status: ACTIVE
    date: 2026-09-27
    subject: "A diagram declares the feature codes it covers in a `covers` field; the catalog cross-reference reads that field instead of grepping the spec for code strings"
    decision: >
      Every `08-diagram/**/*.spec.json` now carries a top-level `covers` array listing the `Fx-nn`
      codes that diagram actually depicts. The diagram half of the "File lien quan" column in
      `01-rd/overview/feature_catalog.md` (and `.html`) is built from that field and nothing else.
      `_`-prefixed note keys are free prose again -- a note may name any code, for contrast or to
      record what was deliberately left out, without that code becoming an attribution.
      `.claude/skills/drawio-uml-architect/scripts/check_catalog.py` reconciles the 57 specs against
      both catalog files, exits non-zero on drift, and rewrites the column with `--fix`. The field is
      inert for rendering: regenerating all 57 `.drawio` after the change produced byte-identical
      output, so no PNG needed re-exporting. Net effect on the catalog: 61 attributions removed,
      10 added, 42 of 111 rows changed.
    rationale: >
      The old rule was "the literal string `Fx-nn` appears anywhere in the spec", and it was wrong in
      both directions. False positives: a spec note reading "KHONG ve F5-22" credited the diagram with
      F5-22. Re-auditing all 57 specs found 61 such attributions, concentrated in the use case specs
      whose notes carry an explicit "Bo bot:" paragraph listing what was deliberately NOT drawn --
      the clearer the note, the more codes it wrongly claimed. False negatives: a drawn label reading
      "F2-05 toi F2-10" counted only for the two endpoints, losing 10 correct attributions in between.
      A "just ignore `_` keys" fix was considered and rejected: all 16 `activity/` specs carry every
      one of their codes in notes and nothing in the drawn body, so that rule would have deleted all
      31 activity references and recreated the 2026-09-20 defect it was meant to prevent. The deeper
      reason for a script rather than a documented procedure: the 2026-09-20 pass was a manual grep,
      it missed the entire `activity/` tree (0 references where there should have been 31), and
      nothing caught it for a week. A cross-reference that cannot be re-derived on demand is not a
      cross-reference, it is a snapshot that silently rots.
    source: "owner request 2026-09-27; plan 06-plan/260927-1620-dong-3-viec-con-mo-so-do.md section B; measurement: 39/57 specs held codes appearing only in `_` notes, hand-reviewed one by one"
    impacted_paths:
      - 08-diagram/
      - .claude/skills/drawio-uml-architect/scripts/check_catalog.py
      - .claude/skills/drawio-uml-architect/references/spec-schema.md
      - 01-rd/overview/feature_catalog.md
      - 01-rd/overview/feature_catalog.html
  - id: DEC-2026-0927-student-area-merge-and-shared-shell
    kind: scope
    status: ACTIVE
    date: 2026-09-27
    subject: "my_progress merged into dashboard; Student nav realigned to _shell.md; one shared responsive container across all three actor shells"
    decision: >
      Four owner instructions from one session, all touching the Student area shell.
      (1) MERGE: `my_progress` (USR0501) is folded into `dashboard` (USR0601) and `/progress` is
      removed. The three blocks `dashboard` did not already have -- the full topic table, the
      by-difficulty card and "Nen uu tien" -- move onto `dashboard`, replacing its shortened topic
      progress bars, which said strictly less. One screen-level range control now drives the daily
      chart, the topic table and the focus card; the chart's own local 7/30/90 tabs are gone.
      (2) NAV: the Student nav is rebuilt from `02-bd/screens/users/_shell.md` muc 2.2-2.3 instead of
      from the prototype. It is now six items -- the BD's five (Bai toan, Workspace, Bai da nop,
      Phong van gia lap, Cau hoi phong van) plus "Tong quan" for the new dashboard. Two id-less
      routes back the BD entries that had none: `/workspace` redirects to the most recent unfinished
      draft and falls back to the problem list (the answer `_shell.md` muc 6 Q2 already gave), and
      `/mock-interview` renders `MockInterviewView`, which always opened on its own entry screen and
      never read a submissionId. The user menu keeps four items -- `_shell.md` lists five, but
      "Tien do cua toi" no longer exists as a screen.
      (3) FOOTER: built for every Student screen, per `_shell.md` muc 3 (decided 2026-09-21). The
      live queue counter is dropped in favour of a static cluster-status line, the recommendation in
      that file's muc 6 Q3. It renders OUTSIDE `<main>` so it is exposed as the `contentinfo`
      landmark.
      (4) SHARED CONTAINER: all three actor shells now wrap content in one `shared/ui/PageContainer`
      whose pixel cap comes from `shared/config/layout.ts` (admin 1320, instructor 1320, student
      1400) and whose horizontal gutter shrinks by breakpoint. This OVERRIDES `_shell.md` muc 4,
      which had said the Student area deliberately has no shared max-width. The narrow-viewport nav
      drawer of `_shell.md` muc 6 Q5 is built at the same time.
    rationale: >
      The merge is what the owner was originally advised toward (one screen, not two ~60%-duplicated
      ones); they first chose two screens, then on seeing both built chose to merge -- so the end
      state matches the earlier recommendation by a different route, with the dashboard rather than
      `my_progress` as the surviving screen.
      The nav rebuild corrects a real defect, not a preference: the previous pass built the nav from
      `09-layoutBase/*.dc.html` alone and dropped Workspace and Phong van gia lap on the reasoning
      that they "need an id and would 404". `_shell.md` muc 6 Q2 had already answered exactly that on
      2026-09-22. The prototype is layout evidence; `02-bd` is the specification, and the SoT ladder
      puts P3 above P4. Same root cause for the footer, which that pass logged as an open question
      when it had been closed on 2026-09-21.
      The shared container replaces two hard-coded `max-w-[...px]` values inlined in two branches of
      `app-shell.tsx`, which had already drifted apart and could not be tuned without editing JSX.
    source: "Owner instructions, session 2026-09-27: merge progress into dashboard, build the missing prototype screens per layoutBase, add the footer, and use one shared responsive container with a configurable px cap across all three actor prototypes"
    supersedes_partially:
      - "DEC-2026-0922-users-and-admin-conflict-resolutions -- its Q1 removed the 'Tong quan' nav item
        because the prototype only pointed at a static file with no slug. DEC-2026-0927-student-dashboard-home
        created that slug, so the item is back. Every other part of the 2026-09-22 decision stands."
      - "DEC-2026-0927-student-dashboard-home -- its boundary note kept `dashboard` and `my_progress`
        as two screens answering two questions. They are now one screen; that note is historical."
    open_questions:
      - "The live judge-queue count in the footer (_shell.md muc 6 Q3) is still unanswered -- the
        static line is a placeholder pending 03-dd/api/F4_judge-orchestration.md."
      - "No BD for USR0601_dashboard yet, now covering the merged scope as well. Ledger section 12."
    impacted_paths:
      - 01-rd/screens/users/USR0601_dashboard.md
      - 01-rd/screens/users/USR0501_my_progress.md
      - 01-rd/overview/system_survey.md
      - 02-bd/screens/users/_shell.md
      - 05-coding/frontend/src/shared/config/layout.ts
      - 05-coding/frontend/src/shared/ui/page-container.tsx
      - 05-coding/frontend/src/widgets/app-shell/
      - 05-coding/frontend/src/views/dashboard/
      - 06-plan/PROTOTYPE_DEBT.md
  - id: DEC-2026-0927-submission-metrics-two-ports
    kind: architecture
    status: ACTIVE
    date: 2026-09-27
    subject: "GetStudentSubmissionMetrics is split into two read ports — GetClassStudentSubmissionMetrics (instructor) and GetMySubmissionMetrics (student) — and finally written into the judge-orchestration module BD"
    decision: >
      `judge-orchestration` now declares, in `02-bd/architecture/F4_judge-orchestration.md` muc 3.3, TWO
      read ports for other modules instead of the single `GetStudentSubmissionMetrics` that five screen
      BDs had been assuming:
      (1) `GetClassStudentSubmissionMetrics(classId, studentIds[], problemIds[], weekCount)` — the
      instructor flow (A2), reads OTHER people's data for a set of students inside one class, gated by
      CLASS_MANAGEMENT and class scope, weekly granularity. Called by INS0201, INS0203, INS0204.
      (2) `GetMySubmissionMetrics(range | dayCount, topicIds[]?)` — the student flow (A1), reads ONLY the
      caller's own data with the identity taken from the session token and NO identity parameter at all,
      daily granularity. Called by USR0501 and USR0601.
      On the calling side `identity` declares two narrow outbound ports, `ClassStudentMetricsPort` and
      `MySubmissionMetricsPort`, replacing the single `StudentSubmissionMetricsPort` the screen BDs named.
      The 364-day activity grid of USR0601 is served by `GetMySubmissionMetrics` with `dayCount = 364`,
      NOT by a separate `GetMyActivityCalendar` owned by `identity` as that screen's BD first proposed.
    rationale: >
      The endpoint had been requested by five screen BDs since 2026-09-21 and existed in none of them's
      owning module: the judge architecture document declared only `JudgeExecutionPort` and
      `ProblemGradingSpecPort`, and its muc 9 "Viec con mo" did not even list it, so the owning module had
      no way to know it was being depended on. A resolution was recorded in
      `07-review/bd_screens_unified_open_questions_260922.md` item B-10 and marked "DA XU LY", but it was
      never copied into the module BD — a review document is not SoT — and it only covered the instructor
      shape. Meanwhile `03-dd/logic/F1_identity.md` BR-10 was already written against the missing contract.
      SPLIT rather than one parameterised port, chosen by the owner over the assistant's recommendation of
      a single port, and the owner's call is stronger on the axis that matters most: both student screen
      BDs carry a mandatory Sheet 9 check that every endpoint must take `user_id` from the session token
      and never from a client parameter. A merged port with an optional `studentIds[]` leaves nothing in
      the signature stopping a student screen from passing someone else's id — the IDOR rule would survive
      only as a sentence in a document. Two ports make it structural: the student-side port has nowhere to
      put an identity. Secondary reasons: different time granularity (weekly vs daily), different
      cardinality (a set of students vs one caller), different authorization layer.
    source: "Owner instruction, session 2026-09-27: 'vay xu ly cho toi se chia lam 2 luong cho instructor rieng va user rieng'"
    supersedes_partially:
      - "07-review/bd_screens_unified_open_questions_260922.md item B-10 — its resolution (one merged query
        method taking a list of user_id and the class problem set) is replaced by the two-port split, and
        is now actually landed in the module BD rather than living only in a review document."
    open_questions:
      - "Real signatures of both ports (parameter names, return types, pagination when the student set is
        large) — deferred to DD, logged in judge-orchestration muc 9."
      - "Performance of the 364-day window of GetMySubmissionMetrics: it runs on the post-login landing
        screen of every student. Three candidate approaches logged in judge-orchestration muc 9; measure
        before choosing."
    impacted_paths:
      - 02-bd/architecture/F4_judge-orchestration.md
      - 02-bd/screens/teacher/INS0201_class_management.md
      - 02-bd/screens/teacher/INS0203_class_progress.md
      - 02-bd/screens/teacher/INS0204_class_student_detail.md
      - 02-bd/screens/users/USR0501_my_progress.md
      - 02-bd/screens/users/USR0601_dashboard.md
      - 03-dd/logic/F1_identity.md
      - 06-plan/PROTOTYPE_DEBT.md

  - id: DEC-2026-0928-diagram-print-legibility
    kind: process-policy
    status: ACTIVE
    date: 2026-09-28
    subject: "A diagram is linted for whether its text is still readable once placed in the report, not for whether its page is A4; label font size is a per-diagram-class setting tuned to that"
    decision: >
      `lint_drawio.py` drops the "page must be A4" check and replaces it with a print-legibility
      check: place the image at the report's usable width (6.53in on Letter with 0.984in margins),
      shrink it to fit the 8.6in of height left after a caption line if it is taller than that, and
      warn when the label font then prints below 6pt. The measurement reads the exported `.png` for
      the real image width (the content bounding box understates it by up to 52%, because message and
      guard labels overflow their node boxes) and derives the export scale from the bounding box
      height, which is accurate; it reads the actual `fontSize` out of the file rather than assuming
      one per diagram kind. Label sizes are raised to match: detail sequence 11 to 13 (stereotype line
      10 to 12), overview use case 12 to 17 with taller ovals. Neither change alters diagram width --
      sequence width follows participant-name length and use case width follows the oval constant --
      so the cost is vertical space only, which the split into page-sized figures had already freed.
    rationale: >
      The old check answered a question nobody had. Every sequence diagram in the project failed it
      ("trang 1315x2340 khong phai A4") while the real defect went unreported for a week: placed in
      the report, those diagrams printed at 3.9-4.4pt and the owner could not read them. Worse, the
      overview use case diagram PASSED every check while printing at 4.5pt -- the single worst figure
      in the report, and the first one a reviewer sees. Complexity ceilings and print legibility are
      different questions: a diagram can sit well inside 24 nodes and still be unreadable, and the
      linter is the only automated check these diagrams get. Measuring from the `.png` rather than the
      XML is a deliberate trade: it means the check is skipped when the image has not been exported
      yet, which is the right failure mode -- silence when there is nothing to measure, rather than a
      confident wrong number from a bounding box that does not include the text.
    source: "owner request 2026-09-28 (\"xử lý nốt cho tôi\"); measurements: 92 diagrams before the change had 5 figures under 6pt including the overview at 4.47pt, after the change 0 of 92, smallest 6.30pt"
    impacted_paths:
      - .claude/skills/drawio-uml-architect/scripts/lint_drawio.py
      - .claude/skills/drawio-uml-architect/scripts/gen_drawio.py
      - scripts/build_bao_cao_docx.py
      - 08-diagram/

  - id: DEC-2026-0928-f2-14-generator-script
    kind: scope
    status: ACTIVE
    date: 2026-09-28
    subject: "F2-14 switches from AI emitting testcase data to AI writing a generator script that runs in the sandbox; the reference solution gets its own code F2-18 and never enters the AI prompt"
    decision: >
      F2-14 is rewritten to the problem-setting model used by competitive judges (generator + model
      solution + validator; Codeforces Polygon is the public reference). AI receives the statement,
      the declared data constraints and the two author-written Sample testcases -- as data
      parameters, separated from the system instruction -- and returns a SCRIPT that generates
      inputs, each labelled with the case class it targets. The system runs that script in the
      sandbox, validates every generated input against the declared constraints, feeds the valid
      ones through the reference solution to obtain real outputs, and stores the pairs as unapproved
      drafts. Three constraints are new: a generated-output SIZE limit (a generator exists to emit a
      lot, unlike a student submission which is bounded by only printing an answer), a fixed random
      seed (without it F2-09 testcase-set versioning is meaningless and "regenerate" silently
      changes what grades students), and input validation against the declared constraints. Only
      APPROVED testcases count toward the publish checklist. The reference solution becomes F2-18, a
      code of its own, and is never placed in an AI prompt. Out of scope, stated explicitly: a
      checker for problems with multiple valid answers, and a second (brute-force) solution to
      cross-check the reference solution.
    rationale: >
      The previous design was safe but not rigorous: it guaranteed the system would not invent a
      wrong expected-output, and said nothing about whether the generated testcases would catch a
      wrong submission. Two holes followed. First, performance testcases were impossible -- catching
      an O(n^2) submission needs a maximum-size input, and having a model emit 100,000 elements
      literally is not workable, so the whole class of test that discriminates on complexity was
      missing. A script produces it in milliseconds. Second, the "output comes from a real run"
      safeguard does not protect against a MALFORMED input: a wrongly shaped input still runs
      through the reference solution and still yields a real output, so the pair looks valid and
      fails a correct submission -- which is the exact risk the safeguard was built to remove, taking
      a different door. Hence the validator. The infrastructure argument decided the shape: BD
      already settled `SampleSolutionRunnerPort` on 2026-09-13 as a narrow "run a program, get
      output" port, so the risky part -- executing untrusted code under limits -- exists and is
      already hardened; only the port contract needs widening for a run with no input and a large
      output. Known limitation recorded rather than hidden: the reference solution is the sole
      oracle, so a bug in it is stamped into every generated testcase, and the only anchor outside
      that circle is the two Sample testcases the author writes by hand.
    source: "owner instruction 2026-09-28, after the owner proposed the generator-script model and reviewed a critique of the data-emitting version; flow diagrams in 06-plan/260928-1510-f2-14-generator-*"
    impacted_paths:
      - 01-rd/req/F2_problem-bank.md
      - 01-rd/screens/shared/SHR0202_problem_authoring.md
      - 01-rd/overview/feature_catalog.md
      - 01-rd/overview/feature_catalog.html
      - 02-bd/architecture/F2_problem-bank.md
      - 02-bd/security/F2_problem-bank.md
      - 05-coding/frontend/src/entities/problem-draft/
      - 05-coding/frontend/src/views/problem-authoring/

  - id: DEC-2026-0929-fsd-views-grouped-entities-domain-only
    kind: architecture
    status: ACTIVE
    date: 2026-09-29
    subject: "Frontend FSD: views/ grouped by actor; entities/ holds only domain nouns, screen-private data lives in its view"
    decision: >
      (1) `views/` gains one grouping level that mirrors `01-rd/screens/`: `views/{admin,teacher,users,shared}/<slug>`.
      The actor prefix is dropped from the slice name (`views/admin-ai-config` -> `views/admin/ai-config`,
      `views/instructor-grading` -> `views/teacher/grading`, `views/auth` -> `views/shared/auth`). A group folder holds
      slices only, never code. ESLint element pattern for views becomes `src/views/*/*` (capture group + slice);
      all element patterns are anchored at `src/` so `shared/*` cannot match `views/shared/*`; the deep-import
      pattern for views becomes `@/views/*/*/*`. (2) A slice under `entities/` must be a domain noun of a Bounded
      Context F1-F6 used by two or more slices (or needed by a planned widget/feature). DTOs and queries used by a
      single screen live in that view's `model/` and `api/` segments. Ten entities moved into their only consumer:
      admin-dashboard -> views/admin/overview, student-dashboard -> views/users/dashboard, ai-config ->
      views/admin/ai-config, ai-usage -> views/admin/ai-usage, audit-log -> views/admin/system-log, judge-queue ->
      views/admin/queue-monitor, language-config -> views/admin/language-config, permission-matrix ->
      views/admin/permission-matrix, admin-user -> views/admin/user-management, admin-problem ->
      views/shared/problem-management. The former entity `index.ts` became `<view>/api/index.ts`. `problem-draft`
      merged into `entities/problem` (`model/draft-types.ts`, `api/__mock__/problem-draft-mocks.ts`). Result:
      entities 22 -> 11, views 33 flat -> 33 in 4 groups. No logic changed; `pnpm check` PASS (37 files, 110 tests).
    rationale: >
      Owner concern 2026-09-29 that `views/` and `entities/` sprawl would not scale. Measured: 13 of 22 entities had
      exactly one consumer, and most of those were screen-shaped DTOs (dashboard counters, log rows) rather than
      business nouns -- entity slices were being opened per screen, so `entities/` grew with the screen count
      instead of with the domain. Keeping screen-private data in the view makes `entities/` track the six Bounded
      Contexts and means deleting a screen deletes its data code. Views were not too many -- one per screen is the
      anti-drift rule -- but 33 flat folders hid which actor owns a screen; mirroring `01-rd/screens/` keeps the
      grep chain `01-rd -> 02-bd -> 03-dd -> code` intact and matches the `admin/` grouping SYS0102 already sketched.
      Grouping by RD folder name (teacher/users) rather than route group name (instructor/student) because the
      anti-drift rule is rooted in the documents. Rejected: grouping entities by actor (entities follow the
      domain, not the screen), and pre-creating features/widgets slices (created when a second consumer appears).
    source: "owner request 2026-09-29 (toi uu architecture FE: views/ va entities/ qua nhieu folder con), approved 'ok trien khai'"
    impacted_paths:
      - 05-coding/frontend/src/views/
      - 05-coding/frontend/src/entities/
      - 05-coding/frontend/src/app/
      - 05-coding/frontend/eslint.config.mjs
      - 01-rd/system/SYS0102_frontend_architecture.md
      - 02-bd/screens/
      - .claude/skills/nextjs-fsd-expert/

  - id: DEC-2026-0929-default-accent-from-layoutbase
    kind: tech-choice
    status: ACTIVE
    date: 2026-09-29
    subject: "Default scope accent tokens adopt the student-area mockup palette instead of the placeholder Tailwind blue"
    decision: >
      In the default (student/public) token scope, `--color-primary` / `--color-on-primary` /
      `--color-danger` are taken verbatim from the student-area mockups: Light #1c9bf2 / #ffffff /
      #d8353a, Dark #4cc2ff / #06131e / #e8615e (09-layoutBase/Dang nhap & Dang ky.dc.html:19 and :25,
      `--accent` / `--onAccent` / `--bad`; identical in all six student screens sampled). Replaces the
      placeholder #2563eb / #3b82f6. Overlay surfaces drawn ON an accent-filled panel stop hardcoding
      white/black and tint from `--color-on-primary` instead (`.on-accent-tint` / `.on-accent-outline`
      in globals.css). Scopes `.admin-shell` and `.instructor-shell` are untouched -- they already carry
      their own mockup palettes. `pnpm check` PASS (37 files, 111 tests).
    rationale: >
      Owner 2026-09-29: the Dark accent "chua phu hop" against the dark background. Root cause was not
      the hue but the pairing -- the placeholder Dark value #3b82f6 is a MID blue carrying near-black
      text, while the mockup pairs near-black text with a much LIGHTER sky blue, which is what makes it
      legible. globals.css already labelled these tokens a placeholder "not the final palette"
      (SYS0102_frontend_architecture.md section 6), so adopting the mockup is a graduation, not a
      divergence. Light was moved with Dark so the two stay one hue family. Rejected: inventing a new
      accent (no SoT), and porting the mockup's dark NEUTRALS (#0B0D11 bg / #14171D panel) in the same
      pass -- that touches every screen and deserves its own before/after review.
    source: "owner request 2026-09-29 (mau phan lai la xanh chua phu hop voi nen toi, tham khao layoutbase)"
    impacted_paths:
      - 05-coding/frontend/src/app/globals.css
      - 05-coding/frontend/src/shared/ui/auth-aside-panel.tsx
      - 05-coding/frontend/src/views/shared/auth/ui/auth-view.tsx
      - 09-layoutBase/
  - id: DEC-2026-1001-single-overview-page-kpi
    kind: product-scope
    status: ACTIVE
    date: 2026-10-01
    subject: "Only the overview pages show KPI; every list page shows header, filters, list and pagination only"
    decision: >
      Each area has exactly one page that shows KPI / overview figures: the Admin overview
      (`admin_overview`) and the Instructor overview. Every other management or monitoring page
      (problem_management, user_management, system_log, interview_question_management, queue_monitor,
      ai_usage) shows only the page header, filters, the list or table, and pagination. Side panels next
      to a list (role distribution, pending tasks, active admins, category breakdown, topic distribution,
      needs-attention) are NOT treated as KPI and stay. The "Yeu cau dat lai mat khau" figure that used
      to sit on the user-management strip moved to the Admin overview as a third stat card. The queue
      and AI-usage figures (waiting / running / throughput / failed, month tokens / daily average /
      estimated cost / calls) were removed with their strips and have no new home yet.
      Alongside, three UI conventions adopted the same day: row actions in list tables are icon-only
      (`IconAction`, lucide icon + tooltip below, danger tone for delete); the Admin sidebar is pinned
      to the viewport like the Instructor one; `PageHeader` has a `leading` slot (back icon on detail and
      authoring screens). The interview-question list became a table like the problem list, and the
      interview-question authoring screen became a full-width two-column layout loaded by route param.
    rationale: >
      The same four-card strip was repeated on every list page, saying nothing the overview did not,
      pushing the list below the fold, and costing one stats endpoint + DTO per screen in BD. One
      overview per area keeps the dashboards in one place and the list pages short. Owner instruction
      2026-10-01.
    consequences: >
      BD `SHR0201`, `SHR0301`, `ADM0201`, `ADM0403` drop the stat-strip area and the matching stats
      DTOs / endpoints (`UserManagementStatsDto` keeps only `pendingVerificationCount` for the
      "Can xu ly" panel); `queue_monitor` and `ai_usage` BDs follow. Not touched: student-area pages
      and `class_student_detail`, which still use stat cards.
    related:
      - DEC-2026-0830-problem-lifecycle-two-states
      - DEC-2026-0831-problem-management-lifecycle-details
    source: "06-plan/reports/261001-doc-sync-lists-and-shell.md"
  - id: DEC-2026-1001-admin-configurable-settings
    kind: product-scope
    status: ACTIVE
    date: 2026-10-01
    subject: "ADMIN configures every parameter and category list; interview-question topics become admin-managed data; password-reset card counts from the audit log"
    decision: >
      Owner principle (2026-10-01): a system administrator (A3) configures the system's parameters and
      category lists themselves and is not held to values fixed in code or seed data. Only integrity
      invariants stay fixed, and only where they protect that very ability. Applied as follows.
      (1) Interview-question TOPICS are admin-managed data. `question_topics` stops being a 5-row seed
      behind an ENUM: ADMIN creates, renames, reorders and deletes topics with no cap on their number;
      INSTRUCTOR (A2) can only pick one. The five original topics (CS_THEORY, SYSTEM_DESIGN, DATABASE,
      LANGUAGE, BEHAVIORAL) become ordinary seed rows. Deleting a topic is refused while any interview
      question references it. This supersedes ONLY sub-decision 4 (Q6, "5-topic taxonomy fixed") of
      `DEC-2026-0830-interview-bank-crud`; its other sub-decisions stand. Problem-bank `topics` (F2-02)
      follow the same treatment, see (5).
      (2) The "Yeu cau dat lai mat khau" card on the Admin overview counts a new audit-log action type
      written when a reset OTP is issued (category "Xac thuc"; the OTP itself is never stored). Redis
      cannot serve it: the OTP key lives 10 minutes.
      (3) The 13 limit-related proposals from the 2026-10-01 doc sync are adopted: autoscale threshold,
      jobs-per-page, SYSTEM_ERROR retention and latency-bar cap on queue_monitor; token price per model
      and budget colour threshold on ai_usage; admin sees and edits every problem while the author filter
      applies to A2 only; selectable page size 8/20/50 remembered per account; "Them tai khoan" put in
      scope for user_management (needs RD code F1-13 extended first); every ADMIN reads the whole audit
      log, retention period and expiry policy are configuration, CSV export has no row cap; no business
      limit on follow-up text length or rubric row count.
      (5) Follow-ups the owner delegated ("dong y, ban xu ly") on 2026-10-01: the STAR answer framework
      stops being tied to topic code BEHAVIORAL and becomes a per-topic flag `uses_star_framework`
      that ADMIN toggles, so BEHAVIORAL can be renamed or deleted like any topic; problem-bank
      `topics` (F2-02) get the SAME admin-managed treatment as interview topics (INSTRUCTOR only
      picks; delete refused while referenced; free-form `tags` unchanged); topic management reuses the
      existing management permission plus an ADMIN role check, no new Function; the password-reset
      card counts issued OTPs, not every "Quen mat khau" click; the endpoint `RenameQuestionTopic`
      becomes `UpdateQuestionTopic` (rename plus STAR flag); the cluster toggles and the autoscale
      threshold of queue_monitor are stored in PostgreSQL, a single-row table `judge.cluster_settings`
      (not Redis, which `judge-orchestration` deliberately does not use); `problems.author_id` is an
      immutable id reference without a physical cross-schema FK; RD codes F1-13, F1-14, F2-02, F6-01
      and F6-13 are amended in place, no new F-codes.
      (4) Two judgment calls inside (3), made under the owner's delegation: the guard "at least one
      active ADMIN always remains" is KEPT as a hard rule (it protects the ability to configure
      anything), while "do not lock yourself" and "do not demote yourself" become confirm-with-warning
      dialogs; audit-log rows stay immutable (no manual per-row delete) while retention and expiry
      policy are admin configuration.
      (6) Round 5, amended 2026-10-03 (owner instruction "admin configures difficulty like topics"):
      interview-question DIFFICULTY gets the same treatment as topics. `interview_questions.difficulty`
      stops being the ENUM EASY/MEDIUM/HARD and becomes `level_id`, a FK to a new admin-managed table
      `question_levels` (`id`, `code`, `display_name`, `sort_order`; no colour column). ADMIN creates,
      renames, reorders and deletes levels with no upper cap; INSTRUCTOR (A2) can only pick one. The
      three original levels become ordinary seed rows. Two invariants: a level is never deleted while
      any interview question references it, and at least ONE level always remains (the authoring form
      needs something to pick). Colour: ADMIN does not choose it; the three seed levels keep their
      badge colours and a level an admin adds renders with the neutral tone. Endpoints
      `ListQuestionLevels`, `CreateQuestionLevel`, `UpdateQuestionLevel`, `DeleteQuestionLevel`,
      `ReorderQuestionLevels` reuse `INTERVIEW_BANK_MANAGEMENT` plus an ADMIN role check, no new
      Function. RD code F6-01 is amended in place, no new F-code. The interviewer seniority levels of
      the mock-interview (intern..senior) are a different concept and stay fixed. Also decided 2026-10-03
      (owner: "bo sung cho toi va xu ly cac no con lai"): the student bank list gets a filter by level
      (AND with search, status, topic); the level `code` is an upper-case slug of the name, numbered on
      clash (same rule as the topic code, confirmed in DD); the manager popups (question topics, question
      levels, problem topics) can reorder (`sort_order`); the entry point for level management is the
      manager popup on `SHR0301`, ADMIN only, with no link from the authoring screen.
      (7) Round 6, 2026-10-03 (owner: "problem difficulty customisable, kept apart from interview
      difficulty"): problem DIFFICULTY (F2-02) gets the same treatment, in its OWN table. `problems.difficulty`
      stops being the ENUM EASY/MEDIUM/HARD and becomes `level_id`, a FK to `problem_levels` (`id`, `code`,
      `display_name`, `sort_order`). The two tables are deliberately NOT shared: F2 and F6 are separate bounded
      contexts, and an admin may want different labels in each. Same rules as (6): ADMIN creates, renames,
      reorders, deletes with no upper cap; INSTRUCTOR only picks; delete refused while any problem references
      the level and for the last remaining level; no colour column (seed levels keep their badge colours, new
      levels are neutral). Difficulty carries NO logic (it does not drive score or limits), it only classifies.
      Endpoints `ListProblemLevels`, `CreateProblemLevel`, `UpdateProblemLevel`, `ReorderProblemLevels`,
      `DeleteProblemLevel` reuse the problem-management permission plus an ADMIN role check, no new Function.
      Sort by difficulty follows `sort_order`. Dashboards: the admin overview difficulty chart draws one group
      per level in order (bar colours stay per series, not per level); the student dashboard difficulty card
      draws one row per level in order. RD code F2-02 is amended in place, no new F-code.
    rationale: >
      The owner wants the administrator to be able to set things up without asking a developer: the
      topic list was the worked example. Fixed enums and hard-coded limits force a code change and a
      decision entry for what is really content or tuning. The two kept invariants are the only places
      where "no limit" would let an admin remove the system's ability to be administered or remove the
      evidence of what admins did.
    consequences: >
      BD interview-bank (database, architecture, security) and screens SHR0301, SHR0302, USR0401 move
      topics to data with ADMIN-only endpoints; ADM0101, ADM0403 and identity BD gain the audit action
      type; 13 open questions across ADM0201, ADM0302, ADM0401, ADM0403, SHR0201, SHR0302 are closed.
      Problem-bank BD (`F2_problem-bank.md`, SHR0201, SHR0202, USR0101) follow the same topic change.
      Prototype: topic store is in-memory (`entities/interview-question/model/topic-store.ts`), manager
      modal opens from the interview-question list for ADMIN only; the config screens for thresholds,
      pricing and retention are decided in BD and not built.
      Round 5 (difficulty): BD interview-bank database gains `question_levels`; architecture and
      security follow; screens SHR0301 (new level manager), SHR0302, SHR0303, USR0401, USR0402 read
      the level label from data. Prototype: `entities/interview-question/model/level-store.ts`
      (in-memory), manager modal beside the topic manager, floor of one level enforced in
      `ManagedListDialog` through `keepAtLeast` and in the store (`minItems`); reorder through `onMove`.
      Round 6 (problem difficulty): BD problem-bank database gains `problem_levels`; architecture and security
      follow; screens SHR0201, SHR0202, SHR0203, USR0101-0103, USR0201, USR0202, USR0501, USR0601, ADM0101,
      ADM0302, INS0101, INS0202 read the level label from data. Prototype: `entities/problem/model/level-store.ts`
      built on the shared factory `shared/lib/level-list-store.ts`.
    related:
      - DEC-2026-0830-interview-bank-crud
      - DEC-2026-1001-single-overview-page-kpi
    source: "06-plan/reports/261001-doc-sync-round3.md; 06-plan/261003-1730-interview-level-managed-list.md"
  - id: DEC-2026-1002-split-detail-and-edit-pages
    kind: architecture
    status: ACTIVE
    date: 2026-10-02
    subject: "Content screens get a read-only detail page; the authoring form moves to /edit"
    decision: >
      For the two shared content screens, the base URL of a record becomes a READ-ONLY detail page and
      the existing authoring form moves under `/edit`. Admin first (owner instruction 2026-10-02), Instructor mounts
      done 2026-10-03 with the same shape (`/instructor/problems/...` and `/instructor/interview-questions/...`).
      Routes (admin): `/admin/problems/[problemId]` = detail (new slug `problem_info`, code SHR0203),
      `/admin/problems/[problemId]/edit` = authoring (`problem_authoring`, unchanged UI),
      `/admin/problems/new` = authoring in create mode (a static segment, so it is not captured by the
      dynamic one). `/admin/interview-questions/[questionId]` = detail (new slug
      `interview_question_info`, code SHR0303), `.../[questionId]/edit` and `.../new` = authoring.
      List rows link the title to the detail page; the edit icon links to `/edit`. The detail page has
      its own "Sua" button to `/edit`. This supersedes ONLY the route lock of `SHR0302`
      (open question Q6, 2026-09-01: authoring at `/[id]`) and the `[id]` authoring routes in
      `SHR0202`; the screens themselves, their actors and `DEC-2026-0825-shared-content-authoring-screens`
      stand.
    rationale: >
      A record needs a place to be read without the risk of editing it, and the list needs a click target
      that does not open a form. The base URL being the form made every row click an edit session.
      Putting edit under a suffix is the conventional split and keeps one URL per intent. Rejected:
      putting the detail under `/view` and leaving the form at the base URL, because the base URL would
      keep meaning "edit" and break the usual reading of a resource URL.
    consequences: >
      Frontend: two new slices `views/shared/problem-info` and `views/shared/interview-question-info`,
      six admin route files (three per entity), list edit icons repointed. New RD `SHR0203`, `SHR0303`
      and BD for both; BD sheet 3 (screen transitions) of SHR0201, SHR0202, SHR0301, SHR0302 gain the
      list to detail to edit path. DD is written after the prototype, as usual for screens. Known
      prototype gaps are in `06-plan/PROTOTYPE_DEBT.md` section 17. List, authoring and info views now take a
      `basePath` prop (resolved 2026-10-03), so no link is hard-coded to one area.
    related:
      - DEC-2026-0825-shared-content-authoring-screens
      - DEC-2026-0830-interview-bank-crud
    source: "06-plan/PROTOTYPE_DEBT.md section 17"
  - id: DEC-2026-1003-markdown-latex-libraries
    kind: tech-choice
    status: ACTIVE
    date: 2026-10-03
    subject: "Markdown and LaTeX preview libraries chosen: react-markdown, remark-gfm, remark-math, rehype-katex, katex"
    decision: >
      Read-only screens render author-written Markdown through one shared component
      `shared/ui/markdown-preview.tsx`: `react-markdown` 10.1.0 with `remark-gfm` 4.0.1 (tables, task
      lists), `remark-math` 6.0.0 and `rehype-katex` 7.0.1 for LaTeX, styled by `katex` 0.19.0 CSS.
      Versions are pinned exactly, like the rest of package.json. Raw HTML in the Markdown is NOT
      parsed: `rehype-raw` is not installed and must not be added without a real sanitizer. Authoring
      screens keep the raw Markdown in a textarea (owner instruction 2026-10-02/03); only read-only
      views show the rendered preview. This settles the "Markdown + LaTeX" row that
      `01-rd/system/SYS0102_frontend_architecture.md` section 1 left open. It does NOT settle the
      sanitizer: `shared/lib/sanitize-html.ts` still throws on purpose, and is needed only if raw
      HTML is ever allowed.
    rationale: >
      The statement and interview-question text are Markdown with LaTeX by RD (F2-01, F6-13), and the
      owner wants a rendered preview on the detail pages. react-markdown builds React elements instead
      of injecting an HTML string, so with no raw-HTML plugin there is no HTML injection path and no
      sanitizer is needed for this use. KaTeX renders synchronously and needs no runtime script
      beyond the bundle. Rejected: `marked` or `markdown-it` plus `dangerouslySetInnerHTML`, because that
      would force the sanitizer decision first.
    consequences: >
      Four runtime dependencies plus katex CSS in the frontend bundle (loaded where the component is
      imported). `problem_info` and `interview_question_info` render through the component. Later
      screens showing Markdown (student problem detail, AI feedback, solution review) reuse it instead
      of choosing again. README frontend and SYS0102 updated.
    related:
      - DEC-2026-1002-split-detail-and-edit-pages
    source: "05-coding/frontend/src/shared/ui/markdown-preview.tsx"
  - id: DEC-2026-1003-bd-core-sot-tier
    kind: process-policy
    status: ACTIVE
    date: 2026-10-03
    subject: "BD raised to the same SoT tier as RD (P2); reading order RD + BD, then DD, then prototype/code"
    decision: >
      In the SoT ladder (`CLAUDE.md` Rules, `.nexa/control/sot-precedence.md` section 2), `02-bd/**`
      moves from P3 to P2 beside `README.md` + `01-rd/**`. RD and BD carry equal weight and are read
      together first. BD is the core of the design (owner ruling 2026-10-03) and wins over RD on
      design detail (schema, flow, state, field names): follow BD, state the divergence, propose
      updating the RD. Exception: a conflict on business scope (a feature in or out, an actor's
      permission) is never settled by tier; ask the owner and record a decision. `03-dd/**`
      (+ `04-tdd/**`) is P3; code and prototype (`05-coding/`, `09-layoutBase/`) are P4, read last.
    rationale: >
      Owner instruction 2026-10-03: BD is the core of the design. Previously BD shared P3 with DD,
      so an RD statement automatically outranked it even where BD had refined or corrected it.
    consequences: >
      Agents open RD and BD for the same slug before DD or code. No existing document changes, but RD
      lines that contradict BD become drift to fix, not authority to follow.
      Replaces only the P2/P3 rows of the ladder; P0, P1 and the exclusions are untouched.
    related:
      - DEC-2026-0819-agentconfig-retarget-algoprep
    source: "owner instruction in session 2026-10-03"
  - id: DEC-2026-1003-toast-feedback-channel
    kind: architecture
    status: ACTIVE
    date: 2026-10-03
    subject: "One shared toast is the only channel for operation feedback and validation errors; no inline notice text"
    decision: >
      Every notification of an operation result (saved, deleted, searched, failed) and every input
      validation error goes through one toast in `shared`, mounted once in `app/providers`, so it
      covers the Admin, Instructor and Student areas and the sign-in screens. Toast sits at the
      top right, stacks, auto-dismisses after a short time (success and info 4 s, error and
      warning 7 s, paused on hover or focus) and has an X button. A field that fails validation only
      gets a red border and `aria-invalid`; its message is carried by the toast, one toast per
      submit. Inline per-field error text (`InlineFieldError`) is removed. Scope settled with the
      owner on 2026-10-03, reading the instruction as "toast must improve UX": (1) a toast reports
      something that just happened; text the user needs while acting stays on the page, so
      `NoticeTile` that is page content (scope notes, an AI-unavailable block replacing missing
      feedback, the F5-18 education notice, empty states) is kept, but a notice whose only job is to
      say why ONE control is locked is removed (owner, 2026-10-03: six such blocks were dropped);
      a control blocked by a page state looks disabled (`aria-disabled`) but stays clickable and a
      click raises a warning toast with the reason;
      (2) data-load error and empty placeholders (`ErrorState`, `EmptyState`, dashboard block
      states) are kept because they replace missing content; a retry the user pressed that fails
      again raises an error toast, the first load failure does not; (3) search raises a toast only
      when the user submits it (Enter) with the result count, never per keystroke. Plan:
      `06-plan/nexa-plan/261003-1443-toast-thong-bao-chung.md`.
    rationale: >
      Owner instruction 2026-10-03: toast for the whole project, no inline notice text, validation
      included. Replaces the earlier rule "errors show inline under the field, never a toast"
      (`02-bd/screens/shared/SHR0101_auth.md:187`, `01-rd/screens/shared/SHR0101_auth.md:92`, header
      comment of the old `shared/ui/inline-field-error.tsx`). Top right is the owner's choice
      (2026-10-03). It was first built bottom right so it would not cover the primary actions of
      `PageHeader`; the owner prefers the top right and accepts that a card can sit over those
      buttons for its few seconds.
    consequences: >
      New `shared/lib/toast-store.ts` and `shared/ui/toaster.tsx`. `TextField` and `TextArea` stop
      rendering error text. BD template, BD screens, RD `SHR0101` and `SYS0102` are re-worded to
      toast. Accessibility trade-off accepted: toast errors use `role="alert"`, longer duration and
      pause on hover or focus to compensate for the lost `aria-describedby` text.
    related:
      - DEC-2026-0825-frontend-base-architecture
    source: "owner instruction in session 2026-10-03"
  - id: DEC-2026-1003-ai-testcase-generation-quota
    kind: product-scope
    status: ACTIVE
    date: 2026-10-03
    subject: "F2-14 testcase generation is capped per problem (default 2, ADMIN-configurable); counter on problems, setting on Cấu hình AI"
    decision: >
      Each problem may run "Sinh tự động" (F2-14) at most N times, default 2. Only a SUCCESSFUL run
      (adds at least one draft testcase) counts; a failed run does not; approving or rejecting drafts
      does not refund an attempt. The counter is the server-owned column
      `problems.ai_testcase_generations_used` (02-bd/database/F2_problem-bank.md section 1.1); the client
      never counts. N is a global ADMIN setting on `ADM0301` Cấu hình AI, a separate card "Sinh
      testcase tự động" (Khu vực H), guarded by Function `AI_CONFIG`; where the value is stored is left
      to the singleton table `problem_bank_settings` in `problem-bank` (`ADM0301` Q12, closed 2026-10-03). Lowering N
      does not clear counters already used. The author sees only
      "Còn N/M lần", never the global setting. The generator script's own sandbox caps (run time, output
      size, RD F2_problem-bank.md:88-91) are NOT part of this decision: they are a separate card on the Vận hành group
      screen `ADM0501` (Khu vực F, owner 2026-10-03; storage `judge.generator_sandbox_settings`, defaults 30 s and 100 MB, `ADM0501` Q10 closed 2026-10-03). The generator
      script is written by the AI in PYTHON (owner 2026-10-03; RD does not name the language). Each click asks the AI for a NEW script, so a
      second run yields other cases; the fixed random seed only makes ONE script's output reproducible
      (RD F2-14 step 5).
    rationale: >
      Owner instruction 2026-10-03. Every run is an AI call (cost, F5-25 budget) and produces drafts the
      author must review, while a problem needs only 8 approved testcases, so 2 runs are enough. A failed
      run is the system's fault and must not spend the author's attempt. A client-side counter could be
      bypassed. The cap is an AI-usage limit, so it sits with the other AI limits on Cấu hình AI rather
      than with judging limits.
    consequences: >
      New column on `problems` (schema, hence this entry). `GetProblemForAuthoring` returns used and
      limit; `GenerateTestcasesWithAi` refuses with a quota error when used >= limit. New endpoints
      `GetAiTestcaseGenerationSettings` / `UpdateAiTestcaseGenerationSettings` (`ADM0301` Sheet 7.3).
      Prototype: both cards are built (`ADM0301` Khu vực H, `ADM0501` Khu vực F) and the authoring mock enforces the cap the ADMIN sets.
    related:
      - DEC-2026-1001-admin-configurable-settings
      - DEC-2026-0928-f2-14-generator-script
    source: "owner instruction in session 2026-10-03; 02-bd/screens/shared/SHR0202_problem_authoring.md Q17, Q18"
  - id: DEC-2026-1003-single-function-signature
    kind: scope
    status: ACTIVE
    date: 2026-10-03
    subject: "F2-03: one shared function signature per problem, per-language names derived; replaces three per-language signatures"
    decision: >
      A problem declares ONE function signature (function name, return type, parameters each with a name
      and a type) for Java, C++ and Python together, instead of three separate per-language signatures.
      The harness maps the types to each language, so only the spelling of the function name differs; it
      is derived from the shared name (Python snake_case, Java and C++ camelCase) and the author may
      override the name for one language (`name_overrides`). `function_signatures` keeps one row per
      `problem_specs` row (the `language` column and the (spec, language) unique key are dropped,
      `name_overrides JSONB` is added). Parameter NAMES stay: they appear in the starter code and in how a
      sample testcase is displayed to the learner (USR0102 `GetStarterCode`, `ListSampleTestcases`);
      the harness itself calls the function by position. Changes RD F2-03 wording ("theo từng ngôn ngữ").
    rationale: >
      Owner instruction 2026-10-03, after asking why parameter names are needed. Entering the same
      types three times is wasted input and invites the three rows drifting apart, while the type schema
      already maps ARRAY, LIST, LINKED_LIST and BINARY_TREE to each language's own type.
    consequences: >
      RD F2-03 and related lines reworded; BD SHR0202 Tab 4 (items 2 to 5, new 10 and 11),
      `FunctionSignatureDto` (no `language`, adds `nameOverrides`), EVT-25; `database/F2_problem-bank.md`
      section 1.5; USR0102 BD wording. The prototype Spec tab declares one signature and previews the
      starter line per language. Migration note for DD: a future merge of the four signature columns
      into `problem_specs` is possible because the relation is now 1-1.
    related:
      - DEC-2026-0831-problem-authoring-spec-tab
      - DEC-2026-0824-dual-submission-model-per-problem
    source: "owner instruction in session 2026-10-03; 02-bd/screens/shared/SHR0202_problem_authoring.md Q19"
  - id: DEC-2026-1005-list-screens-open-questions-closed
    kind: process-policy
    status: ACTIVE
    date: 2026-10-05
    subject: "Closes the open questions of the problem and interview-question list screens, and fixes the UI conventions for list filters, selection and paging"
    decision: >
      Owner accepted all proposals of the 2026-10-05 UX review of `SHR0201` and `SHR0301`
      (`06-plan/reports/261005-1056-ux-review-shr0201-shr0301/`, section 14). Product and data choices:
      (1) rows per page is remembered PER ACCOUNT (this restates `DEC-2026-1001-admin-configurable-settings`);
      the browser store `usePersistedPageSize` is only a stand-in until an account-settings API exists.
      (2) The interview-question list gets row selection with ONE bulk action, Delete (soft delete per F6-13,
      confirm dialog, warning when selected rows are hidden by the filter); bulk Duplicate is NOT offered,
      Duplicate stays a per-row action that opens the authoring form pre-filled (`/new?from=<code>`) and
      creates nothing until Save. (3) Bulk publish on a mixed set is handled per problem with a summary
      ("published 5/8, 3 lack a Hidden testcase", F2-15). (4) A new difficulty level's `code` is generated by
      the server: unaccented, lower case, hyphen-joined, `-2` on collision, immutable after create.
      (5) The "Diem TB" column comes from practice sessions (F6-07, F6-08) via a read model; "-" until it exists.
      (6) The interview-question code `IQ-014` is issued by the server from a sequence, format `IQ-%03d`.
      (7) The user-management list keeps a stub pagination until its list API exists; the rows-per-page
      picker is switched on only for tables that really paginate. UI conventions: a list filter is the shared
      `FilterMenu` (a button opening a glass pop-up); the toolbar above a list is the shared `FilterBar`; the
      selection actions are the shared `BulkActionBar` floating at the top of the screen; unbounded option sets
      (class tabs) use `FilterMenu`, fixed tabs and mode switches keep `SegmentedTabs`. Text contrast is a hard
      requirement of at least 4.5:1 for body text on the glass, so the muted/subtle text, badge text, call-to-action and
      status tokens were darkened in the Admin and Instructor shells. The CSV import button stays visible
      while building and is hidden for demos with `NEXT_PUBLIC_DEMO_MODE=true`.
    rationale: >
      The questions had been open for weeks and blocked DD; each proposal was the option with the least invented
      behaviour (bulk Duplicate has no definition in the BD, and N forms or N silent copies are both
      surprising). Contrast was measured on the real glass render, not estimated: several tokens from the
      mockups were 2.2 to 4.2:1.
    consequences: >
      BD `SHR0201` (V1.18+) and `SHR0301` (V1.15+) record the closed questions; `03-dd/api/F2_problem-bank.md` and
      `03-dd/api/F6_interview-bank.md` must define: bulk publish result shape, level `code` generation, the
      account page-size setting, `ListInterviewQuestions` with average score, server-issued `IQ-` codes, and the
      duplicate pre-fill route. The palette tokens are a cross-screen visual change that still needs the owner's
      sign-off on the look; they are listed in `06-plan/PROTOTYPE_DEBT.md` section 25.
    related:
      - DEC-2026-1001-admin-configurable-settings
      - DEC-2026-0830-interview-bank-crud
      - DEC-2026-0830-problem-lifecycle-two-states
      - DEC-2026-1003-toast-feedback-channel
    source: "owner instruction in session 2026-10-05 (all proposals accepted); 06-plan/reports/261005-1056-ux-review-shr0201-shr0301/report.md section 14"
  - id: DEC-2026-1008-admin-system-screens-review
    kind: process-policy
    status: ACTIVE
    date: 2026-10-08
    subject: "Closes four open questions of the Admin System group (users, system log, permission matrix) and fixes the per-account action touchpoint"
    decision: >
      Owner accepted all four proposals of the 2026-10-08 review of `ADM0201`, `ADM0403` and `ADM0202`.
      (1) `ADM0202` Q1: a permission cell is saved the moment it is toggled (`UpdateRolePermission`), which is the
      literal reading of F1-10 "có hiệu lực ngay"; the header "Lưu thay đổi" button is removed and Q5 (confirm on
      leaving the screen) disappears with it. (2) `ADM0202` Q4: removing the last `PERMISSION_MATRIX:UPDATE` held by
      any role whose `base_category` is `ADMIN` is a HARD block, enforced on the server; turning off a cell of the
      role the acting admin holds is only a confirmation warning (same stance as `ADM0201` Q6, which allows
      self-lock and self-demotion with a warning). (3) `ADM0403` Q6: the log list gets an inclusive "Từ ngày / Đến ngày"
      day-range filter; a lock row shows the admin-typed reason (F1-32), an unlock row shows none. (4) `ADM0201` Q7:
      change-role, reset-password and lock/unlock for ONE account are icon actions on the table row; the bulk bar
      keeps only lock and unlock. A new role name is unique (by generated code and by label, case-insensitive) and at
      most 50 characters.
    rationale: >
      F1-10 says the change applies immediately, so a save step contradicts the requirement and leaves a button
      that does nothing. Removing every ADMIN's right to edit the matrix leaves no way back except raw SQL, so it is
      an invariant like "at least one active ADMIN remains". The log has 12,408 rows behind a 50-row page, so
      reaching an old day without a date filter takes hundreds of clicks. Moving reset-password and change-role out
      of the bulk bar (`ADM0201` Q7) without giving them a per-account home would have made F1-13 unusable.
    consequences: >
      DD `identity` defines: `UpdateRolePermission` rejecting the last-matrix-admin removal with a business error
      code; the audit-log list endpoint taking `from`/`to` day parameters; `ChangeUserRole`,
      `ResetUserPassword` and `ChangeUserStatus` as single-account endpoints. Open follow-ups, not decided here:
      which `base_category` a custom role gets (`ADM0202` Q6), and how reset-password treats an OAuth-only account
      (`ADM0201`). BD and RD of the three screens carry the change in their revision tables.
    related:
      - DEC-2026-1001-admin-configurable-settings
      - DEC-2026-1003-toast-feedback-channel
      - DEC-2026-1005-list-screens-open-questions-closed
    source: "owner instruction in session 2026-10-08 (all four proposals accepted); 02-bd/screens/admin/ADM0202_permission_matrix.md Q1 Q4, 02-bd/screens/admin/ADM0403_system_log.md Q6, 01-rd/screens/admin/ADM0201_user_management.md Q7"
  - id: DEC-2026-1008-admin-system-open-questions-closed
    kind: process-policy
    status: ACTIVE
    date: 2026-10-08
    subject: "Closes the remaining open questions of the permission matrix and the system log that the 2026-10-08 review listed for the owner"
    decision: >
      Owner accepted the proposals for: (1) `ADM0202` Q6: the create-role form picks a base category
      (STUDENT, INSTRUCTOR, ADMIN), default STUDENT; cells are locked only for the SYSTEM role STUDENT, never by
      category, so every custom role stays editable. (2) `ADM0202` Q2: a custom role still held by accounts cannot be
      deleted (blocked, not auto-reassigned). (3) `ADM0202` Q3: no role renaming; "sửa" in F1-11 means editing the
      matrix cells. (4) `ADM0403` Q7: the "Lưu trữ" setting stays on the log screen, with a second confirmation
      when the retention period gets shorter or the policy becomes Delete. (5) `ADM0403` Q8: the export dialog
      applies the filters on screen when it opens, and refuses a start date older than the retention period or a
      date in the future. (6) `ADM0403` Q3: "Quản trị viên hoạt động" counts a 24-hour window.
    rationale: >
      Least invented behaviour in each case: a default of the lowest category cannot widen anyone's access by
      accident; blocking deletion avoids silently moving accounts onto a role the admin did not pick; the export
      should match what the admin was just looking at, and a date outside the retained window can only return
      nothing.
    consequences: >
      DD `identity` defines: `CreateRole` taking `baseCategory`; `DeleteRole` rejecting a role with accounts
      (business error with the count); the list of roles carrying `userCount`; the audit-log export taking the
      list filters and enforcing the date window server side; a place to store archived log (BD `ADM0403`
      Q7 remainder). Still open, not decided here: where the "Nén và lưu trữ" archive lives and how it is read back.
    related:
      - DEC-2026-1008-admin-system-screens-review
      - DEC-2026-1001-admin-configurable-settings
    source: "owner instruction in session 2026-10-08 (proposals accepted); 02-bd/screens/admin/ADM0202_permission_matrix.md Q2 Q3 Q6, 02-bd/screens/admin/ADM0403_system_log.md Q3 Q7 Q8"
  - id: DEC-2026-1008-problem-delete-and-duplicate
    kind: process-policy
    status: ACTIVE
    date: 2026-10-08
    subject: "Soft-delete semantics for problems, one-at-a-time problem duplication, interview-question duplication removed, CSV import real"
    decision: >
      Owner accepted all recommendations of the 2026-10-08 review of `SHR0201` and `SHR0301`. (1) Deleting a problem
      sets `status = UNPUBLISHED` and `deleted = true` (RD wins over the old BD wording "delete does not change
      status"); submissions, scores, bookmarks and class history are kept. (2) The delete confirmation drops "cannot
      be undone" and instead warns, without blocking, about the number of submissions and classes the problem is
      assigned to; bulk delete uses one shared dialog and rejects no problem (report option A, 06-plan/reports/
      261008-1120-problem-delete-with-submissions). (3) F2-16 stays: duplicating a problem opens the authoring form
      pre-filled (`/new?from=<id>`), one problem at a time, nothing is created until Save. (4) Duplication of
      interview questions is removed everywhere (`SHR0301` row and bulk, `SHR0302` admin block, endpoint
      `DuplicateInterviewQuestion`), superseding the duplicate part of `DEC-2026-0830-interview-bank-crud` (F6-13)
      and `SHR0302` Q4. (5) CSV import of interview questions (F6-13) is built for real; the file format is an
      confirmed by the owner in `SHR0301` Q11 on 2026-10-08. (6) `/instructor` mount of the two screens is kept
      mock-only until the DD defines role-scoped access. (7) CSV format fixed (required columns question, topic, level, rubric; optional content, suggested_approach, core_keywords, sample_answer; duplicate content skipped, never overwritten; max 500 rows). (8) Rule for "problem withdrawn from the bank" on learner history and progress screens is recorded in `02-bd/database/F2_problem-bank.md` section 1.1a (history kept, no link, excluded from progress denominator). (9) 2026-10-09 UI review of the two screens: glass dialog effect kept but the dark dialog fill is made more opaque; the bulk-bar label states how many selected rows the current filter hides; sub-header counts come from the same mock rows as the list; toast stays top-right.
    rationale: >
      A hidden problem must not stay "published", and keeping all learner data is what the 2026-08-31 decision
      already promised. A warning is enough while there is no restore path and no DD; blocking would make admin
      clean-up depend on every instructor class. Duplicating a problem is a real authoring need (variants),
      duplicating a question was judged unnecessary by the owner.
    consequences: >
      BD `database/problem-bank` line 19 changes; DD `problem-bank` must return `submissionCount` and
      `assignedClassCount` on delete and bulk delete. Open follow-ups: restore of deleted problems (G3) and the
      "problem withdrawn" rule on learner history screens (G4).
    related:
      - DEC-2026-0831-problem-management-lifecycle-details
      - DEC-2026-0830-interview-bank-crud
      - DEC-2026-1005-list-screens-open-questions-closed
    source: "owner instruction in session 2026-10-08 (all recommendations accepted); 06-plan/reports/261008-1120-problem-delete-with-submissions/report.md"
  - id: DEC-2026-1009-ui-review-screenshot-evidence
    kind: process-policy
    status: ACTIVE
    date: 2026-10-09
    subject: "UI and prototype review findings must carry real Playwright screenshot evidence"
    decision: >
      Every visible finding in a UI or prototype review report carries a real Playwright image taken on the
      running Next.js app: the full frame with a red box plus a zoomed crop, both themes when relevant, and a
      before and after pair once fixed. The finding table has a Figure column. The report states the images
      come from a capture script, not from `pnpm e2e`. Implemented by `.claude/rules/ui-review-evidence.md`
      and the skill `.claude/skills/ui-evidence-review/` (script `scripts/capture-evidence.cjs`).
    rationale: >
      The owner reviews by opening `report.html` and checking each Figure; prose alone cannot be verified
      and the 2026-10-08 review showed severities changing once the real image was seen (F2 was lowered).
    consequences: >
      `nexa-ux-review` points to the rule and skill for its evidence step. A finding that cannot be captured
      is marked `[Không có ảnh]` with the reason.
    related:
      - DEC-2026-1003-toast-feedback-channel
    source: "owner instruction in session 2026-10-09; 06-plan/reports/261008-1500-ux-screenshots-shr0201-shr0301/report.md"
  - id: DEC-2026-1010-admin-selects-model-per-prompt
    kind: product-scope
    status: ACTIVE
    date: 2026-10-10
    subject: "ADMIN picks the LLM model per prompt from a dev-declared list; temperature is not shown; API key stays in backend infrastructure"
    decision: >
      The API key of the LLM provider (for example an OpenRouter key) stays in backend infrastructure
      (environment or secret manager, 02-bd/security/F5_ai-review.md) and never reaches the UI or the
      database. The dev declares the approved model ids in backend config. On `ADM0301`, each prompt card
      shows a Model select whose options are that list; the choice is stored with the prompt version
      (`ai.prompt_templates.model_id`) and goes through "Lưu và phát hành" with the rest of the prompt. The
      admin never types a free model id. Temperature is no longer shown (it was only a read-only mockup
      field, RD F5-23 never asked for it); the max-token limit stays as read-only text because it drives
      per-call cost. The per-model price table in the "Ngân sách và đơn giá" card is unchanged for now;
      whether the gateway returns cost itself is an open question (`ADM0301` Q14).
    rationale: >
      Owner instruction 2026-10-10 after discussing a remote gateway: with one key and a model id string,
      switching model is cheap, and showing temperature gave the admin nothing to act on. An allow-list
      stops the admin from picking an unsupported or expensive model. Reopens `ADM0301` Q7 for the model
      field only (the 2026-09-20 closing said there was no requirement to change model; there is one now).
    consequences: >
      New column `model_id` on `ai.prompt_templates` (schema, hence this entry). `ListPromptTemplates` also
      returns the approved list; `UpdatePromptTemplateDraft` takes `modelId` and the server rejects an id
      outside the list. Prototype: `views/admin/ai-config` Model select, temperature removed.
    related:
      - DEC-2026-1001-admin-configurable-settings
    source: "owner instruction in session 2026-10-10"
  - id: DEC-2026-1010-split-ai-admin-screens
    kind: product-scope
    status: ACTIVE
    date: 2026-10-10
    subject: "AI admin group splits into three screens by purpose: quality (ADM0301), limits and cost (ADM0303, new), monitoring (ADM0302)"
    decision: >
      The "AI" nav group has three screens, one purpose and one save model each. `ADM0301` (slug
      `admin_ai_config`, shown as "Prompt và chất lượng AI") tunes AI quality per feature on tabs (Phân tích
      bài giải, Phỏng vấn giả lập, Sinh testcase): prompt, model, rubric (review tab only), response guards,
      version log of the selected feature; draft then publish. `ADM0303` (slug `admin_ai_limits`, code
      ADM0303, "Hạn mức và chi phí AI") is new and writes immediately: monthly token limit and the unlock
      action (guarded by `AI_TOKEN_BUDGET`), rate limits, budget warning threshold, per-model price table,
      per-problem testcase generation cap (guarded by `AI_CONFIG`). `ADM0302` (`admin_ai_usage`) stays
      read-only monitoring and links to `ADM0303`. Owner answers 2026-10-10: rate limits are per HOUR (10
      reviews, 3 interview sessions, 20 s wait; matches 02-bd/database/F5_ai-review.md section 6); response
      guards are stored per feature with the prompt version and are available on the two feedback tabs;
      "Không đưa lời giải đầy đủ" is locked on and read-only (F5-17, F5-18 are hard constraints); the
      monthly limit and unlock live on `ADM0303` (closes M-01 of the 2026-10-09 report). A paid subscription
      tier that raises a user's AI quota is recorded as a future idea only (ADM0303 Q6), not designed.
    rationale: >
      `ADM0301` mixed "make the AI better" with "cap the spend" (7 cards, two save models, M-09) and the
      monthly limit plus unlock had no owner because `ADM0301` and `ADM0302` each pointed at the other
      (M-01). One purpose per screen keeps each page short, gives one save model per page, and puts every
      setting somewhere. Rubric applies only to SOLUTION_REVIEW, so it belongs on that tab.
    consequences: >
      New RD and BD `ADM0303_ai_limits.md`; BD `ADM0301` V1.0, `ADM0302` edited; column `response_guards`
      on `ai.prompt_templates` (schema, hence this entry). Supersedes in part
      `DEC-2026-1001-admin-configurable-settings` and `DEC-2026-1003-ai-testcase-generation-quota` only about
      WHERE the cards live (they move from `ADM0301` to `ADM0303`); their rules stand. Prototype not yet
      rebuilt (06-plan/261010-1902-ai-admin-tach-man-don-gian-hoa.md phases 2 and 3).
    related:
      - DEC-2026-1010-admin-selects-model-per-prompt
      - DEC-2026-1001-admin-configurable-settings
      - DEC-2026-1003-ai-testcase-generation-quota
    source: "owner instruction in session 2026-10-10"
  - id: DEC-2026-1010-admin-operations-regroup
    kind: product-scope
    status: ACTIVE
    date: 2026-10-10
    subject: "Admin Operations group regrouped into three screens (ADM0401 queue, ADM0404 infra events - new, ADM0501 languages) with simpler layouts; schema changes for language/sandbox/defaults/queue settings"
    decision: >
      Owner instruction 2026-10-10 ("ok, trien khai") after the review in
      06-plan/reports/261010-1030-admin-van-hanh-userflow-va-mau-thuan/report.md: the two Operations screens were
      too dense for an end user. OWNER-APPROVED parts: (1) New screen ADM0404 "Su kien ha tang" (slug
      admin_infra_events, route /admin/infra-events, Bounded Context judge-orchestration, read-only: search, level
      filter, service filter, date range, load 20 more rows) in nav group Van hanh between "Hang doi cham" and
      "Ngon ngu va gioi han"; the infra-event block leaves ADM0401, which keeps one link card with warning/error
      counts. (2) ADM0401 shows a one-line status strip (Binh thuong / Dang don N job / Dang tam dung / Worker qua
      tai, plus "Cap nhat luc" and "Du lieu cu" after 60 s) that is NOT a KPI card, so
      DEC-2026-1001-single-overview-page-kpi still holds; compact worker table; one "Dieu khien" card (average
      wait + the two toggles) with a confirm dialog for pause ON and autoscale OFF; job table with count tabs
      (Dang cho / Loi / Tat ca), 4 columns, paginated by the jobsPerPage parameter; a "Cau hinh" button for the
      four-parameter popup; the refresh button really reloads. Latency bar width = seconds / latencyCapSeconds.
      Refresh: STOMP topic /topic/admin/queue every 5 s, REST polling every 15 s while disconnected, manual
      refresh kept. Worker-cluster figures are read from Prometheus (already in the stack), no new table
      [SoT: Suy luan - go-judge exposing the metric is unverified]; infra events = Prometheus alerts plus stuck
      Outbox warnings. The "Tham so hang doi" popup stays on ADM0401 (viewing needs JUDGE_QUEUE_MONITOR, writing
      needs ADMIN) and its three missing parameters become columns of judge.cluster_settings. (3) ADM0501 has two
      tabs ("Ngon ngu va gioi han", "Sandbox va sinh testcase"); a compact defaults row above a 4-column table
      (language + compiler, time and RAM multipliers, effective limit "x ms - y MB", enabled); the "Ma moi truong
      cham" column and the "Luu y khi doi gioi han" card are dropped (a one-line note sits at the save bar); a
      sticky bottom save bar shows the change count, Hoan tac, Luu; ONE confirm at Save only when a language goes
      enabled -> disabled; unsaved-changes guard is a shared Admin form behaviour (implemented only where the
      prototype wires it). Schema: judge.language_configs gains enabled BOOLEAN DEFAULT true; the four grading
      defaults become editable in a new one-row table judge.grading_defaults; sandbox is GLOBAL, so its three
      columns leave language_configs for a one-row table judge.sandbox_settings (the child-process limit is a
      toggle plus a number when on); the memory multiplier stays and Java is seeded 2.00 (C++ and Python 1.00);
      judge.cluster_settings gains jobs_per_page, error_retention_hours, latency_cap_seconds. Rules: (G1) while
      queue_paused = true the timeout sweep (F4-07, 5 min) skips PENDING jobs; (G2) every operations write (pause or
      resume, autoscale, queue parameters, language multipliers or enable, defaults, sandbox, generator limits) is
      written to the system log under the EXISTING category "Cau hinh" with old and new values in after_json; (G3) a
      changed configuration applies from the moment a job STARTS grading, not from submission time; (G4)
      disabling a language does not cancel queued jobs of that language, they run to completion and only new
      submissions are refused.
      PONYTAIL CLOSURES, OWNER-APPROVED 2026-10-10 ("ok trien khai", criteria in
      .claude/rules/ponytail-criteria.md; the rung that decided each item is named): (4) rung 1 and 2 - remove the
      job "priority" concept, the "Hang doi uu tien cao" latency level and the "Job" id column from ADM0401; latency
      becomes one average-wait figure (ADM0401 Q4 closed). This supersedes the "Uu tien cao label stays" part of
      DEC-2026-0831-judge-orchestration-ops-details. (5) rung 3 and 5 - ADM0401 Q8: NO worker ceiling and NO
      scale-down parameters; "toi da 8" is a constant in application.yml. (6) rung 3 - ADM0401 Q9: NO soft warnings
      for extreme values, the existing minimum of 1 is enough. (7) rung 3 - ADM0404 Q1: no auto-refresh; load on
      entry, on filter change and on a manual "Lam moi" button. (8) rung 2, 4, 5 - ADM0404 Q2: infra events come
      from TWO real sources only: stuck Outbox rows (outbox_events.publish_attempts, constants in application.yml:
      3 attempts = WARN, 5 attempts = ERROR) and judge submissions in SYSTEM_ERROR or sandbox error
      (judge.submissions plus the timeout sweep); AI Gateway, backup, db, MinIO and Redis are NOT sources. The
      "worker cluster figures from Prometheus" part of ADM0401 Q2 stays [SoT: Suy luan]. (9) rung 4 and 5 -
      ADM0404 Q3: retention is the same 90 days as the system log, no config field. (10) rung 3 - ADM0404 Q4: opening
      from the link card while its tone is error pre-sets the level filter through ?level=error (valid values
      error|warn|info, anything else ignored). The exception to rung 5 is DEC-2026-1001-admin-configurable-settings:
      existing inputs stay, no NEW config fields are added. OPEN TECHNICAL RISKS (not owner questions): R1 verify that
      go-judge exposes the worker metrics before the DD, and if not, rung 2 says the worker block has no real source
      and must be dropped or redefined; R2 real autoscale on Docker Compose is hard, the toggle may only write the
      flag and be demoed with mock, to be revisited at the DD.
    rationale: >
      One screen per audience-time: the infra-event log is read only during an incident and has its own need for
      filters and history, so it leaves the queue screen; setup-once cards (sandbox, generator limits) move to a tab
      so the daily table stays short. Exception-first layouts (status strip, tabs with counts, confirm only on
      risky changes) cut the number of things shown at once without removing any setting, which keeps
      DEC-2026-1001-admin-configurable-settings intact. Findings F1-F12 and gaps G1-G6 of the 2026-10-10 report
      showed that pause could silently trip the 5-minute sweep (G1) and that operations writes left no audit trail
      (G2), so those rules are fixed here instead of left implicit. A priority flag has no data source (ADM0401 Q4),
      so showing it only invents data; that is why it is removed (owner-confirmed 2026-10-10).
    consequences: >
      Schema (hence this entry): language_configs.enabled; new judge.grading_defaults; new judge.sandbox_settings
      replacing the three sandbox columns of language_configs; three new columns of judge.cluster_settings. New RD
      and BD ADM0404; BD ADM0401 and ADM0501 revised to V1.0; nav has 12 destinations. Updates RD F4-07 and F4-10
      amendments, BD architecture sweep (5.3), BD database (1.5, 1.7, new 1.9 and 1.10), BD ADM0403 category
      note. Prototype (05-coding/frontend) is rebuilt in parallel by another worker; PROTOTYPE_DEBT section 32
      records what is still mock.
    supersedes_in_part:
      - DEC-2026-0922-users-and-admin-conflict-resolutions (sandbox columns now live in a singleton table, not on language_configs)
      - DEC-2026-0831-judge-orchestration-ops-details (priority label, confirmed 2026-10-10)
    related:
      - DEC-2026-1001-admin-configurable-settings
      - DEC-2026-1001-single-overview-page-kpi
      - DEC-2026-0828-remove-rejudge-scope
    source: "owner instruction in session 2026-10-10"
  - id: DEC-2026-1010-ai-generate-testcase-feature-code
    kind: schema
    status: ACTIVE
    date: 2026-10-10
    subject: "ai.feature_code gets a third value GENERATE_TESTCASE so F2-14 has a prompt and a token ledger without a new table or port"
    decision: >
      The ENUM `feature_code` on `ai.prompt_templates` and `ai.token_usage` (02-bd/database/F5_ai-review.md
      sections 1.1 and 1.6) gains `GENERATE_TESTCASE`. The migration seeds a third ACTIVE prompt, for the
      script-writing prompt of F2-14. `token_usage.owner_id` is the `problems.id` for this value, so
      the "Bài toán tốn token nhất" ranking on `ADM0302` can sum review, interview and testcase tokens per
      problem. `rubric_configs` does not apply to it (rubric is SOLUTION_REVIEW only, F5-23), and the hourly
      rate limits do not apply either: F2-14 already has its own per-problem cap
      (`DEC-2026-1003-ai-testcase-generation-quota`). How `problem-bank` reaches the LLM (a port into
      `ai-review`, not a direct import) is left to the DD; this entry only fixes where the prompt and the
      token ledger live.
    rationale: >
      Ponytail rung 4 (reuse): `prompt_templates` and `token_usage` already do exactly what the testcase
      prompt and its token count need; one enum value costs less than a second prompt table or a second
      ledger, and it closes `ADM0301` Q1, `ADM0302` Q3 and Q8 at once. Rung 2: the prototype already shows a
      testcase tab and a 4,2 tr share that had no table behind them. Owner accepted the proposal
      2026-10-10 (report 06-plan/reports/261010-2041-ai-admin-doi-chieu-hien-trang/, V-05).
    consequences: >
      Schema change (enum value, seed row, `owner_id` meaning). `ADM0301` tab "Sinh testcase" and the
      `ADM0302` feature share are now backed by real tables. DD `ai-review` must define the entry point
      `problem-bank` calls; `03-dd/api/F5_ai-review.md` not written yet.
    related:
      - DEC-2026-1003-ai-testcase-generation-quota
      - DEC-2026-0928-f2-14-generator-script
      - DEC-2026-1010-split-ai-admin-screens
    source: "owner instruction in session 2026-10-10"
  - id: DEC-2026-1010-ai-rate-limit-fixed-window
    kind: tech-choice
    status: ACTIVE
    date: 2026-10-10
    subject: "F5-19 hourly limits use a fixed one-hour window on Redis, plus a separate cooldown key, instead of a sliding window"
    decision: >
      Per-user AI rate limits (F5-19) count in a fixed window per clock hour: key
      `ai:ratelimit:<userId>:<featureCode>:<epochHour>` with INCR and an expiry of one hour set on the first
      increment. The "chờ giữa hai yêu cầu" setting is a separate key `ai:cooldown:<userId>` written with SET
      NX and an expiry equal to the configured seconds. Both limits apply to SOLUTION_REVIEW and
      MOCK_INTERVIEW only (not GENERATE_TESTCASE, which has its own per-problem cap). Cache hits are not counted
      (02-bd/architecture/F5_ai-review.md section 6.1, unchanged). `interview-bank` keeps its own namespace
      (`interview_bank:ratelimit:<userId>`) and is not changed by this entry.
    rationale: >
      Ponytail rung 5 (sensible default as a constant): two Redis commands and no per-request timestamp list,
      against the sliding window that needs a sorted set per user. Known ceiling: a user can use two full
      quotas around an hour boundary (10 calls at 10:59 and 10 at 11:00); upgrade to a sliding window only if
      that is abused (marked with a `ponytail:` comment in the code). Owner accepted the proposal 2026-10-10
      (V-09 of the same report). The limit values stay configurable on `ADM0303` by
      `DEC-2026-1001-admin-configurable-settings`.
    consequences: >
      02-bd/database/F5_ai-review.md (Redis keys) and 02-bd/architecture/F5_ai-review.md section 6.1 change from
      "cửa sổ trượt" to the fixed window; `ADM0303` Q1 closed. DD `ai-review` writes the exact commands.
    related:
      - DEC-2026-1010-split-ai-admin-screens
      - DEC-2026-1001-admin-configurable-settings
    source: "owner instruction in session 2026-10-10"
  - id: DEC-2026-1011-admin-system-open-questions-closed
    kind: product-scope
    status: ACTIVE
    date: 2026-10-11
    subject: "Admin System group (ADM0201 users, ADM0202 permission matrix, ADM0403 system log): open questions H1-H5 closed; users.status gains PENDING_VERIFICATION, ListUsers returns hasPassword, new identity.system_settings key-value table"
    decision: >
      Owner instruction 2026-10-11 ("ok trien khai theo de xuat") after
      06-plan/reports/261011-1827-admin-he-thong-cau-hoi-mo/report.md. (H1, ponytail rung 4 and 1) the "Cho xac thuc
      email" state is a THIRD VALUE of the existing column users.status: ENUM(ACTIVE, DEACTIVATED, PENDING_VERIFICATION);
      no new column and no Redis TTL key (a TTL key cannot be listed or filtered). A self-registered account (F1-01)
      and an account created by an Admin both start as PENDING_VERIFICATION and become ACTIVE when the email is
      verified. (H2, rung 4 and 6) the ListUsers response carries a boolean hasPassword computed as
      password_hash IS NOT NULL, NO new column; when hasPassword = false the reset-password popup says the server
      sends guidance to go back to the OAuth provider and the server still re-checks (ADM0201 BD Sheet 9 NO 10).
      (H3, rung 1) add POST /api/v1/identity/admin/users (CreateUserAccount): displayName, email, roleId; roleId must
      belong to a role whose base_category is not ADMIN; the temporary password is emailed; the new account is
      PENDING_VERIFICATION and must change the password on first sign-in; one system_audit_logs row per creation.
      (H4, rung 5 and 4) one enum AuditAction in identity carries, per action_type, the display category, the service
      name and the i18n message key; the UI builds the sentence from the key plus parameters read from after_json;
      the masked-key list stays the constant already fixed in DD BR-16 (no configuration). (H5 c, rung 1) a small
      key-value table identity.system_settings(key PK, value_json, updated_at, updated_by FK) holds the log retention
      settings (audit_log.retention_days default 90, audit_log.expired_policy), written by PUT
      /api/v1/identity/admin/settings/log-retention which needs Function SYSTEM_AUDIT_LOG. (H5 b, rung 3) the
      "Nen va luu tru" (compress and archive) policy is DEFERRED: the value stays selectable in the popup but the
      cleanup job treats it as "keep the rows" and never deletes, so no audit data is lost (rung 6); only the
      "Xoa han" (delete) policy is implemented for the demo. Also closed as wording debt: ADM0201 BD Q5 (no longer
      applies since the KPI strip was removed 2026-10-01) and the stale 90-day sentence in RD ADM0403.
    rationale: >
      The cheapest design that keeps every confirmed requirement: reuse users.status and password_hash instead of new
      columns (rung 4); one key-value table serves the retention parameters and any later admin setting of identity
      without a table per setting (rung 1 via DEC-2026-1001-admin-configurable-settings); archiving has no demo use
      and building it first would add MinIO read-back for nothing (rung 3), while keeping rows instead of deleting
      preserves the immutability of the audit trail (rung 6).
    consequences: >
      Schema (hence this entry): users.status third value; new identity.system_settings. API: 3 new endpoints (#50 to
      #52) and two new fields on ListUsers (hasPassword, pageSize). Updates BD database identity (1.1, new 1.13, 4, 5),
      DD api, logic and validation of identity, BD and RD of ADM0201 and ADM0403, and PROTOTYPE_DEBT. Prototype:
      mock rows get hasPassword and the reset-password popup text follows it. DD BR-15 still describes self-lock and
      self-demote as hard blocks (IDT-430, IDT-431) while ADM0201 Q6 turned them into confirm warnings on 2026-10-01;
      that mismatch is recorded in the DD, not changed here.
    related:
      - DEC-2026-1001-admin-configurable-settings
      - DEC-2026-1008-admin-system-open-questions-closed
      - DEC-2026-1008-admin-system-screens-review
    source: "owner instruction in session 2026-10-11"
  - id: DEC-2026-1011-ai-practice-feedback-in-shared-budget
    kind: schema
    status: ACTIVE
    date: 2026-10-11
    subject: "F6-08 practice feedback counts toward the shared AI token budget: ai.token_usage.feature_code gains PRACTICE_FEEDBACK, and the F5-25 lock covers it"
    decision: >
      The ENUM `feature_code` on `ai.token_usage` (02-bd/database/F5_ai-review.md section 1.6) gains
      `PRACTICE_FEEDBACK`: one row per AI feedback call of the interview-bank practice mode (F6-08).
      `owner_id` is the `user_answers.id` of the attempt. `ai.prompt_templates` does NOT gain the value (no
      admin-editable prompt for F6-08, so no tab on ADM0301). The monthly budget and the automatic lock of F5-25
      cover it: when `ai_token_budget_configs.locked_at` is set, F6-08 is refused for STUDENT and INSTRUCTOR like
      F5 (ADMIN exempt). `interview-bank` checks the budget and records the tokens through a port into
      `ai-review`, not by sharing tables; if `ai-review` is down the AI part of practice mode degrades (the answer
      is saved without AI feedback), F1-F4 are unaffected. The F6-08 rate limit stays a constant 20 calls per hour
      per user in its own Redis namespace `interview_bank:ratelimit:<userId>`; it is NOT added to ADM0303 (no new
      config field). The ADM0303 cooldown applies to Phan tich and Phong van only.
    rationale: >
      Ponytail rung 6: F6-08 calls the same LLM provider, so leaving it out makes the monthly cap and the lock not a
      real ceiling on spend and hides F6 cost from ADM0302. Rung 4: `token_usage` and the lock already exist; one
      enum value, same pattern as DEC-2026-1010-ai-generate-testcase-feature-code. Rung 5: the rate limit stays a
      constant. Owner agreed 2026-10-11 after the second review in
      06-plan/reports/261010-2041-ai-admin-doi-chieu-hien-trang/ (N-01). The drift was BD interview-bank vs RD F5-25.
    consequences: >
      Does not change F5-25: 01-rd/req/F5_ai-review.md:66 (decided 2026-08-24) already lists F6-08 among the calls the
      lock stops. This entry aligns the BD, which had drifted from it, and adds the missing ledger value. Replaces the sentence "token-budget doc lap voi ai-review" in
      02-bd/architecture/F6_interview-bank.md. ADM0302 gets a fourth series "Cham cau luyen". DD `ai-review` defines
      the budget-check and usage-record port that `interview-bank` calls; DD `interview-bank` handles the refused
      case (locked budget) in the practice screen.
    related:
      - DEC-2026-1010-ai-generate-testcase-feature-code
      - DEC-2026-1010-ai-rate-limit-fixed-window
      - DEC-2026-1010-split-ai-admin-screens
    source: "owner instruction in session 2026-10-11"
  - id: DEC-2026-1011-ai-no-full-solution-can-be-turned-off
    kind: product-scope
    status: ACTIVE
    date: 2026-10-11
    subject: "The answer guard 'Khong dua loi giai day du' (noFullSolution) can be turned off by ADMIN, with a warning; it is no longer locked on"
    decision: >
      Owner instruction 2026-10-11 ("van cho toi tat duoc nhung se kem canh bao"). On ADM0301 the guard
      `noFullSolution` defaults to on, can be switched off, and switching it off shows a warning toast plus a warning
      line under the switch until it is switched on again. The server accepts both values for it and no longer
      forces `true`. Replaces the "locked on, read-only" part of DEC-2026-1010-split-ai-admin-screens and the
      "server forces noFullSolution = true" rule in 02-bd/security/F5_ai-review.md.
    rationale: >
      P0 owner instruction. Risk recorded for the owner: 01-rd/req/F5_ai-review.md:54 treats F5-17 and F5-18 as hard
      constraints, and this switch weakens the educational intent (F5-18) for every learner of that feature once
      published. Prompt-injection defence itself (user code as a data parameter, F5-17) does not depend on this switch and
      stays hard. Open for the owner: amend F5-18 in 01-rd/req/F5_ai-review.md to say the full-solution ban is the default,
      not an absolute rule.
    consequences: >
      BD ADM0301 V1.8, 02-bd/security/F5_ai-review.md, 02-bd/database/F5_ai-review.md (response_guards) and RD ADM0301
      updated. DD ai-review must not hard-code the value. The previous "M-10" closure (guard locked) is superseded.
    related:
      - DEC-2026-1010-split-ai-admin-screens
    source: "owner instruction in session 2026-10-11"
  - id: DEC-2026-1011-class-assignment-popup-on-assignments-screen
    kind: architecture
    status: ACTIVE
    date: 2026-10-11
    subject: "The 'Assign to class' popup lives on the class_assignments screen (INS0202), replacing the problem_list location"
    decision: >
      Approved by the owner on 2026-10-11 (proposal S4 of report
      `06-plan/reports/261011-2030-instructor-userflow-va-mau-thuan/report.md`). The flow that assigns problems
      from the shared problem bank is a popup on the `class_assignments` screen: pick one class, tick the
      published problems not yet assigned to that class, button "Giao n bài". The "assign-from-bank flow gets an
      inline modal" part of `DEC-2026-0831-class-assignments-round2` no longer puts the popup on `problem_list`;
      the soft-remove part (F2-12) is unchanged.
    rationale: >
      Ponytail rung 4 (use one place): `problem_list` is a learner screen and forces the instructor out of the
      Instructor shell; one popup in one place needs no duplicate on another screen. The alternative (a bulk
      "Assign to class" action at `/instructor/problems`) can only assign the instructor's own problems, far
      narrower than the "from the shared problem bank" wording of F2-12.
    consequences: >
      BD `INS0202` V0.5 records it. DD `problem-bank` must define a multi-problem assign operation for one class
      and `ListAssignableProblems` (published problems not yet assigned to that class).
    related:
      - DEC-2026-0831-class-assignments-round2
    source: "owner instruction in session 2026-10-11 (duyệt đồng ý hết); report 261011-2030 mục 4.1 S4"
  - id: DEC-2026-1011-a2-read-published-problems-of-others
    kind: architecture
    status: ACTIVE
    date: 2026-10-11
    subject: "A2 gets read-only access to other authors' published problems; other authors' drafts still return 404"
    decision: >
      Approved by the owner on 2026-10-11 (proposal S5, option a). `problem_info` (`/instructor/problems/{id}`)
      lets A2 open another author's `Đã xuất bản` problem READ-ONLY. Another author's `Chưa xuất bản` problem
      still returns 404, identical to a non-existent id, as in rule `SHR0203` Q8 (2026-10-03). Edit, delete,
      authoring and preview (`/edit`, `/preview`) stay limited to the caller's own problems. The A2
      `problem_management` list still contains only the caller's own problems. The "Chi tiết" button on
      `class_assignments` points to `/instructor/problems/{id}`.
    rationale: >
      Ponytail rung 6 (access control). Instructors need to read the statement before assigning, and F2-12 lets
      them assign problems from the shared bank. The server must enforce this in the application layer: another
      author's draft returns 404 so its existence is not leaked; it must not depend on hiding the button in the UI.
    consequences: >
      Adjusts `SHR0203` Q8; `03-dd/api/F2_problem-bank.md` must define the contract: `GetProblemForAuthoring` / the
      read command for A2 under the condition `author_id = caller OR status = PUBLISHED`, writes/edits/deletes by
      `author_id`. Needs e2e and server tests for both branches (published: 200 read-only; other's draft: 404).
    related:
      - DEC-2026-1002-split-detail-and-edit-pages
      - DEC-2026-0830-problem-lifecycle-two-states
    source: "owner instruction in session 2026-10-11 (duyệt đồng ý hết); report 261011-2030 mục 4.1 S5"
  - id: DEC-2026-1011-teacher-nav-badge-dropped
    kind: product-scope
    status: ACTIVE
    date: 2026-10-11
    subject: "Drop the numeric nav badges of the Instructor area from the BD"
    decision: >
      Approved by the owner on 2026-10-11 (proposal S8). Instructor sidebar nav items carry no numeric badge
      (the 3 / 18 / 9 of the prototype). No summary endpoint is designed for the shell. Closes `_shell` Q3 and
      Q11 of `INS0301` (same question, resolved by dropping).
    rationale: >
      Ponytail rung 2: there is no real data source for the numbers; the three numbers come from three different
      Bounded Contexts, so three API calls on every screen change is wasteful. The build already dropped the
      badges on purpose.
    consequences: >
      BD `_shell`, `INS0101`, `INS0301` drop the badge sentences. To bring them back later: one summary endpoint
      for the shell, decided separately.
    related:
      - DEC-2026-0921-teacher-screens-conflict-resolutions
    source: "owner instruction in session 2026-10-11 (duyệt đồng ý hết); report 261011-2030 mục 4.1 S8"
  - id: DEC-2026-1011-teacher-screens-open-questions-closed
    kind: process-policy
    status: ACTIVE
    date: 2026-10-11
    subject: "Close six open questions of the Instructor area that block API design: INS0101 Q4 Q6, INS0301 Q8 Q9, INS0204 Q5, INS0202 Q10"
    decision: >
      The owner approved on 2026-10-11 all of Table 9 of report `06-plan/reports/261011-2030-instructor-userflow-va-mau-thuan/`
      (the proposals were AI inference, now approved and therefore decisions). (1) INS0101 Q6: the "Hoạt động
      gần đây" feed shows the 10 most recent items, and the UI sub-label is changed to match (ponytail rung 5,
      constant). (2) INS0101 Q4: the feed contains only events of learners belonging to the classes the
      instructor is in charge of, filtered by `class_id`. (3) INS0301 Q8: one class, one instructor
      (`identity.classes.instructor_id`), rung 1 (no RD requirement needs co-teaching). (4) INS0301 Q9: an
      `Accepted` submission without an AI report still enters the manual-grading queue, the AI score cell shows
      "Chưa có", and the instructor grades by hand as usual; basis: the AI subsystem degrades gracefully, F1-F4
      do not depend on F5. (5) INS0204 Q5: A2 views `submission_result` read-only for learners belonging to the
      classes they are in charge of; outside those classes it returns 404. (6) INS0202 Q10: a problem already
      assigned and later unpublished stays in the table with the label "Đã rút xuống" and cannot be assigned
      again; learners no longer see it in `problem_list`; submissions and bookmarks are kept (US-A2-01b).
    rationale: >
      These questions block fixing the API contracts of `identity`, `ai-review`, `problem-bank`. Each option is
      the one that invents the least new behaviour: it reuses existing data (`classes.instructor_id`,
      `class_id`), adds no second queue source, and never makes a problem vanish from the instructor's view.
    consequences: >
      BD `INS0101` V0.3, `INS0301` V0.4, `INS0204` V0.4, `INS0202` V0.5 record them as closed. The column or index
      that filters the feed by class (the `scope_class_id` proposal in the old `INS0101` Q4) is decided by BD
      `database/F1_identity.md` when DD is written. DD `identity` must check class scope for A2's
      `submission_result` (404 outside the class). DD `ai-review`: the manual-grading queue query must include
      `Accepted` submissions with no `solution_reviews` row (the 6.0 threshold applies only when an AI score exists).
    related:
      - DEC-2026-0831-instructor-grading-round2
      - DEC-2026-0830-problem-lifecycle-two-states
      - DEC-2026-1005-list-screens-open-questions-closed
    source: "owner instruction in session 2026-10-11 (duyệt đồng ý hết); report 261011-2030 mục 4.2 Bảng 9"
  - id: DEC-2026-1011-teacher-shell-identity-footer-built
    kind: product-scope
    status: ACTIVE
    date: 2026-10-11
    subject: "The Instructor area builds an identity block + 'Thoát' (sign out) at the sidebar bottom and a footer that reuses the Admin footer component, renamed to a shared one"
    decision: >
      Approved by the owner on 2026-10-11 (S1, S2). (1) The Instructor sidebar bottom has an identity block:
      letter avatar, name, role "Giảng viên", link "Thoát". "Thoát" means SIGN OUT, going to the login screen
      (closes `_shell` Q5). When collapsed, only the avatar shows. (2) The Instructor footer (already decided in
      `DEC-2026-0921-teacher-screens-conflict-resolutions`) reuses the Admin footer component after it is renamed
      into a shared component that takes a label parameter. (3) The Instructor rail-forcing breakpoint stays at
      1024px, same as the Admin area (closes `_shell` Q4). The code is being built in parallel; this entry records
      the decision and cites no code lines.
    rationale: >
      Ponytail rung 1 (the BD already decided the identity block and the footer), rung 4 (reuse the Admin footer
      component and the way the learner area declares its sign-out item), rung 6 (no sign-out is an operational
      hole). One 1024px breakpoint for both areas avoids two thresholds.
    consequences: >
      BD `_shell` records a "Hiện trạng bản dựng" for the identity block and footer. Run `impact` on the old
      footer component when renaming it, and check the Admin area does not change visually.
    related:
      - DEC-2026-0921-teacher-screens-conflict-resolutions
      - DEC-2026-0921-admin-shell-footer
    source: "owner instruction in session 2026-10-11 (duyệt đồng ý hết); report 261011-2030 mục 4.1 S1, S2"

  - id: DEC-2026-1011-instructor-layout-follows-admin
    kind: product-scope
    status: ACTIVE
    date: 2026-10-11
    subject: "The Instructor area uses the same layout as the Admin area: grouped collapsible sidebar, toolbar row with a user menu, shared footer"
    decision: >
      Owner instruction 2026-10-11 ("UI tổng thể tôi muốn layout giống admin"). The Instructor shell follows the
      Admin shell: (1) the sidebar is an overview link plus collapsible groups ("Lớp học", "Giao và chấm bài",
      "Nội dung"), the logo and collapse toggle inside the sidebar, the theme switcher at its foot, the "GIÁO VIÊN"
      eyebrow kept under the wordmark; (2) a toolbar row at the top of the content area with the user menu; settings,
      profile and "Thoát" (sign out) live in that menu and no longer in the sidebar; (3) the shared area footer. The
      colour palette stays the Instructor one (`.instructor-shell`); only the layout follows Admin. This supersedes
      part (1) of DEC-2026-1011-teacher-shell-identity-footer-built (identity block at the sidebar bottom) and
      the flat 5-item sidebar of the mockup (BD `_shell` 3.1, 3.2).
    rationale: >
      Ponytail rung 1 (owner decision) and rung 4 (one grouped-sidebar and one toolbar component serve both
      areas instead of two forks). Admin behaviour is unchanged; the Instructor area matches nested routes by
      longest href prefix.
    consequences: >
      BD `_shell` sections 3.1, 3.2 and 6 describe the grouped layout. e2e `instructor-shell.spec.ts` expands the
      groups before clicking. The three decorative toolbar icons are inherited from Admin and stay decorative.
    related:
      - DEC-2026-1011-teacher-shell-identity-footer-built
      - DEC-2026-0922-users-and-admin-conflict-resolutions
    source: "owner instruction in session 2026-10-11; report 261011-2030"

  - id: DEC-2026-1011-teacher-overview-toolbar-dropped
    kind: product-scope
    status: ACTIVE
    date: 2026-10-11
    subject: "Drop the search box and the 'Tạo lớp mới' button from the instructor overview screen (INS0101), requirement included"
    decision: >
      Owner instruction 2026-10-11 ("ở trang tổng quan bỏ phần thừa search và button tạo lớp mới", then "bỏ luôn
      cho tôi" for the RD). The instructor_overview screen no longer has the header search box or the
      "+ Tạo lớp mới" button, in the prototype, in BD INS0101 (V0.4, Q2 and Q3 closed) and in RD INS0101 section
      2.1 point 3. Class creation stays on class_management; the avatar lives in the shared toolbar user menu.
    rationale: >
      Ponytail rung 3 (the search box never had behaviour and no search result screen exists) and rung 4 (class
      creation already exists on class_management).
    consequences: >
      No search endpoint is needed for this screen (BD Q3). BD INS0101 keeps the struck-through rows so item
      numbers stay stable.
    related:
      - DEC-2026-1011-instructor-layout-follows-admin
      - DEC-2026-0831-instructor-overview-dashboard
    source: "owner instruction in session 2026-10-11"
  - id: DEC-2026-1011-class-detail-page
    kind: product-scope
    status: ACTIVE
    date: 2026-10-11
    subject: "Add a class detail page (INS0205, slug class_detail) under 'Lớp của tôi'; supersedes the 2026-08-28 owner answer in RD INS0201 Q3"
    decision: >
      Owner instruction 2026-10-11, reason given: "3 nút với popup không cung cấp đủ thông tin". New screen
      INS0205 `class_detail`, prototype route `/instructor/classes/[classId]`. Content: header (class name,
      description, Back, Edit, Delete); stats strip (student count, completion rate, average score, to-grade,
      absent); Schedule card (read part by part); Invite codes card (valid codes with expiry and a "create new
      code" button, several valid in parallel per F1-25); class students table (name linking to INS0204,
      status, average score, completion rate, "remove from class" with confirmation). Entry points: click the
      class name or the "view detail" icon on the list; after a class is created the app goes straight to the
      detail page (replacing the popup that showed the first invite code). On the INS0201 list the Invite-code
      popup and the "view students" icon are removed; only view-detail, edit and delete icons remain. The
      Create/Edit class popup stays and is shared. The all-students page `/instructor/students` stays (filter
      by class). This reverses the 2026-08-28 owner answer in RD INS0201 Q3 ("no separate /class/:id child
      page"); that answer is struck through in the RD with "ĐẢO NGƯỢC 2026-10-11".
    rationale: >
      Owner instruction (ponytail rung 1). Rung 4: no new endpoint, the page reuses `ListInstructorClasses`,
      `ListClassInviteCodes`, `CreateClassInviteCode`, `ListClassStudents`, `RemoveStudentFromClass`,
      `UpdateClass`, `DeleteClass` already used by INS0201 and the shared popups.
    consequences: >
      New RD `01-rd/screens/teacher/INS0205_class_detail.md` and BD `02-bd/screens/teacher/INS0205_class_detail.md`.
      BD INS0201 V0.6 records the removed popup/icon and the navigation. DD for the screen is NOT written yet
      (screen DD follows the prototype). Open BD question: `ClassCardDto` has no `description` and there is no
      "get one class" endpoint; either add the field to `ClassCardDto` or add `GetClassDetail` (DD decides).
      Screen counts in `01-rd/README.md` go up by one. Frontend slice `class-detail` is a separate task.
    related:
      - DEC-2026-0828-split-class-management-assignments
      - DEC-2026-1011-class-schedule-structured
    source: "owner instruction in session 2026-10-11"
  - id: DEC-2026-1011-class-schedule-structured
    kind: schema
    status: ACTIVE
    date: 2026-10-11
    subject: "Class schedule becomes structured data: classes.schedule_note (text) replaced by classes.schedule (jsonb)"
    decision: >
      Owner instruction 2026-10-11. Column `identity.classes.schedule_note` (free text) is replaced by
      `identity.classes.schedule` (jsonb, nullable) with keys `days` (array of ISO weekdays, 1 = Monday to
      7 = Sunday), `startTime` and `endTime` (HH:mm), `room`, `startDate` and `endDate` (yyyy-MM-dd), and `note`
      (remaining free text). Every part is optional; a class may have no schedule. Rules: a time requires at
      least one day; start and end time go together and end is after start; end date is not before start date;
      `room` at most 100 characters and `note` at most 200 characters (both confirmed by the owner on
      2026-10-13 in `DEC-2026-1013-teacher-open-questions-closed-round2`). One-line display: "T2 · T4 · T6 — 19:00–21:00 · Phòng B203 · 17/08–20/12 · note".
    rationale: >
      Ponytail rung 1 (owner instruction) and rung 3 (the demo shows the schedule on the class detail page and
      on the list). jsonb rather than five columns because the parts are only ever read and written together
      with the class row and are never filtered or joined on.
    consequences: >
      `02-bd/database/F1_identity.md` section 1.8 and new section 1.14; BD INS0201 V0.6 (Sheet 5, 7, 9);
      `03-dd/validation/F1_identity.md` section 5 (the old 500-character `schedule_note` limit is replaced);
      `03-dd/api/F1_identity.md` PATCH classes row; RD `01-rd/req/F1_identity.md` F1-23 gets a one-line note only.
      Greenfield with no backend `classes` table yet, so no data migration. Frontend type
      `05-coding/frontend/src/entities/class/model/types.ts` still mentions `schedule_note` and is a separate task.
    related:
      - DEC-2026-1011-class-detail-page
    source: "owner instruction in session 2026-10-11"
  - id: DEC-2026-1011-class-detail-no-stat-strip
    kind: product-scope
    status: ACTIVE
    date: 2026-10-11
    subject: "Drop the stat strip (five figure cards) from the class detail page INS0205"
    decision: >
      Owner instruction 2026-10-11. The class detail page `INS0205` (`class_detail`) no longer has the stat strip
      of five cards (students, completion, average score, to-grade, absent). The page keeps four blocks: header,
      Schedule card, Invite codes card, class students table (per-student columns unchanged). RD `INS0205` item 2
      and REQ-03 and BD `INS0205` (V0.2: stats area, Sheet 5 area B, Sheet 6 NO 4, Sheet 9 NO 10, DTO and
      layout notes) are struck through, item numbers kept stable. The class figures stay visible on the class list
      `INS0201`. The instructor overview screen `INS0101` is NOT affected and stays in scope.
    rationale: >
      Ponytail rung 4: the same five figures already show on each row of the class list `INS0201`, so the detail
      page repeating them adds nothing.
    consequences: >
      The detail page no longer calls `ai-review` (`CountPendingManualGrading`), so its AI-degradation check is
      gone. No endpoint or table is removed; `ClassCardDto` stat fields remain used by `INS0201`. Frontend removal
      of the strip is a separate task done by the owner.
    related:
      - DEC-2026-1011-class-detail-page
    source: "owner instruction in session 2026-10-11"
  - id: DEC-2026-1013-instructor-avatar-menu-and-read-source
    kind: product-scope
    status: ACTIVE
    date: 2026-10-13
    subject: "Instructor toolbar avatar is a user menu (popup) like Admin; A2 may read the source code of submissions of learners in their classes"
    decision: >
      Owner answers 2026-10-13. (1) INS0101 Q10: the avatar in the Instructor toolbar HAS a dropdown menu
      (Cài đặt, Trang cá nhân, Thoát), and the header follows the Admin header; this is what the prototype already
      builds (`DEC-2026-1011-instructor-layout-follows-admin`). (2) Q-J (INS0204 Q5 part on source code): A2 CAN
      read the source code in `submission_result` of a learner who belongs to a class they are in charge of,
      read-only; outside those classes it stays 404 (`DEC-2026-1011-teacher-screens-open-questions-closed` item 5).
      Source code is read through `submission_result`, not through the manual-grading queue DTO, so the queue
      endpoints keep returning no source code. This also closes INS0301 Q10 part (c) (the separate decision on
      viewing source while grading).
    rationale: >
      Ponytail rung 4 for (1): the shared area toolbar and user menu already exist, a second identity entry in the
      sidebar is gone. Rung 1 for (2): an instructor cannot grade a submission fairly without seeing the solution
      (US-A2-03); the privacy line is the class scope, which is already enforced by the 404 rule.
    consequences: >
      BD `INS0101` Q10 and its Sheet 5/8 rows, BD `INS0204` Q5 and the "Không hiển thị mã nguồn" lines, BD `INS0301`
      Q10 are updated. BD `INS0301` still forbids source code in its own three endpoints. DD `identity` must check
      class scope on `submission_result` for A2. The grading popup link "Xem bài nộp" to a read-only
      `submission_result` for A2 is a prototype task (no instructor route exists yet).
    related:
      - DEC-2026-1011-teacher-screens-open-questions-closed
      - DEC-2026-1011-instructor-layout-follows-admin
    source: "owner answer in session 2026-10-13"
  - id: DEC-2026-1013-teacher-open-questions-closed-round2
    kind: product-scope
    status: ACTIVE
    date: 2026-10-13
    subject: "Close four open questions of the Instructor area (list-filter URL, schedule limits, re-grading, nav default open)"
    decision: >
      The owner approved on 2026-10-13 the four proposals of the previous round. (1) INS0203 Q6 and INS0204 Q6:
      class tab, search text and page are written into the URL (`?classId=&q=&page=`), one convention for every
      list; the Back link on the student detail uses `router.back()`, falling back to the list URL when there is
      no history. (2) INS0205 Q6: `room` max 100 characters, `note` max 200, and the schedule rules (a time
      needs a day, end time after start time). (3) INS0301 Q7: an already graded score can be edited; only the
      latest value is kept with `manual_graded_at`, edit history is deferred (ponytail rung 3). (4) Q-K: the
      three Instructor nav groups start open; the Admin sidebar is unchanged.
    rationale: >
      Ponytail rung 5 for (2) and (4): constants and one default value. Rung 3 for (3): no demo scenario needs
      an edit history. Rung 4 for (1): one URL convention instead of per-screen state, and the browser history
      already does the restoring.
    consequences: >
      BD `INS0203` V0.4, `INS0204` V0.6, `INS0205` V0.3, `INS0301` V0.7 and `_shell` V1.3 record them. The
      prototype builds (1) and (4); (2) and (3) were already as decided. Other lists keep local state until each
      is touched (grading already reads `?studentId=&classId=`). DD `identity` and `ai-review` take the limits
      and the "latest value only" rule.
    related:
      - DEC-2026-1013-instructor-avatar-menu-and-read-source
      - DEC-2026-1011-instructor-layout-follows-admin
    source: "owner instruction in session 2026-10-13 (ok triển khai tiếp)"
  - id: DEC-2026-1013-instructor-submissions-list
    kind: product-scope
    status: ACTIVE
    date: 2026-10-13
    subject: "Add a submission list to the Instructor area: sidebar item 'Bài nộp' at /instructor/submissions"
    decision: >
      Owner instruction 2026-10-13 ("xử lý submissions cho giảng viên", then "tôi thấy ở sidebar đã có đâu"). The
      Instructor sidebar group "Giao và chấm bài" gets a third item "Bài nộp" (`/instructor/submissions`): a list of every
      submission of the learners in the classes the instructor is in charge of, with a class filter, a result filter
      (all, accepted, not passed), a search by learner or problem, pagination, and the list state in the URL
      (`?classId=&result=&q=&page=`, the convention of `DEC-2026-1013-teacher-open-questions-closed-round2`). A row opens the
      read-only result `/instructor/submissions/{id}` (`DEC-2026-1013-instructor-avatar-menu-and-read-source`) with the learner
      and class in the query. The student detail history and the grading popup use the same read-only result. The list has
      no stat-card strip.
    rationale: >
      Rung 1: the owner asked for it. Rung 4: the read-only result page, `SUBMISSION_STATUS_VARIANT`, the result filter and the
      URL-state convention already exist and are reused. Without a list the only way into a submission was a learner profile
      or the grading popup, so the sidebar had no entry for the feature.
    consequences: >
      RD and BD for the new screen (proposed slug `INS0206` `submission_management`) are not written yet. DD `judge-orchestration`
      needs one endpoint listing submissions by the instructor's class ids, and the class-scope check of the result page
      (404 outside the classes). The prototype builds the rows from the four readable mock submissions, two per learner.
    related:
      - DEC-2026-1013-instructor-avatar-menu-and-read-source
      - DEC-2026-1013-teacher-open-questions-closed-round2
    source: "owner instruction in session 2026-10-13"
  - id: DEC-2026-1013-a2-reads-student-contact
    kind: product-scope
    status: ACTIVE
    date: 2026-10-13
    subject: "A2 may read the email and phone number of learners in their own classes; student detail shows contact and study info instead of a stat strip"
    decision: >
      Owner instruction 2026-10-13. (1) On INS0204 (class student detail) an instructor sees the learner's full
      name, email and phone number, read-only, so they can contact the learner. Scope is the class: only learners
      of a class the A2 is in charge of; any other learner stays 404 like the rest of the screen. (2) The screen has
      no row of stat cards (Điểm TB, Hoàn thành, Chuỗi ngày, Hoạt động) and no "last active" field; it shows two
      cards side by side, "Liên hệ" (name, email, phone) and "Học tập" (class, status, assigned topics). (3)
      Prototype rule: no small overview stat strips on detail or list screens (CLAUDE.md Rules).
    rationale: >
      Ponytail rung 1: the owner asks for it so instructors can reach a learner directly. Rung 4: the numbers of
      the stat strip are already on the student list (INS0203), repeating them on the detail page adds nothing.
      Rung 2: email already exists in `identity.users`; phone has no source yet, so the decision adds the column
      and the profile field rather than showing invented numbers. Rung 6 does not block it: the privacy line is
      the class scope, the same line as
      `DEC-2026-1013-instructor-avatar-menu-and-read-source` for source code.
    consequences: >
      BD `INS0204` V0.9 rewrites Sheet 4, 5 and 7 (stat strip removed, contact card added, DTO gains `email` and
      `phone`, `streakDays` and `lastActiveLabel` leave this screen). `ClassStudent` in the prototype gains optional
      `email` and `phone`. DD `identity` must expose those two profile fields to A2 only within class scope and must
      not return them for learners outside it. Phone is a new nullable column `identity.users.phone`
      (VARCHAR(20), self-declared, not verified, NULL on anonymisation) and a 7th optional field of the F1-09
      profile form (RD `identity` F1-09, BD `USR0502` V0.4); shown as `-` when empty. The student profile input is
      built in the student-UI phase; until then the instructor screen shows mock values (PROTOTYPE_DEBT).
    related:
      - DEC-2026-1013-instructor-avatar-menu-and-read-source
      - DEC-2026-1011-class-detail-no-stat-strip
    source: "owner instruction in session 2026-10-13 (ok bổ sung cho tôi)"
  - id: DEC-2026-1013-grading-queue-all-accepted
    kind: product-scope
    status: SUPERSEDED
    superseded_by: DEC-2026-1013-remove-instructor-manual-grading
    date: 2026-10-13
    subject: "Manual grading queue lists every Accepted submission of the instructor's classes; AI score is reference only, not an entry condition"
    decision: >
      The owner decided on 2026-10-13 that the INS0301 queue is no longer pre-filtered by "AI score below 6/10".
      It lists every `Accepted` submission of the problems of the classes the instructor is in charge of (the
      learner's own AI report is optional, so a submission without a report is listed with the AI cell "Chưa có",
      as already decided in DEC-2026-1011-teacher-screens-open-questions-closed Q9). The AI score stays on screen as
      reference information. This replaces only the threshold part (Q2) of
      DEC-2026-0831-instructor-grading-round2; its other parts (side-by-side AI and manual score, 7-day statistics
      window, class filter) stay. Schema consequence: manual grading no longer lives in `ai.solution_reviews`
      (a row that exists only when the learner asked for an AI report); a new table `ai.manual_grades`
      (`submission_id` unique, `manual_score`, `manual_comment`, `graded_by`, `graded_at`) holds it, and the five
      `manual_*` columns of `solution_reviews` are dropped. The queue base table becomes `judge.submissions`
      LEFT JOIN `ai.solution_reviews` LEFT JOIN `ai.manual_grades`.
    rationale: >
      The AI report is a learner-initiated action (RD ai-review F5.1 amendment 2026-08-31), so "AI score below 6"
      filtered by whether the learner pressed a button, not by solution quality: a learner who never asked for a
      report flooded the queue, one who got 7/10 on a wrong-minded solution was invisible. The instructor needs the
      whole class view. Keeping manual grading outside the AI table also follows "the AI subsystem degrades
      gracefully, F1-F4 never depend on F5" (ponytail rung 1 for owner decision, rung 6 for the data-loss risk of
      a grade that cannot be stored without an AI row).
    consequences: >
      RD `ai-review` F5-27 Q2 and RD `INS0301` Q2 amended; BD `database/ai-review` (section 1.3 manual_* columns
      moved, new section for `manual_grades`, indexes, section 4 query) and BD `INS0301` V0.9, `INS0101` V0.4
      follow. Not decided here and kept as open questions: which of several Accepted submissions of one learner
      on one problem is gradable (INS0301 Q12), and an optional sort by AI score (INS0301 Q13). DD `ai-review` and
      the prototype are not updated yet: mock data still holds only AI scores below 6 (PROTOTYPE_DEBT).
    related:
      - DEC-2026-0831-instructor-grading-round2
      - DEC-2026-1011-teacher-screens-open-questions-closed
      - DEC-2026-0830-remove-student-review-request
    source: "owner answers in session 2026-10-13 (giữ Q9, giảng viên có toàn bộ bài theo lớp; chỉ bài Accepted; điểm AI giữ làm tham khảo)"
  - id: DEC-2026-1013-remove-instructor-manual-grading
    kind: product-scope
    status: ACTIVE
    date: 2026-10-13
    subject: "Remove instructor manual grading (screen INS0301, F5-27, US-A2-06) from the scope"
    decision: >
      The owner decided on 2026-10-13 to drop manual grading by the instructor altogether (option A of the
      session options). Removed: screen `INS0301` (`instructor_grading`) and its nav item "Chấm bài"; requirement
      `F5-27` (manual score and comment, queue, statistics) and user story `US-A2-06`; the table `ai.manual_grades`
      proposed earlier the same day (never built) and the five `manual_*` columns of `ai.solution_reviews`; the
      "Cần chấm tay" block and card of `INS0101`; the "Cần chấm" column and statistic of `INS0201`, `INS0202`,
      `INS0205`; the link "Xem chấm bài của học viên này" of `INS0204`. Kept unchanged: the learner-initiated AI
      solution review (F5.1), `CLASS_MANAGEMENT` (class management still needs it), and the read-only access of A2
      to the source code of a learner in their class (`DEC-2026-1013-instructor-avatar-menu-and-read-source`).
      Supersedes `DEC-2026-0831-instructor-grading-round2` and `DEC-2026-1013-grading-queue-all-accepted`.
      Questions Q7, Q9, Q10, Q11, Q12, Q13 of `INS0301` and the decisions that closed them become moot.
    rationale: >
      Ponytail rung 3: an instructor cannot read the code of every learner, so the queue would be full and unused
      in the demo and defence; the owner confirmed few instructors would use it. Rung 2 also fails: the manual
      score feeds no other screen or report. Deleting it removes a screen, a table, two dashboard blocks and a nav
      item. The AI report is learner-initiated, so any AI-score-driven queue only reflected whether the learner
      pressed a button (see the superseded `DEC-2026-1013-grading-queue-all-accepted`).
    consequences: >
      RD (`ai-review` F5-27, `a2_instructor` US-A2-06, `INS0301`, `INS0101`, `INS0201`, `INS0202`, `INS0204`,
      overview and feature catalog), BD (`INS0301` becomes a removal stub, `INS0101`, `INS0201`, `INS0202`,
      `INS0204`, `INS0205`, `_shell`, `database/ai-review`, `security/ai-review`, architecture) are updated. Open
      and not decided here: (1) `ai.solution_reviews.ai_score_10` was "reference for the instructor only"; with
      F5-27 gone no screen displays it, keep or drop is a separate owner decision; (2) the instructor's entry to
      the read-only submission page (`/instructor/submissions/{id}`) came from the grading popup, so a new entry
      (for example from `INS0204`) needs an owner decision. Prototype code and tests (views/teacher/grading,
      entities/manual-grading, nav item, messages, stat columns) are NOT changed yet: another session is editing
      submission-related files, so the code removal waits (PROTOTYPE_DEBT).
    related:
      - DEC-2026-0831-instructor-grading-round2
      - DEC-2026-1013-grading-queue-all-accepted
      - DEC-2026-1013-instructor-avatar-menu-and-read-source
    source: "owner instruction in session 2026-10-13 (chọn phương án A, triển khai bỏ hẳn)"
  - id: DEC-2026-1014-instructor-overview-trim-and-nav-label
    kind: product-scope
    status: ACTIVE
    date: 2026-10-14
    subject: "Trim INS0101 (drop 'Điểm TB lớp' card and 'Tiến độ học viên' chart), keep the submission read entries, rename the nav group"
    decision: >
      The owner decided on 2026-10-14, as follow-up of DEC-2026-1013-remove-instructor-manual-grading:
      (1) `INS0101` drops the stat card "Điểm TB lớp" and the widget "Tiến độ học viên" (line chart of the average
      score by week), with their DTO fields, events and the "Xem tất cả" link to `class_progress`; `INS0101` keeps
      the cards "Lớp phụ trách" and "Tổng học viên", "Hoạt động gần đây", "Lớp của tôi", "Bài tập của tôi".
      (2) The instructor's entries to the read-only submission page stay as they are (the "Xem kết quả" button in
      the submission history of `INS0204` and the nav item "Bài nộp"); no new entry is added; closes `INS0204` Q11.
      (3) The nav group "Giao và chấm bài" is renamed "Quản lý bài tập đã nộp" (both of its items stay);
      closes `_shell` Q6. The `class_progress` screen (`INS0203`) and its own chart are NOT touched.
    rationale: >
      The card and the chart both read `ai_score_10`, which only existed as the instructor's reference for manual
      grading (ponytail rung 3: no real demo use once the grading screen is gone). The read entries already exist
      and the earlier decision DEC-2026-1013-instructor-avatar-menu-and-read-source needs them (rung 4: reuse). The
      group no longer contains anything to "grade", so its label must change.
    consequences: >
      RD `identity` F1-30, `INS0101`, `INS0204`, BD `INS0101`, `INS0204`, `_shell` updated; frontend i18n label
      `instructorNav.groupWork` and unused `instructorOverview` keys removed. Still open, not decided: whether the
      column `ai.solution_reviews.ai_score_10` is kept (OQ-F5-A in RD `ai-review`): after this decision `INS0101`
      no longer reads it; other screens are not yet checked.
    related:
      - DEC-2026-1013-remove-instructor-manual-grading
      - DEC-2026-1013-instructor-avatar-menu-and-read-source
    source: "owner instruction in session 2026-10-14 (bỏ điểm TB lớp và biểu đồ; giữ lối vào; nhãn nhóm Quản lý bài tập đã nộp)"
  - id: DEC-2026-1014-drop-ai-score-10
    kind: schema
    status: ACTIVE
    date: 2026-10-14
    subject: "Drop the derived column ai.solution_reviews.ai_score_10 and its weighted-conversion rule"
    decision: >
      The owner decided on 2026-10-14 to drop the column `ai.solution_reviews.ai_score_10` (the AI score converted
      to a 0-10 scale by the admin rubric weights) and the rule that computes it. It was only ever shown to the
      instructor (manual grading queue, "Điểm TB lớp" card, trend chart), all removed by
      DEC-2026-1013-remove-instructor-manual-grading and DEC-2026-1014-instructor-overview-trim-and-nav-label;
      INS0201, INS0203 and INS0204 explicitly do not show it and the learner never sees it. Closes OQ-F5-A.
      Kept unchanged: the F5.1 report (`result_json`), `readability_score`, the per-criterion rows of
      `ai.rubric_scores` (owner SOLUTION_REVIEW and MOCK_INTERVIEW) with their comments, and `rubric_configs`.
    rationale: >
      Ponytail rung 3 (no real demo use: no screen displays it) and rung 2 (nothing reads it). A column that
      nobody reads is dead data with a conversion rule to maintain.
    consequences: >
      RD `ai-review` (F5-27 text, OQ-F5-A), `identity` F1-30, RD/BD of INS0101, INS0204, INS0301 stub, BD
      `database/ai-review`, `architecture/ai-review`, `security/ai-review`, `database/interview-bank`, USR0302,
      USR0501, USR0601 are updated where they name the column. NOT decided here, listed as an open question for the
      owner: the admin rubric WEIGHTS of F5-23 (screen ADM0301, table `rubric_configs`, `weight_percent_snapshot`
      of `rubric_scores`) existed to compute the weighted score; with the score gone they only weight the
      per-criterion rows. Keep, simplify to unweighted, or drop is a separate decision. No frontend code reads
      `aiScore10` (the entity was removed with the grading screen).
    related:
      - DEC-2026-1013-remove-instructor-manual-grading
      - DEC-2026-1014-instructor-overview-trim-and-nav-label
    source: "owner answer in session 2026-10-14 (bỏ cột và quy tắc quy đổi)"
  - id: DEC-2026-1014-instructor-batch1-counting-rules
    kind: product-scope
    status: ACTIVE
    date: 2026-10-14
    subject: "Instructor open questions, batch 1: counting rules and data sources"
    decision: >
      The owner answered batch 1 of the instructor open questions on 2026-10-14. (1) INS0101 "Tổng học viên" counts
      distinct people (DISTINCT student_id), not enrolments (INS0101 Q5). (2) The submission count of an assigned
      problem is computed inside the instructor's own classes, not the system-wide problem_stats counter (INS0101 Q9;
      the same rule for INS0202 Q3, owner chose "tính theo lớp ngay" instead of the proposed system-wide label).
      (3) newStudentCount30d and its delta are dropped from the design (Q15a). (4) GetInstructorOverviewStats is
      dropped; INS0101 reads the class summary list (Q15c). (5) activity_type of "Hoạt động gần đây" keeps only
      instantaneous events (submit, publish, streak, join); sweep-based types ("vắng 9 ngày", "cần hỗ trợ") and the
      "kèm điểm AI" wording are dropped (Q7). (6) INS0201 "Hoàn thành TB" drops the "+5% so với tuần trước" delta
      (201-Q1). (7) INS0202 drops the card "Lượt nộp tuần này"; the card "AC trung bình" is relabelled all-time and
      loses its "-3%" delta and "90 ngày" wording (202-Q1, Q2).
    rationale: >
      Ponytail rung 2 for every dropped week/30-day/90-day figure (only cumulative counters exist, no weekly
      snapshot); rung 3 for the sweep-based activity types and the new-student delta (no demo use, extra job);
      rung 4 for dropping the redundant overview endpoint (reuse the class list); rung 5 for counting by person.
    consequences: >
      RD and BD of INS0101, INS0201, INS0202 updated. Prototype code (class-assignment summary fields
      weeklySubmissionCount/weeklySubmissionDelta, the matching card, the "+5%" delta) follows in the same pass. The
      per-class submission count of INS0202 needs a per-class aggregate in DD (judge-orchestration, listed for the
      DD phase); "AC trung bình" is read as per class too, to match, [SoT: Suy luận], owner to confirm if wanted
      system-wide. Distinct-person counting is documented but the prototype still sums studentCount per class (mock
      data has no overlapping enrolments), recorded in PROTOTYPE_DEBT.
    related:
      - DEC-2026-1014-instructor-overview-trim-and-nav-label
      - DEC-2026-1013-remove-instructor-manual-grading
    source: "owner answers in session 2026-10-14 (đợt 1 câu hỏi mở khu giảng viên)"
  - id: DEC-2026-1014-sandbox-child-process-cap-constant
    kind: product-scope
    status: ACTIVE
    date: 2026-10-14
    subject: "Sandbox child-process cap stays a constant (60), no admin number box"
    decision: >
      ADM0501 keeps only the on/off switch "Giới hạn tiến trình con"; the cap itself is a constant 60 set in
      application.yml, not an admin-editable number box. BD ADM0501 item NO5 "Số tiến trình con tối đa" (Sheet 5),
      its Sheet 6 row, DTO field maxChildProcesses and the EVT-4 wording are dropped. The column
      judge.sandbox_settings.max_child_processes is not added.
    rationale: >
      Ponytail rung 5: 60 is a sensible default nobody needs to tune during the demo, and the exception of
      DEC-2026-1001-admin-configurable-settings only protects inputs that already exist; it forbids adding NEW
      config fields. The prototype never had this box (UX review B7, 06-plan/reports/261014-1054-admin-ux-review/).
    consequences: >
      BD ADM0501 updated in the same pass; prototype unchanged (the switch label already says "Tối đa 60 tiến
      trình"). DD for judge-orchestration reads the cap from configuration.
    related:
      - DEC-2026-1001-admin-configurable-settings
      - DEC-2026-1010-admin-operations-regroup
    source: "owner answer in session 2026-10-14 (đợt sửa lỗi Major khu Admin)"
  - id: DEC-2026-1014-instructor-batch2-weekly-rules
    kind: product-scope
    status: ACTIVE
    date: 2026-10-14
    subject: "Instructor open questions, batch 2: week boundary, student status and list scope"
    decision: >
      The owner answered batch 2 on 2026-10-14, all as proposed. (1) INS0203 Q3: the weekly figures behind the
      student status labels ("Cần hỗ trợ", "Vắng bài") and the weekly average-score chart are computed at query time
      from the submission table and cached in Redis for 15 minutes; no weekly snapshot table or job. A week starts on
      Monday (ISO) and the current unfinished week is not counted (the chart only draws complete 7-day weeks).
      (2) INS0203 Q5: the chart draws one line, the all-class total on the "Tất cả" tab and the class line on a
      class tab. (3) INS0203 Q7: the student search box filters only the student table, not the "Cần chú ý" block
      nor the chart. (4) INS0203 Q9: the denominator of "N / tổng học viên" follows the selected class tab ("Tất cả"
      = all classes counted by distinct person, same rule as DEC-2026-1014-instructor-batch1-counting-rules).
      (5) INS0201 Q8: the sub-line of the "Vắng bài" card reads "Chưa nộp 7 ngày qua" (sliding 7-day window kept).
      (6) INS0204 Q9: the submission history keeps submissions of a problem that was soft-removed from the class,
      with the note "Bài đã gỡ khỏi lớp". (7) INS0202 Q11: with a class tab selected the columns "Lượt nộp" and "AC"
      are computed for that class; the "Tất cả" tab aggregates all the instructor's classes.
    rationale: >
      Ponytail rung 2 (query time plus short cache, no new table or job), rung 5 (ISO Monday, complete weeks only,
      one line, table-only search: sensible defaults instead of new inputs) and rung 6 (keep the student's
      submission history instead of hiding data).
    consequences: >
      RD/BD of INS0201, INS0202, INS0203, INS0204 and BD architecture/judge-orchestration (the metrics operation
      GetClassStudentSubmissionMetrics) record the rules. Frontend: no change needed for these rules except to be
      verified later; the prototype keeps its mock data (see PROTOTYPE_DEBT).
    related:
      - DEC-2026-1014-instructor-batch1-counting-rules
    source: "owner answers in session 2026-10-14 (đợt 2 câu hỏi mở khu giảng viên, chọn đề xuất)"
  - id: DEC-2026-1014-instructor-batch3-4-contracts-and-removal
    kind: product-scope
    status: ACTIVE
    date: 2026-10-14
    subject: "Instructor open questions, batches 3 and 4: endpoint ownership, class detail contract, invite code TTL, removal actions"
    decision: >
      The owner answered batches 3 and 4 on 2026-10-14, all as proposed. Batch 3: (1) INS0101 Q12: the three
      endpoints of the overview are owned by the module that owns the data (class summary list and recent activity
      in identity, assigned problems in problem-bank), not merged into identity. (2) INS0205 Q1: the class
      description is added to ClassCardDto and the class detail reuses the class list filtered by id on the client;
      no GetClassDetail endpoint. (3) INS0201 Q5 and INS0205 Q5: the default invite code TTL is a fixed 7 days (as
      03-dd/api/F1_identity.md already states), no input field to choose it, a new code can be created at any time.
      Batch 4: (4) INS0201 Q6: the "Gỡ khỏi lớp" action of a student sits at the end of each student row and is
      always visible (also usable on touch), with a confirmation. (5) INS0202 Q7 and Q6: the "Gỡ khỏi lớp" action of
      an assigned problem sits after the "Chi tiết" button, always visible; one class per removal (on the "Tất cả"
      tab the class is chosen in the confirmation dialog); the half "pass the class to problem_list" is closed
      because the assign flow is a popup on INS0202. (6) INS0204 Q10: after removing the student from the student
      detail screen the page leaves (router.back() to the caller) and the list reloads. (7) INS0201 Q10: clicking
      the already selected class card again deselects it and returns to "Tất cả".
    rationale: >
      Ponytail rung 4 (reuse the class list and keep endpoint ownership with the data owner), rung 5 (fixed 7-day TTL
      and one-class removal are defaults instead of new inputs) and rung 6 (visible, confirmed removal protects
      against accidental data loss and works on touch).
    consequences: >
      RD/BD of INS0101, INS0201, INS0202, INS0204, INS0205 record the rules; DD identity must add description to the
      class card DTO. Prototype follow-up (always-visible remove icons, one-class confirm dialog, router.back()
      after removal, card deselect) is checked against the code in a later pass and listed in PROTOTYPE_DEBT.
    related:
      - DEC-2026-1014-instructor-batch1-counting-rules
      - DEC-2026-1014-instructor-batch2-weekly-rules
    source: "owner answers in session 2026-10-14 (đợt 3 và 4 câu hỏi mở khu giảng viên, chọn đề xuất)"
  - id: DEC-2026-1014-instructor-batch5-6-nav-pagination-rubric
    kind: product-scope
    status: ACTIVE
    date: 2026-10-14
    subject: "Instructor open questions, batches 5 and 6: navigation, pagination, labels, rubric weights, cleanup"
    decision: >
      The owner answered batches 5 and 6 on 2026-10-14, all as proposed. Batch 5: (1) INS0204 Q7 and INS0205 Q3:
      on the student detail and class detail screens the sidebar lights the item of the calling screen. (2) INS0201
      Q9, INS0202 Q9, INS0205 Q4: tables that merge many rows (the student table of the class list, the assigned
      problem table) paginate at 20 rows per page; the student table inside one class (class detail) does not
      paginate; the future submission list uses the same convention. (3) INS0205 Q2: after creating a class the
      Back button returns to the class list. (4) INS0101 Q11: the overview stays static (no new drill-down links);
      INS0202 Q5: the "Chủ đề" column shows the first topic alphabetically plus a "+n" suffix, full list in the
      tooltip. Batch 6: (5) OQ-F5-B: the admin rubric weights of F5-23 are dropped; the admin keeps an editable list
      of criteria (add, edit, enable or disable) but no percent weight; the column rubric_scores.weight_percent_snapshot
      and rubric_configs.weight_percent go away (the MOCK_INTERVIEW fixed criteria keep their own fixed rule, to be
      checked by the BD owner of F5-15). (6) INS0101 Q15c rest: the "Bài giao cho lớp" block shows the 5 problems with
      the most submissions. (7) Closed as stale or duplicated: INS0101 Q1, INS0101 Q8 (INS0101 no longer shows
      "Hoàn thành TB", verify before closing), INS0202 Q8, INS0204 Q8; the RD citation SHR0101_auth.md:90 is fixed to
      :94 (INS0101 Q13). (8) The week rule ISO Monday, no unfinished week, also applies to the "Xu hướng" sparkline
      of the student table.
    rationale: >
      Ponytail rung 5 (defaults: calling-screen highlight, 20 rows, +n topics), rung 4 (reuse the list convention and
      keep one pagination rule), rung 3 and 2 (rubric weights had no remaining purpose once the weighted score was
      dropped; DEC-2026-1001-admin-configurable-settings is kept by leaving the criteria list admin-editable).
    consequences: >
      RD and BD of the instructor screens, RD ai-review F5-23 and ADM0301, BD database/ai-review (rubric_configs,
      rubric_scores), BD ADM0301 and the admin ai-config prototype (weight inputs) are updated. The old DD or code
      for weights, if any, is removed in a follow-up pass.
    related:
      - DEC-2026-1014-drop-ai-score-10
      - DEC-2026-1014-instructor-batch1-counting-rules
      - DEC-2026-1014-instructor-batch2-weekly-rules
      - DEC-2026-1014-instructor-batch3-4-contracts-and-removal
    source: "owner answers in session 2026-10-14 (đợt 5 và 6 câu hỏi mở khu giảng viên, chọn đề xuất)"
  - id: DEC-2026-1014-drop-admin-rubric-config
    kind: product-scope
    status: SUPERSEDED
    superseded_by: DEC-2026-1014-drop-solution-review-rubric
    date: 2026-10-14
    subject: "Drop the admin configuration of the solution-review rubric (F5-23); keep five fixed criteria in the learner report"
    decision: >
      The owner asked the assistant to choose how far to drop the F5-23 rubric ("theo bạn nên bỏ hết hay bỏ 1 phần
      nếu nhìn dự án dài hạn") and on 2026-10-14 the assistant recommended, and the work proceeds with, the partial
      drop (owner can veto in one sentence). Dropped: the rubric section of ADM0301 (weights, and the add/edit/enable
      list proposed earlier the same day as ADM0301 Q16), the table `ai.rubric_configs` and the admin-editable
      criteria. Kept: the five fixed criteria of the learner report (CORRECTNESS, PERFORMANCE, CLEAN_CODE,
      SCALABILITY, DATA_STRUCTURE) as part of the F5.1 response schema, versioned by `response_schema_version`
      together with the prompt version that the admin already manages; the per-criterion rows of
      `ai.rubric_scores` (score and comment, owner SOLUTION_REVIEW) and the five cards of USR0301. The criteria
      change only by releasing a new prompt/schema version, not by a separate config. The Mock Interview rubric
      F5-15 (four fixed criteria, fixed 25/30/25/20 weights) is unchanged. This replaces item (5) of
      DEC-2026-1014-instructor-batch5-6-nav-pagination-rubric ("keep an editable list of criteria"); the other
      items of that decision stay.
    rationale: >
      Ponytail rung 3 and 2 (no weighted score is computed any more, the add/edit/enable UI has no demo use) and
      long-term consistency: past reports keep the criteria they were generated with, and the criteria live with the
      prompt/schema version. DEC-2026-1001-admin-configurable-settings (existing inputs stay) is reversed for this
      one input by explicit owner delegation; it stays valid for every other setting.
    consequences: >
      RD ai-review F5-23 and ADM0301 RD, BD ADM0301 (rubric section, Q15, Q16), BD database/ai-review (drop
      rubric_configs; rubric_scores.criterion_code is a fixed set for SOLUTION_REVIEW; weight_percent_snapshot
      only for MOCK_INTERVIEW), BD architecture/ai-review, USR0301 Q4 are updated. Admin prototype: remove the
      weight editor, its state, validation and messages from views/admin/ai-config. DD ai-review must not create
      rubric_configs.
    related:
      - DEC-2026-1014-instructor-batch5-6-nav-pagination-rubric
      - DEC-2026-1014-drop-ai-score-10
      - DEC-2026-1001-admin-configurable-settings
    source: "owner delegated the choice in session 2026-10-14 (theo bạn nên bỏ hết hay bỏ 1 phần nếu nhìn dự án dài hạn); assistant recommendation applied"
  - id: DEC-2026-1014-drop-solution-review-rubric
    kind: product-scope
    status: ACTIVE
    date: 2026-10-14
    subject: "Drop the whole solution-review rubric (F5-23): admin configuration AND the five criteria cards of the learner report"
    decision: >
      The owner decided on 2026-10-14 to drop the solution-review rubric entirely, after the assistant listed the
      impact: "bỏ đi cho tôi vì học viên xem cái đấy cũng sẽ chưa rõ là 5 tiêu chí đấy phản ánh gì". Dropped: the
      rubric section of ADM0301 (weights, criteria list), the table `ai.rubric_configs`, the five criteria cards
      (CORRECTNESS, PERFORMANCE, CLEAN_CODE, SCALABILITY, DATA_STRUCTURE) of the F5.1 report shown on USR0301, the
      field `criteriaScores` of the F5-07 response schema, and the rows of `ai.rubric_scores` with owner
      SOLUTION_REVIEW (the value SOLUTION_REVIEW leaves `owner_type`). Kept: the rest of the F5.1 report (time and
      space complexity F5-02/F5-03, edge cases and risks F5-04, code-quality remark with the 1-5 readability score
      F5-05, suggested topics F5-06), prompt versioning, model choice, rate limit and regression-run parts of F5-23,
      and the Mock Interview rubric F5-15 with its table rows. This supersedes
      DEC-2026-1014-drop-admin-rubric-config (which kept the five cards as fixed) and item (5) of
      DEC-2026-1014-instructor-batch5-6-nav-pagination-rubric.
    rationale: >
      Ponytail rung 3 and 2: no weighted score is computed any more and the five cards largely duplicate F5-02 to
      F5-05; the owner judged that a learner cannot tell what the five criteria reflect. Fewer features to defend and
      to keep consistent across prompt versions.
    consequences: >
      RD ai-review (F5-23, F5-07, F5-18 wording), ADM0301, USR0301; BD ADM0301, USR0301, database/ai-review
      (drop rubric_configs, rubric_scores owner_type, criteriaScores in the report schema),
      architecture/ai-review; prototype: remove the rubric card of views/admin/ai-config and the criteria card of
      views/users/solution-review, criteriaScores of entities/solution-review, mocks, tests and i18n keys. The prompt
      text that says "đánh giá theo rubric" must be reworded. `weight_percent_snapshot` stays for MOCK_INTERVIEW only
      (open BD question remains).
    related:
      - DEC-2026-1014-drop-admin-rubric-config
      - DEC-2026-1014-instructor-batch5-6-nav-pagination-rubric
      - DEC-2026-1014-drop-ai-score-10
    source: "owner answer in session 2026-10-14 (bỏ đi cho tôi vì học viên xem cái đấy cũng sẽ chưa rõ là 5 tiêu chí đấy phản ánh gì)"
  - id: DEC-2026-1014-batch7-remaining-open-questions
    kind: product-scope
    status: ACTIVE
    date: 2026-10-14
    subject: "Last open questions of the instructor round: completion rate, rubric snapshot weight, AI config publish, model list, solution-review report behaviour, catalog counts"
    decision: >
      The owner answered on 2026-10-14. (1) INS0101 Q8: the "Lớp của tôi" block keeps the completion percentage per
      class, computed by identity (DEC-2026-0921-class-completion-owned-by-identity); the question is closed.
      (2) The column `rubric_scores.weight_percent_snapshot` is DROPPED (the owner did not take the proposal to
      keep it nullable): the fixed 25/30/25/20 weights of the Mock Interview rubric F5-15 are application constants
      tied to the response/schema version; the open question in database/ai-review is closed. (3) ADM0301 Q3: "Lưu
      và phát hành" is one atomic server-side transaction (one call, all or nothing). (4) ADM0301 Q14: the list of
      approved models is declared by the developer in the backend configuration (the admin only selects, never adds
      a model), the token price table of ADM0303 stays as a fallback, model names must equal
      ai.token_usage.model_name; whether the remote gateway returns per-call cost is NOT checked and stays a small
      open point for the time the real gateway is plugged in. (5) USR0301 Q1: applying the AI suggested code keeps the
      previous code in the draft history (localStorage, label "Bản trước khi áp dụng AI Diff") with one-click undo
      (F5-26, DD problem_detail). (6) USR0301 Q2: no regeneration of the report; the cache is hard by the source
      code hash (F5-20), a new report only for a different code. (7) USR0301 Q3: LLM timeout 30 seconds, the UI
      shows a skeleton while waiting. (8) The totals of feature_catalog.md and system_survey.md are reconciled by
      counting the tables (8 retired codes, per-module counts equal to the rows; counting the rows gave 112 live codes, not the 111 first expected, because each file lacked one real code: F1-31 in the catalog, F2-18 in the survey).
    rationale: >
      Ponytail rung 5 (constants instead of a column, one atomic call, fixed timeout), rung 6 (keep the learner's
      original code), rung 4 (model list stays in backend config as DEC-2026-1010-admin-selects-model-per-prompt
      already says) and consistency of the written report.
    consequences: >
      BD database/ai-review (rubric_scores loses weight_percent_snapshot; USR0601, USR0302, USR0501 DTO notes that
      name it are reworded), BD ADM0301 (EVT-13, Q3, Q14), BD USR0301 (Q1 to Q3 closed), BD INS0101 (Q8 closed),
      RD feature_catalog.md and system_survey.md (counts) and the regenerated feature_catalog.html.
    related:
      - DEC-2026-1014-drop-solution-review-rubric
      - DEC-2026-1010-admin-selects-model-per-prompt
    source: "owner answers in session 2026-10-14 (các câu còn lại của khu giảng viên và AI)"
  - id: DEC-2026-1014-instructor-submissions-list-rules
    kind: product-scope
    status: ACTIVE
    date: 2026-10-14
    subject: "Rules of the instructor submission list INS0206 (open questions 206-Q1 to Q10)"
    decision: >
      The owner answered the RD questions of INS0206 on 2026-10-14. (1) 206-Q1: submissions that are pending,
      compiling or judging show on the "Tất cả" filter only; no fourth filter option; "Chưa đạt" means judged and
      failed. (2) 206-Q2: default order newest first, fixed, no sort control. (3) 206-Q3: one small column with the
      testcase pass ratio of F4-13, "-" for a submission not judged yet. (4) 206-Q4: the learner name links to the
      student detail INS0204 (with the class); no link for a learner who left the class. (5) 206-Q5 (not the
      proposal): the submissions of a learner who LEFT the class stay in the list with the note "Đã rời lớp"; the
      class membership must therefore be kept as history so that the instructor's class scope stays valid for a
      submission made while the learner was in the class (design consequence for the BD, [SoT: Suy luận]).
      (6) 206-Q6: the list holds only the problems assigned to the class (same rule as INS0204), not the learner's
      own practice. (7) 206-Q7: one row per submission, attached to the class that holds the assigned problem.
      (8) 206-Q8: the sidebar group is "Quản lý bài tập đã nộp" with the items "Bài giao cho lớp" and "Bài nộp".
      (9) 206-Q9: the read-only result page for the instructor (/instructor/submissions/{id}) is specified as a
      short instructor-area section inside the RD and BD of USR0201 (same page, no duplicate document): read-only,
      no learner action, 404 outside the classes, breadcrumb back to the calling screen. (10) 206-Q10: no new F-code
      is allocated now (owner may allocate later). The prototype default page size 10 (10/20/50 selectable) differs
      from the decided 20 rows and is recorded as prototype debt.
    rationale: >
      Ponytail rung 1 (owner decisions), rung 4 (reuse INS0204's assigned-problem rule and the existing result
      page), rung 5 (fixed order, one row per submission, no extra filter), rung 6 for item 5 (keeping the
      learner's history). Item 5 is the owner's choice over the proposal and has a data cost: class membership history.
    consequences: >
      RD INS0206 and USR0201 updated; BD INS0206 written (9-sheet) and a row "Bài nộp" added to `_shell.md`
      table 3.2; BD USR0201 gets the instructor section; DD `judge-orchestration` needs a list-by-classes endpoint
      with the "left the class" attribute and `identity` must keep enrolment history; PROTOTYPE_DEBT notes the page
      size and the "Đã rời lớp" note.
    related:
      - DEC-2026-1013-instructor-submissions-list
      - DEC-2026-1013-instructor-avatar-menu-and-read-source
      - DEC-2026-1014-instructor-batch5-6-nav-pagination-rubric
    source: "owner answers in session 2026-10-14 (câu hỏi mở RD INS0206)"
  - id: DEC-2026-1014-keep-learning-history-on-leave
    kind: product-scope
    status: ACTIVE
    date: 2026-10-14
    subject: "Removing a learner from a class keeps the learning history and only marks the learner as left"
    decision: >
      The owner decided on 2026-10-14 that when an instructor removes a learner from a class (F1-26, INS0201,
      INS0204, INS0205) the learner's submissions and learning history in that class are KEPT; the learner is only
      marked as having left the class. This replaces the earlier wording "xoá luôn lịch sử làm bài trong lớp".
      Consequences: class membership is kept as history (a membership row with a left-at date instead of a hard
      delete); the instructor keeps read access to submissions made while the learner was in the class (list
      INS0206 shows them with the note "Đã rời lớp", DEC-2026-1014-instructor-submissions-list-rules); the
      learner's own data is untouched; a left learner no longer counts in the class student totals or class
      progress (to be written down by the owners of those screens).
    rationale: >
      Ponytail rung 6: deleting a learner's submission history because an instructor removed them from a class loses
      data the learner owns and breaks the instructor's own audit trail; marking is cheap (one date column).
    consequences: >
      RD F1-26 (identity), INS0201, INS0204, INS0205 and stories; BD database/identity (class_enrollments keeps
      the row with left_at), BD screens that mention deleting history; DD api/identity removal endpoint semantics;
      04-tdd criteria that expect deletion. The removal confirmation text must stop saying the history is deleted.
    related:
      - DEC-2026-1014-instructor-submissions-list-rules
    source: "owner answer in session 2026-10-14 (giữ lịch sử, chỉ đánh dấu đã rời lớp)"
  - id: DEC-2026-1014-class-leave-details
    kind: product-scope
    status: ACTIVE
    date: 2026-10-14
    subject: "Details of leaving a class: rejoin, counts, endpoint verb, confirmation text"
    decision: >
      The owner confirmed on 2026-10-14, all as proposed, the details that follow
      DEC-2026-1014-keep-learning-history-on-leave. (1) A learner who was removed and joins the class again with an
      invite code reuses the old enrolment row and `left_at` is set back to NULL (BR-09 must not use a plain
      ON CONFLICT DO NOTHING). (2) The submission count and the AC rate of INS0202 count only learners currently in the
      class (`left_at IS NULL`), consistent with the student totals and the class progress. (3) The endpoint that
      removes a learner is renamed from DELETE to `POST .../leave` in the DD identity (the semantics is "set
      left_at"). (4) The confirmation text of the removal popup is: "Học viên sẽ rời khỏi lớp. Lịch sử làm bài của học
      viên trong lớp này vẫn được giữ."
    rationale: >
      Ponytail rung 5 (reuse one row, one verb that says what it does, one short sentence) and consistency with the
      class totals.
    consequences: >
      DD identity BR-09, endpoint 27 and TDD AC-20/AC-21; BD database/identity (rejoin rule), INS0201/INS0204/INS0205
      popup text, BD INS0202 counts; RD INS0203/INS0205/INS0201 counts wording.
    related:
      - DEC-2026-1014-keep-learning-history-on-leave
      - DEC-2026-1014-instructor-submissions-list-rules
    source: "owner answers in session 2026-10-14 (chi tiết rời lớp)"
  - id: DEC-2026-1014-instructor-submissions-list-details
    kind: product-scope
    status: ACTIVE
    date: 2026-10-14
    subject: "INS0206 and the instructor read-only result page: final details"
    decision: >
      The owner answered on 2026-10-14, all as proposed. (1) INS0206 Q1: a submission of a learner who is in two classes
      of the same instructor, both assigned the same problem, is attached to the class with the latest assignment
      (`assigned_at`) not after `submitted_at`; one row per submission; no class_id column on the submission table.
      (2) INS0206 Q2: the "back to the list" navigation from the result page restores the list state (filters,
      search, page) by going back in history when the result was opened from the list. (3) INS0206 Q3: the BD/DB
      submission status set is the standard (it has SYSTEM_ERROR, not MEMORY_LIMIT_EXCEEDED); SYSTEM_ERROR appears
      only under "Tất cả", never under "Chưa đạt"; the code follows the BD later. (4) INS0206 Q4 / rejoin: when a
      learner leaves and rejoins, the old `joined_at` is kept and `left_at` goes back to NULL, so the time away counts
      as "in the class" when checking read access; accepted, no period history table. (5) USR0201 Q5: the instructor
      result page knows its calling screen from the query parameter `from=submissions|student|class` (default: the
      submission list). (6) The instructor result page does not subscribe to the realtime STOMP channel: it loads once.
      (7) The server takes the learner name and class from the submission itself and ignores `studentId`/`classId` in
      the URL (the query only hints the breadcrumb).
    rationale: >
      Ponytail rung 5 (defaults), rung 4 (reuse router back and the existing hints) and rung 6 for item (7)
      (do not let a forged URL show a wrong learner or class).
    consequences: >
      RD and BD of INS0206 and USR0201 (instructor area), BD INS0204 (entry from INS0206), BD database/identity (rejoin
      accepted), BD judge-orchestration (status set reference). Prototype follow-ups in PROTOTYPE_DEBT: the `from`
      parameter, the status set, the back navigation.
    related:
      - DEC-2026-1014-instructor-submissions-list-rules
      - DEC-2026-1014-class-leave-details
    source: "owner answers in session 2026-10-14 (chi tiết INS0206 và trang kết quả giảng viên)"
  - id: DEC-2026-1018-instructor-overview-richer
    kind: product-scope
    status: ACTIVE
    date: 2026-10-18
    subject: "Instructor overview (INS0101) prototype restored to 8 blocks with charts; two KPI cards, weekly schedule and recent submissions added"
    decision: >
      On the owner's request ("trang tong quan instructor dep va day du") and after the owner picked the blocks in
      session, the INS0101 prototype shows: two KPI cards (classes, students) restored beside the half-donut with
      per-class absent bars; "Lop cua toi" and "Bai giao cho lop" ranked bars; weekly teaching schedule from
      class.schedule; a seven-day stacked bar chart of submissions (accepted / failed / other); recent submissions
      (links to the INS0206 result page with from=submissions); recent activity. The "students needing attention"
      block was not chosen and stays out. The earlier removal of the grade-average card and score-trend chart stays.
      Shared components added for reuse by other overviews: StatCard icon variant, AgendaList, RecentList.
    rationale: >
      Ponytail rung 3 (a real user sees these during the demo) and rung 2 (every number comes from class, class
      assignment and submission mock entities). The 10-13 owner rule against small KPI strips targets detail and list
      screens, not an overview whose purpose is overview. The 7-day window is anchored on the newest mock submission
      because mock dates are fixed; real data would anchor on today.
    consequences: >
      RD/BD INS0101 note the blocks as unapproved prototype state; PROTOTYPE_DEBT records the mock anchoring and the
      pending owner approval. No API contract change: data comes from existing class, assignment and submission reads.
    related:
      - DEC-2026-1014-instructor-overview-trim-and-nav-label
      - DEC-2026-1014-instructor-submissions-list-details
    source: "owner answers in session 2026-10-18"
  - id: DEC-2026-1018-usr-open-questions-phase1-scope
    kind: product-scope
    status: ACTIVE
    date: 2026-10-18
    subject: "Student-area (USR) open questions, phase 1 (scope keep/defer/drop): KPI strips leave RD, hints and roadmap drop, drafts stay in the browser"
    decision: >
      Owner answered phase 1 of the USR open-question review in session, taking the proposed default every time.
      (1) KPI/stat strips are removed from the requirement documents too: USR0202, USR0401, USR0302 and USR0103 RD drop
      the strip; F1-07 (acceptance rate) is carried by USR0501 area A (overall figures, item myProgress.stats.acRate). (2) problem_detail keeps three tabs (statement,
      submissions, solution review); the stale hint tab in the static prototype is marked obsolete, not edited; the
      "suggestions by your weak spots" block is dropped, the no-problem state shows the latest unfinished problem plus a
      link to problem_list. (3) "In progress" = the learner's latest submission that is not yet ACCEPTED, read from
      judge.submissions, with no draft table; the code draft is stored in this browser per (problem, language,
      submission model) and labelled "Saved in this browser". (4) The buttons "Xem lo trinh" (dashboard), "Them vao phien
      gia lap toi" (USR0401) and "Doi anh" (profile) are dropped. (5) problem_list: login required, topic bar only (chip
      row dropped), tag filter deferred, no save button on table rows, class assignments grouped by class (at most 3),
      assignments have no due date. (6) problem_detail: no session timer, constraints and follow-up questions stay inside
      statement_md, Beats lives in submission_result only, the Workspace nav item redirects to the latest unfinished
      problem, problems.code is shown as is. (7) USR0401/USR0402: no IQ-nnn code on screen, core_keywords stands in for
      tags (no tag table), no "appears in n/12 sessions", practice answer submit is synchronous (PENDING shows as
      "grading", resubmit allowed), attempt history stays on both screens, no draft for practice answers; the practice
      criteria stay hidden until the answer is submitted, then feedback is qualitative plus a percentage per criterion
      (weight_percent), with no combined score and no pass threshold.
    rationale: >
      Ponytail rungs 1 and 2 for everything with no source requirement or no data source (roadmap, avatar, queue button,
      weak-spot suggestions, tags, session code, frequency, due date, timer); rung 4 and 5 for drafts and sync submit
      (reuse the browser and the existing table instead of a new one); rung 3 for the guest view and row save button
      (no demo user does it). Keeping RD and BD in step with the 2026-10-13 and 2026-10-18 owner rules stops each
      screen carrying a requirement the prototype no longer shows.
    consequences: >
      RD and BD of USR0101, USR0102, USR0202, USR0103, USR0501, USR0302, USR0401, USR0402, USR0502 and USR0601 close the
      matching questions as decided 2026-10-18. Code: problem_detail now persists the code draft in localStorage. No API,
      schema or authentication change; no new column or table.
    related:
      - DEC-2026-1001-admin-configurable-settings
      - DEC-2026-0831-remove-tiered-hints-ai-config
    source: "owner answers in session 2026-10-18 (USR open questions, phase 1)"
  - id: DEC-2026-1018-usr-open-questions-phase2-5
    kind: product-scope
    status: ACTIVE
    date: 2026-10-18
    subject: "Student-area (USR) open questions, phases 2 to 5: four new columns, behaviour defaults and limits, account security defaults, shared metric conventions"
    decision: >
      Owner answered phases 2 to 5 of the USR open-question review in session, taking the proposed default every time.
      PHASE 2 (data): testcases.explanation_md TEXT nullable (only meaningful when is_worked_example is true);
      submission_testcase_results.actual_output_snippet (cut like stderr_snippet, written only for SAMPLE testcases);
      users.target_position VARCHAR nullable (current_position keeps only the current role) and
      users.password_changed_at TIMESTAMPTZ nullable (NULL = never changed); the displayed user code is the first 8
      characters of the UUID (no column); the default submission language in Settings is a dynamic list of languages
      with judge.language_configs.enabled = true, falling back to the first enabled language when the saved one was
      switched off; overallScore is computed in the application from the 4 rubric_scores rows (no cache) and
      feedbackSummary is joined from the 4 comments (no new column); per-testcase results are kept 90 days, afterwards
      only the overall verdict stays on submissions.
      PHASE 3 (behaviour and limits): saved problems unsave uses Undo with no confirm popup, notes are edited in place
      in the table, a note is at most 1000 characters, the list is paginated server side at 20 rows (hidden when one
      page); my_submissions has no date-range filter in the first version and a submission can never be deleted or
      hidden; Beats shows only with at least 10 Accepted submissions, otherwise "not enough data to compare"; interview
      bank shows "next review in n days" after a self rating, has an "only bookmarked" switch beside the four status
      tabs, quick drill is a fixed 10 cards (configuration constant) and the difficulty filter travels as the URL
      parameter level (the USR0402 contract changes with it); workspace typed code is limited to 64 KB like an uploaded
      file (checked on the server), the judge-cluster status dot stays hidden until data exists, the last language and
      model are remembered in the browser (a stdio-only problem forces Standard I/O), Run Code creates no submissions
      row; a practice answer is at most 5000 characters (server constant, with a counter); the in-session code draft
      makes the leave-screen confirm (USR0102 EVT-21) and the overwrite confirms on language or model change (EVT-4,
      EVT-5) unnecessary, they are dropped. A mock interview reload restores the session through sessionStorage plus
      GetSessionDetail from Redis and a turn idle for 15 minutes closes the session into the result state; identity
      exposes ListMyProblemSolveStates (problem_id to state, cached in Redis 60 seconds per user) and problem-bank
      filters then paginates, ListTopicsWithProgress lives in problem-bank; the shell footer keeps a static cluster
      status sentence (no live queue count) and the header collapses into a hamburger on narrow screens.
      PHASE 4 (account and security): OAuth links are read only on the profile screen; first-time password set for an
      OAuth-only account needs only a valid session (no OTP); "email already used" is reported directly with a
      per-account rate limit; a password change revokes every other refresh token and keeps the current session; the
      profile screen is built for A1 only; the STOMP channel uses /user/queue/submissions with an ownership check in
      the interceptor; Settings: data export returns the file synchronously with a rate limit, the interview
      conversation export is owned by identity and reports an inline error when ai-review is down (button stays
      enabled), account deletion is confirmed by retyping the account email, the 30-day grace period has no self-cancel
      and the copy does not promise a restore (REVERSED the same day by DEC-2026-1018-account-deletion-grace-cancel:
      self-cancel by signing in again is allowed), "export data" and the danger zone stay in Settings, and the save button
      reports each module result separately.
      PHASE 5 (shared metrics): one fixed system time zone, Vietnam time UTC+7, for every day cut in identity and
      judge-orchestration; rubric_scores use a 0 to 10 scale; the date-range filter applies only to the chart, the
      "AC rate" and "last time" columns of the topic table and the interview-session block, while cumulative figures,
      "solved", "by difficulty" and the 12-month grid ignore it; the chart draws 7 bars for 7 days, 14 bars for 30 days
      and groups by week for "All"; the competence score stays 70 percent AC rate plus 30 percent solved ratio of the
      topic with NO difficulty weight; the 12-month grid reads a per-day read model in identity; "should prioritise"
      (topics) and "suggested problems" (concrete problems) both stay; the interview-session card shows 4 rubric bars
      (the breakdown is added to the DTO); "weakest item" is one of the 4 real rubric criteria; the solution_reviews
      result_json schema is specified before DD; the totals denominators come from problem-bank
      GetPublishedProblemCatalogSummary. Practice feedback: the percentage per criterion is an achievement level from 0
      to 100 shown together with the weight, never multiplied by the weight.
      FOLLOW-UPS: dashboard USR0601 takes F6-10 (completion of review per topic); the "Phong van cau nay" entry from
      USR0401 into USR0302 is dropped from USR0302 BD and RD (REQ-09); problem_list class assignments show at most the
      3 most recently assigned classes with no "view all" link.
    rationale: >
      Ponytail rung 1 for the four columns (each is named by an RD requirement: worked-example explanation, sample
      actual output, two profile fields, password-changed line); rung 5 for fixed time zone, fixed 10 cards, constants
      for the limits and the competence formula without a new weight column; rung 4 for reusing
      language_configs.enabled, rubric_scores and the browser for drafts and preferences; rung 6 for the submission
      delete ban, the ownership check on the STOMP channel and the session revoke on password change; rung 3 for
      dropping the date filter, guest view and the "view all classes" screen.
    consequences: >
      Four schema additions (problem-bank, judge-orchestration, identity with two columns) and ONE new read model table
      (proposed name user_daily_submission_stats(user_id, day, submission_count) in identity, fed by the SubmissionGraded
      event, name to be confirmed in DD, for the 12-month grid) are recorded in 02-bd/database. USR RD and BD close the matching questions as decided 2026-10-18; prototype code follows where the
      decision changes what is shown. 03-dd and 04-tdd do not exist for these screens yet, so no contract file changes.
    related:
      - DEC-2026-1018-usr-open-questions-phase1-scope
      - DEC-2026-1001-admin-configurable-settings
    source: "owner answers in session 2026-10-18 (USR open questions, phases 2 to 5)"
  - id: DEC-2026-1018-account-deletion-grace-cancel
    kind: product-scope
    status: ACTIVE
    date: 2026-10-18
    subject: "Account deletion: the 30-day grace period allows self-cancel by signing in again (amends the Settings Q8 line of DEC-2026-1018-usr-open-questions-phase2-5)"
    decision: >
      Owner confirmed in session: after a learner deletes the account it becomes DEACTIVATED at once, and during the
      30-day grace period signing in again with correct credentials shows the recovery banner of SHR0101 and the learner
      can cancel the deletion (RestoreDeactivatedAccount). The Settings danger-zone copy therefore says that the account
      stops working immediately, that the learner can sign in again within 30 days to cancel, and that identity details
      are anonymized after 30 days with submissions, saved problems and interview sessions kept. This reverses the
      earlier answer "no self-cancel, the copy does not promise a restore".
    rationale: >
      The grace period only means something if it allows a change of mind, and the auth screen (SHR0101 area E, endpoint
      RestoreDeactivatedAccount, banner and cancel button already built) already implements exactly that; the earlier answer
      to USR0503 Q8 rested on the sentence "token expired so no sign-in", which is true for the session but not for the
      sign-in that only opens the recovery banner. Ponytail rung 4: reuse the existing recovery flow instead of an
      admin-only unlock.
    consequences: >
      USR0503 BD and RD close Q8 on this decision and the Settings copy changes (vi and en consequenceText); 02-bd/security
      identity notes that sign-in to a DEACTIVATED account inside the grace period only opens the recovery banner. No code
      change in the auth flow.
    related:
      - DEC-2026-1018-usr-open-questions-phase2-5
    source: "owner answer in session 2026-10-18 (account deletion grace period)"
  - id: DEC-2026-1018-usr-open-questions-last-four
    kind: product-scope
    status: ACTIVE
    date: 2026-10-18
    subject: "Student-area (USR) open questions, last four: self-rating labels, pitfalls block, saved-problems empty state and withdrawn problems"
    decision: >
      Owner answered the last four open questions in session. (1) USR0401: the self-rating buttons stay the three F6-12
      levels (Biet ro / Mo ho / Quen, enum KNOWN / VAGUE / FORGOTTEN) and are chosen by the learner, not by AI; the status
      filter keeps the four tabs of the prototype (All, Not seen, Reviewing, Known); "Not seen" means there is no
      recall_ratings row and is never a self-rating choice; quick drill uses all three buttons so one press always fixes
      interval_days. The owner first asked whether this self-rating was the AI grading after an Accepted problem: it is
      not (that is F5 solution review and mock interview, or F6-08 feedback on a typed practice answer); with that
      explained the owner kept the plain self-rating and rejected an AI-suggested level. (2) USR0402: "Common pitfalls" is a
      section inside suggested_approach (Markdown), no new column, no separate Sheet 5 item. (3) USR0103: the empty state
      of /saved carries a guidance sentence ("save problems from the problem detail page"), no link; the only place to
      save stays problem_detail. (4) USR0103: a saved problem that was later unpublished or soft-hidden stays in the list
      with a warning label and its "open problem" button disabled (precedent INS0202); it still counts in the saved total.
    rationale: >
      Ponytail rung 1 for the F6-12 labels (a source requirement names them) and for keeping AI out of the self-rating
      (F5/F6-08 already own the AI judgement); rung 4/5 for the pitfalls block (Markdown already does it) and rung 3 for
      the empty-state link (no demo user needs it); rung 6 for the withdrawn bookmark (hiding it would silently lose a note
      the learner typed).
    consequences: >
      BD USR0401 Q6, USR0402 Q1, USR0103 Q1 and Q6 close as decided 2026-10-18; USR0103 prototype shows a withdrawn saved
      problem with a warning label and a disabled button. No schema or API change.
    related:
      - DEC-2026-1018-usr-open-questions-phase1-scope
      - DEC-2026-1018-usr-open-questions-phase2-5
    source: "owner answers in session 2026-10-18 (last four USR open questions)"

  - id: DEC-2026-1019-usr-fe-audit-fixes
    kind: product-scope
    status: ACTIVE
    date: 2026-10-19
    subject: "Student-area FE prototype audit: 21 owner decisions applied (list strips, kept-question filter, withdrawn notes, dashboard counts, verdict menu, topic chips)"
    decision: >
      Owner accepted the audit proposals in 06-plan/reports/261019-0814-usr-fe-prototype-audit. USR0201 student zone drops the
      small stat strip for one text line (B12). Interview bank keeps the just-touched question in a filtered list until the
      filter changes (C1). Withdrawn saved problems keep an editable note (B1). Dashboard Mock Interview block shows 3 sessions
      (D3); average per day divides by the real day count (D6); the priority topic is pinned only while unsolved problems
      remain (D9). USR0202 verdict menu has 6 values (B18). USR0302 custom-topic tab builds topic chips (C21) and the source
      list shows titles only (C25). Settings delete-account email match is case-insensitive (D38). BD is corrected to the
      build for A1, A8, A22, A28, B14, B30, C3b, C15, C20, D4, D12, D35; D4 (row link) and C20 (running-state drawer) are debt.
    rationale: >
      Ponytail rung 1 for the rule 2026-10-13 (no small strips), rung 2 for never showing invented values (average by real days),
      rung 4 for reusing existing data (topic chips). Everything else aligns documents with what is built at prototype level.
    consequences: >
      Code, RD (USR0101, USR0401) and BD (USR0101 to USR0602 student screens) updated the same day; PROTOTYPE_DEBT.md lists the
      remaining items. Shared upgrades added: ConfirmDialog confirmDisabled, formatDateVN (fixed UTC+7), FilterBar maxLength,
      toast action. 2026-10-19 follow-up, 7 open questions closed (docs only): USR0301 apply-to-workspace only overwrites the draft and navigates (rung 3); USR0103 EVT-9/10/14 stay debt (rung 3); no toast at the 100-char search cut in USR0101/0103/0202 (rung 3); USR0503 keeps the shared 3-button leave dialog (rung 4); USR0102 4.4 layout deviations accepted as design (rung 3); USR0302 Q2/Q3 deferred to the real API (rung 3) and USR0401 Q10 stays debt (rung 3); F5-28 interviewer levels follow the admin-configured dynamic levels instead of a fixed 3.
  - id: DEC-2026-1020-sequence-labels-plain-language
    kind: process-policy
    status: ACTIVE
    date: 2026-10-20
    subject: "Sequence diagram labels must be plain Vietnamese for non-technical readers: no endpoint paths, HTTP statuses, permission codes or jargon"
    decision: >
      Owner tightened the 2026-10-19 rule. Arrow, phase and alt labels in every sequence diagram are plain Vietnamese actions
      a non-technical reader understands. Endpoint paths and verbs, HTTP status codes, permission codes, error codes and
      jargon (cache, job, worker, model, prompt, popup, toast, LLM, OAuth) are removed from the figure and live in the DD.
      Lifeline names and their stereotype line stay (owner kept them). The word "token" and requirement codes (F1-14, BR-13)
      stay. Applied to the 21 admin sequences on 2026-10-20 (136 labels); drawio-uml-architect SKILL.md and gen_drawio.py
      (new technical-wording WARN) updated; vp-uml-architect and mermaid-uml-architect gained a short pointer section.
    rationale: >
      The figures go into a report read by a review board, not only by engineers. A label such as "GET /language-configs" or
      "USER_MANAGEMENT:UPDATE" reads as pasted from code. Ponytail rung 1 (owner decision). The previous rule explicitly kept
      endpoint, HTTP status and permission code, so it is superseded in that one point.
    consequences: >
      Instructor, student and shared sequences were re-read and rewritten the same day (admin 21, instructor 11, student 21, shared 6 files).
      Endpoint paths drawn earlier were [SoT: Suy luan] anyway because 03-dd/api is not written; they are now simply absent.
  - id: DEC-2026-1020-student-workspace-split-and-password-in-settings
    kind: product-scope
    status: ACTIVE
    date: 2026-10-20
    subject: "Student Workspace split into three pages; password change lives only in Settings; shared Modal portals to the shell root"
    decision: >
      Owner decisions of 2026-10-20 (student UI pass). (1) WORKSPACE SPLIT: the single problem screen becomes three routes.
      `/problems/{id}` is a read-only problem overview (new screen USR0104_problem_overview: statement, examples, per-language
      limits, recent submissions, Solution Review summary, "Giai bai toan" button); `/workspace` is a landing page listing
      unfinished drafts (new screen USR0105_workspace_home); `/workspace/{id}` is the coding screen (USR0102, content
      unchanged). Links that mean "go write code" point to `/workspace/{id}`; every other problem link stays `/problems/{id}`.
      This replaces the part of `DEC-2026-0922-users-and-admin-conflict-resolutions` (`_shell.md` muc 6 Q2, closed 2026-09-22) that made the Workspace nav item redirect to the most
      recent unfinished draft. (2) PASSWORD: changing or setting a password is offered only in Settings (USR0503, group
      "Bao mat", with the last-changed line); the Profile page (USR0502) keeps a read-only sign-in-method row. The
      strength bar is display-only; the blocking rule stays 8 to 128 characters with a letter and a digit. (3) MODAL: the shared
      `Modal` renders into the nearest `.admin-shell`, `.instructor-shell` or `.student-shell` (else `<body>`), so a dialog opened
      inside a `.glass-card` (backdrop-filter makes `position: fixed` relative to the card) is no longer trapped. (4) DEFAULTS
      confirmed: "Bai ngau nhien" opens the overview, not the Workspace; the Workspace landing page holds only in-progress drafts.
    rationale: >
      Ponytail rung 1 (owner decisions). The overview page lets a learner read the statement before committing to the editor,
      which the single screen could not do; the landing page replaces a redirect that gave no way to pick another draft.
      Password in one place avoids two entry points drifting apart. The Modal fix is at the shared layer because the defect
      applies to every dialog opened from a glass card.
    consequences: >
      Code done 2026-10-20 (pnpm check PASS, 97 files). Docs pending, tracked in
      06-plan/reports/261020-1635-usr-ui-chinh-sua-tai-lieu-can-dong-bo/report.md: new RD+BD for USR0104 and USR0105; BD edits
      for _shell, USR0101, USR0102, USR0103, USR0201, USR0202, USR0301, USR0302, USR0401, USR0402, USR0502, USR0503, USR0601;
      RD USR0102 and USR0502; PROTOTYPE_DEBT. `DEC-2026-0922-users-and-admin-conflict-resolutions` (Q2) and `DEC-2026-1018-usr-open-questions-phase2-5`
      stay ACTIVE for everything except the Workspace redirect.
  - id: DEC-2026-1021-student-open-questions-batch-and-run-results-dialog
    kind: product-scope
    status: ACTIVE
    date: 2026-10-21
    subject: "Student open-question batch (USR0102, 0104, 0105, 0101, 0401, 0402) and the Workspace run-results sub-screen"
    decision: >
      Owner decisions of 2026-10-21. (1) WORKSPACE (USR0102): Run Code and Submit open a sub-screen (dialog) listing every
      testcase; it opens immediately on click and shows a running state until the result arrives; the small console keeps only
      the passed/failed totals, metrics and a "Xem chi tiet" button. Hidden testcases show only Passed or Failed, never their
      input or output (README prototype rule; ponytail rung 6), also after a failed Submit. (2) USR0104: recent submissions are
      newest first, every status has a label, each row links to its submission result; "Quay ve danh sach" always goes to
      `/problems`; the Solution Review card links to the full report of the latest accepted submission. (3) USR0105: "in
      progress" means submitted at least once and not Accepted (a browser-only draft never counts); Random picks among
      unsolved problems including in-progress, server side; empty-pool toast has its own key; loading/error/login states are
      prototype debt. (4) USR0101: the in-progress block stays at 3 cards with a "Xem tat ca" link to `/workspace` and shares one
      component with USR0105. (5) USR0401: the filter row uses the shared FilterBar. (6) USR0402: the mark button reads
      "Da danh dau" when set, the History card shows in both modes, Keywords only in study mode.
    rationale: >
      Ponytail rung 1 (owner decisions) with rung 4 for reuse (shared FilterBar, one in-progress component) and rung 6 for not
      leaking hidden testcases (it would let a learner memorise answers and defeats the testcase-set versioning of F2). The
      sub-screen replaces a console that overflowed once a problem had ten or more samples.

  - id: DEC-2026-1022-identity-schema-sync
    kind: schema
    status: ACTIVE
    date: 2026-10-22
    subject: "identity schema sync: account status source, forced password change, email verification OTP, class progress computed on read, activity source, single preferences table, invite code format, default INSTRUCTOR permissions"
    decision: >
      Owner decisions of 2026-10-22 (DB docs sync, phase 0 and 1). (1) ID-02: add identity.users.deactivation_source (SELF
      or ADMIN); the anonymisation job (F1-16) scans only SELF rows past the 30-day grace; Admin lock keeps status =
      DEACTIVATED and is never anonymised by the job. (2) ID-03: add users.must_change_password BOOLEAN set when an Admin
      creates an account. (3) ID-04: keep PENDING_VERIFICATION and add an email OTP flow reusing the F1-17 mechanism (Redis
      key identity:emailverify:<userId>, a verify endpoint, a new F1-nn code in RD). (4) ID-05: drop the
      class_completion_stats read model; completionPct is computed on read as INS0101 V0.13 says; DD BR-12 and endpoint 29
      change. (5) ID-10: ADM0101 active-user numbers (30 days, 7-day sparkline, monthly retention) come from
      refresh_tokens.issued_at; expired tokens are kept at least 6 months and an index on issued_at is added;
      users.last_active_at only feeds the last-active display. (6) ID-07: one table identity.user_preferences (not
      notification_preferences); USR0503 BD is renamed. (7) Class invite code: VARCHAR(8), upper-case letters and digits
      without 0/O/1/I, server generated. (8) Default permission seed: ADMIN full; INSTRUCTOR full CRUD on CLASS_MANAGEMENT,
      PROBLEM_AUTHORING and TESTCASE_MANAGEMENT; STUDENT all false (120 rows).
    rationale: >
      Ponytail rung 6 for (1) (a wrong anonymisation loses data for good), rung 1 for (2), (3), (7), (8) (owner choice, RD
      F1-13, F1-17, F1-23 to F1-28), rung 4 for (4) and (6) (compute or reuse instead of a new table), rung 3 for (5) (the
      dashboard shows these charts in the demo).
    consequences: >
      Docs pending (see 06-plan/261022-0920-db-docs-sync-checklist.md): F1_identity.md DB rows, ARCH identity, DD logic BR-08
      and BR-12, DD api, ADM0201, ADM0101, USR0503, RD (new F1 code for email verification).
  - id: DEC-2026-1022-problem-bank-schema-sync
    kind: schema
    status: ACTIVE
    date: 2026-10-22
    subject: "problem-bank schema sync: constraints_md, reference solution table, testcase origin/approved/category, worked_examples table, merge problem_specs with function_signatures, problem code and submission limit defaults"
    decision: >
      Owner decisions of 2026-10-22 (DB docs sync). (1) PB-03: add problems.constraints_md TEXT; statement_md stays for
      learners and the learner screen joins both parts (USR0102 Q5 changes). (2) PB-04: new table
      problem.problem_reference_solutions (problem_id UNIQUE FK, language, source_code, updated_at), one row per problem;
      the try-run result is transient and not stored. (3) PB-05: testcases gets origin ENUM(MANUAL, AI), approved BOOLEAN,
      category nullable (coverage-matrix label) in place of the single is_ai_generated_draft flag; security problem-bank
      filters by approved. (4) PB-08: new table problem.worked_examples (id, problem_id FK, position, input, output,
      explanation_md); testcases.is_worked_example and testcases.explanation_md are removed. (5) PB-10: merge
      function_signatures into problem_specs; judge.submissions pins by spec_version instead of problem_spec_id. (6) PB-15:
      max_submissions_per_hour NULL means read the system default (60) at check time; the default is not copied into the
      column. (7) PB-16: problems.code is VARCHAR(64), server generated from the title as an upper-case slug with -2, -3
      suffixes on collision, also for duplicates (F2-16).
    rationale: >
      Ponytail rung 1 for (1) to (4) and (7) (RD F2-14, F2-16, F2-18 and the prototype need them), rung 4 for (5) (a 1-to-1
      table is redundant), rung 5 for (6).
    consequences: >
      Docs pending: F2_problem-bank.md DB rows (1.1, 1.4, 1.5, 1.6), F3_harness.md references to 1.4 and 1.5, judge-
      orchestration.md (spec_version pin), SHR0202, SHR0203, USR0102 Q5, security problem-bank lines 104-107, ARCH problem-
      bank.
  - id: DEC-2026-1022-interview-bank-schema-sync
    kind: schema
    status: ACTIVE
    date: 2026-10-22
    subject: "interview-bank schema sync: display_no for admin, two meanings of review due, drop practice_history.questions_total, title is the question field, fixed 1/3/7 day recall intervals"
    decision: >
      Owner decisions of 2026-10-22 (DB docs sync). (1) IB-01: add interview_questions.display_no INT UNIQUE from a server
      sequence, shown only in the admin and instructor areas; learner screens show no code (USR0401 Q3); the prototype keys
      by id instead of code. (2) IB-03: record two names: due (next_review_at <= now(), list F6-09) and reviewing (self-
      rating VAGUE or FORGOTTEN, the USR0401 tab); screens stop reusing one phrase. (3) IB-04: drop
      practice_history.questions_total; totals are COUNT of ACTIVE questions per topic at read time; questions_attempted and
      last_practiced_at stay; the consumer is USR0601, not USR0501. (4) IB-06: title is the short question field
      (VARCHAR(255) NOT NULL), content_markdown is optional extra text. (5) IB-16: recall intervals are fixed: FORGOTTEN 1
      day, VAGUE 3 days, KNOWN 7 days, no accumulation; the x2 with cap 60 days formula is dropped.
    rationale: >
      Ponytail rung 1 for (1) (owner and SHR0301 Q5), rung 4 for (3) (compute instead of keeping a snapshot that drifts),
      rung 5 for (5) (a constant table beats a formula nobody confirmed), rung 3 for (2).
    consequences: >
      Docs pending: F6_interview-bank.md rows, ARCH interview-bank sections 6 and 10, SHR0301, SHR0302 Q5, SHR0303, USR0401,
      USR0402, PROTOTYPE_DEBT (key by id).
  - id: DEC-2026-1022-ai-judge-schema-sync
    kind: schema
    status: ACTIVE
    date: 2026-10-22
    subject: "ai-review and judge schema sync: interviewer_level stays a 4-value enum, early finish scores the session, one OPEN anomaly alert per user, monthly budget rollover job, no effective-limit snapshot on submissions"
    decision: >
      Owner decisions of 2026-10-22 (DB docs sync). (1) AI-03: ai.interview_sessions.interviewer_level and
      identity.user_preferences.preferred_interview_level stay ENUM(INTERN, JUNIOR, MIDDLE, SENIOR); RD F5-28 and USR0302
      lose the wording that levels are admin configurable. (2) AI-04: early finish yields COMPLETED with a score computed
      from the turns so far; ABANDONED only when a session idles 15 minutes without enough turns and is never scored;
      USR0501 keeps ignoring ABANDONED. (3) AI-16: the hourly anomaly job writes no new row while an OPEN row of the same
      user exists; checked in the application, no unique constraint. (4) AI-17: a scheduled job at the start of each month
      sets ai_token_budget_configs.period_start to day 1 and clears locked_at. (5) JD-08: judge.submissions stores no
      effective time or memory limit and no config snapshot; the DB doc says so explicitly.
    rationale: >
      Ponytail rung 1 for (1) and (2) (owner choices that keep RD and the screens consistent), rung 5 for (3) and (4), rung
      3 for (5) (no RD asks for limit traceability and learners never see it).
    consequences: >
      Docs pending: F5_ai-review.md and F4_judge-orchestration.md (done in phase 1 where possible), RD F5-28, USR0302 Q3, USR0501,
      ARCH ai-review sections 4, 6.3, 6.4, 03-dd/jobs for the monthly job.
  - id: DEC-2026-1022-harness-empty-schema-kept
    kind: architecture
    status: ACTIVE
    date: 2026-10-22
    subject: "The harness schema stays and stays empty; no tables"
    decision: >
      Owner decision of 2026-10-22 (DB docs sync, XS-01 and HN-01). Keep CREATE SCHEMA harness in
      05-coding/database/init/01-schemas.sql so the six-schema layout of SYS0301 stays; the schema has no tables. The SQL
      comment, .nexa/domain-registry.json scope for harness, and .nexa/control/dependency-map.md (the line about templates
      and type schemas in PostgreSQL, and the open item on whether harness owns a schema) are corrected to say no tables,
      pure computation. The ERD shows harness as zero tables.
    rationale: >
      Ponytail rung 4: reuse the existing layout; dropping the schema would change search_path, SYS0301 and the name mapping
      for no benefit. 02-bd/database/F3_harness.md already decided no tables.
    consequences: >
      Pending edits: 01-schemas.sql comment, domain-registry.json, dependency-map.md.
  - id: DEC-2026-1022-db-sync-round2
    kind: schema
    status: ACTIVE
    date: 2026-10-22
    subject: "DB docs sync round 2: Admin-created accounts verified by first login, activity calendar read from identity, soft forced password change, SANDBOX_FAULT written by the worker, SubmissionGraded payload, outbox cleanup, ABANDONED session display, sample-answer result not shown, no optimal-complexity input, category display, worked examples endpoint"
    decision: >
      Owner decisions of 2026-10-22 (DB docs sync, phases 3 to 7 follow-up). (1) Email verification: a self-registered
      account stays PENDING_VERIFICATION until the email OTP is entered; an Admin-created account becomes ACTIVE on its
      first successful sign-in with the temporary password (the recipient has proven access to the email), then
      must_change_password applies. This narrows item (3) of DEC-2026-1022-identity-schema-sync and the PENDING_VERIFICATION
      status of DEC-2026-1011-admin-system-open-questions-closed to self-registration. (2) Class completion percentage is
      the average of each enrolled student's completion (RD F1-28, INS0201), computed on read. (3) The 12-month activity
      grid of USR0601 is read from identity.user_daily_submission_stats through a new identity endpoint GET /me/activity-
      calendar; this replaces the grid part of DEC-2026-0927-submission-metrics-two-ports (the per-day chart of USR0601 area
      C keeps the judge source because it filters by topic). (4) Forced password change is soft: the UI redirects to the
      password screen while must_change_password is true; the server does not block other endpoints. (5)
      judge.submissions.failure_reason = SANDBOX_FAULT is written by the worker when go-judge reports an infrastructure
      error that is not a testcase verdict. (6) The SubmissionGraded event carries submittedAt and firstAttemptForProblem
      (computed by judge) in addition to the current fields, so identity can derive the UTC+7 day and
      first_try_accepted_count. (7) outbox_events rows already published are removed by a cleanup job after 90 days (a
      constant in application.yml); the infrastructure-event message of ADM0404 is built in the application from
      publish_attempts and status. (8) A session in stage ABANDONED has no result screen: returning to it opens the entry
      screen with a toast explaining that the earlier session closed after 15 idle minutes without enough turns. (9) SHR0203
      drops the passed/total cell of the reference solution (its run result is not stored). (10)
      problems.known_optimal_complexity gets no input field; Solution Review skips the optimum comparison when it is NULL
      (RD F5-03 limitation noted). (11) SHR0202 shows testcases.category as a Case type item and the coverage matrix at the
      review step. (12) Worked examples are returned with GetProblemForWorkspace (one load of the statement tab); no
      separate endpoint. (13) Defaults applied without a question: ADM0201 shows SELF-deleted accounts as locked and
      unlocking cancels the deletion; rubric description stays NULL with no input; suggested_approach,
      sample_answer_framework and core_keywords stay CSV-only; the SHR0302 error reads that the question must not be empty;
      prototype differences (AI-10, AI-19, IB-01) stay prototype debt.
    rationale: >
      Ponytail rung 1 for (1), (3), (8), (11), (12) (owner choice that keeps RD, BD and DB consistent), rung 6 for (5) and
      (7) (infrastructure errors must stay distinguishable and the outbox must not grow without bound), rung 2 for (9)
      (never show a number with no stored source), rung 3 for (10) (nobody enters it during the demo), rung 5 for (4).
    consequences: >
      Docs pending: identity DB and ARCH, DD identity (api, logic, validation), TDD identity, RD F1-33, ADM0201 Q9 and Q10,
      USR0601 Q12, USR0302 Q9, USR0102 Q18, SHR0202, SHR0203, ADM0404 Q5, DB judge-orchestration and ai-review (outbox
      cleanup), ARCH judge-orchestration (payload, SANDBOX_FAULT), 03-dd/api/api.md catalog; 03-dd/jobs has no file yet for
      the outbox cleanup and the monthly budget reset jobs.
  - id: DEC-2026-1022-self-register-pending-login-blocked
    kind: schema
    status: ACTIVE
    date: 2026-10-22
    subject: "Self-registered PENDING_VERIFICATION accounts cannot sign in until the email OTP is entered; related identity defaults (active-user card source, activity grid level, reset clears flags, unlock blocked after anonymisation)"
    decision: >
      Owner decision of 2026-10-22 (follow-up of DEC-2026-1022-db-sync-round2 item 1). (1) A self-registered account in
      PENDING_VERIFICATION is refused at sign-in with HTTP 403 and a new error code (proposed IDT-113) until the email OTP
      is entered; an Admin-created account is not affected (first sign-in with the temporary password activates it).
      Defaults applied by BD precedence and ponytail without a separate question: (2) endpoint #41 (admin overview active
      users) follows ADM0101: a 30-day window counted from refresh_tokens.issued_at, not users.last_active_at over 24 hours;
      (3) the activity-calendar endpoint returns submissionCount per day only; the 5-level shading is a display concern
      computed by the screen; (4) a password reset through OTP (BR-04) also clears must_change_password and moves an Admin-
      created PENDING_VERIFICATION account to ACTIVE, because the reset proves control of the email; (5) the Admin unlock
      button is disabled for a SELF-deleted account whose anonymized_at is not NULL, since identifying data cannot be
      restored; (6) 03-dd/api/api.md is not created yet (03-dd/api/F1_identity.md section 0 is the catalog) until a second
      module gets its DD.
    rationale: >
      Ponytail rung 1 for (1) (owner choice), rung 6 for (4) and (5) (security and data-loss edge cases), rung 4 for (6) (do
      not duplicate the identity catalog in a second file), rung 5 for (3).
    consequences: >
      Docs: 03-dd/logic/F1_identity.md BR-21 and BR-04, 03-dd/api/F1_identity.md (error code, #41), 03-dd/validation/F1_identity.md
      (new code), 04-tdd/F1_identity.md, USR0601 BD (level), ADM0201 BD (unlock rule).
  - id: DEC-2026-1022-db-sync-round4
    kind: schema
    status: ACTIVE
    date: 2026-10-22
    subject: "DB docs sync round 4: per-problem AI context columns, cache-hit review row, ClassDeleted event, interview defaults in user_preferences"
    decision: >
      Owner decisions of 2026-10-22 (DB docs sync, final open items). (1) PB-07: problems gets three columns ai_brief TEXT,
      no_full_code BOOLEAN, socratic_only BOOLEAN (AI authoring context per problem); the field allowHiddenHint is removed
      from the SHR0202 DTO. (2) AI-11: a Solution Review cache hit creates a new ai.solution_reviews row for the new
      submission, copies result_json and sets served_from_cache = true; submission_id stays unique and the
      SolutionReviewCompleted event is still published. (3) ID-13: hard-deleting a class makes identity publish
      ClassDeleted; problem-bank soft-removes the matching class_assignments rows (removed_at = now()) and
      identity_recent_activity.scope_class_id is set to NULL; class_invite_codes and class_enrollments follow by ON DELETE
      CASCADE. (4) ID-23: user_preferences gets preferred_max_turns and preferred_hint_allowed (per-user defaults of the
      mock interview); the frontend passes them into interview_sessions when a session is created.
    rationale: >
      Ponytail rung 1 for (1) and (4) (prototype and Settings need them), rung 6 for (3) (no dangling references after a
      hard delete), rung 4 for (2) (keep submission_id unique, no mapping table).
    consequences: >
      Docs: DB problem-bank, ai-review, identity; ARCH ai-review 6.2 and identity events; SHR0202 DTO; USR0503; DD api
      identity preferences and ClassDeleted; DD logic class delete; TDD identity.
      Follow-up 2026-10-22 (same session): (5) problems.no_full_code and problems.socratic_only default to true (matches the
      prototype mock and the SHR0202 form). (6) ai_cache_entries keeps its key; changing the per-problem AI columns does not
      invalidate old cache entries (limit documented).
  - id: DEC-2026-1023-db-actor-review-fixes
    kind: schema
    status: ACTIVE
    date: 2026-10-23
    subject: "DB review against the three actors: LANGUAGE_CONFIG becomes the 11th seed function, bootstrap ADMIN at startup, interview_questions.created_by NOT NULL, UUID type convention for reference columns"
    decision: >
      Fixes from the 2026-10-23 review of the database docs against actors A1, A2 and A3
      (06-plan/reports/261023-0831-db-vs-3-actor-diem-venh/report.md, points 1, 3, 5, 14). (1) The functions seed gains
      LANGUAGE_CONFIG (F4-11) as an 11th row; the permissions seed grows from 120 to 132 rows (ADMIN true for all four
      actions, INSTRUCTOR and STUDENT false). It gates language_configs, grading_defaults, sandbox_settings and
      generator_sandbox_settings and screen ADM0501. (2) The first ADMIN account is seeded at application startup, not by an
      SQL migration: when no user holds the ADMIN role, a runner creates one from the environment variables
      ALGOPREP_BOOTSTRAP_ADMIN_EMAIL and ALGOPREP_BOOTSTRAP_ADMIN_PASSWORD (names proposed, DD to confirm) with
      status PENDING_VERIFICATION and must_change_password = true; idempotent; no audit row; startup aborts if the
      variables are missing while no ADMIN exists. (3) interview_bank.interview_questions.created_by becomes NOT NULL; no
      index and no updated_by. (4) Every reference column, including cross-schema ones without a physical foreign key, is
      UUID; stated once per DB doc instead of per cell.
    rationale: >
      Ponytail rung 6 for (2) (without it nobody can sign in to the admin area), rung 1 for (1) (ADM0501 already checks this
      function; the seed was incomplete), rung 3 for (3) (no screen filters by author, so no index and no updated_by), rung 4
      for (4) (one convention line instead of editing about 15 cells). Option not taken for (1): fold the checks into
      JUDGE_QUEUE_MONITOR, which would avoid new seed rows but name a function after queue monitoring while gating language
      settings.
    consequences: >
      Docs updated: 02-bd/database/{F1_identity,F2_problem-bank,F4_judge-orchestration,F5_ai-review,F6_interview-bank}.md,
      02-bd/security/{F4_judge-orchestration,F2_problem-bank}.md, 02-bd/screens/admin/ADM0201 (function count),
      01-rd/req/F1_identity.md (F1-12, F1-13), 01-rd/overview/system_survey.md. Follow-ups done the same day under
      DEC-2026-1023-db-actor-review-decisions: prototype permission matrix (types, mock, vi/en labels), 03-dd/logic/F1_identity.md,
      04-tdd/F1_identity.md (AC-45 to AC-47), 08-diagram/erd report (diagram spec files need no change).
  - id: DEC-2026-1023-db-actor-review-decisions
    kind: architecture
    status: ACTIVE
    date: 2026-10-23
    subject: "DB review against the three actors, owner choices: AuditLogPort for cross-module audit, Solution Review prototype follows the DB schema, all proposed seed values confirmed, matrix label, ADM0303 Q4 defaults"
    decision: >
      Owner choices of 2026-10-23 (follow-up of DEC-2026-1023-db-actor-review-fixes). (1) Cross-module audit: problem-bank,
      interview-bank and judge-orchestration record rows into identity.system_audit_logs only through an inbound port
      AuditLogPort exposed by module identity, in the same JVM and the same DB transaction as the business change; no
      direct write to the identity schema. Option not taken: an asynchronous AuditRecorded domain event (loses the
      same-transaction guarantee already stated in the security BDs). (2) Solution Review: the prototype
      (entities/solution-review and its views) is changed to match ai.solution_reviews.result_json (complexity.*,
      codeDiff.suggestedCode/explanation, no diffLines, no diffNote, no suggestedCode at root); the DB schema is unchanged.
      (3) Every seed value proposed by the BD is confirmed: language multipliers (time Java 2.00, C++ 1.00, Python 3.00;
      memory Java 2.00, C++ 1.00, Python 1.00), AI rate limits (10 solution reviews per hour, 3 interviews per hour, 20
      practice feedbacks per hour), anomaly threshold 5x the average, ChatMemory TTL 2 hours, interview 'enough turns'
      threshold 4, AI testcase generation cap range 1 to 10 (default 2), problem_stats.ac_rate stored as a column. (4) The
      permission-matrix row for LANGUAGE_CONFIG is labelled 'Cấu hình ngôn ngữ & sandbox' (en: 'Language & sandbox
      config'). (5) Defaults taken by the assistant, reversible: ADM0303 Q4 keeps the prototype behaviour (negative price
      becomes 0, deleting a model is not blocked, renaming a model leaves old usage rows without a price, the cost column
      shows a dash); on ADM0501, READ on LANGUAGE_CONFIG opens the screen and UPDATE enables saving, CREATE and DELETE have
      no effect.
      (6) Solution Review screen USR0301: followUpTopics gets a display item (topic chips plus one link to the question bank,
      unfiltered); no summaryText card is added and the schema is unchanged; the prototype keeps its simplified line diff and
      the Monaco Diff Editor is used when the API is wired (PROTOTYPE_DEBT).
    rationale: >
      Ponytail rung 6 for (1) (keep audit atomic with the change), rung 4 for (2) (the Monaco diff editor already computes
      the diff, so no extra keys in the schema), rung 5 for (3) (capstone demo, every value is editable by the admin in the
      UI), rung 5 for (5).
    consequences: >
      Docs: 02-bd/architecture/{F1_identity,F2_problem-bank,F6_interview-bank,F4_judge-orchestration}.md (port), 02-bd/security/*,
      02-bd/database/* (confirmations, AuditLogPort note, closed points), 03-dd/jobs/ created (identity, judge-orchestration,
      ai-review), 03-dd/logic/F1_identity.md (BR-08, BR-18, new BRs), 04-tdd/F1_identity.md. Code: permission-matrix and
      solution-review slices of 05-coding/frontend.
  - id: DEC-2026-1023-open-questions-closed
    kind: scope
    status: ACTIVE
    date: 2026-10-23
    subject: "Open questions closed: SHR0302 Q2 preview from the unsaved form, USR0401 Q13 total is all ACTIVE questions, SHR0302 Q6 rubric kept without seeded names, job lock and sweep rules"
    decision: >
      Owner choices of 2026-10-23. (1) SHR0302 Q2: the "Xem như học viên" popup is built on the client from the current,
      unsaved form; no save is required and GetInterviewQuestionDetail is not called. (2) USR0401 Q13: in "Hiển thị {shown} /
      {total} câu", total is the number of ACTIVE questions in the whole bank (no filter applied) and shown is the number
      matching the filters; ListInterviewQuestions returns both numbers.
      (3) SHR0302 Q6: the per-question answer rubric (F6-13, answer_rubrics) is kept; no suggested criterion names are seeded, the
      author types them. (Only the Solution Review rubric, F5-23, was dropped on 2026-10-14.)
      (4) Background jobs (03-dd/jobs): a single-instance job is guarded by a Postgres advisory lock
      (pg_try_advisory_xact_lock on a hash of the job code) in all three modules, relays use FOR UPDATE SKIP LOCKED, no
      shedlock table and no Redis lock; the abandoned-interview sweep counts turns with turn_count and retries a failed auto-score
      3 cycles before ABANDONED; the anomaly alert baseline is the average over users that used AI in the 24 h window, ADMIN excluded.
      The ADMIN-excluded baseline clause only is overridden by DEC-2026-1028-qa-f5-anomaly-baseline-admin;
      all other choices in this entry remain active.
      The other job values (schedules, batch sizes, relay period) were accepted as proposed.
    rationale: >
      Ponytail rung 5 for (1) (no extra call, drafts can be previewed), rung 2 for (2) (the user sees how much the filters
      narrow the bank).
    consequences: >
      Docs: SHR0302 V0.16, USR0401 V1.15, 02-bd/database/F6_interview-bank.md, 03-dd/jobs/*.md; DD interview-bank must return shown and total from ListInterviewQuestions.
  - id: DEC-2026-1023-module-docs-f-code-names
    kind: process-policy
    status: ACTIVE
    date: 2026-10-23
    subject: "Module-axis documents are named F{n}_{slug}.md with the Bounded Context code"
    decision: >
      Owner decision of 2026-10-23: every module-axis document carries the Bounded Context code in its file name, F1 identity,
      F2 problem-bank, F3 harness, F4 judge-orchestration, F5 ai-review, F6 interview-bank, written F{n}_{slug}.md, and the same
      name is used in every tier: 01-rd/req, 02-bd/{architecture,database,security,storage}, 03-dd/{api,logic,validation,jobs},
      04-tdd. Screen-axis files keep their existing screen codes. Done by renaming 44 files and rewriting every path reference
      (about 6,150 occurrences in 354 files, including historical plans, reports and this registry so citations still resolve);
      archives were left untouched. Convention text updated in CLAUDE.md and in the skills and commands that used the <module>.md
      placeholder.
    rationale: >
      File names now sort and group like the screen documents (USR0101_...), and the code ties a file to the Bounded Context
      table. The rename covers all tiers so the same-slug-across-tiers rule of CLAUDE.md still holds.
    consequences: >
      Planned files follow the same pattern: 03-dd/api/F5_ai-review.md, 03-dd/api/F6_interview-bank.md, 04-tdd/F5_ai-review.md.
      Citations in older documents use the new names; line numbers are unchanged because file contents were not edited.
      A backup of the pre-rename files exists only in the session scratch area, not in the repository.
  - id: DEC-2026-1023-drop-ai-regression-run
    kind: product-scope
    status: ACTIVE
    date: 2026-10-23
    subject: "Drop the AI config 'Chạy đối chiếu' (pre-release comparison run) card"
    decision: >
      Owner decision of 2026-10-23: remove the "Trước khi phát hành" card and its disabled "Chạy đối chiếu" button from
      ADM0301 (admin AI config), in the prototype, RD and BD. The feature is dropped for good, not deferred: no backend, no
      endpoint, no table. This replaces the 2026-08-24 decision (PROTOTYPE_DEBT 2.8 part 2) that kept it on the UI with the backend
      deferred.
    rationale: >
      Ponytail rung 2 (no real data source: the comparison existed to diff rubric scores, and the solution-review rubric was dropped by
      DEC-2026-1014-drop-solution-review-rubric, so there is nothing left to compare; no backend was ever designed) and rung 3
      (nobody uses a permanently disabled button in the demo).
    consequences: >
      Docs: 01-rd/req/F5_ai-review.md (F5-23), 01-rd/screens/admin/ADM0301_ai_config.md, 01-rd/overview/system_survey.md,
      02-bd/screens/admin/ADM0301_ai_config.md V2.5, 02-bd/architecture/F5_ai-review.md, 02-bd/database/F5_ai-review.md,
      06-plan/PROTOTYPE_DEBT.md 2.8. Code: admin-ai-config-view.tsx card removed, i18n keys preReleaseTitle, preReleaseBody,
      runComparison removed from vi.json and en.json. Risk R3 loses one of its cut items. Strikethrough kept in the BD sheets so
      row numbers do not shift.
  - id: DEC-2026-1023-dd-f5-f6-open-choices
    kind: product-scope
    status: ACTIVE
    date: 2026-10-23
    subject: "DD F5/F6: AI decides Mock Interview stage change, F6 topic and level slugs are auto-generated, DD order F5 then F6"
    decision: >
      Owner choices of 2026-10-23 when planning the F5/F6 DD. (1) Mock Interview stage change: the AI returns a stageComplete flag in
      each turn's structured JSON; the backend only enforces a per-stage turn ceiling so a session cannot stall. (2) The code (slug) of
      a topic or question level that an admin creates in F6 is generated from the name, with a numeric suffix on collision; it is
      immutable after creation and a rename does not change it; no input field is added. (3) DD order: F5 (ai-review) first, then F6
      (interview-bank); screen-axis DD follows the modules, grouped by actor.
    rationale: >
      (1) Matches the BD proposal (02-bd/architecture/F5_ai-review.md section 10) and needs the least code. (2) Ponytail rung 5
      (a sensible default exists). (3) F6 calls the F5 token-budget port (DEC-2026-1011-ai-practice-feedback-in-shared-budget), so the
      port signature has to exist first.
    consequences: >
      Written into 03-dd/api/F5_ai-review.md, 03-dd/api/F6_interview-bank.md and their validation files. Closes the open items at
      02-bd/architecture/F5_ai-review.md section 10 (stage condition) and 02-bd/architecture/F6_interview-bank.md section 10 (slug).
  - id: DEC-2026-1023-dd-f5-budget-lock-and-breaker
    kind: product-scope
    status: ACTIVE
    date: 2026-10-23
    subject: "DD F5: budget lock exempts open Mock Interview sessions, no Resilience4j, stage turn ceiling 6 (max 18), F2-14 not blocked by the lock"
    decision: >
      Owner choices of 2026-10-23 on the F5 DD open questions. (1) When the token budget is locked, only creation of new sessions and new
      Solution Review reports is refused; turns and scoring of an already open Mock Interview session still run. This amends RD F5-25 as
      applied to open sessions and removes the "retry 3 cycles then ABANDONED" path of JOB-AI-04 for budget reasons. (2) Resilience4j is not
      added to the locked stack; LLM calls use a timeout and error code AIR-501 only. (3) Per-stage turn ceiling 6 when the AI does not
      return stageComplete, so a SUBMISSION session has at most 18 turns; a constant in application.yml. (4) The budget lock does not block
      test-script generation F2-14; it still records tokens.
    rationale: >
      (1) A learner must not be cut off mid-interview; ponytail rung 6. (2) Ponytail rung 4: the stack has no such dependency and a timeout
      already keeps F1-F4 running. (3) Ponytail rung 5: a sensible default as a constant. (4) Authoring is not learner spend; rung 6.
    consequences: >
      03-dd/api/F5_ai-review.md, 03-dd/logic/F5_ai-review.md, 03-dd/jobs/F5_ai-review.md (JOB-AI-04), 04-tdd/F5_ai-review.md updated to
      match. RD 01-rd/req/F5_ai-review.md F5-25 needs a one-line amendment (open-session exemption). Other F5 DD open questions keep the
      defaults listed in 03-dd/api/F5_ai-review.md section 35 until the owner says otherwise.
  - id: DEC-2026-1023-dd-f6-practice-budget-and-prompt
    kind: product-scope
    status: ACTIVE
    date: 2026-10-23
    subject: "DD F6: locked budget rejects practice feedback without saving the turn, question text added to AnswerFeedbackPort, progress numerator counts ACTIVE only, ClassScopeQueryPort dropped from F6"
    decision: >
      Owner choices of 2026-10-23 on the F6 DD open questions. (1) When the shared token budget is locked, the practice-mode AI feedback
      call (F6-08) is refused with 429 INB-042 and the turn is NOT saved; a turn row with status FAILED is saved only when the AI fails or
      times out. ADMIN is exempt. This follows DEC-2026-1011-ai-practice-feedback-in-shared-budget and USR0402 and replaces the opposite
      wording in 02-bd/architecture/F6_interview-bank.md. (2) AnswerFeedbackPort gains a QuestionData parameter (title, contentMarkdown)
      passed in the user message as data, never in the system instruction, so the AI knows which question the answer addresses.
      (3) The per-topic progress numerator counts ACTIVE questions only: questions_attempted is recomputed on soft delete or topic change
      and clamped with min(100, ...) on read. (4) ClassScopeQueryPort is dropped from F6; scope follows role Functions (F1-12), as F5
      already did on 2026-10-13.
    rationale: >
      (1) Ponytail rung 6 and consistency with the closed 2026-10-11 decision. (2) Rung 1: without the question the feature cannot give
      useful feedback. (3) Rung 6: a ratio above 100 percent is wrong data. (4) Rung 4: role Functions already exist, no new port needed.
    consequences: >
      03-dd/api/F6_interview-bank.md, 03-dd/logic/F6_interview-bank.md and 04-tdd/F6_interview-bank.md already follow these choices.
      Doc edits owed (phase 3): 02-bd/architecture/F6_interview-bank.md (sections 4.2, 8, port list), 02-bd/security/F6_interview-bank.md
      lines 104-107 (stale), 02-bd/database/F6_interview-bank.md lines 159-165 (progress numerator), .nexa/control/dependency-map.md.
  - id: DEC-2026-1023-dd-sync-audit-and-index
    kind: tech-choice
    status: ACTIVE
    date: 2026-10-23
    subject: "JOB-AI-03 auto-unlock writes a log line only (no audit row, no system user in identity); add index user_answers(question_id) to interview-bank"
    decision: >
      Owner choices of 2026-10-23 for the cross-module sync. (1) JOB-AI-03 (monthly budget rollover and auto-unlock) logs at INFO and
      writes no audit row. identity seeds no system user and gets no AI_BUDGET_AUTO_UNLOCKED audit action. Auditing stays human-actor only
      (AuditLogPort keeps actorUserId NOT NULL). (2) 02-bd/database/F6_interview-bank.md gains an index on user_answers(question_id),
      used by usageCount and the average score, which group by question and cannot use the existing user_id-first indexes.
    rationale: >
      (1) Ponytail rung 3: nobody reads this audit row in the demo, and a system user would force changes in F1 seed and the user
      management screen filter. (2) Rung 4/5: one DDL line removes a table scan on the question-grouped queries.
    consequences: >
      03-dd/jobs/F5_ai-review.md JOB-AI-03 reduced to a log line; 03-dd/api/F5_ai-review.md and 04-tdd/F5_ai-review.md references to the
      system actor removed. 02-bd/database/F6_interview-bank.md index list gets the new row. No change to F1.
  - id: DEC-2026-1024-dd-screens-users-core-contract-changes
    kind: api-contract
    status: ACTIVE
    date: 2026-10-24
    subject: "Screens USR0102/USR0201: Sample gets testcaseId (F2), problem gets withdrawn flag (F4), UUID problemId in every route, STOMP client library added"
    decision: >
      Owner choices of 2026-10-24 while writing the screen DD for USR0102 and USR0201. (1) The Sample testcase items returned by
      problem-bank (03-dd/api/F2_problem-bank.md section 8) gain testcaseId, so the client can join judge results by key instead of by
      position; samples are public, nothing hidden leaks. (2) The problem block of the judge-orchestration submission-result response
      (03-dd/api/F4_judge-orchestration.md section 8) gains withdrawn: boolean, so a withdrawn problem is told apart from a problem-bank
      error and the history label "Da rut khoi ngan hang" can show. (3) Every learner route that addresses a problem uses the UUID
      problemId, not a slug; BD USR0201 Sheet 3 is corrected. (4) @stomp/stompjs is added to the frontend as the single STOMP client
      in shared/realtime (F4-08 per-testcase push), replacing no existing dependency.
    rationale: >
      (1) Ponytail rung 1: without a key the sample expansion of USR0201 cannot be built reliably. (2) Rung 1/6: keeps the learner's
      history readable after a problem is withdrawn (02-bd/database/F2_problem-bank.md section 1.1a). (3) Rung 4: the APIs already take
      UUID, a slug would need a new column and endpoint the RD does not ask for. (4) Rung 1: F4-08 requires WebSocket push and the stack
      is locked to STOMP.
    consequences: >
      Doc edits: 03-dd/api/F2_problem-bank.md section 8, 03-dd/api/F4_judge-orchestration.md section 8, 04-tdd/F2 and F4 where affected,
      02-bd/screens/users/USR0201_submission_result.md Sheet 3 route. Code (later, not now): add dependency @stomp/stompjs to
      05-coding/frontend/package.json and build shared/realtime/stomp-client.ts.
  - id: DEC-2026-1024-dd-screens-ai-topic-length-and-rd-trim
    kind: product-scope
    status: ACTIVE
    date: 2026-10-24
    subject: "Screens USR0301/USR0302: F5 topic length raised to 60, unsupported RD blocks of the mock interview screen dropped, strengths/improvements removed from the result DTO, rate-limit wait state added"
    decision: >
      Owner choices of 2026-10-24 while writing the screen DD for USR0301 and USR0302. (1) topics[] of the F5 Mock Interview request
      accepts topic names up to 60 characters, matching the F6 topic name limit (was 50). (2) RD blocks of USR0302 with no data source
      (recent-sessions table, topic-thread and context columns, topics already covered, next-step suggestions) are not built; the RD is
      amended to match; recent sessions stay covered by USR0501 and USR0601 through F5-16. (3) strengths and improvements are removed from
      InterviewResultSummaryDto in the BD and from the prototype type; they have no RD requirement and no column. (4) Solution Review
      adds a rate-limit wait state with a countdown for AIR-402 and AIR-404, text and timer only, no new endpoint.
    rationale: >
      (1) Rung 1 and 5: a topic the admin can create must be selectable. (2) Rung 2: no real data source, never display invented values.
      (3) Rung 2. (4) Rung 1: F5-19 requires the learner to see why a request is blocked.
    consequences: >
      Docs: 03-dd/api/F5_ai-review.md section 8.2, 03-dd/validation/F5_ai-review.md Table 4 and AIR-V06, 04-tdd/F5_ai-review.md,
      01-rd/screens/users/USR0302_mock_interview.md, 02-bd/screens/users/USR0302_mock_interview.md (DTO), 03-dd/screens/users/USR0301
      and USR0302 sections 4, 5, 8. Code later: drop two fields from the InterviewResult type, add the wait state.
  - id: DEC-2026-1023-student-class-screens
    kind: product-scope
    status: ACTIVE
    date: 2026-10-23
    subject: "Student class screens: my_classes (USR0701) and my_class_detail (USR0702), header nav item, Monaco editor"
    decision: >
      Owner request of 2026-10-23: the learner area had no screen for a class the learner belongs to, and no UI to type an invite
      code. Add two screens: USR0701 my_classes at /classes (cards of the learner's current classes plus a "join by invite code" form)
      and USR0702 my_class_detail at /classes/{classId} (read-only: class info, assignment progress, assigned problems). "Lớp của tôi"
      is the 7th item of the top header nav, not an avatar-menu item. No new F-code: the screens extend F1-23 (join by code, now with a
      UI) and F2-12 (assigned problems, previously only a block inside problem_list, "không cần mở màn khác"). Joining an already-joined
      class is idempotent success (03-dd/logic/F1_identity.md BR-09 case b). Same day the problem_detail code area changed from a
      textarea to Monaco through the shared CodeEditor (README.md section 5 already locked Monaco); Monaco loads from the jsDelivr CDN.
    rationale: >
      Ponytail rung 1 (F1-23 says the learner joins by code, and without a UI the requirement is unreachable) and rung 4 (data already
      exists: class_enrollments, classes, class_assignments, user_problem_best_score; no new table, no new F-code). Rung 3 for what was
      left out: leaving a class by oneself, seeing classmates, a count badge in the header. Header placement is the owner's choice.
    consequences: >
      Docs: 01-rd/screens/users/USR0701_my_classes.md, USR0702_my_class_detail.md, 01-rd/overview/system_survey.md, 01-rd/req/user_stories/a1_student.md,
      02-bd/screens/users/USR0701_my_classes.md, USR0702_my_class_detail.md, 02-bd/_rules/bd-template-9sheet.md section 8 (37 screens),
      02-bd/screens/users/_shell.md (7 nav items), 02-bd/screens/users/USR0102_problem_detail.md (Monaco), 06-plan/PROTOTYPE_DEBT.md section 40.
      PROPOSED, not yet approved by the owner (BD USR0701 Q2): split the read API into ListMyClasses (identity) and ListMyClassProblems
      (problem-bank, optional classId), because identity must not depend on problem-bank (.nexa/control/dependency-map.md:100,126) and the existing
      ListMyClassAssignments caps at 3 groups and hides withdrawn problems. Missing and still to do: DD for ListMyClasses, GetMyClass and
      ListMyClassProblems in 03-dd/api/F1_identity.md and F2_problem-bank.md (JoinClassByCode already exists, row 25), 04-tdd. Code: views/users/my-classes, views/users/my-class-detail, entities/class (MyClass, useMyClasses, useMyClass, useJoinClass),
      shared/ui/data/code-editor.tsx, student-nav.ts, e2e student-shell nav count 6 to 7.
  - id: DEC-2026-1024-dd-student-language-contracts
    kind: architecture
    status: ACTIVE
    date: 2026-10-24
    subject: "Student screen DD: reuse the all-language limits contract, expose enabled submission languages, persist default language in identity settings"
    decision: >
      Owner confirmed three choices while reviewing Phase 5 DD on 2026-10-24. The requested single read of effective limits for all
      languages in USR0104 is already supported by F4 endpoint #1 when its optional language query is absent; reuse that contract,
      without adding a duplicate endpoint. F4 adds one authenticated read endpoint listing enabled submission languages for USR0503
      and other learner screens. F1 settings endpoints
      #14 GET and #15 PATCH include defaultLanguage, persisted in identity.users.default_language alongside user_preferences fields.
      The existing single-language execution-limit endpoint remains the source for the selected language in USR0102.
    rationale: >
      Rung 1: USR0104 BD requires the three-language comparison and USR0503 BD requires a dynamic enabled-language list. F4 #1
      already returns all three without a language filter, satisfying the owner's single-request preference. The new list endpoint
      reads the owning schema judge.language_configs and has no problemId parameter.
      Rung 2: identity.users.default_language already exists; omitting it from F1 #14-15 would make the Settings control impossible
      to persist even though the screen BD assigns that field to the account.
    consequences: >
      Update 03-dd/api/F4_judge-orchestration.md, 03-dd/validation/F4_judge-orchestration.md, 04-tdd/F4_judge-orchestration.md,
      03-dd/api/F1_identity.md, 03-dd/validation/F1_identity.md, 04-tdd/F1_identity.md, and DD screens USR0104/USR0503.
      Frontend adapters and mocks are updated when the real API integration begins.
  - id: DEC-2026-1024-dd-student-class-contracts
    kind: architecture
    status: ACTIVE
    date: 2026-10-24
    subject: "Student class DD: split F1 and F2 reads, retain withdrawn assignments in progress, rate-limit invite attempts with Redis"
    decision: >
      Owner confirmed the three open choices for USR0701 and USR0702. F1 owns ListMyClasses and GetMyClass;
      F2 owns ListMyClassProblems. A withdrawn problem remains visible and counts in the assignment total
      while its class assignment remains active; the screen disables opening that problem. Join-by-code attempts
      are limited per account with Redis to prevent invite-code guessing: five failed attempts in a rolling
      15-minute window. The next attempt is rejected until the window expires; a successful join clears the counter.
    rationale: >
      The split follows module ownership. Counting visible withdrawn assignments keeps the progress denominator
      consistent with the detail table. The Redis guard protects eight-character invite codes.
    consequences: >
      Add F1 and F2 read contracts, join-rate validation and module acceptance criteria, then write DD screens
      USR0701 and USR0702. This resolves the corresponding questions in their BD documents and supersedes
      the unapproved split note in DEC-2026-1023-student-class-screens.
  - id: DEC-2026-1024-dd-teacher-f1-read-contracts
    kind: architecture
    status: ACTIVE
    date: 2026-10-24
    subject: "Phase 6 teacher screens add three identity read endpoints for class summaries, completed-week score trend and attention students"
    decision: >
      Owner chose to add three F1 read endpoints for the instructor screens. A class summary endpoint serves both
      INS0101 and INS0201 with per-class cards and aggregate figures. A score-trend endpoint returns one six-point
      series over completed ISO weeks for the selected class or all owned classes. An attention endpoint returns at
      most five students in the selected scope using the existing absence/completion/decline labels. Identity owns
      the HTTP contracts and reads F2/F4 only through ports; the browser does not query foreign schemas.
    rationale: >
      The three business operations appear in teacher BD but the F1 API catalog did not expose them. Explicit
      contracts let Phase 6 screens map every displayed value to a source and keep module boundaries intact.
    consequences: >
      Add F1 endpoints #56-58 and matching validation and TDD, then connect INS0101, INS0201 and INS0203 DD.
  - id: DEC-2026-1024-dd-teacher-student-list
    kind: architecture
    status: ACTIVE
    date: 2026-10-24
    subject: "Teacher class/student tables use one F1 paginated list across all owned classes"
    decision: >
      Owner approved a new F1 read endpoint for the instructor's students across all owned classes, with optional
      classId filter, server-side search and pagination. INS0201 and INS0203 use it for their All-classes tab;
      existing F1 #26 remains the per-class read contract for INS0205.
    rationale: >
      Fetching each class separately and paginating in the browser cannot produce a correct global page or total.
    consequences: >
      Add F1 endpoint #59, validation and acceptance criteria; update the two screen DD documents.
  - id: DEC-2026-1024-dd-student-dashboard-and-interview-navigation
    kind: architecture
    status: ACTIVE
    date: 2026-10-24
    subject: "Student dashboard gets F2 difficulty progress; mock interview resumes by sessionId and pins selected Accepted submission"
    decision: >
      Owner approved an F2 read endpoint that computes solved/total by problem level for published, non-deleted
      problems using SolveStatePort. Reopening a saved mock-interview session uses /mock-interview?sessionId={id}
      in read-only mode and GetSessionDetail. When entry from an Accepted submission refers to an item outside
      the first ten, the screen calls F4 #5 to verify ownership and pins that submission above the shortlist.
    rationale: >
      The difficulty block needs a real source that excludes withdrawn problems. The two navigation rules preserve
      the selected interview context without guessing from a truncated list.
    consequences: >
      Add F2 endpoint and TDD, update USR0601 and USR0302 DD/TDD. No new F6 shortlist endpoint is required;
      F6 GET /questions already supplies the interview-bank tab.
  - id: DEC-2026-1024-dd-f2-topic-score-source
    kind: architecture
    status: ACTIVE
    date: 2026-10-24
    subject: "F2 computes topic skill scores once for dashboard and suggested problems"
    decision: >
      Owner chose F2 as the one server-side source of topic skill scores. F2 computes the 70/30 score from
      published-problem solve progress and all-time submission acceptance rate, returns the scores with
      suggested problems, and uses the same score vector to rank those problems. No LearnerTopicRankingPort
      is added to F1.
    rationale: >
      F2 owns topics and published-problem scope. One computation prevents the dashboard and suggestions
      from disagreeing and avoids a circular F1-to-F2 read dependency.
    consequences: >
      Update F2 suggested-problems API and logic, add F4 P5 with topic-to-problem input to avoid a circular
      F2-to-F4-to-F2 call, and update USR0601 DD. Defer TDD.
  - id: DEC-2026-1024-dd-saved-note-explicit-save
    kind: product
    status: ACTIVE
    date: 2026-10-24
    subject: "Saved-problem notes use explicit Save and Cancel"
    decision: >
      Owner chose Save/Cancel buttons for saved-problem notes, with Ctrl/Cmd+Enter to save and Escape to cancel.
      Losing focus does not save, so clicking Cancel cannot accidentally persist the draft.
    rationale: >
      The old blur-save behavior conflicts with a real Cancel action and risks saving text the learner rejected.
    consequences: >
      Treat USR0103 DD as authoritative for this interaction and align BD when the BD synchronization pass runs.
      Defer TDD.
  - id: DEC-2026-1024-dd-my-submissions-page-and-badge
    kind: product
    status: ACTIVE
    date: 2026-10-24
    subject: "My Submissions uses 20 rows and neutral difficulty badges"
    decision: >
      Owner chose the existing F4 page size of 20 without a page-size selector and a neutral difficulty badge
      using levelDisplayName only. F4 need not expose levelCode for this screen.
    rationale: >
      These choices keep the screen aligned with the current list contract and avoid color semantics based on
      only the original three difficulty codes.
    consequences: >
      Close USR0202 open questions 1 and 2; align BD/prototype when those phases resume. Defer TDD.
  - id: DEC-2026-1025-dd-workspace-quick-insight-trim
    kind: product
    status: ACTIVE
    date: 2026-10-25
    subject: "Workspace Quick Insight omits short review comments"
    decision: >
      Owner chose to remove the one-to-two short review comments from the Workspace RD. The tab shows
      the two complexity values and a link to the full Solution Review; F5 Quick Insight does not gain
      a highlights field.
    rationale: >
      The existing review JSON and Quick Insight read contract have no dedicated highlights value.
    consequences: >
      Update RD USR0102 and close its DD question 7. Align the older BD/prototype when those phases resume.
      Defer TDD.
  - id: DEC-2026-1025-dd-interview-bookmark-url
    kind: product
    status: ACTIVE
    date: 2026-10-25
    subject: "Preserve the bookmarked-only interview filter in the URL"
    decision: >
      When the bookmarked-only switch is on, USR0401 writes bookmarked=1 to the URL. Links to USR0402
      carry it with topic, q, status and level; the return link carries the same normalized filters back.
    rationale: >
      A URL-backed filter survives detail navigation, reloads and opening a question in a new tab.
    consequences: >
      Close USR0401 OQ-1 and align USR0402 navigation. Sync the older BD screens later; defer TDD.
  - id: DEC-2026-1025-dd-email-resend-budget
    kind: product
    status: ACTIVE
    date: 2026-10-25
    subject: "Email-change resend counts toward the daily request limit"
    decision: >
      A resend of the email-change OTP calls F1 #11 and consumes one of the three requests allowed
      per account in 24 hours. The separate 60-second resend cooldown remains in force.
    rationale: >
      A resend sends another email and must share the same account-level abuse limit.
    consequences: >
      Close USR0502 OQ-2 and align F1 API and validation. Defer TDD.
  - id: DEC-2026-1025-dd-auth-recovery-contracts
    kind: product
    status: ACTIVE
    date: 2026-10-25
    subject: "Verify password-reset OTP before reset and restore self-deactivated accounts"
    decision: >
      F1 adds #60 POST /auth/password/verify-otp; a valid check does not consume the code, and #8
      verifies and consumes it on reset. F1 adds #61 POST /auth/deactivation/restore for a self-deactivated
      account within the 30-day grace period, using a one-use ticket issued only after credential or OAuth
      verification. Restore does not log the user in automatically.
    rationale: >
      The first endpoint preserves the separate OTP step in the approved screen flow. The second supplies
      the already-approved self-cancel action without allowing an unauthenticated account unlock.
    consequences: >
      Update F1 API, logic, validation, catalog and SHR0101 DD. Retain ADMIN lock separation and defer TDD.
  - id: DEC-2026-1025-dd-admin-language-display-and-stack
    kind: product
    status: ACTIVE
    date: 2026-10-25
    subject: "Read compiler display names from F4 and omit unsupported Stack field"
    decision: >
      F4 language-config GET and PUT responses include compilerDisplayName from backend configuration as
      a read-only field. SHR0202 and SHR0203 omit Stack (MB); no stack_limit_mb column or API field is added.
    rationale: >
      The compiler label must reflect backend configuration. Stack has no durable source in the current
      problem-bank schema or API.
    consequences: >
      Close ADM0501 OQ-3 and SHR0203 OQ-1, align prototype and older BD in their later passes, defer TDD.
  - id: DEC-2026-1025-dd-admin-dashboard-ten-reads
    kind: product
    status: ACTIVE
    date: 2026-10-25
    subject: "Admin dashboard has one read endpoint per block"
    decision: >
      ADM0101 uses ten separate F1 HTTP reads, one for each BD data block. F1 #41-43 are narrowed
      to one block each and #67-73 supply the remaining seven. The retry action reloads only the
      failed block.
    rationale: >
      The approved BD requires independent loading, failure and retry for each of ten blocks.
      Three grouped responses would couple unrelated blocks and make a local retry ambiguous.
    consequences: >
      Update F1 API, BR-17, the API catalog and ADM0101 screen DD. Defer TDD.
  - id: DEC-2026-1025-dd-audit-user-actions-category
    kind: product
    status: ACTIVE
    date: 2026-10-25
    subject: "Classify user administration actions under permission matrix in audit log"
    decision: >
      The ADM0403 audit log keeps exactly four groups: Authentication, Permission Matrix,
      Configuration and Content. User creation, locking, unlocking and role changes belong
      to Permission Matrix; no fifth User group is added.
    rationale: >
      The owner chose the existing Permission Matrix tab so user administration remains
      visible without changing the approved four-tab layout.
    consequences: >
      Align F1 BR-19, audit filtering and breakdown responses, and ADM0403 DD. Defer TDD.
  - id: DEC-2026-1025-dd-restore-relogin
    kind: product
    status: ACTIVE
    date: 2026-10-25
    subject: "Restored self-deactivated account signs in again"
    decision: >
      After F1 #61 restores a self-deactivated account, SHR0101 shows success and requires
      a fresh sign-in. The restore response carries no access or refresh token.
    rationale: >
      The owner chose the explicit sign-in path already described by BD SHR0101 EVT-8.
    consequences: >
      Close SHR0101 OQ-6; retain F1 #61 response and defer TDD.
  - id: DEC-2026-1025-dd-problem-readonly-and-checklist
    kind: product
    status: ACTIVE
    date: 2026-10-25
    subject: "Hide reference solution from other instructors and preserve published testcase minimums"
    decision: >
      F2 #34 redacts the reference solution when an instructor reads another author's published
      problem. F2 #43-44 reject a change that would leave a published problem with fewer than
      eight approved testcases or two approved Sample cases, using PRB-054.
    rationale: >
      The owner chose to protect the reference code and to preserve the same minimums after
      publication that the publish checklist requires initially.
    consequences: >
      Update F2 API and logic, SHR0202 and SHR0203 DD. Defer TDD.
  - id: DEC-2026-1025-dd-queue-publish-failure-final
    kind: product
    status: ACTIVE
    date: 2026-10-25
    subject: "Failed submission queue publish ends the submission immediately"
    decision: >
      After three RabbitMQ publish attempts without confirmation, a still-PENDING submission
      becomes SYSTEM_ERROR with failure_reason QUEUE_UNAVAILABLE immediately. If a worker
      already advanced the row, the API returns its ticket and does not overwrite it.
    rationale: >
      The owner chose an immediate terminal result instead of leaving a known failed
      submission pending until the five-minute sweep.
    consequences: >
      Update F4 API and logic and the F4 BD database enum; keep sweep for stuck workers.
      Defer TDD.
  - id: DEC-2026-1025-dd-f2-generation-and-event-ledger
    kind: design
    status: ACTIVE
    date: 2026-10-25
    subject: "Persist testcase generation provenance and deduplicate graded events"
    decision: >
      Add problem.testcase_generation_runs for successful AI testcase generation provenance
      and problem.consumed_events keyed by eventId for SubmissionGraded processing.
      Insert the deduplication key and update problem_stats in one transaction.
    rationale: >
      RD F2-14 requires retaining the script and seed. Judge emits graded events at least
      once, so counters need a durable deduplication key, including during replay.
    consequences: >
      Update F2 BD database and F2 DD API; retain deduplication keys until an explicit
      replay retention policy is defined. Defer TDD.
  - id: DEC-2026-1025-dd-testcase-visibility-at-grading
    kind: design
    status: ACTIVE
    date: 2026-10-25
    subject: "Gate historical sample output by visibility at grading and at reading"
    decision: >
      Store is_sample on each judge submission testcase result. Expose sample-only fields
      only when the saved flag and current testcase visibility are both Sample.
    rationale: >
      Reading current visibility alone could reveal output from a testcase that was Hidden
      when graded. Treat missing or deleted current testcase as Hidden.
    consequences: >
      Update F4 BD database, API and logic. Defer TDD.
  - id: DEC-2026-1025-dd-submission-testcase-snapshot
    kind: product
    status: ACTIVE
    date: 2026-10-25
    subject: "Grade queued submissions with the testcase bundle current when submitted"
    decision: >
      F2 publishes an immutable bundle for each graded testcase-set version. F4 pins the
      version when creating a PENDING submission; its worker reads only that version.
      Version bumps cover approved Sample and Hidden testcase changes.
    rationale: >
      The owner requires edits made while a submission is queued not to alter its result.
    consequences: >
      Add F2 testcase_set_snapshots and update F2/F4 database, API and logic documents.
      Defer TDD.
  - id: DEC-2026-1025-dd-harness-internal-system-error
    kind: product
    status: ACTIVE
    date: 2026-10-25
    subject: "Treat generated harness compilation failures as system errors"
    decision: >
      A HARNESS_INTERNAL compilation report ends a submission with SYSTEM_ERROR and
      SANDBOX_FAULT, with no compile_error_message or contribution to AC-rate metrics.
    rationale: >
      The owner chose to keep system-generated code failures separate from learner errors.
    consequences: >
      Update F4 logic and API. Defer TDD.
  - id: DEC-2026-1025-dd-class-metrics-after-assignment
    kind: product
    status: ACTIVE
    date: 2026-10-25
    subject: "Count class submissions only after the problem was assigned"
    decision: >
      Instructor submission lists, detail and class metrics require assignedAt no later
      than submittedAt, and use the same enrollment window rules.
    rationale: >
      The owner chose one scope rule for all instructor screens and metrics.
    consequences: >
      Update F4 class query contracts and metrics port inputs. Defer TDD.
  - id: DEC-2026-1025-dd-system-error-ac-denominator
    kind: product
    status: ACTIVE
    date: 2026-10-25
    subject: "Exclude system errors from AC-rate denominators"
    decision: >
      Keep SYSTEM_ERROR in submission totals and activity, but exclude it from AC-rate
      denominators in F1 and F4. Count eligible final submissions separately in F1.
    rationale: >
      The owner ruled that a generated harness failure is a system error and must not
      reduce the learner's AC rate. The same rule applies to all system failures.
    consequences: >
      Add ac_eligible_count to the F1 read model and align personal and class metrics.
      Defer TDD.
  - id: DEC-2026-1025-dd-class-assignment-time-window
    kind: product
    status: ACTIVE
    date: 2026-10-25
    subject: "Class progress uses submissions within each assignment period"
    decision: >
      Class submissions count only from assignedAt until removedAt, if present.
      A problem is completed for a class only by an Accepted submission after the
      active assignment began. Earlier global best scores do not complete it.
    rationale: >
      The owner chose to exclude self-practice before assignment and after removal
      from class progress while keeping historical submissions made during assignment.
    consequences: >
      F4 filters by the assignment interval and returns active assignment results;
      F1 computes class completion from those results. Defer TDD.
  - id: DEC-2026-1025-dd-function-spec-evaluator-common
    kind: architecture
    status: ACTIVE
    date: 2026-10-25
    subject: "Share one function signature evaluator across F2 and F3"
    decision: >
      Put the pure FunctionSpecEvaluator and its type limits in algoprep-common.
      Both problem-bank and harness use it; code-generation logic stays in harness.
    rationale: >
      The owner chose one source for signature rules to prevent the two modules
      accepting different function specifications.
    consequences: >
      Keep the F3 P5 contract in the shared kernel and close F3 Q1. Defer TDD.
  - id: DEC-2026-1025-dd-ai-testcase-limit-audit
    kind: product
    status: ACTIVE
    date: 2026-10-25
    subject: "Audit changes to the AI testcase generation limit"
    decision: >
      An admin change to max_ai_generations_per_problem records
      AI_TESTCASE_GENERATION_LIMIT_CHANGED in the F1 system audit log, in the same
      transaction as the F2 setting update. Keep updated_by as well.
    rationale: >
      The owner chose a durable system log for this admin limit change, consistent
      with the audit of AI token budget changes.
    consequences: >
      Add the F2 AuditLogPort call and F1 CONFIG action mapping. Defer TDD.
  - id: DEC-2026-1025-dd-starter-code-unpublished-access
    kind: security
    status: ACTIVE
    date: 2026-10-25
    subject: "Restrict starter code for unpublished problems"
    decision: >
      An authenticated user may fetch starter code for a published problem.
      For an unpublished problem, only its author or an admin may do so.
      F3 checks current F2 metadata before reading the possibly cached spec;
      outside scope returns the same 404 as a missing problem.
    rationale: >
      The owner chose to keep unpublished function signatures within the
      author/admin scope and avoid stale permission decisions in the spec cache.
    consequences: >
      Add StarterCodeAccessPort and update F3 P1 security rules. Defer TDD.
  - id: DEC-2026-1025-dd-function-signature-preview
    kind: product
    status: ACTIVE
    date: 2026-10-25
    subject: "Preview function declarations through F3"
    decision: >
      The problem authoring screen calls an authenticated F3 endpoint with the unsaved
      function spec draft. F3 returns declarations for Java, C++ and Python from the
      same naming and type rules used to generate starter code.
    rationale: >
      The owner chose one server-side rule so the preview cannot drift from the
      generated starter code when language naming rules change.
    consequences: >
      Add F3 P6 and its HTTP wrapper; update SHR0202 consumption. Defer TDD.
  - id: DEC-2026-1025-dd-first-failed-testcase-verdict
    kind: product
    status: ACTIVE
    date: 2026-10-25
    subject: "Use the first failed testcase for aggregate verdict"
    decision: >
      Run every testcase to calculate partial score. Derive the submission's final
      non-Accepted status from the first failed testcase by testcase_order.
    rationale: >
      The owner selected the existing DD rule over severity ordering to make the
      reported status deterministic and tied to visible testcase order.
    consequences: >
      Close F4 BR-JD-04's pending owner note. Defer TDD.
  - id: DEC-2026-1025-dd-drop-worker-detail
    kind: product
    status: ACTIVE
    date: 2026-10-25
    subject: "Remove unsupported per-worker queue monitor detail"
    decision: >
      Remove the worker detail block and WORKER_OVERLOADED status from ADM0401.
      Keep the autoscale control and queue metrics backed by F4 data.
    rationale: >
      The owner approved the change after checking go-judge's official metrics:
      they are aggregate and do not provide per-worker IDs, status or load.
    consequences: >
      F4 queue snapshot no longer depends on Prometheus or returns a worker list.
      Override the earlier assumption in the 2026-10-10 admin operations decision.
  - id: DEC-2026-1025-dd-go-judge-adapter-contract
    kind: architecture
    status: ACTIVE
    date: 2026-10-25
    subject: "Define the F3/F4 go-judge request boundary"
    decision: >
      F3 provides compile/run command templates without testcase input or limits.
      F4's adapter adds go-judge cmd, stdin, limits and cached artifact IDs. Compile
      and all runs of a submission use one go-judge instance; the adapter deletes
      cached IDs after completion. Java caches a JAR, C++ a binary, Python its source.
    rationale: >
      The official go-judge API uses cmd arrays, copyIn and copyOutCached/fileIds;
      cached files belong to an instance and require cleanup. This closes the
      cross-module wire contract left open in F3 Q4.
    consequences: >
      Align F3 API/logic and F4 API. Verify image paths and toolchain during coding;
      defer TDD as requested.
  - id: DEC-2026-1025-dd-unordered-multiset
    kind: product
    status: ACTIVE
    date: 2026-10-25
    subject: "Preserve duplicates in unordered output matching"
    decision: >
      UNORDERED_SET compares a multiset of canonical top-level elements. Element
      order is ignored, but the occurrence count of each element must match.
    rationale: >
      The owner chose that [1,1,2] and [1,2] are different; dropping duplicates
      would accept an output that omitted required values.
    consequences: >
      Update F3 matcher and BD wording. Defer TDD.
  - id: DEC-2026-1025-dd-wrapper-runtime-stderr-hidden
    kind: security
    status: ACTIVE
    date: 2026-10-25
    subject: "Hide runtime stderr for function-wrapper submissions"
    decision: >
      F4 never stores or returns stderr for FUNCTION_WRAPPER. For STANDARD_IO it
      stores and returns stderr only for Sample testcases, non-AC verdicts and when
      the admin setting permits it.
    rationale: >
      The owner chose the simple safe rule because runtime stack traces can contain
      generated harness internals and Hidden input.
    consequences: >
      Close F3 runtime-error question without a new mapping port; update F4 DB,
      API, logic and validation. Defer TDD.
  - id: DEC-2026-1025-dd-multidimensional-array-header
    kind: product
    status: ACTIVE
    date: 2026-10-25
    subject: "Use explicit dimensions in multidimensional testcase input"
    decision: >
      The first line of multidimensional array input contains n and m; the parser
      then requires n rows with m elements each.
    rationale: >
      The owner chose the explicit format to avoid inferring dimensions from data
      rows or accepting ragged arrays accidentally.
    consequences: >
      Close F3 Q8 and align problem-bank testcase examples. Defer TDD.
  - id: DEC-2026-1025-dd-worker-consumer-autoscaler
    kind: architecture
    status: ACTIVE
    date: 2026-10-25
    subject: "Make queue autoscale control operational"
    decision: >
      A dedicated F4 worker process runs JOB-JD-05 and dynamically adjusts its
      RabbitMQ grading listener from one to eight consumers based on pending jobs
      and the stored autoscale switch and threshold. It does not scale Docker
      containers. A Postgres session advisory lock keeps one listener leader.
    rationale: >
      The owner kept the autoscale switch and required a real controller. Spring
      AMQP supports dynamic listener concurrency without Docker daemon access.
    consequences: >
      Specify the controller, pause interaction, deployment profile and operations
      in F4 DD. Defer TDD and code.
  - id: DEC-2026-1025-dd-run-sample-rate-limit
    kind: architecture
    status: ACTIVE
    date: 2026-10-25
    subject: "Limit Run Code calls per user with Redis"
    decision: >
      Owner confirmed on 2026-10-26: allow 60 admitted Run Code calls per
      sliding 60 minutes per user, backed by Redis, alongside eight concurrent calls.
      Refund infra failures; Redis outage falls back to the concurrency cap.
    rationale: >
      Run Code shares judge capacity; a concurrency cap alone lets one account
      repeatedly occupy all slots. The owner chose 60 per rolling hour.
    consequences: >
      Add JDG-107 and the rate-limit flow to F4. Defer TDD.
  - id: DEC-2026-1025-dd-spec-unavailable-reason
    kind: architecture
    status: ACTIVE
    date: 2026-10-25
    subject: "Distinguish unusable problem data from sandbox faults"
    decision: >
      Owner confirmed: SYSTEM_ERROR uses SPEC_UNAVAILABLE
      when the grading spec or testcase bundle is missing, corrupt or unparseable.
      SANDBOX_FAULT remains for infrastructure and generated harness failures.
    rationale: >
      The two causes need different operational diagnosis and neither is a learner
      error. The owner confirmed this separation on 2026-10-26.
    consequences: >
      Extend F4 failure_reason and worker mapping. Defer TDD.
  - id: DEC-2026-1025-dd-f2-event-outbox
    kind: architecture
    status: ACTIVE
    date: 2026-10-25
    subject: "Persist F2 domain events in an outbox"
    decision: >
      Owner confirmed: write F2 domain events in
      problem.outbox_events in the business transaction and relay them with publisher
      confirms. Keep unpublished rows until delivered.
    rationale: >
      After-commit best-effort publishing can lose class assignment activity or
      cache invalidation when RabbitMQ is unavailable. The owner confirmed the
      durable outbox on 2026-10-26.
    consequences: >
      Add F2 outbox schema and JOB-PB-01/02. Defer TDD.
  - id: DEC-2026-1026-testcase-position-snapshot
    kind: architecture
    status: ACTIVE
    date: 2026-10-26
    subject: "One testcase position order; published reorder creates a new snapshot"
    decision: >
      The owner confirmed one position order for testcase display and grading.
      A real reorder on a PUBLISHED problem creates an immutable snapshot and
      increments current_testcase_set_version once, including when an unapproved
      draft is moved. Unpublished reorder and no-op retry do not increment it.
      Existing submissions use their pinned old snapshot; new submissions use
      the new snapshot.
    rationale: >
      F4 derives testcase_order and the first failing verdict from snapshot
      position. Changing current position without a new snapshot would make
      the authoring order disagree with the order used for new submissions.
      The owner selected the single-order rule on 2026-10-26.
    consequences: >
      Update F2 API 45, BR-11, DB and authoring-screen contracts, plus TDD
      AC-56/AC-60 and position test cases.
  - id: DEC-2026-1027-qa-f3-preview-body-limit
    kind: api-contract
    status: ACTIVE
    date: 2026-10-27
    subject: "F3 signature preview rejects body above 16 KiB with 413 HRN-062"
    decision: >
      Reject a JSON UTF-8 preview body from 16,385 bytes with HTTP 413 and
      HRN-062 before parsing or invoking P5.
    rationale: >
      A distinct size error gives QA and the client an observable boundary
      separate from invalid function specifications.
    consequences: >
      Update F3 API, validation, error registry, AC-49 and TC-F3-AC49-08.
  - id: DEC-2026-1027-qa-f2-assignment-outbox-failure
    kind: api-contract
    status: ACTIVE
    date: 2026-10-27
    subject: "F2 assignment outbox failure rolls back one item and reports PRB-056"
    decision: >
      Each newly assigned problem and its outbox record commit in one transaction.
      An outbox write failure rolls back only that problem. The batch returns HTTP
      200 with PRB-056 in failedItems while other problems continue.
    rationale: >
      The assignment batch already treats each problem independently. This keeps
      the event and assignment atomic without discarding successful items.
    consequences: >
      Update F2 API 53, BR-18, error registry, instructor screen behavior and
      AC-75/90 test cases.
  - id: DEC-2026-1027-qa-f3-language-boundary
    kind: api-contract
    status: ACTIVE
    date: 2026-10-27
    subject: "F3 rejects unsupported language at raw adapter boundary"
    decision: >
      Keep P2 and P3 inputs typed as the three-value Language enum. Adapters
      reject unknown raw values with HRN-010 before enum conversion or port calls.
    rationale: >
      A fourth value cannot reach a Java method whose parameter is the closed enum.
      The adapter is the observable rejection point.
    consequences: >
      Update F3 API and validation boundary, AC-45 and its adapter test cases.
  - id: DEC-2026-1027-qa-f3-missing-signature-priority
    kind: api-contract
    status: ACTIVE
    date: 2026-10-27
    subject: "F3 checks missing wrapper signature before support flag"
    decision: >
      P1 and P2 return HRN-005 for an absent function signature even when
      functionWrapperSupported is false. A complete but unsupported signature
      returns HRN-004.
    rationale: >
      F2 permits a draft with three null signature fields and a false support flag.
      The prior check order made the documented HRN-005 branch unreachable.
    consequences: >
      Update F3 P1/P2 flow, validation and AC-05 test oracle.
  - id: DEC-2026-1027-qa-f3-cpp-user-note
    kind: api-contract
    status: ACTIVE
    date: 2026-10-27
    subject: "F3 returns an eligible C++ user note as its own diagnostic"
    decision: >
      A C++ compiler note linked to a USER error and located in user source
      becomes a separate diagnostics item with mapped line and message prefixed
      by note:. Notes in harness source are discarded.
    rationale: >
      A separate item preserves the note's own line without adding a DTO field.
      The main error still determines USER_ERRORS classification.
    consequences: >
      Update F3 CompileErrorReport, mapping rules and AC-22 test oracle.
  - id: DEC-2026-1027-qa-f4-all-week-label
    kind: api-contract
    status: ACTIVE
    date: 2026-10-27
    subject: "F4 all-range weekly count uses Monday as the day label"
    decision: >
      For range=all, dailyCounts.day is the Monday at the start of each ISO week
      in UTC+7, including the current incomplete week.
    rationale: >
      Monday identifies the ISO week consistently and resolves the conflict
      between F4 API response text and BR-JD-13.
    consequences: >
      Update F4 API 12.3, AC-39 and its weekly metrics test case.
  - id: DEC-2026-1027-qa-f1-self-impact-ack
    kind: api-contract
    status: ACTIVE
    date: 2026-10-27
    subject: "F1 self-lock and self-demotion require explicit server acknowledgement"
    decision: >
      For API #36/#37, an ADMIN changing their own role out of ADMIN or locking
      their own account must send acknowledgeSelfImpact=true after UI warning.
      Missing or false is rejected without writes. The last-active-ADMIN
      invariant remains a hard block regardless of acknowledgement.
    rationale: >
      The project owner confirmed server-side acknowledgement on 2026-10-27,
      resolving the proposed field name and the AC-29/BR-15 mismatch.
    consequences: >
      Update BD ADM0201, DD F1 API/logic/validation and F1 acceptance cases.
  - id: DEC-2026-1027-qa-f1-attention-severity
    kind: api-contract
    status: ACTIVE
    date: 2026-10-27
    subject: "F1 teacher attention list ranks severity within each status"
    decision: >
      For API #58, rank ABSENT by absent days descending, NEEDS_SUPPORT by
      greatest Accepted-rate decline first, and WATCH by lowest completion
      percentage first. Break remaining ties by studentId ascending; return
      at most five students after sorting.
    rationale: >
      The project owner confirmed these severity rules on 2026-10-27.
    consequences: >
      Update F1 API #58, TDD AC-53 and its ranking testcase.
  - id: DEC-2026-1027-qa-f1-class-leave-event-retry
    kind: api-contract
    status: ACTIVE
    date: 2026-10-27
    subject: "F1 class leave retry republishes the event using original occurredAt"
    decision: >
      Repeating the idempotent class-leave endpoint after publish failure keeps
      left_at unchanged and republishes StudentRemovedFromClass with occurredAt
      equal to the original left_at. Consumers deduplicate by classId, studentId,
      and occurredAt.
    rationale: >
      The project owner confirmed endpoint-based manual retry on 2026-10-27.
    consequences: >
      Update F1 API #27, BR-11, AC-21 and its recovery testcase.
  - id: DEC-2026-1027-qa-f6-history-total-attempts
    kind: api-contract
    status: ACTIVE
    date: 2026-10-27
    subject: "F6 totalAttempts counts all learner attempts independent of cursor"
    decision: >
      On GET question practice history, totalAttempts counts every attempt
      belonging to the current learner and question. The before cursor only
      filters items and hasMore; a new submission can increase the total.
    rationale: >
      The project owner resolved the ambiguous COUNT(*) base condition on
      2026-10-27.
    consequences: >
      Update F6 API section 7.5, AC-21 and its cursor testcase.
  - id: DEC-2026-1027-qa-f1-forgot-cooldown-response
    kind: api-contract
    status: ACTIVE
    date: 2026-10-27
    subject: "F1 forgot-password cooldown returns the same generic 200 response"
    decision: >
      A request within the 60-second per-user cooldown returns the same 200
      message as a normal or unknown-email request, without sending another
      email, changing the OTP, or extending its TTL.
    rationale: >
      The project owner confirmed the generic response on 2026-10-27.
    consequences: >
      Update F1 API #7, BR-04, AC-08 and its cooldown testcase.
  - id: DEC-2026-1027-qa-f1-wrong-current-password
    kind: api-contract
    status: ACTIVE
    date: 2026-10-27
    subject: "F1 wrong current password has a dedicated error code"
    decision: >
      POST /me/password with a wrong currentPassword returns 422 IDT-115.
      It does not change the password or sessions and does not emit success
      mail or audit entries.
    rationale: >
      The project owner selected a dedicated code on 2026-10-27; IDT-115 is
      the next unused F1 authentication error code.
    consequences: >
      Update F1 API #13, BR-06, validation, AC-14 and its negative testcase.
  - id: DEC-2026-1027-qa-f6-csv-physical-line
    kind: api-contract
    status: ACTIVE
    date: 2026-10-27
    subject: "F6 CSV import reports the physical start line of each record"
    decision: >
      ImportRow.line and skippedRows.line are the physical file line on which
      the CSV record begins, counting the header as line one. Embedded
      newlines inside quoted cells do not change that start line.
    rationale: >
      The project owner resolved the CSV line-number oracle on 2026-10-27.
    consequences: >
      Update F6 API section 24, BR-IB-11, AC-36 and parser testcase.
  - id: DEC-2026-1027-qa-f6-average-half-up
    kind: api-contract
    status: ACTIVE
    date: 2026-10-27
    subject: "F6 admin average score rounds one decimal half-up"
    decision: >
      averageScorePercent rounds the arithmetic mean of completed attempt
      scores to one decimal using half-up; 78.25 becomes 78.3.
    rationale: >
      The project owner resolved the tie-rounding oracle on 2026-10-27.
    consequences: >
      Update F6 BR-IB-10, AC-31 and the boundary testcase.
  - id: DEC-2026-1027-qa-f6-audit-failure
    kind: api-contract
    status: ACTIVE
    date: 2026-10-27
    subject: "F6 administration returns 503 INB-503 when AuditLogPort fails"
    decision: >
      When AuditLogPort cannot write during an F6 administration mutation,
      roll back the entire business transaction and return 503 INB-503.
      No business or audit row may be partially committed.
    rationale: >
      The project owner resolved the audit dependency error oracle on
      2026-10-27.
    consequences: >
      Update F6 API and logic contracts, AC-37, and the audit failure cases.
  - id: DEC-2026-1028-qa-f6-technical-limits
    kind: api-contract
    status: ACTIVE
    date: 2026-10-28
    subject: "F6 bulk delete and administration body-size limits"
    decision: >
      Bulk question deletion accepts at most 100 IDs. Create/update question
      request bodies have a 512 KiB limit. CSV import request bodies have a
      5 MiB limit (5120 KiB). Keep the DD error codes for violations.
    rationale: >
      The project owner confirmed all three previously inferred technical
      limits on 2026-10-28.
    consequences: >
      Promote the F6 DD limits to confirmed contracts and review boundary
      testcases at and above each limit.
  - id: DEC-2026-1028-qa-f1-missing-permission
    kind: api-contract
    status: ACTIVE
    date: 2026-10-28
    subject: "F1 missing Function:Action permission returns 403 IDT-445"
    decision: >
      An authenticated request to an F1 endpoint guarded by Function:Action
      that lacks the required permission returns 403 IDT-445 without invoking
      the protected operation. This applies after permission cache invalidation.
    rationale: >
      The project owner resolved the F1 RBAC denial response oracle on
      2026-10-28.
    consequences: >
      Update F1 API and BR-13, AC-25, and its permission-cache testcases.
  - id: DEC-2026-1028-qa-f1-role-delete
    kind: api-contract
    status: ACTIVE
    date: 2026-10-28
    subject: "F1 role deletion rejection codes and successful audit"
    decision: >
      DELETE role rejects system roles with 422 IDT-421 and roles still assigned
      to users with 422 IDT-422. Deleting an unassigned custom role returns
      204 without a body and writes one ROLE_DELETED audit row in the same
      transaction as the role and permission deletion.
    rationale: >
      The project owner resolved both role deletion oracles on 2026-10-28.
    consequences: >
      Update F1 API #34, BR-14, AC-26/27 and role deletion testcases.
  - id: DEC-2026-1028-qa-f1-role-delete-audit-failure
    kind: api-contract
    status: ACTIVE
    date: 2026-10-28
    subject: "F1 role deletion audit failure returns 503 IDT-446"
    decision: >
      When writing ROLE_DELETED audit fails, roll back the role and permission
      deletion and return 503 IDT-446 without a partial success.
    rationale: >
      The project owner resolved the role deletion recovery oracle on
      2026-10-28 during the sample QA audit.
    consequences: >
      Update F1 API #34, BR-14 and add an AC-27 recovery testcase.
  - id: DEC-2026-1028-qa-f1-permission-immediate
    kind: architecture-contract
    status: ACTIVE
    date: 2026-10-28
    subject: "F1 permission changes take effect immediately across instances"
    decision: >
      Preserve the AC-25 guarantee that any request starting after a successful
      permission PATCH sees the committed grant. Store a monotonic permission
      version on the role; an authorization guard reads it synchronously from
      primary PostgreSQL before trusting any local or Redis permission cache.
      Redis pub/sub invalidation is an optimization, not the correctness gate.
    rationale: >
      The project owner chose immediate revocation over post-invalidation
      eventual consistency on 2026-10-28.
    consequences: >
      Update F1 DB, architecture, security and BR-13; test AC-25 while Redis
      delivery to another instance is delayed.
  - id: DEC-2026-1028-qa-f1-audit-export-content
    kind: api-contract
    status: ACTIVE
    date: 2026-10-28
    subject: "F1 audit CSV content is localized safe description"
    decision: >
      The audit CSV content column contains a human-readable description
      rendered from actionType and redacted safeParams, never raw before/after
      JSON. Use Accept-Language vi or en, defaulting to vi when absent.
    rationale: >
      The project owner resolved the AC-32 CSV oracle and locale on 2026-10-28.
    consequences: >
      Update F1 API #40, BR-19/16 and AC-32 testcase.
  - id: DEC-2026-1028-qa-f1-audit-admin-guard
    kind: api-contract
    status: ACTIVE
    date: 2026-10-28
    subject: "F1 audit read rejects non-ADMIN with 403 IDT-447"
    decision: >
      A valid authenticated non-ADMIN calling ADMIN-only audit read endpoints
      is rejected with 403 IDT-447 by the baseCategory guard, separate from
      missing Function:Action permission IDT-445.
    rationale: >
      The project owner resolved the AC-31 role-boundary oracle on 2026-10-28.
    consequences: >
      Update F1 API audit reads and the AC-31 negative testcase.
  - id: DEC-2026-1028-qa-f1-reopen-anonymized
    date: 2026-10-28
    status: ACTIVE
    subject: F1 refuses reopening an anonymized account with 422 IDT-448
    decision: >-
      PATCH #37 targets one user. Reopening a user with anonymized_at non-null
      returns 422 IDT-448 without user/session changes or a success audit.
      A multi-selection UI combines individual responses into a partial outcome.
    rationale: >-
      The identity data has already been removed irreversibly. A dedicated,
      stable status/code lets API consumers and QA distinguish this business
      refusal from permission or dependency errors. Owner confirmed 2026-10-28.
  - id: DEC-2026-1028-qa-f5-anomaly-baseline-admin
    kind: product-scope
    status: ACTIVE
    date: 2026-10-28
    subject: "F5 anomaly baseline includes ADMIN usage; ADMIN is excluded only from alert candidates"
    decision: >
      Owner confirmed on 2026-10-28: calculate the 24-hour anomaly baseline across all users with AI usage,
      including ADMIN users. Exclude ADMIN only from accounts eligible for an anomaly alert. This overrides
      only the ADMIN-excluded baseline clause of DEC-2026-1023-open-questions-closed; other job decisions remain active.
    rationale: >
      The owner chose the baseline population matching the DD aggregate SQL. ADMIN activity contributes
      to system-wide usage while administrators themselves do not receive account anomaly alerts.
    consequences: >
      Update JOB-AI-05 population description and the AC-56 fixture/calculation. Keep role filtering after
      the aggregate baseline calculation and preserve the inclusive ratio >= 5 threshold.
  - id: DEC-2026-1028-qa-f5-admin-audit-failure
    kind: api-contract
    status: ACTIVE
    date: 2026-10-28
    subject: "F5 admin AuditLogPort failure returns 503 AIR-503 with transactional rollback"
    decision: >
      Owner confirmed 503 AIR-503 for AuditLogPort failure during prompt publication (API17), budget limit
      changes (API19) and budget unlock (API20). Roll back the whole transaction, including any earlier
      feature/audit writes in a multi-feature publication. Do not expose a successful partial mutation.
    rationale: >
      An exact dependency-error contract allows API clients and QA to distinguish audit failure from
      provider errors while preserving mandatory audit and business-data consistency.
    consequences: >
      Update F5 API error catalog, logic and TDD plus recovery cases AC40-05 and AC43-05.

```

---

## ACTIVE (human-readable)

| ID | Date | Subject |
| :--- | :--- | :--- |
| `DEC-2026-0819-agentconfig-retarget-algoprep` | 2026-08-19 | Agent config retargeted to AlgoPrep; monorepo; taxonomy `01-rd` to `07-review`; plans to `06-plan` |
| `DEC-2026-0819-language-config-en-docs-vi` | 2026-08-19 | Config in English, deliverable docs in Vietnamese, skill template blocks stay Vietnamese |
| `DEC-2026-0819-gitnexus-hard-rules-after-index` | 2026-08-19 | GitNexus MUST/NEVER are hard rules, dormant until the first `gitnexus analyze` |
| `DEC-2026-0819-zflow-model-tiers-claude5` | 2026-08-19 | zflow tiers: Claude 5 family TIER0/TIER1, haiku TIER2, conservative fallback |
| `DEC-2026-0819-mermaid-supersedes-md-builder` | 2026-08-19 | One owner per diagram format; `nexa-md-builder` archived |
| `DEC-2026-0820-stack-versions` | 2026-08-20 | Java 21 LTS + Spring Boot 4.0.x + Maven; Next.js 16.x App Router + TypeScript strict + pnpm |
| `DEC-2026-0820-architecture-baseline` | 2026-08-20 | Modular Monolith: DDD + Clean Architecture + ports in/out + command/query; FSD on the frontend |
| `DEC-2026-0823-go-judge-default-engine` | 2026-08-23 | go-judge replaces Judge0 as default execution engine; port stays engine-neutral, Judge0 re-pluggable |
| `DEC-2026-0824-dark-light-theme` | 2026-08-24 | AlgoPrep supports both Dark and Light theme; Light is the default |
| `DEC-2026-0824-i18n-vi-en` | 2026-08-24 | AlgoPrep UI supports Vietnamese and English; 4 Admin screens still need the bilingual scaffold |
| `DEC-2026-0824-dual-submission-model-per-problem` | 2026-08-24 | Every problem supports both Function Wrapper and Standard I/O in parallel; learner picks per attempt, not instructor per problem |
| `DEC-2026-0825-frontend-base-architecture` | 2026-08-25 | Frontend base: `/admin` + `/instructor` URL prefixes, next-intl with no locale segment, next-themes + CSS-variable tokens, pnpm workspace root at `05-coding/` |
| `DEC-2026-0825-shared-content-authoring-screens` | 2026-08-25 | `problem_authoring`, `problem_management`, `interview_question_management` shared between A2/A3, mounted at both `/instructor` and `/admin`, scoped by permission |
| `DEC-2026-0825-uml-skill-parity` | 2026-08-25 | `vp-uml-architect` at parity with `mermaid-uml-architect`: shared complexity lint, shared colour palette (monochrome still default), stick-figure actors decide the tool |
| `DEC-2026-0827-drawio-uml-architect` | 2026-08-27 | Third UML skill `drawio-uml-architect`: spec-driven `.drawio` for Use Case / Sequence / Activity with a layout engine and a validator; ceiling now in four scripts; boundary with `nexa-ad-builder` is audience, not file format |
| `DEC-2026-0828-split-class-management-assignments` | 2026-08-28 | `class_management` split into `class_management` + `class_assignments`, one slug per Next.js route; teacher screens 4 → 5, total 29 → 30 |
| `DEC-2026-0828-remove-rejudge-scope` | 2026-08-28 | Rejudge removed entirely: F4-09a→e, `admin_rejudge` screen, `REJUDGE_MANAGEMENT` function all out of scope; testcase fixes handled by student-report + manual instructor fix, no mass re-grading |
| `DEC-2026-0828-remove-per-class-interview-set` | 2026-08-28 | Per-class interview question sets removed: F6-11, `US-A2-04` dropped; students share one system-wide interview bank |
| `DEC-2026-0830-problem-lifecycle-two-states` | 2026-08-30 | Problem lifecycle simplified to two states — `Chưa xuất bản` / `Đã xuất bản` — dropping the prototype's third `Đã ẩn` state; new code `F2-15`, `US-A2-01b` |
| `DEC-2026-0830-remove-student-review-request` | 2026-08-30 | Student-initiated "Yêu cầu review" removed from scope — never had a code; `instructor_grading` queue now sourced only from low AI score |
| `DEC-2026-0830-class-progress-dashboard` | 2026-08-30 | `class_progress` aggregate dashboard settled "good enough": new code `F1-28`, `US-A2-09`; closes Q1-Q6 with default formulas/thresholds |
| `DEC-2026-0830-interview-bank-crud` | 2026-08-30 | System-wide interview question bank CRUD: new code `F6-13`, `US-A2-10`, separate `interview_question_authoring` screen, 5-topic taxonomy replaces stale 4; closes Q2-Q7 |
| `DEC-2026-0831-admin-overview-dashboard-stats` | 2026-08-31 | `admin_overview`: one aggregate code `F1-29` for 8 uncoded stat blocks; difficulty-chart "AI sinh"/"Giảng viên soạn" legend confirmed mislabeled, not a real feature |
| `DEC-2026-0831-remove-tiered-hints-ai-config` | 2026-08-31 | "Gợi ý theo bậc" (tiered in-progress AI hints) cut from scope entirely — full prototype feature, no code, would be a third AI flow |
| `DEC-2026-0831-remove-plagiarism-report` | 2026-08-31 | "Báo cáo nghi gian lận" (source-code plagiarism detection) cut from scope — no code, algorithm out of proportion for capstone |
| `DEC-2026-0831-problem-authoring-spec-tab` | 2026-08-31 | `problem_authoring` gets a 5th tab "Đặc tả" for F2-03/F2-04/F2-09 (function signature, matching strategy, testcase version) — was blocking `harness` BD/DD |
| `DEC-2026-0831-ai-instruction-injection-guard` | 2026-08-31 | F5-17 amended: instructor-authored per-problem AI context text is a second-tier instruction, never a system instruction — closes a real prompt-injection gap |
| `DEC-2026-0831-admin-overview-ui-decisions` | 2026-08-31 | `admin_overview`: 5-verdict display (not 3-group merge), entity search dropped, one "Xem tất cả" link added, per-block empty/loading/error states |
| `DEC-2026-0831-judge-orchestration-ops-details` | 2026-08-31 | `F4-10` gets basic cluster controls, `F4-11` gets 3 sandbox params spelled out, "kỳ thi" confirmed a mislabeled priority tier, not a real feature |
| `DEC-2026-0831-ai-usage-anomaly-alert` | 2026-08-31 | `F5-25` amended: per-account anomalous AI usage gets a soft warning, not an auto-lock — distinct from the budget-exceeded auto-lock |
| `DEC-2026-0831-i18n-scope-expansion` | 2026-08-31 | `DEC-2026-0824-i18n-vi-en` scope widened from 4 named Admin screens to every Admin/Instructor screen — named list had gone stale |
| `DEC-2026-0831-problem-management-lifecycle-details` | 2026-08-31 | `problem_management`: soft delete (`Chưa xuất bản` + `deleted` flag), publish checklist thresholds 8/2/2, catalog-health thresholds 30%/7 days, new codes `F2-16` (duplicate), `F2-17` (CSV export) — CSV import of whole problems cut |
| `DEC-2026-0831-problem-authoring-round2` | 2026-08-31 | `problem_authoring`: "Gợi ý theo cấp độ" cut (same feature as the already-cut "Gợi ý theo bậc"); tags, 2 extra F2-10 limits, and last-edited-by field kept, no new codes |
| `DEC-2026-0831-class-assignments-round2` | 2026-08-31 | `class_assignments`: assign-from-bank flow gets an inline modal; removing an assigned problem is soft (`F2-12` amendment, no submission-history loss) |
| `DEC-2026-0831-instructor-overview-dashboard` | 2026-08-31 | `instructor_overview`: new code `F1-30`, story `US-A2-11` — owner confirmed all sub-decisions (Q1-Q5, Q7) individually; Q3 raised a separate open question on judge/problem-bank scoring, logged, not yet resolved. **Formally approved by the owner 2026-09-05** after the 2026-08-31 confirmation round was interrupted | |
| `DEC-2026-0831-outside-screens-closures` | 2026-08-31 | `F6-07` no re-edit after submit, `F5-01` stays opt-in, `F4-07` timeout = 5 min; email notifications and harness Strategy/Plugin were already answered before the RD review report flagged them as open |
| `DEC-2026-0831-partial-score-testcase-ratio` | 2026-08-31 | New `F4-13`: testcase-pass-ratio score (1pt/problem), shown to learner alongside unchanged Accepted/WA verdict; drops fail-fast (`F4-04`); `F1-06` gets best-attempt aggregation. Partially reverses 2026-08-24 mục 2.6 phần 1 — only the fail-fast half, not the no-author-weights half |
| `DEC-2026-0831-rd-req-split-by-module` | 2026-08-31 | `req.md` split into 7 per-Bounded-Context files + `nfr.md`; `user_stories.md` split into 5 per-actor files; both kept as thin indexes; downstream citations converted from line numbers to `Fx-nn`/`US-Axx-nn` code anchors |
| `DEC-2026-0901-backend-base-architecture` | 2026-09-01 | Backend base: ArchUnit (not Spring Modulith) for boundary tests, all 8 Maven modules declared up front (diverges from `codebase_structure.md` just-in-time rule), JaCoCo 80% configured now and armed per module, Spring Boot 4.0.8, virtual threads on with an asserting test |
| `DEC-2026-0901-bd-layout-by-concern` | 2026-09-01 | `02-bd/` laid out by concern (`architecture`/`database`/`security`/`storage`), not by Bounded Context directory; `packages/` `commons/` `jobs/` `system-docs/` retired, harness BD moved |
| `DEC-2026-0903-sot-citation-scope` | 2026-09-03 | Deliverable docs may name a `DEC-` **by id**, but must not cite `.claude/` or `.nexa/` by path plus line number; narrows the 2026-09-01 blanket ban that RD itself was violating in ~30 files |
| `DEC-2026-0908-diagram-per-actor-subfolders` | 2026-09-08 | `08-diagram/` usecase/ and sequence/ go back to per-actor subfolders (`admin/` `gv/` `user/` `shared/`), reversing the 2026-09-07 flatten; `usecase_tong_quan_actor.*` stays at the `usecase/` root |
| `DEC-2026-0912-judge-callback-contract` | 2026-09-12 | `judge-orchestration` BD chốt: state machine submission (`PENDING→COMPILING→JUDGING→` 7 trạng thái cuối), `judge_run_tokens` làm khoá idempotency trung lập theo port, sweep timeout = 5 phút không cập nhật/chu kỳ quét 60 giây, webhook auth chỉ là chi tiết nội bộ của `Judge0Adapter` |
| `DEC-2026-0912-judge-outbox-pattern` | 2026-09-12 | `judge-orchestration` dùng Transactional Outbox (bảng `outbox_events` + relay polling) cho việc phát sự kiện F4→F5/F1, thay vì `@TransactionalEventListener`, vì cơ chế sau không sống sót qua sự cố broker sau khi commit |
| `DEC-2026-0921-class-completion-owned-by-identity` | 2026-09-21 | Bỏ `GetClassAssignmentCompletion`; `identity` tự tính tỉ lệ Hoàn thành và chốt 4 liên kết chân trang. Nguồn tử số toàn cục được thay bởi `DEC-2026-1025-dd-class-assignment-time-window` |
| `DEC-2026-0921-admin-shell-footer` | 2026-09-21 | Khung Admin dựng chân trang thật cho mọi route `/admin/*`, kể cả `admin_overview` (divergence có chủ đích với prototype của màn đó); đóng Q4 của `_shell.md` |
| `DEC-2026-0921-teacher-screens-conflict-resolutions` | 2026-09-21 | Gộp 5 quyết định của đợt BD khu Giảng viên: khu này cũng có chân trang (đi ngược bằng chứng prototype, ưu tiên nhất quán); một bộ 5 nhãn trạng thái học viên dùng chung cụm lớp; Điểm TB tính theo phạm vi lớp chứ không lấy read model toàn cục; `manual_graded_at IS NULL` chuyển thành điều kiện của tab; màn chấm bài nhận bộ lọc học viên |
| `DEC-2026-0915-admin-separate-login-route` | 2026-09-15 | Admin có route `/admin/login` riêng, tách khỏi màn `auth` dùng chung; cùng contract đăng nhập, chỉ khác route/skin (liquid-glass, không signup/OAuth) |
| `DEC-2026-0922-users-and-admin-conflict-resolutions` | 2026-09-22 | Gộp 7 quyết định chốt mâu thuẫn SoT và 17 khoảng trống schema khu Người học và Quản trị theo tiêu chí ponytail tinh gọn: bộ chuyển Theme/Locale trên Header Người học; 4 cấp độ phỏng vấn; ranh giới users.default_language; Theme/Locale lưu per-device; định nghĩa bài đang làm dở; sandbox toàn cục; audit log luôn mở cho Admin; nav Nội dung cho Giảng viên |
| `DEC-2026-0925-instructor-separate-login-route` | 2026-09-25 | Giảng viên có route `/instructor/login` VÀ API riêng `POST /auth/instructor/login` (không chỉ đổi giao diện như Admin) — API tự chặn tài khoản không phải INSTRUCTOR bằng `403 IDT-111` |
| `DEC-2026-0926-sequence-detail-tier` | 2026-09-26 | `drawio-uml-architect` có thêm ngưỡng độ phức tạp thứ hai cho sơ đồ tuần tự NHIỀU TẦNG (`sequence.detail = true`): 10 participant / 56 message / nhiều khối `alt`, kèm dải phân cách theo giai đoạn và đánh số phân cấp. Ngưỡng dùng chung 6/14/1 của `DEC-2026-0825-uml-skill-parity` giữ nguyên cho mọi sơ đồ không bật cờ này |
| `DEC-2026-0927-diagram-tree-flatten-by-type` | 2026-09-27 | `08-diagram/` xếp theo `<loại>/<actor>/`, bỏ tiền tố đường dẫn tài liệu (`08-diagram/01-rd/req/...` → `08-diagram/...`). Một sơ đồ nay vẽ theo tính năng, lấy nguồn từ RD + BD + DD nên không tài liệu nào sở hữu riêng. Thư mục con theo actor của `DEC-2026-0908` giữ nguyên |
| `DEC-2026-0927-usecase-overview-tier` | 2026-09-27 | `drawio-uml-architect` có ngưỡng riêng cho sơ đồ use case TỔNG QUAN (`usecase.scope = "overview"`): 24 node / 28 cạnh, khổ A3 ngang. Ngưỡng dùng chung 16/16 + A4 giữ nguyên cho 21 sơ đồ use case theo từng chức năng |
| `DEC-2026-0927-diagram-covers-field` | 2026-09-27 | Mỗi `.spec.json` tự khai trường `covers` (các mã `Fx-nn` sơ đồ thật sự vẽ); cột sơ đồ của `feature_catalog` đọc trường đó thay vì grep chuỗi mã trong spec. Ghi chú `_` trở lại là văn xuôi tự do. Đối chiếu bằng `check_catalog.py`, lệch thì mã thoát khác 0 |
| `DEC-2026-0928-diagram-print-legibility` | 2026-09-28 | Lint sơ đồ kiểm **chữ in ra có đọc được không** (đặt rộng 6.53in trong báo cáo, cảnh báo khi dưới 6pt) thay cho kiểm "trang có đúng A4 không". Cỡ chữ nâng theo: sequence chi tiết 11 lên 13, use case tổng quan 12 lên 17. Bề ngang sơ đồ không đổi nên chỉ tốn thêm chiều dọc |
| `DEC-2026-0928-f2-14-generator-script` | 2026-09-28 | F2-14 đổi từ "AI sinh thẳng dữ liệu testcase" sang "AI viết **script** sinh dữ liệu, chạy trong sandbox". Thêm giới hạn kích thước output, hạt giống cố định, và bước kiểm input thoả ràng buộc. Chỉ testcase **đã duyệt** mới tính vào checklist xuất bản. Đáp án mẫu tách thành mã riêng **F2-18**, không bao giờ đưa vào prompt AI |
| `DEC-2026-0927-student-dashboard-home` | 2026-09-27 | Khu Người học có màn Tổng quan riêng `USR0601_dashboard` (màn thứ 13); đích sau đăng nhập của `STUDENT` đổi từ `my_progress` sang `dashboard`, sửa một phần Q3 của `SHR0101_auth.md`. Ranh giới: `dashboard` trả lời "làm gì tiếp theo", `my_progress` giữ vai trò "tôi đang ở đâu" |
| `DEC-2026-0927-student-area-merge-and-shared-shell` | 2026-09-27 | Gộp `my_progress` vào `dashboard` (bỏ `/progress`); dựng lại nav khu Người học theo `_shell.md` mục 2.2 (6 mục, thêm route `/workspace` và `/mock-interview`); dựng chân trang cho cả khu; một `PageContainer` dùng chung cho cả 3 shell với trần px cấu hình ở `shared/config/layout.ts`. Lật một phần Q1 của `DEC-2026-0922` và mục 4 của `_shell.md` |
| `DEC-2026-0929-default-accent-from-layoutbase` | 2026-09-29 | Bảng màu nhấn của scope mặc định (khu người học) lấy verbatim từ mockup: Sáng `#1c9bf2` trên chữ trắng, Tối `#4cc2ff` trên chữ `#06131e` (`Đăng nhập & Đăng ký.dc.html:19` và `:25`). Thay cho placeholder `#2563eb`/`#3b82f6` — bản Tối cũ là xanh TRUNG mang chữ gần đen, mockup ghép chữ gần đen với xanh da trời NHẠT hơn hẳn. Lớp phủ nằm trên panel màu nhấn thôi hardcode trắng/đen, đổi sang pha từ `--color-on-primary`. `.admin-shell` và `.instructor-shell` giữ nguyên |
| `DEC-2026-0929-fsd-views-grouped-entities-domain-only` | 2026-09-29 | Frontend: `views/` chia nhóm theo actor giống `01-rd/screens/` (`views/{admin,teacher,users,shared}/<slug>`, bỏ tiền tố actor khỏi tên slice). `entities/` chỉ giữ danh từ nghiệp vụ có từ 2 slice dùng; DTO của riêng một màn nằm trong `model/` + `api/` của view đó. 10 entity chuyển vào view, `problem-draft` gộp vào `problem`: entities từ 22 xuống 11 |
| `DEC-2026-0927-submission-metrics-two-ports` | 2026-09-27 | `GetStudentSubmissionMetrics` tách làm hai cổng đọc — `GetClassStudentSubmissionMetrics` (Giảng viên, đọc dữ liệu người khác theo phạm vi lớp, gom theo tuần) và `GetMySubmissionMetrics` (Người học, chỉ đọc dữ liệu của chính mình, danh tính lấy từ token, gom theo ngày) — và lần đầu được viết vào BD module `judge-orchestration` mục 3.3. Tách thay vì gộp để quy tắc chống IDOR thành thuộc tính của chữ ký |

| `DEC-2026-1001-single-overview-page-kpi` | 2026-10-01 | Mỗi khu chỉ một trang tổng quan hiển thị KPI; các trang danh sách và giám sát chỉ có tiêu đề, bộ lọc, danh sách, phân trang. Panel phụ cạnh danh sách giữ nguyên. Chỉ số "Yêu cầu đặt lại mật khẩu" chuyển lên tổng quan Admin. Kèm quy ước UI: nút thao tác dạng icon + tooltip (`IconAction`), sidebar Admin ghim, khe `leading` của `PageHeader` |
| `DEC-2026-1001-admin-configurable-settings` | 2026-10-01 | ADMIN tự cấu hình mọi tham số và danh mục; chủ đề câu hỏi phỏng vấn thành dữ liệu do admin quản lý (thay riêng phần "5 chủ đề cố định" của `DEC-2026-0830-interview-bank-crud`); thẻ mật khẩu ở tổng quan đếm từ audit log; chốt 13 đề xuất giới hạn. Giữ hai bất biến: luôn còn ít nhất một ADMIN hoạt động, và dòng audit log không xoá tay. Vòng 5 (2026-10-03): độ khó câu hỏi phỏng vấn cũng thành danh mục ADMIN quản lý (`question_levels`, luôn còn ít nhất một mục, màu không cấu hình). Vòng 6 (2026-10-03): độ khó bài tập thành danh mục riêng `problem_levels`, cùng quy tắc, không gắn logic |
| `DEC-2026-1002-split-detail-and-edit-pages` | 2026-10-02 | Trang chi tiết chỉ đọc cho bài tập và câu hỏi phỏng vấn (slug mới `problem_info`, `interview_question_info`); form soạn chuyển xuống `/edit`, `/new` là form tạo mới. Khu Admin trước, Giảng viên làm sau. Thay riêng phần khoá route của `SHR0302` (Q6) và route `[id]` của `SHR0202` |
| `DEC-2026-1003-markdown-latex-libraries` | 2026-10-03 | Chọn thư viện xem trước Markdown và LaTeX (`react-markdown`, `remark-gfm`, `remark-math`, `rehype-katex`, `katex`), gói trong `shared/ui/markdown-preview.tsx`; không parse HTML thô nên chưa cần sanitizer. Màn soạn giữ Markdown thô, chỉ màn chỉ đọc hiển thị bản xem trước |
| `DEC-2026-1003-bd-core-sot-tier` | 2026-10-03 | BD lên cùng bậc P2 với RD (BD là lõi); thứ tự đọc RD + BD, rồi DD, cuối cùng prototype/code. BD thắng RD về chi tiết thiết kế (làm theo BD, nêu chỗ lệch, đề xuất sửa RD); lệch về phạm vi nghiệp vụ (tính năng có/không, quyền actor) thì hỏi owner và ghi decision |
| `DEC-2026-1003-toast-feedback-channel` | 2026-10-03 | Toast dùng chung (`shared/ui/toaster.tsx`, gắn một lần ở `app/providers`) là kênh duy nhất báo kết quả thao tác và lỗi nhập; hiện bên phải-trên, tự tắt, có nút X; ô sai chỉ đổi viền và `aria-invalid`. Thay chốt cũ "lỗi hiện inline, không toast" ở `SHR0101` |
| `DEC-2026-1003-ai-testcase-generation-quota` | 2026-10-03 | Mỗi bài tối đa 2 lần "Sinh tự động" (F2-14), ADMIN cấu hình ở Cấu hình AI (`ADM0301` Khu vực H); chỉ lần thành công tính; bộ đếm là cột `problems.ai_testcase_generations_used`; người soạn chỉ thấy "Còn N/M lần"; giới hạn sandbox của script thuộc Vận hành |
| `DEC-2026-1003-single-function-signature` | 2026-10-03 | F2-03: MỘT chữ ký hàm chung cho ba ngôn ngữ thay cho ba chữ ký riêng; tên hàm mỗi ngôn ngữ tự suy ra (Python snake_case, Java và C++ camelCase), có ghi đè; `function_signatures` một dòng mỗi bài, thêm `name_overrides`; tên tham số giữ lại cho mã khung và hiển thị testcase mẫu |
| `DEC-2026-1005-list-screens-open-questions-closed` | 2026-10-05 | Đóng các câu hỏi mở của màn bài tập và câu hỏi phỏng vấn theo đề xuất; cỡ trang nhớ theo tài khoản; danh sách câu hỏi chỉ có xoá theo lô, nhân bản chỉ theo dòng và mở form điền sẵn; bộ lọc `FilterMenu`, thanh `FilterBar`, thanh chọn `BulkActionBar` nổi ở trên; độ tương phản chữ tối thiểu 4.5:1 nên đậm các token chữ phụ, nhãn, nút chính; nút Nhập CSV ẩn khi demo |
| `DEC-2026-1008-admin-system-open-questions-closed` | 2026-10-08 | Đóng các câu hỏi mở còn lại của ma trận quyền và nhật ký: form tạo vai trò chọn nhóm cơ bản (mặc định STUDENT), ô chỉ khoá theo vai trò hệ thống STUDENT, chặn xoá vai trò đang có người dùng, không đổi tên vai trò; "Lưu trữ" giữ ở màn nhật ký và xác nhận lần hai khi rút ngắn hạn hoặc đổi sang Xoá hẳn; hộp xuất áp bộ lọc đang chọn và từ chối ngày quá hạn hoặc tương lai |
| `DEC-2026-1008-admin-system-screens-review` | 2026-10-08 | Rà soát ba màn khu Hệ thống: ma trận quyền lưu tức thời từng ô, bỏ nút "Lưu thay đổi", chặn cứng gỡ `PERMISSION_MATRIX:UPDATE` cuối cùng của mọi vai trò `ADMIN`, cảnh báo xác nhận khi tự gỡ quyền của vai trò mình giữ; nhật ký có bộ lọc "Từ ngày / Đến ngày"; danh sách người dùng có thao tác đổi vai trò, đặt lại mật khẩu, khoá/mở khoá trên từng dòng |
| `DEC-2026-1008-problem-delete-and-duplicate` | 2026-10-08 | Xoá bài toán = ẩn mềm (`UNPUBLISHED` + `deleted`), hộp xác nhận cảnh báo không chặn; nhân bản bài toán làm thật, nhân bản câu hỏi phỏng vấn bỏ hẳn; Nhập CSV làm thật. |
| `DEC-2026-1009-ui-review-screenshot-evidence` | 2026-10-09 | Finding review UI/prototype bắt buộc có ảnh Playwright thật (khung đỏ + ảnh cắt, trước/sau khi sửa); rule `ui-review-evidence.md`, skill `ui-evidence-review`. |
| `DEC-2026-1010-admin-selects-model-per-prompt` | 2026-10-10 | API key giữ ở hạ tầng backend; dev khai báo danh sách model được duyệt, ADMIN chọn model cho từng prompt trong danh sách đó (lưu kèm phiên bản prompt, đi qua "Lưu và phát hành"); bỏ hiển thị nhiệt độ, giữ giới hạn token chỉ đọc; danh sách đơn giá chưa đổi |
| `DEC-2026-1010-split-ai-admin-screens` | 2026-10-10 | Nhóm AI ở Admin tách ba màn: `ADM0301` Prompt và chất lượng (tab theo tính năng, nháp rồi phát hành), `ADM0303` Hạn mức và chi phí (màn mới, ghi ngay: hạn mức token tháng, mở khoá, giới hạn tần suất theo giờ, ngưỡng, đơn giá, trần sinh testcase), `ADM0302` chỉ theo dõi. Nguyên tắc trả lời lưu theo từng tính năng, "Không đưa lời giải đầy đủ" khoá bật. Gói đăng ký trả phí chỉ ghi là hướng tương lai |
| `DEC-2026-1010-ai-generate-testcase-feature-code` | 2026-10-10 | `feature_code` của `prompt_templates` và `token_usage` thêm giá trị `GENERATE_TESTCASE` (seed một prompt thứ ba, `owner_id` là `problems.id`), không bảng hay cổng mới; không áp giới hạn theo giờ cho giá trị này. Tiêu chí ponytail bậc 4 và 2 |
| `DEC-2026-1010-ai-rate-limit-fixed-window` | 2026-10-10 | Giới hạn tần suất F5-19 đếm theo cửa sổ cố định mỗi giờ trên Redis (INCR + hết hạn 1 giờ), thời gian chờ giữa hai yêu cầu là khoá riêng; chỉ áp cho phân tích bài giải và phỏng vấn giả lập, trúng cache không tính. Tiêu chí ponytail bậc 5 |
| `DEC-2026-1010-admin-operations-regroup` | 2026-10-10 | Nhóm Vận hành Admin còn ba màn: `ADM0401` Hàng đợi chấm (dải trạng thái, bảng worker gọn, xác nhận khi dừng queue hoặc tắt autoscale, tab job có đếm), `ADM0404` Sự kiện hạ tầng (màn mới, chỉ đọc), `ADM0501` chia hai tab. Schema: `language_configs.enabled`, bảng `grading_defaults`, `sandbox_settings` (toàn cục), ba cột mới của `cluster_settings`. Quy tắc: dừng queue thì sweep bỏ qua `PENDING`, mọi thao tác vận hành ghi Nhật ký hệ thống (phân loại "Cấu hình"), cấu hình mới áp từ lúc bắt đầu chấm. **Đã xác nhận 2026-10-10 (tiêu chí ponytail):** bỏ ưu tiên job, mức độ trễ thứ hai và cột "Job"; không thêm trần worker hay cảnh báo mềm; sự kiện hạ tầng chỉ từ Outbox kẹt và lượt nộp lỗi sandbox, lưu 90 ngày; không tự làm tươi, `?level=error` từ thẻ liên kết. Rủi ro mở: kiểm metric go-judge, autoscale trên Docker Compose |
| `DEC-2026-1011-admin-system-open-questions-closed` | 2026-10-11 | Nhóm Hệ thống Admin đóng câu hỏi mở H1-H5: `users.status` thêm `PENDING_VERIFICATION` (không cột mới); `ListUsers` trả `hasPassword` suy từ `password_hash`; thêm endpoint `CreateUserAccount`; enum `AuditAction` mang phân loại, dịch vụ, khoá mẫu câu; bảng khoá-giá trị `identity.system_settings` giữ cấu hình lưu log; bản "Nén và lưu trữ" hoãn (job giữ nguyên dòng, không xoá) |
| `DEC-2026-1011-admin-system-open-questions-closed` | 2026-10-11 | Nhóm Hệ thống Admin đóng câu hỏi mở H1-H5: `users.status` thêm `PENDING_VERIFICATION` (không cột mới); `ListUsers` trả `hasPassword` suy từ `password_hash`; thêm endpoint `CreateUserAccount`; enum `AuditAction` mang phân loại, dịch vụ, khoá mẫu câu; bảng khoá-giá trị `identity.system_settings` giữ cấu hình lưu log; bản "Nén và lưu trữ" hoãn (job giữ nguyên dòng, không xoá) |
| `DEC-2026-1011-ai-practice-feedback-in-shared-budget` | 2026-10-11 | Chấm câu luyện F6-08 tính vào ngân sách token chung: `token_usage.feature_code` thêm `PRACTICE_FEEDBACK`, khoá ngân sách F5-25 áp cả cho F6-08 (ADMIN được miễn); giới hạn tần suất F6-08 giữ hằng số 20 lượt/giờ, không thêm vào `ADM0303`. Tiêu chí ponytail bậc 6, 4, 5 |
| `DEC-2026-1011-ai-no-full-solution-can-be-turned-off` | 2026-10-11 | Nguyên tắc "Không đưa lời giải đầy đủ" mặc định bật nhưng ADMIN tắt được kèm cảnh báo; máy chủ không ép `true` nữa. Thay phần "khoá bật" của `DEC-2026-1010-split-ai-admin-screens`. Còn mở: có sửa F5-18 ở RD không |
| `DEC-2026-1011-class-assignment-popup-on-assignments-screen` | 2026-10-11 | Popup "Giao bài cho lớp" đặt ở màn `class_assignments` (INS0202), thay vị trí `problem_list` của `DEC-2026-0831-class-assignments-round2`. Tiêu chí ponytail bậc 4 |
| `DEC-2026-1011-a2-read-published-problems-of-others` | 2026-10-11 | A2 xem chỉ đọc (`problem_info`) bài đã xuất bản của người khác; bản nháp người khác vẫn 404, sửa/xoá chỉ bài của mình; nút "Chi tiết" ở `class_assignments` trỏ `/instructor/problems/{id}`. Điều chỉnh `SHR0203` Q8. Tiêu chí ponytail bậc 6 |
| `DEC-2026-1011-teacher-nav-badge-dropped` | 2026-10-11 | Bỏ badge số trên nav khu Giảng viên khỏi BD, không có endpoint tóm tắt cho khung; đóng `_shell` Q3. Tiêu chí ponytail bậc 2 |
| `DEC-2026-1011-teacher-screens-open-questions-closed` | 2026-10-11 | Đóng INS0101 Q4/Q6 (feed 10 mục gần nhất, lọc theo lớp phụ trách), INS0301 Q8/Q9 (một lớp một giảng viên; bài chưa có báo cáo AI vẫn vào hàng đợi, ô AI "Chưa có"), INS0204 Q5 (A2 xem chỉ đọc bài nộp của học viên trong lớp mình, ngoài lớp 404), INS0202 Q10 (bài bị rút vẫn hiện, nhãn "Đã rút xuống", không giao thêm) |
| `DEC-2026-1011-teacher-shell-identity-footer-built` | 2026-10-11 | Khu Giảng viên dựng khối danh tính + "Thoát" (đăng xuất) ở đáy sidebar, chân trang dùng component chung đổi tên từ chân trang Admin, mốc ép rail 1024px; đóng `_shell` Q4, Q5. Tiêu chí ponytail bậc 1, 4, 6 |
| `DEC-2026-1011-instructor-layout-follows-admin` | 2026-10-11 | Khu Giảng viên dùng bố cục giống khu Admin: sidebar nhóm gập (Lớp học, Giao và chấm bài, Nội dung), thanh công cụ có menu người dùng (Cài đặt, Trang cá nhân, Thoát), chân trang chung; giữ bảng màu giảng viên; thay phần (1) của quyết định khối danh tính ở đáy sidebar |
| `DEC-2026-1011-teacher-overview-toolbar-dropped` | 2026-10-11 | Bỏ ô tìm kiếm và nút "Tạo lớp mới" khỏi màn tổng quan giảng viên (`INS0101`), gồm cả yêu cầu trong RD mục 2.1 điểm 3; tạo lớp ở `class_management`, avatar ở menu người dùng chung |
| `DEC-2026-1011-class-detail-page` | 2026-10-11 | Thêm trang chi tiết lớp `INS0205` (`class_detail`, route `/instructor/classes/[classId]`) dưới "Lớp của tôi", đảo câu trả lời 2026-08-28 ở RD `INS0201` Q3; ở danh sách bỏ popup Mã mời và icon "Xem học viên", tạo lớp xong chuyển thẳng sang trang chi tiết. Không thêm endpoint. Tiêu chí ponytail bậc 1, 4 |
| `DEC-2026-1011-class-schedule-structured` | 2026-10-11 | Lịch học có cấu trúc: `classes.schedule_note` (text) đổi thành `classes.schedule` (jsonb: ngày trong tuần, giờ bắt đầu/kết thúc, phòng, từ ngày đến ngày, ghi chú; mọi phần tuỳ chọn); giới hạn phòng 100 và ghi chú 200 ký tự đã được owner xác nhận 2026-10-13. Tiêu chí ponytail bậc 1, 3 |
| `DEC-2026-1011-class-detail-no-stat-strip` | 2026-10-11 | Bỏ dải số liệu (5 thẻ: Học viên, Hoàn thành, Điểm TB, Cần chấm, Vắng bài) khỏi trang chi tiết lớp `INS0205`; các số này đã có ở danh sách lớp `INS0201`. Màn tổng quan giảng viên `INS0101` không bị ảnh hưởng. Tiêu chí ponytail bậc 4 |
| `DEC-2026-1013-instructor-avatar-menu-and-read-source` | 2026-10-13 | Avatar thanh công cụ Giảng viên là menu popup như Admin (đóng `INS0101` Q10); A2 đọc được mã nguồn bài nộp của học viên trong lớp mình, chỉ đọc, ngoài lớp 404 (đóng Q-J và `INS0301` Q10 phần (c)). Tiêu chí ponytail bậc 4, 1 |
| `DEC-2026-1013-teacher-open-questions-closed-round2` | 2026-10-13 | Đóng `INS0203` Q6 và `INS0204` Q6 (bộ lọc ghi vào URL `?classId=&q=&page=`, Quay lại dùng `router.back()`), `INS0205` Q6 (room 100, note 200, quy tắc lịch), `INS0301` Q7 (cho sửa điểm đã chấm, chỉ giữ giá trị mới nhất), Q-K (nhóm nav giảng viên mở sẵn). Tiêu chí ponytail bậc 5, 3, 4 |
| `DEC-2026-1013-instructor-submissions-list` | 2026-10-13 | Thêm mục sidebar "Bài nộp" (`/instructor/submissions`) cho giảng viên: danh sách mọi lượt nộp của học viên trong các lớp mình phụ trách, lọc theo lớp và kết quả, tìm kiếm, phân trang, trạng thái lọc nằm trong URL; mỗi dòng mở kết quả chỉ đọc. Chưa viết RD/BD. Tiêu chí ponytail bậc 1, 4 |
| `DEC-2026-1013-a2-reads-student-contact` | 2026-10-13 | A2 đọc được họ tên, email, số điện thoại của học viên trong lớp mình (chỉ đọc, ngoài lớp 404); màn `INS0204` bỏ dải thẻ số liệu và "Hoạt động gần nhất", thay bằng hai thẻ "Liên hệ" và "Học tập"; quy tắc prototype không dựng dải số liệu nhỏ. Tiêu chí ponytail bậc 1, 4 |
| `DEC-2026-1013-remove-instructor-manual-grading` | 2026-10-13 | Bỏ hẳn chấm tay của giảng viên: màn `INS0301`, `F5-27`, `US-A2-06`, nav "Chấm bài", khối và cột "Cần chấm tay" ở `INS0101`/`INS0201`/`INS0202`/`INS0205`, bảng `manual_grades` (chưa dựng). Giữ F5.1 và quyền A2 đọc mã nguồn. Thay `DEC-2026-0831-instructor-grading-round2` và `DEC-2026-1013-grading-queue-all-accepted`. Tiêu chí ponytail bậc 3, 2 |
| `DEC-2026-1014-instructor-overview-trim-and-nav-label` | 2026-10-14 | `INS0101` bỏ thẻ "Điểm TB lớp" và widget "Tiến độ học viên"; giữ các lối vào trang đọc bài nộp (đóng `INS0204` Q11); đổi nhãn nhóm nav "Giao và chấm bài" thành "Quản lý bài tập đã nộp" (đóng `_shell` Q6). Tiêu chí ponytail bậc 3, 4 |
| `DEC-2026-1014-drop-ai-score-10` | 2026-10-14 | Bỏ cột dẫn xuất `ai.solution_reviews.ai_score_10` và quy tắc quy đổi trọng số rubric thang 10 (không màn nào dùng; đóng OQ-F5-A). Giữ `result_json`, `readability_score`, `rubric_scores` từng tiêu chí và `rubric_configs`; số phận trọng số rubric admin (F5-23, ADM0301) chờ chủ dự án quyết. Tiêu chí ponytail bậc 3, 2 |
| `DEC-2026-1014-instructor-batch1-counting-rules` | 2026-10-14 | Đợt 1 câu hỏi mở giảng viên: đếm học viên theo người; lượt nộp tính theo lớp phụ trách (cả `INS0202`); bỏ `newStudentCount30d` và endpoint `GetInstructorOverviewStats`; hoạt động gần đây chỉ giữ sự kiện tức thời; bỏ mọi chênh lệch tuần/90 ngày (`INS0201`, `INS0202`), bỏ thẻ "Lượt nộp tuần này". Tiêu chí ponytail bậc 2, 3, 4, 5 |
| `DEC-2026-1014-sandbox-child-process-cap-constant` | 2026-10-14 | Giới hạn tiến trình con của sandbox giữ hằng số 60 (application.yml), không thêm ô số cho ADMIN ở `ADM0501`; BD bỏ NO5. Tiêu chí ponytail bậc 5 |
| `DEC-2026-1014-instructor-batch2-weekly-rules` | 2026-10-14 | Đợt 2 câu hỏi mở giảng viên: số liệu tuần tính lúc truy vấn (cache 15 phút), tuần bắt đầu thứ Hai và bỏ tuần dở dang; biểu đồ `INS0203` một đường; ô tìm kiếm chỉ lọc bảng; mẫu số theo tab lớp; dòng phụ "Chưa nộp 7 ngày qua"; giữ lịch sử nộp của bài đã gỡ khỏi lớp; `INS0202` theo lớp đang chọn. Tiêu chí ponytail bậc 2, 5, 6 |
| `DEC-2026-1014-instructor-batch3-4-contracts-and-removal` | 2026-10-14 | Đợt 3 và 4 câu hỏi mở giảng viên: endpoint tổng quan theo module sở hữu dữ liệu; `description` thêm vào `ClassCardDto`; mã mời cố định 7 ngày; nút "Gỡ khỏi lớp" cuối dòng và luôn hiện rõ, gỡ một lớp mỗi lần, sau khi gỡ học viên ở chi tiết thì quay lại màn gọi tới; bấm lại thẻ lớp đang chọn thì bỏ chọn. Tiêu chí ponytail bậc 4, 5, 6 |
| `DEC-2026-1014-instructor-batch5-6-nav-pagination-rubric` | 2026-10-14 | Đợt 5 và 6 câu hỏi mở giảng viên: màn khoan sâu sáng mục của màn gọi tới; phân trang 20 dòng cho bảng gộp, chi tiết lớp không phân trang; Quay lại sau tạo lớp về danh sách; Tổng quan giữ tĩnh; chủ đề "đầu + +n"; bỏ trọng số rubric admin (giữ danh sách tiêu chí chỉnh được, đóng OQ-F5-B); Tổng quan hiện 5 bài nhiều lượt nộp nhất; đóng các câu cũ; xu hướng theo tuần ISO. Tiêu chí ponytail bậc 5, 4, 3, 2 |
| `DEC-2026-1014-drop-solution-review-rubric` | 2026-10-14 | Bỏ hẳn rubric chấm bài giải (F5-23): mục rubric ở `ADM0301`, bảng `rubric_configs`, 5 thẻ tiêu chí trong báo cáo học viên (`USR0301`), trường `criteriaScores` và dòng `rubric_scores` của bài giải; giữ phần còn lại của báo cáo F5.1 và rubric phỏng vấn F5-15. Thay `DEC-2026-1014-drop-admin-rubric-config`. Tiêu chí ponytail bậc 3, 2 |
| `DEC-2026-1014-batch7-remaining-open-questions` | 2026-10-14 | Các câu còn lại: giữ % hoàn thành ở Tổng quan (identity tính); bỏ cột `weight_percent_snapshot`, trọng số phỏng vấn là hằng số trong mã; "Lưu và phát hành" là một giao dịch nguyên tử; danh sách model do dev khai báo ở cấu hình backend; áp mã AI thì lưu bản cũ vào lịch sử nháp; không sinh lại báo cáo; timeout LLM 30 giây; đồng bộ số liệu tổng catalog. Tiêu chí ponytail bậc 5, 6, 4 |
| `DEC-2026-1014-instructor-submissions-list-rules` | 2026-10-14 | Quy tắc danh sách bài nộp giảng viên `INS0206`: bài chưa chấm xong hiện ở "Tất cả"; mới nhất trước; cột tỉ lệ pass; tên học viên liên kết `INS0204`; học viên đã rời lớp vẫn hiện kèm "Đã rời lớp" (cần lưu lịch sử ghi danh); chỉ bài đã giao; một dòng mỗi lượt nộp; trang kết quả giảng viên là mục vùng giảng viên trong `USR0201`; chưa cấp mã F. Tiêu chí ponytail bậc 1, 4, 5, 6 |
| `DEC-2026-1014-keep-learning-history-on-leave` | 2026-10-14 | Gỡ học viên khỏi lớp thì giữ lịch sử làm bài, chỉ đánh dấu đã rời lớp (thay câu "xoá lịch sử" ở F1-26, `INS0205`); ghi danh giữ dòng kèm ngày rời. Tiêu chí ponytail bậc 6 |
| `DEC-2026-1014-class-leave-details` | 2026-10-14 | Chi tiết rời lớp: vào lại bằng mã mời thì dùng lại dòng ghi danh cũ và đặt `left_at` = NULL; số lượt nộp và AC của `INS0202` chỉ tính học viên đang trong lớp; endpoint gỡ học viên đổi từ DELETE sang `POST .../leave`; chữ xác nhận "Học viên sẽ rời khỏi lớp. Lịch sử làm bài ... vẫn được giữ." Tiêu chí ponytail bậc 5 |
| `DEC-2026-1014-instructor-submissions-list-details` | 2026-10-14 | Chi tiết cuối `INS0206` và trang kết quả giảng viên: lượt nộp gắn lớp có lần giao gần nhất; quay lại danh sách khôi phục bộ lọc; bộ trạng thái theo BD/DB, `SYSTEM_ERROR` chỉ ở "Tất cả"; rời rồi vào lại coi cả khoảng là trong lớp; màn gọi tới qua `from=`; trang kết quả giảng viên không realtime; máy chủ lấy học viên và lớp từ lượt nộp. Tiêu chí ponytail bậc 5, 4, 6 |
| `DEC-2026-1018-instructor-overview-richer` | 2026-10-18 | Tổng quan giảng viên dựng lại 8 khối có biểu đồ: hai thẻ số liệu, lịch dạy trong tuần, biểu đồ lượt nộp 7 ngày, bài nộp gần đây; thêm StatCard icon, AgendaList, RecentList vào shared. Chưa duyệt hình. Tiêu chí ponytail bậc 3, 2 | |
| `DEC-2026-1018-usr-open-questions-phase1-scope` | 2026-10-18 | Student-area open questions phase 1: KPI strips leave RD (F1-07 to USR0501), hint tab and roadmap/avatar/queue buttons dropped, in-progress = latest non-ACCEPTED submission, code draft in the browser, problem_list/problem_detail/USR0401-0402 scope defaults accepted, practice feedback qualitative plus percent per criterion |
| `DEC-2026-1018-usr-open-questions-phase2-5` | 2026-10-18 | Student-area open questions phases 2 to 5: four columns (testcases.explanation_md, submission_testcase_results.actual_output_snippet, users.target_position, users.password_changed_at), behaviour defaults and limits (undo unsave, 20-row paging, 64 KB code, 5000-char answers, Beats from 10), account security defaults, UTC+7 and 0-10 scale, dashboard owns F6-10, mock-interview entry dropped, at most 3 classes |
| `DEC-2026-1018-account-deletion-grace-cancel` | 2026-10-18 | Account deletion: the 30-day grace period allows self-cancel by signing in again (SHR0101 recovery banner); reverses the Settings Q8 line of phase2-5; Settings copy updated |
| `DEC-2026-1018-usr-open-questions-last-four` | 2026-10-18 | Student-area last four open questions: three F6-12 self-rating levels chosen by the learner (no AI), pitfalls stay in suggested_approach, /saved empty state is a guidance sentence, withdrawn saved problems stay with a warning label and a disabled button |
| `DEC-2026-1019-usr-fe-audit-fixes` | 2026-10-19 | Student-area FE audit: 21 owner decisions applied (no small strip on USR0201, kept-question filter, editable withdrawn note, dashboard 3 sessions, 6-value verdict menu, topic chips); shared upgrades added |
| `DEC-2026-1020-sequence-labels-plain-language` | 2026-10-20 | Sequence labels in plain Vietnamese for non-technical readers: no endpoint paths, HTTP statuses, permission codes or jargon; lifelines unchanged |
| `DEC-2026-1020-student-workspace-split-and-password-in-settings` | 2026-10-20 | Student Workspace split into overview `/problems/{id}`, landing `/workspace`, coding `/workspace/{id}` (replaces the Workspace-redirect part of `_shell.md` Q2); password change only in Settings, Profile read-only; shared Modal portals to the shell root |
| `DEC-2026-1021-student-open-questions-batch-and-run-results-dialog` | 2026-10-21 | Student open-question batch (USR0102/0104/0105/0101/0401/0402) and Workspace run-results sub-screen: hidden testcases never reveal data, one shared in-progress component, shared FilterBar |
| `DEC-2026-1022-identity-schema-sync` | 2026-10-22 | Identity schema sync: account status source, forced password change, email verification OTP, class progress computed on read, activity source, single preferences table, invite code format, default INSTRUCTOR permissions |
| `DEC-2026-1022-problem-bank-schema-sync` | 2026-10-22 | Problem-bank schema sync: constraints_md, reference solution table, testcase origin/approved/category, worked_examples table, merge problem_specs with function_signatures, problem code and submission limit defaults |
| `DEC-2026-1022-interview-bank-schema-sync` | 2026-10-22 | Interview-bank schema sync: display_no for admin, two meanings of review due, drop practice_history.questions_total, title is the question field, fixed 1/3/7 day recall intervals |
| `DEC-2026-1022-ai-judge-schema-sync` | 2026-10-22 | Ai-review and judge schema sync: interviewer_level stays a 4-value enum, early finish scores the session, one OPEN anomaly alert per user, monthly budget rollover job, no effective-limit snapshot on submissions |
| `DEC-2026-1022-harness-empty-schema-kept` | 2026-10-22 | The harness schema stays and stays empty; no tables |
| `DEC-2026-1022-db-sync-round2` | 2026-10-22 | DB docs sync round 2: Admin-created accounts verified by first login, activity calendar read from identity, soft forced password change, SANDBOX_FAULT written by the worker, SubmissionGraded payload, outbox cleanup, ABANDONED session display, sample-answer result not shown, no optimal-complexity input, category display, worked examples endpoint |
| `DEC-2026-1022-self-register-pending-login-blocked` | 2026-10-22 | Self-registered PENDING_VERIFICATION accounts cannot sign in until the email OTP is entered; related identity defaults (active-user card source, activity grid level, reset clears flags, unlock blocked after anonymisation) |
| `DEC-2026-1022-db-sync-round4` | 2026-10-22 | DB docs sync round 4: per-problem AI context columns, cache-hit review row, ClassDeleted event, interview defaults in user_preferences |
| `DEC-2026-1023-db-actor-review-fixes` | 2026-10-23 | DB review against the three actors: LANGUAGE_CONFIG as 11th seed function (132 permission rows), bootstrap ADMIN at startup, interview_questions.created_by NOT NULL, UUID convention for reference columns |
| `DEC-2026-1023-db-actor-review-decisions` | 2026-10-23 | DB review against the three actors, owner choices: AuditLogPort for cross-module audit, Solution Review prototype follows the DB schema, all proposed seed values confirmed, matrix label, ADM0303 Q4 defaults |
| `DEC-2026-1023-open-questions-closed` | 2026-10-23 | Open questions closed: SHR0302 Q2 preview from the unsaved form, USR0401 Q13 total is all ACTIVE questions, SHR0302 Q6 rubric kept without seeded names, job lock and sweep rules |
| `DEC-2026-1023-module-docs-f-code-names` | 2026-10-23 | Module-axis documents are named F{n}_{slug}.md with the Bounded Context code, in every tier |
| `DEC-2026-1023-drop-ai-regression-run` | 2026-10-23 | Drop the AI config pre-release comparison card ("Chạy đối chiếu"): rubric is gone so nothing to compare, no backend ever designed |
| `DEC-2026-1023-student-class-screens` | 2026-10-23 | Student class screens my_classes (USR0701) and my_class_detail (USR0702), "Lớp của tôi" as 7th header nav item, no new F-code (extends F1-23 and F2-12); problem_detail editor becomes Monaco |
| `DEC-2026-1023-dd-f5-f6-open-choices` | 2026-10-23 | DD F5/F6: AI decides Mock Interview stage change with a backend turn ceiling, F6 topic and level slugs auto-generated, DD order F5 then F6 |
| `DEC-2026-1023-dd-f5-budget-lock-and-breaker` | 2026-10-23 | DD F5: budget lock exempts open Mock Interview sessions, no Resilience4j, stage turn ceiling 6 (max 18), F2-14 not blocked by the lock |
| `DEC-2026-1023-dd-f6-practice-budget-and-prompt` | 2026-10-23 | DD F6: locked budget rejects practice feedback without saving the turn, question text added to AnswerFeedbackPort, progress numerator counts ACTIVE only, ClassScopeQueryPort dropped from F6 |
| `DEC-2026-1023-dd-sync-audit-and-index` | 2026-10-23 | JOB-AI-03 auto-unlock logs only (no audit row, no system user); add index user_answers(question_id) to interview-bank |
| `DEC-2026-1024-dd-screens-users-core-contract-changes` | 2026-10-24 | Screens USR0102/USR0201: Sample gets testcaseId (F2), problem gets withdrawn flag (F4), UUID problemId in every route, STOMP client library added |
| `DEC-2026-1024-dd-screens-ai-topic-length-and-rd-trim` | 2026-10-24 | Screens USR0301/USR0302: F5 topic length raised to 60, unsupported RD blocks of the mock interview screen dropped, strengths/improvements removed from the result DTO, rate-limit wait state added |
| `DEC-2026-1024-dd-student-language-contracts` | 2026-10-24 | Reuse F4 #1 for all language limits; add F4 enabled-language list; F1 #14-15 persists defaultLanguage |
| `DEC-2026-1024-dd-student-class-contracts` | 2026-10-24 | Student classes: F1/F2 split reads, withdrawn assignments remain in progress, Redis invite-attempt limit |
| `DEC-2026-1024-dd-teacher-f1-read-contracts` | 2026-10-24 | Teacher Phase 6: three F1 read endpoints for class summaries, six-week score trend and attention students |
| `DEC-2026-1024-dd-teacher-student-list` | 2026-10-24 | Teacher student tables: one F1 paginated read across owned classes, optional class filter |
| `DEC-2026-1024-dd-student-dashboard-and-interview-navigation` | 2026-10-24 | Student dashboard: F2 level progress; mock interview: sessionId resume and selected Accepted submission pin |
| `DEC-2026-1024-dd-f2-topic-score-source` | 2026-10-24 | F2 returns one topic score vector and uses it to rank suggested problems |
| `DEC-2026-1024-dd-saved-note-explicit-save` | 2026-10-24 | Saved-problem notes use Save/Cancel; blur does not persist drafts |
| `DEC-2026-1024-dd-my-submissions-page-and-badge` | 2026-10-24 | My Submissions uses 20 rows per page and neutral difficulty badges |
| `DEC-2026-1025-dd-workspace-quick-insight-trim` | 2026-10-25 | Workspace Quick Insight omits short review comments |
| `DEC-2026-1025-dd-interview-bookmark-url` | 2026-10-25 | Preserve the bookmarked-only interview filter in the URL |
| `DEC-2026-1025-dd-email-resend-budget` | 2026-10-25 | Email-change resend counts toward the daily request limit |
| `DEC-2026-1025-dd-auth-recovery-contracts` | 2026-10-25 | Verify password-reset OTP before reset and restore self-deactivated accounts |
| `DEC-2026-1025-dd-admin-language-display-and-stack` | 2026-10-25 | Read compiler display names from F4 and omit unsupported Stack field |
| `DEC-2026-1025-dd-admin-dashboard-ten-reads` | 2026-10-25 | Admin overview has one F1 read per data block |
| `DEC-2026-1025-dd-audit-user-actions-category` | 2026-10-25 | Audit log user administration actions belong to the Permission Matrix group |
| `DEC-2026-1025-dd-restore-relogin` | 2026-10-25 | Restored self-deactivated accounts sign in again |
| `DEC-2026-1025-dd-problem-readonly-and-checklist` | 2026-10-25 | Redact reference code for other instructors and preserve published testcase minimums |
| `DEC-2026-1025-dd-queue-publish-failure-final` | 2026-10-25 | Queue publish failure immediately finalizes a pending submission |
| `DEC-2026-1025-dd-f2-generation-and-event-ledger` | 2026-10-25 | Persist testcase generation provenance and deduplicate graded events |
| `DEC-2026-1025-dd-testcase-visibility-at-grading` | 2026-10-25 | Gate historical sample output by visibility at grading and at reading |
| `DEC-2026-1025-dd-submission-testcase-snapshot` | 2026-10-25 | Grade queued submissions with the testcase bundle current when submitted |
| `DEC-2026-1025-dd-harness-internal-system-error` | 2026-10-25 | Treat generated harness compilation failures as system errors |
| `DEC-2026-1025-dd-class-metrics-after-assignment` | 2026-10-25 | Count class submissions only after the problem was assigned |
| `DEC-2026-1025-dd-system-error-ac-denominator` | 2026-10-25 | Exclude system errors from AC-rate denominators |
| `DEC-2026-1025-dd-class-assignment-time-window` | 2026-10-25 | Class progress uses submissions within each assignment period |
| `DEC-2026-1025-dd-function-spec-evaluator-common` | 2026-10-25 | Share one function signature evaluator across F2 and F3 |
| `DEC-2026-1025-dd-ai-testcase-limit-audit` | 2026-10-25 | Audit changes to the AI testcase generation limit |
| `DEC-2026-1025-dd-starter-code-unpublished-access` | 2026-10-25 | Restrict starter code for unpublished problems |
| `DEC-2026-1025-dd-run-sample-rate-limit` | 2026-10-25 | Limit Run Code to 60 calls per rolling hour per user (owner confirmed 2026-10-26) |
| `DEC-2026-1025-dd-spec-unavailable-reason` | 2026-10-25 | Distinguish unusable problem data from sandbox faults (owner confirmed 2026-10-26) |
| `DEC-2026-1025-dd-f2-event-outbox` | 2026-10-25 | Persist F2 domain events in an outbox (owner confirmed 2026-10-26) |
| `DEC-2026-1025-dd-function-signature-preview` | 2026-10-25 | F3 previews Java, C++ and Python function declarations for an unsaved authoring draft |
| `DEC-2026-1025-dd-first-failed-testcase-verdict` | 2026-10-25 | Aggregate submission verdict follows the first failed testcase by order |
| `DEC-2026-1025-dd-drop-worker-detail` | 2026-10-25 | Remove unsupported worker details from the queue monitor |
| `DEC-2026-1025-dd-go-judge-adapter-contract` | 2026-10-25 | F3 templates and F4 adapter form one go-judge request contract |
| `DEC-2026-1025-dd-unordered-multiset` | 2026-10-25 | Unordered matching preserves element counts |
| `DEC-2026-1025-dd-wrapper-runtime-stderr-hidden` | 2026-10-25 | Hide runtime stderr of function-wrapper submissions |
| `DEC-2026-1025-dd-multidimensional-array-header` | 2026-10-25 | Multidimensional array input starts with explicit `n m` |
| `DEC-2026-1025-dd-worker-consumer-autoscaler` | 2026-10-25 | Autoscale RabbitMQ grading consumers with a dedicated F4 controller |
| `DEC-2026-1026-testcase-position-snapshot` | 2026-10-26 | One testcase position order; published reorder creates a new snapshot |
| `DEC-2026-1027-qa-f3-preview-body-limit` | 2026-10-27 | F3 preview rejects body above 16 KiB with 413 HRN-062 |
| `DEC-2026-1027-qa-f2-assignment-outbox-failure` | 2026-10-27 | F2 rolls back one assignment item on Outbox failure and reports PRB-056 |
| `DEC-2026-1027-qa-f3-language-boundary` | 2026-10-27 | F3 rejects unsupported language at the raw adapter boundary |
| `DEC-2026-1027-qa-f3-missing-signature-priority` | 2026-10-27 | F3 checks missing wrapper signature before support flag |
| `DEC-2026-1027-qa-f3-cpp-user-note` | 2026-10-27 | F3 returns eligible C++ user notes as separate diagnostics |
| `DEC-2026-1027-qa-f4-all-week-label` | 2026-10-27 | F4 weekly all-range day label is Monday in UTC+7 |
| `DEC-2026-1027-qa-f1-self-impact-ack` | 2026-10-27 | F1 self-lock/self-demotion requires acknowledgeSelfImpact=true server-side |
| `DEC-2026-1027-qa-f1-attention-severity` | 2026-10-27 | F1 teacher attention list ranks severity within each status |
| `DEC-2026-1027-qa-f1-class-leave-event-retry` | 2026-10-27 | F1 class leave retry republishes event with original occurredAt |
| `DEC-2026-1027-qa-f6-history-total-attempts` | 2026-10-27 | F6 totalAttempts counts all learner attempts, independent of before cursor |
| `DEC-2026-1027-qa-f1-forgot-cooldown-response` | 2026-10-27 | F1 forgot-password cooldown returns generic 200 without resending OTP |
| `DEC-2026-1027-qa-f1-wrong-current-password` | 2026-10-27 | F1 wrong current password returns dedicated 422 IDT-115 |
| `DEC-2026-1027-qa-f6-csv-physical-line` | 2026-10-27 | F6 CSV import reports physical record start line |
| `DEC-2026-1027-qa-f6-average-half-up` | 2026-10-27 | F6 average score rounds one decimal half-up |
| `DEC-2026-1027-qa-f6-audit-failure` | 2026-10-27 | F6 administration audit failure rolls back and returns 503 INB-503 |
| `DEC-2026-1028-qa-f6-technical-limits` | 2026-10-28 | F6 bulk delete 100 IDs, question body 512 KiB, import body 5 MiB |
| `DEC-2026-1028-qa-f1-missing-permission` | 2026-10-28 | F1 missing Function:Action permission returns 403 IDT-445 |
| `DEC-2026-1028-qa-f1-role-delete` | 2026-10-28 | F1 DELETE role uses 422 IDT-421/422 and 204 plus ROLE_DELETED audit |
| `DEC-2026-1028-qa-f1-role-delete-audit-failure` | 2026-10-28 | F1 role deletion audit failure rolls back and returns 503 IDT-446 |
| `DEC-2026-1028-qa-f1-permission-immediate` | 2026-10-28 | F1 permission PATCH is immediately effective across instances via primary-DB version guard |
| `DEC-2026-1028-qa-f1-audit-export-content` | 2026-10-28 | F1 audit CSV content uses redacted action description, Accept-Language vi/en, default vi |
| `DEC-2026-1028-qa-f1-audit-admin-guard` | 2026-10-28 | F1 ADMIN-only audit reads reject non-ADMIN with 403 IDT-447 |
| `DEC-2026-1028-qa-f1-reopen-anonymized` | 2026-10-28 | F1 refuses reopening anonymized user with 422 IDT-448 |
| `DEC-2026-1028-qa-f5-anomaly-baseline-admin` | 2026-10-28 | F5 anomaly baseline includes ADMIN usage; ADMIN excluded only from alert candidates |
| `DEC-2026-1028-qa-f5-admin-audit-failure` | 2026-10-28 | F5 admin audit failure returns 503 AIR-503 and rolls back the complete mutation |

---

## SUPERSEDED

| ID | Date | Subject | Superseded by |
| --- | --- | --- | --- |
| `DEC-2026-0831-instructor-grading-round2` | 2026-08-31 | `instructor_grading`: `F5-27` amended — pre-filtered queue (AI score <6/10), side-by-side AI+manual scores, 7-day stat windows — owner confirmed all sub-decisions (Q2-Q5) individually. **Formally approved by the owner 2026-09-05** after the 2026-08-31 confirmation round was interrupted | `DEC-2026-1013-remove-instructor-manual-grading` |
| `DEC-2026-1013-grading-queue-all-accepted` | 2026-10-13 | Hàng đợi chấm tay `INS0301` lấy mọi bài `Accepted` của lớp phụ trách, bỏ điều kiện điểm AI dưới 6/10 (thay phần ngưỡng của `DEC-2026-0831-instructor-grading-round2` Q2); điểm AI chỉ là thông tin tham khảo. Điểm chấm tay chuyển sang bảng mới `ai.manual_grades` vì `solution_reviews` chỉ tồn tại khi học viên bấm xin báo cáo AI. Tiêu chí ponytail bậc 1, 6 | `DEC-2026-1013-remove-instructor-manual-grading` |
| `DEC-2026-1014-drop-admin-rubric-config` | 2026-10-14 | Bỏ phần admin cấu hình rubric chấm bài giải (F5-23): mục rubric ở `ADM0301`, bảng `rubric_configs`, trọng số, danh sách thêm/sửa/bật tắt; giữ 5 tiêu chí cố định trong báo cáo học viên gắn với phiên bản prompt/schema. Thay mục (5) của `DEC-2026-1014-instructor-batch5-6-nav-pagination-rubric`. Chủ dự án giao quyền chọn, trợ lý đề xuất, có thể đảo bằng một câu. Tiêu chí ponytail bậc 3, 2 | `DEC-2026-1014-drop-solution-review-rubric` |

---

## How to add a decision

1. Add one entry to the YAML block with `id: DEC-<YYYY>-<MMDD>-<slug>` and `status: ACTIVE`.
2. Both `decision` (what) **and** `rationale` (why) are mandatory — without the reason, nobody will dare change it three months from now.
3. Replacing an older decision: mark the old one `status: SUPERSEDED` plus `superseded_by: <new id>` and move its row to the SUPERSEDED table. Never delete it.
4. Update the ACTIVE table above.
