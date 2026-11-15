package com.algoprep.problembank.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.problembank.application.ports.out.ProblemDomainEventPort;
import com.algoprep.problembank.domain.model.OutboxEvent;
import com.algoprep.problembank.domain.ports.out.ProblemOutboxRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProblemOutboxCommandHandlersTest {
  private final ProblemOutboxRepository repository = mock(ProblemOutboxRepository.class);
  private final ProblemDomainEventPort publisher = mock(ProblemDomainEventPort.class);
  private final RelayProblemOutboxCommandHandler relay =
      new RelayProblemOutboxCommandHandler(repository, publisher, 100);

  private static OutboxEvent row(String key, String envelope) {
    return new OutboxEvent(UUID.randomUUID(), "ProblemPublished", key, envelope, 0);
  }

  @Test
  void confirmedRowIsMarkedAndUnconfirmedRowIsKept() {
    var ok = row("problem.problem.published", "{\"a\":1}");
    var nack = row("problem.problem.unpublished", "{}");
    when(repository.lockPending(100)).thenReturn(List.of(ok, nack));
    when(publisher.publish(ok)).thenReturn(true);
    when(publisher.publish(nack)).thenReturn(false);
    assertThat(relay.relay()).isEqualTo(2);
    verify(repository).attempted(ok.id(), true);
    verify(repository).attempted(nack.id(), false);
  }

  @Test
  void transportFailureCountsAsAttemptWithoutPublishing() {
    var event = row("problem.problem.published", "{}");
    when(repository.lockPending(100)).thenReturn(List.of(event));
    doThrow(new IllegalStateException("broker")).when(publisher).publish(any());
    relay.relay();
    verify(repository).attempted(event.id(), false);
  }

  @Test
  void blankRoutingKeyOrBrokenJsonNeverReachesTheBroker() {
    var blank = row(" ", "{}");
    var missing = row(null, "{}");
    var broken = row("problem.problem.published", "{not json");
    when(repository.lockPending(100)).thenReturn(List.of(blank, missing, broken));
    relay.relay();
    verify(publisher, never()).publish(any());
    verify(repository).attempted(blank.id(), false);
    verify(repository).attempted(missing.id(), false);
    verify(repository).attempted(broken.id(), false);
  }

  @Test
  void cleanupStopsAfterAShortChunk() {
    when(repository.deletePublishedOlderThanDays(90, 1000)).thenReturn(1000, 1000, 7);
    var cleanup = new CleanProblemOutboxCommandHandler(repository, 90);
    assertThat(cleanup.clean()).isEqualTo(2007);
  }

  @Test
  void cleanupSwallowsDatabaseFailureAndKeepsWhatWasDeleted() {
    when(repository.deletePublishedOlderThanDays(90, 1000))
        .thenReturn(1000)
        .thenThrow(new IllegalStateException("db"));
    var cleanup = new CleanProblemOutboxCommandHandler(repository, 90);
    assertThat(cleanup.clean()).isEqualTo(1000);
  }
}
