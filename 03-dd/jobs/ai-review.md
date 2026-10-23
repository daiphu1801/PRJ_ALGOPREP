# DD — Job nền module `ai-review` (F5)

**Tạo 2026-10-23** (`DEC-2026-1023-db-actor-review-fixes`, `DEC-2026-1023-db-actor-review-decisions`). Owner chốt 2026-10-23: ngưỡng bất thường gấp 5 lần trung bình,
TTL `ChatMemory` 2 giờ, ngưỡng "đủ lượt" 4. BD nhiều lần giao chi tiết job cho `03-dd/jobs` (`02-bd/database/ai-review.md:122`, `:222`, `:273`).

Bảng 1 liệt kê năm job. Tất cả là component `@Scheduled` trong module `ai-review`; không job nào được làm F1-F4 phụ thuộc vào AI
(`CLAUDE.md`, mục Rules: "The AI subsystem degrades gracefully").

**Bảng 1: Danh sách job của `ai-review`**

| Mã | Job | Lịch | Bảng chạm | Nguồn BD |
| :-: | :--- | :--- | :--- | :--- |
| JOB-AI-01 | Relay outbox | vài giây một lần | `outbox_events` | `02-bd/database/ai-review.md:255-271` |
| JOB-AI-02 | Dọn outbox | hằng ngày | `outbox_events` | `02-bd/database/ai-review.md:273` |
| JOB-AI-03 | Chuyển chu kỳ ngân sách | đầu mỗi tháng | `ai_token_budget_configs` | `02-bd/database/ai-review.md:222`, `02-bd/architecture/ai-review.md:273` |
| JOB-AI-04 | Quét phiên phỏng vấn bỏ trống | mỗi phút | `interview_sessions`, `interview_turns`, `rubric_scores`, `outbox_events`, `token_usage` | `02-bd/database/ai-review.md:116`, `:122` |
| JOB-AI-05 | Cảnh báo bất thường theo tài khoản | mỗi giờ | `token_usage`, `ai_usage_anomaly_alerts` | `02-bd/architecture/ai-review.md:277-285`, `02-bd/database/ai-review.md:237-250` |

## 0. Quy ước chung

- **Chạy nhiều bản (multi-instance).** Owner chốt 2026-10-23 (`DEC-2026-1023-open-questions-closed`): cùng cơ chế với `03-dd/jobs/judge-orchestration.md` mục 0. Relay (JOB-AI-01) dùng `FOR UPDATE SKIP LOCKED`; các job còn lại dùng khoá tư vấn Postgres `pg_try_advisory_xact_lock(hashtext(<mã job>))` để chỉ một instance chạy mỗi chu kỳ (không lấy được khoá thì bỏ lượt).
- **Thời gian** lấy từ `now()` của PostgreSQL. Múi giờ "tháng" và "ngày" của JOB-AI-03 là UTC+7, cùng quy ước hệ thống
  (`02-bd/database/judge-orchestration.md:74-75`) `[SoT: Suy luận]` cho phạm vi `ai-review`, vì BD `ai-review` không nêu riêng.
- **Hằng số** ở `application.yml`; không thêm ô cấu hình admin (ponytail bậc 5).
- **Lỗi một lần chạy** chỉ log và chờ chu kỳ sau; không job nào được ném lỗi ra ngoài luồng request.

## 1. JOB-AI-01 — Relay outbox

- **Mục đích.** Publish `SolutionReviewCompleted` và `MockInterviewFinished` (`02-bd/database/ai-review.md:263-266`) cho `identity` sau khi giao dịch ghi review hoặc
  chuyển `COMPLETED` đã commit. Cùng mẫu `judge.outbox_events` (`02-bd/database/ai-review.md:257-258`).
- **Lịch.** `fixedDelay = 2 s` `[SoT: Suy luận]` (BD không nêu chu kỳ).
- **Truy vấn.**

