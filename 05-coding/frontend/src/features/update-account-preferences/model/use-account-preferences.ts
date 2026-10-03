// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
"use client";

import { useCallback, useMemo, useState } from "react";
import {
  updateInterviewPracticePreferences,
  updateMySettings,
  type AccountSettings,
  type InterviewPracticePreferences,
} from "@/entities/user";

type Draft = AccountSettings & { interview: InterviewPracticePreferences };

function toDraft(settings: AccountSettings, interview: InterviewPracticePreferences): Draft {
  return { ...settings, interview };
}

/**
 * Owns the THREE "wait for Save" groups (Workspace / Phỏng vấn giả lập / Thông báo) as one dirty
 * flag + one button, matching the prototype's single "Lưu cài đặt" (Sheet 5 Khu vực E NO 3). Under
 * the hood it still fires TWO independent calls per BR-07 (`identity`'s `UpdateMySettings` +
 * `ai-review`'s `updateInterviewPracticePreferences`) — a failure in one never blocks the other,
 * but this prototype does not build BD's full per-group dirty-flag machinery for that case; `save`
 * returns which call succeeded so the view can raise the right toast. Tracked as a simplification in the ledger row.
 */
export function useAccountPreferences(initialSettings: AccountSettings, initialInterview: InterviewPracticePreferences) {
  const [saved, setSaved] = useState(() => toDraft(initialSettings, initialInterview));
  const [draft, setDraft] = useState(saved);
  const [isSaving, setIsSaving] = useState(false);

  const dirty = useMemo(() => JSON.stringify(draft) !== JSON.stringify(saved), [draft, saved]);

  const set = useCallback(<K extends keyof Draft>(key: K, value: Draft[K]) => {
    setDraft((prev) => ({ ...prev, [key]: value }));
  }, []);

  const setWorkspace = useCallback((patch: Partial<AccountSettings["workspace"]>) => {
    setDraft((prev) => ({ ...prev, workspace: { ...prev.workspace, ...patch } }));
  }, []);

  const setNotifications = useCallback((patch: Partial<AccountSettings["notifications"]>) => {
    setDraft((prev) => ({ ...prev, notifications: { ...prev.notifications, ...patch } }));
  }, []);

  const setInterview = useCallback((patch: Partial<InterviewPracticePreferences>) => {
    setDraft((prev) => ({ ...prev, interview: { ...prev.interview, ...patch } }));
  }, []);

  const save = useCallback(async () => {
    setIsSaving(true);
    try {
      const { interview, ...settings } = draft;
      const results = await Promise.allSettled([
        updateMySettings(settings),
        updateInterviewPracticePreferences(interview),
      ]);
      if (results[0]!.status === "fulfilled") {
        setSaved((prev) => ({ ...prev, ...(results[0] as PromiseFulfilledResult<AccountSettings>).value }));
      }
      if (results[1]!.status === "fulfilled") {
        setSaved((prev) => ({ ...prev, interview: (results[1] as PromiseFulfilledResult<InterviewPracticePreferences>).value }));
      }
      // The caller turns this outcome into toasts (success / partial failure / failure).
      return { settingsOk: results[0]!.status === "fulfilled", interviewOk: results[1]!.status === "fulfilled" };
    } finally {
      setIsSaving(false);
    }
  }, [draft]);

  return { draft, dirty, isSaving, set, setWorkspace, setNotifications, setInterview, save };
}
