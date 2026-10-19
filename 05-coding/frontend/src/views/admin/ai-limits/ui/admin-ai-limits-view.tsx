// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 29.
//
// BD ADM0303 (admin_ai_limits): limits and cost of the AI, split out of admin_ai_config on 2026-10-10
// (DEC-2026-1010-split-ai-admin-screens). Every control writes immediately, so there is no publish
// step here; that is the point of the split. State lives in module-level stores
// (`entities/ai-budget`, `entities/problem`) that `admin_ai_usage` and the authoring screen read.
//
// ponytail: no role split yet. The monthly cap and the unlock need AI_TOKEN_BUDGET, the other cards
// AI_CONFIG (BD ADM0303 section 4.1); the prototype shows everything editable.
"use client";

import Link from "next/link";
import { useState } from "react";
import {
  addModelPrice,
  aiBudgetSettings,
  aiRateLimits,
  aiTokenBudget,
  removeModelPrice,
  renameModelPrice,
  setModelPriceValue,
  useModelPrices,
} from "@/entities/ai-budget";
import {
  AI_GENERATION_LIMIT_MAX,
  AI_GENERATION_LIMIT_MIN,
  aiGenerationSettings,
} from "@/entities/problem";
import { useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  ManagedListDialog,
  NumberStepper,
  PageHeader,
  ProgressBar,
  SettingRow,
  TextField,
} from "@/shared/ui";

const RATE_LIMITS = [
  { key: "reviewPerHour", unit: "limit.unit.count" },
  { key: "interviewPerHour", unit: "limit.unit.sessions" },
  { key: "cooldownSeconds", unit: "limit.unit.seconds" },
] as const;

const numberFormat = new Intl.NumberFormat("vi-VN");

