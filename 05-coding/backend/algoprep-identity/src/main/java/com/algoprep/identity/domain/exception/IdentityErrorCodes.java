package com.algoprep.identity.domain.exception;

/** Stable {@code IDT-nnn} error codes of module identity (DD api F1 section 0.4.3, 0.5 and 1). */
public final class IdentityErrorCodes {

  /** Wrong email or password, or an account that cannot log in with a password (HTTP 401). */
  public static final String INVALID_CREDENTIALS = "IDT-102";

  /** Five failed attempts in the current email/IP window (HTTP 429). */
  public static final String LOGIN_RATE_LIMITED = "IDT-103";

  /** Account is {@code DEACTIVATED} (HTTP 403). */
  public static final String ACCOUNT_DEACTIVATED = "IDT-104";

  /** Refresh token unknown, expired or its owner is no longer active (HTTP 401). */
  public static final String REFRESH_TOKEN_INVALID = "IDT-105";

  /** Revoked refresh token presented again; the whole family is revoked (HTTP 401). */
  public static final String REFRESH_TOKEN_REUSED = "IDT-106";

  /** Account is not an instructor but used the instructor login (HTTP 403). */
  public static final String NOT_AN_INSTRUCTOR = "IDT-111";

  /** Self-registered account has not confirmed its email OTP yet (HTTP 403). */
  public static final String EMAIL_NOT_VERIFIED = "IDT-113";

  /** {@code currentPassword} of {@code POST /me/password} does not match (HTTP 422). */
  public static final String CURRENT_PASSWORD_INCORRECT = "IDT-115";

  /** E-mail typed to confirm self-deletion does not match the account's (HTTP 422). */
  public static final String DELETE_CONFIRMATION_MISMATCH = "IDT-201";

  /** Class invite code unknown, malformed or expired (HTTP 422). */
  public static final String INVITE_CODE_INVALID = "IDT-410";

  /** Class is not among the caller's current enrolments (HTTP 404). */
  public static final String MY_CLASS_NOT_FOUND = "IDT-411";

  /** Five wrong invite codes inside 15 minutes (HTTP 429). */
  public static final String INVITE_CODE_RATE_LIMITED = "IDT-412";

  /** Class missing, malformed id or not owned by the instructor (HTTP 404). */
  public static final String CLASS_NOT_FOUND = "IDT-414";

  /** New e-mail already belongs to an account, possibly the caller's own (HTTP 409). */
  public static final String EMAIL_TAKEN = "IDT-415";

  /** E-mail change code wrong, expired or already used (HTTP 422). */
  public static final String EMAIL_CHANGE_CODE_INVALID = "IDT-416";

  /** The change would leave no active ADMIN account (HTTP 422, BR-15). */
  public static final String LAST_ADMIN_PROTECTED = "IDT-432";

  /** The audit row of a role deletion could not be written; the deletion was undone (HTTP 503). */
  public static final String ROLE_AUDIT_UNAVAILABLE = "IDT-446";

  /** Built-in role cannot be deleted (HTTP 422). */
  public static final String SYSTEM_ROLE_PROTECTED = "IDT-421";

  /** Role still assigned to users cannot be deleted (HTTP 422). */
  public static final String ROLE_IN_USE = "IDT-422";

  /** Admin-created account e-mail already exists (HTTP 409). */
  public static final String ADMIN_CREATE_EMAIL_TAKEN = "IDT-440";

  /** Target role of an admin-created account is not valid (HTTP 422). */
  public static final String ADMIN_CREATE_ROLE_INVALID = "IDT-441";

  /** Log retention days is not an integer of at least 1 (HTTP 422). */
  public static final String LOG_RETENTION_INVALID = "IDT-442";

  /** Audit log date, group or cursor filter, or CSV column, is invalid (HTTP 400). */
  public static final String AUDIT_FILTER_INVALID = "IDT-443";

  /** Audit log search keyword longer than 100 characters (HTTP 400). */
  public static final String AUDIT_SEARCH_TOO_LONG = "IDT-444";

  /** Admin user list filter or paging parameter is invalid (HTTP 400). */
  public static final String LIST_PARAMETER_INVALID = "IDT-V17";

  /** A locked account that was anonymised cannot be unlocked or sent a password (HTTP 422). */
  public static final String ACCOUNT_ANONYMIZED = "IDT-448";

  /** The account addressed by an admin user action does not exist (HTTP 404) [SoT: Suy luan]. */
  public static final String USER_NOT_FOUND = "IDT-450";

  /** An admin hits their own account without {@code acknowledgeSelfImpact} (HTTP 400). */
  public static final String SELF_IMPACT_NOT_ACKNOWLEDGED = "IDT-V21";

  /** Body of an admin user action is not valid [SoT: Suy luan] (HTTP 422). */
  public static final String ADMIN_USER_REQUEST_INVALID = "IDT-V26";

  /** OAuth {@code state} unknown, expired, used, or the provider refused the code (HTTP 401). */
  public static final String OAUTH_STATE_INVALID = "IDT-109";

  /** The provider did not verify the e-mail, so no account is linked or created (HTTP 422). */
  public static final String OAUTH_EMAIL_UNVERIFIED = "IDT-110";

  /** The provider is not configured or did not answer (HTTP 503). */
  public static final String OAUTH_UNAVAILABLE = "IDT-505";

  /** The path names a provider other than {@code github} or {@code google} (HTTP 400). */
  public static final String OAUTH_PROVIDER_UNKNOWN = "IDT-V28";

  /** The verification e-mail could not be sent, so no account or code is kept (HTTP 503). */
  public static final String VERIFICATION_EMAIL_UNAVAILABLE = "IDT-504";

  /** A block of the admin overview could not read the judge or problem-bank data (HTTP 503). */
  public static final String DASHBOARD_SOURCE_UNAVAILABLE = "IDT-506";

  /** The judge or problem-bank figures a class read needs could not be read (HTTP 503). */
  public static final String CLASS_METRICS_UNAVAILABLE = "IDT-507";

  /** The submission history the personal data export needs could not be read (HTTP 503). */
  public static final String SUBMISSION_EXPORT_UNAVAILABLE = "IDT-508";

  /** {@code problemIds} of #48 is empty, has a malformed UUID or more than 100 ids (HTTP 422). */
  public static final String PROBLEM_IDS_INVALID = "IDT-V27";

  /** {@code X-Requested-With} header missing on the cookie-based refresh call (HTTP 400). */
  public static final String MISSING_REQUESTED_WITH = "IDT-V11";

  /** Authenticated caller lacks the required {@code Function:Action} cell (HTTP 403). */
  public static final String PERMISSION_DENIED = "IDT-445";

  /** Valid JWT but {@code baseCategory} is not ADMIN on an admin-only endpoint (HTTP 403). */
  public static final String ADMIN_ONLY = "IDT-447";

  private IdentityErrorCodes() {}
}
