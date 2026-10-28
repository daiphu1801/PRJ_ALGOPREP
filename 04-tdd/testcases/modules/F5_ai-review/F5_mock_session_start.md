# F5 Mock Interview — mở phiên và khôi phục

- **Scope:** F5 AC-18–23; mở phiên theo ba lối vào, kiểm tra request, quota/failure và khôi phục phiên.
- **TDD:** [`04-tdd/modules/F5_ai-review.md`](../../../modules/F5_ai-review.md)
- **DD oracle:** `03-dd/api/F5_ai-review.md` §8–9; `03-dd/logic/F5_ai-review.md` BR-AI-06, BR-AI-10; `03-dd/validation/F5_ai-review.md` §4, §9; `03-dd/jobs/F5_ai-review.md` §0; `02-bd/database/F5_ai-review.md` session/turn schema.
- **Fixture catalog:** API integration environment with PostgreSQL, Redis, controllable `LlmProviderPort`, fixed UTC clock, and request/DB/provider-call capture. Learner A=`usr-A`; learner B=`usr-B`; admin=`adm-1`. A owns Accepted submission `sub-A` for problem `prob-A`; B owns Accepted `sub-B`; A also has `sub-WA` with `WRONG_ANSWER`. Valid question UUID=`11111111-1111-4111-8111-111111111111`. Valid opening fixture returns one non-empty AI message and usage `{input:20, output:10, model:mock-f5}`. Unless a case says otherwise, A budget is unlocked, hourly count=0, cooldown key absent; no matching session exists. All cases are **Chưa thực hiện**.

## Branch inventory and coverage

| AC | Branches covered | Case IDs | Review |
|---|---|---|---|
| AC-18 | Normal topic start; persistence/reload | TC-F5-AC18-01 | Draft |
| AC-19 | Normal submission; ignored practice fields; non-Accepted; foreign submission security | TC-F5-AC19-01, TC-F5-AC19-02, TC-F5-AC19-03 | Draft |
| AC-20 | Normal question-bank start; missing question text | TC-F5-AC20-01, TC-F5-AC20-02 | Draft |
| AC-21 | Invalid maxTurns; invalid level; missing topics; >10 topics; topic >60; conflicting entry fields; accepted boundary values | TC-F5-AC21-01, TC-F5-AC21-02, TC-F5-AC21-03, TC-F5-AC21-04, TC-F5-AC21-05, TC-F5-AC21-06, TC-F5-AC21-07 | Draft |
| AC-22 | Opening provider failure/retry recovery; learner budget lock vs admin; hourly quota boundary; invalid opening schema | TC-F5-AC22-01, TC-F5-AC22-02, TC-F5-AC22-03, TC-F5-AC22-04 | Draft |
| AC-23 | Redis/context loss recovery; owner read/security | TC-F5-AC23-01, TC-F5-AC23-02 | Draft |

## Cases

### TC-F5-AC18-01 — Tạo phiên chủ đề và đọc lại câu mở đầu đã lưu

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-18; API §8.2–8.6; BR-AI-06; DB5 `interview_sessions`, `interview_turns` |
| Kind / priority | Normal, P1 — đường mở phiên chính |
| Environment | API integration; PostgreSQL + Redis + fake LLM |
| Preconditions | A đã đăng nhập; không có phiên trùng; budget/quota/cooldown đạt; fake LLM trả opening hợp lệ |
| Test data | `entryType=TOPIC_SELECTION`, `topics=["Cấu trúc dữ liệu","Độ phức tạp"]`, `interviewerLevel=JUNIOR`, `maxTurns=8`, `hintAllowed=true` |
| Steps | 1. POST `/interview-sessions` với body trên. 2. Ghi response, DB và số lần gọi LLM. 3. GET `/interview-sessions/{sessionId}`. 4. Xoá Redis key `ai:chatmemory:<sessionId>` rồi GET lần nữa. |
| Expected | 1. 201, `stage=EXPLAIN`, `turnCount=0`, đúng các tuỳ chọn và `entryType`; opening có `turnIndex=1`, `role=AI`, `stage=EXPLAIN`. 2. Một session, một opening turn và usage cùng `owner_id`; đúng một LLM call. 3–4. Cả hai lần đọc trả opening turn đã lưu, không tạo session/turn mới; mất Redis không làm mất lịch sử. |
| Cleanup | Xoá session fixture và các turn, rubric, usage/outbox liên quan theo cascade/test reset; clear Redis keys. |
| Evidence | Request/response; query session, turns, usage; provider call count; GET response sau Redis eviction. |
| Design review | Draft; execution `Chưa thực hiện` |