export function AdminAiLimitsView() {
  const t = useT("adminAiLimits");
  const budget = aiTokenBudget.use();
  const rateLimits = aiRateLimits.use();
  const budgetSettings = aiBudgetSettings.use();
  const generationSettings = aiGenerationSettings.use();
  const modelPrices = useModelPrices();
  const [limitDraft, setLimitDraft] = useState(String(budget.limitTokens));
  const [limitInvalid, setLimitInvalid] = useState(false);
  const [pricesOpen, setPricesOpen] = useState(false);
  const [unlockOpen, setUnlockOpen] = useState(false);

  const percentUsed = Math.min(
    100,
    Math.round((budget.usedTokens / budget.limitTokens) * 100),
  );
  const draftNumber = Number(limitDraft);
  const limitChanged = draftNumber !== budget.limitTokens;
  const canUnlock = budget.locked && budget.limitTokens > budget.usedTokens;

  function saveLimit() {
    // BD ADM0303 Sheet 9 NO 2: a positive whole number.
    if (!Number.isInteger(draftNumber) || draftNumber <= 0) {
      setLimitInvalid(true);
      toast.error(t("budget.errorLimit"));
      return;
    }
    setLimitInvalid(false);
    aiTokenBudget.set({
      limitTokens: draftNumber,
      // The server locks on the next call once usage reaches the cap; the mock does it here.
      ...(draftNumber <= budget.usedTokens ? { locked: true } : {}),
    });
    toast.success(t("budget.savedLimit"));
  }

  function unlock() {
    aiTokenBudget.set({ locked: false });
    setUnlockOpen(false);
    toast.success(t("budget.unlocked"));
  }

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
        <Card
          title={t("budget.title")}
          description={t("budget.subtitle")}
          action={
            <Badge variant={budget.locked ? "negative" : "success"}>
              {budget.locked
                ? t("budget.statusLocked")
                : t("budget.statusOpen")}
            </Badge>
          }
        >
          <div className="flex flex-col gap-3">
            <ProgressBar
              value={percentUsed}
              label={t("budget.usedLabel")}
              fill={
                percentUsed >= budgetSettings.warnPercent
                  ? "var(--color-admin-warn)"
                  : "var(--color-admin-teal)"
              }
            />
            <p className="text-[12.5px] text-[var(--color-text-muted)]">
              {t("budget.used", {
                used: numberFormat.format(budget.usedTokens),
                cap: numberFormat.format(budget.limitTokens),
              })}
            </p>
            <div className="flex flex-wrap items-end gap-3">
              <TextField
                label={t("budget.limitLabel")}
                type="number"
                min={1}
                value={limitDraft}
                invalid={limitInvalid}
                onChange={(event) => setLimitDraft(event.target.value)}
                wrapperClassName="w-56"
              />
              <Button
                size="sm"
                variant="cta"
                disabled={!limitChanged}
                onClick={saveLimit}
              >
                {t("budget.saveLimit")}
              </Button>
              {budget.locked ? (
                <Button
                  size="sm"
                  variant="ghost"
                  aria-disabled={!canUnlock || undefined}
                  className="border border-[var(--color-border)]"
                  onClick={() => {
                    if (!canUnlock) {
                      toast.warning(t("budget.unlockBlocked"));
                      return;
                    }
                    setUnlockOpen(true);
                  }}
                >
                  {t("budget.unlock")}
                </Button>
              ) : null}
            </div>
          </div>
        </Card>

        <Card title={t("limitsTitle")} description={t("limitsSubtitle")}>
          <div className="flex flex-col gap-2.5">
            {RATE_LIMITS.map(({ key, unit }) => (
              <SettingRow
                key={key}
                label={t(`limit.${key}.label`)}
                description={t(`limit.${key}.meta`)}
              >
                <NumberStepper
                  editable
                  value={rateLimits[key]}
                  onValueChange={(value) => aiRateLimits.set({ [key]: value })}
                  label={t(`limit.${key}.label`)}
                  min={1}
                  max={999}
                  step={1}
                  format={(value) => t(unit, { count: value })}
                  unitWidth="9ch"
                  decrementLabel={t("limit.decrement", {
                    name: t(`limit.${key}.label`),
                  })}
                  incrementLabel={t("limit.increment", {
                    name: t(`limit.${key}.label`),
                  })}
                />
              </SettingRow>
            ))}
          </div>
        </Card>

        <Card title={t("cost.title")} description={t("cost.subtitle")}>
          <div className="flex flex-col gap-2.5">
            <SettingRow
              label={t("cost.warnLabel")}
              description={t("cost.warnMeta")}
            >
              <NumberStepper
                editable
                value={budgetSettings.warnPercent}
                onValueChange={(warnPercent) =>
                  aiBudgetSettings.set({ warnPercent })
                }
                label={t("cost.warnLabel")}
                min={1}
                max={100}
                step={5}
                format={(value) => `${value}%`}
                decrementLabel={t("cost.warnDecrement")}
                incrementLabel={t("cost.warnIncrement")}
              />
            </SettingRow>
            <SettingRow
              label={t("cost.pricesLabel")}
              description={t("cost.pricesMeta")}
            >
              <Button
                variant="ghost"
                size="sm"
                className="border border-[var(--color-border)]"
                onClick={() => setPricesOpen(true)}
              >
                {t("cost.manage")}
              </Button>
            </SettingRow>
          </div>
        </Card>

        <Card
          title={t("testcaseGen.title")}
          description={t("testcaseGen.subtitle")}
        >
          <SettingRow
            label={t("testcaseGen.maxLabel")}
            description={t("testcaseGen.maxMeta")}
          >
            <NumberStepper
              editable
              value={generationSettings.maxPerProblem}
              onValueChange={(maxPerProblem) =>
                aiGenerationSettings.set({ maxPerProblem })
              }
              label={t("testcaseGen.maxLabel")}
              min={AI_GENERATION_LIMIT_MIN}
              max={AI_GENERATION_LIMIT_MAX}
              step={1}
              format={(value) => t("testcaseGen.value", { count: value })}
              decrementLabel={t("testcaseGen.decrement")}
              incrementLabel={t("testcaseGen.increment")}
            />
          </SettingRow>
        </Card>
      </div>

      <ConfirmDialog
        open={unlockOpen}
        onClose={() => setUnlockOpen(false)}
        onConfirm={unlock}
        title={t("budget.unlockTitle")}
        confirmLabel={t("budget.unlockConfirm")}
        cancelLabel={t("budget.unlockCancel")}
      >
        {t("budget.unlockBody", {
          used: numberFormat.format(budget.usedTokens),
          cap: numberFormat.format(budget.limitTokens),
        })}
      </ConfirmDialog>

      <ManagedListDialog
        open={pricesOpen}
        onClose={() => setPricesOpen(false)}
        items={modelPrices}
        usage={{}}
        onAdd={addModelPrice}
        onRename={renameModelPrice}
        onRemove={removeModelPrice}
        renderExtra={(item) => (
          <TextField
            label={t("cost.dialog.priceLabel", { name: item.label })}
            hideLabel
            type="number"
            min={0}
            step="0.1"
            value={String(item.usdPerMillionTokens)}
            onChange={(event) =>
              setModelPriceValue(
                item.key,
                Math.max(0, Number(event.target.value) || 0),
              )
            }
            wrapperClassName="w-24"
          />
        )}
        labels={{
          title: t("cost.dialog.title"),
          hint: t("cost.dialog.hint"),
          nameLabel: t("cost.dialog.nameLabel"),
          newLabel: t("cost.dialog.newLabel"),
          newPlaceholder: t("cost.dialog.newPlaceholder"),
          add: t("cost.dialog.add"),
          save: t("cost.dialog.save"),
          delete: t("cost.dialog.delete"),
          close: t("cost.dialog.close"),
          usage: () => "",
          deleteBlocked: () => "",
          error: {
            empty: t("cost.dialog.errorEmpty"),
            duplicate: t("cost.dialog.errorDuplicate"),
          },
          done: {
            add: t("cost.dialog.doneAdd"),
            rename: t("cost.dialog.doneRename"),
            remove: t("cost.dialog.doneRemove"),
          },
        }}
      />
    </div>
  );
}
