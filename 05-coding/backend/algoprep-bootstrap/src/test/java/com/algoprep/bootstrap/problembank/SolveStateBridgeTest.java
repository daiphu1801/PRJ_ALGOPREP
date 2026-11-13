package com.algoprep.bootstrap.problembank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.algoprep.identity.application.ports.in.ListAllMySolveStatesUseCase;
import com.algoprep.identity.domain.model.SolveState;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SolveStateBridgeTest {
  private final ListAllMySolveStatesUseCase identity = mock(ListAllMySolveStatesUseCase.class);
  private final UUID user = UUID.randomUUID();

  @Test
  void identityStatesSplitIntoSolvedAndAttempted() {
    var solved = UUID.randomUUID();
    var tried = UUID.randomUUID();
    when(identity.listAll(user))
        .thenReturn(Map.of(solved, SolveState.SOLVED, tried, SolveState.IN_PROGRESS));

    var states = new SolveStateBridge(identity).mine(user).orElseThrow();

    assertThat(states.solved()).containsExactly(solved);
    assertThat(states.attempted()).containsExactly(tried);
  }

  @Test
  void aFailingIdentityReadIsAnAbsentAnswerNotAnError() {
    when(identity.listAll(user)).thenThrow(new IllegalStateException("down"));

    assertThat(new SolveStateBridge(identity).mine(user)).isEmpty();
  }

  @Test
  void aSlowIdentityReadIsGivenUpOn() {
    when(identity.listAll(user))
        .thenAnswer(
            call -> {
              Thread.sleep(2000);
              return Map.of();
            });

    assertThat(new SolveStateBridge(identity, Duration.ofMillis(50)).mine(user)).isEmpty();
  }
}
