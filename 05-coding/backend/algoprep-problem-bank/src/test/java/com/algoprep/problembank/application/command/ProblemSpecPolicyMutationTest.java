package com.algoprep.problembank.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.problembank.application.dto.SaveProblemSpecCommand;
import com.algoprep.problembank.application.ports.in.SaveProblemSpecUseCase;
import com.algoprep.problembank.application.ports.out.ProblemSpecCachePort;
import com.algoprep.problembank.domain.model.ProblemWriteState;
import com.algoprep.problembank.domain.ports.out.ProblemSpecRepository;
import java.io.ByteArrayOutputStream;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tools.jackson.databind.json.JsonMapper;

/** Mutations compile in an isolated namespace and never change delivery files. */
class ProblemSpecPolicyMutationTest {
  enum Rule {
    OWNERSHIP,
    DRAFT_PRIVACY,
    STALE_VERSION,
    HARD_VALIDATION,
    DURABLE_EVENT
  }

  @TempDir Path temporary;

  @ParameterizedTest
  @EnumSource(Rule.class)
  void weakenedGuardFailsIndependentContractOracle(Rule rule) throws Exception {
    Path root = Path.of("src/main/java/com/algoprep/problembank/application/command");
    String original = Files.readString(root.resolve("SaveProblemSpecCommandHandler.java"));
    String mutant =
        switch (rule) {
          case OWNERSHIP ->
              original.replace(
                  "!command.admin() && !state.authorId().equals(command.actorId())", "false");
          case DRAFT_PRIVACY ->
              original.replace("if (!\"PUBLISHED\".equals(state.status()))", "if (false)");
          case STALE_VERSION ->
              original.replace("!state.updatedAt().equals(command.expectedUpdatedAt())", "false");
          case HARD_VALIDATION -> original.replace("!evaluation.violations().isEmpty()", "false");
          case DURABLE_EVENT -> original.replaceFirst("repository\\.appendEvent\\([\\s\\S]*?;", "");
        };
    assertThat(mutant).isNotEqualTo(original);
    candidate(original, rule, false);
    candidate(mutant, rule, true);
    assertThat(Files.readString(root.resolve("SaveProblemSpecCommandHandler.java")))
        .isEqualTo(original);
    System.out.println(
        "MUTATION CAUGHT SPEC_" + rule + "; original GREEN; mutant oracle RED; source unchanged");
  }

  private void candidate(String source, Rule rule, boolean mutated) throws Exception {
    Path folder = Files.createDirectories(temporary.resolve(mutated ? "mutant" : "original"));
    Path handler = folder.resolve("SaveProblemSpecCommandHandler.java");
    Path helper = folder.resolve("ProblemSpecJson.java");
    Files.writeString(handler, isolate(source));
    Files.writeString(
        helper,
        isolate(
            Files.readString(
                Path.of(
                    "src/main/java/com/algoprep/problembank/application/command/ProblemSpecJson.java"))));
    var errors = new ByteArrayOutputStream();
    int code =
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
                handler.toString(),
                helper.toString());
    assertThat(code).withFailMessage(errors.toString(StandardCharsets.UTF_8)).isZero();
    try (var loader =
        new URLClassLoader(
            new java.net.URL[] {folder.toUri().toURL()}, getClass().getClassLoader())) {
      Class<?> type =
          loader.loadClass(
              "com.algoprep.problembank.application.command.mutationcopy.SaveProblemSpecCommandHandler");
      if (mutated) {
        assertThatThrownBy(() -> oracle(type, rule)).isInstanceOf(AssertionError.class);
      } else {
        oracle(type, rule);
      }
    }
  }

  private String isolate(String source) {
    return source.replace(
        "package com.algoprep.problembank.application.command;",
        "package com.algoprep.problembank.application.command.mutationcopy;");
  }

  private void oracle(Class<?> type, Rule rule) throws Exception {
    var repository = mock(ProblemSpecRepository.class);
    var cache = mock(ProblemSpecCachePort.class);
    var ids = mock(IdGeneratorPort.class);
    UUID id = UUID.randomUUID();
    UUID actor = UUID.randomUUID();
    Instant now = Instant.parse("2026-11-02T00:00:00Z");
    boolean ownership = rule == Rule.OWNERSHIP || rule == Rule.DRAFT_PRIVACY;
    when(repository.lock(id))
        .thenReturn(
            Optional.of(
                new ProblemWriteState(
                    ownership ? UUID.randomUUID() : actor,
                    rule == Rule.DRAFT_PRIVACY ? "UNPUBLISHED" : "PUBLISHED",
                    now)));
    when(repository.find(id)).thenReturn(Optional.empty());
    when(repository.touch(id, actor, true)).thenReturn(now.plusSeconds(1));
    when(repository.touch(id, actor, false)).thenReturn(now.plusSeconds(1));
    when(ids.newId()).thenReturn(UUID.randomUUID());
    var constructor = type.getDeclaredConstructors()[0];
    constructor.setAccessible(true);
    var handler = (SaveProblemSpecUseCase) constructor.newInstance(repository, cache, ids);
    String body =
        rule == Rule.HARD_VALIDATION
            ? "{\"functionName\":\"solve\"}"
            : SaveProblemSpecCommandHandlerTest.VALID;
    var command =
        new SaveProblemSpecCommand(
            id,
            actor,
            false,
            rule == Rule.STALE_VERSION ? now.minusSeconds(1) : now,
            JsonMapper.builder().build().readTree(body));
    if (rule == Rule.DURABLE_EVENT) {
      handler.save(command);
      verify(repository).appendEvent(any(), any());
    } else {
      String expected =
          switch (rule) {
            case OWNERSHIP -> "PRB-003";
            case DRAFT_PRIVACY -> "PRB-010";
            case STALE_VERSION -> "PRB-020";
            default -> "PRB-036";
          };
      assertThatThrownBy(() -> handler.save(command))
          .isInstanceOfSatisfying(
              AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo(expected));
      verify(repository, never()).save(any(), any());
    }
  }
}
