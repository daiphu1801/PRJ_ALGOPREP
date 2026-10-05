// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Data boundary of the problem list. Same shape as the other screens: `withMockData(mock, fetchReal)`
// switched by NEXT_PUBLIC_MOCK_DATA, `fetchReal` a NOT_IMPLEMENTED stub until
// 03-dd/api/problem-bank.md defines `ListProblemsAdmin`. The view passes no author: the server scopes
// A2 to their own problems from the token (BD SHR0201 Q1).
import { useQuery } from "@tanstack/react-query";
import { ApiError, withMockData } from "@/shared/api";
import { loadAdminProblemPage } from "./__mock__/admin-problem-mocks";

function notImplemented(): never {
  throw new ApiError("NOT_IMPLEMENTED", 501, "03-dd/api/problem-bank.md chưa định nghĩa endpoint này");
}

export const useAdminProblemPage = () =>
  useQuery({
    queryKey: ["admin-problem-page"],
    queryFn: () => withMockData(loadAdminProblemPage, notImplemented),
    staleTime: 0,
    retry: false,
  });
