// PROTOTYPE — no DD hook yet for the alert counter. See 06-plan/PROTOTYPE_DEBT.md
//
// Data boundary of the infrastructure event log (ADM0404). `ListInfraEvents` is defined in
// 03-dd/api/F4_judge-orchestration.md; until it is wired, mock mode serves the local rows.
import { useMockQuery } from "@/shared/api/mock-query";
import {
  countRecentInfraAlerts,
  fetchInfraEvents,
} from "./__mock__/infra-event-mocks";

const SPEC = "03-dd/api/F4_judge-orchestration.md";

/** `ListInfraEvents`. Stale at once: the "Làm mới" button refetches it. */
export const useInfraEvents = () =>
  useMockQuery({
    queryKey: ["infra-events"],
    mock: fetchInfraEvents,
    spec: SPEC,
    staleTime: 0,
  });

/** Warn/error counts of the newest day, for the queue screen's link row (no DD endpoint yet). */
export const useRecentInfraAlerts = () =>
  useMockQuery({
    queryKey: ["infra-events", "recent-alerts"],
    mock: countRecentInfraAlerts,
    spec: SPEC,
    staleTime: 0,
  });
