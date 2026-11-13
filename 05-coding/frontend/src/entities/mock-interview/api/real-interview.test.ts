import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError, tokenStore } from "@/shared/api";
import {
  TurnStreamInterrupted,
  realCompleteSession,
  realGetSession,
  realRequestHint,
  realStartSession,
  streamTurn,
  type TurnEvent,
} from "./real-interview";

const get = vi.hoisted(() => vi.fn());
const post = vi.hoisted(() => vi.fn());
const refresh = vi.hoisted(() => vi.fn());

vi.mock("@/shared/api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  api: { get, post },
}));
vi.mock("@/shared/api/client", () => ({ refreshAccessToken: refresh }));

const config = {
  entryType: "custom" as const,
  submissionId: null,
  questionId: null,
  customTopics: ["database"],
  level: "middle" as const,
  maxTurns: 8 as const,
  hintAllowed: true,
};

const sessionDto = {
  sessionId: "s-1",
  entryType: "TOPIC_SELECTION",
  stage: "EXPLAIN",
  turnCount: 0,
  maxTurns: 8,
  hintAllowed: true,
  interviewerLevel: "MIDDLE",
  topics: ["Database"],
  submissionId: null,
  questionRef: null,
  startedAt: "2026-11-12T03:00:00Z",
};
const opening = {
  turnIndex: 0,
  role: "AI",
  stage: "EXPLAIN",
  content: "Hello",
  createdAt: "2026-11-12T03:00:00Z",
};

beforeEach(() => {
  get.mockReset();
  post.mockReset();
  refresh.mockReset();
  tokenStore.set("tok");
});

describe("session calls", () => {
  it("starts a topic session with the exact body and maps the opening", async () => {
    post.mockResolvedValue({ ...sessionDto, openingMessage: opening });
    const out = await realStartSession({
      config,
      questionText: null,
      topicNames: ["Database"],
    });
    expect(post).toHaveBeenCalledWith("/ai/interview-sessions", {
      entryType: "TOPIC_SELECTION",
      topics: ["Database"],
      interviewerLevel: "MIDDLE",
      maxTurns: 8,
      hintAllowed: true,
    });
    expect(out.session).toMatchObject({
      sessionId: "s-1",
      stage: "explain",
      maxTurns: 8,
      level: "middle",
    });
    expect(out.opening).toEqual({
      turnIndex: 0,
      role: "ai",
      stage: "explain",
      content: "Hello",
    });
  });

  it("starts a bank session with ref and text, and a submission with the id only", async () => {
    post.mockResolvedValue({ ...sessionDto, openingMessage: opening });
    await realStartSession({
      config: { ...config, entryType: "bank", questionId: "q-1" },
      questionText: "Design an LRU cache",
      topicNames: [],
    });
    expect(post).toHaveBeenLastCalledWith("/ai/interview-sessions", {
      entryType: "QUESTION_BANK",
      questionRef: "q-1",
      questionText: "Design an LRU cache",
      interviewerLevel: "MIDDLE",
      maxTurns: 8,
      hintAllowed: true,
    });
    await realStartSession({
      config: { ...config, entryType: "submission", submissionId: "sub-1" },
      questionText: null,
      topicNames: [],
    });
    expect(post).toHaveBeenLastCalledWith("/ai/interview-sessions", {
      entryType: "SUBMISSION",
      submissionId: "sub-1",
    });
  });

  it("keeps a null maxTurns (SUBMISSION entry) and a null hint flag", async () => {
    get.mockResolvedValue({
      session: { ...sessionDto, maxTurns: null, hintAllowed: null },
      replyState: "GENERATING",
      turns: [opening],
      result: null,
    });
    const detail = await realGetSession("s-1");
    expect(get).toHaveBeenCalledWith("/ai/interview-sessions/s-1");
    expect(detail.session.maxTurns).toBeNull();
    expect(detail.session.hintAllowed).toBe(false);
    expect(detail.replyState).toBe("GENERATING");
  });

  it("maps a completed session result with the server weights", async () => {
    const result = {
      sessionId: "s-1",
      overallScore: 7.5,
      criteria: [
        { code: "CLARITY", score: 8, weightPercent: 25, comment: "a" },
        {
          code: "TECHNICAL_ACCURACY",
          score: 7,
          weightPercent: 30,
          comment: "b",
        },
        {
          code: "PUSHBACK_HANDLING",
          score: 7,
          weightPercent: 25,
          comment: "c",
        },
        {
          code: "COMPLEXITY_AWARENESS",
          score: 8,
          weightPercent: 20,
          comment: "d",
        },
      ],
      feedbackSummary: "ok",
      turnCount: 3,
      endedAt: "2026-11-12T03:10:00Z",
    };
    post.mockResolvedValue(result);
    const done = await realCompleteSession("s-1");
    expect(post).toHaveBeenCalledWith("/ai/interview-sessions/s-1/complete");
    expect(done.criteria.map((c) => [c.code, c.weightPercent])).toEqual([
      ["clarity", 25],
      ["technicalAccuracy", 30],
      ["pushbackHandling", 25],
      ["complexityAwareness", 20],
    ]);
    expect(done.overallScore).toBe(7.5);
  });

  it("requests a hint without a body", async () => {
    post.mockResolvedValue({ hint: "think about hashing", stage: "EXPLAIN" });
    expect(await realRequestHint("s-1")).toBe("think about hashing");
    expect(post).toHaveBeenCalledWith("/ai/interview-sessions/s-1/hint");
  });

  it.each([
    "AIR-401",
    "AIR-403",
    "AIR-404",
    "AIR-501",
    "AIR-502",
    "AIR-111",
    "AIR-113",
    "AIR-115",
    "AIR-116",
    "AIR-117",
  ])("rethrows %s as ApiError without touching it", async (c) => {
    post.mockRejectedValue(new ApiError(c, 429));
    await expect(realCompleteSession("s-1")).rejects.toMatchObject({ code: c });
  });
});

