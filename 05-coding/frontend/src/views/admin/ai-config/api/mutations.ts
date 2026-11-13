// Writes of the AI config screen: F5 #16 (save draft) and #17 (publish). Mock mode keeps the local
// behaviour (publish computes the next page with `applyPublish`; save draft is a no-op).
import { useMockMutation } from "@/shared/api/mock-query";
import { applyPublish } from "../model/changes";
import { AI_CONFIG_KEY } from "./queries";
import {
  realPublishPrompts,
  realSavePromptDraft,
  type PublishInput,
  type SaveDraftInput,
} from "./real-prompts";

const SPEC = "03-dd/api/F5_ai-review.md";

export const useSavePromptDraft = () =>
  useMockMutation<SaveDraftInput>({
    mock: () => undefined,
    real: realSavePromptDraft,
    spec: SPEC,
    invalidate: [AI_CONFIG_KEY],
  });

/** Resolves to the page as it is live after the publish. */
export const usePublishPrompts = () =>
  useMockMutation({
    mock: ({ saved, draft }: PublishInput) => applyPublish(saved, draft),
    real: realPublishPrompts,
    spec: SPEC,
    invalidate: [AI_CONFIG_KEY],
  });
