// PROTOTYPE mock — no backend endpoint exists yet.
// Values from 09-layoutBase/Admin - Cấu hình AI.dc.html:418-470.
import type { AiConfigPage, PromptConfig, PromptVersionEntry } from "../../model/types";

// dc.html:418-422.
const PROMPTS: PromptConfig[] = [
  {
    feature: "review",
    version: "v3.8",
    status: "live",
    model: "claude-sonnet",
    temperature: "0,2",
    maxTokens: "1.600 token",
    updatedAt: "11/08",
  },
  {
    feature: "interview",
    version: "v2.5",
    status: "abTest",
    model: "claude-sonnet",
    temperature: "0,6",
    maxTokens: "1.200 token",
    updatedAt: "19/08",
  },
  {
    feature: "testcase",
    version: "v1.9",
    status: "draft",
    model: "claude-haiku",
    temperature: "0,4",
    maxTokens: "2.000 token",
    updatedAt: "20/08",
  },
];

/**
 * The "Nhật ký phiên bản" modal has no data in the static mockup — it is a button with nothing
 * behind it. These rows are [SoT: Suy luận], derived from the prompt versions above so the modal
 * shows something coherent rather than an empty box. Whether the log is per feature or combined is
 * an open question (level C in 07-review/bd_screens_admin_open_questions_260913.md).
 */
const VERSION_HISTORY: PromptVersionEntry[] = [
  { id: "v-1", feature: "testcase", version: "v1.9", publishedAt: "20/08", author: "pdai" },
  { id: "v-2", feature: "interview", version: "v2.5", publishedAt: "19/08", author: "pdai" },
  { id: "v-3", feature: "interview", version: "v2.4", publishedAt: "12/08", author: "pdai" },
  { id: "v-4", feature: "review", version: "v3.8", publishedAt: "11/08", author: "minhtri" },
  { id: "v-5", feature: "review", version: "v3.7", publishedAt: "02/08", author: "pdai" },
];

export function fetchAiConfigPage(): AiConfigPage {
  return {
    prompts: PROMPTS.map((prompt) => ({ ...prompt })),
    // dc.html:433 `state.weights`.
    rubricWeights: {
      correctness: 30,
      complexity: 25,
      codeQuality: 20,
      edgeCases: 15,
      explanation: 10,
    },
    // dc.html:451-455.
    rateLimits: {
      reviewPerDay: "10 lượt",
      interviewPerWeek: "5 phiên",
      cooldown: "20 giây",
    },
    // dc.html:434 `state.guards`.
    guards: {
      noFullSolution: true,
      citeLines: true,
      answerInVietnamese: true,
    },
    versionHistory: VERSION_HISTORY.map((entry) => ({ ...entry })),
    // dc.html:259.
    lastComparison: { date: "19/08", averageDelta: "0,4 điểm" },
  };
}
