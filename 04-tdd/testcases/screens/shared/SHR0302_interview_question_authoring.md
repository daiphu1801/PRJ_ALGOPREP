# QA test cases — shared interview question authoring (SHR0302)

Scope: `04-tdd/screens/shared/SHR0302_interview_question_authoring.md`; DD §§2–7, F6 §§9–23 and validation F6. Draft design; not executed.

Fixture aliases, independent scenario reset, role assignments and mock/integration evidence follow [fixture conventions](../FIXTURE_CONVENTIONS.md). Per-case Test data overrides file baseline.

## Fixtures

A3 author admin-23 has create/update/delete permission. Existing question UUID aaaaaaaa-1111-4111-8111-111111111111, displayNo 42; create returns UUID bbbbbbbb-2222-4222-8222-222222222222, displayNo 43. Topic cccccccc-3333-4333-8333-333333333333, level dddddddd-4444-4444-8444-444444444444. Title `Explain idempotency`; follow-up `What can go wrong?`; valid rubric `Correctness:60, Tradeoffs:40`; invalid rubrics total 99 or 101; duplicate names `Clarity`, `clarity`. Save endpoint can return 503 once.

## Coverage

| AC | Branch | Cases |
|---|---|---|
| IQA-AC-01 | unsaved preview, create UUID route, edit identity | TC-SHR0302-IQAAC01-01, -02 |
| IQA-AC-02 | empty/valid rubric; invalid sums/duplicates | TC-SHR0302-IQAAC02-01, -02 |
| IQA-AC-03 | save failure/dirty guard; delete cancel/confirm | TC-SHR0302-IQAAC03-01, -02 |

### TC-SHR0302-IQAAC01-01 — Preview current unsaved content without persistence

| Field | Value |
|---|---|
| AC / DD source | IQA-AC-01; DD BR-SC-01, IQ-A-02 |
| Kind / priority | Normal, Security; P1 (draft loss or unsafe content rendering) |
| Environment | Browser FE mock; F6/F5 request spy |
| Preconditions | New form opened; categories loaded. |
| Test data | Title Explain idempotency; Markdown `<img src=x onerror=alert(1)>`; follow-up above; rubric 60/40. |
| Steps | Fill fields; open preview before save; close with Escape. |
| Expected | Preview reflects current unsaved values; dangerous HTML is escaped/sanitized; no script executes. No F6 create/update or F5 request. Closing restores opener focus and preserves fields. |
| Cleanup | Discard form. |
| Evidence | Network log, DOM snapshot, browser console, field/focus capture. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0302-IQAAC01-02 — Create uses returned UUID; edit preserves existing UUID

| Field | Value |
|---|---|
| AC / DD source | IQA-AC-01; DD IQ-A-01/04, BR-SC-02 |
| Kind / priority | Normal; P1 (wrong question identity) |
| Environment | Browser + F6 integration |
| Preconditions | Valid new form and existing edit form. |
| Test data | Create UUID bbbbbbbb-2222-4222-8222-222222222222, displayNo 43; existing UUID aaaaaaaa-1111-4111-8111-111111111111. |
| Steps | Save new question; edit existing title and save. |
| Expected | Successful POST navigates to `/admin/interview-questions/<new UUID>/edit`; display IQ-043 is not route key. PUT uses existing UUID and remains on that edit route. Dirty clears only after success. |
| Cleanup | Soft-delete created records. |
| Evidence | Requests/responses, URLs, persisted records. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0302-IQAAC02-01 — Empty rubric and exact 100 total save

| Field | Value |
|---|---|
| AC / DD source | IQA-AC-02; DD BR-SC-01, IQ-A-03 |
| Kind / priority | Boundary; P1 (incorrect rubric validation) |
| Environment | FE mock + F6 integration |
| Preconditions | Valid title, topic and level. |
| Test data | Variant A rubric empty; variant B criteria 60 and 40. |
| Steps | Save each variant separately. |
| Expected | Both accepted per DD; persisted rubric exactly matches submitted values and no default criterion appears. |
| Cleanup | Soft-delete created records. |
| Evidence | Payload, response and persisted DTO. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0302-IQAAC02-02 — Invalid rubric total or duplicate names blocks write

| Field | Value |
|---|---|
| AC / DD source | IQA-AC-02; DD validation table, IQ-A-03 |
| Kind / priority | Boundary; P1 (invalid rubric persisted) |
| Environment | FE mock; network spy |
| Preconditions | Form otherwise valid. |
| Test data | Rubric totals 99 then 101; duplicate case-insensitive names Clarity/clarity. |
| Steps | Attempt save for each invalid variant; then correct to unique 60/40 and save. |
| Expected | Each invalid form identifies total/duplicate field; zero POST/record/event for invalid attempts. Corrected values permit save. |
| Cleanup | Delete corrected record. |
| Evidence | Validation messages and mutation call count. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0302-IQAAC03-01 — Save failure preserves draft and leave guard

| Field | Value |
|---|---|
| AC / DD source | IQA-AC-03; DD state table, IQ-A-05 |
| Kind / priority | Abnormal, Recovery; P1 (draft loss) |
| Environment | Browser + F6 integration with one injected 503 |
| Preconditions | Existing edit is dirty; server baseline title is Original. |
| Test data | Draft title Explain retry safety; save returns 503. |
| Steps | Save; attempt internal navigation and choose Stay; retry navigation and choose Leave. |
| Expected | 503 INB-503 maps via apiError.INB-503, draft remains dirty and save unlocks; no success toast. Stay preserves route/draft; Leave navigates without PUT. Integration audit-fault variant confirms server baseline remains Original and no question/audit partial write. |
| Cleanup | Restore fixture. |
| Evidence | Response, dialog, form state and DB baseline. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0302-IQAAC03-02 — Delete cancel sends nothing; confirmation soft-deletes once

| Field | Value |
|---|---|
| AC / DD source | IQA-AC-03; DD BR-SC-02, IQ-A-06, F6 §23 |
| Kind / priority | Boundary; P1 (unintended destructive action) |
| Environment | Browser + F6 integration |
| Preconditions | Existing question and authorized actor. |
| Test data | UUID aaaaaaaa-1111-4111-8111-111111111111. |
| Steps | Open dialog and cancel; reopen and confirm. |
| Expected | Cancel/Escape closes, restores focus, zero delete calls. Confirm sends one UUID to soft-delete endpoint; on success returns to list and history remains. |
| Cleanup | Restore fixture lifecycle. |
| Evidence | Request count/ID, DB status/history, route and focus. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |
