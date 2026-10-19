import { describe, expect, it } from "vitest";
import { formatDateVN } from "./format-date";

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
