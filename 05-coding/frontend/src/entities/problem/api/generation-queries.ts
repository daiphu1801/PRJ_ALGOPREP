import { useMutation, useQueryClient } from "@tanstack/react-query";
import { api, useMockQuery, withMockData } from "@/shared/api";
import { aiGenerationSettings } from "../model/generation-settings-store";
const KEY = ["problem-bank", "ai-generation-settings"] as const;
const PATH = "/problem-bank/settings/ai-testcase-generation";
export function useTestcaseGenerationSettings() {
  return useMockQuery({
    queryKey: KEY,
    mock: () => aiGenerationSettings.get(),
    real: () => api.get<{ maxPerProblem: number }>(PATH),
    spec: "03-dd/api/F2_problem-bank.md",
    staleTime: 0,
  });
}
export function useUpdateTestcaseGenerationSettings() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (maxPerProblem: number) =>
      withMockData(
        () => {
          aiGenerationSettings.set({ maxPerProblem });
          return { maxPerProblem };
        },
        () => api.put<{ maxPerProblem: number }>(PATH, { maxPerProblem }),
      ),
    onSuccess: (result) => {
      client.setQueryData(KEY, result);
      void client.invalidateQueries({ queryKey: ["problem-draft"] });
    },
  });
}
