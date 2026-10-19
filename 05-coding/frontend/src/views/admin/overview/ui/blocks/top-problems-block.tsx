// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import Link from "next/link";
import { useT } from "@/shared/i18n";
import { textTone } from "@/shared/lib";
import { DashboardBlockState, RankedProgressList } from "@/shared/ui";
import { useTopProblems } from "../../api";

// Only block on this screen with a link onward — to problem_management
// (02-bd/screens/admin/ADM0101_overview.md section 2 point 5, DEC-2026-0831-admin-overview-ui-decisions).
export function TopProblemsBlock() {
  const t = useT("adminOverview");
  const query = useTopProblems();

  return (
    <DashboardBlockState
      title={t("blocks.topProblems")}
      isLoading={query.isLoading}
      isError={query.isError}
      isEmpty={query.data ? query.data.items.length === 0 : false}
      onRetry={() => query.refetch()}
      emptyMessage={t("emptyGeneric")}
      errorMessage={t("errorGeneric")}
      retryLabel={t("retry")}
      // BD ADM0101 Sheet 6 (:527): "Xem tất cả" stays visible when the block is empty or failed,
      // so it lives in the always-rendered footer rather than inside the data branch (UX review B5).
      footer={
        <Link
          href="/admin/problems"
          className="mt-3 inline-block text-xs font-medium hover:underline"
          style={{ color: textTone("--color-admin-cyan") }}
        >
          {t("blocks.viewAll")}
        </Link>
      }
    >
      {query.data && (
        <RankedProgressList
          items={query.data.items}
          accentColorVar="--color-admin-cyan"
        />
      )}
    </DashboardBlockState>
  );
}