### TC-F5-AC23-03 — Session ownership and missing-session guards apply to every operation

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-23; API §9–12; BR-AI-07/08/09/16 |
| Kind / priority | Security / Abnormal; P0 |
| Environment | F5 API + JWT role fixtures + PostgreSQL + provider spy |
| Preconditions | A owns active TOPIC_SELECTION sess-A, hintAllowed=true, count1 with a complete pair; B, INSTRUCTOR and ADMIN actors have no ownership of it. Unknown UUID has no session. |
| Test data | For each non-owner call GET session, POST turns with content Answer/expectedTurnCount1, POST hint, POST complete. Repeat four operations with unknown UUID as A. |
| Steps | Snapshot turns/session/usage/events before each independent operation; invoke directly and inspect payloads/provider counts. |
| Expected | All non-owner operations return403 AIR-302 without transcript leakage or mutation/LLM call, including ADMIN/INSTRUCTOR. Unknown session returns404 AIR-110 with no side effects for all four operations. No caller-supplied userId can grant access. |
| Cleanup | Revoke temporary tokens and remove fixture session. |
| Evidence | Actor claims, request/status/code matrix and unchanged DB/provider snapshots. |
| Design review | Reviewed against ownership guard in every use case; not executed. |

### TC-F5-AC21-08 — Required mode fields, UUID and question-text boundaries

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-21; API §8; validation §4 |
| Kind / priority | Abnormal / Boundary; P1 |
| Environment | F5 API + PostgreSQL/Redis + provider spy |
| Preconditions | A authenticated; valid QUESTION_BANK and TOPIC_SELECTION baselines; reset session/quota fixtures per subcase. |
| Test data | Independent invalid mutations: missing/unknown entryType=>AIR-V03; TOPIC_SELECTION missing hintAllowed or string true=>AIR-V03; missing interviewerLevel=>AIR-V04; missing maxTurns=>AIR-V05; topics=[whitespace]=>AIR-V06; QUESTION_BANK questionRef=IQ-001 or questionText length2001/whitespace=>AIR-V07. Valid subcases use questionText lengths1 and2000 with all required self-practice options. |
| Steps | Submit each isolated invalid body then valid boundary bodies; compare writes/provider calls. |
| Expected | Every invalid body gets400 and its exact code with no session/turn/token/quota/cooldown side effect. Each valid boundary body gets201 with one stored opening and usage. Topic duplicates [Graphs,graphs] are a separate valid subcase and normalize to one topic; they are not rejected as an invalid set. |
| Cleanup | Remove created sessions/turns/usage and restore keys. |
| Evidence | Bodies/status/code, text lengths, provider counts and DB/Redis deltas. |
| Design review | Reviewed against validation §4; not executed. |

