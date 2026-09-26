export type { User, Role } from "./model/types";
export type { AppArea } from "./model/area";
export { ROLE_BY_AREA, HOME_PATH_BY_ROLE } from "./model/area";

// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
export type {
  AccountSettings,
  InterviewerLevel,
  InterviewPracticePreferences,
  LinkedProvider,
  SubmissionLanguage,
  UpdateProfileInput,
  UserProfile,
} from "./model/types";
export {
  changePasswordSchema,
  deleteAccountSchema,
  emailOtpSchema,
  profileFormSchema,
  type ChangePasswordInput,
  type EmailOtpInput,
  type ProfileFormInput,
} from "./model/schema";
export { useInterviewPreferences, useMyProfile, useMySettings } from "./api/queries";
export {
  changeMyPassword,
  confirmEmailChange,
  deleteMyAccount,
  exportMyData,
  exportMyInterviewTranscripts,
  requestEmailChange,
  updateInterviewPracticePreferences,
  updateMyProfile,
  updateMySettings,
} from "./api/mutations";
