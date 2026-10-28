# Test case QA — F4: tự động mở rộng consumer chấm bài

> Trạng thái: 10 ca đã review thiết kế; chưa thực thi. Kiểm mục tiêu và số consumer thực bằng metric/log F4, không suy từ số container go-judge.

## Phạm vi và fixture

- TDD: `04-tdd/modules/F4_judge-orchestration.md` AC-77; DD `03-dd/jobs/F4_judge-orchestration.md` JOB-JD-05 và `03-dd/logic/F4_judge-orchestration.md` BR-JD-08.
- Controller poll ở `T0 + 15k` giây (`k=1,2,...`); `T0=2026-10-26T03:00:00Z`. Bắt đầu `targetConsumers=activeConsumers=1`, `autoscale_enabled=true`, `autoscale_job_threshold=20`, `queue_paused=false`, `judge.autoscale.max-workers=8`. Bản kiểm thử có thể đặt chính xác `pendingCount` cho từng poll và giữ lượt đang xử lý ở consumer cần drain. Sau mỗi lần đổi mục tiêu, bộ đếm liên tiếp được đặt lại theo JOB-JD-05.
- Quan sát `judge_grading_consumers_target`, `judge_grading_consumers_active`, log `{oldTarget,newTarget,pendingCount,threshold,reason}`, trạng thái listener và số message được nhận. Dùng DB/queue riêng, khôi phục công tắc và tiến trình sau ca.

## Ma trận nhánh

| AC | Nhánh | Test case | Review | Thực thi |
| :--- | :--- | :--- | :--- | :--- |
| F4 AC-77 | Bằng/vượt ngưỡng và hai poll liên tiếp | TC-F4-AC77-01 | Đã review | Chưa thực hiện |
| F4 AC-77 | Tăng từng bước tới trần 8 | TC-F4-AC77-02 | Đã review | Chưa thực hiện |
| F4 AC-77 | Rỗng bốn poll rồi giảm khi drain | TC-F4-AC77-03 | Đã review | Chưa thực hiện |
| F4 AC-77 | Khoảng giữa ngưỡng giữ mục tiêu | TC-F4-AC77-04 | Đã review | Chưa thực hiện |
| F4 AC-77 | Pause/resume giữ mục tiêu | TC-F4-AC77-05 | Đã review | Chưa thực hiện |
| F4 AC-77 | Tắt autoscale trở về một consumer | TC-F4-AC77-06 | Đã review | Chưa thực hiện |
| F4 AC-77 | Hai worker tranh leader, mất khoá | TC-F4-AC77-07 | Đã review | Chưa thực hiện |
| F4 AC-77 | Đổi ngưỡng có hiệu lực poll kế | TC-F4-AC77-08 | Đã review | Chưa thực hiện |
| F4 AC-77 | Lỗi đọc số PENDING từ DB | TC-F4-AC77-09 | Đã review | Chưa thực hiện |
| F4 AC-77 | Listener từ chối đổi concurrency | TC-F4-AC77-10 | Đã review | Chưa thực hiện |

## Test case

