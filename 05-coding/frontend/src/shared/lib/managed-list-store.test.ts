import { describe, expect, it } from "vitest";
import { renderHook, act } from "@testing-library/react";
import { createManagedListStore, labelOf, slugify } from "./managed-list-store";

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
    expect(result.current.map((row) => row.label)).toEqual([
      "Alpha",
      "Beta",
      "Gamma",
    ]);

    act(() => {
      store.update("a", { label: "Alpha 2", flag: true });
    });
    expect(result.current[0]).toMatchObject({
      key: "a",
      label: "Alpha 2",
      flag: true,
    });

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

  it("moves an item and ignores the ends", () => {
    const store = createManagedListStore<Row>(seed, "row");
    const { result } = renderHook(() => store.use());
    act(() => store.move("a", -1));
    expect(result.current.map((row) => row.key)).toEqual(["a", "b"]);
    act(() => store.move("a", 1));
    expect(result.current.map((row) => row.key)).toEqual(["b", "a"]);
  });

  it("refuses remove at minItems and reports it", () => {
    const store = createManagedListStore<Row>(seed, "row", { minItems: 2 });
    expect(store.remove("a")).toBe(false);
    const free = createManagedListStore<Row>(seed, "row");
    expect(free.remove("a")).toBe(true);
  });

  it("slugKeys makes upper-case slug keys, numbering a clash", () => {
    const store = createManagedListStore<Row>(seed, "row", { slugKeys: true });
    const { result } = renderHook(() => store.use());
    act(() => void store.add("Rất khó", { flag: false }));
    act(() => void store.add("rat-kho", { flag: false }));
    expect(result.current.map((row) => row.key).slice(2)).toEqual([
      "RAT_KHO",
      "RAT_KHO_2",
    ]);
  });

  it("slugify strips Vietnamese diacritics", () => {
    expect(slugify("Đánh giá  nâng cao!")).toBe("DANH_GIA_NANG_CAO");
  });
});
