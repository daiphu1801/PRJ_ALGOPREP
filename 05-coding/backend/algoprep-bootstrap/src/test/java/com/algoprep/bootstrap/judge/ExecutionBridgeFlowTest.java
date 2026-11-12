package com.algoprep.bootstrap.judge;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.common.functionspec.FunctionParameter;
import com.algoprep.common.functionspec.FunctionSpecDraft;
import com.algoprep.common.functionspec.Language;
import com.algoprep.common.functionspec.MatchingStrategy;
import com.algoprep.common.functionspec.TypeKind;
import com.algoprep.common.functionspec.TypeNode;
import com.algoprep.harness.application.dto.PackagedProgram;
import com.algoprep.harness.application.prepare.ProgramPackaging;
import com.algoprep.judge.application.dto.ExecutionModels.CompileRequest;
import com.algoprep.judge.application.dto.ExecutionModels.EvaluationRequest;
import com.algoprep.judge.application.dto.ExecutionModels.EvaluationResult;
import com.algoprep.judge.application.dto.ExecutionModels.Limits;
import com.algoprep.judge.application.dto.ExecutionModels.Matching;
import com.algoprep.judge.application.dto.ExecutionModels.RunRequest;
import com.algoprep.judge.application.dto.ExecutionModels.Status;
import com.algoprep.judge.application.dto.ExecutionModels.Testcase;
import com.algoprep.judge.application.dto.ExecutionModels.Verdict;
import com.algoprep.judge.application.ports.in.EvaluateProgramUseCase;
import com.algoprep.judge.application.ports.out.JudgeExecutionPort;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/** Real go-judge component flow; no DB, HTTP submission route, or fabricated F2 reader. */
@EnabledIfSystemProperty(named = "judge.go-judge.base-url", matches = "http://127.0.0.1:[0-9]+")
class ExecutionBridgeFlowTest {
  private static AnnotationConfigApplicationContext context;
  private static ProgramPackaging packaging;
  private static HarnessProgramBridge bridge;
  private static EvaluateProgramUseCase evaluator;
  private static JudgeExecutionPort engine;
  private static final TypeNode INT = new TypeNode(TypeKind.INT, null, null);
  private static final Limits COMPILE = new Limits(20000, 1073741824, 65536, 60);
  private static final Limits RUN = new Limits(3000, 1073741824, 1024, 60);

  @BeforeAll
  static void wireOnlyOwningComponents() throws Exception {
    context = new AnnotationConfigApplicationContext();
    context.scan(
        "com.algoprep.harness.application.compile",
        "com.algoprep.harness.application.query",
        "com.algoprep.harness.infrastructure.logging",
        "com.algoprep.harness.infrastructure.gojudge",
        "com.algoprep.harness.application.prepare");
    context.register(
        HarnessProgramBridge.class,
        com.algoprep.judge.infrastructure.gojudge.GoJudgeAdapter.class,
        Class.forName("com.algoprep.judge.application.command.EvaluateProgram"));
    context.refresh();
    packaging = context.getBean(ProgramPackaging.class);
    bridge = context.getBean(HarnessProgramBridge.class);
    evaluator = context.getBean(EvaluateProgramUseCase.class);
    engine = context.getBean(JudgeExecutionPort.class);
  }

  @AfterAll
  static void closeContext() {
    if (context != null) {
      context.close();
    }
  }

  private PackagedProgram wrapper(Language language, String source) {
    var draft =
        new FunctionSpecDraft(
            "echo",
            INT,
            List.of(new FunctionParameter("value", INT)),
            Map.of(),
            MatchingStrategy.EXACT,
            null);
    return packaging.functionWrapper(language, draft, source);
  }

  private String echo(Language language) {
    return switch (language) {
      case JAVA ->
          "class Solution { public int echo(int value) { System.out.println(\"debug\"); return value; } }";
      case CPP ->
          "class Solution { public: int echo(int value) { std::cout << \"debug\\n\"; return value; } };";
      case PYTHON -> "class Solution:\n def echo(self, value):\n  print('debug')\n  return value\n";
    };
  }

