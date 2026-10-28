# QA case document format

Use one file for one module or screen risk cluster. A case ID is stable after review: `TC-F2-AC93-01` for a module or `TC-SHR0202-PAAC02-01` for a screen. Add a suffix number for a new branch; never renumber IDs already referenced by TDD or a defect.

## File header

- Scope and TDD file.
- DD sources and decision version/date used for the oracle.
- Fixture catalog shared by the cases in this file, with exact values, account roles, clock assumptions and external-service states.
- Coverage table: AC → branch → case ID → design review state. Keep execution results in a separate run record.

## Case block

```markdown
### <CASE-ID> — <one observable branch>

| Field | Value |
| :--- | :--- |
| AC / DD source | <exact AC ID and DD section or rule> |
| Kind / priority | <Normal, Abnormal, Boundary, Security, Concurrency or Recovery>; <P0/P1/P2 with reason> |
| Environment | <FE mock, API, integration, E2E, job; required services> |
| Preconditions | <state before action, including actor/permission/time/version> |
| Test data | <fixture IDs and concrete values; no “valid data” placeholder> |
| Steps | <ordered actions and any fault injection or clock change> |
| Expected | <observable response/state for each important step; include forbidden side effects> |
| Cleanup | <how to reset fixture, or why isolated data can be retained> |
| Evidence | <response, UI capture, DB query, event/job log or metric needed at execution> |
| Design review | Draft / Reviewed / Oracle blocked |
```

If a case combines two actions with independent outcomes, split it. For rejected writes, verify both the error and the absence of partial records or events. For calculations, state numerator, denominator, empty set and time window when relevant. For asynchronous cases, define event order, timeout and final-state observation instead of “eventually succeeds.” For hidden learner grading testcases, inspect all user-visible surfaces, including logs and accessibility text where applicable.

Do not mark a case Passed or Failed in this design file before execution. A later run record needs case ID, build, environment, run time, actual result, evidence link and defect ID when it fails.
