# QA test cases — shared interview question info (SHR0303)

Scope: `04-tdd/screens/shared/SHR0303_interview_question_info.md`; DD §§2–7, F6 §§9–10,20, decision `DEC-2026-1022-interview-bank-schema-sync`. Design only; not executed.

Fixture aliases, independent scenario reset, role assignments and mock/integration evidence follow [fixture conventions](../FIXTURE_CONVENTIONS.md). Per-case Test data overrides file baseline.

## Fixtures

Valid question UUID `aaaaaaaa-1111-4111-8111-111111111111`, displayNo `42` (`IQ-042`), ACTIVE; retired UUID `bbbbbbbb-2222-4222-8222-222222222222`; malformed ID `IQ-042`; missing UUID `cccccccc-3333-4333-8333-333333333333`. Successful DTO: title `Explain idempotency`, contentMarkdown null, followUps/rubric empty, usageCount 0, averageScorePercent null, practiceAvailable false. A2 read-only role lacks UPDATE; separate no-READ response 403; topic/level queries can independently return 503.

## Coverage

| AC | Branch | Cases |
|---|---|---|
| IQI-AC-01 | valid UUID/display label, malformed/missing/retired not found | TC-SHR0303-IQIAC01-01, -02 |
| IQI-AC-02 | read/update authorization and null/percent rendering | TC-SHR0303-IQIAC02-01, -02 |
| IQI-AC-03 | saved preview focus and independent taxonomy retry | TC-SHR0303-IQIAC03-01, -02 |

### TC-SHR0303-IQIAC01-01 — Load by UUID; display number is never the route key

| Field | Value |
|---|---|
| AC / DD source | IQI-AC-01; DD state table, IQ-01, BR-SC-01 |
| Kind / priority | Normal; P1 (wrong record/navigation) |
| Environment | FE mock + F6 integration |
| Preconditions | A3 has READ/UPDATE; active UUID fixture exists. |
| Test data | UUID `aaaaaaaa-1111-4111-8111-111111111111`; displayNo 42. |
| Steps | Open `/admin/interview-questions/<UUID>` and inspect requests and rendered identity. |
| Expected | Exactly one F6 §20 detail request uses UUID. Display label is `IQ-042`, never used in path/query. Returned saved title renders; no mocked alternate question. |
| Cleanup | Clear session. |
| Evidence | Request URL/count, response, page heading and browser URL. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0303-IQIAC01-02 — Invalid, missing and retired questions share 404 state

| Field | Value |
|---|---|
| AC / DD source | IQI-AC-01; DD state table, IQ-02 |
| Kind / priority | Abnormal, Security; P1 (enumeration/incorrect fallback) |
| Environment | FE + F6 integration |
| Preconditions | Retired ID returns `404 INB-010`; missing ID returns same; malformed `IQ-042` is rejected locally. |
| Test data | Retired `bbbbbbbb-2222-4222-8222-222222222222`; missing `cccccccc-3333-4333-8333-333333333333`; malformed `IQ-042`. |
| Steps | Visit all three routes; inspect request counts and visible output. |
| Expected | Identical not-found state and no title/rubric/usage. Malformed ID makes no F6 call; retired and missing map to same `INB-010` presentation. |
| Cleanup | None. |
| Evidence | Response bodies/statuses, request log, screenshot comparison. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0303-IQIAC02-01 — READ denial hides all question details

| Field | Value |
|---|---|
| AC / DD source | IQI-AC-02; DD state table, IQ-03, BR-SC-02 |
| Kind / priority | Security; P0 (unauthorized content exposure) |
| Environment | Browser + F6 integration |
| Preconditions | Actor is authenticated but lacks INTERVIEW_BANK_MANAGEMENT:READ; detail request returns 403 `INB-301`. |
| Test data | DTO title `Explain idempotency`; no question content should be returned. |
| Steps | Open valid UUID detail; inspect DOM, accessibility tree and network response. |
| Expected | Unauthorized state only; title, content, follow-ups, rubric, usage and preview are absent. No client mock data leaks. |
| Cleanup | Clear session. |
| Evidence | Permission claims, 403 response, DOM snapshot. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0303-IQIAC02-02 — UPDATE denial and nullable score/content render correctly

| Field | Value |
|---|---|
| AC / DD source | IQI-AC-02; DD IQ-03–05, state table, BR-SC-01/02 |
| Kind / priority | Security, Boundary; P1 (unauthorized mutation or false metric) |
| Environment | FE mock + browser |
| Preconditions | Read permitted, UPDATE absent; successful DTO as listed in fixtures. |
| Test data | `contentMarkdown=null`, empty followUps/rubric, count 0, score null. Second DTO variant has averageScorePercent 87.5. |
| Steps | Render both variants; inspect Edit action and score labels. |
| Expected | Edit absent without UPDATE and server still protects direct write. Null content and empty rubric/followups have truthful empty states; null score dash, count0 remains0. 87.5 uses percent scale, never /5. A newly created level outside seed values renders neutral badge rather than undefined/error. |
| Cleanup | None. |
| Evidence | DTOs, DOM/accessibility tree, visible score text. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0303-IQIAC03-01 — Preview uses saved DTO and restores focus

| Field | Value |
|---|---|
| AC / DD source | IQI-AC-03; DD IQ-06, BR-SC-02 |
| Kind / priority | Normal; P2 (preview integrity/accessibility) |
| Environment | Browser + F6 integration |
| Preconditions | Question detail loaded successfully; user can READ. |
| Test data | Saved title `Explain idempotency`; saved follow-up `Give an example`; practiceAvailable=false. |
| Steps | Activate “View as learner”; switch preview mode; close with Escape. |
| Expected | Preview uses saved DTO, no F6/F5 preview request, displays practice-unavailable state, not editor draft. Escape closes dialog and returns focus to opener; page and scroll remain. |
| Cleanup | Close dialog if still open. |
| Evidence | Request log, screenshot, focus target before/after. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |

### TC-SHR0303-IQIAC03-02 — Taxonomy failure does not mask detail and retries independently

| Field | Value |
|---|---|
| AC / DD source | IQI-AC-03; DD state table, IQ-07 |
| Kind / priority | Abnormal, Recovery; P2 (independent query degradation) |
| Environment | FE + F6 integration with topic/level fault injection |
| Preconditions | Detail §20 succeeds; topic request returns 503; level request succeeds. |
| Test data | Detail fixture from TC-01; topic query fails once then succeeds. |
| Steps | Open detail; activate retry for topic classification only. |
| Expected | Question remains visible, topic label says unavailable with retry; level label remains valid. Retry calls topic endpoint only and updates label; detail is neither refetched nor replaced by not-found. |
| Cleanup | Remove fault injection. |
| Evidence | Three endpoint response logs, rendered labels before/after. |
| Design review | Reviewed screen AC/DD 2026-10-28; Chưa thực hiện. See AUDIT_DOT4_SCREENS and fixture conventions for design scope/execution gates. |
