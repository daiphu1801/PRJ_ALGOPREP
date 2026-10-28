# F6 learner: browse, study, recall

## Scope and sources

- Scope: F6 TDD AC-01–09. Design only; all cases are **Chưa thực hiện**.
- TDD: `04-tdd/modules/F6_interview-bank.md` AC-01–09.
- DD: `03-dd/api/F6_interview-bank.md` §2–5, §27; `03-dd/logic/F6_interview-bank.md` BR-IB-04–06, BR-IB-12; `03-dd/validation/F6_interview-bank.md` §2–3, §9; `02-bd/database/F6_interview-bank.md` §3–5, §7. F6 has no jobs (logic §1).
- DD decision baseline: F6 DD updated 2026-10-23; TDD updated 2026-10-23.
- Execution status: Chưa thực hiện; no execution evidence exists.

## Shared fixtures

- Fixed clock for deterministic cases: `2026-10-27T02:00:00Z`; timestamp assertions use UTC instants (display timezone UTC+07:00).
- Learners: `U-A=10000000-0000-4000-8000-000000000001`, `U-B=10000000-0000-4000-8000-000000000002`, authenticated STUDENT tokens. Ownership always comes from token.
- Topic `T-SD=20000000-0000-4000-8000-000000000001`, code `SYSTEM_DESIGN`, display `System design`, `uses_star_framework=false`; `T-BEH=20000000-0000-4000-8000-000000000002`, code `BEHAVIORAL`, display `Hành vi`, `uses_star_framework=true`; level `L-HARD=30000000-0000-4000-8000-000000000001`, code `HARD`.
- Question IDs are `40000000-0000-4000-8000-000000000001` onward; suffixes in named fixtures are exact. `Q-X` suffix 000000000001, title `Design a distributed cache`, T-SD/L-HARD, display_no 1, content `Cache invalidation`, suggested approach `- Define constraints\n- Compare strategies`, framework `Context\nAction\nTradeoffs`, keywords `["cache","replication"]`, follow-ups `["How handle eviction?"]`, rubric `R1=30,R2=40,R3=30`; `Q-Y` suffix 000000000002, same fields except no rubric; `Q-H` suffix 000000000003, T-BEH, framework `Situation\nTask\nAction\nResult`, rubric present; `Q-SD-02` suffix 000000000004, T-SD/L-HARD, title `Distributed systems design`, display_no 2; `Q-SD-03` suffix 000000000005, T-SD/L-HARD, display_no 3. All test records use unique namespace suffixes and are not production seed data.
- External services: AI adapter, F5 budget port and Redis can be independently disabled/stubbed as each case requires. No background job participates.

## Branch inventory and traceability

| AC | Branches covered | Case IDs | Review |
|---|---|---|---|
| AC-01 | combined filters/count; prefix search; ignored invalid query values; no internal IDs | TC-F6-AC01-01, -02 | Draft |
| AC-02 | REVIEWING regardless of due; KNOWN despite due; UNSEEN disjoint | TC-F6-AC02-01 | Draft |
| AC-03 | due order incl KNOWN; unrated fill; embedded study fields; RETIRED exclusion | TC-F6-AC03-01 | Draft; ordering of final “other rated” group is irrelevant with fixture |
| AC-04 | retired absent/detail 404; related learner records retained | TC-F6-AC04-01 | Draft |
| AC-05 | idempotent true/false; owner isolation; retired write rejected | TC-F6-AC05-01, -02 | Draft |
| AC-06 | complete study response; STAR flag; unavailable AI/cache | TC-F6-AC06-01 | Draft |
| AC-07 | rubric-less study allowed; practice write blocked without side effects | TC-F6-AC07-01 | Draft |
| AC-08 | fixed intervals and overwrite; invalid enum; learner isolation | TC-F6-AC08-01, -02 | Draft |
| AC-09 | concurrent upsert same pair | TC-F6-AC09-01 | Draft; which concurrent write is “later” needs commit-order instrumentation |

## Cases

