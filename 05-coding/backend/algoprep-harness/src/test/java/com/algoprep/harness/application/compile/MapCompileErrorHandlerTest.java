package com.algoprep.harness.application.compile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.functionspec.Language;
import com.algoprep.harness.application.dto.CompileErrorReport;
import com.algoprep.harness.application.dto.MapCompileErrorCommand;
import com.algoprep.harness.application.dto.SubmissionModel;
import com.algoprep.harness.application.ports.in.MapCompileErrorUseCase;
import com.algoprep.harness.application.ports.in.MatchOutputUseCase;
import com.algoprep.harness.domain.model.CompileDiagnostic;
import com.algoprep.harness.domain.model.CompileReportStatus;
import com.algoprep.harness.domain.model.LineMap;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class MapCompileErrorHandlerTest {
  private final List<LogEntry> logs = new ArrayList<>();
  private final MapCompileErrorHandler mapper =
      new MapCompileErrorHandler(
          (id, raw, truncated) -> logs.add(new LogEntry(id, raw, truncated)));

  @ParameterizedTest
  @MethodSource("userLocations")
  void mapsFirstNestedAndLastUserLinesForEveryCompiler(Language language, int userLine) {
    String output = header(language, userLine + 2, "bad expression");
    CompileErrorReport report = mapper.map(wrapper(language, output));
    assertThat(report.status()).isEqualTo(CompileReportStatus.USER_ERRORS);
    assertThat(report.diagnostics())
        .containsExactly(
            new CompileDiagnostic(
                userLine,
                language == Language.CPP ? 13 : null,
                language == Language.PYTHON ? "SyntaxError: bad expression" : "bad expression"));
    assertThat(report.fixedMessageKey()).isNull();
    assertThat(report.correlationId()).isEqualTo("case-1");
    assertThat(logs).containsExactly(new LogEntry("case-1", output, false));
  }

  static Stream<Arguments> userLocations() {
    return Arrays.stream(Language.values())
        .flatMap(language -> Stream.of(1, 4, 7).map(line -> Arguments.of(language, line)));
  }

  @ParameterizedTest
  @MethodSource("harnessLocations")
  void hidesPrefixEpilogueAndUnrecognizedCompilerOutput(Language language, String output) {
    CompileErrorReport report = mapper.map(wrapper(language, output));
    assertInternal(report);
    assertThat(report.toString()).doesNotContain("SECRET", "/private", "ApHRunner", "version 99");
    assertThat(logs.getFirst().raw()).isEqualTo(output);
  }

  static Stream<Arguments> harnessLocations() {
    return Arrays.stream(Language.values())
        .flatMap(
            language ->
                Stream.of(
                    Arguments.of(language, header(language, 1, "SECRET ApHRunner /private/file")),
                    Arguments.of(language, header(language, 14, "SECRET internal failure")),
                    Arguments.of(language, "compiler panic SECRET /private/ApHRunner version 99")));
  }

  @ParameterizedTest
  @MethodSource("languages")
  void callSiteUsesOnlyTheFixedSignatureMismatchKey(Language language) {
    CompileErrorReport report = mapper.map(wrapper(language, header(language, 11, "wrong method")));
    assertThat(report.status()).isEqualTo(CompileReportStatus.USER_ERRORS);
    assertThat(report.diagnostics()).isEmpty();
    assertThat(report.fixedMessageKey()).isEqualTo("harness.compile.signatureMismatch");
    assertThat(report.toString()).doesNotContain("wrong method");
  }

  static Stream<Language> languages() {
    return Stream.of(Language.values());
  }

  @Test
  void userErrorsWinAndAreDeduplicatedSortedAndLimitedAfterMapping() {
    LineMap map = new LineMap(2, 12, 16, 17, "Solution.java");
    String raw =
        IntStream.iterate(14, line -> line >= 3, line -> line - 1)
            .mapToObj(line -> header(Language.JAVA, line, "error-" + (line - 2)))
            .reduce("", (left, right) -> left + right + "\n");
    raw +=
        header(Language.JAVA, 3, "error-1")
            + "\n"
            + header(Language.JAVA, 1, "HARNESS")
            + "\n"
            + header(Language.JAVA, 16, "CALL_SITE");
    CompileErrorReport report =
        mapper.map(command(Language.JAVA, SubmissionModel.FUNCTION_WRAPPER, raw, map));
    assertThat(report.status()).isEqualTo(CompileReportStatus.USER_ERRORS);
    assertThat(report.diagnostics()).hasSize(10);
    assertThat(report.diagnostics().stream().map(CompileDiagnostic::line))
        .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    assertThat(report.toString()).doesNotContain("HARNESS", "CALL_SITE", "error-11", "error-12");
  }

  @Test
  void cppNotesRequireAUserErrorParentAndKeepTheirOwnUserLocations() {
    String raw =
        "main.cpp:6:9: error: invalid conversion\n"
            + "main.cpp:5:5: note: candidate\n"
            + "main.cpp:14:2: note: ApHRunner\nrequired from /private/main.cpp:14\n";
    CompileErrorReport report = mapper.map(wrapper(Language.CPP, raw));
    assertThat(report.diagnostics())
        .containsExactly(
            new CompileDiagnostic(3, 5, "note: candidate"),
            new CompileDiagnostic(4, 9, "invalid conversion"));
    assertInternal(mapper.map(wrapper(Language.CPP, "main.cpp:5:5: note: independent")));
    assertInternal(
        mapper.map(
            wrapper(Language.CPP, "main.cpp:14:2: error: bad\nmain.cpp:5:5: note: candidate")));
  }

  @Test
  void warningsBreakNoteAssociationAndSameLineOrderIsStable() {
    String raw =
        "main.cpp:6:9: error: first\nmain.cpp:6:2: note: second\n"
            + "main.cpp:5:1: warning: warning\nmain.cpp:5:2: note: independent\n";
    CompileErrorReport report = mapper.map(wrapper(Language.CPP, raw));
    assertThat(report.diagnostics())
        .containsExactly(
            new CompileDiagnostic(4, 9, "first"), new CompileDiagnostic(4, 2, "note: second"));
  }

  @Test
  void sanitizesPathsFilesAndEmbeddedLineReferencesWithoutChangingOrdinaryWords() {
    String message =
        "graph from /private/tmp/build.cc and C:\\private\\Main.java; "
            + "declared at line 6 and line 40 in Helper.java:40";
    CompileErrorReport report =
        mapper.map(wrapper(Language.JAVA, header(Language.JAVA, 6, message)));
    String cleaned = report.diagnostics().getFirst().message();
    assertThat(cleaned).contains("graph", "<path>", "line 4", "<location>", "<file>");
    assertThat(cleaned).doesNotContain("private", "Main.java", "Helper.java", "line 40", "line 6");
  }

  @ParameterizedTest
  @MethodSource("sensitiveMessages")
  void discardsUserMessagesThatRevealHarnessIdentifiersOrCompilerVersion(String message) {
    assertInternal(mapper.map(wrapper(Language.JAVA, header(Language.JAVA, 6, message))));
  }

  static Stream<String> sensitiveMessages() {
    return Stream.of(
        "unknown apH_p0",
        "ApHRunner",
        "@@APH-RESULT@@",
        "javac compiler version 25",
        "javac 25.0.2",
        "g++ 11.4.0",
        "Python 3.12.10");
  }

  @Test
  void sourceAndCaretLinesAreNeverAppendedToMessages() {
    String raw =
        "Solution.java:6: error: cannot find symbol\nSECRET_SOURCE ApHRunner\n    ^\n1 error\n";
    CompileErrorReport report = mapper.map(wrapper(Language.JAVA, raw));
    assertThat(report.diagnostics().getFirst().message()).isEqualTo("cannot find symbol");
    assertThat(report.toString()).doesNotContain("SECRET_SOURCE", "ApHRunner", "^");
  }

  @Test
  void foreignFilesAndMalformedLocationsFailClosed() {
    for (String raw :
        List.of(
            "Other.java:6: error: secret",
            "Solution.java:0: error: secret",
            "Solution.java:999999999999999: error: secret",
            "Solution.java:x: error: secret")) {
      assertInternal(mapper.map(wrapper(Language.JAVA, raw)));
    }
    assertInternal(mapper.map(wrapper(Language.CPP, "main.cpp:6: error: missing column")));
    assertInternal(mapper.map(wrapper(Language.PYTHON, "SyntaxError: missing file header")));
  }

  @ParameterizedTest
  @MethodSource("languages")
  void standardIoClampsEofAndEmbeddedLocationsToTheLastUserLine(Language language) {
    LineMap map = new LineMap(0, 7, null, null, file(language));
    CompileErrorReport report =
        mapper.map(
            command(
                language,
                SubmissionModel.STANDARD_IO,
                header(language, 8, "bad expression declared at line 9"),
                map));
    assertThat(report.status()).isEqualTo(CompileReportStatus.USER_ERRORS);
    assertThat(report.diagnostics().getFirst().line()).isEqualTo(7);
    assertThat(report.diagnostics().getFirst().message()).contains("line 7");
  }

  @Test
  void onlyCppStandardIoUsesTheNoMainLinkerPattern() {
    String raw =
        "/usr/bin/ld: undefined reference to 'main'\ncollect2: error: ld returned 1 exit status";
    CompileErrorReport report =
        mapper.map(
            command(
                Language.CPP,
                SubmissionModel.STANDARD_IO,
                raw,
                new LineMap(0, 7, null, null, "main.cpp")));
    assertThat(report.status()).isEqualTo(CompileReportStatus.USER_ERRORS);
    assertThat(report.fixedMessageKey()).isEqualTo("harness.compile.noMain");
    assertThat(report.diagnostics()).isEmpty();
    assertInternal(mapper.map(wrapper(Language.CPP, raw)));
    assertInternal(
        mapper.map(
            command(
                Language.JAVA,
                SubmissionModel.STANDARD_IO,
                raw,
                new LineMap(0, 7, null, null, "Main.java"))));
  }

  @ParameterizedTest
  @MethodSource("invalidMaps")
  void invalidLineMapsAre040AndNeverReachTheLog(LineMap map) {
    MapCompileErrorCommand command =
        command(Language.JAVA, SubmissionModel.FUNCTION_WRAPPER, "SECRET", map);
    assertThatThrownBy(() -> mapper.map(command))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, error -> assertThat(error.errorCode()).isEqualTo("HRN-040"));
    assertThat(logs).isEmpty();
  }

  static Stream<LineMap> invalidMaps() {
    return Stream.of(
        null,
        new LineMap(-1, 7, null, null, "Solution.java"),
        new LineMap(2, 0, null, null, "Solution.java"),
        new LineMap(Integer.MAX_VALUE, 1, null, null, "Solution.java"),
        new LineMap(2, 7, 12, 11, "Solution.java"),
        new LineMap(2, 7, 11, null, "Solution.java"),
        new LineMap(2, 7, null, 12, "Solution.java"),
        new LineMap(2, 7, 1, 11, "Solution.java"),
        new LineMap(2, 7, 6, 7, "Solution.java"),
        new LineMap(2, 7, null, null, ""),
        new LineMap(2, 7, null, null, null),
        new LineMap(2, 7, null, null, "/private/Solution.java"));
  }

  @Test
  void standardIoRejectsPrefixAndCallSiteAndReportsInvalidCommandsWithoutRawCauses() {
    for (LineMap map :
        List.of(new LineMap(1, 7, null, null, "Main.java"), new LineMap(0, 7, 8, 9, "Main.java"))) {
      assertThatThrownBy(
              () -> mapper.map(command(Language.JAVA, SubmissionModel.STANDARD_IO, "x", map)))
          .isInstanceOfSatisfying(
              AlgoPrepException.class, error -> assertThat(error.errorCode()).isEqualTo("HRN-040"));
    }
    assertThatThrownBy(() -> mapper.map(null))
        .isInstanceOfSatisfying(
            AlgoPrepException.class,
            error -> {
              assertThat(error.errorCode()).isEqualTo("HRN-031");
              assertThat(error.getCause()).isNull();
            });
  }

  @Test
  void boundsParsingAndLoggingAt65536CharactersAndKeepsTheOriginalLineEndingsInTheLog() {
    String raw = "x".repeat(65536) + "\nSolution.java:6: error: ignored-tail";
    assertInternal(mapper.map(wrapper(Language.JAVA, raw)));
    assertThat(logs.getFirst().raw()).hasSize(65536);
    assertThat(logs.getFirst().truncated()).isTrue();
    logs.clear();
    mapper.map(wrapper(Language.JAVA, "x".repeat(65536)));
    assertThat(logs.getFirst().truncated()).isFalse();
    logs.clear();
    raw = "Solution.java:6: error: bad\r\nsource\r^\r";
    assertThat(mapper.map(wrapper(Language.JAVA, raw)).diagnostics()).hasSize(1);
    assertThat(logs.getFirst().raw()).isEqualTo(raw);
  }

  @Test
  void loggingFailureCannotReplaceOrLeakIntoTheLearnerReport() {
    MapCompileErrorUseCase unavailable =
        new MapCompileErrorHandler(
            (id, output, truncated) -> {
              throw new IllegalStateException("SECRET LOG FAILURE " + output);
            });
    CompileErrorReport report = unavailable.map(wrapper(Language.JAVA, "compiler panic SECRET"));
    assertInternal(report);
    assertThat(report.toString()).doesNotContain("SECRET", "FAILURE");
  }

  @Test
  void reportCollectionsAreImmutableAndCorrelationIdMayBeAbsent() {
    MapCompileErrorCommand command = wrapper(Language.JAVA, "Solution.java:6: error: bad");
    CompileErrorReport report =
        mapper.map(
            new MapCompileErrorCommand(
                command.language(),
                command.model(),
                command.rawCompilerOutput(),
                command.lineMap(),
                null,
                null));
    assertThat(report.correlationId()).isNull();
    assertThatThrownBy(() -> report.diagnostics().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void springDiscoversBothP3AndP4WithTheRealInternalLogAdapter() {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.scan("com.algoprep.harness");
      context.refresh();
      assertThat(context.getBean(MapCompileErrorUseCase.class)).isNotNull();
      assertThat(context.getBean(MatchOutputUseCase.class)).isNotNull();
    }
  }

  @Test
  void realJavacAndGppFixturesMapTheirThreeUserErrors() throws IOException {
    for (Language language : List.of(Language.JAVA, Language.CPP)) {
      String folder = language == Language.JAVA ? "java" : "cpp";
      CompileErrorReport report =
          mapper.map(wrapper(language, fixture(folder + "/three-user-errors.stderr.txt")));
      assertThat(report.status()).isEqualTo(CompileReportStatus.USER_ERRORS);
      assertThat(report.diagnostics().stream().map(CompileDiagnostic::line))
          .containsExactly(1, 4, 7);
    }
  }

  @ParameterizedTest
  @MethodSource("pythonFixtures")
  void realPythonSyntaxFixturesMapUserLines(String name, int line) throws IOException {
    CompileErrorReport report =
        mapper.map(wrapper(Language.PYTHON, fixture("python/" + name + ".stderr.txt")));
    assertThat(report.status()).isEqualTo(CompileReportStatus.USER_ERRORS);
    assertThat(report.diagnostics()).hasSize(1);
    assertThat(report.diagnostics().getFirst().line()).isEqualTo(line);
  }

  static Stream<Arguments> pythonFixtures() {
    return Stream.of(Arguments.of("first", 1), Arguments.of("nested", 4), Arguments.of("last", 7));
  }

  private static String fixture(String name) throws IOException {
    try (var input =
        MapCompileErrorHandlerTest.class.getResourceAsStream("/compiler-output/" + name)) {
      assertThat(input).isNotNull();
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static MapCompileErrorCommand wrapper(Language language, String output) {
    return command(
        language,
        SubmissionModel.FUNCTION_WRAPPER,
        output,
        new LineMap(2, 7, 11, 12, file(language)));
  }

  private static MapCompileErrorCommand command(
      Language language, SubmissionModel model, String output, LineMap map) {
    return new MapCompileErrorCommand(language, model, output, map, "solve()", "case-1");
  }

  private static String file(Language language) {
    return switch (language) {
      case JAVA -> "Solution.java";
      case CPP -> "main.cpp";
      case PYTHON -> "main.py";
    };
  }

  private static String header(Language language, int line, String message) {
    return switch (language) {
      case JAVA -> "Solution.java:" + line + ": error: " + message;
      case CPP -> "main.cpp:" + line + ":13: error: " + message;
      case PYTHON ->
          "  File \"main.py\", line " + line + "\n    source\n    ^\nSyntaxError: " + message;
    };
  }

  private static void assertInternal(CompileErrorReport report) {
    assertThat(report.status()).isEqualTo(CompileReportStatus.HARNESS_INTERNAL);
    assertThat(report.diagnostics()).isEmpty();
    assertThat(report.fixedMessageKey()).isEqualTo("harness.compile.internal");
  }

  private record LogEntry(String id, String raw, boolean truncated) {}
}