### TC-F4-AC77-01 — Bằng ngưỡng không tăng; vượt ngưỡng phải đủ hai poll

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-77; JOB-JD-05 mục 5, quy tắc tăng |
| Kind / priority | Boundary, P0: không tăng sớm khi backlog vừa chạm ngưỡng |
| Environment | Controller F4 + DB `cluster_settings`; stub đếm PENDING/listener tại poll 15 giây |
| Preconditions | Fixture chung; listener đang chạy một consumer |
| Test data | `pendingCount` ở poll 1..4 lần lượt `20,21,20,21`, poll 5 là `21` |
| Steps | Cấp lần lượt năm snapshot, kích hoạt từng poll và ghi metric/log sau mỗi poll. |
| Expected | Poll 1–4 mục tiêu/thực vẫn 1 vì `20` không vượt ngưỡng và chuỗi `21` bị ngắt. Poll 5 là lần `21` liên tiếp thứ hai: mục tiêu tăng đúng 1→2, active hội tụ 2; đúng một log scale, không tạo container go-judge hay ghi DB mỗi poll. |
| Cleanup | Đưa controller về một consumer, dọn fixture |
| Evidence | Chuỗi snapshot, metric target/active từng poll, log scale và DB trước/sau |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC77-02 — Backlog liên tục tăng từng bước nhưng không vượt tám consumer

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-77; JOB-JD-05 mục 5, trần `max-workers=8` và reset bộ đếm |
| Kind / priority | Boundary, P0: không nhân worker vượt trần |
| Environment | Controller F4 + listener stub; `pendingCount` được giữ cố định qua poll |
| Preconditions | Fixture chung, 1 consumer, queue mở |
| Test data | `pendingCount=21` ở poll 1..18, mỗi poll cách 15 giây |
| Steps | Chạy 18 poll, ghi target/active và log ở từng mốc. |
| Expected | Sau poll 1 target 1; sau poll 2 là 2; sau các cặp poll tiếp theo target lần lượt 3..8, tối đa tăng một ở poll đổi mục tiêu; poll 15..18 vẫn 8. `activeConsumers<=8` mọi lúc, không có lần scale lên 9 hoặc DB write per poll. |
| Cleanup | Tắt backlog fixture, drain về một consumer |
| Evidence | Bảng target/active 18 poll, log scale, số listener, DB write trace |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC77-03 — Rỗng bốn poll mới giảm và chờ consumer đang chạy xong

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-77; JOB-JD-05 mục 5, quy tắc giảm/drain |
| Kind / priority | Recovery, P0: không huỷ bài đang chấm khi giảm |
| Environment | Controller F4 + listener stub có job đang xử lý |
| Preconditions | `targetConsumers=activeConsumers=3`; consumer thứ ba đang xử lý J3; hai consumer còn lại rỗi |
| Test data | `pendingCount=0` trong tám poll liên tiếp; J3 đang chạy ở poll 1..3 và hoàn tất/ack ngay trước poll 4 |
| Steps | Chạy poll 1..3 khi J3 còn chạy; cho J3 hoàn tất, chạy poll 4..8; quan sát target/active, ack của J3. |
| Expected | Poll 1..3 target 3; poll 4 giảm về 2 vì đủ bốn poll rỗng và J3 đã xong, không huỷ J3 hoặc ack sớm. Sau reset bộ đếm, poll 5..7 giữ 2, poll 8 giảm về 1. Không xuống 0; J3 có đúng một kết quả cuối. |
| Cleanup | Khôi phục một consumer, dọn J3/fixture |
| Evidence | Poll timeline, target/active, thời điểm J3 kết thúc/ack, DB kết quả |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC77-04 — Backlog ở khoảng giữa giữ nguyên mục tiêu

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-77; JOB-JD-05 mục 5, khoảng giữa hai mốc |
| Kind / priority | Boundary, P1: không dao động worker khi backlog nhỏ |
| Environment | Controller F4 + repository/listener stub |
| Preconditions | `targetConsumers=activeConsumers=3`, ngưỡng 20, queue mở |
| Test data | `pendingCount` ba poll là `1,20,7` |
| Steps | Chạy ba poll, đọc metric và log đổi mục tiêu. |
| Expected | Target và active vẫn 3 ở cả ba poll; không có log scale, không huỷ job hay ghi cấu hình DB. |
| Cleanup | Đưa mục tiêu về một consumer |
| Evidence | Ba snapshot, metric/log |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC77-05 — Pause ngừng nhận bài mới nhưng không tự giảm mục tiêu

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-77; JOB-JD-05 mục 5 Pause/resume, BR-JD-08 |
| Kind / priority | Concurrency, P0: pause không làm mất backlog hoặc thay mục tiêu |
| Environment | F4 worker + RabbitMQ + API queue consumption + DB |
| Preconditions | `targetConsumers=activeConsumers=3`, một job J0 đang chấm; ADMIN gọi PUT pause; 21 job PENDING chờ |
| Test data | `{paused:true,expectedPaused:false}`, giữ trạng thái qua bốn poll; resume `{paused:false,expectedPaused:true}` |
| Steps | Pause, đợi J0 kết thúc; qua bốn poll, quan sát message mới; resume và quan sát listener. |
| Expected | J0 kết thúc đúng một lần; listener dừng nhận job mới, target vẫn 3 trong pause dù backlog 21; không giảm mục tiêu theo chuỗi rỗng. Sau resume listener mở với mục tiêu 3 và nhận job chờ; không có message bị nack/requeue lặp do pause. |
| Cleanup | Bảo đảm queue được resume; dọn J0 và backlog fixture |
| Evidence | Hai response PUT, metric target/active, consumer/message timeline, DB job |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC77-06 — Tắt autoscale drain về một consumer

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-77; DD F4 API §20, JOB-JD-05 mục 5 |
| Kind / priority | Recovery, P0: tắt công tắc không huỷ bài đang chấm |
| Environment | F4 API/controller + RabbitMQ + listener stub có job đang xử lý |
| Preconditions | `autoscale_enabled=true`, target/active 4, consumer số 2..4 đang xử lý J2..J4; ADMIN có quyền |
| Test data | PUT `/admin/queue/autoscale` `{enabled:false,expectedEnabled:true}`; backlog 0 |
| Steps | Gọi PUT, giữ J2..J4 đang chạy qua một poll; cho từng job kết thúc, chạy poll tiếp và đọc metric/ack. |
| Expected | API xác nhận `enabled=false` sau lưu cấu hình, không đợi scale. Target hướng về 1; active chỉ giảm khi J2..J4 hoàn tất/ack, không huỷ hoặc chấm lại job; cuối cùng target=active=1. |
| Cleanup | Khôi phục cờ autoscale và một consumer; dọn job fixture |
| Evidence | PUT response, metric/log poll, trạng thái và ack J2..J4 |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC77-07 — Hai tiến trình worker chỉ một giữ khoá leader

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-77; JOB-JD-05 mục 5 khoá tư vấn session |
| Kind / priority | Concurrency, P0: tránh hai nhóm consumer nhận cùng queue |
| Environment | PostgreSQL + RabbitMQ + hai tiến trình F4 worker riêng, cùng cấu hình `judge.worker.enabled=true` |
| Preconditions | Cả hai tiến trình cùng khởi động, một kết nối giữ được `pg_try_advisory_lock(hashtext('F4_GRADING_WORKER_LEADER'))` |
| Test data | 21 job PENDING; sau khi ổn định, cắt kết nối PostgreSQL của leader đang giữ khoá |
| Steps | Khởi động đồng thời, quan sát lock/readiness/listener; cắt kết nối leader; quan sát ngừng nhận bài, khởi động lại tiến trình còn sống để tranh khoá như JOB-JD-05 quy định rồi xử lý job còn lại. |
| Expected | Trước lỗi chỉ một tiến trình giữ lock và mở listener; tiến trình kia không mở listener, readiness lỗi. Khi leader mất lock, listener đó dừng nhận message mới; sau khi khoá được nhả, chỉ một tiến trình sống trở thành leader, tổng active consumer không vượt 8, mỗi job có đúng một kết quả cuối. |
| Cleanup | Khôi phục kết nối và một worker leader; dọn job fixture |
| Evidence | PostgreSQL lock owner, readiness, listener/metric theo tiến trình, DB kết quả và RabbitMQ deliveries |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC77-08 — Ngưỡng mới có hiệu lực ở poll kế tiếp

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-77; JOB-JD-05 mục 5, DD F4 API §21 |
| Kind / priority | Boundary, P1: controller dùng snapshot cấu hình mới |
| Environment | F4 API/controller + DB + listener stub |
| Preconditions | Target/active 1, `autoscale_job_threshold=20`, `pendingCount=15`, queue mở và autoscale bật |
| Test data | PUT `/admin/queue/settings` `{autoscaleJobThreshold:10,jobsPerPage:20,errorRetentionHours:24,latencyCapSeconds:10}`; giữ 15 PENDING qua hai poll sau commit |
| Steps | Poll một lần trước PUT, lưu cấu hình, poll hai lần sau PUT; đọc threshold trong log và target. |
| Expected | Poll trước PUT giữ target 1; poll kế dùng threshold 10 nhưng mới là lượt vượt thứ nhất nên vẫn 1; poll sau tăng 1→2. Log scale ghi `threshold=10`, không dùng 20 đã cũ. |
| Cleanup | Khôi phục threshold 20, drain về một consumer, dọn job fixture |
| Evidence | PUT response, DB cấu hình, ba poll/metric/log |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC77-09 — Lỗi đọc số PENDING giữ mục tiêu gần nhất

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-77; JOB-JD-05 mục 5 Quan sát và lỗi |
| Kind / priority | Degraded, P1: không tăng consumer khi không có số backlog đáng tin |
| Environment | Controller F4 + repository stub có fault injection |
| Preconditions | Target/active 1, backlog 21 đã vượt ngưỡng một poll, queue mở/autoscale bật |
| Test data | Poll tiếp theo DB lỗi đúng lúc đếm `submissions.status=PENDING` |
| Steps | Bật lỗi DB tại bước đếm, chạy poll; đọc target/active và log; tắt lỗi, chạy poll tiếp theo với count 21. |
| Expected | Poll lỗi giữ mục tiêu/thực 1, không scale khi thiếu snapshot DB; log lỗi không chứa mã học viên. Sau khi DB phục hồi, controller chỉ tăng theo chuỗi hai poll hợp lệ liên tiếp, không coi poll lỗi là lần vượt ngưỡng thứ hai. |
| Cleanup | Tắt fault, đưa về một consumer, dọn fixture |
| Evidence | Fault timeline, metric target/active và log |
| Design review | Đã review; chưa thực thi |

