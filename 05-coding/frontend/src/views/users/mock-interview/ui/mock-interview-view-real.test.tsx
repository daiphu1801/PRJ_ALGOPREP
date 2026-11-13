// USR0302 against the REAL api path (F5 #8 to #12): only the HTTP layer is faked (`api` for REST,
// a stubbed `fetch` for the turn stream), so the real modules, the AIR-* mapping and the real vi
// texts are what is asserted.
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  waitFor,
} from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import viMessages from "../../../../../messages/vi.json";
import { ApiError } from "@/shared/api";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { withTestProviders } from "@/shared/test/render-with-providers";

vi.mock("@/shared/api", async (importOriginal) =>
  (await import("@/shared/test/real-api")).realApiModule(
    await importOriginal<Record<string, unknown>>(),
  ),
);
vi.mock("@/shared/api/mock-mode", () => ({
  isMockMode: () => false,
  withMockData: <T,>(_mock: () => unknown, real: () => Promise<T>) => real(),
}));
vi.mock("@/shared/api/client", () => ({ refreshAccessToken: vi.fn() }));
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn() }),
  usePathname: () => "/mock-interview",
}));

import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { MockInterviewView } from "./mock-interview-view";

const m = viMessages.mockInterview;
const errors = viMessages.apiErrors as Record<string, string>;
const BASE = "/ai/interview-sessions";
const SESSION_KEY = "algoprep:mock-interview:session-id";

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
  startedAt: new Date().toISOString(),
};
const turn = (i: number, role: "AI" | "USER", content: string) => ({
  turnIndex: i,
  role,
  stage: "EXPLAIN",
  content,
  createdAt: "2026-11-12T03:00:00Z",
});
const resultDto = {
  sessionId: "s-1",
  overallScore: 7.5,
  criteria: [
    { code: "CLARITY", score: 8, weightPercent: 25, comment: "clear" },
    { code: "TECHNICAL_ACCURACY", score: 7, weightPercent: 30, comment: "ok" },
    { code: "PUSHBACK_HANDLING", score: 7, weightPercent: 25, comment: "fine" },
    {
      code: "COMPLEXITY_AWARENESS",
      score: 8,
      weightPercent: 20,
      comment: "good",
    },
  ],
  feedbackSummary: "Solid session",
  turnCount: 1,
  endedAt: "2026-11-12T03:10:00Z",
};

const enc = new TextEncoder();
const frame = (event: string, data: object, seq: number) =>
  `id: ${seq}\nevent: ${event}\ndata: ${JSON.stringify({ seq, ...data })}\n\n`;
const accepted = frame(
  "turn.accepted",
  { stage: "EXPLAIN", turnCount: 0, maxTurns: 8 },
  1,
);
const completed = (shouldComplete = false, seq = 2) =>
  frame(
    "turn.completed",
    {
      content: "Next question?",
      stage: "CHALLENGE",
      stageChanged: true,
      turnCount: 1,
      maxTurns: 8,
      shouldComplete,
    },
    seq,
  );
const stream = (chunks: string[]) =>
  new Response(
    new ReadableStream<Uint8Array>({
      start(c) {
        chunks.forEach((x) => c.enqueue(enc.encode(x)));
        c.close();
      },
    }),
  );
const json = (status: number, errorCode: string) =>
  new Response(JSON.stringify({ errorCode, status }), { status });

let toasts: { current: readonly { tone: string; message: string }[] };
const lastToast = () => toasts.current.at(-1)?.message;
let fetchMock: ReturnType<typeof vi.fn>;

