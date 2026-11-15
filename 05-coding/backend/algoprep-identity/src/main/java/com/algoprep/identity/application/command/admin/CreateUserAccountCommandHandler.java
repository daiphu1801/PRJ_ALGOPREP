package com.algoprep.identity.application.command;

import com.algoprep.common.exception.ResourceConflictException;
import com.algoprep.identity.application.command.NewAdminAccountFactory.NewAccount;
import com.algoprep.identity.application.dto.CreateUserAccountCommand;
import com.algoprep.identity.application.ports.in.CreateUserAccountUseCase;
import com.algoprep.identity.application.ports.out.EmailPort;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.AdminUserItem;
import com.algoprep.identity.domain.model.AuditEntry;
import com.algoprep.identity.domain.ports.out.AdminUserRepository;
import com.algoprep.identity.domain.ports.out.AuditLogPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin-created account (#50, BR-21): {@code PENDING_VERIFICATION}, a generated temporary password
 * sent by e-mail (no OTP) and {@code must_change_password = true}; the first login with it makes
 * the account {@code ACTIVE}. Only non-ADMIN roles are allowed ({@code IDT-441}). The password is
 * neither stored in clear nor audited, and the mail goes last so a delivery failure undoes the
 * account.
 */
@Service
class CreateUserAccountCommandHandler implements CreateUserAccountUseCase {
  private final AdminUserRepository users;
  private final NewAdminAccountFactory factory;
  private final AuditLogPort audit;
  private final EmailPort email;

  CreateUserAccountCommandHandler(
      AdminUserRepository users,
      NewAdminAccountFactory factory,
      AuditLogPort audit,
      EmailPort email) {
    this.users = users;
    this.factory = factory;
    this.audit = audit;
    this.email = email;
  }

  @Override
  @Transactional
  public AdminUserItem create(CreateUserAccountCommand command) {
    NewAccount account = factory.prepare(command);
    if (!users.insertCreatedByAdmin(
        account.id(),
        account.email(),
        account.passwordHash(),
        account.displayName(),
        account.role().id())) {
      throw new ResourceConflictException(
          IdentityErrorCodes.ADMIN_CREATE_EMAIL_TAKEN, "An account with this e-mail exists");
    }
    audit.record(
        new AuditEntry(
            command.actorId(),
            "USER_CREATED",
            "USER",
            account.id().toString(),
            null,
            RbacAuditJson.of("roleId", account.role().id(), "roleName", account.role().name())));
    email.send(
        account.email(),
        "AlgoPrep - Tài khoản mới",
        "Quản trị viên đã tạo tài khoản AlgoPrep cho bạn. Mật khẩu tạm thời: "
            + account.temporaryPassword()
            + ". Hãy đăng nhập và đổi mật khẩu ở lần đăng nhập đầu tiên.");
    return users.find(account.id()).orElseThrow(AdminReductionGuard::notFound);
  }
}
