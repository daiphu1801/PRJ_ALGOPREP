// Real calls of the topic and level catalogue (03-dd/api/F6_interview-bank.md #9 to #18). Reads are
// open to every signed-in user; writes need ADMIN (INB-301/INB-302 otherwise, shown through
// `apiErrors`). The server sends no colour for a level (DD 1.3 "LevelDto"), so the tone is chosen on
// the client from the seeded codes; a topic or level is keyed by its stable `code`, which is also what
// a question carries and what the learner filter sends.
import { api } from "@/shared/api";
import type { BadgeVariant } from "@/shared/ui";
import type { InterviewLevel } from "../model/level-store";
import type { InterviewTopic } from "../model/topic-store";

const TOPICS = "/interview-bank/topics";
const LEVELS = "/interview-bank/levels";

type TopicDto = {
  id: string;
  code: string;
  displayName: string;
  sortOrder: number;
  usesStarFramework: boolean;
  questionCount: number | null;
};
type LevelDto = Omit<TopicDto, "usesStarFramework">;

// The three seeded levels keep the colours the screens already use; any level an admin adds is neutral.
const TONE_BY_CODE: Record<string, BadgeVariant> = {
  EASY: "success",
  MEDIUM: "warn",
  HARD: "negative",
};

export function toTopic(dto: TopicDto): InterviewTopic {
  return {
    id: dto.id,
    key: dto.code,
    label: dto.displayName,
    usesStarFramework: dto.usesStarFramework,
    questionCount: dto.questionCount,
  };
}

export function toLevel(dto: LevelDto): InterviewLevel {
  return {
    id: dto.id,
    key: dto.code,
    label: dto.displayName,
    tone: TONE_BY_CODE[dto.code] ?? "neutral",
    questionCount: dto.questionCount,
  };
}

/** #9: in `sort_order`. `includeCounts` is always asked for; only ADMIN gets a number back. */
export async function realListTopics(): Promise<InterviewTopic[]> {
  const { items } = await api.get<{ items: TopicDto[] }>(TOPICS, {
    includeCounts: true,
  });
  return items.map(toTopic);
}

/** #10. */
export async function realListLevels(): Promise<InterviewLevel[]> {
  const { items } = await api.get<{ items: LevelDto[] }>(LEVELS, {
    includeCounts: true,
  });
  return items.map(toLevel);
}

/** #11: INB-V03, INB-020 (duplicate name), INB-301/302. */
export async function realCreateTopic(displayName: string): Promise<void> {
  await api.post(TOPICS, { displayName });
}

/** #12: the `code` never changes. Either field alone is enough (INB-V13 when none). */
export async function realUpdateTopic(
  id: string,
  changes: { displayName?: string; usesStarFramework?: boolean },
): Promise<void> {
  await api.patch(`${TOPICS}/${id}`, changes);
}

/** #13: the full list of ids in the new order (INB-034 when it does not match the stored set). */
export async function realReorderTopics(orderedIds: string[]): Promise<void> {
  await api.put(`${TOPICS}/order`, { orderedIds });
}

/** #14: INB-030 while questions still use the topic. */
export async function realDeleteTopic(id: string): Promise<void> {
  await api.delete(`${TOPICS}/${id}`);
}

/** #15: INB-021 on a duplicate name. */
export async function realCreateLevel(displayName: string): Promise<void> {
  await api.post(LEVELS, { displayName });
}

/** #16: INB-012 when the level is gone. */
export async function realRenameLevel(
  id: string,
  displayName: string,
): Promise<void> {
  await api.patch(`${LEVELS}/${id}`, { displayName });
}

/** #17. */
export async function realReorderLevels(orderedIds: string[]): Promise<void> {
  await api.put(`${LEVELS}/order`, { orderedIds });
}

/** #18: INB-031 while questions use the level, INB-032 for the last one. */
export async function realDeleteLevel(id: string): Promise<void> {
  await api.delete(`${LEVELS}/${id}`);
}
