package com.algoprep.identity.domain.exception;

/** Stable {@code IDT-nnn} error codes of module identity (DD api F1 section 0.4.3 and 0.5). */
public final class IdentityErrorCodes {

  /** Authenticated caller lacks the required {@code Function:Action} cell (HTTP 403). */
  public static final String PERMISSION_DENIED = "IDT-445";

  /** Valid JWT but {@code baseCategory} is not ADMIN on an admin-only endpoint (HTTP 403). */
  public static final String ADMIN_ONLY = "IDT-447";

  private IdentityErrorCodes() {}
}
