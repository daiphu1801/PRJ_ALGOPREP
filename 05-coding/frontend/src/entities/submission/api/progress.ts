import { useEffect, useRef } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { api, isMockMode } from "@/shared/api";
import { subscribeJudge } from "@/shared/api/stomp";

export type JudgeProgress = {
  submissionId: string;
  status: string;
  seq: number;
  isFinal: boolean;
  totalCount: number | null;
  completedCount: number;
  passedCount: number;
  testcases: { order: number; verdict: string }[];
};
export type JudgeResult = {
  submissionId: string;
  status: string;
  passedCount: number | null;
  totalCount: number | null;
  runtimeMs: number | null;
  memoryKb: number | null;
  compileErrorMessage: string | null;
  testcaseDetail: { items: { order: number; verdict: string }[] };
};
type Callbacks = {
  progress: (progress: JudgeProgress) => void;
  finished: (result: JudgeResult) => void;
  error: (error: unknown) => void;
  slow: () => void;
};

export function useSubmissionProgress(
  submissionId: string | undefined,
  callbacks: Callbacks,
) {
  const queryClient = useQueryClient();
  const latest = useRef(callbacks);
  useEffect(() => {
    latest.current = callbacks;
  });
  useEffect(() => {
    if (!submissionId || isMockMode()) return;
    let disposed = false;
    let final = false;
    let busy = false;
    let rerun = false;
    let seq = -1;
    let attempts = 0;
    let lastMessage = 0;
    let timer: ReturnType<typeof setTimeout>;
    let stopSocket = () => {};
    const path = `/judge/submissions/${encodeURIComponent(submissionId)}`;
    async function snapshot() {
      if (disposed || final) return;
      if (busy) {
        rerun = true;
        return;
      }
      busy = true;
      try {
        const value = await api.get<JudgeProgress>(`${path}/progress`);
        if (disposed || final || value.submissionId !== submissionId) return;
        if (value.isFinal || value.seq > seq) {
          seq = Math.max(seq, value.seq);
          if (!value.isFinal) latest.current.progress(value);
        }
        if (value.isFinal) {
          const detail = await api.get<JudgeResult>(path);
          if (disposed) return;
          final = true;
          clearTimeout(timer);
          stopSocket();
          latest.current.finished(detail);
          void queryClient.invalidateQueries({ queryKey: ["submission"] });
          void queryClient.invalidateQueries({
            queryKey: ["problem", "workspace"],
          });
        }
      } catch (error) {
        if (!disposed) latest.current.error(error);
      } finally {
        busy = false;
        if (rerun && !disposed && !final) {
          rerun = false;
          void snapshot();
        }
      }
    }
    const tick = () => {
      if (disposed || final) return;
      if (Date.now() - lastMessage >= 3000) {
        if (attempts >= 10) {
          latest.current.slow();
          return;
        }
        attempts++;
        void snapshot();
      }
      timer = setTimeout(tick, 3000);
    };
    stopSocket = subscribeJudge("/user/queue/submissions", {
      connected: () => {
        attempts = 0;
        lastMessage = Date.now();
        void snapshot();
        clearTimeout(timer);
        timer = setTimeout(tick, 3000);
      },
      disconnected: () => {
        lastMessage = 0;
      },
      message: (message) => {
        const event = message as Partial<JudgeProgress>;
        if (
          event.submissionId !== submissionId ||
          typeof event.seq !== "number"
        )
          return;
        if (!event.isFinal && event.seq <= seq) return;
        lastMessage = Date.now();
        void snapshot();
      },
    });
    // Covers connection failure as well as a completion before SUBSCRIBE.
    void snapshot();
    timer = setTimeout(tick, 3000);
    return () => {
      disposed = true;
      clearTimeout(timer);
      stopSocket();
    };
  }, [submissionId, queryClient]);
}
