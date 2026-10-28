# Audit thiết kế đợt 3 — F5 và F6

> Cập nhật 2026-10-28. **Đã review 14/14 cụm; chưa đóng thiết kế vì một quyết định schema còn mở. 0 ca thực thi.**

## Kết quả kiểm được

| Module | Cụm đã review | ID duy nhất | AC có trace | Oracle còn mở |
| :--- | ---: | ---: | ---: | :--- |
| [F5](modules/F5_ai-review/README.md) | 7/7 | 144 | 58/58 | AC01-04: mảng 11 phần tử |
| [F6](modules/F6_interview-bank/README.md) | 7/7 | 110 | 41/41 | AC10-02: mảng 11 phần tử |

Skill `algoprep-qa-audit` kiểm **254 ID / 254 trace, 0 lỗi cấu trúc, 2 semantic review flags** đúng hai ca trên. Đã đối chiếu AC body, DD API/logic/validation/jobs, fixture, response và DB/token/event/audit side effects. Các ca bổ sung giữ nguyên ID cũ; mới thêm suffix và trace. Kết quả này là review tài liệu, không phải sản phẩm đã pass.

## Inventory review theo từng file

| File | AC | Ca | Nhánh/oracle đối chiếu và kết quả |
| :--- | :--- | ---: | :--- |
| [F5 solution](modules/F5_ai-review/F5_solution_review.md) | 01–17 | 29 | API4–7, BR02/03/04/16, validation2/3: idempotent khác cache hit, hash/prompt version, ownership, quota/reset/refund, schema reject/normalize, upstream fault, reads/degradation. Thêm normalization/upstream ca; một overflow oracle còn mở. |
| [F5 start](modules/F5_ai-review/F5_mock_session_start.md) | 18–23 | 21 | API8/9, BR06/10, validation4: ba entryType, required/forbidden fields, topic dedup, 1/2000-char question text, ignored SUBMISSION options, quota/lock/schema failure, DB reload. Thêm input matrix và kiểm ownership/notfound cho GET/turn/hint/complete. |
| [F5 turns/hints](modules/F5_ai-review/F5_mock_turns_hints.md) | 24–31 | 17 | API2/10/11, BR07/08, validation5/10: commit-before-SSE, control-field privacy, stage/ceil/maxTurns, session lock, reconnect/reuse USER, injection, hint side effects. Sửa fault trước accepted: validation AIR-V09; provider timeout luôn sau accepted. |
| [F5 scoring/lifecycle](modules/F5_ai-review/F5_mock_scoring_lifecycle.md) | 32–37 | 15 | API12/13, BR09, JOB04: fixed weighted score, schema/out-of-range/provider error, zero turns, USER/SWEEP race, 4-turn/15-minute threshold, retry3 failures, summary complete-only/ownership/range/degradation. Sửa loser user=409 AIR-113, thêm retry-exhausted subcase. |
| [F5 admin](modules/F5_ai-review/F5_admin_prompt_budget_usage.md) | 38–48 | 35 | API14–31, BR05/13, validation7/8: independent grants, draft/ACTIVE/archive, atomic publish/CAS/no-op, budget/forecast, unlock, config bounds, prices/rankings/privacy. Sửa phép tính dự báo và thêm hai ca audit rollback AIR-503; mở rộng input matrices. |
| [F5 ports/events](modules/F5_ai-review/F5_admin_ports_events_resilience.md) | 49–54 | 17 | API32/33, BR14/15, JOB01: cross-module budget contracts, P3 input/privacy/quota, stored export, generated/cache-hit events vs replay, broker confirm vs consumer ack, outage and hidden sentinel. Sửa các oracle xuyên module theo DD và quyết định hiện hành. |
| [F5 jobs](modules/F5_ai-review/F5_admin_jobs_budget_lifecycle.md) | 55–58 | 10 | JOB01–05: monthly rollover/catch-up/idempotency, baseline/ratio/open dedup, budget exemptions, stale/fresh session race, sent-only retention/multi-instance. ADMIN baseline theo quyết định mới; bổ sung ratio đúng5 và retention đúng90 ngày. |
| [F6 browse/recall](modules/F6_interview-bank/F6_learner_browse_recall.md) | 01–09 | 13 | API2–5, BR04–06, validation2/3: filters/counts, status-vs-due, three drill groups, retired visibility/retention, bookmark idempotency/ownership, learn-without-AI, recall intervals/commit order. Thêm strict bookmark/recall and unknown/retired guard matrix. |
| [F6 practice/history](modules/F6_interview-bank/F6_learner_practice.md) | 10–21 | 23 | API6/7/25/26, BR07/08, validation4/7: immutable attempts/snapshot, injection, FAILED+usage/refund matrix, hourly/budget/role, lengths, sequence race, cursor/history privacy. Sửa 12 attempts thành 10+2 pages, thêm cursor-invalid/fixed-limit; một overflow oracle còn mở. |
| [F6 progress](modules/F6_interview-bank/F6_learner_progress.md) | 22–23 | 4 | API8, BR09: distinct ACTIVE attempted questions, FAILED vẫn tính, own-user scope, zero topics, soft-delete/topic move recalc and historical attempt retention. Oracle hiện hành nhất quán; không cần thêm ID. |
| [F6 catalogs](modules/F6_interview-bank/F6_admin_catalog.md) | 24–29 | 24 | API9–18, BR01/02, validation6: admin counts, slug collision/name uniqueness, STAR metadata, reorder full-set, retired FK references, last-level concurrency, audit rollback. Sửa fixture slug và stale audit status; thêm level create, malformed reorder, FK race, rename variants. |
| [F6 questions](modules/F6_interview-bank/F6_admin_questions.md) | 30–35 | 19 | API19–23, BR03/10, validation5/6: shared author scope, aggregates/privacy/half-up, CRUD/rubric/body bounds, CSV fields/snapshots, topic move, partial retire/limit/grants. Sửa rubric-less drill oracle; thêm global usage/average sort qua hai trang, đồng bộ DD ORDER BY trước LIMIT. |
| [F6 CSV/audit](modules/F6_interview-bank/F6_admin_csv_audit.md) | 36–38 | 14 | API24/26, BR11/13, validation8: physical CSV lines, quoting/newlines, per-row skip/duplicate/reimport/advisory lock, 500-row and5 MiB boundaries, audit atomics, Function+ADMIN guards. Thêm valid500 subcase và exact403 INB-302. |
| [F6 resilience/ports](modules/F6_interview-bank/F6_admin_budget_resilience_progress.md) | 39–41 | 13 | API6/8/25–27, BR07/08/09/12: budget before AI, postcommit token/WARN, ADMIN exemption, FAILED answer retained, Redis fail-open vs PG budget, core coding unaffected, all-topic progress/no event. Đối chiếu F5 P1/P2 để không lẫn budget refusal với infrastructure failure. |

