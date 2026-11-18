// Data boundary of the user-management screen: `ListUsers` (#35) with the two counters (#62, #63)
// in real mode, the prototype page in mock mode (03-dd/api/F1_identity.md section 0.6). The view
// imports only these hooks.
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { isMockMode, withMockData } from "@/shared/api";
import { fetchAdminUserPage } from "./__mock__/admin-user-mocks";
import {
  realFetchAdminUserPage,
  type AdminUserListParams,
} from "./real-user-management";

export const ADMIN_USERS_KEY = ["admin-user-management"] as const;

/**
 * Real mode asks the server for each filter and page; the previous page stays on screen while the next
 * one loads, so typing in the search box does not blank the table. The mock page ignores the
 * parameters (the view filters it locally), so its key does not carry them either.
 */
export function useAdminUserPage(params: AdminUserListParams) {
  return useQuery({
    queryKey: isMockMode() ? ADMIN_USERS_KEY : [...ADMIN_USERS_KEY, params],
    queryFn: () =>
      withMockData(fetchAdminUserPage, () => realFetchAdminUserPage(params)),
    placeholderData: keepPreviousData,
    staleTime: 30_000,
    retry: false,
  });
}