### TC-F5-AC19-01 — Lối bài nộp bỏ qua các trường chỉ dành cho tự luyện

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-19; API §8.2–8.6; validation §4; DB5:122–124 |
| Kind / priority | Normal, P1 |
| Environment | API integration; PostgreSQL + fake LLM |
| Preconditions | A sở hữu `sub-A` trạng thái `ACCEPTED`; request hợp lệ và quota đạt |
| Test data | `entryType=SUBMISSION`, `submissionId=sub-A`, kèm `maxTurns=8`, `interviewerLevel=JUNIOR`, `hintAllowed=true` |
| Steps | POST create; GET session; POST hint trên session vừa tạo. |
| Expected | Tạo thành công gắn `sub-A`; `maxTurns`, `interviewerLevel`, `hintAllowed` response/DB đều NULL; hint trả `422 AIR-115`, provider không được gọi thêm. Không có topic/question fields lưu cho session. |
| Cleanup | Xoá fixture session và related rows. |
| Evidence | Request/response; DB projection; provider call log trước/sau hint. |
| Design review | Draft; execution `Chưa thực hiện` |

### TC-F5-AC19-02 — Từ chối bài nộp chưa Accepted

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-19; API §8.4; BR-AI-06; validation §4 |
| Kind / priority | Abnormal, P1 — ngăn tạo phiên từ submission không đủ điều kiện |
| Environment | API integration |
| Preconditions | A sở hữu `sub-WA` với status `WRONG_ANSWER`; quota đạt |
| Test data | SUBMISSION request với `submissionId=sub-WA` |
| Steps | Ghi baseline session/turn count; POST create; kiểm tra DB và LLM stub. |
| Expected | HTTP 422 `AIR-102`; không có session, turn, token usage; không gọi LLM; không tiêu quota/cooldown cho lần thất bại theo BR-AI-04 retry semantics. |
| Cleanup | Reset quota/cooldown và test DB state. |
| Evidence | Error response; before/after DB; provider and quota/cooldown logs. |
| Design review | Draft; execution `Chưa thực hiện` |

### TC-F5-AC19-03 — Từ chối submission thuộc người khác

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-19; API §8.4; BR-AI-06; validation §4; SEC5 ownership rule |
| Kind / priority | Security, P1 — ngăn truy cập chéo người dùng |
| Environment | API integration với JWT A và DB submission của B |
| Preconditions | B sở hữu Accepted `sub-B`; A không có quyền với submission này |
| Test data | A gửi `entryType=SUBMISSION, submissionId=sub-B` |
| Steps | POST create bằng JWT A; xác nhận response không tiết lộ nội dung submission; kiểm DB/provider. |
| Expected | HTTP 403 `AIR-302`; không tạo session/turn/usage; không gọi LLM; không lộ snapshot/source của B. |
| Cleanup | Không có record cần dọn ngoài request audit fixture. |
| Evidence | Response; DB counts; provider log; assertion không chứa submission content. |
| Design review | Draft; execution `Chưa thực hiện` |

### TC-F5-AC20-01 — Mở phiên QUESTION_BANK không cần submission

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-20; API §8.2–8.6; BR-AI-06; validation §4 |
| Kind / priority | Normal, P1 |
| Environment | API integration; fake LLM request capture |
| Preconditions | A không sở hữu submission Accepted nào; budget/quota đạt |
| Test data | `entryType=QUESTION_BANK`, `questionRef=11111111-1111-4111-8111-111111111111`, `questionText="Explain how a hash map handles collisions."`, level `MIDDLE`, maxTurns=12, hintAllowed=false |
| Steps | POST create; inspect provider request roles/content and persisted session. |
| Expected | 201 dù không có Accepted submission; entry type và questionRef được lưu; opening turn lưu; question text xuất hiện như user data trong provider input, không thành system instruction. `interview_sessions` không lưu questionText theo API. |
| Cleanup | Xoá session fixture và related rows. |
| Evidence | Request/response; DB rows; captured system/user messages; provider count. |
| Design review | Draft; execution `Chưa thực hiện` |

