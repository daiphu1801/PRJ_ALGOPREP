// PROTOTYPE mock — no backend endpoint exists yet.
// Copied from 09-layoutBase/Admin - Nhật ký hệ thống.dc.html:395-437, MINUS everything belonging to
// the removed rejudge feature (DEC-2026-0828-remove-rejudge-scope), exactly as
// 02-bd/screens/admin/admin_system_log.md section 0 instructs:
//   - the "Phiên chấm lại đã chạy" stat card is gone (3 cards, not 4),
//   - the 15:18:02 "#RJ-0139" event is gone (9 events, not 10),
//   - the "Chấm lại" category is gone (4 categories, not 5).
import type {
  ActiveAdmin,
  AuditEvent,
  AuditLogPage,
  CategoryCount,
} from "../../model/types";

// dc.html:402-413, the rejudge row dropped.
const TODAY = "2026-10-08";

const EVENTS: AuditEvent[] = [
  {
    date: TODAY,
    time: "15:39:18",
    category: "config",
    message: "Cập nhật giới hạn thời gian Python 3 từ 2s lên 3s",
    service: "config",
    actor: "pdai",
    id: "evt_9f2a53",
  },
  {
    date: TODAY,
    time: "15:35:30",
    category: "auth",
    message: "Khóa tài khoản hmtri",
    service: "auth",
    actor: "pdai",
    id: "evt_9f29f7",
    reason:
      "Phát hiện nhiều tài khoản đăng ký hàng loạt từ cùng một địa chỉ IP",
  },
  {
    date: TODAY,
    time: "15:33:41",
    category: "auth",
    message: "Mở khóa tài khoản bklinh",
    service: "auth",
    actor: "pdai",
    id: "evt_9f29b3",
  },
  {
    date: TODAY,
    time: "15:31:05",
    category: "content",
    message: "Thêm testcase biên cho bài Đếm số đảo ngược",
    service: "content",
    actor: "pthuong",
    id: "evt_9f2984",
  },
  {
    date: TODAY,
    time: "15:24:47",
    category: "permission",
    message: "Cấp quyền UPDATE trên TESTCASE_MANAGEMENT cho vai trò Trợ giảng",
    service: "permission",
    actor: "pdai",
    id: "evt_9f2911",
  },
  {
    date: TODAY,
    time: "15:02:39",
    category: "auth",
    message: "Đổi vai trò tài khoản pthuong từ STUDENT sang INSTRUCTOR",
    service: "auth",
    actor: "pdai",
    id: "evt_9f27e0",
  },
  {
    date: TODAY,
    time: "14:47:11",
    category: "config",
    message: "Cập nhật prompt Phỏng vấn giả lập lên bản v2.5",
    service: "ai-config",
    actor: "pdai",
    id: "evt_9f26a5",
  },
  {
    date: TODAY,
    time: "14:30:56",
    category: "content",
    message: "Xuất bản bài toán Minimum Window Substring",
    service: "content",
    actor: "pthuong",
    id: "evt_9f2521",
  },
  {
    date: TODAY,
    time: "14:12:04",
    category: "auth",
    message: "Reset mật khẩu cho tài khoản tranbich theo yêu cầu",
    service: "auth",
    actor: "pdai",
    id: "evt_9f23c9",
  },
  {
    date: TODAY,
    time: "13:55:37",
    category: "permission",
    message: 'Tạo vai trò mới "Trợ giảng", chưa gán quyền nào',
    service: "permission",
    actor: "pdai",
    id: "evt_9f21a0",
  },
];

// dc.html:431-435.
const ACTIVE_ADMINS: ActiveAdmin[] = [
  {
    name: "Phú Đại",
    meta: "Quản trị viên · 28 hành động",
    lastActionAt: "2 phút trước",
    colorVar: "--color-success",
  },
  {
    name: "Minh Trí",
    meta: "Quản trị viên · 9 hành động",
    lastActionAt: "18 phút trước",
    colorVar: "--color-success",
  },
  {
    name: "P. Thương",
    meta: "Giảng viên · 6 hành động",
    lastActionAt: "1 giờ trước",
    colorVar: "--color-accent-blue",
  },
  {
    name: "Thu Hà",
    meta: "Giảng viên · 3 hành động",
    lastActionAt: "3 giờ trước",
  },
];

// dc.html:437-439, the rejudge category dropped.
const BREAKDOWN: CategoryCount[] = [
  { category: "auth", count: 22 },
  { category: "permission", count: 5 },
  { category: "config", count: 11 },
  { category: "content", count: 8 },
];

// dc.html:451 — "trong 12.408 sự kiện".
const TOTAL_EVENTS = 12408;

// Page size of "Tải thêm", and the day each older batch lands on. Synthetic history: the real list is
// 12.408 rows behind a keyset cursor, which no mock can reproduce.
export const LOAD_MORE_COUNT = 50;

export function fetchMoreAuditEvents(loaded: number): AuditEvent[] {
  const count = Math.min(LOAD_MORE_COUNT, TOTAL_EVENTS - loaded);
  return Array.from({ length: Math.max(count, 0) }, (_, i) => {
    const n = loaded + i;
    const template = EVENTS[n % EVENTS.length]!;
    // Five events a day, walking backwards from yesterday.
    const day = new Date(Date.UTC(2026, 9, 7 - Math.floor(n / 5)));
    return {
      ...template,
      date: day.toISOString().slice(0, 10),
      id: `evt_${(0x9f2000 - n).toString(16)}`,
    };
  });
}

export function fetchAuditLogPage(): AuditLogPage {
  return {
    events: EVENTS.map((event) => ({ ...event })),
    activeAdmins: ACTIVE_ADMINS.map((admin) => ({ ...admin })),
    breakdown: BREAKDOWN.map((item) => ({ ...item })),
    totalEvents: TOTAL_EVENTS,
  };
}
