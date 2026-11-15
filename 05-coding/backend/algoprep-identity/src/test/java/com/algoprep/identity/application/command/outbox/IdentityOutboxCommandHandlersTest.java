package com.algoprep.identity.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.identity.application.ports.out.IdentityDomainEventPort;
import com.algoprep.identity.domain.model.OutboxEvent;
import com.algoprep.identity.domain.ports.out.IdentityOutboxRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdentityOutboxCommandHandlersTest {
  @Test
  void unconfirmedOrFailedPublishRemainsPendingAndConfirmedIsMarked() {
    var repository = mock(IdentityOutboxRepository.class);
    var publisher = mock(IdentityDomainEventPort.class);
    var a = new OutboxEvent(UUID.randomUUID(), "ClassDeleted", "class.deleted", "{}", 0);
    var b = new OutboxEvent(UUID.randomUUID(), "ClassDeleted", "class.deleted", "{}", 2);
    var c = new OutboxEvent(UUID.randomUUID(), "ClassDeleted", "class.deleted", "{}", 0);
    when(repository.lockPending(100)).thenReturn(List.of(a, b, c));
    when(publisher.publish(a)).thenReturn(true);
    when(publisher.publish(c)).thenThrow(new IllegalStateException("broker down"));
    assertThat(new RelayIdentityOutboxCommandHandler(repository, publisher, 100).relay())
        .isEqualTo(3);
    verify(repository).attempted(a.id(), true);
    verify(repository).attempted(b.id(), false);
    verify(repository).attempted(c.id(), false);
  }

  @Test
  void malformedStoredEnvelopeIsNotPublished() {
    var repository = mock(IdentityOutboxRepository.class);
    var publisher = mock(IdentityDomainEventPort.class);
    var event = new OutboxEvent(UUID.randomUUID(), "ClassDeleted", "class.deleted", "not-json", 0);
    when(repository.lockPending(100)).thenReturn(List.of(event));
    new RelayIdentityOutboxCommandHandler(repository, publisher, 100).relay();
    verifyNoInteractions(publisher);
    verify(repository).attempted(event.id(), false);
  }
}
