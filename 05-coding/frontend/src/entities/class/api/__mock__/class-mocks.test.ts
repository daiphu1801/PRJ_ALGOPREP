import { describe, expect, it } from "vitest";
import {
  getClassStudent,
  listClassStudents,
  listInstructorClasses,
  removeStudent,
} from "./class-mocks";

// F1-26 / DEC-2026-1014-keep-learning-history-on-leave: removing a learner only marks the learner as
// having left; the learner leaves the active lists and the class totals, nothing is deleted.
describe("class mock: removing a student keeps the history and only marks the learner as left", () => {
  it("drops the learner from the active lists and lowers the class total", async () => {
    const [target] = await listClassStudents();
    // The list hands out the live objects, so read the numbers before the removal.
    const { studentCount: totalBefore } = (await listInstructorClasses()).find(
      (c) => c.id === target!.classId,
    )!;

    await removeStudent(target!.id);

    expect(
      (await listClassStudents(target!.classId)).some(
        (s) => s.id === target!.id,
      ),
    ).toBe(false);
    expect(await getClassStudent(target!.id, target!.classId)).toBeNull();
    const after = (await listInstructorClasses()).find(
      (c) => c.id === target!.classId,
    )!;
    expect(after.studentCount).toBe(totalBefore - 1);
  });

  it("also lowers the absent count when the removed learner was absent, and is idempotent", async () => {
    const absent = (await listClassStudents()).find(
      (s) => s.status === "absent",
    )!;
    const { studentCount: totalBefore, absentCount: absentBefore } = (
      await listInstructorClasses()
    ).find((c) => c.id === absent.classId)!;

    await removeStudent(absent.id);
    await removeStudent(absent.id);

    const after = (await listInstructorClasses()).find(
      (c) => c.id === absent.classId,
    )!;
    expect(after.absentCount).toBe(absentBefore - 1);
    expect(after.studentCount).toBe(totalBefore - 1);
  });
});
