import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { withTestProviders } from "@/shared/test/render-with-providers";
import { mockUrl } from "@/shared/test/mock-url";
import { apiMock, resetApiMock } from "@/shared/test/real-api";

vi.mock("@/shared/api", async (original) =>
  (await import("@/shared/test/real-api")).realApiModule(
    await original<Record<string, unknown>>(),
  ),
);
vi.mock("@/shared/api/mock-mode", () => ({
  isMockMode: () => false,
  withMockData: <T,>(_mock: unknown, real: () => Promise<T>) => real(),
}));
vi.mock("@/entities/problem", () => ({
  useProblemLevels: () => [],
  problemLevelLabel: () => "",
  problemLevelTone: () => "neutral",
}));
const pushed = vi.hoisted(() => vi.fn());
vi.mock("next/navigation", async () => {
  const { navigationMock, mockUrl } = await import("@/shared/test/mock-url");
  return navigationMock({
    push: (url: string, options: unknown) => {
      pushed(url, options);
      mockUrl.reset(url);
    },
  });
});
import { MySubmissionsView } from "./my-submissions-view";
import { readSubmissionFilters } from "../model/submission-url";

beforeEach(() => {
  resetApiMock();
  pushed.mockClear();
  mockUrl.reset("/submissions");
  apiMock.get.mockImplementation(
    async (path: string, params?: Record<string, unknown>) => {
      if (path !== "/judge/me/submissions")
        throw new Error(`unexpected ${path}`);
      return {
        items: [],
        page: params?.page ?? 1,
        pageSize: 20,
        totalCount: 60,
      };
    },
  );
});
afterEach(() => vi.useRealTimers());
const params = () => apiMock.get.mock.calls.at(-1)?.[1];
async function open() {
  render(withTestProviders(<MySubmissionsView />));
  await waitFor(() => expect(apiMock.get).toHaveBeenCalled());
}

describe("USR0202 URL real API boundary", () => {
  it("hydrates a direct URL into the form and the first request", async () => {
    mockUrl.reset("/submissions?q=Sum&verdict=ACCEPTED&language=PYTHON&page=2");
    await open();
    expect(params()).toMatchObject({
      q: "Sum",
      verdict: "ACCEPTED",
      language: "PYTHON",
      page: 2,
    });
    expect(screen.getByRole("textbox", { name: /Tìm/ })).toHaveValue("Sum");
  });
  it("waits400ms before committing search and requesting its first page", async () => {
    await open();
    await act(async () => {});
    vi.useFakeTimers();
    const initial = apiMock.get.mock.calls.length;
    fireEvent.change(screen.getByRole("textbox", { name: /Tìm/ }), {
      target: { value: "Sum" },
    });
    await act(async () => {
      vi.advanceTimersByTime(399);
    });
    expect(apiMock.get.mock.calls.length).toBe(initial);
    await act(async () => {
      vi.advanceTimersByTime(1);
    });
    expect(mockUrl.search.get("q")).toBe("Sum");
    expect(params()).toMatchObject({ q: "Sum", page: 1 });
  });
  it("restores Back/Forward URLs and cancels a pending search from the departed URL", async () => {
    await open();
    await act(async () => {});
    vi.useFakeTimers();
    fireEvent.change(screen.getByRole("textbox", { name: /Tìm/ }), {
      target: { value: "typing" },
    });
    await act(async () => {
      mockUrl.reset("/submissions?q=Returned&language=JAVA&page=3");
    });
    expect(screen.getByRole("textbox", { name: /Tìm/ })).toHaveValue(
      "Returned",
    );
    await act(async () => {
      vi.advanceTimersByTime(500);
    });
    expect(params()).toMatchObject({
      q: "Returned",
      language: "JAVA",
      page: 3,
    });
    expect(mockUrl.search.get("q")).toBe("Returned");
    await act(async () => {
      mockUrl.reset("/submissions?q=Forward&verdict=WRONG_ANSWER&page=2");
    });
    expect(screen.getByRole("textbox", { name: /Tìm/ })).toHaveValue("Forward");
    expect(params()).toMatchObject({
      q: "Forward",
      verdict: "WRONG_ANSWER",
      page: 2,
    });
  });
  it("writes a filter selection into navigation history and resets page while keeping unrelated params", async () => {
    mockUrl.reset("/submissions?q=Sum&page=3&from=overview");
    await open();
    fireEvent.click(screen.getByRole("button", { name: /^Lọc theo kết quả/ }));
    fireEvent.click(
      within(screen.getByRole("listbox")).getByRole("option", {
        name: "Lỗi biên dịch",
      }),
    );
    await waitFor(() =>
      expect(params()).toMatchObject({
        q: "Sum",
        verdict: "COMPILE_ERROR",
        page: 1,
      }),
    );
    expect(mockUrl.search.get("page")).toBeNull();
    expect(mockUrl.search.get("from")).toBe("overview");
    expect(pushed).toHaveBeenCalled();
  });
  it("normalizes unknown filters and malformed page before issuing a request", async () => {
    mockUrl.reset("/submissions?verdict=DELETE&language=SQL&page=NaN");
    await open();
    expect(params()).toMatchObject({
      q: "",
      verdict: "ALL",
      language: "ALL",
      page: 1,
    });
    expect(
      readSubmissionFilters(
        new URLSearchParams("page=9007199254740992&q=" + "x".repeat(101)),
      ),
    ).toMatchObject({ page: 1, query: "x".repeat(100) });
  });
  it("Enter flushes pending search and announces the new result rather than the previous count", async () => {
    apiMock.get.mockImplementation(
      async (_path: string, values?: Record<string, unknown>) => ({
        items: [],
        page: 1,
        pageSize: 20,
        totalCount: values?.q === "missing" ? 0 : 60,
      }),
    );
    const { toast, useToasts } = await import("@/shared/lib/toast-store");
    const { renderHook } = await import("@testing-library/react");
    const observed = renderHook(() => useToasts());
    act(() => toast.clear());
    await open();
    const input = screen.getByRole("textbox", { name: /Tìm/ });
    fireEvent.change(input, { target: { value: "missing" } });
    fireEvent.keyDown(input, { key: "Enter" });
    await waitFor(() =>
      expect(params()).toMatchObject({ q: "missing", page: 1 }),
    );
    await waitFor(() =>
      expect(observed.result.current.at(-1)?.message).toContain("Không"),
    );
    expect(mockUrl.search.get("q")).toBe("missing");
  });
});
