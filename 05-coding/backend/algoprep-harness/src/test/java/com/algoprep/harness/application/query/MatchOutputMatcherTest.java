package com.algoprep.harness.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.functionspec.MatchingStrategy;
import com.algoprep.common.functionspec.TypeKind;
import com.algoprep.common.functionspec.TypeNode;
import com.algoprep.harness.application.dto.MatchOutputCommand;
import com.algoprep.harness.application.dto.MatchResult;
import com.algoprep.harness.application.dto.MatchSpec;
import com.algoprep.harness.application.dto.MatchVerdict;
import com.algoprep.harness.application.dto.SubmissionModel;
import com.algoprep.harness.application.ports.in.MatchOutputUseCase;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class MatchOutputMatcherTest {
  private static final String SENTINEL = "@@APH-RESULT@@";
  private static final BigDecimal EPSILON = new BigDecimal("0.001");
  private final MatchOutputUseCase matcher = new MatchOutputMatcher();

  @ParameterizedTest
  @MethodSource("textCases")
  void exactAndTrimmedFollowLineRules(
      MatchingStrategy strategy, String actual, String expected, boolean matches) {
    assertVerdict(matcher.match(standard(strategy, actual, expected)), matches);
  }

  static Stream<Arguments> textCases() {
    return Stream.of(
        Arguments.of(MatchingStrategy.EXACT, "alpha\r\n\r\n", "alpha\n", true),
        Arguments.of(MatchingStrategy.EXACT, "alpha\rbravo\n", "alpha\nbravo", true),
        Arguments.of(MatchingStrategy.EXACT, "alpha betb", "alpha beta", false),
        Arguments.of(MatchingStrategy.EXACT, "alpha  beta", "alpha beta", false),
        Arguments.of(MatchingStrategy.EXACT, "", "\n\n", true),
        Arguments.of(MatchingStrategy.EXACT, " alpha ", "alpha", false),
        Arguments.of(MatchingStrategy.TRIMMED, "  1   2  \n\t3\t4 ", "1 2\n3 4", true),
        Arguments.of(MatchingStrategy.TRIMMED, "1 2\n3 5", "1 2\n3 4", false),
        Arguments.of(MatchingStrategy.TRIMMED, "1\n \t\n2", "1\n\n2", true),
        Arguments.of(MatchingStrategy.TRIMMED, "1\n\n2", "1\n2", false));
  }

  @Test
  void trimmedPreservesListStringsAndDoesNotNormalizeTreeSpaces() {
    assertVerdict(
        typed(MatchingStrategy.TRIMMED, list(leaf(TypeKind.STRING)), "[\"a  b\"]", "[\"a b\"]"),
        false);
    assertVerdict(typed(MatchingStrategy.TRIMMED, tree(TypeKind.INT), "1  2", "1 2"), false);
    assertVerdict(typed(MatchingStrategy.EXACT, tree(TypeKind.INT), "1 2", "1 # 2"), false);
    assertVerdict(typed(MatchingStrategy.EXACT, tree(TypeKind.INT), "#", "#"), true);
  }

  @Test
  void trimmedPreservesStringArrayContentsAsConfirmedByOwner() {
    assertVerdict(
        typed(MatchingStrategy.TRIMMED, array(TypeKind.STRING, 1), "[\"a  b\"]", "[\"a b\"]"),
        false);
    assertVerdict(
        typed(MatchingStrategy.TRIMMED, array(TypeKind.STRING, 2), "[[\"a  b\"]]", "[[\"a  b\"]]"),
        true);
  }

  @Test
  void sentinelIsAWholeLineAndTheLastOccurrenceWins() {
    MatchResult result =
        matcher.match(
            wrapper(
                MatchingStrategy.EXACT,
                leaf(TypeKind.INT),
                SENTINEL + "\r\n999\r\n" + SENTINEL + "\r\n7\r\n",
                "7\n"));
    assertVerdict(result, true);
    assertThat(result.actualOutput()).isEqualTo("7");
    assertThat(result.userConsole()).isEqualTo(SENTINEL + "\n999\n");
    assertVerdict(
        matcher.match(
            wrapper(MatchingStrategy.EXACT, leaf(TypeKind.INT), "prefix" + SENTINEL + "\n7", "7")),
        false);
  }

  @Test
  void missingSentinelNeverMatchesEvenAnEmptyExpected() {
    MatchResult result =
        matcher.match(wrapper(MatchingStrategy.EXACT, leaf(TypeKind.STRING), "debug\n", ""));
    assertVerdict(result, false);
    assertThat(result.actualOutput()).isEmpty();
    assertThat(result.userConsole()).isEqualTo("debug\n");
    assertVerdict(
        matcher.match(wrapper(MatchingStrategy.EXACT, leaf(TypeKind.STRING), SENTINEL, "")), true);
    assertVerdict(matcher.match(standard(MatchingStrategy.EXACT, SENTINEL + "\n7", "7")), false);
  }

  @ParameterizedTest
  @MethodSource("epsilonCases")
  void epsilonIsInclusiveAndHandlesNonFiniteValues(String actual, String expected, boolean match) {
    assertVerdict(typed(MatchingStrategy.EPSILON, leaf(TypeKind.DOUBLE), actual, expected), match);
  }

  static Stream<Arguments> epsilonCases() {
    return Stream.of(
        Arguments.of("1.001000000000", "1.000000000000", true),
        Arguments.of("1.001100000000", "1.000000000000", false),
        Arguments.of("NaN", "NaN", true),
        Arguments.of("NaN", "1.0", false),
        Arguments.of("Infinity", "Infinity", true),
        Arguments.of("-Infinity", "Infinity", false),
        Arguments.of("-Infinity", "-Infinity", true),
        Arguments.of("0", "-0.000", true),
        Arguments.of("not-a-number", "1.0", false));
  }

  @Test
  void epsilonComparesNestedLeavesAndShapeSeparately() {
    TypeNode nested = list(list(leaf(TypeKind.DOUBLE)));
    assertVerdict(
        typed(MatchingStrategy.EPSILON, nested, "[[1.0005,2],[3.0009]]", "[[1,2],[3]]"), true);
    assertVerdict(typed(MatchingStrategy.EPSILON, nested, "[[1],[3]]", "[[1,2],[3]]"), false);
    assertVerdict(
        typed(MatchingStrategy.EPSILON, nested, "[[1.0005,2.0011],[3]]", "[[1,2],[3]]"), false);
    assertVerdict(
        typed(
            MatchingStrategy.EPSILON, array(TypeKind.DOUBLE, 2), "1.0005 2\n3 4.0009", "1 2\n3 4"),
        true);
    assertVerdict(
        typed(
            MatchingStrategy.EPSILON,
            array(TypeKind.DOUBLE, 3),
            "1 2\n3 4\n\n5 6\n7 8.0005",
            "1 2\n3 4\n\n5 6\n7 8"),
        true);
  }

  @Test
  void standardIoUsesLineAndTokenShapeUnlessATypeIsSupplied() {
    assertVerdict(
        matcher.match(standard(MatchingStrategy.EPSILON, "score 1.0005", "score 1.0")), true);
    assertVerdict(
        matcher.match(standard(MatchingStrategy.EPSILON, "scores 1.0005", "score 1.0")), false);
    assertVerdict(matcher.match(standard(MatchingStrategy.EPSILON, "score 1", "score 1 2")), false);
    assertVerdict(matcher.match(standard(MatchingStrategy.EPSILON, "score\n1", "score 1")), false);
    MatchSpec spec = new MatchSpec(MatchingStrategy.EPSILON, EPSILON, list(leaf(TypeKind.DOUBLE)));
    assertVerdict(
        matcher.match(
            new MatchOutputCommand(SubmissionModel.STANDARD_IO, spec, null, "[1.0005,2]", "[1,2]")),
        true);
  }

  @Test
  void unorderedIsAMultisetOfTopLevelElementsAndRows() {
    assertVerdict(
        typed(MatchingStrategy.UNORDERED_SET, list(leaf(TypeKind.INT)), "[2,1,1]", "[1,1,2]"),
        true);
    assertVerdict(
        typed(MatchingStrategy.UNORDERED_SET, list(leaf(TypeKind.INT)), "[1,2]", "[1,1,2]"), false);
    assertVerdict(matcher.match(standard(MatchingStrategy.UNORDERED_SET, "2 1\n1", "1 1 2")), true);
    assertVerdict(matcher.match(standard(MatchingStrategy.UNORDERED_SET, "1 2", "1 1 2")), false);
    assertVerdict(
        typed(MatchingStrategy.UNORDERED_SET, array(TypeKind.INT, 2), "3 4\n1 2", "1 2\n3 4"),
        true);
    assertVerdict(
        typed(MatchingStrategy.UNORDERED_SET, array(TypeKind.INT, 2), "2 1\n3 4", "1 2\n3 4"),
        false);
    assertVerdict(
        typed(
            MatchingStrategy.UNORDERED_SET,
            list(list(leaf(TypeKind.INT))),
            "[[3],[1,2]]",
            "[[1,2],[3]]"),
        true);
  }

  @Test
  void unorderedUsesPrintedDoubleValuesRatherThanEpsilon() {
    TypeNode type = list(leaf(TypeKind.DOUBLE));
    assertVerdict(typed(MatchingStrategy.UNORDERED_SET, type, "[1.0,2.00]", "[2,1]"), true);
    assertVerdict(typed(MatchingStrategy.UNORDERED_SET, type, "[1.0001]", "[1.0]"), false);
    assertVerdict(typed(MatchingStrategy.UNORDERED_SET, leaf(TypeKind.DOUBLE), "1.0", "1"), true);
    assertCode(
        wrapper(MatchingStrategy.UNORDERED_SET, type, SENTINEL + "\n[1]", "[1e1000000]"),
        "HRN-050");
    assertVerdict(typed(MatchingStrategy.UNORDERED_SET, type, "[1e1000000]", "[1]"), false);
    assertVerdict(typed(MatchingStrategy.UNORDERED_SET, type, "[1e-1000000]", "[0]"), true);
  }

  @ParameterizedTest
  @MethodSource("scalarCases")
  void typedEpsilonKeepsNonDoubleLeavesExact(
      TypeKind kind, String actual, String expected, boolean match) {
    assertVerdict(typed(MatchingStrategy.EPSILON, leaf(kind), actual, expected), match);
  }

  static Stream<Arguments> scalarCases() {
    return Stream.of(
        Arguments.of(TypeKind.INT, "7", "7", true),
        Arguments.of(TypeKind.INT, "8", "7", false),
        Arguments.of(TypeKind.LONG, "9223372036854775807", "9223372036854775807", true),
        Arguments.of(TypeKind.BOOLEAN, "true", "false", false),
        Arguments.of(TypeKind.BOOLEAN, "true", "true", true),
        Arguments.of(TypeKind.CHAR, "x", "x", true),
        Arguments.of(TypeKind.CHAR, " ", "x", false),
        Arguments.of(TypeKind.STRING, "a  b", "a b", false),
        Arguments.of(TypeKind.STRING, "1.0005", "1.0000", false));
  }

  @Test
  void bracketGrammarPreservesEscapesAndStringArrayDimensions() {
    TypeNode strings = array(TypeKind.STRING, 2);
    assertVerdict(
        typed(
            MatchingStrategy.EPSILON,
            strings,
            "[[\"a\\n\\\"b\\\\c\",\"d\"]]",
            "[[\"a\\n\\\"b\\\\c\",\"d\"]]"),
        true);
    assertVerdict(
        typed(MatchingStrategy.EPSILON, strings, "[[\"a\"],[\"b\"]]", "[[\"a\",\"b\"]]"), false);
    assertVerdict(
        typed(MatchingStrategy.EPSILON, list(leaf(TypeKind.CHAR)), "[\"x\"]", "[\"x\"]"), true);
    assertVerdict(
        typed(
            MatchingStrategy.UNORDERED_SET,
            list(leaf(TypeKind.STRING)),
            "[\"b\",\"a  b\"]",
            "[\"a  b\",\"b\"]"),
        true);
  }

  @ParameterizedTest
  @MethodSource("malformedCases")
  void malformedExpectedIsAnErrorWhileMalformedActualIsNoMatch(
      TypeNode type, String bad, String good) {
    assertCode(wrapper(MatchingStrategy.EPSILON, type, SENTINEL + "\n" + good, bad), "HRN-050");
    assertVerdict(typed(MatchingStrategy.EPSILON, type, bad, good), false);
  }

  static Stream<Arguments> malformedCases() {
    return Stream.of(
        Arguments.of(leaf(TypeKind.INT), "2147483648", "1"),
        Arguments.of(leaf(TypeKind.LONG), "9223372036854775808", "1"),
        Arguments.of(leaf(TypeKind.DOUBLE), "secret-invalid", "1"),
        Arguments.of(leaf(TypeKind.BOOLEAN), "True", "true"),
        Arguments.of(leaf(TypeKind.CHAR), "xy", "x"),
        Arguments.of(list(leaf(TypeKind.INT)), "[1,2", "[1,2]"),
        Arguments.of(list(leaf(TypeKind.INT)), "[1,]", "[]"),
        Arguments.of(list(leaf(TypeKind.STRING)), "[\"x\\q\"]", "[]"),
        Arguments.of(list(leaf(TypeKind.STRING)), "[\"x]", "[]"),
        Arguments.of(array(TypeKind.INT, 2), "1 2\n3", "1 2\n3 4"),
        Arguments.of(array(TypeKind.STRING, 2), "[[\"a\"],[\"b\",\"c\"]]", "[]"),
        Arguments.of(tree(TypeKind.INT), "# 1", "#"),
        Arguments.of(tree(TypeKind.INT), "1 #", "1"),
        Arguments.of(new TypeNode(TypeKind.LINKED_LIST, leaf(TypeKind.INT), null), "2\n1", "1\n1"));
  }

  @Test
  void emptyContainersAndLinkedTreeShapesAreValid() {
    assertVerdict(typed(MatchingStrategy.EPSILON, list(leaf(TypeKind.INT)), "[]", "[]"), true);
    assertVerdict(typed(MatchingStrategy.EPSILON, array(TypeKind.INT, 1), "", ""), true);
    TypeNode linked = new TypeNode(TypeKind.LINKED_LIST, leaf(TypeKind.DOUBLE), null);
    assertVerdict(typed(MatchingStrategy.EPSILON, linked, "0", "0\n"), true);
    assertVerdict(typed(MatchingStrategy.EPSILON, linked, "2\n1.0005 2", "2\n1 2"), true);
    assertVerdict(
        typed(MatchingStrategy.EPSILON, tree(TypeKind.DOUBLE), "1 # 2.0005", "1 # 2"), true);
    assertVerdict(typed(MatchingStrategy.EPSILON, tree(TypeKind.INT), "1 2", "1 # 2"), false);
  }

  @Test
  void invalidConfigurationIs051BeforeMatching() {
    assertCode(
        new MatchOutputCommand(
            SubmissionModel.STANDARD_IO,
            new MatchSpec(MatchingStrategy.EPSILON, BigDecimal.ZERO, null),
            null,
            "1",
            "1"),
        "HRN-051");
    assertCode(
        wrapper(MatchingStrategy.UNORDERED_SET, tree(TypeKind.INT), SENTINEL + "\n1", "1"),
        "HRN-051");
    assertCode(wrapper(MatchingStrategy.EXACT, null, SENTINEL + "\n1", "1"), "HRN-051");
    assertCode(
        wrapper(MatchingStrategy.EXACT, new TypeNode(TypeKind.OTHER, null, null), "", ""),
        "HRN-051");
    assertCode(
        new MatchOutputCommand(
            SubmissionModel.FUNCTION_WRAPPER,
            new MatchSpec(MatchingStrategy.EXACT, null, leaf(TypeKind.INT)),
            "bad\nmarker",
            "1",
            "1"),
        "HRN-051");
    assertCode(new MatchOutputCommand(null, null, null, null, null), "HRN-051");
    assertCode(null, "HRN-051");
  }

  @Test
  void invalidExpectedStillRaises050WhenSentinelIsMissing() {
    assertCode(
        wrapper(MatchingStrategy.EPSILON, leaf(TypeKind.DOUBLE), "debug", "SECRET_BAD"), "HRN-050");
    assertCode(
        wrapper(
            MatchingStrategy.UNORDERED_SET, list(leaf(TypeKind.INT)), SENTINEL + "\n[]", "[1,2"),
        "HRN-050");
  }

  @Test
  void resultsAndErrorsDoNotExposeExpectedDataOrParserCauses() {
    MatchResult result =
        matcher.match(standard(MatchingStrategy.EXACT, "wrong", "SECRET_EXPECTED"));
    assertThat(result.toString()).doesNotContain("SECRET_EXPECTED", "diff", "expectedOutput");
    assertThat(Arrays.stream(MatchResult.class.getRecordComponents()).map(c -> c.getName()))
        .containsExactly("verdict", "actualOutput", "userConsole");
    assertThatThrownBy(() -> typed(MatchingStrategy.EPSILON, leaf(TypeKind.INT), "1", "SECRET_BAD"))
        .isInstanceOfSatisfying(
            AlgoPrepException.class,
            error -> {
              assertThat(error.getMessage()).doesNotContain("SECRET_BAD");
              assertThat(error.getCause()).isNull();
            });
  }

  @Test
  void springCanDiscoverTheInboundPortWithoutOtherModules() {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.scan("com.algoprep.harness.application.query");
      context.refresh();
      assertVerdict(
          context
              .getBean(MatchOutputUseCase.class)
              .match(standard(MatchingStrategy.EXACT, "1", "1")),
          true);
    }
  }

  @Test
  void numericScanningAndSubtractionHandleAdversarialTokens() {
    String invalid = "1".repeat(20000) + "x";
    assertVerdict(matcher.match(standard(MatchingStrategy.EPSILON, invalid, "1")), false);
    assertVerdict(matcher.match(standard(MatchingStrategy.EPSILON, "1e1000000", "1")), false);
    assertVerdict(matcher.match(standard(MatchingStrategy.EPSILON, "1e-1000000", "0")), true);
    MatchSpec hugeEpsilon =
        new MatchSpec(MatchingStrategy.EPSILON, new BigDecimal("1e1000000"), null);
    assertVerdict(
        matcher.match(
            new MatchOutputCommand(
                SubmissionModel.STANDARD_IO, hugeEpsilon, null, "1e1000000", "-1")),
        false);
    assertVerdict(
        matcher.match(
            new MatchOutputCommand(
                SubmissionModel.STANDARD_IO, hugeEpsilon, null, "1e1000000", "1")),
        true);
  }

  @Test
  void unrepresentableStandardNumbersHaveSanitizedInternalErrors() {
    assertCode(standard(MatchingStrategy.EPSILON, "1", "9e9999999999"), "HRN-031");
    assertThatThrownBy(() -> matcher.match(standard(MatchingStrategy.EPSILON, "1", "9e9999999999")))
        .hasMessage("Output matching failed")
        .hasNoCause();
  }

  @Test
  void theSameBeanIsDeterministicUnderConcurrentCalls() throws Exception {
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Callable<MatchResult> task =
          () -> typed(MatchingStrategy.EPSILON, list(leaf(TypeKind.DOUBLE)), "[1.0005]", "[1]");
      var results = executor.invokeAll(java.util.Collections.nCopies(40, task));
      for (var result : results) {
        assertVerdict(result.get(), true);
      }
    }
  }

  private MatchResult typed(
      MatchingStrategy strategy, TypeNode type, String actual, String expected) {
    return matcher.match(wrapper(strategy, type, SENTINEL + "\n" + actual, expected));
  }

  private static MatchOutputCommand wrapper(
      MatchingStrategy strategy, TypeNode type, String stdout, String expected) {
    return new MatchOutputCommand(
        SubmissionModel.FUNCTION_WRAPPER,
        new MatchSpec(strategy, EPSILON, type),
        SENTINEL,
        stdout,
        expected);
  }

  private static MatchOutputCommand standard(
      MatchingStrategy strategy, String actual, String expected) {
    return new MatchOutputCommand(
        SubmissionModel.STANDARD_IO,
        new MatchSpec(strategy, EPSILON, null),
        null,
        actual,
        expected);
  }

  private static TypeNode leaf(TypeKind kind) {
    return new TypeNode(kind, null, null);
  }

  private static TypeNode list(TypeNode child) {
    return new TypeNode(TypeKind.LIST, child, null);
  }

  private static TypeNode tree(TypeKind kind) {
    return new TypeNode(TypeKind.BINARY_TREE, leaf(kind), null);
  }

  private static TypeNode array(TypeKind kind, int dimensions) {
    return new TypeNode(TypeKind.ARRAY, leaf(kind), dimensions);
  }

  private static void assertVerdict(MatchResult result, boolean matches) {
    assertThat(result.verdict()).isEqualTo(matches ? MatchVerdict.MATCH : MatchVerdict.NO_MATCH);
  }

  private void assertCode(MatchOutputCommand command, String code) {
    assertThatThrownBy(() -> matcher.match(command))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, error -> assertThat(error.errorCode()).isEqualTo(code));
  }
}
