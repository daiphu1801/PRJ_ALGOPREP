// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 9.
//
// Layout follows 09-layoutBase/Admin - Cấu hình AI.dc.html: sticky header with the "Lưu và phát
// hành" CTA (:147-157), then a 1.55fr / minmax(300px,1fr) grid — prompts and rubric on the left
// (:159-222), rate limits, answer guards and the pre-release card on the right (:224-262).
//
// "Chạy đối chiếu" keeps its button and says nothing about running: PROTOTYPE_DEBT 2.8 settles that
// the UI stays while the backend is deferred to a later phase (risk R3, cost of batching 30 sample
// solutions through the AI). Wiring it now would be the one thing that decision rules out.
//
// The rubric total is shown and flagged when it is not 100%. The mockup does the same; whether the
// screen should refuse to publish at anything other than 100% is not settled anywhere, so it warns
// rather than blocks — carried into the phase report.
"use client";

import { useState } from "react";
import {
  GUARD_KEYS,
  RATE_LIMIT_KEYS,
  RUBRIC_KEYS,
  fetchAiConfigPage,
  totalWeight,
  type AiConfigPage,
  type GuardKey,
  type PromptStatus,
  type RubricKey,
} from "@/entities/ai-config";
import { useT } from "@/shared/i18n";
import {
  Badge,
  Button,
  Card,
  Modal,
  NoticeTile,
  NumberStepper,
  PageHeader,
  ProgressBar,
  SettingRow,
  Toggle,
  type BadgeVariant,
} from "@/shared/ui";

// dc.html:414-417.
const STATUS_VARIANT: Record<PromptStatus, BadgeVariant> = {
  live: "success",
  draft: "warn",
  abTest: "blue",
};

const RUBRIC_COLOR_VAR: Record<RubricKey, string> = {
  correctness: "--color-admin-teal",
  complexity: "--color-admin-warn",
  codeQuality: "--color-admin-cyan",
  edgeCases: "--color-admin-warn",
  explanation: "--color-admin-negative",
};

