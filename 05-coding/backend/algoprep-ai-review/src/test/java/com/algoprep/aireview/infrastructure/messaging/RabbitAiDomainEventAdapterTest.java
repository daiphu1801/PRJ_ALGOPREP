package com.algoprep.aireview.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.aireview.domain.model.AiOutboxEvent;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ReturnListener;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.env.MockEnvironment;

/** Publisher-confirm contract: a row is confirmed only when the broker acks and nothing returns. */
class RabbitAiDomainEventAdapterTest {
  private final ConnectionFactory factory = mock(ConnectionFactory.class);
  private final Connection connection = mock(Connection.class);
  private final Channel channel = mock(Channel.class);
  private final AiOutboxEvent event =
      new AiOutboxEvent(
          UUID.randomUUID(), AiOutboxEvent.SOLUTION_REVIEW_COMPLETED, "{}", Instant.now(), 0);

  private RabbitAiDomainEventAdapter adapter() throws Exception {
    when(factory.newConnection()).thenReturn(connection);
    when(connection.createChannel()).thenReturn(channel);
    return new RabbitAiDomainEventAdapter(factory);
  }

  @Test
  void ackedPublishIsConfirmedWithMandatoryFlagAndRoutingKey() throws Exception {
    var adapter = adapter();
    when(channel.waitForConfirms(anyLong())).thenReturn(true);

    assertThat(adapter.publish(event, "{\"e\":1}")).isTrue();

    var props = ArgumentCaptor.forClass(AMQP.BasicProperties.class);
    verify(channel)
        .basicPublish(
            eq("algoprep.domain-events"),
            eq("ai.solution-review.completed"),
            eq(true),
            props.capture(),
            eq("{\"e\":1}".getBytes(StandardCharsets.UTF_8)));
    assertThat(props.getValue().getMessageId()).isEqualTo(event.id().toString());
    assertThat(props.getValue().getDeliveryMode()).isEqualTo(2);
    verify(connection).abort(100);
  }

  @Test
  void unroutableMessageReturnedByBrokerIsNotConfirmed() throws Exception {
    var adapter = adapter();
    when(channel.waitForConfirms(anyLong())).thenReturn(true);
    ReturnListener[] listener = new ReturnListener[1];
    doAnswer(
            call -> {
              listener[0] = call.getArgument(0);
              return null;
            })
        .when(channel)
        .addReturnListener(any(ReturnListener.class));
    doAnswer(
            call -> {
              listener[0].handleReturn(
                  312, "NO_ROUTE", "x", "k", new AMQP.BasicProperties(), new byte[0]);
              return null;
            })
        .when(channel)
        .basicPublish(any(), any(), eq(true), any(), any());

    assertThat(adapter.publish(event, "{}")).isFalse();
  }

  @Test
  void nackedConfirmKeepsTheRowPending() throws Exception {
    var adapter = adapter();
    when(channel.waitForConfirms(anyLong())).thenReturn(false);

    assertThat(adapter.publish(event, "{}")).isFalse();
  }

  @Test
  void brokerDownReturnsFalseInsteadOfThrowing() throws Exception {
    when(factory.newConnection()).thenThrow(new IOException("connection refused"));

    assertThat(new RabbitAiDomainEventAdapter(factory).publish(event, "{}")).isFalse();
  }

  @Test
  void interruptedCallerPublishesNothing() throws Exception {
    var adapter = adapter();
    Thread.currentThread().interrupt();
    try {
      assertThat(adapter.publish(event, "{}")).isFalse();
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  void environmentConstructorReadsBrokerSettings() {
    var env =
        new MockEnvironment()
            .withProperty("spring.rabbitmq.host", "mq.local")
            .withProperty("spring.rabbitmq.port", "5673")
            .withProperty("spring.rabbitmq.virtual-host", "/ai");

    assertThat(new RabbitAiDomainEventAdapter(env)).isNotNull();
  }
}