### TC-F5-AC20-02 — Thiếu questionText bị từ chối trước khi gọi AI

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-20; API §8.4; validation §4 |
| Kind / priority | Abnormal, P1 |
| Environment | API integration |
| Preconditions | A đăng nhập; fake LLM capture sẵn; quota đạt |
| Test data | QUESTION_BANK với UUID hợp lệ nhưng `questionText` bị thiếu |
| Steps | POST create; đối chiếu DB/provider/quota. |
| Expected | HTTP 400 `AIR-V03` (thiếu trường theo entryType); không tạo session, turn, usage; không gọi LLM. |
| Cleanup | Reset fixture/quota state. |
| Evidence | Error response; DB query; provider call count. |
| Design review | Draft; execution `Chưa thực hiện` |

### TC-F5-AC21-01 — Từ chối maxTurns không thuộc tập cho phép

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-21; API §8.2, §8.4; validation §4 |
| Kind / priority | Boundary, P1 |
| Environment | API integration |
| Preconditions | A đăng nhập; request TOPIC_SELECTION đủ trường khác; quota đạt |
| Test data | `topics=["Graphs"]`, `interviewerLevel=JUNIOR`, `maxTurns=10`, `hintAllowed=true` |
| Steps | POST create; kiểm trạng thái và provider. |
| Expected | HTTP 400 `AIR-V05`; không session/turn/usage; không LLM call. |
| Cleanup | Reset fixture. |
| Evidence | Response, DB baseline/delta, provider call count. |
| Design review | Draft; execution `Chưa thực hiện` |

### TC-F5-AC21-02 — Từ chối interviewerLevel ngoài enum

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-21; API §8.2, §8.4; validation §4 |
| Kind / priority | Abnormal, P1 |
| Environment | API integration |
| Preconditions | A đăng nhập; TOPIC_SELECTION request hợp lệ ngoại trừ level |
| Test data | topics=["Graphs"], maxTurns=8, hintAllowed=true, `interviewerLevel=EXPERT` |
| Steps | POST create; inspect DB/provider. |
| Expected | HTTP 400 `AIR-V04`; không session, turn, usage hoặc LLM call. |
| Cleanup | Reset fixture. |
| Evidence | Response; DB delta; provider call count. |
| Design review | Draft; execution `Chưa thực hiện` |

### TC-F5-AC21-03 — Từ chối danh sách chủ đề rỗng

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-21; API §8.2, §8.4; validation §4 |
| Kind / priority | Boundary, P1 |
| Environment | API integration |
| Preconditions | A đăng nhập; các trường tự luyện còn lại hợp lệ |
| Test data | `topics=[]`, level JUNIOR, maxTurns=8, hintAllowed=true |
| Steps | POST create; kiểm DB/provider. |
| Expected | HTTP 400 `AIR-V06`; không record hoặc LLM call. |
| Cleanup | Reset fixture. |
| Evidence | Response, DB delta, provider count. |
| Design review | Draft; execution `Chưa thực hiện` |

### TC-F5-AC21-04 — Từ chối hơn 10 chủ đề

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-21; API §8.2, §8.4; validation §4 |
| Kind / priority | Boundary, P1 |
| Environment | API integration |
| Preconditions | A đăng nhập; level/maxTurns/hint hợp lệ |
| Test data | `topics=["Topic01",..."Topic11"]` (11 distinct values; each 7 chars) |
| Steps | POST create; inspect response/DB/provider. |
| Expected | HTTP 400 `AIR-V06`; no session/turn/usage and no LLM call. |
| Cleanup | Reset fixture. |
| Evidence | Request body; response; DB delta; provider count. |
| Design review | Draft; execution `Chưa thực hiện` |

### TC-F5-AC21-05 — Từ chối một chủ đề dài 61 ký tự

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-21; API §8.2, §8.4; validation §4 |
| Kind / priority | Boundary, P1 |
| Environment | API integration |
| Preconditions | A đăng nhập; mọi trường còn lại hợp lệ |
| Test data | `topics=["T" repeated 61]` |
| Steps | POST create; verify validation and side effects. |
| Expected | HTTP 400 `AIR-V06`; không session/turn/usage; không gọi LLM. |
| Cleanup | Reset fixture. |
| Evidence | Exact topic length; response; DB/provider delta. |
| Design review | Draft; execution `Chưa thực hiện` |

