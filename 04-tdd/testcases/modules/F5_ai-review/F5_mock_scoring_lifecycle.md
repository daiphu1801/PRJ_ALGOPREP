# F5 Mock Interview — rubric, kết thúc, job và tóm tắt

- **Scope:** F5 AC-32–37; completion result, weighted score, schema/provider errors, idempotency/races, idle sweep, learner summary and graceful degradation.
- **TDD:** [`04-tdd/modules/F5_ai-review.md`](../../../modules/F5_ai-review.md)
- **DD oracle:** `03-dd/api/F5_ai-review.md` §9, §12–13; `03-dd/logic/F5_ai-review.md` BR-AI-09, BR-AI-11, BR-AI-15; `03-dd/jobs/F5_ai-review.md` §0, §4 JOB-AI-04; `03-dd/validation/F5_ai-review.md` §11–12; `02-bd/database/F5_ai-review.md:114,116,120,156–169`.
- **Fixture catalog:** API/job integration with PostgreSQL, controllable LLM, outbox capture, and fixed DB clock. A=`usr-A`. Standard valid scoring response contains exactly four criteria in API order: `CLARITY`, `TECHNICAL_ACCURACY`, `PUSHBACK_HANDLING`, `COMPLEXITY_AWARENESS`; each has score and non-empty comment. Scores below are explicit per case. Sessions have persisted transcript and `turn_count` as specified. Job test can trigger JOB-AI-04 directly and inspect candidate selection/update. Summary service/UI can separately fail its summary request while other page blocks return known data. All cases are **Chưa thực hiện**.

## Branch inventory and coverage

| AC | Branches covered | Case IDs | Review |
|---|---|---|---|
| AC-32 | Normal early completion; full rubric/result/activity; closed-session rejection | TC-F5-AC32-01, TC-F5-AC32-02 |
| AC-33 | Fixed weighted formula; admin cannot configure weights | TC-F5-AC33-01, TC-F5-AC33-02 |
| AC-34 | Missing criterion; out-of-range score; provider failure/retry; empty session | TC-F5-AC34-01, TC-F5-AC34-02, TC-F5-AC34-03, TC-F5-AC34-04 |
| AC-35 | Manual completion races idle sweep; repeat completion idempotency | TC-F5-AC35-01, TC-F5-AC35-02 |
| AC-36 | Idle threshold and enoughTurns split; scorer failure recovery; under-threshold no-score branch | TC-F5-AC36-01, TC-F5-AC36-02 |
| AC-37 | Summary aggregation/filtering/weakest criterion/recent list; zero-session null; degraded module block | TC-F5-AC37-01, TC-F5-AC37-02, TC-F5-AC37-03 |

## Cases

### TC-F5-AC32-01 — Kết thúc sớm lưu đủ rubric và một activity

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-32; API §12.3–12.6; BR-AI-09; DB5:114,163,167 |
| Kind / priority | Normal, P0 — user-visible completion contract |
| Environment | API integration; PostgreSQL + fake LLM + outbox capture |
| Preconditions | A owns running session with `turn_count=2`, at least two completed user/AI pairs; no in-flight request. |
| Test data | Valid rubric for four codes; score values 8,6,7,5 and unique non-empty comments `C1`…`C4`. |
| Steps | POST `/interview-sessions/{id}/complete`; GET session detail; read recent activity projection. |
| Expected | Completion returns 200, stage `COMPLETED`, `isReferenceOnly=true`, four criteria in fixed order with scores/comments/weights, overallScore `6.55`, feedbackSummary comments joined by blank line, turnCount=2, endedAt set. DB has four rubric rows and one `MockInterviewFinished` outbox row; exactly one recent activity. Subsequent turn request gets `AIR-111` with no LLM call. |
| Cleanup | Delete session/turn/rubric/usage/outbox/activity fixtures. |
| Evidence | Complete response; GET response; DB rows; outbox and activity records; provider call count. |
| Design review | Draft; execution `Chưa thực hiện` |

### TC-F5-AC32-02 — Phiên COMPLETED trả bảng điểm đã lưu, không sinh thêm lượt

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-32; API §9.5, §12.1, §12.4; BR-AI-09 steps 1, 9 |
| Kind / priority | Abnormal/Boundary, P1 — terminal-state and saved-result behavior |
| Environment | API integration |
| Preconditions | A owns a completed session with all four saved rubric rows and exactly one outbox event. |
| Test data | Existing result as saved; request complete again and attempt one more turn. |
| Steps | Snapshot DB/provider count; POST complete; POST turns with current count. |
| Expected | Repeat complete returns the exact stored rubric result without LLM call or additional rubric/outbox/activity rows. Turn request is rejected `409 AIR-111`; no turn/LLM side effect. |
| Cleanup | Delete fixture and related rows. |
| Evidence | First and repeated result bodies; DB counts; provider count; terminal turn error. |
| Design review | Draft; execution `Chưa thực hiện` |

