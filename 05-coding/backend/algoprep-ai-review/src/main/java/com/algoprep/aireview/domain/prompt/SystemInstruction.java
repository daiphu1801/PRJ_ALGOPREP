package com.algoprep.aireview.domain.prompt;

import java.util.Objects;

/**
 * Layer 1: the system message. The constructor is package-private on purpose; only {@code
 * PromptComposer} builds one, from the ACTIVE template plus fixed server sentences.
 */
public final class SystemInstruction {
  private final String text;

  SystemInstruction(String text) {
    this.text = Objects.requireNonNull(text, "text");
  }

  public String text() {
    return text;
  }
}
