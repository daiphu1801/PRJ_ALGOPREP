package com.algoprep.identity.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.identity.application.dto.ChangeUserRoleCommand;
import com.algoprep.identity.application.dto.ChangeUserStatusCommand;
import com.algoprep.identity.application.dto.CreateUserAccountCommand;
import com.algoprep.identity.application.ports.out.EmailPort;
import com.algoprep.identity.application.ports.out.PasswordHasherPort;
import com.algoprep.identity.domain.model.AdminUserItem;
import com.algoprep.identity.domain.model.AuditEntry;
import com.algoprep.identity.domain.model.RbacRole;
import com.algoprep.identity.domain.ports.out.AccountLifecycleRepository;
import com.algoprep.identity.domain.ports.out.AdminUserRepository;
import com.algoprep.identity.domain.ports.out.AuditLogPort;
import com.algoprep.identity.domain.ports.out.RbacRepository;
import jakarta.validation.Validation;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** #36 to #38 and #50: the rules of the handlers with the ports mocked. */
class AdminUserHandlersTest {
  private final AdminUserRepository users = mock(AdminUserRepository.class);
  private final RbacRepository rbac = mock(RbacRepository.class);
  private final AccountLifecycleRepository accounts = mock(AccountLifecycleRepository.class);
  private final AuditLogPort audit = mock(AuditLogPort.class);
  private final EmailPort email = mock(EmailPort.class);
  private final PasswordHasherPort hasher = mock(PasswordHasherPort.class);
  private final IdGeneratorPort ids = mock(IdGeneratorPort.class);
  private final AdminReductionGuard guard = new AdminReductionGuard(accounts);
  private final TemporaryPasswordGenerator generator = new TemporaryPasswordGenerator();

  private final ChangeUserRoleCommandHandler roleHandler =
      new ChangeUserRoleCommandHandler(users, rbac, guard, audit);
  private final ChangeUserStatusCommandHandler statusHandler =
      new ChangeUserStatusCommandHandler(users, guard, audit, email);
  private final ResetUserPasswordCommandHandler resetHandler =
      new ResetUserPasswordCommandHandler(users, hasher, generator, audit, email);
  private final CreateUserAccountCommandHandler createHandler =
      new CreateUserAccountCommandHandler(
          users,
          new NewAdminAccountFactory(
              new RegistrationValidation(Validation.buildDefaultValidatorFactory().getValidator()),
              rbac,
              ids,
              hasher,
              generator),
          audit,
          email);

  private final UUID actor = UUID.randomUUID();
  private final UUID target = UUID.randomUUID();
  private final UUID studentRole = UUID.randomUUID();
  private final UUID adminRole = UUID.randomUUID();

  private AdminUserItem user(String category, String status, boolean hasPassword, boolean anon) {
    UUID role = "ADMIN".equals(category) ? adminRole : studentRole;
    return new AdminUserItem(
        target,
        "Name",
        "n@example.test",
        role,
        category,
        category,
        status,
        null,
        hasPassword,
        anon,
        0,
        0);
  }

