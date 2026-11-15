package com.algoprep.judge.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.common.functionspec.Language;
import com.algoprep.common.functionspec.MatchingStrategy;
import com.algoprep.judge.application.dto.ExecutionModels.Compilation;
import com.algoprep.judge.application.dto.ExecutionModels.CompileReport;
import com.algoprep.judge.application.dto.ExecutionModels.CompileRequest;
import com.algoprep.judge.application.dto.ExecutionModels.EvaluationRequest;
import com.algoprep.judge.application.dto.ExecutionModels.EvaluationResult;
import com.algoprep.judge.application.dto.ExecutionModels.Limits;
import com.algoprep.judge.application.dto.ExecutionModels.Match;
import com.algoprep.judge.application.dto.ExecutionModels.Matching;
import com.algoprep.judge.application.dto.ExecutionModels.Model;
import com.algoprep.judge.application.dto.ExecutionModels.Program;
import com.algoprep.judge.application.dto.ExecutionModels.RunRequest;
import com.algoprep.judge.application.dto.ExecutionModels.RunResult;
import com.algoprep.judge.application.dto.ExecutionModels.Status;
import com.algoprep.judge.application.dto.ExecutionModels.Testcase;
import com.algoprep.judge.application.dto.ExecutionModels.Verdict;
import com.algoprep.judge.application.ports.in.EvaluateProgramUseCase;
import com.algoprep.judge.application.ports.out.HarnessEvaluationPort;
import com.algoprep.judge.application.ports.out.JudgeExecutionPort;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Controlled mutations of trusted source copies only; delivery source is never changed. */
class EvaluateProgramMutationTest {
  private static final String CLASS_NAME =
      "com.algoprep.judge.application.command.mutationcopy.EvaluateProgram";
  @TempDir Path temporary;

  enum Rule {
    DEBUG,
    HIDDEN,
    STDERR
  }

  @ParameterizedTest
  @EnumSource(Rule.class)
  void originalIsGreenAndWeakenedSourceCopyIsCaught(Rule rule) throws Exception {
    Path source =
        Path.of("src/main/java/com/algoprep/judge/application/command/EvaluateProgram.java");
    String original = Files.readString(source, StandardCharsets.UTF_8);
    assertProtected(rule, executeCopy(original, "original", rule));
    String changed = weaken(original, rule);
    var mutated = executeCopy(changed, "mutated", rule);
    assertThatThrownBy(() -> assertProtected(rule, mutated)).isInstanceOf(AssertionError.class);
    assertThat(Files.readString(source, StandardCharsets.UTF_8)).isEqualTo(original);
    System.out.println(
        "MUTATION CAUGHT "
            + rule
            + "; original copy GREEN; weakened copy oracle RED; delivery source unchanged");
  }

  private String weaken(String source, Rule rule) {
    return switch (rule) {
      case DEBUG ->
          replaceOnce(
              source,
              "WrapperDebugBudget.exceeded(run.stdout(), request.program().resultSentinel())",
              "false");
      case HIDDEN ->
          replaceOnce(
              replaceOnce(
                  source,
                  "actual = testcase.sample() ? snippet(match.actualOutput()) : null;",
                  "actual = snippet(match.actualOutput());"),
              "console = testcase.sample() ? snippet(match.userConsole()) : null;",
              "console = snippet(match.userConsole());");
      case STDERR ->
          replaceOnce(source, "&& request.program().model() == Model.STANDARD_IO", "&& true");
    };
  }

  private String replaceOnce(String source, String before, String after) {
    assertThat(source.indexOf(before))
        .isGreaterThanOrEqualTo(0)
        .isEqualTo(source.lastIndexOf(before));
    return source.replace(before, after);
  }

