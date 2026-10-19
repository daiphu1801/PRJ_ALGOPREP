// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Badge colour per student status, shared by every instructor screen that shows the status label.
import type { BadgeVariant } from "@/shared/ui";
import type { StudentStatus } from "./types";

export const STUDENT_STATUS_VARIANT: Record<StudentStatus, BadgeVariant> = {
  insufficient_data: "neutral",
  absent: "negative",
  needs_support: "warn",
  watch: "blue",
  on_track: "success",
};
