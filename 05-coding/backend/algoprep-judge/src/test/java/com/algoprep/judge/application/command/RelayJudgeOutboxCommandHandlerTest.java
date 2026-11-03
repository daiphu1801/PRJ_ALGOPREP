package com.algoprep.judge.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.judge.application.ports.out.JudgeDomainEventPort;
import com.algoprep.judge.domain.model.OutboxEvent;
import com.algoprep.judge.domain.ports.out.JudgeOutboxRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RelayJudgeOutboxCommandHandlerTest {
  private final JudgeOutboxRepository repository = mock(JudgeOutboxRepository.class);
  private final JudgeDomainEventPort publisher = mock(JudgeDomainEventPort.class);
  private final RelayJudgeOutboxCommandHandler handler =
      new RelayJudgeOutboxCommandHandler(repository, publisher, 3, 5);

  @Test
  void confirmsAndFailuresAreCountedAndNeverAbandoned() {
    var confirmed = event(0);
    var warn = event(2);
    var error = event(8);
    var unavailable = event(0);
    when(repository.lockPending()).thenReturn(List.of(confirmed, warn, error, unavailable));
    when(publisher.publish(confirmed)).thenReturn(true);
    when(publisher.publish(unavailable)).thenThrow(new IllegalStateException("private payload"));
    assertThat(handler.relay()).isEqualTo(4);
    verify(repository).attempted(confirmed.id(), true);
    verify(repository).attempted(warn.id(), false);
    verify(repository).attempted(error.id(), false);
    verify(repository).attempted(unavailable.id(), false);
  }

  @Test
  void anEmptyBatchDoesNothing() {
    when(repository.lockPending()).thenReturn(List.of());
    assertThat(handler.relay()).isZero();
  }

  @Test
  void databaseFailuresEscapeToRollbackInsteadOfClaimingDelivery() {
    when(repository.lockPending()).thenThrow(new IllegalStateException("db"));
    assertThatThrownBy(handler::relay).isInstanceOf(IllegalStateException.class);
  }

  private OutboxEvent event(int attempts) {
    return new OutboxEvent(UUID.randomUUID(), "SubmissionGraded", "{}", attempts);
  }
}
