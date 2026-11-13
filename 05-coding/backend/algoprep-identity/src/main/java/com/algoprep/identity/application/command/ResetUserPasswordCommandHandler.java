package com.algoprep.identity.application.command;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.identity.application.ports.in.ResetUserPasswordUseCase;
import com.algoprep.identity.application.ports.out.EmailPort;
import com.algoprep.identity.application.ports.out.PasswordHasherPort;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.AdminUserItem;
import com.algoprep.identity.domain.model.AuditEntry;
import com.algoprep.identity.domain.ports.out.AdminUserRepository;
import com.algoprep.identity.domain.ports.out.AuditLogPort;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin-side "temporary password" (#38). The password is generated here, hashed, mailed to the
 * account and never returned, logged or audited; every refresh token is revoked and the user must
 * change it at the next login (soft flag, BR-21). An account that has no password (OAuth only) gets
 * guidance by e-mail and no change. The mail goes last, so a delivery failure rolls the whole
 * action back.
 */
@Service
class ResetUserPasswordCommandHandler implements ResetUserPasswordUseCase {
  private final AdminUserRepository users;
  private final PasswordHasherPort hasher;
  private final TemporaryPasswordGenerator generator;
  private final AuditLogPort audit;
  private final EmailPort email;

  ResetUserPasswordCommandHandler(
      AdminUserRepository users,
      PasswordHasherPort hasher,
      TemporaryPasswordGenerator generator,
      AuditLogPort audit,
      EmailPort email) {
    this.users = users;
    this.hasher = hasher;
    this.generator = generator;
    this.audit = audit;
    this.email = email;
  }

  @Override
  @Transactional
  public void reset(UUID actorId, UUID userId) {
    AdminUserItem user = users.lock(userId).orElseThrow(AdminReductionGuard::notFound);
    if (user.anonymized()) {
      throw new DomainRuleViolationException(
          IdentityErrorCodes.ACCOUNT_ANONYMIZED, "An anonymised account has no e-mail to reach");
    }
    if (!user.hasPassword()) {
      audit.record(entry(actorId, userId, "GUIDANCE_SENT"));
      email.send(
          user.email(),
          "AlgoPrep - Hướng dẫn đăng nhập",
          "Tài khoản của bạn đăng nhập bằng nhà cung cấp bên ngoài nên không có mật khẩu để đặt lại."
              + " Hãy đăng nhập bằng đúng nhà cung cấp đó.");
      return;
    }
    String temporary = generator.generate();
    users.setTemporaryPassword(userId, hasher.encode(temporary));
    users.revokeAllSessions(userId);
    audit.record(entry(actorId, userId, "TEMPORARY_PASSWORD_SENT"));
    email.send(
        user.email(),
        "AlgoPrep - Mật khẩu tạm thời",
        "Quản trị viên đã đặt lại mật khẩu của bạn. Mật khẩu tạm thời: "
            + temporary
            + ". Hãy đăng nhập và đổi mật khẩu ngay.");
  }

  private static AuditEntry entry(UUID actorId, UUID userId, String outcome) {
    return new AuditEntry(
        actorId,
        "USER_PASSWORD_RESET",
        "USER",
        userId.toString(),
        null,
        RbacAuditJson.of("outcome", outcome));
  }
}