  private EvaluationResult evaluate(PackagedProgram program, List<Testcase> cases) {
    return evaluator.evaluate(
        new EvaluationRequest(
            bridge.program(program),
            COMPILE,
            new Matching(
                MatchingStrategy.EXACT, null, program.resultSentinel() == null ? null : INT),
            cases,
            true));
  }

  @ParameterizedTest
  @EnumSource(Language.class)
  void wrapperCompilesOnceRunsAllCasesAndProtectsHidden(Language language) {
    var result =
        evaluate(
            wrapper(language, echo(language)),
            List.of(
                new Testcase("7\n", "7\n", true, RUN), new Testcase("8\n", "9\n", true, RUN),
                new Testcase("999\n", "0\n", false, RUN), new Testcase("10\n", "10\n", true, RUN)));
    assertThat(result.verdict()).isEqualTo(Verdict.WA);
    assertThat(result.results())
        .extracting(row -> row.verdict())
        .containsExactly(Verdict.AC, Verdict.WA, Verdict.WA, Verdict.AC);
    assertThat(result.results().getFirst().userConsole()).isEqualTo("debug\n\n");
    assertThat(result.results().getFirst().actualOutputSnippet()).isEqualTo("7");
    assertThat(result.results().get(2).actualOutputSnippet()).isNull();
    assertThat(result.results().get(2).userConsole()).isNull();
    assertThat(result.results()).allSatisfy(row -> assertThat(row.stderrSnippet()).isNull());
    assertThat(result.toString()).doesNotContain("999");
  }

  @ParameterizedTest
  @EnumSource(Language.class)
  void standardIoHasRealStdinAndDoesNotNeedFunctionSpec(Language language) {
    String source =
        switch (language) {
          case JAVA ->
              "public class Main { public static void main(String[] a) { System.out.println(new java.util.Scanner(System.in).nextInt()+1); } }";
          case CPP -> "#include <iostream>\nint main(){int n;std::cin>>n;std::cout<<n+1<<'\\n';}";
          case PYTHON -> "print(int(input())+1)\n";
        };
    var result =
        evaluate(
            packaging.standardIo(language, source),
            List.of(new Testcase("4\n", "5\n", true, RUN), new Testcase("6\n", "0\n", true, RUN)));
    assertThat(result.results())
        .extracting(row -> row.verdict())
        .containsExactly(Verdict.AC, Verdict.WA);
    assertThat(result.results().getFirst().actualOutputSnippet()).isEqualTo("5");
  }

  @ParameterizedTest
  @EnumSource(Language.class)
  void realCompilerErrorsGoThroughP3WithoutRawSource(Language language) {
    String source =
        switch (language) {
          case JAVA -> "class Solution { public int echo(int value) { return broken; } }";
          case CPP -> "class Solution { public: int echo(int value) { return broken; } };";
          case PYTHON -> "class Solution:\n def echo(self, value)\n  return value\n";
        };
    var result =
        evaluate(wrapper(language, source), List.of(new Testcase("7\n", "7\n", true, RUN)));
    assertThat(result.verdict()).isEqualTo(Verdict.COMPILE_ERROR);
    assertThat(result.compileMessage())
        .contains("Dòng ")
        .doesNotContain("/w/", "ApH", "return broken", "Traceback");
    assertThat(result.results()).isEmpty();
  }

  @ParameterizedTest
  @EnumSource(Language.class)
  void malformedInput90IsSystemSpecFailure(Language language) {
    var result =
        evaluate(
            wrapper(language, echo(language)), List.of(new Testcase("bad\n", "7\n", true, RUN)));
    assertThat(result.verdict()).isEqualTo(Verdict.SYSTEM_ERROR);
    assertThat(result.failureReason()).isEqualTo("SPEC_UNAVAILABLE");
    assertThat(result.toString()).doesNotContain("APH-HARNESS", "bad", "Traceback");
  }

