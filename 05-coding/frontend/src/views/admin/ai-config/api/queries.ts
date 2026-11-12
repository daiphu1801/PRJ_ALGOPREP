// Data boundary of the AI config screen (ADM0301). Mock mode serves the local page; real mode reads
// F5 #14 + #15 (03-dd/api/F5_ai-review.md).
import { useMockQuery } from "@/shared/api/mock-query";
import { fetchAiConfigPage } from "./__mock__/ai-config-mocks";
import { realGetAiConfigPage } from "./real-prompts";

/** Query key of the page; both mutations invalidate this prefix. */
export const AI_CONFIG_KEY = ["ai-config"] as const;

/** Stale at once: the screen seeds editable local state from it, a reopen must start fresh. */
export const useAiConfigPage = () =>
  useMockQuery({
    queryKey: AI_CONFIG_KEY,
    mock: fetchAiConfigPage,
    real: realGetAiConfigPage,
    spec: "03-dd/api/F5_ai-review.md",
    staleTime: 0,
  });
