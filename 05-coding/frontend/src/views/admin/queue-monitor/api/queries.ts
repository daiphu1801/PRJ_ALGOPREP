// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 9.
//
// Data boundary of the queue monitor (ADM0401). Every hook returns the same shape (model/types.ts)
// in mock and real mode: mock reads __mock__/, real calls real-queue.ts and maps the answer, so the
// screen has a single render tree. Real mode also listens to the STOMP topic and falls back to polling.
import { useEffect, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { isMockMode } from "@/shared/api";
import { useMockMutation, useMockQuery } from "@/shared/api/mock-query";
import { subscribeJudge } from "@/shared/api/stomp";
import {
  fetchQueueJobs,
  fetchQueueSummary,
  mockControls,
} from "./__mock__/judge-queue-mocks";
import {
  readJobs,
  readQueue,
  saveQueueSettings,
  setQueueControl,
  summaryFromSnapshot,
  toJobsPage,
  toQueueSummary,
  type QueueSnapshot,
} from "./real-queue";
import { queueSettings } from "../model/settings";
import type {
  ClusterControls,
  QueueSettings,
  QueueSummary,
  QueueTab,
} from "../model/types";

const SPEC = "03-dd/api/F4_judge-orchestration.md";
export const QUEUE_KEY = ["queue-monitor"] as const;
const POLL_MS = 15_000;
const LIVE_STALE_MS = 60_000;

/**
 * Real mode: subscribes to the live topic and returns the poll interval to use while it is
 * disconnected or silent. Mock mode: nothing to subscribe to, never polls.
 */
export function useQueueLive(): number | false {
  const cache = useQueryClient();
  const [connected, setConnected] = useState(false);
  const [lastBatch, setLastBatch] = useState(0);
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    if (isMockMode()) return;
    const timer = setInterval(() => setNow(Date.now()), 10_000);
    const stop = subscribeJudge("/topic/admin/queue", {
      connected: () => {
        setConnected(true);
        setLastBatch(Date.now());
        void cache.invalidateQueries({ queryKey: QUEUE_KEY });
      },
      disconnected: () => setConnected(false),
      message: (value) => {
        const batch = value as {
          type?: string;
          snapshot?: QueueSnapshot;
          infraSummary?: { warnCount: number; errorCount: number };
        };
        if (
          batch?.type !== "QUEUE_BATCH" ||
          !batch.snapshot ||
          !batch.infraSummary
        )
          return;
        setLastBatch(Date.now());
        const fresh = summaryFromSnapshot(batch.snapshot, batch.infraSummary);
        cache.setQueryData<QueueSummary>(
          [...QUEUE_KEY, "summary"],
          (previous) => (previous ? { ...previous, ...fresh } : previous),
        );
        void cache.invalidateQueries({ queryKey: [...QUEUE_KEY, "jobs"] });
      },
    });
    return () => {
      clearInterval(timer);
      stop();
    };
  }, [cache]);
  return !isMockMode() && (!connected || now - lastBatch > LIVE_STALE_MS)
    ? POLL_MS
    : false;
}

export const useQueueSummary = (refetchInterval: number | false) =>
  useMockQuery({
    queryKey: [...QUEUE_KEY, "summary"],
    mock: fetchQueueSummary,
    real: async () => toQueueSummary(await readQueue()),
    spec: SPEC,
    staleTime: 0,
    refetchInterval,
  });

export function useQueueJobs(
  tab: QueueTab,
  page: number,
  direction: "asc" | "desc",
  refetchInterval: number | false,
) {
  // The mock pages by the saved rows-per-page, so it belongs in the key; the server owns it in real mode.
  const { jobsPerPage } = queueSettings.use();
  return useMockQuery({
    queryKey: [...QUEUE_KEY, "jobs", tab, page, direction, jobsPerPage],
    mock: () => fetchQueueJobs(tab, page, direction),
    real: async () => toJobsPage(await readJobs(tab, page)),
    spec: SPEC,
    staleTime: 0,
    keepPrevious: true,
    refetchInterval,
  });
}

type ControlChange = {
  key: keyof ClusterControls;
  value: boolean;
  /** The value the admin saw, so the server can refuse a stale change. */
  expected: boolean;
};

export const useQueueControl = () =>
  useMockMutation<ControlChange>({
    mock: ({ key, value }) => mockControls.set({ [key]: value }),
    real: async ({ key, value, expected }) => {
      await setQueueControl(key, value, expected);
    },
    spec: SPEC,
    invalidate: [QUEUE_KEY],
  });

export const useSaveQueueSettings = () =>
  useMockMutation<QueueSettings>({
    mock: (values) => queueSettings.set(values),
    real: async ({ autoscaleThreshold, ...rest }) => {
      await saveQueueSettings({
        autoscaleJobThreshold: autoscaleThreshold,
        ...rest,
      });
    },
    spec: SPEC,
    invalidate: [QUEUE_KEY],
  });
