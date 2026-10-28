# User screen cases — USR0103 saved problems

Scope: `04-tdd/screens/users/USR0103_saved_problems.md`. DD: `03-dd/screens/users/USR0103_saved_problems.md`, §§3,7. Module-level bookmark authorization and persistence assertions require integration; all cases remain Draft / Chưa thực hiện.

Fixture aliases, independent scenario reset, role assignments and mock/integration evidence follow [fixture conventions](../FIXTURE_CONVENTIONS.md). Per-case Test data overrides file baseline.

## Shared fixtures

- `U1`: authenticated learner Alice, UTC+7.
- `B1`: saved published problem P-301 with note `Review BFS`; `B2`: saved published problem P-302 with empty note; `B3`: saved but withdrawn problem P-303 with note `old note`.
- FE mock verifies interaction only. Integration requires bookmark API/database and a second learner `U2` to confirm ownership isolation.

## Coverage

| AC | Branch | Case | Review |
|---|---|---|---|
| SP-AC-01 | populated, filtered, empty/error | TC-USR0103-SPAC01-01 | Draft |
| SP-AC-02 | save/cancel note edit | TC-USR0103-SPAC02-01 | Draft |
| SP-AC-03 | remove/undo targeted record | TC-USR0103-SPAC03-01 | Draft |
| SP-AC-04 | withdrawn problem retains note, no detail route | TC-USR0103-SPAC04-01 | Draft |
| SP-AC-05 | dirty navigation choice | TC-USR0103-SPAC05-01 | Draft |

### TC-USR0103-SPAC01-01 — Saved list filters, empty and failure states

| Field | Value |
|---|---|
| AC / DD source | SP-AC-01; DD USR0103 §§3,7 |
| Kind / priority | Normal; P2 |
| Environment | FE mock; integration for ownership and server filtering |
| Preconditions | U1 owns B1–B3; U2 owns a separate saved item. |
| Test data | 41 saved rows including B1/B2/B3, different solved states and difficulty codes; pageSize=20; progress-unavailable response has solvedCount=null and solveState=null. There is no topic filter. |
| Steps | Search BFS, filter difficulty/status, sort, page forward/back; load page=999 and inspect canonical page; repeat with progress unavailable, list 503 and empty success. |
| Expected | Only own bookmarks appear, at most 20 per page; note always displays. Filters/page persist in URL; page beyond total normalizes to final page. Progress outage hides status indicators/menu and solved-count phrase while retaining list. Empty and error/retry remain distinct; no topic parameter is sent. |
| Cleanup | Remove test bookmarks. |
| Evidence | UI captures, request params/responses, ownership query. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0103-SPAC02-01 — Save and cancel note edits

| Field | Value |
|---|---|
| AC / DD source | SP-AC-02; DD USR0103 §§3,7 |
| Kind / priority | Normal; P1, learner-authored note persistence |
| Environment | FE mock and bookmark API integration |
| Preconditions | U1 owns B1 and B2. |
| Test data | B1 stored Review BFS; B2 empty; notes of 1000 and 1001 characters; whitespace-only note; save fails once with 503. |
| Steps | Save by button and Ctrl/Cmd+Enter; independently cancel by button/Escape and blur; try each length/whitespace variant; retry after 503. |
| Expected | Only explicit save writes to target bookmark. Cancel/Escape/blur do not persist; 1000 chars accepted, 1001 blocked, whitespace normalizes to empty. Save failure keeps draft/edit mode for retry; success persists exact permitted note. |
| Cleanup | Restore original notes or delete isolated fixtures. |
| Evidence | UI before/after, API payloads, DB note values. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0103-SPAC03-01 — Remove and undo affect only the selected bookmark

| Field | Value |
|---|---|
| AC / DD source | SP-AC-03; DD USR0103 §§3,7 |
| Kind / priority | Recovery; P1 |
| Environment | Integration with bookmark API/database |
| Preconditions | U1 owns B1 and B2; no other list mutation in progress. |
| Test data | Distinct problem IDs P-301/P-302 and notes. |
| Steps | Remove B1 and B2 consecutively; use B1 toast Undo; isolate failed deletion and deletion of the last row on final page; remove withdrawn B3. |
| Expected | Each toast targets its own ID; B1 Undo restores B1 only, B2 remains removed. Removal updates rows/count optimistically; failure restores row at prior position and totals. Removing last row normalizes to last nonempty page. Withdrawn B3 can be removed with no Undo offer. Re-save does not promise restoration of deleted note. |
| Cleanup | Restore fixture rows. |
| Evidence | Timestamped UI states, API calls, DB rows. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0103-SPAC04-01 — Withdrawn problem keeps note but cannot open detail

| Field | Value |
|---|---|
| AC / DD source | SP-AC-04; DD USR0103 §§3,7 |
| Kind / priority | Boundary; P1, historical learner data |
| Environment | Integration with problem and bookmark services |
| Preconditions | U1 owns B3; P-303 is withdrawn and detail endpoint denies normal access. |
| Test data | B3 note `old note`. |
| Steps | Load saved list; inspect B3; attempt its detail action; edit and save its note. |
| Expected | B3 remains represented with `old note`; title and open-problem action are not links. Alice can edit and save the note while the problem is withdrawn. Note text containing `<script>alert(1)</script>` and `**literal**` renders verbatim as text, not executable markup. No withdrawn statement content is returned. |
| Cleanup | Restore B3 note and publication fixture. |
| Evidence | UI capture, bookmark response, detail response and DB note. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0103-SPAC05-01 — Leaving a dirty note editor requires an explicit choice

| Field | Value |
|---|---|
| AC / DD source | SP-AC-05; DD USR0103 §§3,7 |
| Kind / priority | Recovery; P1 |
| Environment | FE mock plus integration persistence check |
| Preconditions | U1 opens B1 note editor. |
| Test data | Stored `Review BFS`; unsaved draft `Review BFS and DFS`. |
| Steps | Change note; navigate to another route; choose stay; repeat and choose discard; repeat and save before leaving. |
| Expected | Guard offers Stay/Discard/Save and leave; Stay returns focus to note with draft, Discard leaves server baseline, Save navigates only after successful write. Injected save failure keeps route and exact draft, with retry; no silent navigation/write. |
| Cleanup | Restore original note. |
| Evidence | Dialog capture, route transition, API/DB values. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |
