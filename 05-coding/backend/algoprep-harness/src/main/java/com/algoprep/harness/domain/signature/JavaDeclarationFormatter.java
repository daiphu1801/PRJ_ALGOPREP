package com.algoprep.harness.domain.signature;

import com.algoprep.common.functionspec.FunctionSpecDraft;
import com.algoprep.common.functionspec.Language;
import com.algoprep.common.functionspec.NameConventions;
import com.algoprep.common.functionspec.TypeNode;
import java.util.Map;
import java.util.stream.Collectors;

final class JavaDeclarationFormatter implements DeclarationFormatter {
  @Override
  public String declaration(FunctionSpecDraft draft) {
    String parameters =
        draft.parameters().stream()
            .map(p -> type(p.type(), false) + " " + name(p.name(), Map.of()))
            .collect(Collectors.joining(", "));
    return "public "
        + type(draft.returnType(), false)
        + " "
        + name(draft.functionName(), draft.nameOverrides())
        + "("
        + parameters
        + ")";
  }

  private static String name(String raw, Map<String, String> overrides) {
    return NameConventions.forLanguage(raw, Language.JAVA, overrides);
  }

  private static String type(TypeNode node, boolean boxed) {
    return switch (node.kind()) {
      case INT -> boxed ? "Integer" : "int";
      case LONG -> boxed ? "Long" : "long";
      case DOUBLE -> boxed ? "Double" : "double";
      case BOOLEAN -> boxed ? "Boolean" : "boolean";
      case CHAR -> boxed ? "Character" : "char";
      case STRING -> "String";
      case ARRAY -> type(node.of(), false) + "[]".repeat(node.dimensions());
      case LIST -> "List<" + type(node.of(), true) + ">";
      case LINKED_LIST -> "ListNode";
      case BINARY_TREE -> "TreeNode";
      case OTHER -> throw new IllegalArgumentException("Unsupported declaration type");
    };
  }
}
