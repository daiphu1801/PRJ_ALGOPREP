package com.algoprep.identity.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.identity.application.ports.out.PasswordHasherPort;
import com.algoprep.identity.domain.ports.out.AdminBootstrapRepository;
import jakarta.validation.Validation;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BootstrapFirstAdminCommandHandlerTest {
  private static final String PASSWORD = "Sup3rSecretPass";

  private final AdminBootstrapRepository admins = mock(AdminBootstrapRepository.class);
  private final PasswordHasherPort hasher = mock(PasswordHasherPort.class);
  private final IdGeneratorPort ids = mock(IdGeneratorPort.class);
  private final BootstrapFirstAdminCommandHandler handler =
      new BootstrapFirstAdminCommandHandler(
          admins,
          hasher,
          ids,
          new RegistrationValidation(Validation.buildDefaultValidatorFactory().getValidator()));

  @Test
  void existingAdminMeansNothingIsReadHashedOrCreated() {
    when(admins.adminExists()).thenReturn(true);
    handler.bootstrap(null, null);
    verify(admins, never()).createAdmin(any(), anyString(), anyString(), anyString());
    verifyNoInteractions(hasher, ids);
  }

  @Test
  void missingVariableStopsStartupNamingTheVariable() {
    assertThatThrownBy(() -> handler.bootstrap("", PASSWORD))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("ALGOPREP_BOOTSTRAP_ADMIN_EMAIL");
    assertThatThrownBy(() -> handler.bootstrap("a@b.vn", " "))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("ALGOPREP_BOOTSTRAP_ADMIN_PASSWORD");
  }

  @Test
  void invalidPasswordNamesTheVariableAndNeverEchoesTheValue() {
    String weak = "short1";
    assertThatThrownBy(() -> handler.bootstrap("admin@algoprep.local", weak))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("ALGOPREP_BOOTSTRAP_ADMIN_PASSWORD")
        .satisfies(e -> assertThat(e.getMessage()).doesNotContain(weak));
    verify(admins, never()).createAdmin(any(), anyString(), anyString(), anyString());
  }

  @Test
  void invalidEmailStopsStartup() {
    assertThatThrownBy(() -> handler.bootstrap("not-an-email", PASSWORD))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("ALGOPREP_BOOTSTRAP_ADMIN_EMAIL");
  }

  @Test
  void createsAdminWithNormalizedEmailAndHashedPassword() {
    UUID id = UUID.randomUUID();
    when(ids.newId()).thenReturn(id);
    when(hasher.encode(PASSWORD)).thenReturn("hash");
    when(admins.createAdmin(id, "admin@algoprep.local", "hash", "Administrator")).thenReturn(true);
    handler.bootstrap("  Admin@AlgoPrep.local ", PASSWORD);
    verify(admins).createAdmin(id, "admin@algoprep.local", "hash", "Administrator");
  }

  @Test
  void emailTakenByNonAdminStopsStartup() {
    when(admins.adminExists()).thenReturn(false, false);
    when(admins.createAdmin(any(), anyString(), anyString(), anyString())).thenReturn(false);
    assertThatThrownBy(() -> handler.bootstrap("student@algoprep.local", PASSWORD))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("non-ADMIN");
  }

  @Test
  void losingTheRaceToAnotherInstanceIsSuccess() {
    when(admins.adminExists()).thenReturn(false, true);
    when(admins.createAdmin(any(), anyString(), anyString(), anyString())).thenReturn(false);
    assertThatCode(() -> handler.bootstrap("admin@algoprep.local", PASSWORD))
        .doesNotThrowAnyException();
  }
}
