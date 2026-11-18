package com.algoprep.aireview.domain.prompt;

import java.util.Objects;

/** One piece of untrusted data: source code, an answer, a question text, a transcript. */
public record DataBlock(BlockName name, String content) {
  public DataBlock {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(content, "content");
  }
}
