import { useState, type ReactNode } from "react";
import { act, cleanup, renderHook, waitFor } from "@testing-library/react";
import {
  keepPreviousData,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { afterEach, expect, it, vi } from "vitest";
import { tokenStore } from "@/shared/api/token-store";
import { useMyProfile } from "@/entities/user";
import { useSubmissionDetail } from "@/entities/submission";
import { QueryProvider } from "./query-provider";

const mocks = vi.hoisted(() => ({ profile: vi.fn(), submission: vi.fn() }));
vi.mock("@/shared/api/mock-mode", () => ({
  isMockMode: () => false,
  withMockData: (_mock: unknown, real: () => unknown) => real(),
}));
vi.mock("@/entities/user/api/real-profile", () => ({
  realGetMyProfile: mocks.profile,
}));
vi.mock("@/entities/submission/api/real-submissions", () => ({
  getRealSubmission: mocks.submission,
  getRealMySubmissions: vi.fn(),
}));

const jwt = (sub: string, revision = 1) =>
  `header.${btoa(JSON.stringify({ sub, revision }))}.signature`;
const wrapper = ({ children }: { children: ReactNode }) => (
  <QueryProvider>{children}</QueryProvider>
);

afterEach(() => {
  cleanup();
  tokenStore.set(null);
  vi.resetAllMocks();
});

it("fetches B's profile instead of reusing A's fresh profile, including logout/login batched together", async () => {
  tokenStore.set(jwt("A"));
  mocks.profile.mockResolvedValueOnce({
    userId: "A",
    email: "private-A@example.test",
  });
  const hook = renderHook(
    () => ({ query: useMyProfile(), client: useQueryClient() }),
    { wrapper },
  );
  await waitFor(() => expect(hook.result.current.query.data?.userId).toBe("A"));
  const oldClient = hook.result.current.client;
  let resolveB!: (data: unknown) => void;
  mocks.profile.mockImplementationOnce(
    () =>
      new Promise((resolve) => {
        resolveB = resolve;
      }),
  );
  act(() => {
    tokenStore.set(null);
    tokenStore.set(jwt("B"));
  });
  expect(hook.result.current.query.data).toBeUndefined();
  expect(hook.result.current.client).not.toBe(oldClient);
  expect(oldClient.getQueryCache().getAll()).toHaveLength(0);
  await waitFor(() => expect(mocks.profile).toHaveBeenCalledTimes(2));
  await act(async () =>
    resolveB({ userId: "B", email: "private-B@example.test" }),
  );
  await waitFor(() => expect(hook.result.current.query.data?.userId).toBe("B"));
});

it("does not reuse cached source code for the same submission URL under another account", async () => {
  tokenStore.set(jwt("A"));
  mocks.submission.mockResolvedValueOnce({ sourceCode: "private source A" });
  const hook = renderHook(() => useSubmissionDetail("same-submission-id"), {
    wrapper,
  });
  await waitFor(() =>
    expect(hook.result.current.data?.sourceCode).toBe("private source A"),
  );
  mocks.submission.mockResolvedValueOnce(null); // B's server response cannot read A's submission.
  act(() => tokenStore.set(jwt("B")));
  expect(hook.result.current.data).toBeUndefined();
  await waitFor(() => expect(hook.result.current.isSuccess).toBe(true));
  expect(hook.result.current.data).toBeNull();
  expect(mocks.submission).toHaveBeenCalledTimes(2);
});

it.each(["classes", "class-assignments", "progress", "submission"])(
  "discards pending %s queries, previous-data placeholders and screen state on a session change",
  async (family) => {
    tokenStore.set(jwt("A"));
    let finishA!: (value: string) => void;
    const fetch = vi
      .fn()
      .mockImplementationOnce(
        () =>
          new Promise<string>((resolve) => {
            finishA = resolve;
          }),
      )
      .mockResolvedValueOnce("B only");
    const hook = renderHook(
      () => {
        const query = useQuery({
          queryKey: [family, "private"],
          queryFn: fetch,
          placeholderData: keepPreviousData,
        });
        const [draft, setDraft] = useState("");
        return { query, draft, setDraft };
      },
      { wrapper },
    );
    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(1));
    act(() => hook.result.current.setDraft("private draft A"));
    act(() => tokenStore.set(jwt("B")));
    expect(hook.result.current.draft).toBe("");
    expect(hook.result.current.query.data).toBeUndefined();
    await waitFor(() => expect(hook.result.current.query.data).toBe("B only"));
    await act(async () => finishA("late private response A"));
    expect(hook.result.current.query.data).toBe("B only");
  },
);

it("keeps cache and screen state across token refresh for the same subject, but resets after logout", async () => {
  tokenStore.set(jwt("A"));
  const fetch = vi.fn().mockResolvedValue("A");
  const hook = renderHook(
    () => {
      const query = useQuery({
        queryKey: ["private"],
        queryFn: fetch,
        staleTime: 30_000,
      });
      const client = useQueryClient();
      const [draft, setDraft] = useState("");
      return { query, client, draft, setDraft };
    },
    { wrapper },
  );
  await waitFor(() => expect(hook.result.current.query.data).toBe("A"));
  const client = hook.result.current.client;
  act(() => hook.result.current.setDraft("keep while refreshing"));
  act(() => tokenStore.set(jwt("A", 2)));
  expect(hook.result.current.client).toBe(client);
  expect(hook.result.current.draft).toBe("keep while refreshing");
  expect(fetch).toHaveBeenCalledTimes(1);
  act(() => {
    tokenStore.set(null);
    tokenStore.set(jwt("A", 3));
  });
  expect(hook.result.current.client).not.toBe(client);
  expect(hook.result.current.draft).toBe("");
});
