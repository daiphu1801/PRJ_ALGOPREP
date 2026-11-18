// ADM0303 Khu vực D: budget warning threshold (F5 #21, #22) and the per-model price table (F5 #23 to #26).
"use client";

import { useState } from "react";
import {
  useLimitsConfig,
  useModelPrices,
  useUpdateLimitsConfig,
} from "@/entities/ai-budget";
import { useApiErrorMessage, useT } from "@/shared/i18n";
import { BlockError } from "./block-error";
import { toast } from "@/shared/lib/toast-store";
import { Button, Card, NumberStepper, SettingRow, Skeleton } from "@/shared/ui";
import { ModelPriceDialog } from "./model-price-dialog";

export function CostCard() {
  const t = useT("adminAiLimits");
  const toMessage = useApiErrorMessage();
  const limits = useLimitsConfig();
  const update = useUpdateLimitsConfig();
  const prices = useModelPrices();
  const [pricesOpen, setPricesOpen] = useState(false);

  return (
    <Card title={t("cost.title")} description={t("cost.subtitle")}>
      <div className="flex flex-col gap-2.5">
        <SettingRow
          label={t("cost.warnLabel")}
          description={t("cost.warnMeta")}
        >
          {limits.isError ? (
            <BlockError query={limits} />
          ) : !limits.data ? (
            <Skeleton className="h-7 w-28" />
          ) : (
            <NumberStepper
              editable
              value={limits.data.warnPercent}
              disabled={update.isPending}
              onValueChange={(warnPercent) =>
                update.mutate(
                  { warnPercent },
                  { onError: (error) => toast.error(toMessage(error)) },
                )
              }
              label={t("cost.warnLabel")}
              min={1}
              max={100}
              step={5}
              format={(value) => `${value}%`}
              decrementLabel={t("cost.warnDecrement")}
              incrementLabel={t("cost.warnIncrement")}
            />
          )}
        </SettingRow>
        <SettingRow
          label={t("cost.pricesLabel")}
          description={t("cost.pricesMeta")}
        >
          {prices.isError ? (
            <BlockError query={prices} />
          ) : (
            <Button
              variant="ghost"
              size="sm"
              className="border border-[var(--color-border)]"
              disabled={!prices.data}
              onClick={() => setPricesOpen(true)}
            >
              {t("cost.manage")}
            </Button>
          )}
        </SettingRow>
      </div>

      <ModelPriceDialog
        open={pricesOpen}
        onClose={() => setPricesOpen(false)}
        items={prices.data ?? []}
      />
    </Card>
  );
}
