package com.algoprep.identity.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.identity.application.dto.UpdateRolePermissionCommand;
import com.algoprep.identity.domain.model.AuditEntry;
import com.algoprep.identity.domain.model.PermissionChange;
import com.algoprep.identity.domain.model.RbacRole;
import com.algoprep.identity.domain.ports.out.AuditLogPort;
import com.algoprep.identity.domain.ports.out.RbacRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** #31 and BR-13: one cell, version bump and audit together; protected cells are refused. */
class UpdateRolePermissionCommandHandlerTest {
  private final RbacRepository rbac = mock(RbacRepository.class);
  private final AuditLogPort audit = mock(AuditLogPort.class);
  private final UpdateRolePermissionCommandHandler handler =
      new UpdateRolePermissionCommandHandler(rbac, audit);

  private final UUID actor = UUID.randomUUID();
  private final UUID function = UUID.randomUUID();
  private final UUID action = UUID.randomUUID();

  private RbacRole role(String code, String category, boolean system) {
    RbacRole role = new RbacRole(UUID.randomUUID(), code, code, category, system);
    when(rbac.lockForPermissionChange(role.id())).thenReturn(Optional.of(role));
    when(rbac.functionCode(function)).thenReturn(Optional.of("CLASS_MANAGEMENT"));
    when(rbac.actionCode(action)).thenReturn(Optional.of("READ"));
    return role;
  }

  private UpdateRolePermissionCommand cmd(RbacRole role, Boolean granted) {
    return new UpdateRolePermissionCommand(actor, role.id(), function, action, granted);
  }

