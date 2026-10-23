# DD — Job nền module `judge-orchestration` (F4)

**Tạo 2026-10-23** (`DEC-2026-1023-db-actor-review-fixes`; owner chốt cùng ngày: mọi giá trị mặc định BD đề xuất đều được xác nhận).
BD đã nhiều lần ghi "chi tiết job nằm ở `03-dd/jobs`" (`02-bd/database/judge-orchestration.md:72`, `:137`); file này là nơi đó.

Bảng 1 liệt kê bốn job của module và nguồn của từng job. Mọi job đều là component `@Scheduled` nằm ở tầng `infrastructure`
(`02-bd/architecture/judge-orchestration.md:14-15`), chạy ngoài luồng request và ngoài giao dịch chấm bài.

**Bảng 1: Danh sách job của `judge-orchestration`**

| Mã | Job | Lịch | Bảng chạm | Nguồn BD |
| :-: | :--- | :--- | :--- | :--- |
| JOB-JD-01 | Sweep bài nộp treo | mỗi 60 giây | `submissions` | `02-bd/architecture/judge-orchestration.md:209-228` |
| JOB-JD-02 | Relay outbox | mỗi vài giây (xem mục 2) | `outbox_events` | `02-bd/architecture/judge-orchestration.md:239-248`, `02-bd/database/judge-orchestration.md:121-135` |
| JOB-JD-03 | Dọn outbox | hằng ngày | `outbox_events` | `02-bd/database/judge-orchestration.md:137` |
| JOB-JD-04 | Dọn `submission_testcase_results` quá 90 ngày | hằng ngày | `submission_testcase_results` | `02-bd/database/judge-orchestration.md:68-72` |

## 0. Quy ước chung cho mọi job

- **Chạy nhiều bản (multi-instance).** Owner chốt 2026-10-23 (`DEC-2026-1023-open-questions-closed`): dùng **khoá tư vấn Postgres** cho mọi job chỉ cần một instance chạy mỗi chu kỳ (JOB-JD-01, 03, 04): đầu giao dịch gọi `pg_try_advisory_xact_lock(...)`; không lấy được khoá thì bỏ lượt, khoá tự nhả khi giao dịch kết thúc. Khoá khoá theo hash của mã job (ví dụ `hashtext('JOB-JD-01')`). Không thêm bảng `shedlock`, không thêm thư viện.
  JOB-JD-02 (relay) không dùng khoá toàn cục mà dùng `FOR UPDATE SKIP LOCKED` để nhiều instance chia nhau lô. Mỗi job còn tự an toàn khi chạy song song nhờ cấu trúc truy vấn (nêu riêng từng job).
- **Múi giờ.** So sánh thời gian dùng `now()` của PostgreSQL (`TIMESTAMPTZ`), không dùng giờ máy ứng dụng. Múi giờ cắt ngày UTC+7 chỉ liên
  quan khi cắt theo ngày, không ảnh hưởng các job ở đây (`02-bd/database/judge-orchestration.md:74-75`).
- **Hằng số** (chu kỳ, ngưỡng, cỡ lô) đặt ở `application.yml`, không thêm ô cấu hình admin (tiêu chí ponytail bậc 5,
  `.claude/rules/ponytail-criteria.md`; `DEC-2026-1001-admin-configurable-settings` chỉ giữ các ô đã có).
- **Lỗi một lô không làm chết job**: bắt ngoại lệ ở vòng ngoài, ghi log `ERROR`, lần chạy sau thử lại.
- **Job không phụ thuộc AI path** (`CLAUDE.md`, mục Rules): không job nào ở đây gọi `ai-review`.

## 1. JOB-JD-01 — Sweep bài nộp treo (F4-07)

- **Mục đích.** Đưa bài nộp mà worker bỏ rơi (crash, bị kill) về trạng thái cuối `SYSTEM_ERROR` để học viên không chờ vô hạn
  (`02-bd/architecture/judge-orchestration.md:226-228`).
- **Lịch.** `fixedDelay = 60 s` (`02-bd/architecture/judge-orchestration.md:213`; chu kỳ là đề xuất BD và đã được owner xác nhận 2026-10-23,
  `02-bd/database/judge-orchestration.md:228`). Ngưỡng treo **5 phút** (RD `01-rd/req/judge-orchestration.md:41-43`, `02-bd/database/judge-orchestration.md:227`).
- **Truy vấn chọn ứng viên.**

```sql
SELECT id FROM judge.submissions
WHERE status IN ('COMPILING','JUDGING')            -- thêm 'PENDING' khi queue_paused = false
  AND updated_at < now() - interval '5 minutes'
ORDER BY updated_at ASC
LIMIT :batchSize;
```

  - `PENDING` chỉ nằm trong `IN (...)` khi `cluster_settings.queue_paused = false`; khi `true` thì bỏ `PENDING`
    (`02-bd/architecture/judge-orchestration.md:217-221`).
  - Index dùng: `submissions(status, updated_at)` (`02-bd/database/judge-orchestration.md:202`).