  @ParameterizedTest
  @EnumSource(Language.class)
  void runtimeResultBudget91BecomesRe(Language language) {
    var tiny = new Limits(3000, 1073741824, 1, 60);
    var exact = new Limits(3000, 1073741824, 2, 60);
    var result =
        evaluate(
            wrapper(language, echo(language)),
            List.of(
                new Testcase("7\n", "7\n", true, exact), new Testcase("7\n", "7\n", true, tiny)));
    assertThat(result.verdict()).isEqualTo(Verdict.RE);
    assertThat(result.results())
        .extracting(row -> row.verdict())
        .containsExactly(Verdict.AC, Verdict.RE);
    assertThat(result.toString()).doesNotContain("APH-HARNESS", "Traceback");
  }

  @Test
  void pythonGuard92IsCompileErrorButBodyAndConstructorRemainRe() {
    var guard =
        evaluate(
            wrapper(Language.PYTHON, "class Solution:\n def other(self): return 7\n"),
            List.of(new Testcase("7\n", "7\n", true, RUN)));
    assertThat(guard.verdict()).isEqualTo(Verdict.COMPILE_ERROR);
    assertThat(guard.compileMessage()).contains("echo").doesNotContain("Traceback", "APH-HARNESS");
    for (String source :
        List.of(
            "class Solution:\n def echo(self, value): raise TypeError('SECRET')\n",
            "class Solution:\n def __init__(self): raise TypeError('SECRET')\n def echo(self, value): return value\n")) {
      var body =
          evaluate(
              wrapper(Language.PYTHON, source), List.of(new Testcase("7\n", "7\n", true, RUN)));
      assertThat(body.verdict()).isEqualTo(Verdict.RE);
      assertThat(body.toString()).doesNotContain("SECRET", "Traceback");
    }
  }

  @Test
  void realTimeoutAndMemoryStatusesPrecedeMatching() {
    var timeout =
        evaluate(
            packaging.standardIo(Language.CPP, "int main(){while(true){}}"),
            List.of(new Testcase("", "", true, new Limits(100, 268435456, 1024, 60))));
    assertThat(timeout.verdict()).isEqualTo(Verdict.TLE);
    var memory =
        evaluate(
            packaging.standardIo(
                Language.CPP,
                "#include <vector>\nint main(){std::vector<char*> p;while(true){auto q=new char[1048576];for(int i=0;i<1048576;i++)q[i]=1;p.push_back(q);}}"),
            List.of(new Testcase("", "", true, new Limits(3000, 67108864, 1024, 60))));
    assertThat(memory.verdict()).isEqualTo(Verdict.MLE);
  }

  @Test
  void standardIoSampleStderrAndHiddenPolicy() {
    var result =
        evaluate(
            packaging.standardIo(
                Language.PYTHON, "import sys\nprint('PRIVATE',file=sys.stderr)\nsys.exit(90)\n"),
            List.of(new Testcase("", "", true, RUN), new Testcase("", "", false, RUN)));
    assertThat(result.verdict()).isEqualTo(Verdict.RE);
    assertThat(result.results().getFirst().stderrSnippet()).isEqualTo("PRIVATE\n");
    assertThat(result.results().get(1).stderrSnippet()).isNull();
  }

