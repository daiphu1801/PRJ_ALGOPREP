# Sweep, recovery và retention F4

## Bật local

Chạy backend với các profile `local,worker,outbox,maintenance` và URL sandbox trong
[runbook worker](../judge-engine/WORKER-RUNBOOK.md). Với cấu hình khởi chạy sẵn có,
thêm `--judge.maintenance.enabled=true`. Mặc định không bật các job này.

Profile worker cấp pool 16 kết nối. Maintenance có scheduler riêng ba thread, không
chiếm scheduler relay hoặc worker pause/autoscale. Đây vẫn là mốc local một backend
HTTP/WebSocket/worker; chưa nghiệm thu production nhiều process.

| Job | Mặc định | Hành vi |
| --- | --- | --- |
| JOB-JD-01 | Mỗi 60 giây; tối đa 200 ứng viên | Bài không cập nhật quá 300 giây chuyển SYSTEM_ERROR/WORKER_TIMEOUT |
| JOB-JD-03 | 03:00 UTC+7 | Xóa Outbox đã published quá 90 ngày, mỗi transaction tối đa 5.000 dòng |
| JOB-JD-04 | 03:30 UTC+7 | Xóa chi tiết testcase quá 90 ngày, giữ bài nộp, điểm tổng, source và token |

Hằng số ở `application-maintenance.yml`; các thuộc tính nằm dưới `judge.maintenance`:
`timeout-seconds`, `sweep-delay-ms`, `sweep-batch-size`, `retention-days`,
`cleanup-batch-size`, `outbox-cron`, `details-cron`. Hai lịch cron dùng múi giờ
`Asia/Ho_Chi_Minh`. Lỗi một lượt được ghi log và chạy lại theo lịch; các lô đã commit
không bị rollback bởi lỗi ở lô sau.

## Sweep và recovery

Worker giữ session lease `judge:sub:<submissionId>` qua các transaction ghi ngắn.
Sweep lấy transaction lock cùng khóa này; nếu worker còn giữ lease thì bỏ qua bài.
Khi lease được nhả, bài vẫn quá ngưỡng sẽ được xét lại ở lượt sau. Bài có kết quả từng
phần được giữ nguyên chi tiết, kết thúc SYSTEM_ERROR và score_ratio=NULL; không chấm lại.

Mỗi ứng viên được kiểm lại dưới khóa dòng: trạng thái còn PENDING/COMPILING/JUDGING,
heartbeat cũ hơn ngưỡng, PENDING không đang pause. Sweep dùng chung finalizer với worker:
thay trạng thái và ghi SubmissionGraded là một transaction; progress chỉ phát sau commit.
Lỗi ghi Outbox rollback cả kết quả cuối. Không phát SubmissionAccepted cho timeout.

## Pause/resume

`ChangeQueuePauseUseCase.change(expectedPaused, paused)` là kernel nội bộ có transaction
REQUIRED/READ_COMMITTED, trả CHANGED/UNCHANGED/CONFLICT. Kernel khóa cluster_settings,
kiểm expectedPaused và chỉ khi true→false mới làm mới updated_at của các bài PENDING
trong cùng transaction đổi cờ. Heartbeat lấy giờ PostgreSQL sau khi lấy khóa.

Kernel này chưa phải API quản trị. Khi triển khai endpoint #19, bên gọi cần kiểm ADMIN,
ghi audit/actor và ánh xạ CONFLICT sang JDG-201 trong cùng giao dịch ngoài. Lỗi audit phải
rollback cả cờ và heartbeat; test rollback kernel đã có. Không dùng lệnh UPDATE cờ đơn lẻ
làm đường resume của sản phẩm vì thiếu phần heartbeat nguyên tử.

Worker controller tiếp tục poll cờ và start/stop/drain theo hợp đồng hiện có.

## Đối chiếu

```sql
SELECT id, status, failure_reason, updated_at, finished_at, score_ratio
FROM judge.submissions
WHERE status IN ('PENDING','COMPILING','JUDGING') OR failure_reason='WORKER_TIMEOUT'
ORDER BY updated_at DESC LIMIT 20;

SELECT count(*) AS pending_outbox
FROM judge.outbox_events WHERE published_at IS NULL;

SELECT count(*) AS old_testcase_details
FROM judge.submission_testcase_results WHERE created_at < now()-interval '90 days';
```

Log ba job chỉ chứa mã job và số dòng; không chứa source/testcase/stderr. Outbox pending
không bị cleanup, bất kể tuổi. `judge_run_tokens` và `callback_logs` được giữ nguyên.
Chi tiết testcase hết retention cần được API/màn kết quả xử lý rõ ở task 7; không suy
bảng con trống thành lượt nộp không có testcase.

## Ranh giới nghiệm thu

Lát này kiểm các giao dịch, cạnh tranh bằng PostgreSQL thật và hồi quy worker/relay.
API quản trị pause/audit, API kết quả/progress/lịch sử, FE, thử chờ 5 phút thực tế và
production rollout vẫn cần nghiệm thu ở lát tương ứng. Network-enabled tiếp tục hoãn.