### TC-F4-AC77-10 — Listener từ chối đổi concurrency không báo active giả

| Field | Value |
| :--- | :--- |
| AC / DD source | F4 AC-77; JOB-JD-05 mục 5 Quan sát và lỗi |
| Kind / priority | Degraded, P1: số consumer thực và lỗi scale phải quan sát được |
| Environment | Controller F4 + listener stub có fault injection |
| Preconditions | Target/active 1, queue mở/autoscale bật, threshold 20 |
| Test data | `pendingCount=21` ở hai poll liên tiếp; tại poll thứ hai listener từ chối đổi concurrency 1→2 |
| Steps | Chạy hai poll, đọc target/active/log và số listener thực; tắt fault, chạy poll kế tiếp. |
| Expected | Không báo `activeConsumers=2` khi thực vẫn 1; có log ERROR về đổi concurrency thất bại và thử lại chu kỳ sau. Khi fault hết, active hội tụ về target hợp lệ, không tạo consumer thứ ba do retry. Snapshot quản trị không có trường chi tiết worker hoặc trạng thái scale thành công giả. |
| Cleanup | Tắt fault, đưa về một consumer, dọn fixture |
| Evidence | Poll timeline, metric target/active, log ERROR và listener count |
| Design review | Đã review; chưa thực thi |
