import { describe, expect, it } from "vitest";
import { translateLabel } from "./labels";

describe("translateLabel", () => {
  const t = (key: string) => `T:${key}`;

  it("translates through the key and keeps the other fields", () => {
    expect(
      translateLabel(t, {
        label: "PYTHON",
        labelKey: "labels.language.PYTHON",
        colorVar: "--c",
      }),
    ).toEqual({
      label: "T:labels.language.PYTHON",
      labelKey: "labels.language.PYTHON",
      colorVar: "--c",
    });
  });

  it("leaves an item with no key (mock data, or an unknown code) as it is", () => {
    const item = { label: "MEMORY_LIMIT_EXCEEDED" };
    expect(translateLabel(t, item)).toBe(item);
  });
});
