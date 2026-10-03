import { afterEach, describe, expect, it } from "vitest";
import { renderHook, act } from "@testing-library/react";
import { MAX_VISIBLE_TOASTS, toast, useToasts } from "./toast-store";

afterEach(() => act(() => toast.clear()));

describe("toast store", () => {
  it("adds a toast with the duration of its tone", () => {
    const { result } = renderHook(() => useToasts());
    act(() => {
      toast.success("Saved");
      toast.error("Failed");
    });
    expect(result.current.map((item) => [item.tone, item.message, item.durationMs])).toEqual([
      ["success", "Saved", 4000],
      ["error", "Failed", 7000],
    ]);
  });

  it("moves a repeated message to the end under a new id instead of stacking it", () => {
    const { result } = renderHook(() => useToasts());
    act(() => {
      toast.error("Same");
      toast.info("Other");
      toast.error("Same");
    });
    expect(result.current.map((item) => item.message)).toEqual(["Other", "Same"]);
  });

  it("keeps only the newest toasts when over the cap", () => {
    const { result } = renderHook(() => useToasts());
    act(() => {
      for (let index = 0; index < MAX_VISIBLE_TOASTS + 2; index++) toast.info(`m${index}`);
    });
    expect(result.current).toHaveLength(MAX_VISIBLE_TOASTS);
    expect(result.current.at(-1)?.message).toBe(`m${MAX_VISIBLE_TOASTS + 1}`);
  });

  it("dismisses one toast by id and ignores blank messages", () => {
    const { result } = renderHook(() => useToasts());
    act(() => {
      toast.info("   ");
    });
    expect(result.current).toHaveLength(0);

    let id = 0;
    act(() => {
      id = toast.warning("Careful");
    });
    act(() => toast.dismiss(id));
    expect(result.current).toHaveLength(0);
  });
});
