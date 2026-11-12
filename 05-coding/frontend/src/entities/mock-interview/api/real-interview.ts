// Real calls of the F5 mock interview (03-dd/api/F5_ai-review.md #8 to #12, SSE section 2).
// The wire is the controller records (InterviewSessionController, InterviewTurnController,
// SseTurnSink), not the DD prose. The answer of the learner travels only as the `content` field of
// the turn body and is never wrapped or prefixed here (prompt-injection defence, DD screen
// BR-SC-11). Error codes: #8 AIR-101/102/302/401/403/404/501/502/V03..V07; #9 AIR-110/302; #10
// AIR-110/111/112/113/114/302/501/502/V08/V09 (JSON before the stream, an SSE `error` event after
// it); #11 AIR-111/113/115/117/501; #12 AIR-111/113/116/501/502. All are shown through
// useApiErrorMessage by the screen.
import { ApiError, api, createSseParser, tokenStore } from "@/shared/api";
import { toApiError, type ErrorBody } from "@/shared/api/api-error";
import { env } from "@/shared/api/config";
import { refreshAccessToken } from "@/shared/api/client";
import {
  RUBRIC_WEIGHTS,
  type InterviewResult,
  type InterviewStage,
  type InterviewerLevel,
  type RubricCriterionCode,
  type SessionConfig,
} from "../model/types";

const BASE = "/ai/interview-sessions";

type StageDto =
  "EXPLAIN" | "CHALLENGE" | "SCALE_UP" | "COMPLETED" | "ABANDONED";
type TurnDto = {
  turnIndex: number;
  role: "USER" | "AI";
  stage: StageDto;
  content: string;
  createdAt: string;
};
type SessionDto = {
  sessionId: string;
  entryType: string;
  stage: StageDto;
  turnCount: number;
  maxTurns: number | null;
  hintAllowed: boolean | null;
  interviewerLevel: string | null;
  topics: string[] | null;
  submissionId: string | null;
  questionRef: string | null;
  startedAt: string;
};
type ResultDto = {
  overallScore: number;
  criteria: {
    code: string;
    score: number;
    weightPercent: number;
    comment: string;
  }[];
  feedbackSummary: string;
  turnCount: number;
  endedAt: string;
};

const STAGE: Record<string, InterviewStage> = {
  EXPLAIN: "explain",
  CHALLENGE: "challenge",
  SCALE_UP: "scaleUp",
};
const CRITERION: Record<string, RubricCriterionCode> = {
  CLARITY: "clarity",
  TECHNICAL_ACCURACY: "technicalAccuracy",
  PUSHBACK_HANDLING: "pushbackHandling",
  COMPLEXITY_AWARENESS: "complexityAwareness",
};

export type SessionStage = InterviewStage | "completed" | "abandoned";
export type ReplyState = "IDLE" | "GENERATING" | "FAILED";

export type RealTurn = {
  turnIndex: number;
  role: "ai" | "user";
  stage: InterviewStage;
  content: string;
};

export type RealSession = {
  sessionId: string;
  stage: SessionStage;
  turnCount: number;
  /** Null for the SUBMISSION entry: the ceiling is a server setting the client never sees. */
  maxTurns: number | null;
  hintAllowed: boolean;
  level: InterviewerLevel | null;
  startedAt: string;
};

export type SessionDetail = {
  session: RealSession;
  replyState: ReplyState;
  turns: RealTurn[];
  result: InterviewResult | null;
};

function toStage(stage: StageDto): SessionStage {
  if (stage === "COMPLETED") return "completed";
  if (stage === "ABANDONED") return "abandoned";
  return STAGE[stage] ?? "explain";
}

export function toStageFromEvent(stage: string): InterviewStage {
  return STAGE[stage] ?? "explain";
}

function toSession(dto: SessionDto): RealSession {
  return {
    sessionId: dto.sessionId,
    stage: toStage(dto.stage),
    turnCount: dto.turnCount,
    maxTurns: dto.maxTurns,
    // SUBMISSION sessions store NULL (hints are not offered there, DD screen BR-SC-07).
    hintAllowed: dto.hintAllowed === true,
    level: dto.interviewerLevel
      ? (dto.interviewerLevel.toLowerCase() as InterviewerLevel)
      : null,
    startedAt: dto.startedAt,
  };
}

