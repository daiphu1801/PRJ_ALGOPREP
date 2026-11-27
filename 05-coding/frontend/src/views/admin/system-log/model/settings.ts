// PROTOTYPE mock only. In real mode the retention comes from F1 #51 and #52 (see api/real-audit-log.ts);
// this store is what the mock reads and writes so the dialog still works with NEXT_PUBLIC_MOCK_DATA.
// Only the retention period and what happens to expired rows are configuration; individual rows stay
// immutable and there is deliberately no per-row delete anywhere on the screen
// (ADM0403 Q2, DEC-2026-1001-admin-configurable-settings).
import { createSettingsStore } from "@/shared/lib/settings-store";
import type { LogRetention } from "./types";

export type ExpiryPolicy = LogRetention["expiry"];

export const logSettings = createSettingsStore<LogRetention>({
  retentionDays: 90,
  expiry: "delete",
});
