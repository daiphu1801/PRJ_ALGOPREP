import { describe, expect, it } from "vitest";
import {
  classScheduleParts,
  formatClassSchedule,
  formatClassScheduleBrief,
} from "./schedule";

const dayLabel = (day: number) => `T${day + 1}`;
const schedule = {
  days: [3, 1],
  startTime: "19:00",
  endTime: "20:30",
  room: "Lab A302",
  startDate: "2026-11-01",
  endDate: "2026-11-29",
  note: "Mang laptop",
};

describe("class schedule text", () => {
  it("brief form keeps days, time and room only", () => {
    expect(formatClassScheduleBrief(schedule, dayLabel)).toBe(
      "T2 · T4 19:00–20:30 · Lab A302",
    );
  });

  it("brief form is empty without a schedule and skips missing parts", () => {
    expect(formatClassScheduleBrief(undefined, dayLabel)).toBe("");
    expect(
      formatClassScheduleBrief({ days: [1], startTime: "08:00" }, dayLabel),
    ).toBe("T2 08:00");
  });

  it("full form still carries the period and the note", () => {
    expect(formatClassSchedule(schedule, dayLabel)).toContain("01/11–29/11");
    expect(formatClassSchedule(schedule, dayLabel)).toContain("Mang laptop");
  });

  it("parts keep days and time, room, period and note apart", () => {
    expect(classScheduleParts(schedule, dayLabel)).toEqual({
      when: "T2 · T4 19:00–20:30",
      room: "Lab A302",
      period: "01/11–29/11",
      note: "Mang laptop",
    });
    expect(classScheduleParts(undefined, dayLabel).when).toBe("");
  });
});
