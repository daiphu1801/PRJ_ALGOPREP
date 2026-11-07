package com.algoprep.identity.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.identity.domain.model.AuditEntry;
import com.algoprep.identity.domain.model.RbacRole;
import com.algoprep.identity.domain.ports.out.AuditLogPort;
import com.algoprep.identity.domain.ports.out.RbacRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

/** #34: built-in and in-use roles are refused; a free custom role goes with one audit row. */
class DeleteRoleCommandHandlerTest {
  private final RbacRepository rbac = mock(RbacRepository.class);
  private final AuditLogPort audit = mock(AuditLogPort.class);
  private final DeleteRoleCommandHandler handler = new DeleteRoleCommandHandler(rbac, audit);

  private final UUID actor = UUID.randomUUID();

  private RbacRole role(boolean system) {
    RbacRole role = new RbacRole(UUID.randomUUID(), "role_x", "Role X", "STUDENT", system);
    when(rbac.lockRole(role.id())).thenReturn(Optional.of(role));
    return role;
  }

  @Test
  void aFailingAuditWriteBecomes503IDT446SoTheTransactionRollsTheDeletionBack() {
    RbacRole role = role(false);
    when(rbac.countUsers(role.id())).thenReturn(0);
    when(rbac.deleteRole(role.id())).thenReturn(true);
    org.mockito.Mockito.doThrow(new IllegalStateException("audit down")).when(audit).record(any());

    assertThatThrownBy(() -> handler.delete(actor, role.id()))
        .isInstanceOf(ServiceUnavailableException.class)
        .satisfies(e -> assertCode(e, "IDT-446"))
        .hasCauseInstanceOf(IllegalStateException.class);
  }

  private static void assertCode(Throwable error, String code) {
    assertThat(error)
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo(code));
  }

  @Test
  void deletesAFreeCustomRoleAndAuditsItsFormerValues() {
    RbacRole role = role(false);
    when(rbac.countUsers(role.id())).thenReturn(0);
    when(rbac.deleteRole(role.id())).thenReturn(true);

    handler.delete(actor, role.id());

    InOrder order = inOrder(rbac, audit);
    order.verify(rbac).deleteRole(role.id());
    ArgumentCaptor<AuditEntry> entry = ArgumentCaptor.forClass(AuditEntry.class);
    order.verify(audit).record(entry.capture());
    assertThat(entry.getValue().actionType()).isEqualTo("ROLE_DELETED");
    assertThat(entry.getValue().actorUserId()).isEqualTo(actor);
    assertThat(entry.getValue().targetType()).isEqualTo("ROLE");
    assertThat(entry.getValue().targetId()).isEqualTo(role.id().toString());
    assertThat(entry.getValue().beforeJson()).contains("Role X", "STUDENT", "\"isSystem\":false");
    assertThat(entry.getValue().afterJson()).isNull();
  }

  @Test
  void aBuiltInRoleIs422Idt421EvenWithUsers() {
    RbacRole role = role(true);
    when(rbac.countUsers(role.id())).thenReturn(5);

    assertThatThrownBy(() -> handler.delete(actor, role.id()))
        .satisfies(e -> assertCode(e, "IDT-421"));
    verify(rbac, never()).deleteRole(any());
    verify(audit, never()).record(any());
  }

  @Test
  void aRoleWithUsersIs422Idt422() {
    RbacRole role = role(false);
    when(rbac.countUsers(role.id())).thenReturn(1);

    assertThatThrownBy(() -> handler.delete(actor, role.id()))
        .satisfies(e -> assertCode(e, "IDT-422"));
    verify(rbac, never()).deleteRole(any());
    verify(audit, never()).record(any());
  }

  @Test
  void aUserAssignedDuringTheDeleteIsAlsoIdt422AndWritesNoAudit() {
    RbacRole role = role(false);
    when(rbac.countUsers(role.id())).thenReturn(0);
    when(rbac.deleteRole(role.id())).thenReturn(false);

    assertThatThrownBy(() -> handler.delete(actor, role.id()))
        .satisfies(e -> assertCode(e, "IDT-422"));
    verify(audit, never()).record(any());
  }

  @Test
  void anUnknownRoleIs404Idt424() {
    UUID missing = UUID.randomUUID();
    when(rbac.lockRole(missing)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.delete(actor, missing))
        .satisfies(e -> assertCode(e, "IDT-424"));
    verify(audit, never()).record(any());
  }
}
