import type { ReactNode } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "./api-error";
import { notImplemented, useMockMutation, useMockQuery } from "./mock-query";

const mode = vi.hoisted(() => ({ mock: true }));
vi.mock("./mock-mode", () => ({
  withMockData: async <T,>(
    mockFn: () => Promise<T> | T,
    real: () => Promise<T>,
  ) => (mode.mock ? mockFn() : real()),
}));

function wrapper({ children }: { children: ReactNode }) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

describe("notImplemented", () => {
  it("throws a 501 NOT_IMPLEMENTED naming the DD file", () => {
    expect(() => notImplemented("03-dd/api/F2_problem-bank.md")()).toThrow(
      /F2_problem-bank/,
    );
  });
});

describe("useMockQuery", () => {
  beforeEach(() => {
    mode.mock = true;
  });

  it("uses the mock in mock mode even when a real fetcher is given", async () => {
    const real = vi.fn().mockResolvedValue("real");
    const { result } = renderHook(
      () =>
        useMockQuery({ queryKey: ["a"], mock: () => "mock", spec: "dd", real }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.data).toBe("mock"));
    expect(real).not.toHaveBeenCalled();
  });

  it("calls the real fetcher in real mode", async () => {
    mode.mock = false;
    const { result } = renderHook(
      () =>
        useMockQuery({
          queryKey: ["b"],
          mock: () => "mock",
          spec: "dd",
          real: () => Promise.resolve("real"),
        }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.data).toBe("real"));
  });

  it("keeps failing with NOT_IMPLEMENTED in real mode without a real fetcher", async () => {
    mode.mock = false;
    const { result } = renderHook(
      () => useMockQuery({ queryKey: ["c"], mock: () => "mock", spec: "dd" }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.error).toBeInstanceOf(ApiError));
    expect((result.current.error as ApiError).code).toBe("NOT_IMPLEMENTED");
  });
});

describe("useMockMutation", () => {
  beforeEach(() => {
    mode.mock = true;
  });

  it("passes the variables to the real call in real mode", async () => {
    mode.mock = false;
    const real = vi.fn().mockResolvedValue("done");
    const { result } = renderHook(
      () =>
        useMockMutation<number, string>({
          mock: () => "mock",
          spec: "dd",
          real,
        }),
      { wrapper },
    );
    await expect(result.current.mutateAsync(7)).resolves.toBe("done");
    expect(real).toHaveBeenCalledWith(7);
  });

  it("uses the mock in mock mode", async () => {
    const real = vi.fn();
    const { result } = renderHook(
      () =>
        useMockMutation<number, string>({
          mock: (n) => `m${n}`,
          spec: "dd",
          real,
        }),
      { wrapper },
    );
    await expect(result.current.mutateAsync(2)).resolves.toBe("m2");
    expect(real).not.toHaveBeenCalled();
  });

  it("rejects with NOT_IMPLEMENTED in real mode without a real call", async () => {
    mode.mock = false;
    const { result } = renderHook(
      () => useMockMutation<number, string>({ mock: () => "mock", spec: "dd" }),
      { wrapper },
    );
    await expect(result.current.mutateAsync(1)).rejects.toMatchObject({
      code: "NOT_IMPLEMENTED",
    });
  });
});
