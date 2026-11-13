// Real session of the mock interview (03-dd/screens/users/USR0302_mock_interview.md BR-SC-01 to
// BR-SC-10). The server is the source of truth: turnCount, stage and the timeline come from the
// stream events or from GET /ai/interview-sessions/{id}; this hook never counts turns itself.
// The answer of the learner is sent as the single `content` field and nothing else (BR-SC-11).
"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import {
  TurnStreamInterrupted,
  realCompleteSession,
  realGetSession,
  realRequestHint,
  realStartSession,
  streamTurn,
  type ChatMessage,
  type InterviewResult,
  type InterviewStage,
  type RealTurn,
  type SessionDetail,
} from "@/entities/mock-interview";
import { useBankQuestionOptions } from "@/entities/mock-interview";
import { useInterviewTopics } from "@/entities/interview-question";
import { ApiError } from "@/shared/api";
import { useT } from "@/shared/i18n";
import { useApiErrorMessage } from "@/shared/i18n/use-api-error-message";
import { toast } from "@/shared/lib/toast-store";
import type { InterviewSession, RunState, ScreenState } from "./session-types";
import { useEntryConfig } from "./use-entry-config";

const SESSION_KEY = "algoprep:mock-interview:session-id";
// DD BR-SC-05: poll the session every 3 s, 20 times (60 s), then say it is slow, not failed.
const POLL_MS = 3_000;
const POLL_MAX = 20;

const STREAM_ID = "ai-stream";
const PENDING_ID = "user-pending";

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));

function toMessage(turn: RealTurn): ChatMessage {
  return {
    id: `turn-${turn.turnIndex}`,
    role: turn.role,
    content: turn.content,
    stage: turn.stage,
  };
}

function code(error: unknown): string {
  return error instanceof ApiError ? error.code : "";
}

