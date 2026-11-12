// The topic and level catalogue as the screens read it. In mock mode it is the in-memory store
// (model/topic-store, model/level-store); in real mode it is the API (api/real-catalog) held in React
// Query. The mode is fixed for the lifetime of the app, so the hook is picked once at load rather than
// branching inside a component (which would break the rules of hooks, and would need a
// QueryClientProvider in every mock-mode test that merely reads a difficulty label). Same shape as
// entities/problem/api/catalog-queries.ts; the verbs are redefined here because entities may not import
// each other.
"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { isMockMode } from "@/shared/api";
import { useApiErrorMessage } from "@/shared/i18n";
import type { ManagedListResult } from "@/shared/ui";
import { toast } from "@/shared/lib/toast-store";
import {
  addInterviewLevel,
  moveInterviewLevel,
  removeInterviewLevel,
  renameInterviewLevel,
  useInterviewLevels as useMockLevels,
  type InterviewLevel,
} from "../model/level-store";
import {
  addInterviewTopic,
  moveInterviewTopic,
  removeInterviewTopic,
  renameInterviewTopic,
  setInterviewTopicStar,
  useInterviewTopics as useMockTopics,
  type InterviewTopic,
} from "../model/topic-store";
import {
  realCreateLevel,
  realCreateTopic,
  realDeleteLevel,
  realDeleteTopic,
  realListLevels,
  realListTopics,
  realRenameLevel,
  realReorderLevels,
  realReorderTopics,
  realUpdateTopic,
} from "./real-catalog";

export const TOPICS_KEY = ["interview-question", "topics"] as const;
export const LEVELS_KEY = ["interview-question", "levels"] as const;

const NONE: readonly never[] = [];

function useRealTopics(): readonly InterviewTopic[] {
  const { data } = useQuery({
    queryKey: TOPICS_KEY,
    queryFn: realListTopics,
    staleTime: 60_000,
  });
  return data ?? NONE;
}

function useRealLevels(): readonly InterviewLevel[] {
  const { data } = useQuery({
    queryKey: LEVELS_KEY,
    queryFn: realListLevels,
    staleTime: 60_000,
  });
  return data ?? NONE;
}

/** Topics in the admin-chosen order. Real mode: empty until the first answer arrives; `key` is the topic code. */
export const useInterviewTopics: () => readonly InterviewTopic[] = isMockMode()
  ? useMockTopics
  : useRealTopics;

/** Levels in the admin-chosen order. Real mode: empty until the first answer arrives; `key` is the level code. */
export const useInterviewLevels: () => readonly InterviewLevel[] = isMockMode()
  ? useMockLevels
  : useRealLevels;

/** What `ManagedListDialog` calls; the same four verbs for a level list and a topic list. */
export type ManagedActions = {
  add: (label: string) => ManagedListResult | Promise<ManagedListResult>;
  rename: (
    key: string,
    label: string,
  ) => ManagedListResult | Promise<ManagedListResult>;
  remove: (key: string) => unknown;
  move: (key: string, delta: -1 | 1) => void;
};

export type TopicActions = ManagedActions & {
  /** The "uses STAR" switch of a topic row; resolves `"reported"` when the server refused (toast shown). */
  setStar: (key: string, usesStarFramework: boolean) => Promise<"reported" | null>;
};

const sameName = (left: string, right: string) =>
  left.trim().toLocaleLowerCase("vi") === right.trim().toLocaleLowerCase("vi");

type Verbs = {
  list: readonly (InterviewTopic | InterviewLevel)[];
  queryKey: readonly string[];
  create: (name: string) => Promise<void>;
  rename: (id: string, name: string) => Promise<void>;
  reorder: (ids: string[]) => Promise<void>;
  remove: (id: string) => Promise<void>;
};

// One implementation for both catalogues. A failure is turned into text through `apiErrors` (INB-011,
// INB-012, INB-020, INB-021, INB-030 to INB-032, INB-034, INB-301/302, INB-503, INB-V03, INB-V13,
// INB-V14) and shown here, then the dialog is told it was reported so it adds nothing of its own. A
// write that went through refreshes the catalogue and every list that shows names or counts.
function useRun(queryKey: readonly string[]) {
  const queryClient = useQueryClient();
  const apiMessage = useApiErrorMessage();
  return async function run(call: () => Promise<void>): Promise<"reported" | null> {
    try {
      await call();
    } catch (error) {
      toast.error(apiMessage(error));
      return "reported";
    }
    await Promise.all([
      queryClient.invalidateQueries({ queryKey }),
      queryClient.invalidateQueries({ queryKey: ["interview-question-page"] }),
      queryClient.invalidateQueries({ queryKey: ["interview-question", "learner"] }),
    ]);
    return null;
  };
}

function useRealActions(verbs: Verbs): ManagedActions {
  const run = useRun(verbs.queryKey);
  const idOf = (key: string) => verbs.list.find((item) => item.key === key)?.id;

  return {
    add: (label) => {
      const name = label.trim();
      if (!name) return "empty";
      if (verbs.list.some((item) => sameName(item.label, name)))
        return "duplicate";
      return run(() => verbs.create(name));
    },
    rename: (key, label) => {
      const name = label.trim();
      const id = idOf(key);
      if (!name) return "empty";
      if (!id) return "reported";
      if (
        verbs.list.some(
          (item) => item.key !== key && sameName(item.label, name),
        )
      )
        return "duplicate";
      return run(() => verbs.rename(id, name));
    },
    remove: async (key) => {
      const id = idOf(key);
      if (!id) return "reported";
      return (await run(() => verbs.remove(id))) ?? undefined;
    },
    move: (key, delta) => {
      const from = verbs.list.findIndex((item) => item.key === key);
      const to = from + delta;
      if (from < 0 || to < 0 || to >= verbs.list.length) return;
      const ids = verbs.list.map((item) => item.id ?? "");
      [ids[from], ids[to]] = [ids[to]!, ids[from]!];
      void run(() => verbs.reorder(ids));
    },
  };
}

function useRealTopicActions(): TopicActions {
  const list = useRealTopics();
  const base = useRealActions({
    list,
    queryKey: TOPICS_KEY,
    create: realCreateTopic,
    rename: (id, name) => realUpdateTopic(id, { displayName: name }),
    reorder: realReorderTopics,
    remove: realDeleteTopic,
  });
  const run = useRun(TOPICS_KEY);
  return {
    ...base,
    setStar: async (key, usesStarFramework) => {
      const id = list.find((item) => item.key === key)?.id;
      if (!id) return "reported";
      return run(() => realUpdateTopic(id, { usesStarFramework }));
    },
  };
}

function useRealLevelActions(): ManagedActions {
  const list = useRealLevels();
  return useRealActions({
    list,
    queryKey: LEVELS_KEY,
    create: realCreateLevel,
    rename: realRenameLevel,
    reorder: realReorderLevels,
    remove: realDeleteLevel,
  });
}

const mockTopicActions: TopicActions = {
  add: addInterviewTopic,
  rename: renameInterviewTopic,
  remove: (key) => void removeInterviewTopic(key),
  move: moveInterviewTopic,
  setStar: async (key, usesStarFramework) => {
    setInterviewTopicStar(key, usesStarFramework);
    return null;
  },
};

const mockLevelActions: ManagedActions = {
  add: addInterviewLevel,
  rename: renameInterviewLevel,
  remove: (key) => void removeInterviewLevel(key),
  move: moveInterviewLevel,
};

/** The verbs behind the "Quản lý chủ đề" dialog (real: F6 #11 to #14). */
export const useInterviewTopicActions: () => TopicActions = isMockMode()
  ? () => mockTopicActions
  : useRealTopicActions;

/** The verbs behind the "Quản lý độ khó" dialog (real: F6 #15 to #18). */
export const useInterviewLevelActions: () => ManagedActions = isMockMode()
  ? () => mockLevelActions
  : useRealLevelActions;