```sql
SELECT id, aggregate_type, aggregate_id, event_type, payload, publish_attempts
FROM ai.outbox_events
WHERE published_at IS NULL
ORDER BY created_at ASC
LIMIT :batchSize
FOR UPDATE SKIP LOCKED;
```

  Index một phần `outbox_events(published_at) WHERE published_at IS NULL` (`02-bd/database/ai-review.md:271`).
- **Xử lý.** Publish; chỉ khi broker ack mới set `published_at = now()`. Mỗi lần thử tăng `publish_attempts`. Thất bại: giữ `published_at IS NULL`, lần sau thử lại.
- **Cảnh báo.** `publish_attempts` từ 3 log `WARN`, từ 5 log `ERROR`, cùng hằng số với module `judge` (`02-bd/database/judge-orchestration.md:241`) `[SoT: Suy luận]`
  cho `ai-review` vì BD `ai-review` không nêu ngưỡng riêng. Không có trạng thái bỏ cuộc.
- **Cỡ lô.** 100 `[SoT: Suy luận]`.
- **Idempotency.** At-least-once; consumer của `identity` phải xử lý trùng theo `aggregate_id` `[SoT: Suy luận]`.
- **Lỗi.** Broker lỗi: cả lô giữ nguyên, không ảnh hưởng việc gọi AI hay chấm.
- **Bảng chạm.** `outbox_events`.

## 2. JOB-AI-02 — Dọn outbox

- **Mục đích.** Xoá dòng đã publish quá 90 ngày; số 90 là hằng số `application.yml`, không thêm cột (`02-bd/database/ai-review.md:273`).
- **Lịch.** Hằng ngày 03:00 UTC+7 `[SoT: Suy luận]`.
- **Truy vấn.**

```sql
DELETE FROM ai.outbox_events
WHERE id IN (
  SELECT id FROM ai.outbox_events
  WHERE published_at IS NOT NULL
    AND published_at < now() - interval '90 days'
  LIMIT :batchSize
);
```

  **Không bao giờ xoá** dòng `published_at IS NULL`.
- **Cỡ lô.** 5.000 `[SoT: Suy luận]`, lặp đến khi lô cuối nhỏ hơn cỡ lô.
- **Idempotency và đồng thời.** An toàn khi chạy lại; khoá một instance (mục 0).
- **Bảng chạm.** `outbox_events` (xoá).

## 3. JOB-AI-03 — Chuyển chu kỳ ngân sách token

- **Mục đích.** Mở chu kỳ ngân sách mới: `period_start` về ngày 1 của tháng và xoá `locked_at` (mở khoá tự động). `usedTokens` luôn tính từ `token_usage` kể từ `period_start`,
  nên chỉ cần `period_start` đúng là số đã dùng tự về 0 (`02-bd/database/ai-review.md:222`; `DEC-2026-1022-ai-judge-schema-sync` mục 4).
- **Lịch.** `cron = "0 5 0 1 * *"` múi giờ UTC+7 (00:05 ngày 1 hằng tháng) `[SoT: Suy luận]` — BD chỉ nói "đầu mỗi tháng". Lệch 5 phút để tránh lượt gọi ngay nửa đêm.
- **Truy vấn.**

```sql
UPDATE ai.ai_token_budget_configs
SET period_start = date_trunc('month', now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date,
    locked_at    = NULL,
    updated_at   = now()
WHERE id = 1
  AND period_start < date_trunc('month', now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date;
```

  - Bảng là singleton `id = 1` (`02-bd/database/ai-review.md:210`).
  - Điều kiện `period_start < <ngày 1 tháng hiện tại>` làm job **idempotent**: chạy lại hoặc hai instance chạy cùng lúc thì lần sau đổi 0 dòng.
  - `updated_by` để `NULL` (hệ thống thực hiện, không phải ADMIN) `[SoT: Suy luận]`.
  - **Bù khi tắt máy.** Nếu hệ thống tắt đúng đầu tháng, job không chạy. Đề xuất `[SoT: Suy luận]`: ngoài cron, chạy thêm một lần lúc khởi động ứng dụng (cùng câu `UPDATE` có điều kiện, an toàn nhờ idempotent).
