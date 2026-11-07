// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Real browser download from the mocked Blob — the swap to a real endpoint only changes what
// exportMyData()/exportMyInterviewTranscripts() do internally, not this hook.
"use client";

import { useCallback, useState } from "react";
import { exportMyData, exportMyInterviewTranscripts } from "@/entities/user";

function download(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

/** Both exporters resolve (never reject); the view raises the toast from the outcome. */
export type ExportResult =
  { ok: true; truncated?: boolean } | { ok: false; error: unknown };

export function useExportData() {
  const [pending, setPending] = useState<"submissions" | "interviews" | null>(
    null,
  );

  const exportSubmissions = useCallback(async () => {
    setPending("submissions");
    try {
      const file = await exportMyData("submissions");
      // Same name the server proposes for this file.
      download(file.blob, "algoprep-submissions.csv");
      return { ok: true, truncated: file.truncated } as ExportResult;
    } catch (error) {
      return { ok: false, error } as ExportResult;
    } finally {
      setPending(null);
    }
  }, []);

  const exportInterviews = useCallback(async () => {
    setPending("interviews");
    try {
      download(
        await exportMyInterviewTranscripts(),
        "interview-transcripts.json",
      );
      return { ok: true } as ExportResult;
    } catch (error) {
      return { ok: false, error } as ExportResult;
    } finally {
      setPending(null);
    }
  }, []);

  return { pending, exportSubmissions, exportInterviews };
}
