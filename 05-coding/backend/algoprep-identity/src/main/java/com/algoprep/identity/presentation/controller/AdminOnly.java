package com.algoprep.identity.presentation.controller;

import com.algoprep.common.exception.PermissionDeniedException;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import org.springframework.security.core.Authentication;

/**
 * The "any ADMIN, no permission cell" gate of the audit-log endpoints (DD api F1 section 0.7).
 *
 * <p>Not {@code @PreAuthorize}: method security denials are always shaped as {@code IDT-445} by
 * {@code ApiAccessDeniedHandler}, but the DD wants {@code 403 IDT-447} here. Throwing from the
 * controller reaches {@code GlobalExceptionHandler}, which keeps the code, and it still runs before
 * any data is read.
 */
final class AdminOnly {
  private static final String ADMIN = "ROLE_ADMIN";

  private AdminOnly() {}

  static void require(Authentication caller) {
    boolean admin =
        caller != null
            && caller.getAuthorities().stream().anyMatch(a -> ADMIN.equals(a.getAuthority()));
    if (!admin) {
      throw new PermissionDeniedException(IdentityErrorCodes.ADMIN_ONLY, "Admin only");
    }
  }
}
