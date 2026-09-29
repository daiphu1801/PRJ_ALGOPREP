// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 10.2 (USR0302_mock_interview).
//
// State machine for the 3-state screen (BD Sheet 4.1: entry -> running -> result). SSE streaming
// (F5-14) is faked with setInterval revealing characters of a scripted reply — see
// entities/mock-interview/api/__mock__/mock-interview-mocks.ts for why the questions themselves are
// scripted rather than "AI-decided".
"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import {
  computeInterviewResult,
  getScriptedQuestion,
  HINT_TEXT,
  type ChatMessage,
  type InterviewEntryType,
  type InterviewResult,
  type InterviewStage,
  type InterviewerLevel,
  type MaxTurns,
} from "@/entities/mock-interview";

export type ScreenState = "entry" | "running" | "result";

const STREAM_CHARS_PER_TICK = 3;
const STREAM_TICK_MS = 20;

let messageIdSeq = 0;
function nextId(): string {
  messageIdSeq += 1;
  return `msg-${messageIdSeq}`;
}

export function useMockInterviewSession() {
  const [screenState, setScreenState] = useState<ScreenState>("entry");

  const [entryType, setEntryType] = useState<InterviewEntryType>("submission");
  const [submissionId, setSubmissionId] = useState<string | null>(null);
  const [questionId, setQuestionId] = useState<string | null>(null);
  const [level, setLevel] = useState<InterviewerLevel>("middle");
  const [maxTurns, setMaxTurns] = useState<MaxTurns>(12);
  const [hintAllowed, setHintAllowed] = useState(true);

  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [currentStage, setCurrentStage] = useState<InterviewStage>("explain");
  const [turnCount, setTurnCount] = useState(0);
  const [elapsedSeconds, setElapsedSeconds] = useState(0);
  const [streamingMessageId, setStreamingMessageId] = useState<string | null>(null);
  const [streamedLength, setStreamedLength] = useState(0);
  const [result, setResult] = useState<InterviewResult | null>(null);

  const streamIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const timerIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const canStart =
    (entryType === "submission" && submissionId !== null) ||
    (entryType === "bank" && questionId !== null) ||
    entryType === "custom";

  const streamAiMessage = useCallback((content: string, stage: InterviewStage) => {
    const id = nextId();
    setMessages((prev) => [...prev, { id, role: "ai", content, stage }]);
    setStreamingMessageId(id);
    setStreamedLength(0);

    if (streamIntervalRef.current) clearInterval(streamIntervalRef.current);
    streamIntervalRef.current = setInterval(() => {
      setStreamedLength((prevLength) => {
        const nextLength = prevLength + STREAM_CHARS_PER_TICK;
        if (nextLength >= content.length) {
          if (streamIntervalRef.current) clearInterval(streamIntervalRef.current);
          setStreamingMessageId(null);
          return content.length;
        }
        return nextLength;
      });
    }, STREAM_TICK_MS);
  }, []);

  const start = useCallback(() => {
    setScreenState("running");
    setTurnCount(0);
    setElapsedSeconds(0);
    setMessages([]);
    setResult(null);

    const first = getScriptedQuestion(0, maxTurns);
    setCurrentStage(first.stage);
    streamAiMessage(first.content, first.stage);

    timerIntervalRef.current = setInterval(() => setElapsedSeconds((s) => s + 1), 1000);
  }, [maxTurns, streamAiMessage]);

  const finish = useCallback(() => {
    if (timerIntervalRef.current) clearInterval(timerIntervalRef.current);
    if (streamIntervalRef.current) clearInterval(streamIntervalRef.current);
    setResult(computeInterviewResult());
    setScreenState("result");
  }, []);

  const sendMessage = useCallback(
    (text: string) => {
      if (!text.trim() || streamingMessageId) return;

      setMessages((prev) => [...prev, { id: nextId(), role: "user", content: text, stage: currentStage }]);
      const nextTurn = turnCount + 1;
      setTurnCount(nextTurn);

      if (nextTurn >= maxTurns) {
        finish();
        return;
      }

      const next = getScriptedQuestion(nextTurn, maxTurns);
      setCurrentStage(next.stage);
      streamAiMessage(next.content, next.stage);
    },
    [currentStage, finish, maxTurns, streamAiMessage, streamingMessageId, turnCount],
  );

  const requestHint = useCallback(() => {
    if (streamingMessageId) return;
    streamAiMessage(HINT_TEXT, currentStage);
  }, [currentStage, streamAiMessage, streamingMessageId]);

  const stopEarly = useCallback(() => finish(), [finish]);

  const retryPractice = useCallback(() => {
    setScreenState("entry");
    setMessages([]);
    setResult(null);
    setTurnCount(0);
  }, []);

  useEffect(() => {
    return () => {
      if (timerIntervalRef.current) clearInterval(timerIntervalRef.current);
      if (streamIntervalRef.current) clearInterval(streamIntervalRef.current);
    };
  }, []);

  const displayedMessages = messages.map((message) =>
    message.id === streamingMessageId
      ? { ...message, content: message.content.slice(0, streamedLength) }
      : message,
  );

  return {
    screenState,
    entryType,
    setEntryType,
    submissionId,
    setSubmissionId,
    questionId,
    setQuestionId,
    level,
    setLevel,
    maxTurns,
    setMaxTurns,
    hintAllowed,
    setHintAllowed,
    canStart,
    start,
    messages: displayedMessages,
    isAiTyping: streamingMessageId !== null,
    currentStage,
    turnCount,
    elapsedSeconds,
    sendMessage,
    requestHint,
    stopEarly,
    retryPractice,
    result,
  };
}