export function AdminAiConfigView() {
  const t = useT("adminAiConfig");
  const [page, setPage] = useState<AiConfigPage>(fetchAiConfigPage);
  const [historyOpen, setHistoryOpen] = useState(false);

  const total = totalWeight(page.rubricWeights);
  const weightsBalanced = total === 100;

  function setWeight(key: RubricKey, value: number) {
    setPage((previous) => ({
      ...previous,
      rubricWeights: { ...previous.rubricWeights, [key]: value },
    }));
  }

  function setGuard(key: GuardKey, value: boolean) {
    setPage((previous) => ({ ...previous, guards: { ...previous.guards, [key]: value } }));
  }

  return (
    <div>
      <PageHeader
        title={t("title")}
        description={t("subtitle")}
        actions={
          <Button variant="cta" size="sm">
            {t("publish")}
          </Button>
        }
      />

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-[minmax(0,1.55fr)_minmax(300px,1fr)]">
        <div className="flex min-w-0 flex-col gap-4">
          <Card
            title={t("promptsTitle")}
            description={t("promptsSubtitle")}
            action={
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setHistoryOpen(true)}
                className="border border-[var(--color-border)]"
              >
                {t("versionLog")}
              </Button>
            }
          >
            <div className="flex flex-col gap-2.5">
              {page.prompts.map((prompt) => (
                <div
                  key={prompt.feature}
                  className="glass-surface rounded-2xl border border-[var(--color-border)] px-3.5 py-3.5"
                >
                  <div className="mb-1.5 flex flex-wrap items-center gap-2.5">
                    <span className="text-[13.5px] font-semibold">
                      {t(`feature.${prompt.feature}`)}
                    </span>
                    <span className="rounded-md bg-[var(--color-track)] px-1.5 py-0.5 font-mono text-[11px] text-[var(--color-text-muted)]">
                      {prompt.version}
                    </span>
                    <Badge variant={STATUS_VARIANT[prompt.status]}>
                      {t(`promptStatus.${prompt.status}`)}
                    </Badge>
                    <Button variant="ghost" size="sm" className="ml-auto">
                      {t("editPrompt")}
                    </Button>
                  </div>
                  <p className="mb-2.5 text-[12.5px] leading-relaxed text-pretty text-[var(--color-text-muted)]">
                    {t(`promptDescription.${prompt.feature}`)}
                  </p>
                  <p className="flex flex-wrap gap-4 text-xs text-[var(--color-text-subtle)]">
                    <span>
                      {t("metaModel")}{" "}
                      <span className="font-mono text-[var(--color-text-muted)]">{prompt.model}</span>
                    </span>
                    <span>
                      {t("metaTemperature")}{" "}
                      <span className="font-mono text-[var(--color-text-muted)]">
                        {prompt.temperature}
                      </span>
                    </span>
                    <span>
                      {t("metaMaxTokens")}{" "}
                      <span className="font-mono text-[var(--color-text-muted)]">
                        {prompt.maxTokens}
                      </span>
                    </span>
                    <span>{t("metaUpdated", { date: prompt.updatedAt })}</span>
                  </p>
                </div>
              ))}
            </div>
          </Card>

          <Card
            title={t("rubricTitle")}
            description={t("rubricSubtitle")}
            action={
              <span
                className="font-mono text-[12.5px]"
                style={{
                  color: weightsBalanced
                    ? "var(--color-text-muted)"
                    : "var(--color-admin-negative)",
                }}
              >
                {t("weightTotal", { total })}
              </span>
            }
          >
            {weightsBalanced ? null : (
              <NoticeTile tone="warn" title={t("weightWarningTitle")} className="mb-3.5">
                {t("weightWarningBody", { total })}
              </NoticeTile>
            )}
            <div className="flex flex-col gap-3.5">
              {RUBRIC_KEYS.map((key) => (
                <div key={key}>
                  <div className="mb-1.5 flex items-center justify-between gap-3">
                    <span className="min-w-0">
                      <span className="text-[13.5px] font-semibold">{t(`rubric.${key}.label`)}</span>
                      <span className="ml-2 text-xs text-[var(--color-text-subtle)]">
                        {t(`rubric.${key}.meta`)}
                      </span>
                    </span>
                    <NumberStepper
                      value={page.rubricWeights[key]}
                      onValueChange={(value) => setWeight(key, value)}
                      label={t("weightLabel", { criterion: t(`rubric.${key}.label`) })}
                      format={(value) => `${value}%`}
                      decrementLabel={t("weightDecrement", { criterion: t(`rubric.${key}.label`) })}
                      incrementLabel={t("weightIncrement", { criterion: t(`rubric.${key}.label`) })}
                    />
                  </div>
                  <ProgressBar
                    value={page.rubricWeights[key]}
                    height={8}
                    label={t(`rubric.${key}.label`)}
                    fill={`var(${RUBRIC_COLOR_VAR[key]})`}
                  />
                </div>
              ))}
            </div>
          </Card>
        </div>

        <div className="flex min-w-0 flex-col gap-4">
          <Card title={t("limitsTitle")} description={t("limitsSubtitle")}>
            <div className="flex flex-col gap-2.5">
              {RATE_LIMIT_KEYS.map((key) => (
                <SettingRow
                  key={key}
                  label={t(`limit.${key}.label`)}
                  description={t(`limit.${key}.meta`)}
                >
                  <span className="font-mono text-sm font-semibold whitespace-nowrap">
                    {page.rateLimits[key]}
                  </span>
                </SettingRow>
              ))}
            </div>
          </Card>

          <Card title={t("guardsTitle")}>
            <div className="flex flex-col gap-2.5">
              {GUARD_KEYS.map((key) => (
                <SettingRow
                  key={key}
                  label={t(`guard.${key}.label`)}
                  description={t(`guard.${key}.meta`)}
                >
                  <Toggle
                    checked={page.guards[key]}
                    onCheckedChange={(value) => setGuard(key, value)}
                    label={t(`guard.${key}.label`)}
                  />
                </SettingRow>
              ))}
            </div>
          </Card>

          <Card title={t("preReleaseTitle")}>
            <p className="mb-3 text-[13px] leading-relaxed text-pretty text-[var(--color-text-muted)]">
              {t("preReleaseBody")}
            </p>
            {/* Deliberately inert — PROTOTYPE_DEBT 2.8 defers the backend, so the button shows the
                intent without pretending to run anything. */}
            <Button
              variant="ghost"
              size="sm"
              disabled
              className="w-full border border-[var(--color-border)]"
            >
              {t("runComparison")}
            </Button>
            <p className="mt-3 text-[12.5px] text-[var(--color-text-subtle)]">
              {t("lastComparison", {
                date: page.lastComparison.date,
                delta: page.lastComparison.averageDelta,
              })}
            </p>
          </Card>
        </div>
      </div>

      <Modal
        open={historyOpen}
        onClose={() => setHistoryOpen(false)}
        title={t("versionLog")}
        footer={
          <Button variant="ghost" size="sm" onClick={() => setHistoryOpen(false)}>
            {t("close")}
          </Button>
        }
      >
        <ul className="flex flex-col gap-2">
          {page.versionHistory.map((entry) => (
            <li
              key={entry.id}
              className="flex items-center gap-3 rounded-xl border border-[var(--color-border)] px-3 py-2"
            >
              <span className="min-w-0 flex-1 text-[13px] font-semibold text-[var(--color-text)]">
                {t(`feature.${entry.feature}`)}
              </span>
              <span className="font-mono text-[11.5px]">{entry.version}</span>
              <span className="font-mono text-[11.5px] text-[var(--color-text-subtle)]">
                {entry.publishedAt} · {entry.author}
              </span>
            </li>
          ))}
        </ul>
      </Modal>
    </div>
  );
}
