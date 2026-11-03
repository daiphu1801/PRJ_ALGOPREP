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
import com.algoprep.identity.application.dto.UpdateMySettingsCommand;
import com.algoprep.identity.domain.model.MySettings;
import com.algoprep.identity.domain.ports.out.MySettingsRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** #15: sent values are validated and merged over the stored ones; unsent values are kept. */
class UpdateMySettingsCommandHandlerTest {
  private final MySettingsRepository settings = mock(MySettingsRepository.class);
  private final UpdateMySettingsCommandHandler handler =
      new UpdateMySettingsCommandHandler(settings);
  private final UUID userId = UUID.randomUUID();
  private final MySettings stored = new MySettings(true, false, "MIDDLE", 12, true, "PYTHON");

  UpdateMySettingsCommandHandlerTest() {
    when(settings.findForUpdate(userId)).thenReturn(Optional.of(stored));
  }

  private UpdateMySettingsCommand cmd(Object... values) {
    // streak, weekly, level, turns, hint, language
    return new UpdateMySettingsCommand(
        userId,
        (Boolean) values[0],
        (Boolean) values[1],
        (String) values[2],
        (Integer) values[3],
        (Boolean) values[4],
        (String) values[5]);
  }

  private static void assertCode(Throwable error, String code) {
    assertThat(error)
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo(code));
  }

  @Test
  void onlyTheSentValuesChangeAndTheStoredResultIsReturned() {
    MySettings result = handler.update(cmd(false, null, "SENIOR", 16, null, "JAVA"));

    MySettings expected = new MySettings(false, false, "SENIOR", 16, true, "JAVA");
    assertThat(result).isEqualTo(expected);
    verify(settings).save(userId, expected);
  }

  @Test
  void anEmptyUpdateKeepsEverything() {
    assertThat(handler.update(cmd(null, null, null, null, null, null))).isEqualTo(stored);
  }

  @Test
  void aValueOutsideTheAllowedSetIs422AndNothingIsWritten() {
    assertThatThrownBy(() -> handler.update(cmd(null, null, "GOD", null, null, null)))
        .satisfies(e -> assertCode(e, "IDT-V18"));
    assertThatThrownBy(() -> handler.update(cmd(null, null, null, 10, null, null)))
        .satisfies(e -> assertCode(e, "IDT-V18"));
    assertThatThrownBy(() -> handler.update(cmd(null, null, null, null, null, "RUST")))
        .satisfies(e -> assertCode(e, "IDT-413"));

    verify(settings, never()).save(any(), any());
  }

  @Test
  void aMissingAccountIs401() {
    when(settings.findForUpdate(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.update(cmd(true, null, null, null, null, null)))
        .isInstanceOf(UnauthenticatedException.class);
  }
}
