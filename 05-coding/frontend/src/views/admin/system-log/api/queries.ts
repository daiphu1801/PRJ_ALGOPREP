// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Data boundary of the system-log screen. `fetchReal` is still NOT_IMPLEMENTED until
// 03-dd/api/F1_identity.md `ListSystemAuditLogs` (#39) is wired. The view imports only these hooks.
import { useMockMutation, useMockQuery } from "@/shared/api";
import {
  fetchAuditLogPage,
  fetchMoreAuditEvents,
} from "./__mock__/audit-log-mocks";

const SPEC = "03-dd/api/F1_identity.md";

/** `ListSystemAuditLogs`, first page. */
export const useAuditLogPage = () =>
  useMockQuery({
    queryKey: ["admin-system-log"],
    mock: fetchAuditLogPage,
    spec: SPEC,
  });

/** `ListSystemAuditLogs`, next batch; the variable is how many rows are already loaded. */
export const useLoadMoreAuditEvents = () =>
  useMockMutation({
    mock: (loaded: number) => fetchMoreAuditEvents(loaded),
    spec: SPEC,
  });
