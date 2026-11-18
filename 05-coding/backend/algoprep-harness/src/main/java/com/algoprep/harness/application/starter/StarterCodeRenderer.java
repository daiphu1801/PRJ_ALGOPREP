package com.algoprep.harness.application.starter;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.functionspec.FunctionSpecDraft;
import com.algoprep.common.functionspec.FunctionSpecEvaluator;
import com.algoprep.common.functionspec.Language;
import com.algoprep.common.functionspec.TypeKind;
import com.algoprep.common.functionspec.TypeNode;
import com.algoprep.harness.application.dto.SubmissionModel;
import com.algoprep.harness.domain.signature.FunctionSignatureDeclarations;
import com.algoprep.harness.domain.starter.StarterTemplates;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Declaration and starter rendering only; P1 must perform real access/spec-version checks. */
public final class StarterCodeRenderer {
  private static final List<String> MARKERS =
      List.of("{{NODE_COMMENTS}}", "{{DECLARATION}}", "{{DEFAULT_RETURN}}");
  private static final Map<TypeKind, String> JAVA_DEFAULTS =
      Map.of(
          TypeKind.INT,
          "0",
          TypeKind.LONG,
          "0L",
          TypeKind.DOUBLE,
          "0.0",
          TypeKind.BOOLEAN,
          "false",
          TypeKind.CHAR,
          "' '");
  private static final Map<Language, LanguageRules> RULES =
      Map.of(
          Language.JAVA,
              new LanguageRules(
                  "//", type -> "return " + JAVA_DEFAULTS.getOrDefault(type, "null") + ";"),
          Language.CPP, new LanguageRules("//", type -> "return {};"),
          Language.PYTHON, new LanguageRules("#", type -> "pass"));
  private final Map<Language, StarterTemplates> templates;

  public StarterCodeRenderer(Map<Language, StarterTemplates> templates) {
    validateTemplates(templates);
    this.templates = Map.copyOf(templates);
  }

  public String render(Language language, SubmissionModel model, FunctionSpecDraft draft) {
    if (language == null) {
      throw error("HRN-010", "Missing starter language");
    }
    if (model == null) {
      throw error("HRN-031", "Missing starter submission model");
    }
    StarterTemplates selected = templates.get(language);
    if (model == SubmissionModel.STANDARD_IO) {
      return selected.standardIo();
    }
    validateDraft(draft);
    LanguageRules rules = RULES.get(language);
    return selected
        .functionWrapper()
        .replace(MARKERS.get(0), nodeComments(draft, rules.commentPrefix()))
        .replace(MARKERS.get(1), FunctionSignatureDeclarations.render(draft).get(language))
        .replace(MARKERS.get(2), rules.defaultReturn().apply(draft.returnType().kind()));
  }

  private static void validateDraft(FunctionSpecDraft draft) {
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
  }

  private static String nodeComments(FunctionSpecDraft draft, String prefix) {
    Map<TypeKind, TypeKind> nodes = new EnumMap<>(TypeKind.class);
    collectNodes(draft.returnType(), nodes);
    draft.parameters().forEach(parameter -> collectNodes(parameter.type(), nodes));
    StringBuilder result = new StringBuilder();
    nodes.forEach(
        (kind, value) -> {
          String description =
              kind == TypeKind.LINKED_LIST
                  ? "ListNode fields: val (" + value + "), next (ListNode)."
                  : "TreeNode fields: val (" + value + "), left/right (TreeNode).";
          result.append(prefix).append(' ').append(description).append(" Provided by the judge.\n");
        });
    return result.toString().stripTrailing();
  }

  private static void collectNodes(TypeNode type, Map<TypeKind, TypeKind> nodes) {
    if (type.kind() == TypeKind.LINKED_LIST || type.kind() == TypeKind.BINARY_TREE) {
      nodes.put(type.kind(), type.of().kind());
    }
    if (type.of() != null) {
      collectNodes(type.of(), nodes);
    }
  }

  private static void validateTemplates(Map<Language, StarterTemplates> templates) {
    if (templates == null || templates.size() != Language.values().length) {
      throw invalidTemplate();
    }
    for (Language language : Language.values()) {
      StarterTemplates pair = templates.get(language);
      if (pair == null
          || pair.functionWrapper() == null
          || pair.standardIo() == null
          || pair.standardIo().isBlank()
          || pair.standardIo().contains("{{")) {
        throw invalidTemplate();
      }
      String remaining = pair.functionWrapper();
      for (String marker : MARKERS) {
        int index = remaining.indexOf(marker);
        if (index < 0 || remaining.indexOf(marker, index + marker.length()) >= 0) {
          throw invalidTemplate();
        }
        remaining = remaining.replace(marker, "");
      }
      if (remaining.contains("{{")) {
        throw invalidTemplate();
      }
    }
  }

  private static DomainRuleViolationException invalidTemplate() {
    return error("HRN-030", "Invalid starter templates");
  }

  private static DomainRuleViolationException error(String code, String message) {
    return new DomainRuleViolationException(code, message);
  }

  private record LanguageRules(String commentPrefix, Function<TypeKind, String> defaultReturn) {}
}
