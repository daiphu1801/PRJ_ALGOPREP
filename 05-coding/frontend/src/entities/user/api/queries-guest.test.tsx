import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook } from "@testing-library/react";
import type { ReactNode } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { tokenStore } from "@/shared/api";

const realProfile = vi.hoisted(() => vi.fn());
vi.mock("./real-profile", () => ({ realGetMyProfile: realProfile }));
vi.mock("@/shared/api/mock-mode", () => ({
  isMockMode: () => false,
  withMockData: <T,>(_mock: () => unknown, real: () => Promise<T>) => real(),
}));

import { useMyProfile } from "./queries";

function wrapper({ children }: { children: ReactNode }) {
  return (
    <QueryClientProvider client={new QueryClient()}>
      {children}
    </QueryClientProvider>
  );
}

// DEC-2026-1126: every shell reads the profile through this hook, and a guest has none.
describe("useMyProfile for a guest", () => {
  afterEach(() => tokenStore.set(null));

  it("does not call the api without a session", () => {
    tokenStore.set(null);
    const { result } = renderHook(() => useMyProfile("student"), { wrapper });
    expect(result.current.fetchStatus).toBe("idle");
    expect(realProfile).not.toHaveBeenCalled();
  });
});