## Findings đã giải quyết

[Batch đầu](AUDIT_F5_PORTS_JOBS_F6_CATALOG.md) lưu findings ban đầu của64 ca. Review tiếp các cụm còn lại đã sửa thêm:

- F5 forecast: 3.000.000 / (2.000.000/7)=10,5; ceil11 ngày, từ27/10 tới07/11, không phải2 ngày tới29/10.
- F5 turn provider timeout không thể xảy ra trước turn.accepted vì BR07 commit USER trước khi gọi provider. Oracle pre-stream dùng request thiếu expectedTurnCount; lỗi provider giữ USER và retry tái dùng.
- F5 audit failure: owner chốt **503 AIR-503**, rollback đầy đủ API17/19/20. Registry/DD/TDD/AC40-05/AC43-05 đồng bộ. Bảng outbound snapshot/context sửa về **AIR-501 UPSTREAM_PORT** theo BR02/API4 để không lẫn mã audit.
- F5 baseline anomaly: quyết định mới **ADMIN được tính trong trung bình**, chỉ loại khỏi ứng viên alert. Ca có số liệu baseline4,45/7,18 và ratio2,25/5,57 kiểm được; registry ghi override một phần quyết định23/10.
- F6 privacy history trước kia đòi12 rows trên một trang10; sửa10+2 và totalAttempts toàn lịch sử. Rubric-less question vẫn vào recall/drill để học, chỉ practice submit bị chặn INB-040.
- F6 sort usage/average phải gom/sắp toàn tập lọc trước phân trang; gom chỉ trang hiện tại chỉ hợp lệ cho sort không dùng hai aggregate đó. Ca AC30-03 dùng9 câu, pageSize8, usage/score ngược displayNo để bắt lỗi.

## Cổng thiết kế còn mở duy nhất

**QA-F5-F6-ARRAY-OVERFLOW-10** — F5 validation§3 và F6 validation§7 giới hạn mỗi mảng định tính10 phần tử nhưng chưa chốt xử lý11: giữ10 phần tử đầu hay từ chối schema. Owner đã được hỏi; chưa có câu trả lời. [TC-F5-AC01-04](modules/F5_ai-review/F5_solution_review.md) và [TC-F6-AC10-02](modules/F6_interview-bank/F6_learner_practice.md) đã chuẩn bị dữ liệu10/11; phần10 có oracle, phần11 ghi rõ **chưa chốt** và không được đánh pass.

Đóng thiết kế đợt3 sau khi owner trả lời, cập nhật hai validation/decision/ca và chạy lại checker; không cần audit lại toàn bộ14 file nếu thay đổi chỉ ở policy này. Các nhánh còn lại đã review; các ca vẫn chưa thực thi.

## Cổng thực thi sau coding

Cần build/version, PostgreSQL thật, Redis/broker, LLM/port stubs đúng hợp đồng, clock/fault/transaction barriers, JWT/grants và reset fixture. Chạy unit/integration/API theo slice rồi smoke/E2E với evidence. Chất lượng nội dung của provider thật và hiệu năng là đánh giá riêng; review tài liệu không chứng minh các kết quả đó.