package com.algoprep.harness.domain.matching;

import com.algoprep.common.functionspec.TypeKind;
import com.algoprep.common.functionspec.TypeNode;
import java.util.ArrayList;
import java.util.List;

/** Type-directed bracket grammar for LIST and ARRAY(STRING), with bounded type depth. */
final class BracketOutputParser {
  private final String input;
  private int position;

  BracketOutputParser(String input) {
    this.input = input;
  }

  OutputValue parse(TypeNode type) {
    OutputValue value = value(type, type.kind() == TypeKind.ARRAY ? type.dimensions() : 0);
    if (position != input.length()) {
      throw ScalarOutput.invalid();
    }
    return value;
  }

  private OutputValue value(TypeNode type, int dimensions) {
    if (type.kind() == TypeKind.LIST || dimensions > 0) {
      return sequence(type, dimensions);
    }
    if (type.kind() == TypeKind.STRING || type.kind() == TypeKind.CHAR) {
      return ScalarOutput.parse(quoted(), type.kind());
    }
    int start = position;
    while (position < input.length()
        && input.charAt(position) != ','
        && input.charAt(position) != ']') {
      position++;
    }
    return ScalarOutput.parse(input.substring(start, position), type.kind());
  }

  private OutputValue sequence(TypeNode type, int dimensions) {
    require('[');
    List<OutputValue> values = new ArrayList<>();
    if (!take(']')) {
      do {
        values.add(dimensions > 1 ? value(type, dimensions - 1) : value(type.of(), 0));
      } while (take(','));
      require(']');
    }
    return OutputValue.sequence(values);
  }

  private String quoted() {
    require('"');
    StringBuilder result = new StringBuilder();
    while (position < input.length()) {
      char next = input.charAt(position++);
      if (next == '"') {
        return result.toString();
      }
      if (next == '\\') {
        next = escaped();
      } else if (next < 32) {
        throw ScalarOutput.invalid();
      }
      result.append(next);
    }
    throw ScalarOutput.invalid();
  }

  private char escaped() {
    if (position == input.length()) {
      throw ScalarOutput.invalid();
    }
    return switch (input.charAt(position++)) {
      case '"' -> '"';
      case '\\' -> '\\';
      case 'n' -> '\n';
      case 't' -> '\t';
      case 'r' -> '\r';
      case 'b' -> '\b';
      case 'f' -> '\f';
      case 'u' -> controlEscape();
      default -> throw ScalarOutput.invalid();
    };
  }

  private char controlEscape() {
    if (position + 4 > input.length()) {
      throw ScalarOutput.invalid();
    }
    String hex = input.substring(position, position + 4);
    if (!hex.matches("00[01][0-9a-fA-F]")) {
      throw ScalarOutput.invalid();
    }
    position += 4;
    return (char) Integer.parseInt(hex, 16);
  }

  private boolean take(char expected) {
    if (position < input.length() && input.charAt(position) == expected) {
      position++;
      return true;
    }
    return false;
  }

  private void require(char expected) {
    if (!take(expected)) {
      throw ScalarOutput.invalid();
    }
  }
}
