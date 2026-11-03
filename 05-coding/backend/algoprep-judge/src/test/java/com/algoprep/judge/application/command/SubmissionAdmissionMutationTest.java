package com.algoprep.judge.application.command;

import static com.algoprep.judge.application.command.RecordPendingSubmissionCommandHandlerTest.rejected;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.judge.application.ports.in.RecordPendingSubmissionUseCase;
import com.algoprep.judge.application.ports.out.ProblemGradingSpecPort;
import com.algoprep.judge.domain.ports.out.SubmissionAdmissionRepository;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.PlatformTransactionManager;

class SubmissionAdmissionMutationTest {
  @TempDir Path temporary;

  @ParameterizedTest
  @ValueSource(
      strings = {
        "PUBLISHED",
        "SNAPSHOT",
        "WRAPPER",
        "TESTCASES",
        "ENABLED",
        "QUOTA",
        "BYTES",
        "BLANK",
        "FILE"
      })
  void copiedAdmissionCannotSkipItsGuards(String rule) throws Exception {
    Path directory = Path.of("src/main/java/com/algoprep/judge/application/command");
    String handler =
        Files.readString(directory.resolve("RecordPendingSubmissionCommandHandler.java"));
    String validation = Files.readString(directory.resolve("SubmissionAdmissionValidation.java"));
    String changedHandler =
        switch (rule) {
          case "PUBLISHED" -> handler.replace("!spec.problem().published()", "false");
          case "SNAPSHOT" -> handler.replace("!spec.problem().snapshotReady()", "false");
          case "WRAPPER" -> handler.replace("!spec.problem().functionWrapperSupported()", "false");
          case "TESTCASES" -> handler.replace("spec.testcases().isEmpty()", "false");
          case "ENABLED" ->
              handler.replace("!repository.languageEnabled(program.language())", "false");
          case "QUOTA" -> handler.replace(">= limit", "> limit");
          default -> handler;
        };
    String changedValidation =
        switch (rule) {
          case "BYTES" ->
              validation.replace(
                  "getBytes(StandardCharsets.UTF_8).length > 65536", "length() > 65536");
          case "BLANK" ->
              validation.replace(
                  "program.sourceCode().isBlank()", "program.sourceCode().isEmpty()");
          case "FILE" ->
              validation.replace("!fileName.toLowerCase(Locale.ROOT).endsWith(extension)", "false");
          default -> validation;
        };
    assertThat(changedHandler + changedValidation).isNotEqualTo(handler + validation);
    candidate(handler, validation, rule, false);
    candidate(changedHandler, changedValidation, rule, true);
    assertThat(Files.readString(directory.resolve("RecordPendingSubmissionCommandHandler.java")))
        .isEqualTo(handler);
    assertThat(Files.readString(directory.resolve("SubmissionAdmissionValidation.java")))
        .isEqualTo(validation);
    System.out.println("MUTATION CAUGHT ADMISSION_" + rule + "; original GREEN; mutant RED");
  }

  private void candidate(String handler, String validation, String rule, boolean mutant)
      throws Exception {
    Path folder = Files.createDirectories(temporary.resolve(mutant ? "mutant" : "original"));
    compile(folder, handler, validation);
    var f = new SubmissionAdmissionFixture();
    f.spec(
        !rule.equals("PUBLISHED"),
        !rule.equals("SNAPSHOT"),
        !rule.equals("WRAPPER"),
        rule.equals("TESTCASES") ? 0 : 1,
        null);
    if (rule.equals("ENABLED")) {
      when(f.repository.languageEnabled("PYTHON")).thenReturn(false);
    }
    if (rule.equals("QUOTA")) {
      when(f.repository.countRecent(f.owner, f.at)).thenReturn(60L);
    }
    String source =
        rule.equals("BYTES") ? "đ".repeat(32769) : rule.equals("BLANK") ? " \t\r\n" : "pass";
    var command =
        f.command(
            source,
            "PYTHON",
            rule.equals("WRAPPER") ? "FUNCTION_WRAPPER" : "STANDARD_IO",
            rule.equals("FILE") ? "FILE_UPLOAD" : "EDITOR",
            "main.java");
    try (var loader =
        new URLClassLoader(new URL[] {folder.toUri().toURL()}, getClass().getClassLoader())) {
      var type =
          loader.loadClass(
              "com.algoprep.judge.application.command.mutationcopy.RecordPendingSubmissionCommandHandler");
      var constructor =
          type.getDeclaredConstructor(
              SubmissionAdmissionRepository.class,
              ProblemGradingSpecPort.class,
              IdGeneratorPort.class,
              PlatformTransactionManager.class,
              int.class);
      constructor.setAccessible(true);
      var useCase =
          (RecordPendingSubmissionUseCase)
              constructor.newInstance(f.repository, f.problems, f.ids, f.manager, 60);
      Runnable oracle = () -> rejected(() -> useCase.recordPending(command), code(rule));
      if (mutant) {
        assertThatThrownBy(oracle::run).isInstanceOf(AssertionError.class);
      } else {
        oracle.run();
      }
    }
  }

  private static String code(String rule) {
    return switch (rule) {
      case "PUBLISHED" -> "JDG-101";
      case "SNAPSHOT" -> "JDG-504";
      case "WRAPPER" -> "JDG-103";
      case "TESTCASES" -> "JDG-104";
      case "ENABLED" -> "JDG-102";
      case "QUOTA" -> "JDG-106";
      case "BYTES" -> "JDG-V02";
      case "BLANK" -> "JDG-V01";
      default -> "JDG-V06";
    };
  }

  private static void compile(Path folder, String handler, String validation) throws Exception {
    String prefix = "package com.algoprep.judge.application.command;";
    Path writer = folder.resolve("RecordPendingSubmissionCommandHandler.java");
    Path validator = folder.resolve("SubmissionAdmissionValidation.java");
    Files.writeString(writer, handler.replace(prefix, prefix.replace(";", ".mutationcopy;")));
    Files.writeString(validator, validation.replace(prefix, prefix.replace(";", ".mutationcopy;")));
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
                writer.toString(),
                validator.toString());
    assertThat(exit).withFailMessage(errors.toString(StandardCharsets.UTF_8)).isZero();
  }
}