### TC-F6-AC01-01 — Combined topic, level, partial keyword and stable total

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-01; API §2.2–2.5; BR-IB-04 steps 2, 5–6; validation §2 |
| Kind / priority | Normal; P1 (primary learner discovery contract) |
| Environment | API integration, PostgreSQL; authenticated learner token |
| Preconditions | Seed exactly 40 ACTIVE questions. Six are `(T-SD,L-HARD)`; of those, exactly Q-X and `Q-SD-02` have title prefix token `Distributed`. Remaining four SD/HARD titles do not. Other 34 are not both SD and HARD. `Q-SD-02` is a unique fixture. |
| Test data | `GET /questions?topic=SYSTEM_DESIGN&level=HARD&q=Distribut`; U-A. Assert IDs are Q-X and Q-SD-02 only. |
| Steps | 1. Call the list endpoint. 2. Record response body and query result count. |
| Expected | HTTP 200; `items` contains exactly the two fixture IDs, `meta.shown=2`, `page.totalItems=2`, `meta.total=40`; default order follows display_no. Each item has no `displayNo` or `IQ-nnn` identifier. No writes/AI calls. |
| Cleanup | Delete only namespaced test rows and associated learner rows. |
| Evidence | Request/response, fixture seed manifest, DB count of ACTIVE rows, AI-port invocation count. |
| Design review | Draft |

### TC-F6-AC05-03 — Strict bookmark/recall input and unavailable-question guards

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-05, AC-08; API §3–5; validation §2–3 |
| Kind / priority | Abnormal / Security; P1 |
| Environment | F6 API + PostgreSQL + AI/budget spies |
| Preconditions | U-A has existing bookmark and KNOWN recall on ACTIVE Q-X; unknown UUID and malformed id do not identify an ACTIVE question; reset baseline per subcase. |
| Test data | PUT bookmark bodies{} and {bookmarked:"true"}; PUT recall bodies{} and {rating:"UNSEEN"}. For GET detail, PUT bookmark validboolean and PUT recall validKNOWN separately use unknown UUID, malformedid and RETIRED Q-R. |
| Steps | Send each independent mutation/read; inspect status/code and bookmark/recall snapshots. |
| Expected | Invalid bookmark body gets400 INB-V05; invalid recall gets400 INB-V04. Valid bodies targeting unknown/malformed/retired question get404 INB-010 on every listed endpoint. Existing bookmark/recall unchanged; no AI/budget call. |
| Cleanup | Restore learner fixture and remove disposable Q-R. |
| Evidence | Request/error matrix and unchanged scoped rows/port counts. |
| Design review | Reviewed against strict write validation and ACTIVE query guards; not executed. |

### TC-F6-AC01-02 — Invalid read filters are ignored rather than rejected

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-01; API §2.2–2.4; BR-IB-04 step 1; validation §2 |
| Kind / priority | Abnormal; P2 (read-wide compatibility behavior) |
| Environment | API integration |
| Preconditions | Same 40 ACTIVE fixture as AC01-01; U-A authenticated. |
| Test data | `GET /questions?topic=NO_SUCH_TOPIC&status=xyz&page=-3`; baseline request without these values. |
| Steps | 1. Call baseline and save ordered IDs. 2. Call request with invalid values. |
| Expected | Both return 200 and invalid-value response uses defaults (`status=ALL`, page 1) and ignores unknown topic; ordered IDs and counts equal baseline. No 400, data writes, or AI calls. |
| Cleanup | No state mutation; remove fixture after suite. |
| Evidence | Both responses and DB fixture inventory. |
| Design review | Draft |

### TC-F6-AC02-01 — Recall status is independent of due time

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-02; API §2; BR-IB-04 step 3 and BR-IB-06; DB6 §7 |
| Kind / priority | Boundary; P1 (status/due distinction) |
| Environment | API integration; fixed DB clock |
| Preconditions | For U-A, Q-X rating FORGOTTEN rated at `now()-2h`, next_review `now()+1d`; Q-SD-02 rating KNOWN rated at `now()-10d`, next_review `now()-3d`; Q-SD-03 has no recall row. All three ACTIVE. |
| Test data | Sequential GET requests with `status=REVIEWING`, `KNOWN`, `UNSEEN`. |
| Steps | Call each filter and collect IDs. |
| Expected | REVIEWING includes Q-X although not due; KNOWN includes only Q-SD-02 although due; UNSEEN includes Q-SD-03. These three IDs do not overlap. `meta.total` stays total ACTIVE bank count for all three. |
| Cleanup | Remove only U-A recall rows and fixtures. |
| Evidence | Three responses and recall rows with rated/next timestamps. |
| Design review | Draft |