const enc = new TextEncoder();
function sse(chunks: string[], status = 200) {
  const body = new ReadableStream<Uint8Array>({
    start(controller) {
      for (const chunk of chunks) controller.enqueue(enc.encode(chunk));
      controller.close();
    },
  });
  return new Response(body, {
    status,
    headers: { "Content-Type": "text/event-stream" },
  });
}
const frame = (event: string, data: object, id = 1) =>
  `id: ${id}\nevent: ${event}\ndata: ${JSON.stringify({ seq: id, ...data })}\n\n`;

const accepted = frame("turn.accepted", {
  sessionId: "s-1",
  userTurnIndex: 1,
  aiTurnIndex: 2,
  stage: "EXPLAIN",
  turnCount: 0,
  maxTurns: 8,
});
const completed = (seq: number, shouldComplete = false) =>
  frame(
    "turn.completed",
    {
      aiTurnIndex: 2,
      content: "Hi there",
      stage: "CHALLENGE",
      stageChanged: true,
      turnCount: 1,
      maxTurns: 8,
      shouldComplete,
    },
    seq,
  );

async function run(
  response: Response | Promise<Response>,
  signal = new AbortController().signal,
) {
  const fetchMock = vi.fn().mockResolvedValue(await response);
  vi.stubGlobal("fetch", fetchMock);
  const events: TurnEvent[] = [];
  const outcome = await streamTurn({
    sessionId: "s-1",
    content: "my answer",
    expectedTurnCount: 0,
    signal,
    onEvent: (e) => events.push(e),
  }).then(
    () => null,
    (error: unknown) => error,
  );
  return { events, outcome, fetchMock };
}

