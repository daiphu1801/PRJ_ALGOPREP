import { beforeEach, describe, expect, it, vi } from "vitest";

const get = vi.fn();

vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: { get: (...args: unknown[]) => get(...args) },
}));

// Import after the mock so the module talks to the fake `api`.
import { realGetActivityCalendar, levelOf } from "./real-activity-calendar";

/** 364 consecutive days ending 2026-11-05, with the given counts by offset from the end. */
function dto(counts: Record<number, number> = {}) {
  const end = Date.UTC(2026, 10, 5);
  const days = Array.from({ length: 364 }, (_, i) => {
    const offsetFromEnd = 363 - i;
    return {
      day: new Date(end - offsetFromEnd * 86_400_000)
        .toISOString()
        .slice(0, 10),
      submissionCount: counts[offsetFromEnd] ?? 0,
    };
  });
  return { from: days[0]?.day, to: days[363]?.day, days };
}

describe("realGetActivityCalendar", () => {
  beforeEach(() => {
    get.mockReset();
  });

  it("reads #55 and lays 364 days into 52 weeks of 7, oldest first", async () => {
    get.mockResolvedValue(dto({ 0: 4, 10: 1 }));

    const calendar = await realGetActivityCalendar();

    expect(get).toHaveBeenCalledWith("/identity/me/activity-calendar");
    expect(calendar.weeks).toHaveLength(52);
    expect(calendar.weeks.every((w) => w.length === 7)).toBe(true);
    expect(calendar.weeks[0]?.[0]?.day).toBe("2025-11-07");
    expect(calendar.weeks[51]?.[6]).toEqual({
      day: "2026-11-05",
      count: 4,
      level: 2,
    });
  });

  it("counts active days and labels the month axis with at most 12 months", async () => {
    get.mockResolvedValue(dto({ 0: 4, 10: 1, 200: 9 }));

    const calendar = await realGetActivityCalendar();

    expect(calendar.activeDayCount).toBe(3);
    expect(calendar.monthLabels.length).toBeLessThanOrEqual(12);
    expect(calendar.monthLabels[calendar.monthLabels.length - 1]).toBe("T11");
  });

  it("an all-zero year has no active day", async () => {
    get.mockResolvedValue(dto());

    expect((await realGetActivityCalendar()).activeDayCount).toBe(0);
  });
});

describe("levelOf", () => {
  it("bands the counts into the five shades", () => {
    expect([0, 1, 2, 3, 4, 5, 7, 8, 30].map(levelOf)).toEqual([
      0, 1, 1, 2, 2, 3, 3, 4, 4,
    ]);
  });
});