### TC-F5-AC33-01 — Điểm tổng dùng đúng trọng số và quy tắc làm tròn

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-33; API §12.3; BR-AI-09 steps 8; DB5:156,167 |
| Kind / priority | Boundary, P1 — financial-like numeric correctness of displayed score |
| Environment | API integration; deterministic structured LLM |
| Preconditions | A owns running session with at least one completed answer. |
| Test data | In fixed criterion order: scores `8.00, 6.00, 7.00, 5.00`; weights `25,30,25,20`; expected numerator `200+180+175+100=655`, divide 100=`6.55`. |
| Steps | Complete session; inspect API response and calculate independently. |
| Expected | `overallScore=6.55`; weights in result are 25,30,25,20 in criterion order. API formula returns weighted sum / 100 rounded half-up to two decimals. |
| Cleanup | Delete session/result fixtures. |
| Evidence | Provider rubric; response JSON; calculation sheet; DB rubric rows. |
| Design review | Draft; execution `Chưa thực hiện` |

### TC-F5-AC33-02 — Không có cấu hình quản trị cho trọng số rubric

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-33; BR-AI-09 fixed constants; decision `DEC-2026-1014-drop-admin-rubric-config`; API admin endpoint inventory §§14–31 |
| Kind / priority | Security/Boundary, P2 — prevent unsupported config surface |
| Environment | API integration/admin route inventory |
| Preconditions | Authenticated ADMIN test account; deployed API route inventory available. |
| Test data | Enumerate documented/admin routes and attempt only a documented rubric-weight configuration route if one exists. |
| Steps | Inspect OpenAPI/admin route list; verify no rubric weight field is exposed in prompt/config endpoints; if route is absent, record route absence. |
| Expected | Rubric weights are fixed application constants and are not configurable through admin routes. No speculative URL or error code is asserted where no endpoint is documented. |
| Cleanup | No mutation expected. |
| Evidence | OpenAPI/route inventory; admin UI/API config schema. |
| Design review | Draft; execution `Chưa thực hiện` |

### TC-F5-AC34-01 — Thiếu criterion trong kết quả chấm không ghi điểm

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-34; API §12.4–12.6; BR-AI-09 steps 4–6; validation §11 |
| Kind / priority | Abnormal, P0 — scoring schema validation and transaction atomicity |
| Environment | API integration; fake LLM structured response injection |
| Preconditions | A owns running session `turn_count=2`; no rubric rows. |
| Test data | Provider result contains only CLARITY, TECHNICAL_ACCURACY, PUSHBACK_HANDLING (missing COMPLEXITY_AWARENESS). |
| Steps | POST complete; inspect response, session, rubric, outbox; replace stub with valid response and retry. |
| Expected | First response 502 `AIR-502`; session stage remains running, no rubric rows or outbox; token usage may be written if provider supplied token usage per BR-AI-09. Retry with valid schema completes successfully. |
| Cleanup | Delete session and all usage/rubric/outbox rows; reset provider stub. |
| Evidence | Both responses; DB state after failure/success; provider calls. |
| Design review | Draft; execution `Chưa thực hiện` |

### TC-F5-AC34-02 — Điểm 10.01 ngoài khoảng không ghi rubric

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-34; API §12.4–12.6; BR-AI-09 step 5; validation §11 |
| Kind / priority | Boundary, P0 — score upper bound |
| Environment | API integration; fake LLM |
| Preconditions | Running session with nonzero turn count; no rubric rows. |
| Test data | Four criteria present; CLARITY score=10.01; other scores valid and comments non-empty. |
| Steps | POST complete; inspect result and DB. |
| Expected | 502 `AIR-502`; session remains in original running stage; no rubric rows or outbox. If token usage returned, record according to BR-AI-09. |
| Cleanup | Delete session and any usage rows; reset stub. |
| Evidence | Error response; DB snapshot; provider result. |
| Design review | Draft; execution `Chưa thực hiện` |

