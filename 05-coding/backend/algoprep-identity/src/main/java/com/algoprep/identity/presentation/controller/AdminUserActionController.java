package com.algoprep.identity.presentation.controller;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.identity.application.dto.ChangeUserRoleCommand;
import com.algoprep.identity.application.dto.ChangeUserStatusCommand;
import com.algoprep.identity.application.ports.in.ChangeUserRoleUseCase;
import com.algoprep.identity.application.ports.in.ChangeUserStatusUseCase;
import com.algoprep.identity.application.ports.in.ResetUserPasswordUseCase;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.AdminUserItem;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Actions on one account, #36 to #38 (DD api F1 section 0.6): {@code ROLE_ADMIN} plus {@code
 * USER_MANAGEMENT:UPDATE}; denial is {@code 403 IDT-445}. A malformed {@code userId} answers 404
 * like an unknown one. Values that decide a rule ({@code targetStatus}, flags) are read as plain
 * objects so a wrong type is a rule violation instead of an unreadable-body 400.
 */
@RestController
@RequestMapping("/api/v1/identity/admin/users")
class AdminUserActionController {
  private final ChangeUserRoleUseCase changeRole;
  private final ChangeUserStatusUseCase changeStatus;
  private final ResetUserPasswordUseCase resetPassword;

  AdminUserActionController(
      ChangeUserRoleUseCase changeRole,
      ChangeUserStatusUseCase changeStatus,
      ResetUserPasswordUseCase resetPassword) {
    this.changeRole = changeRole;
    this.changeStatus = changeStatus;
    this.resetPassword = resetPassword;
  }

  record ChangeRoleRequest(String targetRoleId, Object acknowledgeSelfImpact) {}

  record ChangeStatusRequest(
      Object targetStatus, Object reason, Object notifyByEmail, Object acknowledgeSelfImpact) {}

  @PatchMapping("/{userId}/role")
  @PreAuthorize("hasRole('ADMIN') and hasAuthority('USER_MANAGEMENT:UPDATE')")
  AdminUserItem role(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable String userId,
      @RequestBody ChangeRoleRequest body) {
    return changeRole.change(
        new ChangeUserRoleCommand(
            UUID.fromString(jwt.getSubject()),
            id(userId),
            roleId(body.targetRoleId()),
            body.acknowledgeSelfImpact()));
  }

  @PatchMapping("/{userId}/status")
  @PreAuthorize("hasRole('ADMIN') and hasAuthority('USER_MANAGEMENT:UPDATE')")
  AdminUserItem status(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable String userId,
      @RequestBody ChangeStatusRequest body) {
    return changeStatus.change(
        new ChangeUserStatusCommand(
            UUID.fromString(jwt.getSubject()),
            id(userId),
            body.targetStatus(),
            body.reason(),
            body.notifyByEmail(),
            body.acknowledgeSelfImpact()));
  }

  @PostMapping("/{userId}/reset-password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('ADMIN') and hasAuthority('USER_MANAGEMENT:UPDATE')")
  void resetPassword(@AuthenticationPrincipal Jwt jwt, @PathVariable String userId) {
    resetPassword.reset(UUID.fromString(jwt.getSubject()), id(userId));
  }

  private static UUID id(String raw) {
    try {
      return UUID.fromString(raw);
    } catch (IllegalArgumentException e) {
      throw new ResourceNotFoundException(
          IdentityErrorCodes.USER_NOT_FOUND, "Account is not available");
    }
  }

  /** A malformed role id reaches the handler as null, which answers 422 like a missing one. */
  private static UUID roleId(String raw) {
    try {
      return raw == null ? null : UUID.fromString(raw);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