- **Ghi nhật ký.** Nếu `locked_at` đang khác `NULL` trước khi xoá thì ghi một dòng nhật ký hệ thống "tự mở khoá đầu tháng" qua `AuditLogPort` của `identity`
  (cổng inbound cùng JVM, cùng giao dịch, owner chốt 2026-10-23) `[SoT: Suy luận]` — BD không nêu nhưng khoá/mở khoá là hành động đáng ghi vết.
- **Lỗi.** DB lỗi: log `ERROR`; lần chạy khởi động hoặc cron sau sửa lại nhờ điều kiện `period_start <`.
- **Bảng chạm.** `ai_token_budget_configs` (ghi); `system_audit_logs` của `identity` qua cổng (ghi, nếu có dòng được mở khoá).

## 4. JOB-AI-04 — Quét phiên phỏng vấn bỏ trống

- **Mục đích.** Đóng các phiên Mock Interview bị bỏ trống 15 phút (`02-bd/database/ai-review.md:122`; `DEC-2026-1018-usr-open-questions-phase2-5`,
  `DEC-2026-1022-ai-judge-schema-sync` mục AI-04).
- **Lịch.** `fixedDelay = 60 s` `[SoT: Suy luận]` — BD chỉ nêu ngưỡng 15 phút, không nêu chu kỳ quét.
- **Truy vấn chọn ứng viên.**

```sql
SELECT id, turn_count FROM ai.interview_sessions
WHERE stage IN ('EXPLAIN','CHALLENGE','SCALE_UP')
  AND updated_at < now() - interval '15 minutes'
ORDER BY updated_at ASC
LIMIT :batchSize
FOR UPDATE SKIP LOCKED;
```

  Index `interview_sessions(stage, updated_at)` (`02-bd/database/ai-review.md:122`, `:283`). 15 phút là hằng số đã chốt.
- **Rẽ nhánh theo số lượt.** Hằng số `enoughTurns` ở `application.yml`, mặc định **4** (owner xác nhận 2026-10-23; `02-bd/database/ai-review.md:116`).
  - **`turn_count >= enoughTurns` (đủ lượt):** tự chấm điểm bằng đúng đường chấm của "Kết thúc sớm" (rubric 4 tiêu chí từ các lượt đã có), ghi 4 dòng `rubric_scores`,
    ghi `token_usage`, đặt `stage = COMPLETED`, `ended_at = now()`, ghi `outbox_events` loại `MockInterviewFinished` trong **cùng giao dịch** với việc đổi `stage`.
    Việc gọi LLM chạy **ngoài** giao dịch: gọi xong mới mở giao dịch ghi kết quả.
  - **`turn_count < enoughTurns` (chưa đủ):** `UPDATE ... SET stage = 'ABANDONED', ended_at = now()`. **Không chấm**, không có `rubric_scores`, **không phát sự kiện**
    (`02-bd/database/ai-review.md:116`). Giữ nguyên `interview_turns` (`02-bd/database/ai-review.md:146-148`).
  - Việc chọn nhánh đọc `turn_count` của phiên (`02-bd/database/ai-review.md:117`). `[SoT: Suy luận]`: "lượt" ở ngưỡng được đếm theo `turn_count`, không đếm riêng
    lượt `USER` (chủ dự án chốt 2026-10-23, Q1).
- **Điều kiện ghi có bảo vệ.** Mọi `UPDATE stage` kèm `AND stage IN ('EXPLAIN','CHALLENGE','SCALE_UP')`, để nếu người học vừa gửi lượt mới (đẩy `updated_at`) hoặc vừa Kết thúc sớm thì job
  đổi 0 dòng và bỏ qua `[SoT: Suy luận]`.