function toTurn(dto: TurnDto): RealTurn {
  return {
    turnIndex: dto.turnIndex,
    role: dto.role === "AI" ? "ai" : "user",
    stage: toStageFromEvent(dto.stage),
    content: dto.content,
  };
}

export function toResult(dto: ResultDto): InterviewResult {
  const criteria = dto.criteria.flatMap((c) => {
    const code = CRITERION[c.code];
    return code
      ? [
          {
            code,
            score: c.score,
            comment: c.comment,
            weightPercent: c.weightPercent ?? RUBRIC_WEIGHTS[code],
          },
        ]
      : [];
  });
  return {
    overallScore: dto.overallScore,
    criteria,
    feedbackSummary: dto.feedbackSummary,
    strengths: [],
    improvements: [],
  };
}

/** The topics are sent as display names (DD screen BR-SC-02 step 6). */
export type StartInput = {
  config: SessionConfig;
  questionText: string | null;
  topicNames: string[];
};

export async function realStartSession(
  input: StartInput,
): Promise<{ session: RealSession; opening: RealTurn }> {
  const { config } = input;
  let body: Record<string, unknown>;
  if (config.entryType === "submission") {
    // Level, turns and hint settings are ignored by the server for this entry (stored NULL).
    body = { entryType: "SUBMISSION", submissionId: config.submissionId };
  } else {
    const common = {
      interviewerLevel: config.level.toUpperCase(),
      maxTurns: config.maxTurns,
      hintAllowed: config.hintAllowed,
    };
    body =
      config.entryType === "bank"
        ? {
            entryType: "QUESTION_BANK",
            questionRef: config.questionId,
            questionText: input.questionText,
            ...common,
          }
        : { entryType: "TOPIC_SELECTION", topics: input.topicNames, ...common };
  }
  const dto = await api.post<SessionDto & { openingMessage: TurnDto }>(
    BASE,
    body,
  );
  return { session: toSession(dto), opening: toTurn(dto.openingMessage) };
}

export async function realGetSession(
  sessionId: string,
): Promise<SessionDetail> {
  const dto = await api.get<{
    session: SessionDto;
    replyState: ReplyState;
    turns: TurnDto[];
    result: ResultDto | null;
  }>(`${BASE}/${sessionId}`);
  return {
    session: toSession(dto.session),
    replyState: dto.replyState,
    turns: dto.turns.map(toTurn),
    result: dto.result ? toResult(dto.result) : null,
  };
}

/** Hints are not stored by the server: the text lives only on the client (DD F5 11.3). */
export async function realRequestHint(sessionId: string): Promise<string> {
  const dto = await api.post<{ hint: string; stage: string }>(
    `${BASE}/${sessionId}/hint`,
  );
  return dto.hint;
}

export async function realCompleteSession(
  sessionId: string,
): Promise<InterviewResult> {
  return toResult(await api.post<ResultDto>(`${BASE}/${sessionId}/complete`));
}

// ---- Turn stream (endpoint #10) ---------------------------------------------------------------

export type TurnEvent =
  | {
      type: "accepted";
      stage: InterviewStage;
      turnCount: number;
      maxTurns: number | null;
    }
  | { type: "token"; text: string }
  | {
      type: "completed";
      content: string;
      stage: InterviewStage;
      stageChanged: boolean;
      turnCount: number;
      maxTurns: number | null;
      shouldComplete: boolean;
    }
  | { type: "error"; error: ApiError };

/**
 * The connection dropped, or the stream broke its `seq` order, before a final event. `accepted`
 * says whether `turn.accepted` had arrived (the answer is then stored server side); the screen
 * reconciles through endpoint #9 either way (DD F5 section 2.4).
 */
export class TurnStreamInterrupted extends Error {
  readonly accepted: boolean;
  constructor(accepted: boolean) {
    super("TURN_STREAM_INTERRUPTED");
    this.name = "TurnStreamInterrupted";
    this.accepted = accepted;
  }
}

