package com.algoprep.common.functionspec;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Pure P5 evaluator (DD F3 API 6.3); no persistence, code generation or I/O. */
public final class FunctionSpecEvaluator {
  private FunctionSpecEvaluator() {}

  public static SpecEvaluation evaluate(FunctionSpecDraft draft) {
    Objects.requireNonNull(draft, "draft");
    Evaluation evaluation = new Evaluation(draft);
    evaluation.evaluate();
    return new SpecEvaluation(
        evaluation.hard, evaluation.hard.isEmpty() && evaluation.soft.isEmpty(), evaluation.soft);
  }

  private static final class Evaluation {
    private final FunctionSpecDraft draft;
    private final List<SpecEvaluation.Violation> hard = new ArrayList<>();
    private final List<SpecEvaluation.UnsupportedReason> soft = new ArrayList<>();
    private final Map<TypeKind, TypeNode> nodeTypes = new EnumMap<>(TypeKind.class);

    private Evaluation(FunctionSpecDraft draft) {
      this.draft = draft;
    }

    private void evaluate() {
      checkMatching();
      checkOverrides();
      if (draft.functionName() == null
          && draft.returnType() == null
          && draft.parameters() == null) {
        soft(7, "");
        return;
      }
      if (draft.functionName() == null
          || draft.returnType() == null
          || draft.parameters() == null) {
        hard(17, "");
        return;
      }
      checkFunctionName();
      checkType(draft.returnType(), "/returnType", 0);
      if (draft.parameters().size() > SpecLimits.MAX_PARAMETERS) {
        hard(16, "/parameters");
      }
      Map<Language, Set<String>> names = new EnumMap<>(Language.class);
      for (Language language : Language.values()) {
        names.put(language, new HashSet<>());
      }
      for (int i = 0; i < draft.parameters().size(); i++) {
        var parameter = draft.parameters().get(i);
        String path = "/parameters/" + i;
        checkParameter(parameter.name(), path + "/name", names);
        checkType(parameter.type(), path + "/type", 0);
      }
    }

    private void checkMatching() {
      if (draft.matchingStrategy() == MatchingStrategy.EPSILON
          && (draft.epsilonValue() == null || draft.epsilonValue().signum() <= 0)) {
        hard(20, "/epsilonValue");
      }
      if (draft.matchingStrategy() == MatchingStrategy.UNORDERED_SET
          && draft.returnType() != null
          && (draft.returnType().kind() == TypeKind.LINKED_LIST
              || draft.returnType().kind() == TypeKind.BINARY_TREE)) {
        hard(22, "/returnType");
      }
    }

    private void checkOverrides() {
      Set<String> keys = new HashSet<>();
      for (var entry : draft.nameOverrides().entrySet()) {
        String key = entry.getKey();
        String path = "/nameOverrides/" + pointer(key);
        Language language;
        try {
          language = Language.valueOf(key == null ? "" : key.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
          hard(18, path);
          continue;
        }
        if (!keys.add(language.name())) {
          hard(18, path);
        }
        String value = entry.getValue();
        if (!NameConventions.isIdentifier(value)
            || NameConventions.hasReservedPrefix(value)
            || NameConventions.RESERVED_WORDS.get(language).contains(value)) {
          hard(19, path);
        }
      }
    }

    private void checkFunctionName() {
      String name = draft.functionName();
      if (!NameConventions.isIdentifier(name)) {
        hard(11, "/functionName");
        return;
      }
      if (NameConventions.hasReservedPrefix(name)) {
        hard(12, "/functionName");
      } else {
        for (Language language : Language.values()) {
          String resolved = NameConventions.forLanguage(name, language, draft.nameOverrides());
          if (resolved != null && NameConventions.RESERVED_WORDS.get(language).contains(resolved)) {
            hard(12, "/functionName");
            break;
          }
        }
      }
    }

    private void checkParameter(String name, String path, Map<Language, Set<String>> names) {
      if (!NameConventions.isIdentifier(name)) {
        hard(13, path);
        return;
      }
      boolean reserved = NameConventions.hasReservedPrefix(name);
      boolean duplicate = false;
      for (Language language : Language.values()) {
        String resolved = NameConventions.forLanguage(name, language, Map.of());
        duplicate |= !names.get(language).add(resolved);
        reserved |= NameConventions.RESERVED_WORDS.get(language).contains(resolved);
        reserved |= language == Language.PYTHON && "self".equals(resolved);
        if (NameConventions.isIdentifier(draft.functionName())) {
          reserved |=
              Objects.equals(
                  resolved,
                  NameConventions.forLanguage(
                      draft.functionName(), language, draft.nameOverrides()));
        }
      }
      if (duplicate) {
        hard(14, path);
      }
      if (reserved) {
        hard(15, path);
      }
    }

    private void checkType(TypeNode type, String path, int depth) {
      TypeNode current = type;
      String currentPath = path;
      int currentDepth = depth;
      while (true) {
        checkNode(current, currentPath, currentDepth);
        if (current == null || current.of() == null) {
          return;
        }
        currentDepth += current.kind() != null && current.kind().isContainer() ? 1 : 0;
        current = current.of();
        currentPath += "/of";
      }
    }

    private void checkNode(TypeNode type, String path, int depth) {
      if (type == null || type.kind() == null) {
        hard(10, path);
        return;
      }
      TypeKind kind = type.kind();
      if (kind == TypeKind.OTHER) {
        soft(1, path);
      }
      if (kind != TypeKind.ARRAY && type.dimensions() != null
          || !kind.isContainer() && type.of() != null
          || kind == TypeKind.ARRAY && (type.dimensions() == null || type.dimensions() < 1)) {
        hard(10, path);
      }
      if (!kind.isContainer()) {
        return;
      }
      if (depth >= SpecLimits.MAX_CONTAINER_DEPTH) {
        soft(5, path);
      }
      if (kind == TypeKind.ARRAY
          && type.dimensions() != null
          && type.dimensions() > SpecLimits.MAX_ARRAY_DIMENSIONS) {
        soft(4, path);
      }
      checkElement(type, path);
    }

    private void checkElement(TypeNode type, String path) {
      if (type.of() == null) {
        soft(2, path);
        return;
      }
      TypeKind child = type.of().kind();
      boolean allowed = child != null && child.isScalar();
      if (type.kind() == TypeKind.LIST) {
        allowed |= child == TypeKind.LIST;
      } else if (type.kind() == TypeKind.LINKED_LIST || type.kind() == TypeKind.BINARY_TREE) {
        allowed &= child != TypeKind.STRING && child != TypeKind.CHAR;
        TypeNode previous = nodeTypes.putIfAbsent(type.kind(), type.of());
        if (previous != null && previous.kind() != child) {
          soft(6, path);
        }
      }
      if (!allowed) {
        soft(3, path);
      }
    }

    private void hard(int number, String path) {
      hard.add(new SpecEvaluation.Violation(code(number), path, "Invalid function specification"));
    }

    private void soft(int number, String path) {
      soft.add(new SpecEvaluation.UnsupportedReason(code(number), path));
    }

    private static String code(int number) {
      return "HRN-V" + (number < 10 ? "0" : "") + number;
    }

    private static String pointer(String value) {
      return value == null ? "null" : value.replace("~", "~0").replace("/", "~1");
    }
  }
}
