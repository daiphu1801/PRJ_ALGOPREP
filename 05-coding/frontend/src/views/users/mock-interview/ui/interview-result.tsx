// The scorecard of a finished session (02-bd/screens/users/USR0302_mock_interview.md, result state): the overall score
// ring beside the radar of the four criteria, a legend of the three score bands (Yếu / Khá / Tốt), one card per
// criterion tagged with its band, and the AI's written feedback cut into short paragraphs instead of one block.
"use client";

import Link from "next/link";
import {
  RUBRIC_CRITERION_CODES,
  RUBRIC_WEIGHTS,
  type InterviewResult,
} from "@/entities/mock-interview";
import { useT } from "@/shared/i18n";
import { Button, RadarChart } from "@/shared/ui";
import { splitIntoParagraphs } from "../model/format-feedback";
import {
  SCORE_LEVELS,
  SCORE_LEVEL_COLOR,
  scoreLevel,
} from "../model/score-level";

function LevelChip({ score }: { score: number }) {
  const t = useT("mockInterview");
  const level = scoreLevel(score);
  return (
    <span
      className="rounded-full px-2 py-0.5 text-[11.5px] font-semibold"
      style={{
        color: SCORE_LEVEL_COLOR[level],
        background: `color-mix(in srgb, ${SCORE_LEVEL_COLOR[level]} 14%, transparent)`,
      }}
    >
      {t(`scoreLevel.${level}`)}
    </span>
  );
}

function Paragraphs({ text, lead = false }: { text: string; lead?: boolean }) {
  const parts = splitIntoParagraphs(text);
  return (
    <div className="flex flex-col gap-2">
      {parts.map((part, index) => (
        <p
          key={index}
          className={
            lead && index === 0
              ? "text-[14px] leading-relaxed font-medium text-pretty"
              : "text-[13px] leading-relaxed text-pretty text-[var(--color-text-muted)]"
          }
        >
          {part}
        </p>
      ))}
    </div>
  );
}

export function InterviewResultScreen({
  result,
  onRetry,
}: {
  result: InterviewResult;
  /** Without it the card is read-only inside another screen: no padding, no retry and back buttons. */
  onRetry?: () => void;
}) {
  const t = useT("mockInterview");
  const overall = result.overallScore;
  const level = scoreLevel(overall);
  const tone = SCORE_LEVEL_COLOR[level];
  const criteria = RUBRIC_CRITERION_CODES.flatMap((code) => {
    const criterion = result.criteria.find((c) => c.code === code);
    return criterion ? [{ code, criterion }] : [];
  });
  return (
    <section className={onRetry ? "p-6" : undefined}>
      <div className="mb-4 grid gap-4 lg:[grid-template-columns:minmax(0,1fr)_380px]">
        <div className="glass-card flex flex-wrap items-start gap-6 border border-[var(--color-border)] p-6">
          <div className="flex shrink-0 flex-col items-center gap-2">
            <div
              className="flex h-28 w-28 flex-col items-center justify-center rounded-full border-[6px]"
              style={{ borderColor: tone, color: tone }}
            >
              <span className="text-3xl leading-none font-bold">{overall}</span>
              <span className="text-[12px] font-semibold">/ 10</span>
            </div>
            <LevelChip score={overall} />
          </div>
          <div className="min-w-0 flex-1">
            <p className="mb-1 text-xs font-semibold tracking-[0.08em] text-[var(--color-text-subtle)] uppercase">
              {t("resultOverall")}
            </p>
            <h2 className="mb-2 text-[15px] font-bold">
              {t("result.summaryTitle")}
            </h2>
            <Paragraphs text={result.feedbackSummary} lead />
          </div>
        </div>

        {criteria.length >= 3 ? (
          <div className="glass-card flex flex-col items-center justify-center border border-[var(--color-border)] p-4">
            <RadarChart
              points={criteria.map(({ code, criterion }) => ({
                // Short names on the radar: the full ones are too long for its axes and spill out of the card.
                label: t(`rubricShort.${code}`),
                value: criterion.score * 10,
              }))}
              ariaLabel={t("result.radarLabel")}
              emphasizeLowest
              className="max-w-[360px]"
            />
          </div>
        ) : null}
      </div>

      <ul
        aria-label={t("legend.title")}
        className="mb-3 flex flex-wrap items-center gap-x-4 gap-y-1 text-[12px] text-[var(--color-text-muted)]"
      >
        <li className="font-semibold">{t("legend.title")}</li>
        {SCORE_LEVELS.map((item) => (
          <li key={item} className="flex items-center gap-1.5">
            <span
              aria-hidden="true"
              className="h-2.5 w-2.5 rounded-full"
              style={{ background: SCORE_LEVEL_COLOR[item] }}
            />
            <span className="font-semibold">{t(`scoreLevel.${item}`)}</span>{" "}
            {t(`legend.${item}`)}
          </li>
        ))}
      </ul>

      <div className="mb-4 grid gap-3.5 sm:grid-cols-2">
        {criteria.map(({ code, criterion }) => (
          <div
            key={code}
            className="glass-card border border-[var(--color-border)] p-4"
            style={{
              borderLeft: `4px solid ${SCORE_LEVEL_COLOR[scoreLevel(criterion.score)]}`,
            }}
          >
            <div className="mb-2 flex flex-wrap items-center gap-2">
              <p className="text-[13.5px] font-semibold">
                {t(`rubric.${code}`)} (
                {criterion.weightPercent ?? RUBRIC_WEIGHTS[code]}%)
              </p>
              <LevelChip score={criterion.score} />
              <p className="ml-auto font-mono text-sm font-semibold">
                {criterion.score} / 10
              </p>
            </div>
            <div className="mb-2.5 h-1.5 overflow-hidden rounded-full bg-[var(--color-track)]">
              <div
                className="h-full rounded-full"
                style={{
                  width: `${criterion.score * 10}%`,
                  background: SCORE_LEVEL_COLOR[scoreLevel(criterion.score)],
                }}
              />
            </div>
            <Paragraphs text={criterion.comment} />
          </div>
        ))}
      </div>

      {onRetry ? (
        <div className="flex gap-3">
          <Button variant="cta" size="md" onClick={onRetry}>
            {t("retryCta")}
          </Button>
          <Button variant="ghost" size="md" asChild>
            <Link href="/problems">{t("backCta")}</Link>
          </Button>
        </div>
      ) : null}
    </section>
  );
}
