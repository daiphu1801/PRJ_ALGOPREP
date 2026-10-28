# User screen cases — USR0701 my classes

Scope: learner class list and invitation entry. TDD: `04-tdd/screens/users/USR0701_my_classes.md`; DD: `03-dd/screens/users/USR0701_my_classes.md`, §§3,7; F1 class membership is server oracle. Draft only.

Fixture aliases, independent scenario reset, role assignments and mock/integration evidence follow [fixture conventions](../FIXTURE_CONVENTIONS.md). Per-case Test data overrides file baseline.

## Fixtures

- Alice is active member of class C-701; C-702 is another class. Assignment service can fail independently. Invite code `JOIN-701-ABCD` is valid; `JOIN-BAD-0000` invalid. Class C-703 invitation response can conflict as already joined.

## Coverage

| AC | Case | Review |
|---|---|---|
| AC-SC-01 | TC-USR0701-SCAC01-01 | Draft |
| AC-SC-02 | TC-USR0701-SCAC02-01 | Draft |
| AC-SC-03 | TC-USR0701-SCAC03-01 | Draft |

### TC-USR0701-SCAC01-01 — Class list remains usable when assignment block fails

| Field | Value |
|---|---|
| AC / DD source | AC-SC-01; DD USR0701 §§3,7 |
| Kind / priority | Abnormal; P1 |
| Environment | FE mock and integration |
| Preconditions | Class list returns C-701/C-702; assignments endpoint 503. |
| Test data | Four classes C-701..704; F1 #25A success, F2 #13A 503 then healthy x/y including withdrawn assignments; isolate F1 503. |
| Steps | Load class page; inspect cards; retry assignment block. |
| Expected | All four class cards/name/schedule/instructor/links remain while F2 progress phrases/bars are hidden with no false no-assignment claim. Healthy F2 restores x/y including withdrawn denominator. F1 failure replaces list with error but invite-code form remains usable. |
| Cleanup | Restore endpoint. |
| Evidence | UI and request traces. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0701-SCAC02-01 — Invitation updates membership only after server success

| Field | Value |
|---|---|
| AC / DD source | AC-SC-02; DD USR0701 §§3,7; F1 join-class contract |
| Kind / priority | Security; P1 |
| Environment | Integration |
| Preconditions | Alice is not member of target class; join endpoint controllable. |
| Test data | Empty/whitespace code; lower-case code with spaces; IDT-410; transport fault; already-member success. |
| Steps | Submit each independently; delay successful response and double-click; inspect normalized payload, input, toast and refetches. |
| Expected | Empty trimmed code sends no request. Valid code is trimmed/uppercased; pending disables Submit; only success clears input, names class from response and refetches both F1/F2. IDT-410 versus transport failure uses different feedback, retains exact user input and refetches neither source. Already-member success is idempotent, no duplicate membership. |
| Cleanup | Remove Alice membership and invite fixture. |
| Evidence | UI state, API response and membership query. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-USR0701-SCAC03-01 — Class card routes to matching class detail

| Field | Value |
|---|---|
| AC / DD source | AC-SC-03; DD USR0701 §§3,7 |
| Kind / priority | Normal; P1 |
| Environment | FE mock and integration |
| Preconditions | Alice sees C-701 and C-702. |
| Test data | Distinct class IDs/names and memberships. |
| Steps | Type an unsent invitation code; activate second class detail by mouse and keyboard; return Back. |
| Expected | Link uses second class UUID at /classes/{classId} and accessible name includes class name. Unsent invite input triggers no leave-confirm dialog; no cross-class response/data appears. |
| Cleanup | Clear route state. |
| Evidence | URLs and class API responses. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |
