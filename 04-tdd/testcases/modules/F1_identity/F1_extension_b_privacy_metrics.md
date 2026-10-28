# F1 extension B — Export cá nhân và số liệu tổng hợp

- **Scope/TDD:** F1 AC-56–63, 75; `04-tdd/modules/F1_identity.md`.
- **DD:** `03-dd/api/F1_identity.md` §§0.2, 0.7, 0.8; `03-dd/logic/F1_identity.md` BR-17, BR-19; `03-dd/validation/F1_identity.md`.
- **Fixture catalog:** đồng hồ cố định `2026-10-27T10:00:00+07:00`; U1 học viên, U2 học viên khác; A1 ADMIN có các quyền yêu cầu; A2 ADMIN chỉ đọc audit; I1 INSTRUCTOR. Dữ liệu người dùng/hoạt động giả lập cô lập, mỗi lần có ID riêng. Cần API+DB và các port F2/F4 mock tích hợp có thể fault-inject; các ca không yêu cầu mock chỉ dùng backend thật.
- **Execution:** Tất cả `Chưa thực hiện`; không có run record.

| AC | Nhánh | Case | Review |
|---|---|---|---|
| 56 | export riêng tư, rate limit theo scope | TC-F1-AC56-01, TC-F1-AC56-02 | Reviewed 2026-10-28 |
| 57 | export thành công; lỗi dịch vụ và retry | TC-F1-AC57-01, TC-F1-AC57-02 | Reviewed 2026-10-28 |
| 58 | phân trang gộp; lọc lớp, rời lớp, ngoài phạm vi | TC-F1-AC58-01, TC-F1-AC58-02 | Reviewed 2026-10-28 |
| 59–60 | số liệu tổng quan; port lỗi/retry; mẫu số rỗng và loại lỗi hệ thống | TC-F1-AC59-01, TC-F1-AC59-02, TC-F1-AC60-01, TC-F1-AC60-02 | Reviewed 2026-10-28 |
| 61 | số tổng hợp xuyên trang và quyền | TC-F1-AC61-01 | Reviewed 2026-10-28 |
| 62 | đọc khối audit; SSE reconnect/dedupe; role denial | TC-F1-AC62-01, TC-F1-AC62-02 | Reviewed 2026-10-28 |
| 62 | SSE expired anchor resync, heartbeat and redaction | TC-F1-AC62-03 | Đã review; chưa thực thi |
| 63 | invalid/unauthorized; authorized update + audit | TC-F1-AC63-01, TC-F1-AC63-02 | Reviewed 2026-10-28 |
| 75 | progress arithmetic; activity ownership/window; empty eligible denominator | TC-F1-AC75-01, TC-F1-AC75-02, TC-F1-AC75-03 | Reviewed 2026-10-28 |
| 59 | JOB-ID-04 retention preserves dashboard token history | TC-F1-AC59-03 | Đã review; chưa thực thi |
| 63 | JOB-ID-05 DELETE/ARCHIVE, cutoff and retry | TC-F1-AC63-03 | Đã review; chưa thực thi |
| 75 | JOB-ID-03 feed cutoff, batching and retry | TC-F1-AC75-04 | Đã review; chưa thực thi |

### TC-F1-AC56-01 — Export dữ liệu chỉ của chủ thể, giới hạn cùng loại

| Field | Value |
|---|---|
| AC / DD source | AC-56; API §0.3 #16 |
| Kind / priority | Security; P1 — rò dữ liệu cá nhân |
| Environment | API + DB; đồng hồ kiểm soát cho rate limit |
| Preconditions | U1 và U2 có hồ sơ/bài nộp khác biệt rõ; U1 đã đăng nhập. |
| Test data | U1: email `u1@example.test`, profile marker `PROFILE-U1`, submissions `S-U1A`,`S-U1B`; U2: `u2@example.test`, markers `PROFILE-U2`,`S-U2A`. Scope `profile` và `submissions`. |
| Steps | U1 GET #16 lần lượt scope=profile và scope=submissions; lưu hai file. |
| Expected | Scope profile chỉ xuất dữ liệu hồ sơ U1; submissions chỉ xuất bài nộp U1. API hỗ trợ đúng hai scope `profile|submissions`. |
| Cleanup | Xóa U1/U2 fixture và file export tạm. |
| Evidence | Request/response, hash và nội dung file đã khử định danh, truy vấn DB xác nhận không truy xuất U2. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC57-01 — Export transcript khi dịch vụ phỏng vấn hoạt động

| Field | Value |
|---|---|
| AC / DD source | AC-57; API §0.3 #17; F5 export port |
| Kind / priority | Normal/Security; P1 — dữ liệu transcript cá nhân |
| Environment | FE + API + DB; interview service stub |
| Preconditions | U1 signed in on personal export surface and has transcript T1; U2 has transcript T2; service succeeds. |
| Test data | Transcript marker `ONLY-U1-7F2`; U2 marker `ONLY-U2-91C`. |
| Steps | U1 clicks export interview transcripts in `USR0503_settings`; save downloaded file. |
| Expected | UI starts `GET /me/export/interview-transcripts`; download contains U1 transcript only, not marker `ONLY-U2-91C`. |
| Cleanup | Xóa transcript fixtures và tắt lỗi stub. |
| Evidence | API/UI capture, upstream fault log, kiểm tra marker trong file và audit access. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC56-02 — Rate limit scope độc lập và không thể chuyển chủ thể export

| Field | Value |
|---|---|
| AC / DD source | AC-56; API #16; security F1 §3 |
| Kind / priority | Security/Boundary; P1 — vượt hạn mức hoặc lộ hồ sơ khác |
| Environment | API + DB, clock-controlled rate limiter |
| Preconditions | U1,U2 có fixtures riêng; U1 export profile ở T0. |
| Test data | U1 marker `PRIVATE-U1`; U2 marker `PRIVATE-U2`; scopes `profile`, `submissions`; T0 fixed. |
| Steps | 1. U1 request export profile lần nữa tại T0+4:59. 2. U1 export submissions tại T0+4:59. 3. U1 export profile tại T0+5:00. 4. Gửi `userId=U2` nếu client tự thêm query; mở tệp bằng U1 token. |
| Expected | Profile retry trước đủ 5 phút bị rate-limit; submissions vẫn được vì quota theo scope; profile tại đủ 5 phút được phép. Tệp U1 không chứa marker U2 dù client gắn tham số chọn user; server chỉ dùng userId từ token (tham số không được mở quyền đọc U2). |
| Cleanup | Xóa file/key rate limiter và user fixtures. |
| Evidence | Timestamps, responses, nội dung export đã khử định danh. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC57-02 — Transcript export suy giảm rồi hồi phục

| Field | Value |
|---|---|
| AC / DD source | AC-57; API #17; F5 export port |
| Kind / priority | Recovery; P1 — lỗi dịch vụ làm mất khả năng retry |
| Environment | FE + API + DB + interview port stub |
| Preconditions | U1 transcript tồn tại; stub trả timeout có thể bật/tắt. |
| Test data | U1 marker `ONLY-U1-7F2`; U2 marker `ONLY-U2-91C`; upstream timeout=2s. |
| Steps | 1. U1 clicks export in `USR0503_settings` with timeout enabled. 2. Verify error state/button, recover service and click retry. 3. Sign in as U2 and export. |
| Expected | First request returns 503 and the screen shows a retryable error without permanently disabling export. Retry downloads U1's file. U2 receives only U2 transcript; no cross-user marker is exposed. |
| Cleanup | Tắt stub lỗi; xóa transcript fixtures. |
| Evidence | Responses, stub logs, marker scan. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC58-01 — Danh sách học viên gộp các lớp trước khi phân trang

| Field | Value |
|---|---|
| AC / DD source | AC-58; API #59; validation §11 |
| Kind / priority | Boundary; P1 — bỏ sót/lộ học viên |
| Environment | API + DB; cần hai lớp của I1 và lớp của I2 |
| Preconditions | I1 sở hữu C1,C2; I2 sở hữu C3. S1 ghi danh C1 và C2; S2 chỉ C2; S3 từng ghi danh C1 nhưng `left_at` đã đặt. |
| Test data | C1/C2/C3 UUID cố định; gọi `GET /api/v1/identity/instructors/my/students` theo API #59 với `page=1&pageSize=1`, `page=2&pageSize=1`; lọc `classId=C2`; thử `classId=C3`. |
| Steps | 1. Lấy trang 1 và 2 của tab tất cả. 2. Lấy danh sách lọc C2. 3. Gọi classId C3 từ I1. |
| Expected | Gộp theo từng cặp lớp–học viên còn ghi danh rồi phân trang trên tập đã lọc; S1 có hai bản ghi theo hai lớp; S3 không được tính; lọc C2 chỉ trả S1,S2 trong lớp đó. C3 trả 404 IDT-414. Params sai trả 400 IDT-V17. |
| Cleanup | Xóa enrollments/lớp/users fixture theo thứ tự FK. |
| Evidence | JSON các trang, DB enrollment snapshot, response ngoài phạm vi. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC58-02 — Lọc đúng lớp và loại enrollment đã rời

| Field | Value |
|---|---|
| AC / DD source | AC-58; API #59; validation §11 |
| Kind / priority | Security; P1 — danh sách lớp lẫn hoặc lộ lớp ngoài quyền |
| Environment | API + DB, JWT I1/I2 |
| Preconditions | I1 owns C1,C2; I2 owns C3; S1 active C1/C2; S2 active C2; S3 left C2. |
| Test data | Query `classId=C2`, `page=1`, `pageSize=100`; C3 UUID valid. |
| Steps | 1. I1 filters by C2. 2. I1 filters by C3. 3. I1 requests invalid page=0. |
| Expected | C2 result contains exactly active pairs `(C2,S1)` and `(C2,S2)`; S3 absent. C3 returns 404 IDT-414. Invalid pagination returns 400 IDT-V17. |
| Cleanup | Remove enrollments and classes. |
| Evidence | Responses and enrollment table snapshot. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC59-02 — Một dashboard port lỗi không làm hỏng widget khác; retry thành công

| Field | Value |
|---|---|
| AC / DD source | AC-59; API §0.8.5 #68 and #42; BR-17 |
| Kind / priority | Recovery; P1 — lỗi widget bị che hoặc lan rộng |
| Environment | API + F4 port stub + ADMIN JWT |
| Preconditions | #42 có seed kết quả; #68 port có mode fail/success; mỗi endpoint cache độc lập. |
| Test data | #42 expected six monthly groups; #68 expected five verdict slices after recovery. |
| Steps | 1. Stub GetVerdictDistribution fails; call #68 and #42. 2. Recover port; invalidate/retry #68 per block action; call #42 again. |
| Expected | #68 returns 503 rather than zero; #42 returns its correct groups while #68 fails. Retry #68 succeeds with five slices; failure/retry does not change #42 result/cache. |
| Cleanup | Clear stub/cache and remove seeds. |
| Evidence | API outputs, port traces, cache observations. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC59-01 — Các widget admin truy vấn độc lập và giữ lỗi port

| Field | Value |
|---|---|
| AC / DD source | AC-59; API §0.8.5 #41–43, #67–73; BR-17 |
| Kind / priority | Recovery; P1 — số liệu dashboard sai hoặc che lỗi |
| Environment | API, F2/F4 ports có fault injection; ADMIN có quyền dashboard |
| Preconditions | ADMIN có quyền dashboard. Tất cả read ports thành công và cache rỗng. |
| Test data | Fixed clock `2026-10-27T10:00:00+07:00`. #41: 3 distinct token users in last 30d, 2 in preceding 30d. #42: months M-5…M, month M has 2 new active users and 1 returning token user. #43 F4 port fixture returns total 7 submissions. #67 20 date labels and arrays JAVA=`[1]×20`, CPP=`[2]×20`, PYTHON=`[0]×20`. #68 port fixture has final verdict counts ACCEPTED=2, WRONG_ANSWER=1, other three=0. #69 F2 levels L1(sort=1),L2(sort=2); F4 counts L1=3/1 accepted, L2=1/0. #70 daily counts `[1,0,2,0,0,1,0]`. #71 monthly counts `[1,0,2,0,0,1]`. #72 F2 eligible published/nondeleted problems P1=4,P2=4,P3=2,P4=1; draft P0=99 excluded. #73 reset-request fixture has count 2. |
| Steps | Call each of #41,#42,#43,#67–73 independently without userId/query override; compare each response with the seeded source. |
| Expected | #41 value=3; #42 has six month groups with month M newUsers=2/returningUsers=1; #43 value=7. #67 has 20 date labels and exactly JAVA/CPP/PYTHON series with values `[1]×20`, `[2]×20`, `[0]×20`. #68 has five verdict slices with counts `[2,1,0,0,0]` and server-calculated percentages. #69 includes both levels in sort_order with L1=(3 submissions,1 accepted), L2=(1,0). #70 returns seven ordered days, weekTotal=4, dailyAverage=4/7. #71 returns six points and monthTotal=4. #72 excludes P0 and returns P1/P2/P3/P4 by count descending then ID ascending for ties, max four. #73 value=2. Each endpoint remains an independent contract. |
| Cleanup | Xóa seed ở F1/F2/F4 và cache block. |
| Evidence | Request/response từng endpoint, port traces, DB seed snapshot, cache key/timestamp. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC60-02 — SYSTEM_ERROR không thuộc mẫu số acceptance ratio

| Field | Value |
|---|---|
| AC / DD source | AC-60; API #68; F4 stats port; BR-17 |
| Kind / priority | Boundary; P1 — tỷ lệ chấp thuận sai khi có lỗi hạ tầng |
| Environment | API + F4 port, ADMIN |
| Preconditions | Có 4 submissions cuối: 1 ACCEPTED, 1 WRONG_ANSWER, 2 SYSTEM_ERROR; verdict categories fixed per DD. |
| Test data | Final non-system-error denominator=2; accepted=1; total including SYSTEM_ERROR=4. |
| Steps | Call #68 and inspect all five verdict slices and percentages. |
| Expected | No SYSTEM_ERROR slice. Percentages use the two eligible final verdicts: ACCEPTED=50%, WRONG_ANSWER=50%, other slices=0%; system errors are not included in percentage denominator. Client receives server-calculated values and does not recalculate. |
| Cleanup | Remove F4 seed. |
| Evidence | Response and F4 source snapshot. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC60-01 — Tập rỗng không tạo tỷ lệ giả

| Field | Value |
|---|---|
| AC / DD source | AC-60; API §0.8.5 #43,#68–69 |
| Kind / priority | Boundary; P1 — chia cho 0 và sai thống kê |
| Environment | API + F4/F2 stats fixtures |
| Preconditions | Không có submission cuối cùng; một level F2 có tồn tại nhưng không có submission. |
| Test data | Seed zero submission ở các kỳ liên quan; 5 verdict categories, level `L0`. |
| Steps | Gọi #43, #68, #69. |
| Expected | Các số đếm khác được trả theo nguồn; mọi percent verdict khi tổng bằng 0 là 0; level vẫn hiện với submission/accepted bằng 0; không chia cho 0, không bịa tỷ lệ hoặc tính lỗi hệ thống vào mẫu số. |
| Cleanup | Xóa zero-data fixtures. |
| Evidence | Response JSON và DB/port query snapshot. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC62-02 — SSE resumes after last event and rejects non-admin