describe("streamTurn", () => {
  it("sends the Bearer header and only content + expectedTurnCount", async () => {
    const { fetchMock } = await run(sse([accepted, completed(2)]));
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toMatch(/\/ai\/interview-sessions\/s-1\/turns$/);
    expect(init.method).toBe("POST");
    expect((init.headers as Record<string, string>).Authorization).toBe(
      "Bearer tok",
    );
    expect(JSON.parse(init.body as string)).toEqual({
      content: "my answer",
      expectedTurnCount: 0,
    });
  });

  it("parses events split across chunks and ignores keep-alive comments", async () => {
    const t1 = frame("token", { text: "Hi " }, 2);
    const t2 = frame("token", { text: "there" }, 3);
    const all = accepted + ": keep-alive\n\n" + t1 + t2 + completed(4, true);
    const cut = Math.floor(all.length / 3);
    const { events, outcome } = await run(
      sse([all.slice(0, cut), all.slice(cut, cut * 2), all.slice(cut * 2)]),
    );
    expect(outcome).toBeNull();
    expect(events.map((e) => e.type)).toEqual([
      "accepted",
      "token",
      "token",
      "completed",
    ]);
    expect(events[1]).toEqual({ type: "token", text: "Hi " });
    expect(events[3]).toMatchObject({
      type: "completed",
      content: "Hi there",
      stage: "challenge",
      stageChanged: true,
      turnCount: 1,
      shouldComplete: true,
    });
  });

  it("delivers a token as soon as its chunk arrives, not at the end", async () => {
    let release: () => void = () => undefined;
    const gate = new Promise<void>((resolve) => (release = resolve));
    const body = new ReadableStream<Uint8Array>({
      async start(controller) {
        controller.enqueue(
          enc.encode(accepted + frame("token", { text: "A" }, 2)),
        );
        await gate;
        controller.enqueue(enc.encode(completed(3)));
        controller.close();
      },
    });
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(body)));
    const seen: string[] = [];
    const pending = streamTurn({
      sessionId: "s-1",
      content: "x",
      expectedTurnCount: 0,
      signal: new AbortController().signal,
      onEvent: (e) => seen.push(e.type),
    });
    await vi.waitFor(() => expect(seen).toEqual(["accepted", "token"]));
    release();
    await pending;
    expect(seen).toEqual(["accepted", "token", "completed"]);
  });

  it.each([
    ["AIR-112", 409],
    ["AIR-113", 409],
    ["AIR-114", 409],
    ["AIR-111", 409],
    ["AIR-110", 404],
    ["AIR-302", 403],
    ["AIR-501", 503],
    ["AIR-V08", 400],
    ["AIR-V09", 400],
  ])(
    "turns the JSON error %s before the stream into an ApiError",
    async (c, status) => {
      const { outcome, events } = await run(
        new Response(JSON.stringify({ errorCode: c, status }), {
          status,
          headers: { "Content-Type": "application/problem+json" },
        }),
      );
      expect(events).toEqual([]);
      expect(outcome).toBeInstanceOf(ApiError);
      expect(outcome).toMatchObject({ code: c, status });
    },
  );

  it.each(["AIR-501", "AIR-502"])(
    "reports the SSE error event %s after accepted",
    async (c) => {
      const { events, outcome } = await run(
        sse([
          accepted,
          frame("error", { error: { status: 503, errorCode: c } }, 2),
        ]),
      );
      expect(outcome).toBeNull();
      expect(events[1]).toMatchObject({ type: "error", error: { code: c } });
    },
  );

  it("flags a stream that ends without a final event as interrupted", async () => {
    const afterAccept = await run(sse([accepted]));
    expect(afterAccept.outcome).toBeInstanceOf(TurnStreamInterrupted);
    expect(afterAccept.outcome).toMatchObject({ accepted: true });
    const before = await run(sse([]));
    expect(before.outcome).toMatchObject({ accepted: false });
  });

  it("treats a seq gap as a broken stream", async () => {
    const { outcome, events } = await run(
      sse([accepted, frame("token", { text: "x" }, 5), completed(6)]),
    );
    expect(outcome).toBeInstanceOf(TurnStreamInterrupted);
    expect(events.map((e) => e.type)).toEqual(["accepted"]);
  });

  it("refreshes the token once on 401 and retries", async () => {
    refresh.mockResolvedValue("fresh");
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(new Response("{}", { status: 401 }))
      .mockResolvedValueOnce(sse([accepted, completed(2)]));
    vi.stubGlobal("fetch", fetchMock);
    await streamTurn({
      sessionId: "s-1",
      content: "x",
      expectedTurnCount: 0,
      signal: new AbortController().signal,
      onEvent: () => undefined,
    });
    expect(refresh).toHaveBeenCalledTimes(1);
    const retry = fetchMock.mock.calls[1]?.[1] as RequestInit;
    expect((retry.headers as Record<string, string>).Authorization).toBe(
      "Bearer fresh",
    );
  });

  it("resolves silently when the caller aborts", async () => {
    const controller = new AbortController();
    const fetchMock = vi.fn().mockImplementation(() => {
      controller.abort();
      return Promise.reject(new DOMException("aborted", "AbortError"));
    });
    vi.stubGlobal("fetch", fetchMock);
    await expect(
      streamTurn({
        sessionId: "s-1",
        content: "x",
        expectedTurnCount: 0,
        signal: controller.signal,
        onEvent: () => undefined,
      }),
    ).resolves.toBeUndefined();
  });
});
