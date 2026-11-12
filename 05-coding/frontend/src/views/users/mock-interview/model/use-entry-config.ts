// Source and session settings of the `entry` state, shared by the mock and the real session hook.
"use client";

import { useCallback, useState } from "react";
import type {
  InterviewEntryType,
  InterviewerLevel,
  MaxTurns,
  SessionConfig,
} from "@/entities/mock-interview";

export function useEntryConfig() {
  const [entryType, setEntryType] = useState<InterviewEntryType>("submission");
  const [submissionId, setSubmissionId] = useState<string | null>(null);
  const [questionId, setQuestionId] = useState<string | null>(null);
  const [customTopics, setCustomTopics] = useState<string[]>([]);
  const [level, setLevel] = useState<InterviewerLevel>("middle");
  const [maxTurns, setMaxTurns] = useState<MaxTurns>(12);
  const [hintAllowed, setHintAllowed] = useState(true);

  const canStart =
    (entryType === "submission" && submissionId !== null) ||
    (entryType === "bank" && questionId !== null) ||
    (entryType === "custom" && customTopics.length > 0);

  const toggleCustomTopic = useCallback((key: string) => {
    setCustomTopics((prev) =>
      prev.includes(key) ? prev.filter((k) => k !== key) : [...prev, key],
    );
  }, []);

  const config: SessionConfig = {
    entryType,
    submissionId,
    questionId,
    customTopics,
    level,
    maxTurns,
    hintAllowed,
  };

  return {
    entryType,
    setEntryType,
    submissionId,
    setSubmissionId,
    questionId,
    setQuestionId,
    customTopics,
    toggleCustomTopic,
    level,
    setLevel,
    maxTurns,
    setMaxTurns,
    hintAllowed,
    setHintAllowed,
    canStart,
    config,
  };
}
