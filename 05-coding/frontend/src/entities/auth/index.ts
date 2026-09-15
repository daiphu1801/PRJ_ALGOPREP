// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
export type { AuthMode, AuthOutcome, AuthFieldErrors } from "./model/types";
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
  oauthLogin,
  cancelDeactivation,
  forgotEmail,
  verifyOtp,
  resendOtp,
  resetPassword,
} from "./api/mutations";
