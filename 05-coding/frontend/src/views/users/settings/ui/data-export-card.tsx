// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { Download, FolderDown } from "lucide-react";
import { ApiError } from "@/shared/api";
import { useApiErrorMessage, useT } from "@/shared/i18n";
import { Button, Card } from "@/shared/ui";
import { toast } from "@/shared/lib/toast-store";
import { useExportData, type ExportResult } from "@/features/export-my-data";

export function DataExportCard() {
  const t = useT("settings");
  const apiMessage = useApiErrorMessage();
  const { pending, exportSubmissions, exportInterviews } = useExportData();

  // BD Q6 (closed 2026-10-18): a failed export reports an error toast and the button stays enabled
  // (ai-review down must not look like "no data to export"). Errors go through the toast channel
  // (DEC-2026-1003-toast-feedback-channel), not an inline role="alert".
  // The failures worth their own words are the 5-minute rate limit (IDT-V06) and the judge being
  // unreachable for the submission file (IDT-508).
  async function run(action: () => Promise<ExportResult>) {
    const result = await action();
    if (result.ok) {
      toast.success(t("dataExport.exported"));
      // The server cut the file at the newest 2000 submissions; say so, or the learner thinks it is whole.
      if (result.truncated)
        toast.info(t("dataExport.truncated", { count: 2000 }));
    } else if (
      result.error instanceof ApiError &&
      (result.error.code === "IDT-V06" || result.error.code === "IDT-508")
    )
      toast.error(apiMessage(result.error));
    else toast.error(t("dataExport.exportFailed"));
  }

  return (
    <Card
      title={
        <span className="flex items-center gap-2">
          <FolderDown size={18} aria-hidden="true" />
          {t("dataExport.title")}
        </span>
      }
    >
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
    <Button
      type="button"
      variant="ghost"
      onClick={onClick}
      disabled={disabled}
      aria-busy={disabled || undefined}
      className="h-auto min-h-11 justify-start gap-2 rounded-lg border border-[var(--color-border)] px-3 py-2 text-left whitespace-normal"
    >
      <Download size={16} aria-hidden="true" className="shrink-0" />
      <span className="min-w-0 flex-1">{label}</span>
      <span className="shrink-0 font-mono text-xs text-[var(--color-text-subtle)]">
        {format}
      </span>
    </Button>
  );
}
