# User screen cases — USR0503 settings

Scope: language preferences, account/device state and data export. TDD: `04-tdd/screens/users/USR0503_settings.md`; DD: `03-dd/screens/users/USR0503_settings.md`, §§3,7; F1 export and settings contracts are authoritative. Draft only.

Fixture aliases, independent scenario reset, role assignments and mock/integration evidence follow [fixture conventions](../FIXTURE_CONVENTIONS.md). Per-case Test data overrides file baseline.

## Fixtures

- Alice has one scalar defaultLanguage setting; per-case data specifies current enabled languages and saved value. Device font/autosave/Vim are local, account preferences use F1 #15. Export quotas are tracked independently per file type for five minutes; exports return files synchronously.

## Coverage

| AC | Case | Review |
|---|---|---|
| AC-SC-01 | TC-USR0503-SCAC01-01 | Draft |
| AC-SC-02 | TC-USR0503-SCAC02-01 | Draft |
| AC-SC-03 | TC-USR0503-SCAC03-01 | Draft |
| AC-SC-04 | TC-USR0503-SCAC04-01 | Draft |

### TC-USR0503-SCAC01-01 — Language choices enforce configured default limit

| Field | Value |
|---|---|
| AC / DD source | AC-SC-01; DD USR0503 §§3,7 |
| Kind / priority | Boundary; P1 |
| Environment | FE and settings API integration |
| Preconditions | F1 #14 saved defaultLanguage=JAVA; F4 #24 enabled languages CPP/PYTHON, JAVA just disabled. |
| Test data | One scalar defaultLanguage, CPP/PYTHON options; isolated F4 503. |
| Steps | Open settings; attempt save without reselecting; choose CPP and save; repeat during F4 outage. |
| Expected | Old JAVA remains readable with choose-again notice, not silently replaced. Select offers only CPP/PYTHON and successful F1 #15 writes CPP. F4 failure disables Select and unchecked-language save while other settings remain usable. No multi-language count limit is invented. |
| Cleanup | Restore Python default. |
| Evidence | UI, request and settings record. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0503-SCAC02-01 — Device and account settings track dirty state separately

| Field | Value |
|---|---|
| AC / DD source | AC-SC-02; DD USR0503 §§3,7 |
| Kind / priority | Normal; P1 |
| Environment | FE mock and settings integration |
| Preconditions | Workspace font size stored locally=14; account defaultLanguage=JAVA; CPP currently enabled. |
| Test data | Draft font size=16 and defaultLanguage=CPP; F1 #15 first returns 503, retry succeeds. |
| Steps | Save both sources; inspect dirty flags and local/server state; choose Save and leave with F1 still failing, then retry after recovery. |
| Expected | Successful local font save clears only local dirty; F1 failure retains CPP draft/account dirty and current route. Retry sends one F1 snapshot for account preferences, no legacy F5 preferences endpoint. Only successful required sources allow navigation; local save is not undone. |
| Cleanup | Restore settings. |
| Evidence | UI markers, requests and DB values. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0503-SCAC03-01 — Export enforces quota without blocking other file types

| Field | Value |
|---|---|
| AC / DD source | AC-SC-03; DD USR0503 §§3,7; F1 export contract |
| Kind / priority | Boundary; P1, data export entitlement |
| Environment | Integration with export job/quota service |
| Preconditions | Alice has submissions and interview transcripts; no export used in preceding five minutes for either type. |
| Test data | Exports are F1 #16 scope=submissions and #17 transcripts, response-supplied filename/content-type; fake clock at T0 and T0+300s. |
| Steps | Start submissions download and attempt duplicate during pending; export transcripts; retry submissions within its five-minute limit then at boundary. |
| Expected | Each API returns downloadable file synchronously; no export job is invented and payload is not rendered. Same-type pending button is locked, other export type stays usable. Too-early same-type retry shows server retry wait without extra file; explicit export works after limit. Filename/format come from response, not arbitrary client CSV/JSON options. |
| Cleanup | Remove export jobs and reset quota. |
| Evidence | API/job/quota records and UI. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0503-SCAC04-01 — Transcript export and settings load errors recover independently

| Field | Value |
|---|---|
| AC / DD source | AC-SC-04; DD USR0503 §§3,7 |
| Kind / priority | Recovery; P1 |
| Environment | FE mock and integration |
| Preconditions | Settings GET succeeds; transcript export fails; separate run settings GET fails while export succeeds. |
| Test data | Export 503, settings 503, then recovery. |
| Steps | Trigger each fault separately; retry affected operation. |
| Expected | Export error does not block settings, and settings error does not cancel/duplicate export. Each retry recovers its own resource without losing valid settings. |
| Cleanup | Restore services and remove export fixture. |
| Evidence | UI, request and job logs. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |
