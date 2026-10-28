# User screen cases — USR0601 dashboard

Scope: learner dashboard metrics, time ranges and activity navigation. TDD: `04-tdd/screens/users/USR0601_dashboard.md`; DD: `03-dd/screens/users/USR0601_dashboard.md`, §§3,7. F2/F4 own source metrics; dashboard cases verify display and links. All Draft.

Fixture aliases, independent scenario reset, role assignments and mock/integration evidence follow [fixture conventions](../FIXTURE_CONVENTIONS.md). Per-case Test data overrides file baseline.

## Fixtures

- Alice has dashboard blocks: summary, skill score/recommendations, activity grid and topic table. Historical submissions on 2026-10-20 and 2026-10-27; one withdrawn problem P-601. Clock UTC+7. Each block endpoint independently controllable.

## Coverage

| AC | Case | Review |
|---|---|---|
| DB-AC-01 | TC-USR0601-DBAC01-01, -02 | Draft |
| DB-AC-02 | TC-USR0601-DBAC02-01 | Draft |
| DB-AC-03 | TC-USR0601-DBAC03-01 | Draft |
| DB-AC-04 | TC-USR0601-DBAC04-01 | Draft |
| DB-AC-05 | TC-USR0601-DBAC05-01 | Draft |

### TC-USR0601-DBAC01-01 — Dashboard blocks fail and retry independently

| Field | Value |
|---|---|
| AC / DD source | DB-AC-01; DD USR0601 §§3,7 |
| Kind / priority | Abnormal; P1 |
| Environment | FE mock and integration |
| Preconditions | Summary succeeds; activity endpoint 503; other blocks succeed. |
| Test data | Distinct response fixture per block. |
| Steps | Load; retry activity; repeat with summary failure. |
| Expected | Healthy blocks remain visible; only failed block displays error/retry; no totals synthesized from partial sources. |
| Cleanup | Restore endpoints. |
| Evidence | UI and request traces. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0601-DBAC02-01 — Range switch changes only period metrics

| Field | Value |
|---|---|
| AC / DD source | DB-AC-02; DD USR0601 §§3,7 |
| Kind / priority | Normal; P1 |
| Environment | FE and integration with fixed clock |
| Preconditions | Alice dashboard at default range; summary includes all-time value. |
| Test data | Supported ranges all, 7d and 30d; UTC+7; shared topic-progress query supplies table/chart. |
| Steps | Switch all?7d?all rapidly; resolve newest before delayed older response; count shared-source calls. |
| Expected | Only period-bound F4 metrics and F5 interview summary use selected range; cumulative F1/F2/F6 values and fixed 7d streak stay unchanged. At range=all radar/table share one query key and one F4 request. Prior response cannot overwrite latest range. |
| Cleanup | Restore range and clock. |
| Evidence | URLs/query payloads and rendered values. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0601-DBAC03-01 — Skill score and recommendation use the same result snapshot

| Field | Value |
|---|---|
| AC / DD source | DB-AC-03; DD USR0601 §§3,7; F4 metric source |
| Kind / priority | Data integrity; P1 |
| Environment | Integration |
| Preconditions | Alice has fixed accepted/nonaccepted result set; dashboard snapshot version V1. |
| Test data | Skill values Array=80, Graph=60, DP=null; second fixture has only two scored topics; greeting and recommendation use this same response. |
| Steps | Load dashboard; inspect score and linked recommendations; introduce delayed stale response. |
| Expected | Greeting, skill/ranking and recommendations use same server scores/order; missing DP is unknown, not zero. With fewer than three scored topics radar explains insufficient data rather than drawing fake zero vertices. Delayed stale response does not mix snapshots. |
| Cleanup | Remove snapshot fixture. |
| Evidence | Versioned API payloads and UI. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0601-DBAC04-01 — Withdrawn problem action has no dead link

| Field | Value |
|---|---|
| AC / DD source | DB-AC-04; DD USR0601 §§3,7 |
| Kind / priority | Boundary; P1 |
| Environment | Integration |
| Preconditions | Dashboard F4 in-progress list is loaded for Alice. |
| Test data | Run A returns ordered P-601 (`withdrawn=true`), P-602 (`withdrawn=false`), P-603 (`withdrawn=false`); run B returns only withdrawn/null-problem records among the first three. |
| Steps | Load dashboard and activate Continue in both runs. |
| Expected | Continue chooses the first usable record among F4's first-three response, P-602, and routes to `/workspace/P-602`; it never routes to withdrawn P-601. If all first three are unusable, Continue remains enabled and routes to `/problems` with the start-solving label. No stale route or toast. |
| Cleanup | Restore recommendation/problem. |
| Evidence | UI, link target and API response. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0601-DBAC05-01 — Activity grid and topic table avoid false data

| Field | Value |
|---|---|
| AC / DD source | DB-AC-05; DD USR0601 §§3,7 |
| Kind / priority | Data integrity; P1 |
| Environment | Integration with fixed clock |
| Preconditions | Alice has activity on Oct 20 only; topic table contains Array results. |
| Test data | Complete 364-day grid includes Oct20=2 and Oct21=0 UTC+7; isolated response misses one day; topic solved counts succeed while acceptance rate is null/unavailable. |
| Steps | Inspect tooltip for each boundary date; remove one grid date and render; fail only topic acceptance ratio. |
| Expected | Complete grid preserves 364 dates and local labels/counts. Missing-day response makes grid show its own error, never fills unknown with zero or shifts cells. Topic table keeps known solved counts and displays unknown acceptance ratio as dash; no fabricated zeros. |
| Cleanup | Restore data and clock. |
| Evidence | Aggregation response, UI values and timestamps. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0601-DBAC01-02 — F5 outage degrades only AI dashboard blocks

| Field | Value |
| :--- | :--- |
| AC / DD source | DB-AC-01; DD USR0601 §3, F5 blocks and T-DB-23; F5 degradation contract |
| Kind / priority | Abnormal; P1, protects non-AI dashboard while AI is unavailable |
| Environment | FE mock and integration with F1/F2/F4/F5/F6 endpoints instrumented |
| Preconditions | Alice has successful non-AI summary, activity, topic and class data. Both F5 dashboard endpoints time out; retry later returns successful empty AI data. |
| Test data | F1/F2/F4/F6 fixtures populated; F5 endpoints timeout, then `completedSessionCount=0`, `averageScore=null`, `weakestCriterionCode=null`. |
| Steps | Open `/dashboard`; inspect all blocks and error regions; retry each failed F5 block independently; resolve each retry. |
| Expected | Non-AI blocks remain visible and usable during F5 outage. Each failed AI block shows its own degraded/error state; retry requests only its failed endpoint. Null/empty AI values use the DD empty representation and are not coerced to fabricated score zero. Recovery updates only the retried block. |
| Cleanup | Restore endpoints and clear dashboard cache. |
| Evidence | UI captures, endpoint request counts, returned payloads and console scan. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |
