// Unpublished-change bookkeeping for admin_ai_config (BD ADM0301 V1.0, Sheet 4 flow step 7).
// Pure functions so the count, the publish summary and the publish step are testable without
// rendering the view.
import {
  GUARD_KEYS,
  GUARDED_FEATURES,
  type AiConfigPage,
  type AiFeatureKey,
  type GuardedFeature,
  type PromptVersionEntry,
} from "./types";

export type PendingCounts = Record<AiFeatureKey, number>;

/** What changed on one tab, for the publish confirmation list (BD ADM0301 Sheet 3, Sheet 8 EVT-12). */
export type FeatureChange = {
  feature: AiFeatureKey;
  instruction: boolean;
  model: { from: string; to: string } | null;
  guards: number;
};

/** Mock "today" shown after a publish (the prototype has no clock of its own). */
const PUBLISH_DATE_LABEL = "10/10";
const PUBLISH_AUTHOR = "pdai";

function isGuarded(feature: AiFeatureKey): feature is GuardedFeature {
  return (GUARDED_FEATURES as AiFeatureKey[]).includes(feature);
}

/** Per tab, what differs from the saved copy. */
export function describeChanges(
  saved: AiConfigPage,
  draft: AiConfigPage,
): FeatureChange[] {
  return draft.prompts
    .map((prompt): FeatureChange => {
      const before = saved.prompts.find((p) => p.feature === prompt.feature)!;
      const guards = isGuarded(prompt.feature)
        ? GUARD_KEYS.filter(
            (key) =>
              saved.guards[prompt.feature as GuardedFeature][key] !==
              draft.guards[prompt.feature as GuardedFeature][key],
          ).length
        : 0;
      return {
        feature: prompt.feature,
        instruction: before.systemInstruction !== prompt.systemInstruction,
        model:
          before.modelId !== prompt.modelId
            ? { from: before.modelId, to: prompt.modelId }
            : null,
        guards,
      };
    })
    .filter(
      (change) =>
        change.instruction ||
        change.model ||
        change.guards > 0,
    );
}

/** How many items differ from the saved copy, per tab. */
export function pendingByFeature(
  saved: AiConfigPage,
  draft: AiConfigPage,
): PendingCounts {
  const counts: PendingCounts = { review: 0, interview: 0, testcase: 0 };
  for (const change of describeChanges(saved, draft)) {
    counts[change.feature] =
      (change.instruction ? 1 : 0) +
      (change.model ? 1 : 0) +
      change.guards;
  }
  return counts;
}

export function totalPending(counts: PendingCounts): number {
  return counts.review + counts.interview + counts.testcase;
}

/** "v3.8" becomes "v3.9"; anything that does not match is returned unchanged. */
export function bumpVersion(version: string): string {
  const match = /^v(\d+)\.(\d+)$/.exec(version);
  return match ? `v${match[1]}.${Number(match[2]) + 1}` : version;
}

/** Overwrites the draft instruction of one feature (edit popup save, or restoring a version). */
export function withInstruction(
  page: AiConfigPage,
  feature: AiFeatureKey,
  systemInstruction: string,
): AiConfigPage {
  return {
    ...page,
    prompts: page.prompts.map((prompt) =>
      prompt.feature === feature ? { ...prompt, systemInstruction } : prompt,
    ),
  };
}

/** The draft after restoring an older version: only its instruction comes back (BD EVT-6). */
export function restoreVersion(
  page: AiConfigPage,
  entry: PromptVersionEntry,
): AiConfigPage {
  return withInstruction(page, entry.feature, entry.systemInstruction);
}

/**
 * The draft after publishing: each tab with changes gets a new live version and a log entry. The
 * saved copy becomes the draft itself, so the pending count drops to zero.
 */
export function applyPublish(
  saved: AiConfigPage,
  draft: AiConfigPage,
): AiConfigPage {
  const counts = pendingByFeature(saved, draft);
  const published = draft.prompts.map((prompt) =>
    counts[prompt.feature] > 0
      ? {
          ...prompt,
          version: bumpVersion(prompt.version),
          status: "live" as const,
          updatedAt: PUBLISH_DATE_LABEL,
        }
      : prompt,
  );
  const entries: PromptVersionEntry[] = published
    .filter((prompt) => counts[prompt.feature] > 0)
    .map((prompt) => ({
      id: `v-${prompt.feature}-${prompt.version}`,
      feature: prompt.feature,
      version: prompt.version,
      publishedAt: PUBLISH_DATE_LABEL,
      author: PUBLISH_AUTHOR,
      systemInstruction: prompt.systemInstruction,
    }));
  return {
    ...draft,
    prompts: published,
    versionHistory: [...entries, ...draft.versionHistory],
  };
}