### TC-F6-AC03-01 — Quick drill prioritizes due rows then unrated rows

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-03; API §2.2–2.3; BR-IB-04 step 7; DB6 §7 |
| Kind / priority | Normal; P1 (ordered practice selection) |
| Environment | API integration; fixed clock |
| Preconditions | Create 25 matching ACTIVE questions. Three are due at `now()-5d`, `now()-2d`, `now()-1h` respectively (first is KNOWN, another FORGOTTEN); 10 are unrated; 12 rated but not due. Display numbers make unrated order unambiguous. Give each of first 10 unrated a distinct suggestedApproach and keywords. |
| Test data | U-A calls `GET /questions?topic=SYSTEM_DESIGN&level=HARD&drill=true&limit=10`, then limit=20. Give twelve rated future rows distinct next_review_at values T0+1d through T0+12d. |
| Steps | Call both limits and inspect array order and every returned item. |
| Expected | limit10: first3 due ordered by next_review_at, then first7 unrated by display_no. limit20: all3 due, all10 unrated, then first7 rated-not-due ordered by future next_review_at (DD BR-IB-04 step7). Due KNOWN remains in due group. Every item includes suggestedApproach/coreKeywords, page.totalItems equals selected size, totalPages=1. No AI call. |
| Cleanup | Remove namespaced questions and recall rows. |
| Evidence | Response, fixture timestamps, DB status values, AI-port call count. |
| Design review | Draft |

### TC-F6-AC04-01 — Retired question disappears but learner data remains

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-04; API §2–5, §7; BR-IB-03, BR-IB-04; DB6 §3–7; validation §3 |
| Kind / priority | Recovery; P1 (preserves learner history across retirement) |
| Environment | API integration; seed admin/teacher route or controlled DB retirement |
| Preconditions | Q-X initially has ACTIVE status, and U-A has bookmark, recall row, and one user_answers row (`attempt_no=1`, answer `fixture-answer`, feedback FAILED). Snapshot associated row IDs before setting status RETIRED. |
| Test data | List default and bookmarkedOnly, drill, detail `GET /questions/Q-X`, history endpoint. |
| Steps | 1. Set Q-X RETIRED through authorized retire operation. 2. Query all learner list modes. 3. GET old detail and history URL. 4. Inspect persisted related rows. |
| Expected | Q-X absent in every list/drill; detail returns `404 INB-010` (learner message “câu hỏi không còn khả dụng”). History endpoint also returns `404 INB-010` per endpoint contract. Original user_answers, recall_ratings and bookmarks still exist unchanged; no cascade delete. Note AC wording “xem lịch sử lượt” does not explicitly state UI behavior on retired ID; API contract §7 resolves it as 404 while retaining underlying history. |
| Cleanup | Delete fixture rows in FK-safe order after evidence. |
| Evidence | Responses for each list mode/detail/history and before/after DB row snapshots. |
| Design review | Draft |

### TC-F6-AC05-01 — Bookmark state is idempotent and scoped to actor

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-05; API §5; BR-IB-13; DB6 §5; validation §3 |
| Kind / priority | Boundary; P1 (learner-owned state and duplicate protection) |
| Environment | API integration, PostgreSQL |
| Preconditions | Q-X ACTIVE; no bookmark for U-A or U-B. |
| Test data | PUT `/questions/Q-X/bookmark` bodies `{"bookmarked":true}` twice, then `false` twice; U-A and U-B tokens. |
| Steps | 1. Send true twice as U-A. 2. Verify filtered list. 3. Send false twice. 4. Verify filtered list. 5. Read U-B view. |
| Expected | Every request returns 200 with requested state. Exactly one `(U-A,Q-X)` bookmark row after true twice; zero after false twice. `bookmarkedOnly=true` shows then excludes Q-X for U-A. U-B remains unbookmarked throughout. |
| Cleanup | Remove fixture bookmark and question. |
| Evidence | Request/response series, bookmark row counts by user, filtered list responses. |
| Design review | Draft |

### TC-F6-AC05-02 — Retired question cannot be bookmarked

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-05; API §5.4–5.6; validation §3 |
| Kind / priority | Abnormal; P1 (prevents stale reference writes) |
| Environment | API integration |
| Preconditions | Q-X RETIRED; U-A has no bookmark for it. |
| Test data | PUT `/questions/Q-X/bookmark` body `{"bookmarked":true}`. |
| Steps | Send request and query bookmarks. |
| Expected | 404 `INB-010`; no bookmark row is created. |
| Cleanup | Remove fixture question. |
| Evidence | Error response and DB absence query. |
| Design review | Draft |

### TC-F6-AC06-01 — Study detail is complete and independent of AI/cache

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-06; API §3; BR-IB-05, BR-IB-12; ARCH6 §3.1 |
| Kind / priority | Degraded; P1 (study path must work without AI infrastructure) |
| Environment | API integration; disable AI adapter, F5 budget port, Redis |
| Preconditions | Q-H ACTIVE with stored approach/framework/keywords/follow-ups and topic STAR true; Q-X ACTIVE with same content types and STAR false. U-A authenticated. |
| Test data | GET detail for Q-H and Q-X. |
| Steps | Disable services; fetch both details. |
| Expected | Both 200. Fields `suggestedApproach`, `sampleAnswerFramework`, `coreKeywords`, `followUpQuestions` equal stored content; Q-H `topic.usesStarFramework=true`, Q-X false. Neither response has `rubric`, `rubric_snapshot`, `displayNo`, `status`, score nor `IQ-nnn`; no AI/F5/Redis invocation. |
| Cleanup | Re-enable test services and delete fixtures. |
| Evidence | Both JSON responses, service stubs showing zero calls, stored fixture values. |
| Design review | Draft |

