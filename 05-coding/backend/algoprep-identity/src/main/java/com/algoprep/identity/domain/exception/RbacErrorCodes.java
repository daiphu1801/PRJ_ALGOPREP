package com.algoprep.identity.domain.exception;

/**
 * Error codes of the permission matrix and custom role endpoints (#30 to #34) that are not in
 * {@link IdentityErrorCodes}. Codes 420 to 425 and V22 to V25 are proposals {@code [SoT: Suy luận]}
 * until the DD error table lists them; IDT-420 is named only in DD logic BR-14.
 */
public final class RbacErrorCodes {

  /**
   * Switching off the last {@code PERMISSION_MATRIX:UPDATE} cell of every ADMIN role (HTTP 422).
   */
  public static final String LAST_MATRIX_ADMIN_CELL = "IDT-420";

  /** The built-in STUDENT role is not configurable through the matrix (HTTP 422). */
  public static final String STUDENT_MATRIX_READ_ONLY = "IDT-423";

  /** Role, function or action of the request does not exist, or the id is malformed (HTTP 404). */
  public static final String RBAC_TARGET_NOT_FOUND = "IDT-424";

  /** A role with the same generated code or the same label (case-insensitive) exists (HTTP 409). */
  public static final String ROLE_NAME_DUPLICATE = "IDT-425";

  /** Role name is empty after trimming (HTTP 422). */
  public static final String ROLE_NAME_REQUIRED = "IDT-V22";

  /** Role name is longer than 50 characters after trimming (HTTP 422). */
  public static final String ROLE_NAME_TOO_LONG = "IDT-V23";

  /** Base category is not one of STUDENT, INSTRUCTOR, ADMIN (HTTP 422). */
  public static final String ROLE_BASE_CATEGORY_INVALID = "IDT-V24";

  /** The {@code granted} flag of a matrix cell is missing (HTTP 422). */
  public static final String GRANTED_REQUIRED = "IDT-V25";

  private RbacErrorCodes() {}
}
