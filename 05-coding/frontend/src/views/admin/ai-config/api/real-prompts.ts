// Real calls behind ADM0301 (03-dd/api/F5_ai-review.md #14 list + response guards, #15 history,
// #16 save draft, #17 publish). The record is the wire: PromptTemplateController. Paths are
// relative to /api/v1. Every failure is rethrown as `ApiError` (AIR-*), shown through
// `useApiErrorMessage`; nothing here turns an error into an outcome.
//
// Not served by the backend, so not shown in real mode: the history author (no field in #15) and
// the token cap per version (one read-only `maxTokens` per list, shown as the prompt's limit).
import { api, ApiError } from "@/shared/api";
import { formatDateVN } from "@/shared/lib";
import { describeChanges } from "../model/changes";
import {
  FEATURE_KEYS,
  GUARDED_FEATURES,
  type AiConfigPage,
  type AiFeatureKey,
  type GuardSet,
  type GuardedFeature,
  type PromptDraft,
  type PromptVersionEntry,
} from "../model/types";

const BASE = "/ai/admin/prompt-templates";

/** Wire value of `feature_code` per screen tab. */
export const FEATURE_CODE: Record<AiFeatureKey, string> = {
  review: "SOLUTION_REVIEW",
  interview: "MOCK_INTERVIEW",
  testcase: "GENERATE_TESTCASE",
};

type GuardsDto = {
  citeLines: boolean;
  answerInVietnamese: boolean;
  noFullSolution: boolean;
};

type ItemDto = {
  id: string;
  featureCode: string;
  version: string;
  status: string;
  systemInstruction: string;
  modelId: string;
  responseGuards: GuardsDto | null;
  maxTokens: number;
  publishedAt: string | null;
  createdAt: string;
};

type ListDto = { items: ItemDto[]; modelOptions: string[] };

type HistoryItemDto = {
  id: string;
  version: string;
  status: string;
  publishedAt: string | null;
  createdAt: string;
  systemInstruction: string;
};

type HistoryDto = { items: HistoryItemDto[] };

function isGuarded(feature: AiFeatureKey): feature is GuardedFeature {
  return (GUARDED_FEATURES as AiFeatureKey[]).includes(feature);
}

/** 1600 becomes "1.600 token", the way the screen has always printed the cap. */
function formatTokens(value: number): string {
  return `${String(value).replace(/\B(?=(\d{3})+(?!\d))/g, ".")} token`;
}

function toGuardSet(dto: GuardsDto): GuardSet {
  return {
    noFullSolution: dto.noFullSolution,
    citeLines: dto.citeLines,
    answerInVietnamese: dto.answerInVietnamese,
  };
}

function toGuardsDto(guards: GuardSet): GuardsDto {
  return {
    citeLines: guards.citeLines,
    answerInVietnamese: guards.answerInVietnamese,
    noFullSolution: guards.noFullSolution,
  };
}

/** A shape the contract forbids (missing ACTIVE row, null guards): fail loudly, never invent. */
function malformed(what: string): never {
  throw new ApiError("UNKNOWN_ERROR", 200, `F5 prompt list: ${what}`);
}

function toDraft(dto: ItemDto, feature: AiFeatureKey): PromptDraft {
  return {
    systemInstruction: dto.systemInstruction,
    modelId: dto.modelId,
    guards:
      isGuarded(feature) && dto.responseGuards
        ? toGuardSet(dto.responseGuards)
        : null,
  };
}

export function toHistory(
  feature: AiFeatureKey,
  dto: HistoryDto,
): PromptVersionEntry[] {
  return dto.items
    .filter((item) => item.status !== "DRAFT")
    .map((item) => ({
      id: item.id,
      feature,
      version: item.version,
      publishedAt: formatDateVN(item.publishedAt ?? item.createdAt),
      author: null,
      systemInstruction: item.systemInstruction,
    }));
}

