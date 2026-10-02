// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// ADMIN-set retention of the audit log (ADM0403 Q2, DEC-2026-1001-admin-configurable-settings).
// Only the retention period and what happens to expired rows are configuration; individual rows stay
// immutable and there is deliberately no per-row delete anywhere on the screen.
import { createSettingsStore } from "@/shared/lib/settings-store";

export type ExpiryPolicy = "delete" | "archive";

export const logSettings = createSettingsStore<{ retentionDays: number; expiry: ExpiryPolicy }>({
  retentionDays: 90,
  expiry: "delete",
});
