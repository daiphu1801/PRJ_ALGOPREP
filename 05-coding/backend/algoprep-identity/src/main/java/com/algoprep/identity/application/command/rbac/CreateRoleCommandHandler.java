package com.algoprep.identity.application.command;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.ResourceConflictException;
import com.algoprep.identity.application.dto.CreateRoleCommand;
import com.algoprep.identity.application.ports.in.CreateRoleUseCase;
import com.algoprep.identity.domain.exception.RbacErrorCodes;
import com.algoprep.identity.domain.model.AuditEntry;
import com.algoprep.identity.domain.model.RbacRole;
import com.algoprep.identity.domain.model.RoleListItem;
import com.algoprep.identity.domain.ports.out.AuditLogPort;
import com.algoprep.identity.domain.ports.out.RbacRepository;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates a custom role with no permission (#33). The code is {@code role_} plus the lower-cased
 * name with blanks turned into underscores, the rule of the admin prototype; the name may not match
 * an existing code or label, ignoring case.
 */
@Service
class CreateRoleCommandHandler implements CreateRoleUseCase {
  static final int NAME_MAX = 50;
  private static final Set<String> CATEGORIES = Set.of("STUDENT", "INSTRUCTOR", "ADMIN");

  private final RbacRepository rbac;
  private final AuditLogPort audit;
  private final IdGeneratorPort ids;

  CreateRoleCommandHandler(RbacRepository rbac, AuditLogPort audit, IdGeneratorPort ids) {
    this.rbac = rbac;
    this.audit = audit;
    this.ids = ids;
  }

  @Override
  @Transactional
  public RoleListItem create(CreateRoleCommand command) {
    String name = command.name() == null ? "" : command.name().trim();
    if (name.isEmpty()) {
      throw invalid(RbacErrorCodes.ROLE_NAME_REQUIRED, "Role name is required");
    }
    if (name.length() > NAME_MAX) {
      throw invalid(RbacErrorCodes.ROLE_NAME_TOO_LONG, "Role name is too long");
    }
    if (command.baseCategory() == null || !CATEGORIES.contains(command.baseCategory())) {
      throw invalid(RbacErrorCodes.ROLE_BASE_CATEGORY_INVALID, "Base category is not valid");
    }
    String code = "role_" + name.toLowerCase(Locale.ROOT).replaceAll("\\s+", "_");
    if (rbac.roleNameTaken(code, name)) {
      throw duplicate();
    }
    UUID id = ids.newId();
    if (!rbac.insertRole(new RbacRole(id, code, name, command.baseCategory(), false))) {
      throw duplicate();
    }
    audit.record(
        new AuditEntry(
            command.actorId(),
            "ROLE_CREATED",
            "ROLE",
            id.toString(),
            null,
            RbacAuditJson.of("name", name, "baseCategory", command.baseCategory())));
    return new RoleListItem(id, code, name, command.baseCategory(), false, 0);
  }

  private static DomainRuleViolationException invalid(String code, String message) {
    return new DomainRuleViolationException(code, message);
  }

  private static ResourceConflictException duplicate() {
    return new ResourceConflictException(
        RbacErrorCodes.ROLE_NAME_DUPLICATE, "A role with this name already exists");
  }
}