beforeEach(() => {
  resetApiMock();
  sessionStorage.clear();
  toasts = renderHook(() => useToasts()).result;
  act(() => toast.clear());
  fetchMock = vi.fn();
  vi.stubGlobal("fetch", fetchMock);
  apiMock.get.mockImplementation(async (path: string) => {
    if (path === "/interview-bank/topics")
      return {
        items: [
          {
            id: "t1",
            code: "DATABASE",
            displayName: "Database",
            sortOrder: 1,
            usesStarFramework: false,
            questionCount: null,
          },
        ],
      };
    if (path === `${BASE}/s-1`)
      return {
        session: { ...sessionDto, turnCount: 0 },
        replyState: "IDLE",
        turns: [turn(0, "AI", "Opening question")],
        result: null,
      };
    throw new Error(`unexpected GET ${path}`);
  });
});

async function startRunning() {
  apiMock.post.mockResolvedValueOnce({
    ...sessionDto,
    openingMessage: turn(0, "AI", "Opening question"),
  });
  const view = render(withTestProviders(<MockInterviewView />));
  fireEvent.click(screen.getByRole("button", { name: m.tab.custom }));
  fireEvent.click(await screen.findByRole("button", { name: "Database" }));
  fireEvent.click(screen.getByRole("button", { name: m.startCta }));
  await screen.findByText("Opening question");
  return view;
}

const answer = (text: string) => {
  fireEvent.change(screen.getByLabelText(m.messageInputLabel), {
    target: { value: text },
  });
  fireEvent.click(screen.getByRole("button", { name: m.send }));
};

describe("start", () => {
  it("opens a topic session with display names, the opening shows at once and the id is kept", async () => {
    await startRunning();
    expect(apiMock.post).toHaveBeenCalledWith(BASE, {
      entryType: "TOPIC_SELECTION",
      topics: ["Database"],
      interviewerLevel: "MIDDLE",
      maxTurns: 12,
      hintAllowed: true,
    });
    expect(sessionStorage.getItem(SESSION_KEY)).toBe("s-1");
  });

  it.each([
    ["AIR-401", 429],
    ["AIR-403", 429],
    ["AIR-404", 429],
    ["AIR-501", 503],
    ["AIR-502", 502],
  ])(
    "%s: toast from apiErrors and the screen stays on entry",
    async (c, status) => {
      apiMock.post.mockRejectedValueOnce(new ApiError(c, status));
      render(withTestProviders(<MockInterviewView />));
      fireEvent.click(screen.getByRole("button", { name: m.tab.custom }));
      fireEvent.click(await screen.findByRole("button", { name: "Database" }));
      fireEvent.click(screen.getByRole("button", { name: m.startCta }));
      await waitFor(() => expect(lastToast()).toBe(errors[c]));
      expect(screen.getByRole("button", { name: m.startCta })).toBeEnabled();
      expect(sessionStorage.getItem(SESSION_KEY)).toBeNull();
    },
  );
});

