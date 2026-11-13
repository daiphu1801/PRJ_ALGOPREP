package com.algoprep.judge.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.judge.application.ports.out.SubmissionQueuePort;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Return;
import com.rabbitmq.client.ReturnCallback;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class RabbitSubmissionQueueAdapterTest {
  @TempDir Path temporary;
  private final ConnectionFactory factory = mock(ConnectionFactory.class);
  private final Connection connection = mock(Connection.class);
  private final Channel channel = mock(Channel.class);

  private RabbitSubmissionQueueAdapter adapter() throws Exception {
    when(factory.newConnection()).thenReturn(connection);
    when(connection.createChannel()).thenReturn(channel);
    return new RabbitSubmissionQueueAdapter(factory);
  }

  @Test
  void confirmedPersistentJsonContainsOnlyTheIdAndDeclaresADurableQueue() throws Exception {
    var adapter = adapter();
    when(channel.waitForConfirms(3000)).thenReturn(true);
    UUID id = UUID.randomUUID();
    assertThat(adapter.enqueue(id)).isTrue();
    verify(channel).confirmSelect();
    verify(channel).queueDeclare("judge.submissions", true, false, false, null);
    var properties = ArgumentCaptor.forClass(AMQP.BasicProperties.class);
    var bytes = ArgumentCaptor.forClass(byte[].class);
    verify(channel)
        .basicPublish(
            org.mockito.ArgumentMatchers.eq(""),
            org.mockito.ArgumentMatchers.eq("judge.submissions"),
            org.mockito.ArgumentMatchers.eq(true),
            properties.capture(),
            bytes.capture());
    assertThat(new String(bytes.getValue(), StandardCharsets.UTF_8))
        .isEqualTo("{\"submissionId\":\"" + id + "\"}");
    assertThat(properties.getValue().getDeliveryMode()).isEqualTo(2);
    assertThat(properties.getValue().getContentType()).isEqualTo("application/json");
    assertThat(properties.getValue().getMessageId()).isEqualTo(id.toString());
    verify(factory).newConnection();
    verify(connection).abort(100);
  }

  @ParameterizedTest
  @ValueSource(strings = {"NACK", "RETURN", "THROW"})
  void nackReturnAndTransportFailureAreRetriedOnlyThreeTimes(String fault) throws Exception {
    var adapter = adapter();
    AtomicReference<ReturnCallback> callback = new AtomicReference<>();
    doAnswer(
            call -> {
              callback.set(call.getArgument(0));
              return null;
            })
        .when(channel)
        .addReturnListener(any(ReturnCallback.class));
    when(channel.waitForConfirms(3000))
        .thenAnswer(
            call -> {
              if (fault.equals("THROW")) {
                throw new IllegalStateException("raw credentials");
              }
              if (fault.equals("RETURN")) {
                callback.get().handle(mock(Return.class));
                return true;
              }
              return false;
            });
    long before = System.nanoTime();
    assertThat(adapter.enqueue(UUID.randomUUID())).isFalse();
    assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - before))
        .isGreaterThanOrEqualTo(680);
    verify(factory, times(3)).newConnection();
    verify(connection, times(3)).abort(100);
  }

  @Test
  void aLaterConfirmStopsRetriesImmediately() throws Exception {
    var adapter = adapter();
    when(channel.waitForConfirms(3000)).thenReturn(false, true);
    assertThat(adapter.enqueue(UUID.randomUUID())).isTrue();
    verify(factory, times(2)).newConnection();
  }

  @Test
  void anAlreadyInterruptedCallerDoesNotPublishAndKeepsItsInterruptFlag() throws Exception {
    var adapter = adapter();
    Thread.currentThread().interrupt();
    try {
      assertThat(adapter.enqueue(UUID.randomUUID())).isFalse();
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
      verifyNoInteractions(factory);
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  void interruptionWhileWaitingCancelsTheAttemptAndKeepsItsFlag() throws Exception {
    var adapter = adapter();
    CountDownLatch entered = new CountDownLatch(1);
    when(channel.waitForConfirms(anyLong()))
        .thenAnswer(
            call -> {
              entered.countDown();
              Thread.sleep(10000);
              return true;
            });
    AtomicBoolean confirmed = new AtomicBoolean(true);
    AtomicBoolean interrupted = new AtomicBoolean();
    Thread caller =
        Thread.startVirtualThread(
            () -> {
              confirmed.set(adapter.enqueue(UUID.randomUUID()));
              interrupted.set(Thread.currentThread().isInterrupted());
            });
    assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
    caller.interrupt();
    caller.join(5000);
    assertThat(caller.isAlive()).isFalse();
    assertThat(confirmed.get()).isFalse();
    assertThat(interrupted.get()).isTrue();
    verify(factory).newConnection();
  }

  @Test
  void aSlowConnectionCannotExtendOneAttemptBeyondItsDeadline() throws Exception {
    var adapter = adapter();
    java.util.concurrent.atomic.AtomicInteger calls =
        new java.util.concurrent.atomic.AtomicInteger();
    when(factory.newConnection())
        .thenAnswer(
            call -> {
              if (calls.incrementAndGet() == 1) {
                Thread.sleep(10000);
              }
              return connection;
            });
    long before = System.nanoTime();
    assertThat(adapter.enqueue(UUID.randomUUID())).isFalse();
    long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - before);
    assertThat(elapsed).isBetween(3600L, 6500L);
    assertThat(calls.get()).isEqualTo(3);
  }

  @ParameterizedTest
  @ValueSource(strings = {"ACK", "RETURN", "CAP", "MANDATORY"})
  void copiedPublisherCannotAcceptAnUnconfirmedOrReturnedMessage(String rule) throws Exception {
    Path file =
        Path.of(
            "src/main/java/com/algoprep/judge/infrastructure/messaging/RabbitSubmissionQueueAdapter.java");
    String source = Files.readString(file);
    String mutant =
        switch (rule) {
          case "ACK" -> source.replace("channel.waitForConfirms(3000)", "true");
          case "RETURN" -> source.replace("!returned.get()", "true");
          case "CAP" -> source.replace("attempt < 3", "attempt < 4");
          default -> source.replace("QUEUE, true, properties", "QUEUE, false, properties");
        };
    assertThat(mutant).isNotEqualTo(source);
    publishCandidate(source, rule, false);
    publishCandidate(mutant, rule, true);
    assertThat(Files.readString(file)).isEqualTo(source);
    System.out.println("MUTATION CAUGHT QUEUE_PUBLISH_" + rule + "; original GREEN; mutant RED");
  }

  private void publishCandidate(String source, String rule, boolean mutant) throws Exception {
    reset(factory, connection, channel);
    adapter();
    AtomicReference<ReturnCallback> returned = new AtomicReference<>();
    doAnswer(
            call -> {
              returned.set(call.getArgument(0));
              return null;
            })
        .when(channel)
        .addReturnListener(any(ReturnCallback.class));
    when(channel.waitForConfirms(3000))
        .thenAnswer(
            call -> {
              if (rule.equals("RETURN")) {
                returned.get().handle(mock(Return.class));
              }
              return rule.equals("RETURN") || rule.equals("MANDATORY");
            });
    Path folder = compilePublisher(source, mutant);
    try (var loader =
        new URLClassLoader(new URL[] {folder.toUri().toURL()}, getClass().getClassLoader())) {
      var constructor =
          loader
              .loadClass(
                  "com.algoprep.judge.infrastructure.messaging.mutationcopy.RabbitSubmissionQueueAdapter")
              .getDeclaredConstructor(ConnectionFactory.class);
      constructor.setAccessible(true);
      var publisher = (SubmissionQueuePort) constructor.newInstance(factory);
      Runnable oracle = publisherOracle(publisher, rule);
      if (mutant) {
        assertThatThrownBy(oracle::run).isInstanceOf(AssertionError.class);
      } else {
        oracle.run();
      }
    }
  }

  private Runnable publisherOracle(SubmissionQueuePort publisher, String rule) {
    return () ->
        assertThatCode(
                () -> {
                  assertThat(publisher.enqueue(UUID.randomUUID()))
                      .isEqualTo(rule.equals("MANDATORY"));
                  if (rule.equals("CAP")) {
                    verify(factory, times(3)).newConnection();
                  }
                  if (rule.equals("MANDATORY")) {
                    verify(channel)
                        .basicPublish(
                            org.mockito.ArgumentMatchers.eq(""),
                            org.mockito.ArgumentMatchers.eq("judge.submissions"),
                            org.mockito.ArgumentMatchers.eq(true),
                            any(AMQP.BasicProperties.class),
                            any(byte[].class));
                  }
                })
            .doesNotThrowAnyException();
  }

  private Path compilePublisher(String source, boolean mutant) throws Exception {
    Path folder = Files.createDirectories(temporary.resolve(mutant ? "mutant" : "original"));
    Path file = folder.resolve("RabbitSubmissionQueueAdapter.java");
    String prefix = "package com.algoprep.judge.infrastructure.messaging;";
    Files.writeString(file, source.replace(prefix, prefix.replace(";", ".mutationcopy;")));
    var errors = new ByteArrayOutputStream();
    int exit =
        ToolProvider.getSystemJavaCompiler()
            .run(
                null,
                errors,
                errors,
                "-proc:none",
                "-encoding",
                "UTF-8",
                "-classpath",
                System.getProperty("java.class.path"),
                "-d",
                folder.toString(),
                file.toString());
    assertThat(exit).withFailMessage(errors.toString(StandardCharsets.UTF_8)).isZero();
    return folder;
  }
}