| Field | Value |
|---|---|
| AC / DD source | AC-62; API #66; §0.7.2; BR-19 |
| Kind / priority | Recovery/Security; P1 — duplicate or unauthorized audit stream |
| Environment | API/SSE, DB and two principals |
| Preconditions | A1 ADMIN has seen event L1; L2 is a later audit; STUDENT N1. |
| Test data | `Last-Event-ID=L1`; L2 payload includes safe marker and a sentinel in secret-named field. |
| Steps | 1. Connect SSE as A1 with Last-Event-ID L1. 2. Capture L2 frame. 3. Reconnect from L2 ID. 4. N1 attempts stream. |
| Expected | A1 receives only later `audit-log` events; reconnect does not replay L2. Frame is redacted DTO and excludes token/OTP/raw before/after JSON; `Cache-Control: no-store`. N1 receives no stream data and is denied. |
| Cleanup | Close streams and remove logs. |
| Evidence | SSE frames/headers and auth response. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC61-01 — Stats người dùng không phụ thuộc trang hiện tại

| Field | Value |
|---|---|
| AC / DD source | AC-61; API #35,#62–63, §0.6.0 |
| Kind / priority | Security; P1 — tổng hợp quản trị sai/không phân quyền |
| Environment | API + DB, JWT ADMIN và STUDENT |
| Preconditions | 23 users có vai trò/trạng thái xác định; #35 `pageSize=20`, pending users ở trang sau. |
| Test data | Active users 1–20: STUDENT=10, INSTRUCTOR=7, ADMIN=3. Pending users 21–22: STUDENT=1, INSTRUCTOR=1. User 23 is DEACTIVATED with stored STUDENT role. Thus #62 expected=2; #63 groups STUDENT=11, INSTRUCTOR=8, ADMIN=3, DEACTIVATED=1, total=23. |
| Steps | ADMIN gọi #35 trang 1 rồi #62/#63; thay filter/trang #35 rồi gọi #62/#63; STUDENT gọi #62/#63. |
| Expected | #62=2, độc lập page/filter. #63 trả theo thứ tự STUDENT=11 (47.8%), INSTRUCTOR=8 (34.8%), ADMIN=3 (13.0%), DEACTIVATED=1 (4.3%); deactivated account không được tính lại trong STUDENT. STUDENT bị 403. DB error trả 503, không giả số 0. |
| Cleanup | Xóa users/roles fixtures. |
| Evidence | API JSON, quyền 403, DB aggregate đối chiếu. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC63-02 — Authorized retention update persists and is audited

| Field | Value |
|---|---|
| AC / DD source | AC-63; API #51–52; BR-19/20 |
| Kind / priority | Normal/Security; P1 — policy update missing audit |
| Environment | API + DB, A1 with SYSTEM_AUDIT_LOG |
| Preconditions | Current setting `365/ARCHIVE`; A1 authorized. |
| Test data | Update to `30/DELETE`; audit action `AUDIT_LOG_SETTINGS_CHANGED`. |
| Steps | PUT valid values; GET #51; query matching audit record. |
| Expected | New setting is returned and persisted. One audit entry carries before `{365,ARCHIVE}` and after `{30,DELETE}` values. |
| Cleanup | Restore original setting and clean audit row. |
| Evidence | API response, setting row, audit row. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC62-01 — Read audit blocks và SSE không nhân đôi, giới hạn ADMIN

| Field | Value |
|---|---|
| AC / DD source | AC-62; API #64–66, §0.7.2; BR-19 |
| Kind / priority | Security/Recovery; P1 — rò audit hoặc phát trùng |
| Environment | API/SSE, DB audit logs, hai ADMIN và STUDENT |
| Preconditions | Có năm audit actions của hai ADMIN trong bảy ngày gần nhất; trong 24h gần nhất A1 thực hiện 3, A2 thực hiện 2. |
| Test data | A1: AUTH×2 và PERMISSION×1, lastActionAt=T-1h. A2: CONFIG×1 và CONTENT×1, lastActionAt=T-2h. T là clock chung trong fixture. |
| Steps | 1. Gọi #64/#65 bằng A1 và A2. 2. Gọi các endpoint bằng STUDENT. |
| Expected | #64 xếp A1 trước A2, actionCount lần lượt 3 và 2 trong 24h; #65 luôn có đủ AUTH=2, PERMISSION=1, CONFIG=1, CONTENT=1 cho bảy ngày. Non-admin không nhận audit data. |
| Cleanup | Xóa audit fixtures và đóng SSE clients. |
| Evidence | JSON, SSE frame + Last-Event-ID, kiểm tra các cột nhạy cảm vắng mặt. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC75-02 — Personal recent activity is bounded and never crosses users

