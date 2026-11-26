import { act, renderHook } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { tokenStore } from "@/shared/api";

vi.mock("@/shared/api/mock-mode", () => ({
  isMockMode: () => false,
  withMockData: <T,>(_mock: () => unknown, real: () => Promise<T>) => real(),
}));

import { useSession } from "./use-session";

describe("useSession", () => {
  afterEach(() => tokenStore.set(null));

  it("is guest without a token and follows sign in and sign out", () => {
    const { result } = renderHook(() => useSession());
    expect(result.current).toBe("guest");

    act(() => tokenStore.set("a.eyJzdWIiOiJ1MSJ9.c"));
    expect(result.current).toBe("signedIn");

    act(() => tokenStore.set(null));
    expect(result.current).toBe("guest");
  });
});
