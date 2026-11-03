import { describe, expect, it } from "vitest";
import { formatDateVN, formatRelativeVN } from "./format-date";

describe("formatDateVN", () => {
  it("formats in UTC+7 across the day boundary", () => {
    // 20:00 UTC on the 18th is 03:00 on the 19th in Vietnam.
    const iso = "2026-10-18T20:00:00Z";
    expect(formatDateVN(iso)).toBe("19/10/2026");
    expect(formatDateVN(iso, { withTime: true })).toBe("03:00 19/10/2026");
    expect(formatDateVN(iso, { locale: "en", withTime: true })).toBe(
      "03:00 19/10/2026",
    );
  });

  it("accepts Date and epoch ms, and returns empty for invalid input", () => {
    const d = new Date("2026-01-01T00:00:00Z");
    expect(formatDateVN(d)).toBe("01/01/2026");
    expect(formatDateVN(d.getTime())).toBe("01/01/2026");
    expect(formatDateVN("nope")).toBe("");
  });
});

describe("formatRelativeVN", () => {
  const now = Date.UTC(2026, 10, 2, 12, 0, 0);

  it("reads recent moments as minutes, hours and days", () => {
    expect(formatRelativeVN(now - 30_000, { now })).toBe("bây giờ");
    expect(formatRelativeVN(now - 5 * 60_000, { now })).toBe("5 phút trước");
    expect(formatRelativeVN(now - 3 * 3_600_000, { now })).toBe("3 giờ trước");
    expect(formatRelativeVN(now - 24 * 3_600_000, { now })).toBe("Hôm qua");
    expect(formatRelativeVN(now - 3 * 86_400_000, { now })).toBe(
      "3 ngày trước",
    );
  });

  it("falls back to the date after thirty days and to English on request", () => {
    expect(formatRelativeVN(now - 40 * 86_400_000, { now })).toBe("23/09/2026");
    expect(formatRelativeVN(now - 5 * 60_000, { now, locale: "en" })).toBe(
      "5 minutes ago",
    );
  });

  it("treats the future as now and rejects invalid input", () => {
    expect(formatRelativeVN(now + 60_000, { now })).toBe("bây giờ");
    expect(formatRelativeVN("nope", { now })).toBe("");
  });
});
