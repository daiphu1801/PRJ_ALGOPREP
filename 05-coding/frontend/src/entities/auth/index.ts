// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
export type {
  AuthMode,
  AuthOutcome,
  AuthFieldErrors,
  SignupOutcome,
} from "./model/types";
export {
  setMustChangePassword,
  useMustChangePassword,
} from "./model/must-change-password";
export {
  signupSchema,
  loginSchema,
  forgotEmailSchema,
  forgotOtpSchema,
  forgotResetSchema,
} from "./model/schema";
export type {
  SignupInput,
  LoginInput,
  ForgotEmailInput,
  ForgotOtpInput,
  ForgotResetInput,
} from "./model/schema";
export {
  signup,
  login,
  instructorLogin,
  oauthLogin,
  cancelDeactivation,
  logout,
  verifyEmail,
  resendVerification,
  forgotEmail,
  verifyOtp,
  resendOtp,
  resetPassword,
} from "./api/mutations";
