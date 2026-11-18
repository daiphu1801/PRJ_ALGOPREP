// ADM0303 Khu vực C: per-hour AI limits and the wait between two requests (F5 #21, #22). The steppers
// show the server's values; one click sends only the field that changed and the answer replaces the cache.
"use client";

import { useLimitsConfig, useUpdateLimitsConfig } from "@/entities/ai-budget";
import { useApiErrorMessage, useT } from "@/shared/i18n";
import { BlockError } from "./block-error";
import { toast } from "@/shared/lib/toast-store";
import { Card, NumberStepper, SettingRow, Skeleton } from "@/shared/ui";

const RATE_LIMITS = [
  { key: "reviewPerHour", unit: "limit.unit.count" },
  { key: "interviewPerHour", unit: "limit.unit.sessions" },
  { key: "cooldownSeconds", unit: "limit.unit.seconds" },
] as const;

export function RateLimitsCard() {
  const t = useT("adminAiLimits");
  const toMessage = useApiErrorMessage();
  const query = useLimitsConfig();
  const update = useUpdateLimitsConfig();

  return (
    <Card title={t("limitsTitle")} description={t("limitsSubtitle")}>
      {query.isError ? (
        <BlockError query={query} />
      ) : !query.data ? (
        <Skeleton className="h-24 w-full" />
      ) : (
        <div className="flex flex-col gap-2.5">
          {RATE_LIMITS.map(({ key, unit }) => (
            <SettingRow
              key={key}
              label={t(`limit.${key}.label`)}
              description={t(`limit.${key}.meta`)}
            >
              <NumberStepper
                editable
                value={query.data[key]}
                disabled={update.isPending}
                onValueChange={(value) =>
                  update.mutate(
                    { [key]: value },
                    { onError: (error) => toast.error(toMessage(error)) },
                  )
                }
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
      )}
    </Card>
  );
}
