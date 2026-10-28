# User screen cases — USR0502 profile

Scope: learner profile display/edit and email verification. TDD: `04-tdd/screens/users/USR0502_profile.md`; DD: `03-dd/screens/users/USR0502_profile.md`, §§3,7; F1 owns account and email state. Draft only.

Fixture aliases, independent scenario reset, role assignments and mock/integration evidence follow [fixture conventions](../FIXTURE_CONVENTIONS.md). Per-case Test data overrides file baseline.

## Fixtures

- Alice account U-502: display name `Alice Nguyen`, email `alice@example.test`, verified; profile API sections load independently. Candidate new email `alice.new@example.test`; OTP service can succeed, reject, or time out.

## Coverage

| AC | Case | Review |
|---|---|---|
| PF-AC-01 | TC-USR0502-PFAC01-01 | Draft |
| PF-AC-02 | TC-USR0502-PFAC02-01 | Draft |
| PF-AC-03 | TC-USR0502-PFAC03-01 | Draft |
| PF-AC-04 | TC-USR0502-PFAC04-01 | Draft |

### TC-USR0502-PFAC01-01 — Profile sections load and retry independently

| Field | Value |
|---|---|
| AC / DD source | PF-AC-01; DD USR0502 §§3,7 |
| Kind / priority | Abnormal; P1 |
| Environment | FE mock and integration |
| Preconditions | Alice opens profile; identity succeeds, preferences section fails 503. |
| Test data | F1 #9 identity success; F1 #47/F2 level progress and F5 #13 interview summary faults independently; null interview score. |
| Steps | Load profile; retry preferences; repeat with identity failure. |
| Expected | Healthy identity/practice/interview sections remain visible independently. F5 interview failure shows dashes rather than whole-page error or zero score; only failed source is retried and no fake identity is shown. |
| Cleanup | Restore endpoint. |
| Evidence | UI and requests. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0502-PFAC02-01 — Save error preserves profile draft

| Field | Value |
|---|---|
| AC / DD source | PF-AC-02; DD USR0502 §§3,7 |
| Kind / priority | Recovery; P1 |
| Environment | FE mock and profile API integration |
| Preconditions | Alice opens editable profile; save endpoint returns 500. |
| Test data | New display name `Alice N.`; original `Alice Nguyen`. |
| Steps | Save permitted profile fields successfully; Undo another draft; try invalid displayName and submit valid draft with server 503 then retry. |
| Expected | Only editable fields are sent and success updates identity card. Undo restores saved values without write. Invalid fields block request with associated feedback; 503 retains exact draft and server baseline; explicit retry persists once. |
| Cleanup | Restore original name. |
| Evidence | UI, request payloads, DB value. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0502-PFAC03-01 — New email is not active before verification

| Field | Value |
|---|---|
| AC / DD source | PF-AC-03; DD USR0502 §§3,7; F1 email verification contract |
| Kind / priority | Security; P1 |
| Environment | Integration with OTP/email stub |
| Preconditions | Alice current email verified; no pending change. |
| Test data | Candidate alice.new@example.test, OTP 582941/wrong 000000; cooldown boundary 59s/60s; change-email request/resend total three per rolling 24h; profile name draft Alice N. |
| Steps | Save profile and request email with independent success/failure combinations; verify wrong/right code; resend at boundaries and fourth rolling-24h attempt; cancel OTP dialog. |
| Expected | Profile and email outcomes are independent. Login email stays old until correct verification; wrong OTP keeps dialog. Resend obeys 60s and shared three/24h limit. Cancel keeps old login email and unsaved profile/email draft; valid verification activates candidate once. |
| Cleanup | Restore account email and remove OTP fixture. |
| Evidence | UI, OTP events, account DB state. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0502-PFAC04-01 — Dirty profile navigation requires save/discard choice

| Field | Value |
|---|---|
| AC / DD source | PF-AC-04; DD USR0502 §§3,7 |
| Kind / priority | Recovery; P1 |
| Environment | FE mock plus integration persistence |
| Preconditions | Alice changes display name without saving. |
| Test data | Stored `Alice Nguyen`; draft `Alice N.` |
| Steps | Dirty profile and pending candidate email; use Dashboard/Settings links with Save and leave. Fail profile save once; next run save profile while email awaits OTP; finish OTP then retry exit. |
| Expected | Guard offers explicit choices; Stay keeps draft, Discard does not write. Save and leave never navigates while profile save fails or email verification remains pending; successful profile plus completed/cancelled email flow permits exit. No silent email activation. |
| Cleanup | Restore name. |
| Evidence | Dialog, route, API and DB value. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |
