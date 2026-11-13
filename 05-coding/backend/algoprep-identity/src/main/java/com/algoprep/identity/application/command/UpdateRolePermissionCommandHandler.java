package com.algoprep.identity.application.command;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.identity.application.dto.UpdateRolePermissionCommand;
import com.algoprep.identity.application.ports.in.UpdateRolePermissionUseCase;
import com.algoprep.identity.domain.exception.RbacErrorCodes;
import com.algoprep.identity.domain.model.AuditEntry;
import com.algoprep.identity.domain.model.PermissionChange;
import com.algoprep.identity.domain.model.RbacRole;
import com.algoprep.identity.domain.ports.out.AuditLogPort;
import com.algoprep.identity.domain.ports.out.RbacRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * One matrix cell on or off (#31, BR-13). Cell, version bump and audit row commit together, so a
 * request that starts after the response reads the new {@code permission_version}; that is the
 * whole "effective immediately" mechanism, the converter already compares the version on every
 * request. A repeat of the current value changes nothing: no bump, no audit.
 */
@Service
class UpdateRolePermissionCommandHandler implements UpdateRolePermissionUseCase {
  private static final String MATRIX = "PERMISSION_MATRIX";
  private static final String UPDATE = "UPDATE";

  private final RbacRepository rbac;
  private final AuditLogPort audit;

  UpdateRolePermissionCommandHandler(RbacRepository rbac, AuditLogPort audit) {
    this.rbac = rbac;
    this.audit = audit;
  }

  @Override
  @Transactional
  public PermissionChange update(UpdateRolePermissionCommand command) {
    if (command.granted() == null) {
      throw new DomainRuleViolationException(
          RbacErrorCodes.GRANTED_REQUIRED, "The granted flag is required");
    }
    boolean granted = command.granted();
    RbacRole role =
        rbac.lockForPermissionChange(command.roleId()).orElseThrow(RbacNotFound::exception);
    String function = rbac.functionCode(command.functionId()).orElseThrow(RbacNotFound::exception);
    String action = rbac.actionCode(command.actionId()).orElseThrow(RbacNotFound::exception);
    if (role.isSystem() && "STUDENT".equals(role.code())) {
      throw new DomainRuleViolationException(
          RbacErrorCodes.STUDENT_MATRIX_READ_ONLY, "The STUDENT role is not configurable");
    }
    boolean current = rbac.isGranted(role.id(), command.functionId(), command.actionId());
    PermissionChange same =
        new PermissionChange(role.id(), command.functionId(), command.actionId(), granted, false);
    if (current == granted) {
      return same;
    }
    if (!granted && removesLastAdminEditor(role, function, action)) {
      throw new DomainRuleViolationException(
          RbacErrorCodes.LAST_MATRIX_ADMIN_CELL,
          "At least one admin role must keep the right to edit the matrix");
    }
    rbac.setGranted(
        role.id(), command.functionId(), command.actionId(), granted, command.actorId());
    rbac.bumpPermissionVersion(role.id());
    audit.record(
        new AuditEntry(
            command.actorId(),
            "ROLE_PERMISSION_CHANGED",
            "ROLE",
            role.id().toString(),
            RbacAuditJson.of("function", function, "action", action, "granted", current),
            RbacAuditJson.of("function", function, "action", action, "granted", granted)));
    return new PermissionChange(role.id(), command.functionId(), command.actionId(), granted, true);
  }

  private boolean removesLastAdminEditor(RbacRole role, String function, String action) {
    return "ADMIN".equals(role.baseCategory())
        && MATRIX.equals(function)
        && UPDATE.equals(action)
        && rbac.activeMatrixEditorsOutsideRole(role.id()) == 0;
  }
}