  private static void assertCode(Throwable error, String code) {
    assertThat(error)
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo(code));
  }

  private void noWrites() {
    verify(rbac, never()).setGranted(any(), any(), any(), anyBoolean(), any());
    verify(rbac, never()).bumpPermissionVersion(any());
    verify(audit, never()).record(any());
  }

  @Test
  void switchingACellOffWritesBumpsTheVersionAndAuditsBeforeAndAfter() {
    RbacRole instructor = role("INSTRUCTOR", "INSTRUCTOR", true);
    when(rbac.isGranted(instructor.id(), function, action)).thenReturn(true);

    PermissionChange change = handler.update(cmd(instructor, false));

    assertThat(change.changed()).isTrue();
    assertThat(change.granted()).isFalse();
    verify(rbac).setGranted(instructor.id(), function, action, false, actor);
    verify(rbac).bumpPermissionVersion(instructor.id());
    ArgumentCaptor<AuditEntry> entry = ArgumentCaptor.forClass(AuditEntry.class);
    verify(audit).record(entry.capture());
    assertThat(entry.getValue().actionType()).isEqualTo("ROLE_PERMISSION_CHANGED");
    assertThat(entry.getValue().actorUserId()).isEqualTo(actor);
    assertThat(entry.getValue().targetId()).isEqualTo(instructor.id().toString());
    assertThat(entry.getValue().beforeJson()).contains("\"granted\":true");
    assertThat(entry.getValue().afterJson()).contains("\"granted\":false", "CLASS_MANAGEMENT");
  }

  @Test
  void aMissingRowCountsAsOffSoSwitchingOnCreatesIt() {
    RbacRole custom = role("role_x", "STUDENT", false);
    when(rbac.isGranted(custom.id(), function, action)).thenReturn(false);

    assertThat(handler.update(cmd(custom, true)).changed()).isTrue();

    verify(rbac).setGranted(custom.id(), function, action, true, actor);
  }

  @Test
  void repeatingTheCurrentValueChangesNothing() {
    RbacRole instructor = role("INSTRUCTOR", "INSTRUCTOR", true);
    when(rbac.isGranted(instructor.id(), function, action)).thenReturn(true);

    PermissionChange change = handler.update(cmd(instructor, true));

    assertThat(change.changed()).isFalse();
    assertThat(change.granted()).isTrue();
    noWrites();
  }

  @Test
  void aMissingGrantedFlagIsIdtV25() {
    RbacRole instructor = role("INSTRUCTOR", "INSTRUCTOR", true);

    assertThatThrownBy(() -> handler.update(cmd(instructor, null)))
        .satisfies(e -> assertCode(e, "IDT-V25"));
    noWrites();
  }

  @Test
  void unknownRoleFunctionOrActionIs404Idt424() {
    RbacRole instructor = role("INSTRUCTOR", "INSTRUCTOR", true);
    UUID missing = UUID.randomUUID();
    when(rbac.lockForPermissionChange(missing)).thenReturn(Optional.empty());
    when(rbac.functionCode(missing)).thenReturn(Optional.empty());
    when(rbac.actionCode(missing)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                handler.update(
                    new UpdateRolePermissionCommand(actor, missing, function, action, true)))
        .satisfies(e -> assertCode(e, "IDT-424"));
    assertThatThrownBy(
            () ->
                handler.update(
                    new UpdateRolePermissionCommand(actor, instructor.id(), missing, action, true)))
        .satisfies(e -> assertCode(e, "IDT-424"));
    assertThatThrownBy(
            () ->
                handler.update(
                    new UpdateRolePermissionCommand(
                        actor, instructor.id(), function, missing, true)))
        .satisfies(e -> assertCode(e, "IDT-424"));
    noWrites();
  }

  @Test
  void theBuiltInStudentRoleIsReadOnlyIdt423() {
    RbacRole student = role("STUDENT", "STUDENT", true);

    assertThatThrownBy(() -> handler.update(cmd(student, true)))
        .satisfies(e -> assertCode(e, "IDT-423"));
    noWrites();
  }

  @Test
  void aCustomStudentBasedRoleIsEditable() {
    RbacRole custom = role("STUDENT", "STUDENT", false);

    assertThat(handler.update(cmd(custom, true)).changed()).isTrue();
  }

  @Test
  void removingTheLastAdminMatrixUpdateCellIsIdt420() {
    RbacRole admin = role("ADMIN", "ADMIN", true);
    when(rbac.functionCode(function)).thenReturn(Optional.of("PERMISSION_MATRIX"));
    when(rbac.actionCode(action)).thenReturn(Optional.of("UPDATE"));
    when(rbac.isGranted(admin.id(), function, action)).thenReturn(true);
    when(rbac.activeMatrixEditorsOutsideRole(admin.id())).thenReturn(0);

    assertThatThrownBy(() -> handler.update(cmd(admin, false)))
        .satisfies(e -> assertCode(e, "IDT-420"));
    noWrites();
  }

  @Test
  void removingTheMatrixUpdateCellIsFineWhenAnotherAdminRoleKeepsIt() {
    RbacRole admin = role("ADMIN", "ADMIN", true);
    when(rbac.functionCode(function)).thenReturn(Optional.of("PERMISSION_MATRIX"));
    when(rbac.actionCode(action)).thenReturn(Optional.of("UPDATE"));
    when(rbac.isGranted(admin.id(), function, action)).thenReturn(true);
    when(rbac.activeMatrixEditorsOutsideRole(admin.id())).thenReturn(1);

    assertThat(handler.update(cmd(admin, false)).changed()).isTrue();
  }

  @Test
  void otherMatrixCellsOfAnAdminRoleAreNotProtected() {
    RbacRole admin = role("ADMIN", "ADMIN", true);
    when(rbac.functionCode(function)).thenReturn(Optional.of("PERMISSION_MATRIX"));
    when(rbac.actionCode(action)).thenReturn(Optional.of("READ"));
    when(rbac.isGranted(admin.id(), function, action)).thenReturn(true);

    assertThat(handler.update(cmd(admin, false)).changed()).isTrue();
    verify(rbac, never()).activeMatrixEditorsOutsideRole(any());
  }
}
