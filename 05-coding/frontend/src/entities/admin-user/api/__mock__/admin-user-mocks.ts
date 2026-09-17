// PROTOTYPE mock — no backend endpoint exists yet.
// Values from 09-layoutBase/Admin - Người dùng.dc.html:422-487.
import type {
  AdminUser,
  AdminUserPage,
  AdminUserStat,
  PendingTask,
  RoleDistributionItem,
} from "../../model/types";

// dc.html:422-428.
const STATS: AdminUserStat[] = [
  {
    key: "total",
    value: "1.284",
    delta: "+42",
    deltaColorVar: "--color-success",
    meta: "32 tài khoản mới trong tuần",
  },
  {
    key: "active24h",
    value: "312",
    delta: "+8%",
    deltaColorVar: "--color-success",
    meta: "24% tổng số người học",
  },
  {
    key: "pendingEmail",
    value: "18",
    delta: "−3",
    deltaColorVar: "--color-admin-warn",
    meta: "Quá 7 ngày sẽ tự huỷ",
  },
  {
    key: "locked",
    value: "7",
    delta: "+2",
    deltaColorVar: "--color-admin-negative",
    meta: "3 do vi phạm gian lận",
  },
  {
    key: "passwordResets",
    value: "6",
    delta: "+1",
    deltaColorVar: "--color-admin-warn",
    meta: "Gửi trong 24 giờ qua",
  },
];

// dc.html:437-447. Counts are plain numbers here; the mockup pre-formatted them as strings.
const USERS: AdminUser[] = [
  { name: "Nguyễn Văn An", email: "nguyenvana@sv.edu.vn", role: "student", solvedCount: 142, submissionCount: 1284, lastActiveLabel: "5 phút trước", status: "active" },
  { name: "Trần Thị Bích", email: "tranbich@sv.edu.vn", role: "student", solvedCount: 118, submissionCount: 964, lastActiveLabel: "22 phút trước", status: "active" },
  { name: "Lê Hoàng Nam", email: "lhnam@sv.edu.vn", role: "student", solvedCount: 96, submissionCount: 812, lastActiveLabel: "1 giờ trước", status: "active" },
  { name: "Phạm Thu Hương", email: "pthuong@gv.edu.vn", role: "instructor", solvedCount: 64, submissionCount: 210, lastActiveLabel: "3 giờ trước", status: "active" },
  { name: "Đặng Phú Đại", email: "pdai@algoprep.vn", role: "admin", solvedCount: 38, submissionCount: 96, lastActiveLabel: "Đang trực tuyến", status: "active" },
  { name: "Vũ Thu Hà", email: "vuthuha@sv.edu.vn", role: "student", solvedCount: 81, submissionCount: 702, lastActiveLabel: "6 giờ trước", status: "active" },
  { name: "Đỗ Quốc Bảo", email: "dqbao@sv.edu.vn", role: "student", solvedCount: 12, submissionCount: 48, lastActiveLabel: "2 ngày trước", status: "pending" },
  { name: "Hoàng Minh Trí", email: "hmtri@sv.edu.vn", role: "student", solvedCount: 54, submissionCount: 1910, lastActiveLabel: "4 ngày trước", status: "locked" },
  { name: "Bùi Khánh Linh", email: "bklinh@gv.edu.vn", role: "instructor", solvedCount: 73, submissionCount: 304, lastActiveLabel: "1 ngày trước", status: "active" },
];

// dc.html:476-481.
const ROLE_DISTRIBUTION: RoleDistributionItem[] = [
  { role: "student", label: "1.196 · 93%", percent: 93 },
  { role: "instructor", label: "74 · 6%", percent: 18 },
  { role: "admin", label: "9 · 1%", percent: 6 },
  { role: "deactivated", label: "5 · <1%", percent: 4 },
];

// dc.html:483-486.
const PENDING_TASKS: PendingTask[] = [
  { key: "pendingEmail", count: 18, colorVar: "--color-admin-warn" },
  { key: "instructorRequest", count: 2, colorVar: "--color-accent-blue" },
];

export function fetchAdminUserPage(): AdminUserPage {
  return {
    stats: STATS.map((stat) => ({ ...stat })),
    users: USERS.map((user) => ({ ...user })),
    roleDistribution: ROLE_DISTRIBUTION.map((item) => ({ ...item })),
    pendingTasks: PENDING_TASKS.map((task) => ({ ...task })),
    totalUsers: 1284,
    // dc.html:499 — "Trang 1 trong 143".
    totalPages: 143,
  };
}