### TC-F5-AC21-06 — Từ chối tổ hợp trường không đúng entryType

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-21; API §8.2, §8.4; validation §4 |
| Kind / priority | Abnormal, P1 — kiểm ràng buộc chéo lối vào |
| Environment | API integration |
| Preconditions | A đăng nhập |
| Test data | `entryType=TOPIC_SELECTION`, topics=["Graphs"], level=JUNIOR, maxTurns=8, hintAllowed=true, đồng thời gửi `submissionId=sub-A` |
| Steps | POST create; verify errors and no partial create. |
| Expected | HTTP 400 `AIR-V03`; không session, turn, usage hoặc LLM call. |
| Cleanup | Reset fixture. |
| Evidence | Request/response; DB delta; provider count. |
| Design review | Draft; execution `Chưa thực hiện` |

### TC-F5-AC21-07 — Chấp nhận boundary hợp lệ cho maxTurns và topic length

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-21; API §8.2–8.3; validation §4 |
| Kind / priority | Boundary, P1 |
| Environment | API integration; fake LLM |
| Preconditions | A đăng nhập, quota đạt; use isolated session per value |
| Test data | Lần lượt `maxTurns=8`, `12`, `16`; interviewer levels `INTERN`, `JUNIOR`, `MIDDLE`, `SENIOR`; one topic of exactly 60 chars; each request otherwise valid. |
| Steps | POST one request per maxTurns and level; POST one 60-char topic case. |
| Expected | Mỗi request 201; persisted/returned values match request; topic length 60 accepted. No additional behavior inferred. |
| Cleanup | Xoá các session và related rows. |
| Evidence | Per-request response and DB projection. |
| Design review | Draft; execution `Chưa thực hiện` |

### TC-F5-AC22-01 — Lỗi sinh opening không ghi phiên và retry được ngay

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-22; API §8.4–8.6; BR-AI-04, BR-AI-06; validation §9 |
| Kind / priority | Recovery, P1 |
| Environment | API integration; controllable LLM; Redis quota capture |
| Preconditions | A unlocked, under quota, no cooldown; first provider call times out/fails as `PROVIDER_ERROR`; second returns valid opening. |
| Test data | Valid TOPIC_SELECTION body: topics=["Graphs"], JUNIOR, 8, hintAllowed=true |
| Steps | POST create once; capture counts and cooldown/rate keys. Restore provider; POST same request immediately. |
| Expected | First: 503 `AIR-501`, no session/turn/usage, no retained cooldown or consumed hourly slot. Second: 201 and one persisted opening, one successful-call usage row. |
| Cleanup | Delete successful session rows and clear A's test quota/cooldown keys. |
| Evidence | Both responses; DB snapshots; provider log; Redis key snapshots. |
| Design review | Draft; execution `Chưa thực hiện` |

### TC-F5-AC22-02 — Learner bị khoá ngân sách không thể mở phiên; admin vẫn mở được

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-22; API §8.4; BR-AI-05/06; validation §9 |
| Kind / priority | Security/Abnormal, P1 — role-specific budget gate |
| Environment | API integration; budget row and LLM call capture |
| Preconditions | A là STUDENT với `locked_at` khác NULL; admin `adm-1` cũng thuộc fixture budget đang khóa; quota/cooldown sạch. |
| Test data | Same valid TOPIC_SELECTION request, once as A and once as admin |
| Steps | A POST; then admin POST using same valid body. |
| Expected | A nhận 429 `AIR-401`, không gọi LLM, không tạo record. ADMIN bỏ qua bước khóa ngân sách và phiên tạo thành công; không khẳng định mức quota khác quy tắc API. |
| Cleanup | Xoá admin session and related rows; restore budget row and quota keys. |
| Evidence | Responses; DB; provider calls; actor roles and budget snapshot. |
| Design review | Draft; execution `Chưa thực hiện` |

