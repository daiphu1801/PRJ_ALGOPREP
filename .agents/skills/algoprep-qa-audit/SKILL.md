---
name: algoprep-qa-audit
description: Audit an existing AlgoPrep QA testcase cluster against its TDD ACs and DD contracts, using a small structural checker and a focused branch/oracle review. Use for testcase quality review and design closure, not for executing tests or learner grading data.
---

# AlgoPrep testcase audit

Audit one risk cluster at a time. Keep testcase design, execution evidence and code review separate. The source of truth is the current AC body, its DD contract, and active owner decisions; the case text is the object being audited.

## Small batch workflow

1. Select one case file or tightly related cluster from `04-tdd/testcases/CHECKLIST.md`. Read only its TDD AC bodies/trace rows, cited DD sections and relevant decisions. Use `rg -n` to locate them; avoid loading all modules or all cases.
2. Run `python .agents/skills/algoprep-qa-audit/scripts/audit_case_cluster.py --cases <case-file> --tdd <tdd-file> --ac <AC-ID>` (repeat `--ac` for the selected scope). Fix missing/empty fields, duplicate IDs, missing trace links and wrong link targets before semantic review. The script checks structure, not business correctness.
3. First compare each clause in the AC body with the case Steps/Expected; a matching ID or title does not establish coverage. Then compare each independent Normal, Abnormal, Boundary, Security, Concurrency and Recovery branch that applies in DD with the cases. Check concrete actor/fixture, exact endpoint/input, clock or fault order, response and forbidden side effects, cleanup and evidence. Trace actor → assigned role/owner → permission again after a mutation; do not transfer another role's permission to that actor. For an “immediate” or “after commit” guarantee that depends on async delivery, hold the delivery and probe in the gap; a wait-for-delivery barrier cannot prove the stronger guarantee. Distinguish a branch omitted from a branch covered by a shared fixture. Flag unresolved contracts with exact source citations; ask the owner only when authoritative material cannot settle pass/fail.
4. Edit only the affected DD/TDD/cases and decision registry, preserve existing IDs, and add a new suffix ID for a new branch. Re-run the checker and update the module/screen README, audit report and checklist with actual counts and remaining exceptions. Leave execution status unrun until a build and evidence exist.
5. Treat one batch as complete when structural checks pass and each applicable branch has either a reviewable oracle or a named unresolved decision. Mark design closure only when no unresolved design branch remains. Do not infer product pass from case count or a FE mock.

When reviewing a smoke/E2E selection, validate its shared fixture against every participating module's invariants (including publication thresholds), declare which source-case fixtures are replaced, and order journeys so their setup/cleanup preserves later preconditions. A linked source ID does not make an incompatible shared fixture valid. Check required services against every selected smoke case, even when an external provider is stubbed.

Recompute numeric/date oracles from the actual fixture and DD formula, including population, rounding, timezone, pagination and sort order. Place fault hooks after the last prerequisite commit required to reach the target branch; a provider fault cannot exercise a validation rejection that occurs before the provider is called.

For event/cache or degraded-service cases, distinguish replay of the same aggregate from a new aggregate with a cache hit, and distinguish provider failure from module/port failure. These paths can differ in persistence, quota/refund and emitted events even when the UI shows the same error.

## Efficient reporting

Write a short batch record: selected ACs and case IDs, structural result, semantic findings and corrections, unresolved oracles, execution dependencies, and the next cluster. Keep source links near findings. Reuse the [case format](../algoprep-qa-testcases/references/case-format.md) when editing a case.

For repeat audits, compare current input hashes with [the last reviewed source baseline](../../../04-tdd/testcases/AUDIT_SOURCE_BASELINE.json). Revisit changed contracts/cases and the clusters citing them; reuse unchanged completed review records. Missing/new files and changed owner decisions require scope review. Refresh the baseline only after recording the completed scope and exceptions; hashes alone do not establish review or execution.

Improve this skill only when a real batch reveals a repeatable workflow problem. Record the observed failure and the change in that batch's audit report, then rerun the same batch. Avoid adding rules for one-off domain decisions.

This skill designs and audits QA documents. It does not run application tests or change product code symbols. Follow `AGENTS.md` impact gates when maintaining its checker, and impact/change detection gates for later product code changes.
