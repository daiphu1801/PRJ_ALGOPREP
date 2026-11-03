package com.algoprep.judge.closeout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.common.functionspec.Language;
import com.algoprep.common.functionspec.MatchingStrategy;
import com.algoprep.judge.application.dto.ExecutionModels.Compilation;
import com.algoprep.judge.application.dto.ExecutionModels.CompileReport;
import com.algoprep.judge.application.dto.ExecutionModels.EvaluationRequest;
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
import com.algoprep.judge.domain.exception.JudgeInfrastructureException;
import java.io.ByteArrayOutputStream;
import java.net.URLClassLoader;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Exercises contract oracles against compiled source copies, never the delivered source. */
class RemainingExecutionMutationTest {
  private static final String COPY_PACKAGE = "com.algoprep.judge.closeout.copy";
  private static final Limits LIMITS = new Limits(1000, 268435456, 17, 60);
  @TempDir Path temporary;

  enum Rule {
    MODEL,
    LANGUAGE,
    EXIT,
    MARKER,
    SIGNATURE_MARKER,
    SIGNATURE_EXIT,
    ENGINE_STATUS,
    TLE,
    MLE,
    INTERNAL,
    HIDDEN_STDERR,
    ADMIN_STDERR,
    AC_STDERR,
    ENV_REMOVE,
    ENV_ADD,
    COLLECTOR_CAP,
    BODY_CAP
  }

  @ParameterizedTest
  @EnumSource(Rule.class)
  void originalPassesAndEachWeakenedCopyFailsItsContractOracle(Rule rule) throws Exception {
    String name = typeName(rule);
    Path source = Path.of("src/main/java/com/algoprep/judge", location(rule), name + ".java");
    String original = Files.readString(source, StandardCharsets.UTF_8);
    checkCopy(rule, name, original, "original", false);
    checkCopy(rule, name, weaken(original, rule), "mutated", true);
    assertThat(Files.readString(source, StandardCharsets.UTF_8)).isEqualTo(original);
    System.out.println(
        "MUTATION CAUGHT " + rule + "; original GREEN; mutant oracle RED; source unchanged");
  }

  private String location(Rule rule) {
    return switch (rule) {
      case HIDDEN_STDERR, ADMIN_STDERR, AC_STDERR -> "application/command";
      case ENV_REMOVE, ENV_ADD, COLLECTOR_CAP, BODY_CAP -> "infrastructure/gojudge";
      default -> "application/classify";
    };
  }

  private String typeName(Rule rule) {
    return switch (rule) {
      case HIDDEN_STDERR, ADMIN_STDERR, AC_STDERR -> "EvaluateProgram";
      case ENV_REMOVE, ENV_ADD, COLLECTOR_CAP -> "GoJudgeCommands";
      case BODY_CAP -> "BoundedJudgeBody";
      default -> "RuntimeClassifier";
    };
  }

  private String weaken(String source, Rule rule) {
    return switch (rule) {
      case MODEL -> replace(source, "if (model == Model.FUNCTION_WRAPPER)", "if (true)");
      case LANGUAGE -> replace(source, "&& language == Language.PYTHON", "&& true");
      case EXIT -> replace(source, "exit == 90 &&", "true &&");
      case MARKER ->
          replace(
              source,
              "stderr.equals(\"@@APH-HARNESS-INPUT-ERROR@@\\n\")",
              "stderr.startsWith(\"@@APH-HARNESS-INPUT-ERROR@@\\n\")");
      case SIGNATURE_MARKER ->
          replace(
              source,
              "stderr.equals(\"@@APH-HARNESS-SIGNATURE-ERROR@@\\n\")",
              "stderr.startsWith(\"@@APH-HARNESS-SIGNATURE-ERROR@@\\n\")");
      case SIGNATURE_EXIT -> replace(source, "if (exit == 92", "if (true");
      case ENGINE_STATUS ->
          replace(
              source,
              "case \"Accepted\" -> exit == 0 ? Status.OK : Status.RE;",
              "case \"Accepted\" -> nonzero(exit, stderr, model, language);");
      case TLE ->
          replace(
              source,
              "case \"Time Limit Exceeded\" -> Status.TLE;",
              "case \"Time Limit Exceeded\" -> nonzero(exit, stderr, model, language);");
      case MLE ->
          replace(
              source,
              "case \"Memory Limit Exceeded\" -> Status.MLE;",
              "case \"Memory Limit Exceeded\" -> nonzero(exit, stderr, model, language);");
      case INTERNAL ->
          replace(
              source,
              "default -> throw new JudgeInfrastructureException();",
              "default -> nonzero(exit, stderr, model, language);");
      default -> weakenPrivacyOrLimits(source, rule);
    };
  }

