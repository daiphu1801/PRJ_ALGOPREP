import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api";

const get = vi.fn();
const post = vi.fn();
const patch = vi.fn();
const del = vi.fn();

vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: {
    get: (...args: unknown[]) => get(...args),
    post: (...args: unknown[]) => post(...args),
    patch: (...args: unknown[]) => patch(...args),
    delete: (...args: unknown[]) => del(...args),
  },
}));

// Import after the mock so the module talks to the fake `api`.
import {
  realCreateClass,
  realCreateInviteCode,
  realDeleteClass,
  realGetMyClass,
  realJoinClass,
  realLeaveClass,
  realListInstructorClasses,
  realListInviteCodes,
  realListMyClasses,
  realListRoster,
  realUpdateClass,
  toSchedule,
} from "./real-class";

const CLASSES = "/identity/classes";

const classDto = {
  id: "c1",
  name: "Java K21",
  description: null,
  schedule: {
    days: [1, 3],
    startTime: "19:00",
    endTime: "21:00",
    room: null,
    startDate: null,
    endDate: null,
    note: null,
  },
  studentCount: 12,
  createdAt: "2026-11-01T03:00:00Z",
};

beforeEach(() => {
  get.mockReset();
  post.mockReset();
  patch.mockReset();
  del.mockReset();
});

describe("toSchedule", () => {
  it("drops the parts that came back null and reads an empty object as no schedule", () => {
    expect(toSchedule(classDto.schedule)).toEqual({
      days: [1, 3],
      startTime: "19:00",
      endTime: "21:00",
      room: undefined,
      startDate: undefined,
      endDate: undefined,
      note: undefined,
    });
    expect(toSchedule({})).toBeUndefined();
    expect(toSchedule(null)).toBeUndefined();
    expect(toSchedule({ days: [], note: null })).toBeUndefined();
  });
});

describe("instructor classes (#19 to #22)", () => {
  it("lists the classes without the figures that need F4", async () => {
    get.mockResolvedValue({ items: [classDto] });

    const [first] = await realListInstructorClasses();

    expect(get).toHaveBeenCalledWith(`${CLASSES}/my`);
    expect(first).toMatchObject({
      id: "c1",
      name: "Java K21",
      description: undefined,
      studentCount: 12,
    });
    expect(first).not.toHaveProperty("completionPct");
    expect(first).not.toHaveProperty("absentCount");
  });

  it("creates with only the parts that are set and answers with the class id", async () => {
    post.mockResolvedValue({
      ...classDto,
      id: undefined,
      classId: "new",
      inviteCode: {
        id: "i",
        code: "ABCD2345",
        expiresAt: "2026-11-09T00:00:00Z",
      },
    });

    const created = await realCreateClass({ name: "Java K21" });

    expect(post).toHaveBeenCalledWith(CLASSES, { name: "Java K21" });
    expect(created.id).toBe("new");

    await realCreateClass({
      name: "X",
      description: "d",
      schedule: {
        days: [2],
        startTime: "08:00",
        endTime: "10:00",
        room: undefined,
      },
    });
    expect(post).toHaveBeenLastCalledWith(CLASSES, {
      name: "X",
      description: "d",
      schedule: { days: [2], startTime: "08:00", endTime: "10:00" },
    });
  });

  it("updates with the clearing values for what the form left blank", async () => {
    patch.mockResolvedValue(classDto);

    await realUpdateClass("c1", { name: "Java K21" });

    expect(patch).toHaveBeenCalledWith(`${CLASSES}/c1`, {
      name: "Java K21",
      description: "",
      schedule: {},
    });
  });

  it("deletes by id", async () => {
    del.mockResolvedValue(undefined);
    await realDeleteClass("c1");
    expect(del).toHaveBeenCalledWith(`${CLASSES}/c1`);
  });
});

