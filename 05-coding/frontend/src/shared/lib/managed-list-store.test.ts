import { describe, expect, it } from "vitest";
import { renderHook, act } from "@testing-library/react";
import { createManagedListStore, labelOf } from "./managed-list-store";

type Row = { key: string; label: string; flag: boolean };
const seed: Row[] = [
  { key: "a", label: "Alpha", flag: false },
  { key: "b", label: "Beta", flag: true },
];

describe("createManagedListStore", () => {
  it("adds, renames and removes, and every reader sees the change", () => {
    const store = createManagedListStore<Row>(seed, "row");
    const { result } = renderHook(() => store.use());
    expect(result.current).toHaveLength(2);

    act(() => {
      expect(store.add("  Gamma ", { flag: false })).toBeNull();
    });
    expect(result.current.map((row) => row.label)).toEqual(["Alpha", "Beta", "Gamma"]);

    act(() => {
      store.update("a", { label: "Alpha 2", flag: true });
    });
    expect(result.current[0]).toMatchObject({ key: "a", label: "Alpha 2", flag: true });

    act(() => store.remove("b"));
    expect(result.current.map((row) => row.key)).not.toContain("b");
  });

  it("rejects an empty name and a duplicate, ignoring case and spacing", () => {
    const store = createManagedListStore<Row>(seed, "row");
    expect(store.add("   ", { flag: false })).toBe("empty");
    expect(store.add("alpha ", { flag: false })).toBe("duplicate");
    expect(store.update("a", { label: "BETA" })).toBe("duplicate");
    expect(store.update("a", { label: "Alpha" })).toBeNull();
  });

  it("falls back to the key when the item is gone", () => {
    expect(labelOf(seed, "a")).toBe("Alpha");
    expect(labelOf(seed, "zzz")).toBe("zzz");
  });
});
