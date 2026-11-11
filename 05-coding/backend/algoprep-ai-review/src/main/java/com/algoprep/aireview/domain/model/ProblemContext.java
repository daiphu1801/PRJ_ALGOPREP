package com.algoprep.aireview.domain.model;

import java.util.List;

/**
 * F5 view of the problem (DD F5 section 32.4): statement data and the author's AI switches. No
 * reference solution, no testcase of any kind. Every string is data for the prompt, never an
 * instruction.
 */
public record ProblemContext(
    String title,
    String statementMarkdown,
    String constraintsMarkdown,
    List<String> topics,
    String difficulty,
    String functionSignature,
    String knownOptimalComplexity,
    String aiBrief,
    boolean noFullCode,
    boolean socraticOnly) {
  public ProblemContext {
    topics = topics == null ? List.of() : List.copyOf(topics);
  }
}
