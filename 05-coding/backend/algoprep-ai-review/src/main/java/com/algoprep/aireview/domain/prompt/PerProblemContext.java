package com.algoprep.aireview.domain.prompt;

/**
 * Layer 2: the author's {@code aiBrief} as a labelled, nonce-wrapped block of the USER message (DD
 * F5 BR-AI-01 step 4, owner decision 2026-10-29 F5-08). {@code escapedBrief} already has {@code <}
 * and {@code >} replaced, so it cannot close its own block.
 */
public record PerProblemContext(String nonce, String escapedBrief) {
  static final String TAG = "per_problem_context";

  /** The block text, or an empty string when the author wrote no brief. */
  public String renderUserBlock() {
    return escapedBrief == null || escapedBrief.isBlank()
        ? ""
        : PromptComposer.wrap(TAG, nonce, escapedBrief);
  }
}