- **Hành động cho từng id.**

```sql
UPDATE judge.submissions
SET status = 'SYSTEM_ERROR', failure_reason = 'WORKER_TIMEOUT',
    finished_at = now(), updated_at = now()
WHERE id = :id
  AND status NOT IN (<6 trạng thái cuối>);
```

  - Mệnh đề `status NOT IN (<6 trạng thái cuối>)` là **bắt buộc**: chống ghi đè khi worker chậm hoàn tất đúng lúc sweep chạy
    (`02-bd/architecture/judge-orchestration.md:222-225`). Số dòng bị ảnh hưởng bằng 0 nghĩa là worker đã thắng cuộc đua, bỏ qua, không lỗi.
  - `score_ratio` để `NULL` cho `SYSTEM_ERROR` (`02-bd/database/judge-orchestration.md:31`).
  - Cùng giao dịch với `UPDATE`: ghi `outbox_events` loại `SubmissionGraded` (mọi trạng thái cuối đều phát, `02-bd/architecture/judge-orchestration.md:255`)
    và đẩy trạng thái mới qua WebSocket sau commit `[SoT: Suy luận]` (BD nói trạng thái đẩy qua WebSocket nhưng không nói riêng cho sweep).
    `SubmissionAccepted` không phát vì trạng thái không phải `ACCEPTED`.
- **Chạm `updated_at` khi mở lại hàng đợi.** `queue_paused` chuyển `false` thì ghi `updated_at = now()` cho các dòng `PENDING`, để đồng hồ 5 phút
  tính lại từ lúc mở (`02-bd/architecture/judge-orchestration.md:220-221`, BD giao DD chốt cách chạm). Việc này thuộc luồng "tiếp tục tiêu thụ", không phải thân job sweep.
- **Cỡ lô.** 200 dòng mỗi lần chạy `[SoT: Suy luận]`; còn dư thì lần chạy sau (60 s) xử lý tiếp. Mỗi dòng một giao dịch ngắn (UPDATE + INSERT outbox).
- **Idempotency và đồng thời.** `UPDATE` có điều kiện trạng thái nên chạy lại hay chạy hai instance cùng lúc đều an toàn (instance thứ hai thấy 0 dòng bị đổi).
  Bọc khoá tư vấn Postgres (mục 0) để tránh đẩy WebSocket trùng.
- **Lỗi.** DB lỗi: log `ERROR`, bỏ lượt này. Lỗi đẩy WebSocket sau commit: chỉ log `WARN`, trạng thái đã nằm trong DB nên client đọc lại được khi tải trang.
- **Bảng chạm.** `submissions` (ghi), `outbox_events` (ghi), `cluster_settings` (đọc `queue_paused`).

## 2. JOB-JD-02 — Relay outbox

- **Mục đích.** Publish các sự kiện domain (`SubmissionAccepted`, `SubmissionGraded`) lên RabbitMQ sau khi giao dịch chấm đã commit, để `ai-review` và `identity`
  nhận được dù broker từng chết (`02-bd/architecture/judge-orchestration.md:239-248`).
- **Lịch.** `fixedDelay = 2 s` `[SoT: Suy luận]` — BD chỉ nói relay là component `@Scheduled` polling, không nêu chu kỳ; cần đủ nhanh để F5 và read model của `identity` không trễ rõ rệt.
- **Truy vấn lấy lô.**

```sql
SELECT id, aggregate_type, aggregate_id, event_type, payload, publish_attempts
FROM judge.outbox_events
WHERE published_at IS NULL
ORDER BY created_at ASC
LIMIT :batchSize
FOR UPDATE SKIP LOCKED;
```

  - Index một phần `outbox_events(published_at) WHERE published_at IS NULL` (`02-bd/database/judge-orchestration.md:134-135`).
  - `FOR UPDATE SKIP LOCKED` `[SoT: Suy luận]`: hai instance cùng chạy sẽ không lấy trùng dòng.
- **Xử lý từng dòng.** Publish lên exchange sự kiện domain; chỉ khi broker **ack** mới `UPDATE ... SET published_at = now()`. Mỗi lần thử (thành công hay không)
  tăng `publish_attempts` thêm 1. Thất bại: dòng giữ `published_at IS NULL` để lượt sau thử lại, không bao giờ bỏ dòng (`02-bd/architecture/judge-orchestration.md:247-248`, `:264-266`).
- **Cỡ lô.** 100 dòng `[SoT: Suy luận]`. Giữ thứ tự `created_at ASC`. Thứ tự giữa các aggregate khác nhau không cần bảo toàn tuyệt đối khi chạy hai instance
  (consumer phải idempotent theo `submissionId`) `[SoT: Suy luận]`.
