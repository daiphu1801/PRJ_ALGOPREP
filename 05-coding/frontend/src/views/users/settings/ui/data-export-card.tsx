// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useT } from "@/shared/i18n";
import { Card } from "@/shared/ui";
import { toast } from "@/shared/lib/toast-store";
import { useExportData } from "@/features/export-my-data";

export function DataExportCard() {
  const t = useT("settings");
  const { pending, exportSubmissions, exportInterviews } = useExportData();

  async function run(action: () => Promise<boolean>) {
    if (await action()) toast.success(t("dataExport.exported"));
    else toast.error(t("dataExport.exportFailed"));
  }

  return (
    <Card title={t("dataExport.title")}>
      <div className="flex flex-col gap-2">
        <ExportButton
          label={t("dataExport.exportSubmissions")}
          format="CSV"
          onClick={() => void run(exportSubmissions)}
          disabled={pending === "submissions"}
        />
        <ExportButton
          label={t("dataExport.exportInterviews")}
          format="JSON"
          onClick={() => void run(exportInterviews)}
          disabled={pending === "interviews"}
        />
      </div>
    </Card>
  );
}

function ExportButton({
  label,
  format,
  onClick,
  disabled,
}: {
  label: string;
  format: string;
  onClick: () => void;
  disabled: boolean;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      className="flex h-9 items-center justify-between rounded-lg border border-[var(--color-border)] px-3 text-sm font-semibold text-[var(--color-text)] hover:bg-[var(--color-surface-hover)] disabled:opacity-50"
    >
      <span>{label}</span>
      <span className="font-mono text-xs text-[var(--color-text-subtle)]">
        {format}
      </span>
    </button>
  );
}
