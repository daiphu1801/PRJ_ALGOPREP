import { beforeEach, describe, expect, it, vi } from "vitest";

const get = vi.fn();

vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: { get: (...args: unknown[]) => get(...args) },
}));

// Import after the mock so the module talks to the fake `api`.
import { ApiError } from "@/shared/api";
import {
  realGetClassStudent,
  realGetClassSummary,
  realListClassSummary,
  realListInstructorStudents,
} from "./real-class-metrics";

const CARD = {
  classId: "c1",
  name: "Lớp A",
  description: null,
  schedule: null,
  studentCount: 4,
  completionPercent: 25,
  avgScore: 5.0,
  absentCount: 1,
  createdAt: "2026-11-01T03:00:00Z",
};

describe("realListClassSummary (#56)", () => {
  beforeEach(() => {
    get.mockReset();
  });

  it("passes on the distinct-people totals, not the sum of the cards", async () => {
    // One student sits in both classes: the cards add up to 6, the server counts 4 people.
    get.mockResolvedValue({
      items: [CARD, { ...CARD, classId: "c2", studentCount: 2 }],
      studentCount: 4,
      absentStudentCount: 1,
    });

    const result = await realGetClassSummary();

    expect(result.totals).toEqual({ studentCount: 4, absentStudentCount: 1 });
    expect(result.items.reduce((n, c) => n + c.studentCount, 0)).toBe(6);
  });

  it("maps the cards with their figures and keeps null as unknown", async () => {
    get.mockResolvedValue({
      items: [
        CARD,
        {
          ...CARD,
          classId: "c2",
          studentCount: 0,
          completionPercent: null,
          avgScore: null,
          absentCount: 0,
        },
      ],
    });

    const classes = await realListClassSummary();

    expect(get).toHaveBeenCalledWith("/identity/instructor/classes/summary");
    expect(classes[0]).toMatchObject({
      id: "c1",
      completionPct: 25,
      avgScore: 5,
      absentCount: 1,
      studentCount: 4,
    });
    expect(classes[1]).toMatchObject({
      id: "c2",
      completionPct: null,
      avgScore: null,
      absentCount: 0,
    });
  });

  it("falls back to the plain list (#19) when the judge figures are down (IDT-507)", async () => {
    get.mockImplementation(async (path: string) => {
      if (path.endsWith("/summary")) throw new ApiError("IDT-507", 503, "x");
      return {
        items: [
          {
            id: "c1",
            name: "Lớp A",
            description: null,
            schedule: null,
            studentCount: 4,
            createdAt: "2026-11-01T03:00:00Z",
          },
        ],
      };
    });

    const classes = await realListClassSummary();

    expect(get).toHaveBeenLastCalledWith("/identity/classes/my");
    expect(classes).toHaveLength(1);
    expect(classes[0]!.absentCount).toBeUndefined();
    expect(classes[0]!.completionPct).toBeUndefined();
  });

  it("rethrows any other error, such as the 403 of a missing permission", async () => {
    get.mockRejectedValue(new ApiError("IDT-445", 403, "no"));

    await expect(realListClassSummary()).rejects.toMatchObject({
      code: "IDT-445",
    });
    expect(get).toHaveBeenCalledTimes(1);
  });
});

describe("realListInstructorStudents (#59)", () => {
  beforeEach(() => {
    get.mockReset();
  });

  const ROW = {
    classId: "c1",
    className: "Lớp A",
    studentId: "s1",
    displayName: "Nguyễn Văn An",
    joinedAt: "2026-08-01T00:00:00Z",
    completionPercent: 50,
    avgScore: 7.5,
    status: "NEEDS_SUPPORT",
    streakDays: 2,
    trendPoints: [null, 8, null, 6.5],
    lastSubmittedAt: null,
  };

  it("sends only the filters that are set and maps rows and paging", async () => {
    get.mockResolvedValue({
      items: [ROW],
      page: { page: 2, pageSize: 20, totalCount: 21, totalPages: 2 },
    });

    const page = await realListInstructorStudents({
      classId: "c1",
      q: "an",
      page: 2,
      pageSize: 20,
    });

    expect(get).toHaveBeenCalledWith("/identity/instructor/students", {
      classId: "c1",
      q: "an",
      page: 2,
      pageSize: 20,
    });
    expect(page.total).toBe(21);
    expect(page.totalPages).toBe(2);
    expect(page.rows[0]).toEqual({
      id: "s1",
      name: "Nguyễn Văn An",
      classId: "c1",
      className: "Lớp A",
      avgScore: 7.5,
      completionPct: 50,
      streakDays: 2,
      trend: [8, 6.5],
      lastActiveLabel: "-",
      status: "needs_support",
    });
  });

  it("leaves classId and q out when empty", async () => {
    get.mockResolvedValue({
      items: [],
      page: { page: 1, pageSize: 20, totalCount: 0, totalPages: 0 },
    });

    await realListInstructorStudents({ q: "", page: 1, pageSize: 20 });

    expect(get).toHaveBeenCalledWith("/identity/instructor/students", {
      page: 1,
      pageSize: 20,
    });
  });

  it("rethrows the 400 of a bad filter", async () => {
    get.mockRejectedValue(new ApiError("IDT-V17", 400, "bad"));

    await expect(
      realListInstructorStudents({ q: "", page: 0, pageSize: 20 }),
    ).rejects.toMatchObject({ code: "IDT-V17" });
  });
});

describe("realGetClassStudent (#28)", () => {
  beforeEach(() => {
    get.mockReset();
  });

  it("maps the profile and the best result of each problem", async () => {
    get.mockResolvedValue({
      classId: "c1",
      studentId: "s1",
      displayName: "Binh",
      email: "b@x.vn",
      phone: null,
      className: "Lớp A",
      joinedAt: "2026-08-01T00:00:00Z",
      status: "ABSENT",
      assignmentProgress: [
        { problemId: "p1", bestRatio: 1, bestVerdict: "ACCEPTED" },
        { problemId: "p2", bestRatio: null, bestVerdict: null },
      ],
    });

    const student = await realGetClassStudent("c1", "s1");

    expect(get).toHaveBeenCalledWith("/identity/classes/c1/students/s1/detail");
    expect(student).toEqual({
      id: "s1",
      name: "Binh",
      classId: "c1",
      className: "Lớp A",
      email: "b@x.vn",
      phone: undefined,
      status: "absent",
      assignmentProgress: [
        { problemId: "p1", bestRatio: 1, bestVerdict: "ACCEPTED" },
        { problemId: "p2", bestRatio: null, bestVerdict: null },
      ],
    });
  });

  it("is null for IDT-414 and rethrows the 503 of an unavailable judge", async () => {
    get.mockRejectedValueOnce(new ApiError("IDT-414", 404, "gone"));
    expect(await realGetClassStudent("c1", "s1")).toBeNull();

    get.mockRejectedValueOnce(new ApiError("IDT-507", 503, "down"));
    await expect(realGetClassStudent("c1", "s1")).rejects.toMatchObject({
      code: "IDT-507",
    });
  });
});
