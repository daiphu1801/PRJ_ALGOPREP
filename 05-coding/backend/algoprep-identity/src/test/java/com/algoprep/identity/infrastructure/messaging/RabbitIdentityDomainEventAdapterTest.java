package com.algoprep.identity.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.identity.domain.model.outbox.OutboxEvent;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.env.MockEnvironment;

/** Outbox publish contract: confirmed and not returned means delivered; anything else retries. */
class RabbitIdentityDomainEventAdapterTest {
  private final ConnectionFactory factory = mock(ConnectionFactory.class);
  private final Connection connection = mock(Connection.class);
  private final Channel channel = mock(Channel.class);
  private final OutboxEvent event =
      new OutboxEvent(UUID.randomUUID(), "ClassDeleted", "class.deleted", "{\"a\":1}", 0);

  private RabbitIdentityDomainEventAdapter adapter() throws Exception {
    when(factory.newConnection()).thenReturn(connection);
    when(connection.createChannel()).thenReturn(channel);
    return new RabbitIdentityDomainEventAdapter(factory);
  }

  @Test
  void confirmedPublishIsPersistentWithMessageIdAndTypeAndClosesTheConnection() throws Exception {
    when(channel.waitForConfirms(anyLong())).thenReturn(true);
    assertThat(adapter().publish(event)).isTrue();
    ArgumentCaptor<AMQP.BasicProperties> props =
        ArgumentCaptor.forClass(AMQP.BasicProperties.class);
    verify(channel)
        .basicPublish(
            eq("algoprep.identity.events"), eq("class.deleted"), eq(true), props.capture(), any());
    assertThat(props.getValue().getMessageId()).isEqualTo(event.id().toString());
    assertThat(props.getValue().getType()).isEqualTo("ClassDeleted");
    assertThat(props.getValue().getDeliveryMode()).isEqualTo(2);
    verify(channel).exchangeDeclare("algoprep.identity.events", "topic", true);
    verify(connection).abort(anyInt());
  }

  @Test
  void unconfirmedPublishIsReportedAsFailure() throws Exception {
    when(channel.waitForConfirms(anyLong())).thenReturn(false);
    assertThat(adapter().publish(event)).isFalse();
  }

  @Test
  void brokerFailureIsReportedAsFailure() throws Exception {
    when(factory.newConnection()).thenThrow(new java.io.IOException("refused"));
    assertThat(new RabbitIdentityDomainEventAdapter(factory).publish(event)).isFalse();
  }

  @Test
  void returnedMessageCountsAsUndelivered() throws Exception {
    when(channel.waitForConfirms(anyLong())).thenReturn(true);
    ArgumentCaptor<com.rabbitmq.client.ReturnCallback> listener =
        ArgumentCaptor.forClass(com.rabbitmq.client.ReturnCallback.class);
    org.mockito.Mockito.doReturn(null).when(channel).addReturnListener(listener.capture());
    org.mockito.Mockito.doAnswer(
            inv -> {
              listener.getValue().handle(mock(com.rabbitmq.client.Return.class));
              return null;
            })
        .when(channel)
        .basicPublish(any(), any(), anyBoolean(), any(), any());
    assertThat(adapter().publish(event)).isFalse();
  }

  @Test
  void interruptedThreadSkipsThePublish() throws Exception {
    Thread.currentThread().interrupt();
    try {
      assertThat(adapter().publish(event)).isFalse();
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  void environmentConstructorReadsRabbitSettings() {
    MockEnvironment env =
        new MockEnvironment()
            .withProperty("spring.rabbitmq.host", "mq.local")
            .withProperty("spring.rabbitmq.port", "5673");
    assertThat(new RabbitIdentityDomainEventAdapter(env)).isNotNull();
  }
}