### TC-F6-AC07-01 — Rubric-less question is learnable but cannot be submitted

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-07; API §3 and §6; BR-IB-05, BR-IB-07; validation §4 |
| Kind / priority | Abnormal; P1 (server-side eligibility and side-effect guard) |
| Environment | API integration; Redis and AI call counters; F5 budget stub |
| Preconditions | Q-Y ACTIVE with no answer_rubrics. U-A has <20 hourly attempts and F5 budget available. |
| Test data | GET Q-Y detail then POST practice answer `{"answerText":"Explain the tradeoff."}`. |
| Steps | 1. Fetch detail. 2. Submit directly to API. 3. Compare DB, Redis counter, budget and AI call counts. |
| Expected | Detail returns 200 and all stored study fields with `practiceAvailable=false`; practice submission returns 422 `INB-040`; no `user_answers`, AI call, token usage, or hourly quota increment. |
| Cleanup | Delete Q-Y and associated test records. |
| Evidence | Both responses, before/after DB counts, Redis value, port invocation logs. |
| Design review | Draft |

### TC-F6-AC08-01 — Each recall rating replaces interval from current clock

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-08; API §4; BR-IB-06; validation §3; DB6 §7 |
| Kind / priority | Normal; P1 (review scheduling) |
| Environment | API integration with controllable clock |
| Preconditions | Q-X ACTIVE; no recall rows for U-A/U-B; clock `T0=2026-10-27T02:00:00Z`. |
| Test data | U-A requests FORGOTTEN at T0, VAGUE at T0+10m, KNOWN at T0+20m; U-B request FORGOTTEN at T0+30m. |
| Steps | Send requests in stated order, advance controlled clock before each, then inspect responses and rows. |
| Expected | U-A response intervals respectively 1,3,7; each `nextReviewAt = ratedAt + interval` and final row is one row with KNOWN, `rated_at=T0+20m`, next at `T0+7d+20m`. It is based on current operation, not accumulated. U-B has an independent row and cannot alter U-A state. |
| Cleanup | Remove both recall rows and Q-X. |
| Evidence | Responses, timestamps and row counts scoped by user/question. |
| Design review | Draft |

### TC-F6-AC08-02 — Invalid rating is rejected without changing current state

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-08; API §4.4; validation §3 |
| Kind / priority | Abnormal; P2 (input validation) |
| Environment | API integration |
| Preconditions | Q-X ACTIVE; U-A has existing VAGUE row with known rated_at/next_review_at. |
| Test data | PUT recall body `{"rating":"GOOD"}` (business label “Tốt”). |
| Steps | Send request and reread row. |
| Expected | 400 `INB-V04`; existing row is byte-for-byte unchanged; no second row. |
| Cleanup | Remove recall row and question. |
| Evidence | Error response and DB before/after values. |
| Design review | Draft |

### TC-F6-AC09-01 — Concurrent recall writes upsert one current row

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-09; API §4.5; BR-IB-06; DB6 §7 |
| Kind / priority | Concurrency; P1 (unique current-state invariant) |
| Environment | API integration, PostgreSQL; two synchronized requests under U-A |
| Preconditions | Q-X ACTIVE and no U-A recall row. Configure a DB transaction gate: R1 (`FORGOTTEN`) holds the write lock, R2 (`KNOWN`) waits; release R1 and observe its commit before R2 continues. Repeat with roles reversed. |
| Test data | Two PUT requests to same question with different ratings. |
| Steps | Start R1, pause at the DB write while it holds the row/unique-key lock; start R2 and confirm it waits. Release R1, observe commit, then release/observe R2 commit; query unique row. Repeat with R2 first and R1 second. |
| Expected | Both requests return 200, no unique violation; exactly one row exists. In each controlled run the final `rating`, `interval_days`, `rated_at`, `next_review_at` match the second committed write (`KNOWN` => 7d, `FORGOTTEN` => 1d). No intermediate duplicate row is committed. |
| Cleanup | Delete recall row and fixture. |
| Evidence | Both responses, barrier timestamps, DB transaction/commit order, final row. |
| Design review | Draft; deterministic commit-order fixture required; chưa thực thi |