| Field | Value |
|---|---|
| AC / DD source | AC-75; API #46,#48; BR-18 |
| Kind / priority | Security/Boundary; P1 — stale or cross-user activity/state |
| Environment | API + DB; JWT U1 and U2 |
| Preconditions | Activity feed has >10 records; all events are allowed activity_type values in F1 API §2. |
| Test data | U1 has 11 items within 90 days plus one 91 days old; U2 has marker `U2-ACTIVITY`. U1 best verdict P1=ACCEPTED, P2=WRONG_ANSWER; U2 has opposite statuses. Request problem IDs `[P1,P2]`. |
| Steps | U1 calls #46 and #48; repeat #48 with U2 token; attempt #48 P1/P2 and an unrequested P3. |
| Expected | #46 returns exactly the latest ten U1 activities within 90 days. #48 maps P1 to `SOLVED` and P2 to `ATTEMPTED` using U1's own best-verdict rows; no U2 marker/state is returned. |
| Cleanup | Delete activity/stats fixtures. |
| Evidence | Raw responses and DB ownership/window query. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC63-01 — Thay retention có quyền, validation và audit before/after

| Field | Value |
|---|---|
| AC / DD source | AC-63; API #51–52; BR-19/20 |
| Kind / priority | Security; P1 — thay chính sách lưu log trái phép |
| Environment | API + DB audit log; A1 có SYSTEM_AUDIT_LOG, A2 chỉ đọc |
| Preconditions | Cấu hình hiện tại `retentionDays=365`, `expiredPolicy=ARCHIVE`. |
| Test data | Candidate valid `{retentionDays:30,expiredPolicy:DELETE}`; invalid `{retentionDays:0,expiredPolicy:DELETE}`. |
| Steps | A1 GET #51; PUT `{retentionDays:0,expiredPolicy:DELETE}`; verify state. A2 thử PUT `{retentionDays:30,expiredPolicy:DELETE}` bằng request trực tiếp. |
| Expected | Invalid trả 422 IDT-442 và giữ nguyên cấu hình. A2 có thể GET nhưng PUT bị từ chối server-side; cấu hình và audit log không thay đổi bởi hai request bị từ chối. |
| Cleanup | Khôi phục cấu hình ban đầu và dọn audit fixture nếu môi trường cho phép. |
| Evidence | HTTP, DB `system_settings`, audit rows before/after. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC75-01 — Progress cá nhân loại system error khỏi acceptance ratio

| Field | Value |
|---|---|
| AC / DD source | AC-75; API #46–48 |
| Kind / priority | Normal/Boundary; P1 — số tiến độ sai và lộ trạng thái ngoài yêu cầu |
| Environment | API + PostgreSQL identity read models populated from the stated F4 SubmissionGraded fixtures |
| Preconditions | U1 có submissions Accepted, rejected, system error và ngày không submit; U2 có dữ liệu riêng. |
| Test data | U1: P1 has first Accepted then Wrong; P2 has SystemError only; P3 no submissions. Stats: total=3, accepted=1, AC-eligible=2, solved=1, first-try-accepted=1. U2 has own recent marker. Feed U1 has 11 events: newest 10 within 90 days, oldest at 91 days. |
| Steps | U1 GET #47 summary. |
| Expected | #47 returns solvedProblemCount=1, totalSubmissions=3 (includes system error), acceptedCount=1, acceptanceRatePercent=50 (1/2, rounded integer), firstTryAcceptedCount=1; topLanguage is `JAVA` from fixture counts JAVA=2, CPP=1. |
| Cleanup | Xóa submissions/stats fixtures. |
| Evidence | Request/response, đối chiếu submissions F4 và user ownership. |
| Design review | Reviewed 2026-10-28 |

