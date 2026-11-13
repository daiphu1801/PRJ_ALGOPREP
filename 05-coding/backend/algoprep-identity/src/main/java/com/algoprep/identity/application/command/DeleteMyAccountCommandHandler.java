package com.algoprep.identity.application.command;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.UnauthenticatedException;
import com.algoprep.identity.application.ports.in.DeleteMyAccountUseCase;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.AuditEntry;
import com.algoprep.identity.domain.model.MyProfile;
import com.algoprep.identity.domain.ports.out.AccountLifecycleRepository;
import com.algoprep.identity.domain.ports.out.AuditLogPort;
import com.algoprep.identity.domain.ports.out.MyProfileRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BR-08, the moment of deletion: the typed e-mail must equal the stored one after trimming,
 * ignoring case (owner decision D38 in the settings screen). DD validation F1 section 5 says
 * "exact, no case folding"; the two disagree, this follows the owner and the screen so a value the
 * screen accepts is never refused here. Then the account is deactivated as {@code SELF}, every
 * session is revoked and the request is audited. The access token in hand stops working at once
 * because the authentication converter requires {@code ACTIVE} on every request.
 *
 * <p>An {@code ADMIN} gets no restore ticket, so the sole active administrator is refused with
 * {@code IDT-432} (BR-15); the admin rows are locked first, so two admins deleting at once cannot
 * both pass.
 */
@Service
class DeleteMyAccountCommandHandler implements DeleteMyAccountUseCase {
  private final MyProfileRepository profiles;
  private final AccountLifecycleRepository accounts;
  private final AuditLogPort audit;

  DeleteMyAccountCommandHandler(
      MyProfileRepository profiles, AccountLifecycleRepository accounts, AuditLogPort audit) {
    this.profiles = profiles;
    this.accounts = accounts;
    this.audit = audit;
  }

  @Override
  @Transactional
  public void delete(UUID userId, String emailConfirmation) {
    MyProfile me = profiles.findById(userId).orElseThrow(DeleteMyAccountCommandHandler::gone);
    if (emailConfirmation == null || !emailConfirmation.trim().equalsIgnoreCase(me.email())) {
      throw new DomainRuleViolationException(
          IdentityErrorCodes.DELETE_CONFIRMATION_MISMATCH, "Typed e-mail does not match");
    }
    if (accounts.isLastActiveAdmin(userId)) {
      throw new DomainRuleViolationException(
          IdentityErrorCodes.LAST_ADMIN_PROTECTED,
          "The last active administrator cannot be deleted");
    }
    if (!accounts.deactivateSelf(userId)) {
      throw gone();
    }
    audit.record(
        new AuditEntry(
            userId, "SELF_ACCOUNT_DELETE_REQUESTED", "USER", userId.toString(), null, null));
  }

  private static UnauthenticatedException gone() {
    return new UnauthenticatedException(
        IdentityErrorCodes.REFRESH_TOKEN_INVALID, "Account is not available");
  }
}
