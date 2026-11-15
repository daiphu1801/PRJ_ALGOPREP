package com.algoprep.identity.application.command;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.identity.application.dto.ChangeUserRoleCommand;
import com.algoprep.identity.application.ports.in.ChangeUserRoleUseCase;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.exception.RbacErrorCodes;
import com.algoprep.identity.domain.model.AdminUserItem;
import com.algoprep.identity.domain.model.AuditEntry;
import com.algoprep.identity.domain.model.RbacRole;
import com.algoprep.identity.domain.ports.out.AdminUserRepository;
import com.algoprep.identity.domain.ports.out.AuditLogPort;
import com.algoprep.identity.domain.ports.out.RbacRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Moves an account to another role (#36). Taking an active ADMIN out of the admin category is
 * protected by BR-15 ({@code IDT-432}, then {@code IDT-V21} for a self-demotion). The change is
 * live at once: the authentication converter re-reads role and matrix on every request. Choosing
 * the role the account already has changes nothing and writes no audit row.
 */
@Service
class ChangeUserRoleCommandHandler implements ChangeUserRoleUseCase {
  private final AdminUserRepository users;
  private final RbacRepository rbac;
  private final AdminReductionGuard guard;
  private final AuditLogPort audit;

  ChangeUserRoleCommandHandler(
      AdminUserRepository users,
      RbacRepository rbac,
      AdminReductionGuard guard,
      AuditLogPort audit) {
    this.users = users;
    this.rbac = rbac;
    this.guard = guard;
    this.audit = audit;
  }

  @Override
  @Transactional
  public AdminUserItem change(ChangeUserRoleCommand command) {
    if (command.targetRoleId() == null) {
      throw new DomainRuleViolationException(
          IdentityErrorCodes.ADMIN_USER_REQUEST_INVALID, "targetRoleId is required");
    }
    AdminUserItem peek = users.find(command.userId()).orElseThrow(AdminReductionGuard::notFound);
    RbacRole role =
        rbac.lockRole(command.targetRoleId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        RbacErrorCodes.RBAC_TARGET_NOT_FOUND, "Role is not available"));
    if (peek.roleId().equals(role.id())) {
      return peek;
    }
    // An active admin who moves to a non-ADMIN role, or to an ADMIN role that cannot edit the
    // matrix,
    // leaves the admin set or the repair path: both are guarded the same way.
    if (AdminReductionGuard.isActiveAdmin(peek) && !rbac.isMatrixEditorRole(role.id())) {
      guard.guard(command.actorId(), command.userId(), command.acknowledgeSelfImpact());
    }
    AdminUserItem current = users.lock(command.userId()).orElseThrow(AdminReductionGuard::notFound);
    users.changeRole(command.userId(), role.id());
    audit.record(
        new AuditEntry(
            command.actorId(),
            "USER_ROLE_CHANGED",
            "USER",
            command.userId().toString(),
            RbacAuditJson.of("roleId", current.roleId(), "roleName", current.roleName()),
            RbacAuditJson.of("roleId", role.id(), "roleName", role.name())));
    return users.find(command.userId()).orElseThrow(AdminReductionGuard::notFound);
  }
}
