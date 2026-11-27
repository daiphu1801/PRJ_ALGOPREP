// The level and topic catalogue as the screens read it. In mock mode it is the in-memory store
// (model/level-store, model/topic-store); in real mode it is the API (api/real-catalog) held in React
// Query. The mode is fixed for the lifetime of the app, so the hook is picked once at load rather than
// branching inside a component (which would break the rules of hooks, and would need a
// QueryClientProvider in every mock-mode test that merely reads a difficulty label).
"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { isMockMode } from "@/shared/api";
import { useApiErrorMessage } from "@/shared/i18n";
import type { ManagedListResult } from "@/shared/ui";
import { toast } from "@/shared/lib/toast-store";
import {
  addProblemLevel,
  moveProblemLevel,
  removeProblemLevel,
  renameProblemLevel,
  useProblemLevels as useMockLevels,
  type ProblemLevel,
} from "../model/level-store";
import {
  addProblemTopic,
  moveProblemTopic,
  removeProblemTopic,
  renameProblemTopic,
  useProblemTopics as useMockTopics,
  type ProblemTopic,
} from "../model/topic-store";
import {
  realCreateLevel,
  realCreateTopic,
  realDeleteLevel,
  realDeleteTopic,
  realListLevels,
  realListTopics,
  realRenameLevel,
  realRenameTopic,
  realReorderLevels,
  realReorderTopics,
} from "./real-catalog";

export const LEVELS_KEY = ["problem", "levels"] as const;
export const TOPICS_KEY = ["problem", "topics"] as const;

const NONE: readonly never[] = [];

function useRealLevels(): readonly ProblemLevel[] {
  const { data } = useQuery({
    queryKey: LEVELS_KEY,
    queryFn: realListLevels,
    staleTime: 60_000,
  });
  return data ?? NONE;
}

function useRealTopics(): readonly ProblemTopic[] {
  const { data } = useQuery({
    queryKey: TOPICS_KEY,
    queryFn: realListTopics,
    staleTime: 60_000,
  });
  return data ?? NONE;
}

/** Levels in the admin-chosen order. Real mode: empty until the first answer arrives. */
export const useProblemLevels: () => readonly ProblemLevel[] = isMockMode()
  ? useMockLevels
  : useRealLevels;

/** Topics in the admin-chosen order. Real mode: `key` is the topic id. */
export const useProblemTopics: () => readonly ProblemTopic[] = isMockMode()
  ? useMockTopics
  : useRealTopics;

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

const sameName = (left: string, right: string) =>
  left.trim().toLocaleLowerCase("vi") === right.trim().toLocaleLowerCase("vi");

type Verbs = {
  list: readonly (ProblemLevel | ProblemTopic)[];
  queryKey: readonly string[];
  create: (name: string) => Promise<void>;
  rename: (id: string, name: string) => Promise<void>;
  reorder: (ids: string[]) => Promise<void>;
  remove: (id: string) => Promise<void>;
};

// One implementation for both catalogues. A failure is turned into text through `apiErrors` (PRB-021,
// PRB-022, PRB-031 to PRB-033, PRB-049 and the rest) and shown here, then the dialog is told it was
// reported so it adds nothing of its own. A write that went through refreshes the catalogue and every
// management list, whose rows show the names and counts.
function useRealActions(verbs: Verbs): ManagedActions {
  const queryClient = useQueryClient();
  const apiMessage = useApiErrorMessage();

  async function run(call: () => Promise<void>): Promise<"reported" | null> {
    try {
      await call();
    } catch (error) {
      toast.error(apiMessage(error));
      return "reported";
    }
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: verbs.queryKey }),
      queryClient.invalidateQueries({ queryKey: ["admin-problem-page"] }),
    ]);
    return null;
  }

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

function useRealTopicActions(): ManagedActions {
  const list = useRealTopics();
  return useRealActions({
    list,
    queryKey: TOPICS_KEY,
    create: realCreateTopic,
    rename: realRenameTopic,
    reorder: realReorderTopics,
    remove: realDeleteTopic,
  });
}

const mockLevelActions: ManagedActions = {
  add: addProblemLevel,
  rename: renameProblemLevel,
  remove: (key) => void removeProblemLevel(key),
  move: moveProblemLevel,
};

const mockTopicActions: ManagedActions = {
  add: addProblemTopic,
  rename: renameProblemTopic,
  remove: (key) => void removeProblemTopic(key),
  move: moveProblemTopic,
};

/** The verbs behind the "Quản lý độ khó" dialog. */
export const useProblemLevelActions: () => ManagedActions = isMockMode()
  ? () => mockLevelActions
  : useRealLevelActions;

/** The verbs behind the "Quản lý chủ đề" dialog. */
export const useProblemTopicActions: () => ManagedActions = isMockMode()
  ? () => mockTopicActions
  : useRealTopicActions;
