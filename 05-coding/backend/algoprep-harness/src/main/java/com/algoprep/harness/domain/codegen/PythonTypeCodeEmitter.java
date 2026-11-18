package com.algoprep.harness.domain.codegen;

import com.algoprep.common.functionspec.TypeNode;

/** Python's schema tuple describes only validated types, never testcase data. */
public final class PythonTypeCodeEmitter implements TypeCodeEmitter {
  @Override
  public String type(TypeNode node) {
    return switch (node.kind()) {
      case INT, LONG -> "int";
      case DOUBLE -> "float";
      case BOOLEAN -> "bool";
      case CHAR, STRING -> "str";
      case ARRAY ->
          "List[".repeat(node.dimensions()) + type(node.of()) + "]".repeat(node.dimensions());
      case LIST -> "List[" + type(node.of()) + "]";
      case LINKED_LIST -> "ListNode";
      case BINARY_TREE -> "TreeNode";
      case OTHER -> throw new IllegalArgumentException("Unsupported type");
    };
  }

  @Override
  public String read(TypeNode node) {
    return "apH_read(apH_in, " + schema(node) + ")";
  }

  @Override
  public String write(TypeNode node, String value) {
    return "apH_write(apH_out, " + value + ", " + schema(node) + ")\n";
  }

  private String schema(TypeNode node) {
    return "(\""
        + node.kind()
        + "\", "
        + (node.dimensions() == null ? 0 : node.dimensions())
        + ", "
        + (node.of() == null ? "None" : schema(node.of()))
        + ")";
  }
}
