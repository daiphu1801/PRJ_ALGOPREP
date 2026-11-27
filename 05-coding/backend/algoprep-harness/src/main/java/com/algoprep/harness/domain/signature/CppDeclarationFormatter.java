package com.algoprep.harness.domain.signature;

import com.algoprep.common.functionspec.FunctionSpecDraft;
import com.algoprep.common.functionspec.Language;
import com.algoprep.common.functionspec.NameConventions;
import com.algoprep.common.functionspec.TypeNode;
import java.util.Map;
import java.util.stream.Collectors;

final class CppDeclarationFormatter implements DeclarationFormatter {
  @Override
  public String declaration(FunctionSpecDraft draft) {
    String parameters =
        draft.parameters().stream()
            .map(p -> type(p.type()) + " " + name(p.name(), Map.of()))
            .collect(Collectors.joining(", "));
    return type(draft.returnType())
        + " "
        + name(draft.functionName(), draft.nameOverrides())
        + "("
        + parameters
        + ")";
  }

  private static String name(String raw, Map<String, String> overrides) {
    return NameConventions.forLanguage(raw, Language.CPP, overrides);
  }

  private static String type(TypeNode node) {
    return switch (node.kind()) {
      case INT -> "int";
      case LONG -> "long long";
      case DOUBLE -> "double";
      case BOOLEAN -> "bool";
      case CHAR -> "char";
      case STRING -> "std::string";
      case ARRAY ->
          "vector<".repeat(node.dimensions()) + type(node.of()) + ">".repeat(node.dimensions());
      case LIST -> "vector<" + type(node.of()) + ">";
      case LINKED_LIST -> "ListNode*";
      case BINARY_TREE -> "TreeNode*";
      case OTHER -> throw new IllegalArgumentException("Unsupported declaration type");
    };
  }
}
