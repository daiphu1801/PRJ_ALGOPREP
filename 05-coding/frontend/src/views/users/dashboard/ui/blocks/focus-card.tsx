// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
// Khu vực E — derived from Khu vực B's data, no dedicated endpoint (Sheet 7.1 NO 13).
"use client";

import Link from "next/link";
import { useT } from "@/shared/i18n";
import { Card, EmptyState, Skeleton } from "@/shared/ui";
import {
  deriveFocusSuggestions,
  useTopicProgress,
  type ProgressRange,
} from "@/entities/progress";
import { pickWeakestTopic, scoreTopics } from "../../model/derive-skill-radar";

export function FocusCard({ range }: { range: ProgressRange }) {
  const t = useT("myProgress");
  const query = useTopicProgress(range);
  // The greeting's weakest topic is global (it ignores the range tab), so it reads the widest range too;
  // it is pinned first so the greeting and this block never name different topics.
  const allQuery = useTopicProgress("all");

  // Sheet 6 Khu vực E NO 2: same source as the topics table — hide together on error.
  if (query.isError) return null;

  if (query.isLoading || !query.data) {
    return (
      <Card title={t("focus.title")}>
        <div className="flex flex-col gap-2">
          {Array.from({ length: 3 }, (_, i) => (
            <Skeleton key={i} className="h-12" />
          ))}
        </div>
      </Card>
    );
  }

  const weakest = allQuery.data
    ? pickWeakestTopic(scoreTopics(allQuery.data))
    : null;
  const suggestions = deriveFocusSuggestions(
    query.data,
    new Date(),
    weakest ? { topicId: weakest.topicId, score: weakest.score } : null,
  );

  return (
    <Card title={t("focus.title")}>
      {suggestions.length === 0 ? (
        <EmptyState>{t("focus.allSolved")}</EmptyState>
      ) : (
        <div className="flex flex-col gap-2">
          {suggestions.map((s) => (
            <Link
              key={s.topicId}
              href={`/problems?topicId=${s.topicId}`}
              className="flex items-center gap-2.5 rounded-lg border border-[var(--color-border)] px-3 py-2.5 hover:border-[var(--color-primary)] hover:bg-[var(--color-surface-hover)]"
            >
              <span
                className="h-1.5 w-1.5 shrink-0 rounded-full bg-[var(--color-primary)]"
                aria-hidden="true"
              />
              <span className="min-w-0">
                <span className="block text-[13.5px] font-semibold">
                  {s.topicName}
                </span>
                <span className="mt-0.5 block text-xs text-[var(--color-text-muted)]">
                  {s.reason.code === "weakestSkill" &&
                    t("focus.reasonWeakest", { score: s.reason.score })}
                  {s.reason.code === "lowestAcRate" &&
                    t("focus.reasonLowestAc", { rate: s.reason.acRate })}
                  {s.reason.code === "mostUnsolved" &&
                    t("focus.reasonMostUnsolved", {
                      remaining: s.reason.remaining,
                      rate: s.reason.acRate,
                    })}
                  {s.reason.code === "leastRecentlySubmitted" &&
                    t("focus.reasonStale", { days: s.reason.daysSince })}
                </span>
              </span>
            </Link>
          ))}
        </div>
      )}
    </Card>
  );
}
