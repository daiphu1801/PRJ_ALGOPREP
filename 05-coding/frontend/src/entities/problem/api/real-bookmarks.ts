// Real calls behind USR0103, the saved-problems list (03-dd/api/F2_problem-bank.md #8 to #10, DD sections
// 9 to 11) and the bookmark toggle of the problem pages USR0102 and USR0104 (#11, DD section 12).
import { api, type PageMeta } from "@/shared/api";
import { solveStateParam, toSolveState } from "./real-student-catalog";
import type {
  RestoreInput,
  SaveNoteInput,
  SavedPage,
  SavedParams,
  SavedRow,
} from "../model/saved-types";

const BASE = "/problem-bank/me/bookmarks";

type ItemDto = {
  problemId: string;
  code: string;
  title: string;
  levelCode: string;
  topicNames: string[];
  solveState: string | null;
  note: string;
  savedAt: string;
  withdrawn: boolean;
};

function toRow(dto: ItemDto): SavedRow {
  return {
    id: dto.problemId,
    code: dto.code,
    title: dto.title,
    difficulty: dto.levelCode,
    topics: dto.topicNames,
    solveState: toSolveState(dto.solveState),
    note: dto.note,
    savedAt: dto.savedAt,
    withdrawn: dto.withdrawn,
  };
}

/** #8: filters are read leniently by the server; the page size is fixed at 20 and is not sent. */
export async function realListSaved(params: SavedParams): Promise<SavedPage> {
  const q = params.q.trim();
  const dto = await api.get<{
    items: ItemDto[];
    page: PageMeta;
    meta: SavedPage["meta"];
  }>(BASE, {
    ...(q ? { q } : {}),
    ...(params.status !== "all"
      ? { status: solveStateParam(params.status) }
      : {}),
    ...(params.difficulty !== "all" ? { difficulty: params.difficulty } : {}),
    sort: "savedAt",
    dir: "desc",
    page: params.page,
  });
  return { items: dto.items.map(toRow), page: dto.page, meta: dto.meta };
}

/** #10: idempotent, always 204; deleting the bookmark deletes its note with it. */
export async function realUnsave(problemId: string): Promise<void> {
  await api.delete(`${BASE}/${problemId}`);
}

/**
 * #9 for an edit: `createIfMissing: false`, so a bookmark removed on another device answers PRB-014
 * instead of being quietly created again. An empty note clears it.
 */
export async function realSaveNote(input: SaveNoteInput): Promise<void> {
  await api.put(`${BASE}/${input.problemId}`, {
    note: input.note,
    createIfMissing: false,
  });
}

/**
 * #9 for "Hoàn tác": puts the bookmark and its note back (`createIfMissing: true`). A problem that was
 * withdrawn meanwhile answers PRB-010 and cannot be restored.
 */
export async function realRestore(input: RestoreInput): Promise<void> {
  await api.put(`${BASE}/${input.problemId}`, {
    note: input.note,
    createIfMissing: true,
  });
}

/**
 * #11: the client sends the wanted state, not a flip, so a repeat or a second device cannot invert it.
 * `false` deletes the row together with its note (DD 12.5); `true` on a withdrawn problem is PRB-010.
 */
export async function realSetBookmarked(input: {
  problemId: string;
  bookmarked: boolean;
}): Promise<boolean> {
  const dto = await api.put<{ bookmarked: boolean }>(
    `${BASE}/${input.problemId}/state`,
    { bookmarked: input.bookmarked },
  );
  return dto.bookmarked;
}
