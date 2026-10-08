// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useState } from "react";
import Link from "next/link";
import { fetchProblemListPage } from "@/entities/problem";
import { useTopicProgress } from "@/entities/progress";
import {
  deriveSkillRadar,
  pickWeakestTopic,
} from "../../model/derive-skill-radar";
import { useT } from "@/shared/i18n";
import { Button, Skeleton } from "@/shared/ui";

/**
 * 09-layoutBase/Dashboard AlgoPrep.dc.html:99-108 — greeting on the left, two buttons on the right.
 *
 * Two deliberate departures from the prototype, both recorded in
 * 01-rd/screens/users/USR0601_dashboard.md:
 * - "Xem lộ trình" (dc.html:105) is NOT built (mục 4 Q1): no `Fx-nn` anywhere in 01-rd/req/
 *   describes a learning-roadmap feature and the prototype button has no destination, so building
 *   it would mean inventing a screen. Its slot is left to the single primary action.
 * - The name is a placeholder until `app/providers/auth-provider.tsx` bootstraps a real session —
 *   same call already made in AdminToolbar and the student header's avatar.
 *
 * The weakest topic is derived, never fetched (REQ-04 + mục 4 Q2): `deriveSkillRadar` scores each
 * topic from AC rate and coverage, `pickWeakestTopic` takes the minimum. That keeps this line and
 * the radar block below telling the SAME story — computing "weakest" separately here is how the
 * two would end up naming different topics.
 */
export function GreetingBlock() {
  const t = useT("dashboard");
  // The greeting's weakest-topic callout is a global statement, not a windowed one, so it reads the
  // widest range rather than following any tab.
  const topicsQuery = useTopicProgress("all");
  const radar = topicsQuery.data ? deriveSkillRadar(topicsQuery.data) : [];
  const weakest = pickWeakestTopic(radar);

  // Resume target per REQ-03: the most recent unfinished draft, falling back to the problem list
  // when there is none, so the primary button is never a dead end for a new account.
  const [{ inProgress }] = useState(fetchProblemListPage);
  const resumeHref = inProgress[0]
    ? `/problems/${inProgress[0].problemId}`
    : "/problems";

  return (
    <div className="mb-5 flex flex-wrap items-end justify-between gap-6">
      <div className="min-w-0">
        <h2 className="mb-1.5 text-[22px] font-bold tracking-[-0.02em]">
          {t("greeting.title")}
        </h2>
        {topicsQuery.isLoading ? (
          <Skeleton className="h-5 w-[320px]" />
        ) : weakest ? (
          <p className="text-sm text-[var(--color-text-muted)]">
            {t("greeting.weakestTopic", {
              topic: weakest.topicName,
              rate: weakest.score,
              count: radar.length,
            })}
          </p>
        ) : (
          // REQ-08: a brand-new account has no topic data — say so instead of naming a fake weak spot.
          <p className="text-sm text-[var(--color-text-muted)]">
            {t("greeting.noData")}
          </p>
        )}
      </div>

      <Button asChild variant="cta" size="sm">
        <Link href={resumeHref}>
          {inProgress[0] ? t("greeting.btnResume") : t("greeting.btnStart")}
        </Link>
      </Button>
    </div>
  );
}
