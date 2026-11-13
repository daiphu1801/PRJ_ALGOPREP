// Real calls for `GET` and `PATCH /identity/me/settings` (03-dd/api/F1_identity.md #14 and #15).
//
// The form edits three editor options (font size, autosave, vim mode) that have no column on the
// server, so they are kept on this device in `localStorage`; the DD likewise keeps colour theme and
// UI language on the device. The interview depth parameters (max turns, hints) now live in the same
// `identity` table (DD BR-07, round 4) but the form still saves them as a second, independent
// call, so a PATCH here only ever carries the fields of its own group.
import { api } from "@/shared/api";
import type {
  AccountSettings,
  InterviewPracticePreferences,
} from "../model/types";

const PATH = "/identity/me/settings";
const DEVICE_KEY = "algoprep.editorPrefs";

type SettingsDto = {
  streakReminderEnabled: boolean;
  weeklyReportEnabled: boolean;
  preferredInterviewLevel: AccountSettings["preferredInterviewLevel"];
  preferredMaxTurns: 8 | 12 | 16;
  preferredHintAllowed: boolean;
  defaultLanguage: AccountSettings["workspace"]["defaultLanguage"];
};

type DevicePrefs = Pick<
  AccountSettings["workspace"],
  "editorFontSize" | "autosaveDraft" | "vimMode"
>;

const DEVICE_DEFAULTS: DevicePrefs = {
  editorFontSize: "14",
  autosaveDraft: true,
  vimMode: false,
};

function readDevicePrefs(): DevicePrefs {
  try {
    const raw = localStorage.getItem(DEVICE_KEY);
    return raw
      ? { ...DEVICE_DEFAULTS, ...(JSON.parse(raw) as Partial<DevicePrefs>) }
      : DEVICE_DEFAULTS;
  } catch {
    return DEVICE_DEFAULTS;
  }
}

function writeDevicePrefs(workspace: AccountSettings["workspace"]): void {
  const { editorFontSize, autosaveDraft, vimMode } = workspace;
  try {
    localStorage.setItem(
      DEVICE_KEY,
      JSON.stringify({ editorFontSize, autosaveDraft, vimMode }),
    );
  } catch {
    // Storage blocked: the options then only last until the page is reloaded.
  }
}

function toSettings(dto: SettingsDto): AccountSettings {
  return {
    workspace: { defaultLanguage: dto.defaultLanguage, ...readDevicePrefs() },
    preferredInterviewLevel: dto.preferredInterviewLevel,
    notifications: {
      streakReminderEnabled: dto.streakReminderEnabled,
      weeklyReportEnabled: dto.weeklyReportEnabled,
    },
  };
}

function toInterview(dto: SettingsDto): InterviewPracticePreferences {
  return {
    maxTurnsPerSession: String(dto.preferredMaxTurns) as "8" | "12" | "16",
    hintAllowed: dto.preferredHintAllowed,
  };
}

export async function realGetMySettings(): Promise<AccountSettings> {
  return toSettings(await api.get<SettingsDto>(PATH));
}

export async function realUpdateMySettings(
  input: AccountSettings,
): Promise<AccountSettings> {
  writeDevicePrefs(input.workspace);
  return toSettings(
    await api.patch<SettingsDto>(PATH, {
      streakReminderEnabled: input.notifications.streakReminderEnabled,
      weeklyReportEnabled: input.notifications.weeklyReportEnabled,
      preferredInterviewLevel: input.preferredInterviewLevel,
      defaultLanguage: input.workspace.defaultLanguage,
    }),
  );
}

export async function realGetInterviewPreferences(): Promise<InterviewPracticePreferences> {
  return toInterview(await api.get<SettingsDto>(PATH));
}

export async function realUpdateInterviewPreferences(
  input: InterviewPracticePreferences,
): Promise<InterviewPracticePreferences> {
  return toInterview(
    await api.patch<SettingsDto>(PATH, {
      preferredMaxTurns: Number(input.maxTurnsPerSession),
      preferredHintAllowed: input.hintAllowed,
    }),
  );
}
