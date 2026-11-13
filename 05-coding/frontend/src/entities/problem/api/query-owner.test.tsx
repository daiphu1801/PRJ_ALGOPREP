import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { beforeEach, expect, it, vi } from "vitest";
import type { ReactNode } from "react";
import type * as RealAuthoring from "./real-authoring";

const owner = vi.hoisted(() => ({ value: "author-A" }));
vi.mock("@/shared/api/user-scope", () => ({
  currentUserScope: () => owner.value,
}));
vi.mock("@/shared/api/mock-mode", () => ({
  withMockData: (_mock: unknown, real: () => unknown) => real(),
}));
vi.mock("./real-authoring", async (importOriginal) => ({
  ...(await importOriginal<typeof RealAuthoring>()),
  realLoadDraft: vi.fn(),
}));

import { useProblemDraft } from "./queries";
import { realLoadDraft } from "./real-authoring";

beforeEach(() => {
  owner.value = "author-A";
  vi.clearAllMocks();
});
function wrapper({ children }: { children: ReactNode }) {
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}
let client: QueryClient;
beforeEach(() => {
  client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
});

it("does not paint the previous author's draft while the new account is loading the same problem", async () => {
  const a = { draft: { title: "Private A" } };
  vi.mocked(realLoadDraft).mockResolvedValueOnce(
    a as Awaited<ReturnType<typeof realLoadDraft>>,
  );
  vi.mocked(realLoadDraft).mockImplementationOnce(() => new Promise(() => {}));
  const hook = renderHook(() => useProblemDraft("same-problem"), { wrapper });
  await waitFor(() => expect(hook.result.current.data).toEqual(a));
  owner.value = "author-B";
  hook.rerender();
  expect(hook.result.current.data).toBeUndefined();
  expect(hook.result.current.isPending).toBe(true);
});
