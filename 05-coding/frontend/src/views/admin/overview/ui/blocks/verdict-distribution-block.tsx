// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useT } from "@/shared/i18n";
import { DashboardBlockState, HalfDonutGauge } from "@/shared/ui";
import { useVerdictDistribution } from "../../api";
import { translateLabel } from "../../model/labels";

export function VerdictDistributionBlock() {
  const t = useT("adminOverview");
  const query = useVerdictDistribution();

  return (
    <DashboardBlockState
      className="@min-[880px]:flex @min-[880px]:flex-1 @min-[880px]:flex-col"
      title={t("blocks.verdictDistribution")}
      isLoading={query.isLoading}
      isError={query.isError}
      isEmpty={
        query.data ? query.data.slices.every((s) => s.value === 0) : false
      }
      onRetry={() => query.refetch()}
      emptyMessage={t("emptyGeneric")}
      errorMessage={t("errorGeneric")}
      retryLabel={t("retry")}
      minHeightClassName="min-h-[176px] @min-[1120px]:[@media(max-height:850px)]:min-h-32"
    >
      {query.data && (
        <div className="@min-[880px]:flex @min-[880px]:flex-1 @min-[880px]:flex-col @min-[880px]:justify-center @min-[1120px]:[@media(max-height:900px)]:[&>div]:grid @min-[1120px]:[@media(max-height:900px)]:[&>div]:grid-cols-[auto_minmax(0,1fr)] @min-[1120px]:[@media(max-height:900px)]:[&>div]:items-center @min-[1120px]:[@media(max-height:900px)]:[&>div]:gap-4">
          <HalfDonutGauge
            slices={query.data.slices.map((s) => translateLabel(t, s))}
            chartClassName="max-w-[clamp(8rem,12cqw,12rem)] @min-[880px]:max-w-[min(clamp(8rem,12cqw,12rem),clamp(5.5rem,calc(calc(35*var(--dvh1))-10.5rem),12rem))]"
            fullCircle
          />
        </div>
      )}
    </DashboardBlockState>
  );
}