export function useRealInterviewSession(): InterviewSession {
  const t = useT("mockInterview");
  const apiMessage = useApiErrorMessage();
  const entry = useEntryConfig();
  const topics = useInterviewTopics();
  const bank = useBankQuestionOptions();

  const [screenState, setScreenState] = useState<ScreenState>("entry");
  const [runState, setRunState] = useState<RunState>("idle");
  const [starting, setStarting] = useState(false);
  const [waitingSlow, setWaitingSlow] = useState(false);
  const [hintPending, setHintPending] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [stage, setStage] = useState<InterviewStage>("explain");
  const [turnCount, setTurnCount] = useState(0);
  const [turnLimit, setTurnLimit] = useState<number | null>(null);
  const [hintOffered, setHintOffered] = useState(false);
  const [startedAt, setStartedAt] = useState<string | null>(null);
  const [elapsedSeconds, setElapsedSeconds] = useState(0);
  const [result, setResult] = useState<InterviewResult | null>(null);

  const sessionId = useRef<string | null>(null);
  const busy = useRef(false);
  const abort = useRef<AbortController | null>(null);
  const alive = useRef(true);
  // Latest server turn count, used for the optimistic check of the next turn (AIR-112).
  const serverTurns = useRef(0);

  const forget = useCallback(() => {
    sessionId.current = null;
    sessionStorage.removeItem(SESSION_KEY);
  }, []);

  const goEntry = useCallback(
    (message?: string) => {
      forget();
      setScreenState("entry");
      setRunState("idle");
      setMessages([]);
      setResult(null);
      setTurnCount(0);
      if (message) toast.error(message);
    },
    [forget],
  );

  // Puts a server snapshot on screen; returns the reply state it found.
  const apply = useCallback(
    (detail: SessionDetail) => {
      const { session } = detail;
      sessionId.current = session.sessionId;
      serverTurns.current = session.turnCount;
      setTurnCount(session.turnCount);
      setTurnLimit(session.maxTurns);
      setHintOffered(session.hintAllowed);
      setStartedAt(session.startedAt);
      if (session.level) entry.setLevel(session.level);
      if (session.stage === "abandoned") {
        goEntry(t("toast.sessionAbandoned"));
        return;
      }
      setMessages(detail.turns.map(toMessage));
      if (session.stage === "completed") {
        if (detail.result) {
          setResult(detail.result);
          setScreenState("result");
        } else {
          goEntry(t("toast.sessionGone"));
        }
        return;
      }
      setStage(session.stage);
      setScreenState("running");
      if (detail.replyState === "FAILED") setRunState("failedTurn");
      else if (detail.replyState === "GENERATING") setRunState("waiting");
      else setRunState("idle");
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps -- entry.setLevel is a stable setState
    [goEntry, t],
  );

  const reload = useCallback(async (): Promise<SessionDetail | null> => {
    if (!sessionId.current) return null;
    try {
      const detail = await realGetSession(sessionId.current);
      if (!alive.current) return null;
      apply(detail);
      return detail;
    } catch (error) {
      if (!alive.current) return null;
      if (code(error) === "AIR-110" || code(error) === "AIR-302") {
        goEntry(t("toast.sessionGone"));
      } else {
        toast.error(apiMessage(error));
      }
      return null;
    }
  }, [apply, apiMessage, goEntry, t]);

  // GENERATING / AIR-113: another request is producing the reply; poll until it settles.
  const pollUntilSettled = useCallback(async () => {
    setRunState("waiting");
    setWaitingSlow(false);
    for (let i = 0; i < POLL_MAX; i += 1) {
      await sleep(POLL_MS);
      if (!alive.current) return;
      const detail = await reload();
      if (!detail || detail.replyState !== "GENERATING") return;
    }
    setWaitingSlow(true);
  }, [reload]);

  const complete = useCallback(async () => {
    if (!sessionId.current) return;
    setRunState("completing");
    try {
      const done = await realCompleteSession(sessionId.current);
      if (!alive.current) return;
      setResult(done);
      setScreenState("result");
      setRunState("idle");
      toast.success(t("toast.finished"));
    } catch (error) {
      if (!alive.current) return;
      const c = code(error);
      if (c === "AIR-111") {
        await reload();
      } else if (c === "AIR-113") {
        await pollUntilSettled();
      } else if (c === "AIR-116") {
        toast.error(apiMessage(error));
        setRunState("idle");
      } else {
        // AIR-501/502 and anything else: the session stays as it was, scoring can be retried.
        toast.error(apiMessage(error));
        setRunState("completionPending");
      }
    }
  }, [apiMessage, pollUntilSettled, reload, t]);

  const runTurn = useCallback(
    async (content: string, expected: number, optimistic: boolean) => {
      const controller = new AbortController();
      abort.current = controller;
      setRunState("sending");
      if (optimistic) {
        setMessages((prev) => [
          ...prev,
          { id: PENDING_ID, role: "user", content, stage },
        ]);
      }
      let accepted = false;
      let finalShouldComplete = false;
      let failed = false;
      try {
        await streamTurn({
          sessionId: sessionId.current ?? "",
          content,
          expectedTurnCount: expected,
          signal: controller.signal,
          onEvent: (event) => {
            if (!alive.current) return;
            if (event.type === "accepted") {
              accepted = true;
              setStage(event.stage);
              setMessages((prev) => [
                ...prev,
                { id: STREAM_ID, role: "ai", content: "", stage: event.stage },
              ]);
              setRunState("streaming");
            } else if (event.type === "token") {
              setMessages((prev) =>
                prev.map((m) =>
                  m.id === STREAM_ID
                    ? { ...m, content: m.content + event.text }
                    : m,
                ),
              );
            } else if (event.type === "completed") {
              finalShouldComplete = event.shouldComplete;
              setStage(event.stage);
              setTurnCount(event.turnCount);
              serverTurns.current = event.turnCount;
              setTurnLimit(event.maxTurns);
              // The canonical text replaces what the tokens built.
              setMessages((prev) =>
                prev.map((m) =>
                  m.id === STREAM_ID
                    ? {
                        ...m,
                        id: `ai-${event.turnCount}`,
                        content: event.content,
                        stage: event.stage,
                      }
                    : m.id === PENDING_ID
                      ? { ...m, id: `user-${event.turnCount}` }
                      : m,
                ),
              );
              setRunState("idle");
            } else {
              failed = true;
              setMessages((prev) => prev.filter((m) => m.id !== STREAM_ID));
              toast.error(apiMessage(event.error));
              setRunState("failedTurn");
            }
          },
        });
      } catch (error) {
        if (!alive.current) return false;
        if (error instanceof TurnStreamInterrupted) {
          // DD BR-SC-05: ask the server what happened, never guess.
          const before = serverTurns.current;
          const detail = await reload();
          if (!accepted && detail && detail.session.turnCount === before) {
            return false;
          }
          return true;
        }
        // JSON error before the stream: the answer was not stored.
        setMessages((prev) => prev.filter((m) => m.id !== PENDING_ID));
        const c = code(error);
        if (c === "AIR-112" || c === "AIR-111") {
          await reload();
          if (c === "AIR-111") toast.error(apiMessage(error));
        } else if (c === "AIR-113") {
          void pollUntilSettled();
        } else if (c === "AIR-114") {
          setRunState("completionPending");
          void complete();
        } else if (c === "AIR-110" || c === "AIR-302") {
          goEntry(t("toast.sessionGone"));
        } else {
          toast.error(apiMessage(error));
          setRunState("idle");
        }
        return false;
      } finally {
        busy.current = false;
      }
      if (failed) return true;
      if (finalShouldComplete) await complete();
      return true;
    },
    [apiMessage, complete, goEntry, pollUntilSettled, reload, stage, t],
  );

  const sendMessage = useCallback(
    async (text: string) => {
      if (!text.trim() || busy.current || runState !== "idle") return false;
      busy.current = true;
      return runTurn(text, serverTurns.current, true);
    },
    [runState, runTurn],
  );

  const retryTurn = useCallback(async () => {
    if (busy.current) return;
    busy.current = true;
    // The server reuses the stored USER line and only regenerates the AI reply (AIR-112 safe).
    const detail = sessionId.current
      ? await realGetSession(sessionId.current).catch(() => null)
      : null;
    const last = detail?.turns[detail.turns.length - 1];
    if (!detail || detail.replyState !== "FAILED" || last?.role !== "user") {
      busy.current = false;
      if (detail) apply(detail);
      return;
    }
    await runTurn(last.content, detail.session.turnCount, false);
  }, [apply, runTurn]);

  const start = useCallback(async () => {
    if (starting) return;
    setStarting(true);
    const name = (key: string) =>
      topics.find((topic) => topic.key === key)?.label ?? key;
    try {
      const { session, opening } = await realStartSession({
        config: entry.config,
        questionText:
          bank.data?.find((q) => q.id === entry.questionId)?.title ?? null,
        topicNames: entry.customTopics.map(name),
      });
      sessionId.current = session.sessionId;
      sessionStorage.setItem(SESSION_KEY, session.sessionId);
      serverTurns.current = session.turnCount;
      setTurnCount(session.turnCount);
      setTurnLimit(session.maxTurns);
      setHintOffered(session.hintAllowed);
      setStartedAt(session.startedAt);
      if (session.stage !== "completed" && session.stage !== "abandoned") {
        setStage(session.stage);
      }
      // One block, shown whole: the opening arrives as REST, there is nothing to stream.
      setMessages([
        {
          id: `turn-${opening.turnIndex}`,
          role: "ai",
          content: opening.content,
          stage: opening.stage,
        },
      ]);
      setResult(null);
      setRunState("idle");
      setScreenState("running");
    } catch (error) {
      toast.error(apiMessage(error));
    } finally {
      if (alive.current) setStarting(false);
    }
  }, [
    apiMessage,
    bank.data,
    entry.config,
    entry.customTopics,
    entry.questionId,
    starting,
    topics,
  ]);

  const requestHint = useCallback(async () => {
    if (busy.current || runState !== "idle" || !sessionId.current) return;
    busy.current = true;
    setHintPending(true);
    try {
      const hint = await realRequestHint(sessionId.current);
      if (!alive.current) return;
      setMessages((prev) => [
        ...prev,
        {
          id: `hint-${prev.length}`,
          role: "ai",
          content: hint,
          stage,
          kind: "hint",
        },
      ]);
    } catch (error) {
      if (!alive.current) return;
      if (code(error) === "AIR-111") await reload();
      toast.error(apiMessage(error));
    } finally {
      busy.current = false;
      if (alive.current) setHintPending(false);
    }
  }, [apiMessage, reload, runState, stage]);

  const stopEarly = useCallback(() => {
    // Nothing answered yet: nothing to score (AIR-116); leave the session to the sweeper job.
    if (serverTurns.current === 0) {
      goEntry();
      return;
    }
    void complete();
  }, [complete, goEntry]);

  const retryPractice = useCallback(() => goEntry(), [goEntry]);

  // Restore a running session after a reload (BR-SC-06).
  useEffect(() => {
    alive.current = true;
    const saved = sessionStorage.getItem(SESSION_KEY);
    if (saved) {
      sessionId.current = saved;
      void (async () => {
        // A microtask first: the effect body itself must not set state synchronously.
        await Promise.resolve();
        if (!alive.current) return;
        setScreenState("running");
        setRunState("restoring");
        try {
          const detail = await realGetSession(saved);
          if (!alive.current) return;
          apply(detail);
          if (detail.replyState === "GENERATING") void pollUntilSettled();
        } catch (error) {
          if (alive.current) goEntry(apiMessage(error));
        }
      })();
    }
    return () => {
      // Leaving cancels the stream; the server drops the turn when the client goes away.
      alive.current = false;
      abort.current?.abort();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps -- restore once on mount
  }, []);

  // The clock follows the server `startedAt`, so a reload keeps the real elapsed time.
  useEffect(() => {
    if (screenState !== "running" || !startedAt) return;
    const tick = () =>
      setElapsedSeconds(
        Math.max(0, Math.floor((Date.now() - Date.parse(startedAt)) / 1000)),
      );
    tick();
    const id = setInterval(tick, 1000);
    return () => clearInterval(id);
  }, [screenState, startedAt]);

  return {
    ...entry,
    screenState,
    runState,
    starting,
    waitingSlow,
    hintOffered,
    hintPending,
    start: () => void start(),
    messages,
    isAiTyping: runState === "sending" || runState === "streaming",
    currentStage: stage,
    turnCount,
    turnLimit,
    elapsedSeconds,
    sendMessage,
    requestHint: () => void requestHint(),
    stopEarly,
    retryTurn: () => void retryTurn(),
    retryComplete: () => void complete(),
    retryPractice,
    result,
  };
}