  @Test
  void scopedCacheReusesArtifactAcrossRunsAndRejectsClosedSession() {
    var program = bridge.program(wrapper(Language.JAVA, echo(Language.JAVA)));
    var execution = engine.compile(new CompileRequest(program, COMPILE));
    try {
      assertThat(execution.compilation().success()).isTrue();
      assertThat(execution.run(new RunRequest("1\n", RUN)).status()).isEqualTo(Status.OK);
      assertThat(execution.run(new RunRequest("2\n", RUN)).stdout()).endsWith("2\n");
    } finally {
      execution.close();
    }
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> execution.run(new RunRequest("1\n", RUN)))
        .isInstanceOf(IllegalStateException.class);
    execution.close();
  }

  @Test
  void deletedArtifactIsRecompiledOnceOnTheSameEngine() throws Exception {
    // The go-judge file store is shared with other sessions, so its total size proves nothing.
    // The execution tracks the artifact ids it stored; only those are inspected or deleted.
    var program = bridge.program(wrapper(Language.CPP, echo(Language.CPP)));
    try (var execution = engine.compile(new CompileRequest(program, COMPILE))) {
      var owned = ownedIds(execution);
      assertThat(owned).as("one compile stores exactly one artifact").hasSize(1);
      var lost = owned.iterator().next();
      assertThat(fileIds()).contains(lost);
      deleteFile(lost);
      assertThat(fileIds()).doesNotContain(lost);
      assertThat(execution.run(new RunRequest("3\n", RUN)).status()).isEqualTo(Status.OK);
      assertThat(execution.run(new RunRequest("4\n", RUN)).stdout()).endsWith("4\n");
      var after = new java.util.ArrayList<>(ownedIds(execution));
      assertThat(after).as("the lost artifact is recompiled once").hasSize(2).startsWith(lost);
      assertThat(fileIds()).contains(after.get(1)).doesNotContain(lost);
    }
  }

  @SuppressWarnings("unchecked")
  private static java.util.Set<String> ownedIds(Object execution) throws Exception {
    var field = execution.getClass().getDeclaredField("ownedIds");
    field.setAccessible(true);
    return new java.util.LinkedHashSet<>((java.util.Set<String>) field.get(execution));
  }

  private static java.util.Set<String> fileIds() throws Exception {
    var base = System.getProperty("judge.go-judge.base-url");
    var response =
        java.net.http.HttpClient.newHttpClient()
            .send(
                java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/file"))
                    .GET()
                    .build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
    var ids = new java.util.TreeSet<String>();
    tools.jackson.databind.json.JsonMapper.builder()
        .build()
        .readTree(response.body())
        .propertyNames()
        .forEach(ids::add);
    return ids;
  }

  private static void deleteFile(String id) throws Exception {
    var base = System.getProperty("judge.go-judge.base-url");
    java.net.http.HttpClient.newHttpClient()
        .send(
            java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/file/" + id))
                .DELETE()
                .build(),
            java.net.http.HttpResponse.BodyHandlers.discarding());
  }

  @ParameterizedTest
  @ValueSource(strings = {"90", "91", "92"})
  void standardIoCannotTurnExitNumbersIntoHarnessSignals(String exit) {
    var result =
        evaluate(
            packaging.standardIo(Language.PYTHON, "import sys\nsys.exit(" + exit + ")\n"),
            List.of(new Testcase("", "", true, RUN)));
    assertThat(result.verdict()).isEqualTo(Verdict.RE);
  }

  @Test
  void compilerSignatureAndNoMainUseFixedMessagesAndInternalErrorsStaySystemErrors() {
    var signature =
        evaluate(
            wrapper(
                Language.JAVA, "class Solution { public int other(int value) { return value; } }"),
            List.of(new Testcase("7\n", "7\n", true, RUN)));
    assertThat(signature.verdict()).isEqualTo(Verdict.COMPILE_ERROR);
    assertThat(signature.compileMessage()).contains("echo").doesNotContain("ApHRunner", "/w/");
    var noMain =
        evaluate(
            packaging.standardIo(Language.CPP, "int helper(){return 1;}"),
            List.of(new Testcase("", "", true, RUN)));
    assertThat(noMain.verdict()).isEqualTo(Verdict.COMPILE_ERROR);
    assertThat(noMain.compileMessage()).isEqualTo("Chương trình chưa có hàm `main`.");
    var internal =
        evaluate(
            wrapper(Language.JAVA, "class Solution { public int echo(int value) { return value; }"),
            List.of(new Testcase("7\n", "7\n", true, RUN)));
    assertThat(internal.verdict()).isEqualTo(Verdict.SYSTEM_ERROR);
    assertThat(internal.failureReason()).isEqualTo("SANDBOX_FAULT");
    assertThat(internal.compileMessage()).isNull();
  }

  @Test
  void invalidTypedExpectedOutputIsSystemSpecFailure() {
    var program = wrapper(Language.PYTHON, echo(Language.PYTHON));
    var result =
        evaluator.evaluate(
            new EvaluationRequest(
                bridge.program(program),
                COMPILE,
                new Matching(MatchingStrategy.UNORDERED_SET, null, INT),
                List.of(new Testcase("7\n", "invalid integer", true, RUN)),
                true));
    assertThat(result.verdict()).isEqualTo(Verdict.SYSTEM_ERROR);
    assertThat(result.failureReason()).isEqualTo("SPEC_UNAVAILABLE");
  }

  @Test
  void debugFloodHitsSeparateFiniteEngineCap() {
    var result =
        evaluate(
            wrapper(
                Language.PYTHON,
                "class Solution:\n def echo(self, value):\n  print('x'*10000)\n  return value\n"),
            List.of(new Testcase("7\n", "7\n", false, new Limits(3000, 1073741824, 2, 60))));
    assertThat(result.verdict()).isEqualTo(Verdict.RE);
    assertThat(result.results().getFirst().actualOutputSnippet()).isNull();
    assertThat(result.results().getFirst().userConsole()).isNull();
  }

  @ParameterizedTest
  @EnumSource(Language.class)
  void strictDebugBudgetRejects4097BytesWithSmallPayloadAndLargeResultLimit(Language language) {
    String source =
        switch (language) {
          case JAVA ->
              "class Solution { public int echo(int value) { System.out.print(\"x\".repeat(value)); return 7; } }";
          case CPP ->
              "class Solution { public: int echo(int value) { std::cout << std::string(value, 'x'); return 7; } };";
          case PYTHON ->
              "class Solution:\n def echo(self, value):\n  print('x'*value,end='')\n  return 7\n";
        };
    assertDebugPair(wrapper(language, source));
  }

  private void assertDebugPair(PackagedProgram program) {
    var large = new Limits(3000, 1073741824, 65536, 60);
    var result =
        evaluate(
            program,
            List.of(
                new Testcase("4096\n", "7\n", true, large),
                new Testcase("4097\n", "7\n", true, large),
                new Testcase("4097\n", "7\n", false, large)));
    assertThat(result.results())
        .extracting(row -> row.verdict())
        .containsExactly(Verdict.AC, Verdict.RE, Verdict.RE);
    assertThat(result.results().getFirst().actualOutputSnippet()).isEqualTo("7");
    assertThat(result.results().get(1).actualOutputSnippet()).isNull();
    assertThat(result.results().get(1).userConsole()).isNull();
    assertThat(result.results().get(2).userConsole()).isNull();
    assertThat(result.results()).allSatisfy(row -> assertThat(row.stderrSnippet()).isNull());
  }

  @Test
  void multibyteUtf8DebugUsesBytesAndPreservesP4Console() {
    var program =
        wrapper(
            Language.PYTHON,
            "class Solution:\n def echo(self, value):\n  print('é'*(value//2)+('x' if value%2 else ''),end='')\n  return 7\n");
    assertDebugPair(program);
    var result =
        evaluate(
            program,
            List.of(new Testcase("4096\n", "7\n", true, new Limits(3000, 1073741824, 65536, 60))));
    assertThat(result.results().getFirst().userConsole()).isEqualTo("é".repeat(2048) + "\n");
  }

  @Test
  void rawCrLfDebugDoesNotBorrowBytesFromP4Normalization() {
    var program =
        wrapper(
            Language.PYTHON,
            "class Solution:\n def echo(self, value):\n  print('\\r\\n'*(value//2)+('x' if value%2 else ''),end='')\n  return 7\n");
    assertDebugPair(program);
    var result =
        evaluate(
            program,
            List.of(new Testcase("4096\n", "7\n", true, new Limits(3000, 1073741824, 65536, 60))));
    assertThat(result.results().getFirst().userConsole()).isEqualTo("\n".repeat(2049));
  }

  @Test
  void missingWrapperMarkerRetainsWaAndStandardIoHasNoDebugBudget() {
    var large = new Limits(3000, 1073741824, 65536, 60);
    var missing =
        evaluate(
            wrapper(
                Language.PYTHON,
                "class Solution:\n def echo(self, value):\n  print('x'*4097,end='')\n  raise SystemExit(0)\n"),
            List.of(new Testcase("7\n", "7\n", true, large)));
    assertThat(missing.verdict()).isEqualTo(Verdict.WA);
    var standard =
        evaluate(
            packaging.standardIo(Language.PYTHON, "print('x'*4097+'7')\n"),
            List.of(new Testcase("", "x".repeat(4097) + "7\n", true, large)));
    assertThat(standard.verdict()).isEqualTo(Verdict.AC);
  }
}
