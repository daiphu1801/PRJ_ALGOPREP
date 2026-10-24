// PROTOTYPE — chưa có DD. Xem 06-plan/PROTOTYPE_DEBT.md mục 9.
//
// Trang đầu của Workspace (/workspace, mục "Workspace" trên thanh điều hướng): trước đây là một lệnh
// redirect thẳng vào bài làm dở gần nhất. Owner 2026-10-20 muốn có trang riêng: liệt kê bài đang làm dở
// để tiếp tục, hoặc đi chọn bài. Ghi đè nhánh "mở thẳng nháp" của _shell.md mục 6 Q2.
//
// PROTOTYPE — danh sách lấy từ mock; nguồn thật là `judge.submissions` khi có 03-dd/api/F4_judge-orchestration.md.
"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { Shuffle } from "lucide-react";
import {
  InProgressList,
  useProblemListPage,
  type ProblemListPage,
} from "@/entities/problem";
import { useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import { Button, Card, QueryBoundary } from "@/shared/ui";

// Owner 2026-10-20: at most 12 drafts, most recently submitted first (mock order stands in for that).
const MAX_DRAFTS = 12;

export function WorkspaceHomeView() {
  const query = useProblemListPage();
  return (
    <QueryBoundary query={query}>
      {(page) => <WorkspaceHome page={page} />}
    </QueryBoundary>
  );
}

function WorkspaceHome({ page }: { page: ProblemListPage }) {
  const t = useT("workspaceHome");
  const tl = useT("problemList");
  const router = useRouter();

  // ponytail: picks client-side from the mock; the real flow is server-side (GetRandomProblem with an
  // unsolved-status filter, USR0105 BD Q4). Unsolved includes "in progress".
  function pickRandom() {
    const todo = page.items.filter((problem) => problem.solveState !== "solved");
    const problem = todo[Math.floor(Math.random() * todo.length)];
    if (!problem) {
      toast.warning(t("toast.randomEmpty"));
      return;
    }
    router.push(`/problems/${problem.id}`);
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
          <Button variant="cta" size="sm" onClick={pickRandom}>
            <Shuffle className="mr-1.5 h-3.5 w-3.5" />
            {tl("summary.btnRandom")}
          </Button>
        </div>
      </div>

      <Card className="px-5 py-4">
        <h2 className="mb-3 text-[13px] font-semibold">{t("resumeTitle")}</h2>
        <InProgressList
          items={page.inProgress}
          limit={MAX_DRAFTS}
          layout="grid"
          emptyText={t("empty")}
        />
      </Card>
    </div>
  );
}
