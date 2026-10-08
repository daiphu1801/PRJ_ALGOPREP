// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Mock profile/settings backend. Default values ported from 09-layoutBase/Trang cá nhân.dc.html and
// 09-layoutBase/Cài đặt.dc.html `DEFAULTS`/`state`, minus the parts RD explicitly cut (2FA, `Pro`
// plan — 01-rd/screens/users/USR0502_profile.md O-4/O-5) and with the 4-level interviewer enum from
// `DEC-2026-0922-users-and-admin-conflict-resolutions` instead of the prototype's 3 levels.
import type {
  AccountSettings,
  InterviewPracticePreferences,
  UpdateProfileInput,
  UserProfile,
} from "../../model/types";
import type { ChangePasswordInput } from "../../model/schema";

function delay<T>(value: T, ms = 500): Promise<T> {
  return new Promise((resolve) => setTimeout(() => resolve(value), ms));
}

let profile: UserProfile = {
  userId: "6f6b6e2a-9b2e-4c9a-8a3e-0f6a2c9d10482",
  displayName: "Phú Đại",
  email: "phudai@fabbi.io",
  schoolOrCompany: "Đại học Cần Thơ",
  currentPosition: "Sinh viên năm 4",
  defaultLanguage: "PYTHON",
  targetPosition: "Middle Backend",
  hasPassword: true,
  passwordChangedAt: "2026-06-12",
  linkedProviders: ["GOOGLE"],
  joinedAt: "2026-02-14",
  roleName: "Người học",
};

let settings: AccountSettings = {
  workspace: {
    defaultLanguage: "PYTHON",
    editorFontSize: "14",
    autosaveDraft: true,
    vimMode: false,
  },
  preferredInterviewLevel: "MIDDLE",
  notifications: { streakReminderEnabled: true, weeklyReportEnabled: false },
};

let interviewPreferences: InterviewPracticePreferences = {
  maxTurnsPerSession: "12",
  hintAllowed: true,
};

export function fakeGetMyProfile(): Promise<UserProfile> {
  return delay(profile);
}

export function fakeUpdateMyProfile(
  input: UpdateProfileInput,
): Promise<UserProfile> {
  profile = {
    ...profile,
    displayName: input.displayName,
    schoolOrCompany: input.schoolOrCompany || null,
    currentPosition: input.currentPosition || null,
    defaultLanguage: input.defaultLanguage,
    targetPosition: input.targetPosition || null,
  };
  return delay(profile, 600);
}

/** `RequestMyEmailChange` — always succeeds in the mock; the real endpoint rejects an already-used email directly (logged-in user, no enumeration concern). */
export function fakeRequestEmailChange(
  _newEmail: string,
): Promise<{ ok: true }> {
  return delay({ ok: true }, 500);
}

const VALID_OTP = "123456";

/** `ConfirmMyEmailChange` — canned OTP `123456`, same convention as entities/auth's fake backend. */
export function fakeConfirmEmailChange(
  code: string,
  newEmail: string,
): Promise<{ ok: true } | { ok: false; message: string }> {
  if (code !== VALID_OTP) {
    return delay({ ok: false, message: "errors.otpInvalid" }, 400);
  }
  profile = { ...profile, email: newEmail };
  return delay({ ok: true }, 400);
}

export function fakeChangeMyPassword(
  input: ChangePasswordInput,
): Promise<{ ok: true } | { ok: false; message: string }> {
  if (profile.hasPassword && input.currentPassword !== "correct-password") {
    // Prototype convention: "correct-password" is the one value that passes the current-password
    // check, matching entities/auth's canned-credential approach (no real hashing to compare against).
    return delay({ ok: false, message: "errors.currentPasswordInvalid" }, 500);
  }
  profile = {
    ...profile,
    hasPassword: true,
    passwordChangedAt: new Date().toISOString().slice(0, 10),
  };
  return delay({ ok: true }, 500);
}

export function fakeGetMySettings(): Promise<AccountSettings> {
  return delay(settings);
}

export function fakeUpdateMySettings(
  input: AccountSettings,
): Promise<AccountSettings> {
  settings = input;
  return delay(settings, 600);
}

/** `ai-review`-owned, independent of `UpdateMySettings` (BR-07) — a separate mock function so a failure here never blocks the identity save. */
export function fakeUpdateInterviewPreferences(
  input: InterviewPracticePreferences,
): Promise<InterviewPracticePreferences> {
  interviewPreferences = input;
  return delay(interviewPreferences, 600);
}

export function fakeGetInterviewPreferences(): Promise<InterviewPracticePreferences> {
  return delay(interviewPreferences);
}

/** `ExportMyData` — mock returns a tiny in-memory file instead of a real report. */
export function fakeExportMyData(
  scope: "profile" | "submissions",
): Promise<Blob> {
  const body =
    scope === "profile"
      ? JSON.stringify(profile, null, 2)
      : "id,problemId,verdict\n";
  return delay(
    new Blob([body], {
      type: scope === "profile" ? "application/json" : "text/csv",
    }),
    700,
  );
}

export function fakeExportInterviewTranscripts(): Promise<Blob> {
  return delay(new Blob(["[]"], { type: "application/json" }), 700);
}

export function fakeDeleteMyAccount(
  emailConfirmation: string,
): Promise<{ ok: true } | { ok: false; message: string }> {
  if (emailConfirmation !== profile.email) {
    return delay({ ok: false, message: "errors.confirmPhraseMismatch" }, 400);
  }
  return delay({ ok: true }, 600);
}
