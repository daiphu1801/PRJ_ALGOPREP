package com.algoprep.problembank.domain.model;

import java.util.List;

/**
 * DD F2 section 58.1 {@code ProblemContext}: every field is named data for the AI prompt, never an
 * instruction. There is deliberately no field for a reference solution, a testcase or an object key
 * (BR-06 step 3).
 *
 * @param functionSignature null until the problem declares a complete signature
 * @param knownOptimalComplexity null whenever the column is empty (always, in the current scope)
 */
public record ProblemContextView(
    String title,
    String statementMarkdown,
    String constraintsMarkdown,
    List<String> topics,
    String difficulty,
    FunctionSignature functionSignature,
    String knownOptimalComplexity,
    String aiBrief,
    boolean noFullCode,
    boolean socraticOnly) {

  public ProblemContextView {
    topics = List.copyOf(topics);
  }

  /** {@code returnType} and {@code parameters} are the stored JSON text of the type schema. */
  public record FunctionSignature(String functionName, String returnType, String parameters) {}
}
