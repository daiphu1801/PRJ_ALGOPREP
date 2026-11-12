package com.algoprep.identity.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Class events must be persisted; a write failure must roll back the business operation. */
class AmqpClassEventAdapterTest {
  private final JdbcClient jdbc = mock(JdbcClient.class);

  @SuppressWarnings("unchecked")
  private final ObjectProvider<JdbcClient> provider = mock(ObjectProvider.class);

  private final JdbcClient.StatementSpec statement =
      mock(JdbcClient.StatementSpec.class, RETURNS_SELF);

  @Test
  void storesClassDeletionEnvelope() {
    when(provider.getObject()).thenReturn(jdbc);
    when(jdbc.sql(anyString())).thenReturn(statement);
    UUID id = UUID.randomUUID();
    new AmqpClassEventAdapter(provider, mock(ObjectProvider.class)).classDeleted(id, Instant.EPOCH);
    verify(statement).param("classId", id);
    var envelope = ArgumentCaptor.forClass(Object.class);
    verify(statement).param(org.mockito.ArgumentMatchers.eq("body"), envelope.capture());
    assertThat(envelope.getValue().toString())
        .contains("\"eventType\":\"ClassDeleted\"", "\"classId\":\"" + id + "\"");
    verify(statement).param("at", java.sql.Timestamp.from(Instant.EPOCH));
    verify(statement).update();
  }

  @Test
  void writeFailureIsNotSwallowed() {
    when(provider.getObject()).thenThrow(new IllegalStateException("database down"));
    var adapter = new AmqpClassEventAdapter(provider, mock(ObjectProvider.class));
    assertThatThrownBy(() -> adapter.classDeleted(UUID.randomUUID(), Instant.EPOCH))
        .isInstanceOf(IllegalStateException.class);
  }
}
