package com.algoprep.identity.application.command;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.identity.application.ports.in.DeleteRoleUseCase;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.AuditEntry;
import com.algoprep.identity.domain.model.RbacRole;
import com.algoprep.identity.domain.ports.out.AuditLogPort;
import com.algoprep.identity.domain.ports.out.RbacRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deletes an unused custom role and its cells with one {@code ROLE_DELETED} audit row in the same
 * transaction (#34, BR-14). A failing audit write fails the transaction and keeps the role.
 */
@Service
class DeleteRoleCommandHandler implements DeleteRoleUseCase {
  private final RbacRepository rbac;
  private final AuditLogPort audit;

  DeleteRoleCommandHandler(RbacRepository rbac, AuditLogPort audit) {
    this.rbac = rbac;
    this.audit = audit;
  }

  @Override
  @Transactional
  public void delete(UUID actorId, UUID roleId) {
    RbacRole role = rbac.lockRole(roleId).orElseThrow(RbacNotFound::exception);
    if (role.isSystem()) {
      throw new DomainRuleViolationException(
          IdentityErrorCodes.SYSTEM_ROLE_PROTECTED, "Built-in roles cannot be deleted");
    }
    if (rbac.countUsers(roleId) > 0 || !rbac.deleteRole(roleId)) {
      throw new DomainRuleViolationException(
          IdentityErrorCodes.ROLE_IN_USE, "The role is still assigned to accounts");
    }
    AuditEntry entry =
        new AuditEntry(
            actorId,
            "ROLE_DELETED",
            "ROLE",
            roleId.toString(),
            RbacAuditJson.of(
                "id",
                roleId.toString(),
                "name",
                role.name(),
                "baseCategory",
                role.baseCategory(),
                "isSystem",
                false),
            null);
    try {
      audit.record(entry);
    } catch (RuntimeException auditFailure) {
      // Rethrown as 503 IDT-446 (BR-14): the exception rolls the deletion back, the role stays.
      throw new ServiceUnavailableException(
          IdentityErrorCodes.ROLE_AUDIT_UNAVAILABLE,
          "The audit log is unavailable, the role was not deleted",
          auditFailure);
    }
  }
}
