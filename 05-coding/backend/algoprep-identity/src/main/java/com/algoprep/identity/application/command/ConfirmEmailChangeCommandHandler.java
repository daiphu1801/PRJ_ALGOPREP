package com.algoprep.identity.application.command;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.ResourceConflictException;
import com.algoprep.identity.application.dto.ConfirmEmailChangeCommand;
import com.algoprep.identity.application.ports.in.ConfirmEmailChangeUseCase;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.AuditEntry;
import com.algoprep.identity.domain.ports.out.AuditLogPort;
import com.algoprep.identity.domain.ports.out.MyProfileRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * #12: a wrong, expired or used code all answer the same {@code 422 IDT-416}. If another account
 * took the address between #11 and #12 the unique index refuses it and the old e-mail stays.
 */
@Service
class ConfirmEmailChangeCommandHandler implements ConfirmEmailChangeUseCase {
  private final MyProfileRepository profiles;
  private final EmailChangeService change;
  private final AuditLogPort audit;

  ConfirmEmailChangeCommandHandler(
      MyProfileRepository profiles, EmailChangeService change, AuditLogPort audit) {
    this.profiles = profiles;
    this.change = change;
    this.audit = audit;
  }

  @Override
  @Transactional
  public String confirm(ConfirmEmailChangeCommand command) {
    String code = command.code();
    if (code == null || !code.matches("[0-9]{6}")) {
      throw invalid();
    }
    UUID id = command.userId();
    String newEmail = change.match(id, code).orElseThrow(ConfirmEmailChangeCommandHandler::invalid);
    if (!profiles.updateEmail(id, newEmail)) {
      throw new ResourceConflictException(
          IdentityErrorCodes.EMAIL_TAKEN, "This e-mail already belongs to an account");
    }
    // No before/after: both would be personal data, and the row still names the actor.
    audit.record(new AuditEntry(id, "EMAIL_CHANGED", "USER", id.toString(), null, null));
    change.clearAfterCommit(id, code);
    return newEmail;
  }

  private static DomainRuleViolationException invalid() {
    return new DomainRuleViolationException(
        IdentityErrorCodes.EMAIL_CHANGE_CODE_INVALID, "Code is invalid or unavailable");
  }
}
