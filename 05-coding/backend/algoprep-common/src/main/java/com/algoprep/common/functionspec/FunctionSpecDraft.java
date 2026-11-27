package com.algoprep.common.functionspec;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** P5 input. Nullable signature fields distinguish absent and incomplete declarations. */
public record FunctionSpecDraft(
    String functionName,
    TypeNode returnType,
    List<FunctionParameter> parameters,
    Map<String, String> nameOverrides,
    MatchingStrategy matchingStrategy,
    BigDecimal epsilonValue) {
  public FunctionSpecDraft {
    parameters = parameters == null ? null : List.copyOf(parameters);
    nameOverrides =
        nameOverrides == null
            ? Map.of()
            : Collections.unmodifiableMap(new LinkedHashMap<>(nameOverrides));
  }
}
