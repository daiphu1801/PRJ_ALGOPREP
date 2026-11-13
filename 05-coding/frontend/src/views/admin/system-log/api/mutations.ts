// Writes of the system-log screen: the retention (F1 #52) and the CSV export (#40). The view toasts
// the outcome; a refusal reaches it as an ApiError (IDT-442, IDT-443, IDT-444, IDT-445, IDT-447).
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { withMockData } from "@/shared/api";
import { logSettings } from "../model/settings";
import type { AuditLogParams, LogRetention } from "../model/types";
import { LOG_RETENTION_KEY } from "./queries";
import {
  realExportAuditLogs,
  realSaveLogRetention,
  type ExportColumn,
} from "./real-audit-log";

export function useSaveLogRetention() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: LogRetention) =>
      withMockData(
        () => {
          logSettings.set(input);
          return input;
        },
        () => realSaveLogRetention(input),
      ),
    // The screen shows what the server stored, not what the form held.
    onSuccess: (saved) => queryClient.setQueryData(LOG_RETENTION_KEY, saved),
  });
}

type ExportInput = {
  params: AuditLogParams;
  columns: readonly ExportColumn[];
  locale: "vi" | "en";
};

/** Resolves to the CSV, or to null in mock mode where there is no file to give. */
export function useExportAuditLog() {
  return useMutation({
    mutationFn: (input: ExportInput): Promise<Blob | null> =>
      withMockData<Blob | null>(
        () => null,
        () => realExportAuditLogs(input),
      ),
  });
}
