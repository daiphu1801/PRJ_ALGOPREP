package com.algoprep.identity.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.exception.UnauthenticatedException;
import com.algoprep.identity.application.dto.UpdateMyProfileCommand;
import com.algoprep.identity.domain.model.MyProfile;
import com.algoprep.identity.domain.model.ProfileFields;
import com.algoprep.identity.domain.ports.out.MyProfileRepository;
import jakarta.validation.Validation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** #10: sent fields are validated and merged over the stored ones; unsent fields are kept. */
class UpdateMyProfileCommandHandlerTest {
  private final MyProfileRepository profiles = mock(MyProfileRepository.class);
  private final UpdateMyProfileCommandHandler handler =
      new UpdateMyProfileCommandHandler(
          profiles,
          new RegistrationValidation(Validation.buildDefaultValidatorFactory().getValidator()));
  private final UUID userId = UUID.randomUUID();
  private final MyProfile stored =
      new MyProfile(
          userId,
          "Old Name",
          "a@example.test",
          "Old School",
          "Old Job",
          "Old Goal",
          "PYTHON",
          "0123",
          true,
          null,
          List.of(),
          Instant.parse("2026-10-31T00:00:00Z"),
          "Học viên",
          false);

  UpdateMyProfileCommandHandlerTest() {
    when(profiles.findById(userId)).thenReturn(Optional.of(stored));
  }

  private UpdateMyProfileCommand command(
      String name, String school, String current, String target, String phone) {
    return new UpdateMyProfileCommand(userId, name, school, current, target, phone);
  }

  private static void assertCode(Throwable error, String code) {
    assertThat(error)
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo(code));
  }

  @Test
  void sentFieldsAreTrimmedUnsentOnesKeepTheirValueAndEmptyClears() {
    handler.update(command("  New Name  ", "", null, "  New Goal ", null));

    verify(profiles)
        .updateFields(userId, new ProfileFields("New Name", null, "Old Job", "New Goal", "0123"));
  }

  @Test
  void aPhoneNumberKeepsDigitsAndTheAllowedSeparators() {
    handler.update(command(null, null, null, null, " +84 (90) 123-4567 "));

    verify(profiles)
        .updateFields(
            userId,
            new ProfileFields(
                "Old Name", "Old School", "Old Job", "Old Goal", "+84 (90) 123-4567"));
  }

  @Test
  void invalidValuesAre422WithTheDdCodeAndNothingIsWritten() {
    assertThatThrownBy(() -> handler.update(command("A", null, null, null, null)))
        .satisfies(e -> assertCode(e, "IDT-V19"));
    assertThatThrownBy(() -> handler.update(command("x".repeat(101), null, null, null, null)))
        .satisfies(e -> assertCode(e, "IDT-V20"));
    assertThatThrownBy(() -> handler.update(command(null, "x".repeat(151), null, null, null)))
        .satisfies(e -> assertCode(e, "IDT-V09"));
    assertThatThrownBy(() -> handler.update(command(null, null, "x".repeat(101), null, null)))
        .satisfies(e -> assertCode(e, "IDT-V07"));
    assertThatThrownBy(() -> handler.update(command(null, null, null, "x".repeat(101), null)))
        .satisfies(e -> assertCode(e, "IDT-V08"));
    assertThatThrownBy(() -> handler.update(command(null, null, null, null, "12ab")))
        .satisfies(e -> assertCode(e, "IDT-V13"));
    assertThatThrownBy(() -> handler.update(command(null, null, null, null, "1".repeat(21))))
        .satisfies(e -> assertCode(e, "IDT-V13"));

    verify(profiles, never()).updateFields(any(), any());
  }

  @Test
  void aMissingAccountIs401() {
    when(profiles.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.update(command("New Name", null, null, null, null)))
        .isInstanceOf(UnauthenticatedException.class);
  }
}
