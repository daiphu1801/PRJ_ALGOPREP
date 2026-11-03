package com.algoprep.judge.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.algoprep.judge.application.dto.SubmissionStatusProgress;
import com.algoprep.judge.application.ports.out.SubmissionProgressPort;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class SubmissionProgressMutationTest {
  @TempDir Path temporary;

  @AfterEach
  void clearTransaction() {
    TransactionSynchronizationManager.clear();
  }

  @ParameterizedTest
  @ValueSource(strings = {"COMMIT", "ROLLBACK", "OWNER", "PUSH_FAILURE"})
  void transactionAndOwnerRulesMustBeObserved(String rule) throws Exception {
    Path file =
        Path.of(
            "src/main/java/com/algoprep/judge/infrastructure/messaging/StompSubmissionProgressAdapter.java");
    String source = Files.readString(file);
    String mutant =
        switch (rule) {
          case "COMMIT" ->
              source.replace(
                  "TransactionSynchronizationManager.isActualTransactionActive()", "false");
          case "ROLLBACK" ->
              source.replace(
                  "public void afterCommit()", "public void afterCompletion(int status)");
          case "OWNER" -> source.replace("ownerId.toString()", "\"another-user\"");
          default ->
              source.replace("LOG.warn(\"Submission progress delivery failed\");", "throw error;");
        };
    assertThat(mutant).isNotEqualTo(source);
    candidate(source, rule, false);
    candidate(mutant, rule, true);
    assertThat(Files.readString(file)).isEqualTo(source);
    System.out.println("MUTATION CAUGHT PROGRESS_" + rule + "; original GREEN; mutant RED");
  }

  private void candidate(String source, String rule, boolean mutant) throws Exception {
    Path folder = compile(source, mutant);
    TransactionSynchronizationManager.clear();
    try (var loader =
        new URLClassLoader(new URL[] {folder.toUri().toURL()}, getClass().getClassLoader())) {
      var constructor =
          loader
              .loadClass(
                  "com.algoprep.judge.infrastructure.messaging.mutationcopy.StompSubmissionProgressAdapter")
              .getDeclaredConstructor(SimpMessagingTemplate.class);
      constructor.setAccessible(true);
      SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
      var adapter = (SubmissionProgressPort) constructor.newInstance(messaging);
      Runnable oracle = oracle(adapter, messaging, rule);
      if (mutant) {
        assertThatThrownBy(oracle::run).isInstanceOf(AssertionError.class);
      } else {
        oracle.run();
      }
    }
  }

  private static Runnable oracle(
      SubmissionProgressPort adapter, SimpMessagingTemplate messaging, String rule) {
    UUID owner = UUID.randomUUID();
    var event = new SubmissionStatusProgress(UUID.randomUUID(), Instant.now(), "COMPILING", null);
    return () ->
        assertThatCode(
                () -> {
                  if (rule.equals("PUSH_FAILURE")) {
                    doThrow(new IllegalStateException("Sensitive failure"))
                        .when(messaging)
                        .convertAndSendToUser(
                            eq(owner.toString()), eq("/queue/submissions"), same(event), anyMap());
                  }
                  if (rule.equals("COMMIT") || rule.equals("ROLLBACK")) {
                    TransactionSynchronizationManager.initSynchronization();
                    TransactionSynchronizationManager.setActualTransactionActive(true);
                  }
                  adapter.afterCommit(owner, event);
                  if (rule.equals("COMMIT") || rule.equals("ROLLBACK")) {
                    verifyNoInteractions(messaging);
                    if (rule.equals("ROLLBACK")) {
                      TransactionSynchronizationManager.getSynchronizations()
                          .forEach(
                              s ->
                                  s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
                      verifyNoInteractions(messaging);
                    } else {
                      TransactionSynchronizationManager.getSynchronizations()
                          .forEach(TransactionSynchronization::afterCommit);
                      verify(messaging)
                          .convertAndSendToUser(
                              eq(owner.toString()),
                              eq("/queue/submissions"),
                              same(event),
                              anyMap());
                    }
                  } else if (rule.equals("OWNER")) {
                    verify(messaging)
                        .convertAndSendToUser(
                            eq(owner.toString()), eq("/queue/submissions"), same(event), anyMap());
                  }
                })
            .doesNotThrowAnyException();
  }

  private Path compile(String source, boolean mutant) throws Exception {
    Path folder = Files.createDirectories(temporary.resolve(mutant ? "mutant" : "original"));
    String prefix = "package com.algoprep.judge.infrastructure.messaging;";
    Path file = folder.resolve("StompSubmissionProgressAdapter.java");
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
