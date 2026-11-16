import { act, renderHook } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { ReactNode } from "react";
const get = vi.fn();
const stop = vi.fn();
let socket: {
  message: (value: unknown) => void;
  connected: () => void;
  disconnected: () => void;
};
vi.mock("@/shared/api", () => ({
  api: { get: (...args: unknown[]) => get(...args) },
  isMockMode: () => false,
}));
vi.mock("@/shared/api/stomp", () => ({
  subscribeJudge: (_destination: string, callbacks: typeof socket) => {
    socket = callbacks;
    return stop;
  },
}));
import { useSubmissionProgress } from "./progress";
const snapshot = {
  submissionId: "s1",
  status: "JUDGING",
  seq: 4,
  isFinal: false,
  totalCount: 5,
  completedCount: 2,
  passedCount: 1,
  testcases: [
    { order: 1, verdict: "AC" },
    { order: 2, verdict: "WA" },
  ],
};
function setup() {
  const callbacks = {
    progress: vi.fn(),
    finished: vi.fn(),
    error: vi.fn(),
    slow: vi.fn(),
  };
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  );
  const hook = renderHook(() => useSubmissionProgress("s1", callbacks), {
    wrapper,
  });
  return { ...hook, callbacks };
}
beforeEach(() => {
  vi.useFakeTimers();
  get.mockReset().mockResolvedValue(snapshot);
  stop.mockReset();
});
afterEach(() => {
  vi.useRealTimers();
});
describe("submission snapshot recovery", () => {
  it("recovers a snapshot after subscribing again and ignores another submission", async () => {
    const { callbacks, unmount } = setup();
    await act(async () => {});
    expect(callbacks.progress).toHaveBeenCalledWith(snapshot);
    const count = get.mock.calls.length;
    await act(async () => socket.message({ submissionId: "other", seq: 20 }));
    expect(get).toHaveBeenCalledTimes(count);
    await act(async () => socket.connected());
    expect(get).toHaveBeenCalledTimes(count + 1);
    // Same sequence snapshot must not roll the display back or duplicate a testcase.
    expect(callbacks.progress).toHaveBeenCalledTimes(1);
    unmount();
    expect(stop).toHaveBeenCalled();
  });
  it("loads final detail even for a final event with a lower sequence", async () => {
    const { callbacks, unmount } = setup();
    await act(async () => {});
    const detail = {
      submissionId: "s1",
      status: "COMPILE_ERROR",
      testcaseDetail: { items: [] },
    };
    get
      .mockResolvedValueOnce({
        ...snapshot,
        seq: 3,
        isFinal: true,
        status: "COMPILE_ERROR",
      })
      .mockResolvedValueOnce(detail);
    await act(async () =>
      socket.message({ submissionId: "s1", seq: 3, isFinal: true }),
    );
    expect(callbacks.finished).toHaveBeenCalledWith(detail);
    expect(get).toHaveBeenLastCalledWith("/judge/submissions/s1");
    expect(stop).toHaveBeenCalled();
    unmount();
  });
  it("stops fallback after ten polls and reports delayed updates without inventing a verdict", async () => {
    const { callbacks, unmount } = setup();
    await act(async () => {});
    await act(async () => {
      await vi.advanceTimersByTimeAsync(33000);
    });
    expect(get).toHaveBeenCalledTimes(11); // Initial recovery plus ten fallback polls.
    expect(callbacks.slow).toHaveBeenCalledTimes(1);
    expect(callbacks.finished).not.toHaveBeenCalled();
    unmount();
  });
  it("suppresses a late response after leaving the page", async () => {
    let resolve!: (value: unknown) => void;
    get.mockReturnValue(
      new Promise((done) => {
        resolve = done;
      }),
    );
    const { callbacks, unmount } = setup();
    unmount();
    await act(async () => resolve(snapshot));
    expect(callbacks.progress).not.toHaveBeenCalled();
  });
});

