// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Data boundary of the AI usage screen (ADM0302). 03-dd/api has no F5_ai-review.md yet, so the
// endpoint is still undefined; mock mode serves the local page.
import { useMockQuery } from "@/shared/api/mock-query";
import { fetchAiUsagePage } from "./__mock__/ai-usage-mocks";

export const useAiUsagePage = () =>
  useMockQuery({
    queryKey: ["ai-usage"],
    mock: fetchAiUsagePage,
    spec: "03-dd/api (F5_ai-review.md chưa có)",
  });