- **Ngưỡng cảnh báo.** `publish_attempts` từ **3** thì ghi log `WARN`, từ **5** thì log `ERROR`; hằng số `application.yml`, hiển thị ở `ADM0404` và thẻ liên kết của `ADM0401`;
  không thêm cột hay bảng (`02-bd/database/judge-orchestration.md:241`, `DEC-2026-1010-admin-operations-regroup`, ponytail bậc 5). Dòng vượt 5 lần **vẫn tiếp tục được thử** (không có trạng thái "bỏ cuộc").
- **Idempotency.** At-least-once: có thể publish trùng nếu ack mất sau khi broker đã nhận. Chấp nhận vì consumer xử lý theo khoá nghiệp vụ `[SoT: Suy luận]`.
- **Lỗi.** Broker không phản hồi: cả lô giữ nguyên, tăng `publish_attempts`, thử lại chu kỳ sau. Không ảnh hưởng luồng chấm (`02-bd/architecture/judge-orchestration.md:268-271`).
- **Bảng chạm.** `outbox_events` (đọc, ghi `published_at`, `publish_attempts`).

## 3. JOB-JD-03 — Dọn outbox

- **Mục đích.** Giữ bảng `outbox_events` không phình vô hạn (`02-bd/database/judge-orchestration.md:137`).
- **Lịch.** Hằng ngày, 03:00 giờ hệ thống UTC+7 `[SoT: Suy luận]` (giờ thấp tải).
- **Truy vấn.**

```sql
DELETE FROM judge.outbox_events
WHERE id IN (
  SELECT id FROM judge.outbox_events
  WHERE published_at IS NOT NULL
    AND published_at < now() - interval '90 days'
  LIMIT :batchSize
);
```

  - **Không bao giờ xoá** dòng `published_at IS NULL` — điều kiện `published_at IS NOT NULL` là bắt buộc (`02-bd/database/judge-orchestration.md:137`).
  - 90 ngày là hằng số `application.yml`, không thêm cột.
- **Cỡ lô.** 5.000 dòng mỗi lô, lặp đến khi một lô trả ít hơn cỡ lô, theo cùng mẫu BR-20 (`03-dd/logic/identity.md:44`). `[SoT: Suy luận]` cho con số 5.000 ở module này.
- **Idempotency và đồng thời.** `DELETE` theo điều kiện nên chạy lại an toàn. Khoá tư vấn Postgres để một instance chạy mỗi ngày (mục 0).
- **Lỗi.** Lỗi giữa chừng: các lô đã commit giữ nguyên, lần chạy sau xoá tiếp.
- **Bảng chạm.** `outbox_events` (xoá).

## 4. JOB-JD-04 — Dọn `submission_testcase_results` quá 90 ngày

- **Mục đích.** Giữ chi tiết từng testcase **90 ngày** tính từ `created_at`; sau đó chỉ còn verdict tổng và các số thống kê trên `submissions`
  (`02-bd/database/judge-orchestration.md:68-72`, `DEC-2026-1018-usr-open-questions-phase2-5`).
- **Lịch.** Hằng ngày, 03:30 UTC+7 `[SoT: Suy luận]`, sau JOB-JD-03 để không tranh I/O.
- **Truy vấn.**

```sql
DELETE FROM judge.submission_testcase_results
WHERE id IN (
  SELECT id FROM judge.submission_testcase_results
  WHERE created_at < now() - interval '90 days'
  LIMIT :batchSize
);
```

  - **Không bao giờ xoá** `submissions` và `source_code`; một lượt nộp không bị xoá hay ẩn (`02-bd/database/judge-orchestration.md:70-72`). Job chỉ đụng bảng con.
  - Khoá ngoại `judge_run_token_id` trỏ **vào** `judge_run_tokens`, nên xoá dòng kết quả không vướng FK. Bảng `judge_run_tokens` không thuộc quy tắc 90 ngày
    này (BD không nêu); giữ nguyên `[SoT: Suy luận]`, cần quyết định riêng nếu muốn dọn.
- **Hệ quả cho giao diện (không thuộc job, chỉ ghi nhận).** Màn kết quả nộp bài cũ hơn 90 ngày không còn chi tiết testcase; phải xử lý trạng thái "không còn dữ liệu chi tiết"
  thay vì hiện bảng rỗng `[SoT: Suy luận]`.
- **Cỡ lô.** 5.000 dòng mỗi lô `[SoT: Suy luận]`.
- **Idempotency và đồng thời.** `DELETE` theo điều kiện, an toàn khi chạy lại. Khoá tư vấn Postgres (mục 0).
- **Lỗi.** Như JOB-JD-03.
- **Bảng chạm.** `submission_testcase_results` (xoá). Không chạm `submissions`.

## Câu hỏi mở

| # | Câu hỏi | Đề xuất |
| :-: | :--- | :--- |
| ~~Q1~~ | ~~Chọn ShedLock hay advisory lock để chặn chạy trùng giữa các instance?~~ | **Đã chốt 2026-10-23:** advisory lock Postgres (không thêm bảng, không thêm thư viện); ponytail bậc 4 |
| Q2 | `judge_run_tokens` có cần dọn không? | Hoãn, BD chưa nêu; ponytail bậc 3 |