export function toPage(
  dto: ListDto,
  history: PromptVersionEntry[],
): AiConfigPage {
  const activeIds = {} as Record<AiFeatureKey, string>;
  const drafts: AiConfigPage["drafts"] = {};
  const guards = {} as Record<GuardedFeature, GuardSet>;
  const prompts = FEATURE_KEYS.map((feature) => {
    const rows = dto.items.filter(
      (item) => item.featureCode === FEATURE_CODE[feature],
    );
    const active = rows.find((item) => item.status === "ACTIVE");
    if (!active) return malformed(`no ACTIVE row for ${feature}`);
    const draft = rows.find((item) => item.status === "DRAFT");
    activeIds[feature] = active.id;
    if (draft) drafts[feature] = toDraft(draft, feature);
    if (isGuarded(feature)) {
      if (!active.responseGuards) return malformed(`no guards for ${feature}`);
      guards[feature] = toGuardSet(active.responseGuards);
    }
    return {
      feature,
      version: active.version,
      status: "live" as const,
      modelId: active.modelId,
      systemInstruction: active.systemInstruction,
      maxTokens: formatTokens(active.maxTokens),
      updatedAt: formatDateVN(active.publishedAt ?? active.createdAt),
    };
  });
  return {
    prompts,
    modelOptions: dto.modelOptions,
    activeIds,
    drafts,
    guards,
    versionHistory: history,
  };
}

async function getHistory(feature: AiFeatureKey): Promise<PromptVersionEntry[]> {
  return toHistory(
    feature,
    await api.get<HistoryDto>(`${BASE}/${FEATURE_CODE[feature]}/history`),
  );
}

/** #14 plus #15 for each tab: the screen opens with the whole version log already in hand. */
export async function realGetAiConfigPage(): Promise<AiConfigPage> {
  const [list, ...histories] = await Promise.all([
    api.get<ListDto>(BASE),
    ...FEATURE_KEYS.map(getHistory),
  ]);
  return toPage(list, histories.flat());
}

export type SaveDraftInput = {
  feature: AiFeatureKey;
  systemInstruction: string;
  modelId: string;
  /** Null for testcase generation (the server rejects guards there: AIR-V13). */
  guards: GuardSet | null;
};

/** #16: the 200 body (the saved DRAFT row) is not read; the screen already holds the values. */
export async function realSavePromptDraft(
  input: SaveDraftInput,
): Promise<void> {
  await api.put(`${BASE}/${FEATURE_CODE[input.feature]}/draft`, {
    systemInstruction: input.systemInstruction,
    modelId: input.modelId,
    responseGuards:
      isGuarded(input.feature) && input.guards
        ? toGuardsDto(input.guards)
        : null,
  });
}

export type PublishInput = { saved: AiConfigPage; draft: AiConfigPage };

/**
 * #17: one atomic call for every tab with a pending change. Each change carries the full values
 * (instruction, model, guards) so a stored server DRAFT can never leak into a tab the admin reverted;
 * `expectedActivePromptId` is the id loaded with the page (AIR-201 on a stale one).
 */
export async function realPublishPrompts(
  input: PublishInput,
): Promise<AiConfigPage> {
  const changes = describeChanges(input.saved, input.draft).map((change) => {
    const prompt = input.draft.prompts.find(
      (item) => item.feature === change.feature,
    )!;
    return {
      featureCode: FEATURE_CODE[change.feature],
      expectedActivePromptId: input.saved.activeIds[change.feature],
      systemInstruction: prompt.systemInstruction,
      modelId: prompt.modelId,
      responseGuards: isGuarded(change.feature)
        ? toGuardsDto(input.draft.guards[change.feature])
        : null,
    };
  });
  const list = await api.post<ListDto>(`${BASE}/publish`, { changes });
  // The publish already succeeded: a failed history reload keeps the log the screen had.
  const histories = await Promise.all(
    FEATURE_KEYS.map((feature) =>
      getHistory(feature).catch(() =>
        input.draft.versionHistory.filter((entry) => entry.feature === feature),
      ),
    ),
  );
  return toPage(list, histories.flat());
}
