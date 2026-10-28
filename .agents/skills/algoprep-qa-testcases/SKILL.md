---
name: algoprep-qa-testcases
description: Design or review AlgoPrep QA test cases from the current TDD acceptance criteria and DD contracts, with concrete data, observable oracles, branch coverage, and traceability. Use for requests to write, generate, audit, or extend QA test case documents. Do not use for learner grading testcases or for running/automating test suites.
---

# AlgoPrep QA test cases

Use this skill for the **QA test case documents** under `04-tdd/testcases/`. The word `testcase` in F2/F4 also means learner input/output used by the judge; keep the two meanings separate.

## Scope and source

- Read `04-tdd/testcases/README.md` for the current plan, naming and milestone rules. Resolve the requested module/screen through `04-tdd/overview.md`; work in a coherent risk cluster instead of loading all 552 AC at once.
- For each AC in scope, read its full body and trace row, then the cited DD API/logic/validation/jobs or DD screen test viewpoints. Consult BD/RD and active decisions when DD does not settle the oracle. Do not use prototype behavior to create a business rule.
- Preserve existing AC and test case IDs. Put module cases in `04-tdd/testcases/modules/<module-code>_<module-name>/`, one file per risk cluster; update that folder's `README.md` and the TDD trace links. Put screen cases in `04-tdd/testcases/screens/<group>/`, mirroring TDD ownership. Use [case format](references/case-format.md) when creating or reviewing a case document.

## Design and review

1. Make a branch inventory for the selected ACs: normal, abnormal, boundary, security, concurrency, recovery and degraded behavior where the contract has those branches. One AC may require several independent cases. Reuse fixture definitions across cases, but keep each case's stimulus and pass/fail oracle explicit.
2. Give every case a stable ID, AC/DD source, priority tied to risk, environment needed, exact preconditions and fixture values, action steps, expected observations at the relevant UI/API/DB/event/job surfaces, cleanup and evidence to retain. Assert prohibited side effects as well as positive results.
3. If sources conflict or do not distinguish pass from fail, record the exact question and citations, fix the source only when the answer already follows from authoritative material, and leave that branch unapproved until the oracle is settled. Do not fill a gap with a guess or a mock response.
4. Review both directions: each case points to real AC/DD, and every independent risk branch in the selected ACs has a case. After review, add stable IDs to the corresponding TDD trace rows. Keep execution status `Chưa thực hiện` until a real run with evidence; distinguish FE mock results from integration/E2E results.
5. Report the selected ACs, cases created or reviewed, branch gaps, unresolved oracles and execution dependencies. Do not claim overall coverage from the number of case files.

## Boundaries

- Designing QA cases does not require completed coding. Do not create automated test code or execute suites unless the user asks for that step.
- Class diagrams are optional context for ambiguous object relations. They are not a prerequisite for test case generation and cannot supply a missing business oracle. For lifecycle or asynchronous behavior, prefer DD state/sequence information when available; request a new diagram only when a concrete test branch cannot be understood without it.
- For code edits later, follow `AGENTS.md` impact and change-detection gates. This skill itself does not authorize code changes or a commit.
