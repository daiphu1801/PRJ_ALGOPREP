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

// Both exporters resolve to true on success, false on failure; the view raises the toast.
export function useExportData() {
  const [pending, setPending] = useState<"submissions" | "interviews" | null>(
    null,
  );

  const exportSubmissions = useCallback(async () => {
    setPending("submissions");
    try {
      download(await exportMyData("submissions"), "submissions.csv");
      return true;
    } catch {
      return false;
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
      return true;
    } catch {
      return false;
    } finally {
      setPending(null);
    }
  }, []);

  return { pending, exportSubmissions, exportInterviews };
}
