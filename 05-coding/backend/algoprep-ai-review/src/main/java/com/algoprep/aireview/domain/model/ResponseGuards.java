package com.algoprep.aireview.domain.model;

/**
 * Switches stored in {@code prompt_templates.response_guards}. The server turns each active switch
 * into one fixed sentence (BR-AI-01 step 2c); {@code noFullSolution = false} adds no sentence.
 */
public record ResponseGuards(
    boolean citeLines, boolean answerInVietnamese, boolean noFullSolution) {
  /** GENERATE_TESTCASE templates store NULL guards; nothing is switched on. */
  public static final ResponseGuards NONE = new ResponseGuards(false, false, false);
}
