import { describe, expect, it } from "vitest";
import { act, renderHook } from "@testing-library/react";
import {
  addInterviewLevel,
  moveInterviewLevel,
  levelLabel,
  levelTone,
  removeInterviewLevel,
  renameInterviewLevel,
  useInterviewLevels,
  type InterviewLevel,
} from "./level-store";

const list: InterviewLevel[] = [
  { key: "EASY", label: "Dễ", tone: "success" },
  { key: "x", label: "Lạ", tone: "neutral" },
];

describe("level helpers", () => {
  it("resolves label and tone by key", () => {
    expect(levelLabel(list, "EASY")).toBe("Dễ");
    expect(levelTone(list, "EASY")).toBe("success");
  });

  it("falls back to the raw key and a neutral tone for an unknown key", () => {
    expect(levelLabel(list, "gone")).toBe("gone");
    expect(levelTone(list, "gone")).toBe("neutral");
  });
});

describe("level store", () => {
  it("rejects empty and duplicate names, and adds a new level as a slug code with a neutral tone", () => {
    const levels = renderHook(() => useInterviewLevels());
    expect(addInterviewLevel("  ")).toBe("empty");
    expect(addInterviewLevel("dễ")).toBe("duplicate");
    act(() => {
      expect(addInterviewLevel("Rất khó")).toBeNull();
    });
    expect(levels.result.current.at(-1)).toMatchObject({ key: "RAT_KHO", label: "Rất khó", tone: "neutral" });
    act(() => {
      expect(addInterviewLevel("RẤT-KHÓ!")).toBeNull();
    });
    expect(levels.result.current.at(-1)?.key).toBe("RAT_KHO_2");
  });

  it("rename keeps the key and rejects a clash", () => {
    expect(renameInterviewLevel("EASY", "Khó")).toBe("duplicate");
    expect(renameInterviewLevel("EASY", "Cơ bản")).toBeNull();
    expect(renameInterviewLevel("EASY", "Dễ")).toBeNull();
  });

  it("moves a level up and down, ignoring the ends", () => {
    const levels = renderHook(() => useInterviewLevels());
    const keys = () => levels.result.current.map((item) => item.key);
    const [first, second] = keys();
    act(() => moveInterviewLevel(first!, -1));
    expect(keys()[0]).toBe(first);
    act(() => moveInterviewLevel(first!, 1));
    expect(keys().slice(0, 2)).toEqual([second, first]);
  });

  it("refuses to delete the last remaining level", () => {
    const levels = renderHook(() => useInterviewLevels());
    for (const item of [...levels.result.current]) act(() => void removeInterviewLevel(item.key));
    expect(levels.result.current).toHaveLength(1);
    expect(removeInterviewLevel(levels.result.current[0]!.key)).toBe(false);
    expect(levels.result.current).toHaveLength(1);
  });
});
