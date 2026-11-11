package com.algoprep.aireview.domain.prompt;

/**
 * Exactly one system message and one user message (BR-AI-01 postcondition). The LLM adapter sends
 * these two strings and nothing else.
 */
public record ComposedPrompt(
    SystemInstruction system, PerProblemContext perProblem, UserData data) {
  public String systemMessage() {
    return system.text();
  }

  /** The author brief block first (when present), then the data blocks. */
  public String userMessage() {
    String brief = perProblem.renderUserBlock();
    return brief.isEmpty() ? data.render() : brief + "\n" + data.render();
  }
}