### TC-F5-AC34-03 — LLM scoring lỗi thì phiên giữ nguyên và retry thành công

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-34; API §12.4–12.6; BR-AI-09; BR-AI-11; SEC5:84–87 |
| Kind / priority | Recovery, P0 — transient dependency failure |
| Environment | API integration; controllable LLM and quota/budget capture |
| Preconditions | A owns running session with `turn_count=2`; first complete call times out/provider error; second returns valid rubric. Budget may be locked after session creation to prove completion is not gated. |
| Test data | Valid session; first provider error then valid four-criterion result. |
| Steps | POST complete once; inspect state. Restore provider and immediately retry. |
| Expected | First receives 503 `AIR-501`; session remains running; no rubric or outbox; retry is not blocked by session-start quota/cooldown and succeeds when provider returns valid schema. No budget gate applies to completion of an already-open session. |
| Cleanup | Delete session/turn/rubric/usage/outbox; restore budget and provider. |
| Evidence | Both responses; DB snapshots; budget state; provider call logs. |
| Design review | Draft; execution `Chưa thực hiện` |

### TC-F5-AC34-04 — Phiên chưa trả lời lượt nào không thể chấm

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-34; API §12.4; BR-AI-09 step 2; API decision Q4 |
| Kind / priority | Boundary, P1 |
| Environment | API integration |
| Preconditions | A owns running session with opening turn only and `turn_count=0`. |
| Test data | POST complete with no body |
| Steps | Snapshot DB/provider; POST complete; inspect response and records. |
| Expected | 422 `AIR-116`; no LLM call, rubric rows, outbox or stage transition. |
| Cleanup | Delete empty session and opening turn. |
| Evidence | Response; DB state; provider count. |
| Design review | Draft; execution `Chưa thực hiện` |

### TC-F5-AC35-01 — User complete đua với idle sweeper chỉ tạo một kết quả

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-35; API §12.5; BR-AI-09 steps 3–6; JOB-AI-04 §4 |
| Kind / priority | Concurrency, P0 — prevents duplicate rubric/event/activity |
| Environment | API/job integration; PostgreSQL; provider barrier; outbox/activity observers |
| Preconditions | A owns running session `turn_count>=4`, `updated_at` older than 15 minutes; user completion and JOB-AI-04 can be synchronized to race for session. |
| Test data | Both triggers use same deterministic valid rubric response. |
| Steps | Release user complete and sweep for same session simultaneously; await both; then call user complete once more. |
| Expected | Shared session advisory lock permits one scoring owner; final state has exactly four rubric rows, one `MockInterviewFinished` outbox event and one activity item. A user request colliding with a held lock returns 409 AIR-113; sweep skips a busy session. A path observing an already-completed session reads saved result/skips it. Repeat user complete returns saved result without another LLM/event. |
| Cleanup | Delete session, turns, rubric, outbox, usage/activity; stop job fixture. |
| Evidence | Concurrent traces; DB uniqueness/counts; outbox and activity; provider call count and responses. |
| Design review | Draft; execution `Chưa thực hiện` |

### TC-F5-AC35-02 — Complete lặp sau khi đã COMPLETED là idempotent

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-35; API §12.1, §12.4; BR-AI-09 step 1 |
| Kind / priority | Normal/Boundary, P1 |
| Environment | API integration |
| Preconditions | Session already COMPLETED with four rubric rows/outbox/activity. |
| Test data | No-body complete request repeated by owner A |
| Steps | Snapshot response and row counts; POST complete again. |
| Expected | HTTP 200 with same stored result; no LLM call and counts for rubric/outbox/activity do not increase. |
| Cleanup | Delete fixture result/session rows. |
| Evidence | Response equality; DB counts; provider call count. |
| Design review | Draft; execution `Chưa thực hiện` |

### TC-F5-AC36-01 — JOB-AI-04 tách phiên idle theo ngưỡng 15 phút và 4 lượt

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-36; JOB-AI-04 §4; API §9, §12; BR-AI-09; DB5:114,116,120 |
| Kind / priority | Boundary, P0 — scheduled lifecycle branches |
| Environment | Job integration; PostgreSQL fixed clock; fake LLM; activity/summary readers |
| Preconditions | P1 and P2 are A-owned running sessions. Set both `updated_at=now()-15m-1s`; P1 `turn_count=4`; P2 `turn_count=2`; stage values running; valid scorer response. |
| Test data | P1 has complete saved transcript; P2 has transcript but is below `enoughTurns=4`. |
| Steps | Trigger JOB-AI-04 once; GET each session; query rubric/outbox/activity; load progress summary. |
| Expected | P1 becomes COMPLETED with four scores, endedAt, one outbox/activity through same scoring path. P2 becomes ABANDONED with transcript retained, no rubric, no outbox/activity; GET result null; excluded from completed summary. |
| Cleanup | Delete both sessions/turns/rubric/usage/outbox/activity; reset fixed clock. |
| Evidence | Job log; before/after session state; detail responses; rubric/outbox/activity and summary query. |
| Design review | Draft; execution `Chưa thực hiện` |

