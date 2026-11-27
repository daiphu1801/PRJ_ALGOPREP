# Relay sự kiện F4 → RabbitMQ → F1

JOB-JD-02 gửi các phong bì đã commit trong `judge.outbox_events` tới exchange topic
`algoprep.domain-events`. Hai routing key là `judge.submission.graded` và
`judge.submission.accepted`; F1 sở hữu các queue/binding tương ứng.

## Bật trên local

Chạy backend bằng các profile `local,worker,outbox`, cùng cấu hình kết nối hạ tầng và
`ALGOPREP_WORKER_ENGINE_URL` trong [runbook worker](../judge-engine/WORKER-RUNBOOK.md).
Nếu dùng cấu hình chạy hiện có, thêm `--judge.outbox.enabled=true`.
Không cần khởi động thêm process. Cờ pause queue chấm không dừng relay.

Relay có scheduler riêng, một lượt tối đa 100 dòng, sau lượt trước nghỉ 2 giây.
`judge.outbox.fixed-delay-ms` điều chỉnh khoảng nghỉ; mặc định 2000.
Khi không bật cờ/profile, relay không tự chạy. Có thể gọi use case trong kiểm thử độc lập.

## Hành vi khi lỗi

- Chỉ broker confirm thành công và không có mandatory return mới đặt `published_at`.
- Mỗi lần thử tăng `publish_attempts`; thất bại giữ pending và thử lại ở lượt sau.
- Từ 3 lần thất bại ghi WARN, từ 5 ghi ERROR; vẫn retry. Ngưỡng cấu hình bằng
  `judge.infra.outbox-warn-attempts` và `judge.infra.outbox-error-attempts`.
- Mất DB commit sau broker ack có thể gửi lại. `eventId` và phong bì giữ nguyên;
  F1 khử trùng trong cùng giao dịch cập nhật read model.
- `FOR UPDATE SKIP LOCKED` giữ quyền sở hữu lô đến cuối giao dịch và bỏ qua dòng của
  relay khác. Chỉ mục pending hiện có hỗ trợ thứ tự `(created_at, id)`.
- Mỗi lần publish dùng connection/channel riêng và có thời hạn chờ 10 giây;
  một lô lỗi có thể kéo dài. Scheduler riêng tránh chặn pause/autoscale.

## Kiểm tra sau khi bật

```sql
SELECT event_type, count(*) FILTER (WHERE published_at IS NULL) AS pending,
       max(publish_attempts) AS max_attempts
FROM judge.outbox_events GROUP BY event_type;

SELECT o.id, o.event_type, o.published_at, o.publish_attempts, p.processed_at
FROM judge.outbox_events o
LEFT JOIN identity.processed_domain_events p ON p.event_id = o.id
ORDER BY o.created_at DESC LIMIT 20;
```

`published_at` chứng minh broker đã nhận, không chứng minh mọi consumer đã xử lý.
Đối chiếu `processed_domain_events` và read model F1 để kiểm phần tiêu thụ.
Không xoá pending để dọn queue. Không tự thay `eventId` khi gửi lại.

## Phạm vi

Lát này nối relay và F1. Chưa nghiệm thu consumer F2/F5, cleanup 90 ngày, sweep/recovery,
API kết quả/lịch sử, thống kê lớp/admin hoặc FE. Không thay giới hạn network và không
nghiệm thu triển khai production nhiều process.