### TC-F5-AC22-03 — Phiên thứ tư trong cửa sổ giờ bị từ chối

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-22; API §8.4; BR-AI-04; validation §9 |
| Kind / priority | Boundary, P1 — ngưỡng 3 phiên/giờ |
| Environment | API integration; fixed UTC clock and quota storage |
| Preconditions | Config `interview_per_hour=3`; A đã có ba successful MOCK_INTERVIEW starts trong cùng `epochHour`; cooldown absent and budget unlocked. |
| Test data | Valid TOPIC_SELECTION request |
| Steps | POST fourth create attempt; inspect error, DB/provider, rate key. |
| Expected | 429 `AIR-403`; no session/turn/usage and no LLM call; rejected increment rolled back and just-set cooldown removed per BR-AI-04. |
| Cleanup | Clear A/hour counter and cooldown fixture. |
| Evidence | Response; DB delta; provider count; Redis state before/after. |
| Design review | Draft; execution `Chưa thực hiện` |

### TC-F5-AC22-04 — Câu opening sai schema không tạo phiên

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-22 failure branch; API §8.4–8.6; validation §9; BR-AI-06 |
| Kind / priority | Abnormal, P1 — malformed provider output atomicity |
| Environment | API integration; fake LLM returns invalid/missing opening message schema |
| Preconditions | A unlocked and quota eligible |
| Test data | Valid TOPIC_SELECTION; fake response lacks required non-empty opening message |
| Steps | POST create; inspect DB/provider/quota keys. |
| Expected | 502 `AIR-502`; no session, turn, usage row from a rejected completion according to create transaction rule; no partial open session. API specifies schema error distinct from provider error. |
| Cleanup | Clear quota/cooldown keys and reset DB. |
| Evidence | Response; DB; provider raw fixture and call log; quota keys. |
| Design review | Draft; execution `Chưa thực hiện` |

### TC-F5-AC23-01 — Dựng lại hội thoại từ PostgreSQL khi mất Redis

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-23; API §9.1–9.4; BR-AI-10; DB5:142–146 |
| Kind / priority | Recovery, P1 — durable transcript and context reconstruction |
| Environment | API integration; PostgreSQL and Redis; provider capture |
| Preconditions | A owns running topic session `sess-A`, with opening + 3 completed Q/A pairs in `interview_turns`; stage and turn_count equal stored lifecycle; delete Redis ChatMemory key. |
| Test data | Persisted turn indices 1..7 with distinct contents and stage_at_turn tags matching lifecycle; next LLM response valid. |
| Steps | GET session; send next answer using current expectedTurnCount; inspect reconstructed provider context; GET again. |
| Expected | GET returns all persisted turns in index order and current stage/count. Next request proceeds using reconstructed context from turns and opening message; successful response increments count once. Redis is rebuilt; no existing transcript overwritten. |
| Cleanup | Remove session fixture/rows and Redis key. |
| Evidence | GET body; DB ordered rows; provider request transcript; post-turn count; Redis rebuilt key. |
| Design review | Draft; execution `Chưa thực hiện` |

### TC-F5-AC23-02 — Người không sở hữu phiên không đọc được lịch sử

| Field | Value |
| :--- | :--- |
| AC / DD source | AC-23; API §9.1, §9.4; BR-AI-16 |
| Kind / priority | Security, P1 — owner isolation |
| Environment | API integration; JWT A/B |
| Preconditions | A owns `sess-A` with transcript containing sentinel `A-PRIVATE-TURN`; B has no access |
| Test data | GET `/interview-sessions/sess-A` using JWT B |
| Steps | Request detail; inspect response and any logs accessible to client. |
| Expected | HTTP 403 `AIR-302`; transcript and sentinel are not returned. No DB mutation/provider call. |
| Cleanup | Remove fixture session/turns. |
| Evidence | Response; DB unchanged check; provider count. |
| Design review | Draft; execution `Chưa thực hiện` |
