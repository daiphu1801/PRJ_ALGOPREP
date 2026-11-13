package com.algoprep.harness.domain.matching;

import com.algoprep.common.functionspec.TypeKind;
import com.algoprep.common.functionspec.TypeNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Parses the stdout grammar from DD F3 logic section 3.4, never stdin headers for arrays. */
final class TypedOutputParser {
  private TypedOutputParser() {}

  static OutputValue parse(String input, TypeNode type) {
    return switch (type.kind()) {
      case LIST -> new BracketOutputParser(input).parse(type);
      case ARRAY -> array(input, type);
      case LINKED_LIST -> linked(input, type.of());
      case BINARY_TREE -> tree(input, type.of());
      default -> ScalarOutput.parse(input, type.kind());
    };
  }

  private static OutputValue array(String input, TypeNode type) {
    if (type.of().kind() == TypeKind.STRING) {
      OutputValue value = new BracketOutputParser(input).parse(type);
      rectangular(value, type.dimensions());
      return value;
    }
    return arrayLines(input, type.of(), type.dimensions());
  }

  private static OutputValue arrayLines(String input, TypeNode element, int dimensions) {
    if (dimensions == 1) {
      return tokens(input, element);
    }
    String delimiter = dimensions == 3 ? "\n\n" : "\n";
    List<OutputValue> rows =
        Arrays.stream(input.split(delimiter, -1))
            .map(row -> arrayLines(row, element, dimensions - 1))
            .toList();
    OutputValue value = OutputValue.sequence(rows);
    rectangular(value, dimensions);
    return value;
  }

  private static void rectangular(OutputValue value, int dimensions) {
    if (dimensions < 2 || value.children().isEmpty()) {
      return;
    }
    List<Integer> first = shape(value.children().getFirst(), dimensions - 1);
    for (OutputValue child : value.children()) {
      if (!shape(child, dimensions - 1).equals(first)) {
        throw ScalarOutput.invalid();
      }
    }
  }

  private static List<Integer> shape(OutputValue value, int dimensions) {
    List<Integer> result = new ArrayList<>();
    result.add(value.children().size());
    if (dimensions > 1 && !value.children().isEmpty()) {
      rectangular(value, dimensions);
      result.addAll(shape(value.children().getFirst(), dimensions - 1));
    }
    return result;
  }

  private static OutputValue tokens(String input, TypeNode element) {
    if (input.isEmpty()) {
      return OutputValue.sequence(List.of());
    }
    return OutputValue.sequence(
        Arrays.stream(input.split(" ", -1))
            .map(token -> ScalarOutput.parse(token, element.kind()))
            .toList());
  }

  private static OutputValue linked(String input, TypeNode element) {
    int newline = input.indexOf('\n');
    String count = newline < 0 ? input : input.substring(0, newline);
    int size = Integer.parseInt(count);
    OutputValue values = tokens(newline < 0 ? "" : input.substring(newline + 1), element);
    if (size < 0 || values.children().size() != size || (size > 0 && newline < 0)) {
      throw ScalarOutput.invalid();
    }
    return values;
  }

  private static OutputValue tree(String input, TypeNode element) {
    String[] tokens = input.split(" ", -1);
    List<OutputValue> values = new ArrayList<>();
    long slots = 1;
    for (String token : tokens) {
      if (slots-- == 0) {
        throw ScalarOutput.invalid();
      }
      if (token.equals("#")) {
        values.add(OutputValue.leaf("#"));
      } else {
        values.add(ScalarOutput.parse(token, element.kind()));
        slots += 2;
      }
    }
    if (values.size() > 1 && tokens[tokens.length - 1].equals("#")) {
      throw ScalarOutput.invalid();
    }
    return OutputValue.sequence(values);
  }
}
