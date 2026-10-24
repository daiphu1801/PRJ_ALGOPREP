// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Data boundary of the AI config screen (ADM0301). 03-dd/api has no F5_ai-review.md yet, so the
// endpoint is still undefined; mock mode serves the local page.
import { useMockQuery } from "@/shared/api/mock-query";
import { fetchAiConfigPage } from "./__mock__/ai-config-mocks";

/** Stale at once: the screen seeds editable local state from it, a reopen must start fresh. */
export const useAiConfigPage = () =>
  useMockQuery({
    queryKey: ["ai-config"],
    mock: fetchAiConfigPage,
    spec: "03-dd/api (F5_ai-review.md chưa có)",
    staleTime: 0,
  });