describe("a turn over the stream", () => {
  it("shows the tokens, replaces them with the canonical text and moves the stage", async () => {
    await startRunning();
    fetchMock.mockResolvedValueOnce(
      stream([
        accepted,
        frame("token", { text: "Next " }, 2),
        completed(false, 3),
      ]),
    );
    answer("my answer");
    expect(await screen.findByText("Next question?")).toBeInTheDocument();
    expect(screen.getByText("my answer")).toBeInTheDocument();
    expect(screen.getByText(/Lượt 1 \/ 8/)).toBeInTheDocument();
    const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(JSON.parse(init.body as string)).toEqual({
      content: "my answer",
      expectedTurnCount: 0,
    });
  });

  it("ignores a second send while the first is generating (double-submit guard)", async () => {
    await startRunning();
    let release: () => void = () => undefined;
    const gate = new Promise<void>((r) => (release = r));
    fetchMock.mockResolvedValueOnce(
      new Response(
        new ReadableStream<Uint8Array>({
          async start(c) {
            c.enqueue(enc.encode(accepted));
            await gate;
            c.enqueue(enc.encode(completed()));
            c.close();
          },
        }),
      ),
    );
    answer("first");
    await screen.findByText("first");
    expect(screen.getByLabelText(m.messageInputLabel)).toHaveAttribute(
      "readonly",
    );
    expect(screen.getByRole("button", { name: m.send })).toBeDisabled();
    // The server answers 409 to a complete call while a turn is in flight, so the button waits too.
    expect(screen.getByRole("button", { name: m.stopEarlyCta })).toBeDisabled();
    release();
    await screen.findByText("Next question?");
    expect(screen.getByRole("button", { name: m.stopEarlyCta })).toBeEnabled();
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("AIR-112 reloads the session and keeps the draft in the box", async () => {
    await startRunning();
    fetchMock.mockResolvedValueOnce(json(409, "AIR-112"));
    answer("draft text");
    await waitFor(() =>
      expect(apiMock.get).toHaveBeenCalledWith(`${BASE}/s-1`),
    );
    await waitFor(() =>
      expect(screen.getByLabelText(m.messageInputLabel)).toHaveValue(
        "draft text",
      ),
    );
    expect(screen.queryByText("draft text", { selector: "div" })).toBeNull();
  });

  it("AIR-113 shows the waiting notice and locks the box (the poll runs every 3 s)", async () => {
    await startRunning();
    fetchMock.mockResolvedValueOnce(json(409, "AIR-113"));
    answer("x");
    expect(await screen.findByText(m.waitingReply)).toBeInTheDocument();
    expect(screen.getByLabelText(m.messageInputLabel)).toHaveAttribute(
      "readonly",
    );
  });

  it("AIR-114 (max turns) completes the session on its own and shows the result", async () => {
    await startRunning();
    fetchMock.mockResolvedValueOnce(json(409, "AIR-114"));
    apiMock.post.mockResolvedValueOnce(resultDto);
    answer("x");
    expect(await screen.findByText("Solid session")).toBeInTheDocument();
    expect(apiMock.post).toHaveBeenLastCalledWith(`${BASE}/s-1/complete`);
    expect(
      screen.getByText(/Độ chính xác kỹ thuật \(\s*30%\)/),
    ).toBeInTheDocument();
  });

  it.each([
    ["AIR-501", 503],
    ["AIR-V08", 400],
  ])(
    "%s before the stream: the answer is not kept, draft returns, toast from apiErrors",
    async (c, status) => {
      await startRunning();
      fetchMock.mockResolvedValueOnce(json(status, c));
      answer("keep me");
      await waitFor(() =>
        expect(lastToast()).toBe(errors[c] ?? errors.UNKNOWN_ERROR),
      );
      expect(screen.getByLabelText(m.messageInputLabel)).toHaveValue("keep me");
    },
  );

  it("AIR-111 reloads and shows the server state", async () => {
    await startRunning();
    fetchMock.mockResolvedValueOnce(json(409, "AIR-111"));
    answer("x");
    await waitFor(() => expect(lastToast()).toBe(errors["AIR-111"]));
  });

  it.each(["AIR-501", "AIR-502"])(
    "SSE error %s after accepted: the answer stays, a retry is offered and works",
    async (c) => {
      await startRunning();
      fetchMock.mockResolvedValueOnce(
        stream([
          accepted,
          frame("error", { error: { status: 503, errorCode: c } }, 2),
        ]),
      );
      answer("kept answer");
      expect(await screen.findByText(m.turnFailedTitle)).toBeInTheDocument();
      expect(screen.getByText("kept answer")).toBeInTheDocument();
      expect(lastToast()).toBe(errors[c]);

      apiMock.get.mockResolvedValueOnce({
        session: { ...sessionDto, turnCount: 0 },
        replyState: "FAILED",
        turns: [
          turn(0, "AI", "Opening question"),
          turn(1, "USER", "kept answer"),
        ],
        result: null,
      });
      fetchMock.mockResolvedValueOnce(stream([accepted, completed()]));
      fireEvent.click(screen.getByRole("button", { name: m.turnRetry }));
      expect(await screen.findByText("Next question?")).toBeInTheDocument();
      const init = fetchMock.mock.calls[1]?.[1] as RequestInit;
      expect(JSON.parse(init.body as string)).toEqual({
        content: "kept answer",
        expectedTurnCount: 0,
      });
    },
  );

  it("a stream cut after accepted reconciles through GET and shows the finished reply", async () => {
    await startRunning();
    fetchMock.mockResolvedValueOnce(stream([accepted]));
    apiMock.get.mockResolvedValueOnce({
      session: { ...sessionDto, turnCount: 1, stage: "CHALLENGE" },
      replyState: "IDLE",
      turns: [
        turn(0, "AI", "Opening question"),
        turn(1, "USER", "x"),
        turn(2, "AI", "Saved reply"),
      ],
      result: null,
    });
    answer("x");
    expect(await screen.findByText("Saved reply")).toBeInTheDocument();
  });

  it("unmounting aborts the stream", async () => {
    const view = await startRunning();
    let signal: AbortSignal | undefined;
    fetchMock.mockImplementation((_url: string, init: RequestInit) => {
      signal = init.signal as AbortSignal;
      return new Promise(() => undefined);
    });
    answer("x");
    await waitFor(() => expect(signal).toBeDefined());
    expect(signal?.aborted).toBe(false);
    view.unmount();
    expect(signal?.aborted).toBe(true);
  });
});

describe("hint and completion", () => {
  it("shows a hint as its own block", async () => {
    await startRunning();
    apiMock.post.mockResolvedValueOnce({
      hint: "Think about indexes",
      stage: "EXPLAIN",
    });
    fireEvent.click(screen.getByRole("button", { name: m.hintCta }));
    expect(await screen.findByText("Think about indexes")).toBeInTheDocument();
    expect(screen.getByText(m.hintBlockTitle)).toBeInTheDocument();
    expect(apiMock.post).toHaveBeenLastCalledWith(`${BASE}/s-1/hint`);
  });

  it.each(["AIR-115", "AIR-117", "AIR-113", "AIR-501"])(
    "hint %s: toast from apiErrors, the timeline is untouched",
    async (c) => {
      await startRunning();
      apiMock.post.mockRejectedValueOnce(new ApiError(c, 409));
      fireEvent.click(screen.getByRole("button", { name: m.hintCta }));
      await waitFor(() => expect(lastToast()).toBe(errors[c]));
      expect(screen.queryByText(m.hintBlockTitle)).toBeNull();
    },
  );

  it("leaving with no answer calls nothing (AIR-116 is never provoked)", async () => {
    await startRunning();
    fireEvent.click(screen.getByRole("button", { name: m.stopEarlyCta }));
    fireEvent.click(
      await screen.findByRole("button", { name: m.earlyExitNoTurnConfirm }),
    );
    expect(
      await screen.findByRole("button", { name: m.startCta }),
    ).toBeInTheDocument();
    expect(apiMock.post).toHaveBeenCalledTimes(1);
    expect(sessionStorage.getItem(SESSION_KEY)).toBeNull();
  });

  it("scores after one answer: result with the server weights and one toast", async () => {
    await startRunning();
    fetchMock.mockResolvedValueOnce(stream([accepted, completed()]));
    answer("x");
    await screen.findByText("Next question?");
    apiMock.post.mockResolvedValueOnce(resultDto);
    fireEvent.click(screen.getByRole("button", { name: m.stopEarlyCta }));
    fireEvent.click(
      await screen.findByRole("button", { name: m.earlyExitConfirm }),
    );
    expect(await screen.findByText("Solid session")).toBeInTheDocument();
    expect(screen.getByText(/Độ rõ ràng \(\s*25%\)/)).toBeInTheDocument();
    expect(
      toasts.current.filter((x) => x.message === m.toast.finished),
    ).toHaveLength(1);
  });

  it.each(["AIR-501", "AIR-502"])(
    "complete %s: the session stays and scoring can be retried",
    async (c) => {
      await startRunning();
      fetchMock.mockResolvedValueOnce(stream([accepted, completed(true)]));
      apiMock.post.mockRejectedValueOnce(new ApiError(c, 503));
      answer("x");
      expect(
        await screen.findByText(m.completionPendingTitle),
      ).toBeInTheDocument();
      expect(lastToast()).toBe(errors[c]);
      apiMock.post.mockResolvedValueOnce(resultDto);
      fireEvent.click(screen.getByRole("button", { name: m.completionRetry }));
      expect(await screen.findByText("Solid session")).toBeInTheDocument();
    },
  );

  it("complete AIR-116 is shown through apiErrors", async () => {
    await startRunning();
    fetchMock.mockResolvedValueOnce(stream([accepted, completed()]));
    answer("x");
    await screen.findByText("Next question?");
    apiMock.post.mockRejectedValueOnce(new ApiError("AIR-116", 422));
    fireEvent.click(screen.getByRole("button", { name: m.stopEarlyCta }));
    fireEvent.click(
      await screen.findByRole("button", { name: m.earlyExitConfirm }),
    );
    await waitFor(() => expect(lastToast()).toBe(errors["AIR-116"]));
  });

  it("complete AIR-111 reloads: a session finished elsewhere opens on its result", async () => {
    await startRunning();
    fetchMock.mockResolvedValueOnce(stream([accepted, completed()]));
    answer("x");
    await screen.findByText("Next question?");
    apiMock.post.mockRejectedValueOnce(new ApiError("AIR-111", 409));
    apiMock.get.mockResolvedValueOnce({
      session: { ...sessionDto, stage: "COMPLETED" },
      replyState: "IDLE",
      turns: [turn(0, "AI", "Opening question")],
      result: resultDto,
    });
    fireEvent.click(screen.getByRole("button", { name: m.stopEarlyCta }));
    fireEvent.click(
      await screen.findByRole("button", { name: m.earlyExitConfirm }),
    );
    expect(await screen.findByText("Solid session")).toBeInTheDocument();
  });
});

describe("reload", () => {
  it("restores a saved session and offers the retry when the last reply failed", async () => {
    sessionStorage.setItem(SESSION_KEY, "s-1");
    apiMock.get.mockImplementation(async (path: string) => {
      if (path === `${BASE}/s-1`)
        return {
          session: sessionDto,
          replyState: "FAILED",
          turns: [
            turn(0, "AI", "Opening question"),
            turn(1, "USER", "my reply"),
          ],
          result: null,
        };
      return { items: [] };
    });
    render(withTestProviders(<MockInterviewView />));
    expect(await screen.findByText("my reply")).toBeInTheDocument();
    expect(screen.getByText(m.turnFailedTitle)).toBeInTheDocument();
  });

  it.each(["AIR-110", "AIR-302"])(
    "%s on restore drops the saved id and returns to entry",
    async (c) => {
      sessionStorage.setItem(SESSION_KEY, "s-1");
      apiMock.get.mockImplementation(async (path: string) => {
        if (path === `${BASE}/s-1`) throw new ApiError(c, 404);
        return { items: [] };
      });
      render(withTestProviders(<MockInterviewView />));
      expect(
        await screen.findByRole("button", { name: m.startCta }),
      ).toBeInTheDocument();
      expect(sessionStorage.getItem(SESSION_KEY)).toBeNull();
    },
  );

  it("a completed session opens straight on the result without a toast", async () => {
    sessionStorage.setItem(SESSION_KEY, "s-1");
    apiMock.get.mockImplementation(async (path: string) =>
      path === `${BASE}/s-1`
        ? {
            session: { ...sessionDto, stage: "COMPLETED" },
            replyState: "IDLE",
            turns: [turn(0, "AI", "Opening question")],
            result: resultDto,
          }
        : { items: [] },
    );
    render(withTestProviders(<MockInterviewView />));
    expect(await screen.findByText("Solid session")).toBeInTheDocument();
  });
});