type Frame = Record<string, unknown>;

function parseFrame(data: string): Frame | null {
  try {
    const value: unknown = JSON.parse(data);
    return typeof value === "object" && value !== null
      ? (value as Frame)
      : null;
  } catch {
    return null;
  }
}

async function openTurn(
  url: string,
  content: string,
  expectedTurnCount: number,
  signal: AbortSignal,
): Promise<Response> {
  const post = () => {
    const headers: Record<string, string> = {
      Accept: "text/event-stream",
      "Content-Type": "application/json",
    };
    const token = tokenStore.get();
    if (token) headers.Authorization = `Bearer ${token}`;
    return fetch(url, {
      method: "POST",
      headers,
      credentials: "include",
      signal,
      body: JSON.stringify({ content, expectedTurnCount }),
    });
  };
  let response = await post();
  if (response.status === 401) {
    // Safe to retry once: before `turn.accepted` nothing was written (DD screen section 1).
    const renewed = await refreshAccessToken();
    if (renewed) {
      tokenStore.set(renewed);
      response = await post();
    }
  }
  return response;
}

/**
 * POST /ai/interview-sessions/{id}/turns as a `fetch` stream (never EventSource: it cannot send
 * the Bearer header or a body). Rejects with ApiError for a JSON error before the stream, with
 * TurnStreamInterrupted when the stream ends without a final event; aborting (`signal`) resolves
 * silently. The server cancels the LLM call when the client goes away.
 */
export async function streamTurn(args: {
  sessionId: string;
  content: string;
  expectedTurnCount: number;
  signal: AbortSignal;
  onEvent: (event: TurnEvent) => void;
}): Promise<void> {
  const url = `${env.apiBaseUrl.replace(/\/$/, "")}${BASE}/${args.sessionId}/turns`;
  let accepted = false;
  let finished = false;
  let broken = false;
  let lastSeq = 0;
  try {
    const response = await openTurn(
      url,
      args.content,
      args.expectedTurnCount,
      args.signal,
    );
    if (!response.ok || !response.body) {
      const body = (await response.json().catch(() => undefined)) as
        ErrorBody | undefined;
      throw toApiError(response.status, body);
    }
    const parse = createSseParser((message) => {
      const data = parseFrame(message.data);
      if (!data || finished) return;
      const seq = typeof data.seq === "number" ? data.seq : 0;
      if (seq !== lastSeq + 1) {
        broken = true;
        return;
      }
      lastSeq = seq;
      if (message.event === "turn.accepted") {
        accepted = true;
        args.onEvent({
          type: "accepted",
          stage: toStageFromEvent(String(data.stage)),
          turnCount: Number(data.turnCount),
          maxTurns: data.maxTurns == null ? null : Number(data.maxTurns),
        });
      } else if (message.event === "token") {
        args.onEvent({ type: "token", text: String(data.text ?? "") });
      } else if (message.event === "turn.completed") {
        finished = true;
        args.onEvent({
          type: "completed",
          content: String(data.content ?? ""),
          stage: toStageFromEvent(String(data.stage)),
          stageChanged: data.stageChanged === true,
          turnCount: Number(data.turnCount),
          maxTurns: data.maxTurns == null ? null : Number(data.maxTurns),
          shouldComplete: data.shouldComplete === true,
        });
      } else if (message.event === "error") {
        finished = true;
        const envelope = (data.error ?? {}) as ErrorBody & { status?: number };
        args.onEvent({
          type: "error",
          error: toApiError(envelope.status ?? 503, envelope),
        });
      }
    });
    const reader = response.body.getReader();
    const decoder = new TextDecoder("utf-8");
    for (;;) {
      const { done, value } = await reader.read();
      if (done) break;
      parse(decoder.decode(value, { stream: true }));
      if (broken) {
        await reader.cancel().catch(() => undefined);
        break;
      }
    }
  } catch (error) {
    if (args.signal.aborted) return;
    if (error instanceof ApiError) throw error;
    throw new TurnStreamInterrupted(accepted);
  }
  if (!finished && !args.signal.aborted) {
    throw new TurnStreamInterrupted(accepted);
  }
}