### TC-F5-AC36-02 — Auto-score của idle job phục hồi sau lỗi LLM

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-36 recovery; JOB-AI-04 §4; BR-AI-09; BR-AI-11 |
| Kind / priority | Recovery, P1 — scheduled scoring does not prematurely abandon on transient provider error |
| Environment | Job integration; controllable LLM; fixed clock |
| Preconditions | Two isolated A sessions idle >15m, turn_count=4, sweep retry counters start zero. First session provider fails once then succeeds; second fails all three configured attempts. |
| Test data | Four-turn transcript; first fixture has valid scorer output on cycle2; second always returns provider error with no usage. |
| Steps | Run first fixture for two cycles; reset isolated state and run second fixture for cycles1/2/3 plus an extra sweep; inspect state, Redis retry counters and provider counts at every boundary. |
| Expected | First fixture stays active after failure then completes once with four rubric rows/one outbox on cycle2. Second stays active after failures1/2, becomes ABANDONED after failure3, retains transcript with no rubric/outbox, and extra sweep does not call provider. WARN records failures; no budget gate blocks either path. |
| Cleanup | Delete session and related rows; clear job retry key/provider fault. |
| Evidence | Two job logs; session state after each; provider calls; final rubric/outbox. |
| Design review | Draft; execution `Chưa thực hiện` |

### TC-F5-AC37-01 — Summary tính đúng tập đủ điểm, điểm yếu và danh sách gần đây

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-37; API §13.1–13.3; BR-AI-09 step 9; DB5:163,167–169 |
| Kind / priority | Normal, P0 — progress analytics correctness |
| Environment | API integration; PostgreSQL; summary consumer/UI |
| Preconditions | A has: S1 COMPLETED with scores `[8,6,7,5]`, overall 6.55, endedAt within 7d; S2 COMPLETED with `[8,8,7,5]`, overall 7.15, endedAt within 30d but outside 7d; S3 COMPLETED but only 3 rubric rows (partial); S4 ABANDONED, no scores. Distinct started/ended timestamps. |
| Test data | `range=all` and `range=7d`; request recent list with supported default/limit. |
| Steps | GET summary for all; GET summary for 7d; inspect response/list fields and UI summary block. |
| Expected | `all`: count=2 (S1,S2 only), averageScore=(6.55+7.15)/2=6.85; per-criterion means CLARITY=8, TECHNICAL_ACCURACY=7, PUSHBACK_HANDLING=7, COMPLEXITY_AWARENESS=5, so weakest is COMPLEXITY_AWARENESS. Recent entries contain only S1/S2 with overall and four component values, ordered endedAt descending. S3 partial and S4 abandoned excluded. `7d` includes only S1. |
| Cleanup | Delete all four sessions/turns/rubric rows. |
| Evidence | Request params; API responses; source SQL rows; UI block screenshot if UI consumer is exercised. |
| Design review | Draft; execution `Chưa thực hiện` |

### TC-F5-AC37-02 — Không có phiên đủ rubric thì count 0 và average null

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-37; API §13.3; DB5:163,167 |
| Kind / priority | Boundary, P1 — empty aggregate |
| Environment | API integration; isolated owner with no interview sessions |
| Preconditions | A has no session records in any state within query scope. |
| Test data | GET summary with default range and `range=all` |
| Steps | Request summary; inspect JSON and progress UI. |
| Expected | HTTP 200; `completedSessionCount=0`; `averageScore=null`; no recent sessions; UI has no fabricated average or weakest score. |
| Cleanup | None beyond isolated test account. |
| Evidence | Responses; DB no-session query; UI state. |
| Design review | Draft; execution `Chưa thực hiện` |

### TC-F5-AC37-03 — Lỗi API summary chỉ suy giảm khối phỏng vấn

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-37; API §13.1; BR-AI-11; `02-bd/screens/users/USR0501_my_progress.md:576` |
| Kind / priority | Degraded, P1 — isolate summary outage from other progress blocks |
| Environment | UI integration/E2E with summary endpoint fault injection and other block APIs available |
| Preconditions | Progress page has non-empty data for non-interview blocks; summary endpoint is configured to return 503 or time out; LLM may be unavailable but summary itself is DB-only. |
| Test data | Summary endpoint fault response; successful known responses for the other progress blocks. |
| Steps | Load progress page; capture network requests and visible block states. |
| Expected | Interview block alone displays its degraded/unavailable state per screen contract; other progress blocks render their successful data and page remains usable. Do not assert a specific Vietnamese sentence not fixed by DD. |
| Cleanup | Remove fault injection and clear page fixture. |
| Evidence | Network trace; page screenshot/accessibility tree; successful other-block response payloads. |
| Design review | Draft; execution `Chưa thực hiện` |
