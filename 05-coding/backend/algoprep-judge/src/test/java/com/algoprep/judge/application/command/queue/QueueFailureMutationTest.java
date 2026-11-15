package com.algoprep.judge.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.judge.application.ports.in.ResolveQueueFailureUseCase;
import com.algoprep.judge.domain.ports.out.QueueFailureRepository;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;

class QueueFailureMutationTest {
  @TempDir Path temporary;

  @ParameterizedTest
  @ValueSource(strings = {"WORKER", "NULL_SCORE", "SUBMITTED_AT", "FIRST_ATTEMPT"})
  void copiedFinalizationCannotLoseWorkerStateOrCorruptTheGradedContract(String rule)
      throws Exception {
    Path file =
        Path.of(
            "src/main/java/com/algoprep/judge/application/command/ResolveQueueFailureCommandHandler.java");
    String source = Files.readString(file);
    String mutant =
        switch (rule) {
          case "WORKER" -> source.replace("if (changed.isEmpty())", "if (false)");
          case "NULL_SCORE" ->
              source.replace(
                  "payload.putNull(\"passedTestcaseRatio\");",
                  "payload.put(\"passedTestcaseRatio\", 0);");
          case "SUBMITTED_AT" ->
              source.replace(
                  "submission.timing().submittedAt().toString()",
                  "submission.timing().finishedAt().toString()");
          default -> source.replace("repository.firstAttempt(submission)", "false");
        };
    assertThat(mutant).isNotEqualTo(source);
    candidate(source, rule, false);
    candidate(mutant, rule, true);
    assertThat(Files.readString(file)).isEqualTo(source);
    System.out.println("MUTATION CAUGHT QUEUE_FINAL_" + rule + "; original GREEN; mutant RED");
  }

  private void candidate(String source, String rule, boolean mutant) throws Exception {
    Path folder = compile(source, mutant);
    var f = new QueueFailureFixture();
    if (rule.equals("WORKER")) {
      when(f.repository.finishPending(f.id)).thenReturn(Optional.empty());
      when(f.repository.find(f.id)).thenReturn(Optional.of(f.view));
    }
    clearInvocations(f.repository);
    try (var loader =
        new URLClassLoader(new URL[] {folder.toUri().toURL()}, getClass().getClassLoader())) {
      var constructor =
          loader
              .loadClass(
                  "com.algoprep.judge.application.command.mutationcopy.ResolveQueueFailureCommandHandler")
              .getDeclaredConstructor(
                  QueueFailureRepository.class,
                  IdGeneratorPort.class,
                  PlatformTransactionManager.class);
      constructor.setAccessible(true);
      var useCase =
          (ResolveQueueFailureUseCase) constructor.newInstance(f.repository, f.ids, f.manager);
      Runnable oracle =
          () ->
              assertThatCode(
                      () -> {
                        var result = useCase.resolve(f.id);
                        if (rule.equals("WORKER")) {
                          assertThat(result.finalized()).isFalse();
                        } else {
                          var body = ArgumentCaptor.forClass(String.class);
                          verify(f.repository).appendGraded(any(), any(), body.capture(), any());
                          var payload =
                              JsonMapper.builder()
                                  .build()
                                  .readTree(body.getValue())
                                  .path("payload");
                          assertThat(payload.path("passedTestcaseRatio").isNull()).isTrue();
                          assertThat(payload.path("submittedAt").asText())
                              .isEqualTo(f.submitted.toString());
                          assertThat(payload.path("firstAttemptForProblem").asBoolean()).isTrue();
                        }
                      })
                  .doesNotThrowAnyException();
      if (mutant) {
        assertThatThrownBy(oracle::run).isInstanceOf(AssertionError.class);
      } else {
        oracle.run();
      }
    }
  }

  private Path compile(String source, boolean mutant) throws Exception {
    Path folder = Files.createDirectories(temporary.resolve(mutant ? "mutant" : "original"));
    Path file = folder.resolve("ResolveQueueFailureCommandHandler.java");
    String prefix = "package com.algoprep.judge.application.command;";
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
