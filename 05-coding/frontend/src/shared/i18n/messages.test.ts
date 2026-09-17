import { describe, expect, it } from "vitest";
import en from "../../../messages/en.json";
import vi from "../../../messages/vi.json";
import { defaultLocale, locales } from "./config";

/** Flattens a nested object into a list of keys like "nav.area.student". */
function flattenKeys(obj: unknown, prefix = ""): string[] {
  if (typeof obj !== "object" || obj === null) return [prefix];
  return Object.entries(obj).flatMap(([key, value]) =>
    flattenKeys(value, prefix ? `${prefix}.${key}` : key),
  );
}

describe("messages", () => {
  // The rule "every displayed string needs a key in BOTH vi.json and en.json" used to be only a
  // review convention. This test turns it into a real gate: a missing translation fails CI.
  it("vi.json and en.json declare the exact same set of keys", () => {
    const viKeys = flattenKeys(vi).sort();
    const enKeys = flattenKeys(en).sort();

    expect(enKeys.filter((k) => !viKeys.includes(k))).toEqual([]);
    expect(viKeys.filter((k) => !enKeys.includes(k))).toEqual([]);
  });

  it("declared locales match the number of message files, and vi is the default", () => {
    expect(locales).toHaveLength(2);
    expect(defaultLocale).toBe("vi");
  });
});
