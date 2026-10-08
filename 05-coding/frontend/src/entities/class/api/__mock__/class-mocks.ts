// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// In-memory mock store for the class cluster (class_management, class_assignments, class_progress,
// class_student_detail). A module-level array (not just a returned literal) so writes made in one
// screen (create class, remove student...) are visible from another screen in the same session —
// ponytail: global mutable module state, fine for a single-tab prototype; swap for the real
// `identity` endpoints wholesale at graduation, not patched incrementally.
import type {
  AttentionItem,
  ClassForm,
  ClassScoreTrend,
  ClassStudent,
  ClassSummary,
  InviteCode,
  StudentStatus,
} from "../../model/types";

function delay<T>(value: T, ms = 350): Promise<T> {
  return new Promise((resolve) => setTimeout(() => resolve(value), ms));
}

let classSeq = 4;
const classes: ClassSummary[] = [
  {
    id: "c1",
    name: "Lập trình Java K21",
    scheduleNote: "Thứ 2 · 4 · 6 — 19:00",
    studentCount: 34,
    completionPct: 68,
    avgScore: 7.2,
    pendingGrading: 4,
    absentCount: 3,
    createdAt: "2026-08-01",
  },
  {
    id: "c2",
    name: "Cấu trúc dữ liệu K22",
    scheduleNote: "Thứ 3 · 5 — 18:30",
    studentCount: 29,
    completionPct: 54,
    avgScore: 6.5,
    pendingGrading: 3,
    absentCount: 5,
    createdAt: "2026-08-10",
  },
  {
    id: "c3",
    name: "Giải thuật nâng cao K20",
    scheduleNote: "Thứ 7 — 08:00",
    studentCount: 19,
    completionPct: 81,
    avgScore: 8.1,
    pendingGrading: 2,
    absentCount: 1,
    createdAt: "2026-08-15",
  },
];

const students: ClassStudent[] = [
  {
    id: "s1",
    name: "Nguyễn Văn An",
    classId: "c1",
    className: "Lập trình Java K21",
    avgScore: 7.8,
    completionPct: 72,
    streakDays: 5,
    trend: [6.2, 6.8, 7.0, 7.8],
    lastActiveLabel: "2 giờ trước",
    status: "on_track",
  },
  {
    id: "s2",
    name: "Trần Thị Bích",
    classId: "c1",
    className: "Lập trình Java K21",
    avgScore: 4.1,
    completionPct: 30,
    streakDays: 0,
    trend: [5.0, 4.8, 4.4, 4.1],
    lastActiveLabel: "9 ngày trước",
    status: "absent",
  },
  {
    id: "s3",
    name: "Lê Hoàng Cường",
    classId: "c1",
    className: "Lập trình Java K21",
    avgScore: 6.0,
    completionPct: 42,
    streakDays: 2,
    trend: [6.8, 6.5, 6.2, 6.0],
    lastActiveLabel: "Hôm qua",
    status: "needs_support",
  },
  {
    id: "s4",
    name: "Phạm Thu Duyên",
    classId: "c1",
    className: "Lập trình Java K21",
    avgScore: 8.5,
    completionPct: 90,
    streakDays: 12,
    trend: [7.9, 8.1, 8.3, 8.5],
    lastActiveLabel: "1 giờ trước",
    status: "on_track",
  },
  {
    id: "s5",
    name: "Đỗ Minh Đức",
    classId: "c2",
    className: "Cấu trúc dữ liệu K22",
    avgScore: 5.5,
    completionPct: 46,
    streakDays: 1,
    trend: [6.0, 5.8, 5.6, 5.5],
    lastActiveLabel: "3 ngày trước",
    status: "watch",
  },
  {
    id: "s6",
    name: "Vũ Ngọc Hà",
    classId: "c2",
    className: "Cấu trúc dữ liệu K22",
    avgScore: null,
    completionPct: 0,
    streakDays: 0,
    trend: [],
    lastActiveLabel: "-",
    status: "insufficient_data",
  },
  {
    id: "s7",
    name: "Bùi Anh Khoa",
    classId: "c2",
    className: "Cấu trúc dữ liệu K22",
    avgScore: 6.9,
    completionPct: 60,
    streakDays: 4,
    trend: [6.1, 6.4, 6.7, 6.9],
    lastActiveLabel: "5 giờ trước",
    status: "on_track",
  },
  {
    id: "s8",
    name: "Hoàng Gia Linh",
    classId: "c2",
    className: "Cấu trúc dữ liệu K22",
    avgScore: 3.8,
    completionPct: 22,
    streakDays: 0,
    trend: [4.5, 4.2, 4.0, 3.8],
    lastActiveLabel: "10 ngày trước",
    status: "absent",
  },
  {
    id: "s9",
    name: "Ngô Thảo My",
    classId: "c3",
    className: "Giải thuật nâng cao K20",
    avgScore: 9.0,
    completionPct: 95,
    streakDays: 21,
    trend: [8.6, 8.8, 8.9, 9.0],
    lastActiveLabel: "30 phút trước",
    status: "on_track",
  },
  {
    id: "s10",
    name: "Đặng Quốc Nam",
    classId: "c3",
    className: "Giải thuật nâng cao K20",
    avgScore: 7.4,
    completionPct: 71,
    streakDays: 6,
    trend: [7.0, 7.1, 7.3, 7.4],
    lastActiveLabel: "4 giờ trước",
    status: "on_track",
  },
  {
    id: "s11",
    name: "Trịnh Bảo Ngọc",
    classId: "c3",
    className: "Giải thuật nâng cao K20",
    avgScore: 5.2,
    completionPct: 38,
    streakDays: 0,
    trend: [6.4, 6.0, 5.6, 5.2],
    lastActiveLabel: "Hôm qua",
    status: "needs_support",
  },
  {
    id: "s12",
    name: "Lâm Thị Oanh",
    classId: "c3",
    className: "Giải thuật nâng cao K20",
    avgScore: 6.6,
    completionPct: 55,
    streakDays: 3,
    trend: [6.9, 6.8, 6.7, 6.6],
    lastActiveLabel: "1 ngày trước",
    status: "watch",
  },
];

