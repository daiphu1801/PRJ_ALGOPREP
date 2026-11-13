package com.algoprep.harness.application.prepare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.functionspec.Language;
import com.algoprep.harness.domain.model.LineMap;
import com.algoprep.harness.domain.model.WrapperSource;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ProgramSourceAssemblerTest {
  private final ProgramSourceAssembler assembler = new ProgramSourceAssembler();
  private static final WrapperSource FRAME =
      new WrapperSource("prefix1\nprefix2\n", "read1\nread2\n", "call1\ncall2\n", "print\n");

  @ParameterizedTest
  @EnumSource(Language.class)
  void sevenUserLinesAndMultilineCallHaveExactAbsoluteRegions(Language language) {
    String user = "u1\nu2\nu3\nu4\nu5\nu6\nu7\n";
    var result = assembler.functionWrapper(language, user, FRAME);
    assertThat(result.text())
        .isEqualTo("prefix1\nprefix2\n" + user + "read1\nread2\ncall1\ncall2\nprint\n");
    assertThat(result.lineMap()).isEqualTo(new LineMap(2, 7, 12, 13, result.sourceFileName()));
    assertThat(result.text().lines().toList().get(2)).isEqualTo("u1");
    assertThat(result.text().lines().toList().get(8)).isEqualTo("u7");
    assertThat(result.text().lines().toList().get(9)).isEqualTo("read1");
    assertThat(result.lineMap().isUserLine(9)).isTrue();
    assertThat(result.lineMap().isUserLine(10)).isFalse();
    assertThat(result.lineMap().isCallSite(12)).isTrue();
    assertThat(result.sourceFileName()).isEqualTo(wrapperFile(language));
  }

  @ParameterizedTest
  @EnumSource(Language.class)
  void userMarkersAndClosingBracesCannotChangeFrameworkOrRegions(Language language) {
    String hostile = "@@APH_USER_CODE@@\n{{FUNCTION_NAME}}\n}\n";
    var baseline = assembler.functionWrapper(language, "a\nb\nc\n", FRAME);
    var actual = assembler.functionWrapper(language, hostile, FRAME);
    assertThat(actual.lineMap()).isEqualTo(baseline.lineMap());
    assertThat(actual.text())
        .isEqualTo("prefix1\nprefix2\n" + hostile + "read1\nread2\ncall1\ncall2\nprint\n");
  }

  @ParameterizedTest
  @ValueSource(strings = {"a\r\nb\rc\r\n", "\uFEFFa\nb\nc", "a\nb\nc\n"})
  void normalizesBomAndNewlinesWithoutAddingAnExtraLine(String source) {
    var result = assembler.standardIo(Language.PYTHON, source);
    assertThat(result.text()).isEqualTo("a\nb\nc\n");
    assertThat(result.lineMap()).isEqualTo(new LineMap(0, 3, null, null, "main.py"));
  }

  @Test
  void preservesTabsBlankLinesTrailingSpacesAndInternalBom() {
    String source = "\uFEFF\n\tclass Solution:  \n\n\t\tpass\uFEFF\n\n";
    var result = assembler.functionWrapper(Language.PYTHON, source, FRAME);
    assertThat(result.text()).contains("\n\tclass Solution:  \n\n\t\tpass\uFEFF\n\n");
    assertThat(result.lineMap().userLineCount()).isEqualTo(5);
    assertThat(result.lineMap().callSiteStartLine()).isEqualTo(10);
  }

  @ParameterizedTest
  @EnumSource(Language.class)
  void standardIoAddsNoFrameworkOrSentinel(Language language) {
    var result = assembler.standardIo(language, "u1\nu2\nu3\nu4\nu5\nu6\nu7\nu8\nu9\nu10\n");
    assertThat(result.text()).isEqualTo("u1\nu2\nu3\nu4\nu5\nu6\nu7\nu8\nu9\nu10\n");
    String file = language == Language.JAVA ? "Main.java" : wrapperFile(language);
    assertThat(result.lineMap()).isEqualTo(new LineMap(0, 10, null, null, file));
    assertThat(result.sourceFileName()).isEqualTo(file);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" \t\r\n", "\uFEFF", "\uFEFF \n", "\u2003"})
  void blankSourceIsRejected(String source) {
    assertCode(source, "HRN-020");
  }

  @ParameterizedTest
  @ValueSource(strings = {"\0", "secret\0text"})
  void rejectsNulWithoutEchoingSource(String source) {
    assertCode(source, "HRN-022");
  }

  @ParameterizedTest
  @ValueSource(strings = {"x", "é", "😀"})
  void byteLimitUsesOriginalUtf8BytesAndAcceptsExactBoundary(String unit) {
    int size = unit.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
    String exact = unit.repeat(65536 / size);
    assertThat(assembler.standardIo(Language.JAVA, exact).text()).isEqualTo(exact + "\n");
    assertCode(exact + "x", "HRN-021");
    assertThat(assembler.functionWrapper(Language.JAVA, exact, FRAME).lineMap().userLineCount())
        .isEqualTo(1);
  }

  @Test
  void bomAndCrLfCountTowardOriginalByteLimit() {
    assertThat(assembler.standardIo(Language.JAVA, "\uFEFF" + "x".repeat(65533)).text())
        .isEqualTo("x".repeat(65533) + "\n");
    assertCode("\uFEFF" + "x".repeat(65534), "HRN-021");
    assertCode("x\r\n".repeat(21846), "HRN-021");
  }

  @ParameterizedTest
  @MethodSource("invalidFrameworks")
  void rejectsBrokenInternalFragmentsWithoutLeakingThem(WrapperSource frame) {
    assertThatThrownBy(() -> assembler.functionWrapper(Language.CPP, "valid", frame))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, error -> assertThat(error.errorCode()).isEqualTo("HRN-030"))
        .hasMessage("Invalid rendered framework fragments");
  }

  static Stream<Arguments> invalidFrameworks() {
    return Stream.of(
        Arguments.of((Object) null),
        Arguments.of(new WrapperSource(null, "", "call\n", "")),
        Arguments.of(new WrapperSource("", null, "call\n", "")),
        Arguments.of(new WrapperSource("", "", null, "")),
        Arguments.of(new WrapperSource("", "", "call\n", null)),
        Arguments.of(new WrapperSource("secret", "", "call\n", "")),
        Arguments.of(new WrapperSource("", "secret", "call\n", "")),
        Arguments.of(new WrapperSource("", "", "secret", "")),
        Arguments.of(new WrapperSource("", "", "call\n", "secret")),
        Arguments.of(new WrapperSource("secret\r\n", "", "call\n", "")),
        Arguments.of(new WrapperSource("", "", "\0\n", "")),
        Arguments.of(new WrapperSource("", "", "", "")),
        Arguments.of(new WrapperSource("", "", " \n", "")));
  }

  @Test
  void allowsEmptySurroundingsAndSingleLineCall() {
    var result =
        assembler.functionWrapper(Language.CPP, "user", new WrapperSource("", "", "call\n", ""));
    assertThat(result.text()).isEqualTo("user\ncall\n");
    assertThat(result.lineMap()).isEqualTo(new LineMap(0, 1, 2, 2, "main.cpp"));
  }

  @Test
  void absentLanguageUsesDomainCode() {
    assertThatThrownBy(() -> assembler.standardIo(null, "source"))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, error -> assertThat(error.errorCode()).isEqualTo("HRN-010"));
  }

  @Test
  void sourceValidationPrecedesFrameworkAndLanguageValidation() {
    assertThatThrownBy(() -> assembler.functionWrapper(null, "bad\0", null))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, error -> assertThat(error.errorCode()).isEqualTo("HRN-022"));
  }

  @Test
  void assemblyIsDeterministicAcrossConcurrentCalls() throws Exception {
    var expected = assembler.functionWrapper(Language.JAVA, "class Solution {}", FRAME);
    try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
      Callable<Object> task =
          () -> assembler.functionWrapper(Language.JAVA, "class Solution {}", FRAME);
      for (var result : pool.invokeAll(List.of(task, task, task, task))) {
        assertThat(result.get()).isEqualTo(expected);
      }
    }
  }

  private void assertCode(String source, String code) {
    assertThatThrownBy(() -> assembler.standardIo(Language.JAVA, source))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, error -> assertThat(error.errorCode()).isEqualTo(code))
        .hasMessageNotContaining("secret");
  }

  private static String wrapperFile(Language language) {
    return switch (language) {
      case JAVA -> "Solution.java";
      case CPP -> "main.cpp";
      case PYTHON -> "main.py";
    };
  }
}
