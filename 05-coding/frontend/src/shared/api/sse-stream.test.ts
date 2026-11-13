import { afterEach, describe, expect, it, vi } from "vitest";
import type { ApiError } from "./api-error";
import { tokenStore } from "./token-store";

const refresh = vi.fn();
vi.mock("./client", () => ({ refreshAccessToken: () => refresh() }));

import { createSseParser, openSseStream, type SseMessage } from "./sse-stream";

const encoder = new TextEncoder();

function streamOf(...chunks: string[]) {
  return new Response(
    new ReadableStream({
      start(controller) {
        for (const chunk of chunks) controller.enqueue(encoder.encode(chunk));
        controller.close();
      },
    }),
    { status: 200, headers: { "Content-Type": "text/event-stream" } },
  );
}

afterEach(() => {
  vi.unstubAllGlobals();
  vi.useRealTimers();
  tokenStore.set(null);
  refresh.mockReset();
});

describe("createSseParser", () => {
  it("joins an event split over chunks, ignores comments and reads id, event and data", () => {
    const got: SseMessage[] = [];
    const feed = createSseParser((m) => got.push(m));
    feed(":heartbeat\n\nid: 7\nevent: audit-log\nda");
    expect(got).toEqual([]);
    feed('ta: {"a":1}\ndata: more\r\n\r\nevent: resync\n\n');
    expect(got).toEqual([
      { event: "audit-log", data: '{"a":1}\nmore', id: "7" },
      { event: "resync", data: "", id: undefined },
    ]);
  });
});

describe("openSseStream", () => {
  it("sends the Bearer token, dispatches events and reconnects with Last-Event-ID", async () => {
    vi.useFakeTimers();
    tokenStore.set("tok");
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(streamOf("id: u1\nevent: audit-log\ndata: {}\n\n"))
      .mockImplementation(() => new Promise(() => {}));
    vi.stubGlobal("fetch", fetchMock);
    const messages: SseMessage[] = [];
    const statuses: string[] = [];

    const close = openSseStream("/identity/admin/audit-logs/stream", {
      onMessage: (m) => messages.push(m),
      onStatus: (s) => statuses.push(s),
    });
    await vi.advanceTimersByTimeAsync(1_500);

    expect(messages).toEqual([{ event: "audit-log", data: "{}", id: "u1" }]);
    expect(statuses).toEqual(["open", "reconnecting"]);
    const first = fetchMock.mock.calls[0]!;
    expect(first[0]).toBe(
      "http://localhost:8080/api/v1/identity/admin/audit-logs/stream",
    );
    expect(first[1].headers).toMatchObject({ Authorization: "Bearer tok" });
    expect(first[1].headers["Last-Event-ID"]).toBeUndefined();
    expect(fetchMock.mock.calls[1]![1].headers["Last-Event-ID"]).toBe("u1");
    close();
  });

  it("refreshes the token once on a 401 and retries", async () => {
    refresh.mockResolvedValue("renewed");
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(streamOf("event: resync\n\n"));
    vi.stubGlobal("fetch", fetchMock);
    const messages: SseMessage[] = [];

    const close = openSseStream("/s", { onMessage: (m) => messages.push(m) });
    await vi.waitFor(() => expect(messages).toHaveLength(1));

    expect(refresh).toHaveBeenCalledTimes(1);
    expect(fetchMock.mock.calls[1]![1].headers.Authorization).toBe(
      "Bearer renewed",
    );
    close();
  });

  it("reports a refusal as an ApiError and does not retry", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ errorCode: "IDT-449" }), {
        status: 429,
      }),
    );
    vi.stubGlobal("fetch", fetchMock);
    const refused: ApiError[] = [];

    openSseStream("/s", {
      onMessage: () => {},
      onRefused: (e) => refused.push(e),
    });
    await vi.waitFor(() => expect(refused).toHaveLength(1));

    expect(refused[0]).toMatchObject({ code: "IDT-449", status: 429 });
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("stops asking once it is closed", async () => {
    vi.useFakeTimers();
    const fetchMock = vi.fn().mockImplementation(() => streamOf(""));
    vi.stubGlobal("fetch", fetchMock);

    const close = openSseStream("/s", { onMessage: () => {} });
    await vi.advanceTimersByTimeAsync(100);
    close();
    const calls = fetchMock.mock.calls.length;
    await vi.advanceTimersByTimeAsync(120_000);

    expect(fetchMock.mock.calls.length).toBe(calls);
  });
});