const inviteCodes: Record<string, InviteCode[]> = {
  c1: [
    { id: "i1", code: "JAVA21-XK9F", expiresAtLabel: "Còn hạn tới 30/09/2026" },
    { id: "i2", code: "JAVA21-2T4M", expiresAtLabel: "Còn hạn tới 15/10/2026" },
  ],
  c2: [
    { id: "i3", code: "DSA22-7QWE", expiresAtLabel: "Còn hạn tới 05/10/2026" },
  ],
  c3: [
    { id: "i4", code: "ALGO20-P0K1", expiresAtLabel: "Còn hạn tới 20/10/2026" },
  ],
};
let inviteSeq = 5;

// REASON_BY_STATUS: câu ngắn sinh từ chính ngưỡng đã kích hoạt, không phải chuỗi tự do
// (02-bd/screens/teacher/INS0203_class_progress.md Sheet 5 Khu vực C NO 4).
function reasonFor(student: ClassStudent): string {
  if (student.status === "absent") return "Không nộp bài 9 ngày";
  if (student.status === "needs_support") return "Điểm giảm 3 tuần liên tiếp";
  if (student.status === "watch") return `Hoàn thành ${student.completionPct}%`;
  return "";
}

export function listInstructorClasses(): Promise<ClassSummary[]> {
  return delay(
    [...classes].sort((a, b) => (a.createdAt < b.createdAt ? 1 : -1)),
  );
}

export function listClassStudents(classId?: string): Promise<ClassStudent[]> {
  const rows = classId
    ? students.filter((s) => s.classId === classId)
    : students;
  return delay([...rows]);
}

export function getClassStudent(
  studentId: string,
  classId: string,
): Promise<ClassStudent | null> {
  return delay(
    students.find((s) => s.id === studentId && s.classId === classId) ?? null,
  );
}

export function listInviteCodes(classId: string): Promise<InviteCode[]> {
  return delay([...(inviteCodes[classId] ?? [])]);
}

export function createInviteCode(classId: string): Promise<InviteCode> {
  const code: InviteCode = {
    id: `i${inviteSeq++}`,
    code: `NEW-${Math.random().toString(36).slice(2, 6).toUpperCase()}`,
    expiresAtLabel: "Còn hạn 30 ngày",
  };
  inviteCodes[classId] = [...(inviteCodes[classId] ?? []), code];
  return delay(code);
}

export function createClass(form: ClassForm): Promise<ClassSummary> {
  const created: ClassSummary = {
    id: `c${classSeq++}`,
    name: form.name,
    scheduleNote: form.scheduleNote,
    studentCount: 0,
    completionPct: 0,
    avgScore: null,
    pendingGrading: 0,
    absentCount: 0,
    createdAt: new Date().toISOString().slice(0, 10),
  };
  classes.unshift(created);
  inviteCodes[created.id] = [
    {
      id: `i${inviteSeq++}`,
      code: `${form.name.slice(0, 4).toUpperCase()}-INIT`,
      expiresAtLabel: "Còn hạn 30 ngày",
    },
  ];
  return delay(created);
}

export function updateClass(
  id: string,
  form: ClassForm,
): Promise<ClassSummary> {
  const target = classes.find((c) => c.id === id);
  if (!target) throw new Error("Lớp không tồn tại");
  target.name = form.name;
  target.scheduleNote = form.scheduleNote;
  return delay(target);
}

export function deleteClass(id: string): Promise<void> {
  const index = classes.findIndex((c) => c.id === id);
  if (index >= 0) classes.splice(index, 1);
  return delay(undefined);
}

export function removeStudent(studentId: string): Promise<void> {
  const index = students.findIndex((s) => s.id === studentId);
  if (index >= 0) students.splice(index, 1);
  return delay(undefined);
}

export function listClassAttention(classId?: string): Promise<AttentionItem[]> {
  const scoped = classId
    ? students.filter((s) => s.classId === classId)
    : students;
  const order: StudentStatus[] = ["absent", "needs_support", "watch"];
  const items = scoped
    .filter((s) => order.includes(s.status))
    .sort((a, b) => order.indexOf(a.status) - order.indexOf(b.status))
    .slice(0, 5)
    .map((s) => ({
      studentId: s.id,
      classId: s.classId,
      name: s.name,
      reason: reasonFor(s),
      status: s.status,
    }));
  return delay(items);
}

export function classScoreTrend(classId?: string): Promise<ClassScoreTrend> {
  const scoped = classId ? classes.filter((c) => c.id === classId) : classes;
  const weekLabels = [
    "Tuần 1",
    "Tuần 2",
    "Tuần 3",
    "Tuần 4",
    "Tuần 5",
    "Tuần 6",
  ];
  const series = scoped.map((c, index) => ({
    classId: c.id,
    className: c.name,
    // Deterministic 6-point ramp toward the class's current avgScore — no persisted weekly
    // snapshot table exists yet (Câu hỏi mở Q3 of class_progress BD), so this is a stand-in shape.
    points: weekLabels.map((_, week) => {
      const base = (c.avgScore ?? 6) - 1.2 + week * 0.24 + index * 0.05;
      return Math.round(base * 10) / 10;
    }),
  }));
  return delay({ weekLabels, series });
}
