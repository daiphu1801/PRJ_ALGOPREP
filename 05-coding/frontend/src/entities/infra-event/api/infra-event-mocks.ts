// PROTOTYPE mock — no backend endpoint exists yet. Only the two sources the backend really has
// (ponytail decision, ADM0404 Q2): stuck Outbox rows (`outbox_events.publish_attempts`, warn from 3
// attempts, error from 5) and judge/sandbox failures that end in SYSTEM_ERROR. Three days, newest first.
import type { InfraEvent, InfraLevel } from "../model/types";

const ROWS: [string, string, InfraLevel, string, string][] = [
  [
    "2026-10-10",
    "15:41:02",
    "error",
    "go-judge worker 3 mất kết nối, 4 job được xếp lại hàng đợi",
    "go-judge",
  ],
  [
    "2026-10-10",
    "15:38:44",
    "warn",
    "Job #48207 chờ quá 60 giây vì worker 3 mất kết nối",
    "go-judge",
  ],
  [
    "2026-10-10",
    "15:34:12",
    "error",
    "Bản ghi outbox #3325 đã thử gửi lại 5 lần, chưa gửi được",
    "outbox",
  ],
  [
    "2026-10-10",
    "15:12:40",
    "warn",
    "Bản ghi outbox #3325 đã thử gửi lại 3 lần",
    "outbox",
  ],
  [
    "2026-10-10",
    "14:58:51",
    "info",
    "Bản ghi outbox #3321 đã gửi lại thành công",
    "outbox",
  ],
  [
    "2026-10-10",
    "14:58:09",
    "warn",
    "Bản ghi outbox #3321 đã thử gửi lại 4 lần",
    "outbox",
  ],
  [
    "2026-10-10",
    "13:20:15",
    "error",
    "Sandbox worker 2 bị OOM, lượt nộp #48310 chuyển SYSTEM_ERROR",
    "go-judge",
  ],
  [
    "2026-10-10",
    "11:05:18",
    "warn",
    "Quét job treo: 2 lượt nộp quá 5 phút không có cập nhật, chuyển SYSTEM_ERROR",
    "go-judge",
  ],
  [
    "2026-10-10",
    "08:02:27",
    "warn",
    "Worker go-judge-w1 không phản hồi kiểm tra sức khoẻ, thử lại lần 2",
    "go-judge",
  ],
  [
    "2026-10-09",
    "23:40:12",
    "error",
    "Sandbox worker 2 bị OOM, lượt nộp #48102 chuyển SYSTEM_ERROR",
    "go-judge",
  ],
  [
    "2026-10-09",
    "20:03:09",
    "warn",
    "Bản ghi outbox #3290 đã thử gửi lại 3 lần",
    "outbox",
  ],
  [
    "2026-10-09",
    "16:44:50",
    "warn",
    "Quét job treo: 1 lượt nộp quá 5 phút không có cập nhật, chuyển SYSTEM_ERROR",
    "go-judge",
  ],
  [
    "2026-10-09",
    "14:10:22",
    "info",
    "Bản ghi outbox #3290 đã gửi lại thành công",
    "outbox",
  ],
  [
    "2026-10-09",
    "10:36:48",
    "error",
    "Sandbox worker 4 không khởi tạo được cgroup, lượt nộp #48055 chuyển SYSTEM_ERROR",
    "go-judge",
  ],
  [
    "2026-10-09",
    "07:59:03",
    "warn",
    "go-judge worker 4 khởi động lại, 1 job được xếp lại hàng đợi",
    "go-judge",
  ],
  [
    "2026-10-08",
    "18:06:10",
    "info",
    "Worker go-judge-w1 được khởi động lại thủ công",
    "go-judge",
  ],
  [
    "2026-10-08",
    "18:05:36",
    "error",
    "go-judge worker 1 không phản hồi kiểm tra sức khoẻ",
    "go-judge",
  ],
  [
    "2026-10-08",
    "15:27:19",
    "warn",
    "Sandbox worker 3 chạm giới hạn tiến trình con trên lượt nộp #47891",
    "go-judge",
  ],
  [
    "2026-10-08",
    "12:45:30",
    "info",
    "Bản ghi outbox #3254 đã gửi lại thành công",
    "outbox",
  ],
  [
    "2026-10-08",
    "12:44:55",
    "warn",
    "Bản ghi outbox #3254 đã thử gửi lại 3 lần",
    "outbox",
  ],
  [
    "2026-10-08",
    "09:00:14",
    "error",
    "Bản ghi outbox #3249 đã thử gửi lại 6 lần, chuyển sang hàng đợi lỗi",
    "outbox",
  ],
  [
    "2026-10-08",
    "08:15:02",
    "info",
    "go-judge worker 2 đăng ký lại vào cụm",
    "go-judge",
  ],
];

const EVENTS: InfraEvent[] = ROWS.map(
  ([date, time, level, message, service], index) => ({
    id: `inf-${index + 1}`,
    date,
    time,
    level,
    message,
    service,
  }),
);

export function fetchInfraEvents(): InfraEvent[] {
  return EVENTS.map((event) => ({ ...event }));
}

/** Warning and error counts on the newest day in the log, for the queue screen's link row. */
export function countRecentInfraAlerts(): { warn: number; error: number } {
  const latest = EVENTS[0]?.date;
  const today = EVENTS.filter((event) => event.date === latest);
  return {
    warn: today.filter((event) => event.level === "warn").length,
    error: today.filter((event) => event.level === "error").length,
  };
}