// F4-14 / SR-AC-11 / PD-AC-14: the PENDING queue hint is re-read every 5 s.
const pending = {
  ...snapshot,
  status: "PENDING",
  seq: 1,
  completedCount: 0,
  passedCount: 0,
  testcases: [],
  queue: { ahead: 18, estimatedWaitSeconds: 9 },
};
describe("PENDING queue polling (SR-AC-11, PD-AC-14)", () => {
  it("applies the queue payload and re-reads every 5 s while PENDING", async () => {
    get.mockResolvedValue(pending);
    const { callbacks, unmount } = setup();
    await act(async () => {});
    expect(callbacks.progress).toHaveBeenLastCalledWith(pending);
    expect(get).toHaveBeenCalledTimes(1);
    const next = { ...pending, queue: { ahead: 3, estimatedWaitSeconds: null } };
    get.mockResolvedValue(next);
    await act(async () => {
      await vi.advanceTimersByTimeAsync(5000);
    });
    expect(get).toHaveBeenCalledTimes(2);
    // Same seq, new queue numbers: still applied for PENDING.
    expect(callbacks.progress).toHaveBeenLastCalledWith(next);
    await act(async () => {
      await vi.advanceTimersByTimeAsync(5000);
    });
    expect(get).toHaveBeenCalledTimes(3);
    unmount();
  });

  it("ignores a snapshot with an older seq", async () => {
    get.mockResolvedValue({ ...pending, seq: 5 });
    const { callbacks, unmount } = setup();
    await act(async () => {});
    get.mockResolvedValue({
      ...pending,
      seq: 2,
      queue: { ahead: 99, estimatedWaitSeconds: 1 },
    });
    await act(async () => {
      await vi.advanceTimersByTimeAsync(5000);
    });
    expect(callbacks.progress).toHaveBeenCalledTimes(1);
    unmount();
  });

  it("stops the 5 s timer when the status leaves PENDING", async () => {
    get.mockResolvedValue(pending);
    const { callbacks, unmount } = setup();
    await act(async () => {});
    get.mockResolvedValue({ ...snapshot, queue: null });
    await act(async () => {
      await vi.advanceTimersByTimeAsync(5000);
    });
    expect(callbacks.progress).toHaveBeenLastCalledWith({
      ...snapshot,
      queue: null,
    });
    const count = get.mock.calls.length;
    // Only the 3 s fallback tick remains (two ticks fit in 5 s); a 5 s PENDING read would make three.
    await act(async () => {
      await vi.advanceTimersByTimeAsync(5000);
    });
    expect(get.mock.calls.length - count).toBeLessThanOrEqual(2);
    unmount();
  });

  it("does not poll again once the submission is final", async () => {
    get.mockResolvedValue(pending);
    const { callbacks, unmount } = setup();
    await act(async () => {});
    const detail = { submissionId: "s1", status: "ACCEPTED" };
    get
      .mockResolvedValueOnce({
        ...snapshot,
        status: "ACCEPTED",
        seq: 9,
        isFinal: true,
        queue: null,
      })
      .mockResolvedValueOnce(detail);
    await act(async () => {
      await vi.advanceTimersByTimeAsync(5000);
    });
    expect(callbacks.finished).toHaveBeenCalledWith(detail);
    const count = get.mock.calls.length;
    await act(async () => {
      await vi.advanceTimersByTimeAsync(30000);
    });
    expect(get).toHaveBeenCalledTimes(count);
    unmount();
  });

  it("keeps the old line and retries next cycle when a PENDING read fails", async () => {
    get.mockResolvedValue(pending);
    const { callbacks, unmount } = setup();
    await act(async () => {});
    get.mockRejectedValueOnce(new Error("boom"));
    await act(async () => {
      await vi.advanceTimersByTimeAsync(5000);
    });
    expect(callbacks.error).not.toHaveBeenCalled();
    const next = { ...pending, queue: { ahead: 1, estimatedWaitSeconds: 2 } };
    get.mockResolvedValue(next);
    await act(async () => {
      await vi.advanceTimersByTimeAsync(5000);
    });
    expect(callbacks.progress).toHaveBeenLastCalledWith(next);
    unmount();
  });

  it("stops polling on unmount", async () => {
    get.mockResolvedValue(pending);
    const { unmount } = setup();
    await act(async () => {});
    unmount();
    const count = get.mock.calls.length;
    await act(async () => {
      await vi.advanceTimersByTimeAsync(30000);
    });
    expect(get).toHaveBeenCalledTimes(count);
  });

  it("a STATUS_CHANGED message reads a snapshot immediately, without waiting 5 s", async () => {
    get.mockResolvedValue(pending);
    const { unmount } = setup();
    await act(async () => {});
    await act(async () =>
      socket.message({ submissionId: "s1", seq: 2, isFinal: false }),
    );
    expect(get).toHaveBeenCalledTimes(2);
    unmount();
  });
});
