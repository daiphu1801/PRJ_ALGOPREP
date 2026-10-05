// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 22.
//
// ADMIN-set cap on F2-14 "Sinh tự động" runs per problem (ADM0301 Khu vực H,
// DEC-2026-1003-ai-testcase-generation-quota). Edited on the AI config screen, read by the authoring
// mock when it decides whether a run is refused. Resets on reload: the real value is a row behind
// ADMIN-only endpoints (BD ADM0301 Q12).
"use client";

import { createSettingsStore } from "@/shared/lib/settings-store";

export const AI_GENERATION_LIMIT_MIN = 1;
export const AI_GENERATION_LIMIT_MAX = 10;

/** Default 2: a problem needs 8 approved testcases and each run adds about 4 drafts. */
export const aiGenerationSettings = createSettingsStore({ maxPerProblem: 2 });
