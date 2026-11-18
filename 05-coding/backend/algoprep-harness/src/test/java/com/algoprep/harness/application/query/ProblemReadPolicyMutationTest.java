package com.algoprep.harness.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.functionspec.FunctionParameter;
import com.algoprep.common.functionspec.FunctionSpecDraft;
import com.algoprep.common.functionspec.Language;
import com.algoprep.common.functionspec.MatchingStrategy;
import com.algoprep.common.functionspec.TypeKind;
import com.algoprep.common.functionspec.TypeNode;
import com.algoprep.harness.application.dto.GetStarterCodeQuery;
import com.algoprep.harness.application.dto.PrepareSubmissionCommand;
import com.algoprep.harness.application.dto.ProblemSpecView;
import com.algoprep.harness.application.dto.SubmissionModel;
import com.algoprep.harness.application.ports.in.GetStarterCodeUseCase;
import com.algoprep.harness.application.ports.in.PrepareSubmissionUseCase;
import com.algoprep.harness.application.ports.out.ProblemSpecReaderPort;
import com.algoprep.harness.application.ports.out.StarterCodeAccessPort;
import com.algoprep.harness.application.prepare.ProgramPackaging;
import com.algoprep.harness.infrastructure.gojudge.GoJudgePayloadAssembler;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Compile isolated copies; never weaken the production guards in the workspace. */
class ProblemReadPolicyMutationTest {
  enum Rule {
    ACCESS,
    PINNED_VERSION,
    WRAPPER_FLAG,
    P5_RECHECK
  }

  @TempDir Path temporary;

  @ParameterizedTest
  @EnumSource(Rule.class)
  void removingEachGuardBreaksItsIndependentOracle(Rule rule) throws Exception {
    String simple =
        rule == Rule.ACCESS
            ? "GetStarterCodeQueryHandler"
            : rule == Rule.PINNED_VERSION
                ? "PrepareSubmissionCommandHandler"
                : "CurrentProblemSpecification";
    String area = rule == Rule.PINNED_VERSION ? "prepare" : "query";
    Path file = Path.of("src/main/java/com/algoprep/harness/application", area, simple + ".java");
    String source = Files.readString(file);
    String mutant =
        switch (rule) {
          case ACCESS -> source.replace("if (!readable)", "if (false)");
          case PINNED_VERSION ->
              source.replace("command.expectedSpecVersion() != spec.specVersion()", "false");
          case WRAPPER_FLAG ->
              source.replace("if (!spec.functionWrapperSupported())", "if (false)");
          case P5_RECHECK ->
              source.replace(
                  "!evaluation.violations().isEmpty() || !evaluation.wrapperSupported()", "false");
        };
    assertThat(mutant).isNotEqualTo(source);
    candidate(source, simple, area, rule, false);
    candidate(mutant, simple, area, rule, true);
    assertThat(Files.readString(file)).isEqualTo(source);
    System.out.println(
        "MUTATION CAUGHT READ_" + rule + "; original GREEN; mutant oracle RED; source unchanged");
  }

  private void candidate(String source, String simple, String area, Rule rule, boolean mutant)
      throws Exception {
    Path folder = Files.createDirectories(temporary.resolve(mutant ? "mutant" : "original"));
    Path file = folder.resolve(simple + ".java");
    String prefix = "com.algoprep.harness.application.";
    Files.writeString(file, isolate(source, simple, area));
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
                file.toString());
    assertThat(code).withFailMessage(errors.toString(StandardCharsets.UTF_8)).isZero();
    try (var loader =
        new URLClassLoader(new URL[] {folder.toUri().toURL()}, getClass().getClassLoader())) {
      Class<?> type = loader.loadClass(prefix + area + ".mutationcopy." + simple);
      if (mutant) {
        assertThatThrownBy(() -> oracle(type, rule)).isInstanceOf(AssertionError.class);
      } else {
        oracle(type, rule);
      }
    }
  }

  private String isolate(String source, String simple, String area) {
    String prefix = "com.algoprep.harness.application.";
    String imports =
        "\nimport "
            + prefix
            + "prepare.ProgramPackaging;\nimport "
            + prefix
            + "prepare.ProgramSourceAssembler;\n";
    if (!simple.equals("CurrentProblemSpecification")) {
      imports += "import " + prefix + "query.CurrentProblemSpecification;\n";
    }
    return source.replace(
        "package " + prefix + area + ";", "package " + prefix + area + ".mutationcopy;" + imports);
  }

  private void oracle(Class<?> type, Rule rule) throws Exception {
    UUID id = UUID.randomUUID();
    var reader = mock(ProblemSpecReaderPort.class);
    var integer = new TypeNode(TypeKind.INT, null, null);
    var draft =
        new FunctionSpecDraft(
            rule == Rule.P5_RECHECK ? "bad name" : "echo",
            integer,
            List.of(new FunctionParameter("value", integer)),
            Map.of(),
            MatchingStrategy.EXACT,
            null);
    var view = new ProblemSpecView(id, 130, draft, "in", "out", rule != Rule.WRAPPER_FLAG);
    when(reader.findSpec(id)).thenReturn(Optional.of(view));
    var current = new CurrentProblemSpecification(reader);
    switch (rule) {
      case ACCESS -> {
        starterOracle(type, current, id);
      }
      case PINNED_VERSION -> {
        var packaging = new ProgramPackaging(new GoJudgePayloadAssembler());
        var useCase =
            (PrepareSubmissionUseCase)
                type.getConstructor(CurrentProblemSpecification.class, ProgramPackaging.class)
                    .newInstance(current, packaging);
        assertCode(
            () ->
                useCase.prepare(
                    new PrepareSubmissionCommand(
                        id, Language.PYTHON, SubmissionModel.STANDARD_IO, "x", 129, null)),
            "HRN-002");
      }
      case WRAPPER_FLAG, P5_RECHECK -> {
        wrapperOracle(type, reader, view, rule);
      }
    }
  }

  private void starterOracle(Class<?> type, CurrentProblemSpecification current, UUID id)
      throws Exception {
    var access = mock(StarterCodeAccessPort.class);
    var useCase =
        (GetStarterCodeUseCase)
            type.getConstructor(StarterCodeAccessPort.class, CurrentProblemSpecification.class)
                .newInstance(access, current);
    assertCode(
        () ->
            useCase.getStarterCode(
                new GetStarterCodeQuery(
                    id,
                    Language.PYTHON,
                    SubmissionModel.STANDARD_IO,
                    UUID.randomUUID(),
                    "STUDENT")),
        "HRN-001");
  }

  private void wrapperOracle(
      Class<?> type, ProblemSpecReaderPort reader, ProblemSpecView view, Rule rule)
      throws Exception {
    Object policy = type.getConstructor(ProblemSpecReaderPort.class).newInstance(reader);
    var method = type.getMethod("validateWrapper", ProblemSpecView.class);
    assertThatThrownBy(
            () -> {
              try {
                method.invoke(policy, view);
              } catch (InvocationTargetException failure) {
                throw failure.getCause();
              }
            })
        .isInstanceOfSatisfying(
            AlgoPrepException.class,
            failure ->
                assertThat(failure.errorCode())
                    .isEqualTo(rule == Rule.WRAPPER_FLAG ? "HRN-004" : "HRN-006"));
  }

  private void assertCode(Runnable action, String code) {
    assertThatThrownBy(action::run)
        .isInstanceOfSatisfying(
            AlgoPrepException.class, failure -> assertThat(failure.errorCode()).isEqualTo(code));
  }
}
