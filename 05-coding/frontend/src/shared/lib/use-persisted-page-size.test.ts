import { afterEach, describe, expect, it } from "vitest";
import { act, renderHook } from "@testing-library/react";
import { usePersistedPageSize } from "./use-persisted-page-size";

const OPTIONS = [8, 20, 50] as const;

describe("usePersistedPageSize", () => {
  afterEach(() => localStorage.clear());

  it("starts at the fallback, applies a choice and remembers it", () => {
    const { result } = renderHook(() =>
      usePersistedPageSize("test-size", OPTIONS, 8),
    );
    expect(result.current[0]).toBe(8);
    act(() => result.current[1](20));
    expect(result.current[0]).toBe(20);
    expect(localStorage.getItem("test-size")).toBe("20");
  });

  it("ignores a stored value that is not one of the options", () => {
    localStorage.setItem("test-bad", "13");
    const { result } = renderHook(() =>
      usePersistedPageSize("test-bad", OPTIONS, 8),
    );
    expect(result.current[0]).toBe(8);
  });
});
