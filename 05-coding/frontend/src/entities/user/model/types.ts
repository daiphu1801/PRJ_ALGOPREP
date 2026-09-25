// Role per README.md section 4, F1: STUDENT | INSTRUCTOR | ADMIN — determines which route group
// the user can enter (app/(student)|(instructor)|(admin)), BUT real authorization is enforced by
// the backend; hiding a button on the client is only UX (SYS0102_frontend_architecture.md section 4).
export type Role = "STUDENT" | "INSTRUCTOR" | "ADMIN";

export type User = {
  id: string;
  displayName: string;
  role: Role;
};

// PROTOTYPE — no DD/screen-axis DD yet. See 06-plan/PROTOTYPE_DEBT.md.
// Extends the `user` entity for USR0502_profile/USR0503_settings instead of a new entity, per
// task instruction — same real-world entity (`identity.users`), richer shape than the minimal
// `User` above (which only the nav/session flows need).

/** Submission language, per `identity.users.default_language` [SoT: 02-bd/database/identity.md:19-20]. */
export type SubmissionLanguage = "JAVA" | "CPP" | "PYTHON";

/** OAuth provider linked to the account [SoT: 02-bd/database/identity.md:70]. */
export type LinkedProvider = "GITHUB" | "GOOGLE";

/**
 * `MyProfileDto` [SoT: 02-bd/screens/users/USR0502_profile.md Sheet 7.1 rows 1-12]. `targetPosition`
 * is nullable rather than gated behind BD's Q2 — `03-dd/validation/identity.md` mục 5 (IDT-V08,
 * added today) gives it its own 100-char column, so Q2 reads as resolved even though the BD file
 * itself hasn't been re-opened to say so [SoT: 03-dd/validation/identity.md:47].
 */
export type UserProfile = {
  userId: string;
  displayName: string;
  email: string;
  schoolOrCompany: string | null;
  /** Self-declared job title, NOT the RBAC `role` [SoT: USR0502_profile.md Sheet 7.1 row 4]. */
  currentPosition: string | null;
  defaultLanguage: SubmissionLanguage;
  targetPosition: string | null;
  /** `password_hash IS NULL` when false — account has never set a password (OAuth-only). */
  hasPassword: boolean;
  passwordChangedAt: string | null;
  linkedProviders: LinkedProvider[];
  joinedAt: string;
  /** Display name of the RBAC role, read-only on this screen. */
  roleName: string;
};

export type UpdateProfileInput = {
  displayName: string;
  schoolOrCompany: string;
  currentPosition: string;
  defaultLanguage: SubmissionLanguage;
  targetPosition: string;
};

/** Interviewer depth for a SELF-PRACTICE mock-interview session — 4 levels per `DEC-2026-0922-users-and-admin-conflict-resolutions`. */
export type InterviewerLevel = "INTERN" | "JUNIOR" | "MIDDLE" | "SENIOR";

/**
 * `identity`-owned account preferences, `GetMySettings`/`UpdateMySettings`
 * [SoT: 03-dd/api/identity.md endpoints 14-15]. `preferredInterviewLevel` is the ONLY
 * interview-related field identity owns — max turns / hints belong to `ai-review` (BR-07,
 * saved via an independent `UpdateMyInterviewPreferences` call, see `InterviewPracticePreferences`
 * below) [SoT: 03-dd/logic/identity.md BR-07].
 */
export type AccountSettings = {
  workspace: {
    defaultLanguage: SubmissionLanguage;
    editorFontSize: "13" | "14" | "16";
    autosaveDraft: boolean;
    vimMode: boolean;
  };
  preferredInterviewLevel: InterviewerLevel;
  notifications: {
    streakReminderEnabled: boolean;
    weeklyReportEnabled: boolean;
  };
};

/**
 * `ai-review`-owned self-practice interview parameters (F5-28) — kept in this entity file only
 * because the settings screen edits both in one form; the mock still calls a SEPARATE mutation for
 * this (see api/mutations.ts `updateInterviewPracticePreferences`) so BR-07's "independent,
 * non-transactional" behaviour is real even at prototype stage, not just a comment.
 */
export type InterviewPracticePreferences = {
  maxTurnsPerSession: "8" | "12" | "16";
  hintAllowed: boolean;
};
