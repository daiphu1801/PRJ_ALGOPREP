package com.algoprep.identity.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.identity.application.dto.JoinClassByCodeCommand;
import com.algoprep.identity.application.ports.out.JoinAttemptLimitPort;
import com.algoprep.identity.domain.model.ClassRef;
import com.algoprep.identity.domain.ports.out.StudentClassRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** #25: normalise, block check first, only unusable codes count, success clears the window. */
class JoinClassByCodeCommandHandlerTest {
  private final StudentClassRepository classes = mock(StudentClassRepository.class);
  private final JoinAttemptLimitPort attempts = mock(JoinAttemptLimitPort.class);
  private final JoinClassByCodeCommandHandler handler =
      new JoinClassByCodeCommandHandler(classes, attempts);
  private final UUID userId = UUID.randomUUID();
  private final ClassRef ref = new ClassRef(UUID.randomUUID(), "Algo 101");

  private static void assertCode(Throwable error, String code) {
    assertThat(error)
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo(code));
  }

  @Test
  void trimsUppercasesJoinsAndClearsTheWindow() {
    when(classes.joinByCode(userId, "AB12CD34")).thenReturn(Optional.of(ref));

    ClassRef result = handler.join(new JoinClassByCodeCommand(userId, "  ab12cd34 "));

    assertThat(result).isEqualTo(ref);
    verify(attempts).clear(userId);
    verify(attempts, never()).recordFailure(userId);
  }

  @Test
  void unknownOrExpiredCodeIs422Idt410AndCountsAsFailure() {
    when(classes.joinByCode(userId, "AB12CD34")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.join(new JoinClassByCodeCommand(userId, "AB12CD34")))
        .satisfies(e -> assertCode(e, "IDT-410"));

    verify(attempts).recordFailure(userId);
    verify(attempts, never()).clear(userId);
  }

  @Test
  void malformedAndMissingCodesNeverReachTheRepositoryButStillCount() {
    for (String bad : new String[] {"SHORT", "AB12CD3!", "AB12 CD34", "AB12CD345", "", null}) {
      assertThatThrownBy(() -> handler.join(new JoinClassByCodeCommand(userId, bad)))
          .satisfies(e -> assertCode(e, "IDT-410"));
    }

    verifyNoInteractions(classes);
    verify(attempts, org.mockito.Mockito.times(6)).recordFailure(userId);
  }

  @Test
  void blockedUserGets429Idt412WithoutLookingAtTheCodeOrCounting() {
    when(attempts.isBlocked(userId)).thenReturn(true);

    assertThatThrownBy(() -> handler.join(new JoinClassByCodeCommand(userId, "AB12CD34")))
        .satisfies(e -> assertCode(e, "IDT-412"));

    verifyNoInteractions(classes);
    verify(attempts, never()).recordFailure(userId);
    verify(attempts, never()).clear(userId);
  }

  @Test
  void blockedUserWithAValidCodeIsStillRefused() {
    when(attempts.isBlocked(userId)).thenReturn(true);
    when(classes.joinByCode(userId, "AB12CD34")).thenReturn(Optional.of(ref));

    assertThatThrownBy(() -> handler.join(new JoinClassByCodeCommand(userId, "AB12CD34")))
        .satisfies(e -> assertCode(e, "IDT-412"));

    verify(classes, never()).joinByCode(userId, "AB12CD34");
  }
}
