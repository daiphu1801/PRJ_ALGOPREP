package com.algoprep.identity.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.identity.application.dto.CreateRoleCommand;
import com.algoprep.identity.domain.model.AuditEntry;
import com.algoprep.identity.domain.model.RbacRole;
import com.algoprep.identity.domain.model.RoleListItem;
import com.algoprep.identity.domain.ports.out.AuditLogPort;
import com.algoprep.identity.domain.ports.out.RbacRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** #33: name rules, generated code, duplicate refusal, base category, one audit row. */
class CreateRoleCommandHandlerTest {
  private final RbacRepository rbac = mock(RbacRepository.class);
  private final AuditLogPort audit = mock(AuditLogPort.class);
  private final IdGeneratorPort ids = mock(IdGeneratorPort.class);
  private final CreateRoleCommandHandler handler = new CreateRoleCommandHandler(rbac, audit, ids);

  private final UUID actor = UUID.randomUUID();
  private final UUID newId = UUID.randomUUID();

  private CreateRoleCommand cmd(String name, String category) {
    return new CreateRoleCommand(actor, name, category);
  }

  private static void assertCode(Throwable error, String code) {
    assertThat(error)
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo(code));
  }

  private void nothingWritten() {
    verify(rbac, never()).insertRole(any());
    verify(audit, never()).record(any());
  }

  @Test
  void createsACustomRoleWithAGeneratedCodeAndOneAuditRow() {
    when(ids.newId()).thenReturn(newId);
    when(rbac.insertRole(any())).thenReturn(true);

    RoleListItem created = handler.create(cmd("  Teaching  Assistant ", "INSTRUCTOR"));

    assertThat(created.code()).isEqualTo("role_teaching_assistant");
    assertThat(created.name()).isEqualTo("Teaching  Assistant");
    assertThat(created.isSystem()).isFalse();
    assertThat(created.userCount()).isZero();
    ArgumentCaptor<RbacRole> stored = ArgumentCaptor.forClass(RbacRole.class);
    verify(rbac).insertRole(stored.capture());
    assertThat(stored.getValue().id()).isEqualTo(newId);
    assertThat(stored.getValue().isSystem()).isFalse();
    ArgumentCaptor<AuditEntry> entry = ArgumentCaptor.forClass(AuditEntry.class);
    verify(audit).record(entry.capture());
    assertThat(entry.getValue().actionType()).isEqualTo("ROLE_CREATED");
    assertThat(entry.getValue().targetId()).isEqualTo(newId.toString());
    assertThat(entry.getValue().afterJson()).contains("INSTRUCTOR");
  }

  @Test
  void aBlankNameIsIdtV22() {
    assertThatThrownBy(() -> handler.create(cmd("   ", "STUDENT")))
        .satisfies(e -> assertCode(e, "IDT-V22"));
    assertThatThrownBy(() -> handler.create(cmd(null, "STUDENT")))
        .satisfies(e -> assertCode(e, "IDT-V22"));
    nothingWritten();
  }

  @Test
  void fiftyCharactersPassAndFiftyOneAreIdtV23() {
    when(ids.newId()).thenReturn(newId);
    when(rbac.insertRole(any())).thenReturn(true);

    assertThat(handler.create(cmd("a".repeat(50), "STUDENT")).name()).hasSize(50);
    assertThatThrownBy(() -> handler.create(cmd(" " + "a".repeat(51) + " ", "STUDENT")))
        .satisfies(e -> assertCode(e, "IDT-V23"));
  }

  @Test
  void aBaseCategoryOutsideTheThreeIsIdtV24() {
    assertThatThrownBy(() -> handler.create(cmd("Mentor", "SUPER")))
        .satisfies(e -> assertCode(e, "IDT-V24"));
    assertThatThrownBy(() -> handler.create(cmd("Mentor", null)))
        .satisfies(e -> assertCode(e, "IDT-V24"));
    nothingWritten();
  }

  @Test
  void aNameThatMatchesAnExistingCodeOrLabelIs409Idt425() {
    when(rbac.roleNameTaken("role_admin", "admin")).thenReturn(true);

    assertThatThrownBy(() -> handler.create(cmd("admin", "STUDENT")))
        .satisfies(e -> assertCode(e, "IDT-425"));
    nothingWritten();
  }

  @Test
  void aRaceOnTheUniqueCodeIsAlso409AndWritesNoAudit() {
    when(ids.newId()).thenReturn(newId);
    when(rbac.insertRole(any())).thenReturn(false);

    assertThatThrownBy(() -> handler.create(cmd("Mentor", "STUDENT")))
        .satisfies(e -> assertCode(e, "IDT-425"));
    verify(audit, never()).record(any());
  }
}