  private String weakenPrivacyOrLimits(String source, Rule rule) {
    return switch (rule) {
      case HIDDEN_STDERR -> replace(source, "&& testcase.sample()", "&& true");
      case ADMIN_STDERR -> replace(source, "request.returnStderr()", "true");
      case AC_STDERR -> replace(source, "&& verdict != Verdict.AC", "&& true");
      case ENV_REMOVE ->
          replace(
              source,
              "env.removeIf(value -> value.toString().startsWith(\"APH_MAX_RESULT_BYTES=\"));",
              "/* intentionally retain untrusted environment */");
      case ENV_ADD ->
          replace(
              source,
              "env.add(\"APH_MAX_RESULT_BYTES=\" + limits.outputBytes());",
              "/* intentionally omit authoritative limit */");
      case COLLECTOR_CAP ->
          replace(
              source,
              "cap = Math.addExact(cap, 16 + 4096);",
              "cap = Math.addExact(cap, 16 + 4097);");
      case BODY_CAP -> replace(source, "if (bytes > limit - received)", "if (false)");
      default -> throw new IllegalArgumentException("Not a privacy or limit mutation: " + rule);
    };
  }

  private String replace(String source, String before, String after) {
    assertThat(source.indexOf(before))
        .isGreaterThanOrEqualTo(0)
        .isEqualTo(source.lastIndexOf(before));
    return source.replace(before, after);
  }

  private void checkCopy(Rule rule, String name, String source, String label, boolean mutant)
      throws Exception {
    Path folder = Files.createDirectories(temporary.resolve(label));
    Path java = folder.resolve(name + ".java");
    Files.writeString(
        java,
        source.replaceFirst("package [^;]+;", "package " + COPY_PACKAGE + ";"),
        StandardCharsets.UTF_8);
    var compilerOutput = new ByteArrayOutputStream();
    int exit =
        ToolProvider.getSystemJavaCompiler()
            .run(
                null,
                compilerOutput,
                compilerOutput,
                "-proc:none",
                "-encoding",
                "UTF-8",
                "-classpath",
                System.getProperty("java.class.path"),
                "-d",
                folder.toString(),
                java.toString());
    assertThat(exit).withFailMessage(compilerOutput.toString(StandardCharsets.UTF_8)).isZero();
    try (var loader =
        new URLClassLoader(
            new java.net.URL[] {folder.toUri().toURL()}, getClass().getClassLoader())) {
      Class<?> type = loader.loadClass(COPY_PACKAGE + "." + name);
      if (mutant) {
        assertThatThrownBy(() -> oracle(rule, type)).isInstanceOf(AssertionError.class);
      } else {
        oracle(rule, type);
      }
    }
  }

  private void oracle(Rule rule, Class<?> type) throws Exception {
    switch (rule) {
      case HIDDEN_STDERR, ADMIN_STDERR, AC_STDERR -> privacyOracle(rule, type);
      case ENV_REMOVE, ENV_ADD, COLLECTOR_CAP -> commandOracle(type);
      case BODY_CAP -> bodyOracle(type);
      default -> classifierOracle(rule, type);
    }
  }

  private void classifierOracle(Rule rule, Class<?> type) throws Exception {
    var classifier = type.getConstructor().newInstance();
    var classify =
        type.getMethod(
            "classify", String.class, int.class, String.class, Model.class, Language.class);
    String engine =
        switch (rule) {
          case ENGINE_STATUS -> "Accepted";
          case TLE -> "Time Limit Exceeded";
          case MLE -> "Memory Limit Exceeded";
          case INTERNAL -> "Internal Error";
          default -> "Nonzero Exit Status";
        };
    int exit =
        rule == Rule.LANGUAGE || rule == Rule.SIGNATURE_MARKER
            ? 92
            : rule == Rule.EXIT || rule == Rule.SIGNATURE_EXIT ? 91 : 90;
    boolean signature =
        rule == Rule.LANGUAGE || rule == Rule.SIGNATURE_MARKER || rule == Rule.SIGNATURE_EXIT;
    String marker =
        signature ? "@@APH-HARNESS-SIGNATURE-ERROR@@\n" : "@@APH-HARNESS-INPUT-ERROR@@\n";
    if (rule == Rule.MARKER || rule == Rule.SIGNATURE_MARKER) {
      marker += "learner secret";
    }
    Model model = rule == Rule.MODEL ? Model.STANDARD_IO : Model.FUNCTION_WRAPPER;
    Language language = rule == Rule.LANGUAGE ? Language.JAVA : Language.PYTHON;
    if (rule == Rule.INTERNAL) {
      try {
        classify.invoke(classifier, engine, exit, marker, model, language);
        throw new AssertionError("Internal engine failure must remain infrastructure failure");
      } catch (java.lang.reflect.InvocationTargetException failure) {
        assertThat(failure.getCause()).isInstanceOf(JudgeInfrastructureException.class);
      }
    } else {
      var actual = classify.invoke(classifier, engine, exit, marker, model, language);
      assertThat(actual)
          .isEqualTo(rule == Rule.TLE ? Status.TLE : rule == Rule.MLE ? Status.MLE : Status.RE);
    }
  }

