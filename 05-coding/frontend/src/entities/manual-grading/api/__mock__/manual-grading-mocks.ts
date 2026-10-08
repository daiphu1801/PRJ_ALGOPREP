// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import type {
  GradingStatusFilter,
  ManualGradingItem,
  ManualGradingStats,
} from "../../model/types";

function delay<T>(value: T, ms = 350): Promise<T> {
  return new Promise((resolve) => setTimeout(() => resolve(value), ms));
}

const queue: ManualGradingItem[] = [
  {
    id: "g1",
    studentId: "s2",
    studentName: "Trần Thị Bích",
    problemTitle: "Two Sum",
    classId: "c1",
    className: "Lập trình Java K21",
    submittedAtLabel: "2 giờ trước",
    createdAt: "2026-09-23T08:00:00Z",
    aiScore10: 4.2,
    manualScore: null,
    manualComment: null,
  },
  {
    id: "g2",
    studentId: "s3",
    studentName: "Lê Hoàng Cường",
    problemTitle: "Merge Intervals",
    classId: "c1",
    className: "Lập trình Java K21",
    submittedAtLabel: "5 giờ trước",
    createdAt: "2026-09-23T05:00:00Z",
    aiScore10: 5.1,
    manualScore: null,
    manualComment: null,
  },
  {
    id: "g3",
    studentId: "s5",
    studentName: "Đỗ Minh Đức",
    problemTitle: "Course Schedule",
    classId: "c2",
    className: "Cấu trúc dữ liệu K22",
    submittedAtLabel: "Hôm qua",
    createdAt: "2026-09-22T10:00:00Z",
    aiScore10: 3.6,
    manualScore: null,
    manualComment: null,
  },
  {
    id: "g4",
    studentId: "s8",
    studentName: "Hoàng Gia Linh",
    problemTitle: "Coin Change",
    classId: "c2",
    className: "Cấu trúc dữ liệu K22",
    submittedAtLabel: "2 ngày trước",
    createdAt: "2026-09-21T09:00:00Z",
    aiScore10: 5.8,
    manualScore: 7,
    manualComment: "Logic ổn, cần tối ưu độ phức tạp.",
  },
  {
    id: "g5",
    studentId: "s11",
    studentName: "Trịnh Bảo Ngọc",
    problemTitle: "LRU Cache",
    classId: "c3",
    className: "Giải thuật nâng cao K20",
    submittedAtLabel: "3 ngày trước",
    createdAt: "2026-09-20T09:00:00Z",
    aiScore10: 4.9,
    manualScore: null,
    manualComment: null,
  },
  {
    id: "g6",
    studentId: "s12",
    studentName: "Lâm Thị Oanh",
    problemTitle: "Two Sum",
    classId: "c3",
    className: "Giải thuật nâng cao K20",
    submittedAtLabel: "4 ngày trước",
    createdAt: "2026-09-19T09:00:00Z",
    aiScore10: 5.5,
    manualScore: 6,
    manualComment: "Bài giải ngắn gọn.",
  },
];

export function listManualGradingQueue(
  status: GradingStatusFilter,
  classId?: string,
  studentId?: string,
): Promise<ManualGradingItem[]> {
  const rows = queue
    .filter((item) =>
      status === "all"
        ? true
        : status === "graded"
          ? item.manualScore !== null
          : item.manualScore === null,
    )
    .filter((item) => !classId || classId === "all" || item.classId === classId)
    .filter((item) => !studentId || item.studentId === studentId)
    .sort((a, b) => (a.createdAt < b.createdAt ? -1 : 1));
  return delay([...rows]);
}

export function manualGradingStats(): Promise<ManualGradingStats> {
  const pendingCount = queue.filter((item) => item.manualScore === null).length;
  const graded = queue.filter((item) => item.manualScore !== null);
  const avgManualScore = graded.length
    ? Math.round(
        (graded.reduce((sum, item) => sum + (item.manualScore ?? 0), 0) /
          graded.length) *
          10,
      ) / 10
    : null;
  return delay({ pendingCount, gradedCount7d: graded.length, avgManualScore });
}

export function listPendingManualGradingTop(
  limit = 5,
): Promise<ManualGradingItem[]> {
  return delay(
    queue
      .filter((item) => item.manualScore === null)
      .sort((a, b) => (a.createdAt < b.createdAt ? -1 : 1))
      .slice(0, limit),
  );
}

export function saveManualGrade(
  id: string,
  score: number,
  comment: string,
): Promise<ManualGradingItem> {
  const item = queue.find((row) => row.id === id);
  if (!item) throw new Error("Không tìm thấy bài chấm");
  item.manualScore = score;
  item.manualComment = comment;
  return delay(item);
}
