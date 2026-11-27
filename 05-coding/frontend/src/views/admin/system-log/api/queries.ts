// Data boundary of the system-log screen: the list (F1 #39) behind a keyset cursor, the two side
// blocks (#64, #65) and the retention (#51), real in real mode and the prototype data in mock mode.
// The mock honours the same filters and cursor, so the view has one code path for both
// (03-dd/api/F1_identity.md section 0.7).
import {
  keepPreviousData,
  useInfiniteQuery,
  useQuery,
} from "@tanstack/react-query";
import { withMockData } from "@/shared/api";
import type { AuditLogParams } from "../model/types";
import { logSettings } from "../model/settings";
import {
  fetchActiveAdmins,
  fetchAuditLogs,
  fetchCategoryBreakdown,
} from "./__mock__/audit-log-mocks";
import {
  realFetchActiveAdmins,
  realFetchAuditLogs,
  realFetchCategoryBreakdown,
  realFetchLogRetention,
} from "./real-audit-log";

export const AUDIT_LOG_KEY = ["admin-system-log"] as const;
export const AUDIT_LOG_LIST_KEY = [...AUDIT_LOG_KEY, "list"] as const;
export const LOG_RETENTION_KEY = ["admin-log-retention"] as const;

export const auditLogKey = (params: AuditLogParams) =>
  [...AUDIT_LOG_LIST_KEY, params] as const;

/** `enabled` is off while the filters are invalid (reversed range): nothing is asked of the server. */
export function useAuditLog(params: AuditLogParams, enabled: boolean) {
  return useInfiniteQuery({
    queryKey: auditLogKey(params),
    queryFn: ({ pageParam }) =>
      withMockData(
        () => fetchAuditLogs(params, pageParam),
        () => realFetchAuditLogs(params, pageParam),
      ),
    initialPageParam: null as string | null,
    getNextPageParam: (last) => last.nextCursor,
    placeholderData: keepPreviousData,
    enabled,
    staleTime: 30_000,
    retry: false,
  });
}

export const useActiveAdmins = () =>
  useQuery({
    queryKey: [...AUDIT_LOG_KEY, "active-admins"],
    queryFn: () => withMockData(fetchActiveAdmins, realFetchActiveAdmins),
    staleTime: 30_000,
    retry: false,
  });

export const useCategoryBreakdown = () =>
  useQuery({
    queryKey: [...AUDIT_LOG_KEY, "category-breakdown"],
    queryFn: () =>
      withMockData(fetchCategoryBreakdown, realFetchCategoryBreakdown),
    staleTime: 30_000,
    retry: false,
  });

export const useLogRetention = () =>
  useQuery({
    queryKey: LOG_RETENTION_KEY,
    queryFn: () =>
      withMockData(() => ({ ...logSettings.get() }), realFetchLogRetention),
    staleTime: 30_000,
    retry: false,
  });
