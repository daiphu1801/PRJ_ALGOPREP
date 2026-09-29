// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Khu vực B/C/D + E (Workspace, Phỏng vấn giả lập, Thông báo + trạng thái/lưu), one combined
// dirty flag and Save button per Sheet 5 Khu vực E NO 3.
"use client";

import { useT } from "@/shared/i18n";
import { Button, Card, EmptyState, ErrorState, SegmentedTabs, SettingRow, Skeleton, Toggle } from "@/shared/ui";
import { useInterviewPreferences, useMySettings } from "@/entities/user";
import { useAccountPreferences } from "@/features/update-account-preferences";

export function PreferencesForm() {
  const t = useT("settings");
  const settingsQuery = useMySettings();
  const interviewQuery = useInterviewPreferences();

  if (settingsQuery.isLoading) {
    return (
      <div className="flex flex-col gap-3.5">
        <Skeleton className="h-40" />
        <Skeleton className="h-32" />
        <Skeleton className="h-24" />
      </div>
    );
  }

  if (settingsQuery.isError || !settingsQuery.data) {
    return (
      <Card title={t("workspace.title")}>
        <ErrorState>
          <div className="flex flex-col items-center gap-2">
            <span>{t("loadError")}</span>
            <Button size="sm" variant="ghost" onClick={() => settingsQuery.refetch()}>
              {t("retry")}
            </Button>
          </div>
        </ErrorState>
      </Card>
    );
  }

  return (
    <Body
      settings={settingsQuery.data}
      interview={interviewQuery.data}
      interviewDegraded={interviewQuery.isError}
    />
  );
}

function Body({
  settings,
  interview,
  interviewDegraded,
}: {
  settings: NonNullable<ReturnType<typeof useMySettings>["data"]>;
  interview: ReturnType<typeof useInterviewPreferences>["data"];
  interviewDegraded: boolean;
}) {
  const t = useT("settings");
  const { draft, dirty, isSaving, justSaved, interviewPreferencesWarning, setWorkspace, setNotifications, setInterview, set, save } =
    useAccountPreferences(settings, interview ?? { maxTurnsPerSession: "12", hintAllowed: true });

  const statusText = justSaved ? t("saveBar.statusSaved") : dirty ? t("saveBar.statusDirty") : t("saveBar.statusIdle");

  return (
    <div className="flex flex-col gap-3.5">
      <Card title={t("workspace.title")}>
        <div className="flex flex-col gap-2">
          <SettingRow label={t("workspace.defaultLanguage")} description={t("workspace.defaultLanguageDescription")}>
            <SegmentedTabs
              label={t("workspace.defaultLanguage")}
              value={draft.workspace.defaultLanguage}
              onValueChange={(v) => setWorkspace({ defaultLanguage: v })}
              options={[
                { value: "PYTHON", label: "Python 3" },
                { value: "JAVA", label: "Java 21" },
                { value: "CPP", label: "C++ 17" },
              ]}
            />
          </SettingRow>
          <SettingRow label={t("workspace.editorFontSize")} description={t("workspace.editorFontSizeDescription")}>
            <SegmentedTabs
              label={t("workspace.editorFontSize")}
              value={draft.workspace.editorFontSize}
              onValueChange={(v) => setWorkspace({ editorFontSize: v })}
              options={[
                { value: "13", label: "13" },
                { value: "14", label: "14" },
                { value: "16", label: "16" },
              ]}
            />
          </SettingRow>
          <SettingRow label={t("workspace.autosaveDraft")} description={t("workspace.autosaveDraftDescription")}>
            <Toggle
              label={t("workspace.autosaveDraft")}
              checked={draft.workspace.autosaveDraft}
              onCheckedChange={(v) => setWorkspace({ autosaveDraft: v })}
            />
          </SettingRow>
          <SettingRow label={t("workspace.vimMode")} description={t("workspace.vimModeDescription")}>
            <Toggle label={t("workspace.vimMode")} checked={draft.workspace.vimMode} onCheckedChange={(v) => setWorkspace({ vimMode: v })} />
          </SettingRow>
        </div>
      </Card>

      <Card title={t("interview.title")}>
        {interviewDegraded ? (
          <EmptyState>{t("interview.degradedNotice")}</EmptyState>
        ) : (
          <div className="flex flex-col gap-2">
            <SettingRow label={t("interview.interviewerLevel")} description={t("interview.interviewerLevelDescription")}>
              <SegmentedTabs
                label={t("interview.interviewerLevel")}
                value={draft.preferredInterviewLevel}
                onValueChange={(v) => set("preferredInterviewLevel", v)}
                options={[
                  { value: "INTERN", label: "Intern" },
                  { value: "JUNIOR", label: "Junior" },
                  { value: "MIDDLE", label: "Middle" },
                  { value: "SENIOR", label: "Senior" },
                ]}
              />
            </SettingRow>
            <SettingRow label={t("interview.maxTurns")} description={t("interview.maxTurnsDescription")}>
              <SegmentedTabs
                label={t("interview.maxTurns")}
                value={draft.interview.maxTurnsPerSession}
                onValueChange={(v) => setInterview({ maxTurnsPerSession: v })}
                options={[
                  { value: "8", label: "8" },
                  { value: "12", label: "12" },
                  { value: "16", label: "16" },
                ]}
              />
            </SettingRow>
            <SettingRow label={t("interview.hintAllowed")} description={t("interview.hintAllowedDescription")}>
              <Toggle label={t("interview.hintAllowed")} checked={draft.interview.hintAllowed} onCheckedChange={(v) => setInterview({ hintAllowed: v })} />
            </SettingRow>
            <p className="px-1 text-xs text-[var(--color-text-subtle)]">{t("interview.scopeNote")}</p>
          </div>
        )}
      </Card>

      <Card title={t("notification.title")}>
        <div className="flex flex-col gap-2">
          <SettingRow label={t("notification.streakReminder")} description={t("notification.streakReminderDescription")}>
            <Toggle
              label={t("notification.streakReminder")}
              checked={draft.notifications.streakReminderEnabled}
              onCheckedChange={(v) => setNotifications({ streakReminderEnabled: v })}
            />
          </SettingRow>
          <SettingRow label={t("notification.weeklyReport")} description={t("notification.weeklyReportDescription")}>
            <Toggle
              label={t("notification.weeklyReport")}
              checked={draft.notifications.weeklyReportEnabled}
              onCheckedChange={(v) => setNotifications({ weeklyReportEnabled: v })}
            />
          </SettingRow>
        </div>
      </Card>

      <Card title={t("saveBar.title")}>
        <p className="mb-3 text-sm text-[var(--color-text-muted)]">{statusText}</p>
        {interviewPreferencesWarning ? (
          <p className="mb-3 text-xs text-[var(--color-danger)]">{t("interview.saveFailedWarning")}</p>
        ) : null}
        <Button className="w-full" onClick={() => void save()} disabled={!dirty || isSaving} aria-busy={isSaving || undefined}>
          {justSaved ? t("saveBar.saved") : t("saveBar.save")}
        </Button>
      </Card>
    </div>
  );
}
