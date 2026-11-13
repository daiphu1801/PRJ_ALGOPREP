package com.algoprep.harness.domain.matching;

import com.algoprep.common.functionspec.TypeKind;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

/** Shared scalar grammar; exceptions deliberately exclude input data. */
final class ScalarOutput {
  private ScalarOutput() {}

  static boolean isNumber(String value) {
    int position = sign(value, 0);
    int end = digits(value, position);
    if (end == position) {
      return false;
    }
    position = end;
    if (position < value.length() && value.charAt(position) == '.') {
      end = digits(value, position + 1);
      if (end == position + 1) {
        return false;
      }
      position = end;
    }
    if (position < value.length()
        && (value.charAt(position) == 'e' || value.charAt(position) == 'E')) {
      position = sign(value, position + 1);
      end = digits(value, position);
      if (end == position) {
        return false;
      }
      position = end;
    }
    return position == value.length();
  }

  private static int sign(String value, int position) {
    return position < value.length()
            && (value.charAt(position) == '+' || value.charAt(position) == '-')
        ? position + 1
        : position;
  }

  private static int digits(String value, int position) {
    while (position < value.length()
        && value.charAt(position) >= '0'
        && value.charAt(position) <= '9') {
      position++;
    }
    return position;
  }

  static OutputValue parse(String value, TypeKind kind) {
    String canonical =
        switch (kind) {
          case INT -> Integer.toString(Integer.parseInt(value));
          case LONG -> Long.toString(Long.parseLong(value));
          case DOUBLE -> decimal(value);
          case BOOLEAN -> bool(value);
          case CHAR -> character(value);
          case STRING -> value;
          default -> throw invalid();
        };
    return new OutputValue(canonical, kind, List.of());
  }

  private static String decimal(String value) {
    if (value.equals("NaN") || value.equals("Infinity") || value.equals("-Infinity")) {
      return value;
    }
    if (!isNumber(value)) {
      throw invalid();
    }
    return new BigDecimal(value).stripTrailingZeros().toString();
  }

  private static String bool(String value) {
    if (!value.equals("true") && !value.equals("false")) {
      throw invalid();
    }
    return value;
  }

  private static String character(String value) {
    if (value.length() != 1 || value.charAt(0) < 33 || value.charAt(0) > 126) {
      throw invalid();
    }
    return value;
  }

  static boolean near(String actual, String expected, BigDecimal epsilon) {
    if (isNumber(actual) && isNumber(expected)) {
      // UP cannot round past epsilon when epsilon is exactly representable at this precision.
      // A bounded context avoids allocating billions of zeroes for hostile exponent gaps.
      MathContext context = new MathContext(epsilon.precision() + 2, RoundingMode.UP);
      return new BigDecimal(actual)
              .subtract(new BigDecimal(expected), context)
              .abs()
              .compareTo(epsilon)
          <= 0;
    }
    return actual.equals(expected);
  }

  static String printedDouble(String value) {
    if (!isNumber(value)) {
      return value;
    }
    BigDecimal decimal = new BigDecimal(value);
    if (decimal.precision() - (long) decimal.scale() > 309) {
      throw invalid();
    }
    if (decimal.abs().compareTo(new BigDecimal("0.0000000000005")) < 0) {
      return "0.000000000000";
    }
    return decimal.setScale(12, RoundingMode.HALF_UP).toPlainString();
  }

  static IllegalArgumentException invalid() {
    return new IllegalArgumentException("Invalid output representation");
  }
}
