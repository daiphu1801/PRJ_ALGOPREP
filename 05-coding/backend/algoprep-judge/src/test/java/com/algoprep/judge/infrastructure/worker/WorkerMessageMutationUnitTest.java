package com.algoprep.judge.infrastructure.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.algoprep.judge.application.ports.in.ProcessQueuedSubmissionUseCase;
import com.rabbitmq.client.Channel;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;

/** Compiles isolated source copies; the production listener remains unchanged. */
class WorkerMessageMutationUnitTest {
  private static final Path SOURCE =
      Path.of("src/main/java/com/algoprep/judge/infrastructure/worker/WorkerMessageListener.java");
  @TempDir Path temporary;

  @ParameterizedTest
  @ValueSource(strings = {"ID_ONLY", "ACK_AFTER_COMMIT", "REQUEUE"})
  void messageContractMustKillEachMutant(String rule) throws Exception {
    String source = Files.readString(SOURCE);
    String mutant =
        switch (rule) {
          case "ID_ONLY" -> source.replace("node.size() != 1", "false");
          case "ACK_AFTER_COMMIT" ->
              source.replace(
                  "process.process(id);", "channel.basicAck(tag, false); process.process(id);");
          default ->
              source.replace(
                  "channel.basicNack(tag, false, true);", "channel.basicAck(tag, false);");
        };
    assertThat(mutant).isNotEqualTo(source);
    candidate(source, rule, false);
    candidate(mutant, rule, true);
    assertThat(Files.readString(SOURCE)).isEqualTo(source);
    System.out.println("MUTATION CAUGHT WORKER_" + rule + "; original GREEN; mutant RED");
  }

  private void candidate(String source, String rule, boolean mutant) throws Exception {
    Path folder = compile(source, mutant);
    try (var loader =
        new URLClassLoader(new URL[] {folder.toUri().toURL()}, getClass().getClassLoader())) {
      var process = mock(ProcessQueuedSubmissionUseCase.class);
      var channel = mock(Channel.class);
      var type =
          loader.loadClass(
              "com.algoprep.judge.infrastructure.worker.mutationcopy.WorkerMessageListener");
      var constructor = type.getDeclaredConstructor(ProcessQueuedSubmissionUseCase.class);
      constructor.setAccessible(true);
      var listener = (ChannelAwareMessageListener) constructor.newInstance(process);
      if (mutant) {
        assertThatThrownBy(() -> oracle(listener, process, channel, rule))
            .isInstanceOf(AssertionError.class);
      } else {
        oracle(listener, process, channel, rule);
      }
    }
  }

  private void oracle(
      ChannelAwareMessageListener listener,
      ProcessQueuedSubmissionUseCase process,
      Channel channel,
      String rule)
      throws Exception {
    UUID id = UUID.randomUUID();
    String body =
        "{\"submissionId\":\""
            + id
            + "\""
            + (rule.equals("ID_ONLY") ? ",\"sourceCode\":\"private\"" : "")
            + "}";
    if (rule.equals("REQUEUE")) {
      org.mockito.Mockito.doThrow(new IllegalStateException("database unavailable"))
          .when(process)
          .process(id);
    }
    listener.onMessage(WorkerConsumerUnitTest.message(body), channel);
    if (rule.equals("ID_ONLY")) {
      verifyNoInteractions(process);
      verify(channel).basicReject(7, false);
    } else if (rule.equals("REQUEUE")) {
      verify(channel).basicNack(7, false, true);
    } else {
      var order = org.mockito.Mockito.inOrder(process, channel);
      order.verify(process).process(id);
      order.verify(channel).basicAck(7, false);
      org.mockito.Mockito.verifyNoMoreInteractions(channel);
    }
  }

  private Path compile(String source, boolean mutant) throws Exception {
    Path folder = Files.createDirectories(temporary.resolve(mutant ? "mutant" : "original"));
    Path file = folder.resolve("WorkerMessageListener.java");
    Files.writeString(
        file,
        source.replace(
            "package com.algoprep.judge.infrastructure.worker;",
            "package com.algoprep.judge.infrastructure.worker.mutationcopy;"));
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
