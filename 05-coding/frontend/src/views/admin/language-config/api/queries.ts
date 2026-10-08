// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Data boundary of the language-and-limits screen. Same shape as the other screens:
// `withMockData(mock, fetchReal)` switched by NEXT_PUBLIC_MOCK_DATA, `fetchReal` still a
// NOT_IMPLEMENTED stub until 03-dd/api/judge-orchestration.md defines `GetLanguageConfigs` and
// `UpdateLanguageConfigs`. The view imports only these hooks.
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError, withMockData } from "@/shared/api";
import {
  loadLanguageConfigPage,
  saveLanguageConfigPage,
} from "./__mock__/language-config-mocks";
import type { LanguageConfigPage } from "../model/types";

function notImplemented(): never {
  throw new ApiError(
    "NOT_IMPLEMENTED",
    501,
    "03-dd/api/judge-orchestration.md chưa định nghĩa endpoint này",
  );
}

const KEY = ["language-config"] as const;

/** `GetLanguageConfigs`. Stale at once: a reopened screen must show what was last saved. */
export const useLanguageConfigPage = () =>
  useQuery({
    queryKey: KEY,
    queryFn: () => withMockData(loadLanguageConfigPage, notImplemented),
    staleTime: 0,
    retry: false,
  });

/** `UpdateLanguageConfigs`: one call writes the whole page (BD ADM0501 EVT-5). */
export function useSaveLanguageConfigPage() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (page: LanguageConfigPage) =>
      withMockData(() => saveLanguageConfigPage(page), notImplemented),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: KEY }),
  });
}
