import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { expect, it, vi } from "vitest";
import type { ReactNode } from "react";
import type * as RealManagement from "./real-problem-management";
const owner = vi.hoisted(() => ({ value: "author-A" }));
vi.mock("@/shared/api/user-scope", () => ({
  currentUserScope: () => owner.value,
}));
vi.mock("@/shared/api/mock-mode", () => ({
  withMockData: (_mock: unknown, real: () => unknown) => real(),
}));
vi.mock("@/entities/problem", () => ({ useProblemLevels: () => [] }));
vi.mock("./real-problem-management", async (original) => ({
  ...(await original<typeof RealManagement>()),
  realListAdminProblems: vi.fn(),
}));
import { useAdminProblemList } from "./queries";
import { realListAdminProblems } from "./real-problem-management";
import type { AdminListParams } from "../model/types";

it("does not retain an instructor's list as placeholder data for another account", async () => {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  );
  const a = { items: [{ title: "Private A" }] };
  vi.mocked(realListAdminProblems).mockResolvedValueOnce(
    a as Awaited<ReturnType<typeof realListAdminProblems>>,
  );
  vi.mocked(realListAdminProblems).mockImplementationOnce(
    () => new Promise(() => {}),
  );
  const hook = renderHook(
    () => useAdminProblemList({ page: 1 } as AdminListParams),
    { wrapper },
  );
  await waitFor(() => expect(hook.result.current.data).toEqual(a));
  owner.value = "author-B";
  hook.rerender();
  expect(hook.result.current.data).toBeUndefined();
  expect(hook.result.current.isPlaceholderData).toBe(false);
});
