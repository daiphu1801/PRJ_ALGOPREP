// Real-mode "Luồng chấm" block of ADM0401 (03-dd/screens/admin/ADM0401_queue_monitor.md BR-SC-03, BR-SC-04).
// Shows only what has a real source: the consumers the grading worker reported and the throughput computed
// from submissions. There is no load percentage, uptime or vCPU: go-judge has no worker concept.
import { useT } from "@/shared/i18n";
import type { GradingBlock } from "../model/types";

/** "HH:mm:ss" in the viewer's local time. */
function clock(epochMs: number): string {
  return new Date(epochMs).toLocaleTimeString("vi-VN", { hour12: false });
}

export function GradingCard({
  grading,
  details = false,
}: {
  grading: GradingBlock;
  details?: boolean;
}) {
  const t = useT("adminQueueMonitor");
  const { consumers, estimatedClearSeconds } = grading;
  const last = consumers?.lastScaling;

  // Under a minute in seconds, from a minute rounded up to whole minutes (BR-SC-03).
  const clear =
    estimatedClearSeconds == null
      ? t("grading.clearUnknown")
      : estimatedClearSeconds < 60
        ? t("grading.clearSeconds", { count: estimatedClearSeconds })
        : t("grading.clearMinutes", {
            count: Math.ceil(estimatedClearSeconds / 60),
          });

  const rows = details
    ? consumers
      ? [
          { label: t("grading.target"), value: String(consumers.target) },
          {
            label: t("grading.lastScaling"),
            value: last
              ? t(
                  last.to > last.from
                    ? "grading.scaledUp"
                    : "grading.scaledDown",
                  {
                    time: clock(last.at),
                    from: last.from,
                    to: last.to,
                    reason: t(`grading.reason.${last.reason}`),
                    pending: last.pendingCount,
                  },
                )
              : t("grading.noScaling"),
          },
        ]
      : []
    : [
        {
          label: t("grading.running"),
          value: consumers
            ? t("grading.runningValue", {
                active: consumers.active,
                max: consumers.max,
              })
            : t("grading.noReport"),
        },
        {
          label: t("grading.throughput"),
          value: t("grading.throughputValue", {
            count: grading.finishedLastMinute,
          }),
        },
        { label: t("grading.clear"), value: clear },
      ];

  return rows.map(({ label, value }) => (
    <div key={label}>
      <dt className="text-xs text-[var(--color-text-muted)]">{label}</dt>
      <dd className="mt-1 text-sm font-semibold tabular-nums">{value}</dd>
    </div>
  ));
}
