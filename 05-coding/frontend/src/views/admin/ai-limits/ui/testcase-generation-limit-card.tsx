"use client";
import { useRef } from "react";
import {
  AI_GENERATION_LIMIT_MAX,
  AI_GENERATION_LIMIT_MIN,
  useTestcaseGenerationSettings,
  useUpdateTestcaseGenerationSettings,
} from "@/entities/problem";
import { useApiErrorMessage, useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import {
  Card,
  NumberStepper,
  QueryError,
  SettingRow,
  Skeleton,
} from "@/shared/ui";
/** F2-owned #55/#56; the other cards on ADM0303 belong to F5. */
export function TestcaseGenerationLimitCard() {
  const t = useT("adminAiLimits");
  const toMessage = useApiErrorMessage();
  const query = useTestcaseGenerationSettings();
  const update = useUpdateTestcaseGenerationSettings();
  const writing = useRef(false);
  return (
    <Card
      title={t("testcaseGen.title")}
      description={t("testcaseGen.subtitle")}
    >
      {query.isError ? (
        <QueryError query={query} />
      ) : !query.data ? (
        <Skeleton className="h-10 w-full" />
      ) : (
        <SettingRow
          label={t("testcaseGen.maxLabel")}
          description={t("testcaseGen.maxMeta")}
        >
          <NumberStepper
            editable
            value={query.data.maxPerProblem}
            disabled={update.isPending}
            onValueChange={async (maxPerProblem) => {
              if (writing.current) return;
              writing.current = true;
              try {
                await update.mutateAsync(maxPerProblem);
              } catch (error) {
                toast.error(toMessage(error));
              } finally {
                writing.current = false;
              }
            }}
            label={t("testcaseGen.maxLabel")}
            min={AI_GENERATION_LIMIT_MIN}
            max={AI_GENERATION_LIMIT_MAX}
            step={1}
            format={(value) => t("testcaseGen.value", { count: value })}
            decrementLabel={t("testcaseGen.decrement")}
            incrementLabel={t("testcaseGen.increment")}
          />
        </SettingRow>
      )}
    </Card>
  );
}