  private static void assertCode(Throwable error, String code) {
    assertThat(error)
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo(code));
  }

  private void present(AdminUserItem item) {
    when(users.find(target)).thenReturn(Optional.of(item));
    when(users.lock(target)).thenReturn(Optional.of(item));
  }

  // ---- #36 -----------------------------------------------------------------------------------

  @Test
  void roleChangeMovesTheUserAndWritesOneAuditRow() {
    present(user("STUDENT", "ACTIVE", true, false));
    UUID instructor = UUID.randomUUID();
    when(rbac.lockRole(instructor))
        .thenReturn(
            Optional.of(new RbacRole(instructor, "INSTRUCTOR", "Instructor", "INSTRUCTOR", true)));

    roleHandler.change(new ChangeUserRoleCommand(actor, target, instructor, null));

    verify(users).changeRole(target, instructor);
    ArgumentCaptor<AuditEntry> entry = ArgumentCaptor.forClass(AuditEntry.class);
    verify(audit).record(entry.capture());
    assertThat(entry.getValue().actionType()).isEqualTo("USER_ROLE_CHANGED");
    assertThat(entry.getValue().targetId()).isEqualTo(target.toString());
  }

  @Test
  void roleChangeToTheSameRoleDoesNothing() {
    AdminUserItem item = user("STUDENT", "ACTIVE", true, false);
    present(item);
    when(rbac.lockRole(studentRole))
        .thenReturn(Optional.of(new RbacRole(studentRole, "STUDENT", "Student", "STUDENT", true)));

    assertThat(roleHandler.change(new ChangeUserRoleCommand(actor, target, studentRole, null)))
        .isSameAs(item);

    verify(users, never()).changeRole(any(), any());
    verify(audit, never()).record(any());
  }

  @Test
  void roleChangeRefusesMissingTargetsAndUnknownRoles() {
    assertThatThrownBy(
            () -> roleHandler.change(new ChangeUserRoleCommand(actor, target, null, null)))
        .satisfies(e -> assertCode(e, "IDT-V26"));
    when(users.find(target)).thenReturn(Optional.empty());
    assertThatThrownBy(
            () -> roleHandler.change(new ChangeUserRoleCommand(actor, target, studentRole, null)))
        .satisfies(e -> assertCode(e, "IDT-450"));
    present(user("STUDENT", "ACTIVE", true, false));
    when(rbac.lockRole(any())).thenReturn(Optional.empty());
    assertThatThrownBy(
            () ->
                roleHandler.change(
                    new ChangeUserRoleCommand(actor, target, UUID.randomUUID(), null)))
        .satisfies(e -> assertCode(e, "IDT-424"));
    verify(audit, never()).record(any());
  }

  @Test
  void demotingTheLastActiveAdminIsIdt432EvenWhenAcknowledged() {
    present(user("ADMIN", "ACTIVE", true, false));
    when(rbac.lockRole(studentRole))
        .thenReturn(Optional.of(new RbacRole(studentRole, "STUDENT", "Student", "STUDENT", true)));
    when(accounts.isLastActiveAdmin(target)).thenReturn(true);

    assertThatThrownBy(
            () -> roleHandler.change(new ChangeUserRoleCommand(actor, target, studentRole, true)))
        .satisfies(e -> assertCode(e, "IDT-432"));
    verify(users, never()).changeRole(any(), any());
  }

  @Test
  void demotingYourselfNeedsTheFlagButAnotherAdminDoesNot() {
    present(user("ADMIN", "ACTIVE", true, false));
    when(rbac.lockRole(studentRole))
        .thenReturn(Optional.of(new RbacRole(studentRole, "STUDENT", "Student", "STUDENT", true)));

    assertThatThrownBy(
            () -> roleHandler.change(new ChangeUserRoleCommand(target, target, studentRole, null)))
        .satisfies(e -> assertCode(e, "IDT-V21"));
    roleHandler.change(new ChangeUserRoleCommand(target, target, studentRole, true));
    roleHandler.change(new ChangeUserRoleCommand(actor, target, studentRole, null));
    verify(users, org.mockito.Mockito.times(2)).changeRole(target, studentRole);
  }

  @Test
  void moveBetweenAdminCategoryRolesAndInactiveAdminsSkipTheGuard() {
    UUID other = UUID.randomUUID();
    when(rbac.lockRole(other))
        .thenReturn(Optional.of(new RbacRole(other, "role_x", "X", "ADMIN", false)));
    when(rbac.isMatrixEditorRole(other)).thenReturn(true);
    present(user("ADMIN", "ACTIVE", true, false));
    roleHandler.change(new ChangeUserRoleCommand(target, target, other, null));
    present(user("ADMIN", "DEACTIVATED", true, false));
    when(rbac.lockRole(studentRole))
        .thenReturn(Optional.of(new RbacRole(studentRole, "STUDENT", "Student", "STUDENT", true)));
    roleHandler.change(new ChangeUserRoleCommand(target, target, studentRole, null));
    verify(accounts, never()).isLastActiveAdmin(any());
  }

  @Test
  void movingToAnAdminRoleThatCannotEditTheMatrixIsGuardedLikeLeavingTheAdminSet() {
    UUID empty = UUID.randomUUID();
    when(rbac.lockRole(empty))
        .thenReturn(Optional.of(new RbacRole(empty, "role_empty", "Empty", "ADMIN", false)));
    when(rbac.isMatrixEditorRole(empty)).thenReturn(false);
    present(user("ADMIN", "ACTIVE", true, false));

    when(accounts.isLastActiveAdmin(target)).thenReturn(true);
    assertThatThrownBy(
            () -> roleHandler.change(new ChangeUserRoleCommand(actor, target, empty, null)))
        .satisfies(e -> assertCode(e, "IDT-432"));

    when(accounts.isLastActiveAdmin(target)).thenReturn(false);
    assertThatThrownBy(
            () -> roleHandler.change(new ChangeUserRoleCommand(target, target, empty, null)))
        .satisfies(e -> assertCode(e, "IDT-V21"));
    roleHandler.change(new ChangeUserRoleCommand(actor, target, empty, null));
    verify(users).changeRole(target, empty);
  }

  // ---- #37 -----------------------------------------------------------------------------------

  private ChangeUserStatusCommand status(
      Object targetStatus, Object reason, Object notify, Object ack) {
    return new ChangeUserStatusCommand(actor, target, targetStatus, reason, notify, ack);
  }

  @Test
  void lockDeactivatesRevokesSessionsAuditsAndMailsTheReason() {
    present(user("STUDENT", "ACTIVE", true, false));

    statusHandler.change(status("DEACTIVATED", "  Spam  ", true, null));

    verify(users).deactivateByAdmin(target);
    verify(users).revokeAllSessions(target);
    ArgumentCaptor<AuditEntry> entry = ArgumentCaptor.forClass(AuditEntry.class);
    verify(audit).record(entry.capture());
    assertThat(entry.getValue().actionType()).isEqualTo("USER_LOCKED");
    assertThat(entry.getValue().afterJson()).contains("Spam");
    ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
    verify(email).send(eq("n@example.test"), anyString(), body.capture());
    assertThat(body.getValue()).contains("Spam");
  }

  @Test
  void quietLockAuditsButSendsNothing() {
    present(user("STUDENT", "ACTIVE", true, false));

    statusHandler.change(status("DEACTIVATED", null, false, null));
    statusHandler.change(status("DEACTIVATED", "   ", null, null));

    verify(email, never()).send(any(), any(), any());
    verify(audit, org.mockito.Mockito.times(2)).record(any());
  }

  @Test
  void unlockReactivatesWithoutMailAndIsRefusedForAnonymisedAccounts() {
    present(user("STUDENT", "DEACTIVATED", true, false));
    statusHandler.change(status("ACTIVE", "ignored", true, null));
    verify(users).reactivate(target);
    verify(email, never()).send(any(), any(), any());
    ArgumentCaptor<AuditEntry> entry = ArgumentCaptor.forClass(AuditEntry.class);
    verify(audit).record(entry.capture());
    assertThat(entry.getValue().actionType()).isEqualTo("USER_UNLOCKED");

    present(user("STUDENT", "DEACTIVATED", false, true));
    assertThatThrownBy(() -> statusHandler.change(status("ACTIVE", null, null, null)))
        .satisfies(e -> assertCode(e, "IDT-448"));
  }

  @Test
  void askingForTheStateTheAccountAlreadyHasChangesNothing() {
    present(user("STUDENT", "DEACTIVATED", true, false));
    statusHandler.change(status("DEACTIVATED", null, null, null));
    present(user("STUDENT", "ACTIVE", true, false));
    statusHandler.change(status("ACTIVE", null, null, null));

    verify(users, never()).deactivateByAdmin(any());
    verify(users, never()).reactivate(any());
    verify(audit, never()).record(any());
  }

  @Test
  void badStatusBodiesAreIdtV26AndUnknownUsersIdt450() {
    present(user("STUDENT", "ACTIVE", true, false));
    for (Object[] bad :
        new Object[][] {
          {"NOPE", null, null},
          {null, null, null},
          {"DEACTIVATED", null, true},
          {"DEACTIVATED", "x".repeat(501), false},
          {"DEACTIVATED", 7, false},
          {"DEACTIVATED", "ok", "yes"}
        }) {
      assertThatThrownBy(() -> statusHandler.change(status(bad[0], bad[1], bad[2], null)))
          .satisfies(e -> assertCode(e, "IDT-V26"));
    }
    when(users.find(target)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> statusHandler.change(status("ACTIVE", null, null, null)))
        .satisfies(e -> assertCode(e, "IDT-450"));
    verify(audit, never()).record(any());
  }

  @Test
  void lockingTheLastAdminIsIdt432AndLockingYourselfNeedsTheFlag() {
    present(user("ADMIN", "ACTIVE", true, false));
    when(accounts.isLastActiveAdmin(target)).thenReturn(true);
    assertThatThrownBy(() -> statusHandler.change(status("DEACTIVATED", null, false, true)))
        .satisfies(e -> assertCode(e, "IDT-432"));

    when(accounts.isLastActiveAdmin(target)).thenReturn(false);
    ChangeUserStatusCommand self =
        new ChangeUserStatusCommand(target, target, "DEACTIVATED", null, false, null);
    assertThatThrownBy(() -> statusHandler.change(self)).satisfies(e -> assertCode(e, "IDT-V21"));
    verify(users, never()).deactivateByAdmin(any());
    statusHandler.change(status("DEACTIVATED", null, false, null));
    verify(users).deactivateByAdmin(target);
  }

  // ---- #38 -----------------------------------------------------------------------------------

  @Test
  void resetStoresAHashedTemporaryPasswordAndMailsTheClearOne() {
    present(user("STUDENT", "ACTIVE", true, false));
    when(hasher.encode(anyString())).thenReturn("hash");

    resetHandler.reset(actor, target);

    ArgumentCaptor<String> clear = ArgumentCaptor.forClass(String.class);
    verify(hasher).encode(clear.capture());
    verify(users).setTemporaryPassword(target, "hash");
    verify(users).revokeAllSessions(target);
    ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
    verify(email).send(eq("n@example.test"), anyString(), body.capture());
    assertThat(body.getValue()).contains(clear.getValue());
    ArgumentCaptor<AuditEntry> entry = ArgumentCaptor.forClass(AuditEntry.class);
    verify(audit).record(entry.capture());
    assertThat(entry.getValue().afterJson()).doesNotContain(clear.getValue());
  }

  @Test
  void resetForAPasswordlessAccountOnlySendsGuidance() {
    present(user("STUDENT", "ACTIVE", false, false));

    resetHandler.reset(actor, target);

    verify(users, never()).setTemporaryPassword(any(), any());
    verify(users, never()).revokeAllSessions(any());
    verify(email).send(eq("n@example.test"), anyString(), anyString());
    verify(audit).record(any());
  }

  @Test
  void resetRefusesUnknownAndAnonymisedAccounts() {
    when(users.lock(target)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> resetHandler.reset(actor, target))
        .satisfies(e -> assertCode(e, "IDT-450"));
    present(user("STUDENT", "DEACTIVATED", true, true));
    assertThatThrownBy(() -> resetHandler.reset(actor, target))
        .satisfies(e -> assertCode(e, "IDT-448"));
    verify(email, never()).send(any(), any(), any());
  }

  // ---- #50 -----------------------------------------------------------------------------------

  private CreateUserAccountCommand create(String name, String address, String role) {
    return new CreateUserAccountCommand(actor, name, address, role);
  }

  @Test
  void createStoresAPendingAccountAndMailsTheTemporaryPassword() {
    UUID id = UUID.randomUUID();
    when(ids.newId()).thenReturn(id);
    when(rbac.lockRole(studentRole))
        .thenReturn(Optional.of(new RbacRole(studentRole, "STUDENT", "Student", "STUDENT", true)));
    when(hasher.encode(anyString())).thenReturn("hash");
    when(users.insertCreatedByAdmin(
            eq(id), eq("a@b.test"), eq("hash"), eq("New Name"), eq(studentRole)))
        .thenReturn(true);
    when(users.find(id))
        .thenReturn(Optional.of(user("STUDENT", "PENDING_VERIFICATION", true, false)));

    createHandler.create(create(" New Name ", "  A@B.test ", " " + studentRole + " "));

    ArgumentCaptor<String> clear = ArgumentCaptor.forClass(String.class);
    verify(hasher).encode(clear.capture());
    ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
    verify(email).send(eq("a@b.test"), anyString(), body.capture());
    assertThat(body.getValue()).contains(clear.getValue());
    ArgumentCaptor<AuditEntry> entry = ArgumentCaptor.forClass(AuditEntry.class);
    verify(audit).record(entry.capture());
    assertThat(entry.getValue().actionType()).isEqualTo("USER_CREATED");
    assertThat(entry.getValue().afterJson()).doesNotContain(clear.getValue());
  }

  @Test
  void createRefusesDuplicatesAdminRolesUnknownRolesAndBadInput() {
    when(rbac.lockRole(adminRole))
        .thenReturn(Optional.of(new RbacRole(adminRole, "ADMIN", "Admin", "ADMIN", true)));
    when(rbac.lockRole(studentRole))
        .thenReturn(Optional.of(new RbacRole(studentRole, "STUDENT", "Student", "STUDENT", true)));
    when(ids.newId()).thenReturn(UUID.randomUUID());
    when(hasher.encode(anyString())).thenReturn("hash");
    when(users.insertCreatedByAdmin(any(), any(), any(), any(), any())).thenReturn(false);

    assertThatThrownBy(
            () -> createHandler.create(create("Name", "a@b.test", studentRole.toString())))
        .satisfies(e -> assertCode(e, "IDT-440"));
    for (String role :
        new String[] {adminRole.toString(), UUID.randomUUID().toString(), "x", null}) {
      assertThatThrownBy(() -> createHandler.create(create("Name", "a@b.test", role)))
          .satisfies(e -> assertCode(e, "IDT-441"));
    }
    assertThatThrownBy(() -> createHandler.create(create("Name", "nope", studentRole.toString())))
        .satisfies(e -> assertCode(e, "IDT-400"));
    assertThatThrownBy(() -> createHandler.create(create("N", "a@b.test", studentRole.toString())))
        .satisfies(e -> assertCode(e, "IDT-V19"));
    verify(email, never()).send(any(), any(), any());
    verify(audit, never()).record(any());
  }

  // ---- temporary password --------------------------------------------------------------------

  @Test
  void aTemporaryPasswordAlwaysSatisfiesThePolicy() {
    for (int i = 0; i < 500; i++) {
      String password = generator.generate();
      assertThat(password)
          .hasSize(12)
          .matches("[A-Za-z0-9]+")
          .matches(".*[A-Za-z].*")
          .matches(".*[0-9].*");
    }
    assertThat(generator.generate()).isNotEqualTo(generator.generate());
  }
}
