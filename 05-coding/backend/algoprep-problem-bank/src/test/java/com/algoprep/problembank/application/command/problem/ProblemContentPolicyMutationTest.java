package com.algoprep.problembank.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.problembank.application.dto.SaveProblemContentCommand;
import com.algoprep.problembank.application.ports.in.SaveProblemContentUseCase;
import com.algoprep.problembank.application.ports.out.ProblemListVersionPort;
import com.algoprep.problembank.domain.model.ProblemContentData;
import com.algoprep.problembank.domain.model.ProblemContentSnapshot;
import com.algoprep.problembank.domain.ports.out.ProblemContentRepository;
import java.io.ByteArrayOutputStream;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

class ProblemContentPolicyMutationTest {
  enum Rule {
    PERMISSION,
    OWNERSHIP,
    DRAFT_PRIVACY,
    STALE,
    PUBLISHED_EXAMPLES,
    FOREIGN_EXAMPLE
  }

  @TempDir Path temporary;

  @ParameterizedTest
  @EnumSource(Rule.class)
  void removingEachGuardBreaksIndependentOracle(Rule rule) throws Exception {
    Path file =
        Path.of(
            "src/main/java/com/algoprep/problembank/application/command/SaveProblemContentCommandHandler.java");
    String source = Files.readString(file);
    String mutant =
        switch (rule) {
          case PERMISSION -> source.replace("!command.actionGranted()", "false");
          case OWNERSHIP ->
              source.replace(
                  "!command.admin() && !old.authorId().equals(command.actorId())", "false");
          case DRAFT_PRIVACY ->
              source.replace("if (!\"PUBLISHED\".equals(old.status()))", "if (false)");
          case STALE ->
              source.replace("!old.updatedAt().equals(command.expectedUpdatedAt())", "false");
          case PUBLISHED_EXAMPLES -> source.replace("data.workedExamples().size() < 2", "false");
          case FOREIGN_EXAMPLE ->
              source.replace("example.id() != null && !owned.contains(example.id())", "false");
        };
    assertThat(mutant).isNotEqualTo(source);
    candidate(source, rule, false);
    candidate(mutant, rule, true);
    assertThat(Files.readString(file)).isEqualTo(source);
    System.out.println(
        "MUTATION CAUGHT CONTENT_"
            + rule
            + "; original GREEN; mutant oracle RED; source unchanged");
  }

  private void candidate(String source, Rule rule, boolean mutant) throws Exception {
    Path folder = Files.createDirectories(temporary.resolve(mutant ? "mutant" : "original"));
    Path handler = folder.resolve("SaveProblemContentCommandHandler.java");
    Path helper = folder.resolve("ProblemContentValidation.java");
    Files.writeString(handler, isolate(source));
    Files.writeString(
        helper,
        isolate(
            Files.readString(
                Path.of(
                    "src/main/java/com/algoprep/problembank/application/command/ProblemContentValidation.java"))));
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
      if (mutant) {
        assertThatThrownBy(() -> oracle(loader, rule)).isInstanceOf(AssertionError.class);
      } else {
        oracle(loader, rule);
      }
    }
  }

  private String isolate(String source) {
    return source.replace(
        "package com.algoprep.problembank.application.command;",
        "package com.algoprep.problembank.application.command.mutationcopy;");
  }

  private void oracle(ClassLoader loader, Rule rule) throws Exception {
    var repository = mock(ProblemContentRepository.class);
    UUID id = UUID.randomUUID();
    UUID actor = UUID.randomUUID();
    Instant time = Instant.parse("2026-11-02T00:00:00Z");
    boolean ownership = rule == Rule.OWNERSHIP || rule == Rule.DRAFT_PRIVACY;
    String status =
        rule == Rule.OWNERSHIP || rule == Rule.PUBLISHED_EXAMPLES ? "PUBLISHED" : "UNPUBLISHED";
    var two =
        List.of(
            new ProblemContentData.Example(null, 0, "1", "2", null),
            new ProblemContentData.Example(null, 0, "2", "3", null));
    var data =
        ContentTestData.replace(
            "workedExamples", rule == Rule.PUBLISHED_EXAMPLES ? List.of() : two);
    if (rule == Rule.FOREIGN_EXAMPLE) {
      data =
          ContentTestData.replace(
              "workedExamples",
              List.of(new ProblemContentData.Example(UUID.randomUUID(), 1, "1", "2", null)));
    }
    var old =
        new ProblemContentSnapshot(
            id,
            "FIXED",
            ownership ? UUID.randomUUID() : actor,
            status,
            ContentTestData.valid(),
            time);
    when(repository.lock(id)).thenReturn(Optional.of(old));
    when(repository.cataloguesExist(any(), any())).thenReturn(true);
    when(repository.write(any(), any(), any())).thenReturn(old);
    var handler = construct(loader, repository);
    var command =
        new SaveProblemContentCommand(
            id,
            actor,
            false,
            rule != Rule.PERMISSION,
            rule == Rule.STALE ? time.minusSeconds(1) : time,
            data);
    assertThatThrownBy(() -> handler.save(command))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo(expected(rule)));
    verify(repository, never()).write(any(), any(), any());
  }

  private String expected(Rule rule) {
    return switch (rule) {
      case PERMISSION -> "PRB-002";
      case OWNERSHIP -> "PRB-003";
      case DRAFT_PRIVACY -> "PRB-010";
      case STALE -> "PRB-020";
      case PUBLISHED_EXAMPLES -> "PRB-053";
      default -> "PRB-001";
    };
  }

  private SaveProblemContentUseCase construct(
      ClassLoader loader, ProblemContentRepository repository) throws Exception {
    String root = "com.algoprep.problembank.application.command.mutationcopy.";
    Class<?> validation = loader.loadClass(root + "ProblemContentValidation");
    var validator = validation.getDeclaredConstructor(Environment.class);
    validator.setAccessible(true);
    Class<?> type = loader.loadClass(root + "SaveProblemContentCommandHandler");
    var constructor =
        type.getDeclaredConstructor(
            ProblemContentRepository.class, validation, ProblemListVersionPort.class);
    constructor.setAccessible(true);
    return (SaveProblemContentUseCase)
        constructor.newInstance(
            repository,
            validator.newInstance(new MockEnvironment()),
            mock(ProblemListVersionPort.class));
  }
}
