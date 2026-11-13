package com.algoprep.problembank.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.problembank.domain.model.OutboxEvent;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Return;
import com.rabbitmq.client.ReturnCallback;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.env.MockEnvironment;

class RabbitProblemDomainEventAdapterTest {
  private final ConnectionFactory factory = mock(ConnectionFactory.class);
  private final Connection connection = mock(Connection.class);
  private final Channel channel = mock(Channel.class);

  private RabbitProblemDomainEventAdapter adapter() throws Exception {
    when(factory.newConnection()).thenReturn(connection);
    when(connection.createChannel()).thenReturn(channel);
    return new RabbitProblemDomainEventAdapter(factory);
  }

  private static OutboxEvent event() {
    return new OutboxEvent(
        UUID.randomUUID(), "ProblemPublished", "problem.problem.published", "{\"v\":1}", 4);
  }

  @Test
  void persistentEnvelopeAndIdentityAreReplayedUnchanged() throws Exception {
    var adapter = adapter();
    var event = event();
    when(channel.waitForConfirms(5000)).thenReturn(true);
    assertThat(adapter.publish(event)).isTrue();
    var props = ArgumentCaptor.forClass(AMQP.BasicProperties.class);
    var body = ArgumentCaptor.forClass(byte[].class);
    verify(channel)
        .basicPublish(
            eq("algoprep.domain-events"),
            eq("problem.problem.published"),
            eq(true),
            props.capture(),
            body.capture());
    assertThat(props.getValue().getMessageId()).isEqualTo(event.id().toString());
    assertThat(props.getValue().getType()).isEqualTo("ProblemPublished");
    assertThat(props.getValue().getDeliveryMode()).isEqualTo(2);
    assertThat(new String(body.getValue(), StandardCharsets.UTF_8)).isEqualTo(event.envelope());
    verify(channel).exchangeDeclare("algoprep.domain-events", "topic", true);
    verify(channel).confirmSelect();
    verify(connection).abort(100);
  }

  @ParameterizedTest
  @ValueSource(strings = {"NACK", "RETURN", "THROW"})
  void uncertainOrUnroutableDeliveryRemainsPending(String fault) throws Exception {
    var adapter = adapter();
    var callback = new AtomicReference<ReturnCallback>();
    doAnswer(
            call -> {
              callback.set(call.getArgument(0));
              return null;
            })
        .when(channel)
        .addReturnListener(any(ReturnCallback.class));
    when(channel.waitForConfirms(5000))
        .thenAnswer(
            call -> {
              if (fault.equals("THROW")) {
                throw new IllegalStateException("credentials");
              }
              if (fault.equals("RETURN")) {
                callback.get().handle(mock(Return.class));
                return true;
              }
              return false;
            });
    assertThat(adapter.publish(event())).isFalse();
    verify(connection).abort(100);
  }

  @Test
  void interruptedCallerNeverSends() throws Exception {
    var adapter = adapter();
    Thread.currentThread().interrupt();
    try {
      assertThat(adapter.publish(event())).isFalse();
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
    } finally {
      Thread.interrupted();
    }
    verifyNoInteractions(factory);
  }

  @Test
  void interruptionWhileAwaitingConfirmIsPreserved() throws Exception {
    var adapter = adapter();
    var entered = new CountDownLatch(1);
    when(channel.waitForConfirms(5000))
        .thenAnswer(
            call -> {
              entered.countDown();
              Thread.sleep(20000);
              return true;
            });
    var confirmed = new AtomicBoolean(true);
    var interrupted = new AtomicBoolean();
    Thread caller =
        Thread.startVirtualThread(
            () -> {
              confirmed.set(adapter.publish(event()));
              interrupted.set(Thread.currentThread().isInterrupted());
            });
    assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
    caller.interrupt();
    caller.join(5000);
    assertThat(caller.isAlive()).isFalse();
    assertThat(confirmed.get()).isFalse();
    assertThat(interrupted.get()).isTrue();
  }

  @Test
  void environmentConstructorIsLazyWithoutInfrastructure() {
    assertThat(new RabbitProblemDomainEventAdapter(new MockEnvironment())).isNotNull();
  }
}
