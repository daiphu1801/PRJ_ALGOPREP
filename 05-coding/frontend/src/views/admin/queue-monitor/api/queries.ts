// PROTOTYPE — no DD endpoint wired yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Data boundary of the queue-monitor screen: `GetQueueSnapshot` + `ListQueueJobs`
// (03-dd/api/F4_judge-orchestration.md, rows 13-14). Cluster controls stay local state in the view.
import { useMockQuery } from "@/shared/api/mock-query";
import { fetchQueuePage } from "./__mock__/judge-queue-mocks";

export const useQueuePage = () =>
  useMockQuery({
    queryKey: ["queue-monitor"],
    mock: fetchQueuePage,
    spec: "03-dd/api/F4_judge-orchestration.md",
    staleTime: 0,
  });
