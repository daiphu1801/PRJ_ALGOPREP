// ADM0303 Khu vực B: monthly cap, lock and unlock (F5 #18 to #20). Writes go to the server one action at
// a time; the bar, the badge and the lock come from the server's answer, never from the typed value.
"use client";

import { useState } from "react";
import {
  MAX_LIMIT_TOKENS,
  useSetTokenLimit,
  useTokenBudget,
  useUnlockTokenBudget,
  type TokenBudget,
} from "@/entities/ai-budget";
import { useApiErrorMessage, useT } from "@/shared/i18n";
import { BlockError } from "./block-error";
import { toast } from "@/shared/lib/toast-store";
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  ProgressBar,
  Skeleton,
  TextField,
} from "@/shared/ui";

const numberFormat = new Intl.NumberFormat("vi-VN");

export function BudgetCard() {
  const t = useT("adminAiLimits");
  const query = useTokenBudget();

  if (query.isError || !query.data) {
    return (
      <Card title={t("budget.title")} description={t("budget.subtitle")}>
        {query.isError ? (
          <BlockError query={query} />
        ) : (
          <Skeleton className="h-24 w-full" />
        )}
      </Card>
    );
  }
  return <BudgetBody budget={query.data} />;
}

function BudgetBody({ budget }: { budget: TokenBudget }) {
  const t = useT("adminAiLimits");
  const toMessage = useApiErrorMessage();
  const setLimit = useSetTokenLimit();
  const unlock = useUnlockTokenBudget();
  const [limitDraft, setLimitDraft] = useState(String(budget.limitTokens));
  const [limitInvalid, setLimitInvalid] = useState(false);
  const [unlockOpen, setUnlockOpen] = useState(false);

  // 0 means "not configured": no division by zero, the bar stays empty.
  const percentUsed =
    budget.limitTokens > 0
      ? Math.min(
          100,
          Math.round((budget.usedTokens / budget.limitTokens) * 100),
        )
      : 0;
  const limitChanged = limitDraft.trim() !== String(budget.limitTokens);
  const canUnlock = budget.locked && budget.limitTokens > budget.usedTokens;

  function saveLimit() {
    // BD ADM0303 Sheet 9 NO 2 and DD section 5: a whole number from 1 to the server's maximum. A regex,
    // not Number(), because Number("") is 0 and Number(" 5 ") is 5.
    const text = limitDraft.trim();
    const value = Number(text);
    if (!/^\d+$/.test(text) || value < 1 || value > MAX_LIMIT_TOKENS) {
      setLimitInvalid(true);
      toast.error(t("budget.errorLimit"));
      return;
    }
    setLimitInvalid(false);
    setLimit.mutate(value, {
      onSuccess: () => toast.success(t("budget.savedLimit")),
      onError: (error) => toast.error(toMessage(error)),
    });
  }

  function confirmUnlock() {
    unlock.mutate(undefined, {
      onSuccess: () => {
        setUnlockOpen(false);
        toast.success(t("budget.unlocked"));
      },
      // AIR-203 and the rest: the box stays open so the admin sees the reason next to the lock.
      onError: (error) => toast.error(toMessage(error)),
    });
  }

  return (
    <>
      <Card
        title={t("budget.title")}
        description={t("budget.subtitle")}
        action={
          <Badge variant={budget.locked ? "negative" : "success"}>
            {budget.locked ? t("budget.statusLocked") : t("budget.statusOpen")}
          </Badge>
        }
      >
        <div className="flex flex-col gap-3">
          <ProgressBar
            value={percentUsed}
            label={t("budget.usedLabel")}
            fill={
              percentUsed >= budget.warnPercent
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
              disabled={!limitChanged || setLimit.isPending}
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

      <ConfirmDialog
        open={unlockOpen}
        onClose={() => setUnlockOpen(false)}
        onConfirm={confirmUnlock}
        pending={unlock.isPending}
        title={t("budget.unlockTitle")}
        confirmLabel={t("budget.unlockConfirm")}
        cancelLabel={t("budget.unlockCancel")}
      >
        {t("budget.unlockBody", {
          used: numberFormat.format(budget.usedTokens),
          cap: numberFormat.format(budget.limitTokens),
        })}
      </ConfirmDialog>
    </>
  );
}