  private EvaluationResult executeCopy(String source, String label, Rule rule) throws Exception {
    Path folder = Files.createDirectories(temporary.resolve(label));
    Path java = folder.resolve("EvaluateProgram.java");
    // Test-only namespace prevents mutated bytecode from polluting production JaCoCo class IDs.
    String isolated =
        replaceOnce(
            source,
            "package com.algoprep.judge.application.command;",
            "package com.algoprep.judge.application.command.mutationcopy;");
    Files.writeString(java, isolated, StandardCharsets.UTF_8);
    var output = new ByteArrayOutputStream();
    int code =
        ToolProvider.getSystemJavaCompiler()
            .run(
                null,
                output,
                output,
                "-proc:none",
                "-encoding",
                "UTF-8",
                "-classpath",
                System.getProperty("java.class.path"),
                "-d",
                folder.toString(),
                java.toString());
    assertThat(code).withFailMessage(output.toString(StandardCharsets.UTF_8)).isZero();
    try (var loader = new CopyLoader(folder.toUri().toURL())) {
      var type = loader.loadClass(CLASS_NAME);
      var constructor =
          type.getDeclaredConstructor(JudgeExecutionPort.class, HarnessEvaluationPort.class);
      constructor.setAccessible(true);
      var evaluator = (EvaluateProgramUseCase) constructor.newInstance(engine(rule), harness());
      return evaluator.evaluate(request(rule));
    }
  }

  private EvaluationRequest request(Rule rule) {
    var limits = new Limits(1000, 268435456, 65536, 60);
    var program =
        new Program(
            Language.PYTHON,
            Model.FUNCTION_WRAPPER,
            Map.of(),
            null,
            null,
            "echo(value)",
            "@@APH-RESULT@@");
    return new EvaluationRequest(
        program,
        limits,
        new Matching(MatchingStrategy.EXACT, null, null),
        List.of(new Testcase("", "7", rule != Rule.HIDDEN, limits)),
        true);
  }

  private JudgeExecutionPort engine(Rule rule) {
    return new JudgeExecutionPort() {
      @Override
      public CompiledExecution compile(CompileRequest ignored) {
        return new CompiledExecution() {
          @Override
          public Compilation compilation() {
            return new Compilation(true, "");
          }

          @Override
          public RunResult run(RunRequest request) {
            String debug = rule == Rule.DEBUG ? "x".repeat(4097) : "";
            return new RunResult(
                rule == Rule.STDERR ? Status.RE : Status.OK,
                debug + "\n@@APH-RESULT@@\n7\n",
                "stderr secret",
                1,
                2);
          }

          @Override
          public void close() {
            /* No external resources in this unit fixture. */
          }
        };
      }
    };
  }

  private HarnessEvaluationPort harness() {
    return new HarnessEvaluationPort() {
      @Override
      public CompileReport mapCompile(Program program, String output) {
        return new CompileReport(false, "unused");
      }

      @Override
      public Match match(Program program, Matching matching, String stdout, String expected) {
        return new Match(true, "actual secret", "console secret");
      }
    };
  }

  private void assertProtected(Rule rule, EvaluationResult result) {
    var row = result.results().getFirst();
    assertThat(row.verdict()).isEqualTo(rule == Rule.HIDDEN ? Verdict.AC : Verdict.RE);
    assertThat(row.actualOutputSnippet()).isNull();
    assertThat(row.userConsole()).isNull();
    assertThat(row.stderrSnippet()).isNull();
  }

  private static final class CopyLoader extends URLClassLoader {
    CopyLoader(URL folder) {
      super(new URL[] {folder}, EvaluateProgramMutationTest.class.getClassLoader());
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      if (!name.equals(CLASS_NAME) && !name.startsWith(CLASS_NAME + "$")) {
        return super.loadClass(name, resolve);
      }
      synchronized (getClassLoadingLock(name)) {
        var loaded = findLoadedClass(name);
        if (loaded == null) {
          loaded = findClass(name);
        }
        if (resolve) {
          resolveClass(loaded);
        }
        return loaded;
      }
    }
  }
}