### TC-F1-AC75-03 — No eligible submissions yields null rate, not zero or divide-by-zero

| Field | Value |
|---|---|
| AC / DD source | AC-75; API #47; BR-17 and SubmissionGraded event contract |
| Kind / priority | Boundary; P1 — fabricated rate when denominator is empty |
| Environment | API + DB |
| Preconditions | U1 has exactly two final system-error submissions and no other final outcomes. |
| Test data | Both SYSTEM_ERROR events have `language=JAVA`; `totalSubmissions=2`, `acceptedCount=0`, `ac_eligible_count=0`, `language_counts={JAVA:2}`, `top_language=JAVA`; no solved problems. |
| Steps | U1 GET #47. |
| Expected | `totalSubmissions=2`, `acceptedCount=0`, `solvedProblemCount=0`, `firstTryAcceptedCount=0`, `acceptanceRatePercent=null`, `topLanguage=JAVA`; SYSTEM_ERROR still counts toward language/day totals under API §2 while excluded from AC denominator. No division by zero or invented 0% ratio. |
| Cleanup | Delete U1 submissions and aggregate rows. |
| Evidence | JSON response and backing aggregate rows. |
| Design review | Reviewed 2026-10-28 |


### TC-F1-AC59-03 - Token cleanup preserves six-month dashboard history

| Field | Value |
|---|---|
| AC / DD source | AC-59; API #41/#42; JOB-ID-04 section 4 and jobs common advisory lock |
| Kind / priority | Boundary/Recovery; P1 - historical active-user counts |
| Environment | PostgreSQL + job runner + API + fixed DB clock T0=2026-10-27T10:00:00+07:00 |
| Preconditions | Isolated tokens issued at cutoff T0 minus 6 calendar months minus 1s (R1 revoked, E1 expired), exact cutoff (R2 revoked), cutoff+1s (R3 expired), and old-but-effective V1 (explicit fixture with expires_at>T0 and revoked_at=NULL). All have distinct IDs/users. |
| Test data | JOB-ID-04; snapshot #42 source counts. Fault trial has 5,001 eligible rows with failure in second batch; multi-instance trial holds job advisory transaction lock on runner A while runner B attempts entry. |
| Steps | Run boundary trial twice. Run fault trial, inspect committed/rolled-back batches, clear fault and rerun. Run lock trial. Query token rows and #42 history after each. |
| Expected | Only R1/E1 are deleted in boundary trial; R2/R3/V1 stay because age must be strictly older than cutoff and token ineffective. Rerun changes nothing. Fault trial commits first 5,000, rolls back second batch, then next run removes remaining one; logs ERROR/INFO without token values. Runner B skips while A owns advisory lock. No actor audit is written. Counts within retained six-month history are preserved. |
| Cleanup | Restore isolated token/user snapshot and reset faults/clock/locks. |
| Evidence | Fixed time, token ID diffs, batch transaction logs, lock outcomes and #42 responses/source queries. |
| Design review | Reviewed against jobs -4; not executed |

### TC-F1-AC63-03 - Audit retention deletes only expired rows and keeps ARCHIVE deferred

