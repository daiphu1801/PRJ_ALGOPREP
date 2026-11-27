package com.algoprep.harness.application.prepare;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.functionspec.FunctionSpecDraft;
import com.algoprep.common.functionspec.FunctionSpecEvaluator;
import com.algoprep.common.functionspec.Language;
import com.algoprep.common.functionspec.TypeKind;
import com.algoprep.common.functionspec.TypeNode;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiFunction;

/** Trusted node declarations for P2; callers place Java nodes after the learner region. */
public final class NodeSourceRenderer {
  private static final Map<Language, BiFunction<TypeKind, TypeKind, String>> FORMATTERS =
      Map.of(
          Language.JAVA,
          NodeSourceRenderer::javaNode,
          Language.CPP,
          NodeSourceRenderer::cppNode,
          Language.PYTHON,
          NodeSourceRenderer::pythonNode);

  public String render(Language language, FunctionSpecDraft draft) {
    if (language == null) {
      throw error("HRN-010", "Missing node language");
    }
    if (draft == null
        || draft.functionName() == null
        || draft.returnType() == null
        || draft.parameters() == null) {
      throw error("HRN-005", "Function signature is missing");
    }
    var evaluation = FunctionSpecEvaluator.evaluate(draft);
    if (!evaluation.violations().isEmpty()) {
      throw error("HRN-006", "Invalid stored function specification");
    }
    if (!evaluation.wrapperSupported()) {
      throw error("HRN-004", "Function wrapper is unsupported");
    }
    Map<TypeKind, TypeKind> nodes = new EnumMap<>(TypeKind.class);
    collect(draft.returnType(), nodes);
    draft.parameters().forEach(parameter -> collect(parameter.type(), nodes));
    StringBuilder text = new StringBuilder();
    nodes.forEach((kind, value) -> text.append(FORMATTERS.get(language).apply(kind, value)));
    return text.toString();
  }

  private static void collect(TypeNode type, Map<TypeKind, TypeKind> nodes) {
    if (type.kind() == TypeKind.LINKED_LIST || type.kind() == TypeKind.BINARY_TREE) {
      nodes.put(type.kind(), type.of().kind());
    }
    if (type.of() != null) {
      collect(type.of(), nodes);
    }
  }

  private static String javaNode(TypeKind kind, TypeKind value) {
    String type =
        switch (value) {
          case INT -> "int";
          case LONG -> "long";
          case DOUBLE -> "double";
          case BOOLEAN -> "boolean";
          default -> throw new IllegalArgumentException("Unsupported node value");
        };
    if (kind == TypeKind.LINKED_LIST) {
      return "class ListNode {\n    public "
          + type
          + " val;\n    public ListNode next;\n"
          + "    public ListNode() {}\n    public ListNode("
          + type
          + " val) { this.val = val; }\n"
          + "    public ListNode("
          + type
          + " val, ListNode next) { this.val = val; this.next = next; }\n}\n";
    }
    return "class TreeNode {\n    public "
        + type
        + " val;\n    public TreeNode left, right;\n"
        + "    public TreeNode() {}\n    public TreeNode("
        + type
        + " val) { this.val = val; }\n"
        + "    public TreeNode("
        + type
        + " val, TreeNode left, TreeNode right) {\n"
        + "        this.val = val; this.left = left; this.right = right;\n    }\n}\n";
  }

  private static String cppNode(TypeKind kind, TypeKind value) {
    String type =
        switch (value) {
          case INT -> "int";
          case LONG -> "long long";
          case DOUBLE -> "double";
          case BOOLEAN -> "bool";
          default -> throw new IllegalArgumentException("Unsupported node value");
        };
    if (kind == TypeKind.LINKED_LIST) {
      return "struct ListNode {\n    "
          + type
          + " val;\n    ListNode* next;\n"
          + "    ListNode("
          + type
          + " value = {}, ListNode* link = nullptr) : val(value), next(link) {}\n};\n";
    }
    return "struct TreeNode {\n    "
        + type
        + " val;\n    TreeNode* left;\n    TreeNode* right;\n"
        + "    TreeNode("
        + type
        + " value = {}, TreeNode* l = nullptr, TreeNode* r = nullptr)\n"
        + "        : val(value), left(l), right(r) {}\n};\n";
  }

  private static String pythonNode(TypeKind kind, TypeKind value) {
    String initial = value == TypeKind.BOOLEAN ? "False" : value == TypeKind.DOUBLE ? "0.0" : "0";
    if (kind == TypeKind.LINKED_LIST) {
      return "class ListNode:\n    def __init__(self, val="
          + initial
          + ", next=None):\n"
          + "        self.val = val\n        self.next = next\n";
    }
    return "class TreeNode:\n    def __init__(self, val="
        + initial
        + ", left=None, right=None):\n"
        + "        self.val = val\n        self.left = left\n        self.right = right\n";
  }

  private static DomainRuleViolationException error(String code, String message) {
    return new DomainRuleViolationException(code, message);
  }
}
