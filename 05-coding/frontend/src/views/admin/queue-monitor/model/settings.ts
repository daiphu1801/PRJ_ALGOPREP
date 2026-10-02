// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// ADMIN-tunable parameters of the queue monitor (ADM0401 Q3 and Q6, DEC-2026-1001-admin-configurable-settings).
// The autoscale threshold ends up in `judge.cluster_settings.autoscale_job_threshold`; the other three
// have no table yet. Defaults are the BD's own suggestions [SoT: Suy luận].
import { createSettingsStore } from "@/shared/lib/settings-store";

export const queueSettings = createSettingsStore({
  autoscaleThreshold: 20,
  jobsPerPage: 20,
  errorRetentionHours: 24,
  latencyCapSeconds: 10,
});