| Field | Value |
|---|---|
| AC / DD source | AC-63; BR-20; JOB-ID-05 section 5 and common advisory lock; API #51/#52 |
| Kind / priority | Boundary/Recovery; P1 - audit retention data loss |
| Environment | PostgreSQL + job runner + controlled clock T0=2026-10-27T10:00:00+07:00 + log observer |
| Preconditions | retentionDays=30; logs L1 at cutoff-1s, L2 exactly cutoff, L3 at cutoff+1s. Seed without actor-facing API so no extra change audit contaminates fixture. |
| Test data | Isolated trials DELETE and ARCHIVE; DELETE fault trial 5,001 expired rows with injected second-batch DB failure. Two runners for same-job advisory lock trial. |
| Steps | Run DELETE and compare IDs, then rerun. Restore fixture, set ARCHIVE and run. In fault trial run with fault then without; in lock trial hold first runner transaction before second entry. |
| Expected | DELETE removes L1 only (`created_at < T0 minus retentionDays`), preserves L2/L3 and rerun removes none. ARCHIVE preserves all rows and emits warning because archive is deferred. Fault trial preserves first committed batch of 5,000; failed second batch rolls back and is retried next run. Second runner skips held advisory lock. No scheduled job creates system_audit_logs; logs report count/error without exposing secret JSON. |
| Cleanup | Restore settings/audit snapshot, reset fault/clock and release locks. |
| Evidence | Settings, exact timestamps, row-ID diffs, batch logs and advisory lock observations. |
| Design review | Reviewed against BR-20/JOB-ID-05; not executed |

### TC-F1-AC75-04 - Feed cleanup keeps the exact 90-day boundary and retries failed batches

| Field | Value |
|---|---|
| AC / DD source | AC-75; API #46; JOB-ID-03 section 3 and common job rules |
| Kind / priority | Boundary/Recovery; P1 - personal activity retention |
| Environment | PostgreSQL + job runner + API + controlled DB clock T0=2026-10-27T10:00:00+07:00 |
| Preconditions | U1 owns rows F1 at T0 minus 90d minus 1s, F2 exactly T0 minus 90d, F3 at T0 minus 90d plus 1s; U2 recent marker remains separate. |
| Test data | Boundary trial and independent 5,001-expired-row fault trial; inject DB failure in second 5,000-row batch. Multi-instance trial uses job advisory lock. |
| Steps | Run job twice; inspect DB and GET #46 as U1. In fault trial inspect after failure, clear fault and rerun. Hold runner A lock while B enters. |
| Expected | F1 removed; F2/F3 persist because cutoff predicate is strictly `<`; #46 remains owner-scoped. Second boundary run is idempotent. Fault trial retains first batch deletion, rolls back second batch, and next run removes last eligible row. B skips when A holds lock. INFO records row count; ERROR records failed batch; no system audit with null actor and no AI-service dependency. |
| Cleanup | Restore feed snapshot, close job transactions and reset clock/fault. |
| Evidence | Timestamped fixture, row-ID diffs, #46 response, job/batch and lock traces. |
| Design review | Reviewed against JOB-ID-03; not executed |


### TC-F1-AC62-03 — SSE missing retention anchor requests resync and emits safe heartbeats

| Field | Value |
|---|---|
| AC / DD source | AC-62; API #66 section 0.7.2; BR-16/19 |
| Kind / priority | Recovery/Boundary/Security; P1 — stale reconnect cursor or audit leakage |
| Environment | API/SSE + PostgreSQL + controlled clock + ADMIN and STUDENT principals |
| Preconditions | A1 ADMIN; N1 STUDENT. L0 UUID is an old audit ID removed by retention. L1 is a current safe audit item with token/OTP sentinel only in raw protected snapshot, not safeParams. |
| Test data | Connect #66 with Last-Event-ID=L0; separate no-activity connection lasting at least 30s; N1 attempts same endpoint. |
| Steps | Capture initial event for missing anchor, reload #39 first page after resync, observe idle heartbeat at 30s, publish fresh L1 and capture frame; then call as N1. |
| Expected | Missing anchor emits `resync` without audit data; client can reload #39. Idle connection emits comment heartbeat each 30s, not fake audit rows. New `audit-log` frame has UUID id, redacted AuditLogItemDto, `Cache-Control:no-store`; no token/OTP/raw before_json/after_json. N1 returns `403 IDT-447` before stream/data access. |
| Cleanup | Close connections, remove logs/principals and reset clock. |
| Evidence | SSE frame timestamps/headers, resync payload, #39 reload, sentinel scan and rejected HTTP response. |
| Design review | Reviewed against SSE contract; not executed |
