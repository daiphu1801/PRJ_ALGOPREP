package com.algoprep.harness.domain.codegen;

import com.algoprep.common.functionspec.TypeNode;

/** C++ uses typed template instantiation; learner values never become source text. */
public final class CppTypeCodeEmitter implements TypeCodeEmitter {
  @Override
  public String type(TypeNode node) {
    return switch (node.kind()) {
      case INT -> "int";
      case LONG -> "long long";
      case DOUBLE -> "double";
      case BOOLEAN -> "bool";
      case CHAR -> "char";
      case STRING -> "std::string";
      case ARRAY ->
          "std::vector<".repeat(node.dimensions())
              + type(node.of())
              + ">".repeat(node.dimensions());
      case LIST -> "std::vector<" + type(node.of()) + ">";
      case LINKED_LIST -> "ListNode*";
      case BINARY_TREE -> "TreeNode*";
      case OTHER -> throw new IllegalArgumentException("Unsupported type");
    };
  }

  @Override
  public String read(TypeNode node) {
    return switch (node.kind()) {
      case ARRAY -> "apH_readArray<" + type(node) + ">(apH_in, " + node.dimensions() + ")";
      case LIST -> "apH_readList<" + type(node) + ">(apH_in)";
      case LINKED_LIST -> "apH_readLink<ListNode>(apH_in)";
      case BINARY_TREE -> "apH_readTree<TreeNode>(apH_in)";
      default -> "apH_scalar<" + type(node) + ">(apH_in.line())";
    };
  }

  @Override
  public String write(TypeNode node, String value) {
    return switch (node.kind()) {
      case ARRAY -> "apH_writeArray(apH_out, " + value + ");\n";
      case LIST -> "apH_writeList(apH_out, " + value + ");\n";
      case LINKED_LIST -> "apH_writeLink(apH_out, " + value + ");\n";
      case BINARY_TREE -> "apH_writeTree(apH_out, " + value + ");\n";
      default -> "apH_out.add(apH_text(" + value + "));\n";
    };
  }
}
