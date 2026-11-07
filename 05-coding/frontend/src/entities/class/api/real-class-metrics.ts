// Real calls of the class metric reads (03-dd/api/F1_identity.md #28, #56, #59; "Trien khai 2026-11-07").
// The figures come from the judge through identity: when that source is down the server answers
// 503 IDT-507, never zeros, so the class list falls back to the plain list (#19) and every other
// read lets the screen show the error. #29 (progress), #57 (score trend) and #58 (attention) have no
// screen that reads them yet, so they are not here.
import { ApiError, api } from "@/shared/api";
import { formatRelativeVN } from "@/shared/lib";
import type {
  ClassStudent,
  ClassStudentDetail,
  ClassSummary,
  InstructorStudentPage,
  StudentStatus,
} from "../model/types";
import { realListInstructorClasses, toClassSummary } from "./real-class";

const ID = "/identity";

type CardDto = {
  classId: string;
  name: string;
  description: string | null;
  schedule: Parameters<typeof toClassSummary>[0]["schedule"];
  studentCount: number;
  completionPercent: number | null;
  avgScore: number | null;
  absentCount: number;
  createdAt: string;
};

/** The people counts of #56: each student once, however many of the instructor's classes they sit in. */
export type ClassSummaryTotals = {
  studentCount: number;
  absentStudentCount: number;
};

export type ClassSummaryResult = {
  items: ClassSummary[];
  /** `null` when the judge figures are down and the plain list (#19) stood in. */
  totals: ClassSummaryTotals | null;
};

/**
 * #56: the class cards with completion, average score and absent count, plus the distinct-people
 * totals. A class with no assigned problem has `completionPercent = null` and a class with no
 * graded submission `avgScore = null`; both stay "unknown" on the screen.
 */
export async function realGetClassSummary(): Promise<ClassSummaryResult> {
  try {
    const { items, studentCount, absentStudentCount } = await api.get<{
      items: CardDto[];
      studentCount: number;
      absentStudentCount: number;
    }>(`${ID}/instructor/classes/summary`);
    return {
      items: items.map((card) => ({
        ...toClassSummary({ ...card, id: card.classId }),
        completionPct: card.completionPercent,
        avgScore: card.avgScore,
        absentCount: card.absentCount,
      })),
      totals: { studentCount, absentStudentCount },
    };
  } catch (error) {
    if (error instanceof ApiError && error.code === "IDT-507") {
      return { items: await realListInstructorClasses(), totals: null };
    }
    throw error;
  }
}

export async function realListClassSummary(): Promise<ClassSummary[]> {
  return (await realGetClassSummary()).items;
}

type StudentRowDto = {
  classId: string;
  className: string;
  studentId: string;
  displayName: string;
  joinedAt: string;
  completionPercent: number;
  avgScore: number | null;
  status: string;
  streakDays: number;
  trendPoints: (number | null)[];
  lastSubmittedAt: string | null;
};

export function toStudentStatus(value: string): StudentStatus {
  return value.toLowerCase() as StudentStatus;
}

function toStudent(dto: StudentRowDto): ClassStudent {
  return {
    id: dto.studentId,
    name: dto.displayName,
    classId: dto.classId,
    className: dto.className,
    avgScore: dto.avgScore,
    completionPct: dto.completionPercent,
    streakDays: dto.streakDays,
    // A week without a graded submission has no point; the sparkline draws the weeks that have one.
    trend: dto.trendPoints.filter((point): point is number => point !== null),
    lastActiveLabel: dto.lastSubmittedAt
      ? formatRelativeVN(dto.lastSubmittedAt)
      : "-",
    status: toStudentStatus(dto.status),
  };
}

export type StudentPageQuery = {
  classId?: string;
  q: string;
  page: number;
  pageSize: number;
};

/** #59: one page of (class, student) rows; search and paging are the server's. */
export async function realListInstructorStudents(
  query: StudentPageQuery,
): Promise<InstructorStudentPage> {
  const { items, page } = await api.get<{
    items: StudentRowDto[];
    page: { totalCount: number; totalPages: number };
  }>(`${ID}/instructor/students`, {
    ...(query.classId ? { classId: query.classId } : {}),
    ...(query.q ? { q: query.q } : {}),
    page: query.page,
    pageSize: query.pageSize,
  });
  return {
    rows: items.map(toStudent),
    total: page.totalCount,
    totalPages: page.totalPages,
  };
}

type ProfileDto = {
  classId: string;
  studentId: string;
  displayName: string;
  email: string | null;
  phone: string | null;
  className: string;
  joinedAt: string;
  status: string;
  assignmentProgress: {
    problemId: string;
    bestRatio: number | null;
    bestVerdict: string | null;
  }[];
};

/** #28: one student of the class; IDT-414 (not in this class, left, or not yours) is "not found". */
export async function realGetClassStudent(
  classId: string,
  studentId: string,
): Promise<ClassStudentDetail | null> {
  try {
    const dto = await api.get<ProfileDto>(
      `${ID}/classes/${classId}/students/${studentId}/detail`,
    );
    return {
      id: dto.studentId,
      name: dto.displayName,
      classId: dto.classId,
      className: dto.className,
      email: dto.email ?? undefined,
      phone: dto.phone ?? undefined,
      status: toStudentStatus(dto.status),
      assignmentProgress: dto.assignmentProgress.map((item) => ({
        problemId: item.problemId,
        bestRatio: item.bestRatio,
        bestVerdict: item.bestVerdict,
      })),
    };
  } catch (error) {
    if (error instanceof ApiError && error.code === "IDT-414") {
      return null;
    }
    throw error;
  }
}
