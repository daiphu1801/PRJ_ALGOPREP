package com.algoprep.harness.domain.signature;

import com.algoprep.common.functionspec.FunctionSpecDraft;
import com.algoprep.common.functionspec.Language;
import com.algoprep.common.functionspec.NameConventions;
import com.algoprep.common.functionspec.TypeNode;
import java.util.Map;
import java.util.stream.Collectors;

final class PythonDeclarationFormatter implements DeclarationFormatter {
  @Override
  public String declaration(FunctionSpecDraft draft) {
    String parameters =
        draft.parameters().stream()
            .map(p -> name(p.name(), Map.of()) + ": " + type(p.type()))
            .collect(Collectors.joining(", "));
    return "def "
        + name(draft.functionName(), draft.nameOverrides())
        + "(self"
        + (parameters.isEmpty() ? "" : ", " + parameters)
        + ") -> "
        + type(draft.returnType())
        + ":";
  }

  private static String name(String raw, Map<String, String> overrides) {
    return NameConventions.forLanguage(raw, Language.PYTHON, overrides);
  }

  private static String type(TypeNode node) {
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
      case OTHER -> throw new IllegalArgumentException("Unsupported declaration type");
    };
  }
}