  private Program program(Model model) {
    var template =
        Map.<String, Object>of(
            "args",
            List.of("fixed"),
            "env",
            List.of("LC_ALL=C", "APH_MAX_RESULT_BYTES=999", "APH_MAX_RESULT_BYTES=1"),
            "copyIn",
            Map.of());
    return new Program(
        Language.PYTHON,
        model,
        Map.of("runCmd", template),
        null,
        null,
        "echo(value)",
        "@@APH-RESULT@@");
  }

  private void privacyOracle(Rule rule, Class<?> type) throws Exception {
    JudgeExecutionPort engine =
        ignored ->
            new JudgeExecutionPort.CompiledExecution() {
              public Compilation compilation() {
                return new Compilation(true, "");
              }

              public RunResult run(RunRequest request) {
                return new RunResult(Status.OK, "7\n", "hidden stderr", 1, 2);
              }

              public void close() {
                /* Unit fixture owns no resource. */
              }
            };
    HarnessEvaluationPort harness =
        new HarnessEvaluationPort() {
          public CompileReport mapCompile(Program program, String output) {
            return new CompileReport(false, "unused");
          }

          public Match match(Program program, Matching matching, String stdout, String expected) {
            return new Match(rule == Rule.AC_STDERR, "7", "");
          }
        };
    var constructor =
        type.getDeclaredConstructor(JudgeExecutionPort.class, HarnessEvaluationPort.class);
    constructor.setAccessible(true);
    var evaluator = (EvaluateProgramUseCase) constructor.newInstance(engine, harness);
    var request =
        new EvaluationRequest(
            program(Model.STANDARD_IO),
            LIMITS,
            new Matching(MatchingStrategy.EXACT, null, null),
            List.of(new Testcase("", "7", rule != Rule.HIDDEN_STDERR, LIMITS)),
            rule != Rule.ADMIN_STDERR);
    var result = evaluator.evaluate(request).results().getFirst();
    assertThat(result.verdict()).isEqualTo(rule == Rule.AC_STDERR ? Verdict.AC : Verdict.WA);
    assertThat(result.stderrSnippet()).isNull();
  }

  @SuppressWarnings("unchecked")
  private void commandOracle(Class<?> type) throws Exception {
    var run = type.getDeclaredMethod("run", Program.class, Map.class, String.class, Limits.class);
    run.setAccessible(true);
    var command =
        (Map<String, Object>)
            run.invoke(null, program(Model.FUNCTION_WRAPPER), Map.of(), "", LIMITS);
    assertThat((List<Object>) command.get("env"))
        .containsExactly("LC_ALL=C", "APH_MAX_RESULT_BYTES=17");
    var files = (List<Map<String, Object>>) command.get("files");
    assertThat(files.get(1).get("max")).isEqualTo(17L + 16 + 4096);
    assertThat(files.get(2).get("max")).isEqualTo(65536);
  }

  @SuppressWarnings("unchecked")
  private void bodyOracle(Class<?> type) throws Exception {
    var constructor = type.getDeclaredConstructor(long.class);
    constructor.setAccessible(true);
    var body = (HttpResponse.BodySubscriber<byte[]>) constructor.newInstance(4L);
    var cancelled = new AtomicBoolean();
    body.onSubscribe(
        new Flow.Subscription() {
          public void request(long count) {
            /* Chunks supplied synchronously by this fixture. */
          }

          public void cancel() {
            cancelled.set(true);
          }
        });
    body.onNext(List.of(ByteBuffer.wrap(new byte[] {1, 2}), ByteBuffer.wrap(new byte[] {3, 4})));
    assertThat(cancelled).isFalse();
    assertThat(body.getBody().toCompletableFuture()).isNotDone();
    body.onNext(List.of(ByteBuffer.wrap(new byte[] {5})));
    assertThat(cancelled).isTrue();
    assertThat(body.getBody().toCompletableFuture()).isCompletedExceptionally();
  }
}
