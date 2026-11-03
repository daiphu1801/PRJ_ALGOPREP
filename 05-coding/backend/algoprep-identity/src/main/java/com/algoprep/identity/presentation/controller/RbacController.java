package com.algoprep.identity.presentation.controller;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.identity.application.dto.CreateRoleCommand;
import com.algoprep.identity.application.dto.UpdateRolePermissionCommand;
import com.algoprep.identity.application.ports.in.CreateRoleUseCase;
import com.algoprep.identity.application.ports.in.DeleteRoleUseCase;
import com.algoprep.identity.application.ports.in.GetPermissionMatrixUseCase;
import com.algoprep.identity.application.ports.in.ListRolesUseCase;
import com.algoprep.identity.application.ports.in.UpdateRolePermissionUseCase;
import com.algoprep.identity.domain.exception.RbacErrorCodes;
import com.algoprep.identity.domain.model.PermissionChange;
import com.algoprep.identity.domain.model.PermissionMatrix;
import com.algoprep.identity.domain.model.RoleListItem;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Permission matrix and custom roles, #30 to #34. Layer 1 is {@code ROLE_ADMIN} (a custom role may
 * be ADMIN-based yet limited), writes also need the matching {@code PERMISSION_MATRIX} cell (BD
 * ADM0202 Sheet 9 NO 1 and 2). Denial is {@code 403 IDT-445}; the admin-only code IDT-447 is not
 * produced by the access-denied handler. Malformed ids answer 404 like unknown ones.
 */
@RestController
@RequestMapping("/api/v1/identity/rbac")
class RbacController {
  private final GetPermissionMatrixUseCase matrix;
  private final UpdateRolePermissionUseCase update;
  private final ListRolesUseCase roles;
  private final CreateRoleUseCase create;
  private final DeleteRoleUseCase delete;

  RbacController(
      GetPermissionMatrixUseCase matrix,
      UpdateRolePermissionUseCase update,
      ListRolesUseCase roles,
      CreateRoleUseCase create,
      DeleteRoleUseCase delete) {
    this.matrix = matrix;
    this.update = update;
    this.roles = roles;
    this.create = create;
    this.delete = delete;
  }

  record UpdateCellRequest(Boolean granted) {}

  record CreateRoleRequest(String name, String baseCategory) {}

  record RolesResponse(List<RoleListItem> items) {}

  @GetMapping("/matrix")
  @PreAuthorize("hasRole('ADMIN')")
  PermissionMatrix matrix() {
    return matrix.get();
  }

  @PatchMapping("/matrix/{roleId}/{functionId}/{actionId}")
  @PreAuthorize("hasRole('ADMIN') and hasAuthority('PERMISSION_MATRIX:UPDATE')")
  PermissionChange update(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable String roleId,
      @PathVariable String functionId,
      @PathVariable String actionId,
      @RequestBody UpdateCellRequest body) {
    return update.update(
        new UpdateRolePermissionCommand(
            UUID.fromString(jwt.getSubject()),
            id(roleId),
            id(functionId),
            id(actionId),
            body.granted()));
  }

  @GetMapping("/roles")
  @PreAuthorize("hasRole('ADMIN')")
  RolesResponse roles() {
    return new RolesResponse(roles.list());
  }

  @PostMapping("/roles")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN') and hasAuthority('PERMISSION_MATRIX:CREATE')")
  RoleListItem createRole(@AuthenticationPrincipal Jwt jwt, @RequestBody CreateRoleRequest body) {
    return create.create(
        new CreateRoleCommand(UUID.fromString(jwt.getSubject()), body.name(), body.baseCategory()));
  }

  @DeleteMapping("/roles/{roleId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('ADMIN') and hasAuthority('PERMISSION_MATRIX:DELETE')")
  void deleteRole(@AuthenticationPrincipal Jwt jwt, @PathVariable String roleId) {
    delete.delete(UUID.fromString(jwt.getSubject()), id(roleId));
  }

  private static UUID id(String raw) {
    try {
      return UUID.fromString(raw);
    } catch (IllegalArgumentException e) {
      throw new ResourceNotFoundException(
          RbacErrorCodes.RBAC_TARGET_NOT_FOUND, "Role, function or action is not available");
    }
  }
}