describe("invite codes, roster and leaving (#23, #24, #26, #27)", () => {
  it("reads the codes with a readable expiry and creates another", async () => {
    get.mockResolvedValue({
      items: [
        { id: "i1", code: "ABCD2345", expiresAt: "2026-11-09T05:00:00Z" },
      ],
    });
    expect(await realListInviteCodes("c1")).toEqual([
      { id: "i1", code: "ABCD2345", expiresAtLabel: "Còn hạn tới 09/11/2026" },
    ]);
    expect(get).toHaveBeenCalledWith(`${CLASSES}/c1/invite-codes`);

    post.mockResolvedValue({
      id: "i2",
      code: "WXYZ6789",
      expiresAt: "2026-11-09T05:00:00Z",
    });
    expect((await realCreateInviteCode("c1")).code).toBe("WXYZ6789");
    expect(post).toHaveBeenCalledWith(`${CLASSES}/c1/invite-codes`);
  });

  it("maps the roster to contact details and no F4 figures", async () => {
    get.mockResolvedValue({
      items: [
        {
          studentId: "s1",
          displayName: "An",
          email: "an@x.vn",
          phone: null,
          joinedAt: "2026-11-01T03:00:00Z",
          status: null,
        },
      ],
    });

    const [row] = await realListRoster("c1");

    expect(get).toHaveBeenCalledWith(`${CLASSES}/c1/students`);
    expect(row).toEqual({
      id: "s1",
      classId: "c1",
      name: "An",
      email: "an@x.vn",
      phone: undefined,
      joinedAt: "2026-11-01T03:00:00Z",
    });
  });

  it("leaves by class and student ids", async () => {
    post.mockResolvedValue({ classId: "c1", studentId: "s1", leftAt: "x" });
    await realLeaveClass({ classId: "c1", studentId: "s1" });
    expect(post).toHaveBeenCalledWith(`${CLASSES}/c1/students/s1/leave`);
  });
});

describe("learner side (#25, #25A, #25B)", () => {
  const mine = {
    classId: "c1",
    name: "Java K21",
    description: "",
    schedule: null,
    instructorName: "Thầy Nam",
    joinedAt: "2026-11-01T03:00:00Z",
  };

  it("maps the learner's classes and leaves the assignments out", async () => {
    get.mockResolvedValue({ items: [mine] });

    const [first] = await realListMyClasses();

    expect(get).toHaveBeenCalledWith("/identity/me/classes");
    expect(first).toMatchObject({
      id: "c1",
      name: "Java K21",
      description: undefined,
      instructorName: "Thầy Nam",
      joinedAtLabel: "01/11/2026",
    });
    expect(first).not.toHaveProperty("assignments");
  });

  it("reads one class by id", async () => {
    get.mockResolvedValue(mine);
    expect((await realGetMyClass("c1")).id).toBe("c1");
    expect(get).toHaveBeenCalledWith("/identity/me/classes/c1");
  });

  it("joins by code and answers with the class", async () => {
    post.mockResolvedValue({ classId: "c1", className: "Java K21" });
    expect(await realJoinClass("ABCD2345")).toEqual({
      id: "c1",
      name: "Java K21",
    });
    expect(post).toHaveBeenCalledWith(`${CLASSES}/join`, { code: "ABCD2345" });
  });
});

describe("error codes are rethrown as they came", () => {
  it("passes every code of the cluster through", async () => {
    for (const [code, run] of [
      ["IDT-V14", () => realCreateClass({ name: "" })],
      ["IDT-V16", () => realUpdateClass("c1", { name: "x" })],
      ["IDT-414", () => realDeleteClass("c1")],
      ["IDT-414", () => realListInviteCodes("c1")],
      ["IDT-414", () => realListRoster("c1")],
      ["IDT-414", () => realLeaveClass({ classId: "c1", studentId: "s1" })],
      ["IDT-445", () => realCreateInviteCode("c1")],
      ["IDT-411", () => realGetMyClass("c1")],
      ["IDT-410", () => realJoinClass("bad")],
      ["IDT-412", () => realJoinClass("bad")],
    ] as const) {
      const failure = new ApiError(code, 422);
      get.mockRejectedValue(failure);
      post.mockRejectedValue(failure);
      patch.mockRejectedValue(failure);
      del.mockRejectedValue(failure);
      await expect(run(), code).rejects.toMatchObject({ code });
    }
  });
});
