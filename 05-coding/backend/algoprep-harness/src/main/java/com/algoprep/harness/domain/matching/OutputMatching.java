package com.algoprep.harness.domain.matching;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.functionspec.MatchingStrategy;
import com.algoprep.common.functionspec.TypeKind;
import com.algoprep.common.functionspec.TypeNode;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Pure matching algorithms for P4, DD F3 logic section 7. */
public final class OutputMatching {
  private OutputMatching() {}

  /** Null actual denotes a missing wrapper sentinel; expected data is still checked first. */
  public static boolean matches(
      String actual,
      String expected,
      MatchingStrategy strategy,
      TypeNode type,
      BigDecimal epsilon) {
    return switch (strategy) {
      case EXACT -> expected.equals(actual);
      case TRIMMED -> actual != null && trimmed(actual, type).equals(trimmed(expected, type));
      case EPSILON ->
          type == null
              ? actual != null && tokensNear(actual, expected, epsilon)
              : typed(actual, expected, type, epsilon, false);
      case UNORDERED_SET ->
          type == null
              ? actual != null && frequencies(words(actual)).equals(frequencies(words(expected)))
              : typed(actual, expected, type, epsilon, true);
    };
  }

  private static String trimmed(String value, TypeNode type) {
    if (type != null
        && (type.kind() == TypeKind.LIST
            || type.kind() == TypeKind.BINARY_TREE
            || (type.kind() == TypeKind.ARRAY && type.of().kind() == TypeKind.STRING))) {
      return value;
    }
    return String.join(
        "\n",
        Arrays.stream(value.split("\n", -1))
            .map(line -> line.replaceAll("^[ \\t]+|[ \\t]+$", "").replaceAll("[ \\t]+", " "))
            .toList());
  }

  private static boolean typed(
      String actual, String expected, TypeNode type, BigDecimal epsilon, boolean unordered) {
    OutputValue expectedValue;
    try {
      expectedValue = TypedOutputParser.parse(expected, type);
      if (unordered) {
        expectedValue = canonical(expectedValue);
      }
    } catch (IllegalArgumentException | ArithmeticException invalid) {
      throw new DomainRuleViolationException("HRN-050", "Expected output cannot be parsed");
    }
    if (actual == null) {
      return false;
    }
    try {
      OutputValue actualValue = TypedOutputParser.parse(actual, type);
      return unordered
          ? frequencies(elements(canonical(actualValue)))
              .equals(frequencies(elements(expectedValue)))
          : near(actualValue, expectedValue, epsilon);
    } catch (IllegalArgumentException | ArithmeticException invalid) {
      return false;
    }
  }

  private static boolean near(OutputValue actual, OutputValue expected, BigDecimal epsilon) {
    if (actual.scalar() != null || expected.scalar() != null) {
      return actual.kind() == TypeKind.DOUBLE && expected.kind() == TypeKind.DOUBLE
          ? ScalarOutput.near(actual.scalar(), expected.scalar(), epsilon)
          : actual.equals(expected);
    }
    if (actual.children().size() != expected.children().size()) {
      return false;
    }
    for (int i = 0; i < actual.children().size(); i++) {
      if (!near(actual.children().get(i), expected.children().get(i), epsilon)) {
        return false;
      }
    }
    return true;
  }

  private static List<OutputValue> elements(OutputValue value) {
    return value.scalar() == null ? value.children() : List.of(value);
  }

  private static OutputValue canonical(OutputValue value) {
    if (value.kind() == TypeKind.DOUBLE) {
      return new OutputValue(ScalarOutput.printedDouble(value.scalar()), value.kind(), List.of());
    }
    return value.scalar() == null
        ? OutputValue.sequence(value.children().stream().map(OutputMatching::canonical).toList())
        : value;
  }

  private static <T> Map<T, Long> frequencies(List<T> values) {
    Map<T, Long> result = new HashMap<>();
    for (T value : values) {
      result.merge(value, 1L, Long::sum);
    }
    return result;
  }

  private static List<String> words(String value) {
    return value.isBlank() ? List.of() : List.of(value.strip().split("\\s+"));
  }

  private static boolean tokensNear(String actual, String expected, BigDecimal epsilon) {
    String[] left = actual.split("\n", -1);
    String[] right = expected.split("\n", -1);
    if (left.length != right.length) {
      return false;
    }
    for (int line = 0; line < left.length; line++) {
      if (!lineNear(words(left[line]), words(right[line]), epsilon)) {
        return false;
      }
    }
    return true;
  }

  private static boolean lineNear(List<String> actual, List<String> expected, BigDecimal epsilon) {
    if (actual.size() != expected.size()) {
      return false;
    }
    for (int i = 0; i < actual.size(); i++) {
      if (!ScalarOutput.near(actual.get(i), expected.get(i), epsilon)) {
        return false;
      }
    }
    return true;
  }
}