- **Khi chấm đủ lượt gặp ngân sách khoá hoặc LLM lỗi.** `[SoT: Suy luận]`: không đổi `stage`, ghi log `WARN`, thử lại chu kỳ sau; sau `abandonedScoreMaxRetries` (mặc định 3) lần thì
  chuyển `ABANDONED` (không chấm) để phiên không kẹt mãi. Lý do: chấm tự động không được chặn bởi hạn mức mà người học không còn thấy để xử lý; chủ dự án chốt 2026-10-23 (Q2).
  Bộ đếm thử lại giữ trong Redis theo `sessionId` `[SoT: Suy luận]`, không thêm cột.
- **Cỡ lô.** 20 phiên mỗi lần chạy `[SoT: Suy luận]` (mỗi phiên đủ lượt tốn một lời gọi LLM nên giữ nhỏ).
- **Idempotency và đồng thời.** `FOR UPDATE SKIP LOCKED` chia lô giữa các instance; điều kiện `stage` ở `UPDATE` chặn xử lý hai lần. Phiên đã `COMPLETED`/`ABANDONED` không còn nằm trong truy vấn.
- **TTL ChatMemory.** Phiên `ABANDONED` hay `COMPLETED` không cần job dọn Redis: khoá hết hạn theo TTL 2 giờ (owner xác nhận 2026-10-23; `02-bd/database/ai-review.md:122`); lịch sử lâu dài ở `interview_turns`.
- **Bảng chạm.** `interview_sessions` (ghi `stage`, `ended_at`), `interview_turns` (đọc), `rubric_scores` (ghi, nhánh đủ lượt), `token_usage` (ghi, nhánh đủ lượt),
  `outbox_events` (ghi, nhánh đủ lượt).

## 5. JOB-AI-05 — Cảnh báo bất thường theo tài khoản

- **Mục đích.** Phát hiện tài khoản gọi AI nhiều bất thường và ghi cảnh báo mềm cho ADMIN xem ở `admin_ai_usage`. **Không khoá tài khoản**, không đụng `locked_at`
  (`02-bd/architecture/ai-review.md:279-289`, `DEC-2026-0831-ai-usage-anomaly-alert`).
- **Lịch.** Mỗi giờ, phút 0 (`cron = "0 0 * * * *"`); đề xuất BD, owner xác nhận 2026-10-23 (`02-bd/architecture/ai-review.md:280`).
- **Cửa sổ.** 24 giờ gần nhất: `window_end = now()`, `window_start = now() - interval '24 hours'` (`02-bd/database/ai-review.md:243`).
- **Số lượt gọi AI.** `call_count` = số dòng `token_usage` của user trong cửa sổ (`02-bd/database/ai-review.md:173-185`; mỗi dòng là một lượt gọi LLM, cache hit không có dòng) `[SoT: Suy luận]`
  rằng "lượt gọi" đồng nghĩa dòng `token_usage`. Loại `ADMIN` khỏi xét (chủ dự án chốt 2026-10-23, Q3) vì ADMIN không bị khoá ngân sách (`02-bd/architecture/ai-review.md:270-271`).
- **Trung bình hệ thống (`baseline_avg`).** Chủ dự án chốt 2026-10-23 (Q3) công thức dưới đây; BD ghi "trung bình toàn hệ thống cùng khung giờ" nhưng không định nghĩa công thức. Đề xuất: tổng dòng `token_usage` trong cửa sổ
  chia cho số `user_id` khác nhau có ít nhất một dòng trong cùng cửa sổ (không tính user không dùng AI để trung bình không bị kéo về gần 0). Lưu giá trị đó vào `baseline_avg` (`DECIMAL(8,2)`).
  Khi có ít hơn một người dùng hoạt động, hoặc `baseline_avg = 0`: bỏ qua lượt chạy (tránh chia cho 0).
- **Ngưỡng.** `ratio = call_count / baseline_avg >= 5` (hằng số `application.yml`, **gấp 5 lần**, owner xác nhận 2026-10-23; `02-bd/architecture/ai-review.md:281`). Ghi `ratio` vào `DECIMAL(5,2)`.
- **Truy vấn ứng viên (khung).**

