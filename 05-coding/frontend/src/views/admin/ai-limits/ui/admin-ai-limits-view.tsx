// BD ADM0303 (admin_ai_limits): limits and cost of the AI, split out of admin_ai_config on 2026-10-10
// (DEC-2026-1010-split-ai-admin-screens). Every control writes immediately, so there is no publish
// step here. Each card has its own query and its own error state (DD ADM0303 section 3): F5 #18 to
// #26 for budget, limits and prices (entities/ai-budget), F2 #56 and #57 for the testcase cap.
//
// ponytail: no role split in the UI yet. The monthly cap and the unlock need AI_TOKEN_BUDGET, the other
// cards AI_CONFIG (BD ADM0303 section 4.1); the backend enforces it and answers AIR-301, which the
// cards show as a toast.
"use client";

import Link from "next/link";
import { useT } from "@/shared/i18n";
import { PageHeader } from "@/shared/ui";
import { BudgetCard } from "./budget-card";
import { CostCard } from "./cost-card";
import { RateLimitsCard } from "./rate-limits-card";
import { TestcaseGenerationLimitCard } from "./testcase-generation-limit-card";

export function AdminAiLimitsView() {
  const t = useT("adminAiLimits");

  return (
    <div>
      <PageHeader
        title={t("title")}
        description={t("subtitle")}
        actions={
          <Link
            href="/admin/ai-usage"
            className="text-[13px] font-semibold text-[var(--color-primary)] hover:underline"
          >
            {t("linkUsage")}
          </Link>
        }
      />

      <div className="flex flex-col gap-4">
        <BudgetCard />
        <RateLimitsCard />
        <CostCard />
        <TestcaseGenerationLimitCard />
      </div>
    </div>
  );
}
