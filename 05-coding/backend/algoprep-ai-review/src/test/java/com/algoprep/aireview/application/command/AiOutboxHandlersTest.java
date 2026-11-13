package com.algoprep.aireview.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.aireview.application.ports.out.AiDomainEventPort;
import com.algoprep.aireview.domain.model.AiOutboxEvent;
import com.algoprep.aireview.domain.ports.out.AiOutboxRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AiOutboxHandlersTest {
  private final AiOutboxRepository repository = mock(AiOutboxRepository.class);
  private final AiDomainEventPort publisher = mock(AiDomainEventPort.class);
  private final RelayAiOutboxCommandHandler relay =
      new RelayAiOutboxCommandHandler(repository, publisher, 100);
  private final CleanAiOutboxCommandHandler cleanup =
      new CleanAiOutboxCommandHandler(repository, 90, 5000);

  private static AiOutboxEvent event(String type, String payload, int attempts) {
    return new AiOutboxEvent(
        UUID.randomUUID(), type, payload, Instant.parse("2026-11-10T01:00:00Z"), attempts);
  }

  @Test
  void routingKeysFollowContextAggregateEvent() {
    assertThat(event("SolutionReviewCompleted", "{}", 0).routingKey())
        .isEqualTo("ai.solution-review.completed");
    assertThat(event("MockInterviewFinished", "{}", 0).routingKey())
        .isEqualTo("ai.interview-session.finished");
    assertThat(event("Other", "{}", 0).routingKey()).isNull();
  }

  @Test
  void envelopeCarriesIdTypeTimeVersionAndTheUntouchedPayload() {
    var e = event("SolutionReviewCompleted", "{\"readabilityScore\":4}", 0);
    assertThat(AiEventEnvelope.of(e))
        .isEqualTo(
            "{\"eventId\":\""
                + e.id()
                + "\",\"eventType\":\"SolutionReviewCompleted\","
                + "\"occurredAt\":\"2026-11-10T01:00:00Z\",\"schemaVersion\":1,"
                + "\"payload\":{\"readabilityScore\":4}}");
  }

  @Test
  void aConfirmedRowIsMarkedPublishedAndAnUnconfirmedOneIsCountedOnly() {
    var ok = event("SolutionReviewCompleted", "{}", 0);
    var lost = event("SolutionReviewCompleted", "{}", 4);
    when(repository.lockPending(100)).thenReturn(List.of(ok, lost));
    when(publisher.publish(any(), any())).thenReturn(true, false);
    assertThat(relay.relay()).isEqualTo(2);
    verify(repository).attempted(ok.id(), true);
    verify(repository).attempted(lost.id(), false);
  }

  @Test
  void aThrowingPublisherKeepsTheRowPendingAndDoesNotEscape() {
    var e = event("SolutionReviewCompleted", "{}", 0);
    when(repository.lockPending(100)).thenReturn(List.of(e));
    when(publisher.publish(any(), any())).thenThrow(new IllegalStateException("broker down"));
    assertThat(relay.relay()).isEqualTo(1);
    verify(repository).attempted(e.id(), false);
  }

  @Test
  void anUnknownTypeOrBrokenPayloadIsNeverPublished() {
    var unknown = event("Other", "{}", 0);
    var broken = event("SolutionReviewCompleted", "not json", 0);
    when(repository.lockPending(100)).thenReturn(List.of(unknown, broken));
    assertThat(relay.relay()).isEqualTo(2);
    verify(publisher, never()).publish(any(), any());
    verify(repository).attempted(unknown.id(), false);
    verify(repository).attempted(broken.id(), false);
  }

  @Test
  void theEnvelopeHandedToThePublisherHasTheSameEventIdOnEveryRetry() {
    var e = event("SolutionReviewCompleted", "{}", 1);
    when(repository.lockPending(100)).thenReturn(List.of(e));
    relay.relay();
    relay.relay();
    var captor = ArgumentCaptor.forClass(String.class);
    verify(publisher, times(2)).publish(any(), captor.capture());
    assertThat(captor.getAllValues().get(0)).isEqualTo(captor.getAllValues().get(1));
  }

  @Test
  void cleanupLoopsUntilAShortChunkAndSumsTheDeletes() {
    when(repository.tryCleanupLock()).thenReturn(true);
    when(repository.deletePublishedOlderThanDays(90, 5000)).thenReturn(5000, 5000, 7);
    assertThat(cleanup.clean()).isEqualTo(10007);
  }

  @Test
  void cleanupDoesNothingWhenAnotherInstanceHoldsTheLock() {
    when(repository.tryCleanupLock()).thenReturn(false);
    assertThat(cleanup.clean()).isZero();
    verify(repository, never()).deletePublishedOlderThanDays(anyInt(), anyInt());
  }
}