```sql
WITH per_user AS (
  SELECT user_id, count(*) AS call_count
  FROM ai.token_usage
  WHERE created_at >= now() - interval '24 hours'
  GROUP BY user_id
), base AS (
  SELECT avg(call_count)::numeric(8,2) AS baseline_avg FROM per_user
)
SELECT p.user_id, p.call_count, b.baseline_avg, p.call_count / b.baseline_avg AS ratio
FROM per_user p CROSS JOIN base b
WHERE b.baseline_avg > 0
  AND p.call_count / b.baseline_avg >= :threshold
  AND NOT EXISTS (
    SELECT 1 FROM ai.ai_usage_anomaly_alerts a
    WHERE a.user_id = p.user_id AND a.status = 'OPEN');
```

  Loại `ADMIN` ở tầng ứng dụng sau khi tra vai trò qua cổng của `identity` `[SoT: Suy luận]` (không join xuyên schema).
- **Quy tắc một dòng `OPEN` mỗi user.** Khi user đã có một dòng `status = OPEN` thì **không ghi thêm**; sau khi ADMIN đặt `REVIEWED` thì lần chạy sau được ghi dòng `OPEN` mới cho user đó
  (`02-bd/database/ai-review.md:250`, `02-bd/architecture/ai-review.md:285`). Điều kiện `NOT EXISTS` ở trên là phép kiểm ở tầng ứng dụng.
- **Ghi.** `INSERT INTO ai_usage_anomaly_alerts (user_id, window_start, window_end, call_count, baseline_avg, ratio, status, created_at) VALUES (..., 'OPEN', now())`.
- **Cỡ lô.** Không cần chia lô: số tài khoản vượt ngưỡng nhỏ. Truy vấn gom nhóm chạy trên 24 giờ dữ liệu; dùng các chỉ mục đã có của `token_usage`: `(created_at, feature_code)` và `(user_id, created_at)` (`02-bd/database/ai-review.md` mục 2), không cần thêm chỉ mục.
- **Idempotency và đồng thời.** DB không có unique cứng cho "một dòng `OPEN` mỗi user", nên hai lần chạy đồng thời hiếm hoi có thể ghi hai dòng; BD chấp nhận vì đây là cảnh báo mềm
  (`02-bd/database/ai-review.md:250`). Giảm xác suất bằng khoá một instance (mục 0).
- **Lỗi.** Lỗi truy vấn: log `ERROR`, bỏ lượt, giờ sau chạy lại. Job chỉ đọc `token_usage` nên không ảnh hưởng gọi AI.
- **Bảng chạm.** `token_usage` (đọc), `ai_usage_anomaly_alerts` (đọc, ghi).

## Câu hỏi mở

| # | Câu hỏi | Đề xuất |
| :-: | :--- | :--- |
| ~~Q1~~ | ~~Ngưỡng "đủ lượt" đếm theo `turn_count` hay chỉ lượt `USER`?~~ | **Đã chốt 2026-10-23:** theo `turn_count`, đúng cột đã có (ponytail bậc 4) |
| ~~Q2~~ | ~~Chấm tự động phiên đủ lượt thất bại (LLM lỗi, ngân sách khoá) thì làm gì?~~ | **Đã chốt 2026-10-23:** thử lại tối đa 3 chu kỳ rồi `ABANDONED`, bộ đếm ở Redis (ponytail bậc 5) |
| ~~Q3~~ | ~~Định nghĩa `baseline_avg`: trung bình trên user có hoạt động hay trên toàn bộ user?~~ | **Đã chốt 2026-10-23:** trung bình trên user có dùng AI trong cửa sổ 24 giờ; loại `ADMIN` khỏi việc xét |
| ~~Q4~~ | ~~Chặn chạy trùng giữa các instance: ShedLock hay advisory lock Postgres?~~ | **Đã chốt 2026-10-23:** advisory lock Postgres, không thêm bảng |
