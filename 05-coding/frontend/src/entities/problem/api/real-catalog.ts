// Real calls of the level and topic catalogue (03-dd/api/F2_problem-bank.md #14 to #23, DD sections 15
// to 24). Reads are open to every signed-in user; writes need ADMIN (PRB-004 otherwise, shown through
// `apiErrors`). The server sends no colour for a level (DD 15.3), so the tone is chosen on the client.
import { api } from "@/shared/api";
import type { BadgeVariant } from "@/shared/ui";
import type { ProblemLevel } from "../model/level-store";
import type { ProblemTopic } from "../model/topic-store";

const LEVELS = "/problem-bank/levels";
const TOPICS = "/problem-bank/topics";

type LevelDto = {
  id: string;
  code: string;
  displayName: string;
  sortOrder: number;
  problemCount: number | null;
};
type TopicDto = {
  id: string;
  code: string;
  name: string;
  sortOrder: number;
  problemCount: number | null;
};

// The three seeded levels keep the colours the screens already use; any level an admin adds is neutral.
const TONE_BY_CODE: Record<string, BadgeVariant> = {
  EASY: "success",
  MEDIUM: "warn",
  HARD: "negative",
};

export function toLevel(dto: LevelDto): ProblemLevel {
  return {
    id: dto.id,
    key: dto.code,
    label: dto.displayName,
    tone: TONE_BY_CODE[dto.code] ?? "neutral",
    problemCount: dto.problemCount,
  };
}

/** A topic is keyed by its id: the bulk "assign topic" call and the topic filter both take the id. */
export function toTopic(dto: TopicDto): ProblemTopic {
  return {
    id: dto.id,
    key: dto.id,
    label: dto.name,
    problemCount: dto.problemCount,
  };
}

/**
 * #14: in `sort_order`. `includeCounts` is always asked for; the server fills `problemCount` for ADMIN
 * only and sends null to everyone else.
 */
export async function realListLevels(): Promise<ProblemLevel[]> {
  const { items } = await api.get<{ items: LevelDto[] }>(LEVELS, {
    includeCounts: true,
  });
  return items.map(toLevel);
}

/** #15. */
export async function realCreateLevel(displayName: string): Promise<void> {
  await api.post(LEVELS, { displayName });
}

/** #16: the `code` never changes on a rename. */
export async function realRenameLevel(
  id: string,
  displayName: string,
): Promise<void> {
  await api.patch(`${LEVELS}/${id}`, { displayName });
}

/** #17: the full list of ids in the new order. */
export async function realReorderLevels(orderedIds: string[]): Promise<void> {
  await api.put(`${LEVELS}/order`, { orderedIds });
}

/** #18: PRB-032 while problems still use the level, PRB-033 for the last one. */
export async function realDeleteLevel(id: string): Promise<void> {
  await api.delete(`${LEVELS}/${id}`);
}

/** #19. */
export async function realListTopics(): Promise<ProblemTopic[]> {
  const { items } = await api.get<{ items: TopicDto[] }>(TOPICS, {
    includeCounts: true,
  });
  return items.map(toTopic);
}

/** #20. */
export async function realCreateTopic(name: string): Promise<void> {
  await api.post(TOPICS, { name });
}

/** #21. */
export async function realRenameTopic(id: string, name: string): Promise<void> {
  await api.patch(`${TOPICS}/${id}`, { name });
}

/** #22. */
export async function realReorderTopics(orderedIds: string[]): Promise<void> {
  await api.put(`${TOPICS}/order`, { orderedIds });
}

/** #23: PRB-031 while problems still use the topic. */
export async function realDeleteTopic(id: string): Promise<void> {
  await api.delete(`${TOPICS}/${id}`);
}
