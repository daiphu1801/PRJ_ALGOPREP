package com.algoprep.harness.domain.codegen;

import com.algoprep.common.functionspec.TypeKind;
import com.algoprep.common.functionspec.TypeNode;

/** Java expressions delegate line parsing and bounded serialization to the trusted runtime. */
public final class JavaTypeCodeEmitter implements TypeCodeEmitter {
  @Override
  public String type(TypeNode node) {
    return switch (node.kind()) {
      case INT -> "int";
      case LONG -> "long";
      case DOUBLE -> "double";
      case BOOLEAN -> "boolean";
      case CHAR -> "char";
      case STRING -> "String";
      case ARRAY -> type(node.of()) + "[]".repeat(node.dimensions());
      case LIST -> "java.util.List<" + boxed(node.of()) + ">";
      case LINKED_LIST -> "ListNode";
      case BINARY_TREE -> "TreeNode";
      case OTHER -> throw new IllegalArgumentException("Unsupported type");
    };
  }

  private String boxed(TypeNode node) {
    return switch (node.kind()) {
      case INT -> "Integer";
      case LONG -> "Long";
      case DOUBLE -> "Double";
      case BOOLEAN -> "Boolean";
      case CHAR -> "Character";
      default -> type(node);
    };
  }

  @Override
  public String read(TypeNode node) {
    return "(" + type(node) + ") ApHCodec.read(apH_in, " + schema(node) + ")";
  }

  @Override
  public String write(TypeNode node, String value) {
    return "ApHCodec.write(apH_out, " + value + ", " + schema(node) + ");\n";
  }

  private String schema(TypeNode node) {
    if (node.kind() == TypeKind.ARRAY) {
      return "new ApHType(\"ARRAY\", " + node.dimensions() + ", " + schema(node.of()) + ")";
    }
    if (node.of() != null) {
      return "new ApHType(\"" + node.kind() + "\", 0, " + schema(node.of()) + ")";
    }
    return "new ApHType(\"" + node.kind() + "\", 0, null)";
  }
}
