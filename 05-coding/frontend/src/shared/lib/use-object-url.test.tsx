import { afterEach, describe, expect, it, vi } from "vitest";
import { renderHook } from "@testing-library/react";
import { useObjectUrl } from "./use-object-url";

afterEach(() => vi.restoreAllMocks());

describe("useObjectUrl", () => {
  it("creates a url for a blob and revokes it on change and unmount", () => {
    const create = vi.fn((b: Blob) => `blob:${(b as Blob & { n?: string }).n}`);
    URL.createObjectURL = create as never;
    const revoke = vi.fn();
    URL.revokeObjectURL = revoke;
    const a = Object.assign(new Blob(["a"]), { n: "a" });
    const b = Object.assign(new Blob(["b"]), { n: "b" });
    const { result, rerender, unmount } = renderHook(
      ({ blob }) => useObjectUrl(blob),
      { initialProps: { blob: a as Blob | null } },
    );
    expect(result.current).toBe("blob:a");
    rerender({ blob: b });
    expect(revoke).toHaveBeenCalledWith("blob:a");
    expect(result.current).toBe("blob:b");
    rerender({ blob: null });
    expect(result.current).toBeUndefined();
    expect(revoke).toHaveBeenCalledWith("blob:b");
    unmount();
  });
});
