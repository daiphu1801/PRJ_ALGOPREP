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
import { ChartNoAxesColumn } from "lucide-react";
import { useT } from "@/shared/i18n";
import { Button, PageHeader } from "@/shared/ui";
import { BudgetCard } from "./budget-card";
import { CostCard } from "./cost-card";
import { RateLimitsCard } from "./rate-limits-card";
import { TestcaseGenerationLimitCard } from "./testcase-generation-limit-card";

export function AdminAiLimitsView() {
  const t = useT("adminAiLimits");

  return (
    <div className="@container w-full min-w-0">
      <PageHeader
        title={t("title")}
        description={t("subtitle")}
        className="[&_h1]:whitespace-normal [&_p]:whitespace-normal"
        actions={
          <Button
            asChild
            variant="ghost"
            size="sm"
            className="min-h-9 gap-2 border border-[var(--color-border)]"
          >
            <Link href="/admin/ai-usage">
              <ChartNoAxesColumn size={15} aria-hidden="true" />
              {t("linkUsage")}
            </Link>
          </Button>
        }
      />

      <div className="grid min-w-0 gap-4 @min-[820px]:grid-cols-2">
        <div className="flex min-w-0 flex-col gap-4">
          <BudgetCard />
          <CostCard />
        </div>
        <div className="flex min-w-0 flex-col gap-4">
          <RateLimitsCard />
          <TestcaseGenerationLimitCard />
        </div>
      </div>
    </div>
  );
}
