// PROTOTYPE — chưa có DD. Xem 06-plan/PROTOTYPE_DEBT.md mục 9.
//
// Trang đầu của Workspace (/workspace, mục "Workspace" trên thanh điều hướng): trước đây là một lệnh
// redirect thẳng vào bài làm dở gần nhất. Owner 2026-10-20 muốn có trang riêng: liệt kê bài đang làm dở
// để tiếp tục, hoặc đi chọn bài. Ghi đè nhánh "mở thẳng nháp" của _shell.md mục 6 Q2.
//
// Danh sách bài đang làm dở lấy từ F4 #8 (`useMyInProgress`); nút ngẫu nhiên do máy chủ chọn (F2 #2).
"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { Shuffle } from "lucide-react";
import {
  InProgressList,
  useMyInProgress,
  useRandomProblem,
  type CatalogParams,
  type InProgressProblem,
} from "@/entities/problem";
import { useApiErrorMessage } from "@/shared/i18n/use-api-error-message";
import { useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import { Button, Card, QueryBoundary } from "@/shared/ui";

// Owner 2026-10-20: at most 12 drafts, most recently submitted first (the server orders them).
const MAX_DRAFTS = 12;

// "Unsolved" includes "in progress": ask for a never-tried problem first, then an attempted one.
const RANDOM_BASE: CatalogParams = {
  q: "",
  status: "todo",
  difficulty: "all",
  topicId: "all",
  sort: "difficulty",
  dir: "asc",
  page: 1,
  pageSize: 12,
};

export function WorkspaceHomeView() {
  const query = useMyInProgress(MAX_DRAFTS);
  return (
    <QueryBoundary query={query}>
      {(inProgress) => <WorkspaceHome inProgress={inProgress} />}
    </QueryBoundary>
  );
}

function WorkspaceHome({ inProgress }: { inProgress: InProgressProblem[] }) {
  const t = useT("workspaceHome");
  const tl = useT("problemList");
  const router = useRouter();

  const random = useRandomProblem();
  const apiMessage = useApiErrorMessage();

  async function pickRandom() {
    try {
      const id =
        (await random.mutateAsync(RANDOM_BASE)) ??
        (await random.mutateAsync({ ...RANDOM_BASE, status: "attempted" }));
      if (id) router.push(`/problems/${id}`);
      else toast.warning(t("toast.randomEmpty"));
    } catch (error) {
      toast.error(apiMessage(error));
    }
  }

  return (
    <div className="p-6">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
        <div>
          <h1 className="text-lg font-bold tracking-tight">{t("title")}</h1>
          <p className="text-[12.5px] text-[var(--color-text-muted)]">
            {t("subtitle")}
          </p>
        </div>
        <div className="flex gap-2">
          <Button
            asChild
            variant="ghost"
            size="sm"
            className="border border-[var(--color-border)]"
          >
            <Link href="/problems">{t("linkProblems")}</Link>
          </Button>
          <Button variant="cta" size="sm" onClick={() => void pickRandom()}
            disabled={random.isPending}
          >
            <Shuffle className="mr-1.5 h-3.5 w-3.5" />
            {tl("summary.btnRandom")}
          </Button>
        </div>
      </div>

      <Card className="px-5 py-4">
        <h2 className="mb-3 text-[13px] font-semibold">{t("resumeTitle")}</h2>
        <InProgressList
          items={inProgress}
          limit={MAX_DRAFTS}
          layout="grid"
          emptyText={t("empty")}
        />
      </Card>
    </div>
  );
}
